#include <jfvm.h>
#include <stdbool.h>

//ARC (automatic reference counting)

// NOTE : gcc documentation on __atomic_test_and_set is wrong.  Return value is "false" if previous value was 'set'

/** Gets an object thread safely and increments refcnt. Locking is performed on src. */
void jfvm_arc_get(JVM *jvm, Object **dest, Object **src) {
  Object *obj;
  *dest = NULL;
  do {
    obj = *src;
    if (obj == NULL) return;  //no object there
    //try to get rights
  } while (!__atomic_test_and_set(&obj->reflck, __ATOMIC_SEQ_CST));
  //obtained exclusive rights to object
  __atomic_add_fetch(&obj->refcnt, 1, __ATOMIC_SEQ_CST);
//  printf("inc:%s\n", obj->cls->name);
  *dest = obj;
  __atomic_clear(&obj->reflck, __ATOMIC_SEQ_CST);
}

/** Sets an object to another object thread safely. Locking is performed on dest. */
void jfvm_arc_put(JVM *jvm, Object **dest, Object **src) {
  void* expected;
  Object *org;  //current value of dest
  int cnt;
//  printf(" arc_put:%p=%p\n", dest, src);
  while (1) {
    org = *dest;
    if (org != NULL) {
      if (!__atomic_test_and_set(&org->reflck, __ATOMIC_SEQ_CST)) continue;
      if (org != *dest) {
        __atomic_clear(&org->reflck, __ATOMIC_SEQ_CST);
        continue;  //another thread was too fast (sleight of hand)
      }
      *dest = *src;
      cnt = __atomic_sub_fetch(&org->refcnt, 1, __ATOMIC_SEQ_CST);
//      printf("dec:%s\n", org->cls->name);
      if (cnt == 0) {
        jfvm_arc_delete(jvm, org);
      } else {
        __atomic_clear(&org->reflck, __ATOMIC_SEQ_CST);
      }
    } else {
      expected = NULL;
      if (!__atomic_compare_exchange(dest,&expected,src,false,__ATOMIC_SEQ_CST,__ATOMIC_SEQ_CST)) continue;
    }
    break;
  };
  if (*src != NULL) {
    jfvm_arc_inc(jvm, *src);
  }
}

void jfvm_arc_copy(JVM *jvm, Object **dest, Object **src) {
  Object *temp = NULL;
  jfvm_arc_get(jvm, &temp, src);
  jfvm_arc_put(jvm, dest, &temp);
}

void jfvm_arc_delete(JVM *jvm, Object *obj) {
  if (obj == NULL) return;
//  printf("jfvm_arc_delete:%s:%p\n", obj->cls->name, obj);
  if (obj->locks != NULL) {
    jfvm_arc_free_locks(jvm, obj);
  }
  jfvm_destroy_add(jvm, obj);
}

//increment refcnt for object that is already obtained
void jfvm_arc_inc(JVM *jvm, Object *obj) {
  if (obj == NULL) return;
  while (!__atomic_test_and_set(&obj->reflck, __ATOMIC_SEQ_CST)) {}
  __atomic_add_fetch(&obj->refcnt, 1, __ATOMIC_SEQ_CST);
//  printf("inc:%s %p\n", obj->cls->name, obj);
  __atomic_clear(&obj->reflck, __ATOMIC_SEQ_CST);
}

//decrement refcnt for object that is already obtained
void jfvm_arc_dec(JVM *jvm, Object *obj) {
  int cnt;
  if (obj == NULL) return;
  while (!__atomic_test_and_set(&obj->reflck, __ATOMIC_SEQ_CST)) {}
  cnt = __atomic_sub_fetch(&obj->refcnt, 1, __ATOMIC_SEQ_CST);
//  printf("dec:%s %p\n", obj->cls->name, obj);
  if (cnt == 0) {
    jfvm_arc_delete(jvm, obj);
  } else {
    __atomic_clear(&obj->reflck, __ATOMIC_SEQ_CST);
  }
}

/** Decreases refcnt to object in slot (and destroys object if refcnt == zero) */
void jfvm_arc_release(JVM *jvm, Slot *slot) {
  if (slot->obj == NULL) return;
  //NOTE : sometimes the slot->type is unset (optz) (anytime a temp slot is used)
  jfvm_arc_dec(jvm, slot->obj);
  //DO NOT CLEAR slot->obj (see if_acmpeq and if_acmpne)
  slot->type = 0;
}

/** Create mutex locks for an object if not created yet.*/
void jfvm_arc_create_locks(JVM *jvm, Object *obj) {
  while (!__atomic_test_and_set(&obj->reflck, __ATOMIC_SEQ_CST)) {}
  if (obj->locks == NULL) {
    obj->locks = jfvm_alloc(jvm, sizeof(Locks));
    obj->locks->mutex = jfvm_mutex_alloc();
    obj->locks->cond = jfvm_cond_alloc();
  }
  __atomic_clear(&obj->reflck, __ATOMIC_SEQ_CST);
}

void jfvm_arc_free_locks(JVM *jvm, Object *obj) {
  if (obj->locks == NULL) return;
  jfvm_mutex_free(obj->locks->mutex);
  jfvm_cond_free(obj->locks->cond);
  jfvm_free(jvm, obj->locks);
  obj->locks = NULL;
}

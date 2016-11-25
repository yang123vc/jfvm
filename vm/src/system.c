#include <jfvm.h>
#include <setjmp.h>

/** init pre classpath loading */
void jfvm_init_pre(JVM *jvm) {
  jfvm_init_class_pre(jvm);
  jfvm_init_destroyer_pre(jvm);
}

/** init post classpath loading */
void jfvm_init_pst(JVM *jvm) {
  jfvm_init_class_pst(jvm);
  jfvm_init_destroyer_pst(jvm);
}

int jfvm_get_type_size(char type) {
  switch (type) {
    case 'Z':
    case 'B': return 1;
    case 'C':
    case 'S': return 2;
    case 'I':
    case 'F': return 4;
    case 'J':
    case 'D': return 8;
    case 'L':
    case '[':
    case 'A': return sizeof(void*);
  }
  return 0;
}

static volatile jboolean main_thread_started = J_FALSE;
static Object* main_thread;
static Object* destroyer_thread;
static Slot main_stack[1] = {{0,0}};
static Class* main_class;
static int main_clsidx;
static int main_return;

static void jfvm_main_thread(JVM *jvm, Slot *local) {
  main_thread_started = J_TRUE;
  jfvm_invokestatic(jvm, main_class, main_clsidx, main_stack);
  main_return = main_stack[0].i32;
}

void jfvm_main(const char *lib_name[], void *lib_handle[], int lib_count, int argc, const char **argv, const char *clsname) {
  JVM *jvm;
  Slot stack[1] = {{0,0}};
  jvm = jfvm_alloc(NULL, sizeof(JVM));
  UMethod* umethod = jfvm_alloc(jvm, sizeof(UMethod));
  umethod->type = UMethod_type;
  jvm->ustack = (UStack*)umethod;
  if (setjmp(umethod->buf) != 0) {
    printf("jfvm_main():Unhandled exception\n");
    jfvm_exit(1);
  }
  jfvm_init_pre(jvm);
  //load DLLs
  for(int a=0;a<lib_count;a++) {
    lib_handle[a] = jfvm_classpath_add(jvm, lib_name[a]);
  }
  jfvm_init_pst(jvm);

  main_class = jfvm_find_class(jvm, clsname);
  if (main_class == NULL) { printf("Error:Class not found:%s\n", clsname); jfvm_exit(1); };
  main_clsidx = jfvm_get_static_method_clsidx(jvm, main_class, "main([Ljava/lang/String;)I");
  if (main_clsidx == -1) { printf("Error:static void main(String []) not found!\n"); jfvm_exit(1); };

  Object *main_args = jfvm_anewarray(jvm, "java/lang/String", argc-1);
  main_stack[0].obj = main_args;
  main_stack[0].type = 'L';

  for(int a=1;a<argc;a++) {
    main_args->array->objs[a-1] = jfvm_new_string(jvm, argv[a], strlen(argv[a]));
  }

  //create thread and call jfvm_main_thread(String cls)
  main_thread = jfvm_new(jvm, jfvm_find_class(jvm, "java/lang/Thread"));
  int runobjidx = jfvm_get_method_objidx(jvm, main_thread->cls, "run()V");
  main_thread->methods[runobjidx] = &jfvm_main_thread;
  int startobjidx = jfvm_get_method_objidx(jvm, main_thread->cls, "start()V");
  int joinobjidx = jfvm_get_method_objidx(jvm, main_thread->cls, "join()V");

  jfvm_arc_inc(jvm, main_thread);
  stack[0].obj = main_thread;
  stack[0].type = 'L';
  jfvm_invokevirtual(jvm, main_thread->cls, startobjidx, stack);

  jfvm_arc_inc(jvm, main_thread);
  stack[0].obj = main_thread;
  stack[0].type = 'L';
  jfvm_invokevirtual(jvm, main_thread->cls, joinobjidx, stack);

  jfvm_arc_inc(jvm, destroyer_thread);
  stack[0].obj = destroyer_thread;
  stack[0].type = 'L';
  jfvm_invokevirtual(jvm, destroyer_thread->cls, joinobjidx, stack);  //should never return

  printf("Error:jfvm_main() returning\n");
  jfvm_exit(1);
}

static Object* head = NULL;
static Object* tail = NULL;
static void* destroy_mutex = NULL;
static int threads = 0;

void jfvm_init_destroyer_pre(JVM *jvm) {
  destroy_mutex = jfvm_mutex_alloc();
  head = jfvm_alloc(jvm, sizeof(Object));
  tail = head;
}

void jfvm_init_destroyer_pst(JVM *jvm) {
  Slot stack[1] = {{0,0}};
  destroyer_thread = jfvm_new(jvm, jfvm_find_class(jvm, "java/lang/Thread"));
  int runidx = jfvm_get_method_objidx(jvm, destroyer_thread->cls, "run()V");
  destroyer_thread->methods[runidx] = &jfvm_destroy_run;
  int startobjidx = jfvm_get_method_objidx(jvm, destroyer_thread->cls, "start()V");
  jfvm_arc_inc(jvm, destroyer_thread);
  stack[0].obj = destroyer_thread;
  stack[0].type = 'L';
  jfvm_invokevirtual(jvm, destroyer_thread->cls, startobjidx, stack);
}

void jfvm_destroy_run(JVM *jvm, Slot *local) {
  Slot stack[1] = {{0,0}};
  Object *stop;
  Object *next;
  Class *thread = jfvm_find_class(jvm, "java/lang/Thread");
  Class *object = jfvm_find_class(jvm, "java/lang/Object");
  Class *throwable = jfvm_find_class(jvm, "java/lang/Throwable");
  int sleepclsidx = jfvm_get_static_method_clsidx(jvm, thread, "sleep(J)V");
  int finalobjidx = jfvm_get_method_objidx(jvm, object, "finalize()V");
  UCatch ucatch = {.type = UCatch_type, .cls = throwable, .prev = NULL};
  while (1) {
    stop = head;
    stack[0].i32 = 100;
    stack[0].type = 'I';
    jfvm_invokestatic(jvm, thread, sleepclsidx, stack);  //sleep 100ms
#ifdef JFVM_DEBUG_DETROYER
    printf("deleting start\n");
#endif
    while (tail != stop) {
      next = tail->next;
      tail->next = NULL;  //in case mutex is needed again
      if (tail->type == 0) {
        jfvm_free(jvm, tail);  //first object created in jfvm_init_destroyer_pre()
      } else {
        if (setjmp(ucatch.buf) == 0) {
          jvm->ustack = (UStack*)&ucatch;
          ucatch.prev = NULL;
          tail->reflck = 0;
          tail->refcnt = 2;  //finalize() will dec cnt - don't want it to re-add to destroyer list
          stack[0].obj = tail;
          stack[0].type = 'L';
          jfvm_invokevirtual(jvm, object, finalobjidx, stack);
        } else {
          //TODO:exception throw in finalize() - log it ???
        }
        if (tail->refcnt == 1) {
          jfvm_destroy_obj(jvm, tail);
        } else {
          //finalize() has prevented object from being deleted
          jfvm_arc_dec(jvm, tail);
        }
      }
      tail = next;
    }
#ifdef JFVM_DEBUG_DETROYER
    printf("deleting done\n");
#endif
    if (main_thread_started == J_TRUE && threads == 1) {
      jfvm_exit(main_return);
    }
  }
}

void jfvm_destroy_add(JVM *jvm, Object *obj) {
  jfvm_mutex_lock(destroy_mutex);
  head->next = obj;
  head = obj;
  jfvm_mutex_unlock(destroy_mutex);
}

void jfvm_destroy_obj(JVM *jvm, Object *obj) {
#ifdef JFVM_DEBUG_OBJECT
  printf(" destroy:%p:%s\n", obj, obj->cls->name);
#endif
  Class *cls = obj->cls;
  char type = obj->type;
  if (type == 0) {
    printf("Error:Type of Object to be deleted is unknown:%s\n", obj->cls->name);
    return;
  }
  if (obj->locks != NULL) {
    jfvm_arc_free_locks(jvm, obj);
  }
  if (jfvm_is_object_array(type)) {
    int length = obj->array->length;
    if (type == 'A') {
      //array of objects
      for(int a=0;a<length;a++) {
        jfvm_arc_dec(jvm, obj->array->objs[a]);
      }
    }
    jfvm_free(jvm, obj->array);
    obj->array = NULL;
  } else {
    //complex object (release each field)
    Field *field;
    do {
      field = cls->fields;
      while (field->name != NULL) {
        type = field->desc[0];
        if (type == 'L' || type == '[') {
          int objidx = field->offset;
          Object *subfield = (Object*)obj->fields[objidx];
          if (subfield != NULL) {
            jfvm_arc_dec(jvm, subfield);
          }
        }
        field++;
      };
      cls = cls->super_class;
    } while (cls != NULL);
  }
  jfvm_free(jvm, obj);
}

void jfvm_thread_add(JVM *jvm, Object *obj) {
  jfvm_mutex_lock(destroy_mutex);
  threads++;
  jfvm_mutex_unlock(destroy_mutex);
}

void jfvm_thread_remove(JVM *jvm, Object *obj) {
  jfvm_mutex_lock(destroy_mutex);
  threads--;
  jfvm_mutex_unlock(destroy_mutex);
}

void *jfvm_stack_alloc(JVM *jvm, int count) {
  if (count == 0) count = 1;
  void *ptr = jfvm_alloc(jvm, sizeof(Slot) * count);
#ifdef JFVM_DEBUG_MEMORY
  printf("   stack:%p\n", ptr);
#endif
  return ptr;
}

void jfvm_stack_free(JVM *jvm, void *stack) {
  jfvm_free(jvm, stack);
}

void *jfvm_stack_realloc(JVM *jvm, void *stack, int count) {
  if (count == 0) count = 1;
  return jfvm_realloc(jvm, stack, sizeof(Slot) * count);
}

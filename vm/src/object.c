#include <jfvm.h>

//TODO : these get / put funcs can be moved into the compiler to directly generate correct code

void jfvm_getfield(JVM *jvm, Class *cls, char type, int objidx, Slot *_this, Slot *dest) {
  Object *obj = _this->obj;
  Union *src = (Union*)&obj->fields[objidx];
  switch (type) {
    case 'Z':
    case 'B': dest->i8 = src->i8; break;
    case 'C':
    case 'S': dest->i16 = src->i16; break;
    case 'F':
    case 'I': dest->i32 = src->i32; break;
    case 'J':
    case 'D': dest->i64 = src->i64; break;
    case '[':
    case 'A':
      type = 'L';
    case 'L': jfvm_arc_get(jvm, &dest->obj, &src->obj); break;
  }
  dest->type = type;
}

void jfvm_getstatic(JVM *jvm, Class *cls, char type, int clsidx, Slot *dest) {
  Field *src = &cls->static_fields[clsidx];
  switch (type) {
    case 'Z':
    case 'B': dest->i8 = src->i8; break;
    case 'C':
    case 'S': dest->i16 = src->i16; break;
    case 'F':
    case 'I': dest->i32 = src->i32; break;
    case 'J':
    case 'D': dest->i64 = src->i64; break;
    case '[':
    case 'A':
      type = 'L';
    case 'L': jfvm_arc_get(jvm, &dest->obj, &src->obj); break;
  }
  dest->type = type;
}

void jfvm_putfield(JVM *jvm, Class *cls, char type, int objidx, Slot *_this, Slot *src) {
  Object *obj = _this->obj;
  Union *dest = (Union*)&obj->fields[objidx];
//  printf("putfield:idx=%d obj=%p src->obj=%p dest=%p cls=%s\n", objidx, obj, src->obj, dest, cls->name);
  switch (type) {
    case 'Z':
    case 'B': dest->i8 = src->i8; break;
    case 'C':
    case 'S': dest->i16 = src->i16; break;
    case 'F':
    case 'I': dest->i32 = src->i32; break;
    case 'J':
    case 'D': dest->i64 = src->i64; break;
    case '[':
    case 'A':
    case 'L': jfvm_arc_put(jvm, &dest->obj, &src->obj); break;
  }
}

void jfvm_putstatic(JVM *jvm, Class *cls, char type, int clsidx, Slot *src) {
  Field *dest = &cls->static_fields[clsidx];
  switch (type) {
    case 'Z':
    case 'B': dest->i8 = src->i8; break;
    case 'C':
    case 'S': dest->i16 = src->i16; break;
    case 'F':
    case 'I': dest->i32 = src->i32; break;
    case 'J':
    case 'D': dest->i64 = src->i64; break;
    case '[':
    case 'A':
    case 'L': jfvm_arc_put(jvm, &dest->obj, &src->obj); break;
  }
}

void jfvm_invokevirtual(JVM *jvm, Class *cls, int objidx, Slot *args) {
  void (*method)(JVM *jvm, Slot *local);
  Object *obj = args[0].obj;
  method = (void (*)(JVM *, Slot *)) obj->methods[objidx];
#ifdef JFVM_DEBUG_INVOKE
  printf("ivirtual:%p:%s.<virtual> @ %d:%p:%p\n", jvm->thread, cls->name, objidx, method, jvm->ustack);
#endif
  if (method == NULL) {
    jfvm_throw_abstractmethodcalled(jvm);
  }
  (*method)(jvm, args);
}

void jfvm_invokestatic(JVM *jvm, Class *cls, int clsidx, Slot *args) {
  void (*method)(JVM *jvm, Slot *local);
  method = cls->static_methods[clsidx].method;
#ifdef JFVM_DEBUG_INVOKE
  printf("i static:%p:%s.%s @ %d:%p\n", jvm->thread, cls->name, cls->static_methods[clsidx].name_desc, clsidx, method);
#endif
  if (method == NULL) {
    jfvm_throw_abstractmethodcalled(jvm);
  }
  (*method)(jvm, args);
}

void jfvm_invokespecial(JVM *jvm, Class *cls, int clsidx, Slot *args) {
  void (*method)(JVM *jvm, Slot *local);
  method = cls->methods[clsidx].method;
#ifdef JFVM_DEBUG_INVOKE
  printf("ispecial:%p:%s.%s @ %d:%p\n", jvm->thread, cls->name, cls->methods[clsidx].name_desc, clsidx, method);
#endif
  if (method == NULL) {
    jfvm_throw_abstractmethodcalled(jvm);
  }
  (*method)(jvm, args);
}

//jfvm_invokedynamic ??? see jfvm_create_lambda for now

void jfvm_monitor_enter(JVM *jvm, Object *obj) {
  if (obj == NULL) jfvm_throw_npe(jvm);
  jfvm_arc_inc(jvm, obj);
  jfvm_arc_create_locks(jvm, obj);
  jfvm_mutex_lock(obj->locks->mutex);
  obj->locks->thread = jvm->thread;
  //add USync to unwinding stack
  USync *usync = jfvm_alloc(jvm, sizeof(USync));
  jfvm_usync_add(jvm, usync, obj);
}

static void jfvm_swap_ustack(JVM *jvm) {
  UStack *u1 = jvm->ustack;
  UStack *u2 = u1->prev;
  UStack *u2_prev = u2->prev;
  u1->prev = u2_prev;
  u2->prev = u1;
  jvm->ustack = u2;
}

void jfvm_monitor_exit(JVM *jvm, Object *obj) {
  if (obj == NULL) jfvm_throw_npe(jvm);

  //sync{} blocks have an out-of-order catch block around the monitor_exit but not around the monitor_enter
  // which messes up the unwinding stack.  So need to swap the last two entries on the unwinding stack
  jfvm_swap_ustack(jvm);

  USync *usync = (USync*)jvm->ustack;
  jfvm_usync_remove(jvm);
  jfvm_free(jvm, usync);
  jfvm_arc_dec(jvm, obj);
  obj->locks->thread = NULL;
  jfvm_mutex_unlock(obj->locks->mutex);
}

void jfvm_usync_add(JVM *jvm, USync *usync, Object *obj) {
#ifdef JFVM_DEBUG_SYNC
  printf("    sync:%p:%p:%p\n", jvm->thread, usync, jvm->ustack);
#endif
  usync->type = USync_type;
  usync->prev = jvm->ustack;
  usync->obj = obj;
  jvm->ustack = (UStack*)usync;
}

void jfvm_usync_remove(JVM *jvm) {
  if (jvm->ustack->type != USync_type) {
    printf("Error:Unwinding stack corrupt:Looking for USync @ %p (thread=%p)\n", jvm->ustack, jvm->thread);
    jfvm_exit(1);
  }
  USync *usync = (USync*)jvm->ustack;
  usync->obj = NULL;  //flag as released
  jvm->ustack = usync->prev;
#ifdef JFVM_DEBUG_SYNC
  printf("-   sync:%p:%p:%p\n", jvm->thread, usync, jvm->ustack);
#endif
}

void jfvm_write_handle_ptr(JVM *jvm, Object *obj, void* data) {
  int fidx = jfvm_get_field_objidx(jvm, obj->cls, "handle");
  obj->fields[fidx] = data;
}

void* jfvm_read_handle_ptr(JVM *jvm, Object *obj) {
  int fidx = jfvm_get_field_objidx(jvm, obj->cls, "handle");
  return obj->fields[fidx];
}

void jfvm_write_handle_int(JVM *jvm, Object *obj, int data) {
  int fidx = jfvm_get_field_objidx(jvm, obj->cls, "handle");
  obj->ifields[fidx] = data;
}

int jfvm_read_handle_int(JVM *jvm, Object *obj) {
  int fidx = jfvm_get_field_objidx(jvm, obj->cls, "handle");
  return obj->ifields[fidx];
}

//these are NOT ARC safe.  Use only in constructors.

Object* jfvm_get_object(JVM *jvm, Object *obj, const char *field) {
  int fidx = jfvm_get_field_objidx(jvm, obj->cls, field);
  if (fidx == -1) {
    printf("Error:field not found:%s\n", field);
    return NULL;
  }
  return (Object*)obj->fields[fidx];
}

void jfvm_set_object(JVM *jvm, Object *obj, const char *field, Object *value) {
  int fidx = jfvm_get_field_objidx(jvm, obj->cls, field);
  if (fidx == -1) {
    printf("Error:field not found:%s\n", field);
    return;
  }
  obj->fields[fidx] = value;
}

void jfvm_get_field(JVM *jvm, Object *obj, const char *field, void *value, int size) {
  int fidx = jfvm_get_field_objidx(jvm, obj->cls, field);
  if (fidx == -1) {
    printf("Error:field not found:%s\n", field);
    return;
  }
  memcpy(value, &obj->fields[fidx], size);
}

void jfvm_set_field(JVM *jvm, Object *obj, const char *field, void *value, int size) {
  int fidx = jfvm_get_field_objidx(jvm, obj->cls, field);
  if (fidx == -1) {
    printf("Error:field not found:%s\n", field);
    return;
  }
  memcpy(&obj->fields[fidx], value, size);
}

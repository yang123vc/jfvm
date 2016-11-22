#include <jfvm.h>

//classpath

static Class ***class_pool = NULL;
static int class_pool_size = 0;
static void *class_pool_mutex = NULL;

void jfvm_init_class(JVM *jvm) {
  class_pool_mutex = jfvm_mutex_alloc();
}

/** Loads a DLL adds classes to classpath and returns DLL handle. */
void* jfvm_classpath_add(JVM *jvm, const char *fn) {
  void *lib = jfvm_lib_load(fn);
  if (lib == NULL) {
    printf("Error:Failed to load library:%s\n", fn);
    return NULL;
  }
  Class **(*get_classes)() = (Class** (*)())jfvm_lib_get_ptr(lib, "jfvm_get_classes");
  if (get_classes == NULL) {
    printf("Error:jfvm_get_classes is NULL\n");
    return NULL;
  }
  Class **classes = (*get_classes)();
  jfvm_register_classes(jvm, classes);
  return lib;
}

void jfvm_classpath_del(JVM *jvm, void *lib) {
  //TODO
}

void jfvm_register_classes(JVM *jvm, Class **classes) {
  jfvm_mutex_lock(class_pool_mutex);
  class_pool_size++;
  class_pool = (Class***)jfvm_realloc(jvm, class_pool, class_pool_size * sizeof(void*));
  class_pool[class_pool_size-1] = classes;
  while (jfvm_patch_classes(jvm, classes) != 0) {};
  jfvm_mutex_unlock(class_pool_mutex);
}

void jfvm_unregister_classes(JVM *jvm, Class **classes) {
  //TODO
}

int jfvm_patch_classes(JVM *jvm, Class **classes) {
  int cnt = 0;
  while (*classes != NULL) {
    Class *cls = *classes;
    if (cls->super_class == NULL && cls->super != NULL) {
      cls->super_class = jfvm_find_class(jvm, cls->super);
      if (cls->super_class != NULL) cnt++;
    }
    const char **interfaces = cls->interfaces;
    Class **interfaces_class = cls->interfaces_class;
    while (*interfaces != NULL) {
      if (*interfaces_class == NULL) {
        *interfaces_class = jfvm_find_class(jvm, *interfaces);
        if (*interfaces_class != NULL) cnt++;
      }
      interfaces++;
      interfaces_class++;
    }
    classes++;
  }
  return cnt;
}

Class *jfvm_find_class(JVM *jvm, const char *name) {
  Slot *stack;
  void (*clinit)(JVM *jvm, Slot *local);
  if (name == NULL) return NULL;
  for(int a=0;a<class_pool_size;a++) {
    Class **classes = class_pool[a];
    while (*classes != NULL) {
      Class* cls = *classes;
      if (strcmp(cls->name, name) == 0) {
        while (!__atomic_test_and_set(&cls->object_clinit_reflck, __ATOMIC_SEQ_CST)) {};
        if (cls->object_clinit != NULL) {
          stack = jfvm_stack_alloc(jvm, jfvm_get_static_method_local(jvm, cls, jfvm_get_method_clsidx(jvm, cls, "<clinit>()V")));
          clinit = cls->object_clinit;
          cls->object_clinit = NULL;
          //catch exceptions
          UCatch *ucatch = jfvm_ucatch_alloc(jvm, jfvm_find_class(jvm, "java/lang/Throwable"));  //TODO : cache this
          if (setjmp(ucatch->buf) == 0) {
            (*clinit)(jvm, stack);  //class static { ... }
          } else {
            //TODO : log exception
          }
          jfvm_ucatch_unwind(jvm);
          jfvm_stack_free(jvm, stack);
        }
        __atomic_clear(&cls->object_clinit_reflck, __ATOMIC_SEQ_CST);
        return cls;
      }
      classes++;
    }
  }
  printf("Warning:jfvm_find_class():can not find:%s\n", name);
  return NULL;
}

//class

jboolean jfvm_instanceof_class(JVM *jvm, const char *name, Class *src) {
  jboolean iof = J_FALSE;
  while (src != NULL) {
    if (strcmp(name, src->name) == 0) {
      iof = J_TRUE;
      break;
    }
    src = src->super_class;
  }
  return iof;
}

void jfvm_instanceof(JVM *jvm, const char *name, Slot *slot) {
  //replace obj with boolean (don't forget to arc_release obj first)
  jboolean iof = J_FALSE;
  Class *src = slot->obj->cls;
  while (src != NULL) {
    if (strcmp(name, src->name) == 0) {
      iof = J_TRUE;
      break;
    }
    src = src->super_class;
  }
  jfvm_arc_release(jvm, slot);
  slot->type = 'Z';
  slot->i8 = iof;
}

Object* jfvm_new(JVM *jvm, Class *cls) {
  Slot stack[1];
  int size = sizeof(Object) + cls->size * sizeof(void*);
  Object *obj = jfvm_alloc(jvm, size);
#ifdef JFVM_DEBUG_OBJECT
  printf("     new:%p:%s\n", obj, cls->name);
#endif
  obj->type = 'L';
  obj->refcnt = 1;
  obj->cls = cls;
  //init vtable
  stack[0].obj = obj;
  stack[0].type = 'L';
  (*cls->init_vtable)(jvm, stack);  //does NOT release stack
  return obj;
}

Object* jfvm_newarray(JVM *jvm, char type, int length) {
  Object *obj = jfvm_new(jvm, jfvm_find_class(jvm, "java/lang/Object"));
  obj->type = type;
  obj->refcnt = 1;
  Array *array = jfvm_alloc(jvm, sizeof(Array) + jfvm_get_type_size(type) * length);
#ifdef JFVM_DEBUG_OBJECT
  printf("newarray:%p:%p:%c:%d\n", obj, array, type, length);
#endif
  array->length = length;
  obj->array = array;
  switch (type) {
    case 'Z': obj->cls = jfvm_find_class(jvm, "java/lang/Boolean"); break;
    case 'B': obj->cls = jfvm_find_class(jvm, "java/lang/Byte"); break;
    case 'C': obj->cls = jfvm_find_class(jvm, "java/lang/Character"); break;
    case 'S': obj->cls = jfvm_find_class(jvm, "java/lang/Short"); break;
    case 'I': obj->cls = jfvm_find_class(jvm, "java/lang/Integer"); break;
    case 'J': obj->cls = jfvm_find_class(jvm, "java/lang/Long"); break;
    case 'F': obj->cls = jfvm_find_class(jvm, "java/lang/Float"); break;
    case 'D': obj->cls = jfvm_find_class(jvm, "java/lang/Double"); break;
  }
  return obj;
}

Object* jfvm_anewarray(JVM *jvm, const char *clsdesc, int length) {
  Object *obj = jfvm_new(jvm, jfvm_find_class(jvm, "java/lang/Object"));
#ifdef JFVM_DEBUG_OBJECT
  printf(" a new a:%p:%s:%d\n", obj, clsdesc, length);
#endif
  obj->type = 'A';
  obj->refcnt = 1;
  obj->cls = jfvm_find_class(jvm, clsdesc);
  Array *array = jfvm_alloc(jvm, sizeof(Array) + sizeof(Object*) * length);
  array->length = length;
  obj->array = array;
  return obj;
}

// Object x[][][] = new Object[2][3][4];
// int i[][][] = new int [5][6][7];
Object* jfvm_multianewarray(JVM *jvm, int numdims, const char *clsdesc, Slot *dims) {
  int length = dims->i32;
  Object *obj;
  char type;
  char clsname[512];
  int strlen;
  clsdesc++;
  type = *clsdesc;
  if (type == '[') {
    obj = jfvm_anewarray(jvm, "java/lang/Object", length);
  } else if (*clsdesc == 'L') {
    clsdesc++;
    strlen = strcspn(clsdesc, ";");
    memcpy(clsname, clsdesc, strlen);
    clsname[strlen] = 0;
    obj = jfvm_anewarray(jvm, clsname, length);
    return obj;
  } else {
    //primitive types
    obj = jfvm_newarray(jvm, type, length);
    return obj;
  }
  numdims--;
  obj->array->dims = numdims;
  if (numdims == 0) {
    return obj;
  }
  dims++;
  for(int a=0;a<length;a++) {
    obj->array->objs[a] = jfvm_multianewarray(jvm, numdims, clsdesc, dims);
  }
  return obj;
}

int jfvm_get_static_method_clsidx(JVM *jvm, Class *cls, const char *name_desc) {
  Method *method = cls->static_methods;
  int idx = 0;
  while (method->name != NULL) {
    if (strcmp(method->name_desc, name_desc) == 0) {
      return idx;
    }
    idx++;
    method++;
  }
  return -1;
}

int jfvm_get_method_clsidx(JVM *jvm, Class *cls, const char *name_desc) {
  Method *method;
  int idx = 0;
  method = cls->methods;
  while (method->name != NULL) {
    if (strcmp(method->name_desc, name_desc) == 0) {
      return idx;
    }
    idx++;
    method++;
  }
  return -1;
}

int jfvm_get_method_objidx(JVM *jvm, Class *cls, const char *name_desc) {
  Method *method;
  do {
    method = cls->methods;
    while (method->name != NULL) {
      if (strcmp(method->name_desc, name_desc) == 0) {
        return method->offset;
      }
      method++;
    }
    cls = cls->super_class;
  } while (cls != NULL);
  return -1;
}

int jfvm_get_method_local(JVM *jvm, Class *cls, int clsidx) {
  return cls->methods[clsidx].local;
}

int jfvm_get_static_method_local(JVM *jvm, Class *cls, int clsidx) {
  return cls->static_methods[clsidx].local;
}

int jfvm_get_static_field_clsidx(JVM *jvm, Class *cls, const char *name_desc) {
  Field *field = cls->static_fields;
  int idx = 0;
  while (field->name != NULL) {
    if (strcmp(field->name_desc, name_desc) == 0) {
      return idx;
    }
    idx++;
    field++;
  }
  return -1;
}

int jfvm_get_field_objidx(JVM *jvm, Class *cls, const char *name_desc) {
  Field *field;
  do {
    field = cls->fields;
    while (field->name != NULL) {
      if (strcmp(field->name_desc, name_desc) == 0) {
        return field->offset;
      }
      field++;
    }
    cls = cls->super_class;
  } while (cls != NULL);
  return -1;
}

void jfvm_checkcast_class(JVM *jvm, const char *cls, Slot *slot) {
  //TODO
}

void jfvm_checkcast_interface(JVM *jvm, const char *cls, Slot *slot) {
  //TODO
}

void jfvm_checkcast_array(JVM *jvm, const char *cls, Slot *slot) {
  //TODO
}

Object* jfvm_create_lambda(JVM *jvm, Class *cls, int objidx, void *method) {
  Object *obj = jfvm_new(jvm, cls);
  obj->methods[objidx] = method;
  return obj;
}

Object* jfvm_new_string(JVM *jvm, const char *str, int len) {
  int stackpos = -1;
  Class *strcls = jfvm_find_class(jvm, "java/lang/String");  //TODO : cache this
  Object *strobj;
  int clsidx = jfvm_get_method_clsidx(jvm, strcls, "<init>([B)V");  //TODO : cache this
  Slot *stack = jfvm_stack_alloc(jvm, jfvm_get_method_local(jvm, strcls, clsidx) + 1);  //+1 for dup

  //new String()
  stackpos++;
  stack[stackpos].obj = jfvm_new(jvm, strcls);
  stack[stackpos].type = 'L';
  //dup
  stackpos++;
  stack[stackpos].obj = stack[stackpos-1].obj;
  stack[stackpos].type = stack[stackpos-1].type;
  jfvm_arc_inc(jvm, stack[stackpos].obj);
  //new byte[]
  stackpos++;
  stack[stackpos].obj = jfvm_newarray(jvm,'B',len);
  stack[stackpos].type = 'L';
  memcpy(&stack[stackpos].obj->array->ai8[0],str,len);
  //String(byte[])
  jfvm_invokespecial(jvm, strcls, clsidx, &stack[stackpos-1]);
  stackpos -= 2;
  strobj = stack[stackpos].obj;
  jfvm_stack_free(jvm, stack);
  return strobj;
}

const char *jfvm_string_getbytes(JVM *jvm, Object *str) {
  int fidx = jfvm_get_field_objidx(jvm, str->cls, "value");
  Object *value = str->fields[fidx];
  jchar *ca = &value->array->ai16[0];
  int len = value->array->length;
  char *cstr = jfvm_alloc(jvm, len+1);
  for(int a=0;a<len;a++) {
    cstr[a] = ca[a];
  }
  return cstr;
}

void jfvm_string_releasebytes(JVM *jvm, const char *cstr) {
  jfvm_free(jvm, (void*)cstr);
}

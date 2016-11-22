#include <jfvm.h>

void java_java_lang_ClassLoader_add(JVM *jvm, Slot *local) {
  const char *cstr = jfvm_string_getbytes(jvm, local[0].obj);
  jfvm_arc_release(jvm, &local[0]);
  void *ref = jfvm_classpath_add(jvm, cstr);
  Object *ret = jfvm_newarray(jvm, 'B', sizeof(void*));
  ret->array->ptr[0] = ref;
  local[0].obj = ret;
  local[0].type = 'L';
}

void java_java_lang_ClassLoader_remove(JVM *jvm, Slot *local) {
  void* ref = local[0].obj->array->ptr[0];
  jfvm_classpath_del(jvm, ref);
  jfvm_arc_release(jvm, &local[0]);
}

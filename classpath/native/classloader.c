#include <jfvm.h>

void java_java_lang_ClassLoader_add(JVM *jvm, Slot *args) {
  const char *cstr = jfvm_string_get_utf8(jvm, args[0].obj);
  jfvm_arc_release(jvm, &args[0]);
  void *ref = jfvm_classpath_add(jvm, cstr);
  jfvm_string_release_utf8(jvm, cstr);
  Object *ret = jfvm_newarray(jvm, 'B', sizeof(void*));
  ret->array->ptr[0] = ref;
  args[0].obj = ret;
  args[0].type = 'L';
}

void java_java_lang_ClassLoader_remove(JVM *jvm, Slot *args) {
  void* ref = args[0].obj->array->ptr[0];
  jfvm_classpath_del(jvm, ref);
  jfvm_arc_release(jvm, &args[0]);
}

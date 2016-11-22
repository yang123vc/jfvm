#include <stdio.h>

void java_java_lang_System_getStdIn(JVM *jvm, Slot *local) {
  Object *obj = jfvm_new(jvm, jfvm_find_class(jvm, "java/io/FileInputStream"));
  jfvm_write_handle_int(jvm, obj, 0);
  local[0].obj = obj;
  local[0].type = 'L';
}

void java_java_lang_System_getStdOut(JVM *jvm, Slot *local) {
  Object *obj = jfvm_new(jvm, jfvm_find_class(jvm, "java/io/FileOutputStream"));
  jfvm_write_handle_int(jvm, obj, 1);
  local[0].obj = obj;
  local[0].type = 'L';
}

void java_java_lang_System_getStdErr(JVM *jvm, Slot *local) {
  Object *obj = jfvm_new(jvm, jfvm_find_class(jvm, "java/io/FileOutputStream"));
  jfvm_write_handle_int(jvm, obj, 2);
  local[0].obj = obj;
  local[0].type = 'L';
}

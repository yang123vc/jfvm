#include <stdio.h>

void java_java_lang_System_getStdIn(JVM *jvm, Slot *args) {
  Object *obj = jfvm_new(jvm, jfvm_find_class(jvm, "java/io/FileInputStream"));
  jfvm_write_handle_int(jvm, obj, 0);
  args[0].obj = obj;
  args[0].type = 'L';
}

void java_java_lang_System_getStdOut(JVM *jvm, Slot *args) {
  Object *obj = jfvm_new(jvm, jfvm_find_class(jvm, "java/io/FileOutputStream"));
  jfvm_write_handle_int(jvm, obj, 1);
  args[0].obj = obj;
  args[0].type = 'L';
}

void java_java_lang_System_getStdErr(JVM *jvm, Slot *args) {
  Object *obj = jfvm_new(jvm, jfvm_find_class(jvm, "java/io/FileOutputStream"));
  jfvm_write_handle_int(jvm, obj, 2);
  args[0].obj = obj;
  args[0].type = 'L';
}

void java_java_lang_System_getUserHome(JVM *jvm, Slot *args) {
  //TODO
  args[0].obj = jfvm_new_string_utf8(jvm, "TODO", 4);
  args[0].type = 'L';
}

void java_java_lang_System_getUserName(JVM *jvm, Slot *args) {
  //TODO
  args[0].obj = jfvm_new_string_utf8(jvm, "TODO", 4);
  args[0].type = 'L';
}

#include <windows.h>

void java_java_lang_System_getStdIn(JVM *jvm, Slot *local) {
  Object *obj = jfvm_new(jvm, jfvm_find_class(jvm, "java/io/FileInputStream"));
  HANDLE handle = GetStdHandle(STD_INPUT_HANDLE);
  jfvm_write_handle_ptr(jvm, obj, handle);
  local[0].obj = obj;
  local[0].type = 'L';
}

void java_java_lang_System_getStdOut(JVM *jvm, Slot *local) {
  Object *obj = jfvm_new(jvm, jfvm_find_class(jvm, "java/io/FileOutputStream"));
  HANDLE handle = GetStdHandle(STD_OUTPUT_HANDLE);
  jfvm_write_handle_ptr(jvm, obj, handle);
  local[0].obj = obj;
  local[0].type = 'L';
}

void java_java_lang_System_getStdErr(JVM *jvm, Slot *local) {
  Object *obj = jfvm_new(jvm, jfvm_find_class(jvm, "java/io/FileOutputStream"));
  HANDLE handle = GetStdHandle(STD_ERROR_HANDLE);
  jfvm_write_handle_ptr(jvm, obj, handle);
  local[0].obj = obj;
  local[0].type = 'L';
}

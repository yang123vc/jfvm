#include <windows.h>

//input stream

void java_java_io_FileInputStream_open(JVM *jvm, Slot *local) {
  const char *str = jfvm_string_getbytes(jvm, local);
  HANDLE handle = CreateFile(str, GENERIC_READ, 0, NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
  jfvm_string_releasebytes(jvm, str);
  if (handle == INVALID_HANDLE_VALUE) {
    jfvm_throw_ioexception(jvm);
  }
  jfvm_write_handle_ptr(jvm, local[0].obj, handle);
  jfvm_arc_release(jvm, &local[0]);
}

void java_java_io_FileInputStream_close(JVM *jvm, Slot *local) {
  HANDLE handle = (HANDLE)jfvm_read_handle_ptr(jvm, local[0].obj);
  if (handle == INVALID_HANDLE_VALUE) return;
  CloseHandle(handle);
  jfvm_write_handle_ptr(jvm, local[0].obj, INVALID_HANDLE_VALUE);
}

void java_java_io_FileInputStream_read_(JVM *jvm, Slot *local) {
  HANDLE handle = (HANDLE)jfvm_read_handle_ptr(jvm, local[0].obj);
  int ch = 0;
  int read = 1;
  ReadFile(handle, &ch, 1, &read, NULL);
  jfvm_arc_release(jvm, &local[0]);
  local[0].i32 = ch;
  local[0].type = 'I';
}

void java_java_io_FileInputStream_read_AB(JVM *jvm, Slot *local) {
  HANDLE handle = (HANDLE)jfvm_read_handle_ptr(jvm, local[0].obj);
  int read = local[1].obj->array->length;
  ReadFile(handle, &local[1].obj->array->ai8, read, &read, NULL);
  jfvm_arc_release(jvm, &local[0]);
  jfvm_arc_release(jvm, &local[1]);
  local[0].i32 = read;
  local[0].type = 'I';
}

//output stream

void java_java_io_FileOutputStream_open(JVM *jvm, Slot *local) {
  const char *str = jfvm_string_getbytes(jvm, local);
  HANDLE handle = CreateFile(str, GENERIC_WRITE, 0, NULL, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
  jfvm_string_releasebytes(jvm, str);
  if (handle == INVALID_HANDLE_VALUE) {
    jfvm_throw_ioexception(jvm);
  }
  jfvm_write_handle_ptr(jvm, local[0].obj, handle);
  jfvm_arc_release(jvm, &local[0]);
}

void java_java_io_FileOutputStream_close(JVM *jvm, Slot *local) {
  HANDLE handle = (HANDLE)jfvm_read_handle_ptr(jvm, local[0].obj);
  if (handle == INVALID_HANDLE_VALUE) return;
  CloseHandle(handle);
  jfvm_write_handle_ptr(jvm, local[0].obj, INVALID_HANDLE_VALUE);
  jfvm_arc_release(jvm, &local[0]);
}

void java_java_io_FileOutputStream_write_B(JVM *jvm, Slot *local) {
  HANDLE handle = (HANDLE)jfvm_read_handle_ptr(jvm, local[0].obj);
  int ch = local[1].i32;
  int write = 1;
  WriteFile(handle, &ch, 1, &write, NULL);
  jfvm_arc_release(jvm, &local[0]);
  local[0].i32 = ch;
  local[0].type = 'I';
}

void java_java_io_FileOutputStream_write_AB(JVM *jvm, Slot *local) {
  HANDLE handle = (HANDLE)jfvm_read_handle_ptr(jvm, local[0].obj);
  int write = local[1].obj->array->length;
  WriteFile(handle, &local[1].obj->array->ai8, write, &write, NULL);
  jfvm_arc_release(jvm, &local[0]);
  jfvm_arc_release(jvm, &local[1]);
  local[0].i32 = write;
  local[0].type = 'I';
}

//random

//TODO


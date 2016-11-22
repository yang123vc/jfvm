#include <stdio.h>
#include <fcntl.h>
#include <unistd.h>

//input stream

void java_java_io_FileInputStream_open(JVM *jvm, Slot *local) {
  const char *str = jfvm_string_getbytes(jvm, local);
  int handle = open(str, O_RDONLY);
  jfvm_string_releasebytes(jvm, str);
  if (handle == -1) {
    jfvm_throw_ioexception(jvm);
  }
  jfvm_write_handle_int(jvm, local[0].obj, handle);
  jfvm_arc_release(jvm, &local[0]);
}

void java_java_io_FileInputStream_close(JVM *jvm, Slot *local) {
  int handle = jfvm_read_handle_int(jvm, local[0].obj);
  if (handle == -1) return;
  close(handle);
  jfvm_write_handle_int(jvm, local[0].obj, -1);
}

void java_java_io_FileInputStream_read_(JVM *jvm, Slot *local) {
  int handle = jfvm_read_handle_int(jvm, local[0].obj);
  int ch = 0;
  int readed = read(handle, &ch, 1);
  jfvm_arc_release(jvm, &local[0]);
  if (readed == -1) ch = -1;
  local[0].i32 = ch;
  local[0].type = 'I';
}

void java_java_io_FileInputStream_read_AB(JVM *jvm, Slot *local) {
  int handle = jfvm_read_handle_int(jvm, local[0].obj);
  int toread = local[1].obj->array->length;
  int readed = read(handle, &local[1].obj->array->ai8, toread);
  jfvm_arc_release(jvm, &local[0]);
  jfvm_arc_release(jvm, &local[1]);
  local[0].i32 = readed;
  local[0].type = 'I';
}

//output stream

void java_java_io_FileOutputStream_open(JVM *jvm, Slot *local) {
  const char *str = jfvm_string_getbytes(jvm, local);
  int handle = open(str, O_WRONLY | O_CREAT | O_TRUNC);
  jfvm_string_releasebytes(jvm, str);
  if (handle == -1) {
    jfvm_throw_ioexception(jvm);
  }
  jfvm_write_handle_int(jvm, local[0].obj, handle);
  jfvm_arc_release(jvm, &local[0]);
}

void java_java_io_FileOutputStream_close(JVM *jvm, Slot *local) {
  int handle = jfvm_read_handle_int(jvm, local[0].obj);
  if (handle == -1) return;
  close(handle);
  jfvm_write_handle_int(jvm, local[0].obj, -1);
  jfvm_arc_release(jvm, &local[0]);
}

void java_java_io_FileOutputStream_write_B(JVM *jvm, Slot *local) {
  int handle = jfvm_read_handle_int(jvm, local[0].obj);
  int ch = local[1].i32;
  int writen = write(handle, &ch, 1);
  jfvm_arc_release(jvm, &local[0]);
  local[0].i32 = ch;
  local[0].type = 'I';
}

void java_java_io_FileOutputStream_write_AB(JVM *jvm, Slot *local) {
  int handle = jfvm_read_handle_int(jvm, local[0].obj);
  int towrite = local[1].obj->array->length;
  int writen = write(handle, &local[1].obj->array->ai8, towrite);
  jfvm_arc_release(jvm, &local[0]);
  jfvm_arc_release(jvm, &local[1]);
  local[0].i32 = writen;
  local[0].type = 'I';
}

//random

//TODO


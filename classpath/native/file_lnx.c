#include <stdio.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>

//input stream

void java_java_io_FileInputStream_open(JVM *jvm, Slot *args) {
  const char *str = jfvm_string_get_utf8(jvm, args);
  int handle = open(str, O_RDONLY);
  jfvm_string_release_utf8(jvm, str);
  if (handle == -1) {
    jfvm_stack_release(jvm, args, 2);
    jfvm_throw_ioexception(jvm);
  }
  jfvm_write_handle_int(jvm, args[0].obj, handle);
  jfvm_stack_release(jvm, args, 2);
}

void java_java_io_FileInputStream_close(JVM *jvm, Slot *args) {
  int handle = jfvm_read_handle_int(jvm, args[0].obj);
  if (handle == -1) return;
  close(handle);
  jfvm_write_handle_int(jvm, args[0].obj, -1);
  jfvm_arc_release(jvm, &args[0]);
}

void java_java_io_FileInputStream_read_(JVM *jvm, Slot *args) {
  int handle = jfvm_read_handle_int(jvm, args[0].obj);
  int ch = 0;
  int readed = read(handle, &ch, 1);
  jfvm_arc_release(jvm, &args[0]);
  if (readed == -1) ch = -1;
  args[0].i32 = ch;
  args[0].type = 'I';
}

void java_java_io_FileInputStream_read_ABII(JVM *jvm, Slot *args) {
  int handle = jfvm_read_handle_int(jvm, args[0].obj);
  int toread = args[3].i32; //args[1].obj->array->length;
  int readed = read(handle, &args[1].obj->array->ai8 + args[2].i32, toread);
  jfvm_stack_release(jvm, args, 2);
  args[0].i32 = readed;
  args[0].type = 'I';
}

void java_java_io_FileInputStream_available(JVM *jvm, Slot *args) {
  struct stat fs;
  int handle = jfvm_read_handle_int(jvm, args[0].obj);
  fstat(handle, &fs);
  jfvm_stack_release(jvm, args, 1);
  args[0].i32 = fs.st_size;
  args[0].type = 'I';
}

//output stream

void java_java_io_FileOutputStream_open(JVM *jvm, Slot *args) {
  const char *str = jfvm_string_get_utf8(jvm, args);
  int handle = open(str, O_WRONLY | O_CREAT | O_TRUNC);
  jfvm_string_release_utf8(jvm, str);
  if (handle == -1) {
    jfvm_stack_release(jvm, args, 2);
    jfvm_throw_ioexception(jvm);
  }
  jfvm_write_handle_int(jvm, args[0].obj, handle);
  jfvm_stack_release(jvm, args, 2);
}

void java_java_io_FileOutputStream_close(JVM *jvm, Slot *args) {
  int handle = jfvm_read_handle_int(jvm, args[0].obj);
  if (handle == -1) return;
  close(handle);
  jfvm_write_handle_int(jvm, args[0].obj, -1);
  jfvm_arc_release(jvm, &args[0]);
}

void java_java_io_FileOutputStream_write_B(JVM *jvm, Slot *args) {
  int handle = jfvm_read_handle_int(jvm, args[0].obj);
  int ch = args[1].i32;
  write(handle, &ch, 1);
  jfvm_stack_release(jvm, args, 2);
}

void java_java_io_FileOutputStream_write_ABII(JVM *jvm, Slot *args) {
  int handle = jfvm_read_handle_int(jvm, args[0].obj);
  int towrite = args[3].i32;  //args[1].obj->array->length;
  int writen = write(handle, &args[1].obj->array->ai8 + args[2].i32, towrite);
  jfvm_arc_release(jvm, &args[0]);
  jfvm_arc_release(jvm, &args[1]);
  args[0].i32 = writen;
  args[0].type = 'I';
}

//random

void java_java_io_RandomAccessFile_open(JVM *jvm, Slot *args) {
  const char *str = jfvm_string_get_utf8(jvm, args[1].obj);
  const char *mode = jfvm_string_get_utf8(jvm, args[2].obj);
  int handle;
  if (strchr(mode, 'w') == NULL)
    handle = open(str, O_RDONLY);
  else
    handle = open(str, O_WRONLY | O_CREAT | O_TRUNC);
  jfvm_string_release_utf8(jvm, str);
  jfvm_string_release_utf8(jvm, mode);
  if (handle == -1) {
    jfvm_stack_release(jvm, args, 3);
    jfvm_throw_ioexception(jvm);
  }
  jfvm_write_handle_int(jvm, args[0].obj, handle);
  jfvm_stack_release(jvm, args, 3);
}

void java_java_io_RandomAccessFile_close(JVM *jvm, Slot *args) {
  int handle = jfvm_read_handle_int(jvm, args[0].obj);
  if (handle == -1) return;
  close(handle);
  jfvm_write_handle_int(jvm, args[0].obj, -1);
  jfvm_arc_release(jvm, &args[0]);
}

void java_java_io_RandomAccessFile_read_ABII(JVM *jvm, Slot *args) {
  int handle = jfvm_read_handle_int(jvm, args[0].obj);
  int toread = args[3].i32; //args[1].obj->array->length;
  int readed = read(handle, &args[1].obj->array->ai8 + args[2].i32, toread);
  jfvm_stack_release(jvm, args, 2);
  args[0].i32 = readed;
  args[0].type = 'I';
}

void java_java_io_RandomAccessFile_write_ABII(JVM *jvm, Slot *args) {
  int handle = jfvm_read_handle_int(jvm, args[0].obj);
  int towrite = args[3].i32;  //args[1].obj->array->length;
  int writen = write(handle, &args[1].obj->array->ai8 + args[2].i32, towrite);
  jfvm_arc_release(jvm, &args[0]);
  jfvm_arc_release(jvm, &args[1]);
  args[0].i32 = writen;
  args[0].type = 'I';
}

void java_java_io_RandomAccessFile_getFilePointer(JVM *jvm, Slot *args) {
  int handle = jfvm_read_handle_int(jvm, args[0].obj);
  int low = lseek(handle, 0, SEEK_CUR);
  jfvm_arc_release(jvm, &args[0]);
  args[0].i32low = low;
  args[0].i32high = 0;
  args[0].type = 'J';
}

void java_java_io_RandomAccessFile_seek(JVM *jvm, Slot *args) {
  int handle = jfvm_read_handle_int(jvm, args[0].obj);
  lseek(handle, args[1].i64, SEEK_SET);
  jfvm_arc_release(jvm, &args[0]);
}

void java_java_io_RandomAccessFile_length(JVM *jvm, Slot *args) {
  struct stat fs;
  int handle = jfvm_read_handle_int(jvm, args[0].obj);
  fstat(handle, &fs);
  jfvm_stack_release(jvm, args, 1);
  args[0].i64 = fs.st_size;
  args[0].type = 'I';
}

//File

extern void java_java_io_File_getPath(JVM *jvm, Slot *args);

static const char *File_getPath(JVM *jvm, Slot *args) {
  java_java_io_File_getPath(jvm, args);
  const char *str = jfvm_string_get_utf8(args[0].obj);
  jfvm_arc_release(jvm, &args[0]);
  return str;
}

static void File_releasePath(JVM *jvm, const char *str) {
  jfvm_string_release_utf8(jvm, str);
}

void java_java_io_File_getPathSeparator(JVM *jvm, Slot *args) {
  args[0].i32 = ':';
  args[0].type = 'C';
}

void java_java_io_File_getSeparator(JVM *jvm, Slot *args) {
  args[0].i32 = '/';
  args[0].type = 'C';
}

void java_java_io_File_chdir(JVM *jvm, Slot *args) {
  //TODO
  jfvm_arc_release(jvm, &args[0]);
}

void java_java_io_File_mkdir(JVM *jvm, Slot *args) {
  //TODO
  jfvm_arc_release(jvm, &args[0]);
}

void java_java_io_File_exists(JVM *jvm, Slot *args) {
  //TODO
  jfvm_arc_release(jvm, &args[0]);
}

void java_java_io_File_lastModified(JVM *jvm, Slot *args) {
  //TODO
  jfvm_arc_release(jvm, &args[0]);
}

#include <windows.h>

//input stream

void java_java_io_FileInputStream_open(JVM *jvm, Slot *args) {
  const char *str = jfvm_string_get_utf8(jvm, args[1].obj);
  HANDLE handle = CreateFile(str, GENERIC_READ, 0, NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
  jfvm_string_release_utf8(jvm, str);
  if (handle == INVALID_HANDLE_VALUE) {
    jfvm_stack_release(jvm, args, 2);
    jfvm_throw_ioexception(jvm);
  }
  jfvm_write_handle_ptr(jvm, args[0].obj, handle);
  jfvm_stack_release(jvm, args, 2);
}

void java_java_io_FileInputStream_close(JVM *jvm, Slot *args) {
  HANDLE handle = (HANDLE)jfvm_read_handle_ptr(jvm, args[0].obj);
  if (handle == INVALID_HANDLE_VALUE) {
    jfvm_arc_release(jvm, &args[0]);
    return;
  }
  CloseHandle(handle);
  jfvm_write_handle_ptr(jvm, args[0].obj, INVALID_HANDLE_VALUE);
  jfvm_arc_release(jvm, &args[0]);
}

void java_java_io_FileInputStream_read_(JVM *jvm, Slot *args) {
  HANDLE handle = (HANDLE)jfvm_read_handle_ptr(jvm, args[0].obj);
  int ch = 0;
  int read = 1;
  ReadFile(handle, &ch, 1, &read, NULL);
  jfvm_arc_release(jvm, &args[0]);
  args[0].i32 = ch;
  args[0].type = 'I';
}

void java_java_io_FileInputStream_read_ABII(JVM *jvm, Slot *args) {
  HANDLE handle = (HANDLE)jfvm_read_handle_ptr(jvm, args[0].obj);
  int read = args[3].i32;
  ReadFile(handle, &args[1].obj->array->ai8 + args[2].i32, read, &read, NULL);
  jfvm_stack_release(jvm, args, 2);
  args[0].i32 = read;
  args[0].type = 'I';
}

void java_java_io_FileInputStream_available(JVM *jvm, Slot *args) {
  HANDLE handle = (HANDLE)jfvm_read_handle_ptr(jvm, args[0].obj);
  int high;
  int low = GetFileSize(handle, &high);
  jfvm_arc_release(jvm, &args[0]);
  args[0].i32 = low;
  args[0].type = 'I';
}

//output stream

void java_java_io_FileOutputStream_open(JVM *jvm, Slot *args) {
  const char *str = jfvm_string_get_utf8(jvm, args[1].obj);
  HANDLE handle = CreateFile(str, GENERIC_WRITE, 0, NULL, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
  jfvm_string_release_utf8(jvm, str);
  if (handle == INVALID_HANDLE_VALUE) {
    jfvm_stack_release(jvm, args, 2);
    jfvm_throw_ioexception(jvm);
  }
  jfvm_write_handle_ptr(jvm, args[0].obj, handle);
  jfvm_stack_release(jvm, args, 2);
}

void java_java_io_FileOutputStream_close(JVM *jvm, Slot *args) {
  HANDLE handle = (HANDLE)jfvm_read_handle_ptr(jvm, args[0].obj);
  if (handle == INVALID_HANDLE_VALUE) {
    jfvm_arc_release(jvm, &args[0]);
    return;
  }
  CloseHandle(handle);
  jfvm_write_handle_ptr(jvm, args[0].obj, INVALID_HANDLE_VALUE);
  jfvm_arc_release(jvm, &args[0]);
}

void java_java_io_FileOutputStream_write_B(JVM *jvm, Slot *args) {
  HANDLE handle = (HANDLE)jfvm_read_handle_ptr(jvm, args[0].obj);
  int ch = args[1].i32;
  int write = 1;
  WriteFile(handle, &ch, 1, &write, NULL);
  jfvm_arc_release(jvm, &args[0]);
}

void java_java_io_FileOutputStream_write_ABII(JVM *jvm, Slot *args) {
  HANDLE handle = (HANDLE)jfvm_read_handle_ptr(jvm, args[0].obj);
  int write = args[3].i32;
  WriteFile(handle, &args[1].obj->array->ai8 + args[2].i32, write, &write, NULL);
  jfvm_stack_release(jvm, args, 2);
  args[0].i32 = write;
  args[0].type = 'I';
}

//random

void java_java_io_RandomAccessFile_open(JVM *jvm, Slot *args) {
  const char *str = jfvm_string_get_utf8(jvm, args[1].obj);
  const char *mode = jfvm_string_get_utf8(jvm, args[2].obj);
  HANDLE handle;
  if (strchr(mode, 'w') == NULL)
    handle = CreateFile(str, GENERIC_READ, 0, NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
  else
    handle = CreateFile(str, GENERIC_WRITE, 0, NULL, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
  jfvm_string_release_utf8(jvm, str);
  jfvm_string_release_utf8(jvm, mode);
  if (handle == INVALID_HANDLE_VALUE) {
    jfvm_stack_release(jvm, args, 3);
    jfvm_throw_ioexception(jvm);
  }
  jfvm_write_handle_ptr(jvm, args[0].obj, handle);
  jfvm_stack_release(jvm, args, 3);
}

void java_java_io_RandomAccessFile_close(JVM *jvm, Slot *args) {
  HANDLE handle = (HANDLE)jfvm_read_handle_ptr(jvm, args[0].obj);
  if (handle == INVALID_HANDLE_VALUE) {
    jfvm_arc_release(jvm, &args[0]);
    return;
  }
  CloseHandle(handle);
  jfvm_write_handle_ptr(jvm, args[0].obj, INVALID_HANDLE_VALUE);
  jfvm_arc_release(jvm, &args[0]);
}

void java_java_io_RandomAccessFile_read_ABII(JVM *jvm, Slot *args) {
  HANDLE handle = (HANDLE)jfvm_read_handle_ptr(jvm, args[0].obj);
  int read = args[3].i32;
  ReadFile(handle, &args[1].obj->array->ai8 + args[2].i32, read, &read, NULL);
  jfvm_stack_release(jvm, args, 2);
  args[0].i32 = read;
  args[0].type = 'I';
}

void java_java_io_RandomAccessFile_write_ABII(JVM *jvm, Slot *args) {
  HANDLE handle = (HANDLE)jfvm_read_handle_ptr(jvm, args[0].obj);
  int write = args[3].i32;
  WriteFile(handle, &args[1].obj->array->ai8 + args[2].i32, write, &write, NULL);
  jfvm_stack_release(jvm, args, 2);
  args[0].i32 = write;
  args[0].type = 'I';
}

void java_java_io_RandomAccessFile_getFilePointer(JVM *jvm, Slot *args) {
  HANDLE handle = (HANDLE)jfvm_read_handle_ptr(jvm, args[0].obj);
  int high = 0;
  int low = SetFilePointer(handle, 0, &high, FILE_CURRENT);
  jfvm_arc_release(jvm, &args[0]);
  args[0].i32low = low;
  args[0].i32high = high;
  args[0].type = 'J';
}

void java_java_io_RandomAccessFile_seek(JVM *jvm, Slot *args) {
  HANDLE handle = (HANDLE)jfvm_read_handle_ptr(jvm, args[0].obj);
  int high = args[1].i32high;
  SetFilePointer(handle, args[1].i32low, &high, FILE_BEGIN);
  jfvm_arc_release(jvm, &args[0]);
}

void java_java_io_RandomAccessFile_length(JVM *jvm, Slot *args) {
  HANDLE handle = (HANDLE)jfvm_read_handle_ptr(jvm, args[0].obj);
  int high;
  int low = GetFileSize(handle, &high);
  jfvm_arc_release(jvm, &args[0]);
  jlong length = high;
  length <<= 32;
  length += low;
  args[0].i64 = length;
  args[0].type = 'I';
}

//File

extern void java_java_io_File_getPath(JVM *jvm, Slot *args);

static const char *File_getPath(JVM *jvm, Slot *args) {
  java_java_io_File_getPath(jvm, args);
  const char *str = jfvm_string_get_utf8(jvm, args[0].obj);
  jfvm_arc_release(jvm, &args[0]);
  return str;
}

static void File_releasePath(JVM *jvm, const char *str) {
  jfvm_string_release_utf8(jvm, str);
}

void java_java_io_File_getPathSeparator(JVM *jvm, Slot *args) {
  args[0].i32 = ';';
  args[0].type = 'C';
}

void java_java_io_File_getSeparator(JVM *jvm, Slot *args) {
  args[0].i32 = '\\';
  args[0].type = 'C';
}

void java_java_io_File_chdir(JVM *jvm, Slot *args) {
  const char *str = File_getPath(jvm, args);
  SetCurrentDirectory(str);
  File_releasePath(jvm, str);
  char cd[256];
  GetCurrentDirectory(256, cd);
  //TODO : System.setProperty("user.dir", cd);
}

void java_java_io_File_mkdir(JVM *jvm, Slot *args) {
  const char *str = File_getPath(jvm, args);
  CreateDirectory(str, NULL);
  File_releasePath(jvm, str);
}

void java_java_io_File_exists(JVM *jvm, Slot *args) {
  const char *str = File_getPath(jvm, args);
  HANDLE handle = CreateFile(str, GENERIC_READ, 0, NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
  File_releasePath(jvm, str);
  if (handle == INVALID_HANDLE_VALUE) {
    args[0].i32 = 0;
    args[0].i32 = 'Z';
  } else {
    CloseHandle(handle);
    args[0].i32 = 1;
    args[0].i32 = 'Z';
  }
}

void java_java_io_File_lastModified(JVM *jvm, Slot *args) {
  const char *str = File_getPath(jvm, args);
  HANDLE handle = CreateFile(str, GENERIC_READ, 0, NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
  File_releasePath(jvm, str);
  if (handle == INVALID_HANDLE_VALUE) {
    args[0].i64 = -1L;
    args[0].type = 'J';
    return;
  }
  union {
    FILETIME win_epoch;  //100 nanoseconds from Jan 1, 1601
    jlong i64;
  } u;
  GetFileTime(handle, NULL, NULL, &u.win_epoch);
  CloseHandle(handle);
  //convert epoch(Jan 1, 1601) to epoch(Jan 1, 1970)
  jlong time = u.i64 / 10000L;  //convert 100nano to 1ms
  time -= 11636784000000L;  //369 * 365 * 24 * 60 * 60 * 1000;  //minus 369 years
  time -= 7948800000L;  //92 * 24 * 60 * 60 * 1000;  //minus 92 leap days
  args[0].i64 = time;
  args[0].type = 'J';
}

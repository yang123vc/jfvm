#include <jfvm.h>

#include "zlib/zlib.h"

#define MEM_LEVEL 8

#define EXTRA_SIZE 128  //in case output gets larger (rare)

void java_java_util_zip_ZipUtils_compress(JVM *jvm, Slot *args) {
  //args[0].obj = byte array
  int inSize = args[0].obj->array->length;
  void *in = &args[0].obj->array->ai8;
  void *out = jfvm_alloc(jvm, inSize + EXTRA_SIZE);
  z_stream z;
  z.zalloc = Z_NULL;
  z.zfree = Z_NULL;
  z.opaque = Z_NULL;
  if (deflateInit2(&z, Z_BEST_COMPRESSION, Z_DEFLATED, -MAX_WBITS, MEM_LEVEL, Z_DEFAULT_STRATEGY) != Z_OK) {
    jfvm_arc_release(jvm, &args[0]);
    jfvm_free(jvm, out);
    args[0].obj = NULL;
    args[0].type = 'L';
    return;
  }
  z.next_in = (Bytef*)in;
  z.avail_in = inSize;
  z.next_out = (Bytef*)out;
  z.avail_out = inSize + EXTRA_SIZE;
  int ret = deflate(&z, Z_FINISH);
  deflateEnd(&z);
  if (ret < 0) {
    jfvm_arc_release(jvm, &args[0]);
    jfvm_free(jvm, out);
    args[0].obj = NULL;
    args[0].type = 'L';
    return;
  }
  int outSize = (inSize + EXTRA_SIZE) - z.avail_out;
  jfvm_arc_release(jvm, &args[0]);
  Object *array = jfvm_newarray(jvm, 'B', outSize);
  memcpy(&array->array->ai8, out, outSize);
  jfvm_free(jvm, out);
  args[0].obj = array;
  args[0].type = 'L';
}

void java_java_util_zip_ZipUtils_decompress(JVM *jvm, Slot *args) {
  //args[0].obj = byte array
  //args[1].i32 = outSize
  int deflateSize = args[0].obj->array->length;
  int outSize = args[1].i32;
  void *out = jfvm_alloc(jvm, outSize);
  z_stream z;
  z.zalloc = Z_NULL;
  z.zfree = Z_NULL;
  z.opaque = Z_NULL;
  z.next_in = (Bytef*)&args[0].obj->array->ai8;
  z.avail_in = deflateSize;
  if (inflateInit2(&z, -MAX_WBITS) != Z_OK) {
    jfvm_arc_release(jvm, &args[0]);
    jfvm_free(jvm, out);
    args[0].obj = NULL;
    args[0].type = 'L';
    return;
  }
  z.next_out = (Bytef*)out;
  z.avail_out = outSize;
  int ret = inflate(&z, Z_FINISH);
  inflateEnd(&z);
  if (ret < 0) {
    jfvm_arc_release(jvm, &args[0]);
    jfvm_free(jvm, out);
    args[0].obj = NULL;
    args[0].type = 'L';
    return;
  }
  jfvm_arc_release(jvm, &args[0]);
  Object *array = jfvm_newarray(jvm, 'B', outSize);
  memcpy(&array->array->ai8, out, outSize);
  jfvm_free(jvm, out);
  args[0].obj = array;
  args[0].type = 'L';
}

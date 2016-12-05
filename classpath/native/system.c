#include <jfvm.h>

#ifdef JFVM_WIN
  #include "system_win.c"
#endif

#ifdef JFVM_LNX
  #include "system_lnx.c"
#endif

void java_java_lang_System_getenv(JVM *jvm, Slot *args) {
  //TODO
  jfvm_arc_release(jvm, &args[0]);
}

void java_java_lang_System_exit(JVM *jvm, Slot *args) {
  jfvm_exit(args[0].i32);
}

void java_java_lang_System_debug(JVM *jvm, Slot *args) {
  __asm("int $3");
}

static void copy_bytes(JVM *jvm, Object *src, int srcPos, Object *dest, int destPos, int length) {
  jbyte* srcData = src->array->ai8;
  jbyte* destData = dest->array->ai8;
  int srcLength = src->array->length;
  int destLength = dest->array->length;
  while (length > 0) {
    if (srcPos >= srcLength) {
      jfvm_throw_outofbounds(jvm, src, srcPos);
    }
    if (destPos >= destLength) {
      jfvm_throw_outofbounds(jvm, dest, destPos);
    }
    destData[destPos++] = srcData[srcPos++];
    length--;
  }
}

static void copy_shorts(JVM *jvm, Object *src, int srcPos, Object *dest, int destPos, int length) {
  jshort* srcData = src->array->ai16;
  jshort* destData = dest->array->ai16;
  int srcLength = src->array->length;
  int destLength = dest->array->length;
  while (length > 0) {
    if (srcPos >= srcLength) {
      jfvm_throw_outofbounds(jvm, src, srcPos);
    }
    if (destPos >= destLength) {
      jfvm_throw_outofbounds(jvm, dest, destPos);
    }
    destData[destPos++] = srcData[srcPos++];
    length--;
  }
}

static void copy_ints(JVM *jvm, Object *src, int srcPos, Object *dest, int destPos, int length) {
  jint* srcData = src->array->ai32;
  jint* destData = dest->array->ai32;
  int srcLength = src->array->length;
  int destLength = dest->array->length;
  while (length > 0) {
    if (srcPos >= srcLength) {
      jfvm_throw_outofbounds(jvm, src, srcPos);
    }
    if (destPos >= destLength) {
      jfvm_throw_outofbounds(jvm, dest, destPos);
    }
    destData[destPos++] = srcData[srcPos++];
    length--;
  }
}

static void copy_longs(JVM *jvm, Object *src, int srcPos, Object *dest, int destPos, int length) {
  jlong* srcData = src->array->ai64;
  jlong* destData = dest->array->ai64;
  int srcLength = src->array->length;
  int destLength = dest->array->length;
  while (length > 0) {
    if (srcPos >= srcLength) {
      jfvm_throw_outofbounds(jvm, src, srcPos);
    }
    if (destPos >= destLength) {
      jfvm_throw_outofbounds(jvm, dest, destPos);
    }
    destData[destPos++] = srcData[srcPos++];
    length--;
  }
}

static void copy_objs(JVM *jvm, Object *src, int srcPos, Object *dest, int destPos, int length) {
  Object* srcData = src->array->objs;
  Object* destData = dest->array->objs;
  int srcLength = src->array->length;
  int destLength = dest->array->length;
  while (length > 0) {
    if (srcPos >= srcLength) {
      jfvm_throw_outofbounds(jvm, src, srcPos);
    }
    if (destPos >= destLength) {
      jfvm_throw_outofbounds(jvm, dest, destPos);
    }
    //TODO : need to use arc get/set
    destData[destPos++] = srcData[srcPos++];
    length--;
  }
}

void java_java_lang_System_arraycopy(JVM *jvm, Slot *args) {
  //Object src, int srcPos, Object destPos, int destPos, int length
  if (args[0].obj == NULL) {
    jfvm_throw_npe(jvm);
  }
  if (args[2].obj == NULL) {
    jfvm_throw_npe(jvm);
  }
  Object *src = args[0].obj;
  Object *dest = args[2].obj;
  char srcType = src->type;
  char destType = dest->type;
  if (srcType == 'L' || destType == 'L') {
    //must be arrays
    jfvm_throw_npe(jvm);
  }
  if (srcType != destType) {
    //must be same types
    jfvm_throw_npe(jvm);
  }
  int srcPos = args[1].i32;
  int destPos = args[3].i32;
  int length = args[4].i32;
  switch (srcType) {
    case 'Z':
    case 'B':
      copy_bytes(jvm, src, srcPos, dest, destPos, length);
      break;
    case 'C':
    case 'S':
      copy_shorts(jvm, src, srcPos, dest, destPos, length);
      break;
    case 'I':
    case 'F':
      copy_ints(jvm, src, srcPos, dest, destPos, length);
      break;
    case 'J':
    case 'D':
      copy_longs(jvm, src, srcPos, dest, destPos, length);
      break;
    case 'A':
      copy_objs(jvm, src, srcPos, dest, destPos, length);
      break;
  }
  jfvm_arc_release(jvm, &args[0]);
  jfvm_arc_release(jvm, &args[2]);
}

static void rcopy_bytes(JVM *jvm, Object *src, int srcPos, Object *dest, int destPos, int length) {
  jbyte* srcData = src->array->ai8;
  jbyte* destData = dest->array->ai8;
  int srcLength = src->array->length;
  int destLength = dest->array->length;
  srcPos += length-1;
  destPos += length-1;
  while (length > 0) {
    if (srcPos >= srcLength) {
      jfvm_throw_outofbounds(jvm, src, srcPos);
    }
    if (destPos >= destLength) {
      jfvm_throw_outofbounds(jvm, dest, destPos);
    }
    destData[destPos--] = srcData[srcPos--];
    length--;
  }
}

static void rcopy_shorts(JVM *jvm, Object *src, int srcPos, Object *dest, int destPos, int length) {
  jshort* srcData = src->array->ai16;
  jshort* destData = dest->array->ai16;
  int srcLength = src->array->length;
  int destLength = dest->array->length;
  srcPos += length-1;
  destPos += length-1;
  while (length > 0) {
    if (srcPos >= srcLength) {
      jfvm_throw_outofbounds(jvm, src, srcPos);
    }
    if (destPos >= destLength) {
      jfvm_throw_outofbounds(jvm, dest, destPos);
    }
    destData[destPos--] = srcData[srcPos--];
    length--;
  }
}

static void rcopy_ints(JVM *jvm, Object *src, int srcPos, Object *dest, int destPos, int length) {
  jint* srcData = src->array->ai32;
  jint* destData = dest->array->ai32;
  int srcLength = src->array->length;
  int destLength = dest->array->length;
  srcPos += length-1;
  destPos += length-1;
  while (length > 0) {
    if (srcPos >= srcLength) {
      jfvm_throw_outofbounds(jvm, src, srcPos);
    }
    if (destPos >= destLength) {
      jfvm_throw_outofbounds(jvm, dest, destPos);
    }
    destData[destPos--] = srcData[srcPos--];
    length--;
  }
}

static void rcopy_longs(JVM *jvm, Object *src, int srcPos, Object *dest, int destPos, int length) {
  jlong* srcData = src->array->ai64;
  jlong* destData = dest->array->ai64;
  int srcLength = src->array->length;
  int destLength = dest->array->length;
  srcPos += length-1;
  destPos += length-1;
  while (length > 0) {
    if (srcPos >= srcLength) {
      jfvm_throw_outofbounds(jvm, src, srcPos);
    }
    if (destPos >= destLength) {
      jfvm_throw_outofbounds(jvm, dest, destPos);
    }
    destData[destPos--] = srcData[srcPos--];
    length--;
  }
}

static void rcopy_objs(JVM *jvm, Object *src, int srcPos, Object *dest, int destPos, int length) {
  Object* srcData = src->array->objs;
  Object* destData = dest->array->objs;
  int srcLength = src->array->length;
  int destLength = dest->array->length;
  srcPos += length-1;
  destPos += length-1;
  while (length > 0) {
    if (srcPos >= srcLength) {
      jfvm_throw_outofbounds(jvm, src, srcPos);
    }
    if (destPos >= destLength) {
      jfvm_throw_outofbounds(jvm, dest, destPos);
    }
    //TODO : need to use arc get/set
    destData[destPos--] = srcData[srcPos--];
    length--;
  }
}

void java_java_lang_System_rarraycopy(JVM *jvm, Slot *args) {
  //Object src, int srcPos, Object destPos, int destPos, int length
  if (args[0].obj == NULL) {
    jfvm_throw_npe(jvm);
  }
  if (args[2].obj == NULL) {
    jfvm_throw_npe(jvm);
  }
  Object *src = args[0].obj;
  Object *dest = args[2].obj;
  char srcType = src->type;
  char destType = dest->type;
  if (srcType == 'L' || destType == 'L') {
    //must be arrays
    jfvm_throw_npe(jvm);
  }
  if (srcType != destType) {
    //must be same types
    jfvm_throw_npe(jvm);
  }
  int srcPos = args[1].i32;
  int destPos = args[3].i32;
  int length = args[4].i32;
  switch (srcType) {
    case 'Z':
    case 'B':
      rcopy_bytes(jvm, src, srcPos, dest, destPos, length);
      break;
    case 'C':
    case 'S':
      rcopy_shorts(jvm, src, srcPos, dest, destPos, length);
      break;
    case 'I':
    case 'F':
      rcopy_ints(jvm, src, srcPos, dest, destPos, length);
      break;
    case 'J':
    case 'D':
      rcopy_longs(jvm, src, srcPos, dest, destPos, length);
      break;
    case 'A':
      rcopy_objs(jvm, src, srcPos, dest, destPos, length);
      break;
  }
  jfvm_arc_release(jvm, &args[0]);
  jfvm_arc_release(jvm, &args[2]);
}

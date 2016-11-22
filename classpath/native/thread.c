#include <jfvm.h>

#ifdef JFVM_WIN
  #include "thread_win.c"
#endif

#ifdef JFVM_LNX
  #include "thread_lnx.c"
#endif

void java_java_lang_Thread_holdsLock(JVM *jvm, Slot *local) {
  if (local[1].obj == NULL) {
    jfvm_throw_npe(jvm);
  }
  jboolean cond = J_FALSE;
  if (local[1].obj->locks != NULL) {
    cond = local[1].obj->locks->thread == local[0].obj;
  }
  jfvm_arc_release(jvm, &local[0]);
  jfvm_arc_release(jvm, &local[1]);
  local[0].i32 = cond;
  local[0].type = 'Z';
}

void java_java_lang_Thread_currentThread(JVM *jvm, Slot *local) {
  jfvm_arc_inc(jvm, jvm->thread);
  local[0].obj = jvm->thread;
  local[0].type = 'L';
}

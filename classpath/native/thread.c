#include <jfvm.h>

#ifdef JFVM_WIN
  #include "thread_win.c"
#endif

#ifdef JFVM_LNX
  #include "thread_lnx.c"
#endif

void java_java_lang_Thread_holdsLock(JVM *jvm, Slot *args) {
  if (args[1].obj == NULL) {
    jfvm_throw_npe(jvm);
  }
  jboolean cond = J_FALSE;
  if (args[1].obj->locks != NULL) {
    cond = args[1].obj->locks->thread == args[0].obj;
  }
  jfvm_arc_release(jvm, &args[0]);
  jfvm_arc_release(jvm, &args[1]);
  args[0].i32 = cond;
  args[0].type = 'Z';
}

void java_java_lang_Thread_currentThread(JVM *jvm, Slot *args) {
  jfvm_arc_inc(jvm, jvm->thread);
  args[0].obj = jvm->thread;
  args[0].type = 'L';
}

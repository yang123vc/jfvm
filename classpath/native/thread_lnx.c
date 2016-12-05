#include <pthread.h>
#include <unistd.h>  //usleep()

static void start_thread(Object *arg) {
  JVM *jvm = jfvm_alloc(NULL, sizeof(JVM));
  jvm->thread = arg;
  Slot stack[2] = {{0,0},{0,0}};
  stack[0].obj = arg;
  stack[0].type = 'L';
  jfvm_arc_inc(jvm, arg);
  UCatch *ucatch = jfvm_alloc(jvm, sizeof(UCatch));
  ucatch->type = UCatch_type;
  ucatch->cls = jfvm_find_class(jvm, "java/lang/Throwable");
  jvm->ustack = ucatch;
  if (setjmp(ucatch->buf) == 0) {
    jfvm_invokevirtual(jvm, arg->cls, jfvm_get_method_objidx(jvm, arg->cls, "run()V"), stack);
  } else {
    Object *ecx = jvm->exception;
    printf("Uncaught exception:%s\n", ecx->cls->name);
  }
  jfvm_thread_remove(jvm, arg);
}

void java_java_lang_Thread_start(JVM *jvm, Slot *args) {
  //args[0].obj = this
  union {
    pthread_t handle;
    void* ptr;
  } p;
  pthread_create(&p.handle, NULL, &start_thread, args[0].obj);
  jfvm_write_handle_ptr(jvm, args[0].obj, p.ptr);
  jfvm_thread_add(jvm, args[0].obj);
//  jfvm_arc_release(jvm, &args[0]); //do not release it - new thread will own it
  args[0].type = 0;
}

void java_java_lang_Thread_join(JVM *jvm, Slot *args) {
  union {
    pthread_t handle;
    void* ptr;
  } p;
  p.ptr = jfvm_read_handle_ptr(jvm, args[0].obj);
  pthread_join(p.handle, NULL);
}

void java_java_lang_Thread_sleep(JVM *jvm, Slot *args) {
  usleep(args[0].i32 * 1000);
}


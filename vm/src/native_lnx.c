#include <stdlib.h>
#include <dlfcn.h>
#include <pthread.h>

void* jfvm_lib_load(const char *fn) {
  void *ptr = dlopen(fn, RTLD_LAZY | RTLD_LOCAL);
  if (ptr == NULL) {
    printf("Error:%s\n", dlerror());
  }
  return ptr;
}

void *jfvm_lib_get_ptr(void *lib, const char *sym) {
  return dlsym(lib, sym);
}

void *jfvm_mutex_alloc() {
  pthread_mutex_t *mutex = jfvm_alloc(NULL, sizeof(pthread_mutex_t));
  pthread_mutex_init(mutex, NULL);
  return mutex;
}

void jfvm_mutex_lock(void *mutex) {
  pthread_mutex_lock(mutex);
}

void jfvm_mutex_unlock(void *mutex) {
  pthread_mutex_unlock(mutex);
}

void jfvm_mutex_free(void *mutex) {
  pthread_mutex_destroy(mutex);
}

void* jfvm_cond_alloc() {
  void *cond = jfvm_alloc(NULL, sizeof(pthread_cond_t));
  pthread_cond_init(cond, NULL);
  return cond;
}

void jfvm_cond_wait(void *cond, void *mutex, jlong ms) {  //0=forever
  if (ms == 0) {
    //forever
    pthread_cond_wait(cond, mutex);
  } else {
    struct timespec ts;
    ts.tv_sec = ms / 1000L;
    ts.tv_nsec = ms * 1000000L;
    pthread_cond_timedwait(cond, mutex, &ts);
  }
}

void jfvm_cond_notify(void *cond) {
  pthread_cond_signal(cond);
}

void jfvm_cond_notifyAll(void *cond) {
  pthread_cond_broadcast(cond);
}

void jfvm_cond_free(void *cond) {
  pthread_cond_destroy(cond);
  jfvm_free(NULL, cond);
}

void *jfvm_alloc(JVM *jvm, int size) {
  return calloc(size, 1);  //calloc will zero-fill memory
}

void jfvm_free(JVM *jvm, void *ptr) {
  free(ptr);
}

void* jfvm_realloc(JVM *jvm, void *ptr, int newSize) {
  if (ptr == NULL) return jfvm_alloc(jvm, newSize);
  return realloc(ptr,newSize);  //NOTE : does NOT zero-fill near area !!!
}

void jfvm_exit(int rt) {
  exit(rt);
}

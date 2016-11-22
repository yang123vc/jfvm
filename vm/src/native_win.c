#ifdef _WIN32_WINNT
#undef _WIN32_WINNT
#endif
#define _WIN32_WINNT 0x600  //required for condition variables (Vista+)
#include <windows.h>

void* jfvm_lib_load(const char *fn) {
  return LoadLibrary(fn);
}

void *jfvm_lib_get_ptr(void *lib, const char *sym) {
  return GetProcAddress(lib, sym);
}

void *jfvm_mutex_alloc() {
  //CreateMutex can not be used since the Sleep functions (used in wait()) require a CriticalSection
  void *cs = jfvm_alloc(NULL, sizeof(CRITICAL_SECTION));
  InitializeCriticalSection(cs);
  return cs;
}

void jfvm_mutex_lock(void *mutex) {
  EnterCriticalSection(mutex);
}

void jfvm_mutex_unlock(void *mutex) {
  LeaveCriticalSection(mutex);
}

void jfvm_mutex_free(void *mutex) {
  DeleteCriticalSection(mutex);
  jfvm_free(NULL, mutex);
}

void* jfvm_cond_alloc() {
  void *cond = jfvm_alloc(NULL, sizeof(CONDITION_VARIABLE));
  InitializeConditionVariable(cond);
  return cond;
}

void jfvm_cond_wait(void *cond, void *mutex, jlong ms) {  //0=forever
  if (ms == 0) ms = INFINITE;
  SleepConditionVariableCS(cond, mutex, ms);
}

void jfvm_cond_notify(void *cond) {
  WakeConditionVariable(cond);
}

void jfvm_cond_notifyAll(void *cond) {
  WakeAllConditionVariable(cond);
}

void jfvm_cond_free(void *cond) {
  jfvm_free(NULL, cond);
}

void *jfvm_alloc(JVM *jvm, int size) {
  void * ptr = GlobalAlloc(0x40, size);  //0x40 = zero init
#ifdef JFVM_DEBUG_MEMORY
  printf("   alloc:%p:%p:%d\n", jvm != NULL ? jvm->thread : NULL, ptr, size);
#endif
  return ptr;
}

void jfvm_free(JVM *jvm, void *ptr) {
  if (ptr == NULL) return;
#ifdef JFVM_DEBUG_MEMORY
  printf("    free:%p:%p\n", jvm != NULL ? jvm->thread : NULL, ptr);
#endif
  GlobalFree(ptr);
}

void* jfvm_realloc(JVM *jvm, void *ptr, int newSize) {
  if (ptr == NULL) return jfvm_alloc(jvm, newSize);
  void *newptr = GlobalReAlloc(ptr,newSize,0x42);  //0x40 = zero init : 0x02 = moveable
#ifdef JFVM_DEBUG_MEMORY
  printf(" realloc:%p:%p:%p:%d\n", jvm != NULL ? jvm->thread : NULL, newptr, ptr, newSize);
#endif
  return newptr;
}

void jfvm_exit(int rt) {
  ExitProcess(rt);
}

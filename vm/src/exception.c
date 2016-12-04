#include <jfvm.h>

void jfvm_throw_exception(JVM *jvm, Object *exception) {
  UMethod *umethod;
  UCatch *ucatch;
  USync *usync;
  jvm->exception = exception;
  do {
    switch (jvm->ustack->type) {
      case UMethod_type:
        umethod = (UMethod*)jvm->ustack;
//        printf("  unwind method:%s.%s\n", method->cls, method->method);
        longjmp(umethod->buf, 1);  //function must continue processing exception
        break;
      case UCatch_type:
        ucatch = (UCatch*)jvm->ustack;
//        printf("  unwind catch:%s\n", jcatch->cls->name);
        jvm->ustack = jvm->ustack->prev;
        if (jfvm_instanceof_class(jvm, ucatch->cls->name, exception->cls)) {
//          printf("  throw:found matching catch\n");
          longjmp(ucatch->buf, 1);  //handler will place exception onto empty stack
        }
        if (ucatch->free) jfvm_free(jvm, ucatch);
        break;
      case USync_type:
        usync = (USync*)jvm->ustack;
        jfvm_monitor_exit(jvm, usync->obj);
        jvm->ustack = jvm->ustack->prev;
        break;
    }
  } while (jvm->ustack != NULL);
  printf("Error:Exception not caught:%s\n", exception->cls->name);
  jfvm_exit(1);
}

void jfvm_throw_npe(JVM *jvm) {
  Object *ex = jfvm_new(jvm, jfvm_find_class(jvm, "java/lang/NullPointerException"));
  //TODO : call ctor with string describing cause of exception
  jfvm_throw_exception(jvm, ex);
}

void jfvm_throw_outofbounds(JVM *jvm, Object *obj, int idx) {
  Object *ex = jfvm_new(jvm, jfvm_find_class(jvm, "java/lang/OutOfBoundsException"));
  //TODO : call ctor with string describing cause of exception
  jfvm_throw_exception(jvm, ex);
}

void jfvm_throw_divbyzero(JVM *jvm) {
  Object *obj = jfvm_new(jvm, jfvm_find_class(jvm, "java/lang/DivideByZeroException"));
  //TODO : call ctor with string describing cause of exception
  jfvm_throw_exception(jvm, obj);
}

void jfvm_throw_methodnotfound(JVM *jvm) {
  Object *obj = jfvm_new(jvm, jfvm_find_class(jvm, "java/lang/MethodNotFoundException"));
  //TODO : call ctor with string describing cause of exception
  jfvm_throw_exception(jvm, obj);
}

void jfvm_throw_abstractmethodcalled(JVM *jvm) {
  Object *obj = jfvm_new(jvm, jfvm_find_class(jvm, "java/lang/AbstractMethodError"));
  //TODO : call ctor with string describing cause of exception
  jfvm_throw_exception(jvm, obj);
}

void jfvm_throw_ioexception(JVM *jvm) {
  Object *ex = jfvm_new(jvm, jfvm_find_class(jvm, "java/io/IOException"));
  //TODO : call ctor with string describing cause of exception
  jfvm_throw_exception(jvm, ex);
}

UMethod* jfvm_umethod_alloc(JVM *jvm, const char *clsname, const char *methodname) {
  //add UMethod to unwinding stack
  UMethod *umethod = (UMethod*)jfvm_alloc(jvm, sizeof(UMethod));
#ifdef JFVM_DEBUG_EXCEPTION
  printf(" umethod:%p:%p:%p:%s.%s\n", jvm->thread, umethod, jvm->ustack, clsname, methodname);
#endif
  umethod->type = UMethod_type;
  umethod->cls = clsname;
  umethod->method = methodname;
  umethod->prev = jvm->ustack;
  jvm->ustack = (UStack*)umethod;
  return umethod;
}

void jfvm_umethod_unwind(JVM *jvm) {
  //remove umethod from stack
  if (jvm->ustack->type != UMethod_type) {
    printf("Error:unwinding stack corrupt:Expecting UMethod @ %p (thread=%p)\n", jvm->ustack, jvm->thread);
    jfvm_exit(1);
  }
  UMethod *umethod = (UMethod*)jvm->ustack;
  jvm->ustack = jvm->ustack->prev;
#ifdef JFVM_DEBUG_EXCEPTION
  printf("-umethod:%p:%p:%p:%s.%s\n", jvm->thread, umethod, jvm->ustack, umethod->cls, umethod->method);
#endif
  jfvm_free(jvm, umethod);
  //check for pending exception and continue it
  if (jvm->exception != NULL) {jfvm_throw_exception(jvm, jvm->exception);}
}

UCatch* jfvm_ucatch_alloc(JVM *jvm, Class *exception) {
  //add UCatch to unwinding stack
  UCatch *ucatch = (UCatch*)jfvm_alloc(jvm, sizeof(UCatch));
#ifdef JFVM_DEBUG_EXCEPTION
  printf("  ucatch:%p:%p:%p\n", jvm->thread, ucatch, jvm->ustack);
#endif
  ucatch->type = UCatch_type;
  ucatch->cls = exception;
  ucatch->prev = jvm->ustack;
  ucatch->free = 1;
  jvm->ustack = (UStack*)ucatch;
  return ucatch;
}

void jfvm_ucatch_unwind(JVM *jvm) {
  //popup UCatch from unwinding stack
  if (jvm->ustack->type != UCatch_type) {
    printf("Error:Unwinding stack corrupt:Looking for UCatch @ %p (thread=%p)\n", jvm->ustack, jvm->thread);
    jfvm_exit(1);
  }
  UCatch *ucatch = (UCatch*)jvm->ustack;
#ifdef JFVM_DEBUG_EXCEPTION
  printf("- ucatch:%p:%p:%p\n", jvm->thread, ucatch, jvm->ustack);
#endif
  jvm->ustack = jvm->ustack->prev;
  if (ucatch->free) jfvm_free(jvm, ucatch);
}

void jfvm_stack_release(JVM *jvm, Slot *slots, int slotCount) {
  for(int a=0;a<slotCount;a++) {
    if (slots[a].type == 'L') jfvm_arc_release(jvm, &slots[a]);
  }
}

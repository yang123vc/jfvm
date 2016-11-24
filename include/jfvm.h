#include <stdio.h>
#include <setjmp.h>
#include <string.h>

//define primitive types
#define jboolean char
#define jbyte char
#define jshort short
#define jchar unsigned short
#define jint int
#define jlong long long
#define jfloat float
#define jdouble double
#define J_TRUE 1
#define J_FALSE 0

#define T_OBJECT 'L'
#define T_BOOLEAN 'Z'
#define T_BYTE 'B'
#define T_CHAR 'C'
#define T_SHORT 'S'
#define T_INT 'I'
#define T_LONG 'J'
#define T_FLOAT 'F'
#define T_DOUBLE 'D'

#define T_ARRAY_OBJECT 'A'
#define T_ARRAY_BOOLEAN 'Z'
#define T_ARRAY_BYTE 'B'
#define T_ARRAY_CHAR 'C'
#define T_ARRAY_SHORT 'S'
#define T_ARRAY_INT 'I'
#define T_ARRAY_LONG 'J'
#define T_ARRAY_FLOAT 'F'
#define T_ARRAY_DOUBLE 'D'

#define jfvm_is_object_array(type) (type != 'L')

//#define JFVM_DEBUG_MEMORY
//#define JFVM_DEBUG_OBJECT
//#define JFVM_DEBUG_DETROYER
//#define JFVM_DEBUG_INVOKE
//#define JFVM_DEBUG_METHOD
//#define JFVM_DEBUG_EXCEPTION
//#define JFVM_DEBUG_SYNC
//#define JFVM_DEBUG_CATCH

struct Object;
typedef struct Object Object;

struct Class;
typedef struct Class Class;

typedef struct {
  const char *name;
  const char *desc;
  const char *name_desc;
  short flgs;
  short local;  //# args + local Slots
  int offset;  //virtual function (objidx)
  void *method;  //static / special members
} Method;

typedef struct {
  const char *name;
  const char *desc;
  const char *name_desc;
  int flgs;
  int offset;  //objidx
  union {
    jbyte i8;
    jshort i16;
    jint i32;
    jlong i64;
    jfloat f32;
    jdouble f64;
    Object *obj;
  };
} Field;

typedef struct {
  int length;
  int dims;
  union {
    jbyte ai8[0];  //type Z,B (array)
    jshort ai16[0];  //type C,S (array)
    jint ai32[0];  //type I (array)
    jlong ai64[0];  //type J (array)
    jfloat af32[0];  //type F (array)
    jdouble af64[0];  //type Z (array)
    Object* objs[0];  //type A (array)
    void* ptr[0];  //internal usage
  };
} Array;

typedef struct {
  void *mutex;  //sync
  void *cond;  //wait & notify
  Object *thread;  //thread owner
} Locks;

struct Object {  //4*8=32bytes
  char type;  //'L'=plain object ; 'ZBCSIJFDA'=array object (A=array of objects)
  char reflck;
  short pad1;
  int refcnt;
  Class *cls;
  union {
    Locks *locks;
    Object *next;   //link list for deletion
  };
  union {
    Array *array;  //arrays
    Class *defcls;  //class that Class defines
  };
  union {
    void* methods[0];  //vtable
    void* fields[0];  //x32 - long/double take 2 slots
    int ifields[0];
  };
};

typedef union {
  jbyte i8;
  jshort i16;
  jint i32;
  jlong i64;
  jfloat f32;
  jdouble f64;
  Object *obj;
} Union;

#define Exception Object

struct Slot {
  char type;  //'ZBCSIJFDL' (0=empty) (L=object) (A not used here - use L for array of objects)
  union {
    Object *obj;
    jbyte i8;
    jshort i16;
    jchar u16;
    jint i32;
    unsigned int u32;
    jlong i64;
    jfloat f32;
    jdouble f64;
    void *v;
  };
};

struct JVM;
typedef struct JVM JVM;

typedef struct Slot Slot;

struct Class {
  const char *name;
  const char *super;
  Class *super_class;
  void (*object_clinit)(JVM *jvm, Slot *local);  //static { } method
  const char **interfaces;
  Class **interfaces_class;  //set in object_clsinit
  Method *methods;
  Method *static_methods;
  Field *fields;
  Field *static_fields;
  void (*init_vtable)(JVM *jvm, Slot *stack);  //this func does NOT free the stack[0]
  int flgs;
  int size;
  char object_clinit_reflck;
};

struct UStack;
typedef struct UStack UStack;

//unwinding stack
struct UStack {
  UStack *prev;
  int type;
  //...
};

typedef struct {
  UStack *prev;
  int type;
  jmp_buf buf;
  //TODO : add file/line info for debugging
  const char *cls;
  const char *method;
  int line;
} UMethod;

typedef struct {
  UStack *prev;
  int type;
  jmp_buf buf;
  Class *cls;
  int free;
} UCatch;

typedef struct {
  UStack *prev;
  int type;
  Object *obj;  //used to add a refcnt incase obj is released within sync{} block
} USync;

#define UMethod_type 1
#define UCatch_type 2
#define USync_type 3

struct JVM {
  Object *thread;  //java.lang.Thread instance for current thread
  Exception *exception;  //pending exception
  UStack *ustack;  //top of unwinding stack
  //TODO : add file/line info for debugging
  const char *file;
  int line;
};

//NOTE : all functions here are directly callable from C only
//the native functions in jfvm.jfvm.jfvm_... are in the classpath sub-project
//and they omit the java_classpath prefix which could get confused with these functions.

//system funcs
/** Opens a library file (.DLL .SO) */
void* jfvm_lib_load(const char *fn);
/** Gets a symbol from a library. */
void *jfvm_lib_get_ptr(void *lib, const char *sym);
/** Closes a library file. */
void jfvm_lib_close(void *lib);
/** Allocates a mutex. */
void *jfvm_mutex_alloc();
/** Locks a mutex. */
void jfvm_mutex_lock(void *mutex);
/** Unlocks a mutex. */
void jfvm_mutex_unlock(void *mutex);
/** Frees a mutex. */
void jfvm_mutex_free(void *mutex);
/** Allocates a condition variable (locking) */
void* jfvm_cond_alloc();
/** Waits on a condition while releasing a mutex atomically. */
void jfvm_cond_wait(void *cond, void *mutex, jlong ms);  //0=forever
/** Notify one thread that is waiting. */
void jfvm_cond_notify(void *cond);
/** Notify all threads that are waiting. */
void jfvm_cond_notifyAll(void *cond);
/** Frees a condition variable. */
void jfvm_cond_free(void *cond);
/** Allocates zero-init memory. */
void *jfvm_alloc(JVM *jvm, int size);
/** Frees memory. */
void jfvm_free(JVM *jvm, void *ptr);
/** Reallocates memory. */
void* jfvm_realloc(JVM *jvm, void *ptr, int newSize);
/** Exit the process with return value (rv). */
void jfvm_exit(int rv);
/** Returns size in bytes of a java type. */
int jfvm_get_type_size(char type);
/** Allocates an operand stack */
void *jfvm_stack_alloc(JVM *jvm, int count);
/** Reallocates an operand stack */
void *jfvm_stack_realloc(JVM *jvm, void *stack, int count);
/** Fress an operand stack. */
void jfvm_stack_free(JVM *jvm, void *stack);

//init (do NOT call)
void jfvm_init_pre(JVM *jvm);
void jfvm_init_class(JVM *jvm);

void jfvm_init_pst(JVM *jvm);
void jfvm_init_destroyer_pre(JVM *jvm);
void jfvm_init_destroyer_pst(JVM *jvm);

//arc
/** Gets src into dest in a way that is ARC safe. (locking on src) */
void jfvm_arc_get(JVM *jvm, Object **dest, Object **src);
/** Puts src into dest in a way that is ARC safe. (locking on dest) */
void jfvm_arc_put(JVM *jvm, Object **dest, Object **src);
/** Increments reference count on object. */
void jfvm_arc_inc(JVM *jvm, Object *obj);
/** Decrements reference count on object. */
void jfvm_arc_dec(JVM *jvm, Object *obj);
/** Decrements reference count in slot if it is an object. */
void jfvm_arc_release(JVM *jvm, Slot *slot);
/** Creates mutex/condition variable for an object if needed for sync tasks. */
void jfvm_arc_create_locks(JVM *jvm, Object *obj);
/** Frees mutex/condition variable for object before it is queued for deletion. */
void jfvm_arc_free_locks(JVM *jvm, Object *obj);
/** Queues an object for deletion. */
void jfvm_arc_delete(JVM *jvm, Object *obj);

//object
/** Gets a field from object in src slot and places in dest slot. */
void jfvm_getfield(JVM *jvm, Class *cls, char type, int objidx, Slot *src, Slot *dest);
/** Gets a static field from class cls and places in dest slot. */
void jfvm_getstatic(JVM *jvm, Class *cls, char type, int clsidx, Slot *dest);
/** Puts value into field for object in dest. */
void jfvm_putfield(JVM *jvm, Class *cls, char type, int objidx, Slot *dest, Slot *value);
/** Puts value into static field for class cls. */
void jfvm_putstatic(JVM *jvm, Class *cls, char type, int clsidx, Slot *value);
/** Invokes a static function. */
void jfvm_invokestatic(JVM *jvm, Class *cls, int clsidx, Slot *args);
/** Invokes a virtual function. */
void jfvm_invokevirtual(JVM *jvm, Class *cls, int objidx, Slot *args);  //args[0] = this
/** Invokes a special function (constructors or super references) */
void jfvm_invokespecial(JVM *jvm, Class *cls, int clsidx, Slot *args);  //args[0] = this
/** Starts a sync on obj. */
void jfvm_monitor_enter(JVM *jvm, Object *obj);
/** Ends a sync on obj. (NOTE : java always places a try catch around a monitor exit) */
void jfvm_monitor_exit(JVM *jvm, Object *obj);
/** Adds an unwinding sync block to the unwinding stack. */
void jfvm_usync_add(JVM *jvm, USync *sync, Object *obj);
/** Removes an unwinding sync block from the unwinding stack. */
void jfvm_usync_remove(JVM *jvm);
/** Returns current thread object. */
Object* jfvm_get_current_thread(JVM *jvm);
/** Writes data to 'handle' field of obj. (convenience method) */
void jfvm_write_handle_ptr(JVM *jvm, Object *obj, void* data);
/** Reads data from 'handle' field of obj. (convenience method) */
void* jfvm_read_handle_ptr(JVM *jvm, Object *obj);
/** Writes data to 'handle' field of obj. (convenience method) */
void jfvm_write_handle_int(JVM *jvm, Object *obj, int data);
/** Reads data from 'handle' field of obj. (convenience method) */
int jfvm_read_handle_int(JVM *jvm, Object *obj);
//these functions are NOT ARC safe (use only for objects that are not shared yet - ie: in constructors)
Object* jfvm_get_object(JVM *jvm, Object *obj, const char *field);
void jfvm_set_object(JVM *jvm, Object *obj, const char *field, Object *value);

//class
/** Creates a new instance of class.  Constructor is not called. */
Object* jfvm_new(JVM *jvm, Class *cls);
/** Creates a new primitive array of type. */
Object* jfvm_newarray(JVM *jvm, char type, int size);
/** Creates a new array of objects of type cls. */
Object* jfvm_anewarray(JVM *jvm, const char *cls, int cnt);
/** Creates a new multi-dimensional array. */
Object* jfvm_multianewarray(JVM *jvm, int numdims, const char *cls, Slot *dims);
/** Creates a new string from C string */
Object* jfvm_new_string(JVM *jvm, const char *cstr, int len);
/** Returns C string from a Java String (must call jfvm_string_releasebytes() to free) */
const char *jfvm_string_getbytes(JVM *jvm, Object *str);
/** Frees a C string returned from jfvm_string_getbytes(). */
void jfvm_string_releasebytes(JVM *jvm, const char *cstr);
/** Determines if cls is instance of class of name. */
jboolean jfvm_instanceof_class(JVM *jvm, const char *name, Class *cls);
/** Determines if object in slot is instance of class of name. */
void jfvm_instanceof(JVM *jvm, const char *name, Slot *slot);
/** Returns the clsidx (class index) of a method (used with invokespecial). */
int jfvm_get_method_clsidx(JVM *jvm, Class *cls, const char *name_desc);  //invokespecial
/** Returns the clsidx (class index) of a static method (used with invokestatic). */
int jfvm_get_static_method_clsidx(JVM *jvm, Class *cls, const char *name_desc);  //invokestatic
/** Returns the objidx (object index) of a method (used with invokevirtual). */
int jfvm_get_method_objidx(JVM *jvm, Class *cls, const char *name_desc);  //invokevirtual
/** Returns the objidx (object index) of a field. */
int jfvm_get_field_objidx(JVM *jvm, Class *cls, const char *name);
/** Returns the clsidx (class index) of a static field. */
int jfvm_get_static_field_clsidx(JVM *jvm, Class *cls, const char *name);
/** Creates a lambda object. */
Object* jfvm_create_lambda(JVM *jvm, Class *cls, int objidx, void *method);

//classpath
/** Finds a class by name (ie: "java/lang/Object"). */
Class *jfvm_find_class(JVM *jvm, const char *);
/** Adds a library to the classpath. */
void* jfvm_classpath_add(JVM *jvm, const char *fn);
/** Removes a library from the classpath. */
void jfvm_classpath_del(JVM *jvm, void *);
/** Registers classes with a library (called by jfvm_classpath_add already). */
void jfvm_register_classes(JVM *jvm, Class **classes);
/** Patches classes (called by jfvm_classpath_add already). */
int jfvm_patch_classes(JVM *jvm, Class **classes);

//exception
/** Throws an exception. */
void jfvm_throw_exception(JVM *jvm, Object *exception);
/** Throws a NullPointerException */
void jfvm_throw_npe(JVM *jvm);
/** Throws an index out of bounds exception. */
void jfvm_throw_outofbounds(JVM *jvm, Object *obj, int idx);
/** Throws a divide by zero exception. */
void jfvm_throw_divbyzero(JVM *jvm);
/** Throws a method not found exception. */
void jfvm_throw_methodnotfound(JVM *jvm);
/** Throws an Abstract Method Called exception. */
void jfvm_throw_abstractmethodcalled(JVM *jvm);
/** Throws an IOException */
void jfvm_throw_ioexception(JVM *jvm);
/** Allocates an unwinding method block and places it on the unwinding stack. */
UMethod* jfvm_umethod_alloc(JVM *jvm, const char *cls, const char *method);
/** Allocates an unwinding catch block and places it on the unwinding stack. */
UCatch* jfvm_ucatch_alloc(JVM *jvm, Class *exception);
/** Frees a method unwinding block. */
void jfvm_umethod_unwind(JVM *jvm);
/** Frees a catch unwinding block. */
void jfvm_ucatch_unwind(JVM *jvm);
/** Releases all objects (decrement ref count) in a stack. */
void jfvm_release_stack(JVM *jvm, Slot *stack, int stackCount);

//object destroyer (and thread monitor too)
void jfvm_destroy_run(JVM *jvm, Slot *local);
void jfvm_destroy_add(JVM *jvm, Object *obj);
void jfvm_destroy_obj(JVM *jvm, Object *obj);
void jfvm_destroy_join(JVM *jvm);

//thread pool
void jfvm_thread_add(JVM *jvm, Object *obj);
void jfvm_thread_remove(JVM *jvm, Object *obj);

/** Starts the main thread. */
void jfvm_main(const char *lib_name[], void *lib_handle[], int lib_count, int argc, const char **argv, const char* clsname);

#include <jfvm.h>

void java_java_lang_reflect_Array_newInstance(JVM *jvm, Slot *args) {
  //args[0] = java.lang.Class
  Object *obj = args[0].obj;
  Class *cls = obj->defcls;
  char type;
  jfvm_get_field(jvm, obj, "arrayType", &type, 1);
  Object *array;
  if (type == 'A')
    array = jfvm_anewarray(jvm, cls, args[1].i32);
  else
    array = jfvm_newarray(jvm, type, args[1].i32);
  jfvm_arc_release(jvm, &args[0]);
  args[0].obj = array;
  args[0].type = 'L';
}

#include <jfvm.h>

//java.lang.Class methods
//NOTE : all functions should work on the defcls (cls points to java.lang.Class itself)

void java_java_lang_Class_newInstance(JVM *jvm, Slot *args) {
  Class *defcls = args[0].obj->defcls;
  jfvm_arc_release(jvm, &args[0]);
  args[0].obj = jfvm_new(jvm, defcls);
  args[0].type = 'L';
}

void java_java_lang_Class_getFields(JVM *jvm, Slot *args) {
  int cnt = 0;
  int pos = 0;
  const char *name;
  Class *cls = args[0].obj->defcls;
  while (cls->fields[pos].name != NULL) {
    pos++;
    cnt++;
  }
  pos = 0;
  while (cls->static_fields[pos].name != NULL) {
    pos++;
    cnt++;
  }
  Object *fields = jfvm_anewarray(jvm, "java/lang/reflect/Field", cnt);
  pos = 0;
  cnt = 0;
  while (cls->fields[pos].name != NULL) {
    name = cls->fields[pos].name;
    fields->array->objs[cnt] = jfvm_new_string(jvm, name, strlen(name));
    pos++;
    cnt++;
  }
  pos = 0;
  while (cls->static_fields[pos].name != NULL) {
    name = cls->fields[pos].name;
    fields->array->objs[cnt] = jfvm_new_string(jvm, name, strlen(name));
    pos++;
    cnt++;
  }
  jfvm_arc_release(jvm, &args[0]);
  args[0].obj = fields;
  args[0].type = 'L';
}

void java_java_lang_Class_getMethods(JVM *jvm, Slot *args) {
  int cnt = 0;
  int pos = 0;
  const char *name;
  Class *cls = args[0].obj->defcls;
  while (cls->methods[pos].name != NULL) {
    pos++;
    cnt++;
  }
  pos = 0;
  while (cls->static_methods[pos].name != NULL) {
    pos++;
    cnt++;
  }
  Object *methods = jfvm_anewarray(jvm, "java/lang/reflect/Method", cnt);
  pos = 0;
  cnt = 0;
  while (cls->methods[pos].name != NULL) {
    name = cls->methods[pos].name;
    methods->array->objs[cnt] = jfvm_new_string(jvm, name, strlen(name));
    pos++;
    cnt++;
  }
  pos = 0;
  while (cls->static_methods[pos].name != NULL) {
    name = cls->methods[pos].name;
    methods->array->objs[cnt] = jfvm_new_string(jvm, name, strlen(name));
    pos++;
    cnt++;
  }
  jfvm_arc_release(jvm, &args[0]);
  args[0].obj = methods;
  args[0].type = 'L';
}

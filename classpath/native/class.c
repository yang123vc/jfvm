#include <jfvm.h>

//java.lang.Class methods
//NOTE : all functions should work on the defcls (cls points to java.lang.Class itself)

void java_java_lang_Class_getName(JVM *jvm, Slot *local) {
  const char *name = local[0].obj->defcls->name;
  Object *str = jfvm_new_string(jvm, name, strlen(name));
  jfvm_arc_release(jvm, &local[0]);
  local[0].obj = str;
  local[0].type = 'L';
}

void java_java_lang_Class_newInstance(JVM *jvm, Slot *local) {
  Class *defcls = local[0].obj->defcls;
  jfvm_arc_release(jvm, &local[0]);
  local[0].obj = jfvm_new(jvm, defcls);
  local[0].type = 'L';
}

void java_java_lang_Class_getFields(JVM *jvm, Slot *local) {
  int cnt = 0;
  int pos = 0;
  const char *name;
  Class *cls = local[0].obj->defcls;
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
  jfvm_arc_release(jvm, &local[0]);
  local[0].obj = fields;
  local[0].type = 'L';
}

void java_java_lang_Class_getMethods(JVM *jvm, Slot *local) {
  int cnt = 0;
  int pos = 0;
  const char *name;
  Class *cls = local[0].obj->defcls;
  while (cls->methods[pos].name != NULL) {
    pos++;
    cnt++;
  }
  pos = 0;
  while (cls->static_methods[pos].name != NULL) {
    pos++;
    cnt++;
  }
  Object *methods = jfvm_anewarray(jvm, "java/lang/reflect/Field", cnt);
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
  jfvm_arc_release(jvm, &local[0]);
  local[0].obj = methods;
  local[0].type = 'L';
}

#include <jfvm.h>

//java.lang.Object methods

void java_java_lang_Object_clone(JVM *jvm, Slot *args) {
  Object *obj = args[0].obj;
  Class *cls = obj->cls;
  int size = sizeof(Object) + cls->size * sizeof(void*);
  Object *cln = jfvm_alloc(jvm, size);
  memcpy(cln, obj, sizeof(Object));  //copy vtable only (fields are copied later)
  cln->locks = NULL;  //do NOT copy locking vars
  cln->array = NULL;  //do NOT copy array
  char type = obj->type;
  if (jfvm_is_object_array(type)) {
    int length = obj->array->length;
    int arraysize = jfvm_get_type_size(type) * length;
    if (type == 'A') {
      //array of objects
      for(int a=0;a<length;a++) {
        jfvm_arc_get(jvm, &cln->array->objs[a], &obj->array->objs[a]);
      }
    } else {
      //primitive array
      cln->array = jfvm_alloc(jvm, sizeof(Array) + arraysize);
      memcpy(cln->array, obj->array, sizeof(Array) + arraysize);
    }
  } else {
    //complex object (copy each field)
    Field *field;
    do {
      field = cls->fields;
      while (field->name != NULL) {
        int objidx = field->offset;
        type = field->desc[0];
        if (type == 'L') {
          jfvm_arc_get(jvm, &cln->fields[objidx], &obj->fields[objidx]);
        } else {
          size = jfvm_get_type_size(type);
          memcpy(&cln->fields[objidx], &obj->fields[objidx], size);
        }
        field++;
      };
      cls = cls->super_class;
    } while (cls != NULL);
  }
  jfvm_arc_release(jvm, &args[0]);
  args[0].obj = cln;
  args[0].type = 'L';
}

static Object* getClass(JVM *jvm, Object *obj) {
  Class *cls = obj->cls;
  Object *clsobj = jfvm_new(jvm, jfvm_find_class(jvm, "java/lang/Class"));
  clsobj->defcls = cls;
  jfvm_set_object(jvm, clsobj, "name", jfvm_new_string_utf8(jvm, cls->name, strlen(cls->name)));
  return clsobj;
}

void java_java_lang_Object_getClass(JVM *jvm, Slot *args) {
  //create Class object
  char type = args[0].obj->type;
  int isArray = type != 'L';
  Object *clsobj = getClass(jvm, args[0].obj);
  if (isArray) {
    jfvm_set_field(jvm, clsobj, "isArray", &isArray, 1);
    Object *ct = getClass(jvm, args[0].obj);
    jfvm_set_object(jvm, clsobj, "componentType", ct);
    jfvm_set_field(jvm, clsobj, "arrayType", &type, 1);
  }
  jfvm_arc_release(jvm, &args[0]);
  args[0].obj = clsobj;
  args[0].type = 'L';
}

int java_java_lang_Object_hashCode(JVM *jvm, Slot *args) {
  union {
    int i;
    void *v;
  } u;
  u.v = args[0].obj;
  jfvm_arc_release(jvm, &args[0]);
  return u.i;
}

void java_java_lang_Object_wait_JI(JVM *jvm, Slot *args) {
  //args[0].i64 = ms
  //args[1].i32 = ns (not used)
  Object *obj = args[0].obj;
  jfvm_cond_wait(obj->locks->cond, obj->locks->mutex, args[1].i64);
  jfvm_arc_release(jvm, &args[0]);
}

void java_java_lang_Object_notify(JVM *jvm, Slot *args) {
  Object *obj = args[0].obj;
  jfvm_cond_notify(obj->locks->cond);
  jfvm_arc_release(jvm, &args[0]);
}

void java_java_lang_Object_notifyAll(JVM *jvm, Slot *args) {
  Object *obj = args[0].obj;
  jfvm_cond_notifyAll(obj->locks->cond);
  jfvm_arc_release(jvm, &args[0]);
}

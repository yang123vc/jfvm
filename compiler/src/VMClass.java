public class VMClass {
  public VMClass() {}
  public Const ConstList[];
  public Interface InterfaceList[];
  public Field FieldList[];
  public Attribute AttributeList[];
  public Method MethodList[];
  public short access_flags;
  public short this_class; String name; String cname;
  public short super_class; String super_name;

  public boolean cp;  //classpath (do not compile)
  public int size;  //# of methods & fields (non-static)
  public int msize;  //# of methods (non-static)
  public int fsize;  //# of fields (non-static)
  public int staticmsize;  //# of static methods
  public int staticfsize;  //# of static fields
  public VMClass super_cls;
  public VMClass iface_cls[];

  public boolean isInterface;

  public static int ACC_PUBLIC = 0x0001;
  public static int ACC_FINAL = 0x0010;
  public static int ACC_SUPER = 0x0020;
  public static int ACC_INTERFACE = 0x0200;
  public static int ACC_ABSTRACT = 0x0400;
  public static int ACC_SYNTHETIC = 0x1000;
  public static int ACC_ANNOTATION = 0x2000;
  public static int ACC_ENUM = 0x4000;

  /** Returns a String from constant pool. */
  public String getConstString(int idx) {
    if (idx >= ConstList.length) return null;
    switch (ConstList[idx].type) {
      case 1:
        break;
      case 3:
        ConstInteger ci = (ConstInteger)ConstList[idx];
        return Integer.toString(ci.val);
      case 7:
        ConstClass cc = (ConstClass)ConstList[idx];
        idx = cc.idx;
        break;
      case 8:
        ConstString cs = (ConstString)ConstList[idx];
        idx = cs.idx;
        break;
      default:
        return null;
    }
    if (ConstList[idx].type != 1) return null;
    if (idx >= ConstList.length) return null;
    ConstUTF8 cu8 = (ConstUTF8)ConstList[idx];
    return cu8.str;
  }
  /** Returns integer from constant pool. */
  public int getConstInt(int idx) {
    if (idx >= ConstList.length) return 0;
    if (ConstList[idx].type != 3) return 0;
    ConstInteger i = (ConstInteger)ConstList[idx];
    return i.val;
  }
  public String getConstName(int idx) {
    Const c = ConstList[idx];
    if (!(c instanceof ConstNameType)) {
      System.out.println("Error:Wrong Const Type:" + idx);
    }
    ConstNameType cnt = (ConstNameType)c;
    return getConstString(cnt.name_idx);
  }
  public String getConstType(int idx) {
    Const c = ConstList[idx];
    ConstNameType cnt = (ConstNameType)c;
    return getConstString(cnt.desc_idx);
  }
  public int getFieldConstValue(Field f) {
    for(int a=0;a<f.attr_cnt;a++) {
      if (f.attrs[a] instanceof AttrConst) {
        AttrConst c = (AttrConst)f.attrs[a];
        return c.const_idx;
      }
    }
    return -1;
  }

  public void calcSize() {
    int mcnt = MethodList.length;
    for(int a=0;a<mcnt;a++) {
      Method method = MethodList[a];
      //check if method overrides another in super class
      if (super_cls != null) {
        if (super_cls.hasMethod(method.name, method.desc)) {
          method.isOverrides = true;
          continue;  //offset would be in derived class
        }
      }
      //check if method overrides(implements) in interface classes
      for(int i=0;i<iface_cls.length;i++) {
        if (iface_cls[i].hasMethod(method.name, method.desc)) {
          method.isOverrides = true;
          continue;  //offset would be in derived class
        }
      }
      if (method.isStatic) {
        staticmsize++;
      } else {
        msize++;
      }
    }
    int fcnt = FieldList.length;
    for(int a=0;a<fcnt;a++) {
      Field field = FieldList[a];
      if (field.isStatic) {
        staticfsize++;
      } else {
        fsize++;
        if (!jfvmc.x64 && field.is64()) fsize++;  //field takes 2 slots
      }
    }
    size = msize + fsize;
  }
  /** Returns size of just this class. */
  public int getSize() {
    return size;
  }
  /** Returns size of this class including all super classes. */
  public int getFullSize() {
    int fsize = size;
    VMClass cls = super_cls;
    while (cls != null) {
      fsize += cls.getSize();
      cls = cls.super_cls;
    }
    return fsize;
  }
  public void getSuper(ClassPool clspool) throws Exception {
    if (this.super_class != 0) {
      super_cls = clspool.getClass(super_name);
    }
    iface_cls = new VMClass[InterfaceList.length];
    for(int a=0;a<InterfaceList.length;a++) {
      iface_cls[a] = clspool.getClass(InterfaceList[a].name);
    }
  }
  public void calcOffsets() {
    int cnt = MethodList.length;
    int obj_offset = 0;
    int cls_offset_virtual = 0;
    int cls_offset_static = 0;
    VMClass cls = super_cls;
    while (cls != null) {
      obj_offset += cls.getSize();
      cls = cls.super_cls;
    }
    for(int a=0;a<iface_cls.length;a++) {
      obj_offset += iface_cls[a].getSize();
    }
    for(int a=0;a<cnt;a++) {
      Method method = MethodList[a];
      if (method.isOverrides) {
        method.objidx = -1;  //patch later
      } else {
        if (method.isStatic) {
          method.objidx = -1;  //not used
        } else {
          method.objidx = obj_offset++;
        }
      }
      if (method.isStatic)
        method.clsidx = cls_offset_static++;
      else
        method.clsidx = cls_offset_virtual++;
    }
    int fcnt = FieldList.length;
    int cls_offset = 0;
    cls_offset_static = 0;
    for(int a=0;a<fcnt;a++) {
      Field field = FieldList[a];
      if (field.isStatic) {
        field.clsidx = cls_offset_static++;
        field.objidx = -1;  //not used
      } else {
        field.clsidx = cls_offset++;
        field.objidx = obj_offset++;
        if (!jfvmc.x64 && field.is64()) obj_offset++;  //64bit field takes 2 slots in 32bit VM
      }
    }
  }
  public void calcOverrideOffsets() throws Exception {
    int cnt = MethodList.length;
    for(int a=0;a<cnt;a++) {
      Method method = MethodList[a];
      if (method.isOverrides) {
        if (super_cls != null) {
          method.objidx = super_cls.getMethodObjOffset(method.name_desc, false);
        }
        for(int i=0;i < iface_cls.length && method.objidx == -1;i++) {
          method.objidx = iface_cls[i].getMethodObjOffset(method.name_desc, false);
        }
      }
    }
  }
  private Method getMethod(String name_desc, boolean first) throws Exception {
    int cnt = MethodList.length;
    for(int a=0;a<cnt;a++) {
      Method method = MethodList[a];
      if (method.name_desc.equals(name_desc)) {
        return method;
      }
    }
    if (super_cls != null) {
      Method m = super_cls.getMethod(name_desc, false);
      if (m != null) return m;
    }
    for(int a=0;a<iface_cls.length;a++) {
      Method m = iface_cls[a].getMethod(name_desc, false);
      if (m != null) return m;
    }
    if (first) throw new Exception("Error:VMClass.getMethod():Can not find method:" + name + "." + name_desc);
    return null;
  }
  public Method getMethod(String name_desc) throws Exception {
    return getMethod(name_desc, true);
  }

  private int getMethodObjOffset(String name_desc, boolean first) throws Exception {
    int cnt = MethodList.length;
    for(int a=0;a<cnt;a++) {
      Method method = MethodList[a];
      if (method.name_desc.equals(name_desc)) {
        if (method.objidx == -1) {
          return super_cls.getMethodObjOffset(name_desc, false);
        }
//        System.out.println("offset=" + method.offset);
        return method.objidx;
      }
    }
    if (super_cls != null) {
      int off = super_cls.getMethodObjOffset(name_desc, false);
      if (off != -1) return off;
    }
    for(int a=0;a<iface_cls.length;a++) {
      int off = iface_cls[a].getMethodObjOffset(name_desc, false);
      if (off != -1) return off;
    }
    if (first) throw new Exception("Error:getMethodObjOffset():Can not find method:" + name + "." + name_desc);
    return -1;
  }
  public int getMethodObjOffset(String name_desc) throws Exception {
    return getMethodObjOffset(name_desc, true);
  }
  private int getMethodClsOffset(String name_desc, boolean first) throws Exception {
    int cnt = MethodList.length;
    for(int a=0;a<cnt;a++) {
      Method method = MethodList[a];
      if (method.name_desc.equals(name_desc)) {
        if (method.clsidx == -1) {
          return super_cls.getMethodObjOffset(name_desc, false);
        }
//        System.out.println("offset=" + method.offset);
        return method.clsidx;
      }
    }
    if (super_cls != null) {
      int off = super_cls.getMethodClsOffset(name_desc, false);
      if (off != -1) return off;
    }
    for(int a=0;a<iface_cls.length;a++) {
      int off = iface_cls[a].getMethodClsOffset(name_desc, false);
      if (off != -1) return off;
    }
    if (first) throw new Exception("Error:getMethodClsOffset():Can not find method:" + name + "." + name_desc);
    return -1;
  }
  public int getMethodClsOffset(String name_desc) throws Exception {
    return getMethodClsOffset(name_desc, true);
  }

  private int getFieldObjOffset(String name_desc, boolean first) throws Exception {
    int cnt = FieldList.length;
    for(int a=0;a<cnt;a++) {
      Field field = FieldList[a];
      if (field.name_desc.equals(name_desc)) {
        if (field.objidx == -1) {
          return super_cls.getFieldObjOffset(name_desc, false);
        }
        return field.objidx;
      }
    }
    if (super_cls != null) {
      int off = super_cls.getFieldObjOffset(name_desc, false);
      if (off != -1) return off;
    }
    for(int a=0;a<iface_cls.length;a++) {
      int off = iface_cls[a].getFieldObjOffset(name_desc, false);
      if (off != -1) return off;
    }
    if (first) throw new Exception("Error:getFieldObjOffset():Can not find field:" + name + "." + name_desc);
    return -1;
  }
  public int getFieldObjOffset(String name_desc) throws Exception {
    return getFieldObjOffset(name_desc, true);
  }
  private int getFieldClsOffset(String name_desc, boolean first) throws Exception {
    int cnt = FieldList.length;
    for(int a=0;a<cnt;a++) {
      Field field = FieldList[a];
      if (field.name_desc.equals(name_desc)) {
        if (field.clsidx == -1) {
          return super_cls.getFieldObjOffset(name_desc, false);
        }
        return field.clsidx;
      }
    }
    if (super_cls != null) {
      int off = super_cls.getFieldClsOffset(name_desc, false);
      if (off != -1) return off;
    }
    if (first) throw new Exception("Error:getFieldClsOffset():Can not find field:" + name + "." + name_desc);
    return -1;
  }
  public int getFieldClsOffset(String name_desc) throws Exception {
    return getFieldClsOffset(name_desc, true);
  }

  private int getMethodArgsCount(String name_desc, boolean first) throws Exception {
    int cnt = MethodList.length;
    for(int a=0;a<cnt;a++) {
      Method method = MethodList[a];
      if (method.name_desc.equals(name_desc)) {
        return method.getArgsCount();
      }
    }
    if (super_cls != null) {
      int off = super_cls.getMethodArgsCount(name_desc, false);
      if (off != -1) return off;
    }
    for(int a=0;a<iface_cls.length;a++) {
      int off = iface_cls[a].getMethodArgsCount(name_desc, false);
      if (off != -1) return off;
    }
    if (first) throw new Exception("Error:getMethodArgsCount():Can not find method:" + name + "." + name_desc);
    return -1;
  }
  public int getMethodArgsCount(String name_desc) throws Exception {
    return getMethodArgsCount(name_desc, true);
  }
  /** Returns true if this class (or derived class) has non-static method. */
  private boolean hasMethod(String name, String desc) {
    int cnt = MethodList.length;
    for(int a=0;a<cnt;a++) {
      Method method = MethodList[a];
      if (method.isStatic/* || method.isInit*/) continue;
      if (method.name.equals(name) && method.desc.equals(desc)) return true;
    }
    if (super_cls != null) return super_cls.hasMethod(name, desc);
    return false;
  }
  private Field getField(String name_desc, boolean first) throws Exception {
    int cnt = FieldList.length;
    for(int a=0;a<cnt;a++) {
      Field f = FieldList[a];
      if (f.name_desc.equals(name_desc)) return f;
    }
    if (super_cls != null) {
      Field field = super_cls.getField(name_desc, false);
      if (field != null) return field;
    }
    for(int a=0;a<iface_cls.length;a++) {
      Field field = iface_cls[a].getField(name_desc, false);
      if (field != null) return field;
    }
    if (first) throw new Exception("Error:Can not find field:" + name_desc);
    return null;
  }
  public Field getField(String name_desc) throws Exception {
    return getField(name_desc, true);
  }
  public void printConstPool() {
    int cnt = ConstList.length;
    for(int a=0;a<cnt;a++) {
      System.out.println("  Const[" + a + "]=" + ConstList[a]);
    }
  }
  private boolean isNativeUnique(Method method) {
    int cnt = MethodList.length;
    for(int a=0;a<cnt;a++) {
      Method m = MethodList[a];
      if (m == method) continue;
      if (!m.isNative) continue;
      if (method.name_desc.equals(m.name_desc)) return false;
    }
    return true;
  }
  public String generateNativeSymbol(Method method) {
    if (isNativeUnique(method))
      return "Java_" + cname + "_" + method;
    else
      return "Java_" + cname + "_" + method + "__" + method.generateNativeArgs();
  }
  public String generateMethodCName(Method method) {
//    System.out.println("method:" + name + "/" + method.name);
    String method_name = method.name;
    if (name.equals("jfvm/jfvm")) {
      //special class
      if (method_name.equals("<init>")) {
        return "jfvm_init";
      }
      if (method_name.equals("<clinit>")) {
        return "jfvm_clinit";
      }
      return method.name;
    }
    if (method.isLambda) {
      int idx = method_name.lastIndexOf("$");
      return "lambda_" + method_name.substring(idx+1);
    }
    String mcname;
    String tail = "";
    if (method_name.equals("<init>")) {
      method_name = "init";
      tail = "_";
    }
    if (method_name.equals("<clinit>")) {
      method_name = "clinit";
      tail = "_";
    }
    if (method.isOverloaded) {
      mcname = cname + "_" + method_name.replaceAll("_", "_1").replaceAll("[$]", "_2").replaceAll("/", "_") + "_" + method.getArgsTypes2() + tail;
    } else {
      mcname = cname + "_" + method_name.replaceAll("_", "_1").replaceAll("[$]", "_2").replaceAll("/", "_") + tail;
    }
//    System.out.println("mname=" + mname + " -> " + mcname);
    return mcname;
  }
}

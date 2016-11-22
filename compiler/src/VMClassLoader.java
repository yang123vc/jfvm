import java.io.*;
import java.util.Arrays;

public class VMClassLoader {
  public VMClassLoader() {}
  public boolean DEBUG = true;
  public VMClass load(InputStream is, String filename) {
//    msg("Reading class : " + filename);
    cls = new VMClass();
    dis = new DataInputStream(new BufferedInputStream(is));
    try {
      if (dis.readInt() != 0xCAFEBABE) {
        msg("Bad magic");
        return null;
      }
      short minor = dis.readShort();  //minor_version
      short major = dis.readShort();  //major_version
      //msg("Version = " + major + "." + minor);
      int cc = dis.readShort();  //constant count + 1
      if (cc<2) {
        msg("No constant pool");
        return null;
      }
      //msg("Constant Pool Size=" + cc);
      cls.ConstList = new Const[cc];
      for(int a=1;a<cc;a++) {
        cls.ConstList[a] = readConst();
        if (cls.ConstList[a] == null) {
          msg("Error:failed to read const:"+a);
          return null;
        }
        if (cls.ConstList[a].x2) {
          //long and double use two slots
          a++;
        }
      }
      cls.access_flags = dis.readShort();
      cls.isInterface = (cls.access_flags & VMClass.ACC_INTERFACE) != 0;
      cls.this_class = dis.readShort();
      ConstClass _cc = (ConstClass)cls.ConstList[cls.this_class];
      cls.name = cls.getConstString(_cc.idx);
      cls.cname = "java_" + cls.name.replaceAll("_", "_1").replaceAll("[$]", "_2").replaceAll("/", "_");
      cls.super_class = dis.readShort();
      if (cls.super_class != 0) {
        _cc = (ConstClass)cls.ConstList[cls.super_class];
        cls.super_name = cls.getConstString(_cc.idx);
      }
      int ic = dis.readShort();  //interface count
      cls.InterfaceList = new Interface[ic];
      for(int a=0;a<ic;a++) {
        cls.InterfaceList[a] = readInterface();
      }
      int fc = dis.readShort();  //field count
//      System.out.println("fields:" + fc);
      cls.FieldList = new Field[fc];
      for(int a=0;a<fc;a++) {
        cls.FieldList[a] = readField();
      }
      int mc = dis.readShort();  //method count
//      System.out.println("methods:" + mc);
      cls.MethodList = new Method[mc];
      boolean hasclinit = false;
      for(int a=0;a<mc;a++) {
        cls.MethodList[a] = readMethod();
        String name_desc = cls.MethodList[a].name_desc;
        if (name_desc.equals("<clinit>()V")) hasclinit = true;
      }
      for(int a=0;a<mc;a++) {
        for(int b=0;b<mc;b++) {
          if (a == b) continue;
          if (cls.MethodList[a].name.equals(cls.MethodList[b].name)) {
            cls.MethodList[a].isOverloaded = true;
            cls.MethodList[b].isOverloaded = true;
          }
        }
      }
      if (!cls.isInterface) {
        if (!hasclinit) {
          cls.MethodList = Arrays.copyOf(cls.MethodList, ++mc);
          Method method = new Method();
          method.name = "<clinit>";
          method.desc = "()V";
          method.name_desc = "<clinit>()V";
          method.isStatic = true;
          method.isVoid = true;
          method.calcCounts();
          cls.MethodList[mc-1] = method;
        }
      }
      for(int a=0;a<mc;a++) {
        cls.MethodList[a].cname = cls.generateMethodCName(cls.MethodList[a]);
      }
      int ac = dis.readShort();  //attributes count
      cls.AttributeList = new Attribute[ac];
      for(int a=0;a<ac;a++) {
        cls.AttributeList[a] = readAttribute();
      }
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    return cls;
  }
  private DataInputStream dis;
  private VMClass cls;
  private void msg(String str) { if (DEBUG) System.out.println(str); }
  private String decodeUTF8(byte u[]) {
    try {
      return new String(u, "UTF-8");
    } catch (Exception e) {
      return null;
    }
  }
  private Const readConst() throws Exception {
    int type = dis.readByte();
//    System.out.println("const:" + type);
    switch (type) {
      case 1: //UTF8
        ConstUTF8 c1 = new ConstUTF8();
        int len = dis.readShort();
        byte tmp[] = new byte[len];
        int read = dis.read(tmp, 0, len);
        if (read != len) {
          msg("Error:Failed to read string:");
        }
        c1.type = type;
        c1.str = decodeUTF8(tmp);
        if (c1.str == null) {
          //bad string
          msg("Error:Unable to decode constant string");
          return null;
        }
//        msg("String=" + c1.str);
        return c1;
      case 3: //int
        ConstInteger c3 = new ConstInteger();
        c3.type = type;
        c3.val = dis.readInt();
        return c3;
      case 4: //float
        ConstFloat c4 = new ConstFloat();
        c4.type = type;
        c4.val = dis.readFloat();
        return c4;
      case 5: //long
        ConstLong c5 = new ConstLong();
        c5.type = type;
        c5.val = dis.readLong();
        c5.x2 = true;
        return c5;
      case 6: //double
        ConstDouble c6 = new ConstDouble();
        c6.type = type;
        c6.val = dis.readDouble();
        c6.x2 = true;
        return c6;
      case 7: //class idx
        ConstClass c7 = new ConstClass();
        c7.type = type;
        c7.idx = dis.readShort();
        return c7;
      case 8: //string idx
        ConstString c8 = new ConstString();
        c8.type = type;
        c8.idx = dis.readShort();
        return c8;
      case 9: //field ref (idx,idx)
        ConstFieldRef c9 = new ConstFieldRef();
        c9.type = type;
        c9.cls_idx = dis.readShort();
        c9.name_type_idx = dis.readShort();
        return c9;
      case 10: //method ref (idx,idx)
        ConstMethodRef c10 = new ConstMethodRef();
        c10.type = type;
        c10.cls_idx = dis.readShort();
        c10.name_type_idx = dis.readShort();
        return c10;
      case 11: //interface ref (idx,idx)
        ConstMethodRef c11 = new ConstMethodRef();
        c11.type = type;
        c11.cls_idx = dis.readShort();
        c11.name_type_idx = dis.readShort();
        return c11;
      case 12: //name_desc (idx,idx)
        ConstNameType c12 = new ConstNameType();
        c12.type = type;
        c12.name_idx = dis.readShort();
        c12.desc_idx = dis.readShort();
        return c12;
      case 15:  //method handle
        dis.readByte();  //reference_kind
        dis.readShort();  //reference_index
        return new Const(15);
      case 16:  //method type
        dis.readShort();  //desc_index
        return new Const(16);
      case 18:  //invoke dynamic
        ConstDynamic c18 = new ConstDynamic();
        c18.type = type;
        c18.bootstrap_method_attr_index = dis.readShort();
        c18.name_and_type_index = dis.readShort();
        return c18;
      default:
        System.out.println("Error:Unknown const:" + type);
        break;
    }
    return null;
  }
  private Interface readInterface() throws Exception {
    int id = dis.readShort();
    Const cx = cls.ConstList[id];
    if (cx.type != 7) return null;  //bad index
    Interface i = new Interface();
    ConstClass cc = (ConstClass)cx;
    i.idx = cc.idx;
    i.name = cls.getConstString(i.idx);
    return i;
  }
  private Field readField() throws Exception {
    Field field = new Field();
    field.flgs = dis.readShort();
    field.name_idx = dis.readShort(); field.name = cls.getConstString(field.name_idx);
    field.desc_idx = dis.readShort(); field.desc = cls.getConstString(field.desc_idx);
    field.name_desc = field.name + "$" + field.desc;
    field.attr_cnt = dis.readShort();
    field.isStatic = (field.flgs & Field.ACC_STATIC) != 0;
    if (field.attr_cnt > 0) {
      field.attrs = new Attribute[field.attr_cnt];
      for(int a=0;a<field.attr_cnt;a++) {
        field.attrs[a] = readAttribute();
      }
    }
    return field;
  }
  private boolean isLambda(String name) {
    //lambda methods : class.interface_name$method_defined_in$idx
    // - this idx is unique to the class
    if (name.startsWith("access")) return false;
    int idx = name.lastIndexOf("$");
    if (idx == -1) return false;
    if (idx+1 == name.length()) return false;
    char ch = name.charAt(idx+1);
    return (ch >= '0' && ch <= '9');
  }
  private Method readMethod() throws Exception {
    Method method = new Method();
    method.flgs = dis.readShort();
    method.name_idx = dis.readShort(); method.name = cls.getConstString(method.name_idx);
    method.desc_idx = dis.readShort(); method.desc = cls.getConstString(method.desc_idx);
    method.attr_cnt = dis.readShort();
    method.name_desc = method.name + method.desc;
    method.isLambda = isLambda(method.name);
    method.isStatic = (method.flgs & Method.ACC_STATIC) != 0;
    method.isSync = (method.flgs & Method.ACC_SYNCHRONIZED) != 0;
    method.isNative = (method.flgs & Method.ACC_NATIVE) != 0;
    method.isAbstract = (method.flgs & Method.ACC_ABSTRACT) != 0;
    method.isVoid = method.desc.endsWith("V");
    if (method.attr_cnt > 0) {
      method.attrs = new Attribute[method.attr_cnt];
      for(int a=0;a<method.attr_cnt;a++) {
        method.attrs[a] = readAttribute();
      }
    }
    method.calcCounts();
    return method;
  }
  private Attribute readAttribute() throws Exception {
    short attr_name_idx = dis.readShort();
    int attr_len = dis.readInt();
    //determin attribute type from name_idx
    String type = cls.getConstString(attr_name_idx);
//    System.out.println("attribute:" + type);
    if (type.equals("ConstantValue")) {
      if (attr_len != 2) return null;  //bad attr
      AttrConst attr = new AttrConst();
      attr.attr_name_idx = attr_name_idx;
      attr.attr_len = attr_len;
      attr.const_idx = dis.readShort();
      return attr;
    }
    if (type.equals("Code")) {
      AttrCode attr = new AttrCode();
      attr.attr_name_idx = attr_name_idx;
      attr.attr_len = attr_len;
      attr.max_stack = dis.readShort();
      attr.max_locals = dis.readShort();
      attr.code_len = dis.readInt();
      attr.code = new byte[attr.code_len];
      dis.read(attr.code);
      attr.exception_count = dis.readShort();
      attr.exceptions = new CodeException[attr.exception_count];
      for(int a=0;a<attr.exception_count;a++) {
        CodeException x = new CodeException();
        x.start_pc = dis.readShort();
        x.end_pc = dis.readShort();
        x.handler_pc = dis.readShort();
        x.catch_type = dis.readShort();
        if (x.catch_type == 0) {
          x.catch_cls = "java/lang/Throwable";  //this will catch ALL exceptions
        } else {
          ConstClass cc = (ConstClass)cls.ConstList[x.catch_type];
          x.catch_cls = cls.getConstString(cc.idx);
        }
        attr.exceptions[a] = x;
      }
      attr.attr_cnt = dis.readShort();
      if (attr.attr_cnt > 0) {
        attr.attrs = new Attribute[attr.attr_cnt];
        for(int a=0;a<attr.attr_cnt;a++) {
          attr.attrs[a] = readAttribute();
        }
      }
      return attr;
    }
    if (type.equals("Exceptions")) {
      AttrExceptions attr = new AttrExceptions();
      attr.attr_name_idx = attr_name_idx;
      attr.attr_len = attr_len;
      attr.cnt = dis.readShort();
      attr.exception_index_table = new short[attr.cnt];
      for(int a=0;a<attr.cnt;a++) {
        attr.exception_index_table[a] = dis.readShort();
      }
      return attr;
    }
    if (type.equals("InnerClasses")) {
      AttrInnerClasses attr = new AttrInnerClasses();
      attr.attr_name_idx = attr_name_idx;
      attr.attr_len = attr_len;
      attr.cnt = dis.readShort();
      attr.classes = new InnerClass[attr.cnt];
      for(int a=0;a<attr.cnt;a++) {
        InnerClass cls = new InnerClass();
        cls.inner_class_access_flags = dis.readShort();
        cls.outer_class_info_index = dis.readShort();
        cls.inner_name_index = dis.readShort();
        cls.inner_class_access_flags = dis.readShort();
        attr.classes[a] = cls;
      }
      return attr;
    }
    //unknown attribute
    Attribute attr = new Attribute();
    attr.attr_name_idx = attr_name_idx;
    attr.attr_len = attr_len;
    byte tmp[] = new byte[attr.attr_len];
    dis.read(tmp);
    return attr;
  }
}

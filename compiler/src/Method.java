/**
 *
 * @author pquiring
 */

public class Method {
  public short flgs;
  public short name_idx; public String name; public String cname;
  public short desc_idx; public String desc;  //(args)returnType
  public short attr_cnt;
  public Attribute[] attrs;
  public String name_desc;

  public boolean isStatic;
  public boolean isSync;
  public boolean isOverrides;
  public boolean isOverloaded;
  public boolean isNative;
  public boolean isAbstract;
  public boolean isVoid;
  public boolean isLambda;
  public int clsidx;  //Class.methods and Class.static_methods index
  public int objidx;  //Object.methods[] index
  public int argsCount;
  public int local;  //# of locals (AttrCode.max_local)
  public int stack;  //# of stack (AttrCode.max_stack)
  public int xlocal;  //# of locals beyond # args

  public AttrCode getCode() throws Exception {
    for(int a=0;a<attr_cnt;a++) {
      if (attrs[a] instanceof AttrCode) {
        return (AttrCode)attrs[a];
      }
    }
    if (name_desc.equals("<clinit>()V")) {
      AttrCode ac = new AttrCode();
      ac.max_locals = 0;
      ac.max_stack = 0;
      ac.code = new byte[0];
      return ac;
    }
    throw new Exception("Error:Method does not have bytecode:" + name_desc);
  }
  /** Returns number of args (including "this"). */
  public int getArgsCount() {
    return argsCount;
  }
  public void calcCounts() throws Exception {
    argsCount = 0;
    char ca[] = desc.substring(1,desc.indexOf(")")).toCharArray();
    for(int a=0;a<ca.length;a++) {
      if (ca[a] == '[') {
        continue;
      }
      if (ca[a] == 'L') {
        argsCount++;
        while (ca[a] != ';') a++;
      } else {
        argsCount++;
      }
    }
    if (!isStatic) argsCount++;  //this
    if (isNative) return;
    if (isAbstract) return;
    AttrCode code = getCode();
    xlocal = code.max_locals - argsCount;
    local = code.max_locals;
    stack = code.max_stack;
  }
  public boolean isStatic() {
    return isStatic;
  }
  public boolean isVoid() {
    return isVoid;
  }
  public String getArgsTypes() {
    StringBuffer sb = new StringBuffer();
    for(int a=1;a<desc.length();a++) {
      char c = desc.charAt(a);
      if (c == ')') break;
      sb.append(c);
      if (c == 'L') {
        while (desc.charAt(a) != ';') a++;
      }
    }
    return sb.toString();
  }
  public String getArgsTypes2() {
    StringBuffer sb = new StringBuffer();
    for(int a=1;a<desc.length();a++) {
      char c = desc.charAt(a);
      if (c == ')') break;
      if (c == ';') continue;
      if (c == '/') c = '_';
      if (c == '[') c = 'A';
      sb.append(c);
    }
    return sb.toString();
  }
  /** Returns Java type. */
  public String getReturnType() {
    int idx = desc.indexOf(")");
    return desc.substring(idx+1);
  }
  /** Returns return type in "C" style. */
  public String getReturnCType() {
    char type = getReturnType().charAt(0);
    switch (type) {
      case '[':
      case 'L': return "Object*";
      case 'Z': return "jboolean";
      case 'B': return "jbyte";
      case 'C': return "jchar";
      case 'S': return "jshort";
      case 'I': return "jint";
      case 'J': return "jlong";
      case 'F': return "jfloat";
      case 'D': return "jdouble";
      case 'V': return null;
    }
    return "?";
  }
  /** Returns return type in Slot->name */
  public String getReturnCName() {
    char type = getReturnType().charAt(0);
    switch (type) {
      case '[':
      case 'L': return "obj";
      case 'Z':
      case 'B': return "i8";
      case 'C': return "u16";
      case 'S': return "i16";
      case 'I': return "i32";
      case 'J': return "i64";
      case 'F': return "f32";
      case 'D': return "f64";
      case 'V': return null;
    }
    return "?";
  }
  /** Returns VM type (objects are 'A'). */
  public char getReturnType2() {
    int idx = desc.indexOf(")");
    char ret = desc.charAt(idx+1);
    if (ret == '[') ret = 'A';
    if (ret == 'L') ret = 'A';
    return ret;
  }
  public String generateNativeArgs() {
    StringBuffer sb = new StringBuffer();
    char ca[] = desc.replaceAll("_", "_1").toCharArray();
    //ca[0] = '('
    for(int a=1;a<ca.length;a++) {
      char c = ca[a];
      if (c == ')') break;
      if (c == '[') {
        sb.append("_3");
        continue;
      }
      if (c == ';') {
        sb.append("_2");
        continue;
      }
      sb.append(c);
    }
    return sb.toString();
  }

  public static int ACC_PUBLIC = 0x0001;
  public static int ACC_PRIVATE = 0x0002;
  public static int ACC_PROTECTED = 0x0004;
  public static int ACC_STATIC = 0x0008;
  public static int ACC_FINAL = 0x0010;
  public static int ACC_SYNCHRONIZED = 0x0020;
  public static int ACC_VOLATILE = 0x0040;
  public static int ACC_TRANSIENT = 0x0080;
  public static int ACC_NATIVE = 0x0100;
  public static int ACC_ABSTRACT = 0x0400;
  public static int ACC_STRICT = 0x0800;
  public static int ACC_SYNTHETIC = 0x1000;
}


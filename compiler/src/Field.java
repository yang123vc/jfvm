/**
 *
 * @author pquiring
 */

public class Field {

  short flgs;
  short name_idx;  String name;
  short desc_idx;  String desc;
  String name_desc;
  short attr_cnt;
  Attribute[] attrs;

  public boolean isStatic;
  public int clsidx;  //offset within Class.fields[] or Class.static_fields[]
  public int objidx;  //offset with Object.fields[] (not used with static fields)

  //flags
  public static int ACC_PUBLIC = 0x0001;
  public static int ACC_PRIVATE = 0x0002;
  public static int ACC_PROTECTED = 0x0004;
  public static int ACC_STATIC = 0x0008;
  public static int ACC_FINAL = 0x0010;
  public static int ACC_VOLATILE = 0x0040;
  public static int ACC_TRANSIENT = 0x0080;
  public static int ACC_SYNTHETIC = 0x1000;
  public static int ACC_ENUM = 0x4000;

  public boolean is64() {
    switch (desc.charAt(0)) {
      case 'J':
      case 'D':
        return true;
      default:
        return false;
    }
  }
}

/**
 *
 * @author pquiring
 */

public class Const {
  public int type;
  public boolean x2;  //ConstantPool Slot is long or double (takes up two slots)
  public Const() {}
  public Const(int type) {
    this.type = type;
  }
}

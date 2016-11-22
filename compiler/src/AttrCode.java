/**
 *
 * @author pquiring
 */

public class AttrCode extends Attribute {
  short max_stack;
  short max_locals;
  int code_len;
  byte[] code; //code_len bytes
  short exception_count;
  CodeException[] exceptions; //exception_count
  short attr_cnt;
  Attribute[] attrs;
}

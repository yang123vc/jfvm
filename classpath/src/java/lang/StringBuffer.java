package java.lang;

/** StringBuffer
 *
 * @author pquiring
 */

public class StringBuffer {
  private char value[] = new char[0];

  public String toString() {
    return new String(value);
  }

  public StringBuffer append(String str) {
    char append[] = str.getChars();
    char newvalue[] = new char[value.length + append.length];
    System.arraycopy(value, 0, newvalue, 0, value.length);
    System.arraycopy(append, 0, newvalue, value.length, append.length);
    value = newvalue;
    return this;
  }

  public StringBuffer append(Object obj) {
    return append(obj.toString());
  }

  public StringBuffer append(char ch) {
    char newvalue[] = new char[value.length + 1];
    System.arraycopy(value, 0, newvalue, 0, value.length);
    newvalue[value.length] = ch;
    value = newvalue;
    return this;
  }

  public StringBuffer append(int val) {
    return append(Integer.toString(val));
  }

  public StringBuffer append(long val) {
    return append(Long.toString(val));
  }

  public StringBuffer append(float ival) {
    return append(Float.toString(ival));
  }

  public StringBuffer append(double ival) {
    return append(Double.toString(ival));
  }
}

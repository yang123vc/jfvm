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

  public StringBuffer append(char ch) {
    char newvalue[] = new char[value.length + 1];
    System.arraycopy(value, 0, newvalue, 0, value.length);
    newvalue[value.length] = ch;
    value = newvalue;
    return this;
  }

  public StringBuffer append(int ival) {
    return append(Integer.toString(ival, 10));
  }
}

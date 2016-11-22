package java.lang;

/** StringBuilder
 *
 * @author pquiring
 */

public class StringBuilder {
  private char value[] = new char[0];

  public String toString() {
    return new String(value);
  }

  public StringBuilder append(String str) {
    char append[] = str.getChars();
    char newvalue[] = new char[value.length + append.length];
    System.arraycopy(value, 0, newvalue, 0, value.length);
    System.arraycopy(append, 0, newvalue, value.length, append.length);
    value = newvalue;
    return this;
  }

  public StringBuilder append(char ch) {
    char newvalue[] = new char[value.length + 1];
    System.arraycopy(value, 0, newvalue, 0, value.length);
    newvalue[value.length] = ch;
    value = newvalue;
    return this;
  }

  public StringBuilder append(int ival) {
    return append(Integer.toString(ival, 10));
  }
}

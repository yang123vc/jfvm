package java.lang;

public class String {
  public final char[] value;
  public String() {
    value = null;
  }
  public String(byte s[]) {
    if (s == null) {
      value = null;
      return;
    }
    int len = s.length;
    value = new char[len];
    for(int a=0;a<len;a++) {
      value[a] = (char)s[a];
    }
  }
  public String(char s[]) {
    value = new char[s.length];
    System.arraycopy(s, 0, value, 0, s.length);
  }
  public String(char s[], int off, int length) {
    value = new char[length];
    System.arraycopy(s, off, value, 0, length);
  }
  public char[] getChars() {
    return value;
  }
  public byte[] getBytes() {
    int len = value.length;
    byte ba[] = new byte[value.length];
    for(int a=0;a<len;a++) {
      ba[a] = (byte)value[a];
    }
    return ba;
  }
}

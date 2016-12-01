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
  public String(byte s[], String charset) {
    this(s);
  }
  public String(char s[]) {
    value = new char[s.length];
    System.arraycopy(s, 0, value, 0, s.length);
  }
  public String(char s[], int off, int length) {
    value = new char[length];
    System.arraycopy(s, off, value, 0, length);
  }
  public char charAt(int idx) {
    return value[idx];
  }
  public boolean endsWith(String str) {
    if (str.value.length > value.length) return false;
    return strcmp(value, value.length - str.value.length, str.value, 0, str.value.length);
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
  public int indexOf(char ch) {
    int cnt = value.length;
    for(int i=0;i<cnt;i++) {
      if (value[i] == ch) return i;
    }
    return -1;
  }
  private boolean strcmp(char s1[], int s1off, char s2[], int s2off, int length) {
    while (length > 0) {
      if (s1[s1off++] != s2[s2off++]) return false;
      length--;
    }
    return true;
  }
  private boolean stricmp(char s1[], int s1off, char s2[], int s2off, int length) {
    while (length > 0) {
      if (Character.toLowerCase(s1[s1off++]) != Character.toLowerCase(s2[s2off++])) return false;
      length--;
    }
    return true;
  }
  public int indexOf(String str) {
    char cmp[] = str.value;
    int len = cmp.length;
    int cnt = value.length - len;
    for(int i=0;i<cnt;i++) {
      if (strcmp(value, i, cmp, 0, len)) return i;
    }
    return -1;
  }
  public int lastIndexOf(char ch) {
    for(int i=value.length-1;i>=0;i--) {
      if (value[i] == ch) return i;
    }
    return -1;
  }
  public int lastIndexOf(String str) {
    char cmp[] = str.value;
    int len = cmp.length;
    int cnt = value.length - len;
    for(int i=cnt;i>=0;i--) {
      if (strcmp(value, i, cmp, 0, len)) return i;
    }
    return -1;
  }
  public int length() {
    return value.length;
  }
  public String replaceAll(String regex, String with) {
    StringBuffer sb = new StringBuffer();
    char cmp[] = regex.value;
    int cmplen = cmp.length;
    int strlen = value.length;
    int cnt = strlen - cmplen;
    for(int i=0;i<cnt;i++) {
      //TODO : use regex properly
      if (strcmp(value, i, cmp, 0, cmplen)) {
        sb.append(with);
      } else {
        sb.append(value[i]);
      }
    }
    for(int i=cnt;i<strlen;i++) {
      sb.append(value[i]);
    }
    return sb.toString();
  }
  public boolean startsWith(String str) {
    if (str.value.length > value.length) return false;
    return strcmp(value, 0, str.value, 0, str.value.length);
  }
  public String substring(int sidx) {
    int length = value.length - sidx;
    char array[] = new char[length];
    System.arraycopy(value, sidx, array, 0, length);
    return new String(array);
  }
  public String substring(int sidx, int eidx) {
    int length = eidx - sidx;
    if (length == -1) return null;
    char array[] = new char[length];
    System.arraycopy(value, sidx, array, 0, length);
    return new String(array);
  }
  public char[] toCharArray() {
    char copy[] = new char[value.length];
    System.arraycopy(value, 0, copy, 0, value.length);
    return copy;
  }
}

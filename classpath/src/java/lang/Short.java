package java.lang;

/** Short */

public class Short {
  private short value;
  public short getByte() {
    return value;
  }
  public static String toString(short value) {
    return Integer.toString(value, 10);
  }
  public static String toString(short value, int radix) {
    return Integer.toString(value, radix);
  }
}

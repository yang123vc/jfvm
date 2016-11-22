package java.lang;

public class Byte {
  private byte value;
  public byte getByte() {
    return value;
  }
  public static String toString(byte value) {
    return Integer.toString(value, 10);
  }
  public static String toString(byte value, int radix) {
    return Integer.toString(value, radix);
  }
}

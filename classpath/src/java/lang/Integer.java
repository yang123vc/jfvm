package java.lang;

/** Integer
 *
 * @author pquiring
 */

public class Integer {
  private int value;

  public Integer(int value) {
    this.value = value;
  }

  public int getInt() {
    return value;
  }

  private static char hex[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
  public static String toString(int value, int radix) {
    char ch[] = new char[11];
    int chs = 0;
    boolean neg = false;
    if (value < 0) {
      neg = true;
      value *= -1;
    }
    while (value > 0) {
      int mod = value % radix;
      ch[chs++] = hex[mod];
      value /= radix;
    }
    if (neg) {
      ch[chs++] = '-';
    }
    //invert chars
    int half = chs/2;
    int last = chs - 1;
    for(int i=0;i<half;i++) {
      char i1 = ch[i];
      char i2 = ch[last - i];
      ch[i] = i2;
      ch[last - i] = i1;
    }
    return new String(ch, 0, chs);
  }
  public static String toString(int value) {
    return toString(value, 10);
  }
}

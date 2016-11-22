package java.lang;

/** Long
 *
 * @author pquiring
 */

public class Long {
  private long value;

  public Long(int value) {
    this.value = value;
  }

  public long getInt() {
    return value;
  }

  private static char hex[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
  public static String toString(long value, int radix) {
    char ch[] = new char[21];
    int chs = 0;
    boolean neg = false;
    if (value < 0) {
      neg = true;
      value *= -1;
    }
    while (value > 0) {
      int mod = (int)(value % radix);
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
}

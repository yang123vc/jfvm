package java.lang;

/** Double
 *
 * @author pquiring
 */

public class Double {
  private double value;

  public Double(double value) {
    this.value = value;
  }

  public double getFloat() {
    return value;
  }

  private static char hex[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
  public static String toString(double value) {
    char ch[] = new char[128];
    int chs = 0;
    boolean neg = false;
    if (value < 0) {
      neg = true;
      value *= -1.0;
    }
    while (value > 0.0) {
      int mod = (int)(value % 10.0);
      ch[chs++] = hex[mod];
      value /= 10.0;
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

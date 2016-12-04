package java.lang;

/** Float
 *
 * @author pquiring
 */

public class Float {
  private float value;

  public Float(float value) {
    this.value = value;
  }

  public float getFloat() {
    return value;
  }

  private static char hex[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
  public static String toString(float value) {
    char ch[] = new char[128];
    int chs = 0;
    boolean neg = false;
    if (value < 0) {
      neg = true;
      value *= -1.0;
    }
    while (value > 0.0f) {
      int mod = (int)(value % 10.0f);
      ch[chs++] = hex[mod];
      value /= 10.0f;
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

  public native static float intBitsToFloat(int bits);
  public native static int floatToIntBits(float bits);
}

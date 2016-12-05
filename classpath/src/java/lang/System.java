package java.lang;

/** System
 *
 * @author pquiring
 */

import java.io.*;

public class System {

  static {
    try {
      in = getStdIn();
      out = new PrintStream(getStdOut());
      err = new PrintStream(getStdErr());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static InputStream in;
  public static PrintStream out;
  public static PrintStream err;

  private static native InputStream getStdIn();
  private static native OutputStream getStdOut();
  private static native OutputStream getStdErr();

  /** Copies an array. */
  public static native void arraycopy(Object src, int srcPos, Object dest, int destPos, int length);
  /** Copies an array in reverse order. */
  public static native void rarraycopy(Object src, int srcPos, Object dest, int destPos, int length);

  public static native String getenv(String name);

  public static native void exit(int rv);

  /** Triggers debug breakpoint. */
  public static native void debug();
}

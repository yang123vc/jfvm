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

  public static native void arraycopy(Object src, int srcPos, Object dest, int destPos, int length);
}

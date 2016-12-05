package java.lang;

/** System
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

public class System {

  public static InputStream in;
  public static PrintStream out;
  public static PrintStream err;

  private static Properties props = new Properties();

  static {
    try {
      in = getStdIn();
      out = new PrintStream(getStdOut());
      err = new PrintStream(getStdErr());
    } catch (Exception e) {
      e.printStackTrace();
    }
    setProperty("file.separator", File.separator);
    setProperty("java.vendor", "jfvm");
    setProperty("line.separator", (File.separatorChar == '/' ? "\n" : "\r\n"));
    setProperty("path.separator", File.pathSeparator);
    setProperty("user.dir", File.getdir());
    setProperty("user.home", getUserHome());
    setProperty("user.name", getUserName());
  }

  private static native InputStream getStdIn();
  private static native OutputStream getStdOut();
  private static native OutputStream getStdErr();

  private static native String getUserHome();
  private static native String getUserName();

  /** Copies an array. */
  public static native void arraycopy(Object src, int srcPos, Object dest, int destPos, int length);
  /** Copies an array in reverse order. */
  public static native void rarraycopy(Object src, int srcPos, Object dest, int destPos, int length);

  public static native String getenv(String name);

  public static native void exit(int rv);

  public static void setProperty(String name, String value) {
    props.setProperty(name, value);
  }

  public static String getProperty(String name) {
    return props.getProperty(name);
  }

  public static void clearProperty(String name) {
    props.clearProperty(name);
  }

  /** Triggers debug breakpoint. */
  public static native void debug();
}

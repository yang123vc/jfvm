package java.io;

/** PrintStream
 *
 * @author pquiring
 */

public class PrintStream {
  private OutputStream os;
  public PrintStream(OutputStream os) {
    this.os = os;
  }
  public synchronized void println(String str) {
    try {
      os.write(str.getBytes());
      os.write("\n".getBytes());
    } catch (Exception e) {
    }
  }
  public void println(Object obj) {
    println(obj.toString());
  }
}

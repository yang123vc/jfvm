package java.lang;

/** Throwable
 *
 * @author pquiring
 */

import java.io.PrintStream;

public class Throwable {
  private String message;
  public Throwable() {}
  public Throwable(String name) {
    this.message = name;
  }
  public String getMessage() {
    return message;
  }
  public String toString() {
    if (message == null) return super.toString();
    return message;
  }
  public void printStackTrace() {
    //TODO
    System.out.println(toString());
  }
  public void printStackTrace(PrintStream out) {
    //TODO
    out.println(toString());
  }
}

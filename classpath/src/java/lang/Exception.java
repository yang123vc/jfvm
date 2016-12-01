package java.lang;

/** Exception
 *
 * @author pquiring
 */

public class Exception extends Throwable {
  private String message;
  public Exception() {}
  public Exception(String name) {
    this.message = name;
  }
  public String toString() {
    if (message == null) return super.toString();
    return message;
  }
}

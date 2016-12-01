package java.lang;

/** Throwable
 *
 * @author pquiring
 */

public class Throwable {
  private String message;
  public Throwable() {}
  public Throwable(String name) {
    this.message = name;
  }
  public String toString() {
    if (message == null) return super.toString();
    return message;
  }
  public void printStackTrace() {
    System.out.println(toString());
  }
}

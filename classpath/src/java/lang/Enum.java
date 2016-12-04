package java.lang;

/** Enum
 *
 * @author pquiring
 */

public class Enum<E extends Enum<E>> {
  private String name;
  private int ordinal;
  public Enum(String name, int ordinal) {
    this.name = name;
    this.ordinal = ordinal;
  }
  public String name() {
    return name;
  }
  public static <T extends Enum<T>> T valueOf(Class<T> cls, String name) {
    return null;
  }
}

package java.util;

/** Enumeration
 *
 * @author pquiring
 */

public interface Enumeration<E> {
  public boolean hasMoreElements();
  public E nextElement();
}

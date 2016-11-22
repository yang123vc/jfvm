package java.lang;

/** Class
 *
 * @author pquiring
 */

import java.lang.reflect.*;

public class Class<T> {
  public native String getName();
  public native T newInstance();
  public native Field[] getFields();
  public native Method[] getMethods();
}

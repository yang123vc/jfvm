package java.lang;

/** Class
 *
 * @author pquiring
 */

import java.lang.reflect.*;

public class Class<T> {
  private String name;
  private Class componentType;
  private boolean isArray;
  private byte arrayType;

  public String getName() {
    int idx = name.lastIndexOf('/');
    if (idx == -1) return name;
    return name.substring(idx+1);
  }
  public native T newInstance();
  public native Field[] getFields();
  public native Method[] getMethods();
  public boolean isArray() {
    return isArray;
  }
  public Class getComponentType() {
    return componentType;
  }
}

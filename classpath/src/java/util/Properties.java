package java.util;

/** Properties
 *
 * @author pquiring
 */

public class Properties {
  private class Property {
    String key, value;
  }
  private ArrayList<Property> list = new ArrayList<Property>();

  public String getProperty(String key) {
    int cnt = list.size();
    for(int i=0;i<cnt;i++) {
      Property prop = list.get(i);
      if (prop.key.equals(key)) return prop.value;
    }
    return null;
  }

  public void setProperty(String key, String value) {
    int cnt = list.size();
    for(int i=0;i<cnt;i++) {
      Property prop = list.get(i);
      if (prop.key.equals(key)) {
        prop.value = value;
        return;
      }
    }
    Property prop = new Property();
    prop.key = key;
    prop.value = value;
    list.add(prop);
  }

  public void clearProperty(String key) {
    int cnt = list.size();
    for(int i=0;i<cnt;i++) {
      Property prop = list.get(i);
      if (prop.key.equals(key)) {
        list.remove(i);
        return;
      }
    }
  }

}

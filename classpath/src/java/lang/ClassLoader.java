package java.lang;

/** ClassPath loader
 *
 * @author pquiring
 */

public class ClassLoader {
  /** Adds a library to the classpath. */
  public static native Object add(String filename);
  /** Removes a library from the classpath.
   * Warning : removing a library that is actively in use can crash the program.
   */
  public static native void remove(Object ref);
}

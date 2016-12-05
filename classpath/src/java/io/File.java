package java.io;

/** File
 *
 * @author pquiring
 */

public class File {
  private String path;

  public File(String path) {
    this.path = path;
  }

  public char pathSeparatorChar;
  public char separatorChar;

  public String pathSeparator;
  public String separator;

//  private native void init();

  static {
    //init();
  }

  public String getPath() {
    return path;
  }

  public String getName() {
    int idx = path.lastIndexOf(separator);
    if (idx == -1) return path;
    return path.substring(idx+1);
  }

  public native boolean chdir();  //Yes you can!
  public native boolean mkdir();
  public native boolean mkdirs();

  public native boolean exists();
}

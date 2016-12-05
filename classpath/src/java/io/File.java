package java.io;

/** File
 *
 * @author pquiring
 */

public class File {
  private String path;

  public static final char pathSeparatorChar;  // : or ;
  public static final char separatorChar;  // / or \\

  public static final String pathSeparator;
  public static final String separator;

  private native static char getPathSeparator();
  private native static char getSeparator();

  static {
    char ch[] = new char[1];
    pathSeparatorChar = getPathSeparator();
    ch[0] = pathSeparatorChar;
    pathSeparator = new String(ch);
    separatorChar = getSeparator();
    ch[0] = separatorChar;
    separator = new String(ch);
  }

  public File(String path) {
    this.path = path.replaceAll("\\\\", "/");
  }

  public File(File parent, String child) {
    this.path = parent.path + "/" + path.replaceAll("\\\\", "/");
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
  public boolean mkdirs() {
    char ca[] = path.toCharArray();
    int len = ca.length;
    for(int i=0;i<len;i++) {
      if (ca[i] == '/') {
        File sub = new File(path.substring(0, i));
        if (!sub.mkdir()) return false;
      }
    }
    if (ca[len-1] != '/') {
      if (!mkdir()) return false;
    }
    return true;
  }

  public native boolean exists();

  public native long lastModified();
}

package java.io;

/** FileInputStream
 *
 * @author pquiring
 */

public class FileInputStream extends InputStream {
  private long handle;

  public FileInputStream(String fn) throws Exception {
    open(fn);
  }

  public native void open(String fn) throws Exception;
  public native void close();

  public native int read();
  public native int read(byte[] buf);
}

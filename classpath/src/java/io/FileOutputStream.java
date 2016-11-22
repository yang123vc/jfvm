package java.io;

/** FileOutputStream
 *
 * @author pquiring
 */

public class FileOutputStream extends OutputStream {
  private long handle;

  public FileOutputStream(String fn) throws Exception {
    open(fn);
  }

  public native void open(String fn) throws Exception;
  public native void close();

  public native void write(byte val);

  public native void write(byte[] buf);
}

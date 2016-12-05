package java.io;

/** FileInputStream
 *
 * @author pquiring
 */

public class FileInputStream extends InputStream {
  private long handle;

  public FileInputStream(String fn) throws IOException {
    open(fn);
  }

  public FileInputStream(File file) throws IOException {
    open(file.getPath());
  }

  public native void open(String fn) throws IOException;
  public native void close() throws IOException;

  public native int read() throws IOException;
  public native int read(byte[] buf, int off, int length) throws IOException;

  public native int available() throws IOException;
}

package java.io;

/** FileOutputStream
 *
 * @author pquiring
 */

public class FileOutputStream extends OutputStream {
  private long handle;

  public FileOutputStream(String fn) throws IOException {
    open(fn);
  }

  public FileOutputStream(File file) throws IOException {
    open(file.getPath());
  }

  public native void open(String fn) throws IOException;
  public native void close() throws IOException;

  public native void write(byte val) throws IOException;

  public native void write(byte[] buf, int off, int length) throws IOException;
}

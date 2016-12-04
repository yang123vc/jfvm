package java.io;

/** RandomAccessFile
 *
 * @author pquiring
 */

public class RandomAccessFile {
  private long handle;
  public RandomAccessFile(String file, String mode) throws IOException {
    open(file, mode);
  }
  private native void open(String file, String mode) throws IOException;
  public native int read(byte buf[], int off, int length) throws IOException;
  public int read(byte buf[]) throws IOException {
    return read(buf, 0, buf.length);
  }
  public native void write(byte buf[], int off, int length) throws IOException;
  public void write(byte buf[]) throws IOException {
    write(buf, 0, buf.length);
  }
  public native long getFilePointer() throws IOException;
  public native void seek(long pos) throws IOException;
  public native void close() throws IOException;
  public native long length() throws IOException;
}

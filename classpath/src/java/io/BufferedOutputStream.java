package java.io;

/** BufferedOutputStream
 *
 * @author pquiring
 */

public class BufferedOutputStream {
  private OutputStream os;
  private byte buf[];
  private int size;

  public BufferedOutputStream(OutputStream os) {
    this.os = os;
    buf = new byte[1024];
  }

  public BufferedOutputStream(OutputStream os, int bufSize) {
    this.os = os;
    buf = new byte[bufSize];
  }

  public void flush() throws IOException {
    os.write(buf, 0, size);
    size = 0;
  }

  public void close() throws IOException {
    os.close();
  }

  public void write(byte val) throws IOException {
    if (size == buf.length) flush();
    buf[size++] = val;
  }

  public void write(byte array[], int offset, int length) throws IOException {
    while (length > 0) {
      if (size == buf.length) flush();
      int toCopy = buf.length - size;
      if (toCopy > length) {
        toCopy = length;
      }
      System.arraycopy(array, offset, buf, size, toCopy);
      size += toCopy;
      offset += toCopy;
      length -= toCopy;
    }
  }
}

package java.io;

/** BufferedInputStream
 *
 * @author pquiring
 */

public class BufferedInputStream {
  private InputStream is;
  private byte buf[];
  private int pos;
  private int size;

  public BufferedInputStream(InputStream is) {
    this.is = is;
    buf = new byte[1024];
    size = 0;
  }

  public BufferedInputStream(InputStream is, int bufSize) {
    this.is = is;
    buf = new byte[bufSize];
    size = 0;
  }

  private void readMore() throws IOException {
    pos = 0;
    size = is.read(buf);
    if (size <= 0) {
      size = 0;
      throw new IOException();
    }
  }

  public void close() throws IOException {
    is.close();
  }

  public int read() throws IOException {
    if (pos == size) readMore();
    return buf[pos++] & 0xff;
  }

  public int read(byte array[], int offset, int length) throws IOException {
    if (pos == size) readMore();
    int toCopy = size - pos;
    if (toCopy > length) {
      toCopy = length;
    }
    System.arraycopy(buf, pos, array, offset, toCopy);
    pos += toCopy;
    return toCopy;
  }
}

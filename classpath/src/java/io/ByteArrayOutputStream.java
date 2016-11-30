package java.io;

/** ByteArrayOutputStream
 *
 * @author pquiring
 */

public class ByteArrayOutputStream extends OutputStream {
  private byte buf[];
  private int size;
  private int pos;

  public ByteArrayOutputStream() {
    buf = new byte[1024];
    size = 1024;
    pos = 0;
  }

  public void write(byte val) throws IOException {
    if (pos == size) {
      size += 1024;
      byte newbuf[] = new byte[size];
      System.arraycopy(buf, 0, newbuf, 0, size - 1024);
      buf = newbuf;
    }
    buf[pos++] = val;
  }

  public byte[] toByteArray() {
    byte ret[] = new byte[pos];
    System.arraycopy(buf, 0, ret, 0, pos);
    return ret;
  }
}

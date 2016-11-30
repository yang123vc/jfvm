package java.io;

/** ByteArrayInputStream
 *
 * @author pquiring
 */

public class ByteArrayInputStream extends InputStream {
  private byte buf[];
  private int pos;

  public ByteArrayInputStream(byte data[]) {
    buf = data;
    pos = 0;
  }

  public int read() throws IOException {
    if (pos >= buf.length) throw new IOException();
    return buf[pos++];
  }

  public int available() {
    return buf.length - pos;
  }
}

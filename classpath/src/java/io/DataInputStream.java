package java.io;

/** DataInputStream
 *
 * MSB ordering (big endian)
 *
 * @author pquiring
 */

public class DataInputStream extends InputStream {
  private InputStream is;

  public DataInputStream(InputStream is) {
    this.is = is;
  }

  public int read() throws IOException {
    return is.read();
  }

  public short readShort() throws IOException {
    int v = read();
    v <<= 8;
    v |= read();
    return (short)v;
  }

  public int readInt() throws IOException {
    int v = read();
    v <<= 8;
    v |= read();
    v <<= 8;
    v |= read();
    v <<= 8;
    v |= read();
    return v;
  }
}

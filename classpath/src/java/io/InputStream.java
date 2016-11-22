package java.io;

/**
 *
 * @author pquiring
 */

public abstract class InputStream {
  public abstract int read();
  public int read(byte buf[], int off, int length) {
    for(int a=0;a<length;a++) {
      buf[a + off] = (byte)read();
    }
    return length;
  }
  public int read(byte buf[]) {
    return read(buf, 0, buf.length);
  };
  public void close() {}
}

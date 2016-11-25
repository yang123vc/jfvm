package java.io;

/**
 *
 * @author pquiring
 */

public abstract class InputStream {
  public abstract int read() throws IOException;
  public int read(byte buf[], int off, int length) throws IOException {
    for(int a=0;a<length;a++) {
      buf[a + off] = (byte)read();
    }
    return length;
  }
  public int read(byte buf[]) throws IOException {
    return read(buf, 0, buf.length);
  };
  public void close() throws IOException {}
  public int available() throws IOException {
    return 0;
  }
}

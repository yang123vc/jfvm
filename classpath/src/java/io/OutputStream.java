package java.io;

/**
 *
 * @author pquiring
 */

public abstract class OutputStream {
  public abstract void write(byte val) throws IOException;
  public void write(byte buf[], int off, int length) throws IOException {
    for(int a=0;a<length;a++) {
      write(buf[a+off]);
    }
  }
  public void write(byte buf[]) throws IOException {
    write(buf, 0, buf.length);
  }
  public void close() throws IOException {}
}

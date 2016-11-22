package java.io;

/**
 *
 * @author pquiring
 */

public abstract class OutputStream {
  public abstract void write(byte val);
  public void write(byte buf[], int off, int length) {
    for(int a=0;a<length;a++) {
      write(buf[a+off]);
    }
  }
  public void write(byte buf[]) {
    write(buf, 0, buf.length);
  }
  public void close() {}
}

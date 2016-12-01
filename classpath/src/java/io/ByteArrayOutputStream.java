package java.io;

/** ByteArrayOutputStream
 *
 * @author pquiring
 */

public class ByteArrayOutputStream extends OutputStream {
  private byte buf[];
  private int alloc;
  private int size;

  public ByteArrayOutputStream() {
    buf = new byte[1024];
    alloc = 1024;
    size = 0;
  }

  public void write(byte val) throws IOException {
    if (size == alloc) {
      int newAlloc = alloc + 1024;
      byte newbuf[] = new byte[newAlloc];
      System.arraycopy(buf, 0, newbuf, 0, alloc);
      buf = newbuf;
      alloc = newAlloc;
    }
    buf[size++] = val;
  }

  public void write(byte array[], int offset, int length) throws IOException {
    if (size + length > alloc) {
      int newAlloc = ((size + length + 1023) & 0xfffff800);
      byte newbuf[] = new byte[newAlloc];
      System.arraycopy(array, 0, newbuf, 0, alloc);
      array = newbuf;
      alloc = newAlloc;
    }
    System.arraycopy(array, offset, buf, size, length);
    size += length;
  }

  public byte[] toByteArray() {
    byte ret[] = new byte[size];
    System.arraycopy(buf, 0, ret, 0, size);
    return ret;
  }
}

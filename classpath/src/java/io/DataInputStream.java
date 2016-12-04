package java.io;

/** DataInputStream
 *
 * Default byte order : big endian
 *
 * @author pquiring
 */

public class DataInputStream extends InputStream {
  private InputStream is;
  public enum Order {
    LITTLE, BIG
  }
  private Order order = Order.BIG;

  public DataInputStream(InputStream is) {
    this.is = is;
  }

  public void setByteOrder(Order newOrder) {
    order = newOrder;
  }

  public Order getByteOrder() {
    return order;
  }

  public int read() throws IOException {
    return is.read();
  }

  public byte readByte() throws IOException {
    return (byte)read();
  }

  public short readShort() throws IOException {
    if (order == Order.BIG) {
      int v = read();
      v <<= 8;
      v |= read();
      return (short)v;
    } else {
      int v = read();
      v |= read() << 8;
      return (short)v;
    }
  }

  public int readInt() throws IOException {
    if (order == Order.BIG) {
      int v = read();
      v <<= 8;
      v |= read();
      v <<= 8;
      v |= read();
      v <<= 8;
      v |= read();
      return v;
    } else {
      int v = read();
      v |= read() << 8;
      v |= read() << 16;
      v |= read() << 24;
      return v;
    }
  }

  public long readLong() throws IOException {
    if (order == Order.BIG) {
      long v = read();
      v <<= 8;
      v |= read();
      v <<= 8;
      v |= read();
      v <<= 8;
      v |= read();

      v <<= 8;
      v |= read();
      v <<= 8;
      v |= read();
      v <<= 8;
      v |= read();
      v <<= 8;
      v |= read();

      return v;
    } else {
      long v = read();
      v |= read() << 8;
      v |= read() << 16;
      v |= read() << 24;

      v |= (long)(read()) << 32;
      v |= (long)(read()) << 40;
      v |= (long)(read()) << 48;
      v |= (long)(read()) << 56;

      return v;
    }
  }

  public float readFloat() throws IOException {
    int bits = readInt();
    return Float.intBitsToFloat(bits);
  }

  public double readDouble() throws IOException {
    long bits = readLong();
    return Double.longBitsToDouble(bits);
  }

  public void readFully(byte buf[], int offset, int length) throws IOException {
    int toRead = length;
    while (toRead > 0) {
      int read = read(buf, offset, toRead);
      if (read > 0) {
        offset += read;
        toRead -= read;
      }
    }
  }

  public void readFully(byte buf[]) throws IOException {
    readFully(buf, 0, buf.length);
  }
}

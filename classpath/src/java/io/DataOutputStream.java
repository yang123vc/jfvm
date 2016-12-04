package java.io;

/** DataOutputStream
 *
 * Default byte order : big endian
 *
 * @author pquiring
 */

public class DataOutputStream extends OutputStream {
  private OutputStream os;
  public enum Order {
    LITTLE, BIG
  }
  private Order order = Order.BIG;

  public DataOutputStream(OutputStream is) {
    this.os = is;
  }

  public void setByteOrder(Order newOrder) {
    order = newOrder;
  }

  public Order getByteOrder() {
    return order;
  }

  public void write(byte val) throws IOException {
    os.write(val);
  }

  public void writeByte(byte val) throws IOException {
    write(val);
  }

  public void writeShort(short val) throws IOException {
    if (order == Order.BIG) {
      write((byte)((val & 0xff00) >>> 8));
      write((byte)(val & 0x00ff));
    } else {
      write((byte)(val & 0xff));
      val >>>= 8;
      write((byte)(val & 0xff));
    }
  }

  public void writeInt(int val) throws IOException {
    if (order == Order.BIG) {
      write((byte)((val & 0xff000000) >>> 24));
      write((byte)((val & 0xff0000) >>> 16));
      write((byte)((val & 0xff00) >>> 8));
      write((byte)(val & 0x00ff));
    } else {
      write((byte)(val & 0xff));
      val >>>= 8;
      write((byte)(val & 0xff));
      val >>>= 8;
      write((byte)(val & 0xff));
      val >>>= 8;
      write((byte)(val & 0xff));
    }
  }

  public void writeLong(long val) throws IOException {
    if (order == Order.BIG) {
      write((byte)((val & 0xff00000000000000L) >>> 56));
      write((byte)((val & 0xff000000000000L) >>> 48));
      write((byte)((val & 0xff0000000000L) >>> 40));
      write((byte)((val & 0xff00000000L) >>> 32));
      write((byte)((val & 0xff000000) >>> 24));
      write((byte)((val & 0xff0000) >>> 16));
      write((byte)((val & 0xff00) >>> 8));
      write((byte)(val & 0x00ff));
    } else {
      write((byte)(val & 0xff));
      val >>>= 8;
      write((byte)(val & 0xff));
      val >>>= 8;
      write((byte)(val & 0xff));
      val >>>= 8;
      write((byte)(val & 0xff));
      val >>>= 8;
      write((byte)(val & 0xff));
      val >>>= 8;
      write((byte)(val & 0xff));
      val >>>= 8;
      write((byte)(val & 0xff));
      val >>>= 8;
      write((byte)(val & 0xff));
    }
  }

  public void writeFloat(float val) throws IOException {
    writeInt(Float.floatToIntBits(val));
  }

  public void writeDouble(double val) throws IOException {
    writeLong(Double.doubleToLongBits(val));
  }
}

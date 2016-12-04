package java.io;

/** RandomOutputStream
 *
 * @author pquiring
 */

public class RandomOutputStream {
  private RandomAccessFile raf;
  public RandomOutputStream(RandomAccessFile raf) {
    this.raf = raf;
  }
  public void write(byte val) throws IOException {
    byte data[] = new byte[1];
    data[0] = val;
    raf.write(data);
  }
  public void write(byte buf[],int offset,int length) throws IOException {
    raf.write(buf,offset,length);
  }
}

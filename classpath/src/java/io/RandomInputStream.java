package java.io;

/** RandomInputStream
 *
 * @author pquiring
 */

public class RandomInputStream extends InputStream {
  private RandomAccessFile raf;
  public RandomInputStream(RandomAccessFile raf) {
    this.raf = raf;
  }

  public int read() throws IOException {
    byte data[] = new byte[1];
    raf.read(data);
    return data[0] & 0xff;
  }
  public int read(byte buf[],int offset,int length) throws IOException {
    return raf.read(buf,offset,length);
  }
  public int available() throws IOException {
    return (int)raf.length();
  }
}

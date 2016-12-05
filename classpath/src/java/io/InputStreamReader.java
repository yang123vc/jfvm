package java.io;

/** InputStreamReader
 *
 * @author pquiring
 */

public class InputStreamReader extends Reader {
  private InputStream is;
  public InputStreamReader(InputStream is) {
    this.is = is;
  }
  public int read() throws IOException {
    return is.read();
  }
  public int read(byte buf[], int offset, int length) throws IOException {
    return is.read(buf, offset, length);
  }
  public void close() throws IOException {
    is.close();
  }
  public int available() throws IOException {
    return is.available();
  }
}

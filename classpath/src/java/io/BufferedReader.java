package java.io;

/** BufferedReader
 *
 * @author pquiring
 */

public class BufferedReader extends Reader {
  private BufferedInputStream reader;
  public BufferedReader(Reader reader) {
    this.reader = new BufferedInputStream(reader);
  }
  public int read() throws IOException {
    return reader.read();
  }
  public int read(byte buf[], int offset, int length) throws IOException {
    return reader.read(buf, offset, length);
  }
  public void close() throws IOException {
    reader.close();
  }
  public String readLine() throws IOException {
    StringBuffer sb = new StringBuffer();
    char ch;
    do {
      ch = (char)reader.read();
      if (ch == 10) break;
      if (ch != 13) sb.append(ch);
    } while (true);
    return sb.toString();
  }
}

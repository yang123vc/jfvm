package java.util.zip;

/** ZipEntry
 *
 * @author pquiring
 */

import java.util.Date;

public class ZipEntry {
  protected String name;
  protected boolean isDirectory;
  protected byte[] data;
  protected Date date = new Date();
  protected int offset;
  protected short compType;
  protected int compSize, uncompSize, crc32;

  public String getName() {
    return name;
  }
  public long getSize() {
    return data.length;
  }
  public boolean isDirectory() {
    return isDirectory;
  }
  public Date getDate() {
    return date;
  }
  public long getTime() {
    //TODO
    return 0;
  }
}

package java.util.zip;

/** ZipFile
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

public class ZipFile {
  private RandomAccessFile raf;
  private ArrayList<ZipEntry> entries;

  public ZipFile(String zip) throws IOException {
    entries = new ArrayList<ZipEntry>();
    raf = new RandomAccessFile(zip, "r");
    InputStream is = new RandomInputStream(raf);
    do {
      ZipEntry entry = ZipUtils.readFile(is);
      if (entry == null) break;
      entries.add(entry);
    } while (true);
  }

  public void close() throws IOException {
    if (raf != null) {
      raf.close();
      raf = null;
    }
  }

  public void finalize() {
    try {close();} catch (Exception e) {}
  }

  public int size() {
    return entries.size();
  }

  public Enumeration<ZipEntry> entries() {
    return new ZipEnumerator();
  }

  public InputStream getInputStream(ZipEntry entry) throws IOException {
    raf.seek(entry.offset);
    return new ByteArrayInputStream(entry.data);
  }

  private class ZipEnumerator implements Enumeration {
    int pos = 0;

    public boolean hasMoreElements() {
      return pos < entries.size();
    }

    public Object nextElement() {
      return entries.get(pos++);
    }
  }
}

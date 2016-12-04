package java.util.zip;

/** ZipUtils
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

public class ZipUtils {

  static private int crc_table[];

  static {
    crc_table = new int[256];
    for (int n = 0; n < 256; n++) {
      int c = n;
      for (int k = 8; --k >= 0; ) {
        if ((c & 1) != 0)
          c = 0xedb88320 ^ (c >>> 1);
        else
          c = c >>> 1;
      }
      crc_table[n] = c;
    }
  }

  //zlib functions
  private native static byte[] compress(byte buf[]);
  private native static byte[] decompress(byte buf[], int outSize);

  private static final int FLG_NO_SIZES = 0x0008;  //not supported
  private static final int FLG_UTF8 = 0x0800;

  private static final int COMP_STORE = 0x00;
  private static final int COMP_DEFLATE = 0x08;

  private static int crc32(byte data[]) throws IOException {
    int crc32 = 0;
    int len = data.length;
    int c = ~crc32;
    for(int i=0;i<len;i++) {
      c = crc_table[(c ^ data[i]) & 0xff] ^ (c >>> 8);
    }
    crc32 = ~c;
    return crc32;
  }

  private static short toZipTime(Date date) {
    //hhhhhmmmmmmsssss
    return (short)((date.getHours() << 11) | (date.getMinutes() << 5) | (date.getSeconds() >> 1));
  }

  private static short toZipDate(Date date) {
    //yyyyyyymmmmddddd
    return (short)(((date.getYear() - 1980) << 9) | (date.getMonth() << 5) | (date.getDay()));
  }

  private static Date toJavaDate(short zipTime, short zipDate) {
    Date date = new Date();
    //TODO
    return date;
  }

  public static ZipEntry readFile(InputStream is) throws IOException {
    DataInputStream dis = new DataInputStream(is);
    dis.setByteOrder(DataInputStream.Order.LITTLE);
    int sign = dis.readInt();
    if (sign == 0x02014b50) return null;  //start of directory
    if (sign != 0x04034b50) throw new IOException("Not a ZIP file");
    short verMin = dis.readShort();  //0x000a
    short flags = dis.readShort();
    if ((flags & FLG_NO_SIZES) != 0) throw new IOException("Unsupported ZIP file");
    short comp = dis.readShort();  //0x0008 = deflate 0x0000 = stored
    short time = dis.readShort();
    short date = dis.readShort();
    int crc32 = dis.readInt();
    int compSize = dis.readInt();
    int uncompSize = dis.readInt();
    short nameSize = dis.readShort();
    short extraSize = dis.readShort();
    //read name
    byte name[] = new byte[nameSize];
    dis.readFully(name);
    String str = new String(name);
    if (extraSize > 0) {
      byte extra[] = new byte[extraSize];
      dis.readFully(extra);
    }
    byte compData[] = new byte[compSize];
    if (compSize > 0) dis.readFully(compData);
    byte uncompData[];
    if (comp == 0x0008) {
      uncompData = decompress(compData, uncompSize);
    } else {
      uncompData = compData;
    }
    if (crc32 != 0) {
      int _crc32 = crc32(uncompData);
      if (crc32 != _crc32) {
        System.out.println("crc32 failed:" + Integer.toString(crc32, 16) + "!=" + Integer.toString(_crc32, 16));
        throw new IOException("Zip:CRC32 failed");
      }
    }
    ZipEntry entry = new ZipEntry();
    entry.name = str;
    entry.isDirectory = entry.name.endsWith("/");
    entry.date = toJavaDate(time, date);
    entry.data = uncompData;
    return entry;
  }

  /** Write an entry to a Zip file.
   * If data == null denotes a directory entry.*/
  public static ZipEntry writeFile(OutputStream os, String name, byte data[], Date date, int offset) throws IOException {
    byte comp[] = null;
    if (data != null) {
      comp = compress(data);
    }
    byte utf8[] = name.getBytes();
    DataOutputStream dos = new DataOutputStream(os);
    dos.setByteOrder(DataOutputStream.Order.LITTLE);
    dos.writeInt(0x04034b50);  //sign
    dos.writeShort((short)0x000a);  //minVer
    dos.writeShort((short)FLG_UTF8);  //flags
    short dateShort = toZipDate(date);
    short timeShort = toZipTime(date);
    dos.writeShort(timeShort);
    dos.writeShort(dateShort);
    int crc32 = crc32(data);
    dos.writeInt(crc32);
    if (data != null) {
      dos.writeInt(comp.length);
    } else {
      dos.writeInt(0);  //directory
    }
    dos.writeInt(data.length);
    dos.writeShort((short)utf8.length);
    dos.writeShort((short)0);  //extra length
    dos.write(utf8);
    if (data != null) {
      dos.write(comp);
    }
    ZipEntry entry = new ZipEntry();
    entry.name = name;
    entry.isDirectory = data == null;
    if (data == null) {
      entry.compType = COMP_STORE;
    } else {
      entry.compType = COMP_DEFLATE;
      entry.compSize = comp.length;
      entry.uncompSize = data.length;
      entry.crc32 = crc32;
    }
    entry.date = date;
    entry.offset = offset;
    return entry;
  }

  public static void writeDirectory(OutputStream os, ZipEntry entries[], int offset) throws IOException {
    DataOutputStream dos = new DataOutputStream(os);
    dos.setByteOrder(DataOutputStream.Order.LITTLE);
    int size = 0;
    int cnt = entries.length;
    for(int i=0;i<cnt;i++) {
      ZipEntry entry = entries[i];
      byte utf8[] = entry.name.getBytes();
      dos.writeInt(0x02014b50);  //sign
      dos.writeShort((short)0x000a);  //verCreator
      dos.writeShort((short)0x000a);  //verMin
      dos.writeShort((short)FLG_UTF8);  //flags
      dos.writeShort(entry.compType);  //comp type
      short timeShort = toZipTime(entry.date);
      short dateShort = toZipDate(entry.date);
      dos.writeShort(timeShort);
      dos.writeShort(dateShort);
      dos.writeInt(entry.crc32);  //crc32
      dos.writeInt(entry.compSize);  //compSize
      dos.writeInt(entry.uncompSize);  //uncompSize
      dos.writeShort((short)utf8.length);  //nameSize
      dos.writeShort((short)0);  //extraSize
      dos.writeShort((short)0);  //commentSize
      dos.writeShort((short)0);  //diskNo
      dos.writeShort((short)0);  //int attribs
      dos.writeInt(0);  //ext attribs
      dos.writeInt(entry.offset);  //offset
      dos.write(utf8);
    }
    //write EOCD
    dos.writeInt(0x06054b50);  //sign
    dos.writeShort((short)0);  //diskNo
    dos.writeShort((short)0);  //diskDir
    dos.writeShort((short)cnt);  //dirCount
    dos.writeShort((short)cnt);  //dirCountTotal
    dos.writeInt(size);  //dirSize
    dos.writeInt(offset);  //dirOffset
    dos.writeShort((short)0);  //commentSize
  }
}

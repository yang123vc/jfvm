/** jfvmc - JavaForce Virtual Machine Compiler
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class jfvmc implements ClassPool {
  public static String version = "0.1";
  public static boolean x64 = true;
  public static String bits;
  public static boolean debug = false;

  public ArrayList<VMClass> clspool = new ArrayList<VMClass>();
  private boolean cp = false;
  private String ces = "ces";  //.C filES
  private String oes = "oes";  //.O filES
  private String platform = "";
  private VMClassLoader loader = new VMClassLoader();
  public static void main(String args[]) {
    new jfvmc().run(args);
  }
  private void addJar(String file) throws Exception {
    ZipFile zf = new ZipFile(file);
    Enumeration zfs = zf.entries();
    while (zfs.hasMoreElements()) {
      ZipEntry ze = (ZipEntry)zfs.nextElement();
      String name = ze.getName();
      if (!name.endsWith(".class")) continue;
      VMClass cls = loader.load(zf.getInputStream(ze), name);
      cls.lastModified = ze.getTime();
      cls.cp = cp;
      clspool.add(cls);
    }
    zf.close();
  }
  private void addMod(String file) throws Exception {
    ZipFile zf = new ZipFile(file);
    Enumeration zfs = zf.entries();
    while (zfs.hasMoreElements()) {
      ZipEntry ze = (ZipEntry)zfs.nextElement();
      String name = ze.getName();
      if (!name.startsWith("classes/")) continue;
      if (!name.endsWith(".class")) continue;
      name = name.substring(8);  //remove classes/
      VMClass cls = loader.load(zf.getInputStream(ze), name);
      cls.lastModified = ze.getTime();
      cls.cp = cp;
      clspool.add(cls);
    }
    zf.close();
  }
  private boolean upToDate(long srcTimeStamp, String destFilename) {
    File destfile = new File(destFilename);
    if (!destfile.exists()) return false;
    return (destfile.lastModified() >= srcTimeStamp);
  }
  public void run(String args[]) {
    Compiler compiler = new Compiler();
    String out = "out.dll";
    String gcc = "gcc";
    boolean natives = false;
    boolean isWindows = false;
    ArrayList<String> nativeFiles = new ArrayList<String>();
    ArrayList<String> defines = new ArrayList<String>();
    if ((args.length == 0) || (args[0].equals("-h")) || (args[0].equals("--help"))) {
      System.out.println("Usage : jfvmc [-g] [-Dname] [--32 | --64] files... [--cp files] [--out out.dll] [--natives c_files]");
      System.out.println("  files = .class .jar .jmod");
      return;
    }
    System.out.println("jfvmc/" + version);
    try {
      for(int a=0;a<args.length;a++) {
        String arg = args[a];
        if (arg.equals("--cp")) {
          cp = true;
        } else if (arg.equals("-g")) {
          debug = true;
        } else if (arg.equals("--out")) {
          a++;
          out = args[a];
        } else if (arg.equals("--temp")) {
          a++;
          ces = args[a];
        } else if (arg.equals("--32")) {
          x64 = false;
        } else if (arg.equals("--64")) {
          x64 = true;
        } else if (arg.equals("--natives")) {
          natives = true;
        } else if (arg.startsWith("-D")) {
          defines.add(arg);
        } else {
          if (natives) {
            nativeFiles.add(args[a].replaceAll("\\\\" , "/"));
          } else {
            if (arg.endsWith(".class")) {
              //load class file
              File file = new File(arg);
              VMClass cls = loader.load(new FileInputStream(file), arg);
              cls.cp = cp;
              cls.lastModified = file.lastModified();
              clspool.add(cls);
            } else if (arg.endsWith(".jar")) {
              System.out.println("Loading:" + arg);
              addJar(arg);
            } else if (arg.endsWith(".jmod")) {
              System.out.println("Loading:" + arg);
              addMod(arg);
            } else {
              System.out.println("Unknown option:" + arg);
            }
          }
        }
      }
      if (File.pathSeparatorChar == ';') {
        isWindows = true;
        if (x64)
          gcc = "x86_64-w64-mingw32-gcc";
        else
          gcc = "i686-w64-mingw32-gcc";
        platform = "-DJFVM_WIN";
      } else {
        platform = "-DJFVM_LNX";
      }
      bits = x64 ? "64" : "32";
      System.out.println("Resolving classes");
      int cnt = clspool.size();
      for(int a=0;a<cnt;a++) {
        VMClass cls = clspool.get(a);
        cls.getSuper(this);
        cls.calcSize();
      }
      for(int a=0;a<cnt;a++) {
        VMClass cls = clspool.get(a);
        cls.calcOffsets();
      }
      for(int a=0;a<cnt;a++) {
        VMClass cls = clspool.get(a);
        cls.calcOverrideOffsets();
      }
      new File(ces).mkdir();
      new File(oes).mkdir();
      System.out.println("Generating classes");
      for(int a=0;a<cnt;a++) {
        VMClass cls = clspool.get(a);
        if (cls.cp) continue;
        String outfile = ces + "/" + cls.cname + ".c";
        if (upToDate(cls.lastModified, outfile)) continue;
        System.out.println("Generating:" + cls.name + ".c");
        FileOutputStream fos = new FileOutputStream(outfile);
        StringBuffer fields = new StringBuffer();
        compiler.startClass(this);
        fos.write(fields.toString().getBytes());
        compiler.compileClass(cls);
        for(int m=0;m<cls.MethodList.length;m++) {
          Method method = cls.MethodList[m];
          if (method.isNative || method.isAbstract) {
            continue;
          }
          compiler.compileMethod(cls, method);
        }
        String asm = compiler.finishClass();
        fos.write(asm.getBytes());
        fos.close();
      }

      //generate Dll jfvm_classes
      System.out.println("Generating dll.c");
      StringBuffer sb = new StringBuffer();

      sb.append("#include <jfvm.h>\n");

      //forward declarations
      for(int a=0;a<cnt;a++) {
        VMClass cls = clspool.get(a);
        if (cls.cp) continue;
//        if (cls.isInterface) continue;
        sb.append("extern Class " + cls.cname + ";\n");
      }

      sb.append("Class* jfvm_classes[] = {\n");
      boolean first = true;
      for(int a=0;a<cnt;a++) {
        VMClass cls = clspool.get(a);
        if (cls.cp) continue;
//        if (cls.isInterface) continue;
        if (first) {first = false;} else {sb.append(",");}
        sb.append("  &" + cls.cname + "\n");
      }
      sb.append("  ,NULL\n};\n");  //null term list
      sb.append("Class **jfvm_get_classes() {return jfvm_classes;}\n");
      FileOutputStream fos = new FileOutputStream("ces/dll.c");
      fos.write(sb.toString().getBytes());
      fos.close();

      //now compile everything
      System.out.println("Compiling classes");
      for(int a=0;a<cnt;a++) {
        VMClass cls = clspool.get(a);
        if (cls.cp) continue;
//        if (cls.isInterface) continue;
        String o = oes + "/" + cls.cname + ".o";
        if (upToDate(cls.lastModified, o)) continue;
        exec(new String[] {gcc, x64 ? "-m64" : "-m32", debug ? "-g" : "", "-DX" + bits, platform , isWindows ? "" : "-fPIC", "-Wno-incompatible-pointer-types", "-I", System.getenv("jfvm") + "/include", "-c", ces + "/" + cls.cname + ".c", "-o", o});
        if (!new File(oes + "/" + cls.cname + ".o").exists()) {
          throw new Exception("Failed to compile:" + cls.cname + ".c");
        }
      }

      //compile natives
      int ncnt = nativeFiles.size();
      if (ncnt > 0) System.out.println("Compiling natives");
      for(int a=0;a<ncnt;a++) {
        String n = nativeFiles.get(a);
        int idot = n.lastIndexOf(".");
        int ipath = n.lastIndexOf("/");
        if (ipath == -1) ipath = 0; else ipath++;
        String o = "oes/" + n.substring(ipath, idot) + ".o";
        nativeFiles.set(a, o);
        if (upToDate(new File(n).lastModified(), o)) continue;
        exec(new String[] {gcc, x64 ? "-m64" : "-m32", debug ? "-g" : "", "-DX" + bits, platform , isWindows ? "" : "-fPIC", "-Wno-incompatible-pointer-types", "-I", System.getenv("jfvm") + "/include", "-c", n, "-o", o});
        if (!new File(o).exists()) {
          throw new Exception("Failed to compile:" + n);
        }
      }

      //compile dll.c
      System.out.println("Compiling dll");
      exec(new String[] {gcc, x64 ? "-m64" : "-m32", debug ? "-g" : "", "-DX" + bits, platform , isWindows ? "" : "-fPIC", "-Wno-incompatible-pointer-types", "-I", System.getenv("jfvm") + "/include", "-c", "ces/dll.c", "-o", "oes/dll.o"});
      if (!new File(oes + "/dll.o").exists()) {
        throw new Exception("Failed to compile:dll.c");
      }

      //create dll
      System.out.println("Generating dll");
      ArrayList<String> cmd = new ArrayList<String>();
      cmd.add(gcc);
      cmd.add(x64 ? "-m64" : "-m32");
      if (debug) cmd.add("-g");
      cmd.add("-shared");
      for(int a=0;a<cnt;a++) {
        VMClass cls = clspool.get(a);
        if (cls.cp) continue;
        cmd.add("oes/" + cls.cname + ".o");
      }
      for(int a=0;a<ncnt;a++) {
        cmd.add(nativeFiles.get(a));
      }
      //these should be copied into current folder using any build.xml
      if (isWindows) {
        cmd.add("jfvm.dll.a");
      } else {
        cmd.add("jfvm.so");
      }
      cmd.add("oes/dll.o");
      cmd.add("-o");
      cmd.add(out);
      exec(cmd.toArray(new String[cmd.size()]));

      System.out.println("Done");
    } catch (Exception e) {
      e.printStackTrace(System.out);
    }
  }
  public VMClass getClass(String scls) throws Exception {
    int cnt = clspool.size();
    for(int a=0;a<cnt;a++) {
      VMClass cls = clspool.get(a);
      if (cls.name.equals(scls)) return cls;
    }
    throw new Exception("Error:Can not find class:" + scls);
  }
  private void exec(String cmd[]) throws Exception {
    String msg = "";
    for(int a=0;a<cmd.length;a++) {
      msg += cmd[a];
      msg += " ";
    }
    System.out.println("exec:" + msg);
    ProcessBuilder pb = new ProcessBuilder(cmd);
    Process p = pb.start();
    BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
    Reader rin = new Reader(stdInput);
    rin.start();
    BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
    Reader rerr = new Reader(stdError);
    rerr.start();
    p.waitFor();
    rin.join();
    rerr.join();
  }
  private class Reader extends Thread {
    public BufferedReader br;
    public Reader(BufferedReader br) {
      this.br = br;
    }
    public void run() {
      try {
        String s;
        while ((s = br.readLine()) != null) {
          System.out.println(s);
        }
      } catch (Exception e) {
        e.printStackTrace(System.out);
      }
    }
  }
}

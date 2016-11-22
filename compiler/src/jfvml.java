/** jfvml - JavaForce Virtual Machine Linker
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

public class jfvml {
  public static String version = "0.1";
  private String ces = "ces";  //.C filES
  private String oes = "oes";  //.O filES
  public static void main(String args[]) {
    new jfvml().run(args);
  }
  private void usage() {
    System.out.println("Usage : jfvml [--32 | --64] libraries --main class [--out out.exe]");
    System.out.println("  libraries = .dll | .so files");
    System.exit(0);
  }
  public void run(String args[]) {
    String main = null;
    String out = "out";
    String gcc = "gcc";
    boolean x64 = true;
    boolean isWindows = false;
    boolean debug = false;
    String bits;
    String ext = ".so";
    ArrayList<String> libs = new ArrayList<String>();
    if ((args.length == 0) || (args[0].equals("-h")) || (args[0].equals("--help"))) {
      usage();
    }
    System.out.println("jfvml/" + version);
    for(int a=0;a<args.length;a++) {
      String arg = args[a];
      if (arg.equals("--main")) {
        a++;
        main = args[a];
      } else if (arg.equals("--out")) {
        a++;
        out = args[a];
      } else if (arg.equals("--32")) {
        x64 = false;
      } else if (arg.equals("--64")) {
        x64 = true;
      } else if (arg.equals("-g")) {
        debug = true;
      } else {
        libs.add(arg);
      }
    }
    if (File.pathSeparatorChar == ';') {
      isWindows = true;
      ext = ".dll";
      if (x64)
        gcc = "x86_64-w64-mingw32-gcc";
      else
        gcc = "i686-w64-mingw32-gcc";
    }
    bits = x64 ? "64" : "32";
    try {
      if (main == null) {
        usage();
      }
      new File(ces).mkdir();
      new File(oes).mkdir();
      //generate JavaMain
      System.out.println("Generating main");
      StringBuffer sb = new StringBuffer();
      sb.append("#include <jfvm.h>\n");
      sb.append("const char *dll_name[" + libs.size() + "] = {\n");
        for(int a=0;a<libs.size();a++) {
          if (a > 0) sb.append(",");
          sb.append("\"" + libs.get(a) + ext + "\"\n");
        }
      sb.append("};\n");
      sb.append("void *dll_handle[" + libs.size() + "];\n");
      sb.append("int main(int argc, const char **argv) {\n");
      sb.append("  jfvm_main(dll_name, dll_handle, " + libs.size() + ",argc, argv,\"" + main.replaceAll("[.]", "/") + "\");\n");
      sb.append("  return 0;\n");
      sb.append("}");
      FileOutputStream fos = new FileOutputStream(ces + "/main.c");
      fos.write(sb.toString().getBytes());
      fos.close();

      //compile main.c
      System.out.println("Compiling main");
      exec(new String[] {gcc, x64 ? "-m64" : "-m32", debug ? "-g" : "", "-DX" + bits, "-I", System.getenv("jfvm") + "/include" , "-c", ces + "/main.c", "-o", oes + "/main.o"});
      if (!new File(oes + "/main.o").exists()) {
        throw new Exception("Failed to compile:main.c");
      }

      //build exe
      System.out.println("Linking exe");
      ArrayList<String> cmd = new ArrayList<String>();
      cmd.add(gcc);
      cmd.add(x64 ? "-m64" : "-m32");
      if (debug) cmd.add("-g");
      cmd.add(oes + "/main.o");
      //these should be copied into current folder in ant build.xml
      if (isWindows) {
        cmd.add("jfvm-win" + bits + ".dll.a");
      } else {
        cmd.add("jfvm-lnx" + bits + ".so");
        cmd.add("-ldl");
        cmd.add("-lpthread");
      }
      cmd.add("-o");
      cmd.add(out);
      exec(cmd.toArray(new String[cmd.size()]));
      if (!new File(out).exists()) {
        throw new Exception("Failed to compile:" + out);
      }

      System.out.println("Done");
    } catch (Exception e) {
      e.printStackTrace();
    }
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
        e.printStackTrace();
      }
    }
  }
}

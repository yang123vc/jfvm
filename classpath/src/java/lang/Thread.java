package java.lang;

/** Thread
 *
 * @author pquiring
 */

public class Thread {
  private long handle;
  public void run() {}
  public native void start();
  public native void join();
//  public native void finalize() { //cleanup }
  public static native void sleep(long ms);
  public static native boolean holdsLock(Object obj);
  public static native Thread currentThread();
  public void interrupt() {}
}

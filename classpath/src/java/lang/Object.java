package java.lang;

public class Object {
  protected native Object clone();
  public boolean equals(Object obj) { return this == obj; }
  protected void finalize() {}
  public native Class getClass();
  public native int hashCode();
  public native void notify();
  public native void notifyAll();
  public String toString() {
    return getClass().getName() + "@" + hashCode();
  }
  public void wait() {
    wait(0, 0);
  }
  public void wait(long timeout) {
    wait(timeout, 0);
  }
  public native void wait(long timeout, int nanos);
}

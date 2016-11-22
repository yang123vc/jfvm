package pack;

class Test {
  public int x;
  public Test() {
    x = 250;
  }
  public static int main(String args[]) {
    for(int a=0;a<args.length;a++) {
      System.out.println(args[a]);
    }
    Test t = new Test();
    t.testLambda1();
    t.testLambda2();
    t.testExceptions();
    t.testThreads();
    return 0;
  }

  //test lambda expressions
  public int uselamda1(iface1 l) {
    return l.func1(123);
  }
  public void testLambda1() {
    int val = uselamda1( (x) -> {
      return x + 1000;
    } );
    System.out.println("lambda test1=" + (val-1));
  }

  public int uselamda2(iface2 l) {
    return l.func2(17);
  }
  public void testLambda2() {
    int val = uselamda2( (x) -> {
      return x + 2000;
    } );
    System.out.println("lambda test2=" + (val-1));
  }

  //test exceptions
  public void testExceptions() {
    try {
      throwException();
    } catch (Exception e) {
      String str = e.toString();
      System.out.println(str);
    }
  }
  public void throwException() throws Exception {
    throw new Exception();
  }

  //test threads
  public void testThreads() {
    Thread t1 = new T1();
    Thread t2 = new T2();
    t1.start();
    t2.start();
    try {t1.join();} catch (Exception e) {}
    try {t2.join();} catch (Exception e) {}
  }

  private static Object lock = new Object();
  private static int add(int x, int y) {
    return x + y;
  }

  public static class T1 extends Thread {
    public void run() {
      String s1 = "test1";
      while (true) {
        System.out.println(s1);
        try {Thread.sleep(100);} catch (Exception e) {}
        synchronized(lock) {
          add(1,2);
        }
      }
    }
  }
  public static class T2 extends Thread {
    public void run() {
      String s2 = "test2";
      while (true) {
        System.out.println(s2);
        try {Thread.sleep(200);} catch (Exception e) {}
        synchronized(lock) {
          add(1,2);
        }
      }
    }
  }
}

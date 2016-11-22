package java.util;

/** Timer
 *
 * @author pquiring
 */

public class Timer {
  private TimerTask task;
  private long delay, period;
  private volatile boolean active;

  private Scheduler scheduler;
  private class Scheduler extends Thread {
    public void run() {
      Thread.sleep((int)delay);
      while (active) {
        task.run();
        if (period == -1) break;
        Thread.sleep((int)period);
      }
    }
  }

  //this is a primitive fixed scheduler
  //if the task runs over the period you may lose timer events
  private Object lock = new Object();
  private FixedSchedulerWait fixedschedulerwait;
  private class FixedSchedulerWait extends Thread {
    public void run() {
      Thread.sleep((int)delay);
      while (active) {
        task.run();
        synchronized(lock) {
          lock.wait();
        }
      }
    }
  }
  private FixedSchedulerNotify fixedschedulernotify;
  private class FixedSchedulerNotify extends Thread {
    public void run() {
      Thread.sleep((int)delay);
      while (active) {
        Thread.sleep((int)period);
        synchronized(lock) {
          lock.notify();
        }
      }
    }
  }

  public void cancel() {
    active = false;
    scheduler = null;
    fixedschedulerwait = null;
    fixedschedulernotify = null;
  }
  public void purge() {}
  public void schedule(TimerTask task, long delay) {
    this.task = task;
    this.delay = delay;
    this.period = -1;
    active = true;
    scheduler = new Scheduler();
    scheduler.start();
  }
  public void schedule(TimerTask task, long delay, long period) {
    this.task = task;
    this.delay = delay;
    this.period = period;
    active = true;
    scheduler = new Scheduler();
    scheduler.start();
  }
  public void scheduleAtFixedRate(TimerTask task, long delay, long period) {
    this.task = task;
    this.delay = delay;
    this.period = period;
    active = true;
    fixedschedulerwait = new FixedSchedulerWait();
    fixedschedulerwait.start();
    fixedschedulernotify = new FixedSchedulerNotify();
    fixedschedulernotify.start();
  }
}

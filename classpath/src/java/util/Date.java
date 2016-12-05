package java.util;

/** Date/Time functions
 *
 * @author pquiring
 */

public class Date {
  public int year, month, day, hour, minute, second, ms;

  public int getYear() {return year;}
  public int getMonth() {return month;}
  public int getDay() {return day;}
  public int getHours() {return hour;}
  public int getMinutes() {return minute;}
  public int getSeconds() {return second;}
  public int getMilliSeconds() {return ms;}

  public native static Date now();
  private int daysPerMonth[] = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
  private int daysInYear[] = {31, 59, 90,120,151,181,212,243,273,304,334,365};
  private long msecPerYear = 365 * 24 * 60 * 60 * 1000;
  private long msecPerDay = 24 * 60 * 60 * 1000;
  private long msecPerHour = 60 * 60 * 1000;
  private long msecPerMinute = 60 * 60 * 1000;
  private long msecPerSecond = 60 * 1000;
  /** Returns ms from epoch (Jan 1, 1970) */
  public long getTime() {
    //convert date to epoch
    long epoch = (year - 1970) * msecPerYear;
    if (month > 1) epoch += daysInYear[month-2] * msecPerDay;
    epoch += msecPerDay * (day-1);
    epoch += msecPerHour * (hour-1);
    epoch += msecPerMinute * (minute-1);
    epoch += msecPerSecond * (second-1);
    epoch += ms;
    //add leap days
    int leapDays = (year-1) / 4;
    leapDays -= (year-1) / 400;
    //is this year a leap year?
    if ((year % 4 == 0) && (year % 400 != 0) && (month > 2)) {
      leapDays++;
    }
    leapDays -= 488;  //minus leap days before epoch
    epoch += msecPerDay * leapDays;
    return epoch;
  }
  /** Sets date to epoch (Jan 1, 1970) */
  public void setTime(long epoch) {
    ms = (int)epoch % 1000;
    epoch /= 1000;
    second = (int)epoch % 60;
    epoch /= 60;
    minute = (int)epoch % 60;
    epoch /= 60;
    hour = (int)epoch % 24;
    epoch /= 24;
    year = 1970;
    for(;;) {
      int days = 365;
      if ((year % 4 == 0) && (year % 400 != 0)) days++;
      if (epoch < days) break;
      epoch -= days;
      year++;
    }
    month = 1;
    for(;;) {
      int days = daysPerMonth[month];
      if (epoch < days) break;
      epoch -= days;
      month++;
    }
    day = (int)epoch;
  }
}

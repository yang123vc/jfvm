package java.util;

/** Date/Time functions
 *
 * @author pquiring
 */

public class Date {
  public long epoch;  //ms since Jan 1, 1970
  public int year, month, day, hour, minute, second, ms;

  public int getYear() {return year;}
  public int getMonth() {return month;}
  public int getDay() {return day;}
  public int getHours() {return hour;}
  public int getMinutes() {return minute;}
  public int getSeconds() {return second;}
  public int getMilliSeconds() {return ms;}

  public native static Date now();
}

package java.util;

/** Arrays
 *
 * @author pquiring
 */

import java.lang.reflect.Array;

public class Arrays {

  public static Object[] copyOf(Object array[], int newLength) {
    Class cls = array.getClass().getComponentType();
    Object newArray[] = (Object[]) Array.newInstance(cls, newLength);
    System.arraycopy(array, 0, newArray, 0, newLength < array.length ? newLength : array.length);
    return newArray;
  }

  public static byte[] copyOf(byte array[], int newLength) {
    byte newArray[] = new byte[newLength];
    System.arraycopy(array, 0, newArray, 0, newLength < array.length ? newLength : array.length);
    return newArray;
  }

  public static Object[] copyOf(Object array[], int newLength, Class cls) {
    Object newArray[] = (Object[]) Array.newInstance(cls, newLength);
    System.arraycopy(array, 0, newArray, 0, newLength < array.length ? newLength : array.length);
    return newArray;
  }

  public static Object[] copyOfExcluding(Object[] array, int idx) {
    Class cls = array.getClass().getComponentType();
    Object newArray[] = (Object[]) Array.newInstance(cls, array.length - 1);
    System.arraycopy(array, 0, newArray, 0, idx);
    System.arraycopy(array, idx + 1, newArray, idx, array.length - idx - 1);
    return newArray;
  }

  public static boolean[] copyOfExcluding(boolean[] array, int idx) {
    boolean newArray[] = new boolean[array.length - 1];
    System.arraycopy(array, 0, newArray, 0, idx);
    System.arraycopy(array, idx + 1, newArray, idx, array.length - idx - 1);
    return newArray;
  }

  public static byte[] copyOfExcluding(byte[] array, int idx) {
    byte newArray[] = new byte[array.length - 1];
    System.arraycopy(array, 0, newArray, 0, idx);
    System.arraycopy(array, idx + 1, newArray, idx, array.length - idx - 1);
    return newArray;
  }

  public static short[] copyOfExcluding(short[] array, int idx) {
    short newArray[] = new short[array.length - 1];
    System.arraycopy(array, 0, newArray, 0, idx);
    System.arraycopy(array, idx + 1, newArray, idx, array.length - idx - 1);
    return newArray;
  }

  public static int[] copyOfExcluding(int[] array, int idx) {
    int newArray[] = new int[array.length - 1];
    System.arraycopy(array, 0, newArray, 0, idx);
    System.arraycopy(array, idx + 1, newArray, idx, array.length - idx - 1);
    return newArray;
  }

  public static float[] copyOfExcluding(float[] array, int idx) {
    float newArray[] = new float[array.length - 1];
    System.arraycopy(array, 0, newArray, 0, idx);
    System.arraycopy(array, idx + 1, newArray, idx, array.length - idx - 1);
    return newArray;
  }

  public static double[] copyOfExcluding(double[] array, int idx) {
    double newArray[] = new double[array.length - 1];
    System.arraycopy(array, 0, newArray, 0, idx);
    System.arraycopy(array, idx + 1, newArray, idx, array.length - idx - 1);
    return newArray;
  }

  public static char[] copyOfExcluding(char[] array, int idx) {
    char newArray[] = new char[array.length - 1];
    System.arraycopy(array, 0, newArray, 0, idx);
    System.arraycopy(array, idx + 1, newArray, idx, array.length - idx - 1);
    return newArray;
  }


}

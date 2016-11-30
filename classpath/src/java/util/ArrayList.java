package java.util;

import java.lang.reflect.Array;

/** ArrayList
 *
 * @author pquiring
 */

public class ArrayList<E> implements List<E> {
  private Object list[];
  private int alloc;
  private int size;

  public ArrayList() {
    alloc = 64;
    size = 0;
    list = new Object[64];
  }

  public boolean add(Object e) {
    if (size == alloc) {
      alloc <<= 1;
      list = Arrays.copyOf(list, alloc);
    }
    list[size++] = e;
    return true;
  }

  public boolean add(int idx, Object e) {
    if (size == alloc) {
      alloc <<= 1;
      list = Arrays.copyOf(list, alloc);
    }
    System.rarraycopy(list, idx, list, idx + 1, list.length - idx - 1);
    list[idx] = e;
    size++;
    return true;
  }

  public void clear() {
    size = 0;
  }

  public boolean contains(Object e) {
    return indexOf(e) >= 0;
  }

  public E get(int idx) {
    return (E)list[idx];
  }

  public int indexOf(Object e) {
    int cnt = size;
    for(int i=0;i<cnt;i++) {
      if (list[i].equals(e)) return i;
    }
    return -1;
  }

  public boolean isEmpty() {
    return size == 0;
  }

  public int lastIndexOf(Object e) {
    for(int i=size-1;i>=0;i--) {
      if (list[i].equals(e)) return i;
    }
    return -1;
  }

  public void remove(Object e) {
    remove(indexOf(e));
  }

  public void remove(int idx) {
    if (idx == -1) return;
    System.arraycopy(list, idx, list, idx - 1, size - idx - 1);
    size--;
  }

  public Object set(int idx, Object e) {
    Object old = list[idx];
    list[idx] = e;
    return old;
  }

  public int size() {
    return list.length;
  }

  public Object[] toArray() {
    return Arrays.copyOf(list, size);
  }

  public Object[] toArray(Object[] array) {
    if (array.length == size) {
      System.arraycopy(list, 0, array, 0, size);
      return array;
    } else {
      Object copy[] = (Object[])Array.newInstance(array.getClass(), size);
      System.arraycopy(list, 0, copy, 0, size);
      return copy;
    }
  }
}

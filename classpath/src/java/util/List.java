package java.util;

/** List
 *
 * @author pquiring
 */

public interface List<E> {
  boolean add(E e);
  boolean add(int idx, E e);
  void clear();
  boolean contains(Object e);
  boolean equals(Object e);
  E get(int idx);
  int indexOf(Object e);
  boolean isEmpty();
  int lastIndexOf(Object e);
  void remove(Object e);
  void remove(int idx);
  E set(int idx, E e);
  int size();
  Object[] toArray();
  <T>T[] toArray(T[] a);
}

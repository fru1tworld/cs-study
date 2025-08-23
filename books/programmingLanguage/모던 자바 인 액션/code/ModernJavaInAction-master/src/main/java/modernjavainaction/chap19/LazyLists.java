package modernjavainaction.chap19;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class LazyLists {

  public static void main(String[] args) {
    MyList<Integer> l = new MyLinkedList<>(5, new MyLinkedList<>(10, new Empty<>()));

    System.out.println(l.head());

    LazyList<Integer> numbers = from(2);
    int two = numbers.head();
    int three = numbers.tail().head();
    int four = numbers.tail().tail().head();
    System.out.println(two + " " + three + " " + four);

    numbers = from(2);
    int prime_two = primes(numbers).head();
    int prime_three = primes(numbers).tail().head();
    int prime_five = primes(numbers).tail().tail().head();
    System.out.println(prime_two + " " + prime_three + " " + prime_five);

    // 자바는 꼬리 호출 제거 기능이 없으므로 스택오버플로가 발생할 때까지 실행됨
    // printAll(primes(from(2)));
  }

  interface MyList<T> {

    T head();

    MyList<T> tail();

    default boolean isEmpty() {
      return true;
    }

    MyList<T> filter(Predicate<T> p);

  }

  static class MyLinkedList<T> implements MyList<T> {

    final T head;
    final MyList<T> tail;

    public MyLinkedList(T head, MyList<T> tail) {
      this.head = head;
      this.tail = tail;
    }

    @Override
    public T head() {
      return head;
    }

    @Override
    public MyList<T> tail() {
      return tail;
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public MyList<T> filter(Predicate<T> p) {
      return isEmpty() ? this : p.test(head()) ? new MyLinkedList<>(head(), tail().filter(p)) : tail().filter(p);
    }

  }

  static class Empty<T> implements MyList<T> {

    @Override
    public T head() {
      throw new UnsupportedOperationException();
    }

    @Override
    public MyList<T> tail() {
      throw new UnsupportedOperationException();
    }

    @Override
    public MyList<T> filter(Predicate<T> p) {
      return this;
    }

  }

  static class LazyList<T> implements MyList<T> {

    final T head;
    final Supplier<MyList<T>> tail;

    public LazyList(T head, Supplier<MyList<T>> tail) {
      this.head = head;
      this.tail = tail;
    }

    @Override
    public T head() {
      return head;
    }

    @Override
    public MyList<T> tail() {
      return tail.get();
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public MyList<T> filter(Predicate<T> p) {
      return isEmpty() ? this : p.test(head()) ? new LazyList<>(head(), () -> tail().filter(p)) : tail().filter(p);
    }

  }

  public static LazyList<Integer> from(int n) {
    return new LazyList<Integer>(n, () -> from(n + 1));
  }

  public static MyList<Integer> primes(MyList<Integer> numbers) {
    return new LazyList<>(numbers.head(), () -> primes(numbers.tail().filter(n -> n % numbers.head() != 0)));
  }

  static <T> void printAll(MyList<T> numbers) {
    if (numbers.isEmpty()) {
      return;
    }
    System.out.println(numbers.head());
    printAll(numbers.tail());
  }

}

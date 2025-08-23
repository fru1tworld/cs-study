package modernjavainaction.chap20;

import java.util.function.Function;
import java.util.stream.Stream;

public class Currying {

  public static void main(String[] args) {
    int r = multiply(2, 10);
    System.out.println(r);

    Stream.of(1, 3, 5, 7)
        .map(multiplyCurry(2))
        .forEach(System.out::println);
  }

  static int multiply(int x, int y) {
    return x * y;
  }

  static Function<Integer, Integer> multiplyCurry(int x) {
    return (Integer y) -> x * y;
  }

}

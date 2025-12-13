package modernjavainaction.chap20;

import java.util.stream.IntStream;

public class Foo {

  public static void main(String[] args) {
    IntStream.rangeClosed(2, 6)
        .forEach(n -> System.out.println("Hello " + n + " bottles of beer"));
  }

}

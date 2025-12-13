package modernjavainaction.chap19;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MyMathUtils {

  public static void main(String[] args) {
    System.out.println(primes(25).map(String::valueOf).collect(Collectors.joining(", ")));
  }

  public static Stream<Integer> primes(int n) {
    return Stream.iterate(2, i -> i + 1)
        .filter(MyMathUtils::isPrime)
        .limit(n);
  }

  public static boolean isPrime(int candidate) {
    int candidateRoot = (int) Math.sqrt(candidate);
    return IntStream.rangeClosed(2, candidateRoot)
        .noneMatch(i -> candidate % i == 0);
  }

}

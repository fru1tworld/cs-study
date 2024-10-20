package modernjavainaction.chap04;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class StreamVsCollection {

  public static void main(String... args) {
    List<String> names = Arrays.asList("Java8", "Lambdas", "In", "Action");
    Stream<String> s = names.stream();
    s.forEach(System.out::println);
    // 스트림은 한 번 만 소비할 수 있으므로 아래 행의 주석을 제거하면 IllegalStateException이 발생
    //s.forEach(System.out::println);
  }

}

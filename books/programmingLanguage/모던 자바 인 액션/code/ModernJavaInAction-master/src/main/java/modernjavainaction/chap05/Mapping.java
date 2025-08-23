package modernjavainaction.chap05;

import static java.util.stream.Collectors.toList;
import static modernjavainaction.chap04.Dish.menu;

import java.util.Arrays;
import java.util.List;

import modernjavainaction.chap04.Dish;

public class Mapping{

  public static void main(String... args) {
    // map
    List<String> dishNames = menu.stream()
        .map(Dish::getName)
        .collect(toList());
    System.out.println(dishNames);

    // map
    List<String> words = Arrays.asList("Hello", "World");
    List<Integer> wordLengths = words.stream()
        .map(String::length)
        .collect(toList());
    System.out.println(wordLengths);

    // flatMap
    words.stream()
        .flatMap((String line) -> Arrays.stream(line.split("")))
        .distinct()
        .forEach(System.out::println);

    // flatMap
    List<Integer> numbers1 = Arrays.asList(1,2,3,4,5);
    List<Integer> numbers2 = Arrays.asList(6,7,8);
    List<int[]> pairs = numbers1.stream()
        .flatMap((Integer i) -> numbers2.stream()
            .map((Integer j) -> new int[]{i, j})
        )
        .filter(pair -> (pair[0] + pair[1]) % 3 == 0)
        .collect(toList());
    pairs.forEach(pair -> System.out.printf("(%d, %d)", pair[0], pair[1]));
  }

}

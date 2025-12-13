package modernjavainaction.chap08;

import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

public class WorkingWithCollections {

  public static void main(String[] args) {
    workingWithLists();
    workingWithMaps();
    computingOnMaps();
    removingFromMaps();
    replacingInMaps();
    mergingMaps();
  }

  private static void workingWithLists() {
    System.out.println("------ Working with Lists ------");

    System.out.println("--> Transforming list items with a Stream");
    List<String> referenceCodes = Arrays.asList("a12", "C14", "b13");
    referenceCodes.stream()
        .map(code -> Character.toUpperCase(code.charAt(0)) + code.substring(1))
        .collect(Collectors.toList())
        .forEach(System.out::println);
    System.out.println("... but the original List remains unchanged: " + referenceCodes);

    System.out.println("--> Mutating a list with a ListIterator");
    for (ListIterator<String> iterator = referenceCodes.listIterator(); iterator.hasNext(); ) {
      String code = iterator.next();
      iterator.set(Character.toUpperCase(code.charAt(0)) + code.substring(1));
    }
    System.out.println("This time it's been changed: " + referenceCodes);

    System.out.println("--> Mutating a list with replaceAll()");
    referenceCodes = Arrays.asList("a12", "C14", "b13");
    System.out.println("Back to the original: " + referenceCodes);
    referenceCodes.replaceAll(code -> Character.toUpperCase(code.charAt(0)) + code.substring(1));
    System.out.println("Changed by replaceAll(): " + referenceCodes);
  }

  private static void workingWithMaps() {
    System.out.println("------ Working with Maps ------");

    System.out.println("--> Iterating a map with a for loop");
    Map<String, Integer> ageOfFriends = Map.of("Raphael", 30, "Olivia", 25, "Thibaut", 26);
    for (Map.Entry<String, Integer> entry: ageOfFriends.entrySet()) {
      String friend = entry.getKey();
      Integer age = entry.getValue();
      System.out.println(friend + " is " + age + " years old");
    }

    System.out.println("--> Iterating a map with forEach()");
    ageOfFriends.forEach((friend, age) -> System.out.println(friend + " is " + age + " years old"));

    System.out.println("--> Iterating a map sorted by keys through a Stream");
    Map<String, String> favouriteMovies = Map.ofEntries(
        entry("Raphael", "Star Wars"),
        entry("Cristina", "Matrix"),
        entry("Olivia", "James Bond"));
    favouriteMovies.entrySet().stream()
        .sorted(Entry.comparingByKey())
        .forEachOrdered(System.out::println);

    System.out.println("--> Using getOrDefault()");
    System.out.println(favouriteMovies.getOrDefault("Olivia", "Matrix"));
    System.out.println(favouriteMovies.getOrDefault("Thibaut", "Matrix"));
  }

  private static void computingOnMaps() {
    Map<String, List<String>> friendsToMovies = new HashMap<>();

    System.out.println("--> Adding a friend and movie in a verbose way");
    String friend = "Raphael";
    List<String> movies = friendsToMovies.get(friend);
    if (movies == null) {
       movies = new ArrayList<>();
       friendsToMovies.put(friend, movies);
    }
    movies.add("Star Wars");
    System.out.println(friendsToMovies);

    System.out.println("--> Adding a friend and movie using computeIfAbsent()");
    friendsToMovies.clear();
    friendsToMovies.computeIfAbsent("Raphael", name -> new ArrayList<>())
        .add("Star Wars");
    System.out.println(friendsToMovies);
  }

  private static void removingFromMaps() {
    // 바꿀 수 있는 맵 필요!
    Map<String, String> favouriteMovies = new HashMap<>();
    favouriteMovies.put("Raphael", "Jack Reacher 2");
    favouriteMovies.put("Cristina", "Matrix");
    favouriteMovies.put("Olivia", "James Bond");
    String key = "Raphael";
    String value = "Jack Reacher 2";

    System.out.println("--> Removing an unwanted entry the cumbersome way");
    boolean result = remove(favouriteMovies, key, value);
    System.out.printf("%s [%b]%n", favouriteMovies, result);

    // 두 번째 테스트를 하기 전에 삭제된 항목을 다시 돌려놓음
    favouriteMovies.put("Raphael", "Jack Reacher 2");

    System.out.println("--> Removing an unwanted the easy way");
    favouriteMovies.remove(key, value);
    System.out.printf("%s [%b]%n", favouriteMovies, result);
  }

  private static <K, V> boolean remove(Map<K, V> favouriteMovies, K key, V value) {
    if (favouriteMovies.containsKey(key) && Objects.equals(favouriteMovies.get(key), value)) {
      favouriteMovies.remove(key);
      return true;
    }
    return false;
  }

  private static void replacingInMaps() {
    Map<String, String> favouriteMovies = new HashMap<>();
    favouriteMovies.put("Raphael", "Star Wars");
    favouriteMovies.put("Olivia", "james bond");

    System.out.println("--> Replacing values in a map with replaceAll()");
    favouriteMovies.replaceAll((friend, movie) -> movie.toUpperCase());
    System.out.println(favouriteMovies);
  }

  private static void mergingMaps() {
    Map<String, String> family = Map.ofEntries(
        entry("Teo", "Star Wars"),
        entry("Cristina", "James Bond"));
    Map<String, String> friends = Map.ofEntries(entry("Raphael", "Star Wars"));

    System.out.println("--> Merging the old way");
    Map<String, String> everyone = new HashMap<>(family);
    everyone.putAll(friends);
    System.out.println(everyone);

    Map<String, String> friends2 = Map.ofEntries(
        entry("Raphael", "Star Wars"),
        entry("Cristina", "Matrix"));

    System.out.println("--> Merging maps using merge()");
    Map<String, String> everyone2 = new HashMap<>(family);
    friends2.forEach((k, v) -> everyone2.merge(k, v, (movie1, movie2) -> movie1 + " & " + movie2));
    System.out.println(everyone2);
  }

}

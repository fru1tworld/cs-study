package modernjavainaction.chap06;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.filtering;
import static java.util.stream.Collectors.flatMapping;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.summingInt;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static modernjavainaction.chap06.Dish.dishTags;
import static modernjavainaction.chap06.Dish.menu;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Grouping {

  enum CaloricLevel { DIET, NORMAL, FAT };

  public static void main(String ... args) {
    System.out.println("Dishes grouped by type: " + groupDishesByType());
    System.out.println("Dish names grouped by type: " + groupDishNamesByType());
    System.out.println("Dish tags grouped by type: " + groupDishTagsByType());
    System.out.println("Caloric dishes grouped by type: " + groupCaloricDishesByType());
    System.out.println("Dishes grouped by caloric level: " + groupDishesByCaloricLevel());
    System.out.println("Dishes grouped by type and caloric level: " + groupDishedByTypeAndCaloricLevel());
    System.out.println("Count dishes in groups: " + countDishesInGroups());
    System.out.println("Most caloric dishes by type: " + mostCaloricDishesByType());
    System.out.println("Most caloric dishes by type: " + mostCaloricDishesByTypeWithoutOprionals());
    System.out.println("Sum calories by type: " + sumCaloriesByType());
    System.out.println("Caloric levels by type: " + caloricLevelsByType());
  }

  private static Map<Dish.Type, List<Dish>> groupDishesByType() {
    return menu.stream().collect(groupingBy(Dish::getType));
  }

  private static Map<Dish.Type, List<String>> groupDishNamesByType() {
    return menu.stream().collect(
        groupingBy(Dish::getType,
            mapping(Dish::getName, toList())));
  }

  private static Map<Dish.Type, Set<String>> groupDishTagsByType() {
    return menu.stream().collect(
        groupingBy(Dish::getType,
            flatMapping(dish -> dishTags.get(dish.getName()).stream(), toSet())));
  }

  private static Map<Dish.Type, List<Dish>> groupCaloricDishesByType() {
    //return menu.stream().filter(dish -> dish.getCalories() > 500).collect(groupingBy(Dish::getType));
    return menu.stream().collect(
        groupingBy(Dish::getType,
            filtering(dish -> dish.getCalories() > 500, toList())));
  }

  private static Map<CaloricLevel, List<Dish>> groupDishesByCaloricLevel() {
    return menu.stream().collect(
        groupingBy(dish -> {
          if (dish.getCalories() <= 400) {
            return CaloricLevel.DIET;
          }
          else if (dish.getCalories() <= 700) {
            return CaloricLevel.NORMAL;
          }
          else {
            return CaloricLevel.FAT;
          }
        })
    );
  }

  private static Map<Dish.Type, Map<CaloricLevel, List<Dish>>> groupDishedByTypeAndCaloricLevel() {
    return menu.stream().collect(
        groupingBy(Dish::getType,
            groupingBy((Dish dish) -> {
              if (dish.getCalories() <= 400) {
                return CaloricLevel.DIET;
              }
              else if (dish.getCalories() <= 700) {
                return CaloricLevel.NORMAL;
              }
              else {
                return CaloricLevel.FAT;
              }
            })
        )
    );
  }

  private static Map<Dish.Type, Long> countDishesInGroups() {
    return menu.stream().collect(groupingBy(Dish::getType, counting()));
  }

  private static Map<Dish.Type, Optional<Dish>> mostCaloricDishesByType() {
    return menu.stream().collect(
        groupingBy(Dish::getType,
            reducing((Dish d1, Dish d2) -> d1.getCalories() > d2.getCalories() ? d1 : d2)));
  }

  private static Map<Dish.Type, Dish> mostCaloricDishesByTypeWithoutOprionals() {
    return menu.stream().collect(
        groupingBy(Dish::getType,
            collectingAndThen(
                reducing((d1, d2) -> d1.getCalories() > d2.getCalories() ? d1 : d2),
                Optional::get)));
  }

  private static Map<Dish.Type, Integer> sumCaloriesByType() {
    return menu.stream().collect(groupingBy(Dish::getType,
        summingInt(Dish::getCalories)));
  }

  private static Map<Dish.Type, Set<CaloricLevel>> caloricLevelsByType() {
    return menu.stream().collect(
        groupingBy(Dish::getType, mapping(
            dish -> {
              if (dish.getCalories() <= 400) {
                return CaloricLevel.DIET;
              }
              else if (dish.getCalories() <= 700) {
                return CaloricLevel.NORMAL;
              }
              else {
                return CaloricLevel.FAT;
              }
            },
            toSet()
        ))
    );
  }

}

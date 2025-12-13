package modernjavainaction.chap11;

import java.util.Optional;

public class Person {

  private Optional<Car> car;
  private int age;

  public Optional<Car> getCar() {
    return car;
  }

  public int getAge() {
    return age;
  }

}

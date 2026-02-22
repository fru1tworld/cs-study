# Java 8 금요일 선물: 람다와 정렬

> 원문: https://blog.jooq.org/java-8-friday-goodies-lambdas-and-sorting/

Data Geekery에서는 Java와 SQL에 열광합니다. 그래서 우리가 가장 좋아하는 언어인 Java가 멋진 새 기능들로 업데이트되어 출시될 때, 우리는 진정한 Java 8 금요일 시리즈를 시작하고자 합니다. 매주 금요일마다 여러분이 즐길 수 있는 새로운 Java 8 관련 선물을 제공하는 블로그 시리즈입니다. GitHub에서 소스 코드를 찾아보실 수 있습니다.

## Java 8 선물: 람다와 정렬

아마도 가장 많이 언급되는 Java 8 기능은 람다 표현식일 것입니다. 그러나 람다 자체만으로는 그다지 유용하지 않으며, 업그레이드의 가장 큰 개선점은 JDK 라이브러리에서 볼 수 있습니다. 그 중 하나는 Comparator를 사용하여 컬렉션을 정렬할 때 볼 수 있습니다. Comparator는 JDK 1.2부터 항상 @FunctionalInterface였습니다!

이러한 것들을 설명하기 위해 다음과 같이 간단한 Person 클래스를 사용하겠습니다:

```java
static class Person {
    final String firstName;
    final String lastName;

    Person(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    public String toString() {
        return "Person{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                '}';
    }
}
```

물론 이 클래스에 equals()와 hashCode()를 추가하고 싶으시겠지만, 이 예제에서는 필요하지 않습니다. 이제 몇 명의 사람들을 만들어 보겠습니다:

```java
List<Person> people =
Arrays.asList(
    new Person("Jane", "Henderson"),
    new Person("Michael", "White"),
    new Person("Henry", "Brighton"),
    new Person("Hannah", "Plowman"),
    new Person("William", "Henderson")
);
```

우리는 이들을 성(lastName)으로 먼저 정렬하고, 그 다음 이름(firstName)으로 정렬하고 싶습니다.

## Java 7 방식

익명 내부 클래스를 사용하는 "고전적인" Java 7 방식은 다음과 같습니다:

```java
people.sort(new Comparator<Person>() {
  @Override
  public int compare(Person o1, Person o2) {
    int result = o1.lastName.compareTo(o2.lastName);

    if (result == 0)
      result = o1.firstName.compareTo(o2.firstName);

    return result;
  }
});
people.forEach(System.out::println);
```

그리고 위는 다음을 출력합니다:

```
Person{firstName='Henry', lastName='Brighton'}
Person{firstName='Jane', lastName='Henderson'}
Person{firstName='William', lastName='Henderson'}
Person{firstName='Hannah', lastName='Plowman'}
Person{firstName='Michael', lastName='White'}
```

## Java 8 방식

이제 동일한 것을 Java 8에서 람다 표현식을 사용하여 작성할 수 있습니다. 이를 위해 몇 가지 옵션이 있습니다.

### 체이닝을 사용한 기본 람다

```java
Comparator<Person> c = (p, o) ->
    p.lastName.compareTo(o.lastName);

c = c.thenComparing((p, o) ->
    p.firstName.compareTo(o.firstName));

people.sort(c);
people.forEach(System.out::println);
```

위의 중간 할당이 아쉽습니다. 안타깝게도 Java의 타입 추론은 "왼쪽에서 오른쪽으로" 작동합니다. 즉, 선언에서 표현식 방향으로 진행됩니다. Scala나 Ceylon과 같은 일부 다른 언어는 역방향 추론도 지원하며, 이 경우 타입이 명시적으로 지정된 첫 번째 thenComparing() 표현식에서 추론될 수 있습니다.

### 유틸리티 메서드 우회 방법

중간 변수 할당 없이 유창성을 개선하기 위해:

```java
class Utils {
    static <E> Comparator<E> compare() {
        return (e1, e2) -> 0;
    }
}
```

사용 방법:

```java
people.sort(
    Utils.<Person>compare()
         .thenComparing((p, o) ->
              p.lastName.compareTo(o.lastName))
         .thenComparing((p, o) ->
              p.firstName.compareTo(o.firstName))
);
```

### 키 추출자

함수 기반 추출을 사용하는 더 깔끔한 접근 방식:

```java
people.sort(Utils.<Person>compare()
      .thenComparing(p -> p.lastName)
      .thenComparing(p -> p.firstName));
people.forEach(System.out::println);
```

### 표준 라이브러리 접근 방식

내장된 Comparator.comparing() 메서드를 사용:

```java
people.sort(
    Comparator.comparing((Person p) -> p.lastName)
          .thenComparing(p -> p.firstName));
people.forEach(System.out::println);
```

comparing()의 첫 번째 인수에서 어떻게 Person 타입이 추론될 수 없고 명시적으로 지정해야 하는지 주목하세요.

## 메서드 참조

getter 메서드가 있다면, 코드는 훨씬 더 깔끔해집니다:

```java
people.sort(
    Comparator.comparing(Person::getLastName)
          .thenComparing(Person::getFirstName));
```

## 핵심 요점

이 글에서는 "업그레이드의 가장 큰 개선점은 JDK 라이브러리에서 볼 수 있다"는 점을 강조합니다. Java 8은 Comparator 사용을 더 읽기 쉽게 만들면서도 타입 안전성을 유지합니다. 때때로 타입 추론의 특이점으로 인해 명시적인 매개변수 타입이 필요하기는 하지만요.

다음 주에도 더 많은 Java 8 선물이 있으니 계속 지켜봐 주세요!

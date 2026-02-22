# 우리는 나뉜다: Optional

> 원문: https://blog.jooq.org/divided-we-stand-optional/

우리의 최근 글 'NULL은 십억 달러짜리 실수가 아니다. 반박문'은 많은 조회수, 논쟁적인 댓글, 그리고 블로그 글이 게시되고 투표될 수 있는 거의 모든 곳에서 50/50의 찬반 비율을 얻었습니다.

## Java의 null 문제

Java의 `null` 구현은 다른 언어들과 다르며, 세 가지 핵심 문제가 있습니다:

1. 컴파일 타임 vs 런타임 타이핑 충돌: 컴파일 타임 타이핑은 런타임 NullPointerException을 방지하지 못합니다.
2. null 리터럴의 고유한 위치: Java는 어떤 참조 타입에도 null을 할당할 수 있게 허용하지만, null은 어떤 타입의 인스턴스도 아닙니다.
3. null 참조에서 호출 가능한 메서드: null 리터럴은 "아무것도 아님"임에도 불구하고 메서드를 호출할 수 있습니다.

## Java 8의 Optional

Java 8에서 `Optional`이 도입되면서, 타입은 이제 카디널리티(cardinality)를 명확하게 전달할 수 있게 되었습니다:

- 카디널리티 1: `Type t1;`
- 카디널리티 0-1: `Optional<Type> t01;`
- 카디널리티 0..n: `Iterable<Type> tn;`

이 방식은 값이 필수인지, 선택적인지, 또는 여러 개인지를 명확하게 표현합니다.

## 하위 호환성 문제

하위 호환성은 `Optional`의 미흡한 채택으로 이어질 것입니다. JDK API는 nullable과 `Optional`을 반환하는 메서드가 혼재되어 있어, 개발자들이 일관된 동작에 의존하는 것이 불가능합니다.

예를 들어, `Map.get()` 메서드는 `Optional`이 아닌 `null`을 반환합니다. 이는 개발자들이 두 가지 패턴을 동시에 기억해야 하도록 강제하여 `Optional`의 목적을 훼손합니다.

다음 코드를 살펴보겠습니다:

```java
Map<Integer, List<Integer>> map =
Stream.of(1, 1, 2, 3, 5, 8)
      .collect(Collectors.groupingBy(n -> n % 5));

IntStream.range(0, 5)
         .mapToObj(map::get)
         .map(List::size)
         .forEach(System.out::println);
```

이 코드는 `NullPointerException`을 던집니다. 왜냐하면 `Map.get()`이 null을 반환할 수 있기 때문입니다.

수정된 해결책은 다음과 같습니다:

```java
IntStream.range(0, 5)
         .mapToObj(map::get)
         .map(l -> l == null ? Collections.emptyList() : l)
         .map(List::size)
         .forEach(System.out::println);
```

만약 `Map.get()`이 `Optional`을 반환했다면, 코드는 다음과 같이 더 자연스러웠을 것입니다:

```java
IntStream.range(0, 5)
         .mapToObj(map::get)
         .map(l -> l.orElse(Collections.emptyList()))
         .map(List::size)
         .forEach(System.out::println);
```

## 안티패턴: 일관성 없는 Optional 사용

다음과 같은 도메인 모델링 접근 방식은 문제가 있습니다:

```java
public class User {
    private final String username;
    private Optional<String> fullname;

    public Optional<String> getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = Optional.of(fullname);
    }
}
```

이 설계는 일관성 없는 계약을 수립하기 때문에 비판받습니다. getter는 `Optional`을 노출하여 API 소비자에게 null 가능성에 대한 우려를 신호하지만, setter는 `Optional` 래핑 없이 일반 `String` 파라미터를 받습니다. 이러한 모순은 혼란을 야기하며, 편의성이 근본적인 설계 불일치를 가리는 "안티패턴"을 나타냅니다.

## Doug Lea의 견해

Java 전문가 그룹의 Doug Lea는 다음과 같이 말했습니다:

> "[Optional]에 대해 여기저기서 수년간 많은 논의가 있었습니다... 일부 컬렉션은 null 요소를 허용하는데, 이는 null을 그렇지 않았다면 유일하게 합리적인 의미인 '거기에 아무것도 없다'로 모호하지 않게 사용할 수 없다는 것을 의미합니다... 일부 사람들은 더 유창한 API를 허용하기 위해 Optional을 사용하는 아이디어를 좋아합니다... 하지만 그들이 Optionalism이 그들의 설계 전반에 전파되기 시작하여 `Set<Optional<T>>`와 같은 것으로 이어진다는 것을 깨달았을 때 때때로 덜 행복해합니다. 여기서 이기기는 어렵습니다."

## Optional이 도입된 이유

주된 동기는 기본형 타입 스트림을 다루기 위한 것이었습니다. `IntStream`이 부재하는 값을 표현할 방법이 필요했기 때문에, 라이브러리는 `OptionalInt`를 도입했습니다. API 일관성을 위해 `Optional`이 객체 스트림에도 추가되었습니다.

## 전부 아니면 전무(All-or-Nothing)

`Optional`을 코드베이스에 도입하면, 암묵적으로 0-1 카디널리티 값은 항상 `Optional` 래퍼를 사용할 것이라고 가정하게 됩니다. 그때부터 코드베이스는 더 이상 0-1 카디널리티에 대해 `Optional`이 아닌 단순한 `Type` 타입을 사용해서는 안 됩니다. 절대로요.

`Optional`과 nullable 타입을 혼합하면 명확성 대신 혼란을 야기합니다.

## 결론

접근 방식을 일관되게 선택하세요. 코드베이스 전체에서 `Optional`을 수용하거나, `null` 처리 규칙을 받아들이세요. 하지만 코드베이스 전반에 걸친 일관성 없는 사용은 해결하려는 것보다 더 나쁜 문제를 만들어냅니다.

근본적인 문제는 Java가 레거시 API에 null 가능성 보장을 소급 적용할 수 없다는 것입니다. 전체 코드베이스와 JDK에 걸쳐 완전한 채택 없이는 `Optional`은 명확성보다 더 많은 모호함을 만들어냅니다.

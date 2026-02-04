# Java 8 금요일: 레거시 라이브러리를 더 이상 사용하지 않는 것으로 표시하자

> 원문: https://blog.jooq.org/java-8-friday-lets-deprecate-those-legacy-libs/

Data Geekery에서 우리는 Java를 좋아합니다. 그리고 우리는 jOOQ의 유창한 API와 쿼리 DSL이 Java 8의 새로운 기능과 함께 사용될 때 어떻게 활용될 수 있는지에 대해 정말 흥분하고 있습니다.

## Java 8 금요일

매주 금요일마다 Java 8의 새로운 기능을 포함한 다양한 튜토리얼 스타일의 블로그 게시물을 선보입니다. 이 게시물은 람다 표현식, 확장 메서드 및 기타 훌륭한 것들을 사용합니다. 소스 코드는 GitHub에서 찾을 수 있습니다.

## 레거시 라이브러리를 deprecate하자

람다 표현식과 확장 메서드 외에도, JDK는 많은 새로운 라이브러리(예: Streams API)를 추가로 제공했습니다. 이는 우리가 기술 스택을 다시 평가하고 더 이상 필요하지 않은 타사 라이브러리를 하나씩 제거할 수 있음을 의미합니다.

### LINQ 스타일 라이브러리

Java 8의 Streams API가 출시되면서 다양한 LINQ 에뮬레이션 라이브러리들을 deprecate할 수 있게 되었습니다.

#### LambdaJ

LambdaJ는 람다 표현식이 없던 시절에 클로저를 에뮬레이션하기 위한 흥미로운 시도였습니다. `ThreadLocal`을 사용하여 클로저를 시뮬레이션하는 복잡한 우회 방법이 필요했습니다.

이전에는 다음과 같이 작성해야 했습니다:

```java
Closure println = closure(); {
  of(System.out).println(var(String.class));
}
println.each("one", "two", "three");
```

Java 8에서는 메서드 참조를 사용하여 이렇게 단순화됩니다:

```java
Consumer<String> println = System.out::println;
Stream.of("one", "two", "three").forEach(println);
```

#### Linq4j

Linq4j는 여전히 활발하게 개발되고 있지만, 이 라이브러리가 LINQ-to-SQL을 파서 수정과 함께 추구하는 것을 보면, Streams API가 Java 언어 수정 없이도 더 우수한 대안을 제공하는데 정말 필요한지 의문이 듭니다.

#### Coolection

이 라이브러리는 컬렉션에 대해 쿼리 스타일의 구문을 사용할 수 있게 해주었습니다. Java 8 Streams는 람다 표현식과 메서드 참조를 통해 동일한 기능을 더 우아하게 달성합니다.

### Guava의 일부 기능

Guava는 한동안 JDK에 있어야 할 기능들의 "집결지" 역할을 해왔습니다. 이제 여러 컴포넌트에 대해 표준 대체품이 있습니다:

- `com.google.common.base.Optional` -> `java.util.Optional`
- `com.google.common.base.Predicate` -> `java.util.function.Predicate`
- `com.google.common.base.Supplier` -> `java.util.function.Supplier`
- `com.google.common.base.Joiner` -> Stream과 `joining()` 컬렉터 사용

#### 문자열 연결 예제

이전에는 다음과 같이 작성해야 했습니다:

```java
Joiner joiner = Joiner.on("; ").skipNulls();
return joiner.join("Harry", null, "Ron", "Hermione");
```

이제 Streams를 사용합니다:

```java
Stream.of("Harry", null, "Ron", "Hermione")
      .filter(s -> s != null)
      .collect(joining("; "));
```

Streams 접근 방식은 연결과 필터링을 우아하게 분리합니다.

### JodaTime

인기 있는 JodaTime 라이브러리는 JSR-310을 통해 Java 8의 `java.time` 패키지로 표준화되었습니다. 이로써 타사 날짜/시간 라이브러리가 필요 없게 되었습니다.

### Apache Commons-IO

Java 7/8 이전에는 파일 읽기가 번거로웠습니다. Java 8의 `java.nio.file.Files`는 우수한 대안을 제공합니다:

```java
List<String> lines = Files.readAllLines(path);
```

이것은 이전에 필요했던 장황한 try-with-resources 패턴을 대체합니다.

### Base64 인코딩 (JEP 135)

1990년대부터 RFC 명세가 존재했음에도 불구하고, Java는 Java 8의 `java.util.Base64`가 나오기 전까지 네이티브 Base64 지원이 없었습니다. 이것은 수많은 타사 구현을 제거합니다:

- Apache Commons Codec
- JAXB의 내부 인코더
- Guava의 BaseEncoding
- JEE의 `javax.mail.internet.MimeUtility`
- Jetty의 B64Code
- 다양한 독립적인 구현들

### 동시성 개선 (JEP 155)

JEP 155는 향상된 `ConcurrentHashMap`과 새로운 `LongAdder` 클래스를 포함한 개선 사항을 도입하여, Guava의 동시성 유틸리티에 대한 의존성을 줄여줍니다.

### 추가 Deprecation

Op4j도 Streams에 의해 더 이상 필요 없게 된 또 다른 LINQ 스타일 라이브러리입니다. Apache Commons Collections의 함수형 프로그래밍 유틸리티도 마찬가지입니다.

## 결론

Java 8이 이전에 분산되어 있던 기능들을 표준화함으로써 외부 의존성 없이 더 깔끔한 코드베이스를 가능하게 합니다. 라이브러리 제작자들의 가치 있는 기여를 인정하면서도, JDK로의 통합은 진보를 나타내며, 개발자들이 우회 방법 대신 혁신에 집중할 수 있게 해줍니다.

*면책 조항: 이 글의 모든 내용이 완전히 진지하게 작성된 것은 아닙니다. 이러한 라이브러리들이 표준화되기 전에 제공한 기여에 대해 존경을 표하면서도, 표준화의 이점을 축하합니다.*

더 deprecate할 수 있는 다른 라이브러리가 있다면 댓글로 알려주세요!

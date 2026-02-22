# 높은 응집력으로 버그를 제거하는 방법

> 원문: https://blog.jooq.org/how-to-eliminate-bugs-through-high-cohesion/

Lukas Eder가 2014년 2월 26일에 작성

## 문제점: 메서드 시그니처의 코드 스멜

다양한 타입의 매개변수를 많이 받으면서 종종 null로 설정되는 메서드들은 뚜렷한 코드 스멜을 가지고 있습니다. 예를 들어, Java의 `JavaCompiler.getTask()` 메서드를 살펴보겠습니다:

```java
CompilationTask getTask(
    Writer out,
    JavaFileManager fileManager,
    DiagnosticListener<? super JavaFileObject> diagnosticListener,
    Iterable<String> options,
    Iterable<String> classes,
    Iterable<? extends JavaFileObject> compilationUnits
);
```

이 메서드를 실제로 사용할 때는 다음과 같이 많은 매개변수에 null을 전달하게 됩니다:

```java
Iterable<? extends JavaFileObject> compilationUnits1 =
    fileManager.getJavaFileObjectsFromFiles(
        Arrays.asList(files1));

compiler.getTask(null, fileManager, null,
                 null, null, compilationUnits1)
        .call();
```

이러한 설계는 메서드의 재사용성을 감소시킵니다. JArchitect 용어로 말하자면, 낮은 안정성(stability)과 낮은 추상성(abstractness)을 가진 "고통의 영역(Zone of Pain)"에 위치하게 됩니다.

## 높은 응집력의 가치

"높은 응집력(high cohesion)"의 가장 큰 가치는 - 이상적인 안정성/추상성 균형을 갖추면 - 매우 재사용 가능한 코드를 갖게 된다는 것입니다. 이것은 단순히 개발자들이 작업을 구현하는 데 적은 시간을 소비하기 때문에 좋은 것만이 아닙니다. 이는 또한 코드가 극도로 오류에 강하다는 것을 의미합니다.

높은 응집력을 가진 코드에서 버그가 발생하면, 그 버그는 다음 두 가지 중 하나에 해당합니다:

1. 단순히 외관상의 문제(cosmetic): 단일 메서드나 리프(leaf)에만 국한된 극도로 지역적인 버그
2. 완전히 치명적인 문제(catastrophic): 전체 트리에 영향을 미치는 극도로 전역적인 버그

중간 지대는 없습니다. 미묘한 회귀 버그가 숨어들 위험한 중간 영역이 제거됩니다.

## jOOQ의 데이터 타입 변환 예제

jOOQ의 데이터 타입 변환 계층 구조를 예로 들어보겠습니다. 모든 변환은 프레임워크 전체에서 사용되는 단일 변환 API를 통해 흐릅니다. 이것은 역삼각형 구조로 시각화할 수 있으며, 모든 분기가 하나의 중앙 변환 API를 향해 수렴합니다.

이 설계 덕분에 데이터 타입 변환과 관련된 모든 회귀 버그는 즉시 수백 개의 단위 테스트와 통합 테스트를 실패하게 만듭니다. 버그가 숨어서 나중에 프로덕션에서 발견되는 일이 없습니다.

### 실제 구현 예시: 중첩된 사용자 정의 타입

Oracle에서 다음과 같은 중첩된 UDT(User-Defined Type)가 있다고 가정해 보겠습니다:

```sql
CREATE TYPE street_type AS OBJECT (
  street VARCHAR2(100),
  no VARCHAR2(30)
);

CREATE TYPE address_type AS OBJECT (
  street street_type,
  zip VARCHAR2(50),
  city VARCHAR2(50)
);
```

이에 대응하는 Java POJO 클래스는 다음과 같습니다:

```java
public class Street {
    public String street;
    public String number;
}

public class Address {
    public Street street;
    public String city;
    public String country;
}

public class Person {
    public String firstName;
    public String lastName;
    public Address address;
}
```

jOOQ에서는 이러한 중첩된 구조를 다음과 같이 간단하게 매핑할 수 있습니다:

```java
Person person = DSL.using(configuration)
                   .selectFrom(PERSON)
                   .where(PERSON.ID.eq(1))
                   .fetchOneInto(Person.class);
```

jOOQ가 중첩된 사용자 정의 타입에 대한 재귀적 매핑을 추가했을 때, 이를 단일 `ConvertAll` 구현 지점에서 구현했습니다. 이것이 의미하는 바는:

- 단일 변경 지점: 한 곳만 수정하면 됩니다
- 최소한의 복잡도 증가: 기존 구조에 자연스럽게 녹아듭니다
- 모든 사용 사례 자동 지원: 새 기능이 모든 곳에서 자동으로 작동합니다

## 구현 전략: 무자비한 리팩토링

해결책은 "무자비하게 리팩토링"하는 것입니다 - 절대로 기능을 지역적으로 도입하지 마세요. 기능을 올바른 추상화 수준에서 한 번 구현하면, 복잡도와 버그 위험을 줄이면서 기능을 확장할 수 있습니다.

완벽한 설계는 처음부터 예측할 수 없습니다. 점진적으로 진화해야 합니다. 하지만 이러한 진화의 방향은 항상 더 높은 응집력을 향해야 합니다.

## 권장 사항

핵심적인 코드(essential core code)에 대해서는, 균형 잡힌 안정성/추상성 비율로 높은 응집력을 달성하는 것이 중요합니다. 은행 시스템, 결제 로직 등 중요한 시스템에서는 높은 응집력을 달성하는 데 많은 투자를 하세요.

비핵심적인 코드(UI나 DB 접근 등)에 대해서는, Vaadin, ZK, Hibernate, jOOQ, Spring Data와 같은 서드파티 소프트웨어에 의존하는 것을 권장합니다. 이러한 벤더들은 높은 코드 품질을 달성하는 데 상당한 시간을 투자하기 때문입니다.

## 결론

높은 응집력은 단순히 좋은 설계 원칙이 아닙니다. 이것은 버그 제거를 위한 실용적인 전략입니다. 모든 것이 단일 구현 지점을 통해 흐르도록 코드를 구조화하면:

1. 버그가 발생했을 때 즉시 발견됩니다
2. 수정이 한 곳에서만 이루어집니다
3. 새 기능이 자동으로 모든 곳에 적용됩니다
4. 테스트 커버리지가 자연스럽게 포괄적이 됩니다

철학을 요약하면: 올바른 추상화 수준에서 기능을 한 번 구현하세요. 복잡도와 버그 위험을 줄이면서 기능을 확장할 수 있습니다.

# 제네릭과 하위 호환성에 속지 마라. 제네릭 제네릭 타입을 사용하라

> 원문: https://blog.jooq.org/dont-be-fooled-by-generics-and-backwards-compatibility-use-generic-generic-types/

*2015년 4월 1일 lukaseder 작성*

최근 Ergon의 Sebastian Gruber와 나눈 대화 덕분에 jOOQ 엔지니어링 팀은 jOOQ API를 완전히 다시 작성해야 한다는 결론에 도달했습니다. Ergon은 jOOQ의 초기 고객 중 하나로, 이들의 피드백은 항상 매우 가치 있습니다.

## 현재 제네릭 사용 방식

jOOQ는 이미 여러 방식으로 제네릭을 구현하고 있습니다:

- 컬럼 타입 제네릭: `interface Field<T> { ... }` 형태로 `Field<String> field = BOOK.TITLE;`과 같이 사용합니다.
- 테이블 타입 제네릭: `interface Table<R extends Record> { ... }` 형태로 `Table<BookRecord> books = BOOK;`과 같이 사용합니다.
- `<T>`와 `<R>` 파라미터를 모두 사용하는 결합 제네릭

## 문제점

핵심 문제는 다음과 같습니다: Java 클래스는 한 번만 제네릭화할 수 있습니다. 초기 구현은 작동합니다:

```java
// 여전히 호환됨
class Foo<Bar, Baz> {}
```

하지만 나중에 타입 변수를 추가하면 클라이언트 코드가 깨집니다:

```java
// 호환성 파괴
class Foo<Bar, Baz, Fizz> {}
```

## 해결책: 제네릭 제네릭 타입

이 접근 방식은 SQL 데이터베이스 설계 패턴을 모방합니다. 컬럼별 특정 속성 대신 SQL은 범용 컬럼을 사용합니다:

```sql
CREATE TABLE foo (
    bar int,
    baz int,
    fizz int,
    generic_1 varchar(4000),
    generic_2 varchar(4000),
    generic_3 varchar(4000),
    generic_4 varchar(4000),
    -- [...]
);
```

Java 구현도 이와 유사하게 따를 것입니다:

```java
class Foo<
    Bar,
    Baz,
    Fizz,
    Generic1,
    Generic2,
    Generic3,
    Generic4,
    // [...]
> {}
```

## 구현 세부 사항

jOOQ는 모든 타입을 정확히 256개의 제네릭 타입 파라미터로 제네릭화할 것입니다. 이는 MS Access가 가능한 컬럼 수로 사용한 것과 동일한 제한입니다. 이렇게 하면 고객이 한 번만 업그레이드하면 제네릭 타입 하위 호환성이 영원히 보장됩니다.

## 결론

해피 코딩!

---

참고: 이 글은 4월 1일에 게시되었으며, 실제 기술적 제약에 대한 터무니없는 해결책을 제시하는 만우절 기사입니다.

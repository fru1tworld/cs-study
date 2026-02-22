> 원문: https://blog.jooq.org/a-hidden-jooq-gem-foreach-loop-over-resultquery/

# 숨겨진 jOOQ의 보석: ResultQuery에 대한 Foreach 루프

jOOQ의 `ResultQuery` 인터페이스에는 잘 알려지지 않은 기능이 있습니다. 바로 Java의 `Iterable`로 동작하여 명시적으로 `fetch()`를 호출하지 않고도 foreach 루프에서 직접 사용할 수 있다는 점입니다.

## 두 가지 루프 문법

다음 두 가지 방식은 동일하게 동작합니다:

```java
// 명시적 fetch() 사용
for (MyTableRecord rec : ctx
    .selectFrom(MY_TABLE)
    .orderBy(MY_TABLE.COLUMN)
    .fetch())
```

```java
// fetch() 없이 - 동일하게 동작
for (MyTableRecord rec : ctx
    .selectFrom(MY_TABLE)
    .orderBy(MY_TABLE.COLUMN))
```

## 구현 세부사항

이 기능은 `ResultQuery`가 `Iterable<R>`을 구현하기 때문에 가능합니다. 반복자(iterator)가 호출되면 쿼리가 실행되고 전체 결과 집합이 클라이언트 메모리에 구체화(materialize)됩니다. 이는 PL/SQL의 암시적 커서(implicit cursor) 동작과 유사합니다.

## 리소스 관리 고려사항

여기에는 한 가지 제한사항이 있습니다. Java에는 `AutoCloseableIterable` 같은 구조가 없기 때문에, 단순한 foreach 방식은 전체 결과의 구체화가 필요합니다. 적절한 리소스 처리와 함께 지연 페칭(lazy fetching)을 사용하려면 다음과 같은 대안이 있습니다:

- `fetchLazy()`를 try-with-resources 및 명시적 커서 관리와 함께 사용
- `forEach()` 메서드를 통한 내부 반복(internal iteration) 사용

이 기능은 jOOQ가 데이터베이스 패러다임의 친숙함을 Java 개발에 어떻게 가져오는지를 잘 보여줍니다.

# jOOQ에서 데이터를 가져오는 다양한 방법들

> 원문: https://blog.jooq.org/the-many-different-ways-to-fetch-data-in-jooq/

jOOQ API는 편의성을 최우선으로 하며, `fetch()` 작업은 다양한 구현 옵션을 제공하는 핵심 기능입니다.

## 기본 페칭(Default Fetching)

표준 접근 방식은 전체 결과 집합을 메모리에 로드하고 JDBC 리소스를 즉시 닫습니다:

```java
Result<Record1<String>> result =
ctx.select(BOOK.TITLE)
   .from(BOOK)
   .fetch();

for (Record1<String> record : result) {
    // ...
}
```

## 반복 가능한 페칭(Iterable Fetching)

`ResultQuery<R>`가 `Iterable<R>`을 구현하고 있기 때문에, 명시적인 `fetch()` 호출 없이도 반복을 통해 쿼리를 실행할 수 있습니다.

외부 반복(External iteration) 은 PL/SQL의 커서 루프와 유사합니다:

```java
for (Record1<String> record : ctx
    .select(BOOK.TITLE)
    .from(BOOK)
) {
    // ...
}
```

내부 반복(Internal iteration) 은 상속된 `Iterable::forEach`를 사용합니다:

```java
ctx.select(BOOK.TITLE)
   .from(BOOK)
   .forEach(record -> {
       // ...
   });
```

두 방법 모두 여전히 전체 결과 집합을 메모리에 적재해야 합니다.

## 단일 레코드 페칭(Single Record Fetching)

하나의 값을 반환하는 쿼리에 대해 jOOQ는 세 가지 옵션을 제공합니다:

Nullable 레코드:

```java
Record1<String> r = query.fetchOne();
```

결과가 없으면 null을 반환합니다. 여러 레코드가 존재하면 `TooManyRowsException`을 던집니다.

Optional 레코드:

```java
Optional<Record1<String>> r = query.fetchOptional();
```

Single 레코드:

```java
Record1<String> r = query.fetchSingle();
```

결과가 없으면 `NoDataFoundException`을 던집니다. 정확히 하나의 레코드를 보장합니다.

## 리소스 관리 페칭(Resourceful Fetching)

대용량 데이터셋의 경우, 페칭하는 동안 JDBC 리소스를 열어 둔 상태로 유지합니다.

명령형 접근 방식 - `fetchLazy()` 사용:

```java
try (Cursor<Record1<String>> cursor = ctx
    .select(BOOK.TITLE)
    .from(BOOK)
    .fetchLazy()
) {
    for (Record1<String> record : cursor) {
        // ...
    }
}
```

`Cursor<R>`는 `Iterable<R>`을 확장하며 수동 페칭도 지원합니다:

```java
Record record;

while ((record = cursor.fetchNext()) != null) {
    // ...
}
```

함수형 접근 방식 - `fetchStream()` 사용:

```java
try (Stream<Record1<String>> stream = ctx
    .select(BOOK.TITLE)
    .from(BOOK)
    .fetchStream()
) {
    stream.forEach(record -> {
        // ...
    });
}
```

참고: Stream API는 자동 닫기(auto-closing) 동작이 없으므로, 명시적으로 `try-with-resources`로 감싸야 합니다.

## 함수형 비리소스 페칭(Functional Non-Resourceful Fetching)

`fetch()`와 스트리밍을 결합하여 메모리 내 데이터를 처리할 수 있습니다:

```java
ctx.select(BOOK.TITLE)
   .from(BOOK)
   .fetch()
   .stream()
   .forEach(record -> {
       // ...
   });
```

## 컬렉터 페칭(Collector Fetching)

jOOQ 3.11부터 `ResultQuery::collect`와 `Cursor::collect`는 JDK의 `Collector` API를 활용하여 강력한 변환을 수행합니다.

jOOQ는 `Records` 클래스에서 유용한 컬렉터를 제공합니다. 예를 들어, `Records.intoMap()`은 타입 안전한 맵을 생성합니다:

```java
Map<Integer, String> books =
ctx.select(BOOK.ID, BOOK.TITLE)
   .from(BOOK)
   .collect(Records.intoMap());
```

이 접근 방식은 동등한 `ResultQuery` 메서드보다 더 편리하며, 중간 `Result` 구조를 생성하는 것을 피할 수 있습니다.

## 리액티브 페칭(Reactive Fetching)

jOOQ 3.15부터 R2DBC 지원으로 `ResultQuery<R>`는 리액티브 스트림즈의 `Publisher<R>`가 됩니다:

```java
Flux<Record1<String>> flux = Flux.from(ctx
    .select(BOOK.TITLE)
    .from(BOOK)
);
```

## 다중 결과 집합(Multiple Result Sets)

여러 결과 집합을 생성하는 쿼리의 경우:

```java
Results results = ctx.fetch("sp_help");

for (Result<?> result : results) {
    for (Record record : result) {
        // ...
    }
}
```

필요한 경우 `Results.resultsOrRows()`를 사용하여 인터리브된 행 수(row count)에 접근할 수 있습니다.

## 결론

jOOQ의 API 설계는 조합 가능한 기본 요소(composable primitives)를 통한 편의성을 강조하며, 표준 JDK 컬렉션 API와 일치하는 타입 안전한 SQL 통합을 가능하게 합니다.

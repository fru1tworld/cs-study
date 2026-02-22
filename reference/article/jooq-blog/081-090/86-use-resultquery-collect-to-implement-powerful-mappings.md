# ResultQuery.collect()로 강력한 매핑 구현하기

> 원문: https://blog.jooq.org/use-resultquery-collect-to-implement-powerful-mappings/

## 핵심 개념

모든 `Iterable<T>`는 표준 JDK 컬렉터나 커스텀 구현체를 사용하여 콘텐츠를 변환할 수 있도록 `<R> collect(Collector<T, ?, R>)` 메서드를 제공해야 합니다. jOOQ의 `ResultQuery<R>`는 기본적인 반복(iteration) 기능을 넘어서 이미 이 기능을 제공하고 있습니다.

## Record를 사용한 기본 예제

쿼리 결과를 Java record로 매핑하는 방법을 살펴보겠습니다:

```java
record Book (int id, String title) {}

List<Book> books =
ctx.select(BOOK.ID, BOOK.TITLE)
   .from(BOOK)
   .collect(Collectors.mapping(
        r -> r.into(Book.class),
        Collectors.toList()
   ));
```

## Left Join 문제

Stack Overflow에서 제기된 질문에 따르면, `fetchGroups()`는 left join을 제대로 처리하지 못합니다. 부모에게 자식이 없는 경우, 빈 리스트 대신 단일 NULL 항목이 포함된 리스트를 반환합니다. 단순한 접근 방식은 다음과 같습니다:

```java
Map<AuthorRecord, List<BookRecord>> result =
ctx.select()
   .from(AUTHOR)
   .leftJoin(BOOK).onKey()
   .fetchGroups(AUTHOR, BOOK);
```

## Collector 기반 해결책

권장하는 해결 방법은 조합된 컬렉터를 사용하는 것입니다:

```java
Map<AuthorRecord, List<BookRecord>> result =
ctx.select()
   .from(AUTHOR)
   .leftJoin(BOOK).onKey()
   .collect(groupingBy(
        r -> r.into(AUTHOR),
        filtering(
            r -> r.get(BOOK.ID) != null,
            mapping(
                r -> r.into(BOOK),
                toList()
            )
        )
    ));
```

## 단계별 설명

1. AUTHOR로 그룹화 — `fetchGroups()`와 유사하게 맵 키를 AuthorRecord로 설정합니다
2. NULL 레코드 필터링 — left join에서 매칭되지 않아 ID가 null인 BOOK 항목을 제거합니다
3. 값으로 매핑 — 결과를 BookRecord 인스턴스로 변환합니다
4. 리스트로 수집 — 자식 레코드들을 집계합니다

쿼리는 자동으로 실행되고, 리소스를 효율적으로 관리하며, 불필요한 중간 데이터 구조를 우회합니다.

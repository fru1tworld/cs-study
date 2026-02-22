# jOOQ 오늘의 팁: 모든 기본 키 발견하기

> 원문: https://blog.jooq.org/jooq-tip-of-the-day-discover-all-primary-keys/

게시일: 2014년 11월 14일
작성자: Lukas Eder
태그: information_schema, jooq, meta data, query, schema information

---

데이터베이스 스키마 내의 모든 기본 키를 발견하는 것은 jOOQ API를 사용하면 매우 간단합니다. 이 작업을 수행하는 두 가지 접근 방식이 있습니다.

## 방법 1: 생성된 메타데이터 사용

jOOQ 코드 생성기를 사용하는 경우, 생성된 스키마 클래스에서 직접 테이블과 해당 기본 키에 접근할 수 있습니다.

### Java 8 스트림 사용

```java
GENERATED_SCHEMA
    .getTables()
    .stream()
    .map(t -> t.getPrimaryKey())
    .filter(Objects::nonNull)
    .forEach(System.out::println);
```

### Java 7 및 이전 버전 (전통적인 반복문)

```java
for (Table<?> t : GENERATED_SCHEMA.getTables()) {
    UniqueKey<?> key = t.getPrimaryKey();

    if (key != null)
        System.out.println(key);
}
```

## 방법 2: 런타임 메타데이터 사용

코드 생성 없이 런타임에 데이터베이스 메타데이터를 탐색하려면, `DSLContext.meta()` 메서드를 사용할 수 있습니다:

```java
DSL.using(configuration)
   .meta()
   .getTables()
   // [위와 동일한 필터링 적용]
```

이 방법은 jOOQ API를 통해 메타 정보를 쿼리하는 것과 관련된 또 다른 사용 사례를 참조합니다 - 상호 연관된 테이블을 통해 쿼리하는 것입니다.

## 커뮤니티 피드백

독자들이 `table.getIdentity()` 및 런타임 메타데이터에서 nullable 필드 상태 확인과 유사한 기능을 요청했습니다. 저자는 이에 응답하여 GitHub에 해당 기능 요청 이슈를 생성했습니다.

---

Happy coding!

# SQL에서 추가 왕복 없이 페이지네이션 메타데이터 계산하기

> 원문: https://blog.jooq.org/calculating-pagination-metadata-without-extra-roundtrips-in-sql/

## 핵심 문제

SQL에서 오프셋 기반 페이지네이션을 구현할 때, 개발자들은 일반적으로 여러 쿼리가 필요합니다: 페이지네이션된 데이터를 위한 쿼리 하나와 전체 행 수를 세기 위한 또 다른 쿼리입니다. 이 글에서는 윈도우 함수를 사용하여 단일 SQL 쿼리로 모든 페이지네이션 메타데이터를 계산하는 방법을 설명합니다.

## 완전한 해결책

### 메인 SQL 쿼리 구조

이 접근 방식은 윈도우 함수와 함께 중첩된 파생 테이블을 사용합니다. 완전한 패턴은 다음과 같습니다:

```sql
SELECT
  t.first_name,
  t.last_name,
  COUNT(*) OVER () AS actual_page_size,
  MAX(row) OVER () = total_rows AS last_page,
  total_rows,
  row,
  ((row - 1) / :max_page_size) + 1 AS current_page
FROM (
  SELECT
    u.*,
    COUNT(*) OVER () AS total_rows,
    ROW_NUMBER () OVER (ORDER BY u.actor_id) AS row
  FROM (
    SELECT *
    FROM actor
  ) AS u
  ORDER BY u.actor_id
  OFFSET :offset ROWS
  FETCH NEXT :max_page_size ROWS ONLY
) AS t
ORDER BY t.actor_id
```

### 필요한 메타데이터 출력

이 쿼리는 다음을 계산합니다:
- TOTAL_ROWS: 페이지네이션 없이 전체 레코드 수
- CURRENT_PAGE: 현재 페이지 번호
- ACTUAL_PAGE_SIZE: 현재 페이지에서 반환된 행 수
- LAST_PAGE: 마지막 페이지인지 여부를 나타내는 불리언
- ROW: 각 레코드의 행 번호

### 단계별 설명

내부 파생 테이블 (u): 모든 비즈니스 로직, 조인, 필터가 그대로 포함된 원본 쿼리를 담고 있습니다. 정렬에 필요한 컬럼과 외부 쿼리에서 필요한 컬럼을 프로젝션합니다.

중간 파생 테이블 계산: `COUNT(*) OVER ()`를 적용하여 필터링된 전체 데이터셋의 총 행 수를 가져옵니다. `ROW_NUMBER() OVER (ORDER BY...)`를 사용하여 순차적인 번호를 할당합니다. 빈 지정을 가진 `OVER ()` 절은 FROM 절의 모든 행에 대해 작동합니다.

페이지네이션 적용: 중간 레벨에서 `ORDER BY`와 `OFFSET .. FETCH`를 적용합니다. 이렇게 하면 결과를 제한하기 전에 올바른 행 번호가 매겨집니다.

외부 파생 테이블 (t): 페이지네이션된 부분 집합에 대해 `COUNT(*) OVER ()`를 사용하여 `actual_page_size`를 계산합니다. 최대 행 값을 전체 행 수와 비교하여 `last_page`를 결정합니다. `((row - 1) / max_page_size) + 1` 공식을 사용하여 `current_page`를 계산합니다.

최종 정렬: 항상 가장 바깥쪽 레벨에서 `ORDER BY`를 다시 적용합니다. SQL은 명시적인 정렬 없이는 서브쿼리를 통해 순서가 보존되는 것을 보장하지 않습니다.

## jOOQ 구현

```java
static Select<?> paginate(
    DSLContext ctx,
    Select<?> original,
    Field<?> [] sort,
    int limit,
    int offset
) {
    Table<?> u = original.asTable("u");
    Field<Integer> totalRows = count().over().as("total_rows");
    Field<Integer> row = rowNumber()
        .over()
        .orderBy(u.fields(sort))
        .as("row");

    Table<?> t = ctx
        .select(u.asterisk())
        .select(totalRows, row)
        .from(u)
        .orderBy(u.fields(sort))
        .limit(limit)
        .offset(offset)
        .asTable("t");

    Select<?> result = ctx
        .select(t.fields(
            original.getSelect().toArray(Field[]::new)))
        .select(
            count().over().as("actual_page_size"),
            field(max(t.field(row)).over().eq(t.field(totalRows)))
                .as("last_page"),
            t.field(totalRows),
            t.field(row),
            t.field(row).minus(inline(1))
                .div(limit).plus(inline(1))
                .as("current_page"))
        .from(t)
        .orderBy(t.fields(sort));

    return result;
}
```

### 사용 예제

```java
System.out.println(
    paginate(
        ctx,
        ctx.select(ACTOR.ACTOR_ID, ACTOR.FIRST_NAME,
                   ACTOR.LAST_NAME)
           .from(ACTOR),
        new Field[] { ACTOR.ACTOR_ID },
        15,
        30
    ).fetch()
);
```

## 샘플 결과

3페이지 (offset 30, limit 15):
- `ACTOR_ID`: 31-45
- `actual_page_size`: 15
- `last_page`: false
- `total_rows`: 200
- `current_page`: 3

마지막 페이지 (offset 195, limit 15):
- `ACTOR_ID`: 196-200
- `actual_page_size`: 5
- `last_page`: true
- `total_rows`: 200
- `current_page`: 14

## 주요 장점

이 단일 쿼리 접근 방식은 윈도우 함수를 효율적으로 활용하면서 카운트 정보를 위한 데이터베이스 왕복을 제거합니다. jOOQ 구현은 동적 SQL 구성을 가능하게 하여, 복잡한 조인과 필터링이 있는 임의의 쿼리에 재사용 가능한 페이지네이션 로직을 적용할 수 있습니다.

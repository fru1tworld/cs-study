# 바인드 변수만으로 충분하지 않을 때: 동적 IN 리스트

> 원문: https://blog.jooq.org/when-using-bind-variables-is-not-enough-dynamic-in-lists/

## 서론

이전 블로그 글에서 바인드 변수가 성능과 보안 두 가지 이유로 기본 선택이 되어야 한다고 강조했습니다. 그러나 이 글에서는 바인드 변수만으로는 충분하지 않은 예외적인 경우를 다룹니다. 특히 동적 IN 리스트는 프로덕션 환경에서 심각한 성능 문제를 야기할 수 있습니다.

## 동적 IN 리스트의 문제점

IN 조건절은 여러 개의 OR 조건을 축약한 형태로 동작합니다. 다음 두 쿼리는 의미적으로 동일합니다:

```sql
-- IN 조건절
SELECT *
FROM actor
WHERE actor_id IN (1, 2, 3)

-- OR 조건절
SELECT *
FROM actor
WHERE actor_id = 1
OR actor_id = 2
OR actor_id = 3
```

두 쿼리 모두 INLIST ITERATOR를 사용하여 세 번의 기본 키 조회를 수행하는 동일한 실행 계획을 생성하며, 최적의 카디널리티 추정치를 제공합니다.

## 실행 계획 캐싱 문제

핵심 문제는 다음과 같습니다: 가변 길이의 IN 리스트에 바인드 변수를 사용하면, 각각의 고유한 리스트 크기가 별도의 SQL 문을 생성합니다:

```sql
SELECT count(*) FROM actor WHERE actor_id IN (?)
SELECT count(*) FROM actor WHERE actor_id IN (?, ?)
SELECT count(*) FROM actor WHERE actor_id IN (?, ?, ?)
SELECT count(*) FROM actor WHERE actor_id IN (?, ?, ?, ?)
SELECT count(*) FROM actor WHERE actor_id IN (?, ?, ?, ?, ?)
```

각 변형은 Oracle의 커서 캐시에서 고유한 SQL_ID를 받게 되어, 부하가 걸릴 때 캐시 용량을 고갈시킬 수 있습니다.

## PL/SQL 구현 예제

이 글에서는 중첩된 EXECUTE IMMEDIATE 문을 사용하여 이러한 동적 쿼리를 생성하는 방법을 보여줍니다:

```sql
SET SERVEROUTPUT ON
DECLARE
  v_count NUMBER;

  FUNCTION binds (n NUMBER) RETURN VARCHAR2 IS
    v_result VARCHAR2(1000);
  BEGIN
    FOR i IN 1..n LOOP
      IF v_result IS NULL THEN
        v_result := ':b' || i;
      ELSE
        v_result := v_result || ', :b' || i;
      END IF;
    END LOOP;

    RETURN v_result;
  END binds;

  FUNCTION vals (n NUMBER) RETURN VARCHAR2 IS
    v_result VARCHAR2(1000);
  BEGIN
    FOR i IN 1..n LOOP
      IF v_result IS NULL THEN
        v_result := i;
      ELSE
        v_result := v_result || ', ' || i;
      END IF;
    END LOOP;

    RETURN v_result;
  END vals;
BEGIN
  FOR i IN 1..5 LOOP
    EXECUTE IMMEDIATE '
      DECLARE
        v_count NUMBER;
      BEGIN
        EXECUTE IMMEDIATE ''
          SELECT count(*)
          FROM actor
          WHERE actor_id IN (' || binds(i) || ')
        ''
        INTO v_count
        USING ' || vals(i) || ';

        :v_count := v_count;
      END;
    '
    USING OUT v_count
    ;

    dbms_output.put_line(v_count);
  END LOOP;
END;
/
```

중첩된 EXECUTE IMMEDIATE가 필요한 이유는 가변 바인드 리스트로 동적 SQL을 구현할 때의 복잡성을 보여줍니다.

## jOOQ를 사용한 Java 구현

동일한 기능의 jOOQ 구현은 훨씬 더 간단합니다:

```java
for (int i = 1; i <= 5; i++) {
    System.out.println(
        DSL.using(configuration)
           .select(count())
           .from(ACTOR)
           .where(ACTOR.ACTOR_ID.in(
                IntStream.rangeClosed(1, i)
                         .boxed()
                         .collect(toList())
           ))
           .fetchOne(count())
    );
}
```

이 방식은 동일한 문제가 있는 SQL 변형을 생성하지만, 코드 복잡성은 최소화됩니다.

## 벤치마크 분석

이 글에서는 두 가지 접근 방식을 비교하는 초기 벤치마크 결과를 제시합니다:

1. 정확한 크기의 IN 리스트 (각 고유 리스트 크기마다 하나의 SQL 문)
2. 다음 2의 거듭제곱으로 패딩된 IN 리스트 (예: 100개 요소 리스트를 128개로 패딩)

벤치마크 설정 세부사항:
- 5회 벤치마크 실행
- 실행당 20,000개 쿼리
- 1개에서 100개 요소까지의 IN 리스트

가장 빠른 실행 대비 비율로 나타낸 결과:

```
Run 1, Statement 1 : 1.16944
Run 1, Statement 2 : 1.11204
Run 2, Statement 1 : 1.06086
Run 2, Statement 2 : 1.03591
Run 3, Statement 1 : 1.03589
Run 3, Statement 2 : 1.03605
Run 4, Statement 1 : 1.33935
Run 4, Statement 2 : 1.2822
Run 5, Statement 1 : 1
Run 5, Statement 2 : 1.04648
```

벤치마크 결과는 패딩으로 인한 유의미한 성능 오버헤드가 없음을 보여줍니다.

## 실행 횟수 결과

실행 빈도를 보여주는 쿼리 결과:

```
EXECS   BINDS   SQL_TEXT
2000    1       SELECT count(*) FROM actor WHERE actor_id IN (:b1)
2000    2       SELECT count(*) FROM actor WHERE actor_id IN (:b1, :b2)
1000    3       SELECT count(*) FROM actor WHERE actor_id IN (:b1, :b2, :b3)
3000    4       ...
1000    5       ...
1000    6       ...
1000    7       ...
5000    8       ...
1000    9       ...
1000    10      ...
1000    11      ...
1000    12      ...
1000    13      ...
1000    14      ...
1000    15      ...
9000    16      ...
...
1000    100     ...
36000   128     ...
```

정확한 크기의 리스트: 각 1,000회 실행 (총 100개의 고유한 문장)
패딩된 리스트: 1,000 × X회 실행 (128 = 2^7이므로 7개의 고유한 문장)

## IN 리스트 패딩 전략

패딩 방식은 바인드 값을 반복 사용하여 고유한 SQL 문의 수를 100개에서 7개로 줄입니다. 예를 들어, 5개 요소 리스트를 8개 요소로 패딩할 때 다섯 번째 값을 세 번 반복 사용합니다.

## 더 긴 리스트를 사용한 확장 벤치마크

IN 리스트가 최대 4,096개 요소까지 테스트할 경우, Oracle은 이를 분할합니다:

```sql
SELECT count(*)
FROM actor
WHERE actor_id IN (:b1, :b2, ..., :b1000)
OR actor_id IN (:b1001, :b1002, ..., :b2000)
OR actor_id IN (:b2001, ...)
```

성능 비교 결과 패딩 방식이 약 1.5배 향상된 성능을 보여줍니다:

```
Run 1, Statement 1 : 1.42696
Run 1, Statement 2 : 1
```

패딩 방식은 커서 캐시 고갈을 방지하면서 적당한 성능 향상을 보여줍니다.

## 대안적 접근 방식

저자는 다음과 같은 여러 대안을 권장합니다:

배열 타입: 배열을 지원하는 데이터베이스(Oracle, PostgreSQL)의 경우, IN 리스트 대신 네이티브 배열 타입을 사용합니다. 이에 대한 비교는 이전 블로그 글에서 다루었습니다.

임시 테이블: 대용량 리스트의 경우, ID를 임시 테이블에 대량 삽입한 후 세미 조인합니다:

```sql
SELECT count(*)
FROM actor
WHERE actor_id IN (
  SELECT id FROM temp_table
)
```

원본 쿼리에서 세미 조인: ID가 다른 테이블에서 유래한 경우 최적의 접근 방식입니다:

```sql
SELECT count(*)
FROM actor
WHERE actor_id IN (
  SELECT id FROM other_table WHERE ...
)
```

저자는 다음과 같이 말합니다: "마지막 옵션은 항상 첫 번째 선택이 되어야 합니다. 다른 모든 방식보다 성능이 훨씬 좋을 가능성이 높기 때문입니다."

## 요약

이 글에서는 바인드 변수가 보안과 일반적인 성능을 위해 여전히 필수적이지만, 동적 IN 리스트는 여러 개의 고유한 SQL 문을 통해 커서 캐시 단편화를 야기한다는 점을 확립합니다. 패딩 기법은 리스트 크기를 2의 거듭제곱으로 반올림하여 이러한 단편화를 로그 스케일로 줄여주며, 성능 저하 없이 실용적인 해결책을 제공합니다. 그러나 대용량 데이터셋의 경우 임시 테이블이나 세미 조인과 같은 대안적 접근 방식이 종종 더 우수한 결과를 제공합니다.

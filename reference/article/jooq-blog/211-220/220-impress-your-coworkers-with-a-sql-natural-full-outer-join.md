> 원문: https://blog.jooq.org/impress-your-coworkers-with-a-sql-natural-full-outer-join/

# SQL NATURAL FULL OUTER JOIN으로 동료들을 감탄시키세요!

*2017년 3월 7일, Lukas Eder*

오늘 저는 고객을 돕다가 실제로 `NATURAL FULL OUTER JOIN`이 유용하게 쓰이는 경우를 발견했습니다. 이것이 어떻게 가능할까요? 먼저 각 구성 요소가 무엇인지 살펴보겠습니다.

## FULL JOIN이란?

`FULL JOIN`은 `JOIN` 조건이 일치하는지 여부와 관계없이 `JOIN` 연산의 양쪽 데이터를 모두 유지하는 `OUTER JOIN`의 한 종류입니다. Sakila 데이터베이스에서 배우(actor)와 영화(film)의 관계를 생각해 보세요.

```sql
SELECT first_name, last_name, title
FROM actor
FULL JOIN film_actor USING (actor_id)
FULL JOIN film USING (film_id)
```

이 쿼리는 다음을 포함하는 결과를 반환합니다:

- 영화에 출연한 배우들(양쪽이 일치)
- 영화에 출연하지 않은 배우들(film 테이블에서 NULL)
- 배우가 없는 영화들(actor 테이블에서 NULL)

## NATURAL JOIN이란?

`NATURAL JOIN`은 공통 이름을 가진 모든 열(column)을 사용하여 자동으로 테이블을 조인합니다. 명시적으로 `USING` 절을 작성하는 대신:

```sql
SELECT first_name, last_name, title
FROM actor
NATURAL JOIN film_actor
NATURAL JOIN film
```

이렇게 작성할 수 있습니다. 동일한 결과를 생성하지만 `USING (actor_id)`와 `USING (film_id)`를 명시적으로 작성할 필요가 없습니다.

그러나 실제 상황에서 이것은 전혀 의미가 없습니다. 현실 세계에서는 `NATURAL JOIN`에서 고려되어서는 안 되는 우연히 충돌하는 열 이름이 항상 존재하기 때문입니다. 예를 들어, `ID`, `CREATED_AT`, `MODIFIED_AT` 같은 열들이 그렇습니다.

## 그렇다면 왜 NATURAL FULL OUTER JOIN을 사용할까요?

제가 작업하던 상황에서 이 조합이 유용했습니다. 두 개의 유사한 결제 관련 테이블이 있었습니다: `PAYMENT`와 `STANDING_ORDER`. 두 테이블 모두 다음과 같은 공통 열을 가지고 있었습니다:

- `ID`
- `CREATED_AT`
- `MODIFIED_AT`
- `DELETED`
- `AMOUNT`
- `CURRENCY`

그리고 각 테이블에는 고유한 열들도 있었습니다.

## 해결책: 행 타입(Row Type) 생성하기

두 테이블 구조를 결합하는 뷰(view)를 생성하여 두 테이블의 모든 열을 포함하는 행 타입을 만들 수 있습니다:

```sql
CREATE VIEW like_a_payment
SELECT *
FROM payment
NATURAL FULL JOIN standing_order
WHERE 1 = 0
```

이 접근 방식이 작동하는 이유:

- `NATURAL`은 모든 공통 열로 자동으로 조인합니다
- `FULL`은 어느 테이블에 데이터가 있든 결과를 보장하며, 일치하지 않는 열은 `NULL`로 채워집니다
- `WHERE 1 = 0` 절은 구조만 설정하고 실제 행은 반환하지 않습니다

## PL/SQL에서의 구현

이 뷰를 사용하여 레코드를 채우는 방법:

```sql
DECLARE
  l_paym_id like_a_payment.id%TYPE;
  l_paym    like_a_payment%ROWTYPE;
BEGIN
  l_paym_id := 1;

  SELECT *
  INTO l_paym
  FROM (
    SELECT * FROM payment WHERE id = l_paym_id
  )
  NATURAL FULL JOIN (
    SELECT * FROM standing_order WHERE id = l_paym_id
  );
END;
```

이 패턴에서는 일반적으로 하나의 쿼리만 데이터를 반환하고, 다른 쿼리는 `NULL` 값을 기여합니다. 이 접근 방식은 열 정의가 변경될 때마다 수동으로 추적할 필요가 없어집니다.

## 결론

물론 이것은 일종의 "핵(hack)"입니다. 그리고 이 기술이 작동하려면 몇 가지 조건이 충족되어야 했습니다:

- 모든 열을 포괄적으로 사용해야 함
- Oracle 성능을 위해 파생 테이블(derived table)이 필요함
- 각 데이터 유형에 대해 별도의 쿼리가 필요함

하지만 유사한 테이블 구조를 통합할 때 빠른 리팩토링을 위해 이 방법이 유용하게 사용될 수 있습니다. `NATURAL FULL OUTER JOIN`이 실제로 유용한 드문 경우 중 하나입니다!

# jOOQ로 Oracle 스타일 암묵적 조인을 ANSI JOIN으로 자동 변환하기

> 원문: https://blog.jooq.org/automatically-transform-oracle-style-implicit-joins-to-ansi-join-using-jooq/

jOOQ는 주로 Java에서 임베디드 동적 SQL을 위한 내부 SQL DSL로 사용되지만, 점점 더 중요한 보조 목적을 수행하고 있습니다: 바로 파서 기능입니다. jOOQ 3.9에서 DDL 문 파싱과 스키마 리버스 엔지니어링을 위해 처음 도입된 파서는 이후 크게 확장되었습니다. 이제 명령줄 인터페이스, 웹사이트 또는 표준 jOOQ API를 통해 SQL 변환 기능을 제공합니다. jOOQ 3.14에서 추가된 주목할 만한 기능 중 하나는 레거시 Oracle 스타일 암묵적 조인을 현대적인 ANSI JOIN 구문으로 변환하는 것입니다.

## 왜 "암묵적 조인"을 피해야 할까요?

오래된 Oracle 암묵적 조인 구문은 대부분의 관계형 데이터베이스 시스템에서 여전히 지원되고 최적화됩니다. SQL-92 표준 이전에는 이것이 표준적인 조인 방식이었습니다. Sakila 데이터베이스의 예시를 보겠습니다:

```sql
SELECT *
FROM actor a, film_actor fa, film f
WHERE a.actor_id = fa.actor_id
AND fa.film_id = f.film_id
```

직관적으로 보이지만, 이 구문은 오류가 발생하기 쉽습니다. 개발자는 모든 조인 조건을 반드시 포함해야 하며, 하나라도 누락하면 데이터 불일치가 발생합니다. SQL-92부터 표준화된 ANSI JOIN 구문은 명시적인 명확성을 제공합니다:

```sql
SELECT *
FROM actor a
JOIN film_actor fa ON a.actor_id = fa.actor_id
JOIN film f ON fa.film_id = f.film_id
```

이 구문은 ON 절을 누락하면 구문 오류가 발생하기 때문에 조건을 실수로 빠뜨리는 것을 방지합니다(안타깝게도 ON 절이 선택 사항인 MySQL은 예외입니다).

## jOOQ의 암묵적 JOIN

jOOQ와 JPQL에서 "암묵적 조인"이라는 용어는 다른 의미를 가집니다. jOOQ는 ANSI SQL보다 더욱 오류 방지에 효과적인 외래 키 경로 기반 시스템을 구현합니다:

```java
ctx.select(
    FILM_ACTOR.actor().asterisk(),
    FILM_ACTOR.asterisk(),
    FILM_ACTOR.film().asterisk())
   .from(FILM_ACTOR)
   .fetch();
```

일대일 관계 경로를 참조하면 적절한 LEFT JOIN 또는 INNER JOIN 절이 자동으로 추가됩니다. 이것은 표준 ANSI JOIN을 대체하는 것이 아니라 그 위에 구축된 편의 기능입니다.

## Oracle 암묵적 조인 변환하기

레거시 코드베이스를 현대화하는 데 jOOQ의 변환 기능은 매우 유용합니다. 사용자는 무료 웹사이트 https://www.jooq.org/translate 를 이용하거나 프로그래밍 방식을 사용할 수 있습니다.

### 예제 1: 기본 암묵적 조인 변환

입력 쿼리:

```sql
SELECT
    a.first_name,
    a.last_name,
    count(c.category_id)
FROM
    actor a,
    film_actor fa,
    film f,
    film_category fc,
    category c
WHERE a.actor_id = fa.actor_id
AND fa.film_id = f.film_id
AND fc.category_id = c.category_id
GROUP BY
    a.actor_id,
    a.first_name,
    a.last_name
```

출력:

```sql
SELECT
    a.first_name,
    a.last_name,
    count(c.category_id)
FROM actor a
JOIN film_actor fa
    ON a.actor_id = fa.actor_id
JOIN film f
    ON fa.film_id = f.film_id
CROSS JOIN (
    film_category fc
    JOIN category c
        ON fc.category_id = c.category_id
)
GROUP BY
    a.actor_id,
    a.first_name,
    a.last_name
```

이 도구는 film과 film_category 테이블 사이의 누락된 조인 조건을 즉시 발견하며, 암묵적 조인의 오류 발생 가능성을 보여줍니다. 수정 후:

수정된 입력:

```sql
SELECT
    a.first_name,
    a.last_name,
    count(c.category_id)
FROM
    actor a,
    film_actor fa,
    film f,
    film_category fc,
    category c
WHERE a.actor_id = fa.actor_id
AND fa.film_id = f.film_id
AND f.film_id = fc.film_id
AND fc.category_id = c.category_id
GROUP BY
    a.actor_id,
    a.first_name,
    a.last_name
```

수정된 출력:

```sql
SELECT
    a.first_name,
    a.last_name,
    count(c.category_id)
FROM actor a
JOIN film_actor fa
    ON a.actor_id = fa.actor_id
JOIN film f
    ON fa.film_id = f.film_id
JOIN film_category fc
    ON f.film_id = fc.film_id
JOIN category c
    ON fc.category_id = c.category_id
GROUP BY
    a.actor_id,
    a.first_name,
    a.last_name
```

### 예제 2: Oracle 외부 조인 구문 ((+) 표기법 사용)

이 도구는 Oracle의 `(+)` 외부 조인 표기법과 SQL Server의 더 이상 사용되지 않는 `*=` 구문도 처리합니다.

외부 조인 마커가 있는 입력:

```sql
SELECT
    a.first_name,
    a.last_name,
    count(c.category_id)
FROM
    actor a,
    film_actor fa,
    film f,
    film_category fc,
    category c
WHERE a.actor_id = fa.actor_id(+)
AND fa.film_id = f.film_id(+)
AND f.film_id = fc.film_id(+)
AND fc.category_id(+) = c.category_id
GROUP BY
    a.actor_id,
    a.first_name,
    a.last_name
```

초기 변환 (오류 포함):

```sql
SELECT
    a.first_name,
    a.last_name,
    count(c.category_id)
FROM actor a
LEFT OUTER JOIN film_actor fa
    ON a.actor_id = fa.actor_id
LEFT OUTER JOIN film f
    ON fa.film_id = f.film_id
LEFT OUTER JOIN (
    film_category fc
    RIGHT OUTER JOIN category c
        ON fc.category_id = c.category_id
)
    ON f.film_id = fc.film_id
GROUP BY
    a.actor_id,
    a.first_name,
    a.last_name
```

잘못 배치된 `(+)` 기호가 원치 않는 RIGHT OUTER JOIN을 생성하며, 이는 다시 한번 레거시 구문의 오류 발생 가능성을 보여줍니다.

수정된 입력:

```sql
SELECT
    a.first_name,
    a.last_name,
    count(c.category_id)
FROM
    actor a,
    film_actor fa,
    film f,
    film_category fc,
    category c
WHERE a.actor_id = fa.actor_id(+)
AND fa.film_id = f.film_id(+)
AND f.film_id = fc.film_id(+)
AND fc.category_id = c.category_id(+)
GROUP BY
    a.actor_id,
    a.first_name,
    a.last_name
```

수정된 출력:

```sql
SELECT
    a.first_name,
    a.last_name,
    count(c.category_id)
FROM actor a
LEFT OUTER JOIN film_actor fa
    ON a.actor_id = fa.actor_id
LEFT OUTER JOIN film f
    ON fa.film_id = f.film_id
LEFT OUTER JOIN film_category fc
    ON f.film_id = fc.film_id
LEFT OUTER JOIN category c
    ON fc.category_id = c.category_id
GROUP BY
    a.actor_id,
    a.first_name,
    a.last_name
```

## 결론

https://www.jooq.org/translate 에서 사용할 수 있는 변환 도구를 직접 사용해 보시고 의견을 알려주세요!

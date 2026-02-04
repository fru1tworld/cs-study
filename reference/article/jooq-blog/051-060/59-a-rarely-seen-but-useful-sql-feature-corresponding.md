# 잘 알려지지 않았지만 유용한 SQL 기능: CORRESPONDING

> 원문: https://blog.jooq.org/a-rarely-seen-but-useful-sql-feature-corresponding/

SQL 표준에는 거의 사용되지 않지만 매우 유용한 키워드가 있다. 바로 `CORRESPONDING`이다. 이 키워드는 `UNION`, `INTERSECT`, `EXCEPT`와 같은 집합 연산(set operation)과 함께 사용되며, 여러 테이블의 컬럼을 위치가 아닌 이름으로 자동 매칭해준다.

## CORRESPONDING의 동작 원리

일반적으로 집합 연산을 수행할 때는 각 하위 쿼리에서 동일한 컬럼을 수동으로 선택해야 한다. 예를 들어, Sakila 데이터베이스에서 사람 관련 테이블 세 개(actor, customer, staff)를 결합하려면 보통 다음과 같이 작성한다:

```sql
SELECT first_name, last_name
FROM actor
UNION ALL
SELECT first_name, last_name
FROM customer
UNION ALL
SELECT first_name, last_name
FROM staff
```

`CORRESPONDING` 키워드를 사용하면, 데이터베이스가 모든 테이블에서 공통으로 존재하는 컬럼 이름의 교집합을 자동으로 식별하고, 해당 컬럼만을 프로젝션(projection)하여 집합 연산을 적용한다. 즉, 다음과 같이 작성할 수 있다:

```sql
SELECT *
FROM actor
UNION ALL CORRESPONDING
SELECT *
FROM customer
UNION ALL CORRESPONDING
SELECT *
FROM staff
```

이 쿼리는 세 테이블 모두에 존재하는 `FIRST_NAME`, `LAST_NAME`, `LAST_UPDATE` 컬럼을 자동으로 식별하여 해당 컬럼만 결과에 포함시킨다.

## CORRESPONDING BY 절

더 안전한 접근 방식은 `CORRESPONDING BY`를 사용하여 포함할 컬럼을 명시적으로 지정하는 것이다. 이는 `JOIN...USING()` 구문과 유사한 방식이다:

```sql
SELECT *
FROM actor
UNION ALL CORRESPONDING BY (first_name, last_name)
SELECT *
FROM customer
```

이렇게 하면 의도하지 않은 컬럼이 포함되는 것을 방지할 수 있으며, 스키마가 변경되더라도 쿼리의 유지보수성이 높아지고 깨지기 어려워진다.

## 실용적인 활용 사례

단순한 `UNION` 외에도 `CORRESPONDING`은 `INTERSECT`와 `EXCEPT` 쿼리에서도 의미 있게 사용할 수 있다. 예를 들어, 배우와 동일한 이름을 가진 고객을 찾으려면 다음과 같이 작성할 수 있다:

```sql
SELECT *
FROM actor
INTERSECT CORRESPONDING BY (first_name, last_name)
SELECT *
FROM customer
```

이 쿼리는 `first_name`과 `last_name`이 동일한 배우와 고객의 교집합을 반환한다.

## 위험성과 고려사항

`CORRESPONDING`은 `NATURAL JOIN`과 유사한 위험성을 내포하고 있다. 스키마가 변경되면 구문 오류 없이 쿼리 결과가 자동으로 달라질 수 있다. 테이블 구조가 예상치 않게 변경될 경우 예기치 않은 동작이 발생할 수 있으므로 주의가 필요하다. 따라서 `CORRESPONDING BY`를 사용하여 컬럼을 명시적으로 지정하는 것이 더 안전한 방법이다.

## 역사적 배경

이 키워드를 ANSI X3H2에 제안한 Joe Celko에 따르면, 그는 COBOL에서 이 용어를 차용했으며, SQL 개발자들에게 직관적인 선택이 되도록 의도했다고 한다.

## 현재 지원 현황

현재 이 기능은 HSQLDB에서 구현되어 있다. PostgreSQL에서는 Vik Fearing이 작업 중인 브랜치가 있지만, 개발이 멈춘 상태이다. jOOQ 라이브러리는 API, 파서(parser), 번역기(translator)를 통해 이 기능의 지원을 검토하고 있다.

이 기능은 현대 데이터베이스 시스템에서 "거의 볼 수 없는" 상태로 남아 있지만, 여러 테이블의 데이터를 집합 연산으로 결합할 때 코드의 간결성과 가독성을 크게 향상시킬 수 있는 유용한 SQL 표준 기능이다.

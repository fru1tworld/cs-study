# SQL DISTINCT와 ORDER BY의 관계

> 원문: https://blog.jooq.org/how-sql-distinct-and-order-by-are-related/

SQL에서 `DISTINCT`와 `ORDER BY`를 결합할 때 예상치 못한 제약 사항들로 인해 많은 사용자들이 혼란을 겪는다. 이 글에서는 왜 특정 쿼리들이 실패하는지, 그리고 `DISTINCT` 없이는 유사한 쿼리가 잘 작동하는지를 설명한다.

## 기본 사항

먼저 세 가지 기본적인 쿼리 패턴을 살펴보자.

쿼리 1: 고유한 길이 값을 임의의 순서로 반환한다 (중복 제거를 위해 해싱이 사용될 수 있기 때문이다):

```sql
SELECT DISTINCT length FROM film
```

쿼리 2: 중복을 포함하여 모든 행을 길이 순으로 정렬하여 반환한다:

```sql
SELECT length FROM film ORDER BY length
```

쿼리 3: 두 연산을 결합하여 고유한 길이 값을 오름차순으로 반환한다:

```sql
SELECT DISTINCT length FROM film ORDER BY length
```

## 문제: 왜 이것은 실패하는가

다음 쿼리를 실행하려고 하면:

```sql
SELECT DISTINCT length FROM film ORDER BY title
```

대부분의 데이터베이스에서 오류가 발생한다. Oracle에서는 "ORA-01791: not a SELECTed expression"과 같은 오류 메시지가 나타난다.

그러나 다음 쿼리는 정상적으로 작동한다:

```sql
SELECT length FROM film ORDER BY title
```

이 쿼리는 영화 제목 순으로 정렬된 길이 값을 반환한다.

## 논리적 연산 순서 설명

핵심적인 통찰은 SQL 연산의 논리적 순서와 구문적 순서의 차이를 이해하는 것이다.

`SELECT DISTINCT length FROM film ORDER BY length` 쿼리의 경우, 데이터베이스는 다음 순서로 처리한다:

1. `FROM` 절이 FILM 테이블을 로드한다
2. `SELECT` 절이 LENGTH 열을 투영한다
3. `DISTINCT`가 중복 튜플을 제거한다
4. `ORDER BY`가 LENGTH로 정렬한다

`SELECT length FROM film ORDER BY title` 쿼리의 경우, 데이터베이스는 "확장된 정렬 키 열(extended sort key columns)"이라는 개념을 적용한다:

1. `FROM`이 테이블을 로드한다
2. `SELECT`가 LENGTH를 투영하고 내부적으로 TITLE을 합성적으로 추가한다
3. `ORDER BY`가 TITLE을 사용하여 정렬한다
4. 암묵적인 최종 `SELECT`가 LENGTH만 투영하고 TITLE을 버린다

## 왜 DISTINCT가 이 패턴을 깨뜨리는가

`SELECT DISTINCT length FROM film ORDER BY title`을 결합하면:

- 확장된 정렬 키 열이 추가된 후 `DISTINCT` 연산은 (LENGTH, TITLE) 튜플에 적용되어야 한다
- 이것은 `DISTINCT`의 의미론을 근본적으로 변경한다
- 쿼리가 모호해진다: "각 고유한 길이에 대해 어떤 제목이 반환되어야 하는가?"

다음 데이터를 포함하는 테이블 T로 설명해보자:

| A | B |
|---|---|
| 1 | 1 |
| 1 | 2 |
| 2 | 3 |
| 1 | 4 |
| 2 | 5 |

`SELECT DISTINCT a FROM t ORDER BY b` 쿼리는 모호성을 만들어낸다. 왜냐하면 각 고유한 A 값에 대해 여러 B 값이 매핑되기 때문이다. A=1에 대해 B는 1, 2, 4가 될 수 있고, A=2에 대해 B는 3, 5가 될 수 있다. 데이터베이스는 정렬을 위해 어떤 B 값을 선택해야 하는지 알 수 없다.

## 규칙의 예외

`DISTINCT`와 함께 `ORDER BY`의 일부 표현식은 허용된다:

```sql
SELECT DISTINCT length FROM film ORDER BY mod(length, 10), length
```

이 쿼리는 `MOD(length, 10)`이 선택된 열에서 파생될 수 있기 때문에 작동한다.

```sql
SELECT DISTINCT a, b FROM t ORDER BY a - b
```

이 쿼리도 `a - b`가 선택된 열들에서 파생될 수 있기 때문에 작동한다.

이러한 쿼리들이 작동하는 이유는 `ORDER BY` 표현식이 확장된 정렬 키 열을 필요로 하지 않고 `SELECT` 목록 표현식에서 완전히 파생될 수 있기 때문이다.

핵심 규칙: "`DISTINCT`를 사용할 때 ORDER BY 표현식은 SELECT 목록에서 파생 가능해야 한다."

## 모호성 문제

각 길이당 여러 제목을 포함하는 구성된 테이블에서, `SELECT DISTINCT length FROM film ORDER BY title`은 불가능한 정렬 로직을 만들어낸다 - 중복된 길이에 대해 어떤 제목이 정렬 순서를 결정해야 하는가?

이러한 근본적인 모호성이 대부분의 데이터베이스가 그러한 쿼리를 거부하는 이유를 설명한다.

## 결론

SQL이 이상하게 느껴지는 이유는 구문적 순서가 논리적 순서와 다르기 때문이다. `DISTINCT`와 `ORDER BY`에 대한 제약 사항은 이러한 불일치에서 비롯된다. `GROUP BY`, `LIMIT/FETCH`, 또는 `UNION`을 추가하면 SQL의 논리적 연산 순서에 대한 더 깊은 이해가 필요한 추가적인 복잡성이 생긴다.

# 추가한 인덱스가 쓸모없다. 왜?

> 원문: https://blog.jooq.org/the-index-youve-added-is-useless-why/

Bob은 자신이 Oracle 데이터베이스에서 경쟁 조건을 발견했다고 생각했다. 그가 보기에는 누군가(Alice)가 엄청나게 느린 쿼리를 실행하고 있어서 공유 커넥션을 모두 차지하고 있었기 때문이다. 만약 Alice가 `DATE_OF_BIRTH`라는 열에 인덱스를 추가해뒀다면, 그녀의 쿼리는 훨씬 빨라졌을 것이다. Bob은 생각했다: "이거 쉽네, 내가 해결할 수 있어". 그래서 그는 다음과 같이 인덱스를 추가했다:

```sql
CREATE INDEX i_date_of_birth ON person(date_of_birth);
```

그런 다음 그는 인덱스가 실제로 사용되는지 테스트했다:

```sql
SELECT *
FROM person
WHERE date_of_birth > DATE '1980-01-01';
```

실행 계획은 인덱스가 사용되고 있음을 보여줬다. 훌륭하다!

```
--------------------------------------------------------------
| Id  | Operation                   | Name            | Rows |
--------------------------------------------------------------
|   0 | SELECT STATEMENT            |                 |    1 |
|   1 |  TABLE ACCESS BY INDEX ROWID| PERSON          |    1 |
|*  2 |   INDEX RANGE SCAN          | I_DATE_OF_BIRTH |    1 |
--------------------------------------------------------------

Predicate Information (identified by operation id):
---------------------------------------------------

   2 - access("DATE_OF_BIRTH">TO_DATE(' 1980-01-01 00:00:00',
              'syyyy-mm-dd hh24:mi:ss'))
```

그래서 Bob은 만족스러워하며 점심을 먹으러 갔고 Alice에게 그녀의 쿼리를 다시 실행해보라고 말했다...

하지만 "다시" 아무것도 변하지 않았다. 여전히 느렸다. 인덱스가 사용되지 않았다.

## 무슨 일이 일어난 걸까?

불쌍한 Bob은 Oracle이 '일반적인' 인덱스에 NULL 값을 저장하지 않는다는 것을 잊었다(또는 몰랐다). 이것이 바로 Alice의 쿼리에 문제를 일으킨 것이다. Alice의 쿼리를 살펴보자:

```sql
SELECT *
FROM event
WHERE event_date NOT IN (
  SELECT date_of_birth FROM person
);
```

무엇이 문제일까? Bob의 인덱스에는 NULL이 들어있지 않기 때문에 `NOT IN` 술어와 함께 사용할 수 없다. 실행 계획을 살펴보자:

```
----------------------------------------------------------
| Id  | Operation          | Name   | Rows | Cost (%CPU)|
----------------------------------------------------------
|   0 | SELECT STATEMENT   |        |    1 |     5   (0)|
|*  1 |  FILTER            |        |      |            |
|   2 |   TABLE ACCESS FULL| EVENT  |    1 |     2   (0)|
|*  3 |   TABLE ACCESS FULL| PERSON |    1 |     3   (0)|
----------------------------------------------------------

Predicate Information (identified by operation id):
---------------------------------------------------

   1 - filter( NOT EXISTS (SELECT 0 FROM "PERSON" "PERSON"
              WHERE LNNVL("DATE_OF_BIRTH"<>:B1)))
   3 - filter(LNNVL("DATE_OF_BIRTH"<>:B1))
```

무슨 일이 일어났을까? Oracle은 내부적으로 `NOT IN`을 `NOT EXISTS`로 변환해야 했다 - 하지만 `LNNVL()` 함수를 함께 사용해야 했다. 왜 그럴까?

### 원인

`NOT IN (a, b, NULL, c, d)`는 항상 NULL을 반환한다. 우리의 `DATE '1980-01-01'` 값이 인덱스에 있든 없든, 우리는 여전히 전체 테이블을 스캔하여 `date_of_birth` 열에 단 하나의 NULL 값이라도 포함되어 있는지 확인해야 한다. 왜냐하면 만약 NULL 값이 있다면, Alice 쿼리의 `NOT IN` 술어는 절대 TRUE나 FALSE를 반환하지 않고 NULL을 반환하기 때문이다.

## 해결책

Alice는 `NOT IN`을 `NOT EXISTS`로 교체함으로써 이 문제를 쉽게 스스로 해결할 수 있다. `NOT EXISTS`는 SQL의 독특한 3값 불리언 논리로 인한 문제가 없다:

```sql
SELECT *
FROM event
WHERE NOT EXISTS (
  SELECT 1 FROM person
  WHERE person.date_of_birth = event.event_date
);
```

그러나 최선의 해결책은 단순히 해당 열을 `NOT NULL`로 설정하는 것이다. 만약 Alice가 날짜 값이 없는 사람들을 원한다면, 그녀는 NULL과 다른 센티넬 값을 사용해야 한다. `NOT NULL`로 설정하면 다음과 같이 할 수 있다:

```sql
ALTER TABLE person MODIFY date_of_birth DATE NOT NULL;
```

이렇게 하면 Bob의 인덱스를 사용할 수 있게 된다. 그러나 더 중요한 것은, Alice가 원래의 `NOT IN` 쿼리를 다시 사용할 수 있다는 것이다. 왜냐하면 더 이상 SQL의 3값 논리를 고려할 필요가 없기 때문이다. NULL은 `date_of_birth` 열에서 더 이상 가능한 값이 아니다.

## 이러한 문제를 진단하는 방법

여러분의 스키마에서 nullable 열을 포함하는 인덱스를 찾으려면 다음 쿼리를 사용할 수 있다:

```sql
SELECT
  i.index_name,
  i.table_name,
  LISTAGG(
    LPAD(c.column_name, 30) ||
    ' (' || DECODE(c.nullable, 'Y', 'NULL', 'NOT NULL') || ')',
    ', '
  ) WITHIN GROUP (ORDER BY ic.column_position) AS columns
FROM user_indexes i
JOIN user_ind_columns ic
  ON i.index_name = ic.index_name
JOIN user_tab_cols c
  ON (ic.table_name, ic.column_name) = ((c.table_name, c.column_name))
WHERE EXISTS (
  SELECT 1
  FROM user_ind_columns ic2
  JOIN user_tab_cols c2
    ON (ic2.table_name, ic2.column_name) = ((c2.table_name, c2.column_name))
  WHERE i.index_name = ic2.index_name
  AND c2.nullable = 'Y'
)
GROUP BY i.index_name, i.table_name;
```

이 쿼리는 적어도 하나의 nullable 열을 포함하는 모든 인덱스를 나열한다. 이를 통해 팀은 스키마를 감사하고 적절한 곳에 `NOT NULL` 제약 조건을 추가하여 성능을 개선할 수 있다.

## 결론

데이터베이스 인덱스를 추가할 때는 해당 열의 NULL 가능성을 항상 고려해야 한다. Oracle과 같은 많은 데이터베이스에서 NULL 값은 일반 인덱스에 저장되지 않는다. 이는 `NOT IN`과 같은 술어를 사용할 때 인덱스가 완전히 무시되어 전체 테이블 스캔이 발생할 수 있음을 의미한다.

가능하다면:
1. 열에 `NOT NULL` 제약 조건을 추가하라
2. `NOT IN` 대신 `NOT EXISTS`를 사용하라
3. 인덱스를 생성하기 전에 실제 쿼리 패턴을 분석하라

이 간단한 원칙들을 기억하면 Bob과 Alice 모두 더 빠른 쿼리를 즐길 수 있을 것이다.

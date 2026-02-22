# 몰랐던 진정한 SQL 보석: EVERY() 집계 함수

> 원문: https://blog.jooq.org/a-true-sql-gem-you-didnt-know-yet-the-every-aggregate-function/

게시일: 2014년 12월 18일 (2022년 11월 7일 업데이트)
작성자: lukaseder

---

우리는 방금 jOOQ에 `EVERY()` 집계 함수 지원(#1391)을 추가했으며, 이 기회를 빌어 가끔(`EVERY(now and then)`) 유용하게 사용될 수 있는 이 진정한 SQL 보석에 대해 알려드리고자 합니다.

## 샘플 데이터

다음과 같은 book 테이블 데이터가 있다고 가정합니다:

```sql
INSERT INTO book (id, author_id, title) VALUES
(1, 1, '1984'),
(2, 1, 'Animal Farm'),
(3, 2, 'O Alquimista'),
(4, 2, 'Brida');
```

## 첫 번째 쿼리 예제

질문: "모든(`EVERY()`) ID가 10보다 작은가?"

```sql
SELECT EVERY(id < 10)
FROM book
```

결과:
```
every
-----
true
```

모든 book의 id가 10보다 작으므로 `true`를 반환합니다.

## 두 번째 쿼리 예제

질문: "각 저자별로 모든(`EVERY()`) 책 제목이 문자 'a'로 끝나는가?"

```sql
SELECT author_id, EVERY(title LIKE '%a')
FROM book
GROUP BY author_id
```

결과:
```
author_id   every
-----------------
1           false
2           true
```

저자 1(조지 오웰)의 책들 중 '1984'는 'a'로 끝나지 않으므로 `false`입니다. 저자 2(파울로 코엘료)의 책들 'O Alquimista'와 'Brida'는 모두 'a'로 끝나므로 `true`입니다.

## 윈도우 함수로서의 EVERY()

`EVERY()`는 윈도우 함수로도 사용할 수 있습니다:

```sql
SELECT
  book.*,
  EVERY(title LIKE '%a') OVER (PARTITION BY author_id)
FROM book
```

결과:
```
id  author_id   title          every
------------------------------------
1   1           1984           false
2   1           Animal Farm    false
3   2           O Alquimista   true
4   2           Brida          true
```

각 행에 대해 동일한 author_id를 가진 모든 책들이 조건을 만족하는지 보여줍니다.

## 데이터베이스 지원

SQL 표준은 섹션 10.9의 "계산 연산(computational operations)" 부분에 EVERY를 포함하고 있습니다. PostgreSQL은 집계 함수 문서에서 이 함수를 명시적으로 지원합니다.

## EVERY()를 지원하지 않는 데이터베이스를 위한 에뮬레이션

EVERY() 함수를 네이티브로 지원하지 않는 데이터베이스에서는 다음과 같이 에뮬레이션할 수 있습니다:

```sql
SELECT MIN(CASE WHEN id < 10 THEN 1 ELSE 0 END)
FROM book;
```

이 방법은 조건이 거짓인 행이 하나라도 있으면 0을 반환하고, 모든 행이 조건을 만족하면 1을 반환합니다.

윈도우 함수 에뮬레이션:

```sql
SELECT
  book.*,
  MIN(CASE WHEN title LIKE '%a' THEN 1 ELSE 0 END)
    OVER(PARTITION BY author_id)
FROM book;
```

## jOOQ 구현

jOOQ 3.6 이상에서는 이 에뮬레이션을 자동으로 처리하므로, 기본 데이터베이스의 지원 여부와 관계없이 이식 가능한 코드를 작성할 수 있습니다:

```java
DSL.using(configuration)
   .select(BOOK.fields())
   .select(every(BOOK.TITLE.like("%a"))
           .over(partitionBy(BOOK.AUTHOR_ID)))
   .from(BOOK)
   .fetch();
```

---

## 댓글 섹션

Stew Ashton이 대안적인 에뮬레이션 방법을 제공했습니다:

```sql
SELECT 1-sign(SUM(CASE WHEN id < 10 THEN 0 ELSE 1 END))
FROM book;
```

Julian Hyde가 언급하기를: "데이터베이스가 BOOLEAN 값에 대해 MIN과 MAX를 허용한다면(FALSE < TRUE인 경우), EVERY는 MIN과 동등합니다."

즉, 다음과 같이 사용할 수 있습니다:

```sql
SELECT MIN(id < 10) AS bool_and
FROM book;
```

이 방법은 BOOLEAN 타입에서 MIN/MAX 연산이 가능한 데이터베이스에서 작동합니다.

---

결론: `EVERY()` 함수는 SQL에서 집계 불리언 로직을 수행하는 데 매우 유용하지만 많이 알려지지 않은 기능입니다. jOOQ를 사용하면 데이터베이스의 네이티브 지원 여부와 관계없이 일관된 방식으로 이 기능을 활용할 수 있습니다.

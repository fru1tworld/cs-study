# 놀라운 SQL 트릭: 뷰에 대한 제약 조건

> 원문: https://blog.jooq.org/awesome-sql-trick-constraints-on-views/

게시일: 2014년 9월 2일
저자: lukaseder

CHECK 제약 조건은 데이터 정제에 유용하지만, 테이블 수준에서만 적용됩니다. 컨텍스트별 제약 조건이 필요할 때, SQL 표준의 `WITH CHECK OPTION` 절(Oracle과 SQL Server에서 지원)이 해결책을 제공합니다.

## 코드 예제 1: CHECK OPTION이 있는 뷰 생성

```sql
CREATE TABLE books (
  id    NUMBER(10)         NOT NULL,
  title VARCHAR2(100 CHAR) NOT NULL,
  price NUMBER(10, 2)      NOT NULL,

  CONSTRAINT pk_book PRIMARY KEY (id)
);

CREATE VIEW expensive_books
AS
SELECT id, title, price
FROM books
WHERE price > 100
WITH CHECK OPTION;

INSERT INTO books
VALUES (1, '1984', 35.90);

INSERT INTO books
VALUES (
  2,
  'The Answer to Life, the Universe, and Everything',
  999.90
);
```

`expensive_books` 뷰는 가격이 100.00을 초과하는 책만 보여줍니다:

```
ID TITLE                                       PRICE
-- ----------------------------------------- -------
 2 The Answer to Life, the Universe, and ...   999.9
```

## 제약 조건 위반 예제

expensive_books 뷰를 통해 저렴한 책을 삽입하려는 시도는 실패합니다:

```sql
INSERT INTO expensive_books
VALUES (3, '10 Reasons why jOOQ is Awesome', 9.99);
```

결과: `ORA-01402: view WITH CHECK OPTION where-clause violation`

마찬가지로, 비싼 책을 비싸지 않은 가격으로 업데이트하는 것도 차단됩니다:

```sql
UPDATE expensive_books
SET price = 9.99;
```

## 인라인 WITH CHECK OPTION

```sql
INSERT INTO (
  SELECT *
  FROM expensive_books
  WHERE price > 1000
  WITH CHECK OPTION
) really_expensive_books
VALUES (3, 'Modern Enterprise Software', 999.99);
```

이것도 `ORA-01402` 에러를 발생시킵니다.

## SQL 변환 응용

CHECK OPTION은 적절한 접근 권한이 부여된 저장된 뷰에서 잘 작동합니다. 인라인 CHECK OPTION은 애플리케이션 계층에서 동적 SQL 변환에 유용하며, jOOQ의 SQL 변환 기능을 사용하여 멀티 테넌시나 행 수준 보안을 구현하는 데 활용할 수 있습니다.

---

태그: CHECK CONSTRAINT, CHECK OPTION, constraints, Multi-Tenancy, Oracle, row-level security, sql, SQL Server, WITH CHECK OPTION

# SQL DML에서 데이터를 반환하는 다양한 방법

> 원문: https://blog.jooq.org/the-many-ways-to-return-data-from-sql-dml/

SQL에서 표준화하기 가장 어려운 것은 아마도 DML 문에서 데이터를 `RETURNING`하는 것일 것이다. 이 글에서는 jOOQ를 사용하여, jOOQ가 지원하는 다양한 방언에서, 그리고 JDBC를 직접 사용하여 이를 수행하는 여러 가지 방법을 살펴보겠다.

## jOOQ에서의 사용 방법

다음과 같은 테이블 구조를 가정한다:

```sql
CREATE TABLE actor (
  id INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
  first_name TEXT,
  last_name TEXT,
  last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

jOOQ는 Firebird, MariaDB, PostgreSQL, Oracle PL/SQL에서 구문적 영감을 받은 `RETURNING` 절 구문을 지원한다:

```sql
INSERT INTO actor (first_name, last_name)
VALUES ('John', 'Doe')
RETURNING id, last_update
```

모든 컬럼을 반환할 수도 있다:

```sql
INSERT INTO actor (first_name, last_name)
VALUES ('John', 'Doe')
RETURNING *
```

jOOQ에서 `RETURNING` 절은 INSERT, UPDATE, DELETE 문에 추가할 수 있으며, SELECT 쿼리의 프로젝션처럼 동작한다. jOOQ 코드로는 다음과 같다:

```java
ActorRecord actor = ctx.insertInto(ACTOR, ACTOR.FIRST_NAME, ACTOR.LAST_NAME)
   .values("John", "Doe")
   .returning()
   .fetchOne();
```

## PL/SQL에서의 지원 방식

Oracle PL/SQL은 이 구문을 지원하지만, PostgreSQL과는 차이가 있다. jOOQ가 단일 행만 삽입된다는 것을 알 수 있을 때는 JDBC의 `Statement.RETURN_GENERATED_KEYS`에 위임한다. 여러 행을 처리하는 경우에는, jOOQ가 `FORALL` 문과 벌크 수집(bulk collection)을 사용하여 정교한 PL/SQL을 생성하고, 커서와 테이블 변수를 통해 반환 데이터를 처리한다.

jOOQ가 여러 행을 삽입할 때 생성하는 PL/SQL 코드는 다음과 같다:

```sql
DECLARE
  i0 DBMS_SQL.VARCHAR2_TABLE;
  i1 DBMS_SQL.VARCHAR2_TABLE;
  o0 DBMS_SQL.VARCHAR2_TABLE;
  o1 DBMS_SQL.TIMESTAMP_TABLE;
  c0 sys_refcursor;
  c1 sys_refcursor;
BEGIN
  i0(1) := ?;
  i0(2) := ?;
  i1(1) := ?;
  i1(2) := ?;
  FORALL i IN 1 .. i0.count
    INSERT INTO actor (first_name, last_name)
    VALUES (i0(i), i1(i))
    RETURNING id, last_update
    BULK COLLECT INTO o0, o1;
  ? := sql%rowcount;
  OPEN c0 FOR SELECT * FROM table(o0);
  OPEN c1 FOR SELECT * FROM table(o1);
  ? := c0;
  ? := c1;
END;
```

## Db2, H2, 표준 SQL에서의 지원 방식

SQL 표준은 `<data change delta table>` 구문을 사용하며, `{ OLD | NEW | FINAL } TABLE` 연산자를 활용한다:

```sql
SELECT id, last_update
FROM FINAL TABLE (
  INSERT INTO actor (first_name, last_name)
  VALUES ('John', 'Doe')
) a
```

이 구문은 세 가지 수정자를 지원한다:

- `OLD`: 수정 전의 데이터를 반환
- `NEW`: 수정 후, 트리거 실행 전의 데이터를 반환
- `FINAL`: 모든 트리거에 의해 생성된 값이 반영된 최종 삽입 데이터를 반환

`RETURNING`과 달리, 이 구문은 `MERGE`에서도 동작한다. 더 강력하지만 가독성은 떨어지는 구문이다.

## 다시 PostgreSQL로 돌아가서

PostgreSQL은 `RETURNING` 문을 공통 테이블 식(CTE, Common Table Expression)인 `WITH` 절 안에 배치할 수 있다는 독특한 특징이 있다:

```sql
WITH a (id, last_update) AS (
  INSERT INTO actor (first_name, last_name)
  VALUES ('John', 'Doe')
  RETURNING id, last_update
)
SELECT *
FROM a;
```

이를 통해 반환된 데이터에 대해 추가적인 쿼리 처리가 가능하다. 그러나 논리적으로 동등함에도 불구하고, PostgreSQL에서 파생 테이블(derived table)은 이 구문을 지원하지 않는다는 점에 유의해야 한다.

## SQL Server의 OUTPUT 절

SQL Server는 `INSERTED`와 `DELETED` 의사 테이블(pseudo-table)과 함께 `OUTPUT` 절을 사용한다. 이를 통해 수정 전후의 데이터에 동시에 접근할 수 있다:

```sql
DECLARE @result TABLE (id INT, last_update DATETIME2);
INSERT INTO actor (first_name, last_name)
OUTPUT inserted.id, inserted.last_update INTO @result
VALUES ('John', 'Doe');
MERGE INTO @result r
USING (SELECT actor.id, actor.last_update AS x FROM actor) s
ON r.id = s.id
WHEN MATCHED THEN UPDATE SET last_update = s.x;
SELECT id, last_update FROM @result;
```

jOOQ는 메모리 내 결과 테이블을 생성하고 MERGE 연산을 수행하여 트리거에 의해 생성된 값을 처리한다.

## JDBC를 사용하여 생성된 키 가져오기 (Oracle, HSQLDB)

기본적인 JDBC 접근 방식은 다음과 같다:

```java
try (PreparedStatement s = c.prepareStatement(
    "INSERT INTO actor (first_name, last_name) VALUES (?, ?)",
    new String[] { "ID", "LAST_UPDATE" }
)) {
    s.setString(1, firstName);
    s.setString(2, lastName);
    s.executeUpdate();

    try (ResultSet rs = s.getGeneratedKeys()) {
        while (rs.next()) {
            System.out.println("ID = " + rs.getInt(1));
            System.out.println("LAST_UPDATE = " + rs.getTimestamp(2));
        }
    }
}
```

## JDBC를 사용하여 생성된 키 가져오기 (기타 데이터베이스)

대부분의 드라이버는 다음과 같은 방식을 사용한다:

```java
try (PreparedStatement s = c.prepareStatement(
    "INSERT INTO actor (first_name, last_name) VALUES (?, ?)",
    Statement.RETURN_GENERATED_KEYS
)) {
    s.setString(1, firstName);
    s.setString(2, lastName);
    s.executeUpdate();

    try (ResultSet rs = s.getGeneratedKeys()) {
        System.out.println("ID = " + rs.getInt(1));
    }
}
```

그러나 많은 드라이버들은 여러 행에 대한 작업, `INSERT`가 아닌 문, 또는 아이덴티티 컬럼이 없는 테이블에서는 이를 지원하지 않는다. 대부분의 데이터베이스는 기본적인 아이덴티티 검색 이상의 생성된 키에 대한 강력한 JDBC 드라이버 지원이 부족하여, 기능이 제한적이다.

## 결론

늘 그렇듯, 다양한 SQL 벤더 간의 차이는 SQL 지원과 JDBC 지원 모두에서 매우 크다. jOOQ는 여러분을 대신해서 JDBC를 해킹해 왔으므로, 여러분이 직접 할 필요가 없다. jOOQ를 사용하면, 위의 모든 것이 일반적으로 모든 방언에서 다음과 같이 동작한다 -- 적어도 단일 행을 삽입할 때는 말이다:

```java
ActorRecord actor = ctx.insertInto(ACTOR, ACTOR.FIRST_NAME, ACTOR.LAST_NAME)
   .values("John", "Doe")
   .returning()
   .fetchOne();
```

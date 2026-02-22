# jOOQ로 T-SQL 함수에 테이블 값 매개변수를 전달하는 방법

> 원문: https://blog.jooq.org/how-to-pass-a-table-valued-parameter-to-a-t-sql-function-with-jooq/

## 개요

Microsoft T-SQL은 테이블 값 매개변수(TVP, Table-Valued Parameter) 를 지원합니다. 이는 테이블 타입의 매개변수를 저장 프로시저나 함수에 전달할 수 있게 해주는 언어 기능입니다. 이 블로그 글에서는 jOOQ의 코드 생성기를 사용하여 TVP를 원활하게 다루는 방법을 보여줍니다.

## T-SQL 예제

먼저 사용자 정의 타입과 함수를 생성하는 T-SQL 예제부터 살펴보겠습니다:

```sql
CREATE TYPE u_number_table AS TABLE (column_value INTEGER);

CREATE FUNCTION f_cross_multiply (
  @numbers u_number_table READONLY
)
RETURNS @result TABLE (
  i1 INTEGER,
  i2 INTEGER,
  product INTEGER
)
AS
BEGIN
  INSERT INTO @result
  SELECT
    n1.column_value,
    n2.column_value,
    n1.column_value * n2.column_value
  FROM @numbers n1
  CROSS JOIN @numbers n2
  RETURN
END
```

이 함수는 TVP를 받아서 교차 곱(cross product) 결과 집합을 반환합니다. 네이티브 T-SQL에서는 다음과 같이 호출합니다:

```sql
DECLARE @t u_number_table;
INSERT INTO @t VALUES (1), (2), (3);
SELECT * FROM f_cross_multiply(@t);
```

## jOOQ를 사용한 Java 구현

네이티브 JDBC에서는 `com.microsoft.sqlserver.jdbc.SQLServerDataTable`을 사용할 수 있지만, jOOQ와 코드 생성기를 함께 사용하면 훨씬 더 간단합니다. 생성된 코드를 통해 함수를 직접 호출할 수 있습니다:

```java
List<Integer> l = List.of(1, 2, 3);
Result<FCrossMultiplyRecord> result = ctx
    .selectFrom(fCrossMultiply(new UNumberTableRecord(
        l.stream().map(UNumberTableElementTypeRecord::new).toList()
    )))
    .fetch();
```

## 생성되는 객체들

코드 생성기는 다음과 같은 주요 산출물을 생성합니다:

1. FCrossMultiplyRecord — 결과 행을 담는 `TableRecord`
2. Routines.fCrossMultiply — 테이블 값 함수 호출을 모델링하는 메서드
3. UNumberTableRecord — 사용자 정의 TVP 타입을 나타내는 레코드
4. UNumberTableElementTypeRecord — 개별 행을 나타내는 합성(synthetic) 레코드 타입

## 출력 결과

이 쿼리는 교차 곱셈 결과를 보여주는 9개의 행으로 구성된 결과 집합을 생성합니다. 결과는 출력하거나 반복 처리할 수 있습니다:

```java
result.forEach(r -> {
    System.out.println(
        r.getI1() + " * " + r.getI2() + " = " + r.getProduct()
    );
});
```

## 결론

jOOQ의 코드 생성기를 SQL Server 데이터베이스에 연결하면, 수동으로 JDBC를 다루는 복잡한 작업 없이도 테이블 값 매개변수를 받는 함수를 간편하게 호출할 수 있습니다.

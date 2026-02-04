> 원문: https://blog.jooq.org/jooq-3-10-will-support-sql-servers-table-valued-parameters/

# jOOQ 3.10에서 SQL Server의 테이블 값 매개변수(Table Valued Parameters) 지원

## 개요

이 글에서는 jOOQ 3.10에서 SQL Server의 테이블 값 매개변수(TVP, Table-Valued Parameters)를 지원하는 방법에 대해 설명합니다. TVP는 개발자가 대량 데이터 처리를 위해 테이블 변수를 저장 프로시저에 전달할 수 있게 해주는 기능입니다.

## 테이블 값 매개변수(Table-Valued Parameters)란?

SQL Server의 TVP는 테이블 구조의 데이터를 저장 프로시저(Stored Procedure)와 인라인 테이블 값 함수(Inline Table-Valued Functions)에 전달할 수 있게 해줍니다. 이 글에서는 숫자 테이블을 받아서 해당 테이블의 자기 자신과의 교차 곱(Cross Product)을 반환하는 `cross_multiply` 함수를 예시로 설명합니다.

## 기존 JDBC 방식

SQL Server JDBC 드라이버를 직접 사용하면 벤더 특화 API 호출이 필요합니다. 개발자는 다음과 같은 작업을 해야 합니다:

- `SQLServerDataTable` 객체 생성
- 이름과 타입을 포함한 컬럼 메타데이터를 수동으로 추가
- 타입 이름, 컬럼 위치, 구조 세부 정보를 기억
- `setStructured()`를 사용하여 매개변수 바인딩

## jOOQ의 간소화된 접근 방식

코드 생성기가 테이블 타입에 대한 타입 안전(Type-Safe) 클래스를 생성하여 수동 바인딩 작업을 제거합니다. 원시 JDBC 코드 대신, 개발자는 다음과 같이 작성할 수 있습니다:

```java
Numbers numbers = new NumbersRecord(
    new NumbersElementTypeRecord(1),
    new NumbersElementTypeRecord(2),
    new NumbersElementTypeRecord(3),
    new NumbersElementTypeRecord(4)
);

Result<CrossMultiplyRecord> r = DSL.using(configuration)
   .selectFrom(crossMultiply(numbers))
   .where(F_CROSS_MULTIPLY.PRODUCT.gt(5))
   .fetch();
```

이 접근 방식은 타입 안전성을 제공하고, jOOQ의 쿼리 빌더와 원활하게 통합되며, 테이블 값 함수를 조건절(Predicate)과 조인(Join)과 함께 FROM 절에 직접 삽입할 수 있게 해줍니다.

## 결론

jOOQ 3.10의 SQL Server TVP 지원은 복잡한 JDBC 코드를 간결하고 타입 안전한 코드로 대체하여 개발자 경험을 크게 향상시킵니다. 이를 통해 대량 데이터를 저장 프로시저에 전달하는 작업이 훨씬 더 직관적이고 안전해집니다.

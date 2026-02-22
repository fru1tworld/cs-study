> 원문: https://blog.jooq.org/use-jooq-to-read-write-oracle-plsql-record-types/

# jOOQ를 사용하여 Oracle PL/SQL RECORD 타입 읽기/쓰기

## 개요

jOOQ 3.9에서는 Oracle PL/SQL RECORD 타입에 대한 지원이 추가되었습니다. 이를 통해 Java 개발자들은 기본적으로 이 기능을 지원하지 않는 JDBC를 통해서도 이러한 복잡한 데이터 구조를 다룰 수 있게 되었습니다.

## 핵심 문제

Oracle의 JDBC 드라이버는 RECORD 타입을 포함한 여러 PL/SQL 기능을 지원하지 않습니다. RECORD 타입은 구조체(struct)와 유사한 구조화된 데이터 컨테이너입니다. 예를 들어, PL/SQL BOOLEAN이나 RECORD 타입을 표준 SQL이나 JDBC 인터페이스를 통해 직접 SELECT할 수 없습니다. 이전에는 개발자들이 덜 편리한 Oracle SQL OBJECT 타입을 우회책으로 사용해야 했습니다.

## 예제 PL/SQL 패키지

다음은 RECORD 타입을 사용하는 PL/SQL 패키지 예제입니다:

```sql
CREATE PACKAGE customers AS
  TYPE person IS RECORD (
    first_name VARCHAR2(50),
    last_name VARCHAR2(50)
  );

  FUNCTION get_customer(p_customer_id NUMBER) RETURN person;
END customers;
/
```

패키지 본문(body) 구현:

```sql
CREATE PACKAGE BODY customers AS
  FUNCTION get_customer(p_customer_id NUMBER) RETURN person IS
    v_person customers.person;
  BEGIN
    SELECT c.first_name, c.last_name
    INTO v_person
    FROM customer c
    WHERE c.customer_id = p_customer_id;

    RETURN v_person;
  END get_customer;
END customers;
/
```

PL/SQL에서는 다음과 같이 이 함수를 호출할 수 있습니다:

```sql
SET SERVEROUTPUT ON
DECLARE
  v_person customers.person;
BEGIN
  v_person := customers.get_customer(1);

  dbms_output.put_line(v_person.first_name);
  dbms_output.put_line(v_person.last_name);
END;
/
```

## 솔루션 구현

jOOQ의 코드 생성기는 PL/SQL RECORD 구조를 나타내는 상용구(boilerplate) Java 클래스를 자동으로 생성합니다. first_name과 last_name 필드를 가진 PERSON 레코드에 대해 jOOQ는 다음과 같이 생성합니다:

```java
package org.jooq.demo.sakila.packages.customers.records;

public class PersonRecord extends UDTRecordImpl<PersonRecord> {
    public void   setFirstName(String value) { ... }
    public String getFirstName()             { ... }
    public void   setLastName(String value)  { ... }
    public String getLastName()              { ... }

    public PersonRecord() { ... }
    public PersonRecord(String firstName, String lastName) { ... }
}
```

또한 패키지 클래스도 생성됩니다:

```java
public class Customers extends PackageImpl {
    public static PersonRecord getCustomer(
        Configuration configuration, Long pCustomerId
    ) { ... }
}
```

## Java에서의 사용법

생성된 코드를 사용하면 Java에서 매우 간단하게 호출할 수 있습니다:

```java
PersonRecord person = Customers.getCustomer(configuration(), 1L);
System.out.println(person);
// 출력: SAKILA.CUSTOMERS.PERSON('MARY', 'SMITH')
```

## 내부 동작 원리

JDBC의 제한된 저장 프로시저 이스케이프(escape) 구문에 의존하는 대신, jOOQ는 익명(anonymous) PL/SQL 블록을 생성합니다. 이 블록은 로컬 RECORD 변수를 인스턴스화하고, 대상 함수를 실행한 다음, 결과를 개별 바인드 파라미터로 분해(destructure)합니다. 이로써 Java 코드에서는 이 작업이 투명하게 처리됩니다.

jOOQ가 생성하는 익명 PL/SQL 블록의 예:

```sql
DECLARE
  r1 "CUSTOMERS"."PERSON";
BEGIN
  r1 := "CUSTOMERS"."GET_CUSTOMER"("P_CUSTOMER_ID" => ?);
  ? := r1."FIRST_NAME";
  ? := r1."LAST_NAME";
END;
```

함수 결과가 로컬 RECORD 변수에 할당된 후, 개별 바인드 파라미터로 분해됩니다.

## 코드 생성 접근 방식

코드 생성기는 Oracle의 딕셔너리 뷰(dictionary views), 특히 `ALL_ARGUMENTS`에 대한 정교한 다단계 SQL 쿼리를 사용하여 RECORD 구조, 필드 이름, 타입 및 계층적 관계를 식별합니다. 결과 쿼리는 윈도우 함수(window functions)와 집계 연산(aggregate operations)을 사용하여 중첩된 타입 정보를 재구성합니다.

## 지원 기능

이 접근 방식은 다음을 자동으로 처리합니다:

- 중첩 타입(Nested types)
- 다중 IN/OUT 파라미터
- 중첩된 RECORD 구조

## 결론

jOOQ 3.9의 Oracle PL/SQL RECORD 타입 지원을 통해, 이전에는 JDBC의 한계로 인해 접근하기 어려웠던 PL/SQL 기능을 Java 애플리케이션에서 손쉽게 활용할 수 있게 되었습니다. 코드 생성기가 모든 복잡한 작업을 처리해주므로, 개발자는 비즈니스 로직에만 집중할 수 있습니다.

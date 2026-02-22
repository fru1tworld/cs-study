# Oracle LONG과 LONG RAW가 "Stream has already been closed" 예외를 일으킴

> 원문: https://blog.jooq.org/oracle-long-and-long-raw-causing-stream-has-already-been-closed-exception/

Oracle에는 `LONG`과 `LONG RAW`라는 레거시 데이터 타입이 있습니다. 이들은 각각 `CLOB`과 `BLOB`과 어느 정도 동일하지만, 완전히 같지는 않습니다. 다음 스키마를 살펴보겠습니다:

```sql
CREATE TABLE t_long_raw_and_blob (
  id        NUMBER(7),
  blob1     BLOB,
  longx     LONG RAW,
  blob2     BLOB,
  CONSTRAINT pk_t_long_raw_and_blob PRIMARY KEY (id)
);

CREATE TABLE t_long_and_clob (
  id        NUMBER(7),
  clob1     CLOB,
  longx     LONG,
  clob2     CLOB,
  CONSTRAINT pk_t_long_and_clob PRIMARY KEY (id)
);
```

## 문제 상황

표준 JDBC를 사용하여 일반적인 컬럼 조회 순서로 데이터를 가져오려고 하면 문제가 발생합니다. 다음과 같은 코드를 작성했다고 가정해 보겠습니다:

```java
try (PreparedStatement s = con.prepareStatement(
        "SELECT * FROM t_long_raw_and_blob");
     ResultSet rs = s.executeQuery()) {
    while (rs.next()) {
        System.out.println("ID    = " + rs.getInt(1));
        System.out.println("BLOB1 = " + rs.getBytes(2));
        System.out.println("LONGX = " + rs.getBytes(3));
        System.out.println("BLOB2 = " + rs.getBytes(4));
    }
}
```

이 코드를 실행하면 `"Stream has already been closed"` 예외가 발생합니다. 이는 `LONG` 또는 `LONG RAW` 컬럼을 다른 컬럼들 다음에 접근하려고 할 때 발생하는 오류입니다.

## 근본 원인

이 문제는 Oracle JDBC 드라이버의 구현 방식에서 비롯됩니다. `LONG`과 `LONG RAW` 데이터 타입은 특별한 처리가 필요합니다. 이들은 ResultSet에서 다른 컬럼들보다 먼저 조회되어야 합니다. 이것은 JDBC API에 의도치 않게 노출된 저수준 Oracle 프로토콜의 결함으로 설명됩니다.

핵심은 다음과 같습니다: 모든 `LONG` 또는 `LONG RAW` 컬럼은 다른 모든 컬럼보다 먼저 `ResultSet`에서 조회되어야 합니다.

## 해결 방법

이 문제를 해결하려면 `LONG` 또는 `LONG RAW` 컬럼을 먼저 조회해야 합니다:

```java
try (PreparedStatement s = con.prepareStatement(
        "SELECT * FROM t_long_raw_and_blob");
     ResultSet rs = s.executeQuery()) {
    while (rs.next()) {
        // LONG RAW 컬럼을 먼저 조회
        byte[] longx = rs.getBytes(3);
        System.out.println("ID    = " + rs.getInt(1));
        System.out.println("BLOB1 = " + rs.getBytes(2));
        System.out.println("LONGX = " + longx);
        System.out.println("BLOB2 = " + rs.getBytes(4));
    }
}
```

## jOOQ의 해결책

jOOQ 라이브러리는 이 문제를 투명하게 처리합니다. 개발자는 원하는 순서대로 컬럼을 선택할 수 있으며, jOOQ가 내부적으로 컬럼 조회 순서를 재정렬합니다:

```java
DSL.using(configuration)
   .select(
       T_LONG_RAW_AND_BLOB.ID,
       T_LONG_RAW_AND_BLOB.BLOB1,
       T_LONG_RAW_AND_BLOB.LONGX,
       T_LONG_RAW_AND_BLOB.BLOB2
   )
   .from(T_LONG_RAW_AND_BLOB)
   .fetch();
```

jOOQ는 `ResultSet`에서 컬럼을 가져올 때 내부적으로 순서를 재정렬하여 이 예외가 발생하지 않도록 합니다. 이를 통해 개발자는 컬럼 순서를 신경 쓰지 않고 쿼리를 작성할 수 있습니다.

## 결론

Oracle의 `LONG`과 `LONG RAW`는 레거시 데이터 타입이지만, 여전히 일부 시스템에서 사용되고 있습니다. 이러한 타입을 다룰 때는 JDBC의 컬럼 조회 순서에 주의해야 합니다. jOOQ를 사용하면 이러한 저수준의 복잡성을 프레임워크가 자동으로 처리해 주므로, 개발자는 비즈니스 로직에 집중할 수 있습니다.

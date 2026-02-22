# Oracle DATE를 올바르게 바인딩하고 있는가? 아닐 것이다

> 원문: https://blog.jooq.org/are-you-binding-your-oracle-dates-correctly-i-bet-you-arent/

게시일: 2014년 12월 22일
작성자: Lukas Eder

---

## Oracle DATE 타입의 문제

Oracle의 `DATE` 타입은 표준 SQL DATE 구현과 근본적으로 다릅니다. 날짜 정보만 저장하는 대신, Oracle의 `DATE`는 실제로 `TIMESTAMP(0)`처럼 작동합니다 - 소수점 이하 초 정밀도가 0인 타임스탬프입니다. 이 차이는 JDBC를 통해 변수를 바인딩할 때 매우 중요합니다.

레거시 Oracle 데이터베이스들은 일반적으로 소수점 이하 초가 없는 완전한 타임스탬프를 저장하기 위해 `DATE`를 사용합니다. 예를 들면:
- 1970-01-01 00:00:00
- 2000-02-20 20:00:20
- 1337-01-01 13:37:00

## 바인드 변수 이슈

인덱스가 걸린 `DATE` 컬럼에 대한 범위 조건을 생각해 봅시다:

```java
PreparedStatement stmt = connection.prepareStatement(
    "SELECT * FROM my_table WHERE execute_at > ? AND execute_at < ?");
```

`java.sql.Date` 객체를 바인딩하면, 실행 계획은 `INDEX RANGE SCAN`으로 최적 상태를 유지합니다. 하지만 `java.sql.Timestamp` 객체를 바인딩하면 Oracle의 내부 타입 변환 메커니즘이 작동하여, 효율적인 범위 스캔이 `INTERNAL_FUNCTION()` 호출로 감싸진 전체 인덱스 스캔으로 대체됩니다.

`setTimestamp()`를 범위 조건에서 사용할 때:

```java
stmt.setTimestamp(1, start);
stmt.setTimestamp(2, end);
```

실행 계획은 최적의 `INDEX RANGE SCAN`에서 `INDEX FULL SCAN`으로 저하됩니다. 실행 계획은 다음과 같이 보입니다:

```
Predicate Information:
1 - filter(:1:1 AND INTERNAL_FUNCTION("EXECUTE_AT")<:2)
```

반면 `setDate()`를 사용한 올바른 계획은:

```
Predicate Information:
1 - filter(:1:1 AND "EXECUTE_AT"<:2)
```

## INTERNAL_FUNCTION()이 나타나는 이유

Oracle은 잠재적인 정밀도 불일치를 처리하기 위해 이 암시적 변환을 적용합니다. `Timestamp` 타입의 소수점 이하 초는 범위 조건에서 경계 조건을 변경시킬 수 있습니다. 잘못된 결과의 위험을 감수하는 대신, Oracle은 정밀도가 낮은 `DATE` 컬럼을 `Timestamp` 바인드 변수의 정밀도에 맞추기 위해 확장합니다. 이 접근 방식은 인덱스 사용 최적화를 방해합니다.

이 변환에 대한 함수 기반 인덱스를 생성하려는 시도는 실패합니다:

```sql
CREATE INDEX oracle_why_oh_why
  ON my_table(INTERNAL_FUNCTION(execute_at));
```

이 구문은 지원되지 않아서, 개발자들은 암시적 변환을 우회하여 최적화할 방법이 없습니다.

## 해결책

해답은 SQL 수준에서 명시적 캐스팅을 필요로 합니다:

```java
PreparedStatement stmt = connection.prepareStatement(
    "SELECT * FROM my_table " +
    "WHERE execute_at > CAST(? AS DATE) " +
    "AND execute_at < CAST(? AS DATE)");
```

이 접근 방식은 조건절에서 `java.sql.Timestamp` 값을 Oracle `DATE` 컬럼에 바인딩할 때마다 일관되게 적용되어야 합니다. 기능적으로는 작동하지만, 이 해결책은 전체 코드베이스에 걸쳐 규율을 요구합니다.

## 프레임워크별 접근 방식

JDBC: 직접 구현은 영향을 받는 모든 쿼리에서 수동 캐스팅을 필요로 합니다.

JPA/Hibernate: 생성된 SQL에 대한 제어가 제한적이어서 이러한 패턴을 수정하기 어렵습니다.

jOOQ 3.5+: 커스텀 타입 바인딩 기능이 관련 컬럼에 대해 이러한 변환을 자동으로 투명하게 처리하여, 수동 개입 없이 `CAST(? AS DATE)` 표현식을 렌더링합니다.

## Oracle을 넘어서

Oracle의 바인드 변수 타입 추론에 대한 유연성은 실제로 예외적입니다. 다른 데이터베이스들은 더욱 까다로운 캐스팅 요구사항을 제시하며, 이는 RDBMS 바인드 변수 캐스팅 문제에 대한 보완 문서에서 자세히 설명되어 있습니다.

---

## 핵심 요약

1. Oracle의 `DATE`는 실제로 `TIMESTAMP(0)`처럼 작동합니다
2. `java.sql.Timestamp`를 `DATE` 컬럼에 바인딩하면 `INTERNAL_FUNCTION()`이 적용되어 인덱스를 사용할 수 없게 됩니다
3. 해결책은 `CAST(? AS DATE)`를 사용하여 명시적으로 캐스팅하는 것입니다
4. jOOQ 3.5+는 커스텀 타입 바인딩으로 이 문제를 자동으로 처리합니다

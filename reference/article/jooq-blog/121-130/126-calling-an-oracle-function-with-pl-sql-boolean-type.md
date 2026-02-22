# SQL에서 PL/SQL BOOLEAN 타입의 Oracle 함수 호출하기

> 원문: https://blog.jooq.org/calling-an-oracle-function-with-pl-sql-boolean-type-from-sql/

Oracle 데이터베이스에서 가장 많이 요청받는 기능 중 하나는 BOOLEAN 타입 지원입니다. SQL 표준에서는 이미 이를 명시하고 있으며, PostgreSQL과 같은 다른 RDBMS 플랫폼에서는 그 기능을 보여주고 있습니다. 특히 EVERY()와 같은 집계 함수에서 그렇습니다.

참고: Oracle 23c부터 드디어 표준 BOOLEAN 타입이 지원됩니다!

## 초기 함수 정의

PL/SQL 언어는 boolean 타입을 지원합니다. 다음과 같이 작성할 수 있습니다:

```sql
CREATE OR REPLACE FUNCTION number_to_boolean (i NUMBER)
RETURN BOOLEAN
IS
BEGIN
  RETURN NOT i = 0;
END number_to_boolean;
/

CREATE OR REPLACE FUNCTION boolean_to_number (b BOOLEAN)
RETURN NUMBER
IS
BEGIN
  RETURN CASE WHEN b THEN 1 WHEN NOT b THEN 0 END;
END boolean_to_number;
/
```

## PL/SQL 사용 예제

PL/SQL에서는 이러한 함수들을 간단하게 호출할 수 있습니다:

```sql
SET SERVEROUTPUT ON
BEGIN
  IF number_to_boolean(1) THEN
    dbms_output.put_line('1 is true');
  END IF;
  IF NOT number_to_boolean(0) THEN
    dbms_output.put_line('0 is false');
  END IF;
  IF number_to_boolean(NULL) IS NULL THEN
    dbms_output.put_line('null is null');
  END IF;
END;
/
```

이 코드는 다음을 출력합니다:

```
1 is true
0 is false
null is null
```

## SQL 엔진의 한계

SQL에서 동일한 작업을 시도하면 실패합니다:

```sql
SELECT
  number_to_boolean(1),
  number_to_boolean(0),
  number_to_boolean(null)
FROM dual;
```

결과: `ORA-00902: invalid datatype`

SQL 엔진은 PL/SQL BOOLEAN 타입을 직접 처리할 수 없습니다. Oracle은 결국 이 제한 사항을 해결할 계획입니다.

## WITH 절을 사용한 해결 방법

Oracle 12c에서는 WITH 절 내에서 함수 선언이 도입되었습니다:

```sql
WITH
  FUNCTION f RETURN NUMBER IS
  BEGIN
    RETURN 1;
  END f;
SELECT f
FROM dual;
```

반환값: `1`

WITH 절은 PL/SQL을 사용하므로 여기서 BOOLEAN 타입을 활용할 수 있습니다. 실패하는 쿼리 대신 다음과 같이 작성합니다:

```sql
WITH
  FUNCTION number_to_boolean_(i NUMBER)
  RETURN NUMBER
  IS
    b BOOLEAN;
  BEGIN
    -- 실제 함수 호출
    b := number_to_boolean(i);

    -- 숫자 결과로 변환
    RETURN CASE b WHEN TRUE THEN 1 WHEN FALSE THEN 0 END;
  END number_to_boolean_;
SELECT
  number_to_boolean_(1) AS a,
  number_to_boolean_(0) AS b,
  number_to_boolean_(null) AS c
FROM dual;
```

결과:

```
 A   B   C
 1   0  null
```

결과가 진정한 boolean 타입은 아니지만, JDBC는 1/0/null을 true/false/null 값으로 투명하게 매핑할 수 있습니다.

## 체이닝 예제

다음과 같은 쿼리는 ORA-00902 오류로 실패합니다:

```sql
SELECT
  boolean_to_number(number_to_boolean(1)),
  boolean_to_number(number_to_boolean(0)),
  boolean_to_number(number_to_boolean(null))
FROM dual;
```

대신 다음과 같이 사용합니다:

```sql
WITH
  FUNCTION number_to_boolean_(i NUMBER)
  RETURN NUMBER
  IS
    b BOOLEAN;
  BEGIN
    -- 실제 함수 호출
    b := number_to_boolean(i);

    -- 숫자 결과로 변환
    RETURN CASE b WHEN TRUE THEN 1 WHEN FALSE THEN 0 END;
  END number_to_boolean_;

  FUNCTION boolean_to_number_(b NUMBER)
  RETURN NUMBER
  IS
  BEGIN
    -- 실제 함수 호출
    RETURN boolean_to_number(NOT b = 0);
  END boolean_to_number_;
SELECT
  boolean_to_number_(number_to_boolean_(1)) AS a,
  boolean_to_number_(number_to_boolean_(0)) AS b,
  boolean_to_number_(number_to_boolean_(null)) AS c
FROM dual;
```

결과:

```
 A   B   C
 1   0  null
```

이 기법은 %ROWTYPE 파라미터를 포함하여 BOOLEAN 타입을 받거나 반환하는 모든 PL/SQL 함수에 대해 자동화될 수 있습니다.

## jOOQ 3.12 지원

jOOQ 3.12부터 SQL에서 이러한 함수들에 대한 네이티브 지원이 추가되었습니다. 다음과 같은 함수가 있다면:

```sql
FUNCTION f_bool (i BOOLEAN) RETURN BOOLEAN;
```

이제 jOOQ 문장 내 어디에서든 호출할 수 있습니다:

```java
Record1<Integer> r =
create()
    .select(one())
    .where(PlsObjects.fBool(false))
    .fetchOne();

assertNull(r);
```

생성되는 SQL은 다음과 같습니다:

```sql
with
  function "F_BOOL_"(I integer)
  return integer
  is
    "r" boolean;
  begin
    "r" := "TEST"."PLS_OBJECTS"."F_BOOL"(not I = 0);
    return case when "r" then 1 when not "r" then 0 end;
  end "F_BOOL_";
  select 1
from dual
where (F_BOOL_(0) = 1)
```

이제 boolean 표현식이 진정한 boolean 술어로 코딩됩니다.

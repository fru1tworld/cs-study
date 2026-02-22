# jOOQ로 벤더 독립적인 동적 절차적 로직 작성하기

> 원문: https://blog.jooq.org/vendor-agnostic-dynamic-procedural-logic-with-jooq/

## 소개

현대의 관계형 데이터베이스 관리 시스템은 SQL과 절차적 코드를 결합하는 데 탁월합니다. SQL은 쿼리와 대량 데이터 조작에 최적화된 "4세대 프로그래밍 언어(4GL)"로 기능하는 반면, 명령형 3GL 언어는 저장 프로시저와 같은 절차적 컨텍스트에서 빛을 발합니다.

## 지원 데이터베이스

jOOQ는 BigQuery, Db2, Exasol, Firebird, HANA, HSQLDB, Informix, MariaDB, MySQL, Oracle, PostgreSQL, SQL Server, Vertica를 포함한 다양한 데이터베이스에서 절차적 로직을 지원합니다. 많은 데이터베이스가 "ISO/IEC 9075-4 영속 저장 모듈(SQL/PSM)" 표준을 따르는 절차적 언어를 구현하고 있으며, 다른 데이터베이스들은 독자적인 접근 방식을 유지하고 있습니다.

## jOOQ의 절차적 지원

버전 3.12부터 jOOQ의 상용 배포판은 익명 블록과 IF, LOOP 구문과 같은 절차적 문장을 지원해 왔습니다. 버전 3.15에서는 CREATE PROCEDURE, CREATE FUNCTION, CREATE TRIGGER 문을 포함하도록 기능이 확장되었습니다.

주요 사용 사례는 다음과 같습니다:
- 벤더 독립적인 절차적 로직이 필요한 제품 벤더
- SQL 로직 패턴과 일치하는 동적 절차적 요구사항
- 프로시저를 직접 생성할 수 없는 스키마 제한 상황

## 익명 블록 예제

빈 익명 블록은 데이터베이스 방언에 따라 다릅니다:

Db2:
```sql
BEGIN
END
```

PostgreSQL:
```sql
DO $$
BEGIN
  NULL;
END;
$$
```

다음과 같이 실행합니다: `ctx.begin().execute();`

## 실용적인 LOOP 예제

1부터 10까지의 값을 삽입하는 WHILE 루프:

```java
Variable<Integer> i = variable(unquotedName("i"), INTEGER);
Table<?> t = table(unquotedName("t"));
Field<Integer> col = field(unquotedName("col"), INTEGER);

ctx.begin(
    declare(i).set(1),
    while_(i.le(10)).loop(
        insertInto(t).columns(col).values(i),
        i.set(i.plus(1))
    )
).execute();
```

Oracle 출력:
```sql
DECLARE
  i number(10);
BEGIN
  i := 1;
  WHILE i <= 10 LOOP
    INSERT INTO t (c)
    VALUES (i);
    i := (i + 1);
  END LOOP;
END;
```

## FOR 루프 대안

jOOQ는 네이티브 지원이 없는 방언에서도 FOR 루프를 에뮬레이션합니다:

```java
ctx.begin(
    for_(i).in(1, 10).loop(
        insertInto(t).columns(col).values(i)
    )
).execute();
```

## SQL vs. 절차적 로직

이 글에서는 순수 SQL이 종종 절차적 접근 방식을 능가한다고 언급합니다:

```java
ctx.insertInto(t, c)
   .select(selectFrom(generateSeries(1, 10)))
   .execute();
```

이 코드는 네이티브 시리즈 생성 함수를 활용하여 데이터베이스 간에 지능적으로 변환됩니다.

## 절차적 로직 저장하기

영속적인 로직을 위해 프로시저를 생성할 수 있습니다:

```java
Name p = unquotedName("p");

ctx.createProcedure(p)
   .modifiesSQLData()
   .as(
        declare(i).set(1),
        while_(i.le(10)).loop(
            insertInto(t).columns(col).values(i),
            i.set(i.plus(1))
        )
   )
   .execute();
```

프로시저는 다음과 같이 호출할 수 있습니다:
```java
ctx.begin(call(unquotedName("p"))).execute();
```

## 변환 기능

jOOQ는 https://www.jooq.org/translate 에서 PL/SQL, T-SQL, PL/pgSQL과 같은 절차적 방언 간의 변환을 가능하게 하는 파서/번역기를 제공합니다.

## 결론

이 글에서는 가능한 경우 SQL을 활용할 것을 권장하지만, 3GL 로직이 더 우수한 시나리오가 있음을 인정합니다. jOOQ는 SQL 기능을 미러링하면서 서버 측 실행을 통해 성능을 향상시키는 "동적이고, 벤더 독립적이며, 익명이거나 저장된" 절차적 코드를 생성할 수 있게 해줍니다.

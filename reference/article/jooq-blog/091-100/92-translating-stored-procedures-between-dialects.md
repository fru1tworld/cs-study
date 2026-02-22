# 방언 간 저장 프로시저 변환하기
> 원문: https://blog.jooq.org/translating-stored-procedures-between-dialects/

jOOQ는 단순한 DDL 에뮬레이션 지원에서 시작하여 포괄적인 절차적 언어 기능을 제공하는 수준까지 발전했습니다. 이 글에서는 jOOQ가 절차적 SQL 코드를 다양한 데이터베이스 방언 간에 어떻게 변환하는지 살펴보겠습니다.

## 익명 블록

익명 블록은 "이름이 없는 절차적 로직"으로, Java의 익명 클래스와 비교할 수 있습니다. 익명 블록은 다음과 같은 실용적인 목적을 위해 사용됩니다:

- 예외 처리가 포함된 원자적 임시 코드 단위 실행
- 동적 절차적 코드 생성 가능
- 저장 프로시저 배포를 방해하는 조직적 제약 우회
- 코드가 자주 변경되는 시나리오 지원

jOOQ는 변수 선언, 조건문(IF), 다양한 루프 유형(LOOP, WHILE, REPEAT, FOR), 흐름 제어 명령(EXIT, CONTINUE, RETURN, GOTO), 예외 처리(SIGNAL, RAISE) 등 포괄적인 절차적 구문을 지원합니다.

## 실제 예제

다음 Java 기반 jOOQ 정의가 여러 방언에서 어떻게 변환되는지 보여주는 코드 예제입니다:

```java
Variable<Integer> i = var(name("i"), INTEGER);
ctx.begin(
  for_(i).in(1, 10).loop(
    insertInto(T).columns(T.COL).values(i)
  )
).execute();
```

Db2/MySQL 출력:
```sql
begin
  declare I bigint;
  set I = 1;
  while I <= 10 do
    insert into T (COL) values (I);
    set I = (I + 1);
  end while;
end;
```

PostgreSQL 출력:
```sql
do $$
begin
  for I in 1 .. 10 loop
    insert into T (COL) values (I);
  end loop;
end;
$$
```

Oracle 출력:
```sql
begin
  for I in 1 .. 10 loop
    insert into T (COL) values (I);
  end loop;
end;
```

이처럼 동일한 jOOQ 코드가 Db2, MySQL, PostgreSQL, Oracle, SQL Server에 대해 각 데이터베이스의 특정 구문 요구사항에 맞는 적절한 문법을 생성합니다.

## 저장 프로시저로 코드 저장하기

jOOQ 3.15부터 개발자는 `CREATE PROCEDURE`와 `CREATE FUNCTION` 문을 사용하여 절차적 로직을 데이터베이스에 저장할 수 있습니다. `CREATE PACKAGE` 지원은 "희망 목록에서 높은 순위"에 있지만 해당 릴리스에서 지원될지는 불확실합니다.

매개변수가 있는 프로시저 예제는 Db2, MySQL, MariaDB, Oracle, PostgreSQL, SQL Server에 대해 변수 선언과 루프 구성에 적절한 문법을 사용하여 방언별 변형을 생성합니다.

```java
Variable<Integer> i = var("i", INTEGER);
Parameter<Integer> i1 = in("i1", INTEGER);
Parameter<Integer> i2 = in("i2", INTEGER);

ctx.createProcedure("insert_into_t")
   .parameters(i1, i2)
   .as(for_(i).in(i1, i2).loop(
     insertInto(T).columns(T.COL).values(i)
   ))
   .execute();
```

## 까다로운 변환

이 글에서는 방언 간의 미묘하지만 중요한 차이점을 다룹니다:

- 변수 선언 범위 차이
- LEAVE/EXIT 문에 대한 레이블 요구사항
- SQL과의 서로 다른 통합 패턴

예를 들어, "Db2는 루프 종료문에 레이블이 필요하지만, Oracle은 레이블 없는 EXIT를 지원합니다." 중첩 루프와 종료문이 포함된 예제는 일부 방언(Db2)에서는 레이블 생성이 필요하지만 다른 방언(Oracle)에서는 필요하지 않음을 보여주며, 복잡한 변환 로직을 보여줍니다.

```java
Name t = unquotedName("t");
Name a = unquotedName("a");
Variable<Integer> i = var(unquotedName("i"), INTEGER);

ctx.begin(
     insertInto(t).columns(a).values(1),
     declare(i).set(2),
     loop(
       insertInto(t).columns(a).values(i),
       i.set(i.plus(1)),
       if_(i.gt(10)).then(loop(exit()), exit())
     )
   )
   .execute();
```

## 실용적인 활용

이 기능이 가치 있는 실제 시나리오들이 있습니다:

- 데이터베이스 마이그레이션: 한 데이터베이스 시스템에서 다른 시스템으로 절차적 코드를 이동할 때
- 다중 방언 지원: 여러 데이터베이스 백엔드를 지원해야 하는 애플리케이션에서
- 동적 코드 생성: 런타임에 절차적 로직을 생성해야 할 때
- 조직적 장벽 우회: 저장 프로시저 배포에 제약이 있는 환경에서

## 주요 리소스

jOOQ는 https://www.jooq.org/translate/ 에서 변환 도구를 제공하며, 절차적 문에 대한 문서는 매뉴얼에서 참조할 수 있습니다.

## 기술적 프레임워크

jOOQ는 다음을 지원합니다: 변수 선언, IF 문, 루프(LOOP, WHILE, REPEAT, FOR), 흐름 제어(EXIT/LEAVE, CONTINUE/ITERATE), RETURN, GOTO, SIGNAL/RAISE, 레이블, CALL 문, 동적 SQL 실행을 위한 EXECUTE 문.

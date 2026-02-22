# jOOQ와 함께 Java 13+ 텍스트 블록을 평문 SQL에 사용하기

> 원문: https://blog.jooq.org/using-java-13-text-blocks-for-plain-sql-with-jooq/

저자: lukaseder
게시일: 2020년 3월 5일

## 소개

대부분의 jOOQ 사용자들은 컴파일 타임 타입 안전성과 동적 SQL을 위해 jOOQ DSL API를 활용합니다. 그러나 DSL은 때때로 단순한 쿼리에는 과한 경우가 있고, Oracle의 `MODEL`이나 `MATCH_RECOGNIZE` 절과 같은 고급 벤더 전용 SQL에는 너무 제한적일 수 있습니다.

jOOQ는 Stream API 통합, 내보내기 기능 등을 포함한 부가 기능들을 제공합니다—본질적으로 개선된 JDBC로 기능합니다. Java 13부터 (프리뷰 기능 활성화 시), 텍스트 블록은 Java 코드에 정적 SQL을 내장하기에 이상적인 여러 줄 문자열을 가능하게 합니다.

## 사용 사례 1: 평문 SQL

### 기본 예제

```java
System.out.println(ctx.fetch("""
        SELECT table_schema, count(*)
        FROM information_schema.tables
        GROUP BY table_schema
        ORDER BY table_schema
        """));
```

출력:
```
+------------------+--------+
|TABLE_SCHEMA      |COUNT(*)|
+------------------+--------+
|INFORMATION_SCHEMA|      33|
|MCVE              |       2|
|PUBLIC            |       1|
+------------------+--------+
```

### 동적 절을 사용한 평문 SQL 템플릿

정적 import 사용: `import static org.jooq.impl.DSL.*;`

```java
Stream.of(
        field("table_schema"),
        list(field("table_schema"), field("table_type")))
    .forEach(q -> {
        System.out.println(ctx.fetch("""
          SELECT {0}, count(*), row_number() OVER (ORDER BY {0}) AS rn
          FROM information_schema.tables
          GROUP BY {0}
          ORDER BY {0}
          """, q));
    });
```

첫 번째 반복의 출력:
```
+------------------+--------+----+
|TABLE_SCHEMA      |COUNT(*)|  RN|
+------------------+--------+----+
|INFORMATION_SCHEMA|      33|   1|
|MCVE              |       2|   2|
|PUBLIC            |       1|   3|
+------------------+--------+----+
```

두 번째 반복의 출력:
```
+------------------+------------+--------+----+
|TABLE_SCHEMA      |TABLE_TYPE  |COUNT(*)|  RN|
+------------------+------------+--------+----+
|INFORMATION_SCHEMA|SYSTEM TABLE|      33|   1|
|MCVE              |TABLE       |       1|   2|
|MCVE              |VIEW        |       1|   3|
|PUBLIC            |TABLE       |       1|   4|
+------------------+------------+--------+----+
```

이 접근 방식은 전체 jOOQ 도구 체인에 대한 접근을 유지하면서 동적 SQL 구성을 가능하게 합니다—역사적으로 MyBatis나 자체 제작한 Velocity 템플릿 프레임워크의 사용 사례였습니다.

## 사용 사례 2: jOOQ 파서

### SQL 포맷팅 및 정규화

Oracle과 같은 데이터베이스는 공백 차이를 별도의 SQL 문자열로 처리하여 실행 계획 캐시 경합을 유발합니다. 파서는 SQL을 정규화합니다:

```java
System.out.println(
    ctx.parser()
       .parseResultQuery("""
            SELECT table_schema, count(*)
            FROM information_schema.tables
            GROUP BY table_schema
            -- 컬럼 인덱스로 정렬!
            ORDER BY 1
            """)
       .fetch()
);
```

JDBC로 전송되는 결과 정규화된 SQL:
```sql
select table_schema, count(*) from information_schema.tables group by table_schema order by 1
```

`new Settings().withRenderFormatted(true)`로 포맷팅 활성화 시:
```sql
select
  table_schema,
  count(*)
from information_schema.tables
group by table_schema
order by 1
```

### SQL 방언 변환

파서는 데이터베이스 간 호환성을 가능하게 합니다. H2를 위한 SQL Server 구문:

```java
System.out.println(
    ctx.parser()
       .parseResultQuery("""
            SELECT TOP 1 table_schema, count(*)
            FROM information_schema.tables
            GROUP BY table_schema
            ORDER BY count(*) DESC
            """)
       .fetch()
);
```

H2로 변환됨:
```sql
select
  table_schema,
  count(*)
from information_schema.tables
group by table_schema
order by count(*) desc
limit 1
```

### 고급 변환: FETCH NEXT WITH TIES

```java
System.out.println(
    ctx.parser()
       .parseResultQuery("""
            SELECT TOP 1 WITH TIES table_schema, count(*)
            FROM information_schema.tables
            GROUP BY table_schema
            ORDER BY count(*) DESC
            """)
       .fetch()
);
```

H2에서:
```sql
select
  TABLE_SCHEMA,
  count(*)
from INFORMATION_SCHEMA.TABLES
group by TABLE_SCHEMA
order by 2 desc
fetch next 1 rows with ties
```

PostgreSQL에서:
```sql
select
  "v0" as table_schema,
  "v1" as "count"
from (
  select
    table_schema as "v0",
    count(*) as "v1",
    rank() over (order by 2 desc) as "rn"
  from information_schema.tables
  group by table_schema
) "x"
where "rn" > 0
and "rn" <= (0 + 1)
order by "rn"
```

## 사용 사례 3: 파서 파생 기능

### 스키마 관리

```java
System.out.println(
    ctx.meta("""
    create table t (
      i int
    )
    """).apply("""
    alter table t
      add j int;
    alter table t
      add constraint t_pk primary key (i)
    """)
);
```

출력:
```sql
create table T(
  I int null,
  J int null,
  constraint T_PK
    primary key (I)
);
```

### 스키마 차이 계산

```java
System.out.println(
    ctx.meta("""
    create table t (
      i int
    )
    """).migrateTo(ctx.meta("""
    create table t (
      i int,
      j int,
      constraint t_pk primary key (i)
    )
    """))
);
```

출력 (적용된 증분):
```sql
alter table T
  add J int null;
alter table T
  add constraint T_PK
    primary key (I);
```

## 결론

jOOQ의 DSL은 타입 안전성, 컴파일 타임 검증, 그리고 훌륭한 자동 완성을 제공합니다. 그러나 DSL이 제한적이 될 때, 텍스트 블록을 사용한 평문 SQL은 jOOQ의 완전한 도구 체인—포맷팅, 파싱, 템플릿, 내보내기 기능—에 대한 접근을 유지하면서 강력한 대안을 제공합니다. 선택은 이분법적이지 않습니다; 사용자는 동일한 애플리케이션 내에서 두 가지 접근 방식을 모두 활용할 수 있습니다.

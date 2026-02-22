# jOOQ-meta. "하드코어 SQL" 개념 증명

> 원문: https://blog.jooq.org/jooq-meta-a-hard-core-sql-proof-of-concept/

jOOQ-meta는 단순히 데이터베이스 스키마의 메타데이터를 탐색하는 것 이상입니다. 이것은 더 복잡한 jOOQ 쿼리에 대한 개념 증명이기도 합니다. 여러분 사용자들이 다음과 같은 단순한 바닐라 쿼리가 동작할 것이라고 믿는 것은 쉬운 일입니다: `create.selectFrom(AUTHOR).where(LAST_NAME.equal("Cohen"));`

하지만 jOOQ는 "하드코어 SQL 라이브러리"임을 표방합니다.

그러면 jOOQ-meta의 하드코어 SQL 중 일부를 살펴보겠습니다. 다음은 Postgres 저장 함수를 jOOQ의 공통 루틴(routines) 개념에 매핑하는 멋진 Postgres 쿼리입니다. 이 예제 쿼리가 모델링하는 Postgres의 매우 흥미로운 두 가지 특성이 있습니다:

Postgres는 함수만 알고 있습니다. 함수에 OUT 파라미터가 하나 있으면, 해당 파라미터를 함수의 반환 값으로 처리할 수 있습니다. 함수에 OUT 파라미터가 두 개 이상 있으면, 해당 파라미터들을 함수 반환 커서로 처리할 수 있습니다. 다시 말해, 모든 함수는 테이블입니다. 정말 흥미로운 특성입니다. 하지만 현재로서는 jOOQ가 이를 지원하지 않으므로, 여러 개의 OUT 파라미터는 void 결과로 처리해야 합니다.

Postgres는 독립 실행형(standalone) 함수의 오버로딩을 허용합니다(Oracle 같은 경우에는 허용되지 않습니다). 따라서 SQL 문에서 직접 모든 함수에 대한 오버로드 인덱스를 생성하기 위해, CASE 표현식 내에서 `SELECT COUNT(*)` 서브셀렉트를 실행하며 기본값은 null입니다.

```java
Routines r1 = ROUTINES.as("r1");
Routines r2 = ROUTINES.as("r2");

for (Record record : create().select(
        r1.ROUTINE_NAME,
        r1.SPECIFIC_NAME,

        // OUT 파라미터가 존재하면 "void"로 디코딩
        decode()
            .when(exists(create()
                .selectOne()
                .from(PARAMETERS)
                .where(PARAMETERS.SPECIFIC_SCHEMA.equal(r1.SPECIFIC_SCHEMA))
                .and(PARAMETERS.SPECIFIC_NAME.equal(r1.SPECIFIC_NAME))
                .and(upper(PARAMETERS.PARAMETER_MODE).notEqual("IN"))),
                    val("void"))
            .otherwise(r1.DATA_TYPE).as("data_type"),
        r1.NUMERIC_PRECISION,
        r1.NUMERIC_SCALE,
        r1.TYPE_UDT_NAME,

        // 오버로드 인덱스 계산
        decode().when(
        exists(
            create().selectOne()
                .from(r2)
                .where(r2.ROUTINE_SCHEMA.equal(getSchemaName()))
                .and(r2.ROUTINE_NAME.equal(r1.ROUTINE_NAME))
                .and(r2.SPECIFIC_NAME.notEqual(r1.SPECIFIC_NAME))),
            create().select(count())
                .from(r2)
                .where(r2.ROUTINE_SCHEMA.equal(getSchemaName()))
                .and(r2.ROUTINE_NAME.equal(r1.ROUTINE_NAME))
                .and(r2.SPECIFIC_NAME.lessOrEqual(r1.SPECIFIC_NAME)).asField())
        .as("overload"))
    .from(r1)
    .where(r1.ROUTINE_SCHEMA.equal(getSchemaName()))
    .orderBy(r1.ROUTINE_NAME.asc())
    .fetch()) {
```

SQL을 살펴보겠습니다:

```sql
select
  r1.routine_name,
  r1.specific_name,
  case when exists (
            select 1 from information_schema.parameters
            where (information_schema.parameters.specific_schema
              = r1.specific_schema
            and information_schema.parameters.specific_name
              = r1.specific_name
            and upper(information_schema.parameters.parameter_mode)
              <> 'IN'))
       then 'void'
       else r1.data_type
       end as data_type,
  r1.numeric_precision,
  r1.numeric_scale,
  r1.type_udt_name,
  case when exists (
            select 1 from information_schema.routines as r2
            where (r2.routine_schema = 'public'
            and r2.routine_name = r1.routine_name
            and r2.specific_name <> r1.specific_name))
       then (select count(*)
             from information_schema.routines as r2
             where (r2.routine_schema = 'public'
             and r2.routine_name = r1.routine_name
             and r2.specific_name <= r1.specific_name))
       end as overload
from information_schema.routines as r1
where r1.routine_schema = 'public'
order by r1.routine_name asc
```

위의 SQL 문은 Postgres용 소스 코드를 생성할 때 실행되며 완벽하게 동작합니다. jOOQ 2.0에서는 DSL이 더 간결하고 더 강력해질 것입니다. JDBC를 직접 사용할 때보다 훨씬 적은 SQL을 작성할 수는 없을 것입니다. 그리고 JPA, JPQL, HQL 등과 같은 다른 제품들로는 이런 것을 즉시 잊어버리셔도 됩니다 :-)

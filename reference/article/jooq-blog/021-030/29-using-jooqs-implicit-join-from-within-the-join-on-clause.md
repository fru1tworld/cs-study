# JOIN .. ON 절 내에서 jOOQ의 암묵적 조인 사용하기

> 원문: https://blog.jooq.org/using-jooqs-implicit-join-from-within-the-join-on-clause/

jOOQ 3.11부터 타입 안전한 암묵적 `JOIN`이 사용 가능해졌으며, jOOQ 3.17에서는 DML 문에서도 지원되도록 개선되었습니다. 오늘은 명시적 `JOIN`의 `ON` 절 내에서 추가 테이블을 조인할 때 사용하는, 다소 특이하지만 매우 강력한 암묵적 `JOIN`의 활용 사례에 초점을 맞추겠습니다.

## 활용 사례

jOOQ 코드 생성기는 다양한 딕셔너리 뷰를 조회할 때 jOOQ를 매우 많이 사용합니다. PostgreSQL에서 대부분의 쿼리는 SQL 표준인 `information_schema`로 전달되지만, 가끔 표준 메타데이터가 충분하지 않아 더 완전하지만 훨씬 더 기술적인 `pg_catalog`도 함께 조회해야 하는 경우가 있습니다.

많은 `information_schema` 뷰에 대해 동일한 정보를 포함하는 거의 동등한 `pg_catalog` 테이블이 존재합니다. 예를 들어:

| information_schema | pg_catalog |
|---|---|
| schemata | pg_namespace |
| tables 또는 user_defined_types | pg_class |
| columns 또는 attributes | pg_attribute |

흥미롭게도, PostgreSQL은 ORDBMS이기 때문에 테이블과 사용자 정의 타입은 동일한 것이며 타입 시스템에서 종종 상호 교환이 가능합니다. 하지만 이것은 향후 블로그 포스트에서 다룰 주제입니다.

이 블로그 포스트의 핵심은 `information_schema.attributes`와 같은 뷰를 조회할 때 추가 데이터를 얻기 위해 `pg_catalog.pg_attribute`도 함께 조회해야 하는 경우가 많다는 것입니다. 예를 들어, UDT(사용자 정의 타입) 속성의 선언된 배열 차원을 찾으려면 `pg_catalog.pg_attribute.attndims`에 접근해야 하는데, 이 정보는 `information_schema` 어디에서도 찾을 수 없습니다. H2 / PostgreSQL 다차원 배열 지원을 추가할 jOOQ 기능 요청 #252도 참고하세요.

따라서 다음과 같은 UDT가 있을 수 있습니다:

```sql
CREATE TYPE u_multidim_a AS (
  i integer[][],
  n numeric(10, 5)[][][],
  v varchar(10)[][][][]
);
```

`attributes` 뷰에서 `pg_attribute` 테이블에 접근하는 정석적인 SQL 방법은 다음과 같습니다:

```sql
SELECT
  is_a.udt_schema,
  is_a.udt_name,
  is_a.attribute_name,
  pg_a.attndims
FROM information_schema.attributes AS is_a
  JOIN pg_attribute AS pg_a
    ON is_a.attribute_name = pg_a.attname
  JOIN pg_class AS pg_c
    ON is_a.udt_name = pg_c.relname
    AND pg_a.attrelid = pg_c.oid
  JOIN pg_namespace AS pg_n
    ON is_a.udt_schema = pg_n.nspname
    AND pg_c.relnamespace = pg_n.oid
WHERE is_a.data_type = 'ARRAY'
ORDER BY
  is_a.udt_schema,
  is_a.udt_name,
  is_a.attribute_name,
  is_a.ordinal_position
```

시각적으로 표현하면:

```
                +----- udt_schema = nspname ------> pg_namespace
                |                                      ^
                |                                      |
                |                                     oid
                |                                      =
                |                                     relnamespace
                |                                      |
                |                                      v
                +------- udt_name = relname ------> pg_class
                |                                      ^
                |                                      |
                |                                     oid
                |                                      =
                |                                     attrelid
                |                                      |
                |                                      v
is.attributes <-+- attribute_name = attname ------> pg_attribute
```

이제 다차원 배열을 포함하는 몇 가지 통합 테스트 사용자 정의 타입을 확인할 수 있습니다:

| udt_schema | udt_name | attribute_name | attndims |
|---|---|---|---|
| public | u_multidim_a | i | 2 |
| public | u_multidim_a | n | 3 |
| public | u_multidim_a | v | 4 |
| public | u_multidim_b | a1 | 1 |
| public | u_multidim_b | a2 | 2 |
| public | u_multidim_b | a3 | 3 |
| public | u_multidim_c | b | 2 |

하지만 저 `JOIN` 표현식들을 보세요. 분명 즐겁지 않습니다. 다른 UDT나 다른 스키마에서 모호하게 이름이 지정된 데이터를 가져오지 않도록 하기 위해, `pg_attribute`에서 `pg_namespace`까지의 전체 경로를 일일이 작성해야 합니다.

## 대신 암묵적 조인 사용하기

바로 여기서 암묵적 `JOIN`의 힘이 발휘됩니다. SQL에서 우리가 정말로 _작성하고 싶은_ 것은 다음과 같습니다:

```sql
SELECT
  is_a.udt_schema,
  is_a.udt_name,
  is_a.attribute_name,
  pg_a.attndims

-- 이 테이블이 필요합니다
FROM information_schema.attributes AS is_a

-- 이 테이블도 필요합니다
JOIN pg_attribute AS pg_a
  ON is_a.attribute_name = pg_a.attname

-- 하지만 pg_attribute에서 pg_namespace까지의 경로 조인은
-- 암묵적이어야 합니다
  AND pg_a.pg_class.relname = is_a.udt_name
  AND pg_a.pg_class.pg_namespace.nspname = is_a.udt_schema
WHERE is_a.data_type = 'ARRAY'
ORDER BY
  is_a.udt_schema,
  is_a.udt_name,
  is_a.attribute_name,
  is_a.ordinal_position
```

_그렇게_ 많이 짧아지지는 않지만, 각 단계를 어떻게 조인해야 하는지 더 이상 고민할 필요가 없다는 점에서 확실히 매우 편리합니다. 다른 경우에서는 `SELECT`나 `WHERE`에서 이러한 경로를 통해 암묵적 조인을 사용했지만, 이번에는 `JOIN .. ON` 절 내에서 사용하고 있다는 점에 주목하세요! jOOQ에서는 다음과 같이 작성할 수 있습니다:

```java
Attributes isA = ATTRIBUTES.as("is_a");
PgAttribute pgA = PgAttribute.as("pg_a");

ctx.select(
       isA.UDT_SCHEMA,
       isA.UDT_NAME,
       isA.ATTRIBUTE_NAME,
       pgA.ATTNDIMS)
   .from(isA)
   .join(pgA)
     .on(isA.ATTRIBUTE_NAME.eq(pgA.ATTNAME))
     .and(isA.UDT_NAME.eq(pgA.pgClass().RELNAME))
     .and(isA.UDT_SCHEMA.eq(pgA.pgClass().pgNamespace().NSPNAME))
   .where(isA.DATA_TYPE.eq("ARRAY"))
   .orderBy(
       isA.UDT_SCHEMA,
       isA.UDT_NAME,
       isA.ATTRIBUTE_NAME,
       isA.ORDINAL_POSITION)
   .fetch();
```

생성된 SQL은 원래 쿼리와 약간 다르게 보입니다. jOOQ의 암묵적 `JOIN` 알고리즘은 `LEFT JOIN`, `FULL JOIN` 또는 기타 연산자가 존재하는 경우 중요한 `JOIN` 연산자 우선순위를 보존하기 위해 `JOIN` 트리를 절대 평탄화하지 않기 때문입니다. 출력은 다음과 같이 보입니다:

```sql
FROM information_schema.attributes AS is_a
  JOIN (
    pg_catalog.pg_attribute AS pg_a
      JOIN (
        pg_catalog.pg_class AS alias_70236485
          JOIN pg_catalog.pg_namespace AS alias_96617829
            ON alias_70236485.relnamespace = alias_96617829.oid
      )
        ON pg_a.attrelid = alias_70236485.oid
    )
    ON (
      is_a.attribute_name = pg_a.attname
      AND is_a.udt_name = alias_70236485.relname
      AND is_a.udt_schema = alias_96617829.nspname
    )
```

보시다시피, "읽기 쉬운" 테이블 별칭(`is_a`와 `pg_a`)은 사용자가 제공한 것이고, "읽기 어려운" 시스템 생성 별칭(`alias_70236485`와 `alias_96617829`)은 암묵적 `JOIN`에서 생성된 것입니다. 그리고 다시 한번 강조하지만, 이러한 암묵적 조인이 경로 표현식을 시작한 경로 루트 `pg_a`가 속한 바로 그 위치에 포함되는 것이 중요합니다. 이것이 올바른 `JOIN` 연산자 우선순위 의미론을 유지할 수 있는 유일한 방법입니다. 예를 들어 `is_a`와 `pg_a` 사이에 `LEFT JOIN`을 사용한 경우가 이에 해당합니다.

## 향후 개선 사항

향후에는 이러한 그래프를 직접 연결할 수 있는 더 나은 `JOIN` 경로가 제공될 수 있습니다. `information_schema.attributes`와 `pg_catalog.pg_attribute`를 조인해야 할 때마다 `(udt_schema, udt_name, attribute_name)` 튜플에 대해 동일한 등호 조건을 반복해야 하기 때문입니다. 암묵적 `JOIN`이 도움이 되었지만, 이것이 더 개선될 수 있다는 것은 쉽게 알 수 있습니다. 이상적인 쿼리는 다음과 같을 것입니다:

```sql
SELECT
  is_a.udt_schema,
  is_a.udt_name,
  is_a.attribute_name,
  pg_a.attndims
FROM information_schema.attributes AS is_a

-- 여기서 마법이 일어납니다
MAGIC JOIN pg_attribute AS pg_a
  ON jooq_do_your_thing
WHERE is_a.data_type = 'ARRAY'
ORDER BY
  is_a.udt_schema,
  is_a.udt_name,
  is_a.attribute_name,
  is_a.ordinal_position
```

하지만 아직 거기까지는 도달하지 못했습니다.

## 조인 경로에 접근하기

`information_schema` 뷰와 `pg_catalog` 테이블 모두 외래 키 메타데이터를 노출하지 않습니다. 외래 키 메타데이터는 암묵적 조인 경로 표현식 및 기타 jOOQ 코드 생성 기능의 전제 조건입니다. 이것은 큰 문제가 아닌데, 바로 이런 이유로 코드 생성기에 합성 외래 키를 지정할 수 있기 때문입니다. information schema 쿼리를 위한 합성 외래 키에 관한 이전 블로그 포스트도 참고하세요. 이 경우, 최소한 다음과 같은 명세만 있으면 됩니다:

```xml
<configuration>
  <generator>
    <database>
      <syntheticObjects>
        <foreignKeys>
          <foreignKey>
            <tables>pg_attribute</tables>
            <fields><field>attrelid</field></fields>
            <referencedTable>pg_class</referencedTable>
          </foreignKey>
          <foreignKey>
            <tables>pg_class</tables>
            <fields><field>relnamespace</field></fields>
            <referencedTable>pg_namespace</referencedTable>
          </foreignKey>
        </foreignKeys>
      </syntheticObjects>
    </database>
  </generator>
</configuration>
```

이렇게 하면, 이전 예제에서 본 것과 같은 `JOIN` 경로를 사용할 수 있게 됩니다.

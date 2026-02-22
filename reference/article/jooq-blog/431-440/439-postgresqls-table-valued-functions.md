# PostgreSQL의 테이블 값 함수

> 원문: https://blog.jooq.org/postgresqls-table-valued-functions/

게시일: 2014년 7월 7일 | 작성자: lukaseder

테이블 값 함수는 정말 훌륭한 기능입니다. 많은 데이터베이스가 어떤 식으로든 이를 지원하며, PostgreSQL도 마찬가지입니다. PostgreSQL에서는 (거의) 모든 것이 테이블입니다. 예를 들어, 다음과 같이 작성할 수 있습니다:

```sql
CREATE OR REPLACE FUNCTION
    f_1 (v1 INTEGER, v2 OUT INTEGER)
AS $$
BEGIN
    v2 := v1;
END
$$ LANGUAGE plpgsql;
```

믿거나 말거나, 이것이 바로 테이블입니다! 다음과 같이 작성할 수 있습니다:

```sql
select * from f_1(1);
```

위 쿼리는 다음을 반환합니다:

```
+----+
| v2 |
+----+
|  1 |
+----+
```

생각해보면 꽤 직관적입니다. 단일 컬럼을 가진 단일 레코드를 출력하는 것뿐입니다. 두 개의 컬럼을 원한다면, 다음과 같이 작성할 수 있습니다:

```sql
CREATE OR REPLACE FUNCTION
    f_2 (v1 INTEGER, v2 OUT INTEGER, v3 OUT INTEGER)
AS $$
BEGIN
    v2 := v1;
    v3 := v1 + 1;
END
$$ LANGUAGE plpgsql;
```

그리고 나서:

```sql
select * from f_2(1);
```

위 쿼리는 다음을 반환합니다:

```
+----+----+
| v2 | v3 |
+----+----+
|  1 |  2 |
+----+----+
```

유용하긴 하지만, 이것들은 단일 레코드일 뿐입니다. 전체 테이블을 생성하고 싶다면 어떻게 해야 할까요? 간단합니다. `OUT` 파라미터를 사용하는 대신 실제로 `TABLE` 타입을 반환하도록 함수를 변경하면 됩니다:

```sql
CREATE OR REPLACE FUNCTION f_3 (v1 INTEGER)
RETURNS TABLE(v2 INTEGER, v3 INTEGER)
AS $$
BEGIN
    RETURN QUERY
    SELECT *
    FROM (
        VALUES(v1, v1 + 1),
              (v1 * 2, (v1 + 1) * 2)
    ) t(a, b);
END
$$ LANGUAGE plpgsql;
```

위의 매우 유용한 함수를 SELECT하면 다음과 같은 테이블을 얻게 됩니다:

```sql
select * from f_3(1);
```

위 쿼리는 다음을 반환합니다:

```
+----+----+
| v2 | v3 |
+----+----+
|  1 |  2 |
|  2 |  4 |
+----+----+
```

그리고 원한다면 이 함수를 다른 테이블과 `LATERAL` 조인할 수 있습니다:

```sql
select *
from book, lateral f_3(book.id)
```

이것은 예를 들어 다음과 같은 결과를 생성할 수 있습니다:

```
+----+--------------+----+----+
| id | title        | v2 | v3 |
+----+--------------+----+----+
|  1 | 1984         |  1 |  2 |
|  1 | 1984         |  2 |  4 |
|  2 | Animal Farm  |  2 |  4 |
|  2 | Animal Farm  |  4 |  6 |
+----+--------------+----+----+
```

사실, 적어도 PostgreSQL에서는 `LATERAL` 키워드가 이 경우에 선택적인 것으로 보입니다. 테이블 값 함수는 매우 강력합니다!

## 테이블 값 함수 발견하기

jOOQ의 스키마 역엔지니어링 관점에서, [이 Stack Overflow 질문](https://stackoverflow.com/q/24512606/521799)에서 볼 수 있듯이 상황이 조금 까다로워질 수 있습니다. PostgreSQL은 `OUT` 파라미터를 `TABLE` 반환 타입과 매우 유사한 방식으로 처리합니다. 이는 `INFORMATION_SCHEMA`에 대한 다음 쿼리에서 확인할 수 있습니다:

```sql
SELECT r.routine_name, r.data_type, p.parameter_name, p.data_type
FROM   information_schema.routines r
JOIN   information_schema.parameters p
USING (specific_catalog, specific_schema, specific_name);
```

출력은 다음과 같습니다:

```
routine_name | data_type | parameter_name | data_type
-------------+-----------+----------------+----------
f_1          | integer   | v1             | integer
f_1          | integer   | v2             | integer
f_2          | record    | v1             | integer
f_2          | record    | v2             | integer
f_2          | record    | v3             | integer
f_3          | record    | v1             | integer
f_3          | record    | v2             | integer
f_3          | record    | v3             | integer
```

보시다시피, 이 관점에서 출력은 정말 구분이 불가능합니다. 다행히도, `pg_catalog.pg_proc` 테이블을 조인할 수 있으며, 이 테이블에는 함수가 집합을 반환하는지 여부를 나타내는 관련 플래그가 포함되어 있습니다:

```sql
SELECT   r.routine_name,
         r.data_type,
         p.parameter_name,
         p.data_type,
         pg_p.proretset
FROM     information_schema.routines r
JOIN     information_schema.parameters p
USING   (specific_catalog, specific_schema, specific_name)
JOIN     pg_namespace pg_n
ON       r.specific_schema = pg_n.nspname
JOIN     pg_proc pg_p
ON       pg_p.pronamespace = pg_n.oid
AND      pg_p.proname = r.routine_name
ORDER BY routine_name, parameter_name;
```

이제 다음을 얻게 됩니다:

```
routine_name | data_type | parameter_name | data_type | proretset
-------------+-----------+----------------+-----------+----------
f_1          | integer   | v1             | integer   | f
f_1          | integer   | v2             | integer   | f
f_2          | record    | v1             | integer   | f
f_2          | record    | v2             | integer   | f
f_2          | record    | v3             | integer   | f
f_3          | record    | v1             | integer   | t
f_3          | record    | v2             | integer   | t
f_3          | record    | v3             | integer   | t
```

`f_3`만이 실제로 레코드 집합을 반환하는 함수이고, `f_1`과 `f_2`는 단일 레코드만 반환한다는 것을 알 수 있습니다. 이제 `OUT` 파라미터가 아닌 모든 파라미터를 제거하면 테이블 타입을 얻게 됩니다:

```sql
SELECT   r.routine_name,
         p.parameter_name,
         p.data_type,
         row_number() OVER (
           PARTITION BY r.specific_name
           ORDER BY p.ordinal_position
         ) AS ordinal_position
FROM     information_schema.routines r
JOIN     information_schema.parameters p
USING   (specific_catalog, specific_schema, specific_name)
JOIN     pg_namespace pg_n
ON       r.specific_schema = pg_n.nspname
JOIN     pg_proc pg_p
ON       pg_p.pronamespace = pg_n.oid
AND      pg_p.proname = r.routine_name
WHERE    pg_p.proretset
AND      p.parameter_mode = 'OUT'
ORDER BY routine_name, parameter_name;
```

이것은 다음을 제공합니다:

```
routine_name | parameter_name | data_type | position |
-------------+----------------+-----------+----------+
f_3          | v2             | integer   |        1 |
f_3          | v3             | integer   |        2 |
```

## jOOQ에서 이러한 쿼리를 어떻게 실행할까요?

위의 코드가 생성되면, 어떤 jOOQ 쿼리에서도 테이블 값 함수를 쉽게 호출할 수 있습니다. BOOK 예제를 다시 생각해봅시다 (SQL로):

```sql
select *
from book, lateral f_3(book.id)
```

그리고 jOOQ로:

```java
DSL.using(configuration)
   .select()
   .from(BOOK, lateral(F_3.call(BOOK.ID)))
   .fetch();
```

반환된 레코드에는 다음과 같은 값이 포함됩니다:

```java
record.getValue(F_3.V2);
record.getValue(F_3.V3);
```

이 모든 타입 안전성은 곧 출시될 jOOQ 3.5에서 무료로 사용할 수 있습니다! (SQL Server, Oracle, HSQLDB 테이블 값 함수는 이미 지원됩니다!)

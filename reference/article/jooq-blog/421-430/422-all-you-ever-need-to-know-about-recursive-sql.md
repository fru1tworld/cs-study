# 재귀 SQL에 대해 알아야 할 모든 것

> 원문: https://blog.jooq.org/all-you-ever-need-to-know-about-recursive-sql/

게시일: 2014년 8월 18일 | 저자: Lukas Eder

## 소개

여러분 중 일부는 Oracle의 SYNONYM에 익숙할 것입니다. 이것은 하나의 데이터베이스 객체(예: 스키마와 테이블)가 다른 데이터베이스 객체(예: 다른 스키마와 테이블)를 "가리키도록" 하는 데 사용됩니다. 이는 일반적으로 하위 호환성을 위해 수행됩니다.

일반적으로 다음과 같은 시나리오를 발견합니다:

```sql
CREATE TABLE my_table (col NUMBER(7));
CREATE SYNONYM my_table_old FOR my_table;
CREATE SYNONYM my_table_bak FOR my_table_old;
```

이 경우 다음과 같은 체인이 있습니다:

```
my_table_bak -> my_table_old -> my_table
```

`my_table_bak`이나 `my_table_old`를 쿼리하는 사용자는 `my_table`에서 데이터를 가져오는 것을 알아차리지 못합니다. 이것은 레거시 뷰, SQL 쿼리 또는 저장 프로시저가 오래된 테이블 이름을 사용하는 경우에 유용합니다.

## ALL_SYNONYMS 테이블

Oracle은 `ALL_SYNONYMS` 딕셔너리 뷰를 통해 시노님 메타데이터를 제공합니다:

```sql
SELECT *
FROM   ALL_SYNONYMS
WHERE  TABLE_OWNER = 'PLAYGROUND'
```

이 쿼리는 `PLAYGROUND` 스키마의 시노님들을 보여줍니다. 하지만 이 간단한 쿼리는 전이적 관계(transitive relationships)를 명확하게 해결하지 못합니다.

결과는 다음과 같을 수 있습니다:

| OWNER | SYNONYM_NAME | TABLE_OWNER | TABLE_NAME |
|-------|--------------|-------------|------------|
| PLAYGROUND | MY_TABLE_BAK | PLAYGROUND | MY_TABLE_OLD |
| PLAYGROUND | MY_TABLE_OLD | PLAYGROUND | MY_TABLE |

보시다시피, 각 시노님이 직접 가리키는 대상만 보여줍니다. 하지만 `MY_TABLE_BAK`이 최종적으로 `MY_TABLE`을 가리킨다는 것을 알려면 이 체인을 직접 따라가야 합니다.

## CONNECT BY를 사용한 계층적 쿼리

바로 이때 마법의 `CONNECT BY` 절이 등장합니다! 계층의 각 레벨에서 `TABLE_NAME`을 이전의 `SYNONYM_NAME`과 연결합니다.

```sql
SELECT
  s.OWNER,
  s.SYNONYM_NAME,
  CONNECT_BY_ROOT s.TABLE_OWNER TABLE_OWNER,
  CONNECT_BY_ROOT s.TABLE_NAME  TABLE_NAME
FROM       ALL_SYNONYMS s
WHERE      s.TABLE_OWNER = 'PLAYGROUND'
CONNECT BY s.TABLE_OWNER = PRIOR s.OWNER
AND        s.TABLE_NAME  = PRIOR s.SYNONYM_NAME
```

`CONNECT BY` 절에서:
- `PRIOR`는 재귀의 이전 레벨(부모 행)을 참조합니다
- 현재 행의 `TABLE_OWNER`가 이전 행의 `OWNER`와 일치하고
- 현재 행의 `TABLE_NAME`이 이전 행의 `SYNONYM_NAME`과 일치하면 연결됩니다

`CONNECT_BY_ROOT` 함수는 각 계층 경로에서 루트 값(시작점)을 반환합니다. 이를 통해 체인의 최종 대상을 식별할 수 있습니다.

## LEVEL을 사용한 재귀 깊이 확인

`LEVEL` 의사 열(pseudo-column)을 추가하면 재귀 깊이를 확인할 수 있습니다:

```sql
SELECT
  LEVEL,
  s.OWNER,
  s.SYNONYM_NAME,
  CONNECT_BY_ROOT s.TABLE_OWNER TABLE_OWNER,
  CONNECT_BY_ROOT s.TABLE_NAME  TABLE_NAME
FROM       ALL_SYNONYMS s
WHERE      s.TABLE_OWNER = 'PLAYGROUND'
CONNECT BY s.TABLE_OWNER = PRIOR s.OWNER
AND        s.TABLE_NAME  = PRIOR s.SYNONYM_NAME
```

결과:

| LEVEL | OWNER | SYNONYM_NAME | TABLE_OWNER | TABLE_NAME |
|-------|-------|--------------|-------------|------------|
| 1 | PLAYGROUND | MY_TABLE_OLD | PLAYGROUND | MY_TABLE |
| 2 | PLAYGROUND | MY_TABLE_BAK | PLAYGROUND | MY_TABLE |
| 1 | PLAYGROUND | MY_TABLE_BAK | PLAYGROUND | MY_TABLE_OLD |

`LEVEL`이 1이면 해당 시노님이 직접 대상 객체를 가리키고, 더 높은 숫자는 중간 시노님을 거친다는 것을 의미합니다.

## START WITH 절로 필터링

결과에서 중간 시노님(예: `MY_TABLE_OLD`)부터 시작하는 별도의 레코드가 나타나는 것을 볼 수 있습니다. `START WITH` 절을 사용하면 계층 순회를 시작할 행을 제한할 수 있습니다:

```sql
SELECT
  s.OWNER,
  s.SYNONYM_NAME,
  CONNECT_BY_ROOT s.TABLE_OWNER TABLE_OWNER,
  CONNECT_BY_ROOT s.TABLE_NAME  TABLE_NAME
FROM       ALL_SYNONYMS s
WHERE      s.TABLE_OWNER = 'PLAYGROUND'
CONNECT BY s.TABLE_OWNER = PRIOR s.OWNER
AND        s.TABLE_NAME  = PRIOR s.SYNONYM_NAME
START WITH EXISTS (
  SELECT 1
  FROM   ALL_OBJECTS
  WHERE  s.TABLE_OWNER           = ALL_OBJECTS.OWNER
  AND    s.TABLE_NAME            = ALL_OBJECTS.OBJECT_NAME
  AND    ALL_OBJECTS.OWNER       = 'PLAYGROUND'
  AND    ALL_OBJECTS.OBJECT_TYPE <> 'SYNONYM'
)
```

`START WITH` 절은 시노님이 아닌 실제 객체(테이블, 뷰 등)로 재귀를 시작하도록 제한합니다. 이렇게 하면 중간 시노님이 별도의 결과 세트로 나타나는 것을 방지합니다.

## PUBLIC 시노님 처리

Oracle에는 PUBLIC 스키마라는 특별한 개념이 있습니다. PUBLIC 시노님은 모든 사용자가 접근할 수 있는 시노님입니다. 하지만 Oracle의 딕셔너리 뷰에서 PUBLIC 시노님은 약간 까다롭게 보고됩니다.

문제는 PUBLIC 시노님의 `TABLE_OWNER`가 대상 스키마로 보고되어 체인이 끊어진다는 것입니다. 해결책은 합성(synthetic) PUBLIC 소유권 레코드로 데이터를 복제하는 것입니다:

```sql
SELECT
  s.OWNER,
  s.SYNONYM_NAME,
  CONNECT_BY_ROOT s.TABLE_OWNER TABLE_OWNER,
  CONNECT_BY_ROOT s.TABLE_NAME  TABLE_NAME
FROM (
  SELECT OWNER, SYNONYM_NAME, TABLE_OWNER, TABLE_NAME
  FROM ALL_SYNONYMS
  UNION ALL
  SELECT OWNER, SYNONYM_NAME, 'PUBLIC', TABLE_NAME
  FROM ALL_SYNONYMS
) s
WHERE      s.TABLE_OWNER IN (
  'PLAYGROUND', 'PUBLIC'
)
CONNECT BY s.TABLE_OWNER = PRIOR s.OWNER
AND        s.TABLE_NAME  = PRIOR s.SYNONYM_NAME
START WITH EXISTS (
  SELECT 1
  FROM   ALL_OBJECTS
  WHERE  s.TABLE_OWNER           = ALL_OBJECTS.OWNER
  AND    s.TABLE_NAME            = ALL_OBJECTS.OBJECT_NAME
  AND    ALL_OBJECTS.OWNER       = 'PLAYGROUND'
  AND    ALL_OBJECTS.OBJECT_TYPE <> 'SYNONYM'
)
```

서브쿼리에서 `UNION ALL`을 사용하여:
1. 원래 시노님 레코드를 포함하고
2. `TABLE_OWNER`를 'PUBLIC'으로 설정한 복제 레코드를 추가합니다

이렇게 하면 PUBLIC 시노님 체인도 올바르게 해결됩니다.

## SYS_CONNECT_BY_PATH로 전체 경로 표시

계층 경로를 시각화하려면 `SYS_CONNECT_BY_PATH` 함수를 사용합니다. 이 함수는 전체 계층을 구분자로 연결된 문자열로 구성합니다:

```sql
SELECT
  SUBSTR(
    sys_connect_by_path(
         s.TABLE_OWNER
      || '.'
      || s.TABLE_NAME, ' <- '
    ) || ' <- '
      || s.OWNER
      || '.'
      || s.SYNONYM_NAME, 5
  )
FROM (
  SELECT OWNER, SYNONYM_NAME, TABLE_OWNER, TABLE_NAME
  FROM ALL_SYNONYMS
  UNION ALL
  SELECT OWNER, SYNONYM_NAME, 'PUBLIC', TABLE_NAME
  FROM ALL_SYNONYMS
) s
WHERE      s.TABLE_OWNER IN (
  'PLAYGROUND', 'PUBLIC'
)
CONNECT BY s.TABLE_OWNER = PRIOR s.OWNER
AND        s.TABLE_NAME  = PRIOR s.SYNONYM_NAME
START WITH EXISTS (
  SELECT 1
  FROM   ALL_OBJECTS
  WHERE  s.TABLE_OWNER           = ALL_OBJECTS.OWNER
  AND    s.TABLE_NAME            = ALL_OBJECTS.OBJECT_NAME
  AND    ALL_OBJECTS.OWNER       = 'PLAYGROUND'
  AND    ALL_OBJECTS.OBJECT_TYPE <> 'SYNONYM'
)
```

출력 예시:
```
PLAYGROUND.MY_TABLE <- PLAYGROUND.MY_TABLE_OLD <- PLAYGROUND.MY_TABLE_BAK
```

이것은 시노님 해결 체인을 명확하게 보여줍니다: `MY_TABLE_BAK`이 `MY_TABLE_OLD`를 가리키고, 이것이 다시 `MY_TABLE`을 가리킵니다.

## 오래된 시노님과 사이클 방지

때로는 더 이상 존재하지 않는 객체를 가리키는 "오래된(stale)" 시노님이 있을 수 있습니다. 이러한 시노님은 무한 루프(사이클)를 생성할 수 있습니다. 이를 방지하려면 자기 참조 레코드를 필터링해야 합니다:

```sql
WHERE (s.OWNER       , s.SYNONYM_NAME)
   != (s.TABLE_OWNER , s.TABLE_NAME)
```

이 조건은 시노님이 자기 자신을 가리키는 경우를 제외합니다.

또한 Oracle의 `CONNECT_BY_ISCYCLE` 의사 열과 `NOCYCLE` 키워드를 사용하여 사이클을 감지하고 방지할 수 있습니다:

```sql
SELECT
  s.OWNER,
  s.SYNONYM_NAME,
  CONNECT_BY_ISCYCLE AS is_cycle
FROM       ALL_SYNONYMS s
WHERE      s.TABLE_OWNER = 'PLAYGROUND'
CONNECT BY NOCYCLE s.TABLE_OWNER = PRIOR s.OWNER
AND        s.TABLE_NAME  = PRIOR s.SYNONYM_NAME
```

`NOCYCLE` 키워드는 사이클이 감지되면 재귀를 중지하고, `CONNECT_BY_ISCYCLE`은 사이클이 발생한 행을 식별합니다.

## jOOQ를 사용한 구현

jOOQ는 Oracle의 `CONNECT BY` 구문을 완벽하게 지원합니다. 다음은 위의 쿼리를 jOOQ의 유창한(fluent) API로 표현한 예시입니다:

```java
// jOOQ를 사용한 시노님 해결 쿼리
DSL.using(configuration)
   .select(
       ALL_SYNONYMS.OWNER,
       ALL_SYNONYMS.SYNONYM_NAME,
       connectByRoot(ALL_SYNONYMS.TABLE_OWNER).as("TABLE_OWNER"),
       connectByRoot(ALL_SYNONYMS.TABLE_NAME).as("TABLE_NAME")
   )
   .from(ALL_SYNONYMS)
   .where(ALL_SYNONYMS.TABLE_OWNER.eq("PLAYGROUND"))
   .connectBy(
       ALL_SYNONYMS.TABLE_OWNER.eq(prior(ALL_SYNONYMS.OWNER))
       .and(ALL_SYNONYMS.TABLE_NAME.eq(prior(ALL_SYNONYMS.SYNONYM_NAME)))
   )
   .startWith(
       exists(
           selectOne()
           .from(ALL_OBJECTS)
           .where(ALL_SYNONYMS.TABLE_OWNER.eq(ALL_OBJECTS.OWNER))
           .and(ALL_SYNONYMS.TABLE_NAME.eq(ALL_OBJECTS.OBJECT_NAME))
           .and(ALL_OBJECTS.OWNER.eq("PLAYGROUND"))
           .and(ALL_OBJECTS.OBJECT_TYPE.ne("SYNONYM"))
       )
   )
   .fetch();
```

jOOQ는 다음과 같은 계층적 쿼리 관련 메서드를 제공합니다:
- `connectBy()` - CONNECT BY 절
- `connectByRoot()` - CONNECT_BY_ROOT 함수
- `prior()` - PRIOR 연산자
- `sysConnectByPath()` - SYS_CONNECT_BY_PATH 함수
- `level()` - LEVEL 의사 열
- `startWith()` - START WITH 절

## 재귀적 공통 테이블 표현식 (Recursive CTE)

Oracle의 `CONNECT BY`는 강력하지만, 이것은 Oracle 특유의 구문입니다. SQL 표준은 재귀적 공통 테이블 표현식(Recursive CTE)을 제공하며, 이는 더 많은 데이터베이스에서 지원됩니다.

다음은 재귀적 CTE를 사용한 동등한 쿼리입니다:

```sql
WITH RECURSIVE synonym_chain (
  owner, synonym_name, table_owner, table_name, level_num
) AS (
  -- 앵커 멤버: 실제 객체를 가리키는 시노님으로 시작
  SELECT
    s.OWNER,
    s.SYNONYM_NAME,
    s.TABLE_OWNER,
    s.TABLE_NAME,
    1 AS level_num
  FROM ALL_SYNONYMS s
  WHERE EXISTS (
    SELECT 1
    FROM ALL_OBJECTS o
    WHERE s.TABLE_OWNER = o.OWNER
    AND s.TABLE_NAME = o.OBJECT_NAME
    AND o.OBJECT_TYPE <> 'SYNONYM'
  )

  UNION ALL

  -- 재귀 멤버: 이전 시노님을 가리키는 시노님 찾기
  SELECT
    s.OWNER,
    s.SYNONYM_NAME,
    sc.table_owner,  -- 루트의 TABLE_OWNER 유지
    sc.table_name,   -- 루트의 TABLE_NAME 유지
    sc.level_num + 1
  FROM ALL_SYNONYMS s
  JOIN synonym_chain sc
    ON s.TABLE_OWNER = sc.OWNER
    AND s.TABLE_NAME = sc.SYNONYM_NAME
)
SELECT * FROM synonym_chain;
```

재귀적 CTE는 두 부분으로 구성됩니다:
1. 앵커 멤버(Anchor member): 재귀의 시작점을 정의
2. 재귀 멤버(Recursive member): `UNION ALL`로 연결되며, CTE 자체를 참조

## 데이터베이스 지원

CONNECT BY 지원:
- Oracle
- CUBRID
- Informix

재귀적 CTE 지원:
- PostgreSQL
- SQL Server
- DB2
- Firebird
- HSQLDB
- Sybase SQL Anywhere
- H2 (실험적)
- Oracle 11g R2 이상
- MySQL 8.0 이상
- MariaDB 10.2 이상
- SQLite 3.8.3 이상

## 결론

재귀 SQL은 계층적 데이터를 처리하는 강력한 도구입니다. Oracle의 `CONNECT BY`와 SQL 표준의 재귀적 CTE 모두 관계형 데이터베이스에서 트리 구조나 그래프를 탐색하는 데 효과적입니다.

단순한 계층 구조의 경우 이러한 기법들이 잘 작동합니다. 하지만 매우 복잡한 그래프 데이터의 경우, Neo4j와 같은 전용 그래프 데이터베이스를 고려해 볼 수 있습니다.

중요한 점은 관계형 데이터에 계층적 구조가 자연스럽게 나타날 때 이러한 기능을 사용하는 것입니다. 조직도, 파일 시스템, 카테고리 트리, 또는 이 글에서 본 것처럼 시노님 체인과 같은 경우가 좋은 예입니다.

jOOQ를 사용하면 이러한 재귀 쿼리를 타입 안전하고 데이터베이스에 독립적인 방식으로 작성할 수 있습니다.

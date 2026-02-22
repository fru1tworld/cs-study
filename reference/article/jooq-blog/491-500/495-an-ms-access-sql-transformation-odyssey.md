# MS Access SQL 변환 오디세이

> 원문: https://blog.jooq.org/an-ms-access-sql-transformation-odyssey/

최근 jOOQ 3.3에서 MS Access 데이터베이스 지원을 추가했습니다. 이것은 지금까지 가장 어려운 통합 작업이었을 것입니다. MS Access 데이터베이스는 고유한 방식이 있고, 그것도 아주 많습니다. 하지만 다행히도 jOOQ의 내부 SQL 변환 기능은 이미 매우 발전되어 있습니다. 이전에 행 값 표현식 IN 조건자 에뮬레이션에 관한 블로그 포스트에서 보여드린 바 있습니다. 이 글에서는 다중 값 INSERT 문을 MS Access에서도 작동하도록 여러 단계를 거쳐 투명하게 에뮬레이션하는 방법을 보여드리겠습니다.

다음과 같은 표준 SQL-92 `INSERT` 문을 고려해 봅시다:

```sql
INSERT INTO books (author, title)
VALUES ('George Orwell', '1984'),
       ('Leo Tolstoy', 'War and Peace');
```

jOOQ로는 다음과 같이 간단하게 작성할 수 있습니다:

```java
DSL.using(configuration)
   .insertInto(BOOKS, AUTHOR, TITLE)
   .values("George Orwell", "1984")
   .values("Leo Tolstoy", "War and Peace")
   .execute();
```

위의 다중 레코드 `INSERT` 구문은 다양한 데이터베이스에서 지원되지만, 다음 데이터베이스들에서는 지원되지 않습니다:

- Firebird
- Ingres
- MS Access
- Oracle
- SQLite
- Sybase Adaptive Server Enterprise

하지만 다행히도 위의 구문은 `INSERT .. SELECT`를 사용하여 에뮬레이션할 수 있습니다:

```sql
INSERT INTO books (author, title)
SELECT 'George Orwell', '1984'
UNION ALL
SELECT 'Leo Tolstoy', 'War and Peace';
```

일부 데이터베이스는 대부분의 SQL 문에서 `FROM` 절을 필요로 합니다. MS Access도 마찬가지입니다. Oracle에서 DUAL이라고 부르는 것을 MS Access에서 에뮬레이션하는 간단한 방법은 다음과 같습니다:

```sql
INSERT INTO books (author, title)
SELECT 'George Orwell', '1984'
FROM (SELECT count(*) FROM MSysResources) AS DUAL
UNION ALL
SELECT 'Leo Tolstoy', 'War and Peace'
FROM (SELECT count(*) FROM MSysResources) AS DUAL
```

간단히 하기 위해 DUAL이 실제로 존재한다고 가정해 봅시다:

```sql
INSERT INTO books (author, title)
SELECT 'George Orwell', '1984'
FROM DUAL
UNION ALL
SELECT 'Leo Tolstoy', 'War and Peace'
FROM DUAL
```

하지만 이 구문도 MS Access에서는 지원되지 않습니다. 매뉴얼에서 볼 수 있듯이 다음과 같이 설명되어 있습니다:

> "구문 다중 레코드 추가 쿼리: INSERT INTO target [(field1[, field2[, ...]])] [IN externaldatabase] SELECT field1[, field2[, ...] FROM tableexpression. tableexpression: 레코드가 삽입되는 테이블 또는 테이블들의 이름."

`UNION ALL` 절이 들어갈 자리가 분명히 없지만, `FROM` 절에서 "저장된 쿼리"를 사용할 수 있습니다. 우리의 원래 의도를 감안하면, 이것은 대략 다음과 같이 변환됩니다:

```sql
INSERT INTO books (author, title)
SELECT *
FROM (
  SELECT 'George Orwell', '1984'
  FROM DUAL
  UNION ALL
  SELECT 'Leo Tolstoy', 'War and Peace'
  FROM DUAL
)
```

안타깝게도 위의 시도는 다음과 같은 오류 메시지를 발생시킵니다:

> "소스 또는 대상 테이블에 다중 값 필드가 포함된 경우 INSERT INTO 쿼리에서 SELECT *를 사용할 수 없습니다"

따라서 새로 생성된 파생 테이블에서 각 컬럼을 명시적으로 선택해야 합니다. 하지만 그 컬럼들에는 (아직) 이름이 없습니다. 파생 테이블의 컬럼에 이름을 할당하는 표준 방법은 파생 컬럼 목록을 사용하는 것입니다. 이를 통해 테이블과 모든 컬럼의 이름을 한 번에 바꿀 수 있습니다. 우리의 SQL 문에서 이것은 다음을 의미합니다:

```sql
INSERT INTO books (author, title)
SELECT a, b
FROM (
  SELECT 'George Orwell', '1984'
  FROM DUAL
  UNION ALL
  SELECT 'Leo Tolstoy', 'War and Peace'
  FROM DUAL
) t(a, b)
```

이쯤 되면 우리의 SQL 변환 오디세이가 아직 끝나지 않았다는 것을 짐작하셨을 것입니다. MS Access는 파생 컬럼 목록을 지원하지 않으며, 같은 처지에 있는 데이터베이스들도 많습니다. 다음 데이터베이스들도 지원하지 않습니다:

- H2
- Ingres
- MariaDB
- MS Access
- MySQL
- Oracle
- SQLite

이것은 또 다른 SQL 기능을 에뮬레이션해야 한다는 것을 의미하며, 결과적으로 다음과 같은 쿼리가 됩니다:

```sql
INSERT INTO books (author, title)
SELECT a, b
FROM (
  -- 이 서브쿼리는 컬럼 이름을 정의합니다
  SELECT '' AS a, '' AS b
  FROM DUAL
  WHERE 1 = 0
  UNION ALL
  -- 이 서브쿼리는 우리의 데이터를 제공합니다
  SELECT *
  FROM (
    SELECT 'George Orwell', '1984'
    FROM DUAL
    UNION ALL
    SELECT 'Leo Tolstoy', 'War and Peace'
    FROM DUAL
  ) t
) t
```

이제 됐습니다. 거의요. 실제 DUAL 표현식을 쿼리에 다시 대입해 봅시다:

```sql
INSERT INTO books (author, title)
SELECT a, b
FROM (
  -- 이 서브쿼리는 컬럼 이름을 정의합니다
  SELECT '' AS a, '' AS b
  FROM (SELECT count(*) FROM MSysResources) AS DUAL
  WHERE 1 = 0
  UNION ALL
  -- 이 서브쿼리는 우리의 데이터를 제공합니다
  SELECT *
  FROM (
    SELECT 'George Orwell', '1984'
    FROM (SELECT count(*) FROM MSysResources) AS DUAL
    UNION ALL
    SELECT 'Leo Tolstoy', 'War and Peace'
    FROM (SELECT count(*) FROM MSysResources) AS DUAL
  ) t
) t
```

이거 정말 아름답지 않나요!?

## 실제 시나리오

실제로는 아무도 이런 SQL을 수동으로 작성하거나 읽고 유지보수하지 않기 때문에 어떤 식으로든 이 제한 사항을 우회할 것입니다. 아마도 단일 레코드 `INSERT` 문을 여러 번 실행하거나, 배치 처리를 하거나, 다른 방법을 사용할 것입니다. 하지만 실제로는 MS Access 외에도 SQL Server나 Oracle 또는 다른 데이터베이스를 지원해야 할 것이고, 이런 종류의 문제들을 수동으로 패치해야 하는 상황에 끊임없이 직면하게 됩니다. 이것은 매우 답답할 수 있습니다! 물론, jOOQ를 사용하고 위의 세부 사항들을 잊어버린 채 다음과 같이 직관적인 표준 SQL 문을 간단히 작성한다면 말이죠:

```java
DSL.using(configuration)
   .insertInto(BOOKS, AUTHOR, TITLE)
   .values("George Orwell", "1984")
   .values("Leo Tolstoy", "War and Peace")
   .execute();
```

... 이것은 jOOQ가 지원하는 16개의 모든 RDBMS에서 정확히 이와 같이 작동합니다. 무엇을 기다리고 계신가요?

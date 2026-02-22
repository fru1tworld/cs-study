# SQL GROUP BY에서의 함수적 종속성

> 원문: https://blog.jooq.org/functional-dependencies-in-sql-group-by/

SQL 표준에는 흥미로운 기능이 있습니다. `GROUP BY` 절에 나열된 기본 키(또는 유니크 키)의 함수적 종속성을 가진 컬럼들을 `GROUP BY` 절에 명시적으로 추가하지 않고도 SELECT 절에서 사용할 수 있습니다.

이것이 무슨 의미일까요? 다음과 같은 간단한 스키마를 생각해봅시다:

```sql
CREATE TABLE author (
  id INT NOT NULL PRIMARY KEY,
  name TEXT NOT NULL
);

CREATE TABLE book (
  id INT NOT NULL PRIMARY KEY,
  author_id INT NOT NULL REFERENCES author,
  title TEXT NOT NULL
);
```

저자별 도서 수를 세기 위해 우리는 보통 다음과 같이 작성합니다:

```sql
SELECT a.name, count(b.id)
FROM author a
LEFT JOIN book b ON a.id = b.author_id
GROUP BY
  a.id,  -- 이름은 유니크하지 않으므로 필수
  a.name -- 일부 데이터베이스에서는 필수이지만, 다른 곳에서는 아님
```

이 경우 유니크한 값으로 그룹화해야 합니다. 왜냐하면 두 명의 저자가 모두 John Doe라는 이름을 가지고 있더라도, 여전히 별도의 그룹을 생성해야 하기 때문입니다. 따라서 `GROUP BY a.id`는 필수입니다.

우리는 `a.name`도 `GROUP BY`에 포함하는 것에 익숙합니다. 특히 다음과 같이 이를 요구하는 데이터베이스에서는 `a.name`을 `SELECT` 절에서 사용하므로 필수입니다:

- Db2
- Derby
- Exasol
- Firebird
- HANA
- Informix
- Oracle
- SQL Server

하지만 정말 필수일까요? SQL 표준에 따르면 필수가 아닙니다. `author.id`와 `author.name` 사이에 함수적 종속성이 있기 때문입니다. 다시 말해, 각 `author.id` 값에 대해 정확히 하나의 `author.name` 값만 존재합니다. 즉, `author.name`은 `author.id`의 함수입니다.

이것은 두 컬럼을 모두 `GROUP BY`하든, 기본 키만 `GROUP BY`하든 상관없다는 것을 의미합니다. 두 경우 모두 결과는 동일해야 하므로, 다음과 같이 작성할 수 있습니다:

```sql
SELECT a.name, count(b.id)
FROM author a
LEFT JOIN book b ON a.id = b.author_id
GROUP BY a.id
```

## 어떤 SQL 데이터베이스가 이를 지원하나요?

최소한 다음 SQL 데이터베이스들이 이 언어 기능을 지원합니다:

- CockroachDB
- H2
- HSQLDB
- MariaDB
- MySQL
- PostgreSQL
- SQLite
- Yugabyte

주목할 점은 MySQL이 예전에는 `GROUP BY`가 있을 때 컬럼을 명확하게 프로젝션할 수 있는지 여부를 단순히 무시했다는 것입니다. 다음 쿼리는 대부분의 데이터베이스에서 거부되지만, MySQL에서는 ONLY_FULL_GROUP_BY 모드가 도입되기 전까지 허용되었습니다:

```sql
SELECT author_id, title, count(*)
FROM author
GROUP BY author_id
```

저자가 두 권 이상의 책을 썼다면 `author.title`에 무엇을 표시해야 할까요? 이것은 의미가 없지만, MySQL은 여전히 이를 허용했고, 그룹에서 임의의 값을 프로젝션했습니다.

오늘날 MySQL은 SQL 표준에서 허용하는 대로, `GROUP BY` 절에 대한 함수적 종속성이 있는 컬럼만 프로젝션할 수 있도록 허용합니다.

## 장단점

추가 컬럼을 피하는 더 짧은 구문이 유지보수하기 쉬울 수 있지만(필요한 경우 추가 컬럼을 쉽게 프로젝션할 수 있음), 프로덕션에서 쿼리가 깨질 위험이 있습니다. 예를 들어 마이그레이션을 위해 기본 제약 조건이 비활성화된 경우입니다. 라이브 시스템에서 기본 키가 비활성화될 가능성은 낮지만, 여전히 그럴 수 있습니다. 키가 없으면 MySQL의 이전 해석이 유효하지 않았던 것과 같은 이유로 이전에 유효했던 쿼리가 더 이상 유효하지 않게 됩니다: 함수적 종속성의 보장이 더 이상 없기 때문입니다.

## 다른 구문

jOOQ 3.16과 #11834부터, `GROUP BY` 절에서 개별 컬럼 대신 테이블을 직접 참조할 수 있게 됩니다. 예를 들어:

```sql
SELECT a.name, count(b.id)
FROM author a
LEFT JOIN book b ON a.id = b.author_id
GROUP BY a
```

의미는 다음과 같습니다:

- 테이블에 기본 키가 있으면(복합 키든 아니든), `GROUP BY` 절에서 대신 그것을 사용합니다.
- 테이블에 기본 키가 없으면, 테이블의 모든 컬럼을 나열합니다.

jOOQ가 지원하는 RDBMS 중 현재 이 구문을 지원하는 것이 없으므로, 이것은 순수하게 jOOQ만의 합성 기능입니다.

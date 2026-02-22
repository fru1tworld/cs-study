# SQL에서 바이너리 데이터, 더 많은 퀴즈

> 원문: https://blog.jooq.org/binary-data-in-sql-more-trivia/

어제, 저는 [SQL에서 boolean 리터럴 인라이닝](https://blog.jooq.org/sql-and-booleans-some-trivia/)에 대해 블로그에 글을 썼습니다. 이 주제는 다른 데이터 타입으로도 이어집니다. BLOB과 BINARY 데이터를 전반적으로 살펴봅시다.

이것은 SQL 표준에도 정의되어 있지만, SQL 1992에는 포함되어 있지 않습니다:

```
<binary string literal> ::=
  X <quote> [ <space>... ]
  [ { <hexit> [ <space>... ] <hexit> [ <space>... ] }... ] <quote>
```

하지만 이것이 모든 데이터베이스에서 그대로 지원될까요? 물론, 늘 그렇듯이, 답은 'NO'입니다.

SQL Server, Sybase ASE, Sybase SQL Anywhere

T-SQL 데이터베이스들은 바이너리 리터럴을 다른 프로그래밍 언어에서 16진수 숫자를 다루는 것처럼 처리합니다.

```sql
INSERT INTO lob_table VALUES (0x01FF);
```

DB2

DB2는 표준을 따르지만, blob 생성자를 사용해야 할 수 있습니다. VARCHAR FOR BIT DATA 타입에는 이것이 필요하지 않습니다.

```sql
INSERT INTO lob_table VALUES (blob(X'01FF'));
```

Derby, H2, HSQLDB, Ingres, MySQL, SQLite

대부분의 데이터베이스는 표준을 따르며, 특히 Java 데이터베이스들이 그렇습니다.

```sql
INSERT INTO lob_table VALUES (X'01FF');
```

Oracle

Oracle은 표준의 "X"를 생략합니다.

```sql
INSERT INTO lob_table VALUES (hextoraw('01FF'));
```

PostgreSQL

이상하게도 PostgreSQL은 이번에는 정말 동떨어져 있는데, 리터럴에서 바이트의 8진수 표현을 사용합니다. 16진수 인코딩도 있지만 제대로 동작시키지 못했습니다. 명시적 캐스트는 중요합니다.

```sql
INSERT INTO lob_table VALUES (E'\\001\\377'::bytea);
```

# IS DISTINCT FROM 술어

> 원문: https://blog.jooq.org/the-is-distinct-from-predicate/

SQL-1999 표준은 일반적인 부등 비교 술어와는 다르게 동작하는 유용한 IS DISTINCT FROM 술어를 명시하고 있습니다.

## 표준 정의

SQL-1999 표준(섹션 8.13)에 따르면:

```
<distinct predicate> ::=
  <row value expression 3> IS DISTINCT FROM <row value expression 4>
```

## DISTINCT 술어의 목적

DISTINCT 술어는 술어에서 NULL/UNKNOWN 값 처리 문제를 해결합니다. 이를 진리표를 통해 설명하겠습니다:

부등 비교 술어:
- NULL != NULL은 NULL을 반환합니다 (FALSE가 아님)
- NULL != [임의의 값]은 NULL을 반환합니다 (TRUE가 아님)
- [임의의 값] != NULL은 NULL을 반환합니다 (TRUE가 아님)
- [임의의 값] != [임의의 값]은 TRUE/FALSE를 반환합니다

DISTINCT 술어:
- NULL IS DISTINCT FROM NULL은 FALSE를 반환합니다
- NULL IS DISTINCT FROM [임의의 값]은 TRUE를 반환합니다
- [임의의 값] IS DISTINCT FROM NULL은 TRUE를 반환합니다
- [임의의 값] IS DISTINCT FROM [임의의 값]은 TRUE/FALSE를 반환합니다
- NULL IS NOT DISTINCT FROM NULL은 TRUE를 반환합니다
- NULL IS NOT DISTINCT FROM [임의의 값]은 FALSE를 반환합니다
- [임의의 값] IS NOT DISTINCT FROM NULL은 FALSE를 반환합니다
- [임의의 값] IS NOT DISTINCT FROM [임의의 값]은 TRUE/FALSE를 반환합니다

DISTINCT 술어는 "NULL을 대부분의 다른 언어들이 처리하는 방식과 동일하게" 다루므로, 프로그래밍 언어에서의 NULL 처리에 익숙한 개발자들에게 더 직관적입니다.

## 데이터베이스 지원

네이티브 지원 (오픈 소스):
- Firebird
- H2
- HSQLDB
- Postgres

MySQL 대안:
MySQL은 표준 술어 대신 동등한 동작을 하는 특별한 "equal-to (<=>)" 연산자를 사용합니다.

네이티브 지원 없음:
다음 데이터베이스들은 네이티브 DISTINCT 술어 지원이 없습니다:
- CUBRID
- DB2
- Derby
- Ingres
- Oracle
- SQL Server
- SQLite
- Sybase ASE
- Sybase SQL Anywhere

## jOOQ 시뮬레이션

지원되지 않는 데이터베이스의 경우, jOOQ는 다음과 같이 술어를 시뮬레이션합니다:

```sql
CASE WHEN [this] IS     NULL AND [field] IS     NULL THEN FALSE
     WHEN [this] IS     NULL AND [field] IS NOT NULL THEN TRUE
     WHEN [this] IS NOT NULL AND [field] IS     NULL THEN TRUE
     WHEN [this] =               [field]             THEN FALSE
     ELSE                                                 TRUE
END
```

## 댓글 섹션

Chris Bandy (2013년 9월 8일)는 SQLite가 IS와 IS NOT 연산자로 이 기능을 지원한다고 언급하며, SQLite 문서를 참조했습니다.

lukaseder는 GitHub 이슈 #2729를 통해 다음 jOOQ 버전에 이 기능이 추가될 것이라고 답변했습니다.

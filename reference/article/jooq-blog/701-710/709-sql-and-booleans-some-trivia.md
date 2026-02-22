# SQL과 불리언, 약간의 퀴즈

> 원문: https://blog.jooq.org/sql-and-booleans-some-trivia/

SQL과 불리언에 대한 약간의 퀴즈:

SQL 1992는 불리언에 대해 세 가지 값을 정의한다:

```
<truth value> ::=
        TRUE
      | FALSE
      | UNKNOWN
```

하지만 진정한 불리언이 항상 지원되는 것은 아니다. 다음은 불리언 지원에 대한 진리표이다:

| SQL 방언 | 불리언 지원 여부 |
|---|---|
| DB2 | 0 (대신 1/0을 사용) |
| Derby | true (안전하게 true/false를 사용할 수 있음) |
| H2 | true |
| HSQLDB | true |
| Ingres | true |
| MySQL | true |
| Oracle | 0 |
| Postgres | true |
| SQL Server | 0 |
| SQLite | 0 |
| Sybase ASE | 0 |
| Sybase SQL Anywhere | 0 |

퀴즈 같은 내용이지만... 알아두면 좋다.

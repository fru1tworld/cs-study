# 미묘한 SQL 차이: 제약 조건 이름

> 원문: https://blog.jooq.org/subtle-sql-differences-constraint-names/

다양한 SQL 제품 벤더들은 SQL을 해석하는 방식에서 미묘한 차이를 보인다. 이번에는 스키마/데이터베이스 내에서 제약 조건 이름의 재사용에 대해 살펴보았다 (스키마가 무엇이고 데이터베이스가 무엇인지는 또 다른 이야기다). 다음은 그 요약이다:

제약 조건 이름이 스키마 내에서 고유해야 하는 경우

- Derby
- H2
- HSQLDB
- Ingres
- MySQL
- Oracle
- SQL Server

제약 조건 이름이 테이블 내에서 고유해야 하는 경우

- DB2
- SQLite
- Sybase SQL Anywhere

"이상한 것들"

- Postgres: 외래 키 이름은 재사용할 수 있다. 유니크/기본 키 이름은 재사용할 수 없다.
- Sybase ASE: 유니크/기본 키 이름은 재사용할 수 있다. 외래 키 이름은 재사용할 수 없다.

데이터베이스 간 최대한의 호환성을 위해, 이름을 재사용하는 것은 결코 좋은 생각이 아니다. 제약 조건 이름은 스키마 전체에서 고유하게 유지하라.

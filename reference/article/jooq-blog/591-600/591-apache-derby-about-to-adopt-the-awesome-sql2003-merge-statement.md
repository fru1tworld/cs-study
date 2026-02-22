# Apache Derby가 놀라운 SQL:2003 MERGE 문을 채택하려 한다

> 원문: https://blog.jooq.org/apache-derby-about-to-adopt-the-awesome-sql2003-merge-statement/

H2, HSQLDB와 함께 세 가지 인기 있는 Java 임베디드 데이터베이스 중 하나인 Apache Derby가 SQL:2003 MERGE 문을 채택하려 하고 있습니다. 티켓 DERBY-3155가 열린 지 약 6년 만에, Rick Hillegas가 드래프트 명세를 첨부했습니다.

이것이 구현되면, jOOQ가 지원하는 데이터베이스 중 SQL:2003 MERGE 문을 지원하는 8번째 데이터베이스가 됩니다. 현재 지원하는 데이터베이스는 다음과 같습니다:

- CUBRID
- DB2
- Firebird
- HSQLDB
- Oracle
- SQL Server
- Sybase SQL Anywhere

다른 데이터베이스들은 고유한 방식을 사용합니다. H2는 "MERGE .. KEY 구문"을 사용하고, MySQL과 MariaDB는 "INSERT .. ON DUPLICATE KEY UPDATE"를 사용합니다.

DERBY-3155를 방문하여 이 놀랍고 강력한 SQL 문을 구현하는 메인테이너들에게 응원을 보내주세요! SQL:2003 MERGE 문으로 할 수 있는 신비로운 마법에 대해 더 알아보세요.

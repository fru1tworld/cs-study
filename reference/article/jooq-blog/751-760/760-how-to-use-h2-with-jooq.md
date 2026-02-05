# H2와 jOOQ를 사용하는 방법

> 원문: https://blog.jooq.org/how-to-use-h2-with-jooq/

H2와 HSQLDB 데이터베이스의 최신 발전 과정을 지켜보는 것은 jOOQ 개발자인 나에게 매우 흥미로운 경험이었다. 이 두 데이터베이스는 공통된 유산을 가지고 있으며 많은 기능을 공유한다. 두 데이터베이스 모두 각각의 리드 개발자를 중심으로 매우 활발한 커뮤니티와 함께 빠른 속도로 발전하고 있다. 나는 최근 이 두 데이터베이스가 서로 비슷한 방식으로 변수 바인딩을 처리하는 방법에 대한 글을 게시한 바 있다. 두 데이터베이스 모두 컴파일 시점에는 많은 타입을 추론하지만, 바인드/실행 시점에는 소수의 타입만 추론한다:

https://blog.jooq.org/rdbms-bind-variable-casting-madness

이러한 세부 사항들이 대형 데이터베이스(주로 DB2, Oracle, Postgres, SQL Server, Sybase)에 비해 사소한 기능 부족처럼 보일 수 있지만, 이 두 데이터베이스는 속도와 유연성 면에서 충분히 만회한다. H2와 HSQLDB 모두 "대형" 데이터베이스의 함수, 구문 절 및 기타 특수 기능을 광범위하게 모방하며, 이는 통합 테스트 시스템이나 개발 환경에서 테스트 데이터베이스로 쉽게 사용할 수 있다는 것을 의미한다. 이는 주로 다음을 모방하는 경우에 해당된다:

- MySQL
- Ingres
- Oracle
- Postgres

하지만 다음의 경우에는 다소 덜 적합하다:

- DB2 (타입 시스템이 아마 너무 강력해서 모방하기 어려움)
- SQL Server (T-SQL이 SQL-92와 약간 다름)
- Sybase SQL Anywhere (역시 T-SQL 문제...)

jOOQ의 통합 테스트를 실행할 때, H2와 HSQLDB가 임베드 가능하고 고성능인 Java 데이터베이스라는 사실이 정말 마음에 든다. 가까운 미래에, 나는 다음 도구들의 조합으로 완전한 기능을 갖춘 바로 실행 가능한 통합 솔루션을 출시하고 싶다:

- Play! Framework, Wicket, Vaadin을 GUI 계층으로
- jOOQ를 중간 계층으로
- H2 / HSQLDB를 데이터 계층으로

그때까지, jOOQ를 H2와 함께 사용하는 방법에 대한 H2 튜토리얼 섹션을 보게 되어 자랑스럽다:

http://www.h2database.com/html/tutorial.html#using_jooq

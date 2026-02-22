# jOOQ 사용자들이 가장 많이 사용하는 데이터베이스

> 원문: https://blog.jooq.org/jooq-users-most-frequently-used-databases/

2012년 4월 21일 Lukas Eder 작성

저는 jOOQ 사용자들에게 어떤 데이터베이스를 선호하는지 물어보는 설문조사를 진행했습니다. 단순히 결과만 나열하는 대신, jOOQ 라이브러리의 OLAP 기능을 사용하여 설문 데이터를 분석함으로써 jOOQ의 분석 기능을 시연했습니다.

## 설문조사 쿼리

다음은 jOOQ를 사용하여 득표수별로 데이터베이스 순위를 매기는 Java 코드 예제입니다:

```java
System.out.println(
create.select(
         denseRank().over().orderBy(POLL.VOTES.desc()),
         POLL.VOTES
             .mul(100)
             .div(sum(POLL.VOTES).over())
             .concat(" %")
             .lpad(4, ' ').as("percent"),
         POLL.DIALECT)
      .from(POLL)
      .orderBy(POLL.VOTES.desc())
      .fetch());
```

## 설문조사 결과 (총 40표)

순위 결과는 다음과 같습니다:

- MySQL과 Oracle이 각각 22%로 공동 1위
- PostgreSQL과 H2가 각각 15%로 공동 2위
- SQL Server가 10%로 3위
- HSQLDB가 7%로 4위
- DB2, Derby, 기타가 각각 2%로 공동 5위
- SQLite, Ingres, Sybase SQL Anywhere, Sybase ASE, CUBRID가 0%로 공동 6위

이 글은 jOOQ의 유창한(fluent) API를 통해 SQL의 윈도우 함수와 분석 기능을 실용적으로 사용하는 방법을 보여줍니다.

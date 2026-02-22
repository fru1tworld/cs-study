# CQLC 소개 - jOOQ에서 영감을 받은 Go용 Cassandra CQL 쿼리 DSL

> 원문: https://blog.jooq.org/introducing-cqlc-a-query-dsl-for-cassandras-cql-in-go-inspired-by-jooq/

게시일: 2014년 1월 27일, Lukas Eder

우리의 오랜 jOOQ 사용자이자 기여자인 Ben Hood가 Go 언어를 위한 Cassandra의 CQL용 유창한(fluent) API이자 쿼리 DSL인 CQLC를 만들었습니다.

## Hood의 동기

Hood는 다음과 같이 말했습니다:

> JVM에서 SQL 데이터베이스 작업을 할 때 jOOQ가 매우 유용하다는 것을 알게 되었고, Go로 Cassandra 개발을 할 때도 비슷한 기능을 원했습니다.

CQLC는 Cassandra 스키마에서 Go 코드를 생성하여, 자연스러운 쿼리 구문으로 타입 안전한 CQL 문을 작성할 수 있게 해주면서 보일러플레이트 코드를 줄여줍니다.

## 프로젝트 정보

- 프로젝트 사이트: http://relops.com/cqlc/
- 동기를 설명하는 블로그 포스트: http://relops.com/blog/2014/01/25/cqlc/

## jOOQ의 관점

jOOQ 팀은 Cassandra의 CQL을 연구한 적이 있지만, 공식적인 jOOQ 지원은 현실적이지 않다는 결론을 내렸습니다. jOOQ API의 90%가 CQL에서는 작동하지 않을 것이며, 이는 JCR-SQL2나 CMIS SQL 지원에서 겪었던 어려움과 유사합니다.

## 결론

Cassandra를 사용하는 Go 개발자라면, 쿼리 언어 추상화에 대한 jOOQ의 실용적인 접근 방식에서 영감을 받은 솔루션인 CQLC를 살펴보시기 바랍니다.

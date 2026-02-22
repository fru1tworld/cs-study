# QueryDSL vs. jOOQ: 기능 완성도 vs. 그 어느 때보다

> 원문: https://blog.jooq.org/querydsl-vs-jooq-feature-completeness-vs-now-more-than-ever/

이번 주에 QueryDSL의 Timo Westkamper가 QueryDSL 사용자 그룹에서 기능 완성도를 발표했습니다. 그와 함께 기여에 대한 요청과 버그 수정 및 문서화에 대한 집중도를 높이겠다고 밝혔습니다.

Timo와 jOOQ 팀은 항상 긴밀하게 연락하며 서로의 제품을 관찰해 왔습니다. 2009년 jOOQ 초창기에는 QueryDSL이 앞서 있었습니다. 하지만 jOOQ는 빠르게 학습하고 단점들을 개선하여 2011년에는 두 도구가 대등한 수준에 이르렀습니다. 그 이후로 두 도구는 비슷한 목표를 가지고 서로에게 영감을 주었습니다.

Richard Warburton이 2014년 5월 26일에 다음과 같이 말했습니다:

> "QueryDSL과 jOOQ는 자신만의 매핑을 제어하고 싶을 때 인기 있는 선택지인 것 같습니다."

QueryDSL은 JPA 기반 환경에서 종종 좋은 선택이며, jOOQ는 SQL 기반 환경에서 대부분 최선의 선택입니다. 물론 jOOQ도 JPA 환경에서 인정을 받고 있습니다.

## jOOQ는 기능 완성과는 거리가 멉니다

그리고 사실 저희는 그렇게 되기를 원하지 않습니다. jOOQ는 처음부터 SQLJ가 되었어야 했던 것입니다. 우리의 비전은 SQL이 Java에서 실행될 수 있는 최고의 방법을 제공하는 것입니다. SQL을 문자열에 넣는 것, SQLJ를 사용하는 것, 심지어 저장 프로시저로 래핑하는 것까지... 이러한 방법들로는 SQL을 진정으로 수용할 수 없습니다. 왜냐하면 Java와 SQL은 아마도 세계에서 가장 많은 개발자가 사용하는 두 가지 플랫폼이기 때문입니다.

jOOQ는 이미 다음을 지원합니다:

- 테이블 값 함수(Table-valued functions)
- PIVOT 테이블
- DDL (jOOQ 3.4부터)
- MERGE 문
- 파생 테이블과 파생 열 목록(Derived tables and derived column lists)
- 행 값 표현식(Row value expressions)
- 플래시백 쿼리(Flashback query)
- 윈도우 함수(Window functions)
- 정렬된 집계 함수(Ordered aggregate functions)
- 공통 테이블 표현식(Common table expressions, jOOQ 3.4부터)
- 객체 지향 PL/SQL
- 사용자 정의 타입(User-defined types)
- 계층적 SQL(Hierarchical SQL)
- 사용자 정의 SQL 변환(Custom SQL transformation)
- 16개 RDBMS 지원 (MS Access 포함)

jOOQ가 기능 완성에 도달했다고 하려면, Java 컴파일러가 실제 SQL 코드를 네이티브하게 컴파일하여 jOOQ의 AST 모델을 통해 변환할 수 있어야 할 것입니다. 그래서 저희는 "그 어느 때보다"라고 말하는 것이 더 낫습니다. jOOQ 팀은 항상 여러분의 SQL 요구사항을 더 잘 충족시키기 위해 노력하고 있습니다.

Timo의 QueryDSL 성과를 진심으로 축하드립니다!

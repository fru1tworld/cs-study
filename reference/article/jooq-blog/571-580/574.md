# jOOQ 뉴스레터 2013년 9월 17일

> 원문: https://blog.jooq.org/jooq-newsletter-september-17-2013/

## 계산을 위한 SQL

이번 뉴스레터에서는 비즈니스 계산을 수행할 때 SQL과 Java 중 어느 것이 더 나은지에 대해 논의합니다. 한 가지 접근 방식이 우월하다고 선언하기보다는, 저자는 실용적인 입장을 취합니다: "어느 접근 방식이 '더 낫다'고 할 수 없습니다." 이 글에서는 많은 개발자들이 강력한 데이터베이스별 SQL 기능을 놓치고 있다는 점을 강조합니다.

누적 합계를 계산하기 위한 두 가지 Oracle SQL 예제가 제공됩니다:
- Oracle의 MODEL 절 사용
- SQL:2003 윈도우 함수 사용

저자는 독자들에게 데이터베이스 기반 계산에 대한 경험을 공유해 달라고 요청합니다.

## SQL Performance Explained

Markus Winand가 저술한 "SQL Performance Explained" 책을 추천합니다. 이 책은 필수적인 SQL 성능 최적화 개념을 "매우 간단한 용어로" 다룹니다. 저자는 이 책이 "모든 SQL 개발자가 알아야 할 내용의 90%"를 다룬다고 설명하며, 초보자와 숙련된 개발자 모두에게 추천합니다. 이 자료는 영어, 독일어, 프랑스어로 제공됩니다.

## PostgreSQL 9.3 릴리스

PostgreSQL 9.3이 새로운 기능들과 함께 출시되었습니다. 여기에는 구체화된 뷰(materialized views), 업데이트 가능한 뷰(updatable views), 그리고 SQL 표준 LATERAL JOIN 지원이 포함됩니다. 저자는 jOOQ가 곧 이 조인 유형을 지원할 것이라고 언급합니다.

## Google과 MariaDB 마이그레이션

RedHat이 MySQL에서 MariaDB로 전환한 이후, Google도 Oracle의 MySQL에서 마이그레이션할 계획을 발표했습니다. 주요 기업들의 이러한 움직임은 기여자들이 Oracle의 MySQL에서 이탈함에 따라 MariaDB의 커뮤니티와 오픈소스 데이터베이스 생태계에서의 위치를 강화할 것으로 예상됩니다.

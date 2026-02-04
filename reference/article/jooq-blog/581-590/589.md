# 느린 SQL JOIN 연산에 대한 신화

> 원문: https://blog.jooq.org/the-myth-about-slow-sql-join-operations/

최근 대형 스위스 은행을 위한 SQL 작업에서, 저는 중첩된 데이터베이스 뷰 괴물들을 관리해왔습니다. 이 뷰들의 중첩을 풀면 SQL 코드가 최대 5,000줄에 달했고, UNION 연산으로 결합된 별도의 서브셀렉트들에서 동일한 테이블을 반복해서 조인하고 있었습니다. 이 괴물 같은 쿼리는 어떻게 쿼리하든 상관없이 50ms 훨씬 이내로 수행되었습니다(쿼리 속도에 대해서는 ["10가지 더 흔한 실수"](https://blog.jooq.org/10-more-common-mistakes-java-developers-make-when-writing-sql/)를 참조하세요). 물론 이 성능은 많은 미세 조정, 부하 테스트, 벤치마킹 후에야 달성되었습니다. 하지만 작동했습니다. 우리의 Oracle 데이터베이스는 이런 것들에서 결코 우리를 실망시키지 않았습니다.

그럼에도 불구하고, 많은 SQL 사용자들은 JOIN 연산이 느리다고 생각합니다. 왜일까요? 아마도 MySQL에서 느렸거나/느렸기 때문일까요? 저는 현재 [Markus Winand](http://winand.at/)가 쓴 흥미로운 책을 읽고 있습니다. 이 책의 제목은 [SQL Performance Explained](http://sql-performance-explained.com/)입니다. 그는 또한 [Use-The-Index-Luke.com](http://use-the-index-luke.com)의 저자이기도 한데, 여기서 그의 책에 대한 무료 인사이트를 얻을 수 있습니다. 그래도 전체 책을 읽어보시길 권장합니다. 저 같은 SQL 고참이나 SQL 덕후조차도 1~2가지 새롭고 매우 흥미로운 접근 방식을 발견할 수 있을 것이며, 그중 일부는 곧 [jOOQ](https://www.jooq.org)에 통합될 예정입니다!

특히, Hash JOIN 연산이 어떻게 작동하는지 매우 잘 설명하는 다음 페이지를 참고하세요:
http://use-the-index-luke.com/sql/join/hash-join-partial-objects

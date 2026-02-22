# MyBatis의 대안 트랜잭션 관리

> 원문: https://blog.jooq.org/mybatis-alternative-transaction-management/

jOOQ 사용자 커뮤니티에서 트랜잭션 관리 접근 방식에 대해 자주 문의합니다. 답변은 간단합니다: 트랜잭션 처리는 jOOQ의 책임이 아닙니다. 대신, 개발자들은 다음을 포함한 선호하는 트랜잭션 API를 선택해야 합니다:

- JDBC
- Spring
- JEE JTA (WebLogic에서 지원)
- Bitronix TM
- Hibernate

이 목록은 완전하지 않습니다. 트랜잭션 관리는 복잡하며, 주요 초점이 다른 라이브러리에 의해 강제되어서는 안 됩니다. 그러한 라이브러리들은 일반적으로 "트랜잭션 모델의 매우 누수가 있는 추상화"를 제공하기 때문입니다. 표준 접근 방식에서 약간만 벗어나도 상당한 복잡성이 발생합니다.

MyBatis에 대하여:

MyBatis는 Velocity나 StringTemplate 같은 기본 템플릿 시스템 이상의 기능을 제공하는 SQL 템플릿 엔진으로 작동합니다. 트랜잭션 관리가 그러한 기능 중 하나입니다. 문서에서는 MyBatis 트랜잭션 관리자가 Spring으로 재정의될 수 있다고 제안하지만, 구현 과정은 불명확합니다.

MyBatis는 커넥션 풀링과 매핑을 다루는데, 이는 c3p0, DBCP, Spring의 JdbcTemplate, jOOQ의 RecordMapper 같은 대안이 있는 문제들입니다. 이 프레임워크는 핵심 SQL 템플릿 범위 외의 문제를 해결하려 시도하여, 복잡한 모델에 대한 잠재적인 종속성(lock-in) 상황을 만듭니다. 이 트랜잭션 관리 접근 방식이 MyBatis 사용자들에게 도움이 되었는지 의문입니다.

---

## 댓글 섹션

댓글 1 - vladmihalcea (2013년 12월 22일 20:16)

이 결정에 동의하며, Bitronix가 데이터베이스와 JMS 데이터소스 전반에서 신뢰성이 있다고 칭찬합니다. 파일 기반 트랜잭션을 위한 JCA 어댑터 구현을 언급하며, XA 명세 탐색을 "고통"이라고 설명합니다. 프레임워크 조합을 옹호합니다: "slf4j는 로깅을 하고, spring core는 DI를 하고, JOOQ는 SQL 쿼리를 일상용품으로 만듭니다."

답글 - lukaseder (2013년 12월 23일 12:01)

트랜잭션 관리자와 모델에 대한 곧 나올 기사를 기대한다고 표현합니다.

답글 - vladmihalcea (2013년 12월 24일 12:07)

기사를 연기하겠다고 밝히며, 광범위한 트랜잭션 주제가 존재한다고 언급합니다 (데이터베이스, 비즈니스, JDBC 커넥션, ORM 요구사항, Spring 트랜잭션, JEE, JMS). 이로 인해 사람들이 트랜잭션 문제에 대해 혼란스러워합니다.

답글 - lukaseder (2013년 12월 24일 14:24)

이 주제는 여러 포스트의 가치가 있으며, 잠재적으로 "트랜잭션에 대해 사람들이 오해하는 Top 10"과 같은 글이 될 수 있다고 제안합니다.

댓글 2 - Venkat (2013년 12월 23일 02:53)

사용자들이 다양한 트랜잭션 프레임워크와의 통합 예제를 찾고 있으며, 최대한의 제어를 제공하는 커스텀 프레임워크를 선호한다고 관찰합니다.

답글 - lukaseder (2013년 12월 23일 12:02)

동의하며, jOOQ가 Spring 예제를 개선하고 잠재적으로 WLS나 다른 JEE 컨테이너에서 JTA 통합을 시연할 것이라고 언급합니다.

댓글 3 - Stéphane Cl. (2013년 12월 28일 10:26)

이 문제가 흑백 논리가 아니라고 주장합니다. MyBatis는 Threadlocal 기반 트랜잭션 관리를 제공합니다. 이전 MyBatis 사용자로서, 피할 수 없는 데이터베이스 애플리케이션 주제에 대한 API를 높이 평가했습니다. JDBC의 장황한 try-commit-catch-rollback-close 패턴과 MyBatis의 단순함을 대조합니다. MyBatis가 "부두교 마법" 없이 간단한 솔루션을 제공했다고 주장하며, Joel Spolsky의 과도하게 복잡한 시스템에 대한 기사를 참조합니다.

답글 - lukaseder (2013년 12월 28일 10:50)

그 의견들을 인정하며, jOOQ 3.4가 의미론을 재정의하기 위한 서비스 제공자 인터페이스와 함께 간단한 트랜잭션 API를 구현할 것이라고 언급합니다. 기본 SPI 구현은 호출을 JDBC로 전달하며, Spring-TX, JTA, Guice를 지원합니다. MyBatis가 "망치-톱-수준기-못 상자"로서 "망치"를 제공한다고 제안하며, MyBatis의 기본값은 쉽게 재정의되지 않는다고 언급합니다.

답글 - Stéphane (2013년 12월 28일 12:35)

MyBatis의 `public SqlSessionManager openSession (Connection connection)` 메서드를 설명하며, jOOQ의 Factory 패턴과 비교합니다. SqlSession 객체가 매핑 파일을 읽고, 리플렉션으로 얻은 데이터를 캐시한다고 언급합니다. MyBatis가 순수 JDBC와 큰 ORM 사이에 제한된 옵션이 있던 시절에 iBatis에서 발전했다는 것을 기억합니다.

답글 - lukaseder (2013년 12월 28일 12:39)

통찰에 감사하며, 특히 OSGi와 복잡한 클래스 로더에 관한 리플렉션 캐싱 솔루션에 대해 질문합니다. jOOQ가 Java 5 제네릭과 함께 8년 전에 존재했으면 좋았을 것이라고 농담합니다.

댓글 4 - emacarron (2014년 4월 26일 09:12)

MyBatis의 Eduardo가 MyBatis는 트랜잭션 관리를 제공하지 않는다고 명확히 합니다 - JDBC를 간단한 API로 감쌀 뿐입니다. SqlSession은 커넥션, 캐시된 데이터, 배치를 포함합니다. SqlSession#commit()은 단순히 JDBC에 위임합니다. MyBatis는 매퍼 인터페이스 매핑을 지원하여 보이는 API를 제거합니다. iBATIS 2.x 트랜잭션 처리에 기반한 SqlSessionManager는 레거시 코드 이식성을 위해 MyBatis 3에 남아 있지만 문서화되어 있지 않습니다 (하지만 제거된 적도 없습니다).

답글 - lukaseder (2014년 4월 26일 15:58)

Eduardo에게 감사하며, jOOQ 3.4가 의미론 재정의를 허용하는 서비스 제공자 인터페이스와 함께 간단한 트랜잭션 API를 구현할 것이라고 언급합니다. 기본 구현은 호출을 JDBC로 전달하며, Spring-TX, JTA, Guice 등을 지원합니다. MyBatis의 구현을 검토하겠다고 밝힙니다.

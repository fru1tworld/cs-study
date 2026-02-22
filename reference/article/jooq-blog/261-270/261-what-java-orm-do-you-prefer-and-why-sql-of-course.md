# "어떤 Java ORM을 선호하시나요, 그리고 왜?" - 물론 SQL!

> 원문: https://blog.jooq.org/what-java-orm-do-you-prefer-and-why-sql-of-course/

Stack Overflow에 올라온 흥미로운 질문이 있습니다. 사용자 Mike가 새 프로젝트를 위한 Java ORM 선택에 대해 물었습니다:

> ["어떤 Java ORM을 선호하시나요, 그리고 왜?"](https://stackoverflow.com/questions/452385/what-java-orm-do-you-prefer-and-why)

이 질문에 대한 가장 많은 추천을 받은 상위 4개의 답변 중 3개가 흥미롭게도 ORM을 사용하지 말라고 권장하고 있습니다!

## 최다 추천 답변 (164 포인트) - David Crawshaw

> "저는 ORM 사용을 중단했습니다.
>
> 그 이유는 개념 자체의 큰 결함 때문이 아닙니다. Hibernate는 잘 작동합니다. 대신, 쿼리의 오버헤드가 낮고 복잡한 로직을 대용량 SQL 쿼리에 담을 수 있다는 것을 알게 되었습니다. 그리고 로직의 많은 부분을 데이터베이스로 옮김으로써 더 나은 성능을 얻을 수 있었습니다.
>
> 그래서 저는 이제 JDBC를 직접 사용하는 것을 권장합니다."

이 답변은 ORM의 개념적 결함이 아닌, SQL의 강력함과 효율성을 강조합니다. 복잡한 비즈니스 로직을 SQL로 직접 표현하면 성능상의 이점을 얻을 수 있다는 것입니다.

## 두 번째 답변 (66 포인트)

> "없습니다. ORM을 사용하면 너무 많은 제어권을 빼앗기고 그에 비해 얻는 이점은 적습니다...
>
> ORM 비정상 동작을 디버깅할 때 시간 절약 효과는 사라집니다...
>
> ORM은 개발자들이 SQL과 관계형 데이터베이스의 작동 방식을 배우는 것을 방해합니다. 그리고 그런 지식 없이는 진정한 문제 해결이 어렵습니다."

이 답변은 ORM이 개발자들의 SQL 학습을 저해한다는 중요한 문제점을 지적합니다. ORM의 편리함에 의존하다 보면 정작 데이터베이스의 근본적인 작동 원리를 이해하지 못하게 되고, 이는 복잡한 문제 상황에서 큰 장애물이 됩니다.

## 세 번째 답변 (51 포인트) - Lukas Eder

저 역시 SQL을 직접 사용할 것을 권장하며, 타입 안전한 SQL을 위해 [jOOQ](https://www.jooq.org/)를 사용하는 방법을 제안했습니다.

## 네 번째 답변 (46 포인트) - Abdullah Jibaly

상위 4개 답변 중 유일하게 Hibernate(가장 인기 있는 Java ORM)를 언급한 답변입니다.

## ORM이 잘하는 것과 못하는 것

ORM은 CRUD(Create, Read, Update, Delete) 작업의 보일러플레이트 코드를 제거하는 데 탁월합니다. 단순한 엔티티 매핑과 기본적인 데이터 조작에는 분명히 유용합니다.

하지만 다음과 같은 영역에서는 SQL이 훨씬 뛰어납니다:

- 복잡한 쿼리: 다중 조인, 서브쿼리, 윈도우 함수 등을 사용하는 복잡한 쿼리
- 배치 처리: 대량의 데이터를 효율적으로 처리해야 하는 경우
- 분석 및 리포팅: 집계, 그룹화, 피벗 등의 분석 작업
- 성능 최적화: 데이터베이스 고유의 최적화 기능 활용

## 관련 글

이 주제에 대해 더 자세히 알고 싶다면 다음 글들을 참고하세요:

- [JPA의 loadgraph와 fetchgraph 힌트 대신 SQL을 사용하세요](https://blog.jooq.org/turn-around-dont-use-jpas-loadgraph-and-fetchgraph-hints-use-sql-instead/)
- [Java 8: 더 이상 ORM이 필요 없습니다](https://blog.jooq.org/java-8-no-more-need-for-orms/)
- [JPA의 Native Query API를 위한 타입 안전 쿼리](https://blog.jooq.org/type-safe-queries-for-jpas-native-query-api/)
- [NoSQL? No, SQL! - 누적 합계 계산 방법](https://blog.jooq.org/nosql-no-sql-how-to-calculate-running-totals/)
- [가능하다고 생각하지 못했던 10가지 SQL 트릭](https://blog.jooq.org/10-sql-tricks-that-you-didnt-think-were-possible/)

## 함수형 프로그래밍과 선언적 접근의 시대

프로그래밍 패러다임이 점점 함수형(Functional)과 선언적(Declarative) 방식으로 전환되고 있습니다. Java 8의 Stream API, 람다 표현식 등이 대표적인 예입니다.

이러한 변화는 데이터 처리 방식에도 영향을 미칩니다. 객체 지향적인 ORM 추상화보다는 SQL의 선언적 특성이 이러한 패러다임과 더 잘 어울립니다. SQL은 본질적으로 "무엇(what)"을 원하는지 선언하면 데이터베이스가 "어떻게(how)" 처리할지 결정하는 선언적 언어입니다.

## 결론

물론 ORM이 모든 상황에서 나쁘다는 것은 아닙니다. Hibernate 창시자 Gavin King이 말했듯이, ORM 사용의 나쁜 결과는 종종 도구 자체의 한계보다는 개발팀의 데이터베이스 전문성 부족에서 비롯됩니다.

하지만 SQL을 직접 다루는 능력은 여전히 중요하며, 복잡한 데이터 액세스 문제를 해결할 때 ORM 추상화에만 의존하는 것보다 SQL 숙련도가 훨씬 더 가치 있습니다. SQL은 50년 가까이 검증된 기술이며, 관계형 데이터베이스와 함께 앞으로도 오랫동안 핵심 기술로 남을 것입니다.

당신은 어떤 Java ORM을 선호하시나요? 아마도... SQL일지도 모르겠습니다!

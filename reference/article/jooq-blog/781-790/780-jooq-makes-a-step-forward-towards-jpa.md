# jOOQ가 JPA를 향해 한 걸음 나아가다

> 원문: https://blog.jooq.org/jooq-makes-a-step-forward-towards-jpa/

2011년 8월 1일 lukaseder 작성

곧 출시될 릴리스 1.6.4와 [#198](https://github.com/jOOQ/jOOQ/issues/198)의 구현으로, jOOQ는 "표준" J2EE 아키텍처에 더 잘 통합되기 시작할 것입니다. jOOQ는 JPA의 대안으로 개발되고 있지만, JPA와 경쟁해서는 안 됩니다. 이는 J2EE 애플리케이션에서 jOOQ와 쉽게 공존할 수 있는 JPA의 일부 요소가 있다는 것을 의미합니다:

- EntityManager와 어노테이션된 엔티티의 영속성 관리
- 분산 애플리케이션에서의 2차 캐시
- EJB 3.0 스펙을 사용한 트랜잭션 처리

이 목적을 위해, jOOQ는 org.jooq.Result의 페치를 JPA 어노테이션된 엔티티와 인터페이스할 것입니다. 이 인터페이스는 릴리스 1.6.4부터 다음 타입들에서 접근할 수 있습니다:

```java
public interface ResultQuery {
    // [...]
    // 결과를 사용자 정의 어노테이션된 엔티티 리스트로 직접 페치
    <E> List<E> fetchInto(Class<? extends E> type) throws SQLException;
}

public interface Result {
    // [...]
    // 기존 결과를 사용자 정의 어노테이션된 엔티티 리스트로 복사
    <E> List<E> into(Class<? extends E> type);
}

public interface Record {
    // [...]
    // 단일 레코드를 사용자 정의 어노테이션된 엔티티로 변환
    <E> E into(Class<? extends E> type);
}
```

위의 API 확장은 JPA 어노테이션된 타입이 얼마나 쉽게 jOOQ에 통합될 수 있는지를 보여줍니다. 이 메서드들을 사용하면, 개발자들은 jOOQ의 플루언트 API를 사용하여 복잡한 쿼리를 실행한 다음, JPA 구현체를 통해 수정하고 영속화할 수 있는 POJO를 사용하여 데이터를 처리할 수 있습니다. 중요한 것은, POJO가 jOOQ 자체에 대한 의존성이 없다는 것입니다. 이는 어노테이션되지 않은 타입에서도 작동하며, jOOQ는 네이밍 컨벤션을 통해 Record 필드와 Class 멤버 및 메서드를 매칭합니다. 스냅샷 미리보기는 Sonatype Maven 저장소에서 사용할 수 있습니다.

# Spring Boot에서 jOOQ 상용 배포판을 사용하는 방법

> 원문: https://blog.jooq.org/how-to-use-jooqs-commercial-distributions-with-spring-boot/

작성자: lukaseder
작성일: 2019년 6월 26일 (2025년 3월 10일 업데이트)

Spring Boot가 훌륭한 이유 중 하나는 모든 것이 표준 start.spring.io 방식으로 "그냥 작동"한다는 것입니다. jOOQ를 포함해서요. jOOQ를 의존성으로 선택하기만 하면 됩니다!

하지만 jOOQ 상용 배포판을 사용하고 싶다면 어떻게 해야 할까요?

## 상용 배포판의 차이점

jOOQ 상용 배포판은 두 가지 면에서 오픈 소스 배포판과 다릅니다:

1. 저장소 위치: Maven Central에 호스팅되지 않고, jOOQ 웹사이트에서 다운로드한 후 프라이빗 저장소에 배치해야 합니다
2. 그룹 ID: 각 배포판을 쉽게 구분할 수 있도록 서로 다른 Maven `groupId`를 사용합니다

## 배포판별 Maven GroupId

다음은 사용 가능한 모든 groupId 목록입니다:

- `org.jooq` – 오픈 소스 에디션
- `org.jooq.trial` – 트라이얼 에디션
- `org.jooq.trial-java-8` – 트라이얼 에디션 (Java 8+)
- `org.jooq.trial-java-11` – 트라이얼 에디션 (Java 11+)
- `org.jooq.pro` – 익스프레스/프로페셔널/엔터프라이즈 에디션 (최신 JDK)
- `org.jooq.pro-java-8` – 프로페셔널/엔터프라이즈 에디션 (Java 8+)
- `org.jooq.pro-java-11` – 프로페셔널/엔터프라이즈 에디션 (Java 11+)

## Spring Boot 기본 설정

기본적으로 Spring Boot 스타터 설정에는 `spring-boot-starter-jooq`가 의존성으로 포함되어 있으며, 이는 자동으로 오픈 소스 jOOQ 에디션을 가져옵니다.

## 버전 오버라이드

jOOQ 버전은 `${jooq.version}` Maven 프로퍼티를 사용하여 제어할 수 있습니다:

```xml
<properties>
  <jooq.version>3.11.0</jooq.version>
</properties>
```

## 상용 배포판으로 전환하기

상용 배포판을 사용하려면 오픈 소스 에디션을 제외하고 원하는 상용 버전을 추가해야 합니다:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-jooq</artifactId>
  <exclusions>
    <exclusion>
      <groupId>org.jooq</groupId>
      <artifactId>jooq</artifactId>
    </exclusion>
  </exclusions>
</dependency>

<dependency>
  <groupId>org.jooq.trial</groupId>
  <artifactId>jooq</artifactId>
  <version>${jooq.version}</version>
</dependency>
```

위 예제에서는 트라이얼 에디션(`org.jooq.trial`)을 사용했지만, 보유하고 있는 라이선스에 따라 적절한 groupId로 변경하면 됩니다.

## 핵심 포인트

모든 jOOQ 배포판은 "소스 및 바이너리 호환성"을 유지하기 때문에, 에디션 간 전환은 의존성 수정만으로 가능합니다. 애플리케이션 코드는 변경할 필요가 없습니다.

이 방식은 Spring Boot의 자동 구성 기능을 그대로 활용하면서도 상용 jOOQ 기능을 사용할 수 있게 해줍니다.

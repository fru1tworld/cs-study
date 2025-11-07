# Spring / 스프링

> 카테고리: 프레임워크
> [← 면접 질문 목록으로 돌아가기](../interview.md)

---

## 📌 Java 기초 개념

### JAVA-041
JVM이 정확히 무엇이고, 어떤 기능을 하는지 설명해 주세요.

### JAVA-042
그럼, 자바 말고 다른 언어는 JVM 위에 올릴 수 없나요?

### JAVA-043
반대로 JVM 계열 언어를 일반적으로 컴파일해서 사용할 순 없나요?

### JAVA-044
VM을 사용함으로써 얻을 수 있는 장점과 단점에 대해 설명해 주세요.

### JAVA-045
JVM과 내부에서 실행되고 있는 프로그램은 부모 프로세스 - 자식 프로세스 관계를 갖고 있다고 봐도 무방한가요?

---

## 📌 Java 키워드와 객체지향

### JAVA-046
final 키워드를 사용하면, 어떤 이점이 있나요?

### JAVA-047
그렇다면 컴파일 과정에서, final 키워드는 다르게 취급되나요?

### JAVA-048
인터페이스와 추상 클래스의 차이에 대해 설명해 주세요.

### JAVA-049
왜 클래스는 단일 상속만 가능한데, 인터페이스는 2개 이상 구현이 가능할까요?

### JAVA-050
리플렉션에 대해 설명해 주세요.

### JAVA-051
의미만 들어보면 리플렉션은 보안적인 문제가 있을 가능성이 있어보이는데, 실제로 그렇게 생각하시나요? 만약 그렇다면, 어떻게 방지할 수 있을까요?

### JAVA-052
리플렉션을 언제 활용할 수 있을까요?

### JAVA-053
static class와 static method를 비교해 주세요.

### JAVA-054
static 을 사용하면 어떤 이점을 얻을 수 있나요? 어떤 제약이 걸릴까요?

### JAVA-055
컴파일 과정에서 static 이 어떻게 처리되는지 설명해 주세요.

---

## 📌 Java 예외 처리

### JAVA-056
Java의 Exception에 대해 설명해 주세요.

### JAVA-057
예외처리를 하는 세 방법에 대해 설명해 주세요.

### JAVA-058
CheckedException, UncheckedException 의 차이에 대해 설명해 주세요.

### JAVA-059
예외처리가 성능에 큰 영향을 미치나요? 만약 그렇다면, 어떻게 하면 부하를 줄일 수 있을까요?

---

## 📌 Java 동시성과 스레드

### JAVA-060
Synchronized 키워드에 대해 설명해 주세요.

### JAVA-061
Synchronized 키워드가 어디에 붙는지에 따라 의미가 약간씩 변화하는데, 각각 어떤 의미를 갖게 되는지 설명해 주세요.

### JAVA-062
효율적인 코드 작성 측면에서, Synchronized는 좋은 키워드일까요?

### JAVA-063
Synchronized 를 대체할 수 있는 자바의 다른 동기화 기법에 대해 설명해 주세요.

### JAVA-064
Thread Local에 대해 설명해 주세요.

---

## 📌 Java Stream과 함수형 프로그래밍

### JAVA-065
Java Stream에 대해 설명해 주세요.

### JAVA-066
Stream과 for ~ loop의 성능 차이를 비교해 주세요,

### JAVA-067
Stream은 병렬처리 할 수 있나요?

### JAVA-068
Stream에서 사용할 수 있는 함수형 인터페이스에 대해 설명해 주세요.

### JAVA-069
가끔 외부 변수를 사용할 때, final 키워드를 붙여서 사용하는데 왜 그럴까요? 꼭 그래야 할까요?

---

## 📌 Java 가비지 컬렉션

### JAVA-070
Java의 GC에 대해 설명해 주세요.

### JAVA-071
finalize() 를 수동으로 호출하는 것은 왜 문제가 될 수 있을까요?

### JAVA-072
어떤 변수의 값이 null이 되었다면, 이 값은 GC가 될 가능성이 있을까요?

---

## 📌 Java 메서드 오버라이딩

### JAVA-073
equals()와 hashcode()에 대해 설명해 주세요.

### JAVA-074
본인이 hashcode() 를 정의해야 한다면, 어떤 점을 염두에 두고 구현할 것 같으세요?

### JAVA-075
그렇다면 equals() 를 재정의 해야 할 때, 어떤 점을 염두에 두어야 하는지 설명해 주세요.

---

## 📌 Spring 핵심 개념

### SPRING-001
IoC와 DI에 대해 설명해 주세요.

### SPRING-002
후보 없이 특정 기능을 하는 클래스가 딱 한 개하면, 구체 클래스를 그냥 사용해도 되지 않나요? 그럼에도 불구하고 왜 Spring에선 Bean을 사용 할까요?

### SPRING-003
Spring의 Bean 생성 주기에 대해 설명해 주세요.

### SPRING-004
프로토타입 빈은 무엇인가요?

### SPRING-005
AOP에 대해 설명해 주세요.

### SPRING-006
@Aspect는 어떻게 동작하나요?

---

## 📌 Spring Web 계층

### SPRING-007
Spring 에서 Interceptor와 Servlet Filter에 대해 설명해 주세요.

### SPRING-008
설명만 들어보면 인터셉터만 쓰는게 나아보이는데, 아닌가요? 필터는 어떤 상황에 사용 해야 하나요?

### SPRING-009
DispatcherServlet 의 역할에 대해 설명해 주세요.

### SPRING-010
요청이 들어온다고 가정할 때, DispatcherServlet은 한번에 여러 요청을 모두 받을 수 있나요?

### SPRING-011
@Controller 를 DispatcherServlet은 어떻게 구분 할까요?

---

## 📌 Spring JPA와 ORM

### SPRING-012
JPA와 같은 ORM을 사용하는 이유가 무엇인가요?

### SPRING-013
영속성은 어떤 기능을 하나요? 이게 진짜 성능 향상에 큰 도움이 되나요?

### SPRING-014
N + 1 문제에 대해 설명해 주세요.

### SPRING-015
@Transactional 은 어떤 기능을 하나요?

### SPRING-016
@Transactional(readonly=true) 는 어떤 기능인가요? 이게 도움이 되나요?

### SPRING-017
그런데, 읽기에 트랜잭션을 걸 필요가 있나요? @Transactional을 안 붙이면 되는거 아닐까요?

---

## 📌 Spring 어노테이션

### SPRING-018
Java 에서 Annotation 은 어떤 기능을 하나요?

### SPRING-019
별 기능이 없는 것 같은데, 어떻게 Spring 에서는 Annotation 이 그렇게 많은 기능을 하는 걸까요?

### SPRING-020
Lombok의 @Data를 잘 사용하지 않는 이유는 무엇일까요?

---

## 📌 서버와 네트워크

### SPRING-021
Tomcat이 정확히 어떤 역할을 하는 도구인가요?

### SPRING-022
혹시 Netty에 대해 들어보셨나요? 왜 이런 것을 사용할까요?

---

## 📌 Spring Framework 기초

### SPRING-023
Spring Framework의 기본 개념과 주요 특징에 대해 설명해주세요.

### SPRING-024
Spring Boot와 전통적 Spring Framework의 차이점은 무엇인가요?

### SPRING-025
IoC(Inversion of Control)와 DI(Dependency Injection)의 개념 및 이점에 대해 설명해주세요.

### SPRING-026
Spring Bean의 라이프사이클과 관련 콜백 메서드에 대해 설명해주세요.

### SPRING-027
@Component, @Service, @Repository의 차이점 및 사용 사례는 무엇인가요?

### SPRING-028
AOP(Aspect Oriented Programming)를 활용한 공통 관심사 분리 방법에 대해 설명해주세요.

### SPRING-029
Spring에서 트랜잭션 관리와 @Transactional 어노테이션의 역할에 대해 설명해주세요.

---

## 📌 Spring MVC와 웹 개발

### SPRING-030
Spring MVC 아키텍처의 구성 요소와 요청 처리 과정을 설명해주세요.

### SPRING-031
Spring Boot의 자동 구성(Auto-Configuration) 원리에 대해 설명해주세요.

### SPRING-032
예외 처리를 위한 @ControllerAdvice의 역할과 활용 방법은 무엇인가요?

### SPRING-033
Spring Security의 기본 개념과 인증/인가 처리 흐름에 대해 설명해주세요.

### SPRING-034
RESTful API를 Spring에서 구현하는 방법과 모범 사례는 무엇인가요?

---

## 📌 Spring Boot 운영과 모니터링

### SPRING-035
Spring Boot Actuator를 통한 애플리케이션 모니터링 방법은 무엇인가요?

### SPRING-036
Spring Cloud를 활용한 마이크로서비스 아키텍처 구현 전략에 대해 설명해주세요.

### SPRING-037
Spring에서 메시징 시스템(Kafka, RabbitMQ 등)과의 연동 방법은 무엇인가요?

### SPRING-038
Spring의 캐싱 추상화(Cache Abstraction)와 캐시 적용 방법에 대해 설명해주세요.

### SPRING-039
Spring Boot에서 프로파일 관리와 환경별 설정 적용 방법은 무엇인가요?

---

## 📌 Spring Bean 고급

### SPRING-040
Spring Bean의 Scope(싱글톤, 프로토타입 등) 차이점과 활용 사례는 무엇인가요?

### SPRING-041
Spring의 이벤트 발행 및 리스너(Event Listener) 메커니즘에 대해 설명해주세요.

### SPRING-042
커스텀 어노테이션을 생성하고 이를 Spring에서 활용하는 방법은 무엇인가요?

---

## 📌 Spring WebFlux와 비동기

### SPRING-043
Spring WebFlux와 Spring MVC의 차이점 및 사용 시나리오는 무엇인가요?

### SPRING-044
Spring에서 비동기 처리(Asynchronous Processing)를 구현하는 방법에 대해 설명해주세요.

---

## 📌 Spring 로깅과 메시지 변환

### SPRING-045
Logback을 이용한 Spring Boot의 로깅 설정과 관리 방법은 무엇인가요?

### SPRING-046
HttpMessageConverter의 역할과 Spring에서의 메시지 변환 과정을 설명해주세요.

### SPRING-047
RestTemplate과 WebClient의 차이점 및 사용 사례에 대해 설명해주세요.

---

## 📌 Spring 스케줄링과 Starter

### SPRING-048
@Scheduled 애노테이션을 사용한 스케줄링 작업 구현 방법은 무엇인가요?

### SPRING-049
Spring Boot Starter의 개념과 주요 Starter들의 역할에 대해 설명해주세요.

### SPRING-050
Java Config와 XML Config를 통한 Bean 등록 및 설정 방식의 차이점은 무엇인가요?

---

## 📌 Spring 최신 트렌드

### SPRING-051
최신 Spring 버전에서 추가된 기능 및 개선 사항에 대해 설명해주세요.

# Akka 디스커버리와 유틸리티

## Akka 디스커버리와 유틸리티

> 원본: https://doc.akka.io/libraries/akka-core/current/

---

### 목차

1. [디스커버리(Discovery)](#1-디스커버리discovery)
2. [이벤트 스트림(Event Stream)](#2-이벤트-스트림event-stream)
3. [로깅(Logging)](#3-로깅logging)
4. [서킷 브레이커(Circuit Breaker)](#4-서킷-브레이커circuit-breaker)
5. [퓨처 패턴(Futures Patterns)](#5-퓨처-패턴futures-patterns)
6. [코디네이션과 리스(Coordination & Lease)](#6-코디네이션과-리스coordination--lease)
7. [Akka 확장하기(Extending Akka)](#7-akka-확장하기extending-akka)
8. [참고 자료](#참고-자료)

---

### 1. 디스커버리(Discovery)

#### 1.1 목적과 범위

- Akka 디스커버리(Akka Discovery) API: 다양한 기술을 통해 서비스 디스커버리(service discovery) 제공
- 엔드포인트(endpoint) 조회(lookup) 기능을 추상화 → 설정 파일에 값을 하드코딩하는 대신 실행 환경에 따라 서비스 구성 가능

#### 1.2 사용 가능한 디스커버리 방식

- 내장(built-in) 구현체
  - 설정(Configuration) - HOCON 기반
  - DNS - SRV 레코드 기반
  - 애그리게이트(Aggregate) - 여러 방식을 조합
- Akka 매니지먼트(Akka Management)를 통해 제공되는 방식
  - 쿠버네티스 API(Kubernetes API)
  - AWS EC2 태그 기반 디스커버리(AWS EC2 Tag-Based Discovery)
  - AWS ECS 디스커버리(AWS ECS Discovery)
  - 콘설(Consul)

#### 1.3 의존성(Dependency) 설정

- sbt

```
val AkkaVersion = "2.10.19"
libraryDependencies += "com.typesafe.akka" %% "akka-discovery" % AkkaVersion
```

- Maven
  - BOM 임포트와 함께 `akka-discovery` 아티팩트 사용
  - Scala 바이너리 버전: 2.13
- Gradle

```
implementation platform("com.typesafe.akka:akka-bom_${versions.ScalaBinary}:2.10.19")
implementation "akka-discovery_${versions.ScalaBinary}"
```

#### 1.4 핵심 API 사용법

확장(extension) 로드 및 조회 수행 방법.

- Scala

```scala
import akka.discovery.Discovery
val system = ActorSystem()
val serviceDiscovery = Discovery(system).discovery
```

- Java

```java
ActorSystem as = ActorSystem.create();
ServiceDiscovery serviceDiscovery = Discovery.get(as).discovery();
```

조회 파라미터(세 개, 선택적):

- `serviceName` (필수)
- `portName`
- `protocol`

#### 1.5 DNS 디스커버리 방식

`application.conf` 설정.

```
akka {
  discovery {
    method = akka-dns
  }
}
```

- 쿼리(query) 매핑
  - 세 필드(`serviceName`, `portName`, `protocol`)가 모두 존재 → SRV 쿼리(`_port._protocol.name`) 수행
  - 일부 필드 누락 → A/AAAA 쿼리 수행
- DNS 레코드 타입
  - SRV 레코드: 호스트명(hostname)과 포트(port) 정보를 우선순위(priority) 및 가중치(weight) 값과 함께 포함
  - A/AAAA 레코드: 포트 정보 없이 IP 주소로 해석

#### 1.6 설정(Configuration) 디스커버리 방식

`application.conf` 설정.

```
akka {
  discovery.method = config
}
```

서비스는 `akka.discovery.config.services` 아래에 정의.

```
akka.discovery.config.services = {
  service1 = {
    endpoints = [
      { host = "cat", port = 1233 },
      { host = "dog", port = 1234 }
    ]
  },
  service2 = {
    endpoints = []
  }
}
```

#### 1.7 애그리게이트(Aggregate) 디스커버리 방식

여러 디스커버리 방식을 폴백(fallback) 로직과 함께 조합.

```
akka {
  discovery {
    method = aggregate
    aggregate {
      discovery-methods = ["akka-dns", "config"]
    }
    config {
      services {
        service1 {
          endpoints = [
            { host = "host1", port = 1233 },
            { host = "host2", port = 1234 }
          ]
        }
      }
    }
  }
}
```

위 설정: 먼저 DNS 시도 → DNS 실패·결과 없음 시 설정된 서비스로 폴백.

#### 1.8 프로젝트 정보

- 아티팩트(Artifact): com.typesafe.akka:akka-discovery:2.10.19
- 지원 JDK: 11, 17, 21 (Eclipse Temurin)
- Scala 버전: 2.13.17, 3.3.7
- JPMS 모듈 이름: akka.discovery
- 라이선스: BUSL-1.1
- 상태: Lightbend의 상업적 지원을 받는 정식 지원(Supported)
- 제공 시작: 2018년 12월 7일 (버전 2.5.19)

#### 1.9 Akka 매니지먼트(1.0.0 이전)로부터의 마이그레이션

- 커스텀 디스커버리 구현체는 `akka.discovery.ServiceDiscovery` 상속 필수
- 설정의 `discovery-method`에는 `akka.discovery` 아래에서 구현체의 완전한 클래스명(fully qualified name)을 지정하는 `class` 속성 필요

---

### 2. 이벤트 스트림(Event Stream)

#### 2.1 핵심 정의

- 이벤트 스트림(EventStream): 각 액터 시스템의 주요 이벤트 버스(Event Bus)
- 로그 메시지와 데드 레터(Dead Letter) 전달에 사용 → 사용자 코드에서 다른 목적으로도 활용 가능
- 관련 채널 집합에 대한 구독을 지원하기 위해 서브채널 분류(Subchannel Classification) 사용

#### 2.2 의존성

- Akka Actor Typed: `com.typesafe.akka:akka-actor-typed` 버전 2.10.19를 sbt·Maven·Gradle 중 하나의 빌드 도구로 추가

#### 2.3 구독(Subscription) 메커니즘

##### 기본 사용 패턴

- 액터는 메시지 어댑터(message adapter) 방식으로 이벤트 스트림 구독
- 구독자 종료 시 구독 자동 해제 → 수동 정리 불필요

##### 계층적 구독(Hierarchical Subscriptions)

- 핵심 기능: 이벤트 그룹을 공통 상위 클래스(superclass)로 구독 가능
- 예: 어떤 액터가 `AllKindsOfMusic` 구독 → `Jazz`와 `Electronic` 이벤트 모두 수신 가능·다른 구독자는 `Jazz`만 등록 가능

#### 2.4 데드 레터(Dead Letters) 시스템

- 종료된 액터로 전송된 메시지 → 데드 레터 메일박스(dead letter mailbox)로 재라우팅되어 `DeadLetter` 객체로 발행
- 해당 객체: 원래 송신자·수신자·메시지 정보 포함

##### 억제된 데드 레터(Suppressed Dead Letters)

- `DeadLetterSuppression`으로 표시된 메시지는 기본 데드 레터 로그 미생성
- `SuppressedDeadLetter` 또는 `AllDeadLetters` 클래스를 통해 명시적으로 구독 가능

#### 2.5 EventBus 아키텍처

- EventBus 인터페이스가 정의하는 네 가지 핵심 연산: `subscribe()`·`unsubscribe()`·`publish()`·일괄(bulk) 구독 해제
- 중요한 제약: 이벤트 스트림은 발행된 메시지의 송신자 정보를 보존하지 않음 → 원래 송신자 참조가 필요하면 메시지 페이로드 안에 포함시켜야 함

##### 추상 타입(Abstract Type) 요구사항

모든 EventBus 구현체가 정의해야 하는 것.

- Event - 발행되는 메시지 타입
- Subscriber - 허용되는 구독자 타입
- Classifier - 선택 기준(selection criteria) 타입

#### 2.6 분류 전략(Classification Strategies)

##### 룩업 분류(Lookup Classification)

- 이벤트로부터 임의의 분류자(classifier) 추출 → 분류자별로 구독자 집합 유지
- 구현 시 필요한 메서드
  - `classify()` - 이벤트에서 분류자를 추출
  - `publish()` - 구독자에게 전달
  - `compareSubscribers()` - 순서(ordering) 정의
  - `mapSize()` - 초기 인덱스 용량(capacity)

##### 서브채널 분류(Subchannel Classification)

- 구독이 상위 및 하위 분류에 적용되는 계층적 분류자 지원
- `isEqual()`과 `isSubclass()` 관계를 정의하는 `Subclassification` 객체 필요

##### 스캐닝 분류(Scanning Classification)

- 서로 겹치는(overlapping), 비계층적인(non-hierarchical) 분류자를 매칭 함수로 처리
- 필요한 메서드
  - `compareClassifiers()`와 `compareSubscribers()` - 순서 정의
  - `matches()` - 분류자와 이벤트 간 호환성 판단
  - `publish()` - 메시지 전달
- 성능: 매치(match)의 수가 아니라 구독(subscription)의 수에 비례

##### 액터 분류(Actor Classification)

- 구독자와 분류자 모두에 `ActorRef` 사용에 특화 → 주로 DeathWatch 구현을 위해 설계
- `ManagedActorClassification`: 종료된 액터의 구독을 자동으로 해제

#### 2.7 분산 환경에서의 제약

- 이벤트 스트림은 로컬 기능(local facility) → 클러스터(cluster) 환경에서 다른 노드로 이벤트를 분산하지 않음(원격 액터를 명시적으로 구독시키지 않는 한)
- 명시적인 수신자 참조 없이 클러스터 전역(cluster-wide)으로 브로드캐스트(broadcast)하려면 다음 대안 고려
  - 리셉셔니스트(Receptionist)
  - 그룹 라우터(Group Routers)
  - 클러스터의 분산 발행-구독(Distributed Publish Subscribe in Cluster)

#### 2.8 코드 예제 구조

- 공식 문서: 모든 구독 패턴·분류자 구현·테스트 예시에 대해 Scala와 Java 두 언어의 병렬 구현 제공, 소스 코드 저장소 링크도 함께 제공

---

### 3. 로깅(Logging)

#### 3.1 개요

- Akka 타입드(typed) 액터 프레임워크는 로깅에 SLF4J 사용 → `ActorContext`를 통해 로거 접근
- 공식 문서는 프로덕션 환경에서의 성능 영향 최소화를 위해 비동기 어펜더(asynchronous appender) 설정을 특히 강조

#### 3.2 핵심 로깅 개념

##### 기본 설정

- 로깅 사용 조건: `akka-actor-typed` 의존성 + Logback 같은 SLF4J 백엔드(backend)
- `ActorContext`: 개별 액터별로 `org.slf4j.Logger` 접근 가능
- 핵심 원칙: 로깅이 성능에 미치는 영향 최소화 → SLF4J 백엔드에 비동기 어펜더 설정 필요

##### 로깅하는 방법

- 개발자는 `ActorContext`를 통해 로거 접근

```scala
Behaviors.receive[String] { (context, message) =>
  context.log.info("Received message: {}", message)
  Behaviors.same
}
```

- 로거는 `Behavior` 클래스 기반 이름을 자동으로 부여받음
- 커스텀 로거 이름은 `setLoggerName()`으로 설정

```scala
context.setLoggerName("com.myservice.BackendManager")
context.log.info("Starting up")
```

##### 중요한 구현 세부사항

- `context.log` 또는 `context.getLog()`를 통해 로거를 여러 번 가져오는 것은 오버헤드가 낮음 → 캐싱 불필요
- `ActorContext`의 로깅 관련 메서드는 스레드 안전(thread-safe)하지 않음 → 반드시 액터의 메시지 처리 스레드에서만 호출, `Future`나 `CompletionStage` 콜백 안에서 호출 금지

##### 플레이스홀더(Placeholder) 인자

- 로그 메시지는 인자 치환을 위한 `{}` 플레이스홀더 지원 → 로그 레벨 비활성화 시 불필요한 문자열 연결 방지
- 인자 1~2개인 메서드는 가변 인자(vararg) 배열을 할당하지 않음
- 인자 3개 이상이면 로그가 비활성화된 상태에서도 약간의 할당 비용 발생

##### Behaviors.logMessages

- 메시지와 시그널(signal)에 대한 상세 로깅 → `Behaviors.logMessages()`로 동작(behavior) 데코레이트(decorate)

```scala
import akka.actor.typed.LogOptions
import org.slf4j.event.Level

Behaviors.logMessages(LogOptions().withLevel(Level.TRACE), BackendManager())
```

#### 3.3 MDC(Mapped Diagnostic Context)

- 기본적으로 Akka는 액터 경로를 `akkaSource` 키로 MDC에 자동으로 추가
- 추가 태그는 `ActorTags`로 부착 가능

```scala
context.spawn(myBehavior, "MyActor", ActorTags("processing"))
```

- 태그는 `akkaTags` 키 아래에 쉼표로 구분된 목록으로 MDC에 표시

##### 커스텀 MDC 값

- 정적(static) 또는 동적(dynamic) MDC 속성 추가 → `Behaviors.withMdc` 사용

```scala
val staticMdc = Map("startTime" -> system.startTime.toString)
Behaviors.withMdc[BackendManager.Command](
  staticMdc,
  mdcForMessage = (msg: BackendManager.Command) =>
    Map("identifier" -> msg.identifier, "upTime" -> system.uptime.toString)) {
  BackendManager()
}
```

- `MDC` API 직접 사용 시 주의점: `log`/`getLog()`에 접근한 경우 Akka가 메시지 처리 후 커스텀 값을 포함한 전체 MDC를 지움
- 둘 중 어느 것에도 접근하지 않으면 MDC는 자동으로 지워지지 않음

#### 3.4 SLF4J 백엔드 설정

##### SLF4J API 호환성

- Akka 2.10.0부터 SLF4J 버전 2.0 이상만 지원
- 서로 다른 버전의 로거 백엔드와 SLF4J API 혼용 → 호환성 문제 발생

##### Logback 통합

- Logback이 권장되는 SLF4J 백엔드
- 의존성 포함

```xml
<dependency>
  <groupId>ch.qos.logback</groupId>
  <artifactId>logback-classic</artifactId>
  <version>1.5.18</version>
</dependency>
```

##### AsyncAppender 설정

- 프로덕션 환경에서는 로깅을 백그라운드 스레드로 위임하기 위해 Logback의 `AsyncAppender` 사용

```xml
<appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
  <queueSize>8192</queueSize>
  <neverBlock>true</neverBlock>
  <appender-ref ref="FILE" />
</appender>
```

- `neverBlock` 설정: 큐(queue)가 가득 찰 경우 `INFO`와 `DEBUG` 메시지를 버려서 점진적 성능 저하(graceful degradation) 가능
- 고성능을 위한 대안: Logstash 비동기 어펜더

##### 프로덕션 설정 예제

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>myapp.log</file>
    <immediateFlush>false</immediateFlush>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
      <fileNamePattern>myapp_%d{yyyy-MM-dd}.log</fileNamePattern>
    </rollingPolicy>
    <encoder>
      <pattern>[%date{ISO8601}] [%level] [%logger] [%marker] [%thread] - %msg MDC: {%mdc}%n</pattern>
    </encoder>
  </appender>

  <appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
    <queueSize>8192</queueSize>
    <neverBlock>true</neverBlock>
    <appender-ref ref="FILE" />
  </appender>

  <import class="ch.qos.logback.core.hook.DefaultShutdownHook"/>
  <shutdownHook class="DefaultShutdownHook"/>

  <root level="INFO">
    <appender-ref ref="ASYNC"/>
  </root>
</configuration>
```

##### 개발 환경 설정 예제

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
      <level>INFO</level>
    </filter>
    <encoder>
      <pattern>[%date{ISO8601}] [%level] [%logger] [%marker] [%thread] - %msg MDC: {%mdc}%n</pattern>
    </encoder>
  </appender>

  <appender name="FILE" class="ch.qos.logback.core.FileAppender">
    <file>target/myapp-dev.log</file>
    <encoder>
      <pattern>[%date{ISO8601}] [%level] [%logger] [%marker] [%thread] - %msg MDC: {%mdc}%n</pattern>
    </encoder>
  </appender>

  <root level="DEBUG">
    <appender-ref ref="STDOUT"/>
    <appender-ref ref="FILE"/>
  </root>
</configuration>
```

- 프로덕션용: `logback.xml`을 `src/main/resources/`에 배치
- 테스트용: `logback-test.xml`을 `src/test/resources/`에 배치

##### 출력에서의 MDC 값

MDC 속성은 패턴(pattern)에 포함 가능.

- `%X{akkaSource}` - 액터 경로
- `%X{akkaAddress}` - 호스트명/포트가 포함된 ActorSystem 주소
- `%X{akkaTags}` - 쉼표로 구분된 태그 목록
- `%X{sourceActorSystem}` - ActorSystem 이름
- `%mdc` - 모든 MDC 속성을 키-값 쌍으로 표현

#### 3.5 Akka 내부 로깅(Internal Logging)

##### 이벤트 버스 아키텍처

- Akka의 내부 및 클래식(classic) 액터 로깅은 이벤트 버스를 통해 비동기적으로 동작
- `akka-actor-typed`와 `akka-slf4j`가 모두 클래스패스에 있으면 `Slf4jLogger`가 자동으로 이벤트를 SLF4J로 방출 → `akka.use-slf4j=off`로 비활성화 가능

##### 로그 레벨 설정

- `akka.loglevel` 설정 속성: SLF4J 백엔드를 참조하기 전에 이벤트를 필터링
- 기본값: `INFO` → 백엔드에서 활성화되어 있더라도 Akka 내부의 `DEBUG` 로그 억제
- 프로덕션 오버헤드 없이 `DEBUG` 레벨 활성화

```
akka.loglevel = "DEBUG"
```

- 모든 Akka 내부 로깅 비활성화(권장하지 않음)

```
akka {
  stdout-loglevel = "OFF"
  loglevel = "OFF"
}
```

- `stdout-loglevel`은 시작(startup) 및 종료(shutdown) 시에만 적용

##### 시작 및 종료 로깅

- ActorSystem 시작과 종료 중에는 설정된 로거가 우회됨
- 메시지는 기본 레벨 `WARNING`으로 stdout에 출력 → `akka.stdout-loglevel=OFF`로 억제 가능

##### 데드 레터 로깅

- 데드 레터 로깅은 기본적으로 INFO 레벨로 활성화
- 로깅되는 데드 레터의 개수와 종료 시 동작 제어 가능

```
akka {
  log-dead-letters = 10
  log-dead-letters-during-shutdown = on
}
```

- 커스텀 데드 레터 처리가 필요하면 이벤트 스트림(Event Stream) 구독

##### 디버그 설정 옵션

다음 옵션들은 `akka.loglevel = "DEBUG"`와 함께 활성화.

- 시작 시 설정 로깅

```
akka {
  log-config-on-start = on
}
```

- 처리되지 않은(unhandled) 메시지 로깅

```
akka {
  actor {
    debug {
      unhandled = on
    }
  }
}
```

- EventStream 구독 모니터링

```
akka {
  actor {
    debug {
      event-stream = on
    }
  }
}
```

##### 원격(Remote) 로깅 옵션

Artery 리모팅 계층(remoting layer) 디버깅을 위한 옵션.

- 송신(outbound) 메시지 로깅

```
akka.remote.artery {
  log-sent-messages = on
}
```

- 수신(inbound) 메시지 로깅

```
akka.remote.artery {
  log-received-messages = on
}
```

- 초과 크기(oversized) 메시지 로깅

```
akka.remote.artery {
  log-frame-size-exceeding = 10000b
}
```

##### 내부 로깅에서 제공되는 MDC 값

Akka의 내부 로깅은 비동기적 → 다음 값들 제공.

- `sourceThread` - 로깅이 발생한 스레드
- `akkaSource` - 액터 경로
- `sourceActorSystem` - ActorSystem 이름
- `akkaAddress` - 호스트/포트가 포함된 시스템 주소
- `akkaTimestamp` - 타입드 액터 로그의 타임스탬프(내부/클래식 액터는 이 값을 채우지 않음)

##### 마커(Markers)

Akka는 특정 로그 타입을 강조하기 위해 마커 사용.

- `SECURITY` - 보안 관련 이벤트
- `ActorLogMarker` - 액터 생명주기(lifecycle) 이벤트
- `ClusterLogMarker` - 클러스터 이벤트
- `RemoteLogMarker` - 리모팅 이벤트
- `ShardingLogMarker` - 샤딩(sharding) 이벤트

- 마커를 출력에 포함시키려면 `%marker` 사용
- Logstash Logback 인코더는 마커와 MDC 속성을 자동으로 인식

##### 로거 이름 설정

- 특정 Akka 모듈의 로그 레벨을 로거 이름(logger name)으로 설정 가능. 클러스터 샤딩(Cluster Sharding) 예제.

```xml
<logger name="akka.cluster.sharding" level="DEBUG" />

<root level="INFO">
  <appender-ref ref="ASYNC"/>
</root>
```

자주 사용되는 로거 이름/접두사(prefix):

```
akka.cluster
akka.cluster.Cluster
akka.cluster.ClusterHeartbeat
akka.cluster.ClusterGossip
akka.cluster.ddata
akka.cluster.pubsub
akka.cluster.singleton
akka.cluster.sharding
akka.coordination.lease
akka.discovery
akka.persistence
akka.remote
```

#### 3.6 테스트에서의 로깅

- 테스트 시 로그 캡처 관련 유틸리티는 공식 문서의 Testing 섹션에서 다룸

---

### 4. 서킷 브레이커(Circuit Breaker)

#### 4.1 목적과 동기

- 서킷 브레이커(circuit breaker): 연쇄 장애(cascading failure)를 방지 → 분산 시스템의 안정성 향상
- 대표 시나리오: 서드파티 웹 서비스가 과부하 상태가 되어 타임아웃 발생 → 사용자가 요청을 반복적으로 재시도하며 자원을 고갈시켜 전체 사용자에게 영향
- 서킷 브레이커 도입 → 요청이 빠르게 실패(fail-fast)하기 시작해 사용자에게 문제 상황을 즉시 알릴 수 있음, 장애 범위를 영향받는 사용자에게만 한정 가능

#### 4.2 3-상태(Three-State) 모델

서킷 브레이커는 세 가지 명확한 상태로 동작.

- 닫힘 상태(Closed State, 정상 동작)
  - 예외 발생 또는 설정된 타임아웃 초과 호출 → 실패 카운터 증가
  - 성공한 호출 → 실패 횟수를 0으로 리셋
  - 실패 횟수가 `maxFailures`에 도달 → 열림(Open) 상태로 전환
- 열림 상태(Open State)
  - 모든 호출은 즉시 `CircuitBreakerOpenException`으로 실패
  - 설정된 `resetTimeout` 이후 반열림(Half-Open) 상태로 진입
- 반열림 상태(Half-Open State)
  - 첫 번째 호출은 즉시 실패하지 않고 통과 허용
  - 그 외의 모든 호출은 열림 상태와 마찬가지로 빠르게 실패
  - 첫 번째 호출 성공 → 닫힘(Closed) 상태로 리셋
  - 첫 번째 호출 실패 → 다시 열림(Open) 상태로 복귀

#### 4.3 설정 예제

```
akka.circuit-breaker.data-access {
   max-failures = 5
   call-timeout = 10s
   reset-timeout = 1m
}
```

#### 4.4 Scala 구현

##### 초기화

```scala
val circuitBreaker = CircuitBreaker("data-access")(context.system)
```

##### Future 기반 API

```scala
class DataAccess(
    context: ActorContext[DataAccess.Command],
    id: String,
    service: ThirdPartyWebService,
    circuitBreaker: CircuitBreaker) {
  import DataAccess._

  private def active(): Behavior[Command] = {
    Behaviors.receiveMessagePartial {
      case Handle(value, replyTo) =>
        val futureResult: Future[Done] = circuitBreaker.withCircuitBreaker {
          service.call(id, value)
        }
        context.pipeToSelf(futureResult) {
          case Success(_)         => HandleSuceeded(replyTo)
          case Failure(exception) => HandleFailed(replyTo, exception)
        }
        Behaviors.same
      case HandleSuceeded(replyTo) =>
        replyTo ! StatusReply.Ack
        Behaviors.same
      case HandleFailed(replyTo, exception) =>
        context.log.warn("Failed to call web service", exception)
        replyTo ! StatusReply.error("Dependency service not available")
        Behaviors.same
    }
  }
}
```

##### 커스텀 실패 정의(Custom Failure Definition)

```scala
val evenNumberAsFailure: Try[Int] => Boolean = {
  case Success(n) => n % 2 == 0
  case Failure(_) => true
}

val breaker = CircuitBreaker("dangerous-breaker")

// 이 호출은 8888을 반환하면서 실패 횟수를 증가시킨다
breaker.withCircuitBreaker(Future(8888), evenNumberAsFailure)
```

##### 로우 레벨(Low-Level) API

```scala
import akka.actor.typed.scaladsl.adapter._
val breaker =
  new CircuitBreaker(
    context.system.scheduler.toClassic,
    maxFailures = 5,
    callTimeout = 10.seconds,
    resetTimeout = 1.minute).onOpen(context.self ! BreakerOpen)
```

##### Tell 보호 패턴(Tell Protection Pattern)

```scala
object CircuitBreakingIntermediateActor {
  sealed trait Command
  case class Call(payload: String, replyTo: ActorRef[StatusReply[Done]]) extends Command
  private case class OtherActorReply(reply: Try[Done], originalReplyTo: ActorRef[StatusReply[Done]]) extends Command
  private case object BreakerOpen extends Command

  def apply(recipient: ActorRef[OtherActor.Command]): Behavior[Command] =
    Behaviors.setup { context =>
      implicit val askTimeout: Timeout = 11.seconds
      import context.executionContext
      import akka.actor.typed.scaladsl.adapter._
      val breaker =
        new CircuitBreaker(
          context.system.scheduler.toClassic,
          maxFailures = 5,
          callTimeout = 10.seconds,
          resetTimeout = 1.minute).onOpen(context.self ! BreakerOpen)

      Behaviors.receiveMessage {
        case Call(payload, replyTo) =>
          if (breaker.isClosed || breaker.isHalfOpen) {
            context.askWithStatus(recipient, OtherActor.Call(payload, _))(OtherActorReply(_, replyTo))
          } else {
            replyTo ! StatusReply.error("Service unavailable")
          }
          Behaviors.same
        case OtherActorReply(reply, originalReplyTo) =>
          if (reply.isSuccess) breaker.succeed()
          else breaker.fail()
          originalReplyTo ! StatusReply.fromTry(reply)
          Behaviors.same
        case BreakerOpen =>
          context.log.warn("Circuit breaker open")
          Behaviors.same
      }
    }
}
```

#### 4.5 Java 구현

##### 초기화

```java
CircuitBreaker circuitBreaker =
    CircuitBreaker.lookup("data-access", context.getSystem());
```

##### Future 기반 API

```java
class DataAccess extends AbstractBehavior<DataAccess.Command> {
  public interface Command {}

  public static class Handle implements Command {
    final String value;
    final ActorRef<StatusReply<Done>> replyTo;

    public Handle(String value, ActorRef<StatusReply<Done>> replyTo) {
      this.value = value;
      this.replyTo = replyTo;
    }
  }

  private final class HandleFailed implements Command {
    final Throwable failure;
    final ActorRef<StatusReply<Done>> replyTo;

    public HandleFailed(Throwable failure, ActorRef<StatusReply<Done>> replyTo) {
      this.failure = failure;
      this.replyTo = replyTo;
    }
  }

  private final class HandleSuceeded implements Command {
    final ActorRef<StatusReply<Done>> replyTo;

    public HandleSuceeded(ActorRef<StatusReply<Done>> replyTo) {
      this.replyTo = replyTo;
    }
  }

  private final class CircuitBreakerStateChange implements Command {
    final String newState;

    public CircuitBreakerStateChange(String newState) {
      this.newState = newState;
    }
  }

  public static Behavior<Command> create(String id, ThirdPartyWebService service) {
    return Behaviors.setup(
        context -> {
          CircuitBreaker circuitBreaker =
              CircuitBreaker.lookup("data-access", context.getSystem());
          return new DataAccess(context, id, service, circuitBreaker);
        });
  }

  private final String id;
  private final ThirdPartyWebService service;
  private final CircuitBreaker circuitBreaker;

  public DataAccess(
      ActorContext<Command> context,
      String id,
      ThirdPartyWebService service,
      CircuitBreaker circuitBreaker) {
    super(context);
    this.id = id;
    this.service = service;
    this.circuitBreaker = circuitBreaker;
  }

  @Override
  public Receive<Command> createReceive() {
    return newReceiveBuilder()
        .onMessage(Handle.class, this::onHandle)
        .onMessage(HandleSuceeded.class, this::onHandleSucceeded)
        .onMessage(HandleFailed.class, this::onHandleFailed)
        .build();
  }

  private Behavior<Command> onHandle(Handle handle) {
    CompletionStage<Done> futureResult =
        circuitBreaker.callWithCircuitBreakerCS(() -> service.call(id, handle.value));
    getContext()
        .pipeToSelf(
            futureResult,
            (done, throwable) -> {
              if (throwable != null) {
                return new HandleFailed(throwable, handle.replyTo);
              } else {
                return new HandleSuceeded(handle.replyTo);
              }
            });
    return this;
  }

  private Behavior<Command> onHandleSucceeded(HandleSuceeded handleSuceeded) {
    handleSuceeded.replyTo.tell(StatusReply.ack());
    return this;
  }

  private Behavior<Command> onHandleFailed(HandleFailed handleFailed) {
    getContext().getLog().warn("Failed to call web service", handleFailed.failure);
    handleFailed.replyTo.tell(StatusReply.error("Dependency service not available"));
    return this;
  }
}
```

##### 커스텀 실패 정의

```java
BiFunction<Optional<Integer>, Optional<Throwable>, Boolean> evenNoAsFailure =
    (result, err) -> (result.isPresent() && result.get() % 2 == 0);

// 이 호출은 8888을 반환하면서 실패 횟수를 증가시킨다
return circuitBreaker.callWithSyncCircuitBreaker(() -> 8888, evenNoAsFailure);
```

##### 로우 레벨 API

```java
breaker =
    CircuitBreaker.create(
            getContext().getSystem().classicSystem().getScheduler(),
            // maxFailures
            5,
            // callTimeout
            Duration.ofSeconds(10),
            // resetTimeout
            Duration.ofMinutes(1))
        .addOnOpenListener(() -> context.getSelf().tell(new BreakerOpen()));
```

##### Tell 보호 패턴

```java
static class CircuitBreakingIntermediateActor
    extends AbstractBehavior<CircuitBreakingIntermediateActor.Command> {

  public interface Command {}

  public static class Call implements Command {
    final String payload;
    final ActorRef<StatusReply<Done>> replyTo;

    public Call(String payload, ActorRef<StatusReply<Done>> replyTo) {
      this.payload = payload;
      this.replyTo = replyTo;
    }
  }

  private class OtherActorReply implements Command {
    final Optional<Throwable> failure;
    final ActorRef<StatusReply<Done>> originalReplyTo;

    public OtherActorReply(
        Optional<Throwable> failure, ActorRef<StatusReply<Done>> originalReplyTo) {
      this.failure = failure;
      this.originalReplyTo = originalReplyTo;
    }
  }

  private class BreakerOpen implements Command {}

  private final ActorRef<OtherActor.Command> target;
  private final CircuitBreaker breaker;

  public CircuitBreakingIntermediateActor(
      ActorContext<Command> context, ActorRef<OtherActor.Command> targetActor) {
    super(context);
    this.target = targetActor;
    breaker =
        CircuitBreaker.create(
                getContext().getSystem().classicSystem().getScheduler(),
                // maxFailures
                5,
                // callTimeout
                Duration.ofSeconds(10),
                // resetTimeout
                Duration.ofMinutes(1))
            .addOnOpenListener(() -> context.getSelf().tell(new BreakerOpen()));
  }

  @Override
  public Receive<Command> createReceive() {
    return newReceiveBuilder()
        .onMessage(Call.class, this::onCall)
        .onMessage(OtherActorReply.class, this::onOtherActorReply)
        .onMessage(BreakerOpen.class, this::breakerOpened)
        .build();
  }

  private Behavior<Command> onCall(Call call) {
    if (breaker.isClosed() || breaker.isHalfOpen()) {
      getContext()
          .askWithStatus(
              Done.class,
              target,
              Duration.ofSeconds(11),
              (replyTo) -> new OtherActor.Call(call.payload, replyTo),
              (done, failure) -> new OtherActorReply(Optional.ofNullable(failure), call.replyTo));
    } else {
      call.replyTo.tell(StatusReply.error("Service unavailable"));
    }
    return this;
  }

  private Behavior<Command> onOtherActorReply(OtherActorReply otherActorReply) {
    if (otherActorReply.failure.isPresent()) {
      breaker.fail();
      getContext().getLog().warn("Service failure", otherActorReply.failure.get());
      otherActorReply.originalReplyTo.tell(StatusReply.error("Service unavailable"));
    } else {
      breaker.succeed();
      otherActorReply.originalReplyTo.tell(StatusReply.ack());
    }
    return this;
  }

  private Behavior<Command> breakerOpened(BreakerOpen breakerOpen) {
    getContext().getLog().warn("Circuit breaker open");
    return this;
  }
}
```

#### 4.6 콜백(Callback) 타입

- 상태 전이 리스너(State Transition Listeners)
  - `onOpen`: 브레이커가 열릴 때 실행
  - `onClose`: 브레이커가 닫힐 때 실행
  - `onHalfOpen`: 브레이커가 반열림 상태에 진입할 때 실행
- 호출 결과 리스너(Call Result Listeners)
  - `onCallSuccess`: 호출이 성공했을 때 호출
  - `onCallFailure`: 호출이 실패했을 때 호출
  - `onCallTimeout`: 호출이 타임아웃을 초과했을 때 호출
  - `onCallBreakerOpen`: 호출 시점에 브레이커가 열려 있을 때 호출

모든 콜백은 제공된 `ExecutionContext` 또는 기본 시스템 디스패처(dispatcher)에서 실행됨.

#### 4.7 주요 메서드

- 상태 조회(State Query) 메서드
  - `isClosed()`: 브레이커가 닫힘 상태이면 true 반환
  - `isOpen()`: 브레이커가 열림 상태이면 true 반환
  - `isHalfOpen()`: 브레이커가 반열림 상태이면 true 반환
- 호출 보호(Call Protection) 메서드
  - `withCircuitBreaker()`: 비동기 호출을 래핑(Scala)
  - `callWithCircuitBreakerCS()`: 비동기 호출을 래핑(Java)
  - `withSyncCircuitBreaker()`: 동기 호출을 래핑(Scala)
  - `callWithSyncCircuitBreaker()`: 동기 호출을 래핑(Java)
- 고급 사용자(Power-User) 메서드
  - `succeed()`: 성공한 호출을 수동으로 기록
  - `fail()`: 실패한 호출을 수동으로 기록

#### 4.8 커스텀 실패 함수(Custom Failure Functions)

- 두 API 모두 커스텀 실패 로직 지원
  - Scala: `Try[T] => Boolean` 함수
  - Java: `BiFunction<Optional<T>, Optional<Throwable>, Boolean>`
- `true` 반환 → 실패 횟수 증가 · `false` 반환 → 변화 없음
- 기본 동작과 독립적으로 특정 결과를 실패 또는 성공으로 취급 가능

---

### 5. 퓨처 패턴(Futures Patterns)

#### 5.1 개요와 의존성

이 헬퍼들은 Akka 코어 모듈의 일부. 의존성 설정 예제.

- sbt: `"com.typesafe.akka" %% "akka-actor" % "2.10.19"`
- Maven: BOM 임포트와 함께 아티팩트 ID `akka-actor_${scala.binary.version}` 사용
- Gradle: 플랫폼 의존성 관리(platform dependency management) 방식 사용

#### 5.2 After 패턴

`akka.pattern.after` 유틸리티: 지정된 타임아웃 기간 이후에 Future 또는 CompletionStage를 값 또는 예외로 완료.

- Scala 예제

```scala
val delayed = akka.pattern.after(200.millis)(
  Future.failed(new IllegalStateException("OHNOES")))
val future = Future { Thread.sleep(1000); "foo" }
val result = Future.firstCompletedOf(Seq(future, delayed))
```

- Java 예제

```java
import akka.pattern.Patterns;

CompletionStage<String> failWithException =
    CompletableFuture.supplyAsync(
        () -> {
          throw new IllegalStateException("OHNOES1");
        });
CompletionStage<String> delayed =
    Patterns.after(Duration.ofMillis(200), system, () -> failWithException);
```

#### 5.3 Retry 패턴

`akka.pattern.retry` 함수: Future 또는 CompletionStage를 각 시도 사이에 간격(delay)을 두면서 여러 번 재시도.

- Scala 예제

```scala
import akka.actor.typed.scaladsl.adapter._
implicit val scheduler: akka.actor.Scheduler = system.scheduler.toClassic
implicit val ec: ExecutionContext = system.executionContext

@volatile var failCount = 0
def futureToAttempt() = {
  if (failCount < 5) {
    failCount += 1
    Future.failed(new IllegalStateException(failCount.toString))
  } else Future.successful(5)
}

val retried: Future[Int] = akka.pattern.retry(
  () => futureToAttempt(), attempts = 10, 100 milliseconds)
```

- Java 예제

```java
import akka.pattern.Patterns;

Callable<CompletionStage<String>> attempt =
  () -> CompletableFuture.completedFuture("test");

CompletionStage<String> retriedFuture =
    Patterns.retry(attempt, 3, java.time.Duration.ofMillis(200), system);
```

---

### 6. 코디네이션과 리스(Coordination & Lease)

#### 6.1 개요

- Akka Coordination: 분산 코디네이션을 위한 도구 모음
- 리스(lease) 컴포넌트: 분산 락(distributed lock)을 위한 플러그형(pluggable) API 제공

#### 6.2 모듈 정보

- 현재 버전: 2.10.19
- 아티팩트: com.typesafe.akka / akka-coordination
- 지원 JDK 버전: Eclipse Temurin JDK 11, 17, 21
- Scala 버전: 2.13.17, 3.3.7
- 라이선스: BUSL-1.1
- 상태: Lightbend의 상업적 지원을 받는 정식 지원(Supported)
- 제공 시작: 2.5.22 (2019년 4월 3일)

#### 6.3 의존성 설정

##### sbt

```
val AkkaVersion = "2.10.19"
libraryDependencies += "com.typesafe.akka" %% "akka-coordination" % AkkaVersion
```

##### Maven

```xml
<properties>
  <scala.binary.version>2.13</scala.binary.version>
</properties>
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.typesafe.akka</groupId>
      <artifactId>akka-bom_${scala.binary.version}</artifactId>
      <version>2.10.19</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
<dependencies>
  <dependency>
    <groupId>com.typesafe.akka</groupId>
    <artifactId>akka-coordination_${scala.binary.version}</artifactId>
  </dependency>
</dependencies>
```

##### Gradle

```
def versions = [
  ScalaBinary: "2.13"
]
dependencies {
  implementation platform("com.typesafe.akka:akka-bom_${versions.ScalaBinary}:2.10.19")
  implementation "com.typesafe.akka:akka-coordination_${versions.ScalaBinary}"
}
```

#### 6.4 리스(Lease) 보장

모든 리스 구현체가 제공해야 하는 두 가지 핵심 보장.

1. 같은 이름의 리스를, 심지어 서로 다른 노드에서, 여러 번 로드하더라도 동일한 리스임
2. 한 번에 단 하나의 소유자(owner)만이 리스를 획득(acquire) 가능

#### 6.5 리스 사용하기

리스는 세 개의 파라미터로 로드됨: 리스 이름 · 구현체를 지정하는 설정 위치(configuration location) · 소유자 이름(owner name).

##### Scala 사용법

```scala
val lease = LeaseProvider(system).getLease("<name of the lease>", "docs-lease", "owner")
val acquired: Future[Boolean] = lease.acquire()
val stillAcquired: Boolean = lease.checkLease()
val released: Future[Boolean] = lease.release()
```

##### Java 사용법

```java
Lease lease = LeaseProvider.get(system).getLease("<name of the lease>", "jdocs-lease", "<owner name>");
CompletionStage<Boolean> acquired = lease.acquire();
boolean stillAcquired = lease.checkLease();
CompletionStage<Boolean> released = lease.release();
```

#### 6.6 리스 연산

- Acquire(획득): 리스 구현체는 일반적으로 쿠버네티스 API 서버나 Zookeeper 같은 서드파티 시스템을 통해 구현 → 획득 결과는 Future/CompletionStage로 반환
- Check Lease(리스 확인): 리스 획득 후 `checkLease` 호출로 리스가 여전히 유효한지 확인 가능. 이 호출은 동기(synchronous) → 리스가 필요한 연산을 수행하기 전에 호출
- Reentrant(재진입 가능): 같은 소유자가 리스를 여러 번 획득 시도 → 성공. 즉 리스는 재진입 가능(reentrant)

#### 6.7 고유한 리스 이름 짓기

노드별로 클러스터 내에서 고유한(cluster-unique) 리스를 만드는 방법.

##### Scala

```scala
val owner = Cluster(system).selfAddress.hostPort
```

##### Java

```java
// String owner = Cluster.get(system).selfAddress().hostPort();
```

- 같은 노드에서 여러 개의 리스를 사용하려면 이름에 고유한 식별자 추가 필요
- 예: Cluster Sharding은 리스 이름에 샤드 ID를 포함

#### 6.8 리스 하트비트(Heartbeat) 설정

- 리스를 보유한 노드가 크래시되거나 응답하지 않게 되면 → `heartbeat-timeout`은 다른 노드가 리스를 획득할 수 있을 때까지 걸리는 시간
- `heartbeat-timeout`은 예상되는 최대 JVM 정지(pause) 시간(가비지 컬렉션 등)보다 길게 설정 필요 → 그렇지 않으면 원래 소유자가 복구되는 동안 다른 노드가 리스를 획득해 충돌 구간(conflict window) 발생 가능

#### 6.9 리스 구현체

현재 제공되는 구현체.

- 쿠버네티스 API(Kubernetes API) - akka-management 라이브러리를 통해 제공

#### 6.10 커스텀 리스 구현하기

커스텀 구현체는 `akka.coordination.lease.scaladsl.Lease` 또는 `akka.coordination.lease.javadsl.Lease` 상속.

##### Scala 구현 예제

```scala
class SampleLease(settings: LeaseSettings) extends Lease(settings) {
  override def acquire(): Future[Boolean] = {
    Future.successful(true)
  }

  override def acquire(leaseLostCallback: Option[Throwable] => Unit): Future[Boolean] = {
    Future.successful(true)
  }

  override def release(): Future[Boolean] = {
    Future.successful(true)
  }

  override def checkLease(): Boolean = {
    true
  }
}
```

##### Java 구현 예제

```java
static class SampleLease extends Lease {
  private LeaseSettings settings;

  public SampleLease(LeaseSettings settings) {
    this.settings = settings;
  }

  @Override
  public LeaseSettings getSettings() {
    return settings;
  }

  @Override
  public CompletionStage<Boolean> acquire() {
    return CompletableFuture.completedFuture(true);
  }

  @Override
  public CompletionStage<Boolean> acquire(Consumer<Optional<Throwable>> leaseLostCallback) {
    return CompletableFuture.completedFuture(true);
  }

  @Override
  public CompletionStage<Boolean> release() {
    return CompletableFuture.completedFuture(true);
  }

  @Override
  public boolean checkLease() {
    return true;
  }
}
```

#### 6.11 커스텀 구현을 위한 메서드 보장

- acquire(): 리스가 획득되면 `true`로, 다른 소유자가 보유 중이면 `false`로 완료해야 함 · 서드파티 시스템과의 통신이 실패하면 실패해야 함
- release(): 확실히 해제되었으면 `true`로, 확실히 해제되지 않았으면 `false`로 완료해야 함 · 해제 상태를 알 수 없으면 실패해야 함
- checkLease(): 획득된 상태이면 `true` · acquire의 Future/CompletionStage가 완료되기 전까지는 `false` · 통신 오류로 리스를 잃었으면 `false` 반환 · 블로킹(block) 금지
- acquire()의 리스 손실 콜백(lease lost callback): acquire의 Future/CompletionStage가 완료된 이후에만 호출됨 · 리스를 잃었을 때(예: 서드파티 시스템과의 통신 실패) 트리거됨

#### 6.12 TTL(Time-to-Live) 메커니즘

- 커스텀 구현체는 노드 크래시 이후 무한정 리스를 보유하는 것을 방지하기 위한 TTL(time-to-live) 메커니즘 포함 필요
- 최대한의 안전성을 선호하는 사용자는 TTL을 무한(infinite)으로 설정 가능하되 수동 개입(manual intervention)이 필요할 수 있음을 감수해야 함

#### 6.13 설정 요구사항

설정에는 반드시 리스 구현체의 FQCN(완전한 클래스명)을 지정하는 `lease-class` 속성 정의 필요.

##### 기본 설정 레퍼런스

```
# 리스를 획득한 노드가 크래시되면, 다른 소유자가 리스를 가져갈 수 있게 되기까지 리스를 얼마나 오래 보유해야 하는지
heartbeat-timeout = 120s

# 리스가 여전히 보유 중인지 확인하기 위해 서드파티와 통신하는 간격
heartbeat-interval = 12s

# 리스 구현체는 acquire 및 release 호출에 타임아웃을 두거나, 연산 타임아웃을
# 구현하지 않음을 문서화할 것으로 기대된다
lease-operation-timeout = 5s
```

##### 설정 예제(Scala/Java)

```
akka.actor.provider = cluster
docs-lease {
  lease-class = "docs.coordination.SampleLease"
  heartbeat-timeout = 100s
  heartbeat-interval = 1s
  lease-operation-timeout = 1s
  # 리스에 특화된 임의의 설정
}
```

#### 6.14 다른 Akka 모듈과의 통합

- 리스는 스플릿 브레인 리졸버(Split Brain Resolver) · 클러스터 싱글톤(Cluster Singleton) · 클러스터 샤딩(Cluster Sharding)에 사용 가능

---

### 7. Akka 확장하기(Extending Akka)

#### 7.1 개요

- Akka 확장(extension)은 매우 다양한 용도로 활용 가능 → ActorSystem 전체에 걸쳐 특정 클래스의 인스턴스를 단 한 번만 생성하고 어디서나 접근 가능
- Cluster·Serialization·Sharding 같은 기능들도 Akka 확장으로 구현
- 대표적인 사용 사례: 애플리케이션 전반에서 공유되어야 하는 값비싼 데이터베이스 연결 풀(connection pool) 관리

#### 7.2 핵심 구현 요구사항

- 확장은 Akka 자체에 후킹(hook)하는 방법 → 확장 구현자는 스레드 안전(thread safety)과 논블로킹(non-blocking)을 반드시 보장 필요

#### 7.3 확장 만들기: 단계별 안내

##### 1단계: 리소스 클래스 생성

먼저 관리할 리소스를 정의.

- Scala

```scala
class ExpensiveDatabaseConnection {
  def executeQuery(query: String): Future[Any] = ???
}
```

- Java

```java
public class ExpensiveDatabaseConnection {
  public CompletionStage<Object> executeQuery(String query) {
    throw new RuntimeException("I should do a database query");
  }
}
```

##### 2단계: 확장 클래스 구현

`Extension` 인터페이스를 구현하고 리소스를 감싸는 클래스 생성.

- Scala

```scala
class DatabasePool(system: ActorSystem[_]) extends Extension {
  private val _connection = new ExpensiveDatabaseConnection()

  def connection(): ExpensiveDatabaseConnection = _connection
}
```

- Java

```java
public class DatabaseConnectionPool implements Extension {
  private final ExpensiveDatabaseConnection _connection;

  private DatabaseConnectionPool(ActorSystem<?> system) {
    _connection = new ExpensiveDatabaseConnection();
  }

  public ExpensiveDatabaseConnection connection() {
    return _connection;
  }
}
```

- 확장은 생성자에서 `ActorSystem`을 전달받음 → 필요하다면 시스템 설정으로부터 구성을 로드 가능

##### 3단계: ExtensionId 생성

인스턴스화(instantiation)를 관리하는 식별자(identifier) 정의.

- Scala

```scala
object DatabasePool extends ExtensionId[DatabasePool] {
  def createExtension(system: ActorSystem[_]): DatabasePool =
    new DatabasePool(system)

  def get(system: ActorSystem[_]): DatabasePool = apply(system)
}
```

- Java

```java
public static class Id extends ExtensionId<DatabaseConnectionPool> {
  private static final Id instance = new Id();

  private Id() {}

  @Override
  public DatabaseConnectionPool createExtension(ActorSystem<?> system) {
    return new DatabaseConnectionPool(system);
  }

  public static DatabaseConnectionPool get(ActorSystem<?> system) {
    return instance.apply(system);
  }
}
```

- Scala에서는 확장의 동반 객체(companion object)를 ExtensionId로 두는 것이 좋은 관례
- Java에서는 ExtensionId를 확장의 정적 내부 클래스(static inner class)로 정의하는 것이 권장됨

#### 7.4 확장에 접근하기

확장을 만들고 나면 애플리케이션 어디에서나 접근 가능.

- Scala

```scala
Behaviors.setup[Any] { ctx =>
  DatabasePool(ctx.system).connection().executeQuery("insert into...")
  initialBehavior
}
```

- Java

```java
Behaviors.setup(
  (context) -> {
    DatabaseConnectionPool.Id.get(context.getSystem())
      .connection()
      .executeQuery("insert into...");
    return initialBehavior();
  });
```

- 해당 ActorSystem에 대한 이후의 모든 조회에서는 동일한 인스턴스가 반환됨

#### 7.5 설정으로부터 로드하기

- 확장은 ActorSystem 시작 시 설정에 명시함으로써 즉시(eagerly) 로드되도록 할 수 있음 → 선택사항이며 일종의 최적화
- `akka.actor.typed.extensions` 설정 섹션에 완전한 클래스명 추가

- Scala

```
akka.actor.typed.extensions = ["docs.akka.extensions.DatabasePool"]
```

- Java

```
akka.actor.typed {
  extensions = ["jdocs.akka.extensions.ExtensionDocTest$DatabaseConnectionPool$Id"]
}
```

- 이 설정이 없으면 확장은 시작 시점이 아니라 최초 접근 시점에 인스턴스화되고 등록됨

---

### 참고 자료

- [Akka 공식 문서](https://doc.akka.io/libraries/akka-core/current/)
- [Akka Discovery](https://doc.akka.io/libraries/akka-core/current/discovery/index.html)
- [Event Stream](https://doc.akka.io/libraries/akka-core/current/typed/event-stream.html)
- [Logging](https://doc.akka.io/libraries/akka-core/current/typed/logging.html)
- [Circuit Breaker](https://doc.akka.io/libraries/akka-core/current/common/circuitbreaker.html)
- [Futures](https://doc.akka.io/libraries/akka-core/current/futures.html)
- [Coordination](https://doc.akka.io/libraries/akka-core/current/coordination.html)
- [Extending Akka](https://doc.akka.io/libraries/akka-core/current/typed/extending.html)

# Akka 패키징·배포·운영과 프로젝트 정보

## Akka 패키징, 배포, 운영

> 원본: https://doc.akka.io/libraries/akka-core/current/additional/

---

### 목차

1. [패키징(Packaging)](#패키징packaging)
   - [개요](#개요)
   - [sbt: 네이티브 패키저(Native Packager)](#sbt-네이티브-패키저native-packager)
   - [Maven: Shade 플러그인](#maven-shade-플러그인)
   - [Gradle: Shadow 플러그인](#gradle-shadow-플러그인)
2. [배포(Deploying)](#배포deploying)
   - [쿠버네티스(Kubernetes)에 배포하기](#쿠버네티스kubernetes에-배포하기)
   - [CPU 설정](#cpu-설정)
   - [메모리 설정](#메모리-설정)
   - [Docker 컨테이너 배포](#docker-컨테이너-배포)
3. [운영(Operating)](#운영operating)
   - [클러스터 시작하기](#클러스터-시작하기)
   - [클러스터 중지하기](#클러스터-중지하기)
   - [클러스터 관리 도구](#클러스터-관리-도구)
   - [모니터링과 관측 가능성(Observability)](#모니터링과-관측-가능성observability)
4. [롤링 업데이트(Rolling Updates)](#롤링-업데이트rolling-updates)
   - [개요](#개요-1)
   - [정상 종료(Graceful shutdown)](#정상-종료graceful-shutdown)
   - [직렬화 호환성(Serialization compatibility)](#직렬화-호환성serialization-compatibility)
   - [클러스터 샤딩(Cluster Sharding)](#클러스터-샤딩cluster-sharding)
   - [클러스터 싱글톤(Cluster Singleton)](#클러스터-싱글톤cluster-singleton)
   - [설정 호환성 검사(Configuration compatibility check)](#설정-호환성-검사configuration-compatibility-check)
   - [Akka 버전 마이그레이션과 롤링 업데이트](#akka-버전-마이그레이션과-롤링-업데이트)
   - [전체 종료가 필요한 경우](#전체-종료가-필요한-경우)
5. [네이티브 이미지(Native Image)](#네이티브-이미지native-image)
   - [개요](#개요-2)
   - [지원되지 않는 기능](#지원되지-않는-기능)
   - [추가 메타데이터가 필요한 기능](#추가-메타데이터가-필요한-기능)
   - [Jackson 직렬화](#jackson-직렬화)
   - [기타 확장 지점](#기타-확장-지점)
   - [로깅](#로깅)

---

### 패키징(Packaging)

#### 개요

- Akka 사용 시 가장 단순한 방법: Akka jar 파일들을 클래스패스(classpath)에 추가
- 분석 클러스터(analytics cluster) 등에 배포하기 위해 "팻 jar(fat jar)"를 생성하는 경우 → 추가 설정 필요
  - 각 Akka jar에 기본값(default value)들을 담은 `reference.conf` 리소스 포함
  - 팻 jar 생성 시 여러 jar의 동일한 이름 리소스가 서로 덮어쓰지 않고 병합(merge)되어야 함
- 단순히 모든 클래스·리소스를 하나의 jar로 합치면 → 마지막에 들어간 jar의 `reference.conf`만 남음 → 나머지 Akka 모듈 기본 설정 소실
  - 방지 방법: 빌드 도구별로 `reference.conf`(및 버전 정보를 담은 `version.conf`)를 이어붙이는(append) 변환(transformer) 적용

---

#### sbt: 네이티브 패키저(Native Packager)

- Akka 애플리케이션 배포본(distribution) 생성 시 [sbt-native-packager](https://github.com/sbt/sbt-native-packager) 사용 권장
- `project/build.properties` 파일에 sbt 버전 정의

```
sbt.version=1.3.12
```

- `project/plugins.sbt` 파일에 네이티브 패키저 플러그인 추가

```
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.5")
```

- 이후 sbt-native-packager 공식 문서의 [`JavaAppPackaging`](https://www.scala-sbt.org/sbt-native-packager/archetypes/java_app/index.html) 안내 따름

---

#### Maven: Shade 플러그인

- Maven 사용자: 리소스 변환기(Resource Transformer)와 함께 [Apache Maven Shade 플러그인](https://maven.apache.org/plugins/maven-shade-plugin/) 활용
- `AppendingTransformer`로 여러 jar의 `reference.conf`·`version.conf` 파일 병합
- `ManifestResourceTransformer`로 메인 클래스(main class) 진입점 지정

```xml
<plugin>
 <groupId>org.apache.maven.plugins</groupId>
 <artifactId>maven-shade-plugin</artifactId>
 <version>1.5</version>
 <executions>
  <execution>
   <id>shade-my-jar</id>
   <phase>package</phase>
   <goals>
    <goal>shade</goal>
   </goals>
   <configuration>
    <shadedArtifactAttached>true</shadedArtifactAttached>
    <shadedClassifierName>allinone</shadedClassifierName>
    <artifactSet>
     <includes>
      <include>*:*</include>
     </includes>
    </artifactSet>
    <transformers>
      <transformer
       implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
       <resource>reference.conf</resource>
      </transformer>
      <transformer
       implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
       <resource>version.conf</resource>
      </transformer>
      <transformer
       implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
       <manifestEntries>
        <Main-Class>myapp.Main</Main-Class>
       </manifestEntries>
      </transformer>
    </transformers>
   </configuration>
  </execution>
 </executions>
</plugin>
```

- 위 설정의 핵심
  - 두 개의 `AppendingTransformer` → 각각 `reference.conf`·`version.conf`를 병합 대상으로 지정 → 여러 Akka 모듈의 기본 설정 손실 방지
  - `ManifestResourceTransformer` → 매니페스트(manifest)에 `Main-Class` 항목 추가 → 생성된 jar 직접 실행 가능(예시에서는 `myapp.Main`을 진입점으로 지정)

---

#### Gradle: Shadow 플러그인

- Gradle 사용자: 일반적으로 Java 플러그인의 `Jar` 태스크 사용
- 팻 jar 생성에는 [Shadow 플러그인](https://github.com/johnrengelman/shadow) 권장 → Groovy DSL·Kotlin DSL 양쪽 문법 모두 지원

Groovy DSL 예시.

```gradle
import com.github.jengelman.gradle.plugins.shadow.transformers.AppendingTransformer

plugins {
    id 'java'
    id "com.github.johnrengelman.shadow" version "7.0.0"
}

shadowJar {
    append 'reference.conf'
    append 'version.conf'
    with jar
}
```

Kotlin DSL 예시.

```kotlin
tasks.withType<ShadowJar> {
    val newTransformer = AppendingTransformer()
    newTransformer.resource = "reference.conf"
    transformers.add(newTransformer)
}
```

- 두 경우 모두 `reference.conf`(및 `version.conf`)를 이어붙이도록(append) 설정 → 팻 jar에 포함된 여러 Akka 모듈의 기본 설정이 올바르게 병합됨

---

### 배포(Deploying)

#### 쿠버네티스(Kubernetes)에 배포하기

- Akka 클러스터를 컨테이너 오케스트레이션(container orchestration) 환경에 배포하는 구체적인 방법 → "Deploying Akka Cluster to Kubernetes" 가이드 참고
- 쿠버네티스 환경에서 클러스터 구성 시 핵심 기능
  - 클러스터 부트스트랩(Cluster Bootstrap): Akka Cluster Bootstrap이 [Akka Discovery](https://doc.akka.io/libraries/akka-management/current/discovery/index.html)를 사용해 동료 노드(peer node)를 발견 → 클러스터 형성(form) 또는 기존 클러스터 합류(join) 지원. 쿠버네티스 인프라와 함께 동작
  - 롤링 업데이트(Rolling Updates): 무중단 배포를 위해 Akka Management의 "Kubernetes Rolling Updates" 기능과 `app-version` 관련 기능 활성화

---

#### CPU 설정

- 쿠버네티스에 Akka 애플리케이션 배포 시 CPU 제한(`resources.limits.cpu`) 미설정 권장
- CPU 제한 대신 CPU 요청(`resources.requests.cpu`) 사용 → CFS(Completely Fair Scheduler) 스케줄러로 인한 스로틀링 문제 방지
- JVM의 프로세서 개수 감지 방식
  - CPU 제한 설정 시 → 그 값 사용
  - CPU 제한 미설정 시 → 노드의 사용 가능한 모든 CPU 사용 → 스레드 풀(thread pool)이 노드 전체 CPU 수 기준으로 과도하게 커질 수 있음
- 조절 방법: `-XX:ActiveProcessorCount` 옵션으로 JVM이 감지하는 프로세서 개수 직접 지정

> 예시 시나리오: Akka 애플리케이션이 16 CPU 노드에 배포되고 CPU 요청으로 2를 사용하는 경우 → JVM 옵션에 `-XX:ActiveProcessorCount=4` 추가 → 스레드 풀이 4 CPU에 맞게 크기 조정됨

- 노드의 물리적 CPU 수(16)·요청 값(2)에 그대로 의존하지 않고 애플리케이션 특성에 맞는 값(여기서는 4)을 명시적으로 지정하는 방식

---

#### 메모리 설정

- 메모리: 요청 값(`resources.requests.memory`)과 제한 값(`resources.limits.memory`)을 동일한 값으로 설정하는 것이 모범 사례
- 힙(heap) 크기는 메모리 제한에 대한 상대 비율로 설정 → `-XX:InitialRAMPercentage`·`-XX:MaxRAMPercentage` 옵션 사용

> 힙 비율(heap percentage)은 사용 가능한 메모리의 70%로 설정 권장

- 목적: 힙 외 영역(non-heap area, 스택·메타스페이스·다이렉트 버퍼 등)을 위한 여유 공간 확보
  - 메모리 제한이 더 낮은 경우 비율을 더 작게 조정 필요

---

#### Docker 컨테이너 배포

- Akka는 Docker 환경에서도 잘 동작 → 특히 NAT(네트워크 주소 변환) 시나리오와 관련해 네트워크 설정 주의 필요
  - 노드가 서로 다른 내부/외부 IP 주소를 가지는 경우 → 리모팅(remoting) 설정을 "Akka behind NAT or in a Docker container" 가이드에 따라 구성
- 메모리: Docker는 컨테이너 메모리 제한을 자동 감지 → 쿠버네티스와 동일하게 `-XX:InitialRAMPercentage`·`-XX:MaxRAMPercentage` 사용, 힙 외 영역을 고려해 힙은 사용 가능한 메모리의 70%로 설정
- CPU: CFS 스케줄러와 멀티스레드 애플리케이션 사이의 충돌을 피하기 위해 `--cpus`·`--cpu-quota` 플래그 사용 회피 권장
  - 대신 상대적 가중치(relative weighting)를 부여하는 `--cpu-shares` 사용
  - CPU 쿼터(quota)가 존재하는 환경이라면 → JVM이 자원을 과도하게 크게 할당하지 않도록 `-XX:ActiveProcessorCount`도 함께 설정

---

### 운영(Operating)

#### 클러스터 시작하기

##### 클러스터 부트스트랩(Cluster Bootstrap)

- 쿠버네티스·AWS·Google Cloud·Azure·Mesos 등 클라우드 플랫폼에 클러스터 배포 시 → 노드 자동 발견(discovery) 기능이 있으면 클러스터 합류(join) 과정이 크게 단순해짐
- Akka Management 라이브러리가 이 역할을 담당하는 Cluster Bootstrap 모듈 제공

> 중요한 설정 참고 사항: Akka를 Docker 안에서 실행하거나 노드가 서로 다른 내부/외부 IP 주소를 가지는 환경에서는 리모팅(remoting)을 "Akka behind NAT or in a Docker container" 가이드에 따라 구성 필요

---

#### 클러스터 중지하기

- 클러스터를 정상적으로 중지(graceful shutdown)하는 절차 → "롤링 업데이트(Rolling Updates) 및 코디네이티드 셧다운(Coordinated Shutdown)" 절에 문서화됨

---

#### 클러스터 관리 도구

##### HTTP API

- 클러스터 관리 기능은 Akka Management를 통해 HTTP로 접근 가능

##### JMX 모니터링

- 클러스터 정보는 루트 이름(root name) `akka.Cluster` 아래의 JMX MBean으로 제공 → JConsole·JVisualVM 같은 도구로 확인 가능
- JMX를 통해 운영자(operator)가 수행 가능한 작업
  - 클러스터 멤버십(membership) 확인
  - 노드 상태(status) 모니터링
  - 멤버 역할(role) 조회
  - 기존 클러스터에 노드 추가
  - 노드를 다운(down) 상태로 표시
  - 노드에게 클러스터를 떠나도록(leave) 지시
- 노드 주소 형식

```
akka://actor-system-name@hostname:port
```

---

#### 모니터링과 관측 가능성(Observability)

- 로그 모니터링·플랫폼 제공 도구 외에도 Akka Insights(Akka Subscription을 통해 이용 가능)가 런타임 통찰(insight) 제공
  - 액터(Actor)·클러스터(Cluster)·HTTP 및 관련 컴포넌트에 대한 메트릭(metrics)·이벤트(events)·분산 추적(distributed tracing) 포함

---

### 롤링 업데이트(Rolling Updates)

#### 개요

> "롤링 업데이트(rolling update)란 시스템의 한 버전을 무중단(without downtime)으로 다른 버전으로 교체하는 과정이다."

- 적용 대상: 코드 변경 · Akka 새 버전 등 의존성(dependency) 업그레이드 · 설정(configuration) 변경
- 롤링 업데이트는 특히 상태를 가지는(stateful) Akka 클러스터에서 큰 가치를 가짐
  - 블루-그린 배포(blue-green deployment)처럼 업데이트 중 두 클러스터를 병렬로 실행할 수 없는 경우에 유용

---

#### 정상 종료(Graceful shutdown)

- 롤링 업데이트는 클러스터에서 노드가 정상적으로 떠나도록(graceful departure) 코디네이티드 셧다운(Coordinated Shutdown) 활용 필요
  - 노드가 자신을 `Exiting`(탈퇴 중) 상태로 감지하면 SIGTERM 신호에 의해 자동 활성화됨
- 쿠버네티스는 노드 종료 시 SIGTERM 신호 전송 → JVM 래퍼 스크립트(wrapper script)가 이 신호를 JVM에 올바르게 전달(forward)하도록 보장 필요
- 클러스터 싱글톤(Cluster Singleton)·클러스터 샤딩(Cluster Sharding)은 정상 종료를 자동 처리

##### 비정상 종료(Ungraceful shutdown)

- 네트워크 장애 등으로 노드를 수동으로 Down 상태로 설정해야 하는 경우 존재
- 지원 기능: Cluster Downing 기능 · 스플릿 브레인 리졸버(Split Brain Resolver)
  - 스플릿 브레인 리졸버는 네트워크 분할(partition)이나 장애 상황에서도 클러스터가 일관되게 동작하도록 보장

##### 동시에 재배포할 노드 수(Number of nodes to redeploy at the same time)

- 롤링 업데이트는 본질적으로 일부 노드를 점진적으로 교체하는 과정 → 한 번에 종료/재배포하는 노드 수를 조절해 클러스터 가용성·일관성 유지 필요
  - 한꺼번에 너무 많은 노드를 교체하면 → 데이터 이동·멤버십 변경 부담 증가

---

#### 직렬화 호환성(Serialization compatibility)

- 롤링 업데이트 중 주의 필요한 두 가지 핵심 영역
  1. 구 버전 노드와 신 버전 노드 간의 원격 메시지 프로토콜(remote message protocol) 호환성
  2. 이벤트(event)와 스냅샷(snapshot)의 직렬화 포맷(serialization format) 영속성 호환성
- 시스템의 직렬화 예외(exception) 처리 방식

> "`fromBinary`에서 `java.io.NotSerializableException`이 발생하면 이는 일시적인(transient) 문제로 간주되어, 문제가 로그로 남고 해당 메시지는 폐기(drop)된다."

- 그 외 다른 예외들은 전송(transport)의 손상을 의미할 수 있음 → 해당 연결(connection)을 끊음
- 무중단으로 직렬화 전략을 진화(evolve)시키려면 → 직렬화기(serializer)를 전환하는 2단계 접근법(two-step approach)과 영속성 스키마 진화(persistence schema evolution) 가이드 참고

---

#### 클러스터 샤딩(Cluster Sharding)

- 롤링 업데이트 중 할당 전략(allocation strategy)에 따라 샤딩된 엔티티(sharded entity)들이 재배치(relocate)될 수 있음
- 구 버전 노드와 신 버전 노드 구분: `app-version` 설정 속성 사용

```
akka.cluster.app-version = 1.2.3
```

> "`LeastShardAllocationStrategy`는 롤링 업데이트 중에 구 버전 노드(old nodes)에 샤드를 할당하는 것을 피한다."

- 버전 비교는 `Version` 유틸리티 클래스를 통해 표준 규칙(semantic versioning 관례)에 따라 이루어짐
- 쿠버네티스 배포에서는 Akka Management의 app-version 기능을 활성화 → 배포 리비전 어노테이션(deployment revision annotation)으로부터 버전 자동 도출 권장
- 롤링 업데이트 중에는 리밸런싱(rebalancing) 비활성화
  - 중지된 노드의 샤드는 메시지가 도착할 때 신 버전 노드로 마이그레이션되기 때문
  - Akka Management 사용 시 클러스터 샤딩에 대한 헬스 체크(health check) 권장 → 초기화 완료까지 트래픽 유입 지연 가능
- 다음 변경은 전체 클러스터 재시작(full cluster restart) 필요
  - `extractShardId` 함수
  - 샤드 영역 역할(shard region role)
  - 영속성 모드(persistence mode)
  - `number-of-shards` 설정

---

#### 클러스터 싱글톤(Cluster Singleton)

> "클러스터 싱글톤(Cluster singleton)은 항상 가장 오래된 노드(oldest node)에서 실행된다."

- 싱글톤 마이그레이션 부담 최소화 방법: 가장 오래된 노드를 마지막에 업그레이드 → 전체 업데이트 주기 동안 싱글톤이 단 한 번만 이동
- 쿠버네티스에서 롤링 업데이트 전략을 사용하는 배포의 경우 → Akka Management의 Kubernetes Rolling Updates 기능을 활성화 → 파드(pod)가 선호되는 순서(preferred order)로 삭제되도록 가능

---

#### 설정 호환성 검사(Configuration compatibility check)

- 롤링 업데이트 중에는 기존 노드의 설정이 클러스터 호환성 검사(compatibility check)를 통과해야 함
- 클래식(Classic)에서 타입드 액터(Typed Actor)로 마이그레이션하는 경우 필요한 2단계 접근법
  1. `akka.cluster.configuration-compatibility-check.enforce-on-join = off`로 배포
  2. `enforce-on-join = on`으로 재배포

---

#### Akka 버전 마이그레이션과 롤링 업데이트

##### Java 직렬화에서 Jackson으로

- Akka 2.5에서 2.6으로의 마이그레이션은 네 단계의 롤링 업데이트로 진행
- 1단계(Stage 1): `akka.actor.allow-java-serialization=on` 상태로 2.6.0으로 업데이트
- 2단계(Stage 2): 직렬화는 활성화하지 않고 역직렬화(deserialization) 지원 준비
  - Jackson 문서에 따라 마커 인터페이스(marker interface)와 어노테이션(annotation) 추가
  - 먼저 새로운 클러스터에서 테스트
  - `serialization-bindings`에서 마커 인터페이스 바인딩 제거
  - `akka.serialization.jackson.allowed-class-prefix=["com.myapp"]` 설정
- 3단계(Stage 3): Jackson 직렬화 활성화
  - 마커 인터페이스 바인딩을 Jackson 직렬화기에 추가
  - `allowed-class-prefix` 설정 제거
  - 이 단계에서 구 버전 노드는 계속 Java 직렬화로 메시지 전송, 신 버전 노드는 Jackson으로 메시지 전송
- 4단계(Stage 4): Java 직렬화 비활성화
  - `allow-java-serialization` 설정 제거
  - 수정했다면 `warn-about-java-serializer-usage` 설정도 제거

##### Receptionist를 사용하는 Akka Typed

- Receptionist 또는 Cluster Receptionist를 사용하며 2.5에서 2.6으로 마이그레이션하는 경우 → 롤링 업데이트 동안 서로 다른 버전 사이에서 정보 전파(dissemination)가 이루어지지 않음
  - 구 버전 노드가 모두 제거되고 나면 기능 정상 복구

---

#### 전체 종료가 필요한 경우

- 다음 변경은 롤링 업데이트가 아니라 전체 클러스터 종료(full shutdown) 필요

##### 클러스터 샤딩 설정 변경

- 다음 변경은 전체 클러스터 재시작 필요
  - `extractShardId` 함수 변경
  - 샤드 영역 역할(shard region role) 변경
  - 영속성 모드(persistence mode) 변경(모든 노드에서 일관되게 유지되어야 함)
  - `number-of-shards` 조정(참고: 클러스터 노드 수의 변경은 샤드 수의 변경을 요구하지 않음)

##### 클러스터 설정 변경

> "SBR(Split Brain Resolver) 전략을 변경하는 경우 전체 재시작이 필요하다."

##### PersistentFSM에서 EventSourcedBehavior로 마이그레이션

- 클러스터 샤딩과 함께 PersistentFSM에서 EventSourcedBehavior로 마이그레이션하는 경우 → 샤드가 노드 버전 간에 재배치될 수 있음 → 전체 종료 필요

##### 클래식 리모팅(Classic Remoting)에서 Artery로 마이그레이션

- 프로토콜 차이로 인해 클래식 리모팅에서 Artery로 마이그레이션할 때는 롤링 업데이트 미지원

##### 리모팅 전송(transport) 변경

- 리모팅 전송을 변경하는 경우 롤링 업데이트 미지원

##### 클래식 샤딩에서 타입드 샤딩으로 마이그레이션

- 클래식 샤딩(classic sharding)에서 타입드 샤딩(typed sharding)으로 마이그레이션하기 위한 3단계 롤링 업데이트 절차 존재 → 관련 샘플 PR에 자세히 설명됨

---

### 네이티브 이미지(Native Image)

#### 개요

- Akka로 GraalVM 네이티브 이미지(native image)를 빌드하는 것은 로컬(local) 액터 시스템 애플리케이션과 클러스터(cluster) 액터 시스템 애플리케이션 모두 지원

> "Akka의 대부분의 내장(built-in) 기능은 그대로 사용할 수 있지만, 일부 기능은 사용할 수 없으며, 일부 확장 지점(extension point)은 리플렉션(reflective) 접근이 동작하도록 추가 메타데이터(metadata)가 필요하다."

- 여러 Akka 프로젝트와 빌드 도구 설정을 포함한 종합적인 샘플: [Akka Edge Documentation](https://doc.akka.io/libraries/akka-edge/current/lightweight-deployments.html#graalvm-native-image)
  - 빌드 도구(sbt·Maven·Gradle)별 구체적인 네이티브 이미지 빌드 설정도 이 문서 참고

---

#### 지원되지 않는 기능

- 다음 기능은 네이티브 이미지에서 기본 상태로는 사용 불가
  - Lightbend Telemetry
  - Aeron UDP 리모팅
  - 테스트킷(Testkits)
  - LevelDB 및 InMem Akka Persistence 플러그인
  - Akka Distributed Data를 위한 영구 저장소(durable storage)
  - Scala 3

---

#### 추가 메타데이터가 필요한 기능

> "서드파티(third-party) 라이브러리나 사용자 코드가 제공하는 커스텀 구현을 애플리케이션 설정 파일의 항목을 통해 Akka에 끼워넣는(plug in) 모든 기능은, 리플렉션 메타데이터를 명시적으로 추가해야 한다."

- 예시
  - 커스텀 직렬화기(Custom Serializers)
  - 커스텀 메일박스 타입(Custom Mailbox Types)
  - Akka Persistence 플러그인
  - Akka Discovery 구현체
  - Akka Lease 구현체

---

#### Jackson 직렬화

- `JsonSerializable`·`CborSerializable` 마커 트레이트(marker trait) 사용 시 → 해당 메시지 타입들이 자동으로 리플렉션을 위해 등록(register)됨
- 주의 사항
  - 원시 타입(primitive)·표준 라이브러리 타입·Akka 타입만 포함하는 메시지는 자동 등록됨
  - 복잡한 메시지 구조·특수한 Jackson 어노테이션 사용 시 신중한 테스트 필요
  - 제네릭(generic)에서 타입 파라미터로만 참조되는 타입(예: `List[MyClass]`)은 명시적으로 마커 트레이트를 구현하거나 `reflect-config.json`에 항목 추가 필요
  - Scala 표준 라이브러리의 열거형(enumeration)은 기본 미지원
  - 커스텀 마커 트레이트 사용 시 → 해당 트레이트·구체 메시지 타입(concrete message type)·필드 타입(field type) 모두 리플렉션 메타데이터 필요
  - `JacksonMigration` 구현체는 리플렉션 메타데이터에 반드시 나열 필요
  - `akka.serialization.jackson.jackson-modules` 설정을 통해 추가된 ObjectMapper들은 리플렉션 메타데이터 항목 필요

##### 서드파티 직렬화기(Third-Party Serializers)

- 서드파티 직렬화기는 직렬화기 구현체·`serialization-bindings`에 등록된 타입들·직렬화기 로직에 따라 메시지별(per-message) 메타데이터까지 리플렉션 메타데이터 필요 가능

---

#### 기타 확장 지점

##### 확장(Extensions)

- 설정을 통해 로드되는 클래식 및 타입드 확장(`akka.extensions`, `akka.actor.typed.extensions`, `akka.actor.library-extensions`, `akka.actor.typed.library-extensions`)은 클래스 및 생성자(constructor) 정보를 포함한 리플렉션 메타데이터 항목 필요

##### Akka Persistence 이벤트 어댑터(Event Adapters)

- 애플리케이션에서 정의한 이벤트 어댑터(event adapter)는 클래스 및 생성자 정보와 함께 리플렉션 메타데이터에 나열 필요

##### 리플렉션 기반 클래식 액터 생성

- 타입 기반(type-based) 또는 클래스 기반(class-based) 방식으로 `Props`를 사용하는 클래식 액터는 → 조회(lookup) 및 전달된 파라미터에 맞는 생성자(constructor)에 대한 리플렉션 항목 필요
- 문서 언급: "리플렉션 요구 사항을 피하기 위해 람다 팩토리(lambda factory)를 사용하는 것이 더 쉬운 경로"
- 리플렉션 기반 접근은 주로 클래식 원격 배포(classic remote deploy) 기능에서 필요

---

#### 로깅

- 로깅에 `akka-slf4j` 사용 시 → 선택한 구체 로거(concrete logger)는 추가 설정이 필요할 가능성 높음
- Akka는 특정 로거를 강제하지 않지만 샘플에서는 `logback-classic`이 흔하게 사용됨 → Akka는 이에 대한 리플렉션 메타데이터 제공
  - logback을 사용하는 프로젝트에는 네이티브 이미지 플래그 `--initialize-at-build-time=ch.qos.logback` 필요
- 비동기(async) logback 어펜더(appender)의 경우 → `ch.qos.logback.classic.AsyncAppender`를 사용하지 않거나, 빌드 시점(build time)에 스레드를 시작하지 않는 지연(lazy) 버전 선언
  - 예제 구현: Akka Projections의 엣지 복제(edge replication) 샘플

---

### 참고 자료

- [Akka 공식 문서](https://doc.akka.io/libraries/akka-core/current/)

---

## Akka 프로젝트 정보와 보안

> 원본: https://doc.akka.io/libraries/akka-core/current/project/index.html

---

### 목차

1. [바이너리 호환성 규칙 (Binary Compatibility Rules)](#1-바이너리-호환성-규칙-binary-compatibility-rules)
2. ["변경될 수 있음"으로 표시된 모듈 (Modules marked "May Change")](#2-변경될-수-있음으로-표시된-모듈-modules-marked-may-change)
3. [자주 묻는 질문 (FAQ)](#3-자주-묻는-질문-faq)
4. [보안 공지 (Security Announcements)](#4-보안-공지-security-announcements)
5. [마이그레이션 가이드 (Migration Guides)](#5-마이그레이션-가이드-migration-guides)
6. [라이선스 (Licenses)](#6-라이선스-licenses)
7. [참고 자료](#7-참고-자료)

---

### 1. 바이너리 호환성 규칙 (Binary Compatibility Rules)

- Akka는 여러 버전에 걸쳐 하위 바이너리 호환성(backwards binary compatibility) 유지
  - 새로운 JAR가 이전 JAR를 그대로 대체할 수 있는 드롭인 교체(drop-in replacement)가 됨을 의미(Scala 인라이너(inliner) 사용 시는 제외)
- 핵심 원칙: 애플리케이션을 다시 컴파일하지 않고도 의존하는 Akka 라이브러리의 JAR 교체만으로 업그레이드 수행 가능

#### 1.1 호환성 프레임워크

- 바이너리 호환성이 유지되는 경우
  - 마이너 버전(minor version)과 패치 버전(patch version) 사이에서 유지(단, 이러한 호환성 정의는 2.4.0부터 새롭게 정비됨)
- 바이너리 호환성이 유지되지 않는(보장되지 않는) 경우
  - 메이저 버전(major version) 사이
  - "변경될 수 있음(may change)"으로 표시된 모듈
  - 아래에 명시된 특정 예외 항목들

#### 1.2 버전 체계의 변화 (Version Scheme Evolution)

- Akka의 버전 명명 체계 전환
  - 2.3.x까지: `epoch.major.minor` 체계
  - 2.4.0 이후: `major.minor.patch` 체계
- 이 변경으로 더 강력한 호환성 보장(stronger compatibility guarantees) 가능해짐 → 그 결과 2.4.x 계열은 2.3.x 릴리스에 대해 하위 호환성(backwards compatibility) 달성

#### 1.3 실제 예시 (Practical Examples)

- 공식 문서가 설명하는 유효한 업그레이드 경로 예시
  - `2.2.0 → 2.2.1 → ... → 2.2.x`(패치 업그레이드 허용)
  - `2.3.x → 2.4.x`(특별한 마이그레이션 사례)
  - `2.4.0 → 2.5.x`(2.4 이후의 마이너 업그레이드 허용)

#### 1.4 주요 제약 사항 (Key Restrictions)

- 혼합 버전 사용 금지(Mixed versioning prohibited): 함께 릴리스된 Akka 모듈들은 반드시 함께 업그레이드 필요
  - 예: Actor 2.6.2와 Cluster 2.5.3을 함께 사용하는 것은 각 모듈이 개별적으로는 바이너리 호환성을 주장하더라도 이 규칙 위반
- 한 프로젝트 내에서 사용하는 모든 Akka 모듈은 동일한 버전 번호 유지 필요

#### 1.5 주목할 만한 예외 (Notable Exceptions)

- 보안 취약점(security vulnerabilities)으로 인해 마이너 릴리스에서 호환성을 깨는 변경(breaking changes)이 불가피하게 발생 가능
- 바이너리 호환성 보장에서 제외되는 모듈
  - `-testkit` 모듈
  - `-tck` 모듈
  - "변경될 수 있음(may change)"으로 표시된 모든 모듈

#### 1.6 폐기 정책 (Deprecation Policy)

- 폐기(deprecated)된 메서드는 최소한 하나의 완전한 마이너 버전 주기(at least one complete minor version cycle) 동안 유지(다만 드물게 예외 존재 가능)

#### 1.7 API 안정성 표시자 (API Stability Markers)

- 세 가지 핵심 애너테이션(annotation)이 API의 안정성 상태 식별
  - `@InternalApi` — 호환성을 전혀 보장하지 않으며 사전 통지 없이 변경될 수 있는 내부 API
  - `@ApiMayChange` — 불안정한(unstable) API. 자세한 내용은 ["변경될 수 있음"으로 표시된 모듈](#2-변경될-수-있음으로-표시된-모듈-modules-marked-may-change) 섹션 참고
  - `@DoNotInherit` — 폐쇄형 세계 가정(closed-world assumption)을 따르며 사용자가 상속(extends)할 경우 호환성이 깨질 위험이 있는 타입 표시

#### 1.8 검증 절차 (Verification Process)

- Akka는 모든 풀 리퀘스트(pull request)에 대해 바이너리 호환성을 자동으로 검증하기 위해 MiMa(Migration Manager for Scala, Lightbend가 유지 관리) 사용

#### 1.9 Scala 직렬화 관련 주의 사항

- Java 직렬화(Java serialization)는 서로 다른 Scala 메이저 버전(Scala major versions) 사이에서 호환성 보장하지 않음

---

### 2. "변경될 수 있음"으로 표시된 모듈 (Modules marked "May Change")

#### 2.1 개요 (Overview)

- Akka는 새로운 모듈·API를 즉시 바이너리 호환성 보장 아래로 동결(freeze)하지 않으면서도 조기에 릴리스할 수 있도록 "변경될 수 있음(may change)" 지정 방식 도입

#### 2.2 정의와 범위 (Definition and Scope)

- 공식 문서에 따르면 "변경될 수 있음(may change)"은 어떤 API·모듈이 얼리 액세스(early access) 모드에 있음을 의미
- 특성
  - Lightbend가 명시적으로 달리 언급하지 않는 한 상업적으로 지원되지 않음(not commercially supported)
  - 마이너 릴리스 간에 바이너리 호환성 없음(not binary compatible)
  - 마이너 릴리스에서 사전 경고 없이 API가 깨질 수 있음(API may break)
  - 마이너 릴리스에서 완전히 중단(discontinued)될 수 있음

#### 2.3 API 수준의 지정 (API-Level Designation)

- 개별 공개(public) API에는 [`ApiMayChange`](https://doc.akka.io/japi/akka-core/2.10/akka/annotation/ApiMayChange.html) 애너테이션 부여 → 해당 API가 안정적인 모듈 API보다 적은 보장(fewer guarantees)을 가짐을 알림
- 이 방식으로 안정적으로 자리잡은 기존 모듈에 실험적인 Java 8 API를 도입하면서도 해당 API가 아직 동결되지 않았음(unfrozen)을 신호로 전달 가능

#### 2.4 근거 (Rationale)

> "기능을 조기에 릴리스하여 쉽게 사용할 수 있도록 만들고, 피드백을 바탕으로 개선하거나, 심지어 해당 모듈이나 API가 유용하지 않았다는 사실을 발견하기 위함이다."
>
> ("release features early and make them easily available and improve based on feedback, or even discover that the module or API wasn't useful.")

#### 2.5 현재 "변경될 수 있음"으로 표시된 모듈

- 현재 완전한 모듈 두 개에 이 지정이 붙어 있음
  1. 다중 노드 테스트(Multi Node Testing)
  2. 신뢰성 있는 전달(Reliable Delivery)

#### 2.6 마이그레이션 지원 (Migration Support)

- "변경될 수 있음" 모듈에 대해서도 마이그레이션 가이드(migration guidance) 제공 가능 → 사례별(case-by-case basis)로 개별적으로 결정됨

---

### 3. 자주 묻는 질문 (FAQ)

#### 3.1 Akka 프로젝트 관련 (Akka Project Section)

Q. Akka라는 이름은 어디에서 유래했나요? (Where does the name Akka come from?)

- 이 이름은 "스웨덴 북부 라포니아(Laponia) 지역에 있는 아름다운 스웨덴 산"에서 유래 → 이 산은 '라포니아의 여왕(The Queen of Laponia)'으로도 알려짐
  - 사미(Sámi) 신화에서 Akka는 "세상의 모든 아름다움과 선함을 상징하는 여신"을 의미
- AKKA라는 약어는 회문(palindrome) 구조를 통해 "Actor Kernel(액터 커널)"을 반영하기도 함
- 그 밖의 추가적인 의미
  - 셀마 라겔뢰프(Selma Lagerlöf)의 작품 "닐스의 신기한 여행(The Wonderful Adventures of Nils)"에 등장하는 거위 캐릭터
  - 핀란드어 및 인도 언어에서의 용어
  - 한 서체(typeface) 디자인의 이름
  - 모로코의 한 마을
  - 지구 근접 소행성(near-earth asteroid)

Q. 명시적인 생명주기(lifecycle)를 가진 리소스 (Resources with Explicit Lifecycle)

- 액터(Actor)·액터 시스템(ActorSystem)·머티리얼라이저(Materializer)는 명시적으로 해제해야 하는 리소스를 점유
  - 생성할 때마다 대응하는 `stop`·`terminate`·`shutdown` 호출 필요
  - 액터가 독립적으로 존재하며 고유한 생명주기를 갖도록 설계되었기 때문

Q. JVM 애플리케이션 또는 Scala REPL이 "멈춘(hanging)" 것처럼 보이는 이유 (JVM application or Scala REPL "hanging")

- 시스템이 자동으로 종료되지 않는 이유: JVM이 명시적으로 중단되기 전까지는 종료되지 않기 때문
- 정상적인 종료를 위해서는 실행 중인 애플리케이션 또는 Scala REPL 세션 내의 모든 ActorSystem을 명시적으로 종료(shutdown) 필요

#### 3.2 액터 관련 (Actors Section)

Q. 왜 `OutOfMemoryError`가 발생하나요? (Why OutOfMemoryError?)

- 흔한 원인: 메시지 흐름 제어(message flow control)의 실패
  - 순수 푸시(push) 기반 시스템에서 메시지 소비자(consumer)가 생산자(producer)보다 처리 속도가 느릴 경우 → 반드시 어떤 형태의 메시지 흐름 제어(flow control) 추가 필요

#### 3.3 클러스터 관련 (Cluster Section)

Q. 메시지 전달은 얼마나 신뢰할 수 있나요? (How reliable is the message delivery?)

- 프레임워크는 최대 한 번 전달(at-most-once delivery) 방식으로 동작 → 보장된 전달(no guaranteed delivery)이 아님
  - 다만 이 기반 위에 더 강력한 신뢰성을 구축(constructed above this foundation) 가능

#### 3.4 디버깅 관련 (Debugging Section)

Q. 디버그 로깅(debug logging)을 어떻게 켜나요? (How do I turn on debug logging?)

- 다음 설정 추가

```hocon
akka.loglevel = DEBUG
```

---

### 4. 보안 공지 (Security Announcements)

#### 4.1 주요 정보 (Key Information)

- 현재 버전(Current Version): 2.10.19
- 보안 공지 위치(Security Announcements Location): 모든 Akka 프로젝트에 대한 보안 공지 내용은 [akka.io/security](https://akka.io/security)로 통합됨

#### 4.2 보안 권고(Advisory) 수신 방법

- 사용자는 Google Groups를 통해 "Akka 보안 메일링 리스트(Akka security list)" 구독 필요
- 이 메일링 리스트는 매우 적은 양의 트래픽으로 운영 → 핵심 팀(core team)이 보안 보고를 처리하고 수정 사항을 공개적으로 제공한 이후에만 알림 배포

#### 4.3 취약점 보고 절차 (Vulnerability Reporting Process)

- 잠재적인 취약점은 공개 공시(public disclosure) 이전에 전용 비공개 보안 이메일 주소로 먼저 보고할 것을 강력히 권장
- 보고된 내용은 보안 팀이 검토 → 신속한 수정을 위해 보고자와 협력
- 연락처(Contact): security@akka.io

#### 4.4 수정된 보안 이슈 (Fixed Security Issues)

- 문서에 기록된 과거 취약점 세 가지
  1. Java 직렬화(Java Serialization) — Akka 2.4.17에서 해결됨(2017년 2월)
  2. Camel 의존성(Camel Dependency) — Akka 2.5.4에서 해결됨(2017년 8월)
  3. 난수 생성기(Random Number Generators) — `AES128CounterSecureRNG`/`AES256CounterSecureRNG` 관련, Akka 2.5.16에서 해결됨(2018년 8월)

#### 4.5 관련 문서 (Related Documentation)

- 보안 관련 가이드가 다루는 주제
  - Java 직렬화(Java serialization) 사용 시의 주의 사항 및 모범 사례
  - 원격 배포(remote deployment) 보호 장치
  - 원격 시스템 보안(remote system security) 프로토콜

---

### 5. 마이그레이션 가이드 (Migration Guides)

#### 5.1 제공되는 마이그레이션 가이드 (Available Migration Guides)

- 제공되는 마이그레이션 경로(migration path)
  1. 마이그레이션 가이드 2.9.x → 2.10.x(가장 최신)
  2. 마이그레이션 가이드 2.8.x → 2.9.x
  3. 마이그레이션 가이드 2.7.x → 2.8.x
  4. 마이그레이션 가이드 2.6.x → 2.7.x
  5. 마이그레이션 가이드 2.5.x → 2.6.x
  6. 이전 마이그레이션 가이드(Older Migration Guides)(그 이전 버전용 아카이브)

---

### 6. 라이선스 (Licenses)

#### 6.1 주요 라이선스: Business Source License 1.1

- Akka 2.10.19는 Business Source License 1.1(BSL 1.1) 하에서 라이선스 부여 → Lightbend, Inc.가 관리

BSL 1.1 주요 조건

- 라이선스 부여(License Grant): 사용자는 라이선스 대상 저작물(Licensed Work)을 "복사(copy)하고, 수정(modify)하고, 2차 저작물(derivative works)을 생성하고, 재배포(redistribute)하며, 비프로덕션 용도로 사용(make non-production use)"할 권리 부여받음. 프로덕션 용도의 사용은 추가 사용 허가(Additional Use Grant)를 통해 제한적으로 허용
- 변경 날짜(Change Date): 2029년 6월 4일
- 전환 라이선스(Conversion License): Apache License 2.0(변경 날짜(Change Date) 또는 최초 공개 배포 후 4주년 중 먼저 도래하는 시점에 자동으로 적용됨)
- Play Framework를 위한 추가 사용 허가(Additional Use Grant for Play Framework): akka-streams를 포함하는 Play Framework 바이너리를 사용하는 개발자는 해당 바이너리를 애플리케이션 개발에 사용 가능하나 그 범위는 "Play Framework 웹소켓(websocket)에 연결하거나 Play Framework의 요청/응답 본문(request/response bodies)에 연결하는 것"으로 제한
- 주요 제한 사항(Key Restrictions): 사용자는 승인 없이 소프트웨어를 역공학(reverse engineer)하거나, 수정(modify)하거나, 배포(distribute)하거나, 양도(transfer)할 수 없으며, 독점 표시(proprietary notices) 제거 불가

#### 6.2 문서 및 테스트 소스 라이선스 (Documentation and Test Sources License)

- 별도의 Lightbend Commercial Software License Agreement(Lightbend 상용 소프트웨어 라이선스 계약)가 문서와 테스트 소스 코드를 규율
- 특징
  - 제한적이고(limited), 비독점적이며(non-exclusive), 양도 불가능한(non-transferable) 바이너리 실행 권한
  - 역공학(reverse engineering), 수정(modification), 재배포(redistribution)의 금지
  - 오픈 소스 소프트웨어(Open Source Software) 구성 요소는 각자의 원래 라이선스를 따름
  - 상품성(merchantability) 및 특정 목적 적합성(fitness for purpose)을 포함한 보증의 부인(Disclaimer of warranties)
  - 미화 500달러(USD)의 책임 한도(liability cap)
  - 캘리포니아 주법(California law)의 적용 및 중재(arbitration) 요구

#### 6.3 커미터 라이선스 계약 (Committer License Agreement)

- 모든 Akka 커미터(committer)는 Akka 커미터 라이선스 계약(Akka Committer License Agreement, CLA) 체결 → Lightbend 웹사이트에서 온라인으로 서명 가능

#### 6.4 의존성 라이선스 (Dependency Licenses)

- 개별 의존성(dependency)의 라이선스는 프로젝트 빌드 파일인 `AkkaBuild.scala`(버전 2.10.19)에 문서화됨 → 각 의존성 선언 옆에 라이선스 정보가 주석(comment) 형태로 표시

---

### 7. 참고 자료

- [Akka 공식 문서](https://doc.akka.io/libraries/akka-core/current/)
- [바이너리 호환성 규칙 (Binary Compatibility Rules)](https://doc.akka.io/libraries/akka-core/current/common/binary-compatibility-rules.html)
- ["변경될 수 있음"으로 표시된 모듈 (May Change)](https://doc.akka.io/libraries/akka-core/current/common/may-change.html)
- [자주 묻는 질문 (FAQ)](https://doc.akka.io/libraries/akka-core/current/additional/faq.html)
- [보안 공지 (Security Announcements)](https://doc.akka.io/libraries/akka-core/current/security/index.html)
- [마이그레이션 가이드 (Migration Guides)](https://doc.akka.io/libraries/akka-core/current/project/migration-guides.html)
- [라이선스 (Licenses)](https://doc.akka.io/libraries/akka-core/current/project/licenses.html)
- [Akka 보안 페이지](https://akka.io/security)

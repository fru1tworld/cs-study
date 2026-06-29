# Akka 패키징, 배포, 운영

> 원본: https://doc.akka.io/libraries/akka-core/current/additional/

---

## 목차

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

## 패키징(Packaging)

### 개요

Akka를 사용하는 가장 단순한 방법은 Akka의 jar 파일들을 클래스패스(classpath)에 추가하는 것입니다.

그러나 분석 클러스터(analytics cluster) 등에 배포하기 위해 "팻 jar(fat jar)"를 생성하는 경우에는 추가적인 설정이 필요합니다. 그 이유는 각 Akka jar에는 기본값(default value)들을 담은 `reference.conf` 리소스가 포함되어 있는데, 팻 jar를 만들 때 여러 jar의 동일한 이름의 리소스가 서로 덮어쓰지 않고 올바르게 병합(merge)되어야 하기 때문입니다.

다시 말해, 단순히 모든 클래스와 리소스를 하나의 jar로 합치면 마지막에 들어간 jar의 `reference.conf`만 남고 나머지 Akka 모듈의 기본 설정은 사라지게 됩니다. 이를 방지하려면 빌드 도구별로 `reference.conf`(및 버전 정보를 담은 `version.conf`)를 이어붙이는(append) 변환(transformer)을 적용해야 합니다.

---

### sbt: 네이티브 패키저(Native Packager)

Akka 애플리케이션의 배포본(distribution)을 만들기 위해 [sbt-native-packager](https://github.com/sbt/sbt-native-packager)를 사용하는 것을 권장합니다.

먼저 `project/build.properties` 파일에 sbt 버전을 정의합니다.

```
sbt.version=1.3.12
```

다음으로 `project/plugins.sbt` 파일에 네이티브 패키저 플러그인을 추가합니다.

```
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.5")
```

이후에는 sbt-native-packager 공식 문서의 [`JavaAppPackaging`](https://www.scala-sbt.org/sbt-native-packager/archetypes/java_app/index.html) 안내를 따릅니다.

---

### Maven: Shade 플러그인

Maven 사용자는 리소스 변환기(Resource Transformer)와 함께 [Apache Maven Shade 플러그인](https://maven.apache.org/plugins/maven-shade-plugin/)을 활용할 수 있습니다.

`AppendingTransformer`를 사용하여 여러 jar의 `reference.conf`와 `version.conf` 파일을 병합하고, `ManifestResourceTransformer`를 사용하여 메인 클래스(main class) 진입점을 지정합니다.

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

위 설정에서 핵심은 다음과 같습니다.

- 두 개의 `AppendingTransformer`는 각각 `reference.conf`와 `version.conf`를 병합 대상으로 지정합니다. 이를 통해 여러 Akka 모듈의 기본 설정이 손실되지 않습니다.
- `ManifestResourceTransformer`는 매니페스트(manifest)에 `Main-Class` 항목을 추가하여, 생성된 jar를 직접 실행할 수 있게 합니다. 예시에서는 `myapp.Main`을 진입점으로 지정하였습니다.

---

### Gradle: Shadow 플러그인

Gradle 사용자는 일반적으로 Java 플러그인의 `Jar` 태스크를 사용합니다. 팻 jar 생성에는 [Shadow 플러그인](https://github.com/johnrengelman/shadow)을 권장하며, 표준 Groovy DSL과 Kotlin DSL 양쪽 문법을 모두 지원합니다.

**Groovy DSL** 예시입니다.

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

**Kotlin DSL** 예시입니다.

```kotlin
tasks.withType<ShadowJar> {
    val newTransformer = AppendingTransformer()
    newTransformer.resource = "reference.conf"
    transformers.add(newTransformer)
}
```

두 경우 모두 `reference.conf`(및 `version.conf`)를 이어붙이도록(append) 설정하여, 팻 jar에 포함된 여러 Akka 모듈의 기본 설정이 올바르게 병합됩니다.

---

## 배포(Deploying)

### 쿠버네티스(Kubernetes)에 배포하기

Akka 클러스터를 컨테이너 오케스트레이션(container orchestration) 환경에 배포하는 구체적인 방법은 "Deploying Akka Cluster to Kubernetes(쿠버네티스에 Akka 클러스터 배포하기)" 가이드를 참고하십시오.

쿠버네티스 환경에서 클러스터를 구성할 때 핵심이 되는 기능들은 다음과 같습니다.

- **클러스터 부트스트랩(Cluster Bootstrap)**: Akka Cluster Bootstrap은 [Akka Discovery](https://doc.akka.io/libraries/akka-management/current/discovery/index.html)를 사용하여 동료 노드(peer node)들을 발견하고, 이를 통해 클러스터를 형성(form)하거나 기존 클러스터에 합류(join)하는 과정을 도와줍니다. 쿠버네티스 인프라와 함께 동작합니다.
- **롤링 업데이트(Rolling Updates)**: 무중단 배포를 위해 Akka Management에서 제공하는 "Kubernetes Rolling Updates" 기능과 `app-version` 관련 기능을 활성화하십시오.

---

### CPU 설정

쿠버네티스에 Akka 애플리케이션을 배포할 때, **CPU 제한(`resources.limits.cpu`)을 설정하지 않는 것**이 권장됩니다. CPU 제한 대신 **CPU 요청(`resources.requests.cpu`)을 사용**하십시오. 이는 CFS(Completely Fair Scheduler) 스케줄러로 인해 발생할 수 있는 스로틀링(throttling) 문제를 방지하기 위함입니다.

JVM은 프로세서 개수를 감지할 때, CPU 제한이 설정되어 있으면 그 값을 사용하고, 그렇지 않으면 노드의 사용 가능한 모든 CPU를 사용합니다. 따라서 CPU 제한을 설정하지 않으면 JVM이 노드의 전체 CPU 수를 기준으로 스레드 풀(thread pool)을 과도하게 크게 설정할 수 있습니다.

이를 적절히 조절하려면 `-XX:ActiveProcessorCount` 옵션으로 JVM이 감지하는 프로세서 개수를 직접 지정할 수 있습니다.

> 예시 시나리오: Akka 애플리케이션이 16 CPU 노드(16 CPU nodes)에 배포되고 있으며, CPU 요청(CPU request)으로 2를 사용하는 경우, JVM 옵션에 `-XX:ActiveProcessorCount=4`를 추가하여 스레드 풀이 4 CPU에 맞게 적절히 크기가 설정되도록 합니다.

노드의 물리적 CPU 수(16)나 요청 값(2)에 그대로 의존하지 않고, 애플리케이션 특성에 맞는 적절한 값(여기서는 4)을 명시적으로 지정하는 방식입니다.

---

### 메모리 설정

메모리에 대해서는 요청 값(`resources.requests.memory`)과 제한 값(`resources.limits.memory`)을 **동일한 값으로 설정**하는 것이 모범 사례(best practice)입니다.

힙(heap) 크기는 메모리 제한에 대한 상대 비율로 설정하기 위해 `-XX:InitialRAMPercentage`와 `-XX:MaxRAMPercentage` 옵션을 사용합니다.

> 힙 비율(heap percentage)은 사용 가능한 메모리의 **70%**로 설정하는 것이 권장됩니다.

이는 힙 외 영역(non-heap area, 예: 스택, 메타스페이스, 다이렉트 버퍼 등)을 위한 여유 공간을 확보하기 위함입니다. 다만 메모리 제한이 더 낮은 경우에는 비율을 더 작게 조정해야 할 수 있습니다.

---

### Docker 컨테이너 배포

Akka는 Docker 환경 안에서도 잘 동작하지만, 특히 NAT(네트워크 주소 변환) 시나리오와 관련하여 네트워크 설정에 주의가 필요합니다. 노드가 서로 다른 내부/외부 IP 주소를 가지는 경우, 리모팅(remoting) 설정을 "Akka behind NAT or in a Docker container" 가이드에 따라 구성해야 합니다.

**메모리**: Docker는 컨테이너의 메모리 제한을 자동으로 감지합니다. 쿠버네티스의 경우와 동일하게 `-XX:InitialRAMPercentage`와 `-XX:MaxRAMPercentage`를 사용하며, 힙 외 영역을 고려하여 힙은 사용 가능한 메모리의 **70%**로 설정합니다.

**CPU**: CFS 스케줄러와 멀티스레드 애플리케이션 사이의 충돌을 피하기 위해 `--cpus` 및 `--cpu-quota` 플래그 사용을 피하는 것이 권장됩니다. 대신 상대적 가중치(relative weighting)를 부여하는 `--cpu-shares`를 사용하십시오. 만약 CPU 쿼터(quota)가 존재하는 환경이라면, JVM이 자원을 과도하게 크게 할당하지 않도록 `-XX:ActiveProcessorCount`를 함께 설정하십시오.

---

## 운영(Operating)

### 클러스터 시작하기

#### 클러스터 부트스트랩(Cluster Bootstrap)

쿠버네티스(Kubernetes), AWS, Google Cloud, Azure, Mesos 등 클라우드 플랫폼에 클러스터를 배포할 때, 노드를 자동으로 발견(discovery)하는 기능이 있으면 클러스터 합류(join) 과정이 크게 단순해집니다. Akka Management 라이브러리는 바로 이 역할을 담당하는 Cluster Bootstrap 모듈을 제공합니다.

> **중요한 설정 참고 사항**: Akka를 Docker 안에서 실행하거나, 노드가 서로 다른 내부/외부 IP 주소를 가지는 환경에서는 리모팅(remoting)을 "Akka behind NAT or in a Docker container" 가이드에 따라 구성해야 합니다.

---

### 클러스터 중지하기

클러스터를 정상적으로 중지(graceful shutdown)하는 절차는 "롤링 업데이트(Rolling Updates) 및 코디네이티드 셧다운(Coordinated Shutdown)" 절에 문서화되어 있습니다.

---

### 클러스터 관리 도구

#### HTTP API

클러스터 관리 기능은 Akka Management를 통해 HTTP로 접근할 수 있습니다.

#### JMX 모니터링

클러스터 정보는 루트 이름(root name) `akka.Cluster` 아래의 JMX MBean으로 제공되며, JConsole이나 JVisualVM과 같은 도구로 확인할 수 있습니다.

JMX를 통해 운영자(operator)는 다음과 같은 작업을 수행할 수 있습니다.

- 클러스터 멤버십(membership) 확인
- 노드 상태(status) 모니터링
- 멤버 역할(role) 조회
- 기존 클러스터에 노드 추가
- 노드를 다운(down) 상태로 표시
- 노드에게 클러스터를 떠나도록(leave) 지시

노드는 다음과 같은 주소 형식을 사용합니다.

```
akka://actor-system-name@hostname:port
```

---

### 모니터링과 관측 가능성(Observability)

로그 모니터링이나 플랫폼이 제공하는 도구 외에도, **Akka Insights**(Akka Subscription을 통해 이용 가능)는 런타임에 대한 통찰(insight)을 제공합니다. 여기에는 액터(Actor), 클러스터(Cluster), HTTP 및 관련 컴포넌트에 대한 메트릭(metrics), 이벤트(events), 분산 추적(distributed tracing)이 포함됩니다.

---

## 롤링 업데이트(Rolling Updates)

### 개요

> "롤링 업데이트(rolling update)란 시스템의 한 버전을 무중단(without downtime)으로 다른 버전으로 교체하는 과정이다."

이는 코드 변경, Akka 새 버전 등 의존성(dependency) 업그레이드, 또는 설정(configuration) 변경 모두에 적용됩니다.

롤링 업데이트는 특히 상태를 가지는(stateful) Akka 클러스터에서 큰 가치를 가집니다. 블루-그린 배포(blue-green deployment)처럼 업데이트 중에 두 개의 클러스터를 병렬로 실행할 수 없는 경우에 유용하기 때문입니다.

---

### 정상 종료(Graceful shutdown)

롤링 업데이트는 클러스터에서 노드가 정상적으로 떠나도록(graceful departure) **코디네이티드 셧다운(Coordinated Shutdown)**을 활용해야 합니다. 코디네이티드 셧다운은 노드가 자신을 `Exiting`(탈퇴 중) 상태로 감지하면 SIGTERM 신호에 의해 자동으로 활성화됩니다.

쿠버네티스는 노드 종료 시 SIGTERM 신호를 전송하므로, JVM 래퍼 스크립트(wrapper script)가 이 신호를 JVM에 올바르게 전달(forward)하도록 보장해야 합니다. 클러스터 싱글톤(Cluster Singleton)과 클러스터 샤딩(Cluster Sharding)은 정상 종료를 자동으로 처리합니다.

#### 비정상 종료(Ungraceful shutdown)

네트워크 장애 등으로 인해 노드를 수동으로 Down 상태로 설정해야 하는 경우가 있습니다. 이를 위해 Cluster Downing 기능과 스플릿 브레인 리졸버(Split Brain Resolver)가 제공됩니다. 특히 스플릿 브레인 리졸버는 네트워크 분할(partition)이나 장애 상황에서도 클러스터가 일관되게 동작하도록 보장합니다.

#### 동시에 재배포할 노드 수(Number of nodes to redeploy at the same time)

롤링 업데이트는 본질적으로 일부 노드를 점진적으로 교체하는 과정이므로, 한 번에 종료/재배포하는 노드 수를 조절하여 클러스터의 가용성과 일관성을 유지해야 합니다. 한꺼번에 너무 많은 노드를 교체하면 데이터 이동과 멤버십 변경 부담이 커질 수 있습니다.

---

### 직렬화 호환성(Serialization compatibility)

롤링 업데이트 중에는 두 가지 핵심 영역에서 주의가 필요합니다.

1. 구 버전 노드와 신 버전 노드 간의 **원격 메시지 프로토콜(remote message protocol) 호환성**
2. 이벤트(event)와 스냅샷(snapshot)의 **직렬화 포맷(serialization format) 영속성** 호환성

시스템은 직렬화 예외(exception)를 상황에 따라 다르게 처리합니다.

> "`fromBinary`에서 `java.io.NotSerializableException`이 발생하면 이는 일시적인(transient) 문제로 간주되어, 문제가 로그로 남고 해당 메시지는 폐기(drop)된다."

그 외의 다른 예외들은 전송(transport)의 손상을 의미할 수 있으므로, 해당 연결(connection)을 끊습니다.

무중단으로 직렬화 전략을 진화(evolve)시키려면, 직렬화기(serializer)를 전환하는 **2단계 접근법(two-step approach)**과 영속성 스키마 진화(persistence schema evolution)에 대한 가이드를 참고하십시오.

---

### 클러스터 샤딩(Cluster Sharding)

롤링 업데이트 중에는 할당 전략(allocation strategy)에 따라 샤딩된 엔티티(sharded entity)들이 재배치(relocate)될 수 있습니다. 시스템은 구 버전 노드와 신 버전 노드를 구분하기 위해 `app-version` 설정 속성을 사용합니다.

```
akka.cluster.app-version = 1.2.3
```

> "`LeastShardAllocationStrategy`는 롤링 업데이트 중에 구 버전 노드(old nodes)에 샤드를 할당하는 것을 피한다."

버전 비교는 `Version` 유틸리티 클래스를 통해 표준 규칙(semantic versioning 관례)에 따라 이루어집니다.

쿠버네티스 배포에서는 Akka Management의 app-version 기능을 활성화하여 배포 리비전 어노테이션(deployment revision annotation)으로부터 버전을 자동으로 도출하도록 하는 것이 권장됩니다.

롤링 업데이트 중에는 리밸런싱(rebalancing)이 비활성화됩니다. 중지된 노드의 샤드는 메시지가 도착할 때 신 버전 노드로 마이그레이션되기 때문입니다. Akka Management를 사용하는 경우, 클러스터 샤딩에 대한 헬스 체크(health check)를 권장합니다. 이를 통해 초기화가 완료될 때까지 트래픽 유입을 지연시킬 수 있습니다.

특정 샤딩 설정의 변경은 **전체 클러스터 재시작(full cluster restart)**이 필요합니다.

- `extractShardId` 함수
- 샤드 영역 역할(shard region role)
- 영속성 모드(persistence mode)
- `number-of-shards` 설정

---

### 클러스터 싱글톤(Cluster Singleton)

> "클러스터 싱글톤(Cluster singleton)은 항상 가장 오래된 노드(oldest node)에서 실행된다."

싱글톤 마이그레이션 부담을 최소화하려면, **가장 오래된 노드를 마지막에 업그레이드**하십시오. 그러면 전체 업데이트 주기 동안 싱글톤이 단 한 번만 이동하게 됩니다.

쿠버네티스에서 롤링 업데이트 전략을 사용하는 배포의 경우, Akka Management의 Kubernetes Rolling Updates 기능을 활성화하여 파드(pod)가 선호되는 순서(preferred order)로 삭제되도록 할 수 있습니다.

---

### 설정 호환성 검사(Configuration compatibility check)

롤링 업데이트 중에는 기존 노드의 설정이 클러스터 호환성 검사(compatibility check)를 통과해야 합니다.

클래식(Classic)에서 타입드 액터(Typed Actor)로 마이그레이션하는 경우 다음과 같은 2단계 접근법이 필요합니다.

1. `akka.cluster.configuration-compatibility-check.enforce-on-join = off`로 배포
2. `enforce-on-join = on`으로 재배포

---

### Akka 버전 마이그레이션과 롤링 업데이트

#### Java 직렬화에서 Jackson으로

Akka 2.5에서 2.6으로의 마이그레이션은 네 단계의 롤링 업데이트로 진행됩니다.

**1단계 (Stage 1)**: `akka.actor.allow-java-serialization=on` 상태로 2.6.0으로 업데이트합니다.

**2단계 (Stage 2)**: 직렬화는 활성화하지 않고 역직렬화(deserialization) 지원을 준비합니다.
- Jackson 문서에 따라 마커 인터페이스(marker interface)와 어노테이션(annotation)을 추가합니다.
- 먼저 새로운 클러스터에서 테스트합니다.
- `serialization-bindings`에서 마커 인터페이스 바인딩을 제거합니다.
- `akka.serialization.jackson.allowed-class-prefix=["com.myapp"]`를 설정합니다.

**3단계 (Stage 3)**: Jackson 직렬화를 활성화합니다.
- 마커 인터페이스 바인딩을 Jackson 직렬화기에 추가합니다.
- `allowed-class-prefix` 설정을 제거합니다.
- 이 단계에서 구 버전 노드는 계속 Java 직렬화로 메시지를 보내고, 신 버전 노드는 Jackson으로 메시지를 보냅니다.

**4단계 (Stage 4)**: Java 직렬화를 비활성화합니다.
- `allow-java-serialization` 설정을 제거합니다.
- 수정했다면 `warn-about-java-serializer-usage` 설정도 제거합니다.

#### Receptionist를 사용하는 Akka Typed

Receptionist 또는 Cluster Receptionist를 사용하면서 2.5에서 2.6으로 마이그레이션하는 경우, 롤링 업데이트 동안에는 서로 다른 버전 사이에서 정보 전파(dissemination)가 이루어지지 않습니다. 다만 구 버전 노드가 모두 제거되고 나면 기능이 정상 복구됩니다.

---

### 전체 종료가 필요한 경우

다음과 같은 변경은 롤링 업데이트가 아니라 **전체 클러스터 종료(full shutdown)**가 필요합니다.

#### 클러스터 샤딩 설정 변경

다음 변경은 전체 클러스터 재시작이 필요합니다.

- `extractShardId` 함수 변경
- 샤드 영역 역할(shard region role) 변경
- 영속성 모드(persistence mode) 변경 (모든 노드에서 일관되게 유지되어야 함)
- `number-of-shards` 조정 (참고: 클러스터 노드 수의 변경은 샤드 수의 변경을 요구하지 않음)

#### 클러스터 설정 변경

> "SBR(Split Brain Resolver) 전략을 변경하는 경우 전체 재시작이 필요하다."

#### PersistentFSM에서 EventSourcedBehavior로 마이그레이션

클러스터 샤딩과 함께 PersistentFSM에서 EventSourcedBehavior로 마이그레이션하는 경우, 샤드가 노드 버전 간에 재배치될 수 있으므로 전체 종료가 필요합니다.

#### 클래식 리모팅(Classic Remoting)에서 Artery로 마이그레이션

프로토콜 차이로 인해, 클래식 리모팅에서 Artery로 마이그레이션할 때는 롤링 업데이트가 지원되지 않습니다.

#### 리모팅 전송(transport) 변경

리모팅 전송을 변경하는 경우 롤링 업데이트가 지원되지 않습니다.

#### 클래식 샤딩에서 타입드 샤딩으로 마이그레이션

클래식 샤딩(classic sharding)에서 타입드 샤딩(typed sharding)으로 마이그레이션하기 위한 3단계 롤링 업데이트 절차가 존재하며, 관련 샘플 PR에 자세히 설명되어 있습니다.

---

## 네이티브 이미지(Native Image)

### 개요

Akka로 GraalVM 네이티브 이미지(native image)를 빌드하는 것은 로컬(local) 액터 시스템 애플리케이션과 클러스터(cluster) 액터 시스템 애플리케이션 모두에 대해 지원됩니다.

> "Akka의 대부분의 내장(built-in) 기능은 그대로 사용할 수 있지만, 일부 기능은 사용할 수 없으며, 일부 확장 지점(extension point)은 리플렉션(reflective) 접근이 동작하도록 추가 메타데이터(metadata)가 필요하다."

여러 Akka 프로젝트와 빌드 도구 설정을 포함한 종합적인 샘플은 [Akka Edge Documentation](https://doc.akka.io/libraries/akka-edge/current/lightweight-deployments.html#graalvm-native-image)에서 확인할 수 있으며, 빌드 도구(sbt, Maven, Gradle)별 구체적인 네이티브 이미지 빌드 설정도 이 문서를 참고하십시오.

---

### 지원되지 않는 기능

다음 기능들은 네이티브 이미지에서 기본 상태로는 사용할 수 없습니다.

- Lightbend Telemetry
- Aeron UDP 리모팅
- 테스트킷(Testkits)
- LevelDB 및 InMem Akka Persistence 플러그인
- Akka Distributed Data를 위한 영구 저장소(durable storage)
- Scala 3

---

### 추가 메타데이터가 필요한 기능

> "서드파티(third-party) 라이브러리나 사용자 코드가 제공하는 커스텀 구현을 애플리케이션 설정 파일의 항목을 통해 Akka에 끼워넣는(plug in) 모든 기능은, 리플렉션 메타데이터를 명시적으로 추가해야 한다."

예시는 다음과 같습니다.

- 커스텀 직렬화기(Custom Serializers)
- 커스텀 메일박스 타입(Custom Mailbox Types)
- Akka Persistence 플러그인
- Akka Discovery 구현체
- Akka Lease 구현체

---

### Jackson 직렬화

`JsonSerializable` 및 `CborSerializable` 마커 트레이트(marker trait)를 사용하면, 해당 메시지 타입들이 자동으로 리플렉션을 위해 등록(register)됩니다. 다만 다음과 같은 주의 사항이 있습니다.

- 원시 타입(primitive), 표준 라이브러리 타입, 또는 Akka 타입만 포함하는 메시지는 자동으로 등록됩니다.
- 복잡한 메시지 구조나 특수한 Jackson 어노테이션이 사용된 경우에는 신중한 테스트가 필요합니다.
- 제네릭(generic)에서 타입 파라미터로만 참조되는 타입(예: `List[MyClass]`)은 명시적으로 마커 트레이트를 구현하거나 `reflect-config.json`에 항목을 추가해야 합니다.
- Scala 표준 라이브러리의 열거형(enumeration)은 기본 지원되지 않습니다.
- 커스텀 마커 트레이트를 사용하는 경우, 해당 트레이트, 구체 메시지 타입(concrete message type), 그리고 필드 타입(field type) 모두에 대해 리플렉션 메타데이터가 필요합니다.
- `JacksonMigration` 구현체는 리플렉션 메타데이터에 반드시 나열되어야 합니다.
- `akka.serialization.jackson.jackson-modules` 설정을 통해 추가된 ObjectMapper들은 리플렉션 메타데이터 항목이 필요합니다.

#### 서드파티 직렬화기(Third-Party Serializers)

서드파티 직렬화기는 직렬화기 구현체, `serialization-bindings`에 등록된 타입들, 그리고 직렬화기 로직에 따라 메시지별(per-message) 메타데이터까지 리플렉션 메타데이터가 필요할 수 있습니다.

---

### 기타 확장 지점

#### 확장(Extensions)

설정을 통해 로드되는 클래식 및 타입드 확장(`akka.extensions`, `akka.actor.typed.extensions`, `akka.actor.library-extensions`, `akka.actor.typed.library-extensions`)은 클래스 및 생성자(constructor) 정보를 포함한 리플렉션 메타데이터 항목이 필요합니다.

#### Akka Persistence 이벤트 어댑터(Event Adapters)

애플리케이션에서 정의한 이벤트 어댑터(event adapter)는 클래스 및 생성자 정보와 함께 리플렉션 메타데이터에 나열되어야 합니다.

#### 리플렉션 기반 클래식 액터 생성

타입 기반(type-based) 또는 클래스 기반(class-based) 방식으로 `Props`를 사용하는 클래식 액터는, 조회(lookup) 및 전달된 파라미터에 맞는 생성자(constructor)에 대한 리플렉션 항목이 필요합니다. 문서에서는 "리플렉션 요구 사항을 피하기 위해 람다 팩토리(lambda factory)를 사용하는 것이 더 쉬운 경로"라고 언급합니다. 리플렉션 기반 접근은 주로 클래식 원격 배포(classic remote deploy) 기능에서 필요합니다.

---

### 로깅

로깅에 `akka-slf4j`를 사용하는 경우, 선택한 구체 로거(concrete logger)는 추가 설정이 필요할 가능성이 높습니다.

Akka는 특정 로거를 강제하지 않지만 샘플에서는 `logback-classic`이 흔하게 사용되며, Akka는 이에 대한 리플렉션 메타데이터를 제공합니다. logback을 사용하는 프로젝트에는 네이티브 이미지 플래그 `--initialize-at-build-time=ch.qos.logback`이 필요합니다.

비동기(async) logback 어펜더(appender)의 경우, `ch.qos.logback.classic.AsyncAppender`를 사용하지 않거나, 빌드 시점(build time)에 스레드를 시작하지 않는 지연(lazy) 버전을 선언하십시오. 예제 구현은 Akka Projections의 엣지 복제(edge replication) 샘플에서 확인할 수 있습니다.

---

## 참고 자료

- [Akka 공식 문서](https://doc.akka.io/libraries/akka-core/current/)

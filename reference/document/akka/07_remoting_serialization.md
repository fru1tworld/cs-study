# Akka 원격 통신과 직렬화

> 이 문서는 Akka 공식 문서의 "Remoting (Artery)", "Remote Security", "Serialization", "Serialization with Jackson", "Multi Node Testing" 섹션을 한국어로 번역한 것입니다.
> 원본: https://doc.akka.io/libraries/akka-core/current/

---

## 목차

1. [원격 통신(Remoting)이란 무엇인가](#1-원격-통신remoting이란-무엇인가)
2. [Artery란 무엇인가](#2-artery란-무엇인가)
3. [전송 방식(Transport) 선택](#3-전송-방식transport-선택)
4. [원격 통신을 위한 ActorSystem 준비](#4-원격-통신을-위한-actorsystem-준비)
5. [표준 주소(Canonical Address)와 네트워크 토폴로지](#5-표준-주소canonical-address와-네트워크-토폴로지)
6. [원격 액터 참조 획득과 메시지 전송](#6-원격-액터-참조-획득과-메시지-전송)
7. [메시지 전달 보장(Delivery Guarantees)](#7-메시지-전달-보장delivery-guarantees)
8. [직렬화 요구사항과 ByteBuffer 기반 직렬화](#8-직렬화-요구사항과-bytebuffer-기반-직렬화)
9. [장애 감지(Failure Detection)와 격리(Quarantine)](#9-장애-감지failure-detection와-격리quarantine)
10. [원격 액터 감시(Watching Remote Actors)](#10-원격-액터-감시watching-remote-actors)
11. [성능 튜닝(Performance Tuning)](#11-성능-튜닝performance-tuning)
12. [원격 목적지를 가진 라우터(Routers)](#12-원격-목적지를-가진-라우터routers)
13. [원격 액터 생성(Remote Deployment)](#13-원격-액터-생성remote-deployment)
14. [컨테이너 환경(Docker/Kubernetes) 고려사항](#14-컨테이너-환경dockerkubernetes-고려사항)
15. [플라이트 레코더(Flight Recorder)](#15-플라이트-레코더flight-recorder)
16. [원격 통신 보안(Remote Security)](#16-원격-통신-보안remote-security)
17. [직렬화(Serialization) 개요](#17-직렬화serialization-개요)
18. [직렬화 설정과 바인딩](#18-직렬화-설정과-바인딩)
19. [프로그래밍 API: SerializationExtension](#19-프로그래밍-api-serializationextension)
20. [커스텀 직렬화기 만들기](#20-커스텀-직렬화기-만들기)
21. [ActorRef 직렬화](#21-actorref-직렬화)
22. [Java 직렬화 비활성화](#22-java-직렬화-비활성화)
23. [직렬화 검증(Verification)](#23-직렬화-검증verification)
24. [Jackson을 이용한 직렬화](#24-jackson을-이용한-직렬화)
25. [스키마 진화(Schema Evolution)](#25-스키마-진화schema-evolution)
26. [롤링 업데이트(Rolling Updates)](#26-롤링-업데이트rolling-updates)
27. [멀티노드 테스트(Multi Node Testing)](#27-멀티노드-테스트multi-node-testing)
28. [참고 자료](#참고-자료)

---

## 1. 원격 통신(Remoting)이란 무엇인가

원격 통신(remoting)은 서로 다른 노드(node) 위에 있는 액터 시스템(Actor System)들이 통신할 수 있게 해 주는 내부 메커니즘입니다.

공식 문서는 중요한 권장사항을 강조합니다: 애플리케이션은 일반적으로 원격 통신을 직접 사용하기보다는, Akka Cluster나 기술 중립적인 프로토콜(HTTP, gRPC 등)과 같은 더 높은 수준의 추상화(higher-level abstractions)를 활용해야 합니다. 원격 통신은 이러한 상위 계층 도구들이 그 위에 구축되는 기반 계층입니다.

### 의존성(Dependency)

Akka 원격 통신 모듈을 프로젝트에 추가합니다(버전 2.10.19 기준).

**sbt:**
```scala
val AkkaVersion = "2.10.19"
libraryDependencies += "com.typesafe.akka" %% "akka-remote" % AkkaVersion
```

> 참고: 액터 프로바이더(provider)로는 `remote`보다 `cluster`를 권장합니다. 따라서 실무에서는 `akka-cluster` 의존성을 함께 사용하는 경우가 많습니다.

---

## 2. Artery란 무엇인가

Artery는 Akka 원격 통신 계층(remoting layer)의 현대적인 재구현(reimplementation)으로, "고처리량(high-throughput), 저지연(low-latency) 통신"을 최우선으로 합니다. 주요 개선 사항은 다음과 같습니다.

- Akka 스트림(Akka Streams) TCP/TLS 또는 Aeron(UDP)을 기반으로 한 전송(transport) 옵션
- 전용 서브채널(dedicated subchannel)을 통한, 사용자 메시지(user messages)와 제어 메시지(control messages)의 격리(isolation)
- "거의 할당 없는 동작(mostly allocation-free operation)" — 메시지 처리 과정에서의 객체 할당을 최소화
- 와이어(wire) 상의 오버헤드를 줄이기 위한 액터 경로(actor path) 압축(compression)
- 성능 향상을 위한 ByteBuffer 기반 직렬화(serialization)
- Akka 메이저(major) 버전 간 프로토콜 안정성(protocol stability)

이러한 설계 목표 덕분에 Artery는 대용량 메시지 트래픽과 지연에 민감한 워크로드 모두에서 우수한 성능을 제공합니다.

---

## 3. 전송 방식(Transport) 선택

`akka.remote.artery.transport` 설정을 통해 세 가지 전송 옵션을 사용할 수 있습니다.

| 전송 방식(Transport) | 특성 |
|----------------------|------|
| **tcp** | Akka 스트림 TCP(기본값). 좋은 성능과 운영 단순성(operational simplicity)을 제공 |
| **tls-tcp** | 암호화(encryption)가 적용된 TCP. 보안이 필요할 때 권장됨 |
| **aeron-udp** | 고성능 UDP 전송. 유휴 상태(idle)에서 CPU 사용량이 더 높음. 64비트 JVM 필요 |

공식 문서는 다음과 같이 안내합니다: "무엇을 선택해야 할지 확신이 서지 않는다면 좋은 선택은 기본값인 `tcp`를 사용하는 것입니다(if you are uncertain of what to select a good choice is to use the default, which is `tcp`)."

### Aeron 전송을 위한 추가 의존성

`aeron-udp` 전송을 사용하려면 다음 의존성을 추가해야 합니다.

```scala
libraryDependencies ++= Seq(
  "io.aeron" % "aeron-driver" % "1.45.2",
  "io.aeron" % "aeron-client" % "1.45.2"
)
```

### Java 17에서 Aeron 사용 시 JVM 플래그

Java 17 이상에서 Aeron 전송을 사용하려면 다음 JVM 플래그가 필요합니다.

```
--add-opens=java.base/sun.nio.ch=ALL-UNNAMED
```

---

## 4. 원격 통신을 위한 ActorSystem 준비

최소한의 `application.conf` 설정 예시는 다음과 같습니다.

```hocon
akka {
  actor {
    provider = cluster  # "remote"보다 "cluster"를 권장
  }
  remote {
    artery {
      transport = tcp
      canonical.hostname = "127.0.0.1"
      canonical.port = 25520
    }
  }
}
```

원격 통신을 활성화하기 위한 네 가지 핵심 설정 요소는 다음과 같습니다.

1. 액터 프로바이더(actor provider)를 로컬(local)에서 변경합니다.
2. Artery를 원격 통신 구현체(remoting implementation)로 활성화합니다.
3. 호스트명(hostname)을 설정합니다. 프로덕션 환경에서는 localhost가 아니라 전역적으로 도달 가능한(globally reachable) 주소여야 합니다.
4. 포트(port)를 설정합니다. 같은 머신에서 여러 액터 시스템을 운영할 경우 각각 고유한 포트를 가져야 합니다.

---

## 5. 표준 주소(Canonical Address)와 네트워크 토폴로지

표준 주소(canonical address) 개념은 원격 통신의 핵심입니다. 문서에 따르면 "동일한 네트워크상의 각 시스템은 다른 어떤 시스템에든 메시지를 보낼 수 있다(each system can send messages to any other system on the same network)"는 것이 가능하려면 "고유하고 전역적으로 도달 가능한(unique, globally reachable) 주소와 포트"가 필요합니다. 이 주소는 시스템의 고유한 정체성(identity)의 일부가 되며, 원격 시스템들이 연결을 맺는 데 사용됩니다.

### NAT 또는 Docker 시나리오에서의 바인드 주소 분리

NAT(Network Address Translation)나 Docker 환경에서는, 바인드(bind) 주소와 표준(canonical) 주소를 분리해야 합니다.

```hocon
akka {
  remote {
    artery {
      canonical.hostname = my.domain.com      # 외부(논리적) 호스트명
      canonical.port = 8000                   # 외부(논리적) 포트
      bind.hostname = local.address           # 내부(바인드) 호스트명
      bind.port = 25520                       # 내부(바인드) 포트
    }
  }
}
```

- `canonical.*`: 외부에서 이 시스템에 도달하기 위해 사용하는 논리적 주소입니다. 다른 노드들에게 광고(advertise)되는 주소입니다.
- `bind.*`: 시스템이 실제로 소켓을 바인딩하는 로컬 네트워크 인터페이스 주소입니다.

### 프로그래밍 방식 설정

원격 통신 속성을 프로그래밍 방식으로 설정할 수도 있습니다.

```java
ConfigFactory.parseString("akka.remote.artery.canonical.hostname=\"1.2.3.4\"")
  .withFallback(ConfigFactory.load());
```

---

## 6. 원격 액터 참조 획득과 메시지 전송

### ActorSelection을 통한 조회(Lookup)

원격 액터 참조(remote actor reference)를 획득하기 위한 문서화된 패턴은 다음과 같은 경로 형식을 사용합니다.

```
akka://<actor system>@<hostname>:<port>/<actor path>
```

**Scala 예시:**
```scala
val selection = context.actorSelection("akka://[email protected]:25520/user/actorName")
selection ! "Pretty awesome feature"
```

**Java 예시:**
```java
ActorSelection selection = context.actorSelection("akka://[email protected]:25520/user/actorName");
selection.tell("Pretty awesome feature", getSelf());
```

### ActorSelection을 ActorRef로 변환

`ActorSelection`을 `ActorRef`로 변환하려면, 내장된 `Identify` 메시지를 사용하거나 `resolveOne()` 메서드를 사용합니다. `resolveOne()`은 "일치하는 `ActorRef`를 담은 [`Future`/`CompletionStage`]를 반환(returns a [`Future`/`CompletionStage`] of the matching `ActorRef`)"합니다.

---

## 7. 메시지 전달 보장(Delivery Guarantees)

문서는 액터 메시지 전달의 보장 수준을 명시합니다.

- 일반 메시지(regular messages)는 **최대 한 번(at-most-once)** 전달 기준으로 동작합니다. 즉, 메시지는 한 번 전달되거나 전혀 전달되지 않으며, 중복 전달은 없습니다.
- 시스템 메시지(system messages, 예: 데스 워치(death watch)와 배포(deployment))는 확인(confirmation)과 재전송(resending) 메커니즘을 통해 **정확히 한 번(exactly-once)** 전달이 보장됩니다.

---

## 8. 직렬화 요구사항과 ByteBuffer 기반 직렬화

문서는 명시합니다: "액터 메시지에 대해 [직렬화]를 활성화해야 한다(You need to enable [serialization] for your actor messages)." 기본 선택지로는 Jackson 기반 직렬화가 권장됩니다.

### ByteBuffer 기반 직렬화

Artery는 고처리량 메시징(high-throughput messaging)에서의 성능 향상을 위해 `ByteBufferSerializer`를 도입했습니다.

**Scala:**
```scala
trait ByteBufferSerializer {
  def toBinary(o: AnyRef, buf: ByteBuffer): Unit
  def fromBinary(buf: ByteBuffer, manifest: String): AnyRef
}
```

**Java:**
```java
interface ByteBufferSerializer {
  void toBinary(Object o, ByteBuffer buf);
  Object fromBinary(ByteBuffer buf, String manifest);
}
```

구현체는 일반적으로 `SerializerWithStringManifest`를 확장하고, 배열(array) 기반 메서드를 ByteBuffer 기반 메서드로 위임(delegate)해야 합니다.

---

## 9. 장애 감지(Failure Detection)와 격리(Quarantine)

### Phi 누적 장애 감지기(Phi Accrual Failure Detector)

시스템은 "Phi 누적 장애 감지기(The Phi Accrual Failure Detector)" 알고리즘을 구현하며, phi 값을 다음과 같이 계산합니다.

```
phi = -log10(1 - F(timeSinceLastHeartbeat))
```

여기서 F는 누적 분포 함수(cumulative distribution function)를 나타냅니다.

- `akka.remote.watch-failure-detector.threshold` (기본값: 10) 설정은 장애 감지의 민감도(sensitivity)를 결정합니다.
- `acceptable-heartbeat-pause` 파라미터는 가비지 컬렉션(garbage collection) 일시 정지나 일시적인 네트워크 장애를 수용(accommodate)합니다.

### 격리(Quarantine) 메커니즘

연관(association)이 격리(quarantine) 상태에 진입하는 경우는 다음과 같습니다.

- 클러스터 노드가 멤버십(membership)에서 제거될 때
- 원격 장애 감지기(remote failure detector)가 (감시(watch)를 통해) 발동할 때
- 시스템 메시지 버퍼(system message buffer)가 오버플로(overflow)할 때
- 예상치 못한 인프라 예외(infrastructure exception)가 발생할 때

각 ActorSystem은 인카네이션(incarnation, 같은 주소로 재시작된 새로운 인스턴스)을 구별하기 위한 고유 식별자(UID, unique identifier)를 가집니다.

문서에 따르면: "이 상태에서 복구하는 유일한 방법은 액터 시스템 중 하나를 재시작하는 것입니다(The only way to recover from this state is to restart one of the actor systems)." 격리된 시스템으로 보내진 메시지는 폐기(drop)되지만, `actorSelection` 메시지는 시스템 재시작 여부를 탐지(probe)하기 위해 여전히 전달될 수 있습니다.

---

## 10. 원격 액터 감시(Watching Remote Actors)

원격 데스 워치(remote death watching)는 하트비트(heartbeat) 메시지와 장애 감지기를 활용하여 `Terminated` 메시지를 생성합니다.

이 접근 방식은 "API 측면에서 로컬 액터를 감시하는 것과 다르지 않습니다(API wise not different than watching a local actor)." 다만 장애 감지기는 원격 시스템이 정상적으로 종료(graceful shutdown)되지 않고 충돌(crash)하는 시나리오를 처리합니다. 즉, 원격 노드가 갑자기 사라지더라도 감시하는 액터는 `Terminated` 알림을 받을 수 있습니다.

---

## 11. 성능 튜닝(Performance Tuning)

### 병렬 처리를 위한 레인(Lanes)

여러 개의 인바운드(inbound) 및 아웃바운드(outbound) 레인을 두면 직렬화/역직렬화를 병렬로 처리할 수 있습니다.

```hocon
akka.remote.artery {
  inbound-lanes = 4
  outbound-lanes = 4
}
```

레인 선택은 수신자(recipient) ActorRef의 일관된 해싱(consistent hashing)을 사용하여, 수신자별 메시지 순서(per-receiver message ordering)를 보존합니다.

문서는 다음과 같이 언급합니다: "가장 낮은 지연(lowest latency)은 `inbound-lanes=1`과 `outbound-lanes=1`로 달성할 수 있다(lowest latency can be achieved with `inbound-lanes=1` and `outbound-lanes=1`)." 여러 레인을 사용하면 비동기 경계(async boundaries)가 도입되기 때문입니다. 즉, 처리량(throughput)을 위해서는 레인을 늘리고, 최저 지연을 위해서는 레인을 1로 둡니다.

### 대용량 메시지 처리(Large Message Handling)

전용 서브채널(dedicated subchannel)은 대용량 메시지를 일반 트래픽으로부터 격리합니다. 액터 경로 패턴(actor path patterns)을 통해 설정합니다.

```hocon
akka.remote.artery.large-message-destinations = [
  "/user/largeMessageActor",
  "/user/largeMessagesGroup/*",
  "/user/anotherGroup/*/largeMessages",
  "/user/thirdGroup/**"
]
```

패턴 매칭은 와일드카드(wildcard)를 지원합니다.
- `*`: 한 단계(single level)를 매칭
- `**`: 여러 단계(multiple levels)를 매칭

와일드카드가 없는 정확한 매칭(non-wildcard match)이 더 높은 우선순위를 가집니다.

대용량 메시지 로깅(logging)을 활성화하려면 다음과 같이 설정합니다.

```hocon
akka.remote.artery {
  log-frame-size-exceeding = 10000b
}
```

### Aeron 미디어 드라이버(Media Driver) 설정

멀티 JVM 환경에서는, 외부 공유 미디어 드라이버(external shared media driver)를 사용하여 리소스 경합(resource contention)을 줄일 수 있습니다.

```hocon
akka.remote.artery.advanced.aeron {
  embedded-media-driver = off
  aeron-dir = /dev/shm/aeron
}
```

### CPU-지연 트레이드오프(Aeron)

```hocon
# 1(저지연 선호, 높은 CPU) ~ 10 사이의 값
akka.remote.artery.advanced.aeron.idle-cpu-level = 5
```

값이 낮을수록 "수면(sleeping)" 기간이 늘어나, 유휴 상태의 CPU 사용량을 줄이는 대신 반응 시간(reaction time)이 길어집니다.

---

## 12. 원격 목적지를 가진 라우터(Routers)

### 풀(Pool) 기반 원격 라우팅

라우터가 원격 노드에 라우티(routee) 액터를 생성합니다.

```hocon
akka.actor.deployment {
  /parent/remotePool {
    router = round-robin-pool
    nr-of-instances = 10
    target.nodes = [
      "tcp://[email protected]:2552",
      "akka://[email protected]:2552"
    ]
  }
}
```

### 그룹(Group) 기반 원격 라우팅

그룹 기반 원격 라우팅은 원격 노드에 이미 존재하는(pre-existing) 액터를 필요로 합니다.

```hocon
akka.actor.deployment {
  /parent/remoteGroup {
    router = round-robin-group
    routees.paths = [
      "akka://[email protected]:2552/user/workers/w1",
      "akka://[email protected]:2552/user/workers/w2"
    ]
  }
}
```

---

## 13. 원격 액터 생성(Remote Deployment)

> **권장되지 않음(Not Recommended)**: 문서는 다음과 같은 경고를 포함합니다. "원격 배포(remote deployment)라고도 알려진 원격 액터 생성(Creating Actors Remotely)은 권장하지 않으나, 완결성을 위해 여기에 문서화합니다(We recommend against Creating Actors Remotely, also known as remote deployment, but it is documented here for completeness)."

설정 예시:
```hocon
akka {
  actor {
    deployment {
      /sampleActor {
        remote = "akka.tcp://[email protected]:2553"
      }
    }
  }
}
```

### 원격 배포를 위한 허용 목록(Allow List)

어떤 액터가 원격으로 배포될 수 있는지 제한하려면 다음과 같이 설정합니다.

```hocon
akka.remote.deployment {
  enable-allow-list = on
  allowed-actor-classes = [
    "akka.remote.artery.RemoteDeploymentSpec.Echo1"
  ]
}
```

문서에 따르면: "허용 목록에 포함되지 않은 액터 클래스는 이 시스템에 원격 배포될 수 없습니다(Actor classes not included in the allow list will not be allowed to be remote deployed onto this system)."

---

## 14. 컨테이너 환경(Docker/Kubernetes) 고려사항

컨테이너화된(containerized) 환경에서 `aeron-udp`를 사용하는 경우, 충분한 공유 메모리(shared memory)를 확보해야 합니다.

**Docker:**
```
--shm-size="512mb"
```

**Kubernetes 볼륨 마운트(volume mount):**
```yaml
volumeMounts:
- mountPath: /dev/shm
  name: media-driver
volumes:
- name: media-driver
  emptyDir:
    medium: Memory
```

---

## 15. 플라이트 레코더(Flight Recorder)

JDK 11 이상에서는 Artery 전용 Java 플라이트 레코더(Java Flight Recorder) 이벤트가 자동으로 활성화됩니다. 필요한 경우 비활성화할 수 있습니다.

```hocon
akka.java-flight-recorder.enabled = false
```

플라이트 레코더는 원격 통신의 내부 동작을 진단(diagnose)하는 데 유용합니다.

---

## 16. 원격 통신 보안(Remote Security)

### 핵심 보안 원칙

문서에 따르면, `ActorSystem`은 신뢰할 수 없는 네트워크(untrusted network, 예: 인터넷)에 평문 Aeron/UDP 또는 TCP를 통해 노출되어서는 안 됩니다(An `ActorSystem` should not be exposed via Akka Remote (Artery) over plain Aeron/UDP or TCP to an untrusted network). 권장사항은 네트워크 보안(firewalls)으로 시스템을 보호하거나, "상호 인증을 동반한 TLS(TLS with mutual authentication)"를 활성화하는 것입니다.

### SSL/TLS 설정

#### 기본 전송 설정

TLS를 활성화하려면 전송 방식을 다음과 같이 설정합니다.

```hocon
akka.remote.artery {
  transport = tls-tcp
}
```

#### SSL/TLS 파라미터

완전한 설정 예시:

```hocon
akka.remote.artery {
  transport = tls-tcp

  ssl.config-ssl-engine {
    key-store = "/example/path/to/mykeystore.jks"
    trust-store = "/example/path/to/mytruststore.jks"

    key-store-password = ${SSL_KEY_STORE_PASSWORD}
    key-password = ${SSL_KEY_PASSWORD}
    trust-store-password = ${SSL_TRUST_STORE_PASSWORD}

    protocol = "TLSv1.2"

    enabled-algorithms = [TLS_DHE_RSA_WITH_AES_128_GCM_SHA256]
  }
}
```

**중요한 실천 사항:** 자격 증명(credentials)을 설정 파일에 하드코딩(hardcoding)하지 말고, 비밀번호에 대해 환경 변수 치환(environment variable substitution)을 사용하십시오.

#### 권장 암호화 방식(Cipher Suites)

TLS 1.2에 대한 RFC 7525 권고에 따른 암호화 방식:

- TLS_DHE_RSA_WITH_AES_128_GCM_SHA256
- TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
- TLS_DHE_RSA_WITH_AES_256_GCM_SHA384
- TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384

문서는 설정 전에 현재의 보안 권고를 확인할 것을 권합니다.

### 상호 인증(Mutual Authentication)

상호 인증(mutual authentication)은 기본적으로 활성화되어 있습니다. 이는 "연결의 수동 측(passive side, 즉 TLS 서버 측)이 연결하는 피어(peer)로부터도 인증서를 요청하고 검증한다(the passive side (the TLS server side) of a connection will also request and verify a certificate from the connecting peer)"는 것을 의미합니다. Akka는 피어 투 피어(peer-to-peer) 통신을 사용하므로, 두 노드 모두 키스토어(keystore)와 트러스트스토어(truststore)를 설정해야 합니다.

#### 호스트명 검증(Hostname Verification)

호스트명 검증을 활성화하려면 다음과 같이 설정합니다.

```hocon
akka.remote.artery.ssl.config-ssl-engine.hostname-verification=on
```

이는 목적지 호스트명(destination hostname)이 피어 인증서(peer certificate)의 호스트명과 일치하는지 검증합니다. 동적 호스트명(dynamic hostname)을 사용하는 배포 환경에서는 검증을 비활성화할 수 있습니다.

### 인증서 전략 옵션(Certificate Strategy Options)

#### 옵션 1: 단일 인증서, 호스트명 검사 비활성화
- 하나의 키/인증서를 모든 노드에 배포
- 자체 서명(self-signed) 인증서 허용
- 노드 추가가 단순함
- **위험:** 키가 탈취되면 클러스터 전체가 위협받음

#### 옵션 2: 모든 호스트명을 포함하는 단일 인증서, 호스트명 검사 활성화
- 단일 인증서가 모든 신뢰된 호스트명을 나열
- 노드가 인증서 안에 포함되어 있는지 검증 가능
- 확장 시 와일드카드 인증서나 CA가 필요
- **위험:** 탈취된 인증서는 신뢰된 호스트명에서만 사용 가능

#### 옵션 3: CA 기반, 노드별 개별 인증서, 호스트명 검사 활성화
- 각 노드마다 개별 인증서
- CA 인증서가 클러스터 전반에서 신뢰됨
- 인증서 폐기(revocation) 지원
- **위험:** (DNS가 변조되지 않는 한) 탈취된 노드만 클러스터에 접근 가능

### Kubernetes에서 키 회전(Rotating Keys, mTLS)

#### 아키텍처

구현은 cert-manager를 사용하며 다음으로 구성됩니다.

1. **자체 서명 발급자(Self-signed issuer)** — CA 인증서를 발급
2. **CA 발급자(CA issuer)** — 자주 회전되는 서비스 인증서를 발급
3. **서비스 인증서(Service certificates)** — 예시에서는 24시간마다 회전

이 접근 방식은 전환 기간(transition period) 동안 노드 간에 동일한 인증서를 요구하지 않고도 인증서를 회전(rotate)할 수 있게 합니다.

#### Kubernetes 리소스

**자체 서명 발급자:**
```yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: self-signed-issuer
spec:
  selfSigned: {}
```

**CA 인증서 (100년 기간):**
```yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: akka-tls-ca-certificate
  namespace: default
spec:
  issuerRef:
    name: self-signed-issuer
    kind: ClusterIssuer
  secretName: akka-tls-ca-certificate
  commonName: default.akka.cluster.local
  duration: 876000h
  renewBefore: 867240h
  isCA: true
  privateKey:
    rotationPolicy: Always
```

**CA 발급자:**
```yaml
apiVersion: cert-manager.io/v1
kind: Issuer
metadata:
  name: akka-tls-ca-issuer
  namespace: default
spec:
  ca:
    secretName: akka-tls-ca-certificate
```

**서비스 인증서 (24시간 회전):**
```yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: my-service-akka-tls-certificate
  namespace: default
spec:
  issuerRef:
    name: akka-tls-ca-issuer
  secretName: my-service-akka-tls-certificate
  dnsNames:
  - my-service.default.akka.cluster.local
  duration: 24h
  renewBefore: 16h
  privateKey:
    rotationPolicy: Always
```

#### 키 회전을 위한 Akka 설정

```hocon
akka.remote.artery {
  transport = tls-tcp
  ssl.ssl-engine-provider = "akka.remote.artery.tcp.ssl.RotatingKeysSSLEngineProvider"
}
```

#### Kubernetes 배포 통합

인증서 시크릿(secret)을 마운트합니다.

```yaml
volumes:
- name: akka-tls
  secret:
    secretName: my-service-akka-tls-certificate
```

컨테이너 볼륨 마운트:
```yaml
volumeMounts:
- name: akka-tls
  mountPath: /var/run/secrets/akka-tls/rotating-keys-engine
```

### 신뢰할 수 없는 모드(Untrusted Mode)

활성화 방법:
```hocon
akka.remote.artery.untrusted-mode = on
```

#### 차단되는 작업(Blocked Operations)

신뢰할 수 없는 모드가 활성화되면, 시스템은 다음을 무시합니다.

- 원격 배포(remote deployment) 및 원격 감독(remote supervision)
- 원격 데스 워치(remote DeathWatch)
- `system.stop()`, `PoisonPill`, `Kill` 명령
- `PossiblyHarmful` 인터페이스를 확장하는 메시지
- 액터 셀렉션(actor selection) 메시지 (허용 목록에 있지 않은 한)

#### 신뢰된 셀렉션 경로(Trusted Selection Paths)

특정 액터들이 셀렉션 메시지를 받을 수 있도록 허용합니다.

```hocon
akka.remote.artery.trusted-selection-paths = ["/user/receptionist", "/user/namingService"]
```

#### 한계(Limitations)

신뢰할 수 없는 모드만으로는 완전한 보호를 제공하지 못합니다. 문서는 "Java 직렬화는 여전히 활성화해서는 안 된다(Java serialization should still not be enabled)"는 점을 강조하며, 신뢰할 수 없는 모드를 네트워크 보안 및/또는 상호 인증 TLS와 결합할 것을 권장합니다.

모범 사례는 "잘 정의된 진입점 액터 집합(a well-defined set of entry point actors)"으로 시스템을 설계하고, 이 진입점 액터들이 요청을 검증한 뒤 로컬 참조(local references)를 사용하여 워커 시스템(worker systems)으로 전달하도록 하는 것입니다.

### 추가 보안 권장사항

- Akka에서는 알려진 공격 표면(attack surface)으로 인해 Java 직렬화가 기본적으로 비활성화되어 있습니다.
- SHA1PRNG를 사용하는 Linux 시스템에서는 차단(blocking)을 방지하기 위해 `-Djava.security.egd=file:/dev/urandom`을 지정하십시오.
- 키스토어와 인증서 리소스는 Lightbend의 SSL-Config 라이브러리에 문서화되어 있습니다.
- 설정 상세 정보는 공식 Java Secure Socket Extension(JSSE) 문서를 참고하십시오.

---

## 17. 직렬화(Serialization) 개요

Akka의 직렬화(serialization) 메커니즘은 JVM 객체를 바이트 배열(byte array)로 변환하여 JVM 간 통신을 가능하게 합니다. 로컬(local) 액터 메시지는 참조 전달(reference passing)을 사용하지만, 원격(remote) 메시지는 직렬화가 필요합니다. 프레임워크는 커스텀 직렬화기(custom serializer)와 자동 바인딩(binding) 설정을 지원합니다.

### 의존성 설정

코어 액터 의존성을 추가합니다(버전 2.10.19).

**sbt:**
```scala
val AkkaVersion = "2.10.19"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % AkkaVersion
```

**Maven:**
```xml
<dependency>
  <groupId>com.typesafe.akka</groupId>
  <artifactId>akka-actor_2.13</artifactId>
</dependency>
```

**Gradle:**
```gradle
implementation "com.typesafe.akka:akka-actor_2.13:2.10.19"
```

---

## 18. 직렬화 설정과 바인딩

### 직렬화기 등록(Serializer Registration)

직렬화기 구현체를 설정에서 이름에 바인딩합니다.

```hocon
akka {
  actor {
    serializers {
      jackson-json = "akka.serialization.jackson.JacksonJsonSerializer"
      jackson-cbor = "akka.serialization.jackson.JacksonCborSerializer"
      proto = "akka.remote.serialization.ProtobufSerializer"
      myown = "docs.serialization.MyOwnSerializer"
    }
  }
}
```

### 직렬화 바인딩(Serialization Bindings)

메시지 타입(message type)을 직렬화기에 연결합니다.

```hocon
akka {
  actor {
    serialization-bindings {
      "com.google.protobuf.Message" = proto
      "docs.serialization.MyOwnSerializable" = myown
    }
  }
}
```

**핵심 사항:**
- 트레이트(trait)/인터페이스(interface) 이름이나 추상 기반 클래스(abstract base class)를 지정합니다.
- 모호한(ambiguous) 설정의 경우, 가장 구체적인(most specific) 클래스가 선택됩니다.
- 메시지를 담고 있는 Scala 객체(object)는 완전한 정규화 이름(fully qualified name)을 요구합니다. 예: `Wrapper.Message` 대신 `Wrapper$Message`.

### 식별자 설정(Identifier Configuration)

식별자를 하드코딩하는 대신 설정에서 정의할 수 있습니다.

```hocon
akka {
  actor {
    serialization-identifiers {
      "docs.serialization.MyOwnSerializer" = 1234567
    }
  }
}
```

---

## 19. 프로그래밍 API: SerializationExtension

### 기본 사용법

**Scala:**
```scala
val system = ActorSystem("example")
val serialization = SerializationExtension(system)

val original = "woohoo"
val bytes = serialization.serialize(original).get
val serializerId = serialization.findSerializerFor(original).identifier
val manifest = Serializers.manifestFor(
  serialization.findSerializerFor(original),
  original
)

val back = serialization.deserialize(bytes, serializerId, manifest).get
```

**Java:**
```java
ActorSystem system = ActorSystem.create("example");
Serialization serialization = SerializationExtension.get(system);

String original = "woohoo";
byte[] bytes = serialization.serialize(original).get();
int serializerId = serialization.findSerializerFor(original).identifier();
String manifest = Serializers.manifestFor(
  serialization.findSerializerFor(original),
  original
);

String back = (String) serialization.deserialize(
  bytes,
  serializerId,
  manifest
).get();
```

### 타입 지정 액터 시스템(Typed Actor Systems)

```scala
val system = ActorSystem(Behaviors.empty, "example")
val serialization = SerializationExtension(system)
```

**중요:** 역직렬화(deserialization)를 위해서는 매니페스트(manifest)와 직렬화기 ID(serializer ID)가 바이트와 함께 동반되어야 합니다. 직렬화 바인딩이 변경되는 롤링 업데이트(rolling update)를 지원하려면 이 세 가지 구성 요소(바이트, 직렬화기 ID, 매니페스트)를 함께 저장하십시오.

---

## 20. 커스텀 직렬화기 만들기

### 기본 직렬화기(Basic Serializer)

`Serializer`(Scala) 또는 `JSerializer`(Java)를 확장합니다.

**Scala:**
```scala
class MyOwnSerializer extends Serializer {
  def includeManifest: Boolean = true
  def identifier = 1234567

  def toBinary(obj: AnyRef): Array[Byte] = {
    // 직렬화 로직
    Array[Byte]()
  }

  def fromBinary(bytes: Array[Byte], clazz: Option[Class[_]]): AnyRef = {
    // 역직렬화 로직
    null
  }
}
```

**Java:**
```java
static class MyOwnSerializer extends JSerializer {
  @Override
  public boolean includeManifest() {
    return false;
  }

  @Override
  public int identifier() {
    return 1234567;
  }

  @Override
  public byte[] toBinary(Object obj) {
    return new byte[0];
  }

  @Override
  public Object fromBinaryJava(byte[] bytes, Class<?> clazz) {
    return null;
  }
}
```

**요구사항:**
- 식별자(identifier)는 고유해야 하며, 버전 간에 일정하게 유지되어야 합니다.
- 식별자 0~40은 Akka가 예약(reserved)했습니다.
- 매니페스트(manifest)는 다형적(polymorphic) 역직렬화를 위한 타입 힌트(type hint)를 제공합니다.

### SerializerWithStringManifest (권장)

문자열 기반 매니페스트(string-based manifest)는 더 나은 스키마 진화(schema evolution)와 영속성(persistence) 지원을 가능하게 합니다.

**Scala:**
```scala
class MyOwnSerializer2 extends SerializerWithStringManifest {
  val CustomerManifest = "customer"
  val UserManifest = "user"
  val UTF_8 = StandardCharsets.UTF_8.name()

  def identifier = 1234567

  def manifest(obj: AnyRef): String = obj match {
    case _: Customer => CustomerManifest
    case _: User => UserManifest
  }

  def toBinary(obj: AnyRef): Array[Byte] = obj match {
    case Customer(name) => name.getBytes(UTF_8)
    case User(name) => name.getBytes(UTF_8)
  }

  def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    manifest match {
      case CustomerManifest => Customer(new String(bytes, UTF_8))
      case UserManifest => User(new String(bytes, UTF_8))
    }
}
```

**모범 사례:** 알 수 없는 매니페스트(unknown manifest)에 대해서는 `IllegalArgumentException`이나 `NotSerializableException`을 던져, 혼합 버전(mixed versions)이 공존하는 롤링 업데이트를 가능하게 하십시오.

---

## 21. ActorRef 직렬화

액터 참조(actor reference)를 직렬화하려면 `ActorRefResolver`를 사용합니다.

**Scala:**
```scala
class PingSerializer(system: ExtendedActorSystem)
    extends SerializerWithStringManifest {
  private val actorRefResolver = ActorRefResolver(system.toTyped)

  override def toBinary(msg: AnyRef) = msg match {
    case PingService.Ping(who) =>
      actorRefResolver.toSerializationFormat(who)
        .getBytes(StandardCharsets.UTF_8)
  }

  override def fromBinary(bytes: Array[Byte], manifest: String) =
    manifest match {
      case PingManifest =>
        val str = new String(bytes, StandardCharsets.UTF_8)
        val ref = actorRefResolver.resolveActorRef[PingService.Pong.type](str)
        PingService.Ping(ref)
    }
}
```

`ActorRefResolver`는 액터 참조를 직렬화 형식(serialization format)의 문자열로 변환(`toSerializationFormat`)하고, 역으로 문자열로부터 액터 참조를 복원(`resolveActorRef`)합니다.

---

## 22. Java 직렬화 비활성화

Java 직렬화는 성능과 보안 우려로 인해 기본적으로 비활성화되어 있습니다. 초기 프로토타이핑(early prototyping) 단계에서만 활성화하십시오.

```hocon
akka.actor.allow-java-serialization = on
akka.actor.warn-about-java-serializer-usage = off
```

**경고:** 프로덕션(production) 사용은 강력히 권장되지 않습니다. 혼합 Scala 버전(mixed Scala versions)은 Java 직렬화와 함께 사용하기에 안전하지 않습니다.

---

## 23. 직렬화 검증(Verification)

테스트 목적으로 로컬 메시지(local messages)에 대해 직렬화를 강제할 수 있습니다.

```hocon
akka {
  actor {
    serialize-messages = on
    serialize-creators = on
  }
}
```

특정 메시지를 검증에서 제외하려면 `NoSerializationVerificationNeeded` 마커 트레이트(marker trait)를 구현하거나, 클래스 접두사(class prefix)를 설정하십시오.

**참고:** 이 설정은 테스트(testing) 중에만 사용하십시오. 로컬 메시지 전달 최적화(local message passing optimization)를 비활성화합니다.

### 주요 권장사항(요약)

- 대부분의 경우 기본값으로 "Jackson 직렬화(Jackson serialization)"를 사용하십시오.
- 엄격한 스키마 진화 제어가 필요하면 프로토콜 버퍼(Protocol Buffers)를 사용하십시오.
- 영속성(persistence)을 위해서는 `SerializerWithStringManifest`를 구현하십시오.
- 직렬화 검증은 테스트에서만 수행하십시오.
- 직렬화기 식별자를 버전 간에 안정적으로 유지하십시오.
- 저장/전송 시 바이트, 직렬화기 ID, 매니페스트를 함께 결합하십시오.

---

## 24. Jackson을 이용한 직렬화

Akka의 Jackson 직렬화기는 JSON 또는 CBOR(바이너리) 형식을 사용하여 메시지와 이벤트를 직렬화할 수 있게 합니다.

### 의존성 설정

**sbt:**
```scala
val AkkaVersion = "2.10.19"
libraryDependencies += "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion
```

**Maven:**
```xml
<dependency>
  <groupId>com.typesafe.akka</groupId>
  <artifactId>akka-serialization-jackson_${scala.binary.version}</artifactId>
</dependency>
```

**Gradle:**
```gradle
implementation "com.typesafe.akka:akka-serialization-jackson_${versions.ScalaBinary}:2.10.19"
```

### 마커 트레이트(Marker Trait)를 이용한 기본 사용법

Akka는 Jackson 직렬화를 활성화하기 위한 두 가지 사전 정의된 마커 인터페이스를 제공합니다.

**Scala:**
```scala
import akka.serialization.jackson.JsonSerializable
final case class Message(name: String, nr: Int) extends JsonSerializable
```

**Java:**
```java
import akka.serialization.jackson.JsonSerializable;
class MyMessage implements JsonSerializable {
  public final String name;
  public final int nr;
  public MyMessage(String name, int nr) {
    this.name = name;
    this.nr = nr;
  }
}
```

대안으로, 바이너리 CBOR 형식을 위해 `CborSerializable`을 구현하거나, 그에 상응하는 설정 바인딩을 가진 커스텀 마커 인터페이스를 만들 수 있습니다.

### 형식 선택(Format Selection)

`serialization-bindings`를 통해 두 가지 형식을 지원합니다.

- **`jackson-json`**: 텍스트 기반 JSON 형식. 사람이 읽을 수 있으나(human-readable) 장황함(verbose).
- **`jackson-cbor`**: 바이너리 CBOR 형식. 더 압축적(compact)이고 더 나은 성능을 제공.

### 다형적 타입 처리(Polymorphic Type Handling)

중첩된 필드(nested field)가 추상 기반 타입(abstract base type)을 사용하는 경우, Jackson 애노테이션(annotation)을 사용하여 모든 구현체를 선언해야 합니다.

**Scala:**
```scala
final case class Zoo(primaryAttraction: Animal) extends JsonSerializable

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(Array(
  new JsonSubTypes.Type(value = classOf[Lion], name = "lion"),
  new JsonSubTypes.Type(value = classOf[Elephant], name = "elephant")
))
sealed trait Animal

final case class Lion(name: String) extends Animal
final case class Elephant(name: String, age: Int) extends Animal
```

**Java:**
```java
public class Zoo implements JsonSerializable {
  public final Animal primaryAttraction;
  public Zoo(Animal primaryAttraction) {
    this.primaryAttraction = primaryAttraction;
  }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = Lion.class, name = "lion"),
  @JsonSubTypes.Type(value = Elephant.class, name = "elephant")
})
interface Animal {}

public final class Lion implements Animal {
  public final String name;
  public Lion(String name) { this.name = name; }
}
```

> **중요한 보안 주의:** `@JsonTypeInfo(use = Id.CLASS)`나 `ObjectMapper.enableDefaultTyping()`을 절대 사용하지 마십시오. 이들은 직렬화 가젯 공격(serialization gadget attacks)에 대한 취약점을 만듭니다. 명시적인 서브타입 선언(`@JsonSubTypes`)이 안전하지 않은 역직렬화를 방지합니다.

### Scala 열거형(Enumeration) 지원

더 깔끔한 직렬화를 위해 `@JsonScalaEnumeration` 애노테이션을 사용합니다.

```scala
object Planet extends Enumeration {
  type Planet = Value
  val Mercury, Venus, Earth, Mars, Krypton = Value
}

class PlanetType extends TypeReference[Planet.type] {}

final case class Superhero(
  name: String,
  @JsonScalaEnumeration(classOf[PlanetType]) planet: Planet.Planet
) extends JsonSerializable
```

### ADT와 케이스 객체(Case Objects)

case object를 가진 Scala sealed trait의 경우, 커스텀 역직렬화기(custom deserializer)를 정의합니다.

```scala
@JsonSerialize(using = classOf[DirectionJsonSerializer])
@JsonDeserialize(using = classOf[DirectionJsonDeserializer])
sealed trait Direction

object Direction {
  case object North extends Direction
  case object East extends Direction
  case object South extends Direction
  case object West extends Direction
}

class DirectionJsonSerializer extends StdSerializer[Direction](classOf[Direction]) {
  override def serialize(value: Direction, gen: JsonGenerator,
    provider: SerializerProvider): Unit = {
    val strValue = value match {
      case Direction.North => "N"
      case Direction.East => "E"
      case Direction.South => "S"
      case Direction.West => "W"
    }
    gen.writeString(strValue)
  }
}

class DirectionJsonDeserializer extends StdDeserializer[Direction](classOf[Direction]) {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): Direction = {
    p.getText match {
      case "N" => Direction.North
      case "E" => Direction.East
      case "S" => Direction.South
      case "W" => Direction.West
    }
  }
}
```

### 압축 설정(Compression Configuration)

기본적으로 Jackson JSON은 32 KiB를 초과하는 페이로드(payload)에 대해 GZIP 압축을 활성화합니다.

```hocon
akka.serialization.jackson.jackson-json.compression {
  algorithm = gzip      # 옵션: off, gzip, lz4
  compress-larger-than = 32 KiB
}
```

CBOR 바인딩은 기본적으로 압축을 비활성화하지만, 동일한 설정을 지원합니다.

### 날짜/시간 직렬화(Date/Time Serialization)

기본적으로 날짜는 상호 운용성(interoperability)을 위해 ISO-8601 형식으로 직렬화됩니다.

```hocon
akka.serialization.jackson.serialization-features {
  WRITE_DATES_AS_TIMESTAMPS = off
  WRITE_DURATIONS_AS_TIMESTAMPS = off
}
```

외부 호환성이 필요하지 않을 때 더 나은 성능을 위해 타임스탬프(timestamp) 형식을 활성화할 수 있습니다.

```hocon
akka.serialization.jackson.serialization-features {
  WRITE_DATES_AS_TIMESTAMPS = on
  WRITE_DURATIONS_AS_TIMESTAMPS = on
}
```

Jackson은 설정과 무관하게 두 형식 모두 역직렬화합니다.

### 바인딩별 설정(Per-Binding Configuration)

특정 바인딩에 대해 전역 설정을 재정의(override)합니다.

```hocon
akka.actor {
  serializers {
    jackson-json-message = "akka.serialization.jackson.JacksonJsonSerializer"
    jackson-json-event = "akka.serialization.jackson.JacksonJsonSerializer"
  }
  serialization-bindings {
    "com.myservice.MyMessage" = jackson-json-message
    "com.myservice.MyEvent" = jackson-json-event
  }
}

akka.serialization.jackson {
  jackson-json-message {
    serialization-features {
      WRITE_DATES_AS_TIMESTAMPS = on
    }
  }
  jackson-json-event {
    serialization-features {
      WRITE_DATES_AS_TIMESTAMPS = off
    }
  }
}
```

### 매니페스트 없는 직렬화(Manifest-Less Serialization)

매니페스트에서 클래스 이름을 제외하여 영속성 오버헤드(persistence overhead)를 줄입니다.

```hocon
akka.actor {
  serializers {
    jackson-json-event = "akka.serialization.jackson.JacksonJsonSerializer"
  }
  serialization-bindings {
    "com.myservice.MyEvent" = jackson-json-event
  }
}

akka.serialization.jackson {
  jackson-json-event {
    type-in-manifest = off
    deserialization-type = "com.myservice.MyEvent"
  }
}
```

이 경우 타입이 구체적(concrete)이거나, 타입 구별(type discrimination)을 위해 다형적 Jackson 애노테이션을 사용해야 합니다.

### ObjectMapper 설정

설정에서 Jackson의 직렬화/역직렬화 기능(features)을 커스터마이즈합니다.

```hocon
akka.serialization.jackson {
  serialization-features {
    WRITE_DATES_AS_TIMESTAMPS = off
    WRITE_DURATIONS_AS_TIMESTAMPS = off
    FAIL_ON_EMPTY_BEANS = off
  }

  deserialization-features {
    FAIL_ON_UNKNOWN_PROPERTIES = off
  }

  mapper-features {}

  constructor-detector-mode = "USE_PROPERTIES_BASED"

  visibility {
    FIELD = ANY
  }
}
```

### 보안 고려사항(Security Considerations)

직렬화기는 다음과 같은 여러 안전장치(safeguards)를 구현합니다.

- "`java.lang.Object`, `java.io.Serializable`, `java.lang.Comparable`과 같은 개방형 타입(open-ended types)에 Jackson 직렬화기를 바인딩하는 것은 허용되지 않습니다(Disallowed to bind Jackson serializers to open-ended types ...)."
- 알려진 직렬화 가젯 클래스(serialization gadget classes)에 대한 Jackson의 거부 목록(deny list)이 적용됩니다.
- 명시적인 `@JsonSubTypes` 선언이 임의 클래스(arbitrary classes)의 로딩을 방지합니다.

### 임베디드 Akka 직렬화(Embedded Akka Serialization)

이미 Akka 직렬화기를 가진 타입의 경우, `AkkaSerializationSerializer`와 `AkkaSerializationDeserializer`를 사용합니다.

```scala
final case class MyMessage(
  @JsonSerialize(using = classOf[AkkaSerializationSerializer])
  @JsonDeserialize(using = classOf[AkkaSerializationDeserializer])
  data: SomeType
) extends JsonSerializable
```

이는 직렬화기 ID, 매니페스트, base64로 인코딩된 페이로드를 임베드(embed)합니다.

### 기본 Jackson 모듈(Default Jackson Modules)

다음 모듈들이 자동으로 로드됩니다.

```hocon
akka.serialization.jackson.jackson-modules += "akka.serialization.jackson.AkkaJacksonModule"
akka.serialization.jackson.jackson-modules += "akka.serialization.jackson.AkkaTypedJacksonModule"
akka.serialization.jackson.jackson-modules += "akka.serialization.jackson.AkkaStreamJacksonModule"
akka.serialization.jackson.jackson-modules += "com.fasterxml.jackson.module.paramnames.ParameterNamesModule"
akka.serialization.jackson.jackson-modules += "com.fasterxml.jackson.datatype.jdk8.Jdk8Module"
akka.serialization.jackson.jackson-modules += "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule"
akka.serialization.jackson.jackson-modules += "com.fasterxml.jackson.module.scala.DefaultScalaModule"
```

`-parameters` 컴파일러 플래그로 Java 파라미터 이름(parameter names)을 활성화하면 애노테이션 요구사항을 줄일 수 있습니다.

### 바인딩 없이 역직렬화 허용하기

직렬화 바인딩에서 클래스를 제거하면서도 역직렬화 능력은 보존합니다.

```hocon
akka.serialization.jackson.allowed-class-prefix = [
  "com.myservice.event.OrderAdded",
  "com.myservice.command"
]
```

직렬화기 전환(serializer transition) 중이거나 레거시 저장 데이터(legacy stored data)를 읽을 때 유용합니다.

---

## 25. 스키마 진화(Schema Evolution)

### 필드 제거(Removing Fields)

마이그레이션 코드(migration code)가 필요 없습니다. Jackson은 JSON의 알 수 없는 속성(unknown properties)을 무시합니다.

### 선택적 필드 추가(Adding Optional Fields)

**기존 클래스:**
```scala
case class ItemAdded(shoppingCartId: String, productId: String, quantity: Int)
  extends JsonSerializable
```

**선택적 필드가 추가된 새 클래스:**
```scala
case class ItemAdded(
  shoppingCartId: String,
  productId: String,
  quantity: Int,
  discount: Option[Double]
) extends JsonSerializable
```

직렬화된 데이터에서 누락된 경우, 선택적 필드는 자동으로 `None`/`Optional.empty`를 받습니다.

### 필수 필드 추가(Adding Mandatory Fields)

필수 신규 필드의 경우, `JacksonMigration` 클래스를 생성합니다.

**Scala 마이그레이션:**
```scala
import akka.serialization.jackson.JacksonMigration
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

class ItemAddedMigration extends JacksonMigration {
  override def currentVersion: Int = 2

  override def transform(fromVersion: Int, json: JsonNode): JsonNode = {
    val root = json.asInstanceOf[ObjectNode]
    if (fromVersion <= 1) {
      root.set("discount", DoubleNode.valueOf(0.0))
    }
    root
  }
}
```

**설정(HOCON):**
```hocon
akka.serialization.jackson.migrations {
  "com.myservice.event.ItemAdded" = "com.myservice.event.ItemAddedMigration"
}
```

### 필드 이름 변경(Renaming Fields)

마이그레이션 로직에서 필드 이름을 변환합니다.

```scala
class ItemAddedMigration extends JacksonMigration {
  override def currentVersion: Int = 2

  override def transform(fromVersion: Int, json: JsonNode): JsonNode = {
    val root = json.asInstanceOf[ObjectNode]
    if (fromVersion <= 1) {
      root.set("itemId", root.get("productId"))
      root.remove("productId")
    }
    root
  }
}
```

### 클래스 이름 변경(Renaming Classes)

클래스 이름 변경을 처리하려면 `transformClassName()`을 재정의합니다.

```scala
class OrderPlacedMigration extends JacksonMigration {
  override def currentVersion: Int = 2

  override def transformClassName(fromVersion: Int, className: String): String =
    classOf[OrderPlaced].getName

  override def transform(fromVersion: Int, json: JsonNode): JsonNode = json
}
```

설정은 기존 클래스 이름(old class name)을 참조합니다.

```hocon
akka.serialization.jackson.migrations {
  "com.myservice.event.OrderAdded" = "com.myservice.event.OrderPlacedMigration"
}
```

---

## 26. 롤링 업데이트(Rolling Updates)

### 직렬화기 마이그레이션 전략(Serialization)

서로 다른 직렬화기 간 마이그레이션은 2단계(두 번의) 롤링 업데이트를 사용합니다.

**1단계:** 바인딩 없이 새 직렬화기를 등록
- 직렬화기 클래스를 설정에 추가
- 모든 노드에 배포
- 이 시점에서 기존 노드들은 아직 새 형식을 역직렬화할 수 없음

**2단계:** 직렬화 바인딩 갱신
- 메시지 클래스를 바인딩 설정에 추가
- 롤링 업데이트 배포
- 새 노드는 새 직렬화기를 사용하고, 기존 노드는 두 형식 모두 처리

**선택적 3단계:** 어떤 영속 이벤트(persistent event)도 사용하지 않는다면 기존 직렬화기를 제거합니다.

### Jackson 순방향 호환성을 통한 롤링 업데이트(Forward Compatibility)

무중단(zero-downtime) 배포를 위해, `supportedForwardVersion`을 사용하여 (현재는 기존 형식을 생성하면서도) 더 새로운 스키마(newer schemas)를 읽을 수 있습니다.

```scala
class ItemAddedMigration extends JacksonMigration {
  override def currentVersion: Int = 1
  override def supportedForwardVersion: Int = 2

  override def transform(fromVersion: Int, json: JsonNode): JsonNode = {
    val root = json.asInstanceOf[ObjectNode]
    if (fromVersion == 2) {
      // 더 새로운 스키마를 현재 버전으로 다운캐스트(down-cast)
      root.set("productId", root.get("itemId"))
      root.remove("itemId")
    }
    root
  }
}
```

모든 노드가 버전 2를 처리할 수 있게 되면, 갱신된 클래스 정의와 최종 마이그레이션 코드를 담은 두 번째 바이너리를 배포합니다.

---

## 27. 멀티노드 테스트(Multi Node Testing)

### 모듈 정보

멀티노드 테스트 툴킷(Multi Node Testing toolkit)은 Akka 버전 2.10.19에 대해 `akka-multi-node-testkit`으로 제공됩니다. Akka의 보안 라이브러리(secure library)에 접근하려면 https://account.akka.io/token 에서 토큰화된 URL(tokenized URL)이 필요합니다.

#### 의존성 설정

**sbt:**
```scala
val AkkaVersion = "2.10.19"
libraryDependencies += "com.typesafe.akka" %% "akka-multi-node-testkit" % AkkaVersion % Test
```

**Maven:**
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
    <artifactId>akka-multi-node-testkit_${scala.binary.version}</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

**Gradle:**
```gradle
def versions = [
  ScalaBinary: "2.13"
]
dependencies {
  implementation platform("com.typesafe.akka:akka-bom_${versions.ScalaBinary}:2.10.19")
  testImplementation "com.typesafe.akka:akka-multi-node-testkit_${versions.ScalaBinary}"
}
```

#### 프로젝트 메타데이터

- **아티팩트:** com.typesafe.akka / akka-multi-node-testkit
- **라이선스:** BUSL-1.1
- **상태:** Lightbend가 지원하는 정식 지원(Supported)
- **JDK 지원:** Eclipse Temurin 11, 17, 21
- **Scala 버전:** 2.13.17, 3.3.7
- **JPMS 모듈:** akka.remote.testkit

### 핵심 개념

멀티노드 테스트(Multi-node testing)는 별도의 JVM에서 실행되는 여러 액터 시스템에 걸쳐 테스트를 조율(coordinate)합니다. 이 툴킷은 세 가지 주요 구성 요소로 이루어집니다.

1. **테스트 컨덕터(Test Conductor)**: 노드를 오케스트레이션(orchestrate)
2. **MultiNodeSpec**: 편의 추상화(convenience abstractions) 제공
3. **SbtMultiJvm 플러그인**: 테스트 실행 관리

### 테스트 컨덕터(The Test Conductor)

테스트 컨덕터(TestConductor)는 "네트워크 스택에 끼워 넣어지는(plugs in to the network stack) Akka 확장(Akka Extension)"으로, 테스트 실행을 동기화합니다. 주요 기능은 다음과 같습니다.

- **노드 주소 조회(Node Address Lookup):** 공유 설정 없이 원격 테스트 노드까지의 전체 경로(full path)를 해석
- **배리어 조율(Barrier Coordination):** 노드들이 동료(peer)를 기다리는 동기화 지점(synchronization point)
- **네트워크 장애 주입(Network Failure Injection):** 패킷 손실(packet loss), 스로틀링(throttling), 노드 단절(disconnection)을 시뮬레이션

컨덕터 아키텍처는 배리어(barrier)와 클라이언트 연결을 관리하는 서버(server)를 사용하며, 클라이언트들은 장애 시나리오를 실행합니다. 서버는 배리어를 조율하고 트래픽 조작(traffic manipulation)을 위한 명령을 클라이언트에게 디스패치(dispatch)합니다.

### MultiNodeSpec

이 컴포넌트는 두 부분으로 구성됩니다.

- **MultiNodeConfig:** 공통 설정을 수립하고, 역할(roles)을 열거하며, 테스트 참가자(participants)에게 이름을 부여합니다.
- **MultiNodeSpec:** 노드 상호작용을 가능하게 하는 편의 함수(convenience functions)를 제공합니다.

#### 시스템 속성(System Properties) 설정

MultiNodeSpec은 Java 시스템 속성(`-Dproperty=value`)을 통해 설정합니다.

- `multinode.max-nodes`: 최대 테스트 노드 수
- `multinode.host`: 노드 호스트명/IP (InetAddress로 해석 가능해야 함)
- `multinode.port`: 노드 포트 (기본값 0은 무작위 할당)
- `multinode.server-host`: 서버 호스트명/IP
- `multinode.server-port`: 서버 포트 (기본값 4711)
- `multinode.index`: 노드 순서 인덱스 (0은 서버를 지정)

### SbtMultiJvm 플러그인

이 플러그인은 `multinode.*` 속성 생성을 자동화하여, 별도 설정 없이 단일 머신에서 실행할 수 있게 합니다. 멀티노드 확장은 SSH와 rsync를 사용한 여러 머신 간 분산 테스트(distributed testing)를 지원합니다.

#### 멀티노드 전용 설정(Multi-Node Specific Settings)

- `multiNodeHosts`: 호스트 시퀀스, 형식: `user@host:java`
- `multiNodeHostsFileName`: 호스트를 담은 파일 (기본값 `multi-node-test.hosts`)
- `multiNodeTargetDirName`: 원격 디렉터리 이름 (기본값 `multi-node-test`)
- `multiNodeJavaName`: 대상의 Java 실행 파일 이름 (기본값 `java`)

#### 호스트 정의 예시

```
localhost
user1@host1
user2@host2:/usr/lib/jvm/java-7-openjdk-amd64/bin/java
host3:/usr/lib/jvm/java-6-openjdk-amd64/bin/java
```

플러그인은 SbtAssembly를 사용하여 테스트 클래스와 의존성을 JAR로 패키징합니다: `<projectName>_<scalaVersion>-<projectVersion>-multi-jvm-assembly.jar`

#### 테스트 실행

**멀티노드 모드(분산):**
```
multiNodeTest
multiNodeTestOnly your.MultiNodeTest
```

**멀티 JVM 모드(로컬):**
```
multi-jvm:test
multi-jvm:testOnly your.MultiNodeTest
```

### 구현 예시

#### ScalaTest 통합 트레이트

```scala
package akka.remote.testkit

import scala.language.implicitConversions
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

/**
 * MultiNodeSpec을 ScalaTest와 연결합니다.
 */
trait STMultiNodeSpec
  extends MultiNodeSpecCallbacks
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  self: MultiNodeSpec =>

  override def beforeAll() = multiNodeSpecBeforeAll()
  override def afterAll() = multiNodeSpecAfterAll()

  override implicit def convertToWordSpecStringWrapper(s: String):
    WordSpecStringWrapper =
    new WordSpecStringWrapper(
      s"$s (on node '${self.myself.name}', $getClass)")
}
```

#### 설정 정의

```scala
package akka.remote.sample
import akka.remote.testkit.{ MultiNodeConfig, STMultiNodeSpec }

object MultiNodeSampleConfig extends MultiNodeConfig {
  val node1 = role("node1")
  val node2 = role("node2")
}
```

#### 테스트 구현

```scala
package akka.remote.sample

import akka.actor.{ Actor, Props }
import akka.remote.testkit.MultiNodeSpec
import akka.testkit.ImplicitSender

class MultiNodeSampleSpecMultiJvmNode1 extends MultiNodeSample
class MultiNodeSampleSpecMultiJvmNode2 extends MultiNodeSample

object MultiNodeSample {
  class Ponger extends Actor {
    def receive = {
      case "ping" => sender() ! "pong"
    }
  }
}

class MultiNodeSample
  extends MultiNodeSpec(MultiNodeSampleConfig)
  with STMultiNodeSpec
  with ImplicitSender {

  import MultiNodeSample._
  import MultiNodeSampleConfig._

  def initialParticipants = roles.size

  "A MultiNodeSample" must {
    "wait for all nodes to enter a barrier" in {
      enterBarrier("startup")
    }

    "send to and receive from a remote node" in {
      runOn(node1) {
        enterBarrier("deployed")
        val ponger = system.actorSelection(node(node2) / "user" / "ponger")
        ponger ! "ping"
        import scala.concurrent.duration._
        expectMsg(10.seconds, "pong")
      }

      runOn(node2) {
        system.actorOf(Props[Ponger](), "ponger")
        enterBarrier("deployed")
      }

      enterBarrier("finished")
    }
  }
}
```

### 명심해야 할 사항(Things to Keep in Mind)

안정적인 멀티노드 테스트를 위한 핵심 지침은 다음과 같습니다.

- **노드 0 보호:** 첫 번째 노드(node 0)는 테스트 실행을 제어하므로 종료(shut down)하지 마십시오.
- **장애 주입(Failure Injection):** `blackhole`, `passThrough`, `throttle`을 사용하려면 MultiNodeConfig에서 `testTransport(on = true)`로 장애 주입기(failure injector)/스로틀러(throttler) 어댑터를 활성화하십시오.
- **장애 주입 위치:** 스로틀링과 장애 주입은 오직 노드 0에서만 실행하십시오.
- **주소 캐싱(Address Caching):** 종료 전에 노드 주소를 미리 가져오십시오. 종료 후에는 `node(address)` 호출이 실패합니다.
- **스레드 제약(Thread Restrictions):** MultiNodeSpec 메서드(주소 조회, 배리어 진입)를 메인 테스트 스레드(main test thread) 외부에서 호출하지 마십시오. 액터, 퓨처(future), 스케줄링된 작업에서 절대 호출하지 마십시오.

### 설정 참조(Configuration Reference)

추가적인 멀티노드 테스트 모듈 속성은 레퍼런스 설정 문서(reference configuration documentation, 섹션: akka-multi-node-testkit)에 존재합니다. 고급 동작 커스터마이징(advanced behavioral customization)을 위해 이 설정들을 참고하십시오.

---

## 참고 자료

- [Akka 공식 문서](https://doc.akka.io/libraries/akka-core/current/)
- [Remoting (Artery)](https://doc.akka.io/libraries/akka-core/current/remoting-artery.html)
- [Remote Security](https://doc.akka.io/libraries/akka-core/current/remote-security.html)
- [Serialization](https://doc.akka.io/libraries/akka-core/current/serialization.html)
- [Serialization with Jackson](https://doc.akka.io/libraries/akka-core/current/serialization-jackson.html)
- [Multi Node Testing](https://doc.akka.io/libraries/akka-core/current/multi-node-testing.html)

# Tier-1 투자은행의 초저지연 트레이딩 시스템을 Apache Kafka로 설계하기

> **원문:** [How a Tier-1 Investment Bank Designs Its Ultra-Low-Latency Trading System Using Apache Kafka](https://www.confluent.io/blog/tier-1-bank-ultra-low-latency-trading-design/)
>
> **저자:** Daniel Ish-Shalom, Senior Solutions Engineer, Confluent
>
> **게시일:** 2025년 3월 26일

---

## 소개

금융 서비스 산업에서 속도는 곧 돈이다. 특히 전자 트레이딩(Electronic Trading) 분야에서는 마이크로초(microsecond) 단위의 지연 시간 차이가 수백만 달러의 수익 또는 손실을 결정할 수 있다. Tier-1 투자은행들은 최적의 가격에 거래를 체결하기 위해 초저지연(ultra-low latency) 시스템에 막대한 투자를 하고 있다.

Apache Kafka는 전통적으로 높은 처리량(high throughput)과 내구성(durability)을 갖춘 데이터 스트리밍 플랫폼으로 알려져 있다. 그러나 Kafka가 마이크로초 수준의 지연 시간이 요구되는 트레이딩 시스템에 적합할 수 있을까? 이 블로그 포스트에서는 Tier-1 투자은행이 어떻게 Apache Kafka를 활용하여 초저지연 트레이딩 아키텍처를 설계하는지, 그리고 이를 통해 지연 시간을 최소화하면서도 Kafka의 핵심적인 이점을 유지하는 방법을 살펴본다.

---

## 왜 트레이딩에 Kafka를 사용하는가?

Kafka가 트레이딩 시스템에서 선택되는 데는 여러 가지 이유가 있다:

- **이벤트 기반 아키텍처(Event-Driven Architecture):** Kafka는 본질적으로 이벤트 기반 시스템이며, 트레이딩 워크플로우는 본질적으로 이벤트의 연속이다. 시장 데이터 업데이트, 주문 제출, 체결 확인 등 모든 것이 이벤트다.
- **내구성과 복제(Durability and Replication):** Kafka의 복제 메커니즘은 메시지 손실 없이 장애를 견딜 수 있게 해주며, 이는 금융 규제 준수에 필수적이다.
- **확장성(Scalability):** 트레이딩 볼륨이 급증하는 시장 이벤트 시에도 Kafka는 수평적으로 확장할 수 있다.
- **감사 추적(Audit Trail):** Kafka의 불변 로그(immutable log)는 모든 거래에 대한 완전한 감사 추적을 제공한다.
- **생태계 통합(Ecosystem Integration):** Kafka Connect, Kafka Streams, ksqlDB 등 풍부한 생태계를 통해 다양한 시스템과 통합할 수 있다.

그러나 기본 Kafka 설정만으로는 초저지연 요구사항을 충족하기 어렵다. 트레이딩 시스템에 적합하도록 세밀한 튜닝이 필요하다.

---

## 초저지연 트레이딩 아키텍처의 핵심 설계 원칙

### 지연 시간의 구성 요소 이해

Kafka 기반 트레이딩 시스템에서 엔드투엔드(end-to-end) 지연 시간은 여러 구성 요소로 나뉜다:

1. **Producer 지연 시간:** 메시지가 생성되어 Kafka 브로커에 도달하는 데 걸리는 시간
2. **브로커 처리 지연 시간:** 브로커가 메시지를 수신하고, 저장하고, 복제하는 데 걸리는 시간
3. **Consumer 지연 시간:** Consumer가 브로커로부터 메시지를 가져오는 데 걸리는 시간
4. **네트워크 지연 시간:** 네트워크를 통한 데이터 전송 시간

각 구성 요소를 최적화하는 것이 전체 지연 시간을 최소화하는 핵심이다.

---

### Producer 최적화

Producer 측에서 지연 시간을 최소화하기 위한 핵심 설정은 다음과 같다:

#### `linger.ms = 0`

기본적으로 Kafka Producer는 배치 효율성을 위해 메시지를 잠시 대기시킬 수 있다. 초저지연 환경에서는 `linger.ms`를 0으로 설정하여 메시지가 즉시 전송되도록 한다.

#### `batch.size` 최소화

배치 크기를 줄여 메시지가 버퍼에 쌓이지 않고 즉시 전송되도록 한다. 그러나 이는 처리량과의 트레이드오프가 있다.

#### `acks` 설정

- `acks=0`: Producer는 브로커의 확인을 기다리지 않는다. 가장 빠르지만 메시지 손실 위험이 있다.
- `acks=1`: 리더 브로커의 확인만 기다린다. 지연 시간과 안정성의 균형점이다.
- `acks=all`: 모든 인-싱크 레플리카(ISR)의 확인을 기다린다. 가장 안전하지만 지연 시간이 증가한다.

Tier-1 은행들은 일반적으로 트레이딩 경로(trading path)에서는 `acks=1`을, 주문 확인이나 결제 같은 크리티컬 경로에서는 `acks=all`을 사용하는 하이브리드 접근법을 채택한다.

#### `compression.type = none`

압축은 CPU 오버헤드를 추가하므로, 초저지연 환경에서는 압축을 비활성화한다. 네트워크 대역폭은 충분히 확보되어 있는 환경을 가정한다.

#### `max.in.flight.requests.per.connection`

이 값을 1로 설정하면 순서를 보장할 수 있지만, 파이프라이닝(pipelining)을 제한하여 지연 시간이 증가할 수 있다. 트레이딩 시스템에서는 순서 보장이 중요한 경우가 많으므로, 파티션 전략과 함께 고려해야 한다.

---

### 브로커 최적화

#### 디스크 I/O 최적화

Kafka 브로커의 성능은 디스크 I/O에 크게 의존한다. 초저지연 환경에서는:

- **NVMe SSD** 사용: 기존 SAS/SATA SSD보다 훨씬 빠른 읽기/쓰기 성능을 제공한다.
- **페이지 캐시(Page Cache) 활용:** Kafka는 OS의 페이지 캐시를 적극적으로 활용한다. 충분한 메모리를 확보하여 활성 세그먼트(active segment)가 항상 캐시에 존재하도록 한다.
- **전용 디스크:** Kafka 로그 디렉토리를 전용 디스크에 배치하여 다른 워크로드와의 I/O 경합을 방지한다.

#### 네트워크 최적화

- **커널 바이패스(Kernel Bypass):** DPDK(Data Plane Development Kit)나 RDMA(Remote Direct Memory Access) 같은 기술을 활용하여 커널 네트워크 스택을 우회한다.
- **TCP 튜닝:** `socket.send.buffer.bytes`와 `socket.receive.buffer.bytes`를 적절히 설정하고, TCP_NODELAY를 활성화하여 Nagle 알고리즘을 비활성화한다.
- **네트워크 인터페이스 카드(NIC) 최적화:** 인터럽트 코얼레싱(interrupt coalescing)을 최소화하고, RSS(Receive Side Scaling)를 적절히 설정한다.

#### `num.replica.fetchers`

레플리카 페처(replica fetcher) 스레드 수를 늘리면 복제 지연 시간을 줄일 수 있다. 이는 `acks=all` 설정에서 특히 중요하다.

#### `log.flush.interval.messages`와 `log.flush.interval.ms`

Kafka는 기본적으로 OS에 fsync를 위임한다. 명시적 flush 간격을 설정하는 것은 일반적으로 권장되지 않지만, 특정 내구성 요구사항이 있는 경우 조정할 수 있다. 초저지연 환경에서는 OS의 페이지 캐시 flush 메커니즘에 의존하는 것이 일반적이다.

---

### Consumer 최적화

#### `fetch.min.bytes = 1`

Consumer가 최소 바이트 수를 기다리지 않고 즉시 데이터를 가져오도록 한다.

#### `fetch.max.wait.ms` 최소화

브로커가 `fetch.min.bytes`를 충족하지 못하더라도 빠르게 응답하도록 최대 대기 시간을 최소화한다.

#### 폴링 전략(Polling Strategy)

Consumer는 타이트 루프(tight loop)에서 `poll()`을 호출하여 새 메시지를 최대한 빠르게 가져온다. 폴링 간격을 최소화하는 것이 핵심이다.

#### Consumer 스레딩 모델

단일 Consumer 스레드가 파티션에서 메시지를 가져오고, 별도의 처리 스레드 풀에서 비즈니스 로직을 실행하는 모델을 사용한다. 이렇게 하면 메시지 페치와 처리를 병렬화할 수 있다.

---

### 토픽 및 파티션 설계

#### 파티션 수 결정

파티션 수는 병렬 처리 수준을 결정한다. 그러나 파티션이 너무 많으면:

- 리더 선출에 시간이 더 걸린다
- 프로듀서 측에서 더 많은 배치가 필요하다
- 메타데이터 오버헤드가 증가한다

트레이딩 시스템에서는 금융 상품(instrument)이나 심볼(symbol)별로 파티셔닝하는 것이 일반적이며, 이를 통해 같은 상품에 대한 이벤트가 순서대로 처리되도록 보장한다.

#### 파티셔닝 전략

커스텀 파티셔너(Custom Partitioner)를 사용하여 특정 트레이딩 심볼이나 주문 ID를 기반으로 파티션을 결정한다. 이는 관련 이벤트가 같은 파티션에 도달하여 순서가 보장되도록 한다.

#### 토픽 설계

- **핫 패스(Hot Path)와 콜드 패스(Cold Path) 분리:** 시장 데이터, 주문 라우팅 같은 지연 시간에 민감한 이벤트는 별도의 토픽으로 분리하고, 리스크 분석이나 보고 같은 덜 민감한 이벤트와 분리한다.
- **리텐션(Retention) 정책:** 핫 패스 토픽은 짧은 리텐션 기간을 설정하여 디스크 사용량을 최소화하고, 콜드 패스 토픽은 규제 요구사항에 맞게 긴 리텐션을 유지한다.

---

## 하드웨어 및 인프라 고려사항

### 코로케이션(Co-location)

Tier-1 은행들은 Kafka 브로커를 거래소 매칭 엔진 가까이에 코로케이션하여 네트워크 지연 시간을 최소화한다. 같은 데이터 센터, 이상적으로는 같은 랙 내에 배치하는 것이 중요하다.

### CPU 핀닝(CPU Pinning)과 NUMA 인식

- **CPU 핀닝:** Kafka 브로커 프로세스와 트레이딩 애플리케이션 스레드를 특정 CPU 코어에 고정하여 컨텍스트 스위칭(context switching) 오버헤드를 줄인다.
- **NUMA(Non-Uniform Memory Access) 인식:** NUMA 노드 경계를 넘는 메모리 접근을 최소화하여 메모리 지연 시간을 줄인다.

### 네트워크 하드웨어

- **25Gbps/100Gbps 이더넷:** 충분한 네트워크 대역폭을 확보한다.
- **커널 바이패스 NIC:** Solarflare, Mellanox 같은 저지연 NIC를 사용한다.
- **스위치 최적화:** 네트워크 스위치의 지연 시간을 최소화하고, 컷스루(cut-through) 스위칭을 활성화한다.

### JVM 튜닝

Kafka 브로커와 Java 기반 클라이언트는 JVM 위에서 동작한다. 초저지연을 위한 JVM 튜닝:

- **GC(Garbage Collection) 최적화:** ZGC나 Shenandoah 같은 저지연 가비지 컬렉터를 사용하여 GC 일시 정지(pause) 시간을 최소화한다.
- **힙 크기 설정:** 힙을 너무 크게 잡으면 GC 일시 정지가 길어질 수 있다. 적절한 크기를 설정하고, 오프-힙(off-heap) 메모리를 활용한다.
- **JIT(Just-In-Time) 컴파일:** 웜업(warm-up) 기간을 두어 JIT 컴파일러가 핫 코드를 최적화할 시간을 확보한다.

---

## 아키텍처 패턴

### 이벤트 소싱(Event Sourcing) 패턴

Kafka의 불변 로그는 이벤트 소싱 패턴에 자연스럽게 적합하다. 트레이딩 시스템에서:

- 모든 주문 상태 변경이 이벤트로 기록된다
- 현재 상태는 이벤트 시퀀스를 재생(replay)하여 재구성할 수 있다
- 감사 추적이 자연스럽게 구축된다
- 장애 복구 시 이벤트를 재생하여 상태를 복원할 수 있다

### CQRS(Command Query Responsibility Segregation)

명령(쓰기)과 조회(읽기)를 분리하는 패턴:

- **명령 측:** 주문 제출, 취소 등의 명령은 Kafka 토픽에 기록된다
- **조회 측:** 별도의 Consumer가 이벤트를 소비하여 읽기 최적화된 뷰(예: 주문 장부, 포지션 뷰)를 구축한다

이 분리를 통해 쓰기 경로의 지연 시간을 최소화하면서도 풍부한 조회 기능을 제공할 수 있다.

### 시장 데이터 분배(Market Data Distribution)

시장 데이터 피드는 트레이딩 시스템에서 가장 지연 시간에 민감한 경로 중 하나다:

1. **시장 데이터 수집기(Ingester):** 거래소로부터 시장 데이터를 수신하여 Kafka 토픽에 게시한다
2. **정규화(Normalization):** 다양한 거래소 형식을 표준 형식으로 변환한다
3. **분배(Distribution):** 구독하는 트레이딩 전략과 리스크 시스템에 데이터를 분배한다

Kafka를 사용하면 멀티캐스트 방식으로 여러 Consumer에게 동시에 데이터를 분배할 수 있으며, Consumer 그룹을 통해 수평 확장이 가능하다.

---

## 모니터링과 관측 가능성(Observability)

### 핵심 메트릭(Key Metrics)

초저지연 트레이딩 시스템에서 반드시 모니터링해야 하는 Kafka 메트릭:

- **엔드투엔드 지연 시간(End-to-End Latency):** Producer에서 Consumer까지의 전체 지연 시간. P50, P99, P99.9 백분위수를 모니터링한다.
- **Producer 요청 지연 시간:** `request-latency-avg`, `request-latency-max`
- **브로커 요청 처리 시간:** `RequestHandlerAvgIdlePercent`, `NetworkProcessorAvgIdlePercent`
- **Consumer 지연(Lag):** `records-lag-max`. Consumer가 처리하지 못한 메시지 수가 증가하면 즉시 경고를 발생시킨다.
- **ISR 수축(ISR Shrink):** `IsrShrinksPerSec`. 이 메트릭이 증가하면 복제 문제가 발생하고 있음을 나타낸다.
- **Under-Replicated 파티션:** 복제가 뒤처진 파티션 수

### 분산 추적(Distributed Tracing)

각 메시지에 타임스탬프와 추적 ID를 포함하여, 시스템 전반에 걸친 메시지의 경로와 지연 시간을 추적한다. OpenTelemetry 같은 프레임워크를 활용할 수 있다.

### 이상 탐지(Anomaly Detection)

정상적인 지연 시간 패턴을 학습하고, 이상 징후를 자동으로 탐지하는 시스템을 구축한다. 예를 들어, 평소 P99 지연 시간이 1ms인데 갑자기 5ms로 증가하면 즉시 알림을 발생시킨다.

---

## 복원력과 장애 허용(Resilience and Fault Tolerance)

### 다중 데이터 센터 배포

Tier-1 은행들은 일반적으로 다중 데이터 센터에 Kafka 클러스터를 배포한다:

- **액티브-패시브(Active-Passive):** 하나의 데이터 센터가 주 트래픽을 처리하고, 다른 데이터 센터는 대기 상태로 유지된다. MirrorMaker 2 또는 Confluent Cluster Linking을 통해 데이터를 복제한다.
- **액티브-액티브(Active-Active):** 두 데이터 센터 모두 트래픽을 처리한다. 이는 더 복잡하지만 더 낮은 장애 복구 시간(RTO)을 제공한다.

### Confluent Cluster Linking

Confluent Cluster Linking은 다중 클러스터 간 토픽을 실시간으로 미러링하는 기능을 제공한다. MirrorMaker 2에 비해:

- 더 낮은 복제 지연 시간
- 오프셋(offset) 보존
- Consumer 그룹 마이그레이션 지원
- 관리 오버헤드 감소

### 그레이스풀 디그레이데이션(Graceful Degradation)

시스템 장애 시 전체 서비스가 중단되지 않도록 그레이스풀 디그레이데이션 전략을 구현한다:

- **서킷 브레이커(Circuit Breaker):** 다운스트림 시스템에 장애가 발생하면 자동으로 트래픽을 차단한다
- **백프레셔(Backpressure):** 시스템이 과부하 상태일 때 Producer의 전송 속도를 조절한다
- **폴백(Fallback):** 주요 시스템에 장애가 발생하면 대체 경로로 트래픽을 라우팅한다

---

## Kafka를 넘어서: 하이브리드 접근법

많은 Tier-1 은행들은 순수한 Kafka 기반 아키텍처 대신 하이브리드 접근법을 채택한다:

### 핫 패스: 전용 메시징

가장 지연 시간에 민감한 경로(예: 주문 라우팅, 시장 데이터)에서는 Kafka 대신 공유 메모리(shared memory), 커널 바이패스 메시징, 또는 Aeron 같은 초저지연 전용 메시징 시스템을 사용할 수 있다.

### 웜/콜드 패스: Kafka

리스크 계산, 포지션 관리, 규제 보고, 감사 추적 같은 경로에서는 Kafka의 내구성, 확장성, 풍부한 생태계를 최대한 활용한다.

### 통합 계층

두 경로 사이의 통합 계층에서 데이터가 핫 패스에서 Kafka로 흘러들어가 더 넓은 시스템에서 소비될 수 있도록 한다. 이를 통해:

- 핫 패스의 초저지연 요구사항을 충족하면서
- Kafka의 내구성과 확장성으로 나머지 시스템을 지원한다

---

## 벤치마킹과 테스트

### 지연 시간 벤치마킹

프로덕션 배포 전에 철저한 벤치마킹을 수행해야 한다:

- **마이크로 벤치마크:** 개별 구성 요소(Producer, 브로커, Consumer)의 지연 시간을 측정한다
- **엔드투엔드 벤치마크:** 전체 파이프라인의 지연 시간을 측정한다
- **부하 테스트:** 다양한 부하 수준에서의 지연 시간 프로파일을 파악한다
- **꼬리 지연 시간(Tail Latency) 분석:** P99, P99.9, P99.99 백분위수를 주의 깊게 분석한다. 평균 지연 시간이 아닌 꼬리 지연 시간이 트레이딩 성과를 결정한다.

### 카오스 엔지니어링(Chaos Engineering)

프로덕션과 유사한 환경에서 장애를 시뮬레이션하여 시스템의 복원력을 테스트한다:

- 브로커 장애
- 네트워크 파티션
- 디스크 장애
- GC 일시 정지 주입
- CPU 스로틀링

---

## 규제 고려사항

금융 트레이딩 시스템은 엄격한 규제 요구사항을 충족해야 한다:

- **MiFID II/MiFIR:** 유럽에서는 주문의 타임스탬프 정확도가 마이크로초 수준이어야 하며, 모든 거래 활동에 대한 상세한 기록을 유지해야 한다.
- **SEC Rule 613 (CAT):** 미국에서는 Consolidated Audit Trail에 주문 이벤트를 보고해야 한다.
- **데이터 보존:** 규제에 따라 거래 기록을 일정 기간(일반적으로 5-7년) 보관해야 한다.

Kafka의 불변 로그와 보존 정책은 이러한 규제 요구사항을 충족하는 데 자연스러�게 적합하다. Tiered Storage를 활용하면 장기 보관 비용도 절감할 수 있다.

---

## Confluent의 역할

Confluent Platform은 초저지연 트레이딩 시스템을 위한 여러 엔터프라이즈 기능을 제공한다:

- **Confluent Server:** 오픈 소스 Kafka를 넘어선 성능 최적화와 엔터프라이즈 기능
- **Cluster Linking:** 다중 데이터 센터 간 실시간 복제
- **Tiered Storage:** 규제 준수를 위한 장기 데이터 보존
- **Schema Registry:** 메시지 스키마 관리와 진화(evolution) 지원
- **Confluent Control Center:** 종합적인 모니터링과 관리
- **Self-Balancing Clusters:** 자동 파티션 리밸런싱으로 운영 부담 감소

---

## 결론

Tier-1 투자은행이 초저지연 트레이딩 시스템에 Apache Kafka를 활용하는 것은 단순히 Kafka를 배포하는 것 이상을 의미한다. 하드웨어, 네트워크, 운영 체제, JVM, Kafka 설정, 애플리케이션 코드에 이르기까지 전체 스택에 걸친 세밀한 최적화가 필요하다.

핵심 요약:

- **Producer, 브로커, Consumer 각각에서 지연 시간을 최적화한다.** `linger.ms=0`, `fetch.min.bytes=1` 같은 설정이 기본이다.
- **하드웨어 선택이 중요하다.** NVMe SSD, 저지연 NIC, CPU 핀닝은 필수적이다.
- **하이브리드 아키텍처를 고려한다.** 가장 민감한 경로에는 전용 메시징을, 나머지에는 Kafka를 활용한다.
- **모니터링과 벤치마킹은 지속적으로 수행한다.** 꼬리 지연 시간에 특히 주의한다.
- **규제 준수를 설계에 포함한다.** Kafka의 불변 로그와 보존 정책을 활용한다.

Apache Kafka는 올바르게 튜닝하고 적절한 아키텍처 패턴과 결합하면, 초저지연 트레이딩 시스템의 핵심 인프라로 충분히 활용할 수 있다. Confluent Platform의 엔터프라이즈 기능은 이러한 여정을 더욱 가속화한다.

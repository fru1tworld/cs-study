# Druid 프로세스와 서비스

> 원본: https://druid.apache.org/docs/latest/design/architecture
> 원본: https://druid.apache.org/docs/latest/design/coordinator
> 원본: https://druid.apache.org/docs/latest/design/overlord
> 원본: https://druid.apache.org/docs/latest/design/broker
> 원본: https://druid.apache.org/docs/latest/design/historical
> 원본: https://druid.apache.org/docs/latest/design/middlemanager
> 원본: https://druid.apache.org/docs/latest/design/indexer
> 원본: https://druid.apache.org/docs/latest/design/peons
> 원본: https://druid.apache.org/docs/latest/design/router

Druid를 구성하는 서비스(프로세스)의 종류와 역할, 서버 유형별 배치 방식, 그리고 각 서비스의 동작 방식과 설정을 설명합니다.

---

## 목차

1. [서비스와 서버 유형 개요](#서비스와-서버-유형-개요)
2. [Coordinator](#coordinator)
3. [Overlord](#overlord)
4. [Broker](#broker)
5. [Historical](#historical)
6. [Middle Manager](#middle-manager)
7. [Peon](#peon)
8. [Indexer](#indexer)
9. [Router](#router)
10. [참고 자료](#참고-자료)

---

## 서비스와 서버 유형 개요

### 서비스 종류

Druid는 여러 종류의 서비스로 구성됩니다.

| 서비스 | 역할 |
| --- | --- |
| **Coordinator** | 클러스터의 데이터 가용성(availability)을 관리합니다. |
| **Overlord** | 데이터 인제스천(ingestion) 워크로드의 할당을 제어합니다. |
| **Broker** | 외부 클라이언트의 쿼리를 처리합니다. |
| **Router** | 요청을 Broker, Coordinator, Overlord로 라우팅합니다. |
| **Historical** | 쿼리 가능한 데이터를 저장합니다. |
| **Middle Manager**와 **Peon** | 데이터를 적재합니다. Peon은 Middle Manager가 생성하는 별도 JVM입니다. |
| **Indexer** | Middle Manager + Peon 조합을 대체하는 실험적(experimental) 서비스입니다. |

### 서버 유형

Druid 서비스는 목적에 따라 세 가지 서버 유형에 나누어 배치하는 방식을 권장합니다.

**Master 서버** — 데이터 인제스천과 가용성을 관리합니다.

- **Coordinator**: Historical 서비스를 감시하고 세그먼트(segment)를 할당하며 균형을 유지합니다.
- **Overlord**: Middle Manager를 감시하고 인제스천 태스크(task)를 할당하며 세그먼트 발행(publish)을 조율합니다.

**Query 서버** — 사용자와 클라이언트가 접근하는 엔드포인트를 제공합니다.

- **Broker**: 외부 쿼리를 받아 Data 서버로 전달하고 결과를 병합합니다.
- **Router**: 통합 API 게이트웨이 역할을 하며, 관리용 웹 콘솔을 제공합니다.

**Data 서버** — 인제스천 작업을 실행하고 쿼리 가능한 데이터를 저장합니다.

- **Historical**: 과거 데이터와 커밋된 스트리밍 데이터의 저장·쿼리를 담당합니다.
- **Middle Manager**: 새 데이터를 적재하고 세그먼트를 발행합니다.
- **Peon**: Middle Manager가 별도 JVM으로 생성해 개별 태스크를 실행합니다.

### 배치 권장 사항

- 세그먼트 수가 매우 많은 클러스터에서는 Coordinator와 Overlord를 분리해 자원을 각각 할당하는 편이 좋습니다.
- 인제스천 부하나 쿼리 부하가 커지면 Historical과 Middle Manager를 서로 다른 호스트에 배치해 CPU·메모리 경합을 피할 수 있습니다.
- 인제스천 실행 방식은 MiddleManager, MiddleManager 없는 Kubernetes 기반 인제스천, Indexer 중 하나만 선택해 배포하는 것이 일반적이며, 여러 방식을 동시에 운용하지 않습니다.

---

## Coordinator

Coordinator는 세그먼트의 생명주기(lifecycle)를 관리하는 서비스입니다. 설정에 따라 Historical 서비스에 세그먼트를 로드하거나 삭제하도록 지시하고, 세그먼트가 적절히 복제되도록 보장하며, 클러스터 전체에 세그먼트가 고르게 분포하도록 균형을 맞춥니다.

### 실행

```
org.apache.druid.cli.Main server coordinator
```

### 주요 기능

**세그먼트 관리**

- 새 세그먼트 로드와 오래된 세그먼트 삭제
- Historical 노드 간 복제 유지
- 클러스터 균형을 위한 세그먼트 재배치

**정리(cleanup) 작업**

- 새 데이터로 대체된(overshadowed) 이전 버전 세그먼트 제거
- 특정 조건(-INF에서 시작하거나 INF에서 끝나며 core partition이 0인 경우)을 만족하는 미사용 eternity tombstone 세그먼트 제거

**세그먼트 가용성**

Historical 서비스가 재시작하면 Coordinator가 장애를 감지하고 해당 세그먼트를 다른 노드에 재할당합니다. 다만 설정한 수명(lifetime)이 지나면 만료되는 임시 구조를 유지하기 때문에, 노드가 빠르게 복구되면 불필요한 재할당을 하지 않습니다.

### 밸런싱 전략

세그먼트 분배에는 세 가지 전략이 있습니다.

| 전략 | 설명 |
| --- | --- |
| **cost** (기본값) | 데이터 구간(interval)의 근접도에 기반한 배치 비용을 최소화합니다. 시간상 인접한 세그먼트가 같은 서버에 몰리지 않게 합니다. |
| **diskNormalized** | 서버의 디스크 사용 비율로 비용에 가중치를 둡니다. 알려진 문제가 있습니다. |
| **random** | 실험적 전략으로, 프로덕션에서는 권장하지 않습니다. |

모든 전략은 디스크 여유가 가장 부족한 Historical에서 세그먼트를 우선 옮깁니다.

### 자동 컴팩션(Automatic Compaction)

Coordinator는 작은 세그먼트를 병합하거나 큰 세그먼트를 분할하는 컴팩션을 관리합니다. 컴팩션에 쓸 수 있는 태스크 용량은 다음과 같이 계산합니다.

```
min(전체 worker capacity의 합 * slotRatio, maxSlots)
```

컴팩션이 활성화되어 있으면 최소 한 개의 컴팩션 태스크는 항상 제출됩니다.

Coordinator의 듀티(duty)는 별도 그룹으로 분리해 실행 주기를 따로 지정할 수 있습니다.

```
druid.coordinator.dutyGroups
druid.coordinator.<SOME_GROUP_NAME>.duties
druid.coordinator.<SOME_GROUP_NAME>.period
```

### 연결과 설정

Coordinator는 클러스터 정보를 위해 ZooKeeper에 연결하고, 세그먼트 메타데이터와 로드 규칙(rule)을 위해 메타데이터 데이터베이스에 연결합니다. 상세 설정은 [Coordinator configuration](https://druid.apache.org/docs/latest/configuration/#coordinator)과 [Rule Configuration](https://druid.apache.org/docs/latest/operations/rule-configuration)을 참고하십시오.

### FAQ 요점

- 클라이언트는 Coordinator에 직접 접속하지 않습니다.
- Historical과 Broker 서비스는 Coordinator의 존재를 인지하지 않습니다.
- 서비스 시작 순서는 중요하지 않습니다. Coordinator가 없으면 토폴로지 변경(세그먼트 로드·삭제·재배치)만 일어나지 않을 뿐입니다.

---

## Overlord

Overlord는 태스크의 생명주기를 관리합니다. 태스크를 접수하고, 태스크 분배를 조율하고, 태스크 락(lock)을 생성하며, 호출자에게 상태를 반환합니다.

### 실행 모드

| 모드 | 설명 |
| --- | --- |
| **local** (기본값) | Overlord가 직접 Peon을 생성해 태스크를 실행합니다. Middle Manager와 Peon 설정이 함께 필요하며, 단순한 워크플로에 적합합니다. |
| **remote** | Overlord와 Middle Manager가 서로 다른 서버에서 별도 서비스로 동작합니다. indexing service를 Druid 인제스천의 주 엔드포인트로 사용할 때 권장합니다. |

### 워커 블랙리스트(Blacklisted Workers)

Middle Manager의 태스크 실패가 임계값을 초과하면 Overlord가 해당 워커를 블랙리스트에 올립니다. 관련 설정은 다음과 같습니다.

```
druid.indexer.runner.maxRetriesBeforeBlacklist
druid.indexer.runner.workerBlackListBackoffTime
druid.indexer.runner.workerBlackListCleanupPeriod
druid.indexer.runner.maxPercentageBlacklistWorkers
```

동시에 블랙리스트에 올릴 수 있는 Middle Manager는 최대 20%이며, 블랙리스트에 오른 워커는 주기적으로 다시 화이트리스트로 돌아옵니다.

### 오토스케일링(Autoscaling)

오토스케일링을 활성화하면, 태스크가 오랫동안 pending 상태로 남을 때 새 Middle Manager를 추가하고, 오랫동안 유휴 상태인 Middle Manager를 제거합니다.

상세 설정은 [Overlord configuration](https://druid.apache.org/docs/latest/configuration/#overlord)을, HTTP 엔드포인트는 Service status API reference를 참고하십시오.

---

## Broker

Broker는 분산 클러스터 구성에서 쿼리를 라우팅하는 서비스입니다. 어떤 세그먼트가 어느 서비스에 있는지를 담은 ZooKeeper 메타데이터를 해석해 쿼리를 알맞은 서비스로 전달하고, 개별 서비스가 반환한 결과 집합을 병합해 최종 결과를 만듭니다.

### 실행

```
org.apache.druid.cli.Main server broker
```

### 쿼리 전달(Forwarding Queries)

Broker는 ZooKeeper 정보를 바탕으로 세그먼트의 타임라인과 각 세그먼트를 서빙하는 Historical·Peon 서비스를 파악합니다. 시간 구간이 포함된 쿼리가 들어오면, 해당 데이터소스의 타임라인에서 쿼리 구간을 조회해 데이터를 가진 서비스를 찾아 쿼리를 전달합니다.

### 캐싱(Caching)

Broker는 LRU 무효화 전략을 쓰는 캐시로 세그먼트별(per-segment) 결과를 저장합니다. 캐시는 로컬에 둘 수도 있고 memcached로 분산 구성할 수도 있습니다. 실시간(real-time) 세그먼트는 내용이 계속 변해 캐시된 결과를 신뢰할 수 없으므로 캐시하지 않습니다.

상세 설정은 [Broker configuration](https://druid.apache.org/docs/latest/configuration/#broker)을 참고하십시오.

---

## Historical

Historical은 과거 데이터의 저장과 쿼리를 담당하는 서비스입니다. 세그먼트를 로컬에 캐시하고, 디스크 캐시와 메모리에서 쿼리를 서빙합니다.

### 실행

```
org.apache.druid.cli.Main server historical
```

### 세그먼트 로딩

Historical은 딥 스토리지(deep storage)에서 세그먼트 파일을 받아 로컬 세그먼트 캐시에 저장합니다. Coordinator는 Historical과 직접 통신하지 않고 ZooKeeper의 load queue 경로를 통해 세그먼트 할당을 지시합니다. Historical은 큐에서 새 항목을 감지하면 다음 과정을 수행합니다.

1. ZooKeeper에서 세그먼트 메타데이터를 조회합니다.
2. 딥 스토리지에서 세그먼트를 내려받아 처리합니다.
3. ZooKeeper의 served segments 경로를 통해 해당 세그먼트를 서빙 중임을 알립니다.

### 메모리 맵 캐시

세그먼트 캐시는 메모리 매핑(memory mapping)을 사용하므로, 자주 접근하는 세그먼트 부분은 운영 체제가 메모리에 유지합니다. 쿼리 성능은 필요한 데이터가 메모리 맵 캐시에 있는지, 디스크 읽기가 필요한지에 따라 달라집니다. 시스템 메모리가 클수록 세그먼트가 메모리에 남을 확률이 높아져 쿼리 응답이 빨라집니다.

### 쿼리

Historical 서비스는 쿼리 로깅과 메트릭 리포팅을 지원해 성능 모니터링과 분석에 활용할 수 있습니다.

상세 설정은 [Historical configuration](https://druid.apache.org/docs/latest/configuration/#historical)을 참고하십시오.

---

## Middle Manager

Middle Manager는 제출된 태스크를 실행하는 워커(worker) 서비스입니다. 태스크를 직접 실행하지 않고, 별도 JVM에서 동작하는 Peon에게 전달합니다. 이 구조 덕분에 태스크마다 자원과 로그가 격리됩니다.

- Peon 하나는 한 번에 태스크 하나만 실행합니다.
- Middle Manager 하나는 여러 Peon을 관리할 수 있습니다.

### 실행

```
org.apache.druid.cli.Main server middleManager
```

상세 설정은 [Middle Manager and Peon configuration](https://druid.apache.org/docs/latest/configuration/#middle-manager-and-peon)을, HTTP 엔드포인트는 [Service status API reference](https://druid.apache.org/docs/latest/api-reference/service-status-api#middle-manager)를 참고하십시오.

---

## Peon

Peon은 Middle Manager가 생성하는 태스크 실행 엔진입니다. 각 Peon은 별도 JVM으로 실행되며 단일 태스크 하나의 실행을 담당합니다. Peon은 항상 자신을 생성한 Middle Manager와 같은 호스트에서 동작합니다.

### 실행

Peon은 보통 Middle Manager가 생성하므로 단독으로 실행하는 일은 드물지만, 필요하다면 다음 명령으로 실행할 수 있습니다.

```
org.apache.druid.cli.Main internal peon <task_file> <status_file>
```

- `task_file`: 태스크 JSON 객체가 담긴 파일 경로
- `status_file`: 태스크 상태를 기록할 파일 경로

프로덕션 환경에서는 Peon을 Middle Manager와 분리해 단독으로 운용하는 경우가 거의 없습니다.

상세 설정은 Peon Query Configuration과 Additional Peon Configuration 문서를 참고하십시오.

---

## Indexer

Indexer는 Middle Manager + Peon 조합을 대체하는 선택적(optional)·실험적(experimental) 서비스입니다. 태스크를 별도 프로세스가 아니라 단일 JVM 안의 스레드로 실행하므로, 설정과 배포가 더 간단하고 자원 공유 효율이 높습니다.

배치(batch) 인제스천에는 Middle Manager + Peon 시스템 또는 Kubernetes 기반 대안을 권장하며, Indexer는 설정을 단순화하고 싶은 스트리밍 인제스천 워크로드에 적합합니다.

### 실행

```
org.apache.druid.cli.Main server indexer
```

### 주요 설정

| 속성 | 설명 |
| --- | --- |
| `druid.worker.globalIngestionHeapLimitBytes` | 인제스천에 쓸 전역 힙 한도. 기본값은 JVM 힙의 1/6입니다. |
| `druid.worker.capacity` | 태스크 슬롯 수 |
| `druid.worker.numConcurrentMerges` | 동시 병합 수. 기본값은 capacity/2(내림)입니다. |
| `druid.server.http.numThreads` | chat handler용과 일반 요청용으로 같은 크기의 스레드 풀을 만듭니다. |

### 태스크 자원 공유

**메모리 관리**

전역 인제스천 힙 한도는 태스크 슬롯에 균등하게 분배됩니다. 태스크별 힙 한도가 태스크의 `maxBytesInMemory` 설정을 재정의(override)하며, 최대 메모리 사용량은 대략 `maxBytesInMemory * (2 + maxPendingPersists)`에 이릅니다.

**쿼리 자원**

처리 스레드, 버퍼, (선택 시) 캐시는 공유 엔드포인트를 통해 모든 태스크가 함께 사용합니다.

**HTTP 스레드**

같은 크기의 스레드 풀 두 개가 각각 태스크 제어 메시지와 일반 요청을 처리하고, lookup 처리용 스레드 두 개가 별도로 존재합니다.

### 현재 제한 사항

- 태스크별 로그를 따로 제공하지 않습니다. 모든 태스크 로그가 Indexer 서비스 로그에 함께 기록됩니다.
- 모든 태스크에 동일한 메모리 한도가 적용됩니다. 이 제약은 이후 릴리스에서 제거할 계획입니다.

상세 설정은 Indexer Configuration 문서를 참고하십시오.

---

## Router

Router는 여러 Broker 서비스에 쿼리를 분배하는 서비스로, 테라바이트급 이상의 클러스터에서 특히 유용합니다. 데이터 관리와 클러스터 모니터링을 위한 웹 콘솔도 Router가 호스팅합니다.

### 실행

```
org.apache.druid.cli.Main server router
```

### 주요 설정

| 속성 | 설명 |
| --- | --- |
| `druid.router.defaultBrokerServiceName` | 기본으로 사용할 Broker 서비스 |
| `druid.router.coordinatorServiceName` | Coordinator 서비스 이름 |
| `druid.router.tierToBrokerMap` | 티어(tier) 이름을 Broker 서비스에 매핑 |
| `druid.router.http.numConnections` | 커넥션 풀 크기 |
| `druid.router.http.readTimeout` | 요청 타임아웃 |
| `druid.router.http.numMaxThreads` | 프록시 스레드 수 |
| `druid.server.http.numThreads` | 서버 스레드 풀 크기 |

### 관리 프록시(Management Proxy)

`druid.router.managementProxy.enabled=true`로 활성화하면 Router가 Coordinator·Overlord API의 프록시 역할을 합니다.

| 경로 | 대상 |
| --- | --- |
| `/druid/coordinator/*` | Coordinator (암시적 라우팅) |
| `/druid/indexer/*` | Overlord (암시적 라우팅) |
| `/proxy/coordinator/*` | Coordinator (명시적, 접두사 제거 후 전달) |
| `/proxy/overlord/*` | Overlord (명시적, 접두사 제거 후 전달) |

### 라우팅 전략(Routing Strategies)

| 전략 | 설명 |
| --- | --- |
| **timeBoundary** | 모든 timeBoundary 쿼리를 우선순위가 가장 높은 Broker로 보냅니다. |
| **priority** | 쿼리의 priority 값을 기준으로 라우팅하며 `minPriority`, `maxPriority` 임계값을 설정할 수 있습니다. |
| **manual** | 쿼리 컨텍스트의 `brokerService` 값을 읽어 라우팅하고, 없으면 `defaultManualBrokerService`를 사용합니다. |
| **JavaScript** | 쿼리 명세를 처리하는 JavaScript 함수로 커스텀 라우팅 로직을 구현합니다. |

SQL 쿼리도 `druid.router.sql.enable=true`로 설정하면 기본 Broker 할당 대신 라우팅 전략을 적용합니다.

### Avatica JDBC 쿼리 밸런싱

Avatica JDBC 연결은 connection ID를 해싱해 같은 연결의 요청이 항상 같은 Broker로 가도록 보장합니다.

**Rendezvous hash (기본값)**

```
druid.router.avatica.balancer.type=rendezvousHash
```

**Consistent hash**

```
druid.router.avatica.balancer.type=consistentHash
```

### 프로덕션 설정 예시

```properties
druid.host=#{IP_ADDR}:8080
druid.plaintextPort=8080
druid.service=druid/router
druid.router.defaultBrokerServiceName=druid:broker-cold
druid.router.coordinatorServiceName=druid:coordinator
druid.router.tierToBrokerMap={"hot":"druid:broker-hot","_default_tier":"druid:broker-cold"}
druid.router.http.numConnections=50
druid.router.http.readTimeout=PT5M
druid.router.http.numMaxThreads=100
druid.server.http.numThreads=100
```

상세 설정은 [Router configuration](https://druid.apache.org/docs/latest/configuration/#router)을 참고하십시오.

---

## 참고 자료

- [Druid Architecture](https://druid.apache.org/docs/latest/design/architecture)
- [Coordinator service](https://druid.apache.org/docs/latest/design/coordinator)
- [Overlord service](https://druid.apache.org/docs/latest/design/overlord)
- [Broker service](https://druid.apache.org/docs/latest/design/broker)
- [Historical service](https://druid.apache.org/docs/latest/design/historical)
- [Middle Manager service](https://druid.apache.org/docs/latest/design/middlemanager)
- [Indexer service](https://druid.apache.org/docs/latest/design/indexer)
- [Peon service](https://druid.apache.org/docs/latest/design/peons)
- [Router service](https://druid.apache.org/docs/latest/design/router)
- [Configuration reference](https://druid.apache.org/docs/latest/configuration/)
- [Basic cluster tuning](https://druid.apache.org/docs/latest/operations/basic-cluster-tuning)

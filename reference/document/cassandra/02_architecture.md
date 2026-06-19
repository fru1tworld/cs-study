# Cassandra 아키텍처

> 이 문서는 Apache Cassandra 공식 문서의 "Architecture" 섹션을 한국어로 번역한 것입니다.
> 원본: https://cassandra.apache.org/doc/latest/cassandra/architecture/

---

## 목차

1. [개요](#개요)
2. [Dynamo 기반 아키텍처](#dynamo-기반-아키텍처)
   - [데이터셋 분할: 일관성 해싱](#데이터셋-분할-일관성-해싱)
   - [멀티 마스터 복제](#멀티-마스터-복제)
   - [튜닝 가능한 일관성 수준](#튜닝-가능한-일관성-수준)
   - [클러스터 멤버십과 장애 감지](#클러스터-멤버십과-장애-감지)
   - [점진적 확장](#점진적-확장)
3. [스토리지 엔진](#스토리지-엔진)
   - [커밋 로그](#커밋-로그)
   - [멤테이블](#멤테이블)
   - [SSTable](#sstable)
4. [보장 사항](#보장-사항)
5. [스니치](#스니치)
6. [참고 자료](#참고-자료)

---

## 개요

Apache Cassandra는 오픈 소스 분산 NoSQL 데이터베이스(distributed NoSQL database)입니다. 분할된 와이드 컬럼 스토리지 모델(partitioned wide-column storage model)을 구현하며 최종 일관성(eventually consistent) 시맨틱을 제공합니다.

Cassandra는 Facebook에서 시작되었으며, 두 가지 영향력 있는 시스템의 설계 원칙을 결합했습니다. 분산 스토리지와 복제(distributed storage and replication)는 Amazon의 Dynamo에서, 데이터 모델과 스토리지 엔진 아키텍처는 Google의 Bigtable에서 가져왔습니다. 시스템은 스테이지 기반 이벤트 구동 아키텍처(staged event-driven architecture, SEDA)를 채택합니다.

### 설계 목표

Cassandra는 다음과 같은 핵심 요구 사항을 충족하도록 설계되었습니다.

- 완전한 멀티 프라이머리 데이터베이스 복제(full multi-primary database replication)
- 낮은 지연 시간의 읽기/쓰기를 갖춘 글로벌 가용성(global availability)
- 범용 하드웨어(commodity hardware)에서의 수평 확장(horizontal scaling)
- 프로세서를 추가할 때마다 선형적으로 증가하는 처리량(linear throughput)
- 온라인 부하 분산(online load balancing) 및 클러스터 확장
- 파티션 키 기반 쿼리(partitioned key-oriented query) 지원
- 유연한 스키마 설계(flexible schema design)

### 주요 기능

**데이터 구성 (CQL을 통한):**

Cassandra는 다음 단위로 정보를 구성합니다.

- **키스페이스(keyspace)**: 복제 설정을 담는 컨테이너
- **테이블(table)**: 행과 컬럼 구조
- **파티션(partition)**: 노드 위치를 결정하는 기본 키(primary key)의 구성 요소
- **행(row)**: 고유한 기본 키로 식별되는 컬럼들의 집합
- **컬럼(column)**: 타입이 지정된 개별 데이터 요소

**고급 기능:**

컬렉션 타입(collection types), 사용자 정의 타입(user-defined types), 스토리지 연결 인덱싱(storage-attached indexing, SAI), 로컬 보조 인덱스(local secondary indexes), 원자적 시맨틱을 갖춘 경량 트랜잭션(lightweight transactions), 구체화된 뷰(materialized views)를 지원합니다.

**의도적인 제한:**

Cassandra는 높은 가용성과 성능을 유지하기 위해 교차 파티션 트랜잭션(cross-partition transactions), 분산 조인(distributed joins), 외래 키 제약 조건(foreign key constraints)을 의도적으로 제외합니다.

### 운영

설정은 `cassandra.yaml` 파일을 통해 이루어집니다. 관리 도구로는 런타임 클러스터 제어를 위한 `nodetool`과 함께 감사 로깅(audit logging), 쿼리 분석, 스냅샷(snapshots), 증분 백업(incremental backups)을 위한 유틸리티가 있습니다.

---

## Dynamo 기반 아키텍처

Apache Cassandra는 Amazon의 Dynamo 분산 스토리지 시스템에서 여러 핵심 기법을 채택했습니다. 각 노드는 세 가지 주요 구성 요소를 포함합니다. 요청 코디네이션(request coordination), 링 멤버십 및 장애 감지(ring membership/failure detection), 로컬 영속성(local persistence)입니다. Cassandra는 Dynamo의 클러스터링 메커니즘을 활용하면서 자체적인 LSM(Log Structured Merge) 트리 스토리지 엔진을 구현합니다.

시스템은 Dynamo에서 파생된 네 가지 핵심 기법에 의존합니다.

- 데이터셋 분할을 위한 일관성 해싱(consistent hashing)
- 버전이 매겨진 데이터와 튜닝 가능한 일관성을 갖춘 멀티 마스터 복제(multi-master replication)
- 가십 프로토콜(gossip protocol)을 통한 분산 클러스터 멤버십
- 표준 하드웨어에서의 점진적 확장(incremental scaling)

### 데이터셋 분할: 일관성 해싱

단순한 해시 기반 버킷팅(naive hash-based bucketing) 대신, Cassandra는 일관성 해싱(consistent hashing)을 사용합니다. 이 방식에서 "모든 노드는 연속적인 해시 링(continuous hash ring) 위의 하나 이상의 토큰(token)에 매핑됩니다." 노드가 추가되거나 제거될 때, 키 중 극히 일부만 재매핑하면 되므로 매끄러운 클러스터 확장이 가능합니다.

#### 토큰 링 아키텍처

"토큰 범위(token range)라고도 불리는 키 범위들은 동일한 물리적 노드 집합에 매핑됩니다." 복제 계수(replication factor) 3을 가진 8개 노드 클러스터에서, 각 토큰 범위는 서로 다른 3개의 노드에 복제되어 단일 장애 지점(single point of failure)을 방지합니다.

#### 가상 노드 (vnodes)

클러스터 균형을 개선하기 위해 Cassandra는 물리적 노드당 여러 개의 토큰을 할당합니다. 이 접근 방식은 세 가지 이점을 제공합니다.

1. 새 노드가 거의 동일한 양의 데이터 분포를 받습니다.
2. 제거(decommission)된 노드가 복제본 전반에 걸쳐 데이터를 고르게 잃습니다.
3. 사용 불가능한 노드가 쿼리 부하를 고르게 분산합니다.

그러나 여러 토큰을 사용하는 것은 트레이드오프를 동반합니다. 링 위에서 이웃 관계(neighbor relationship)가 늘어나면, 여러 노드가 동시에 장애를 일으킬 때 중단(outage) 확률이 높아지고, 클러스터 유지 보수 작업이 그에 비례하여 느려집니다.

### 멀티 마스터 복제

#### 복제 계수와 전략

"모든 데이터 파티션은 높은 가용성과 내구성(durability)을 유지하기 위해 클러스터 전반의 여러 노드에 복제됩니다." 복제 계수(replication factor)는 존재하는 복사본의 수를 결정합니다. Cassandra는 플러그 가능한(pluggable) 복제 전략을 지원합니다.

- **NetworkTopologyStrategy** (운영 환경 권장): 데이터센터별로 복제 계수를 지정하며, 각 데이터센터 내에서 서로 다른 랙(rack)에 복제본을 분산하려고 시도합니다.
- **SimpleStrategy** (테스트 전용): 모든 노드를 동일하게 취급하며, 데이터센터와 랙 구성을 무시합니다.

#### 데이터 버전 관리

Cassandra는 "최종 쓰기 우선(last-write-wins)" 충돌 해결 방식을 구현합니다. "시스템에 들어오는 모든 변경(mutation)은 타임스탬프(timestamp)와 함께 들어옵니다." 이 접근 방식은 Dynamo의 벡터 클록(vector clock)보다 단순하며, NTP를 통한 적절한 시간 동기화에 의존합니다.

#### 복제본 동기화

세 가지 메커니즘이 복제본의 수렴(convergence)을 이끕니다.

1. **읽기 복구(Read Repair)**: 읽기 시점의 최선 노력(best-effort) 동기화
2. **힌티드 핸드오프(Hinted Handoff)**: 쓰기 시점의 최선 노력 전달
3. **안티 엔트로피 복구(Anti-Entropy Repair)**: 머클 트리(Merkle tree)를 사용하여 복제본 간 불일치 데이터를 식별하고 해결

Cassandra는 Dynamo의 복구 기능을 서브 레인지(sub-range) 복구와 증분(incremental) 복구 옵션으로 확장합니다.

### 튜닝 가능한 일관성 수준

Cassandra는 "일관성 수준(Consistency Level)을 통해 작업별로 일관성과 가용성 사이의 트레이드오프"를 제공합니다. 이는 Dynamo의 `R + W > N` 원칙을 구현한 것입니다.

| 수준(Level) | 동작 |
|-------------|------|
| `ONE`, `TWO`, `THREE` | 지정된 수의 복제본이 응답 |
| `QUORUM` | 복제본의 과반수(`n/2 + 1`)가 응답 |
| `ALL` | 모든 복제본이 응답 |
| `LOCAL_QUORUM` | 로컬 데이터센터 내 과반수가 응답 |
| `EACH_QUORUM` | 각 데이터센터의 과반수가 응답 |
| `LOCAL_ONE` | 로컬 복제본 하나가 응답 |
| `ANY` | 복제본 하나 또는 코디네이터 힌트(쓰기 전용) |

"쓰기 작업은 일관성 수준과 관계없이 항상 모든 복제본으로 전송됩니다." 일관성 수준은 단지 코디네이터(coordinator)가 몇 개의 응답을 기다릴지를 제어할 뿐입니다.

### 클러스터 멤버십과 장애 감지

#### 가십 프로토콜

"가십(Gossip)은 Cassandra가 엔드포인트 멤버십(endpoint membership)과 노드 간 네트워크 프로토콜 버전 같은 기본적인 클러스터 부트스트래핑 정보를 전파하는 방식입니다." 모든 노드는 매초 독립적으로 가십 작업을 실행합니다.

1. 로컬 하트비트(heartbeat) 상태를 갱신합니다.
2. 클러스터 내 무작위 노드와 상태를 교환합니다.
3. 확률적으로 도달 불가능한(unreachable) 노드와 가십을 시도합니다.
4. 필요하면 시드 노드(seed node)와 가십합니다.

"시드 노드는 다른 시드 노드를 보지 않고도 링에 부트스트랩(bootstrap)하는 것이 허용되며", 가십 핫스팟(hotspot) 역할을 합니다. 여러 개의 시드 노드(이상적으로는 랙/데이터센터당 하나)는 클러스터 탐색(discovery)을 용이하게 합니다.

#### 장애 감지

"Cassandra의 모든 노드는 Phi Accrual 장애 감지기(Phi Accrual Failure Detector)의 변형을 실행하며", 하트비트 상태를 기반으로 피어(peer)의 가용성을 독립적으로 판단합니다. "특정 시간 동안 어떤 노드로부터 증가하는 하트비트를 보지 못하면, 장애 감지기는 그 노드를 유죄(convict)로 판정하고", 읽기 대신 쓰기를 힌트(hint)로 라우팅합니다.

결정적으로, "Cassandra는 운영자의 명시적인 지시 없이는 가십 상태에서 노드를 절대 제거하지 않으며", 이는 일시적 장애 동안 불필요한 데이터 재분배(rebalancing)를 방지합니다.

### 점진적 확장

Cassandra는 "범용 하드웨어(commodity hardware)"를 위해 설계되었으며, "노드는 언제든지 장애를 일으킬 수 있다"는 가정을 전제로 합니다. 시스템은 리소스 사용을 자동으로 튜닝하며, 압축(compression)과 캐싱(caching)을 활용하여 스토리지 효율을 극대화합니다.

#### 단순한 쿼리 모델

SQL 시스템과 달리, "Cassandra는 교차 파티션 트랜잭션(cross-partition transactions)을 제공하지 않기로 선택했습니다." 대신 "단일 파티션 작업에 대해 어떤 규모에서든 빠르고 일관된 지연 시간"을 제공합니다. 시스템은 경량 트랜잭션(lightweight transactions)을 통한 단일 파티션 비교 후 교환(compare-and-swap)을 지원합니다.

#### 스토리지 인터페이스

Cassandra는 "와이드 컬럼 스토어(wide-column store) 인터페이스"를 제공하며, "파티션은 여러 행을 포함하고, 각 행은 개별적으로 타입이 지정된 컬럼의 유연한 집합을 가집니다." 모든 행은 파티션 키(partition key)와 클러스터링 키(clustering key)로 고유하게 식별됩니다. 이 설계는 동시적인 메타데이터 전용 스키마 변경(metadata-only schema changes)을 통해 "사용자가 기존 데이터셋에 새 컬럼을 유연하게 추가할 수 있도록" 합니다.

---

## 스토리지 엔진

Cassandra 스토리지 엔진은 "전통적인 관계형 데이터베이스의 B-트리(B-tree) 설계 대신 추가 전용(append-only) 접근 방식을 활용하는 LSM(Log Structured Merge) 트리"를 사용합니다. 이는 쓰기 성능을 최적화하면서 블룸 필터(Bloom filter)를 통해 읽기/쓰기 트레이드오프를 관리합니다.

**쓰기 경로(Write Path) 순서:**

1. 커밋 로그(commit log)에 기록
2. 멤테이블(memtable)에 쓰기
3. 멤테이블에서 플러시(flush)
4. 디스크의 SSTable에 저장

### 커밋 로그

커밋 로그(Commit Log)는 선행 기록 로깅(Write-Ahead Logging, WAL)을 담당합니다. 시스템은 모든 쓰기를 "디스크의 로컬 추가 전용 커밋 로그"에 기록하여 크래시 복구(crash recovery)를 보장합니다. 주요 설정은 다음과 같습니다.

- **`commitlog_segment_size`**: 기본값 32MiB. 새 세그먼트를 생성하기 전 세그먼트 크기를 제한합니다.
- **`commitlog_sync`**: 내구성 보장을 위한 모드로 `periodic`(기본값, 10000ms) 또는 `batch` 중 선택합니다.
- **`commitlog_directory`**: 저장 위치. 자기 디스크(magnetic drive)의 경우 별도의 스핀들(spindle)을 권장합니다.
- **`commitlog_compression`**: LZ4, Snappy, Deflate, Zstd 압축을 지원합니다.
- **`commitlog_total_space`**: 기본값 8192MiB. 초과 시 플러시를 트리거합니다.

### 멤테이블

멤테이블(Memtable)은 인메모리 라이트백 캐시(write-back cache)로, 설정된 한계에 도달할 때까지 데이터를 정렬된 순서로 저장합니다. "일반적으로 테이블당 하나의 활성 멤테이블이 있으며, 데이터 파티션의 캐시 역할을 합니다." 플러시(flush)는 메모리 임계값 또는 커밋 로그 압박(pressure)에 의해 트리거됩니다. 데이터는 `nodetool flush` 또는 자동 메커니즘을 통해 영속화됩니다.

### SSTable

SSTable은 불변(immutable) 디스크 데이터 파일이며, 여러 구성 요소(component)를 포함합니다.

- **`Data.db`**: 실제 행(row) 내용
- **`Partitions.db`**: 파티션 키를 파일 위치에 매핑
- **`Rows.db`**: 다중 행 파티션을 위한 행 인덱스
- **`Index.db`**: 파티션 키 위치
- **`Summary.db`**: Index 항목의 샘플링
- **`Filter.db`**: 파티션 키의 블룸 필터(Bloom filter)
- **`Statistics.db`**: 타임스탬프, 툼스톤(tombstone), TTL 등의 메타데이터
- **`Digest.crc32`**: 체크섬 검증
- **`TOC.txt`**: 구성 요소 파일 목록

파일 내에서 "행은 파티션별로 구성되며 토큰 순서(token order)로 정렬되고", "행은 클러스터링 키(clustering key) 순서로 저장됩니다."

#### SSTable 버전

공식 문서는 0.7.0부터 5.0까지의 버전 진행 과정을 상세히 다루며, BigFormat 버전과 함께 Cassandra 5.0에서 도입된 새로운 BTI(Trie-indexed) 포맷을 포함합니다. BTI 포맷은 `sstable.selected_format: bti` 설정으로 구성할 수 있습니다.

---

## 보장 사항

Cassandra는 페타바이트(petabyte) 규모의 데이터셋을 처리하는 웹 스케일 애플리케이션을 위해 설계된, 고도로 확장 가능하고 신뢰성 있는 데이터베이스입니다. 시스템은 CAP 정리(CAP theorem) 원칙에 기반하여 확장성, 가용성, 신뢰성에 대한 구체적인 보장을 제공합니다.

### CAP 정리 기반

CAP 정리에 따르면, 분산 데이터 스토어는 다음 세 가지 속성을 동시에 보장할 수 없습니다.

- **일관성(Consistency)**: "모든 읽기는 가장 최근의 쓰기를 받거나 오류를 반환한다"
- **가용성(Availability)**: "모든 요청이 응답을 받는다"
- **분할 내성(Partition tolerance)**: 네트워크 분할(network partition) 장애를 견디면서 계속 동작한다

Cassandra는 고가용성 웹 애플리케이션을 지원하기 위해 엄격한 일관성보다 가용성과 분할 내성을 우선시합니다.

### 핵심 보장

Cassandra는 다음을 보장합니다.

**확장성과 가용성**

- 가십 기반 프로토콜을 통한 노드 추가/제거로 높은 확장성을 제공합니다.
- 가십 기반 장애 감지를 갖춘 내결함성(fault-tolerant) 아키텍처를 통해 높은 가용성을 제공합니다.

**데이터 내구성**

- 서로 다른 노드와 데이터센터에 데이터를 복제하여 내구성을 보장합니다.
- 복제본이 존재하는 한, 복구 불가능한 장애가 발생해도 전체 데이터 손실로 이어지지 않습니다.

**일관성 모델**

- "최종 일관성(eventually consistent) 스토리지 시스템"으로, 업데이트는 결국 모든 복제본에 도달합니다.
- 일시적으로 분기된(divergent) 데이터 버전은 일관된 상태로 조정(reconcile)됩니다.
- Paxos 합의 프로토콜(consensus protocol)을 사용하는 경량 트랜잭션(lightweight transactions)은 선형화 가능한 일관성(linearizable consistency)을 제공합니다.
- 비교 후 설정(compare-and-set, CAS) 트랜잭션은 격리(isolation)를 보장합니다.

**다중 테이블 작업**

- 여러 테이블에 걸친 배치 쓰기(batched writes)는 전부 성공하거나 전혀 적용되지 않습니다.
- 배치로그 복제(batchlog replication)는 코디네이터 노드 장애에도 작업 완료를 보장합니다.

**인덱싱**

- 보조 인덱스(secondary index)는 "로컬 복제본과 일관성이 유지됨"이 보장됩니다.

---

## 스니치

Cassandra에서 스니치(snitch)는 두 가지 핵심 기능을 수행합니다. "요청을 효율적으로 라우팅하기 위해 Cassandra에게 네트워크 토폴로지(network topology)에 대해 충분히 알려주는" 것과, 노드를 데이터센터와 랙으로 조직하여 "상관 장애(correlated failure)를 피하도록 클러스터 전반에 복제본을 분산"하는 것입니다.

`cassandra.yaml`의 `endpoint_snitch` 파라미터는 `IEndpointSnitch`를 구현하는 클래스로 설정되어야 하며, 이 클래스는 동적 스니치(dynamic snitch)에 의해 래핑되어 두 엔드포인트가 같은 데이터센터에 있는지 또는 같은 랙에 있는지를 결정합니다.

### 동적 스니치

동적 스니치(Dynamic Snitching)는 읽기 지연 시간(read latency)을 모니터링하여 느린 호스트로 요청이 라우팅되는 것을 방지합니다. 동적 스니치는 이미 구성된 하위 스니치(underlying snitch)로부터 기본적인 클러스터 토폴로지 정보를 얻은 다음, 노드의 읽기 지연 시간을 모니터링하고 컴팩션(compaction) 작업까지 추적합니다. 설정은 `cassandra.yaml`에서 다음 속성으로 이루어집니다.

- **`dynamic_snitch`**: 동적 스니치 기능을 활성화/비활성화합니다.
- **`dynamic_snitch_update_interval`**: 기본값 100ms. 비용이 큰 호스트 점수(host score) 계산의 빈도를 제어합니다.
- **`dynamic_snitch_reset_interval`**: 기본값 10m. 양수일 때 캐시 용량을 높이기 위한 복제본 고정(replica pinning)을 허용합니다.
- **`dynamic_snitch_badness_threshold`**: 느린 고정 호스트가 선호도를 잃는 시점을 결정하는 백분율 임계값입니다. 0.2는 성능이 20% 저하될 때까지 Cassandra가 복제본을 전환하지 않고 기다린다는 의미입니다.

### 스니치 유형

**GossipingPropertyFileSnitch**: 운영 환경에서 권장되는 선택입니다. 로컬 랙과 데이터센터 설정을 `cassandra-rackdc.properties` 파일에 정의하고 가십 프로토콜(gossip protocol)을 통해 다른 노드에 전파합니다. 레거시 마이그레이션을 위해 `cassandra-topology.properties` 파일이 존재하면 이를 폴백(fallback)으로 사용합니다. `PropertyFileSnitch`처럼 랙과 데이터센터를 수동으로 동기화할 필요 없이, 각 노드가 자신의 랙과 데이터센터 이름을 개별적으로 정의합니다.

**SimpleSnitch**: 복제 전략(replication strategy)의 순서를 근접성(proximity) 지표로 취급합니다. 읽기 복구(read repair)가 비활성화된 상태에서 캐시 지역성(cache locality)을 높이려는 단일 데이터센터 배포에만 적합합니다.

**PropertyFileSnitch**: `cassandra-topology.properties` 내의 명시적인 랙과 데이터센터 구성에 의존하여 근접성을 결정합니다.

**Ec2Snitch**: 단일 리전(region) EC2 배포(또는 2017년 이후 인터 리전 VPC가 활성화된 다중 리전)를 위해 설계되었습니다. EC2 API에서 리전(Region)과 가용 영역(Availability Zone) 데이터를 자동으로 가져와, 리전을 데이터센터에, 가용 영역을 랙에 매핑합니다.

**Ec2MultiRegionSnitch**: 공인 IP(public IP)를 브로드캐스트 주소(broadcast address)로 사용하여 리전 간 연결을 지원합니다. 공용 방화벽에서 `storage_port` 또는 `ssl_storage_port`를 열어야 하며, Cassandra는 리전 내 통신에서는 사설 IP(private IP)로 전환합니다.

**RackInferringSnitch**: IP 옥텟(octet)에서 랙/데이터센터를 추론합니다(각각 3번째와 2번째 옥텟). 주로 커스텀 스니치 개발을 위한 참조 구현(reference implementation) 역할을 합니다.

**GoogleCloudSnitch / CloudstackSnitch**: 각각 Google Cloud Platform 및 Apache CloudStack 환경을 위한 스니치입니다.

---

## 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [Architecture - Overview](https://cassandra.apache.org/doc/latest/cassandra/architecture/overview.html)
- [Architecture - Dynamo](https://cassandra.apache.org/doc/latest/cassandra/architecture/dynamo.html)
- [Architecture - Storage Engine](https://cassandra.apache.org/doc/latest/cassandra/architecture/storage-engine.html)
- [Architecture - Guarantees](https://cassandra.apache.org/doc/latest/cassandra/architecture/guarantees.html)
- [Architecture - Snitch](https://cassandra.apache.org/doc/4.1/cassandra/architecture/snitch.html)

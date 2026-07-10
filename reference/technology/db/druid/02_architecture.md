# Druid 아키텍처

> 원본: https://druid.apache.org/docs/latest/design/architecture
> 원본: https://druid.apache.org/docs/latest/design/storage
> 원본: https://druid.apache.org/docs/latest/design/segments
> 원본: https://druid.apache.org/docs/latest/design/deep-storage
> 원본: https://druid.apache.org/docs/latest/design/metadata-storage
> 원본: https://druid.apache.org/docs/latest/design/zookeeper

Druid의 서비스 구성과 서버 유형, 스토리지 설계, 세그먼트(segment) 파일 구조, 그리고 외부 의존성인 딥 스토리지(deep storage)·메타데이터 스토리지·ZooKeeper를 차례로 설명합니다.

---

## 목차

1. [전체 아키텍처](#전체-아키텍처)
2. [스토리지 설계](#스토리지-설계)
3. [세그먼트](#세그먼트)
4. [딥 스토리지](#딥-스토리지)
5. [메타데이터 스토리지](#메타데이터-스토리지)
6. [ZooKeeper](#zookeeper)
7. [참고 자료](#참고-자료)

---

## 전체 아키텍처

Apache Druid는 클라우드 친화적이고 운영하기 쉬운 분산 아키텍처를 채택했습니다. 각 서비스를 독립적으로 확장할 수 있으며, 한 구성 요소에 장애가 발생해도 다른 구성 요소로 곧바로 번지지 않는 내결함성(fault tolerance)을 제공합니다.

### Druid 서비스

Druid에는 다음과 같은 서비스 유형이 있습니다.

| 서비스 | 역할 |
| --- | --- |
| **Coordinator** | 클러스터의 데이터 가용성을 관리합니다. |
| **Overlord** | 데이터 인제스천(ingestion) 워크로드의 할당을 제어합니다. |
| **Broker** | 외부 클라이언트의 쿼리를 처리합니다. |
| **Router** | 요청을 Broker, Coordinator, Overlord로 라우팅합니다. |
| **Historical** | 쿼리 가능한 데이터를 저장하고 서빙합니다. |
| **Middle Manager & Peon** | 데이터 인제스천을 담당합니다. Peon은 개별 인제스천 태스크(task)를 별도 JVM에서 실행합니다. |
| **Indexer** | Middle Manager + Peon의 대안으로, 태스크를 단일 JVM 안의 스레드로 실행합니다. (experimental) |

### 서버 유형

배포 시에는 보통 서비스를 세 가지 서버 유형으로 묶어 구성합니다.

- **Master 서버**: Coordinator와 Overlord 서비스를 실행하며, 인제스천과 데이터 가용성을 관리합니다.
- **Query 서버**: Broker와 Router 서비스를 실행하며, 웹 콘솔 UI를 포함해 사용자 대면 엔드포인트를 제공합니다.
- **Data 서버**: Historical과 Middle Manager 서비스를 실행하며, 인제스천을 수행하고 데이터를 저장합니다.

### 서비스 배치(colocation) 고려 사항

- 세그먼트 수가 많은 클러스터에서는 Coordinator와 Overlord를 분리 배치해 리소스 경합을 줄이는 것이 유리할 수 있습니다.
- 인제스천 부하나 쿼리 부하가 큰 경우 Historical과 Middle Manager를 분리 배치해 CPU·메모리 충돌을 방지할 수 있습니다.

### 외부 의존성

Druid 클러스터는 세 가지 외부 시스템에 의존합니다.

| 의존성 | 설명 |
| --- | --- |
| **딥 스토리지(Deep storage)** | S3, HDFS, 네트워크 파일시스템 같은 공유 파일 스토리지입니다. 인제스천된 모든 데이터를 보관하며 서비스 간 세그먼트 전달을 담당합니다. |
| **메타데이터 스토리지(Metadata storage)** | PostgreSQL, MySQL 같은 전통적인 RDBMS로, 세그먼트와 태스크의 메타데이터를 저장합니다. |
| **ZooKeeper** | 서비스 디스커버리, 조율(coordination), 리더 선출(leader election)을 담당합니다. |

---

## 스토리지 설계

### 데이터소스와 세그먼트

Druid는 데이터를 **데이터소스(datasource)** 단위로 저장하며, 이는 관계형 데이터베이스의 테이블에 해당합니다. 각 데이터소스는 시간 기준으로 파티셔닝(partitioning)되고(예: 일 단위 청크), 필요하면 다른 속성으로 추가 파티셔닝할 수 있습니다. 각 시간 청크(time chunk) 안에서 데이터는 하나 이상의 **세그먼트**로 나뉩니다. 세그먼트는 보통 수백만 행을 담는 단일 파일이며, 데이터소스에 따라 세그먼트가 몇 개에서 수백만 개까지 존재할 수 있습니다.

각 세그먼트는 Middle Manager에서 가변(mutable)·미커밋(uncommitted) 상태로 생성됩니다. 세그먼트 빌드 과정에서 다음과 같은 방식으로 데이터를 쿼리에 최적화된 형태로 변환합니다.

- 컬럼 지향(columnar) 포맷으로 변환
- 빠른 필터링을 위한 비트맵(bitmap) 인덱스 생성
- 다양한 압축 적용
  - 문자열 컬럼의 사전 인코딩(dictionary encoding)과 ID 저장 공간 최소화
  - 비트맵 인덱스의 비트맵 압축
  - 모든 컬럼에 대한 타입 인지(type-aware) 압축

세그먼트는 커밋되면 딥 스토리지로 옮겨지고 이후 **불변(immutable)** 이 됩니다. 이때 메타데이터 스토리지에 세그먼트의 스키마, 크기, 딥 스토리지 내 위치를 기술하는 메타데이터 레코드를 기록합니다. 이 레코드는 Coordinator가 클러스터에서 어떤 데이터를 사용할 수 있는지 파악하는 근거가 됩니다.

### 인덱싱과 핸드오프(handoff)

**인덱싱 측 절차:**

1. 인덱싱 태스크가 시작되면 Overlord를 통해 세그먼트 식별자를 할당받습니다.
2. 실시간(realtime) 태스크(Kafka, Kinesis)는 세그먼트를 만들자마자 곧바로 쿼리 대상으로 서빙합니다.
3. 태스크가 끝나면 세그먼트를 딥 스토리지로 푸시하고 메타데이터를 발행(publish)합니다.
4. 실시간 태스크는 Historical 서비스가 세그먼트를 로드할 때까지 기다렸다가 종료합니다.

**Coordinator/Historical 측 절차:**

1. Coordinator가 메타데이터 스토리지를 주기적으로(기본 60초) 폴링해 새로 발행된 세그먼트를 찾습니다.
2. 발행되었지만 아직 사용 불가한 세그먼트를 발견하면 Historical 하나를 골라 로드를 지시합니다.
3. Historical이 세그먼트를 로드해 서빙을 시작합니다.
4. 세그먼트 핸드오프를 기다리던 인덱싱 태스크가 있으면 이 시점에 종료합니다.

### 세그먼트 식별자

세그먼트 식별자는 네 부분으로 구성됩니다.

- **데이터소스 이름**
- **시간 간격(interval)**: 인제스천 시 `segmentGranularity`로 지정한 시간 청크에 대응합니다.
- **버전 번호**: 보통 세그먼트 집합이 처음 생성된 시점의 ISO8601 타임스탬프입니다.
- **파티션 번호**: 정수이며, 데이터소스 + 간격 + 버전 조합 안에서 유일합니다.

예시:

```
clarity-cloud0_2018-05-21T16:00:00.000Z_2018-05-21T17:00:00.000Z_2018-05-21T15:56:09.909Z_1
```

파티션 번호가 0인 세그먼트는 식별자에서 파티션 번호를 생략합니다.

### 세그먼트 버전과 MVCC

버전 번호는 배치(batch) 덮어쓰기를 지원하는 **다중 버전 동시성 제어(MVCC, multi-version concurrency control)** 를 구현합니다. 특정 시간 간격의 데이터를 덮어쓰면 같은 데이터소스·같은 시간 간격에 더 높은 버전 번호를 가진 새 세그먼트 집합이 생성됩니다.

새 데이터는 클러스터에 로드되는 동안 쿼리에 보이지 않다가, 로드가 끝나면 쿼리가 즉시(instantaneously) 새 세그먼트로 전환되고 이후 이전 버전은 클러스터에서 제거됩니다. 사용자 입장에서는 일관성이 깨지는 순간 없이 매끄럽게 데이터가 교체됩니다.

### 세그먼트 생명주기

세그먼트의 생명주기는 세 영역에 걸쳐 있습니다.

1. **메타데이터 스토리지**: 세그먼트가 만들어지면 메타데이터(보통 수 KB 크기의 JSON payload)를 메타데이터 스토리지에 저장합니다. 이 발행(publishing) 시점에 boolean `used` 플래그로 쿼리 가능 여부를 제어합니다.
2. **딥 스토리지**: 메타데이터를 발행하기 직전에 세그먼트 데이터 파일을 딥 스토리지로 푸시합니다.
3. **쿼리 가용성**: 세그먼트는 실시간 태스크, 딥 스토리지 직접 쿼리, 또는 Historical 서비스에서 쿼리할 수 있습니다.

`sys.segments` SQL 테이블에서 세그먼트 상태를 나타내는 플래그를 확인할 수 있습니다.

| 플래그 | 의미 |
| --- | --- |
| `is_published` | 메타데이터가 발행되었고 `used`가 true입니다. |
| `is_available` | 실시간 태스크나 Historical에서 현재 쿼리 가능합니다. |
| `is_realtime` | 실시간 태스크에서만 사용 가능합니다. |
| `is_overshadowed` | 발행되었지만 다른 세그먼트에 완전히 가려진(overshadowed) 상태입니다. |

### 가용성과 일관성

**인제스천 일관성**: 주요 인제스천 방식(Kafka, Kinesis 같은 supervised 스트리밍, SQL `REPLACE`, 네이티브 배치)은 트랜잭션 기반의 전부-아니면-전무(all-or-nothing) 발행을 제공합니다. 인제스천이 실패하면 발행되지 않은 데이터는 롤백됩니다.

**멱등성(idempotency)**: 일부 인제스천 방식은 같은 작업을 반복 실행해도 데이터가 중복되지 않도록 보장합니다.

- supervised 스트리밍 방식은 스트림 오프셋(offset)을 세그먼트 메타데이터와 함께 원자적으로 저장합니다.
- SQL `REPLACE`는 멱등합니다. (자기 자신을 참조하는 연산은 예외)
- 네이티브 배치 인제스천은 append 모드를 쓰거나 자기 참조 소스를 읽는 경우가 아니면 멱등합니다.

**쿼리 일관성**: Broker는 쿼리 시작 시점에 어떤 세그먼트 버전을 사용할지 결정하므로, 하나의 쿼리에는 일관된 세그먼트 집합이 참여합니다.

**원자적 교체(atomic replacement)**: 시간 청크 단위로 이전 버전에서 새 버전으로의 전환이 순간적으로 일어나며 성능에 영향을 주지 않습니다. 이는 **core set** 개념에 기반합니다. 더 높은 버전의 새 세그먼트들이 모두 사용 가능해진 뒤에야 Broker가 전환하며, 시간 청크당 버전별로 core set은 하나만 존재합니다.

**deprecated 잠금 모드**: `forceTimeChunkLock`을 false로 설정하면 기존 버전을 그대로 사용하는 원자적 업데이트 그룹(atomic update group) 방식이 활성화됩니다. 같은 버전의 원자적 그룹을 여러 개 둘 수 있어 시간 청크 일부만 교체하거나 교체와 추가(append)를 동시에 수행할 수 있는 더 유연한 방식이지만, deprecated 상태입니다.

**장애 조치(failover)**: 복제본을 서빙하던 Historical 여러 대가 동시에 내려가 세그먼트가 사용 불가 상태가 되더라도 쿼리는 남은 세그먼트로 계속 수행됩니다. Druid는 백그라운드에서 사용 불가한 세그먼트를 다른 Historical에 자동으로 다시 로드합니다.

---

## 세그먼트

### 세그먼트 파일 개요

Druid는 데이터를 시간 간격 기준으로 파티셔닝한 세그먼트 파일에 저장합니다. 데이터가 있는 간격마다 세그먼트가 만들어지며, 데이터가 없는 간격에는 세그먼트가 생성되지 않습니다. 여러 인제스천 작업이 같은 간격에 데이터를 넣으면 해당 간격에 세그먼트가 여러 개 생길 수 있고, 컴팩션(compaction)이 이를 간격당 하나의 세그먼트로 병합해 성능을 최적화합니다.

- 시간 간격은 `granularitySpec`의 `segmentGranularity` 파라미터로 제어합니다.
- 무거운 쿼리 부하에서 잘 동작하려면 세그먼트 파일 크기를 **300~700MB** 범위로 유지하는 것이 좋습니다.
- 이 범위를 벗어나면 시간 간격의 세밀도(granularity)를 조정하거나, 파티셔닝 방식을 바꾸거나, `targetRowsPerSegment`(권장 시작값: 500만 행)를 조정합니다.

### 컬럼 지향 파일 구조

세그먼트는 컬럼 지향 구조로, 컬럼별 데이터를 별도 자료 구조에 저장합니다. 쿼리에 실제로 필요한 컬럼만 스캔하므로 쿼리 지연 시간이 줄어듭니다. 컬럼은 크게 세 가지 유형입니다.

**Timestamp 컬럼과 메트릭 컬럼**

LZ4로 압축된 정수 또는 부동소수점 배열로 저장합니다. 쿼리 시 필요한 행을 압축 해제해 값을 꺼낸 뒤 집계 연산을 적용하며, 쿼리에 쓰이지 않는 컬럼은 아예 건너뜁니다.

**디멘션 컬럼**

필터와 group-by 연산을 지원해야 하므로 세 가지 자료 구조를 사용합니다.

1. **사전(dictionary)**: 값(항상 문자열로 취급)을 정수 ID에 매핑해, 리스트와 비트맵을 압축된 형태로 표현할 수 있게 합니다.
2. **리스트(list)**: 사전으로 인코딩된 컬럼 값의 목록입니다. GroupBy·TopN 쿼리에 필요하며, 집계만 수행하는 쿼리라면 이 구조 없이도 동작할 수 있습니다.
3. **비트맵(bitmap)**: 컬럼의 고유 값마다 비트맵 하나를 만들어 어떤 행이 그 값을 담고 있는지 표시합니다. AND·OR 연산을 빠르게 적용할 수 있어 필터링에 유리합니다.

"Page" 컬럼에 "Justin Bieber", "Ke$ha" 두 값이 있는 예시입니다.

```
1: 사전
   {
    "Justin Bieber": 0,
    "Ke$ha":         1
   }

2: 컬럼 데이터(리스트)
   [0, 0, 1, 1]

3: 비트맵 — 컬럼의 고유 값마다 하나
   value="Justin Bieber": [1, 1, 0, 0]
   value="Ke$ha":         [0, 0, 1, 1]
```

비트맵의 크기는 데이터 크기 × 컬럼 카디널리티(cardinality)입니다. 카디널리티가 높은 컬럼일수록 비트맵이 극도로 희소(sparse)해져 압축률이 매우 높아집니다. Druid는 이런 특성에 맞는 비트맵 압축 알고리즘(예: Roaring bitmap compression)을 사용합니다.

### 다중 값(multi-value) 컬럼

한 행이 한 컬럼에 여러 문자열 값(개념상 배열)을 가질 수 있습니다. 행이 여러 값을 가지면 다음과 같이 저장이 달라집니다.

- 리스트 항목이 사전 ID의 배열이 됩니다.
- 해당 행이 값 개수만큼 여러 비트맵에서 0이 아닌 항목을 갖게 됩니다.

위 예시에서 두 번째 행이 두 값을 모두 가진다면 다음과 같습니다.

```
2: 컬럼 데이터(리스트)
   [0, [0, 1], 1, 1]

3: 비트맵
   value="Justin Bieber": [1, 1, 0, 0]
   value="Ke$ha":         [0, 1, 1, 1]
```

### null 값 처리

- 문자열 컬럼은 null을 사전의 ID 0으로 저장하고, 필터링에 쓸 수 있도록 null 값에 대한 비트맵 항목도 함께 저장합니다.
- 숫자 컬럼은 null 여부를 검사하는 집계·필터 연산을 위해 별도의 null 값 비트맵 인덱스를 유지합니다.
- 기본 모드에서는 세그먼트에 없는 디멘션이 빈 값(blank)처럼 동작하고, SQL 호환 모드에서는 null처럼 동작합니다. 마찬가지로 일부 세그먼트에 없는 메트릭은 집계 시 존재하지 않는 것처럼 처리됩니다.

### 세그먼트 이름과 샤딩(sharding)

세그먼트 식별자는 다음 형식을 따릅니다.

```
datasource_intervalStart_intervalEnd_version_partitionNum
```

같은 간격에 파티션이 여러 개인 예시입니다.

```
foo_2015-01-01/2015-01-02_v1_0
foo_2015-01-01/2015-01-02_v1_1
foo_2015-01-01/2015-01-02_v1_2
```

새 스키마로 재색인하면 새 버전 ID가 부여됩니다.

```
foo_2015-01-01/2015-01-02_v2_0
foo_2015-01-01/2015-01-02_v2_1
foo_2015-01-01/2015-01-02_v2_2
```

하나의 간격에 존재하는 여러 세그먼트는 **블록(block)** 을 이룹니다. `shardSpec` 유형에 따라 다르지만, 대부분의 shard spec에서는 블록이 완성되어야(간격의 모든 세그먼트가 로드되어야) 해당 간격에 대한 쿼리가 완료됩니다. 예외는 linear shard spec으로, 샤드가 전부 로드되지 않아도 쿼리가 완료될 수 있습니다. 예컨대 linear shard spec을 쓰는 실시간 인제스천에서는 세그먼트가 일부만 로드된 상태에서도 쿼리가 정상 수행됩니다.

### 세그먼트 구성 파일

세그먼트는 다음 파일들로 구성됩니다.

| 파일 | 내용 |
| --- | --- |
| `version.bin` | 세그먼트 포맷 버전을 나타내는 4바이트 정수입니다. 현재 포맷은 v9입니다. |
| `meta.smoosh` | smoosh 파일 안에 담긴 다른 파일들의 이름과 오프셋(offset) 메타데이터입니다. |
| `XXXXX.smoosh` | 여러 파일을 하나로 합친 바이너리 파일입니다. 파일 디스크립터 수를 줄이기 위해 사용하며, 최대 2GB까지 담습니다. 각 컬럼(타임스탬프 컬럼 `__time` 포함)의 데이터 파일과, 추가 세그먼트 메타데이터를 담은 `index.drd` 파일이 들어 있습니다. |

### 압축

- 문자열·long·float·double 컬럼의 값 블록에는 기본적으로 LZ4 압축을 사용합니다.
- 문자열 컬럼의 비트맵과 숫자 컬럼의 null 값 인덱스에는 Roaring 압축을 사용합니다.
- 카디널리티가 높은 문자열 컬럼의 다중 값 필터링에서는 Roaring이 Concise보다 확실히 빠릅니다. Concise가 저장 공간은 조금 더 작을 수 있으나 필터링 성능은 떨어집니다.
- 압축은 개별 컬럼 단위가 아니라 세그먼트 수준에서 `IndexSpec` 파라미터로 설정합니다.

### 세그먼트 간 스키마 차이

같은 데이터소스 안에서도 세그먼트마다 스키마가 다를 수 있습니다. 어떤 세그먼트에 특정 디멘션이나 메트릭이 없어도 여러 세그먼트에 걸친 쿼리는 정상 동작합니다. 없는 디멘션은 빈 값(SQL 호환 모드에서는 null)처럼, 없는 메트릭은 존재하지 않는 것처럼 처리됩니다.

### 세그먼트 갱신의 함의

Druid의 MVCC 버전은 세그먼트 포맷 버전(v9)과는 별개의 개념입니다. 여러 간격에 걸친 갱신은 전체가 아니라 **간격 단위로만 원자적**입니다. v2 세그먼트가 v1을 교체하는 도중에는 클러스터에 v1과 v2가 섞여 있을 수 있습니다.

```
갱신 전:  foo_2015-01-01/2015-01-02_v1_0
          foo_2015-01-02/2015-01-03_v1_1
          foo_2015-01-03/2015-01-04_v1_2

로드 중:  foo_2015-01-01/2015-01-02_v1_0
          foo_2015-01-02/2015-01-03_v2_1
          foo_2015-01-03/2015-01-04_v1_2
```

v2 세그먼트는 완성되는 즉시 클러스터에 로드되어 겹치는 시간 구간의 v1 세그먼트를 교체하므로, 전환 기간의 쿼리는 v1과 v2가 섞인 결과를 볼 수 있습니다.

---

## 딥 스토리지

딥 스토리지는 세그먼트를 저장하는 곳으로, Apache Druid가 직접 제공하지 않는 외부 스토리지 메커니즘입니다. 딥 스토리지의 핵심 역할은 **데이터 내구성(durability)** 입니다. Druid 프로세스들이 이 스토리지에 접근해 세그먼트를 가져올 수만 있다면, Druid 노드를 아무리 많이 잃어도 데이터는 유실되지 않습니다.

세그먼트를 딥 스토리지에만 둘지, 딥 스토리지와 Historical 프로세스 양쪽에 둘지는 설정한 로드 규칙(load rule)에 따라 결정됩니다.

### 지원 구현체

**로컬(local)**

단일 서버 배포, 또는 NFS 같은 공유 파일시스템을 쓰는 다중 서버 환경에 적합합니다. 프로덕션 클러스터에서는 확장성과 견고성 면에서 클라우드 기반 스토리지가 더 낫습니다.

| 속성 | 가능한 값 | 설명 | 기본값 |
| --- | --- | --- | --- |
| `druid.storage.type` | `local` | 필수 설정입니다. | — |
| `druid.storage.storageDirectory` | 임의의 로컬 디렉터리 | 세그먼트를 저장할 디렉터리입니다. 세그먼트 캐시 위치와 달라야 합니다. | `/tmp/druid/localStorage` |
| `druid.storage.zip` | `true`, `false` | 세그먼트를 디렉터리로 저장할지 zip 파일로 저장할지 지정합니다. | `false` |

예시 설정:

```
druid.storage.type=local
druid.storage.storageDirectory=/tmp/druid/localStorage
```

**Amazon S3 및 S3 호환 스토리지**

Minio 같은 S3 호환 스토리지를 포함합니다. `druid-s3-extensions` 익스텐션(extension) 문서를 참고합니다.

**Google Cloud Storage**

`druid-google-extensions` 익스텐션으로 지원합니다.

**Azure Blob Storage**

`druid-azure-extensions` 익스텐션으로 지원합니다.

**HDFS**

`druid-hdfs-storage` 익스텐션으로 지원합니다.

**기타**

위 목록 외에도 커뮤니티 익스텐션 목록에 추가적인 딥 스토리지 구현체가 있습니다.

### 딥 스토리지에서 직접 쿼리

Druid는 주로 딥 스토리지에만 있는 세그먼트에 대한 쿼리도 지원합니다. Historical 프로세스에 로드된 세그먼트를 쿼리할 때보다 성능은 낮지만, Historical 프로세스의 수나 용량을 늘리지 않고도 더 많은 데이터에 접근할 수 있어 전체 스토리지 비용을 낮출 수 있습니다. 즉 쿼리 지연 시간을 어느 정도 감수하는 대신 비용 효율을 얻는 트레이드오프입니다. 자세한 내용은 Query from deep storage 문서를 참고합니다.

---

## 메타데이터 스토리지

Druid는 시스템에 관한 각종 메타데이터를 외부 메타데이터 스토리지에 보관합니다. 실제 데이터를 저장하는 곳이 아니라, Druid 클러스터가 동작하는 데 필수적인 메타데이터만 담습니다.

### 지원 데이터베이스

- **Derby**: 기본값이지만 프로덕션에는 적합하지 않습니다.
- **MySQL**, **PostgreSQL**: 프로덕션 환경에 권장합니다.

메타데이터 스토리지는 산발적인 태스크 실패를 막기 위해 반드시 ACID를 준수해야 합니다.

Derby 설정 예시(Coordinator 설정, `metadata.storage.*` 속성은 Derby를 쓰는 경우 Coordinator를 가리켜야 합니다):

```
druid.metadata.storage.type=derby
druid.metadata.storage.connector.connectURI=jdbc:derby://localhost:1527//opt/var/druid_state/derby;create=true
```

### 저장 내용

메타데이터 스토리지에는 다음 다섯 범주의 레코드가 저장됩니다.

- 세그먼트 레코드
- 규칙(rule) 레코드
- 설정(configuration) 레코드
- 태스크 관련 테이블
- 감사(audit) 레코드

### 세그먼트 테이블

`druid.metadata.storage.tables.segments` 속성으로 제어합니다. Coordinator가 이 테이블을 폴링해 클러스터에서 사용 가능해야 하는 세그먼트 집합을 결정합니다.

- `used` 컬럼: 1이면 세그먼트를 로드해야 함을, 0이면 언로드해야 함을 나타냅니다.
- `used_status_last_updated` 컬럼: `used` 상태가 마지막으로 변경된 시각입니다. 사용하지 않게 된 세그먼트를 메타데이터 스토리지에서 완전히 삭제할 후보인지 판단할 때 유용합니다.
- `payload` 컬럼: 세그먼트 전체 메타데이터를 JSON으로 저장합니다. dataSource, interval, version, loadSpec, dimensions, metrics, shardSpec, binaryVersion, size, identifier 필드를 포함합니다.

### 규칙 테이블

세그먼트를 클러스터 어디에 배치할지 결정하는 규칙을 저장합니다. Coordinator가 세그먼트 할당을 결정할 때 이 규칙을 참고합니다.

### 설정 테이블

런타임 설정 객체를 저장합니다. 클러스터 전체 설정을 실행 중에 변경할 수 있도록 하기 위한 용도입니다.

### 태스크 관련 테이블

Overlord와 Middle Manager가 태스크를 관리하면서 생성하고 사용합니다.

### 감사 테이블

Coordinator 규칙 변경을 비롯한 설정 변경 이력을 기록합니다.

### 커넥션 풀(DBCP) 설정

`druid.metadata.storage.connector.dbcp.` 접두사로 데이터베이스 커넥션 풀 속성을 지정할 수 있습니다. 단, `username`, `password`, `connectURI`, `validationQuery`, `testOnBorrow`는 `druid.metadata.storage.connector.` 접두사를 사용해야 합니다.

### 접근 프로세스

메타데이터 스토리지에 접근하는 프로세스는 다음 세 가지뿐입니다.

1. 인덱싱 서비스 프로세스
2. Realtime 프로세스
3. Coordinator 프로세스

따라서 네트워크 수준에서 이 프로세스들만 메타데이터 스토리지에 접근하도록 권한을 제한해야 합니다.

---

## ZooKeeper

Druid는 현재 클러스터 상태 관리에 Apache ZooKeeper(ZK)를 사용합니다. ZooKeeper 릴리스 페이지에서 받을 수 있는 모든 안정(stable) 버전을 지원합니다.

### ZooKeeper가 담당하는 작업

1. **Coordinator 리더 선출**
2. **Historical의 세그먼트 "게시" 프로토콜**: Historical과 Realtime 프로세스가 자신의 존재와 서빙 중인 세그먼트를 알립니다.
3. **Overlord 리더 선출**
4. **Overlord–Middle Manager 태스크 관리**

### 리더 선출

Coordinator 리더 선출은 Curator의 LeaderLatch 레시피를 사용하며, 다음 경로에서 수행됩니다.

```
${druid.zk.paths.coordinatorPath}/_COORDINATOR
```

Overlord 리더 선출도 같은 방식의 메커니즘을 사용합니다.

### 세그먼트 게시(announcement) 프로토콜

Historical 프로세스는 두 가지 znode 경로를 사용합니다.

- **announcements 경로**: 각 Historical 서비스가 자신의 존재를 알리는 임시(ephemeral) znode를 만듭니다.

```
${druid.zk.paths.announcementsPath}/${druid.host}
```

- **live segments 경로**: 영구(permanent) znode를 만든 뒤,

```
${druid.zk.paths.liveSegmentsPath}/${druid.host}
```

세그먼트를 로드할 때마다 그 아래에 세그먼트별 임시 znode를 추가합니다.

```
${druid.zk.paths.liveSegmentsPath}/${druid.host}/_segment_identifier_
```

Coordinator와 Broker는 이 경로들을 감시(watch)해 클러스터의 어떤 프로세스가 어떤 세그먼트를 서빙 중인지 추적합니다.

---

## 참고 자료

- [Architecture](https://druid.apache.org/docs/latest/design/architecture)
- [Storage overview](https://druid.apache.org/docs/latest/design/storage)
- [Segments](https://druid.apache.org/docs/latest/design/segments)
- [Deep storage](https://druid.apache.org/docs/latest/design/deep-storage)
- [Metadata storage](https://druid.apache.org/docs/latest/design/metadata-storage)
- [ZooKeeper](https://druid.apache.org/docs/latest/design/zookeeper)

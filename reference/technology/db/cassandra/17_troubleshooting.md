# Cassandra 트러블슈팅

> 원본: https://cassandra.apache.org/doc/latest/cassandra/troubleshooting/

---

## 목차

1. [개요](#개요)
2. [문제 노드 찾기 (Find The Misbehaving Nodes)](#문제-노드-찾기-find-the-misbehaving-nodes)
   - [클라이언트 로그와 에러 (Client Logs and Errors)](#클라이언트-로그와-에러-client-logs-and-errors)
   - [메트릭 (Metrics)](#메트릭-metrics)
3. [Cassandra 로그 읽기 (Cassandra Logs)](#cassandra-로그-읽기-cassandra-logs)
   - [system.log](#systemlog)
   - [debug.log](#debuglog)
   - [gc.log](#gclog)
   - [로깅 레벨 변경하기](#로깅-레벨-변경하기)
4. [nodetool 활용하기 (Use Nodetool)](#nodetool-활용하기-use-nodetool)
   - [클러스터 상태 (nodetool status)](#클러스터-상태-nodetool-status)
   - [코디네이터 쿼리 지연 (nodetool proxyhistograms)](#코디네이터-쿼리-지연-nodetool-proxyhistograms)
   - [로컬 쿼리 지연 (nodetool tablehistograms)](#로컬-쿼리-지연-nodetool-tablehistograms)
   - [스레드풀 상태 (nodetool tpstats)](#스레드풀-상태-nodetool-tpstats)
   - [컴팩션 상태 (nodetool compactionstats)](#컴팩션-상태-nodetool-compactionstats)
   - [데이터 파일 경로 (nodetool datapaths)](#데이터-파일-경로-nodetool-datapaths)
5. [심층 분석: 외부 도구 활용 (Diving Deep, Use External Tools)](#심층-분석-외부-도구-활용-diving-deep-use-external-tools)
   - [JVM 도구](#jvm-도구)
   - [OS 도구](#os-도구)
   - [고급 도구](#고급-도구)
   - [패킷 캡처 (Packet Capture)](#패킷-캡처-packet-capture)
6. [참고 자료](#참고-자료)

---

## 개요

분산 데이터베이스라면 어느 것이든 그렇듯, Cassandra도 때때로 문제가 발생하므로 무슨 일이 벌어지고 있는지 트러블슈팅해야 합니다. 트러블슈팅 과정의 핵심은 클러스터 안에서 문제를 일으키는 머신을 찾아내고, 로그와 인트로스펙션 도구를 통해 그 문제를 격리하는 것입니다.

다른 분산 Java 프로그램을 디버깅하듯, 우선 클러스터 안에서 어떤 머신이 비정상적으로 동작하는지 찾아낸 다음, 로그와 도구를 사용해 문제를 좁혀나가야 합니다.

이 문서의 예제들은 대부분 Linux/Unix 시스템을 기준으로 작성되었습니다. 다른 플랫폼에서는 그에 상응하는 도구가 필요할 수 있습니다.

트러블슈팅은 다음 네 가지 주제로 구성됩니다.

1. **문제 노드 찾기 (Finding nodes)** — 클러스터 안에서 어떤 머신이 문제를 겪고 있는지 위치를 파악합니다.
2. **로그 읽기 (Reading logs)** — 진단(diagnostic) 정보를 얻기 위해 Cassandra 로그 파일을 이해하고 해석합니다.
3. **nodetool 활용 (Using nodetool)** — Cassandra의 명령줄(command-line) 관리 유틸리티를 사용하여 클러스터를 분석합니다.
4. **외부 도구 활용 (Using tools)** — 외부 도구를 사용하여 클러스터 동작을 더 깊이 조사하고 분석합니다.

---

## 문제 노드 찾기 (Find The Misbehaving Nodes)

트러블슈팅의 첫 단계는 문제가 클라이언트 측에서 발생한 것인지 서버 측에서 발생한 것인지 구분하고, 그다음으로 클러스터 안에서 문제를 일으키는 노드의 위치를 찾아내는 것입니다. 이때 목표는 클러스터 전체에 영향을 미치는 시스템적 문제인지, 아니면 특정 노드에 국한된 문제인지를 구별하는 데 있습니다.

많은 경우, 에러 메시지를 읽는 것만으로도 여러 장애 모드를 배제할 수 있습니다. 또한 다수의 Cassandra 에러 메시지에는 마지막으로 접속을 시도한 코디네이터 정보가 포함되어 있어, 운영자가 어떤 노드부터 조사를 시작해야 할지 파악하는 데 도움이 됩니다.

### 클라이언트 로그와 에러 (Client Logs and Errors)

클러스터의 클라이언트는 따라가기 가장 좋은 단서를 남기는 경우가 많습니다. 클라이언트 측에서 발생하는 예외와 에러를 분석하면 문제의 원인을 빠르게 좁힐 수 있습니다. 자주 등장하는 예외 유형과 그 의미는 다음과 같습니다.

- **`SyntaxError` (및 기타 `QueryValidationException`)**: 클라이언트가 잘못된 형식의 요청을 보냈음을 의미합니다. 서버 문제인 경우는 드물고, 대개 쿼리 자체에 문제가 있습니다. 이 에러가 보이면 거의 항상 클라이언트 측 쿼리를 확인해야 합니다.

- **`UnavailableException`**: Cassandra 코디네이터 노드가 사용 가능한 복제본이 충분하지 않다고 판단하여 쿼리를 거부했음을 의미합니다. 일관성 수준을 만족시킬 만큼 충분한 복제본이 살아 있지 않다는 뜻이며, 여러 노드가 다운되어 있을 가능성을 시사합니다.

- **`OperationTimedOutException`**: 클라이언트 측 타임아웃으로, 쿼리가 클라이언트가 지정한 타임아웃보다 오래 걸렸음을 뜻합니다. 클라이언트의 타임아웃 설정이 지나치게 짧게 잡혀 있을 때 자주 발생합니다.

- **`ReadTimeoutException` / `WriteTimeoutException`**: 클라이언트가 별도로 더 짧은 타임아웃을 지정하지 않은 상태에서 `cassandra.yaml`에 지정된 값에 따라 코디네이터 타임아웃이 발생한 경우입니다. 대개 심각한 서버 측 문제를 나타내므로, 클라이언트 측 타임아웃보다 훨씬 심각한 신호로 해석해야 합니다.

이러한 예외를 구분하는 것이 중요한 이유는, 문제의 책임 소재가 클라이언트인지 서버인지를 빠르게 판별할 수 있기 때문입니다. `SyntaxError`처럼 클라이언트 측 문제로 분류되는 에러는 서버 노드 조사가 불필요하지만, `ReadTimeoutException`이나 `WriteTimeoutException`처럼 코디네이터에서 발생한 타임아웃은 특정 노드 또는 클러스터 전반의 심각한 문제를 가리킵니다.

### 메트릭 (Metrics)

클라이언트 로그로 문제의 대략적인 위치를 파악했다면, 다음으로는 메트릭을 분석하여 어떤 노드가 문제를 일으키는지 더 구체적으로 좁혀나갑니다.

#### 에러: 드롭된 메시지 (Errors / Dropped Messages)

Cassandra는 노드 간 메시징 에러를 "드롭(drops)"이라고 부릅니다. 특정 노드에서 드롭이 집중적으로 발생하고 있다면 그 노드가 문제와 관련되어 있을 가능성이 높습니다.

#### 지연 (Latency)

코디네이터 레벨 메트릭(`CoordinatorReadLatency`, `CoordinatorWriteLatency`)과 복제본 레벨 메트릭(`ReadLatency`, `WriteLatency`)을 비교하면, 문제의 양상을 다음 세 가지 패턴으로 구분할 수 있습니다.

1. **코디네이터 지연은 모든 노드에서 높지만, 일부 노드의 로컬 읽기 지연만 높은 경우**: 소수의 느린 복제본이 전체 코디네이터 지연을 끌어올리고 있는 상황입니다.

2. **코디네이터 지연과 복제본 지연이 일부 노드에서 동시에 상승하는 경우**: 특정 토큰 범위를 담당하는 복제본들이 느려졌음을 의미합니다.

3. **코디네이터 지연과 로컬 지연이 다수의 노드에서 모두 높은 경우**: 클러스터 전반의 용량 부족이거나 새로운 쿼리 패턴이 도입된 경우가 원인인 경우가 많습니다.

#### 쿼리 비율 / 배치 증폭 (Query Rates / Batch Amplification)

배치 연산은 코디네이터 요청 수에 비해 복제본 레벨의 부하를 크게 증폭시킬 수 있습니다. 예를 들어 클라이언트가 50개의 명령문을 담은 단일 `BATCH` 쿼리를 보내고, 복제본이 9개(RF=3, 3개의 데이터센터)라면, 코디네이터의 `BATCH` 쓰기 1건이 450개의 복제본 쓰기로 변환됩니다. 따라서 코디네이터에서 보이는 쿼리 비율이 낮더라도 복제본 레벨에서는 그보다 훨씬 큰 부하가 발생할 수 있습니다.

#### 조사 경로 (Investigation Path)

문제의 위치를 좁힌 후에는 해당 노드에 SSH로 접속하여 로그를 확인하고, `nodetool`과 OS 레벨 진단 도구를 활용하여 더 깊이 분석합니다.

---

## Cassandra 로그 읽기 (Cassandra Logs)

Cassandra는 풍부한 로깅 지원을 제공하며, 운영 가시성을 확보하면서도 불필요한 노이즈는 최소화하도록 설계된 세 가지 주요 로그 파일을 갖고 있습니다.

### system.log

`system.log`는 기본(default) 로그 파일로, 다음과 같은 정보를 담고 있습니다.

- 디버깅을 위한 처리되지 않은 예외(uncaught exceptions)
- 가비지 컬렉션(garbage collection, GC) 일시 정지에 관한 `GCInspector` 메시지
- 클러스터 토폴로지(topology) 변경 및 토큰 메타데이터(token metadata) 업데이트
- 키스페이스(keyspace) 및 테이블(table) 연산
- `StartupChecks` 검증 결과
- 백그라운드 작업(background task) 정보

이 로그에서 문제를 찾으려면 다음과 같이 WARN 및 ERROR 레벨 메시지를 필터링합니다.

```
$ grep 'WARN\|ERROR' system.log | tail
```

`system.log`는 시작 시 발생하는 이벤트, 예외, 그리고 운영 중 발생하는 주요 사건들을 기록하므로, 트러블슈팅의 출발점으로 가장 먼저 살펴봐야 할 로그입니다.

### debug.log

`debug.log`는 트러블슈팅에 유용한 추가 디버깅 정보를 담고 있으며, 특히 다음 내용에 대한 상세한 정보를 제공합니다.

- 컴팩션(compaction) 활동 및 SSTable 정보
- 멤테이블 플러시(memtable flush) 연산과 그것이 커밋로그(commitlog)에 미치는 영향

이 로그는 양이 방대하므로 필터링하여 분석해야 합니다. 예를 들어 컴팩션 작업을 전후 5줄(`-C 5`)의 문맥과 함께 살펴보려면 다음과 같이 합니다.

```
$ grep CompactionTask debug.log -C 5
```

플러시 분포를 추적하여 어떤 테이블이 가장 자주 플러시되는지 확인하려면 "Enqueuing flush" 메시지를 추출하여 집계합니다.

```
$ grep "Enqueuing flush" debug.log | cut -f 10 -d ' ' | sort | uniq -c
    6 compaction_history:
    1 test_keyspace:
    2 local:
    17 size_estimates:
    17 sstable_activity:
```

위 출력은 각 테이블이 몇 번 플러시 큐(queue)에 들어갔는지를 보여줍니다. 예를 들어 `size_estimates`와 `sstable_activity` 테이블이 각각 17번씩 플러시되었음을 알 수 있습니다.

### gc.log

`gc.log`는 표준 Java 가비지 컬렉션 데이터를 담고 있으며, 애플리케이션의 일시 정지 시간과 처리량 메트릭을 보여줍니다. 이 로그를 분석하면 JVM 튜닝에 대한 통찰을 얻을 수 있습니다. GC 일시 정지는 Cassandra의 테일 레이턴시를 일으키는 주요 원인 중 하나입니다.

`gc.log`의 출력 예시는 다음과 같이 일시 정지 메트릭을 보여줍니다.

```
2018-08-29T00:19:39.522+0000: 3022663.591: Total time for which
application threads were stopped: 0.0332813 seconds, Stopping
threads took: 0.0008189 seconds
```

위 로그는 애플리케이션 스레드가 약 0.033초 동안 정지되었으며, 스레드를 정지시키는 데 약 0.0008초가 걸렸음을 나타냅니다.

가장 긴 일시 정지를 분석하려면 "stopped" 메시지에서 정지 시간을 추출한 뒤 정렬하여 상위 값을 확인합니다.

```
$ grep stopped gc.log.0.current | cut -f 11 -d ' ' | sort -n | tail
```

이처럼 일시 정지 시간을 추출하여 정렬하면 수 초에 달하는 JVM 일시 정지를 식별할 수 있습니다. 이러한 장시간 일시 정지는 디스크 지연 문제로 인해 세이프포인트 진입이 지연되면서 발생하는 경우가 많습니다. 일시 정지 시간의 분포를 히스토그램 형태로 분석하면 평균, 분산, 그리고 밀리초 단위 구간별 분포를 통해 문제가 되는 패턴을 식별할 수 있습니다.

### 로깅 레벨 변경하기

`nodetool setlogginglevel` 명령을 사용하면 로깅 레벨을 동적으로 조정할 수 있습니다. 예를 들어 Gossip 관련 로그를 `TRACE` 레벨로 자세히 보려면 다음과 같이 실행합니다.

```
$ nodetool setlogginglevel org.apache.cassandra.gms.Gossiper TRACE
```

이렇게 동적으로 변경한 로깅 레벨은 노드를 재시작하면 원래대로 되돌아갑니다. 변경 사항을 영구적으로 적용하려면 `logback.xml` 설정 파일에 다음 항목을 추가합니다.

```xml
<logger name="org.apache.cassandra.gms.Gossiper" level="TRACE"/>
```

---

## nodetool 활용하기 (Use Nodetool)

Cassandra의 `nodetool`은 클러스터 차원의 문제를 특정 노드 단위로 좁히는 데 유용하며, Cassandra 프로세스 상태에 대한 풍부한 통찰을 제공합니다. 예를 들어 다수의 코디네이터에서 `UnavailableException`이 발생하고 있다면, `nodetool status`로 다운된 노드를 식별할 수 있습니다.

### 클러스터 상태 (nodetool status)

`nodetool status`는 클러스터 내 각 노드의 상태(주소/부하/토큰/소유 비율/호스트 ID/랙)를 보여줍니다. 키스페이스를 선택적으로 지정하면 해당 키스페이스 기준의 유효 소유 비율(effective ownership)을 확인할 수 있습니다.

```
$ nodetool status <optional keyspace>

Datacenter: dc1
=======================
Status=Up/Down
|/ State=Normal/Leaving/Joining/Moving
--  Address    Load       Tokens       Owns (effective)  Host ID                               Rack
UN  127.0.1.1  4.69 GiB   1            100.0%            35ea8c9f-b7a2-40a7-b9c5-0ee8b91fdd0e  r1
UN  127.0.1.2  4.71 GiB   1            100.0%            752e278f-b7c5-4f58-974b-9328455af73f  r2
UN  127.0.1.3  4.69 GiB   1            100.0%            9dc1a293-2cc0-40fa-a6fd-9e6054da04a7  r3
```

출력 첫머리의 범례를 통해 각 노드의 상태를 해석할 수 있습니다.

- **Status** = `U`(Up) / `D`(Down)
- **State** = `N`(Normal) / `L`(Leaving) / `J`(Joining) / `M`(Moving)

예를 들어 `UN`은 Up이면서 Normal 상태를 의미합니다. 노드가 `DN`으로 표시된다면 해당 노드가 다운된 것이므로 우선적으로 조사해야 합니다. **Load**는 디스크에 저장된 데이터 양, **Owns (effective)**는 복제 인자(replication factor)를 반영한 유효 데이터 소유 비율을 나타냅니다.

### 코디네이터 쿼리 지연 (nodetool proxyhistograms)

`nodetool proxyhistograms`는 코디네이터 관점에서 본 쿼리 지연의 백분위수 분포를 보여줍니다. 코디네이터가 클라이언트 요청을 처리하는 데 걸리는 전체 시간(여러 복제본으로의 요청 포함)을 나타냅니다.

```
$ nodetool proxyhistograms
Percentile       Read Latency      Write Latency      Range Latency   CAS Read Latency  CAS Write Latency View Write Latency
                     (micros)           (micros)           (micros)           (micros)           (micros)           (micros)
50%                    454.83             219.34               0.00               0.00               0.00               0.00
75%                    545.79             263.21               0.00               0.00               0.00               0.00
95%                    654.95             315.85               0.00               0.00               0.00               0.00
98%                    785.94             379.02               0.00               0.00               0.00               0.00
99%                   3379.39            2346.80               0.00               0.00               0.00               0.00
Min                     42.51             105.78               0.00               0.00               0.00               0.00
Max                  25109.16           43388.63               0.00               0.00               0.00               0.00
```

단위는 마이크로초입니다. 50%(중앙값) 읽기 지연은 약 454마이크로초이지만, p99 읽기 지연은 약 3379마이크로초로 크게 뜁니다. p99/Max 값이 중앙값 대비 비정상적으로 크다면 테일 레이턴시 문제가 있음을 의미합니다. `CAS Read/Write Latency`는 경량 트랜잭션(LWT), `View Write Latency`는 구체화된 뷰(materialized view) 관련 지연입니다.

### 로컬 쿼리 지연 (nodetool tablehistograms)

`nodetool tablehistograms`는 특정 키스페이스와 테이블에 대해 해당 노드의 로컬 쿼리 지연 및 관련 분포를 보여줍니다. 코디네이터 지연과 비교하면 문제가 코디네이션 경로에 있는지 로컬 스토리지 경로에 있는지 구분할 수 있습니다.

```
$ nodetool tablehistograms keyspace table
Percentile  SSTables     Write Latency      Read Latency    Partition Size        Cell Count
                              (micros)          (micros)           (bytes)
50%             0.00             73.46            182.79             17084               103
75%             1.00             88.15            315.85             17084               103
95%             2.00            126.93            545.79             17084               103
98%             2.00            152.32            654.95             17084               103
99%             2.00            182.79            785.94             17084               103
Min             0.00             42.51             24.60             14238                87
Max             2.00          12108.97          17436.92             17084               103
```

각 열의 의미는 다음과 같습니다.

- **SSTables**: 단일 읽기 쿼리에서 접근한 SSTable 개수입니다. 값이 높다면 읽기 증폭(read amplification)이 발생하고 있으며, 컴팩션 전략이나 데이터 모델에 문제가 있을 수 있습니다.
- **Write Latency / Read Latency**: 해당 노드의 로컬 쓰기/읽기 지연(마이크로초 단위)입니다.
- **Partition Size**: 파티션 크기(바이트 단위)로, 값이 크면 핫 파티션이나 비대해진 파티션 문제를 의심할 수 있습니다.
- **Cell Count**: 파티션당 셀 개수입니다.

### 스레드풀 상태 (nodetool tpstats)

`nodetool tpstats`는 Cassandra 내부 스레드풀(stage라고도 함) 상태와 드롭된 메시지 통계를 보여줍니다. Cassandra 프로세스가 어떤 작업에서 병목을 겪는지 파악하는 데 유용합니다.

```
$ nodetool tpstats
Pool Name                         Active   Pending      Completed   Blocked  All time blocked
ReadStage                              2         0             12         0                 0
MiscStage                              0         0              0         0                 0
CompactionExecutor                     0         0           1940         0                 0
MutationStage                          0         0              0         0                 0
GossipStage                            0         0          10293         0                 0
Repair-Task                            0         0              0         0                 0
RequestResponseStage                   0         0             16         0                 0
ReadRepairStage                        0         0              0         0                 0
CounterMutationStage                   0         0              0         0                 0
MemtablePostFlush                      0         0             83         0                 0
ValidationExecutor                     0         0              0         0                 0
MemtableFlushWriter                    0         0             30         0                 0
ViewMutationStage                      0         0              0         0                 0
CacheCleanupExecutor                   0         0              0         0                 0
MemtableReclaimMemory                  0         0             30         0                 0
PendingRangeCalculator                 0         0             11         0                 0
SecondaryIndexManagement               0         0              0         0                 0
HintsDispatcher                        0         0              0         0                 0
Native-Transport-Requests              0         0            192         0                 0
MigrationStage                         0         0             14         0                 0
PerDiskMemtableFlushWriter_0           0         0             30         0                 0
Sampler                                0         0              0         0                 0
ViewBuildExecutor                      0         0              0         0                 0
InternalResponseStage                  0         0              0         0                 0
AntiEntropyStage                       0         0              0         0                 0

Message type           Dropped                  Latency waiting in queue (micros)
                                             50%               95%               99%               Max
READ                         0               N/A               N/A               N/A               N/A
RANGE_SLICE                  0              0.00              0.00              0.00              0.00
_TRACE                       0               N/A               N/A               N/A               N/A
HINT                         0               N/A               N/A               N/A               N/A
MUTATION                     0               N/A               N/A               N/A               N/A
COUNTER_MUTATION             0               N/A               N/A               N/A               N/A
BATCH_STORE                  0               N/A               N/A               N/A               N/A
BATCH_REMOVE                 0               N/A               N/A               N/A               N/A
REQUEST_RESPONSE             0              0.00              0.00              0.00              0.00
PAGED_RANGE                  0               N/A               N/A               N/A               N/A
READ_REPAIR                  0               N/A               N/A               N/A               N/A
```

**스레드풀(상단 표)의 각 열 의미:**

- **Active**: 현재 실행 중인 작업 수입니다.
- **Pending**: 실행 대기 중인 작업 수입니다. 지속적으로 높다면 해당 스테이지가 처리 한계를 초과하는 부하를 받고 있는 것입니다.
- **Completed**: 노드 시작 이후 완료된 작업의 누적 수입니다.
- **Blocked**: 현재 차단된 작업 수입니다.
- **All time blocked**: 노드 시작 이후 차단된 작업의 누적 수입니다. 값이 크면 해당 스테이지가 한계에 도달했음을 나타냅니다.

주요 스테이지로는 `ReadStage`(읽기), `MutationStage`(쓰기), `CompactionExecutor`(컴팩션), `GossipStage`(가십 프로토콜), `Native-Transport-Requests`(CQL 네이티브 프로토콜 요청) 등이 있습니다.

**드롭된 메시지(하단 표)의 의미:**

`Dropped` 열은 각 메시지 유형(`READ`, `MUTATION`, `RANGE_SLICE` 등)별로 노드가 드롭한 메시지 수를 보여줍니다. Cassandra는 처리 시간이 타임아웃을 초과한 내부 메시지를 드롭합니다. 특정 노드에서 드롭 수가 0보다 크다면 해당 노드가 부하를 감당하지 못하고 있으며 문제의 근원일 가능성이 높습니다. `Latency waiting in queue` 열은 각 메시지 유형이 큐에서 대기한 시간의 백분위 분포를 보여줘, 어느 단계에서 지연이 누적되는지 파악할 수 있습니다.

### 컴팩션 상태 (nodetool compactionstats)

`nodetool compactionstats`는 현재 진행 중이거나 대기 중인 컴팩션 작업을 보여줍니다. 대기 중인 컴팩션(pending tasks)이 많이 쌓여 있다면 컴팩션이 쓰기 부하를 따라가지 못하고 있다는 신호이며, 읽기 지연 증가로 이어질 수 있습니다.

```
$ nodetool compactionstats
pending tasks: 2
- keyspace.table: 2

id                                   compaction type keyspace table completed total    unit  progress
2062b290-7f3a-11e8-9358-cd941b956e60 Compaction      keyspace table 21848273  97867583 bytes 22.32%
Active compaction remaining time :   0h00m04s
```

`pending tasks: 2`는 대기 중인 컴팩션이 2개임을 의미하며, 진행 행은 `keyspace.table` 컴팩션이 전체 약 9786만 바이트 중 2184만 바이트를 처리하여 22.32% 진행되었음을 보여줍니다. `Active compaction remaining time`은 예상 잔여 시간(약 4초)입니다.

### 데이터 파일 경로 (nodetool datapaths)

`nodetool datapaths`는 지정한 키스페이스의 각 테이블이 디스크의 어느 경로에 저장되어 있는지 보여줍니다. 디스크 레벨에서 SSTable 파일을 직접 조사해야 할 때 유용합니다.

```
% nodetool datapaths -- system_auth
Keyspace: system_auth
	Table: role_permissions
	Paths:
		/var/lib/cassandra/data/system_auth/role_permissions-3afbe79f219431a7add7f5ab90d8ec9c

	Table: network_permissions
	Paths:
		/var/lib/cassandra/data/system_auth/network_permissions-d46780c22f1c3db9b4c1b8d9fbc0cc23

	Table: resource_role_permissons_index
	Paths:
		/var/lib/cassandra/data/system_auth/resource_role_permissons_index-5f2fbdad91f13946bd25d5da3a5c35ec

	Table: roles
	Paths:
		/var/lib/cassandra/data/system_auth/roles-5bc52802de2535edaeab188eecebb090

	Table: role_members
	Paths:
		/var/lib/cassandra/data/system_auth/role_members-0ecdaa87f8fb3e6088d174fb36fe5c0d
```

---

## 심층 분석: 외부 도구 활용 (Diving Deep, Use External Tools)

로그와 `nodetool`로 문제 노드를 식별한 뒤에도 원인이 명확하지 않다면 OS와 JVM 레벨의 외부 도구로 더 깊이 파고들어야 합니다. 트러블슈팅은 일반적으로 어떤 자원(CPU, RAM, 디스크, 네트워크)이 병목인지 식별하고, 그에 따라 자원을 추가하거나 쿼리 패턴을 최적화하는 방향으로 귀결됩니다.

### JVM 도구

Cassandra는 JVM 위에서 동작하므로 JVM 레벨의 진단 도구가 매우 중요합니다.

#### jstat (가비지 컬렉션 상태)

`jstat`은 힙 사용량과 가비지 컬렉션 활동을 실시간으로 모니터링합니다. 다음 명령은 500밀리초마다 Cassandra 프로세스의 GC 상태를 출력합니다.

```
jstat -gcutil <cassandra pid> 500ms
```

이 명령은 Eden 영역의 힙 사용량, Old Generation 점유율, 컬렉션 횟수, 소요 시간을 보여줍니다. Old Generation이 일상적으로 75%를 초과한다면 힙을 늘려야 할 가능성이 높습니다.

#### jstack (스레드 덤프)

`jstack`은 특정 시점의 스레드 활동 스냅샷을 캡처합니다. 다음과 같이 Cassandra 프로세스의 스레드 덤프를 파일로 저장할 수 있습니다.

```
jstack <cassandra pid> > threaddump
```

출력에는 각 스레드의 상태, 차단 조건, 잠금 정보가 포함되어 있어 경합 문제를 진단하는 데 활용할 수 있습니다. 예를 들어 많은 스레드가 동일한 잠금을 기다리며 `BLOCKED` 상태에 있다면 그 잠금이 병목임을 알 수 있습니다.

#### JVM 힙 분석 (JVM Heap Analysis) 및 Java Flight Recorder

힙 메모리 문제(메모리 누수, OOM 등)를 분석하려면 힙 덤프를 떠서 분석 도구로 조사합니다. Java Flight Recorder(JFR)와 같은 프로파일링 도구를 사용하면 일정 기간 동안의 JVM 동작(메모리 할당, 잠금 경합, GC, 메서드 호출 프로파일 등)을 기록하고 사후에 분석할 수 있습니다.

### OS 도구

OS 레벨 도구는 머신 전체의 자원 사용 현황을 파악하는 데 사용됩니다.

#### top / htop

`top`(또는 `htop`)은 시스템 부하(load), CPU 사용률(사용자/시스템/io-wait 구분), 메모리 사용량을 평가하는 데 사용됩니다. 부하 평균(load average)이 CPU 코어 수보다 크다면 Cassandra에서 100밀리초 미만의 양호한 지연 성능을 기대하기 어렵습니다. CPU 사용률에서 io-wait 비중이 높다면 디스크가 병목임을 시사합니다.

#### iostat

`iostat`은 디스크 성능을 측정하며, 지연 분포(`await`), 처리량, 사용률을 보여줍니다. 출력에는 초당 읽기/쓰기 연산 수와 서비스 시간이 표시됩니다. `await` 값이 높다면 디스크 I/O가 병목이 되고 있는 것입니다.

#### 네트워크 도구

네트워크 관련 진단에는 다음 도구들이 사용됩니다.

- **`ping` / `traceroute` / `mtr`**: 노드 간 지연과 경로를 평가합니다. `mtr`은 지속적인 경로 추적과 패킷 손실 통계를 함께 제공합니다.
- **`iftop`**: repair, bootstrap 등의 작업 중 대역폭 사용량을 모니터링합니다.

### 고급 도구

#### bcc-tools

`bcc-tools`는 eBPF 기반의 커널 레벨 통찰을 제공합니다.

- **cachestat**: OS 페이지 캐시의 히트/미스를 측정합니다.
- **biolatency**: 디스크 I/O 지연 분포를 보여줍니다.
- **biosnoop**: 개별 I/O별 지연 세부 정보를 캡처합니다.

디스크 I/O 병목이나 페이지 캐시 효율을 정밀하게 진단할 때 유용합니다.

#### vmtouch

`vmtouch`는 데이터 파일 중 얼마나 많은 비율이 OS 페이지 캐시에 상주하고 있는지 확인합니다.

```
./vmtouch /var/lib/cassandra/data/
```

페이지 캐시에 데이터가 많이 적재되어 있을수록 디스크 접근이 줄어 읽기 성능이 향상됩니다.

#### CPU 플레임그래프 (CPU Flamegraphs)

`perf`를 프레임 포인터가 활성화된 상태로 사용하면 Cassandra 코드 경로 전반의 CPU 사용 패턴을 플레임그래프로 시각화할 수 있습니다. 어떤 메서드/코드 경로가 CPU 시간을 가장 많이 소비하는지 한눈에 파악할 수 있습니다.

### 패킷 캡처 (Packet Capture)

`tcpdump`와 Wireshark를 사용하여 실시간 CQL 트래픽을 캡처하고 분석할 수 있습니다. 다음 명령은 9042 포트(CQL 네이티브 프로토콜)의 TCP 트래픽을 `cassandra.pcap` 파일로 저장합니다.

```
sudo tcpdump -U -s0 -i <INTERFACE> -w cassandra.pcap -n "tcp port 9042"
```

각 옵션의 의미는 다음과 같습니다.

- `-U`: 패킷을 버퍼링하지 않고 즉시 기록합니다(packet-buffered 출력).
- `-s0`: 패킷 전체를 잘리지 않게 캡처합니다.
- `-i <INTERFACE>`: 캡처할 네트워크 인터페이스를 지정합니다.
- `-w cassandra.pcap`: 캡처 결과를 파일로 저장합니다.
- `-n`: 호스트명/포트명을 이름으로 해석하지 않고 숫자로 표시합니다.
- `"tcp port 9042"`: CQL 네이티브 프로토콜 포트의 TCP 트래픽만 필터링합니다.

이렇게 저장한 `.pcap` 파일을 Wireshark로 열어 클라이언트와 코디네이터 간에 오간 실제 쿼리와 응답을 분석하면, 쿼리 패턴이나 비정상적인 트래픽을 정밀하게 조사할 수 있습니다.

---

## 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [Troubleshooting (트러블슈팅 개요)](https://cassandra.apache.org/doc/latest/cassandra/troubleshooting/)
- [Find The Misbehaving Nodes (문제 노드 찾기)](https://cassandra.apache.org/doc/latest/cassandra/troubleshooting/finding_nodes.html)
- [Cassandra Logs (로그 읽기)](https://cassandra.apache.org/doc/latest/cassandra/troubleshooting/reading_logs.html)
- [Use Nodetool (nodetool 활용)](https://cassandra.apache.org/doc/latest/cassandra/troubleshooting/use_nodetool.html)
- [Diving Deep, Use External Tools (외부 도구 활용)](https://cassandra.apache.org/doc/latest/cassandra/troubleshooting/use_tools.html)

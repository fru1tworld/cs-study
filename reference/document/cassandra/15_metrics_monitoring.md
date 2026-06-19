# Cassandra 메트릭과 모니터링

> 이 문서는 Apache Cassandra 공식 문서의 "Metrics" 및 "Monitoring" 섹션을 한국어로 번역한 것입니다.
> 원본: https://cassandra.apache.org/doc/latest/cassandra/managing/operating/metrics.html

---

## 목차

1. [개요](#개요)
2. [메트릭 타입(Metric Types)](#메트릭-타입metric-types)
3. [테이블 메트릭(Table Metrics)](#테이블-메트릭table-metrics)
4. [키스페이스 메트릭(Keyspace Metrics)](#키스페이스-메트릭keyspace-metrics)
5. [스레드 풀 메트릭(ThreadPool Metrics)](#스레드-풀-메트릭threadpool-metrics)
6. [클라이언트 요청 메트릭(Client Request Metrics)](#클라이언트-요청-메트릭client-request-metrics)
7. [캐시 메트릭(Cache Metrics)](#캐시-메트릭cache-metrics)
8. [CQL 메트릭(CQL Metrics)](#cql-메트릭cql-metrics)
9. [드롭된 메시지 메트릭(DroppedMessage Metrics)](#드롭된-메시지-메트릭droppedmessage-metrics)
10. [스트리밍 메트릭(Streaming Metrics)](#스트리밍-메트릭streaming-metrics)
11. [컴팩션 메트릭(Compaction Metrics)](#컴팩션-메트릭compaction-metrics)
12. [커밋로그 메트릭(CommitLog Metrics)](#커밋로그-메트릭commitlog-metrics)
13. [스토리지 메트릭(Storage Metrics)](#스토리지-메트릭storage-metrics)
14. [힌티드 핸드오프 메트릭(HintedHandoff Metrics)](#힌티드-핸드오프-메트릭hintedhandoff-metrics)
15. [힌트 서비스 메트릭(HintsService Metrics)](#힌트-서비스-메트릭hintsservice-metrics)
16. [SSTable 인덱스 메트릭(SSTable Index Metrics)](#sstable-인덱스-메트릭sstable-index-metrics)
17. [버퍼 풀 메트릭(BufferPool Metrics)](#버퍼-풀-메트릭bufferpool-metrics)
18. [클라이언트 메트릭(Client Metrics)](#클라이언트-메트릭client-metrics)
19. [배치 메트릭(Batch Metrics)](#배치-메트릭batch-metrics)
20. [JVM 메트릭(JVM Metrics)](#jvm-메트릭jvm-metrics)
21. [메트릭 접근 방법](#메트릭-접근-방법)
22. [가상 테이블을 통한 모니터링(Virtual Tables)](#가상-테이블을-통한-모니터링virtual-tables)
23. [참고 자료](#참고-자료)

---

## 개요

Cassandra의 메트릭(metric)은 **Dropwizard Metrics** 라이브러리를 사용하여 관리됩니다. 메트릭은 **JMX**, **가상 테이블(Virtual Tables)** 을 통해 조회하거나, 다양한 내장 리포터(reporter) 또는 서드파티(third-party) 리포터 플러그인을 사용하여 외부 모니터링 시스템으로 전송(push)할 수 있습니다.

메트릭은 **단일 노드(single node)** 단위로 수집됩니다. 이를 클러스터 전체 관점에서 집계(aggregate)하는 것은 운영자(operator)가 외부 모니터링 시스템을 통해 수행해야 합니다.

---

## 메트릭 타입(Metric Types)

모든 메트릭은 다음 타입 중 하나로 분류됩니다.

| 타입(Type) | 설명 |
|------|------|
| **Gauge(게이지)** | 어떤 값에 대한 순간적인(instantaneous) 측정값입니다. |
| **Counter(카운터)** | `AtomicLong` 인스턴스에 대한 게이지로, 모니터링 시스템이 마지막 호출 이후의 변화량(change)을 소비(consume)합니다. |
| **Histogram(히스토그램)** | 값들의 통계적 분포(statistical distribution)를 측정하며, 최소(min), 최대(max), 평균(mean), 중앙값(median), 75번째, 90번째, 95번째, 98번째, 99번째, 99.9번째 백분위수(percentile)를 포함합니다. |
| **Timer(타이머)** | 코드 실행 속도(rate)와 그 실행 시간(duration)에 대한 히스토그램을 모두 측정합니다. |
| **Latency(레이턴시)** | 레이턴시(마이크로초 단위)를 추적하는 특수 타입으로, Timer에 더해 누적된 총 레이턴시(total accrued latency)를 추적하는 Counter를 함께 제공합니다. |
| **Meter(미터)** | 평균 처리량(mean throughput)과 1분, 5분, 15분 단위의 지수 가중 이동 평균(exponentially-weighted moving average)을 측정합니다. |

---

## 테이블 메트릭(Table Metrics)

각 테이블(table)에 대해 다음과 같은 메트릭이 수집됩니다.

**형식(Format):**
- 메트릭 이름: `org.apache.cassandra.metrics.Table.<MetricName>.<Keyspace>.<Table>`
- JMX MBean: `org.apache.cassandra.metrics:type=Table keyspace=<Keyspace> scope=<Table> name=<MetricName>`

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| MemtableOnHeapSize | Gauge<Long> | 컬럼 오버헤드(column overhead)와 덮어쓰여진(overwritten) 파티션을 포함한, 온힙(on-heap) 멤테이블의 총 데이터입니다. |
| MemtableOffHeapSize | Gauge<Long> | 컬럼 오버헤드와 덮어쓰여진 파티션을 포함한, 오프힙(off-heap) 멤테이블의 총 데이터입니다. |
| MemtableLiveDataSize | Gauge<Long> | 자료 구조 오버헤드(data structure overhead)를 제외한, 멤테이블 내 총 라이브(live) 데이터입니다. |
| AllMemtablesOnHeapSize | Gauge<Long> | 모든 멤테이블(보조 인덱스(2i) 및 플러시 대기 중인 것 포함)의 온힙 총 데이터입니다. |
| AllMemtablesOffHeapSize | Gauge<Long> | 모든 멤테이블(보조 인덱스(2i) 및 플러시 대기 중인 것 포함)의 오프힙 총 데이터입니다. |
| AllMemtablesLiveDataSize | Gauge<Long> | 오버헤드를 제외한, 모든 멤테이블의 총 라이브 데이터입니다. |
| MemtableColumnsCount | Gauge<Long> | 멤테이블 내 컬럼의 총 개수입니다. |
| MemtableSwitchCount | Counter | 플러시(flush)가 멤테이블 교체(switch)를 유발한 횟수입니다. |
| CompressionRatio | Gauge<Double> | 모든 SSTable에 대한 현재 압축률(compression ratio)입니다. |
| EstimatedPartitionSizeHistogram | Gauge<long[]> | 추정 파티션 크기(바이트 단위)의 히스토그램입니다. |
| EstimatedPartitionCount | Gauge<Long> | 테이블 내 대략적인 키(key) 개수입니다. |
| EstimatedColumnCountHistogram | Gauge<long[]> | 추정 컬럼 개수의 히스토그램입니다. |
| SSTablesPerReadHistogram | Histogram | 파티션 읽기당 접근한 SSTable 파일의 수입니다. |
| ReadLatency | Latency | 이 테이블에 대한 로컬 읽기 레이턴시입니다. |
| RangeLatency | Latency | 이 테이블에 대한 로컬 범위 스캔(range scan) 레이턴시입니다. |
| WriteLatency | Latency | 이 테이블에 대한 로컬 쓰기 레이턴시입니다. |
| CoordinatorReadLatency | Timer | 이 테이블에 대한 코디네이터(coordinator) 읽기 레이턴시입니다. |
| CoordinatorWriteLatency | Timer | 이 테이블에 대한 코디네이터 쓰기 레이턴시입니다. |
| CoordinatorScanLatency | Timer | 이 테이블에 대한 코디네이터 범위 스캔 레이턴시입니다. |
| PendingFlushes | Counter | 이 테이블에 대한 추정 대기 중(pending) 플러시 작업 수입니다. |
| BytesFlushed | Counter | 서버 시작 이후 플러시된 총 바이트 수입니다. |
| CompactionBytesWritten | Counter | 서버 시작 이후 컴팩션(compaction)에 의해 쓰여진 총 바이트 수입니다. |
| PendingCompactions | Gauge<Integer> | 이 테이블에 대한 추정 대기 중 컴팩션 수입니다. |
| LiveSSTableCount | Gauge<Integer> | 이 테이블에 대해 디스크상에 존재하는 SSTable의 수입니다. |
| LiveDiskSpaceUsed | Counter | SSTable이 사용하는 디스크 공간(바이트)입니다. |
| TotalDiskSpaceUsed | Counter | 가비지 컬렉션(GC) 대기 중인 오래된(obsolete) SSTable을 포함한 총 디스크 공간입니다. |
| MaxSSTableSize | Gauge<Long> | 디스크상의 최대 물리적 SSTable 크기(바이트)입니다. |
| MaxSSTableDuration | Gauge<Long> | 최대 SSTable 지속 시간(maxTimestamp - minTimestamp), 밀리초 단위입니다. |
| MinPartitionSize | Gauge<Long> | 가장 작은 컴팩션된 파티션의 크기(바이트)입니다. |
| MaxPartitionSize | Gauge<Long> | 가장 큰 컴팩션된 파티션의 크기(바이트)입니다. |
| MeanPartitionSize | Gauge<Long> | 평균 컴팩션된 파티션의 크기(바이트)입니다. |
| BloomFilterFalsePositives | Gauge<Long> | 이 테이블의 블룸 필터(bloom filter)에서 발생한 거짓 양성(false positive)의 수입니다. |
| BloomFilterFalseRatio | Gauge<Double> | 이 테이블의 블룸 필터의 거짓 양성 비율입니다. |
| BloomFilterDiskSpaceUsed | Gauge<Long> | 블룸 필터가 사용하는 디스크 공간(바이트)입니다. |
| BloomFilterOffHeapMemoryUsed | Gauge<Long> | 블룸 필터가 사용하는 오프힙 메모리입니다. |
| IndexSummaryOffHeapMemoryUsed | Gauge<Long> | 인덱스 요약(index summary)이 사용하는 오프힙 메모리입니다. |
| CompressionMetadataOffHeapMemoryUsed | Gauge<Long> | 압축 메타데이터(compression metadata)가 사용하는 오프힙 메모리입니다. |
| KeyCacheHitRate | Gauge<Double> | 이 테이블에 대한 키 캐시(key cache) 적중률(hit rate)입니다. |
| TombstoneScannedHistogram | Histogram | 쿼리 중 스캔된 툼스톤(tombstone)의 히스토그램입니다. |
| TombstoneWarnings | Counter | `tombstone_warn_threshold`를 초과한 읽기 수입니다. |
| TombstoneFailures | Counter | `tombstone_failure_threshold`를 초과한 읽기 수입니다. |
| LiveScannedHistogram | Histogram | 쿼리 중 스캔된 라이브 셀(live cell)의 히스토그램입니다. |
| ColUpdateTimeDeltaHistogram | Histogram | 컬럼 갱신 시간 델타(time delta)의 히스토그램입니다. |
| ViewLockAcquireTime | Timer | 구체화된 뷰(materialized view) 갱신을 위해 파티션 잠금(lock)을 획득하는 데 걸린 시간입니다. |
| ViewReadTime | Timer | 구체화된 뷰 갱신 시 로컬 읽기에 걸린 시간입니다. |
| TrueSnapshotsSize | Gauge<Long> | 모든 SSTable 구성요소를 포함한 스냅샷(snapshot)이 사용하는 디스크 공간입니다. |
| RowCacheHitOutOfRange | Counter | 쿼리 필터를 충족하지 못한 로우 캐시(row cache) 적중 수입니다. |
| RowCacheHit | Counter | 로우 캐시 적중 횟수입니다. |
| RowCacheMiss | Counter | 로우 캐시 미스(miss) 횟수입니다. |
| CasPrepare | Latency | Paxos prepare 라운드의 레이턴시입니다. |
| CasPropose | Latency | Paxos propose 라운드의 레이턴시입니다. |
| CasCommit | Latency | Paxos commit 라운드의 레이턴시입니다. |
| PercentRepaired | Gauge<Double> | 디스크상에서 복구(repair)된 테이블 데이터의 비율(퍼센트)입니다. |
| BytesRepaired | Gauge<Long> | 디스크상에서 복구된 테이블 데이터의 크기입니다. |
| BytesUnrepaired | Gauge<Long> | 디스크상에서 복구되지 않은 테이블 데이터의 크기입니다. |
| BytesPendingRepair | Gauge<Long> | 진행 중인 증분 복구(incremental repair)를 위해 격리된 테이블 데이터의 크기입니다. |
| SpeculativeRetries | Counter | 이 테이블에 대해 전송된 추측성 재시도(speculative retry)의 수입니다. |
| SpeculativeFailedRetries | Counter | 타임아웃 방지에 실패한 추측성 재시도의 수입니다. |
| SpeculativeInsufficientReplicas | Counter | 레플리카(replica) 부족으로 시도되지 않은 추측성 재시도의 수입니다. |
| SpeculativeSampleLatencyNanos | Gauge<Long> | 추측(speculation)을 시도하기 전 대기하는 시간(나노초)입니다. |
| AnticompactionTime | Timer | 일관된(consistent) 복구 전 안티컴팩션(anticompaction)에 소요된 시간입니다. |
| ValidationTime | Timer | 복구 중 검증 컴팩션(validation compaction)에 소요된 시간입니다. |
| SyncTime | Timer | 복구 중 스트리밍(streaming)에 소요된 시간입니다. |
| BytesValidated | Histogram | 검증 중 읽은 바이트의 히스토그램입니다. |
| PartitionsValidated | Histogram | 검증 중 읽은 파티션의 히스토그램입니다. |
| BytesAnticompacted | Counter | 안티컴팩션된 바이트 수입니다. |
| BytesMutatedAnticompaction | Counter | 완전 포함(full containment)으로 인해 안티컴팩션을 회피한 바이트 수입니다. |
| MutatedAnticompactionGauge | Gauge<Double> | 변경된(mutated) 바이트 대 복구된 총 바이트의 비율입니다. |

---

## 키스페이스 메트릭(Keyspace Metrics)

각 키스페이스(keyspace)에 대해 다음과 같은 메트릭이 수집됩니다.

**형식(Format):**
- 메트릭 이름: `org.apache.cassandra.metrics.keyspace.<MetricName>.<Keyspace>`
- JMX MBean: `org.apache.cassandra.metrics:type=Keyspace scope=<Keyspace> name=<MetricName>`

**참고:** 대부분의 메트릭은 테이블 메트릭(Table Metrics)의 집계(aggregation)입니다. 다음은 키스페이스에 고유한 메트릭입니다.

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| WriteFailedIdeaCL | Counter | 구성된 이상적 일관성 수준(ideal consistency level)을 달성하지 못한 쓰기의 수입니다. |
| IdealCLWriteLatency | Latency | 구성된 이상적 일관성 수준에서의 코디네이터 레이턴시입니다. |
| RepairTime | Timer | 복구 코디네이터(repair coordinator)로서 소요된 총 시간입니다. |
| RepairPrepareTime | Timer | 복구를 준비(prepare)하는 데 소요된 총 시간입니다. |

---

## 스레드 풀 메트릭(ThreadPool Metrics)

Cassandra는 스테이지드 이벤트 기반 아키텍처(SEDA, Staged Event-Driven Architecture)를 사용하므로, 여러 스레드 풀(thread pool)에 대해 다음과 같은 메트릭이 수집됩니다.

**형식(Format):**
- 메트릭 이름: `org.apache.cassandra.metrics.ThreadPools.<MetricName>.<Path>.<ThreadPoolName>`
- JMX MBean: `org.apache.cassandra.metrics:type=ThreadPools path=<Path> scope=<ThreadPoolName> name=<MetricName>`

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| ActiveTasks | Gauge<Integer> | 이 풀이 현재 작업 중인 태스크(task)의 수입니다. |
| PendingTasks | Gauge<Integer> | 이 풀에 큐(queue)에 들어가 있는 태스크의 수입니다. |
| CompletedTasks | Counter | 완료된 태스크의 총 수입니다. |
| TotalBlockedTasks | Counter | 큐 포화(saturation)로 인해 차단된(blocked) 태스크의 수입니다. |
| CurrentlyBlockedTask | Counter | 현재 차단되어 있지만 재시도 가능한(retryable) 태스크의 수입니다. |
| MaxPoolSize | Gauge<Integer> | 이 풀의 최대 스레드 수입니다. |
| MaxTasksQueued | Gauge<Integer> | 차단되기 전 큐에 들어갈 수 있는 최대 태스크 수입니다. |

다음 스레드 풀 각각에 대해 위 메트릭이 수집됩니다.

**전송(Transport) 스레드 풀:**

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| Native-Transport-Requests | transport | 클라이언트 CQL 요청을 처리합니다. |

**요청(Request) 스레드 풀:**

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| CounterMutationStage | request | 카운터(counter) 쓰기를 담당합니다. |
| ViewMutationStage | request | 구체화된 뷰(materialized view) 쓰기를 담당합니다. |
| MutationStage | request | 그 외 모든 쓰기를 담당합니다. |
| ReadRepairStage | request | 읽기 복구(ReadRepair)를 실행하는 스레드 풀입니다. |
| ReadStage | request | 로컬 읽기를 담당하는 스레드 풀입니다. |
| RequestResponseStage | request | 클러스터로 향하는 코디네이터 요청을 담당합니다. |

**내부(Internal) 스레드 풀:**

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| AntiEntropyStage | internal | 복구를 위한 머클 트리(merkle tree)를 구축합니다. |
| CacheCleanupExecutor | internal | 캐시 유지보수를 수행합니다. |
| CompactionExecutor | internal | 컴팩션을 실행합니다. |
| GossipStage | internal | 가십(gossip) 요청을 처리합니다. |
| HintsDispatcher | internal | 힌티드 핸드오프(hinted handoff)를 수행합니다. |
| InternalResponseStage | internal | 클러스터 내부(intra-cluster) 콜백을 처리합니다. |
| MemtableFlushWriter | internal | 멤테이블을 디스크에 씁니다. |
| MemtablePostFlush | internal | 플러시 후 커밋 로그(commit log) 정리를 수행합니다. |
| MemtableReclaimMemory | internal | 멤테이블 재활용(recycling)을 수행합니다. |
| MigrationStage | internal | 스키마 마이그레이션(schema migration)을 수행합니다. |
| MiscStage | internal | 기타 잡다한 태스크를 처리합니다. |
| PendingRangeCalculator | internal | 토큰 범위(token range)를 계산합니다. |
| PerDiskMemtableFlushWriter_0 | internal | 디스크별 사양에 따른 쓰기를 수행합니다(0번부터 N번까지). |
| Sampler | internal | SSTable 인덱스 요약(index summary)을 재샘플링합니다. |
| SecondaryIndexManagement | internal | 보조 인덱스(secondary index)를 갱신합니다. |
| ValidationExecutor | internal | 검증 컴팩션(validation compaction) 및 스크럽(scrubbing)을 수행합니다. |
| ViewBuildExecutor | internal | 구체화된 뷰의 초기 빌드(initial build)를 수행합니다. |

---

## 클라이언트 요청 메트릭(Client Request Metrics)

클라이언트 요청 메트릭은 코디네이터 노드가 처리하는 클라이언트 요청에 대한 통계를 노출합니다.

**형식(Format):**
- 메트릭 이름: `org.apache.cassandra.metrics.ClientRequest.<MetricName>.<RequestType>`
- JMX MBean: `org.apache.cassandra.metrics:type=ClientRequest scope=<RequestType> name=<MetricName>`

### CASRead 메트릭 (경량 트랜잭션 읽기)

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| Timeouts | Counter | 발생한 타임아웃(timeout) 수입니다. |
| Failures | Counter | 발생한 트랜잭션 실패 수입니다. |
| Latency | Latency | 트랜잭션 읽기 레이턴시입니다. |
| Unavailables | Counter | 발생한 사용 불가(unavailable) 예외 수입니다. |
| UnfinishedCommit | Counter | 읽기 중 커밋된 트랜잭션 수입니다. |
| ConditionNotMet | Counter | 사전 조건(precondition)이 일치하지 않은 트랜잭션 수입니다. |
| ContentionHistogram | Histogram | 경합이 발생한(contended) 읽기의 분포입니다. |

### CASWrite 메트릭 (경량 트랜잭션 쓰기)

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| Timeouts | Counter | 발생한 타임아웃 수입니다. |
| Failures | Counter | 발생한 트랜잭션 실패 수입니다. |
| Latency | Latency | 트랜잭션 쓰기 레이턴시입니다. |
| Unavailables | Counter | 발생한 사용 불가 예외 수입니다. |
| UnfinishedCommit | Counter | 쓰기 중 커밋된 트랜잭션 수입니다. |
| ConditionNotMet | Counter | 사전 조건이 일치하지 않은 트랜잭션 수입니다. |
| ContentionHistogram | Histogram | 경합이 발생한 쓰기의 분포입니다. |
| MutationSizeHistogram | Histogram | 변경(mutation)의 총 크기(바이트)입니다. |

### Read 메트릭 (읽기)

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| Timeouts | Counter | 발생한 타임아웃 수입니다. |
| Failures | Counter | 발생한 읽기 실패 수입니다. |
| Latency | Latency | 읽기 레이턴시입니다. |
| Unavailables | Counter | 발생한 사용 불가 예외 수입니다. |

### RangeSlice 메트릭 (범위 쿼리)

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| Timeouts | Counter | 발생한 타임아웃 수입니다. |
| Failures | Counter | 발생한 범위 쿼리(range query) 실패 수입니다. |
| Latency | Latency | 범위 쿼리 레이턴시입니다. |
| Unavailables | Counter | 발생한 사용 불가 예외 수입니다. |

### Write 메트릭 (쓰기)

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| Timeouts | Counter | 발생한 타임아웃 수입니다. |
| Failures | Counter | 발생한 쓰기 실패 수입니다. |
| Latency | Latency | 쓰기 레이턴시입니다. |
| Unavailables | Counter | 발생한 사용 불가 예외 수입니다. |
| MutationSizeHistogram | Histogram | 변경의 총 크기(바이트)입니다. |

### ViewWrite 메트릭 (구체화된 뷰 쓰기)

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| Timeouts | Counter | 발생한 타임아웃 수입니다. |
| Failures | Counter | 발생한 트랜잭션 실패 수입니다. |
| Unavailables | Counter | 발생한 사용 불가 예외 수입니다. |
| ViewReplicasAttempted | Counter | 시도된 뷰 레플리카 쓰기의 수입니다. |
| ViewReplicasSuccess | Counter | 성공한 뷰 레플리카 쓰기의 수입니다. |
| ViewPendingMutations | Gauge<Long> | 시도된 쓰기 수에서 성공한 쓰기 수를 뺀 값입니다. |
| ViewWriteLatency | Timer | 베이스 테이블(base table) 변경 시점과 뷰에 대한 CL.ONE 사이의 시간입니다. |

---

## 캐시 메트릭(Cache Metrics)

Cassandra는 여러 종류의 캐시(cache)에 대해 다음 메트릭을 수집합니다.

**형식(Format):**
- 메트릭 이름: `org.apache.cassandra.metrics.Cache.<MetricName>.<CacheName>`
- JMX MBean: `org.apache.cassandra.metrics:type=Cache scope=<CacheName> name=<MetricName>`

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| Capacity | Gauge<Long> | 캐시 용량(바이트)입니다. |
| Entries | Gauge<Integer> | 캐시 엔트리(entry)의 총 개수입니다. |
| FifteenMinuteCacheHitRate | Gauge<Double> | 15분 단위 캐시 적중률입니다. |
| FiveMinuteCacheHitRate | Gauge<Double> | 5분 단위 캐시 적중률입니다. |
| OneMinuteCacheHitRate | Gauge<Double> | 1분 단위 캐시 적중률입니다. |
| HitRate | Gauge<Double> | 전체 기간(all-time) 캐시 적중률입니다. |
| Hits | Meter | 캐시 적중의 총 횟수입니다. |
| Misses | Meter | 캐시 미스의 총 횟수입니다. |
| MissLatency | Timer | 미스의 레이턴시입니다. |
| Requests | Gauge<Long> | 캐시 요청의 총 횟수입니다. |
| Size | Gauge<Long> | 점유된(occupied) 캐시 크기(바이트)입니다. |

**캐시 종류:**

| 이름(Name) | 설명 |
|------|------|
| CounterCache | 성능을 위해 인기 있는(hot) 카운터를 메모리에 유지합니다. |
| ChunkCache | 프로세스 내(in-process) 비압축(uncompressed) 페이지 캐시입니다. |
| KeyCache | 파티션을 SSTable 오프셋(offset)으로 매핑하는 캐시입니다. |
| RowCache | 메모리에 유지되는 로우(row)에 대한 캐시입니다. |

> **참고:** `Misses`와 `MissLatency`는 ChunkCache에 대해서만 정의됩니다.

---

## CQL 메트릭(CQL Metrics)

CQL 실행 관련 메트릭을 노출합니다.

**형식(Format):**
- 메트릭 이름: `org.apache.cassandra.metrics.CQL.<MetricName>`
- JMX MBean: `org.apache.cassandra.metrics:type=CQL name=<MetricName>`

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| PreparedStatementsCount | Gauge<Integer> | 캐시된 준비된 구문(prepared statement)의 개수입니다. |
| PreparedStatementsEvicted | Counter | 캐시에서 축출(evict)된 준비된 구문의 수입니다. |
| PreparedStatementsExecuted | Counter | 실행된 준비된 구문의 수입니다. |
| RegularStatementsExecuted | Counter | 실행된 비준비(non-prepared) 구문의 수입니다. |
| PreparedStatementsRatio | Gauge<Double> | 준비된 구문 대 비준비 구문의 비율(퍼센트)입니다. |

---

## 드롭된 메시지 메트릭(DroppedMessage Metrics)

내부 타임아웃으로 인해 드롭된(dropped) 메시지를 추적합니다. 이 메트릭은 노드 또는 클러스터에 부하가 걸려 있는지를 진단하는 데 유용합니다.

**형식(Format):**
- 메트릭 이름: `org.apache.cassandra.metrics.DroppedMessage.<MetricName>.<Type>`
- JMX MBean: `org.apache.cassandra.metrics:type=DroppedMessage scope=<Type> name=<MetricName>`

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| CrossNodeDroppedLatency | Timer | 노드 간(cross-node) 드롭 레이턴시입니다. |
| InternalDroppedLatency | Timer | 노드 내부(within node) 드롭 레이턴시입니다. |
| Dropped | Meter | 드롭된 메시지의 수입니다. |

**메시지 타입:**

| 이름(Name) | 설명 |
|------|------|
| BATCH_STORE | 배치로그(batchlog) 쓰기입니다. |
| BATCH_REMOVE | 성공적으로 적용된 후 수행되는 배치로그 정리입니다. |
| COUNTER_MUTATION | 카운터 쓰기입니다. |
| HINT | 힌트(hint) 재생(replay)입니다. |
| MUTATION | 일반 쓰기입니다. |
| READ | 일반 읽기입니다. |
| READ_REPAIR | 읽기 복구(read repair)입니다. |
| PAGED_SLICE | 페이지 단위(paged) 읽기입니다. |
| RANGE_SLICE | 토큰 범위(token range) 읽기입니다. |
| REQUEST_RESPONSE | RPC 콜백(callback)입니다. |
| _TRACE | 트레이싱(tracing) 쓰기입니다. |

---

## 스트리밍 메트릭(Streaming Metrics)

노드 간 스트리밍 작업(부트스트랩, 복구, 디커미션 등) 동안 각 피어(peer)별로 수집되는 메트릭입니다.

**형식(Format):**
- 메트릭 이름: `org.apache.cassandra.metrics.Streaming.<MetricName>.<PeerIP>`
- JMX MBean: `org.apache.cassandra.metrics:type=Streaming scope=<PeerIP> name=<MetricName>`

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| IncomingBytes | Counter | 피어로부터 이 노드로 스트리밍된 바이트 수입니다. |
| OutgoingBytes | Counter | 이 노드로부터 피어로 스트리밍된 바이트 수입니다. |

---

## 컴팩션 메트릭(Compaction Metrics)

컴팩션 활동에 대한 메트릭을 노출합니다.

**형식(Format):**
- 메트릭 이름: `org.apache.cassandra.metrics.Compaction.<MetricName>`
- JMX MBean: `org.apache.cassandra.metrics:type=Compaction name=<MetricName>`

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| BytesCompacted | Counter | 서버 시작 이후 컴팩션된 총 바이트 수입니다. |
| PendingTasks | Gauge<Integer> | 추정 남은 컴팩션 수입니다. |
| CompletedTasks | Gauge<Long> | 서버 시작 이후 완료된 컴팩션 수입니다. |
| TotalCompactionsCompleted | Meter | 완료된 컴팩션의 처리량(throughput)입니다. |
| PendingTasksByTableName | Gauge<Map<String, Map<String, Integer>>> | 키스페이스 및 테이블별 대기 중 컴팩션 수입니다. |

---

## 커밋로그 메트릭(CommitLog Metrics)

커밋 로그(commit log)에 대한 메트릭을 노출합니다.

**형식(Format):**
- 메트릭 이름: `org.apache.cassandra.metrics.CommitLog.<MetricName>`
- JMX MBean: `org.apache.cassandra.metrics:type=CommitLog name=<MetricName>`

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| CompletedTasks | Gauge<Long> | 재시작 이후 쓰여진 커밋 로그 메시지의 수입니다. |
| PendingTasks | Gauge<Long> | fsync를 대기 중인 커밋 로그 메시지의 수입니다. |
| TotalCommitLogSize | Gauge<Long> | 모든 커밋 로그 세그먼트(segment)의 현재 크기(바이트)입니다. |
| WaitingOnSegmentAllocation | Timer | CommitLogSegment 할당(allocation)을 대기한 시간입니다. |
| WaitingOnCommit | Timer | CommitLog의 fsync를 대기한 시간입니다. |

---

## 스토리지 메트릭(Storage Metrics)

노드 전역의 스토리지 관련 메트릭을 노출합니다.

**형식(Format):**
- 메트릭 이름: `org.apache.cassandra.metrics.Storage.<MetricName>`
- JMX MBean: `org.apache.cassandra.metrics:type=Storage name=<MetricName>`

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| Exceptions | Counter | 포착된 내부 예외(internal exception)의 수입니다. |
| Load | Counter | 이 노드가 관리하는 디스크상 데이터 크기(바이트)입니다. |
| TotalHints | Counter | 재시작 이후 쓰여진 힌트 메시지의 수입니다. |
| TotalHintsInProgress | Counter | 현재 전송 중인 힌트의 수입니다. |

---

## 힌티드 핸드오프 메트릭(HintedHandoff Metrics)

힌티드 핸드오프 매니저(Hinted Handoff Manager)에 대한 메트릭을 노출합니다.

**형식(Format):**
- 메트릭 이름: `org.apache.cassandra.metrics.HintedHandOffManager.<MetricName>`
- JMX MBean: `org.apache.cassandra.metrics:type=HintedHandOffManager name=<MetricName>`

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| Hints_created-<PeerIP> | Counter | 이 피어에 대해 디스크에 저장된 힌트의 수입니다. |
| Hints_not_stored-<PeerIP> | Counter | 피어의 다운타임(downtime)이 힌트 윈도우(hint window)를 초과하여 저장되지 못한 힌트의 수입니다. |

---

## 힌트 서비스 메트릭(HintsService Metrics)

힌트 서비스(Hints Service)에 대한 메트릭을 노출합니다.

**형식(Format):**
- 메트릭 이름: `org.apache.cassandra.metrics.HintsService.<MetricName>`
- JMX MBean: `org.apache.cassandra.metrics:type=HintsService name=<MetricName>`

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| HintsSucceeded | Meter | 성공적으로 전달된 힌트의 수입니다. |
| HintsFailed | Meter | 전달에 실패한 힌트의 수입니다. |
| HintsTimedOut | Meter | 타임아웃된 힌트의 수입니다. |
| Hint_delays | Histogram | 힌트 전달 지연(delay)의 히스토그램(밀리초 단위)입니다. |
| Hint_delays-<PeerIP> | Histogram | 피어별 힌트 전달 지연의 히스토그램(밀리초 단위)입니다. |

---

## SSTable 인덱스 메트릭(SSTable Index Metrics)

SSTable 내 로우 인덱스 엔트리(row index entry)에 대한 메트릭을 노출합니다.

**형식(Format):**
- 메트릭 이름: `org.apache.cassandra.metrics.Index.<MetricName>.RowIndexEntry`
- JMX MBean: `org.apache.cassandra.metrics:type=Index scope=RowIndexEntry name=<MetricName>`

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| IndexedEntrySize | Histogram | 모든 SSTable에 걸친 온힙 인덱스 크기(바이트)입니다. |
| IndexInfoCount | Histogram | 모든 SSTable에 걸쳐 관리되는 온힙 인덱스 엔트리의 수입니다. |
| IndexInfoGets | Histogram | SSTable당 수행된 인덱스 탐색(seek)의 수입니다. |

---

## 버퍼 풀 메트릭(BufferPool Metrics)

Cassandra의 버퍼 풀(buffer pool)에 대한 메트릭을 노출합니다.

**형식(Format):**
- 메트릭 이름: `org.apache.cassandra.metrics.BufferPool.<MetricName>`
- JMX MBean: `org.apache.cassandra.metrics:type=BufferPool name=<MetricName>`

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| Size | Gauge<Long> | 관리되는 버퍼 풀의 크기(바이트)입니다. |
| Misses | Meter | 풀 미스(miss)의 비율로, 발생한 할당(allocation)을 나타냅니다. |

---

## 클라이언트 메트릭(Client Metrics)

연결된 클라이언트(client)에 대한 메트릭을 노출합니다.

**형식(Format):**
- 메트릭 이름: `org.apache.cassandra.metrics.Client.<MetricName>`
- JMX MBean: `org.apache.cassandra.metrics:type=Client name=<MetricName>`

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| connectedNativeClients | Gauge<Integer> | 네이티브 프로토콜(native protocol) 서버에 연결된 클라이언트의 수입니다. |
| connections | Gauge<List<Map<String, String>>> | 모든 연결과 그 상태 정보(state information)입니다. |
| connectedNativeClientsByUser | Gauge<Map<String, Int>> | 사용자 이름(username)별로 연결된 네이티브 클라이언트의 수입니다. |

---

## 배치 메트릭(Batch Metrics)

배치(batch) 구문에 대한 메트릭을 노출합니다.

**형식(Format):**
- 메트릭 이름: `org.apache.cassandra.metrics.Batch.<MetricName>`
- JMX MBean: `org.apache.cassandra.metrics:type=Batch name=<MetricName>`

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| PartitionsPerCounterBatch | Histogram | 카운터 배치당 처리된 파티션의 분포입니다. |
| PartitionsPerLoggedBatch | Histogram | 로깅된(logged) 배치당 처리된 파티션의 분포입니다. |
| PartitionsPerUnloggedBatch | Histogram | 로깅되지 않은(unlogged) 배치당 처리된 파티션의 분포입니다. |

---

## JVM 메트릭(JVM Metrics)

메모리와 가비지 컬렉션(garbage collection)에 대한 JVM 메트릭은 JMX 또는 메트릭 리포터를 통해 접근할 수 있습니다.

### BufferPool (JVM 버퍼 풀)

**형식(Format):**
- 메트릭 이름: `jvm.buffers.<direct|mapped>.<MetricName>`
- JMX MBean: `java.nio:type=BufferPool name=<direct|mapped>`

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| Capacity | Gauge<Long> | 추정 버퍼 풀 총 용량입니다. |
| Count | Gauge<Long> | 풀 내 추정 버퍼 개수입니다. |
| Used | Gauge<Long> | 버퍼 풀이 사용하는 추정 메모리입니다. |

### FileDescriptorRatio (파일 디스크립터 비율)

**형식(Format):**
- 메트릭 이름: `jvm.fd.<MetricName>`
- JMX MBean: `java.lang:type=OperatingSystem name=<OpenFileDescriptorCount|MaxFileDescriptorCount>`

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| Usage | Ratio | 사용 중인 파일 디스크립터 대 전체 파일 디스크립터의 비율입니다. |

### GarbageCollector (가비지 컬렉터)

**형식(Format):**
- 메트릭 이름: `jvm.gc.<gc_type>.<MetricName>`
- JMX MBean: `java.lang:type=GarbageCollector name=<gc_type>`

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| Count | Gauge<Long> | 발생한 컬렉션(collection)의 총 횟수입니다. |
| Time | Gauge<Long> | 누적된 컬렉션 경과 시간의 근사치(밀리초)입니다. |

### Memory (메모리)

**형식(Format):**
- 메트릭 이름: `jvm.memory.<heap/non-heap/total>.<MetricName>`
- JMX MBean: `java.lang:type=Memory`

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| Committed | Gauge<Long> | JVM 사용을 위해 커밋된(committed) 메모리(바이트)입니다. |
| Init | Gauge<Long> | OS로부터 최초 요청한 메모리(바이트)입니다. |
| Max | Gauge<Long> | 관리를 위한 최대 메모리(바이트)입니다. |
| Usage | Ratio | 사용된 메모리 대 최대 메모리의 비율입니다. |
| Used | Gauge<Long> | 사용된 메모리(바이트)입니다. |

### MemoryPool (메모리 풀)

**형식(Format):**
- 메트릭 이름: `jvm.memory.pools.<memory_pool>.<MetricName>`
- JMX MBean: `java.lang:type=MemoryPool name=<memory_pool>`

| 이름(Name) | 타입(Type) | 설명 |
|------|------|------|
| Committed | Gauge<Long> | JVM 사용을 위해 커밋된 메모리(바이트)입니다. |
| Init | Gauge<Long> | OS로부터 최초 요청한 메모리(바이트)입니다. |
| Max | Gauge<Long> | 관리를 위한 최대 메모리(바이트)입니다. |
| Usage | Ratio | 사용된 메모리 대 최대 메모리의 비율입니다. |
| Used | Gauge<Long> | 사용된 메모리(바이트)입니다. |

---

## 메트릭 접근 방법

### JMX 접근

모든 JMX 기반 클라이언트는 Cassandra의 메트릭에 접근할 수 있습니다. JMX 메트릭에 HTTP로 접근하려면 **Mx4jTool**을 다운로드하여 클래스패스(classpath)에 배치하면 됩니다. 정상적으로 설정되면 시작 시 다음과 같은 로그가 출력됩니다.

```
HttpAdaptor version 3.0.2 started on port 8081
```

포트(port)와 주소(address) 설정은 `cassandra-env.sh` 파일에서 `MX4J_ADDRESS` 및 `MX4J_PORT` 옵션을 통해 구성할 수 있습니다.

### 메트릭 리포터(Metric Reporters)

Cassandra의 메트릭은 Dropwizard Metrics 라이브러리의 **내장 리포터(built-in reporters)** 또는 **서드파티 리포터 플러그인(third-party reporter plug-ins)** 을 사용하여 외부 모니터링 시스템으로 내보낼(export) 수 있습니다. 이를 통해 Graphite, Prometheus 등 다양한 모니터링 도구와 연동할 수 있습니다.

---

## 가상 테이블을 통한 모니터링(Virtual Tables)

> 원본: https://cassandra.apache.org/doc/latest/cassandra/managing/operating/virtualtables.html

### 개요

가상 테이블(Virtual Tables)은 Apache Cassandra 4.0에서 도입된 기능으로, **SSTable로 명시적으로 관리·저장되는 데이터 대신 API에 의해 뒷받침되는(backed by an API) 테이블**을 제공합니다. 이 테이블은 특수한 가상 키스페이스(virtual keyspace) 안에 존재하며, CQL 쿼리를 통해 운영 메트릭과 구성 정보를 노출합니다.

### 주요 특성

가상 테이블은 다음과 같은 고유한 속성을 가집니다.

- **읽기 전용(Read-only) 접근**: 사용자는 가상 테이블을 생성하는 DDL이나 수정하는 DML을 실행할 수 없습니다.
- **노드 로컬(Node-local)**: 클러스터 전체에 복제(replicate)되지 않으며, 각 노드가 자신의 가상 테이블을 유지합니다.
- **SSTable 없음**: 데이터는 전통적인 저장 파일이 아니라 API로부터 제공됩니다.
- **일관성 무시(Consistency ignored)**: 쿼리의 일관성 수준(consistency level)이 적용되지 않습니다.
- **LocalPartitioner**: 해시 값(hash value)이 아니라 파티션 키(partition key)를 기준으로 정렬합니다.
- **고급 쿼리 허용**: 일반적으로 권장되지 않는 `ALLOW FILTERING`과 집계(aggregation)도 실행할 수 있습니다.

### 가상 키스페이스(Virtual Keyspaces)

Cassandra는 두 개의 전용 키스페이스를 구현합니다.

1. **system_virtual_schema**: 가상 테이블의 스키마를 정의하는 메타데이터 테이블(`keyspaces`, `columns`, `tables`)을 포함합니다.
2. **system_views**: 실제로 쿼리 가능한 가상 테이블을 보관합니다.

### 가상 테이블의 제약 사항

문서에서 명시하는 제약은 다음과 같습니다.

- 가상 키스페이스는 변경(alter)하거나 삭제(drop)할 수 없습니다.
- 가상 테이블은 DDL을 통해 생성, 수정, 트렁케이트(truncate), 삭제할 수 없습니다.
- 보조 인덱스(secondary index), 타입(type), 함수(function), 집계(aggregate), 구체화된 뷰(materialized view), 트리거(trigger)는 지원되지 않습니다.
- TTL 컬럼은 생성할 수 없습니다.
- 조건부 갱신/삭제(conditional update/delete)는 금지됩니다.
- 가상 테이블의 변경(mutation)은 일반 테이블과 함께 배치 구문(batch statement)에 참여할 수 없습니다.

### 전체 가상 테이블 목록

| 테이블 이름 | 용도 |
|-----------|------|
| `caches` | 캐시 메트릭(chunks, counters, keys, rows)으로 용량, 적중률, 요청 속도를 포함합니다. |
| `cidr_filtering_metrics_counts` | CIDR 필터 접근의 허용/거부(acceptance/rejection) 횟수입니다. |
| `cidr_filtering_metrics_latencies` | CIDR 필터의 레이턴시 측정값(나노초 단위)입니다. |
| `clients` | 드라이버 정보, SSL 상태, 요청 횟수를 포함한 활성 클라이언트 연결입니다. |
| `coordinator_read_latency` | 코디네이터 수준의 읽기 작업 메트릭입니다. |
| `coordinator_scan` | 코디네이터 수준의 스캔 작업 메트릭입니다. |
| `coordinator_write_latency` | 코디네이터 수준의 쓰기 작업 메트릭입니다. |
| `cql_metrics` | 준비된 구문(prepared statement) 캐시 통계입니다. |
| `disk_usage` | 키스페이스 및 테이블별 스토리지 소비량입니다. |
| `internode_inbound` | 인바운드(inbound) 노드 간(internode) 메시징 통계입니다. |
| `internode_outbound` | 아웃바운드(outbound) 노드 간 메시징 통계입니다. |
| `local_read_latency` | 로컬 읽기 작업 성능 데이터입니다. |
| `local_scan` | 로컬 스캔 작업 성능 데이터입니다. |
| `local_write_latency` | 로컬 쓰기 작업 성능 데이터입니다. |
| `max_partition_size` | 최대 파티션 크기 메트릭입니다. |
| `rows_per_read` | 읽기 작업당 로우(row) 개수 통계입니다. |
| `settings` | 현재 `cassandra.yaml` 구성 값입니다. |
| `sstable_tasks` | 진행 중인 컴팩션/업그레이드 작업과 그 진행 상황입니다. |
| `system_logs` | CQLLOG 어펜더(appender)를 통한 Cassandra 로그입니다. |
| `system_properties` | JVM 시스템 속성(system properties)입니다. |
| `thread_pools` | 스레드 풀 메트릭(활성, 대기, 차단 태스크)입니다. |
| `tombstones_per_read` | 읽기당 툼스톤(tombstone) 개수 통계입니다. |

### 주요 가상 테이블 상세

#### clients 테이블

연결된 클라이언트를 나열하며, 주소(address), 포트(port), 드라이버 이름/버전, 프로토콜 버전(protocol version), 요청 횟수, SSL 정보 등의 컬럼을 포함합니다. 업그레이드 전에 오래된 드라이버를 식별하거나, 과도한 요청 패턴을 탐지하는 데 유용합니다.

#### caches 테이블

네 가지 캐시 타입(chunks, counters, keys, rows)에 대한 메트릭을 제공합니다. 각 항목은 용량(capacity), 엔트리 개수, 적중률(hit ratio), 최근 요청 속도(request rate)를 보여줍니다.

#### settings 테이블

모든 활성 `cassandra.yaml` 구성 매개변수를 노출합니다(암호화 옵션은 마스킹(mask)됩니다). 구성 파일에 직접 접근하지 않고도 런타임 구성을 확인할 때 유용합니다.

#### thread_pools 테이블

모든 스레드 풀의 세부 정보를 활성 태스크 수, 한계(limit), 완료된 태스크, 대기 작업과 함께 제공합니다. 병목(bottleneck)과 자원 제약을 식별하는 데 도움이 됩니다.

#### sstable_tasks 테이블

컴팩션과 같은 진행 중인 작업을 추적하며, 전체 작업량 대비 진행 상황을 바이트 단위로 보여줍니다. 장시간 실행되는 유지보수 작업을 모니터링할 수 있게 합니다.

### 사용 예시

**디스크 사용량이 높은 테이블 찾기:**

```sql
SELECT * FROM disk_usage WHERE mebibytes > 1 ALLOW FILTERING;
```

**읽기 레이턴시가 높은 테이블 식별:**

```sql
SELECT * FROM local_read_latency WHERE per_second > 1 ALLOW FILTERING;
```

**SSTable 작업의 남은 작업량 계산:**

```sql
SELECT total - progress AS remaining FROM system_views.sstable_tasks;
```

### 구현 참고 사항

CASSANDRA-18238에 따라, `system_logs`를 제외한 모든 가상 테이블은 CQL 명세상 필요할 때 `ALLOW FILTERING`을 자동으로 적용하여 사용성을 향상시킵니다.

가상 테이블은 `DESCRIBE TABLE` 구문으로 설명(describe)할 수 있지만, 그 결과로 나오는 DDL로는 가상 테이블을 재생성할 수 없습니다. 가상 테이블은 전적으로 Cassandra 내부에서 관리되기 때문입니다.

---

## 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [Cassandra Metrics 공식 문서](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/metrics.html)
- [Cassandra Virtual Tables 공식 문서](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/virtualtables.html)
- [Dropwizard Metrics](https://metrics.dropwizard.io/)

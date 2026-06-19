# Cassandra 컴팩션과 압축

> 이 문서는 Apache Cassandra 공식 문서의 "Compaction" 및 "Compression" 섹션을 한국어로 번역한 것입니다.
> 원본: https://cassandra.apache.org/doc/latest/cassandra/managing/operating/

---

## 목차

1. [컴팩션(Compaction) 개요](#1-컴팩션compaction-개요)
   - [컴팩션이란 무엇인가](#11-컴팩션이란-무엇인가)
   - [컴팩션이 필요한 이유](#12-컴팩션이-필요한-이유)
   - [컴팩션이 달성하는 것](#13-컴팩션이-달성하는-것)
   - [컴팩션 동작 원리](#14-컴팩션-동작-원리)
2. [컴팩션 작업의 종류](#2-컴팩션-작업의-종류)
3. [툼스톤(Tombstone)](#3-툼스톤tombstone)
   - [툼스톤이란 무엇인가](#31-툼스톤이란-무엇인가)
   - [툼스톤이 필요한 이유](#32-툼스톤이-필요한-이유)
   - [좀비(Zombie)](#33-좀비zombie)
   - [유예 기간(Grace Period)](#34-유예-기간grace-period)
   - [툼스톤 제거 조건](#35-툼스톤-제거-조건)
   - [완전히 만료된 SSTable](#36-완전히-만료된-sstable)
4. [TTL(Time-To-Live)](#4-ttltime-to-live)
5. [복구된/복구되지 않은 데이터(Repaired/Unrepaired Data)](#5-복구된복구되지-않은-데이터repairedunrepaired-data)
6. [데이터 디렉터리(Data Directories)](#6-데이터-디렉터리data-directories)
7. [단일 SSTable 툼스톤 컴팩션](#7-단일-sstable-툼스톤-컴팩션)
8. [모든 전략에 공통된 컴팩션 옵션](#8-모든-전략에-공통된-컴팩션-옵션)
9. [컴팩션 관련 nodetool 명령어](#9-컴팩션-관련-nodetool-명령어)
10. [JMX를 통한 전략 전환](#10-jmx를-통한-전략-전환)
11. [Size Tiered 컴팩션 전략(STCS)](#11-size-tiered-컴팩션-전략stcs)
12. [Leveled 컴팩션 전략(LCS)](#12-leveled-컴팩션-전략lcs)
13. [Time Window 컴팩션 전략(TWCS)](#13-time-window-컴팩션-전략twcs)
14. [압축(Compression)](#14-압축compression)
    - [압축이란 무엇인가](#141-압축이란-무엇인가)
    - [압축 알고리즘의 트레이드오프](#142-압축-알고리즘의-트레이드오프)
    - [사용 가능한 압축기(Compressor)](#143-사용-가능한-압축기compressor)
    - [공통 압축 옵션](#144-공통-압축-옵션)
    - [압축기별 전용 옵션](#145-압축기별-전용-옵션)
    - [압축 설정 예제(CQL)](#146-압축-설정-예제cql)
    - [압축 변경 적용 방법](#147-압축-변경-적용-방법)
    - [압축의 이점](#148-압축의-이점)
    - [적합한 사용 사례와 부적합한 사용 사례](#149-적합한-사용-사례와-부적합한-사용-사례)
    - [운영 시 고려사항](#1410-운영-시-고려사항)
15. [참고 자료](#15-참고-자료)

---

## 1. 컴팩션(Compaction) 개요

### 1.1 컴팩션이란 무엇인가

컴팩션(Compaction)은 Cassandra가 주기적으로 SSTable들을 병합(merge)하고 오래된 데이터를 폐기하는 과정입니다. 데이터는 멤테이블(memtable)에서 시작되며, 메모리 임계값(memory threshold)에 도달하면 데이터는 디스크상의 불변(immutable) SSTable 파일로 기록됩니다. SSTable은 수정할 수 없기 때문에, 업데이트(update)와 삭제(deletion)는 기존 데이터를 덮어쓰는 대신 갱신된 타임스탬프(timestamp)를 가진 새로운 SSTable을 생성합니다. 이러한 누적은 데이터베이스의 건강성(health)을 유지하기 위해 주기적인 병합을 필요로 합니다.

### 1.2 컴팩션이 필요한 이유

"SSTable은 읽기(read) 작업 중에 참조되기 때문에, SSTable의 개수를 작게 유지하는 것이 중요합니다(it is important to keep the number of SSTables small)." 쓰기(write) 작업은 SSTable의 수를 증가시키므로 컴팩션이 필수적입니다. 툼스톤(tombstone) 관리뿐만 아니라, 데이터 삭제는 TTL(Time-To-Live) 만료 및 명시적 삭제(explicit delete)를 통해서도 발생하며, 이 모든 것이 컴팩션의 필요성을 유발합니다.

### 1.3 컴팩션이 달성하는 것

두 가지 주요 이점이 발생합니다: 성능 향상(performance improvement)과 디스크 공간 회수(disk space reclamation)입니다. "SSTable에 읽어야 하는 중복 데이터(duplicate data)가 있으면 읽기 작업이 느려집니다. 일단 툼스톤과 중복이 제거되면 읽기 작업이 빨라집니다(If SSTables have duplicate data that must be read, read operations are slower. Once tombstones and duplicates are removed, read operations are faster)." 또한 컴팩션을 통한 SSTable 크기 감소를 통해 디스크 공간 절감도 이루어집니다.

### 1.4 컴팩션 동작 원리

"컴팩션은 SSTable들의 집합(collection)에 대해 동작합니다. 이 SSTable들로부터, 컴팩션은 각 고유 행(unique row)의 모든 버전을 수집하고, 가장 최신 버전(타임스탬프 기준)을 사용하여 하나의 완전한 행(one complete row)을 조립합니다(Compaction works on a collection of SSTables. From these SSTables, compaction collects all versions of each unique row and assembles one complete row, using the most up-to-date version (by timestamp))."

병합 과정은 성능이 우수한데(performant), 그 이유는 각 SSTable 내에서 행들이 파티션 키(partition key)로 정렬되어 있어 랜덤 I/O(random I/O)를 피할 수 있기 때문입니다. 새로운 행 버전은 새로운 SSTable에 기록되며, 오래된 버전은 대기 중인 읽기(pending reads)가 완료될 때까지 원본 SSTable에 남아 있습니다.

---

## 2. 컴팩션 작업의 종류

**마이너 컴팩션(Minor Compaction)** - 다음과 같은 경우에 자동으로 트리거됩니다: SSTable이 플러시(flush)될 때, 자동 컴팩션(autocompaction)이 다시 활성화될 때, 컴팩션이 새로운 SSTable을 추가할 때, 또는 5분마다 수행되는 자동 점검(automatic checks) 중에 트리거됩니다.

**메이저 컴팩션(Major Compaction)** - 노드의 모든 SSTable에 걸쳐 사용자가 실행하는(user-executed) 컴팩션입니다.

**사용자 정의 컴팩션(User-Defined Compaction)** - 특정 SSTable 집합에 대해 사용자가 트리거하는(user-triggered) 컴팩션입니다.

**스크럽(Scrub)** - SSTable 복구를 시도하는 컴팩션입니다. 손상된(corrupted) 유효 데이터를 제거할 가능성이 있으므로, 이후 전체 복구(full repair)가 필요합니다.

**UpgradeSSTables** - 메이저 버전 업그레이드(major version upgrade) 이후 SSTable을 업그레이드하는 컴팩션입니다.

**클린업(Cleanup)** - 노드가 더 이상 소유하지 않는 범위(range)를 제거하는 컴팩션으로, 일반적으로 부트스트래핑(bootstrapping) 이후 인접 노드(neighboring nodes)에서 트리거됩니다.

**보조 인덱스 재구축(Secondary Index Rebuild)** - 보조 인덱스(secondary index)를 재구축할 때 트리거되는 컴팩션입니다.

**안티컴팩션(Anticompaction)** - 복구(repair) 이후 복구된 범위(repaired ranges)를 원본 SSTable로부터 분리하는 컴팩션입니다.

**서브 레인지 컴팩션(Sub Range Compaction)** - `nodetool compact -st x -et y`를 사용하여 특정 토큰 범위(token range)를 대상으로 하는 컴팩션입니다. 과도한 업데이트나 삭제(excessive updates or deletes)로 오작동하는 토큰(misbehaving tokens)에 유용합니다.

### 컴팩션 전략 개요

**통합 컴팩션 전략(Unified Compaction Strategy, UCS)** - 대부분의 워크로드에 권장됩니다. 불변 시계열 데이터(immutable time-series data), 업데이트/삭제가 많은(update/delete-heavy) 워크로드, 회전식 디스크(spinning disks), SSD를 모두 처리합니다.

**Size Tiered 컴팩션 전략(Size Tiered Compaction Strategy, STCS)** - 기본(default) 전략입니다. 엄격하게 시계열이 아닌(non-strictly time-series) 워크로드로서 회전식 디스크를 사용하거나, LCS의 I/O가 과도할 때 적합합니다.

**Leveled 컴팩션 전략(Leveled Compaction Strategy, LCS)** - 읽기가 많은(read-heavy) 워크로드 또는 광범위한 업데이트/삭제가 있는 워크로드에 최적화되어 있습니다. 불변 시계열 데이터에는 부적합합니다.

**Time Window 컴팩션 전략(Time Window Compaction Strategy, TWCS)** - TTL이 적용되고 대부분 불변인(mostly immutable) 시계열 데이터를 위해 설계되었습니다.

---

## 3. 툼스톤(Tombstone)

### 3.1 툼스톤이란 무엇인가

"Cassandra는 삭제(deletion)를 삽입(insertion)으로 취급하며, 툼스톤(tombstone)이라고 불리는 타임스탬프가 찍힌 삭제 마커(time-stamped deletion marker)를 삽입합니다(Cassandra treats a deletion as an insertion, and inserts a time-stamped deletion marker called a tombstone)." 툼스톤은 내장된 만료 일자(built-in expiration dates)를 가지며, Cassandra의 일반적인 쓰기 경로(normal write path)를 통해 처리되어 여러 노드에 걸쳐 SSTable에 기록됩니다. 쿼리(query)는 툼스톤 삽입 이전에 타임스탬프가 찍힌 값(values timestamped before tombstone insertion)을 무시합니다.

TTL이 표시된 데이터(TTL-marked data) 역시 만료 시 툼스톤과 동일한 처리를 받습니다.

### 3.2 툼스톤이 필요한 이유

툼스톤 방식은 Cassandra의 분산 아키텍처(distributed architecture)를 수용합니다. 값을 즉시 제거하는 대신, 툼스톤은 복제된 데이터(replicated data)에 걸쳐 안전한 삭제를 가능하게 하면서 데이터 부활(data resurrection)을 방지합니다.

### 3.3 좀비(Zombie)

다중 노드 클러스터(multi-node cluster)에서는 복제본(replica)이 삭제 명령을 놓칠 수 있습니다. 응답하지 않는 노드(unresponsive node)는 삭제 이전 버전의 데이터를 보존합니다. 만약 툼스톤 처리된 객체가 해당 노드가 복구되기 전에 다른 곳에서 사라진다면, 지속되는 삭제된 데이터—이를 "좀비(zombie)"라고 부릅니다—가 새로운 데이터로서 클러스터 전체에 전파됩니다.

### 3.4 유예 기간(Grace Period)

"툼스톤의 유예 기간(grace period)은 테이블 속성 `WITH gc_grace_seconds`로 설정됩니다(The grace period for a tombstone is set with the table property `WITH gc_grace_seconds`)." 기본값(default value)은 864000초(10일)입니다. 이 기간은 응답하지 않는 노드에게 복구할 시간을 줍니다. 유예 기간 동안 새로운 업데이트는 툼스톤을 덮어쓰며, 읽기 작업은 툼스톤을 무시합니다. 유예 기간이 만료된 후에는 컴팩션이 툼스톤을 삭제할 수 있습니다.

또한 노드가 유예 기간 만료 이후에 복구되는 경우, Cassandra는 힌티드 핸드오프(hinted handoff)를 통해 툼스톤 처리된 객체에 대한 변경(mutation)을 재생하지(replay) 않습니다.

### 3.5 툼스톤 제거 조건

툼스톤 제거를 위해서는 다음이 필요합니다:
- 툼스톤의 나이(age)가 `gc_grace_seconds`를 초과해야 합니다.
- 컴팩션이 툼스톤을 포함하는 SSTable과, 해당 파티션의 더 오래된 데이터를 가진 모든 SSTable을 함께 포함해야 합니다(Compaction including both the SSTable containing the tombstone and all SSTables with older data for that partition).
- `only_purge_repaired_tombstones`가 활성화된 경우, 툼스톤 제거 전에 데이터가 복구(repair)되어 있어야 합니다.

**툼스톤이 없는 삭제(Deletes Without Tombstones)** - 3개 노드 클러스터 예제로 설명됩니다: 만약 하나의 노드가 실패하고, 삭제가 단순히 값만 제거한다면, 복구(repair)는 삭제된 데이터를 좀비로 부활시킵니다.

**툼스톤이 있는 삭제(Deletes With Tombstones)** - 값 [A]를 포함하는 3개의 복제된 노드로 시작하여, 삭제는 툼스톤을 추가합니다: [A, Tombstone[A]]. 복구는 데이터를 부활시키는 대신 툼스톤을 전파(propagate)하여 올바른 삭제 상태를 유지합니다.

### 3.6 완전히 만료된 SSTable(Fully Expired SSTables)

오직 툼스톤만 담고 있는 SSTable(만료된 TTL 데이터 포함)은, 다른 SSTable의 데이터를 가리지 않는다(not shadow data in other SSTables)는 것이 보장되는 경우 컴팩션 중에 폐기될(dropped) 수 있습니다. `sstableexpiredblockers` 도구는 폐기 가능한(droppable) SSTable과 이를 막고 있는(blocking) 의존성을 식별합니다. `TimeWindowCompactionStrategy`는 `unsafe_aggressive_sstable_expiration` 옵션을 통해 가리기(shadowing) 보장을 우회하여 TTL이 적용된 SSTable을 공격적으로(aggressive) 만료시키는 것을 허용합니다.

---

## 4. TTL(Time-To-Live)

데이터는 자동 만료를 유발하는 TTL 속성을 가질 수 있습니다. TTL이 만료되면 데이터는 최소 `gc_grace_seconds` 동안 지속되는 툼스톤으로 변환됩니다. TTL 데이터와 비-TTL(non-TTL) 데이터를 혼합하면 툼스톤 폐기가 복잡해지는데, 그 이유는 파티션이 컴팩션되지 않은 수많은 SSTable에 걸쳐 분산될 수 있기 때문입니다.

---

## 5. 복구된/복구되지 않은 데이터(Repaired/Unrepaired Data)

증분 복구(incremental repair)는 복구된 데이터(repaired data)와 복구되지 않은 데이터(unrepaired data)를 추적해야 합니다. 안티컴팩션(anticompaction)은 이들을 독립적인 컴팩션 전략 인스턴스(separate compaction strategy instances)를 가진 별도의 SSTable로 분리합니다. 만약 증분 복구가 한 번만 실행되고 다시 반복되지 않는다면, 오래된 복구된 데이터는 더 새로운 복구되지 않은 SSTable에 있는 툼스톤의 삭제를 막을 수 있습니다.

---

## 6. 데이터 디렉터리(Data Directories)

"데이터를 다시 살아나게 만드는 것(making data live)을 피하기 위해, 툼스톤과 실제 데이터(actual data)는 항상 동일한 데이터 디렉터리(same data directory)에 위치합니다(To avoid making data live tombstones and actual data are always in the same data directory)." 디스크 장애로 인한 SSTable 손실 시 데이터 부활을 방지합니다. 컴팩션 전략 인스턴스는 복구/미복구 인스턴스에 더하여 데이터 디렉터리마다(per data directory) 실행됩니다. 4개의 데이터 디렉터리가 있다면 8개의 전략 인스턴스가 실행됩니다.

이점은 다음과 같습니다:
- 병렬 컴팩션(parallel compaction) 가능; Leveled Compaction은 디렉터리별로 별도의 레벨링(separate levelings)을 유지합니다.
- 단일 데이터 디렉터리의 백업/복원(backup/restoration) 가능.
- 현재 모든 디렉터리는 동등하게(equal) 취급됩니다. 디스크 크기가 일치하지 않는(mismatched disk sizes) 경우에 대한 우회책(workaround)이 존재합니다.

---

## 7. 단일 SSTable 툼스톤 컴팩션

"SSTable이 기록될 때 툼스톤 만료 시간(tombstone expiry times)에 대한 히스토그램(histogram)이 생성됩니다(When an SSTable is written a histogram with the tombstone expiry times is created)." 이는 툼스톤이 많은(tombstone-heavy) SSTable을 식별하기 위한 것입니다. 단일 SSTable 컴팩션(Single SSTable compaction)은 툼스톤 삭제를 시도하며, 실제 삭제가 일어날 가능성과 SSTable의 겹침(overlap) 여부를 확인합니다. `unchecked_tombstone_compaction` 옵션은 이러한 점검(check)을 우회합니다.

---

## 8. 모든 전략에 공통된 컴팩션 옵션

**enabled** (기본값: true)
마이너 컴팩션(minor compaction)이 실행될지 여부입니다. `'enabled': true`를 `nodetool enableautocompaction`과 결합할 수 있습니다.

**tombstone_threshold** (기본값: 0.2)
단일 SSTable 컴팩션을 고려하도록 트리거하는, SSTable 내 툼스톤의 비율(proportion)입니다.

**tombstone_compaction_interval** (기본값: 86400초 / 1일)
지속적인 재컴팩션(constant recompaction)을 방지하기 위한, 단일 SSTable 컴팩션 시도 사이의 최소 시간(minimum time)입니다.

**log_all** (기본값: false)
로그 디렉터리(log directory)에 상세한 컴팩션 로깅(detailed compaction logging)을 활성화합니다.

**unchecked_tombstone_compaction** (기본값: false)
엄격한 단일 SSTable 컴팩션 점검(strict single SSTable compaction checks)을 비활성화하여, 불필요한 SSTable 재작성(rewrite)을 줄일 수 있습니다.

**only_purge_repaired_tombstones** (기본값: false)
툼스톤 제거가 데이터 복구 완료 이후에만 일어나도록 보장합니다.

**min_threshold** (기본값: 4)
컴팩션을 트리거하는 SSTable 개수의 하한(lower limit)입니다(LeveledCompactionStrategy에서는 사용되지 않음).

**max_threshold** (기본값: 32)
컴팩션을 트리거하는 SSTable 개수의 상한(upper limit)입니다(LeveledCompactionStrategy에서는 사용되지 않음).

---

## 9. 컴팩션 관련 nodetool 명령어

- `enableautocompaction` - 컴팩션을 활성화합니다.
- `disableautocompaction` - 컴팩션을 비활성화합니다.
- `setcompactionthroughput` - 최대 컴팩션 속도(maximum compaction speed)를 설정합니다(기본값: 64MiB/s).
- `compactionstats` - 현재/대기 중인(current/pending) 컴팩션 통계를 표시합니다.
- `compactionhistory` - 최근 컴팩션 세부 정보를 나열합니다.
- `setcompactionthreshold` - 최소/최대(min/max) SSTable 개수를 조정합니다(기본값: 4/32).

---

## 10. JMX를 통한 전략 전환

JMX를 통해 단일 노드(single-node)의 컴팩션 전략과 옵션을 수정할 수 있습니다:
- mbean: `org.apache.cassandra.db:type=ColumnFamilies,keyspace=<keyspace_name>,columnfamily=<table_name>`
- 속성(attribute): `CompactionParameters` 또는 `CompactionParametersJson`

JSON 구문은 `ALTER TABLE` 구문과 동일합니다:

```json
{ 'class': 'LeveledCompactionStrategy', 'sstable_size_in_mb': 123, 'fanout_size': 10}
```

이 설정은 `ALTER TABLE`로 수정하거나 노드를 재시작(restart)할 때까지 유지됩니다.

### 상세 컴팩션 로깅

`log_all` 컴팩션 옵션을 통해 활성화하면, 로그 디렉터리에 향상된 로깅(enhanced logging)이 생성됩니다.

---

## 11. Size Tiered 컴팩션 전략(STCS)

### STCS를 언제 사용해야 하는가

STCS는 여전히 기본(default) 컴팩션 전략이며, 특히 "쓰기 집약적 워크로드(write-intensive workloads)"에 권장됩니다. 다만, Cassandra 5.0에서는 대부분의 워크로드에 대해 통합 컴팩션 전략(Unified Compaction Strategy, UCS)이 권장되는 접근 방식이 되었음을 공식 문서가 명시하고 있습니다.

### STCS의 동작 원리

이 전략은 직관적인 트리거링 메커니즘을 통해 동작합니다: "STCS는 Cassandra가 비슷한 크기(similar-sized)의 SSTable을 정해진 개수(기본값: 4)만큼 축적했을 때 컴팩션을 시작합니다(STCS initiates compaction when Cassandra has accumulated a set number (default: 4) of similar-sized SSTables)." 이 테이블들은 점진적으로 더 큰 SSTable로 병합되어, 클러스터 전반에 걸쳐 다양한 크기의 계층화된(tiered) 구조를 형성합니다.

### 버킷팅 알고리즘(The Bucketing Algorithm)

STCS는 SSTable을 크기별로 그룹화하기 위해 버킷팅(bucketing) 프로세스를 사용합니다. 이 메커니즘은 "[평균 크기 × bucket_low]와 [평균 크기 × bucket_high]" 범위 내의 크기를 가진 테이블들을 그룹화합니다(with a size within [average-size × bucket_low] and [average-size × bucket_high]). 기본적으로 이는 평균 버킷 크기의 50%에서 150% 사이의 테이블들을 함께 그룹화하는 범위를 만들어, 전략이 컴팩션 후보를 식별할 수 있게 합니다.

### 성능 트레이드오프

쓰기가 많은 시나리오에 효과적이지만, STCS는 읽기 성능 패널티(read performance penalties)를 유발합니다. 크기 기반 병합(merge-by-size) 방식은 데이터를 행(row) 단위로 그룹화하지 못하므로, "특정 행의 버전들이 여러 SSTable에 걸쳐 퍼질 수 있음(versions of a particular row may be spread over many SSTables)"을 의미합니다. 또한, "STCS는 삭제된 데이터를 예측 가능하게(predictably) 제거하지 못하는데, 그 이유는 컴팩션의 트리거가 SSTable 크기이기 때문입니다(STCS does not evict deleted data predictably, because its trigger for compaction is SSTable size)."

### 공간 증폭(Space Amplification) 문제

메이저 컴팩션(major compaction) 중에 중대한 한계가 드러납니다: "STCS 컴팩션 중에 새로운 SSTable과 오래된 SSTable이 동시에(simultaneously) 필요로 하는 디스크 공간의 양이, 노드에서 일반적으로 사용 가능한 디스크 공간을 초과할 수 있습니다(the amount of disk space needed for both the new and old SSTables simultaneously during STCS compaction can outstrip a typical amount of disk space on a node)." 공식 문서는 "STCS에 대해 메이저 컴팩션은 권장되지 않습니다(major compactions are not recommended for STCS)"라고 명시적으로 언급합니다.

### STCS 설정 옵션

| 옵션 | 기본값 | 목적 |
|------|--------|------|
| `enabled` | true | "백그라운드 컴팩션을 활성화합니다(Enables background compaction)" |
| `min_threshold` | 4 | "마이너 컴팩션을 트리거하기 위한 SSTable의 최소 개수(The minimum number of SSTables to trigger a minor compaction)" |
| `max_threshold` | 32 | "마이너 컴팩션에서 허용할 SSTable의 최대 개수(The maximum number of SSTables to allow in a minor compaction)" |
| `bucket_low` | 0.5 | "SSTable의 크기가 해당 버킷 평균 크기의 50%보다 크면 그 버킷에 추가됩니다(An SSTable is added to a bucket if the SSTable size is greater than 50% of the average size of that bucket)" |
| `bucket_high` | 1.5 | "SSTable의 크기가 평균 크기의 150%보다 작으면 그 버킷에 추가됩니다(An SSTable is added to a bucket if its size is less than 150% of the average size)" |
| `min_sstable_size` | 50MB | "이 값보다 작은 SSTable들은 하나의 버킷으로 그룹화됩니다(SSTables smaller than this value will be grouped into one bucket)" |
| `tombstone_threshold` | 0.2 | "가비지 컬렉션 가능한(garbage-collectable) 툼스톤이 전체 포함된 컬럼 대비 차지하는 비율(The ratio of garbage-collectable tombstones to all contained columns)" |
| `tombstone_compaction_interval` | 86400 | "Cassandra가 툼스톤 컴팩션을 위해 SSTable을 고려하기 전에, 해당 SSTable이 생성된 후 경과해야 하는 최소 초 단위 시간(The minimum number of seconds after which an SSTable is created before Cassandra considers the SSTable for tombstone compaction)" |
| `unchecked_tombstone_compaction` | false | "어떤 테이블이 적격인지 사전 점검 없이(without pre-checking which tables are eligible)" 툼스톤 컴팩션을 허용합니다 |
| `only_purge_repaired_tombstones` | false | 활성화 시 "복구된 SSTable에서만(only from repaired SSTables)" 툼스톤 제거를 제한합니다 |
| `log_all` | false | "전체 클러스터에 대한 고급 로깅을 활성화합니다(Activates advanced logging for the entire cluster)" |

---

## 12. Leveled 컴팩션 전략(LCS)

### LCS를 언제 사용해야 하는가

LCS는 읽기가 많은(read-heavy) 워크로드에 적합합니다. 다만 공식 문서는 "통합 컴팩션 전략(UCS)이 Cassandra 5.0부터 대부분의 워크로드에 권장되는 컴팩션 전략입니다(The Unified Compaction Strategy (UCS) is the recommended compaction strategy for most workloads starting with {cass-50})"라고 언급합니다.

### LCS의 동작 원리

LCS는 계층화된 레벨 시스템(tiered level system)을 통해 동작합니다. 멤테이블이 플러시되면, SSTable은 레벨 L0에 기록되며 이 레벨에서는 SSTable들이 서로 겹칠(overlap) 수 있습니다. 이후 전략은 "이 최초의 SSTable들을 레벨 L1에 있는 더 큰 SSTable들과 병합합니다. 각 레벨은 기본적으로 이전 레벨의 10배 크기입니다(merges these first SSTables with larger SSTables in level L1. Each level is by default 10x the size of the previous one)."

데이터가 L1 이상에 도달하면, "해당 SSTable은 같은 레벨의 다른 SSTable과 겹치지 않는 것이 보장됩니다(the SSTable is guaranteed to be non-overlapping with other SSTables in the same level)." 이 설계는 "읽기 작업이 어떤 행에 접근해야 할 때, 레벨당 하나의 SSTable만 보면 된다(if a read operation needs to access a row, it will only need to look at one SSTable per level)"는 것을 의미합니다.

### 컴팩션 프로세스

컴팩션은 겹치는 모든 SSTable을 다음 레벨의 새로운 SSTable로 병합함으로써 동작합니다. "L0 → L1 컴팩션의 경우, 대부분의 L0 SSTable이 전체 파티션 범위를 커버하기 때문에 우리는 거의 항상 모든 L1 SSTable을 포함해야 합니다(For L0 → L1 compactions, we almost always need to include all L1 SSTables since most L0 SSTables cover the full range of partitions)."

### L0 안전장치(L0 Safeguard)

LCS는 페일세이프(failsafe) 보호 장치를 포함합니다: "L0에 32개를 초과하는 SSTable이 있으면 L0에서 STCS 컴팩션이 트리거됩니다(An STCS compaction will be triggered in L0 if there are more than 32 SSTables in L0)."

### 성능 특성

LCS는 "STCS만큼 디스크를 많이 소모하지 않으며(not as disk hungry as STCS), 실행에 약 10%의 디스크만 필요로 하지만, I/O와 CPU를 더 많이 사용합니다(it is more IO and CPU intensive)." 그러나 "쓰기가 많은 워크로드에는 좋은 선택이 아니며(It is not a good choice for write-heavy workloads)", "LCS에 대해 메이저 컴팩션은 권장되지 않습니다(Major compactions are not recommended for LCS)."

### Starved SSTable 문제

LCS는 "starved sstables"라는 문제를 일으킬 수 있는데, 이는 "낮은 레벨의 SSTable이 병합 및 컴팩션되지 않기 때문에, 높은 레벨의 SSTable이 고립되어(stranded) 컴팩션되지 않을 때(High level SSTables can be stranded and not compacted, because SSTables in lower levels are not getting merged and compacted)" 발생합니다. 이는 일반적으로 사용자가 `sstable_size` 설정을 낮출 때 발생합니다.

### LCS 설정 옵션

| 옵션 | 설명 | 기본값 |
|------|------|--------|
| `enabled` | 백그라운드 컴팩션을 활성화합니다 | true |
| `tombstone_compaction_interval` | 툼스톤 컴팩션을 위해 SSTable을 고려하기 전 최소 초 단위 시간 | 86400 |
| `tombstone_threshold` | 컴팩션을 트리거하는, 가비지 컬렉션 가능한 툼스톤의 비율 | 0.2 |
| `unchecked_tombstone_compaction` | 사전 점검 없이 툼스톤 컴팩션을 허용합니다 | false |
| `log_all` | 고급 클러스터 로깅을 활성화합니다 | false |
| `sstable_size_in_mb` | 목표 SSTable 크기(Target SSTable size) | 160 |
| `fanout_size` | 레벨 크기 증가 배수(Level size increase multiplier) | 10 |
| `single_sstable_uplevel` | (목적은 문서에 명시되지 않음) | true |

시작 옵션(startup option) `-Dcassandra.disable_stcs_in_l0=true`는 L0에서의 STCS를 비활성화합니다.

---

## 13. Time Window 컴팩션 전략(TWCS)

### 개요

TWCS는 시계열(time-series) 및 TTL로 만료되는(TTL-expiring) 데이터 워크로드를 위해 설계되었습니다. TWCS는 "SSTable을 시간 윈도우(time window)별로 그룹화한 다음, 만료될 때 전체 SSTable을 폐기합니다(groups SSTables by time window, and then drop entire SSTables when they expire)." 이를 통해 STCS나 LCS보다 더 효율적으로 디스크 공간을 회수할 수 있습니다.

### 동작 원리

이 전략은 데이터를 시간 기반 버킷(time-based bucket)으로 조직화하여 동작합니다. 활성 윈도우(active window) 동안, TWCS는 플러시된 SSTable에 대해 STCS 스타일의 컴팩션을 사용합니다. 시간 윈도우가 닫히면(closes), 모든 SSTable이 최대 타임스탬프(maximum timestamp)를 기준으로 하나의 SSTable로 컴팩션됩니다. 이 메이저 컴팩션이 완료된 후, 해당 데이터는 더 이상 컴팩션을 거치지 않습니다(no further compaction). 이 과정은 후속 윈도우(subsequent windows)에 대해 반복됩니다.

### 주요 설정 옵션

| 옵션 | 기본값 | 목적 |
|------|--------|------|
| `enabled` | true | 백그라운드 컴팩션을 활성화합니다 |
| `compaction_window_unit` | days | 시간 단위(DAYS, HOURS 등) |
| `compaction_window_size` | 1 | 버킷당 단위 수(Units per bucket) |
| `timestamp_resolution` | microseconds | 데이터 삽입 타임스탬프의 해상도(Data insertion timestamp resolution) |
| `tombstone_threshold` | 0.2 | 가비지 컬렉션 가능한 툼스톤의 비율 |
| `tombstone_compaction_interval` | 86400 | SSTable이 적격이 되기 전까지의 초 단위 시간 |
| `expired_sstable_check_frequency_seconds` | 600 | 만료 점검 빈도(Expiration check frequency) |

### 운영 시 고려사항

주요 위험은 순서가 어긋난 데이터 쓰기(out-of-order data writes)와 관련되어 있습니다—즉, 오래된 데이터와 새로운 데이터가 단일 SSTable에 혼합되는 것입니다. 이는 타임스탬프가 혼합된 직접 쓰기(direct writes with mixed timestamps)를 통해, 또는 읽기 복구(read repair)를 통해 발생할 수 있습니다. 공식 문서는 데이터가 섞이는 것(data commingling)을 방지하기 위해 CQL에서 명시적인 타임스탬프 설정(explicit timestamp setting)을 피하고, 빈번한 복구(frequent repairs)를 실행할 것을 권장합니다.

### 사이징 권장사항(Sizing Recommendations)

운영자는 대략 20~30개의 전체 윈도우(total windows)를 생성하는 윈도우 파라미터를 선택해야 합니다. 예를 들어, 90일 TTL 데이터의 경우 3일 윈도우(3-day window)가 적절합니다.

---

## 14. 압축(Compression)

### 14.1 압축이란 무엇인가

Cassandra는 테이블별(per-table) 압축을 가능하게 하며, 이는 SSTable을 설정 가능한 청크(configurable chunks) 단위로 압축하여 디스크상의 데이터 크기를 줄입니다. SSTable은 불변(immutable)이므로, 압축의 CPU 비용은 쓰기(write) 시에만 발생합니다. 읽기(read)는 표준 읽기 작업을 진행하기 전에 전체 청크(full chunks)를 압축 해제(decompress)합니다.

### 14.2 압축 알고리즘의 트레이드오프

모든 알고리즘은 세 가지 요소(three factors)의 균형을 맞춥니다:

- **압축 속도(Compression speed)**: 플러시/컴팩션 경로(flush/compaction paths)에 중요합니다.
- **압축 해제 속도(Decompression speed)**: 읽기/컴팩션 경로(read/compaction paths)에 중요합니다.
- **압축률(Ratio)**: 압축되지 않은 데이터의 감소 비율(Uncompressed data reduction percentage)입니다.

### 14.3 사용 가능한 압축기(Compressor)

| 알고리즘 | 클래스(Class) | 압축 | 압축 해제 | 압축률 | 버전 |
|----------|---------------|------|-----------|--------|------|
| LZ4 | `LZ4Compressor` | A+ | A+ | C+ | ≥1.2.2 |
| LZ4HC | `LZ4Compressor` | C+ | A+ | B+ | ≥3.6 |
| Zstd | `ZstdCompressor` | A- | A- | A+ | ≥4.0 |
| Snappy | `SnappyCompressor` | A- | A | C | ≥1.0 |
| Deflate (zlib) | `DeflateCompressor` | C | C | A | ≥1.0 |

**권장사항(Recommendations):**
- 성능이 중요한(performance-critical) 애플리케이션: 최적의 속도 대비 압축률을 위해 `LZ4`(기본값)를 사용하십시오.
- 저장 공간이 중요한(storage-critical) 애플리케이션: 우수한 압축을 위해 `Zstd`를 사용하십시오.
- 레거시(Legacy): `Snappy`와 `Deflate`는 하위 호환성(backward compatibility)을 위해 유지됩니다.

### 14.4 공통 압축 옵션

**class** (기본값: `LZ4Compressor`)
- 압축 알고리즘 클래스를 지정합니다.

**chunk_length_in_kb** (기본값: `16KiB`)
- 압축 청크당 데이터 킬로바이트(Data kilobytes per compression chunk)입니다. 더 큰 청크는 압축률을 향상시키지만 더 많은 디스크 I/O를 필요로 합니다(Larger chunks improve ratio but require more disk I/O).

**crc_check_chance** (기본값: `1.0`)
- 읽기 중 체크섬 검증 확률(checksum verification probability)을 결정하는 부동소수점(Float, 0.0–1.0) 값입니다. 비트로트(bitrot, 비트 변질)로부터 보호합니다.

**enabled** (기본값: `true`)
- 압축 활성화를 제어하는 불리언(Boolean) 값입니다.

### 14.5 압축기별 전용 옵션

#### LZ4Compressor 전용 옵션

**lz4_compressor_type** (기본값: `fast`)
- 옵션: `fast`(표준 LZ4) 또는 `high`(LZ4HC).

**lz4_high_compressor_level** (기본값: `9`)
- 범위: 1–17. 값이 높을수록 속도보다 압축률을 우선합니다(Higher values prioritize ratio over speed).

#### ZstdCompressor 전용 옵션

**compression_level** (기본값: `3`)
- 범위: -131072 ~ 22. 값이 낮을수록 속도가 증가합니다(Lower values increase speed). 레벨 20–22("ultra")는 추가 메모리(additional memory)를 필요로 합니다.

### 14.6 압축 설정 예제(CQL)

테이블 생성 시 압축을 활성화하기:

```sql
CREATE TABLE keyspace.table (id int PRIMARY KEY)
   WITH compression = {'class': 'LZ4Compressor'};
```

기존 테이블의 압축을 수정하기:

```sql
ALTER TABLE keyspace.table
   WITH compression = {'class': 'LZ4Compressor', 'chunk_length_in_kb': 64};
```

압축을 비활성화하기:

```sql
ALTER TABLE keyspace.table
   WITH compression = {'enabled':'false'};
```

### 14.7 압축 변경 적용 방법

압축은 SSTable이 기록될 때(during SSTable writes) 적용됩니다. 기존 SSTable은 컴팩션될 때까지 변경되지 않은 채로 남아 있습니다. 즉각적인 효과를 원한다면, `nodetool scrub` 또는 `nodetool upgradesstables -a`를 사용하여 SSTable 재작성(rewrites)을 트리거하십시오.

### 14.8 압축의 이점

- 디스크 저장 공간 요구량을 줄입니다.
- I/O 양을 줄임으로써 종종 읽기/쓰기 처리량(throughput)을 증가시킵니다.
- 압축의 CPU 오버헤드는 일반적으로 비압축 디스크 작업(uncompressed disk operations)보다 빠릅니다.

### 14.9 적합한 사용 사례와 부적합한 사용 사례

**최적의 사용 사례(Optimal Use Cases):**
- 유사한 행(similar rows)이 많은 테이블.
- 반복되는 JSON 블롭(repeated JSON blobs)이나 텍스트 컬럼.
- 압축률이 높은(high compressibility) 데이터.

**압축이 잘 되지 않는 시나리오(Poor compression scenarios):**
- 이미 압축된 데이터(Pre-compressed data).
- 무작위/벤치마크 데이터셋(Random/benchmark datasets).

### 14.10 운영 시 고려사항

- 압축 메타데이터(compression metadata)는 디스크상 데이터 1테라바이트당 1–3GB의 오프힙(off-heap) RAM을 필요로 합니다.
- 스트리밍 작업(streaming operations)은 압축/압축 해제 오버헤드를 유발합니다.
- 느린 압축기(slow compressors, 예: Zstd, Deflate, LZ4HC)는 기본적으로 빠른 LZ4를 사용하여 플러시합니다. 이후 일반 컴팩션(normal compaction)이 원하는 알고리즘으로 다시 압축합니다(recompress).
- 오직 압축된 테이블(compressed tables)만이 `crc_check_chance` 검증을 지원합니다.

#### 고급 구현(Advanced Implementation)

사용자는 `org.apache.cassandra.io.compress.ICompressor` 인터페이스를 통해 커스텀 압축(custom compression)을 구현할 수 있습니다.

---

## 15. 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [Compaction 개요](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/compaction/index.html)
- [Size Tiered Compaction Strategy (STCS)](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/compaction/stcs.html)
- [Leveled Compaction Strategy (LCS)](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/compaction/lcs.html)
- [Time Window Compaction Strategy (TWCS)](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/compaction/twcs.html)
- [Compression](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/compression.html)

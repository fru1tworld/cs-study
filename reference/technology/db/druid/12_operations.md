# Druid 운영

> 원본: https://druid.apache.org/docs/latest/operations/web-console
> 원본: https://druid.apache.org/docs/latest/operations/rolling-updates
> 원본: https://druid.apache.org/docs/latest/operations/high-availability
> 원본: https://druid.apache.org/docs/latest/operations/rule-configuration
> 원본: https://druid.apache.org/docs/latest/operations/metrics
> 원본: https://druid.apache.org/docs/latest/operations/alerts
> 원본: https://druid.apache.org/docs/latest/operations/clean-metadata-store

웹 콘솔, 롤링 업데이트, 고가용성 구성, retention 규칙, 메트릭과 알림, 메타데이터 스토리지 정리 등 Druid 클러스터 운영에 필요한 내용을 정리합니다.

---

## 목차

1. [웹 콘솔](#웹-콘솔)
2. [롤링 업데이트](#롤링-업데이트)
3. [고가용성](#고가용성)
4. [Retention 규칙 설정](#retention-규칙-설정)
5. [메트릭](#메트릭)
6. [알림](#알림)
7. [메타데이터 스토리지 정리](#메타데이터-스토리지-정리)
8. [참고 자료](#참고-자료)

---

## 웹 콘솔

Druid 웹 콘솔은 데이터 관리, 클러스터 상태 모니터링, 쿼리 실행을 위한 내장 인터페이스입니다. Router 서비스가 호스팅합니다.

### 접속과 사전 요구 사항

- 접속 주소: `http://<ROUTER_IP>:<ROUTER_PORT>`
- 다음 두 설정이 필요합니다(기본적으로 활성화되어 있습니다).
  - Router의 management proxy 활성화
  - Broker 프로세스에서 Druid SQL 활성화
- 보안 참고: 사용자 권한을 적절히 구성해야 하며, Druid를 root 사용자로 실행하지 않아야 합니다.

### 주요 뷰

| 뷰 | 기능 |
| --- | --- |
| **Home** | Status, Datasources, Segments, Supervisors, Tasks, Services, Lookups로 이동하는 카드를 보여주는 대시보드입니다. |
| **Query** | 멀티 탭 SQL 인터페이스입니다. Druid 24.0부터 기본인 multi-stage query task engine을 지원합니다. |
| **Data loader** | 단계별 마법사(wizard)로 인제스천(ingestion) 스펙을 작성하며, 각 단계마다 데이터 미리보기를 제공합니다. |
| **Datasources** | 로드된 모든 데이터소스와 크기·가용성을 보여줍니다. retention 규칙 편집, 자동 컴팩션 설정, 데이터 삭제, 세그먼트 타임라인 조회를 지원합니다. |
| **Supervisors** | 인덱싱 태스크 슈퍼바이저를 관리합니다. suspend, resume, reset, 스펙 제출이 가능하며 상세 진행 리포트를 제공합니다. |
| **Tasks** | 실행 중·완료된 태스크 목록을 Type, Datasource, Status별로 그룹화해 보여줍니다. 태스크를 직접 제출하고 상세 정보를 조회할 수 있습니다. |
| **Segments** | 클러스터의 모든 세그먼트를 보여줍니다. Datasource, Start, End, Version, Partition 열로 필터링·정렬할 수 있습니다. |
| **Services** | 클러스터 노드 상태를 Type 또는 Tier별로 그룹화해 요약 통계와 함께 보여줍니다. |
| **Lookups** | 쿼리 타임 lookup을 생성하고 편집하는 관리 인터페이스입니다. |

### Query 뷰 세부 기능

- 스키마/데이터소스 브라우저 패널
- Run/Preview 버튼으로 쿼리 실행
- API 엔드포인트를 선택하는 engine 선택기
- 실시간 진행 상황 추적과 라이브 리포트
- 쿼리 히스토리와 태스크 모니터링
- SQL 실행 계획(EXPLAIN) 확인, 인제스천 스펙 변환 도구

---

## 롤링 업데이트

Druid 클러스터는 무중단(zero downtime)으로 롤링 업데이트할 수 있습니다. 서비스를 다음 순서로 업데이트합니다.

1. Historical
2. Middle Manager와 Indexer
3. Broker
4. Router
5. Overlord (autoscaling을 사용하면 Middle Manager보다 먼저 업데이트할 수 있습니다)
6. Coordinator (또는 Coordinator+Overlord 통합 프로세스)

다운그레이드할 때는 이 순서를 반대로, Coordinator부터 시작합니다.

### Historical

한 번에 하나씩 업데이트합니다. Historical 프로세스는 재시작 후 업데이트 이전에 서빙하던 모든 세그먼트를 다시 메모리 매핑해야 하므로, 하드웨어에 따라 시작에 수 초에서 수 분이 걸립니다.

### Overlord

한 번에 하나씩 순차적으로 업데이트합니다.

### Middle Manager / Indexer

실시간 인덱싱 태스크가 실행 중이므로 세 가지 전략 중 하나를 사용합니다.

**1. 태스크 복원(restore) 기반**

`druid.indexer.task.restoreTasksOnRestart=true`를 설정하면 Middle Manager 재시작 시 태스크가 복원됩니다. 단, 실시간(realtime) 태스크만 복원을 지원하며, 그 외 태스크는 다시 제출해야 합니다.

**2. 우아한 종료(graceful termination)**

disable API로 Middle Manager가 새 태스크를 받지 않도록 만든 뒤, 실행 중인 태스크가 모두 끝나면 업데이트합니다.

```bash
# 새 태스크 수락 중지
POST http://<MM_IP>:<PORT>/druid/worker/v1/disable

# 실행 중인 태스크 확인 (빈 목록이 되면 업데이트 가능)
GET http://<MM_IP>:<PORT>/druid/worker/v1/tasks
```

재시작하면 자동으로 다시 활성화됩니다.

**3. Autoscaling 기반 교체**

autoscaling을 사용하는 환경에서는 다음 두 속성의 버전 값을 올리면, 새 버전의 Middle Manager가 대량으로 기동되고 기존 프로세스는 우아하게 종료됩니다.

```properties
druid.indexer.runner.minWorkerVersion=#{VERSION}
druid.indexer.autoscale.workerVersion=#{VERSION}
```

### Standalone Real-time

한 번에 하나씩 순차적으로 업데이트합니다.

### Broker

한 번에 하나씩 업데이트하되, 각 프로세스 사이에 간격을 둡니다. Broker는 유효한 결과를 반환하려면 클러스터의 전체 상태를 먼저 로드해야 하기 때문입니다.

### Coordinator

한 번에 하나씩 업데이트합니다.

---

## 고가용성

### ZooKeeper

고가용성 ZooKeeper를 구성하려면 3개 또는 5개 노드로 이루어진 ZooKeeper 클러스터가 필요합니다. 전용 하드웨어를 사용하거나, Overlord·Coordinator를 호스팅하는 Master 서버 3~5대에서 함께 실행할 수 있습니다.

### 메타데이터 스토리지

고가용성 메타데이터 저장을 위해 복제(replication)와 페일오버(failover)를 활성화한 MySQL 또는 PostgreSQL 사용을 권장합니다.

### Coordinator와 Overlord

Coordinator와 Overlord의 고가용성을 위해 여러 대의 서버를 실행하는 것을 권장합니다. 동일한 ZooKeeper 클러스터와 메타데이터 스토리지를 바라보도록 구성하면 자동으로 페일오버합니다. 한 번에 하나만 활성(active) 상태이며, 비활성 서버는 요청을 현재 활성 서버로 리다이렉트합니다.

### Broker

Broker는 수평 확장(scale out)할 수 있고 실행 중인 모든 서버가 활성 상태로 쿼리를 처리합니다. 쿼리 분산을 위해 로드 밸런서 뒤에 배치하는 것을 권장합니다.

---

## Retention 규칙 설정

retention 규칙은 Druid가 어떤 데이터를 유지하고 어떤 데이터를 삭제(drop)할지 결정합니다. 규칙은 JSON 객체로 메타데이터 스토리지에 영속 저장되며, Coordinator가 각 세그먼트에 대해 규칙을 평가합니다.

### 규칙 평가 순서

Coordinator는 규칙 목록에 나타난 순서대로 규칙을 읽습니다. 각 세그먼트는 처음으로 매칭되는 규칙 하나에만 적용되므로 규칙의 순서가 매우 중요합니다.

### 규칙 설정 방법

**웹 콘솔**: Datasources > 데이터소스 선택 > Actions > Edit retention rules > +New rule > 속성 설정 > Save

**Coordinator API**:

```bash
# 기본 규칙 설정
POST /druid/coordinator/v1/rules/_default

# 특정 데이터소스 규칙 설정
POST /druid/coordinator/v1/rules/{datasourceName}

# 전체 규칙 조회
GET /druid/coordinator/v1/rules
```

API 요청마다 원하는 순서로 정렬한 규칙 배열 전체를 전달해야 합니다.

### 로드(load) 규칙

세그먼트를 Historical 티어에 할당하고 복제본 수를 지정합니다. 모든 로드 규칙은 다음 속성을 지원합니다.

- `tieredReplicants`: 티어 이름과 복제본 수의 매핑
- `useDefaultTierForNull`: 기본값 `true`. `tieredReplicants`를 지정하지 않으면 `{"_default_tier": 2}`를 사용합니다.

**Forever Load Rule (`loadForever`)** — 모든 세그먼트에 적용됩니다.

```json
{
  "type": "loadForever",
  "tieredReplicants": {
    "hot": 1,
    "_default_tier": 1
  }
}
```

**Period Load Rule (`loadByPeriod`)** — ISO 8601 기간 내의 세그먼트를 대상으로 합니다. `includeFuture`가 true이면 기간과 겹치거나 기간 시작 이후에 시작하는 세그먼트를 매칭합니다.

```json
{
  "type": "loadByPeriod",
  "period": "P1M",
  "includeFuture": true,
  "tieredReplicants": {
    "hot": 1,
    "_default_tier": 1
  }
}
```

**Interval Load Rule (`loadByInterval`)** — 고정된 날짜 구간을 대상으로 합니다.

```json
{
  "type": "loadByInterval",
  "interval": "2012-01-01/2013-01-01",
  "tieredReplicants": {
    "hot": 1,
    "_default_tier": 1
  }
}
```

### 드롭(drop) 규칙

세그먼트를 클러스터에서 제거할 조건을 정의합니다. 드롭된 세그먼트도 딥 스토리지(deep storage)에는 남아 있습니다.

**Forever Drop Rule (`dropForever`)** — 모든 세그먼트를 드롭합니다.

```json
{
  "type": "dropForever"
}
```

**Period Drop Rule (`dropByPeriod`)** — 기간에 매칭되는 데이터를 드롭합니다.

```json
{
  "type": "dropByPeriod",
  "period": "P1M",
  "includeFuture": true
}
```

**Period Drop Before Rule (`dropBeforeByPeriod`)** — 지정한 기간 이전의 오래된 데이터를 드롭합니다.

```json
{
  "type": "dropBeforeByPeriod",
  "period": "P1M"
}
```

**Interval Drop Rule (`dropByInterval`)** — 특정 구간의 데이터가 로드되지 않도록 합니다.

```json
{
  "type": "dropByInterval",
  "interval": "2012-01-01/2013-01-01"
}
```

### 브로드캐스트(broadcast) 규칙

세그먼트를 모든 Broker에 로드합니다(테스트 환경 전용). Broker에 `druid.segmentCache.locations` 설정이 필요합니다.

```json
{ "type": "broadcastForever" }
```

```json
{
  "type": "broadcastByPeriod",
  "period": "P1M",
  "includeFuture": true
}
```

```json
{
  "type": "broadcastByInterval",
  "interval": "2012-01-01/2013-01-01"
}
```

### 영구 삭제와 재로드

- **영구 삭제**: 규칙으로 클러스터에서 드롭된 세그먼트는 항상 `unused`로 표시됩니다. `unused` 세그먼트를 딥 스토리지에서까지 삭제하려면 kill 태스크를 제출합니다.
- **드롭된 데이터 재로드**: 규칙 하나로는 불가능합니다. (1) retention 기간을 늘리고, (2) API 또는 웹 콘솔에서 세그먼트를 `used`로 표시하면 Coordinator가 누락된 세그먼트를 다시 로드합니다.

---

## 메트릭

Druid는 설정 가능한 emitter로 메트릭을 내보냅니다. 모든 메트릭은 `timestamp`, `metric`(메트릭 이름), `service`, `host`, `version`, `buildRevision`, 숫자 `value` 필드를 공통으로 가집니다. 대부분의 메트릭 값은 `druid.monitoring.emissionPeriod`로 정한 방출 주기마다 초기화됩니다.

### 쿼리 메트릭 (Broker/Historical)

| 메트릭 | 설명 | 정상 값 |
| --- | --- | --- |
| `query/time` | 쿼리 완료까지 걸린 시간(ms) | < 1s |
| `query/bytes` | 클라이언트에 반환한 응답 바이트 수 | — |
| `query/success/count` | 성공한 쿼리 수 (`QueryCountStatsMonitor` 필요) | — |
| `query/failed/count` | 실패한 쿼리 수 | — |
| `query/node/time` | 개별 historical/realtime 프로세스 쿼리 시간 | < 1s |
| `query/cpu/time` | 소비한 CPU 시간(마이크로초) | — |

### SQL 메트릭

| 메트릭 | 설명 | 정상 값 |
| --- | --- | --- |
| `sqlQuery/time` | SQL 쿼리 완료 시간 | < 1s |
| `sqlQuery/planningTimeMs` | SQL을 네이티브 쿼리로 변환하는 데 걸린 시간 | — |
| `sqlQuery/bytes` | SQL 쿼리 응답 바이트 수 | — |

### 인제스천(ingestion) 메트릭

| 메트릭 | 설명 |
| --- | --- |
| `ingest/events/processed` | 방출 주기당 처리한 이벤트 수 |
| `ingest/kafka/lag` | Kafka 파티션 전체의 오프셋 지연(lag) |
| `ingest/kafka/maxLag` | 파티션별 최대 지연 |
| `ingest/rows/published` | 성공적으로 발행(publish)된 행 수 |

### Coordinator 메트릭

| 메트릭 | 설명 | 정상 값 |
| --- | --- | --- |
| `segment/assigned/count` | 로드하도록 할당된 세그먼트 수 | — |
| `segment/moved/count` | 재분배(rebalance)로 이동한 세그먼트 수 | — |
| `segment/dropped/count` | 과잉 복제로 드롭된 세그먼트 수 | — |
| `segment/unavailable/count` | 로드를 기다리는 세그먼트 수 | 0 |

### JVM 및 상태(health) 메트릭

| 메트릭 | 설명 |
| --- | --- |
| `jvm/mem/used` | 현재 힙 사용량 |
| `jvm/mem/max` | 사용 가능한 최대 메모리 |
| `jvm/gc/count` | GC 발생 횟수 |
| `jvm/gc/cpu` | GC에 소비한 시간(나노초). 전체 CPU의 10~30% 수준이어야 합니다 |
| `service/heartbeat` | 서비스 동작 지표. 값 1 (`ServiceStatusMonitor` 필요) |

### 시스템 메트릭 (OshiSysMonitor 권장)

| 메트릭 | 설명 |
| --- | --- |
| `sys/mem/used` | 시스템 메모리 사용량 |
| `sys/cpu` | 프로세스별 CPU 사용률 |
| `sys/disk/read/size` | 디스크 읽기 바이트 수 |
| `cgroup/memory/usage/bytes` | 컨테이너 메모리 사용량 (cgroup 환경) |

각 메트릭에는 필터링과 집계를 위한 디멘션(dimension)이 함께 붙습니다(`dataSource`, `taskId`, `server`, `tier` 등).

---

## 알림

Druid는 예상하지 못한 상황을 만나면 알림(alert)을 생성합니다. 알림은 JSON 객체로 런타임 로그 파일에 기록하거나 HTTP로 Apache Kafka 같은 외부 서비스에 내보낼 수 있습니다. 알림 방출은 기본적으로 비활성화되어 있으므로, 사용하려면 emitter 설정에서 명시적으로 활성화해야 합니다.

### 공통 알림 필드

| 필드 | 설명 |
| --- | --- |
| `timestamp` | 알림이 생성된 시각 |
| `service` | 알림을 발생시킨 서비스 이름 |
| `host` | 알림을 발생시킨 호스트 이름 |
| `severity` | 심각도. 예: `anomaly`, `component-failure`, `service-failure` |
| `description` | 알림에 대한 맥락 정보 |
| `data` | 예외의 경우 `exceptionType`, `exceptionMessage`, `exceptionStackTrace`를 담은 JSON 객체 |

알림은 요청 로깅(request logging), 메트릭 수집과 함께 Druid 운영 모니터링을 구성하는 요소 중 하나입니다.

---

## 메타데이터 스토리지 정리

데이터소스와 개체를 자주 생성·삭제하는 환경에서는 메타데이터 스토리지에 오래된 레코드가 쌓여 성능이 저하될 수 있습니다. Druid는 메타데이터 레코드를 자동으로 정리하는 기능을 제공합니다. 기본 retention 기간은 90일이며, 컴팩션 설정과 인덱서 태스크 로그 정리는 기본적으로 비활성화되어 있습니다.

### 정리 대상 메타데이터 유형

1. **세그먼트 레코드와 딥 스토리지의 세그먼트** — kill 태스크 설정 필요
2. **감사(audit) 레코드** — retention 기간이 지나면 전부 정리 대상
3. **슈퍼바이저 레코드** — 슈퍼바이저 종료 후 retention 기간이 지나면 대상
4. **규칙(rule) 레코드** — kill 태스크 필요, 모든 세그먼트가 kill된 후 대상
5. **컴팩션 설정 레코드** — 세그먼트가 없는 비활성 데이터소스 대상
6. **데이터소스 레코드** — 슈퍼바이저가 생성한 레코드, 슈퍼바이저 종료 후 대상
7. **인덱싱 상태 레코드** — 미사용이거나 대기(pending) 상태로 retention 기간이 지난 경우
8. **인덱서 태스크 로그** — 딥 스토리지와 메타데이터에서 함께 제거

### Coordinator 설정 속성

| 속성 | 기본값 | 용도 |
| --- | --- | --- |
| `druid.coordinator.period.metadataStoreManagementPeriod` | — | 메타데이터 관리 작업 실행 주기 |
| `druid.coordinator.kill.on` | true | 세그먼트 레코드 정리 활성화 |
| `druid.coordinator.kill.period` | P1D | kill 태스크 실행 주기 (ISO 8601) |
| `druid.coordinator.kill.durationToRetain` | P90D | 삭제 전 보존 기간 |
| `druid.coordinator.kill.bufferPeriod` | — | 정리 전 버퍼 기간 |
| `druid.coordinator.kill.maxSegments` | — | 태스크당 최대 삭제 세그먼트 수 |
| `druid.coordinator.kill.audit.on` | false | 감사 레코드 정리 활성화 |
| `druid.coordinator.kill.audit.period` | P1D | 감사 레코드 정리 주기 |
| `druid.coordinator.kill.audit.durationToRetain` | P90D | 감사 레코드 보존 기간 |
| `druid.coordinator.kill.supervisor.on` | false | 슈퍼바이저 레코드 정리 활성화 |
| `druid.coordinator.kill.supervisor.period` | P1D | 슈퍼바이저 레코드 정리 주기 |
| `druid.coordinator.kill.supervisor.durationToRetain` | P90D | 슈퍼바이저 레코드 보존 기간 |
| `druid.coordinator.kill.rule.on` | false | 규칙 레코드 정리 활성화 |
| `druid.coordinator.kill.rule.period` | P1D | 규칙 레코드 정리 주기 |
| `druid.coordinator.kill.rule.durationToRetain` | P90D | 규칙 레코드 보존 기간 |
| `druid.coordinator.kill.compaction.on` | false | 컴팩션 설정 정리 활성화 |
| `druid.coordinator.kill.compaction.period` | P1D | 컴팩션 설정 정리 주기 |
| `druid.coordinator.kill.datasource.on` | false | 데이터소스 레코드 정리 활성화 |
| `druid.coordinator.kill.datasource.period` | P1D | 데이터소스 레코드 정리 주기 |
| `druid.coordinator.kill.datasource.durationToRetain` | P90D | 데이터소스 레코드 보존 기간 |

### Overlord 설정 속성

| 속성 | 기본값 | 용도 |
| --- | --- | --- |
| `druid.overlord.kill.indexingStates.on` | false | 인덱싱 상태 레코드 정리 활성화 |
| `druid.overlord.kill.indexingStates.period` | P1D | 인덱싱 상태 정리 주기 |
| `druid.overlord.kill.indexingStates.durationToRetain` | P7D | 비활성 상태 보존 기간 |
| `druid.overlord.kill.indexingStates.pendingDurationToRetain` | P7D | 대기 상태 보존 기간 |
| `druid.indexer.logs.kill.enabled` | false | 태스크 로그 정리 활성화 |
| `druid.indexer.logs.kill.durationToRetain` | — | 태스크 로그 보존 기간 (밀리초) |
| `druid.indexer.logs.kill.initialDelay` | — | 첫 정리까지의 초기 지연 (밀리초) |
| `druid.indexer.logs.kill.delay` | — | 정리 작업 간 지연 (밀리초) |

### 사전 요구 사항과 주의점

- 규칙 레코드와 컴팩션 설정 정리에는 kill 태스크 활성화(`druid.coordinator.kill.on=true`)가 선행되어야 합니다.
- 메타데이터 관리 주기(`metadataStoreManagementPeriod`)는 개별 정리 작업 주기와 같거나 더 짧아야 합니다.
- kill 태스크는 dynamic configuration의 `killDataSourceWhitelist`를 따릅니다.
- kill 태스크는 메타데이터뿐 아니라 딥 스토리지의 실제 데이터까지 삭제하는 유일한 정리 작업입니다.
- 데이터소스가 존재하기 전에 만든 컴팩션 설정은 조기에 삭제될 수 있습니다.
- 감사(audit) 규정 준수가 필요하면 정리를 활성화하기 전에 감사 레코드를 미리 내보내야 합니다.
- 컴팩션 설정이 크면 감사 로그 크기 제한을 초과할 수 있으므로 `druid.audit.manager.maxPayloadSizeBytes`를 조정합니다.
- 인덱싱 상태 정리는 기본 비활성화이며, 자동 컴팩션 슈퍼바이저를 사용할 때만 해당 레코드가 생성됩니다.
- 정리를 끄려면 `druid.coordinator.kill.on=false`와 함께 각 개체별 정리 플래그를 `false`로 설정합니다.

---

## 참고 자료

- [Web console](https://druid.apache.org/docs/latest/operations/web-console)
- [Rolling updates](https://druid.apache.org/docs/latest/operations/rolling-updates)
- [High availability](https://druid.apache.org/docs/latest/operations/high-availability)
- [Using rules to drop and retain data](https://druid.apache.org/docs/latest/operations/rule-configuration)
- [Metrics](https://druid.apache.org/docs/latest/operations/metrics)
- [Alerts](https://druid.apache.org/docs/latest/operations/alerts)
- [Automated cleanup for metadata records](https://druid.apache.org/docs/latest/operations/clean-metadata-store)

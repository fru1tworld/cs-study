# Kafka Connect

> 이 문서는 Apache Kafka 공식 문서의 Kafka Connect 섹션을 한국어로 번역한 것입니다.
> 원본: https://kafka.apache.org/documentation/#connect

---

## 목차

1. [개요](#개요)
2. [Kafka Connect 실행하기](#kafka-connect-실행하기)
3. [커넥터 구성](#커넥터-구성)
4. [변환(Transformations)](#변환transformations)
5. [REST API](#rest-api)
6. [오류 처리](#오류-처리)
7. [정확히 한 번 시맨틱스(Exactly-Once Semantics)](#정확히-한-번-시맨틱스exactly-once-semantics)
8. [플러그인 검색](#플러그인-검색)
9. [커넥터 개발 가이드](#커넥터-개발-가이드)
10. [관리](#관리)

---

## 개요

Kafka Connect는 Apache Kafka와 외부 시스템 간에 데이터를 안정적으로 전송하기 위한 확장 가능한 도구입니다. 이 플랫폼은 대량의 데이터를 Kafka 인프라로 이동하거나 Kafka에서 외부로 내보내는 커넥터를 빠르게 정의할 수 있게 해줍니다.

### 핵심 기능

Kafka Connect는 다음과 같은 핵심 기능을 제공합니다:

| 기능 | 설명 |
|------|------|
| 공통 프레임워크 | Kafka 커넥터를 위한 공통 프레임워크로, 외부 데이터 시스템이 Kafka와 통합되는 방식을 표준화하여 커넥터 개발, 배포 및 관리의 전체 라이프사이클을 단순화합니다. |
| 배포 유연성 | 분산(Distributed) 및 독립 실행(Standalone) 모드로 운영할 수 있어, 엔터프라이즈 전체 서비스로 확장하거나 개발 및 테스트 환경으로 축소할 수 있습니다. |
| REST API | 사용하기 쉬운 REST API를 통해 Kafka Connect 클러스터에 커넥터를 제출하고 관리할 수 있습니다. |
| 자동 오프셋 관리 | 오프셋 커밋 프로세스를 자동으로 관리하여, 전통적으로 오류가 발생하기 쉬운 작업에 대한 개발자 부담을 줄입니다. |
| 확장성 | Kafka의 그룹 관리 프로토콜을 기반으로 구축되어, 추가 워커를 배치하여 수평으로 확장할 수 있습니다. |
| 스트리밍/배치 통합 | 스트리밍과 배치 시스템을 효과적으로 연결하여, 혼합 데이터 처리 워크플로를 관리하는 조직에 적합합니다. |

### 사용 사례

Kafka Connect는 다음과 같은 시나리오를 처리합니다:
- 데이터베이스 수집(Database ingestion)
- 애플리케이션 메트릭 수집
- 보조 저장소 시스템으로의 데이터 내보내기
- 배치 분석 통합

---

## Kafka Connect 실행하기

### 독립 실행 모드 (Standalone Mode)

독립 실행 모드는 개발 및 테스트 환경이나 단일 에이전트로 운영해야 하는 경우에 적합합니다.

실행 명령:

```bash
bin/connect-standalone.sh config/connect-standalone.properties connector1.properties [connector2.properties ...]
```

필수 워커 설정:

| 설정 | 설명 |
|------|------|
| `bootstrap.servers` | Kafka 서버 연결 주소 |
| `key.converter` | 키 형식 변환기 (JSON, Avro 등) |
| `value.converter` | 값 형식 변환기 |
| `plugin.path` | 커넥터 및 변환 플러그인이 포함된 경로 |
| `offset.storage.file.filename` | 소스 커넥터 오프셋을 저장할 파일 |

### 분산 모드 (Distributed Mode)

분산 모드는 프로덕션 환경에서 권장되며, 내결함성과 확장성을 제공합니다.

실행 명령:

```bash
bin/connect-distributed.sh config/connect-distributed.properties
```

핵심 설정 파라미터:

| 설정 | 기본값 | 설명 |
|------|--------|------|
| `group.id` | `connect-cluster` | Connect 클러스터의 고유 식별자 |
| `config.storage.topic` | `connect-configs` | 커넥터 설정 저장 토픽 (단일 파티션, 압축) |
| `offset.storage.topic` | `connect-offsets` | 오프셋 저장 토픽 (다중 파티션, 압축) |
| `status.storage.topic` | `connect-status` | 상태 저장 토픽 (다중 파티션, 압축) |

---

## 커넥터 구성

### 공통 설정 옵션

| 설정 | 설명 |
|------|------|
| `name` | 고유한 커넥터 식별자 |
| `connector.class` | 커넥터의 Java 클래스 |
| `tasks.max` | 최대 태스크 인스턴스 수 |
| `key.converter` | 키 변환기 (선택적 오버라이드) |
| `value.converter` | 값 변환기 (선택적 오버라이드) |

### Sink 커넥터 입력 옵션

Sink 커넥터는 Kafka에서 데이터를 가져와 외부 시스템으로 내보냅니다:

| 설정 | 설명 |
|------|------|
| `topics` | 쉼표로 구분된 토픽 목록 |
| `topics.regex` | 토픽 매칭을 위한 Java 정규 표현식 |

---

## 변환(Transformations)

변환(Transformations)은 가벼운 메시지 수정을 가능하게 합니다. 커넥터에서 처리하기 전이나 후에 레코드를 변환할 수 있습니다.

### 변환 설정 구조

```properties
transforms=MakeMap,InsertSource
transforms.MakeMap.type=org.apache.kafka.connect.transforms.HoistField$Value
transforms.MakeMap.field=line
transforms.InsertSource.type=org.apache.kafka.connect.transforms.InsertField$Value
transforms.InsertSource.static.field=source
transforms.InsertSource.static.value=mySource
```

### 내장 변환 (16가지)

| 변환 | 용도 |
|------|------|
| `Cast` | 필드의 타입 변환 |
| `DropHeaders` | 이름으로 헤더 제거 |
| `ExtractField` | 특정 필드 추출 |
| `Filter` | 조건부로 메시지 제거 |
| `Flatten` | 중첩 구조 평탄화 |
| `HeaderFrom` | 필드를 헤더로 복사/이동 |
| `HoistField` | 데이터를 단일 필드로 래핑 |
| `InsertField` | 정적 또는 메타데이터 필드 추가 |
| `InsertHeader` | 레코드에 헤더 추가 |
| `MaskField` | 필드를 null 또는 사용자 정의 값으로 대체 |
| `RegexRouter` | 정규식 기반으로 토픽 수정 |
| `ReplaceField` | 필드 필터링 또는 이름 변경 |
| `SetSchemaMetadata` | 스키마 메타데이터 수정 |
| `TimestampConverter` | 타임스탬프 형식 변환 |
| `TimestampRouter` | 타임스탬프 기반 라우팅 |
| `ValueToKey` | 값 필드에서 키 대체 |

### 조건부 적용 (Predicates)

Predicate를 사용하면 변환을 조건부로 적용할 수 있습니다:

| Predicate | 설명 |
|-----------|------|
| `TopicNameMatches` | 정규식으로 토픽 매칭 |
| `HasHeaderKey` | 특정 헤더가 있는 레코드 매칭 |
| `RecordIsTombstone` | null 값 레코드 매칭 |

Predicate 설정 예시:

```properties
transforms=Extract
transforms.Extract.type=org.apache.kafka.connect.transforms.ExtractField$Value
transforms.Extract.field=data
transforms.Extract.predicate=IsBar
transforms.Extract.negate=true

predicates=IsBar
predicates.IsBar.type=org.apache.kafka.connect.transforms.predicates.TopicNameMatches
predicates.IsBar.pattern=bar.*
```

---

## REST API

Kafka Connect는 커넥터를 관리하기 위한 REST API를 제공합니다.

기본 엔드포인트: `http://localhost:8083` (`listeners` 설정으로 변경 가능)

### 커넥터 관리

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| `GET` | `/connectors` | 활성 커넥터 목록 조회 |
| `POST` | `/connectors` | 커넥터 생성 |
| `GET` | `/connectors/{name}` | 커넥터 정보 조회 |
| `PUT` | `/connectors/{name}/config` | 커넥터 설정 업데이트 |
| `PATCH` | `/connectors/{name}/config` | 커넥터 설정 부분 업데이트 |
| `DELETE` | `/connectors/{name}` | 커넥터 삭제 |

### 커넥터 제어

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| `PUT` | `/connectors/{name}/pause` | 처리 일시 중지 |
| `PUT` | `/connectors/{name}/stop` | 중지 및 리소스 해제 |
| `PUT` | `/connectors/{name}/resume` | 작업 재개 |
| `POST` | `/connectors/{name}/restart` | 커넥터 재시작 |

### 상태 및 모니터링

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| `GET` | `/connectors/{name}/status` | 전체 상태 조회 |
| `GET` | `/connectors/{name}/tasks` | 태스크 목록 조회 |
| `GET` | `/connectors/{name}/tasks/{taskid}/status` | 태스크 상태 조회 |
| `GET` | `/connectors/{name}/topics` | 활성 토픽 조회 |

### 오프셋 관리 (KIP-875)

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| `GET` | `/connectors/{name}/offsets` | 오프셋 조회 |
| `DELETE` | `/connectors/{name}/offsets` | 오프셋 리셋 |
| `PATCH` | `/connectors/{name}/offsets` | 오프셋 변경 |

### 플러그인 정보

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| `GET` | `/connector-plugins` | 설치된 플러그인 목록 |
| `GET` | `/connector-plugins/{type}/config` | 설정 정의 조회 |
| `PUT` | `/connector-plugins/{type}/config/validate` | 설정 유효성 검사 |

### 커넥터 생성 예시

```bash
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "my-connector",
    "config": {
      "connector.class": "org.apache.kafka.connect.file.FileStreamSourceConnector",
      "tasks.max": "1",
      "file": "/tmp/test.txt",
      "topic": "my-topic"
    }
  }'
```

---

## 오류 처리

Kafka Connect는 다양한 오류 처리 옵션을 제공합니다.

### 설정 옵션

| 설정 | 설명 |
|------|------|
| `errors.log.enable` | 오류 로깅 활성화 (`true`/`false`) |
| `errors.log.include.messages` | 로그에 레코드 세부 정보 포함 |
| `errors.deadletterqueue.topic.name` | Dead Letter Queue 토픽 이름 |
| `errors.retry.timeout` | 재시도 시간 (밀리초) |
| `errors.tolerance` | 허용 수준: `none` (기본값), `all` |

### 오류 처리 설정 예시

```properties
# 재시도 설정
errors.retry.timeout=600000
errors.retry.delay.max.ms=30000

# 로깅 설정
errors.log.enable=true
errors.log.include.messages=true

# Dead Letter Queue 설정
errors.deadletterqueue.topic.name=my-connector-errors
errors.deadletterqueue.topic.replication.factor=3

# 오류 허용
errors.tolerance=all
```

이 설정으로:
- 최대 10분(600000ms) 동안 재시도
- 재시도 간격 최대 30초
- 오류를 로그에 기록
- 처리할 수 없는 레코드를 Dead Letter Queue로 전송
- 모든 오류를 허용하고 계속 처리

---

## 정확히 한 번 시맨틱스(Exactly-Once Semantics)

### Sink 커넥터

Sink 커넥터에서 정확히 한 번 시맨틱스를 활성화하려면:

```properties
consumer.isolation.level=read_committed
```

이 설정으로 중단된(aborted) 트랜잭션을 무시합니다.

### Source 커넥터 (분산 모드에서만)

Source 커넥터에서 정확히 한 번 시맨틱스를 활성화하려면 워커 설정이 필요합니다:

워커 설정:

| 설정 | 설명 |
|------|------|
| `exactly.once.source.support=enabled` | 새 클러스터에서 활성화 |
| `exactly.once.source.support=preparing` | 기존 클러스터 업그레이드 시 먼저 설정 |

기존 클러스터 업그레이드 절차:
1. 먼저 `preparing`으로 설정하여 모든 워커 업그레이드
2. 그 다음 `enabled`로 변경

필요한 ACL:

- TransactionalId 리소스에 대한 Write/Describe 권한
- 오프셋 토픽에 대한 Write/Read/Describe 권한
- Cluster 리소스에 대한 IdempotentWrite 권한

---

## 플러그인 검색

### 검색 전략

| 전략 | 설명 |
|------|------|
| `hybrid_warn` | 기본값 (3.6+), 비호환성 경고 |
| `hybrid_fail` | 비호환성 시 오류 발생 |
| `service_load` | 가장 빠름, 호환 플러그인 필요 |
| `only_scan` | 가장 느림, 모든 플러그인 호환 |

### 검증 단계

1. `hybrid_warn` 전략으로 워커 시작
2. 로그에서 호환성 경고 확인
3. 또는 `hybrid_fail` 전략으로 테스트

### 아티팩트 마이그레이션

플러그인 경로 도구를 사용하여 마이그레이션할 수 있습니다:

Linux:

```bash
# 플러그인 목록 확인
bin/connect-plugin-path.sh list --worker-config config/connect-distributed.properties

# 마이그레이션 시뮬레이션
bin/connect-plugin-path.sh sync-manifests --dry-run

# 실제 마이그레이션 실행
bin/connect-plugin-path.sh sync-manifests
```

Windows:

```batch
bin\windows\connect-plugin-path.bat list --worker-config config\connect-distributed.properties
bin\windows\connect-plugin-path.bat sync-manifests --dry-run
bin\windows\connect-plugin-path.bat sync-manifests
```

### 소스 코드 마이그레이션 (개발자용)

플러그인 개발자는 `META-INF/services/`에 ServiceLoader 매니페스트를 추가해야 합니다:

지원되는 플러그인 타입:

- `org.apache.kafka.connect.sink.SinkConnector`
- `org.apache.kafka.connect.source.SourceConnector`
- `org.apache.kafka.connect.storage.Converter`
- `org.apache.kafka.connect.storage.HeaderConverter`
- `org.apache.kafka.connect.transforms.Transformation`
- `org.apache.kafka.connect.transforms.predicates.Predicate`
- `org.apache.kafka.common.config.provider.ConfigProvider`

매니페스트 파일 예시:

`META-INF/services/org.apache.kafka.connect.sink.SinkConnector` 파일 내용:

```
com.example.MySinkConnector
```

---

## 커넥터 개발 가이드

### 핵심 개념

#### 커넥터와 태스크 구조

커넥터는 두 가지 유형이 있습니다:
- SourceConnector: 다른 시스템에서 데이터를 가져옴
- SinkConnector: 다른 시스템으로 데이터를 내보냄

커넥터는 실제 복사 작업을 태스크(Task)에 위임하며, 태스크는 병렬 처리를 위해 워커 전체에 분산됩니다.

#### 데이터 모델

레코드는 키-값 쌍과 연관된 스트림 ID 및 오프셋으로 구성됩니다. 프레임워크는 주기적으로 오프셋을 자동 커밋하여, 실패 시 데이터를 재처리하거나 중복하지 않고 복구할 수 있게 합니다.

#### 동적 모니터링

커넥터는 외부 시스템의 변경을 모니터링해야 합니다. 새 데이터베이스 테이블과 같은 구조적 변경이 발생하면, `ConnectorContext.requestTaskReconfiguration()`을 통해 프레임워크에 알립니다.

### 구현 요구 사항

#### 두 가지 핵심 인터페이스

개발자는 `Connector`와 `Task` 인터페이스를 구현해야 합니다.

#### ServiceLoader 등록

런타임 검색을 위해 `META-INF/services/org.apache.kafka.connect.source.SourceConnector`에 정규화된 클래스 이름을 추가합니다.

### SourceTask 구현

#### 라이프사이클 메서드

| 메서드 | 설명 |
|--------|------|
| `start()` | 리소스 할당 |
| `stop()` | 리소스 해제 (스레드 안전을 위해 동기화) |
| `poll()` | 소스 파티션, 오프셋, 토픽, 값을 포함한 `List<SourceRecord>` 반환 |

#### 오프셋 관리

태스크는 `SourceContext.offsetStorageReader()`를 통해 이전 오프셋에 접근하여 데이터 손실 없이 커밋된 위치에서 재개할 수 있습니다.

#### 커밋 API

| API | 설명 |
|-----|------|
| `commit()` | 벌크 오프셋 확인 |
| `commitRecord()` | 레코드별 확인 |

### SinkTask 구현

#### Push 기반 인터페이스

| 메서드 | 설명 |
|--------|------|
| `put()` | 처리 및 버퍼링을 위해 `Collection<SinkRecord>` 수신 |
| `flush()` | 미처리 데이터를 푸시하고 확인될 때까지 블록 |
| `initialize()` | `SinkTaskContext` 수신 |

#### 오류 처리

`ErrantRecordReporter`를 사용하면 활성화된 경우 개별 레코드에 대한 안전한 오류 보고가 가능하며, 하위 호환성을 위한 null-safe 패턴 처리를 지원합니다.

### 정확히 한 번 시맨틱스 (v3.3.0+)

#### 프레임워크 지원

소스 커넥터는 의미 있는 오프셋을 내보내고 메시지 삭제나 중복 없이 재개함으로써 정확히 한 번 전달을 제공할 수 있습니다.

#### 트랜잭션 경계

커넥터 설정을 통해 활성화되면, 태스크는 `TransactionContext`에 접근하여 트랜잭션이 커밋되거나 중단되는 시점을 제어할 수 있습니다. 이는 관련 레코드의 원자적 전달에 유용합니다.

#### 검증 메서드

| 메서드 | 설명 |
|--------|------|
| `exactlyOnceSupport()` | 기능 수준 선언 |
| `canDefineTransactionBoundaries()` | 사용자 정의 경계 지원 표시 |

### 설정 및 검증

#### ConfigDef 선언

커넥터는 모든 파라미터, 타입, 기본값, 검증기, 추천기를 정의하는 `ConfigDef` 인스턴스를 반환하는 `config()` 메서드를 통해 설정을 노출합니다.

```java
public ConfigDef config() {
    return new ConfigDef()
        .define("file", ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, "Source file path")
        .define("topic", ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, "Destination topic");
}
```

#### 검증 훅

프레임워크 기본값을 넘어서는 사용자 정의 검증 로직을 위해 `validate()`를 오버라이드하여, 상호 의존적인 설정과 동적 가시성 규칙을 지원합니다.

### 스키마 관리

#### 복잡한 데이터 처리

구조화된 레코드에는 `Schema`와 `Struct` 클래스를 사용합니다. 소스 커넥터는 가능한 경우 스키마를 정적으로 생성해야 하며, 싱크 커넥터는 들어오는 스키마가 예상 형식과 일치하는지 검증해야 합니다.

#### 동적 스키마 진화

데이터베이스 커넥터는 동적 스키마의 예입니다. 테이블은 인스턴스마다 다르며 ALTER TABLE 명령을 통해 변경될 수 있어 감지와 적응이 필요합니다.

### 동적 스트림 관리

#### 소스 관점

테이블 추가/삭제를 모니터링하고 재구성을 위해 프레임워크에 알립니다.

#### 싱크 관점

다운스트림 리소스(데이터베이스 테이블)를 생성하여 새 입력 스트림을 처리하고, 여러 태스크가 동시에 추가를 감지할 때 충돌을 관리합니다.

---

## 관리

### 리밸런싱 메커니즘

#### 프로토콜 진화

| 버전 | 동작 |
|------|------|
| 2.3.0 이전 | Eager 리밸런싱: 모든 커넥터와 태스크 재분배 |
| 2.3.0+ | Incremental Cooperative 리밸런싱 (기본값): 새로 추가, 제거 또는 재배치된 태스크만 영향 |

#### 워커 이탈 처리

워커가 떠나면, Connect는 `scheduled.rebalance.max.delay.ms` (기본값 5분) 동안 기다린 후 태스크를 재할당하여, 워커가 즉시 재분배 없이 다시 참여할 수 있게 합니다.

### 태스크 상태

API는 다음 상태를 지원합니다:

| 상태 | 설명 |
|------|------|
| `UNASSIGNED` | 아직 워커에 할당되지 않음 |
| `RUNNING` | 활발히 처리 중 |
| `PAUSED` | 관리자에 의해 중지됨 |
| `STOPPED` | 커넥터 종료됨 (3.5.0+) |
| `FAILED` | 예외 발생 |
| `RESTARTING` | 활발히 또는 예상대로 재시작 중 |

### 운영 기능

#### 모니터링

REST 엔드포인트로 커넥터 및 태스크 할당을 워커 ID와 함께 표시합니다:

```bash
curl http://localhost:8083/connectors/file-source/status
```

응답 예시:

```json
{
  "name": "file-source",
  "connector": {
    "state": "RUNNING",
    "worker_id": "192.168.1.10:8083"
  },
  "tasks": [
    {
      "id": 0,
      "state": "RUNNING",
      "worker_id": "192.168.1.10:8083"
    }
  ],
  "type": "source"
}
```

#### 일시 중지/재개

리소스 해제 없이 메시지 처리를 일시적으로 중지하여 빠른 재개를 가능하게 합니다:

```bash
# 일시 중지
curl -X PUT http://localhost:8083/connectors/file-source/pause

# 재개
curl -X PUT http://localhost:8083/connectors/file-source/resume
```

#### Stop API (3.5.0+)

커넥터 태스크를 완전히 종료하고 리소스를 해제합니다. 일시 중지보다 효율적이지만 재개하는 데 더 오래 걸립니다:

```bash
# 중지
curl -X PUT http://localhost:8083/connectors/file-source/stop

# 재개
curl -X PUT http://localhost:8083/connectors/file-source/resume
```

#### 토픽 추적 (2.5.0+)

커넥터별 토픽을 모니터링하고 리셋할 수 있습니다:

```bash
# 토픽 조회
curl http://localhost:8083/connectors/file-source/topics

# 토픽 리셋
curl -X PUT http://localhost:8083/connectors/file-source/topics/reset
```

---

## 참고 자료

- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Kafka Connect Overview](https://kafka.apache.org/41/kafka-connect/overview/)
- [Kafka Connect User Guide](https://kafka.apache.org/41/kafka-connect/user-guide/)
- [Kafka Connect Connector Development Guide](https://kafka.apache.org/41/kafka-connect/connector-development-guide/)
- [Kafka Connect Administration](https://kafka.apache.org/41/kafka-connect/administration/)

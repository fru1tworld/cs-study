# Kafka 토픽 설정

> 이 문서는 Apache Kafka 공식 문서의 Topic Configuration 섹션을 한국어로 번역한 것입니다.
> 원본: https://kafka.apache.org/documentation/#topicconfigs

## 목차

1. [개요](#개요)
2. [보존 및 정리 설정](#보존-및-정리-설정)
3. [압축 설정](#압축-설정)
4. [로그 컴팩션 설정](#로그-컴팩션-설정)
5. [세그먼트 관리 설정](#세그먼트-관리-설정)
6. [메시지 처리 설정](#메시지-처리-설정)
7. [복제 및 내구성 설정](#복제-및-내구성-설정)
8. [플러싱 및 인덱싱 설정](#플러싱-및-인덱싱-설정)
9. [계층형 스토리지 설정](#계층형-스토리지-설정)
10. [기타 설정](#기타-설정)

---

## 개요

토픽 레벨 설정은 각 토픽에 대해 개별적으로 구성할 수 있는 설정입니다. 모든 설정은 토픽 생성 시 또는 이후에 `kafka-configs.sh` 도구를 사용하여 변경할 수 있습니다. 토픽 레벨 설정은 브로커의 기본 설정보다 우선합니다.

---

## 보존 및 정리 설정

### cleanup.policy

로그 세그먼트에 사용할 보존 정책을 지정합니다.

| 속성 | 값 |
|------|------|
| 기본값 | delete |
| 유효한 값 | compact, delete |
| 중요도 | medium |

- `delete`: 오래된 세그먼트는 보존 시간 또는 크기 제한에 도달하면 삭제됩니다.
- `compact`: 로그 컴팩션이 활성화됩니다. 각 키의 최신 값만 유지됩니다.
- 쉼표로 구분된 목록(예: `"compact,delete"`)을 사용하여 두 정책을 모두 활성화할 수 있습니다.

```bash
kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name my-topic --alter --add-config cleanup.policy=compact
```

### retention.ms

오래된 로그 세그먼트를 폐기하기 전 최대 보존 시간을 제어합니다. 로그 세그먼트의 나이가 이 값보다 오래되면 삭제 대상이 됩니다.

| 속성 | 값 |
|------|------|
| 기본값 | 604800000 (7일) |
| 유효한 값 | [-1, ...] |
| 중요도 | medium |

- `-1`로 설정하면 시간 기반 보존이 비활성화됩니다(무제한 보존).
- 밀리초 단위입니다.

```bash
# 3일 보존 설정
kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name my-topic --alter --add-config retention.ms=259200000
```

### retention.bytes

오래된 세그먼트를 폐기하기 전 파티션의 최대 크기를 제어합니다. 파티션 크기가 이 값을 초과하면 가장 오래된 세그먼트부터 삭제됩니다.

| 속성 | 값 |
|------|------|
| 기본값 | -1 (제한 없음) |
| 중요도 | medium |

- `-1`로 설정하면 크기 기반 보존이 비활성화됩니다.
- 파티션별 적용됩니다 (토픽 전체가 아님).

```bash
# 파티션당 1GB 제한 설정
kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name my-topic --alter --add-config retention.bytes=1073741824
```

---

## 압축 설정

### compression.type

토픽의 최종 압축 코덱을 지정합니다.

| 속성 | 값 |
|------|------|
| 기본값 | producer |
| 유효한 값 | uncompressed, zstd, lz4, snappy, gzip, producer |
| 중요도 | medium |

- `producer`: 프로듀서가 설정한 압축 방식을 그대로 유지합니다.
- `uncompressed`: 압축을 사용하지 않습니다.
- `zstd`, `lz4`, `snappy`, `gzip`: 해당 압축 알고리즘을 사용합니다.

```bash
kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name my-topic --alter --add-config compression.type=lz4
```

### compression.gzip.level

GZIP 압축 레벨을 설정합니다.

| 속성 | 값 |
|------|------|
| 기본값 | -1 |
| 유효한 범위 | [1, ..., 9] 또는 -1 |

- `-1`은 기본 압축 레벨을 사용합니다.
- 높은 값일수록 더 높은 압축률을 제공하지만 CPU 사용량이 증가합니다.

### compression.lz4.level

LZ4 압축 레벨을 설정합니다.

| 속성 | 값 |
|------|------|
| 기본값 | 9 |
| 유효한 범위 | [1, ..., 17] |

### compression.zstd.level

Zstandard 압축 레벨을 설정합니다.

| 속성 | 값 |
|------|------|
| 기본값 | 3 |
| 유효한 범위 | [-131072, ..., 22] |

---

## 로그 컴팩션 설정

로그 컴팩션은 각 메시지 키의 최신 값만 유지하여 로그 크기를 줄이는 메커니즘입니다.

### min.cleanable.dirty.ratio

로그 컴팩터가 로그 정리를 시도하는 빈도를 제어합니다. 이 비율은 컴팩션 대상이 되는 "더티(dirty)" 로그 부분과 이미 컴팩션된 "클린(clean)" 로그 부분의 비율입니다.

| 속성 | 값 |
|------|------|
| 기본값 | 0.5 |
| 중요도 | medium |

- 값이 낮을수록 컴팩션이 더 자주 발생하지만 리소스를 더 많이 사용합니다.
- 값이 높을수록 컴팩션 빈도가 줄어들지만 로그가 더 커질 수 있습니다.

### min.compaction.lag.ms

메시지가 컴팩션되지 않은 상태로 유지되는 최소 시간입니다.

| 속성 | 값 |
|------|------|
| 기본값 | 0 |

- 이 시간이 지나기 전에는 메시지가 컴팩션되지 않습니다.
- 컨슈머가 모든 메시지를 읽을 수 있도록 하려면 이 값을 설정하세요.

### max.compaction.lag.ms

메시지가 컴팩션 대상이 되기까지의 최대 시간입니다.

| 속성 | 값 |
|------|------|
| 기본값 | 9223372036854775807 (Long.MAX_VALUE) |

- 이 시간이 지나면 메시지는 컴팩션 대상이 됩니다.

### delete.retention.ms

컴팩션된 토픽에서 삭제 톰스톤(tombstone) 마커를 보존하는 시간입니다.

| 속성 | 값 |
|------|------|
| 기본값 | 86400000 (1일) |

- 톰스톤은 키에 대한 삭제 표시입니다.
- 컨슈머가 삭제를 확인할 수 있도록 충분한 시간을 설정해야 합니다.

```bash
kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name my-topic --alter --add-config delete.retention.ms=172800000
```

---

## 세그먼트 관리 설정

### segment.bytes

세그먼트 파일의 크기를 제어합니다. 세그먼트가 이 크기에 도달하면 새 세그먼트가 생성됩니다.

| 속성 | 값 |
|------|------|
| 기본값 | 1073741824 (1GB) |
| 유효한 범위 | [1048576, ...] |

- 작은 세그먼트는 보존 정책이 더 세밀하게 적용되지만 파일 핸들이 더 많이 필요합니다.
- 큰 세그먼트는 I/O 효율성이 높지만 보존 정책 적용이 덜 정밀합니다.

```bash
# 512MB 세그먼트 설정
kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name my-topic --alter --add-config segment.bytes=536870912
```

### segment.ms

Kafka가 로그 롤(roll)을 강제하는 시간 주기입니다. 세그먼트가 가득 차지 않아도 이 시간이 지나면 새 세그먼트가 생성됩니다.

| 속성 | 값 |
|------|------|
| 기본값 | 604800000 (7일) |

```bash
# 1일마다 새 세그먼트 생성
kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name my-topic --alter --add-config segment.ms=86400000
```

### segment.index.bytes

오프셋-위치 인덱스의 크기를 제어합니다.

| 속성 | 값 |
|------|------|
| 기본값 | 10485760 (10MB) |
| 유효한 범위 | [4, ...] |

- 인덱스 파일은 세그먼트 파일 내에서 오프셋을 빠르게 찾는 데 사용됩니다.

### segment.jitter.ms

예정된 세그먼트 롤 시간에서 차감되는 최대 무작위 지터(jitter)입니다.

| 속성 | 값 |
|------|------|
| 기본값 | 0 |

- 여러 파티션의 세그먼트 롤이 동시에 발생하는 것을 방지합니다.
- I/O 스파이크를 줄이는 데 유용합니다.

---

## 메시지 처리 설정

### max.message.bytes

Kafka가 허용하는 가장 큰 레코드 배치 크기입니다 (압축 후 크기).

| 속성 | 값 |
|------|------|
| 기본값 | 1048588 |
| 중요도 | medium |

- 이 값을 늘리면 `replica.fetch.max.bytes` 브로커 설정도 함께 늘려야 합니다.
- 컨슈머의 `fetch.message.max.bytes` 설정도 충분히 크게 설정해야 합니다.

```bash
# 최대 메시지 크기 10MB 설정
kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name my-topic --alter --add-config max.message.bytes=10485760
```

### message.timestamp.type

타임스탬프가 생성 시간을 나타내는지 또는 추가 시간을 나타내는지 정의합니다.

| 속성 | 값 |
|------|------|
| 기본값 | CreateTime |
| 유효한 값 | CreateTime, LogAppendTime |

- `CreateTime`: 프로듀서가 메시지를 생성한 시간을 사용합니다.
- `LogAppendTime`: 브로커가 메시지를 로그에 추가한 시간을 사용합니다.

```bash
kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name my-topic --alter --add-config message.timestamp.type=LogAppendTime
```

### message.timestamp.after.max.ms

메시지 타임스탬프가 브로커 시간보다 늦을 때 허용되는 최대 차이입니다.

| 속성 | 값 |
|------|------|
| 기본값 | 3600000 (1시간) |

- 이 값을 초과하는 타임스탬프를 가진 메시지는 거부됩니다.

### message.timestamp.before.max.ms

메시지 타임스탬프가 브로커 시간보다 이를 때 허용되는 최대 차이입니다.

| 속성 | 값 |
|------|------|
| 기본값 | 9223372036854775807 (Long.MAX_VALUE) |

---

## 복제 및 내구성 설정

### min.insync.replicas

`acks=all` (또는 `acks=-1`)로 설정된 프로듀서의 쓰기가 성공하기 위해 필요한 최소 동기화 복제본(ISR) 수입니다.

| 속성 | 값 |
|------|------|
| 기본값 | 1 |
| 유효한 범위 | [1, ...] |

- 이 값이 충족되지 않으면 프로듀서는 `NotEnoughReplicasException`을 받습니다.
- 데이터 내구성을 보장하려면 `min.insync.replicas=2`와 `replication.factor=3`을 함께 사용하는 것이 일반적입니다.

```bash
kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name my-topic --alter --add-config min.insync.replicas=2
```

### unclean.leader.election.enable

ISR(In-Sync Replica) 세트에 없는 복제본이 최후의 수단으로 리더가 될 수 있는지 여부를 허용합니다.

| 속성 | 값 |
|------|------|
| 기본값 | false |

- `true`: 데이터 손실 가능성이 있지만 가용성이 향상됩니다.
- `false`: 데이터 손실을 방지하지만 ISR 복제본이 없으면 파티션을 사용할 수 없습니다.

> 주의: 이 설정을 `true`로 변경하면 데이터 손실이 발생할 수 있습니다.

### leader.replication.throttled.replicas

리더 측에서 스로틀링할 복제본 목록입니다.

| 속성 | 값 |
|------|------|
| 기본값 | "" (빈 문자열) |

- 형식: `[partition_id]:[broker_id],[partition_id]:[broker_id]...`

### follower.replication.throttled.replicas

팔로워 측에서 스로틀링할 복제본 목록입니다.

| 속성 | 값 |
|------|------|
| 기본값 | "" (빈 문자열) |

- 형식: `[partition_id]:[broker_id],[partition_id]:[broker_id]...`

---

## 플러싱 및 인덱싱 설정

### flush.messages

메시지 쓰기 후 fsync를 강제하는 간격입니다.

| 속성 | 값 |
|------|------|
| 기본값 | 9223372036854775807 (Long.MAX_VALUE) |
| 유효한 범위 | [1, ...] |

- 기본값은 OS의 백그라운드 플러시에 의존합니다.
- 낮은 값은 내구성을 높이지만 성능에 영향을 줍니다.

> 참고: 일반적으로 복제에 의존하는 것이 fsync에 의존하는 것보다 권장됩니다.

### flush.ms

fsync 작업을 강제하는 시간 간격입니다.

| 속성 | 값 |
|------|------|
| 기본값 | 9223372036854775807 (Long.MAX_VALUE) |
| 유효한 범위 | [0, ...] |

### index.interval.bytes

Kafka가 오프셋 인덱스에 항목을 추가하는 빈도를 제어합니다.

| 속성 | 값 |
|------|------|
| 기본값 | 4096 (4KB) |

- 이 바이트 수만큼 데이터가 추가될 때마다 인덱스 항목이 생성됩니다.
- 작은 값은 인덱스가 더 정밀해지지만 크기가 커집니다.

### file.delete.delay.ms

파일 시스템에서 파일을 삭제하기 전 대기 시간입니다.

| 속성 | 값 |
|------|------|
| 기본값 | 60000 (1분) |

---

## 계층형 스토리지 설정

계층형 스토리지(Tiered Storage)를 사용하면 오래된 데이터를 원격 스토리지(예: S3, GCS)로 이동할 수 있습니다.

### remote.storage.enable

토픽에 대해 계층형 스토리지를 활성화합니다.

| 속성 | 값 |
|------|------|
| 기본값 | false |

```bash
kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name my-topic --alter --add-config remote.storage.enable=true
```

### remote.log.copy.disable

계층형 데이터를 읽기 전용으로 만들어 업로드를 비활성화합니다.

| 속성 | 값 |
|------|------|
| 기본값 | false |

- `true`로 설정하면 새 데이터가 원격 스토리지로 복사되지 않습니다.

### remote.log.delete.on.disable

계층형 스토리지가 비활성화될 때 계층형 데이터를 삭제합니다.

| 속성 | 값 |
|------|------|
| 기본값 | false |

### local.retention.ms

로컬 로그가 삭제되기 전에 유지되는 시간(밀리초)입니다.

| 속성 | 값 |
|------|------|
| 기본값 | -2 |

- `-2`: `retention.ms` 값을 사용합니다.
- `-1`: 무제한 로컬 보존입니다.
- 계층형 스토리지가 활성화된 경우에만 적용됩니다.

### local.retention.bytes

로컬 로그의 최대 크기입니다.

| 속성 | 값 |
|------|------|
| 기본값 | -2 |

- `-2`: `retention.bytes` 값을 사용합니다.
- `-1`: 무제한 로컬 보존입니다.
- 계층형 스토리지가 활성화된 경우에만 적용됩니다.

---

## 기타 설정

### preallocate

새 로그 세그먼트에 대해 디스크의 파일을 미리 할당할지 여부입니다.

| 속성 | 값 |
|------|------|
| 기본값 | false |

- `true`로 설정하면 세그먼트 생성 시 전체 크기가 미리 할당됩니다.
- 일부 파일 시스템에서 성능을 향상시킬 수 있습니다.

---

## 설정 관리 방법

### 토픽 생성 시 설정 적용

```bash
kafka-topics.sh --bootstrap-server localhost:9092 --create --topic my-topic \
  --partitions 3 \
  --replication-factor 2 \
  --config retention.ms=604800000 \
  --config cleanup.policy=compact
```

### 기존 토픽 설정 변경

```bash
kafka-configs.sh --bootstrap-server localhost:9092 \
  --entity-type topics \
  --entity-name my-topic \
  --alter \
  --add-config retention.ms=259200000,cleanup.policy=delete
```

### 토픽 설정 조회

```bash
kafka-configs.sh --bootstrap-server localhost:9092 \
  --entity-type topics \
  --entity-name my-topic \
  --describe
```

### 설정 삭제 (기본값으로 복원)

```bash
kafka-configs.sh --bootstrap-server localhost:9092 \
  --entity-type topics \
  --entity-name my-topic \
  --alter \
  --delete-config retention.ms
```

---

## 요약

Kafka 토픽 설정은 토픽별로 데이터 보존, 압축, 복제, 성능 등을 세밀하게 제어할 수 있게 합니다. 주요 포인트는 다음과 같습니다:

1. 보존 정책: `retention.ms`와 `retention.bytes`로 데이터 보존 기간과 크기를 제어합니다.
2. 정리 정책: `cleanup.policy`로 삭제(delete) 또는 컴팩션(compact) 방식을 선택합니다.
3. 복제 내구성: `min.insync.replicas`로 쓰기 내구성을 보장합니다.
4. 압축: `compression.type`으로 저장 효율성을 높일 수 있습니다.
5. 세그먼트 관리: `segment.bytes`와 `segment.ms`로 세그먼트 크기와 롤링 주기를 제어합니다.
6. 계층형 스토리지: `remote.storage.enable`로 비용 효율적인 장기 보관이 가능합니다.

---

## 참고 자료

- [Apache Kafka Documentation - Topic Configs](https://kafka.apache.org/documentation/#topicconfigs)
- [Apache Kafka Documentation - Broker Configs](https://kafka.apache.org/documentation/#brokerconfigs)
- [Apache Kafka Documentation - Producer Configs](https://kafka.apache.org/documentation/#producerconfigs)
- [Apache Kafka Documentation - Consumer Configs](https://kafka.apache.org/documentation/#consumerconfigs)

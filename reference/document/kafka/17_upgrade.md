# Kafka 업그레이드

> 이 문서는 Apache Kafka 공식 문서의 "Upgrade" 섹션을 한국어로 번역한 것입니다.
> 원본: https://kafka.apache.org/documentation/#upgrade

## 개요

이 가이드는 Apache Kafka를 새로운 버전으로 업그레이드하는 방법을 설명합니다. 롤링 업그레이드(Rolling Upgrade)를 통해 클러스터 가용성을 유지하면서 업그레이드할 수 있습니다.

---

## 1. 업그레이드 경로 개요

### 1.1 지원되는 업그레이드 버전

다음 주요 버전에 대한 업그레이드가 지원됩니다:

| 대상 버전 | 주요 변경 사항 |
|-----------|----------------|
| 4.1.x | Queues for Kafka (Share Groups) 도입 |
| 4.0.x | KRaft 전용 모드, ZooKeeper 지원 제거 |
| 3.9.x | KRaft 안정화, 메타데이터 버전 관리 개선 |
| 3.x.x | KRaft 모드 도입 및 점진적 안정화 |

---

## 2. 일반 롤링 업그레이드 절차

대부분의 업그레이드에서 표준 프로세스는 다음과 같습니다:

### 2.1 ZooKeeper 기반 클러스터 (3.x 이하)

1. 설정 업데이트: `inter.broker.protocol.version`을 현재 버전으로 설정하고, `log.message.format.version`을 기존 메시지 형식으로 설정합니다.

2. 브로커 업그레이드: 브로커를 순차적으로 업그레이드합니다 - 종료, 코드 업데이트, 재시작

3. 프로토콜 버전 업데이트: 클러스터 상태를 확인한 후 `inter.broker.protocol.version`을 대상 버전으로 업데이트합니다.

4. 브로커 재시작: 새 프로토콜이 적용되도록 각 브로커를 개별적으로 재시작합니다.

5. 메시지 형식 업데이트: 이전에 재정의한 경우, 메시지 형식을 업그레이드하기 위해 한 번 더 롤링 재시작을 수행합니다.

### 2.2 KRaft 기반 클러스터 (3.3.0 이상)

KRaft 클러스터는 더 이상 다단계 프로토콜 버전 업데이트가 필요하지 않습니다:

1. 브로커 업그레이드: 브로커를 순차적으로 업그레이드합니다 (종료 -> 업데이트 -> 재시작)

2. 메타데이터 버전 업그레이드: 클러스터 안정성이 확인되면 다음 명령을 실행합니다:

```bash
bin/kafka-features.sh --bootstrap-server localhost:9092 upgrade --release-version <버전>
```

예시:
```bash
bin/kafka-features.sh --bootstrap-server localhost:9092 upgrade --release-version 4.0
```

또는 메타데이터 버전만 지정:
```bash
bin/kafka-features.sh upgrade --metadata 3.9
```

---

## 3. 버전별 업그레이드 가이드

### 3.1 4.1.x로 업그레이드

#### 4.1.1 업그레이드

4.1.1 버전은 다음과 같은 중요한 Kafka Streams 버그를 해결합니다:
- 범위 스캔(Range Scan)에서의 메모리 누수
- 데이터 손실 문제

#### 4.1.0 업그레이드

새로운 기능: Queues for Kafka (Share Groups)

4.1.0은 "Queues for Kafka"를 도입하여 Share Groups를 Consumer Groups의 대안으로 제공합니다. 파티션 할당 없이 협력적인 레코드 소비가 가능합니다.

### 3.2 4.0.x로 업그레이드

#### 중요 요구 사항

KRaft 모드 필수: Apache Kafka 4.0은 KRaft 모드만 지원합니다. ZooKeeper 지원이 완전히 제거되었습니다.

> 경고: 4.0.0 이상으로 브로커를 업그레이드하려면 KRaft 모드가 필요하며, 소프트웨어 및 메타데이터 버전이 최소 3.3.x 이상이어야 합니다.

#### 클라이언트 요구 사항

클라이언트(Streams 및 Connect 포함)는 4.0으로 업그레이드하기 전에 버전 2.1 이상이어야 합니다.

#### 롤링 업그레이드 절차

1. 브로커를 개별적으로 업그레이드합니다 (종료 -> 업데이트 -> 재시작)

2. 클러스터 안정성이 확인되면 다음 명령으로 완료합니다:

```bash
bin/kafka-features.sh --bootstrap-server localhost:9092 upgrade --release-version 4.0
```

3. 상태 변경 로그 파일 로테이션이 `stage-change.log.[date]`에서 `state-change.log.[date]`로 수정되었습니다.

#### 주요 호환성 변경 사항

프로토콜 및 호환성:
- 이전 프로토콜 API 버전이 제거됨; 브로커와 클라이언트 모두 2.1 이상이어야 함
- Java 요구 사항 증가: 클라이언트/Streams는 Java 11 필요; 브로커/Connect/도구는 Java 17 필요
- Scala 2.12 지원 중단

설정 제거:
- `log.message.format.version` 및 `message.format.version` 제거
- ZooKeeper 전용 도구 및 보안 마이그레이터 제거
- 여러 더 이상 사용되지 않는 메트릭 및 클래스 제거

Consumer/Producer 변경:
- `poll(long)` 메서드 제거; 대신 `poll(Duration)` 사용

```java
// 이전 (제거됨)
consumer.poll(100);

// 새로운 방식
consumer.poll(Duration.ofMillis(100));
```

- 기본 `linger.ms`가 0에서 5밀리초로 변경
- `enable.idempotence`가 더 이상 `max.in.flight.requests.per.connection`을 자동 조정하지 않음

Kafka Streams 변경:
- `KStream#transformValues()` 제거; `KStreams#processValues()`로 마이그레이션 필요

```java
// 이전 (제거됨)
stream.transformValues(...)

// 새로운 방식
stream.processValues(...)
```

- 3.6 이전의 모든 더 이상 사용되지 않는 API 제거

로깅 변경:
- Log4j에서 Log4j2로 마이그레이션

MirrorMaker 변경:
- 기존 MirrorMaker (MM1) 제거; Connect 기반 MM2 사용

새로운 기능 활성화:
- 차세대 Consumer Rebalance Protocol (KIP-848)이 GA(Generally Available)됨
- Transactions Server Side Defense (KIP-890)가 트랜잭션 보장 강화
- Eligible Leader Replicas가 복제 프로토콜 개선

### 3.3 3.9.x로 업그레이드

#### 주요 변경 사항

- 더 이상 사용되지 않는 프로토콜 API 버전에 대한 요청 로깅이 DEBUG에서 INFO 레벨로 변경됨
- 선택적 요청 로깅 구성 활성화

#### 업그레이드 명령

```bash
bin/kafka-features.sh upgrade --metadata 3.9
```

### 3.4 3.8.x로 업그레이드

#### ZooKeeper 요구 사항

ZooKeeper를 버전 3.8.3 이상으로 업그레이드해야 합니다.

#### 압축 라이브러리 설정

플랫폼에서 `/tmp` 파티션에 실행 권한이 필요합니다. 사용할 수 없는 경우 JVM 플래그를 설정합니다:

```bash
-DZstdTempFolder=/opt/kafka/tmp -Dorg.xerial.snappy.tempdir=/opt/kafka/tmp
```

### 3.5 3.0.x 이상으로 업그레이드

- `log.message.format.version` 설정은 더 이상 사용되지 않음
- `inter.broker.protocol.version`이 3.0 이상인 경우 두 설정 모두 항상 3.0으로 가정됨

### 3.6 2.1.x 이상으로 업그레이드

Consumer 오프셋 저장 스키마가 영구적으로 변경되었습니다. 일단 업그레이드하면 2.1 이전 버전으로 다운그레이드할 수 없습니다.

---

## 4. 클라이언트 업그레이드

### 4.1 롤링 클라이언트 업그레이드

롤링 업그레이드는 클라이언트를 개별적으로 종료하고, 코드를 업데이트한 후 재시작하여 진행합니다.

```bash
# 1. 클라이언트 종료
# 2. 코드 업데이트
# 3. 순차적으로 재시작
```

### 4.2 클라이언트 호환성

| 클라이언트 버전 | 최소 브로커 버전 | 비고 |
|-----------------|------------------|------|
| 4.0.x | 2.1.x | KRaft 모드 권장 |
| 3.x.x | 0.10.0.0 | ZooKeeper 또는 KRaft |
| 2.x.x | 0.10.0.0 | ZooKeeper |

---

## 5. 성능 고려 사항

### 5.1 메시지 형식 변환

0.10.0.0에서 업그레이드하면 메시지당 타임스탬프 필드가 도입됩니다. 메시지 형식 변환이 광범위하게 실행되면 "CPU 사용률이 업그레이드 전 20%에서 업그레이드 후 100%"로 증가할 수 있습니다.

Consumer가 업그레이드될 때까지 `log.message.format.version` 업데이트를 지연하여 비용이 많이 드는 브로커 측 변환을 방지하세요.

### 5.2 압축 메시지

0.11.0 버전에서 압축 메시지의 경우, snappy가 1KB 대신 압축 스킴의 기본 블록 크기(2 x 32KB)를 사용합니다. 이는 압축 비율을 개선하지만 힙 요구 사항이 비례하여 증가합니다.

---

## 6. 다운그레이드 제한 사항

### 6.1 일반적인 제한

대부분의 주요 버전 업그레이드는 프로토콜 버전이 완료되면 다운그레이드를 방지합니다.

### 6.2 KRaft 클러스터 제한

3.3-IV0 이후의 KRaft 클러스터는 메타데이터 스키마 변경으로 인해 다운그레이드가 명시적으로 방지됩니다.

> 경고: 이 소프트웨어 버전에서는 메타데이터 변경이 있으므로 클러스터 메타데이터 다운그레이드가 지원되지 않습니다.

---

## 7. ZooKeeper에서 KRaft로 마이그레이션

### 7.1 마이그레이션 개요

Kafka 4.0부터 ZooKeeper 지원이 완전히 제거되었으므로, 4.0 이상으로 업그레이드하기 전에 KRaft로 마이그레이션해야 합니다.

### 7.2 마이그레이션 전제 조건

1. 현재 클러스터가 3.3.x 이상에서 실행 중이어야 합니다
2. 모든 클라이언트가 2.1 이상이어야 합니다
3. 메타데이터 버전이 3.3.x 이상이어야 합니다

### 7.3 마이그레이션 단계

1. KRaft 컨트롤러 배포: 새로운 KRaft 컨트롤러 노드를 배포합니다

2. 마이그레이션 모드 활성화: 브로커 설정에서 마이그레이션 모드를 활성화합니다

3. 메타데이터 마이그레이션: ZooKeeper 메타데이터를 KRaft로 마이그레이션합니다

4. ZooKeeper 의존성 제거: 마이그레이션이 완료되면 ZooKeeper 의존성을 제거합니다

5. 마이그레이션 완료: 클러스터가 완전히 KRaft 모드로 전환됩니다

---

## 8. 버전별 주요 변경 사항 요약

### 8.1 4.0.0 주요 변경 사항

| 영역 | 변경 사항 |
|------|-----------|
| 플랫폼 | ZooKeeper 지원 제거 |
| Java | 클라이언트: Java 11, 브로커: Java 17 필수 |
| Scala | 2.12 지원 중단 |
| API | 여러 더 이상 사용되지 않는 API 제거 |
| 로깅 | Log4j2로 마이그레이션 |
| 도구 | MirrorMaker 1 제거 |

### 8.2 3.2.0 주요 변경 사항

- Producer 멱등성(idempotence)이 기본적으로 활성화됨
- 이전 브로커에서 `IDEMPOTENT_WRITE` 권한 필요

### 8.3 3.0.0 주요 변경 사항

- 더 이상 사용되지 않는 Scala 클라이언트 제거
- 새 메시지 형식에는 Java 클라이언트 필수

### 8.4 2.4.0 주요 변경 사항

- 기본 파티셔너가 스티키 할당 전략 사용
- 배치 분배에 영향

### 8.5 2.0.0 주요 변경 사항

- Scala consumer/producer 제거
- Java 클라이언트 필수

### 8.6 0.11.0 주요 변경 사항

- 새 메시지 형식에 타임스탬프 포함
- 이전 Scala 클라이언트는 성능 오버헤드 없이 읽을 수 없음

---

## 9. 업그레이드 체크리스트

업그레이드를 수행하기 전에 다음 사항을 확인하세요:

- [ ] 대상 버전의 릴리스 노트 검토
- [ ] 클라이언트 호환성 확인
- [ ] Java 버전 요구 사항 충족 확인
- [ ] 백업 수행
- [ ] 스테이징 환경에서 테스트
- [ ] 롤백 계획 수립
- [ ] 모니터링 및 알림 설정

---

## 10. 문제 해결

### 10.1 업그레이드 후 클러스터가 불안정한 경우

1. 모든 브로커가 동일한 버전을 실행하는지 확인
2. 메타데이터 버전이 올바르게 설정되었는지 확인
3. 로그에서 오류 메시지 확인

### 10.2 클라이언트 연결 문제

1. 클라이언트 버전이 브로커와 호환되는지 확인
2. 프로토콜 버전 설정 확인
3. 네트워크 연결 상태 확인

### 10.3 성능 저하

1. 메시지 형식 변환이 발생하는지 확인
2. `log.message.format.version` 설정 검토
3. JVM 힙 크기 조정 고려

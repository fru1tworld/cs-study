# Kafka KRaft 모드

> 이 문서는 Apache Kafka 공식 문서의 "KRaft" 섹션을 한국어로 번역한 것입니다.
> 원본: https://kafka.apache.org/documentation/#kraft

## 개요

KRaft는 Apache Kafka의 메타데이터 관리 시스템으로, ZooKeeper를 대체합니다. 이 문서는 KRaft의 설정, 업그레이드, 노드 프로비저닝, 운영 방법 및 ZooKeeper에서 KRaft로의 마이그레이션 절차를 설명합니다.

---

## 1. 설정 (Configuration)

### 프로세스 역할 (Process Roles)

`process.roles` 속성으로 각 서버의 역할을 정의합니다:

| 역할 | 설명 |
|------|------|
| `broker` | 브로커로만 동작 |
| `controller` | 컨트롤러로만 동작 |
| `broker,controller` | 두 역할 모두 수행 ("combined" 서버) |

> 참고: Combined 서버는 개발 환경과 같은 소규모 사용 사례에서는 운영하기 더 간단하지만, 프로덕션 환경에서는 권장하지 않습니다.

### 컨트롤러 구성

메타데이터 쿼럼(Metadata Quorum)에 참여할 컨트롤러를 선택합니다:

- 일반적으로 3개 또는 5개 서버 선택
- 3개 컨트롤러: 1개 장애 허용
- 5개 컨트롤러: 2개 장애 허용

필수 설정:

```properties
controller.quorum.bootstrap.servers=host1:port1,host2:port2,host3:port3
```

컨트롤러 구성 예시:

```properties
process.roles=controller
node.id=1
listeners=CONTROLLER://controller1.example.com:9093
controller.quorum.bootstrap.servers=controller1.example.com:9093,controller2.example.com:9093,controller3.example.com:9093
controller.listener.names=CONTROLLER
```

---

## 2. 업그레이드 (Upgrade)

### KRaft 버전 확인

```bash
bin/kafka-features.sh --bootstrap-controller localhost:9093 describe
```

동적 컨트롤러 구성은 `kraft.version=1` (Kafka 4.1 이상)에서 지원됩니다.

### KRaft 버전 업그레이드

```bash
bin/kafka-features.sh --bootstrap-server localhost:9092 upgrade --release-version 4.1
```

### 설정 업데이트

`kraft.version` 1 이상으로 업그레이드 후:

- `controller.quorum.voters` 제거
- `controller.quorum.bootstrap.servers` 추가

---

## 3. 노드 프로비저닝 (Provisioning Nodes)

### 클러스터 ID 생성

```bash
bin/kafka-storage.sh random-uuid
```

### 독립 컨트롤러 부트스트랩

```bash
bin/kafka-storage.sh format --cluster-id <CLUSTER_ID> --standalone --config config/controller.properties
```

이 명령은 `meta.properties` 파일과 초기 스냅샷을 생성합니다.

### 다중 컨트롤러로 부트스트랩

```bash
CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
CONTROLLER_0_UUID="$(bin/kafka-storage.sh random-uuid)"
CONTROLLER_1_UUID="$(bin/kafka-storage.sh random-uuid)"
CONTROLLER_2_UUID="$(bin/kafka-storage.sh random-uuid)"

bin/kafka-storage.sh format --cluster-id ${CLUSTER_ID} \
  --initial-controllers "0@controller-0:1234:${CONTROLLER_0_UUID},1@controller-1:1234:${CONTROLLER_1_UUID},2@controller-2:1234:${CONTROLLER_2_UUID}" \
  --config config/controller.properties
```

### 브로커 및 새 컨트롤러 포맷

```bash
bin/kafka-storage.sh format --cluster-id <CLUSTER_ID> --config config/server.properties --no-initial-controllers
```

---

## 4. 컨트롤러 멤버십 변경 (Controller Membership Changes)

### 정적 vs 동적 쿼럼

| 유형 | 설명 |
|------|------|
| 정적 (Static) | `controller.quorum.voters`로 모든 컨트롤러 명시 |
| 동적 (Dynamic) | `controller.quorum.bootstrap.servers` 사용 (KIP-853) |

### 새 컨트롤러 추가

```bash
bin/kafka-metadata-quorum.sh --bootstrap-server localhost:9092 add-controller
```

또는 컨트롤러 엔드포인트 사용:

```bash
bin/kafka-metadata-quorum.sh --bootstrap-controller localhost:9093 add-controller
```

### 컨트롤러 제거

```bash
bin/kafka-metadata-quorum.sh --bootstrap-server localhost:9092 remove-controller --controller-id <id> --controller-directory-id <directory-id>
```

---

## 5. 디버깅 (Debugging)

### 메타데이터 쿼럼 도구

```bash
bin/kafka-metadata-quorum.sh --bootstrap-server localhost:9092 describe --status
```

출력 예시:

```
ClusterId:              fMCL8kv1SWm87L_Md-I2hg
LeaderId:               3002
LeaderEpoch:            2
HighWatermark:          10
```

### 로그 덤프 도구

```bash
bin/kafka-dump-log.sh --cluster-metadata-decoder --files metadata_log_dir/__cluster_metadata-0/00000000000000000000.log
```

### 메타데이터 셸

```bash
bin/kafka-metadata-shell.sh --snapshot metadata_log_dir/__cluster_metadata-0/00000000000000007228-0000000001.checkpoint
```

---

## 6. 배포 고려사항 (Deploying Considerations)

KRaft 모드로 클러스터를 배포할 때 다음 사항을 고려해야 합니다:

- `process.roles`를 `broker` 또는 `controller`로 설정 (프로덕션에서는 둘 다 아님)
- 고가용성을 위해 3개 이상의 컨트롤러 권장
- N개 장애 허용 시 2N+1개 컨트롤러 필요
- 각 컨트롤러: 약 5GB 메모리, 5GB 메타데이터 로그 디스크 공간 권장

---

## 7. ZooKeeper에서 KRaft로의 마이그레이션

> 중요: ZooKeeper에서 KRaft로 마이그레이션하려면 브릿지 릴리스(Bridge Release)를 사용해야 합니다. 마지막 브릿지 릴리스는 Kafka 3.9입니다.

### 용어 정의

| 용어 | 설명 |
|------|------|
| ZK 모드 | 브로커가 Apache ZooKeeper에 메타데이터를 저장하는 기존 방식 |
| KRaft 모드 | 브로커가 KRaft 쿼럼에 메타데이터를 저장하는 신규 방식 |
| 마이그레이션 | ZooKeeper의 클러스터 메타데이터를 KRaft 쿼럼으로 이동하는 프로세스 |

### 마이그레이션 단계

마이그레이션은 5가지 주요 단계를 거칩니다:

1. 초기 단계: 모든 브로커가 ZK 모드, ZK 기반 컨트롤러 운영
2. 메타데이터 로드: KRaft 쿼럼이 ZooKeeper에서 메타데이터 로드
3. 하이브리드 단계: 일부 브로커는 ZK 모드, 컨트롤러는 KRaft 모드
4. 듀얼-라이트 단계 (Dual-Write): 모든 브로커가 KRaft이지만 컨트롤러는 ZK에도 기록
5. 완료 단계: 더 이상 ZooKeeper에 메타데이터를 기록하지 않음

### 주요 제약사항

- 마이그레이션 중 메타데이터 버전(`inter.broker.protocol.version`) 변경 불가능
- 완료 후 ZooKeeper 모드로의 복귀 불가능
- 다중 로그 디렉토리를 가진 ZK 브로커는 디렉토리 장애 시 종료됨

---

## 8. 마이그레이션 준비

### 소프트웨어 요구사항

브로커를 버전 3.8.1로 업그레이드하고 `inter.broker.protocol.version`을 `3.8`로 설정합니다.

### 로깅 설정

KRaft 컨트롤러의 `log4j.properties`에 다음을 추가합니다:

```properties
log4j.logger.org.apache.kafka.metadata.migration=TRACE
```

---

## 9. KRaft 컨트롤러 쿼럼 프로비저닝

### 기존 클러스터의 클러스터 ID 확인

```bash
bin/zookeeper-shell.sh localhost:2181 get /cluster/id
```

### 마이그레이션 대비 KRaft 컨트롤러 샘플 설정

```properties
process.roles=controller
node.id=3000
controller.quorum.voters=3000@localhost:9093
controller.listener.names=CONTROLLER
listeners=CONTROLLER://:9093
zookeeper.metadata.migration.enable=true
zookeeper.connect=localhost:2181
inter.broker.listener.name=PLAINTEXT
```

> 중요: KRaft 클러스터의 `node.id` 값은 기존 ZK 브로커의 `broker.id`와 달라야 합니다.

---

## 10. 브로커 마이그레이션 모드 진입

각 브로커에 다음 설정을 추가합니다:

- `controller.quorum.voters`
- `controller.listener.names`
- `zookeeper.metadata.migration.enable`

### 마이그레이션 대비 ZK 브로커 샘플 설정

```properties
broker.id=0
listeners=PLAINTEXT://:9092
advertised.listeners=PLAINTEXT://localhost:9092
listener.security.protocol.map=PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
inter.broker.protocol.version=3.8
zookeeper.metadata.migration.enable=true
zookeeper.connect=localhost:2181
controller.quorum.voters=3000@localhost:9093
controller.listener.names=CONTROLLER
```

모든 ZK 브로커 재시작 시 자동으로 마이그레이션이 시작됩니다.

완료 신호 (컨트롤러 로그에서 확인):

```
Completed migration of metadata from Zookeeper to KRaft
```

---

## 11. 브로커를 KRaft로 마이그레이션

각 브로커를 다음 설정으로 변경하고 재시작합니다:

```properties
process.roles=broker
node.id=0
listeners=PLAINTEXT://:9092
advertised.listeners=PLAINTEXT://localhost:9092
listener.security.protocol.map=PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
controller.quorum.voters=3000@localhost:9093
controller.listener.names=CONTROLLER
```

### 제거할 설정

- `zookeeper.metadata.migration.enable`
- `inter.broker.protocol.version`
- `zookeeper` 관련 설정들
- `control.plane.listener.name`

### 권한 설정 변경

```
kafka.security.authorizer.AclAuthorizer → org.apache.kafka.metadata.authorizer.StandardAuthorizer
```

---

## 12. 마이그레이션 최종화

모든 브로커가 KRaft 모드로 전환된 후, 각 KRaft 컨트롤러에서 마이그레이션 플래그를 제거합니다:

```properties
# 다음 설정을 제거합니다:
# zookeeper.metadata.migration.enable=true
# zookeeper.connect=localhost:2181
```

컨트롤러를 한 번에 하나씩 재시작합니다.

완료 후 ZooKeeper 클러스터는 안전하게 폐기할 수 있습니다.

---

## 13. 마이그레이션 중 ZooKeeper 모드로의 복귀

완료 단계별 롤백 절차:

| 완료 단계 | 롤백 방법 |
|-----------|-----------|
| 마이그레이션 준비 | 작업 없음 |
| KRaft 쿼럼 프로비저닝 | KRaft 쿼럼 폐기 |
| 브로커 마이그레이션 모드 진입 | KRaft 쿼럼 폐기 후 `zookeeper-shell.sh`에서 `rmr /controller` 실행, 브로커 설정 되돌려 재시작 |
| 브로커를 KRaft로 마이그레이션 | 브로커를 ZK 설정으로 복원하여 2회 재시작, `rmr /controller` 실행 |
| 마이그레이션 최종화 | 복귀 불가능 |

> 주의: `/controller` 제거 시 빠르게 수행하여 컨트롤러 없는 시간을 최소화해야 합니다.

> 권장 사항: 일부 사용자는 ZooKeeper 클러스터를 유지하면서 1-2주 대기 후 최종화하는 것을 권장합니다.

---

## 요약

### KRaft 모드의 장점

| 장점 | 설명 |
|------|------|
| 단순화된 아키텍처 | ZooKeeper 의존성 제거로 운영 복잡성 감소 |
| 향상된 확장성 | 더 많은 파티션 지원 가능 |
| 빠른 복구 | 컨트롤러 장애 조치 시간 단축 |
| 단일 보안 모델 | Kafka 자체 보안 메커니즘만 관리 |

### 마이그레이션 체크리스트

- [ ] Kafka 브릿지 릴리스(3.9) 사용
- [ ] `inter.broker.protocol.version` 설정 확인
- [ ] KRaft 컨트롤러 쿼럼 프로비저닝
- [ ] 브로커 마이그레이션 모드 설정
- [ ] 브로커 KRaft 모드 전환
- [ ] 마이그레이션 최종화
- [ ] ZooKeeper 클러스터 폐기

---

## 참고 자료

- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [KRaft 운영 가이드](https://kafka.apache.org/documentation/#kraft)
- [KIP-500: Replace ZooKeeper with a Self-Managed Metadata Quorum](https://cwiki.apache.org/confluence/display/KAFKA/KIP-500%3A+Replace+ZooKeeper+with+a+Self-Managed+Metadata+Quorum)
- [KIP-853: Dynamic Quorum Reconfiguration](https://cwiki.apache.org/confluence/display/KAFKA/KIP-853%3A+KRaft+Controller+Membership+Changes)

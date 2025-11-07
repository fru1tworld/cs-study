# Kafka / 카프카

> 카테고리: 메시징 시스템
> [← 면접 질문 목록으로 돌아가기](../interview.md)

---

## 📌 Kafka 기본 아키텍처

### KAFKA-001
Kafka의 기본 아키텍처와 주요 컴포넌트(Producer, Broker, Consumer, Topic 등)에 대해 설명해주세요.

### KAFKA-002
Kafka Broker의 역할과 주요 기능은 무엇인가요?

### KAFKA-003
Producer와 Consumer의 차이점 및 역할에 대해 설명해 주세요.

### KAFKA-004
Kafka에서 Partition과 Offset의 개념 및 활용 방법은 무엇인가요?

---

## 📌 Kafka 메시지 보존과 처리

### KAFKA-005
Kafka의 메시지 보존 정책(retention policy)은 어떻게 작동하나요?

### KAFKA-006
Consumer Group의 개념과 이를 통해 메시지 병렬 처리를 어떻게 구현하는지 설명해주세요.

---

## 📌 Kafka 고가용성

### KAFKA-007
Kafka에서 리플리케이션(replication)의 필요성과 설정 방법은 무엇인가요?

### KAFKA-008
Kafka 클러스터의 장애 복구(failover) 메커니즘에 대해 설명해주세요.

---

## 📌 Kafka Connect와 Streams

### KAFKA-009
Kafka Connect의 역할과 이를 활용한 데이터 파이프라인 구축 방법은 무엇인가요?

### KAFKA-010
Kafka Streams와 KSQL의 차이점 및 각각의 사용 사례에 대해 설명해주세요.

---

## 📌 Kafka 메시지 전달 보장

### KAFKA-011
Kafka에서 Exactly-Once Semantics를 구현하는 방법에 대해 설명해주세요.

### KAFKA-012
Producer 측에서 발생할 수 있는 메시지 중복 문제를 어떻게 해결할 수 있나요?

### KAFKA-013
Kafka Consumer가 재시작될 때 오프셋(offset) 관리를 어떻게 수행하나요?

---

## 📌 Kafka 로그 관리

### KAFKA-014
Kafka 로그 컴팩션(log compaction)이란 무엇이며, 어떤 상황에서 사용되나요?

---

## 📌 Kafka 성능 튜닝

### KAFKA-015
Kafka 성능 튜닝을 위한 주요 고려 사항에는 어떤 것들이 있나요?

### KAFKA-016
Kafka 클러스터 구성 시 네트워크 및 하드웨어 설정에서 고려해야 할 점은 무엇인가요?

### KAFKA-017
Kafka에서 데이터 손실을 방지하기 위한 전략은 무엇인가요?

---

## 📌 Kafka 보안

### KAFKA-018
Kafka의 ACL(Access Control List) 및 보안 설정 방법에 대해 설명해주세요.

### KAFKA-019
SSL/TLS 및 SASL을 사용한 Kafka 보안 구성 방법에 대해 설명해주세요.

---

## 📌 Kafka 클러스터 안정성

### KAFKA-020
Kafka Broker 재시작 시 클러스터 안정성을 유지하는 방법은 무엇인가요?

### KAFKA-021
In-Sync Replica(ISR)의 역할과 중요성은 무엇인가요?

---

## 📌 Kafka 메시지 순서 보장

### KAFKA-022
Kafka에서 메시지의 순서를 보장하는 방법은 무엇인가요?

### KAFKA-023
Producer의 ACK 설정 옵션(0, 1, all)의 차이점과 의미에 대해 설명해주세요.

---

## 📌 Kafka vs 다른 메시징 시스템

### KAFKA-024
Kafka와 RabbitMQ 같은 다른 메시징 시스템의 주요 차이점은 무엇인가요?

---

## 📌 Kafka 배포와 처리 패턴

### KAFKA-025
Kafka 클러스터의 Zero Downtime 배포 전략에 대해 설명해주세요.

### KAFKA-026
스트림 처리와 배치 처리의 차이점을 Kafka 관점에서 설명해주세요.

### KAFKA-027
Kafka에서 멀티 테넌시(Multi-Tenancy)를 어떻게 지원하나요?

---

## 📌 Kafka 모니터링

### KAFKA-028
Kafka 클러스터 모니터링을 위한 주요 지표와 사용 도구에는 어떤 것들이 있나요?

### KAFKA-029
Consumer Rebalance 과정과 이를 최적화하기 위한 방법은 무엇인가요?

---

## 📌 Kafka Producer/Consumer 최적화

### KAFKA-030
Producer 성능 병목 현상 발생 시 해결 전략은 무엇인가요?

---

## 📌 Kafka ZooKeeper와 KRaft

### KAFKA-031
ZooKeeper의 역할과 KRaft 모드의 차이점에 대해 설명해주세요.

---

## 📌 Kafka 메시지 압축

### KAFKA-032
Kafka의 메시지 압축 옵션(gzip, snappy, lz4 등)의 장단점은 무엇인가요?

---

## 📌 Kafka 에러 처리

### KAFKA-033
메시지 처리 중 오류가 발생했을 때의 처리 전략(예: DLQ 도입 등)에 대해 설명해주세요.

---

## 📌 Kafka 확장성

### KAFKA-034
Kafka 클러스터 확장(Scale-out) 시 고려해야 할 모범 사례는 무엇인가요?

### KAFKA-035
Kafka에서 KRaft 모드 전환 시 고려해야 할 사항은 무엇인가요?

---

## 📌 Kafka 지연(Latency) 최적화

### KAFKA-036
Kafka 메시지 전송 지연(latency)을 최소화하는 방법에는 어떤 것들이 있나요?

### KAFKA-037
Sync와 Async 전송 방식의 차이점과 각각의 장단점은 무엇인가요?

---

## 📌 Kafka 장애 대응

### KAFKA-038
Kafka 클러스터 운영 시 예상할 수 있는 장애와 그에 대한 대응 방안은 무엇인가요?

### KAFKA-039
Consumer Lag(지연) 모니터링 방법과 이를 해결하기 위한 전략은 무엇인가요?

---

## 📌 Kafka 일관성

### KAFKA-040
Kafka에서 데이터 일관성을 보장하는 방법은 무엇인가요?

---

## 📌 Kafka Producer 설정

### KAFKA-041
Producer의 배치 전송(batch sending) 설정이 성능에 미치는 영향에 대해 설명해주세요.

---

## 📌 Kafka Consumer Offset

### KAFKA-042
Kafka Consumer의 오프셋 커밋 전략(자동 vs 수동 커밋)에 대해 설명해주세요.

---

## 📌 Kafka Dead Letter Queue

### KAFKA-043
Dead Letter Queue(DLQ)를 Kafka에서 구현하는 방법은 무엇인가요?

---

## 📌 Kafka Schema Registry

### KAFKA-044
Kafka 메시지 스키마 관리(Schema Registry)의 역할과 필요성은 무엇인가요?

---

## 📌 Kafka 데이터베이스 연동

### KAFKA-045
Kafka와 NoSQL 데이터베이스를 연동할 때 고려해야 할 사항은 무엇인가요?

---

## 📌 Kafka Streams State Store

### KAFKA-046
Kafka Streams의 상태 저장소(State Store) 관리 방식에 대해 설명해주세요.

---

## 📌 Kafka 마이크로서비스

### KAFKA-047
Kafka를 활용한 마이크로서비스 아키텍처 설계 사례에 대해 설명해주세요.

---

## 📌 Kafka 브로커 추가

### KAFKA-048
Kafka 클러스터에 새로운 브로커를 추가할 때 고려해야 할 주요 요소는 무엇인가요?

---

## 📌 Kafka Connector 개발

### KAFKA-049
Kafka 커넥터(Connector) 개발 및 커스터마이징 방법에 대해 설명해주세요.

---

## 📌 Kafka 운영 지표

### KAFKA-050
Kafka 운영 시 모니터링과 경보 시스템 설정 시 중요한 핵심 지표는 무엇인가요?

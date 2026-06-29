# Kafka 사용 사례

> 원본: https://kafka.apache.org/documentation/#uses

## 개요

Apache Kafka는 다양한 사용 사례에 적용 가능한 범용 분산 스트리밍 플랫폼입니다. 아래는 Kafka의 주요 사용 사례입니다.

---

## 1. 메시징 (Messaging)

Kafka는 전통적인 메시지 브로커(Message Broker)를 대체하는 용도로 잘 작동합니다. 메시지 브로커는 데이터 생산자와 처리를 분리하거나, 처리되지 않은 메시지를 버퍼링하는 등 다양한 목적으로 사용됩니다. 대부분의 메시징 시스템과 비교할 때 Kafka는 더 높은 처리량(Throughput), 내장 파티셔닝(Partitioning), 복제(Replication), 내결함성(Fault-tolerance)을 제공하므로, 대규모 메시지 처리 애플리케이션에 적합한 솔루션입니다.

메시징 사용 사례는 처리량이 비교적 낮더라도 종단 간 지연 시간(End-to-end Latency)이 낮아야 하고, 강력한 내구성(Durability) 보장이 필요한 경우가 많습니다.

이 영역에서 Kafka는 [ActiveMQ](http://activemq.apache.org/)나 [RabbitMQ](https://www.rabbitmq.com/) 같은 전통적인 메시징 시스템과 견줄 수 있습니다.

---

## 2. 웹사이트 활동 추적 (Website Activity Tracking)

Kafka의 원래 사용 사례는 사용자 활동 추적 파이프라인을 실시간 발행-구독(Publish-Subscribe) 피드 집합으로 재구성하는 것이었습니다. 사이트 활동(페이지 조회, 검색 등 사용자가 수행하는 각종 행동)은 활동 유형별로 각각의 토픽에 게시됩니다.

이러한 피드는 실시간 처리, 실시간 모니터링, 오프라인 처리 및 보고를 위한 Hadoop 또는 오프라인 데이터 웨어하우스 시스템으로의 적재 등 다양한 목적으로 구독할 수 있습니다.

활동 추적은 사용자의 페이지 조회 한 번에 대해서도 다수의 활동 메시지가 생성되므로, 매우 높은 처리량이 요구되는 경우가 많습니다.

---

## 3. 메트릭 (Metrics)

Kafka는 운영 모니터링 데이터 처리에 자주 활용됩니다. 분산 애플리케이션의 통계를 집계해 운영 데이터를 중앙화된 피드로 제공하는 것이 대표적인 예입니다.

---

## 4. 로그 집계 (Log Aggregation)

많은 경우 Kafka를 로그 집계 솔루션의 대체제로 사용합니다. 로그 집계는 일반적으로 서버에서 물리적 로그 파일을 수집해 처리를 위한 중앙 위치(파일 서버 또는 HDFS 등)에 저장하는 방식입니다.

Kafka는 파일의 세부 사항을 추상화하고, 로그 또는 이벤트 데이터를 메시지 스트림으로 깔끔하게 표현합니다. 덕분에 더 낮은 지연 시간으로 처리할 수 있고, 여러 데이터 소스와 분산 소비를 쉽게 지원합니다.

Scribe나 Flume 같은 로그 중심 시스템과 비교할 때, Kafka는 동등한 수준의 성능과 복제 기반의 강력한 내구성 보장, 그리고 훨씬 낮은 종단 간 지연 시간을 제공합니다.

---

## 5. 스트림 처리 (Stream Processing)

많은 Kafka 사용자들은 여러 단계로 구성된 처리 파이프라인에서 데이터를 처리합니다. 이 파이프라인에서 원시 입력 데이터는 Kafka 토픽에서 소비된 뒤 집계, 보강, 변환 등을 거쳐 후속 처리를 위한 새로운 토픽에 게시됩니다.

예를 들어, 뉴스 기사 추천 파이프라인은 RSS 피드에서 기사 콘텐츠를 크롤링해 "articles" 토픽에 게시하고, 이후 단계에서 콘텐츠를 정규화하거나 중복을 제거해 새 토픽에 게시합니다. 최종 처리 단계에서는 이 콘텐츠를 바탕으로 사용자에게 기사를 추천합니다.

이러한 처리 파이프라인은 개별 토픽을 기반으로 실시간 데이터 흐름 그래프를 형성합니다. 버전 0.10.0.0부터 Apache Kafka에 포함된 경량 스트림 처리 라이브러리인 [Kafka Streams](https://kafka.apache.org/documentation/streams)를 이러한 처리에 활용할 수 있습니다.

Kafka Streams 외에도 [Apache Storm](https://storm.apache.org/)이나 [Apache Samza](http://samza.apache.org/) 같은 대안적인 오픈 소스 스트림 처리 도구를 사용할 수 있습니다.

---

## 6. 이벤트 소싱 (Event Sourcing)

이벤트 소싱은 상태 변경을 시간 순서로 정렬된 레코드 시퀀스로 기록하는 애플리케이션 설계 방식입니다. Kafka는 대용량 로그 데이터 저장을 잘 지원하므로, 이 방식으로 구축된 애플리케이션의 백엔드로 적합합니다.

---

## 7. 커밋 로그 (Commit Log)

Kafka는 분산 시스템을 위한 외부 커밋 로그(Commit Log) 역할을 할 수 있습니다. 로그는 노드 간 데이터 복제를 돕고, 장애가 발생한 노드가 데이터를 복원할 수 있는 재동기화 메커니즘으로 작동합니다.

Kafka의 [로그 컴팩션(Log Compaction)](https://kafka.apache.org/documentation.html#compaction) 기능은 이 사용 사례를 잘 지원합니다. 이 측면에서 Kafka는 [Apache BookKeeper](https://bookkeeper.apache.org/) 프로젝트와 유사합니다.

---

## 요약

| 사용 사례 | 설명 | 특징 |
|-----------|------|------|
| 메시징 | 전통적인 메시지 브로커 대체 | 높은 처리량, 파티셔닝, 복제, 내결함성 |
| 웹사이트 활동 추적 | 사용자 활동의 실시간 추적 | 대용량 처리, 실시간 피드 |
| 메트릭 | 운영 모니터링 데이터 집계 | 분산 애플리케이션 통계 중앙 집중화 |
| 로그 집계 | 서버 로그의 중앙 집중식 수집 | 낮은 지연 시간, 분산 소비 지원 |
| 스트림 처리 | 다단계 데이터 처리 파이프라인 | 실시간 데이터 흐름, Kafka Streams |
| 이벤트 소싱 | 상태 변경의 시간순 기록 | 대용량 로그 데이터 저장 |
| 커밋 로그 | 분산 시스템의 외부 커밋 로그 | 로그 컴팩션, 데이터 복제 및 복원 |

---

## 참고 자료

- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Kafka Streams](https://kafka.apache.org/documentation/streams)
- [Kafka 로그 컴팩션](https://kafka.apache.org/documentation.html#compaction)

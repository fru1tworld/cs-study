# Kafka 소개, 빠른 시작, 사용 사례, API

## Kafka 소개

> 원본: https://kafka.apache.org/documentation/#introduction

---

### 목차

1. [이벤트 스트리밍이란?](#이벤트-스트리밍이란)
2. [Apache Kafka는 이벤트 스트리밍 플랫폼입니다](#apache-kafka는-이벤트-스트리밍-플랫폼입니다)
3. [Kafka는 어떻게 동작하나요?](#kafka는-어떻게-동작하나요)
4. [주요 개념과 용어](#주요-개념과-용어)
5. [Kafka API](#kafka-api)
6. [다음 단계](#다음-단계)

---

### 이벤트 스트리밍이란?

이벤트 스트리밍(Event Streaming)은 인체의 중추신경계에 해당하는 디지털 기술입니다. '상시 가동(always-on)' 세계의 기술적 기반으로, 비즈니스가 점점 더 소프트웨어에 의해 정의되고 자동화되며, 소프트웨어 사용자 자체도 소프트웨어인 세상을 지원합니다.

기술적으로, 이벤트 스트리밍은 데이터베이스, 센서, 모바일 기기, 클라우드 서비스, 소프트웨어 애플리케이션 등의 이벤트 소스에서 이벤트 스트림 형태로 실시간 데이터를 캡처하는 것입니다. 캡처된 이벤트 스트림을 나중에 검색할 수 있도록 영구적으로 저장하고, 실시간으로 또는 소급하여 처리하며, 필요에 따라 다른 목적지 기술로 라우팅합니다. 이벤트 스트리밍은 데이터의 지속적인 흐름과 해석을 보장하여 올바른 정보가 적시에 적절한 위치에 있도록 합니다.

#### 이벤트 스트리밍의 활용 사례

이벤트 스트리밍은 다양한 산업과 조직의 광범위한 사용 사례에 적용됩니다. 대표적인 활용 사례는 다음과 같습니다:

- 금융 서비스: 은행, 주식 거래소, 보험사 등에서 실시간으로 결제 및 금융 거래를 처리
- 물류 및 운송: 차량, 트럭, 선박, 화물 등을 실시간으로 추적하고 모니터링
- 사물인터넷(IoT): 공장 및 풍력 발전 단지와 같은 IoT 기기에서 센서 데이터를 수집하고 분석
- 소매 및 서비스: 소매, 호텔 및 여행 산업, 모바일 애플리케이션에서 고객 상호작용 및 주문을 실시간 추적
- 헬스케어: 환자 모니터링 및 건강 상태 변화 예측을 통한 응급 상황 대응
- 데이터 플랫폼: 기업의 여러 부서 간 데이터 연결 및 실시간 공유
- 마이크로서비스 아키텍처: 데이터 플랫폼, 이벤트 기반 아키텍처, 마이크로서비스의 기반 기술로 활용

---

### Apache Kafka는 이벤트 스트리밍 플랫폼입니다

Kafka는 세 가지 핵심 기능을 결합하여 엔드투엔드(end-to-end) 이벤트 스트리밍을 단일 검증된 솔루션으로 구현할 수 있게 해줍니다:

1. 이벤트 스트림 발행(publish) 및 구독(subscribe): 다른 시스템에서 데이터를 지속적으로 가져오거나(import) 내보내기(export)
2. 이벤트 스트림의 영구 저장: 원하는 기간 동안 이벤트 스트림을 안정적이고 신뢰성 있게 저장
3. 이벤트 스트림 처리: 이벤트가 발생하는 시점 또는 소급하여 이벤트 스트림을 처리

이러한 모든 기능은 분산, 고확장성(highly scalable), 탄력성(elastic), 내결함성(fault-tolerant), 보안 방식으로 제공됩니다. Kafka는 베어메탈 하드웨어, 가상 머신, 컨테이너에 배포할 수 있으며, 온프레미스 환경과 클라우드 환경 모두에서 운영할 수 있습니다. 또한 Kafka 환경을 직접 관리하거나 다양한 벤더가 제공하는 완전 관리형 서비스를 사용할 수 있습니다.

---

### Kafka는 어떻게 동작하나요?

Kafka는 서버와 클라이언트로 구성된 분산 시스템으로, 고성능 TCP 네트워크 프로토콜을 통해 통신합니다. 온프레미스 및 클라우드 환경의 베어메탈 하드웨어, 가상 머신, 컨테이너에 배포할 수 있습니다.

#### 서버 (Servers)

Kafka는 하나 이상의 서버로 구성된 클러스터로 실행되며, 여러 데이터센터나 클라우드 리전에 걸쳐 있을 수 있습니다.

- 브로커(Broker): 일부 서버는 스토리지 레이어를 형성하며, 이를 브로커라고 합니다.
- Kafka Connect: 다른 서버들은 Kafka Connect를 실행하여 데이터를 이벤트 스트림으로 지속적으로 가져오고(import) 내보내기(export)하며, 관계형 데이터베이스나 다른 Kafka 클러스터와 같은 기존 시스템과 Kafka를 통합합니다.

미션 크리티컬한 사용 사례를 충족하기 위해, Kafka 클러스터는 고확장성과 내결함성을 갖추고 있습니다. 서버 중 하나에 장애가 발생하면 다른 서버들이 작업을 인계받아 데이터 손실 없이 지속적인 운영을 보장합니다.

#### 클라이언트 (Clients)

클라이언트를 사용하면 네트워크 문제나 머신 장애가 발생하더라도 내결함성을 갖춘 방식으로 이벤트 스트림을 병렬·대규모(at scale)로 읽고, 쓰고, 처리하는 분산 애플리케이션과 마이크로서비스를 작성할 수 있습니다.

Kafka에는 커뮤니티에서 제공하는 수십 개의 클라이언트와 함께 여러 클라이언트가 포함되어 있습니다. Java, Scala 등의 고수준 언어와 Go, Python, C/C++ 등 다양한 프로그래밍 언어용 클라이언트, 그리고 REST API를 사용할 수 있습니다.

---

### 주요 개념과 용어

#### 이벤트 (Event)

이벤트는 세상에서 "무언가가 일어났다"는 사실을 기록합니다. 레코드(record) 또는 메시지(message)라고도 합니다. Kafka에서 데이터를 읽거나 쓸 때는 이벤트 형태로 수행합니다.

개념적으로 이벤트는 키(key), 값(value), 타임스탬프(timestamp), 그리고 선택적 메타데이터 헤더(metadata headers)를 가집니다.

다음은 이벤트의 예시입니다:

- 이벤트 키: "Alice"
- 이벤트 값: "Bob에게 $200를 지불함"
- 이벤트 타임스탬프: "Jun. 25, 2020 at 2:06 p.m."

#### 프로듀서와 컨슈머 (Producers and Consumers)

프로듀서(Producer)는 Kafka에 이벤트를 발행(write)하는 클라이언트 애플리케이션이고, 컨슈머(Consumer)는 이러한 이벤트를 구독(read 및 처리)하는 클라이언트 애플리케이션입니다.

Kafka에서 프로듀서와 컨슈머는 완전히 분리(decoupled)되어 있으며 서로를 알지 못합니다. 이는 Kafka가 높은 확장성을 달성하기 위한 핵심 설계 요소입니다. 예를 들어, 프로듀서는 컨슈머를 기다릴 필요가 없습니다. Kafka는 이벤트를 정확히 한 번(exactly-once) 처리하는 기능과 같은 다양한 보장(guarantees)을 제공합니다.

#### 토픽 (Topics)

이벤트는 토픽(Topic)으로 구성되고 영구적으로 저장됩니다. 단순하게 설명하면, 토픽은 파일 시스템의 폴더와 유사하고, 이벤트는 해당 폴더 안의 파일과 같습니다.

예를 들어, 토픽 이름은 "payments"일 수 있습니다. Kafka의 토픽은 항상 다중 프로듀서(multi-producer) 및 다중 구독자(multi-subscriber) 입니다. 즉, 토픽에는 0개, 하나, 또는 여러 프로듀서가 이벤트를 쓸 수 있고, 0개, 하나, 또는 여러 컨슈머가 이러한 이벤트를 구독할 수 있습니다.

토픽의 이벤트는 필요한 만큼 여러 번 읽을 수 있습니다. 전통적인 메시징 시스템과 달리, 이벤트는 소비 후에도 삭제되지 않습니다. 대신 토픽별 설정을 통해 Kafka가 이벤트를 얼마나 오래 보관할지 정의하며, 이후 오래된 이벤트가 삭제됩니다. Kafka의 성능은 데이터 크기와 관계없이 일정하므로, 데이터를 장기간 저장해도 문제가 없습니다.

#### 파티션 (Partitions)

토픽은 파티션(Partition) 됩니다. 즉, 토픽은 서로 다른 Kafka 브로커에 위치한 여러 "버킷"에 분산됩니다. 데이터의 이러한 분산 배치는 확장성에 매우 중요합니다. 클라이언트 애플리케이션이 여러 브로커에서 동시에 데이터를 읽고 쓸 수 있기 때문입니다.

새 이벤트가 토픽에 발행되면, 실제로는 토픽의 파티션 중 하나에 추가됩니다. 동일한 이벤트 키(예: 고객 ID 또는 차량 ID)를 가진 이벤트는 같은 파티션에 기록되며, Kafka는 특정 토픽 파티션의 모든 컨슈머가 항상 해당 파티션의 이벤트를 기록된 순서와 정확히 동일한 순서로 읽는 것을 보장합니다.

#### 복제 (Replication)

내결함성과 고가용성을 보장하기 위해 모든 토픽은 복제(replicate)될 수 있습니다. 복제는 지리적 리전이나 데이터센터 간에도 가능하므로, 장애·유지보수 등 어떤 상황에서도 데이터 복사본을 가진 여러 브로커가 존재합니다.

일반적인 프로덕션 설정은 복제 팩터(replication factor) 3 입니다. 즉, 데이터의 복사본이 항상 3개 존재합니다. 이 복제는 토픽 파티션 수준에서 수행됩니다.

---

### Kafka API

Kafka에는 다음과 같은 다섯 가지 핵심 API가 있습니다:

#### Producer API

Producer API를 사용하면 애플리케이션이 하나 이상의 Kafka 토픽에 이벤트 스트림을 발행(write)할 수 있습니다.

#### Consumer API

Consumer API를 사용하면 애플리케이션이 하나 이상의 토픽을 구독하고 해당 토픽의 이벤트 스트림을 처리할 수 있습니다.

#### Kafka Streams API

Kafka Streams API를 사용하면 스트림 처리 애플리케이션과 마이크로서비스를 구현할 수 있습니다. 입력 스트림을 출력 스트림으로 변환하는 기능을 비롯해 이벤트를 처리하는 고수준 함수를 제공합니다.

주요 기능:
- 변환(Transformations)
- 상태 저장 연산(Stateful Operations): 집계(aggregations), 조인(joins)
- 윈도잉(Windowing)
- 스트림과 테이블 처리

#### Kafka Connect API

Kafka Connect API는 Kafka 토픽에서 데이터를 읽거나 쓰는 재사용 가능한 커넥터(Connectors)를 구축하고 실행합니다.

예를 들어, PostgreSQL과 같은 관계형 데이터베이스에 연결하는 커넥터는 테이블의 모든 변경 사항을 캡처할 수 있습니다. Kafka Connect 생태계에는 이미 수백 개의 즉시 사용 가능한 커넥터가 있습니다.

#### Admin API

Admin API를 사용하면 토픽, 브로커 및 기타 Kafka 객체를 관리하고 검사할 수 있습니다.

---

### 다음 단계

Kafka를 시작하기 위한 추가 자료는 다음을 참조하세요:

- [빠른 시작 가이드](https://kafka.apache.org/quickstart): 첫 번째 Kafka 클러스터를 설정하고 실행해 보세요.
- [문서](https://kafka.apache.org/documentation/): 상세한 Kafka 문서를 참조하세요.
- [사용 사례](https://kafka.apache.org/powered-by): 커뮤니티에서 Kafka를 어떻게 활용하고 있는지 확인하세요.
- [Kafka Summit](https://kafka.apache.org/events): Kafka Summit 및 지역 커뮤니티 이벤트에 참가하세요.

---

### 참고 자료

- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Apache Kafka 소개 페이지](https://kafka.apache.org/intro)
- [Kafka Quickstart](https://kafka.apache.org/quickstart)
- [Kafka APIs](https://kafka.apache.org/documentation/#api)
- [Kafka Design](https://kafka.apache.org/documentation/#design)

---

## Kafka 빠른 시작

> 원본: https://kafka.apache.org/quickstart

### 개요

메시지를 게시하고 소비하는 기본 기능부터 Kafka Connect와 Kafka Streams를 활용한 데이터 파이프라인 구축까지 단계별로 안내합니다.

---

### 1단계: Kafka 다운로드

최신 Kafka 릴리스를 다운로드하고 압축을 해제합니다:

```bash
$ tar -xzf kafka_2.13-4.1.1.tgz
$ cd kafka_2.13-4.1.1
```

---

### 2단계: Kafka 환경 시작

요구 사항: Java 17 이상이 로컬에 설치되어 있어야 합니다.

#### 옵션 A: 다운로드한 파일 사용

클러스터 식별자(Cluster ID)를 생성합니다:

```bash
$ KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
```

로그 디렉토리를 포맷합니다:

```bash
$ bin/kafka-storage.sh format --standalone -t $KAFKA_CLUSTER_ID -c config/server.properties
```

서버를 실행합니다:

```bash
$ bin/kafka-server-start.sh config/server.properties
```

#### 옵션 B: Docker (JVM 기반)

```bash
$ docker pull apache/kafka:4.1.1
$ docker run -p 9092:9092 apache/kafka:4.1.1
```

#### 옵션 C: Docker (GraalVM Native)

```bash
$ docker pull apache/kafka-native:4.1.1
$ docker run -p 9092:9092 apache/kafka-native:4.1.1
```

---

### 3단계: 토픽(Topic) 생성

Kafka는 분산 이벤트 스트리밍 플랫폼으로서 데이터를 토픽(Topic)으로 구성합니다. 새 터미널을 열고 다음 명령을 실행합니다:

```bash
$ bin/kafka-topics.sh --create --topic quickstart-events --bootstrap-server localhost:9092
```

토픽 상세 정보를 확인합니다:

```bash
$ bin/kafka-topics.sh --describe --topic quickstart-events --bootstrap-server localhost:9092
```

---

### 4단계: 이벤트 쓰기 (Producer)

콘솔 프로듀서(Console Producer)를 사용하여 메시지를 전송합니다:

```bash
$ bin/kafka-console-producer.sh --topic quickstart-events --bootstrap-server localhost:9092
>This is my first event
>This is my second event
```

메시지 입력을 중지하려면 `Ctrl-C`를 누릅니다.

---

### 5단계: 이벤트 읽기 (Consumer)

다른 터미널을 열고 메시지를 소비합니다:

```bash
$ bin/kafka-console-consumer.sh --topic quickstart-events --from-beginning --bootstrap-server localhost:9092
```

생성된 메시지가 표시됩니다. `Ctrl-C`로 컨슈머를 중지할 수 있습니다.

---

### 6단계: Kafka Connect를 사용한 데이터 가져오기/내보내기

Kafka Connect는 외부 시스템과 Kafka 사이에서 데이터를 스트리밍하는 도구입니다.

#### 플러그인 경로 설정

`config/connect-standalone.properties` 파일에 플러그인 경로를 설정합니다:

```bash
$ echo "plugin.path=libs/connect-file-4.1.1.jar" >> config/connect-standalone.properties
```

#### 테스트 데이터 생성

```bash
$ echo -e "foo\nbar" > test.txt
```

#### 커넥터 시작

```bash
$ bin/connect-standalone.sh config/connect-standalone.properties config/connect-file-source.properties config/connect-file-sink.properties
```

#### 결과 확인

```bash
$ more test.sink.txt
```

`test.txt` 파일의 내용이 `test.sink.txt` 파일에 복사된 것을 확인할 수 있습니다.

---

### 7단계: Kafka Streams로 이벤트 처리

Kafka Streams는 Java 및 Scala 애플리케이션에서 이벤트를 실시간으로 처리하고 변환하는 클라이언트 라이브러리입니다.

#### 주요 특징

- 스트림 처리: 이벤트 스트림을 실시간으로 처리
- 상태 저장: 로컬 상태 저장소를 통한 상태 관리
- 정확히 한 번 처리: Exactly-Once Semantics 지원
- 폴트 톨러런스: 장애 발생 시 자동 복구

#### WordCount 예제

Kafka Streams의 대표적인 예제인 WordCount는 입력 토픽에서 텍스트를 읽어 단어별로 집계한 뒤 출력 토픽에 기록합니다. 데이터를 실시간으로 변환하고 집계하는 방법을 보여줍니다.

---

### 8단계: 종료

모든 컴포넌트를 중지하려면 `Ctrl-C`를 누릅니다.

#### 로컬 데이터 정리 (선택 사항)

로컬 환경의 모든 Kafka 데이터를 삭제하려면:

```bash
$ rm -rf /tmp/kafka-logs /tmp/kraft-combined-logs
```

주의: 이 명령은 토픽에 저장된 모든 데이터를 영구적으로 삭제합니다.

---

### 핵심 개념 요약

#### Producer (프로듀서)
메시지를 Kafka 토픽에 게시하는 클라이언트입니다.

#### Consumer (컨슈머)
Kafka 토픽에서 메시지를 읽는 클라이언트입니다.

#### Topic (토픽)
메시지가 저장되는 카테고리 또는 피드입니다. 토픽은 여러 파티션으로 분할될 수 있습니다.

#### Broker (브로커)
Kafka 클러스터를 구성하는 서버입니다. 메시지를 저장하고 클라이언트 요청을 처리합니다.

#### Kafka Connect
외부 시스템과 Kafka 간의 데이터 통합을 위한 프레임워크입니다.

#### Kafka Streams
실시간 스트림 처리를 위한 클라이언트 라이브러리입니다.

---

### 다음 단계

다음 주제를 계속 탐색해 보세요:

- [Kafka 설계](https://kafka.apache.org/documentation/#design): Kafka의 내부 설계와 아키텍처 학습
- [Kafka 설정](https://kafka.apache.org/documentation/#configuration): 상세한 설정 옵션 탐색
- [Kafka API](https://kafka.apache.org/documentation/#api): Producer, Consumer, Streams, Connect API 학습
- [Kafka 운영](https://kafka.apache.org/documentation/#operations): 프로덕션 환경에서의 Kafka 운영 가이드

---

### 참고 자료

- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Kafka Quickstart](https://kafka.apache.org/quickstart)
- [Kafka Downloads](https://kafka.apache.org/downloads)
- [Kafka GitHub Repository](https://github.com/apache/kafka)

---

## Kafka 사용 사례

> 원본: https://kafka.apache.org/documentation/#uses

### 개요

Apache Kafka는 다양한 사용 사례에 적용 가능한 범용 분산 스트리밍 플랫폼입니다. 아래는 Kafka의 주요 사용 사례입니다.

---

### 1. 메시징 (Messaging)

Kafka는 전통적인 메시지 브로커(Message Broker)를 대체하는 용도로 잘 작동합니다. 메시지 브로커는 데이터 생산자와 처리를 분리하거나, 처리되지 않은 메시지를 버퍼링하는 등 다양한 목적으로 사용됩니다. 대부분의 메시징 시스템과 비교할 때 Kafka는 더 높은 처리량(Throughput), 내장 파티셔닝(Partitioning), 복제(Replication), 내결함성(Fault-tolerance)을 제공하므로, 대규모 메시지 처리 애플리케이션에 적합한 솔루션입니다.

메시징 사용 사례는 처리량이 비교적 낮더라도 종단 간 지연 시간(End-to-end Latency)이 낮아야 하고, 강력한 내구성(Durability) 보장이 필요한 경우가 많습니다.

이 영역에서 Kafka는 [ActiveMQ](http://activemq.apache.org/)나 [RabbitMQ](https://www.rabbitmq.com/) 같은 전통적인 메시징 시스템과 견줄 수 있습니다.

---

### 2. 웹사이트 활동 추적 (Website Activity Tracking)

Kafka의 원래 사용 사례는 사용자 활동 추적 파이프라인을 실시간 발행-구독(Publish-Subscribe) 피드 집합으로 재구성하는 것이었습니다. 사이트 활동(페이지 조회, 검색 등 사용자가 수행하는 각종 행동)은 활동 유형별로 각각의 토픽에 게시됩니다.

이러한 피드는 실시간 처리, 실시간 모니터링, 오프라인 처리 및 보고를 위한 Hadoop 또는 오프라인 데이터 웨어하우스 시스템으로의 적재 등 다양한 목적으로 구독할 수 있습니다.

활동 추적은 사용자의 페이지 조회 한 번에 대해서도 다수의 활동 메시지가 생성되므로, 매우 높은 처리량이 요구되는 경우가 많습니다.

---

### 3. 메트릭 (Metrics)

Kafka는 운영 모니터링 데이터 처리에 자주 활용됩니다. 분산 애플리케이션의 통계를 집계해 운영 데이터를 중앙화된 피드로 제공하는 것이 대표적인 예입니다.

---

### 4. 로그 집계 (Log Aggregation)

많은 경우 Kafka를 로그 집계 솔루션의 대체제로 사용합니다. 로그 집계는 일반적으로 서버에서 물리적 로그 파일을 수집해 처리를 위한 중앙 위치(파일 서버 또는 HDFS 등)에 저장하는 방식입니다.

Kafka는 파일의 세부 사항을 추상화하고, 로그 또는 이벤트 데이터를 메시지 스트림으로 깔끔하게 표현합니다. 덕분에 더 낮은 지연 시간으로 처리할 수 있고, 여러 데이터 소스와 분산 소비를 쉽게 지원합니다.

Scribe나 Flume 같은 로그 중심 시스템과 비교할 때, Kafka는 동등한 수준의 성능과 복제 기반의 강력한 내구성 보장, 그리고 훨씬 낮은 종단 간 지연 시간을 제공합니다.

---

### 5. 스트림 처리 (Stream Processing)

많은 Kafka 사용자들은 여러 단계로 구성된 처리 파이프라인에서 데이터를 처리합니다. 이 파이프라인에서 원시 입력 데이터는 Kafka 토픽에서 소비된 뒤 집계, 보강, 변환 등을 거쳐 후속 처리를 위한 새로운 토픽에 게시됩니다.

예를 들어, 뉴스 기사 추천 파이프라인은 RSS 피드에서 기사 콘텐츠를 크롤링해 "articles" 토픽에 게시하고, 이후 단계에서 콘텐츠를 정규화하거나 중복을 제거해 새 토픽에 게시합니다. 최종 처리 단계에서는 이 콘텐츠를 바탕으로 사용자에게 기사를 추천합니다.

이러한 처리 파이프라인은 개별 토픽을 기반으로 실시간 데이터 흐름 그래프를 형성합니다. 버전 0.10.0.0부터 Apache Kafka에 포함된 경량 스트림 처리 라이브러리인 [Kafka Streams](https://kafka.apache.org/documentation/streams)를 이러한 처리에 활용할 수 있습니다.

Kafka Streams 외에도 [Apache Storm](https://storm.apache.org/)이나 [Apache Samza](http://samza.apache.org/) 같은 대안적인 오픈 소스 스트림 처리 도구를 사용할 수 있습니다.

---

### 6. 이벤트 소싱 (Event Sourcing)

이벤트 소싱은 상태 변경을 시간 순서로 정렬된 레코드 시퀀스로 기록하는 애플리케이션 설계 방식입니다. Kafka는 대용량 로그 데이터 저장을 잘 지원하므로, 이 방식으로 구축된 애플리케이션의 백엔드로 적합합니다.

---

### 7. 커밋 로그 (Commit Log)

Kafka는 분산 시스템을 위한 외부 커밋 로그(Commit Log) 역할을 할 수 있습니다. 로그는 노드 간 데이터 복제를 돕고, 장애가 발생한 노드가 데이터를 복원할 수 있는 재동기화 메커니즘으로 작동합니다.

Kafka의 [로그 컴팩션(Log Compaction)](https://kafka.apache.org/documentation.html#compaction) 기능은 이 사용 사례를 잘 지원합니다. 이 측면에서 Kafka는 [Apache BookKeeper](https://bookkeeper.apache.org/) 프로젝트와 유사합니다.

---

### 요약

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

### 참고 자료

- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Kafka Streams](https://kafka.apache.org/documentation/streams)
- [Kafka 로그 컴팩션](https://kafka.apache.org/documentation.html#compaction)

---

## Kafka APIs

> 원본: https://kafka.apache.org/documentation/#api

---

### 개요

Kafka는 다섯 가지 핵심 API를 제공합니다:

1. Producer API - 토픽에 데이터 스트림 전송
2. Consumer API - 토픽에서 데이터 스트림 읽기
3. Streams API - 입력 토픽에서 출력 토픽으로 데이터 스트림 변환
4. Connect API - 외부 시스템과 Kafka 간 데이터 연동
5. Admin API - Kafka 객체 관리 및 검사

---

### Producer API

Producer API를 사용하면 애플리케이션이 Kafka 클러스터의 토픽에 데이터 스트림을 전송할 수 있습니다.

#### Maven 의존성

Producer API를 사용하려면 다음 Maven 의존성을 추가합니다:

```xml
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>3.9.1</version>
</dependency>
```

#### 주요 특징

- 비동기 전송: 메시지를 비동기적으로 전송하여 높은 처리량 달성
- 배치 처리: 여러 메시지를 배치로 묶어 네트워크 효율성 향상
- 파티셔닝: 메시지를 특정 파티션에 할당하여 순서 보장
- 직렬화: 다양한 데이터 형식 지원 (String, Avro, JSON 등)

#### 기본 사용 예제

```java
import org.apache.kafka.clients.producer.*;
import java.util.Properties;

public class SimpleProducer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer",
            "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer",
            "org.apache.kafka.common.serialization.StringSerializer");

        Producer<String, String> producer = new KafkaProducer<>(props);

        ProducerRecord<String, String> record =
            new ProducerRecord<>("my-topic", "key", "value");

        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                System.out.println("Message sent to partition " +
                    metadata.partition() + " with offset " + metadata.offset());
            } else {
                exception.printStackTrace();
            }
        });

        producer.close();
    }
}
```

#### 주요 설정 옵션

| 설정 | 설명 | 기본값 |
|------|------|--------|
| `bootstrap.servers` | Kafka 브로커 주소 목록 | - |
| `acks` | 메시지 확인 수준 (0, 1, all) | all |
| `retries` | 전송 실패 시 재시도 횟수 | 2147483647 |
| `batch.size` | 배치 크기 (바이트) | 16384 |
| `linger.ms` | 배치 전송 대기 시간 | 0 |
| `buffer.memory` | 버퍼 메모리 크기 | 33554432 |

자세한 내용은 [KafkaProducer Javadoc](https://kafka.apache.org/javadoc/org/apache/kafka/clients/producer/KafkaProducer.html)을 참조하세요.

---

### Consumer API

Consumer API를 사용하면 애플리케이션이 Kafka 클러스터의 토픽에서 데이터 스트림을 읽을 수 있습니다.

#### Maven 의존성

Consumer API를 사용하려면 다음 Maven 의존성을 추가합니다:

```xml
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>3.9.1</version>
</dependency>
```

#### 주요 특징

- Consumer Group: 여러 컨슈머가 그룹을 형성하여 파티션을 분산 처리
- 오프셋 관리: 자동 또는 수동으로 오프셋 커밋 관리
- 리밸런싱: 컨슈머 추가/제거 시 자동으로 파티션 재할당
- 역직렬화: 다양한 데이터 형식 지원

#### 기본 사용 예제

```java
import org.apache.kafka.clients.consumer.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class SimpleConsumer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "my-consumer-group");
        props.put("key.deserializer",
            "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer",
            "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("auto.offset.reset", "earliest");

        Consumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("my-topic"));

        try {
            while (true) {
                ConsumerRecords<String, String> records =
                    consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record : records) {
                    System.out.printf("partition = %d, offset = %d, " +
                        "key = %s, value = %s%n",
                        record.partition(), record.offset(),
                        record.key(), record.value());
                }
            }
        } finally {
            consumer.close();
        }
    }
}
```

#### 주요 설정 옵션

| 설정 | 설명 | 기본값 |
|------|------|--------|
| `bootstrap.servers` | Kafka 브로커 주소 목록 | - |
| `group.id` | Consumer Group ID | - |
| `auto.offset.reset` | 초기 오프셋 설정 (earliest, latest) | latest |
| `enable.auto.commit` | 자동 오프셋 커밋 여부 | true |
| `auto.commit.interval.ms` | 자동 커밋 간격 | 5000 |
| `max.poll.records` | poll() 호출당 최대 레코드 수 | 500 |

자세한 내용은 [KafkaConsumer Javadoc](https://kafka.apache.org/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html)을 참조하세요.

---

### Streams API

Streams API를 사용하면 입력 토픽에서 출력 토픽으로 데이터 스트림을 변환할 수 있습니다. 이 API는 클라이언트 라이브러리로 제공되어 별도의 클러스터 없이 스트림 처리 애플리케이션을 구축할 수 있습니다.

#### Maven 의존성

Streams API를 사용하려면 다음 Maven 의존성을 추가합니다:

```xml
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-streams</artifactId>
    <version>3.9.1</version>
</dependency>
```

#### Scala DSL (선택 사항)

Scala 2.13 사용자를 위한 DSL 라이브러리도 제공됩니다:

```xml
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-streams-scala_2.13</artifactId>
    <version>3.9.1</version>
</dependency>
```

#### 주요 특징

- Stateless 변환: filter, map, flatMap 등
- Stateful 변환: aggregation, join, windowing 등
- Exactly-once 처리: 정확히 한 번 처리 보장
- 내결함성: 상태 저장소 자동 복구
- 확장성: 파티션 기반 병렬 처리

#### 기본 사용 예제

```java
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import java.util.Properties;

public class SimpleStreams {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "my-stream-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
            Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
            Serdes.String().getClass());

        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> source = builder.stream("input-topic");

        source.filter((key, value) -> value.length() > 5)
              .mapValues(value -> value.toUpperCase())
              .to("output-topic");

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();

        // 종료 훅 추가
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
```

#### 주요 DSL 연산자

| 연산자 | 유형 | 설명 |
|--------|------|------|
| `filter` | Stateless | 조건에 맞는 레코드만 통과 |
| `map` | Stateless | 키-값 쌍 변환 |
| `flatMap` | Stateless | 하나의 레코드를 여러 레코드로 변환 |
| `groupByKey` | Stateful | 키별로 레코드 그룹화 |
| `aggregate` | Stateful | 그룹화된 레코드 집계 |
| `join` | Stateful | 두 스트림 또는 테이블 조인 |
| `windowedBy` | Stateful | 시간 기반 윈도우 적용 |

자세한 내용은 [Kafka Streams 문서](https://kafka.apache.org/documentation/streams/)를 참조하세요.

---

### Connect API

Connect API를 사용하면 외부 데이터 시스템에서 Kafka로 데이터를 지속적으로 가져오거나(Source Connector), Kafka에서 외부 시스템으로 데이터를 내보내는(Sink Connector) 커넥터를 구현할 수 있습니다.

#### Maven 의존성

Connect API를 사용하려면 다음 Maven 의존성을 추가합니다:

```xml
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>3.9.1</version>
</dependency>
```

#### 주요 특징

- Source Connector: 외부 시스템에서 Kafka로 데이터 가져오기
- Sink Connector: Kafka에서 외부 시스템으로 데이터 내보내기
- 분산 모드: 여러 워커에서 커넥터 분산 실행
- 스탠드얼론 모드: 단일 프로세스에서 실행
- 자동 오프셋 관리: 커넥터 진행 상태 자동 추적
- 풍부한 에코시스템: 다양한 사전 구축된 커넥터 제공

#### 커넥터 유형

##### Source Connector

외부 시스템에서 Kafka로 데이터를 가져옵니다:

```json
{
  "name": "jdbc-source-connector",
  "config": {
    "connector.class": "io.confluent.connect.jdbc.JdbcSourceConnector",
    "connection.url": "jdbc:mysql://localhost:3306/mydb",
    "connection.user": "user",
    "connection.password": "password",
    "topic.prefix": "mysql-",
    "mode": "incrementing",
    "incrementing.column.name": "id"
  }
}
```

##### Sink Connector

Kafka에서 외부 시스템으로 데이터를 내보냅니다:

```json
{
  "name": "elasticsearch-sink-connector",
  "config": {
    "connector.class":
      "io.confluent.connect.elasticsearch.ElasticsearchSinkConnector",
    "connection.url": "http://localhost:9200",
    "topics": "my-topic",
    "type.name": "_doc",
    "key.ignore": true
  }
}
```

#### 인기 있는 커넥터

| 커넥터 | 유형 | 설명 |
|--------|------|------|
| JDBC | Source/Sink | 관계형 데이터베이스 연동 |
| Elasticsearch | Sink | Elasticsearch로 데이터 전송 |
| HDFS | Sink | Hadoop HDFS에 데이터 저장 |
| S3 | Sink | AWS S3에 데이터 저장 |
| FileStream | Source/Sink | 파일 시스템 연동 |
| Debezium | Source | CDC(Change Data Capture) |

대부분의 경우 커스텀 커넥터를 직접 구현하지 않고 기존에 공개된 커넥터를 그대로 활용할 수 있습니다.

자세한 내용은 [Kafka Connect 문서](https://kafka.apache.org/documentation/#connect)를 참조하세요.

---

### Admin API

Admin API는 토픽, 브로커, ACL 및 기타 Kafka 객체를 관리하고 검사하는 기능을 지원합니다.

#### Maven 의존성

Admin API를 사용하려면 다음 Maven 의존성을 추가합니다:

```xml
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>3.9.1</version>
</dependency>
```

#### 주요 기능

- 토픽 관리: 토픽 생성, 삭제, 설정 변경
- 파티션 관리: 파티션 추가, 재할당
- ACL 관리: 접근 제어 목록 관리
- 설정 관리: 브로커 및 토픽 설정 조회/변경
- Consumer Group 관리: Consumer Group 정보 조회 및 삭제
- 클러스터 정보 조회: 브로커, 토픽, 파티션 메타데이터 조회

#### 기본 사용 예제

```java
import org.apache.kafka.clients.admin.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class SimpleAdmin {
    public static void main(String[] args)
            throws ExecutionException, InterruptedException {

        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

        try (AdminClient admin = AdminClient.create(props)) {

            // 토픽 목록 조회
            ListTopicsResult topics = admin.listTopics();
            Set<String> topicNames = topics.names().get();
            System.out.println("Topics: " + topicNames);

            // 새 토픽 생성
            NewTopic newTopic = new NewTopic("new-topic", 3, (short) 1);
            admin.createTopics(Collections.singleton(newTopic)).all().get();
            System.out.println("Topic created successfully");

            // 토픽 상세 정보 조회
            DescribeTopicsResult describeResult =
                admin.describeTopics(Collections.singleton("new-topic"));
            Map<String, TopicDescription> descriptions =
                describeResult.allTopicNames().get();

            for (TopicDescription desc : descriptions.values()) {
                System.out.println("Topic: " + desc.name());
                System.out.println("Partitions: " + desc.partitions().size());
            }

            // 토픽 삭제
            admin.deleteTopics(Collections.singleton("new-topic")).all().get();
            System.out.println("Topic deleted successfully");
        }
    }
}
```

#### Consumer Group 관리 예제

```java
import org.apache.kafka.clients.admin.*;
import java.util.*;

public class ConsumerGroupAdmin {
    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

        try (AdminClient admin = AdminClient.create(props)) {

            // Consumer Group 목록 조회
            ListConsumerGroupsResult groups = admin.listConsumerGroups();
            Collection<ConsumerGroupListing> listings =
                groups.all().get();

            for (ConsumerGroupListing listing : listings) {
                System.out.println("Group ID: " + listing.groupId());
            }

            // Consumer Group 상세 정보 조회
            DescribeConsumerGroupsResult describeResult =
                admin.describeConsumerGroups(
                    Collections.singleton("my-consumer-group"));

            Map<String, ConsumerGroupDescription> descriptions =
                describeResult.all().get();

            for (ConsumerGroupDescription desc : descriptions.values()) {
                System.out.println("Group: " + desc.groupId());
                System.out.println("State: " + desc.state());
                System.out.println("Members: " + desc.members().size());
            }
        }
    }
}
```

#### 주요 Admin 작업

| 작업 | 메서드 | 설명 |
|------|--------|------|
| 토픽 생성 | `createTopics()` | 새 토픽 생성 |
| 토픽 삭제 | `deleteTopics()` | 토픽 삭제 |
| 토픽 조회 | `listTopics()` | 토픽 목록 조회 |
| 토픽 상세 | `describeTopics()` | 토픽 상세 정보 조회 |
| 설정 조회 | `describeConfigs()` | 브로커/토픽 설정 조회 |
| 설정 변경 | `alterConfigs()` | 브로커/토픽 설정 변경 |
| ACL 생성 | `createAcls()` | ACL 규칙 생성 |
| ACL 삭제 | `deleteAcls()` | ACL 규칙 삭제 |
| Consumer Group 조회 | `listConsumerGroups()` | Consumer Group 목록 조회 |
| Consumer Group 삭제 | `deleteConsumerGroups()` | Consumer Group 삭제 |

자세한 내용은 [AdminClient Javadoc](https://kafka.apache.org/javadoc/org/apache/kafka/clients/admin/AdminClient.html)을 참조하세요.

---

### 클라이언트 라이브러리

Apache Kafka 프로젝트는 Java 클라이언트 라이브러리만 공식적으로 유지 관리합니다. 다른 프로그래밍 언어용 클라이언트는 독립적인 오픈 소스 프로젝트로 제공됩니다.

#### 공식 Java 클라이언트

모든 Java API는 `kafka-clients` 또는 `kafka-streams` 아티팩트에 포함되어 있습니다.

#### 커뮤니티 클라이언트

다양한 프로그래밍 언어용 커뮤니티 클라이언트가 있습니다:

| 언어 | 클라이언트 |
|------|-----------|
| Python | confluent-kafka-python, kafka-python |
| Go | confluent-kafka-go, sarama |
| C/C++ | librdkafka |
| .NET | confluent-kafka-dotnet |
| Node.js | kafkajs, node-rdkafka |
| Ruby | ruby-kafka |
| PHP | php-kafka |
| Rust | rust-rdkafka |

---

### 참고 자료

- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Kafka APIs 문서](https://kafka.apache.org/documentation/#api)
- [KafkaProducer Javadoc](https://kafka.apache.org/javadoc/org/apache/kafka/clients/producer/KafkaProducer.html)
- [KafkaConsumer Javadoc](https://kafka.apache.org/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html)
- [Kafka Streams 문서](https://kafka.apache.org/documentation/streams/)
- [Kafka Connect 문서](https://kafka.apache.org/documentation/#connect)
- [AdminClient Javadoc](https://kafka.apache.org/javadoc/org/apache/kafka/clients/admin/AdminClient.html)

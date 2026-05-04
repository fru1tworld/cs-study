# Apache Kafka를 위한 큐(Queues for Apache Kafka)가 출시되었습니다: Confluent에서 시작하기 위한 가이드

> **원문:** [Queues for Apache Kafka® Is Here: Your Guide to Getting Started in Confluent](https://www.confluent.io/blog/kafka-queue-semantics-share-consumer-ga/)

> **저자:** Jonathan Lacefield, Group Product Manager

> **게시일:** 2026년 3월 3일 | **읽는 시간:** 6분

---

Kafka를 위한 큐(Queues for Kafka)가 Confluent Cloud에서 정식 출시(GA)되었으며, Apache Kafka 4.2 릴리스와 함께 Confluent Platform에도 곧 제공될 예정입니다. 이번 마일스톤은 KIP-932를 통해 프로덕션에 바로 사용 가능한 큐 시맨틱스와 탄력적인 컨슈머 스케일링을 Kafka에 네이티브로 제공하여, 조직이 메시징 인프라를 통합하는 동시에 탄력적인 컨슈머 스케일링과 메시지별 처리 제어를 확보할 수 있게 합니다.

## 인프라 통합의 과제

오늘날 많은 조직이 두 개의 별도 메시징 시스템을 유지하고 있습니다: 고처리량 스트리밍을 위한 Apache Kafka와 작업 분배 및 잡 처리를 위한 전통적인 메시지 큐입니다. 이러한 이중 인프라 접근 방식은 중복된 보안 프로토콜, 별도의 모니터링 스택, 분산된 데이터 거버넌스로 인해 운영 오버헤드를 발생시킵니다.

팀은 또한 근본적인 트레이드오프에 직면합니다: 전통적인 큐는 메시지별 확인 응답(acknowledgment)과 간단한 작업 분배를 제공하지만 Kafka의 내구성과 확장성이 부족합니다. 반면 Kafka의 컨슈머 그룹은 강력한 순서 보장과 안정적인 스트리밍을 제공하지만, 엄격한 1:1 파티션-컨슈머 매핑과 헤드 오브 라인 블로킹(head-of-line blocking)으로 인해 급증하는 워크로드를 처리하는 데 어려움을 겪습니다.

Kafka를 위한 큐는 큐 시맨틱스를 Kafka에 직접 도입함으로써 이러한 트레이드오프를 제거합니다. 조직은 이제 단일 플랫폼에서 스트리밍과 큐잉 워크로드를 모두 실행할 수 있어, 전통적인 메시지 큐에서 개발자가 기대하는 운영 단순성을 유지하면서 총 소유 비용(TCO)을 절감할 수 있습니다.

## Kafka를 위한 큐란 무엇인가?

Kafka를 위한 큐는 Kafka에 두 가지 주요 기능을 도입합니다:

- 새로운 공유 컨슈머 API(share consumer API)와 공유 그룹(share groups)은 해당 토픽의 파티션 수에 관계없이 여러 컨슈머가 동일한 토픽의 메시지를 협력적으로 처리할 수 있게 합니다.
- 메시지별 시맨틱스(per-message semantics)는 개발자가 Kafka에서 작업 큐처럼 코딩할 수 있게 합니다.

각 파티션이 정확히 하나의 컨슈머에 매핑되는 전통적인 Kafka 컨슈머 그룹과 달리, 공유 그룹은 파티션 수를 초과하여 컨슈머를 탄력적으로 확장할 수 있게 합니다. 브로커는 개별 레코드에 대한 획득 잠금(acquisition lock)을 관리하여, 컨슈머가 메시지를 독립적으로 확인(acknowledge), 해제(release), 거부(reject) 또는 갱신(renew)할 수 있게 합니다.

Kafka를 위한 큐는 다음과 같은 주요 기능을 제공합니다:

- **인프라 통합:** 동일한 Kafka 클러스터에서 큐와 스트림 워크로드를 실행하여 중복 인프라를 제거합니다.
- **파티션 제약 없는 탄력적 스케일링:** 리밸런싱이나 파티션 과다 프로비저닝 없이 공유 그룹에 컨슈머를 즉시 추가할 수 있으며, 이 과정에서 헤드 오브 라인 블로킹을 방지합니다.
- **메시지별 처리 제어:** 성공적인 처리를 확인하고, 재시도를 위해 메시지를 해제하고, 처리 불가능한 레코드를 향후 처리를 위해 거부 및 라우팅하고, 장시간 실행되는 작업 처리를 위해 메시지를 개별적으로 갱신합니다.
- **Kafka의 검증된 내구성:** 큐잉 워크로드에 장기 보존, 재생 가능성, 메시지 무손실을 적용합니다.

## 작동 원리

Kafka를 위한 큐는 브로커 레벨에서 큐 시맨틱스를 구현하는 네이티브 Kafka 개선 사항인 KIP-932를 기반으로 구축되었습니다. 공유 컨슈머가 레코드를 가져올 때, 브로커는 기본 30초의 시간 제한이 있는 잠금으로 해당 레코드를 컨슈머를 위해 획득합니다. 그러면 컨슈머는 레코드를 확인(즉, 처리 완료로 표시), 해제(즉, 재시도 가능하게 만들기), 거부(즉, 처리 불가능으로 표시), 또는 잠금 갱신(즉, 처리 시간 연장)할 수 있습니다.

아무런 조치가 취해지지 않으면 잠금이 자동으로 만료되고 해당 레코드는 다른 컨슈머에게 사용 가능해집니다.

이 획득 잠금 메커니즘은 처리 보장을 유지하면서 개별 파티션에서의 병렬 소비를 가능하게 합니다. 시스템은 전달 시도 횟수를 추적하며(설정 가능한 최대 5회), 향후 계획으로는 불량 레코드를 추가 처리를 위해 라우팅하는 퍼스트 클래스 데드 레터 큐(DLQ) 기능이 포함되어 있습니다.

공유 컨슈머 API는 KafkaShareConsumer 클래스를 사용하며, 이미 Kafka 컨슈머를 사용하는 개발자에게 익숙한 인터페이스를 제공합니다. 공유 컨슈머는 전통적인 컨슈머 그룹이 이미 소비하고 있는 토픽을 포함하여 기존 토픽에 추가할 수 있어, 도입이 파괴적이 아닌 추가적인 방식으로 이루어집니다. 그리고 최소한의 설정으로 KafkaConsumer 클래스를 사용하고 있다면, 새로운 KafkaShareConsumer를 컨슈머의 탄력적 스케일링을 가능하게 하는 드롭인 대체품으로 사용할 수 있을 것입니다.

다음은 기본 "암시적(implicit)" 모드를 사용하는 새로운 KafkaShareConsumer의 간단한 코드 예제입니다:

```java
Properties props = new Properties();
props.setProperty("bootstrap.servers", "localhost:9092");
props.setProperty("group.id", "myshare");

KafkaShareConsumer<String, String> consumer = new KafkaShareConsumer<>(props,
 new StringDeserializer(), new StringDeserializer());

consumer.subscribe(Arrays.asList("foo"));

while (true) {
 ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
 for (ConsumerRecord<String, String> record : records) {
   doProcessing(record);
 }
}
```

다음은 명시적 확인 응답(explicit acknowledgement)을 사용하는 의사 코드(pseudo-code)의 더 상세한 예제입니다 - 세 가지 코드 경로: 정상 경로(ack), 실패(nack/reject), 장시간 실행 작업(renew):

```java
Properties props = new Properties();
props.setProperty("bootstrap.servers", "localhost:9092");
props.setProperty("group.id", "test");
props.setProperty("share.acknowledgement.mode", "explicit");
props.setProperty("share.acquire.mode", "record_limit");
props.setProperty("max.poll.records", "1");

HashMap<Long, ConsumerRecord<String, String>> processing = new HashMap<>();

KafkaShareConsumer<String, String> consumer = new KafkaShareConsumer<>(props, new StringDeserializer(), new StringDeserializer());

consumer.setAcknowledgementCommitCallback((offsets, exception) -> {
 if (exception != null) {
   for (long offset: offsets) {
     if (processing.remove(offset) != null) {
       // 여기에 취소 또는 재시도 로직을 기록합니다
     }
   }
 }
});

consumer.subscribe(Arrays.asList("foo"));

while (true) {
 ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
 int timeToWaitMs = consumer.acquisitionLockTimeoutMs().getOrElse(10000) / 2;

 for (ConsumerRecord<String, String> record : records) {
   if (processing.put(record.offset(), record) == null) {
     // 여기에서 새 스레드에서/비동기적으로 처리를 시작합니다
   }

   if (processingCompletedSuccessfully) {
     consumer.acknowledge(record, AcknowledgeType.ACCEPT);
     processing.remove(record.offset());
   } else if (processingFailed) {
     consumer.acknowledge(record, AcknowledgeType.REJECT);
     processing.remove(record.offset());
   } else {
     consumer.acknowledge(record, AcknowledgeType.RENEW);
   }
 }
}
```

## Confluent Cloud 및 Confluent Platform에서의 Kafka를 위한 큐

GA 릴리스는 Confluent Cloud와 Confluent Platform 모두에서 Kafka를 위한 큐를 제공하여, 모든 Kafka 배포판 중 최고의 사용자 경험을 제공합니다.

Confluent Cloud는 설정이 필요 없는 완전 관리형 Kafka를 위한 큐 경험을 제공합니다. 공유 컨슈머를 생성하고, 공유 그룹을 지정하고, 처리를 시작하면 됩니다 - 브로커 설정이 필요 없습니다. 클라우드 콘솔에는 공유 컨슈머와 공유 그룹의 심층적인 가시성과 관리를 제공하는 전용 공유 그룹 사용자 인터페이스(UI)가 포함되어 있으며, Confluent CLI는 그룹 설정 지원과 함께 원활한 명령줄 작업을 제공합니다. Confluent Cloud는 또한 자동 스케일링 결정을 위한 공유 지연(share lag, 큐 깊이와 유사한 개념)을 포함한 큐 전용 메트릭을 Metrics API를 통해 제공합니다. REST API는 자동화 워크플로를 위한 프로그래밍 방식의 관리를 가능하게 합니다. 그리고 연결당 첫 번째 공유 가져오기 요청은 Confluent Cloud의 감사 로그에 기록됩니다.

Confluent Platform은 오픈 소스 Kafka에서는 사용할 수 없는 UI 가시성을 제공하는 Control Center와의 완전한 통합으로 공유 그룹을 제공합니다. Ansible Playbooks for Confluent Platform 또는 Confluent for Kubernetes를 사용하여 클러스터를 배포하고 관리하며, 고급 튜닝을 위한 브로커 레벨 설정 제어를 제공합니다. Confluent에서만 사용 가능한 새로운 공유 지연 메트릭을 포함하여 JMX 메트릭과 Control Center를 통해 공유 그룹을 모니터링합니다.

## 공유 컨슈머 API와 컨슈머 API를 언제 사용해야 하는가

Kafka를 위한 큐는 공유 컨슈머 API를 통해 Kafka 사용자에게 새로운 컨슈머 패러다임을 도입합니다. 공유 컨슈머 API와 전통적인 컨슈머 API 중 선택은 워크로드 특성에 따라 달라집니다:

**공유 컨슈머 API(공유 그룹)**는 메시지별 확인 응답, 파티션 병렬 소비, 파티션 수를 초과한 탄력적 스케일링이 필요한 운영 및 애플리케이션 워크로드에 사용합니다. 여기에는 명령 호출, 서비스 통신, 작업 실행, 워크 큐, 잡 처리, 백프레셔가 있는 이벤트 기반 워크플로가 포함됩니다. 공유 그룹은 엄격한 순서 보장보다 탄력적 스케일링을 우선시합니다.

**컨슈머 API(컨슈머 그룹)**는 엄격한 파티션별 순서와 전통적인 스트리밍 패턴이 필요한 분석 및 통합 워크로드에 사용합니다. 여기에는 데이터 파이프라인, ETL 워크로드, 순서가 보장된 데이터가 중요한 스트림 처리 애플리케이션이 포함됩니다. 컨슈머 그룹은 순서를 보장하는 1:1 파티션-컨슈머 매핑을 유지합니다.

주요 결정 요소는 순서 요구 사항, 스케일링 필요성, 메시지 처리 모델입니다. 공유 그룹은 탄력적 스케일링과 메시지별 제어를 위해 순서를 희생하는 반면, 컨슈머 그룹은 파티션 기반 스케일링 제한과 함께 엄격한 순서를 유지합니다.

## 시작하기 - 지금 Kafka를 위한 큐를 사용해 보세요

Kafka를 위한 큐는 Apache Kafka 기능의 근본적인 확장을 나타내며, 조직이 전통적인 큐에서 개발자가 기대하는 운영 단순성을 제공하면서 메시징 인프라를 통합할 수 있게 합니다. Confluent Cloud와 Confluent Platform의 차별화된 경험은 귀하의 요구 사항에 맞는 배포 모델에서 Kafka를 위한 큐를 실행할 수 있도록 보장합니다.

Kafka를 위한 큐는 Enterprise 및 Dedicated 클러스터를 위한 Confluent Cloud에서 사용 가능하며, Confluent Platform 8.2와 함께 제공됩니다. 현재 Apache Kafka 4.2+ Java 클라이언트가 지원됩니다. 추가 Confluent Cloud 클러스터 유형 및 비Java 클라이언트 지원은 2026년 하반기를 목표로 하고 있습니다.

## 앞으로의 계획

GA 릴리스는 시작에 불과합니다. 단기 개선 사항으로는 2026년에 비Java 클라이언트 지원과 Confluent Cloud의 추가 클러스터 유형 지원이 포함됩니다. 전달 불가능한 레코드를 전용 DLQ 토픽으로 자동 복사하기 위한 DLQ 지원(KIP-1191, 2026년 Apache Kafka 4.4 목표)을 적극적으로 개발하고 있습니다.

향후 로드맵 항목에는 Kafka 트랜잭션에서의 전달 확인을 통한 정확히 한 번(exactly-once) 시맨틱스, 재전달 시도를 위한 지수 백오프(exponential back-off), 그리고 레코드 키별 순서 보장과 파티션 공유를 결합하는 중요한 개선 사항인 키 기반 순서 보장(key-based ordering)이 포함됩니다.

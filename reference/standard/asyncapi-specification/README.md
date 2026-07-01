# AsyncAPI Specification 상세 정리

## 목차

1. [개요](#1-개요)
2. [역사](#2-역사)
3. [핵심 개념 및 구조](#3-핵심-개념-및-구조)
4. [버전별 차이](#4-버전별-차이)
5. [주요 키워드 및 필드 설명](#5-주요-키워드-및-필드-설명)
6. [사용 사례 및 활용 도구](#6-사용-사례-및-활용-도구)
7. [장점과 단점](#7-장점과-단점)
8. [실제 예시](#8-실제-예시)
9. [관련 생태계 및 커뮤니티](#9-관련-생태계-및-커뮤니티)

---

## 1. 개요

### AsyncAPI란

AsyncAPI Specification은 이벤트 기반(Event-Driven) 및 비동기(Asynchronous) API를 기술하기 위한 표준 인터페이스 정의 언어이다. OpenAPI Specification이 RESTful HTTP API의 표준 명세 역할을 수행하는 것과 마찬가지로, AsyncAPI는 메시지 브로커(Kafka, RabbitMQ 등), 메시징 프로토콜(MQTT, AMQP, WebSocket 등)을 통한 비동기 통신의 모든 측면을 선언적으로 기술한다. JSON 또는 YAML 형식으로 작성되며, 메시지의 구조, 채널(토픽/큐), 서버 정보, 프로토콜 바인딩 등을 정의할 수 있다.

### 왜 만들어졌는가

마이크로서비스 아키텍처와 이벤트 기반 아키텍처(EDA)의 확산으로 비동기 API의 사용이 급증했으나, 이를 체계적으로 기술할 수 있는 표준 명세가 부재했다. 다음과 같은 문제들이 대두되었다.

- 비동기 API 명세의 부재: OpenAPI는 HTTP 요청-응답 패턴만을 대상으로 하므로, Kafka 토픽이나 MQTT 메시지 같은 비동기 통신을 기술할 방법이 없었다. 이벤트의 구조와 채널 정보가 코드 주석이나 위키 문서에 산재하여, 서비스 간 통합 시 혼란이 발생했다.
- 이벤트 카탈로그의 필요: 수십 개의 마이크로서비스가 수백 개의 이벤트를 주고받는 환경에서, 어떤 서비스가 어떤 이벤트를 발행(publish)하고 구독(subscribe)하는지 파악하기 어려웠다.
- 코드 생성 및 자동화의 한계: 동기 API는 OpenAPI를 통해 클라이언트 SDK와 서버 스텁을 자동 생성할 수 있었으나, 비동기 API에는 동등한 자동화 파이프라인이 존재하지 않았다.
- 프로토콜 다양성에 대한 추상화 부재: Kafka, RabbitMQ, MQTT, NATS, WebSocket 등 다양한 메시징 프로토콜이 사용되지만, 이들을 통합적으로 기술할 수 있는 프로토콜 중립적인 명세가 없었다.
- 계약 기반 개발의 부재: 동기 API에서는 OpenAPI 명세를 기반으로 계약 테스트를 수행할 수 있었으나, 비동기 API에서는 메시지 형식의 변경이 하위 호환성을 깨뜨리는지 검증할 수 없었다.

AsyncAPI는 이러한 문제를 해결하기 위해 이벤트 기반 아키텍처를 위한 단일 진실 공급원(Single Source of Truth) 역할을 수행하도록 설계되었다.

### 핵심 철학

| 원칙 | 설명 |
|------|------|
| 프로토콜 중립성 | 특정 메시징 프로토콜에 종속되지 않으며, Kafka, MQTT, AMQP, WebSocket, NATS 등 다양한 프로토콜을 동일한 형식으로 기술한다 |
| OpenAPI와의 유사성 | OpenAPI에 익숙한 개발자가 최소한의 학습으로 AsyncAPI를 사용할 수 있도록, 유사한 구조와 용어를 채택했다 |
| 사람과 기계 모두를 위한 설계 | 개발자가 읽을 수 있으면서도 도구가 파싱할 수 있는 형식이다 |
| 선언적 기술 | 메시지가 "무엇"인지를 기술하며, "어떻게" 처리하는지는 다루지 않는다 |
| 재사용성 | `components`를 통해 메시지, 스키마, 채널 등을 재사용 가능하게 정의한다 |
| 확장성 | 프로토콜별 바인딩(bindings)을 통해 각 프로토콜 고유의 설정을 기술할 수 있다 |

---

## 2. 역사

### 탄생 배경 (2017)

AsyncAPI는 Fran Mendez가 2017년 자신이 근무하던 스타트업 Hitch에서 시작한 프로젝트이다. 당시 Hitch는 IoT 장치와 마이크로서비스 간의 비동기 통신을 구축하고 있었는데, 이벤트 기반 API를 문서화할 표준적인 방법이 없어 어려움을 겪었다. Fran Mendez는 OpenAPI(당시 Swagger)가 REST API에 제공하는 것과 동일한 수준의 명세를 비동기 API에도 제공해야 한다는 필요성을 느끼고, OpenAPI의 구조를 참고하여 AsyncAPI의 첫 버전을 설계했다.

### 초기 발전 (2017-2019)

- 2017년: AsyncAPI 1.0.0이 공개되었다. OpenAPI 2.0(Swagger)의 구조를 기반으로 비동기 API를 기술할 수 있는 최초의 체계적인 명세였다. `topics`라는 개념으로 메시지 채널을 정의했다.
- 2018년: AsyncAPI 1.1.0 및 1.2.0이 발표되었다. 프로토콜 바인딩 개념이 도입되기 시작했으며, 커뮤니티의 피드백을 반영하여 명세가 정교해졌다.
- 2019년 9월: AsyncAPI 2.0.0이 발표되었다. 이 버전에서 대대적인 구조 개편이 이루어졌다. `topics`가 `channels`로 변경되었고, `servers` 개념이 도입되었으며, 프로토콜 바인딩이 공식적으로 체계화되었다. OpenAPI 3.0의 구조적 영향을 받아 `components`가 도입되었다.

### AsyncAPI Initiative와 Linux Foundation (2019-2021)

- 2019년: AsyncAPI 프로젝트가 단일 개인 프로젝트에서 커뮤니티 주도 프로젝트로 전환되기 시작했다. AsyncAPI Initiative가 조직되어 거버넌스 체계가 수립되었다.
- 2020년: AsyncAPI 2.1.0, 2.2.0이 발표되며 지속적으로 개선되었다. Generator, Parser 등 핵심 도구들이 개발되었다.
- 2021년 3월: AsyncAPI Initiative가 Linux Foundation에 합류했다. 이를 통해 AsyncAPI는 벤더 중립적인 오픈 거버넌스 체계를 확보하고, 기업 채택을 위한 신뢰성을 높였다.

### 성숙기 (2021-현재)

- 2021년: AsyncAPI 2.3.0, 2.4.0이 발표되었다. 메시지 트레이트(traits), 서버 변수 등 고급 기능이 추가되었다.
- 2022년: AsyncAPI 2.5.0, 2.6.0이 발표되었다. 명세의 안정성과 도구 생태계가 성숙해졌다.
- 2023년 9월: AsyncAPI 3.0.0이 발표되었다. 이 버전은 가장 큰 규모의 구조적 변경을 포함하며, `operations`가 `channels`에서 분리되고, `send`/`receive` 개념이 도입되었다. Request-Reply 패턴 지원도 추가되었다.
- 2024년: AsyncAPI 3.0 생태계의 도구 지원이 확대되었다. Modelina, Studio 등이 3.0을 완전 지원하기 시작했다.

### 타임라인 요약

```
2017       Fran Mendez, Hitch에서 AsyncAPI 프로젝트 시작
2017       AsyncAPI 1.0.0 공개
2018       AsyncAPI 1.1.0 / 1.2.0 발표
2019.09    AsyncAPI 2.0.0 발표 (대규모 구조 개편: channels, servers 도입)
2019       AsyncAPI Initiative 조직
2020       AsyncAPI 2.1.0 / 2.2.0 발표
2021.03    Linux Foundation 합류
2021       AsyncAPI 2.3.0 / 2.4.0 발표
2022       AsyncAPI 2.5.0 / 2.6.0 발표
2023.09    AsyncAPI 3.0.0 발표 (operations 분리, send/receive 도입)
2024       AsyncAPI 3.0 도구 생태계 확대
```

---

## 3. 핵심 개념 및 구조

AsyncAPI 3.0 기준으로 문서의 구조와 각 구성 요소를 상세히 설명한다. AsyncAPI 3.0은 2.x 대비 상당한 구조적 변경이 이루어졌으며, 특히 `operations`가 `channels`에서 분리된 것이 가장 큰 변화이다.

### 3.1 최상위 구조

```yaml
asyncapi: "3.0.0"           # 필수. AsyncAPI 버전
info: { ... }                # 필수. API 메타데이터
servers: { ... }             # 메시지 브로커/서버 정보
defaultContentType: "..."    # 기본 메시지 콘텐츠 타입
channels: { ... }            # 메시지 채널(토픽/큐) 정의
operations: { ... }          # 오퍼레이션(send/receive) 정의
components: { ... }          # 재사용 가능한 컴포넌트
```

AsyncAPI 3.0에서는 `channels`와 `operations`가 명확히 분리되었다. 채널은 메시지가 전달되는 경로를 정의하고, 오퍼레이션은 해당 채널에서 수행되는 동작을 정의한다.

### 3.2 Info Object

API에 대한 기본 메타데이터를 정의한다.

```yaml
info:
  title: "주문 관리 이벤트 API"          # 필수. API 이름
  version: "1.0.0"                       # 필수. API 버전
  description: |
    전자상거래 시스템의 주문 관련 이벤트를 정의한다.
    Kafka를 통해 마이크로서비스 간 이벤트를 교환한다.
  termsOfService: "https://example.com/terms"
  contact:
    name: "이벤트 플랫폼 팀"
    url: "https://example.com/support"
    email: "events@example.com"
  license:
    name: "Apache 2.0"
    url: "https://www.apache.org/licenses/LICENSE-2.0.html"
  tags:
    - name: "orders"
      description: "주문 관련 이벤트"
    - name: "inventory"
      description: "재고 관련 이벤트"
  externalDocs:
    description: "이벤트 아키텍처 문서"
    url: "https://docs.example.com/events"
```

### 3.3 Servers

메시지 브로커 또는 서버의 연결 정보를 정의한다. OpenAPI의 `servers`와 유사하지만, 프로토콜(protocol) 필드가 핵심적인 차이이다.

```yaml
servers:
  productionKafka:
    host: "kafka.example.com:9092"
    protocol: kafka
    protocolVersion: "3.5.0"
    description: "운영 Kafka 클러스터"
    security:
      - $ref: '#/components/securitySchemes/saslScram'
    tags:
      - name: "production"
    bindings:
      kafka:
        schemaRegistryUrl: "https://schema-registry.example.com"
        schemaRegistryVendor: "confluent"

  stagingKafka:
    host: "kafka-staging.example.com:9092"
    protocol: kafka
    description: "스테이징 Kafka 클러스터"

  productionMqtt:
    host: "mqtt.example.com:8883"
    protocol: mqtt
    protocolVersion: "5.0"
    description: "운영 MQTT 브로커"
    security:
      - $ref: '#/components/securitySchemes/userPassword'

  websocketServer:
    host: "ws.example.com"
    pathname: "/events"
    protocol: ws
    description: "WebSocket 이벤트 서버"
```

지원하는 주요 프로토콜:

| 프로토콜 | `protocol` 값 | 설명 |
|----------|---------------|------|
| Apache Kafka | `kafka` / `kafka-secure` | 대용량 분산 메시지 스트리밍 플랫폼 |
| MQTT | `mqtt` / `secure-mqtt` | 경량 IoT 메시징 프로토콜 |
| AMQP | `amqp` / `amqps` | 고급 메시지 큐잉 프로토콜 (RabbitMQ 등) |
| WebSocket | `ws` / `wss` | 양방향 실시간 통신 프로토콜 |
| NATS | `nats` | 클라우드 네이티브 메시징 시스템 |
| Apache Pulsar | `pulsar` / `pulsar+ssl` | 분산 메시징 및 스트리밍 플랫폼 |
| Redis | `redis` / `rediss` | Redis Pub/Sub |
| HTTP | `http` / `https` | HTTP 기반 비동기 통신 (Webhook 등) |
| Google Pub/Sub | `googlepubsub` | Google Cloud Pub/Sub |
| Amazon SQS/SNS | `sqs` / `sns` | AWS 메시징 서비스 |
| Solace | `solace` | Solace PubSub+ |
| STOMP | `stomp` / `stomps` | Simple Text Oriented Messaging Protocol |
| JMS | `jms` | Java Message Service |

AsyncAPI 3.0에서 서버 객체의 URL이 `host`와 `pathname`으로 분리되었다. 2.x에서는 `url` 하나로 통합되어 있었다.

### 3.4 Channels

채널(channel) 은 메시지가 전달되는 경로를 정의한다. Kafka에서는 토픽(topic), RabbitMQ에서는 큐(queue) 또는 익스체인지(exchange), MQTT에서는 토픽(topic)에 해당한다.

AsyncAPI 3.0에서 채널은 오퍼레이션과 분리되어, 채널 자체는 메시지의 구조와 경로만을 정의한다.

```yaml
channels:
  orderCreated:
    address: "orders.created"
    description: "새로운 주문이 생성되었을 때 발행되는 이벤트"
    messages:
      OrderCreatedMessage:
        $ref: '#/components/messages/OrderCreated'
    parameters:
      storeId:
        description: "매장 식별자"
    servers:
      - $ref: '#/servers/productionKafka'
    bindings:
      kafka:
        topic: "orders.created"
        partitions: 12
        replicas: 3

  orderConfirmed:
    address: "orders.confirmed"
    description: "주문이 확인되었을 때 발행되는 이벤트"
    messages:
      OrderConfirmedMessage:
        $ref: '#/components/messages/OrderConfirmed'

  inventoryChanged:
    address: "inventory.changed.{warehouseId}"
    description: "재고가 변경되었을 때 발행되는 이벤트"
    parameters:
      warehouseId:
        description: "창고 식별자"
        schema:
          type: string
    messages:
      InventoryChangedMessage:
        $ref: '#/components/messages/InventoryChanged'
```

3.0에서의 주요 변경사항:
- 2.x에서는 `channels`가 `publish`/`subscribe` 오퍼레이션을 직접 포함했으나, 3.0에서는 채널이 메시지와 경로만 정의한다
- `address` 필드가 도입되어 채널의 실제 주소(토픽명 등)를 명시한다
- 채널 내에서 `messages`를 맵 형태로 정의한다

### 3.5 Operations

오퍼레이션(operation) 은 채널에서 수행되는 동작을 정의한다. AsyncAPI 3.0에서 가장 큰 변화 중 하나로, 기존의 `publish`/`subscribe`가 `send`/`receive`로 대체되었다.

```yaml
operations:
  publishOrderCreated:
    action: send
    channel:
      $ref: '#/channels/orderCreated'
    summary: "주문 생성 이벤트 발행"
    description: "새로운 주문이 생성될 때 이 이벤트를 발행한다."
    tags:
      - name: "orders"
    messages:
      - $ref: '#/channels/orderCreated/messages/OrderCreatedMessage'
    bindings:
      kafka:
        groupId:
          type: string
        clientId:
          type: string

  consumeOrderCreated:
    action: receive
    channel:
      $ref: '#/channels/orderCreated'
    summary: "주문 생성 이벤트 수신"
    description: "주문 생성 이벤트를 수신하여 처리한다."
    messages:
      - $ref: '#/channels/orderCreated/messages/OrderCreatedMessage'

  publishOrderConfirmed:
    action: send
    channel:
      $ref: '#/channels/orderConfirmed'
    summary: "주문 확인 이벤트 발행"
    messages:
      - $ref: '#/channels/orderConfirmed/messages/OrderConfirmedMessage'

  publishInventoryChanged:
    action: send
    channel:
      $ref: '#/channels/inventoryChanged'
    summary: "재고 변경 이벤트 발행"
    messages:
      - $ref: '#/channels/inventoryChanged/messages/InventoryChangedMessage'
```

`action` 필드의 의미:

| 값 | 설명 | 2.x 대응 |
|-----|------|-----------|
| `send` | 이 애플리케이션이 해당 채널로 메시지를 보낸다 | `publish` (관점에 따라 혼란이 있었음) |
| `receive` | 이 애플리케이션이 해당 채널에서 메시지를 받는다 | `subscribe` (관점에 따라 혼란이 있었음) |

2.x에서는 `publish`/`subscribe`의 의미가 "애플리케이션 관점"인지 "채널 관점"인지 혼란이 있었다. 3.0에서는 항상 명세를 작성하는 애플리케이션의 관점에서 `send`/`receive`를 사용하여 모호함을 제거했다.

Request-Reply 패턴 (3.0 신규):

AsyncAPI 3.0에서는 오퍼레이션에 `reply` 필드를 추가하여 요청-응답 패턴을 기술할 수 있다.

```yaml
operations:
  getOrderStatus:
    action: send
    channel:
      $ref: '#/channels/orderStatusRequest'
    summary: "주문 상태 조회 요청"
    messages:
      - $ref: '#/channels/orderStatusRequest/messages/OrderStatusRequest'
    reply:
      channel:
        $ref: '#/channels/orderStatusResponse'
      messages:
        - $ref: '#/channels/orderStatusResponse/messages/OrderStatusResponse'
```

### 3.6 Messages

메시지(message) 는 채널을 통해 전달되는 데이터의 구조를 정의한다. 헤더, 페이로드, 상관관계 ID 등을 포함할 수 있다.

```yaml
components:
  messages:
    OrderCreated:
      name: "OrderCreated"
      title: "주문 생성 이벤트"
      summary: "새로운 주문이 생성되었음을 알리는 이벤트"
      contentType: "application/json"
      correlationId:
        location: "$message.header#/correlationId"
        description: "이벤트 추적을 위한 상관관계 ID"
      headers:
        type: object
        properties:
          correlationId:
            type: string
            format: uuid
            description: "상관관계 ID"
          eventTime:
            type: string
            format: date-time
            description: "이벤트 발생 시각"
          eventType:
            type: string
            const: "OrderCreated"
          source:
            type: string
            description: "이벤트 발행 서비스명"
      payload:
        type: object
        required:
          - orderId
          - customerId
          - items
          - totalAmount
          - createdAt
        properties:
          orderId:
            type: string
            format: uuid
            description: "주문 고유 식별자"
          customerId:
            type: string
            format: uuid
            description: "고객 식별자"
          items:
            type: array
            minItems: 1
            items:
              $ref: '#/components/schemas/OrderItem'
          totalAmount:
            type: number
            format: double
            minimum: 0
            description: "총 주문 금액"
          currency:
            type: string
            enum: [KRW, USD, EUR, JPY]
            default: KRW
            description: "통화 코드"
          createdAt:
            type: string
            format: date-time
            description: "주문 생성 시각"
      traits:
        - $ref: '#/components/messageTraits/commonHeaders'
      bindings:
        kafka:
          key:
            type: string
            description: "Kafka 메시지 키 (orderId 사용)"
          schemaIdLocation: "header"
          schemaIdPayloadEncoding: "4"
```

### 3.7 Components

재사용 가능한 객체들을 정의하는 컨테이너이다. OpenAPI의 `components`와 동일한 역할을 수행한다.

```yaml
components:
  schemas: { ... }           # 데이터 모델 (JSON Schema)
  servers: { ... }           # 재사용 가능한 서버 정의
  channels: { ... }          # 재사용 가능한 채널 정의
  operations: { ... }        # 재사용 가능한 오퍼레이션 정의
  messages: { ... }          # 메시지 정의
  securitySchemes: { ... }   # 보안 스킴 정의
  serverVariables: { ... }   # 서버 변수 정의
  parameters: { ... }        # 채널 파라미터 정의
  correlationIds: { ... }    # 상관관계 ID 정의
  operationTraits: { ... }   # 오퍼레이션 트레이트
  messageTraits: { ... }     # 메시지 트레이트
  serverBindings: { ... }    # 서버 바인딩
  channelBindings: { ... }   # 채널 바인딩
  operationBindings: { ... } # 오퍼레이션 바인딩
  messageBindings: { ... }   # 메시지 바인딩
  externalDocs: { ... }      # 외부 문서
  tags: { ... }              # 태그
```

트레이트(Traits):

트레이트는 여러 메시지나 오퍼레이션에 공통으로 적용되는 속성을 정의하는 메커니즘이다. 코드의 믹스인(mixin)과 유사한 개념으로, 중복을 줄이고 일관성을 유지하는 데 활용된다.

```yaml
components:
  messageTraits:
    commonHeaders:
      headers:
        type: object
        properties:
          correlationId:
            type: string
            format: uuid
          eventTime:
            type: string
            format: date-time
          source:
            type: string
          version:
            type: string
            default: "1.0"

  operationTraits:
    kafkaConsumer:
      bindings:
        kafka:
          groupId:
            type: string
          clientId:
            type: string
```

---

## 4. 버전별 차이

### 4.1 AsyncAPI 1.x vs 2.x vs 3.0 비교

| 항목 | 1.x | 2.x | 3.0 |
|------|-----|-----|-----|
| 최상위 키 | `asyncapi: "1.x.x"` | `asyncapi: "2.x.x"` | `asyncapi: "3.0.0"` |
| 채널 정의 | `topics` | `channels` (publish/subscribe 포함) | `channels` (메시지/주소만 정의) |
| 오퍼레이션 | `topics` 내에 포함 | `channels` 내 `publish`/`subscribe` | 독립된 `operations` (send/receive) |
| 서버 정의 | 제한적 | `servers` 객체 (URL 통합) | `servers` 객체 (host/pathname 분리) |
| 서버 URL | - | `url` 단일 필드 | `host` + `pathname` 분리 |
| 메시지 정의 | 인라인 | `components/messages` + 인라인 | `components/messages` + 채널 내 참조 |
| 프로토콜 바인딩 | 미지원 | `bindings` 지원 | `bindings` 지원 (확장됨) |
| Request-Reply | 미지원 | 미지원 | `reply` 필드로 지원 |
| 보안 | 제한적 | `securitySchemes` | `securitySchemes` (서버/오퍼레이션 레벨) |
| 트레이트 | 미지원 | `messageTraits`, `operationTraits` | `messageTraits`, `operationTraits` |
| $ref | 제한적 | 지원 | 지원 (확장됨) |
| defaultContentType | 미지원 | 지원 | 지원 |
| 채널 주소 | 키 자체가 주소 | 키 자체가 주소 | `address` 필드로 분리 |
| 다중 메시지 | 미지원 | `oneOf` 사용 | 채널 내 `messages` 맵 |

### 4.2 2.x에서 3.0으로의 주요 변경점

구조적 변경:

| 2.x | 3.0 | 설명 |
|-----|-----|------|
| `channels.{name}.publish` | `operations.{name}.action: send` | 발행 오퍼레이션이 channels에서 분리 |
| `channels.{name}.subscribe` | `operations.{name}.action: receive` | 구독 오퍼레이션이 channels에서 분리 |
| `servers.{name}.url` | `servers.{name}.host` + `pathname` | 서버 URL이 분리됨 |
| 채널 키 = 주소 | `channels.{name}.address` | 채널 식별자와 주소가 분리됨 |
| `channels.{name}.publish.message` | `channels.{name}.messages` | 메시지가 채널 레벨에서 맵으로 정의 |
| `oneOf` (다중 메시지) | 채널 내 `messages` 맵 | 다중 메시지 지원 방식 변경 |

의미적 변경 - publish/subscribe에서 send/receive로:

2.x에서 `publish`와 `subscribe`의 의미가 혼란스러웠다. 예를 들어, 명세를 작성하는 애플리케이션이 Kafka 토픽에 메시지를 보내는 경우, 이것이 `publish`인지(애플리케이션이 발행) 아니면 `subscribe`인지(다른 애플리케이션이 구독할 수 있도록) 모호했다.

```yaml
# 2.x - 모호한 의미
channels:
  orders/created:
    publish:    # 이 앱이 발행? 아니면 이 채널에 publish 가능?
      message:
        $ref: '#/components/messages/OrderCreated'

# 3.0 - 명확한 의미 (항상 이 앱의 관점)
operations:
  publishOrderCreated:
    action: send    # 이 앱이 메시지를 보냄 (명확)
    channel:
      $ref: '#/channels/orderCreated'
```

새로운 기능:

- Request-Reply 패턴: `operations.{name}.reply` 필드로 요청-응답 패턴을 기술할 수 있다
- 채널 재사용 강화: 채널과 오퍼레이션이 분리됨으로써, 동일한 채널을 여러 오퍼레이션에서 참조할 수 있다
- 서버-채널 연결: 채널이 특정 서버에만 연결됨을 명시할 수 있다

### 4.3 마이그레이션 시 주의사항

2.x에서 3.0으로 마이그레이션할 때 주요 작업은 다음과 같다.

1. `channels` 내의 `publish`/`subscribe`를 별도의 `operations`로 이동한다
2. `publish`를 `action: send`로, `subscribe`를 `action: receive`로 변환한다
3. 서버 `url`을 `host`와 `pathname`으로 분리한다
4. 채널 키를 논리적 이름으로 변경하고, 실제 주소를 `address` 필드에 기입한다
5. 채널 내 메시지를 `messages` 맵 형태로 변환한다

AsyncAPI는 공식적으로 `@asyncapi/converter` 라이브러리를 제공하여 2.x 문서를 3.0으로 자동 변환할 수 있도록 지원한다.

---

## 5. 주요 키워드 및 필드 설명

### 5.1 프로토콜 바인딩 (Protocol Bindings)

프로토콜 바인딩은 특정 메시징 프로토콜에 고유한 설정을 기술하기 위한 확장 메커니즘이다. AsyncAPI의 핵심 명세는 프로토콜 중립적이지만, 실제 시스템에서는 프로토콜별 세부 설정이 필요하다. 바인딩은 서버, 채널, 오퍼레이션, 메시지 네 가지 레벨에서 정의할 수 있다.

Kafka 바인딩:

```yaml
# 채널 바인딩
channels:
  orderEvents:
    address: "orders.events"
    bindings:
      kafka:
        topic: "orders.events"
        partitions: 12
        replicas: 3
        topicConfiguration:
          cleanup.policy: ["delete"]
          retention.ms: 604800000        # 7일
          max.message.bytes: 1048576     # 1MB
          min.insync.replicas: 2

# 오퍼레이션 바인딩
operations:
  consumeOrderEvents:
    action: receive
    channel:
      $ref: '#/channels/orderEvents'
    bindings:
      kafka:
        groupId:
          type: string
          enum: ["order-processing-group"]
        clientId:
          type: string
          enum: ["order-processor-01"]
        bindingVersion: "0.5.0"

# 메시지 바인딩
components:
  messages:
    OrderCreated:
      bindings:
        kafka:
          key:
            type: string
            description: "파티션 키 (orderId)"
          schemaIdLocation: "header"
          schemaIdPayloadEncoding: "confluent"
          bindingVersion: "0.5.0"

# 서버 바인딩
servers:
  kafka:
    host: "kafka.example.com:9092"
    protocol: kafka
    bindings:
      kafka:
        schemaRegistryUrl: "https://schema-registry.example.com"
        schemaRegistryVendor: "confluent"
        bindingVersion: "0.5.0"
```

| Kafka 바인딩 필드 | 레벨 | 설명 |
|-------------------|------|------|
| `topic` | 채널 | Kafka 토픽명 |
| `partitions` | 채널 | 파티션 수 |
| `replicas` | 채널 | 복제 팩터 |
| `topicConfiguration` | 채널 | 토픽 설정 (retention, cleanup 등) |
| `groupId` | 오퍼레이션 | 컨슈머 그룹 ID |
| `clientId` | 오퍼레이션 | 클라이언트 ID |
| `key` | 메시지 | 메시지 키 스키마 |
| `schemaIdLocation` | 메시지 | 스키마 ID 위치 (header/payload) |
| `schemaRegistryUrl` | 서버 | 스키마 레지스트리 URL |

MQTT 바인딩:

```yaml
# 서버 바인딩
servers:
  mqttBroker:
    host: "mqtt.example.com:8883"
    protocol: secure-mqtt
    bindings:
      mqtt:
        clientId: "iot-gateway-01"
        cleanSession: true
        lastWill:
          topic: "devices/iot-gateway-01/status"
          qos: 1
          message: '{"status": "offline"}'
          retain: true
        keepAlive: 60
        sessionExpiryInterval: 3600
        maximumPacketSize: 65536
        bindingVersion: "0.2.0"

# 오퍼레이션 바인딩
operations:
  publishTemperature:
    action: send
    channel:
      $ref: '#/channels/temperatureReading'
    bindings:
      mqtt:
        qos: 1
        retain: false
        messageExpiryInterval: 300
        bindingVersion: "0.2.0"

  subscribeAlerts:
    action: receive
    channel:
      $ref: '#/channels/deviceAlerts'
    bindings:
      mqtt:
        qos: 2
        bindingVersion: "0.2.0"
```

| MQTT 바인딩 필드 | 레벨 | 설명 |
|-----------------|------|------|
| `clientId` | 서버 | MQTT 클라이언트 ID |
| `cleanSession` | 서버 | 클린 세션 여부 |
| `lastWill` | 서버 | 유언(Last Will) 메시지 설정 |
| `keepAlive` | 서버 | 킵얼라이브 간격 (초) |
| `qos` | 오퍼레이션 | QoS 레벨 (0: At most once, 1: At least once, 2: Exactly once) |
| `retain` | 오퍼레이션 | 메시지 보존(retain) 여부 |
| `messageExpiryInterval` | 오퍼레이션 | 메시지 만료 시간 (초) |

AMQP 바인딩:

```yaml
# 채널 바인딩
channels:
  orderQueue:
    address: "order-processing-queue"
    bindings:
      amqp:
        is: routingKey
        exchange:
          name: "orders-exchange"
          type: topic
          durable: true
          autoDelete: false
          vhost: "/production"
        queue:
          name: "order-processing-queue"
          durable: true
          exclusive: false
          autoDelete: false
          deadLetterExchange: "orders-dlx"
          deadLetterRoutingKey: "orders.failed"
          messageTtl: 86400000
          maxLength: 100000
        bindingVersion: "0.3.0"

# 오퍼레이션 바인딩
operations:
  consumeOrders:
    action: receive
    channel:
      $ref: '#/channels/orderQueue'
    bindings:
      amqp:
        ack: true
        prefetchCount: 10
        bindingVersion: "0.3.0"

# 메시지 바인딩
components:
  messages:
    OrderCreated:
      bindings:
        amqp:
          contentEncoding: "utf-8"
          messageType: "OrderCreated"
          bindingVersion: "0.3.0"
```

| AMQP 바인딩 필드 | 레벨 | 설명 |
|-----------------|------|------|
| `is` | 채널 | `routingKey` 또는 `queue` |
| `exchange` | 채널 | 익스체인지 설정 (name, type, durable 등) |
| `queue` | 채널 | 큐 설정 (name, durable, DLX 등) |
| `ack` | 오퍼레이션 | 메시지 확인(ack) 필요 여부 |
| `prefetchCount` | 오퍼레이션 | 프리페치 카운트 |
| `contentEncoding` | 메시지 | 콘텐츠 인코딩 |
| `messageType` | 메시지 | AMQP 메시지 타입 |

WebSocket 바인딩:

```yaml
# 채널 바인딩
channels:
  notifications:
    address: "/notifications"
    bindings:
      ws:
        method: GET
        query:
          type: object
          properties:
            token:
              type: string
              description: "인증 토큰"
            channels:
              type: array
              items:
                type: string
              description: "구독할 채널 목록"
        headers:
          type: object
          properties:
            Authorization:
              type: string
              description: "Bearer 인증 토큰"
        bindingVersion: "0.1.0"
```

| WebSocket 바인딩 필드 | 레벨 | 설명 |
|---------------------|------|------|
| `method` | 채널 | HTTP 메서드 (핸드셰이크 시) |
| `query` | 채널 | 쿼리 파라미터 스키마 |
| `headers` | 채널 | HTTP 헤더 스키마 |

### 5.2 메시지 구조 상세

AsyncAPI에서 메시지는 다음과 같은 구성 요소로 이루어진다.

| 필드 | 설명 |
|------|------|
| `headers` | 메시지 헤더의 스키마. 메타데이터(상관관계 ID, 이벤트 타입, 타임스탬프 등)를 포함한다 |
| `payload` | 메시지 본문의 스키마. 실제 비즈니스 데이터를 정의한다 |
| `correlationId` | 메시지의 상관관계를 추적하기 위한 식별자. `location`으로 헤더 또는 페이로드 내 위치를 지정한다 |
| `contentType` | 메시지 페이로드의 콘텐츠 타입. 예: `application/json`, `application/avro`, `application/protobuf` |
| `name` | 메시지의 기계 판독 가능한 이름 |
| `title` | 메시지의 사람 판독 가능한 이름 |
| `summary` | 메시지의 간단한 설명 |
| `description` | 메시지의 상세 설명 (Markdown 지원) |
| `tags` | 메시지 분류를 위한 태그 |
| `externalDocs` | 외부 문서 링크 |
| `bindings` | 프로토콜별 바인딩 |
| `examples` | 메시지 예시 |
| `traits` | 적용할 메시지 트레이트 목록 |
| `schemaFormat` | 페이로드 스키마의 형식 (JSON Schema, Avro, Protobuf 등) |

correlationId 상세:

```yaml
components:
  correlationIds:
    orderCorrelation:
      description: "주문 이벤트 추적을 위한 상관관계 ID"
      location: "$message.header#/correlationId"

    payloadCorrelation:
      description: "페이로드 내 상관관계 ID"
      location: "$message.payload#/metadata/traceId"
```

`location` 필드는 런타임 표현식을 사용하여 상관관계 ID의 위치를 지정한다.

| 표현식 | 설명 |
|--------|------|
| `$message.header#/correlationId` | 메시지 헤더의 `correlationId` 필드 |
| `$message.payload#/id` | 메시지 페이로드의 `id` 필드 |
| `$message.payload#/metadata/traceId` | 중첩된 경로의 필드 |

다양한 스키마 형식 지원:

AsyncAPI는 JSON Schema 외에도 다양한 스키마 형식을 지원한다.

```yaml
components:
  messages:
    # JSON Schema (기본)
    OrderCreatedJson:
      contentType: "application/json"
      payload:
        type: object
        properties:
          orderId:
            type: string

    # Apache Avro
    OrderCreatedAvro:
      contentType: "application/avro"
      schemaFormat: "application/vnd.apache.avro;version=1.11.0"
      payload:
        type: record
        name: OrderCreated
        namespace: com.example.events
        fields:
          - name: orderId
            type: string
          - name: amount
            type: double

    # Protocol Buffers 참조
    OrderCreatedProtobuf:
      contentType: "application/protobuf"
      schemaFormat: "application/vnd.google.protobuf;version=3"
      payload:
        $ref: "path/to/order.proto#OrderCreated"
```

### 5.3 스키마 검증 키워드

AsyncAPI는 JSON Schema를 기반으로 스키마를 정의한다. OpenAPI 3.1과 마찬가지로 JSON Schema Draft 2020-12의 키워드를 사용할 수 있다.

문자열 검증:

| 키워드 | 설명 |
|--------|------|
| `minLength` | 최소 문자열 길이 |
| `maxLength` | 최대 문자열 길이 |
| `pattern` | 정규표현식 패턴 |
| `enum` | 허용 값 목록 |
| `const` | 고정 값 |
| `format` | 형식 힌트 (date-time, uuid, email, uri 등) |

숫자 검증:

| 키워드 | 설명 |
|--------|------|
| `minimum` | 최솟값 (이상) |
| `maximum` | 최댓값 (이하) |
| `exclusiveMinimum` | 최솟값 미포함 |
| `exclusiveMaximum` | 최댓값 미포함 |
| `multipleOf` | 배수 조건 |

배열 검증:

| 키워드 | 설명 |
|--------|------|
| `items` | 배열 요소의 스키마 |
| `minItems` | 최소 요소 수 |
| `maxItems` | 최대 요소 수 |
| `uniqueItems` | 요소 중복 불허 |
| `contains` | 배열에 포함되어야 하는 요소 스키마 |

객체 검증:

| 키워드 | 설명 |
|--------|------|
| `properties` | 프로퍼티 정의 |
| `required` | 필수 프로퍼티 목록 |
| `additionalProperties` | 정의되지 않은 프로퍼티 허용 여부/스키마 |
| `minProperties` | 최소 프로퍼티 수 |
| `maxProperties` | 최대 프로퍼티 수 |
| `patternProperties` | 정규식 패턴으로 프로퍼티명 매칭 |

조합 키워드:

| 키워드 | 설명 |
|--------|------|
| `allOf` | 모든 스키마를 동시에 만족 |
| `oneOf` | 정확히 하나의 스키마만 만족 |
| `anyOf` | 하나 이상의 스키마를 만족 |
| `not` | 지정된 스키마를 만족하지 않아야 함 |
| `if`/`then`/`else` | 조건부 스키마 |

### 5.4 보안 스킴

AsyncAPI에서 지원하는 보안 스킴 유형은 다음과 같다.

```yaml
components:
  securitySchemes:
    saslScram:
      type: scramSha256
      description: "SASL/SCRAM-SHA-256 인증 (Kafka)"

    saslPlain:
      type: plain
      description: "SASL/PLAIN 인증"

    userPassword:
      type: userPassword
      description: "사용자명/비밀번호 인증 (MQTT)"

    apiKey:
      type: apiKey
      in: user
      description: "API 키 인증"

    x509:
      type: X509
      description: "X.509 클라이언트 인증서"

    oauth2:
      type: oauth2
      flows:
        clientCredentials:
          tokenUrl: "https://auth.example.com/token"
          scopes:
            orders:read: "주문 이벤트 읽기"
            orders:write: "주문 이벤트 쓰기"
```

| 보안 스킴 타입 | 설명 | 주요 사용 프로토콜 |
|---------------|------|-------------------|
| `userPassword` | 사용자명/비밀번호 | MQTT, AMQP |
| `apiKey` | API 키 | HTTP, WebSocket |
| `X509` | X.509 인증서 | Kafka, MQTT (TLS) |
| `symmetricEncryption` | 대칭 암호화 | 범용 |
| `asymmetricEncryption` | 비대칭 암호화 | 범용 |
| `httpApiKey` | HTTP API 키 | HTTP |
| `http` | HTTP 인증 (Basic, Bearer) | HTTP, WebSocket |
| `oauth2` | OAuth 2.0 | 범용 |
| `openIdConnect` | OpenID Connect | 범용 |
| `plain` | SASL/PLAIN | Kafka |
| `scramSha256` | SASL/SCRAM-SHA-256 | Kafka |
| `scramSha512` | SASL/SCRAM-SHA-512 | Kafka |
| `gssapi` | SASL/GSSAPI (Kerberos) | Kafka |

---

## 6. 사용 사례 및 활용 도구

### 6.1 주요 사용 사례

이벤트 기반 아키텍처(EDA) 문서화

마이크로서비스 간에 교환되는 이벤트의 구조, 채널, 프로토콜을 체계적으로 문서화한다. 각 서비스가 발행하는 이벤트와 구독하는 이벤트를 명확히 정의하여, 시스템 전체의 이벤트 흐름을 파악할 수 있게 한다.

```
주문 서비스 --[OrderCreated]--> orders.created 토픽
결제 서비스 <--[OrderCreated]-- orders.created 토픽
재고 서비스 <--[OrderCreated]-- orders.created 토픽
알림 서비스 <--[OrderCreated]-- orders.created 토픽
```

마이크로서비스 이벤트 카탈로그

조직 내 모든 이벤트를 중앙에서 관리하는 이벤트 카탈로그를 구축한다. 각 서비스별 AsyncAPI 명세를 통합하면 전사적 이벤트 맵을 만들 수 있다. 이를 통해 새로운 서비스를 개발할 때 기존에 어떤 이벤트가 사용 가능한지 쉽게 파악할 수 있다.

코드 생성

AsyncAPI 명세로부터 메시지 모델 클래스, 프로듀서/컨슈머 코드, 유효성 검증 로직을 자동 생성한다. 이는 서비스 간 메시지 형식의 일관성을 보장하고, 수작업으로 인한 오류를 줄인다.

계약 기반 개발 및 테스트

명세를 기반으로 이벤트 생산자와 소비자 간의 계약을 정의하고, 계약 테스트를 수행한다. 이벤트 스키마가 변경될 때 하위 호환성을 자동으로 검증한다.

모킹 및 시뮬레이션

실제 메시지 브로커 없이도 명세 기반의 가상 이벤트를 생성하여, 서비스 개발 및 테스트를 진행할 수 있다.

IoT 시스템 설계

MQTT 기반 IoT 장치의 메시지 프로토콜을 정의한다. 센서 데이터, 명령, 상태 보고 등의 메시지 형식을 표준화한다.

### 6.2 핵심 도구

AsyncAPI Generator

AsyncAPI 명세로부터 다양한 출력물을 생성하는 코드 생성 도구이다.

| 템플릿 | 설명 |
|--------|------|
| `@asyncapi/nodejs-template` | Node.js 애플리케이션 생성 |
| `@asyncapi/nodejs-ws-template` | Node.js WebSocket 서버 생성 |
| `@asyncapi/java-template` | Java 프로젝트 생성 |
| `@asyncapi/java-spring-template` | Spring Boot 프로젝트 생성 |
| `@asyncapi/java-spring-cloud-stream-template` | Spring Cloud Stream 프로젝트 생성 |
| `@asyncapi/python-paho-template` | Python MQTT 클라이언트 생성 |
| `@asyncapi/html-template` | HTML 문서 생성 |
| `@asyncapi/markdown-template` | Markdown 문서 생성 |

```bash
# 설치
npm install -g @asyncapi/generator

# Node.js 프로젝트 생성
ag asyncapi.yaml @asyncapi/nodejs-template -o output

# HTML 문서 생성
ag asyncapi.yaml @asyncapi/html-template -o docs

# Spring Boot 프로젝트 생성
ag asyncapi.yaml @asyncapi/java-spring-template -o output
```

AsyncAPI Studio

브라우저 기반의 AsyncAPI 명세 편집기이다. 실시간 문법 검증, 시각화, 미리보기를 제공한다. OpenAPI의 Swagger Editor에 해당하는 도구이다.

- 실시간 명세 편집 및 검증
- 채널, 오퍼레이션, 메시지의 시각적 표현
- 2.x에서 3.0으로의 자동 변환 지원
- 웹 브라우저에서 바로 사용 가능 (`studio.asyncapi.com`)

AsyncAPI Modelina

AsyncAPI(또는 JSON Schema, OpenAPI) 명세로부터 데이터 모델 클래스를 생성하는 도구이다.

| 지원 언어 | 설명 |
|-----------|------|
| TypeScript | 인터페이스 및 클래스 생성 |
| Java | POJO 클래스 생성 |
| C# | 클래스 생성 |
| Go | 구조체 생성 |
| Python | 데이터 클래스 생성 |
| Kotlin | 데이터 클래스 생성 |
| Rust | 구조체 생성 |
| Dart | 클래스 생성 |

```bash
# 설치
npm install @asyncapi/modelina

# TypeScript 모델 생성 예시 (프로그래밍 방식)
```

```javascript
const { TypeScriptGenerator } = require('@asyncapi/modelina');
const generator = new TypeScriptGenerator();
const models = await generator.generate(asyncapiDocument);
```

AsyncAPI Parser

AsyncAPI 명세를 파싱하고 검증하는 라이브러리이다. JavaScript/TypeScript로 구현되어 있으며, 다른 도구들의 기반이 된다.

```javascript
const { Parser } = require('@asyncapi/parser');
const parser = new Parser();

const { document, diagnostics } = await parser.parse(asyncapiYaml);

if (document) {
  console.log(document.info().title());
  for (const [name, channel] of document.channels()) {
    console.log(`채널: ${name}, 주소: ${channel.address()}`);
  }
}
```

AsyncAPI Glee

AsyncAPI 명세를 기반으로 이벤트 기반 애플리케이션 프레임워크를 제공한다. 명세를 작성하면 이벤트 핸들러만 구현하면 되는 서버리스 스타일의 개발 경험을 제공한다.

```javascript
// functions/onOrderCreated.js
export default async function onOrderCreated({ payload }) {
  console.log(`새 주문 수신: ${payload.orderId}`);
  // 비즈니스 로직 처리
}
```

Spectral (AsyncAPI 린팅)

Stoplight의 Spectral 도구는 OpenAPI뿐만 아니라 AsyncAPI 명세의 린팅도 지원한다. 커스텀 규칙을 정의하여 조직 차원의 명세 작성 표준을 강제할 수 있다.

```yaml
# .spectral.yaml
extends: ["spectral:asyncapi"]
rules:
  asyncapi-info-contact:
    description: "Info 객체에 contact 정보가 있어야 한다"
    severity: warn
  asyncapi-operation-description:
    description: "모든 오퍼레이션에 description이 있어야 한다"
    severity: error
```

```bash
# 린팅 실행
spectral lint asyncapi.yaml --ruleset .spectral.yaml
```

### 6.3 도구 생태계 요약

| 도구 | 범주 | 설명 |
|------|------|------|
| AsyncAPI Generator | 코드 생성 | 명세로부터 코드, 문서 등 생성 |
| AsyncAPI Studio | 편집기 | 브라우저 기반 명세 편집 및 시각화 |
| AsyncAPI Modelina | 모델 생성 | 데이터 모델 클래스 생성 |
| AsyncAPI Parser | 파서/검증 | 명세 파싱 및 검증 라이브러리 |
| AsyncAPI Glee | 프레임워크 | 이벤트 기반 앱 프레임워크 |
| AsyncAPI CLI | CLI 도구 | 명세 관리를 위한 통합 CLI |
| AsyncAPI Converter | 변환 | 버전 간 명세 변환 |
| Spectral | 린팅 | 명세 규칙 검증 |
| Microcks | 모킹/테스트 | API 모킹 및 계약 테스트 |
| AsyncAPI React | UI 컴포넌트 | React 기반 문서 렌더링 컴포넌트 |
| EventCatalog | 이벤트 카탈로그 | AsyncAPI 기반 이벤트 카탈로그 UI |

---

## 7. 장점과 단점

### 7.1 장점

이벤트 기반 아키텍처의 표준화
- 비동기 API를 위한 사실상의 유일한 표준 명세이다
- 마이크로서비스 간 이벤트 통신을 체계적으로 문서화할 수 있다
- 이벤트 카탈로그를 통해 조직 전체의 이벤트 흐름을 파악할 수 있다

프로토콜 중립적 추상화
- Kafka, MQTT, AMQP, WebSocket, NATS 등 다양한 프로토콜을 동일한 형식으로 기술한다
- 프로토콜 변경 시에도 핵심 명세 구조는 유지되며, 바인딩만 수정하면 된다
- 멀티프로토콜 환경에서 통합적인 문서화가 가능하다

OpenAPI와의 유사성
- OpenAPI에 익숙한 개발자가 빠르게 학습할 수 있다
- JSON Schema를 공유하므로 데이터 모델을 동기/비동기 API 간에 재사용할 수 있다
- 동일한 도구 생태계(Spectral 등)를 활용할 수 있다

자동화 지원
- 코드 생성, 문서 생성, 모델 생성 등 다양한 자동화가 가능하다
- CI/CD 파이프라인에 명세 검증을 통합할 수 있다
- 스키마 레지스트리와 연동하여 스키마 호환성을 자동 검증할 수 있다

계약 기반 개발
- 이벤트 생산자와 소비자 간의 명확한 계약을 수립할 수 있다
- 스키마 변경 시 하위 호환성을 자동으로 검증할 수 있다
- 이벤트 스키마의 버전 관리가 용이하다

커뮤니티와 거버넌스
- Linux Foundation 산하의 오픈 거버넌스 체계를 갖추고 있어 장기적 안정성이 보장된다
- 활발한 오픈 소스 커뮤니티가 도구 생태계를 지속적으로 발전시킨다

### 7.2 단점

생태계 성숙도
- OpenAPI 대비 도구 생태계가 아직 성숙하지 않다
- 일부 도구가 AsyncAPI 3.0을 완전히 지원하지 않는 경우가 있다
- 프레임워크별 통합이 OpenAPI만큼 풍부하지 않다

학습 곡선
- 비동기 메시징 자체에 대한 이해가 선행되어야 한다
- 프로토콜 바인딩의 세부 설정은 해당 프로토콜에 대한 깊은 지식을 요구한다
- 3.0의 구조 변경으로 기존 2.x 사용자도 재학습이 필요하다

명세 복잡도
- 여러 프로토콜, 다수의 채널, 복잡한 바인딩을 포함하면 명세가 매우 방대해진다
- 프로토콜 바인딩 명세가 별도로 관리되어, 전체 명세의 파악이 어려울 수 있다
- 트레이트, 컴포넌트, 참조가 얽히면 명세의 가독성이 떨어진다

실행 시점의 간극
- AsyncAPI는 메시지의 구조를 기술하지만, 메시지의 순서, 타이밍, 실패 처리 등 런타임 동작은 기술하지 않는다
- 이벤트의 인과 관계(causality)나 사가(saga) 패턴 같은 복잡한 워크플로를 표현하기 어렵다
- 명세만으로는 시스템의 전체적인 이벤트 흐름을 파악하기 어려울 수 있다

버전 호환성 관리
- 2.x에서 3.0으로의 마이그레이션에 상당한 노력이 필요하다
- 프로토콜 바인딩 명세의 버전이 독립적으로 관리되어, 호환성 매트릭스가 복잡하다
- 도구마다 지원하는 AsyncAPI 버전이 다를 수 있다

채택률
- OpenAPI 대비 산업 전반의 채택률이 낮다
- 많은 조직이 아직 비동기 API를 비공식적인 방법으로 문서화한다
- 상용 도구의 AsyncAPI 지원이 제한적인 경우가 있다

---

## 8. 실제 예시

아래는 Kafka 기반 전자상거래 주문 이벤트 시스템의 완전한 AsyncAPI 3.0 명세이다. 주문 생성, 주문 확인, 주문 취소, 결제 처리, 재고 변경, 배송 상태 업데이트 등의 이벤트를 포함한다.

```yaml
asyncapi: "3.0.0"

# ================================================================
# 메타데이터
# ================================================================
info:
  title: "전자상거래 주문 이벤트 시스템"
  version: "2.1.0"
  description: |
    전자상거래 플랫폼의 주문 처리 파이프라인에서 발생하는 이벤트를 정의한다.

    ## 아키텍처 개요
    이 시스템은 이벤트 기반 아키텍처(EDA)를 채택하여 마이크로서비스 간의
    느슨한 결합(loose coupling)을 달성한다. Apache Kafka를 중앙 메시지
    브로커로 사용하며, 각 서비스는 관심 있는 이벤트를 구독하여 처리한다.

    ## 서비스 구성
    - 주문 서비스: 주문 생성, 확인, 취소 이벤트 발행
    - 결제 서비스: 결제 처리 결과 이벤트 발행
    - 재고 서비스: 재고 변경 이벤트 발행
    - 배송 서비스: 배송 상태 변경 이벤트 발행
    - 알림 서비스: 모든 이벤트를 구독하여 고객에게 알림 전송
  contact:
    name: "이벤트 플랫폼 팀"
    email: "event-platform@example.com"
    url: "https://internal.example.com/event-platform"
  license:
    name: "Apache 2.0"
    url: "https://www.apache.org/licenses/LICENSE-2.0.html"
  tags:
    - name: "order"
      description: "주문 관련 이벤트"
    - name: "payment"
      description: "결제 관련 이벤트"
    - name: "inventory"
      description: "재고 관련 이벤트"
    - name: "shipping"
      description: "배송 관련 이벤트"
  externalDocs:
    description: "이벤트 아키텍처 상세 문서"
    url: "https://docs.example.com/event-architecture"

defaultContentType: "application/json"

# ================================================================
# 서버 (Kafka 클러스터)
# ================================================================
servers:
  productionCluster:
    host: "kafka-prod-01.example.com:9092,kafka-prod-02.example.com:9092,kafka-prod-03.example.com:9092"
    protocol: kafka-secure
    protocolVersion: "3.5.0"
    description: "운영 Kafka 클러스터 (3-노드)"
    security:
      - $ref: '#/components/securitySchemes/saslScram'
    tags:
      - name: "production"
    bindings:
      kafka:
        schemaRegistryUrl: "https://schema-registry-prod.example.com"
        schemaRegistryVendor: "confluent"

  stagingCluster:
    host: "kafka-staging.example.com:9092"
    protocol: kafka
    description: "스테이징 Kafka 클러스터"
    tags:
      - name: "staging"
    bindings:
      kafka:
        schemaRegistryUrl: "https://schema-registry-staging.example.com"
        schemaRegistryVendor: "confluent"

# ================================================================
# 채널 (Kafka 토픽)
# ================================================================
channels:
  # --- 주문 이벤트 ---
  orderCreated:
    address: "ecommerce.orders.created"
    description: |
      새로운 주문이 생성되었을 때 발행되는 이벤트.
      주문 서비스에서 발행하며, 결제 서비스, 재고 서비스, 알림 서비스가 구독한다.
    messages:
      OrderCreatedEvent:
        $ref: '#/components/messages/OrderCreated'
    servers:
      - $ref: '#/servers/productionCluster'
      - $ref: '#/servers/stagingCluster'
    bindings:
      kafka:
        topic: "ecommerce.orders.created"
        partitions: 12
        replicas: 3
        topicConfiguration:
          cleanup.policy: ["delete"]
          retention.ms: 604800000
          min.insync.replicas: 2

  orderConfirmed:
    address: "ecommerce.orders.confirmed"
    description: |
      주문이 확인(결제 완료 + 재고 확보)되었을 때 발행되는 이벤트.
      주문 서비스에서 발행하며, 배송 서비스, 알림 서비스가 구독한다.
    messages:
      OrderConfirmedEvent:
        $ref: '#/components/messages/OrderConfirmed'
    bindings:
      kafka:
        topic: "ecommerce.orders.confirmed"
        partitions: 6
        replicas: 3

  orderCancelled:
    address: "ecommerce.orders.cancelled"
    description: |
      주문이 취소되었을 때 발행되는 이벤트.
      결제 환불, 재고 복원, 배송 취소 등의 보상 트랜잭션을 트리거한다.
    messages:
      OrderCancelledEvent:
        $ref: '#/components/messages/OrderCancelled'
    bindings:
      kafka:
        topic: "ecommerce.orders.cancelled"
        partitions: 6
        replicas: 3

  # --- 결제 이벤트 ---
  paymentProcessed:
    address: "ecommerce.payments.processed"
    description: |
      결제 처리가 완료되었을 때 발행되는 이벤트.
      결제 성공 또는 실패 결과를 포함한다.
    messages:
      PaymentProcessedEvent:
        $ref: '#/components/messages/PaymentProcessed'
    bindings:
      kafka:
        topic: "ecommerce.payments.processed"
        partitions: 6
        replicas: 3
        topicConfiguration:
          retention.ms: 2592000000

  # --- 재고 이벤트 ---
  inventoryChanged:
    address: "ecommerce.inventory.changed"
    description: |
      재고 수량이 변경되었을 때 발행되는 이벤트.
      주문으로 인한 차감, 입고로 인한 증가, 취소로 인한 복원 등을 포함한다.
    messages:
      InventoryChangedEvent:
        $ref: '#/components/messages/InventoryChanged'
    bindings:
      kafka:
        topic: "ecommerce.inventory.changed"
        partitions: 12
        replicas: 3

  # --- 배송 이벤트 ---
  shippingStatusUpdated:
    address: "ecommerce.shipping.status-updated"
    description: |
      배송 상태가 변경되었을 때 발행되는 이벤트.
      배송 준비, 출고, 배송 중, 배송 완료 등의 상태 변경을 포함한다.
    messages:
      ShippingStatusUpdatedEvent:
        $ref: '#/components/messages/ShippingStatusUpdated'
    bindings:
      kafka:
        topic: "ecommerce.shipping.status-updated"
        partitions: 6
        replicas: 3

  # --- 데드레터 큐 ---
  orderDeadLetter:
    address: "ecommerce.orders.dead-letter"
    description: |
      처리에 실패한 주문 관련 이벤트가 전송되는 데드레터 토픽.
      수동 조사 및 재처리를 위해 사용된다.
    messages:
      DeadLetterEvent:
        $ref: '#/components/messages/DeadLetterMessage'
    bindings:
      kafka:
        topic: "ecommerce.orders.dead-letter"
        partitions: 3
        replicas: 3
        topicConfiguration:
          retention.ms: 2592000000

# ================================================================
# 오퍼레이션
# ================================================================
operations:
  # --- 주문 서비스 (발행) ---
  sendOrderCreated:
    action: send
    channel:
      $ref: '#/channels/orderCreated'
    summary: "주문 생성 이벤트 발행"
    description: |
      고객이 새로운 주문을 제출하면 이 이벤트를 발행한다.
      메시지 키는 orderId를 사용하여 동일 주문의 이벤트가
      동일 파티션에 저장되도록 보장한다.
    tags:
      - name: "order"
    messages:
      - $ref: '#/channels/orderCreated/messages/OrderCreatedEvent'
    bindings:
      kafka:
        clientId:
          type: string
          enum: ["order-service"]

  sendOrderConfirmed:
    action: send
    channel:
      $ref: '#/channels/orderConfirmed'
    summary: "주문 확인 이벤트 발행"
    description: "결제와 재고 확보가 모두 완료되면 주문 확인 이벤트를 발행한다."
    tags:
      - name: "order"
    messages:
      - $ref: '#/channels/orderConfirmed/messages/OrderConfirmedEvent'

  sendOrderCancelled:
    action: send
    channel:
      $ref: '#/channels/orderCancelled'
    summary: "주문 취소 이벤트 발행"
    description: "고객 요청 또는 시스템 규칙에 의해 주문이 취소되면 이 이벤트를 발행한다."
    tags:
      - name: "order"
    messages:
      - $ref: '#/channels/orderCancelled/messages/OrderCancelledEvent'

  # --- 결제 서비스 ---
  receiveOrderCreatedForPayment:
    action: receive
    channel:
      $ref: '#/channels/orderCreated'
    summary: "주문 생성 이벤트 수신 (결제 처리)"
    description: "주문 생성 이벤트를 수신하여 결제를 시도한다."
    tags:
      - name: "payment"
    messages:
      - $ref: '#/channels/orderCreated/messages/OrderCreatedEvent'
    bindings:
      kafka:
        groupId:
          type: string
          enum: ["payment-service-group"]
        clientId:
          type: string
          enum: ["payment-service"]

  sendPaymentProcessed:
    action: send
    channel:
      $ref: '#/channels/paymentProcessed'
    summary: "결제 처리 결과 이벤트 발행"
    description: "결제 처리(성공/실패) 결과를 이벤트로 발행한다."
    tags:
      - name: "payment"
    messages:
      - $ref: '#/channels/paymentProcessed/messages/PaymentProcessedEvent'

  # --- 재고 서비스 ---
  receiveOrderCreatedForInventory:
    action: receive
    channel:
      $ref: '#/channels/orderCreated'
    summary: "주문 생성 이벤트 수신 (재고 차감)"
    description: "주문 생성 이벤트를 수신하여 재고를 차감한다."
    tags:
      - name: "inventory"
    messages:
      - $ref: '#/channels/orderCreated/messages/OrderCreatedEvent'
    bindings:
      kafka:
        groupId:
          type: string
          enum: ["inventory-service-group"]

  receiveOrderCancelledForInventory:
    action: receive
    channel:
      $ref: '#/channels/orderCancelled'
    summary: "주문 취소 이벤트 수신 (재고 복원)"
    description: "주문 취소 이벤트를 수신하여 재고를 복원한다."
    tags:
      - name: "inventory"
    messages:
      - $ref: '#/channels/orderCancelled/messages/OrderCancelledEvent'
    bindings:
      kafka:
        groupId:
          type: string
          enum: ["inventory-service-group"]

  sendInventoryChanged:
    action: send
    channel:
      $ref: '#/channels/inventoryChanged'
    summary: "재고 변경 이벤트 발행"
    description: "재고 수량이 변경될 때 이벤트를 발행한다."
    tags:
      - name: "inventory"
    messages:
      - $ref: '#/channels/inventoryChanged/messages/InventoryChangedEvent'

  # --- 배송 서비스 ---
  receiveOrderConfirmedForShipping:
    action: receive
    channel:
      $ref: '#/channels/orderConfirmed'
    summary: "주문 확인 이벤트 수신 (배송 준비)"
    description: "주문 확인 이벤트를 수신하여 배송을 준비한다."
    tags:
      - name: "shipping"
    messages:
      - $ref: '#/channels/orderConfirmed/messages/OrderConfirmedEvent'
    bindings:
      kafka:
        groupId:
          type: string
          enum: ["shipping-service-group"]

  sendShippingStatusUpdated:
    action: send
    channel:
      $ref: '#/channels/shippingStatusUpdated'
    summary: "배송 상태 변경 이벤트 발행"
    description: "배송 상태가 변경될 때 이벤트를 발행한다."
    tags:
      - name: "shipping"
    messages:
      - $ref: '#/channels/shippingStatusUpdated/messages/ShippingStatusUpdatedEvent'

# ================================================================
# 재사용 컴포넌트
# ================================================================
components:

  # ----------------------------------------------------------
  # 메시지 정의
  # ----------------------------------------------------------
  messages:
    OrderCreated:
      name: "OrderCreated"
      title: "주문 생성 이벤트"
      summary: "새로운 주문이 생성되었음을 알리는 이벤트"
      contentType: "application/json"
      correlationId:
        location: "$message.header#/correlationId"
      headers:
        $ref: '#/components/schemas/EventHeaders'
      payload:
        $ref: '#/components/schemas/OrderCreatedPayload'
      traits:
        - $ref: '#/components/messageTraits/kafkaMessage'
      bindings:
        kafka:
          key:
            type: string
            description: "파티션 키 (orderId)"

    OrderConfirmed:
      name: "OrderConfirmed"
      title: "주문 확인 이벤트"
      summary: "주문이 확인(결제 완료 + 재고 확보)되었음을 알리는 이벤트"
      contentType: "application/json"
      correlationId:
        location: "$message.header#/correlationId"
      headers:
        $ref: '#/components/schemas/EventHeaders'
      payload:
        $ref: '#/components/schemas/OrderConfirmedPayload'
      traits:
        - $ref: '#/components/messageTraits/kafkaMessage'
      bindings:
        kafka:
          key:
            type: string
            description: "파티션 키 (orderId)"

    OrderCancelled:
      name: "OrderCancelled"
      title: "주문 취소 이벤트"
      summary: "주문이 취소되었음을 알리는 이벤트"
      contentType: "application/json"
      correlationId:
        location: "$message.header#/correlationId"
      headers:
        $ref: '#/components/schemas/EventHeaders'
      payload:
        $ref: '#/components/schemas/OrderCancelledPayload'
      traits:
        - $ref: '#/components/messageTraits/kafkaMessage'
      bindings:
        kafka:
          key:
            type: string
            description: "파티션 키 (orderId)"

    PaymentProcessed:
      name: "PaymentProcessed"
      title: "결제 처리 완료 이벤트"
      summary: "결제 처리 결과(성공/실패)를 알리는 이벤트"
      contentType: "application/json"
      correlationId:
        location: "$message.header#/correlationId"
      headers:
        $ref: '#/components/schemas/EventHeaders'
      payload:
        $ref: '#/components/schemas/PaymentProcessedPayload'
      traits:
        - $ref: '#/components/messageTraits/kafkaMessage'
      bindings:
        kafka:
          key:
            type: string
            description: "파티션 키 (orderId)"

    InventoryChanged:
      name: "InventoryChanged"
      title: "재고 변경 이벤트"
      summary: "재고 수량이 변경되었음을 알리는 이벤트"
      contentType: "application/json"
      correlationId:
        location: "$message.header#/correlationId"
      headers:
        $ref: '#/components/schemas/EventHeaders'
      payload:
        $ref: '#/components/schemas/InventoryChangedPayload'
      traits:
        - $ref: '#/components/messageTraits/kafkaMessage'
      bindings:
        kafka:
          key:
            type: string
            description: "파티션 키 (productId)"

    ShippingStatusUpdated:
      name: "ShippingStatusUpdated"
      title: "배송 상태 변경 이벤트"
      summary: "배송 상태가 변경되었음을 알리는 이벤트"
      contentType: "application/json"
      correlationId:
        location: "$message.header#/correlationId"
      headers:
        $ref: '#/components/schemas/EventHeaders'
      payload:
        $ref: '#/components/schemas/ShippingStatusUpdatedPayload'
      traits:
        - $ref: '#/components/messageTraits/kafkaMessage'
      bindings:
        kafka:
          key:
            type: string
            description: "파티션 키 (orderId)"

    DeadLetterMessage:
      name: "DeadLetterMessage"
      title: "데드레터 메시지"
      summary: "처리에 실패한 메시지"
      contentType: "application/json"
      payload:
        $ref: '#/components/schemas/DeadLetterPayload'

  # ----------------------------------------------------------
  # 스키마 정의
  # ----------------------------------------------------------
  schemas:
    # --- 공통 헤더 ---
    EventHeaders:
      type: object
      required:
        - correlationId
        - eventType
        - eventTime
        - source
        - version
      properties:
        correlationId:
          type: string
          format: uuid
          description: "이벤트 추적을 위한 상관관계 ID"
        eventType:
          type: string
          description: "이벤트 타입명"
        eventTime:
          type: string
          format: date-time
          description: "이벤트 발생 시각 (ISO 8601)"
        source:
          type: string
          description: "이벤트 발행 서비스명"
        version:
          type: string
          description: "이벤트 스키마 버전"
          default: "1.0"
        traceId:
          type: string
          description: "분산 추적 ID"
        spanId:
          type: string
          description: "분산 추적 스팬 ID"

    # --- 주문 생성 ---
    OrderCreatedPayload:
      type: object
      required:
        - orderId
        - customerId
        - items
        - totalAmount
        - currency
        - shippingAddress
        - createdAt
      properties:
        orderId:
          type: string
          format: uuid
          description: "주문 고유 식별자"
        customerId:
          type: string
          format: uuid
          description: "고객 식별자"
        items:
          type: array
          minItems: 1
          description: "주문 상품 목록"
          items:
            $ref: '#/components/schemas/OrderItem'
        totalAmount:
          type: number
          format: double
          minimum: 0
          description: "총 주문 금액 (세금 포함)"
        subtotalAmount:
          type: number
          format: double
          minimum: 0
          description: "소계 (세금 미포함)"
        taxAmount:
          type: number
          format: double
          minimum: 0
          description: "세금"
        shippingFee:
          type: number
          format: double
          minimum: 0
          description: "배송비"
        currency:
          type: string
          enum: [KRW, USD, EUR, JPY]
          description: "통화 코드 (ISO 4217)"
        paymentMethod:
          type: string
          enum: [CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, VIRTUAL_ACCOUNT, MOBILE_PAY]
          description: "결제 수단"
        shippingAddress:
          $ref: '#/components/schemas/Address'
        couponCode:
          type: string
          description: "적용된 쿠폰 코드"
        discountAmount:
          type: number
          format: double
          minimum: 0
          description: "할인 금액"
        customerNote:
          type: string
          maxLength: 500
          description: "고객 요청사항"
        createdAt:
          type: string
          format: date-time
          description: "주문 생성 시각"

    # --- 주문 확인 ---
    OrderConfirmedPayload:
      type: object
      required:
        - orderId
        - customerId
        - paymentId
        - confirmedAt
      properties:
        orderId:
          type: string
          format: uuid
          description: "주문 식별자"
        customerId:
          type: string
          format: uuid
          description: "고객 식별자"
        paymentId:
          type: string
          format: uuid
          description: "결제 식별자"
        estimatedDeliveryDate:
          type: string
          format: date
          description: "예상 배송일"
        confirmedAt:
          type: string
          format: date-time
          description: "주문 확인 시각"

    # --- 주문 취소 ---
    OrderCancelledPayload:
      type: object
      required:
        - orderId
        - customerId
        - reason
        - cancelledAt
      properties:
        orderId:
          type: string
          format: uuid
          description: "주문 식별자"
        customerId:
          type: string
          format: uuid
          description: "고객 식별자"
        reason:
          type: string
          enum:
            - CUSTOMER_REQUEST
            - PAYMENT_FAILED
            - OUT_OF_STOCK
            - FRAUD_DETECTED
            - SYSTEM_ERROR
          description: "취소 사유"
        reasonDetail:
          type: string
          maxLength: 1000
          description: "취소 사유 상세"
        refundAmount:
          type: number
          format: double
          minimum: 0
          description: "환불 금액"
        items:
          type: array
          items:
            $ref: '#/components/schemas/CancelledItem'
          description: "취소된 상품 목록"
        cancelledAt:
          type: string
          format: date-time
          description: "주문 취소 시각"

    # --- 결제 처리 ---
    PaymentProcessedPayload:
      type: object
      required:
        - paymentId
        - orderId
        - status
        - processedAt
      properties:
        paymentId:
          type: string
          format: uuid
          description: "결제 고유 식별자"
        orderId:
          type: string
          format: uuid
          description: "연관된 주문 식별자"
        status:
          type: string
          enum: [SUCCESS, FAILED, PENDING]
          description: "결제 처리 결과"
        amount:
          type: number
          format: double
          minimum: 0
          description: "결제 금액"
        currency:
          type: string
          enum: [KRW, USD, EUR, JPY]
        paymentMethod:
          type: string
          enum: [CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, VIRTUAL_ACCOUNT, MOBILE_PAY]
        transactionId:
          type: string
          description: "PG사 거래 ID"
        failureReason:
          type: string
          description: "결제 실패 사유 (실패 시)"
        processedAt:
          type: string
          format: date-time
          description: "결제 처리 시각"

    # --- 재고 변경 ---
    InventoryChangedPayload:
      type: object
      required:
        - productId
        - warehouseId
        - changeType
        - quantityChange
        - currentQuantity
        - changedAt
      properties:
        productId:
          type: string
          format: uuid
          description: "상품 식별자"
        productName:
          type: string
          description: "상품명"
        sku:
          type: string
          description: "SKU 코드"
        warehouseId:
          type: string
          description: "창고 식별자"
        changeType:
          type: string
          enum:
            - ORDER_RESERVED
            - ORDER_CANCELLED_RESTORE
            - INBOUND
            - ADJUSTMENT
            - DAMAGED
            - RETURNED
          description: "재고 변경 유형"
        quantityChange:
          type: integer
          description: "변경 수량 (양수: 증가, 음수: 감소)"
        previousQuantity:
          type: integer
          minimum: 0
          description: "변경 전 수량"
        currentQuantity:
          type: integer
          minimum: 0
          description: "변경 후 현재 수량"
        relatedOrderId:
          type: string
          format: uuid
          description: "연관된 주문 ID (주문 관련 변경 시)"
        changedAt:
          type: string
          format: date-time
          description: "재고 변경 시각"

    # --- 배송 상태 변경 ---
    ShippingStatusUpdatedPayload:
      type: object
      required:
        - shipmentId
        - orderId
        - status
        - updatedAt
      properties:
        shipmentId:
          type: string
          format: uuid
          description: "배송 식별자"
        orderId:
          type: string
          format: uuid
          description: "연관된 주문 식별자"
        status:
          type: string
          enum:
            - PREPARING
            - PICKED_UP
            - IN_TRANSIT
            - OUT_FOR_DELIVERY
            - DELIVERED
            - FAILED_DELIVERY
            - RETURNED
          description: "배송 상태"
        carrier:
          type: string
          description: "배송 업체명"
        trackingNumber:
          type: string
          description: "운송장 번호"
        estimatedDeliveryDate:
          type: string
          format: date
          description: "예상 배송일"
        actualDeliveryDate:
          type: string
          format: date-time
          description: "실제 배송 완료 시각"
        currentLocation:
          type: string
          description: "현재 위치"
        recipientName:
          type: string
          description: "수령인"
        updatedAt:
          type: string
          format: date-time
          description: "상태 변경 시각"

    # --- 데드레터 ---
    DeadLetterPayload:
      type: object
      required:
        - originalTopic
        - originalMessage
        - errorMessage
        - failedAt
        - retryCount
      properties:
        originalTopic:
          type: string
          description: "원래 토픽명"
        originalMessage:
          type: object
          description: "원래 메시지"
        errorMessage:
          type: string
          description: "에러 메시지"
        errorStackTrace:
          type: string
          description: "에러 스택 트레이스"
        failedAt:
          type: string
          format: date-time
          description: "실패 시각"
        retryCount:
          type: integer
          minimum: 0
          description: "재시도 횟수"
        consumerGroup:
          type: string
          description: "실패한 컨슈머 그룹"

    # --- 공통 하위 스키마 ---
    OrderItem:
      type: object
      required:
        - productId
        - productName
        - quantity
        - unitPrice
      properties:
        productId:
          type: string
          format: uuid
          description: "상품 식별자"
        productName:
          type: string
          description: "상품명"
        sku:
          type: string
          description: "SKU 코드"
        quantity:
          type: integer
          minimum: 1
          description: "주문 수량"
        unitPrice:
          type: number
          format: double
          minimum: 0
          description: "단가"
        totalPrice:
          type: number
          format: double
          minimum: 0
          description: "소계 (단가 x 수량)"

    CancelledItem:
      type: object
      required:
        - productId
        - quantity
      properties:
        productId:
          type: string
          format: uuid
          description: "상품 식별자"
        quantity:
          type: integer
          minimum: 1
          description: "취소 수량"
        refundAmount:
          type: number
          format: double
          minimum: 0
          description: "해당 상품 환불 금액"

    Address:
      type: object
      required:
        - zipCode
        - address1
        - city
        - country
      properties:
        zipCode:
          type: string
          pattern: "^\\d{5}$"
          description: "우편번호"
        address1:
          type: string
          description: "기본 주소"
        address2:
          type: string
          description: "상세 주소"
        city:
          type: string
          description: "시/군/구"
        state:
          type: string
          description: "도/시"
        country:
          type: string
          description: "국가 코드 (ISO 3166-1 alpha-2)"
          default: "KR"
        recipientName:
          type: string
          description: "수령인 이름"
        recipientPhone:
          type: string
          pattern: "^01[0-9]-\\d{3,4}-\\d{4}$"
          description: "수령인 전화번호"

  # ----------------------------------------------------------
  # 메시지 트레이트
  # ----------------------------------------------------------
  messageTraits:
    kafkaMessage:
      bindings:
        kafka:
          schemaIdLocation: "header"
          schemaIdPayloadEncoding: "confluent"

  # ----------------------------------------------------------
  # 오퍼레이션 트레이트
  # ----------------------------------------------------------
  operationTraits:
    kafkaConsumer:
      bindings:
        kafka:
          groupId:
            type: string
          clientId:
            type: string

  # ----------------------------------------------------------
  # 보안 스킴
  # ----------------------------------------------------------
  securitySchemes:
    saslScram:
      type: scramSha512
      description: "SASL/SCRAM-SHA-512 인증"

    saslPlain:
      type: plain
      description: "SASL/PLAIN 인증"

    mtls:
      type: X509
      description: "mTLS (상호 TLS) 인증서 기반 인증"
```

위 예시의 주요 특징을 정리하면 다음과 같다.

| 항목 | 설명 |
|------|------|
| 채널 수 | 7개 (주문 생성/확인/취소, 결제, 재고, 배송, 데드레터) |
| 메시지 수 | 7개 (각 채널당 1개) |
| 오퍼레이션 수 | 10개 (발행 5개, 구독 5개) |
| 프로토콜 | Kafka (kafka-secure) |
| 보안 | SASL/SCRAM-SHA-512 |
| 스키마 레지스트리 | Confluent Schema Registry 연동 |
| 파티셔닝 전략 | orderId/productId 기반 파티션 키 |
| 데드레터 큐 | 처리 실패 메시지 격리 |
| 분산 추적 | correlationId, traceId, spanId 지원 |

---

## 9. 관련 생태계 및 커뮤니티

### 9.1 OpenAPI와의 관계

AsyncAPI는 OpenAPI의 자매 명세(sister specification) 로 불린다. AsyncAPI의 설계 철학, 구조, 용어는 의도적으로 OpenAPI와 유사하게 설계되었으며, 이는 다음과 같은 이점을 제공한다.

- 학습 곡선 최소화: OpenAPI에 익숙한 개발자가 빠르게 AsyncAPI를 습득할 수 있다
- 스키마 공유: JSON Schema 기반의 데이터 모델을 동기/비동기 API 간에 재사용할 수 있다
- 도구 통합: Spectral, Microcks 등의 도구가 OpenAPI와 AsyncAPI를 모두 지원한다
- 통합 API 카탈로그: 동기(REST) API와 비동기(이벤트) API를 하나의 카탈로그에서 관리할 수 있다

두 명세의 주요 차이는 다음과 같다.

| 관점 | OpenAPI | AsyncAPI |
|------|---------|----------|
| 대상 | RESTful HTTP API | 이벤트 기반 비동기 API |
| 통신 패턴 | 요청-응답 (동기) | 발행-구독, 이벤트 스트림 (비동기) |
| 핵심 개념 | Paths, Operations, Responses | Channels, Operations, Messages |
| 프로토콜 | HTTP/HTTPS | Kafka, MQTT, AMQP, WebSocket, NATS 등 |
| 방향성 | 단방향 (클라이언트 -> 서버 -> 클라이언트) | 다방향 (생산자 -> 브로커 -> 소비자) |
| 거버넌스 | OpenAPI Initiative (Linux Foundation) | AsyncAPI Initiative (Linux Foundation) |

### 9.2 CloudEvents와의 관계

CloudEvents는 CNCF(Cloud Native Computing Foundation) 산하의 이벤트 데이터 형식 표준이다. AsyncAPI와 CloudEvents는 상호 보완적인 관계에 있다.

| 관점 | AsyncAPI | CloudEvents |
|------|----------|-------------|
| 범위 | API 명세 (채널, 오퍼레이션, 메시지 구조) | 이벤트 데이터 형식 (메타데이터 표준) |
| 초점 | "어떤 채널에서 어떤 메시지를 교환하는가" | "이벤트의 메타데이터를 어떻게 표현하는가" |
| 관계 | AsyncAPI 메시지의 페이로드가 CloudEvents 형식을 따를 수 있다 | - |

CloudEvents 형식의 메시지를 AsyncAPI로 기술하는 예시:

```yaml
components:
  schemas:
    CloudEventEnvelope:
      type: object
      required:
        - specversion
        - type
        - source
        - id
      properties:
        specversion:
          type: string
          const: "1.0"
        type:
          type: string
        source:
          type: string
          format: uri
        id:
          type: string
          format: uuid
        time:
          type: string
          format: date-time
        datacontenttype:
          type: string
        data:
          type: object
```

### 9.3 CNCF와의 관계

AsyncAPI는 직접적으로 CNCF 프로젝트는 아니지만, CNCF 생태계와 밀접한 관련이 있다.

- CloudEvents: CNCF 졸업 프로젝트로, AsyncAPI 메시지 형식으로 사용될 수 있다
- Kubernetes: 클라우드 네이티브 환경에서 이벤트 기반 마이크로서비스의 배포에 사용된다
- NATS: CNCF 인큐베이팅 프로젝트로, AsyncAPI가 지원하는 프로토콜 중 하나이다
- gRPC: CNCF 인큐베이팅 프로젝트로, 스트리밍 RPC 패턴을 AsyncAPI로 기술할 수 있다
- Dapr: CNCF 인큐베이팅 프로젝트로, Pub/Sub 빌딩 블록의 메시지를 AsyncAPI로 문서화할 수 있다

### 9.4 AsyncAPI Initiative

역할:
- Linux Foundation 산하의 오픈 거버넌스 조직으로, AsyncAPI Specification의 개발과 발전을 주도한다
- 명세, 도구, 커뮤니티의 로드맵을 관리한다

거버넌스 구조:

| 조직 | 역할 |
|------|------|
| Technical Steering Committee (TSC) | 명세와 도구의 기술적 방향 결정 |
| Code of Conduct Committee | 커뮤니티 행동 강령 관리 |
| Ambassadors | 커뮤니티 전파 및 교육 |
| Maintainers | 개별 프로젝트(파서, 생성기 등) 유지보수 |

주요 후원사 및 참여 기업:
Postman, Solace, SAP, Salesforce, Slack, eBay, PayPal, LEGO 등이 AsyncAPI Initiative에 참여하고 있다. 이들 기업은 자사의 이벤트 기반 시스템에 AsyncAPI를 채택하거나, 도구 개발에 기여하고 있다.

### 9.5 커뮤니티 채널

| 채널 | URL / 설명 |
|------|------------|
| GitHub | [github.com/asyncapi/spec](https://github.com/asyncapi/spec) - 명세 원본 및 이슈 트래킹 |
| 공식 웹사이트 | [asyncapi.com](https://www.asyncapi.com/) - 공식 문서, 도구, 블로그 |
| Slack | AsyncAPI 커뮤니티 Slack 워크스페이스 |
| YouTube | AsyncAPI 공식 YouTube 채널 - 미팅 녹화, 튜토리얼 |
| X (Twitter) | [@AsyncAPISpec](https://twitter.com/AsyncAPISpec) - 공식 계정 |
| LinkedIn | AsyncAPI 공식 LinkedIn 페이지 |
| Stack Overflow | `asyncapi` 태그 |

### 9.6 커뮤니티 활동

- AsyncAPI Conference: 연례 커뮤니티 컨퍼런스로, 온라인으로 진행되며 명세의 발전 방향, 사용 사례, 도구 소개 등을 다룬다
- Open Governance Meetings: 매주 진행되는 공개 미팅으로, 명세 변경 사항과 도구 개발 진행 상황을 논의한다
- Mentorship Program: Google Summer of Code(GSoC) 등의 멘토십 프로그램에 참여하여 신규 기여자를 육성한다
- AsyncAPI Bounty Program: 특정 기능 개발이나 버그 수정에 대해 보상을 제공하는 프로그램이다

### 9.7 AsyncAPI의 미래

- 멀티프로토콜 지원 강화: 신규 메시징 프로토콜(Solace, Amazon EventBridge 등)에 대한 바인딩이 지속적으로 추가되고 있다
- 도구 생태계 확장: IDE 플러그인, CI/CD 통합, 모니터링 도구 등의 생태계가 확대되고 있다
- AI/LLM 통합: AsyncAPI 명세를 기반으로 AI가 이벤트 기반 시스템을 이해하고 코드를 생성하는 패턴이 부상하고 있다
- OpenAPI와의 수렴: 동기/비동기 API를 통합적으로 기술할 수 있는 방향에 대한 논의가 진행 중이다
- 스키마 레지스트리 통합: Confluent Schema Registry, AWS Glue Schema Registry 등과의 네이티브 통합이 강화되고 있다
- 이벤트 카탈로그 표준화: 조직 전체의 이벤트를 관리하는 카탈로그 도구와의 통합이 발전하고 있다

---

## 참고 자료

- [AsyncAPI Specification 공식 문서](https://www.asyncapi.com/docs/reference/specification/v3.0.0)
- [AsyncAPI GitHub Organization](https://github.com/asyncapi)
- [AsyncAPI Spec Repository](https://github.com/asyncapi/spec)
- [AsyncAPI Blog](https://www.asyncapi.com/blog)
- [AsyncAPI Studio](https://studio.asyncapi.com/)
- [AsyncAPI Tools](https://www.asyncapi.com/tools)
- [CloudEvents Specification](https://cloudevents.io/)
- [OpenAPI Specification](https://spec.openapis.org/oas/latest.html)

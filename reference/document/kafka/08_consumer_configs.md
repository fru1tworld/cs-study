# Kafka 컨슈머 설정

> 이 문서는 Apache Kafka 공식 문서의 "Consumer Configs" 섹션을 한국어로 번역한 것입니다.
> 원본: https://kafka.apache.org/documentation/#consumerconfigs

## 개요

이 문서는 Apache Kafka Consumer의 설정 옵션을 설명합니다. 각 설정은 중요도(Importance)에 따라 High, Medium, Low로 분류되어 있습니다.

---

## High 중요도 설정

### bootstrap.servers

Kafka 클러스터에 대한 초기 연결을 설정하는 데 사용되는 호스트/포트 쌍 목록입니다. 클라이언트는 이 목록에 지정된 서버 중 하나를 사용하여 전체 클러스터 멤버십을 검색합니다. 이 목록은 모든 서버를 포함할 필요가 없으며, 클라이언트는 초기 연결 후 전체 브로커 세트를 자동으로 검색합니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | "" |
| 유효값 | 비어 있지 않은 문자열 |
| 중요도 | high |

---

### key.deserializer

`org.apache.kafka.common.serialization.Deserializer` 인터페이스를 구현하는 키 역직렬화 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | (없음) |
| 유효값 | (any) |
| 중요도 | high |

---

### value.deserializer

`org.apache.kafka.common.serialization.Deserializer` 인터페이스를 구현하는 값 역직렬화 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | (없음) |
| 유효값 | (any) |
| 중요도 | high |

---

### fetch.min.bytes

서버가 fetch 요청에 대해 반환해야 하는 최소 데이터 양입니다. 데이터가 충분하지 않으면 요청은 충분한 데이터가 누적될 때까지 대기합니다. 기본값 1바이트는 데이터가 도착하는 즉시 또는 fetch 요청이 대기 중 타임아웃되면 fetch 요청에 응답함을 의미합니다. 이 값을 1보다 크게 설정하면 서버가 더 많은 데이터를 누적할 때까지 대기하게 되어 대기 시간이 약간 증가하는 대신 서버 처리량이 향상됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 1 |
| 유효값 | [0,...] |
| 중요도 | high |

---

### group.id

이 컨슈머가 속한 컨슈머 그룹을 식별하는 고유 문자열입니다. 컨슈머가 그룹 관리(`subscribe(topic)` 사용) 또는 Kafka 기반 오프셋 관리(`commitSync` 또는 `commitAsync` 사용)를 사용하는 경우 이 속성이 필요합니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | high |

---

### group.protocol

그룹 관리에 사용할 프로토콜입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | classic |
| 유효값 | [CONSUMER, CLASSIC] |
| 중요도 | high |

---

### heartbeat.interval.ms

그룹 관리 기능을 사용할 때 그룹 코디네이터에 대한 하트비트 간 예상 시간입니다. 하트비트는 컨슈머의 세션이 활성 상태임을 보장하고 새 멤버가 그룹에 참여하거나 탈퇴할 때 리밸런싱을 용이하게 합니다. 이 값은 `session.timeout.ms`보다 낮게 설정해야 하며, 일반적으로 해당 값의 1/3 이하로 설정해야 합니다. 예상 리밸런싱 시간을 조절하기 위해 더 낮게 설정할 수 있습니다. 참고: classic 그룹 프로토콜에만 적용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 3000 (3초) |
| 유효값 | (any) |
| 중요도 | high |

---

### max.partition.fetch.bytes

서버가 파티션당 반환하는 최대 데이터 양입니다. 레코드는 컨슈머에 의해 배치로 가져옵니다. fetch의 첫 번째 비어 있지 않은 파티션의 첫 번째 레코드 배치가 이 제한보다 크면, 컨슈머가 진행할 수 있도록 배치가 여전히 반환됩니다. 브로커가 허용하는 최대 레코드 배치 크기는 `message.max.bytes`(브로커 설정) 또는 `max.message.bytes`(토픽 설정)를 통해 정의됩니다. 컨슈머 요청 크기를 제한하려면 `fetch.max.bytes`를 참조하세요.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 1048576 (1MB) |
| 유효값 | [0,...] |
| 중요도 | high |

---

### session.timeout.ms

그룹 관리 기능을 사용할 때 클라이언트 장애를 감지하는 데 사용되는 타임아웃입니다. 클라이언트는 브로커에 주기적으로 하트비트를 전송하여 활성 상태임을 알립니다. 이 세션 타임아웃이 만료되기 전에 브로커가 하트비트를 받지 못하면, 브로커는 이 클라이언트를 그룹에서 제거하고 리밸런싱을 시작합니다. 이 값은 브로커 설정 `group.min.session.timeout.ms`와 `group.max.session.timeout.ms` 범위 내에 있어야 합니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 45000 (45초) |
| 유효값 | (any) |
| 중요도 | high |

---

### ssl.key.password

keystore 파일 내 개인 키의 비밀번호 또는 `ssl.keystore.key`에 지정된 PEM 키의 비밀번호입니다.

| 속성 | 값 |
|------|-----|
| 타입 | password |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | high |

---

### ssl.keystore.certificate.chain

`ssl.keystore.type`에 지정된 형식의 인증서 체인입니다. 기본 SSL 엔진 팩토리는 X.509 인증서 목록이 포함된 PEM 형식만 지원합니다.

| 속성 | 값 |
|------|-----|
| 타입 | password |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | high |

---

### ssl.keystore.key

`ssl.keystore.type`에 지정된 형식의 개인 키입니다. 기본 SSL 엔진 팩토리는 PKCS#8 키가 포함된 PEM 형식만 지원합니다. 키가 암호화된 경우 `ssl.key.password`를 사용하여 키 비밀번호를 지정해야 합니다.

| 속성 | 값 |
|------|-----|
| 타입 | password |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | high |

---

### ssl.keystore.location

keystore 파일의 위치입니다. 클라이언트에서는 선택 사항이며 양방향 인증에 사용할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | high |

---

### ssl.keystore.password

keystore 파일의 저장소 비밀번호입니다. 클라이언트에서는 선택 사항이며 `ssl.keystore.location`이 설정된 경우에만 필요합니다. PEM 형식에서는 keystore 비밀번호가 지원되지 않습니다.

| 속성 | 값 |
|------|-----|
| 타입 | password |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | high |

---

### ssl.truststore.certificates

`ssl.truststore.type`에 지정된 형식의 신뢰할 수 있는 인증서입니다. 기본 SSL 엔진 팩토리는 X.509 인증서가 포함된 PEM 형식만 지원합니다.

| 속성 | 값 |
|------|-----|
| 타입 | password |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | high |

---

### ssl.truststore.location

trust store 파일의 위치입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | high |

---

### ssl.truststore.password

trust store 파일의 비밀번호입니다. 비밀번호가 설정되지 않으면 설정된 trust store 파일이 여전히 사용되지만 무결성 검사가 비활성화됩니다. PEM 형식에서는 truststore 비밀번호가 지원되지 않습니다.

| 속성 | 값 |
|------|-----|
| 타입 | password |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | high |

---

## Medium 중요도 설정

### allow.auto.create.topics

토픽을 구독하거나 할당할 때 브로커에서 자동으로 토픽을 생성할 수 있도록 허용합니다. 브로커의 `auto.create.topics.enable` 설정이 true인 경우에만 자동 생성됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | boolean |
| 기본값 | true |
| 유효값 | (any) |
| 중요도 | medium |

---

### auto.offset.reset

Kafka에 초기 오프셋이 없거나 현재 오프셋이 더 이상 서버에 존재하지 않는 경우(예: 해당 데이터가 삭제된 경우) 수행할 작업입니다:
- `earliest`: 가장 오래된 오프셋으로 자동 재설정
- `latest`: 가장 최근 오프셋으로 자동 재설정
- `by_duration:<duration>`: 현재 타임스탬프에서 duration을 뺀 시점의 오프셋으로 재설정 (예: `by_duration:PT5M`은 5분 전)
- `none`: 컨슈머 그룹에 대한 이전 오프셋을 찾을 수 없는 경우 컨슈머에게 예외 발생

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | latest |
| 유효값 | [latest, earliest, none, by_duration] |
| 중요도 | medium |

---

### client.dns.lookup

클라이언트가 DNS 조회를 사용하는 방법을 제어합니다:
- `use_all_dns_ips`: 조회에서 반환된 각 IP 주소에 대해 순서대로 연결을 시도합니다. 연결이 실패하면 다음 IP로 시도합니다.
- `resolve_canonical_bootstrap_servers_only`: 각 부트스트랩 주소를 정규 이름 목록으로 확인합니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | use_all_dns_ips |
| 유효값 | [use_all_dns_ips, resolve_canonical_bootstrap_servers_only] |
| 중요도 | medium |

---

### connections.max.idle.ms

이 설정에 지정된 시간(밀리초) 동안 유휴 상태인 연결을 닫습니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 540000 (9분) |
| 유효값 | (any) |
| 중요도 | medium |

---

### default.api.timeout.ms

명시적인 타임아웃 매개변수를 지정하지 않는 클라이언트 API의 기본 타임아웃을 지정합니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 60000 (60초) |
| 유효값 | [0,...] |
| 중요도 | medium |

---

### enable.auto.commit

true인 경우 컨슈머의 오프셋이 백그라운드에서 주기적으로 자동 커밋됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | boolean |
| 기본값 | true |
| 유효값 | (any) |
| 중요도 | medium |

---

### exclude.internal.topics

구독된 패턴과 일치하는 내부 토픽을 구독에서 제외할지 여부입니다. 내부 토픽을 명시적으로 구독할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | boolean |
| 기본값 | true |
| 유효값 | (any) |
| 중요도 | medium |

---

### fetch.max.bytes

서버가 fetch 요청에 대해 반환해야 하는 최대 데이터 양입니다. 레코드는 컨슈머에 의해 배치로 가져오며, fetch의 첫 번째 비어 있지 않은 파티션의 첫 번째 레코드 배치가 이 값보다 크면 컨슈머가 진행할 수 있도록 레코드 배치가 여전히 반환됩니다. 따라서 이것은 절대적인 최대값이 아닙니다. 브로커가 허용하는 최대 레코드 배치 크기는 `message.max.bytes`(브로커 설정) 또는 `max.message.bytes`(토픽 설정)를 통해 정의됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 52428800 (50MB) |
| 유효값 | [0,...] |
| 중요도 | medium |

---

### group.instance.id

엔드 유저가 제공하는 컨슈머 인스턴스의 고유 식별자입니다. 비어 있지 않은 문자열만 허용됩니다. 설정된 경우 컨슈머는 정적 멤버로 처리되어 한 번에 하나의 인스턴스만 이 ID로 그룹에 참여할 수 있습니다. 이는 더 큰 세션 타임아웃과 함께 사용하여 일시적인 가용성 저하(예: 프로세스 재시작)로 인한 그룹 리밸런싱을 방지할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효값 | 비어 있지 않은 문자열 |
| 중요도 | medium |

---

### group.remote.assignor

서버 측 할당자의 이름입니다. `group.protocol`이 "consumer"로 설정된 경우에만 적용됩니다. 설정되지 않은 경우 그룹 코디네이터가 할당자를 선택합니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | medium |

---

### isolation.level

트랜잭션 방식으로 작성된 메시지를 읽는 방법을 제어합니다:
- `read_committed`: `poll()`은 커밋된 트랜잭션 메시지만 반환합니다. 비트랜잭션 메시지, 커밋된 트랜잭션 메시지 및 열린 트랜잭션에 속하지 않는 메시지를 반환합니다.
- `read_uncommitted`: `poll()`은 중단된 트랜잭션 메시지를 포함하여 모든 메시지를 반환합니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | read_uncommitted |
| 유효값 | [read_committed, read_uncommitted] |
| 중요도 | medium |

---

### max.poll.interval.ms

그룹 관리를 사용할 때 `poll()` 호출 사이의 최대 지연입니다. 이것은 컨슈머가 더 많은 레코드를 가져오기 전에 유휴 상태일 수 있는 시간의 상한을 설정합니다. 이 타임아웃이 만료되기 전에 `poll()`이 호출되지 않으면 컨슈머가 실패한 것으로 간주되고 그룹은 파티션을 다른 멤버에게 재할당하기 위해 리밸런싱합니다. 이 타임아웃에 도달한 `group.instance.id`가 null이 아닌 컨슈머의 경우, 파티션이 즉시 재할당되지 않습니다. 대신, 컨슈머는 하트비트 전송을 중지하고 `session.timeout.ms` 만료 후 파티션이 재할당됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 300000 (5분) |
| 유효값 | [1,...] |
| 중요도 | medium |

---

### max.poll.records

`poll()` 호출에서 반환되는 최대 레코드 수입니다. 이것은 기본 fetch 동작에 영향을 미치지 않습니다. 컨슈머는 각 fetch 요청에서 레코드를 캐시하고 `poll()`에서 점진적으로 반환합니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 500 |
| 유효값 | [1,...] |
| 중요도 | medium |

---

### partition.assignment.strategy

클라이언트가 지원하는 파티션 할당 전략 클래스 목록입니다. 그룹 관리를 사용할 때 그룹 멤버 간에 파티션 소유권을 분배하는 데 사용됩니다. 사용 가능한 옵션:
- `org.apache.kafka.clients.consumer.RangeAssignor`: 토픽당 범위 기반 할당
- `org.apache.kafka.clients.consumer.RoundRobinAssignor`: 라운드 로빈 방식 할당
- `org.apache.kafka.clients.consumer.StickyAssignor`: 기존 할당을 유지하며 균형 재조정
- `org.apache.kafka.clients.consumer.CooperativeStickyAssignor`: 협력적 리밸런싱을 통한 스티키 할당

참고: classic 그룹 프로토콜에만 적용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | RangeAssignor, CooperativeStickyAssignor |
| 유효값 | 비어 있지 않은 문자열 |
| 중요도 | medium |

---

### receive.buffer.bytes

데이터를 읽을 때 사용할 TCP 수신 버퍼(SO_RCVBUF) 크기입니다. 값이 -1이면 OS 기본값이 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 65536 (64KB) |
| 유효값 | [-1,...] |
| 중요도 | medium |

---

### request.timeout.ms

클라이언트가 요청의 응답을 기다리는 최대 시간입니다. 타임아웃이 경과하기 전에 응답이 수신되지 않으면 클라이언트는 필요한 경우 요청을 재전송하거나 재시도가 소진되면 요청을 실패 처리합니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 30000 (30초) |
| 유효값 | [0,...] |
| 중요도 | medium |

---

### sasl.client.callback.handler.class

AuthenticateCallbackHandler 인터페이스를 구현하는 SASL 클라이언트 콜백 핸들러 클래스의 정규화된 이름입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | medium |

---

### sasl.jaas.config

JAAS 구성 파일에서 사용되는 형식의 SASL 연결에 대한 JAAS 로그인 컨텍스트 매개변수입니다. JAAS 구성 파일 형식은 Oracle의 문서에 설명되어 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | password |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | medium |

---

### sasl.kerberos.service.name

Kafka가 실행되는 Kerberos principal 이름입니다. 이는 Kafka의 JAAS 구성 또는 Kafka의 구성에서 정의할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | medium |

---

### sasl.login.callback.handler.class

AuthenticateCallbackHandler 인터페이스를 구현하는 SASL 로그인 콜백 핸들러 클래스의 정규화된 이름입니다. 브로커의 경우 로그인 콜백 핸들러 구성은 리스너 접두사와 소문자 SASL 메커니즘 이름으로 접두사가 붙어야 합니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | medium |

---

### sasl.login.class

Login 인터페이스를 구현하는 클래스의 정규화된 이름입니다. 브로커의 경우 로그인 구성은 리스너 접두사와 소문자 SASL 메커니즘 이름으로 접두사가 붙어야 합니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | medium |

---

### sasl.mechanism

클라이언트 연결에 사용되는 SASL 메커니즘입니다. 보안 공급자가 사용 가능한 모든 메커니즘이 될 수 있습니다. GSSAPI가 기본 메커니즘입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | GSSAPI |
| 유효값 | (any) |
| 중요도 | medium |

---

### sasl.oauthbearer.jwks.endpoint.url

OAuth/OIDC 공급자의 JWKS(JSON Web Key Set) 검색 URL입니다. HTTP(S) 기반 또는 파일 기반 URL이 될 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | medium |

---

### sasl.oauthbearer.token.endpoint.url

OAuth/OIDC ID 공급자의 토큰 엔드포인트 URL 또는 액세스 토큰이 포함된 파일의 URL입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | medium |

---

### security.protocol

브로커와 통신하는 데 사용되는 프로토콜입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | PLAINTEXT |
| 유효값 | [PLAINTEXT, SSL, SASL_PLAINTEXT, SASL_SSL] |
| 중요도 | medium |

---

### send.buffer.bytes

데이터를 보낼 때 사용할 TCP 송신 버퍼(SO_SNDBUF) 크기입니다. 값이 -1이면 OS 기본값이 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 131072 (128KB) |
| 유효값 | [-1,...] |
| 중요도 | medium |

---

### socket.connection.setup.timeout.max.ms

클라이언트가 소켓 연결이 설정될 때까지 기다리는 최대 시간입니다. 연결 설정 타임아웃은 연속적인 연결 실패에 대해 지수적으로 증가합니다. 연결 폭풍을 방지하기 위해 타임아웃에 0.2의 무작위 인수가 적용되어 계산된 값보다 20% 낮은 값에서 20% 높은 값 사이의 무작위 범위가 됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 30000 (30초) |
| 유효값 | (any) |
| 중요도 | medium |

---

### socket.connection.setup.timeout.ms

클라이언트가 소켓 연결이 설정될 때까지 기다리는 시간입니다. 연결이 설정되기 전에 타임아웃이 경과하면 클라이언트는 소켓 채널을 닫습니다. 이 값은 지수 백오프의 초기 백오프 값이며 `socket.connection.setup.timeout.max.ms` 값까지 증가합니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 10000 (10초) |
| 유효값 | (any) |
| 중요도 | medium |

---

### ssl.enabled.protocols

SSL 연결에 대해 활성화된 프로토콜 목록입니다. 기본값은 Java 11 이상에서 실행할 때 'TLSv1.2,TLSv1.3'이고, 그렇지 않으면 'TLSv1.2'입니다. Java 11의 기본값에서는 클라이언트와 서버가 모두 지원하는 경우 TLSv1.3을 선호하고, 그렇지 않으면 TLSv1.2로 폴백합니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | TLSv1.2,TLSv1.3 |
| 유효값 | (any) |
| 중요도 | medium |

---

### ssl.keystore.type

keystore 파일의 파일 형식입니다. 클라이언트에서는 선택 사항입니다. 사용 가능한 값: JKS, PKCS12, PEM.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | JKS |
| 유효값 | (any) |
| 중요도 | medium |

---

### ssl.protocol

SSLContext를 생성하는 데 사용되는 SSL 프로토콜입니다. 기본값은 Java 11 이상에서 실행할 때 'TLSv1.3'이고, 그렇지 않으면 'TLSv1.2'입니다. 이 값은 대부분의 사용 사례에 적합합니다. 최신 JVM에서 허용되는 값은 'TLSv1.2' 및 'TLSv1.3'입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | TLSv1.3 |
| 유효값 | (any) |
| 중요도 | medium |

---

### ssl.provider

SSL 연결에 사용되는 보안 공급자의 이름입니다. 기본값은 JVM의 기본 보안 공급자입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | medium |

---

### ssl.truststore.type

truststore 파일의 파일 형식입니다. 사용 가능한 값: JKS, PKCS12, PEM.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | JKS |
| 유효값 | (any) |
| 중요도 | medium |

---

## Low 중요도 설정

### auto.commit.interval.ms

`enable.auto.commit`이 true로 설정된 경우 컨슈머 오프셋이 Kafka에 자동 커밋되는 빈도(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 5000 (5초) |
| 유효값 | [0,...] |
| 중요도 | low |

---

### check.crcs

소비된 레코드의 CRC32를 자동으로 확인합니다. 이것은 메시지의 on-the-wire 또는 on-disk 손상이 발생하지 않았는지 확인합니다. 이 확인은 약간의 오버헤드를 추가하므로 극단적인 성능을 추구하는 경우 비활성화할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | boolean |
| 기본값 | true |
| 유효값 | (any) |
| 중요도 | low |

---

### client.id

요청을 할 때 서버에 전달할 ID 문자열입니다. 이것의 목적은 서버 측 요청 로깅에서 논리적 애플리케이션 이름을 포함하여 IP/포트 이상의 요청 소스를 추적할 수 있도록 하는 것입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | "" |
| 유효값 | (any) |
| 중요도 | low |

---

### client.rack

이 클라이언트의 랙 식별자입니다. 이것은 브로커에 구성된 브로커 구성 `broker.rack`에 해당하는 문자열 값이 될 수 있습니다. 이것은 이 컨슈머가 있는 위치를 나타냅니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | "" |
| 유효값 | (any) |
| 중요도 | low |

---

### enable.metrics.push

클라이언트 메트릭 구독과 일치하는 구독이 있는 경우 클러스터에 클라이언트 메트릭을 푸시할 수 있도록 합니다.

| 속성 | 값 |
|------|-----|
| 타입 | boolean |
| 기본값 | true |
| 유효값 | (any) |
| 중요도 | low |

---

### fetch.max.wait.ms

`fetch.min.bytes`로 지정된 요구 사항을 즉시 충족하기에 데이터가 충분하지 않은 경우 fetch 요청에 응답하기 전에 서버가 차단하는 최대 시간입니다. 이것은 로컬 로그에서 가져오는 데에만 적용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 500 |
| 유효값 | [0,...] |
| 중요도 | low |

---

### interceptor.classes

인터셉터로 사용할 클래스 목록입니다. `org.apache.kafka.clients.consumer.ConsumerInterceptor` 인터페이스를 구현하면 컨슈머가 수신하는 레코드를 가로채거나 변경할 수 있습니다. 기본적으로 인터셉터는 없습니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | "" |
| 유효값 | 비어 있지 않은 문자열 |
| 중요도 | low |

---

### metadata.max.age.ms

파티션 리더십 변경이 없더라도 메타데이터 새로 고침을 강제하는 기간(밀리초)입니다. 새 브로커나 파티션을 사전에 발견할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 300000 (5분) |
| 유효값 | [0,...] |
| 중요도 | low |

---

### metadata.recovery.rebootstrap.trigger.ms

클라이언트가 알려진 모든 브로커에서 메타데이터를 얻을 수 없는 경우, 클라이언트가 부트스트랩 주소를 사용하여 브로커를 (재)검색하기 전까지의 기간입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 300000 (5분) |
| 유효값 | [0,...] |
| 중요도 | low |

---

### metadata.recovery.strategy

모든 알려진 브로커를 사용할 수 없게 된 경우 복구 전략을 정의합니다:
- `none`: 현재 알려진 브로커가 사용 불가능하면 메타데이터 요청이 실패합니다.
- `rebootstrap`: 부트스트랩 주소를 사용하여 다시 부트스트랩을 시도합니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | rebootstrap |
| 유효값 | [REBOOTSTRAP, NONE] |
| 중요도 | low |

---

### metric.reporters

메트릭 리포터로 사용할 클래스 목록입니다. `org.apache.kafka.common.metrics.MetricsReporter` 인터페이스를 구현하면 새 메트릭 생성 알림을 받는 클래스를 플러그인할 수 있습니다. JmxReporter는 항상 포함되어 JMX 통계를 등록합니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | org.apache.kafka.common.metrics.JmxReporter |
| 유효값 | 비어 있지 않은 문자열 |
| 중요도 | low |

---

### metrics.num.samples

메트릭 계산을 위해 유지되는 샘플 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 2 |
| 유효값 | [1,...] |
| 중요도 | low |

---

### metrics.recording.level

메트릭의 가장 높은 기록 수준입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | INFO |
| 유효값 | [INFO, DEBUG, TRACE] |
| 중요도 | low |

---

### metrics.sample.window.ms

메트릭 샘플이 계산되는 시간 창입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 30000 (30초) |
| 유효값 | [0,...] |
| 중요도 | low |

---

### reconnect.backoff.max.ms

반복적으로 연결에 실패한 브로커에 다시 연결할 때 대기하는 최대 시간(밀리초)입니다. 제공되면 호스트별 백오프가 연속적으로 연결에 실패할 때마다 지수적으로 증가합니다. 연결 폭풍을 방지하기 위해 백오프 증가에 20%의 무작위 지터가 추가됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 1000 (1초) |
| 유효값 | [0,...] |
| 중요도 | low |

---

### reconnect.backoff.ms

주어진 호스트에 다시 연결을 시도하기 전에 대기하는 기본 시간입니다. 이것은 빡빡한 루프에서 호스트에 반복적으로 연결하는 것을 방지합니다. 이 백오프는 클라이언트가 브로커에 보내는 모든 연결 시도에 적용됩니다. 이 값은 지수 백오프의 초기 백오프 값이며 `reconnect.backoff.max.ms` 값까지 증가합니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 50 |
| 유효값 | [0,...] |
| 중요도 | low |

---

### retry.backoff.max.ms

주어진 토픽 파티션에 실패한 요청을 재시도할 때 대기하는 최대 시간(밀리초)입니다. 제공되면 파티션별 백오프가 연속적으로 요청이 실패할 때마다 지수적으로 증가합니다. 재시도 폭풍을 방지하기 위해 백오프 증가에 20%의 무작위 지터가 추가됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 1000 (1초) |
| 유효값 | [0,...] |
| 중요도 | low |

---

### retry.backoff.ms

실패한 요청을 재시도하기 전에 대기하는 시간입니다. 이것은 빡빡한 루프에서 일부 실패 시나리오에 반복적으로 요청을 보내는 것을 방지합니다. 이 값은 지수 백오프의 초기 백오프 값이며 `retry.backoff.max.ms` 값까지 증가합니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 100 |
| 유효값 | [0,...] |
| 중요도 | low |

---

### sasl.kerberos.kinit.cmd

Kerberos kinit 명령 경로입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | /usr/bin/kinit |
| 유효값 | (any) |
| 중요도 | low |

---

### sasl.kerberos.min.time.before.relogin

새로 고침 시도 사이의 로그인 스레드 대기 시간입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 60000 (60초) |
| 유효값 | (any) |
| 중요도 | low |

---

### sasl.kerberos.ticket.renew.jitter

갱신 시간에 추가되는 무작위 지터의 백분율입니다.

| 속성 | 값 |
|------|-----|
| 타입 | double |
| 기본값 | 0.05 |
| 유효값 | (any) |
| 중요도 | low |

---

### sasl.kerberos.ticket.renew.window.factor

로그인 스레드는 마지막 새로 고침에서 티켓 만료까지의 시간에 지정된 창 인수를 더한 시간에 도달하면 대기합니다. 이 시간에 티켓 갱신을 시도합니다.

| 속성 | 값 |
|------|-----|
| 타입 | double |
| 기본값 | 0.8 |
| 유효값 | (any) |
| 중요도 | low |

---

### sasl.login.connect.timeout.ms

외부 인증 공급자 연결 타임아웃의 (선택적) 값(밀리초)입니다. 현재 OAUTHBEARER에만 적용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | low |

---

### sasl.login.read.timeout.ms

외부 인증 공급자 읽기 타임아웃의 (선택적) 값(밀리초)입니다. 현재 OAUTHBEARER에만 적용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | low |

---

### sasl.login.refresh.buffer.seconds

자격 증명을 새로 고침할 때 자격 증명이 만료되기 전에 유지할 버퍼 시간(초)입니다. 새로 고침이 버퍼 초 수보다 가깝게 발생하면 버퍼 시간만큼 일찍 만료되도록 새로 고침이 이동됩니다. 유효값은 0에서 3600(1시간) 사이입니다. 값이 지정되지 않으면 기본값 300(5분)이 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | short |
| 기본값 | 300 |
| 유효값 | [0,...,3600] |
| 중요도 | low |

---

### sasl.login.refresh.min.period.seconds

로그인 새로 고침 스레드가 자격 증명을 새로 고치기 전에 대기하는 최소 시간(초)입니다. 유효값은 0에서 900(15분) 사이입니다. 값이 지정되지 않으면 기본값 60(1분)이 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | short |
| 기본값 | 60 |
| 유효값 | [0,...,900] |
| 중요도 | low |

---

### sasl.login.refresh.window.factor

로그인 새로 고침 스레드는 자격 증명 수명을 기준으로 지정된 창 인수에 도달하면 자격 증명을 새로 고치려고 시도합니다. 유효값은 0.5(50%)에서 1.0(100%) 사이입니다. 값이 지정되지 않으면 기본값 0.8(80%)이 사용됩니다. 현재 OAUTHBEARER에만 적용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | double |
| 기본값 | 0.8 |
| 유효값 | [0.5,...,1.0] |
| 중요도 | low |

---

### sasl.login.refresh.window.jitter

로그인 새로 고침 스레드의 대기 시간에 추가되는 최대 무작위 지터입니다. 유효값은 0에서 0.25(25%) 사이입니다. 값이 지정되지 않으면 기본값 0.05(5%)가 사용됩니다. 현재 OAUTHBEARER에만 적용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | double |
| 기본값 | 0.05 |
| 유효값 | [0.0,...,0.25] |
| 중요도 | low |

---

### sasl.login.retry.backoff.max.ms

외부 인증 공급자에 대한 로그인 시도 사이의 최대 대기 시간(밀리초)입니다. 로그인은 지수 백오프 알고리즘을 사용하며 초기 대기는 `sasl.login.retry.backoff.ms` 설정에 기반하고 시도 사이에 두 배가 되어 `sasl.login.retry.backoff.max.ms` 설정에 지정된 최대 대기 길이까지 증가합니다. 현재 OAUTHBEARER에만 적용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 10000 (10초) |
| 유효값 | (any) |
| 중요도 | low |

---

### sasl.login.retry.backoff.ms

외부 인증 공급자에 대한 로그인 시도 사이의 초기 대기 시간(밀리초)입니다. 로그인은 지수 백오프 알고리즘을 사용하며 초기 대기는 `sasl.login.retry.backoff.ms` 설정에 기반하고 시도 사이에 두 배가 되어 `sasl.login.retry.backoff.max.ms` 설정에 지정된 최대 대기 길이까지 증가합니다. 현재 OAUTHBEARER에만 적용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 100 |
| 유효값 | (any) |
| 중요도 | low |

---

### sasl.oauthbearer.clock.skew.seconds

OAuth/OIDC ID 공급자와 브로커 사이의 허용 가능한 시간 차이(초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 30 |
| 유효값 | (any) |
| 중요도 | low |

---

### sasl.oauthbearer.expected.audience

브로커가 JWT가 발급된 예상 대상 중 하나로 확인할 쉼표로 구분된 대상 목록입니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | low |

---

### sasl.oauthbearer.expected.issuer

브로커가 JWT가 발급된 예상 발급자로 확인할 발급자입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | low |

---

### sasl.oauthbearer.header.urlencode

RFC6749에 따라 OAuth 클라이언트 자격 증명을 authorization 헤더에서 URL 인코딩할지 여부입니다.

| 속성 | 값 |
|------|-----|
| 타입 | boolean |
| 기본값 | false |
| 유효값 | (any) |
| 중요도 | low |

---

### sasl.oauthbearer.jwks.endpoint.refresh.ms

브로커가 JWT 서명을 확인하기 위해 키가 포함된 JWKS(JSON Web Key Set) 캐시를 새로 고치기 전에 대기하는 시간(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 3600000 (1시간) |
| 유효값 | (any) |
| 중요도 | low |

---

### sasl.oauthbearer.jwks.endpoint.retry.backoff.max.ms

외부 인증 공급자의 JWKS(JSON Web Key Set) 검색 시도 사이의 최대 대기 시간(밀리초)입니다. JWKS 검색은 지수 백오프 알고리즘을 사용합니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 10000 (10초) |
| 유효값 | (any) |
| 중요도 | low |

---

### sasl.oauthbearer.jwks.endpoint.retry.backoff.ms

외부 인증 공급자의 JWKS(JSON Web Key Set) 검색 시도 사이의 초기 대기 시간(밀리초)입니다. JWKS 검색은 지수 백오프 알고리즘을 사용합니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 100 |
| 유효값 | (any) |
| 중요도 | low |

---

### sasl.oauthbearer.scope.claim.name

OAuth 범위를 찾을 클레임 이름입니다. 기본값은 "scope"입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | scope |
| 유효값 | (any) |
| 중요도 | low |

---

### sasl.oauthbearer.sub.claim.name

주체를 찾을 OAuth 클레임 이름입니다. 기본값은 "sub"입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | sub |
| 유효값 | (any) |
| 중요도 | low |

---

### security.providers

보안 알고리즘을 구현하는 공급자를 반환하는 구성 가능한 생성자 클래스 목록입니다. 이러한 클래스는 `org.apache.kafka.common.security.auth.SecurityProviderCreator` 인터페이스를 구현해야 합니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | low |

---

### ssl.cipher.suites

암호 스위트 목록입니다. 이것은 TLS 또는 SSL 네트워크 프로토콜을 사용하여 네트워크 연결의 보안 설정을 협상하는 데 사용되는 인증, 암호화, MAC 및 키 교환 알고리즘의 명명된 조합입니다. 기본적으로 사용 가능한 모든 암호 스위트가 지원됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | low |

---

### ssl.endpoint.identification.algorithm

서버 인증서를 사용하여 서버 호스트 이름을 검증하는 엔드포인트 식별 알고리즘입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | https |
| 유효값 | (any) |
| 중요도 | low |

---

### ssl.engine.factory.class

SSLEngine 객체를 제공하기 위한 org.apache.kafka.common.security.auth.SslEngineFactory 타입의 클래스입니다. 기본값은 org.apache.kafka.common.security.ssl.DefaultSslEngineFactory입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | low |

---

### ssl.keymanager.algorithm

SSL 연결에 대해 키 관리자 팩토리가 사용하는 알고리즘입니다. 기본값은 Java Virtual Machine에 대해 구성된 키 관리자 팩토리 알고리즘입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | SunX509 |
| 유효값 | (any) |
| 중요도 | low |

---

### ssl.secure.random.implementation

SSL 암호화 작업에 사용할 SecureRandom PRNG 구현입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | low |

---

### ssl.trustmanager.algorithm

SSL 연결에 대해 신뢰 관리자 팩토리가 사용하는 알고리즘입니다. 기본값은 Java Virtual Machine에 대해 구성된 신뢰 관리자 팩토리 알고리즘입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | PKIX |
| 유효값 | (any) |
| 중요도 | low |

---

## 주요 설정 사용 예시

### 기본 컨슈머 설정

```java
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");
props.put("group.id", "my-consumer-group");
props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
props.put("enable.auto.commit", "true");
props.put("auto.commit.interval.ms", "1000");
props.put("auto.offset.reset", "earliest");
```

### 성능 튜닝 설정

```java
// fetch 최적화
props.put("fetch.min.bytes", 50000);
props.put("fetch.max.wait.ms", 1000);
props.put("max.partition.fetch.bytes", 1048576);
props.put("fetch.max.bytes", 52428800);

// poll 설정
props.put("max.poll.records", 500);
props.put("max.poll.interval.ms", 300000);
```

### SSL 보안 설정

```java
props.put("security.protocol", "SSL");
props.put("ssl.truststore.location", "/path/to/truststore.jks");
props.put("ssl.truststore.password", "truststore-password");
props.put("ssl.keystore.location", "/path/to/keystore.jks");
props.put("ssl.keystore.password", "keystore-password");
props.put("ssl.key.password", "key-password");
```

### SASL 인증 설정

```java
props.put("security.protocol", "SASL_SSL");
props.put("sasl.mechanism", "PLAIN");
props.put("sasl.jaas.config",
    "org.apache.kafka.common.security.plain.PlainLoginModule required " +
    "username=\"user\" password=\"password\";");
```

---

## 참고 자료

- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Consumer Configuration](https://kafka.apache.org/documentation/#consumerconfigs)
- [Kafka Consumer API](https://kafka.apache.org/documentation/#consumerapi)

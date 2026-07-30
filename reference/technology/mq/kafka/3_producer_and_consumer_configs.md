# Kafka 프로듀서와 컨슈머 설정

## Kafka 프로듀서 설정

> 원본 문서: https://kafka.apache.org/documentation/#producerconfigs

---

### 목차

1. [개요](#개요)
2. [높은 중요도 설정](#높은-중요도-설정)
3. [중간 중요도 설정](#중간-중요도-설정)
4. [낮은 중요도 설정](#낮은-중요도-설정)

---

### 개요

Kafka 프로듀서는 Kafka 토픽에 레코드를 게시하는 클라이언트입니다. 각 설정은 중요도(importance)에 따라 높음(high), 중간(medium), 낮음(low)으로 분류됩니다.

---

### 높은 중요도 설정

#### key.serializer

키(key)에 대한 직렬화기(Serializer) 클래스입니다. `org.apache.kafka.common.serialization.Serializer` 인터페이스를 구현해야 합니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | - |
| 유효한 값 | - |
| 중요도 | high |

---

#### value.serializer

값(value)에 대한 직렬화기(Serializer) 클래스입니다. `org.apache.kafka.common.serialization.Serializer` 인터페이스를 구현해야 합니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | - |
| 유효한 값 | - |
| 중요도 | high |

---

#### bootstrap.servers

Kafka 클러스터에 대한 초기 연결에 사용하는 호스트/포트 쌍의 목록입니다. 클라이언트는 부트스트랩 이후에는 여기에 지정한 서버와 무관하게 모든 서버를 사용합니다. 이 목록은 전체 서버 세트를 탐색하기 위한 초기 호스트에만 영향을 미칩니다. 형식은 `host1:port1,host2:port2,...`여야 합니다. 이 서버들은 전체 클러스터 멤버십(동적으로 변경될 수 있음)을 탐색하기 위한 초기 연결에만 사용되므로 전체 서버 세트를 포함할 필요는 없습니다(단, 서버 장애에 대비해 둘 이상 지정하는 것을 권장합니다).

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | "" |
| 유효한 값 | non-null string |
| 중요도 | high |

---

#### buffer.memory

프로듀서가 서버로 전송하기 전에 레코드를 버퍼링하는 데 사용할 수 있는 총 메모리 바이트 수입니다. 레코드가 서버로 전달될 수 있는 속도보다 빠르게 유입되면, 프로듀서는 `max.block.ms` 동안 블로킹한 후 예외를 발생시킵니다.

이 설정은 프로듀서가 사용하는 총 메모리와 대략적으로 일치해야 하지만, 모든 메모리가 버퍼링에 사용되는 것은 아니므로 엄격한 상한은 아닙니다. 압축(압축이 활성화된 경우)과 진행 중인 요청을 유지하는 데 일부 메모리가 추가로 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 33554432 (32MB) |
| 유효한 값 | [0,...] |
| 중요도 | high |

---

#### compression.type

프로듀서가 생성한 모든 데이터에 대한 압축 유형입니다. 기본값은 `none`(압축 없음)입니다. 유효한 값은 `none`, `gzip`, `snappy`, `lz4`, `zstd`입니다. 압축은 전체 데이터 배치에 대해 수행되므로 배치 효율성도 압축 비율에 영향을 미칩니다(더 많은 배칭은 더 나은 압축을 의미합니다).

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | none |
| 유효한 값 | [none, gzip, snappy, lz4, zstd] |
| 중요도 | high |

---

#### retries

일시적인 오류로 실패한 요청을 재시도하는 횟수입니다. 값을 0보다 크게 설정하면 클라이언트는 일시적인 오류로 실패할 수 있는 요청을 재전송합니다. `delivery.timeout.ms`로 설정된 전달 타임아웃이 먼저 만료되지 않는 한 재시도를 수행합니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 2147483647 |
| 유효한 값 | [0,...,2147483647] |
| 중요도 | high |

---

#### ssl.key.password

키 저장소(key store) 파일에 있는 개인 키의 비밀번호입니다. 클라이언트에서는 선택 사항입니다.

| 속성 | 값 |
|------|-----|
| 타입 | password |
| 기본값 | null |
| 유효한 값 | - |
| 중요도 | high |

---

#### ssl.keystore.certificate.chain

'ssl.keystore.type'으로 지정된 형식의 인증서 체인입니다. 기본 SSL 엔진 팩토리는 X.509 인증서 목록이 있는 PEM 형식만 지원합니다.

| 속성 | 값 |
|------|-----|
| 타입 | password |
| 기본값 | null |
| 유효한 값 | - |
| 중요도 | high |

---

#### ssl.keystore.key

'ssl.keystore.type'으로 지정된 형식의 개인 키입니다. 기본 SSL 엔진 팩토리는 PKCS#8 키가 있는 PEM 형식만 지원합니다. 키가 암호화된 경우 'ssl.key.password'를 사용하여 키 비밀번호를 지정해야 합니다.

| 속성 | 값 |
|------|-----|
| 타입 | password |
| 기본값 | null |
| 유효한 값 | - |
| 중요도 | high |

---

#### ssl.keystore.location

키 저장소 파일의 위치입니다. 클라이언트에서는 선택 사항이며 클라이언트의 양방향 인증에 사용할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효한 값 | - |
| 중요도 | high |

---

#### ssl.keystore.password

키 저장소 파일의 저장소 비밀번호입니다. 클라이언트에서는 선택 사항이며 'ssl.keystore.location'이 구성된 경우에만 필요합니다. PEM 형식에서는 키 저장소 비밀번호가 지원되지 않습니다.

| 속성 | 값 |
|------|-----|
| 타입 | password |
| 기본값 | null |
| 유효한 값 | - |
| 중요도 | high |

---

#### ssl.truststore.certificates

'ssl.truststore.type'으로 지정된 형식의 신뢰할 수 있는 인증서입니다. 기본 SSL 엔진 팩토리는 X.509 인증서가 있는 PEM 형식만 지원합니다.

| 속성 | 값 |
|------|-----|
| 타입 | password |
| 기본값 | null |
| 유효한 값 | - |
| 중요도 | high |

---

#### ssl.truststore.location

신뢰 저장소(trust store) 파일의 위치입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효한 값 | - |
| 중요도 | high |

---

#### ssl.truststore.password

신뢰 저장소 파일의 비밀번호입니다. 비밀번호가 설정되지 않으면 구성된 신뢰 저장소 파일에 대한 액세스는 여전히 가능하지만 무결성 검사는 비활성화됩니다. PEM 형식에서는 신뢰 저장소 비밀번호가 지원되지 않습니다.

| 속성 | 값 |
|------|-----|
| 타입 | password |
| 기본값 | null |
| 유효한 값 | - |
| 중요도 | high |

---

### 중간 중요도 설정

#### batch.size

동일한 파티션으로 전송되는 레코드를 묶는 기본 배치 크기(바이트)를 제어합니다. 프로듀서는 레코드를 함께 배치하여 요청 수를 줄이며, 이를 통해 클라이언트와 서버 모두의 성능이 향상됩니다.

이 크기를 초과하는 레코드 배치는 만들지 않습니다. 브로커로 전송하는 요청에는 여러 배치가 포함될 수 있으며, 전송할 데이터가 있는 파티션마다 하나씩 포함됩니다.

배치 크기가 작으면 배칭 빈도가 낮아져 처리량이 떨어질 수 있습니다(배치 크기가 0이면 배칭이 완전히 비활성화됩니다). 배치 크기가 너무 크면 추가 레코드를 기다리며 항상 해당 크기의 버퍼를 미리 할당하므로 메모리가 다소 낭비될 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 16384 (16KB) |
| 유효한 값 | [0,...] |
| 중요도 | medium |

---

#### client.dns.lookup

클라이언트의 DNS 조회 방식을 제어합니다. `use_all_dns_ips`로 설정하면 연결이 수립될 때까지 반환된 IP 주소를 순서대로 시도합니다. 연결이 끊어지면 다음 IP를 사용하며, 모든 IP를 한 번씩 시도한 뒤에는 호스트 이름을 다시 조회합니다(단, JVM과 OS 모두 DNS 조회 결과를 캐시합니다). `resolve_canonical_bootstrap_servers_only`로 설정하면 각 부트스트랩 주소를 정식 이름 목록으로 확인하며, 부트스트랩 단계 이후에는 `use_all_dns_ips`와 동일하게 동작합니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | use_all_dns_ips |
| 유효한 값 | [use_all_dns_ips, resolve_canonical_bootstrap_servers_only] |
| 중요도 | medium |

---

#### client.id

요청 시 서버에 전달하는 ID 문자열입니다. 서버 측 요청 로그에 논리적 애플리케이션 이름을 포함함으로써 IP/포트 이상으로 요청 출처를 추적할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | "" |
| 유효한 값 | - |
| 중요도 | medium |

---

#### compression.gzip.level

`compression.type`이 `gzip`으로 설정된 경우 사용할 압축 수준입니다. 허용되는 범위는 1-9이며 값이 높을수록 더 나은 압축 비율을 제공하지만 더 많은 CPU 오버헤드를 발생시킵니다. 기본값 -1은 gzip 라이브러리의 기본 수준(일반적으로 6)을 사용합니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | -1 |
| 유효한 값 | [1,...,9] 또는 -1 |
| 중요도 | medium |

---

#### compression.lz4.level

`compression.type`이 `lz4`로 설정된 경우 사용할 압축 수준입니다. 허용되는 범위는 1-17이며 값이 높을수록 더 나은 압축 비율을 제공하지만 더 많은 CPU 오버헤드를 발생시킵니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 9 |
| 유효한 값 | [1,...,17] |
| 중요도 | medium |

---

#### compression.zstd.level

`compression.type`이 `zstd`로 설정된 경우 사용할 압축 수준입니다. 허용되는 범위는 -131072에서 22까지이며 값이 높을수록 더 나은 압축 비율을 제공하지만 더 많은 CPU 오버헤드를 발생시킵니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 3 |
| 유효한 값 | [-131072,...,22] |
| 중요도 | medium |

---

#### connections.max.idle.ms

지정된 밀리초 수 후에 유휴 연결을 닫습니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 540000 (9분) |
| 유효한 값 | - |
| 중요도 | medium |

---

#### delivery.timeout.ms

`send()` 호출이 반환된 후 성공 또는 실패를 보고하는 시간의 상한입니다. 레코드 전송 전 대기 시간, 브로커의 승인(ack) 대기 시간(해당하는 경우), 재시도 가능한 전송 실패에 허용되는 시간을 모두 포함하여 제한합니다. 복구 불가능한 오류가 발생하거나, 재시도 횟수가 소진되거나, 이미 만료 기한이 지난 배치에 레코드가 추가되면 이 설정보다 일찍 실패를 보고할 수 있습니다. 이 값은 `request.timeout.ms`와 `linger.ms`의 합 이상이어야 합니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 120000 (2분) |
| 유효한 값 | [0,...] |
| 중요도 | medium |

---

#### linger.ms

프로듀서는 요청 전송 사이에 도착하는 모든 레코드를 단일 배치 요청으로 묶습니다. 이는 일반적으로 레코드가 전송 속도보다 빠르게 유입되는 부하 상황에서만 발생합니다. 그러나 적당한 부하에서도 요청 수를 줄이고 싶은 경우가 있습니다. 이 설정은 작은 인위적 지연을 추가하여 이를 구현합니다. 레코드를 즉시 보내는 대신 프로듀서는 지정된 시간까지 기다려 레코드를 함께 배치합니다. TCP의 Nagle 알고리즘과 유사한 개념입니다. 이 설정은 배칭 지연의 상한을 제공합니다: 파티션에 `batch.size`만큼의 레코드가 누적되면 이 설정과 무관하게 즉시 전송되지만, 누적된 바이트가 그보다 적으면 지정된 시간 동안 추가 레코드를 기다립니다. 기본값은 5로, 부하가 없을 때 5ms의 지연을 발생시킵니다. `linger.ms=0`으로 설정하면 레코드는 즉시 전송되며, 이 경우 이전 전송이 진행 중일 때 도착하는 레코드에 한해서만 배칭이 발생합니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 5 |
| 유효한 값 | [0,...] |
| 중요도 | medium |

---

#### max.block.ms

프로듀서의 `send()` 및 관련 메서드가 블로킹되는 최대 시간을 제어합니다. 버퍼가 가득 차거나 메타데이터를 사용할 수 없는 경우 이 메서드들이 블로킹될 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 60000 (1분) |
| 유효한 값 | [0,...] |
| 중요도 | medium |

---

#### max.request.size

요청의 최대 크기(바이트)입니다. 단일 요청에서 전송할 레코드 배치 수를 제한하며, 압축되지 않은 단일 레코드 배치의 최대 크기에 대한 상한 역할도 합니다. 서버에도 레코드 배치 크기에 대한 자체 상한이 있으며(압축이 활성화된 경우 압축 후 크기 기준), 이 값과 다를 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 1048576 (1MB) |
| 유효한 값 | [0,...] |
| 중요도 | medium |

---

#### partitioner.class

레코드가 생성될 때 어떤 파티션으로 보낼지 결정합니다. 사용 가능한 옵션은 다음과 같습니다:

- `null`로 설정하면: 기본 파티셔닝 로직이 사용됩니다. 레코드에 파티션이 지정되면 해당 파티션을 사용합니다. 키가 있는 레코드는 키의 해시를 기반으로 파티션에 할당됩니다. 키가 없는 레코드는 배치가 가득 찰 때 변경되는 스티키 파티셔닝(sticky partition) 전략을 사용합니다.

- `org.apache.kafka.clients.producer.RoundRobinPartitioner`: 파티션 세트를 순환하며 라운드 로빈 방식으로 할당합니다. 배치 효율성이 낮기 때문에 권장되지 않습니다.

- `org.apache.kafka.clients.producer.Partitioner` 인터페이스를 구현하는 커스텀 파티셔너의 정규화된 클래스 이름입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 유효한 값 | - |
| 중요도 | medium |

---

#### partitioner.ignore.keys

'true'로 설정하면 프로듀서는 파티션을 선택할 때 레코드 키를 사용하지 않습니다. 'false'이면 프로듀서는 키가 있을 때 키의 해시를 기반으로 파티션을 선택합니다. 참고: 커스텀 파티셔너가 사용되는 경우 이 설정은 효과가 없습니다.

| 속성 | 값 |
|------|-----|
| 타입 | boolean |
| 기본값 | false |
| 유효한 값 | - |
| 중요도 | medium |

---

#### receive.buffer.bytes

데이터를 읽을 때 사용할 TCP 수신 버퍼(SO_RCVBUF)의 크기입니다. 값이 -1이면 OS 기본값이 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 32768 (32KB) |
| 유효한 값 | [-1,...] |
| 중요도 | medium |

---

#### request.timeout.ms

클라이언트가 요청에 대한 응답을 기다리는 최대 시간을 제어합니다. 타임아웃이 경과하기 전에 응답을 받지 못하면 필요한 경우 클라이언트가 요청을 재전송하거나, 재시도 횟수가 소진되면 요청이 실패합니다. 불필요한 프로듀서 재시도로 인한 메시지 중복 가능성을 줄이기 위해 브로커 설정인 `replica.lag.time.max.ms`보다 큰 값으로 설정해야 합니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 30000 (30초) |
| 유효한 값 | [0,...] |
| 중요도 | medium |

---

#### sasl.client.callback.handler.class

AuthenticateCallbackHandler 인터페이스를 구현하는 SASL 클라이언트 콜백 핸들러 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 유효한 값 | - |
| 중요도 | medium |

---

#### sasl.jaas.config

JAAS 구성 파일에서 사용되는 형식으로 SASL 연결에 대한 JAAS 로그인 컨텍스트 매개변수입니다. JAAS 구성 파일 형식은 [Oracle 문서](https://docs.oracle.com/javase/8/docs/technotes/guides/security/jgss/tutorials/LoginConfigFile.html)에 설명되어 있습니다. 값의 형식은 `loginModuleClass controlFlag (optionName=optionValue)*;`입니다. 브로커의 경우 리스너 접두사와 소문자 SASL 메커니즘 이름으로 구성에 접두사를 붙여야 합니다. 예: `listener.name.sasl_ssl.scram-sha-256.sasl.jaas.config=com.example.ScramLoginModule required;`

| 속성 | 값 |
|------|-----|
| 타입 | password |
| 기본값 | null |
| 유효한 값 | - |
| 중요도 | medium |

---

#### sasl.kerberos.service.name

Kafka가 실행되는 Kerberos 주체 이름입니다. 이는 Kafka의 JAAS 구성에서 정의하거나 Kafka 구성에서 정의할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효한 값 | - |
| 중요도 | medium |

---

#### sasl.login.callback.handler.class

AuthenticateCallbackHandler 인터페이스를 구현하는 SASL 로그인 콜백 핸들러 클래스입니다. 브로커의 경우 로그인 콜백 핸들러 구성은 리스너 접두사와 소문자 SASL 메커니즘 이름으로 접두사가 붙어야 합니다. 예: `listener.name.sasl_ssl.scram-sha-256.sasl.login.callback.handler.class=com.example.CustomScramLoginCallbackHandler`

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 유효한 값 | - |
| 중요도 | medium |

---

#### sasl.login.class

SASL 인증을 위한 Login 인터페이스를 구현하는 클래스입니다. 브로커의 경우 로그인 구성은 리스너 접두사와 소문자 SASL 메커니즘 이름으로 접두사가 붙어야 합니다. 예: `listener.name.sasl_ssl.scram-sha-256.sasl.login.class=com.example.CustomScramLogin`

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 유효한 값 | - |
| 중요도 | medium |

---

#### sasl.mechanism

클라이언트 연결에 사용되는 SASL 메커니즘입니다. 보안 제공자가 사용 가능한 모든 메커니즘이 될 수 있습니다. GSSAPI가 기본 메커니즘입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | GSSAPI |
| 유효한 값 | - |
| 중요도 | medium |

---

#### sasl.oauthbearer.jwks.endpoint.url

제공자의 JWKS(JSON Web Key Set)를 검색할 수 있는 URL입니다. URL은 HTTP(S) 기반 또는 파일 기반일 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효한 값 | - |
| 중요도 | medium |

---

#### sasl.oauthbearer.token.endpoint.url

OAuth/OIDC ID 제공자의 URL입니다. 구성 값이 OAUTHBEARER 및 액세스 토큰 검색과 관련된 URL 접두사로 적합하다고 생각되면 'https://'로 시작해야 합니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효한 값 | - |
| 중요도 | medium |

---

#### security.protocol

브로커와 통신하는 데 사용되는 프로토콜입니다. 유효한 값은 PLAINTEXT, SSL, SASL_PLAINTEXT, SASL_SSL입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | PLAINTEXT |
| 유효한 값 | (대소문자 구분 없음) [SASL_SSL, PLAINTEXT, SSL, SASL_PLAINTEXT] |
| 중요도 | medium |

---

#### send.buffer.bytes

데이터를 보낼 때 사용할 TCP 전송 버퍼(SO_SNDBUF)의 크기입니다. 값이 -1이면 OS 기본값이 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 131072 (128KB) |
| 유효한 값 | [-1,...] |
| 중요도 | medium |

---

#### socket.connection.setup.timeout.max.ms

클라이언트가 소켓 연결 수립을 기다리는 최대 시간입니다. 연결 실패가 반복될수록 타임아웃은 지수적으로 증가하며 이 최대값까지 늘어납니다. 연결 폭풍(connection storm)을 방지하기 위해 타임아웃에 20%의 랜덤 지터가 적용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 30000 (30초) |
| 유효한 값 | - |
| 중요도 | medium |

---

#### socket.connection.setup.timeout.ms

클라이언트가 소켓 연결 수립을 기다리는 시간입니다. 타임아웃이 경과하기 전에 연결이 수립되지 않으면 클라이언트는 소켓 채널을 닫습니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 10000 (10초) |
| 유효한 값 | - |
| 중요도 | medium |

---

#### ssl.enabled.protocols

SSL 연결에 사용 가능한 프로토콜 목록입니다. Java 11 이상에서는 기본값이 'TLSv1.2,TLSv1.3'이고, 그 미만에서는 'TLSv1.2'입니다. Java 11의 기본값을 사용하면 클라이언트와 서버가 모두 지원하는 경우 TLSv1.3을 우선 사용하고, 그렇지 않으면 TLSv1.2로 대체합니다(양쪽 모두 최소 TLSv1.2를 지원한다고 가정).

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | TLSv1.2,TLSv1.3 |
| 유효한 값 | - |
| 중요도 | medium |

---

#### ssl.keystore.type

키 저장소 파일의 파일 형식입니다. 클라이언트에서는 선택 사항입니다. 사용 가능한 기본값은 기본 JRE에 따라 다릅니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | JKS |
| 유효한 값 | - |
| 중요도 | medium |

---

#### ssl.protocol

SSLContext를 생성하는 데 사용되는 SSL 프로토콜입니다. Java 11 이상에서는 기본값이 'TLSv1.3'이고, 그 미만에서는 'TLSv1.2'입니다. 대부분의 사용 사례에서 이 기본값이 적합합니다. 최신 JVM에서 허용되는 값은 'TLSv1.2'와 'TLSv1.3'입니다. 'TLS', 'TLSv1.1', 'SSL', 'SSLv2', 'SSLv3'은 이전 JVM에서 지원될 수 있으나 알려진 보안 취약점이 있으므로 사용하지 않는 것을 권장합니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | TLSv1.3 |
| 유효한 값 | - |
| 중요도 | medium |

---

#### ssl.provider

SSL 연결에 사용되는 보안 제공자의 이름입니다. 기본값은 JVM의 기본 보안 제공자입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효한 값 | - |
| 중요도 | medium |

---

#### ssl.truststore.type

신뢰 저장소 파일의 파일 형식입니다. 사용 가능한 기본값은 기본 JRE에 따라 다릅니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | JKS |
| 유효한 값 | - |
| 중요도 | medium |

---

### 낮은 중요도 설정

#### acks

프로듀서가 요청을 완료된 것으로 간주하기 전에 리더가 받아야 하는 승인(acknowledgment) 수입니다. 전송된 레코드의 내구성을 제어합니다. 허용되는 값은 다음과 같습니다:

- `acks=0`: 프로듀서는 서버로부터 어떠한 승인도 기다리지 않습니다. 레코드는 즉시 소켓 버퍼에 추가되고 전송된 것으로 간주됩니다. 서버가 레코드를 수신했다는 보장이 없으며, `retries` 설정도 적용되지 않습니다(클라이언트는 일반적으로 실패를 감지하지 못하기 때문입니다). 각 레코드에 대해 반환되는 오프셋은 항상 -1입니다.

- `acks=1`: 리더는 레코드를 로컬 로그에 기록한 후 팔로워의 완전한 승인을 기다리지 않고 응답합니다. 리더가 레코드를 승인한 직후 팔로워가 복제하기 전에 장애가 발생하면 레코드가 손실될 수 있습니다.

- `acks=all`: 리더는 동기화된 모든 레플리카(ISR)가 레코드를 승인할 때까지 기다립니다. 최소 하나의 동기화된 레플리카가 살아 있는 한 레코드가 손실되지 않음을 보장합니다. 가장 강력한 내구성 보장이며, `acks=-1` 설정과 동일합니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | all |
| 유효한 값 | [all, -1, 0, 1] |
| 중요도 | low |

---

#### enable.idempotence

'true'로 설정하면 프로듀서는 스트림에 메시지가 정확히 한 번만 기록되도록 보장합니다. 'false'이면 브로커 장애 등으로 인한 프로듀서 재시도가 스트림에 중복 메시지를 기록할 수 있습니다. 멱등성(idempotence)을 활성화하려면 `max.in.flight.requests.per.connection`이 5 이하이고(메시지 순서 보장), `retries`가 0보다 크며, `acks`가 'all'이어야 합니다.

멱등성은 기본적으로 활성화되어 있습니다. 멱등성을 명시적으로 비활성화하지 않은 상태에서 충돌하는 설정이 있으면 프로듀서가 시작되지 않고 `ConfigException`이 발생합니다.

| 속성 | 값 |
|------|-----|
| 타입 | boolean |
| 기본값 | true |
| 유효한 값 | - |
| 중요도 | low |

---

#### enable.metrics.push

클라이언트 메트릭을 클러스터로 푸시할지 여부입니다. 활성화되면 브로커가 클라이언트 메트릭 구독을 구성할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | boolean |
| 기본값 | true |
| 유효한 값 | - |
| 중요도 | low |

---

#### interceptor.classes

인터셉터로 사용할 클래스 목록입니다. `org.apache.kafka.clients.producer.ProducerInterceptor` 인터페이스를 구현하면 프로듀서가 수신한 레코드를 Kafka 클러스터에 게시하기 전에 가로채거나 변환할 수 있습니다. 기본적으로 인터셉터는 설정되어 있지 않습니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | "" |
| 유효한 값 | non-null string |
| 중요도 | low |

---

#### max.in.flight.requests.per.connection

클라이언트가 블로킹 전에 단일 연결에서 보낼 수 있는 최대 미승인 요청 수입니다. 이 값이 1보다 크고 `enable.idempotence`가 false인 경우, 전송 실패 후 재시도로 인해 메시지 순서가 바뀔 수 있습니다(재시도가 활성화된 경우).

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 5 |
| 유효한 값 | [1,...] |
| 중요도 | low |

---

#### metadata.max.age.ms

새 브로커나 파티션이 없더라도 파티션 리더십 변경을 사전에 감지하기 위해 강제로 메타데이터를 갱신하는 주기(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 300000 (5분) |
| 유효한 값 | [0,...] |
| 중요도 | low |

---

#### metadata.max.idle.ms

프로듀서가 유휴 토픽의 메타데이터를 캐시하는 기간을 제어합니다. 토픽이 마지막으로 사용된 후 경과한 시간이 이 유휴 기간을 초과하면 해당 토픽의 메타데이터가 제거되고, 다음 접근 시 메타데이터를 강제로 다시 가져옵니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 300000 (5분) |
| 유효한 값 | [5000,...] |
| 중요도 | low |

---

#### metadata.recovery.rebootstrap.trigger.ms

클라이언트가 메타데이터를 얻지 못할 때 재부트스트랩을 시도하는 간격입니다. DNS를 사용하고 클러스터 호스트가 변경될 수 있는 환경에서는 재부트스트랩이 유용할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 300000 (5분) |
| 유효한 값 | [0,...] |
| 중요도 | low |

---

#### metadata.recovery.strategy

알려진 브로커를 사용할 수 없을 때 클라이언트가 복구하는 방법을 제어합니다. 'none'으로 설정하면 클라이언트는 알려진 브로커에 재연결하는 방식에 의존합니다. 'rebootstrap'으로 설정하면 클라이언트는 주기적으로 재부트스트랩을 시도하여 새 연결 엔드포인트를 얻습니다(호스트 이름이 변경될 수 있는 VIP를 사용하는 경우 유용합니다).

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | rebootstrap |
| 유효한 값 | (대소문자 구분 없음) [REBOOTSTRAP, NONE] |
| 중요도 | low |

---

#### metric.reporters

메트릭 리포터로 사용할 클래스 목록입니다. `org.apache.kafka.common.metrics.MetricsReporter` 인터페이스를 구현하면 새로운 메트릭 생성 시 알림을 받는 클래스를 플러그인할 수 있습니다. JMX 통계를 등록하기 위해 `JmxReporter`는 항상 포함됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | org.apache.kafka.common.metrics.JmxReporter |
| 유효한 값 | non-null string |
| 중요도 | low |

---

#### metrics.num.samples

메트릭을 계산하기 위해 유지되는 샘플 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 2 |
| 유효한 값 | [1,...] |
| 중요도 | low |

---

#### metrics.recording.level

메트릭의 최고 기록 수준입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | INFO |
| 유효한 값 | [INFO, DEBUG, TRACE] |
| 중요도 | low |

---

#### metrics.sample.window.ms

메트릭 샘플이 계산되는 시간 창입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 30000 (30초) |
| 유효한 값 | [0,...] |
| 중요도 | low |

---

#### partitioner.adaptive.partitioning.enable

'true'로 설정하면 프로듀서가 브로커 성능에 적응하여 더 빠른 브로커에 더 많은 메시지를 전송합니다. 'false'이면 메시지를 균등하게 분배합니다. 커스텀 파티셔너를 사용하는 경우 이 설정은 적용되지 않습니다.

| 속성 | 값 |
|------|-----|
| 타입 | boolean |
| 기본값 | true |
| 유효한 값 | - |
| 중요도 | low |

---

#### partitioner.availability.timeout.ms

브로커가 파티션에 대한 생성 요청을 처리할 수 없을 때, 해당 파티션이 충분히 빠르게 복구되거나 `delivery.timeout.ms`가 먼저 만료될 때까지 해당 파티션을 사용 불가 상태로 처리합니다. 값이 0이면 이 로직이 비활성화됩니다. 커스텀 파티셔너를 사용하거나 `partitioner.adaptive.partitioning.enable`이 'false'인 경우 이 설정은 적용되지 않습니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 0 |
| 유효한 값 | [0,...] |
| 중요도 | low |

---

#### reconnect.backoff.max.ms

반복적으로 연결에 실패한 브로커에 재연결할 때 기다리는 최대 시간(밀리초)입니다. 연결 실패가 반복될수록 호스트별 백오프가 지수적으로 증가하며 이 최대값까지 늘어납니다. 연결 폭풍을 방지하기 위해 백오프 계산 후 20%의 랜덤 지터가 추가됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 1000 (1초) |
| 유효한 값 | [0,...] |
| 중요도 | low |

---

#### reconnect.backoff.ms

특정 호스트에 재연결하기 전에 기다리는 기본 시간입니다. 빡빡한 루프에서 호스트에 반복적으로 연결 시도하는 것을 방지합니다. 이 백오프는 클라이언트가 브로커에 시도하는 모든 연결에 적용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 50 |
| 유효한 값 | [0,...] |
| 중요도 | low |

---

#### retry.backoff.max.ms

파티션에 대해 반복적으로 실패한 요청을 재시도할 때 기다리는 최대 시간(밀리초)입니다. 요청 실패가 반복될수록 파티션별 백오프가 지수적으로 증가하며 이 최대값까지 늘어납니다. 요청 폭풍을 방지하기 위해 백오프 계산 후 20%의 랜덤 지터가 추가됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 1000 (1초) |
| 유효한 값 | [0,...] |
| 중요도 | low |

---

#### retry.backoff.ms

실패한 요청을 재시도하기 전에 기다리는 시간입니다. 일부 실패 시나리오에서 빡빡한 루프로 요청을 반복 전송하는 것을 방지합니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 100 |
| 유효한 값 | [0,...] |
| 중요도 | low |

---

#### sasl.kerberos.kinit.cmd

Kerberos kinit 명령 경로입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | /usr/bin/kinit |
| 유효한 값 | - |
| 중요도 | low |

---

#### sasl.kerberos.min.time.before.relogin

새로 고침 시도 사이의 로그인 스레드 대기 시간입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 60000 (1분) |
| 유효한 값 | - |
| 중요도 | low |

---

#### sasl.kerberos.ticket.renew.jitter

갱신 시간에 추가되는 랜덤 지터의 비율입니다.

| 속성 | 값 |
|------|-----|
| 타입 | double |
| 기본값 | 0.05 |
| 유효한 값 | - |
| 중요도 | low |

---

#### sasl.kerberos.ticket.renew.window.factor

로그인 스레드는 마지막 갱신 시점부터 티켓 만료까지의 시간 중 지정된 비율에 도달할 때까지 대기한 후 티켓 갱신을 시도합니다.

| 속성 | 값 |
|------|-----|
| 타입 | double |
| 기본값 | 0.8 |
| 유효한 값 | - |
| 중요도 | low |

---

#### sasl.login.connect.timeout.ms

외부 인증 제공자 연결 시간 초과 값(밀리초)입니다. 현재 OAUTHBEARER에만 적용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | null |
| 유효한 값 | - |
| 중요도 | low |

---

#### sasl.login.read.timeout.ms

외부 인증 제공자 읽기 시간 초과 값(밀리초)입니다. 현재 OAUTHBEARER에만 적용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | null |
| 유효한 값 | - |
| 중요도 | low |

---

#### sasl.login.refresh.buffer.seconds

자격 증명 갱신 시 자격 증명 만료 전에 유지할 버퍼 시간(초)입니다. 갱신 시점이 자격 증명 만료보다 버퍼 시간 이내에 있으면, 가능한 한 버퍼 시간을 확보하도록 갱신 시점을 앞당깁니다. 유효한 값은 0~3600(1시간)입니다. 기본값은 300(5분)입니다. 이 값과 `sasl.login.refresh.min.period.seconds`의 합이 자격 증명의 남은 수명을 초과하면 두 설정 모두 무시됩니다. 현재 OAUTHBEARER에만 적용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | short |
| 기본값 | 300 |
| 유효한 값 | [0,...,3600] |
| 중요도 | low |

---

#### sasl.login.refresh.min.period.seconds

자격 증명 갱신 전에 로그인 갱신 스레드가 대기하는 최소 시간(초)입니다. 유효한 값은 0~900(15분)입니다. 기본값은 60(1분)입니다. 이 값과 `sasl.login.refresh.buffer.seconds`의 합이 자격 증명의 남은 수명을 초과하면 두 설정 모두 무시됩니다. 현재 OAUTHBEARER에만 적용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | short |
| 기본값 | 60 |
| 유효한 값 | [0,...,900] |
| 중요도 | low |

---

#### sasl.login.refresh.window.factor

자격 증명 수명에 대한 창 비율로, 로그인 갱신 스레드가 자격 증명 수명의 해당 비율에 도달할 때까지 대기한 후 갱신을 시도합니다. 유효한 값은 0.5(50%)~1.0(100%)입니다. 기본값은 0.8(80%)입니다. 현재 OAUTHBEARER에만 적용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | double |
| 기본값 | 0.8 |
| 유효한 값 | [0.5,...,1.0] |
| 중요도 | low |

---

#### sasl.login.refresh.window.jitter

자격 증명 수명에 대한 최대 랜덤 지터 비율로, 로그인 갱신 스레드의 대기 시간에 추가됩니다. 유효한 값은 0~0.25(25%)입니다. 기본값은 0.05(5%)입니다. 현재 OAUTHBEARER에만 적용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | double |
| 기본값 | 0.05 |
| 유효한 값 | [0.0,...,0.25] |
| 중요도 | low |

---

#### sasl.login.retry.backoff.max.ms

외부 인증 제공자에 대한 로그인 시도 사이의 최대 대기 시간입니다. 로그인은 `sasl.login.retry.backoff.ms`를 기반으로 지수 백오프 알고리즘을 사용하며, 시도 간격을 두 배씩 늘려 최대 `sasl.login.retry.backoff.max.ms`까지 증가시킵니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 10000 (10초) |
| 유효한 값 | - |
| 중요도 | low |

---

#### sasl.login.retry.backoff.ms

외부 인증 제공자에 대한 로그인 시도 사이의 초기 대기 시간입니다. 로그인은 이 값을 기반으로 지수 백오프 알고리즘을 사용하며, 시도 간격을 두 배씩 늘려 최대 `sasl.login.retry.backoff.max.ms`까지 증가시킵니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 100 |
| 유효한 값 | - |
| 중요도 | low |

---

#### sasl.oauthbearer.clock.skew.seconds

OAuth/OIDC ID 제공자와 브로커 간의 시간 차이를 허용하는 값(초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 30 |
| 유효한 값 | - |
| 중요도 | low |

---

#### sasl.oauthbearer.expected.audience

JWT가 예상 대상에게 발행되었는지 브로커가 확인하기 위한 쉼표 구분 설정입니다. 표준 OAuth "aud" 클레임을 검사하며, 이 값이 설정된 경우 JWT의 "aud" 클레임이 예상 대상 중 하나와 일치하는지 확인합니다. 일치하지 않으면 브로커가 JWT를 거부합니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | null |
| 유효한 값 | - |
| 중요도 | low |

---

#### sasl.oauthbearer.expected.issuer

JWT가 예상 발행자에 의해 생성되었는지 브로커가 확인하기 위한 설정입니다. 표준 OAuth "iss" 클레임을 검사하며, 이 값이 설정된 경우 JWT의 "iss" 클레임과 정확히 일치하는지 비교합니다. 일치하지 않으면 브로커가 JWT를 거부합니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효한 값 | - |
| 중요도 | low |

---

#### sasl.oauthbearer.jwks.endpoint.refresh.ms

JWT 서명 키를 포함하는 JWKS(JSON Web Key Set) 캐시를 갱신하는 주기(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 3600000 (1시간) |
| 유효한 값 | - |
| 중요도 | low |

---

#### sasl.oauthbearer.jwks.endpoint.retry.backoff.max.ms

인증 제공자로부터 JWKS(JSON Web Key Set)를 검색하는 시도 사이의 최대 대기 시간입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 10000 (10초) |
| 유효한 값 | - |
| 중요도 | low |

---

#### sasl.oauthbearer.jwks.endpoint.retry.backoff.ms

인증 제공자로부터 JWKS(JSON Web Key Set)를 검색하는 시도 사이의 초기 대기 시간입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 100 |
| 유효한 값 | - |
| 중요도 | low |

---

#### sasl.oauthbearer.scope.claim.name

JWT 페이로드에서 범위에 대한 OAuth 클레임 이름입니다. 기본값은 OAuth 사양을 따르는 scope입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | scope |
| 유효한 값 | - |
| 중요도 | low |

---

#### sasl.oauthbearer.sub.claim.name

JWT 페이로드에서 주체에 대한 OAuth 클레임 이름입니다. 기본값은 OAuth 사양을 따르는 sub입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | sub |
| 유효한 값 | - |
| 중요도 | low |

---

#### security.providers

보안 알고리즘 구현 제공자를 반환하는 구성 가능한 생성자 클래스 목록입니다. 이 클래스들은 `org.apache.kafka.common.security.auth.SecurityProviderCreator` 인터페이스를 구현해야 합니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효한 값 | - |
| 중요도 | low |

---

#### ssl.cipher.suites

TLS/SSL 네트워크 프로토콜 협상에 사용되는 암호 스위트 목록입니다. 암호 스위트는 인증, 암호화, MAC, 키 교환 알고리즘의 명명된 조합입니다. 기본적으로 사용 가능한 모든 암호 스위트가 지원됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | null |
| 유효한 값 | - |
| 중요도 | low |

---

#### ssl.endpoint.identification.algorithm

서버 인증서를 사용하여 서버 호스트 이름을 검증하는 엔드포인트 식별 알고리즘입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | https |
| 유효한 값 | - |
| 중요도 | low |

---

#### ssl.engine.factory.class

`SSLEngine` 객체를 제공하는 `org.apache.kafka.common.security.auth.SslEngineFactory` 타입의 클래스입니다. 기본값은 `org.apache.kafka.common.security.ssl.DefaultSslEngineFactory`입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 유효한 값 | - |
| 중요도 | low |

---

#### ssl.keymanager.algorithm

SSL 연결에서 키 관리자 팩토리가 사용하는 알고리즘입니다. 기본값은 JVM에 구성된 키 관리자 팩토리 알고리즘입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | SunX509 |
| 유효한 값 | - |
| 중요도 | low |

---

#### ssl.secure.random.implementation

SSL 암호화 작업에 사용할 SecureRandom PRNG 구현입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효한 값 | - |
| 중요도 | low |

---

#### ssl.trustmanager.algorithm

SSL 연결에서 신뢰 관리자 팩토리가 사용하는 알고리즘입니다. 기본값은 JVM에 구성된 신뢰 관리자 팩토리 알고리즘입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | PKIX |
| 유효한 값 | - |
| 중요도 | low |

---

#### transaction.timeout.ms

트랜잭션이 중단되기 전에 열린 상태로 유지될 수 있는 최대 시간입니다. 이 시간이 경과하도록 프로듀서가 트랜잭션을 중단하지 않으면, 트랜잭션 코디네이터가 선제적으로 트랜잭션을 중단합니다. 이 값은 브로커에 설정된 `transaction.max.timeout.ms`를 초과할 수 없습니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 60000 (1분) |
| 유효한 값 | - |
| 중요도 | low |

---

#### transactional.id

트랜잭션 전달에 사용할 TransactionalId입니다. 클라이언트가 새 트랜잭션을 시작하기 전에 동일한 TransactionalId를 사용하는 기존 트랜잭션이 완료되도록 보장하므로, 여러 프로듀서 세션에 걸친 신뢰성 있는 시맨틱이 가능합니다. TransactionalId가 설정되지 않으면 프로듀서는 멱등 전달로 제한됩니다. 트랜잭션이 활성화되면 `enable.idempotence`가 자동으로 활성화됩니다. 기본적으로 TransactionalId는 설정되지 않으며, 이는 트랜잭션을 사용할 수 없음을 의미합니다. 트랜잭션에는 기본적으로 최소 3개의 브로커로 구성된 클러스터가 필요하며, 이는 프로덕션 환경에서 권장되는 구성입니다. 개발 환경에서는 `transaction.state.log.replication.factor` 설정을 조정하여 변경할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효한 값 | 비어 있지 않은 문자열 |
| 중요도 | low |

---

### 참고 자료

- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Kafka Producer Configs](https://kafka.apache.org/documentation/#producerconfigs)
- [Kafka Producer API](https://kafka.apache.org/documentation/#producerapi)

---

## Kafka 컨슈머 설정

> 원본: https://kafka.apache.org/documentation/#consumerconfigs

각 설정은 중요도(Importance)에 따라 High, Medium, Low로 분류되어 있습니다.

---

### High 중요도 설정

#### bootstrap.servers

Kafka 클러스터에 대한 초기 연결을 설정하는 데 사용되는 호스트/포트 쌍 목록입니다. 클라이언트는 이 목록에 지정된 서버 중 하나를 사용하여 전체 클러스터 멤버십을 검색합니다. 이 목록은 모든 서버를 포함할 필요가 없으며, 클라이언트는 초기 연결 후 전체 브로커 세트를 자동으로 검색합니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | "" |
| 유효값 | 비어 있지 않은 문자열 |
| 중요도 | high |

---

#### key.deserializer

`org.apache.kafka.common.serialization.Deserializer` 인터페이스를 구현하는 키 역직렬화 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | (없음) |
| 유효값 | (any) |
| 중요도 | high |

---

#### value.deserializer

`org.apache.kafka.common.serialization.Deserializer` 인터페이스를 구현하는 값 역직렬화 클래스입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | (없음) |
| 유효값 | (any) |
| 중요도 | high |

---

#### fetch.min.bytes

서버가 fetch 요청에 대해 반환해야 하는 최소 데이터 양입니다. 데이터가 충분하지 않으면 요청은 충분한 데이터가 누적될 때까지 대기합니다. 기본값 1바이트는 데이터가 도착하는 즉시 또는 fetch 요청이 대기 중 타임아웃되면 fetch 요청에 응답함을 의미합니다. 이 값을 1보다 크게 설정하면 서버가 더 많은 데이터를 누적할 때까지 대기하게 되어 대기 시간이 약간 증가하는 대신 서버 처리량이 향상됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 1 |
| 유효값 | [0,...] |
| 중요도 | high |

---

#### group.id

이 컨슈머가 속한 컨슈머 그룹을 식별하는 고유 문자열입니다. 컨슈머가 그룹 관리(`subscribe(topic)` 사용) 또는 Kafka 기반 오프셋 관리(`commitSync` 또는 `commitAsync` 사용)를 사용하는 경우 이 속성이 필요합니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | high |

---

#### group.protocol

그룹 관리에 사용할 프로토콜입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | classic |
| 유효값 | [CONSUMER, CLASSIC] |
| 중요도 | high |

---

#### heartbeat.interval.ms

그룹 관리 기능을 사용할 때 그룹 코디네이터에 대한 하트비트 간 예상 시간입니다. 하트비트는 컨슈머의 세션이 활성 상태임을 보장하고 새 멤버가 그룹에 참여하거나 탈퇴할 때 리밸런싱을 용이하게 합니다. 이 값은 `session.timeout.ms`보다 낮게 설정해야 하며, 일반적으로 해당 값의 1/3 이하로 설정해야 합니다. 예상 리밸런싱 시간을 조절하기 위해 더 낮게 설정할 수 있습니다. 참고: classic 그룹 프로토콜에만 적용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 3000 (3초) |
| 유효값 | (any) |
| 중요도 | high |

---

#### max.partition.fetch.bytes

서버가 파티션당 반환하는 최대 데이터 양입니다. 레코드는 컨슈머에 의해 배치로 가져옵니다. fetch의 첫 번째 비어 있지 않은 파티션의 첫 번째 레코드 배치가 이 제한보다 크면, 컨슈머가 진행할 수 있도록 배치가 여전히 반환됩니다. 브로커가 허용하는 최대 레코드 배치 크기는 `message.max.bytes`(브로커 설정) 또는 `max.message.bytes`(토픽 설정)를 통해 정의됩니다. 컨슈머 요청 크기를 제한하려면 `fetch.max.bytes`를 참조하세요.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 1048576 (1MB) |
| 유효값 | [0,...] |
| 중요도 | high |

---

#### session.timeout.ms

그룹 관리 기능을 사용할 때 클라이언트 장애를 감지하는 데 사용되는 타임아웃입니다. 클라이언트는 브로커에 주기적으로 하트비트를 전송하여 활성 상태임을 알립니다. 이 세션 타임아웃이 만료되기 전에 브로커가 하트비트를 받지 못하면, 브로커는 이 클라이언트를 그룹에서 제거하고 리밸런싱을 시작합니다. 이 값은 브로커 설정 `group.min.session.timeout.ms`와 `group.max.session.timeout.ms` 범위 내에 있어야 합니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 45000 (45초) |
| 유효값 | (any) |
| 중요도 | high |

---

#### ssl.key.password

keystore 파일 내 개인 키의 비밀번호 또는 `ssl.keystore.key`에 지정된 PEM 키의 비밀번호입니다.

| 속성 | 값 |
|------|-----|
| 타입 | password |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | high |

---

#### ssl.keystore.certificate.chain

`ssl.keystore.type`에 지정된 형식의 인증서 체인입니다. 기본 SSL 엔진 팩토리는 X.509 인증서 목록이 포함된 PEM 형식만 지원합니다.

| 속성 | 값 |
|------|-----|
| 타입 | password |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | high |

---

#### ssl.keystore.key

`ssl.keystore.type`에 지정된 형식의 개인 키입니다. 기본 SSL 엔진 팩토리는 PKCS#8 키가 포함된 PEM 형식만 지원합니다. 키가 암호화된 경우 `ssl.key.password`를 사용하여 키 비밀번호를 지정해야 합니다.

| 속성 | 값 |
|------|-----|
| 타입 | password |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | high |

---

#### ssl.keystore.location

keystore 파일의 위치입니다. 클라이언트에서는 선택 사항이며 양방향 인증에 사용할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | high |

---

#### ssl.keystore.password

keystore 파일의 저장소 비밀번호입니다. 클라이언트에서는 선택 사항이며 `ssl.keystore.location`이 설정된 경우에만 필요합니다. PEM 형식에서는 keystore 비밀번호가 지원되지 않습니다.

| 속성 | 값 |
|------|-----|
| 타입 | password |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | high |

---

#### ssl.truststore.certificates

`ssl.truststore.type`에 지정된 형식의 신뢰할 수 있는 인증서입니다. 기본 SSL 엔진 팩토리는 X.509 인증서가 포함된 PEM 형식만 지원합니다.

| 속성 | 값 |
|------|-----|
| 타입 | password |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | high |

---

#### ssl.truststore.location

trust store 파일의 위치입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | high |

---

#### ssl.truststore.password

trust store 파일의 비밀번호입니다. 비밀번호가 설정되지 않으면 설정된 trust store 파일이 여전히 사용되지만 무결성 검사가 비활성화됩니다. PEM 형식에서는 truststore 비밀번호가 지원되지 않습니다.

| 속성 | 값 |
|------|-----|
| 타입 | password |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | high |

---

### Medium 중요도 설정

#### allow.auto.create.topics

토픽을 구독하거나 할당할 때 브로커에서 자동으로 토픽을 생성할 수 있도록 허용합니다. 브로커의 `auto.create.topics.enable` 설정이 true인 경우에만 자동 생성됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | boolean |
| 기본값 | true |
| 유효값 | (any) |
| 중요도 | medium |

---

#### auto.offset.reset

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

#### client.dns.lookup

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

#### connections.max.idle.ms

이 설정에 지정된 시간(밀리초) 동안 유휴 상태인 연결을 닫습니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 540000 (9분) |
| 유효값 | (any) |
| 중요도 | medium |

---

#### default.api.timeout.ms

명시적인 타임아웃 매개변수를 지정하지 않는 클라이언트 API의 기본 타임아웃을 지정합니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 60000 (60초) |
| 유효값 | [0,...] |
| 중요도 | medium |

---

#### enable.auto.commit

true인 경우 컨슈머의 오프셋이 백그라운드에서 주기적으로 자동 커밋됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | boolean |
| 기본값 | true |
| 유효값 | (any) |
| 중요도 | medium |

---

#### exclude.internal.topics

구독된 패턴과 일치하는 내부 토픽을 구독에서 제외할지 여부입니다. 내부 토픽을 명시적으로 구독할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | boolean |
| 기본값 | true |
| 유효값 | (any) |
| 중요도 | medium |

---

#### fetch.max.bytes

서버가 fetch 요청에 대해 반환해야 하는 최대 데이터 양입니다. 레코드는 컨슈머에 의해 배치로 가져오며, fetch의 첫 번째 비어 있지 않은 파티션의 첫 번째 레코드 배치가 이 값보다 크면 컨슈머가 진행할 수 있도록 레코드 배치가 여전히 반환됩니다. 따라서 이것은 절대적인 최대값이 아닙니다. 브로커가 허용하는 최대 레코드 배치 크기는 `message.max.bytes`(브로커 설정) 또는 `max.message.bytes`(토픽 설정)를 통해 정의됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 52428800 (50MB) |
| 유효값 | [0,...] |
| 중요도 | medium |

---

#### group.instance.id

엔드 유저가 제공하는 컨슈머 인스턴스의 고유 식별자입니다. 비어 있지 않은 문자열만 허용됩니다. 설정된 경우 컨슈머는 정적 멤버로 처리되어 한 번에 하나의 인스턴스만 이 ID로 그룹에 참여할 수 있습니다. 이는 더 큰 세션 타임아웃과 함께 사용하여 일시적인 가용성 저하(예: 프로세스 재시작)로 인한 그룹 리밸런싱을 방지할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효값 | 비어 있지 않은 문자열 |
| 중요도 | medium |

---

#### group.remote.assignor

서버 측 할당자의 이름입니다. `group.protocol`이 "consumer"로 설정된 경우에만 적용됩니다. 설정되지 않은 경우 그룹 코디네이터가 할당자를 선택합니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | medium |

---

#### isolation.level

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

#### max.poll.interval.ms

그룹 관리를 사용할 때 `poll()` 호출 사이의 최대 지연입니다. 이것은 컨슈머가 더 많은 레코드를 가져오기 전에 유휴 상태일 수 있는 시간의 상한을 설정합니다. 이 타임아웃이 만료되기 전에 `poll()`이 호출되지 않으면 컨슈머가 실패한 것으로 간주되고 그룹은 파티션을 다른 멤버에게 재할당하기 위해 리밸런싱합니다. 이 타임아웃에 도달한 `group.instance.id`가 null이 아닌 컨슈머의 경우, 파티션이 즉시 재할당되지 않습니다. 대신, 컨슈머는 하트비트 전송을 중지하고 `session.timeout.ms` 만료 후 파티션이 재할당됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 300000 (5분) |
| 유효값 | [1,...] |
| 중요도 | medium |

---

#### max.poll.records

`poll()` 호출에서 반환되는 최대 레코드 수입니다. 이것은 기본 fetch 동작에 영향을 미치지 않습니다. 컨슈머는 각 fetch 요청에서 레코드를 캐시하고 `poll()`에서 점진적으로 반환합니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 500 |
| 유효값 | [1,...] |
| 중요도 | medium |

---

#### partition.assignment.strategy

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

#### receive.buffer.bytes

데이터를 읽을 때 사용할 TCP 수신 버퍼(SO_RCVBUF) 크기입니다. 값이 -1이면 OS 기본값이 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 65536 (64KB) |
| 유효값 | [-1,...] |
| 중요도 | medium |

---

#### request.timeout.ms

클라이언트가 요청의 응답을 기다리는 최대 시간입니다. 타임아웃이 경과하기 전에 응답이 수신되지 않으면 클라이언트는 필요한 경우 요청을 재전송하거나 재시도가 소진되면 요청을 실패 처리합니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 30000 (30초) |
| 유효값 | [0,...] |
| 중요도 | medium |

---

#### sasl.client.callback.handler.class

AuthenticateCallbackHandler 인터페이스를 구현하는 SASL 클라이언트 콜백 핸들러 클래스의 정규화된 이름입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | medium |

---

#### sasl.jaas.config

JAAS 구성 파일에서 사용되는 형식의 SASL 연결에 대한 JAAS 로그인 컨텍스트 매개변수입니다. JAAS 구성 파일 형식은 Oracle의 문서에 설명되어 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | password |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | medium |

---

#### sasl.kerberos.service.name

Kafka가 실행되는 Kerberos principal 이름입니다. 이는 Kafka의 JAAS 구성 또는 Kafka의 구성에서 정의할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | medium |

---

#### sasl.login.callback.handler.class

AuthenticateCallbackHandler 인터페이스를 구현하는 SASL 로그인 콜백 핸들러 클래스의 정규화된 이름입니다. 브로커의 경우 로그인 콜백 핸들러 구성은 리스너 접두사와 소문자 SASL 메커니즘 이름으로 접두사가 붙어야 합니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | medium |

---

#### sasl.login.class

Login 인터페이스를 구현하는 클래스의 정규화된 이름입니다. 브로커의 경우 로그인 구성은 리스너 접두사와 소문자 SASL 메커니즘 이름으로 접두사가 붙어야 합니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | medium |

---

#### sasl.mechanism

클라이언트 연결에 사용되는 SASL 메커니즘입니다. 보안 공급자가 사용 가능한 모든 메커니즘이 될 수 있습니다. GSSAPI가 기본 메커니즘입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | GSSAPI |
| 유효값 | (any) |
| 중요도 | medium |

---

#### sasl.oauthbearer.jwks.endpoint.url

OAuth/OIDC 공급자의 JWKS(JSON Web Key Set) 검색 URL입니다. HTTP(S) 기반 또는 파일 기반 URL이 될 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | medium |

---

#### sasl.oauthbearer.token.endpoint.url

OAuth/OIDC ID 공급자의 토큰 엔드포인트 URL 또는 액세스 토큰이 포함된 파일의 URL입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | medium |

---

#### security.protocol

브로커와 통신하는 데 사용되는 프로토콜입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | PLAINTEXT |
| 유효값 | [PLAINTEXT, SSL, SASL_PLAINTEXT, SASL_SSL] |
| 중요도 | medium |

---

#### send.buffer.bytes

데이터를 보낼 때 사용할 TCP 송신 버퍼(SO_SNDBUF) 크기입니다. 값이 -1이면 OS 기본값이 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 131072 (128KB) |
| 유효값 | [-1,...] |
| 중요도 | medium |

---

#### socket.connection.setup.timeout.max.ms

클라이언트가 소켓 연결이 설정될 때까지 기다리는 최대 시간입니다. 연결 설정 타임아웃은 연속적인 연결 실패에 대해 지수적으로 증가합니다. 연결 폭풍을 방지하기 위해 타임아웃에 0.2의 무작위 인수가 적용되어 계산된 값보다 20% 낮은 값에서 20% 높은 값 사이의 무작위 범위가 됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 30000 (30초) |
| 유효값 | (any) |
| 중요도 | medium |

---

#### socket.connection.setup.timeout.ms

클라이언트가 소켓 연결이 설정될 때까지 기다리는 시간입니다. 연결이 설정되기 전에 타임아웃이 경과하면 클라이언트는 소켓 채널을 닫습니다. 이 값은 지수 백오프의 초기 백오프 값이며 `socket.connection.setup.timeout.max.ms` 값까지 증가합니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 10000 (10초) |
| 유효값 | (any) |
| 중요도 | medium |

---

#### ssl.enabled.protocols

SSL 연결에 대해 활성화된 프로토콜 목록입니다. 기본값은 Java 11 이상에서 실행할 때 'TLSv1.2,TLSv1.3'이고, 그렇지 않으면 'TLSv1.2'입니다. Java 11의 기본값에서는 클라이언트와 서버가 모두 지원하는 경우 TLSv1.3을 선호하고, 그렇지 않으면 TLSv1.2로 폴백합니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | TLSv1.2,TLSv1.3 |
| 유효값 | (any) |
| 중요도 | medium |

---

#### ssl.keystore.type

keystore 파일의 파일 형식입니다. 클라이언트에서는 선택 사항입니다. 사용 가능한 값: JKS, PKCS12, PEM.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | JKS |
| 유효값 | (any) |
| 중요도 | medium |

---

#### ssl.protocol

SSLContext를 생성하는 데 사용되는 SSL 프로토콜입니다. 기본값은 Java 11 이상에서 실행할 때 'TLSv1.3'이고, 그렇지 않으면 'TLSv1.2'입니다. 이 값은 대부분의 사용 사례에 적합합니다. 최신 JVM에서 허용되는 값은 'TLSv1.2' 및 'TLSv1.3'입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | TLSv1.3 |
| 유효값 | (any) |
| 중요도 | medium |

---

#### ssl.provider

SSL 연결에 사용되는 보안 공급자의 이름입니다. 기본값은 JVM의 기본 보안 공급자입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | medium |

---

#### ssl.truststore.type

truststore 파일의 파일 형식입니다. 사용 가능한 값: JKS, PKCS12, PEM.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | JKS |
| 유효값 | (any) |
| 중요도 | medium |

---

### Low 중요도 설정

#### auto.commit.interval.ms

`enable.auto.commit`이 true로 설정된 경우 컨슈머 오프셋이 Kafka에 자동 커밋되는 빈도(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 5000 (5초) |
| 유효값 | [0,...] |
| 중요도 | low |

---

#### check.crcs

소비된 레코드의 CRC32를 자동으로 확인합니다. 이것은 메시지의 on-the-wire 또는 on-disk 손상이 발생하지 않았는지 확인합니다. 이 확인은 약간의 오버헤드를 추가하므로 극단적인 성능을 추구하는 경우 비활성화할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | boolean |
| 기본값 | true |
| 유효값 | (any) |
| 중요도 | low |

---

#### client.id

요청을 할 때 서버에 전달할 ID 문자열입니다. 서버 측 요청 로그에 논리적 애플리케이션 이름을 포함시켜 IP/포트 외에도 요청 출처를 추적할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | "" |
| 유효값 | (any) |
| 중요도 | low |

---

#### client.rack

이 클라이언트의 랙 식별자입니다. 브로커 설정의 `broker.rack` 값에 대응하는 문자열로, 이 컨슈머가 위치한 랙을 나타냅니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | "" |
| 유효값 | (any) |
| 중요도 | low |

---

#### enable.metrics.push

클라이언트 메트릭 구독과 일치하는 구독이 있는 경우 클러스터에 클라이언트 메트릭을 푸시할 수 있도록 합니다.

| 속성 | 값 |
|------|-----|
| 타입 | boolean |
| 기본값 | true |
| 유효값 | (any) |
| 중요도 | low |

---

#### fetch.max.wait.ms

`fetch.min.bytes`로 지정된 요구 사항을 즉시 충족하기에 데이터가 충분하지 않은 경우 fetch 요청에 응답하기 전에 서버가 차단하는 최대 시간입니다. 이것은 로컬 로그에서 가져오는 데에만 적용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 500 |
| 유효값 | [0,...] |
| 중요도 | low |

---

#### interceptor.classes

인터셉터로 사용할 클래스 목록입니다. `org.apache.kafka.clients.consumer.ConsumerInterceptor` 인터페이스를 구현하면 컨슈머가 수신하는 레코드를 가로채거나 변경할 수 있습니다. 기본적으로 인터셉터는 없습니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | "" |
| 유효값 | 비어 있지 않은 문자열 |
| 중요도 | low |

---

#### metadata.max.age.ms

파티션 리더십 변경이 없더라도 메타데이터를 강제로 갱신하는 주기(밀리초)입니다. 이를 통해 새 브로커나 파티션을 사전에 감지할 수 있습니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 300000 (5분) |
| 유효값 | [0,...] |
| 중요도 | low |

---

#### metadata.recovery.rebootstrap.trigger.ms

클라이언트가 알려진 모든 브로커에서 메타데이터를 얻을 수 없는 경우, 클라이언트가 부트스트랩 주소를 사용하여 브로커를 (재)검색하기 전까지의 기간입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 300000 (5분) |
| 유효값 | [0,...] |
| 중요도 | low |

---

#### metadata.recovery.strategy

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

#### metric.reporters

메트릭 리포터로 사용할 클래스 목록입니다. `org.apache.kafka.common.metrics.MetricsReporter` 인터페이스를 구현하면 새 메트릭 생성 시 알림을 받는 클래스를 플러그인할 수 있습니다. JmxReporter는 기본으로 포함되어 JMX 통계를 등록합니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | org.apache.kafka.common.metrics.JmxReporter |
| 유효값 | 비어 있지 않은 문자열 |
| 중요도 | low |

---

#### metrics.num.samples

메트릭 계산을 위해 유지되는 샘플 수입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 2 |
| 유효값 | [1,...] |
| 중요도 | low |

---

#### metrics.recording.level

메트릭의 가장 높은 기록 수준입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | INFO |
| 유효값 | [INFO, DEBUG, TRACE] |
| 중요도 | low |

---

#### metrics.sample.window.ms

메트릭 샘플이 계산되는 시간 창입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 30000 (30초) |
| 유효값 | [0,...] |
| 중요도 | low |

---

#### reconnect.backoff.max.ms

반복적으로 연결에 실패한 브로커에 다시 연결할 때 대기하는 최대 시간(밀리초)입니다. 제공되면 호스트별 백오프가 연속적으로 연결에 실패할 때마다 지수적으로 증가합니다. 연결 폭풍을 방지하기 위해 백오프 증가에 20%의 무작위 지터가 추가됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 1000 (1초) |
| 유효값 | [0,...] |
| 중요도 | low |

---

#### reconnect.backoff.ms

주어진 호스트에 재연결을 시도하기 전에 대기하는 기본 시간입니다. 짧은 간격으로 반복 연결하는 것을 방지하며, 클라이언트가 브로커에 보내는 모든 연결 시도에 적용됩니다. 이 값은 지수 백오프의 초기값이며 `reconnect.backoff.max.ms` 값까지 증가합니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 50 |
| 유효값 | [0,...] |
| 중요도 | low |

---

#### retry.backoff.max.ms

주어진 토픽 파티션에 실패한 요청을 재시도할 때 대기하는 최대 시간(밀리초)입니다. 제공되면 파티션별 백오프가 연속적으로 요청이 실패할 때마다 지수적으로 증가합니다. 재시도 폭풍을 방지하기 위해 백오프 증가에 20%의 무작위 지터가 추가됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 1000 (1초) |
| 유효값 | [0,...] |
| 중요도 | low |

---

#### retry.backoff.ms

실패한 요청을 재시도하기 전에 대기하는 시간입니다. 일부 실패 시나리오에서 짧은 간격으로 요청을 반복하는 것을 방지합니다. 이 값은 지수 백오프의 초기값이며 `retry.backoff.max.ms` 값까지 증가합니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 100 |
| 유효값 | [0,...] |
| 중요도 | low |

---

#### sasl.kerberos.kinit.cmd

Kerberos kinit 명령 경로입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | /usr/bin/kinit |
| 유효값 | (any) |
| 중요도 | low |

---

#### sasl.kerberos.min.time.before.relogin

새로 고침 시도 사이의 로그인 스레드 대기 시간입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 60000 (60초) |
| 유효값 | (any) |
| 중요도 | low |

---

#### sasl.kerberos.ticket.renew.jitter

갱신 시간에 추가되는 무작위 지터의 백분율입니다.

| 속성 | 값 |
|------|-----|
| 타입 | double |
| 기본값 | 0.05 |
| 유효값 | (any) |
| 중요도 | low |

---

#### sasl.kerberos.ticket.renew.window.factor

로그인 스레드는 마지막 갱신 시점부터 티켓 만료까지 남은 시간에 지정된 창 비율(window factor)을 곱한 시점이 되면 대기 상태로 들어가며, 해당 시점에 티켓 갱신을 시도합니다.

| 속성 | 값 |
|------|-----|
| 타입 | double |
| 기본값 | 0.8 |
| 유효값 | (any) |
| 중요도 | low |

---

#### sasl.login.connect.timeout.ms

외부 인증 공급자 연결 타임아웃의 (선택적) 값(밀리초)입니다. 현재 OAUTHBEARER에만 적용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | low |

---

#### sasl.login.read.timeout.ms

외부 인증 공급자 읽기 타임아웃의 (선택적) 값(밀리초)입니다. 현재 OAUTHBEARER에만 적용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | low |

---

#### sasl.login.refresh.buffer.seconds

자격 증명을 새로 고침할 때 자격 증명이 만료되기 전에 유지할 버퍼 시간(초)입니다. 새로 고침이 버퍼 초 수보다 가깝게 발생하면 버퍼 시간만큼 일찍 만료되도록 새로 고침이 이동됩니다. 유효값은 0에서 3600(1시간) 사이입니다. 값이 지정되지 않으면 기본값 300(5분)이 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | short |
| 기본값 | 300 |
| 유효값 | [0,...,3600] |
| 중요도 | low |

---

#### sasl.login.refresh.min.period.seconds

로그인 새로 고침 스레드가 자격 증명을 새로 고치기 전에 대기하는 최소 시간(초)입니다. 유효값은 0에서 900(15분) 사이입니다. 값이 지정되지 않으면 기본값 60(1분)이 사용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | short |
| 기본값 | 60 |
| 유효값 | [0,...,900] |
| 중요도 | low |

---

#### sasl.login.refresh.window.factor

로그인 새로 고침 스레드는 자격 증명 수명을 기준으로 지정된 창 인수에 도달하면 자격 증명을 새로 고치려고 시도합니다. 유효값은 0.5(50%)에서 1.0(100%) 사이입니다. 값이 지정되지 않으면 기본값 0.8(80%)이 사용됩니다. 현재 OAUTHBEARER에만 적용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | double |
| 기본값 | 0.8 |
| 유효값 | [0.5,...,1.0] |
| 중요도 | low |

---

#### sasl.login.refresh.window.jitter

로그인 새로 고침 스레드의 대기 시간에 추가되는 최대 무작위 지터입니다. 유효값은 0에서 0.25(25%) 사이입니다. 값이 지정되지 않으면 기본값 0.05(5%)가 사용됩니다. 현재 OAUTHBEARER에만 적용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | double |
| 기본값 | 0.05 |
| 유효값 | [0.0,...,0.25] |
| 중요도 | low |

---

#### sasl.login.retry.backoff.max.ms

외부 인증 공급자에 대한 로그인 시도 사이의 최대 대기 시간(밀리초)입니다. 로그인은 지수 백오프 알고리즘을 사용하며 초기 대기는 `sasl.login.retry.backoff.ms` 설정에 기반하고 시도 사이에 두 배가 되어 `sasl.login.retry.backoff.max.ms` 설정에 지정된 최대 대기 길이까지 증가합니다. 현재 OAUTHBEARER에만 적용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 10000 (10초) |
| 유효값 | (any) |
| 중요도 | low |

---

#### sasl.login.retry.backoff.ms

외부 인증 공급자에 대한 로그인 시도 사이의 초기 대기 시간(밀리초)입니다. 로그인은 지수 백오프 알고리즘을 사용하며 초기 대기는 `sasl.login.retry.backoff.ms` 설정에 기반하고 시도 사이에 두 배가 되어 `sasl.login.retry.backoff.max.ms` 설정에 지정된 최대 대기 길이까지 증가합니다. 현재 OAUTHBEARER에만 적용됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 100 |
| 유효값 | (any) |
| 중요도 | low |

---

#### sasl.oauthbearer.clock.skew.seconds

OAuth/OIDC ID 공급자와 브로커 사이의 허용 가능한 시간 차이(초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | int |
| 기본값 | 30 |
| 유효값 | (any) |
| 중요도 | low |

---

#### sasl.oauthbearer.expected.audience

브로커가 JWT가 발급된 예상 대상 중 하나로 확인할 쉼표로 구분된 대상 목록입니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | low |

---

#### sasl.oauthbearer.expected.issuer

브로커가 JWT가 발급된 예상 발급자로 확인할 발급자입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | low |

---

#### sasl.oauthbearer.header.urlencode

RFC6749에 따라 OAuth 클라이언트 자격 증명을 authorization 헤더에서 URL 인코딩할지 여부입니다.

| 속성 | 값 |
|------|-----|
| 타입 | boolean |
| 기본값 | false |
| 유효값 | (any) |
| 중요도 | low |

---

#### sasl.oauthbearer.jwks.endpoint.refresh.ms

브로커가 JWT 서명을 확인하기 위해 키가 포함된 JWKS(JSON Web Key Set) 캐시를 새로 고치기 전에 대기하는 시간(밀리초)입니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 3600000 (1시간) |
| 유효값 | (any) |
| 중요도 | low |

---

#### sasl.oauthbearer.jwks.endpoint.retry.backoff.max.ms

외부 인증 공급자의 JWKS(JSON Web Key Set) 검색 시도 사이의 최대 대기 시간(밀리초)입니다. JWKS 검색은 지수 백오프 알고리즘을 사용합니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 10000 (10초) |
| 유효값 | (any) |
| 중요도 | low |

---

#### sasl.oauthbearer.jwks.endpoint.retry.backoff.ms

외부 인증 공급자의 JWKS(JSON Web Key Set) 검색 시도 사이의 초기 대기 시간(밀리초)입니다. JWKS 검색은 지수 백오프 알고리즘을 사용합니다.

| 속성 | 값 |
|------|-----|
| 타입 | long |
| 기본값 | 100 |
| 유효값 | (any) |
| 중요도 | low |

---

#### sasl.oauthbearer.scope.claim.name

OAuth 범위를 찾을 클레임 이름입니다. 기본값은 "scope"입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | scope |
| 유효값 | (any) |
| 중요도 | low |

---

#### sasl.oauthbearer.sub.claim.name

주체를 찾을 OAuth 클레임 이름입니다. 기본값은 "sub"입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | sub |
| 유효값 | (any) |
| 중요도 | low |

---

#### security.providers

보안 알고리즘을 구현하는 공급자를 반환하는 구성 가능한 생성자 클래스 목록입니다. 이러한 클래스는 `org.apache.kafka.common.security.auth.SecurityProviderCreator` 인터페이스를 구현해야 합니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | low |

---

#### ssl.cipher.suites

암호 스위트 목록입니다. 이것은 TLS 또는 SSL 네트워크 프로토콜을 사용하여 네트워크 연결의 보안 설정을 협상하는 데 사용되는 인증, 암호화, MAC 및 키 교환 알고리즘의 명명된 조합입니다. 기본적으로 사용 가능한 모든 암호 스위트가 지원됩니다.

| 속성 | 값 |
|------|-----|
| 타입 | list |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | low |

---

#### ssl.endpoint.identification.algorithm

서버 인증서를 사용하여 서버 호스트 이름을 검증하는 엔드포인트 식별 알고리즘입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | https |
| 유효값 | (any) |
| 중요도 | low |

---

#### ssl.engine.factory.class

SSLEngine 객체를 제공하기 위한 org.apache.kafka.common.security.auth.SslEngineFactory 타입의 클래스입니다. 기본값은 org.apache.kafka.common.security.ssl.DefaultSslEngineFactory입니다.

| 속성 | 값 |
|------|-----|
| 타입 | class |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | low |

---

#### ssl.keymanager.algorithm

SSL 연결에 대해 키 관리자 팩토리가 사용하는 알고리즘입니다. 기본값은 Java Virtual Machine에 대해 구성된 키 관리자 팩토리 알고리즘입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | SunX509 |
| 유효값 | (any) |
| 중요도 | low |

---

#### ssl.secure.random.implementation

SSL 암호화 작업에 사용할 SecureRandom PRNG 구현입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | null |
| 유효값 | (any) |
| 중요도 | low |

---

#### ssl.trustmanager.algorithm

SSL 연결에 대해 신뢰 관리자 팩토리가 사용하는 알고리즘입니다. 기본값은 Java Virtual Machine에 대해 구성된 신뢰 관리자 팩토리 알고리즘입니다.

| 속성 | 값 |
|------|-----|
| 타입 | string |
| 기본값 | PKIX |
| 유효값 | (any) |
| 중요도 | low |

---

### 주요 설정 사용 예시

#### 기본 컨슈머 설정

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

#### 성능 튜닝 설정

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

#### SSL 보안 설정

```java
props.put("security.protocol", "SSL");
props.put("ssl.truststore.location", "/path/to/truststore.jks");
props.put("ssl.truststore.password", "truststore-password");
props.put("ssl.keystore.location", "/path/to/keystore.jks");
props.put("ssl.keystore.password", "keystore-password");
props.put("ssl.key.password", "key-password");
```

#### SASL 인증 설정

```java
props.put("security.protocol", "SASL_SSL");
props.put("sasl.mechanism", "PLAIN");
props.put("sasl.jaas.config",
    "org.apache.kafka.common.security.plain.PlainLoginModule required " +
    "username=\"user\" password=\"password\";");
```

---

### 참고 자료

- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Consumer Configuration](https://kafka.apache.org/documentation/#consumerconfigs)
- [Kafka Consumer API](https://kafka.apache.org/documentation/#consumerapi)

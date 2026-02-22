# Kafka Admin Client 설정

> 이 문서는 Apache Kafka 공식 문서의 "Admin Client Configuration" 섹션을 한국어로 번역한 것입니다.
> 원본 문서: https://kafka.apache.org/documentation/#adminclientconfigs

---

## 목차

1. [개요](#개요)
2. [높은 중요도 설정](#높은-중요도-설정)
3. [중간 중요도 설정](#중간-중요도-설정)
4. [낮은 중요도 설정](#낮은-중요도-설정)

---

## 개요

Admin Client는 Kafka 클러스터를 관리하기 위한 클라이언트입니다. 토픽 생성/삭제, ACL 관리, 설정 변경 등의 관리 작업을 수행할 수 있습니다. 이 문서에서는 Admin Client의 모든 설정 옵션을 설명합니다.

---

## 높은 중요도 설정

### bootstrap.controllers

- 타입: list
- 기본값: ""
- 중요도: high

KRaft 컨트롤러 쿼럼에 초기 연결을 설정하는 데 사용되는 호스트/포트 쌍 목록입니다. 형식은 `host1:port1,host2:port2,...`입니다.

### bootstrap.servers

- 타입: list
- 기본값: ""
- 중요도: high

Kafka 클러스터 검색을 위한 초기 연결 지점입니다. 형식은 `host1:port1,host2:port2,...`입니다. 이 목록은 전체 클러스터 멤버십을 검색하기 위한 초기 호스트로만 사용되며, 동적으로 변경될 수 있습니다. 이 목록에 모든 서버를 포함할 필요는 없지만, 하나의 서버가 다운되었을 경우를 대비하여 최소 두 개 이상을 지정하는 것이 좋습니다.

### ssl.key.password

- 타입: password
- 기본값: null
- 중요도: high

키 저장소 파일 또는 `ssl.keystore.key`에 지정된 PEM 키의 개인 키 비밀번호입니다.

### ssl.keystore.certificate.chain

- 타입: password
- 기본값: null
- 중요도: high

`ssl.keystore.type`에 의해 지정된 형식의 인증서 체인입니다. 기본 SSL 엔진 팩토리는 X.509 인증서 목록이 포함된 PEM 형식만 지원합니다.

### ssl.keystore.key

- 타입: password
- 기본값: null
- 중요도: high

`ssl.keystore.type`에 의해 지정된 형식의 개인 키입니다. 기본 SSL 엔진 팩토리는 PKCS#8 키가 포함된 PEM 형식만 지원합니다. 키가 암호화된 경우 `ssl.key.password`를 사용하여 키 비밀번호를 지정해야 합니다.

### ssl.keystore.location

- 타입: string
- 기본값: null
- 중요도: high

키 저장소 파일의 위치입니다. 클라이언트의 경우 선택 사항이며, 클라이언트의 양방향 인증에 사용할 수 있습니다.

### ssl.keystore.password

- 타입: password
- 기본값: null
- 중요도: high

키 저장소 파일의 저장소 비밀번호입니다. 클라이언트의 경우 선택 사항이며, `ssl.keystore.location`이 구성된 경우에만 필요합니다. PEM 형식에서는 키 저장소 비밀번호가 지원되지 않습니다.

### ssl.truststore.certificates

- 타입: password
- 기본값: null
- 중요도: high

`ssl.truststore.type`에 의해 지정된 형식의 신뢰할 수 있는 인증서입니다. 기본 SSL 엔진 팩토리는 X.509 인증서가 포함된 PEM 형식만 지원합니다.

### ssl.truststore.location

- 타입: string
- 기본값: null
- 중요도: high

신뢰 저장소 파일의 위치입니다.

### ssl.truststore.password

- 타입: password
- 기본값: null
- 중요도: high

신뢰 저장소 파일의 비밀번호입니다. 비밀번호가 설정되지 않은 경우 구성된 신뢰 저장소 파일이 여전히 사용되지만 무결성 검사가 비활성화됩니다. PEM 형식에서는 신뢰 저장소 비밀번호가 지원되지 않습니다.

---

## 중간 중요도 설정

### client.dns.lookup

- 타입: string
- 기본값: use_all_dns_ips
- 유효한 값: [use_all_dns_ips, resolve_canonical_bootstrap_servers_only]
- 중요도: medium

클라이언트가 DNS 조회를 사용하는 방법을 제어합니다. `use_all_dns_ips`로 설정하면 연결이 성공적으로 설정될 때까지 반환된 각 IP 주소에 순서대로 연결합니다. 연결 해제 후 다음 IP가 사용됩니다. 모든 IP가 한 번 사용되면 클라이언트는 호스트 이름에서 IP를 다시 확인합니다. `resolve_canonical_bootstrap_servers_only`로 설정하면 각 부트스트랩 주소를 정규 이름 목록으로 확인합니다. 부트스트랩 단계 후 이는 `use_all_dns_ips`와 동일하게 작동합니다.

### client.id

- 타입: string
- 기본값: ""
- 중요도: medium

요청 시 서버에 전달할 ID 문자열입니다. 이 목적은 서버 측 요청 로깅에 논리적 애플리케이션 이름을 포함하여 IP/포트를 넘어서 요청의 출처를 추적할 수 있도록 하는 것입니다.

### connections.max.idle.ms

- 타입: long
- 기본값: 300000 (5분)
- 중요도: medium

이 설정에 의해 지정된 시간(밀리초) 이후 유휴 연결을 닫습니다.

### default.api.timeout.ms

- 타입: int
- 기본값: 60000 (1분)
- 유효한 값: [0, ...]
- 중요도: medium

클라이언트 API에 대한 기본 타임아웃을 밀리초 단위로 지정합니다. 이 타임아웃은 명시적인 타임아웃 매개변수를 지정하지 않는 모든 클라이언트 작업에 사용됩니다.

### receive.buffer.bytes

- 타입: int
- 기본값: 65536 (64KB)
- 유효한 값: [-1, ...]
- 중요도: medium

데이터를 읽을 때 사용할 TCP 수신 버퍼(SO_RCVBUF)의 크기입니다. 값이 -1이면 OS 기본값이 사용됩니다.

### request.timeout.ms

- 타입: int
- 기본값: 30000 (30초)
- 유효한 값: [0, ...]
- 중요도: medium

클라이언트가 요청 응답을 기다리는 최대 시간을 제어합니다. 타임아웃이 경과하기 전에 응답이 수신되지 않으면 클라이언트는 필요한 경우 요청을 재전송하거나 재시도가 소진되면 요청을 실패 처리합니다.

### sasl.client.callback.handler.class

- 타입: class
- 기본값: null
- 중요도: medium

AuthenticateCallbackHandler 인터페이스를 구현하는 SASL 클라이언트 콜백 핸들러 클래스의 정규화된 이름입니다.

### sasl.jaas.config

- 타입: password
- 기본값: null
- 중요도: medium

JAAS 구성 파일에서 사용되는 형식의 SASL 연결에 대한 JAAS 로그인 컨텍스트 매개변수입니다. JAAS 구성 파일 형식은 여기에 설명되어 있습니다. 값의 형식은 `loginModuleClass controlFlag (optionName=optionValue)*;`입니다. 브로커의 경우 구성 앞에 리스너 접두사와 소문자 SASL 메커니즘 이름이 붙어야 합니다.

### sasl.kerberos.service.name

- 타입: string
- 기본값: null
- 중요도: medium

Kafka가 실행되는 Kerberos 주체 이름입니다. 이는 Kafka의 JAAS 구성 또는 Kafka의 구성에서 정의할 수 있습니다.

### sasl.login.callback.handler.class

- 타입: class
- 기본값: null
- 중요도: medium

AuthenticateCallbackHandler 인터페이스를 구현하는 SASL 로그인 콜백 핸들러 클래스의 정규화된 이름입니다. 브로커의 경우 로그인 콜백 핸들러 구성 앞에 리스너 접두사와 소문자 SASL 메커니즘 이름이 붙어야 합니다.

### sasl.login.class

- 타입: class
- 기본값: null
- 중요도: medium

Login 인터페이스를 구현하는 클래스의 정규화된 이름입니다. 브로커의 경우 로그인 구성 앞에 리스너 접두사와 소문자 SASL 메커니즘 이름이 붙어야 합니다.

### sasl.mechanism

- 타입: string
- 기본값: GSSAPI
- 중요도: medium

클라이언트 연결에 사용되는 SASL 메커니즘입니다. 보안 제공자가 사용 가능한 모든 메커니즘이 될 수 있습니다. GSSAPI가 기본 메커니즘입니다.

### sasl.oauthbearer.jwks.endpoint.url

- 타입: string
- 기본값: null
- 중요도: medium

제공자의 JWKS(JSON Web Key Set) 검색이 가능한 OAuth/OIDC 제공자 URL입니다. URL은 HTTP(S) 기반이거나 파일 기반일 수 있습니다. URL이 HTTP(S) 기반인 경우 JWKS 데이터는 브로커 시작 시 구성된 URL을 통해 OAuth/OIDC 제공자로부터 검색됩니다. 당시 최신이었던 모든 키는 들어오는 요청에 사용할 수 있도록 브로커에 캐시됩니다. URL이 파일 기반인 경우 브로커는 구성된 위치에서 JWKS 파일을 로드합니다.

### sasl.oauthbearer.token.endpoint.url

- 타입: string
- 기본값: null
- 중요도: medium

OAuth/OIDC ID 제공자의 URL입니다. URL이 HTTP(S) 기반인 경우 `sasl.jaas.config`의 구성에 따라 로그인 요청이 이 URL로 전송되어 토큰을 검색하는 `sasl.login.callback.handler.class`의 기본 구현입니다. URL이 파일 기반인 경우 인가 토큰에 사용할 OAuth/OIDC ID 제공자에서 발급한 액세스 토큰(JWT 직렬화 형식)이 포함된 파일을 지정합니다.

### security.protocol

- 타입: string
- 기본값: PLAINTEXT
- 유효한 값: [PLAINTEXT, SSL, SASL_PLAINTEXT, SASL_SSL]
- 중요도: medium

브로커와 통신하는 데 사용되는 프로토콜입니다.

### send.buffer.bytes

- 타입: int
- 기본값: 131072 (128KB)
- 유효한 값: [-1, ...]
- 중요도: medium

데이터를 전송할 때 사용할 TCP 전송 버퍼(SO_SNDBUF)의 크기입니다. 값이 -1이면 OS 기본값이 사용됩니다.

### socket.connection.setup.timeout.max.ms

- 타입: long
- 기본값: 30000 (30초)
- 중요도: medium

클라이언트가 소켓 연결이 설정될 때까지 기다리는 최대 시간입니다. 연결 설정 타임아웃은 연속적인 연결 실패마다 기하급수적으로 증가하며 이 최대값까지 증가합니다. 연결 폭풍을 피하기 위해 타임아웃에 0.2의 무작위화 인자가 적용되어 계산된 값보다 20% 낮은 값과 20% 높은 값 사이의 무작위 범위가 됩니다.

### socket.connection.setup.timeout.ms

- 타입: long
- 기본값: 10000 (10초)
- 중요도: medium

클라이언트가 소켓 연결이 설정될 때까지 기다리는 시간입니다. 타임아웃이 경과하기 전에 연결이 설정되지 않으면 클라이언트는 소켓 채널을 닫습니다. 이 값은 초기 백오프 값이며 연속적인 연결 실패마다 `socket.connection.setup.timeout.max.ms` 값까지 기하급수적으로 증가합니다.

### ssl.enabled.protocols

- 타입: list
- 기본값: TLSv1.2,TLSv1.3
- 중요도: medium

SSL 연결에 활성화된 프로토콜 목록입니다. Java 11 이상에서 실행할 때 기본값은 'TLSv1.2,TLSv1.3'이고, 그렇지 않으면 'TLSv1.2'입니다. Java 11의 기본값에서 클라이언트와 서버 모두 TLSv1.3을 지원하는 경우 선호되고, 그렇지 않으면 TLSv1.2로 폴백됩니다(둘 다 최소 TLSv1.2를 지원한다고 가정).

### ssl.keystore.type

- 타입: string
- 기본값: JKS
- 중요도: medium

키 저장소 파일의 파일 형식입니다. 클라이언트의 경우 선택 사항입니다. 사용 가능한 값은 JKS, PKCS12, PEM입니다.

### ssl.protocol

- 타입: string
- 기본값: TLSv1.3
- 중요도: medium

SSLContext를 생성하는 데 사용되는 SSL 프로토콜입니다. Java 11 이상에서 실행할 때 기본값은 'TLSv1.3'이고, 그렇지 않으면 'TLSv1.2'입니다. 이 값은 대부분의 사용 사례에 적합합니다. 최근 JVM에서 허용되는 값은 'TLSv1.2' 및 'TLSv1.3'입니다. 'TLS', 'TLSv1.1', 'SSL', 'SSLv2' 및 'SSLv3'은 이전 JVM에서 지원될 수 있지만 알려진 보안 취약성으로 인해 사용이 권장되지 않습니다.

### ssl.provider

- 타입: string
- 기본값: null
- 중요도: medium

SSL 연결에 사용되는 보안 제공자의 이름입니다. 기본값은 JVM의 기본 보안 제공자입니다.

### ssl.truststore.type

- 타입: string
- 기본값: JKS
- 중요도: medium

신뢰 저장소 파일의 파일 형식입니다. 사용 가능한 값은 JKS, PKCS12, PEM입니다.

### sasl.oauthbearer.assertion.algorithm

- 타입: string
- 기본값: null
- 중요도: medium

JWT 서명에 사용되는 알고리즘입니다. RS256 또는 ES256을 사용할 수 있습니다.

### sasl.oauthbearer.assertion.claim.aud

- 타입: string
- 기본값: null
- 중요도: medium

JWT 생성에 사용되는 audience(대상) 클레임입니다.

### sasl.oauthbearer.assertion.claim.iss

- 타입: string
- 기본값: null
- 중요도: medium

JWT 생성에 사용되는 issuer(발급자) 클레임입니다.

### sasl.oauthbearer.assertion.claim.sub

- 타입: string
- 기본값: null
- 중요도: medium

JWT 생성에 사용되는 subject(주체) 클레임입니다.

### sasl.oauthbearer.assertion.claim.jti

- 타입: string
- 기본값: null
- 중요도: medium

JWT 생성에 사용되는 JWT ID 클레임입니다.

### sasl.oauthbearer.assertion.file

- 타입: string
- 기본값: null
- 중요도: medium

사전 생성된 JWT 파일로, 실시간 로테이션을 지원합니다.

### sasl.oauthbearer.assertion.private.key.file

- 타입: string
- 기본값: null
- 중요도: medium

JWT 서명에 사용되는 개인 키 파일입니다.

### sasl.oauthbearer.assertion.private.key.passphrase

- 타입: password
- 기본값: null
- 중요도: medium

키 파일 복호화 암호입니다.

### sasl.oauthbearer.assertion.template.file

- 타입: string
- 기본값: null
- 중요도: medium

JWT 헤더/페이로드 템플릿 파일입니다.

### sasl.oauthbearer.client.credentials.client.id

- 타입: string
- 기본값: null
- 중요도: medium

OAuth 클라이언트 식별자입니다.

### sasl.oauthbearer.client.credentials.client.secret

- 타입: password
- 기본값: null
- 중요도: medium

OAuth 클라이언트 시크릿입니다.

### sasl.oauthbearer.jwt.retriever.class

- 타입: class
- 기본값: null
- 중요도: medium

사용자 정의 JWT 검색 구현 클래스입니다.

### sasl.oauthbearer.jwt.validator.class

- 타입: class
- 기본값: null
- 중요도: medium

사용자 정의 JWT 검증 구현 클래스입니다.

### sasl.oauthbearer.scope

- 타입: string
- 기본값: null
- 중요도: medium

리소스/API 요청에 대한 액세스 수준입니다.

---

## 낮은 중요도 설정

### enable.metrics.push

- 타입: boolean
- 기본값: false
- 중요도: low

클라이언트 메트릭을 클러스터로 푸시할지 여부입니다.

### metadata.max.age.ms

- 타입: long
- 기본값: 300000 (5분)
- 유효한 값: [0, ...]
- 중요도: low

새로운 브로커나 파티션을 사전에 발견하기 위해 파티션 리더십 변경이 없더라도 메타데이터 새로 고침을 강제하는 시간(밀리초)입니다.

### metadata.recovery.rebootstrap.trigger.ms

- 타입: long
- 기본값: 300000 (5분)
- 중요도: low

재부트스트랩 트리거 임계값입니다.

### metadata.recovery.strategy

- 타입: string
- 기본값: rebootstrap
- 유효한 값: [none, rebootstrap]
- 중요도: low

복구 접근 방식입니다. `none` 또는 `rebootstrap`을 선택할 수 있습니다.

### metric.reporters

- 타입: list
- 기본값: ""
- 중요도: low

메트릭 리포터로 사용할 클래스 목록입니다. `org.apache.kafka.common.metrics.MetricsReporter` 인터페이스를 구현하면 새 메트릭 생성 알림을 받을 수 있는 클래스를 연결할 수 있습니다. JmxReporter는 JMX 통계를 등록하기 위해 항상 포함됩니다.

### metrics.num.samples

- 타입: int
- 기본값: 2
- 유효한 값: [1, ...]
- 중요도: low

메트릭 계산을 위해 유지되는 샘플 수입니다.

### metrics.recording.level

- 타입: string
- 기본값: INFO
- 유효한 값: [INFO, DEBUG, TRACE]
- 중요도: low

메트릭의 최고 기록 수준입니다.

### metrics.sample.window.ms

- 타입: long
- 기본값: 30000 (30초)
- 유효한 값: [0, ...]
- 중요도: low

메트릭 샘플이 계산되는 시간 창입니다.

### reconnect.backoff.max.ms

- 타입: long
- 기본값: 1000 (1초)
- 유효한 값: [0, ...]
- 중요도: low

브로커에 반복적으로 연결 실패할 때 대기할 최대 시간(밀리초)입니다. 제공되는 경우 호스트당 백오프는 연속적인 연결 실패마다 이 최대값까지 기하급수적으로 증가합니다. 백오프 증가 계산 후 연결 폭풍을 피하기 위해 20%의 무작위 지터가 추가됩니다.

### reconnect.backoff.ms

- 타입: long
- 기본값: 50
- 유효한 값: [0, ...]
- 중요도: low

주어진 호스트에 재연결을 시도하기 전 대기할 기본 시간입니다. 이렇게 하면 타이트한 루프에서 호스트에 반복적으로 연결하는 것을 방지합니다. 이 백오프는 클라이언트가 브로커에 대한 모든 연결 시도에 적용됩니다. 이 값은 초기 백오프 값이며 연속적인 연결 실패마다 `reconnect.backoff.max.ms` 값까지 기하급수적으로 증가합니다.

### retries

- 타입: int
- 기본값: 2147483647
- 유효한 값: [0, ...]
- 중요도: low

실패한 요청을 재시도할 횟수입니다. 기본값은 사실상 무제한입니다.

### retry.backoff.max.ms

- 타입: long
- 기본값: 1000 (1초)
- 유효한 값: [0, ...]
- 중요도: low

반복적인 실패 요청 시 대기할 최대 시간(밀리초)입니다. 제공되는 경우 요청당 백오프는 연속적인 실패마다 이 최대값까지 기하급수적으로 증가합니다. 백오프 증가 계산 후 연결 폭풍을 피하기 위해 20%의 무작위 지터가 추가됩니다.

### retry.backoff.ms

- 타입: long
- 기본값: 100
- 유효한 값: [0, ...]
- 중요도: low

실패한 요청을 재시도하기 전 대기할 기본 시간입니다. 이렇게 하면 일부 실패 시나리오에서 타이트한 루프에서 반복적으로 요청을 보내는 것을 방지합니다. 이 값은 초기 백오프 값이며 연속적인 실패마다 `retry.backoff.max.ms` 값까지 기하급수적으로 증가합니다.

### sasl.kerberos.kinit.cmd

- 타입: string
- 기본값: /usr/bin/kinit
- 중요도: low

Kerberos kinit 명령 경로입니다.

### sasl.kerberos.min.time.before.relogin

- 타입: long
- 기본값: 60000 (1분)
- 중요도: low

새로 고침 시도 사이의 로그인 스레드 휴면 시간입니다.

### sasl.kerberos.ticket.renew.jitter

- 타입: double
- 기본값: 0.05
- 중요도: low

갱신 시간에 추가되는 무작위 지터의 백분율입니다.

### sasl.kerberos.ticket.renew.window.factor

- 타입: double
- 기본값: 0.8
- 중요도: low

로그인 스레드는 지정된 창 인자에서 마지막 새로 고침부터 티켓 만료까지의 시간이 도달할 때까지 휴면한 후 티켓 갱신을 시도합니다.

### sasl.login.connect.timeout.ms

- 타입: int
- 기본값: null
- 중요도: low

외부 인증 제공자 연결 타임아웃입니다.

### sasl.login.read.timeout.ms

- 타입: int
- 기본값: null
- 중요도: low

외부 인증 제공자 읽기 타임아웃입니다.

### sasl.login.refresh.buffer.seconds

- 타입: short
- 기본값: 300 (5분)
- 유효한 값: [0, 3600]
- 중요도: low

자격 증명을 새로 고칠 때 자격 증명 만료 전 유지할 버퍼 시간(초)입니다. 새로 고침이 버퍼 시간보다 만료에 더 가깝게 발생하면 새로 고침은 가능한 한 많은 버퍼 시간을 유지하도록 앞당겨집니다. 유효한 값은 0에서 3600(1시간) 사이이며, 값이 지정되지 않으면 기본값 300(5분)이 사용됩니다.

### sasl.login.refresh.min.period.seconds

- 타입: short
- 기본값: 60 (1분)
- 유효한 값: [0, 900]
- 중요도: low

로그인 새로 고침 스레드가 자격 증명을 새로 고치기 전 대기하는 최소 시간(초)입니다. 유효한 값은 0에서 900(15분) 사이이며, 값이 지정되지 않으면 기본값 60(1분)이 사용됩니다.

### sasl.login.refresh.window.factor

- 타입: double
- 기본값: 0.8
- 유효한 값: [0.5, 1.0]
- 중요도: low

로그인 새로 고침 스레드는 자격 증명의 수명에 대해 지정된 창 인자에 도달할 때까지 휴면한 후 자격 증명 새로 고침을 시도합니다. 유효한 값은 0.5(50%)에서 1.0(100%) 사이이며, 값이 지정되지 않으면 기본값 0.8(80%)이 사용됩니다.

### sasl.login.refresh.window.jitter

- 타입: double
- 기본값: 0.05
- 유효한 값: [0.0, 0.25]
- 중요도: low

로그인 새로 고침 스레드의 휴면 시간에 추가되는 자격 증명 수명에 대한 최대 무작위 지터입니다. 유효한 값은 0에서 0.25(25%) 사이이며, 값이 지정되지 않으면 기본값 0.05(5%)가 사용됩니다.

### sasl.login.retry.backoff.max.ms

- 타입: long
- 기본값: 10000 (10초)
- 중요도: low

로그인 실패 시 최대 재시도 백오프 시간입니다.

### sasl.login.retry.backoff.ms

- 타입: long
- 기본값: 100
- 중요도: low

로그인 실패 시 초기 재시도 백오프 시간입니다.

### sasl.oauthbearer.clock.skew.seconds

- 타입: int
- 기본값: 30
- 중요도: low

OAuth/OIDC ID 제공자와 브로커 간의 시간 차이 허용 오차(초)입니다.

### sasl.oauthbearer.expected.audience

- 타입: list
- 기본값: null
- 중요도: low

예상되는 JWT audience 값 목록입니다. 브로커가 구성된 audience 중 하나가 JWT에 포함되어 있는지 확인합니다.

### sasl.oauthbearer.expected.issuer

- 타입: string
- 기본값: null
- 중요도: low

예상되는 JWT issuer 값입니다.

### sasl.oauthbearer.header.urlencode

- 타입: boolean
- 기본값: false
- 중요도: low

RFC6749에 따라 인증 헤더를 URL 인코딩할지 여부입니다.

### sasl.oauthbearer.jwks.endpoint.refresh.ms

- 타입: long
- 기본값: 3600000 (1시간)
- 중요도: low

JWKS 캐시 새로 고침 간격(밀리초)입니다.

### sasl.oauthbearer.jwks.endpoint.retry.backoff.ms

- 타입: long
- 기본값: 100
- 중요도: low

JWKS 재시도 초기 백오프 시간(밀리초)입니다.

### sasl.oauthbearer.jwks.endpoint.retry.backoff.max.ms

- 타입: long
- 기본값: 10000 (10초)
- 중요도: low

JWKS 재시도 최대 백오프 시간(밀리초)입니다.

### sasl.oauthbearer.scope.claim.name

- 타입: string
- 기본값: scope
- 중요도: low

OAuth scope 클레임 이름입니다.

### sasl.oauthbearer.sub.claim.name

- 타입: string
- 기본값: sub
- 중요도: low

OAuth subject 클레임 이름입니다.

### sasl.oauthbearer.assertion.claim.exp.seconds

- 타입: int
- 기본값: 300 (5분)
- 유효한 값: [0, 86400]
- 중요도: low

JWT 만료 오프셋(초)입니다.

### sasl.oauthbearer.assertion.claim.nbf.seconds

- 타입: int
- 기본값: 60 (1분)
- 유효한 값: [0, 3600]
- 중요도: low

JWT not-before 오프셋(초)입니다.

### security.providers

- 타입: string
- 기본값: null
- 중요도: low

구성 가능한 생성자 클래스 목록으로, 각각 보안 알고리즘을 구현하는 제공자를 반환합니다. 이러한 클래스는 `org.apache.kafka.common.security.auth.SecurityProviderCreator` 인터페이스를 구현해야 합니다.

### ssl.cipher.suites

- 타입: list
- 기본값: null
- 중요도: low

암호 모음 목록입니다. 이는 TLS 또는 SSL 네트워크 프로토콜을 사용하여 네트워크 연결의 보안 설정을 협상하는 데 사용되는 인증, 암호화, MAC 및 키 교환 알고리즘의 명명된 조합입니다. 기본적으로 사용 가능한 모든 암호 모음이 지원됩니다.

### ssl.endpoint.identification.algorithm

- 타입: string
- 기본값: https
- 중요도: low

서버 인증서를 사용하여 서버 호스트 이름을 검증하는 엔드포인트 식별 알고리즘입니다.

### ssl.engine.factory.class

- 타입: class
- 기본값: null
- 중요도: low

SSLEngine 객체를 제공하기 위한 `org.apache.kafka.common.security.auth.SslEngineFactory` 타입의 클래스입니다. 기본값은 `org.apache.kafka.common.security.ssl.DefaultSslEngineFactory`입니다. 또는 `org.apache.kafka.common.security.ssl.CommonNameLoggingSslEngineFactory`로 설정하면 클라이언트가 피어 인증서에서 일반 이름을 읽으려고 시도할 때 SSL 핸드셰이크가 발생합니다. 성공하면 DEBUG 수준에서 일반 이름이 로깅됩니다.

### ssl.keymanager.algorithm

- 타입: string
- 기본값: SunX509
- 중요도: low

SSL 연결에 대해 키 관리자 팩토리가 사용하는 알고리즘입니다. 기본값은 Java 가상 머신에 대해 구성된 키 관리자 팩토리 알고리즘입니다.

### ssl.secure.random.implementation

- 타입: string
- 기본값: null
- 중요도: low

SSL 암호화 작업에 사용되는 SecureRandom PRNG 구현입니다.

### ssl.trustmanager.algorithm

- 타입: string
- 기본값: PKIX
- 중요도: low

SSL 연결에 대해 신뢰 관리자 팩토리가 사용하는 알고리즘입니다. 기본값은 Java 가상 머신에 대해 구성된 신뢰 관리자 팩토리 알고리즘입니다.

# Kafka 보안

> 이 문서는 Apache Kafka 4.1.X 공식 문서의 "Security" 섹션을 한국어로 번역한 것입니다.
> 원본: https://kafka.apache.org/documentation/#security

---

## 목차

1. [보안 개요](#1-보안-개요)
2. [리스너 구성](#2-리스너-구성)
3. [SSL을 사용한 암호화 및 인증](#3-ssl을-사용한-암호화-및-인증)
4. [SASL을 사용한 인증](#4-sasl을-사용한-인증)
5. [권한 부여 및 ACL](#5-권한-부여-및-acl)
6. [운영 중인 클러스터에 보안 기능 적용](#6-운영-중인-클러스터에-보안-기능-적용)

---

## 1. 보안 개요

Apache Kafka는 다음과 같은 보안 기능을 제공합니다:

### 1.1 인증 (Authentication)

클라이언트(프로듀서 및 컨슈머), 다른 브로커, 도구로부터 브로커로의 연결 인증을 SSL 또는 SASL을 사용하여 수행합니다.

지원되는 SASL 메커니즘:
- SASL/GSSAPI (Kerberos) - 버전 0.9.0.0 이상
- SASL/PLAIN - 버전 0.10.0.0 이상
- SASL/SCRAM-SHA-256 및 SASL/SCRAM-SHA-512 - 버전 0.10.2.0 이상
- SASL/OAUTHBEARER - 버전 2.0 이상

### 1.2 암호화 (Encryption)

SSL을 사용하여 브로커와 클라이언트 간, 브로커 간, 또는 브로커와 도구 간에 전송되는 데이터를 암호화합니다. 암호화를 활성화하면 CPU 유형과 JVM 구현에 따라 성능에 영향을 줄 수 있습니다.

### 1.3 권한 부여 (Authorization)

클라이언트의 읽기/쓰기 작업에 대한 권한을 부여합니다. 플러그형 설계로 외부 권한 부여 시스템과의 통합이 가능합니다.

### 1.4 유연성

보안 구현은 선택 사항입니다. 개발 환경에서는 보안 조치 없이 클러스터를 운영하거나, 인증된 클라이언트와 비인증 클라이언트, 암호화된 클라이언트와 암호화되지 않은 클라이언트를 단일 배포에서 혼합하여 지원할 수 있습니다.

---

## 2. 리스너 구성

Kafka 클러스터를 보안하려면 서버와 통신하는 데 사용되는 채널을 보호해야 합니다. 각 서버는 클라이언트 및 서버 간 요청을 수신하기 위한 리스너를 정의해야 하며, 인증 메커니즘과 트래픽 암호화를 구성할 수 있습니다.

### 2.1 리스너 구성 형식

Kafka 서버는 `listeners` 속성을 통해 쉼표로 구분된 값으로 여러 포트 연결을 허용합니다:

```
{LISTENER_NAME}://{hostname}:{port}
```

예시:
```
listeners=CLIENT://localhost:9092
```

### 2.2 보안 프로토콜 매핑

`listener.security.protocol.map` 구성은 리스너에 보안 프로토콜을 할당합니다:

```
listener.security.protocol.map=CLIENT:SSL,BROKER:PLAINTEXT
```

사용 가능한 보안 프로토콜 (대소문자 구분 없음):
1. `PLAINTEXT` - 보안 없음
2. `SSL` - SSL/TLS 암호화
3. `SASL_PLAINTEXT` - SASL 인증 (암호화 없음)
4. `SASL_SSL` - SASL 인증 + SSL/TLS 암호화

### 2.3 대안적 리스너 정의

프로토콜 이름을 직접 사용할 수도 있습니다:

```
listeners=SSL://localhost:9092,PLAINTEXT://localhost:9093
```

그러나 명확성을 위해 명시적 이름 지정을 권장합니다.

### 2.4 브로커 간 통신

`inter.broker.listener.name` 구성은 브로커 간 파티션 복제에 사용되는 리스너를 지정합니다. 정의되지 않은 경우 `security.inter.broker.protocol` 설정이 적용됩니다 (기본값: PLAINTEXT).

### 2.5 KRaft 클러스터 구성

KRaft 아키텍처에서 브로커(`broker` 역할)와 컨트롤러(`controller` 역할)는 별도의 리스너가 필요합니다:

- 브로커: `inter.broker.listener.name` 사용
- 컨트롤러: `controller.listener.names` 사용
- 이 둘은 동일할 수 없음

독립 실행형 브로커 예시:
```properties
process.roles=broker
listeners=BROKER://localhost:9092
inter.broker.listener.name=BROKER
controller.quorum.bootstrap.servers=localhost:9093
controller.listener.names=CONTROLLER
listener.security.protocol.map=BROKER:SASL_SSL,CONTROLLER:SASL_SSL
```

브로커와 컨트롤러 활성화 예시:
```properties
process.roles=broker,controller
listeners=BROKER://localhost:9092,CONTROLLER://localhost:9093
inter.broker.listener.name=BROKER
controller.quorum.bootstrap.servers=localhost:9093
controller.listener.names=CONTROLLER
listener.security.protocol.map=BROKER:SASL_SSL,CONTROLLER:SASL_SSL
```

### 2.6 클라이언트 연결

일반적으로 클라이언트 리스너를 클러스터 내부 통신과 분리하여 네트워크 격리를 제공합니다. 컨트롤러는 클라이언트와 직접 상호 작용하지 않으므로 격리된 상태로 유지해야 합니다. 컨트롤러로 향하는 클라이언트 요청은 자동으로 전달됩니다.

---

## 3. SSL을 사용한 암호화 및 인증

Apache Kafka는 클라이언트가 트래픽 암호화 및 인증을 위해 SSL을 사용할 수 있도록 합니다. 기본적으로 SSL은 비활성화되어 있지만 필요에 따라 활성화할 수 있습니다.

### 3.1 각 Kafka 브로커용 SSL 키 및 인증서 생성

초기 설정에서는 모든 서버에 대해 공개/개인 키 쌍을 생성해야 합니다. Kafka는 모든 키와 인증서가 Java의 keytool 명령을 사용하여 keystore에 저장되기를 기대합니다. 두 가지 keystore 형식이 있습니다: 더 이상 사용되지 않는 Java 전용 jks 형식과 PKCS12 (Java 9부터 기본값).

명령:
```bash
$ keytool -keystore {keystorefile} -alias localhost -validity {validity} -genkey -keyalg RSA -storetype pkcs12
```

매개변수:
- `keystorefile`: 브로커의 키와 나중에 인증서를 저장하며, 개인 키와 공개 키를 포함하므로 안전하게 저장해야 함
- `validity`: 키 유효 기간(일 단위) - 서명 시 결정되는 인증서 유효 기간과 다름

인증서 서명 요청 생성:
```bash
$ keytool -keystore server.keystore.jks -alias localhost -validity {validity} -genkey -keyalg RSA -destkeystoretype pkcs12 -ext SAN=DNS:{FQDN},IP:{IPADDRESS1}
```

### 3.2 호스트 이름 확인

호스트 이름 확인은 연결하려는 서버가 제시하는 인증서의 속성을 해당 서버의 실제 호스트 이름 또는 IP 주소와 비교하여 확인하는 프로세스입니다.

주요 목적은 중간자 공격(man-in-the-middle attack)을 방지하는 것입니다. Kafka 2.0.0부터 클라이언트 연결 및 브로커 간 연결에 대해 서버의 호스트 이름 확인이 기본적으로 활성화됩니다.

확인을 비활성화하려면 `ssl.endpoint.identification.algorithm`을 빈 문자열로 설정합니다.

동적으로 구성된 브로커 리스너의 경우:
```bash
$ bin/kafka-configs.sh --bootstrap-server localhost:9093 --entity-type brokers --entity-name 0 --alter --add-config "listener.name.internal.ssl.endpoint.identification.algorithm="
```

> 참고: 일반적으로 호스트 이름 확인을 비활성화해야 할 좋은 이유는 없습니다. 이는 '일단 작동하게 만드는' 가장 빠른 방법일 뿐입니다.

클라이언트는 다음에 대해 서버의 FQDN 또는 IP 주소를 확인합니다:
1. Common Name (CN)
2. Subject Alternative Name (SAN)

SAN 필드는 훨씬 더 유연하며 인증서에 여러 DNS 및 IP 항목을 선언할 수 있다는 장점이 있습니다.

SAN 필드 추가:
```bash
$ keytool -keystore server.keystore.jks -alias localhost -validity {validity} -genkey -keyalg RSA -destkeystoretype pkcs12 -ext SAN=DNS:{FQDN},IP:{IPADDRESS1}
```

### 3.3 자체 CA 생성

인증 기관(CA, Certificate Authority)은 인증서 서명을 담당합니다. CA는 여권을 발급하는 정부와 유사하게 작동합니다 - 인증서에 스탬프/서명하여 위조하기 어렵게 만듭니다.

구성 파일 (openssl-ca.cnf):
```ini
HOME            = .
RANDFILE        = $ENV::HOME/.rnd

####################################################################
[ ca ]
default_ca    = CA_default

[ CA_default ]

base_dir      = .
certificate   = $base_dir/cacert.pem
private_key   = $base_dir/cakey.pem
new_certs_dir = $base_dir
database      = $base_dir/index.txt
serial        = $base_dir/serial.txt

default_days     = 1000
default_crl_days = 30
default_md       = sha256
preserve         = no

x509_extensions = ca_extensions
email_in_dn     = no
copy_extensions = copy

####################################################################
[ req ]
default_bits       = 4096
default_keyfile    = cakey.pem
distinguished_name = ca_distinguished_name
x509_extensions    = ca_extensions
string_mask        = utf8only

####################################################################
[ ca_distinguished_name ]
countryName         = Country Name (2 letter code)
countryName_default = DE

stateOrProvinceName         = State or Province Name (full name)
stateOrProvinceName_default = Test Province

localityName                = Locality Name (eg, city)
localityName_default        = Test Town

organizationName            = Organization Name (eg, company)
organizationName_default    = Test Company

organizationalUnitName         = Organizational Unit (eg, division)
organizationalUnitName_default = Test Unit

commonName         = Common Name (e.g. server FQDN or YOUR name)
commonName_default = Test Name

emailAddress         = Email Address
emailAddress_default = test@test.com

####################################################################
[ ca_extensions ]

subjectKeyIdentifier   = hash
authorityKeyIdentifier = keyid:always, issuer
basicConstraints       = critical, CA:true
keyUsage               = keyCertSign, cRLSign

####################################################################
[ signing_policy ]
countryName            = optional
stateOrProvinceName    = optional
localityName           = optional
organizationName       = optional
organizationalUnitName = optional
commonName             = supplied
emailAddress           = optional

####################################################################
[ signing_req ]
subjectKeyIdentifier   = hash
authorityKeyIdentifier = keyid,issuer
basicConstraints       = CA:FALSE
keyUsage               = digitalSignature, keyEncipherment
```

데이터베이스 및 시리얼 파일 생성:
```bash
$ echo 01 > serial.txt
$ touch index.txt
```

CA 생성:
```bash
$ openssl req -x509 -config openssl-ca.cnf -newkey rsa:4096 -sha256 -nodes -out cacert.pem -outform PEM
```

CA는 단순히 자체 서명된 공개/개인 키 쌍과 인증서이며, 다른 인증서에 서명하기 위한 용도로만 사용됩니다.

클라이언트의 truststore에 CA 추가:
```bash
$ keytool -keystore client.truststore.jks -alias CARoot -import -file ca-cert
```

브로커의 truststore에 CA 추가 (클라이언트 인증이 필요한 경우):
```bash
$ keytool -keystore server.truststore.jks -alias CARoot -import -file ca-cert
```

1단계에서 각 시스템의 고유 ID를 저장하는 keystore와 달리, 클라이언트의 truststore는 클라이언트가 신뢰해야 하는 모든 인증서를 저장합니다.

### 3.4 인증서 서명

CA로 인증서 서명:
```bash
$ openssl ca -config openssl-ca.cnf -policy signing_policy -extensions signing_req -out {server certificate} -infiles {certificate signing request}
```

keystore에 CA 및 서명된 인증서 가져오기:
```bash
$ keytool -keystore {keystore} -alias CARoot -import -file {CA certificate}
$ keytool -keystore {keystore} -alias localhost -import -file cert-signed
```

매개변수 정의:
- `keystore`: keystore 위치
- `CA certificate`: CA의 인증서
- `certificate signing request`: 서버 키로 생성된 CSR
- `server certificate`: 서명된 서버 인증서용 파일

이렇게 하면 `truststore.jks`라는 하나의 truststore가 생성됩니다. 이는 모든 클라이언트와 브로커에서 동일할 수 있으며 민감한 정보를 포함하지 않으므로 보안할 필요가 없습니다.

> 참고: easyRSA 프로젝트가 스크립팅 지원을 제공합니다.

### 3.5 PEM 형식의 SSL 키 및 인증서

2.7.0 버전부터 SSL 키와 trust store를 PEM 형식으로 Kafka 브로커와 클라이언트 구성에서 직접 구성할 수 있습니다.

구성:
- PEM 형식의 개인 키: `ssl.keystore.key`
- PEM 형식의 인증서 체인: `ssl.keystore.certificate.chain`
- 신뢰 인증서: `ssl.truststore.certificates`

여러 줄 문자열은 백슬래시('\')를 사용하여 줄 연속을 표시합니다.

PEM의 경우 `ssl.keystore.password` 및 `ssl.truststore.password` 구성은 사용되지 않습니다. 개인 키가 암호화된 경우 `ssl.key.password`에 비밀번호를 제공하세요.

개인 키는 비밀번호 없이 암호화되지 않은 형태로 제공될 수 있습니다. 프로덕션 배포에서는 이 경우 Kafka의 비밀번호 보호 기능을 사용하여 구성을 암호화하거나 외부화해야 합니다.

> 참고: 기본 SSL 엔진 팩토리는 OpenSSL과 같은 외부 도구를 사용한 암호화된 개인 키 복호화에 제한된 기능을 갖습니다. BouncyCastle과 같은 타사 라이브러리를 사용자 정의 SslEngineFactory와 통합하여 더 넓은 범위를 지원할 수 있습니다.

### 3.6 프로덕션의 일반적인 함정

프로덕션 클러스터는 일반적으로 자체 서명 인증서 대신 엔터프라이즈 CA를 사용합니다. 그러나 몇 가지 일반적인 문제가 발생합니다:

#### 3.6.1 확장 키 사용 (Extended Key Usage)

인증서에는 인증서 사용 목적을 제어하는 확장 필드가 포함될 수 있습니다.

Kafka 브로커는 클러스터 내부 통신 중에 클라이언트와 서버 역할을 모두 수행하므로 클라이언트 및 서버 인증 사용이 모두 허용되어야 합니다. 웹 서버 프로필과 같은 기업 CA 서명 프로필에는 serverAuth 사용만 포함될 수 있어 SSL 핸드셰이크 실패가 발생합니다.

#### 3.6.2 중간 인증서 (Intermediate Certificates)

보안을 위해 기업 루트 CA는 오프라인으로 유지되며, 중간 CA가 일상적인 서명을 처리합니다. 중간에 의해 서명된 인증서를 가져올 때 루트 CA까지의 전체 신뢰 체인을 제공해야 합니다. keytool로 가져오기 전에 `cat`을 통해 인증서 파일을 결합하세요.

#### 3.6.3 확장 필드 복사 실패

CA 운영자는 종종 CSR에서 요청된 확장 필드를 복사하는 것을 꺼리며 독립적으로 지정하는 것을 선호합니다. 서명된 인증서에 적절한 호스트 이름 확인을 가능하게 하는 모든 요청된 SAN 필드가 포함되어 있는지 다시 확인하세요.

인증서 세부 정보 확인:
```bash
$ openssl x509 -in certificate.crt -text -noout
```

### 3.7 Kafka 브로커 구성

브로커 간 통신에 SSL이 활성화되지 않은 경우 PLAINTEXT 및 SSL 포트가 모두 필요합니다.

```properties
listeners=PLAINTEXT://host.name:port,SSL://host.name:port
```

필수 SSL 구성:
```properties
ssl.keystore.location=/var/private/ssl/server.keystore.jks
ssl.keystore.password=test1234
ssl.key.password=test1234
ssl.truststore.location=/var/private/ssl/server.truststore.jks
ssl.truststore.password=test1234
```

> 참고: `ssl.truststore.password`는 기술적으로 선택 사항이지만 강력히 권장됩니다. 비밀번호가 설정되지 않으면 truststore에 대한 액세스는 여전히 가능하지만 무결성 검사가 비활성화됩니다.

선택적 설정:
1. `ssl.client.auth=none` ("required" = 클라이언트 인증 필수; "requested" = 요청하지만 선택 사항; "requested"는 권장하지 않음)
2. `ssl.cipher.suites` (선택 사항): 인증, 암호화, MAC 및 키 교환 알고리즘의 명명된 조합
3. `ssl.enabled.protocols=TLSv1.2,TLSv1.1,TLSv1`: 클라이언트에서 허용되는 SSL 프로토콜 목록 (SSL은 더 이상 사용되지 않음)
4. `ssl.keystore.type=JKS`
5. `ssl.truststore.type=JKS`
6. `ssl.secure.random.implementation=SHA1PRNG`

브로커 간 통신에 SSL 활성화:
```properties
security.inter.broker.protocol=SSL
```

암호화 강도 제한:

일부 국가의 수입 규정으로 인해 Oracle 구현은 기본적으로 사용 가능한 암호화 알고리즘의 강도를 제한합니다. 더 강력한 알고리즘이 필요한 경우 (예: 256비트 키를 사용하는 AES) JCE Unlimited Strength Jurisdiction Policy Files를 받아 설치해야 합니다.

의사 난수 생성기 성능:

JRE/JDK에는 암호화 작업에 사용되는 기본 의사 난수 생성기(PRNG)가 있으므로 `ssl.secure.random.implementation` 구현을 구성할 필요가 없습니다. 그러나 일부 구현(특히 Linux 시스템에서 기본적으로 선택되는 NativePRNG는 전역 잠금을 사용함)에는 성능 문제가 있습니다.

SHA1PRNG 구현은 비차단이며 높은 부하(브로커당 복제 트래픽을 포함하여 초당 50MB의 생성된 메시지)에서 매우 좋은 성능 특성을 보여주었습니다.

브로커 시작 확인:

예상되는 server.log 출력:
```
with addresses: PLAINTEXT -> EndPoint(192.168.64.1,9092,PLAINTEXT),SSL -> EndPoint(192.168.64.1,9093,SSL)
```

빠른 keystore/truststore 검증:
```bash
$ openssl s_client -debug -connect localhost:9093 -tls1
```

(TLSv1이 ssl.enabled.protocols 아래에 나열되어야 함)

예상 출력에는 서버 인증서가 포함됩니다:
```
-----BEGIN CERTIFICATE-----
{variable sized random bytes}
-----END CERTIFICATE-----
subject=/C=US/ST=CA/L=Santa Clara/O=org/OU=org/CN=Sriharsha Chintalapani
issuer=/C=US/ST=CA/L=Santa Clara/O=org/OU=org/CN=kafka/emailAddress=test@test.com
```

인증서 부재 또는 오류 메시지는 부적절한 keystore 설정을 나타냅니다.

### 3.8 Kafka 클라이언트 구성

SSL은 새로운 Kafka Producer 및 Consumer에서만 지원됩니다. 이전 API는 지원되지 않습니다. SSL 구성은 프로듀서와 컨슈머 모두 동일합니다.

클라이언트 인증 없는 최소 구성:
```properties
security.protocol=SSL
ssl.truststore.location=/var/private/ssl/client.truststore.jks
ssl.truststore.password=test1234
```

> 참고: `ssl.truststore.password`는 기술적으로 선택 사항이지만 강력히 권장됩니다.

클라이언트 인증이 필요한 구성:
```properties
ssl.keystore.location=/var/private/ssl/client.keystore.jks
ssl.keystore.password=test1234
ssl.key.password=test1234
```

추가 가능한 설정:
1. `ssl.provider` (선택 사항): SSL 연결에 대한 보안 공급자 이름; 기본값은 JVM 기본값
2. `ssl.cipher.suites` (선택 사항): 인증, 암호화, MAC 및 키 교환 알고리즘의 명명된 조합
3. `ssl.enabled.protocols=TLSv1.2,TLSv1.1,TLSv1`: 브로커 측에 구성된 프로토콜 중 하나 이상을 나열해야 함
4. `ssl.truststore.type=JKS`
5. `ssl.keystore.type=JKS`

콘솔 도구 예시:
```bash
$ bin/kafka-console-producer.sh --bootstrap-server localhost:9093 --topic test --producer.config client-ssl.properties
$ bin/kafka-console-consumer.sh --bootstrap-server localhost:9093 --topic test --consumer.config client-ssl.properties
```

---

## 4. SASL을 사용한 인증

### 4.1 JAAS 구성

Kafka는 SASL 구성을 위해 Java 인증 및 권한 부여 서비스(JAAS)를 사용합니다.

#### 브로커용

- 브로커의 JAAS 파일에서 섹션 이름은 `KafkaServer`입니다
- 섹션 이름 앞에 리스너 이름을 소문자로 붙이고 마침표를 붙일 수 있습니다 (예: `sasl_ssl.KafkaServer`)
- 브로커는 `sasl.jaas.config` 속성을 사용하여 JAAS를 구성할 수 있습니다
- 속성 이름 형식: `listener.name.{listenerName}.{saslMechanism}.sasl.jaas.config`
- 구성 값당 하나의 로그인 모듈만 지정할 수 있습니다
- 여러 메커니즘은 각각에 대해 별도의 구성이 필요합니다

구성 우선 순위:
1. 브로커 구성 속성 `listener.name.{listenerName}.{saslMechanism}.sasl.jaas.config`
2. 정적 JAAS 구성의 `{listenerName}.KafkaServer` 섹션
3. 정적 JAAS 구성의 `KafkaServer` 섹션

브로커 구성 예시:
```properties
listener.name.sasl_ssl.scram-sha-256.sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required \
    username="admin" \
    password="admin-secret";
listener.name.sasl_ssl.plain.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required \
    username="admin" \
    password="admin-secret" \
    user_admin="admin-secret" \
    user_alice="alice-secret";
```

#### 클라이언트용

- 클라이언트는 구성 속성 `sasl.jaas.config` 또는 정적 JAAS 구성 파일을 사용할 수 있습니다
- 동일한 JVM의 다른 프로듀서/컨슈머는 다른 자격 증명을 사용할 수 있습니다
- `java.security.auth.login.config` 시스템 속성과 `sasl.jaas.config` 클라이언트 속성이 모두 지정된 경우 클라이언트 속성이 우선합니다

Kerberos 클라이언트 예시:
```
KafkaClient {
    com.sun.security.auth.module.Krb5LoginModule required
    useKeyTab=true
    storeKey=true
    keyTab="/etc/security/keytabs/kafka_client.keytab"
    principal="kafka-client-1@EXAMPLE.COM";
};
```

### 4.2 SASL 구성

전송 계층 옵션:
- SASL은 PLAINTEXT 또는 SSL을 전송 계층으로 사용할 수 있음
- 보안 프로토콜: `SASL_PLAINTEXT` 또는 `SASL_SSL`
- `SASL_SSL`을 사용하는 경우 SSL도 구성해야 함

지원되는 SASL 메커니즘:
- GSSAPI (Kerberos)
- PLAIN
- SCRAM-SHA-256
- SCRAM-SHA-512
- OAUTHBEARER

브로커용:

1. server.properties에서 `listeners` 매개변수에 SASL 포트를 추가하여 구성:
   ```properties
   listeners=SASL_PLAINTEXT://host.name:port
   ```

2. SASL 전용 설정의 경우 브로커 간 프로토콜 설정:
   ```properties
   security.inter.broker.protocol=SASL_PLAINTEXT (또는 SASL_SSL)
   ```

3. 지원되는 메커니즘 중 하나 이상을 선택하고 구성

클라이언트용:
- SASL 인증은 새로운 Java Kafka 프로듀서 및 컨슈머에서만 지원됩니다. 이전 API는 지원되지 않습니다.
- 브로커에서 활성화된 메커니즘 선택
- `security.protocol` 및 `sasl.mechanism` 속성 구성

> DNS 관련 참고: SASL을 통해 브로커에 연결을 설정할 때 클라이언트는 브로커 주소의 역방향 DNS 조회를 수행할 수 있습니다. JRE가 역방향 DNS 조회를 구현하는 방식으로 인해 클라이언트의 `bootstrap.servers`와 브로커의 `advertised.listeners` 모두에 정규화된 도메인 이름을 사용하지 않으면 SASL 핸드셰이크가 느려질 수 있습니다.

### 4.3 SASL/Kerberos를 사용한 인증

#### 전제 조건

1. Kerberos 서버:
   - 사용 가능한 경우 기존 조직 Kerberos 서버 사용
   - 그렇지 않으면 설치 (Ubuntu/Redhat 패키지 사용 가능)
   - Oracle Java 사용자는 JCE 정책 파일을 다운로드하고 `$JAVA_HOME/jre/lib/security`에 복사해야 함

2. Kerberos Principal 생성:
   - 조직 서버의 경우: Kerberos 관리자에게 principal 요청
   - 자체 설치 서버의 경우 principal 생성:
   ```bash
   $ sudo /usr/sbin/kadmin.local -q 'addprinc -randkey kafka/{hostname}@{REALM}'
   $ sudo /usr/sbin/kadmin.local -q "ktadd -k /etc/security/keytabs/{keytabname}.keytab kafka/{hostname}@{REALM}"
   ```

3. 호스트 해석:
   모든 호스트가 호스트 이름을 사용하여 연결 가능한지 확인하세요 - 모든 호스트가 FQDN으로 해석될 수 있어야 하는 것은 Kerberos 요구 사항입니다.

#### Kafka 브로커 구성

1. JAAS 구성 파일 생성 (예: `kafka_server_jaas.conf`):
```
KafkaServer {
    com.sun.security.auth.module.Krb5LoginModule required
    useKeyTab=true
    storeKey=true
    keyTab="/etc/security/keytabs/kafka_server.keytab"
    principal="kafka/kafka1.hostname.com@EXAMPLE.COM";
};
```

2. JAAS 및 krb5 파일 위치를 JVM 매개변수로 전달:
```
-Djava.security.krb5.conf=/etc/kafka/krb5.conf
-Djava.security.auth.login.config=/etc/kafka/kafka_server_jaas.conf
```

3. Kafka 브로커를 시작하는 OS 사용자가 keytab을 읽을 수 있는지 확인

4. server.properties 구성:
```properties
listeners=SASL_PLAINTEXT://host.name:port
security.inter.broker.protocol=SASL_PLAINTEXT
sasl.mechanism.inter.broker.protocol=GSSAPI
sasl.enabled.mechanisms=GSSAPI
sasl.kerberos.service.name=kafka
```

#### Kafka 클라이언트 구성

1. 클라이언트 principal을 얻거나 생성하고 JAAS 구성:

   keytab 사용 (장시간 실행 프로세스에 권장):
   ```properties
   sasl.jaas.config=com.sun.security.auth.module.Krb5LoginModule required \
       useKeyTab=true \
       storeKey=true  \
       keyTab="/etc/security/keytabs/kafka_client.keytab" \
       principal="kafka-client-1@EXAMPLE.COM";
   ```

   명령줄 유틸리티용 kinit 사용:
   ```properties
   sasl.jaas.config=com.sun.security.auth.module.Krb5LoginModule required \
       useTicketCache=true;
   ```

2. 클라이언트를 시작하는 OS 사용자가 keytab을 읽을 수 있는지 확인

3. 선택적으로 krb5 파일을 JVM 매개변수로 전달:
   ```
   -Djava.security.krb5.conf=/etc/kafka/krb5.conf
   ```

4. 클라이언트 속성 구성:
```properties
security.protocol=SASL_PLAINTEXT (또는 SASL_SSL)
sasl.mechanism=GSSAPI
sasl.kerberos.service.name=kafka
```

### 4.4 SASL/PLAIN을 사용한 인증

SASL/PLAIN은 일반적으로 안전한 인증을 구현하기 위해 암호화용 TLS와 함께 사용되는 간단한 사용자 이름/비밀번호 인증 메커니즘입니다.

`principal.builder.class`의 기본 구현에서 사용자 이름은 ACL 구성 등을 위한 인증된 `Principal`로 사용됩니다.

#### Kafka 브로커 구성

1. JAAS 구성 파일 생성:
```
KafkaServer {
    org.apache.kafka.common.security.plain.PlainLoginModule required
    username="admin"
    password="admin-secret"
    user_admin="admin-secret"
    user_alice="alice-secret";
};
```

`username` 및 `password` 속성은 브로커 간 통신용입니다. `user_{userName}` 속성은 브로커에 연결하는 모든 사용자의 비밀번호를 정의합니다.

2. JAAS 구성 파일을 JVM 매개변수로 전달:
```
-Djava.security.auth.login.config=/etc/kafka/kafka_server_jaas.conf
```

3. server.properties 구성:
```properties
listeners=SASL_SSL://host.name:port
security.inter.broker.protocol=SASL_SSL
sasl.mechanism.inter.broker.protocol=PLAIN
sasl.enabled.mechanisms=PLAIN
```

#### Kafka 클라이언트 구성

1. 클라이언트 구성에서 JAAS 속성 구성:
```properties
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required \
    username="alice" \
    password="alice-secret";
```

다른 클라이언트는 다른 `sasl.jaas.config` 지정을 통해 다른 사용자로 연결할 수 있습니다.

2. 클라이언트 속성 구성:
```properties
security.protocol=SASL_SSL
sasl.mechanism=PLAIN
```

#### 프로덕션 사용

- SASL/PLAIN은 암호화 없이 일반 텍스트 비밀번호가 전송되지 않도록 SSL을 전송 계층으로만 사용해야 합니다.
- Kafka 버전 2.0부터 `sasl.server.callback.handler.class` 및 `sasl.client.callback.handler.class` 구성 옵션을 사용하여 외부 소스에서 사용자 이름과 비밀번호를 가져오는 자체 콜백 핸들러를 구성하여 디스크에 일반 텍스트 비밀번호를 저장하지 않을 수 있습니다.
- Kafka 버전 2.0부터 `sasl.server.callback.handler.class`를 구성하여 비밀번호 확인을 위해 외부 인증 서버를 사용하는 자체 콜백 핸들러를 플러그인할 수 있습니다.

### 4.5 SASL/SCRAM을 사용한 인증

SCRAM(Salted Challenge Response Authentication Mechanism)은 PLAIN 및 DIGEST-MD5와 같은 사용자 이름/비밀번호 인증을 수행하는 기존 메커니즘의 보안 문제를 해결하는 SASL 메커니즘 계열입니다.

Kafka는 RFC 5802 및 RFC 7677에 정의된 SCRAM-SHA-256 및 SCRAM-SHA-512를 지원합니다.

`principal.builder.class`의 기본 구현에서 사용자 이름은 ACL 구성 등을 위한 인증된 `Principal`로 사용됩니다.

Kafka의 기본 SCRAM 구현은 SCRAM 자격 증명을 메타데이터 로그에 저장합니다.

#### SCRAM 자격 증명 생성

Kafka의 SCRAM 구현은 메타데이터 로그를 자격 증명 저장소로 사용합니다. 자격 증명은 `kafka-storage.sh` 또는 `kafka-configs.sh`를 사용하여 메타데이터 로그에 생성할 수 있습니다.

활성화된 각 SCRAM 메커니즘에 대해 메커니즘 이름으로 구성을 추가하여 자격 증명을 생성해야 합니다. 브로커 간 통신을 위한 자격 증명은 Kafka 브로커를 시작하기 전에 생성해야 합니다.

admin 사용자의 초기 자격 증명 생성:
```bash
$ bin/kafka-storage.sh format -t $(bin/kafka-storage.sh random-uuid) -c config/server.properties --add-scram 'SCRAM-SHA-256=[name="admin",password="admin-secret"]'
```

alice 사용자의 자격 증명 생성:
```bash
$ bin/kafka-configs.sh --bootstrap-server localhost:9092 --alter --add-config 'SCRAM-SHA-256=[iterations=8192,password=alice-secret]' --entity-type users --entity-name alice --command-config client.properties
```

반복 횟수를 지정하지 않으면 기본 반복 횟수 4096이 사용됩니다. salt를 지정하지 않으면 무작위 salt가 생성됩니다.

기존 자격 증명 나열:
```bash
$ bin/kafka-configs.sh --bootstrap-server localhost:9092 --describe --entity-type users --entity-name alice --command-config client.properties
```

자격 증명 삭제:
```bash
$ bin/kafka-configs.sh --bootstrap-server localhost:9092 --alter --delete-config 'SCRAM-SHA-256' --entity-type users --entity-name alice --command-config client.properties
```

#### Kafka 브로커 구성

1. JAAS 구성 파일 생성:
```
KafkaServer {
    org.apache.kafka.common.security.scram.ScramLoginModule required
    username="admin"
    password="admin-secret";
};
```

2. JAAS 구성 파일을 JVM 매개변수로 전달:
```
-Djava.security.auth.login.config=/etc/kafka/kafka_server_jaas.conf
```

3. server.properties 구성:
```properties
listeners=SASL_SSL://host.name:port
security.inter.broker.protocol=SASL_SSL
sasl.mechanism.inter.broker.protocol=SCRAM-SHA-256 (또는 SCRAM-SHA-512)
sasl.enabled.mechanisms=SCRAM-SHA-256 (또는 SCRAM-SHA-512)
```

#### Kafka 클라이언트 구성

1. JAAS 속성 구성:
```properties
sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required \
    username="alice" \
    password="alice-secret";
```

2. 클라이언트 속성 구성:
```properties
security.protocol=SASL_SSL
sasl.mechanism=SCRAM-SHA-256 (또는 SCRAM-SHA-512)
```

#### SASL/SCRAM 보안 고려 사항

- Kafka의 기본 SASL/SCRAM 구현은 SCRAM 자격 증명을 메타데이터 로그에 저장합니다. 이는 KRaft 컨트롤러가 안전하고 개인 네트워크에 있는 설치에서 프로덕션 사용에 적합합니다.
- Kafka는 최소 반복 횟수 4096으로 강력한 해시 함수 SHA-256 및 SHA-512만 지원합니다. 강력한 해시 함수와 강력한 비밀번호 및 높은 반복 횟수는 KRaft 컨트롤러 보안이 손상된 경우 무차별 대입 공격으로부터 보호합니다.
- SCRAM은 SCRAM 교환의 가로채기를 방지하기 위해 TLS 암호화와 함께만 사용해야 합니다. 이는 KRaft 컨트롤러 보안이 손상된 경우 사전 또는 무차별 대입 공격 및 사칭으로부터 보호합니다.
- Kafka 버전 2.0부터 KRaft 컨트롤러가 안전하지 않은 설치에서 `sasl.server.callback.handler.class`를 구성하여 기본 SASL/SCRAM 자격 증명 저장소를 사용자 정의 콜백 핸들러로 재정의할 수 있습니다.

### 4.6 SASL/OAUTHBEARER를 사용한 인증

OAuth 2 인증 프레임워크는 타사 애플리케이션이 리소스 소유자를 대신하여 또는 자체적으로 HTTP 서비스에 대한 제한된 액세스를 얻을 수 있도록 합니다.

SASL OAUTHBEARER 메커니즘은 SASL(즉, 비HTTP) 컨텍스트에서 프레임워크를 사용할 수 있게 합니다. RFC 7628에 정의되어 있습니다.

Kafka의 기본 OAUTHBEARER 구현은 비보안 JSON 웹 토큰을 생성하고 검증하며 비프로덕션 Kafka 설치에서만 사용하기에 적합합니다.

최근 Apache Kafka 버전에는 OAuth 2.0 표준 호환 ID 공급자와의 상호 작용을 지원하는 프로덕션 준비 OAUTHBEARER 구현이 추가되었습니다.

`principal.builder.class`의 기본 구현에서 OAuthBearerToken의 principalName이 ACL 구성 등을 위한 인증된 `Principal`로 사용됩니다.

#### 비프로덕션 Kafka 브로커 구성

1. JAAS 구성 파일 생성:
```
KafkaServer {
    org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required
    unsecuredLoginStringClaim_sub="admin";
};
```

`unsecuredLoginStringClaim_sub` 속성은 브로커 간 연결에 사용됩니다.

2. JAAS 구성 파일을 JVM 매개변수로 전달:
```
-Djava.security.auth.login.config=/etc/kafka/kafka_server_jaas.conf
```

3. server.properties 구성:
```properties
listeners=SASL_SSL://host.name:port (비프로덕션인 경우 SASL_PLAINTEXT)
security.inter.broker.protocol=SASL_SSL (비프로덕션인 경우 SASL_PLAINTEXT)
sasl.mechanism.inter.broker.protocol=OAUTHBEARER
sasl.enabled.mechanisms=OAUTHBEARER
```

#### 프로덕션 Kafka 브로커 구성

1. JAAS 구성 파일 생성:
```
KafkaServer {
    org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required ;
};
```

2. JAAS 구성 파일을 JVM 매개변수로 전달:
```
-Djava.security.auth.login.config=/etc/kafka/kafka_server_jaas.conf
```

3. server.properties 구성:
```properties
listeners=SASL_SSL://host.name:port
security.inter.broker.protocol=SASL_SSL
sasl.mechanism.inter.broker.protocol=OAUTHBEARER
sasl.enabled.mechanisms=OAUTHBEARER
listener.name.<listener name>.oauthbearer.sasl.server.callback.handler.class=org.apache.kafka.common.security.oauthbearer.OAuthBearerValidatorCallbackHandler
listener.name.<listener name>.oauthbearer.sasl.oauthbearer.jwks.endpoint.url=https://example.com/oauth2/v1/keys
```

OAUTHBEARER 브로커 구성 옵션:
- `sasl.oauthbearer.clock.skew.seconds`
- `sasl.oauthbearer.expected.audience`
- `sasl.oauthbearer.expected.issuer`
- `sasl.oauthbearer.jwks.endpoint.refresh.ms`
- `sasl.oauthbearer.jwks.endpoint.retry.backoff.max.ms`
- `sasl.oauthbearer.jwks.endpoint.retry.backoff.ms`
- `sasl.oauthbearer.jwks.endpoint.url`
- `sasl.oauthbearer.scope.claim.name`
- `sasl.oauthbearer.sub.claim.name`

#### 프로덕션 Kafka 클라이언트 구성

1. JAAS 속성 구성:
```properties
sasl.jaas.config=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required ;
```

2. 클라이언트 속성 구성. OAuth `client_credentials` 그랜트 예시:
```properties
security.protocol=SASL_SSL
sasl.mechanism=OAUTHBEARER
sasl.oauthbearer.jwt.retriever.class=org.apache.kafka.common.security.oauthbearer.ClientCredentialsJwtRetriever
sasl.oauthbearer.client.credentials.client.id=jdoe
sasl.oauthbearer.client.credentials.client.secret=$3cr3+
sasl.oauthbearer.scope=my-application-scope
sasl.oauthbearer.token.endpoint.url=https://example.com/oauth2/v1/token
```

OAuth `urn:ietf:params:oauth:grant-type:jwt-bearer` 그랜트 예시:
```properties
security.protocol=SASL_SSL
sasl.mechanism=OAUTHBEARER
sasl.oauthbearer.jwt.retriever.class=org.apache.kafka.common.security.oauthbearer.JwtBearerJwtRetriever
sasl.oauthbearer.assertion.private.key.file=/path/to/private.key
sasl.oauthbearer.assertion.algorithm=RS256
sasl.oauthbearer.assertion.claim.exp.seconds=600
sasl.oauthbearer.assertion.template.file=/path/to/template.json
sasl.oauthbearer.scope=my-application-scope
sasl.oauthbearer.token.endpoint.url=https://example.com/oauth2/v1/token
```

#### SASL/OAUTHBEARER 보안 고려 사항

- Kafka의 기본 SASL/OAUTHBEARER 구현은 비보안 JSON 웹 토큰을 생성하고 검증합니다. 이는 비프로덕션 사용에만 적합합니다.
- OAUTHBEARER는 토큰 가로채기를 방지하기 위해 프로덕션 환경에서 TLS 암호화와 함께만 사용해야 합니다.
- 기본 비보안 SASL/OAUTHBEARER 구현은 위에서 설명한 대로 사용자 정의 로그인 및 SASL 서버 콜백 핸들러를 사용하여 재정의할 수 있습니다 (프로덕션 환경에서는 반드시 재정의해야 함).

### 4.7 브로커에서 여러 SASL 메커니즘 활성화

1. JAAS 구성의 `KafkaServer` 섹션에서 활성화된 모든 메커니즘에 대한 구성을 지정:
```
KafkaServer {
    com.sun.security.auth.module.Krb5LoginModule required
    useKeyTab=true
    storeKey=true
    keyTab="/etc/security/keytabs/kafka_server.keytab"
    principal="kafka/kafka1.hostname.com@EXAMPLE.COM";

    org.apache.kafka.common.security.plain.PlainLoginModule required
    username="admin"
    password="admin-secret"
    user_admin="admin-secret"
    user_alice="alice-secret";
};
```

2. server.properties에서 SASL 메커니즘 활성화:
```properties
sasl.enabled.mechanisms=GSSAPI,PLAIN,SCRAM-SHA-256,SCRAM-SHA-512,OAUTHBEARER
```

3. 브로커 간 통신을 위한 SASL 보안 프로토콜 및 메커니즘 지정:
```properties
security.inter.broker.protocol=SASL_PLAINTEXT (또는 SASL_SSL)
sasl.mechanism.inter.broker.protocol=GSSAPI (또는 다른 활성화된 메커니즘 중 하나)
```

4. 각 활성화된 메커니즘에 대한 메커니즘별 단계 수행

### 4.8 운영 중인 클러스터에서 SASL 메커니즘 수정

SASL 메커니즘은 다음 순서를 사용하여 수정할 수 있습니다:

1. 각 브로커의 server.properties에서 `sasl.enabled.mechanisms`에 추가하여 새 메커니즘을 활성화합니다. JAAS 구성 파일을 업데이트하여 두 메커니즘을 모두 포함합니다. 클러스터 노드를 순차적으로 재시작합니다.

2. 새 메커니즘을 사용하여 클라이언트를 재시작합니다.

3. 브로커 간 통신 메커니즘을 변경하려면 (필요한 경우) server.properties에서 `sasl.mechanism.inter.broker.protocol`을 새 메커니즘으로 설정하고 클러스터를 다시 순차적으로 재시작합니다.

4. 이전 메커니즘을 제거하려면 (필요한 경우) server.properties의 `sasl.enabled.mechanisms`에서 제거하고 JAAS 구성 파일에서 항목을 제거합니다. 클러스터를 다시 순차적으로 재시작합니다.

### 4.9 위임 토큰을 사용한 인증

위임 토큰 기반 인증은 기존 SASL/SSL 방법을 보완하는 경량 인증 메커니즘입니다. 위임 토큰은 Kafka 브로커와 클라이언트 간의 공유 비밀입니다.

위임 토큰은 2방향 SSL을 사용할 때 Kerberos TGT/keytab 또는 keystore를 배포하는 추가 비용 없이 보안 환경에서 사용 가능한 워커에게 작업 부하를 분산하는 처리 프레임워크에 도움이 됩니다.

`principal.builder.class`의 기본 구현에서 위임 토큰의 소유자가 ACL 구성 등을 위한 인증된 `Principal`로 사용됩니다.

#### 일반적인 사용 단계

1. 사용자가 SASL 또는 SSL을 통해 인증하고 Admin API 또는 `kafka-delegation-tokens.sh` 스크립트를 사용하여 위임 토큰을 얻습니다.

2. 사용자가 인증을 위해 위임 토큰을 Kafka 클라이언트에 안전하게 전달합니다.

3. 토큰 소유자/갱신자가 위임 토큰을 갱신/만료할 수 있습니다.

#### 토큰 관리

비밀은 위임 토큰을 생성하고 확인하는 데 사용됩니다. 이는 `delegation.token.secret.key` 구성 옵션을 사용하여 제공됩니다. 모든 브로커에서 동일한 비밀 키를 구성해야 합니다. 컨트롤러도 동일한 구성 옵션을 사용하여 비밀로 구성해야 합니다.

비밀이 설정되지 않거나 빈 문자열로 설정되면 위임 토큰 인증 및 API 작업이 실패합니다.

토큰 세부 정보는 컨트롤러 노드의 다른 메타데이터와 함께 저장되며 위임 토큰은 컨트롤러가 개인 네트워크에 있거나 브로커와 컨트롤러 간의 모든 통신이 암호화된 경우 사용하기에 적합합니다.

현재 이 비밀은 server.properties 구성 파일에 일반 텍스트로 저장됩니다. 향후 Kafka 릴리스에서 이를 구성 가능하게 만들 예정입니다.

토큰 구성:
- 기본값: 토큰은 최대 7일 동안 24시간마다 갱신해야 함
- 구성 가능: `delegation.token.expiry.time.ms` 및 `delegation.token.max.lifetime.ms`

토큰은 명시적으로 취소할 수도 있습니다. 토큰이 만료 시간까지 갱신되지 않거나 토큰이 최대 수명을 초과하면 모든 브로커 캐시에서 삭제됩니다.

#### 위임 토큰 생성

토큰은 Admin API 또는 `kafka-delegation-tokens.sh` 스크립트를 사용하여 생성할 수 있습니다. 위임 토큰 요청(생성/갱신/만료/설명)은 SASL 또는 SSL 인증 채널에서만 발행해야 합니다.

초기 인증이 위임 토큰을 통해 수행된 경우 토큰을 요청할 수 없습니다.

사용자는 해당 사용자 또는 `--owner-principal` 매개변수를 지정하여 다른 사용자를 위해 토큰을 생성할 수 있습니다. 소유자/갱신자는 토큰을 갱신하거나 만료할 수 있습니다.

소유자/갱신자는 항상 자신의 토큰을 설명할 수 있습니다. 다른 토큰을 설명하려면 토큰 소유자를 나타내는 User 리소스에 DESCRIBE_TOKEN 권한을 추가해야 합니다.

위임 토큰 생성:
```bash
$ bin/kafka-delegation-tokens.sh --bootstrap-server localhost:9092 --create --max-life-time-period -1 --command-config client.properties --renewer-principal User:user1
```

다른 소유자를 위한 토큰 생성:
```bash
$ bin/kafka-delegation-tokens.sh --bootstrap-server localhost:9092 --create --max-life-time-period -1 --command-config client.properties --renewer-principal User:user1 --owner-principal User:owner1
```

위임 토큰 갱신:
```bash
$ bin/kafka-delegation-tokens.sh --bootstrap-server localhost:9092 --renew --renew-time-period -1 --command-config client.properties --hmac ABCDEFGHIJK
```

위임 토큰 만료:
```bash
$ bin/kafka-delegation-tokens.sh --bootstrap-server localhost:9092 --expire --expiry-time-period -1 --command-config client.properties --hmac ABCDEFGHIJK
```

기존 토큰 설명:
```bash
$ bin/kafka-delegation-tokens.sh --bootstrap-server localhost:9092 --describe --command-config client.properties --owner-principal User:user1
```

#### 토큰 인증

위임 토큰 인증은 현재 SASL/SCRAM 인증 메커니즘에 편승합니다. 여기에 설명된 대로 Kafka 클러스터에서 SASL/SCRAM 메커니즘을 활성화해야 합니다.

Kafka 클라이언트 구성:

1. producer.properties 또는 consumer.properties에서 JAAS 속성 구성:
```properties
sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required \
    username="tokenID123" \
    password="lAYYSFmLs4bTjf+lTZ1LCHR/ZZFNA==" \
    tokenauth="true";
```

`username` 및 `password` 속성은 토큰 ID 및 토큰 HMAC를 구성합니다. `tokenauth` 속성은 서버에 토큰 인증을 나타냅니다.

2. 다른 클라이언트는 다른 `sasl.jaas.config` 지정을 통해 다른 토큰을 사용하여 연결할 수 있습니다.

#### 수동 비밀 순환 절차

비밀을 순환해야 할 때 재배포가 필요합니다. 이 프로세스 중에 이미 연결된 클라이언트는 계속 작동합니다. 그러나 새 연결 요청과 이전 토큰을 사용한 갱신/만료 요청은 실패할 수 있습니다.

단계:
1. 모든 기존 토큰 만료
2. 롤링 업그레이드로 비밀 순환
3. 새 토큰 생성

향후 Kafka 릴리스에서 이를 자동화할 예정입니다.

---

## 5. 권한 부여 및 ACL

Kafka는 서버 구성의 `authorizer.class.name` 속성으로 구성되는 플러그형 권한 부여 프레임워크와 함께 제공됩니다.

KRaft 클러스터의 경우 권장 구성은 다음과 같습니다:
```properties
authorizer.class.name=org.apache.kafka.metadata.authorizer.StandardAuthorizer
```

ACL은 다음 형식을 따릅니다: "Principal {P}는 ResourcePattern {RP}와 일치하는 모든 Resource {R}에서 Host {H}로부터 Operation {O}를 [허용|거부]됨".

### 5.1 ACL이 없는 경우 기본 동작

리소스에 정의된 ACL이 없으면 Kafka는 슈퍼 사용자에게만 액세스를 제한합니다.

### 5.2 기본 동작 변경

ACL이 없을 때 무제한 액세스를 허용하려면 server.properties에 추가:
```properties
allow.everyone.if.no.acl.found=true
```

세미콜론으로 구분된 principal로 슈퍼 사용자 정의:
```properties
super.users=User:Bob;User:Alice
```

### 5.3 KRaft Principal 전달

KRaft 환경에서 관리 요청은 먼저 브로커 리스너에 도달한 다음 구성된 컨트롤러 리스너를 통해 활성 컨트롤러로 전달됩니다. 권한 부여는 클라이언트 요청과 클라이언트 principal을 모두 패키징하는 Envelope 요청을 사용하여 컨트롤러 노드에서 발생합니다.

사용자 정의된 principal은 적절한 직렬화/역직렬화를 위해 `org.apache.kafka.common.security.auth.KafkaPrincipalSerde`를 구현해야 합니다.

### 5.4 SSL 사용자 이름 사용자 정의

기본 SSL 사용자 이름은 X.500 고유 이름 형식을 사용합니다. `ssl.principal.mapping.rules`를 통해 사용자 정의:

```
RULE:pattern/replacement/
RULE:pattern/replacement/[LU]
```

예시 규칙:
```properties
RULE:^CN=(.*?),OU=ServiceUsers.*$/$1/,
RULE:^CN=(.*?),OU=(.*?),O=(.*?),L=(.*?),ST=(.*?),C=(.*?)$/$1@$2/L,
```

대안: 사용자 정의 PrincipalBuilder 클래스 설정:
```properties
principal.builder.class=CustomizedPrincipalBuilderClass
```

### 5.5 SASL 사용자 이름 사용자 정의

`sasl.kerberos.principal.to.local.rules`를 통해 Kerberos principal 변환 구성:

```
RULE:[n:string](regexp)s/pattern/replacement/
RULE:[n:string](regexp)s/pattern/replacement/g
RULE:[n:string](regexp)s/pattern/replacement//L
```

user@MYDOMAIN.COM 변환 예시:
```properties
sasl.kerberos.principal.to.local.rules=RULE:[1:$1@$0](.*@MYDOMAIN.COM)s/@.*//,DEFAULT
```

### 5.6 명령줄 인터페이스: kafka-acls.sh

주요 옵션:

| 옵션 | 설명 | 유형 |
|------|------|------|
| `--add` | ACL 추가 | 작업 |
| `--remove` | ACL 제거 | 작업 |
| `--list` | ACL 나열 | 작업 |
| `--bootstrap-server` | 브로커 연결을 위한 호스트/포트 쌍 | 구성 |
| `--bootstrap-controller` | 컨트롤러 연결을 위한 호스트/포트 쌍 | 구성 |
| `--command-config` | Admin Client 구성을 위한 속성 파일 | 구성 |
| `--cluster` | 클러스터 리소스와 상호 작용 | 리소스 패턴 |
| `--topic [name]` | 토픽 리소스와 상호 작용 | 리소스 패턴 |
| `--group [name]` | 컨슈머 그룹 리소스와 상호 작용 | 리소스 패턴 |
| `--transactional-id` | ACL용 TransactionalId | 리소스 패턴 |
| `--delegation-token` | ACL용 위임 토큰 | 리소스 패턴 |
| `--user-principal` | 사용자 리소스 (위임 토큰) | 리소스 패턴 |
| `--resource-pattern-type` | 패턴 유형: literal, prefixed, any, match | 구성 |
| `--allow-principal` | 허용 권한을 가진 Principal (PrincipalType:name) | Principal |
| `--deny-principal` | 거부 권한을 가진 Principal | Principal |
| `--principal` | --list 작업을 위한 Principal | Principal |
| `--allow-host` | 허용된 액세스를 위한 IP 주소 | 호스트 |
| `--deny-host` | 거부된 액세스를 위한 IP 주소 | 호스트 |
| `--operation` | 작업 유형 (Read, Write, Create, Delete, Alter, Describe, ClusterAction, DescribeConfigs, AlterConfigs, IdempotentWrite, CreateTokens, DescribeTokens, All) | 작업 |
| `--producer` | 프로듀서 역할을 위한 편의 옵션 (토픽에 대해 WRITE, DESCRIBE, CREATE) | 편의 |
| `--consumer` | 컨슈머 역할을 위한 편의 옵션 (토픽에 대해 READ, DESCRIBE; 그룹에 대해 READ) | 편의 |
| `--idempotent` | 프로듀서 멱등성 활성화 (--producer와 함께) | 편의 |
| `--force` | 모든 쿼리에 예라고 가정 | 편의 |

### 5.7 예시

#### ACL 추가

Bob과 Alice를 위한 기본 예시:
```bash
$ bin/kafka-acls.sh --bootstrap-server localhost:9092 --add \
  --allow-principal User:Bob --allow-principal User:Alice \
  --allow-host 198.51.100.0 --allow-host 198.51.100.1 \
  --operation Read --operation Write --topic Test-topic
```

특정 IP에서 BadBob을 제외한 모든 사용자 허용:
```bash
$ bin/kafka-acls.sh --bootstrap-server localhost:9092 --add \
  --allow-principal User:'*' --allow-host '*' \
  --deny-principal User:BadBob --deny-host 198.51.100.3 \
  --operation Read --topic Test-topic
```

모든 토픽에 대한 프로듀서:
```bash
$ bin/kafka-acls.sh --bootstrap-server localhost:9092 --add \
  --allow-principal User:Peter --allow-host 198.51.200.1 \
  --producer --topic '*'
```

접두사 리소스 패턴:
```bash
$ bin/kafka-acls.sh --bootstrap-server localhost:9092 --add \
  --allow-principal User:Jane --producer --topic Test- \
  --resource-pattern-type prefixed
```

#### ACL 제거

```bash
$ bin/kafka-acls.sh --bootstrap-server localhost:9092 --remove \
  --allow-principal User:Bob --allow-principal User:Alice \
  --allow-host 198.51.100.0 --allow-host 198.51.100.1 \
  --operation Read --operation Write --topic Test-topic
```

접두사 패턴 제거:
```bash
$ bin/kafka-acls.sh --bootstrap-server localhost:9092 --remove \
  --allow-principal User:Jane --producer --topic Test- \
  --resource-pattern-type Prefixed
```

#### ACL 나열

리터럴 리소스 패턴:
```bash
$ bin/kafka-acls.sh --bootstrap-server localhost:9092 --list --topic Test-topic
```

와일드카드 리소스:
```bash
$ bin/kafka-acls.sh --bootstrap-server localhost:9092 --list --topic '*'
```

리소스에 영향을 주는 모든 ACL (와일드카드 및 접두사 포함):
```bash
$ bin/kafka-acls.sh --bootstrap-server localhost:9092 --list \
  --topic Test-topic --resource-pattern-type match
```

#### 프로듀서/컨슈머 편의 옵션

Bob을 프로듀서로 추가:
```bash
$ bin/kafka-acls.sh --bootstrap-server localhost:9092 --add \
  --allow-principal User:Bob --producer --topic Test-topic
```

Alice를 컨슈머로 추가:
```bash
$ bin/kafka-acls.sh --bootstrap-server localhost:9092 --add \
  --allow-principal User:Alice --consumer --topic Test-topic --group Group-1
```

### 5.8 권한 부여 기본 요소

#### Kafka의 작업 (Operations)

- Read
- Write
- Create
- Delete
- Alter
- Describe
- ClusterAction
- DescribeConfigs
- AlterConfigs
- IdempotentWrite
- CreateTokens
- DescribeTokens
- All

#### Kafka의 리소스 (Resources)

Topic: 토픽을 나타냅니다. 토픽에 작용하는 프로토콜 호출에는 해당 권한이 필요합니다. 권한 부여 오류는 TOPIC_AUTHORIZATION_FAILED (오류 코드 29)를 반환합니다.

Group: 컨슈머 그룹을 나타냅니다. 그룹과 함께 작동하는 프로토콜 호출에는 권한이 필요합니다. 권한 부여 오류는 GROUP_AUTHORIZATION_FAILED (오류 코드 30)를 반환합니다.

Cluster: 클러스터를 나타냅니다. 전체 클러스터에 영향을 주는 작업에는 클러스터 권한이 필요합니다. 권한 부여 오류는 CLUSTER_AUTHORIZATION_FAILED (오류 코드 31)를 반환합니다.

TransactionalId: 트랜잭션 관련 작업을 나타냅니다. 권한 부여 오류는 TRANSACTIONAL_ID_AUTHORIZATION_FAILED (오류 코드 53)를 반환합니다.

DelegationToken: 위임 토큰을 나타냅니다. 특수 동작 세부 사항은 KIP-48을 참조하세요.

User: CreateToken 및 DescribeToken 작업은 다른 사용자의 토큰을 관리하기 위해 User 리소스에 부여할 수 있습니다 (KIP-373).

### 5.9 프로토콜에 대한 작업 및 리소스

다음은 주요 Kafka 프로토콜과 필요한 작업 및 리소스의 매핑입니다:

| 프로토콜 | 필요한 작업 | 리소스 |
|----------|------------|--------|
| PRODUCE | Write | TransactionalId (트랜잭셔널 프로듀서) |
| | IdempotentWrite | Cluster (멱등성 프로듀서) |
| | Write | Topic (일반 프로듀서) |
| FETCH | ClusterAction | Cluster (팔로워가 파티션 데이터 가져오기) |
| | Read | Topic (일반 컨슈머) |
| LIST_OFFSETS | Describe | Topic |
| METADATA | Describe | Topic |
| | Create | Cluster/Topic (자동 생성 활성화 시) |
| OFFSET_COMMIT | Read | Group, Topic |
| OFFSET_FETCH | Describe | Group, Topic |
| FIND_COORDINATOR | Describe | Group/TransactionalId |
| JOIN_GROUP | Read | Group |
| HEARTBEAT | Read | Group |
| LEAVE_GROUP | Read | Group |
| SYNC_GROUP | Read | Group |
| DESCRIBE_GROUPS | Describe | Group |
| LIST_GROUPS | Describe | Cluster, Group |
| CREATE_TOPICS | Create | Cluster/Topic |
| DELETE_TOPICS | Delete | Topic |
| DELETE_RECORDS | Delete | Topic |
| INIT_PRODUCER_ID | Write | TransactionalId |
| | IdempotentWrite | Cluster |
| ADD_PARTITIONS_TO_TXN | Write | TransactionalId, Topic |
| ADD_OFFSETS_TO_TXN | Write | TransactionalId |
| | Read | Group |
| END_TXN | Write | TransactionalId |
| DESCRIBE_ACLS | Describe | Cluster |
| CREATE_ACLS | Alter | Cluster |
| DELETE_ACLS | Alter | Cluster |
| DESCRIBE_CONFIGS | DescribeConfigs | Cluster/Topic |
| ALTER_CONFIGS | AlterConfigs | Cluster/Topic |
| CREATE_PARTITIONS | Alter | Topic |
| CREATE_DELEGATION_TOKEN | CreateTokens | User |
| DESCRIBE_DELEGATION_TOKEN | Describe | DelegationToken |
| | DescribeTokens | User |
| DELETE_GROUPS | Delete | Group |
| DESCRIBE_CLUSTER | Describe | Cluster |
| DESCRIBE_PRODUCERS | Read | Topic |

---

## 6. 운영 중인 클러스터에 보안 기능 적용

이 문서는 활성 Kafka 클러스터를 보안하기 위한 단계적 접근 방식을 설명합니다. 이 프로세스는 추가 보안 포트를 열기 위해 클러스터 노드를 순차적으로 재시작한 후 클라이언트를 재구성하고 브로커 간 보안을 활성화하기 전에 추가 서버 재시작을 수행하고 마지막으로 비보안 포트를 닫는 것을 포함합니다.

### 6.1 주요 구현 단계

보안 구현을 통해 브로커-클라이언트 및 브로커-브로커 통신 모두에 대해 다른 프로토콜을 구성할 수 있으며, 이는 별도의 재시작에서 활성화해야 합니다.

### 6.2 운영 요구 사항

클러스터 수정 중에 관리자는 SIGTERM을 통해 브로커를 깨끗하게 중지하고 다음 노드로 이동하기 전에 재시작된 복제본이 ISR 목록에 복귀할 때까지 기다려야 합니다.

### 6.3 SSL 구성 시나리오 예시

첫 번째 재시작:
```properties
listeners=PLAINTEXT://broker1:9091,SSL://broker1:9092
```

클라이언트 구성 업데이트:
```properties
bootstrap.servers = [broker1:9092,...]
security.protocol = SSL
```

두 번째 재시작:
```properties
listeners=PLAINTEXT://broker1:9091,SSL://broker1:9092
security.inter.broker.protocol=SSL
```

최종 재시작:
```properties
listeners=SSL://broker1:9092
security.inter.broker.protocol=SSL
```

### 6.4 다중 프로토콜 예시 (SSL + SASL)

여러 포트를 여는 예시:
```properties
listeners=PLAINTEXT://broker1:9091,SSL://broker1:9092,SASL_SSL://broker1:9093
```

클라이언트는 다음을 사용합니다:
```properties
bootstrap.servers = [broker1:9093,...]
security.protocol = SASL_SSL
```

최종 구성은 암호화된 브로커 간 통신과 인증된 클라이언트 통신을 유지하면서 일반 텍스트 액세스를 닫습니다.

---

## 참고 자료

- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [KIP-48: Delegation Token](https://cwiki.apache.org/confluence/display/KAFKA/KIP-48+Delegation+token+support+for+Kafka)
- [KIP-373: User resource type for delegation tokens](https://cwiki.apache.org/confluence/display/KAFKA/KIP-373%3A+Modify+DelegationToken+operations+to+use+User+resource+type)
- [RFC 5802: SCRAM](https://tools.ietf.org/html/rfc5802)
- [RFC 7677: SCRAM-SHA-256](https://tools.ietf.org/html/rfc7677)
- [RFC 7628: SASL OAUTHBEARER](https://tools.ietf.org/html/rfc7628)

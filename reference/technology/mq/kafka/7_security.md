# Kafka 보안

## Kafka 보안

> 원본: https://kafka.apache.org/documentation/#security

---

### 목차

1. [보안 개요](#1-보안-개요)
2. [리스너 구성](#2-리스너-구성)
3. [SSL을 사용한 암호화 및 인증](#3-ssl을-사용한-암호화-및-인증)
4. [SASL을 사용한 인증](#4-sasl을-사용한-인증)
5. [권한 부여 및 ACL](#5-권한-부여-및-acl)
6. [운영 중인 클러스터에 보안 기능 적용](#6-운영-중인-클러스터에-보안-기능-적용)

---

### 1. 보안 개요

Apache Kafka는 다음과 같은 보안 기능을 제공함:

#### 1.1 인증 (Authentication)

프로듀서·컨슈머·다른 브로커·도구 등 클라이언트에서 브로커로의 연결을 SSL 또는 SASL로 인증.

지원되는 SASL 메커니즘:
- SASL/GSSAPI (Kerberos) - 버전 0.9.0.0 이상
- SASL/PLAIN - 버전 0.10.0.0 이상
- SASL/SCRAM-SHA-256 및 SASL/SCRAM-SHA-512 - 버전 0.10.2.0 이상
- SASL/OAUTHBEARER - 버전 2.0 이상

#### 1.2 암호화 (Encryption)

SSL을 사용하여 브로커-클라이언트, 브로커 간, 브로커-도구 간 전송 데이터를 암호화. 암호화를 활성화하면 CPU 유형과 JVM 구현에 따라 성능 영향 발생 가능.

#### 1.3 권한 부여 (Authorization)

클라이언트의 읽기/쓰기 작업에 대한 권한을 제어. 플러그인 방식으로 외부 권한 부여 시스템과 통합 가능.

#### 1.4 유연성

보안 기능은 선택적으로 적용 가능. 개발 환경에서는 보안 설정 없이 클러스터를 운영하거나, 인증/비인증 클라이언트, 암호화/비암호화 클라이언트를 단일 배포에서 혼합하여 지원 가능.

---

### 2. 리스너 구성

Kafka 클러스터 보호 → 서버 통신에 사용되는 채널을 안전하게 구성 필요. 각 서버는 클라이언트 및 서버 간 요청을 수신할 리스너를 정의하고, 인증 메커니즘과 트래픽 암호화를 설정 가능.

#### 2.1 리스너 구성 형식

Kafka 서버는 `listeners` 속성에 쉼표로 구분된 값을 지정하여 여러 포트 연결을 허용:

```
{LISTENER_NAME}://{hostname}:{port}
```

예시:
```
listeners=CLIENT://localhost:9092
```

#### 2.2 보안 프로토콜 매핑

`listener.security.protocol.map` 설정은 리스너에 보안 프로토콜을 매핑:

```
listener.security.protocol.map=CLIENT:SSL,BROKER:PLAINTEXT
```

사용 가능한 보안 프로토콜 (대소문자 구분 없음):
1. `PLAINTEXT` - 보안 없음
2. `SSL` - SSL/TLS 암호화
3. `SASL_PLAINTEXT` - SASL 인증 (암호화 없음)
4. `SASL_SSL` - SASL 인증 + SSL/TLS 암호화

#### 2.3 대안적 리스너 정의

프로토콜 이름을 직접 사용 가능:

```
listeners=SSL://localhost:9092,PLAINTEXT://localhost:9093
```

그러나 명확성을 위해 명시적 이름 지정 권장.

#### 2.4 브로커 간 통신

`inter.broker.listener.name` 설정은 브로커 간 파티션 복제에 사용할 리스너를 지정. 지정하지 않으면 `security.inter.broker.protocol` 설정이 적용됨 (기본값: PLAINTEXT).

#### 2.5 KRaft 클러스터 구성

KRaft 아키텍처에서 브로커(`broker` 역할)와 컨트롤러(`controller` 역할)는 별도의 리스너 필요:

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

#### 2.6 클라이언트 연결

일반적으로 클라이언트 리스너는 클러스터 내부 통신과 분리하여 네트워크 격리 제공. 컨트롤러는 클라이언트와 직접 통신하지 않으므로 격리된 상태로 유지 필요. 클라이언트 요청이 컨트롤러를 대상으로 하는 경우에는 자동으로 전달됨.

---

### 3. SSL을 사용한 암호화 및 인증

Apache Kafka는 클라이언트의 트래픽 암호화 및 인증에 SSL 지원. 기본적으로 SSL은 비활성화 상태이며 필요에 따라 활성화 가능.

#### 3.1 각 Kafka 브로커용 SSL 키 및 인증서 생성

초기 설정 단계에서는 모든 서버에 공개/개인 키 쌍 생성 필요. Kafka는 키와 인증서를 Java keytool 명령으로 keystore에 저장하는 방식을 사용함. keystore 형식은 두 가지: 더 이상 사용을 권장하지 않는 Java 전용 JKS 형식과 PKCS12 (Java 9부터 기본값).

명령:
```bash
$ keytool -keystore {keystorefile} -alias localhost -validity {validity} -genkey -keyalg RSA -storetype pkcs12
```

매개변수:
- `keystorefile`: 브로커의 키와 나중에 인증서를 저장하며, 개인 키와 공개 키를 포함하므로 안전하게 저장 필요
- `validity`: 키 유효 기간(일 단위), 서명 시 결정되는 인증서 유효 기간과 다름

인증서 서명 요청 생성:
```bash
$ keytool -keystore server.keystore.jks -alias localhost -validity {validity} -genkey -keyalg RSA -destkeystoretype pkcs12 -ext SAN=DNS:{FQDN},IP:{IPADDRESS1}
```

#### 3.2 호스트 이름 확인

호스트 이름 확인은 연결 대상 서버가 제시하는 인증서 속성을 해당 서버의 실제 호스트 이름 또는 IP 주소와 대조하는 과정임.

주요 목적은 중간자 공격(man-in-the-middle attack) 방지. Kafka 2.0.0부터 클라이언트 연결과 브로커 간 연결 모두에서 서버 호스트 이름 확인이 기본으로 활성화됨.

확인을 비활성화하려면 `ssl.endpoint.identification.algorithm`을 빈 문자열로 설정.

동적으로 구성된 브로커 리스너의 경우:
```bash
$ bin/kafka-configs.sh --bootstrap-server localhost:9093 --entity-type brokers --entity-name 0 --alter --add-config "listener.name.internal.ssl.endpoint.identification.algorithm="
```

> 참고: 일반적으로 호스트 이름 확인을 비활성화할 타당한 이유는 없음. 이는 단순히 '일단 동작하게 만드는' 가장 빠른 방법일 뿐임.

클라이언트는 다음에 대해 서버의 FQDN 또는 IP 주소를 확인:
1. Common Name (CN)
2. Subject Alternative Name (SAN)

SAN 필드는 훨씬 유연하여 인증서 하나에 여러 DNS 및 IP 항목 선언 가능하다는 장점 있음.

SAN 필드 추가:
```bash
$ keytool -keystore server.keystore.jks -alias localhost -validity {validity} -genkey -keyalg RSA -destkeystoretype pkcs12 -ext SAN=DNS:{FQDN},IP:{IPADDRESS1}
```

#### 3.3 자체 CA 생성

인증 기관(CA, Certificate Authority)은 인증서 서명을 담당함. CA는 여권을 발급하는 정부와 유사하게 동작함. 인증서에 서명함으로써 위조를 어렵게 만듦.

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

CA는 자체 서명된 공개/개인 키 쌍과 인증서로 구성되며, 다른 인증서에 서명하는 용도로만 사용됨.

클라이언트의 truststore에 CA 추가:
```bash
$ keytool -keystore client.truststore.jks -alias CARoot -import -file ca-cert
```

브로커의 truststore에 CA 추가 (클라이언트 인증이 필요한 경우):
```bash
$ keytool -keystore server.truststore.jks -alias CARoot -import -file ca-cert
```

각 시스템의 고유 ID를 저장하는 keystore와 달리, truststore는 클라이언트가 신뢰해야 하는 모든 인증서를 저장함.

#### 3.4 인증서 서명

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

결과로 생성된 `truststore.jks`는 모든 클라이언트와 브로커에서 동일하게 사용 가능하며, 민감한 정보를 포함하지 않으므로 별도 보호 불필요.

> 참고: easyRSA 프로젝트가 스크립팅 지원을 제공함.

#### 3.5 PEM 형식의 SSL 키 및 인증서

2.7.0 버전부터 SSL 키와 truststore를 PEM 형식으로 Kafka 브로커 및 클라이언트 구성에 직접 지정 가능.

구성:
- PEM 형식의 개인 키: `ssl.keystore.key`
- PEM 형식의 인증서 체인: `ssl.keystore.certificate.chain`
- 신뢰 인증서: `ssl.truststore.certificates`

여러 줄에 걸친 문자열은 백슬래시('\')로 줄 연속 표시.

PEM 형식에서는 `ssl.keystore.password` 및 `ssl.truststore.password` 설정 미사용. 개인 키가 암호화된 경우 `ssl.key.password`에 비밀번호를 지정할 것.

개인 키는 암호화 없이 제공 가능. 프로덕션 환경에서 이 경우에는 Kafka의 비밀번호 보호 기능을 활용하여 구성을 암호화하거나 외부화 필요.

> 참고: 기본 SSL 엔진 팩토리는 OpenSSL과 같은 외부 도구를 사용한 암호화된 개인 키 복호화에 제한된 기능을 가짐. BouncyCastle과 같은 타사 라이브러리를 사용자 정의 SslEngineFactory와 통합하여 더 넓은 범위 지원 가능.

#### 3.6 프로덕션의 일반적인 함정

프로덕션 클러스터는 일반적으로 자체 서명 인증서 대신 엔터프라이즈 CA 사용. 이 경우 몇 가지 일반적인 문제 발생 가능:

##### 3.6.1 확장 키 사용 (Extended Key Usage)

인증서에는 인증서 사용 목적을 제어하는 확장 필드 포함 가능.

Kafka 브로커는 클러스터 내부 통신에서 클라이언트와 서버 역할을 모두 수행하므로, 클라이언트 인증과 서버 인증 사용이 모두 허용되어야 함. 웹 서버 프로필 같은 기업 CA 서명 프로필에는 serverAuth만 포함되는 경우가 있어 SSL 핸드셰이크 실패 발생 가능.

##### 3.6.2 중간 인증서 (Intermediate Certificates)

보안을 위해 기업 루트 CA는 오프라인으로 유지하고, 중간 CA가 일상적인 서명 담당. 중간 CA가 서명한 인증서를 가져올 때는 루트 CA까지의 전체 신뢰 체인 제공 필요. keytool로 가져오기 전에 `cat` 명령으로 인증서 파일을 결합할 것.

##### 3.6.3 확장 필드 복사 실패

CA 운영자는 CSR에서 요청된 확장 필드를 그대로 복사하는 대신 직접 지정하는 것을 선호하는 경우가 많음. 서명된 인증서에 호스트 이름 확인에 필요한 SAN 필드가 모두 포함되어 있는지 반드시 확인할 것.

인증서 세부 정보 확인:
```bash
$ openssl x509 -in certificate.crt -text -noout
```

#### 3.7 Kafka 브로커 구성

브로커 간 통신에 SSL을 활성화하지 않는 경우 PLAINTEXT 포트와 SSL 포트가 모두 필요.

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

> 참고: `ssl.truststore.password`는 기술적으로 선택 사항이지만 강력히 권장됨. 비밀번호가 설정되지 않으면 truststore에 대한 액세스는 여전히 가능하지만 무결성 검사가 비활성화됨.

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

일부 국가의 수입 규정으로 인해 Oracle 구현은 기본적으로 사용 가능한 암호화 알고리즘의 강도를 제한함. 더 강력한 알고리즘(예: 256비트 키의 AES)이 필요한 경우 JCE Unlimited Strength Jurisdiction Policy Files를 다운로드하여 설치 필요.

의사 난수 생성기 성능:

JRE/JDK는 암호화 작업에 기본 의사 난수 생성기(PRNG)를 사용하므로 `ssl.secure.random.implementation`을 별도로 설정할 필요 없음. 다만 일부 구현(특히 Linux에서 기본으로 선택되는 NativePRNG는 전역 잠금을 사용)에서 성능 문제 발생 가능.

SHA1PRNG 구현은 비차단 방식이며 높은 부하(브로커당 복제 트래픽 포함 초당 50MB 메시지 생성) 환경에서도 우수한 성능을 보임.

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

예상 출력에는 서버 인증서 포함됨:
```
-----BEGIN CERTIFICATE-----
{variable sized random bytes}
-----END CERTIFICATE-----
subject=/C=US/ST=CA/L=Santa Clara/O=org/OU=org/CN=Sriharsha Chintalapani
issuer=/C=US/ST=CA/L=Santa Clara/O=org/OU=org/CN=kafka/emailAddress=test@test.com
```

인증서 부재 또는 오류 메시지는 부적절한 keystore 설정을 나타냄.

#### 3.8 Kafka 클라이언트 구성

SSL은 새로운 Kafka Producer 및 Consumer에서만 지원되며, 이전 API는 미지원. SSL 구성은 프로듀서와 컨슈머 모두 동일함.

클라이언트 인증 없는 최소 구성:
```properties
security.protocol=SSL
ssl.truststore.location=/var/private/ssl/client.truststore.jks
ssl.truststore.password=test1234
```

> 참고: `ssl.truststore.password`는 기술적으로 선택 사항이지만 강력히 권장됨.

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

### 4. SASL을 사용한 인증

#### 4.1 JAAS 구성

Kafka는 SASL 구성에 Java 인증 및 권한 부여 서비스(JAAS)를 사용함.

##### 브로커용

- 브로커의 JAAS 파일에서 섹션 이름은 `KafkaServer`임
- 섹션 이름 앞에 리스너 이름을 소문자로 붙이고 마침표를 붙일 수 있음 (예: `sasl_ssl.KafkaServer`)
- 브로커는 `sasl.jaas.config` 속성을 사용하여 JAAS 구성 가능
- 속성 이름 형식: `listener.name.{listenerName}.{saslMechanism}.sasl.jaas.config`
- 구성 값당 하나의 로그인 모듈만 지정 가능
- 여러 메커니즘은 각각에 대해 별도 구성 필요

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

##### 클라이언트용

- 클라이언트는 구성 속성 `sasl.jaas.config` 또는 정적 JAAS 구성 파일 사용 가능
- 동일한 JVM 내의 다른 프로듀서/컨슈머는 서로 다른 자격 증명 사용 가능
- `java.security.auth.login.config` 시스템 속성과 `sasl.jaas.config` 클라이언트 속성이 모두 지정된 경우 클라이언트 속성이 우선함

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

#### 4.2 SASL 구성

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
- SASL 인증은 새로운 Java Kafka 프로듀서 및 컨슈머에서만 지원됨. 이전 API는 미지원.
- 브로커에서 활성화된 메커니즘 선택
- `security.protocol` 및 `sasl.mechanism` 속성 구성

> DNS 관련 참고: SASL을 통해 브로커에 연결할 때 클라이언트가 브로커 주소에 대한 역방향 DNS 조회 수행 가능. JRE의 역방향 DNS 조회 구현 방식으로 인해, 클라이언트의 `bootstrap.servers`와 브로커의 `advertised.listeners` 모두에 FQDN을 사용하지 않으면 SASL 핸드셰이크가 느려질 수 있음.

#### 4.3 SASL/Kerberos를 사용한 인증

##### 전제 조건

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
   모든 호스트를 호스트 이름으로 연결할 수 있는지 확인할 것. 모든 호스트가 FQDN으로 해석될 수 있어야 하는 것은 Kerberos의 요구 사항임.

##### Kafka 브로커 구성

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

##### Kafka 클라이언트 구성

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

#### 4.4 SASL/PLAIN을 사용한 인증

SASL/PLAIN은 간단한 사용자 이름/비밀번호 인증 메커니즘으로, 안전한 인증을 위해 일반적으로 TLS 암호화와 함께 사용함.

`principal.builder.class`의 기본 구현에서 사용자 이름은 ACL 구성 등을 위한 인증된 `Principal`로 사용됨.

##### Kafka 브로커 구성

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

`username` 및 `password` 속성은 브로커 간 통신용임. `user_{userName}` 속성은 브로커에 연결하는 모든 사용자의 비밀번호를 정의함.

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

##### Kafka 클라이언트 구성

1. 클라이언트 구성에서 JAAS 속성 구성:
```properties
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required \
    username="alice" \
    password="alice-secret";
```

다른 클라이언트는 다른 `sasl.jaas.config` 지정을 통해 다른 사용자로 연결 가능.

2. 클라이언트 속성 구성:
```properties
security.protocol=SASL_SSL
sasl.mechanism=PLAIN
```

##### 프로덕션 사용

- SASL/PLAIN은 암호화 없이 일반 텍스트 비밀번호가 전송되지 않도록 SSL을 전송 계층으로만 사용 필요.
- Kafka 버전 2.0부터 `sasl.server.callback.handler.class` 및 `sasl.client.callback.handler.class` 구성 옵션을 사용하여 외부 소스에서 사용자 이름과 비밀번호를 가져오는 자체 콜백 핸들러를 구성 → 디스크에 일반 텍스트 비밀번호를 저장하지 않을 수 있음.
- Kafka 버전 2.0부터 `sasl.server.callback.handler.class`를 구성하여 비밀번호 확인을 위해 외부 인증 서버를 사용하는 자체 콜백 핸들러 플러그인 가능.

#### 4.5 SASL/SCRAM을 사용한 인증

SCRAM(Salted Challenge Response Authentication Mechanism)은 PLAIN, DIGEST-MD5 같은 기존 사용자 이름/비밀번호 인증 메커니즘의 보안 문제를 해결하기 위한 SASL 메커니즘 계열임.

Kafka는 RFC 5802 및 RFC 7677에 정의된 SCRAM-SHA-256 및 SCRAM-SHA-512 지원.

`principal.builder.class`의 기본 구현에서 사용자 이름은 ACL 구성 등을 위한 인증된 `Principal`로 사용됨.

Kafka의 기본 SCRAM 구현은 SCRAM 자격 증명을 메타데이터 로그에 저장함.

##### SCRAM 자격 증명 생성

Kafka SCRAM 구현은 메타데이터 로그를 자격 증명 저장소로 사용함. 자격 증명은 `kafka-storage.sh` 또는 `kafka-configs.sh`를 사용하여 메타데이터 로그에 생성 가능.

활성화된 각 SCRAM 메커니즘에 대해 메커니즘 이름으로 구성을 추가하여 자격 증명 생성 필요. 브로커 간 통신용 자격 증명은 Kafka 브로커를 시작하기 전에 미리 생성 필요.

admin 사용자의 초기 자격 증명 생성:
```bash
$ bin/kafka-storage.sh format -t $(bin/kafka-storage.sh random-uuid) -c config/server.properties --add-scram 'SCRAM-SHA-256=[name="admin",password="admin-secret"]'
```

alice 사용자의 자격 증명 생성:
```bash
$ bin/kafka-configs.sh --bootstrap-server localhost:9092 --alter --add-config 'SCRAM-SHA-256=[iterations=8192,password=alice-secret]' --entity-type users --entity-name alice --command-config client.properties
```

반복 횟수를 지정하지 않으면 기본값인 4096이 사용됨. salt를 지정하지 않으면 무작위 salt가 생성됨.

기존 자격 증명 나열:
```bash
$ bin/kafka-configs.sh --bootstrap-server localhost:9092 --describe --entity-type users --entity-name alice --command-config client.properties
```

자격 증명 삭제:
```bash
$ bin/kafka-configs.sh --bootstrap-server localhost:9092 --alter --delete-config 'SCRAM-SHA-256' --entity-type users --entity-name alice --command-config client.properties
```

##### Kafka 브로커 구성

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

##### Kafka 클라이언트 구성

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

##### SASL/SCRAM 보안 고려 사항

- Kafka의 기본 SASL/SCRAM 구현은 SCRAM 자격 증명을 메타데이터 로그에 저장함. KRaft 컨트롤러가 안전한 사설 네트워크에 있는 환경에서는 프로덕션 사용에 적합함.
- Kafka는 최소 반복 횟수 4096의 SHA-256 및 SHA-512 해시 함수만 지원함. 강력한 해시 함수·강력한 비밀번호·높은 반복 횟수의 조합 → KRaft 컨트롤러가 침해된 경우에도 무차별 대입 공격 방어.
- SCRAM은 교환 가로채기를 방지하기 위해 TLS 암호화와 함께만 사용 필요. 이를 통해 KRaft 컨트롤러가 침해된 상황에서도 사전/무차별 대입 공격과 사칭을 방어함.
- Kafka 2.0부터 KRaft 컨트롤러가 안전하지 않은 환경에서는 `sasl.server.callback.handler.class`를 구성하여 기본 SASL/SCRAM 자격 증명 저장소를 사용자 정의 콜백 핸들러로 재정의 가능.

#### 4.6 SASL/OAUTHBEARER를 사용한 인증

OAuth 2 인증 프레임워크는 타사 애플리케이션이 리소스 소유자를 대신하거나 자체적으로 HTTP 서비스에 대한 제한된 액세스를 얻을 수 있도록 함.

SASL OAUTHBEARER 메커니즘은 SASL(비HTTP) 컨텍스트에서 이 프레임워크를 사용 가능하게 하며 RFC 7628에 정의됨.

Kafka의 기본 OAUTHBEARER 구현은 비보안 JSON 웹 토큰을 생성하고 검증하므로, 비프로덕션 환경에서만 사용하기에 적합함.

최근 Apache Kafka 버전에는 OAuth 2.0 표준 호환 ID 공급자와의 연동을 지원하는 프로덕션용 OAUTHBEARER 구현이 추가됨.

`principal.builder.class`의 기본 구현에서 OAuthBearerToken의 principalName이 ACL 구성 등을 위한 인증된 `Principal`로 사용됨.

##### 비프로덕션 Kafka 브로커 구성

1. JAAS 구성 파일 생성:
```
KafkaServer {
    org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required
    unsecuredLoginStringClaim_sub="admin";
};
```

`unsecuredLoginStringClaim_sub` 속성은 브로커 간 연결에 사용됨.

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

##### 프로덕션 Kafka 브로커 구성

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

##### 프로덕션 Kafka 클라이언트 구성

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

##### SASL/OAUTHBEARER 보안 고려 사항

- Kafka의 기본 SASL/OAUTHBEARER 구현은 비보안 JSON 웹 토큰을 생성하고 검증하므로 비프로덕션 환경에서만 사용 필요.
- OAUTHBEARER는 토큰 가로채기를 방지하기 위해 프로덕션 환경에서 반드시 TLS 암호화와 함께 사용 필요.
- 기본 비보안 SASL/OAUTHBEARER 구현은 사용자 정의 로그인 및 SASL 서버 콜백 핸들러를 통해 재정의 가능하며, 프로덕션 환경에서는 반드시 재정의 필요.

#### 4.7 브로커에서 여러 SASL 메커니즘 활성화

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

#### 4.8 운영 중인 클러스터에서 SASL 메커니즘 수정

SASL 메커니즘은 다음 순서로 변경 가능:

1. 각 브로커의 server.properties에서 `sasl.enabled.mechanisms`에 추가하여 새 메커니즘 활성화. JAAS 구성 파일을 업데이트하여 두 메커니즘을 모두 포함. 클러스터 노드를 순차적으로 재시작.

2. 새 메커니즘을 사용하여 클라이언트 재시작.

3. 브로커 간 통신 메커니즘을 변경하려면 (필요한 경우) server.properties에서 `sasl.mechanism.inter.broker.protocol`을 새 메커니즘으로 설정하고 클러스터를 다시 순차적으로 재시작.

4. 이전 메커니즘을 제거하려면 (필요한 경우) server.properties의 `sasl.enabled.mechanisms`에서 제거하고 JAAS 구성 파일에서 항목 제거. 클러스터를 다시 순차적으로 재시작.

#### 4.9 위임 토큰을 사용한 인증

위임 토큰 기반 인증은 기존 SASL/SSL 방식을 보완하는 경량 인증 메커니즘임. 위임 토큰은 Kafka 브로커와 클라이언트 간에 공유되는 비밀임.

위임 토큰은 상호 SSL을 사용할 때 Kerberos TGT/keytab 또는 keystore 배포 비용 없이, 보안 환경에서 워커에게 작업 부하를 분산하는 처리 프레임워크에 유용함.

`principal.builder.class`의 기본 구현에서 위임 토큰의 소유자가 ACL 구성 등을 위한 인증된 `Principal`로 사용됨.

##### 일반적인 사용 단계

1. 사용자가 SASL 또는 SSL을 통해 인증하고 Admin API 또는 `kafka-delegation-tokens.sh` 스크립트를 사용하여 위임 토큰 획득.

2. 사용자가 인증을 위해 위임 토큰을 Kafka 클라이언트에 안전하게 전달.

3. 토큰 소유자/갱신자가 위임 토큰을 갱신/만료 가능.

##### 토큰 관리

비밀은 위임 토큰 생성 및 검증에 사용되며 `delegation.token.secret.key` 설정으로 제공. 모든 브로커와 컨트롤러에 동일한 비밀 키 구성 필요.

비밀이 설정되지 않거나 빈 문자열인 경우 위임 토큰 인증 및 API 작업 실패.

토큰 세부 정보는 컨트롤러 노드의 다른 메타데이터와 함께 저장됨. 위임 토큰은 컨트롤러가 사설 네트워크에 있거나 브로커-컨트롤러 간 모든 통신이 암호화된 경우에 적합함.

현재 이 비밀은 server.properties에 일반 텍스트로 저장됨. 향후 Kafka 릴리스에서 구성 방식 개선 예정.

토큰 구성:
- 기본값: 토큰은 최대 7일 동안 24시간마다 갱신 필요
- 구성 가능: `delegation.token.expiry.time.ms` 및 `delegation.token.max.lifetime.ms`

토큰은 명시적으로 취소 가능. 만료 시간까지 갱신되지 않거나 최대 수명을 초과한 토큰은 모든 브로커 캐시에서 삭제됨.

##### 위임 토큰 생성

토큰은 Admin API 또는 `kafka-delegation-tokens.sh` 스크립트로 생성 가능. 위임 토큰 요청(생성/갱신/만료/조회)은 SASL 또는 SSL 인증 채널에서만 발행 필요.

초기 인증이 위임 토큰을 통해 이루어진 경우에는 토큰 요청 불가.

사용자는 자신을 위한 토큰을 생성하거나, `--owner-principal` 매개변수를 지정하여 다른 사용자를 위한 토큰 생성 가능. 소유자/갱신자는 토큰을 갱신하거나 만료 가능.

소유자/갱신자는 항상 자신의 토큰을 조회 가능. 다른 사용자의 토큰을 조회하려면 해당 토큰 소유자를 나타내는 User 리소스에 DESCRIBE_TOKEN 권한 부여 필요.

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

##### 토큰 인증

위임 토큰 인증은 현재 SASL/SCRAM 인증 메커니즘을 기반으로 동작함. Kafka 클러스터에서 SASL/SCRAM 메커니즘 활성화 필요.

Kafka 클라이언트 구성:

1. producer.properties 또는 consumer.properties에서 JAAS 속성 구성:
```properties
sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required \
    username="tokenID123" \
    password="lAYYSFmLs4bTjf+lTZ1LCHR/ZZFNA==" \
    tokenauth="true";
```

`username` 및 `password` 속성은 토큰 ID 및 토큰 HMAC를 구성함. `tokenauth` 속성은 서버에 토큰 인증을 나타냄.

2. 다른 클라이언트는 다른 `sasl.jaas.config` 지정을 통해 다른 토큰을 사용하여 연결 가능.

##### 수동 비밀 순환 절차

비밀을 순환할 때는 재배포 필요. 이 과정에서 이미 연결된 클라이언트는 계속 동작하지만, 새 연결 요청과 이전 토큰을 사용한 갱신/만료 요청은 실패 가능.

단계:
1. 모든 기존 토큰 만료
2. 롤링 업그레이드로 비밀 순환
3. 새 토큰 생성

향후 Kafka 릴리스에서 이를 자동화할 예정.

---

### 5. 권한 부여 및 ACL

Kafka는 서버 구성의 `authorizer.class.name` 속성으로 설정하는 플러그인 방식의 권한 부여 프레임워크를 제공함.

KRaft 클러스터의 경우 권장 구성:
```properties
authorizer.class.name=org.apache.kafka.metadata.authorizer.StandardAuthorizer
```

ACL은 다음 형식을 따름: "Principal {P}는 ResourcePattern {RP}와 일치하는 모든 Resource {R}에서 Host {H}로부터 Operation {O}를 [허용|거부]함".

#### 5.1 ACL이 없는 경우 기본 동작

리소스에 정의된 ACL이 없으면 Kafka는 슈퍼 사용자만 접근 허용.

#### 5.2 기본 동작 변경

ACL이 없을 때 무제한 접근을 허용하려면 server.properties에 추가:
```properties
allow.everyone.if.no.acl.found=true
```

세미콜론으로 구분된 principal로 슈퍼 사용자 정의:
```properties
super.users=User:Bob;User:Alice
```

#### 5.3 KRaft Principal 전달

KRaft 환경에서 관리 요청은 먼저 브로커 리스너에 도달 → 구성된 컨트롤러 리스너를 통해 활성 컨트롤러로 전달됨. 권한 부여는 클라이언트 요청과 클라이언트 principal을 함께 패키징한 Envelope 요청을 사용하여 컨트롤러 노드에서 수행됨.

사용자 정의 principal은 올바른 직렬화/역직렬화를 위해 `org.apache.kafka.common.security.auth.KafkaPrincipalSerde` 구현 필요.

#### 5.4 SSL 사용자 이름 사용자 정의

기본 SSL 사용자 이름은 X.500 고유 이름 형식을 사용함. `ssl.principal.mapping.rules`로 사용자 정의 가능:

```
RULE:pattern/replacement/
RULE:pattern/replacement/[LU]
```

예시 규칙:
```properties
RULE:^CN=(.*?),OU=ServiceUsers.*$/$1/,
RULE:^CN=(.*?),OU=(.*?),O=(.*?),L=(.*?),ST=(.*?),C=(.*?)$/$1@$2/L,
```

대안으로 사용자 정의 PrincipalBuilder 클래스 설정 가능:
```properties
principal.builder.class=CustomizedPrincipalBuilderClass
```

#### 5.5 SASL 사용자 이름 사용자 정의

`sasl.kerberos.principal.to.local.rules`로 Kerberos principal 변환 구성:

```
RULE:[n:string](regexp)s/pattern/replacement/
RULE:[n:string](regexp)s/pattern/replacement/g
RULE:[n:string](regexp)s/pattern/replacement//L
```

user@MYDOMAIN.COM 변환 예시:
```properties
sasl.kerberos.principal.to.local.rules=RULE:[1:$1@$0](.*@MYDOMAIN.COM)s/@.*//,DEFAULT
```

#### 5.6 명령줄 인터페이스: kafka-acls.sh

주요 옵션:

- `--add`
  - 설명: ACL 추가
  - 유형: 작업
- `--remove`
  - 설명: ACL 제거
  - 유형: 작업
- `--list`
  - 설명: ACL 나열
  - 유형: 작업
- `--bootstrap-server`
  - 설명: 브로커 연결을 위한 호스트/포트 쌍
  - 유형: 구성
- `--bootstrap-controller`
  - 설명: 컨트롤러 연결을 위한 호스트/포트 쌍
  - 유형: 구성
- `--command-config`
  - 설명: Admin Client 구성을 위한 속성 파일
  - 유형: 구성
- `--cluster`
  - 설명: 클러스터 리소스와 상호 작용
  - 유형: 리소스 패턴
- `--topic [name]`
  - 설명: 토픽 리소스와 상호 작용
  - 유형: 리소스 패턴
- `--group [name]`
  - 설명: 컨슈머 그룹 리소스와 상호 작용
  - 유형: 리소스 패턴
- `--transactional-id`
  - 설명: ACL용 TransactionalId
  - 유형: 리소스 패턴
- `--delegation-token`
  - 설명: ACL용 위임 토큰
  - 유형: 리소스 패턴
- `--user-principal`
  - 설명: 사용자 리소스 (위임 토큰)
  - 유형: 리소스 패턴
- `--resource-pattern-type`
  - 설명: 패턴 유형: literal, prefixed, any, match
  - 유형: 구성
- `--allow-principal`
  - 설명: 허용 권한을 가진 Principal (PrincipalType:name)
  - 유형: Principal
- `--deny-principal`
  - 설명: 거부 권한을 가진 Principal
  - 유형: Principal
- `--principal`
  - 설명: --list 작업을 위한 Principal
  - 유형: Principal
- `--allow-host`
  - 설명: 허용된 액세스를 위한 IP 주소
  - 유형: 호스트
- `--deny-host`
  - 설명: 거부된 액세스를 위한 IP 주소
  - 유형: 호스트
- `--operation`
  - 설명: 작업 유형 (Read, Write, Create, Delete, Alter, Describe, ClusterAction, DescribeConfigs, AlterConfigs, IdempotentWrite, CreateTokens, DescribeTokens, All)
  - 유형: 작업
- `--producer`
  - 설명: 프로듀서 역할을 위한 편의 옵션 (토픽에 대해 WRITE, DESCRIBE, CREATE)
  - 유형: 편의
- `--consumer`
  - 설명: 컨슈머 역할을 위한 편의 옵션 (토픽에 대해 READ, DESCRIBE; 그룹에 대해 READ)
  - 유형: 편의
- `--idempotent`
  - 설명: 프로듀서 멱등성 활성화 (--producer와 함께)
  - 유형: 편의
- `--force`
  - 설명: 모든 쿼리에 예라고 가정
  - 유형: 편의

#### 5.7 예시

##### ACL 추가

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

##### ACL 제거

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

##### ACL 나열

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

##### 프로듀서/컨슈머 편의 옵션

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

#### 5.8 권한 부여 기본 요소

##### Kafka의 작업 (Operations)

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

##### Kafka의 리소스 (Resources)

Topic: 토픽을 나타냄. 토픽에 작용하는 프로토콜 호출에는 해당 권한 필요. 권한 부여 오류는 TOPIC_AUTHORIZATION_FAILED (오류 코드 29) 반환.

Group: 컨슈머 그룹을 나타냄. 그룹과 함께 작동하는 프로토콜 호출에는 권한 필요. 권한 부여 오류는 GROUP_AUTHORIZATION_FAILED (오류 코드 30) 반환.

Cluster: 클러스터를 나타냄. 전체 클러스터에 영향을 주는 작업에는 클러스터 권한 필요. 권한 부여 오류는 CLUSTER_AUTHORIZATION_FAILED (오류 코드 31) 반환.

TransactionalId: 트랜잭션 관련 작업을 나타냄. 권한 부여 오류는 TRANSACTIONAL_ID_AUTHORIZATION_FAILED (오류 코드 53) 반환.

DelegationToken: 위임 토큰을 나타냄. 특수 동작 세부 사항은 KIP-48 참조.

User: CreateToken 및 DescribeToken 작업은 다른 사용자의 토큰을 관리하기 위해 User 리소스에 부여 가능 (KIP-373).

#### 5.9 프로토콜에 대한 작업 및 리소스

다음은 주요 Kafka 프로토콜과 필요한 작업 및 리소스의 매핑임:

- PRODUCE
  - 필요한 작업: Write
  - 리소스: TransactionalId (트랜잭셔널 프로듀서)
- 
  - 필요한 작업: IdempotentWrite
  - 리소스: Cluster (멱등성 프로듀서)
- 
  - 필요한 작업: Write
  - 리소스: Topic (일반 프로듀서)
- FETCH
  - 필요한 작업: ClusterAction
  - 리소스: Cluster (팔로워가 파티션 데이터 가져오기)
- 
  - 필요한 작업: Read
  - 리소스: Topic (일반 컨슈머)
- LIST_OFFSETS
  - 필요한 작업: Describe
  - 리소스: Topic
- METADATA
  - 필요한 작업: Describe
  - 리소스: Topic
- 
  - 필요한 작업: Create
  - 리소스: Cluster/Topic (자동 생성 활성화 시)
- OFFSET_COMMIT
  - 필요한 작업: Read
  - 리소스: Group, Topic
- OFFSET_FETCH
  - 필요한 작업: Describe
  - 리소스: Group, Topic
- FIND_COORDINATOR
  - 필요한 작업: Describe
  - 리소스: Group/TransactionalId
- JOIN_GROUP
  - 필요한 작업: Read
  - 리소스: Group
- HEARTBEAT
  - 필요한 작업: Read
  - 리소스: Group
- LEAVE_GROUP
  - 필요한 작업: Read
  - 리소스: Group
- SYNC_GROUP
  - 필요한 작업: Read
  - 리소스: Group
- DESCRIBE_GROUPS
  - 필요한 작업: Describe
  - 리소스: Group
- LIST_GROUPS
  - 필요한 작업: Describe
  - 리소스: Cluster, Group
- CREATE_TOPICS
  - 필요한 작업: Create
  - 리소스: Cluster/Topic
- DELETE_TOPICS
  - 필요한 작업: Delete
  - 리소스: Topic
- DELETE_RECORDS
  - 필요한 작업: Delete
  - 리소스: Topic
- INIT_PRODUCER_ID
  - 필요한 작업: Write
  - 리소스: TransactionalId
- 
  - 필요한 작업: IdempotentWrite
  - 리소스: Cluster
- ADD_PARTITIONS_TO_TXN
  - 필요한 작업: Write
  - 리소스: TransactionalId, Topic
- ADD_OFFSETS_TO_TXN
  - 필요한 작업: Write
  - 리소스: TransactionalId
- 
  - 필요한 작업: Read
  - 리소스: Group
- END_TXN
  - 필요한 작업: Write
  - 리소스: TransactionalId
- DESCRIBE_ACLS
  - 필요한 작업: Describe
  - 리소스: Cluster
- CREATE_ACLS
  - 필요한 작업: Alter
  - 리소스: Cluster
- DELETE_ACLS
  - 필요한 작업: Alter
  - 리소스: Cluster
- DESCRIBE_CONFIGS
  - 필요한 작업: DescribeConfigs
  - 리소스: Cluster/Topic
- ALTER_CONFIGS
  - 필요한 작업: AlterConfigs
  - 리소스: Cluster/Topic
- CREATE_PARTITIONS
  - 필요한 작업: Alter
  - 리소스: Topic
- CREATE_DELEGATION_TOKEN
  - 필요한 작업: CreateTokens
  - 리소스: User
- DESCRIBE_DELEGATION_TOKEN
  - 필요한 작업: Describe
  - 리소스: DelegationToken
- 
  - 필요한 작업: DescribeTokens
  - 리소스: User
- DELETE_GROUPS
  - 필요한 작업: Delete
  - 리소스: Group
- DESCRIBE_CLUSTER
  - 필요한 작업: Describe
  - 리소스: Cluster
- DESCRIBE_PRODUCERS
  - 필요한 작업: Read
  - 리소스: Topic

---

### 6. 운영 중인 클러스터에 보안 기능 적용

활성 Kafka 클러스터에 보안을 적용하는 단계적 접근 방식임. 클러스터 노드를 순차적으로 재시작하여 보안 포트를 추가로 열고, 클라이언트를 재구성한 뒤, 브로커 간 보안을 활성화하기 위한 추가 서버 재시작을 수행하고, 마지막으로 비보안 포트를 닫음.

#### 6.1 주요 구현 단계

보안 구현을 통해 브로커-클라이언트 및 브로커 간 통신 모두에 서로 다른 프로토콜 구성 가능하며, 각각 별도의 재시작으로 활성화 필요.

#### 6.2 운영 요구 사항

클러스터 수정 중에 관리자는 SIGTERM으로 브로커를 정상 종료한 뒤, 다음 노드로 이동하기 전에 재시작된 복제본이 ISR 목록에 복귀할 때까지 대기 필요.

#### 6.3 SSL 구성 시나리오 예시

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

#### 6.4 다중 프로토콜 예시 (SSL + SASL)

여러 포트를 여는 예시:
```properties
listeners=PLAINTEXT://broker1:9091,SSL://broker1:9092,SASL_SSL://broker1:9093
```

클라이언트는 다음을 사용함:
```properties
bootstrap.servers = [broker1:9093,...]
security.protocol = SASL_SSL
```

최종 구성은 암호화된 브로커 간 통신과 인증된 클라이언트 통신을 유지하면서 일반 텍스트 접근을 차단함.

---

### 참고 자료

- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [KIP-48: Delegation Token](https://cwiki.apache.org/confluence/display/KAFKA/KIP-48+Delegation+token+support+for+Kafka)
- [KIP-373: User resource type for delegation tokens](https://cwiki.apache.org/confluence/display/KAFKA/KIP-373%3A+Modify+DelegationToken+operations+to+use+User+resource+type)
- [RFC 5802: SCRAM](https://tools.ietf.org/html/rfc5802)
- [RFC 7677: SCRAM-SHA-256](https://tools.ietf.org/html/rfc7677)
- [RFC 7628: SASL OAUTHBEARER](https://tools.ietf.org/html/rfc7628)

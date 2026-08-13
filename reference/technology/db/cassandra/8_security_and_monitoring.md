# Cassandra 보안, 메트릭, 모니터링

## Cassandra 보안

> 원본: https://cassandra.apache.org/doc/latest/cassandra/managing/operating/security.html

---

### 목차

1. [개요](#개요)
2. [TLS/SSL 암호화](#tlsssl-암호화)
   - [일반 원칙](#일반-원칙)
   - [SSL 인증서 핫 리로딩](#ssl-인증서-핫-리로딩)
   - [PEM 기반 키 자료](#pem-기반-키-자료)
   - [노드 간(Inter-node) 암호화](#노드-간inter-node-암호화)
   - [클라이언트-노드(Client-to-node) 암호화](#클라이언트-노드client-to-node-암호화)
3. [역할(Roles)](#역할roles)
4. [인증(Authentication)](#인증authentication)
   - [기본 인증기(Authenticator)](#기본-인증기authenticator)
   - [비밀번호 인증 활성화](#비밀번호-인증-활성화)
5. [권한 부여(Authorization)](#권한-부여authorization)
   - [기본 권한 부여기(Authorizer)](#기본-권한-부여기authorizer)
   - [내부 권한 부여 활성화](#내부-권한-부여-활성화)
6. [캐싱(Caching)](#캐싱caching)
7. [JMX 접근 제어](#jmx-접근-제어)
   - [표준 JMX 인증](#표준-jmx-인증)
   - [Cassandra 통합 인증 및 권한 부여](#cassandra-통합-인증-및-권한-부여)
   - [JMX와 SSL](#jmx와-ssl)
8. [암호화 공급자(Crypto Providers)](#암호화-공급자crypto-providers)
9. [감사 로깅(Audit Logging)](#감사-로깅audit-logging)
   - [기록되는 항목](#기록되는-항목)
   - [cassandra.yaml을 통한 설정](#cassandrayaml을-통한-설정)
   - [nodetool 명령](#nodetool-명령)
   - [감사 로그 파일 조회](#감사-로그-파일-조회)
   - [BinAuditLogger 설정](#binauditlogger-설정)
   - [FileAuditLogger 설정](#fileauditlogger-설정)
10. [참고 자료](#참고-자료)

---

### 개요

- Cassandra는 3가지 주요 보안 구성 요소 제공
  - 클라이언트-클러스터 간·클러스터 내 노드 간 통신용 TLS/SSL 암호화(encryption)
  - 클라이언트 인증(authentication)
  - 권한 부여(authorization)
- 기본값은 세 기능 모두 비활성화 상태 → 상당한 공격 표면(attack surface) 노출됨
- 클러스터를 안전하게 구성하려면 세 요소 모두 이해 필요

세 가지 보안 영역:

- TLS/SSL 암호화(encryption): 전송 중인 데이터(data in flight)의 안전한 전송 보장
- 인증(authentication): 클라이언트 신원 확인
- 권한 부여(authorization): 인증된 사용자의 수행 가능 작업 제어

---

### TLS/SSL 암호화

- Cassandra는 클라이언트 머신-데이터베이스 클러스터 간, 클러스터 내 노드 간 안전한 통신 제공
- 암호화 활성화 시 전송 중인 데이터(data in flight)가 손상 없이 안전하게 전송됨

#### 일반 원칙

- 암호화 활성화 시 JVM 기본 프로토콜(protocols)·암호 스위트(cipher suites) 사용
  - `cassandra.yaml`에서 재정의(override) 가능하나, 정책상 필요하거나 취약한 암호(vulnerable ciphers)를 비활성화해야 하는 경우가 아니면 비권장
- FIPS 준수(FIPS-compliant) 설정은 `cassandra.yaml` 설정 파일이 아닌 JVM 수준(JVM level)에서 구성 필요
- Cassandra는 JKS·PKCS12를 포함한 여러 키스토어(keystore) 형식과 PEM 표준 지원
  - `ISslContextCreationFactory` 인터페이스 구현 또는 그 public 하위 클래스 확장 시 SSL 컨텍스트 생성 방식 커스터마이징 가능
  - `ssl_context_factory` 설정은 `server_encryption_options`·`client_encryption_options` 두 섹션 모두에 적용
- Java가 지원하는 키스토어를 사용한 SSL 통신에서 키스토어(keystore)·트러스트스토어(truststore) 파일 생성 방법은 Java 공식 문서(키스토어 생성 가이드) 참고

#### SSL 인증서 핫 리로딩

- Cassandra 4부터 SSL 인증서(Certificates) 핫 리로딩(hot reloading) 지원
- SSL/TLS 지원 활성화 + 기본 파일 기반 키 자료(default file based key material) 사용 시, 노드가 주기적으로(매 10분마다) `cassandra.yaml`에 지정된 트러스트스토어(Trust Store)·키스토어(Key Store) 폴링(poll)
- 키스토어/트러스트스토어 파일 갱신 시 Cassandra가 이후 연결에 해당 파일을 자동으로 다시 로드
  - 갱신된 파일은 기존 설정과 동일한 비밀번호 사용 필요
- 인증서 핫 리로딩은 `nodetool reloadssl` 명령으로도 트리거 가능 → Cassandra가 변경된 인증서를 즉시 인식하게 할 때 사용

```
nodetool reloadssl
```

#### PEM 기반 키 자료

- 내장 클래스 `PEMBasedSSLContextFactory`가 PEM 기반 키 자료(PEM-based key material) 설정 처리
- 설정 방식은 2가지 존재

인라인 PEM 데이터 설정(Inline PEM Data Configuration):

```yaml
client/server_encryption_options:
  ssl_context_factory:
    class_name: org.apache.cassandra.security.PEMBasedSslContextFactory
    parameters:
      private_key: |
        -----BEGIN ENCRYPTED PRIVATE KEY----- OR -----BEGIN PRIVATE KEY-----
        <base64 encoded private key>
        -----END ENCRYPTED PRIVATE KEY----- OR -----END PRIVATE KEY-----
        -----BEGIN CERTIFICATE-----
        <base64 encoded certificate chain>
        -----END CERTIFICATE-----
      private_key_password: "<password if encrypted>"
      trusted_certificates: |
        -----BEGIN CERTIFICATE-----
        <base64 encoded certificate>
        -----END CERTIFICATE-----
```

파일 기반 PEM 설정(File-Based PEM Configuration):

```yaml
client/server_encryption_options:
  ssl_context_factory:
    class_name: org.apache.cassandra.security.PEMBasedSslContextFactory
  keystore: <개인 키와 인증서 체인이 포함된 PEM 키스토어의 파일 경로>
  keystore_password: "<암호화되어 있는 경우의 비밀번호>"
  truststore: <PEM 트러스트스토어의 파일 경로>
```

#### 노드 간(Inter-node) 암호화

- 노드 간 암호화 설정은 `cassandra.yaml`의 `server_encryption_options` 섹션에 위치
- `internode_encryption` 파라미터로 제어, 옵션은 다음과 같음
  - `none`(기본값): 나가는(outgoing) 연결 비암호화
  - `dc`: 다른 데이터센터(datacenter) 피어와의 연결만 암호화, 같은 데이터센터 내부는 비암호화
  - `rack`: 다른 랙(rack) 피어와의 연결만 암호화, 같은 랙 내부는 비암호화
  - `all`: 항상 암호화된 연결 사용

`server_encryption_options`의 전체 설정 블록:

```yaml
server_encryption_options:
  # 나가는(outbound) 연결에서, 어떤 종류의 피어와 보안 연결을 맺을지 결정합니다.
  #   사용 가능한 옵션:
  #     none : 나가는 연결을 암호화하지 않음
  #     dc   : 다른 데이터센터의 피어와의 연결은 암호화하되 데이터센터 내부에서는 하지 않음
  #     rack : 다른 랙의 피어와의 연결은 암호화하되 랙 내부에서는 하지 않음
  #     all  : 항상 암호화된 연결을 사용
  internode_encryption: none
  # true로 설정하면, storage_port에서 암호화된 연결과 암호화되지 않은 연결을 모두 허용합니다.
  # 이 값은 암호화되지 않은 운영 또는 전환(transitional) 운영 중에만 true여야 합니다.
  # internode_encryption이 none이면 optional은 기본적으로 true가 됩니다.
  # optional: true
  # 활성화되면, ssl_storage_port에서 암호화된 수신(listening) 소켓을 엽니다. 4.0으로의
  # 업그레이드 중에만 사용해야 하며, 그 외에는 false로 설정하십시오.
  legacy_ssl_storage_port_enabled: false
  # internode_encryption이 dc, rack 또는 all인 경우 유효한 키스토어로 설정하십시오.
  keystore: conf/.keystore
  #keystore_password: cassandra
  # 키스토어 비밀번호를 별도 파일에 지정하는 선택적 설정입니다.
  # keystore_password와 keystore_password_file이 모두 지정되면 keystore_password가 우선합니다.
  # 파일 내 비밀번호는 첫 번째 줄에 있어야 합니다.
  #keystore_password_file: conf/keystore_passwordfile.txt
  # Cassandra가 SSL 컨텍스트를 생성하는 방식을 구성합니다.
  # PEM 기반 키 자료를 사용하려면 org.apache.cassandra.security.PEMBasedSslContextFactory 참고.
  # ssl_context_factory:
  #     # org.apache.cassandra.security.ISslContextFactory의 인스턴스여야 합니다.
  #     class_name: org.apache.cassandra.security.DefaultSslContextFactory
  # 노드 간 mTLS 인증 중, 인바운드(inbound) 연결(서버 역할)은 서버 인증서가 포함된
  # keystore, keystore_password를 사용하여 SSLContext를 생성하고,
  # 아웃바운드(outbound) 연결(클라이언트 역할)은 클라이언트 인증서가 포함된
  # outbound_keystore & outbound_keystore_password를 사용하여 SSLContext를 생성합니다.
  # 기본적으로 outbound_keystore는 keystore와 동일하며, 이는 mTLS가 활성화되지 않았음을 의미합니다.
#  outbound_keystore: conf/.keystore
#  outbound_keystore_password: cassandra
  # 아웃바운드 키스토어 비밀번호를 별도 파일에 지정하는 선택적 설정입니다.
  # outbound_keystore_password와 outbound_keystore_password_file이 모두 지정되면
  # outbound_keystore_password가 우선합니다.
  # 파일 내 비밀번호는 첫 번째 줄에 있어야 합니다.
  #outbound_keystore_password_file: conf/outbound_keystore_passwordfile.txt
  # 피어 서버 인증서를 검증합니다.
  require_client_auth: false
  # require_client_auth가 true이면 유효한 트러스트스토어로 설정하십시오.
  truststore: conf/.truststore
  #truststore_password: cassandra
  # 트러스트스토어 비밀번호를 별도 파일에 지정하는 선택적 설정입니다.
  # truststore_password와 truststore_password_file이 모두 지정되면 truststore_password가 우선합니다.
  # 파일 내 비밀번호는 첫 번째 줄에 있어야 합니다.
  #truststore_password_file: conf/truststore_passwordfile.txt
  # 인증서의 호스트 이름이 연결된 호스트와 일치하는지 검증합니다.
  require_endpoint_verification: false
  # 더 고급 기본값:
  # protocol: TLS
  # store_type: JKS
  # cipher_suites: [
  #   TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384, TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
  #   TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256, TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,
  #   TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA, TLS_RSA_WITH_AES_128_GCM_SHA256, TLS_RSA_WITH_AES_128_CBC_SHA,
  #   TLS_RSA_WITH_AES_256_CBC_SHA
  # ]
  # 노드 간 인바운드 연결에 사용되는 클라이언트 인증서의 최대 허용 유효 기간을 정의하는
  # 선택적 설정입니다. 예를 들어 max_certificate_validity_period가 30일로 지정되어 있고
  # 클라이언트가 30일을 초과하여 발급된 인증서를 사용하면, 해당 연결은 거부됩니다.
  # max_certificate_validity_period: 365d
  # 경고 임계값(warning threshold)을 정의하는 선택적 설정입니다. 노드 간 인증서 유효 기간에
  # 대해 임계값을 초과하면, 인증서 만료 정보를 담은 경고가 로깅됩니다.
  # certificate_validity_warn_threshold: 10d
```

주요 옵션 정리:

- `internode_encryption`: 어떤 노드 간 연결을 암호화할지 제어(`none`/`dc`/`rack`/`all`)
- `optional`: true이면 동일 포트에서 암호화/비암호화 연결 모두 허용, 전환 운영 중에만 사용 권장
- `legacy_ssl_storage_port_enabled`: `ssl_storage_port`에서 암호화된 수신 소켓 오픈, 4.0 업그레이드 중에만 사용
- `keystore` / `keystore_password`: 서버 개인 키와 인증서 체인이 포함된 키스토어 및 비밀번호
- `outbound_keystore` / `outbound_keystore_password`: mTLS 시 아웃바운드(클라이언트 역할) 연결용 키스토어
- `require_client_auth`: 피어 서버 인증서 검증 여부
- `truststore` / `truststore_password`: `require_client_auth`가 true일 때 사용되는 트러스트스토어
- `require_endpoint_verification`: 인증서의 호스트 이름이 연결 호스트와 일치하는지 검증

#### 클라이언트-노드(Client-to-node) 암호화

- 클라이언트-노드 암호화 설정은 `cassandra.yaml`의 `client_encryption_options` 섹션, 두 개의 주요 토글로 제어
  - `enabled`와 `optional` 모두 false: 클라이언트 연결 전적으로 비암호화(unencrypted)
  - `enabled` true, `optional` false: 모든 클라이언트 연결 필수 보안 처리
  - 둘 다 true: 동일 포트에서 암호화/비암호화 연결 모두 지원
- 또는 `native_transport_port_ssl` 설정으로 보안 클라이언트 통신용 별도 포트 구성 + `optional`은 false로 유지 가능

`client_encryption_options`의 전체 설정 블록:

```yaml
# 클라이언트-서버 암호화를 구성합니다.
#
# **참고** 이 기본 설정은 안전하지 않은(insecure) 설정입니다. 클라이언트-서버 암호화를
# 활성화하려면 다음에 따라 서버 키스토어(상호 인증을 위한 트러스트스토어 포함)를 생성하십시오.
# http://download.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#CreateKeystore
# 그런 다음 아래 설정 변경을 수행하십시오.
#
# 1단계: enabled=true로 설정하고 optional=true를 명시적으로 설정합니다. 모든 노드를 재시작합니다.
#
# 2단계: optional=false로 설정(또는 제거)하고, 트러스트스토어를 생성하여 상호 인증(mutual auth)을
# 사용하려면 require_client_auth=true로 설정합니다. 모든 노드를 재시작합니다.
client_encryption_options:
  # 클라이언트-서버 암호화를 활성화합니다.
  enabled: false
  # true로 설정하면, native_transport_port에서 암호화된 연결과 암호화되지 않은 연결을 모두 허용합니다.
  # 이 값은 암호화되지 않은 운영 또는 전환(transitional) 운영 중에만 true여야 합니다.
  # optional은 enabled가 false이면 기본적으로 true, enabled가 true이면 기본적으로 false가 됩니다.
  # optional: true
  # enabled가 true이면 keystore와 keystore_password를 유효한 키스토어로 설정하십시오.
  keystore: conf/.keystore
  #keystore_password: cassandra
  # 키스토어 비밀번호를 별도 파일에 지정하는 선택적 설정입니다.
  # keystore_password와 keystore_password_file이 모두 지정되면 keystore_password가 우선합니다.
  # 파일 내 비밀번호는 첫 번째 줄에 있어야 합니다.
  #keystore_password_file: conf/keystore_passwordfile.txt
  # Cassandra가 SSL 컨텍스트를 생성하는 방식을 구성합니다.
  # PEM 기반 키 자료를 사용하려면 org.apache.cassandra.security.PEMBasedSslContextFactory 참고.
  # ssl_context_factory:
  #     # org.apache.cassandra.security.ISslContextFactory의 인스턴스여야 합니다.
  #     class_name: org.apache.cassandra.security.DefaultSslContextFactory
  # 클라이언트 인증서를 검증합니다.
  # - true/REQUIRED, 클라이언트를 검증하고 클라이언트가 클라이언트 인증서를 보내도록 강제합니다.
  # - false/NOT_REQUIRED, 클라이언트를 검증하지 않습니다.
  # - optional, 클라이언트 인증서가 전송되면 선택적으로 검증하지만, 인증서 전송을 강제하지는 않습니다.
  require_client_auth: false
  # require_endpoint_verification: false
  # require_client_auth가 true이면 truststore와 truststore_password를 설정하십시오.
  # truststore: conf/.truststore
  # truststore_password: cassandra
  # 트러스트스토어 비밀번호를 별도 파일에 지정하는 선택적 설정입니다.
  # truststore_password와 truststore_password_file이 모두 지정되면 truststore_password가 우선합니다.
  # 파일 내 비밀번호는 첫 번째 줄에 있어야 합니다.
  #truststore_password_file: conf/truststore_passwordfile.txt
  # 더 고급 기본값:
  # protocol: TLS
  # store_type: JKS
  # cipher_suites: [
  #   TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384, TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
  #   TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256, TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,
  #   TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA, TLS_RSA_WITH_AES_128_GCM_SHA256, TLS_RSA_WITH_AES_128_CBC_SHA,
  #   TLS_RSA_WITH_AES_256_CBC_SHA
  # ]
  # 서버에 연결을 맺을 수 있는 클라이언트 인증서의 최대 유효 기간을 정의하는 선택적 설정입니다.
  # 예를 들어 max_certificate_validity_period가 10일로 구성되어 있고 클라이언트가 더 긴
  # 유효 기간(예: 30일)의 인증서로 인증을 시도하면, 해당 연결은 거부됩니다.
  # max_certificate_validity_period: 365d
  # 경고 임계값(warning threshold)을 정의하는 선택적 설정입니다. 클라이언트 인증서 유효 기간에
  # 대해 임계값을 초과하면, 인증서 만료 정보를 담은 경고가 로깅됩니다. 또한 세션 수립 중에
  # 클라이언트 경고가 보고됩니다.
  # certificate_validity_warn_threshold: 10d
```

- 참고: 기본 설정은 안전하지 않음 → `enabled: false` 상태에서는 클라이언트-서버 통신 비암호화
  - 활성화 절차: 1단계 `enabled=true` + `optional=true` 설정 후 전 노드 재시작 → 2단계 `optional=false` 설정(필요 시 `require_client_auth=true`) 후 전 노드 재시작

---

### 역할(Roles)

- Cassandra는 인증·권한 관리 모두에서 데이터베이스 역할(database roles) 사용
  - 역할은 단일 사용자(single user) 또는 사용자 그룹(group of users)을 나타낼 수 있음
- 역할 관리는 Cassandra의 확장 지점(extension point) → `cassandra.yaml`의 `role_manager` 설정으로 구성
- 기본 구현체 `CassandraRoleManager`는 역할 정보를 `system_auth` 키스페이스(keyspace) 테이블에 저장

---

### 인증(Authentication)

- 인증은 플러그인 방식(pluggable)으로 구성, `cassandra.yaml`의 `authenticator` 설정으로 구성
- 기본 배포판에 두 가지 옵션 포함

#### 기본 인증기(Authenticator)

- `AllowAllAuthenticator`: 기본 설정, 인증 검사 미수행 + 자격 증명(credentials) 불필요 → 인증 완전 비활성화
- `PasswordAuthenticator`: 암호화된 자격 증명을 시스템 테이블에 저장 → 단순 사용자 이름/비밀번호(username/password) 인증 가능
  - 역할 관리를 위해 `CassandraRoleManager` 필요

#### 비밀번호 인증 활성화

- 클러스터 전체 인증 활성화 전, 클라이언트 애플리케이션에 자격 증명 사전 구성 필요
- 초기 구성은 클라이언트 트래픽으로부터 격리된 노드에서 수행

1단계 - system_auth 복제(replication) 조정:

- `cqlsh` 세션을 열고 `system_auth` 키스페이스의 복제 계수(replication factor) 수정

```cql
ALTER KEYSPACE system_auth WITH replication = {'class': 'NetworkTopologyStrategy', 'DC1': 3, 'DC2': 3};
```

- 기본적으로 이 키스페이스는 `SimpleReplicationStrategy` 사용, `replication_factor`는 1
- 모범 사례(best practice)는 데이터센터당 3~5의 복제 계수 권장

2단계 - cassandra.yaml 편집:

```yaml
authenticator: PasswordAuthenticator
```

3단계 - 노드 재시작

4단계 - 기본 슈퍼유저(superuser)로 로그인:

```
$ cqlsh -u cassandra -p cassandra
```

5단계 - 새 슈퍼유저 생성(선택사항이나 권장):

- 기본 슈퍼유저 자격 증명은 `QUORUM` 일관성(consistency)으로 읽히는 반면, 다른 사용자는 `LOCAL_ONE` 사용
- 추가 슈퍼유저 생성 시 성능과 보안 향상됨

```cql
CREATE ROLE dba WITH SUPERUSER = true AND LOGIN = true AND PASSWORD = 'super';
```

6단계 - 기본 슈퍼유저 비활성화:

- 새 슈퍼유저로 새 cqlsh 세션 오픈

```cql
ALTER ROLE cassandra WITH SUPERUSER = false AND LOGIN = false;
```

7단계 - 애플리케이션 사용자 역할 생성:

- `CREATE ROLE` 문으로 애플리케이션 사용자의 자격 증명 설정

클러스터 전체 롤아웃(Rollout):

- 각 클러스터 노드에서 2~3단계 반복
- 모든 노드 재시작 완료 시 인증이 클러스터 전체에서 완전히 활성화됨

---

### 권한 부여(Authorization)

- 권한 부여는 플러그인 방식(pluggable)으로 구성, `cassandra.yaml`의 `authorizer` 설정으로 구성
- 기본 배포판에 두 가지 옵션 포함

#### 기본 권한 부여기(Authorizer)

- `AllowAllAuthorizer`: 기본 설정, 권한 검사 미수행 + 모든 역할에 모든 권한 부여 → 반드시 `AllowAllAuthenticator`와 함께 사용
- `CassandraAuthorizer`: 완전한 권한 관리 기능 구현, 데이터를 Cassandra 시스템 테이블에 저장

#### 내부 권한 부여 활성화

- 권한은 화이트리스트(whitelist)로 모델링됨 → 기본적으로 어떤 역할도 데이터베이스 리소스 접근 권한 없음
  - 노드에서 권한 부여 활성화 시, 필요한 권한이 부여되기 전까지 모든 요청 거부
- 클라이언트 요청을 처리하지 않는 노드에서 초기 설정 수행

1단계 - cassandra.yaml 편집:

```yaml
authorizer: CassandraAuthorizer
```

2단계 - 노드 재시작

3단계 - 슈퍼유저 자격 증명으로 로그인:

```
$ cqlsh -u dba -p super
```

4단계 - 권한 부여:

- `GRANT PERMISSION` 문으로 접근 권한(access privileges) 구성

```cql
GRANT SELECT ON ks.t1 TO db_user;
```

- 나머지 노드들이 구성되기 전까지는 영향을 받지 않음 → 클라이언트 중단(disruption) 회피 가능

5단계 - 클러스터 전체에 롤아웃:

- 나머지 각 노드에 대해 1~2단계 반복
- 각 노드 재시작 및 클라이언트 재연결 완료 시 권한 적용(permission enforcement) 시작

---

### 캐싱(Caching)

- 인증·권한 부여는 `system_auth` 테이블에서 빈번한 읽기를 유발해 추가 부하(load) 발생
  - 이 읽기는 클라이언트 작업의 임계 경로(critical path)에서 발생 → 서비스 품질에 영향 가능
- 인증 데이터 캐싱(auth data caching)으로 이러한 영향 완화

#### 캐시 설정 옵션

각 캐시는 세 가지 구성 가능한 옵션 보유:

- 유효 기간(Validity Period): 캐시 항목의 만료(expiration) 제어, 기간 경과 시 항목 무효화·제거
- 갱신 주기(Refresh Rate): 기저 데이터(underlying data) 변경 사항 반영을 위한 백그라운드 읽기(background read) 빈도 제어
  - 비동기 갱신(async refresh) 중에는 캐시가 다소 오래된(stale) 데이터 반환 가능
- 최대 항목 수(Max Entries): 캐시 크기의 상한(upper bound) 제어

#### 설정 명명 규칙

`cassandra.yaml`의 설정 옵션은 다음 패턴을 따름:

- `<type>_validity_in_ms`
- `<type>_update_interval_in_ms`
- `<type>_cache_max_entries`
- 여기서 `<type>`은 `credentials`, `permissions`, 또는 `roles`

- 이 설정들은 `org.apache.cassandra.auth` 도메인 아래에서 JMX로도 노출됨
  - JMX 기반 변경 사항은 영구적이지 않음(not persistent) → 노드 재시작 시 원래대로 되돌아감

---

### JMX 접근 제어

- JMX 접근 제어는 CQL 인증·권한 부여와 별도로 구성
- 사용 가능한 제공자(provider)는 표준 JMX 보안(standard JMX security)과 Cassandra 통합 인증 서브시스템(integrated auth subsystem) 2가지
- 기본 Cassandra 설정은 JMX를 로컬호스트(localhost)로만 제한
  - 원격 JMX 연결 활성화하려면 `cassandra-env.sh`를 편집해 `LOCAL_JMX`를 `no`로 변경
  - 원격 연결은 자동으로 표준 JMX 인증 활성화
- 로컬 전용 연결은 기본적으로 인증 대상이 아니지만 활성화 가능
- 원격 연결 활성화 시 SSL 권장

#### 표준 JMX 인증

- 연결을 허용할 사용자는 단순한 텍스트 파일에 지정
- 비밀번호 파일 위치는 `cassandra-env.sh`에 설정

```
JVM_OPTS="$JVM_OPTS -Dcom.sun.management.jmxremote.password.file=/etc/cassandra/jmxremote.password"
```

- 비밀번호 파일을 편집해 사용자 이름/비밀번호 쌍 추가

```
jmx_user jmx_password
```

- 자격 증명 파일 보호

```
$ chown cassandra:cassandra /etc/cassandra/jmxremote.password
$ chmod 400 /etc/cassandra/jmxremote.password
```

선택적 접근 제어(Optional Access Control):

- 운영 범위(operational scope) 제한하려면 `cassandra-env.sh`에서 다음 줄의 주석 해제

```
#JVM_OPTS="$JVM_OPTS -Dcom.sun.management.jmxremote.access.file=/etc/cassandra/jmxremote.access"
```

- 접근 파일을 편집해 JMX 사용자 권한 부여

```
jmx_user readwrite
```

- 새 설정 적용을 위해 Cassandra 재시작 필요

#### Cassandra 통합 인증 및 권한 부여

- 표준 JMX 인증의 대안으로 Cassandra의 네이티브 인증·권한 부여 제공자 사용 가능
- 더 높은 유연성·보안 제공하나, 노드가 링(ring)에 합류하기 전까지는 사용 불가 → 인증 서브시스템이 그 시점까지 완전히 초기화되지 않음
- 모니터링 목적상 부트스트랩(bootstrap) 중에 JMX 접근이 필요한 경우가 많음
  - 권장 방식: 부트스트랩 중에는 로컬 전용 JMX 인증 사용 → 노드가 링에 합류해 초기 설정 완료 후, 원격 연결 필요 시 통합 인증으로 전환
- CQL 인증에 사용되는 데이터베이스 역할(database roles)로 JMX 접근 제어 가능
  - `cqlsh`를 통한 중앙 집중식 관리(centralized management) 가능
  - `GRANT PERMISSION` 문으로 특정 MBean 작업에 대한 세밀한 접근 제어(fine-grained control) 설정 가능

통합 인증 활성화:

- `cassandra-env.sh`에서 다음 줄의 주석 해제

```
#JVM_OPTS="$JVM_OPTS -Dcassandra.jmx.remote.login.config=CassandraLogin"
#JVM_OPTS="$JVM_OPTS -Djava.security.auth.login.config=$CASSANDRA_HOME/conf/cassandra-jaas.config"
```

- 표준 JMX 인증은 다음과 같이 주석 처리해 비활성화

```
JVM_OPTS="$JVM_OPTS -Dcom.sun.management.jmxremote.password.file=/etc/cassandra/jmxremote.password"
```

통합 권한 부여 활성화:

- 다음 줄의 주석 해제

```
#JVM_OPTS="$JVM_OPTS -Dcassandra.jmx.authorizer=org.apache.cassandra.auth.jmx.AuthorizationProxy"
```

- 표준 접근 제어가 비활성화되어 있는지 확인

```
#JVM_OPTS="$JVM_OPTS -Dcom.sun.management.jmxremote.access.file=/etc/cassandra/jmxremote.access"
```

예시 역할 구성:

```cql
CREATE ROLE jmx WITH LOGIN = false;
GRANT SELECT ON ALL MBEANS TO jmx;
GRANT DESCRIBE ON ALL MBEANS TO jmx;
GRANT EXECUTE ON MBEAN 'java.lang:type=Threading' TO jmx;
GRANT EXECUTE ON MBEAN 'com.sun.management:type=HotSpotDiagnostic' TO jmx;

# nodetool 권한 부여
GRANT EXECUTE ON MBEAN 'org.apache.cassandra.db:type=EndpointSnitchInfo' TO jmx;
GRANT EXECUTE ON MBEAN 'org.apache.cassandra.db:type=StorageService' TO jmx;

# 로그인 권한이 있는 사용자에게 역할 부여
CREATE ROLE ks_user WITH PASSWORD = 'password' AND LOGIN = true AND SUPERUSER = false;
GRANT jmx TO ks_user;
```

세밀한 MBean 접근(Fine-grained MBean Access):

```cql
GRANT EXECUTE ON MBEAN 'org.apache.cassandra.db:type=Tables,keyspace=test_keyspace,table=t1' TO ks_user;
GRANT EXECUTE ON MBEAN 'org.apache.cassandra.db:type=Tables,keyspace=test_keyspace,table=*' TO ks_owner;
```

- 역할·권한의 추가/제거는 초기 설정 이후 동적으로 처리 → 추가 재시작 불필요

#### JMX와 SSL

- SSL 설정은 `cassandra-env.sh`의 시스템 속성(system properties)으로 제어

주요 속성:

- `com.sun.management.jmxremote.ssl`: SSL 활성화 시 true로 설정
- `com.sun.management.jmxremote.ssl.need.client.auth`: 클라이언트 인증서 검증 시 true로 설정
- `com.sun.management.jmxremote.registry.ssl`: RMI 레지스트리에 SSL 활성화
- `com.sun.management.jmxremote.ssl.enabled.protocols`: 기본 프로토콜을 쉼표로 구분된 목록으로 재정의(일반적으로 불필요)
- `com.sun.management.jmxremote.ssl.enabled.cipher.suites`: 기본 암호 스위트(cipher suites) 재정의(일반적으로 불필요)
- `javax.net.ssl.keyStore`: 서버 개인 키와 인증서가 포함된 키스토어 경로
- `javax.net.ssl.keyStorePassword`: 키스토어 파일 비밀번호
- `javax.net.ssl.trustStore`: 클라이언트 인증서가 포함된 트러스트스토어 경로(클라이언트 인증서 검증 시)
- `javax.net.ssl.trustStorePassword`: 트러스트스토어 파일 비밀번호

- 참고: `cassandra-env.sh` 시스템 속성을 통한 JMX SSL 구성 방식은 레거시(legacy)로 간주 → 하위 호환성(backward compatibility) 목적으로만 지원
  - `cassandra-env.sh`와 `cassandra.yaml`의 `jmx_encryption_options` 양쪽으로 SSL 활성화 시 시작 시 설정 오류(configuration error) 발생
  - JMX SSL의 경우 `SSLContext` 핫 리로딩(hot reloading)은 아직 미지원
  - `client/server_encryption_options`와 유사하게, `jmx_encryption_options`에서 PEM 기반 키 자료 지정 또는 `ssl_context_factory`로 SSL 설정 커스터마이징 가능

`jmx_server_options` 및 `jmx_encryption_options`의 예시(`cassandra.yaml`):

```yaml
#jmx_server_options:
  # enabled: true
  # remote: false
  # jmx_port: 7199
  #
  # 원격 연결이 활성화될 때 RMI 레지스트리가 사용하는 포트입니다.
  # 방화벽 설정을 단순화하기 위해 JMX 서버 포트(port)와 동일하게 설정할 수 있습니다(CASSANDRA-7087 참고).
  # 그러나 ssl이 활성화되면 jmx와 rmi에 동일한 포트를 사용할 수 없으므로, 이 속성에 다른 값을
  # 지정하십시오. 또는 주석 처리하거나 0으로 설정하여 임의의 포트를 사용할 수 있습니다(CASSANDRA-7087 이전 동작).
  # rmi_port: 7199
  #
  # jmx ssl 옵션 - 원격 연결이 활성화될 때만 적용됨
  #
  # jmx_encryption_options:
  #   enabled: true
  #   cipher_suites: [TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256]
  #   accepted_protocols: [TLSv1.2,TLSv1.3,TLSv1.1]
  #   keystore: conf/cassandra_ssl.keystore
  #   keystore_password: cassandra
```

---

### 암호화 공급자(Crypto Providers)

- 커스텀 Java 암호화 공급자(Java Crypto Provider) 지정 기능은 CASSANDRA-18624에서 구현됨

#### 기본 설정

`cassandra.yaml`의 기본 `crypto_provider` 설정:

```yaml
crypto_provider:
  - class_name: org.apache.cassandra.security.DefaultCryptoProvider
    parameters:
      - fail_on_missing_provider: "false"
```

- `DefaultCryptoProvider`는 Amazon Corretto Crypto Provider 설치
  - JRE 기본 암호화 공급자보다 월등히 높은 성능 입증됨
- Amazon Corretto Crypto Provider는 x86_64·aarch64 플랫폼에서 동작
  - 설치 실패 시 기본 JRE 암호화 공급자로 폴백(fall back)
  - `fail_on_missing_provider`를 `"true"`로 설정하면 공급자 설치 미완료 시 노드 시작 강제 실패
- `JREProvider` 클래스는 커스텀 공급자 설치를 우회하고 JRE의 기본 공급자 사용

#### 레거시 노드 업그레이드

- `cassandra.yaml`에 `crypto_provider`가 없는 상태로 Cassandra 5.0으로 업그레이드되는 구형 노드는 `JREProvider`를 기본값으로 사용 → JRE에 설치된 암호화 공급자 사용

#### 커스텀 암호화 공급자 구현

대체 암호화 공급자에 대한 설정 옵션 2가지 존재:

- 옵션 1: JRE의 `java.security` 파일을 구성해 암호화 공급자 지정 + 구현체를 JRE 클래스패스(classpath)에 추가
- 옵션 2: `org.apache.cassandra.security.AbstractCryptoProvider`를 확장하고 다음 4가지 메서드 구현
  - `getProviderName`: 공급자 이름 반환
  - `getProviderClassAsString`: `java.security.Provider`를 확장하는 암호화 공급자의 FQCN(완전 정규화 클래스 이름) 반환
  - `installator`: 런타임에 공급자를 설치하는 `Runnable` 반환
  - `isHealthyInstallation`: 설치가 정상이면 true, 그렇지 않으면 false 반환

#### 공급자 설치 동작

- 설치 시 `AbstractCryptoProvider`가 공급자 기설치 여부 확인
  - 설치 + 첫 번째 위치(first)에 배치됨 → 메시지 로깅
  - 설치되었으나 첫 번째가 아닌 상태에서 `fail_on_missing_provider`가 true → 예외 발생, 노드 시작 실패
  - 설치 자체가 실패한 경우도 동일하게 노드 시작 실패

#### 플랫폼별 라이브러리

- 플랫폼별 라이브러리(platform-specific libraries)는 `cassandra.in.sh`에 의해 Cassandra 클래스패스에 자동 추가
  - 각 아키텍처용 JAR 파일은 `lib/aarch64`와 `lib/x86_64` 디렉터리에 위치
  - 플랫폼 판별은 `uname -m` 명령 출력 사용

---

### 감사 로깅(Audit Logging)

> 원본: https://cassandra.apache.org/doc/latest/cassandra/managing/operating/audit_logging.html

- Cassandra의 감사 로깅(audit logging)은 수신되는 모든 CQL 명령 요청과 노드에 대한 인증(로그인 성공/실패)을 기록
- 구현체는 성능 측면에서 커뮤니티가 권장하는 `BinAuditLogger`와 `FileAuditLogger` 2가지 사용 가능

#### 기록되는 항목

시스템이 기록하는 항목:

- 로그인 시도(성공 및 실패 모두)
- 네이티브 CQL 프로토콜을 통해 실행되는 모든 데이터베이스 명령

주요 제약 사항(Key Limitation):

- 준비된 문(prepared statements) 실행 시 prepare 호출 시 클라이언트가 제공한 쿼리가 로깅됨
  - 실행 시 바인딩된 실제 값(actual values bound)은 감사 로그에 미기록

로깅되는 속성(Logged Attributes):

- 각 구현체는 다음 정보에 접근 가능: 사용자(user) · 호스트(host) · 소스 IP 주소(source IP address) · 소스 포트(source port) · 타임스탬프(timestamp) · 요청 유형(request type) · 카테고리(category) · 키스페이스(keyspace) · 스코프(scope) · 작업(operation, 실제 CQL 명령)

로그 출력 형식 예시:

```
LogMessage: user:anonymous|host:localhost/X.X.X.X|source:/X.X.X.X|port:60878|timestamp:1521158923615|type:USE_KS|category:DDL|ks:dev1|operation:USE "dev1"
```

로깅되는 필드: `user`, `host`, `source`(소스 IP 주소), `port`(소스 포트), `timestamp`(유닉스 타임스탬프), `type`(요청 유형), `category`(카테고리), `ks`(키스페이스), `scope`(스코프), `operation`(실제 CQL 명령)

#### cassandra.yaml을 통한 설정

`cassandra.yaml`의 `audit_logging_options` 블록:

```yaml
audit_logging_options:
  enabled: false
  logger:
    - class_name: BinAuditLogger
  #   parameters:
  #     - key_value_separator: ":"
  #       field_separator: "|"
  # audit_logs_dir:
  # included_keyspaces:
  # excluded_keyspaces: system, system_schema, system_virtual_schema
  # included_categories:
  # excluded_categories:
  # included_users:
  # excluded_users:
  # roll_cycle: FAST_HOURLY
  # block: true
  # max_queue_weight: 268435456 # 256 MiB
  # max_log_size: 17179869184 # 16 GiB
  #
  ## archive_command가 비어 있거나 설정되지 않으면, Cassandra는 내장 DeletingArchiver를 사용하며
  ## max_log_size에 도달하면 가장 오래된 파일을 삭제합니다.
  ## archive_command가 설정되면, Cassandra는 DeletingArchiver를 사용하지 않으므로 필요한 정리(cleanup)는
  ## 스크립트의 책임입니다.
  ## 예시: "/path/to/script.sh %path" 여기서 %path는 롤링되는 파일로 대체됩니다.
  # archive_command:
  # max_archive_retries: 10
```

사용 가능한 옵션:

- `enabled`: 감사 로깅 활성화/비활성화
- `logger`: 로거 클래스 이름(`BinAuditLogger` 또는 `FileAuditLogger`, 또는 커스텀 로거) 지정
- `audit_logs_dir`: 감사 로그의 디렉터리 위치
- `included_keyspaces`: 포함할 키스페이스(쉼표로 구분, 기본값: 전체)
- `excluded_keyspaces`: 제외할 키스페이스(기본값: `system`, `system_schema`, `system_virtual_schema`)
- `included_categories`: 포함할 감사 로그 카테고리(쉼표로 구분)
- `excluded_categories`: 제외할 카테고리
- `included_users`: 포함할 사용자
- `excluded_users`: 제외할 사용자

`BinAuditLogger`에만 적용되는 고급 옵션:

- `roll_cycle`: 로그 롤링 주기(기본값: `HOURLY`)
- `block`: 로깅이 뒤처질 때 차단(block)할지 레코드를 드롭(drop)할지 여부(기본값: true)
- `max_queue_weight`: 메모리 큐 한도(기본값: 256 MiB, 268435456 바이트)
- `max_log_size`: 보존되는 최대 파일 크기(기본값: 16 GiB, 17179869184 바이트)
- `archive_command`: 롤링된 파일을 아카이브하는 스크립트(`%path`가 롤링 파일로 대체됨)
  - 비어 있으면 내장 `DeletingArchiver`가 `max_log_size` 도달 시 가장 오래된 파일 삭제
- `max_archive_retries`: 아카이브 재시도 최대 횟수(기본값: 10)

사용 가능한 카테고리(Categories): `QUERY`, `DML`, `DDL`, `DCL`, `OTHER`, `AUTH`, `ERROR`, `PREPARE`

- `key_value_separator`(기본값 `:`)와 `field_separator`(기본값 `|`)는 `parameters` 아래에서 커스터마이징 가능

#### nodetool 명령

감사 로깅 활성화:

```
nodetool enableauditlog
```

지원 옵션:

- `--excluded-categories`: 제외할 카테고리(쉼표로 구분)
- `--excluded-keyspaces`: 제외할 키스페이스(쉼표로 구분)
- `--excluded-users`: 제외할 사용자(쉼표로 구분)
- `--included-categories`: 포함할 카테고리(쉼표로 구분)
- `--included-keyspaces`: 포함할 키스페이스(쉼표로 구분)
- `--included-users`: 포함할 사용자(쉼표로 구분)
- `--logger`: 사용할 로거 클래스 이름

감사 로깅 비활성화:

```
nodetool disableauditlog
```

필터 재로드(Reload filters):

- `nodetool enableauditlog`를 다시 실행하면 필터 갱신 가능

```
nodetool enableauditlog --loggername <loggerName> --included-keyspaces <values>
```

#### 감사 로그 파일 조회

- `auditlogviewer` 유틸리티는 바이너리 로그(binary log) 내용을 사람이 읽을 수 있는 형식으로 표시

```
auditlogviewer <path1> [<path2>...<pathN>] [options]
```

옵션:

- `-f,--follow`: 로그를 지속적으로 모니터링(continuous monitoring)
- `-r,--roll_cycle`: 로그 롤링 빈도(rotation frequency) 지정
- `-h,--help`: 도움말 표시

#### BinAuditLogger 설정

- `BinAuditLogger`는 성능을 위해 커뮤니티가 권장하는 구현체, 바이너리 형식으로 로그 기록

고급 설정 옵션:

- `block`: 로깅이 뒤처질 때 레코드를 차단(block)할지 드롭(drop)할지 여부(기본값: true)
- `max_queue_weight`: 메모리 큐 한도(기본값: 256MB)
- `max_log_size`: 보존되는 최대 파일 크기(기본값: 16GB)
- `roll_cycle`: 롤링 빈도(기본값: HOURLY)

- `BinAuditLogger`가 생성하는 바이너리 로그는 `auditlogviewer` 유틸리티로 읽기 가능

#### FileAuditLogger 설정

- `FileAuditLogger`는 SLF4J 로거를 통해 텍스트 형식으로 감사 이벤트 기록
- 사용하려면 `logback.xml`에 감사 이벤트 전용 어펜더(appender)를 구성해 `audit/audit.log`로 출력 + 커스텀 롤링 정책(rolling policy)·보존(retention) 설정 적용 필요

`logback.xml` 예시:

```xml
<!-- Audit Logging (FileAuditLogger) rolling file appender to audit.log -->
<appender name="AUDIT" class="ch.qos.logback.core.rolling.RollingFileAppender">
  <file>${cassandra.logdir}/audit/audit.log</file>
  <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
    <!-- rollover daily -->
    <fileNamePattern>${cassandra.logdir}/audit/audit.log.%d{yyyy-MM-dd}.%i.zip</fileNamePattern>
    <!-- each file should be at most 50MB, keep 30 days worth of history, but at most 5GB -->
    <maxFileSize>50MB</maxFileSize>
    <maxHistory>30</maxHistory>
    <totalSizeCap>5GB</totalSizeCap>
  </rollingPolicy>
  <encoder>
    <pattern>%-5level [%thread] %date{"yyyy-MM-dd'T'HH:mm:ss,SSS", UTC} %F:%L - %msg%n</pattern>
  </encoder>
</appender>

<!-- Audit Logging additivity to redirect audt logging events to audit/audit.log -->
<logger name="org.apache.cassandra.audit" additivity="false" level="INFO">
    <appender-ref ref="AUDIT"/>
</logger>
```

이 설정의 주요 요소:

- `<file>`: 감사 로그가 기록되는 활성 파일 경로(`${cassandra.logdir}/audit/audit.log`)
- `SizeAndTimeBasedRollingPolicy`: 크기·시간 기반으로 로그 롤오버
  - `<fileNamePattern>`: 롤링된 파일 이름 패턴(매일 `.zip`으로 압축)
  - `<maxFileSize>`: 각 파일의 최대 크기(50MB)
  - `<maxHistory>`: 보존 기간(30일)
  - `<totalSizeCap>`: 전체 보존 용량 상한(5GB)
- `<encoder>`의 `<pattern>`: UTC 기준 타임스탬프와 함께 로그 라인 형식 정의
- `<logger name="org.apache.cassandra.audit" additivity="false">`: 감사 로깅 이벤트를 `AUDIT` 어펜더로 리다이렉트
  - `additivity="false"` 설정 → 이벤트가 다른 어펜더로 중복 전파되지 않음

---

### 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [Security (공식 문서)](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/security.html)
- [Audit Logging (공식 문서)](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/audit_logging.html)
- [cassandra.yaml 설정 파일 (Apache Cassandra GitHub)](https://github.com/apache/cassandra/blob/trunk/conf/cassandra.yaml)
- [JSSE Reference Guide - Creating a Keystore (Oracle)](http://download.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#CreateKeystore)

---

## Cassandra 메트릭과 모니터링

> 원본: https://cassandra.apache.org/doc/latest/cassandra/managing/operating/metrics.html

---

### 목차

1. [개요](#개요)
2. [메트릭 타입(Metric Types)](#메트릭-타입metric-types)
3. [테이블 메트릭(Table Metrics)](#테이블-메트릭table-metrics)
4. [키스페이스 메트릭(Keyspace Metrics)](#키스페이스-메트릭keyspace-metrics)
5. [스레드 풀 메트릭(ThreadPool Metrics)](#스레드-풀-메트릭threadpool-metrics)
6. [클라이언트 요청 메트릭(Client Request Metrics)](#클라이언트-요청-메트릭client-request-metrics)
7. [캐시 메트릭(Cache Metrics)](#캐시-메트릭cache-metrics)
8. [CQL 메트릭(CQL Metrics)](#cql-메트릭cql-metrics)
9. [드롭된 메시지 메트릭(DroppedMessage Metrics)](#드롭된-메시지-메트릭droppedmessage-metrics)
10. [스트리밍 메트릭(Streaming Metrics)](#스트리밍-메트릭streaming-metrics)
11. [컴팩션 메트릭(Compaction Metrics)](#컴팩션-메트릭compaction-metrics)
12. [커밋로그 메트릭(CommitLog Metrics)](#커밋로그-메트릭commitlog-metrics)
13. [스토리지 메트릭(Storage Metrics)](#스토리지-메트릭storage-metrics)
14. [힌티드 핸드오프 메트릭(HintedHandoff Metrics)](#힌티드-핸드오프-메트릭hintedhandoff-metrics)
15. [힌트 서비스 메트릭(HintsService Metrics)](#힌트-서비스-메트릭hintsservice-metrics)
16. [SSTable 인덱스 메트릭(SSTable Index Metrics)](#sstable-인덱스-메트릭sstable-index-metrics)
17. [버퍼 풀 메트릭(BufferPool Metrics)](#버퍼-풀-메트릭bufferpool-metrics)
18. [클라이언트 메트릭(Client Metrics)](#클라이언트-메트릭client-metrics)
19. [배치 메트릭(Batch Metrics)](#배치-메트릭batch-metrics)
20. [JVM 메트릭(JVM Metrics)](#jvm-메트릭jvm-metrics)
21. [메트릭 접근 방법](#메트릭-접근-방법)
22. [가상 테이블을 통한 모니터링(Virtual Tables)](#가상-테이블을-통한-모니터링virtual-tables)
23. [참고 자료](#참고-자료)

---

### 개요

- Cassandra의 메트릭(metric)은 Dropwizard Metrics 라이브러리로 관리됨
- 메트릭은 JMX, 가상 테이블(Virtual Tables)로 조회 가능하며, 다양한 내장 리포터(reporter)·서드파티(third-party) 리포터 플러그인으로 외부 모니터링 시스템에 전송(push) 가능
- 메트릭은 단일 노드(single node) 단위로 수집됨
  - 클러스터 전체 관점 집계(aggregate)는 운영자(operator)가 외부 모니터링 시스템을 통해 수행 필요

---

### 메트릭 타입(Metric Types)

모든 메트릭은 다음 타입 중 하나로 분류됨:

- Gauge(게이지): 어떤 값에 대한 순간적인(instantaneous) 측정값
- Counter(카운터): `AtomicLong` 인스턴스에 대한 게이지, 모니터링 시스템이 마지막 호출 이후의 변화량(change) 소비(consume)
- Histogram(히스토그램): 값들의 통계적 분포(statistical distribution) 측정
  - 최소(min), 최대(max), 평균(mean), 중앙값(median), 75/90/95/98/99/99.9번째 백분위수(percentile) 포함
- Timer(타이머): 코드 실행 속도(rate)와 실행 시간(duration)에 대한 히스토그램을 모두 측정
- Latency(레이턴시): 레이턴시(마이크로초 단위)를 추적하는 특수 타입
  - Timer에 더해 누적된 총 레이턴시(total accrued latency)를 추적하는 Counter도 함께 제공
- Meter(미터): 평균 처리량(mean throughput)과 1/5/15분 단위 지수 가중 이동 평균(exponentially-weighted moving average) 측정

---

### 테이블 메트릭(Table Metrics)

- 각 테이블(table)에 대해 다음과 같은 메트릭 수집

형식(Format):

- 메트릭 이름: `org.apache.cassandra.metrics.Table.<MetricName>.<Keyspace>.<Table>`
- JMX MBean: `org.apache.cassandra.metrics:type=Table keyspace=<Keyspace> scope=<Table> name=<MetricName>`

메트릭 목록:

- `MemtableOnHeapSize` (Gauge\<Long\>): 컬럼 오버헤드(column overhead)와 덮어쓴(overwritten) 파티션을 포함한, 온힙(on-heap) 멤테이블의 총 데이터
- `MemtableOffHeapSize` (Gauge\<Long\>): 컬럼 오버헤드와 덮어쓴 파티션을 포함한, 오프힙(off-heap) 멤테이블의 총 데이터
- `MemtableLiveDataSize` (Gauge\<Long\>): 자료 구조 오버헤드(data structure overhead)를 제외한, 멤테이블 내 총 라이브(live) 데이터
- `AllMemtablesOnHeapSize` (Gauge\<Long\>): 모든 멤테이블(보조 인덱스(2i) 및 플러시 대기 중인 것 포함)의 온힙 총 데이터
- `AllMemtablesOffHeapSize` (Gauge\<Long\>): 모든 멤테이블(보조 인덱스(2i) 및 플러시 대기 중인 것 포함)의 오프힙 총 데이터
- `AllMemtablesLiveDataSize` (Gauge\<Long\>): 오버헤드를 제외한, 모든 멤테이블의 총 라이브 데이터
- `MemtableColumnsCount` (Gauge\<Long\>): 멤테이블 내 컬럼의 총 개수
- `MemtableSwitchCount` (Counter): 플러시(flush)가 멤테이블 교체(switch)를 유발한 횟수
- `CompressionRatio` (Gauge\<Double\>): 모든 SSTable에 대한 현재 압축률(compression ratio)
- `EstimatedPartitionSizeHistogram` (Gauge\<long[]\>): 추정 파티션 크기(바이트 단위)의 히스토그램
- `EstimatedPartitionCount` (Gauge\<Long\>): 테이블 내 대략적인 키(key) 개수
- `EstimatedColumnCountHistogram` (Gauge\<long[]\>): 추정 컬럼 개수의 히스토그램
- `SSTablesPerReadHistogram` (Histogram): 파티션 읽기당 접근한 SSTable 파일의 수
- `ReadLatency` (Latency): 이 테이블에 대한 로컬 읽기 레이턴시
- `RangeLatency` (Latency): 이 테이블에 대한 로컬 범위 스캔(range scan) 레이턴시
- `WriteLatency` (Latency): 이 테이블에 대한 로컬 쓰기 레이턴시
- `CoordinatorReadLatency` (Timer): 이 테이블에 대한 코디네이터(coordinator) 읽기 레이턴시
- `CoordinatorWriteLatency` (Timer): 이 테이블에 대한 코디네이터 쓰기 레이턴시
- `CoordinatorScanLatency` (Timer): 이 테이블에 대한 코디네이터 범위 스캔 레이턴시
- `PendingFlushes` (Counter): 이 테이블에 대한 추정 대기 중(pending) 플러시 작업 수
- `BytesFlushed` (Counter): 서버 시작 이후 플러시된 총 바이트 수
- `CompactionBytesWritten` (Counter): 서버 시작 이후 컴팩션이 기록한 총 바이트 수
- `PendingCompactions` (Gauge\<Integer\>): 이 테이블에 대한 추정 대기 중 컴팩션 수
- `LiveSSTableCount` (Gauge\<Integer\>): 이 테이블에 대해 디스크상에 존재하는 SSTable의 수
- `LiveDiskSpaceUsed` (Counter): SSTable이 사용하는 디스크 공간(바이트)
- `TotalDiskSpaceUsed` (Counter): 가비지 컬렉션(GC) 대기 중인 오래된(obsolete) SSTable을 포함한 총 디스크 공간
- `MaxSSTableSize` (Gauge\<Long\>): 디스크상의 최대 물리적 SSTable 크기(바이트)
- `MaxSSTableDuration` (Gauge\<Long\>): 최대 SSTable 지속 시간(maxTimestamp - minTimestamp), 밀리초 단위
- `MinPartitionSize` (Gauge\<Long\>): 가장 작은 컴팩션된 파티션의 크기(바이트)
- `MaxPartitionSize` (Gauge\<Long\>): 가장 큰 컴팩션된 파티션의 크기(바이트)
- `MeanPartitionSize` (Gauge\<Long\>): 평균 컴팩션된 파티션의 크기(바이트)
- `BloomFilterFalsePositives` (Gauge\<Long\>): 이 테이블의 블룸 필터(bloom filter)에서 발생한 거짓 양성(false positive)의 수
- `BloomFilterFalseRatio` (Gauge\<Double\>): 이 테이블의 블룸 필터의 거짓 양성 비율
- `BloomFilterDiskSpaceUsed` (Gauge\<Long\>): 블룸 필터가 사용하는 디스크 공간(바이트)
- `BloomFilterOffHeapMemoryUsed` (Gauge\<Long\>): 블룸 필터가 사용하는 오프힙 메모리
- `IndexSummaryOffHeapMemoryUsed` (Gauge\<Long\>): 인덱스 요약(index summary)이 사용하는 오프힙 메모리
- `CompressionMetadataOffHeapMemoryUsed` (Gauge\<Long\>): 압축 메타데이터(compression metadata)가 사용하는 오프힙 메모리
- `KeyCacheHitRate` (Gauge\<Double\>): 이 테이블에 대한 키 캐시(key cache) 적중률(hit rate)
- `TombstoneScannedHistogram` (Histogram): 쿼리 중 스캔된 툼스톤(tombstone)의 히스토그램
- `TombstoneWarnings` (Counter): `tombstone_warn_threshold`를 초과한 읽기 수
- `TombstoneFailures` (Counter): `tombstone_failure_threshold`를 초과한 읽기 수
- `LiveScannedHistogram` (Histogram): 쿼리 중 스캔된 라이브 셀(live cell)의 히스토그램
- `ColUpdateTimeDeltaHistogram` (Histogram): 컬럼 갱신 시간 델타(time delta)의 히스토그램
- `ViewLockAcquireTime` (Timer): 구체화된 뷰(materialized view) 갱신을 위해 파티션 잠금(lock)을 획득하는 데 걸린 시간
- `ViewReadTime` (Timer): 구체화된 뷰 갱신 시 로컬 읽기에 걸린 시간
- `TrueSnapshotsSize` (Gauge\<Long\>): 모든 SSTable 구성요소를 포함한 스냅샷(snapshot)이 사용하는 디스크 공간
- `RowCacheHitOutOfRange` (Counter): 쿼리 필터를 충족하지 못한 로우 캐시(row cache) 적중 수
- `RowCacheHit` (Counter): 로우 캐시 적중 횟수
- `RowCacheMiss` (Counter): 로우 캐시 미스(miss) 횟수
- `CasPrepare` (Latency): Paxos prepare 라운드의 레이턴시
- `CasPropose` (Latency): Paxos propose 라운드의 레이턴시
- `CasCommit` (Latency): Paxos commit 라운드의 레이턴시
- `PercentRepaired` (Gauge\<Double\>): 디스크상에서 복구(repair)된 테이블 데이터의 비율(퍼센트)
- `BytesRepaired` (Gauge\<Long\>): 디스크상에서 복구된 테이블 데이터의 크기
- `BytesUnrepaired` (Gauge\<Long\>): 디스크상에서 복구되지 않은 테이블 데이터의 크기
- `BytesPendingRepair` (Gauge\<Long\>): 진행 중인 증분 복구(incremental repair)를 위해 격리된 테이블 데이터의 크기
- `SpeculativeRetries` (Counter): 이 테이블에 대해 전송된 추측성 재시도(speculative retry)의 수
- `SpeculativeFailedRetries` (Counter): 타임아웃 방지에 실패한 추측성 재시도의 수
- `SpeculativeInsufficientReplicas` (Counter): 레플리카(replica) 부족으로 시도되지 않은 추측성 재시도의 수
- `SpeculativeSampleLatencyNanos` (Gauge\<Long\>): 추측(speculation)을 시도하기 전 대기하는 시간(나노초)
- `AnticompactionTime` (Timer): 일관된(consistent) 복구 전 안티컴팩션(anticompaction)에 소요된 시간
- `ValidationTime` (Timer): 복구 중 검증 컴팩션(validation compaction)에 소요된 시간
- `SyncTime` (Timer): 복구 중 스트리밍(streaming)에 소요된 시간
- `BytesValidated` (Histogram): 검증 중 읽은 바이트의 히스토그램
- `PartitionsValidated` (Histogram): 검증 중 읽은 파티션의 히스토그램
- `BytesAnticompacted` (Counter): 안티컴팩션된 바이트 수
- `BytesMutatedAnticompaction` (Counter): 완전 포함(full containment)으로 인해 안티컴팩션을 회피한 바이트 수
- `MutatedAnticompactionGauge` (Gauge\<Double\>): 변경된(mutated) 바이트 대 복구된 총 바이트의 비율

---

### 키스페이스 메트릭(Keyspace Metrics)

- 각 키스페이스(keyspace)에 대해 다음과 같은 메트릭 수집

형식(Format):

- 메트릭 이름: `org.apache.cassandra.metrics.keyspace.<MetricName>.<Keyspace>`
- JMX MBean: `org.apache.cassandra.metrics:type=Keyspace scope=<Keyspace> name=<MetricName>`

- 참고: 대부분의 메트릭은 테이블 메트릭(Table Metrics)의 집계(aggregation) → 아래는 키스페이스에 고유한 메트릭

키스페이스 고유 메트릭:

- `WriteFailedIdeaCL` (Counter): 구성된 이상적 일관성 수준(ideal consistency level)을 달성하지 못한 쓰기의 수
- `IdealCLWriteLatency` (Latency): 구성된 이상적 일관성 수준에서의 코디네이터 레이턴시
- `RepairTime` (Timer): 복구 코디네이터(repair coordinator)로서 소요된 총 시간
- `RepairPrepareTime` (Timer): 복구를 준비(prepare)하는 데 소요된 총 시간

---

### 스레드 풀 메트릭(ThreadPool Metrics)

- Cassandra는 스테이지드 이벤트 기반 아키텍처(SEDA, Staged Event-Driven Architecture) 사용 → 여러 스레드 풀(thread pool)에 대해 다음과 같은 메트릭 수집

형식(Format):

- 메트릭 이름: `org.apache.cassandra.metrics.ThreadPools.<MetricName>.<Path>.<ThreadPoolName>`
- JMX MBean: `org.apache.cassandra.metrics:type=ThreadPools path=<Path> scope=<ThreadPoolName> name=<MetricName>`

공통 메트릭:

- `ActiveTasks` (Gauge\<Integer\>): 이 풀이 현재 작업 중인 태스크(task)의 수
- `PendingTasks` (Gauge\<Integer\>): 이 풀에 큐(queue)에 들어가 있는 태스크의 수
- `CompletedTasks` (Counter): 완료된 태스크의 총 수
- `TotalBlockedTasks` (Counter): 큐 포화(saturation)로 인해 차단된(blocked) 태스크의 수
- `CurrentlyBlockedTask` (Counter): 현재 차단되어 있지만 재시도 가능한(retryable) 태스크의 수
- `MaxPoolSize` (Gauge\<Integer\>): 이 풀의 최대 스레드 수
- `MaxTasksQueued` (Gauge\<Integer\>): 차단되기 전 큐에 들어갈 수 있는 최대 태스크 수

- 다음 스레드 풀 각각에 대해 위 메트릭 수집

전송(Transport) 스레드 풀:

- `Native-Transport-Requests` (transport): 클라이언트 CQL 요청 처리

요청(Request) 스레드 풀:

- `CounterMutationStage` (request): 카운터(counter) 쓰기 담당
- `ViewMutationStage` (request): 구체화된 뷰(materialized view) 쓰기 담당
- `MutationStage` (request): 그 외 모든 쓰기 담당
- `ReadRepairStage` (request): 읽기 복구(ReadRepair)를 실행하는 스레드 풀
- `ReadStage` (request): 로컬 읽기를 담당하는 스레드 풀
- `RequestResponseStage` (request): 클러스터로 향하는 코디네이터 요청 담당

내부(Internal) 스레드 풀:

- `AntiEntropyStage` (internal): 복구를 위한 머클 트리(merkle tree) 구축
- `CacheCleanupExecutor` (internal): 캐시 유지보수 수행
- `CompactionExecutor` (internal): 컴팩션 실행
- `GossipStage` (internal): 가십(gossip) 요청 처리
- `HintsDispatcher` (internal): 힌티드 핸드오프(hinted handoff) 수행
- `InternalResponseStage` (internal): 클러스터 내부(intra-cluster) 콜백 처리
- `MemtableFlushWriter` (internal): 멤테이블을 디스크에 씀
- `MemtablePostFlush` (internal): 플러시 후 커밋 로그(commit log) 정리 수행
- `MemtableReclaimMemory` (internal): 멤테이블 재활용(recycling) 수행
- `MigrationStage` (internal): 스키마 마이그레이션(schema migration) 수행
- `MiscStage` (internal): 기타 잡다한 태스크 처리
- `PendingRangeCalculator` (internal): 토큰 범위(token range) 계산
- `PerDiskMemtableFlushWriter_0` (internal): 디스크별 사양에 따른 쓰기 수행(0번부터 N번까지)
- `Sampler` (internal): SSTable 인덱스 요약(index summary) 재샘플링
- `SecondaryIndexManagement` (internal): 보조 인덱스(secondary index) 갱신
- `ValidationExecutor` (internal): 검증 컴팩션(validation compaction) 및 스크럽(scrubbing) 수행
- `ViewBuildExecutor` (internal): 구체화된 뷰의 초기 빌드(initial build) 수행

---

### 클라이언트 요청 메트릭(Client Request Metrics)

- 클라이언트 요청 메트릭은 코디네이터 노드가 처리하는 클라이언트 요청에 대한 통계 노출

형식(Format):

- 메트릭 이름: `org.apache.cassandra.metrics.ClientRequest.<MetricName>.<RequestType>`
- JMX MBean: `org.apache.cassandra.metrics:type=ClientRequest scope=<RequestType> name=<MetricName>`

#### CASRead 메트릭 (경량 트랜잭션 읽기)

- `Timeouts` (Counter): 발생한 타임아웃(timeout) 수
- `Failures` (Counter): 발생한 트랜잭션 실패 수
- `Latency` (Latency): 트랜잭션 읽기 레이턴시
- `Unavailables` (Counter): 발생한 사용 불가(unavailable) 예외 수
- `UnfinishedCommit` (Counter): 읽기 중 커밋된 트랜잭션 수
- `ConditionNotMet` (Counter): 사전 조건(precondition)이 일치하지 않은 트랜잭션 수
- `ContentionHistogram` (Histogram): 경합이 발생한(contended) 읽기의 분포

#### CASWrite 메트릭 (경량 트랜잭션 쓰기)

- `Timeouts` (Counter): 발생한 타임아웃 수
- `Failures` (Counter): 발생한 트랜잭션 실패 수
- `Latency` (Latency): 트랜잭션 쓰기 레이턴시
- `Unavailables` (Counter): 발생한 사용 불가 예외 수
- `UnfinishedCommit` (Counter): 쓰기 중 커밋된 트랜잭션 수
- `ConditionNotMet` (Counter): 사전 조건이 일치하지 않은 트랜잭션 수
- `ContentionHistogram` (Histogram): 경합이 발생한 쓰기의 분포
- `MutationSizeHistogram` (Histogram): 변경(mutation)의 총 크기(바이트)

#### Read 메트릭 (읽기)

- `Timeouts` (Counter): 발생한 타임아웃 수
- `Failures` (Counter): 발생한 읽기 실패 수
- `Latency` (Latency): 읽기 레이턴시
- `Unavailables` (Counter): 발생한 사용 불가 예외 수

#### RangeSlice 메트릭 (범위 쿼리)

- `Timeouts` (Counter): 발생한 타임아웃 수
- `Failures` (Counter): 발생한 범위 쿼리(range query) 실패 수
- `Latency` (Latency): 범위 쿼리 레이턴시
- `Unavailables` (Counter): 발생한 사용 불가 예외 수

#### Write 메트릭 (쓰기)

- `Timeouts` (Counter): 발생한 타임아웃 수
- `Failures` (Counter): 발생한 쓰기 실패 수
- `Latency` (Latency): 쓰기 레이턴시
- `Unavailables` (Counter): 발생한 사용 불가 예외 수
- `MutationSizeHistogram` (Histogram): 변경의 총 크기(바이트)

#### ViewWrite 메트릭 (구체화된 뷰 쓰기)

- `Timeouts` (Counter): 발생한 타임아웃 수
- `Failures` (Counter): 발생한 트랜잭션 실패 수
- `Unavailables` (Counter): 발생한 사용 불가 예외 수
- `ViewReplicasAttempted` (Counter): 시도된 뷰 레플리카 쓰기의 수
- `ViewReplicasSuccess` (Counter): 성공한 뷰 레플리카 쓰기의 수
- `ViewPendingMutations` (Gauge\<Long\>): 시도된 쓰기 수에서 성공한 쓰기 수를 뺀 값
- `ViewWriteLatency` (Timer): 베이스 테이블(base table) 변경 시점과 뷰에 대한 CL.ONE 사이의 시간

---

### 캐시 메트릭(Cache Metrics)

- Cassandra는 여러 종류의 캐시(cache)에 대해 다음 메트릭 수집

형식(Format):

- 메트릭 이름: `org.apache.cassandra.metrics.Cache.<MetricName>.<CacheName>`
- JMX MBean: `org.apache.cassandra.metrics:type=Cache scope=<CacheName> name=<MetricName>`

메트릭 목록:

- `Capacity` (Gauge\<Long\>): 캐시 용량(바이트)
- `Entries` (Gauge\<Integer\>): 캐시 엔트리(entry)의 총 개수
- `FifteenMinuteCacheHitRate` (Gauge\<Double\>): 15분 단위 캐시 적중률
- `FiveMinuteCacheHitRate` (Gauge\<Double\>): 5분 단위 캐시 적중률
- `OneMinuteCacheHitRate` (Gauge\<Double\>): 1분 단위 캐시 적중률
- `HitRate` (Gauge\<Double\>): 전체 기간(all-time) 캐시 적중률
- `Hits` (Meter): 캐시 적중의 총 횟수
- `Misses` (Meter): 캐시 미스의 총 횟수
- `MissLatency` (Timer): 미스의 레이턴시
- `Requests` (Gauge\<Long\>): 캐시 요청의 총 횟수
- `Size` (Gauge\<Long\>): 점유된(occupied) 캐시 크기(바이트)

캐시 종류:

- `CounterCache`: 성능을 위해 인기 있는(hot) 카운터를 메모리에 유지
- `ChunkCache`: 프로세스 내(in-process) 비압축(uncompressed) 페이지 캐시
- `KeyCache`: 파티션을 SSTable 오프셋(offset)으로 매핑하는 캐시
- `RowCache`: 메모리에 유지되는 로우(row)에 대한 캐시

- 참고: `Misses`·`MissLatency`는 ChunkCache에만 정의됨

---

### CQL 메트릭(CQL Metrics)

- CQL 실행 관련 메트릭 노출

형식(Format):

- 메트릭 이름: `org.apache.cassandra.metrics.CQL.<MetricName>`
- JMX MBean: `org.apache.cassandra.metrics:type=CQL name=<MetricName>`

메트릭 목록:

- `PreparedStatementsCount` (Gauge\<Integer\>): 캐시된 준비된 구문(prepared statement)의 개수
- `PreparedStatementsEvicted` (Counter): 캐시에서 축출(evict)된 준비된 구문의 수
- `PreparedStatementsExecuted` (Counter): 실행된 준비된 구문의 수
- `RegularStatementsExecuted` (Counter): 실행된 비준비(non-prepared) 구문의 수
- `PreparedStatementsRatio` (Gauge\<Double\>): 준비된 구문 대 비준비 구문의 비율(퍼센트)

---

### 드롭된 메시지 메트릭(DroppedMessage Metrics)

- 내부 타임아웃으로 인해 드롭된(dropped) 메시지 추적
- 노드 또는 클러스터에 부하가 걸려 있는지 진단할 때 유용

형식(Format):

- 메트릭 이름: `org.apache.cassandra.metrics.DroppedMessage.<MetricName>.<Type>`
- JMX MBean: `org.apache.cassandra.metrics:type=DroppedMessage scope=<Type> name=<MetricName>`

메트릭 목록:

- `CrossNodeDroppedLatency` (Timer): 노드 간(cross-node) 드롭 레이턴시
- `InternalDroppedLatency` (Timer): 노드 내부(within node) 드롭 레이턴시
- `Dropped` (Meter): 드롭된 메시지의 수

메시지 타입:

- `BATCH_STORE`: 배치로그(batchlog) 쓰기
- `BATCH_REMOVE`: 성공적으로 적용된 후 수행되는 배치로그 정리
- `COUNTER_MUTATION`: 카운터 쓰기
- `HINT`: 힌트(hint) 재생(replay)
- `MUTATION`: 일반 쓰기
- `READ`: 일반 읽기
- `READ_REPAIR`: 읽기 복구(read repair)
- `PAGED_SLICE`: 페이지 단위(paged) 읽기
- `RANGE_SLICE`: 토큰 범위(token range) 읽기
- `REQUEST_RESPONSE`: RPC 콜백(callback)
- `_TRACE`: 트레이싱(tracing) 쓰기

---

### 스트리밍 메트릭(Streaming Metrics)

- 노드 간 스트리밍 작업(부트스트랩, 복구, 디커미션 등) 동안 각 피어(peer)별로 수집되는 메트릭

형식(Format):

- 메트릭 이름: `org.apache.cassandra.metrics.Streaming.<MetricName>.<PeerIP>`
- JMX MBean: `org.apache.cassandra.metrics:type=Streaming scope=<PeerIP> name=<MetricName>`

메트릭 목록:

- `IncomingBytes` (Counter): 피어로부터 이 노드로 스트리밍된 바이트 수
- `OutgoingBytes` (Counter): 이 노드로부터 피어로 스트리밍된 바이트 수

---

### 컴팩션 메트릭(Compaction Metrics)

- 컴팩션 활동에 대한 메트릭 노출

형식(Format):

- 메트릭 이름: `org.apache.cassandra.metrics.Compaction.<MetricName>`
- JMX MBean: `org.apache.cassandra.metrics:type=Compaction name=<MetricName>`

메트릭 목록:

- `BytesCompacted` (Counter): 서버 시작 이후 컴팩션된 총 바이트 수
- `PendingTasks` (Gauge\<Integer\>): 추정 남은 컴팩션 수
- `CompletedTasks` (Gauge\<Long\>): 서버 시작 이후 완료된 컴팩션 수
- `TotalCompactionsCompleted` (Meter): 완료된 컴팩션의 처리량(throughput)
- `PendingTasksByTableName` (Gauge\<Map\<String, Map\<String, Integer\>\>\>): 키스페이스 및 테이블별 대기 중 컴팩션 수

---

### 커밋로그 메트릭(CommitLog Metrics)

- 커밋 로그(commit log)에 대한 메트릭 노출

형식(Format):

- 메트릭 이름: `org.apache.cassandra.metrics.CommitLog.<MetricName>`
- JMX MBean: `org.apache.cassandra.metrics:type=CommitLog name=<MetricName>`

메트릭 목록:

- `CompletedTasks` (Gauge\<Long\>): 재시작 이후 기록된 커밋 로그 메시지의 수
- `PendingTasks` (Gauge\<Long\>): fsync를 대기 중인 커밋 로그 메시지의 수
- `TotalCommitLogSize` (Gauge\<Long\>): 모든 커밋 로그 세그먼트(segment)의 현재 크기(바이트)
- `WaitingOnSegmentAllocation` (Timer): CommitLogSegment 할당(allocation)을 대기한 시간
- `WaitingOnCommit` (Timer): CommitLog의 fsync를 대기한 시간

---

### 스토리지 메트릭(Storage Metrics)

- 노드 전역의 스토리지 관련 메트릭 노출

형식(Format):

- 메트릭 이름: `org.apache.cassandra.metrics.Storage.<MetricName>`
- JMX MBean: `org.apache.cassandra.metrics:type=Storage name=<MetricName>`

메트릭 목록:

- `Exceptions` (Counter): 포착된 내부 예외(internal exception)의 수
- `Load` (Counter): 이 노드가 관리하는 디스크상 데이터 크기(바이트)
- `TotalHints` (Counter): 재시작 이후 기록된 힌트 메시지의 수
- `TotalHintsInProgress` (Counter): 현재 전송 중인 힌트의 수

---

### 힌티드 핸드오프 메트릭(HintedHandoff Metrics)

- 힌티드 핸드오프 매니저(Hinted Handoff Manager)에 대한 메트릭 노출

형식(Format):

- 메트릭 이름: `org.apache.cassandra.metrics.HintedHandOffManager.<MetricName>`
- JMX MBean: `org.apache.cassandra.metrics:type=HintedHandOffManager name=<MetricName>`

메트릭 목록:

- `Hints_created-<PeerIP>` (Counter): 이 피어에 대해 디스크에 저장된 힌트의 수
- `Hints_not_stored-<PeerIP>` (Counter): 피어의 다운타임(downtime)이 힌트 윈도우(hint window)를 초과하여 저장되지 못한 힌트의 수

---

### 힌트 서비스 메트릭(HintsService Metrics)

- 힌트 서비스(Hints Service)에 대한 메트릭 노출

형식(Format):

- 메트릭 이름: `org.apache.cassandra.metrics.HintsService.<MetricName>`
- JMX MBean: `org.apache.cassandra.metrics:type=HintsService name=<MetricName>`

메트릭 목록:

- `HintsSucceeded` (Meter): 성공적으로 전달된 힌트의 수
- `HintsFailed` (Meter): 전달에 실패한 힌트의 수
- `HintsTimedOut` (Meter): 타임아웃된 힌트의 수
- `Hint_delays` (Histogram): 힌트 전달 지연(delay)의 히스토그램(밀리초 단위)
- `Hint_delays-<PeerIP>` (Histogram): 피어별 힌트 전달 지연의 히스토그램(밀리초 단위)

---

### SSTable 인덱스 메트릭(SSTable Index Metrics)

- SSTable 내 로우 인덱스 엔트리(row index entry)에 대한 메트릭 노출

형식(Format):

- 메트릭 이름: `org.apache.cassandra.metrics.Index.<MetricName>.RowIndexEntry`
- JMX MBean: `org.apache.cassandra.metrics:type=Index scope=RowIndexEntry name=<MetricName>`

메트릭 목록:

- `IndexedEntrySize` (Histogram): 모든 SSTable에 걸친 온힙 인덱스 크기(바이트)
- `IndexInfoCount` (Histogram): 모든 SSTable에 걸쳐 관리되는 온힙 인덱스 엔트리의 수
- `IndexInfoGets` (Histogram): SSTable당 수행된 인덱스 탐색(seek)의 수

---

### 버퍼 풀 메트릭(BufferPool Metrics)

- Cassandra의 버퍼 풀(buffer pool)에 대한 메트릭 노출

형식(Format):

- 메트릭 이름: `org.apache.cassandra.metrics.BufferPool.<MetricName>`
- JMX MBean: `org.apache.cassandra.metrics:type=BufferPool name=<MetricName>`

메트릭 목록:

- `Size` (Gauge\<Long\>): 관리되는 버퍼 풀의 크기(바이트)
- `Misses` (Meter): 풀 미스(miss)의 비율, 발생한 할당(allocation)을 나타냄

---

### 클라이언트 메트릭(Client Metrics)

- 연결된 클라이언트(client)에 대한 메트릭 노출

형식(Format):

- 메트릭 이름: `org.apache.cassandra.metrics.Client.<MetricName>`
- JMX MBean: `org.apache.cassandra.metrics:type=Client name=<MetricName>`

메트릭 목록:

- `connectedNativeClients` (Gauge\<Integer\>): 네이티브 프로토콜(native protocol) 서버에 연결된 클라이언트의 수
- `connections` (Gauge\<List\<Map\<String, String\>\>\>): 모든 연결과 그 상태 정보(state information)
- `connectedNativeClientsByUser` (Gauge\<Map\<String, Int\>\>): 사용자 이름(username)별로 연결된 네이티브 클라이언트의 수

---

### 배치 메트릭(Batch Metrics)

- 배치(batch) 구문에 대한 메트릭 노출

형식(Format):

- 메트릭 이름: `org.apache.cassandra.metrics.Batch.<MetricName>`
- JMX MBean: `org.apache.cassandra.metrics:type=Batch name=<MetricName>`

메트릭 목록:

- `PartitionsPerCounterBatch` (Histogram): 카운터 배치당 처리된 파티션의 분포
- `PartitionsPerLoggedBatch` (Histogram): 로깅된(logged) 배치당 처리된 파티션의 분포
- `PartitionsPerUnloggedBatch` (Histogram): 로깅되지 않은(unlogged) 배치당 처리된 파티션의 분포

---

### JVM 메트릭(JVM Metrics)

- 메모리와 가비지 컬렉션(garbage collection)에 대한 JVM 메트릭은 JMX 또는 메트릭 리포터로 접근 가능

#### BufferPool (JVM 버퍼 풀)

형식(Format):

- 메트릭 이름: `jvm.buffers.<direct|mapped>.<MetricName>`
- JMX MBean: `java.nio:type=BufferPool name=<direct|mapped>`

메트릭 목록:

- `Capacity` (Gauge\<Long\>): 추정 버퍼 풀 총 용량
- `Count` (Gauge\<Long\>): 풀 내 추정 버퍼 개수
- `Used` (Gauge\<Long\>): 버퍼 풀이 사용하는 추정 메모리

#### FileDescriptorRatio (파일 디스크립터 비율)

형식(Format):

- 메트릭 이름: `jvm.fd.<MetricName>`
- JMX MBean: `java.lang:type=OperatingSystem name=<OpenFileDescriptorCount|MaxFileDescriptorCount>`

메트릭 목록:

- `Usage` (Ratio): 사용 중인 파일 디스크립터 대 전체 파일 디스크립터의 비율

#### GarbageCollector (가비지 컬렉터)

형식(Format):

- 메트릭 이름: `jvm.gc.<gc_type>.<MetricName>`
- JMX MBean: `java.lang:type=GarbageCollector name=<gc_type>`

메트릭 목록:

- `Count` (Gauge\<Long\>): 발생한 컬렉션(collection)의 총 횟수
- `Time` (Gauge\<Long\>): 누적된 컬렉션 경과 시간의 근사치(밀리초)

#### Memory (메모리)

형식(Format):

- 메트릭 이름: `jvm.memory.<heap/non-heap/total>.<MetricName>`
- JMX MBean: `java.lang:type=Memory`

메트릭 목록:

- `Committed` (Gauge\<Long\>): JVM 사용을 위해 커밋된(committed) 메모리(바이트)
- `Init` (Gauge\<Long\>): OS로부터 최초 요청한 메모리(바이트)
- `Max` (Gauge\<Long\>): 관리를 위한 최대 메모리(바이트)
- `Usage` (Ratio): 사용된 메모리 대 최대 메모리의 비율
- `Used` (Gauge\<Long\>): 사용된 메모리(바이트)

#### MemoryPool (메모리 풀)

형식(Format):

- 메트릭 이름: `jvm.memory.pools.<memory_pool>.<MetricName>`
- JMX MBean: `java.lang:type=MemoryPool name=<memory_pool>`

메트릭 목록:

- `Committed` (Gauge\<Long\>): JVM 사용을 위해 커밋된 메모리(바이트)
- `Init` (Gauge\<Long\>): OS로부터 최초 요청한 메모리(바이트)
- `Max` (Gauge\<Long\>): 관리를 위한 최대 메모리(바이트)
- `Usage` (Ratio): 사용된 메모리 대 최대 메모리의 비율
- `Used` (Gauge\<Long\>): 사용된 메모리(바이트)

---

### 메트릭 접근 방법

#### JMX 접근

- 모든 JMX 기반 클라이언트는 Cassandra의 메트릭에 접근 가능
- JMX 메트릭에 HTTP로 접근하려면 Mx4jTool을 다운로드해 클래스패스(classpath)에 배치
  - 정상 설정 시 시작 로그에 다음 출력됨

```
HttpAdaptor version 3.0.2 started on port 8081
```

- 포트(port)·주소(address) 설정은 `cassandra-env.sh` 파일의 `MX4J_ADDRESS`·`MX4J_PORT` 옵션으로 구성 가능

#### 메트릭 리포터(Metric Reporters)

- Cassandra의 메트릭은 Dropwizard Metrics 라이브러리의 내장 리포터(built-in reporters) 또는 서드파티 리포터 플러그인(third-party reporter plug-ins)으로 외부 모니터링 시스템에 내보내기(export) 가능
  - Graphite, Prometheus 등 다양한 모니터링 도구와 연동 가능

---

### 가상 테이블을 통한 모니터링(Virtual Tables)

> 원본: https://cassandra.apache.org/doc/latest/cassandra/managing/operating/virtualtables.html

#### 개요

- 가상 테이블(Virtual Tables)은 Apache Cassandra 4.0 도입 기능, SSTable로 명시적으로 관리·저장되는 데이터 대신 API에 의해 뒷받침되는(backed by an API) 테이블 제공
- 이 테이블은 특수한 가상 키스페이스(virtual keyspace) 안에 존재 → CQL 쿼리로 운영 메트릭과 구성 정보 노출

#### 주요 특성

가상 테이블의 고유한 속성:

- 읽기 전용(Read-only) 접근: 사용자는 가상 테이블을 생성하는 DDL이나 수정하는 DML 실행 불가
- 노드 로컬(Node-local): 클러스터 전체에 복제(replicate)되지 않음 → 각 노드가 자신의 가상 테이블 유지
- SSTable 없음: 데이터는 전통적인 저장 파일이 아니라 API로부터 제공됨
- 일관성 무시(Consistency ignored): 쿼리의 일관성 수준(consistency level) 미적용
- LocalPartitioner: 해시 값(hash value)이 아니라 파티션 키(partition key) 기준으로 정렬
- 고급 쿼리 허용: 일반적으로 비권장인 `ALLOW FILTERING`과 집계(aggregation)도 실행 가능

#### 가상 키스페이스(Virtual Keyspaces)

Cassandra가 구현하는 두 개의 전용 키스페이스:

- `system_virtual_schema`: 가상 테이블의 스키마를 정의하는 메타데이터 테이블(`keyspaces`, `columns`, `tables`) 포함
- `system_views`: 실제로 쿼리 가능한 가상 테이블 보관

#### 가상 테이블의 제약 사항

제약 사항:

- 가상 키스페이스는 변경(alter)·삭제(drop) 불가
- 가상 테이블은 DDL을 통한 생성, 수정, 트렁케이트(truncate), 삭제 불가
- 보조 인덱스(secondary index), 타입(type), 함수(function), 집계(aggregate), 구체화된 뷰(materialized view), 트리거(trigger) 미지원
- TTL 컬럼 생성 불가
- 조건부 갱신/삭제(conditional update/delete) 금지
- 가상 테이블의 변경(mutation)은 일반 테이블과 함께 배치 구문(batch statement)에 참여 불가

#### 전체 가상 테이블 목록

- `caches`: 캐시 메트릭(chunks, counters, keys, rows) → 용량, 적중률, 요청 속도 포함
- `cidr_filtering_metrics_counts`: CIDR 필터 접근의 허용/거부(acceptance/rejection) 횟수
- `cidr_filtering_metrics_latencies`: CIDR 필터의 레이턴시 측정값(나노초 단위)
- `clients`: 드라이버 정보, SSL 상태, 요청 횟수를 포함한 활성 클라이언트 연결
- `coordinator_read_latency`: 코디네이터 수준의 읽기 작업 메트릭
- `coordinator_scan`: 코디네이터 수준의 스캔 작업 메트릭
- `coordinator_write_latency`: 코디네이터 수준의 쓰기 작업 메트릭
- `cql_metrics`: 준비된 구문(prepared statement) 캐시 통계
- `disk_usage`: 키스페이스 및 테이블별 스토리지 소비량
- `internode_inbound`: 인바운드(inbound) 노드 간(internode) 메시징 통계
- `internode_outbound`: 아웃바운드(outbound) 노드 간 메시징 통계
- `local_read_latency`: 로컬 읽기 작업 성능 데이터
- `local_scan`: 로컬 스캔 작업 성능 데이터
- `local_write_latency`: 로컬 쓰기 작업 성능 데이터
- `max_partition_size`: 최대 파티션 크기 메트릭
- `rows_per_read`: 읽기 작업당 로우(row) 개수 통계
- `settings`: 현재 `cassandra.yaml` 구성 값
- `sstable_tasks`: 진행 중인 컴팩션/업그레이드 작업과 그 진행 상황
- `system_logs`: CQLLOG 어펜더(appender)를 통한 Cassandra 로그
- `system_properties`: JVM 시스템 속성(system properties)
- `thread_pools`: 스레드 풀 메트릭(활성, 대기, 차단 태스크)
- `tombstones_per_read`: 읽기당 툼스톤(tombstone) 개수 통계

#### 주요 가상 테이블 상세

##### clients 테이블

- 연결된 클라이언트를 나열, 주소(address), 포트(port), 드라이버 이름/버전, 프로토콜 버전(protocol version), 요청 횟수, SSL 정보 등의 컬럼 포함
- 업그레이드 전 오래된 드라이버 식별 또는 과도한 요청 패턴 탐지 시 유용

##### caches 테이블

- 네 가지 캐시 타입(chunks, counters, keys, rows)에 대한 메트릭 제공
- 각 항목은 용량(capacity), 엔트리 개수, 적중률(hit ratio), 최근 요청 속도(request rate)를 보여줌

##### settings 테이블

- 모든 활성 `cassandra.yaml` 구성 매개변수 노출(암호화 옵션은 마스킹(mask)됨)
- 구성 파일에 직접 접근하지 않고도 런타임 구성 확인 시 유용

##### thread_pools 테이블

- 모든 스레드 풀의 세부 정보(활성 태스크 수, 한계(limit), 완료된 태스크, 대기 작업) 제공
- 병목(bottleneck)과 자원 제약 식별 시 유용

##### sstable_tasks 테이블

- 컴팩션과 같은 진행 중인 작업 추적, 전체 작업량 대비 진행 상황을 바이트 단위로 표시
- 장시간 실행되는 유지보수 작업 모니터링 시 유용

#### 사용 예시

디스크 사용량이 높은 테이블 찾기:

```sql
SELECT * FROM disk_usage WHERE mebibytes > 1 ALLOW FILTERING;
```

읽기 레이턴시가 높은 테이블 식별:

```sql
SELECT * FROM local_read_latency WHERE per_second > 1 ALLOW FILTERING;
```

SSTable 작업의 남은 작업량 계산:

```sql
SELECT total - progress AS remaining FROM system_views.sstable_tasks;
```

#### 구현 참고 사항

- CASSANDRA-18238에 따라, `system_logs`를 제외한 모든 가상 테이블은 CQL 명세상 필요할 때 `ALLOW FILTERING`을 자동 적용 → 사용성 향상
- 가상 테이블은 `DESCRIBE TABLE` 구문으로 설명(describe) 가능하나, 그 결과로 나오는 DDL로는 가상 테이블 재생성 불가
  - 가상 테이블은 전적으로 Cassandra 내부에서 관리되기 때문

---

### 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [Cassandra Metrics 공식 문서](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/metrics.html)
- [Cassandra Virtual Tables 공식 문서](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/virtualtables.html)
- [Dropwizard Metrics](https://metrics.dropwizard.io/)

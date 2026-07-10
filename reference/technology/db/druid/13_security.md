# Druid 보안

> 원본: https://druid.apache.org/docs/latest/operations/security-overview
> 원본: https://druid.apache.org/docs/latest/operations/security-user-auth
> 원본: https://druid.apache.org/docs/latest/operations/auth-ldap
> 원본: https://druid.apache.org/docs/latest/operations/tls-support
> 원본: https://druid.apache.org/docs/latest/operations/password-provider
> 원본: https://druid.apache.org/docs/latest/operations/dynamic-config-provider

Druid 클러스터의 보안 모범 사례, 인증(authentication)과 인가(authorization) 모델, LDAP 연동, TLS 설정, 패스워드 프로바이더와 다이내믹 설정 프로바이더를 차례로 설명합니다.

---

## 목차

1. [보안 개요](#보안-개요)
2. [인증과 인가 모델](#인증과-인가-모델)
3. [Basic 보안 익스텐션 설정](#basic-보안-익스텐션-설정)
4. [LDAP 인증](#ldap-인증)
5. [TLS 지원](#tls-지원)
6. [패스워드 프로바이더](#패스워드-프로바이더)
7. [다이내믹 설정 프로바이더](#다이내믹-설정-프로바이더)
8. [참고 자료](#참고-자료)

---

## 보안 개요

Druid는 배포를 단순하게 하기 위해 기본적으로 보안 기능을 모두 비활성화한 상태로 출시됩니다. 프로덕션 환경에서는 TLS, 인증, 인가를 반드시 활성화해야 합니다.

### 클러스터 구성 모범 사례

**사용자와 프로세스 관리**

- Druid를 권한 없는(unprivileged) Unix 사용자로 실행합니다. root 사용자로 실행하지 않습니다.
- Druid 관리자는 Druid 프로세스를 실행하는 OS 계정의 권한을 그대로 상속하므로, root로 실행하면 파일 시스템 접근 관련 취약점이 생길 수 있습니다.

**접근 제어**

- 프로덕션 환경과 신뢰할 수 없는 네트워크에서는 인증을 활성화합니다.
- 웹 콘솔을 노출하기 전에 인가를 먼저 활성화합니다.
- 사용자 권한을 부여할 때는 최소 권한 원칙(principle of least privilege)을 적용합니다.
- `consumerProperties` 같은 설정 파일에 평문(plain-text) 패스워드를 저장하지 않습니다.

**코드 보안**

- JavaScript 기능은 보안 가이드에 따라 비활성화합니다.

### 네트워크 보안 권장 사항

- 클러스터 내부 통신과 외부 통신 모두에 TLS 암호화를 활성화합니다.
- API 게이트웨이를 두어 접근을 제한하고, 허용 목록(allowlist)을 만들고, 요청량 제한(throttling)을 적용합니다.
- 방화벽 규칙으로 노출 서비스를 특정 포트와 IP 대역으로 제한합니다.
- Broker 포트는 인가된 다운스트림 쿼리 애플리케이션만 접근하도록 제한합니다.

### 인가 모델 모범 사례

권한은 신뢰 수준에 따라 차등 부여합니다.

- **`DATASOURCE`에 대한 `WRITE` 권한**: 사실상 Druid 프로세스 수준의 권한을 얻게 되므로 매우 신뢰하는 사용자에게만 부여합니다.
- **`STATE READ`, `STATE WRITE`, `CONFIG WRITE`, `DATASOURCE WRITE`**: 시스템 수준 접근이 필요한, 극히 신뢰하는 사용자로 제한합니다.
- **입력 소스 검증**: 클라이언트 애플리케이션은 사용자가 제어하는 인제스천(ingestion) 태스크 URL을 검증하여, 로컬 파일 시스템이나 내부 네트워크 자원이 악용되지 않도록 해야 합니다.

### 보안 신뢰 모델(Trust Model)

**사용자 권한 계층**

- **리소스 WRITE 권한 보유 사용자**: Druid 프로세스 자체와 동등한 수준의 접근 권한을 갖습니다.
- **인증된 읽기 전용 사용자**: 인가된 데이터소스에 대한 쿼리만 가능합니다.
- **권한 없는 사용자**: 어떤 리소스에도 의존하지 않는 쿼리만 실행할 수 있습니다.

**프로세스 수준 신뢰 가정**

- Druid 프로세스는 실행 Unix 사용자의 파일 권한을 상속하며, 인제스천 태스크 워커도 부모 프로세스의 자격 증명을 상속합니다. 태스크 제출 권한을 부여하면 암묵적으로 파일 시스템 접근 권한도 부여하는 셈입니다.
- Druid는 격리되고 보호된 네트워크에서 내부 트래픽이 암호화된 상태로 동작한다고 가정합니다.
- 메타데이터 저장소와 ZooKeeper 노드가 안전하다고 가정합니다. 방화벽과 네트워크 통제는 운영자의 책임입니다.

**저장소와 클라이언트 신뢰 경계**

- **딥 스토리지(deep storage)**: 해당 스토리지 시스템 고유의 보안 정책을 따르며, 파일 암호화도 스토리지 시스템의 기능에 위임합니다.
- **클라이언트 통신**: Authenticator가 사용자를 검증하고 Authorizer가 권한을 집행합니다.

### 보안 취약점 신고

취약점은 `security@apache.org`로 신고합니다. 이 채널은 비공개 메일링 리스트이며, 취약점 하나당 평문 이메일 한 통으로 신고합니다.

처리 절차는 다음과 같습니다.

1. 비공개로 취약점 제출
2. 신고 접수 확인
3. 비공개 협업으로 수정
4. 패치 릴리스 배포
5. 수정 안내와 함께 취약점 공개 발표

---

## 인증과 인가 모델

Druid 보안 모델의 핵심은 **리소스**(사용자가 접근하는 대상)와 **액션**(사용자가 수행하는 작업)입니다. 요청은 Authenticator의 인증 검사를 통과한 뒤 Authorizer의 인가 평가를 받습니다.

### 리소스 타입

| 리소스 타입 | 설명 |
| --- | --- |
| `DATASOURCE` | SQL로 접근할 수 있는 개별 Druid 테이블 |
| `CONFIG` | 클러스터 구성 요소의 설정 엔드포인트 |
| `EXTERNAL` | SQL의 EXTERN 함수로 접근하는 외부 데이터 |
| `STATE` | 클러스터 전체 운영 상태 엔드포인트 |
| `SYSTEM_TABLE` | 시스템 스키마 테이블 (`druid.sql.planner.authorizeSystemTablesDirectly` 활성화 시) |

### 액션

| 액션 | 설명 |
| --- | --- |
| `READ` | 읽기 전용 작업 (주로 GET 요청) |
| `WRITE` | 변경 작업 (POST/DELETE 요청) |

주의할 점으로, 리소스에 대한 `WRITE` 권한이 `READ` 권한을 포함하지 않습니다. 둘 다 필요하면 각각 명시적으로 부여해야 합니다.

### 리소스 타입별 권한 상세

- **`DATASOURCE`**: 리소스 이름이 실제 데이터소스 이름과 대응하므로 데이터소스 단위로 세밀한 접근 제어가 가능합니다.
- **`CONFIG`**: 리소스 이름이 두 가지입니다.
  - `"CONFIG"` — Coordinator, Overlord, MiddleManager 프로세스의 워커 관리·설정 엔드포인트
  - `"security"` — Coordinator의 `/druid-ext/basic-security/authentication`, `/druid-ext/basic-security/authorization` 엔드포인트
- **`EXTERNAL`**: 리소스 이름은 `"EXTERNAL"`만 허용합니다. EXTERN 함수를 포함한 쿼리 실행 권한을 부여합니다.
- **`STATE`**: 리소스 이름은 `"STATE"` 하나이며, Coordinator 운영 엔드포인트(`/druid/coordinator/v1/*`), Broker 상태 라우트, Overlord 리더 엔드포인트, 워커 태스크 관리, Historical 세그먼트 정보 등에 대한 접근을 제어합니다.
- **`SYSTEM_TABLE`**: 리소스 이름이 실제 시스템 스키마 테이블 이름(예: `sys.segments`, `sys.server_segments`)과 대응합니다.

### SQL 쿼리 인가

- 데이터소스 쿼리에는 해당 데이터소스에 대한 `DATASOURCE READ` 권한이 필요합니다.
- EXTERN 함수를 사용하려면 `EXTERNAL READ` 권한이 필요합니다.
- INFORMATION_SCHEMA 쿼리는 접근 가능한 데이터소스만 반환합니다.
- 시스템 스키마 테이블 접근에는 해당 `READ` 권한이 필요하며, `druid.sql.planner.authorizeSystemTablesDirectly`가 활성화된 경우 `SYSTEM_TABLE` 인가도 필요합니다.

`DATASOURCE`에 대한 `WRITE` 권한은 파일 시스템이나 S3 버킷 같은 민감한 인프라에 대한 광범위한 접근을 허용하므로, 관리자 수준 사용자에게만 선별적으로 부여해야 합니다.

### 기본 사용자 계정

**Authenticator 측**

- **admin 사용자**: `druid.auth.authenticator.<name>.initialAdminPassword`를 설정하면 생성됩니다.
- **druid_system 사용자**: `druid.auth.authenticator.<name>.initialInternalClientPassword`를 설정하면 생성됩니다.

**Authorizer 측**

모든 Authorizer에는 전체 권한을 가진 `admin`과 `druid_system` 계정이 자동으로 포함됩니다.

### 설정 전파

Authenticator와 Authorizer의 메타데이터는 각 프로세스에 캐시되며, 주기적인 폴링으로 갱신됩니다. 폴링 주기는 `druid.auth.basic.common.pollingPeriod`와 `druid.auth.basic.common.maxRandomDelay`로 제어합니다. `enableCacheNotifications` 설정으로 푸시 알림을 켜면 전파 지연을 줄일 수 있습니다.

---

## Basic 보안 익스텐션 설정

`druid-basic-security` 익스텐션을 사용하면 메타데이터 저장소 기반의 사용자/패스워드 인증과 역할(role) 기반 인가를 구성할 수 있습니다.

### common.runtime.properties 설정

```properties
druid.extensions.loadList=["druid-basic-security", "druid-histogram",
  "druid-datasketches", "druid-kafka-indexing-service"]

# Authenticator
druid.auth.authenticatorChain=["MyBasicMetadataAuthenticator"]
druid.auth.authenticator.MyBasicMetadataAuthenticator.type=basic
druid.auth.authenticator.MyBasicMetadataAuthenticator.initialAdminPassword=password1
druid.auth.authenticator.MyBasicMetadataAuthenticator.initialInternalClientPassword=password2
druid.auth.authenticator.MyBasicMetadataAuthenticator.credentialsValidator.type=metadata
druid.auth.authenticator.MyBasicMetadataAuthenticator.skipOnFailure=false
druid.auth.authenticator.MyBasicMetadataAuthenticator.authorizerName=MyBasicMetadataAuthorizer

# Escalator (내부 프로세스 간 통신용 자격 증명)
druid.escalator.type=basic
druid.escalator.internalClientUsername=druid_system
druid.escalator.internalClientPassword=password2
druid.escalator.authorizerName=MyBasicMetadataAuthorizer

# Authorizer
druid.auth.authorizers=["MyBasicMetadataAuthorizer"]
druid.auth.authorizer.MyBasicMetadataAuthorizer.type=basic
```

### API로 사용자와 역할 관리

사용자 관리 API는 Coordinator 엔드포인트에서 제공합니다. (비 TLS 포트 8081, TLS 포트 8281)

**사용자 생성 (Authenticator)**

```
POST /druid-ext/basic-security/authentication/db/MyBasicMetadataAuthenticator/users/<USERNAME>
```

**자격 증명 설정**

```
POST /druid-ext/basic-security/authentication/db/MyBasicMetadataAuthenticator/users/<USERNAME>/credentials
```

```json
{"password": "my_password"}
```

**사용자 생성 (Authorizer)**

```
POST /druid-ext/basic-security/authorization/db/MyBasicMetadataAuthorizer/users/<USERNAME>
```

**역할 생성**

```
POST /druid-ext/basic-security/authorization/db/MyBasicMetadataAuthorizer/roles/<ROLENAME>
```

**사용자에게 역할 부여**

```
POST /druid-ext/basic-security/authorization/db/MyBasicMetadataAuthorizer/users/<USERNAME>/roles/<ROLENAME>
```

**역할에 권한 부여**

```
POST /druid-ext/basic-security/authorization/db/MyBasicMetadataAuthorizer/roles/<ROLENAME>/permissions
```

권한 페이로드 구조는 다음과 같습니다.

```json
[
  {
    "resource": {
      "type": "DATASOURCE",
      "name": "<PATTERN>"
    },
    "action": "READ"
  },
  {
    "resource": {
      "type": "STATE",
      "name": "STATE"
    },
    "action": "READ"
  }
]
```

리소스 이름은 정규 표현식으로 동작하므로, 하나의 권한으로 여러 데이터소스에 대한 접근을 부여할 수 있습니다.

---

## LDAP 인증

`druid-basic-security` 익스텐션의 credentials validator를 `ldap`으로 지정하면 LDAP 디렉터리로 사용자를 인증할 수 있습니다.

### 사전 확인

설정 전에 LDAP 서버 연결을 먼저 검증합니다.

```bash
ldapwhoami -vv -H ldap://ip_address:389 -D "myuser@example.com" -W
```

디렉터리에서 사용자를 검색할 수 있는지도 확인합니다.

```bash
ldapsearch -x -W -H ldap://ip_address:389 -D "cn=admin,dc=example,dc=com" \
  -b "dc=example,dc=com" "(sAMAccountName=myuser)" +
```

검색 결과의 `memberOf` 속성이 그룹 멤버십을 나타내며, Druid는 이 속성으로 사용자의 소속 그룹을 판별합니다.

### 기본 설정

`druid-basic-security` 익스텐션을 활성화하고 `common.runtime.properties`에 다음을 추가합니다.

```properties
druid.auth.authenticatorChain=["ldap"]
druid.auth.authenticator.ldap.type=basic
druid.auth.authenticator.ldap.enableCacheNotifications=true
druid.auth.authenticator.ldap.credentialsValidator.type=ldap
druid.auth.authenticator.ldap.credentialsValidator.url=ldap://ip_address:port
druid.auth.authenticator.ldap.credentialsValidator.bindUser=administrator@example.com
druid.auth.authenticator.ldap.credentialsValidator.bindPassword=adminpassword
druid.auth.authenticator.ldap.credentialsValidator.baseDn=dc=example,dc=com
druid.auth.authenticator.ldap.credentialsValidator.userSearch=(&(sAMAccountName=%s)(objectClass=user))
druid.auth.authenticator.ldap.credentialsValidator.userAttribute=sAMAccountName
druid.auth.authenticator.ldap.authorizerName=ldapauth

druid.escalator.type=basic
druid.escalator.internalClientUsername=internal@example.com
druid.escalator.internalClientPassword=internaluserpassword
druid.escalator.authorizerName=ldapauth

druid.auth.authorizers=["ldapauth"]
druid.auth.authorizer.ldapauth.type=basic
druid.auth.authorizer.ldapauth.initialAdminUser=internal@example.com
druid.auth.authorizer.ldapauth.initialAdminRole=admin
druid.auth.authorizer.ldapauth.roleProvider.type=ldap
```

### 역할과 권한 관리

**Druid 역할 생성**

```bash
curl -i -v -H "Content-Type: application/json" -u internal -X POST \
  http://localhost:8081/druid-ext/basic-security/authorization/db/ldapauth/roles/readRole
```

**역할 목록 조회**

```bash
curl -i -v -H "Content-Type: application/json" -u internal -X GET \
  http://localhost:8081/druid-ext/basic-security/authorization/db/ldapauth/roles
```

**권한 정의 파일 작성 (perm.json)**

```json
[
  { "resource": { "name": "wikipedia", "type": "DATASOURCE" }, "action": "READ" },
  { "resource": { "name": ".*", "type": "STATE" }, "action": "READ" },
  { "resource": { "name": ".*", "type": "CONFIG" }, "action": "READ" }
]
```

**역할에 권한 부여**

```bash
curl -i -v -H "Content-Type: application/json" -u internal -X POST \
  -d@perm.json http://localhost:8081/druid-ext/basic-security/authorization/db/ldapauth/roles/readRole/permissions
```

### LDAP 그룹을 역할에 매핑

**그룹 매핑 파일 작성 (groupmap.json)**

```json
{
  "name": "mygroupmap",
  "groupPattern": "CN=mygroup,CN=Users,DC=example,DC=com",
  "roles": ["readRole"]
}
```

**그룹 매핑 생성**

```bash
curl -i -v -H "Content-Type: application/json" -u internal -X POST \
  -d @groupmap.json http://localhost:8081/druid-ext/basic-security/authorization/db/ldapauth/groupMappings/mygroupmap
```

**그룹 매핑 조회**

```bash
# 전체 매핑 조회
curl -i -v -H "Content-Type: application/json" -u internal -X GET \
  http://localhost:8081/druid-ext/basic-security/authorization/db/ldapauth/groupMappings

# 특정 매핑 조회
curl -i -v -H "Content-Type: application/json" -u internal -X GET \
  http://localhost:8081/druid-ext/basic-security/authorization/db/ldapauth/groupMappings/mygroupmap
```

**기존 매핑에 역할 추가**

```bash
curl -i -v -H "Content-Type: application/json" -u internal -X POST \
  http://localhost:8081/druid-ext/basic-security/authorization/db/ldapauth/groupMappings/mygroup/roles/queryrole
```

### 개별 LDAP 사용자에게 역할 부여

그룹 매핑 대신 개별 사용자 단위로도 역할을 부여할 수 있습니다.

```bash
# LDAP 사용자 추가
curl -i -v -H "Content-Type: application/json" -u internal -X POST \
  http://localhost:8081/druid-ext/basic-security/authorization/db/ldapauth/users/myuser

# 역할 부여
curl -i -v -H "Content-Type: application/json" -u internal -X POST \
  http://localhost:8081/druid-ext/basic-security/authorization/db/ldapauth/users/myuser/roles/queryRole
```

### LDAPS 설정

LDAP over TLS(LDAPS)를 사용하려면 먼저 LDAP 서버 인증서를 JVM 트러스트스토어에 등록합니다.

```bash
# 인증서 임포트
keytool -import -trustcacerts -keystore /Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home/jre/lib/security/cacerts \
  -storepass mypassword -alias myAlias -file /etc/ssl/certs/my-certificate.cer

# 루트 CA 인증서 임포트
keytool -importcert -keystore /Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home/jre/lib/security/cacerts \
  -storepass mypassword -alias myAlias -file /etc/ssl/certs/my-certificate.cer
```

`common.runtime.properties`에 트러스트스토어 설정을 추가합니다.

```properties
druid.auth.basic.ssl.trustStorePath=/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home/jre/lib/security/cacerts
druid.auth.basic.ssl.protocol=TLS
druid.auth.basic.ssl.trustStorePassword=xxxxxx
```

LDAP URL도 LDAPS로 변경합니다.

```properties
druid.auth.authenticator.ldap.credentialsValidator.url=ldaps://ip_address:636
```

### 트러블슈팅

- **연결 실패**: Coordinator 로그를 확인하고, 방화벽 규칙과 포트 접근 가능 여부를 점검합니다.
- **클러스터 동작 이상**: 서비스 간 escalator 설정이 일치하는지 확인합니다.
- **웹 콘솔 로그인은 되는데 401 오류 발생**: 사용자가 많은 대규모 환경에서는 LDAP 서버 응답 시간을 확인합니다.

---

## TLS 지원

### 일반 설정

| 속성 | 설명 | 기본값 |
| --- | --- | --- |
| `druid.enablePlaintextPort` | HTTP 커넥터 활성화/비활성화 | `true` |
| `druid.enableTlsPort` | HTTPS 커넥터 활성화/비활성화 | `false` |

두 커넥터를 동시에 활성화할 수 있지만 권장하지 않습니다. 포트 값은 `druid.plaintextPort`와 `druid.tlsPort`로 변경합니다.

### 인증서 준비

`keytool`로 키스토어와 트러스트스토어를 생성하는 예시입니다.

```bash
keytool -keystore keystore.jks -alias druid -genkey -keyalg RSA
keytool -export -alias druid -keystore keystore.jks -rfc -file public.cert
keytool -import -file public.cert -alias druid -keystore truststore.jks
```

프로덕션 환경에서는 자체 서명(self-signed) 인증서를 사용하면 안 됩니다.

### Jetty 서버 TLS 설정

**핵심 키스토어 설정**

| 속성 | 설명 | 기본값 | 필수 |
| --- | --- | --- | --- |
| `druid.server.https.keyStorePath` | TLS/SSL 키스토어의 파일 경로 또는 URL | 없음 | 예 |
| `druid.server.https.keyStoreType` | 키스토어 타입 | 없음 | 예 |
| `druid.server.https.certAlias` | 커넥터에 사용할 TLS/SSL 인증서의 alias | 없음 | 예 |
| `druid.server.https.keyStorePassword` | 키스토어 패스워드 (Password Provider 또는 문자열) | 없음 | 예 |
| `druid.server.https.reloadSslContext` | 키스토어 파일 변경을 감지해 다시 로드할지 여부 | `false` | 아니요 |
| `druid.server.https.reloadSslContextSeconds` | 키스토어 변경 스캔 주기 (초) | `60` | 예 |
| `druid.server.https.forceApplyConfig` | 기존 SslContextFactory가 있어도 TLS 설정을 강제 적용 | `false` | 아니요 |

**클라이언트 인증서 인증**

| 속성 | 설명 | 기본값 | 필수 |
| --- | --- | --- | --- |
| `druid.server.https.requireClientCertificate` | 클라이언트의 TLS 인증서 제시를 필수로 요구 | `false` | 아니요 |
| `druid.server.https.requestClientCertificate` | true이면 클라이언트가 선택적으로 TLS 인증서를 제시할 수 있음 | `false` | 아니요 |
| `druid.server.https.trustStoreType` | 클라이언트 인증서 검증용 트러스트스토어 타입 | `java.security.KeyStore.getDefaultType()` | 아니요 |
| `druid.server.https.trustStorePath` | 클라이언트 인증서 검증에 사용할 인증서를 담은 트러스트스토어의 파일 경로 또는 URL | 없음 | requireClientCertificate=true이면 예 |
| `druid.server.https.trustStoreAlgorithm` | 인증서 체인 검증용 TrustManager 알고리즘 | `javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm()` | 아니요 |
| `druid.server.https.trustStorePassword` | 트러스트스토어 패스워드 (Password Provider 또는 문자열) | 없음 | 아니요 |
| `druid.server.https.validateHostnames` | true이면 클라이언트 호스트명이 클라이언트 인증서의 CN/subjectAltNames와 일치하는지 검사 | `true` | 아니요 |
| `druid.server.https.crlPath` | 정적 인증서 폐기 목록(CRL) 파일 경로 | null | 아니요 |

**고급 설정**

| 속성 | 설명 | 기본값 | 필수 |
| --- | --- | --- | --- |
| `druid.server.https.keyManagerFactoryAlgorithm` | KeyManager 생성 알고리즘 | `javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm()` | 아니요 |
| `druid.server.https.keyManagerPassword` | 키 매니저 패스워드 (Password Provider 또는 문자열) | 없음 | 아니요 |
| `druid.server.https.includeCipherSuites` | 포함할 cipher suite 목록 (정확한 이름 또는 정규식) | Jetty 기본값 | 아니요 |
| `druid.server.https.excludeCipherSuites` | 제외할 cipher suite 목록 (정확한 이름 또는 정규식) | Jetty 기본값 | 아니요 |
| `druid.server.https.includeProtocols` | 포함할 프로토콜 이름 목록 | Jetty 기본값 | 아니요 |
| `druid.server.https.excludeProtocols` | 제외할 프로토콜 이름 목록 | Jetty 기본값 | 아니요 |

### 내부 통신 TLS

Druid 프로세스는 프로세스 간 통신에서 HTTPS를 우선 사용합니다. 내부 HttpClient가 서버 인증서를 검증하려면 적절한 SSLContext 설정이 필요합니다. Druid는 익스텐션을 통해 SSLContext의 Guice 바인딩을 찾으며, 일반적인 요구 사항에는 `simple-client-sslcontext` 익스텐션이면 충분합니다.

`common.runtime.properties`에 클라이언트 측 TLS를 설정하는 예시입니다.

```properties
druid.enableTlsPort=true
druid.enablePlaintextPort=false
druid.extensions.loadList=[......., "simple-client-sslcontext"]
druid.client.https.protocol=TLSv1.2
druid.client.https.trustStoreType=jks
druid.client.https.trustStorePath=truststore.jks
druid.client.https.trustStorePassword=secret123
druid.server.https.keyStoreType=jks
druid.server.https.keyStorePath=my-keystore.jks
druid.server.https.keyStorePassword=secret123
druid.server.https.certAlias=druid
```

Coordinator/Overlord 프로세스에서 HTTP와 HTTPS를 모두 활성화하면, 리더가 아닌 노드로 온 요청은 HTTPS로 리다이렉트됩니다. 무중단 업그레이드 순서는 다음과 같습니다.

1. 클라이언트의 HTTPS 처리를 먼저 활성화합니다.
2. Coordinator/Overlord에서 두 포트를 모두 활성화합니다.
3. 클라이언트 설정을 HTTPS 엔드포인트로 전환합니다.
4. HTTP 포트를 비활성화합니다.

### 커스텀 인증서 검사

| 속성 | 설명 | 기본값 | 필수 |
| --- | --- | --- | --- |
| `druid.tls.certificateChecker` | 익스텐션이 제공하는 커스텀 TLS 인증서 검사기의 타입 이름 | `"default"` | 아니요 |

기본 검사기는 표준 트러스트 매니저에 위임하며 추가 검증을 수행하지 않습니다.

---

## 패스워드 프로바이더

패스워드는 메타데이터 저장소, 서버 인증서를 담은 키스토어 등 Druid 시스템을 보호하는 데 쓰이며, `druid.metadata.storage.connector.password` 같은 런타임 속성으로 설정합니다.

### 기본 프로바이더

기본적으로는 런타임 속성에 패스워드를 평문으로 직접 적을 수 있습니다.

### 환경 변수 패스워드 프로바이더

설정 파일에 패스워드가 노출되지 않도록 환경 변수에서 읽어오게 할 수 있습니다.

```properties
druid.metadata.storage.connector.password={ "type": "environment", "variable": "METADATA_STORAGE_PASSWORD" }
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `type` | String | `"environment"` |
| `variable` | String | 읽어올 환경 변수 이름 |

### 커스텀 구현

더 세밀한 제어가 필요하면 `PasswordProvider` 인터페이스를 구현하는 커스텀 익스텐션을 만들어 런타임에 패스워드를 안전하게 가져올 수 있습니다. Druid 시작 시 익스텐션이 등록되어야 하며, 설정은 다음 형태를 따릅니다.

```properties
druid.metadata.storage.connector.password={ "type": "<registered_password_provider_name>", "<jackson_property>": "<value>", ... }
```

구현 방법은 Druid 개발 문서의 "Adding a new Password Provider implementation" 섹션을 참고합니다.

---

## 다이내믹 설정 프로바이더

다이내믹 설정 프로바이더(dynamic config provider)는 Druid 익스텐션 내부에서 서로 연관된 여러 자격 증명, 시크릿, 설정 묶음을 공급하는 메커니즘입니다. 민감한 정보를 평문 설정 밖에서 관리하여 보안을 강화합니다.

패스워드 프로바이더가 값 하나를 다루는 것과 달리 여러 값을 한꺼번에 다룰 수 있으며, 장기적으로 `PasswordProvider`를 대체할 예정입니다.

### 환경 변수 다이내믹 설정 프로바이더

내장 구현으로, 민감한 값을 시스템 환경 변수에 저장해 두고 참조합니다.

```json
{
  "type": "environment",
  "variables": {
    "secret1": "SECRET1_VAR",
    "secret2": "SECRET2_VAR"
  }
}
```

유의 사항은 다음과 같습니다.

- 수동 설정과 다이내믹 설정 프로바이더가 같은 키를 지정하면 다이내믹 설정 프로바이더의 값이 우선합니다.
- Supervisor 명세(spec)에서 사용할 때는 Overlord와 Peon 서비스를 실행하는 사용자 모두 해당 환경 변수에 접근할 수 있어야 합니다.

### Kafka consumer properties 예시

먼저 환경 변수를 설정합니다.

```bash
export SSL_KEY_PASSWORD=mysecretkeypassword
export SSL_KEYSTORE_PASSWORD=mysecretkeystorepassword
export SSL_TRUSTSTORE_PASSWORD=mysecrettruststorepassword
```

`consumerProperties`에서 다이내믹 설정 프로바이더로 패스워드를 주입합니다.

```json
{
  "consumerProperties": {
    "bootstrap.servers": "localhost:9092",
    "ssl.keystore.location": "/opt/kafka/config/kafka01.keystore.jks",
    "ssl.truststore.location": "/opt/kafka/config/kafka.truststore.jks",
    "druid.dynamic.config.provider": {
      "type": "environment",
      "variables": {
        "ssl.key.password": "SSL_KEY_PASSWORD",
        "ssl.keystore.password": "SSL_KEYSTORE_PASSWORD",
        "ssl.truststore.password": "SSL_TRUSTSTORE_PASSWORD"
      }
    }
  }
}
```

---

## 참고 자료

- [Security overview](https://druid.apache.org/docs/latest/operations/security-overview)
- [User authentication and authorization](https://druid.apache.org/docs/latest/operations/security-user-auth)
- [LDAP auth](https://druid.apache.org/docs/latest/operations/auth-ldap)
- [TLS support](https://druid.apache.org/docs/latest/operations/tls-support)
- [Password providers](https://druid.apache.org/docs/latest/operations/password-provider)
- [Dynamic Config Providers](https://druid.apache.org/docs/latest/operations/dynamic-config-provider)

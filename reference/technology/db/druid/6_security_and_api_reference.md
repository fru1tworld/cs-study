# Druid 보안과 API 레퍼런스

## Druid 보안

> 원본: https://druid.apache.org/docs/latest/operations/security-overview
> 원본: https://druid.apache.org/docs/latest/operations/security-user-auth
> 원본: https://druid.apache.org/docs/latest/operations/auth-ldap
> 원본: https://druid.apache.org/docs/latest/operations/tls-support
> 원본: https://druid.apache.org/docs/latest/operations/password-provider
> 원본: https://druid.apache.org/docs/latest/operations/dynamic-config-provider

Druid 클러스터의 보안 모범 사례, 인증(authentication)과 인가(authorization) 모델, LDAP 연동, TLS 설정, 패스워드 프로바이더와 다이내믹 설정 프로바이더를 차례로 설명함.

---

### 목차

1. [보안 개요](#보안-개요)
2. [인증과 인가 모델](#인증과-인가-모델)
3. [Basic 보안 익스텐션 설정](#basic-보안-익스텐션-설정)
4. [LDAP 인증](#ldap-인증)
5. [TLS 지원](#tls-지원)
6. [패스워드 프로바이더](#패스워드-프로바이더)
7. [다이내믹 설정 프로바이더](#다이내믹-설정-프로바이더)
8. [참고 자료](#참고-자료)

---

### 보안 개요

Druid는 배포를 단순하게 하기 위해 기본적으로 보안 기능을 모두 비활성화한 상태로 출시됨 → 프로덕션 환경에서는 TLS·인증·인가를 반드시 활성화 필요.

#### 클러스터 구성 모범 사례

사용자와 프로세스 관리

- Druid를 권한 없는(unprivileged) Unix 사용자로 실행. root 사용자로 실행 금지
- Druid 관리자는 Druid 프로세스를 실행하는 OS 계정의 권한을 그대로 상속 → root로 실행하면 파일 시스템 접근 관련 취약점 발생 가능

접근 제어

- 프로덕션 환경과 신뢰할 수 없는 네트워크에서는 인증 활성화 필요
- 웹 콘솔을 노출하기 전에 인가를 먼저 활성화 필요
- 사용자 권한 부여 시 최소 권한 원칙(principle of least privilege) 적용
- `consumerProperties` 같은 설정 파일에 평문(plain-text) 패스워드 저장 금지

코드 보안

- JavaScript 기능은 보안 가이드에 따라 비활성화 필요

#### 네트워크 보안 권장 사항

- 클러스터 내부 통신과 외부 통신 모두에 TLS 암호화 활성화 필요
- API 게이트웨이를 두어 접근 제한 · 허용 목록(allowlist) 구성 · 요청량 제한(throttling) 적용
- 방화벽 규칙으로 노출 서비스를 특정 포트와 IP 대역으로 제한
- Broker 포트는 인가된 다운스트림 쿼리 애플리케이션만 접근하도록 제한

#### 인가 모델 모범 사례

권한은 신뢰 수준에 따라 차등 부여.

- `DATASOURCE`에 대한 `WRITE` 권한: 사실상 Druid 프로세스 수준의 권한을 얻게 되므로 매우 신뢰하는 사용자에게만 부여
- `STATE READ`, `STATE WRITE`, `CONFIG WRITE`, `DATASOURCE WRITE`: 시스템 수준 접근이 필요한, 극히 신뢰하는 사용자로 제한
- 입력 소스 검증: 클라이언트 애플리케이션은 사용자가 제어하는 인제스천(ingestion) 태스크 URL을 검증 → 로컬 파일 시스템이나 내부 네트워크 자원 악용 방지 필요

#### 보안 신뢰 모델(Trust Model)

사용자 권한 계층

- 리소스 WRITE 권한 보유 사용자: Druid 프로세스 자체와 동등한 수준의 접근 권한 보유
- 인증된 읽기 전용 사용자: 인가된 데이터소스에 대한 쿼리만 가능
- 권한 없는 사용자: 어떤 리소스에도 의존하지 않는 쿼리만 실행 가능

프로세스 수준 신뢰 가정

- Druid 프로세스는 실행 Unix 사용자의 파일 권한을 상속 · 인제스천 태스크 워커도 부모 프로세스의 자격 증명을 상속 → 태스크 제출 권한을 부여하면 암묵적으로 파일 시스템 접근 권한도 부여하는 셈
- Druid는 격리되고 보호된 네트워크에서 내부 트래픽이 암호화된 상태로 동작한다고 가정
- 메타데이터 저장소와 ZooKeeper 노드가 안전하다고 가정 → 방화벽과 네트워크 통제는 운영자 책임

저장소와 클라이언트 신뢰 경계

- 딥 스토리지(deep storage): 해당 스토리지 시스템 고유의 보안 정책을 따름 · 파일 암호화도 스토리지 시스템의 기능에 위임
- 클라이언트 통신: Authenticator가 사용자를 검증하고 Authorizer가 권한을 집행

#### 보안 취약점 신고

취약점은 `security@apache.org`로 신고. 이 채널은 비공개 메일링 리스트이며, 취약점 하나당 평문 이메일 한 통으로 신고.

처리 절차는 다음과 같음.

1. 비공개로 취약점 제출
2. 신고 접수 확인
3. 비공개 협업으로 수정
4. 패치 릴리스 배포
5. 수정 안내와 함께 취약점 공개 발표

---

### 인증과 인가 모델

Druid 보안 모델의 핵심은 리소스(사용자가 접근하는 대상)와 액션(사용자가 수행하는 작업)임. 요청은 Authenticator의 인증 검사를 통과한 뒤 Authorizer의 인가 평가를 받음.

#### 리소스 타입

- `DATASOURCE`: SQL로 접근할 수 있는 개별 Druid 테이블
- `CONFIG`: 클러스터 구성 요소의 설정 엔드포인트
- `EXTERNAL`: SQL의 EXTERN 함수로 접근하는 외부 데이터
- `STATE`: 클러스터 전체 운영 상태 엔드포인트
- `SYSTEM_TABLE`: 시스템 스키마 테이블(`druid.sql.planner.authorizeSystemTablesDirectly` 활성화 시)

#### 액션

- `READ`: 읽기 전용 작업(주로 GET 요청)
- `WRITE`: 변경 작업(POST/DELETE 요청)

주의할 점으로, 리소스에 대한 `WRITE` 권한이 `READ` 권한을 포함하지 않음 → 둘 다 필요하면 각각 명시적으로 부여 필요.

#### 리소스 타입별 권한 상세

- `DATASOURCE`: 리소스 이름이 실제 데이터소스 이름과 대응 → 데이터소스 단위로 세밀한 접근 제어 가능
- `CONFIG`: 리소스 이름이 두 가지
  - `"CONFIG"`: Coordinator, Overlord, MiddleManager 프로세스의 워커 관리·설정 엔드포인트
  - `"security"`: Coordinator의 `/druid-ext/basic-security/authentication`, `/druid-ext/basic-security/authorization` 엔드포인트
- `EXTERNAL`: 리소스 이름은 `"EXTERNAL"`만 허용. EXTERN 함수를 포함한 쿼리 실행 권한을 부여
- `STATE`: 리소스 이름은 `"STATE"` 하나이며, Coordinator 운영 엔드포인트(`/druid/coordinator/v1/*`), Broker 상태 라우트, Overlord 리더 엔드포인트, 워커 태스크 관리, Historical 세그먼트 정보 등에 대한 접근을 제어
- `SYSTEM_TABLE`: 리소스 이름이 실제 시스템 스키마 테이블 이름(예: `sys.segments`, `sys.server_segments`)과 대응

#### SQL 쿼리 인가

- 데이터소스 쿼리에는 해당 데이터소스에 대한 `DATASOURCE READ` 권한 필요
- EXTERN 함수를 사용하려면 `EXTERNAL READ` 권한 필요
- INFORMATION_SCHEMA 쿼리는 접근 가능한 데이터소스만 반환
- 시스템 스키마 테이블 접근에는 해당 `READ` 권한 필요 · `druid.sql.planner.authorizeSystemTablesDirectly`가 활성화된 경우 `SYSTEM_TABLE` 인가도 필요

`DATASOURCE`에 대한 `WRITE` 권한은 파일 시스템이나 S3 버킷 같은 민감한 인프라에 대한 광범위한 접근을 허용 → 관리자 수준 사용자에게만 선별적으로 부여 필요.

#### 기본 사용자 계정

Authenticator 측

- admin 사용자: `druid.auth.authenticator.<name>.initialAdminPassword`를 설정하면 생성
- druid_system 사용자: `druid.auth.authenticator.<name>.initialInternalClientPassword`를 설정하면 생성

Authorizer 측

모든 Authorizer에는 전체 권한을 가진 `admin`과 `druid_system` 계정이 자동으로 포함됨.

#### 설정 전파

Authenticator와 Authorizer의 메타데이터는 각 프로세스에 캐시되며, 주기적인 폴링으로 갱신됨. 폴링 주기는 `druid.auth.basic.common.pollingPeriod`와 `druid.auth.basic.common.maxRandomDelay`로 제어. `enableCacheNotifications` 설정으로 푸시 알림을 켜면 전파 지연 감소 가능.

---

### Basic 보안 익스텐션 설정

`druid-basic-security` 익스텐션을 사용하면 메타데이터 저장소 기반의 사용자/패스워드 인증과 역할(role) 기반 인가 구성 가능.

#### common.runtime.properties 설정

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

#### API로 사용자와 역할 관리

사용자 관리 API는 Coordinator 엔드포인트에서 제공(비 TLS 포트 8081, TLS 포트 8281).

사용자 생성(Authenticator)

```
POST /druid-ext/basic-security/authentication/db/MyBasicMetadataAuthenticator/users/<USERNAME>
```

자격 증명 설정

```
POST /druid-ext/basic-security/authentication/db/MyBasicMetadataAuthenticator/users/<USERNAME>/credentials
```

```json
{"password": "my_password"}
```

사용자 생성(Authorizer)

```
POST /druid-ext/basic-security/authorization/db/MyBasicMetadataAuthorizer/users/<USERNAME>
```

역할 생성

```
POST /druid-ext/basic-security/authorization/db/MyBasicMetadataAuthorizer/roles/<ROLENAME>
```

사용자에게 역할 부여

```
POST /druid-ext/basic-security/authorization/db/MyBasicMetadataAuthorizer/users/<USERNAME>/roles/<ROLENAME>
```

역할에 권한 부여

```
POST /druid-ext/basic-security/authorization/db/MyBasicMetadataAuthorizer/roles/<ROLENAME>/permissions
```

권한 페이로드 구조는 다음과 같음.

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

리소스 이름은 정규 표현식으로 동작 → 하나의 권한으로 여러 데이터소스에 대한 접근 부여 가능.

---

### LDAP 인증

`druid-basic-security` 익스텐션의 credentials validator를 `ldap`으로 지정하면 LDAP 디렉터리로 사용자 인증 가능.

#### 사전 확인

설정 전에 LDAP 서버 연결을 먼저 검증.

```bash
ldapwhoami -vv -H ldap://ip_address:389 -D "myuser@example.com" -W
```

디렉터리에서 사용자를 검색할 수 있는지도 확인.

```bash
ldapsearch -x -W -H ldap://ip_address:389 -D "cn=admin,dc=example,dc=com" \
  -b "dc=example,dc=com" "(sAMAccountName=myuser)" +
```

검색 결과의 `memberOf` 속성이 그룹 멤버십을 나타내며, Druid는 이 속성으로 사용자의 소속 그룹을 판별.

#### 기본 설정

`druid-basic-security` 익스텐션을 활성화하고 `common.runtime.properties`에 다음을 추가.

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

#### 역할과 권한 관리

Druid 역할 생성

```bash
curl -i -v -H "Content-Type: application/json" -u internal -X POST \
  http://localhost:8081/druid-ext/basic-security/authorization/db/ldapauth/roles/readRole
```

역할 목록 조회

```bash
curl -i -v -H "Content-Type: application/json" -u internal -X GET \
  http://localhost:8081/druid-ext/basic-security/authorization/db/ldapauth/roles
```

권한 정의 파일 작성(perm.json)

```json
[
  { "resource": { "name": "wikipedia", "type": "DATASOURCE" }, "action": "READ" },
  { "resource": { "name": ".*", "type": "STATE" }, "action": "READ" },
  { "resource": { "name": ".*", "type": "CONFIG" }, "action": "READ" }
]
```

역할에 권한 부여

```bash
curl -i -v -H "Content-Type: application/json" -u internal -X POST \
  -d@perm.json http://localhost:8081/druid-ext/basic-security/authorization/db/ldapauth/roles/readRole/permissions
```

#### LDAP 그룹을 역할에 매핑

그룹 매핑 파일 작성(groupmap.json)

```json
{
  "name": "mygroupmap",
  "groupPattern": "CN=mygroup,CN=Users,DC=example,DC=com",
  "roles": ["readRole"]
}
```

그룹 매핑 생성

```bash
curl -i -v -H "Content-Type: application/json" -u internal -X POST \
  -d @groupmap.json http://localhost:8081/druid-ext/basic-security/authorization/db/ldapauth/groupMappings/mygroupmap
```

그룹 매핑 조회

```bash
# 전체 매핑 조회
curl -i -v -H "Content-Type: application/json" -u internal -X GET \
  http://localhost:8081/druid-ext/basic-security/authorization/db/ldapauth/groupMappings

# 특정 매핑 조회
curl -i -v -H "Content-Type: application/json" -u internal -X GET \
  http://localhost:8081/druid-ext/basic-security/authorization/db/ldapauth/groupMappings/mygroupmap
```

기존 매핑에 역할 추가

```bash
curl -i -v -H "Content-Type: application/json" -u internal -X POST \
  http://localhost:8081/druid-ext/basic-security/authorization/db/ldapauth/groupMappings/mygroup/roles/queryrole
```

#### 개별 LDAP 사용자에게 역할 부여

그룹 매핑 대신 개별 사용자 단위로도 역할 부여 가능.

```bash
# LDAP 사용자 추가
curl -i -v -H "Content-Type: application/json" -u internal -X POST \
  http://localhost:8081/druid-ext/basic-security/authorization/db/ldapauth/users/myuser

# 역할 부여
curl -i -v -H "Content-Type: application/json" -u internal -X POST \
  http://localhost:8081/druid-ext/basic-security/authorization/db/ldapauth/users/myuser/roles/queryRole
```

#### LDAPS 설정

LDAP over TLS(LDAPS)를 사용하려면 먼저 LDAP 서버 인증서를 JVM 트러스트스토어에 등록 필요.

```bash
# 인증서 임포트
keytool -import -trustcacerts -keystore /Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home/jre/lib/security/cacerts \
  -storepass mypassword -alias myAlias -file /etc/ssl/certs/my-certificate.cer

# 루트 CA 인증서 임포트
keytool -importcert -keystore /Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home/jre/lib/security/cacerts \
  -storepass mypassword -alias myAlias -file /etc/ssl/certs/my-certificate.cer
```

`common.runtime.properties`에 트러스트스토어 설정을 추가.

```properties
druid.auth.basic.ssl.trustStorePath=/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home/jre/lib/security/cacerts
druid.auth.basic.ssl.protocol=TLS
druid.auth.basic.ssl.trustStorePassword=xxxxxx
```

LDAP URL도 LDAPS로 변경.

```properties
druid.auth.authenticator.ldap.credentialsValidator.url=ldaps://ip_address:636
```

#### 트러블슈팅

- 연결 실패: Coordinator 로그를 확인하고, 방화벽 규칙과 포트 접근 가능 여부를 점검
- 클러스터 동작 이상: 서비스 간 escalator 설정이 일치하는지 확인
- 웹 콘솔 로그인은 되는데 401 오류 발생: 사용자가 많은 대규모 환경에서는 LDAP 서버 응답 시간 확인 필요

---

### TLS 지원

#### 일반 설정

- `druid.enablePlaintextPort`: HTTP 커넥터 활성화/비활성화, 기본값 `true`
- `druid.enableTlsPort`: HTTPS 커넥터 활성화/비활성화, 기본값 `false`

두 커넥터를 동시에 활성화할 수 있으나 권장하지 않음. 포트 값은 `druid.plaintextPort`와 `druid.tlsPort`로 변경.

#### 인증서 준비

`keytool`로 키스토어와 트러스트스토어를 생성하는 예시.

```bash
keytool -keystore keystore.jks -alias druid -genkey -keyalg RSA
keytool -export -alias druid -keystore keystore.jks -rfc -file public.cert
keytool -import -file public.cert -alias druid -keystore truststore.jks
```

프로덕션 환경에서는 자체 서명(self-signed) 인증서 사용 금지.

#### Jetty 서버 TLS 설정

핵심 키스토어 설정

- `druid.server.https.keyStorePath`: TLS/SSL 키스토어의 파일 경로 또는 URL, 기본값 없음, 필수
- `druid.server.https.keyStoreType`: 키스토어 타입, 기본값 없음, 필수
- `druid.server.https.certAlias`: 커넥터에 사용할 TLS/SSL 인증서의 alias, 기본값 없음, 필수
- `druid.server.https.keyStorePassword`: 키스토어 패스워드(Password Provider 또는 문자열), 기본값 없음, 필수
- `druid.server.https.reloadSslContext`: 키스토어 파일 변경을 감지해 다시 로드할지 여부, 기본값 `false`, 선택
- `druid.server.https.reloadSslContextSeconds`: 키스토어 변경 스캔 주기(초), 기본값 `60`, 필수
- `druid.server.https.forceApplyConfig`: 기존 SslContextFactory가 있어도 TLS 설정을 강제 적용, 기본값 `false`, 선택

클라이언트 인증서 인증

- `druid.server.https.requireClientCertificate`: 클라이언트의 TLS 인증서 제시를 필수로 요구, 기본값 `false`, 선택
- `druid.server.https.requestClientCertificate`: true이면 클라이언트가 선택적으로 TLS 인증서를 제시 가능, 기본값 `false`, 선택
- `druid.server.https.trustStoreType`: 클라이언트 인증서 검증용 트러스트스토어 타입, 기본값 `java.security.KeyStore.getDefaultType()`, 선택
- `druid.server.https.trustStorePath`: 클라이언트 인증서 검증에 사용할 인증서를 담은 트러스트스토어의 파일 경로 또는 URL, 기본값 없음, requireClientCertificate=true이면 필수
- `druid.server.https.trustStoreAlgorithm`: 인증서 체인 검증용 TrustManager 알고리즘, 기본값 `javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm()`, 선택
- `druid.server.https.trustStorePassword`: 트러스트스토어 패스워드(Password Provider 또는 문자열), 기본값 없음, 선택
- `druid.server.https.validateHostnames`: true이면 클라이언트 호스트명이 클라이언트 인증서의 CN/subjectAltNames와 일치하는지 검사, 기본값 `true`, 선택
- `druid.server.https.crlPath`: 정적 인증서 폐기 목록(CRL) 파일 경로, 기본값 null, 선택

고급 설정

- `druid.server.https.keyManagerFactoryAlgorithm`: KeyManager 생성 알고리즘, 기본값 `javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm()`, 선택
- `druid.server.https.keyManagerPassword`: 키 매니저 패스워드(Password Provider 또는 문자열), 기본값 없음, 선택
- `druid.server.https.includeCipherSuites`: 포함할 cipher suite 목록(정확한 이름 또는 정규식), 기본값 Jetty 기본값, 선택
- `druid.server.https.excludeCipherSuites`: 제외할 cipher suite 목록(정확한 이름 또는 정규식), 기본값 Jetty 기본값, 선택
- `druid.server.https.includeProtocols`: 포함할 프로토콜 이름 목록, 기본값 Jetty 기본값, 선택
- `druid.server.https.excludeProtocols`: 제외할 프로토콜 이름 목록, 기본값 Jetty 기본값, 선택

#### 내부 통신 TLS

Druid 프로세스는 프로세스 간 통신에서 HTTPS를 우선 사용. 내부 HttpClient가 서버 인증서를 검증하려면 적절한 SSLContext 설정 필요. Druid는 익스텐션을 통해 SSLContext의 Guice 바인딩을 찾으며, 일반적인 요구 사항에는 `simple-client-sslcontext` 익스텐션이면 충분.

`common.runtime.properties`에 클라이언트 측 TLS를 설정하는 예시.

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

Coordinator/Overlord 프로세스에서 HTTP와 HTTPS를 모두 활성화하면, 리더가 아닌 노드로 온 요청은 HTTPS로 리다이렉트됨. 무중단 업그레이드 순서는 다음과 같음.

1. 클라이언트의 HTTPS 처리를 먼저 활성화
2. Coordinator/Overlord에서 두 포트를 모두 활성화
3. 클라이언트 설정을 HTTPS 엔드포인트로 전환
4. HTTP 포트를 비활성화

#### 커스텀 인증서 검사

- `druid.tls.certificateChecker`: 익스텐션이 제공하는 커스텀 TLS 인증서 검사기의 타입 이름, 기본값 `"default"`, 선택

기본 검사기는 표준 트러스트 매니저에 위임하며 추가 검증을 수행하지 않음.

---

### 패스워드 프로바이더

패스워드는 메타데이터 저장소, 서버 인증서를 담은 키스토어 등 Druid 시스템을 보호하는 데 쓰이며, `druid.metadata.storage.connector.password` 같은 런타임 속성으로 설정.

#### 기본 프로바이더

기본적으로는 런타임 속성에 패스워드를 평문으로 직접 기재 가능.

#### 환경 변수 패스워드 프로바이더

설정 파일에 패스워드가 노출되지 않도록 환경 변수에서 읽어오게 하는 방식.

```properties
druid.metadata.storage.connector.password={ "type": "environment", "variable": "METADATA_STORAGE_PASSWORD" }
```

- `type`(String): `"environment"`
- `variable`(String): 읽어올 환경 변수 이름

#### 커스텀 구현

더 세밀한 제어가 필요하면 `PasswordProvider` 인터페이스를 구현하는 커스텀 익스텐션을 만들어 런타임에 패스워드를 안전하게 가져올 수 있음. Druid 시작 시 익스텐션이 등록되어야 하며, 설정은 다음 형태를 따름.

```properties
druid.metadata.storage.connector.password={ "type": "<registered_password_provider_name>", "<jackson_property>": "<value>", ... }
```

구현 방법은 Druid 개발 문서의 "Adding a new Password Provider implementation" 섹션 참고.

---

### 다이내믹 설정 프로바이더

다이내믹 설정 프로바이더(dynamic config provider)는 Druid 익스텐션 내부에서 서로 연관된 여러 자격 증명, 시크릿, 설정 묶음을 공급하는 메커니즘. 민감한 정보를 평문 설정 밖에서 관리해 보안을 강화함.

패스워드 프로바이더가 값 하나를 다루는 것과 달리 여러 값을 한꺼번에 다룰 수 있으며, 장기적으로 `PasswordProvider`를 대체할 예정.

#### 환경 변수 다이내믹 설정 프로바이더

내장 구현으로, 민감한 값을 시스템 환경 변수에 저장해 두고 참조.

```json
{
  "type": "environment",
  "variables": {
    "secret1": "SECRET1_VAR",
    "secret2": "SECRET2_VAR"
  }
}
```

유의 사항은 다음과 같음.

- 수동 설정과 다이내믹 설정 프로바이더가 같은 키를 지정하면 다이내믹 설정 프로바이더의 값이 우선
- Supervisor 명세(spec)에서 사용할 때는 Overlord와 Peon 서비스를 실행하는 사용자 모두 해당 환경 변수에 접근 가능해야 함

#### Kafka consumer properties 예시

먼저 환경 변수를 설정.

```bash
export SSL_KEY_PASSWORD=mysecretkeypassword
export SSL_KEYSTORE_PASSWORD=mysecretkeystorepassword
export SSL_TRUSTSTORE_PASSWORD=mysecrettruststorepassword
```

`consumerProperties`에서 다이내믹 설정 프로바이더로 패스워드를 주입.

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

### 참고 자료

- [Security overview](https://druid.apache.org/docs/latest/operations/security-overview)
- [User authentication and authorization](https://druid.apache.org/docs/latest/operations/security-user-auth)
- [LDAP auth](https://druid.apache.org/docs/latest/operations/auth-ldap)
- [TLS support](https://druid.apache.org/docs/latest/operations/tls-support)
- [Password providers](https://druid.apache.org/docs/latest/operations/password-provider)
- [Dynamic Config Providers](https://druid.apache.org/docs/latest/operations/dynamic-config-provider)

---

## Druid API 레퍼런스

> 원본: https://druid.apache.org/docs/latest/api-reference/
> 원본: https://druid.apache.org/docs/latest/api-reference/sql-api
> 원본: https://druid.apache.org/docs/latest/api-reference/sql-ingestion-api
> 원본: https://druid.apache.org/docs/latest/api-reference/tasks-api
> 원본: https://druid.apache.org/docs/latest/api-reference/supervisor-api
> 원본: https://druid.apache.org/docs/latest/api-reference/data-management-api
> 원본: https://druid.apache.org/docs/latest/api-reference/service-status-api

Druid가 제공하는 HTTP API 가운데 SQL 쿼리, SQL 기반 인제스천(MSQ), 태스크, 슈퍼바이저(supervisor), 데이터 관리, 서비스 상태 API의 주요 엔드포인트를 정리함.

---

### 목차

1. [API 레퍼런스 개요](#api-레퍼런스-개요)
2. [Druid SQL API](#druid-sql-api)
3. [SQL 인제스천 API](#sql-인제스천-api)
4. [Tasks API](#tasks-api)
5. [Supervisor API](#supervisor-api)
6. [데이터 관리 API](#데이터-관리-api)
7. [서비스 상태 API](#서비스-상태-api)
8. [참고 자료](#참고-자료)

---

### API 레퍼런스 개요

Druid는 클러스터의 각 기능을 HTTP API로 노출함. 공식 문서의 API 레퍼런스 섹션은 다음 범주로 구성됨.

- Druid SQL queries: Druid SQL API로 SQL 쿼리를 제출
- SQL-based ingestion: SQL 기반 배치 인제스천 요청을 제출
- JSON querying: JSON 기반 네이티브(native) 쿼리를 제출
- Tasks: 데이터 인제스천 작업(태스크)을 관리
- Supervisors: 스트리밍 인제스천의 생명주기를 담당하는 슈퍼바이저를 관리
- Retention rules: 데이터 보존(retention) 정책을 정의하고 관리
- Data management: 데이터 세그먼트(segment)를 관리
- Automatic compaction: 인제스천 이후 세그먼트 크기를 최적화
- Lookups: 키-값 데이터소스(lookup)를 관리
- Service status: 클러스터 구성 요소의 상태를 모니터링
- Dynamic configuration: Coordinator와 Overlord 프로세스의 동적 설정을 관리
- Legacy metadata: 데이터소스 메타데이터를 조회

Java API로는 Avatica JDBC 드라이버 기반의 SQL JDBC driver를 제공하며, 이 드라이버로 Druid에 접속해 SQL 쿼리 실행 가능.

예시 요청의 `ROUTER_IP:ROUTER_PORT`는 Router 서비스 주소(예: `localhost:8888`)로 바꿔서 사용.

---

### Druid SQL API

#### 쿼리 제출(Historical에 적재된 데이터)

```
POST /druid/v2/sql
```

적재된 데이터에 대해 SQL 쿼리를 실행. 요청 본문은 JSON 객체이며 다음 필드를 사용.

- `query`: 실행할 SQL 문. 문자열 안에 여러 개의 `SET` 문을 넣어 컨텍스트 파라미터 지정 가능
- `resultFormat`: 결과 포맷. `object`, `array`, `objectLines`, `arrayLines`, `csv` 중 하나
- `header`: `true`이면 첫 행에 컬럼 이름 반환
- `typesHeader`: `true`이면 Druid 런타임 타입 정보를 함께 반환. `header: true` 필요
- `sqlTypesHeader`: `true`이면 SQL 타입 정보를 함께 반환. `header: true` 필요
- `context`: 쿼리 컨텍스트 파라미터(`timeZone`, `queryId` 등)를 담는 JSON 객체
- `parameters`: 파라미터화 쿼리(parameterized query)에 사용할 값의 배열. 각 원소는 `type`과 `value`로 구성

결과 포맷별 특징은 다음과 같음.

- `object`: JSON 객체의 배열, `Content-Type`은 `application/json`
- `array`: JSON 배열의 배열, `Content-Type`은 `application/json`
- `objectLines`: 줄바꿈으로 구분한 JSON 객체(newline-delimited)
- `arrayLines`: 줄바꿈으로 구분한 JSON 배열
- `csv`: 쉼표로 구분한 값. 큰따옴표로 이스케이프

요청 예시는 다음과 같음.

```json
{
  "query": "SELECT COUNT(*) AS cnt FROM wikipedia WHERE __time >= CURRENT_TIMESTAMP - INTERVAL '1' DAY",
  "resultFormat": "object",
  "header": true,
  "typesHeader": true,
  "sqlTypesHeader": true,
  "context": {
    "sqlTimeZone": "America/Los_Angeles"
  }
}
```

JSON 대신 `Content-Type: text/plain` 또는 `application/x-www-form-urlencoded`로 본문에 SQL 문만 담아 보내는 것도 가능. 이 경우 결과는 `object` 포맷의 `application/json`으로 반환됨.

응답 상태 코드는 다음과 같음.

- `200 OK`: 쿼리 성공. 결과와 메타데이터를 반환
- `400 BAD REQUEST`: 잘못된 요청. 오류 상세를 반환
- `500 INTERNAL SERVER ERROR`: 서버 오류. 오류 객체를 반환

#### 오류 처리와 응답 잘림

응답 본문을 전송하는 도중 오류가 발생하면 서버는 응답을 중간에 끊음. `objectLines`, `arrayLines` 같은 줄 단위 포맷에서 마지막 줄바꿈이 없다면 응답이 잘린 것이므로 오류로 처리 필요. 정상적으로 완료된 응답은 항상 마지막 줄바꿈을 포함.

오류 객체는 `error`, `errorMessage`, `errorClass`, `host` 필드를 포함.

#### 쿼리 취소

```
DELETE /druid/v2/sql/{sqlQueryId}
```

실행 중인 쿼리를 취소. 성공하면 HTTP `202 ACCEPTED`를 반환. 취소는 최선 노력(best-effort) 방식이라 요청 이후에도 쿼리가 잠시 계속 실행될 수 있음. 취소하려면 쿼리가 접근하는 모든 리소스에 대한 READ 권한 필요.

#### 딥 스토리지 쿼리(Query from deep storage)

딥 스토리지(deep storage)에 저장된 세그먼트를 비동기(async)로 조회하는 엔드포인트.

쿼리 제출

```
POST /druid/v2/sql/statements
```

요청 본문은 `/druid/v2/sql`과 유사하며 다음 컨텍스트 파라미터를 추가로 사용.

- `context.executionMode`: 현재 `ASYNC`만 지원
- `context.selectDestination`: 대용량 결과(약 3,000행 초과)는 `durableStorage`를 사용

응답은 `queryId`, `state`, `createdAt`, `schema`, `durationMs` 필드를 담은 JSON 객체.

쿼리 상태 조회

```
GET /druid/v2/sql/statements/{queryId}
```

- `detail`(선택, boolean): 스테이지(stage), 카운터(counter), 경고(warning) 정보를 포함해 반환

완료된 쿼리의 응답에는 전체 행 수와 결과 페이지 목록(`pages` 배열)을 담은 `result` 객체가 포함됨.

결과 조회

```
GET /druid/v2/sql/statements/{queryId}/results
```

- `page`(선택, int): 조회할 페이지 번호. 생략하면 전체 결과를 순서대로 반환
- `resultFormat`(선택): `arrayLines`, `objectLines`, `array`, `object`, `csv` 중 하나
- `filename`(선택): `Content-Disposition` 헤더를 붙여 파일로 내려받게 함. 최대 255자이며 특수 문자 사용 불가

성공 시 HTTP `200`을 반환하고, 쿼리가 없거나 실패한 경우 `404`를 반환.

쿼리 취소

```
DELETE /druid/v2/sql/statements/{queryId}
```

성공적으로 취소하면 빈 본문과 함께 HTTP `202 ACCEPTED`를 반환. 딥 스토리지 쿼리의 오류 객체에는 `errorCode`, `persona`, `category`, `context` 필드가 추가로 포함됨.

---

### SQL 인제스천 API

MSQ(멀티 스테이지 쿼리) 태스크 엔진으로 SQL 기반 인제스천을 실행하는 API.

#### 쿼리(태스크) 제출

```
POST /druid/v2/sql/task
```

MSQ 태스크 엔진에 SQL 요청을 제출. `query`, `context`, `parameters` 필드를 사용하는 JSON-over-HTTP 형식이며, INSERT, REPLACE, SELECT 문을 지원.

주요 컨텍스트 파라미터는 다음과 같음.

- `maxNumTasks`: 병렬로 실행할 최대 태스크 수를 제한
- `finalizeAggregations`: 인제스천 시 집계(aggregation) 타입의 최종화 여부를 제어

컨텍스트 파라미터는 쿼리 문자열 안의 `SET` 문으로도 지정 가능.

```sql
SET maxNumTasks=3; SET finalizeAggregations=false;
INSERT INTO ...
```

제출 성공 시 응답은 다음과 같음.

```json
{
  "taskId": "query-f795a235-4dc7-4fef-abac-3ae3f9686b79",
  "state": "RUNNING"
}
```

#### 태스크 상태 조회

```
GET /druid/indexer/v1/task/{taskId}/status
```

`statusCode`(`RUNNING`, `SUCCESS`, `FAILED`)를 비롯한 태스크 메타데이터와 데이터소스 정보를 반환.

#### 태스크 리포트 조회

```
GET /druid/indexer/v1/task/{taskId}/reports
```

`multiStageQuery.payload` 아래에 실행 스테이지, 워커(worker) 상세, 카운터, 세그먼트 로딩 상태가 중첩된 구조로 담김. 리포트로 행 수·전송 바이트·병렬 워커별 처리 진행 상황 추적 가능.

#### 태스크 취소

```
POST /druid/indexer/v1/task/{taskId}/shutdown
```

실행 중인 MSQ 태스크를 취소. 응답은 다음과 같음.

```json
{
  "task": "query-655efe33-781a-4c50-ae84-c2911b42d63c"
}
```

---

### Tasks API

Overlord가 관리하는 인제스천 태스크의 조회·제출·종료 API.

#### 태스크 목록 조회

```
GET /druid/indexer/v1/tasks
```

모든 태스크를 조회하며, 다음 쿼리 파라미터로 필터링.

- `state`: 태스크 상태로 필터링. `running`, `complete`, `waiting`, `pending` 중 하나
- `datasource`: 데이터소스 이름으로 필터링
- `createdTimeInterval`: 생성 시각 기준 ISO-8601 인터벌. 구분자로 `_` 사용(예: `2023-06-27_2023-06-28`)
- `max`: 반환할 완료(complete) 태스크의 최대 개수
- `type`: 태스크 타입으로 필터링

응답 예시는 다음과 같음.

```json
[
  {
    "id": "query-223549f8-b993-4483-b028-1b0d54713cad",
    "type": "query_worker",
    "status": "SUCCESS",
    "dataSource": "wikipedia_api"
  }
]
```

상태별 전용 엔드포인트도 제공.

- `GET /druid/indexer/v1/completeTasks`: 완료된 태스크 목록. `?state=complete`와 동일하며 위 쿼리 파라미터를 지원
- `GET /druid/indexer/v1/runningTasks`: 실행 중인 태스크 목록
- `GET /druid/indexer/v1/waitingTasks`: 대기(waiting) 상태 태스크 목록
- `GET /druid/indexer/v1/pendingTasks`: 보류(pending) 상태 태스크 목록

#### 개별 태스크 조회

- `GET /druid/indexer/v1/task/{taskId}`: 태스크의 전체 설정(payload)과 스펙을 반환
- `GET /druid/indexer/v1/task/{taskId}/status`: 실행 시간(duration), 위치(location), 오류 정보 등 상태 메타데이터를 반환
- `GET /druid/indexer/v1/task/{taskId}/reports`: 인제스천 통계와 파싱 예외(parse exception)를 담은 완료 리포트를 반환
- `GET /druid/indexer/v1/task/{taskId}/log`: 태스크 실행 생명주기 동안의 이벤트 로그를 반환. `offset` 파라미터로 앞부분 항목 제외 가능

`GET /druid/indexer/v1/task/{taskId}/segments`는 더 이상 지원하지 않으며 `404`를 반환. 대신 `segment/added/bytes` 메트릭을 사용.

#### 여러 태스크 상태 일괄 조회

```
POST /druid/indexer/v1/taskStatus
```

태스크 ID 배열을 본문으로 받아 각 태스크의 상태 객체를 반환.

```json
["index_parallel_wikipedia_auto_jndhkpbo_2023-06-26T17:23:05.308Z"]
```

#### 태스크 제출

```
POST /druid/indexer/v1/task
```

JSON 인제스천 스펙을 제출하고 태스크 ID를 반환받음.

```json
{
  "type": "index_parallel",
  "spec": {
    "dataSchema": {
      "dataSource": "wikipedia_auto",
      "granularitySpec": {
        "intervals": ["2015-09-12/2015-09-13"]
      }
    },
    "ioConfig": { "type": "index_parallel" },
    "tuningConfig": { "type": "index_parallel" }
  }
}
```

응답 예시는 다음과 같음.

```json
{"task": "index_parallel_wikipedia_odofhkle_2023-06-23T21:07:28.226Z"}
```

#### 태스크 종료

- `POST /druid/indexer/v1/task/{taskId}/shutdown`: 지정한 태스크를 종료
- `POST /druid/indexer/v1/datasources/{datasource}/shutdownAllTasks`: 지정한 데이터소스와 연관된 모든 태스크를 종료

#### 보류 세그먼트 정리

```
DELETE /druid/indexer/v1/pendingSegments/{datasource}
```

메타데이터 스토리지에서 보류 중인(pending) 세그먼트를 수동으로 정리하고, 삭제한 개수(`numDeleted`)를 반환.

---

### Supervisor API

Kafka·Kinesis 같은 스트리밍 인제스천의 슈퍼바이저를 관리하는 API.

#### 슈퍼바이저 정보 조회

- `GET /druid/indexer/v1/supervisor`: 활성 슈퍼바이저 이름(ID)의 문자열 배열을 반환
- `GET /druid/indexer/v1/supervisor?full`: 전체 스펙을 포함한 활성 슈퍼바이저 객체 배열을 반환
- `GET /druid/indexer/v1/supervisor?state=true`: 각 슈퍼바이저의 `id`, `state`, `detailedState`, `healthy`, `suspended` 상태 객체 배열을 반환
- `GET /druid/indexer/v1/supervisor/{supervisorId}`: 단일 슈퍼바이저의 스펙(`dataSchema`, `ioConfig`, `tuningConfig` 포함)을 반환
- `GET /druid/indexer/v1/supervisor/{supervisorId}/status`: 태스크 상태와 최근 예외를 포함한 현재 상태 리포트를 반환
- `GET /druid/indexer/v1/supervisor/{supervisorId}/health`: 슈퍼바이저 상태와 Overlord 설정 임계값을 기준으로 건강 상태를 반환. 비정상이면 `503`을 반환
- `GET /druid/indexer/v1/supervisor/{supervisorId}/stats`: 태스크별 인제스천 행 카운터의 스냅샷과 이동 평균을 반환. 태스크 그룹 ID → 태스크 ID → 행 통계의 중첩 맵 구조

#### 감사 이력(audit history)

- `GET /druid/indexer/v1/supervisor/history`: 모든 슈퍼바이저의 스펙 감사 이력을 반환
- `GET /druid/indexer/v1/supervisor/{supervisorId}/history`: 단일 슈퍼바이저의 감사 이력을 반환. `count` 파라미터로 최근 n건만 조회 가능

#### 생성·수정

```
POST /druid/indexer/v1/supervisor
```

새 슈퍼바이저를 생성하거나 기존 스펙을 갱신. 본문에는 `type`(`kafka` 또는 `kinesis`), `spec`(그 안에 `dataSchema`, `ioConfig`, 선택적 `tuningConfig`)을 담음. `?skipRestartIfUnmodified=true`를 지정하면 스펙이 변경되지 않은 경우 재시작 생략.

#### 일시 중지와 재개

- `POST /druid/indexer/v1/supervisor/{supervisorId}/suspend`: 실행 중인 슈퍼바이저를 일시 중지. 재개할 때까지 태스크가 중지된 상태로 유지되며, 그동안에도 슈퍼바이저는 로그와 메트릭을 계속 내보냄
- `POST /druid/indexer/v1/supervisor/suspendAll`: 모든 슈퍼바이저를 일시 중지. 대상이 없어도 `200`을 반환
- `POST /druid/indexer/v1/supervisor/{supervisorId}/resume`: 일시 중지된 슈퍼바이저의 인덱싱 태스크를 재개
- `POST /druid/indexer/v1/supervisor/resumeAll`: 모든 슈퍼바이저를 재개. 대상이 없어도 `200`을 반환

#### 리셋(reset)

```
POST /druid/indexer/v1/supervisor/{supervisorId}/reset
```

슈퍼바이저가 실행 중일 때만 사용 가능. 슈퍼바이저 메타데이터(저장된 오프셋)를 모두 지우고, `useEarliestOffset` 설정에 따라 가장 이른 위치 또는 가장 최근 위치부터 데이터를 다시 읽게 함 → 저장된 오프셋을 지운 뒤 활성 태스크를 종료하고 재생성해 유효한 위치부터 읽기 시작. 오프셋 유실(메시지 보존 기간 만료, 토픽 삭제 후 재생성 등)로 슈퍼바이저가 멈춘 상태를 복구할 때 사용하며, 메시지를 건너뛰어 데이터 유실이나 중복이 생길 수 있으므로 주의해서 사용 필요.

#### 오프셋 리셋(reset offsets)

```
POST /druid/indexer/v1/supervisor/{supervisorId}/resetOffsets
```

전체를 리셋하지 않고 지정한 파티션의 오프셋만 리셋. 저장된 오프셋이 없으면 지정한 오프셋을 메타데이터 스토리지에 기록. 리셋 후에는 해당 파티션과 관련된 활성 태스크를 종료·재생성해 지정한 오프셋부터 읽기 시작하고, 지정하지 않은 파티션은 마지막 저장 오프셋부터 계속 읽음. 이 역시 메시지 누락으로 데이터 유실·중복이 생길 수 있음.

요청 본문(reset offsets metadata)의 구조는 다음과 같음.

- `type`(String, 필수): 페이로드 타입. 슈퍼바이저의 타입과 일치해야 하며 `kafka` 또는 `kinesis`
- `partitions`(Object, 필수): 리셋 메타데이터 객체. 내부의 `type`은 `end`로 설정해 리셋할 끝(end) 시퀀스 번호를 나타냄

#### 종료(terminate)

- `POST /druid/indexer/v1/supervisor/{supervisorId}/terminate`: 슈퍼바이저와 연관된 인덱싱 태스크를 종료하고 세그먼트 발행(publish)을 트리거. 재시작 시 다시 로드되지 않도록 메타데이터 스토리지에 톰스톤(tombstone) 마커를 남기며, 종료된 슈퍼바이저는 메타데이터 스토리지에 남아 이력 조회 가능. 잘못된 ID이거나 실행 중이 아니면 `404`를 반환
- `POST /druid/indexer/v1/supervisor/terminateAll`: 모든 슈퍼바이저를 종료. 대상이 없어도 `200`을 반환

#### 조기 핸드오프(handoff)

```
POST /druid/indexer/v1/supervisor/{supervisorId}/taskGroups/handoff
```

지정한 태스크 그룹의 핸드오프를 조기에 트리거. 최선 노력 방식의 API로 핸드오프 실행을 보장하지 않으며, 요청이 수락되면 `202 ACCEPTED`를 반환하고 백그라운드에서 핸드오프를 시작.

#### 셧다운(deprecated)

```
POST /druid/indexer/v1/supervisor/{supervisorId}/shutdown
```

슈퍼바이저를 종료하는 이전 방식의 엔드포인트. deprecated 상태로 향후 릴리스에서 제거될 예정이므로, 동일한 기능의 terminate 엔드포인트 사용 필요.

---

### 데이터 관리 API

세그먼트의 사용(used)·미사용(unused) 상태 전환과 영구 삭제를 담당하는 API. Overlord가 제공하는 API는 클러스터 전체에서 일관된 메타데이터를 보장. 같은 데이터소스에 대해 인덱싱 태스크나 kill 태스크가 실행 중일 때는 이 API를 동시에 사용 금지. 또한 세그먼트를 used로 표시하더라도 실제로 Historical 프로세스에 로드하려면 로드 규칙(load rule)이 별도로 설정되어 있어야 함.

#### 단일 세그먼트

- `DELETE` `/druid/indexer/v1/datasources/{datasource}/segments/{segmentId}`: 세그먼트를 unused로 표시. 세그먼트가 존재하지 않아도 `200`을 반환하므로 응답 페이로드로 실제 결과 확인 필요
- `POST` `/druid/indexer/v1/datasources/{datasource}/segments/{segmentId}`: 세그먼트를 used로 표시해 활성 상태로 되돌림

#### 세그먼트 그룹

- `POST` `/druid/indexer/v1/datasources/{datasource}/markUnused`: 여러 세그먼트를 unused로 표시
- `POST` `/druid/indexer/v1/datasources/{datasource}/markUsed`: 여러 세그먼트를 used로 표시

두 엔드포인트 모두 요청 본문에 `segmentIds` 배열 또는 `interval`(ISO 8601 형식) 중 하나를 담음. 선택적으로 `versions` 파라미터를 지정해 세그먼트 버전으로 필터링 가능.

```json
{
  "interval": "2015-09-12/2015-09-13"
}
```

#### 데이터소스 전체

- `DELETE` `/druid/indexer/v1/datasources/{datasource}`: 데이터소스의 모든 세그먼트를 unused로 표시. Historical 프로세스에서 내리는 소프트 삭제(soft delete)
- `POST` `/druid/indexer/v1/datasources/{datasource}`: 다른 세그먼트에 가려지지(overshadowed) 않은 모든 unused 세그먼트를 used로 표시

#### 세그먼트 영구 삭제

```
DELETE /druid/coordinator/v1/datasources/{datasource}/intervals/{interval}
```

지정한 인터벌의 세그먼트를 영구 삭제하는 kill 태스크를 전송. 인터벌은 ISO 8601 형식에 구분자로 `_` 사용(예: `2015-09-12_2015-09-13`). 성공 시 빈 본문과 함께 HTTP `200`을 반환.

---

### 서비스 상태 API

클러스터의 각 서비스(프로세스) 상태를 확인하는 API.

#### 공통 엔드포인트(모든 서비스)

- `GET` `/status`: Druid 버전, 로드된 익스텐션(extension), 메모리 사용량 등을 반환
- `GET` `/status/health`: 서비스 동작 여부를 확인하는 단순 헬스 체크
- `GET` `/status/properties`: 현재 설정 프로퍼티를 반환
- `GET` `/status/selfDiscovered/status`: 노드가 클러스터에 자기 탐색(self-discovery)으로 합류했는지를 JSON으로 반환
- `GET` `/status/selfDiscovered`: 노드 탐색 여부를 HTTP 상태 코드로 반환

#### Coordinator

- `GET` `/druid/coordinator/v1/leader`: 현재 리더 Coordinator의 주소를 반환
- `GET` `/druid/coordinator/v1/isLeader`: 해당 서버가 현재 리더이면 `true`를 반환
- `GET` `/druid/coordinator/v1/config/cloneStatus`: Historical 클로닝(cloning)의 현재 상태를 반환
- `GET` `/druid/coordinator/v1/config/syncedBrokers`: 최신 설정으로 동기화된 Broker 목록을 반환

#### Overlord

- `GET` `/druid/indexer/v1/leader`: 현재 리더 Overlord의 주소를 반환
- `GET` `/druid/indexer/v1/isLeader`: 해당 서버가 현재 리더 Overlord인지 반환

#### Historical / Broker 로드 상태

- `GET` `/druid/historical/v1/loadstatus`: 로컬 캐시의 모든 세그먼트가 로드되었는지 반환
- `GET` `/druid/historical/v1/readiness`: Historical의 세그먼트 준비 상태를 HTTP 상태 코드로 반환
- `GET` `/druid/broker/v1/loadstatus`: Broker가 클러스터의 모든 세그먼트를 인지하고 있는지를 나타내는 플래그를 반환
- `GET` `/druid/broker/v1/readiness`: Broker가 쿼리를 받을 준비가 되었는지를 HTTP 상태 코드로 반환

---

### 참고 자료

- [API reference 개요](https://druid.apache.org/docs/latest/api-reference/)
- [Druid SQL API](https://druid.apache.org/docs/latest/api-reference/sql-api)
- [SQL-based ingestion API](https://druid.apache.org/docs/latest/api-reference/sql-ingestion-api)
- [Tasks API](https://druid.apache.org/docs/latest/api-reference/tasks-api)
- [Supervisor API](https://druid.apache.org/docs/latest/api-reference/supervisor-api)
- [Data management API](https://druid.apache.org/docs/latest/api-reference/data-management-api)
- [Service status API](https://druid.apache.org/docs/latest/api-reference/service-status-api)
- [JSON querying API](https://druid.apache.org/docs/latest/api-reference/json-querying-api)
- [SQL JDBC driver API](https://druid.apache.org/docs/latest/api-reference/sql-jdbc)

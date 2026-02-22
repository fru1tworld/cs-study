# Chapter 20. 클라이언트 인증 (Client Authentication)

PostgreSQL 18 공식 문서 번역

---

## 목차

- [20.1 pg_hba.conf 파일](#201-pg_hbaconf-파일)
- [20.2 사용자 이름 맵](#202-사용자-이름-맵)
- [20.3 인증 방법](#203-인증-방법)
- [20.4 Trust 인증](#204-trust-인증)
- [20.5 패스워드 인증](#205-패스워드-인증)
- [20.6 GSSAPI 인증](#206-gssapi-인증)
- [20.7 SSPI 인증](#207-sspi-인증)
- [20.8 Ident 인증](#208-ident-인증)
- [20.9 Peer 인증](#209-peer-인증)
- [20.10 LDAP 인증](#2010-ldap-인증)
- [20.11 RADIUS 인증](#2011-radius-인증)
- [20.12 인증서 인증](#2012-인증서-인증)
- [20.13 PAM 인증](#2013-pam-인증)
- [20.14 BSD 인증](#2014-bsd-인증)
- [20.15 OAuth 인증/권한 부여](#2015-oauth-인증권한-부여)
- [20.16 인증 문제 해결](#2016-인증-문제-해결)

---

## 개요

클라이언트 애플리케이션이 데이터베이스 서버에 연결할 때, 해당 연결 요청에 사용될 PostgreSQL 데이터베이스 사용자 이름을 지정합니다. 이는 SQL 환경 내에서의 접근 권한을 결정하기 위해 특정 데이터베이스 사용자 이름으로 연결을 요청하는 방식과 유사합니다.

인증(Authentication) 은 데이터베이스 서버가 클라이언트의 신원을 확인하고, 궁극적으로 해당 클라이언트 애플리케이션(또는 클라이언트 애플리케이션을 실행하는 사용자)이 요청한 사용자 이름으로 연결할 수 있는지 결정하는 프로세스입니다.

PostgreSQL은 클라이언트를 인증하기 위한 다양한 방법을 제공합니다. 특정 클라이언트 연결을 인증하는 데 사용되는 방법은 호스트 주소, 데이터베이스, 사용자 를 기반으로 선택될 수 있습니다.

PostgreSQL 데이터베이스 사용자 이름은 서버가 실행되는 운영체제의 사용자 이름과 논리적으로 분리되어 있습니다. 특정 서버의 모든 사용자가 서버 머신에 계정을 갖고 있다면 운영체제 사용자 이름과 일치하는 데이터베이스 사용자 이름을 할당하는 것이 합리적입니다. 그러나 원격 연결을 허용하는 서버는 로컬 운영체제 계정이 없는 많은 데이터베이스 사용자를 가질 수 있습니다. 이 경우 데이터베이스 사용자 이름과 운영체제 사용자 이름 간의 연결은 필요하지 않습니다.

`LOGIN` 권한을 가진 역할(role)을 "데이터베이스 사용자"라고 부릅니다. 데이터베이스 객체에 대한 접근 권한은 활성 데이터베이스 사용자 이름에 의해 결정됩니다.

---

## 20.1 pg_hba.conf 파일

`pg_hba.conf` 파일은 PostgreSQL에서 클라이언트 인증을 제어하는 기본 설정 파일입니다. HBA는 "Host-Based Authentication(호스트 기반 인증)"을 의미합니다. 이 파일은 데이터베이스 클러스터의 데이터 디렉토리에 저장되며, 서버 시작 시와 메인 서버 프로세스가 SIGHUP 신호를 수신할 때 읽힙니다.

### 파일 다시 읽기

- Linux/Unix에서: 변경 사항을 적용하려면 다음 방법으로 postmaster에 신호를 보내야 합니다:
  - `pg_ctl reload`
  - SQL 함수 `pg_reload_conf()`
  - `kill -HUP`

- Windows에서: 변경 사항은 이후의 새 연결에 즉시 적용됩니다.

### 일반 형식

- 레코드는 한 줄에 하나씩 작성
- 빈 줄과 주석(`#`으로 시작)은 무시됨
- 백슬래시(`\`)를 사용하여 다음 줄로 레코드 계속 가능
- 필드는 공백 및/또는 탭으로 구분
- 공백을 포함하는 필드는 큰따옴표로 묶어야 함
- 특수 키워드(`all`, `replication` 등)를 따옴표로 묶으면 리터럴 데이터베이스/사용자/호스트 이름으로 처리됨

### 레코드 형식

인증 레코드는 다음과 같은 형식을 갖습니다:

```
local               database  user  auth-method [auth-options]
host                database  user  address     auth-method [auth-options]
hostssl             database  user  address     auth-method [auth-options]
hostnossl           database  user  address     auth-method [auth-options]
hostgssenc          database  user  address     auth-method [auth-options]
hostnogssenc        database  user  address     auth-method [auth-options]
host                database  user  IP-address  IP-mask  auth-method [auth-options]
hostssl             database  user  IP-address  IP-mask  auth-method [auth-options]
hostnossl           database  user  IP-address  IP-mask  auth-method [auth-options]
hostgssenc          database  user  IP-address  IP-mask  auth-method [auth-options]
hostnogssenc        database  user  IP-address  IP-mask  auth-method [auth-options]
include             file
include_if_exists   file
include_dir         directory
```

### 연결 유형 필드

| 유형 | 설명 |
|------|------|
| `local` | Unix 도메인 소켓 연결 |
| `host` | TCP/IP 연결 (SSL 또는 비-SSL, GSSAPI 암호화 여부 무관) |
| `hostssl` | SSL 암호화를 사용하는 TCP/IP 연결만 |
| `hostnossl` | SSL을 사용하지 않는 TCP/IP 연결 |
| `hostgssenc` | GSSAPI 암호화를 사용하는 TCP/IP 연결만 |
| `hostnogssenc` | GSSAPI 암호화를 사용하지 않는 TCP/IP 연결 |

참고: 원격 TCP/IP 연결은 적절한 `listen_addresses` 설정으로 서버가 시작되어야 합니다 (기본값은 localhost만).

### 데이터베이스 필드

레코드가 일치하는 데이터베이스를 지정합니다:

- `all` — 모든 데이터베이스와 일치
- `sameuser` — 데이터베이스 이름이 사용자 이름과 같으면 일치
- `samerole` — 사용자가 데이터베이스와 같은 이름의 역할의 멤버이면 일치
- `samegroup` — `samerole`의 더 이상 사용되지 않는 표기
- `replication` — 물리적 복제 연결과 일치 (논리적 복제는 아님)
- 특정 데이터베이스 이름
- 정규 표현식 (`/`로 시작하는 경우)
- 쉼표로 구분된 여러 값
- 외부 파일 (`@`로 시작)

정규 표현식 예제:
```
/^db\d{2,4}$
```

### 사용자 필드

레코드가 일치하는 데이터베이스 사용자를 지정합니다:

- `all` — 모든 사용자와 일치
- 특정 사용자 이름
- 정규 표현식 (`/`로 시작하는 경우)
- 그룹 이름 (`+`로 시작하면 해당 역할과 모든 직접/간접 멤버와 일치)
- 쉼표로 구분된 여러 값
- 외부 파일 (`@`로 시작)

### 주소 필드

클라이언트 머신 주소를 지정합니다:

#### IP 주소 범위 (CIDR 표기법)
```
172.20.143.89/32        # 단일 IPv4 호스트
172.20.143.0/24         # 소규모 IPv4 네트워크
10.6.0.0/16             # 대규모 IPv4 네트워크
::1/128                 # 단일 IPv6 호스트 (루프백)
fe80::7a31:c1ff:0000:0000/96  # IPv6 네트워크
0.0.0.0/0               # 모든 IPv4 주소
::0/0                   # 모든 IPv6 주소
```

IPv4 마스크 길이: 단일 호스트의 경우 32
IPv6 마스크 길이: 단일 호스트의 경우 128

#### 대체 넷마스크 형식
```
IP-address  IP-mask
127.0.0.1   255.255.255.255  # 단일 호스트
172.20.143.0 255.255.255.0   # 네트워크 /24
```

#### 특수 키워드
- `all` — 모든 IP 주소
- `samehost` — 서버 자체의 모든 IP 주소
- `samenet` — 직접 연결된 서브넷의 모든 주소

#### 호스트 이름
- 대소문자 구분 없이 처리
- 정방향 및 역방향 DNS 해석 수행
- `.`으로 시작하는 호스트 이름은 접미사와 일치 (예: `.example.com`은 `foo.example.com`과 일치)
- 호스트 이름 데이터베이스는 역방향 DNS 조회 결과와 일치해야 함

호스트 이름 해석 프로세스:
1. 클라이언트 IP의 역방향 DNS 조회
2. 결과 호스트 이름의 정방향 DNS 조회
3. 둘 다 클라이언트 IP와 일치해야 항목이 일치함

### 인증 방법

| 방법 | 설명 |
|------|------|
| `trust` | 무조건 연결 허용 (패스워드 불필요) |
| `reject` | 무조건 연결 거부 |
| `scram-sha-256` | SCRAM-SHA-256 패스워드 인증 |
| `md5` | SCRAM-SHA-256 또는 MD5 패스워드 인증 (더 이상 권장하지 않음) |
| `password` | 암호화되지 않은 패스워드 (신뢰할 수 없는 네트워크에서는 권장하지 않음) |
| `gss` | GSSAPI 인증 (TCP/IP만) |
| `sspi` | SSPI 인증 (Windows만) |
| `ident` | ident 서버를 통한 운영체제 사용자 이름 (TCP/IP 또는 로컬) |
| `peer` | 운영체제 사용자 이름 (로컬 연결만) |
| `ldap` | LDAP 서버 인증 |
| `radius` | RADIUS 서버 인증 |
| `cert` | SSL 클라이언트 인증서 인증 |
| `pam` | 플러그형 인증 모듈 |
| `bsd` | BSD 인증 서비스 |
| `oauth` | OAuth 2.0 ID 제공자 |

참고: 모든 방법 이름은 소문자이며 대소문자를 구분합니다.

### 인증 옵션

#### 방법별 옵션
옵션은 auth-method 필드 뒤에 `name=value` 쌍으로 지정됩니다.

#### 방법에 독립적인 옵션

##### `clientcert` (hostssl 레코드만)
사용 가능한 값:
- `verify-ca` — 클라이언트가 유효한 SSL 인증서를 제시하는지 확인
- `verify-full` — 인증서를 확인하고 CN(Common Name)이 사용자 이름과 일치하는지 확인

##### `clientname` (클라이언트 인증서 인증)
사용 가능한 값:
- `CN` (기본값) — 인증서의 Common Name과 사용자 이름 일치 확인
- `DN` — 전체 Distinguished Name과 사용자 이름 일치 확인 (RFC 2253 형식)

클라이언트 인증서 DN 보기:
```bash
openssl x509 -in myclient.crt -noout -subject -nameopt RFC2253 | sed "s/^subject=//"
```

### Include 지시문

#### `include`
해당 줄을 주어진 파일의 내용으로 대체합니다.

#### `include_if_exists`
파일이 존재하면 파일 내용으로 대체하고, 그렇지 않으면 메시지를 로그에 기록합니다.

#### `include_dir`
해당 줄을 `.`으로 시작하지 않고 `.conf`로 끝나는 디렉토리의 모든 파일로 대체합니다. 파일 이름 순서대로 처리됩니다 (C 로케일: 숫자가 문자보다 먼저, 대문자가 소문자보다 먼저).

#### 외부 이름 목록 (@)
`@`로 참조되는 파일은 공백이나 쉼표로 구분된 이름을 포함합니다:
```
@admins       # 외부 파일 참조
@demodbs      # 데이터베이스용
```

- 주석은 `#` 사용
- 중첩된 `@` 구문 허용
- 상대 경로는 참조하는 파일 기준

### 레코드 처리

- 각 연결 시도마다 레코드가 순차적으로 검사됨
- 첫 번째 일치하는 레코드가 사용됨 — 폴스루나 백업 없음
- 일치하는 레코드에서 인증이 실패하면 후속 레코드는 무시됨
- 일치하는 레코드가 없으면 접근이 거부됨
- 순서가 중요함: 일반적으로 약한 인증 방법의 엄격한 일치 기준을 먼저, 강한 방법의 느슨한 기준을 나중에 배치

### 설정 예제

#### 기본 로컬 Trust
```
# Unix 도메인 소켓 - 모든 로컬 사용자 신뢰
local   all             all                                     trust

# TCP/IP localhost - 신뢰
host    all             all             127.0.0.1/32            trust
host    all             all             ::1/128                 trust
```

#### 패스워드 보호 원격 접근
```
# 로컬 네트워크는 패스워드 필요
host    postgres        all             192.168.12.10/32        scram-sha-256

# 도메인은 패스워드 필요
host    all             all             .example.com            scram-sha-256
```

#### 혼합 인증
```
# 구형 클라이언트 예외
host    all             mike            .example.com            md5
host    all             all             .example.com            scram-sha-256
```

#### GSSAPI 설정
```
# 특정 호스트 거부
host    all             all             192.168.54.1/32         reject

# 어디서든 GSSAPI 암호화
hostgssenc all          all             0.0.0.0/0               gss

# 특정 호스트에서 GSSAPI 비암호화
host    all             all             192.168.12.10/32        gss
```

#### 데이터베이스 및 사용자 제한
```
# 사용자는 동일한 이름의 데이터베이스만
local   sameuser        all                                     md5

# 헬프데스크 사용자는 모든 데이터베이스
local   all             /^.*helpdesk$                           md5

# 관리자 파일
local   all             @admins                                 md5

# 지원 역할 멤버
local   all             +support                                md5

# 결합
local   all             @admins,+support                        md5
```

#### 정규 표현식
```
# 데이터베이스 이름 패턴
host    "/^db\d{2,4}$"  all             localhost               trust
```

#### 맵핑과 함께 Ident 인증
```
host    all             all             192.168.0.0/16          ident map=omicron
```

### 중요 참고 사항

#### 연결 권한
사용자는 pg_hba.conf 검사를 통과하고 데이터베이스에 대한 `CONNECT` 권한도 가져야 합니다. 세분화된 제어를 위해 pg_hba.conf보다 데이터베이스 권한을 사용하세요.

#### 호스트 이름 해석 성능
- 로컬 이름 캐시 사용 (예: `nscd`)
- IP 주소 대신 호스트 이름을 로그에 기록하려면 `log_hostname` 활성화
- 접미사 일치 (`.example.com`)에는 역방향 DNS 필요

#### 슈퍼유저 멤버십
슈퍼유저는 명시적으로 할당된 경우에만 역할의 멤버로 간주되며, 슈퍼유저라는 이유만으로는 멤버가 아닙니다.

### 디버깅을 위한 뷰

시스템 뷰 `pg_hba_file_rules`는 변경 사항 테스트 또는 문제 진단에 도움이 됩니다. null이 아닌 `error` 필드가 있는 행은 해당 줄의 문제를 나타냅니다.

---

## 20.2 사용자 이름 맵

사용자 이름 맵을 사용하면 Ident나 GSSAPI와 같은 외부 인증 시스템을 사용할 때 운영체제 사용자 이름을 데이터베이스 사용자(역할) 이름에 매핑할 수 있습니다. 이는 연결을 시작하는 OS 사용자와 의도한 데이터베이스 사용자가 다를 때 필요합니다.

### 설정

#### 사용자 이름 맵 활성화
사용자 이름 매핑을 사용하려면 `pg_hba.conf`의 옵션 필드에 `map=map-name`을 지정하세요. 이 옵션은 외부 사용자 이름을 받는 모든 인증 방법에서 작동합니다.

#### 맵 파일 위치
사용자 이름 맵은 ident 맵 파일 에 정의되며, 기본적으로 `pg_ident.conf`라는 이름으로 클러스터의 데이터 디렉토리에 저장됩니다. 위치는 `ident_file` 설정 매개변수로 사용자 정의할 수 있습니다.

### 파일 문법

`pg_ident.conf` 파일은 다음 형식의 줄을 포함합니다:

```
map-name       system-username       database-username
include        file
include_if_exists   file
include_dir    directory
```

필드 설명:
- `map-name`: `pg_hba.conf`에서 참조되는 임의의 식별자
- `system-username`: 운영체제 사용자 이름
- `database-username`: PostgreSQL 데이터베이스 사용자 이름

주석, 공백, 줄 계속은 `pg_hba.conf`와 동일한 규칙을 따릅니다.

### 다시 읽기 메커니즘

`pg_ident.conf` 파일은 다음 시점에 읽힙니다:
- 서버 시작 시
- 메인 서버 프로세스가 SIGHUP 신호를 수신할 때

활성 시스템에서 다시 읽기:
- `pg_ctl reload`
- SQL 함수 `pg_reload_conf()`
- `kill -HUP` 명령

### 고급 기능

#### 특수 문자 및 키워드

`all` 키워드: 시스템 사용자가 모든 기존 데이터베이스 사용자로 연결할 수 있도록 `database-username`으로 사용:
```
map-name    system-username    all
```
`"all"`을 따옴표로 묶으면 리터럴 사용자 이름으로 처리됩니다.

`+` 접두사: 해당 역할에 속하는 모든 사용자로 로그인 허용:
```
map-name    system-username    +role-name
```
- `+role-name`: 지정된 역할의 직접 또는 간접 멤버인 모든 역할과 일치
- `role-name` (`+` 없이): 해당 특정 역할만 일치

특수 의미를 제거하려면 사용자 이름을 따옴표로 묶으세요 (예: `"+admin"`).

#### 정규 표현식

시스템 사용자 이름에 정규 표현식: `system-username`이 `/`로 시작하면 POSIX 정규 표현식으로 처리됩니다:
```
map-name    /^(.*)@mydomain\.com$    \1
```

기능:
- 단일 캡처 그룹(괄호로 묶인 하위 표현식)을 포함할 수 있음
- `database-username`에서 `\1` (백슬래시-1)을 사용하여 캡처된 내용 참조
- 모범 사례: 전체 문자열을 일치시키려면 `^` 및 `$` 앵커 사용

데이터베이스 사용자 이름에 정규 표현식: `database-username`이 `/`로 시작하면 정규 표현식으로 처리됩니다:
```
map-name    system-username    /^(allowed_users)$/
```

제한 사항: 데이터베이스 사용자 이름 정규 표현식 패턴 내에서 `\1` 참조를 사용할 수 없습니다.

### 실제 예제

#### 예제 20.2: 샘플 `pg_ident.conf` 파일

```
# MAPNAME       SYSTEM-USERNAME         PG-USERNAME

omicron         bryanh                  bryanh
omicron         ann                     ann
# bob는 이 머신들에서 사용자 이름이 robert
omicron         robert                  bob
# bryanh는 guest1으로도 연결 가능
omicron         bryanh                  guest1
```

동작:
- `bryanh`: `bryanh` 또는 `guest1`로 연결 가능
- `ann`: `ann`으로만 연결 가능
- `robert`: `bob`으로만 연결 가능
- 다른 모든 사용자: 접근 거부

#### 고급 정규 표현식 예제

```
mymap   /^(.*)@mydomain\.com$      \1
mymap   /^(.*)@otherdomain\.com$   guest
```

- 첫 번째 줄: `@mydomain.com` 사용자의 도메인 접미사 제거
- 두 번째 줄: 모든 `@otherdomain.com` 사용자가 `guest`로 연결하도록 허용

### 진단 및 문제 해결

#### 시스템 뷰: `pg_ident_file_mappings`
시스템 뷰 `pg_ident_file_mappings`는 다음에 도움이 됩니다:
- `pg_ident.conf` 변경 사항 사전 테스트
- 로딩 문제 진단
- 설정 문제 식별 (문제를 나타내는 null이 아닌 값에 대한 `error` 열 확인)

### 핵심 개념

1. 카디널리티 제한 없음: 하나의 OS 사용자에서 여러 데이터베이스 사용자로 매핑 가능하며, 그 반대도 가능
2. 권한 모델: 맵 항목은 "*이 OS 사용자가 이 데이터베이스 사용자로 연결하도록 허용됨*"을 의미 (동등성이 아님)
3. 연결 로직: 맵 항목이 외부 인증 시스템 사용자 이름과 요청된 데이터베이스 사용자 이름을 연결하면 연결이 허용됨

---

## 20.3 인증 방법

PostgreSQL은 사용자를 인증하기 위한 13가지 다른 인증 방법 을 제공합니다. 연결 유형과 보안 인프라 가용성에 따라 적절한 방법을 선택하는 것이 좋습니다.

### 사용 가능한 인증 방법

#### 1. Trust 인증
- 사용자가 주장하는 대로 신뢰
- 패스워드 불필요
- 자세한 내용: [20.4 Trust 인증](#204-trust-인증)

#### 2. 패스워드 인증
- 사용자가 패스워드를 보내야 함
- 대부분의 시나리오에 적합한 표준 접근 방식
- 자세한 내용: [20.5 패스워드 인증](#205-패스워드-인증)

#### 3. GSSAPI 인증
- GSSAPI 호환 보안 라이브러리 사용
- 일반적으로 인증 서버 (Kerberos, Microsoft Active Directory)에 사용
- 자세한 내용: [20.6 GSSAPI 인증](#206-gssapi-인증)

#### 4. SSPI 인증
- GSSAPI와 유사한 Windows 전용 프로토콜
- 플랫폼별 구현
- 자세한 내용: [20.7 SSPI 인증](#207-sspi-인증)

#### 5. Ident 인증
- "Identification Protocol" (RFC 1413) 서비스 사용
- 로컬 Unix 소켓 연결의 경우 peer 인증으로 처리됨
- 자세한 내용: [20.8 Ident 인증](#208-ident-인증)

#### 6. Peer 인증
- 운영체제 기능을 사용하여 연결 끝점의 프로세스 식별
- 로컬 연결만 지원 (원격은 지원하지 않음)
- 자세한 내용: [20.9 Peer 인증](#209-peer-인증)

#### 7. LDAP 인증
- LDAP 인증 서버 사용
- 자세한 내용: [20.10 LDAP 인증](#2010-ldap-인증)

#### 8. RADIUS 인증
- RADIUS 인증 서버 사용
- 자세한 내용: [20.11 RADIUS 인증](#2011-radius-인증)

#### 9. 인증서 인증
- SSL 연결 필요
- SSL 인증서 검증을 통해 사용자 인증
- 자세한 내용: [20.12 인증서 인증](#2012-인증서-인증)

#### 10. PAM 인증
- PAM (Pluggable Authentication Modules) 라이브러리 사용
- 자세한 내용: [20.13 PAM 인증](#2013-pam-인증)

#### 11. BSD 인증
- BSD Authentication 프레임워크 사용
- OpenBSD에서만 사용 가능
- 자세한 내용: [20.14 BSD 인증](#2014-bsd-인증)

#### 12. OAuth 인증/권한 부여
- 외부 OAuth 2.0 ID 제공자 사용
- 자세한 내용: [20.15 OAuth 인증/권한 부여](#2015-oauth-인증권한-부여)

### 연결 유형별 권장 사항

| 연결 유형 | 권장 방법 |
|-----------|-----------|
| 로컬 연결 | Peer 인증 (선호), Trust 인증 (충분한 경우) |
| 원격 연결 | 패스워드 인증 (가장 쉬운 선택) |
| 기업 환경 | GSSAPI, LDAP, RADIUS, 인증서, OAuth |
| Windows 환경 | SSPI 인증 |

### 주요 고려 사항

- 외부 인프라 필요: GSSAPI, SSPI, LDAP, RADIUS, 인증서, PAM, BSD, OAuth 방법은 인증 서버나 인증 기관이 필요합니다
- 플랫폼별: SSPI (Windows), BSD 인증 (OpenBSD)
- 보안 대 단순성 트레이드오프: Trust 인증이 가장 간단하지만 보안이 가장 낮음; 다른 방법은 더 많은 인프라가 필요하지만 더 나은 보안 제공

---

## 20.4 Trust 인증

Trust 인증은 서버가 연결할 수 있는 모든 사람이 지정한 모든 사용자 이름(슈퍼유저 이름 포함)으로 데이터베이스에 접근할 권한이 있다고 가정하는 PostgreSQL 인증 방법입니다. `pg_hba.conf`의 `database` 및 `user` 열 제한은 여전히 적용됩니다.

### 주요 특성

#### 사용 시기
- 적절한 경우: 단일 사용자 워크스테이션에서의 로컬 연결
- 적절하지 않은 경우: 다중 사용자 머신에서 단독으로

### 보안 고려 사항

Unix 도메인 소켓 연결의 경우:
- 파일 시스템 권한을 사용하여 Unix 도메인 소켓 파일에 대한 접근을 제한하면 다중 사용자 머신에서 사용 가능
- 다음 매개변수를 설정하세요:
  - `unix_socket_permissions` - 소켓 파일 권한 설정
  - `unix_socket_group` - 선택적으로 소켓 그룹 소유권 설정
  - `unix_socket_directories` - 소켓을 제한된 디렉토리에 배치

TCP/IP 연결의 경우:
- 다음 경우에만 적합: `pg_hba.conf`가 연결을 허용하는 모든 머신의 모든 사용자를 신뢰하는 경우
- 보안 위험: 로컬 TCP/IP 연결은 파일 시스템 권한으로 제한되지 않음
- 모범 사례: localhost (127.0.0.1)에서의 TCP/IP 연결에만 `trust` 사용
- 중요: 로컬 보안을 위해 파일 시스템 권한에 의존하는 경우 `pg_hba.conf`에서 `host ... 127.0.0.1 ...` 줄을 `trust`에서 다른 인증 방법으로 제거하거나 변경하세요

### 중요 경고
Trust 인증은 서버에 대한 연결에 적절한 운영체제 수준 보호 가 있는 경우에만 사용해야 합니다. 추가 접근 제어 없이 프로덕션 다중 사용자 시스템이 아닌 격리된 단일 사용자 환경에 가장 적합한 편의 방법입니다.

---

## 20.5 패스워드 인증

PostgreSQL의 패스워드 기반 인증 방법은 사용자 패스워드가 서버에 저장되는 방식과 연결을 통해 패스워드가 전송되는 방식에 따라 다릅니다.

### 인증 방법

#### 1. scram-sha-256 (권장)
- 표준: RFC 7677 준수
- 유형: 챌린지-응답 스킴
- 보안 수준: 현재 사용 가능한 가장 안전한 방법
- 기능:
  - 신뢰할 수 없는 연결에서 패스워드 스니핑 방지
  - 암호화 해시된 패스워드 저장 지원
  - 패스워드가 공격에 안전한 것으로 간주됨
- 제한 사항: 구형 클라이언트 라이브러리에서 지원되지 않음

#### 2. md5 (더 이상 권장하지 않음)
- 유형: 사용자 정의 챌린지-응답 메커니즘
- 보안 수준: 덜 안전함
- 기능:
  - 패스워드 스니핑 방지
  - 서버에 평문 패스워드 저장 방지
- 제한 사항:
  - 서버에서 패스워드 해시가 도난당한 경우 보호 없음
  - MD5 알고리즘은 더 이상 결정적인 공격에 안전하지 않은 것으로 간주됨
  - 지원이 더 이상 권장되지 않으며 향후 PostgreSQL 릴리스에서 제거될 예정

자동 업그레이드 기능: `pg_hba.conf`에 `md5`가 지정되어 있지만 사용자의 패스워드가 SCRAM으로 암호화된 경우, SCRAM 기반 인증이 자동으로 선택됩니다.

#### 3. password (가장 안전하지 않음)
- 유형: 평문 전송
- 보안 수준: 패스워드 스니핑 공격에 취약
- 권장 사항: 연결이 SSL로 암호화되지 않는 한 피하세요
- 대안: SSL 인증서 인증이 더 나을 수 있음

### 패스워드 관리

저장 위치: `pg_authid` 시스템 카탈로그

관리 명령:
```sql
CREATE ROLE foo WITH LOGIN PASSWORD 'secret';
ALTER ROLE username PASSWORD 'new_password';
```

대안: psql의 `\password` 명령

기본 상태: 패스워드가 설정되지 않으면 저장된 패스워드는 null이고 인증은 항상 실패합니다.

### 패스워드 암호화 설정

`password_encryption` 매개변수는 패스워드가 설정될 때 사용되는 암호화 방법을 제어합니다.

#### 암호화 및 방법 호환성

| 패스워드 암호화 방식 | 사용 가능한 인증 방법 |
|----------------------|----------------------|
| `scram-sha-256` | `scram-sha-256`, `password` (평문), `md5` (자동으로 SCRAM으로 전환) |
| `md5` | `md5`, `password` (평문) |

### 마이그레이션 가이드: MD5에서 SCRAM-SHA-256으로

기존 설치를 업그레이드하려면:

1. 클라이언트 호환성 확인: 모든 클라이언트 라이브러리가 SCRAM을 지원하는지 확인
2. 설정 업데이트:
   ```
   password_encryption = 'scram-sha-256'
   ```
   (`postgresql.conf`에서)
3. 사용자 패스워드 재설정: 모든 사용자가 새 패스워드를 설정해야 함
4. 인증 방법 업데이트:
   ```
   # pg_hba.conf에서
   # 변경 전: md5
   # 변경 후: scram-sha-256
   ```

### 중요 참고 사항

- PostgreSQL 데이터베이스 패스워드는 운영체제 사용자 패스워드와 별개 입니다
- 이전 PostgreSQL 릴리스는 평문 패스워드 저장을 지원했지만; 이제는 더 이상 불가능합니다
- 현재 저장된 패스워드 해시를 확인하려면 `pg_authid` 시스템 카탈로그를 쿼리하세요

---

## 20.6 GSSAPI 인증

GSSAPI (Generic Security Service Application Program Interface)는 RFC 2743에 정의된 안전한 인증을 위한 산업 표준 프로토콜입니다. PostgreSQL은 다음을 위해 GSSAPI를 지원합니다:
- 인증
- 통신 암호화
- 인증과 암호화 모두

주요 기능: 지원되는 시스템에서 자동 인증 (싱글 사인온) 제공.

### 전제 조건
- PostgreSQL 빌드 시 GSSAPI 지원이 활성화되어야 함 (설치 세부 사항은 Chapter 17 참조)
- GSSAPI 암호화 또는 SSL 암호화가 사용되면 데이터가 암호화됨; 그렇지 않으면 암호화되지 않음

### 서비스 주체 이름 형식

GSSAPI가 Kerberos를 사용할 때 표준 형식을 사용합니다:
```
servicename/hostname@realm
```

#### 주체 구성 요소:
- servicename: 일반적으로 `postgres`이지만 libpq의 `krbsrvname` 연결 매개변수로 사용자 정의 가능
- hostname: libpq가 연결하는 정규화된 호스트 이름
- realm: 서버가 접근할 수 있는 Kerberos 설정 파일에서 선호하는 realm

주체 이름은 PostgreSQL 서버에 인코딩되지 않습니다; 대신 서버가 읽는 keytab 파일 에 지정됩니다. keytab에 여러 주체가 있으면 서버는 그 중 하나를 수락합니다.

### 클라이언트 요구 사항

클라이언트는 다음을 해야 합니다:
1. 연결하려는 서버의 주체 이름을 알아야 함
2. 유효한 티켓이 있는 자체 주체 이름을 가져야 함
3. 클라이언트 주체가 PostgreSQL 데이터베이스 사용자 이름과 연관되어야 함

#### 사용자 이름 매핑
사용자 이름 매핑은 `pg_ident.conf`를 통해 설정할 수 있습니다. 예를 들어:
- `pgusername@realm` → `pgusername`
- 또는 전체 `username@realm` 주체를 PostgreSQL 역할 이름으로 사용

중요: PostgreSQL은 realm을 제거하여 매핑하는 것도 지원하지만 (이전 버전과의 호환성만을 위해), 이것은 강력히 권장하지 않습니다. 같은 이름을 가진 다른 realm의 사용자를 구별하지 못하기 때문입니다.

### 설정 매개변수

#### 항목별 옵션 (`pg_hba.conf`에서)

`include_realm`
- 값: 0 또는 1 (기본값: 1)
- 0으로 설정하면: 사용자 이름 매핑 전에 인증된 주체에서 realm 이름 제거
- 상태: 권장하지 않음; 주로 이전 버전과의 호환성을 위해
- 보안 참고: `krb_realm`도 함께 사용하지 않으면 다중 realm 환경에서 안전하지 않음
- 권장 사항: 기본값 (1)을 유지하고 `pg_ident.conf`에서 명시적 매핑 사용

`map`
- 클라이언트 주체에서 데이터베이스 사용자 이름으로 매핑 허용
- 자세한 내용은 Section 20.2 참조
- 매핑을 위한 주체 형식:
  - `username@EXAMPLE.COM` (또는 `username/hostbased@EXAMPLE.COM`)
  - `include_realm` = 0인 경우: `username` (또는 `username/hostbased`) 사용

`krb_realm`
- 사용자 주체 이름과 일치시킬 realm 설정
- 설정된 경우: 해당 realm의 사용자만 수락됨
- 설정되지 않은 경우: 모든 realm의 사용자가 연결 가능 (사용자 이름 매핑에 따라)

#### 서버 전체 옵션

`krb_caseins_users`
- 유형: 불리언
- 효과: true로 설정하면 클라이언트 주체가 사용자 맵 항목에 대소문자를 구분하지 않고 일치됨
- `krb_realm`의 대소문자를 구분하지 않는 일치에도 영향을 줌

`krb_server_keyfile`
- 서버의 keytab 파일 위치 지정
- 보안 권장 사항: PostgreSQL 서버 전용 별도 keytab 사용 (시스템 keytab 아님)
- 권한: keytab은 PostgreSQL 서버 계정만 읽을 수 있어야 함 (읽기 전용 권장)

### Keytab 파일 생성

keytab 파일은 Kerberos 소프트웨어를 사용하여 생성됩니다. 문서에서는 MIT Kerberos의 `kadmin` 도구를 참조합니다 (자세한 지침은 Kerberos 문서 참조).

### 보안 고려 사항

1. 별도 Keytab: 시스템 keytab이 아닌 PostgreSQL 전용 keytab 사용
2. 파일 권한: keytab이 PostgreSQL 서버 계정만 읽을 수 있도록 보장
3. 다중 Realm 환경: realm 제거에 의존하지 않고 명시적 `pg_ident.conf` 매핑 사용
4. Realm 일치: 단일 realm 설치에서 추가 보안을 위해 `krb_realm` 매개변수 사용

---

## 20.7 SSPI 인증

SSPI (Security Support Provider Interface)는 PostgreSQL에서 싱글 사인온을 통한 안전한 인증을 위한 Windows 기술입니다. 주요 특성:

- 작동 모드: `negotiate` 모드를 사용하며 먼저 Kerberos를 시도하고 필요한 경우 자동으로 NTLM으로 폴백
- 상호 운용성: SSPI와 GSSAPI 클라이언트 및 서버가 상호 운용됨 (예: SSPI 클라이언트가 GSSAPI 서버에 인증 가능)
- 플랫폼 권장 사항: Windows 플랫폼에서는 SSPI 사용, 비-Windows 플랫폼에서는 GSSAPI 사용

### Kerberos 인증

Kerberos 인증을 사용할 때 SSPI는 GSSAPI와 동일하게 작동합니다. 자세한 정보는 Section 20.6 (GSSAPI 인증)을 참조하세요.

### 설정 옵션

#### 1. include_realm
- 기본값: 1 (활성화)
- 기능: 인증된 사용자 주체의 realm 이름을 포함할지 여부 제어
- 0으로 설정하면: 사용자 이름 매핑을 통과하기 전에 realm 이름 제거
- 보안 참고: 비활성화는 권장하지 않으며 주로 이전 버전과의 호환성을 위함; `krb_realm`도 함께 사용하지 않으면 다중 realm 환경에서 안전하지 않음
- 권장 사항: 기본값 (1)을 유지하고 `pg_ident.conf`에서 명시적 매핑 제공

#### 2. compat_realm
- 기본값: 1 (활성화)
- 기능: `include_realm` 옵션에 사용되는 realm 이름 형식 결정
  - 활성화 시: 도메인의 SAM 호환 이름 (NetBIOS 이름) 사용
  - 비활성화 (0) 시: Kerberos 사용자 주체 이름의 실제 realm 이름 사용
- 중요 경고: 다음 조건이 충족되지 않으면 비활성화하지 마세요:
  - 서버가 도메인 계정 (가상 서비스 계정 포함)으로 실행됨
  - 그리고 모든 SSPI 클라이언트도 도메인 계정 사용
  - 그렇지 않으면 인증이 실패함

#### 3. upn_username
- 기본값: 비활성화 (0)
- 기능: 인증에 사용되는 사용자 이름 형식 제어
  - `compat_realm`과 함께 활성화하면: Kerberos UPN (User Principal Name) 사용
  - 비활성화하면: SAM 호환 사용자 이름 사용
- 참고: 기본적으로 새 사용자 계정의 경우 이 두 이름은 동일함
- libpq 관련 중요 사항: libpq는 명시적 사용자 이름이 지정되지 않으면 SAM 호환 이름 사용. 이 옵션을 비활성화된 상태로 두거나 연결 문자열에서 명시적으로 사용자 이름 지정

#### 4. map
- 기능: 시스템과 데이터베이스 사용자 이름 간의 매핑 허용
- 참조: 자세한 내용은 Section 20.2 (사용자 이름 맵) 참조
- 매핑 동작:
  - `username@EXAMPLE.COM` (또는 `username/hostbased@EXAMPLE.COM`)과 같은 SSPI/Kerberos 주체의 경우 매핑된 사용자 이름은 기본적으로 전체 주체
  - `include_realm`이 0으로 설정되면 `username` (또는 `username/hostbased`)만 매핑에 사용됨

#### 5. krb_realm
- 기능: 사용자 주체 이름과 일치시킬 realm 설정
- 설정된 경우: 지정된 realm의 사용자만 수락됨
- 설정되지 않은 경우: 모든 realm의 사용자가 연결 가능 (사용자 이름 매핑 규칙에 따라)

### 모범 사례 요약

1. `pg_ident.conf`에서 명시적 매핑과 함께 `include_realm`을 기본값 (1)으로 유지
2. 모든 클라이언트와 서버가 도메인 계정을 사용하는 경우에만 `compat_realm` 비활성화
3. libpq와 함께 연결 문자열에서 명시적 사용자 이름 사용
4. 필요시 특정 realm으로 인증을 제한하려면 `krb_realm` 사용

---

## 20.8 Ident 인증

### Ident 인증이란?

Ident 인증은 ident 서버에서 클라이언트의 운영체제 사용자 이름을 가져와 허용된 데이터베이스 사용자 이름으로 사용하는 방법입니다 (선택적 사용자 이름 매핑 사용). TCP/IP 연결에서만 지원됩니다.

#### 중요 참고
로컬 (비-TCP/IP) 연결에 ident가 지정되면 대신 peer 인증 이 사용됩니다 (Section 20.9 참조).

### 설정 옵션

#### `map`
시스템과 데이터베이스 사용자 이름 간의 매핑을 허용합니다. 자세한 내용은 Section 20.2 (사용자 이름 맵) 참조.

### 기술 세부 사항

#### 프로토콜 기반
- RFC 1413 에 설명된 Identification Protocol 기반
- 거의 모든 Unix 계열 운영체제에는 기본적으로 TCP 포트 113 에서 수신하는 ident 서버 포함

#### 작동 방식
ident 서버는 다음과 같은 쿼리에 응답합니다: *"당신의 포트 X에서 나가서 내 포트 Y에 연결하는 연결을 시작한 사용자는 누구입니까?"*

PostgreSQL은 물리적 연결이 설정될 때 포트 X (클라이언트)와 포트 Y (서버) 모두를 알고 있으므로:
1. 클라이언트 호스트의 ident 서버에 질의 가능
2. 이론적으로 주어진 연결에 대한 운영체제 사용자 결정 가능

### 보안 고려 사항

#### 중요한 단점
이 방법은 전적으로 클라이언트 머신 무결성 에 의존합니다. 신뢰할 수 없거나 손상된 클라이언트 머신에서 공격자는:
- 포트 113에서 모든 프로그램을 실행할 수 있음
- 원하는 모든 사용자 이름을 반환할 수 있음

#### 적절한 사용 사례
Ident 인증은 다음 경우에만 적합합니다:
- 클라이언트 머신에 대한 엄격한 제어가 있는 폐쇄형 네트워크
- 데이터베이스 및 시스템 관리자가 긴밀하게 협력하는 환경
- ident 서버를 실행하는 머신을 신뢰해야 함

#### RFC 1413 경고
> *"Identification Protocol은 권한 부여 또는 접근 제어 프로토콜로 의도되지 않았습니다."*

### 중요 보안 경고

#### 암호화 옵션 위험
일부 ident 서버에는 원래 머신 관리자만 아는 키를 사용하여 반환된 사용자 이름을 암호화하는 비표준 옵션이 있습니다.

이 옵션은 PostgreSQL과 함께 사용해서는 안 됩니다 이유:
- PostgreSQL은 반환된 문자열을 해독할 방법이 없음
- 실제 사용자 이름을 결정할 수 없음

---

## 20.9 Peer 인증

### 개요
Peer 인증은 커널에서 클라이언트의 운영체제 사용자 이름을 가져와 허용된 데이터베이스 사용자 이름으로 사용하는 PostgreSQL 인증 방법입니다.

### 주요 특성

적용 가능성:
- 로컬 연결 에서만 지원됨 (원격/TCP 연결 아님)
- 다음에 대한 운영체제 지원 필요:
  - `getpeereid()` 함수
  - `SO_PEERCRED` 소켓 매개변수
  - 또는 유사한 메커니즘

지원되는 운영체제:
- Linux
- 대부분의 BSD 변형 (macOS 포함)
- Solaris

### 설정 옵션

peer 인증 방법은 다음 설정 옵션을 지원합니다:

| 옵션 | 설명 |
|------|------|
| `map` | 시스템과 데이터베이스 사용자 이름 간의 매핑 허용. 자세한 내용은 Section 20.2 (사용자 이름 맵) 참조. |

### 작동 방식

1. 인증 방법이 커널에서 클라이언트의 운영체제 사용자 이름 검색
2. 이 OS 사용자 이름이 데이터베이스 사용자 이름과 일치됨
3. 선택적 사용자 이름 매핑을 적용하여 시스템 사용자와 데이터베이스 사용자 간 변환 가능

### 맥락

Peer 인증은 OS 사용자 신원을 운영체제 수준에서 신뢰할 수 있게 얻을 수 있는 로컬 시스템 연결에 특히 유용합니다.

---

## 20.10 LDAP 인증

### 개요

PostgreSQL의 LDAP 인증은 패스워드 인증과 유사하게 작동하지만 LDAP를 사용하여 패스워드를 확인합니다. 중요: LDAP 인증을 사용하기 전에 사용자가 PostgreSQL 데이터베이스에 이미 존재해야 합니다.

### 두 가지 작동 모드

#### 1. 단순 바인드 모드
서버가 prefix + username + suffix 로 구성된 DN에 바인딩합니다.

- 사용 사례: `cn=` 접두사 또는 `DOMAIN\` (Active Directory)
- 장점: 더 간단한 설정
- 단점: 디렉토리 구조의 유연성이 낮음

#### 2. 검색+바인드 모드
서버가 2단계 프로세스를 수행합니다:
1. 고정 자격 증명(또는 익명)으로 디렉토리에 바인딩
2. 사용자를 검색한 다음 클라이언트의 패스워드로 다시 바인딩

- 장점: 디렉토리에서 더 유연한 사용자 위치
- 단점: 두 개의 추가 LDAP 서버 요청

### 설정 옵션 (두 모드 공통)

| 옵션 | 설명 |
|------|------|
| `ldapserver` | LDAP 서버 이름/IP (공백으로 구분하여 여러 개) |
| `ldapport` | 포트 번호 (생략 시 LDAP 라이브러리 기본값 사용) |
| `ldapscheme` | SSL을 통한 LDAP의 경우 `ldaps`로 설정 |
| `ldaptls` | StartTLS를 사용한 TLS 암호화의 경우 `1`로 설정 (RFC 4513) |

보안 참고: `ldapscheme` 또는 `ldaptls`는 PostgreSQL과 LDAP 간 트래픽만 암호화하며 PostgreSQL 클라이언트와 서버 간 트래픽은 암호화하지 않습니다.

### 단순 바인드 모드 옵션

| 옵션 | 설명 |
|------|------|
| `ldapprefix` | DN 형성을 위해 사용자 이름 앞에 추가되는 문자열 |
| `ldapsuffix` | DN 형성을 위해 사용자 이름 뒤에 추가되는 문자열 |

#### 예제: 단순 바인드

```
host ... ldap ldapserver=ldap.example.net ldapprefix="cn=" ldapsuffix=", dc=example, dc=net"
```

결과: 사용자 `someuser`는 DN: `cn=someuser, dc=example, dc=net`으로 바인딩 시도

#### 예제: 사용자 정의 포트를 사용한 LDAPS

```
host ... ldap ldapurl="ldaps://ldap.example.net:49151" ldapprefix="cn=" ldapsuffix=", dc=example, dc=net"
```

### 검색+바인드 모드 옵션

| 옵션 | 설명 |
|------|------|
| `ldapbasedn` | 사용자 검색을 위한 루트 DN |
| `ldapbinddn` | 검색을 위해 바인딩할 사용자의 DN (익명의 경우 생략) |
| `ldapbindpasswd` | 검색 바인드를 위한 패스워드 |
| `ldapsearchattribute` | 사용자 이름과 일치시킬 속성 (기본값: `uid`) |
| `ldapsearchfilter` | `$username` 자리 표시자를 사용한 사용자 정의 검색 필터 |

#### 예제: 익명 검색+바인드

```
host ... ldap ldapserver=ldap.example.net ldapbasedn="dc=example, dc=net" ldapsearchattribute=uid
```

결과: `dc=example, dc=net` 아래에서 `(uid=someuser)`를 익명으로 검색한 다음 찾은 DN과 클라이언트 패스워드로 바인딩.

#### 예제: URL로서의 검색+바인드

```
host ... ldap ldapurl="ldap://ldap.example.net/dc=example,dc=net?uid?sub"
```

#### 예제: 사용자 정의 검색 필터 (여러 속성)

```
host ... ldap ldapserver=ldap.example.net ldapbasedn="dc=example, dc=net" ldapsearchfilter="(|(uid=$username)(mail=$username))"
```

결과: `uid` 또는 `mail` 속성으로 사용자 인증.

### LDAP URL 형식

RFC 4516 준수 형식:

```
ldap[s]://host[:port]/basedn[?[attribute][?[scope][?[filter]]]]
```

- scope: `base`, `one`, 또는 `sub` (기본값: `base`)
- attribute: 지정된 경우 `ldapsearchattribute`로 사용됨
- filter: 속성이 비어 있으면 `ldapsearchfilter`로 사용됨
- 스킴 `ldaps`: `ldapscheme=ldaps`와 동일

참고: LDAP URL은 OpenLDAP에서만 지원됨 (Windows 아님).

### 고급 기능

#### DNS SRV 검색 (OpenLDAP만)

PostgreSQL이 OpenLDAP로 컴파일되고 `ldapserver`가 생략되면 시스템은 RFC 2782 DNS SRV 조회를 수행합니다. `_ldap._tcp.DOMAIN`에서 DOMAIN은 `ldapbasedn`에서 추출됩니다.

예제:
```
host ... ldap ldapbasedn="dc=example,dc=net"
```

### 설정 규칙

1. 모드 혼합 금지: 단순 바인드와 검색+바인드 옵션을 결합할 수 없음
2. 검색+바인드 기본값: `ldapsearchattribute`도 `ldapsearchfilter`도 지정되지 않으면 기본값은 `ldapsearchattribute=uid`
3. 등가성: `ldapsearchattribute=foo` ≡ `ldapsearchfilter="(foo=$username)"`
4. DN 따옴표 처리: 쉼표/공백을 포함하는 DN에는 큰따옴표 사용

### 다른 모드와의 주요 차이점

`password` 모드와 달리 LDAP는 인증을 외부 LDAP 디렉토리에 위임하지만 여전히 데이터베이스 사용자가 PostgreSQL의 사용자 데이터베이스에 존재해야 합니다.

---

## 20.11 RADIUS 인증

### 개요
PostgreSQL의 RADIUS 인증은 패스워드 인증과 유사하게 작동하지만 RADIUS 서버를 사용하여 패스워드를 확인합니다. RADIUS 인증을 사용하기 전에 사용자가 데이터베이스에 이미 존재해야 합니다.

### 작동 방식

RADIUS 인증이 활성화되면:
1. Access Request 메시지 (`Authenticate Only` 유형)가 설정된 RADIUS 서버로 전송됨
2. 요청에 포함되는 내용:
   - 사용자 이름
   - 패스워드 (암호화됨)
   - NAS 식별자
3. 요청이 서버와의 공유 비밀을 사용하여 암호화됨
4. RADIUS 서버가 `Access Accept` 또는 `Access Reject`로 응답함
5. RADIUS 계정은 지원되지 않음

### 다중 RADIUS 서버

여러 RADIUS 서버를 지정할 수 있으며 순차적으로 시도됩니다:
- 부정적인 응답을 받으면 → 인증 실패
- 응답이 없으면 → 목록의 다음 서버 시도
- 서버를 큰따옴표로 둘러싼 쉼표 구분 목록으로 지정
- 다른 RADIUS 옵션도 쉼표로 구분 가능 (서버별 개별 값) 또는 단일 값 (모든 서버에 적용)

### 설정 옵션

#### `radiusservers` (필수)
- 연결할 RADIUS 서버의 DNS 이름 또는 IP 주소

#### `radiussecrets` (필수)
- RADIUS 서버와의 안전한 통신을 위한 공유 비밀
- PostgreSQL과 RADIUS 서버에서 정확히 일치해야 함
- 권장 최소 길이: 16자
- 참고: 암호화 강도는 OpenSSL 지원에 따라 다름. OpenSSL 없이는 전송이 난독화되지만 보안되지 않음. 필요시 외부 보안 조치 적용.

#### `radiusports` (선택)
- RADIUS 서버에 연결할 포트 번호
- 기본값: `1812` (표준 RADIUS 포트)

#### `radiusidentifiers` (선택)
- RADIUS 요청에서 `NAS Identifier`로 사용되는 문자열
- 사용 사례: 사용자가 연결하는 데이터베이스 클러스터 식별 (RADIUS 서버 정책 일치에 유용)
- 기본값: `postgresql`

### 설정 예제

쉼표나 공백을 포함하는 값의 경우 큰따옴표 사용 (이중 따옴표 처리 필요):

```
host ... radius radiusservers="server1,server2" radiussecrets="""secret one"",""secret two"""
```

이 예제는 공백을 포함하는 별도의 비밀을 가진 두 RADIUS 서버를 설정합니다.

---

## 20.12 인증서 인증

### 개요
인증서 인증은 클라이언트 인증서를 사용하여 사용자 신원을 확인하는 SSL 기반 인증 방법입니다. SSL 연결에서만 사용 가능합니다.

### 주요 특성

- 패스워드 프롬프트 없음: 클라이언트가 유효하고 신뢰할 수 있는 인증서를 제공해야 함
- 인증 메커니즘: 인증서의 `cn` (Common Name) 속성이 요청된 데이터베이스 사용자 이름과 비교됨
- 일치 요구 사항: 인증서의 CN이 데이터베이스 사용자 이름과 일치하면 로그인 허용
- 유연성: 사용자 이름 매핑을 사용하여 CN이 데이터베이스 사용자 이름과 다를 수 있음

### 설정 옵션

#### `map`
- 목적: 시스템과 데이터베이스 사용자 이름 간의 매핑 허용
- 참조: 자세한 설정 지침은 [Section 20.2](사용자-이름-맵) (사용자 이름 맵) 참조

### 중요 참고 사항

#### `clientcert` 옵션과의 관계
`cert` 인증과 함께 `clientcert` 옵션을 사용하는 것은 중복 입니다 이유:
- `cert` 인증은 효과적으로 `clientcert=verify-full`을 사용한 `trust` 인증과 동일함

### 전제 조건
- 서버에서 SSL이 적절히 설정되어야 함
- 클라이언트가 유효하고 신뢰할 수 있는 인증서를 제공해야 함
- SSL 설정 지침은 Section 18.9.2 (OpenSSL 설정) 참조

---

## 20.13 PAM 인증

### 개요

PostgreSQL의 PAM (Pluggable Authentication Modules) 인증은 패스워드 인증과 유사하게 작동하지만 PostgreSQL의 네이티브 패스워드 처리 대신 PAM을 기본 인증 메커니즘으로 사용합니다.

### 주요 특성:

- 기본 PAM 서비스 이름: `postgresql`
- 목적: 사용자 이름/패스워드 쌍을 검증하고 선택적으로 연결된 원격 호스트 이름 또는 IP 주소를 검증
- 전제 조건: PAM 인증을 사용하기 전에 사용자가 PostgreSQL 데이터베이스에 이미 존재해야 함
- 사용자 검증: PAM은 자격 증명 검증에만 사용되며 사용자 생성에는 사용되지 않음

### 설정 옵션

#### 1. pamservice
- 설명: PAM 서비스 이름 지정
- 기본값: `postgresql`
- 사용법: 사용할 사용자 정의 PAM 서비스 설정 지정 가능

#### 2. pam_use_hostname
- 설명: 원격 IP 주소 또는 호스트 이름을 PAM 모듈에 제공할지 결정
- 유효한 값: 0 (기본값) 또는 1
- 기본 동작 (0): IP 주소 사용
- 대안 (1): 대신 해석된 호스트 이름 사용
- 중요 참고: 호스트 이름 해석은 로그인 지연을 초래할 수 있음
- 고려 사항: 대부분의 PAM 설정은 이 정보를 사용하지 않으므로 PAM 설정이 특별히 필요로 하는 경우에만 활성화

### 중요한 제한 사항

#### Shadow 파일 접근 문제

중요 참고: PAM이 `/etc/shadow`를 읽도록 설정되면 PostgreSQL 서버가 비-root 사용자에 의해 시작되어 shadow 패스워드 파일에 접근할 수 없으므로 인증이 실패합니다.

해결 방법: PAM이 다음을 사용하도록 설정된 경우에는 문제 없음:
- LDAP 인증
- 기타 비-shadow 인증 방법

---

## 20.14 BSD 인증

### 개요

BSD 인증은 패스워드 인증과 유사하게 작동하지만 BSD Authentication을 사용하여 사용자 자격 증명을 확인하는 PostgreSQL의 인증 방법입니다. 이 인증 메커니즘은 현재 OpenBSD에서만 사용 가능 합니다.

### 주요 특성

#### 작동 방식
- BSD Authentication 프레임워크를 통해 사용자 이름/패스워드 쌍 인증
- BSD Authentication을 사용하기 전에 사용자의 역할이 데이터베이스에 이미 존재해야 함
- 데이터베이스 역할을 생성하지 않음; 자격 증명만 검증

#### 로그인 클래스 설정
PostgreSQL은 `auth-postgresql` 로그인 유형을 사용하고 `login.conf`에 정의된 경우 `postgresql` 로그인 클래스로 인증을 시도합니다:
- `postgresql` 로그인 클래스가 `login.conf`에 정의되어 있으면 사용됨
- 정의되어 있지 않으면 PostgreSQL은 기본 로그인 클래스로 기본 설정됨

### 전제 조건 및 설정

#### 중요 요구 사항
BSD 인증을 사용하려면 PostgreSQL 사용자 계정 (서버를 실행하는 운영체제 사용자)이 `auth` 그룹에 추가 되어야 합니다.

- `auth` 그룹은 OpenBSD 시스템에 기본적으로 존재함
- 이 그룹의 멤버십 없이는 BSD 인증이 작동하지 않음

### 플랫폼 가용성

- 지원되는 플랫폼: OpenBSD만
- 가용성: 다른 운영체제에서는 사용 불가

### 중요 참고 사항

1. BSD 인증은 자격 증명 검증만을 위해 설계됨
2. 데이터베이스 사용자 역할은 인증 전에 미리 생성되어야 함
3. 설정은 OpenBSD의 `login.conf` 파일을 통해 처리됨
4. PostgreSQL 서버 프로세스는 작동하기 위해 적절한 시스템 그룹 멤버십이 필요함

---

## 20.15 OAuth 인증/권한 부여

### 개요

OAuth 2.0은 제3자 애플리케이션이 보호된 리소스에 대한 제한된 접근을 얻을 수 있게 하는 산업 표준 프레임워크입니다 (RFC 6749). PostgreSQL OAuth 클라이언트 지원은 빌드 프로세스 중에 활성화되어야 합니다.

### 주요 용어

| 용어 | 정의 |
|------|------|
| 리소스 소유자/최종 사용자 | 보호된 리소스를 소유하고 접근을 부여하는 사용자 또는 시스템. OAuth와 함께 psql을 사용할 때 사용자가 리소스 소유자입니다. |
| 클라이언트 | 접근 토큰을 사용하여 보호된 리소스에 접근하는 시스템 (예: psql, libpq 애플리케이션). |
| 리소스 서버 | 보호된 리소스를 호스팅하는 시스템. 연결하는 PostgreSQL 클러스터. |
| 제공자 | OAuth 권한 부여 서버와 클라이언트를 개발하고 관리하는 조직 또는 엔티티. |
| 권한 부여 서버 | 리소스 소유자가 승인을 부여한 후 클라이언트에 접근 토큰 발급. PostgreSQL은 이것을 제공하지 않습니다. |
| 발급자 | OAuth 클라이언트에 대한 신뢰할 수 있는 네임스페이스를 제공하는 권한 부여 서버에 대한 HTTPS URL 식별자. |

### Bearer 토큰

PostgreSQL은 bearer 토큰 (RFC 6750)을 지원합니다—OAuth 2.0과 함께 사용되는 불투명 문자열. 토큰 형식은 구현에 따라 다르며 각 권한 부여 서버에 의해 선택됩니다.

### 설정 옵션

#### `issuer` (필수)
- 다음 중 하나인 HTTPS URL:
  - 권한 부여 서버의 정확한 발급자 식별자 (검색 문서에서), 또는
  - 검색 문서를 직접 가리키는 잘 알려진 URI
- 검색 URL 구성:
  - 기본값: 발급자에 `/.well-known/openid-configuration` 추가
  - 발급자가 `/.well-known/` 경로 세그먼트를 포함하면 해당 URL이 그대로 사용됨

경고: 발급자 설정은 검색 문서의 발급자 식별자 및 클라이언트의 `oauth_issuer` 설정과 정확히 일치해야 합니다. 대소문자나 형식 변형이 허용되지 않습니다.

#### `scope` (필수)
- 다음에 필요한 OAuth 스코프의 공백으로 구분된 목록:
  - 클라이언트의 서버 권한 부여
  - 사용자 인증
- 값은 권한 부여 서버와 OAuth 검증 모듈에 의해 결정됨 (Chapter 50 참조)

#### `validator` (선택/조건부)
- bearer 토큰 검증을 위한 라이브러리 지정
- `oauth_validator_libraries`의 라이브러리와 정확히 일치해야 함
- `oauth_validator_libraries`에 둘 이상의 라이브러리가 포함된 경우 필수
- 그렇지 않으면 선택 사항

#### `map` (선택)
- OAuth ID 제공자와 데이터베이스 사용자 이름 간의 매핑
- 사용자 이름 매핑에 대한 자세한 내용은 Section 20.2 참조
- 지정되지 않으면 토큰의 연결된 사용자 이름이 요청된 역할 이름과 정확히 일치해야 함

#### `delegate_ident_mapping` (고급/선택)
- 표준 `pg_ident.conf` 사용자 매핑을 건너뛰려면 `1`로 설정
- OAuth 검증기가 최종 사용자 신원을 데이터베이스 역할에 매핑하는 전체 책임을 짐
- `map` 옵션과 호환되지 않음

경고: 토큰 권한을 확인하기 위해 신중한 검증기 구현이 필요합니다. 주의해서 사용하세요.

### 중요 참고 사항

- 소규모 배포의 경우 "제공자", "권한 부여 서버", "발급자"가 동의어일 수 있음
- 복잡한 설정에서는 일대다 또는 다대다 관계가 있을 수 있음
- PostgreSQL 구현은 OpenID Connect/OIDC와 상호 운용 가능하도록 의도되었지만 그 자체가 OIDC 클라이언트가 아니며 그 사용을 요구하지 않음

---

## 20.16 인증 문제 해결

### 개요
이 문서 섹션은 PostgreSQL 서버에 연결할 때 일반적인 인증 실패 및 관련 문제를 다룹니다.

### 일반적인 인증 오류 메시지

#### 1. pg_hba.conf 항목 없음
```
FATAL:  no pg_hba.conf entry for host "123.123.123.123", user "andym", database "testdb"
```
의미: 서버에 성공적으로 접속했지만 `pg_hba.conf` 설정 파일에 일치하는 항목이 없어 연결 요청이 거부되었습니다.

해결 방법: 호스트, 사용자, 데이터베이스 조합을 허용하도록 `pg_hba.conf`에 적절한 항목을 설정하세요.

---

#### 2. 패스워드 인증 실패
```
FATAL:  password authentication failed for user "andym"
```
의미: 서버가 연결을 수락하지만 `pg_hba.conf`에 지정된 권한 부여 방법 (이 경우 패스워드 인증)을 통과해야 합니다.

해결 방법:
- 제공하는 패스워드가 올바른지 확인
- 오류에서 해당 인증 유형이 언급된 경우 Kerberos 또는 ident 소프트웨어 확인

---

#### 3. 사용자가 존재하지 않음
```
FATAL:  user "andym" does not exist
```
의미: 지정된 데이터베이스 사용자 이름이 시스템에서 찾을 수 없습니다.

해결 방법: 사용자를 생성하거나 올바른 사용자 이름을 확인하세요.

---

#### 4. 데이터베이스가 존재하지 않음
```
FATAL:  database "testdb" does not exist
```
의미: 연결하려는 데이터베이스가 존재하지 않습니다.

참고: 데이터베이스 이름을 지정하지 않으면 데이터베이스 사용자 이름이 기본값으로 사용됩니다.

---

### 문제 해결 팁

> 서버 로그에는 클라이언트에 보고되는 것보다 인증 실패에 대한 더 많은 정보가 포함될 수 있습니다. 실패 이유가 혼란스러우면 서버 로그를 확인하세요.

이것은 인증 문제의 자세한 디버깅에 매우 중요합니다.

---

## 요약

PostgreSQL의 클라이언트 인증 시스템은 매우 유연하고 다양한 환경에 적응할 수 있도록 설계되었습니다. 주요 포인트:

1. pg_hba.conf 는 누가 어떤 방법으로 연결할 수 있는지를 제어하는 핵심 설정 파일입니다.
2. 사용자 이름 맵 을 통해 외부 시스템 사용자를 데이터베이스 사용자로 매핑할 수 있습니다.
3. 13가지 인증 방법 중에서 환경과 요구 사항에 맞는 것을 선택할 수 있습니다.
4. 보안과 편의성 사이의 균형을 고려하여 적절한 인증 방법을 선택하세요.
5. 문제가 발생하면 서버 로그를 확인하여 자세한 오류 정보를 얻으세요.

---

*이 문서는 PostgreSQL 18 공식 문서의 Chapter 20 "Client Authentication"을 한국어로 번역한 것입니다.*

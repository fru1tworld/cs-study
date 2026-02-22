# RFC 1034 - Domain Names: Concepts and Facilities

## 문서 정보

- RFC 번호: 1034
- 제목: Domain Names - Concepts and Facilities
- 저자: P. Mockapetris (ISI)
- 발행일: 1987년 11월
- 목적: DNS의 기본 개념, 구조, 네임 서버의 역할 정의

---

## 1. 개요

### 1.1 DNS란?

DNS(Domain Name System)는 인터넷의 계층적 분산 명명 시스템입니다. 사람이 읽을 수 있는 도메인 이름을 IP 주소로 변환합니다.

```
www.example.com  →  DNS  →  93.184.216.34
```

### 1.2 DNS의 세 가지 핵심 구성 요소

| 구성 요소 | 설명 |
|----------|------|
| 도메인 네임 스페이스 | 트리 구조의 계층적 이름 체계 |
| 네임 서버 | 도메인 정보를 저장하고 응답하는 서버 |
| 리졸버 | 클라이언트의 질의를 처리하는 소프트웨어 |

### 1.3 DNS 설계 목표

- 일관된 네임스페이스 (네트워크 식별자 미포함)
- 분산 데이터베이스와 로컬 캐싱
- 다양한 애플리케이션에서 범용 사용
- 클래스 구분으로 다른 프로토콜 패밀리 지원
- 기반 통신 시스템과의 독립성

---

## 2. 도메인 네임 스페이스

### 2.1 트리 구조

DNS는 트리 구조 로 구성되어 있습니다:

```
                           (root)
                             ""
                             |
         +----------+--------+--------+----------+
         |          |        |        |          |
        com        net      org      kr        edu
         |          |        |        |          |
     +---+---+      |        |     +--+--+       |
     |       |      |        |     |     |       |
  google  amazon   ...      ...   go   naver   mit
     |       |                           |       |
    www    aws                         www     ...
```

### 2.2 레이블과 도메인 이름

| 용어 | 설명 | 예시 |
|------|------|------|
| 레이블 (Label) | 각 노드의 이름 (0-63 옥텟) | "www", "google", "com" |
| 도메인 이름 | 루트까지의 레이블 나열 | www.google.com. |
| FQDN | Fully Qualified Domain Name (끝에 점 포함) | www.google.com. |

### 2.3 도메인 이름 규칙

```
절대 이름 (Absolute): www.example.com.  (끝에 점)
상대 이름 (Relative): www.example.com   (끝에 점 없음)
```

- 대소문자 무관: example.COM = EXAMPLE.com = example.com
- 최대 길이: 전체 도메인 이름 255 옥텟
- 레이블 최대 길이: 63 옥텟

### 2.4 특수 도메인

| 도메인 | 용도 |
|--------|------|
| IN-ADDR.ARPA | IP → 도메인 역방향 조회 |
| IP6.ARPA | IPv6 역방향 조회 |

#### 역방향 조회 예시

```
IP: 192.168.1.100

역방향 도메인: 100.1.168.192.in-addr.arpa.
              ^   ^   ^   ^
              |   |   |   |
              +---+---+---+-- IP 주소를 역순으로
```

---

## 3. 리소스 레코드 (Resource Records)

### 3.1 리소스 레코드 구조

각 리소스 레코드(RR)는 다음 필드를 포함합니다:

| 필드 | 설명 |
|------|------|
| Owner Name | 레코드 소유자 도메인 이름 |
| Type | 레코드 유형 |
| Class | 프로토콜 패밀리 (보통 IN = Internet) |
| TTL | Time To Live (캐시 유효 시간, 초) |
| RDATA | 타입에 따른 실제 데이터 |

### 3.2 RRset (Resource Record Set)

동일한 Owner Name, Type, Class를 가진 리소스 레코드들의 집합을 RRset 이라고 합니다.

```
예시: example.com의 A 레코드 RRset
example.com.    IN    A    93.184.216.34
example.com.    IN    A    93.184.216.35
example.com.    IN    A    93.184.216.36
```

RRset 특징:

| 특징 | 설명 |
|------|------|
| 동일한 TTL | 같은 RRset 내 모든 레코드는 동일한 TTL을 가져야 함 |
| 원자적 처리 | RRset은 하나의 단위로 캐시되고 반환됨 |
| 순서 무관 | RRset 내 레코드의 순서는 의미 없음 (라운드 로빈 가능) |

```
# 잘못된 예시 (TTL이 다름)
example.com.  3600  IN  A  93.184.216.34
example.com.  7200  IN  A  93.184.216.35  ← 잘못됨!

# 올바른 예시 (TTL이 동일)
example.com.  3600  IN  A  93.184.216.34
example.com.  3600  IN  A  93.184.216.35  ← 올바름
```

### 3.3 주요 레코드 타입

| Type | 이름 | 설명 | RDATA 예시 |
|------|------|------|------------|
| A | Address | IPv4 주소 | 192.168.1.1 |
| AAAA | IPv6 Address | IPv6 주소 | 2001:db8::1 |
| CNAME | Canonical Name | 별칭 (다른 이름을 가리킴) | www → webserver.example.com |
| MX | Mail Exchange | 메일 서버 | 10 mail.example.com |
| NS | Name Server | 권한 있는 네임 서버 | ns1.example.com |
| PTR | Pointer | 역방향 조회용 | server.example.com |
| SOA | Start of Authority | 존 권한 정보 | (아래 설명) |
| TXT | Text | 텍스트 정보 | "v=spf1 ..." |

### 3.4 A 레코드 (Address Record)

```
www.example.com.    IN    A    93.184.216.34
     ^               ^    ^         ^
     |               |    |         |
   Owner          Class  Type    RDATA(IP주소)
```

### 3.5 CNAME 레코드 (Canonical Name)

CNAME은 별칭(alias)을 만듭니다:

```
# blog.example.com은 www.example.com의 별칭
blog.example.com.   IN    CNAME   www.example.com.

# 조회 과정
blog.example.com → CNAME → www.example.com → A → 93.184.216.34
```

CNAME 규칙:
- CNAME이 있는 노드에는 다른 레코드가 있으면 안 됨
- CNAME 체인을 따라가서 최종 A 레코드를 찾음

### 3.6 MX 레코드 (Mail Exchange)

```
example.com.    IN    MX    10    mail1.example.com.
example.com.    IN    MX    20    mail2.example.com.
                            ^
                            |
                        우선순위 (낮을수록 우선)
```

### 3.7 NS 레코드 (Name Server)

```
example.com.    IN    NS    ns1.example.com.
example.com.    IN    NS    ns2.example.com.
```

### 3.8 SOA 레코드 (Start of Authority)

존(Zone)의 권한 정보를 담습니다:

```
example.com.    IN    SOA    ns1.example.com. admin.example.com. (
                              2024010101  ; SERIAL   - 존 버전 번호
                              3600        ; REFRESH  - 보조 서버 갱신 주기
                              900         ; RETRY    - 갱신 실패 시 재시도
                              604800      ; EXPIRE   - 존 만료 시간
                              86400       ; MINIMUM  - 부정 캐싱 TTL
                            )
```

| 필드 | 설명 |
|------|------|
| MNAME | 주 네임 서버 |
| RNAME | 관리자 이메일 (@ → .) |
| SERIAL | 존 버전 (변경 시 증가) |
| REFRESH | 보조 서버가 주 서버 확인 주기 |
| RETRY | REFRESH 실패 시 재시도 간격 |
| EXPIRE | 보조 서버 데이터 만료 시간 |
| MINIMUM | 부정 응답 캐시 TTL |

---

## 4. 존 (Zone)

### 4.1 존이란?

존(Zone)은 네임 스페이스의 연속된 부분으로, 하나의 권한으로 관리됩니다.

```
도메인 (Domain) ≠ 존 (Zone)

example.com 도메인은 여러 존으로 나뉠 수 있음:
- example.com 존
- sub.example.com 존 (위임됨)
```

### 4.2 존의 구성

```
example.com 존 내용:
┌─────────────────────────────────────────────────────────────┐
│ example.com.      SOA    ns1.example.com. admin...          │
│ example.com.      NS     ns1.example.com.                   │
│ example.com.      NS     ns2.example.com.                   │
│ example.com.      MX     10 mail.example.com.               │
│ example.com.      A      93.184.216.34                      │
│ www.example.com.  A      93.184.216.34                      │
│ mail.example.com. A      93.184.216.35                      │
│ ns1.example.com.  A      93.184.216.36                      │
│ ns2.example.com.  A      93.184.216.37                      │
│                                                             │
│ # sub.example.com은 다른 존으로 위임                        │
│ sub.example.com.  NS     ns1.sub.example.com.               │
│ ns1.sub.example.com. A   198.51.100.1  ← 글루 레코드        │
└─────────────────────────────────────────────────────────────┘
```

### 4.3 존 위임 (Delegation)

```
                    example.com 존
                         |
          +--------------+--------------+
          |                             |
     www.example.com              sub.example.com 존
     (이 존에 포함)                     (위임됨)
                                        |
                                  app.sub.example.com
                                  (위임된 존에 포함)
```

### 4.4 글루 레코드 (Glue Records)

위임된 존의 네임 서버 IP를 제공합니다:

```
# sub.example.com의 NS가 ns1.sub.example.com인 경우
# ns1.sub.example.com의 IP를 알려면 sub.example.com 존을 조회해야 함
# 하지만 그 존의 NS IP를 모름 → 순환 의존성!

# 해결: 상위 존에 글루 레코드 추가
sub.example.com.       NS     ns1.sub.example.com.
ns1.sub.example.com.   A      198.51.100.1  ← 글루 레코드
```

---

## 5. 네임 서버

### 5.1 네임 서버 유형

| 유형 | 설명 |
|------|------|
| 권한 서버 (Authoritative) | 특정 존의 공식 데이터 보유 |
| 주 서버 (Primary/Master) | 존 데이터의 원본 보유 |
| 보조 서버 (Secondary/Slave) | 주 서버에서 복제한 데이터 보유 |
| 캐싱 서버 | 다른 서버 응답을 캐시 |

### 5.2 쿼리 처리 모드

#### 반복 쿼리 (Iterative Query)

```
클라이언트                 서버
    |                       |
    |-- 쿼리 -------------->|
    |<-- 다른 서버 참조 ----|
    |                       |
    (클라이언트가 다른 서버에 직접 쿼리)
```

#### 재귀 쿼리 (Recursive Query)

```
클라이언트                 서버                    다른 서버들
    |                       |                          |
    |-- 재귀 쿼리 --------->|                          |
    |                       |-- 반복 쿼리 ------------>|
    |                       |<-- 응답/참조 ------------|
    |                       |-- 반복 쿼리 ------------>|
    |                       |<-- 최종 응답 ------------|
    |<-- 최종 응답 ---------|                          |
```

### 5.3 RD (Recursion Desired) / RA (Recursion Available) 비트

DNS 메시지 헤더에는 재귀 처리와 관련된 두 가지 중요한 플래그 비트가 있습니다:

#### RD (Recursion Desired) 비트

```
┌────────────────────────────────────────────────────────────────┐
│                       DNS 헤더 (12 bytes)                       │
├────────────────────────────────────────────────────────────────┤
│  ID   │  QR  OPCODE  AA  TC  RD  RA  Z  AD  CD  │    RCODE     │
│       │                         ^                              │
│       │                         |                              │
│       │                    RD 비트                             │
└────────────────────────────────────────────────────────────────┘
```

| 값 | 의미 |
|----|------|
| RD = 1 | 재귀 질의 요청 (클라이언트가 재귀 해석을 원함) |
| RD = 0 | 반복 질의 요청 (참조 응답만 받겠다는 의미) |

```
RD = 1 설정 시:
클라이언트 → 서버: "www.example.com 주소 알려줘. 끝까지 찾아줘!"

RD = 0 설정 시:
클라이언트 → 서버: "www.example.com 주소 알려줘. 모르면 힌트만 줘."
```

#### RA (Recursion Available) 비트

| 값 | 의미 |
|----|------|
| RA = 1 | 서버가 재귀 질의를 지원함 |
| RA = 0 | 서버가 재귀 질의를 지원하지 않음 (권한 서버 등) |

#### RD/RA 비트 조합

| RD | RA | 상황 | 결과 |
|----|----|------|------|
| 1 | 1 | 클라이언트가 재귀 요청, 서버 지원 | 재귀 해석 수행 |
| 1 | 0 | 클라이언트가 재귀 요청, 서버 미지원 | 참조 응답만 반환 |
| 0 | 1 | 클라이언트가 반복 요청, 서버 지원 | 참조 응답만 반환 |
| 0 | 0 | 클라이언트가 반복 요청, 서버 미지원 | 참조 응답만 반환 |

```bash
# dig 명령어로 RD 비트 확인
$ dig www.example.com

;; flags: qr rd ra; QUERY: 1, ANSWER: 1, ...
            ^  ^
            |  |
            |  +-- RA: 서버가 재귀 지원
            +-- RD: 재귀 요청됨

# RD 비트를 끄고 질의 (+norecurse)
$ dig +norecurse www.example.com @a.root-servers.net

;; flags: qr; QUERY: 1, ANSWER: 0, AUTHORITY: 13, ...
         ^
         |
         +-- RD 없음: 참조 응답 (AUTHORITY 섹션에 NS 레코드)
```

### 5.4 존 전송 (Zone Transfer)

존 전송은 주 서버(Primary)에서 보조 서버(Secondary)로 존 데이터를 복제하는 메커니즘입니다.

#### 존 전송이 필요한 이유

```
┌─────────────┐                    ┌─────────────┐
│  주 서버     │  ---- 존 전송 ---> │  보조 서버   │
│  (Primary)  │                    │ (Secondary) │
│             │                    │             │
│ 원본 데이터  │                    │ 복제 데이터  │
└─────────────┘                    └─────────────┘
       ↑                                  ↑
       |                                  |
    관리자가                           자동으로
    직접 수정                          동기화됨
```

- 가용성: 주 서버 장애 시에도 서비스 지속
- 부하 분산: 여러 서버가 질의를 분산 처리
- 지리적 분산: 사용자와 가까운 서버에서 응답

#### 존 전송 과정

```
1. 보조 서버가 주 서버의 SOA 레코드 확인 (REFRESH 주기마다)
2. SERIAL 번호 비교
3. SERIAL이 증가했으면 존 전송 요청 (AXFR)
4. 전체 존 데이터 수신
5. 로컬 데이터 갱신
```

```
보조 서버                              주 서버
    |                                    |
    |-- SOA 질의 ----------------------->|
    |<-- SOA 응답 (SERIAL: 2024010102) --|
    |                                    |
    | [비교: 로컬 SERIAL < 서버 SERIAL]  |
    |                                    |
    |-- AXFR 요청 ---------------------->|
    |<-- 존 데이터 전송 -----------------|
    |<-- ... (레코드들) ... -------------|
    |<-- 전송 완료 ----------------------|
    |                                    |
```

#### AXFR vs IXFR

| 전송 유형 | 설명 | 장점 | 단점 |
|----------|------|------|------|
| AXFR | 전체 존 전송 | 구현 단순, 항상 일관성 보장 | 대역폭 소모 큼 |
| IXFR | 증분 존 전송 (변경분만) | 대역폭 효율적 | 복잡, 히스토리 필요 |

```bash
# AXFR 존 전송 테스트 (dig 사용)
$ dig @ns1.example.com example.com AXFR

# 출력 예시
; <<>> DiG 9.16.1 <<>> @ns1.example.com example.com AXFR
example.com.        3600    IN    SOA    ns1.example.com. admin.example.com. ...
example.com.        3600    IN    NS     ns1.example.com.
example.com.        3600    IN    NS     ns2.example.com.
example.com.        3600    IN    A      93.184.216.34
www.example.com.    3600    IN    A      93.184.216.34
example.com.        3600    IN    SOA    ns1.example.com. admin.example.com. ...
```

#### SOA 레코드와 존 전송

```
example.com.    IN    SOA    ns1.example.com. admin.example.com. (
                              2024010101  ; SERIAL   - 변경 시마다 증가
                              3600        ; REFRESH  - 보조 서버 확인 주기 (1시간)
                              900         ; RETRY    - 실패 시 재시도 (15분)
                              604800      ; EXPIRE   - 만료 시간 (7일)
                              86400       ; MINIMUM  - 네거티브 캐싱 TTL
                            )
```

| SOA 필드 | 존 전송에서의 역할 |
|----------|------------------|
| SERIAL | 존 버전 식별, 변경 감지 기준 |
| REFRESH | 보조 서버가 주 서버 확인하는 주기 |
| RETRY | REFRESH 실패 시 재시도 간격 |
| EXPIRE | 이 시간 동안 주 서버 연결 불가 시 존 데이터 폐기 |

#### 존 전송 보안

존 전송은 보안 위험이 있어 제한이 필요합니다:

```
# BIND 설정 예시 - 특정 IP만 존 전송 허용
zone "example.com" {
    type master;
    file "example.com.zone";
    allow-transfer { 192.168.1.10; 192.168.1.11; };
};
```

| 보안 방법 | 설명 |
|----------|------|
| IP 제한 | 특정 IP에서만 AXFR 허용 |
| TSIG | 공유 비밀 키로 서명/검증 |
| TLS | DNS over TLS로 암호화 (최신)

---

## 6. 리졸버 (Resolver)

### 6.1 리졸버 역할

리졸버는 사용자 프로그램과 네임 서버 사이의 인터페이스입니다:

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│ 응용 프로그램 │ --> │   리졸버     │ --> │  네임 서버   │
│   (브라우저)  │ <-- │ (OS 내장)    │ <-- │             │
└─────────────┘     └──────────────┘     └─────────────┘
```

### 6.2 Stub Resolver vs Full Resolver

리졸버는 기능에 따라 두 가지 유형으로 구분됩니다:

#### Stub Resolver (스텁 리졸버)

```
┌─────────────┐     ┌──────────────┐     ┌─────────────────┐     ┌────────────┐
│ 응용 프로그램 │ --> │ Stub Resolver│ --> │  재귀 DNS 서버   │ --> │ 권한 서버들 │
│             │ <-- │  (OS 내장)   │ <-- │ (Full Resolver) │ <-- │            │
└─────────────┘     └──────────────┘     └─────────────────┘     └────────────┘
```

| 특징 | 설명 |
|------|------|
| 위치 | 클라이언트 OS에 내장 |
| 기능 | 최소한의 DNS 처리만 수행 |
| 캐싱 | 없거나 매우 제한적 |
| 재귀 질의 | 항상 재귀 DNS 서버에 의존 |
| 설정 | /etc/resolv.conf (Linux), DNS 설정 (Windows) |

```bash
# Linux에서 Stub Resolver 설정 확인
$ cat /etc/resolv.conf
nameserver 8.8.8.8
nameserver 8.8.4.4
```

#### Full Resolver (풀 리졸버 / 재귀 리졸버)

```
┌──────────────────────────────────────────────────────────────────┐
│                      Full Resolver                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │  캐시 관리   │  │  반복 질의   │  │  응답 검증   │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
└──────────────────────────────────────────────────────────────────┘
         ↕                ↕                 ↕
    로컬 캐시        루트 서버           권한 서버들
                     TLD 서버
```

| 특징 | 설명 |
|------|------|
| 위치 | ISP, 기업, 공용 DNS 서버 (8.8.8.8, 1.1.1.1) |
| 기능 | 완전한 이름 해석 수행 |
| 캐싱 | 대규모 캐시 유지 |
| 반복 질의 | 루트부터 권한 서버까지 직접 질의 |
| 예시 | BIND, Unbound, PowerDNS Recursor |

#### 비교 요약

| 항목 | Stub Resolver | Full Resolver |
|------|--------------|---------------|
| 복잡도 | 단순 | 복잡 |
| 캐시 | 없음/제한적 | 대규모 |
| 독립성 | 재귀 서버 의존 | 독립적 동작 |
| RD 비트 | 항상 1로 설정 | 수신 후 반복 질의 수행 |
| 사용 사례 | 일반 클라이언트 | DNS 서버 |

### 6.3 리졸버의 세 가지 기능

| 기능 | 설명 |
|------|------|
| 호스트→주소 변환 | 도메인 이름 → IP 주소 (A 레코드) |
| 주소→호스트 변환 | IP 주소 → 도메인 이름 (PTR 레코드) |
| 일반 조회 | 임의의 RR 타입 조회 |

### 6.4 리졸버 동작 과정

```
1. 로컬 캐시 확인
2. 캐시에 없으면 재귀 DNS 서버에 쿼리
3. 응답 수신 및 캐시 저장
4. 결과 반환
```

### 6.5 이름 해석 실패 처리

| 오류 유형 | 설명 |
|----------|------|
| NXDOMAIN | 도메인이 존재하지 않음 (영구적) |
| SERVFAIL | 서버 오류 (일시적) |
| TIMEOUT | 응답 없음 (일시적) |

리졸버는 일시적 오류와 영구적 오류를 구분하여 처리합니다.

### 6.6 NXDOMAIN vs NODATA

DNS 질의에서 "찾을 수 없음" 응답에는 두 가지 유형이 있습니다:

#### NXDOMAIN (Non-Existent Domain)

도메인 이름 자체가 존재하지 않는 경우입니다.

```
질의: nonexistent.example.com A

응답:
  - RCODE: NXDOMAIN (3)
  - 의미: "이 도메인 이름은 존재하지 않습니다"
```

#### NODATA

도메인은 존재하지만, 요청한 레코드 타입이 없는 경우입니다.

```
질의: example.com AAAA

응답 (example.com에 A 레코드만 있고 AAAA가 없는 경우):
  - RCODE: NOERROR (0)
  - Answer Section: 비어 있음
  - 의미: "도메인은 있지만 AAAA 레코드는 없습니다"
```

#### 비교 표

| 항목 | NXDOMAIN | NODATA |
|------|----------|--------|
| RCODE | 3 (NXDOMAIN) | 0 (NOERROR) |
| 도메인 존재 | 존재하지 않음 | 존재함 |
| 레코드 존재 | 해당 없음 | 요청한 타입 없음 |
| Answer 섹션 | 비어 있음 | 비어 있음 |
| 예시 | xyz.example.com A | example.com AAAA |

```
예시 시나리오:

존 파일:
  example.com.      A     93.184.216.34
  www.example.com.  A     93.184.216.35

질의 결과:
  - example.com A        → 정상 응답 (93.184.216.34)
  - example.com AAAA     → NODATA (도메인 있음, AAAA 없음)
  - xyz.example.com A    → NXDOMAIN (도메인 자체가 없음)
  - xyz.example.com AAAA → NXDOMAIN (도메인 자체가 없음)
```

---

## 7. DNS 쿼리 과정

### 7.1 전체 해석 과정 예시

```
www.example.com 조회 과정:

                      ┌─────────────┐
                      │   루트 서버  │
                      │      .      │
                      └──────┬──────┘
                             │
                             ↓
                      ┌─────────────┐
                      │  .com 서버  │
                      └──────┬──────┘
                             │
                             ↓
                      ┌──────────────────┐
                      │ example.com 서버 │
                      └──────┬───────────┘
                             │
                             ↓
                         IP 주소
```

### 7.2 상세 과정

```
1. 클라이언트 → 로컬 리졸버: www.example.com 조회
2. 리졸버 → 루트 서버: www.example.com?
3. 루트 서버 → 리졸버: .com 서버들 참조 (NS 레코드)
4. 리졸버 → .com 서버: www.example.com?
5. .com 서버 → 리졸버: example.com 서버들 참조 (NS 레코드)
6. 리졸버 → example.com 서버: www.example.com?
7. example.com 서버 → 리졸버: A 레코드 (93.184.216.34)
8. 리졸버 → 클라이언트: 93.184.216.34
```

---

## 8. 와일드카드 (Wildcards)

### 8.1 와일드카드 레코드

`*` 레이블을 사용하여 존재하지 않는 이름에 대해 레코드를 합성합니다:

```
*.example.com.    IN    A    93.184.216.34
```

### 8.2 와일드카드 매칭

```
레코드: *.example.com.  A  93.184.216.34

매칭되는 쿼리:
- foo.example.com      → 93.184.216.34 (합성)
- bar.example.com      → 93.184.216.34 (합성)
- anything.example.com → 93.184.216.34 (합성)

매칭되지 않는 쿼리:
- www.example.com      → (www에 대한 레코드가 있으면 그것 사용)
- sub.foo.example.com  → (2단계 이상은 매칭 안됨)
```

---

## 9. 캐싱

### 9.1 TTL (Time To Live)

각 레코드는 TTL 값을 가지며, 캐시 유효 시간을 지정합니다:

```
www.example.com.  3600  IN  A  93.184.216.34
                   ^
                   |
               TTL = 3600초 (1시간)
```

### 9.2 캐싱 전략

| 캐싱 유형 | 설명 |
|----------|------|
| 긍정 캐싱 | 성공한 응답 캐시 |
| 부정 캐싱 | NXDOMAIN 응답 캐시 |

### 9.3 네거티브 캐싱 (Negative Caching)

네거티브 캐싱은 부정적인 응답(NXDOMAIN, NODATA)을 캐시하는 메커니즘입니다.

#### 네거티브 캐싱이 필요한 이유

```
네거티브 캐싱이 없는 경우:
┌──────────┐     ┌──────────┐     ┌──────────┐
│  Client  │ --> │ Resolver │ --> │ Auth NS  │
│          │     │          │     │          │
│ xyz.com? │     │ 캐시없음  │     │ NXDOMAIN │
└──────────┘     └──────────┘     └──────────┘
     ↓                                 ↓
  1초 후...                         매번 질의
     ↓                                 ↓
┌──────────┐     ┌──────────┐     ┌──────────┐
│  Client  │ --> │ Resolver │ --> │ Auth NS  │
│ xyz.com? │     │ 캐시없음  │     │ NXDOMAIN │
└──────────┘     └──────────┘     └──────────┘
```

- 불필요한 트래픽 감소: 존재하지 않는 도메인에 대한 반복 질의 방지
- 권한 서버 보호: 잘못된 질의로 인한 서버 부하 감소
- 응답 시간 개선: 캐시된 부정 응답으로 빠른 응답

#### 네거티브 캐싱 대상

| 응답 유형 | 캐시 여부 | TTL 결정 방법 |
|----------|----------|--------------|
| NXDOMAIN | 캐시됨 | SOA MINIMUM 필드 |
| NODATA | 캐시됨 | SOA MINIMUM 필드 |
| SERVFAIL | 일반적으로 캐시 안 함 | (일시적 오류) |
| REFUSED | 캐시 안 함 | (정책적 거부) |

#### 네거티브 캐싱 TTL

네거티브 응답의 TTL은 SOA 레코드의 MINIMUM 필드에서 결정됩니다:

```
example.com.    IN    SOA    ns1.example.com. admin.example.com. (
                              2024010101  ; SERIAL
                              3600        ; REFRESH
                              900         ; RETRY
                              604800      ; EXPIRE
                              86400       ; MINIMUM ← 네거티브 캐싱 TTL (24시간)
                            )
```

RFC 2308에 따르면:
- 네거티브 캐싱 TTL = min(SOA의 TTL, SOA MINIMUM 필드)
- 최대 3시간(10800초)으로 제한하는 것을 권장

#### 네거티브 캐싱 동작 예시

```
1. 클라이언트가 nonexistent.example.com 질의
2. 권한 서버가 NXDOMAIN 응답 (+ SOA 레코드)
3. 리졸버가 NXDOMAIN 응답 캐시 (TTL: SOA MINIMUM)
4. 동일 질의가 오면 캐시된 NXDOMAIN 반환

시간 흐름:
[0초] 질의 → 권한 서버 → NXDOMAIN (캐시 저장, TTL=3600)
[10초] 동일 질의 → 캐시 NXDOMAIN 반환
[100초] 동일 질의 → 캐시 NXDOMAIN 반환
...
[3600초] 캐시 만료 → 다시 권한 서버 질의
```

#### 네거티브 캐싱 확인 (dig)

```bash
# NXDOMAIN 응답 확인
$ dig nonexistent.example.com

;; Got answer:
;; ->>HEADER<<- opcode: QUERY, status: NXDOMAIN, id: 12345
                              ^^^^^^^^
                              도메인 없음

;; AUTHORITY SECTION:
example.com.   3600   IN   SOA   ns1.example.com. admin.example.com. ...
                ^
                |
           네거티브 캐싱 TTL로 사용
```

### 9.4 TTL 설정 고려사항

| TTL | 장점 | 단점 |
|-----|------|------|
| 짧음 | 변경 빠르게 전파 | DNS 트래픽 증가 |
| 김 | DNS 트래픽 감소 | 변경 전파 느림 |

---

## 10. 실제 DNS 조회 명령어

### 10.1 dig 명령어

```bash
# A 레코드 조회
$ dig www.example.com A

# MX 레코드 조회
$ dig example.com MX

# 모든 레코드 조회
$ dig example.com ANY

# 특정 DNS 서버 사용
$ dig @8.8.8.8 www.example.com

# 역방향 조회
$ dig -x 93.184.216.34

# 추적 모드 (전체 해석 과정 표시)
$ dig +trace www.example.com
```

### 10.2 nslookup 명령어

```bash
# 기본 조회
$ nslookup www.example.com

# 레코드 타입 지정
$ nslookup -type=MX example.com

# 특정 DNS 서버 사용
$ nslookup www.example.com 8.8.8.8
```

---

## 11. DNS 계층 구조

### 11.1 루트 서버

전 세계에 13개 루트 서버 클러스터가 있습니다 (A-M):

| 서버 | 운영 기관 |
|------|----------|
| A | Verisign |
| B | USC-ISI |
| C | Cogent |
| D | University of Maryland |
| E | NASA |
| F | Internet Systems Consortium |
| G | US DoD NIC |
| H | US Army |
| I | Netnod |
| J | Verisign |
| K | RIPE NCC |
| L | ICANN |
| M | WIDE Project |

### 11.2 TLD (Top-Level Domain)

| 유형 | 예시 |
|------|------|
| gTLD (일반) | .com, .net, .org, .info |
| ccTLD (국가) | .kr, .jp, .uk, .de |
| 새 gTLD | .app, .dev, .blog |
| 인프라 | .arpa |

---

## 12. 요약

| 항목 | 내용 |
|------|------|
| 목적 | 도메인 이름 ↔ IP 주소 변환 |
| 구조 | 트리 형태의 계층적 분산 시스템 |
| 핵심 구성요소 | 네임 스페이스, 네임 서버, 리졸버 |
| 데이터 단위 | 리소스 레코드 (RR) |
| 관리 단위 | 존 (Zone) |
| 확장성 | 위임을 통한 분산 관리 |
| 성능 | TTL 기반 캐싱 |

---

## 참고 자료

- RFC 1034: https://www.rfc-editor.org/rfc/rfc1034
- RFC 1035: Domain Names - Implementation and Specification
- RFC 2181: Clarifications to the DNS Specification
- RFC 4033-4035: DNSSEC

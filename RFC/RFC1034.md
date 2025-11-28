# RFC 1034 - Domain Names: Concepts and Facilities

## 문서 정보

- **RFC 번호**: 1034
- **제목**: Domain Names - Concepts and Facilities
- **저자**: P. Mockapetris (ISI)
- **발행일**: 1987년 11월
- **목적**: DNS의 기본 개념, 구조, 네임 서버의 역할 정의

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
| **도메인 네임 스페이스** | 트리 구조의 계층적 이름 체계 |
| **네임 서버** | 도메인 정보를 저장하고 응답하는 서버 |
| **리졸버** | 클라이언트의 질의를 처리하는 소프트웨어 |

### 1.3 DNS 설계 목표

- 일관된 네임스페이스 (네트워크 식별자 미포함)
- 분산 데이터베이스와 로컬 캐싱
- 다양한 애플리케이션에서 범용 사용
- 클래스 구분으로 다른 프로토콜 패밀리 지원
- 기반 통신 시스템과의 독립성

---

## 2. 도메인 네임 스페이스

### 2.1 트리 구조

DNS는 **트리 구조**로 구성되어 있습니다:

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
| **레이블 (Label)** | 각 노드의 이름 (0-63 옥텟) | "www", "google", "com" |
| **도메인 이름** | 루트까지의 레이블 나열 | www.google.com. |
| **FQDN** | Fully Qualified Domain Name (끝에 점 포함) | www.google.com. |

### 2.3 도메인 이름 규칙

```
절대 이름 (Absolute): www.example.com.  (끝에 점)
상대 이름 (Relative): www.example.com   (끝에 점 없음)
```

- **대소문자 무관**: example.COM = EXAMPLE.com = example.com
- **최대 길이**: 전체 도메인 이름 255 옥텟
- **레이블 최대 길이**: 63 옥텟

### 2.4 특수 도메인

| 도메인 | 용도 |
|--------|------|
| **IN-ADDR.ARPA** | IP → 도메인 역방향 조회 |
| **IP6.ARPA** | IPv6 역방향 조회 |

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
| **Owner Name** | 레코드 소유자 도메인 이름 |
| **Type** | 레코드 유형 |
| **Class** | 프로토콜 패밀리 (보통 IN = Internet) |
| **TTL** | Time To Live (캐시 유효 시간, 초) |
| **RDATA** | 타입에 따른 실제 데이터 |

### 3.2 주요 레코드 타입

| Type | 이름 | 설명 | RDATA 예시 |
|------|------|------|------------|
| **A** | Address | IPv4 주소 | 192.168.1.1 |
| **AAAA** | IPv6 Address | IPv6 주소 | 2001:db8::1 |
| **CNAME** | Canonical Name | 별칭 (다른 이름을 가리킴) | www → webserver.example.com |
| **MX** | Mail Exchange | 메일 서버 | 10 mail.example.com |
| **NS** | Name Server | 권한 있는 네임 서버 | ns1.example.com |
| **PTR** | Pointer | 역방향 조회용 | server.example.com |
| **SOA** | Start of Authority | 존 권한 정보 | (아래 설명) |
| **TXT** | Text | 텍스트 정보 | "v=spf1 ..." |

### 3.3 A 레코드 (Address Record)

```
www.example.com.    IN    A    93.184.216.34
     ^               ^    ^         ^
     |               |    |         |
   Owner          Class  Type    RDATA(IP주소)
```

### 3.4 CNAME 레코드 (Canonical Name)

CNAME은 별칭(alias)을 만듭니다:

```
# blog.example.com은 www.example.com의 별칭
blog.example.com.   IN    CNAME   www.example.com.

# 조회 과정
blog.example.com → CNAME → www.example.com → A → 93.184.216.34
```

**CNAME 규칙**:
- CNAME이 있는 노드에는 다른 레코드가 있으면 안 됨
- CNAME 체인을 따라가서 최종 A 레코드를 찾음

### 3.5 MX 레코드 (Mail Exchange)

```
example.com.    IN    MX    10    mail1.example.com.
example.com.    IN    MX    20    mail2.example.com.
                            ^
                            |
                        우선순위 (낮을수록 우선)
```

### 3.6 NS 레코드 (Name Server)

```
example.com.    IN    NS    ns1.example.com.
example.com.    IN    NS    ns2.example.com.
```

### 3.7 SOA 레코드 (Start of Authority)

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
| **MNAME** | 주 네임 서버 |
| **RNAME** | 관리자 이메일 (@ → .) |
| **SERIAL** | 존 버전 (변경 시 증가) |
| **REFRESH** | 보조 서버가 주 서버 확인 주기 |
| **RETRY** | REFRESH 실패 시 재시도 간격 |
| **EXPIRE** | 보조 서버 데이터 만료 시간 |
| **MINIMUM** | 부정 응답 캐시 TTL |

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
| **권한 서버 (Authoritative)** | 특정 존의 공식 데이터 보유 |
| **주 서버 (Primary/Master)** | 존 데이터의 원본 보유 |
| **보조 서버 (Secondary/Slave)** | 주 서버에서 복제한 데이터 보유 |
| **캐싱 서버** | 다른 서버 응답을 캐시 |

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

### 5.3 존 전송 (Zone Transfer)

보조 서버가 주 서버로부터 존 데이터를 복제하는 과정:

```
1. 보조 서버가 주 서버의 SOA 레코드 확인 (REFRESH 주기마다)
2. SERIAL 번호 비교
3. SERIAL이 증가했으면 존 전송 요청 (AXFR)
4. 전체 존 데이터 수신
5. 로컬 데이터 갱신
```

| 전송 유형 | 설명 |
|----------|------|
| **AXFR** | 전체 존 전송 |
| **IXFR** | 증분 존 전송 (변경된 부분만) |

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

### 6.2 리졸버의 세 가지 기능

| 기능 | 설명 |
|------|------|
| **호스트→주소 변환** | 도메인 이름 → IP 주소 (A 레코드) |
| **주소→호스트 변환** | IP 주소 → 도메인 이름 (PTR 레코드) |
| **일반 조회** | 임의의 RR 타입 조회 |

### 6.3 리졸버 동작 과정

```
1. 로컬 캐시 확인
2. 캐시에 없으면 재귀 DNS 서버에 쿼리
3. 응답 수신 및 캐시 저장
4. 결과 반환
```

### 6.4 이름 해석 실패 처리

| 오류 유형 | 설명 |
|----------|------|
| **NXDOMAIN** | 도메인이 존재하지 않음 (영구적) |
| **SERVFAIL** | 서버 오류 (일시적) |
| **TIMEOUT** | 응답 없음 (일시적) |

리졸버는 일시적 오류와 영구적 오류를 구분하여 처리합니다.

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
| **긍정 캐싱** | 성공한 응답 캐시 |
| **부정 캐싱** | NXDOMAIN 응답 캐시 |

### 9.3 TTL 설정 고려사항

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
| **gTLD** (일반) | .com, .net, .org, .info |
| **ccTLD** (국가) | .kr, .jp, .uk, .de |
| **새 gTLD** | .app, .dev, .blog |
| **인프라** | .arpa |

---

## 12. 요약

| 항목 | 내용 |
|------|------|
| **목적** | 도메인 이름 ↔ IP 주소 변환 |
| **구조** | 트리 형태의 계층적 분산 시스템 |
| **핵심 구성요소** | 네임 스페이스, 네임 서버, 리졸버 |
| **데이터 단위** | 리소스 레코드 (RR) |
| **관리 단위** | 존 (Zone) |
| **확장성** | 위임을 통한 분산 관리 |
| **성능** | TTL 기반 캐싱 |

---

## 참고 자료

- RFC 1034: https://www.rfc-editor.org/rfc/rfc1034
- RFC 1035: Domain Names - Implementation and Specification
- RFC 2181: Clarifications to the DNS Specification
- RFC 4033-4035: DNSSEC

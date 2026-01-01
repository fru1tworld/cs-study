# RFC 8484 - DNS Queries over HTTPS (DoH)

> **발행일**: 2018년 10월
> **저자**: Paul Hoffman (ICANN), Patrick McManus (Mozilla)
> **상태**: Standards Track

## 1. 개요

RFC 8484는 **HTTPS를 통해 DNS 쿼리와 응답을 전송**하는 프로토콜을 정의합니다. 각 DNS 쿼리-응답 쌍이 HTTP 교환에 매핑되며, TLS 암호화를 통해 DNS 트래픽의 기밀성과 무결성을 보장합니다.

### DoH가 제공하는 핵심 기능

| 기능 | 설명 |
|------|------|
| **암호화** | TLS를 통한 DNS 트래픽 암호화 |
| **프라이버시** | DNS 쿼리 내용의 제3자 노출 방지 |
| **우회 방지** | 중간 장치의 DNS 조작 방지 |
| **웹 통합** | 브라우저 API를 통한 DNS 접근 가능 |

### 주요 목적

```
1. DNS 작업 보호
   - 중간 장치의 DNS 간섭 방지
   - 패킷 감시 및 변조 차단

2. 웹 애플리케이션 통합
   - CORS를 통한 브라우저 API 접근
   - JavaScript에서 DNS 조회 가능
```

## 2. HTTP 통합

### 2.1 기본 원칙

```
"각 DNS 쿼리-응답 쌍은 HTTP 교환에 매핑됩니다"

DoH는 단순 터널링이 아닌 HTTP 기능 활용:
- 콘텐츠 협상
- 캐싱
- 리디렉션
- 프록시
- 인증
- 압축
```

### 2.2 HTTPS 요구사항

```
필수 요구사항:
- HTTPS URI 스킴 사용 (http:// 금지)
- TLS를 통한 전송 보안
- 서버 인증서 검증

보안 제공:
- 기밀성 (Confidentiality)
- 무결성 (Integrity)
- 서버 인증 (Server Authentication)
```

## 3. HTTP 요청 명세

### 3.1 메서드 지원

DoH 서버는 **POST**와 **GET** 메서드를 모두 구현해야 합니다:

#### POST 메서드

```http
POST /dns-query HTTP/2
Host: dns.example.com
Content-Type: application/dns-message
Content-Length: 33
Accept: application/dns-message

<DNS 쿼리 바이너리 데이터>
```

#### GET 메서드

```http
GET /dns-query?dns=AAABAAABAAAAAAAAA3d3dwdleGFtcGxlA2NvbQAAAQAB HTTP/2
Host: dns.example.com
Accept: application/dns-message
```

### 3.2 메서드 비교

| 항목 | POST | GET |
|------|------|-----|
| DNS 쿼리 위치 | 메시지 본문 | URI 쿼리 파라미터 |
| 인코딩 | 바이너리 그대로 | base64url |
| 캐시 가능성 | 특별한 헤더 필요 | 기본적으로 캐시 가능 |
| Content-Type | application/dns-message | 없음 |

### 3.3 Base64url 인코딩

```
GET 요청 시 DNS 쿼리 인코딩:

1. DNS 와이어 형식 데이터 준비
2. base64url 인코딩 적용
3. 패딩 문자(=) 제거
4. "dns" 파라미터로 전달

예시:
원본 DNS 쿼리 (hex): 00 00 01 00 00 01 00 00 00 00 00 00 ...
Base64url: AAABAAABAAAAAAAAA3d3dwdleGFtcGxlA2NvbQAAAQAB
URI: /dns-query?dns=AAABAAABAAAAAAAAA3d3dwdleGFtcGxlA2NvbQAAAQAB
```

### 3.4 URI 템플릿

```
DoH 클라이언트 구성:
- URI 템플릿으로 해석 URL 구성 방법 정의
- 구성 탐색은 프로토콜 외부에서 발생 (DHCP 등)

예시 템플릿:
https://dns.example.com/dns-query{?dns}

⚠️ 중요: 클라이언트는 구성 외부에서 발견된
다른 URI를 사용해서는 안 됨 (MUST NOT)
```

### 3.5 요청 헤더

```http
필수/권장 헤더:

Accept: application/dns-message
- 지원하는 응답 형식 명시

Content-Type: application/dns-message (POST)
- 요청 본문 형식 명시

DNS ID 권장사항:
- HTTP가 요청/응답을 상관시키므로
- DNS Message ID는 0 사용 권장
- 캐싱 효율성 향상
```

## 4. HTTP 응답 처리

### 4.1 성공 응답

```
HTTP 2xx 상태 코드:
- 모든 유효한 DNS 응답에 적용
- SERVFAIL, NXDOMAIN 등 DNS 오류도 포함

예시:
HTTP/2 200 OK
Content-Type: application/dns-message
Content-Length: 64
Cache-Control: max-age=300

<DNS 응답 바이너리 데이터>

→ HTTP 전송 성공 ≠ DNS 쿼리 성공
→ DNS 응답 코드를 별도로 확인해야 함
```

### 4.2 오류 처리

| HTTP 상태 | 의미 | 클라이언트 동작 |
|-----------|------|-----------------|
| 401 | 권한 부여 실패 | 동일 서버에서 재시도 가능 |
| 403 | 접근 거부 | 다른 서버 시도 |
| 415 | 미디어 유형 미지원 | 대체 서버 시도 |
| 429 | 요청 과다 | 재시도 대기 |
| 5xx | 서버 오류 | 대체 서버 시도 |

### 4.3 HTTP/전송 오류 vs DNS 오류

```
분리된 오류 의미론:

HTTP 수준 (전송):
- 2xx: HTTP 전송 성공
- 4xx/5xx: HTTP 전송 실패

DNS 수준 (내용):
- NOERROR (0): DNS 쿼리 성공
- SERVFAIL (2): 서버 실패
- NXDOMAIN (3): 도메인 없음
- REFUSED (5): 쿼리 거부

예시: HTTP 200 + DNS NXDOMAIN
→ HTTP 전송 성공, 도메인이 존재하지 않음
```

## 5. 미디어 유형

### 5.1 application/dns-message

```
미디어 유형: application/dns-message

특성:
- RFC 1035 DNS 와이어 형식 사용
- 최대 크기: 65,535 바이트
- 바이너리 인코딩

차이점 (DNS-over-TLS와):
- DoH: 길이 접두사 없음
- DoT: 2바이트 길이 접두사 포함
```

### 5.2 GET vs POST 인코딩

```
POST 요청:
Content-Type: application/dns-message
Body: <DNS 와이어 형식 그대로>

GET 요청:
URI: ?dns=<base64url 인코딩된 DNS 쿼리>
패딩 문자(=) 제거
```

### 5.3 EDNS 처리

```
EDNS (Extension Mechanisms for DNS):

서버 동작:
- EDNS UDP 페이로드 크기 값 무시
- HTTP가 크기 제한 관리

클라이언트 동작:
- EDNS 옵션 포함 가능
- 패딩 옵션 사용 권장 (트래픽 분석 방지)
```

## 6. 캐싱

### 6.1 HTTP 캐시 통합

```
DoH는 HTTP 캐싱 인프라 활용:

장점:
- 기존 HTTP 캐시 재사용
- CDN 캐싱 가능
- 브라우저 캐시 활용

GET 요청:
- 기본적으로 캐시 가능
- URL이 캐시 키로 동작

POST 요청:
- 기본적으로 캐시 불가
- 특별한 헤더로 캐시 가능하게 설정 가능
```

### 6.2 TTL 관리

```
HTTP 신선도 vs DNS TTL:

규칙:
HTTP freshness lifetime ≤ min(DNS TTL)

예시:
DNS 응답에 두 레코드:
- A 레코드: TTL 600초
- CNAME 레코드: TTL 300초

→ HTTP Cache-Control: max-age=300 (최소값)
→ 만료된 RRset 제공 방지
```

### 6.3 Age 헤더 처리

```
클라이언트는 Age 헤더 고려해야 함:

예시:
- DNS TTL: 600초
- HTTP Age: 250초
- 유효 남은 시간: 350초

계산:
effective_ttl = original_ttl - age
             = 600 - 250
             = 350초
```

### 6.4 부정 응답 캐싱

```
NXDOMAIN 등 부정 응답:

SOA 레코드의 MINIMUM 필드가 TTL 결정:
- SOA MINIMUM: 3600초
→ Cache-Control: max-age=3600

목적:
- 존재하지 않는 도메인 쿼리 반복 방지
- 서버 부하 감소
```

## 7. HTTP/2 요구사항

### 7.1 최소 권장 버전

```
HTTP/2가 DoH의 최소 권장 버전

이유:
UDP 기반 DNS의 장점:
- 비순서화 (Unordering)
- 낮은 오버헤드
- 병렬 쿼리

HTTP/2 제공:
- 멀티플렉싱 (재정렬)
- 병렬 요청
- 우선순위
- 헤더 압축 (HPACK)
```

### 7.2 HTTP/1.1 제한

```
HTTP/1.1 사용 시 문제:

1. Head-of-line blocking
   - 순차 요청/응답
   - 느린 응답이 전체 지연

2. 연결 오버헤드
   - 병렬화를 위해 다중 연결 필요
   - 리소스 낭비

3. 헤더 압축 없음
   - 반복 헤더 전송
   - 대역폭 낭비
```

### 7.3 HTTP/3 (QUIC)

```
HTTP/3도 DoH에 적합:

장점:
- UDP 기반으로 더 낮은 지연
- 연결 마이그레이션
- 0-RTT 재연결

참고: DNS over QUIC (DoQ)는 별도 프로토콜
- RFC 9250
- HTTP 없이 직접 QUIC 사용
```

## 8. 보안 고려사항

### 8.1 전송 보안

```
HTTPS + TLS 제공:

1. 암호화
   - DNS 쿼리/응답 내용 보호
   - 도청 방지

2. 서버 인증
   - 인증서 검증으로 서버 신원 확인
   - 위장 서버 방지

3. 무결성
   - 전송 중 변조 감지
   - 중간자 공격 방지

⚠️ 제한:
"HTTPS 연결은 DoH 서버와 클라이언트 간의
전송 보안을 제공하지만, DNSSEC에서 제공하는
DNS 데이터의 응답 무결성을 제공하지는 않습니다"
```

### 8.2 DNSSEC 독립성

```
DNSSEC과 DoH는 독립적이고 호환되는 프로토콜:

DoH 제공:
- 전송 계층 보안
- 클라이언트-서버 간 암호화

DNSSEC 제공:
- 데이터 원본 인증
- 데이터 무결성 (서명)

함께 사용:
- DoH로 전송 보호
- DNSSEC로 데이터 검증
- AD (Authentic Data) 비트 확인
```

### 8.3 트래픽 분석 방어

```
세션 암호화의 한계:
- 패킷 길이 노출
- 타이밍 정보 노출
- 트래픽 패턴 분석 가능

방어 수단:

1. DNS 패딩 (EDNS)
   - 클라이언트가 패딩 옵션 요청
   - 서버가 응답에 패딩 추가

2. HTTP/2 패딩
   - HEADERS/DATA 프레임 패딩
   - RFC 7540 Section 6.1

3. 균일 크기 레코드
   - 모든 응답을 동일 크기로
```

### 8.4 캐시 보안

```
HTTP 캐시 제어자의 위험:

공격자가 캐시 제어 시:
- 조작된 DNS 응답 삽입 가능
- 클라이언트가 잘못된 IP로 연결

완화:
- 캐시 무결성 검증
- DNSSEC 검증
- 신뢰할 수 있는 캐시만 사용
```

## 9. 프라이버시 고려사항

### 9.1 와이어상 프라이버시

```
DoH의 프라이버시 이점:

1. DNS 트래픽 암호화
   - ISP가 DNS 쿼리 내용 볼 수 없음
   - 감시 방지

2. 포트 443 사용
   - 다른 HTTPS 트래픽과 혼합
   - 선별적 차단 어려움

3. 서버 인증
   - 위장 DNS 서버 방지
```

### 9.2 서버측 프라이버시

```
DNS 와이어 형식에는 클라이언트 식별자 없음

그러나 HTTPS에서 상관관계 가능:

IP 계층:
- 클라이언트 IP 주소

TCP 계층:
- 장기 연결 추적

TLS 계층:
- 세션 재개 (Session Resumption)
- 클라이언트 인증서

HTTP 계층:
- User-Agent 헤더
- Accept-Language 헤더
- 쿠키
```

### 9.3 HTTP 쿠키

```
⚠️ 중요 권장사항:

"HTTP 쿠키는 사용 사례에서 명시적으로 요구되지 않는 한
DoH 클라이언트에 의해 수락되어서는 안 됩니다 (SHOULD NOT)"

이유:
- 세션 간 클라이언트 추적 가능
- 프라이버시 침해
- DNS 익명성 훼손

예외:
- 인증된 DNS 서비스
- 사용자 동의 있는 경우
```

### 9.4 핑거프린팅

```
HTTP 요청 핑거프린팅:

추적 가능 요소:
- 헤더 순서
- 헤더 값 조합
- TLS 특성 (JA3 등)

완화:
- 표준화된 요청 형식 사용
- 불필요한 헤더 제거
- 여러 클라이언트와 유사한 형식
```

## 10. 운영 고려사항

### 10.1 서버 선택 영향

```
DNS 서버 선택이 결과에 영향:

분할 DNS (Split DNS):
- 회사 내부: 내부 서버 주소 반환
- 외부: 공개 서버 주소 반환

DoH 사용 시:
- 기업 DNS 대신 공개 DoH 사용하면
- 내부 리소스 접근 불가능
```

### 10.2 인증서 검증 교착상태

```
문제:
DoH 서버 연결 → 인증서 검증 필요
→ OCSP/CRL 확인 필요 → DNS 조회 필요
→ DNS 조회에 DoH 필요 → 교착상태

해결책:

1. OCSP Stapling
   - 서버가 TLS 핸드셰이크에 OCSP 응답 포함
   - 추가 DNS 조회 불필요

2. 중간 인증서 번들링
   - 서버가 전체 체인 제공
   - AIA 조회 불필요

3. 초기 해석 구성
   - DoH 서버 IP 주소 직접 구성
   - 호스트명 해석 불필요
```

### 10.3 부트스트래핑

```
DoH 클라이언트 초기 구성:

문제:
- DoH 서버 URI에 호스트명 포함
- 호스트명 해석에 DNS 필요
- 아직 DoH 연결 안 됨

해결책:

1. IP 기반 URI
   https://1.1.1.1/dns-query
   - 호스트명 해석 불필요
   - IP 기반 인증서 필요

2. 전통적 DNS로 초기 해석
   - 첫 해석만 일반 DNS 사용
   - 이후 DoH로 전환
   - HTTPS 인증으로 보안 유지

3. 구성 파일
   - 서버 IP 주소 사전 구성
   - 업데이트 메커니즘 필요
```

### 10.4 상태 비저장 설계

```
HTTP의 상태 비저장 특성:

의미:
- 요청 간 순서 보장 없음
- 각 요청이 독립적

영향:
- 순서 의존적 프로토콜에 부적합
- DNS 자체는 대부분 순서 무관

예외:
- DNS 업데이트 (RFC 2136)
- 영역 전송 (AXFR/IXFR)
```

### 10.5 EDNS 확장 비적용

```
전송 특정 EDNS 확장은 DoH에 적용 안 됨:

예시:
- edns-tcp-keepalive
- TCP 연결 유지용
- HTTP가 연결 관리하므로 불필요

DoH에서 사용 가능한 EDNS:
- EDNS Padding
- EDNS Client Subnet (선택)
- DNSSEC OK (DO) 비트
```

## 11. IANA 등록

### 11.1 미디어 유형 등록

```
유형: application
하위유형: dns-message
인코딩: RFC 1035 DNS 메시지 바이너리 형식
보안: 비실행 가능 콘텐츠
사용: COMMON

등록자: Paul Hoffman (paul.hoffman@icann.org)
```

### 11.2 URI 스킴

```
DoH는 기존 https URI 스킴 사용:

형식:
https://dns.example.com/dns-query{?dns}

템플릿 변수:
- dns: base64url 인코딩된 DNS 쿼리 (GET용)
```

## 12. 구현 예시

### 12.1 클라이언트 요청 예시

```
DNS 쿼리: www.example.com A 레코드

POST 요청:
POST /dns-query HTTP/2
Host: cloudflare-dns.com
Accept: application/dns-message
Content-Type: application/dns-message
Content-Length: 33

[바이너리 DNS 쿼리]

GET 요청:
GET /dns-query?dns=AAABAAABAAAAAAAAA3d3dwdleGFtcGxlA2NvbQAAAQAB HTTP/2
Host: cloudflare-dns.com
Accept: application/dns-message
```

### 12.2 서버 응답 예시

```http
HTTP/2 200 OK
Content-Type: application/dns-message
Content-Length: 64
Cache-Control: max-age=300

[바이너리 DNS 응답]
```

### 12.3 주요 DoH 서버

| 제공자 | URI |
|--------|-----|
| Cloudflare | https://cloudflare-dns.com/dns-query |
| Google | https://dns.google/dns-query |
| Quad9 | https://dns.quad9.net/dns-query |

## 요약

RFC 8484 DNS over HTTPS (DoH)는 다음을 제공합니다:

- **암호화된 DNS**: HTTPS를 통한 DNS 쿼리/응답 보호
- **HTTP 통합**: 캐싱, 프록시, 압축 등 HTTP 기능 활용
- **프라이버시 강화**: DNS 트래픽 감시 및 조작 방지
- **웹 호환성**: 브라우저 및 웹 애플리케이션에서 DNS 접근
- **DNSSEC 호환**: 전송 보안과 데이터 인증 독립적으로 제공

DoH는 **현대 인터넷 프라이버시의 핵심 구성요소**로, 전통적인 평문 DNS의 보안 취약점을 해결합니다.

---

*원본: https://www.rfc-editor.org/rfc/rfc8484*

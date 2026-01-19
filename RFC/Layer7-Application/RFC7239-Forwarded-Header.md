# RFC 7239: Forwarded HTTP Extension

> HTTP 요청이 프록시를 거칠 때 손실되는 정보를 전달하기 위한 표준화된 Forwarded 헤더 필드

## 문서 정보

| 항목 | 내용 |
|------|------|
| **RFC 번호** | 7239 |
| **분류** | Standards Track (표준 트랙) |
| **작성자** | A. Petersson, M. Nilsson (Opera Software) |
| **발행일** | 2014년 6월 |
| **상태** | Proposed Standard (제안 표준) |
| **ISSN** | 2070-1721 |

## 개요

RFC 7239는 프록시가 HTTP 요청을 처리할 때 손실되는 정보를 복원하기 위한 **Forwarded HTTP 헤더 필드**를 정의합니다. 프록시는 요청이 마치 프록시의 IP 주소에서 시작된 것처럼 보이게 만들기 때문에, 원래 클라이언트의 IP 주소나 프록시 체인의 정보가 손실됩니다.

이 문서는 다음 정보를 전달할 수 있는 표준화된 방법을 제공합니다:
- 요청을 시작한 클라이언트의 IP 주소
- 프록시의 사용자 에이전트 측 인터페이스 IP 주소
- 원본 Host 헤더 값
- 사용된 프로토콜 (HTTP/HTTPS)

---

## 표기법 규칙

이 문서에서 사용된 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", "OPTIONAL"이라는 키워드는 RFC 2119에 기술된 대로 해석되어야 합니다.

---

## X-Forwarded-* 헤더와의 관계

### 기존 비표준 헤더들

이전에는 프록시 관련 정보를 전달하기 위해 다음과 같은 비표준 헤더 필드들이 사용되었습니다:

| 비표준 헤더 | 용도 |
|------------|------|
| `X-Forwarded-For` | 클라이언트 IP 주소 전달 |
| `X-Forwarded-By` | 프록시 IP 주소 전달 |
| `X-Forwarded-Proto` | 원본 프로토콜 전달 |
| `X-Forwarded-Host` | 원본 Host 헤더 전달 |

### 표준화의 이점

RFC 7239의 `Forwarded` 헤더는 이러한 비표준 헤더들을 대체하기 위해 설계되었습니다:

```
표준화 이점:
├── 구현 간 상호운용성 향상
├── 일관된 형식과 의미론
├── 단일 헤더로 모든 정보 통합
├── IPv6 주소 형식 명확화
└── 보안 및 프라이버시 고려사항 명시
```

### 마이그레이션 예시

```
X-Forwarded-For: 192.0.2.43
X-Forwarded-Proto: https
X-Forwarded-Host: example.com

→ 변환 후:

Forwarded: for=192.0.2.43;proto=https;host=example.com
```

---

## Forwarded 헤더 필드

### ABNF 문법

Forwarded 헤더 필드의 값은 ABNF(Augmented Backus-Naur Form) 문법으로 다음과 같이 정의됩니다:

```abnf
Forwarded         = 1#forwarded-element
forwarded-element = [ forwarded-pair ] *( ";" [ forwarded-pair ] )
forwarded-pair    = token "=" value
value             = token / quoted-string

token             = <RFC 7230 Section 3.2.6에 정의됨>
quoted-string     = <RFC 7230 Section 3.2.6에 정의됨>
```

### 기본 형식

```
Forwarded: <파라미터>=<값>; <파라미터>=<값>, <다음 프록시 정보>
```

- **세미콜론(;)**: 같은 프록시에서 추가한 파라미터 구분
- **쉼표(,)**: 다른 프록시에서 추가한 정보 구분

### 예제

```http
# 단일 프록시
Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43

# 다중 프록시 체인
Forwarded: for=192.0.2.43, for=198.51.100.17;by=203.0.113.60;proto=http;host=example.com

# IPv6 주소 (반드시 따옴표로 감싸야 함)
Forwarded: for="[2001:db8:cafe::17]:47011"

# 난독화된 식별자
Forwarded: for=_hidden;by=_proxy01
```

### 파라미터 규칙

각 파라미터에 대해 다음 규칙이 적용됩니다:

1. **파라미터 이름은 대소문자 구분 없음** (case-insensitive)
2. **각 파라미터는 field-value당 한 번만 나타날 수 있음**
3. **순서**: 목록의 첫 번째 요소는 첫 번째 프록시가 추가한 정보

---

## 핵심 파라미터

### 1. "for" 파라미터

**목적**: 요청을 시작한 클라이언트 또는 프록시 체인의 이전 프록시를 식별합니다.

```abnf
for-value = node
```

**특징**:
- 클라이언트의 IP 주소를 전달하는 가장 일반적인 파라미터
- 프라이버시를 위해 난독화된 식별자 사용 가능
- 기본 설정은 난독화된 식별자를 사용해야 함(SHOULD)

**예시**:

```http
# IPv4 주소
Forwarded: for=192.0.2.43

# IPv4 주소와 포트
Forwarded: for="192.0.2.43:47011"

# IPv6 주소 (반드시 대괄호와 따옴표 사용)
Forwarded: for="[2001:db8:cafe::17]"

# IPv6 주소와 포트
Forwarded: for="[2001:db8:cafe::17]:47011"

# 알 수 없는 클라이언트
Forwarded: for=unknown

# 난독화된 식별자
Forwarded: for=_secret-client-id
```

---

### 2. "by" 파라미터

**목적**: 프록시의 사용자 에이전트 측 인터페이스를 식별합니다.

```abnf
by-value = node
```

**특징**:
- 주로 리버스 프록시가 백엔드 서버에 정보를 전달할 때 사용
- 멀티홈(multi-homed) 환경에서 요청이 어느 인터페이스로 들어왔는지 표시
- 기본 설정은 난독화된 식별자를 사용해야 함(SHOULD)

**예시**:

```http
# 프록시 IP 주소
Forwarded: by=203.0.113.60

# 난독화된 프록시 식별자
Forwarded: by=_proxy01
```

**사용 사례**:

```
클라이언트 → [프록시 인터페이스 A] 로드밸런서 [인터페이스 B] → 백엔드 서버
                      ↑
                 by 파라미터로 식별
```

---

### 3. "host" 파라미터

**목적**: 원본 HTTP 요청의 `Host` 헤더 필드 값을 전달합니다.

```abnf
host-value = uri-host [ ":" port ]
             ; uri-host와 port는 RFC 3986 Section 3.2.2에 정의됨
```

**특징**:
- 리버스 프록시가 Host 헤더를 내부 호스트명으로 재작성할 때 유용
- 원본 서버가 클라이언트가 요청한 원래 호스트를 알 수 있음
- RFC 7230 Section 5.4의 Host ABNF를 따라야 함(MUST)

**예시**:

```http
# 원본 호스트
Forwarded: host=example.com

# 포트 포함
Forwarded: host=example.com:8080
```

**사용 시나리오**:

```
원래 요청:
GET / HTTP/1.1
Host: www.example.com

리버스 프록시가 내부 서버로 전달:
GET / HTTP/1.1
Host: internal-server.local
Forwarded: host=www.example.com
```

---

### 4. "proto" 파라미터

**목적**: 클라이언트가 사용한 원본 프로토콜을 표시합니다.

```abnf
proto-value = uri-scheme
              ; RFC 3986 Section 3.1에 정의됨
```

**특징**:
- 일반적인 값: `http` 또는 `https`
- RFC 3986 Section 3.1의 URI 스킴 이름 형식을 따라야 함(MUST)
- IANA에 등록된 프로토콜이어야 함

**예시**:

```http
# HTTPS로 들어온 요청
Forwarded: proto=https

# 여러 파라미터와 함께
Forwarded: for=192.0.2.43;proto=https;host=example.com
```

**중요성**:

```
클라이언트 ──HTTPS──> 로드밸런서 ──HTTP──> 백엔드 서버
                                    │
                    proto=https로 원본 프로토콜 전달
```

이를 통해 백엔드 서버가:
- 올바른 리다이렉트 URL 생성 가능
- HTTPS 전용 기능 활성화 가능
- 보안 쿠키 설정 가능

---

## 노드 식별자

### ABNF 정의

```abnf
node          = nodename [ ":" node-port ]
nodename      = IPv4address / "[" IPv6address "]" / "unknown" / obfnode

IPv4address   = <RFC 3986 Section 3.2.2에 정의됨>
IPv6address   = <RFC 3986 Section 3.2.2에 정의됨>
obfnode       = "_" 1*( ALPHA / DIGIT / "." / "_" / "-" )

node-port     = port / obfport
port          = 1*5DIGIT
obfport       = "_" 1*( ALPHA / DIGIT / "." / "_" / "-" )

DIGIT         = <RFC 5234 Section 3.4에 정의됨>
ALPHA         = <RFC 5234 Section B.1에 정의됨>
```

### 식별자 유형

#### 1. IPv4 및 IPv6 주소

IP 주소를 직접 사용하여 노드를 식별합니다.

```http
# IPv4
Forwarded: for=192.0.2.43

# IPv6 (반드시 대괄호로 감싸야 함)
Forwarded: for="[2001:db8:cafe::17]"
```

**IPv6 주소 규칙**:
- RFC 5952의 텍스트 표현 권장사항을 따라야 함(SHOULD)
- 소문자 사용
- 0 압축 사용
- 반드시 대괄호로 감싸야 함
- 콜론(:) 포함 시 반드시 따옴표로 감싸야 함

**내부 네트워크 주소**:
- RFC 1918 (사설 IPv4) 주소 사용 가능
- RFC 4193 (고유 로컬 IPv6) 주소 사용 가능

---

#### 2. "unknown" 식별자

이전 엔티티의 신원을 알 수 없지만, 요청 전달이 이루어졌음을 표시하고자 할 때 사용합니다.

```http
Forwarded: for=unknown
```

**사용 사례**:
- 프록시 서버 프로세스가 수신 요청 TCP 소켓에 직접 접근하지 않고 발신 요청을 생성하는 경우
- 클라이언트 정보를 의도적으로 숨기고자 하는 경우

**포트와 함께 사용**:

```http
Forwarded: for=unknown:47011
```

---

#### 3. 난독화된(Obfuscated) 식별자

실제 IP 주소나 포트 번호를 숨기면서도 요청 추적이 가능하도록 하는 토큰입니다.

```http
# 난독화된 노드명
Forwarded: for=_hidden

# 난독화된 노드명과 포트
Forwarded: for=_hidden:_port123

# 복잡한 난독화 식별자
Forwarded: for=_secret.client-id_123
```

**규칙**:
- 반드시 밑줄(_)로 시작해야 함(MUST)
- ALPHA, DIGIT, ".", "_", "-" 문자만 사용 가능(MUST)
- 요청마다 무작위로 생성해야 함(SHOULD)
- 요청 간 지속되는 식별자가 필요한 경우 수명을 제한해야 함(SHOULD)
- 클라이언트 IP 주소보다 오래 지속되어서는 안 됨(SHOULD NOT)

**보안 주의사항**:
- 난독화 식별자 생성 시 잠재적으로 민감한 정보를 포함하지 않도록 주의해야 함

---

## 인용 규칙

### 따옴표가 필요한 경우

콜론(:)은 token에서 허용되지 않는 문자이므로, 다음 경우에는 반드시 따옴표로 감싸야 합니다:

| 상황 | 예시 |
|------|------|
| IPv6 주소 | `for="[2001:db8::1]"` |
| 포트가 포함된 IP 주소 | `for="192.0.2.43:8080"` |
| 포트가 포함된 노드명 | `for="unknown:1234"` |

```http
# 올바른 예
Forwarded: for="[2001:db8:cafe::17]:47011"

# 잘못된 예 (따옴표 없음)
Forwarded: for=[2001:db8:cafe::17]:47011  # 파싱 오류!
```

---

## 구현 고려사항

### 헤더 필드 추가 방법

프록시는 다음 두 가지 방법 중 하나로 Forwarded 정보를 추가할 수 있습니다:

#### 방법 1: 기존 헤더에 값 추가

```http
# 원래 요청
Forwarded: for=192.0.2.43

# 두 번째 프록시 통과 후
Forwarded: for=192.0.2.43, for=198.51.100.17;by=203.0.113.60
```

#### 방법 2: 새 헤더 인스턴스 추가

```http
Forwarded: for=192.0.2.43
Forwarded: for=198.51.100.17;by=203.0.113.60
```

### 헤더 필드 제거

정책에 따라 프록시는 모든 Forwarded 헤더를 제거할 수 있습니다:

```
사용 사례:
├── 내부 네트워크 구조 숨기기
├── 프라이버시 보호
├── 보안 경계에서의 정보 차단
└── 규정 준수
```

### 기본 설정 권장사항

```
┌─────────────────────────────────────────────────────────────┐
│ 중요: 이 헤더 필드는 기본적으로 비활성화되어야 합니다(SHOULD)  │
└─────────────────────────────────────────────────────────────┘
```

- 배포 시 기본값: **비활성화**
- by와 for 파라미터: **난독화된 식별자 사용** (기본)
- 명시적 필요성이 있을 때만 실제 IP 주소 사용

---

## 보안 고려사항

### 1. 헤더 유효성 및 무결성

```
┌──────────────────────────────────────────────────────────────────┐
│ 경고: Forwarded 헤더 필드는 신뢰할 수 없습니다!                    │
│                                                                    │
│ 서버까지의 모든 노드(클라이언트 포함)에 의해 실수로 또는            │
│ 악의적으로 수정될 수 있습니다.                                     │
└──────────────────────────────────────────────────────────────────┘
```

**신뢰성 확보 방법**:

1. **프록시 화이트리스트**: 신뢰할 수 있는 프록시만 허용
   - 단점 1: 프록시에 도달하기 전의 IP 주소 체인은 신뢰 불가
   - 단점 2: 프록시와 엔드포인트 간 통신이 보안되지 않으면 공격자가 데이터 수정 가능

2. **보안 통신 채널 사용**: 프록시 간 TLS 연결

```
공격 시나리오:
악의적 클라이언트 → Forwarded: for=신뢰IP → 프록시 → 서버
                          ↑
            공격자가 허위 IP 주입
```

---

### 2. 정보 유출

Forwarded 헤더 필드는 NAT 또는 프록시 뒤의 내부 네트워크 구조를 노출할 수 있습니다.

**대응 방법**:

| 방법 | 설명 |
|------|------|
| 난독화된 요소 사용 | 실제 IP 대신 `_proxy01` 같은 식별자 사용 |
| 내부 노드 업데이트 방지 | 내부 노드가 헤더를 수정하지 못하게 함 |
| 출구 프록시에서 제거 | 내부 정보를 담은 항목을 제거 |

**응답에 복사 금지**:

```
중요: Forwarded 헤더 필드를 응답 메시지에 절대 복사해서는 안 됩니다!
      → 전체 프록시 체인이 클라이언트에게 노출됨
```

**TRACE 요청 주의**:

```http
# TRACE 요청 시 Forwarded 헤더가 응답 본문에 나타남
TRACE /path HTTP/1.1
Host: example.com
Forwarded: for=192.0.2.43;by=10.0.0.5

# 응답에 내부 IP가 노출됨!
```

Forwarded 필드가 사용되는 호스팅 환경에서는 TRACE 요청을 허용하지 않도록 특별히 주의해야 합니다.

---

### 3. 프라이버시 고려사항

**기본 원칙**:

사용자 프라이버시와 유용한 정보(디버깅, 통계, 위치 기반 콘텐츠) 제공 사이에는 트레이드오프가 존재합니다.

**프라이버시 요청 시 행동**:

```
HTTP 요청에 프라이버시를 명시적으로 요청하는 헤더 필드가 포함된 경우:

프록시는 Forwarded 헤더 필드를 사용해서는 안 되며(SHOULD NOT),
다른 어떤 방식으로도 IP 주소와 같은 개인 정보를 다음 홉으로
전달해서는 안 됩니다(SHOULD NOT).
```

**클라이언트 IP 주소의 민감성**:

```
IP 주소로 알 수 있는 정보:
├── 개별 클라이언트 식별 가능
├── 사용 중인 인터넷 서비스 제공자
├── 대략적인 지리적 위치
└── 네트워크 환경 정보
```

**난독화 식별자 권장사항**:

| 항목 | 권장사항 |
|------|----------|
| 기본 설정 | 난독화된 식별자 사용(SHOULD) |
| 생성 빈도 | 요청마다 무작위 생성(SHOULD) |
| 수명 제한 | 요청 간 지속 시 수명 제한(SHOULD) |
| 최대 수명 | 클라이언트 IP 주소보다 오래 지속 금지(SHOULD NOT) |

**핑거프린팅 위험**:

```
사용자 추적 방법:
├── Forwarded 헤더의 고유 클라이언트 식별자
├── 프록시 체인 정보로 인한 핑거프린팅 용이성 증가
├── 브라우저 헤더 필드 핑거프린팅
└── 기타 누출 경로
```

---

### 4. 네트워크 주소 변환 (NAT)

NAT 환경에서는 여러 클라이언트가 동일한 공인 IP를 공유할 수 있습니다.

```
내부 네트워크:
├── 192.168.1.10 ─┐
├── 192.168.1.11 ─┼── NAT ── 공인 IP: 203.0.113.50 ── 서버
└── 192.168.1.12 ─┘
```

이 경우 포트 번호가 클라이언트를 구분하는 데 중요할 수 있습니다:

```http
Forwarded: for="203.0.113.50:12345"
Forwarded: for="203.0.113.50:12346"
```

---

### 5. 서비스 거부 (DoS)

대규모 Forwarded 헤더 필드 값은 서비스 거부 공격에 사용될 수 있습니다.

**공격 시나리오**:

```http
# 매우 긴 Forwarded 헤더로 서버 리소스 소모
Forwarded: for=192.0.2.1, for=192.0.2.2, for=192.0.2.3, ... (수천 개)
```

**대응 방법**:
- 헤더 크기 제한
- 프록시 홉 수 제한
- 비정상적으로 큰 헤더 거부

---

## IANA 고려사항

### Forwarded 헤더 필드 등록

RFC 7239는 "Forwarded" 헤더 필드를 "영구 메시지 헤더 필드 이름" 레지스트리에 등록합니다.

| 항목 | 값 |
|------|-----|
| 헤더 필드 | Forwarded |
| 적용 프로토콜 | http |
| 상태 | standard |
| 저자/변경 관리자 | IETF (iesg@ietf.org) |
| 참조 문서 | RFC 7239 |

### HTTP Forwarded Parameters 레지스트리

IANA는 "HTTP Forwarded Parameters"라는 새 레지스트리를 생성하고 관리합니다.

**등록 절차**: IETF Review (RFC 5226)

**초기 등록 파라미터**:

| 파라미터 | 설명 | 참조 |
|----------|------|------|
| by | 프록시의 수신 인터페이스 IP 주소 | RFC 7239, Section 5.1 |
| for | 요청하는 클라이언트의 IP 주소 | RFC 7239, Section 5.2 |
| host | 원본 Host 헤더 필드 값 | RFC 7239, Section 5.3 |
| proto | 사용된 프로토콜 타입 | RFC 7239, Section 5.4 |

### 확장 파라미터

새로운 파라미터를 등록할 때는 다음을 준수해야 합니다:

1. **보안 및 프라이버시 영향 문서화**: 모든 새 파라미터의 보안/프라이버시 영향을 철저히 문서화해야 함
2. **ABNF 준수**: 새 파라미터와 값은 Section 4의 forwarded-pair ABNF를 따라야 함(MUST)
3. **간단한 설명 제공**: 등록 시 짧은 설명 포함

---

## 실제 사용 예제

### 기본 사용

```http
# 단순 클라이언트 IP 전달
GET /resource HTTP/1.1
Host: example.com
Forwarded: for=192.0.2.60

# 프로토콜과 함께
GET /resource HTTP/1.1
Host: example.com
Forwarded: for=192.0.2.60;proto=https
```

### 다중 프록시 환경

```
클라이언트(192.0.2.43) → 프록시1(198.51.100.178) → 프록시2(203.0.113.60) → 서버
```

```http
# 프록시2를 거친 후 서버가 받는 요청
GET /resource HTTP/1.1
Host: example.com
Forwarded: for=192.0.2.43, for=198.51.100.178;by=203.0.113.60;proto=http;host=example.com
```

### 리버스 프록시 환경

```http
# 원래 요청 (클라이언트 → 로드밸런서)
GET /api/users HTTP/1.1
Host: api.example.com
X-Request-ID: abc123

# 백엔드로 전달되는 요청
GET /api/users HTTP/1.1
Host: internal-api-server.local
Forwarded: for=192.0.2.43;host=api.example.com;proto=https;by=10.0.0.5
X-Request-ID: abc123
```

### 프라이버시 보호 설정

```http
# 난독화된 식별자 사용
Forwarded: for=_req-12345;by=_proxy-01

# 알 수 없는 클라이언트
Forwarded: for=unknown;proto=https
```

### CDN 환경

```http
# CDN 엣지 서버를 거친 요청
GET /static/image.png HTTP/1.1
Host: cdn.example.com
Forwarded: for=192.0.2.43;proto=https;by=edge-server-sjc.cdn.example.com
```

---

## 구현 가이드라인

### 마이그레이션 전략

기존 X-Forwarded-* 헤더에서 Forwarded 헤더로 마이그레이션할 때:

```
권장 접근 방식:
1. 두 헤더 모두 지원 (과도기)
2. Forwarded 우선 처리
3. X-Forwarded-* 폴백
4. 점진적 전환
```

### 보안 구현

```python
# 의사 코드: 안전한 Forwarded 헤더 처리

def process_forwarded_header(request, trusted_proxies):
    forwarded = request.headers.get('Forwarded')

    if not forwarded:
        return None

    # 신뢰할 수 있는 프록시 체인에서만 처리
    remote_addr = request.remote_addr
    if remote_addr not in trusted_proxies:
        # 신뢰할 수 없는 소스의 헤더 무시
        return None

    # 파싱 및 유효성 검사
    parsed = parse_forwarded(forwarded)

    # 크기 제한 적용
    if len(parsed) > MAX_PROXY_HOPS:
        raise SecurityError("Too many proxy hops")

    return parsed
```

### 호환성 처리

```
두 헤더 형식 모두 존재할 때:

1. 서버가 Forwarded 지원을 명시한 경우:
   → Forwarded 사용, X-Forwarded-* 무시

2. 서버 지원이 불명확한 경우:
   → X-Forwarded-* 사용, Forwarded 무시

3. 보안 주의:
   → 공격자가 Forwarded를 조작하고 서버가 X-Forwarded-*만
     확인하는 경우 IP 스푸핑 가능
```

---

## 참고 자료

- [RFC 7239 원문 (IETF)](https://datatracker.ietf.org/doc/html/rfc7239)
- [RFC 7239 (RFC Editor)](https://www.rfc-editor.org/rfc/rfc7239.html)
- [HTTP Forwarded Parameters Registry (IANA)](https://www.iana.org/assignments/http-parameters)

---

## 관련 RFC

| RFC | 제목 | 관계 |
|-----|------|------|
| RFC 7230 | HTTP/1.1 Message Syntax and Routing | 토큰, quoted-string 정의 참조 |
| RFC 3986 | URI Generic Syntax | IP 주소, URI 스킴 정의 참조 |
| RFC 5234 | ABNF | 문법 표기법 |
| RFC 5952 | IPv6 주소 텍스트 표현 | IPv6 표기 권장사항 |
| RFC 1918 | 사설 인터넷 주소 | 사설 IPv4 주소 범위 |
| RFC 4193 | 고유 로컬 IPv6 유니캐스트 주소 | 사설 IPv6 주소 범위 |
| RFC 2119 | 요구사항 수준 키워드 | MUST, SHOULD 등 정의 |

---

## 요약

RFC 7239는 프록시 환경에서 클라이언트와 프록시 정보를 표준화된 방식으로 전달하기 위한 `Forwarded` HTTP 헤더 필드를 정의합니다.

### 핵심 파라미터

| 파라미터 | 용도 |
|----------|------|
| `for` | 클라이언트/이전 프록시 식별 |
| `by` | 현재 프록시 인터페이스 식별 |
| `host` | 원본 Host 헤더 값 |
| `proto` | 원본 프로토콜 (http/https) |

### 핵심 보안 원칙

1. **기본 비활성화**: 헤더 필드는 기본적으로 꺼져 있어야 함
2. **난독화 기본**: by와 for 파라미터는 난독화된 식별자 사용 권장
3. **신뢰 불가**: 헤더 값을 무조건 신뢰해서는 안 됨
4. **응답 복사 금지**: 응답에 Forwarded 헤더를 포함하지 말 것
5. **프라이버시 존중**: 프라이버시 요청 시 정보 전달 자제

---

*이 문서는 RFC 7239의 한국어 번역 및 정리본입니다.*

# RFC 6265: HTTP State Management Mechanism (HTTP 상태 관리 메커니즘)

> HTTP 쿠키의 표준 명세 - Set-Cookie 및 Cookie 헤더 필드 정의

## 문서 정보

| 항목 | 내용 |
|------|------|
| RFC 번호 | 6265 |
| 분류 | Proposed Standard |
| 작성자 | A. Barth (UC Berkeley) |
| 발행일 | 2011년 4월 |
| 상태 | 현행 표준 |
| 폐기한 문서 | RFC 2965 |

---

## 1. 개요 (Introduction)

이 문서는 HTTP Cookie 및 Set-Cookie 헤더 필드를 정의합니다. Set-Cookie 헤더 필드를 사용하여 HTTP 서버는 이름/값 쌍과 관련 메타데이터(쿠키라고 함)를 사용자 에이전트에게 전달할 수 있습니다. 사용자 에이전트가 서버에 후속 요청을 보낼 때, 메타데이터와 기타 정보를 사용하여 Cookie 헤더에 이름/값 쌍을 반환할지 여부를 결정합니다.

### 1.1 쿠키의 복잡성

표면적으로는 단순해 보이지만, 쿠키에는 여러 복잡성이 있습니다:

- 범위(Scope): 서버는 각 쿠키를 보낼 때 범위를 지정합니다. 이 범위는 사용자 에이전트가 쿠키를 반환해야 하는 최대 기간, 쿠키를 반환해야 하는 서버들, 쿠키가 적용되는 URI 스킴을 나타냅니다.

### 1.2 보안 및 개인정보 문제

역사적인 이유로 쿠키에는 여러 보안 및 개인정보 관련 결함이 있습니다:

| 문제점 | 설명 |
|--------|------|
| Secure 속성 한계 | 서버가 쿠키가 "보안" 연결용임을 표시할 수 있지만, Secure 속성은 활성 네트워크 공격자에 대한 무결성을 제공하지 않습니다 |
| 포트 격리 부재 | 특정 호스트의 쿠키는 해당 호스트의 모든 포트에서 공유됩니다. 이는 웹 브라우저가 다른 포트를 통해 검색된 콘텐츠를 격리하는 일반적인 "동일 출처 정책"과 다릅니다 |

### 1.3 대상 독자

이 명세의 대상 독자는 두 그룹입니다:

1. 쿠키 생성 서버 개발자: Section 4에 정의된 잘 동작하는(well-behaved) 프로파일을 따라야 합니다(SHOULD)
2. 쿠키 소비 사용자 에이전트 개발자: Section 5에 정의된 더 관대한 처리 규칙을 구현해야 합니다(MUST)

### 1.4 이전 명세와의 관계

이 문서 이전에 쿠키에 대한 최소 세 가지 설명이 있었습니다:
- "Netscape 쿠키 명세"
- RFC 2109
- RFC 2965

그러나 이 문서들 중 어느 것도 Cookie 및 Set-Cookie 헤더가 인터넷에서 실제로 사용되는 방식을 설명하지 않았습니다.

이 문서의 조치사항:
1. RFC 2109의 상태를 Historic으로 변경
2. RFC 2965의 상태를 Historic으로 변경
3. RFC 2965가 이 문서에 의해 폐기됨을 표시

특히, RFC 2965를 Historic으로 이동하고 폐기함에 따라 Cookie2 및 Set-Cookie2 헤더 필드의 사용이 더 이상 권장되지 않습니다(deprecated).

---

## 2. 규칙 (Conventions)

### 2.1 준수 기준

이 문서에서 사용된 키워드 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", "OPTIONAL"은 RFC 2119에 기술된 대로 해석되어야 합니다.

명령형으로 표현된 알고리즘 요구사항(예: "선행 공백 문자 제거" 또는 "false를 반환하고 이 단계를 중단")은 해당 알고리즘을 도입하는 키워드("MUST", "SHOULD", "MAY" 등)의 의미로 해석되어야 합니다.

중요: 이 명세에 정의된 알고리즘은 이해하기 쉽게 의도된 것이며, 성능을 위해 의도된 것이 아닙니다. 준수하는 구현체는 동등한 결과를 달성하는 모든 접근 방식을 사용할 수 있습니다.

### 2.2 구문 표기법

이 명세는 RFC 5234의 ABNF(Augmented Backus-Naur Form) 표기법을 사용합니다.

사용되는 ABNF 핵심 규칙:
- ALPHA (알파벳)
- DIGIT (숫자)
- DQUOTE (큰따옴표)
- HEXDIG (16진수 숫자)
- SP (공백)
- VCHAR (보이는 문자)

### 2.3 용어

| 용어 | 정의 |
|------|------|
| 사용자 에이전트(User Agent) | "클라이언트" 역할을 수행하는 모든 HTTP 구현체 |
| 클라이언트(Client) | 요청을 보내기 위해 연결을 설정하는 프로그램 |
| 서버(Server) | 요청을 수신하기 위해 연결을 수락하는 애플리케이션 프로그램 |
| 요청 호스트(request-host) | HTTP 요청에서 사용자 에이전트가 전송하는 Host 헤더의 호스트 |
| 요청 URI(request-uri) | URI를 획득하기 위해 HTTP 요청이 전송된 URI |

대소문자 무관 비교: 두 옥텟 시퀀스를 비교할 때, 0x41부터 0x5A까지의 범위(A-Z)의 옥텟을 0x61부터 0x7A까지(a-z)의 범위로 매핑한 후 옥텟 단위로 동일하면 대소문자 무관하게 일치합니다.

---

## 3. 개요 (Overview)

이 섹션은 원본 서버가 사용자 에이전트에게 상태 정보를 보내고, 사용자 에이전트가 원본 서버에게 상태 정보를 반환하는 방법을 설명합니다.

### 3.1 기본 메커니즘

```
┌─────────────┐                           ┌─────────────┐
│             │    HTTP 응답              │             │
│   서버      │ ─────────────────────────▶│ 사용자      │
│             │    Set-Cookie: SID=31d4d  │ 에이전트    │
│             │                           │             │
│             │    HTTP 요청              │             │
│             │ ◀─────────────────────────│             │
│             │    Cookie: SID=31d4d      │             │
└─────────────┘                           └─────────────┘
```

상태 저장: 원본 서버는 HTTP 응답에 Set-Cookie 헤더를 포함시켜 상태를 사용자 에이전트에 저장합니다.

상태 반환: 후속 요청에서 사용자 에이전트는 이전에 수신한 Set-Cookie 헤더의 쿠키를 Cookie 요청 헤더에 포함시켜 반환합니다.

### 3.2 기본 규칙

- 원본 서버는 모든 응답에 Set-Cookie를 자유롭게 포함할 수 있습니다(MAY)
- 사용자 에이전트는 100-레벨 상태 코드 응답의 Set-Cookie를 무시할 수 있지만(MAY), 다른 응답의 Set-Cookie는 반드시 처리해야 합니다(MUST)
- 원본 서버는 단일 응답에 여러 Set-Cookie 헤더 필드를 포함할 수 있습니다(MAY)
- 여러 Set-Cookie 필드를 단일 헤더 필드로 병합하지 않아야 합니다(MUST NOT) - 헤더 폴딩 방지

### 3.3 실제 예제

#### 예제 1: 세션 식별자 설정

```http
== 서버 → 사용자 에이전트 ==
Set-Cookie: SID=31d4d96e407aad42

== 사용자 에이전트 → 서버 ==
Cookie: SID=31d4d96e407aad42
```

서버가 세션 ID를 설정하고, 사용자 에이전트가 후속 요청에서 이를 반환합니다.

#### 예제 2: Domain 및 Path 속성 사용

```http
== 서버 → 사용자 에이전트 ==
Set-Cookie: SID=31d4d96e407aad42; Path=/; Domain=example.com

== 사용자 에이전트 → 서버 ==
Cookie: SID=31d4d96e407aad42
```

`Path=/`와 `Domain=example.com`으로 쿠키의 범위를 지정합니다.

#### 예제 3: 여러 쿠키 저장

```http
== 서버 → 사용자 에이전트 ==
Set-Cookie: SID=31d4d96e407aad42; Path=/; Secure; HttpOnly
Set-Cookie: lang=en-US; Path=/; Domain=example.com

== 사용자 에이전트 → 서버 ==
Cookie: SID=31d4d96e407aad42; lang=en-US
```

서버가 두 개의 쿠키를 설정하고, 사용자 에이전트가 두 쿠키를 모두 반환합니다.

#### 예제 4: 쿠키 만료 및 삭제

```http
== 서버 → 사용자 에이전트 ==
Set-Cookie: lang=; Expires=Sun, 06 Nov 1994 08:49:37 GMT
```

과거 날짜로 Expires를 설정하여 쿠키를 삭제합니다.

---

## 4. 서버 요구사항 (Server Requirements)

이 섹션은 Cookie 및 Set-Cookie 헤더의 구문과 의미에 대한 "잘 동작하는(well-behaved)" 프로파일을 설명합니다.

### 4.1 Set-Cookie 헤더

#### 4.1.1 구문 (Syntax)

ABNF 문법:

```abnf
set-cookie-header = "Set-Cookie:" SP set-cookie-string
set-cookie-string = cookie-pair *( ";" SP cookie-av )
cookie-pair       = cookie-name "=" cookie-value
cookie-name       = token
cookie-value      = *cookie-octet / ( DQUOTE *cookie-octet DQUOTE )
cookie-octet      = %x21 / %x23-2B / %x2D-3A / %x3C-5B / %x5D-7E
                    ; US-ASCII 문자 중 CTL, 공백, DQUOTE, 콤마,
                    ; 세미콜론, 백슬래시 제외

token             = <RFC 2616의 token 정의>

cookie-av         = expires-av / max-age-av / domain-av /
                    path-av / secure-av / httponly-av /
                    extension-av
expires-av        = "Expires=" sane-cookie-date
sane-cookie-date  = <RFC 1123의 rfc1123-date>
max-age-av        = "Max-Age=" non-zero-digit *DIGIT
                    ; 0이 아닌 양의 정수
non-zero-digit    = %x31-39
                    ; 1-9
domain-av         = "Domain=" domain-value
domain-value      = <RFC 1034의 subdomain>
path-av           = "Path=" path-value
path-value        = <CTL 또는 ";"를 제외한 모든 CHAR>
secure-av         = "Secure"
httponly-av       = "HttpOnly"
extension-av      = <CTL 또는 ";"를 제외한 모든 CHAR>
```

cookie-value 허용 문자:

| 코드 범위 | 설명 |
|-----------|------|
| `%x21` | ! |
| `%x23-2B` | # $ % & ' ( ) * + |
| `%x2D-3A` | - . / 0-9 : |
| `%x3C-5B` | < = > ? @ A-Z [ |
| `%x5D-7E` | ] ^ _ ` a-z { \| } ~ |

제외되는 문자: CTL(제어 문자), 공백, 큰따옴표("), 콤마(,), 세미콜론(;), 백슬래시(\\)

#### 4.1.2 의미 (Semantics) - 비종료

##### 4.1.2.1 Expires 속성

목적: 쿠키의 최대 수명을 절대 날짜로 지정

```http
Set-Cookie: id=a3fWa; Expires=Wed, 09 Jun 2021 10:18:14 GMT
```

동작:
- 사용자 에이전트는 지정된 날짜/시간이 지나면 쿠키를 제거
- Expires가 없고 Max-Age도 없으면 "세션 쿠키"가 됨 (브라우저 종료 시 삭제)
- 서버는 명세와 다른 구문을 사용하는 것에 의존하지 않아야 합니다(SHOULD NOT)

##### 4.1.2.2 Max-Age 속성

목적: 쿠키의 최대 수명을 초 단위로 지정

```http
Set-Cookie: id=a3fWa; Max-Age=2592000
```

동작:
- 쿠키가 수신된 시간으로부터 지정된 초가 지나면 만료
- 0 또는 음수 값: 쿠키 즉시 삭제 (과거 만료 시간 설정)
- Expires보다 우선: 두 속성이 모두 있으면 Max-Age가 우선

참고: 일부 레거시 사용자 에이전트는 Max-Age를 지원하지 않습니다.

##### 4.1.2.3 Domain 속성

목적: 쿠키가 전송될 호스트를 지정

```http
Set-Cookie: id=a3fWa; Domain=example.com
```

동작:
- 생략 시: 현재 호스트에만 쿠키 적용 (하위 도메인 제외)
- 지정 시: 지정된 도메인과 모든 하위 도메인에 쿠키 적용

예시:

| Domain 속성 | 쿠키 전송 대상 |
|-------------|----------------|
| 생략 | `example.com`만 |
| `Domain=example.com` | `example.com`, `www.example.com`, `api.example.com` 등 |

보안 고려사항:
- 서버는 자신의 도메인 또는 상위 도메인만 지정 가능
- 예: `www.example.com`은 `Domain=example.com` 지정 가능, `Domain=other.com`은 거부됨

##### 4.1.2.4 Path 속성

목적: 쿠키가 적용되는 URL 경로 범위 지정

```http
Set-Cookie: id=a3fWa; Path=/docs
```

동작:
- 지정된 경로와 그 하위 경로의 요청에만 쿠키 포함
- 기본값: Set-Cookie를 보낸 URL의 디렉토리 경로

예시:

| Path 속성 | 쿠키 전송 경로 |
|-----------|----------------|
| `Path=/` | 모든 경로 |
| `Path=/docs` | `/docs`, `/docs/`, `/docs/Web/` 등 |
| `Path=/docs` | `/` 또는 `/other/`에는 전송 안 됨 |

주의: Path 속성은 보안 기능이 아닙니다. 동일 출처의 다른 경로에서 JavaScript로 쿠키에 접근할 수 있습니다.

##### 4.1.2.5 Secure 속성

목적: 보안(암호화된) 연결을 통해서만 쿠키 전송

```http
Set-Cookie: id=a3fWa; Secure
```

동작:
- Secure 속성이 있으면 HTTPS 요청에서만 쿠키 전송
- HTTP(비암호화) 연결에서는 쿠키가 포함되지 않음

보안 고려사항:
- Secure 속성만으로는 쿠키의 완전한 보안을 보장하지 않음
- 네트워크 공격자가 HTTP를 통해 쿠키를 덮어쓸 수 있음 (Section 8.6 참조)

##### 4.1.2.6 HttpOnly 속성

목적: JavaScript 등 비HTTP API를 통한 쿠키 접근 차단

```http
Set-Cookie: id=a3fWa; HttpOnly
```

동작:
- HttpOnly 쿠키는 `document.cookie` API로 접근 불가
- HTTP 요청 시에만 서버로 전송

보안 이점:
- XSS(Cross-Site Scripting) 공격으로 인한 세션 하이재킹 완화
- 악성 스크립트가 세션 쿠키를 읽어 공격자에게 전송하는 것을 방지

예시:

```http
Set-Cookie: sessionId=abc123; HttpOnly; Secure; Path=/
```

#### 4.1.3 서버 권장사항

서버가 따라야 하는 권장사항:

1. 동일한 속성명 중복 사용 금지
   ```http
   ❌ Set-Cookie: id=a; Path=/; Path=/docs
   ✅ Set-Cookie: id=a; Path=/docs
   ```

2. 단일 응답에서 동일한 쿠키명 중복 사용 자제
   ```http
   ❌ Set-Cookie: id=first
      Set-Cookie: id=second
   ✅ Set-Cookie: id=final
   ```

### 4.2 Cookie 헤더

사용자 에이전트는 저장된 쿠키를 Cookie 헤더에 포함시켜 서버로 전송합니다.

ABNF 문법:

```abnf
cookie-header = "Cookie:" OWS cookie-string OWS
cookie-string = cookie-pair *( ";" SP cookie-pair )
```

특징:
- 이름/값 쌍만 포함 (속성 정보 제외)
- 만료 시간, Domain, Path 등의 속성은 전송되지 않음
- 여러 쿠키는 세미콜론과 공백(`; `)으로 구분

예시:

```http
Cookie: SID=31d4d96e407aad42; lang=en-US
```

---

## 5. 사용자 에이전트 요구사항 (User Agent Requirements)

이 섹션은 사용자 에이전트가 Set-Cookie 헤더를 처리하고 Cookie 헤더를 생성하는 방법을 명시합니다.

### 5.1 하위 구성요소 알고리즘

#### 5.1.1 날짜 파싱 (Dates)

쿠키 날짜를 파싱하는 알고리즘:

입력: cookie-date 문자열
출력: 날짜 또는 실패

단계:

1. 날짜 토큰 분리
   - 구분자: 0x09(탭), 0x20-0x2F, 0x3B-0x40, 0x5B-0x60, 0x7B-0x7E
   - 각 토큰에서 다음을 추출:
     - 시간: `hh:mm:ss` 형식 (0-23 : 0-59 : 0-59)
     - 일: 1-31
     - 월: Jan, Feb, Mar, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec
     - 연도: 2-4자리 숫자

2. 연도 정규화
   ```
   if 70 ≤ year ≤ 99:
       year += 1900
   else if 0 ≤ year ≤ 69:
       year += 2000
   ```

3. 유효성 검사
   - 일, 월, 연도, 시간 모두 파싱되어야 함
   - 일: 1 ≤ day ≤ 31
   - 연도 ≥ 1601
   - 시간: 0 ≤ hour ≤ 23, 0 ≤ minute ≤ 59, 0 ≤ second ≤ 59

#### 5.1.2 정규화된 호스트 이름 (Canonicalized Host Names)

호스트 이름을 소문자 ASCII 레이블의 시퀀스로 변환합니다.

국제화 도메인 이름(IDN) 처리:
- 사용자 에이전트는 IDNA2008 (RFC 5891)을 구현해야 합니다(SHOULD)
- IDNA2008이 불가능한 경우 IDNA2003 구현

#### 5.1.3 도메인 매칭 (Domain Matching)

정의: 문자열 A가 도메인 문자열 B와 "도메인 일치"하는 경우:

```
A와 B가 도메인 일치하려면:
1. A와 B가 동일 (대소문자 무관), 또는
2. 다음 모든 조건 충족:
   - A가 B의 접미사
   - A의 B와 일치하지 않는 마지막 문자가 "."
   - A가 IP 주소가 아님
```

예시:

| A | B | 도메인 일치? |
|---|---|-------------|
| `www.example.com` | `example.com` | 예 |
| `example.com` | `example.com` | 예 |
| `www.example.com` | `www.example.com` | 예 |
| `example.com` | `www.example.com` | 아니오 |
| `192.168.1.1` | `168.1.1` | 아니오 (IP 주소) |

#### 5.1.4 경로 매칭 (Paths)

기본 경로 결정 알고리즘:

```
1. uri-path가 비어있거나 "/"로 시작하지 않으면:
   → 기본 경로 = "/"
2. uri-path에 "/" 문자가 하나만 있으면:
   → 기본 경로 = "/"
3. 그 외:
   → 기본 경로 = uri-path의 처음부터 마지막 "/"의 직전 문자까지
```

경로 매칭 규칙:

요청 경로가 쿠키 경로와 일치하는 경우:
1. 쿠키 경로와 요청 경로가 동일
2. 쿠키 경로가 요청 경로의 접두사이고, 쿠키 경로의 마지막 문자가 "/"
3. 쿠키 경로가 요청 경로의 접두사이고, 요청 경로에서 쿠키 경로 다음 문자가 "/"

예시:

| 쿠키 경로 | 요청 경로 | 일치? |
|-----------|-----------|-------|
| `/` | `/anything` | 예 |
| `/docs` | `/docs` | 예 |
| `/docs` | `/docs/` | 예 |
| `/docs` | `/docs/Web` | 예 |
| `/docs` | `/docsweb` | 아니오 |

### 5.2 Set-Cookie 헤더 처리

사용자 에이전트가 Set-Cookie 헤더를 처리하는 알고리즘:

중요: 사용자 에이전트는 Set-Cookie 헤더를 완전히 무시할 수 있습니다(MAY). 예: 제3자 요청 차단 정책

#### 5.2.1 파싱 알고리즘

```
1. name-value-pair와 unparsed-attributes 분리
   - 첫 번째 ";" 문자 위치 찾기
   - ";" 이전 = name-value-pair
   - ";" 이후 = unparsed-attributes

2. name-value-pair에서 이름과 값 추출
   - 첫 번째 "=" 문자 위치 찾기
   - "=" 없으면: 전체 문자열 무시
   - "=" 이전 = name (선행/후행 공백 제거)
   - "=" 이후 = value (선행/후행 공백 제거)

3. name이 비어있으면: 전체 문자열 무시

4. 속성 파싱 (unparsed-attributes)
   - 첫 번째 ";" 제거
   - 다음 ";"까지를 cookie-av로 추출
   - "=" 있으면: 이전 = 속성명, 이후 = 속성값
   - "=" 없으면: 전체 = 속성명, 속성값 = 빈 문자열
```

#### 5.2.2 속성별 처리

Expires 속성:
```
1. 속성값을 날짜로 파싱
2. 파싱 실패: 속성 무시
3. 파싱 성공: expiry-time을 파싱된 결과로 설정
```

Max-Age 속성:
```
1. 첫 문자가 DIGIT 또는 "-"가 아니면: 무시
2. 나머지 문자 중 비DIGIT이 있으면: 무시
3. delta-seconds = 숫자 값
4. delta-seconds ≤ 0: expiry-time = 가장 이른 가능한 날짜
5. delta-seconds > 0: expiry-time = 현재시간 + delta-seconds
```

Domain 속성:
```
1. 속성값이 비어있으면: 무시
2. 첫 문자가 "."이면: 제거
3. 속성값을 소문자로 변환
4. cookie-domain = 변환된 값
```

Path 속성:
```
1. 속성값이 비어있거나 "/"로 시작하지 않으면:
   → cookie-path = 기본 경로
2. 그 외:
   → cookie-path = 속성값
```

Secure 속성:
```
→ secure-only-flag = true
```

HttpOnly 속성:
```
→ http-only-flag = true
```

### 5.3 저장 모델 (Storage Model)

사용자 에이전트는 각 쿠키에 대해 다음 필드를 유지합니다:

| 필드 | 설명 |
|------|------|
| name | 쿠키 이름 |
| value | 쿠키 값 |
| expiry-time | 만료 시간 |
| domain | 적용 도메인 |
| path | 적용 경로 |
| creation-time | 생성 시간 |
| last-access-time | 마지막 접근 시간 |
| persistent-flag | 영속 여부 |
| host-only-flag | 호스트 전용 여부 |
| secure-only-flag | 보안 전용 여부 |
| http-only-flag | HTTP 전용 여부 |

#### 5.3.1 쿠키 저장 알고리즘 (12단계)

1단계: 무시 가능한 경우
```
사용자 에이전트는 수신된 쿠키를 무시할 수 있음(MAY):
- 제3자 요청인 경우
- 쿠키 크기가 한도 초과인 경우
```

2-3단계: 쿠키 생성
```
새 쿠키 생성:
- name, value 설정
- creation-time = 현재 시간
- last-access-time = 현재 시간
```

4-5단계: 만료 시간 결정
```
if Max-Age 속성 존재:
    persistent-flag = true
    expiry-time = Max-Age 기반 계산
else if Expires 속성 존재:
    persistent-flag = true
    expiry-time = Expires 값
else:
    persistent-flag = false
    expiry-time = 세션 종료 시
```

6단계: Domain 속성 처리
```
if Domain 속성 없음:
    host-only-flag = true
    domain = request-host (정규화)
else:
    if domain이 공용 접미사(public suffix):
        → 쿠키 무시 (또는 host-only로 처리)
    if request-host가 domain과 도메인 일치하지 않음:
        → 쿠키 무시
    host-only-flag = false
    domain = Domain 속성값
```

7단계: Path 속성 처리
```
if Path 속성 존재:
    path = Path 속성값
else:
    path = request-uri의 기본 경로
```

8-9단계: 플래그 설정
```
secure-only-flag = Secure 속성 존재 여부
http-only-flag = HttpOnly 속성 존재 여부
```

10단계: HttpOnly 검증
```
if http-only-flag == true && 비HTTP API에서 수신:
    → 쿠키 무시
```

11단계: 기존 쿠키 확인
```
if 동일한 (name, domain, path)의 기존 쿠키 존재:
    if 기존 쿠키의 http-only-flag == true && 비HTTP API에서 수신:
        → 쿠키 무시
    creation-time = 기존 쿠키의 creation-time
    기존 쿠키 제거
```

12단계: 쿠키 삽입
```
쿠키 저장소에 새 쿠키 삽입
```

#### 5.3.2 쿠키 제거 정책

만료된 쿠키 제거:
```
사용자 에이전트는 언제든지(at any time) 만료된 쿠키를 제거해야 합니다(MUST):
- 만료 시간이 지난 쿠키가 존재하면 제거
```

저장소 한도 초과 시 제거 순서:

```
1. 만료된 쿠키 (최우선)
2. 도메인당 한도 초과 쿠키
3. 전체 한도 초과 쿠키 (최후 수단)

동일 우선순위 내에서:
→ last-access-time이 가장 오래된 쿠키부터 제거
```

세션 종료 시:
```
persistent-flag == false인 모든 쿠키 제거
```

### 5.4 Cookie 헤더 생성

사용자 에이전트가 HTTP 요청에 Cookie 헤더를 포함시키는 알고리즘:

원칙: 사용자 에이전트는 단일 Cookie 헤더 필드만 첨부해야 합니다(MUST)

#### 5.4.1 Cookie-String 생성 알고리즘 (5단계)

1단계: 적격 쿠키 선별

```
cookie-list에 포함할 쿠키 조건:

1. 도메인 매칭:
   - host-only-flag == true: request-host와 domain이 정확히 일치
   - host-only-flag == false: request-host가 domain과 도메인 일치

2. 경로 매칭:
   - request-uri 경로가 쿠키의 path와 일치

3. 보안 검증:
   - secure-only-flag == true: 보안 프로토콜(HTTPS) 필요

4. HTTP 전용:
   - http-only-flag == true: HTTP API에서만 포함
```

2단계: 쿠키 정렬 (권장)

```
정렬 기준:
1. path가 긴 쿠키를 먼저 나열
2. 동일 path: creation-time이 이른 쿠키를 먼저 나열
```

3단계: 접근 시간 업데이트

```
cookie-list의 각 쿠키에 대해:
    last-access-time = 현재 시간
```

4단계: 쿠키 직렬화

```
cookie-string = ""
for each cookie in cookie-list:
    cookie-string += cookie.name + "=" + cookie.value
    if 다음 쿠키 존재:
        cookie-string += "; "
```

5단계: 헤더 생성

```
if cookie-string이 비어있지 않음:
    Cookie 헤더 = "Cookie: " + cookie-string
```

#### 5.4.2 중요 참고사항

- 정렬 의존 금지: 서버는 쿠키 순서에 의존해서는 안 됩니다(SHOULD NOT)
- 동일 이름 다중 쿠키: 동일한 name으로 여러 쿠키가 있을 수 있으며, 순서는 예측 불가능합니다
- 비 ASCII 처리: cookie-string은 옥텟 시퀀스이며, 서버가 UTF-8로 디코딩할 수 있습니다

---

## 6. 구현 고려사항 (Implementation Considerations)

### 6.1 한도 (Limits)

일반 용도의 사용자 에이전트에 대한 최소 권장 사항:

| 항목 | 최소 요구량 |
|------|------------|
| 쿠키당 크기 | 4096 바이트 (이름 + 값 + 속성의 합) |
| 도메인당 쿠키 수 | 50개 |
| 전체 쿠키 수 | 3000개 |

서버 권장사항:

- 쿠키 사용을 최소화하여 구현 한도에 도달하는 것을 방지
- 쿠키가 모든 요청에 포함되므로 네트워크 대역폭 절약

### 6.2 애플리케이션 프로그래밍 인터페이스

Cookie 및 Set-Cookie 헤더의 이상한 구문은 많은 플랫폼에서 문자열 기반 API를 사용하기 때문입니다. 이로 인해 프로그래머들이 구문을 직접 생성하고 파싱하게 되어 상호운용성 문제가 발생했습니다.

권장사항: 플랫폼은 더 의미론적인 API를 제공해야 합니다

```
❌ 잘못된 API:
   setCookie("Expires=Wed, 09 Jun 2021 10:18:14 GMT")

✅ 권장 API:
   setCookie({
       name: "id",
       value: "abc123",
       expires: Date("2021-06-09T10:18:14Z")
   })
```

### 6.3 IDNA 종속성 및 마이그레이션

국제화 도메인 이름 처리를 위해:

| 버전 | 권장 수준 |
|------|----------|
| IDNA2008 (RFC 5891) | SHOULD 구현 |
| IDNA2003 | IDNA2008이 불가능한 경우 사용 |

IDNA2003과 IDNA2008 간 전환 기간 동안 적절한 도메인 이름 처리를 위해 필요합니다.

---

## 7. 개인정보 고려사항 (Privacy Considerations)

### 7.1 제3자 쿠키 (Third-Party Cookies)

쿠키는 서버가 사용자를 추적할 수 있게 해주어 자주 비판받습니다. 추적은 영속 쿠키가 세션 간에 호스트 간에 공유될 때 발생합니다.

특히 우려되는 것은 "제3자" 쿠키입니다:

```
사용자가 example.com 방문
    │
    ├── example.com의 HTML 문서 로드
    │
    └── HTML에 포함된 광고 네트워크 리소스 요청
            │
            └── ads.tracker.com에서 이미지/스크립트 로드
                    │
                    └── ads.tracker.com이 쿠키 설정
                            │
                            └── 사용자가 other-site.com 방문
                                    │
                                    └── 동일한 ads.tracker.com 리소스 요청
                                            │
                                            └── 이전에 설정한 쿠키 전송
                                                    │
                                                    └── 추적 완료!
```

사용자 에이전트의 대응:

| 정책 | 설명 |
|------|------|
| 제3자 Cookie 헤더 차단 | 제3자 요청에 Cookie 헤더 미포함 |
| 제3자 Set-Cookie 차단 | 제3자 응답의 Set-Cookie 헤더 무시 |

중요 한계: 제3자 쿠키 차단이 완벽한 방어책은 아닙니다.

> "두 개의 협력하는 서버는 쿠키를 전혀 사용하지 않고도 동적 URL에 식별 정보를 주입하여 사용자를 추적할 수 있습니다."

### 7.2 사용자 컨트롤 (User Controls)

사용자 에이전트는 쿠키 관리 메커니즘을 제공해야 합니다(SHOULD):

권장 기능:

| 기능 | 설명 |
|------|------|
| 시간 기반 삭제 | 특정 기간에 생성된 쿠키 삭제 |
| 도메인 기반 삭제 | 특정 도메인의 쿠키 삭제 |
| 쿠키 비활성화 | 쿠키 기능 완전 끄기 |
| 비공개 브라우징 | 모든 쿠키를 세션 전용으로 처리 |

쿠키 비활성화 시:

```
쿠키가 비활성화되면:
- 사용자 에이전트는 Cookie 헤더를 포함하지 않아야 합니다(MUST NOT)
- 나가는 HTTP 요청에 Cookie 헤더 미포함
```

### 7.3 만료 날짜 (Expiration Dates)

서버 권장사항:

- 쿠키의 목적에 따라 합리적인 만료 기간 선택
- 임의로 먼 미래 날짜 설정 자제
- 사용자 개인정보를 고려한 쿠키 설계

```
❌ 나쁜 예:
   Set-Cookie: track=abc; Expires=Tue, 19 Jan 2038 03:14:07 GMT

✅ 좋은 예:
   Set-Cookie: session=abc; Max-Age=3600  // 1시간
   Set-Cookie: prefs=abc; Max-Age=2592000 // 30일
```

---

## 8. 보안 고려사항 (Security Considerations)

### 8.1 개요

쿠키에는 여러 고유한 보안 취약점이 있습니다:

| 취약점 | 영향 |
|--------|------|
| Ambient Authority | 개발자가 인증을 위해 ambient authority에 의존하게 되어 CSRF 등의 공격에 취약 |
| 세션 고정 | 세션 식별자를 쿠키에 저장할 때 세션 고정 취약점 발생 가능 |
| 네트워크 공격 | HTTPS 사용에도 불구하고 쿠키 프로토콜의 취약점으로 인해 네트워크 공격자가 쿠키 획득 또는 변조 가능 |

### 8.2 Ambient Authority

쿠키 인증 방식은 ambient authority(환경 권한)에 해당합니다:

```
문제점:
원격 당사자가 HTTP 리다이렉트나 HTML 폼을 통해
사용자 에이전트에서 HTTP 요청을 발행할 수 있습니다.

사용자 에이전트는 쿠키 내용을 모르는 원격 당사자의 요청에도
쿠키를 첨부하므로, 의도하지 않은 행동이 수행될 수 있습니다.
```

CSRF(Cross-Site Request Forgery) 예시:

```html
<!-- 악성 사이트의 코드 -->
<img src="https://bank.com/transfer?to=attacker&amount=10000">

<!-- 사용자가 bank.com에 로그인 상태이면,
     브라우저는 자동으로 bank.com 쿠키를 포함하여 요청 전송 -->
```

완화 방법:

- URL을 capability(능력)으로 취급하여 지정과 권한을 결합
- 예측 불가능한 토큰을 URL이나 폼 필드에 포함

### 8.3 평문 전송 (Clear Text)

보안 채널(TLS)을 통하지 않으면:

```
위험:
┌─────────────┐    평문    ┌─────────────┐
│   서버      │ ◀────────▶│ 사용자      │
└─────────────┘           │ 에이전트    │
                          └─────────────┘
                               ▲
                               │ 도청 가능
                               │
                          ┌─────────────┐
                          │   공격자    │
                          └─────────────┘
```

위험 요소:

| 공격자 유형 | 가능한 공격 |
|-------------|-------------|
| 수동적 네트워크 공격자 | Cookie/Set-Cookie 헤더의 민감 정보 도청 |
| 활성 중간자 공격자 | 헤더 내용 변조 |
| 악성 클라이언트 | Cookie 헤더 전송 전 수정 |

권장 보안 조치:

1. 쿠키 내용 암호화 및 서명
2. HTTPS를 통해서만 쿠키 사용
3. 모든 쿠키에 Secure 속성 설정

```http
✅ 보안 권장:
Set-Cookie: session=encrypted_value; Secure; HttpOnly
```

### 8.4 세션 식별자 (Session Identifiers)

권장 방식:

```
서버 측 세션 저장소:
┌─────────────────────────────────────────┐
│  세션 ID  │    세션 데이터              │
├───────────┼─────────────────────────────┤
│  abc123   │  user_id=42, role=admin...  │
└───────────┴─────────────────────────────┘

쿠키에는 세션 ID만 저장:
Set-Cookie: sessionId=abc123; Secure; HttpOnly
```

장점:
- 공격자가 쿠키 내용을 알아도 직접적 피해 제한
- 세션 데이터가 클라이언트에 노출되지 않음

세션 고정 공격 (Session Fixation):

```
공격 시나리오:
1. 공격자가 서버에서 유효한 세션 ID(예: SID=abc123) 획득
2. 공격자가 피해자의 브라우저에 해당 세션 ID를 심음
   (예: 악성 링크, XSS 등)
3. 피해자가 해당 세션 ID로 서버에 로그인
4. 공격자가 동일한 세션 ID로 서버에 접근
5. 공격자가 피해자의 인증된 세션 사용 가능!
```

완화 방법:
- 인증 후 새 세션 ID 발급
- 세션 ID를 쿠키 외의 방법으로도 검증

### 8.5 약한 기밀성 (Weak Confidentiality)

쿠키는 여러 측면에서 격리를 제공하지 않습니다:

#### 8.5.1 포트 격리 부재

```
example.com:80  ◄──────────────► 쿠키 공유 ◄──────────────► example.com:8080
                     │
                     ▼
         두 포트의 서비스가 동일한 쿠키에 접근 가능
```

위험:
- 한 포트의 서비스가 읽을 수 있는 쿠키는 다른 포트의 서비스도 읽을 수 있음
- 권장사항: 서로 다르게 신뢰하는 서비스를 동일 호스트의 다른 포트에서 실행하면서 보안 민감 정보를 쿠키에 저장하지 않아야 합니다(SHOULD NOT)

#### 8.5.2 스킴 격리 부재

```
http://example.com ◄──────────────► 쿠키 공유 ◄──────────────► https://example.com
                          │
                          ▼
             Secure 속성 없으면 HTTP에서도 전송됨
```

#### 8.5.3 경로 격리 불완전

```
/admin  ◄──────────────► /public
         │
         ▼
    동일 출처이므로 JavaScript로 상호 접근 가능
```

중요: Path 속성은 보안 기능이 아닙니다!

### 8.6 약한 무결성 (Weak Integrity)

#### 8.6.1 형제 도메인 간 무결성 부재

```
foo.example.com이 설정:
Set-Cookie: session=abc; Domain=example.com

bar.example.com이 수신:
Cookie: session=abc

문제: bar.example.com은 이 쿠키가 자신이 설정한 것인지,
      foo.example.com이 설정한 것인지 구별할 수 없음
```

#### 8.6.2 활성 네트워크 공격자의 쿠키 주입

```
공격자가 HTTP를 가장하여 HTTPS 연결에 쿠키 주입:

1. 사용자가 https://secure.example.com 접속
2. 공격자가 DNS/네트워크 조작으로 HTTP 트래픽 가로챔
3. http://example.com 응답에 쿠키 설정:
   Set-Cookie: session=malicious; Domain=example.com
4. 해당 쿠키가 https://secure.example.com에도 전송됨!
```

#### 8.6.3 쿠키 재전송 방지 불가

암호화와 서명으로도 쿠키 재전송(replay)을 방지할 수 없습니다:

```
공격자가 과거에 캡처한 유효한 쿠키를 재전송 가능
서버는 이것이 정당한 요청인지 재전송인지 구별 불가
```

### 8.7 DNS 의존성 (Reliance on DNS)

쿠키는 DNS 보안에 의존합니다:

```
DNS가 손상되면:
- 공격자가 도메인을 가장할 수 있음
- 쿠키가 공격자에게 전송될 수 있음
- 쿠키 프로토콜이 필요한 보안 속성을 제공하지 못할 수 있음
```

완화 방법:
- DNSSEC 사용
- HTTPS와 인증서 검증 함께 사용

---

## 9. SameSite 속성 (추가 참고)

> 참고: RFC 6265 원본에는 SameSite 속성이 포함되어 있지 않습니다.
> SameSite는 RFC 6265bis (draft)에서 추가되었으며, 현대 브라우저에서 널리 지원됩니다.

### 9.1 SameSite 속성 개요

SameSite 속성은 쿠키가 교차 사이트 요청에 포함될지 여부를 제어합니다:

```http
Set-Cookie: session=abc123; SameSite=Strict
Set-Cookie: session=abc123; SameSite=Lax
Set-Cookie: session=abc123; SameSite=None; Secure
```

### 9.2 SameSite 값

| 값 | 동작 |
|------|------|
| Strict | 동일 사이트 요청에서만 쿠키 전송 |
| Lax | 동일 사이트 + 최상위 네비게이션 GET 요청에서 쿠키 전송 |
| None | 모든 요청에 쿠키 전송 (Secure 필수) |

### 9.3 CSRF 방어

SameSite 속성은 CSRF 공격 완화에 효과적입니다:

```
Strict 모드:
- 외부 사이트에서 링크 클릭 시 쿠키 미전송
- 가장 강력한 보호이나, 사용자 경험 영향 가능

Lax 모드 (권장):
- 외부 사이트에서 링크 클릭 시 쿠키 전송 (GET만)
- POST/PUT/DELETE 요청에는 쿠키 미전송
- CSRF 공격의 대부분 차단하면서 사용성 유지
```

---

## 10. 요약

### 10.1 쿠키 속성 요약표

| 속성 | 목적 | 예시 |
|------|------|------|
| Expires | 절대 만료 시간 | `Expires=Wed, 09 Jun 2021 10:18:14 GMT` |
| Max-Age | 상대 만료 시간 (초) | `Max-Age=3600` |
| Domain | 쿠키 전송 도메인 | `Domain=example.com` |
| Path | 쿠키 전송 경로 | `Path=/docs` |
| Secure | HTTPS 전용 | `Secure` |
| HttpOnly | JavaScript 접근 차단 | `HttpOnly` |
| SameSite | 교차 사이트 제한 | `SameSite=Lax` |

### 10.2 보안 권장 쿠키 설정

```http
Set-Cookie: session=abc123; Path=/; Secure; HttpOnly; SameSite=Lax; Max-Age=3600
```

| 설정 | 이유 |
|------|------|
| `Secure` | HTTPS에서만 전송, 도청 방지 |
| `HttpOnly` | XSS로 인한 세션 하이재킹 방지 |
| `SameSite=Lax` | CSRF 공격 완화 |
| `Max-Age=3600` | 합리적인 만료 시간 |

### 10.3 서버 개발자 체크리스트

- [ ] 모든 민감한 쿠키에 `Secure` 속성 설정
- [ ] 세션 쿠키에 `HttpOnly` 속성 설정
- [ ] `SameSite` 속성 사용 (최소 `Lax`)
- [ ] 적절한 만료 시간 설정
- [ ] 쿠키 크기 최소화 (4KB 이하)
- [ ] 쿠키 개수 최소화
- [ ] 민감한 데이터는 쿠키에 직접 저장하지 않고 서버에 저장

### 10.4 사용자 에이전트 개발자 체크리스트

- [ ] Section 5의 처리 알고리즘 완전 구현
- [ ] 최소 한도 지원 (4096바이트/쿠키, 50쿠키/도메인, 3000쿠키 전체)
- [ ] 제3자 쿠키 제어 기능 제공
- [ ] 쿠키 관리 UI 제공
- [ ] 비공개 브라우징 모드 지원

---

## 참고 자료

- [RFC 6265 원문 (IETF)](https://datatracker.ietf.org/doc/html/rfc6265)
- [RFC 6265bis (SameSite 포함 draft)](https://datatracker.ietf.org/doc/html/draft-ietf-httpbis-rfc6265bis)
- [RFC 2119 - 요구사항 수준 키워드](https://datatracker.ietf.org/doc/html/rfc2119)
- [MDN Web Docs - HTTP Cookies](https://developer.mozilla.org/ko/docs/Web/HTTP/Cookies)

---

## 관련 RFC

| RFC | 제목 | 관계 |
|-----|------|------|
| RFC 2109 | HTTP State Management Mechanism | 폐기됨 (Historic) |
| RFC 2965 | HTTP State Management Mechanism | 폐기됨 (Historic) |
| RFC 6265bis | Cookies: HTTP State Management Mechanism | 후속 draft (SameSite 포함) |
| RFC 7230 | HTTP/1.1 Message Syntax and Routing | 참조 |
| RFC 5234 | ABNF for Syntax Specifications | ABNF 정의 |

---

*이 문서는 RFC 6265의 한국어 번역 및 정리본입니다.*

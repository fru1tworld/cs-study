# WHATWG URL Standard 완벽 가이드

## 목차

1. [개요](#1-개요)
2. [URL 구조](#2-url-구조)
3. [URL 파싱 알고리즘](#3-url-파싱-알고리즘)
4. [호스트 파싱](#4-호스트-파싱)
5. [Percent-encoding](#5-percent-encoding)
6. [URL 직렬화](#6-url-직렬화)
7. [Origin](#7-origin)
8. [URL API](#8-url-api)
9. [URLSearchParams API](#9-urlsearchparams-api)
10. [URL 패턴](#10-url-패턴)
11. [특수 스킴](#11-특수-스킴special-schemes)
12. [Blob URL](#12-blob-url)
13. [data: URL 처리](#13-data-url-처리)
14. [상대 URL 해석](#14-상대-url-해석)
15. [URL 등가성](#15-url-등가성equivalence)

---

## 1. 개요

### 1.1 URL Standard란

WHATWG URL Standard는 웹에서 사용되는 URL(Uniform Resource Locator)의 파싱, 직렬화, 조작에 관한 살아있는 표준(Living Standard)이다. 이 표준은 브라우저가 실제로 URL을 처리하는 방식을 정의하며, 모든 주요 웹 브라우저 벤더(Google, Mozilla, Apple, Microsoft)가 참여하여 유지보수한다.

URL은 웹의 가장 기본적인 요소 중 하나로, 리소스의 위치를 식별하고 접근하는 데 사용된다. 매일 수십억 개의 URL이 파싱되고 처리되므로, 이 동작의 정확한 정의가 필수적이다.

- 공식 문서: https://url.spec.whatwg.org/
- 유지보수 주체: WHATWG (Web Hypertext Application Technology Working Group)
- 문서 유형: Living Standard (지속적으로 업데이트되는 표준)

### 1.2 역사: RFC 3986/3987과의 관계

URL의 역사는 복잡하다. 초기에는 IETF(Internet Engineering Task Force)에서 관련 표준을 관리했다.

| 표준 | 연도 | 내용 |
|------|------|------|
| RFC 1738 | 1994 | 최초의 URL 명세 |
| RFC 2396 | 1998 | URI(Uniform Resource Identifier) 일반 구문 |
| RFC 2732 | 1999 | IPv6 주소에 대한 URL 형식 |
| RFC 3986 | 2005 | URI 일반 구문 (RFC 2396 대체) |
| RFC 3987 | 2005 | IRI(Internationalized Resource Identifier) |
| WHATWG URL | 2012~ | 브라우저 호환 URL 파싱 표준 |

RFC 3986은 URI의 이론적 구문을 정의하지만, 실제 브라우저 구현과 상당한 차이가 있었다. 예를 들어:

```
# RFC 3986에서는 유효하지 않지만 브라우저에서는 동작하는 URL들
http://example.com/path with spaces
http://example.com:80/  (기본 포트 명시)
HTTP://EXAMPLE.COM/  (대소문자 혼용)
http://example.com/foo/../bar  (경로 정규화)
```

### 1.3 왜 별도 표준이 필요한가

WHATWG URL Standard가 등장한 핵심 이유는 다음과 같다.

1) 브라우저 호환성 문제

RFC 3986은 이론적으로 정확하지만, 실제 웹에서 사용되는 URL 중 상당수가 RFC를 위반한다. 브라우저들은 이러한 "잘못된" URL도 합리적으로 처리해야 했고, 각 브라우저가 서로 다른 방식으로 처리하면서 호환성 문제가 발생했다.

2) 단일 파싱 알고리즘의 부재

RFC 3986은 URL의 구문만 정의할 뿐, 구체적인 파싱 알고리즘을 제공하지 않는다. WHATWG URL Standard는 상태 머신 기반의 구체적인 파싱 알고리즘을 정의하여 모든 구현이 동일한 결과를 내도록 한다.

3) 국제화 지원

RFC 3987(IRI)이 국제화를 다루지만, WHATWG URL Standard는 이를 통합하여 하나의 표준에서 처리한다. 도메인의 IDNA 처리, 비ASCII 문자의 percent-encoding 등을 포괄적으로 다룬다.

4) 웹 플랫폼 API 정의

`URL`, `URLSearchParams` 같은 JavaScript API를 표준에서 직접 정의하여, 웹 개발자가 프로그래밍 방식으로 URL을 조작할 수 있는 통일된 인터페이스를 제공한다.

```javascript
// WHATWG URL API를 사용한 URL 파싱
const url = new URL('https://user:pass@example.com:8080/path?q=1#frag');

console.log(url.protocol); // "https:"
console.log(url.username); // "user"
console.log(url.password); // "pass"
console.log(url.hostname); // "example.com"
console.log(url.port);     // "8080"
console.log(url.pathname); // "/path"
console.log(url.search);   // "?q=1"
console.log(url.hash);     // "#frag"
```

---

## 2. URL 구조

### 2.1 URL의 내부 표현

WHATWG URL Standard에서 URL은 단순한 문자열이 아니라 구조화된 객체(record)이다. 파싱된 URL은 다음 구성 요소를 가진다.

```
 https://user:password@www.example.com:443/path/to/resource?key=value#section
 |____| |__| |______| |_____________| |_| |________________| |_______| |_____|
   |      |      |           |         |          |              |         |
 scheme  user  password    host       port      path           query   fragment
```

### 2.2 각 구성 요소 상세

#### scheme (스킴)

URL의 프로토콜을 나타낸다. 항상 소문자 ASCII 영문자로 정규화된다.

```javascript
const url = new URL('HTTPS://example.com');
console.log(url.protocol); // "https:" (소문자로 정규화)

// 유효한 스킴: 영문자로 시작, 영문자/숫자/+/-/. 허용
// 유효: http, https, ftp, ssh+git, my.scheme
// 무효: 1http, -ftp
```

특수 스킴(special scheme)은 별도의 기본 포트와 처리 규칙이 있다:

| 스킴 | 기본 포트 |
|------|-----------|
| ftp | 21 |
| file | (없음) |
| http | 80 |
| https | 443 |
| ws | 80 |
| wss | 443 |

#### username (사용자 이름)

인증에 사용되는 사용자 이름이다. 기본값은 빈 문자열이다.

```javascript
const url = new URL('https://admin@example.com');
console.log(url.username); // "admin"

// 특수 문자는 percent-encoding 된다
const url2 = new URL('https://user%40name@example.com');
console.log(url2.username); // "user%40name"
```

#### password (비밀번호)

인증에 사용되는 비밀번호이다. 기본값은 빈 문자열이다. 보안상 URL에 비밀번호를 포함하는 것은 권장되지 않는다.

```javascript
const url = new URL('https://user:p%40ss@example.com');
console.log(url.password); // "p%40ss"

// username 없이 password만 설정할 수도 있다 (비표준적이지만 파싱 가능)
const url2 = new URL('https://:secret@example.com');
console.log(url2.username); // ""
console.log(url2.password); // "secret"
```

#### host (호스트)

리소스가 위치한 서버를 식별한다. 다음 형태 중 하나일 수 있다:

- 도메인(domain): `example.com`
- IPv4 주소: `192.168.1.1`
- IPv6 주소: `[::1]`
- 빈 호스트(empty host): `""` (file 스킴에서 사용)
- 불투명 호스트(opaque host): 특수 스킴이 아닌 경우

```javascript
// 도메인
const url1 = new URL('https://www.example.com/');
console.log(url1.host); // "www.example.com"

// IPv4
const url2 = new URL('https://127.0.0.1:8080/');
console.log(url2.host); // "127.0.0.1:8080"

// IPv6
const url3 = new URL('https://[::1]:8080/');
console.log(url3.host); // "[::1]:8080"

// hostname은 포트를 제외한 호스트
console.log(url2.hostname); // "127.0.0.1"
console.log(url3.hostname); // "[::1]"
```

#### port (포트)

네트워크 포트 번호이다. 기본 포트와 동일하면 빈 문자열로 표현된다.

```javascript
const url1 = new URL('https://example.com:443/');
console.log(url1.port); // "" (기본 포트이므로 생략)

const url2 = new URL('https://example.com:8443/');
console.log(url2.port); // "8443"

// 포트 범위: 0~65535
// 65536 이상은 파싱 실패
try {
  new URL('https://example.com:99999/');
} catch (e) {
  console.log(e.message); // Invalid URL
}
```

#### path (경로)

리소스의 경로를 나타낸다. 특수 스킴에서는 항상 `/`로 시작하며, `.`과 `..` 세그먼트가 정규화된다.

```javascript
const url1 = new URL('https://example.com/a/b/../c');
console.log(url1.pathname); // "/a/c" (..이 정규화됨)

const url2 = new URL('https://example.com/a/./b');
console.log(url2.pathname); // "/a/b" (.이 정규화됨)

// 비특수 스킴에서는 opaque path일 수 있다
const url3 = new URL('mailto:user@example.com');
console.log(url3.pathname); // "user@example.com"
```

#### query (쿼리)

`?` 뒤에 오는 키-값 쌍의 문자열이다. 기본값은 null이다.

```javascript
const url = new URL('https://example.com/search?q=hello&lang=ko');
console.log(url.search); // "?q=hello&lang=ko"

// URLSearchParams로 구조적 접근
console.log(url.searchParams.get('q'));    // "hello"
console.log(url.searchParams.get('lang')); // "ko"
```

#### fragment (프래그먼트)

`#` 뒤에 오는 문서 내 위치 식별자이다. 서버로 전송되지 않는다. 기본값은 null이다.

```javascript
const url = new URL('https://example.com/page#section-2');
console.log(url.hash); // "#section-2"

// fragment는 서버에 전송되지 않음
// fetch('https://example.com/page#section-2') 에서
// 실제 요청 URL은 'https://example.com/page'
```

### 2.3 URL 구성 요소 요약표

| 구성 요소 | 기본값 | 직렬화 시 접두사/접미사 | 예시 |
|-----------|--------|----------------------|------|
| scheme | (필수) | `:` 접미사 | `https` |
| username | `""` | `@` 앞 (password와 함께) | `user` |
| password | `""` | `:` 접두사, `@` 접미사 | `pass` |
| host | null | `//` 접두사 | `example.com` |
| port | null | `:` 접두사 | `8080` |
| path | `[]` 또는 `""` | `/`로 결합 | `/a/b/c` |
| query | null | `?` 접두사 | `key=value` |
| fragment | null | `#` 접두사 | `section` |

---

## 3. URL 파싱 알고리즘

### 3.1 상태 머신 개요

WHATWG URL 파싱 알고리즘은 유한 상태 머신(Finite State Machine)으로 구현된다. 입력 문자열을 한 문자씩 순회하면서, 현재 상태와 읽은 문자에 따라 다음 상태로 전이하고 URL의 각 구성 요소를 채워나간다.

```
[입력 문자열] → [전처리] → [상태 머신 파싱] → [URL 레코드 또는 실패]
```

전처리 단계에서는 다음을 수행한다:
- 선행/후행 C0 제어 문자 및 공백 제거
- 탭(`\t`)과 줄바꿈(`\n`, `\r`) 제거

```javascript
// 전처리 예시: 앞뒤 공백과 탭이 제거된다
const url = new URL('  \t https://example.com \n ');
console.log(url.href); // "https://example.com/"
```

### 3.2 주요 파싱 상태 상세

파싱 알고리즘에는 약 30개의 상태가 있다. 주요 상태를 순서대로 설명한다.

#### Scheme Start State

파싱의 시작 상태이다. 첫 번째 문자가 ASCII 영문자이면 소문자로 변환하고 버퍼에 추가한 후 Scheme State로 전이한다.

```
입력: "Https://example.com"
       ^
상태: Scheme Start State
동작: 'H' → 소문자 'h' → 버퍼에 추가 → Scheme State로 전이
```

첫 문자가 영문자가 아니고, state override가 없으면 No Scheme State로 전이한다.

#### Scheme State

스킴의 나머지 부분을 읽는다. ASCII 영문자, 숫자, `+`, `-`, `.`이 올 수 있다.

`:` 문자를 만나면:
- 버퍼에 모인 문자열이 스킴이 된다
- 특수 스킴인지 확인한다
- 이후 `//`가 오면 Authority State로, 그렇지 않으면 다른 적절한 상태로 전이한다

```
입력: "https://example.com"
       ^^^^^
상태: Scheme State
버퍼: "https"
':' 만남 → scheme = "https" → 특수 스킴 → "//" 확인 → Authority State
```

```javascript
// 스킴 파싱 예시
const url1 = new URL('custom+scheme://host');
console.log(url1.protocol); // "custom+scheme:"

// 유효하지 않은 스킴 문자
try {
  new URL('inv@lid://host');
} catch (e) {
  console.log('파싱 실패: 스킴에 @ 사용 불가');
}
```

#### Authority State

`//` 뒤에서 authority(인증 정보 + 호스트 + 포트) 파싱을 시작한다. `@` 문자를 발견하면 그 앞은 userinfo이고, 그 뒤부터가 호스트이다.

```
입력: "https://user:pass@example.com:8080/path"
              ^^^^^^^^^^^^^^^^^^^^^^^^
              authority 영역
```

#### Host State

호스트 문자열을 파싱한다. `[`를 만나면 IPv6 파싱 모드로 들어가고, `:`이나 `/`, `?`, `#`을 만나면 호스트 파싱을 종료한다.

```
입력: "https://example.com:8080/path"
              ^^^^^^^^^^^
              Host State에서 처리
```

#### Port State

`:` 이후의 포트 번호를 파싱한다. 숫자만 허용되며, 파싱 완료 시 기본 포트와 비교하여 동일하면 null로 설정한다.

```
입력: "https://example.com:8080/path"
                           ^^^^
                           Port State에서 처리
```

```javascript
// 기본 포트는 직렬화 시 생략된다
const url = new URL('https://example.com:443/path');
console.log(url.port); // "" (기본 포트이므로 빈 문자열)
console.log(url.href); // "https://example.com/path" (포트 생략)
```

#### Path Start State / Path State

경로를 파싱한다. 특수 스킴에서는 `\`도 `/`와 동일하게 경로 구분자로 취급한다.

```javascript
// 백슬래시가 슬래시로 정규화된다 (특수 스킴에서만)
const url1 = new URL('https://example.com/a\\b\\c');
console.log(url1.pathname); // "/a/b/c"

// 비특수 스킴에서는 백슬래시가 그대로 유지된다
const url2 = new URL('custom://host/a\\b\\c');
console.log(url2.pathname); // "/a%5Cb%5Cc"
```

경로 세그먼트에서 `.`과 `..`은 특별히 처리된다:

```javascript
// 단일 점(.) - 현재 디렉토리, 세그먼트 제거
const url1 = new URL('https://example.com/a/./b');
console.log(url1.pathname); // "/a/b"

// 이중 점(..) - 상위 디렉토리, 이전 세그먼트 제거
const url2 = new URL('https://example.com/a/b/../c');
console.log(url2.pathname); // "/a/c"

// 연속 점은 일반 세그먼트로 취급
const url3 = new URL('https://example.com/a/.../b');
console.log(url3.pathname); // "/a/.../b"

// 단일 점과 이중 점의 변형도 인식한다
// %2e, %2E 등도 . 으로 취급
const url4 = new URL('https://example.com/a/%2e%2e/b');
console.log(url4.pathname); // "/b"
```

#### Query State

`?` 이후의 쿼리 문자열을 파싱한다. `#`을 만나면 쿼리 파싱 종료 후 Fragment State로 전이한다.

특수 스킴과 비특수 스킴에서 percent-encoding 규칙이 다르다:
- 특수 스킴: special-query percent-encode set 사용
- 비특수 스킴: query percent-encode set 사용

```javascript
const url = new URL("https://example.com/search?q=hello world&lang=한국어");
console.log(url.search); // "?q=hello%20world&lang=%ED%95%9C%EA%B5%AD%EC%96%B4"
```

#### Fragment State

`#` 이후의 프래그먼트를 파싱한다. 입력 끝까지 읽는다.

```javascript
const url = new URL('https://example.com/page#섹션-1');
console.log(url.hash); // "#%EC%84%B9%EC%85%98-1"
```

### 3.3 파싱 상태 전이 흐름도

```
Scheme Start State
       │
       ▼
  Scheme State ─────────────────────────────────┐
       │                                         │
       │ (특수 스킴 + "//")                      │ (비특수 스킴, "//" 없음)
       ▼                                         ▼
  Authority State                          Opaque Path State
       │                                         │
       │ (@ 발견 시 userinfo 파싱)               │
       ▼                                         ▼
  Host State ──── Port State              Query State
       │               │                        │
       ▼               ▼                        ▼
  Path Start State ◄──┘                  Fragment State
       │
       ▼
  Path State
       │
       ├──── Query State
       │          │
       ▼          ▼
  Fragment State ◄┘
```

### 3.4 파싱 실패 사례

```javascript
// 파싱이 실패하는 경우들
const invalidURLs = [
  '',                        // 빈 문자열
  'not a url',              // 스킴 없음 (base URL 없을 때)
  'https://',               // 특수 스킴인데 호스트 없음 -> 실제로는 빈 호스트로 파싱됨
  'https://[::1',           // 불완전한 IPv6
  'https://example.com:abc', // 포트에 문자
  'https://example.com:99999', // 포트 범위 초과
];

invalidURLs.forEach(input => {
  try {
    new URL(input);
    console.log(`"${input}" → 파싱 성공`);
  } catch (e) {
    console.log(`"${input}" → 파싱 실패`);
  }
});
```

### 3.5 유효성 검사 오류(Validation Errors)

파싱이 성공하더라도 입력에 문제가 있을 수 있다. 표준은 이를 "validation error"로 정의한다. 예를 들어:

- `INVALID_URL_UNIT`: URL에 허용되지 않는 문자가 포함
- `SPECIAL_SCHEME_MISSING_FOLLOWING_SOLIDUS`: 특수 스킴 뒤에 `//`가 없음
- `MISSING_SCHEME_NON_RELATIVE_URL`: 스킴이 없고 base URL도 없음
- `INVALID_CREDENTIALS`: `file:` 스킴에 credentials 포함
- `HOST_MISSING`: 특수 스킴인데 호스트가 없음

```javascript
// 유효성 검사 오류가 발생하지만 파싱은 성공하는 경우
const url = new URL('https:example.com'); // 슬래시 누락
console.log(url.href); // "https://example.com/" (자동 보정됨)
```

---

## 4. 호스트 파싱

### 4.1 호스트 타입

WHATWG URL Standard에서 호스트(host)는 다음 중 하나이다:

1. 도메인(domain): DNS에서 해석되는 문자열 (예: `example.com`)
2. IPv4 주소: 32비트 숫자 주소 (예: `192.168.1.1`)
3. IPv6 주소: 128비트 숫자 주소 (예: `[2001:db8::1]`)
4. 불투명 호스트(opaque host): 비특수 스킴에서 사용되는 percent-encoded 문자열
5. 빈 호스트(empty host): `file:` 스킴에서 로컬 파일을 가리킬 때

### 4.2 도메인 파싱

도메인 파싱은 다음 단계를 거친다:

1. 도메인-유니코드 변환: 입력 문자열을 유니코드로 디코딩
2. IDNA 처리: 국제화 도메인 이름을 ASCII로 변환 (domain to ASCII)
3. 유효성 검증: 금지 문자 확인, 라벨 길이 제한 등

```javascript
// 유니코드 도메인 파싱
const url1 = new URL('https://한국.com');
console.log(url1.hostname); // "xn--3e0b707e.com" (Punycode 변환)

const url2 = new URL('https://münchen.de');
console.log(url2.hostname); // "xn--mnchen-3ya.de"

// 대소문자는 소문자로 정규화
const url3 = new URL('https://EXAMPLE.COM');
console.log(url3.hostname); // "example.com"
```

도메인 라벨 규칙:
- 각 라벨(점으로 구분된 부분)은 최대 63바이트
- 전체 도메인 이름은 최대 253바이트
- 빈 라벨은 허용되지 않음 (후행 점 제외)

### 4.3 IPv4 주소 파싱

IPv4 주소 파싱은 단순한 점 표기법 이상을 처리한다. 역사적 이유로 다양한 형식을 지원한다.

```javascript
// 표준 점 표기법
const url1 = new URL('http://192.168.1.1/');
console.log(url1.hostname); // "192.168.1.1"

// 16진수 표기
const url2 = new URL('http://0xC0.0xA8.0x01.0x01/');
console.log(url2.hostname); // "192.168.1.1"

// 8진수 표기
const url3 = new URL('http://0300.0250.0001.0001/');
console.log(url3.hostname); // "192.168.1.1"

// 축약 표기 (2파트 - 마지막이 24비트)
const url4 = new URL('http://192.11010049/');
console.log(url4.hostname); // "192.168.1.1"

// 단일 정수 표기
const url5 = new URL('http://3232235777/');
console.log(url5.hostname); // "192.168.1.1"
```

IPv4 파싱 알고리즘:
1. 점(`.`)으로 분할
2. 각 부분의 숫자 형식 감지 (10진, 16진, 8진)
3. 숫자로 변환
4. 범위 검증 (각 옥텟: 0~255, 또는 축약 시 마지막 부분에 따라 다름)
5. 32비트 정수로 결합
6. 표준 점 표기법으로 직렬화

### 4.4 IPv6 주소 파싱

IPv6 주소는 `[`와 `]`로 감싸져야 한다.

```javascript
// 완전한 IPv6 주소
const url1 = new URL('http://[2001:0db8:85a3:0000:0000:8a2e:0370:7334]/');
console.log(url1.hostname); // "[2001:db8:85a3::8a2e:370:7334]" (압축 형태)

// 루프백 주소
const url2 = new URL('http://[::1]/');
console.log(url2.hostname); // "[::1]"

// IPv4-mapped IPv6 주소
const url3 = new URL('http://[::ffff:192.168.1.1]/');
console.log(url3.hostname); // "[::ffff:c0a8:101]"

// 존 ID는 허용되지 않음 (보안상 이유)
try {
  new URL('http://[fe80::1%25eth0]/');
} catch (e) {
  console.log('존 ID 포함 IPv6는 URL에서 사용 불가');
}
```

IPv6 직렬화 규칙:
- 선행 0 제거: `0db8` → `db8`
- 가장 긴 연속 0 그룹을 `::`로 압축
- 동일 길이면 첫 번째 그룹을 압축
- 소문자 16진수 사용

### 4.5 불투명 호스트(Opaque Host)

비특수 스킴에서는 호스트가 불투명 호스트로 파싱된다. 금지 문자를 제외한 모든 문자를 percent-encoding하여 저장한다.

```javascript
const url = new URL('custom://my host name/path');
// 비특수 스킴에서의 호스트 처리
console.log(url.hostname); // "my%20host%20name"
```

### 4.6 IDNA (Internationalized Domain Names in Applications)

IDNA는 비ASCII 문자가 포함된 도메인 이름을 ASCII 호환 인코딩(ACE)으로 변환하는 프로토콜이다. WHATWG URL Standard는 IDNA 2008의 변형인 UTS46(Unicode IDNA Compatibility Processing)을 사용한다.

```javascript
// IDNA 변환 과정
// 1. 유니코드 정규화(NFC)
// 2. 매핑 (대소문자 변환, 호환 문자 매핑)
// 3. Punycode 인코딩
// 4. 유효성 검증

// 예시: 한국어 도메인
const url = new URL('https://테스트.한국/');
console.log(url.hostname); // Punycode로 변환된 형태

// 혼합 스크립트 검증 (보안)
// 일부 문자 조합은 피싱 방지를 위해 거부될 수 있다
```

IDNA 처리에서 중요한 개념:

| 용어 | 설명 |
|------|------|
| A-label | ASCII 호환 형태 (`xn--...`) |
| U-label | 유니코드 원본 형태 |
| Punycode | 유니코드를 ASCII로 인코딩하는 알고리즘 |
| UTS46 | IDNA 2003/2008 호환성 처리 |

---

## 5. Percent-encoding

### 5.1 개념

Percent-encoding(퍼센트 인코딩)은 URL에서 허용되지 않는 바이트를 `%HH` 형태(H는 16진수)로 인코딩하는 메커니즘이다. UTF-8로 인코딩한 후 각 바이트를 percent-encode한다.

```javascript
// 한글 "안녕"의 percent-encoding
// "안" → UTF-8: 0xEC 0x95 0x88 → %EC%95%88
// "녕" → UTF-8: 0xEB 0x85 0x95 → %EB%85%95
const encoded = encodeURIComponent('안녕');
console.log(encoded); // "%EC%95%88%EB%85%95"
```

### 5.2 Percent-encode Set (인코딩 대상 집합)

WHATWG URL Standard는 URL의 각 부분에서 인코딩해야 하는 문자 집합을 정의한다. 더 제한적인 위치일수록 더 많은 문자를 인코딩한다.

#### C0 Control Percent-encode Set

가장 기본적인 집합. C0 제어 문자(U+0000~U+001F)와 U+007F 이상의 모든 코드 포인트를 포함한다.

```
범위: U+0000 ~ U+001F, U+007E 초과
```

#### Fragment Percent-encode Set

C0 control set에 추가로: 공백(` `), `"`, `<`, `>`, 백틱(`` ` ``)

```javascript
const url = new URL('https://example.com/page#hello world<>');
console.log(url.hash); // "#hello%20world%3C%3E"
```

#### Query Percent-encode Set

C0 control set에 추가로: 공백(` `), `"`, `#`, `<`, `>`

```
인코딩 대상: C0 controls, space, ", #, <, >
```

#### Special-query Percent-encode Set

Query set에 추가로: `'` (작은따옴표)

특수 스킴(http, https 등)의 쿼리에서는 작은따옴표도 인코딩한다.

```javascript
// 특수 스킴에서 작은따옴표 인코딩
const url = new URL("https://example.com/?q=it's");
console.log(url.search); // "?q=it%27s"

// 비특수 스킴에서는 작은따옴표가 그대로 유지
const url2 = new URL("custom://host/?q=it's");
console.log(url2.search); // "?q=it's"
```

#### Path Percent-encode Set

Query set에 추가로: `?`, `` ` ``, `{`, `}`

```javascript
const url = new URL('https://example.com/path with {braces}');
console.log(url.pathname); // "/path%20with%20%7Bbraces%7D"
```

#### Userinfo Percent-encode Set

Path set에 추가로: `/`, `:`, `;`, `=`, `@`, `[`, `\`, `]`, `^`, `|`

```javascript
const url = new URL('https://example.com');
url.username = 'user@domain';
url.password = 'p:ss/w=rd';
console.log(url.href);
// "https://user%40domain:p%3Ass%2Fw%3Drd@example.com/"
```

#### Component Percent-encode Set

Userinfo set에 추가로: `$`, `%`, `&`, `+`, `,`

`URLSearchParams`에서 사용되며, `application/x-www-form-urlencoded` 인코딩과 관련된다.

### 5.3 Percent-encode Set 포함 관계

```
C0 Control ⊂ Fragment ⊂ Query ⊂ Special-query
                         Query ⊂ Path ⊂ Userinfo ⊂ Component
```

### 5.4 Percent-encode 알고리즘

```
function percentEncode(byte):
    if byte가 percent-encode set에 포함:
        return "%" + 대문자_16진수(byte)
    else:
        return byte를 문자로
```

UTF-8 percent-encode 알고리즘:
1. 코드 포인트를 UTF-8로 인코딩하여 바이트 시퀀스 생성
2. 각 바이트를 percent-encode set과 비교하여 인코딩 여부 결정

```javascript
// 수동 percent-encoding 구현 예시
function utf8PercentEncode(codePoint, percentEncodeSet) {
  const encoder = new TextEncoder();
  const bytes = encoder.encode(String.fromCodePoint(codePoint));
  let result = '';
  for (const byte of bytes) {
    if (percentEncodeSet.includes(byte)) {
      result += '%' + byte.toString(16).toUpperCase().padStart(2, '0');
    } else {
      result += String.fromCharCode(byte);
    }
  }
  return result;
}
```

### 5.5 Percent-decode 알고리즘

Percent-decoding은 `%HH` 시퀀스를 원래 바이트로 복원한다.

```
function percentDecode(input):
    output = empty byte sequence
    for i = 0; i < input.length:
        if input[i] == '%' and 다음 두 문자가 16진수:
            output += hexToInt(input[i+1..i+2])
            i += 3
        else:
            output += input[i]
            i += 1
    return output
```

```javascript
// percent-decode 예시
function percentDecode(input) {
  const bytes = [];
  for (let i = 0; i < input.length; i++) {
    if (input[i] === '%' && i + 2 < input.length) {
      const hex = input.substring(i + 1, i + 3);
      if (/^[0-9A-Fa-f]{2}$/.test(hex)) {
        bytes.push(parseInt(hex, 16));
        i += 2;
        continue;
      }
    }
    bytes.push(input.charCodeAt(i));
  }
  return new Uint8Array(bytes);
}

// 사용 예시
const decoded = new TextDecoder().decode(
  percentDecode('%EC%95%88%EB%85%95')
);
console.log(decoded); // "안녕"
```

### 5.6 application/x-www-form-urlencoded

HTML 폼에서 사용되는 특별한 인코딩 방식으로, percent-encoding의 변형이다.

주요 차이점:
- 공백을 `%20`이 아닌 `+`로 인코딩
- `*`, `-`, `.`, `_` 이외의 비ASCII/비영숫자 문자를 모두 인코딩

```javascript
// URLSearchParams는 application/x-www-form-urlencoded를 사용
const params = new URLSearchParams();
params.set('query', 'hello world');
params.set('name', '김철수');
console.log(params.toString());
// "query=hello+world&name=%EA%B9%80%EC%B2%A0%EC%88%98"

// 공백이 +로 인코딩된 것에 주의
// URL.search에서는 %20으로 표시될 수 있음
```

---

## 6. URL 직렬화

### 6.1 URL 직렬화 알고리즘

URL 직렬화(serialization)는 파싱된 URL 레코드를 다시 문자열로 변환하는 과정이다. 이 과정은 URL을 정규화(normalize)하는 효과가 있다.

직렬화 알고리즘:

```
function serializeURL(url, excludeFragment = false):
    output = url.scheme + ":"

    if url.host is not null:
        output += "//"

        if url.username != "" or url.password != "":
            output += url.username
            if url.password != "":
                output += ":" + url.password
            output += "@"

        output += serializeHost(url.host)

        if url.port is not null:
            output += ":" + url.port

    // opaque path인 경우
    if url has opaque path:
        output += url.path
    else:
        for each segment in url.path:
            output += "/" + segment

    if url.query is not null:
        output += "?" + url.query

    if excludeFragment is false and url.fragment is not null:
        output += "#" + url.fragment

    return output
```

### 6.2 직렬화 예시

```javascript
// 직렬화를 통한 URL 정규화
const examples = [
  'HTTP://EXAMPLE.COM:80/a/../b',      // → "http://example.com/b"
  'https://example.com:443/',           // → "https://example.com/"
  'https://example.com/a/./b/../c',     // → "https://example.com/a/c"
  'https://example.com/path?',          // → "https://example.com/path?"
  'https://example.com/path#',          // → "https://example.com/path#"
];

examples.forEach(input => {
  const url = new URL(input);
  console.log(`"${input}" → "${url.href}"`);
});
```

### 6.3 호스트 직렬화

호스트 타입에 따라 직렬화 방식이 다르다.

```javascript
// 도메인: 그대로 출력
new URL('https://example.com').hostname; // "example.com"

// IPv4: 점 표기법
new URL('https://0x7f000001/').hostname; // "127.0.0.1"

// IPv6: 대괄호 + 압축 표기
new URL('https://[0:0:0:0:0:0:0:1]/').hostname; // "[::1]"
```

IPv6 직렬화의 세부 규칙:

```javascript
// 가장 긴 0 그룹을 ::로 압축
// [2001:db8:0:0:0:0:0:1] → [2001:db8::1]

// 동일 길이의 0 그룹이 여러 개면 첫 번째를 압축
// [2001:0:0:1:0:0:0:1] → [2001::1:0:0:0:1] (앞의 0그룹 압축)

// 길이 1인 0 그룹은 압축하지 않음 (구현에 따라 다를 수 있음)
const url = new URL('https://[2001:db8:0:1:0:0:0:1]/');
console.log(url.hostname); // "[2001:db8:0:1::1]"
```

### 6.4 프래그먼트 제외 직렬화

일부 컨텍스트(예: HTTP 요청)에서는 프래그먼트를 제외하고 직렬화해야 한다.

```javascript
// URL API에서는 직접적인 exclude-fragment 옵션이 없지만
// 내부적으로 fetch() 등에서 사용됨

const url = new URL('https://example.com/path?q=1#section');

// href는 프래그먼트를 포함
console.log(url.href); // "https://example.com/path?q=1#section"

// 프래그먼트 없는 URL을 얻으려면
const withoutFragment = url.origin + url.pathname + url.search;
console.log(withoutFragment); // "https://example.com/path?q=1"
```

---

## 7. Origin

### 7.1 Origin이란

Origin(출처)은 웹 보안의 핵심 개념으로, 리소스가 어디에서 왔는지를 식별한다. 동일 출처 정책(Same-Origin Policy)의 기반이 되며, WHATWG URL Standard에서 정의된다.

Origin은 두 가지 종류가 있다:
- tuple origin: (scheme, host, port, domain) 으로 구성
- opaque origin: 내부적으로 고유한 식별자를 가지는 불투명한 origin

### 7.2 Tuple Origin

특수 스킴(http, https, ftp, ws, wss)과 file 스킴에 대해 tuple origin이 생성된다.

```javascript
// tuple origin 예시
const url1 = new URL('https://example.com:443/path');
console.log(url1.origin); // "https://example.com" (기본 포트 생략)

const url2 = new URL('https://example.com:8443/path');
console.log(url2.origin); // "https://example.com:8443"

const url3 = new URL('http://localhost:3000/');
console.log(url3.origin); // "http://localhost:3000"
```

### 7.3 Opaque Origin

비특수 스킴의 URL은 opaque origin을 가진다. Opaque origin은 다른 어떤 origin과도 동일하지 않다(자기 자신과도 동일하지 않음).

```javascript
// opaque origin 예시
const url1 = new URL('data:text/html,<h1>Hello</h1>');
console.log(url1.origin); // "null"

const url2 = new URL('blob:https://example.com/uuid');
console.log(url2.origin); // "https://example.com" (blob의 경우 내부 URL의 origin)

const url3 = new URL('custom://example.com/path');
console.log(url3.origin); // "null"

// file: 스킴의 origin은 구현에 따라 다름
const url4 = new URL('file:///home/user/file.txt');
console.log(url4.origin); // 브라우저마다 다름 ("null" 또는 "file://")
```

### 7.4 Same Origin (동일 출처)

두 origin이 동일한지 판단하는 알고리즘:

```
function sameOrigin(A, B):
    if A is opaque and B is opaque:
        return A와 B가 동일한 opaque origin인지
    if A is opaque or B is opaque:
        return false
    if A.scheme == B.scheme and A.host == B.host and A.port == B.port:
        return true
    return false
```

```javascript
// Same Origin 비교 예시
function sameOrigin(url1, url2) {
  return new URL(url1).origin === new URL(url2).origin;
}

console.log(sameOrigin(
  'https://example.com/a',
  'https://example.com/b'
)); // true (같은 origin)

console.log(sameOrigin(
  'https://example.com',
  'http://example.com'
)); // false (스킴 다름)

console.log(sameOrigin(
  'https://example.com',
  'https://www.example.com'
)); // false (호스트 다름)

console.log(sameOrigin(
  'https://example.com',
  'https://example.com:8443'
)); // false (포트 다름)

console.log(sameOrigin(
  'https://example.com:443',
  'https://example.com'
)); // true (443은 기본 포트이므로 동일)
```

### 7.5 Same Origin-Domain

`document.domain`을 통해 설정된 도메인까지 고려한 비교이다. 이 기능은 보안상의 이유로 점차 폐지(deprecated)되고 있다.

```javascript
// same origin-domain은 document.domain 설정을 고려
// 예: a.example.com과 b.example.com이 모두
// document.domain = "example.com"으로 설정하면
// same origin-domain이 된다

// 주의: document.domain 설정은 현재 폐지 과정에 있다
// Chrome 106+에서는 기본적으로 비활성화
```

### 7.6 Origin 직렬화

```
function serializeOrigin(origin):
    if origin is opaque:
        return "null"
    result = origin.scheme + "://" + serializeHost(origin.host)
    if origin.port is not null:
        result += ":" + origin.port
    return result
```

```javascript
// origin 직렬화 예시
const urls = [
  'https://example.com/',
  'https://example.com:8443/',
  'ftp://files.example.com/',
  'data:text/plain,hello',
  'blob:https://example.com/uuid',
];

urls.forEach(u => {
  const url = new URL(u);
  console.log(`${u} → origin: "${url.origin}"`);
});
// https://example.com/ → origin: "https://example.com"
// https://example.com:8443/ → origin: "https://example.com:8443"
// ftp://files.example.com/ → origin: "ftp://files.example.com"
// data:text/plain,hello → origin: "null"
// blob:https://example.com/uuid → origin: "https://example.com"
```

---

## 8. URL API

### 8.1 URL 생성자

`URL` 생성자는 문자열을 파싱하여 URL 객체를 만든다.

```javascript
// 기본 사용법
const url = new URL('https://example.com/path?q=1#hash');

// base URL과 함께 사용 (상대 URL 해석)
const relative = new URL('/other-path', 'https://example.com/path');
console.log(relative.href); // "https://example.com/other-path"

// 파싱 실패 시 TypeError 발생
try {
  new URL('not-a-valid-url');
} catch (e) {
  console.log(e instanceof TypeError); // true
  console.log(e.message); // "Invalid URL" 등
}

// URL.canParse() - 파싱 가능 여부 확인 (예외 없이)
console.log(URL.canParse('https://example.com')); // true
console.log(URL.canParse('not-a-url'));            // false
console.log(URL.canParse('/path', 'https://example.com')); // true

// URL.parse() - 파싱 실패 시 null 반환 (예외 없이)
const parsed = URL.parse('https://example.com');
console.log(parsed?.href); // "https://example.com/"

const failed = URL.parse('invalid');
console.log(failed); // null
```

### 8.2 URL 속성

#### href

전체 URL 문자열이다. 설정 시 URL이 재파싱된다.

```javascript
const url = new URL('https://example.com');

// 읽기
console.log(url.href); // "https://example.com/"

// 쓰기 - 전체 URL이 새로 파싱됨
url.href = 'https://other.com/new-path';
console.log(url.hostname); // "other.com"
console.log(url.pathname); // "/new-path"

// 유효하지 않은 URL 설정 시 TypeError
try {
  url.href = 'not-valid';
} catch (e) {
  console.log('href 설정 실패');
}
```

#### origin (읽기 전용)

URL의 origin을 반환한다. 읽기 전용이므로 설정할 수 없다.

```javascript
const url = new URL('https://example.com:8443/path');
console.log(url.origin); // "https://example.com:8443"

// url.origin = 'https://other.com'; // 무시됨 (읽기 전용)
```

#### protocol

스킴에 `:`을 붙인 문자열이다.

```javascript
const url = new URL('https://example.com');
console.log(url.protocol); // "https:"

// 프로토콜 변경
url.protocol = 'http';
console.log(url.href); // "http://example.com/"

// 특수 스킴 ↔ 비특수 스킴 전환은 불가
const url2 = new URL('https://example.com');
url2.protocol = 'custom'; // 실패 (무시됨)
console.log(url2.protocol); // "https:" (변경되지 않음)
```

#### username / password

```javascript
const url = new URL('https://example.com');

url.username = 'admin';
url.password = 'secret123';
console.log(url.href); // "https://admin:secret123@example.com/"

// 특수 문자는 자동으로 percent-encode
url.username = 'user@name';
console.log(url.username); // "user%40name"
console.log(url.href);     // "https://user%40name:secret123@example.com/"

// file: 스킴에서는 credentials 설정 불가
const fileUrl = new URL('file:///path/to/file');
fileUrl.username = 'user'; // 무시됨
console.log(fileUrl.username); // ""
```

#### host / hostname

`host`는 호스트+포트, `hostname`은 호스트만 포함한다.

```javascript
const url = new URL('https://example.com:8443/path');

console.log(url.host);     // "example.com:8443"
console.log(url.hostname); // "example.com"

// host 설정 (포트 포함 가능)
url.host = 'other.com:9443';
console.log(url.hostname); // "other.com"
console.log(url.port);     // "9443"

// hostname 설정 (포트 변경 안 됨)
url.hostname = 'third.com';
console.log(url.port);     // "9443" (유지)

// IPv6 호스트
const url2 = new URL('https://[::1]:8080/');
console.log(url2.hostname); // "[::1]"
console.log(url2.host);     // "[::1]:8080"

// opaque path가 있는 URL에서는 호스트 설정 불가
const url3 = new URL('mailto:user@example.com');
url3.hostname = 'other.com'; // 무시됨
```

#### port

```javascript
const url = new URL('https://example.com:8443/');

console.log(url.port); // "8443"

// 포트 변경
url.port = '9443';
console.log(url.href); // "https://example.com:9443/"

// 기본 포트로 설정하면 빈 문자열이 됨
url.port = '443';
console.log(url.port); // "" (https의 기본 포트)
console.log(url.href); // "https://example.com/"

// 숫자가 아닌 값은 파싱 가능한 부분까지만 사용
url.port = '80abc';
console.log(url.port); // "80"

// 범위 초과 시 무시
url.port = '99999'; // 무시됨

// 빈 문자열로 설정하면 포트 제거
url.port = '';
console.log(url.port); // ""
```

#### pathname

```javascript
const url = new URL('https://example.com/a/b/c');
console.log(url.pathname); // "/a/b/c"

// 경로 변경
url.pathname = '/new/path';
console.log(url.href); // "https://example.com/new/path"

// 자동 정규화
url.pathname = '/a/../b/./c';
console.log(url.pathname); // "/b/c"

// 특수 문자 자동 인코딩
url.pathname = '/path with spaces';
console.log(url.pathname); // "/path%20with%20spaces"

// 비특수 스킴에서의 opaque path
const url2 = new URL('mailto:user@example.com');
console.log(url2.pathname); // "user@example.com"
```

#### search

쿼리 문자열이다. `?`를 포함하여 반환된다.

```javascript
const url = new URL('https://example.com/path?key=value&foo=bar');
console.log(url.search); // "?key=value&foo=bar"

// 쿼리 변경
url.search = '?newkey=newvalue';
console.log(url.href); // "https://example.com/path?newkey=newvalue"

// ?를 생략해도 됨
url.search = 'another=query';
console.log(url.search); // "?another=query"

// 빈 문자열로 설정하면 쿼리 제거
url.search = '';
console.log(url.search); // ""
console.log(url.href);   // "https://example.com/path"

// search 변경 시 searchParams도 업데이트됨
url.search = '?a=1&b=2';
console.log(url.searchParams.get('a')); // "1"
```

#### hash

프래그먼트이다. `#`을 포함하여 반환된다.

```javascript
const url = new URL('https://example.com/page#section');
console.log(url.hash); // "#section"

// 해시 변경
url.hash = '#new-section';
console.log(url.href); // "https://example.com/page#new-section"

// # 생략해도 됨
url.hash = 'another';
console.log(url.hash); // "#another"

// 빈 문자열로 설정하면 프래그먼트 제거
url.hash = '';
console.log(url.hash); // ""
console.log(url.href); // "https://example.com/page"
```

### 8.3 toString() / toJSON()

```javascript
const url = new URL('https://example.com/path?q=1#hash');

// toString()은 href와 동일
console.log(url.toString()); // "https://example.com/path?q=1#hash"
console.log(url.toString() === url.href); // true

// toJSON()도 href와 동일 (JSON.stringify에서 사용)
console.log(url.toJSON()); // "https://example.com/path?q=1#hash"
console.log(JSON.stringify(url)); // '"https://example.com/path?q=1#hash"'

// toJSON이 있으므로 JSON 직렬화가 자연스럽게 동작
const data = { endpoint: new URL('https://api.example.com/v1') };
console.log(JSON.stringify(data));
// '{"endpoint":"https://api.example.com/v1"}'
```

### 8.4 URL 속성 변경의 상호 영향

```javascript
const url = new URL('https://example.com:8443/path?q=1#hash');

// protocol 변경 시 기본 포트도 재평가
url.protocol = 'http';
console.log(url.port); // "8443" (http 기본 포트 80이 아니므로 유지)

// search 변경 시 searchParams 자동 갱신
url.search = '?new=value';
console.log(url.searchParams.get('new')); // "value"

// searchParams 변경 시 search 자동 갱신
url.searchParams.set('extra', 'data');
console.log(url.search); // "?new=value&extra=data"
```

---

## 9. URLSearchParams API

### 9.1 생성자

`URLSearchParams`는 쿼리 문자열을 구조적으로 조작하기 위한 API이다. 여러 형태의 입력을 받는다.

#### 문자열로 생성

```javascript
// 선행 ?는 자동 제거
const params1 = new URLSearchParams('?key=value&foo=bar');
console.log(params1.toString()); // "key=value&foo=bar"

// ? 없이도 가능
const params2 = new URLSearchParams('key=value&foo=bar');
console.log(params2.toString()); // "key=value&foo=bar"

// application/x-www-form-urlencoded 디코딩
const params3 = new URLSearchParams('name=%EA%B9%80%EC%B2%A0%EC%88%98&age=30');
console.log(params3.get('name')); // "김철수"

// +는 공백으로 디코딩
const params4 = new URLSearchParams('q=hello+world');
console.log(params4.get('q')); // "hello world"
```

#### 객체로 생성

```javascript
const params = new URLSearchParams({
  query: 'javascript',
  page: '1',
  limit: '20',
});
console.log(params.toString()); // "query=javascript&page=1&limit=20"

// 값은 문자열로 변환됨
const params2 = new URLSearchParams({
  num: 42,       // "42"로 변환
  bool: true,    // "true"로 변환
  nil: null,     // "null"로 변환
});
console.log(params2.toString()); // "num=42&bool=true&nil=null"
```

#### 배열(이터러블)로 생성

동일한 키에 여러 값을 설정할 때 유용하다.

```javascript
// 배열의 배열
const params1 = new URLSearchParams([
  ['color', 'red'],
  ['color', 'blue'],
  ['size', 'large'],
]);
console.log(params1.toString()); // "color=red&color=blue&size=large"
console.log(params1.getAll('color')); // ["red", "blue"]

// Map으로도 생성 가능 (이터러블이므로)
const map = new Map([['key1', 'value1'], ['key2', 'value2']]);
const params2 = new URLSearchParams(map);
console.log(params2.toString()); // "key1=value1&key2=value2"
```

### 9.2 append(name, value)

새 키-값 쌍을 추가한다. 이미 같은 키가 있어도 새로 추가된다.

```javascript
const params = new URLSearchParams();
params.append('tag', 'javascript');
params.append('tag', 'web');
params.append('tag', 'url');
console.log(params.toString()); // "tag=javascript&tag=web&tag=url"
console.log(params.getAll('tag')); // ["javascript", "web", "url"]
```

### 9.3 delete(name, value?)

지정된 키(및 선택적으로 값)의 항목을 제거한다.

```javascript
const params = new URLSearchParams('a=1&b=2&a=3&c=4');

// 키로 모든 항목 삭제
params.delete('a');
console.log(params.toString()); // "b=2&c=4"

// 키+값으로 특정 항목만 삭제 (최근 추가된 기능)
const params2 = new URLSearchParams('color=red&color=blue&color=green');
params2.delete('color', 'blue');
console.log(params2.toString()); // "color=red&color=green"
```

### 9.4 get(name) / getAll(name)

```javascript
const params = new URLSearchParams('lang=ko&lang=en&region=asia');

// get: 첫 번째 값만 반환, 없으면 null
console.log(params.get('lang'));    // "ko"
console.log(params.get('region')); // "asia"
console.log(params.get('missing')); // null

// getAll: 모든 값을 배열로 반환, 없으면 빈 배열
console.log(params.getAll('lang'));    // ["ko", "en"]
console.log(params.getAll('missing')); // []
```

### 9.5 has(name, value?)

지정된 키(및 선택적으로 값)의 존재 여부를 반환한다.

```javascript
const params = new URLSearchParams('color=red&color=blue&size=large');

console.log(params.has('color'));         // true
console.log(params.has('missing'));       // false

// 키+값으로 확인 (최근 추가된 기능)
console.log(params.has('color', 'red'));  // true
console.log(params.has('color', 'green')); // false
```

### 9.6 set(name, value)

지정된 키의 첫 번째 항목의 값을 설정하고, 나머지 동일 키 항목은 제거한다. 키가 없으면 새로 추가한다.

```javascript
const params = new URLSearchParams('color=red&size=large&color=blue');

// 기존 키 설정: 첫 번째를 변경하고 나머지 제거
params.set('color', 'green');
console.log(params.toString()); // "color=green&size=large"

// 새 키 설정: 끝에 추가
params.set('weight', '100');
console.log(params.toString()); // "color=green&size=large&weight=100"
```

### 9.7 sort()

키 이름을 기준으로 모든 항목을 정렬한다. 동일한 키를 가진 항목들의 상대적 순서는 보존된다(안정 정렬).

```javascript
const params = new URLSearchParams('z=3&a=1&m=2&a=4');
params.sort();
console.log(params.toString()); // "a=1&a=4&m=2&z=3"

// 캐시 키로 사용할 때 유용
function normalizeQueryString(qs) {
  const params = new URLSearchParams(qs);
  params.sort();
  return params.toString();
}

console.log(normalizeQueryString('b=2&a=1') === normalizeQueryString('a=1&b=2'));
// true
```

### 9.8 toString()

모든 항목을 `application/x-www-form-urlencoded` 형식으로 직렬화한다.

```javascript
const params = new URLSearchParams();
params.set('query', 'hello world');
params.set('name', '홍길동');
params.set('special', 'a&b=c');

console.log(params.toString());
// "query=hello+world&name=%ED%99%8D%EA%B8%B8%EB%8F%99&special=a%26b%3Dc"

// 공백 → +
// 한글 → UTF-8 percent-encoding
// & → %26 (구분자와 혼동 방지)
// = → %3D (키-값 구분자와 혼동 방지)
```

### 9.9 이터레이션

`URLSearchParams`는 이터러블이므로 다양한 방법으로 순회할 수 있다.

```javascript
const params = new URLSearchParams('a=1&b=2&c=3');

// entries() - [key, value] 쌍
for (const [key, value] of params.entries()) {
  console.log(`${key}: ${value}`);
}
// a: 1
// b: 2
// c: 3

// keys() - 키만
for (const key of params.keys()) {
  console.log(key);
}
// a, b, c

// values() - 값만
for (const value of params.values()) {
  console.log(value);
}
// 1, 2, 3

// forEach
params.forEach((value, key) => {
  console.log(`${key}=${value}`);
});

// Symbol.iterator (entries와 동일)
for (const [key, value] of params) {
  console.log(`${key}: ${value}`);
}

// 스프레드 연산자
const array = [...params];
console.log(array); // [["a", "1"], ["b", "2"], ["c", "3"]]

// Object.fromEntries로 객체 변환
const obj = Object.fromEntries(params);
console.log(obj); // { a: "1", b: "2", c: "3" }
// 주의: 동일 키가 여러 개면 마지막 값만 남음
```

### 9.10 URL 객체와의 연동

```javascript
const url = new URL('https://example.com/search?q=hello&lang=ko');

// URL.searchParams로 접근
const params = url.searchParams;

// searchParams 수정 시 URL도 자동 갱신
params.set('q', 'world');
params.append('page', '1');
console.log(url.search); // "?q=world&lang=ko&page=1"
console.log(url.href);   // "https://example.com/search?q=world&lang=ko&page=1"

// URL.search 수정 시 searchParams도 자동 갱신
url.search = '?new=param';
console.log(url.searchParams.get('new')); // "param"
console.log(url.searchParams.get('q'));   // null (이전 파라미터 사라짐)
```

### 9.11 실용적인 활용 예시

```javascript
// API URL 빌더
function buildApiUrl(base, endpoint, params = {}) {
  const url = new URL(endpoint, base);
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null) {
      if (Array.isArray(value)) {
        value.forEach(v => url.searchParams.append(key, v));
      } else {
        url.searchParams.set(key, String(value));
      }
    }
  });
  return url.toString();
}

console.log(buildApiUrl('https://api.example.com', '/v1/search', {
  q: '검색어',
  tags: ['js', 'web'],
  page: 1,
  limit: 20,
}));
// "https://api.example.com/v1/search?q=%EA%B2%80%EC%83%89%EC%96%B4&tags=js&tags=web&page=1&limit=20"

// 쿼리 파라미터 병합
function mergeSearchParams(...paramSources) {
  const merged = new URLSearchParams();
  for (const source of paramSources) {
    const params = new URLSearchParams(source);
    for (const [key, value] of params) {
      merged.append(key, value);
    }
  }
  return merged;
}

const merged = mergeSearchParams('a=1&b=2', 'c=3&d=4', { e: '5' });
console.log(merged.toString()); // "a=1&b=2&c=3&d=4&e=5"
```

---

## 10. URL 패턴

### 10.1 URLPattern API 개요

`URLPattern`은 URL 패턴 매칭을 위한 API로, WHATWG URL Standard와는 별도의 사양(URLPattern Standard)이지만 밀접하게 관련되어 있다. 라우팅, URL 필터링 등에 활용된다.

> 참고: URLPattern은 Chrome 95+, Edge 95+, Deno에서 지원된다. Firefox와 Safari에서는 아직 제한적이다.

### 10.2 기본 사용법

```javascript
// 문자열 패턴
const pattern1 = new URLPattern('https://example.com/users/:id');
console.log(pattern1.test('https://example.com/users/123'));  // true
console.log(pattern1.test('https://example.com/users/'));     // false

const result = pattern1.exec('https://example.com/users/456');
console.log(result.pathname.groups.id); // "456"

// 구성 요소별 패턴
const pattern2 = new URLPattern({
  protocol: 'https',
  hostname: '*.example.com',
  pathname: '/api/:version/*',
});

console.log(pattern2.test('https://sub.example.com/api/v1/users'));  // true
console.log(pattern2.test('http://sub.example.com/api/v1/users'));   // false (http)
```

### 10.3 패턴 문법

```javascript
// 명명된 그룹 - :name
const p1 = new URLPattern({ pathname: '/users/:userId/posts/:postId' });
const r1 = p1.exec({ pathname: '/users/42/posts/100' });
console.log(r1.pathname.groups); // { userId: "42", postId: "100" }

// 와일드카드 - *
const p2 = new URLPattern({ pathname: '/static/*' });
const r2 = p2.exec({ pathname: '/static/css/style.css' });
console.log(r2.pathname.groups[0]); // "css/style.css"

// 선택적 그룹 - :name?
const p3 = new URLPattern({ pathname: '/users/:id/posts{/:postId}?' });
console.log(p3.test({ pathname: '/users/42/posts' }));      // true
console.log(p3.test({ pathname: '/users/42/posts/100' }));   // true

// 정규식 그룹 - (regex)
const p4 = new URLPattern({ pathname: '/files/:name(\\d+\\.\\w+)' });
console.log(p4.test({ pathname: '/files/123.txt' }));  // true
console.log(p4.test({ pathname: '/files/abc.txt' }));  // false
```

### 10.4 라우팅 활용 예시

```javascript
// 간단한 라우터 구현
class Router {
  #routes = [];

  add(pattern, handler) {
    this.#routes.push({
      pattern: new URLPattern(pattern),
      handler,
    });
  }

  match(url) {
    for (const route of this.#routes) {
      const result = route.pattern.exec(url);
      if (result) {
        return { handler: route.handler, params: result };
      }
    }
    return null;
  }
}

const router = new Router();

router.add({ pathname: '/' }, () => 'Home');
router.add({ pathname: '/users/:id' }, (params) => `User ${params.pathname.groups.id}`);
router.add({ pathname: '/api/*' }, () => 'API');

const match = router.match('https://example.com/users/42');
if (match) {
  console.log(match.handler(match.params)); // "User 42"
}
```

### 10.5 URLPattern과 Service Worker

```javascript
// Service Worker에서의 URLPattern 활용
self.addEventListener('fetch', (event) => {
  const apiPattern = new URLPattern({
    pathname: '/api/:version/:resource',
  });

  const result = apiPattern.exec(event.request.url);
  if (result) {
    const { version, resource } = result.pathname.groups;
    event.respondWith(handleApiRequest(version, resource));
    return;
  }

  // 기본 처리
  event.respondWith(fetch(event.request));
});
```

---

## 11. 특수 스킴(Special Schemes)

### 11.1 특수 스킴이란

WHATWG URL Standard는 6개의 스킴을 "특수 스킴(special schemes)"으로 정의한다. 이들은 다른 스킴과 다르게 처리되는 규칙이 여러 가지 있다.

```javascript
// 특수 스킴과 기본 포트
const specialSchemes = {
  'ftp':   21,
  'file':  null,
  'http':  80,
  'https': 443,
  'ws':    80,
  'wss':   443,
};
```

### 11.2 특수 스킴의 특수 처리

#### 1) 경로 구분자로 `\` 허용

```javascript
// 특수 스킴에서는 \가 /로 정규화
const url1 = new URL('https://example.com/a\\b\\c');
console.log(url1.pathname); // "/a/b/c"

// 비특수 스킴에서는 \가 percent-encode
const url2 = new URL('custom://host/a\\b\\c');
console.log(url2.pathname); // "/a%5Cb%5Cc"
```

#### 2) 호스트 필수

```javascript
// 특수 스킴에서는 호스트가 필수 (file 제외)
try {
  new URL('https:///path'); // 빈 호스트 → 실패
} catch (e) {
  console.log('특수 스킴에서 호스트 없으면 파싱 실패');
}

// file 스킴에서는 빈 호스트 허용
const fileUrl = new URL('file:///path/to/file');
console.log(fileUrl.hostname); // ""
```

#### 3) 기본 포트 생략

```javascript
// 기본 포트가 지정되면 직렬화 시 생략
const url = new URL('https://example.com:443/');
console.log(url.port); // ""
console.log(url.href); // "https://example.com/"

// 기본 포트가 아닌 경우 표시
const url2 = new URL('https://example.com:8443/');
console.log(url2.port); // "8443"
console.log(url2.href); // "https://example.com:8443/"
```

#### 4) 빈 경로 → `/`

```javascript
// 특수 스킴에서는 빈 경로가 /로 정규화
const url = new URL('https://example.com');
console.log(url.pathname); // "/"
console.log(url.href);     // "https://example.com/"
```

#### 5) 스킴 전환 제한

```javascript
// 특수 ↔ 비특수 스킴 전환 불가
const url1 = new URL('https://example.com/');
url1.protocol = 'custom';  // 무시됨
console.log(url1.protocol); // "https:"

// 특수 ↔ 특수 스킴 전환은 가능
const url2 = new URL('https://example.com/');
url2.protocol = 'http';
console.log(url2.protocol); // "http:"

// http ↔ https 전환
const url3 = new URL('http://example.com/path?q=1');
url3.protocol = 'https';
console.log(url3.href); // "https://example.com/path?q=1"
```

### 11.3 file: 스킴의 특수성

`file:` 스킴은 로컬 파일 시스템을 참조하며, 다른 특수 스킴과 차별화된다.

```javascript
// 기본 file URL
const url1 = new URL('file:///home/user/document.txt');
console.log(url1.hostname); // ""
console.log(url1.pathname); // "/home/user/document.txt"

// Windows 경로
const url2 = new URL('file:///C:/Users/user/document.txt');
console.log(url2.pathname); // "/C:/Users/user/document.txt"

// file 스킴은 기본 포트가 없음
// file 스킴에서는 credentials 설정 불가
const url3 = new URL('file:///path');
url3.username = 'user'; // 무시됨
url3.password = 'pass'; // 무시됨
console.log(url3.href); // "file:///path"

// file 스킴에서 호스트 이름은 보통 비어 있지만 UNC 경로 표현 가능
const url4 = new URL('file://server/share/file.txt');
console.log(url4.hostname); // "server"
console.log(url4.pathname); // "/share/file.txt"
```

### 11.4 각 스킴별 상세

#### ftp (File Transfer Protocol)

```javascript
const url = new URL('ftp://ftp.example.com/pub/files/readme.txt');
console.log(url.protocol); // "ftp:"
console.log(url.port);     // "" (기본 포트 21)
console.log(url.origin);   // "ftp://ftp.example.com"

// 인증 정보 포함
const url2 = new URL('ftp://user:pass@ftp.example.com/files/');
console.log(url2.username); // "user"
```

#### ws / wss (WebSocket)

```javascript
const url1 = new URL('ws://echo.websocket.org/');
console.log(url1.port);   // "" (기본 포트 80)
console.log(url1.origin); // "ws://echo.websocket.org"

const url2 = new URL('wss://secure.example.com:8443/socket');
console.log(url2.port);   // "8443"
console.log(url2.origin); // "wss://secure.example.com:8443"

// WebSocket 연결에 사용
// const ws = new WebSocket('wss://example.com/socket');
```

---

## 12. Blob URL

### 12.1 Blob URL 개요

Blob URL(`blob:` 스킴)은 메모리에 있는 `Blob` 또는 `File` 객체를 참조하는 URL이다. 브라우저의 현재 세션에서만 유효하며, `URL.createObjectURL()`로 생성하고 `URL.revokeObjectURL()`로 해제한다.

### 12.2 createObjectURL

```javascript
// Blob에서 URL 생성
const blob = new Blob(['<h1>Hello, World!</h1>'], { type: 'text/html' });
const blobUrl = URL.createObjectURL(blob);
console.log(blobUrl); // "blob:https://example.com/550e8400-e29b-41d4-a716-446655440000"

// 이미지 미리보기에 활용
const fileInput = document.querySelector('input[type="file"]');
fileInput.addEventListener('change', (event) => {
  const file = event.target.files[0];
  const previewUrl = URL.createObjectURL(file);

  const img = document.querySelector('#preview');
  img.src = previewUrl; // blob URL을 이미지 소스로 사용

  // 이미지 로드 후 URL 해제
  img.onload = () => URL.revokeObjectURL(previewUrl);
});

// 다운로드 링크 생성
function downloadBlob(data, filename, mimeType) {
  const blob = new Blob([data], { type: mimeType });
  const url = URL.createObjectURL(blob);

  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();

  URL.revokeObjectURL(url);
}

downloadBlob(
  JSON.stringify({ key: 'value' }, null, 2),
  'data.json',
  'application/json'
);
```

### 12.3 revokeObjectURL

```javascript
const blob = new Blob(['data']);
const url = URL.createObjectURL(blob);

// URL 사용 후 반드시 해제해야 메모리 누수 방지
URL.revokeObjectURL(url);

// 해제된 URL은 더 이상 유효하지 않음
// img.src = url; // 이미지 로드 실패
```

### 12.4 Blob URL의 Origin

Blob URL의 origin은 해당 URL을 생성한 환경의 origin을 상속한다.

```javascript
// https://example.com에서 생성된 blob URL
const blob = new Blob(['test']);
const blobUrl = URL.createObjectURL(blob);

const url = new URL(blobUrl);
console.log(url.origin); // "https://example.com" (생성 환경의 origin)
```

### 12.5 Blob URL 구조

```
blob:https://example.com/550e8400-e29b-41d4-a716-446655440000
|__| |_________________| |____________________________________|
  |          |                          |
scheme   origin 부분              UUID (고유 식별자)
```

---

## 13. data: URL 처리

### 13.1 data: URL 구조

`data:` URL은 데이터를 URL 자체에 인라인으로 포함한다. 작은 파일을 외부 요청 없이 사용할 때 유용하다.

```
data:[<mediatype>][;base64],<data>
```

```javascript
// 텍스트 데이터
const textUrl = 'data:text/plain;charset=utf-8,Hello%2C%20World!';

// Base64 인코딩 데이터
const base64Url = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==';

// HTML 데이터
const htmlUrl = 'data:text/html,<h1>Hello</h1>';

// JSON 데이터
const jsonUrl = `data:application/json,${encodeURIComponent(JSON.stringify({ key: 'value' }))}`;
```

### 13.2 data: URL 파싱

WHATWG Fetch Standard에서 정의하는 data URL 처리 알고리즘:

```javascript
// data: URL 파싱 구현 예시
function parseDataURL(url) {
  const str = typeof url === 'string' ? url : url.href;

  // "data:" 접두사 확인
  if (!str.startsWith('data:')) {
    return null;
  }

  const rest = str.substring(5); // "data:" 이후

  // 첫 번째 , 를 찾아 mediatype과 data 분리
  const commaIndex = rest.indexOf(',');
  if (commaIndex === -1) {
    return null;
  }

  const mediaType = rest.substring(0, commaIndex);
  let data = rest.substring(commaIndex + 1);

  // base64 여부 확인
  const isBase64 = mediaType.endsWith(';base64');
  const mimeType = isBase64
    ? mediaType.slice(0, -7) // ";base64" 제거
    : mediaType;

  if (isBase64) {
    data = atob(data);
  } else {
    data = decodeURIComponent(data);
  }

  return {
    mimeType: mimeType || 'text/plain;charset=US-ASCII',
    isBase64,
    data,
  };
}

// 사용 예시
const result = parseDataURL('data:text/html;charset=utf-8,<h1>Hello</h1>');
console.log(result);
// { mimeType: "text/html;charset=utf-8", isBase64: false, data: "<h1>Hello</h1>" }
```

### 13.3 data: URL의 Origin

`data:` URL은 항상 opaque origin을 가진다. 이는 보안에 중요한 의미를 가진다.

```javascript
const url = new URL('data:text/plain,hello');
console.log(url.origin); // "null" (opaque origin)

// data: URL에서 로드된 콘텐츠는 어떤 사이트의 origin과도 동일하지 않다
// 따라서 XHR, fetch 등에서 same-origin 정책의 영향을 받는다
```

### 13.4 data: URL 활용

```javascript
// CSS에서 인라인 이미지
const css = `
  .icon {
    background-image: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10" fill="blue"/></svg>');
  }
`;

// fetch로 data URL 읽기
async function readDataUrl(dataUrl) {
  const response = await fetch(dataUrl);
  return await response.text();
}

const text = await readDataUrl('data:text/plain,Hello');
console.log(text); // "Hello"

// Blob에서 data URL로 변환
function blobToDataUrl(blob) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = reject;
    reader.readAsDataURL(blob);
  });
}

const blob = new Blob(['Hello, World!'], { type: 'text/plain' });
const dataUrl = await blobToDataUrl(blob);
console.log(dataUrl); // "data:text/plain;base64,SGVsbG8sIFdvcmxkIQ=="
```

---

## 14. 상대 URL 해석

### 14.1 기본 개념

상대 URL은 base URL을 기준으로 절대 URL로 해석된다. 이는 HTML의 `<base>` 태그, 링크, 리소스 참조 등에서 널리 사용된다.

```javascript
const base = 'https://example.com/a/b/c';

// 절대 경로
console.log(new URL('/d/e', base).href);
// "https://example.com/d/e"

// 상대 경로
console.log(new URL('d/e', base).href);
// "https://example.com/a/b/d/e" (마지막 세그먼트 c가 대체됨)

// 상위 디렉토리
console.log(new URL('../d', base).href);
// "https://example.com/a/d"

// 현재 디렉토리
console.log(new URL('./d', base).href);
// "https://example.com/a/b/d"

// 쿼리만 변경
console.log(new URL('?q=1', base).href);
// "https://example.com/a/b/c?q=1"

// 프래그먼트만 변경
console.log(new URL('#section', base).href);
// "https://example.com/a/b/c#section"

// 프로토콜 상대 URL
console.log(new URL('//other.com/path', base).href);
// "https://other.com/path" (base의 스킴을 상속)
```

### 14.2 상대 URL 해석 알고리즘

상대 URL 해석은 URL 파싱 알고리즘의 일부로 수행된다. 스킴이 없는 입력이 주어지면, base URL을 기반으로 절대 URL을 구성한다.

```
1. 입력이 스킴으로 시작 → 절대 URL로 파싱
2. 입력이 "//"로 시작 → base의 스킴 + 입력의 authority/path/query/fragment
3. 입력이 "/"로 시작 → base의 스킴/authority + 입력의 절대 경로
4. 입력이 "?"로 시작 → base의 스킴/authority/path + 입력의 쿼리
5. 입력이 "#"로 시작 → base의 스킴/authority/path/query + 입력의 프래그먼트
6. 그 외 → base의 경로 마지막 세그먼트를 제거하고 입력을 결합
```

```javascript
// 단계별 해석 예시

const base = 'https://user:pass@example.com:8443/a/b/c?q=1#frag';

// 1. 절대 URL - base 무시
new URL('http://other.com/', base).href;
// "http://other.com/"

// 2. protocol-relative - base의 scheme만 사용
new URL('//cdn.example.com/lib.js', base).href;
// "https://cdn.example.com/lib.js"

// 3. 절대 경로 - base의 scheme + authority 사용
new URL('/new/path', base).href;
// "https://user:pass@example.com:8443/new/path"

// 4. 쿼리 변경 - base의 path까지 사용
new URL('?new=query', base).href;
// "https://user:pass@example.com:8443/a/b/c?new=query"

// 5. 프래그먼트 변경 - base의 query까지 사용
new URL('#new-frag', base).href;
// "https://user:pass@example.com:8443/a/b/c?q=1#new-frag"

// 6. 상대 경로 - base 경로의 마지막 세그먼트 대체
new URL('d/e', base).href;
// "https://user:pass@example.com:8443/a/b/d/e"
```

### 14.3 경로 정규화

상대 경로 해석 후 `.`과 `..` 세그먼트가 정규화된다.

```javascript
const base = 'https://example.com/a/b/c/d';

// .. 연속 사용
new URL('../../../../e', base).href;
// "https://example.com/e" (루트를 넘어가지 않음)

// . 과 .. 혼합
new URL('./../../e/./f/../g', base).href;
// "https://example.com/a/e/g"

// 정규화 과정 추적
// base path: /a/b/c/d
// 상대 경로: ./../../e/./f/../g
// 1. base에서 마지막 세그먼트 제거: /a/b/c/
// 2. 결합: /a/b/c/./../../e/./f/../g
// 3. ./ 제거: /a/b/c/../../e/./f/../g
// 4. ../../ 처리: /a/e/./f/../g
// 5. ./ 제거: /a/e/f/../g
// 6. ../ 처리: /a/e/g
```

### 14.4 빈 문자열 상대 URL

빈 문자열은 현재 URL의 복사본을 만든다(프래그먼트 제외).

```javascript
const base = 'https://example.com/path?q=1#frag';
const url = new URL('', base);
console.log(url.href); // "https://example.com/path?q=1"
// 프래그먼트가 제거된 것에 주의
```

### 14.5 HTML에서의 base URL

```html
<!DOCTYPE html>
<html>
<head>
  <!-- base 태그로 문서의 기본 URL 설정 -->
  <base href="https://cdn.example.com/assets/">
</head>
<body>
  <!-- 상대 URL이 base를 기준으로 해석됨 -->
  <img src="images/logo.png">
  <!-- → https://cdn.example.com/assets/images/logo.png -->

  <a href="/about">About</a>
  <!-- → https://cdn.example.com/about (절대 경로) -->

  <a href="https://other.com">Other</a>
  <!-- → https://other.com (절대 URL, base 무시) -->
</body>
</html>
```

```javascript
// JavaScript에서 현재 문서의 base URL 확인
console.log(document.baseURI);

// base URL을 기준으로 상대 URL 해석
function resolveUrl(relative) {
  return new URL(relative, document.baseURI).href;
}
```

---

## 15. URL 등가성(Equivalence)

### 15.1 URL 비교의 어려움

URL 비교는 단순한 문자열 비교보다 복잡하다. 동일한 리소스를 가리키는 URL이 여러 가지 문자열 표현을 가질 수 있기 때문이다.

```javascript
// 모두 같은 리소스를 가리키지만 문자열이 다른 URL들
const urls = [
  'https://example.com/path',
  'HTTPS://EXAMPLE.COM/path',
  'https://example.com:443/path',
  'https://example.com/./path',
  'https://example.com/other/../path',
  'https://example.com/path?',  // 빈 쿼리
];

// 단순 문자열 비교로는 동일성 판단 불가
console.log(urls[0] === urls[1]); // false
```

### 15.2 URL 정규화를 통한 비교

WHATWG URL Standard의 파싱 + 직렬화를 통해 URL을 정규화할 수 있다.

```javascript
// URL 정규화 함수
function normalizeUrl(input) {
  try {
    return new URL(input).href;
  } catch {
    return null;
  }
}

// 파싱+직렬화를 통한 정규화
console.log(normalizeUrl('HTTPS://EXAMPLE.COM/path'));
// "https://example.com/path"

console.log(normalizeUrl('https://example.com:443/path'));
// "https://example.com/path" (기본 포트 제거)

console.log(normalizeUrl('https://example.com/other/../path'));
// "https://example.com/path" (경로 정규화)

// URL 등가성 비교 함수
function urlEquals(url1, url2, options = {}) {
  try {
    const a = new URL(url1);
    const b = new URL(url2);

    if (options.excludeFragment) {
      a.hash = '';
      b.hash = '';
    }

    if (options.excludeQuery) {
      a.search = '';
      b.search = '';
    }

    return a.href === b.href;
  } catch {
    return false;
  }
}

// 사용 예시
console.log(urlEquals(
  'https://example.com/path',
  'HTTPS://EXAMPLE.COM:443/./path'
)); // true

console.log(urlEquals(
  'https://example.com/path#a',
  'https://example.com/path#b',
  { excludeFragment: true }
)); // true
```

### 15.3 WHATWG 표준에서 정의하는 등가성

표준은 두 URL이 "동일(equal)"한지 판단할 때 직렬화 결과를 비교한다. exclude-fragment 플래그를 사용하여 프래그먼트를 제외할 수 있다.

```
URL A와 B가 동일한가?
1. A를 직렬화한다 (exclude-fragment 옵션 적용)
2. B를 직렬화한다 (동일 옵션 적용)
3. 두 문자열이 동일하면 URL이 동일하다
```

### 15.4 정규화되지 않는 차이점

URL 파싱으로도 정규화되지 않는 차이점이 있다.

```javascript
// 1. 쿼리 파라미터 순서
const url1 = 'https://example.com/?a=1&b=2';
const url2 = 'https://example.com/?b=2&a=1';
console.log(new URL(url1).href === new URL(url2).href); // false
// 의미적으로 동일할 수 있지만, 표준은 순서를 보존

// 2. 불필요한 percent-encoding
const url3 = 'https://example.com/p%61th'; // %61 = 'a'
const url4 = 'https://example.com/path';
console.log(new URL(url3).href === new URL(url4).href); // false
// 일부 percent-encoding은 정규화되지 않음 (구현에 따라 다를 수 있음)

// 3. 후행 슬래시
const url5 = 'https://example.com/path';
const url6 = 'https://example.com/path/';
console.log(new URL(url5).href === new URL(url6).href); // false
// 서버에서는 동일하게 처리할 수 있지만 URL 표준에서는 다름
```

### 15.5 고급 URL 비교 구현

```javascript
// 의미적 URL 비교 (서버 관점)
function semanticUrlEquals(input1, input2) {
  try {
    const url1 = new URL(input1);
    const url2 = new URL(input2);

    // 스킴 비교 (이미 소문자로 정규화됨)
    if (url1.protocol !== url2.protocol) return false;

    // 호스트 비교 (이미 소문자로 정규화됨)
    if (url1.hostname !== url2.hostname) return false;

    // 포트 비교 (기본 포트 이미 정규화됨)
    if (url1.port !== url2.port) return false;

    // 경로 비교 (후행 슬래시 무시)
    const path1 = url1.pathname.replace(/\/$/, '') || '/';
    const path2 = url2.pathname.replace(/\/$/, '') || '/';
    if (path1 !== path2) return false;

    // 쿼리 비교 (파라미터 정렬 후)
    const params1 = new URLSearchParams(url1.search);
    const params2 = new URLSearchParams(url2.search);
    params1.sort();
    params2.sort();
    if (params1.toString() !== params2.toString()) return false;

    // 프래그먼트는 무시 (서버에 전달되지 않으므로)
    return true;
  } catch {
    return false;
  }
}

// 테스트
console.log(semanticUrlEquals(
  'https://example.com/path/?b=2&a=1#frag1',
  'HTTPS://EXAMPLE.COM:443/path?a=1&b=2#frag2'
)); // true

console.log(semanticUrlEquals(
  'https://example.com/path',
  'https://example.com/path/'
)); // true
```

### 15.6 캐시 키로서의 URL

브라우저 캐시에서 URL을 키로 사용할 때는 정규화된 형태를 사용한다.

```javascript
// 캐시 친화적 URL 정규화
function toCacheKey(urlString) {
  const url = new URL(urlString);

  // 프래그먼트 제거 (서버 요청에 포함되지 않으므로)
  url.hash = '';

  // 쿼리 파라미터 정렬 (선택적)
  url.searchParams.sort();

  return url.href;
}

const key1 = toCacheKey('https://example.com/api?b=2&a=1#section');
const key2 = toCacheKey('https://example.com/api?a=1&b=2#other');
console.log(key1 === key2); // true
// 둘 다 "https://example.com/api?a=1&b=2"
```

---

## 부록 A: 주요 참고 자료

| 자료 | URL |
|------|-----|
| WHATWG URL Standard | https://url.spec.whatwg.org/ |
| MDN - URL API | https://developer.mozilla.org/ko/docs/Web/API/URL |
| MDN - URLSearchParams | https://developer.mozilla.org/ko/docs/Web/API/URLSearchParams |
| URLPattern Standard | https://urlpattern.spec.whatwg.org/ |
| RFC 3986 (URI) | https://datatracker.ietf.org/doc/html/rfc3986 |
| RFC 3987 (IRI) | https://datatracker.ietf.org/doc/html/rfc3987 |
| UTS #46 (IDNA) | https://unicode.org/reports/tr46/ |

## 부록 B: 브라우저 호환성 요약

| 기능 | Chrome | Firefox | Safari | Edge |
|------|--------|---------|--------|------|
| URL() 생성자 | 32+ | 26+ | 7+ | 12+ |
| URL.canParse() | 120+ | 115+ | 17+ | 120+ |
| URL.parse() | 126+ | 126+ | 18+ | 126+ |
| URLSearchParams | 49+ | 44+ | 10.1+ | 17+ |
| URLSearchParams.sort() | 61+ | 54+ | 11+ | 61+ |
| URLPattern | 95+ | 미지원 | 미지원 | 95+ |
| URLSearchParams.delete(name, value) | 117+ | 115+ | 17+ | 117+ |
| URLSearchParams.has(name, value) | 117+ | 115+ | 17+ | 117+ |

## 부록 C: Node.js에서의 URL 처리

```javascript
// Node.js에서 WHATWG URL API 사용 (기본 내장)
const { URL, URLSearchParams } = globalThis;
// 또는
// const { URL, URLSearchParams } = require('url');

const url = new URL('https://example.com/path?q=hello');
console.log(url.hostname); // "example.com"

// Node.js 레거시 URL API (비권장)
const legacyUrl = require('url');
const parsed = legacyUrl.parse('https://example.com/path?q=hello');
console.log(parsed.hostname); // "example.com"
// 주의: legacyUrl.parse()는 WHATWG 표준과 다르게 동작할 수 있다

// Node.js에서 상대 경로 해석
const resolved = new URL('../other', 'https://example.com/a/b');
console.log(resolved.href); // "https://example.com/other"
```

## 부록 D: 자주 발생하는 실수와 해결 방법

```javascript
// 1. URL 생성자에 base URL 누락
try {
  new URL('/path'); // base URL 없이 상대 경로 → TypeError
} catch (e) {
  console.log('base URL 필요');
}
// 해결:
new URL('/path', 'https://example.com').href; // "https://example.com/path"

// 2. searchParams와 search의 불일치
const url = new URL('https://example.com/?a=1');
url.searchParams.set('b', '2');
// url.search는 자동 갱신됨 → "?a=1&b=2"

// 3. 인코딩 이중 적용
const query = encodeURIComponent('hello world'); // "hello%20world"
const url2 = new URL(`https://example.com/?q=${query}`);
console.log(url2.searchParams.get('q')); // "hello world" (자동 디코딩)

// URLSearchParams를 쓸 때는 직접 인코딩하지 않아야 함
const url3 = new URL('https://example.com/');
url3.searchParams.set('q', 'hello world'); // 자동 인코딩됨
console.log(url3.search); // "?q=hello+world"

// 4. origin 비교에 포트 누락
const a = new URL('https://example.com/');
const b = new URL('https://example.com:443/');
console.log(a.origin === b.origin); // true (기본 포트 정규화)

// 5. file: URL에서의 주의사항
const fileUrl = new URL('file:///C:/path/to/file.txt');
console.log(fileUrl.pathname); // "/C:/path/to/file.txt"
// Windows 경로로 변환 시 선행 / 제거 필요

// 6. URL이 유효한지 안전하게 확인
function isValidUrl(string) {
  return URL.canParse(string);
  // 또는 (canParse 미지원 환경):
  // try { new URL(string); return true; } catch { return false; }
}
```

## 부록 E: URL 파싱 구현 의사코드 (간략화)

```
function parseURL(input, base = null):
    // 1. 전처리
    input = trimControlChars(input)
    input = removeTabAndNewlines(input)

    // 2. 상태 머신 초기화
    state = SCHEME_START_STATE
    buffer = ""
    url = new URL record
    pointer = 0

    // 3. 메인 루프
    while pointer <= input.length:
        c = input[pointer]  // EOF면 특수 값

        switch state:
            case SCHEME_START_STATE:
                if c is ASCII alpha:
                    buffer += toLower(c)
                    state = SCHEME_STATE
                elif base is not null:
                    state = NO_SCHEME_STATE
                    pointer--  // 다시 처리
                else:
                    FAILURE

            case SCHEME_STATE:
                if c is ASCII alphanumeric or c in {'+', '-', '.'}:
                    buffer += toLower(c)
                elif c == ':':
                    url.scheme = buffer
                    buffer = ""
                    // 특수 스킴 처리, authority 확인 등
                    if input[pointer+1..pointer+2] == "//":
                        state = AUTHORITY_STATE
                        pointer += 2
                    // ... 다른 경우 처리
                else:
                    FAILURE

            case AUTHORITY_STATE:
                // @ 를 찾아 userinfo 분리
                // host, port 파싱으로 전이

            case HOST_STATE:
                // 호스트 문자열 수집
                // IPv6 ([...]) 처리
                // 호스트 파싱 호출

            case PORT_STATE:
                // 숫자 수집
                // 기본 포트 비교

            case PATH_STATE:
                // 경로 세그먼트 수집
                // . 과 .. 정규화

            case QUERY_STATE:
                // 쿼리 문자열 수집
                // percent-encoding 적용

            case FRAGMENT_STATE:
                // 프래그먼트 수집

        pointer++

    // 4. 결과 반환
    return url
```

---

이 문서는 WHATWG URL Standard(https://url.spec.whatwg.org/)를 기반으로 작성되었다. URL 표준은 Living Standard로서 지속적으로 업데이트되므로, 최신 내용은 공식 사양 문서를 참고하기 바란다.

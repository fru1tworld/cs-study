# WHATWG URL과 Fetch

## WHATWG URL Standard 완벽 가이드

### 목차

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

### 1. 개요

#### 1.1 URL Standard란

WHATWG URL Standard: 웹에서 사용되는 URL(Uniform Resource Locator)의 파싱·직렬화·조작에 관한 살아있는 표준(Living Standard).

- 브라우저가 실제로 URL을 처리하는 방식을 정의
- 모든 주요 웹 브라우저 벤더(Google·Mozilla·Apple·Microsoft)가 참여해 유지보수

URL: 웹의 가장 기본적인 요소 중 하나 → 리소스의 위치를 식별하고 접근하는 데 사용.

- 매일 수십억 개의 URL이 파싱·처리 → 이 동작의 정확한 정의가 필수

- 공식 문서: https://url.spec.whatwg.org/
- 유지보수 주체: WHATWG (Web Hypertext Application Technology Working Group)
- 문서 유형: Living Standard (지속적으로 업데이트되는 표준)

#### 1.2 역사: RFC 3986/3987과의 관계

URL의 역사: 복잡함. 초기에는 IETF(Internet Engineering Task Force)에서 관련 표준을 관리.

- RFC 1738 (1994): 최초의 URL 명세
- RFC 2396 (1998): URI(Uniform Resource Identifier) 일반 구문
- RFC 2732 (1999): IPv6 주소에 대한 URL 형식
- RFC 3986 (2005): URI 일반 구문 (RFC 2396 대체)
- RFC 3987 (2005): IRI(Internationalized Resource Identifier)
- WHATWG URL (2012~): 브라우저 호환 URL 파싱 표준

RFC 3986: URI의 이론적 구문을 정의하지만 실제 브라우저 구현과 상당한 차이 존재. 예시:

```
# RFC 3986에서는 유효하지 않지만 브라우저에서는 동작하는 URL들
http://example.com/path with spaces
http://example.com:80/  (기본 포트 명시)
HTTP://EXAMPLE.COM/  (대소문자 혼용)
http://example.com/foo/../bar  (경로 정규화)
```

#### 1.3 왜 별도 표준이 필요한가

WHATWG URL Standard 등장의 핵심 이유:

1) 브라우저 호환성 문제

- RFC 3986: 이론적으로 정확하나, 실제 웹에서 사용되는 URL 중 상당수가 RFC를 위반
- 브라우저들은 이런 "잘못된" URL도 합리적으로 처리해야 함 → 각 브라우저가 서로 다른 방식으로 처리 → 호환성 문제 발생

2) 단일 파싱 알고리즘의 부재

- RFC 3986: URL의 구문만 정의, 구체적인 파싱 알고리즘은 미제공
- WHATWG URL Standard: 상태 머신 기반의 구체적인 파싱 알고리즘을 정의 → 모든 구현이 동일한 결과를 내도록 함

3) 국제화 지원

- RFC 3987(IRI)이 국제화를 다루지만, WHATWG URL Standard는 이를 통합해 하나의 표준에서 처리
- 도메인의 IDNA 처리·비ASCII 문자의 percent-encoding 등을 포괄적으로 다룸

4) 웹 플랫폼 API 정의

- `URL`, `URLSearchParams` 같은 JavaScript API를 표준에서 직접 정의 → 웹 개발자가 프로그래밍 방식으로 URL을 조작할 수 있는 통일된 인터페이스 제공

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

### 2. URL 구조

#### 2.1 URL의 내부 표현

WHATWG URL Standard에서 URL: 단순한 문자열이 아니라 구조화된 객체(record). 파싱된 URL의 구성 요소:

```
 https://user:password@www.example.com:443/path/to/resource?key=value#section
 |____| |__| |______| |_____________| |_| |________________| |_______| |_____|
   |      |      |           |         |          |              |         |
 scheme  user  password    host       port      path           query   fragment
```

#### 2.2 각 구성 요소 상세

##### scheme (스킴)

URL의 프로토콜을 나타냄. 항상 소문자 ASCII 영문자로 정규화됨.

```javascript
const url = new URL('HTTPS://example.com');
console.log(url.protocol); // "https:" (소문자로 정규화)

// 유효한 스킴: 영문자로 시작, 영문자/숫자/+/-/. 허용
// 유효: http, https, ftp, ssh+git, my.scheme
// 무효: 1http, -ftp
```

특수 스킴(special scheme): 별도의 기본 포트와 처리 규칙 존재.

- ftp: 기본 포트 21
- file: 기본 포트 없음
- http: 기본 포트 80
- https: 기본 포트 443
- ws: 기본 포트 80
- wss: 기본 포트 443

##### username (사용자 이름)

인증에 사용되는 사용자 이름. 기본값: 빈 문자열.

```javascript
const url = new URL('https://admin@example.com');
console.log(url.username); // "admin"

// 특수 문자는 percent-encoding 된다
const url2 = new URL('https://user%40name@example.com');
console.log(url2.username); // "user%40name"
```

##### password (비밀번호)

인증에 사용되는 비밀번호. 기본값: 빈 문자열.

- 보안상 URL에 비밀번호를 포함하는 것은 권장되지 않음

```javascript
const url = new URL('https://user:p%40ss@example.com');
console.log(url.password); // "p%40ss"

// username 없이 password만 설정할 수도 있다 (비표준적이지만 파싱 가능)
const url2 = new URL('https://:secret@example.com');
console.log(url2.username); // ""
console.log(url2.password); // "secret"
```

##### host (호스트)

리소스가 위치한 서버를 식별. 다음 형태 중 하나:

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

##### port (포트)

네트워크 포트 번호. 기본 포트와 동일하면 빈 문자열로 표현됨.

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

##### path (경로)

리소스의 경로를 나타냄. 특수 스킴에서는 항상 `/`로 시작 → `.`과 `..` 세그먼트가 정규화됨.

```javascript
const url1 = new URL('https://example.com/a/b/../c');
console.log(url1.pathname); // "/a/c" (..이 정규화됨)

const url2 = new URL('https://example.com/a/./b');
console.log(url2.pathname); // "/a/b" (.이 정규화됨)

// 비특수 스킴에서는 opaque path일 수 있다
const url3 = new URL('mailto:user@example.com');
console.log(url3.pathname); // "user@example.com"
```

##### query (쿼리)

`?` 뒤에 오는 키-값 쌍의 문자열. 기본값: null.

```javascript
const url = new URL('https://example.com/search?q=hello&lang=ko');
console.log(url.search); // "?q=hello&lang=ko"

// URLSearchParams로 구조적 접근
console.log(url.searchParams.get('q'));    // "hello"
console.log(url.searchParams.get('lang')); // "ko"
```

##### fragment (프래그먼트)

`#` 뒤에 오는 문서 내 위치 식별자. 서버로 전송되지 않음. 기본값: null.

```javascript
const url = new URL('https://example.com/page#section-2');
console.log(url.hash); // "#section-2"

// fragment는 서버에 전송되지 않음
// fetch('https://example.com/page#section-2') 에서
// 실제 요청 URL은 'https://example.com/page'
```

#### 2.3 URL 구성 요소 요약표

- scheme
  - 기본값: (필수)
  - 직렬화 시 접두사/접미사: `:` 접미사
  - 예시: `https`
- username
  - 기본값: `""`
  - 직렬화 시 접두사/접미사: `@` 앞 (password와 함께)
  - 예시: `user`
- password
  - 기본값: `""`
  - 직렬화 시 접두사/접미사: `:` 접두사, `@` 접미사
  - 예시: `pass`
- host
  - 기본값: null
  - 직렬화 시 접두사/접미사: `//` 접두사
  - 예시: `example.com`
- port
  - 기본값: null
  - 직렬화 시 접두사/접미사: `:` 접두사
  - 예시: `8080`
- path
  - 기본값: `[]` 또는 `""`
  - 직렬화 시 접두사/접미사: `/`로 결합
  - 예시: `/a/b/c`
- query
  - 기본값: null
  - 직렬화 시 접두사/접미사: `?` 접두사
  - 예시: `key=value`
- fragment
  - 기본값: null
  - 직렬화 시 접두사/접미사: `#` 접두사
  - 예시: `section`

---
### 3. URL 파싱 알고리즘

#### 3.1 상태 머신 개요

WHATWG URL 파싱 알고리즘: 유한 상태 머신(Finite State Machine)으로 구현.
- 입력 문자열을 한 문자씩 순회
- 현재 상태와 읽은 문자에 따라 다음 상태로 전이 → URL의 각 구성 요소를 채워나감

```
[입력 문자열] → [전처리] → [상태 머신 파싱] → [URL 레코드 또는 실패]
```

전처리 단계 수행 내용:
- 선행/후행 C0 제어 문자 및 공백 제거
- 탭(`\t`)과 줄바꿈(`\n`, `\r`) 제거

```javascript
// 전처리 예시: 앞뒤 공백과 탭이 제거된다
const url = new URL('  \t https://example.com \n ');
console.log(url.href); // "https://example.com/"
```

#### 3.2 주요 파싱 상태 상세

파싱 알고리즘: 약 30개의 상태로 구성. 주요 상태를 순서대로 설명.

##### Scheme Start State

파싱의 시작 상태.
- 첫 번째 문자가 ASCII 영문자 → 소문자로 변환 후 버퍼에 추가 → Scheme State로 전이

```
입력: "Https://example.com"
       ^
상태: Scheme Start State
동작: 'H' → 소문자 'h' → 버퍼에 추가 → Scheme State로 전이
```

첫 문자가 영문자가 아니고 state override가 없으면 → No Scheme State로 전이.

##### Scheme State

스킴의 나머지 부분을 읽음. 허용 문자: ASCII 영문자·숫자·`+`·`-`·`.`

`:` 문자를 만나면:
- 버퍼에 모인 문자열이 스킴이 됨
- 특수 스킴 여부 확인
- 이후 `//`가 오면 Authority State로 전이 · 그렇지 않으면 다른 적절한 상태로 전이

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

##### Authority State

`//` 뒤에서 authority(인증 정보 + 호스트 + 포트) 파싱 시작.
- `@` 문자 발견 시 → 그 앞은 userinfo · 그 뒤부터는 호스트

```
입력: "https://user:pass@example.com:8080/path"
              ^^^^^^^^^^^^^^^^^^^^^^^^
              authority 영역
```

##### Host State

호스트 문자열 파싱.
- `[`를 만나면 → IPv6 파싱 모드로 진입
- `:`·`/`·`?`·`#`을 만나면 → 호스트 파싱 종료

```
입력: "https://example.com:8080/path"
              ^^^^^^^^^^^
              Host State에서 처리
```

##### Port State

`:` 이후의 포트 번호 파싱.
- 숫자만 허용
- 파싱 완료 시 기본 포트와 비교 → 동일하면 null로 설정

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

##### Path Start State / Path State

경로 파싱. 특수 스킴에서는 `\`도 `/`와 동일하게 경로 구분자로 취급.

```javascript
// 백슬래시가 슬래시로 정규화된다 (특수 스킴에서만)
const url1 = new URL('https://example.com/a\\b\\c');
console.log(url1.pathname); // "/a/b/c"

// 비특수 스킴에서는 백슬래시가 그대로 유지된다
const url2 = new URL('custom://host/a\\b\\c');
console.log(url2.pathname); // "/a%5Cb%5Cc"
```

경로 세그먼트에서 `.`과 `..`은 특별히 처리됨:

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

##### Query State

`?` 이후의 쿼리 문자열 파싱. `#`을 만나면 → 쿼리 파싱 종료 후 Fragment State로 전이.

특수 스킴과 비특수 스킴은 percent-encoding 규칙이 다름:
- 특수 스킴: special-query percent-encode set 사용
- 비특수 스킴: query percent-encode set 사용

```javascript
const url = new URL("https://example.com/search?q=hello world&lang=한국어");
console.log(url.search); // "?q=hello%20world&lang=%ED%95%9C%EA%B5%AD%EC%96%B4"
```

##### Fragment State

`#` 이후의 프래그먼트 파싱. 입력 끝까지 읽음.

```javascript
const url = new URL('https://example.com/page#섹션-1');
console.log(url.hash); // "#%EC%84%B9%EC%85%98-1"
```

#### 3.3 파싱 상태 전이 흐름도

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

#### 3.4 파싱 실패 사례

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

#### 3.5 유효성 검사 오류(Validation Errors)

파싱 성공하더라도 입력에 문제가 있을 수 있음 → 표준은 이를 "validation error"로 정의. 예:

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

### 4. 호스트 파싱

#### 4.1 호스트 타입

WHATWG URL Standard에서 호스트(host)는 다음 중 하나:

1. 도메인(domain): DNS에서 해석되는 문자열 (예: `example.com`)
2. IPv4 주소: 32비트 숫자 주소 (예: `192.168.1.1`)
3. IPv6 주소: 128비트 숫자 주소 (예: `[2001:db8::1]`)
4. 불투명 호스트(opaque host): 비특수 스킴에서 사용되는 percent-encoded 문자열
5. 빈 호스트(empty host): `file:` 스킴에서 로컬 파일을 가리킬 때

#### 4.2 도메인 파싱

도메인 파싱 단계:

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

#### 4.3 IPv4 주소 파싱

IPv4 주소 파싱: 단순한 점 표기법 이상을 처리 → 역사적 이유로 다양한 형식 지원.

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
2. 각 부분의 숫자 형식 감지 (10진·16진·8진)
3. 숫자로 변환
4. 범위 검증 (각 옥텟: 0~255, 또는 축약 시 마지막 부분에 따라 다름)
5. 32비트 정수로 결합
6. 표준 점 표기법으로 직렬화

#### 4.4 IPv6 주소 파싱

IPv6 주소는 `[`와 `]`로 감싸야 함.

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

#### 4.5 불투명 호스트(Opaque Host)

비특수 스킴에서는 호스트가 불투명 호스트로 파싱됨. 금지 문자를 제외한 모든 문자를 percent-encoding하여 저장.

```javascript
const url = new URL('custom://my host name/path');
// 비특수 스킴에서의 호스트 처리
console.log(url.hostname); // "my%20host%20name"
```

#### 4.6 IDNA (Internationalized Domain Names in Applications)

IDNA: 비ASCII 문자가 포함된 도메인 이름을 ASCII 호환 인코딩(ACE)으로 변환하는 프로토콜.
- WHATWG URL Standard는 IDNA 2008의 변형인 UTS46(Unicode IDNA Compatibility Processing)을 사용

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

- A-label: ASCII 호환 형태 (`xn--...`)
- U-label: 유니코드 원본 형태
- Punycode: 유니코드를 ASCII로 인코딩하는 알고리즘
- UTS46: IDNA 2003/2008 호환성 처리

---

### 5. Percent-encoding

#### 5.1 개념

Percent-encoding(퍼센트 인코딩): URL에서 허용되지 않는 바이트를 `%HH` 형태(H는 16진수)로 인코딩하는 메커니즘.

- UTF-8로 인코딩 → 각 바이트를 percent-encode

```javascript
// 한글 "안녕"의 percent-encoding
// "안" → UTF-8: 0xEC 0x95 0x88 → %EC%95%88
// "녕" → UTF-8: 0xEB 0x85 0x95 → %EB%85%95
const encoded = encodeURIComponent('안녕');
console.log(encoded); // "%EC%95%88%EB%85%95"
```

#### 5.2 Percent-encode Set (인코딩 대상 집합)

WHATWG URL Standard: URL의 각 부분마다 인코딩해야 하는 문자 집합을 정의.

- 더 제한적인 위치일수록 더 많은 문자를 인코딩

##### C0 Control Percent-encode Set

가장 기본적인 집합.

- 포함 범위: C0 제어 문자(U+0000~U+001F)·U+007F 이상의 모든 코드 포인트

```
범위: U+0000 ~ U+001F, U+007E 초과
```

##### Fragment Percent-encode Set

- C0 control set + 공백(` `)·`"`·`<`·`>`·백틱(`` ` ``)

```javascript
const url = new URL('https://example.com/page#hello world<>');
console.log(url.hash); // "#hello%20world%3C%3E"
```

##### Query Percent-encode Set

- C0 control set + 공백(` `)·`"`·`#`·`<`·`>`

```
인코딩 대상: C0 controls, space, ", #, <, >
```

##### Special-query Percent-encode Set

- Query set + `'`(작은따옴표)
- 특수 스킴(http, https 등)의 쿼리에서는 작은따옴표도 인코딩

```javascript
// 특수 스킴에서 작은따옴표 인코딩
const url = new URL("https://example.com/?q=it's");
console.log(url.search); // "?q=it%27s"

// 비특수 스킴에서는 작은따옴표가 그대로 유지
const url2 = new URL("custom://host/?q=it's");
console.log(url2.search); // "?q=it's"
```

##### Path Percent-encode Set

- Query set + `?`·`` ` ``·`{`·`}`

```javascript
const url = new URL('https://example.com/path with {braces}');
console.log(url.pathname); // "/path%20with%20%7Bbraces%7D"
```

##### Userinfo Percent-encode Set

- Path set + `/`·`:`·`;`·`=`·`@`·`[`·`\`·`]`·`^`·`|`

```javascript
const url = new URL('https://example.com');
url.username = 'user@domain';
url.password = 'p:ss/w=rd';
console.log(url.href);
// "https://user%40domain:p%3Ass%2Fw%3Drd@example.com/"
```

##### Component Percent-encode Set

- Userinfo set + `$`·`%`·`&`·`+`·`,`
- `URLSearchParams`에서 사용 → `application/x-www-form-urlencoded` 인코딩과 관련

#### 5.3 Percent-encode Set 포함 관계

```
C0 Control ⊂ Fragment ⊂ Query ⊂ Special-query
                         Query ⊂ Path ⊂ Userinfo ⊂ Component
```

#### 5.4 Percent-encode 알고리즘

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

#### 5.5 Percent-decode 알고리즘

Percent-decoding: `%HH` 시퀀스를 원래 바이트로 복원.

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

#### 5.6 application/x-www-form-urlencoded

HTML 폼에서 사용되는 특별한 인코딩 방식 → percent-encoding의 변형.

주요 차이점:
- 공백을 `%20`이 아닌 `+`로 인코딩
- `*`·`-`·`.`·`_` 이외의 비ASCII/비영숫자 문자를 모두 인코딩

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

### 6. URL 직렬화

#### 6.1 URL 직렬화 알고리즘

URL 직렬화(serialization): 파싱된 URL 레코드를 다시 문자열로 변환하는 과정.

- 이 과정 자체가 URL을 정규화(normalize)하는 효과를 가짐

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

#### 6.2 직렬화 예시

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

#### 6.3 호스트 직렬화

호스트 타입에 따라 직렬화 방식이 다름.

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

#### 6.4 프래그먼트 제외 직렬화

일부 컨텍스트(예: HTTP 요청)에서는 프래그먼트를 제외하고 직렬화해야 함.

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

### 7. Origin

#### 7.1 Origin이란

Origin(출처): 웹 보안의 핵심 개념 → 리소스가 어디에서 왔는지를 식별.

- 동일 출처 정책(Same-Origin Policy)의 기반
- WHATWG URL Standard에서 정의

Origin의 두 종류:
- tuple origin: (scheme, host, port, domain)으로 구성
- opaque origin: 내부적으로 고유한 식별자를 가지는 불투명한 origin

#### 7.2 Tuple Origin

- 특수 스킴(http, https, ftp, ws, wss)과 file 스킴에 대해 tuple origin 생성

```javascript
// tuple origin 예시
const url1 = new URL('https://example.com:443/path');
console.log(url1.origin); // "https://example.com" (기본 포트 생략)

const url2 = new URL('https://example.com:8443/path');
console.log(url2.origin); // "https://example.com:8443"

const url3 = new URL('http://localhost:3000/');
console.log(url3.origin); // "http://localhost:3000"
```

#### 7.3 Opaque Origin

- 비특수 스킴의 URL은 opaque origin을 가짐
- opaque origin은 다른 어떤 origin과도 동일하지 않음(자기 자신과도 동일하지 않음)

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

#### 7.4 Same Origin (동일 출처)

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

#### 7.5 Same Origin-Domain

`document.domain`을 통해 설정된 도메인까지 고려한 비교.

- 보안상의 이유로 점차 폐지(deprecated) 진행 중

```javascript
// same origin-domain은 document.domain 설정을 고려
// 예: a.example.com과 b.example.com이 모두
// document.domain = "example.com"으로 설정하면
// same origin-domain이 된다

// 주의: document.domain 설정은 현재 폐지 과정에 있다
// Chrome 106+에서는 기본적으로 비활성화
```

#### 7.6 Origin 직렬화

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

### 8. URL API

#### 8.1 URL 생성자

`URL` 생성자: 문자열을 파싱하여 URL 객체 생성.

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

#### 8.2 URL 속성

##### href

- 전체 URL 문자열
- 설정 시 URL 재파싱

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

##### origin (읽기 전용)

- URL의 origin 반환
- 읽기 전용 → 설정 불가

```javascript
const url = new URL('https://example.com:8443/path');
console.log(url.origin); // "https://example.com:8443"

// url.origin = 'https://other.com'; // 무시됨 (읽기 전용)
```

##### protocol

- 스킴에 `:`을 붙인 문자열

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

##### username / password

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

##### host / hostname

- `host`: 호스트+포트
- `hostname`: 호스트만

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

##### port

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

##### pathname

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

##### search

- 쿼리 문자열, `?` 포함하여 반환

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

##### hash

- 프래그먼트, `#` 포함하여 반환

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

#### 8.3 toString() / toJSON()

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

#### 8.4 URL 속성 변경의 상호 영향

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

### 9. URLSearchParams API

#### 9.1 생성자

- `URLSearchParams`: 쿼리 문자열을 구조적으로 조작하기 위한 API
- 여러 형태의 입력 지원

##### 문자열로 생성

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

##### 객체로 생성

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

##### 배열(이터러블)로 생성

- 동일한 키에 여러 값을 설정할 때 유용

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

#### 9.2 append(name, value)

- 새 키-값 쌍 추가
- 같은 키가 이미 있어도 새로 추가됨

```javascript
const params = new URLSearchParams();
params.append('tag', 'javascript');
params.append('tag', 'web');
params.append('tag', 'url');
console.log(params.toString()); // "tag=javascript&tag=web&tag=url"
console.log(params.getAll('tag')); // ["javascript", "web", "url"]
```

#### 9.3 delete(name, value?)

- 지정된 키(및 선택적으로 값)의 항목 제거

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

#### 9.4 get(name) / getAll(name)

- `get`: 첫 번째 값만 반환, 없으면 null
- `getAll`: 모든 값을 배열로 반환, 없으면 빈 배열

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

#### 9.5 has(name, value?)

- 지정된 키(및 선택적으로 값)의 존재 여부 반환

```javascript
const params = new URLSearchParams('color=red&color=blue&size=large');

console.log(params.has('color'));         // true
console.log(params.has('missing'));       // false

// 키+값으로 확인 (최근 추가된 기능)
console.log(params.has('color', 'red'));  // true
console.log(params.has('color', 'green')); // false
```

#### 9.6 set(name, value)

- 지정된 키의 첫 번째 항목의 값 설정
- 나머지 동일 키 항목은 제거
- 키가 없으면 새로 추가

```javascript
const params = new URLSearchParams('color=red&size=large&color=blue');

// 기존 키 설정: 첫 번째를 변경하고 나머지 제거
params.set('color', 'green');
console.log(params.toString()); // "color=green&size=large"

// 새 키 설정: 끝에 추가
params.set('weight', '100');
console.log(params.toString()); // "color=green&size=large&weight=100"
```

#### 9.7 sort()

- 키 이름 기준으로 모든 항목 정렬
- 동일한 키를 가진 항목들의 상대적 순서는 보존(안정 정렬)

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

#### 9.8 toString()

- 모든 항목을 `application/x-www-form-urlencoded` 형식으로 직렬화

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

#### 9.9 이터레이션

- `URLSearchParams`는 이터러블 → 다양한 방법으로 순회 가능

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

#### 9.10 URL 객체와의 연동

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

#### 9.11 실용적인 활용 예시

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

### 10. URL 패턴

#### 10.1 URLPattern API 개요

- `URLPattern`: URL 패턴 매칭을 위한 API
- WHATWG URL Standard와는 별도의 사양(URLPattern Standard)이지만 밀접하게 관련
- 라우팅·URL 필터링 등에 활용

참고: URLPattern은 Chrome/Edge 95+, Deno에 이어 Firefox 142+, Safari 26+에서도 지원 → 현재는 주요 브라우저 전반에서 폭넓게 사용 가능.

#### 10.2 기본 사용법

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

#### 10.3 패턴 문법

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

#### 10.4 라우팅 활용 예시

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

#### 10.5 URLPattern과 Service Worker

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

### 11. 특수 스킴(Special Schemes)

#### 11.1 특수 스킴이란

WHATWG URL Standard는 6개의 스킴을 "특수 스킴(special schemes)"으로 정의함 → 다른 스킴과 다르게 처리되는 규칙이 여러 가지 존재.

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

#### 11.2 특수 스킴의 특수 처리

##### 1) 경로 구분자로 `\` 허용

```javascript
// 특수 스킴에서는 \가 /로 정규화
const url1 = new URL('https://example.com/a\\b\\c');
console.log(url1.pathname); // "/a/b/c"

// 비특수 스킴에서는 \가 percent-encode
const url2 = new URL('custom://host/a\\b\\c');
console.log(url2.pathname); // "/a%5Cb%5Cc"
```

##### 2) 호스트 필수

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

##### 3) 기본 포트 생략

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

##### 4) 빈 경로 → `/`

```javascript
// 특수 스킴에서는 빈 경로가 /로 정규화
const url = new URL('https://example.com');
console.log(url.pathname); // "/"
console.log(url.href);     // "https://example.com/"
```

##### 5) 스킴 전환 제한

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

#### 11.3 file: 스킴의 특수성

- `file:` 스킴: 로컬 파일 시스템을 참조하는 스킴
  - 다른 특수 스킴과 차별화된 처리 규칙 적용

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

#### 11.4 각 스킴별 상세

##### ftp (File Transfer Protocol)

```javascript
const url = new URL('ftp://ftp.example.com/pub/files/readme.txt');
console.log(url.protocol); // "ftp:"
console.log(url.port);     // "" (기본 포트 21)
console.log(url.origin);   // "ftp://ftp.example.com"

// 인증 정보 포함
const url2 = new URL('ftp://user:pass@ftp.example.com/files/');
console.log(url2.username); // "user"
```

##### ws / wss (WebSocket)

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

### 12. Blob URL

#### 12.1 Blob URL 개요

- Blob URL(`blob:` 스킴): 메모리에 있는 `Blob`·`File` 객체를 참조하는 URL
  - 브라우저의 현재 세션에서만 유효
  - `URL.createObjectURL()`로 생성, `URL.revokeObjectURL()`로 해제

#### 12.2 createObjectURL

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

#### 12.3 revokeObjectURL

```javascript
const blob = new Blob(['data']);
const url = URL.createObjectURL(blob);

// URL 사용 후 반드시 해제해야 메모리 누수 방지
URL.revokeObjectURL(url);

// 해제된 URL은 더 이상 유효하지 않음
// img.src = url; // 이미지 로드 실패
```

#### 12.4 Blob URL의 Origin

- Blob URL의 origin: 해당 URL을 생성한 환경의 origin을 상속

```javascript
// https://example.com에서 생성된 blob URL
const blob = new Blob(['test']);
const blobUrl = URL.createObjectURL(blob);

const url = new URL(blobUrl);
console.log(url.origin); // "https://example.com" (생성 환경의 origin)
```

#### 12.5 Blob URL 구조

```
blob:https://example.com/550e8400-e29b-41d4-a716-446655440000
|__| |_________________| |____________________________________|
  |          |                          |
scheme   origin 부분              UUID (고유 식별자)
```

---

### 13. data: URL 처리

#### 13.1 data: URL 구조

- `data:` URL: 데이터를 URL 자체에 인라인으로 포함
  - 작은 파일을 외부 요청 없이 사용할 때 유용

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

#### 13.2 data: URL 파싱

WHATWG Fetch Standard에서 정의하는 data URL 처리 알고리즘

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

#### 13.3 data: URL의 Origin

- `data:` URL: 항상 opaque origin을 가짐 → 보안상 중요한 의미

```javascript
const url = new URL('data:text/plain,hello');
console.log(url.origin); // "null" (opaque origin)

// data: URL에서 로드된 콘텐츠는 어떤 사이트의 origin과도 동일하지 않다
// 따라서 XHR, fetch 등에서 same-origin 정책의 영향을 받는다
```

#### 13.4 data: URL 활용

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

### 14. 상대 URL 해석

#### 14.1 기본 개념

- 상대 URL: base URL을 기준으로 절대 URL로 해석됨
  - HTML의 `<base>` 태그·링크·리소스 참조 등에서 널리 사용

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

#### 14.2 상대 URL 해석 알고리즘

- 상대 URL 해석: URL 파싱 알고리즘의 일부로 수행
  - 스킴이 없는 입력이 주어지면 → base URL을 기반으로 절대 URL 구성

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

#### 14.3 경로 정규화

- 상대 경로 해석 후 `.`·`..` 세그먼트가 정규화됨

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

#### 14.4 빈 문자열 상대 URL

- 빈 문자열: 현재 URL의 복사본을 만듦(프래그먼트 제외)

```javascript
const base = 'https://example.com/path?q=1#frag';
const url = new URL('', base);
console.log(url.href); // "https://example.com/path?q=1"
// 프래그먼트가 제거된 것에 주의
```

#### 14.5 HTML에서의 base URL

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

### 15. URL 등가성(Equivalence)

#### 15.1 URL 비교의 어려움

- URL 비교: 단순 문자열 비교보다 복잡
  - 동일한 리소스를 가리키는 URL이 여러 문자열 표현을 가질 수 있음 → 단순 비교로는 동일성 판단 불가

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

#### 15.2 URL 정규화를 통한 비교

- WHATWG URL Standard의 파싱 + 직렬화 → URL 정규화 가능

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

#### 15.3 WHATWG 표준에서 정의하는 등가성

- 표준: 두 URL의 동일(equal) 여부 판단 시 직렬화 결과를 비교
  - exclude-fragment 플래그로 프래그먼트 제외 가능

```
URL A와 B가 동일한가?
1. A를 직렬화한다 (exclude-fragment 옵션 적용)
2. B를 직렬화한다 (동일 옵션 적용)
3. 두 문자열이 동일하면 URL이 동일하다
```

#### 15.4 정규화되지 않는 차이점

- URL 파싱으로도 정규화되지 않는 차이점 존재

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

#### 15.5 고급 URL 비교 구현

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

#### 15.6 캐시 키로서의 URL

- 브라우저 캐시에서 URL을 키로 사용할 때 → 정규화된 형태 사용

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

### 부록 A: 주요 참고 자료

- WHATWG URL Standard: https://url.spec.whatwg.org/
- MDN - URL API: https://developer.mozilla.org/ko/docs/Web/API/URL
- MDN - URLSearchParams: https://developer.mozilla.org/ko/docs/Web/API/URLSearchParams
- URLPattern Standard: https://urlpattern.spec.whatwg.org/
- RFC 3986 (URI): https://datatracker.ietf.org/doc/html/rfc3986
- RFC 3987 (IRI): https://datatracker.ietf.org/doc/html/rfc3987
- UTS #46 (IDNA): https://unicode.org/reports/tr46/

### 부록 B: 브라우저 호환성 요약

- URL() 생성자
  - Chrome 32+ · Firefox 26+ · Safari 7+ · Edge 12+
- URL.canParse()
  - Chrome 120+ · Firefox 115+ · Safari 17+ · Edge 120+
- URL.parse()
  - Chrome 126+ · Firefox 126+ · Safari 18+ · Edge 126+
- URLSearchParams
  - Chrome 49+ · Firefox 44+ · Safari 10.1+ · Edge 17+
- URLSearchParams.sort()
  - Chrome 61+ · Firefox 54+ · Safari 11+ · Edge 61+
- URLPattern
  - Chrome 95+ · Firefox 142+ · Safari 26+ · Edge 95+
- URLSearchParams.delete(name, value)
  - Chrome 117+ · Firefox 115+ · Safari 17+ · Edge 117+
- URLSearchParams.has(name, value)
  - Chrome 117+ · Firefox 115+ · Safari 17+ · Edge 117+

### 부록 C: Node.js에서의 URL 처리

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

### 부록 D: 자주 발생하는 실수와 해결 방법

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

### 부록 E: URL 파싱 구현 의사코드 (간략화)

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

이 문서는 WHATWG URL Standard(https://url.spec.whatwg.org/)를 기반으로 작성함. URL 표준은 Living Standard로서 지속적으로 업데이트됨 → 최신 내용은 공식 사양 문서 참고.

---

## WHATWG Fetch Standard 완벽 가이드

### 목차

1. [개요](#1-개요)
2. [기본 개념](#2-기본-개념)
3. [fetch() API](#3-fetch-api)
4. [Request 인터페이스](#4-request-인터페이스)
5. [Response 인터페이스](#5-response-인터페이스)
6. [Headers 인터페이스](#6-headers-인터페이스)
7. [Body Mixin](#7-body-mixin)
8. [CORS](#8-cors-cross-origin-resource-sharing)
9. [Request Mode](#9-request-mode)
10. [Credentials Mode](#10-credentials-mode)
11. [Cache Mode](#11-cache-mode)
12. [Redirect Mode](#12-redirect-mode)
13. [Referrer Policy](#13-referrer-policy)
14. [Subresource Integrity](#14-subresource-integrity-sri)
15. [Fetch 알고리즘](#15-fetch-알고리즘)
16. [Service Worker와의 관계](#16-service-worker와의-관계)
17. [Streaming](#17-streaming)
18. [AbortController를 이용한 요청 취소](#18-abortcontroller를-이용한-요청-취소)
19. [에러 처리](#19-에러-처리)

---

### 1. 개요

#### 1.1 Fetch Standard란?

- Fetch Standard: WHATWG(Web Hypertext Application Technology Working Group)에서 정의한 웹 표준
  - 네트워크 요청·응답 처리를 위한 통합 아키텍처 제공
  - `fetch()` API 정의뿐 아니라 브라우저가 리소스를 가져오는 전체 과정을 포괄적으로 명세
    - 요청(Request) 생성
    - 네트워크 전송
    - 응답(Response) 처리
    - CORS 정책 적용

- Fetch Standard의 핵심 목표
  - 통합된 리소스 획득 모델: HTML `<img>`·`<script>`·`<link>` 태그를 통한 리소스 로딩, CSS `@import`, JavaScript `fetch()` 호출 등 모든 리소스 획득 과정을 하나의 일관된 모델로 정의
  - 보안 모델의 명확화: Same-Origin Policy와 CORS 메커니즘을 정확히 명세 → 크로스 오리진 리소스 접근에 대한 보안 규칙 통일
  - Promise 기반의 현대적 API: 콜백 기반 레거시 API를 대체하는 깔끔하고 조합 가능한 비동기 인터페이스 제공

#### 1.2 왜 필요한가?

- Fetch Standard 이전 → 네트워크 요청을 위한 명확한 단일 표준 부재
- `XMLHttpRequest`가 사실상의 표준으로 사용되었으나 여러 한계 존재
  - 이벤트 기반의 복잡한 인터페이스 → 콜백 지옥(callback hell) 야기
  - 스트리밍 미지원 → 응답 전체가 도착해야만 데이터 접근 가능
  - Service Worker에서 사용 불가 → XHR은 Service Worker 컨텍스트에서 동작하지 않음
  - 요청/응답 객체의 부재 → 요청과 응답을 일급(first-class) 객체로 다룰 수 없음
  - 일관성 부족 → 브라우저의 내부 리소스 로딩 과정과 JavaScript API 사이 모델이 다름

#### 1.3 XMLHttpRequest와의 비교

- API 스타일
  - XMLHttpRequest: 이벤트 기반(콜백)
  - fetch(): Promise 기반
- 요청 취소
  - XMLHttpRequest: `abort()` 메서드
  - fetch(): `AbortController` / `AbortSignal`
- 스트리밍
  - XMLHttpRequest: 제한적(`responseType` 의존)
  - fetch(): `ReadableStream` 네이티브 지원
- 요청/응답 객체
  - XMLHttpRequest: 없음(단일 객체가 모든 것을 관리)
  - fetch(): `Request`, `Response` 분리
- Service Worker
  - XMLHttpRequest: 사용 불가
  - fetch(): 네이티브 지원
- CORS 처리
  - XMLHttpRequest: 암묵적
  - fetch(): `mode` 옵션으로 명시적 제어
- 쿠키 전송
  - XMLHttpRequest: 기본적으로 전송
  - fetch(): `credentials` 옵션으로 명시적 제어
- 진행 상황 추적
  - XMLHttpRequest: `onprogress` 이벤트
  - fetch(): `ReadableStream`으로 구현 가능
- 타임아웃
  - XMLHttpRequest: `timeout` 속성
  - fetch(): `AbortSignal.timeout()` 활용
- 동기 요청
  - XMLHttpRequest: 지원(비권장)
  - fetch(): 미지원(비동기 전용)

```javascript
// XMLHttpRequest 방식
function fetchDataXHR(url) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('GET', url);
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve(JSON.parse(xhr.responseText));
      } else {
        reject(new Error(`HTTP Error: ${xhr.status}`));
      }
    };
    xhr.onerror = () => reject(new Error('Network Error'));
    xhr.send();
  });
}

// fetch() 방식
async function fetchDataFetch(url) {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`HTTP Error: ${response.status}`);
  }
  return response.json();
}
```

#### 1.4 역사

- 2011-2012년: WHATWG 내에서 XHR을 대체할 새로운 API 논의 시작
- 2014년: Fetch Standard 초안 작성 시작 → "Fetch"라는 이름은 브라우저가 리소스를 "가져오는(fetch)" 내부 동작을 표준화한다는 의미에서 채택
- 2015년: Chrome 42, Firefox 39에서 `fetch()` API 구현 시작
- 2015-2017년: `Request`, `Response`, `Headers` 인터페이스와 `AbortController` 지원 점진적 추가
- 2017-2018년: Edge, Safari 등 주요 브라우저에서 완전한 지원 이루어짐
- 현재: Fetch Standard는 Living Standard로서 지속적으로 업데이트 중 → 스트리밍, `Response.json()` 정적 메서드 등 새로운 기능 계속 추가

---

### 2. 기본 개념

Fetch Standard는 네 가지 핵심 인터페이스를 중심으로 설계됨.

#### 2.1 Request

- `Request`: 리소스에 대한 요청을 나타내는 객체
  - HTTP 메서드·URL·헤더·본문(body) 등 요청에 필요한 모든 정보를 캡슐화

```javascript
// Request 객체 생성
const request = new Request('https://api.example.com/data', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({ key: 'value' }),
});

console.log(request.method);  // "POST"
console.log(request.url);     // "https://api.example.com/data"
```

#### 2.2 Response

- `Response`: 요청에 대한 응답을 나타내는 객체
  - 상태 코드·헤더·본문 등 응답 정보 포함

```javascript
// fetch()가 반환하는 Response
const response = await fetch('https://api.example.com/data');
console.log(response.status);     // 200
console.log(response.ok);         // true
console.log(response.statusText); // "OK"

// 수동으로 Response 생성 (Service Worker에서 유용)
const customResponse = new Response(JSON.stringify({ message: 'Hello' }), {
  status: 200,
  headers: { 'Content-Type': 'application/json' },
});
```

#### 2.3 Headers

- `Headers`: HTTP 헤더의 이름-값 쌍 목록을 나타내는 객체
  - 대소문자를 구분하지 않는 이름으로 헤더 관리

```javascript
const headers = new Headers();
headers.append('Content-Type', 'application/json');
headers.append('X-Custom-Header', 'custom-value');

console.log(headers.get('content-type')); // "application/json" (대소문자 무관)
console.log(headers.has('X-Custom-Header')); // true
```

#### 2.4 Body

- `Body`: Request와 Response 모두에 포함될 수 있는 본문 데이터
  - Body mixin → 본문 데이터를 다양한 형식(JSON, Text, Blob, ArrayBuffer, FormData)으로 읽을 수 있는 메서드 제공

```javascript
// Response body를 다양한 형식으로 읽기
const response = await fetch('https://api.example.com/data');

// JSON으로 읽기
const jsonData = await response.json();

// 텍스트로 읽기 (새 요청 필요 - body는 한 번만 읽을 수 있음)
const response2 = await fetch('https://api.example.com/data');
const textData = await response2.text();
```

- 주의: Body는 한 번만 소비(consume) 가능
  - `bodyUsed` 속성 → 이미 소비되었는지 확인 가능
  - 여러 번 읽어야 할 경우 `clone()` 사용 필요

```javascript
const response = await fetch('https://api.example.com/data');
console.log(response.bodyUsed); // false

const data = await response.json();
console.log(response.bodyUsed); // true

// 이 시점에서 response.text()를 호출하면 TypeError 발생
// await response.text(); // TypeError: body stream already read

// 해결 방법: clone() 사용
const response3 = await fetch('https://api.example.com/data');
const cloned = response3.clone();
const json = await response3.json();
const text = await cloned.text();
```

---

### 3. fetch() API

#### 3.1 기본 사용법

- `fetch()` 함수: 전역(global) 스코프에서 사용 가능
  - 네트워크 요청 수행 → `Promise<Response>` 반환

```javascript
// 가장 기본적인 사용
fetch('https://api.example.com/users')
  .then(response => response.json())
  .then(data => console.log(data))
  .catch(error => console.error('Error:', error));

// async/await 사용
async function getUsers() {
  try {
    const response = await fetch('https://api.example.com/users');
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    const users = await response.json();
    return users;
  } catch (error) {
    console.error('Failed to fetch users:', error);
  }
}
```

`fetch()` 함수 시그니처:

```
fetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response>
```

- `input`: URL 문자열, `URL` 객체, 또는 `Request` 객체
- `init`: 요청 설정 옵션(선택적)

#### 3.2 옵션 (RequestInit)

`fetch()`의 두 번째 매개변수로 전달하는 옵션 객체의 전체 속성.

##### 3.2.1 method

- HTTP 요청 메서드 지정. 기본값 `"GET"`

```javascript
// GET 요청 (기본값)
await fetch('/api/users');

// POST 요청
await fetch('/api/users', { method: 'POST', body: JSON.stringify(newUser) });

// PUT 요청
await fetch('/api/users/1', { method: 'PUT', body: JSON.stringify(updatedUser) });

// PATCH 요청
await fetch('/api/users/1', { method: 'PATCH', body: JSON.stringify(partialUpdate) });

// DELETE 요청
await fetch('/api/users/1', { method: 'DELETE' });

// HEAD 요청 (헤더만 가져옴)
await fetch('/api/users', { method: 'HEAD' });

// OPTIONS 요청 (CORS preflight에 사용됨)
await fetch('/api/users', { method: 'OPTIONS' });
```

##### 3.2.2 headers

- 요청에 포함할 HTTP 헤더 지정

```javascript
// 객체 리터럴 사용
await fetch('/api/data', {
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer eyJhbGciOiJIUzI1NiIs...',
    'Accept': 'application/json',
    'X-Request-ID': crypto.randomUUID(),
  },
});

// Headers 객체 사용
const headers = new Headers();
headers.set('Content-Type', 'application/json');
headers.set('Authorization', 'Bearer token123');
await fetch('/api/data', { headers });

// 배열의 배열 사용
await fetch('/api/data', {
  headers: [
    ['Content-Type', 'application/json'],
    ['Accept', 'application/json'],
  ],
});
```

##### 3.2.3 body

- 요청 본문 지정
- `GET`과 `HEAD` 메서드에서는 body 사용 불가

```javascript
// JSON 본문
await fetch('/api/users', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ name: '홍길동', email: 'hong@example.com' }),
});

// FormData (multipart/form-data로 자동 설정)
const formData = new FormData();
formData.append('name', '홍길동');
formData.append('avatar', fileInput.files[0]);
await fetch('/api/users', { method: 'POST', body: formData });

// URLSearchParams (application/x-www-form-urlencoded로 자동 설정)
const params = new URLSearchParams();
params.append('username', 'hong');
params.append('password', 'secret');
await fetch('/api/login', { method: 'POST', body: params });

// Blob
const blob = new Blob(['Hello, World!'], { type: 'text/plain' });
await fetch('/api/upload', { method: 'POST', body: blob });

// ArrayBuffer / TypedArray
const buffer = new Uint8Array([72, 101, 108, 108, 111]);
await fetch('/api/upload', { method: 'POST', body: buffer });

// ReadableStream
const stream = new ReadableStream({
  start(controller) {
    controller.enqueue(new TextEncoder().encode('chunk1'));
    controller.enqueue(new TextEncoder().encode('chunk2'));
    controller.close();
  },
});
await fetch('/api/upload', { method: 'POST', body: stream, duplex: 'half' });
```

##### 3.2.4 mode

- 요청의 모드 지정 → CORS 동작 제어

```javascript
// cors (기본값) - 크로스 오리진 요청 허용, CORS 헤더 필요
await fetch('https://other-domain.com/api', { mode: 'cors' });

// no-cors - 크로스 오리진이지만 CORS 헤더 불필요 (opaque 응답)
await fetch('https://other-domain.com/image.png', { mode: 'no-cors' });

// same-origin - 같은 오리진만 허용
await fetch('/api/data', { mode: 'same-origin' });

// navigate - 내비게이션 요청용 (일반 코드에서 사용 불가)
```

##### 3.2.5 credentials

- 요청에 자격 증명(쿠키, HTTP 인증 등) 포함 여부 결정

```javascript
// same-origin (기본값) - 같은 오리진일 때만 자격 증명 포함
await fetch('/api/profile', { credentials: 'same-origin' });

// include - 항상 자격 증명 포함 (크로스 오리진도)
await fetch('https://api.other.com/profile', { credentials: 'include' });

// omit - 자격 증명을 절대 포함하지 않음
await fetch('/api/public-data', { credentials: 'omit' });
```

##### 3.2.6 cache

- HTTP 캐시와의 상호작용 방식 지정

```javascript
// default - 브라우저 기본 캐시 동작
await fetch('/api/data', { cache: 'default' });

// no-store - 캐시를 완전히 무시
await fetch('/api/data', { cache: 'no-store' });

// reload - 캐시를 무시하고 항상 네트워크에서 가져옴 (응답으로 캐시 업데이트 가능)
await fetch('/api/data', { cache: 'reload' });

// no-cache - 항상 서버에 조건부 요청으로 검증
await fetch('/api/data', { cache: 'no-cache' });

// force-cache - 캐시에 있으면 (만료되었더라도) 캐시 사용
await fetch('/api/data', { cache: 'force-cache' });

// only-if-cached - 캐시에 있을 때만 반환 (mode: 'same-origin'과 함께 사용)
await fetch('/api/data', { cache: 'only-if-cached', mode: 'same-origin' });
```

##### 3.2.7 redirect

- 리다이렉트 처리 방식 지정

```javascript
// follow (기본값) - 리다이렉트 자동 추적
await fetch('/api/old-endpoint', { redirect: 'follow' });

// error - 리다이렉트 발생 시 에러
await fetch('/api/old-endpoint', { redirect: 'error' });

// manual - 리다이렉트를 추적하지 않고 opaqueredirect 응답 반환
await fetch('/api/old-endpoint', { redirect: 'manual' });
```

##### 3.2.8 referrer

- 요청의 referrer 지정

```javascript
// 기본값 - 현재 페이지 URL이 referrer
await fetch('/api/data');

// 특정 URL 지정
await fetch('/api/data', { referrer: 'https://example.com/page' });

// 빈 문자열 - referrer 없음
await fetch('/api/data', { referrer: '' });

// "about:client" - 기본 referrer (기본값과 동일)
await fetch('/api/data', { referrer: 'about:client' });
```

##### 3.2.9 referrerPolicy

- Referrer 헤더에 포함되는 정보의 범위 제어

```javascript
await fetch('/api/data', { referrerPolicy: 'no-referrer' });
await fetch('/api/data', { referrerPolicy: 'no-referrer-when-downgrade' });
await fetch('/api/data', { referrerPolicy: 'origin' });
await fetch('/api/data', { referrerPolicy: 'origin-when-cross-origin' });
await fetch('/api/data', { referrerPolicy: 'same-origin' });
await fetch('/api/data', { referrerPolicy: 'strict-origin' });
await fetch('/api/data', { referrerPolicy: 'strict-origin-when-cross-origin' });
await fetch('/api/data', { referrerPolicy: 'unsafe-url' });
```

##### 3.2.10 integrity

- Subresource Integrity(SRI) 해시 지정 → 응답 본문의 무결성 검증

```javascript
await fetch('https://cdn.example.com/lib.js', {
  integrity: 'sha256-BpfBw7ivV8q2jLiT13fxDYAe2tJllusRSZ273h2nFSE=',
});
```

##### 3.2.11 keepalive

- 페이지가 종료(unload)된 후에도 요청이 완료될 수 있도록 함 → 분석 데이터 전송 등에 유용

```javascript
// 페이지 종료 시 분석 데이터 전송
window.addEventListener('unload', () => {
  fetch('/api/analytics', {
    method: 'POST',
    body: JSON.stringify({ event: 'page_unload', timestamp: Date.now() }),
    keepalive: true,
    headers: { 'Content-Type': 'application/json' },
  });
});

// navigator.sendBeacon()과 유사한 역할
document.addEventListener('visibilitychange', () => {
  if (document.visibilityState === 'hidden') {
    fetch('/api/analytics/heartbeat', {
      method: 'POST',
      body: JSON.stringify({ lastActive: Date.now() }),
      keepalive: true,
      headers: { 'Content-Type': 'application/json' },
    });
  }
});
```

##### 3.2.12 signal

- `AbortSignal` 객체를 전달 → 요청 취소 가능

```javascript
const controller = new AbortController();

// 5초 후 자동 취소
setTimeout(() => controller.abort(), 5000);

try {
  const response = await fetch('/api/slow-endpoint', {
    signal: controller.signal,
  });
  const data = await response.json();
} catch (error) {
  if (error.name === 'AbortError') {
    console.log('요청이 취소되었습니다.');
  }
}

// AbortSignal.timeout() 사용 (더 간편)
try {
  const response = await fetch('/api/data', {
    signal: AbortSignal.timeout(5000),
  });
} catch (error) {
  if (error.name === 'TimeoutError') {
    console.log('요청 시간이 초과되었습니다.');
  }
}
```

##### 3.2.13 duplex

- 요청 본문을 스트림으로 보낼 때 지정해야 하는 옵션
  - 현재는 `'half'`만 유효한 값
  - 요청을 반이중(half-duplex)으로 전송함(본문을 다 보내기 전에는 응답을 읽지 않음)을 명시
  - `ReadableStream`을 `body`로 사용할 때 필수

```javascript
await fetch('/api/upload', {
  method: 'POST',
  body: readableStream,
  duplex: 'half',
});
```

##### 3.2.14 priority

- 요청의 상대적 우선순위를 브라우저에 알려줌
  - `"high"`, `"low"`, `"auto"`(기본값) 중 하나 지정

```javascript
// 중요한 API 응답은 높은 우선순위로
await fetch('/api/critical-data', { priority: 'high' });

// 백그라운드 프리페치는 낮은 우선순위로
await fetch('/api/prefetch-data', { priority: 'low' });
```

#### 3.3 반환값 (Promise<Response>)

- `fetch()`는 항상 `Promise<Response>` 반환
- 중요: HTTP 에러 상태(4xx, 5xx)에서도 Promise가 reject되지 않음
  - 오직 네트워크 에러(네트워크 단절, DNS 실패 등)에서만 reject

```javascript
// HTTP 404도 fulfilled 상태로 resolve됨
const response = await fetch('/api/nonexistent');
console.log(response.status); // 404
console.log(response.ok);     // false (status가 200-299 범위 밖)

// 네트워크 에러만 catch로 잡힘
try {
  await fetch('https://unreachable-server.example.com');
} catch (error) {
  console.log(error); // TypeError: Failed to fetch
}

// 올바른 에러 처리 패턴
async function safeFetch(url, options) {
  const response = await fetch(url, options);
  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(`HTTP ${response.status}: ${errorBody}`);
  }
  return response;
}
```

---

### 4. Request 인터페이스

#### 4.1 생성

Request 생성자 호출 형태 - 두 가지.

```javascript
// 1. URL 문자열 + 옵션
const req1 = new Request('https://api.example.com/users', {
  method: 'GET',
  headers: { 'Accept': 'application/json' },
});

// 2. 기존 Request 객체 + 옵션 오버라이드
const baseRequest = new Request('https://api.example.com/users', {
  headers: { 'Authorization': 'Bearer token123' },
});
const req2 = new Request(baseRequest, { method: 'POST' });
// req2는 baseRequest의 헤더를 상속하면서 method만 POST로 변경

// 3. fetch()에 Request 객체 전달
const request = new Request('/api/data', { method: 'GET' });
const response = await fetch(request);
```

#### 4.2 속성

##### 읽기 전용 속성들

```javascript
const request = new Request('https://api.example.com/users?page=1', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer token',
  },
  body: JSON.stringify({ name: '홍길동' }),
  mode: 'cors',
  credentials: 'include',
  cache: 'no-cache',
  redirect: 'follow',
  referrer: 'about:client',
  referrerPolicy: 'strict-origin-when-cross-origin',
  integrity: '',
  keepalive: false,
});

// 기본 속성
console.log(request.method);          // "POST"
console.log(request.url);             // "https://api.example.com/users?page=1"
console.log(request.headers);         // Headers 객체
console.log(request.destination);     // "" (프로그래밍 방식 fetch의 경우)
console.log(request.referrer);        // "about:client"
console.log(request.referrerPolicy);  // "strict-origin-when-cross-origin"
console.log(request.mode);            // "cors"
console.log(request.credentials);     // "include"
console.log(request.cache);           // "no-cache"
console.log(request.redirect);        // "follow"
console.log(request.integrity);       // ""
console.log(request.keepalive);       // false
console.log(request.signal);          // AbortSignal 객체

// Body 관련 속성
console.log(request.body);            // ReadableStream
console.log(request.bodyUsed);        // false
```

##### destination 속성

- destination: 요청이 어떤 종류의 리소스를 위한 것인지 나타내는 속성
  - 프로그래밍 방식 fetch() 호출 → 빈 문자열
  - 브라우저 내부 리소스 로딩 → 다양한 값을 가짐

```javascript
// destination 가능한 값들:
// ""            - fetch() 직접 호출
// "audio"       - <audio> 태그
// "audioworklet" - AudioWorklet
// "document"    - 내비게이션
// "embed"       - <embed> 태그
// "font"        - CSS @font-face
// "frame"       - <frame> 태그
// "iframe"      - <iframe> 태그
// "image"       - <img>, CSS background-image 등
// "json"        - import ... with { type: "json" } 등 JSON 모듈
// "manifest"    - <link rel="manifest">
// "object"      - <object> 태그
// "paintworklet" - CSS Paint API
// "report"      - CSP report, NEL report
// "script"      - <script> 태그
// "serviceworker" - Service Worker 등록
// "sharedworker" - SharedWorker
// "style"       - <link rel="stylesheet">, CSS @import
// "track"       - <track> 태그
// "video"       - <video> 태그
// "webidentity" - FedCM(Federated Credential Management)
// "worker"      - Worker
// "xslt"        - XSLT 스타일시트
```

##### isReloadNavigation, isHistoryNavigation

```javascript
// Service Worker 내에서 유용
self.addEventListener('fetch', (event) => {
  const request = event.request;

  if (request.isReloadNavigation) {
    // 사용자가 새로고침(F5, Ctrl+R)한 경우
    console.log('페이지 새로고침 요청');
  }

  if (request.isHistoryNavigation) {
    // 뒤로/앞으로 가기 버튼 사용
    console.log('히스토리 내비게이션 요청');
  }
});
```

#### 4.3 clone()

- Request 객체를 복제
- body가 이미 소비된 Request → 복제 불가

```javascript
const original = new Request('/api/data', {
  method: 'POST',
  body: JSON.stringify({ key: 'value' }),
  headers: { 'Content-Type': 'application/json' },
});

const clone = original.clone();

// 두 요청 모두 독립적으로 사용 가능
const response1 = await fetch(original);
const response2 = await fetch(clone);

// body가 이미 소비된 후에는 clone 불가
const consumed = new Request('/api/data', { method: 'POST', body: 'test' });
await consumed.text(); // body 소비
// consumed.clone(); // TypeError: body stream already read
```

---

### 5. Response 인터페이스

#### 5.1 생성

```javascript
// 기본 생성
const response = new Response('Hello, World!', {
  status: 200,
  statusText: 'OK',
  headers: {
    'Content-Type': 'text/plain',
  },
});

// JSON 응답 생성
const jsonResponse = new Response(JSON.stringify({ message: 'success' }), {
  status: 200,
  headers: { 'Content-Type': 'application/json' },
});

// 빈 응답
const emptyResponse = new Response(null, { status: 204 });

// Blob을 body로 사용
const blob = new Blob(['<h1>Hello</h1>'], { type: 'text/html' });
const htmlResponse = new Response(blob);

// ReadableStream을 body로 사용
const stream = new ReadableStream({
  start(controller) {
    controller.enqueue(new TextEncoder().encode('streaming data'));
    controller.close();
  },
});
const streamResponse = new Response(stream);
```

#### 5.2 정적 메서드

##### Response.error()

- 네트워크 에러를 나타내는 Response 생성
- type이 "error"인 Response 반환

```javascript
const errorResponse = Response.error();
console.log(errorResponse.type);   // "error"
console.log(errorResponse.status); // 0
console.log(errorResponse.ok);     // false

// Service Worker에서 활용
self.addEventListener('fetch', (event) => {
  event.respondWith(
    fetch(event.request).catch(() => Response.error())
  );
});
```

##### Response.redirect()

- 리다이렉트 응답 생성
- status는 301·302·303·307·308 중 하나여야 함

```javascript
const redirect301 = Response.redirect('https://example.com/new-url', 301);
console.log(redirect301.status);                    // 301
console.log(redirect301.headers.get('Location'));    // "https://example.com/new-url"

const redirect302 = Response.redirect('/new-path', 302);
console.log(redirect302.status); // 302

// Service Worker에서 리다이렉트 구현
self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);
  if (url.pathname === '/old-path') {
    event.respondWith(Response.redirect('/new-path', 301));
  }
});
```

##### Response.json()

- JSON 데이터로부터 Response를 생성하는 편의 메서드
- Content-Type이 자동으로 application/json으로 설정됨

```javascript
// 기존 방식
const oldWay = new Response(JSON.stringify({ key: 'value' }), {
  headers: { 'Content-Type': 'application/json' },
});

// Response.json() 사용 (더 간편)
const newWay = Response.json({ key: 'value' });

// 상태 코드 지정
const errorJson = Response.json(
  { error: 'Not Found', message: '리소스를 찾을 수 없습니다.' },
  { status: 404 }
);

// Service Worker에서 활용
self.addEventListener('fetch', (event) => {
  if (event.request.url.endsWith('/api/health')) {
    event.respondWith(Response.json({ status: 'healthy', timestamp: Date.now() }));
  }
});
```

#### 5.3 속성

```javascript
const response = await fetch('https://api.example.com/data');

// type - 응답의 유형
console.log(response.type);
// "basic"          - same-origin 응답
// "cors"           - 유효한 CORS 크로스 오리진 응답
// "error"          - 네트워크 에러
// "opaque"         - no-cors 모드의 크로스 오리진 응답
// "opaqueredirect" - redirect: "manual" 모드의 리다이렉트 응답

// url - 응답의 최종 URL (리다이렉트 후의 URL)
console.log(response.url); // "https://api.example.com/data"

// redirected - 리다이렉트를 거쳤는지 여부
console.log(response.redirected); // false

// status - HTTP 상태 코드
console.log(response.status); // 200

// ok - status가 200-299 범위인지
console.log(response.ok); // true

// statusText - 상태 메시지
console.log(response.statusText); // "OK"

// headers - 응답 헤더
console.log(response.headers.get('Content-Type'));

// body - 응답 본문 (ReadableStream)
console.log(response.body); // ReadableStream

// bodyUsed - 본문이 이미 소비되었는지
console.log(response.bodyUsed); // false
```

#### 5.4 clone()

```javascript
const response = await fetch('/api/data');
const clone = response.clone();

// 원본과 복제본을 독립적으로 사용
const jsonData = await response.json();
const textData = await clone.text();
console.log(jsonData);
console.log(textData);

// 캐싱 패턴: 하나는 캐시에, 하나는 즉시 사용
async function fetchAndCache(request) {
  const response = await fetch(request);
  const cache = await caches.open('my-cache');
  cache.put(request, response.clone()); // 캐시에 복제본 저장
  return response; // 원본 반환
}
```

---

### 6. Headers 인터페이스

#### 6.1 생성

```javascript
// 빈 Headers
const headers1 = new Headers();

// 객체 리터럴로 초기화
const headers2 = new Headers({
  'Content-Type': 'application/json',
  'Accept': 'application/json',
  'Authorization': 'Bearer token123',
});

// 배열의 배열로 초기화 (동일 이름의 헤더 가능)
const headers3 = new Headers([
  ['Accept', 'text/html'],
  ['Accept', 'application/json'],
  ['X-Custom', 'value'],
]);

// 기존 Headers로 초기화 (복사)
const headers4 = new Headers(headers2);
```

#### 6.2 메서드

##### append(name, value)

- 기존 헤더에 값을 추가
- 같은 이름의 헤더가 이미 있으면 → 값이 결합됨

```javascript
const headers = new Headers();
headers.append('Accept', 'text/html');
headers.append('Accept', 'application/json');
console.log(headers.get('Accept')); // "text/html, application/json"

headers.append('X-Custom', 'value1');
headers.append('X-Custom', 'value2');
console.log(headers.get('X-Custom')); // "value1, value2"
```

##### set(name, value)

- 헤더의 값을 설정
- 이미 존재하면 대체, 없으면 새로 추가

```javascript
const headers = new Headers();
headers.set('Content-Type', 'text/plain');
console.log(headers.get('Content-Type')); // "text/plain"

headers.set('Content-Type', 'application/json');
console.log(headers.get('Content-Type')); // "application/json" (대체됨)
```

##### get(name)

- 헤더의 값을 반환
- 없으면 null 반환

```javascript
const headers = new Headers({ 'Content-Type': 'application/json' });
console.log(headers.get('Content-Type'));    // "application/json"
console.log(headers.get('content-type'));    // "application/json" (대소문자 무관)
console.log(headers.get('X-Nonexistent'));   // null
```

##### has(name)

- 해당 이름의 헤더가 존재하는지 확인

```javascript
const headers = new Headers({ 'Content-Type': 'application/json' });
console.log(headers.has('Content-Type'));  // true
console.log(headers.has('Authorization')); // false
```

##### delete(name)

- 해당 이름의 헤더를 삭제

```javascript
const headers = new Headers({
  'Content-Type': 'application/json',
  'Authorization': 'Bearer token',
});
headers.delete('Authorization');
console.log(headers.has('Authorization')); // false
```

##### forEach(callback)

- 모든 헤더를 순회

```javascript
const headers = new Headers({
  'Content-Type': 'application/json',
  'Accept': 'text/html',
  'X-Custom': 'value',
});

headers.forEach((value, name) => {
  console.log(`${name}: ${value}`);
});
// accept: text/html
// content-type: application/json
// x-custom: value
// (이름이 소문자로 정규화됨, 알파벳 순으로 정렬됨)
```

##### entries(), keys(), values()

- 이터레이터를 반환

```javascript
const headers = new Headers({
  'Content-Type': 'application/json',
  'Accept': 'text/html',
});

// entries()
for (const [name, value] of headers.entries()) {
  console.log(`${name}: ${value}`);
}

// keys()
for (const name of headers.keys()) {
  console.log(name); // "accept", "content-type"
}

// values()
for (const value of headers.values()) {
  console.log(value); // "text/html", "application/json"
}

// 전개 연산자 사용
const headerArray = [...headers]; // [["accept", "text/html"], ["content-type", "application/json"]]
const headerObj = Object.fromEntries(headers);
```

##### getSetCookie()

- Set-Cookie 헤더는 여러 개가 존재할 수 있음 → get()은 값을 콤마로 합쳐 반환 → 개별 쿠키 구분이 어려움
- getSetCookie() → 모든 Set-Cookie 값을 배열로 그대로 반환
- 주로 서비스 워커·Deno·Node.js 같은 서버/워커 환경에서 응답의 Set-Cookie 헤더를 다룰 때 유용

```javascript
const headers = new Headers();
headers.append('Set-Cookie', 'a=1');
headers.append('Set-Cookie', 'b=2');

headers.get('Set-Cookie');        // "a=1, b=2" (구분이 애매함)
headers.getSetCookie();           // ["a=1", "b=2"]
```

#### 6.3 Headers Guard

- Headers 객체에는 "guard"라는 내부 속성 존재 → 특정 헤더의 변경을 제한
- API를 통해 직접 접근은 불가하지만, 동작 방식 이해는 중요

Guard 종류 및 적용 대상.

- "none": 제한 없음
  - 적용 대상: `new Headers()`
- "request": forbidden header name 수정 불가
  - 적용 대상: Request 객체의 headers
- "request-no-cors": CORS-safelisted 헤더만 허용
  - 적용 대상: no-cors 모드 Request의 headers
- "response": forbidden response header name 수정 불가
  - 적용 대상: Response 객체의 headers
- "immutable": 모든 수정 불가
  - 적용 대상: error(), redirect() 등의 응답 headers

```javascript
// "none" guard - 제한 없음
const headers = new Headers();
headers.set('Set-Cookie', 'test=1'); // 가능

// "request" guard - 금지된 요청 헤더 설정 불가
const request = new Request('/api');
request.headers.set('Content-Type', 'text/plain'); // 가능
// request.headers.set('Host', 'evil.com');  // 무시됨 (forbidden header)

// "immutable" guard - 수정 불가
const errorResp = Response.error();
// errorResp.headers.set('X-Custom', 'test'); // TypeError
```

- Forbidden Header Names: 요청에서 스크립트가 직접 설정할 수 없는, 브라우저가 자동 관리하는 헤더

`Accept-Charset`, `Accept-Encoding`, `Access-Control-Request-Headers`, `Access-Control-Request-Method`, `Connection`, `Content-Length`, `Cookie`, `Cookie2`, `Date`, `DNT`, `Expect`, `Host`, `Keep-Alive`, `Origin`, `Referer`, `TE`, `Trailer`, `Transfer-Encoding`, `Upgrade`, `Via`, `Proxy-*`, `Sec-*`

- 참고: Set-Cookie는 위 목록과는 별개로 forbidden response header name → 요청 헤더가 아니라 응답 헤더로서 스크립트가 Response.headers를 통해 읽거나 설정할 수 없도록 금지됨 → "request" guard가 아니라 "response"/"immutable" guard가 적용되는 개념

---

### 7. Body Mixin

- Body mixin은 Request·Response 모두에 구현됨
- 본문 데이터를 다양한 형식으로 읽을 수 있는 메서드 제공

#### 7.1 text()

- 본문을 UTF-8 문자열로 읽음

```javascript
const response = await fetch('/api/data');
const text = await response.text();
console.log(text); // "Hello, World!"

// HTML 파싱에 활용
const htmlResponse = await fetch('/page.html');
const html = await htmlResponse.text();
const parser = new DOMParser();
const doc = parser.parseFromString(html, 'text/html');
```

#### 7.2 json()

- 본문을 JSON으로 파싱

```javascript
const response = await fetch('/api/users');
const users = await response.json();
console.log(users); // [{id: 1, name: "홍길동"}, ...]

// 잘못된 JSON이면 SyntaxError 발생
try {
  const resp = await fetch('/not-json');
  const data = await resp.json(); // SyntaxError 가능
} catch (e) {
  if (e instanceof SyntaxError) {
    console.error('잘못된 JSON 형식');
  }
}
```

#### 7.3 blob()

- 본문을 Blob 객체로 읽음

```javascript
// 이미지 다운로드
const response = await fetch('/images/photo.png');
const blob = await response.blob();
const imageUrl = URL.createObjectURL(blob);
const img = document.createElement('img');
img.src = imageUrl;
document.body.appendChild(img);

// 파일 다운로드 트리거
const fileResponse = await fetch('/api/export/report.pdf');
const fileBlob = await fileResponse.blob();
const downloadUrl = URL.createObjectURL(fileBlob);
const a = document.createElement('a');
a.href = downloadUrl;
a.download = 'report.pdf';
a.click();
URL.revokeObjectURL(downloadUrl);
```

#### 7.4 arrayBuffer()

- 본문을 ArrayBuffer로 읽음

```javascript
// 바이너리 데이터 처리
const response = await fetch('/api/binary-data');
const buffer = await response.arrayBuffer();
const view = new DataView(buffer);
console.log(view.getUint32(0)); // 처음 4바이트를 unsigned 32비트 정수로

// 오디오 디코딩
const audioResponse = await fetch('/audio/music.mp3');
const audioBuffer = await audioResponse.arrayBuffer();
const audioContext = new AudioContext();
const decodedAudio = await audioContext.decodeAudioData(audioBuffer);

// WebAssembly 모듈 로딩
const wasmResponse = await fetch('/module.wasm');
const wasmBuffer = await wasmResponse.arrayBuffer();
const wasmModule = await WebAssembly.instantiate(wasmBuffer);
```

#### 7.5 formData()

- 본문을 FormData 객체로 파싱
- multipart/form-data 또는 application/x-www-form-urlencoded 형식이어야 함

```javascript
// multipart/form-data 응답 파싱
const response = await fetch('/api/form-response');
const formData = await response.formData();
console.log(formData.get('username'));
console.log(formData.get('email'));

// Request에서 FormData 읽기 (Service Worker에서 유용)
self.addEventListener('fetch', async (event) => {
  if (event.request.method === 'POST') {
    const formData = await event.request.formData();
    console.log('제출된 이름:', formData.get('name'));
  }
});
```

#### 7.6 body (ReadableStream)

- body 속성은 본문의 ReadableStream에 직접 접근할 수 있게 함

```javascript
const response = await fetch('/api/large-data');
const reader = response.body.getReader();
const decoder = new TextDecoder();
let result = '';

while (true) {
  const { done, value } = await reader.read();
  if (done) break;
  result += decoder.decode(value, { stream: true });
  console.log(`받은 데이터 크기: ${value.length} bytes`);
}
console.log('최종 결과:', result);
```

#### 7.7 bodyUsed

```javascript
const response = await fetch('/api/data');
console.log(response.bodyUsed); // false

const text = await response.text();
console.log(response.bodyUsed); // true

try {
  await response.json(); // TypeError: body stream already read
} catch (e) {
  console.error(e.message);
}
```

---

### 8. CORS (Cross-Origin Resource Sharing)

#### 8.1 Same-Origin Policy (동일 출처 정책)

동일 출처 정책은 웹 보안의 핵심 메커니즘.

- "출처(origin)": 프로토콜(scheme)·호스트(host)·포트(port)의 조합으로 정의

```
https://example.com:443/path/page.html
  |        |          |
scheme   host       port
  \________|________/
        origin
```

```javascript
// https://www.example.com 기준 동일 출처 판단
// https://www.example.com/page2   -> 동일 출처 (경로만 다름)
// https://www.example.com:443/    -> 동일 출처 (443은 HTTPS 기본 포트)
// http://www.example.com/         -> 다른 출처 (프로토콜 다름)
// https://api.example.com/        -> 다른 출처 (호스트 다름)
// https://www.example.com:8080/   -> 다른 출처 (포트 다름)
```

#### 8.2 CORS 메커니즘

CORS: 서버가 HTTP 응답 헤더를 통해 다른 출처의 요청을 허용할 수 있게 하는 메커니즘.

```
[브라우저]                             [서버 (https://api.example.com)]
    |                                        |
    |  GET /data HTTP/1.1                    |
    |  Origin: https://www.example.com       |
    |--------------------------------------->|
    |                                        |
    |  HTTP/1.1 200 OK                       |
    |  Access-Control-Allow-Origin:          |
    |    https://www.example.com             |
    |<---------------------------------------|
    |                                        |
```

#### 8.3 Simple Request (단순 요청)

다음 조건을 모두 만족하는 요청 → Preflight 없이 바로 전송:

- 메서드: `GET`·`HEAD`·`POST` 중 하나
- 헤더: CORS-safelisted request header만 사용
  - `Accept`
  - `Accept-Language`
  - `Content-Language`
  - `Content-Type` (단, 값이 다음 중 하나: `application/x-www-form-urlencoded`·`multipart/form-data`·`text/plain`)
  - `Range` (단순 범위 헤더 값만)
- ReadableStream body 미사용
- 이벤트 리스너: `XMLHttpRequestUpload`에 이벤트 리스너 미등록

```javascript
// 단순 요청의 예
await fetch('https://api.other.com/data'); // GET, 추가 헤더 없음

await fetch('https://api.other.com/submit', {
  method: 'POST',
  headers: { 'Content-Type': 'text/plain' },
  body: 'Hello',
});
```

#### 8.4 Preflight Request (사전 요청)

단순 요청 조건을 만족하지 않는 크로스 오리진 요청 → 실제 요청 전에 OPTIONS 메서드로 Preflight 요청을 보냄.

```
[브라우저]                                   [서버]
    |                                           |
    |  OPTIONS /api/data HTTP/1.1               |
    |  Origin: https://client.com               |
    |  Access-Control-Request-Method: PUT       |
    |  Access-Control-Request-Headers:          |
    |    Content-Type, X-Custom-Header          |
    |------------------------------------------>|
    |                                           |
    |  HTTP/1.1 204 No Content                  |
    |  Access-Control-Allow-Origin:             |
    |    https://client.com                     |
    |  Access-Control-Allow-Methods:            |
    |    GET, POST, PUT, DELETE                 |
    |  Access-Control-Allow-Headers:            |
    |    Content-Type, X-Custom-Header          |
    |  Access-Control-Max-Age: 86400            |
    |<------------------------------------------|
    |                                           |
    |  PUT /api/data HTTP/1.1                   |
    |  Origin: https://client.com               |
    |  Content-Type: application/json           |
    |  X-Custom-Header: value                   |
    |  {"key": "value"}                         |
    |------------------------------------------>|
    |                                           |
    |  HTTP/1.1 200 OK                          |
    |  Access-Control-Allow-Origin:             |
    |    https://client.com                     |
    |<------------------------------------------|
```

```javascript
// Preflight가 발생하는 요청 예시
await fetch('https://api.other.com/data', {
  method: 'PUT', // PUT은 단순 요청 메서드가 아님
  headers: {
    'Content-Type': 'application/json', // json은 단순 Content-Type이 아님
    'X-Custom-Header': 'value',         // 커스텀 헤더
  },
  body: JSON.stringify({ key: 'value' }),
});
```

#### 8.5 Access-Control-* 헤더들

##### 응답 헤더 (서버 -> 클라이언트)

- `Access-Control-Allow-Origin`: 허용할 출처
  - 예시: `https://example.com` 또는 `*`
- `Access-Control-Allow-Methods`: 허용할 HTTP 메서드
  - 예시: `GET, POST, PUT, DELETE`
- `Access-Control-Allow-Headers`: 허용할 요청 헤더
  - 예시: `Content-Type, Authorization`
- `Access-Control-Expose-Headers`: JS에서 접근 가능한 응답 헤더
  - 예시: `X-Total-Count, X-Request-Id`
- `Access-Control-Max-Age`: Preflight 캐시 시간(초)
  - 예시: `86400`
- `Access-Control-Allow-Credentials`: 자격 증명 허용 여부
  - 예시: `true`

##### 요청 헤더 (클라이언트 -> 서버, Preflight에서 사용)

- `Origin`: 요청 출처
- `Access-Control-Request-Method`: 실제 요청에서 사용할 메서드
- `Access-Control-Request-Headers`: 실제 요청에서 사용할 헤더

```javascript
// 서버 측 CORS 설정 예시 (Node.js/Express)
app.use((req, res, next) => {
  res.header('Access-Control-Allow-Origin', 'https://client.example.com');
  res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.header('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  res.header('Access-Control-Allow-Credentials', 'true');
  res.header('Access-Control-Max-Age', '86400');
  res.header('Access-Control-Expose-Headers', 'X-Total-Count');

  if (req.method === 'OPTIONS') {
    return res.sendStatus(204);
  }
  next();
});
```

#### 8.6 Opaque Response (불투명 응답)

`mode: 'no-cors'`로 크로스 오리진 요청을 하면 → 서버가 CORS 헤더를 보내지 않아도 요청 자체는 성공하지만 "opaque" 응답을 받게 됨.

```javascript
const response = await fetch('https://other-domain.com/resource', {
  mode: 'no-cors',
});

console.log(response.type);       // "opaque"
console.log(response.status);     // 0 (접근 불가)
console.log(response.statusText); // "" (접근 불가)
console.log(response.url);        // "" (접근 불가)
// response.headers는 비어있음
// response.body는 null

// Opaque 응답은 캐시할 수 있지만 내용에 접근할 수 없음
// Service Worker의 Cache API에 저장하는 용도 등에 유용
```

#### 8.7 CORS-safelisted 헤더

응답 헤더 중 기본적으로 JavaScript에서 접근 가능한 헤더:

- `Cache-Control`
- `Content-Language`
- `Content-Length`
- `Content-Type`
- `Expires`
- `Last-Modified`
- `Pragma`

그 외의 헤더에 접근 → 서버가 `Access-Control-Expose-Headers`로 명시해야 함.

```javascript
// 서버가 Access-Control-Expose-Headers를 설정하지 않은 경우
const response = await fetch('https://api.other.com/data');
console.log(response.headers.get('Content-Type'));  // 접근 가능
console.log(response.headers.get('X-Custom'));      // null (접근 불가)
console.log(response.headers.get('X-Total-Count')); // null (접근 불가)

// 서버가 Access-Control-Expose-Headers: X-Total-Count 를 설정한 경우
const response2 = await fetch('https://api.other.com/data');
console.log(response2.headers.get('X-Total-Count')); // "42" (접근 가능)
```

---

### 9. Request Mode

#### 9.1 cors (기본값)

크로스 오리진 요청이 가능하며, CORS 프로토콜에 따라 동작.

```javascript
// cors 모드 (기본값)
const response = await fetch('https://api.other-domain.com/data', {
  mode: 'cors',
});
// 서버가 적절한 CORS 헤더를 응답하면 성공
// 그렇지 않으면 TypeError 발생
```

#### 9.2 no-cors

CORS 헤더 없이도 크로스 오리진 요청을 보낼 수 있으나 → 응답은 "opaque"가 되어 내용에 접근 불가. 단순 요청만 가능.

```javascript
// no-cors 모드
const response = await fetch('https://other.com/image.png', {
  mode: 'no-cors',
});
// response.type === "opaque"
// 응답 내용에 접근 불가

// 주요 용도: Service Worker에서 캐싱
self.addEventListener('fetch', (event) => {
  event.respondWith(
    caches.match(event.request).then((cached) => {
      return cached || fetch(event.request.clone());
    })
  );
});
```

#### 9.3 same-origin

같은 출처의 요청만 허용. 크로스 오리진 요청을 하면 TypeError 발생.

```javascript
// same-origin 모드
await fetch('/api/local-data', { mode: 'same-origin' }); // 성공

try {
  await fetch('https://other.com/data', { mode: 'same-origin' });
} catch (e) {
  console.error(e); // TypeError: Failed to fetch (크로스 오리진 차단)
}
```

#### 9.4 navigate

문서 내비게이션에 사용되는 모드 → 일반 JavaScript 코드에서는 설정 불가. 브라우저가 내부적으로 사용.

```javascript
// Service Worker에서 navigate 모드 감지
self.addEventListener('fetch', (event) => {
  if (event.request.mode === 'navigate') {
    // 페이지 내비게이션 요청 처리
    event.respondWith(
      fetch(event.request).catch(() => caches.match('/offline.html'))
    );
  }
});
```

---

### 10. Credentials Mode

#### 10.1 omit

어떤 경우에도 쿠키·HTTP 인증 정보·TLS 클라이언트 인증서를 요청에 포함하지 않음.

```javascript
await fetch('/api/public-data', { credentials: 'omit' });
// 같은 출처라도 쿠키를 보내지 않음
```

#### 10.2 same-origin (기본값)

같은 출처의 요청에만 자격 증명을 포함.

```javascript
await fetch('/api/profile', { credentials: 'same-origin' });
// 같은 출처 -> 쿠키 포함
// 다른 출처 -> 쿠키 미포함
```

#### 10.3 include

항상 자격 증명을 포함. 크로스 오리진 요청에도 쿠키를 보냄.

```javascript
await fetch('https://api.other-domain.com/profile', {
  credentials: 'include',
});
// 크로스 오리진이지만 쿠키 포함

// 주의: credentials: 'include'를 사용할 때:
// 1. 서버는 Access-Control-Allow-Credentials: true를 응답해야 함
// 2. 서버의 Access-Control-Allow-Origin은 "*"가 될 수 없음 (구체적 출처 명시 필요)
// 3. 서버의 Access-Control-Allow-Headers도 "*"가 될 수 없음
// 4. 서버의 Access-Control-Allow-Methods도 "*"가 될 수 없음
```

```javascript
// credentials 모드별 동작 비교 예시
const urls = ['/api/same-origin', 'https://other.com/api/cross-origin'];
const modes = ['omit', 'same-origin', 'include'];

for (const url of urls) {
  for (const mode of modes) {
    try {
      const resp = await fetch(url, { credentials: mode });
      console.log(`${url} [${mode}]: ${resp.status}`);
    } catch (e) {
      console.log(`${url} [${mode}]: ${e.message}`);
    }
  }
}
```

---

### 11. Cache Mode

#### 11.1 default

브라우저의 표준 HTTP 캐시 동작을 따름.

- 캐시에 신선한(fresh) 응답이 있으면 사용
- 만료되었으면 조건부 요청(If-None-Match, If-Modified-Since)으로 검증

```javascript
// 기본 캐시 동작
await fetch('/api/data', { cache: 'default' });
// 또는 cache 옵션 생략 시 기본값
await fetch('/api/data');
```

#### 11.2 no-store

캐시를 완전히 우회 → 캐시를 확인하지도 않고, 응답을 캐시에 저장하지도 않음.

```javascript
// 매번 새로운 요청, 캐시 저장 안 함
await fetch('/api/sensitive-data', { cache: 'no-store' });
// 민감한 데이터를 다룰 때 권장
```

#### 11.3 reload

캐시에 있는 응답을 무시하고 항상 네트워크에서 새로 가져옴. 가져온 응답은 캐시를 업데이트할 수 있음.

```javascript
// 캐시 무시, 항상 네트워크에서 가져옴
await fetch('/api/data', { cache: 'reload' });
// no-store와 비슷하지만 응답이 캐시에 저장될 수 있음
```

#### 11.4 no-cache

항상 서버에 조건부 요청을 보내 캐시의 유효성을 검증. 서버가 304 Not Modified를 응답하면 캐시된 응답을 사용.

```javascript
// 항상 서버에 검증 요청
await fetch('/api/data', { cache: 'no-cache' });
// If-None-Match 또는 If-Modified-Since 헤더 포함
```

#### 11.5 force-cache

캐시에 응답이 있으면 만료 여부와 관계없이 그 응답을 사용. 캐시에 없을 때만 네트워크 요청.

```javascript
// 캐시 우선, 만료되어도 사용
await fetch('/api/rarely-changing-data', { cache: 'force-cache' });
// 오프라인 우선(offline-first) 전략에 유용
```

#### 11.6 only-if-cached

캐시에 있을 때만 응답을 반환. 캐시에 없으면 네트워크 에러 발생. `mode: 'same-origin'`과 함께 사용해야 함.

```javascript
// 캐시에 있을 때만 사용
try {
  const response = await fetch('/api/data', {
    cache: 'only-if-cached',
    mode: 'same-origin', // 필수
  });
  console.log('캐시에서 가져옴:', await response.json());
} catch (e) {
  console.log('캐시에 없음, 네트워크 요청 필요');
}
```

```javascript
// 캐시 모드별 동작 요약 테이블
// | 모드            | 캐시 확인 | 네트워크 사용 | 캐시 저장 | 조건부 요청 |
// |----------------|----------|-------------|----------|-----------|
// | default        | O        | 필요시       | O        | 만료 시    |
// | no-store       | X        | 항상         | X        | X         |
// | reload         | X        | 항상         | O        | X         |
// | no-cache       | O        | 항상(검증)    | O        | 항상       |
// | force-cache    | O        | 캐시 없을 때  | O        | X         |
// | only-if-cached | O        | X           | X        | X         |
```

---

### 12. Redirect Mode

#### 12.1 follow (기본값)

리다이렉트를 자동으로 따라감. 최대 20회까지 리다이렉트를 추적.

```javascript
const response = await fetch('/old-url', { redirect: 'follow' });
// /old-url -> 301 -> /new-url -> 200
console.log(response.url);        // "https://example.com/new-url" (최종 URL)
console.log(response.redirected); // true
console.log(response.status);     // 200 (최종 응답의 상태)
```

#### 12.2 error

리다이렉트 발생 시 네트워크 에러로 처리.

```javascript
try {
  const response = await fetch('/old-url', { redirect: 'error' });
} catch (e) {
  console.log(e); // TypeError: Failed to fetch (리다이렉트 발생 시)
}

// 리다이렉트를 허용하지 않는 API 호출에 유용
async function fetchNoRedirect(url) {
  try {
    return await fetch(url, { redirect: 'error' });
  } catch {
    throw new Error('예상치 못한 리다이렉트가 발생했습니다.');
  }
}
```

#### 12.3 manual

리다이렉트를 자동으로 추적하지 않고, `opaqueredirect` 타입의 응답을 반환. 리다이렉트 정보에 직접 접근하려면 이 모드를 사용.

```javascript
const response = await fetch('/old-url', { redirect: 'manual' });
console.log(response.type);       // "opaqueredirect"
console.log(response.status);     // 0
console.log(response.url);        // ""
console.log(response.redirected); // false

// 참고: opaqueredirect 응답에서는 리다이렉트 대상 URL에 직접 접근할 수 없음
// Service Worker에서 리다이렉트를 가로채는 데 주로 사용됨

// Service Worker에서의 활용
self.addEventListener('fetch', (event) => {
  event.respondWith(
    fetch(event.request, { redirect: 'manual' }).then((response) => {
      if (response.type === 'opaqueredirect') {
        // 리다이렉트를 커스텀 페이지로 대체
        return Response.redirect('/custom-redirect-page', 302);
      }
      return response;
    })
  );
});
```

---

### 13. Referrer Policy

Referrer Policy: 요청 시 `Referer` 헤더에 포함되는 URL 정보의 범위를 제어.

#### 13.1 정책별 동작

- `no-referrer`
  - 동일 출처: 없음
  - 크로스 오리진 (HTTPS->HTTPS): 없음
  - 다운그레이드 (HTTPS->HTTP): 없음
- `no-referrer-when-downgrade`
  - 동일 출처: 전체 URL
  - 크로스 오리진 (HTTPS->HTTPS): 전체 URL
  - 다운그레이드 (HTTPS->HTTP): 없음
- `same-origin`
  - 동일 출처: 전체 URL
  - 크로스 오리진 (HTTPS->HTTPS): 없음
  - 다운그레이드 (HTTPS->HTTP): 없음
- `origin`
  - 동일 출처: 출처만
  - 크로스 오리진 (HTTPS->HTTPS): 출처만
  - 다운그레이드 (HTTPS->HTTP): 출처만
- `strict-origin`
  - 동일 출처: 출처만
  - 크로스 오리진 (HTTPS->HTTPS): 출처만
  - 다운그레이드 (HTTPS->HTTP): 없음
- `origin-when-cross-origin`
  - 동일 출처: 전체 URL
  - 크로스 오리진 (HTTPS->HTTPS): 출처만
  - 다운그레이드 (HTTPS->HTTP): 출처만
- `strict-origin-when-cross-origin`
  - 동일 출처: 전체 URL
  - 크로스 오리진 (HTTPS->HTTPS): 출처만
  - 다운그레이드 (HTTPS->HTTP): 없음
- `unsafe-url`
  - 동일 출처: 전체 URL
  - 크로스 오리진 (HTTPS->HTTPS): 전체 URL
  - 다운그레이드 (HTTPS->HTTP): 전체 URL

- "전체 URL" = `https://example.com/path/page?query=1`
- "출처만" = `https://example.com/`
- "없음" = Referer 헤더를 보내지 않음

#### 13.2 사용 예시

```javascript
// 요청 기준 URL: https://www.example.com/page/article?id=123

// no-referrer: Referer 헤더를 절대 보내지 않음
await fetch('https://api.other.com/data', {
  referrerPolicy: 'no-referrer',
});
// Referer: (없음)

// origin: 출처만 전송
await fetch('https://api.other.com/data', {
  referrerPolicy: 'origin',
});
// Referer: https://www.example.com/

// strict-origin-when-cross-origin (권장 기본값)
await fetch('https://api.other.com/data', {
  referrerPolicy: 'strict-origin-when-cross-origin',
});
// 크로스 오리진 HTTPS->HTTPS: Referer: https://www.example.com/

await fetch('/api/local', {
  referrerPolicy: 'strict-origin-when-cross-origin',
});
// 동일 출처: Referer: https://www.example.com/page/article?id=123

// unsafe-url: 항상 전체 URL (보안에 주의)
await fetch('http://insecure.com/api', {
  referrerPolicy: 'unsafe-url',
});
// HTTPS->HTTP 다운그레이드에서도: Referer: https://www.example.com/page/article?id=123
// 쿼리 파라미터에 민감한 정보가 있을 수 있으므로 주의
```

---

### 14. Subresource Integrity (SRI)

#### 14.1 개념

SRI: 가져온 리소스가 의도한 것과 일치하는지 브라우저가 검증할 수 있게 하는 보안 기능. CDN이 해킹되어 악성 코드가 삽입된 경우를 방어할 수 있음.

#### 14.2 integrity 속성

`integrity` 값: `해시알고리즘-base64인코딩된해시값` 형식.

```javascript
// SHA-256 해시를 사용한 무결성 검증
await fetch('https://cdn.example.com/library.js', {
  integrity: 'sha256-BpfBw7ivV8q2jLiT13fxDYAe2tJllusRSZ273h2nFSE=',
});

// SHA-384 해시
await fetch('https://cdn.example.com/library.js', {
  integrity: 'sha384-oqVuAfXRKap7fdgcCY5uykM6+R9GqQ8K/uxy9rx7HNQlGYl1kPzQho1wx4JwY8wC',
});

// SHA-512 해시
await fetch('https://cdn.example.com/library.js', {
  integrity: 'sha512-...',
});

// 여러 해시 제공 (가장 강력한 것이 사용됨)
await fetch('https://cdn.example.com/library.js', {
  integrity: 'sha256-abc123... sha384-def456...',
});
```

```javascript
// 해시 생성 예시 (Node.js)
const crypto = require('crypto');
const fs = require('fs');

function generateSRI(filePath) {
  const content = fs.readFileSync(filePath);
  const hash = crypto.createHash('sha256').update(content).digest('base64');
  return `sha256-${hash}`;
}

// 브라우저에서 해시 생성
async function generateSRIBrowser(url) {
  const response = await fetch(url);
  const buffer = await response.arrayBuffer();
  const hashBuffer = await crypto.subtle.digest('SHA-256', buffer);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  const hashBase64 = btoa(String.fromCharCode(...hashArray));
  return `sha256-${hashBase64}`;
}
```

```javascript
// integrity 검증 실패 시
try {
  await fetch('https://cdn.example.com/library.js', {
    integrity: 'sha256-WRONG_HASH_VALUE',
  });
} catch (e) {
  // TypeError: Failed to fetch
  // 해시가 일치하지 않으면 네트워크 에러로 처리됨
  console.error('무결성 검증 실패:', e);
}
```

---


### 15. Fetch 알고리즘

Fetch Standard는 리소스를 가져오는 과정을 여러 단계의 알고리즘으로 정의함 → 브라우저 내부에서 순차적으로 실행됨.

#### 15.1 Main Fetch

- 전체 fetch 과정의 진입점
- 요청(Request)을 받아 최종 응답(Response)을 반환하는 최상위 알고리즘

```
Main Fetch 알고리즘 흐름:

1. 요청(Request) 수신
2. 요청의 URL scheme 확인
   - "about:" -> about fetch
   - "blob:" -> blob fetch
   - "data:" -> data fetch
   - "file:" -> file fetch
   - "http:" / "https:" -> HTTP fetch
3. 응답 타입 결정 (basic, cors, opaque 등)
4. CSP(Content Security Policy) 검사
5. Mixed Content 검사
6. 응답(Response) 반환
```

```javascript
// 의사 코드로 표현한 Main Fetch
function mainFetch(request, recursive = false) {
  // 1. 요청 유효성 검증
  if (request.url.scheme === 'about' && request.url.path === 'blank') {
    return new Response('', { status: 200, headers: { 'Content-Type': 'text/html;charset=utf-8' } });
  }

  // 2. scheme에 따른 분기
  let response;
  if (request.url.scheme === 'http' || request.url.scheme === 'https') {
    response = httpFetch(request);
  } else if (request.url.scheme === 'data') {
    response = dataFetch(request);
  } else if (request.url.scheme === 'blob') {
    response = blobFetch(request);
  }

  // 3. 응답 필터링 (CORS, opaque 등)
  response = filterResponse(response, request.mode);

  return response;
}
```

#### 15.2 HTTP Fetch

- HTTP/HTTPS 요청을 처리하는 알고리즘

```
HTTP Fetch 알고리즘 흐름:

1. 요청이 Service Worker에 의해 가로채어질 수 있는지 확인
   - 가능하면 Service Worker에 FetchEvent 발송
   - Service Worker가 응답하면 그 응답 사용
2. Service Worker를 거치지 않는 경우 (또는 Service Worker가 fetch를 호출한 경우)
   -> HTTP-network-or-cache fetch 실행
3. CORS 모드인 경우 CORS 검증 수행
4. 리다이렉트 처리
   - redirect: "follow" -> 리다이렉트 추적 (최대 20회)
   - redirect: "error" -> 네트워크 에러
   - redirect: "manual" -> opaqueredirect 응답 반환
```

#### 15.3 HTTP-network-or-cache Fetch

- 캐시와 네트워크 사이의 상호작용을 관리하는 알고리즘

```
HTTP-network-or-cache Fetch:

1. HTTP 캐시 확인 (cache mode에 따라)
   - "only-if-cached": 캐시에 없으면 네트워크 에러
   - "force-cache": 캐시에 있으면 사용 (만료 무시)
   - "default": 캐시의 freshness 확인
   - "no-cache": 항상 검증 요청
   - "no-store": 캐시 무시
   - "reload": 캐시 무시
2. 캐시에 유효한 응답이 없으면 -> HTTP-network fetch 실행
3. 조건부 요청 헤더 추가 (If-None-Match, If-Modified-Since)
4. 네트워크 응답 수신
5. 304 응답이면 캐시된 응답 사용
6. 캐시 저장 (cache mode가 "no-store"가 아닌 경우)
```

#### 15.4 HTTP-network Fetch

- 실제 네트워크 연결을 수행하는 가장 낮은 수준의 알고리즘

```
HTTP-network Fetch:

1. 연결 설정
   - DNS 조회
   - TCP 연결
   - TLS 핸드셰이크 (HTTPS)
2. 요청 전송
   - 요청 헤더 전송
   - 요청 본문 전송 (있는 경우)
3. 응답 수신
   - 상태 줄 수신
   - 응답 헤더 수신
   - 응답 본문 수신 (스트리밍)
4. 연결 관리 (keep-alive, connection pool)
```

#### 15.5 CORS-preflight Fetch

- CORS preflight 요청을 처리하는 알고리즘

```
CORS-preflight Fetch:

1. Preflight 캐시 확인
   - 캐시에 유효한 항목이 있으면 실제 요청 허용
2. OPTIONS 요청 생성
   - Origin 헤더 설정
   - Access-Control-Request-Method 설정
   - Access-Control-Request-Headers 설정
3. OPTIONS 요청 전송
4. 응답 검증
   - Access-Control-Allow-Origin 확인
   - Access-Control-Allow-Methods 확인
   - Access-Control-Allow-Headers 확인
5. 검증 성공 시 Preflight 캐시에 저장
   - Access-Control-Max-Age 값만큼 캐시
6. 실제 요청 허용 또는 거부
```

```javascript
// Preflight 캐시 동작 시뮬레이션
// 첫 번째 요청: Preflight 발생
await fetch('https://api.other.com/data', {
  method: 'PUT',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ key: 'value' }),
});
// OPTIONS /data -> 204 (Access-Control-Max-Age: 600)
// PUT /data -> 200

// 10분 내 두 번째 요청: Preflight 캐시 사용
await fetch('https://api.other.com/data', {
  method: 'PUT',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ key: 'value2' }),
});
// PUT /data -> 200 (Preflight 생략됨)
```

---

### 16. Service Worker와의 관계

#### 16.1 FetchEvent

- Service Worker는 페이지에서 발생하는 모든 네트워크 요청을 `fetch` 이벤트로 가로챌 수 있음
- Fetch Standard와 Service Worker API의 핵심적인 연결 지점

```javascript
// service-worker.js
self.addEventListener('fetch', (event) => {
  console.log('요청 가로챔:', event.request.url);
  console.log('요청 메서드:', event.request.method);
  console.log('요청 모드:', event.request.mode);
  console.log('요청 목적:', event.request.destination);

  // 기본 동작: 원래 요청을 그대로 전달
  event.respondWith(fetch(event.request));
});
```

#### 16.2 respondWith()

- `FetchEvent.respondWith()` → 커스텀 응답으로 요청에 대응 가능

```javascript
// 캐시 우선 전략 (Cache First)
self.addEventListener('fetch', (event) => {
  event.respondWith(
    caches.match(event.request).then((cachedResponse) => {
      if (cachedResponse) {
        return cachedResponse; // 캐시에 있으면 캐시 응답 반환
      }
      return fetch(event.request).then((networkResponse) => {
        // 네트워크 응답을 캐시에 저장
        const responseClone = networkResponse.clone();
        caches.open('v1').then((cache) => {
          cache.put(event.request, responseClone);
        });
        return networkResponse;
      });
    })
  );
});
```

```javascript
// 네트워크 우선 전략 (Network First)
self.addEventListener('fetch', (event) => {
  event.respondWith(
    fetch(event.request)
      .then((networkResponse) => {
        const responseClone = networkResponse.clone();
        caches.open('v1').then((cache) => cache.put(event.request, responseClone));
        return networkResponse;
      })
      .catch(() => {
        return caches.match(event.request);
      })
  );
});
```

```javascript
// Stale-While-Revalidate 전략
self.addEventListener('fetch', (event) => {
  event.respondWith(
    caches.open('v1').then((cache) => {
      return cache.match(event.request).then((cachedResponse) => {
        const fetchPromise = fetch(event.request).then((networkResponse) => {
          cache.put(event.request, networkResponse.clone());
          return networkResponse;
        });
        // 캐시가 있으면 즉시 반환하고, 백그라운드에서 업데이트
        return cachedResponse || fetchPromise;
      });
    })
  );
});
```

```javascript
// 요청 유형별 분기 처리
self.addEventListener('fetch', (event) => {
  const { request } = event;
  const url = new URL(request.url);

  // API 요청: 네트워크 우선
  if (url.pathname.startsWith('/api/')) {
    event.respondWith(networkFirst(request));
    return;
  }

  // 이미지: 캐시 우선
  if (request.destination === 'image') {
    event.respondWith(cacheFirst(request));
    return;
  }

  // 내비게이션: 네트워크 우선 + 오프라인 폴백
  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request).catch(() => caches.match('/offline.html'))
    );
    return;
  }

  // 기타: 기본 동작
  event.respondWith(fetch(request));
});

// 모의 응답(Mock Response) 생성
self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);

  // 특정 API를 목(mock) 응답으로 대체
  if (url.pathname === '/api/mock-test') {
    event.respondWith(
      Response.json({
        id: 1,
        name: '테스트 데이터',
        timestamp: Date.now(),
      })
    );
    return;
  }
});
```

---

### 17. Streaming

#### 17.1 Response Body 스트리밍 읽기

- Fetch API는 `ReadableStream`을 통해 응답 본문을 청크(chunk) 단위로 읽음

```javascript
// 기본 스트리밍 읽기
async function streamResponse(url) {
  const response = await fetch(url);
  const reader = response.body.getReader();
  const decoder = new TextDecoder();

  let receivedLength = 0;
  const chunks = [];

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    chunks.push(value);
    receivedLength += value.length;
    console.log(`받은 데이터: ${receivedLength} bytes`);
  }

  // 모든 청크를 하나의 Uint8Array로 합치기
  const allChunks = new Uint8Array(receivedLength);
  let position = 0;
  for (const chunk of chunks) {
    allChunks.set(chunk, position);
    position += chunk.length;
  }

  return decoder.decode(allChunks);
}
```

#### 17.2 다운로드 진행률 표시

```javascript
async function downloadWithProgress(url, onProgress) {
  const response = await fetch(url);
  const contentLength = +response.headers.get('Content-Length');
  const reader = response.body.getReader();

  let receivedLength = 0;
  const chunks = [];

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    chunks.push(value);
    receivedLength += value.length;

    if (contentLength) {
      const progress = (receivedLength / contentLength) * 100;
      onProgress(progress, receivedLength, contentLength);
    }
  }

  const allChunks = new Uint8Array(receivedLength);
  let position = 0;
  for (const chunk of chunks) {
    allChunks.set(chunk, position);
    position += chunk.length;
  }

  return new Blob([allChunks]);
}

// 사용 예
downloadWithProgress('https://example.com/large-file.zip', (progress, received, total) => {
  console.log(`다운로드 진행률: ${progress.toFixed(1)}% (${received}/${total} bytes)`);
  // UI 업데이트
  progressBar.style.width = `${progress}%`;
});
```

#### 17.3 스트림 변환 (TransformStream)

```javascript
// 응답을 줄 단위로 처리
async function fetchLines(url) {
  const response = await fetch(url);

  const lineStream = response.body
    .pipeThrough(new TextDecoderStream())
    .pipeThrough(new TransformStream({
      buffer: '',
      transform(chunk, controller) {
        this.buffer += chunk;
        const lines = this.buffer.split('\n');
        this.buffer = lines.pop(); // 마지막 불완전한 줄은 버퍼에 보관
        for (const line of lines) {
          controller.enqueue(line);
        }
      },
      flush(controller) {
        if (this.buffer) {
          controller.enqueue(this.buffer);
        }
      },
    }));

  const reader = lineStream.getReader();
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    console.log('줄:', value);
  }
}
```

#### 17.4 Server-Sent Events (SSE) 스트리밍 처리

```javascript
// SSE를 fetch + ReadableStream으로 구현
async function fetchSSE(url, onEvent) {
  const response = await fetch(url);
  const reader = response.body
    .pipeThrough(new TextDecoderStream())
    .getReader();

  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += value;
    const events = buffer.split('\n\n');
    buffer = events.pop(); // 마지막 불완전한 이벤트 보관

    for (const eventStr of events) {
      const event = parseSSE(eventStr);
      if (event) onEvent(event);
    }
  }
}

function parseSSE(eventStr) {
  const lines = eventStr.split('\n');
  const event = {};
  for (const line of lines) {
    if (line.startsWith('event:')) event.type = line.slice(6).trim();
    if (line.startsWith('data:')) event.data = line.slice(5).trim();
    if (line.startsWith('id:')) event.id = line.slice(3).trim();
  }
  return event.data ? event : null;
}

// 사용 예
fetchSSE('/api/stream', (event) => {
  console.log('이벤트 타입:', event.type);
  console.log('데이터:', event.data);
});
```

#### 17.5 Request Body 스트리밍 (업로드)

```javascript
// ReadableStream을 사용한 스트리밍 업로드
const stream = new ReadableStream({
  async start(controller) {
    for (let i = 0; i < 10; i++) {
      const chunk = new TextEncoder().encode(`청크 ${i}\n`);
      controller.enqueue(chunk);
      await new Promise((resolve) => setTimeout(resolve, 100));
    }
    controller.close();
  },
});

await fetch('/api/upload', {
  method: 'POST',
  body: stream,
  duplex: 'half', // 스트리밍 업로드에 필요
  headers: { 'Content-Type': 'text/plain' },
});
```

#### 17.6 스트림 파이핑을 활용한 응답 복제 및 처리

```javascript
// 응답 스트림을 분기(tee)하여 두 곳에서 동시에 사용
async function processAndCache(url) {
  const response = await fetch(url);
  const [stream1, stream2] = response.body.tee();

  // 스트림 1: 즉시 처리
  const processPromise = new Response(stream1).json();

  // 스트림 2: 캐시에 저장
  const cachePromise = caches.open('api-cache').then((cache) => {
    return cache.put(url, new Response(stream2, {
      headers: response.headers,
    }));
  });

  const [data] = await Promise.all([processPromise, cachePromise]);
  return data;
}
```

---

### 18. AbortController를 이용한 요청 취소

#### 18.1 기본 사용법

```javascript
const controller = new AbortController();
const signal = controller.signal;

// 요청 시작
const fetchPromise = fetch('/api/data', { signal });

// 나중에 취소
controller.abort();

try {
  const response = await fetchPromise;
} catch (error) {
  if (error.name === 'AbortError') {
    console.log('요청이 사용자에 의해 취소되었습니다.');
  } else {
    console.error('다른 에러 발생:', error);
  }
}
```

#### 18.2 타임아웃 구현

```javascript
// 방법 1: setTimeout + AbortController
function fetchWithTimeout(url, options = {}, timeoutMs = 5000) {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

  return fetch(url, {
    ...options,
    signal: controller.signal,
  }).finally(() => {
    clearTimeout(timeoutId);
  });
}

// 방법 2: AbortSignal.timeout() (더 간편, 모던 브라우저)
async function fetchWithTimeout2(url, timeoutMs = 5000) {
  return fetch(url, {
    signal: AbortSignal.timeout(timeoutMs),
  });
}

// 사용 예
try {
  const response = await fetchWithTimeout('/api/slow', {}, 3000);
  const data = await response.json();
} catch (error) {
  if (error.name === 'AbortError') {
    console.log('요청 시간 초과');
  }
}
```

#### 18.3 복수 요청 동시 취소

```javascript
// 하나의 controller로 여러 요청 취소
const controller = new AbortController();

const requests = [
  fetch('/api/users', { signal: controller.signal }),
  fetch('/api/posts', { signal: controller.signal }),
  fetch('/api/comments', { signal: controller.signal }),
];

// 모든 요청 동시 취소
controller.abort();

// Promise.allSettled로 결과 확인
const results = await Promise.allSettled(requests);
results.forEach((result, index) => {
  if (result.status === 'rejected') {
    console.log(`요청 ${index} 취소됨:`, result.reason.name);
  }
});
```

#### 18.4 경쟁 요청 패턴 (Race Pattern)

```javascript
// 가장 빠른 서버의 응답 사용
async function fetchFromFastestMirror(mirrors) {
  const controller = new AbortController();

  try {
    const response = await Promise.any(
      mirrors.map((mirror) =>
        fetch(mirror, { signal: controller.signal }).then((resp) => {
          if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
          return resp;
        })
      )
    );
    // 가장 빠른 응답을 받으면 나머지 취소
    controller.abort();
    return response;
  } catch (error) {
    throw new Error('모든 미러 서버 요청 실패');
  }
}

const mirrors = [
  'https://mirror1.example.com/data',
  'https://mirror2.example.com/data',
  'https://mirror3.example.com/data',
];
const response = await fetchFromFastestMirror(mirrors);
```

#### 18.5 검색 자동완성에서의 취소 패턴

```javascript
// 이전 검색 요청을 자동으로 취소하는 패턴
class SearchController {
  constructor() {
    this.currentController = null;
  }

  async search(query) {
    // 이전 요청 취소
    if (this.currentController) {
      this.currentController.abort();
    }

    this.currentController = new AbortController();

    try {
      const response = await fetch(`/api/search?q=${encodeURIComponent(query)}`, {
        signal: this.currentController.signal,
      });
      const results = await response.json();
      return results;
    } catch (error) {
      if (error.name === 'AbortError') {
        console.log('이전 검색이 취소되었습니다 (새 검색으로 대체됨)');
        return null;
      }
      throw error;
    }
  }
}

const searcher = new SearchController();
// 사용자가 빠르게 입력할 때 이전 요청이 자동 취소됨
inputElement.addEventListener('input', async (e) => {
  const results = await searcher.search(e.target.value);
  if (results) {
    renderResults(results);
  }
});
```

#### 18.6 AbortSignal.any() 조합

```javascript
// 여러 신호를 조합: 사용자 취소 OR 타임아웃
const userController = new AbortController();
const combinedSignal = AbortSignal.any([
  userController.signal,
  AbortSignal.timeout(10000),
]);

// 취소 버튼
cancelButton.addEventListener('click', () => userController.abort());

try {
  const response = await fetch('/api/data', { signal: combinedSignal });
  const data = await response.json();
} catch (error) {
  if (error.name === 'AbortError') {
    console.log('사용자가 취소했습니다.');
  } else if (error.name === 'TimeoutError') {
    console.log('요청 시간이 초과되었습니다.');
  }
}
```

---

### 19. 에러 처리

#### 19.1 에러 유형 분류

Fetch API에서 발생 가능한 에러는 크게 세 가지로 분류:

- 네트워크 에러 (TypeError) → 네트워크 연결 실패·DNS 실패·CORS 위반 등
- 중단 에러 (AbortError / TimeoutError) → `AbortController`로 취소되거나 타임아웃 발생
- HTTP 에러 상태 코드 → 4xx·5xx 응답 (Promise가 reject되지 않음)

```javascript
// 중요한 차이점:
// fetch()는 HTTP 에러(404, 500 등)에서 reject되지 않는다!
const response = await fetch('/api/not-found');
// Promise가 성공적으로 resolve됨
console.log(response.status); // 404
console.log(response.ok);     // false (200-299 범위 밖)
```

#### 19.2 TypeError (네트워크 에러)

```javascript
try {
  await fetch('https://nonexistent-domain.example.com/api');
} catch (error) {
  console.log(error instanceof TypeError); // true
  console.log(error.name);    // "TypeError"
  console.log(error.message); // "Failed to fetch" (브라우저마다 다를 수 있음)
}

// TypeError가 발생하는 상황들:
// 1. 네트워크 연결 불가 (오프라인)
// 2. DNS 조회 실패
// 3. CORS 정책 위반
// 4. HTTPS에서 잘못된 인증서
// 5. SRI 무결성 검증 실패
// 6. Content Security Policy 위반
// 7. 잘못된 URL 형식
// 8. redirect: 'error'에서 리다이렉트 발생
```

#### 19.3 AbortError (요청 취소)

```javascript
const controller = new AbortController();
controller.abort(); // 즉시 취소

try {
  await fetch('/api/data', { signal: controller.signal });
} catch (error) {
  console.log(error instanceof DOMException); // true
  console.log(error.name);    // "AbortError"
  console.log(error.message); // "The user aborted a request."
}
```

#### 19.4 TimeoutError (AbortSignal.timeout)

```javascript
try {
  await fetch('/api/slow-endpoint', {
    signal: AbortSignal.timeout(100), // 100ms 타임아웃
  });
} catch (error) {
  console.log(error instanceof DOMException); // true
  console.log(error.name);    // "TimeoutError"
  console.log(error.message); // "The operation was aborted due to timeout"
}
```

#### 19.5 HTTP 에러 처리 패턴

```javascript
// 기본 HTTP 에러 처리
async function fetchWithHttpErrorHandling(url, options) {
  const response = await fetch(url, options);

  if (!response.ok) {
    // 응답 본문에서 에러 정보 추출 시도
    let errorMessage;
    try {
      const errorData = await response.json();
      errorMessage = errorData.message || errorData.error || JSON.stringify(errorData);
    } catch {
      errorMessage = await response.text().catch(() => response.statusText);
    }

    const error = new Error(errorMessage);
    error.status = response.status;
    error.statusText = response.statusText;
    error.response = response;
    throw error;
  }

  return response;
}

// 사용 예
try {
  const response = await fetchWithHttpErrorHandling('/api/users/999');
  const data = await response.json();
} catch (error) {
  if (error.status === 404) {
    console.log('사용자를 찾을 수 없습니다.');
  } else if (error.status === 401) {
    console.log('인증이 필요합니다. 로그인 페이지로 이동합니다.');
  } else if (error.status === 403) {
    console.log('접근 권한이 없습니다.');
  } else if (error.status >= 500) {
    console.log('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
  } else {
    console.log('요청 실패:', error.message);
  }
}
```

#### 19.6 종합적인 에러 처리 유틸리티

```javascript
class FetchError extends Error {
  constructor(message, type, status, response) {
    super(message);
    this.name = 'FetchError';
    this.type = type;     // 'network', 'abort', 'timeout', 'http', 'parse'
    this.status = status;
    this.response = response;
  }
}

async function robustFetch(url, options = {}) {
  try {
    const response = await fetch(url, options);

    if (!response.ok) {
      let body;
      try {
        body = await response.json();
      } catch {
        body = await response.text().catch(() => null);
      }

      throw new FetchError(
        `HTTP ${response.status}: ${response.statusText}`,
        'http',
        response.status,
        { headers: Object.fromEntries(response.headers), body }
      );
    }

    return response;
  } catch (error) {
    if (error instanceof FetchError) throw error;

    if (error.name === 'AbortError') {
      throw new FetchError('요청이 취소되었습니다.', 'abort', null, null);
    }

    if (error.name === 'TimeoutError') {
      throw new FetchError('요청 시간이 초과되었습니다.', 'timeout', null, null);
    }

    if (error instanceof TypeError) {
      throw new FetchError(
        '네트워크 에러가 발생했습니다.',
        'network',
        null,
        null
      );
    }

    throw error;
  }
}

// 재시도 로직 포함
async function fetchWithRetry(url, options = {}, maxRetries = 3) {
  let lastError;

  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      return await robustFetch(url, options);
    } catch (error) {
      lastError = error;

      // 취소된 요청은 재시도하지 않음
      if (error.type === 'abort') throw error;

      // 4xx 에러는 재시도 무의미 (429 Too Many Requests 제외)
      if (error.type === 'http' && error.status >= 400 && error.status < 500 && error.status !== 429) {
        throw error;
      }

      if (attempt < maxRetries) {
        // 지수 백오프 (exponential backoff)
        const delay = Math.min(1000 * Math.pow(2, attempt), 10000);
        const jitter = Math.random() * 1000;
        console.log(`재시도 ${attempt + 1}/${maxRetries} (${delay + jitter}ms 후)`);
        await new Promise((resolve) => setTimeout(resolve, delay + jitter));
      }
    }
  }

  throw lastError;
}

// 사용 예
try {
  const response = await fetchWithRetry('/api/data', {
    signal: AbortSignal.timeout(30000),
  }, 3);
  const data = await response.json();
  console.log('성공:', data);
} catch (error) {
  if (error instanceof FetchError) {
    switch (error.type) {
      case 'network':
        showNotification('인터넷 연결을 확인해주세요.');
        break;
      case 'timeout':
        showNotification('서버 응답이 느립니다. 잠시 후 다시 시도해주세요.');
        break;
      case 'abort':
        // 사용자 취소 - 별도 처리 불필요
        break;
      case 'http':
        if (error.status === 401) redirectToLogin();
        else if (error.status === 403) showNotification('접근 권한이 없습니다.');
        else if (error.status === 429) showNotification('요청이 너무 많습니다.');
        else showNotification(`서버 오류 (${error.status})`);
        break;
    }
  }
}
```

---

### 부록: 실전 패턴 모음

#### A. API 클라이언트 클래스

```javascript
class APIClient {
  constructor(baseURL, defaultHeaders = {}) {
    this.baseURL = baseURL;
    this.defaultHeaders = {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      ...defaultHeaders,
    };
    this.interceptors = { request: [], response: [] };
  }

  addRequestInterceptor(fn) {
    this.interceptors.request.push(fn);
  }

  addResponseInterceptor(fn) {
    this.interceptors.response.push(fn);
  }

  async request(path, options = {}) {
    let url = `${this.baseURL}${path}`;
    let config = {
      ...options,
      headers: { ...this.defaultHeaders, ...options.headers },
    };

    // 요청 인터셉터 적용
    for (const interceptor of this.interceptors.request) {
      config = await interceptor(config);
    }

    let response = await fetch(url, config);

    // 응답 인터셉터 적용
    for (const interceptor of this.interceptors.response) {
      response = await interceptor(response);
    }

    if (!response.ok) {
      const error = new Error(`HTTP ${response.status}`);
      error.response = response;
      throw error;
    }

    const contentType = response.headers.get('Content-Type');
    if (contentType && contentType.includes('application/json')) {
      return response.json();
    }
    return response.text();
  }

  get(path, options) {
    return this.request(path, { ...options, method: 'GET' });
  }

  post(path, data, options) {
    return this.request(path, {
      ...options,
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  put(path, data, options) {
    return this.request(path, {
      ...options,
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  patch(path, data, options) {
    return this.request(path, {
      ...options,
      method: 'PATCH',
      body: JSON.stringify(data),
    });
  }

  delete(path, options) {
    return this.request(path, { ...options, method: 'DELETE' });
  }
}

// 사용 예
const api = new APIClient('https://api.example.com');

// 인증 인터셉터 추가
api.addRequestInterceptor(async (config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers = { ...config.headers, Authorization: `Bearer ${token}` };
  }
  return config;
});

// 토큰 갱신 인터셉터 추가
api.addResponseInterceptor(async (response) => {
  if (response.status === 401) {
    const newToken = await refreshToken();
    localStorage.setItem('accessToken', newToken);
    // 원래 요청을 새 토큰으로 재시도할 수 있음
  }
  return response;
});

const users = await api.get('/users');
const newUser = await api.post('/users', { name: '홍길동', email: 'hong@example.com' });
```

#### B. 병렬 요청과 에러 격리

```javascript
// Promise.allSettled를 사용한 병렬 요청 (일부 실패 허용)
async function fetchDashboardData() {
  const [usersResult, postsResult, statsResult] = await Promise.allSettled([
    fetch('/api/users').then((r) => r.json()),
    fetch('/api/posts').then((r) => r.json()),
    fetch('/api/stats').then((r) => r.json()),
  ]);

  return {
    users: usersResult.status === 'fulfilled' ? usersResult.value : [],
    posts: postsResult.status === 'fulfilled' ? postsResult.value : [],
    stats: statsResult.status === 'fulfilled' ? statsResult.value : null,
    errors: [usersResult, postsResult, statsResult]
      .filter((r) => r.status === 'rejected')
      .map((r) => r.reason),
  };
}
```

#### C. 파일 업로드 (진행률 포함)

```javascript
async function uploadFileWithProgress(file, url, onProgress) {
  // XMLHttpRequest 사용 (fetch는 업로드 진행률을 직접 지원하지 않음)
  // 하지만 fetch + ReadableStream으로 구현 가능

  const totalSize = file.size;
  let uploadedSize = 0;

  const stream = new ReadableStream({
    async start(controller) {
      const reader = file.stream().getReader();
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        uploadedSize += value.length;
        onProgress((uploadedSize / totalSize) * 100);
        controller.enqueue(value);
      }
      controller.close();
    },
  });

  return fetch(url, {
    method: 'POST',
    body: stream,
    duplex: 'half',
    headers: {
      'Content-Type': file.type,
      'Content-Length': totalSize.toString(),
    },
  });
}
```

---

### 참고 자료

- [WHATWG Fetch Standard 공식 명세](https://fetch.spec.whatwg.org/)
- [MDN Web Docs - Fetch API](https://developer.mozilla.org/ko/docs/Web/API/Fetch_API)
- [MDN Web Docs - Using Fetch](https://developer.mozilla.org/ko/docs/Web/API/Fetch_API/Using_Fetch)
- [MDN Web Docs - AbortController](https://developer.mozilla.org/ko/docs/Web/API/AbortController)
- [MDN Web Docs - ReadableStream](https://developer.mozilla.org/ko/docs/Web/API/ReadableStream)
- [MDN Web Docs - Service Worker API](https://developer.mozilla.org/ko/docs/Web/API/Service_Worker_API)

# WHATWG Fetch Standard 완벽 가이드

## 목차

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

## 1. 개요

### 1.1 Fetch Standard란?

Fetch Standard는 WHATWG(Web Hypertext Application Technology Working Group)에서 정의한 웹 표준으로, 네트워크 요청과 응답을 처리하는 통합된 아키텍처를 제공한다. 이 표준은 단순히 `fetch()` API만을 정의하는 것이 아니라, 브라우저가 리소스를 가져오는 전체 과정 - 요청(Request)의 생성, 네트워크 전송, 응답(Response) 처리, CORS 정책 적용 등 - 을 포괄적으로 명세한다.

Fetch Standard의 핵심 목표는 다음과 같다:

- 통합된 리소스 획득 모델: HTML의 `<img>`, `<script>`, `<link>` 태그를 통한 리소스 로딩, CSS의 `@import`, JavaScript의 `fetch()` 호출 등 모든 리소스 획득 과정을 하나의 일관된 모델로 정의한다.
- 보안 모델의 명확화: Same-Origin Policy와 CORS 메커니즘을 정확히 명세하여 크로스 오리진 리소스 접근에 대한 보안 규칙을 통일한다.
- Promise 기반의 현대적 API: 콜백 기반의 레거시 API를 대체하는 깔끔하고 조합 가능한 비동기 인터페이스를 제공한다.

### 1.2 왜 필요한가?

Fetch Standard 이전에는 네트워크 요청을 위한 명확한 단일 표준이 없었다. `XMLHttpRequest`는 사실상의 표준으로 사용되었지만, 여러 한계를 가지고 있었다:

1. 이벤트 기반의 복잡한 인터페이스: 콜백 지옥(callback hell)을 야기하기 쉬웠다.
2. 스트리밍 미지원: 응답 전체가 도착해야만 데이터에 접근할 수 있었다.
3. Service Worker에서 사용 불가: XHR은 Service Worker 컨텍스트에서 동작하지 않는다.
4. 요청/응답 객체의 부재: 요청과 응답을 일급(first-class) 객체로 다룰 수 없었다.
5. 일관성 부족: 브라우저의 내부 리소스 로딩 과정과 JavaScript API 사이에 모델이 달랐다.

### 1.3 XMLHttpRequest와의 비교

| 특성 | XMLHttpRequest | fetch() |
|---|---|---|
| API 스타일 | 이벤트 기반 (콜백) | Promise 기반 |
| 요청 취소 | `abort()` 메서드 | `AbortController` / `AbortSignal` |
| 스트리밍 | 제한적 (`responseType` 의존) | `ReadableStream` 네이티브 지원 |
| 요청/응답 객체 | 없음 (단일 객체가 모든 것을 관리) | `Request`, `Response` 분리 |
| Service Worker | 사용 불가 | 네이티브 지원 |
| CORS 처리 | 암묵적 | `mode` 옵션으로 명시적 제어 |
| 쿠키 전송 | 기본적으로 전송 | `credentials` 옵션으로 명시적 제어 |
| 진행 상황 추적 | `onprogress` 이벤트 | `ReadableStream`으로 구현 가능 |
| 타임아웃 | `timeout` 속성 | `AbortSignal.timeout()` 활용 |
| 동기 요청 | 지원 (비권장) | 미지원 (비동기 전용) |

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

### 1.4 역사

- 2011-2012년: WHATWG 내에서 XHR을 대체할 새로운 API에 대한 논의가 시작되었다.
- 2014년: Fetch Standard 초안이 작성되기 시작했다. 당시 "Fetch"라는 이름은 브라우저가 리소스를 "가져오는(fetch)" 내부 동작을 표준화한다는 의미에서 채택되었다.
- 2015년: Chrome 42, Firefox 39에서 `fetch()` API가 구현되기 시작했다.
- 2015-2017년: `Request`, `Response`, `Headers` 인터페이스와 `AbortController` 지원이 점진적으로 추가되었다.
- 2017-2018년: Edge, Safari 등 주요 브라우저에서 완전한 지원이 이루어졌다.
- 현재: Fetch Standard는 Living Standard로서 지속적으로 업데이트되고 있으며, 스트리밍, `Response.json()` 정적 메서드 등 새로운 기능이 계속 추가되고 있다.

---

## 2. 기본 개념

Fetch Standard는 네 가지 핵심 인터페이스를 중심으로 설계되어 있다.

### 2.1 Request

`Request`는 리소스에 대한 요청을 나타내는 객체다. HTTP 메서드, URL, 헤더, 본문(body) 등 요청에 필요한 모든 정보를 캡슐화한다.

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

### 2.2 Response

`Response`는 요청에 대한 응답을 나타내는 객체다. 상태 코드, 헤더, 본문 등 응답 정보를 포함한다.

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

### 2.3 Headers

`Headers`는 HTTP 헤더의 이름-값 쌍 목록을 나타내는 객체다. 대소문자를 구분하지 않는 이름으로 헤더를 관리한다.

```javascript
const headers = new Headers();
headers.append('Content-Type', 'application/json');
headers.append('X-Custom-Header', 'custom-value');

console.log(headers.get('content-type')); // "application/json" (대소문자 무관)
console.log(headers.has('X-Custom-Header')); // true
```

### 2.4 Body

`Body`는 Request와 Response 모두에 포함될 수 있는 본문 데이터를 나타낸다. Body mixin은 본문 데이터를 다양한 형식(JSON, Text, Blob, ArrayBuffer, FormData)으로 읽을 수 있는 메서드를 제공한다.

```javascript
// Response body를 다양한 형식으로 읽기
const response = await fetch('https://api.example.com/data');

// JSON으로 읽기
const jsonData = await response.json();

// 텍스트로 읽기 (새 요청 필요 - body는 한 번만 읽을 수 있음)
const response2 = await fetch('https://api.example.com/data');
const textData = await response2.text();
```

중요: Body는 한 번만 소비(consume)할 수 있다. `bodyUsed` 속성으로 이미 소비되었는지 확인할 수 있으며, 여러 번 읽어야 할 경우 `clone()`을 사용해야 한다.

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

## 3. fetch() API

### 3.1 기본 사용법

`fetch()` 함수는 전역(global) 스코프에서 사용 가능한 함수로, 네트워크 요청을 수행하고 `Promise<Response>`를 반환한다.

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
- `init`: 요청 설정 옵션 (선택적)

### 3.2 옵션 (RequestInit)

`fetch()`의 두 번째 매개변수로 전달하는 옵션 객체의 전체 속성을 살펴보자.

#### 3.2.1 method

HTTP 요청 메서드를 지정한다. 기본값은 `"GET"`이다.

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

#### 3.2.2 headers

요청에 포함할 HTTP 헤더를 지정한다.

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

#### 3.2.3 body

요청 본문을 지정한다. `GET`과 `HEAD` 메서드에서는 body를 사용할 수 없다.

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

#### 3.2.4 mode

요청의 모드를 지정한다. CORS 동작을 제어한다.

```javascript
// cors (기본값) - 크로스 오리진 요청 허용, CORS 헤더 필요
await fetch('https://other-domain.com/api', { mode: 'cors' });

// no-cors - 크로스 오리진이지만 CORS 헤더 불필요 (opaque 응답)
await fetch('https://other-domain.com/image.png', { mode: 'no-cors' });

// same-origin - 같은 오리진만 허용
await fetch('/api/data', { mode: 'same-origin' });

// navigate - 내비게이션 요청용 (일반 코드에서 사용 불가)
```

#### 3.2.5 credentials

요청에 자격 증명(쿠키, HTTP 인증 등)을 포함할지 여부를 결정한다.

```javascript
// same-origin (기본값) - 같은 오리진일 때만 자격 증명 포함
await fetch('/api/profile', { credentials: 'same-origin' });

// include - 항상 자격 증명 포함 (크로스 오리진도)
await fetch('https://api.other.com/profile', { credentials: 'include' });

// omit - 자격 증명을 절대 포함하지 않음
await fetch('/api/public-data', { credentials: 'omit' });
```

#### 3.2.6 cache

HTTP 캐시와의 상호작용 방식을 지정한다.

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

#### 3.2.7 redirect

리다이렉트 처리 방식을 지정한다.

```javascript
// follow (기본값) - 리다이렉트 자동 추적
await fetch('/api/old-endpoint', { redirect: 'follow' });

// error - 리다이렉트 발생 시 에러
await fetch('/api/old-endpoint', { redirect: 'error' });

// manual - 리다이렉트를 추적하지 않고 opaqueredirect 응답 반환
await fetch('/api/old-endpoint', { redirect: 'manual' });
```

#### 3.2.8 referrer

요청의 referrer를 지정한다.

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

#### 3.2.9 referrerPolicy

Referrer 헤더에 포함되는 정보의 범위를 제어한다.

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

#### 3.2.10 integrity

Subresource Integrity(SRI) 해시를 지정하여 응답 본문의 무결성을 검증한다.

```javascript
await fetch('https://cdn.example.com/lib.js', {
  integrity: 'sha256-BpfBw7ivV8q2jLiT13fxDYAe2tJllusRSZ273h2nFSE=',
});
```

#### 3.2.11 keepalive

페이지가 종료(unload)된 후에도 요청이 완료될 수 있도록 한다. 분석 데이터 전송 등에 유용하다.

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

#### 3.2.12 signal

`AbortSignal` 객체를 전달하여 요청을 취소할 수 있게 한다.

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

### 3.3 반환값 (Promise<Response>)

`fetch()`는 항상 `Promise<Response>`를 반환한다. 중요한 점은 HTTP 에러 상태(4xx, 5xx)에서도 Promise가 reject되지 않는다는 것이다. 오직 네트워크 에러(네트워크 단절, DNS 실패 등)에서만 reject된다.

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

## 4. Request 인터페이스

### 4.1 생성

`Request` 생성자는 두 가지 형태로 호출할 수 있다.

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

### 4.2 속성

#### 읽기 전용 속성들

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

#### destination 속성

`destination`은 요청이 어떤 종류의 리소스를 위한 것인지 나타낸다. 프로그래밍 방식의 `fetch()` 호출에서는 빈 문자열이지만, 브라우저의 내부 리소스 로딩에서는 다양한 값을 가진다.

```javascript
// destination 가능한 값들:
// ""            - fetch() 직접 호출
// "audio"       - <audio> 태그
// "audioworklet" - AudioWorklet
// "document"    - <iframe>, 내비게이션
// "embed"       - <embed> 태그
// "font"        - CSS @font-face
// "frame"       - <frame> 태그
// "image"       - <img>, CSS background-image 등
// "manifest"    - <link rel="manifest">
// "object"      - <object> 태그
// "paintworklet" - CSS Paint API
// "report"      - CSP report, NEL report
// "script"      - <script> 태그
// "sharedworker" - SharedWorker
// "style"       - <link rel="stylesheet">, CSS @import
// "track"       - <track> 태그
// "video"       - <video> 태그
// "worker"      - Worker
// "xslt"        - XSLT 스타일시트
```

#### isReloadNavigation, isHistoryNavigation

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

### 4.3 clone()

`Request` 객체를 복제한다. body가 이미 소비된 Request는 복제할 수 없다.

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

## 5. Response 인터페이스

### 5.1 생성

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

### 5.2 정적 메서드

#### Response.error()

네트워크 에러를 나타내는 Response를 생성한다. `type`이 `"error"`인 Response를 반환한다.

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

#### Response.redirect()

리다이렉트 응답을 생성한다. status는 301, 302, 303, 307, 308 중 하나여야 한다.

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

#### Response.json()

JSON 데이터로부터 Response를 생성하는 편의 메서드다. `Content-Type`이 자동으로 `application/json`으로 설정된다.

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

### 5.3 속성

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

### 5.4 clone()

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

## 6. Headers 인터페이스

### 6.1 생성

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

### 6.2 메서드

#### append(name, value)

기존 헤더에 값을 추가한다. 같은 이름의 헤더가 이미 있으면 값이 결합된다.

```javascript
const headers = new Headers();
headers.append('Accept', 'text/html');
headers.append('Accept', 'application/json');
console.log(headers.get('Accept')); // "text/html, application/json"

headers.append('X-Custom', 'value1');
headers.append('X-Custom', 'value2');
console.log(headers.get('X-Custom')); // "value1, value2"
```

#### set(name, value)

헤더의 값을 설정한다. 이미 존재하면 대체하고, 없으면 새로 추가한다.

```javascript
const headers = new Headers();
headers.set('Content-Type', 'text/plain');
console.log(headers.get('Content-Type')); // "text/plain"

headers.set('Content-Type', 'application/json');
console.log(headers.get('Content-Type')); // "application/json" (대체됨)
```

#### get(name)

헤더의 값을 반환한다. 없으면 `null`을 반환한다.

```javascript
const headers = new Headers({ 'Content-Type': 'application/json' });
console.log(headers.get('Content-Type'));    // "application/json"
console.log(headers.get('content-type'));    // "application/json" (대소문자 무관)
console.log(headers.get('X-Nonexistent'));   // null
```

#### has(name)

해당 이름의 헤더가 존재하는지 확인한다.

```javascript
const headers = new Headers({ 'Content-Type': 'application/json' });
console.log(headers.has('Content-Type'));  // true
console.log(headers.has('Authorization')); // false
```

#### delete(name)

해당 이름의 헤더를 삭제한다.

```javascript
const headers = new Headers({
  'Content-Type': 'application/json',
  'Authorization': 'Bearer token',
});
headers.delete('Authorization');
console.log(headers.has('Authorization')); // false
```

#### forEach(callback)

모든 헤더를 순회한다.

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

#### entries(), keys(), values()

이터레이터를 반환한다.

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

### 6.3 Headers Guard

Headers 객체에는 "guard"라는 내부 속성이 있어 특정 헤더의 변경을 제한한다. 이는 API를 통해 직접 접근할 수 없지만, 동작 방식을 이해하는 것이 중요하다.

| Guard | 설명 | 적용 대상 |
|---|---|---|
| `"none"` | 제한 없음 | `new Headers()` |
| `"request"` | forbidden header name 수정 불가 | `Request` 객체의 headers |
| `"request-no-cors"` | CORS-safelisted 헤더만 허용 | `no-cors` 모드 Request의 headers |
| `"response"` | forbidden response header name 수정 불가 | `Response` 객체의 headers |
| `"immutable"` | 모든 수정 불가 | `error()`, `redirect()` 등의 응답 headers |

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

Forbidden Header Names (브라우저가 자동 관리하는 헤더):

`Accept-Charset`, `Accept-Encoding`, `Access-Control-Request-Headers`, `Access-Control-Request-Method`, `Connection`, `Content-Length`, `Cookie`, `Cookie2`, `Date`, `DNT`, `Expect`, `Host`, `Keep-Alive`, `Origin`, `Referer`, `Set-Cookie`, `TE`, `Trailer`, `Transfer-Encoding`, `Upgrade`, `Via`, `Proxy-*`, `Sec-*`

---

## 7. Body Mixin

Body mixin은 `Request`와 `Response` 모두에 구현되어 있으며, 본문 데이터를 다양한 형식으로 읽을 수 있는 메서드를 제공한다.

### 7.1 text()

본문을 UTF-8 문자열로 읽는다.

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

### 7.2 json()

본문을 JSON으로 파싱한다.

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

### 7.3 blob()

본문을 Blob 객체로 읽는다.

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

### 7.4 arrayBuffer()

본문을 ArrayBuffer로 읽는다.

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

### 7.5 formData()

본문을 FormData 객체로 파싱한다. `multipart/form-data` 또는 `application/x-www-form-urlencoded` 형식이어야 한다.

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

### 7.6 body (ReadableStream)

`body` 속성은 본문의 `ReadableStream`에 직접 접근할 수 있게 한다.

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

### 7.7 bodyUsed

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

## 8. CORS (Cross-Origin Resource Sharing)

### 8.1 Same-Origin Policy (동일 출처 정책)

동일 출처 정책은 웹 보안의 핵심 메커니즘이다. "출처(origin)"는 프로토콜(scheme), 호스트(host), 포트(port)의 조합으로 정의된다.

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

### 8.2 CORS 메커니즘

CORS는 서버가 HTTP 응답 헤더를 통해 다른 출처의 요청을 허용할 수 있게 하는 메커니즘이다.

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

### 8.3 Simple Request (단순 요청)

다음 조건을 모두 만족하는 요청은 Preflight 없이 바로 전송된다:

1. 메서드: `GET`, `HEAD`, `POST` 중 하나
2. 헤더: CORS-safelisted request header만 사용
   - `Accept`
   - `Accept-Language`
   - `Content-Language`
   - `Content-Type` (단, 값이 다음 중 하나: `application/x-www-form-urlencoded`, `multipart/form-data`, `text/plain`)
   - `Range` (단순 범위 헤더 값만)
3. ReadableStream body 미사용
4. 이벤트 리스너: `XMLHttpRequestUpload`에 이벤트 리스너 미등록

```javascript
// 단순 요청의 예
await fetch('https://api.other.com/data'); // GET, 추가 헤더 없음

await fetch('https://api.other.com/submit', {
  method: 'POST',
  headers: { 'Content-Type': 'text/plain' },
  body: 'Hello',
});
```

### 8.4 Preflight Request (사전 요청)

단순 요청 조건을 만족하지 않는 크로스 오리진 요청은 실제 요청 전에 OPTIONS 메서드로 Preflight 요청을 보낸다.

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

### 8.5 Access-Control-* 헤더들

#### 응답 헤더 (서버 -> 클라이언트)

| 헤더 | 설명 | 예시 |
|---|---|---|
| `Access-Control-Allow-Origin` | 허용할 출처 | `https://example.com` 또는 `*` |
| `Access-Control-Allow-Methods` | 허용할 HTTP 메서드 | `GET, POST, PUT, DELETE` |
| `Access-Control-Allow-Headers` | 허용할 요청 헤더 | `Content-Type, Authorization` |
| `Access-Control-Expose-Headers` | JS에서 접근 가능한 응답 헤더 | `X-Total-Count, X-Request-Id` |
| `Access-Control-Max-Age` | Preflight 캐시 시간(초) | `86400` |
| `Access-Control-Allow-Credentials` | 자격 증명 허용 여부 | `true` |

#### 요청 헤더 (클라이언트 -> 서버, Preflight에서 사용)

| 헤더 | 설명 |
|---|---|
| `Origin` | 요청 출처 |
| `Access-Control-Request-Method` | 실제 요청에서 사용할 메서드 |
| `Access-Control-Request-Headers` | 실제 요청에서 사용할 헤더 |

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

### 8.6 Opaque Response (불투명 응답)

`mode: 'no-cors'`로 크로스 오리진 요청을 하면, 서버가 CORS 헤더를 보내지 않아도 요청 자체는 성공하지만 "opaque" 응답을 받게 된다.

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

### 8.7 CORS-safelisted 헤더

응답 헤더 중 기본적으로 JavaScript에서 접근 가능한 헤더는 다음과 같다:

- `Cache-Control`
- `Content-Language`
- `Content-Length`
- `Content-Type`
- `Expires`
- `Last-Modified`
- `Pragma`

그 외의 헤더에 접근하려면 서버가 `Access-Control-Expose-Headers`로 명시해야 한다.

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

## 9. Request Mode

### 9.1 cors (기본값)

크로스 오리진 요청이 가능하며, CORS 프로토콜에 따라 동작한다.

```javascript
// cors 모드 (기본값)
const response = await fetch('https://api.other-domain.com/data', {
  mode: 'cors',
});
// 서버가 적절한 CORS 헤더를 응답하면 성공
// 그렇지 않으면 TypeError 발생
```

### 9.2 no-cors

CORS 헤더 없이도 크로스 오리진 요청을 보낼 수 있지만, 응답은 "opaque"가 되어 내용에 접근할 수 없다. 단순 요청만 가능하다.

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

### 9.3 same-origin

같은 출처의 요청만 허용한다. 크로스 오리진 요청을 하면 TypeError가 발생한다.

```javascript
// same-origin 모드
await fetch('/api/local-data', { mode: 'same-origin' }); // 성공

try {
  await fetch('https://other.com/data', { mode: 'same-origin' });
} catch (e) {
  console.error(e); // TypeError: Failed to fetch (크로스 오리진 차단)
}
```

### 9.4 navigate

문서 내비게이션에 사용되는 모드로, 일반 JavaScript 코드에서는 설정할 수 없다. 브라우저가 내부적으로 사용한다.

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

## 10. Credentials Mode

### 10.1 omit

어떤 경우에도 쿠키, HTTP 인증 정보, TLS 클라이언트 인증서를 요청에 포함하지 않는다.

```javascript
await fetch('/api/public-data', { credentials: 'omit' });
// 같은 출처라도 쿠키를 보내지 않음
```

### 10.2 same-origin (기본값)

같은 출처의 요청에만 자격 증명을 포함한다.

```javascript
await fetch('/api/profile', { credentials: 'same-origin' });
// 같은 출처 -> 쿠키 포함
// 다른 출처 -> 쿠키 미포함
```

### 10.3 include

항상 자격 증명을 포함한다. 크로스 오리진 요청에도 쿠키를 보낸다.

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

## 11. Cache Mode

### 11.1 default

브라우저의 표준 HTTP 캐시 동작을 따른다. 캐시에 신선한(fresh) 응답이 있으면 사용하고, 만료되었으면 조건부 요청(If-None-Match, If-Modified-Since)으로 검증한다.

```javascript
// 기본 캐시 동작
await fetch('/api/data', { cache: 'default' });
// 또는 cache 옵션 생략 시 기본값
await fetch('/api/data');
```

### 11.2 no-store

캐시를 완전히 우회한다. 캐시를 확인하지도 않고, 응답을 캐시에 저장하지도 않는다.

```javascript
// 매번 새로운 요청, 캐시 저장 안 함
await fetch('/api/sensitive-data', { cache: 'no-store' });
// 민감한 데이터를 다룰 때 권장
```

### 11.3 reload

캐시에 있는 응답을 무시하고 항상 네트워크에서 새로 가져온다. 가져온 응답은 캐시를 업데이트할 수 있다.

```javascript
// 캐시 무시, 항상 네트워크에서 가져옴
await fetch('/api/data', { cache: 'reload' });
// no-store와 비슷하지만 응답이 캐시에 저장될 수 있음
```

### 11.4 no-cache

항상 서버에 조건부 요청을 보내 캐시의 유효성을 검증한다. 서버가 304 Not Modified를 응답하면 캐시된 응답을 사용한다.

```javascript
// 항상 서버에 검증 요청
await fetch('/api/data', { cache: 'no-cache' });
// If-None-Match 또는 If-Modified-Since 헤더 포함
```

### 11.5 force-cache

캐시에 응답이 있으면 만료 여부와 관계없이 그 응답을 사용한다. 캐시에 없을 때만 네트워크 요청을 한다.

```javascript
// 캐시 우선, 만료되어도 사용
await fetch('/api/rarely-changing-data', { cache: 'force-cache' });
// 오프라인 우선(offline-first) 전략에 유용
```

### 11.6 only-if-cached

캐시에 있을 때만 응답을 반환한다. 캐시에 없으면 네트워크 에러가 발생한다. `mode: 'same-origin'`과 함께 사용해야 한다.

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

## 12. Redirect Mode

### 12.1 follow (기본값)

리다이렉트를 자동으로 따라간다. 최대 20회까지 리다이렉트를 추적한다.

```javascript
const response = await fetch('/old-url', { redirect: 'follow' });
// /old-url -> 301 -> /new-url -> 200
console.log(response.url);        // "https://example.com/new-url" (최종 URL)
console.log(response.redirected); // true
console.log(response.status);     // 200 (최종 응답의 상태)
```

### 12.2 error

리다이렉트 발생 시 네트워크 에러로 처리한다.

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

### 12.3 manual

리다이렉트를 자동으로 추적하지 않고, `opaqueredirect` 타입의 응답을 반환한다. 리다이렉트 정보에 직접 접근하려면 이 모드를 사용한다.

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

## 13. Referrer Policy

Referrer Policy는 요청 시 `Referer` 헤더에 포함되는 URL 정보의 범위를 제어한다.

### 13.1 정책별 동작

| 정책 | 동일 출처 | 크로스 오리진 (HTTPS->HTTPS) | 다운그레이드 (HTTPS->HTTP) |
|---|---|---|---|
| `no-referrer` | 없음 | 없음 | 없음 |
| `no-referrer-when-downgrade` | 전체 URL | 전체 URL | 없음 |
| `same-origin` | 전체 URL | 없음 | 없음 |
| `origin` | 출처만 | 출처만 | 출처만 |
| `strict-origin` | 출처만 | 출처만 | 없음 |
| `origin-when-cross-origin` | 전체 URL | 출처만 | 출처만 |
| `strict-origin-when-cross-origin` | 전체 URL | 출처만 | 없음 |
| `unsafe-url` | 전체 URL | 전체 URL | 전체 URL |

> "전체 URL" = `https://example.com/path/page?query=1`
> "출처만" = `https://example.com/`
> "없음" = Referer 헤더를 보내지 않음

### 13.2 사용 예시

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

## 14. Subresource Integrity (SRI)

### 14.1 개념

SRI는 가져온 리소스가 의도한 것과 일치하는지 브라우저가 검증할 수 있게 하는 보안 기능이다. CDN이 해킹되어 악성 코드가 삽입된 경우를 방어할 수 있다.

### 14.2 integrity 속성

`integrity` 값은 `해시알고리즘-base64인코딩된해시값` 형식이다.

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

## 15. Fetch 알고리즘

Fetch Standard는 리소스를 가져오는 과정을 여러 단계의 알고리즘으로 정의한다. 이 알고리즘들은 브라우저 내부에서 순차적으로 실행된다.

### 15.1 Main Fetch

전체 fetch 과정의 진입점이다. 요청(Request)을 받아서 최종 응답(Response)을 반환하는 최상위 알고리즘이다.

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

### 15.2 HTTP Fetch

HTTP/HTTPS 요청을 처리하는 알고리즘이다.

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

### 15.3 HTTP-network-or-cache Fetch

캐시와 네트워크 사이의 상호작용을 관리하는 알고리즘이다.

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

### 15.4 HTTP-network Fetch

실제 네트워크 연결을 수행하는 가장 낮은 수준의 알고리즘이다.

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

### 15.5 CORS-preflight Fetch

CORS preflight 요청을 처리하는 알고리즘이다.

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

## 16. Service Worker와의 관계

### 16.1 FetchEvent

Service Worker는 페이지에서 발생하는 모든 네트워크 요청을 `fetch` 이벤트로 가로챌 수 있다. 이는 Fetch Standard와 Service Worker API의 핵심적인 연결 지점이다.

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

### 16.2 respondWith()

`FetchEvent.respondWith()`는 커스텀 응답으로 요청에 대응할 수 있게 한다.

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

## 17. Streaming

### 17.1 Response Body 스트리밍 읽기

Fetch API는 `ReadableStream`을 통해 응답 본문을 청크(chunk) 단위로 읽을 수 있다.

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

### 17.2 다운로드 진행률 표시

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

### 17.3 스트림 변환 (TransformStream)

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

### 17.4 Server-Sent Events (SSE) 스트리밍 처리

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

### 17.5 Request Body 스트리밍 (업로드)

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

### 17.6 스트림 파이핑을 활용한 응답 복제 및 처리

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

## 18. AbortController를 이용한 요청 취소

### 18.1 기본 사용법

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

### 18.2 타임아웃 구현

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

### 18.3 복수 요청 동시 취소

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

### 18.4 경쟁 요청 패턴 (Race Pattern)

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

### 18.5 검색 자동완성에서의 취소 패턴

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

### 18.6 AbortSignal.any() 조합

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

## 19. 에러 처리

### 19.1 에러 유형 분류

Fetch API에서 발생할 수 있는 에러는 크게 세 가지로 분류된다:

1. 네트워크 에러 (TypeError): 네트워크 연결 실패, DNS 실패, CORS 위반 등
2. 중단 에러 (AbortError / TimeoutError): `AbortController`로 취소되었거나 타임아웃
3. HTTP 에러 상태 코드: 4xx, 5xx 응답 (Promise가 reject되지 않음!)

```javascript
// 중요한 차이점:
// fetch()는 HTTP 에러(404, 500 등)에서 reject되지 않는다!
const response = await fetch('/api/not-found');
// Promise가 성공적으로 resolve됨
console.log(response.status); // 404
console.log(response.ok);     // false (200-299 범위 밖)
```

### 19.2 TypeError (네트워크 에러)

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

### 19.3 AbortError (요청 취소)

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

### 19.4 TimeoutError (AbortSignal.timeout)

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

### 19.5 HTTP 에러 처리 패턴

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

### 19.6 종합적인 에러 처리 유틸리티

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

## 부록: 실전 패턴 모음

### A. API 클라이언트 클래스

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

### B. 병렬 요청과 에러 격리

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

### C. 파일 업로드 (진행률 포함)

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

## 참고 자료

- [WHATWG Fetch Standard 공식 명세](https://fetch.spec.whatwg.org/)
- [MDN Web Docs - Fetch API](https://developer.mozilla.org/ko/docs/Web/API/Fetch_API)
- [MDN Web Docs - Using Fetch](https://developer.mozilla.org/ko/docs/Web/API/Fetch_API/Using_Fetch)
- [MDN Web Docs - AbortController](https://developer.mozilla.org/ko/docs/Web/API/AbortController)
- [MDN Web Docs - ReadableStream](https://developer.mozilla.org/ko/docs/Web/API/ReadableStream)
- [MDN Web Docs - Service Worker API](https://developer.mozilla.org/ko/docs/Web/API/Service_Worker_API)
- [MDN Web Docs - CORS](https://developer.mozilla.org/ko/docs/Web/HTTP/CORS)

# WHATWG XMLHttpRequest Standard 완벽 가이드

## 목차

1. [개요](#1-개요)
2. [XMLHttpRequest 인터페이스](#2-xmlhttprequest-인터페이스)
3. [요청 메서드](#3-요청-메서드)
4. [응답 처리](#4-응답-처리)
5. [이벤트](#5-이벤트)
6. [XMLHttpRequestUpload](#6-xmlhttprequestupload)
7. [ProgressEvent](#7-progressevent)
8. [FormData 인터페이스](#8-formdata-인터페이스)
9. [CORS와 XHR](#9-cors와-xhr)
10. [동기 vs 비동기](#10-동기-vs-비동기)
11. [보안 제한](#11-보안-제한)
12. [실용 예제](#12-실용-예제)

---

## 1. 개요

### 1.1 XMLHttpRequest Standard란?

XMLHttpRequest Standard는 WHATWG(Web Hypertext Application Technology Working Group)에서 관리하는 웹 표준으로, 클라이언트 측 스크립트에서 HTTP(S) 요청을 프로그래밍 방식으로 전송하고 응답을 처리하기 위한 API를 정의한다. 이름에 "XML"이 포함되어 있지만 XML에 국한되지 않으며, JSON, HTML, 텍스트, 바이너리 등 다양한 형식의 데이터를 주고받을 수 있다.

이 표준은 다음의 핵심 인터페이스를 정의한다:

- XMLHttpRequest: HTTP 요청과 응답을 처리하는 주 인터페이스
- XMLHttpRequestUpload: 업로드 진행 상황 모니터링 인터페이스
- ProgressEvent: 전송 진행률 정보를 담는 이벤트 인터페이스
- FormData: 폼 데이터를 키-값 쌍으로 관리하는 인터페이스

> 표준 문서: https://xhr.spec.whatwg.org/

### 1.2 역사

XMLHttpRequest의 역사는 웹 개발 패러다임 전환의 핵심 축이다.

| 시기 | 사건 |
|------|------|
| 1998~1999 | Microsoft가 Outlook Web Access를 위해 `XMLHTTP` ActiveX 객체 개발 |
| 2000 | IE 5.0에서 `new ActiveXObject("Microsoft.XMLHTTP")`로 사용 가능 |
| 2002 | Mozilla가 `XMLHttpRequest`라는 이름으로 네이티브 객체 구현 |
| 2004 | Gmail 출시 — XHR을 활용한 비동기 인터페이스로 웹 애플리케이션의 가능성 입증 |
| 2005 | Jesse James Garrett이 "AJAX" 용어를 처음 사용 (Asynchronous JavaScript and XML) |
| 2005 | Google Maps 출시 — XHR 기반 동적 지도 로딩으로 AJAX 대중화 |
| 2006 | W3C가 XMLHttpRequest Level 1 초안 발행 |
| 2008 | XMLHttpRequest Level 2 초안 — CORS, 진행률 이벤트, 바이너리 데이터 지원 추가 |
| 2012 | Level 1과 Level 2가 단일 XMLHttpRequest 표준으로 통합 |
| 2014~ | WHATWG가 Living Standard로 관리 |

```javascript
// 2000년대 초반: IE에서의 XHR 생성 (ActiveX)
var xhr;
if (window.ActiveXObject) {
  xhr = new ActiveXObject("Microsoft.XMLHTTP");   // IE 5~6
} else if (window.XMLHttpRequest) {
  xhr = new XMLHttpRequest();                      // 모던 브라우저
}

// 현재: 표준화된 방식
const xhr = new XMLHttpRequest();
```

### 1.3 AJAX의 탄생

AJAX(Asynchronous JavaScript and XML)는 특정 기술이 아니라 여러 기술의 조합을 지칭하는 용어이다. 2005년 Jesse James Garrett이 발표한 글에서 처음 명명되었다.

AJAX의 핵심 개념은 전체 페이지를 새로고침하지 않고도 서버와 데이터를 교환하여 페이지 일부만 동적으로 갱신하는 것이다.

AJAX 이전과 이후의 웹 상호작용 모델:

```
[전통적 웹 모델]
사용자 액션 → 서버 요청 → 전체 페이지 다시 로드 → 화면 깜빡임

[AJAX 모델]
사용자 액션 → XHR로 서버 요청 (백그라운드) → JSON/XML 응답 수신 → DOM 일부만 업데이트
```

```javascript
// AJAX의 전형적인 패턴
const xhr = new XMLHttpRequest();
xhr.open('GET', '/api/search?q=ajax');
xhr.onreadystatechange = function () {
  if (xhr.readyState === 4 && xhr.status === 200) {
    const data = JSON.parse(xhr.responseText);
    document.getElementById('results').innerHTML = renderResults(data);
  }
};
xhr.send();
```

### 1.4 Fetch API와의 비교

XMLHttpRequest와 Fetch API는 동일한 목적(HTTP 통신)을 수행하지만 설계 철학이 근본적으로 다르다.

| 특성 | XMLHttpRequest | Fetch API |
|------|---------------|-----------|
| API 패러다임 | 이벤트 기반 (콜백) | Promise 기반 |
| 요청/응답 객체 | 단일 객체가 모든 것을 관리 | `Request`, `Response` 분리 |
| 스트리밍 | 제한적 (`responseType` 의존) | `ReadableStream` 네이티브 지원 |
| 업로드 진행률 | `xhr.upload.onprogress` 네이티브 지원 | 직접 구현 필요 (ReadableStream) |
| 다운로드 진행률 | `xhr.onprogress` 네이티브 지원 | `Response.body` ReadableStream으로 구현 |
| 요청 취소 | `xhr.abort()` | `AbortController` / `AbortSignal` |
| 타임아웃 | `xhr.timeout` 속성 | `AbortSignal.timeout()` |
| 동기 요청 | 지원 (비권장) | 미지원 (비동기 전용) |
| 쿠키 전송 | 동일 출처 시 기본 전송 | `credentials` 옵션으로 명시적 제어 |
| HTTP 에러 | `status`로 직접 확인 | Promise가 reject되지 않음 (`ok` 속성 확인) |
| Service Worker | 사용 불가 | 네이티브 지원 |
| 캐시 제어 | 헤더로 수동 제어 | `cache` 옵션 제공 |
| 리다이렉트 제어 | 자동 추적 (제어 불가) | `redirect` 옵션 제공 |
| 표준 관리 | WHATWG XMLHttpRequest Standard | WHATWG Fetch Standard |

```javascript
// 동일한 작업의 두 가지 접근 방식

// XMLHttpRequest
function getJSON_XHR(url) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('GET', url);
    xhr.responseType = 'json';
    xhr.timeout = 5000;
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve(xhr.response);
      } else {
        reject(new Error(`HTTP ${xhr.status}: ${xhr.statusText}`));
      }
    };
    xhr.onerror = () => reject(new Error('Network error'));
    xhr.ontimeout = () => reject(new Error('Request timed out'));
    xhr.send();
  });
}

// Fetch API
async function getJSON_Fetch(url) {
  const response = await fetch(url, {
    signal: AbortSignal.timeout(5000),
  });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
  }
  return response.json();
}
```

---

## 2. XMLHttpRequest 인터페이스

### 2.1 생성자

`XMLHttpRequest` 객체는 `new` 키워드로 생성한다. 생성자는 인자를 받지 않는다.

```javascript
const xhr = new XMLHttpRequest();
```

생성 직후 객체의 초기 상태:

| 속성 | 초기값 |
|------|--------|
| `readyState` | `0` (UNSENT) |
| `status` | `0` |
| `statusText` | `""` |
| `responseType` | `""` |
| `response` | `""` |
| `responseText` | `""` |
| `responseURL` | `""` |
| `timeout` | `0` |
| `withCredentials` | `false` |

### 2.2 readyState

`readyState` 속성은 XHR 객체의 현재 상태를 나타내는 읽기 전용 정수값이다. 요청의 생명주기 전체를 5단계로 표현한다.

| 상수 | 값 | 설명 |
|------|----|------|
| `XMLHttpRequest.UNSENT` | `0` | 객체가 생성되었지만 `open()`이 호출되지 않은 상태 |
| `XMLHttpRequest.OPENED` | `1` | `open()`이 호출된 상태. `setRequestHeader()`와 `send()` 호출 가능 |
| `XMLHttpRequest.HEADERS_RECEIVED` | `2` | `send()`가 호출되었고, 응답 헤더가 수신된 상태 |
| `XMLHttpRequest.LOADING` | `3` | 응답 본문(body)을 수신 중인 상태 |
| `XMLHttpRequest.DONE` | `4` | 요청이 완료된 상태 (성공 또는 실패) |

```
상태 전이 흐름:

  UNSENT(0) ──open()──→ OPENED(1) ──send()──→ HEADERS_RECEIVED(2) ──→ LOADING(3) ──→ DONE(4)
     ↑                                                                                   │
     └───────────────── open() 재호출로 초기화 ←─────────────────────────────────────────┘
```

```javascript
const xhr = new XMLHttpRequest();
console.log(xhr.readyState); // 0 (UNSENT)

xhr.open('GET', '/api/data');
console.log(xhr.readyState); // 1 (OPENED)

xhr.onreadystatechange = function () {
  switch (xhr.readyState) {
    case XMLHttpRequest.HEADERS_RECEIVED: // 2
      console.log('응답 헤더 수신:', xhr.status);
      console.log('Content-Type:', xhr.getResponseHeader('Content-Type'));
      break;
    case XMLHttpRequest.LOADING: // 3
      console.log('응답 본문 수신 중...');
      // responseType이 ""이거나 "text"일 때만 responseText 접근 가능
      if (xhr.responseType === '' || xhr.responseType === 'text') {
        console.log('지금까지 받은 데이터 길이:', xhr.responseText.length);
      }
      break;
    case XMLHttpRequest.DONE: // 4
      console.log('요청 완료. 상태 코드:', xhr.status);
      break;
  }
};

xhr.send();
```

### 2.3 주요 속성

#### status / statusText

HTTP 응답 상태 코드와 상태 메시지를 나타낸다. `readyState`가 `UNSENT` 또는 `OPENED`일 때 접근하면 각각 `0`과 `""`을 반환한다.

```javascript
xhr.onload = function () {
  console.log(xhr.status);      // 200
  console.log(xhr.statusText);  // "OK"
};
```

> 참고: HTTP/2에서는 상태 텍스트가 정의되지 않으므로 `statusText`는 빈 문자열일 수 있다.

#### responseType

응답 데이터의 타입을 지정한다. `open()` 호출 후, `send()` 호출 전에 설정해야 한다.

| 값 | `response` 타입 | 설명 |
|----|-----------------|------|
| `""` (기본값) | `string` | 텍스트로 반환 (`responseText`와 동일) |
| `"text"` | `string` | 텍스트로 반환 |
| `"json"` | `object` / `null` | JSON 파싱 결과 반환 |
| `"document"` | `Document` / `null` | HTML/XML 문서로 파싱 |
| `"arraybuffer"` | `ArrayBuffer` / `null` | 바이너리 데이터 |
| `"blob"` | `Blob` / `null` | Blob 객체 |

```javascript
// JSON 응답 자동 파싱
const xhr = new XMLHttpRequest();
xhr.open('GET', '/api/users');
xhr.responseType = 'json';
xhr.onload = function () {
  // xhr.response는 이미 파싱된 JavaScript 객체
  const users = xhr.response;
  console.log(users[0].name);
};
xhr.send();
```

#### response / responseText / responseXML

- `response`: `responseType`에 따라 적절한 타입의 데이터를 반환하는 범용 속성
- `responseText`: 응답을 텍스트 문자열로 반환. `responseType`이 `""` 또는 `"text"`일 때만 접근 가능하며, 그 외의 경우 `InvalidStateError`가 발생한다
- `responseXML`: 응답을 `Document`로 파싱하여 반환. `responseType`이 `""` 또는 `"document"`일 때만 접근 가능

```javascript
// responseText 사용
const xhr1 = new XMLHttpRequest();
xhr1.open('GET', '/api/data');
xhr1.onload = function () {
  const text = xhr1.responseText;  // 문자열
  const data = JSON.parse(text);    // 수동 파싱
};
xhr1.send();

// responseXML 사용
const xhr2 = new XMLHttpRequest();
xhr2.open('GET', '/feed.xml');
xhr2.onload = function () {
  const xmlDoc = xhr2.responseXML;
  const items = xmlDoc.querySelectorAll('item');
  items.forEach(item => {
    console.log(item.querySelector('title').textContent);
  });
};
xhr2.send();
```

#### responseURL

최종 응답의 URL을 반환한다. 리다이렉트가 발생한 경우 리다이렉트 후의 최종 URL이 된다. URL 프래그먼트(#)는 제거된다.

```javascript
const xhr = new XMLHttpRequest();
xhr.open('GET', '/old-endpoint');  // 301 리다이렉트 → /new-endpoint
xhr.onload = function () {
  console.log(xhr.responseURL);    // "https://example.com/new-endpoint"
};
xhr.send();
```

#### timeout

요청 타임아웃을 밀리초 단위로 설정한다. 기본값은 `0`(무제한)이다. 설정된 시간 내에 응답이 완료되지 않으면 요청이 자동으로 종료되고 `timeout` 이벤트가 발생한다.

```javascript
const xhr = new XMLHttpRequest();
xhr.open('GET', '/api/slow-endpoint');
xhr.timeout = 10000; // 10초
xhr.ontimeout = function () {
  console.error('요청 시간 초과');
};
xhr.send();
```

> 주의: 동기 요청에서 `timeout`을 설정하면 `InvalidAccessError`가 발생한다.

#### withCredentials

크로스 오리진 요청 시 쿠키, HTTP 인증 정보, TLS 클라이언트 인증서 등의 자격 증명(credentials)을 포함할지 여부를 지정한다. 기본값은 `false`이다.

```javascript
const xhr = new XMLHttpRequest();
xhr.open('GET', 'https://api.other-domain.com/data');
xhr.withCredentials = true;  // 크로스 오리진 쿠키 포함
xhr.onload = function () {
  console.log(xhr.response);
};
xhr.send();
```

#### upload

`XMLHttpRequestUpload` 객체를 반환한다. 이 객체에 이벤트 핸들러를 등록하여 업로드 진행 상황을 모니터링할 수 있다. 자세한 내용은 [6장](#6-xmlhttprequestupload)을 참고한다.

---

## 3. 요청 메서드

### 3.1 open(method, url [, async [, username [, password]]])

요청을 초기화한다. 이미 활성화된 요청이 있으면 `abort()`를 호출한 것과 동일하게 동작한다.

| 매개변수 | 타입 | 설명 |
|----------|------|------|
| `method` | `string` | HTTP 메서드 (`GET`, `POST`, `PUT`, `DELETE`, `PATCH` 등) |
| `url` | `string` | 요청 URL (상대 또는 절대 경로) |
| `async` | `boolean` | 비동기 여부 (기본값: `true`) |
| `username` | `string` | HTTP 인증 사용자명 (선택) |
| `password` | `string` | HTTP 인증 비밀번호 (선택) |

```javascript
const xhr = new XMLHttpRequest();

// 기본 사용
xhr.open('GET', '/api/users');

// 절대 URL
xhr.open('POST', 'https://api.example.com/data');

// 동기 요청 (비권장)
xhr.open('GET', '/api/data', false);

// HTTP 인증 정보 포함
xhr.open('GET', '/protected/resource', true, 'admin', 'secret');
```

`open()` 호출 후 변경되는 상태:
- `readyState` → `OPENED` (1)
- `status` → `0`으로 초기화
- `statusText` → `""`로 초기화
- 기존에 설정된 요청 헤더가 모두 제거됨

> 금지된 메서드: `CONNECT`, `TRACE`, `TRACK`은 보안상의 이유로 사용할 수 없으며, `SecurityError`가 발생한다.

### 3.2 setRequestHeader(name, value)

요청 헤더를 설정한다. 반드시 `open()` 호출 후, `send()` 호출 전에 사용해야 한다. 동일한 헤더를 여러 번 호출하면 값이 쉼표로 구분되어 합쳐진다.

```javascript
const xhr = new XMLHttpRequest();
xhr.open('POST', '/api/data');

// Content-Type 설정
xhr.setRequestHeader('Content-Type', 'application/json');

// 커스텀 헤더
xhr.setRequestHeader('X-Custom-Header', 'my-value');

// 동일 헤더 중복 호출 — 값이 합쳐짐
xhr.setRequestHeader('Accept', 'application/json');
xhr.setRequestHeader('Accept', 'text/plain');
// 실제 전송: Accept: application/json, text/plain

xhr.send(JSON.stringify({ key: 'value' }));
```

금지된 요청 헤더(forbidden request headers)는 설정할 수 없으며, 시도해도 조용히 무시된다. 자세한 목록은 [11장](#11-보안-제한)을 참고한다.

### 3.3 send(body)

요청을 전송한다. `body` 매개변수는 요청 본문이며, 다양한 타입을 지원한다.

| body 타입 | 자동 Content-Type | 설명 |
|-----------|-------------------|------|
| `null` / 생략 | 없음 | GET, HEAD 요청 시 사용 |
| `string` | `text/plain;charset=UTF-8` | 텍스트 데이터 |
| `FormData` | `multipart/form-data` (boundary 포함) | 폼 데이터, 파일 업로드 |
| `Blob` | Blob의 `type` 속성 | 바이너리 데이터 |
| `ArrayBuffer` / `ArrayBufferView` | 없음 | 로우 바이너리 데이터 |
| `URLSearchParams` | `application/x-www-form-urlencoded;charset=UTF-8` | URL 인코딩된 데이터 |
| `Document` | HTML: `text/html`, XML: `application/xml` | HTML/XML 문서 |

```javascript
// GET 요청 — body 없이 전송
const xhr1 = new XMLHttpRequest();
xhr1.open('GET', '/api/users');
xhr1.send(); // 또는 xhr1.send(null);

// POST — JSON 문자열
const xhr2 = new XMLHttpRequest();
xhr2.open('POST', '/api/users');
xhr2.setRequestHeader('Content-Type', 'application/json');
xhr2.send(JSON.stringify({ name: '홍길동', age: 30 }));

// POST — FormData (파일 업로드)
const formData = new FormData();
formData.append('username', 'hong');
formData.append('avatar', fileInput.files[0]);

const xhr3 = new XMLHttpRequest();
xhr3.open('POST', '/api/upload');
// Content-Type은 자동 설정 (multipart/form-data + boundary)
xhr3.send(formData);

// POST — URLSearchParams
const params = new URLSearchParams();
params.append('q', '검색어');
params.append('page', '1');

const xhr4 = new XMLHttpRequest();
xhr4.open('POST', '/api/search');
xhr4.send(params);

// POST — Blob
const blob = new Blob(['Hello, World!'], { type: 'text/plain' });
const xhr5 = new XMLHttpRequest();
xhr5.open('POST', '/api/upload-text');
xhr5.send(blob);

// POST — ArrayBuffer
const buffer = new ArrayBuffer(16);
const view = new Uint8Array(buffer);
view.set([0x48, 0x65, 0x6C, 0x6C, 0x6F]); // "Hello"

const xhr6 = new XMLHttpRequest();
xhr6.open('POST', '/api/binary');
xhr6.send(buffer);
```

> `send()`는 비동기 요청일 때 즉시 반환되며, 동기 요청일 때는 응답이 완료될 때까지 블로킹한다.

### 3.4 abort()

이미 전송된 요청을 중단한다. 호출 시 다음이 발생한다:

1. `readyState`가 `UNSENT`로 초기화
2. `status`가 `0`으로 초기화
3. `abort` 이벤트 발생
4. 업로드가 완료되지 않았으면 `upload` 객체에서도 `abort` 이벤트 발생

```javascript
const xhr = new XMLHttpRequest();
xhr.open('GET', '/api/large-data');
xhr.onprogress = function (event) {
  if (event.loaded > 1024 * 1024) {
    // 1MB 이상 수신되면 중단
    xhr.abort();
  }
};
xhr.onabort = function () {
  console.log('요청이 사용자에 의해 중단됨');
};
xhr.send();

// 외부에서 중단
setTimeout(() => xhr.abort(), 5000); // 5초 후 강제 중단
```

### 3.5 overrideMimeType(mime)

서버가 반환한 MIME 타입을 무시하고 지정된 MIME 타입으로 응답을 해석하도록 강제한다. `send()` 호출 전에 사용해야 한다.

```javascript
// 서버가 text/plain으로 응답하지만 실제로는 XML인 경우
const xhr = new XMLHttpRequest();
xhr.open('GET', '/data/legacy-feed');
xhr.overrideMimeType('application/xml');
xhr.onload = function () {
  const xmlDoc = xhr.responseXML;  // 정상적으로 XML로 파싱
  console.log(xmlDoc.documentElement.nodeName);
};
xhr.send();

// 바이너리 데이터를 원시 문자열로 수신
const xhr2 = new XMLHttpRequest();
xhr2.open('GET', '/binary-file');
xhr2.overrideMimeType('text/plain; charset=x-user-defined');
xhr2.onload = function () {
  const rawData = xhr2.responseText;
  for (let i = 0; i < rawData.length; i++) {
    const byte = rawData.charCodeAt(i) & 0xff;
    // 각 바이트 처리
  }
};
xhr2.send();
```

---

## 4. 응답 처리

### 4.1 getResponseHeader(name)

지정된 이름의 응답 헤더 값을 반환한다. 이름은 대소문자를 구분하지 않는다. 해당 헤더가 없으면 `null`을 반환한다. 동일 이름의 헤더가 여러 개 있으면 쉼표로 구분된 단일 문자열로 반환한다.

```javascript
xhr.onload = function () {
  const contentType = xhr.getResponseHeader('Content-Type');
  // "application/json; charset=utf-8"

  const cacheControl = xhr.getResponseHeader('Cache-Control');
  // "no-cache, no-store"

  const custom = xhr.getResponseHeader('X-Nonexistent');
  // null
};
```

> 참고: CORS 환경에서는 기본적으로 "단순 응답 헤더(CORS-safelisted response headers)"만 접근 가능하다: `Cache-Control`, `Content-Language`, `Content-Length`, `Content-Type`, `Expires`, `Last-Modified`, `Pragma`. 그 외 헤더는 서버가 `Access-Control-Expose-Headers`로 명시적 허용해야 한다.

### 4.2 getAllResponseHeaders()

모든 응답 헤더를 하나의 문자열로 반환한다. 각 헤더는 `\r\n`(CRLF)으로 구분되며, 헤더 이름과 값은 `: `(콜론+공백)으로 구분된다.

```javascript
xhr.onload = function () {
  const headersStr = xhr.getAllResponseHeaders();
  // "content-type: application/json\r\ncache-control: no-cache\r\n..."

  // 헤더 문자열을 객체로 변환하는 유틸리티
  function parseHeaders(headerStr) {
    const headers = {};
    if (!headerStr) return headers;

    headerStr.trim().split('\r\n').forEach(line => {
      const colonIndex = line.indexOf(': ');
      if (colonIndex > 0) {
        const name = line.substring(0, colonIndex).toLowerCase();
        const value = line.substring(colonIndex + 2);
        headers[name] = value;
      }
    });
    return headers;
  }

  const headers = parseHeaders(headersStr);
  console.log(headers['content-type']); // "application/json"
};
```

### 4.3 responseType 상세

`responseType`을 설정하면 브라우저가 응답 데이터를 자동으로 해당 형식으로 변환한다.

```javascript
// "json" — 자동 JSON 파싱
const xhr1 = new XMLHttpRequest();
xhr1.open('GET', '/api/users');
xhr1.responseType = 'json';
xhr1.onload = function () {
  const users = xhr1.response; // 이미 파싱된 객체
  // JSON 파싱 실패 시 null
};
xhr1.send();

// "arraybuffer" — 바이너리 데이터 처리
const xhr2 = new XMLHttpRequest();
xhr2.open('GET', '/api/audio.mp3');
xhr2.responseType = 'arraybuffer';
xhr2.onload = function () {
  const audioData = xhr2.response; // ArrayBuffer
  const audioContext = new AudioContext();
  audioContext.decodeAudioData(audioData, buffer => {
    const source = audioContext.createBufferSource();
    source.buffer = buffer;
    source.connect(audioContext.destination);
    source.start(0);
  });
};
xhr2.send();

// "blob" — 파일 다운로드
const xhr3 = new XMLHttpRequest();
xhr3.open('GET', '/api/files/report.pdf');
xhr3.responseType = 'blob';
xhr3.onload = function () {
  const blob = xhr3.response; // Blob 객체
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'report.pdf';
  a.click();
  URL.revokeObjectURL(url);
};
xhr3.send();

// "document" — HTML/XML 문서
const xhr4 = new XMLHttpRequest();
xhr4.open('GET', '/page.html');
xhr4.responseType = 'document';
xhr4.onload = function () {
  const doc = xhr4.response; // Document 객체
  const title = doc.querySelector('title').textContent;
  const links = doc.querySelectorAll('a');
};
xhr4.send();
```

---

## 5. 이벤트

### 5.1 이벤트 목록

XMLHttpRequest와 XMLHttpRequestUpload는 `XMLHttpRequestEventTarget`을 상속하며, 다음 이벤트를 지원한다.

| 이벤트 | 발생 시점 | 이벤트 객체 |
|--------|-----------|-------------|
| `readystatechange` | `readyState`가 변경될 때 | `Event` |
| `loadstart` | 요청 전송이 시작될 때 | `ProgressEvent` |
| `progress` | 데이터 수신/전송 중 (주기적) | `ProgressEvent` |
| `abort` | `abort()` 호출로 요청이 취소될 때 | `ProgressEvent` |
| `error` | 네트워크 오류 발생 시 | `ProgressEvent` |
| `load` | 요청이 성공적으로 완료될 때 | `ProgressEvent` |
| `timeout` | 설정된 시간 내에 응답이 오지 않을 때 | `ProgressEvent` |
| `loadend` | 요청이 완료될 때 (성공/실패/취소 불문) | `ProgressEvent` |

### 5.2 이벤트 발생 순서

표준에서 정의하는 이벤트 발생 순서는 다음과 같다.

성공적인 요청 시:
```
1. loadstart
2. progress (0회 이상)
3. load
4. loadend
```

오류 발생 시:
```
1. loadstart
2. progress (0회 이상)
3. error
4. loadend
```

타임아웃 시:
```
1. loadstart
2. progress (0회 이상)
3. timeout
4. loadend
```

요청 취소 시:
```
1. loadstart
2. progress (0회 이상)
3. abort
4. loadend
```

> `readystatechange`는 별도로, `readyState`가 변경될 때마다 발생한다. `load`, `error`, `timeout`, `abort` 중 하나만 발생하며, `loadend`는 항상 마지막에 발생한다.

```javascript
const xhr = new XMLHttpRequest();

xhr.onreadystatechange = function () {
  console.log(`readyState 변경: ${xhr.readyState}`);
};

xhr.onloadstart = function (e) {
  console.log('1. loadstart — 요청 시작');
};

xhr.onprogress = function (e) {
  if (e.lengthComputable) {
    const percent = ((e.loaded / e.total) * 100).toFixed(1);
    console.log(`2. progress — ${percent}% (${e.loaded}/${e.total})`);
  } else {
    console.log(`2. progress — ${e.loaded} bytes 수신`);
  }
};

xhr.onload = function (e) {
  console.log(`3. load — 완료 (status: ${xhr.status})`);
};

xhr.onerror = function (e) {
  console.log('3. error — 네트워크 오류');
};

xhr.ontimeout = function (e) {
  console.log('3. timeout — 시간 초과');
};

xhr.onabort = function (e) {
  console.log('3. abort — 요청 취소');
};

xhr.onloadend = function (e) {
  console.log('4. loadend — 요청 종료 (항상 마지막)');
};

xhr.open('GET', '/api/large-data');
xhr.send();
```

### 5.3 이벤트 핸들러 등록 방식

이벤트 핸들러는 두 가지 방식으로 등록할 수 있다.

```javascript
// 방식 1: on* 속성 (하나의 핸들러만 등록 가능)
xhr.onload = function (event) {
  console.log('완료');
};

// 방식 2: addEventListener (여러 핸들러 등록 가능)
xhr.addEventListener('load', function (event) {
  console.log('핸들러 1');
});
xhr.addEventListener('load', function (event) {
  console.log('핸들러 2');
});
```

---

## 6. XMLHttpRequestUpload

### 6.1 개요

`XMLHttpRequestUpload`은 업로드 과정을 모니터링하기 위한 인터페이스이다. `XMLHttpRequest` 객체의 `upload` 속성을 통해 접근하며, `XMLHttpRequestEventTarget`을 상속하므로 동일한 이벤트 세트(`loadstart`, `progress`, `load`, `error`, `abort`, `timeout`, `loadend`)를 지원한다.

### 6.2 업로드 진행률 모니터링

```javascript
const xhr = new XMLHttpRequest();
const file = document.getElementById('fileInput').files[0];

// 업로드 이벤트
xhr.upload.onloadstart = function (e) {
  console.log('업로드 시작');
};

xhr.upload.onprogress = function (e) {
  if (e.lengthComputable) {
    const percent = ((e.loaded / e.total) * 100).toFixed(1);
    console.log(`업로드 진행: ${percent}%`);
    document.getElementById('progress-bar').style.width = percent + '%';
    document.getElementById('progress-text').textContent = percent + '%';
  }
};

xhr.upload.onload = function (e) {
  console.log('업로드 완료');
};

xhr.upload.onerror = function (e) {
  console.log('업로드 오류');
};

// 응답 이벤트 (서버의 처리 결과)
xhr.onload = function () {
  if (xhr.status === 200) {
    console.log('서버 응답:', xhr.response);
  }
};

const formData = new FormData();
formData.append('file', file);

xhr.open('POST', '/api/upload');
xhr.send(formData);
```

### 6.3 upload 이벤트와 CORS

중요한 보안 사항: 크로스 오리진 요청에서 `xhr.upload`에 이벤트 리스너를 등록하면 해당 요청이 "단순 요청(simple request)"에서 제외되어 반드시 preflight 요청이 발생한다. 이는 업로드 진행률을 통해 서버의 존재 여부나 네트워크 특성을 추론하는 공격을 방지하기 위한 것이다.

```javascript
// 크로스 오리진 요청에서의 upload 이벤트 주의
const xhr = new XMLHttpRequest();
xhr.open('POST', 'https://api.other-domain.com/upload');

// 이 리스너 등록만으로도 preflight가 발생
xhr.upload.onprogress = function (e) {
  console.log('진행률:', (e.loaded / e.total * 100).toFixed(1) + '%');
};

xhr.send(formData);
```

---

## 7. ProgressEvent

### 7.1 인터페이스 정의

`ProgressEvent`는 작업의 진행 상태를 나타내는 이벤트이다. `Event`를 상속하며 세 가지 추가 속성을 갖는다.

| 속성 | 타입 | 설명 |
|------|------|------|
| `lengthComputable` | `boolean` | 전체 크기를 알 수 있는지 여부 |
| `loaded` | `number` | 현재까지 전송된 바이트 수 |
| `total` | `number` | 전체 바이트 수 (`lengthComputable`이 `false`이면 `0`) |

```javascript
xhr.onprogress = function (event) {
  console.log('lengthComputable:', event.lengthComputable);
  console.log('loaded:', event.loaded);
  console.log('total:', event.total);

  if (event.lengthComputable) {
    const percent = (event.loaded / event.total) * 100;
    updateProgressBar(percent);
  }
};
```

### 7.2 lengthComputable이 false인 경우

서버가 `Content-Length` 헤더를 제공하지 않거나 Transfer-Encoding이 chunked인 경우 `lengthComputable`이 `false`가 된다. 이 경우 `total`은 `0`이다.

```javascript
xhr.onprogress = function (event) {
  if (event.lengthComputable) {
    // 전체 크기를 아는 경우: 퍼센트 표시
    const percent = ((event.loaded / event.total) * 100).toFixed(1);
    progressBar.style.width = percent + '%';
    progressText.textContent = `${percent}% (${formatBytes(event.loaded)} / ${formatBytes(event.total)})`;
  } else {
    // 전체 크기를 모르는 경우: 수신량만 표시
    progressText.textContent = `${formatBytes(event.loaded)} 수신`;
    // 스피너 또는 불확정 진행 표시줄 사용
  }
};

function formatBytes(bytes) {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}
```

### 7.3 ProgressEvent 생성

커스텀 ProgressEvent를 생성할 수도 있다 (테스트 등의 목적).

```javascript
const event = new ProgressEvent('progress', {
  lengthComputable: true,
  loaded: 500,
  total: 1000,
});

console.log(event.type);             // "progress"
console.log(event.lengthComputable); // true
console.log(event.loaded);          // 500
console.log(event.total);           // 1000
```

---

## 8. FormData 인터페이스

### 8.1 개요

`FormData` 인터페이스는 HTML 폼 데이터를 키-값 쌍으로 쉽게 관리할 수 있게 해주는 API이다. `XMLHttpRequest.send()`에 직접 전달할 수 있으며, 이 경우 `multipart/form-data` 인코딩으로 자동 전송된다.

### 8.2 생성자

```javascript
// 빈 FormData 생성
const fd1 = new FormData();

// 기존 HTML 폼에서 생성
const form = document.getElementById('myForm');
const fd2 = new FormData(form);
// 폼의 모든 필드(name 속성이 있는)가 자동으로 추가됨
```

### 8.3 메서드

```javascript
const fd = new FormData();

// append(name, value [, filename])
// 키-값 쌍을 추가한다. 같은 키로 여러 번 호출 가능.
fd.append('username', 'hong');
fd.append('hobby', '독서');
fd.append('hobby', '코딩');  // 같은 키로 중복 추가 가능
fd.append('avatar', fileInput.files[0], 'profile.jpg'); // 파일 + 파일명

// set(name, value [, filename])
// 키에 해당하는 기존 값을 모두 제거하고 새 값을 설정한다.
fd.set('username', 'kim');  // 기존 'hong'을 'kim'으로 교체
fd.set('hobby', '등산');     // 기존 '독서', '코딩'을 모두 제거하고 '등산'만 설정

// get(name) — 해당 키의 첫 번째 값 반환
console.log(fd.get('username')); // "kim"

// getAll(name) — 해당 키의 모든 값을 배열로 반환
console.log(fd.getAll('hobby')); // ["등산"]

// has(name) — 해당 키가 존재하는지 확인
console.log(fd.has('username')); // true
console.log(fd.has('email'));    // false

// delete(name) — 해당 키의 모든 값을 제거
fd.delete('hobby');
console.log(fd.has('hobby'));    // false
```

### 8.4 이터레이터 메서드

`FormData`는 이터러블(iterable)이며, `entries()`, `keys()`, `values()` 메서드를 제공한다.

```javascript
const fd = new FormData();
fd.append('name', '홍길동');
fd.append('age', '30');
fd.append('hobby', '독서');
fd.append('hobby', '여행');

// entries() — [key, value] 쌍을 순회
for (const [key, value] of fd.entries()) {
  console.log(`${key}: ${value}`);
}
// name: 홍길동
// age: 30
// hobby: 독서
// hobby: 여행

// keys() — 키만 순회
for (const key of fd.keys()) {
  console.log(key);
}
// name, age, hobby, hobby

// values() — 값만 순회
for (const value of fd.values()) {
  console.log(value);
}

// for...of 직접 사용 (entries()와 동일)
for (const [key, value] of fd) {
  console.log(`${key} = ${value}`);
}

// 스프레드 연산자 활용
const entries = [...fd];
console.log(entries);
// [["name", "홍길동"], ["age", "30"], ["hobby", "독서"], ["hobby", "여행"]]
```

### 8.5 FormData와 XHR 전송

```javascript
// 파일 업로드 예제
const form = document.getElementById('uploadForm');
form.addEventListener('submit', function (e) {
  e.preventDefault();

  const fd = new FormData(this);
  // 프로그래밍 방식으로 추가 데이터 첨부
  fd.append('timestamp', Date.now().toString());

  const xhr = new XMLHttpRequest();
  xhr.open('POST', '/api/upload');

  xhr.upload.onprogress = function (e) {
    if (e.lengthComputable) {
      const percent = (e.loaded / e.total * 100).toFixed(1);
      console.log(`업로드: ${percent}%`);
    }
  };

  xhr.onload = function () {
    if (xhr.status === 200) {
      console.log('업로드 성공:', xhr.response);
    }
  };

  // Content-Type 헤더를 수동 설정하지 않는다!
  // FormData 전송 시 브라우저가 multipart/form-data + boundary를 자동 설정
  xhr.send(fd);
});
```

---

## 9. CORS와 XHR

### 9.1 동일 출처와 교차 출처 요청

XMLHttpRequest는 기본적으로 동일 출처 정책(Same-Origin Policy)의 제약을 받는다. 동일 출처란 프로토콜, 호스트, 포트가 모두 같은 것을 의미한다.

```
페이지 출처: https://example.com:443

https://example.com/api/data       → 동일 출처 (경로만 다름)
https://example.com:443/api/data   → 동일 출처 (포트 명시, 동일)
http://example.com/api/data        → 교차 출처 (프로토콜 다름)
https://api.example.com/data       → 교차 출처 (호스트 다름)
https://example.com:8080/data      → 교차 출처 (포트 다름)
```

### 9.2 CORS 동작 방식

교차 출처 요청이 허용되려면 서버가 적절한 CORS 헤더를 응답에 포함해야 한다.

단순 요청(Simple Request): 다음 조건을 모두 만족하면 preflight 없이 직접 전송된다.

- 메서드: `GET`, `HEAD`, `POST` 중 하나
- 헤더: `Accept`, `Accept-Language`, `Content-Language`, `Content-Type` (값 제한 있음)만 사용
- `Content-Type`: `application/x-www-form-urlencoded`, `multipart/form-data`, `text/plain` 중 하나
- `xhr.upload`에 이벤트 리스너가 등록되지 않음
- `ReadableStream`이 body로 사용되지 않음

```javascript
// 단순 요청 예시
const xhr = new XMLHttpRequest();
xhr.open('GET', 'https://api.other-domain.com/data');
xhr.onload = function () {
  // 서버 응답에 Access-Control-Allow-Origin이 포함되어야 함
  console.log(xhr.response);
};
xhr.send();
```

Preflight 요청: 단순 요청 조건을 충족하지 않으면 브라우저가 자동으로 `OPTIONS` 메서드의 preflight 요청을 먼저 전송한다.

```javascript
// Preflight가 발생하는 요청
const xhr = new XMLHttpRequest();
xhr.open('PUT', 'https://api.other-domain.com/data/1');
xhr.setRequestHeader('Content-Type', 'application/json');  // 단순 요청 Content-Type이 아님
xhr.setRequestHeader('X-Custom-Header', 'value');          // 커스텀 헤더
xhr.onload = function () {
  console.log(xhr.response);
};
xhr.send(JSON.stringify({ name: 'updated' }));
```

Preflight 요청/응답:

```
[Preflight 요청 — 브라우저가 자동 전송]
OPTIONS /data/1 HTTP/1.1
Host: api.other-domain.com
Origin: https://example.com
Access-Control-Request-Method: PUT
Access-Control-Request-Headers: Content-Type, X-Custom-Header

[Preflight 응답 — 서버]
HTTP/1.1 204 No Content
Access-Control-Allow-Origin: https://example.com
Access-Control-Allow-Methods: GET, POST, PUT, DELETE
Access-Control-Allow-Headers: Content-Type, X-Custom-Header
Access-Control-Max-Age: 86400
```

### 9.3 withCredentials

교차 출처 요청에서 쿠키나 인증 정보를 포함하려면 `withCredentials`를 `true`로 설정해야 하며, 서버는 `Access-Control-Allow-Credentials: true`를 응답해야 한다. 이 경우 `Access-Control-Allow-Origin`에 와일드카드(`*`)를 사용할 수 없다.

```javascript
const xhr = new XMLHttpRequest();
xhr.open('GET', 'https://api.other-domain.com/user/profile');
xhr.withCredentials = true;
xhr.onload = function () {
  console.log(xhr.response);
};
xhr.send();

// 서버 응답에 필요한 헤더:
// Access-Control-Allow-Origin: https://example.com   (와일드카드 * 불가)
// Access-Control-Allow-Credentials: true
```

---

## 10. 동기 vs 비동기

### 10.1 비동기 요청 (기본)

`open()`의 세 번째 인자 `async`가 `true`(기본값)이면 비동기 요청이다. `send()` 호출 즉시 반환되며, 응답은 이벤트 핸들러를 통해 처리한다.

```javascript
const xhr = new XMLHttpRequest();
xhr.open('GET', '/api/data', true); // 또는 xhr.open('GET', '/api/data')
xhr.onload = function () {
  console.log('비동기 응답:', xhr.response);
};
xhr.send();
console.log('send() 직후 — 응답 대기 중'); // 이 줄이 먼저 실행됨
```

### 10.2 동기 요청

`async`를 `false`로 설정하면 동기 요청이다. `send()`가 응답이 올 때까지 블로킹한다.

```javascript
const xhr = new XMLHttpRequest();
xhr.open('GET', '/api/data', false); // 동기 요청
xhr.send();
// send()가 응답을 받을 때까지 여기서 블로킹
console.log('동기 응답:', xhr.responseText); // 응답 즉시 사용 가능
```

### 10.3 동기 XHR의 문제점

동기 XHR은 여러 심각한 문제를 유발하며, 메인 스레드(document 환경)에서의 사용은 강력히 비권장(deprecated)되고 있다.

1. UI 프리징: 메인 스레드가 네트워크 응답을 기다리는 동안 모든 사용자 상호작용(클릭, 스크롤, 입력)이 차단된다
2. 이벤트 처리 중단: 타이머, 애니메이션, 다른 비동기 작업이 모두 대기 상태가 된다
3. 사용자 경험 저하: 브라우저가 "응답 없음" 상태로 보이며, 탭이 멈춘 것처럼 느껴진다

```javascript
// 동기 XHR의 제한 사항
const xhr = new XMLHttpRequest();

// 메인 스레드에서 동기 요청 시 timeout 설정 불가
xhr.open('GET', '/api/data', false);
xhr.timeout = 5000; // InvalidAccessError 발생!

// 메인 스레드에서 동기 요청 시 responseType 설정 불가 (일부 브라우저)
xhr.responseType = 'json'; // InvalidAccessError 발생!
```

브라우저 경고 및 제한:

| 환경 | 동기 XHR 동작 |
|------|---------------|
| 메인 스레드 (window) | 동작하지만 콘솔 경고 발생, 일부 기능 제한 |
| Worker 내부 | 제한 없이 동작 (Worker는 별도 스레드) |
| 페이지 unload 중 (`beforeunload`, `unload`) | 브라우저에 따라 무시될 수 있음 |
| `document` 환경에서 `async=false` | 표준에서 deprecated, 향후 제거 가능 |

```javascript
// Worker에서의 동기 XHR (허용되는 유일한 적절한 사용처)
// worker.js
self.addEventListener('message', function (e) {
  const xhr = new XMLHttpRequest();
  xhr.open('GET', e.data.url, false); // Worker에서는 동기 요청 가능
  xhr.send();
  self.postMessage({
    status: xhr.status,
    data: xhr.responseText,
  });
});
```

---

## 11. 보안 제한

### 11.1 Same-Origin Policy

XMLHttpRequest는 기본적으로 동일 출처 정책의 적용을 받는다. 교차 출처 요청은 CORS 메커니즘을 통해서만 허용된다.

### 11.2 금지 메서드 (Forbidden Methods)

다음 HTTP 메서드는 `open()`에서 사용할 수 없으며, `SecurityError`가 발생한다.

| 메서드 | 금지 이유 |
|--------|-----------|
| `CONNECT` | 프록시 터널링에 사용되며, 악용 시 내부 네트워크 접근 가능 |
| `TRACE` | 요청 내용을 그대로 반환하므로 쿠키/인증 정보 탈취 위험(XST 공격) |
| `TRACK` | `TRACE`의 Microsoft 구현체, 동일한 보안 위험 |

```javascript
const xhr = new XMLHttpRequest();
try {
  xhr.open('TRACE', '/');
} catch (e) {
  console.log(e.name);    // "SecurityError"
  console.log(e.message); // "Failed to execute 'open' on 'XMLHttpRequest'..."
}
```

### 11.3 금지 요청 헤더 (Forbidden Request Headers)

다음 헤더는 `setRequestHeader()`로 설정할 수 없다. 시도하면 조용히 무시된다(에러 없음).

- `Accept-Charset`
- `Accept-Encoding`
- `Access-Control-Request-Headers`
- `Access-Control-Request-Method`
- `Connection`
- `Content-Length`
- `Cookie`
- `Cookie2`
- `Date`
- `DNT`
- `Expect`
- `Host`
- `Keep-Alive`
- `Origin`
- `Referer`
- `Set-Cookie`
- `TE`
- `Trailer`
- `Transfer-Encoding`
- `Upgrade`
- `Via`
- `Proxy-` 접두사로 시작하는 모든 헤더
- `Sec-` 접두사로 시작하는 모든 헤더

```javascript
const xhr = new XMLHttpRequest();
xhr.open('GET', '/api/data');

// 다음 헤더들은 설정해도 무시됨
xhr.setRequestHeader('Cookie', 'sessionId=abc');      // 무시됨
xhr.setRequestHeader('Host', 'evil.com');              // 무시됨
xhr.setRequestHeader('Origin', 'https://evil.com');    // 무시됨
xhr.setRequestHeader('Referer', 'https://evil.com');   // 무시됨
xhr.setRequestHeader('Sec-Custom', 'value');           // 무시됨
xhr.setRequestHeader('Proxy-Authorization', 'Basic x'); // 무시됨

// 허용되는 커스텀 헤더
xhr.setRequestHeader('X-My-Header', 'value');          // 정상 동작
xhr.setRequestHeader('Authorization', 'Bearer token'); // 정상 동작

xhr.send();
```

### 11.4 금지 응답 헤더 (Forbidden Response Headers)

CORS 교차 출처 요청 시, 기본적으로 접근 가능한 응답 헤더는 CORS-safelisted response headers로 제한된다.

```javascript
// 교차 출처 응답에서 기본 접근 가능한 헤더
xhr.getResponseHeader('Cache-Control');     // 접근 가능
xhr.getResponseHeader('Content-Language');   // 접근 가능
xhr.getResponseHeader('Content-Length');     // 접근 가능
xhr.getResponseHeader('Content-Type');       // 접근 가능
xhr.getResponseHeader('Expires');            // 접근 가능
xhr.getResponseHeader('Last-Modified');      // 접근 가능
xhr.getResponseHeader('Pragma');             // 접근 가능

// 서버가 Access-Control-Expose-Headers로 명시하지 않으면 null 반환
xhr.getResponseHeader('X-Custom-Header');    // null (접근 불가)
xhr.getResponseHeader('X-Request-Id');       // null (접근 불가)

// 서버 응답에 다음이 포함되어야 접근 가능:
// Access-Control-Expose-Headers: X-Custom-Header, X-Request-Id
```

---

## 12. 실용 예제

### 12.1 GET 요청

```javascript
function httpGet(url) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('GET', url);
    xhr.responseType = 'json';

    xhr.onload = function () {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve({
          status: xhr.status,
          data: xhr.response,
          headers: xhr.getAllResponseHeaders(),
        });
      } else {
        reject(new Error(`HTTP Error ${xhr.status}: ${xhr.statusText}`));
      }
    };

    xhr.onerror = function () {
      reject(new Error('Network Error'));
    };

    xhr.send();
  });
}

// 사용
httpGet('/api/users')
  .then(result => console.log(result.data))
  .catch(err => console.error(err));
```

### 12.2 POST 요청

```javascript
function httpPost(url, data, contentType = 'application/json') {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('POST', url);
    xhr.responseType = 'json';

    let body;
    if (contentType === 'application/json') {
      xhr.setRequestHeader('Content-Type', 'application/json');
      body = JSON.stringify(data);
    } else if (data instanceof FormData) {
      // FormData일 때는 Content-Type을 설정하지 않음
      body = data;
    } else {
      xhr.setRequestHeader('Content-Type', contentType);
      body = data;
    }

    xhr.onload = function () {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve({ status: xhr.status, data: xhr.response });
      } else {
        reject(new Error(`HTTP Error ${xhr.status}: ${xhr.statusText}`));
      }
    };

    xhr.onerror = () => reject(new Error('Network Error'));

    xhr.send(body);
  });
}

// JSON 전송
httpPost('/api/users', { name: '홍길동', email: 'hong@example.com' })
  .then(result => console.log('생성됨:', result.data))
  .catch(err => console.error(err));
```

### 12.3 파일 업로드 (진행률 표시)

```javascript
function uploadFile(url, file, onProgress) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    const formData = new FormData();
    formData.append('file', file);

    // 업로드 진행률
    xhr.upload.onprogress = function (e) {
      if (e.lengthComputable && onProgress) {
        onProgress({
          loaded: e.loaded,
          total: e.total,
          percent: (e.loaded / e.total) * 100,
        });
      }
    };

    xhr.onload = function () {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve(xhr.response);
      } else {
        reject(new Error(`Upload failed: ${xhr.status}`));
      }
    };

    xhr.onerror = () => reject(new Error('Upload network error'));
    xhr.onabort = () => reject(new Error('Upload aborted'));
    xhr.ontimeout = () => reject(new Error('Upload timeout'));

    xhr.open('POST', url);
    xhr.responseType = 'json';
    xhr.timeout = 60000; // 60초
    xhr.send(formData);
  });
}

// 사용
const fileInput = document.getElementById('fileInput');
fileInput.addEventListener('change', async function () {
  const file = this.files[0];
  if (!file) return;

  try {
    const result = await uploadFile('/api/upload', file, progress => {
      console.log(`${progress.percent.toFixed(1)}% 완료 (${progress.loaded}/${progress.total})`);
      document.getElementById('progress').style.width = progress.percent + '%';
    });
    console.log('업로드 완료:', result);
  } catch (err) {
    console.error('업로드 실패:', err.message);
  }
});
```

### 12.4 JSON 통신 (CRUD)

```javascript
class ApiClient {
  constructor(baseURL) {
    this.baseURL = baseURL;
  }

  request(method, path, data = null) {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open(method, this.baseURL + path);
      xhr.responseType = 'json';
      xhr.setRequestHeader('Accept', 'application/json');

      if (data !== null) {
        xhr.setRequestHeader('Content-Type', 'application/json');
      }

      xhr.onload = function () {
        const response = {
          status: xhr.status,
          statusText: xhr.statusText,
          data: xhr.response,
          url: xhr.responseURL,
        };

        if (xhr.status >= 200 && xhr.status < 300) {
          resolve(response);
        } else {
          const error = new Error(`HTTP ${xhr.status}`);
          error.response = response;
          reject(error);
        }
      };

      xhr.onerror = () => reject(new Error('Network Error'));
      xhr.ontimeout = () => reject(new Error('Timeout'));

      xhr.timeout = 10000;
      xhr.send(data ? JSON.stringify(data) : null);
    });
  }

  get(path) {
    return this.request('GET', path);
  }

  post(path, data) {
    return this.request('POST', path, data);
  }

  put(path, data) {
    return this.request('PUT', path, data);
  }

  delete(path) {
    return this.request('DELETE', path);
  }
}

// 사용 예제
const api = new ApiClient('https://api.example.com');

// 목록 조회
const users = await api.get('/users');
console.log(users.data);

// 생성
const newUser = await api.post('/users', {
  name: '김철수',
  email: 'kim@example.com',
});

// 수정
await api.put('/users/1', { name: '김영희' });

// 삭제
await api.delete('/users/1');
```

### 12.5 이미지 다운로드 (Blob)

```javascript
function downloadImage(url) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('GET', url);
    xhr.responseType = 'blob';

    xhr.onprogress = function (e) {
      if (e.lengthComputable) {
        console.log(`다운로드: ${(e.loaded / e.total * 100).toFixed(1)}%`);
      }
    };

    xhr.onload = function () {
      if (xhr.status === 200) {
        const blob = xhr.response;
        const objectURL = URL.createObjectURL(blob);
        resolve({ blob, objectURL });
      } else {
        reject(new Error(`Download failed: ${xhr.status}`));
      }
    };

    xhr.onerror = () => reject(new Error('Download error'));
    xhr.send();
  });
}

// 사용: 이미지를 다운로드하여 페이지에 표시
async function displayImage(imageUrl) {
  try {
    const { blob, objectURL } = await downloadImage(imageUrl);

    const img = document.createElement('img');
    img.src = objectURL;
    img.onload = () => URL.revokeObjectURL(objectURL); // 메모리 해제
    document.body.appendChild(img);

    console.log(`이미지 타입: ${blob.type}, 크기: ${blob.size} bytes`);
  } catch (err) {
    console.error('이미지 로드 실패:', err);
  }
}

displayImage('/images/photo.jpg');
```

### 12.6 요청 취소 (abort)

```javascript
class CancellableRequest {
  constructor() {
    this.xhr = null;
  }

  get(url) {
    // 이전 요청이 있으면 취소
    this.cancel();

    return new Promise((resolve, reject) => {
      this.xhr = new XMLHttpRequest();
      this.xhr.open('GET', url);
      this.xhr.responseType = 'json';

      this.xhr.onload = () => {
        this.xhr = null;
        if (this.xhr === null) {
          // 이미 정리됨
        }
        resolve(arguments[0]);
      };

      const self = this;
      this.xhr.onload = function () {
        const response = this.response;
        const status = this.status;
        self.xhr = null;
        if (status >= 200 && status < 300) {
          resolve(response);
        } else {
          reject(new Error(`HTTP ${status}`));
        }
      };

      this.xhr.onabort = () => {
        this.xhr = null;
        reject(new DOMException('Request aborted', 'AbortError'));
      };

      this.xhr.onerror = () => {
        this.xhr = null;
        reject(new Error('Network Error'));
      };

      this.xhr.send();
    });
  }

  cancel() {
    if (this.xhr) {
      this.xhr.abort();
      this.xhr = null;
    }
  }
}

// 사용 예: 자동 완성 검색 (이전 요청 자동 취소)
const searchRequest = new CancellableRequest();
const searchInput = document.getElementById('search');

searchInput.addEventListener('input', async function () {
  const query = this.value.trim();
  if (!query) return;

  try {
    const results = await searchRequest.get(`/api/search?q=${encodeURIComponent(query)}`);
    renderResults(results);
  } catch (err) {
    if (err.name !== 'AbortError') {
      console.error('검색 오류:', err);
    }
    // AbortError는 새 요청에 의한 정상적인 취소이므로 무시
  }
});
```

### 12.7 Promise 래퍼 (범용)

```javascript
/
 * XMLHttpRequest를 Promise로 감싸는 범용 래퍼
 * @param {Object} options - 요청 옵션
 * @returns {Promise<Object>} 응답 객체
 */
function xhrRequest(options) {
  const {
    method = 'GET',
    url,
    headers = {},
    body = null,
    responseType = 'json',
    timeout = 30000,
    withCredentials = false,
    onProgress = null,
    onUploadProgress = null,
  } = options;

  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open(method, url);
    xhr.responseType = responseType;
    xhr.timeout = timeout;
    xhr.withCredentials = withCredentials;

    // 헤더 설정
    Object.entries(headers).forEach(([name, value]) => {
      xhr.setRequestHeader(name, value);
    });

    // 다운로드 진행률
    if (onProgress) {
      xhr.onprogress = function (e) {
        onProgress({
          lengthComputable: e.lengthComputable,
          loaded: e.loaded,
          total: e.total,
          percent: e.lengthComputable ? (e.loaded / e.total) * 100 : null,
        });
      };
    }

    // 업로드 진행률
    if (onUploadProgress) {
      xhr.upload.onprogress = function (e) {
        onUploadProgress({
          lengthComputable: e.lengthComputable,
          loaded: e.loaded,
          total: e.total,
          percent: e.lengthComputable ? (e.loaded / e.total) * 100 : null,
        });
      };
    }

    xhr.onload = function () {
      const response = {
        status: xhr.status,
        statusText: xhr.statusText,
        headers: xhr.getAllResponseHeaders(),
        data: xhr.response,
        url: xhr.responseURL,
      };

      if (xhr.status >= 200 && xhr.status < 300) {
        resolve(response);
      } else {
        const error = new Error(`Request failed with status ${xhr.status}`);
        error.response = response;
        reject(error);
      }
    };

    xhr.onerror = () => reject(new Error('Network Error'));
    xhr.ontimeout = () => reject(new Error(`Timeout of ${timeout}ms exceeded`));
    xhr.onabort = () => {
      const error = new Error('Request aborted');
      error.name = 'AbortError';
      reject(error);
    };

    // body 처리
    let processedBody = body;
    if (body !== null && typeof body === 'object' && !(body instanceof FormData)
        && !(body instanceof Blob) && !(body instanceof ArrayBuffer)
        && !(body instanceof URLSearchParams)) {
      if (!headers['Content-Type']) {
        xhr.setRequestHeader('Content-Type', 'application/json');
      }
      processedBody = JSON.stringify(body);
    }

    xhr.send(processedBody);
  });
}

// 사용 예시

// GET
const users = await xhrRequest({ url: '/api/users' });
console.log(users.data);

// POST with JSON
const created = await xhrRequest({
  method: 'POST',
  url: '/api/users',
  body: { name: '홍길동', role: 'admin' },
});

// 파일 업로드 with 진행률
const formData = new FormData();
formData.append('file', file);

const uploaded = await xhrRequest({
  method: 'POST',
  url: '/api/upload',
  body: formData,
  timeout: 120000,
  onUploadProgress: (p) => {
    console.log(`업로드: ${p.percent?.toFixed(1)}%`);
  },
});

// 이미지 다운로드
const imageData = await xhrRequest({
  url: '/images/large-photo.jpg',
  responseType: 'blob',
  onProgress: (p) => {
    if (p.percent !== null) {
      console.log(`다운로드: ${p.percent.toFixed(1)}%`);
    }
  },
});
```

### 12.8 재시도 로직

```javascript
/
 * 실패 시 자동 재시도하는 XHR 요청
 * @param {Object} options - xhrRequest 옵션
 * @param {number} maxRetries - 최대 재시도 횟수
 * @param {number} retryDelay - 재시도 간 대기 시간(ms)
 * @returns {Promise<Object>}
 */
async function xhrWithRetry(options, maxRetries = 3, retryDelay = 1000) {
  let lastError;

  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      return await xhrRequest(options);
    } catch (error) {
      lastError = error;

      // 4xx 에러는 재시도하지 않음 (클라이언트 측 오류)
      if (error.response && error.response.status >= 400 && error.response.status < 500) {
        throw error;
      }

      // 마지막 시도였으면 에러를 그대로 던짐
      if (attempt === maxRetries) {
        throw error;
      }

      // 지수 백오프 대기
      const delay = retryDelay * Math.pow(2, attempt);
      console.log(`요청 실패 (${attempt + 1}/${maxRetries + 1}). ${delay}ms 후 재시도...`);
      await new Promise(resolve => setTimeout(resolve, delay));
    }
  }

  throw lastError;
}

// 사용
try {
  const data = await xhrWithRetry(
    { url: '/api/unreliable-endpoint' },
    3,    // 최대 3회 재시도
    1000  // 초기 대기 1초 (1초, 2초, 4초로 증가)
  );
  console.log('성공:', data);
} catch (err) {
  console.error('모든 재시도 실패:', err);
}
```

### 12.9 동시 요청 관리

```javascript
/
 * 여러 XHR 요청을 동시에 실행하고 모든 결과를 수집
 */
function xhrAll(requestConfigs) {
  return Promise.all(requestConfigs.map(config => xhrRequest(config)));
}

/
 * 여러 XHR 요청 중 가장 먼저 완료되는 결과를 반환
 */
function xhrRace(requestConfigs) {
  return Promise.race(requestConfigs.map(config => xhrRequest(config)));
}

// 병렬 요청
const [users, posts, comments] = await xhrAll([
  { url: '/api/users' },
  { url: '/api/posts' },
  { url: '/api/comments' },
]);

console.log('사용자:', users.data.length);
console.log('게시글:', posts.data.length);
console.log('댓글:', comments.data.length);
```

---

## 참고 자료

- [WHATWG XMLHttpRequest Standard](https://xhr.spec.whatwg.org/)
- [WHATWG Fetch Standard](https://fetch.spec.whatwg.org/)
- [MDN - XMLHttpRequest](https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest)
- [MDN - FormData](https://developer.mozilla.org/en-US/docs/Web/API/FormData)
- [MDN - ProgressEvent](https://developer.mozilla.org/en-US/docs/Web/API/ProgressEvent)
- [MDN - Using XMLHttpRequest](https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest_API/Using_XMLHttpRequest)

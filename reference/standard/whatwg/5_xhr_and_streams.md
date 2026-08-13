# WHATWG XMLHttpRequest와 Streams

## WHATWG XMLHttpRequest Standard 완벽 가이드

### 목차

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

### 1. 개요

#### 1.1 XMLHttpRequest Standard란?

XMLHttpRequest Standard: WHATWG(Web Hypertext Application Technology Working Group)에서 관리하는 웹 표준 → 클라이언트 측 스크립트에서 HTTP(S) 요청을 프로그래밍 방식으로 전송·응답 처리하기 위한 API 정의

- 이름에 "XML" 포함 → 그러나 XML에 국한되지 않음
- JSON·HTML·텍스트·바이너리 등 다양한 형식의 데이터 송수신 가능

이 표준이 정의하는 핵심 인터페이스:

- XMLHttpRequest: HTTP 요청과 응답을 처리하는 주 인터페이스
- XMLHttpRequestUpload: 업로드 진행 상황 모니터링 인터페이스
- ProgressEvent: 전송 진행률 정보를 담는 이벤트 인터페이스
- FormData: 폼 데이터를 키-값 쌍으로 관리하는 인터페이스

> 표준 문서: https://xhr.spec.whatwg.org/

#### 1.2 역사

XMLHttpRequest의 역사: 웹 개발 패러다임 전환의 핵심 축

시기별 사건:
- 1998~1999: Microsoft가 Outlook Web Access를 위해 `XMLHTTP` ActiveX 객체 개발
- 2000: IE 5.0에서 `new ActiveXObject("Microsoft.XMLHTTP")`로 사용 가능
- 2002: Mozilla가 `XMLHttpRequest`라는 이름으로 네이티브 객체 구현
- 2004: Gmail 출시 · XHR을 활용한 비동기 인터페이스로 웹 애플리케이션의 가능성 입증
- 2005: Jesse James Garrett이 "AJAX" 용어를 처음 사용 (Asynchronous JavaScript and XML)
- 2005: Google Maps 출시 · XHR 기반 동적 지도 로딩으로 AJAX 대중화
- 2006: W3C가 XMLHttpRequest Level 1 초안 발행
- 2008: XMLHttpRequest Level 2 초안 · CORS·진행률 이벤트·바이너리 데이터 지원 추가
- 2012: Level 1과 Level 2가 단일 XMLHttpRequest 표준으로 통합
- 2014~: WHATWG가 Living Standard로 관리

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

#### 1.3 AJAX의 탄생

AJAX(Asynchronous JavaScript and XML): 특정 기술이 아니라 여러 기술의 조합을 지칭하는 용어

- 2005년 Jesse James Garrett이 발표한 글에서 처음 명명

AJAX의 핵심 개념: 전체 페이지를 새로고침하지 않고도 서버와 데이터를 교환 → 페이지 일부만 동적으로 갱신

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

#### 1.4 Fetch API와의 비교

XMLHttpRequest와 Fetch API: 동일한 목적(HTTP 통신) 수행 → 그러나 설계 철학이 근본적으로 다름

특성별 비교:
- API 패러다임
  - XMLHttpRequest: 이벤트 기반 (콜백)
  - Fetch API: Promise 기반
- 요청/응답 객체
  - XMLHttpRequest: 단일 객체가 모든 것을 관리
  - Fetch API: `Request`, `Response` 분리
- 스트리밍
  - XMLHttpRequest: 제한적 (`responseType` 의존)
  - Fetch API: `ReadableStream` 네이티브 지원
- 업로드 진행률
  - XMLHttpRequest: `xhr.upload.onprogress` 네이티브 지원
  - Fetch API: 직접 구현 필요 (ReadableStream)
- 다운로드 진행률
  - XMLHttpRequest: `xhr.onprogress` 네이티브 지원
  - Fetch API: `Response.body` ReadableStream으로 구현
- 요청 취소
  - XMLHttpRequest: `xhr.abort()`
  - Fetch API: `AbortController` / `AbortSignal`
- 타임아웃
  - XMLHttpRequest: `xhr.timeout` 속성
  - Fetch API: `AbortSignal.timeout()`
- 동기 요청
  - XMLHttpRequest: 지원 (비권장)
  - Fetch API: 미지원 (비동기 전용)
- 쿠키 전송
  - XMLHttpRequest: 동일 출처 시 기본 전송
  - Fetch API: `credentials` 옵션으로 명시적 제어
- HTTP 에러
  - XMLHttpRequest: `status`로 직접 확인
  - Fetch API: Promise가 reject되지 않음 (`ok` 속성 확인)
- Service Worker
  - XMLHttpRequest: 사용 불가
  - Fetch API: 네이티브 지원
- 캐시 제어
  - XMLHttpRequest: 헤더로 수동 제어
  - Fetch API: `cache` 옵션 제공
- 리다이렉트 제어
  - XMLHttpRequest: 자동 추적 (제어 불가)
  - Fetch API: `redirect` 옵션 제공
- 표준 관리
  - XMLHttpRequest: WHATWG XMLHttpRequest Standard
  - Fetch API: WHATWG Fetch Standard

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

### 2. XMLHttpRequest 인터페이스

#### 2.1 생성자

`XMLHttpRequest` 객체: `new` 키워드로 생성 → 생성자는 인자를 받지 않음

```javascript
const xhr = new XMLHttpRequest();
```

생성 직후 객체의 초기 상태:
- `readyState`: `0` (UNSENT)
- `status`: `0`
- `statusText`: `""`
- `responseType`: `""`
- `response`: `""`
- `responseText`: `""`
- `responseURL`: `""`
- `timeout`: `0`
- `withCredentials`: `false`

#### 2.2 readyState

`readyState` 속성: XHR 객체의 현재 상태를 나타내는 읽기 전용 정수값 → 요청의 생명주기 전체를 5단계로 표현

상수별 값과 설명:
- `XMLHttpRequest.UNSENT`: `0` · 객체가 생성되었지만 `open()`이 호출되지 않은 상태
- `XMLHttpRequest.OPENED`: `1` · `open()`이 호출된 상태. `setRequestHeader()`와 `send()` 호출 가능
- `XMLHttpRequest.HEADERS_RECEIVED`: `2` · `send()`가 호출되었고, 응답 헤더가 수신된 상태
- `XMLHttpRequest.LOADING`: `3` · 응답 본문(body)을 수신 중인 상태
- `XMLHttpRequest.DONE`: `4` · 요청이 완료된 상태 (성공 또는 실패)

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

#### 2.3 주요 속성

##### status / statusText

HTTP 응답 상태 코드와 상태 메시지 나타냄 → `readyState`가 `UNSENT` 또는 `OPENED`일 때 접근하면 각각 `0`과 `""` 반환

```javascript
xhr.onload = function () {
  console.log(xhr.status);      // 200
  console.log(xhr.statusText);  // "OK"
};
```

> 참고: HTTP/2에서는 상태 텍스트가 정의되지 않으므로 `statusText`는 빈 문자열일 수 있음

##### responseType

응답 데이터의 타입 지정 → `open()` 호출 후, `send()` 호출 전에 설정해야 함

값별 `response` 타입과 설명:
- `""` (기본값): `string` · 텍스트로 반환 (`responseText`와 동일)
- `"text"`: `string` · 텍스트로 반환
- `"json"`: `object` / `null` · JSON 파싱 결과 반환
- `"document"`: `Document` / `null` · HTML/XML 문서로 파싱
- `"arraybuffer"`: `ArrayBuffer` / `null` · 바이너리 데이터
- `"blob"`: `Blob` / `null` · Blob 객체

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

##### response / responseText / responseXML

- `response`: `responseType`에 따라 적절한 타입의 데이터를 반환하는 범용 속성
- `responseText`: 응답을 텍스트 문자열로 반환. `responseType`이 `""` 또는 `"text"`일 때만 접근 가능 → 그 외의 경우 `InvalidStateError` 발생
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

##### responseURL

최종 응답의 URL 반환 → 리다이렉트가 발생한 경우 리다이렉트 후의 최종 URL이 됨 → URL 프래그먼트(#)는 제거됨

```javascript
const xhr = new XMLHttpRequest();
xhr.open('GET', '/old-endpoint');  // 301 리다이렉트 → /new-endpoint
xhr.onload = function () {
  console.log(xhr.responseURL);    // "https://example.com/new-endpoint"
};
xhr.send();
```

##### timeout

요청 타임아웃을 밀리초 단위로 설정 → 기본값은 `0`(무제한) → 설정된 시간 내에 응답이 완료되지 않으면 요청이 자동으로 종료되고 `timeout` 이벤트 발생

```javascript
const xhr = new XMLHttpRequest();
xhr.open('GET', '/api/slow-endpoint');
xhr.timeout = 10000; // 10초
xhr.ontimeout = function () {
  console.error('요청 시간 초과');
};
xhr.send();
```

> 주의: 동기 요청에서 `timeout`을 설정하면 `InvalidAccessError` 발생

##### withCredentials

크로스 오리진 요청 시 쿠키·HTTP 인증 정보·TLS 클라이언트 인증서 등의 자격 증명(credentials) 포함 여부 지정 → 기본값은 `false`

```javascript
const xhr = new XMLHttpRequest();
xhr.open('GET', 'https://api.other-domain.com/data');
xhr.withCredentials = true;  // 크로스 오리진 쿠키 포함
xhr.onload = function () {
  console.log(xhr.response);
};
xhr.send();
```

##### upload

`XMLHttpRequestUpload` 객체 반환 → 이 객체에 이벤트 핸들러를 등록하여 업로드 진행 상황 모니터링 가능. 자세한 내용은 [6장](#6-xmlhttprequestupload) 참고

---

### 3. 요청 메서드

#### 3.1 open(method, url [, async [, username [, password]]])

요청을 초기화함 → 이미 활성화된 요청이 있으면 `abort()`를 호출한 것과 동일하게 동작

매개변수별 타입과 설명:
- `method`: `string` · HTTP 메서드 (`GET`, `POST`, `PUT`, `DELETE`, `PATCH` 등)
- `url`: `string` · 요청 URL (상대 또는 절대 경로)
- `async`: `boolean` · 비동기 여부 (기본값: `true`)
- `username`: `string` · HTTP 인증 사용자명 (선택)
- `password`: `string` · HTTP 인증 비밀번호 (선택)

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

> 금지된 메서드: `CONNECT`, `TRACE`, `TRACK`은 보안상의 이유로 사용할 수 없으며, `SecurityError` 발생

#### 3.2 setRequestHeader(name, value)

요청 헤더를 설정함 → 반드시 `open()` 호출 후, `send()` 호출 전에 사용해야 함 → 동일한 헤더를 여러 번 호출하면 값이 쉼표로 구분되어 합쳐짐

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

금지된 요청 헤더(forbidden request headers)는 설정 불가 → 시도해도 조용히 무시됨. 자세한 목록은 [11장](#11-보안-제한) 참고

#### 3.3 send(body)

요청을 전송함 → `body` 매개변수는 요청 본문이며, 다양한 타입을 지원함

body 타입별 자동 Content-Type과 설명:
- `null` / 생략: 없음 · GET, HEAD 요청 시 사용
- `string`: `text/plain;charset=UTF-8` · 텍스트 데이터
- `FormData`: `multipart/form-data` (boundary 포함) · 폼 데이터, 파일 업로드
- `Blob`: Blob의 `type` 속성 · 바이너리 데이터
- `ArrayBuffer` / `ArrayBufferView`: 없음 · 로우 바이너리 데이터
- `URLSearchParams`: `application/x-www-form-urlencoded;charset=UTF-8` · URL 인코딩된 데이터
- `Document`: HTML: `text/html`, XML: `application/xml` · HTML/XML 문서

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

> `send()`는 비동기 요청일 때 즉시 반환되며, 동기 요청일 때는 응답이 완료될 때까지 블로킹함

#### 3.4 abort()

이미 전송된 요청을 중단함. 호출 시 다음이 발생:

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

#### 3.5 overrideMimeType(mime)

서버가 반환한 MIME 타입을 무시하고 지정된 MIME 타입으로 응답을 해석하도록 강제함 → `send()` 호출 전에 사용해야 함

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

### 4. 응답 처리

#### 4.1 getResponseHeader(name)

지정된 이름의 응답 헤더 값 반환 → 이름은 대소문자를 구분하지 않음 → 해당 헤더가 없으면 `null` 반환 → 동일 이름의 헤더가 여러 개 있으면 쉼표로 구분된 단일 문자열로 반환

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

> 참고: CORS 환경에서는 기본적으로 "단순 응답 헤더(CORS-safelisted response headers)"만 접근 가능함 · `Cache-Control`, `Content-Language`, `Content-Length`, `Content-Type`, `Expires`, `Last-Modified`, `Pragma`. 그 외 헤더는 서버가 `Access-Control-Expose-Headers`로 명시적 허용해야 함

#### 4.2 getAllResponseHeaders()

모든 응답 헤더를 하나의 문자열로 반환함 → 각 헤더는 `\r\n`(CRLF)으로 구분 → 헤더 이름과 값은 `: `(콜론+공백)으로 구분

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

#### 4.3 responseType 상세

`responseType`을 설정하면 브라우저가 응답 데이터를 자동으로 해당 형식으로 변환함

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

### 5. 이벤트

#### 5.1 이벤트 목록

XMLHttpRequest와 XMLHttpRequestUpload는 `XMLHttpRequestEventTarget`을 상속함 → 다음 이벤트를 지원함

이벤트별 발생 시점과 이벤트 객체:
- `readystatechange`: `readyState`가 변경될 때 · `Event`
- `loadstart`: 요청 전송이 시작될 때 · `ProgressEvent`
- `progress`: 데이터 수신/전송 중 (주기적) · `ProgressEvent`
- `abort`: `abort()` 호출로 요청이 취소될 때 · `ProgressEvent`
- `error`: 네트워크 오류 발생 시 · `ProgressEvent`
- `load`: 요청이 성공적으로 완료될 때 · `ProgressEvent`
- `timeout`: 설정된 시간 내에 응답이 오지 않을 때 · `ProgressEvent`
- `loadend`: 요청이 완료될 때 (성공/실패/취소 불문) · `ProgressEvent`

#### 5.2 이벤트 발생 순서

표준에서 정의하는 이벤트 발생 순서:

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

> `readystatechange`는 별도로, `readyState`가 변경될 때마다 발생함. `load`, `error`, `timeout`, `abort` 중 하나만 발생하며, `loadend`는 항상 마지막에 발생함

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

#### 5.3 이벤트 핸들러 등록 방식

이벤트 핸들러는 두 가지 방식으로 등록 가능:

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
### 6. XMLHttpRequestUpload

#### 6.1 개요

`XMLHttpRequestUpload`: 업로드 과정을 모니터링하기 위한 인터페이스

- `XMLHttpRequest` 객체의 `upload` 속성을 통해 접근
- `XMLHttpRequestEventTarget`을 상속 → 동일한 이벤트 세트(`loadstart`, `progress`, `load`, `error`, `abort`, `timeout`, `loadend`) 지원

#### 6.2 업로드 진행률 모니터링

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

#### 6.3 upload 이벤트와 CORS

보안 사항:

- 크로스 오리진 요청에서 `xhr.upload`에 이벤트 리스너 등록 → 해당 요청이 "단순 요청(simple request)"에서 제외 → 반드시 preflight 요청 발생
- 목적: 업로드 진행률을 통해 서버의 존재 여부·네트워크 특성을 추론하는 공격 방지

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

### 7. ProgressEvent

#### 7.1 인터페이스 정의

`ProgressEvent`: 작업의 진행 상태를 나타내는 이벤트

- `Event`를 상속 → 세 가지 추가 속성 보유
  - `lengthComputable` (`boolean`): 전체 크기를 알 수 있는지 여부
  - `loaded` (`number`): 현재까지 전송된 바이트 수
  - `total` (`number`): 전체 바이트 수 (`lengthComputable`이 `false`이면 `0`)

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

#### 7.2 lengthComputable이 false인 경우

- 서버가 `Content-Length` 헤더를 제공하지 않거나 Transfer-Encoding이 chunked인 경우 → `lengthComputable`이 `false`
- 이 경우 `total`은 `0`

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

#### 7.3 ProgressEvent 생성

- 커스텀 ProgressEvent 생성 가능 (테스트 등의 목적)

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

### 8. FormData 인터페이스

#### 8.1 개요

`FormData` 인터페이스: HTML 폼 데이터를 키-값 쌍으로 관리할 수 있게 해주는 API

- `XMLHttpRequest.send()`에 직접 전달 가능 → 이 경우 `multipart/form-data` 인코딩으로 자동 전송

#### 8.2 생성자

```javascript
// 빈 FormData 생성
const fd1 = new FormData();

// 기존 HTML 폼에서 생성
const form = document.getElementById('myForm');
const fd2 = new FormData(form);
// 폼의 모든 필드(name 속성이 있는)가 자동으로 추가됨
```

#### 8.3 메서드

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

#### 8.4 이터레이터 메서드

- `FormData`는 이터러블(iterable) → `entries()`, `keys()`, `values()` 메서드 제공

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

#### 8.5 FormData와 XHR 전송

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

### 9. CORS와 XHR

#### 9.1 동일 출처와 교차 출처 요청

- XMLHttpRequest: 기본적으로 동일 출처 정책(Same-Origin Policy)의 제약을 받음
- 동일 출처: 프로토콜·호스트·포트가 모두 같은 경우

```
페이지 출처: https://example.com:443

https://example.com/api/data       → 동일 출처 (경로만 다름)
https://example.com:443/api/data   → 동일 출처 (포트 명시, 동일)
http://example.com/api/data        → 교차 출처 (프로토콜 다름)
https://api.example.com/data       → 교차 출처 (호스트 다름)
https://example.com:8080/data      → 교차 출처 (포트 다름)
```

#### 9.2 CORS 동작 방식

- 교차 출처 요청 허용 조건: 서버가 적절한 CORS 헤더를 응답에 포함

단순 요청(Simple Request): 다음 조건을 모두 만족하면 preflight 없이 직접 전송

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

Preflight 요청: 단순 요청 조건 미충족 시 → 브라우저가 자동으로 `OPTIONS` 메서드의 preflight 요청을 먼저 전송

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

#### 9.3 withCredentials

- 교차 출처 요청에서 쿠키·인증 정보 포함 → `withCredentials`를 `true`로 설정 + 서버가 `Access-Control-Allow-Credentials: true` 응답 필요
- 이 경우 `Access-Control-Allow-Origin`에 와일드카드(`*`) 사용 불가

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

### 10. 동기 vs 비동기

#### 10.1 비동기 요청 (기본)

- `open()`의 세 번째 인자 `async`가 `true`(기본값) → 비동기 요청
- `send()` 호출 즉시 반환 → 응답은 이벤트 핸들러를 통해 처리

```javascript
const xhr = new XMLHttpRequest();
xhr.open('GET', '/api/data', true); // 또는 xhr.open('GET', '/api/data')
xhr.onload = function () {
  console.log('비동기 응답:', xhr.response);
};
xhr.send();
console.log('send() 직후 — 응답 대기 중'); // 이 줄이 먼저 실행됨
```

#### 10.2 동기 요청

- `async`를 `false`로 설정 → 동기 요청
- `send()`가 응답이 올 때까지 블로킹

```javascript
const xhr = new XMLHttpRequest();
xhr.open('GET', '/api/data', false); // 동기 요청
xhr.send();
// send()가 응답을 받을 때까지 여기서 블로킹
console.log('동기 응답:', xhr.responseText); // 응답 즉시 사용 가능
```

#### 10.3 동기 XHR의 문제점

동기 XHR: 여러 심각한 문제 유발 → 메인 스레드(document 환경)에서의 사용은 강력히 비권장(deprecated)

- UI 프리징: 메인 스레드가 네트워크 응답을 기다리는 동안 모든 사용자 상호작용(클릭·스크롤·입력) 차단
- 이벤트 처리 중단: 타이머·애니메이션·다른 비동기 작업 모두 대기 상태
- 사용자 경험 저하: 브라우저가 "응답 없음" 상태로 보임 → 탭이 멈춘 것처럼 느껴짐

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

- 메인 스레드 (window): 동작하지만 콘솔 경고 발생 · 일부 기능 제한
- Worker 내부: 제한 없이 동작 (Worker는 별도 스레드)
- 페이지 unload 중 (`beforeunload`, `unload`): 브라우저에 따라 무시될 수 있음
- `document` 환경에서 `async=false`: 표준에서 deprecated · 향후 제거 가능

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

### 11. 보안 제한

#### 11.1 Same-Origin Policy

- XMLHttpRequest: 기본적으로 동일 출처 정책의 적용을 받음
- 교차 출처 요청은 CORS 메커니즘을 통해서만 허용

#### 11.2 금지 메서드 (Forbidden Methods)

다음 HTTP 메서드는 `open()`에서 사용 불가 → `SecurityError` 발생

- `CONNECT`: 프록시 터널링에 사용 · 악용 시 내부 네트워크 접근 가능
- `TRACE`: 요청 내용을 그대로 반환 → 쿠키·인증 정보 탈취 위험(XST 공격)
- `TRACK`: `TRACE`의 Microsoft 구현체 · 동일한 보안 위험

```javascript
const xhr = new XMLHttpRequest();
try {
  xhr.open('TRACE', '/');
} catch (e) {
  console.log(e.name);    // "SecurityError"
  console.log(e.message); // "Failed to execute 'open' on 'XMLHttpRequest'..."
}
```

#### 11.3 금지 요청 헤더 (Forbidden Request Headers)

다음 헤더는 `setRequestHeader()`로 설정 불가 → 시도해도 조용히 무시됨(에러 없음)

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
- `X-HTTP-Method` (조건부 금지: 메서드 오버라이드 목적 사용 시)
- `X-HTTP-Method-Override` (조건부 금지: 메서드 오버라이드 목적 사용 시)
- `X-Method-Override` (조건부 금지: 메서드 오버라이드 목적 사용 시)
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

#### 11.4 금지 응답 헤더 (Forbidden Response Headers)

- CORS 교차 출처 요청 시 → 기본적으로 접근 가능한 응답 헤더는 CORS-safelisted response headers로 제한

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

### 12. 실용 예제

#### 12.1 GET 요청

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

#### 12.2 POST 요청

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

#### 12.3 파일 업로드 (진행률 표시)

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

#### 12.4 JSON 통신 (CRUD)

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

#### 12.5 이미지 다운로드 (Blob)

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

#### 12.6 요청 취소 (abort)

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

#### 12.7 Promise 래퍼 (범용)

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

#### 12.8 재시도 로직

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

#### 12.9 동시 요청 관리

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

### 참고 자료

- [WHATWG XMLHttpRequest Standard](https://xhr.spec.whatwg.org/)
- [WHATWG Fetch Standard](https://fetch.spec.whatwg.org/)
- [MDN - XMLHttpRequest](https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest)
- [MDN - FormData](https://developer.mozilla.org/en-US/docs/Web/API/FormData)
- [MDN - ProgressEvent](https://developer.mozilla.org/en-US/docs/Web/API/ProgressEvent)
- [MDN - Using XMLHttpRequest](https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest_API/Using_XMLHttpRequest)

---

## WHATWG Streams Standard

### 목차

1. [개요](#1-개요)
2. [스트림 기본 개념](#2-스트림-기본-개념)
3. [ReadableStream](#3-readablestream)
4. [ReadableStreamDefaultReader](#4-readablestreamdefaultreader)
5. [ReadableStreamBYOBReader](#5-readablestreambyobreader)
6. [ReadableByteStreamController](#6-readablebytestreamcontroller)
7. [ReadableStreamDefaultController](#7-readablestreamdefaultcontroller)
8. [WritableStream](#8-writablestream)
9. [WritableStreamDefaultWriter](#9-writablestreamdefaultwriter)
10. [WritableStreamDefaultController](#10-writablestreamdefaultcontroller)
11. [TransformStream](#11-transformstream)
12. [TransformStreamDefaultController](#12-transformstreamdefaultcontroller)
13. [큐잉 전략 (Queuing Strategies)](#13-큐잉-전략-queuing-strategies)
14. [배압 (Backpressure) 메커니즘](#14-배압-backpressure-메커니즘)
15. [파이프 (Piping)](#15-파이프-piping)
16. [Tee (분기)](#16-tee-분기)
17. [바이트 스트림 (Byte Streams)](#17-바이트-스트림-byte-streams)
18. [실용 예제](#18-실용-예제)

---

### 1. 개요

#### Streams Standard란

WHATWG Streams Standard: 웹 플랫폼에서 스트리밍 데이터를 생성·소비하기 위한 API를 정의하는 표준

- 데이터를 한 번에 전부 메모리에 올리지 않고 청크(chunk) 단위로 점진적 처리 가능한 추상화 계층 제공
- 공식 명세: https://streams.spec.whatwg.org/

#### 왜 필요한가

전통적인 웹 API(예: `XMLHttpRequest`) → 응답 전체가 도착해야만 데이터 사용 가능 → 다음 한계 존재

- 메모리 비효율: 수 GB 파일 다운로드 시 전체를 메모리에 적재해야 함
- 지연 시간: 전체 데이터가 도착할 때까지 아무것도 할 수 없음
- 배압 부재: 생산자와 소비자 간 속도 차이 제어 불가
- 조합 불가: 데이터 변환 파이프라인을 유연하게 구성 불가

Streams API → 이 모든 문제 해결

```js
// 전통적 방식: 전체 응답을 한 번에 가져옴
const response = await fetch('/large-file');
const data = await response.text(); // 전체가 메모리에 올라감

// 스트림 방식: 청크 단위로 점진적 처리
const response = await fetch('/large-file');
const reader = response.body.getReader();

while (true) {
  const { done, value } = await reader.read();
  if (done) break;
  processChunk(value); // 청크 단위로 처리, 메모리 효율적
}
```

#### 스트림의 개념

스트림: 시간에 걸쳐 사용 가능해지는 일련의 데이터를 추상화한 것

- 물리적 비유: 수도관 → 물(데이터)이 수도관(스트림)을 통해 흐르며, 수도꼭지(소비자)에서 필요한 만큼 받아 사용

Streams Standard가 정의하는 세 가지 유형의 스트림

```
ReadableStream  →  TransformStream  →  WritableStream
(데이터 소스)       (데이터 변환)         (데이터 싱크)
```

- ReadableStream: 데이터를 읽을 수 있는 소스 · `fetch()` 응답의 `body`가 대표적
- WritableStream: 데이터를 쓸 수 있는 싱크 · 파일 쓰기·네트워크 전송 등
- TransformStream: 데이터를 변환하는 중간 단계 · 읽기 가능 면(readable)과 쓰기 가능 면(writable)을 모두 가짐

#### 역사

- 2013년: Streams API 초기 논의 시작
- 2014년: WHATWG에서 Streams Standard 초안 작성 시작(Domenic Denicola 주도)
- 2015년: Chrome에서 `ReadableStream` 최초 구현
- 2016년: `WritableStream`, `TransformStream` 명세 추가
- 2017년: Fetch API와 Streams 통합(response.body)
- 2020년: 모든 주요 브라우저에서 기본 Streams API 지원
- 2022년: `ReadableStream.from()` 정적 메서드 추가
- 2023-현재: 바이트 스트림, async iterable 지원 등 지속 확장

---

### 2. 스트림 기본 개념

#### 청크 (Chunk)

청크: 스트림을 구성하는 개별 데이터 조각

- 어떤 타입이든 가능 · 문자열, `Uint8Array`, JSON 객체 등 자바스크립트의 모든 값이 청크가 될 수 있음

```js
// 문자열 청크를 사용하는 스트림
const stream = new ReadableStream({
  start(controller) {
    controller.enqueue('첫 번째 청크');
    controller.enqueue('두 번째 청크');
    controller.enqueue('세 번째 청크');
    controller.close();
  }
});

// 바이트 청크를 사용하는 스트림
const byteStream = new ReadableStream({
  start(controller) {
    controller.enqueue(new Uint8Array([72, 101, 108, 108, 111]));
    controller.enqueue(new Uint8Array([87, 111, 114, 108, 100]));
    controller.close();
  }
});
```

#### 내부 큐 (Internal Queue)

모든 스트림 → 내부 큐 보유

- 아직 소비되지 않은 청크들을 임시 보관하는 버퍼 역할

```
[생산자] → [ 큐: [청크1][청크2][청크3] ] → [소비자]
             ↑ enqueue()                    ↑ read()
```

- 내부 큐의 크기는 큐잉 전략(Queuing Strategy)이 관리
- 큐가 가득 차면 → 생산자에게 배압(backpressure) 전달

#### 배압 (Backpressure)

배압: 스트림 시스템의 핵심 메커니즘

- 소비자가 처리할 수 있는 속도보다 생산자가 더 빠르게 데이터를 만들어 내는 상황 제어

```
빠른 생산자 ──→ [큐가 차오름] ──→ 배압 신호 ──→ 생산자 속도 감소
                                               ↓
느린 소비자 ←── [큐에서 꺼냄] ←── 소비 진행   ←── 큐 여유 생김
                                               ↓
                                          생산자 재개
```

배압 부재 → 메모리 사용량 무한 증가 또는 데이터 손실 가능

```js
// 배압이 적용되는 예제
const readable = new ReadableStream({
  pull(controller) {
    // pull()은 내부 큐에 여유가 있을 때만 호출된다
    // 즉, 소비자가 데이터를 읽어서 큐에 공간이 생겨야 호출된다
    console.log('desiredSize:', controller.desiredSize);
    controller.enqueue(generateData());
  }
}, new CountQueuingStrategy({ highWaterMark: 5 }));
```

#### 파이프 체인 (Pipe Chain)

여러 스트림을 연결 → 데이터 처리 파이프라인 구성 가능

- Unix의 파이프(`|`)와 유사한 개념

```js
// 파이프 체인 예시
// ReadableStream → TransformStream → TransformStream → WritableStream
await readableStream
  .pipeThrough(new TextDecoderStream())       // 바이트 → 문자열 변환
  .pipeThrough(new TransformStream({          // 커스텀 변환
    transform(chunk, controller) {
      controller.enqueue(chunk.toUpperCase());
    }
  }))
  .pipeTo(writableStream);                    // 최종 싱크로 전달
```

파이프 체인에서 배압은 역방향으로 전파

- 최종 싱크(WritableStream)가 느려지면 → 그 영향이 체인을 따라 역순으로 전파 → 소스(ReadableStream)의 생산 속도까지 제어

---

### 3. ReadableStream

`ReadableStream`: 데이터를 읽을 수 있는 스트림을 나타내는, 스트림 API의 가장 기본이 되는 인터페이스

#### 생성자

```js
new ReadableStream(underlyingSource?, queuingStrategy?)
```

##### underlyingSource 객체

- `start(controller)`: 스트림 생성 시 한 번 호출 · 초기화 로직 수행
- `pull(controller)`: 내부 큐에 여유가 있을 때 호출 · 데이터 공급
- `cancel(reason)`: 스트림이 취소될 때 호출 · 리소스 정리
- `type`: `"bytes"`로 설정하면 바이트 스트림 생성
- `autoAllocateChunkSize`: 바이트 스트림에서 자동 버퍼 할당 크기

##### start(controller)

스트림이 생성될 때 즉시 한 번 호출

- Promise를 반환할 수 있음 → 이 경우 Promise가 이행될 때까지 다른 메서드 호출이 지연됨

```js
const stream = new ReadableStream({
  async start(controller) {
    // 데이터베이스 연결 등 초기화 작업
    const db = await connectToDatabase();
    this.db = db;

    // 초기 데이터를 즉시 큐에 넣을 수도 있다
    const firstBatch = await db.query('SELECT * FROM items LIMIT 10');
    for (const item of firstBatch) {
      controller.enqueue(item);
    }
  }
});
```

##### pull(controller)

내부 큐의 크기가 `highWaterMark` 미만일 때 반복적으로 호출

- 비동기 데이터 소스에서 데이터를 가져오는 데 적합

```js
const stream = new ReadableStream({
  start() {
    this.offset = 0;
    this.pageSize = 100;
  },

  async pull(controller) {
    // 페이지네이션된 API에서 데이터를 가져온다
    const response = await fetch(`/api/items?offset=${this.offset}&limit=${this.pageSize}`);
    const data = await response.json();

    if (data.items.length === 0) {
      controller.close(); // 더 이상 데이터가 없으면 스트림 닫기
      return;
    }

    for (const item of data.items) {
      controller.enqueue(item);
    }
    this.offset += this.pageSize;
  }
});
```

중요: `pull()`이 Promise를 반환하면 → 해당 Promise가 이행될 때까지 다음 `pull()` 호출이 보류 → 자연스러운 배압 형성

##### cancel(reason)

소비자가 스트림을 취소했을 때 호출

- 리소스 정리(파일 핸들 닫기, 네트워크 연결 해제 등)에 사용

```js
const stream = new ReadableStream({
  start(controller) {
    this.intervalId = setInterval(() => {
      controller.enqueue(new Date().toISOString());
    }, 1000);
  },

  cancel(reason) {
    console.log('스트림 취소됨:', reason);
    clearInterval(this.intervalId); // 타이머 정리
  }
});
```

##### type

`"bytes"`로 설정 → 바이트 스트림

- `ReadableByteStreamController`를 사용하며 BYOB(Bring Your Own Buffer) 리더 지원

```js
const byteStream = new ReadableStream({
  type: 'bytes',
  start(controller) {
    // controller는 ReadableByteStreamController
    controller.enqueue(new Uint8Array([1, 2, 3]));
  }
});
```

#### queuingStrategy

내부 큐의 동작을 제어

- `highWaterMark`: 배압이 적용되기 시작하는 큐 크기 임계값
- `size(chunk)`: 각 청크의 크기를 계산하는 함수

```js
const stream = new ReadableStream(
  {
    pull(controller) {
      controller.enqueue({ data: 'some data', timestamp: Date.now() });
    }
  },
  {
    highWaterMark: 10,                    // 큐에 최대 10개 청크
    size(chunk) { return 1; }             // 각 청크의 크기는 1
  }
);
```

#### 인스턴스 메서드

##### getReader(options?)

스트림에서 데이터를 읽기 위한 Reader를 획득

- Reader가 활성 상태인 동안 스트림은 잠긴(locked) 상태

```js
const reader = stream.getReader();
// stream.locked === true

// 기본 리더
const defaultReader = stream.getReader(); // ReadableStreamDefaultReader

// BYOB 리더 (바이트 스트림 전용)
const byobReader = byteStream.getReader({ mode: 'byob' }); // ReadableStreamBYOBReader
```

##### pipeThrough(transformStream, options?)

스트림 데이터를 `TransformStream`에 통과 → 변환된 결과를 새로운 `ReadableStream`으로 반환

```js
const uppercased = readableStream.pipeThrough(new TransformStream({
  transform(chunk, controller) {
    controller.enqueue(chunk.toUpperCase());
  }
}));

// options 매개변수
const result = readableStream.pipeThrough(transformStream, {
  preventClose: false,    // true면 소스 종료 시 변환 스트림을 닫지 않음
  preventAbort: false,    // true면 소스 오류 시 변환 스트림을 중단하지 않음
  preventCancel: false,   // true면 대상 오류 시 소스 스트림을 취소하지 않음
  signal: abortController.signal  // AbortSignal로 파이프 취소 가능
});
```

##### pipeTo(writableStream, options?)

스트림 데이터를 `WritableStream`에 전송

- 파이프가 완료되면 이행되는 Promise를 반환

```js
await readableStream.pipeTo(writableStream);

// 옵션과 함께 사용
const controller = new AbortController();

const pipePromise = readableStream.pipeTo(writableStream, {
  preventClose: false,
  preventAbort: false,
  preventCancel: false,
  signal: controller.signal
});

// 필요시 파이프 중단
setTimeout(() => controller.abort('시간 초과'), 5000);

try {
  await pipePromise;
  console.log('파이프 완료');
} catch (e) {
  console.error('파이프 실패:', e);
}
```

##### tee()

스트림을 두 개의 독립적인 스트림으로 분기

- 원본 스트림의 데이터가 두 분기 모두에 동일하게 전달됨

```js
const [branch1, branch2] = readableStream.tee();

// branch1과 branch2는 독립적으로 소비할 수 있다
// 원본 스트림은 잠긴 상태가 된다
const reader1 = branch1.getReader();
const reader2 = branch2.getReader();
```

##### cancel(reason?)

스트림 취소

- underlying source의 `cancel()` 메서드가 호출되고 스트림이 닫힘

```js
const stream = new ReadableStream({
  cancel(reason) {
    console.log('취소 사유:', reason);
  }
});

await stream.cancel('더 이상 데이터가 필요 없음');
```

##### locked (속성)

스트림이 Reader에 의해 잠겨 있는지 여부를 나타내는 불리언 값

```js
console.log(stream.locked); // false

const reader = stream.getReader();
console.log(stream.locked); // true

reader.releaseLock();
console.log(stream.locked); // false
```

##### values(options?)

스트림을 비동기 이터러블(async iterable)로 변환

- `for await...of` 루프에서 사용 가능

```js
const stream = new ReadableStream({
  start(controller) {
    controller.enqueue('a');
    controller.enqueue('b');
    controller.enqueue('c');
    controller.close();
  }
});

// values() 사용
for await (const chunk of stream.values()) {
  console.log(chunk); // 'a', 'b', 'c'
}

// 옵션: preventCancel
for await (const chunk of stream.values({ preventCancel: true })) {
  console.log(chunk);
  if (chunk === 'b') break; // preventCancel이 true이므로 스트림이 취소되지 않음
}

// ReadableStream 자체가 async iterable이므로 직접 사용 가능
for await (const chunk of stream) {
  console.log(chunk);
}
```

##### ReadableStream.from(asyncIterable) (정적 메서드)

비동기 이터러블 또는 이터러블을 `ReadableStream`으로 변환

```js
// 배열에서 스트림 생성
const stream1 = ReadableStream.from(['hello', 'world']);

// 제너레이터에서 스트림 생성
async function* generateNumbers() {
  for (let i = 0; i < 100; i++) {
    yield i;
    await new Promise(r => setTimeout(r, 10));
  }
}

const stream2 = ReadableStream.from(generateNumbers());

// async iterable에서 스트림 생성
const asyncIterable = {
  async *[Symbol.asyncIterator]() {
    yield 'first';
    yield 'second';
    yield 'third';
  }
};

const stream3 = ReadableStream.from(asyncIterable);
```

---

### 4. ReadableStreamDefaultReader

`ReadableStreamDefaultReader`: `ReadableStream`에서 청크를 읽기 위한 기본 리더

#### read()

스트림에서 다음 청크를 읽음

- `{ value, done }` 형태의 결과를 반환하는 Promise를 반환

```js
const reader = stream.getReader();

// 단일 읽기
const { value, done } = await reader.read();
if (!done) {
  console.log('읽은 데이터:', value);
}

// 전체 스트림 읽기 패턴
async function readAll(stream) {
  const reader = stream.getReader();
  const chunks = [];

  try {
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      chunks.push(value);
    }
    return chunks;
  } finally {
    reader.releaseLock();
  }
}
```

- `done`이 `true`이면 스트림이 닫힌 것 → 이 경우 `value`는 `undefined`

#### releaseLock()

리더와 스트림 간의 잠금을 해제

- 이후 다른 리더를 획득 가능

```js
const reader = stream.getReader();
// ... 읽기 작업 수행 ...
reader.releaseLock();

// 이제 새 리더를 획득하거나 다른 작업 가능
const newReader = stream.getReader();
```

주의: 아직 미완료된 `read()` 요청이 있는 상태에서 `releaseLock()`을 호출 → 그 `read()` Promise는 `TypeError`로 거부됨

#### cancel(reason?)

리더를 통해 기반 스트림을 취소

```js
const reader = stream.getReader();
const { value } = await reader.read();

if (value === 'ERROR_MARKER') {
  await reader.cancel('오류 마커 발견');
}
```

#### closed (속성)

스트림이 닫히거나 오류가 발생했을 때 이행/거부되는 Promise

```js
const reader = stream.getReader();

reader.closed
  .then(() => console.log('스트림이 정상적으로 닫힘'))
  .catch(err => console.error('스트림 오류:', err));

// 또는 async/await 사용
try {
  await reader.closed;
  console.log('스트림 닫힘');
} catch (err) {
  console.error('오류:', err);
}
```

#### 종합 예제

```js
async function processStream(readableStream) {
  const reader = readableStream.getReader();

  try {
    let totalSize = 0;

    // closed Promise를 별도로 감시
    reader.closed.then(() => {
      console.log(`총 처리 크기: ${totalSize} bytes`);
    });

    while (true) {
      const { value, done } = await reader.read();

      if (done) {
        console.log('스트림 소비 완료');
        break;
      }

      totalSize += value.byteLength || value.length || 0;
      await processChunk(value);
    }
  } catch (error) {
    console.error('스트림 읽기 중 오류:', error);
    await reader.cancel(error.message);
  } finally {
    reader.releaseLock();
  }
}
```

---

### 5. ReadableStreamBYOBReader

`ReadableStreamBYOBReader`: 바이트 스트림 전용 리더

- BYOB = "Bring Your Own Buffer"의 약자 → 소비자가 직접 제공한 버퍼에 데이터를 채워 넣음 → 메모리 할당 최소화 가능

#### read(view)

사용자가 제공한 `ArrayBufferView`에 데이터를 읽어 들임

```js
const byteStream = new ReadableStream({
  type: 'bytes',
  async pull(controller) {
    const data = await fetchSomeBytes();
    controller.enqueue(new Uint8Array(data));
  }
});

const reader = byteStream.getReader({ mode: 'byob' });

// 1024 바이트 버퍼를 직접 제공
const buffer = new ArrayBuffer(1024);
let view = new Uint8Array(buffer, 0, 1024);

const { value, done } = await reader.read(view);
// value는 데이터가 채워진 새 Uint8Array (원본 view의 버퍼를 전이받음)
// value.byteLength는 실제로 읽힌 바이트 수
```

핵심 개념: `read(view)` 호출 후 원본 `view`는 더 이상 유효하지 않음(내부 `ArrayBuffer`가 전이(transfer)됨) → 반환된 `value`를 사용해야 함

```js
// 반복적으로 BYOB 읽기
async function readWithBYOB(stream) {
  const reader = stream.getReader({ mode: 'byob' });
  let buffer = new ArrayBuffer(4096);
  let offset = 0;
  const chunks = [];

  try {
    while (true) {
      const view = new Uint8Array(buffer, offset, buffer.byteLength - offset);
      const { value, done } = await reader.read(view);

      if (done) break;

      chunks.push(new Uint8Array(value.buffer, 0, offset + value.byteLength));

      // value.buffer를 재사용하여 다음 읽기에 활용
      buffer = value.buffer;
      offset = 0;
    }
  } finally {
    reader.releaseLock();
  }

  return chunks;
}
```

#### releaseLock()

기본 리더와 동일하게 잠금을 해제

```js
const reader = byteStream.getReader({ mode: 'byob' });
// ... 읽기 작업 ...
reader.releaseLock();
```

#### cancel(reason?)

바이트 스트림을 취소

```js
const reader = byteStream.getReader({ mode: 'byob' });
await reader.cancel('파일 전송 중단');
```

#### closed (속성)

스트림 닫힘/오류를 감지하는 Promise

- `ReadableStreamDefaultReader.closed`와 동일하게 동작

#### BYOB vs Default Reader 비교

```js
const byteStream = new ReadableStream({
  type: 'bytes',
  pull(controller) {
    controller.enqueue(new Uint8Array([1, 2, 3, 4, 5]));
  }
});

// 방법 1: Default Reader (바이트 스트림에서도 사용 가능)
const defaultReader = byteStream.getReader();
const { value: v1 } = await defaultReader.read();
// v1은 스트림이 enqueue한 Uint8Array 그대로

// 방법 2: BYOB Reader
const byobReader = byteStream.getReader({ mode: 'byob' });
const buf = new Uint8Array(10); // 10바이트 버퍼 직접 제공
const { value: v2 } = await byobReader.read(buf);
// v2는 buf의 버퍼에 데이터가 채워진 새 Uint8Array
// 메모리 할당 없이 기존 버퍼 재사용
```

---

### 6. ReadableByteStreamController

`ReadableByteStreamController`: 바이트 스트림(`type: 'bytes'`)의 underlying source에 전달되는 컨트롤러

#### byobRequest (속성)

현재 BYOB 리더의 읽기 요청을 나타내는 `ReadableStreamBYOBRequest` 객체

- BYOB 리더가 `read(view)`를 호출했을 때만 존재 · 그렇지 않으면 `null`

```js
const stream = new ReadableStream({
  type: 'bytes',

  async pull(controller) {
    if (controller.byobRequest) {
      // BYOB 리더가 사용 중 - 소비자의 버퍼에 직접 쓴다
      const view = controller.byobRequest.view;
      const bytesRead = await readIntoBuffer(view);
      controller.byobRequest.respond(bytesRead);
    } else {
      // 기본 리더가 사용 중 - 새 버퍼를 생성하여 enqueue
      const data = await fetchBytes(1024);
      controller.enqueue(new Uint8Array(data));
    }
  }
});
```

##### byobRequest.view

소비자가 제공한 버퍼의 뷰(view)

- 이 뷰에 데이터를 직접 쓸 수 있음

##### byobRequest.respond(bytesWritten)

뷰에 `bytesWritten` 바이트만큼 데이터를 채웠음을 알림

```js
async pull(controller) {
  const request = controller.byobRequest;
  if (request) {
    const view = request.view;
    // view 버퍼에 직접 데이터 복사
    const src = await getNextBytes(view.byteLength);
    new Uint8Array(view.buffer, view.byteOffset, src.length).set(src);
    request.respond(src.length);
  }
}
```

##### byobRequest.respondWithNewView(view)

완전히 새로운 뷰로 응답

- 원본 버퍼와 다른 버퍼를 사용해야 할 때 유용

#### desiredSize (속성)

내부 큐를 `highWaterMark`까지 채우는 데 필요한 크기

- 배압 판단에 사용

```js
pull(controller) {
  console.log('desiredSize:', controller.desiredSize);
  // 양수: 큐에 여유 있음
  // 0: 큐가 가득 참
  // 음수: 큐 초과 상태 (오버플로우)
  // null: 스트림이 닫혔거나 오류 상태
}
```

#### close()

스트림을 닫음

- 큐에 남은 청크가 모두 소비된 후 스트림이 종료됨

```js
pull(controller) {
  if (noMoreData) {
    controller.close();
  }
}
```

#### enqueue(chunk)

`Uint8Array` 등의 `ArrayBufferView`를 큐에 추가

- 바이트 스트림이므로 반드시 바이트 타입이어야 함

```js
pull(controller) {
  const data = new Uint8Array(1024);
  fillWithData(data);
  controller.enqueue(data);
}
```

#### error(reason)

스트림을 오류 상태로 만듦

- 이후 모든 읽기 시도는 거부됨

```js
pull(controller) {
  try {
    const data = readFromHardware();
    controller.enqueue(data);
  } catch (e) {
    controller.error(new Error('하드웨어 읽기 실패: ' + e.message));
  }
}
```

---

### 7. ReadableStreamDefaultController

`ReadableStreamDefaultController`: 기본(비-바이트) 스트림의 컨트롤러

#### desiredSize (속성)

```js
const stream = new ReadableStream({
  pull(controller) {
    // highWaterMark가 5이고 큐에 3개 청크가 있으면 desiredSize는 2
    console.log('desiredSize:', controller.desiredSize);

    if (controller.desiredSize <= 0) {
      // 큐가 가득 참 - 새 데이터를 넣지 않아도 됨
      return;
    }

    controller.enqueue(getNextChunk());
  }
}, { highWaterMark: 5 });
```

#### close()

스트림을 정상적으로 닫음

```js
const stream = new ReadableStream({
  start(controller) {
    controller.enqueue('데이터 1');
    controller.enqueue('데이터 2');
    controller.close(); // 더 이상 enqueue 불가
  }
});
```

#### enqueue(chunk)

청크를 내부 큐에 추가

- 어떤 타입의 값이든 가능

```js
const stream = new ReadableStream({
  start(controller) {
    controller.enqueue('문자열');
    controller.enqueue(42);
    controller.enqueue({ key: 'value' });
    controller.enqueue(new Uint8Array([1, 2, 3]));
    controller.close();
  }
});
```

#### error(reason)

스트림을 오류 상태로 전환

```js
const stream = new ReadableStream({
  start(controller) {
    controller.enqueue('정상 데이터');

    // 오류 발생 시
    controller.error(new Error('데이터 소스 장애'));
    // 이후 enqueue(), close() 호출 불가
  }
});

const reader = stream.getReader();
const { value } = await reader.read();    // '정상 데이터'
try {
  await reader.read();                     // Error: 데이터 소스 장애
} catch (e) {
  console.error(e);
}
```

---

### 8. WritableStream

`WritableStream`: 데이터를 쓸 수 있는 싱크(sink)

#### 생성자

```js
new WritableStream(underlyingSink?, queuingStrategy?)
```

#### underlyingSink 객체

- `start(controller)`: 스트림 생성 시 호출 · 초기화 로직
- `write(chunk, controller)`: 청크가 쓰일 때마다 호출
- `close()`: 스트림이 닫힐 때 호출 · 마무리 작업
- `abort(reason)`: 스트림이 중단될 때 호출 · 리소스 정리

##### start(controller)

```js
const ws = new WritableStream({
  async start(controller) {
    this.connection = await openDatabaseConnection();
    this.buffer = [];
    console.log('WritableStream 초기화 완료');
  }
});
```

##### write(chunk, controller)

- 각 청크가 기록될 때 호출
- Promise 반환 시 해당 Promise가 이행될 때까지 다음 write 대기 → WritableStream의 배압 메커니즘

```js
const ws = new WritableStream({
  async write(chunk, controller) {
    // 느린 I/O 작업 시뮬레이션
    await new Promise(resolve => setTimeout(resolve, 100));
    console.log('기록됨:', chunk);
  }
});
```

##### close()

모든 쓰기 완료 후 스트림을 닫을 때 호출

```js
const ws = new WritableStream({
  start() {
    this.data = [];
  },
  write(chunk) {
    this.data.push(chunk);
  },
  close() {
    // 모아둔 데이터를 한 번에 저장
    localStorage.setItem('stream-data', JSON.stringify(this.data));
    console.log(`${this.data.length}개 청크 저장 완료`);
  }
});
```

##### abort(reason)

- 스트림이 비정상적으로 중단될 때 호출
- `close()`와 달리 불완전한 상태에서의 정리 목적

```js
const ws = new WritableStream({
  start() {
    this.tempFile = openTempFile();
  },
  write(chunk) {
    writeTo(this.tempFile, chunk);
  },
  close() {
    renameTempToFinal(this.tempFile);
  },
  abort(reason) {
    // 임시 파일 삭제 등 정리 작업
    deleteTempFile(this.tempFile);
    console.log('중단 사유:', reason);
  }
});
```

#### queuingStrategy

```js
const ws = new WritableStream(
  {
    write(chunk) {
      console.log('기록:', chunk);
    }
  },
  new CountQueuingStrategy({ highWaterMark: 5 })
  // 또는
  // { highWaterMark: 1024, size(chunk) { return chunk.byteLength; } }
);
```

#### getWriter()

스트림에 데이터를 쓰기 위한 Writer 획득

```js
const writer = ws.getWriter();
// ws.locked === true

await writer.write('첫 번째 데이터');
await writer.write('두 번째 데이터');
await writer.close();
```

#### locked (속성)

Writer에 의해 잠겨 있는지 여부

```js
console.log(ws.locked); // false
const writer = ws.getWriter();
console.log(ws.locked); // true
writer.releaseLock();
console.log(ws.locked); // false
```

#### abort(reason?)

스트림 중단

```js
await ws.abort('사용자가 업로드 취소');
```

#### close()

스트림을 정상적으로 닫음

```js
await ws.close();
```

#### 종합 예제

```js
// 콘솔 로깅 WritableStream
function createLoggingStream(prefix) {
  let count = 0;

  return new WritableStream({
    start() {
      console.log(`[${prefix}] 스트림 시작`);
    },

    write(chunk) {
      count++;
      console.log(`[${prefix}] #${count}: ${chunk}`);
    },

    close() {
      console.log(`[${prefix}] 스트림 종료 (총 ${count}개 청크)`);
    },

    abort(reason) {
      console.error(`[${prefix}] 스트림 중단: ${reason}`);
    }
  });
}

const logStream = createLoggingStream('APP');
const writer = logStream.getWriter();

await writer.write('서버 시작');
await writer.write('요청 수신');
await writer.write('응답 전송');
await writer.close();
```

---

### 9. WritableStreamDefaultWriter

`WritableStreamDefaultWriter`: `WritableStream`에 데이터를 쓰기 위한 인터페이스

#### write(chunk)

- 스트림에 청크를 씀
- 청크가 내부 큐에 성공적으로 추가되거나 underlying sink에 기록되면 이행되는 Promise 반환

```js
const writer = writableStream.getWriter();

await writer.write('Hello');
await writer.write('World');

// 순차적 쓰기 대신 write() Promise를 바로 반환받는 것도 가능
// (배압 관리가 필요 없는 경우)
writer.write('chunk1');
writer.write('chunk2');
writer.write('chunk3');
await writer.close();
```

#### close()

- 스트림을 정상적으로 닫음
- 큐에 있는 모든 청크 처리 후 underlying sink의 `close()` 호출

```js
const writer = writableStream.getWriter();
await writer.write('마지막 데이터');
await writer.close();
// 이후 write() 호출은 오류를 발생시킨다
```

#### abort(reason?)

- 스트림을 즉시 중단
- 큐에 남은 청크는 버려지고 underlying sink의 `abort()` 호출

```js
const writer = writableStream.getWriter();
writer.write('데이터 1');
writer.write('데이터 2'); // 이 데이터는 처리되지 않을 수 있다

await writer.abort('긴급 중단');
```

#### releaseLock()

Writer의 잠금 해제

```js
const writer = writableStream.getWriter();
await writer.write('데이터');
writer.releaseLock();

// 새 Writer 획득 가능
const newWriter = writableStream.getWriter();
```

#### ready (속성)

- 배압이 해제되어 쓰기가 가능해지면 이행되는 Promise
- 효율적인 배압 관리의 핵심

```js
const writer = writableStream.getWriter();

async function writeChunks(chunks) {
  for (const chunk of chunks) {
    // ready가 이행될 때까지 대기 - 배압 존중
    await writer.ready;
    writer.write(chunk); // await 없이 호출하여 성능 최적화
  }
  await writer.ready; // 마지막 쓰기가 큐에 들어갈 때까지 대기
  await writer.close();
}
```

- `ready`는 중요한 패턴
- `write()`를 await하지 않고 `ready`만 await → 배압을 존중하면서 불필요한 대기 축소

#### closed (속성)

스트림이 닫히거나 오류가 발생하면 이행/거부되는 Promise

```js
const writer = writableStream.getWriter();

writer.closed
  .then(() => console.log('스트림 정상 닫힘'))
  .catch(err => console.error('스트림 오류:', err));
```

#### desiredSize (속성)

내부 큐를 `highWaterMark`까지 채우기 위해 필요한 크기

```js
const writer = writableStream.getWriter();

console.log(writer.desiredSize); // highWaterMark (초기 상태)

await writer.write('데이터');
console.log(writer.desiredSize); // 줄어든 값

// desiredSize가 0 이하이면 큐가 가득 찬 것
// writer.ready가 이행될 때까지 기다려야 한다
```

#### 종합 예제: 대량 데이터 쓰기

```js
async function writeWithBackpressure(writableStream, dataSource) {
  const writer = writableStream.getWriter();

  try {
    for await (const chunk of dataSource) {
      // 배압을 존중하며 쓰기
      if (writer.desiredSize <= 0) {
        await writer.ready;
      }
      writer.write(chunk);
    }

    // 모든 쓰기가 완료될 때까지 대기
    await writer.ready;
    await writer.close();
    console.log('모든 데이터 기록 완료');
  } catch (error) {
    console.error('쓰기 오류:', error);
    await writer.abort(error.message);
  }
}
```

---

### 10. WritableStreamDefaultController

`WritableStreamDefaultController`: underlying sink에 전달되는 컨트롤러 · `ReadableStreamDefaultController`보다 단순

#### signal (속성)

- `AbortSignal` 인스턴스
- 스트림이 중단(abort)되면 이 signal 발동
- 비동기 쓰기 작업을 취소하는 데 사용 가능

```js
const ws = new WritableStream({
  async write(chunk, controller) {
    // AbortSignal을 fetch에 전달하여
    // 스트림 중단 시 네트워크 요청도 취소되게 한다
    await fetch('/api/data', {
      method: 'POST',
      body: chunk,
      signal: controller.signal
    });
  }
});

// 스트림 중단 시 진행 중인 fetch도 함께 취소된다
const writer = ws.getWriter();
writer.write(someData);
await writer.abort('사용자 취소');
```

```js
const ws = new WritableStream({
  async write(chunk, controller) {
    // signal의 상태를 직접 확인할 수도 있다
    if (controller.signal.aborted) {
      return; // 이미 중단된 상태
    }

    // signal에 이벤트 리스너 등록
    return new Promise((resolve, reject) => {
      controller.signal.addEventListener('abort', () => {
        reject(new Error('쓰기 중단됨'));
      });

      performLongOperation(chunk).then(resolve, reject);
    });
  }
});
```

#### error(reason)

- 스트림을 오류 상태로 전환
- 이후 모든 쓰기 거부

```js
const ws = new WritableStream({
  write(chunk, controller) {
    if (!isValidChunk(chunk)) {
      controller.error(new TypeError('잘못된 청크 형식'));
      return;
    }
    processChunk(chunk);
  }
});
```

```js
// 쓰기 횟수 제한 예제
const ws = new WritableStream({
  start() {
    this.writeCount = 0;
    this.maxWrites = 100;
  },

  write(chunk, controller) {
    this.writeCount++;
    if (this.writeCount > this.maxWrites) {
      controller.error(new Error(`최대 쓰기 횟수(${this.maxWrites}) 초과`));
      return;
    }
    saveChunk(chunk);
  }
});
```

---

### 11. TransformStream

`TransformStream`: 데이터를 변환하는 중간 스트림 · 읽기 가능 면(`readable`)과 쓰기 가능 면(`writable`)을 가진 듀플렉스(duplex) 스트림

#### 생성자

```js
new TransformStream(transformer?, writableStrategy?, readableStrategy?)
```

#### transformer 객체

- `start(controller)`: 초기화 시 호출
- `transform(chunk, controller)`: 각 청크 변환
- `flush(controller)`: 쓰기 면이 닫힐 때 호출 · 잔여 데이터 처리
- `cancel(reason)`: 읽기 면이 취소될 때 호출

##### start(controller)

```js
const ts = new TransformStream({
  start(controller) {
    // 초기 상태 설정
    this.count = 0;
    this.buffer = '';
  }
});
```

##### transform(chunk, controller)

- 입력 청크를 변환하여 출력 큐에 넣는 핵심 메서드
- Promise 반환 가능

```js
// 문자열을 대문자로 변환
const upperCaseTransform = new TransformStream({
  transform(chunk, controller) {
    controller.enqueue(chunk.toUpperCase());
  }
});

// 하나의 입력 청크에서 여러 출력 청크 생성 가능
const splitTransform = new TransformStream({
  transform(chunk, controller) {
    const lines = chunk.split('\n');
    for (const line of lines) {
      if (line.trim()) {
        controller.enqueue(line.trim());
      }
    }
  }
});

// 필터링: 일부 청크를 건너뛰기 (enqueue하지 않으면 됨)
const filterTransform = new TransformStream({
  transform(chunk, controller) {
    if (chunk.length > 0) {  // 빈 문자열 필터링
      controller.enqueue(chunk);
    }
    // enqueue를 호출하지 않으면 해당 청크는 버려진다
  }
});
```

##### flush(controller)

- 쓰기 면에 대한 모든 쓰기가 완료되고 스트림이 닫힐 때 호출
- 버퍼에 남은 데이터 처리에 사용

```js
// 줄 단위 파서 (불완전한 줄을 버퍼링)
const lineTransform = new TransformStream({
  start() {
    this.buffer = '';
  },

  transform(chunk, controller) {
    this.buffer += chunk;
    const lines = this.buffer.split('\n');
    // 마지막 요소는 불완전한 줄일 수 있으므로 버퍼에 보관
    this.buffer = lines.pop();
    for (const line of lines) {
      controller.enqueue(line);
    }
  },

  flush(controller) {
    // 버퍼에 남은 마지막 줄 처리
    if (this.buffer.length > 0) {
      controller.enqueue(this.buffer);
    }
  }
});
```

##### cancel(reason)

- 읽기 면이 취소될 때 호출
- 변환에 사용되는 리소스 정리 가능

```js
const ts = new TransformStream({
  start() {
    this.decoder = new SomeDecoder();
  },

  transform(chunk, controller) {
    controller.enqueue(this.decoder.decode(chunk));
  },

  cancel(reason) {
    this.decoder.dispose();
    console.log('변환 취소:', reason);
  }
});
```

#### readable (속성)

변환된 데이터를 읽을 수 있는 `ReadableStream`

#### writable (속성)

변환할 데이터를 쓸 수 있는 `WritableStream`

```js
const { readable, writable } = new TransformStream({
  transform(chunk, controller) {
    controller.enqueue(chunk * 2);
  }
});

// writable에 쓰면 transform을 거쳐 readable에서 읽힌다
const writer = writable.getWriter();
const reader = readable.getReader();

writer.write(5);
const { value } = await reader.read(); // value === 10
```

#### 다양한 TransformStream 예제

```js
// JSON 파서 TransformStream
const jsonParseTransform = new TransformStream({
  transform(chunk, controller) {
    try {
      controller.enqueue(JSON.parse(chunk));
    } catch (e) {
      controller.error(new Error(`JSON 파싱 실패: ${e.message}`));
    }
  }
});

// 지연(throttle) TransformStream
function createThrottleTransform(ms) {
  return new TransformStream({
    async transform(chunk, controller) {
      await new Promise(resolve => setTimeout(resolve, ms));
      controller.enqueue(chunk);
    }
  });
}

// 배치(batch) TransformStream
function createBatchTransform(batchSize) {
  return new TransformStream({
    start() {
      this.batch = [];
    },
    transform(chunk, controller) {
      this.batch.push(chunk);
      if (this.batch.length >= batchSize) {
        controller.enqueue(this.batch);
        this.batch = [];
      }
    },
    flush(controller) {
      if (this.batch.length > 0) {
        controller.enqueue(this.batch);
      }
    }
  });
}

// 사용 예
const batchedStream = inputStream
  .pipeThrough(createBatchTransform(10))
  .pipeThrough(new TransformStream({
    async transform(batch, controller) {
      // 10개씩 모아서 일괄 처리
      const results = await processBatch(batch);
      for (const result of results) {
        controller.enqueue(result);
      }
    }
  }));
```

#### identity TransformStream

- transformer 미전달 시 입력을 그대로 통과시키는 identity TransformStream 생성
- ReadableStream과 WritableStream을 연결하는 브릿지로 유용

```js
const { readable, writable } = new TransformStream();
// 입력이 그대로 출력으로 전달됨

// 활용: ReadableStream을 직접 제어 가능한 형태로 변환
const writer = writable.getWriter();
writer.write('manual data 1');
writer.write('manual data 2');
writer.close();

for await (const chunk of readable) {
  console.log(chunk); // 'manual data 1', 'manual data 2'
}
```

---

### 12. TransformStreamDefaultController

`TransformStreamDefaultController`: transformer 객체의 메서드에 전달되는 컨트롤러

#### desiredSize (속성)

- 읽기 면(readable side)의 내부 큐에 대한 desiredSize
- 변환 출력의 배압 판단에 사용 가능

```js
const ts = new TransformStream({
  transform(chunk, controller) {
    console.log('출력 큐 desiredSize:', controller.desiredSize);

    // desiredSize가 음수이면 출력 큐가 포화 상태
    if (controller.desiredSize < 0) {
      console.warn('출력 큐 포화 - 소비 속도가 느림');
    }

    controller.enqueue(transformData(chunk));
  }
});
```

#### enqueue(chunk)

변환된 청크를 읽기 면의 큐에 추가

```js
const ts = new TransformStream({
  transform(chunk, controller) {
    // 하나의 입력에서 여러 출력
    controller.enqueue(`[시작] ${chunk}`);
    controller.enqueue(`[내용] ${chunk.toUpperCase()}`);
    controller.enqueue(`[끝] ${chunk}`);
  }
});
```

#### error(reason)

읽기 면과 쓰기 면 모두를 오류 상태로 전환

```js
const ts = new TransformStream({
  transform(chunk, controller) {
    if (typeof chunk !== 'string') {
      controller.error(new TypeError('문자열 청크만 허용됩니다'));
      return;
    }
    controller.enqueue(chunk.trim());
  }
});
```

#### terminate()

- 읽기 면을 닫고 쓰기 면을 오류 상태로 전환
- 변환을 조기에 종료해야 할 때 사용

```js
// 특정 조건에서 스트림 조기 종료
const ts = new TransformStream({
  transform(chunk, controller) {
    if (chunk === 'END_OF_DATA') {
      controller.terminate(); // 더 이상의 변환 중단
      return;
    }
    controller.enqueue(processChunk(chunk));
  }
});
```

```js
// 최대 N개 청크만 통과시키는 TransformStream
function createTakeTransform(n) {
  let count = 0;
  return new TransformStream({
    transform(chunk, controller) {
      if (count < n) {
        controller.enqueue(chunk);
        count++;
      }
      if (count >= n) {
        controller.terminate();
      }
    }
  });
}

// 처음 5개 청크만 가져오기
const first5 = inputStream.pipeThrough(createTakeTransform(5));
```

---

### 13. 큐잉 전략 (Queuing Strategies)

- 큐잉 전략: 내부 큐의 배압 경계 정의
- 생산자에게 "잠깐 멈춰라" 신호를 보낼 시점 결정

#### CountQueuingStrategy

청크의 개수를 기준으로 큐 크기 관리

```js
const strategy = new CountQueuingStrategy({ highWaterMark: 10 });
// 큐에 10개 이상의 청크가 쌓이면 배압 발생

const stream = new ReadableStream({
  pull(controller) {
    controller.enqueue(getNextItem());
  }
}, strategy);
```

내부적으로 `size()` 함수는 항상 `1` 반환

```js
const strategy = new CountQueuingStrategy({ highWaterMark: 10 });
console.log(strategy.size('아무 값')); // 1
console.log(strategy.highWaterMark);   // 10
```

#### ByteLengthQueuingStrategy

- 청크의 바이트 크기를 기준으로 큐 크기 관리
- `byteLength` 속성이 있는 청크(ArrayBuffer·TypedArray·DataView 등)에 적합

```js
const strategy = new ByteLengthQueuingStrategy({ highWaterMark: 1024 * 16 }); // 16KB
// 큐에 16KB 이상의 데이터가 쌓이면 배압 발생

const stream = new ReadableStream({
  pull(controller) {
    const chunk = new Uint8Array(1024);
    fillWithData(chunk);
    controller.enqueue(chunk);
  }
}, strategy);
```

내부적으로 `size(chunk)` 함수는 `chunk.byteLength` 반환

```js
const strategy = new ByteLengthQueuingStrategy({ highWaterMark: 65536 });
console.log(strategy.size(new Uint8Array(1024)));  // 1024
console.log(strategy.size(new ArrayBuffer(4096))); // 4096
```

#### 커스텀 큐잉 전략

- 내장 전략이 부족할 때 직접 전략 정의 가능
- `highWaterMark`와 `size()` 함수 직접 지정

```js
// 문자열 길이 기반 큐잉 전략
const stringLengthStrategy = {
  highWaterMark: 10000, // 총 문자 수 기준 10,000자
  size(chunk) {
    return chunk.length; // 문자열 길이를 크기로 사용
  }
};

const stream = new ReadableStream({
  pull(controller) {
    controller.enqueue(getNextString());
  }
}, stringLengthStrategy);
```

```js
// JSON 객체의 직렬화 크기 기반 큐잉 전략
const jsonSizeStrategy = {
  highWaterMark: 1024 * 1024, // 1MB
  size(chunk) {
    return JSON.stringify(chunk).length;
  }
};

const objectStream = new ReadableStream({
  pull(controller) {
    controller.enqueue({ id: 1, data: 'some data', timestamp: Date.now() });
  }
}, jsonSizeStrategy);
```

```js
// 가중치 기반 큐잉 전략
const priorityStrategy = {
  highWaterMark: 100,
  size(chunk) {
    // 우선순위가 높은 청크는 큰 크기를 부여하여 큐를 빠르게 채움
    switch (chunk.priority) {
      case 'high':   return 10;
      case 'medium': return 5;
      case 'low':    return 1;
      default:       return 1;
    }
  }
};
```

#### 기본 큐잉 전략

큐잉 전략 미명시 시 기본값 적용

- ReadableStream(기본)
  - 기본 highWaterMark: 1
  - 기본 size(): `() => 1`
- ReadableStream(바이트)
  - 기본 highWaterMark: 0
  - 기본 size(): N/A(바이트 단위 관리)
- WritableStream
  - 기본 highWaterMark: 1
  - 기본 size(): `() => 1`
- TransformStream readable
  - 기본 highWaterMark: 0
  - 기본 size(): `() => 1`
- TransformStream writable
  - 기본 highWaterMark: 1
  - 기본 size(): `() => 1`

---

### 14. 배압 (Backpressure) 메커니즘

#### 배압의 작동 원리

- 배압: 스트림 시스템에서 자동으로 작동하는 흐름 제어 메커니즘
- 핵심 구성 요소는 다음과 같음

```
                    highWaterMark = 3
                         ↓
내부 큐: [ ][ ][ ][ ][ ][ ]
          ↑              ↑
        비어있음      가득 참

desiredSize = highWaterMark - queueSize
```

- desiredSize 값별 의미와 동작
  - 양수: 큐에 여유 있음 → 생산자에게 pull() 호출
  - 0: 큐가 정확히 가득 참 → pull() 호출 중단
  - 음수: 큐 오버플로우 → pull() 호출 중단 + 추가 배압
  - null: 스트림 닫힘/오류 → 더 이상 pull() 없음

#### desiredSize를 이용한 배압

```js
const readable = new ReadableStream({
  pull(controller) {
    console.log(`desiredSize: ${controller.desiredSize}`);

    // desiredSize가 양수인 동안만 데이터를 생성
    if (controller.desiredSize > 0) {
      const data = generateData();
      controller.enqueue(data);
    }
  }
}, { highWaterMark: 5 });
```

#### 파이프 체인에서의 배압 전파

배압은 파이프 체인을 따라 하류(downstream)에서 상류(upstream)로 전파

```
ReadableStream → TransformStream → WritableStream
    ↑ pull()         ↑                 ↑ (느린 I/O)
    |                |                 |
    ← 배압 전파 ←── 배압 전파 ←──── 배압 시작점
```

```js
// 배압 전파 시연
const source = new ReadableStream({
  pull(controller) {
    const timestamp = Date.now();
    console.log(`[소스] 데이터 생성 at ${timestamp}`);
    controller.enqueue({ data: 'chunk', timestamp });
  }
}, { highWaterMark: 2 });

const transform = new TransformStream({
  transform(chunk, controller) {
    console.log('[변환] 청크 변환');
    controller.enqueue({ ...chunk, transformed: true });
  }
});

const sink = new WritableStream({
  async write(chunk) {
    console.log('[싱크] 청크 기록 시작');
    // 느린 쓰기 시뮬레이션 - 이것이 배압의 원인
    await new Promise(resolve => setTimeout(resolve, 1000));
    console.log('[싱크] 청크 기록 완료');
  }
}, { highWaterMark: 1 });

// 파이프 연결 - 배압이 자동으로 전파됨
await source.pipeThrough(transform).pipeTo(sink);
```

#### WritableStream에서의 배압

`WritableStreamDefaultWriter`의 `ready` Promise와 `desiredSize`로 배압 확인 가능

```js
const ws = new WritableStream({
  async write(chunk) {
    await slowOperation(chunk);
  }
}, { highWaterMark: 3 });

const writer = ws.getWriter();

// 배압을 존중하는 쓰기 패턴
async function writeWithBackpressure(chunks) {
  for (const chunk of chunks) {
    // ready가 이행될 때까지 대기하여 배압 존중
    await writer.ready;
    console.log(`desiredSize: ${writer.desiredSize}`);
    writer.write(chunk); // await 없이 호출
  }
  await writer.ready;
  await writer.close();
}
```

#### 수동 배압 관리

일부 시나리오에서는 배압을 수동으로 관리해야 함

```js
// 외부 이벤트 소스를 ReadableStream으로 감쌀 때의 배압 관리
function fromEventSource(eventSource) {
  let paused = false;
  const pendingChunks = [];

  return new ReadableStream({
    start(controller) {
      eventSource.onmessage = (event) => {
        if (controller.desiredSize <= 0) {
          // 배압이 걸림 - 이벤트 소스 일시 정지가 불가능하면 버퍼링
          paused = true;
          pendingChunks.push(event.data);
          console.warn('배압 발생 - 버퍼링 중');
          return;
        }
        controller.enqueue(event.data);
      };

      eventSource.onerror = (error) => {
        controller.error(error);
      };
    },

    pull(controller) {
      // 배압 해제 - 버퍼링된 데이터 전달
      while (pendingChunks.length > 0 && controller.desiredSize > 0) {
        controller.enqueue(pendingChunks.shift());
      }
      if (pendingChunks.length === 0) {
        paused = false;
      }
    },

    cancel() {
      eventSource.close();
    }
  });
}
```

---

### 15. 파이프 (Piping)

#### pipeTo()

`ReadableStream`의 데이터를 `WritableStream`으로 전송하는 메서드

- 모든 데이터 전송 완료 시 이행되는 Promise 반환

```js
const readable = new ReadableStream({
  start(controller) {
    controller.enqueue('Hello');
    controller.enqueue('World');
    controller.close();
  }
});

const writable = new WritableStream({
  write(chunk) {
    console.log('받음:', chunk);
  }
});

await readable.pipeTo(writable);
// 출력:
// 받음: Hello
// 받음: World
```

##### pipeTo 옵션

```js
const abortController = new AbortController();

try {
  await readable.pipeTo(writable, {
    // 소스 스트림이 닫힐 때 싱크도 닫을지 여부
    preventClose: false,

    // 소스 스트림에서 오류 발생 시 싱크를 abort할지 여부
    preventAbort: false,

    // 싱크에서 오류 발생 시 소스를 cancel할지 여부
    preventCancel: false,

    // AbortSignal - 외부에서 파이프를 취소할 수 있음
    signal: abortController.signal
  });
} catch (e) {
  if (e.name === 'AbortError') {
    console.log('파이프가 외부에서 취소됨');
  }
}

// 5초 후 파이프 취소
setTimeout(() => abortController.abort(), 5000);
```

##### preventClose, preventAbort, preventCancel 상세

```js
// preventClose: true - 소스가 닫혀도 싱크를 닫지 않음
// 여러 소스를 순차적으로 하나의 싱크에 파이프할 때 유용
const writable = new WritableStream({
  write(chunk) { console.log(chunk); },
  close() { console.log('싱크 닫힘'); }
});

const source1 = ReadableStream.from(['a', 'b']);
const source2 = ReadableStream.from(['c', 'd']);

await source1.pipeTo(writable, { preventClose: true });
// 싱크는 아직 열려 있음
await source2.pipeTo(writable);
// 이제 싱크가 닫힘
```

#### pipeThrough()

`ReadableStream`의 데이터를 `TransformStream`에 통과시켜 변환

- 변환 결과를 새 `ReadableStream`으로 반환

```js
const readable = new ReadableStream({
  start(controller) {
    controller.enqueue('hello world');
    controller.enqueue('foo bar');
    controller.close();
  }
});

const result = readable.pipeThrough(new TransformStream({
  transform(chunk, controller) {
    controller.enqueue(chunk.toUpperCase());
  }
}));

// result는 ReadableStream
const reader = result.getReader();
console.log(await reader.read()); // { value: 'HELLO WORLD', done: false }
console.log(await reader.read()); // { value: 'FOO BAR', done: false }
console.log(await reader.read()); // { value: undefined, done: true }
```

#### 파이프 체인 구성

- 여러 `pipeThrough()`를 연쇄해 복잡한 변환 파이프라인 구성 가능

```js
// 다단계 파이프라인: 바이트 → 텍스트 → 줄 분리 → 필터링 → JSON 파싱
const processedStream = fetchResponse.body
  .pipeThrough(new TextDecoderStream())          // Uint8Array → 문자열
  .pipeThrough(createLineSplitTransform())        // 문자열 → 개별 줄
  .pipeThrough(createFilterTransform(line =>      // 빈 줄 필터링
    line.trim().length > 0
  ))
  .pipeThrough(createJsonParseTransform());       // 문자열 → JSON 객체

// 결과 소비
for await (const obj of processedStream) {
  console.log('파싱된 객체:', obj);
}
```

```js
// 유틸리티 함수들
function createLineSplitTransform() {
  let buffer = '';
  return new TransformStream({
    transform(chunk, controller) {
      buffer += chunk;
      const lines = buffer.split('\n');
      buffer = lines.pop(); // 마지막 불완전한 줄은 버퍼에 유지
      for (const line of lines) {
        controller.enqueue(line);
      }
    },
    flush(controller) {
      if (buffer) controller.enqueue(buffer);
    }
  });
}

function createFilterTransform(predicate) {
  return new TransformStream({
    transform(chunk, controller) {
      if (predicate(chunk)) {
        controller.enqueue(chunk);
      }
    }
  });
}

function createJsonParseTransform() {
  return new TransformStream({
    transform(chunk, controller) {
      try {
        controller.enqueue(JSON.parse(chunk));
      } catch {
        // 파싱 실패한 줄은 무시
      }
    }
  });
}
```

#### 내장 TransformStream

브라우저 제공 내장 TransformStream

- TextDecoderStream · TextEncoderStream · CompressionStream · DecompressionStream 등

```js
// TextDecoderStream: 바이트 → 문자열
const textStream = byteStream.pipeThrough(new TextDecoderStream('utf-8'));

// TextEncoderStream: 문자열 → 바이트
const byteStream2 = textStream.pipeThrough(new TextEncoderStream());

// CompressionStream: 압축
const compressed = readableStream.pipeThrough(new CompressionStream('gzip'));

// DecompressionStream: 압축 해제
const decompressed = compressed.pipeThrough(new DecompressionStream('gzip'));
```

---

### 16. Tee (분기)

#### ReadableStream.tee()

`tee()`: 하나의 `ReadableStream`을 두 개의 동일한 스트림으로 복제

- 같은 데이터를 두 가지 다른 방식으로 처리해야 할 때 유용

```js
const [stream1, stream2] = originalStream.tee();
```

이름의 유래: 배관에서 사용하는 T자형 연결관(tee fitting)

- 하나의 파이프가 두 갈래로 나뉘는 것과 동일한 개념

#### 기본 사용

```js
const original = new ReadableStream({
  start(controller) {
    controller.enqueue(1);
    controller.enqueue(2);
    controller.enqueue(3);
    controller.close();
  }
});

const [branch1, branch2] = original.tee();

// 두 브랜치에서 동일한 데이터를 독립적으로 읽을 수 있다
const reader1 = branch1.getReader();
const reader2 = branch2.getReader();

console.log(await reader1.read()); // { value: 1, done: false }
console.log(await reader2.read()); // { value: 1, done: false }
console.log(await reader1.read()); // { value: 2, done: false }
// reader2는 아직 두 번째 청크를 읽지 않았지만 독립적으로 유지됨
```

#### 실용적 tee 사용 사례

##### 캐시와 처리를 동시에

```js
async function fetchAndCacheAndProcess(url) {
  const response = await fetch(url);
  const [forCache, forProcessing] = response.body.tee();

  // 브랜치 1: 캐시에 저장
  const cachePromise = caches.open('my-cache').then(cache => {
    const cachedResponse = new Response(forCache, {
      headers: response.headers
    });
    return cache.put(url, cachedResponse);
  });

  // 브랜치 2: 즉시 처리
  const processPromise = processStream(forProcessing);

  await Promise.all([cachePromise, processPromise]);
}
```

##### 로깅과 실제 처리 동시 수행

```js
const [logBranch, processBranch] = dataStream.tee();

// 브랜치 1: 로깅
logBranch.pipeTo(new WritableStream({
  write(chunk) {
    console.log(`[LOG] 청크 크기: ${JSON.stringify(chunk).length}`);
  }
}));

// 브랜치 2: 실제 비즈니스 로직
processBranch
  .pipeThrough(transformStream)
  .pipeTo(outputStream);
```

##### 여러 갈래로 분기

- `tee()`는 두 갈래만 지원 → 재귀적으로 사용하면 여러 갈래로 분기 가능

```js
function teeN(stream, n) {
  if (n <= 1) return [stream];
  if (n === 2) return stream.tee();

  const [first, rest] = stream.tee();
  return [first, ...teeN(rest, n - 1)];
}

// 4갈래 분기
const [s1, s2, s3, s4] = teeN(originalStream, 4);
```

#### tee의 배압 특성

- `tee()`로 생성된 두 브랜치는 독립적인 배압 보유
- 한 브랜치의 소비가 느리면 원본 스트림 전체가 느려짐 (핵심 특징)
- 내부적으로 두 브랜치 모두에 데이터가 전달되어야 함 → 가장 느린 브랜치가 전체 속도 결정

```js
const [fast, slow] = dataStream.tee();

// fast 브랜치는 빠르게 소비하지만
fast.pipeTo(fastSink);

// slow 브랜치가 느리면 전체가 느려진다
slow.pipeTo(new WritableStream({
  async write(chunk) {
    await new Promise(r => setTimeout(r, 1000)); // 1초 딜레이
  }
}));
// fast 브랜치도 slow 브랜치의 속도에 영향을 받는다
```

---

### 17. 바이트 스트림 (Byte Streams)

- 바이트 스트림: 바이너리 데이터를 효율적으로 처리하기 위한 특수한 `ReadableStream`
- `type: 'bytes'`를 지정하여 생성

#### Underlying Byte Source

```js
const byteStream = new ReadableStream({
  type: 'bytes',

  start(controller) {
    // controller는 ReadableByteStreamController
    console.log('바이트 스트림 시작');
  },

  async pull(controller) {
    // BYOB 리더의 요청이 있으면 byobRequest를 통해 처리
    if (controller.byobRequest) {
      const view = controller.byobRequest.view;
      const bytesRead = await readFromSource(view.buffer, view.byteOffset, view.byteLength);

      if (bytesRead === 0) {
        controller.close();
        controller.byobRequest.respond(0);
      } else {
        controller.byobRequest.respond(bytesRead);
      }
    } else {
      // 기본 리더 사용 시 - 새 버퍼를 만들어 enqueue
      const buffer = new Uint8Array(1024);
      const bytesRead = await readFromSource(buffer.buffer, 0, 1024);

      if (bytesRead === 0) {
        controller.close();
      } else {
        controller.enqueue(new Uint8Array(buffer.buffer, 0, bytesRead));
      }
    }
  },

  cancel() {
    closeSource();
  }
});
```

#### BYOB Reader 상세

BYOB(Bring Your Own Buffer) 리더

- 소비자가 직접 버퍼를 제공해 메모리 할당을 최소화하는 패턴

```js
async function readFileWithBYOB(byteStream) {
  const reader = byteStream.getReader({ mode: 'byob' });
  const chunks = [];
  let totalBytes = 0;

  try {
    while (true) {
      // 매 읽기마다 새 버퍼를 할당하지 않고 재사용
      const buffer = new ArrayBuffer(64 * 1024); // 64KB 버퍼
      let offset = 0;

      // 버퍼가 가득 찰 때까지 반복 읽기
      while (offset < buffer.byteLength) {
        const view = new Uint8Array(buffer, offset, buffer.byteLength - offset);
        const { value, done } = await reader.read(view);

        if (done) {
          if (offset > 0) {
            chunks.push(new Uint8Array(buffer, 0, offset));
            totalBytes += offset;
          }
          console.log(`총 ${totalBytes} 바이트 읽음`);
          return concatenateChunks(chunks, totalBytes);
        }

        offset += value.byteLength;
        // 주의: value는 새 뷰이므로 value.buffer를 사용해야 한다
      }

      chunks.push(new Uint8Array(buffer));
      totalBytes += buffer.byteLength;
    }
  } finally {
    reader.releaseLock();
  }
}

function concatenateChunks(chunks, totalLength) {
  const result = new Uint8Array(totalLength);
  let offset = 0;
  for (const chunk of chunks) {
    result.set(chunk, offset);
    offset += chunk.byteLength;
  }
  return result;
}
```

#### autoAllocateChunkSize

- `autoAllocateChunkSize` 설정 시 기본(default) 리더 사용에도 내부적으로 BYOB 메커니즘 활용
- 자동으로 지정된 크기의 버퍼가 할당되어 `byobRequest`에 연결됨

```js
const stream = new ReadableStream({
  type: 'bytes',
  autoAllocateChunkSize: 1024, // 1KB 자동 할당

  async pull(controller) {
    // autoAllocateChunkSize 덕분에 기본 리더를 사용해도
    // controller.byobRequest가 null이 아니다
    const view = controller.byobRequest.view;
    const bytesRead = await readFromDevice(
      view.buffer, view.byteOffset, view.byteLength
    );

    if (bytesRead === 0) {
      controller.close();
      controller.byobRequest.respond(0);
    } else {
      controller.byobRequest.respond(bytesRead);
    }
  }
});

// 기본 리더로도 사용 가능 (내부적으로 BYOB 메커니즘 활용)
const reader = stream.getReader();
const { value } = await reader.read(); // Uint8Array
```

#### 바이트 스트림 vs 기본 스트림 비교

- 청크 타입
  - 기본 스트림: 아무 JS 값
  - 바이트 스트림: ArrayBufferView만
- 컨트롤러
  - 기본 스트림: ReadableStreamDefaultController
  - 바이트 스트림: ReadableByteStreamController
- BYOB 리더
  - 기본 스트림: 사용 불가
  - 바이트 스트림: 사용 가능
- byobRequest
  - 기본 스트림: 없음
  - 바이트 스트림: 있음
- autoAllocateChunkSize
  - 기본 스트림: 없음
  - 바이트 스트림: 사용 가능
- 주요 용도
  - 기본 스트림: 범용 데이터
  - 바이트 스트림: 바이너리 데이터
- 메모리 효율
  - 기본 스트림: 보통
  - 바이트 스트림: 높음(버퍼 재사용)

#### 파일 읽기 바이트 스트림 예제

```js
// Blob을 바이트 스트림으로 읽기
function blobToByteStream(blob, chunkSize = 64 * 1024) {
  let offset = 0;

  return new ReadableStream({
    type: 'bytes',

    async pull(controller) {
      if (offset >= blob.size) {
        controller.close();
        return;
      }

      const end = Math.min(offset + chunkSize, blob.size);
      const slice = blob.slice(offset, end);
      const buffer = await slice.arrayBuffer();

      controller.enqueue(new Uint8Array(buffer));
      offset = end;
    }
  });
}

// 사용
const file = new File(['Hello, World!'], 'test.txt');
const stream = blobToByteStream(file, 5);

const reader = stream.getReader();
while (true) {
  const { value, done } = await reader.read();
  if (done) break;
  console.log(new TextDecoder().decode(value));
}
```

---

### 18. 실용 예제

#### 18.1 Fetch와 함께 사용

##### 스트리밍 응답 읽기

```js
async function fetchWithStreaming(url) {
  const response = await fetch(url);

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  // response.body는 ReadableStream<Uint8Array>
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let result = '';

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    result += decoder.decode(value, { stream: true });
    console.log('수신 중...', result.length, '바이트');
  }

  // 디코더 플러시
  result += decoder.decode();
  return result;
}
```

##### Server-Sent Events (SSE) 스트림 파싱

```js
async function* parseSSE(url) {
  const response = await fetch(url);
  const reader = response.body
    .pipeThrough(new TextDecoderStream())
    .getReader();

  let buffer = '';

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;

    buffer += value;
    const events = buffer.split('\n\n');
    buffer = events.pop(); // 마지막 불완전한 이벤트는 버퍼에 유지

    for (const event of events) {
      if (!event.trim()) continue;

      const lines = event.split('\n');
      const parsed = {};

      for (const line of lines) {
        if (line.startsWith('data: ')) {
          parsed.data = (parsed.data || '') + line.slice(6);
        } else if (line.startsWith('event: ')) {
          parsed.event = line.slice(7);
        } else if (line.startsWith('id: ')) {
          parsed.id = line.slice(4);
        }
      }

      yield parsed;
    }
  }
}

// 사용
for await (const event of parseSSE('/api/events')) {
  console.log('이벤트:', event.event, '데이터:', event.data);
}
```

##### 스트리밍 POST 요청

```js
// 대용량 데이터를 스트리밍으로 업로드
async function streamUpload(url, dataSource) {
  const readable = new ReadableStream({
    async start(controller) {
      for await (const chunk of dataSource) {
        controller.enqueue(new TextEncoder().encode(
          JSON.stringify(chunk) + '\n'
        ));
      }
      controller.close();
    }
  });

  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-ndjson' },
    body: readable, // ReadableStream을 직접 body로 전달
    duplex: 'half'  // 스트리밍 업로드에 필요
  });

  return response.json();
}
```

#### 18.2 파일 처리

##### 대용량 파일 해시 계산

```js
async function hashFile(file) {
  const stream = file.stream(); // File → ReadableStream<Uint8Array>
  const reader = stream.getReader();

  // SubtleCrypto는 스트리밍을 직접 지원하지 않으므로
  // 전체를 읽어야 하지만, 진행률 추적은 가능
  const chunks = [];
  let loaded = 0;

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;

    chunks.push(value);
    loaded += value.byteLength;
    console.log(`진행: ${((loaded / file.size) * 100).toFixed(1)}%`);
  }

  // 청크 병합 및 해시 계산
  const combined = new Uint8Array(loaded);
  let offset = 0;
  for (const chunk of chunks) {
    combined.set(chunk, offset);
    offset += chunk.byteLength;
  }

  const hashBuffer = await crypto.subtle.digest('SHA-256', combined);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}
```

##### CSV 파서 스트림

```js
function createCSVParserTransform(options = {}) {
  const { delimiter = ',', hasHeader = true } = options;
  let headers = null;
  let isFirstLine = true;
  let buffer = '';

  return new TransformStream({
    transform(chunk, controller) {
      buffer += chunk;
      const lines = buffer.split('\n');
      buffer = lines.pop(); // 불완전한 마지막 줄 보관

      for (const line of lines) {
        const trimmed = line.trim();
        if (!trimmed) continue;

        const fields = parseCSVLine(trimmed, delimiter);

        if (isFirstLine && hasHeader) {
          headers = fields;
          isFirstLine = false;
          continue;
        }

        if (headers) {
          const obj = {};
          headers.forEach((h, i) => { obj[h] = fields[i]; });
          controller.enqueue(obj);
        } else {
          controller.enqueue(fields);
        }

        isFirstLine = false;
      }
    },

    flush(controller) {
      if (buffer.trim()) {
        const fields = parseCSVLine(buffer.trim(), delimiter);
        if (headers) {
          const obj = {};
          headers.forEach((h, i) => { obj[h] = fields[i]; });
          controller.enqueue(obj);
        } else {
          controller.enqueue(fields);
        }
      }
    }
  });
}

function parseCSVLine(line, delimiter) {
  const fields = [];
  let current = '';
  let inQuotes = false;

  for (let i = 0; i < line.length; i++) {
    const char = line[i];
    if (char === '"') {
      if (inQuotes && line[i + 1] === '"') {
        current += '"';
        i++;
      } else {
        inQuotes = !inQuotes;
      }
    } else if (char === delimiter && !inQuotes) {
      fields.push(current);
      current = '';
    } else {
      current += char;
    }
  }
  fields.push(current);
  return fields;
}

// 사용
const response = await fetch('/data/large-file.csv');
const objects = response.body
  .pipeThrough(new TextDecoderStream())
  .pipeThrough(createCSVParserTransform({ delimiter: ',', hasHeader: true }));

for await (const row of objects) {
  console.log(row); // { name: '홍길동', age: '30', city: '서울' }
}
```

#### 18.3 데이터 변환 파이프라인

##### 다단계 텍스트 처리 파이프라인

```js
// 1단계: 줄 분리
function lineSplitter() {
  let buffer = '';
  return new TransformStream({
    transform(chunk, controller) {
      buffer += chunk;
      const lines = buffer.split('\n');
      buffer = lines.pop();
      lines.forEach(line => controller.enqueue(line));
    },
    flush(controller) {
      if (buffer) controller.enqueue(buffer);
    }
  });
}

// 2단계: 빈 줄 필터링
function emptyLineFilter() {
  return new TransformStream({
    transform(chunk, controller) {
      if (chunk.trim().length > 0) {
        controller.enqueue(chunk);
      }
    }
  });
}

// 3단계: 번호 매기기
function lineNumberer() {
  let lineNum = 0;
  return new TransformStream({
    transform(chunk, controller) {
      lineNum++;
      controller.enqueue(`${lineNum}: ${chunk}`);
    }
  });
}

// 4단계: 접두사 추가
function prefixer(prefix) {
  return new TransformStream({
    transform(chunk, controller) {
      controller.enqueue(`${prefix} ${chunk}`);
    }
  });
}

// 파이프라인 조합
async function processTextFile(url) {
  const response = await fetch(url);

  const processed = response.body
    .pipeThrough(new TextDecoderStream())
    .pipeThrough(lineSplitter())
    .pipeThrough(emptyLineFilter())
    .pipeThrough(lineNumberer())
    .pipeThrough(prefixer('[LOG]'));

  const results = [];
  for await (const line of processed) {
    results.push(line);
  }
  return results;
}
```

##### JSON Lines (NDJSON) 처리 파이프라인

```js
function createNDJSONTransform() {
  let buffer = '';

  return new TransformStream({
    transform(chunk, controller) {
      buffer += chunk;
      const lines = buffer.split('\n');
      buffer = lines.pop();

      for (const line of lines) {
        if (line.trim()) {
          try {
            controller.enqueue(JSON.parse(line));
          } catch (e) {
            console.warn('JSON 파싱 실패:', line);
          }
        }
      }
    },

    flush(controller) {
      if (buffer.trim()) {
        try {
          controller.enqueue(JSON.parse(buffer));
        } catch (e) {
          console.warn('마지막 줄 JSON 파싱 실패:', buffer);
        }
      }
    }
  });
}

// 집계 TransformStream
function createAggregator(keyFn, valueFn) {
  const groups = new Map();

  return new TransformStream({
    transform(chunk) {
      const key = keyFn(chunk);
      const value = valueFn(chunk);
      if (!groups.has(key)) {
        groups.set(key, []);
      }
      groups.get(key).push(value);
    },

    flush(controller) {
      for (const [key, values] of groups) {
        controller.enqueue({
          key,
          count: values.length,
          sum: values.reduce((a, b) => a + b, 0),
          avg: values.reduce((a, b) => a + b, 0) / values.length
        });
      }
    }
  });
}

// NDJSON 데이터 집계 파이프라인
async function aggregateData(url) {
  const response = await fetch(url);

  const results = [];
  const pipeline = response.body
    .pipeThrough(new TextDecoderStream())
    .pipeThrough(createNDJSONTransform())
    .pipeThrough(createAggregator(
      item => item.category,     // 카테고리별 그룹화
      item => item.amount         // 금액 집계
    ));

  for await (const result of pipeline) {
    results.push(result);
    console.log(`카테고리: ${result.key}, 건수: ${result.count}, 합계: ${result.sum}, 평균: ${result.avg.toFixed(2)}`);
  }

  return results;
}
```

#### 18.4 진행률 표시

##### 다운로드 진행률

```js
function createProgressStream(totalSize, onProgress) {
  let loaded = 0;
  let startTime = Date.now();

  return new TransformStream({
    transform(chunk, controller) {
      loaded += chunk.byteLength;
      const elapsed = (Date.now() - startTime) / 1000;
      const speed = loaded / elapsed; // 바이트/초

      onProgress({
        loaded,
        total: totalSize,
        percentage: totalSize > 0
          ? ((loaded / totalSize) * 100).toFixed(1)
          : 'unknown',
        speed: formatBytes(speed) + '/s',
        elapsed: elapsed.toFixed(1) + 's',
        remaining: totalSize > 0
          ? ((totalSize - loaded) / speed).toFixed(1) + 's'
          : 'unknown'
      });

      controller.enqueue(chunk);
    }
  });
}

function formatBytes(bytes) {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  return (bytes / (1024 * 1024 * 1024)).toFixed(1) + ' GB';
}

// 사용 예
async function downloadWithProgress(url) {
  const response = await fetch(url);
  const totalSize = Number(response.headers.get('Content-Length')) || 0;

  const progressStream = createProgressStream(totalSize, (info) => {
    console.log(`다운로드: ${info.percentage}% | ${info.speed} | 남은 시간: ${info.remaining}`);
    // UI 업데이트
    // progressBar.style.width = info.percentage + '%';
    // progressText.textContent = `${info.percentage}% (${info.speed})`;
  });

  const data = await new Response(
    response.body.pipeThrough(progressStream)
  ).arrayBuffer();

  console.log(`다운로드 완료: ${formatBytes(data.byteLength)}`);
  return data;
}
```

##### 업로드 진행률

```js
function createUploadProgressStream(totalSize, onProgress) {
  let uploaded = 0;

  return new TransformStream({
    transform(chunk, controller) {
      uploaded += chunk.byteLength;
      onProgress({
        uploaded,
        total: totalSize,
        percentage: ((uploaded / totalSize) * 100).toFixed(1)
      });
      controller.enqueue(chunk);
    }
  });
}

async function uploadFileWithProgress(url, file) {
  const totalSize = file.size;

  const progressStream = createUploadProgressStream(totalSize, (info) => {
    console.log(`업로드: ${info.percentage}%`);
  });

  const body = file.stream().pipeThrough(progressStream);

  const response = await fetch(url, {
    method: 'POST',
    body,
    duplex: 'half',
    headers: {
      'Content-Type': file.type,
      'Content-Length': totalSize.toString()
    }
  });

  return response.json();
}
```

#### 18.5 청크 단위 텍스트 처리

##### 실시간 검색/하이라이팅

```js
function createSearchHighlightTransform(searchTerm) {
  return new TransformStream({
    transform(line, controller) {
      if (line.toLowerCase().includes(searchTerm.toLowerCase())) {
        const highlighted = line.replace(
          new RegExp(`(${escapeRegex(searchTerm)})`, 'gi'),
          '$1'
        );
        controller.enqueue({ line: highlighted, matched: true });
      } else {
        controller.enqueue({ line, matched: false });
      }
    }
  });
}

function escapeRegex(string) {
  return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

// 대용량 로그 파일에서 실시간 검색
async function searchInLogFile(url, searchTerm) {
  const response = await fetch(url);
  const matches = [];

  const pipeline = response.body
    .pipeThrough(new TextDecoderStream())
    .pipeThrough(lineSplitter())
    .pipeThrough(createSearchHighlightTransform(searchTerm));

  for await (const result of pipeline) {
    if (result.matched) {
      matches.push(result.line);
    }
  }

  return matches;
}
```

##### 스트리밍 단어 카운터

```js
function createWordCountTransform() {
  const wordCounts = new Map();

  return new TransformStream({
    transform(chunk) {
      const words = chunk
        .toLowerCase()
        .replace(/[^\w\s가-힣]/g, '')
        .split(/\s+/)
        .filter(Boolean);

      for (const word of words) {
        wordCounts.set(word, (wordCounts.get(word) || 0) + 1);
      }
      // 중간 결과는 enqueue하지 않음 (집계만 수행)
    },

    flush(controller) {
      // 최종 결과를 정렬하여 출력
      const sorted = [...wordCounts.entries()]
        .sort((a, b) => b[1] - a[1]);

      for (const [word, count] of sorted) {
        controller.enqueue({ word, count });
      }
    }
  });
}

// 사용
async function countWords(url) {
  const response = await fetch(url);

  const pipeline = response.body
    .pipeThrough(new TextDecoderStream())
    .pipeThrough(createWordCountTransform());

  const topWords = [];
  let rank = 0;

  for await (const { word, count } of pipeline) {
    rank++;
    if (rank <= 20) { // 상위 20개만
      topWords.push({ rank, word, count });
      console.log(`${rank}. "${word}": ${count}회`);
    }
  }

  return topWords;
}
```

##### 스트리밍 마크다운 → HTML 변환 (간이 버전)

```js
function createMarkdownToHtmlTransform() {
  return new TransformStream({
    transform(line, controller) {
      let html = line;

      // 헤더 변환
      if (html.startsWith('### ')) {
        html = `<h3>${html.slice(4)}</h3>`;
      } else if (html.startsWith('## ')) {
        html = `<h2>${html.slice(3)}</h2>`;
      } else if (html.startsWith('# ')) {
        html = `<h1>${html.slice(2)}</h1>`;
      }
      // 굵게
      else {
        html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
        html = html.replace(/\*(.+?)\*/g, '<em>$1</em>');
        html = html.replace(/`(.+?)`/g, '<code>$1</code>');
        html = `<p>${html}</p>`;
      }

      controller.enqueue(html);
    }
  });
}

// 마크다운 파일을 스트리밍으로 HTML 변환
async function convertMarkdown(url) {
  const response = await fetch(url);

  const htmlParts = [];
  const pipeline = response.body
    .pipeThrough(new TextDecoderStream())
    .pipeThrough(lineSplitter())
    .pipeThrough(createMarkdownToHtmlTransform());

  for await (const html of pipeline) {
    htmlParts.push(html);
  }

  return htmlParts.join('\n');
}
```

#### 18.6 압축/해제

```js
// Gzip 압축 후 업로드
async function compressAndUpload(url, data) {
  const textEncoder = new TextEncoderStream();
  const compressionStream = new CompressionStream('gzip');

  const readable = new ReadableStream({
    start(controller) {
      controller.enqueue(data);
      controller.close();
    }
  });

  const compressedStream = readable
    .pipeThrough(textEncoder)
    .pipeThrough(compressionStream);

  const response = await fetch(url, {
    method: 'POST',
    body: compressedStream,
    duplex: 'half',
    headers: {
      'Content-Encoding': 'gzip',
      'Content-Type': 'application/json'
    }
  });

  return response;
}

// 압축된 응답 스트리밍 해제
async function fetchAndDecompress(url) {
  const response = await fetch(url);

  const decompressed = response.body
    .pipeThrough(new DecompressionStream('gzip'))
    .pipeThrough(new TextDecoderStream());

  let result = '';
  for await (const chunk of decompressed) {
    result += chunk;
  }

  return result;
}
```

#### 18.7 ReadableStream을 활용한 무한 데이터 소스

```js
// 피보나치 수열 무한 스트림
function fibonacciStream() {
  let a = 0n, b = 1n;

  return new ReadableStream({
    pull(controller) {
      controller.enqueue(a);
      [a, b] = [b, a + b];
    }
  });
}

// 처음 50개 피보나치 수 가져오기
async function getFirstNFibonacci(n) {
  const stream = fibonacciStream();
  const reader = stream.getReader();
  const results = [];

  for (let i = 0; i < n; i++) {
    const { value } = await reader.read();
    results.push(value);
  }

  await reader.cancel(); // 무한 스트림이므로 명시적 취소 필요
  return results;
}

// 타이머 기반 이벤트 스트림
function timerStream(intervalMs) {
  let id;
  let count = 0;

  return new ReadableStream({
    start(controller) {
      id = setInterval(() => {
        count++;
        controller.enqueue({
          tick: count,
          timestamp: Date.now()
        });
      }, intervalMs);
    },

    cancel() {
      clearInterval(id);
    }
  });
}

// 3초 동안의 100ms 타이머 이벤트 수집
async function collectTimerEvents() {
  const stream = timerStream(100);
  const reader = stream.getReader();
  const events = [];

  const timeout = setTimeout(() => reader.cancel('시간 초과'), 3000);

  try {
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      events.push(value);
    }
  } catch {
    // cancel로 인한 오류 무시
  }

  clearTimeout(timeout);
  return events;
}
```

#### 18.8 웹소켓과 스트림 결합

```js
function webSocketToStreams(url) {
  const ws = new WebSocket(url);

  const readable = new ReadableStream({
    start(controller) {
      ws.onmessage = (event) => {
        controller.enqueue(event.data);
      };

      ws.onerror = (error) => {
        controller.error(error);
      };

      ws.onclose = () => {
        controller.close();
      };
    },

    cancel() {
      ws.close();
    }
  });

  const writable = new WritableStream({
    write(chunk) {
      if (ws.readyState === WebSocket.OPEN) {
        ws.send(chunk);
      } else {
        throw new Error('WebSocket이 열려 있지 않습니다');
      }
    },

    close() {
      ws.close();
    },

    abort() {
      ws.close();
    }
  });

  return { readable, writable };
}

// 사용
const { readable, writable } = webSocketToStreams('wss://example.com/ws');

// 수신 데이터를 JSON으로 파싱하여 처리
readable
  .pipeThrough(new TransformStream({
    transform(chunk, controller) {
      try {
        controller.enqueue(JSON.parse(chunk));
      } catch {
        console.warn('비-JSON 메시지:', chunk);
      }
    }
  }))
  .pipeTo(new WritableStream({
    write(message) {
      handleMessage(message);
    }
  }));

// 데이터 전송
const writer = writable.getWriter();
await writer.write(JSON.stringify({ type: 'subscribe', channel: 'updates' }));
```

#### 18.9 여러 스트림 병합(Merge)

```js
// 여러 ReadableStream을 하나로 병합
function mergeStreams(...streams) {
  let activeReaders = streams.map(s => s.getReader());

  return new ReadableStream({
    async pull(controller) {
      if (activeReaders.length === 0) {
        controller.close();
        return;
      }

      // 모든 활성 리더에서 동시에 읽기 시도
      const readPromises = activeReaders.map((reader, index) =>
        reader.read().then(result => ({ result, index }))
      );

      const { result, index } = await Promise.race(readPromises);

      if (result.done) {
        // 완료된 리더 제거
        activeReaders.splice(index, 1);
        if (activeReaders.length === 0) {
          controller.close();
        }
      } else {
        controller.enqueue(result.value);
      }
    },

    cancel() {
      activeReaders.forEach(reader => reader.cancel());
      activeReaders = [];
    }
  });
}

// 사용: 여러 API 엔드포인트의 데이터를 하나의 스트림으로
const stream1 = (await fetch('/api/source1')).body;
const stream2 = (await fetch('/api/source2')).body;
const merged = mergeStreams(stream1, stream2);
```

#### 18.10 스트림 기반 간이 ETL 파이프라인

```js
// Extract(추출) - Transform(변환) - Load(적재) 파이프라인
async function etlPipeline() {
  // 1. Extract: 여러 소스에서 데이터 추출
  const response = await fetch('/api/raw-data');

  // 2. Transform: 다단계 변환
  const transformed = response.body
    .pipeThrough(new TextDecoderStream())
    .pipeThrough(createNDJSONTransform())
    // 데이터 정제
    .pipeThrough(new TransformStream({
      transform(record, controller) {
        // null 값 제거, 타입 변환 등
        const cleaned = {
          id: Number(record.id),
          name: (record.name || '').trim(),
          amount: parseFloat(record.amount) || 0,
          date: new Date(record.date).toISOString(),
          category: (record.category || 'unknown').toLowerCase()
        };

        // 유효성 검사
        if (cleaned.id > 0 && cleaned.name) {
          controller.enqueue(cleaned);
        }
      }
    }))
    // 배치로 묶기
    .pipeThrough(createBatchTransform(100));

  // 3. Load: 배치 단위로 적재
  await transformed.pipeTo(new WritableStream({
    async write(batch) {
      console.log(`${batch.length}개 레코드 적재 중...`);
      await fetch('/api/warehouse/bulk-insert', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(batch)
      });
      console.log(`${batch.length}개 레코드 적재 완료`);
    },

    close() {
      console.log('ETL 파이프라인 완료');
    }
  }));
}

function createBatchTransform(size) {
  let batch = [];
  return new TransformStream({
    transform(chunk, controller) {
      batch.push(chunk);
      if (batch.length >= size) {
        controller.enqueue(batch);
        batch = [];
      }
    },
    flush(controller) {
      if (batch.length > 0) {
        controller.enqueue(batch);
      }
    }
  });
}
```

---

### 부록: API 요약표

#### ReadableStream

- `constructor(source?, strategy?)`
  - 반환 타입: `ReadableStream`
  - 설명: 새 스트림 생성
- `locked`
  - 반환 타입: `boolean`
  - 설명: 잠금 상태
- `cancel(reason?)`
  - 반환 타입: `Promise<void>`
  - 설명: 스트림 취소
- `getReader(options?)`
  - 반환 타입: `Reader`
  - 설명: 리더 획득
- `pipeThrough(transform, options?)`
  - 반환 타입: `ReadableStream`
  - 설명: 변환 파이프
- `pipeTo(destination, options?)`
  - 반환 타입: `Promise<void>`
  - 설명: 싱크로 파이프
- `tee()`
  - 반환 타입: `[ReadableStream, ReadableStream]`
  - 설명: 두 갈래로 분기
- `values(options?)`
  - 반환 타입: `AsyncIterator`
  - 설명: 비동기 이터레이터
- `ReadableStream.from(iterable)`
  - 반환 타입: `ReadableStream`
  - 설명: 이터러블에서 생성

#### WritableStream

- `constructor(sink?, strategy?)`
  - 반환 타입: `WritableStream`
  - 설명: 새 스트림 생성
- `locked`
  - 반환 타입: `boolean`
  - 설명: 잠금 상태
- `abort(reason?)`
  - 반환 타입: `Promise<void>`
  - 설명: 스트림 중단
- `close()`
  - 반환 타입: `Promise<void>`
  - 설명: 스트림 닫기
- `getWriter()`
  - 반환 타입: `WritableStreamDefaultWriter`
  - 설명: 라이터 획득

#### TransformStream

- `constructor(transformer?, wStrategy?, rStrategy?)`
  - 반환 타입: `TransformStream`
  - 설명: 새 스트림 생성
- `readable`
  - 반환 타입: `ReadableStream`
  - 설명: 읽기 면
- `writable`
  - 반환 타입: `WritableStream`
  - 설명: 쓰기 면

---

### 참고 자료

- [WHATWG Streams Standard (공식 명세)](https://streams.spec.whatwg.org/)
- [MDN Web Docs - Streams API](https://developer.mozilla.org/en-US/docs/Web/API/Streams_API)
- [MDN - ReadableStream](https://developer.mozilla.org/en-US/docs/Web/API/ReadableStream)
- [MDN - WritableStream](https://developer.mozilla.org/en-US/docs/Web/API/WritableStream)
- [MDN - TransformStream](https://developer.mozilla.org/en-US/docs/Web/API/TransformStream)

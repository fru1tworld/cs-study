# HTML Living Standard - Web API 및 고급 주제 (Part 2)

> 이 문서는 [HTML Living Standard 기본](html.md)의 후속 문서입니다.

---

## 1. HTML 파싱

브라우저가 HTML 문자열을 DOM 트리로 변환하는 과정은 토크나이저와 트리 구성 두 단계로 나뉜다.

### 1.1 토크나이저 (Tokenizer)

상태 기반 머신으로 동작하며, 입력 스트림을 다음 토큰들로 변환한다.

| 토큰 타입 | 설명 | 예시 |
|-----------|------|------|
| DOCTYPE | 문서 타입 선언 | `<!DOCTYPE html>` |
| Start Tag | 여는 태그 | `<div class="a">` |
| End Tag | 닫는 태그 | `</div>` |
| Comment | 주석 | `<!-- 주석 -->` |
| Character | 텍스트 문자 | `Hello` |
| EOF | 입력 끝 | - |

토크나이저는 80개 이상의 상태를 가지며, 각 문자를 읽을 때마다 상태가 전이된다.

```
Data State → '<' → Tag Open State → 'a'-'z' → Tag Name State → ...
```

### 1.2 트리 구성 (Tree Construction)

토크나이저가 생성한 토큰을 받아 DOM 트리를 구성한다. 열린 요소 스택(stack of open elements)을 관리하며, 삽입 모드(insertion mode)에 따라 동작이 달라진다.

주요 삽입 모드:
- `initial` → `before html` → `before head` → `in head` → `after head` → `in body` → `after body` → `after after body`

### 1.3 에러 처리

HTML 파서는 절대 실패하지 않는다. 잘못된 마크업도 일관된 규칙으로 처리한다.

```html
<!-- 입력 -->
<p>단락 1<p>단락 2

<!-- 파서 해석 결과 -->
<p>단락 1</p><p>단락 2</p>
```

```html
<!-- 테이블 내 잘못된 요소 -->
<table><b>텍스트</b></table>

<!-- 파서 결과: <b>가 테이블 밖으로 이동 -->
<b>텍스트</b><table></table>
```

### 1.4 Foster Parenting

`<table>`, `<tbody>`, `<tr>`, `<td>` 내부에 허용되지 않는 노드가 등장하면, 해당 노드를 테이블의 부모 요소 앞으로 이동시킨다. 이를 foster parenting이라 한다.

```html
<!-- 입력 -->
<table>
  <tr><td>셀</td></tr>
  잘못된 텍스트
</table>

<!-- DOM 결과 -->
잘못된 텍스트
<table>
  <tbody><tr><td>셀</td></tr></tbody>
</table>
```

> `<tbody>`가 자동 삽입되고, "잘못된 텍스트"는 테이블 앞으로 foster parent 된다.

---

## 2. 스크립팅

### 2.1 `<script>` 로딩 전략

```html
<!-- 동기(기본): HTML 파싱을 차단 -->
<script src="app.js"></script>

<!-- defer: HTML 파싱 완료 후, DOMContentLoaded 전 실행. 순서 보장 -->
<script defer src="a.js"></script>
<script defer src="b.js"></script>

<!-- async: 다운로드 완료 즉시 실행. 순서 보장 안 됨 -->
<script async src="analytics.js"></script>

<!-- module: 기본적으로 defer 동작. strict mode 적용 -->
<script type="module" src="app.mjs"></script>
<script type="module">
  import { greet } from './utils.mjs';
  greet();
</script>
```

실행 순서 비교:

```
일반:   HTML 파싱 중단 → 다운로드 → 실행 → 파싱 재개
defer:  HTML 파싱과 병렬 다운로드 → 파싱 완료 후 순서대로 실행
async:  HTML 파싱과 병렬 다운로드 → 다운로드 완료 즉시 실행
module: defer와 동일 (async 속성 추가 시 async 동작)
```

### 2.2 `<template>`

렌더링되지 않는 HTML 조각을 선언적으로 정의한다. `content` 프로퍼티로 `DocumentFragment`에 접근한다.

```html
<template id="card-template">
  <div class="card">
    <h2 class="card-title"></h2>
    <p class="card-body"></p>
  </div>
</template>

<script>
  const template = document.getElementById('card-template');
  const clone = template.content.cloneNode(true);
  clone.querySelector('.card-title').textContent = '제목';
  clone.querySelector('.card-body').textContent = '내용';
  document.body.appendChild(clone);
</script>
```

### 2.3 `<slot>`

Shadow DOM 내에서 외부 콘텐츠를 삽입할 위치를 지정한다.

```html
<user-card>
  <span slot="name">김철수</span>
  <span slot="role">개발자</span>
</user-card>

<script>
  class UserCard extends HTMLElement {
    constructor() {
      super();
      const shadow = this.attachShadow({ mode: 'open' });
      shadow.innerHTML = `
        <div class="card">
          <h3><slot name="name">이름 없음</slot></h3>
          <p><slot name="role">역할 없음</slot></p>
          <slot></slot> <!-- 기본(이름 없는) 슬롯 -->
        </div>
      `;
    }
  }
  customElements.define('user-card', UserCard);
</script>
```

`slotchange` 이벤트로 슬롯 내용 변경을 감지할 수 있다.

---

## 3. Window 인터페이스

`Window`는 브라우저 탭(또는 프레임)의 전역 객체다.

### 3.1 주요 속성

| 속성 | 설명 |
|------|------|
| `window.document` | 현재 Document 객체 |
| `window.location` | 현재 URL 정보 (Location 객체) |
| `window.history` | History 객체 |
| `window.navigator` | Navigator 객체 (UA, 언어, 온라인 상태 등) |
| `window.innerWidth/Height` | 뷰포트 크기 (스크롤바 제외) |
| `window.outerWidth/Height` | 브라우저 창 전체 크기 |
| `window.devicePixelRatio` | 물리 픽셀 / CSS 픽셀 비율 |
| `window.name` | 창 이름 (탭 간 유지) |
| `window.opener` | 현재 창을 연 창의 참조 |
| `window.closed` | 창이 닫혔는지 여부 |

### 3.2 주요 메서드

```javascript
// 타이머
const id = setTimeout(() => console.log('1초 후'), 1000);
clearTimeout(id);
const intervalId = setInterval(() => console.log('반복'), 500);
clearInterval(intervalId);

// 고성능 반복 (애니메이션)
function animate(timestamp) {
  // 렌더링 로직
  requestAnimationFrame(animate);
}
requestAnimationFrame(animate);

// 대화상자
alert('알림');
const ok = confirm('확인하시겠습니까?');
const input = prompt('이름을 입력하세요', '기본값');

// 창 제어
const popup = window.open('https://example.com', '_blank', 'width=400,height=300');
window.close();

// base64
const encoded = btoa('Hello');       // "SGVsbG8="
const decoded = atob('SGVsbG8=');    // "Hello"

// 구조화 복제
const clone = structuredClone({ a: 1, b: [2, 3] });

// 스크롤
window.scrollTo({ top: 0, behavior: 'smooth' });
```

---

## 4. Navigation / History API

### 4.1 History API (전통적)

```javascript
// 새 항목 추가 (페이지 이동 없이 URL 변경)
history.pushState({ page: 2 }, '', '/page/2');

// 현재 항목 교체
history.replaceState({ page: 2, updated: true }, '', '/page/2');

// 뒤로/앞으로
history.back();
history.forward();
history.go(-2); // 2단계 뒤로

// popstate 이벤트: 뒤로가기/앞으로가기 시 발생
window.addEventListener('popstate', (e) => {
  console.log('상태:', e.state);
  renderPage(e.state);
});
```

> `pushState`/`replaceState` 호출 시에는 `popstate`가 발생하지 않는다.

### 4.2 Location 객체

```javascript
// URL: https://example.com:8080/path?q=hello#section
location.href;       // 전체 URL
location.protocol;   // "https:"
location.host;       // "example.com:8080"
location.hostname;   // "example.com"
location.port;       // "8080"
location.pathname;   // "/path"
location.search;     // "?q=hello"
location.hash;       // "#section"
location.origin;     // "https://example.com:8080"

// 네비게이션
location.assign('/new-page');   // 이동 (히스토리에 추가)
location.replace('/new-page');  // 이동 (히스토리 교체)
location.reload();              // 새로고침
```

### 4.3 Navigation API (신규)

`History API`의 한계를 보완하는 새로운 API다.

```javascript
// navigate 이벤트 인터셉트
navigation.addEventListener('navigate', (e) => {
  if (!e.canIntercept) return;

  const url = new URL(e.destination.url);

  if (url.pathname.startsWith('/articles/')) {
    e.intercept({
      async handler() {
        const content = await fetchArticle(url.pathname);
        renderArticle(content);
      }
    });
  }
});

// 프로그래밍 방식 네비게이션
const result = navigation.navigate('/page/3', {
  state: { from: 'home' }
});
await result.committed;  // URL 변경됨
await result.finished;   // handler 완료됨

// 뒤로/앞으로
navigation.back();
navigation.forward();

// 현재 항목 상태 접근
const state = navigation.currentEntry.getState();

// 항목 목록
const entries = navigation.entries();
```

---

## 5. Web Storage

동일 출처(origin) 단위로 키-값 데이터를 저장한다. 값은 항상 문자열이다.

### 5.1 localStorage vs sessionStorage

| 특성 | `localStorage` | `sessionStorage` |
|------|----------------|-------------------|
| 수명 | 영구 (수동 삭제 전까지) | 탭/창 닫으면 소멸 |
| 범위 | 동일 출처의 모든 탭 | 해당 탭/창에만 한정 |
| 용량 | 약 5~10MB | 약 5MB |
| storage 이벤트 | 다른 탭에서 발생 | 발생하지 않음 |

### 5.2 API

```javascript
// 저장
localStorage.setItem('theme', 'dark');
localStorage.setItem('user', JSON.stringify({ name: '김철수', age: 30 }));

// 조회
const theme = localStorage.getItem('theme');
const user = JSON.parse(localStorage.getItem('user'));

// 삭제
localStorage.removeItem('theme');
localStorage.clear(); // 전체 삭제

// 길이 및 키 순회
for (let i = 0; i < localStorage.length; i++) {
  const key = localStorage.key(i);
  console.log(key, localStorage.getItem(key));
}
```

### 5.3 storage 이벤트

같은 출처의 다른 탭에서 `localStorage`가 변경될 때 발생한다.

```javascript
window.addEventListener('storage', (e) => {
  console.log('변경된 키:', e.key);
  console.log('이전 값:', e.oldValue);
  console.log('새 값:', e.newValue);
  console.log('출처:', e.url);
  console.log('스토리지 객체:', e.storageArea);
});
```

> 현재 탭에서의 변경에는 이벤트가 발생하지 않는다. 탭 간 간단한 동기화에 활용 가능하다.

---

## 6. Web Workers

메인 스레드와 분리된 백그라운드 스레드에서 스크립트를 실행한다. DOM에 접근할 수 없다.

### 6.1 Dedicated Worker

하나의 생성자(페이지)만 통신할 수 있다.

```javascript
// main.js
const worker = new Worker('worker.js');

worker.postMessage({ type: 'compute', data: [1, 2, 3, 4, 5] });

worker.onmessage = (e) => {
  console.log('결과:', e.data);
};

worker.onerror = (e) => {
  console.error('워커 에러:', e.message);
};

worker.terminate(); // 즉시 종료
```

```javascript
// worker.js
self.onmessage = (e) => {
  const { type, data } = e.data;

  if (type === 'compute') {
    const sum = data.reduce((a, b) => a + b, 0);
    self.postMessage({ result: sum });
  }
};
```

### 6.2 Shared Worker

여러 페이지/탭이 하나의 워커 인스턴스를 공유한다.

```javascript
// main.js (여러 탭에서 동일 코드)
const shared = new SharedWorker('shared-worker.js');

shared.port.onmessage = (e) => {
  console.log('응답:', e.data);
};

shared.port.postMessage('안녕하세요');
```

```javascript
// shared-worker.js
const ports = [];

self.onconnect = (e) => {
  const port = e.ports[0];
  ports.push(port);

  port.onmessage = (event) => {
    // 연결된 모든 포트에 브로드캐스트
    ports.forEach(p => p.postMessage(`전체 연결 수: ${ports.length}`));
  };

  port.start();
};
```

### 6.3 Transferable Objects

데이터를 복사 대신 소유권을 이전하여 성능을 높인다. 전송 후 원본은 사용 불가가 된다.

```javascript
// ArrayBuffer 전송 (복사 없이 이동)
const buffer = new ArrayBuffer(1024 * 1024); // 1MB
console.log(buffer.byteLength); // 1048576

worker.postMessage(buffer, [buffer]); // 두 번째 인자: transfer list
console.log(buffer.byteLength); // 0 (소유권 이전됨)

// structuredClone의 transfer와 동일한 개념
const ab = new ArrayBuffer(100);
worker.postMessage({ payload: ab }, { transfer: [ab] });
```

전송 가능한 타입: `ArrayBuffer`, `MessagePort`, `ImageBitmap`, `OffscreenCanvas`, `ReadableStream`, `WritableStream` 등.

---

## 7. MessageChannel / BroadcastChannel

### 7.1 MessageChannel

양방향 통신을 위한 전용 채널을 생성한다. 두 개의 `MessagePort`로 구성된다.

```javascript
const channel = new MessageChannel();

// port1과 port2는 서로 연결된 쌍
channel.port1.onmessage = (e) => console.log('port1 수신:', e.data);
channel.port2.onmessage = (e) => console.log('port2 수신:', e.data);

channel.port1.postMessage('port2에게');
channel.port2.postMessage('port1에게');

// iframe에 포트 전달
const iframe = document.querySelector('iframe');
iframe.contentWindow.postMessage('포트 전달', '*', [channel.port2]);
```

### 7.2 BroadcastChannel

같은 출처의 모든 탭/창/iframe/워커 간 간편한 메시지 브로드캐스트를 제공한다.

```javascript
// 탭 A
const bc = new BroadcastChannel('app-events');
bc.postMessage({ type: 'LOGOUT' });

// 탭 B (같은 출처)
const bc = new BroadcastChannel('app-events');
bc.onmessage = (e) => {
  if (e.data.type === 'LOGOUT') {
    // 로그아웃 처리
    window.location.href = '/login';
  }
};

// 채널 닫기
bc.close();
```

MessageChannel vs BroadcastChannel:

| 특성 | MessageChannel | BroadcastChannel |
|------|---------------|------------------|
| 통신 방식 | 1:1 (두 포트 간) | 1:N (구독자 전체) |
| 포트 전달 | 필요 | 불필요 (채널명으로 연결) |
| 크로스 오리진 | postMessage로 포트 전달 시 가능 | 동일 출처만 |

---

## 8. Server-Sent Events (SSE)

서버에서 클라이언트로의 단방향 실시간 스트림이다. HTTP 기반이며 자동 재연결을 지원한다.

### 클라이언트

```javascript
const source = new EventSource('/api/events');

// 기본 message 이벤트
source.onmessage = (e) => {
  console.log('데이터:', e.data);
  console.log('ID:', e.lastEventId);
};

// 커스텀 이벤트 타입
source.addEventListener('notification', (e) => {
  const data = JSON.parse(e.data);
  showNotification(data.title, data.body);
});

// 연결 상태
source.onopen = () => console.log('연결됨');
source.onerror = (e) => {
  if (source.readyState === EventSource.CLOSED) {
    console.log('연결 종료');
  }
  // CONNECTING 상태면 자동 재연결 시도
};

// 인증 헤더가 필요한 경우
const source2 = new EventSource('/api/events', {
  withCredentials: true // 쿠키 전송
});

source.close(); // 연결 종료
```

### 서버 응답 형식

```
Content-Type: text/event-stream

data: 간단한 메시지

data: {"name": "알림", "count": 5}
id: 42
event: notification
retry: 3000

data: 여러 줄
data: 메시지도 가능
```

| 필드 | 설명 |
|------|------|
| `data:` | 메시지 본문 |
| `event:` | 이벤트 타입 (기본: `message`) |
| `id:` | 이벤트 ID (재연결 시 `Last-Event-ID` 헤더로 전송) |
| `retry:` | 재연결 대기 시간 (ms) |

---

## 9. Cross-document Messaging

서로 다른 출처의 문서(iframe, popup) 간 안전한 통신을 제공한다.

```javascript
// 보내는 쪽 (부모 페이지)
const iframe = document.querySelector('iframe');
iframe.contentWindow.postMessage(
  { action: 'resize', height: 500 },
  'https://child.example.com'  // 수신자 출처 지정 (보안 중요)
);

// 받는 쪽 (iframe 내부)
window.addEventListener('message', (e) => {
  // 1. 반드시 출처 검증
  if (e.origin !== 'https://parent.example.com') return;

  // 2. 메시지 처리
  if (e.data.action === 'resize') {
    document.body.style.height = e.data.height + 'px';
  }

  // 3. 응답
  e.source.postMessage({ status: 'ok' }, e.origin);
});
```

보안 주의사항:
- `targetOrigin`에 `'*'`를 사용하지 말 것 (민감한 데이터 유출 위험)
- 수신 측에서 반드시 `event.origin` 검증
- `event.data`의 내용을 `innerHTML`에 직접 삽입하지 말 것

---

## 10. Drag and Drop API

### 10.1 이벤트 흐름

```
드래그 소스:  dragstart → drag(반복) → dragend
드롭 대상:   dragenter → dragover(반복) → drop / dragleave
```

### 10.2 구현 예시

```html
<div id="item" draggable="true">드래그할 아이템</div>
<div id="dropzone">여기에 놓으세요</div>

<script>
  const item = document.getElementById('item');
  const zone = document.getElementById('dropzone');

  // 드래그 시작
  item.addEventListener('dragstart', (e) => {
    e.dataTransfer.setData('text/plain', item.id);
    e.dataTransfer.effectAllowed = 'move';
    item.classList.add('dragging');
  });

  item.addEventListener('dragend', (e) => {
    item.classList.remove('dragging');
  });

  // 드롭 영역
  zone.addEventListener('dragover', (e) => {
    e.preventDefault(); // 필수: drop 이벤트 허용
    e.dataTransfer.dropEffect = 'move';
    zone.classList.add('over');
  });

  zone.addEventListener('dragleave', () => {
    zone.classList.remove('over');
  });

  zone.addEventListener('drop', (e) => {
    e.preventDefault();
    zone.classList.remove('over');

    const id = e.dataTransfer.getData('text/plain');
    const el = document.getElementById(id);
    zone.appendChild(el);
  });
</script>
```

### 10.3 파일 드래그

```javascript
zone.addEventListener('drop', (e) => {
  e.preventDefault();

  const files = e.dataTransfer.files;
  for (const file of files) {
    console.log(`${file.name} (${file.size} bytes, ${file.type})`);
  }

  // DataTransferItemList API (디렉토리 지원)
  for (const item of e.dataTransfer.items) {
    if (item.kind === 'file') {
      const entry = item.webkitGetAsEntry();
      if (entry.isDirectory) {
        // 디렉토리 순회
      }
    }
  }
});
```

---

## 11. Custom Elements

### 11.1 정의 및 등록

```javascript
class MyCounter extends HTMLElement {
  #count = 0;
  #shadow;

  // 감시할 속성 목록
  static observedAttributes = ['initial'];

  constructor() {
    super();
    this.#shadow = this.attachShadow({ mode: 'open' });
    this.#shadow.innerHTML = `
      <style>
        :host { display: inline-flex; align-items: center; gap: 8px; }
        button { cursor: pointer; }
        span { min-width: 2em; text-align: center; }
      </style>
      <button id="dec">-</button>
      <span id="display">0</span>
      <button id="inc">+</button>
    `;
  }

  // DOM에 삽입될 때
  connectedCallback() {
    this.#shadow.getElementById('inc').addEventListener('click', () => this.#update(1));
    this.#shadow.getElementById('dec').addEventListener('click', () => this.#update(-1));
  }

  // DOM에서 제거될 때
  disconnectedCallback() {
    // 정리 작업
  }

  // 다른 문서로 이동될 때
  adoptedCallback() {}

  // 감시 속성 변경 시
  attributeChangedCallback(name, oldVal, newVal) {
    if (name === 'initial') {
      this.#count = parseInt(newVal) || 0;
      this.#render();
    }
  }

  #update(delta) {
    this.#count += delta;
    this.#render();
    this.dispatchEvent(new CustomEvent('count-changed', {
      detail: { count: this.#count },
      bubbles: true
    }));
  }

  #render() {
    this.#shadow.getElementById('display').textContent = this.#count;
  }
}

// 등록 (이름에 반드시 하이픈 포함)
customElements.define('my-counter', MyCounter);
```

```html
<my-counter initial="10"></my-counter>

<script>
  // 정의 완료 대기
  customElements.whenDefined('my-counter').then(() => {
    console.log('my-counter 사용 가능');
  });
</script>
```

### 11.2 기존 요소 확장 (Customized Built-in)

```javascript
class FancyButton extends HTMLButtonElement {
  connectedCallback() {
    this.style.background = 'linear-gradient(45deg, #6a5acd, #00bfff)';
    this.style.color = 'white';
  }
}

customElements.define('fancy-button', FancyButton, { extends: 'button' });
```

```html
<button is="fancy-button">클릭</button>
```

> Safari는 Customized Built-in Elements를 지원하지 않는다. Autonomous Custom Element 사용을 권장.

### 11.3 ElementInternals

Custom Element가 폼 참여, 접근성, 상태 관리를 네이티브 요소처럼 수행할 수 있게 한다.

```javascript
class MyInput extends HTMLElement {
  static formAssociated = true; // 폼 연동 선언

  #internals;

  constructor() {
    super();
    this.#internals = this.attachInternals();
    this.attachShadow({ mode: 'open' });
    this.shadowRoot.innerHTML = `<input type="text" />`;
  }

  connectedCallback() {
    const input = this.shadowRoot.querySelector('input');

    input.addEventListener('input', () => {
      // 폼 값 설정
      this.#internals.setFormValue(input.value);

      // 유효성 검사
      if (!input.value) {
        this.#internals.setValidity(
          { valueMissing: true },
          '값을 입력해주세요',
          input
        );
      } else {
        this.#internals.setValidity({});
      }
    });

    // ARIA 역할 및 속성 설정
    this.#internals.role = 'textbox';
    this.#internals.ariaRequired = 'true';
  }

  // 폼 콜백
  formResetCallback() {
    this.shadowRoot.querySelector('input').value = '';
    this.#internals.setFormValue('');
  }

  formStateRestoreCallback(state, mode) {
    this.shadowRoot.querySelector('input').value = state;
  }
}

customElements.define('my-input', MyInput);
```

```html
<form>
  <my-input name="username"></my-input>
  <button type="submit">제출</button>
</form>
```

---

## 12. Microdata

HTML에 기계가 읽을 수 있는 구조화된 데이터를 내장하는 메커니즘이다. 주로 Schema.org 어휘와 함께 사용한다.

### 12.1 기본 문법

| 속성 | 설명 |
|------|------|
| `itemscope` | 새로운 아이템(항목)의 범위를 선언 |
| `itemtype` | 아이템의 타입 (Schema.org URL) |
| `itemprop` | 속성 이름 |
| `itemid` | 아이템의 고유 식별자 |

### 12.2 예시: 제품 정보

```html
<div itemscope itemtype="https://schema.org/Product">
  <h1 itemprop="name">무선 이어폰</h1>
  <img itemprop="image" src="earphone.jpg" alt="무선 이어폰">
  <p itemprop="description">고품질 블루투스 무선 이어폰입니다.</p>

  <div itemprop="offers" itemscope itemtype="https://schema.org/Offer">
    <span itemprop="priceCurrency">KRW</span>
    <span itemprop="price" content="89000">89,000원</span>
    <link itemprop="availability" href="https://schema.org/InStock" />
    <meta itemprop="url" content="https://shop.example.com/earphone" />
  </div>

  <div itemprop="aggregateRating" itemscope itemtype="https://schema.org/AggregateRating">
    평점: <span itemprop="ratingValue">4.5</span> / 5
    (<span itemprop="reviewCount">128</span>개 리뷰)
  </div>
</div>
```

### 12.3 속성값 추출 규칙

요소에 따라 `itemprop`의 값이 추출되는 방식이 다르다.

| 요소 | 값 소스 |
|------|---------|
| `<meta>` | `content` 속성 |
| `<a>`, `<area>`, `<link>` | `href` 속성 |
| `<img>`, `<source>`, `<video>`, `<audio>` | `src` 속성 |
| `<time>` | `datetime` 속성 |
| `<data>` | `value` 속성 |
| 기타 요소 | `textContent` |

### 12.4 중첩 아이템과 itemid

```html
<article itemscope itemtype="https://schema.org/BlogPosting">
  <h2 itemprop="headline">HTML Microdata 가이드</h2>
  <time itemprop="datePublished" datetime="2025-01-15">2025년 1월 15일</time>

  <div itemprop="author" itemscope itemtype="https://schema.org/Person"
       itemid="https://example.com/people/kim">
    <a itemprop="url" href="https://example.com/people/kim">
      <span itemprop="name">김영희</span>
    </a>
  </div>
</article>
```

### 12.5 JavaScript Microdata API

```javascript
// itemscope 요소 찾기
const products = document.querySelectorAll('[itemscope][itemtype*="Product"]');

products.forEach(product => {
  // properties 접근 (HTMLPropertiesCollection)
  const nameEl = product.querySelector('[itemprop="name"]');
  const priceEl = product.querySelector('[itemprop="price"]');
  console.log(nameEl.textContent, priceEl.getAttribute('content'));
});
```

> 검색엔진은 Microdata, JSON-LD, RDFa 중 JSON-LD를 선호하는 추세이지만, Microdata는 HTML과 콘텐츠가 직접 연결되어 동기화 문제가 적다는 장점이 있다.

---

## 참고 자료

- [HTML Living Standard (WHATWG)](https://html.spec.whatwg.org/multipage/)
- [HTML 파싱 명세](https://html.spec.whatwg.org/multipage/parsing.html)
- [Navigation API 명세](https://html.spec.whatwg.org/multipage/nav-history-apis.html)
- [Web Workers 명세](https://html.spec.whatwg.org/multipage/workers.html)
- [Custom Elements 명세](https://html.spec.whatwg.org/multipage/custom-elements.html)
- [Schema.org](https://schema.org/)

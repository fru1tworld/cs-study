# Chrome 확장 프로그램: Content Scripts, scripting API, Service Worker

## Chrome 확장 프로그램: Content Scripts

> 공식 문서: https://developer.chrome.com/docs/extensions/develop/concepts/content-scripts

### 1. 개요

Content Script는 확장 프로그램이 방문 중인 웹 페이지의 DOM에 직접 접근할 수 있게 해주는 JavaScript로, 페이지의 렌더링 프로세스(Isolated World)에서 실행된다. 확장 프로그램 백그라운드(Service Worker)와 페이지 자체 JavaScript 사이의 다리 역할을 한다.

```
[웹 페이지의 JS 세계]        [Content Script Isolated World]        [확장 프로그램 SW]
   window, document 공유         같은 DOM 참조,                        chrome.* API 대부분 접근 가능
   확장 API 접근 불가             독립된 JS 변수/함수 스코프              DOM 직접 접근 불가
         ▲                              │      ▲
         └── postMessage/DOM 이벤트 ─────┘      └── chrome.runtime.sendMessage ──┘
```

---

### 2. Isolated World 개념

Content Script는 페이지와 같은 DOM을 보고 조작할 수 있지만, JavaScript 실행 컨텍스트(변수, 함수, 프로토타입 체인)는 페이지와 완전히 분리되어 있다.

| 항목 | 공유 여부 |
|------|-----------|
| DOM 트리 | 공유 (같은 노드를 읽고 쓸 수 있음) |
| `window` 객체의 커스텀 속성 | 공유 안 됨 (페이지가 설정한 전역 변수는 Content Script에서 보이지 않음) |
| 페이지에 설치된 JS 라이브러리 | 공유 안 됨 (같은 라이브러리를 다시 로드해야 함) |
| `chrome.*` API | Content Script는 제한된 일부만 접근 가능 (`chrome.runtime` 등), 대부분은 SW에 위임 |

페이지의 JS 변수/함수에 직접 접근해야 한다면 `<script>` 태그를 페이지에 주입해 Main World에서 실행하거나, `world: "MAIN"` 옵션(scripting API)을 사용해야 한다.

---

### 3. 선언적(Manifest 기반) vs 프로그래밍적(scripting API) 주입

| 방식 | 선언 위치 | 특징 |
|------|-----------|------|
| 정적(Static) | `manifest.json`의 `content_scripts` 필드 | URL 패턴에 매칭되는 모든 페이지에 자동 주입, 코드 변경 시 확장 재로드 필요 |
| 동적(Dynamic) | `chrome.scripting.executeScript()` 등 런타임 API | 특정 조건(버튼 클릭 등)에서만 원하는 탭에 주입, 세밀한 제어 가능 ([scripting-api.md](./scripting-api.md) 참고) |

```json
{
  "content_scripts": [
    {
      "matches": ["https://example.com/*"],
      "js": ["content.js"],
      "run_at": "document_idle"
    }
  ]
}
```

| `run_at` 값 | 실행 시점 |
|-------------|-----------|
| `document_start` | CSS 로딩 직후, DOM 생성 전 |
| `document_end` | DOM 생성 완료, 이미지/서브프레임 로딩 전 |
| `document_idle` (기본값) | `document_end` 이후 유휴 시점 (가장 흔히 사용) |

---

### 4. Content Script와 SW 간 통신

```javascript
// content.js → background service worker
chrome.runtime.sendMessage({ type: "PAGE_INFO", title: document.title });

// background.js
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.type === "PAGE_INFO") {
    console.log(`탭 ${sender.tab.id}: ${message.title}`);
  }
});
```

지속적인 양방향 통신이 필요하면 `chrome.runtime.connect()`로 포트를 열어 사용한다.

---

### 5. 페이지 JS와의 통신 (Main World 브리지)

Isolated World는 페이지 JS 변수에 직접 접근할 수 없으므로, 필요하면 `window.postMessage` 또는 커스텀 DOM 이벤트로 우회한다.

```javascript
// content script (Isolated World)
window.addEventListener("message", (event) => {
  if (event.source !== window || event.data?.type !== "FROM_PAGE") return;
  console.log("페이지로부터 수신:", event.data.payload);
});

// 페이지에 주입된 스크립트 (Main World)에서
window.postMessage({ type: "FROM_PAGE", payload: "hello" }, "*");
```

---

### 6. 권한과 매칭 패턴

| 매니페스트 필드 | 역할 |
|------------------|------|
| `matches` | Content Script를 주입할 URL 패턴 |
| `exclude_matches` | 제외할 URL 패턴 |
| `all_frames` | `true`면 모든 iframe에도 주입 (기본값 `false`, 최상위 프레임만) |
| `host_permissions` | 동적 주입(`scripting.executeScript`)을 사용하려는 출처에 별도로 필요 |

---

### 7. 요약

- Content Script는 페이지 DOM에는 접근하지만 페이지 JS 컨텍스트와는 분리된 Isolated World에서 실행된다.
- 정적 주입(`manifest.json`)과 동적 주입(`scripting` API) 두 방식이 있으며, 세밀한 제어가 필요하면 동적 주입을 쓴다.
- SW와는 메시지 패싱으로, 페이지 JS와는 `postMessage`/DOM 이벤트로 통신한다.
- `run_at` 값으로 주입 시점을 제어하고, `all_frames`로 iframe 포함 여부를 결정한다.

---

### 참고 자료

- [Content scripts (Chrome 공식)](https://developer.chrome.com/docs/extensions/develop/concepts/content-scripts)
- [scripting API](./scripting-api.md)

---

## Chrome 확장 프로그램: chrome.scripting API

> 공식 문서: https://developer.chrome.com/docs/extensions/reference/api/scripting

### 1. 개요

`chrome.scripting`은 Manifest V3에서 Content Script를 런타임에 동적으로 주입/제거하고, CSS를 삽입/제거하는 API다. Manifest V2의 `chrome.tabs.executeScript`를 대체하며, 문자열 코드 실행(`code` 파라미터)을 없애고 함수/파일 기반 주입만 허용해 CSP·보안성을 강화했다.

---

### 2. 주요 메서드

| 메서드 | 역할 |
|--------|------|
| `executeScript()` | 지정한 탭/프레임에 JS 함수 또는 파일을 주입해 실행 |
| `insertCSS()` | 지정한 탭/프레임에 CSS 삽입 |
| `removeCSS()` | 삽입했던 CSS 제거 |
| `registerContentScripts()` | 런타임에 조건부로 Content Script 등록 (매니페스트 수정 없이) |
| `unregisterContentScripts()` | 등록한 동적 Content Script 해제 |
| `getRegisteredContentScripts()` | 현재 등록된 동적 Content Script 목록 조회 |

---

### 3. executeScript 예시

```javascript
// 특정 탭에 함수를 주입해 실행하고 반환값을 받음
const [{ result }] = await chrome.scripting.executeScript({
  target: { tabId, allFrames: false },
  func: () => document.title,
});
console.log(result); // 페이지 제목

// 함수에 인자 전달
await chrome.scripting.executeScript({
  target: { tabId },
  func: (color) => { document.body.style.backgroundColor = color; },
  args: ["lightblue"],
});

// 별도 파일 주입
await chrome.scripting.executeScript({
  target: { tabId },
  files: ["content.js"],
});
```

| 옵션 | 설명 |
|------|------|
| `target.tabId` | 주입 대상 탭 |
| `target.frameIds` | 특정 프레임만 지정 (생략 시 최상위 프레임) |
| `target.allFrames` | `true`면 탭 내 모든 프레임에 주입 |
| `world` | `"ISOLATED"`(기본) 또는 `"MAIN"` — MAIN이면 페이지 JS 컨텍스트에서 직접 실행 |
| `injectImmediately` | `true`면 `document_idle`을 기다리지 않고 즉시 주입 |

---

### 4. 실행 환경(World) 선택

```javascript
// MAIN world에서 실행 → 페이지의 전역 변수/함수에 직접 접근 가능
await chrome.scripting.executeScript({
  target: { tabId },
  world: "MAIN",
  func: () => window.someAppInternalState,
});
```

Content Script의 Isolated World와 달리 `world: "MAIN"`으로 실행한 코드는 페이지 JS와 같은 컨텍스트를 공유하므로, 페이지가 정의한 함수/객체에 바로 접근할 수 있다. 다만 이 경우 페이지의 CSP 정책이 그대로 적용된다.

---

### 5. 권한 요구사항

| 권한 | 필요 시점 |
|------|-----------|
| `"scripting"` | `chrome.scripting` API 자체 사용 |
| `host_permissions` (예: `"https://*/*"`) | 실제 대상 탭의 출처에 대한 접근 권한 |
| `activeTab` | 사용자가 확장 아이콘을 클릭한 현재 탭에 한해 임시로 `host_permissions` 없이 실행 허용 |

```json
{
  "permissions": ["scripting", "activeTab"],
  "host_permissions": ["https://example.com/*"]
}
```

`activeTab`을 쓰면 광범위한 `host_permissions`를 요청하지 않고도 "사용자가 명시적으로 클릭한 탭"에만 최소 권한으로 스크립트를 주입할 수 있어, 스토어 심사와 사용자 신뢰 측면에서 권장된다.

---

### 6. 동적 Content Script 등록

```javascript
await chrome.scripting.registerContentScripts([
  {
    id: "highlight-script",
    matches: ["https://example.com/*"],
    js: ["highlight.js"],
    runAt: "document_idle",
  },
]);
```

매니페스트 재배포 없이 조건에 따라 Content Script를 등록/해제할 수 있어, 사용자가 설정에서 기능을 켜고 끄는 형태의 확장 프로그램에 유용하다.

---

### 7. Manifest V2 `executeScript`와의 차이

| 항목 | Manifest V2 (`chrome.tabs.executeScript`) | Manifest V3 (`chrome.scripting.executeScript`) |
|------|---------------------------------------------|---------------------------------------------------|
| 문자열 코드 실행 | `code: "..."` 문자열 직접 실행 가능 | 제거됨 — `func` 또는 `files`만 허용 |
| CSP 영향 | 문자열 eval에 준하는 위험 존재 | 함수 직렬화 방식으로 보안성 강화 |
| 반환값 처리 | 콜백 기반 | Promise 기반, `InjectionResult[]` 반환 |

---

### 8. 요약

- `chrome.scripting`은 Manifest V3에서 동적 스크립트/CSS 주입을 담당하는 API다.
- `func`/`files` 기반 주입만 허용해 문자열 eval 위험을 없앴다.
- `world: "MAIN"`으로 페이지 JS 컨텍스트에 직접 접근하는 실행도 가능하다.
- `activeTab` 권한과 조합하면 광범위한 host 권한 없이도 최소 권한 원칙을 지킬 수 있다.

---

### 참고 자료

- [scripting API (Chrome 공식)](https://developer.chrome.com/docs/extensions/reference/api/scripting)
- [Content scripts](./content-scripts.md)

---

## Chrome 확장 프로그램: Service Worker Lifecycle

> 공식 문서: https://developer.chrome.com/docs/extensions/develop/concepts/service-workers/lifecycle

### 1. 개요

Manifest V3부터 확장 프로그램의 백그라운드 실행 환경은 상시 상주하던 Background Page(Manifest V2) 대신 이벤트 기반으로 실행/종료되는 Service Worker로 바뀌었다. Service Worker는 필요할 때 깨어나 이벤트를 처리하고, 일정 시간 유휴 상태가 지속되면 브라우저가 종료해 메모리를 회수한다. 이는 확장 프로그램의 코드가 "항상 살아있다"고 가정할 수 없다는 것을 의미하며, 상태 관리 방식 자체를 바꿔야 한다.

```
[이벤트 발생] → SW 시작(cold start) → 이벤트 리스너 실행 → 유휴(idle) → 약 30초 후 종료
      ↑                                                              │
      └────────────────── 다음 이벤트 발생 시 재시작 ───────────────────┘
```

---

### 2. 생명주기 상태

| 상태 | 설명 |
|------|------|
| Installed | 확장 프로그램 설치/업데이트 직후 |
| Starting | 이벤트 수신으로 SW가 새로 시작되는 중 |
| Running | 이벤트 리스너 콜백 실행 중 |
| Idle | 처리할 작업이 없어 대기 중 (약 30초 후 종료됨) |
| Stopped | 완전히 종료되어 메모리에서 사라짐 |

---

### 3. 전역 상태를 유지할 수 없는 문제

SW가 종료되면 메모리상의 전역 변수, 타이머, 진행 중인 클로저 상태가 모두 사라진다. 따라서 세션 간 유지해야 하는 데이터는 다음 방법으로 영속화해야 한다.

| 저장 방식 | 용도 |
|-----------|------|
| `chrome.storage.session` | SW 재시작에도 유지되지만 브라우저 종료 시 사라지는 메모리 기반 저장소 |
| `chrome.storage.local` / `chrome.storage.sync` | 브라우저 재시작 후에도 유지되는 영속 저장소 |
| `IndexedDB` | 대용량 구조화 데이터 |
| `setTimeout`/`setInterval` 사용 금지 | SW가 종료되면 타이머도 사라짐 → `chrome.alarms` API로 대체 |

```javascript
// 잘못된 패턴 - SW 종료 시 타이머 소실
setTimeout(() => doSomething(), 60_000);

// 올바른 패턴 - alarms API 사용
chrome.alarms.create('myAlarm', { delayInMinutes: 1 });
chrome.alarms.onAlarm.addListener((alarm) => {
  if (alarm.name === 'myAlarm') doSomething();
});
```

---

### 4. SW를 살아있게 유지하는 요인

| 요인 | 설명 |
|------|------|
| 실행 중인 이벤트 콜백 | 콜백이 반환되기 전까지는 종료되지 않음 (단, 콜백은 빠르게 끝나야 함) |
| 진행 중인 네트워크 요청 (`fetch`) | 요청이 완료될 때까지 SW 유지 |
| `chrome.debugger`로 부착된 디버깅 세션 | 부착이 살아있는 동안 SW 종료 방지 (Chrome 118부터 명시적 keepalive 동작 변경, `chrome-debugger-and-keepalive.md` 참고) |
| 메시지 포트(`chrome.runtime.connect`) 연결 | 포트가 열려 있는 동안 유지 |

---

### 5. 개발/디버깅 시 유의점

| 항목 | 권장 사항 |
|------|-----------|
| 상태 초기화 로직 | SW가 재시작될 때마다 필요한 상태를 다시 계산하거나 storage에서 복원하도록 설계 |
| 긴 작업 처리 | SW 내부에서 장시간 연산을 돌리지 말고, 필요하면 `offscreen` 문서나 별도 탭으로 위임 |
| 이벤트 리스너 등록 위치 | 최상위 스코프에서 동기적으로 등록해야 SW 재시작 후에도 이벤트를 놓치지 않음 (비동기 코드 안에서 등록하면 누락 위험) |
| DevTools 강제 종료 | `chrome://extensions`에서 SW를 수동으로 종료시켜 재시작 시나리오를 테스트 가능 |

---

### 6. 요약

- Manifest V3 Service Worker는 이벤트 기반으로 시작/종료되며 상시 상주하지 않는다.
- 전역 변수/타이머에 의존한 상태 관리는 동작하지 않으므로 `chrome.storage`, `chrome.alarms`로 대체해야 한다.
- 이벤트 리스너는 최상위 스코프에서 동기적으로 등록해야 재시작 후에도 이벤트를 수신할 수 있다.
- `chrome.debugger` 연결, 진행 중인 fetch, 열린 메시지 포트는 SW 종료를 지연시키는 요인이다.

---

### 참고 자료

- [Service worker lifecycle (Chrome 공식)](https://developer.chrome.com/docs/extensions/develop/concepts/service-workers/lifecycle)
- [chrome.debugger와 keepalive](../automation-internals/chrome-debugger-and-keepalive.md)

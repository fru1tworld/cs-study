# Chrome 확장 프로그램: Content Scripts

> 공식 문서: https://developer.chrome.com/docs/extensions/develop/concepts/content-scripts

## 1. 개요

Content Script는 확장 프로그램이 방문 중인 웹 페이지의 DOM에 직접 접근할 수 있게 해주는 JavaScript로, 페이지의 렌더링 프로세스(Isolated World)에서 실행된다. 확장 프로그램 백그라운드(Service Worker)와 페이지 자체 JavaScript 사이의 다리 역할을 한다.

```
[웹 페이지의 JS 세계]        [Content Script Isolated World]        [확장 프로그램 SW]
   window, document 공유         같은 DOM 참조,                        chrome.* API 대부분 접근 가능
   확장 API 접근 불가             독립된 JS 변수/함수 스코프              DOM 직접 접근 불가
         ▲                              │      ▲
         └── postMessage/DOM 이벤트 ─────┘      └── chrome.runtime.sendMessage ──┘
```

---

## 2. Isolated World 개념

Content Script는 페이지와 같은 DOM을 보고 조작할 수 있지만, JavaScript 실행 컨텍스트(변수, 함수, 프로토타입 체인)는 페이지와 완전히 분리되어 있다.

| 항목 | 공유 여부 |
|------|-----------|
| DOM 트리 | 공유 (같은 노드를 읽고 쓸 수 있음) |
| `window` 객체의 커스텀 속성 | 공유 안 됨 (페이지가 설정한 전역 변수는 Content Script에서 보이지 않음) |
| 페이지에 설치된 JS 라이브러리 | 공유 안 됨 (같은 라이브러리를 다시 로드해야 함) |
| `chrome.*` API | Content Script는 제한된 일부만 접근 가능 (`chrome.runtime` 등), 대부분은 SW에 위임 |

페이지의 JS 변수/함수에 직접 접근해야 한다면 `<script>` 태그를 페이지에 주입해 Main World에서 실행하거나, `world: "MAIN"` 옵션(scripting API)을 사용해야 한다.

---

## 3. 선언적(Manifest 기반) vs 프로그래밍적(scripting API) 주입

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

## 4. Content Script와 SW 간 통신

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

## 5. 페이지 JS와의 통신 (Main World 브리지)

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

## 6. 권한과 매칭 패턴

| 매니페스트 필드 | 역할 |
|------------------|------|
| `matches` | Content Script를 주입할 URL 패턴 |
| `exclude_matches` | 제외할 URL 패턴 |
| `all_frames` | `true`면 모든 iframe에도 주입 (기본값 `false`, 최상위 프레임만) |
| `host_permissions` | 동적 주입(`scripting.executeScript`)을 사용하려는 출처에 별도로 필요 |

---

## 7. 요약

- Content Script는 페이지 DOM에는 접근하지만 페이지 JS 컨텍스트와는 분리된 Isolated World에서 실행된다.
- 정적 주입(`manifest.json`)과 동적 주입(`scripting` API) 두 방식이 있으며, 세밀한 제어가 필요하면 동적 주입을 쓴다.
- SW와는 메시지 패싱으로, 페이지 JS와는 `postMessage`/DOM 이벤트로 통신한다.
- `run_at` 값으로 주입 시점을 제어하고, `all_frames`로 iframe 포함 여부를 결정한다.

---

## 참고 자료

- [Content scripts (Chrome 공식)](https://developer.chrome.com/docs/extensions/develop/concepts/content-scripts)
- [scripting API](./scripting-api.md)

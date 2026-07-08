# Chrome 확장 프로그램: chrome.scripting API

> 공식 문서: https://developer.chrome.com/docs/extensions/reference/api/scripting

## 1. 개요

`chrome.scripting`은 Manifest V3에서 Content Script를 런타임에 동적으로 주입/제거하고, CSS를 삽입/제거하는 API다. Manifest V2의 `chrome.tabs.executeScript`를 대체하며, 문자열 코드 실행(`code` 파라미터)을 없애고 함수/파일 기반 주입만 허용해 CSP·보안성을 강화했다.

---

## 2. 주요 메서드

| 메서드 | 역할 |
|--------|------|
| `executeScript()` | 지정한 탭/프레임에 JS 함수 또는 파일을 주입해 실행 |
| `insertCSS()` | 지정한 탭/프레임에 CSS 삽입 |
| `removeCSS()` | 삽입했던 CSS 제거 |
| `registerContentScripts()` | 런타임에 조건부로 Content Script 등록 (매니페스트 수정 없이) |
| `unregisterContentScripts()` | 등록한 동적 Content Script 해제 |
| `getRegisteredContentScripts()` | 현재 등록된 동적 Content Script 목록 조회 |

---

## 3. executeScript 예시

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

## 4. 실행 환경(World) 선택

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

## 5. 권한 요구사항

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

## 6. 동적 Content Script 등록

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

## 7. Manifest V2 `executeScript`와의 차이

| 항목 | Manifest V2 (`chrome.tabs.executeScript`) | Manifest V3 (`chrome.scripting.executeScript`) |
|------|---------------------------------------------|---------------------------------------------------|
| 문자열 코드 실행 | `code: "..."` 문자열 직접 실행 가능 | 제거됨 — `func` 또는 `files`만 허용 |
| CSP 영향 | 문자열 eval에 준하는 위험 존재 | 함수 직렬화 방식으로 보안성 강화 |
| 반환값 처리 | 콜백 기반 | Promise 기반, `InjectionResult[]` 반환 |

---

## 8. 요약

- `chrome.scripting`은 Manifest V3에서 동적 스크립트/CSS 주입을 담당하는 API다.
- `func`/`files` 기반 주입만 허용해 문자열 eval 위험을 없앴다.
- `world: "MAIN"`으로 페이지 JS 컨텍스트에 직접 접근하는 실행도 가능하다.
- `activeTab` 권한과 조합하면 광범위한 host 권한 없이도 최소 권한 원칙을 지킬 수 있다.

---

## 참고 자료

- [scripting API (Chrome 공식)](https://developer.chrome.com/docs/extensions/reference/api/scripting)
- [Content scripts](./content-scripts.md)

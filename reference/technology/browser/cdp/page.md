# CDP: Page 도메인

> 공식 문서: https://chromedevtools.github.io/devtools-protocol/tot/Page/

## 1. 개요

CDP `Page` 도메인은 탭(페이지) 단위의 생명주기 제어와 상태 조회를 담당한다. 네비게이션, 로드 이벤트, 스크린샷/PDF 캡처, 다이얼로그 처리, 프레임 트리 조회 등 "탭 자체"를 다루는 기능이 여기에 모여 있다.

---

## 2. 핵심 메서드

| 메서드 | 역할 |
|--------|------|
| `navigate` | 지정한 URL로 이동 |
| `reload` | 페이지 새로고침 (캐시 무시 옵션 포함) |
| `captureScreenshot` | 현재 화면을 이미지로 캡처 |
| `printToPDF` | 페이지를 PDF로 렌더링 |
| `getFrameTree` | 현재 탭의 프레임(메인 프레임 + iframe) 구조 조회 |
| `setBypassCSP` | 테스트 목적으로 페이지의 CSP 정책을 우회 |
| `addScriptToEvaluateOnNewDocument` | 새 문서가 생성될 때마다 자동으로 스크립트 주입 (네비게이션 전에 선주입) |
| `handleJavaScriptDialog` | `alert`/`confirm`/`prompt` 다이얼로그 자동 처리 |

---

## 3. 로드 생명주기 이벤트

```
navigate() 호출
     │
     ▼
frameStartedLoading
     │
     ▼
domContentEventFired      ← DOMContentLoaded와 대응
     │
     ▼
loadEventFired            ← window.onload와 대응
     │
     ▼
frameStoppedLoading
```

| 이벤트 | 대응하는 DOM 이벤트 |
|--------|------------------------|
| `domContentEventFired` | `DOMContentLoaded` |
| `loadEventFired` | `load` |
| `frameNavigated` | 프레임의 URL이 바뀔 때 (SPA 라우팅 포함 여부는 네비게이션 종류에 따라 다름) |
| `lifecycleEvent` | `init`, `firstPaint`, `networkIdle` 등 세분화된 렌더링 단계 |

자동화 스크립트는 보통 `navigate()` 호출 후 `loadEventFired` 또는 `lifecycleEvent`(`networkIdle`)를 기다린 다음 DOM 조작을 시작한다.

---

## 4. 프레임 트리와 OOPIF

```javascript
const { frameTree } = await client.send("Page.getFrameTree");
```

```json
{
  "frame": { "id": "main-frame-id", "url": "https://example.com" },
  "childFrames": [
    { "frame": { "id": "iframe-id", "url": "https://ads.example.com/widget" } }
  ]
}
```

크로스 오리진 iframe은 Site Isolation 정책에 따라 별도 렌더링 프로세스(OOPIF)에서 실행될 수 있어, 해당 프레임 내부의 DOM/Accessibility 정보를 얻으려면 [Target 도메인](./target.md)으로 별도 세션을 맺어야 하는 경우가 있다. 자세한 내용은 [OOPIF / Site Isolation](../automation-internals/oopif-site-isolation.md) 문서를 참고한다.

---

## 5. 다이얼로그 처리

페이지가 `window.alert()`, `confirm()`, `prompt()`를 호출하면 브라우저 UI 스레드가 블로킹되어 다른 CDP 명령도 멈출 수 있다. 이를 막기 위해 `Page.javascriptDialogOpening` 이벤트를 구독하고 `Page.handleJavaScriptDialog`로 즉시 응답하는 패턴을 사용한다.

```javascript
client.on("Page.javascriptDialogOpening", async () => {
  await client.send("Page.handleJavaScriptDialog", { accept: true });
});
```

---

## 6. 스크린샷/PDF 캡처

```javascript
const { data } = await client.send("Page.captureScreenshot", {
  format: "png",
  captureBeyondViewport: true,
});
// data는 base64 인코딩된 이미지
```

| 옵션 | 설명 |
|------|------|
| `format` | `png` / `jpeg` / `webp` |
| `clip` | 특정 영역만 캡처 (x, y, width, height, scale) |
| `captureBeyondViewport` | 뷰포트 밖 콘텐츠도 포함해 전체 페이지 캡처 |

---

## 7. 요약

- `Page` 도메인은 탭 단위 네비게이션, 로드 생명주기, 캡처, 다이얼로그 처리를 담당한다.
- 로드 완료 판단은 `loadEventFired` 또는 `lifecycleEvent`로 감지하는 것이 일반적이다.
- 프레임 트리는 `getFrameTree`로 조회하며, 크로스 오리진 자식 프레임은 별도 `Target` 세션이 필요할 수 있다.
- `alert`/`confirm` 같은 블로킹 다이얼로그는 이벤트 구독 후 즉시 응답하지 않으면 세션 전체가 멈출 수 있다.

---

## 참고 자료

- [CDP Page 도메인 (공식)](https://chromedevtools.github.io/devtools-protocol/tot/Page/)
- [DOM 도메인](./dom.md)
- [Target 도메인](./target.md)
- [OOPIF / Site Isolation](../automation-internals/oopif-site-isolation.md)

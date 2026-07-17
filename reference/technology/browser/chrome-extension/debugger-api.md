# Chrome 확장 프로그램: chrome.debugger API

> 공식 문서: https://developer.chrome.com/docs/extensions/reference/api/debugger

## 1. 개요

`chrome.debugger`는 확장 프로그램이 [Chrome DevTools Protocol(CDP)](../cdp/README.md)에 직접 접근할 수 있게 해주는 API다. DevTools가 내부적으로 CDP를 사용해 페이지를 검사·조작하는 것처럼, 확장 프로그램도 이 API를 통해 탭에 "부착(attach)"해서 CDP 명령을 보내고 이벤트를 받을 수 있다. 브라우저 자동화 도구, 네트워크 모니터링 확장, 성능 프로파일러 등이 이 API를 기반으로 동작한다.

```
확장 프로그램 (SW)
     │  chrome.debugger.attach({tabId}, "1.3")
     ▼
CDP 세션 수립
     │  chrome.debugger.sendCommand(target, "Page.navigate", {url})
     ▼
대상 탭 (Renderer)
```

---

## 2. 핵심 메서드

| 메서드 | 역할 |
|--------|------|
| `attach(target, requiredVersion)` | 지정한 탭/워커에 디버거로 부착 |
| `detach(target)` | 부착 해제 |
| `sendCommand(target, method, params)` | CDP 메서드 호출 (예: `Page.navigate`, `Input.dispatchMouseEvent`) |
| `getTargets()` | 디버깅 가능한 대상(탭, 확장, 워커 등) 목록 조회 |
| `onEvent` | CDP 이벤트 수신 리스너 |
| `onDetach` | 세션이 예기치 않게 끊겼을 때(사용자가 DevTools를 직접 열었을 경우 등) 호출 |

```javascript
const target = { tabId };
await chrome.debugger.attach(target, "1.3");
await chrome.debugger.sendCommand(target, "Page.enable");

chrome.debugger.onEvent.addListener((source, method, params) => {
  if (method === "Page.loadEventFired") {
    console.log("페이지 로드 완료", params);
  }
});

await chrome.debugger.sendCommand(target, "Page.navigate", {
  url: "https://example.com",
});
```

---

## 3. 권한과 사용자 고지

| 요구사항 | 설명 |
|----------|------|
| `"debugger"` 권한 | manifest에 명시 필요 |
| 상단 배너 표시 | Chrome은 디버거가 부착된 탭에 "확장 프로그램이 이 탭을 디버깅하고 있습니다" 배너를 강제로 표시해 사용자에게 알림 (숨길 수 없음) |
| 단일 세션 제약 | 한 탭에는 한 번에 하나의 디버거만 부착 가능 (DevTools를 직접 열면 확장의 세션과 충돌) |

이 배너는 사용자가 보이지 않는 원격 제어를 인지하지 못하는 것을 막기 위한 보안 장치이며, 스토어 심사에서도 디버거 권한 사용 사유를 명확히 요구한다.

---

## 4. Service Worker keepalive와의 관계

`chrome.debugger`로 세션이 활성 상태인 동안에는 확장 SW가 유휴 종료되지 않도록 브라우저가 생명주기를 연장해 왔다. 다만 Chrome 118 전후로 이 동작이 조정되어, 디버거 연결만으로 SW가 무기한 유지된다고 가정할 수 없게 됐다 — 자세한 변경 이력은 [chrome-debugger-and-keepalive.md](../automation-internals/chrome-debugger-and-keepalive.md)에서 다룬다.

---

## 5. 대표 활용 사례

| 사용 사례 | 사용하는 CDP 도메인 |
|-----------|------------------------|
| 헤드리스 브라우저 자동화(확장 기반) | `Page`, `Input`, `DOM`, `Runtime` |
| 네트워크 요청 가로채기/수정 | `Network`, `Fetch` |
| 접근성 트리 검사 확장 | `Accessibility` |
| 성능 트레이스 수집 | `Tracing`, `Performance` |
| `chrome-devtools-mcp` 같은 MCP 서버 | `Page`, `Input`, `DOM`, `Accessibility` 등 다수 도메인 조합 |

---

## 6. 주의사항

| 항목 | 설명 |
|------|------|
| CDP 버전 호환성 | `attach()`에 지정하는 `requiredVersion`(예: `"1.3"`)과 실제 Chrome 버전의 CDP 지원 여부를 맞춰야 함 |
| 탭 네비게이션 시 컨텍스트 무효화 | 페이지 이동(`Page.navigate`) 시 이전 `Runtime.executionContextId` 등은 무효화되므로 새로 조회 필요 |
| DevTools와의 충돌 | 사용자가 F12로 DevTools를 열면 확장의 디버거 세션이 강제로 끊길 수 있음(`onDetach` 처리 필요) |
| 스토어 정책 | 디버거 권한 남용(과도한 감시, 사용자 동의 없는 자동화)은 정책 위반으로 심사 거부 사유가 됨 |

---

## 7. 요약

- `chrome.debugger`는 확장 프로그램이 CDP를 통해 탭을 프로그래밍 방식으로 제어할 수 있게 하는 API다.
- 사용 중임을 알리는 배너가 강제로 표시되어 투명성을 보장한다.
- Service Worker의 keepalive 동작과 밀접하게 연관되어 있다.
- 네트워크 가로채기, 자동화, MCP 기반 브라우저 제어 도구의 기반 기술이다.

---

## 참고 자료

- [chrome.debugger (Chrome 공식)](https://developer.chrome.com/docs/extensions/reference/api/debugger)
- [CDP 개요](../cdp/README.md)
- [chrome-debugger-and-keepalive.md](../automation-internals/chrome-debugger-and-keepalive.md)

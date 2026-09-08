# Chrome 확장 프로그램: debugger API와 네트워크 요청

## Chrome 확장 프로그램: chrome.debugger API

> 공식 문서: https://developer.chrome.com/docs/extensions/reference/api/debugger

### 1. 개요

`chrome.debugger`는 확장 프로그램에서 [Chrome DevTools Protocol(CDP)](../cdp/README.md)에 접근하는 API다. DevTools가 CDP로 페이지를 검사하고 조작하듯이, 확장 프로그램도 탭에 부착(attach)한 뒤 명령을 보내고 이벤트를 받을 수 있다. 브라우저 자동화 도구와 네트워크 모니터링 확장, 성능 프로파일러 등이 이 기능을 사용한다.

```
확장 프로그램 (SW)
     │  chrome.debugger.attach({tabId}, "1.3")
     ▼
CDP 세션 수립
     │  chrome.debugger.sendCommand(target, "Page.navigate", {url})
     ▼
대상 탭 (Renderer)
```

### 2. 핵심 메서드

- `attach(target, requiredVersion)`: 지정한 탭/워커에 디버거로 부착
- `detach(target)`: 부착 해제
- `sendCommand(target, method, params)`: CDP 메서드 호출 (예: `Page.navigate`, `Input.dispatchMouseEvent`)
- `getTargets()`: 디버깅 가능한 대상(탭, 확장, 워커 등) 목록 조회
- `onEvent`: CDP 이벤트 수신 리스너
- `onDetach`: 세션이 예기치 않게 끊겼을 때(사용자가 DevTools를 직접 열었을 경우 등) 호출

다음 예제는 탭에 부착한 뒤 `Page` 도메인을 활성화한다. 페이지 로드 이벤트를 받을 리스너를 먼저 등록하고, `Page.navigate`로 대상 URL을 연다.

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

### 3. 권한과 사용자 고지

- `"debugger"` 권한: manifest에 명시 필요
- 상단 배너 표시: Chrome은 디버거가 부착된 탭에 "확장 프로그램이 이 탭을 디버깅하고 있습니다" 배너를 강제로 표시해 사용자에게 알림 (숨길 수 없음)
- 단일 세션 제약: 한 탭에는 한 번에 하나의 디버거만 부착 가능 (DevTools를 직접 열면 확장의 세션과 충돌)

이 배너는 사용자가 모르는 사이에 탭이 원격 제어되는 상황을 막기 위한 보안 장치다. 스토어 심사에서도 디버거 권한을 사용하는 사유를 명확히 요구한다.

### 4. Service Worker keepalive와의 관계

`chrome.debugger` 세션이 활성 상태인 동안에는 확장 SW가 유휴 상태로 종료되지 않도록 브라우저가 생명주기를 연장해 왔다. 다만 Chrome 118 전후로 이 동작이 조정되어, 디버거 연결만으로 SW가 무기한 유지된다고 가정할 수는 없게 되었다. 자세한 변경 이력은 [chrome-debugger-and-keepalive.md](../automation-internals/01_chrome-debugger-and-keepalive.md)에서 다룬다.

### 5. 대표 활용 사례

- 헤드리스 브라우저 자동화(확장 기반): `Page`, `Input`, `DOM`, `Runtime` 도메인 사용
- 네트워크 요청 가로채기/수정: `Network`, `Fetch` 도메인 사용
- 접근성 트리 검사 확장: `Accessibility` 도메인 사용
- 성능 트레이스 수집: `Tracing`, `Performance` 도메인 사용
- `chrome-devtools-mcp` 같은 MCP 서버: `Page`, `Input`, `DOM`, `Accessibility` 등 다수 도메인 조합 사용

### 6. 주의사항

- CDP 버전 호환성: `attach()`에 지정하는 `requiredVersion`(예: `"1.3"`)과 실제 Chrome 버전의 CDP 지원 여부를 맞춰야 함
- 탭 네비게이션 시 컨텍스트 무효화: 페이지 이동(`Page.navigate`) 시 이전 `Runtime.executionContextId` 등은 무효화되므로 새로 조회 필요
- DevTools와의 충돌: 사용자가 F12로 DevTools를 열면 확장의 디버거 세션이 강제로 끊길 수 있음(`onDetach` 처리 필요)
- 스토어 정책: 디버거 권한 남용(과도한 감시, 사용자 동의 없는 자동화)은 정책 위반으로 심사 거부 사유가 됨

### 참고 자료

- [chrome.debugger (Chrome 공식)](https://developer.chrome.com/docs/extensions/reference/api/debugger)
- [CDP 개요](../cdp/README.md)
- [chrome-debugger-and-keepalive.md](../automation-internals/01_chrome-debugger-and-keepalive.md)

## Chrome 확장 프로그램: 네트워크 요청 다루기

> 공식 문서: https://developer.chrome.com/docs/extensions/develop/concepts/network-requests

### 1. 개요

확장 프로그램이 네트워크 요청을 관찰, 차단, 수정하는 방식은 Manifest V2의 블로킹 `webRequest` API에서 Manifest V3의 `declarativeNetRequest` 중심 모델로 크게 바뀌었다. 성능을 높이고, 확장 프로그램이 모든 요청 내용을 직접 들여다보지 못하게 해 프라이버시를 개선하려는 변화다.

### 2. 접근 방식 비교

- `webRequest` (Manifest V2, 블로킹)
  - 방식: 확장 코드가 매 요청마다 동기적으로 개입
  - 요청 내용 열람: 가능
  - 실시간 수정: 가능하지만 성능 저하 및 남용 위험
- `webRequest` (Manifest V3, 논블로킹)
  - 방식: 관찰(observe)만 가능, 차단/수정 불가
  - 요청 내용 열람: 가능
  - 실시간 수정: 불가
- `declarativeNetRequest` (DNR)
  - 방식: 브라우저에 규칙(rule)을 선언적으로 등록, 브라우저가 직접 매칭, 처리
  - 요청 내용 열람: 확장 코드는 요청 본문을 보지 않음
  - 실시간 수정: 규칙 기반으로 가능 (차단/리다이렉트/헤더 수정)

### 3. declarativeNetRequest 규칙 예시

첫 번째 규칙은 `ads.example.com`의 script, image, xmlhttprequest 요청을 차단한다. 이어지는 코드는 런타임에 동적 규칙을 추가해 `old.js` 요청을 다른 URL로 리다이렉트한다.

```json
{
  "id": 1,
  "priority": 1,
  "action": { "type": "block" },
  "condition": {
    "urlFilter": "||ads.example.com^",
    "resourceTypes": ["script", "image", "xmlhttprequest"]
  }
}
```

```javascript
await chrome.declarativeNetRequest.updateDynamicRules({
  addRules: [
    {
      id: 2,
      priority: 1,
      action: {
        type: "redirect",
        redirect: { url: "https://example.com/replacement.js" },
      },
      condition: { urlFilter: "https://cdn.example.com/old.js" },
    },
  ],
  removeRuleIds: [],
});
```

규칙은 다음 요소로 구성된다.

- `condition`: URL 패턴, 리소스 타입, 요청 메서드 등 매칭 조건
- `action.type`: `block`, `redirect`, `allow`, `modifyHeaders`, `upgradeScheme` 등
- `priority`: 여러 규칙이 겹칠 때 우선순위
- 정적 규칙 vs 동적 규칙: manifest에 JSON 파일로 등록(정적) 또는 런타임에 `updateDynamicRules`로 등록(동적)

### 4. 왜 선언적 모델로 바뀌었나

Manifest V2의 블로킹 webRequest는 매 요청마다 확장 프로세스와 IPC로 왕복해야 해서 성능이 떨어졌다. declarativeNetRequest에서는 브라우저 엔진 내부에서 규칙을 매칭하므로 확장 프로세스가 개입할 필요가 없다.

프라이버시도 문제였다. 블로킹 webRequest에서는 확장 코드가 모든 요청의 URL, 헤더, 본문을 관찰할 수 있었다. declarativeNetRequest에서는 규칙만 등록하면 되므로 확장이 사용자 트래픽을 직접 들여다볼 필요가 없다.

또한 광고 차단기처럼 모든 요청에 개입해야 하는 확장은 SW 종료와 무관하게 항상 동작해야 한다. declarativeNetRequest 규칙은 SW가 종료되어도 브라우저가 계속 적용한다.

### 5. `fetch`/`XMLHttpRequest`와 CORS

확장 프로그램 자체(백그라운드 SW)에서 보내는 네트워크 요청은 `host_permissions`에 명시된 출처에 한해 CORS 제약 없이 보낼 수 있다. 반면 Content Script에서 보내는 요청은 페이지의 CSP와 CORS 제약을 그대로 받는다.

- Service Worker (백그라운드): `host_permissions` 대상이면 CORS 우회 가능
- Content Script: 페이지와 동일한 CORS/CSP 제약 적용

### 참고 자료

- [Network requests (Chrome 공식)](https://developer.chrome.com/docs/extensions/develop/concepts/network-requests)
- [declarativeNetRequest API (Chrome 공식)](https://developer.chrome.com/docs/extensions/reference/api/declarativeNetRequest)

# chrome-devtools-mcp 개요

## 1. 개요

`chrome-devtools-mcp`는 [MCP(Model Context Protocol)](../../../standard/mcp/specification.md) 서버 형태로 Chrome DevTools Protocol을 감싸, LLM 기반 에이전트가 표준화된 도구 호출만으로 브라우저를 제어할 수 있게 해주는 도구다. 에이전트가 CDP의 저수준 WebSocket 프로토콜을 직접 다루는 대신, `navigate`, `click`, `take_screenshot` 같은 상위 수준 도구 이름으로 브라우저를 조작한다.

```
LLM 에이전트
     │  MCP 도구 호출 (예: click, navigate, take_snapshot)
     ▼
chrome-devtools-mcp (MCP 서버)
     │  CDP 명령으로 변환
     ▼
Chrome (--remote-debugging-port)
```

---

## 2. 대표적으로 제공하는 도구 범주

| 범주 | 예시 도구 | 내부적으로 쓰는 CDP 도메인 |
|------|-----------|------------------------------|
| 네비게이션 | `navigate_page`, `new_page`, `close_page` | [Page](../cdp/page.md), [Target](../cdp/target.md) |
| 요소 조작 | `click`, `fill`, `hover`, `drag` | [Input](../cdp/input.md), [DOM](../cdp/dom.md) |
| 페이지 상태 파악 | `take_snapshot`(접근성 트리 기반), `take_screenshot` | [Accessibility](../cdp/accessibility.md), [Page](../cdp/page.md) |
| 진단/디버깅 | `list_console_messages`, `list_network_requests` | Runtime, Network |
| 성능 분석 | `performance_start_trace` 등 | Tracing/Performance |

---

## 3. 좌표 기반이 아닌 스냅샷 기반 상호작용

이런 도구들이 채택하는 핵심 설계는, 에이전트에게 스크린샷 좌표를 직접 계산시키는 대신 [접근성 트리](../cdp/accessibility.md) 기반의 구조화된 스냅샷(요소의 role, name, 계층 구조)을 제공하고, 에이전트가 "이 이름의 버튼을 클릭해줘" 같은 의미 기반 참조로 도구를 호출하게 하는 것이다. 실제 좌표 계산과 클릭 이벤트 생성은 MCP 서버 내부에서 [DOM.getBoxModel](../cdp/dom.md) → [Input.dispatchMouseEvent](../cdp/input.md) 순서로 처리된다.

```
에이전트: "click 도구를 호출, 대상은 스냅샷의 노드 #14"
        │
        ▼
MCP 서버: DOM.getBoxModel(nodeId=14)로 좌표 계산
        │
        ▼
MCP 서버: Input.dispatchMouseEvent로 신뢰된 클릭 이벤트 생성
```

이 방식은 좌표가 화면 해상도/스크롤 위치에 따라 매번 바뀌는 문제와, LLM이 픽셀 좌표를 직접 추정해야 하는 부정확성을 동시에 줄인다.

---

## 4. WebMCP와의 차이

| 구분 | chrome-devtools-mcp | [WebMCP](../../../standard/w3c/webmcp.md) |
|------|------------------------|----------------------------------------------|
| 제어 방식 | 브라우저 외부에서 CDP로 "원격 조작" (사람이 마우스/키보드를 쓰듯) | 웹 페이지 자신이 함수형 도구를 직접 노출 |
| 페이지 협조 필요 여부 | 불필요 (기존 웹사이트를 그대로 자동화) | 필요 (사이트가 `navigator.modelContext`로 도구를 등록해야 함) |
| 신뢰성 | DOM 구조 변경에 상대적으로 취약 | 사이트가 명시한 안정적 인터페이스 사용 가능 |
| 적용 범위 | 임의의 기존 웹사이트 | WebMCP를 지원하도록 개발된 사이트 |

두 접근은 대립적이라기보다 보완적이다 — WebMCP를 지원하지 않는 대다수 사이트에는 `chrome-devtools-mcp` 같은 CDP 기반 자동화가, WebMCP를 지원하는 사이트에는 더 안정적인 네이티브 도구 호출이 쓰일 수 있다.

---

## 5. 요약

- `chrome-devtools-mcp`는 CDP를 MCP 도구 인터페이스로 감싸 LLM 에이전트의 브라우저 자동화를 표준화한다.
- 접근성 트리 기반 스냅샷으로 의미 기반 요소 참조를 가능하게 하고, 내부적으로 CDP `DOM`/`Input`/`Accessibility` 도메인을 조합해 동작한다.
- 페이지의 협조 없이 기존 웹사이트를 그대로 자동화한다는 점에서 WebMCP와 접근 방식이 다르다.

---

## 참고 자료

- [MCP Specification](../../../standard/mcp/specification.md)
- [WebMCP Explainer](../../../standard/w3c/webmcp.md)
- [CDP Accessibility 도메인](../cdp/accessibility.md)

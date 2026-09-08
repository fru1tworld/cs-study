# chrome-devtools-mcp 개요

## 1. 개요

`chrome-devtools-mcp`는 Chrome DevTools Protocol을 [MCP(Model Context Protocol)](../../../../standard/mcp/specification.md) 도구로 제공하는 서버다. LLM 기반 에이전트는 CDP의 저수준 WebSocket 프로토콜을 직접 다룰 필요 없이 `navigate`, `click`, `take_screenshot` 같은 상위 수준 도구를 호출해 브라우저를 제어한다.

```
LLM 에이전트
     │  MCP 도구 호출 (예: click, navigate, take_snapshot)
     ▼
chrome-devtools-mcp (MCP 서버)
     │  CDP 명령으로 변환
     ▼
Chrome (--remote-debugging-port)
```

## 2. 대표적으로 제공하는 도구 범주

- 네비게이션
  - 예시 도구: `navigate_page`, `new_page`, `close_page`
  - 내부적으로 쓰는 CDP 도메인: [Page](../cdp/03_page.md), [Target](../cdp/02_target.md)
- 요소 조작
  - 예시 도구: `click`, `fill`, `hover`, `drag`
  - 내부적으로 쓰는 CDP 도메인: [Input](../cdp/05_input.md), [DOM](../cdp/04_dom.md)
- 페이지 상태 파악
  - 예시 도구: `take_snapshot`(접근성 트리 기반), `take_screenshot`
  - 내부적으로 쓰는 CDP 도메인: [Accessibility](../cdp/06_accessibility.md), [Page](../cdp/03_page.md)
- 진단/디버깅
  - 예시 도구: `list_console_messages`, `list_network_requests`
  - 내부적으로 쓰는 CDP 도메인: Runtime, Network
- 성능 분석
  - 예시 도구: `performance_start_trace` 등
  - 내부적으로 쓰는 CDP 도메인: Tracing/Performance

## 3. 좌표 기반이 아닌 스냅샷 기반 상호작용

이런 도구들은 에이전트에게 스크린샷 좌표를 직접 계산시키지 않는다. 대신 [접근성 트리](../cdp/06_accessibility.md) 기반의 구조화된 스냅샷(요소의 role, name, 계층 구조)을 제공하고, 에이전트가 "이 이름의 버튼을 클릭해줘" 같은 의미 기반 참조로 도구를 호출하게 한다. 실제 좌표 계산과 클릭 이벤트 생성은 MCP 서버 내부에서 [DOM.getBoxModel](../cdp/04_dom.md) → [Input.dispatchMouseEvent](../cdp/05_input.md) 순서로 처리한다.

```
에이전트: "click 도구를 호출, 대상은 스냅샷의 노드 #14"
        │
        ▼
MCP 서버: DOM.getBoxModel(nodeId=14)로 좌표 계산
        │
        ▼
MCP 서버: Input.dispatchMouseEvent로 신뢰된 클릭 이벤트 생성
```

이 방식은 좌표가 화면 해상도와 스크롤 위치에 따라 매번 바뀌는 문제와, LLM이 픽셀 좌표를 직접 추정해야 하는 부정확성을 함께 줄인다.

## 4. WebMCP와의 차이

- 제어 방식
  - chrome-devtools-mcp: 브라우저 외부에서 CDP로 "원격 조작"(사람이 마우스/키보드를 쓰듯)
  - [WebMCP](../../../../standard/w3c/webmcp.md): 웹 페이지 자신이 함수형 도구를 직접 노출
- 페이지 협조 필요 여부
  - chrome-devtools-mcp: 불필요(기존 웹사이트를 그대로 자동화)
  - WebMCP: 필요(사이트가 `navigator.modelContext`로 도구를 등록해야 함)
- 신뢰성
  - chrome-devtools-mcp: DOM 구조 변경에 상대적으로 취약
  - WebMCP: 사이트가 명시한 안정적 인터페이스 사용 가능
- 적용 범위
  - chrome-devtools-mcp: 임의의 기존 웹사이트
  - WebMCP: WebMCP를 지원하도록 개발된 사이트

사이트의 지원 여부에 따라 두 접근을 함께 사용할 수 있다. WebMCP를 지원하지 않는 대다수 사이트에는 `chrome-devtools-mcp` 같은 CDP 기반 자동화를 적용하고, 지원하는 사이트에서는 더 안정적인 네이티브 도구 호출을 사용할 수 있다.

## 참고 자료

- [MCP Specification](../../../../standard/mcp/specification.md)
- [WebMCP Explainer](../../../../standard/w3c/webmcp.md)
- [CDP Accessibility 도메인](../cdp/06_accessibility.md)

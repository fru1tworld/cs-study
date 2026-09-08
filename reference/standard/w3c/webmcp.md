# WebMCP Explainer (WICG Draft)

> Web Incubator Community Group(WICG) 초안, 표준화 초기 단계

## 1. 개요

WebMCP는 웹사이트가 자신의 기능을 AI 에이전트에 구조화된 도구(tool)로 제공하는 웹 플랫폼 API 제안이다. Anthropic의 [Model Context Protocol(MCP)](../mcp/specification.md) 개념을 브라우저 네이티브 API로 가져오려는 시도다.

에이전트가 DOM을 분석하거나 좌표를 클릭해 기능을 찾아가는 방식은 화면 구조에 의존한다. WebMCP에서는 사이트가 제공할 기능을 함수로 선언하고, 에이전트가 그 함수를 직접 호출하도록 한다.

```
기존 방식 (DOM 기반 자동화)
  에이전트 → 페이지 스냅샷 분석 → 좌표 클릭/입력 → 결과 재분석

WebMCP 방식
  에이전트 → navigator.modelContext.tools 목록 조회 → 함수 직접 호출 → 구조화된 결과 반환
```

---

## 2. 제안된 API 형태

아직 초안 단계라 세부 명세는 바뀔 수 있다. 도구를 등록하는 형태는 다음 예제와 같다.

```javascript
// 사이트가 스스로 도구를 등록
navigator.modelContext.registerTool({
  name: "add_to_cart",
  description: "지정한 상품을 장바구니에 추가한다",
  inputSchema: {
    type: "object",
    properties: {
      productId: { type: "string" },
      quantity: { type: "number" }
    },
    required: ["productId"]
  },
  execute: async ({ productId, quantity = 1 }) => {
    await cart.add(productId, quantity);
    return { success: true, cartTotal: cart.total };
  }
});
```

- 요소별 역할
  - `name` / `description`: 에이전트가 도구의 용도를 이해하는 근거
  - `inputSchema`: MCP와 마찬가지로 JSON Schema로 입력 형식을 정의
  - `execute`: 실제로 페이지 내부 상태를 조작하는 함수 → 에이전트가 직접 DOM을 건드리지 않음

명세와 구현의 차이도 있다. WICG Explainer는 API를 `partial interface Document { readonly attribute ModelContext modelContext; }`로 정의해 `document.modelContext`로 접근하도록 한다. 반면 Chrome의 Origin Trial 구현은 `navigator.modelContext`를 사용한다. 아직 초안이라 명세와 구현이 다를 수 있다. 여기서는 Chrome에서 사용하는 `navigator.modelContext`를 기준으로 설명한다.

---

## 3. 왜 필요한가

- 신뢰성
  - DOM 기반 자동화의 한계: 레이아웃이나 셀렉터가 바뀌면 영향을 받음
  - WebMCP가 노리는 개선: 페이지가 명시한 안정적인 함수 인터페이스 사용
- 성능
  - DOM 기반 자동화의 한계: 스크린샷과 접근성 트리를 분석하는 비용이 큼
  - WebMCP가 노리는 개선: 함수 호출 한 번으로 처리
- 의도 파악
  - DOM 기반 자동화의 한계: 좌표와 텍스트만으로 버튼의 실제 기능을 추론해야 함
  - WebMCP가 노리는 개선: 사이트가 `description`으로 의미를 직접 제공
- 보안/권한
  - DOM 기반 자동화의 한계: 에이전트가 임의 요소를 클릭해 의도치 않은 동작 유발 가능
  - WebMCP가 노리는 개선: 사이트가 노출할 기능 범위를 스스로 통제

---

## 4. MCP와의 관계

- 실행 주체
  - MCP(서버-클라이언트): 별도 MCP 서버 프로세스
  - WebMCP(브라우저 API): 사이트 자신의 JavaScript
- 통신 방식
  - MCP: JSON-RPC(stdio/HTTP)
  - WebMCP: 브라우저 내 JS API 호출
- 발견 방식
  - MCP: 서버가 `tools/list` 응답
  - WebMCP: `navigator.modelContext.tools`
- 신뢰 경계
  - MCP: 로컬 프로세스, 사용자가 설치
  - WebMCP: 방문 중인 사이트(출처 기반 신뢰 필요)

WebMCP는 이처럼 MCP의 도구 노출 개념을 웹 콘텐츠의 보안 모델 안에서 구현하려 한다. 따라서 Same-Origin, 사용자 동의, 권한 프롬프트를 함께 고려해야 한다.

---

## 5. Chrome의 Origin Trial

- Chrome: WebMCP 개념 검증을 위해 Origin Trial(제한적 실험 배포) 진행 중
- Origin Trial 기간: 등록한 출처에서만 실험적 API 활성화 가능
- 정식 표준화 여부와 API 형태: 이 과정에서 계속 변경 가능

---

## 6. 보안/신뢰 관련 열린 문제

- 악성 사이트의 도구 등록: 사용자가 인지하지 못한 상태에서 에이전트가 의도치 않은 `execute`를 호출할 위험
- 사용자 동의 UX: 에이전트가 도구를 호출하기 전 사용자 승인이 필요한 범위를 어떻게 정할지
- 사이트 간 스코프: 여러 탭과 출처에 걸친 작업을 에이전트가 조율할 때의 권한 경계
- 검증 가능성: 사이트가 선언한 `description`과 실제 `execute` 동작이 달라 잘못 판단하거나 속을 때의 대응

---

## 7. 요약

- WebMCP: 사이트가 AI 에이전트에게 구조화된 함수 인터페이스를 직접 노출하는 브라우저 API 제안
- MCP의 도구 스키마 개념을 브라우저 네이티브 API(`navigator.modelContext`)로 가져옴
- 목표: 좌표나 DOM에 의존하는 자동화의 취약성을 줄이고 사이트가 통제할 수 있는 안정적인 인터페이스 제공
- 아직 WICG 초안 + Chrome Origin Trial 단계 → API 형태와 보안 모델은 유동적

---

## 참고 자료

- [WebMCP Explainer (WICG)](https://webmachinelearning.github.io/webmcp/)
- [Chrome AI/WebMCP Origin Trial 발표](../../technology/frontend/browser/chrome-extension/03_side_panel_and_webmcp.md)
- [MCP Specification](../mcp/specification.md)

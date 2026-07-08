# WebMCP Explainer (WICG Draft)

> Web Incubator Community Group(WICG) 초안, 표준화 초기 단계

## 1. 개요

WebMCP는 사이트가 자신의 기능을 AI 에이전트(브라우저에서 동작하는 LLM 기반 어시스턴트 등)에게 구조화된 "도구(tool)"로 노출할 수 있게 하는 웹 플랫폼 API 제안이다. 이름에서 알 수 있듯 Anthropic의 [Model Context Protocol(MCP)](../mcp/specification.md) 개념을 브라우저 네이티브 API로 가져오려는 시도다.

핵심 아이디어는, 에이전트가 웹사이트를 사용할 때 지금처럼 DOM을 스크린 스크래핑하거나 좌표를 클릭하는 대신, 사이트가 스스로 "내가 제공하는 기능은 이런 것들이다"라고 선언한 함수 목록을 에이전트가 직접 호출하게 하는 것이다.

```
기존 방식 (DOM 기반 자동화)
  에이전트 → 페이지 스냅샷 분석 → 좌표 클릭/입력 → 결과 재분석

WebMCP 방식
  에이전트 → navigator.modelContext.tools 목록 조회 → 함수 직접 호출 → 구조화된 결과 반환
```

---

## 2. 제안된 API 형태

초안 단계이므로 세부 스펙은 계속 바뀌지만, 핵심 개념은 다음과 같다.

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

| 요소 | 역할 |
|------|------|
| `name` / `description` | 에이전트가 도구의 용도를 이해하는 근거 |
| `inputSchema` | MCP와 마찬가지로 JSON Schema로 입력 형식을 정의 |
| `execute` | 실제로 페이지 내부 상태를 조작하는 함수 (에이전트가 직접 DOM을 건드리지 않음) |

---

## 3. 왜 필요한가

| 문제 | DOM 기반 자동화의 한계 | WebMCP가 노리는 개선 |
|------|--------------------------|------------------------|
| 신뢰성 | 레이아웃 변경/셀렉터 변경에 취약 | 페이지가 명시한 안정적인 함수 인터페이스 사용 |
| 성능 | 스크린샷/접근성 트리 분석 등 비용이 큼 | 함수 호출 한 번으로 처리 |
| 의도 파악 | 좌표/텍스트만으로 버튼의 진짜 의미를 추론해야 함 | 사이트가 `description`으로 의미를 직접 제공 |
| 보안/권한 | 에이전트가 임의 요소를 클릭해 의도치 않은 동작 유발 가능 | 사이트가 노출할 기능 범위를 스스로 통제 |

---

## 4. MCP와의 관계

| 구분 | MCP (서버-클라이언트) | WebMCP (브라우저 API) |
|------|--------------------------|--------------------------|
| 실행 주체 | 별도 MCP 서버 프로세스 | 사이트 자신의 JavaScript |
| 통신 방식 | JSON-RPC (stdio/HTTP) | 브라우저 내 JS API 호출 |
| 발견 방식 | 서버가 `tools/list` 응답 | `navigator.modelContext.tools` |
| 신뢰 경계 | 로컬 프로세스, 사용자가 설치 | 방문 중인 사이트 (출처 기반 신뢰 필요) |

즉 WebMCP는 MCP의 "도구 노출" 철학을 웹 콘텐츠 보안 모델(Same-Origin, 사용자 동의, 권한 프롬프트) 안에서 재구현하려는 시도로 볼 수 있다.

---

## 5. Chrome의 Origin Trial

Chrome은 WebMCP 개념 검증을 위해 Origin Trial(제한적 실험 배포)을 진행 중이다. Origin Trial 기간 동안은 등록한 출처에서만 실험적 API를 활성화할 수 있으며, 정식 표준화 여부와 API 형태는 이 과정에서 계속 바뀔 수 있다.

---

## 6. 보안/신뢰 관련 열린 문제

| 쟁점 | 설명 |
|------|------|
| 악성 사이트의 도구 등록 | 사용자가 인지하지 못한 상태에서 에이전트가 의도치 않은 `execute`를 호출할 위험 |
| 사용자 동의 UX | 에이전트가 도구를 호출하기 전 사용자 승인이 필요한 범위를 어떻게 정할지 |
| 사이트 간 스코프 | 여러 탭/출처에 걸친 작업을 에이전트가 조율할 때의 권한 경계 |
| 검증 가능성 | 사이트가 선언한 `description`이 실제 `execute` 동작과 다를 때(오탐/기만) 대응 |

---

## 7. 요약

- WebMCP는 사이트가 AI 에이전트에게 구조화된 함수 인터페이스를 직접 노출하는 브라우저 API 제안이다.
- MCP의 도구 스키마 개념을 브라우저 네이티브 API(`navigator.modelContext`)로 가져온다.
- 목표는 좌표/DOM 기반 자동화의 취약성을 없애고, 사이트가 통제 가능한 안정적 인터페이스를 제공하는 것이다.
- 아직 WICG 초안 + Chrome Origin Trial 단계로, API 형태와 보안 모델은 유동적이다.

---

## 참고 자료

- [WebMCP Explainer (WICG)](https://webmachinelearning.github.io/webmcp/)
- [Chrome AI/WebMCP Origin Trial 발표](../../technology/browser/chrome-extension/webmcp-origin-trial.md)
- [MCP Specification](../mcp/specification.md)

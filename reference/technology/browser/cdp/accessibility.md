# CDP: Accessibility 도메인

> 공식 문서: https://chromedevtools.github.io/devtools-protocol/tot/Accessibility/

## 1. 개요

CDP의 `Accessibility` 도메인은 브라우저가 내부적으로 구성한 접근성 트리(Accessibility Tree)를 조회할 수 있게 해준다. 이 트리는 [WAI-ARIA](../../../standard/w3c/wai-aria.md) 역할/상태와 [Core-AAM](../../../standard/w3c/core-aam.md) 매핑 규칙을 거쳐 만들어진 결과물로, 스크린 리더 등 보조 기술이 실제로 소비하는 데이터와 동일한 소스다. 브라우저 자동화 도구가 좌표 기반 클릭 대신 "의미 기반"으로 요소를 찾을 때 핵심적으로 사용하는 도메인이다.

---

## 2. 핵심 메서드

| 메서드 | 역할 |
|--------|------|
| `getFullAXTree` | 문서 전체(또는 특정 프레임)의 접근성 트리를 한 번에 조회 |
| `getPartialAXTree` | 특정 노드를 기준으로 그 조상/자손 일부만 조회 (대형 페이지에서 효율적) |
| `getChildAXNodes` | 지정한 AX 노드의 자식 노드 조회 |
| `queryAXTree` | 접근 가능한 이름(accessible name)이나 역할로 노드를 검색 |
| `enable` / `disable` | 도메인 이벤트 활성화/비활성화 |

```javascript
// CDP 세션에서 접근성 트리 조회 예시 (chrome.debugger 또는 Puppeteer 저수준 API 기준)
const { nodes } = await client.send("Accessibility.getFullAXTree");
```

---

## 3. AXNode 구조

```json
{
  "nodeId": "12",
  "ignored": false,
  "role": { "type": "role", "value": "button" },
  "name": { "type": "computedString", "value": "장바구니에 담기" },
  "properties": [
    { "name": "focusable", "value": { "type": "booleanOrUndefined", "value": true } }
  ],
  "childIds": ["13", "14"],
  "backendDOMNodeId": 55
}
```

| 필드 | 설명 |
|------|------|
| `role` | ARIA/네이티브 매핑을 거쳐 계산된 최종 역할 |
| `name` | [AccName 계산 규칙](../../../standard/w3c/core-aam.md)에 따라 도출된 접근 가능한 이름 |
| `ignored` | 브라우저가 이 노드를 접근성 트리에서 의미 없다고 판단해 제외했는지 여부 |
| `backendDOMNodeId` | `DOM` 도메인의 노드와 연결하는 식별자 |
| `properties` | `focusable`, `disabled`, `expanded` 등 state/property 값 |

---

## 4. DOM 도메인과의 연동

접근성 트리 노드는 `backendDOMNodeId`로 [DOM 도메인](./dom.md)의 실제 DOM 노드와 연결된다. 자동화 도구는 보통 다음 순서로 동작한다.

```
1. Accessibility.getFullAXTree()로 "장바구니 담기" 버튼의 role/name 확인
2. backendDOMNodeId로 DOM.describeNode() 호출해 실제 DOM 노드 정보 획득
3. DOM.getBoxModel()로 화면 좌표 계산
4. Input.dispatchMouseEvent()로 해당 좌표에 클릭 이벤트 전송
```

---

## 5. `ignored` 노드와 관련 사유(ignoredReasons)

브라우저는 시각적으로 숨겨진 요소, 빈 텍스트 노드, 순수 레이아웃 목적의 `<div>` 등을 접근성 트리에서 제외한다. `getFullAXTree`에 `ignored: true`인 노드가 포함될 때, `ignoredReasons` 필드로 구체적 사유(예: `hiddenByChildTree`, `notRendered`, `presentationalRole`)를 확인할 수 있다.

---

## 6. 활용 사례

| 사용처 | 활용 방식 |
|--------|-----------|
| 브라우저 자동화(Playwright, Puppeteer) | 좌표가 아닌 role+name으로 요소를 안정적으로 특정 |
| `chrome-devtools-mcp` 같은 MCP 서버 | LLM 에이전트에게 "현재 페이지에 어떤 상호작용 가능한 요소가 있는지"를 구조화된 형태로 제공 |
| 접근성 린터/자동 감사 도구 | 실제 브라우저가 계산한 역할/이름을 기준으로 WCAG 위반 여부 검증 |
| E2E 테스트 | `getByRole` 유형의 셀렉터가 내부적으로 이 트리를 참조 |

---

## 7. 요약

- `Accessibility` 도메인은 브라우저가 계산한 접근성 트리(role, name, state)를 CDP로 노출한다.
- `backendDOMNodeId`로 `DOM` 도메인 노드와 연결해 실제 좌표/조작으로 이어갈 수 있다.
- LLM 기반 브라우저 자동화(MCP 서버 등)가 좌표 기반 클릭보다 신뢰성 있는 요소 특정 수단으로 널리 사용한다.

---

## 참고 자료

- [CDP Accessibility 도메인 (공식)](https://chromedevtools.github.io/devtools-protocol/tot/Accessibility/)
- [DOM 도메인](./dom.md)
- [WAI-ARIA 1.2](../../../standard/w3c/wai-aria.md)
- [Core-AAM 1.2](../../../standard/w3c/core-aam.md)

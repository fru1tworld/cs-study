# CDP: DOM 도메인

> 공식 문서: https://chromedevtools.github.io/devtools-protocol/tot/DOM/

## 1. 개요

CDP `DOM` 도메인은 렌더링 프로세스의 실제 DOM 트리를 조회·조작하는 기능을 제공한다. 페이지 JS를 거치지 않고 브라우저 엔진 내부 표현에 직접 접근하므로, 페이지가 이벤트 리스너를 오염시키거나 `console`을 가로채는 등의 방어 코드를 우회해 안정적으로 DOM 상태를 관찰할 수 있다.

---

## 2. 핵심 메서드

| 메서드 | 역할 |
|--------|------|
| `getDocument` | 문서 루트 노드부터 지정한 깊이까지 DOM 트리 조회 |
| `describeNode` | 특정 노드의 상세 정보(태그, 속성, 자식 수 등) 조회 |
| `querySelector` / `querySelectorAll` | CSS 셀렉터로 노드 검색 |
| `getBoxModel` | 노드의 content/padding/border/margin 박스 좌표 조회 |
| `resolveNode` | DOM 노드를 `Runtime` 도메인의 JS 객체 참조로 변환 |
| `setAttributeValue` / `removeAttribute` | 속성 값 변경/삭제 |
| `getOuterHTML` / `setOuterHTML` | HTML 직렬화 조회/설정 |
| `pushNodesByBackendIdsToFrontend` | `backendNodeId`를 프론트엔드에서 참조 가능한 `nodeId`로 변환 |

---

## 3. NodeId와 BackendNodeId

CDP는 노드를 가리키는 두 가지 식별자를 구분해서 쓴다.

| 식별자 | 특징 |
|--------|------|
| `nodeId` | 프론트엔드(CDP 클라이언트)에 이미 전송되어 알려진 노드의 세션 한정 ID. DOM 변경 시 무효화될 수 있음 |
| `backendNodeId` | 브라우저 내부에서 노드를 가리키는 안정적 ID. 아직 프론트엔드에 "push"되지 않은 노드도 참조 가능 |

`Accessibility` 도메인의 `AXNode.backendDOMNodeId`처럼, 다른 도메인은 보통 `backendNodeId`로 노드를 참조하고, 실제 조작이 필요할 때 `pushNodesByBackendIdsToFrontend`로 `nodeId`를 얻는 흐름이 일반적이다.

---

## 4. getBoxModel과 좌표 계산

```javascript
const { model } = await client.send("DOM.getBoxModel", { nodeId });
// model.content = [x1, y1, x2, y2, x3, y3, x4, y4] — 사각형 4개 꼭짓점
const [x1, y1, , , x3, y3] = model.content;
const centerX = (x1 + x3) / 2;
const centerY = (y1 + y3) / 2;
```

`Input.dispatchMouseEvent`로 특정 요소를 클릭하려면, 먼저 `getBoxModel`로 요소의 화면상 사각형 좌표를 얻고 중심점을 계산하는 것이 일반적인 패턴이다. 이 좌표는 CSS 픽셀 기준 뷰포트 좌표이며, 자세한 좌표계 구분은 [Blink 좌표 공간](../automation-internals/blink-coordinate-spaces.md) 문서를 참고한다.

---

## 5. DOM 변경 이벤트

| 이벤트 | 설명 |
|--------|------|
| `documentUpdated` | 문서 전체가 갱신됨(네비게이션 등), 기존 `nodeId`가 모두 무효화됨 |
| `setChildNodes` | 지연 로드된 자식 노드 목록이 도착함 |
| `attributeModified` / `attributeRemoved` | 속성 변경 감지 |
| `childNodeInserted` / `childNodeRemoved` | 자식 노드 추가/제거 감지 |

`documentUpdated` 이벤트를 받으면 이전에 캐시해둔 모든 `nodeId`를 버리고 다시 조회해야 한다 — 이는 페이지 이동(SPA 라우팅 포함) 후 자동화 스크립트가 자주 겪는 "stale node" 오류의 근본 원인이다.

---

## 6. iframe/OOPIF와의 관계

크로스 오리진 iframe은 별도의 렌더링 프로세스(Out-Of-Process iframe, OOPIF)에서 실행될 수 있어, 부모 프레임의 `DOM` 도메인 세션만으로는 자식 프레임 내부를 조회할 수 없는 경우가 있다. 이때는 [Target 도메인](./target.md)으로 해당 프레임의 별도 CDP 세션에 연결해야 한다. 자세한 배경은 [OOPIF / Site Isolation](../automation-internals/oopif-site-isolation.md) 문서를 참고한다.

---

## 7. 요약

- `DOM` 도메인은 렌더링 프로세스의 실제 DOM 트리를 조회/조작하는 CDP 핵심 도메인이다.
- `nodeId`(세션 한정, 휘발성)와 `backendNodeId`(안정적)를 구분해서 사용해야 한다.
- `getBoxModel`로 얻은 좌표는 `Input` 도메인 이벤트 좌표 계산의 기반이 된다.
- `documentUpdated` 이벤트 이후에는 캐시된 노드 참조를 모두 재조회해야 한다.
- 크로스 오리진 iframe 내부는 별도 `Target` 세션이 필요할 수 있다.

---

## 참고 자료

- [CDP DOM 도메인 (공식)](https://chromedevtools.github.io/devtools-protocol/tot/DOM/)
- [Accessibility 도메인](./accessibility.md)
- [Target 도메인](./target.md)
- [Blink 좌표 공간](../automation-internals/blink-coordinate-spaces.md)

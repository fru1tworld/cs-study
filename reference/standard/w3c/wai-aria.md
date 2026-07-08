# WAI-ARIA 1.2 (Accessible Rich Internet Applications)

> W3C Recommendation, 2023년 6월

## 1. 개요

WAI-ARIA(Accessible Rich Internet Applications)는 HTML만으로는 표현하기 어려운 동적 UI(탭, 트리, 다이얼로그, 실시간 갱신 영역 등)를 보조 기술(스크린 리더 등)이 이해할 수 있도록 의미론적 정보를 추가하는 속성 명세다. HTML 시맨틱 요소가 부족한 부분을 `role`, `aria-*` 속성으로 보완한다.

핵심 원칙은 "First Rule of ARIA" — 네이티브 HTML 요소로 같은 의미와 동작을 구현할 수 있다면 ARIA 대신 그 요소를 사용해야 한다는 것이다. ARIA는 대체 수단이 없을 때만 쓴다.

---

## 2. 세 가지 속성군

| 분류 | 설명 | 예시 |
|------|------|------|
| Role | 요소가 어떤 종류의 UI 컴포넌트인지 정의 | `role="button"`, `role="tablist"`, `role="dialog"` |
| State | 요소의 현재 동적 상태 (자주 변함) | `aria-expanded`, `aria-checked`, `aria-selected`, `aria-disabled` |
| Property | 요소의 비교적 정적인 특성 | `aria-label`, `aria-labelledby`, `aria-describedby`, `aria-required` |

---

## 3. Role 분류 체계

WAI-ARIA 1.2는 역할을 상위 추상 역할(Abstract Roles, 직접 사용 금지)부터 구체적인 위젯 역할까지 계층으로 정의한다.

| 대분류 | 대표 역할 |
|--------|-----------|
| Widget | `button`, `checkbox`, `radio`, `slider`, `tab`, `tabpanel`, `menuitem`, `option` |
| Composite Widget | `tablist`, `tree`, `grid`, `listbox`, `menu`, `combobox` |
| Document Structure | `article`, `heading`, `list`, `listitem`, `table`, `row`, `cell` |
| Landmark | `banner`, `navigation`, `main`, `complementary`, `contentinfo`, `search` |
| Live Region | `alert`, `status`, `log`, `marquee`, `timer` |
| Window | `dialog`, `alertdialog` |

```html
<div role="tablist" aria-label="설정 메뉴">
  <button role="tab" id="tab1" aria-selected="true" aria-controls="panel1">일반</button>
  <button role="tab" id="tab2" aria-selected="false" aria-controls="panel2">보안</button>
</div>
<div role="tabpanel" id="panel1" aria-labelledby="tab1">일반 설정 내용</div>
<div role="tabpanel" id="panel2" aria-labelledby="tab2" hidden>보안 설정 내용</div>
```

---

## 4. 라이브 리전 (Live Region)

DOM이 변경될 때 스크린 리더가 자동으로 읽어주도록 지정하는 메커니즘이다.

| 속성 | 값 | 의미 |
|------|-----|------|
| `aria-live` | `off`(기본) / `polite` / `assertive` | `polite`는 현재 낭독이 끝난 뒤 알림, `assertive`는 즉시 중단하고 알림 |
| `aria-atomic` | `true` / `false` | 변경된 부분만 읽을지, 영역 전체를 다시 읽을지 |
| `aria-relevant` | `additions` / `removals` / `text` / `all` | 어떤 종류의 변경을 알림 대상으로 볼지 |
| `role="alert"` | - | `aria-live="assertive" aria-atomic="true"`와 동등한 축약 역할 |

```html
<div aria-live="polite" aria-atomic="true">
  장바구니에 상품이 추가되었습니다.
</div>
```

---

## 5. 1.2 버전의 주요 변경/추가 사항

| 항목 | 내용 |
|------|------|
| `role="searchbox"`, `role="meter"` 등 신규 역할 | 이전 버전에 없던 세부 역할 추가 |
| `aria-description` | `aria-describedby`와 달리 별도 DOM 요소 없이 직접 설명 문자열 지정 |
| Presentational children 규칙 명확화 | `role="presentation"`을 자식 요소까지 전파하는 조건을 명확히 정의 |
| 역할별 필수/허용 속성 표(Role 별 aria-* 매핑) 정비 | 각 role이 지원해야 하는 state/property 목록 표준화 |

> 향후 계획: `aria-braillelabel` / `aria-brailleroledescription`(점자 디스플레이 전용 레이블)은 1.2에는 존재하지 않으며, ARIA 1.3 First Public Working Draft(2024년 1월)에서 신규 도입된 속성이다.

---

## 6. Accessibility Tree와의 관계

브라우저는 DOM + ARIA 정보를 바탕으로 접근성 트리(Accessibility Tree)를 구성하고, 이를 운영체제 접근성 API(Windows UIA, macOS NSAccessibility 등)로 노출한다. 이 매핑 규칙 자체는 WAI-ARIA가 아니라 [Core-AAM](./core-aam.md) 명세가 정의한다.

```
HTML + ARIA 속성
      │
      ▼
Accessibility Tree (브라우저 내부)   ← Core-AAM이 매핑 규칙 정의
      │
      ▼
OS Accessibility API (UIA, NSAccessibility, ATK)
      │
      ▼
보조 기술 (스크린 리더, 스위치 컨트롤 등)
```

---

## 7. 흔한 오용 사례

| 안티패턴 | 문제 | 대안 |
|----------|------|------|
| `<div role="button">` + 클릭 핸들러만 추가 | 키보드 포커스/Enter·Space 활성화가 자동으로 되지 않음 | `tabindex="0"` 및 키보드 이벤트 직접 구현, 또는 `<button>` 사용 |
| 모든 요소에 `aria-label` 남발 | 스크린 리더 낭독이 오히려 장황해짐 | 시각적으로 이미 명확한 텍스트가 있으면 생략 |
| `role="presentation"`을 인터랙티브 요소에 사용 | 요소의 의미론이 완전히 제거되어 접근 불가 상태가 됨 | 인터랙티브 요소에는 사용하지 않음 |

---

## 8. 요약

- WAI-ARIA는 동적 UI에 의미론적 정보를 부여해 보조 기술이 이해하게 하는 속성 명세다.
- Role/State/Property 세 축으로 구성되며, "네이티브 요소 우선" 원칙이 First Rule of ARIA다.
- Live Region으로 비동기 DOM 변경을 스크린 리더에 알릴 수 있다.
- 실제 OS 접근성 API로의 매핑 규칙은 [Core-AAM](./core-aam.md)이 별도로 정의한다.

---

## 참고 자료

- [WAI-ARIA 1.2 (W3C)](https://www.w3.org/TR/wai-aria-1.2/)
- [ARIA Authoring Practices Guide (APG)](https://www.w3.org/WAI/ARIA/apg/)
- [Core-AAM 1.2](./core-aam.md)

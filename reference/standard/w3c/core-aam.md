# Core Accessibility API Mappings (Core-AAM) 1.2

> W3C Candidate Recommendation Draft(진행 중, 아직 정식 REC 아님) — 현재 확정된 버전은 Core-AAM 1.1(W3C Recommendation, 2017년 12월 14일)이며, 1.2는 이를 갱신해 향후 대체 예정.

## 1. 개요

Core-AAM: [WAI-ARIA](./wai-aria.md) 역할·상태·속성을 실제 운영체제 접근성 API(Windows UI Automation, macOS NSAccessibility/AX API, Linux ATK/AT-SPI)의 개념으로 매핑하는 규칙을 정의하는 명세.
- 브라우저 엔진이 접근성 트리를 만들고 이를 OS에 노출할 때 따라야 하는 "번역 규칙집"

```
WAI-ARIA role/state/property
          │  (Core-AAM이 정의하는 매핑)
          ▼
OS 접근성 API 개념
  - Windows: UIA ControlType, PatternId, PropertyId
  - macOS: NSAccessibilityRole, NSAccessibilityAttribute
  - Linux: AtkRole, AtkStateType
          │
          ▼
스크린 리더 등 보조 기술
```

---

## 2. 왜 별도 명세가 필요한가

- WAI-ARIA: 웹 콘텐츠 저작자가 사용할 속성을 정의 — "브라우저가 이 속성을 받아서 OS에 정확히 어떻게 전달해야 하는가"는 별개 문제
- OS마다 접근성 API의 개념 모델이 다름
  - 예: Windows UIA의 `Pattern` 개념 → macOS AX API에 정확히 대응하는 개념 없음
- 각 조합에 대한 명시적 매핑 규칙 부재 → 브라우저마다 스크린 리더 경험이 달라짐
- Core-AAM → 이 불일치를 없애기 위한 상호운용성 계층

---

## 3. 매핑 예시

### 3.1 Role 매핑

- `button`
  - Windows UIA ControlType: Button
  - macOS AX Role: AXButton
  - Linux ATK Role: ATK_ROLE_PUSH_BUTTON
- `checkbox`
  - Windows UIA ControlType: CheckBox
  - macOS AX Role: AXCheckBox
  - Linux ATK Role: ATK_ROLE_CHECK_BOX
- `tab`
  - Windows UIA ControlType: TabItem
  - macOS AX Role: AXRadioButton(탭은 라디오 그룹으로 매핑)
  - Linux ATK Role: ATK_ROLE_PAGE_TAB
- `dialog`
  - Windows UIA ControlType: Pane(Window 패턴 포함)
  - macOS AX Role: AXWindow / AXSheet
  - Linux ATK Role: ATK_ROLE_DIALOG
- `alert`
  - Windows UIA ControlType: 없음(LiveRegion 이벤트로 처리)
  - macOS AX Role: AXWindow(알림)
  - Linux ATK Role: ATK_ROLE_ALERT

### 3.2 State/Property 매핑

- `aria-checked`
  - Windows UIA: `TogglePattern.ToggleState`
  - macOS AX: `AXValue`
- `aria-disabled`
  - Windows UIA: `IsEnabledProperty`(반전)
  - macOS AX: `AXEnabled`
- `aria-expanded`
  - Windows UIA: `ExpandCollapsePattern.ExpandCollapseState`
  - macOS AX: `AXExpanded`
- `aria-label`
  - Windows UIA: `NameProperty`
  - macOS AX: `AXTitle` / `AXDescription`
- `aria-live`
  - Windows UIA: LiveRegion 이벤트
  - macOS AX: `AXPriority`(알림 우선순위)

---

## 4. 이름 계산 규칙(Accessible Name Computation)

Core-AAM: [Accessible Name and Description Computation (AccName)](https://www.w3.org/TR/accname-1.2/) 명세와 함께, 요소의 "접근 가능한 이름" 계산 우선순위를 정의.

```
우선순위(높음 → 낮음):
1. aria-labelledby로 참조된 요소들의 텍스트
2. aria-label 속성값
3. 네이티브 HTML 레이블 메커니즘 (<label for>, <caption> 등)
4. title 속성
5. 요소 자신의 텍스트 콘텐츠 (버튼, 링크 등)
```

```html
<!-- aria-labelledby가 aria-label보다 우선 -->
<span id="lbl">저장</span>
<button aria-labelledby="lbl" aria-label="다른 이름">저장</button>
<!-- 스크린 리더는 "저장"으로 읽음 -->
```

---

## 5. 실제 구현체와의 관계

- Chromium (Blink): Core-AAM 규칙을 `AXPlatformNode` 계층에서 구현, CDP `Accessibility` 도메인으로 결과 조회 가능
- Firefox (Gecko): `accessible/` 모듈에서 자체 구현
- WebKit (Safari): `AccessibilityObject` 계층에서 구현
- 테스트 스위트: [ARIA-AT](https://aria-at.w3.org/) 프로젝트가 브라우저×스크린리더 조합별 실제 동작을 검증

---

## 6. 개발자가 알아야 할 실무 포인트

- 스크린 리더마다 같은 마크업을 다르게 읽음
  - 원인: 브라우저-OS API 매핑, OS-스크린리더 해석이 조합별로 미묘하게 다름 → 완전한 상호운용성은 실무에서 한계
- `role="tab"`에 키보드 핸들러가 필요한 이유
  - Role 매핑은 정적 의미만 전달 → 상호작용(포커스 이동, 활성화)은 저작자가 JS로 구현해야 함
- `aria-label`이 시각적 텍스트를 덮어씀
  - AccName 계산 우선순위상 `aria-label`이 요소의 텍스트 콘텐츠보다 우선

---

## 7. 요약

- Core-AAM: WAI-ARIA 개념을 실제 OS 접근성 API로 번역하는 규칙 정의
- Role/State/Property별로 Windows UIA·macOS AX·Linux ATK 매핑 규칙 존재
- 접근 가능한 이름 계산 우선순위(`aria-labelledby` > `aria-label` > 네이티브 레이블 > 텍스트 콘텐츠) 규정
- 브라우저별 실제 구현 차이는 여전히 존재 → 크로스 브라우저 접근성 테스트 필요

---

## 참고 자료

- [Core-AAM 1.2 (W3C)](https://www.w3.org/TR/core-aam-1.2/)
- [Accessible Name and Description Computation (AccName)](https://www.w3.org/TR/accname-1.2/)
- [WAI-ARIA 1.2](./wai-aria.md)
- [ARIA-AT (브라우저×스크린리더 테스트)](https://aria-at.w3.org/)

# Trusted Input 구현 비교: Chromium / Puppeteer / Playwright / React

브라우저 자동화 도구와 프레임워크가 "신뢰된 입력"과 "요소 조작 가능 여부"를 다루는 방식을 개념 수준에서 비교 정리한다. 각 프로젝트의 실제 소스 코드를 옮기지 않고, 공개적으로 알려진 동작 방식과 설계 의도를 요약한다.

---

## 1. Chromium: 신뢰된 클릭의 생성 원리

Chromium 블로그와 개발 문서에서 설명하는 "신뢰된 입력 자동화"의 핵심은, 클릭이 페이지 JavaScript의 `dispatchEvent()`가 아니라 브라우저 프로세스(Browser Process)에서 렌더러 프로세스로 IPC를 통해 전달되는 실제 입력 이벤트 경로를 거쳐야 `isTrusted: true`가 된다는 점이다. [CDP Input 도메인](../cdp/input.md)의 `dispatchMouseEvent`/`dispatchKeyEvent`가 바로 이 경로를 프로그래밍 방식으로 트리거하는 공식 인터페이스다.

```
일반적인 사용자 클릭 경로:
  OS 입력 이벤트 → 브라우저 프로세스 → IPC → 렌더러 프로세스 → DOM 이벤트(isTrusted=true)

CDP Input 도메인 경로:
  CDP 명령 → 브라우저 프로세스 (OS 입력을 흉내) → IPC → 렌더러 프로세스 → DOM 이벤트(isTrusted=true)

페이지 JS dispatchEvent 경로:
  페이지 JS → 렌더러 프로세스 내부에서 바로 이벤트 생성 → DOM 이벤트(isTrusted=false)
```

즉 CDP `Input` 도메인은 "가짜로 신뢰 플래그를 켜는" 우회가 아니라, 실제 입력이 브라우저 프로세스를 거치는 정상 경로를 프로그래밍 인터페이스로 재현하는 것이다.

---

## 2. Puppeteer: clickablePoint()

Puppeteer의 `ElementHandle`은 클릭을 수행하기 전 요소가 실제로 클릭 가능한 지점을 계산하는 단계를 거친다. 개념적으로는 다음을 확인한다.

1. 요소의 바운딩 박스(quad)를 CDP `DOM.getContentQuads` 유사 API로 조회
2. 박스가 화면 밖으로 잘려 있거나 다른 요소에 완전히 가려져 있지 않은지 확인
3. 클릭 가능한 중심점(또는 안전한 지점)을 계산해 반환
4. 이 좌표로 [CDP Input](../cdp/input.md) 이벤트를 디스패치

이 과정이 없으면, 예를 들어 요소의 절반이 모달 창에 가려져 있을 때 잘못된 지점을 클릭해 자동화가 실패하는 문제가 생긴다.

---

## 3. Playwright: Actionability 체크

Playwright는 클릭/입력 같은 액션을 수행하기 전 요소가 다음 조건을 모두 만족하는지 자동으로 재시도하며 확인하는 "actionability" 개념을 둔다.

| 체크 항목 | 의미 |
|-----------|------|
| Attached | 요소가 DOM에 붙어 있는지 |
| Visible | 화면에 렌더링되어 보이는지 (크기 0이 아님, `visibility:hidden` 아님) |
| Stable | 애니메이션 등으로 위치가 계속 바뀌지 않고 안정화됐는지 |
| Receives Events | 클릭 지점의 최상위 요소가 실제로 그 요소인지 (다른 요소에 가려져 있지 않은지) |
| Enabled | 폼 요소가 `disabled` 상태가 아닌지 |

이 체크를 통과할 때까지 자동으로 짧은 간격으로 재시도하는 방식이, "요소가 나타날 때까지 기다렸다가 클릭"하는 안정적인 자동화 스크립트를 가능하게 하는 핵심 설계다.

### 3.1 입력 이벤트 처리 (crInput 계열)

Playwright의 Chromium 드라이버 계층은 [CDP Input 도메인](../cdp/input.md)을 감싸 마우스 이동 → 누름 → 뗌, 키 누름 → 뗌 시퀀스를 생성하는 내부 모듈을 갖고 있다. 실제 타이핑을 흉내낼 때는 각 문자를 개별 키 이벤트로 순차 디스패치해, 한 번에 전체 문자열을 밀어넣는 것보다 페이지의 `keydown`/`input` 리스너에 더 가깝게 동작하도록 한다.

### 3.2 값 채우기(fill)

`fill()` 계열 동작은 타이핑을 시뮬레이션하는 대신, 폼 요소의 값을 직접 설정한 뒤 `input`/`change` 이벤트를 발생시키는 더 빠른 경로를 사용한다. 다만 이 방식은 IME 조합이나 커스텀 입력 마스킹처럼 실제 키 입력 순서에 의존하는 로직에는 완전히 대응하지 못할 수 있어, 그런 케이스에는 개별 키 입력 시뮬레이션(`type`류 API)이 별도로 필요하다.

---

## 4. React: 값 트래킹(inputValueTracking)과의 상호작용

React는 제어 컴포넌트(controlled input)의 `value`를 다시 렌더링할 때, 네이티브 `<input>` 요소의 값 setter를 감싼 내부 트래킹 메커니즘을 사용해 "실제 값이 바뀌었는지"를 추적한다. 이 때문에 자동화 도구가 `element.value = "text"`처럼 네이티브 프로퍼티만 직접 대입하면, React가 내부적으로 캐시해 둔 이전 값과 비교해 변경을 감지하지 못해 `onChange` 핸들러가 호출되지 않는 문제가 발생할 수 있다.

이를 우회하기 위해 자동화 도구/라이브러리는 보통 다음 순서를 따른다.

```javascript
// 네이티브 value setter를 직접 호출해 React의 캐시된 추적 값을 우회
const nativeInputValueSetter = Object.getOwnPropertyDescriptor(
  window.HTMLInputElement.prototype, "value"
).set;
nativeInputValueSetter.call(inputElement, "새 값");

// 그런 다음 React가 감지할 수 있는 진짜 input 이벤트를 발생시킴
inputElement.dispatchEvent(new Event("input", { bubbles: true }));
```

CDP `Input` 도메인으로 실제 키 입력을 하나씩 시뮬레이션하면 이 문제 자체가 발생하지 않는다 — React가 감시하는 네이티브 setter 경로를 실제 키 입력이 정상적으로 통과하기 때문이다. 즉 "값을 직접 대입"하는 방식에서만 발생하는 React 특유의 함정이다.

---

## 5. 종합 비교

| 계층 | 확인/처리하는 문제 |
|------|------------------------|
| Chromium (CDP Input) | 이벤트의 `isTrusted` 여부, 실제 입력 경로 재현 |
| Puppeteer (clickablePoint) | 요소가 실제로 클릭 가능한 좌표를 갖는지 |
| Playwright (Actionability) | 요소가 보이고, 안정적이고, 다른 요소에 가려지지 않았는지 |
| React (값 트래킹) | 네이티브 값 대입만으로는 감지되지 않는 제어 컴포넌트 상태 동기화 문제 |

이 네 계층은 서로 독립적인 문제를 다루지만, 실제 자동화 스크립트가 "요소를 찾아서 클릭했는데 아무 일도 안 일어난다"는 흔한 실패의 원인들을 각각 설명해 준다.

---

## 참고 자료

- [CDP Input 도메인](../cdp/input.md)
- [MDN Event.isTrusted](./mdn-input-events-reference.md)
- [Puppeteer 공식 문서](https://pptr.dev/)
- [Playwright Actionability 공식 문서](https://playwright.dev/docs/actionability)

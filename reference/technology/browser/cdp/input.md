# CDP: Input 도메인

> 공식 문서: https://chromedevtools.github.io/devtools-protocol/tot/Input/

## 1. 개요

CDP `Input` 도메인은 마우스, 키보드, 터치 이벤트를 프로그래밍 방식으로 생성해 페이지에 전달한다. 단순히 JavaScript로 `element.click()`을 호출하는 것과 달리, 이 도메인을 통해 발생한 이벤트는 브라우저 렌더링 프로세스 수준에서 생성되어 `isTrusted: true` 속성을 가진 신뢰된(trusted) 이벤트로 처리된다는 점이 핵심 차이다.

---

## 2. 핵심 메서드

| 메서드 | 역할 |
|--------|------|
| `dispatchMouseEvent` | 마우스 이동/클릭/휠 이벤트 전송 |
| `dispatchKeyEvent` | 키보드 keydown/keyup/char 이벤트 전송 |
| `dispatchTouchEvent` | 터치 이벤트 전송 |
| `insertText` | IME 조합 없이 텍스트를 직접 삽입 (composition 이벤트 우회) |
| `dispatchDragEvent` | 드래그 앤 드롭 시퀀스 이벤트 전송 |
| `setInterceptDrags` | 드래그 동작을 가로채 CDP로 제어 |

---

## 3. 마우스 이벤트 시퀀스

실제 클릭은 단일 이벤트가 아니라 최소 3단계로 구성해야 자연스러운 사용자 조작을 재현할 수 있다.

```javascript
await client.send("Input.dispatchMouseEvent", {
  type: "mouseMoved", x: 100, y: 200,
});
await client.send("Input.dispatchMouseEvent", {
  type: "mousePressed", x: 100, y: 200, button: "left", clickCount: 1,
});
await client.send("Input.dispatchMouseEvent", {
  type: "mouseReleased", x: 100, y: 200, button: "left", clickCount: 1,
});
```

| 좌표 기준 | 설명 |
|-----------|------|
| `x`, `y` | CSS 픽셀 기준 뷰포트 좌표 (물리 픽셀이 아님, `devicePixelRatio` 별도 고려 불필요) |

좌표계 상세는 [Blink 좌표 공간](../automation-internals/blink-coordinate-spaces.md) 문서를 참고한다.

---

## 4. 키보드 이벤트

```javascript
await client.send("Input.dispatchKeyEvent", {
  type: "keyDown",
  key: "a",
  code: "KeyA",
  text: "a",
});
await client.send("Input.dispatchKeyEvent", {
  type: "keyUp",
  key: "a",
  code: "KeyA",
});
```

| 필드 | 역할 |
|------|------|
| `type` | `keyDown` / `keyUp` / `rawKeyDown` / `char` |
| `key` / `code` | [DOM `KeyboardEvent`](https://developer.mozilla.org/docs/Web/API/UI_Events/Keyboard_event_key_values) 규격과 동일한 논리 키 값 |
| `text` | `char` 타입일 때 실제로 입력될 문자 |
| `modifiers` | Alt/Ctrl/Meta/Shift 비트마스크 |

한글 등 IME 조합이 필요한 입력은 `dispatchKeyEvent`만으로는 자연스럽게 재현하기 어려워, 실무에서는 `Input.insertText`로 조합 완료된 텍스트를 바로 삽입하는 방식을 병행한다. IME 조합 이벤트 자체의 표준 동작은 [compositionstart](../automation-internals/mdn-input-events-reference.md) 문서를 참고한다.

---

## 5. Trusted Event와 자동화의 관계

브라우저는 사용자가 실제로 조작했는지를 `Event.isTrusted`로 구분한다. `element.dispatchEvent(new MouseEvent(...))`처럼 페이지 JS가 만든 이벤트는 `isTrusted: false`로 표시되어, 일부 보안에 민감한 동작(파일 업로드 다이얼로그, 팝업 차단 우회, 클립보드 접근 등)이 차단된다. 반면 CDP `Input` 도메인을 통해 브라우저 프로세스가 직접 생성한 이벤트는 `isTrusted: true`로 취급되어 실제 사용자 조작과 동일하게 처리된다.

```
페이지 JS의 dispatchEvent()  →  isTrusted: false  →  일부 보안 동작 차단됨
CDP Input.dispatch*Event()   →  isTrusted: true   →  실제 사용자 입력과 동일하게 처리
```

이 차이가 브라우저 자동화 도구(Puppeteer, Playwright)가 페이지 JS로 이벤트를 합성하지 않고 CDP `Input` 도메인을 사용하는 근본적인 이유다. 관련 내용은 [trusted-input-implementations.md](../automation-internals/trusted-input-implementations.md)에서 자세히 다룬다.

---

## 6. User Activation과의 관계

`Input` 도메인으로 생성한 클릭/키 입력은 [User Activation](../automation-internals/user-activation.md) 상태도 실제 사용자 입력처럼 활성화시킨다. 이는 팝업 차단, 자동재생 정책, 전체화면 API 등 "사용자 제스처 필요" 조건이 걸린 API를 자동화 시나리오에서도 정상 호출할 수 있게 해준다.

---

## 7. 요약

- `Input` 도메인은 브라우저 프로세스 수준에서 신뢰된(trusted) 마우스/키보드/터치 이벤트를 생성한다.
- 클릭은 `mouseMoved` → `mousePressed` → `mouseReleased` 시퀀스로 구성해야 자연스럽다.
- `isTrusted: true` 특성 덕분에 페이지 JS의 합성 이벤트로는 불가능한 보안 민감 동작도 트리거할 수 있다.
- User Activation 상태도 함께 활성화되어 사용자 제스처가 필요한 API 호출이 가능해진다.

---

## 참고 자료

- [CDP Input 도메인 (공식)](https://chromedevtools.github.io/devtools-protocol/tot/Input/)
- [Blink 좌표 공간](../automation-internals/blink-coordinate-spaces.md)
- [User Activation](../automation-internals/user-activation.md)
- [Trusted Input 구현 비교](../automation-internals/trusted-input-implementations.md)

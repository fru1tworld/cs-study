# User Activation

## 1. 개요

User Activation(사용자 활성화)은 브라우저가 "이 동작이 실제 사용자의 의도적 조작에서 비롯됐는가"를 추적하는 내부 상태다. 팝업 차단, 자동재생 정책, 클립보드 쓰기, 전체화면 진입, 결제 요청 등 오남용 가능성이 있는 API 다수가 "유효한 사용자 활성화 상태에서만 호출 가능"이라는 제약을 건다.

---

## 2. 두 가지 상태

| 상태 | 설명 | 지속 시간 |
|------|------|-----------|
| Sticky Activation | 문서가 한 번이라도 사용자 활성화를 받은 적이 있는지 (`navigator.userActivation.hasBeenActive`) | 문서 수명 전체 동안 유지 |
| Transient Activation | 가장 최근 사용자 제스처로부터 짧은 시간 동안만 유효 (`navigator.userActivation.isActive`) | 수 초 내외의 짧은 타이머 (브라우저별 상이, 명세는 구체 수치를 강제하지 않음) |

```javascript
button.addEventListener("click", () => {
  console.log(navigator.userActivation.isActive); // true (클릭 직후)
  setTimeout(() => {
    console.log(navigator.userActivation.isActive); // 시간 경과 후 false
  }, 10_000);
});
```

---

## 3. Transient Activation이 필요한 대표 API

| API | 이유 |
|-----|------|
| `window.open()` (팝업) | 사용자 의도 없는 팝업 스팸 방지 |
| `element.requestFullscreen()` | 사용자 동의 없는 전체화면 전환 방지 |
| `navigator.clipboard.writeText()` | 사용자 모르게 클립보드를 덮어쓰는 것 방지 |
| `<video>.play()` (자동재생 정책) | 소리 있는 미디어 자동 재생으로 인한 UX 저하 방지 |
| `PaymentRequest.show()` | 사용자 개입 없는 결제 UI 노출 방지 |
| Web Share API (`navigator.share()`) | 임의 시점에 공유 시트가 뜨는 것 방지 |

---

## 4. 소비(consumption) 모델

일부 API는 Transient Activation을 "소비"한다. 즉 한 번의 사용자 클릭으로 여러 개의 제한된 API를 연속 호출하면, 첫 호출이 활성화 상태를 소비해 이후 호출은 활성화가 없는 것으로 취급될 수 있다.

```javascript
button.addEventListener("click", () => {
  window.open("https://a.example.com"); // 활성화 상태 소비 가능
  window.open("https://b.example.com"); // 브라우저에 따라 차단될 수 있음
});
```

이 동작은 표준 명세가 세부 동작을 구현체 재량에 맡기고 있어 브라우저별로 다를 수 있다.

---

## 5. 자동화(CDP)와의 관계

[CDP Input 도메인](../cdp/input.md)의 `dispatchMouseEvent`, `dispatchKeyEvent`로 생성된 이벤트는 실제 하드웨어 입력과 동일하게 [Trusted Event](./trusted-input-implementations.md)로 처리되며, User Activation 상태도 함께 활성화된다. 반면 페이지 JS가 `element.dispatchEvent(new MouseEvent("click"))`처럼 합성한 이벤트는 `isTrusted: false`이며 User Activation을 발생시키지 않는다. 이 차이 때문에 브라우저 자동화 도구는 팝업/클립보드/전체화면 등을 트리거하는 시나리오에서 반드시 CDP `Input` 도메인을 거쳐야 한다.

---

## 6. 요약

- User Activation은 사용자의 실제 조작 여부를 추적해 오남용 가능한 API 호출을 제한하는 브라우저 내부 상태다.
- Sticky(문서 전체 지속)와 Transient(짧은 시간만 유효) 두 종류가 있다.
- 팝업, 클립보드, 전체화면, 결제 요청 등 다수 API가 Transient Activation을 요구한다.
- CDP `Input` 도메인으로 생성한 이벤트만 실제 사용자 입력처럼 이 상태를 활성화시킨다.

---

## 참고 자료

- [MDN User activation](https://developer.mozilla.org/docs/Web/Security/User_activation)
- [CDP Input 도메인](../cdp/input.md)
- [Trusted Input 구현 비교](./trusted-input-implementations.md)

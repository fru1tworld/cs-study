# User Activation

## 1. 개요

User Activation(사용자 활성화)은 브라우저가 "이 동작이 실제 사용자의 의도적 조작에서 비롯됐는가"를 추적하는 내부 상태다. 팝업 차단, 자동재생 정책, 클립보드 쓰기, 전체화면 진입, 결제 요청처럼 오남용될 수 있는 API 다수가 "유효한 사용자 활성화 상태에서만 호출 가능"이라는 제약을 둔다.

## 2. 두 가지 상태

- Sticky Activation
  - 설명: 문서가 한 번이라도 사용자 활성화를 받은 적이 있는지(`navigator.userActivation.hasBeenActive`)
  - 지속 시간: 문서 수명 전체 동안 유지
- Transient Activation
  - 설명: 가장 최근 사용자 제스처로부터 짧은 시간 동안만 유효(`navigator.userActivation.isActive`)
  - 지속 시간: 수 초 내외의 짧은 타이머(브라우저별 상이, 명세는 구체 수치를 강제하지 않음)

```javascript
button.addEventListener("click", () => {
  console.log(navigator.userActivation.isActive); // true (클릭 직후)
  setTimeout(() => {
    console.log(navigator.userActivation.isActive); // 시간 경과 후 false
  }, 10_000);
});
```

## 3. Transient Activation이 필요한 대표 API

- `window.open()`(팝업): 사용자 의도 없는 팝업 스팸 방지
- `element.requestFullscreen()`: 사용자 동의 없는 전체화면 전환 방지
- `navigator.clipboard.writeText()`: 사용자 모르게 클립보드를 덮어쓰는 것 방지
- `<video>.play()`(자동재생 정책): 소리 있는 미디어 자동 재생으로 인한 UX 저하 방지
- `PaymentRequest.show()`: 사용자 개입 없는 결제 UI 노출 방지
- Web Share API (`navigator.share()`): 임의 시점에 공유 시트가 뜨는 것 방지

## 4. 소비(consumption) 모델

일부 API는 Transient Activation을 사용하면서 소비한다. 따라서 한 번의 클릭으로 제한된 API를 여러 번 호출하면, 첫 호출이 활성화 상태를 소비해 뒤의 호출은 활성화가 없는 것으로 처리될 수 있다. 다음 예제에서도 두 번째 팝업은 차단될 수 있다.

```javascript
button.addEventListener("click", () => {
  window.open("https://a.example.com"); // 활성화 상태 소비 가능
  window.open("https://b.example.com"); // 브라우저에 따라 차단될 수 있음
});
```

표준 명세가 세부 동작을 구현체 재량에 맡기고 있어 이 동작은 브라우저마다 다를 수 있다.

## 5. 자동화(CDP)와의 관계

[CDP Input 도메인](../cdp/05_input.md)의 `dispatchMouseEvent`, `dispatchKeyEvent`로 생성된 이벤트는 실제 하드웨어 입력과 동일하게 [Trusted Event](./08_trusted-input-implementations.md)로 처리되며, User Activation 상태도 함께 활성화한다. 반면 페이지 JS가 `element.dispatchEvent(new MouseEvent("click"))`처럼 합성한 이벤트는 `isTrusted: false`이며 User Activation을 발생시키지 않는다. 이 차이 때문에 브라우저 자동화 도구는 팝업, 클립보드, 전체화면 등을 트리거하는 시나리오에서 반드시 CDP `Input` 도메인을 거쳐야 한다.

## 참고 자료

- [MDN User activation](https://developer.mozilla.org/docs/Web/Security/User_activation)
- [CDP Input 도메인](../cdp/05_input.md)
- [Trusted Input 구현 비교](./08_trusted-input-implementations.md)

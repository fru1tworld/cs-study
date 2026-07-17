# MDN 입력 이벤트 레퍼런스 정리

브라우저 자동화/신뢰된 입력과 관련해 자주 참조하는 MDN 개념 네 가지를 모았다.

## 1. Event.isTrusted

`Event.isTrusted`는 이벤트가 브라우저 자체(실제 사용자 조작 또는 [CDP Input 도메인](../cdp/input.md) 같은 브라우저 프로세스 수준 API)에 의해 생성되었는지, 아니면 페이지 JavaScript가 `dispatchEvent()`로 합성했는지를 구분하는 읽기 전용 불리언 속성이다.

```javascript
document.addEventListener("click", (e) => {
  console.log(e.isTrusted); // 실제 마우스 클릭이면 true
});

element.dispatchEvent(new MouseEvent("click"));
// 이 이벤트의 isTrusted는 항상 false
```

`isTrusted: false`인 이벤트는 일반적인 이벤트 리스너는 그대로 트리거하지만, 브라우저가 내부적으로 "신뢰된 사용자 조작"을 요구하는 동작(파일 업로드 다이얼로그 자동 열기, [User Activation](./user-activation.md) 소비 등)에는 영향을 주지 못한다.

---

## 2. User Activation

사용자의 실제 조작 여부를 추적하는 브라우저 상태로, 팝업 차단·자동재생 정책·클립보드 접근 등 다수 API의 호출 가능 여부를 결정한다. 상세 내용은 [user-activation.md](./user-activation.md) 문서에서 다룬다.

---

## 3. compositionstart

`compositionstart`는 IME(입력기)를 사용하는 텍스트 입력(한글, 일본어, 중국어 등)에서 조합이 시작될 때 발생하는 이벤트다. 사용자가 자음/모음을 눌러 글자를 조합하는 동안에는 `compositionupdate`가 반복 발생하고, 조합이 확정되면 `compositionend`가 발생한다.

```javascript
input.addEventListener("compositionstart", () => console.log("조합 시작"));
input.addEventListener("compositionupdate", (e) => console.log("조합 중:", e.data));
input.addEventListener("compositionend", (e) => console.log("조합 확정:", e.data));
```

| 이벤트 | 시점 |
|--------|------|
| `compositionstart` | IME 조합 시작 (예: 한글 자음 첫 입력) |
| `compositionupdate` | 조합 중인 글자가 바뀔 때마다 |
| `compositionend` | 조합 완료, 최종 문자 확정 |

브라우저 자동화에서 [CDP Input.dispatchKeyEvent](../cdp/input.md)만으로는 실제 IME 조합 과정을 재현하기 어렵기 때문에, 한글 등 조합형 문자 입력은 보통 `Input.insertText`로 조합이 끝난 최종 문자열을 한 번에 삽입해 이 문제를 우회한다. 다만 이 경우 페이지가 `compositionstart`/`compositionend`에 의존한 로직(일부 자동완성/맞춤법 검사기)을 두고 있다면 그 로직은 트리거되지 않을 수 있다.

---

## 4. devicePixelRatio

`window.devicePixelRatio`는 CSS 픽셀 1개가 실제 디바이스(물리) 픽셀 몇 개에 대응하는지를 나타내는 비율이다.

```javascript
console.log(window.devicePixelRatio);
// 일반 디스플레이: 1
// Retina/고DPI 디스플레이: 2 또는 3
```

| 항목 | 관계 |
|------|------|
| CSS 픽셀 좌표 (CDP `Input`/`DOM`이 사용) | `devicePixelRatio`와 무관하게 그대로 사용 가능 |
| 스크린샷 이미지의 픽셀 좌표 (`Page.captureScreenshot`) | 물리 픽셀 기준이므로, CSS 좌표로 변환하려면 `devicePixelRatio`로 나눠야 함 |

자세한 좌표계 구분은 [Blink 좌표 공간](./blink-coordinate-spaces.md) 문서를 참고한다.

---

## 5. 요약

- `isTrusted`는 이벤트가 브라우저 생성인지 페이지 JS 합성인지 구분하는 기준이다.
- IME 조합 이벤트(`compositionstart` 등)는 CDP 키 이벤트만으로 완전히 재현하기 어려워 `insertText`로 우회하는 경우가 많다.
- `devicePixelRatio`는 CSS 픽셀과 스크린샷의 물리 픽셀 좌표를 변환할 때 필요하다.

---

## 참고 자료

- [MDN Event.isTrusted](https://developer.mozilla.org/docs/Web/API/Event/isTrusted)
- [MDN User activation](https://developer.mozilla.org/docs/Web/Security/User_activation)
- [MDN compositionstart](https://developer.mozilla.org/docs/Web/API/Element/compositionstart_event)
- [MDN devicePixelRatio](https://developer.mozilla.org/docs/Web/API/Window/devicePixelRatio)

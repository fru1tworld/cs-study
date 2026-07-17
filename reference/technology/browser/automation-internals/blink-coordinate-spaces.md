# Blink 좌표 공간(Coordinate Spaces) 개요

## 1. 왜 여러 좌표계가 존재하는가

브라우저 렌더링 엔진(Blink)은 화면 배율, 스크롤, 페이지 확대/축소, iframe 중첩 등 다양한 변환이 겹치는 환경에서 요소의 위치를 다뤄야 한다. 이 때문에 "좌표"라는 말이 가리키는 기준점이 문맥에 따라 달라지며, 자동화 도구에서 클릭 좌표가 어긋나는 버그의 상당수는 좌표계 혼동에서 발생한다.

---

## 2. 주요 좌표 공간

| 좌표계 | 기준 | 영향 요소 |
|--------|------|-----------|
| CSS 픽셀 (viewport 좌표) | 현재 뷰포트의 좌상단 (0,0), 스크롤 위치 반영 | `getBoundingClientRect()`, CDP `Input`/`DOM.getBoxModel` 대부분이 이 기준 사용 |
| 문서 좌표 (document 좌표) | 문서 전체의 좌상단, 스크롤과 무관 | `pageX`/`pageY` (스크롤 오프셋을 더한 값) |
| 물리 픽셀 (device 좌표) | 실제 디스플레이 픽셀, `devicePixelRatio` 반영 | 스크린샷 캡처, 고DPI 디스플레이 렌더링 |
| 화면 좌표 (screen 좌표) | 운영체제 화면 전체 기준 | `window.screenX/Y`, OS 수준 입력 이벤트 |

```
CSS 픽셀 좌표 × devicePixelRatio = 물리 픽셀 좌표
```

---

## 3. CDP Input/DOM에서 사용하는 기준

[CDP Input 도메인](../cdp/input.md)의 `dispatchMouseEvent`와 [DOM 도메인](../cdp/dom.md)의 `getBoxModel`은 모두 **CSS 픽셀 기준 뷰포트 좌표**를 사용한다. 즉 `devicePixelRatio`가 2인 Retina 디스플레이에서도 좌표값을 물리 픽셀로 변환할 필요가 없다. 반면 `Page.captureScreenshot`로 얻은 이미지의 픽셀 좌표는 물리 픽셀 기준이므로, 스크린샷에서 좌표를 역산해 클릭 위치를 계산할 때는 배율 보정이 필요하다.

| 작업 | 사용하는 좌표계 |
|------|------------------|
| `Input.dispatchMouseEvent` | CSS 픽셀 (뷰포트) |
| `DOM.getBoxModel` | CSS 픽셀 (뷰포트) |
| `Page.captureScreenshot` 결과 이미지 | 물리 픽셀 (devicePixelRatio 반영) |
| `window.devicePixelRatio` | 두 좌표계 간 변환 비율 제공 ([MDN 참고](./mdn-input-events-reference.md)) |

---

## 4. iframe 중첩과 좌표 변환

중첩된 iframe 내부 요소의 좌표는 자신이 속한 iframe 기준의 로컬 좌표다. 최상위 페이지 기준 절대 좌표를 구하려면 각 상위 프레임의 오프셋을 누적해서 더해야 한다.

```
최상위 페이지 좌표
  = iframe.getBoundingClientRect() 오프셋
  + (iframe 내부 요소의 로컬 뷰포트 좌표)
```

크로스 오리진 iframe(OOPIF)의 경우 부모 페이지의 JS에서 `getBoundingClientRect()`로 내부 요소 좌표를 직접 읽을 수 없으므로 ([Same-Origin Policy](../../../standard/RFC/Layer7-Application/HTTP/RFC6454-Web-Origin.md) 제약), CDP로 각 프레임에 개별 세션을 맺어 좌표를 조합해야 한다. 관련 구조는 [OOPIF / Site Isolation](./oopif-site-isolation.md) 문서를 참고한다.

---

## 5. 확대/축소(Page Zoom)의 영향

브라우저의 페이지 확대(Ctrl+`+`/`-`)는 CSS 픽셀 좌표계 자체를 재계산하므로, CDP 좌표 기반 자동화는 보통 이 값에 자동으로 맞춰진다. 반면 OS 레벨 디스플레이 배율(모니터 설정의 150% 등)은 `devicePixelRatio`에 반영되어 스크린샷/물리 좌표 계산에서만 영향을 준다.

---

## 6. 요약

- Blink는 CSS 픽셀, 문서 좌표, 물리 픽셀, 화면 좌표 등 여러 좌표계를 구분해서 다룬다.
- CDP `Input`/`DOM` 도메인은 CSS 픽셀(뷰포트) 좌표를 쓰지만, 스크린샷 이미지는 물리 픽셀 기준이다.
- iframe 중첩 시 좌표는 로컬 기준이므로 상위 프레임 오프셋을 누적해야 하고, 크로스 오리진 iframe은 CDP 세션 분리가 필요할 수 있다.

---

## 참고 자료

- [CDP Input 도메인](../cdp/input.md)
- [CDP DOM 도메인](../cdp/dom.md)
- [MDN 입력 이벤트 레퍼런스 (devicePixelRatio)](./mdn-input-events-reference.md)
- [OOPIF / Site Isolation](./oopif-site-isolation.md)

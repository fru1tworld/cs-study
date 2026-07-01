# Compatibility Standard 상세 가이드

## 목차

1. [개요](#1-개요)
2. [왜 호환성 표준이 필요한가](#2-왜-호환성-표준이-필요한가)
3. [CSS 호환성](#3-css-호환성)
4. [DOM/JS 호환성](#4-domjs-호환성)
5. [터치 이벤트 호환성](#5-터치-이벤트-호환성)
6. [렌더링 호환성](#6-렌더링-호환성)
7. [Vendor Prefix 표준화 과정](#7-vendor-prefix-표준화-과정)
8. [브라우저별 호환성 현황](#8-브라우저별-호환성-현황)
9. [실용 예제](#9-실용-예제)
10. [개발자를 위한 권장사항](#10-개발자를-위한-권장사항)

---

## 1. 개요

Compatibility Standard는 WHATWG에서 관리하는 Living Standard 중 하나로, 웹 브라우저 간의 호환성을 보장하기 위해 비표준(non-standard) 기능들을 공식적으로 문서화한 표준이다.

### 핵심 개념

웹의 역사를 통해 특정 브라우저(특히 WebKit/Blink 기반 브라우저)에서만 지원하던 비표준 기능들이 널리 사용되면서, 다른 브라우저들도 이를 지원해야 하는 상황이 발생했다. Compatibility Standard는 이러한 기능들을 정리하고, 모든 브라우저가 일관되게 구현할 수 있도록 명세를 제공한다.

```
[역사적 비표준 기능] → [광범위한 웹 사용] → [호환성 문제 발생] → [Compatibility Standard로 표준화]
```

### 표준의 범위

| 영역 | 설명 |
|------|------|
| CSS 호환성 | `-webkit-` 접두사 속성의 표준화 |
| DOM/JS 호환성 | `window.orientation` 등 비표준 API |
| 터치 이벤트 | WebKit 기반 터치 이벤트 |
| 렌더링 호환성 | 특정 렌더링 동작의 표준화 |

---

## 2. 왜 호환성 표준이 필요한가

### 레거시 웹과의 호환

웹의 발전 과정에서 브라우저 제조사들은 경쟁적으로 새로운 기능을 도입했다. 특히 모바일 웹 초기에 WebKit(Safari, 초기 Chrome) 엔진이 시장을 지배하면서, 많은 웹사이트가 WebKit 전용 기능에 의존하게 되었다.

```
[2007-2013 모바일 웹 초기]
├── Safari (WebKit) - iOS 독점
├── Chrome (WebKit → Blink) - Android 주력
├── Opera (Presto → Blink) - 엔진 변경
└── Firefox (Gecko) - 호환성 문제 직면
```

### 문제의 발생

```javascript
// 많은 모바일 웹사이트가 이런 코드를 사용
if ('onorientationchange' in window) {
    // 모바일 디바이스로 판단
    window.addEventListener('orientationchange', handleOrientation);
}

// -webkit- 접두사만 사용
.element {
    -webkit-transform: rotate(45deg);
    /* 표준 속성 없음! */
}
```

이러한 코드가 WebKit/Blink 이외의 브라우저에서 동작하지 않아, 사용자 경험에 심각한 문제가 발생했다.

### 해결 방안으로서의 Compatibility Standard

Mozilla(Firefox)와 다른 브라우저 제조사들은 두 가지 선택지에 직면했다:

1. 비표준 기능을 무시 - 많은 웹사이트가 깨짐
2. 비표준 기능을 구현 - 표준 없이 각자 구현하면 또 다른 호환성 문제 발생

결국 WHATWG에서 이러한 비표준 기능들을 공식 표준으로 문서화하여, 모든 브라우저가 동일하게 구현할 수 있도록 하는 것이 최선의 해결책이 되었다.

---

## 3. CSS 호환성

### 3.1 -webkit- 접두사 속성 개요

Compatibility Standard에서 가장 큰 비중을 차지하는 것이 CSS `-webkit-` 접두사 속성의 표준화다.

#### Vendor Prefix 시스템

| 접두사 | 브라우저/엔진 |
|--------|--------------|
| `-webkit-` | WebKit, Blink (Safari, Chrome, Edge) |
| `-moz-` | Gecko (Firefox) |
| `-ms-` | Trident (IE), EdgeHTML (구 Edge) |
| `-o-` | Presto (구 Opera) |

원래 vendor prefix는 실험적 기능을 안전하게 노출하기 위한 메커니즘이었으나, 웹 개발자들이 `-webkit-` 접두사 속성을 프로덕션 코드에 광범위하게 사용하면서 사실상 표준처럼 되어버렸다.

### 3.2 -webkit-transform 관련

```css
/* Compatibility Standard에 의해 모든 브라우저가 지원해야 하는 속성 */
.element {
    -webkit-transform: rotate(45deg);        /* → transform으로 매핑 */
    -webkit-transform-origin: center center;  /* → transform-origin으로 매핑 */
    -webkit-transform-style: preserve-3d;     /* → transform-style로 매핑 */
    -webkit-perspective: 1000px;              /* → perspective로 매핑 */
    -webkit-perspective-origin: 50% 50%;      /* → perspective-origin으로 매핑 */
    -webkit-backface-visibility: hidden;      /* → backface-visibility로 매핑 */
}
```

#### 매핑 메커니즘

```
파싱 단계:
"-webkit-transform: rotate(45deg)"
    ↓ [Compatibility Standard 매핑 규칙]
"transform: rotate(45deg)" 으로 처리
```

### 3.3 -webkit-transition 관련

```css
.element {
    -webkit-transition: all 0.3s ease;
    -webkit-transition-property: opacity;
    -webkit-transition-duration: 0.3s;
    -webkit-transition-timing-function: ease-in-out;
    -webkit-transition-delay: 0.1s;
}

/* 위의 속성들은 각각 표준 속성으로 매핑됨 */
/*
    -webkit-transition           → transition
    -webkit-transition-property  → transition-property
    -webkit-transition-duration  → transition-duration
    -webkit-transition-timing-function → transition-timing-function
    -webkit-transition-delay     → transition-delay
*/
```

### 3.4 -webkit-animation 관련

```css
@-webkit-keyframes fadeIn {
    from { opacity: 0; }
    to { opacity: 1; }
}

.element {
    -webkit-animation: fadeIn 1s ease-in;
    -webkit-animation-name: fadeIn;
    -webkit-animation-duration: 1s;
    -webkit-animation-timing-function: ease-in;
    -webkit-animation-delay: 0s;
    -webkit-animation-iteration-count: infinite;
    -webkit-animation-direction: alternate;
    -webkit-animation-fill-mode: both;
    -webkit-animation-play-state: running;
}

/* 모두 표준 animation 속성으로 매핑 */
```

### 3.5 -webkit-appearance

`-webkit-appearance`는 폼 요소의 네이티브 스타일링을 제어하는 속성이다.

```css
/* 네이티브 스타일 제거 */
input, button, select, textarea {
    -webkit-appearance: none;
    appearance: none;  /* 표준 속성 */
}

/* 지원되는 값 */
.element {
    -webkit-appearance: none;
    -webkit-appearance: auto;
    -webkit-appearance: button;
    -webkit-appearance: textfield;
    -webkit-appearance: checkbox;
    -webkit-appearance: radio;
    -webkit-appearance: menulist;
    -webkit-appearance: listbox;
    -webkit-appearance: meter;
    -webkit-appearance: progress-bar;
    -webkit-appearance: searchfield;
    -webkit-appearance: textarea;
}
```

### 3.6 -webkit-filter

```css
.element {
    -webkit-filter: blur(5px);
    -webkit-filter: brightness(0.5);
    -webkit-filter: contrast(200%);
    -webkit-filter: drop-shadow(16px 16px 20px blue);
    -webkit-filter: grayscale(50%);
    -webkit-filter: hue-rotate(90deg);
    -webkit-filter: invert(75%);
    -webkit-filter: opacity(25%);
    -webkit-filter: saturate(30%);
    -webkit-filter: sepia(60%);

    /* 여러 필터 조합 */
    -webkit-filter: contrast(175%) brightness(3%);
}

/* → filter 표준 속성으로 매핑 */
```

### 3.7 Flexbox 관련 -webkit- 속성

Flexbox는 구 사양(2009)과 신 사양(2012+)이 있어, 호환성이 특히 복잡하다.

```css
/* 구 Flexbox (-webkit- 접두사, 2009 사양) */
.container {
    display: -webkit-box;           /* → display: flex */
    display: -webkit-inline-box;    /* → display: inline-flex */
    -webkit-box-orient: horizontal; /* → flex-direction */
    -webkit-box-direction: normal;  /* → flex-direction */
    -webkit-box-pack: center;       /* → justify-content: center */
    -webkit-box-align: center;      /* → align-items: center */
    -webkit-box-ordinal-group: 1;   /* → order: 0 */
    -webkit-box-flex: 1;            /* → flex: 1 */
    -webkit-box-lines: multiple;    /* → flex-wrap: wrap */
}

/* Compatibility Standard에 의한 매핑 규칙 */
/*
    display: -webkit-box       → display: flex
    display: -webkit-inline-box → display: inline-flex
    -webkit-box-orient + -webkit-box-direction → flex-direction
        horizontal + normal = row
        horizontal + reverse = row-reverse
        vertical + normal = column
        vertical + reverse = column-reverse
    -webkit-box-pack:
        start   → justify-content: flex-start
        center  → justify-content: center
        end     → justify-content: flex-end
        justify → justify-content: space-between
    -webkit-box-align:
        start    → align-items: flex-start
        center   → align-items: center
        end      → align-items: flex-end
        baseline → align-items: baseline
        stretch  → align-items: stretch
*/
```

### 3.8 -webkit-text-size-adjust

모바일 브라우저에서 텍스트 크기 자동 조절을 제어한다.

```css
/* 텍스트 자동 크기 조절 비활성화 */
body {
    -webkit-text-size-adjust: 100%;
    -webkit-text-size-adjust: none;   /* 자동 조절 완전 비활성화 */
    -webkit-text-size-adjust: auto;   /* 브라우저 기본 동작 */
}
```

### 3.9 기타 -webkit- CSS 속성

```css
/* -webkit-background-clip: text */
.gradient-text {
    background: linear-gradient(to right, #ff0000, #0000ff);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    /* → background-clip: text 으로 매핑 */
}

/* -webkit-text-fill-color */
.element {
    -webkit-text-fill-color: red;
    /* 텍스트의 채우기(fill) 색상 설정 */
}

/* -webkit-text-stroke */
.element {
    -webkit-text-stroke: 2px black;
    -webkit-text-stroke-width: 2px;
    -webkit-text-stroke-color: black;
}

/* -webkit-opacity → opacity */
.element {
    -webkit-opacity: 0.5;  /* → opacity: 0.5 */
}

/* -webkit-align-items, -webkit-align-content 등 */
.container {
    -webkit-align-items: center;        /* → align-items */
    -webkit-align-content: center;      /* → align-content */
    -webkit-align-self: center;         /* → align-self */
    -webkit-justify-content: center;    /* → justify-content */
    -webkit-flex: 1;                    /* → flex */
    -webkit-flex-basis: auto;           /* → flex-basis */
    -webkit-flex-direction: row;        /* → flex-direction */
    -webkit-flex-flow: row wrap;        /* → flex-flow */
    -webkit-flex-grow: 1;              /* → flex-grow */
    -webkit-flex-shrink: 0;            /* → flex-shrink */
    -webkit-flex-wrap: wrap;           /* → flex-wrap */
    -webkit-order: 1;                  /* → order */
}
```

### 3.10 -webkit- 접두사 속성 전체 매핑 표

| -webkit- 속성 | 표준 속성 | 비고 |
|---------------|----------|------|
| `-webkit-transform` | `transform` | |
| `-webkit-transform-origin` | `transform-origin` | |
| `-webkit-transform-style` | `transform-style` | |
| `-webkit-perspective` | `perspective` | |
| `-webkit-perspective-origin` | `perspective-origin` | |
| `-webkit-backface-visibility` | `backface-visibility` | |
| `-webkit-transition` | `transition` | |
| `-webkit-animation` | `animation` | |
| `-webkit-filter` | `filter` | |
| `-webkit-appearance` | `appearance` | |
| `-webkit-background-clip` | `background-clip` | `text` 값 포함 |
| `-webkit-opacity` | `opacity` | |
| `-webkit-order` | `order` | |
| `-webkit-flex` | `flex` | |
| `display: -webkit-box` | `display: flex` | 구 Flexbox |
| `display: -webkit-flex` | `display: flex` | 신 Flexbox |
| `-webkit-box-orient` | `flex-direction` (부분) | 복합 매핑 |
| `-webkit-box-pack` | `justify-content` | 값 매핑 필요 |
| `-webkit-box-align` | `align-items` | 값 매핑 필요 |

---

## 4. DOM/JS 호환성

### 4.1 window.orientation

`window.orientation` 속성은 Apple이 iOS Safari에서 처음 도입한 비표준 API로, 디바이스의 현재 화면 방향을 각도로 반환한다.

```javascript
// window.orientation 값
// 0   : 세로 모드 (portrait, 기본)
// 90  : 가로 모드 (landscape, 왼쪽으로 회전)
// -90 : 가로 모드 (landscape, 오른쪽으로 회전)
// 180 : 거꾸로 세로 모드 (portrait, 뒤집힘)

console.log(window.orientation);
// 출력: 0, 90, -90, 또는 180
```

#### 방향 다이어그램

```
window.orientation 값:

    0° (Portrait)          90° (Landscape Left)
    ┌──────────┐           ┌──────────────────┐
    │          │           │                  │
    │          │           │                  │
    │  SCREEN  │           │     SCREEN       │
    │          │           │                  │
    │          │           │                  │
    │          │           └──────────────────┘
    │   [  ]   │
    └──────────┘

   -90° (Landscape Right)  180° (Portrait Upside Down)
    ┌──────────────────┐   ┌──────────┐
    │                  │   │   [  ]   │
    │                  │   │          │
    │     SCREEN       │   │          │
    │                  │   │  SCREEN  │
    │                  │   │          │
    └──────────────────┘   │          │
                           └──────────┘
```

### 4.2 orientationchange 이벤트

```javascript
// orientationchange 이벤트 리스닝
window.addEventListener('orientationchange', function(event) {
    console.log('화면 방향 변경:', window.orientation);

    switch (window.orientation) {
        case 0:
            console.log('세로 모드');
            break;
        case 90:
            console.log('가로 모드 (왼쪽 회전)');
            break;
        case -90:
            console.log('가로 모드 (오른쪽 회전)');
            break;
        case 180:
            console.log('거꾸로 세로 모드');
            break;
    }
});

// 인라인 이벤트 핸들러
window.onorientationchange = function() {
    // ...
};
```

### 4.3 Screen Orientation API와의 관계

`window.orientation`은 비표준이며, 표준 대안은 Screen Orientation API이다.

```javascript
// [비표준] window.orientation
console.log(window.orientation); // 0, 90, -90, 180

// [표준] Screen Orientation API
console.log(screen.orientation.type);
// "portrait-primary", "portrait-secondary",
// "landscape-primary", "landscape-secondary"

console.log(screen.orientation.angle);
// 0, 90, 180, 270

// 표준 이벤트
screen.orientation.addEventListener('change', function() {
    console.log('방향:', screen.orientation.type);
    console.log('각도:', screen.orientation.angle);
});
```

#### 비교 표

| 기능 | `window.orientation` (비표준) | `screen.orientation` (표준) |
|------|------------------------------|---------------------------|
| 반환값 | 0, 90, -90, 180 | type 문자열 + angle 숫자 |
| 이벤트 | `orientationchange` | `change` |
| 이벤트 대상 | `window` | `screen.orientation` |
| 잠금 기능 | 없음 | `lock()`, `unlock()` |
| Compatibility Standard | 포함 | W3C Screen Orientation API |

### 4.4 webkit 접두사 DOM API

```javascript
// webkitRequestAnimationFrame
// Compatibility Standard에서 표준화
window.webkitRequestAnimationFrame(callback);
// → window.requestAnimationFrame(callback) 으로 매핑

// webkitCancelAnimationFrame
window.webkitCancelAnimationFrame(id);
// → window.cancelAnimationFrame(id) 으로 매핑

// webkitURL
window.webkitURL;
// → window.URL 으로 매핑

// HTMLVideoElement의 webkit 접두사 메서드
video.webkitEnterFullscreen();
video.webkitExitFullscreen();
video.webkitDisplayingFullscreen;   // 읽기 전용 속성
video.webkitSupportsFullscreen;     // 읽기 전용 속성
```

---

## 5. 터치 이벤트 호환성

### 5.1 터치 이벤트 모델

Compatibility Standard는 터치 이벤트의 호환성 동작도 정의한다. WebKit에서 처음 구현된 터치 이벤트 모델은 이후 W3C Touch Events 사양으로 표준화되었지만, 일부 호환성 관련 동작은 Compatibility Standard에서 다룬다.

```javascript
// 기본 터치 이벤트
element.addEventListener('touchstart', function(e) {
    const touch = e.touches[0];
    console.log('터치 시작:', touch.clientX, touch.clientY);
});

element.addEventListener('touchmove', function(e) {
    const touch = e.touches[0];
    console.log('터치 이동:', touch.clientX, touch.clientY);
});

element.addEventListener('touchend', function(e) {
    console.log('터치 종료');
});

element.addEventListener('touchcancel', function(e) {
    console.log('터치 취소');
});
```

### 5.2 마우스 이벤트와의 호환성

터치 디바이스에서 마우스 이벤트와의 호환성 순서가 중요하다.

```
터치 이벤트 시퀀스:

[사용자가 화면 터치]
    ↓
touchstart
    ↓
touchmove (0회 이상)
    ↓
touchend
    ↓
[약 300ms 지연] ← 더블 탭 감지를 위해
    ↓
mousemove
    ↓
mousedown
    ↓
mouseup
    ↓
click
```

### 5.3 ontouchstart 속성

```javascript
// Compatibility Standard에서 정의하는 ontouchstart 등의 이벤트 핸들러
// 이 속성들이 존재하는지 확인하여 터치 지원을 감지하는 코드가 많음

// 흔한 (하지만 부정확한) 터치 감지 방법
if ('ontouchstart' in window) {
    // 터치 디바이스로 판단
}

// Compatibility Standard는 데스크톱 브라우저에서도
// ontouchstart가 존재할 수 있음을 명시
// → 터치 감지에 이 방법만 사용하면 안 됨

// 더 나은 방법
if (window.matchMedia('(pointer: coarse)').matches) {
    // 대략적인 포인팅 디바이스 (터치)
}

if (window.matchMedia('(hover: none)').matches) {
    // 호버를 지원하지 않는 디바이스
}
```

### 5. 4 터치 이벤트와 관련된 호환성 문제

```javascript
// preventDefault()의 호환성 동작
element.addEventListener('touchstart', function(e) {
    // passive: false가 아니면 preventDefault()가 무시될 수 있음
    e.preventDefault();
}, { passive: false });

// Chrome은 성능을 위해 touchstart를 passive로 기본 설정
// Compatibility Standard는 이 동작도 문서화

// Touch 인터페이스의 호환성 속성
element.addEventListener('touchstart', function(e) {
    const touch = e.touches[0];

    // 표준 속성
    touch.identifier;
    touch.target;
    touch.clientX;
    touch.clientY;
    touch.screenX;
    touch.screenY;
    touch.pageX;
    touch.pageY;

    // WebKit 비표준 속성 (Compatibility Standard)
    touch.radiusX;     // 터치 영역 반경 X
    touch.radiusY;     // 터치 영역 반경 Y
    touch.rotationAngle; // 터치 영역 회전각
    touch.force;       // 터치 압력 (0.0 ~ 1.0)
});
```

---

## 6. 렌더링 호환성

### 6.1 -webkit- 접두사의 렌더링 영향

Compatibility Standard는 특정 렌더링 동작의 호환성도 정의한다.

```css
/* -webkit-text-size-adjust의 렌더링 영향 */
/* 모바일에서 가로 모드 전환 시 텍스트 크기 자동 조절 */
html {
    -webkit-text-size-adjust: 100%;
    text-size-adjust: 100%;
}

/* 이 속성이 없으면 일부 브라우저에서
   가로 모드 전환 시 텍스트가 확대됨 */
```

### 6.2 -webkit-line-clamp

여러 줄 텍스트 말줄임 처리를 위한 비표준 속성으로, 매우 널리 사용된다.

```css
/* 3줄 말줄임 */
.multiline-ellipsis {
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

/* Compatibility Standard에 의해 모든 브라우저가 지원해야 함 */
/* 이 세 가지 속성의 조합이 필수 */
/*
    display: -webkit-box;      → line-clamp의 전제 조건
    -webkit-box-orient: vertical; → 세로 방향 설정
    -webkit-line-clamp: N;     → N줄 제한
*/
```

### 6.3 -webkit-tap-highlight-color

모바일에서 터치 시 나타나는 하이라이트 색상을 제어한다.

```css
/* 터치 하이라이트 제거 */
* {
    -webkit-tap-highlight-color: transparent;
    -webkit-tap-highlight-color: rgba(0, 0, 0, 0);
}

/* 커스텀 하이라이트 색상 */
a, button {
    -webkit-tap-highlight-color: rgba(0, 100, 255, 0.3);
}
```

### 6.4 -webkit-overflow-scrolling

iOS에서 관성(momentum) 스크롤을 활성화하는 속성이다.

```css
.scrollable {
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;  /* 관성 스크롤 활성화 */
}

/* 값:
   auto  - 일반 스크롤 (관성 없음)
   touch - 관성 스크롤 (네이티브 스크롤처럼 동작)
*/
```

### 6.5 -webkit-user-select

텍스트 선택 가능 여부를 제어한다.

```css
/* 텍스트 선택 제어 */
.no-select {
    -webkit-user-select: none;
    user-select: none;
}

.all-select {
    -webkit-user-select: all;
    user-select: all;
}

/* 값:
   none - 선택 불가
   text - 텍스트만 선택 가능 (기본)
   all  - 클릭 시 전체 요소 선택
   auto - 기본 동작
   contain - 선택 범위를 요소 내로 제한
*/
```

---

## 7. Vendor Prefix 표준화 과정

### 7.1 역사적 배경

```
[Timeline: Vendor Prefix의 역사]

2005-2009: Vendor prefix 도입기
├── CSS3 모듈 초안 단계
├── 브라우저들이 실험적 기능에 prefix 사용
└── -webkit-, -moz-, -ms-, -o- 도입

2009-2013: 문제 심화기
├── 모바일 웹 폭발적 성장
├── 개발자들이 -webkit-만 사용하는 관행 확산
├── Firefox, Opera 등에서 웹사이트 깨짐 발생
└── Mozilla가 -webkit- prefix 지원 검토 시작

2013-2016: 해결 모색기
├── Compatibility Standard 초안 작성
├── Firefox가 일부 -webkit- 속성 지원 시작
├── Edge(EdgeHTML)도 -webkit- 속성 지원 추가
└── Vendor prefix 사용 자제 권고 시작

2016-현재: 표준화 안정기
├── Compatibility Standard Living Standard 발행
├── 대부분의 -webkit- 속성이 표준 속성으로 대체
├── prefix 없는 표준 속성 사용이 일반화
└── 레거시 호환성을 위해 prefix 지원은 유지
```

### 7.2 Vendor Prefix 폐기 방향

현대 웹 개발에서 vendor prefix의 필요성은 크게 감소했다.

```css
/* [권장하지 않음] 과거 스타일 */
.element {
    -webkit-border-radius: 10px;
    -moz-border-radius: 10px;
    border-radius: 10px;
}

/* [권장] 현대 스타일 */
.element {
    border-radius: 10px;
}
```

### 7.3 여전히 필요한 Vendor Prefix

일부 속성은 아직 vendor prefix가 필요하다.

```css
/* 2024년 기준 여전히 -webkit- 필요한 경우 */

/* -webkit-line-clamp (대안 없음) */
.truncate {
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

/* -webkit-text-fill-color (일부 상황) */
.custom-text {
    -webkit-text-fill-color: transparent;
    background-clip: text;
    -webkit-background-clip: text;
}

/* -webkit-tap-highlight-color (모바일) */
button {
    -webkit-tap-highlight-color: transparent;
}
```

---

## 8. 브라우저별 호환성 현황

### 8.1 Compatibility Standard 지원 현황

| 기능 | Chrome | Firefox | Safari | Edge |
|------|--------|---------|--------|------|
| `-webkit-transform` | 지원 | 지원 | 지원 | 지원 |
| `-webkit-transition` | 지원 | 지원 | 지원 | 지원 |
| `-webkit-animation` | 지원 | 지원 | 지원 | 지원 |
| `-webkit-appearance` | 지원 | 지원 | 지원 | 지원 |
| `-webkit-filter` | 지원 | 지원 | 지원 | 지원 |
| `-webkit-flex` | 지원 | 지원 | 지원 | 지원 |
| `display: -webkit-box` | 지원 | 지원 | 지원 | 지원 |
| `-webkit-line-clamp` | 지원 | 지원 | 지원 | 지원 |
| `-webkit-text-size-adjust` | 지원 | 지원 | 지원 | 지원 |
| `-webkit-text-fill-color` | 지원 | 지원 | 지원 | 지원 |
| `-webkit-text-stroke` | 지원 | 지원 | 지원 | 지원 |
| `window.orientation` | 지원 | 지원 | 지원 | 지원 |
| `orientationchange` | 지원 | 지원 | 지원 | 지원 |
| `-webkit-user-select` | 지원 | 지원 | 지원 | 지원 |

### 8.2 Firefox의 -webkit- 접두사 지원 이력

```
Firefox의 -webkit- 지원 추가 이력:

Firefox 46 (2016.04):
  - -webkit-filter
  - -webkit-text-size-adjust

Firefox 47 (2016.06):
  - -webkit-mask 관련 속성

Firefox 49 (2016.09):
  - display: -webkit-box, -webkit-inline-box
  - -webkit-box-* 속성들
  - -webkit-line-clamp

Firefox 63 (2018.10):
  - -webkit-appearance

Firefox 64+ (2018.12):
  - 대부분의 -webkit- Flexbox 속성
```

---

## 9. 실용 예제

### 9.1 호환성을 고려한 CSS 작성

```css
/* 레거시 브라우저 호환성을 고려한 그라데이션 텍스트 */
.gradient-text {
    /* 폴백: 단색 텍스트 */
    color: #667eea;

    /* -webkit- 접두사 (Compatibility Standard) */
    background: -webkit-linear-gradient(left, #667eea, #764ba2);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;

    /* 표준 */
    background: linear-gradient(to right, #667eea, #764ba2);
    background-clip: text;
    color: transparent;
}

/* 호환성을 고려한 다중 줄 말줄임 */
.text-clamp {
    /* 폴백: 단일 줄 말줄임 */
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;

    /* -webkit-line-clamp 지원 브라우저 */
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    white-space: normal;
}
```

### 9.2 호환성을 고려한 JavaScript 작성

```javascript
// 화면 방향 감지 (호환성 고려)
function getOrientation() {
    // 표준 API 우선 시도
    if (screen.orientation) {
        return {
            type: screen.orientation.type,
            angle: screen.orientation.angle
        };
    }

    // Compatibility Standard: window.orientation 폴백
    if (typeof window.orientation !== 'undefined') {
        const angle = window.orientation;
        let type;
        switch (angle) {
            case 0: type = 'portrait-primary'; break;
            case 90: type = 'landscape-primary'; break;
            case -90: type = 'landscape-secondary'; break;
            case 180: type = 'portrait-secondary'; break;
            default: type = 'portrait-primary';
        }
        return { type, angle };
    }

    // 최종 폴백: 뷰포트 크기 기반 추정
    const isPortrait = window.innerHeight > window.innerWidth;
    return {
        type: isPortrait ? 'portrait-primary' : 'landscape-primary',
        angle: isPortrait ? 0 : 90
    };
}

// 방향 변경 이벤트 리스너 (호환성 고려)
function onOrientationChange(callback) {
    if (screen.orientation) {
        screen.orientation.addEventListener('change', function() {
            callback(getOrientation());
        });
    } else if ('onorientationchange' in window) {
        window.addEventListener('orientationchange', function() {
            // 약간의 지연 후 콜백 호출 (값 안정화)
            setTimeout(function() {
                callback(getOrientation());
            }, 100);
        });
    } else {
        // resize 이벤트로 폴백
        window.addEventListener('resize', function() {
            callback(getOrientation());
        });
    }
}

// 사용
onOrientationChange(function(orientation) {
    console.log('방향 변경:', orientation.type, orientation.angle);
});
```

### 9.3 PostCSS를 활용한 자동 접두사 관리

```javascript
// postcss.config.js
module.exports = {
    plugins: [
        require('autoprefixer')({
            // Autoprefixer가 Compatibility Standard에 맞는
            // -webkit- 접두사를 자동으로 추가
            overrideBrowserslist: [
                'last 2 versions',
                '> 1%',
                'iOS >= 10',
                'Android >= 5'
            ]
        })
    ]
};

// 입력 CSS:
// .element { transform: rotate(45deg); }

// 출력 CSS (Autoprefixer 처리 후):
// .element {
//     -webkit-transform: rotate(45deg);
//     transform: rotate(45deg);
// }
```

### 9.4 기능 감지 패턴

```javascript
// CSS 기능 감지 (Compatibility Standard 관련)
function supportsCSS(property, value) {
    // @supports 사용
    if (window.CSS && CSS.supports) {
        return CSS.supports(property, value);
    }

    // 폴백: DOM 요소 테스트
    const el = document.createElement('div');
    el.style[property] = value;
    return el.style[property] === value;
}

// -webkit-line-clamp 지원 확인
const supportsLineClamp = supportsCSS('-webkit-line-clamp', '3');

// appearance 지원 확인
const supportsAppearance =
    supportsCSS('appearance', 'none') ||
    supportsCSS('-webkit-appearance', 'none');

console.log('line-clamp 지원:', supportsLineClamp);
console.log('appearance 지원:', supportsAppearance);
```

---

## 10. 개발자를 위한 권장사항

### 10.1 현대 웹 개발에서의 접근 방식

```
우선순위:

1. 표준 속성 사용
   transform: rotate(45deg);              ← 우선 사용

2. 필요 시 -webkit- 접두사 병행
   -webkit-transform: rotate(45deg);       ← 레거시 지원 필요 시
   transform: rotate(45deg);

3. Autoprefixer 등 도구 활용
   postcss + autoprefixer로 자동 관리       ← 가장 권장

4. Compatibility Standard에만 있는 기능은 주의
   -webkit-line-clamp                      ← 대안이 없으므로 사용
   -webkit-text-fill-color                 ← 표준화 진행 중
```

### 10.2 피해야 할 패턴

```css
/* [피해야 함] -webkit- 접두사만 사용 */
.bad {
    -webkit-transform: rotate(45deg);
    /* 표준 속성 누락! */
}

/* [올바른 방법] 표준 속성 포함 */
.good {
    -webkit-transform: rotate(45deg);
    transform: rotate(45deg);
}

/* [더 나은 방법] 표준 속성만 사용 */
.better {
    transform: rotate(45deg);
}
```

```javascript
// [피해야 함] -webkit- API로만 기능 감지
if ('webkitRequestAnimationFrame' in window) {
    // ...
}

// [올바른 방법] 표준 API 우선 확인
const raf = window.requestAnimationFrame || window.webkitRequestAnimationFrame;
if (raf) {
    raf(callback);
}
```

### 10.3 Compatibility Standard의 미래

Compatibility Standard는 Living Standard로서 지속적으로 업데이트된다. 새로운 호환성 문제가 발생할 때마다 문서에 추가되며, 더 이상 필요하지 않은 항목은 제거된다.

```
[호환성 표준의 생명주기]

비표준 기능 등장
    ↓
광범위한 웹 사용
    ↓
다른 브라우저에서 호환성 문제 발생
    ↓
Compatibility Standard에 추가          ← 비표준 기능의 표준화
    ↓
모든 브라우저가 표준에 맞게 구현
    ↓
표준 CSS/DOM 속성으로 대체 진행
    ↓
레거시 호환성 유지 (제거하지 않음)       ← 웹은 절대 깨뜨리지 않는다
```

---

## 참고 자료

- [Compatibility Standard (WHATWG)](https://compat.spec.whatwg.org/)
- [Can I Use](https://caniuse.com/)
- [MDN Web Docs - Browser compatibility](https://developer.mozilla.org/en-US/docs/Web/CSS/WebKit_Extensions)
- [Autoprefixer](https://github.com/postcss/autoprefixer)

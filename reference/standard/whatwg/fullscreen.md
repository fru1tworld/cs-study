# Fullscreen API Standard 상세 가이드

## 목차

1. [개요](#1-개요)
2. [전체 화면 진입](#2-전체-화면-진입)
3. [전체 화면 종료](#3-전체-화면-종료)
4. [전체 화면 상태 확인](#4-전체-화면-상태-확인)
5. [이벤트](#5-이벤트)
6. [CSS 의사 클래스와 의사 요소](#6-css-의사-클래스와-의사-요소)
7. [전체 화면 순서와 스택](#7-전체-화면-순서와-스택)
8. [iframe과 전체 화면](#8-iframe과-전체-화면)
9. [보안 고려사항](#9-보안-고려사항)
10. [실용 예제](#10-실용-예제)
11. [브라우저 호환성](#11-브라우저-호환성)
12. [일반적인 문제와 해결 방법](#12-일반적인-문제와-해결-방법)

---

## 1. 개요

### 전체 화면 API란

Fullscreen API는 WHATWG Living Standard로, 웹 페이지의 특정 요소를 전체 화면으로 표시하거나 전체 화면에서 나갈 수 있게 해주는 API이다. 비디오 재생, 게임, 프레젠테이션, 이미지 갤러리 등에서 광범위하게 사용된다.

```
[일반 모드]                        [전체 화면 모드]
┌────────────────────────┐        ┌──────────────────────────────┐
│ 브라우저 탭바           │        │                              │
├────────────────────────┤        │                              │
│ 주소 표시줄             │        │                              │
├────────────────────────┤        │     전체 화면으로 표시된       │
│ ┌──────────────────┐   │        │     요소 콘텐츠               │
│ │                  │   │   →    │                              │
│ │  대상 요소       │   │        │                              │
│ │                  │   │        │                              │
│ └──────────────────┘   │        │                              │
│                        │        │                              │
│ 다른 콘텐츠             │        └──────────────────────────────┘
└────────────────────────┘        (브라우저 UI, 작업 표시줄 등 숨김)
```

### 핵심 구성 요소

| 구성 요소 | 타입 | 설명 |
|-----------|------|------|
| `Element.requestFullscreen()` | 메서드 | 전체 화면 진입 요청 |
| `Document.exitFullscreen()` | 메서드 | 전체 화면 종료 |
| `Document.fullscreenElement` | 속성 | 현재 전체 화면 요소 |
| `Document.fullscreenEnabled` | 속성 | 전체 화면 가능 여부 |
| `fullscreenchange` | 이벤트 | 전체 화면 상태 변경 시 |
| `fullscreenerror` | 이벤트 | 전체 화면 진입 실패 시 |
| `:fullscreen` | CSS 의사 클래스 | 전체 화면 요소 스타일링 |
| `::backdrop` | CSS 의사 요소 | 전체 화면 배경 스타일링 |

---

## 2. 전체 화면 진입

### 2.1 Element.requestFullscreen()

```javascript
// 기본 사용법
const element = document.getElementById('myElement');

element.requestFullscreen()
    .then(() => {
        console.log('전체 화면으로 전환되었습니다.');
    })
    .catch((error) => {
        console.error('전체 화면 전환 실패:', error);
    });
```

### 2.2 FullscreenOptions

`requestFullscreen()`은 선택적으로 옵션 객체를 받는다.

```typescript
interface FullscreenOptions {
    navigationUI?: 'auto' | 'show' | 'hide';
}
```

```javascript
// navigationUI 옵션
const element = document.getElementById('content');

// 'auto': 브라우저가 결정 (기본값)
element.requestFullscreen({ navigationUI: 'auto' });

// 'show': 네비게이션 UI(주소 표시줄 등) 표시
element.requestFullscreen({ navigationUI: 'show' });

// 'hide': 네비게이션 UI 숨김
element.requestFullscreen({ navigationUI: 'hide' });
```

```
navigationUI 옵션 비교:

[navigationUI: 'show']            [navigationUI: 'hide']
┌────────────────────────────┐   ┌────────────────────────────┐
│ ◀ ▶ │ https://example.com │   │                            │
├────────────────────────────┤   │                            │
│                            │   │                            │
│    전체 화면 콘텐츠         │   │    전체 화면 콘텐츠         │
│                            │   │                            │
│                            │   │                            │
│                            │   │                            │
└────────────────────────────┘   └────────────────────────────┘
(주소 표시줄 표시됨)              (완전한 전체 화면)
```

### 2.3 requestFullscreen()의 반환값

```javascript
// requestFullscreen()은 Promise를 반환
const promise = element.requestFullscreen();

// Promise가 fulfill 되는 시점:
// - fullscreenchange 이벤트가 발생한 후

// Promise가 reject 되는 경우:
// - 이미 전체 화면 상태가 아닌 경우 (다른 문서가 전체 화면)
// - 요소가 문서에 연결되어 있지 않은 경우
// - 요소가 전체 화면을 허용하지 않는 경우
// - 사용자 활성화가 없는 경우
// - 요소의 문서가 전체 화면을 허용하지 않는 경우

// async/await 패턴
async function enterFullscreen(element) {
    try {
        await element.requestFullscreen();
        console.log('전체 화면 진입 성공');
    } catch (error) {
        switch (error.name) {
            case 'TypeError':
                console.error('전체 화면이 지원되지 않는 요소입니다.');
                break;
            default:
                console.error('전체 화면 진입 실패:', error.message);
        }
    }
}
```

### 2.4 어떤 요소를 전체 화면으로 만들 수 있는가

```javascript
// 거의 모든 HTML 요소를 전체 화면으로 만들 수 있음

// 비디오 요소
document.querySelector('video').requestFullscreen();

// div 요소 (게임 캔버스 컨테이너 등)
document.querySelector('.game-container').requestFullscreen();

// canvas 요소
document.querySelector('canvas').requestFullscreen();

// 전체 문서
document.documentElement.requestFullscreen();

// iframe 요소 (allowfullscreen 속성 필요)
document.querySelector('iframe').requestFullscreen();

// SVG 요소
document.querySelector('svg').requestFullscreen();

// dialog 요소
document.querySelector('dialog').requestFullscreen();
```

---

## 3. 전체 화면 종료

### 3.1 Document.exitFullscreen()

```javascript
// 전체 화면 종료
document.exitFullscreen()
    .then(() => {
        console.log('전체 화면이 종료되었습니다.');
    })
    .catch((error) => {
        console.error('전체 화면 종료 실패:', error);
        // 이미 전체 화면이 아닌 경우 등
    });

// async/await
async function exitFullscreen() {
    try {
        await document.exitFullscreen();
        console.log('전체 화면 종료');
    } catch (error) {
        console.error('종료 실패:', error);
    }
}
```

### 3.2 전체 화면 종료 방법들

```
전체 화면 종료 방법:

1. JavaScript API:
   document.exitFullscreen()

2. 사용자 키보드:
   - Esc 키 (대부분의 브라우저)
   - F11 키 (브라우저 전체 화면과 혼동 주의)

3. 브라우저 UI:
   - "전체 화면 종료" 버튼/메시지
   - 마우스를 화면 상단으로 이동하면 나타나는 UI

4. 시스템:
   - Alt+Tab (다른 윈도우로 전환 시)
   - 시스템 알림 등으로 인한 포커스 변경
```

### 3.3 전체 화면 토글 패턴

```javascript
// 전체 화면 토글 유틸리티
function toggleFullscreen(element) {
    if (document.fullscreenElement) {
        // 현재 전체 화면 → 종료
        return document.exitFullscreen();
    } else {
        // 전체 화면이 아님 → 진입
        return (element || document.documentElement).requestFullscreen();
    }
}

// 사용
document.getElementById('toggleBtn').addEventListener('click', () => {
    toggleFullscreen(document.getElementById('content'));
});

// 키보드 단축키와 조합
document.addEventListener('keydown', (event) => {
    if (event.key === 'f' || event.key === 'F') {
        // 'F' 키로 전체 화면 토글
        toggleFullscreen(document.getElementById('player'));
    }
});
```

---

## 4. 전체 화면 상태 확인

### 4.1 Document.fullscreenElement

```javascript
// 현재 전체 화면 요소 확인
const currentFullscreenElement = document.fullscreenElement;

if (currentFullscreenElement) {
    console.log('전체 화면 요소:', currentFullscreenElement.tagName);
    console.log('요소 ID:', currentFullscreenElement.id);
} else {
    console.log('전체 화면이 아닙니다.');
}

// 특정 요소가 전체 화면인지 확인
function isElementFullscreen(element) {
    return document.fullscreenElement === element;
}

// Shadow DOM에서의 fullscreenElement
// Shadow DOM 내부의 요소가 전체 화면인 경우,
// document.fullscreenElement는 Shadow Host를 반환
// ShadowRoot.fullscreenElement로 실제 요소를 얻을 수 있음
```

### 4.2 Document.fullscreenEnabled

```javascript
// 전체 화면 사용 가능 여부 확인
if (document.fullscreenEnabled) {
    console.log('전체 화면을 사용할 수 있습니다.');
    showFullscreenButton();
} else {
    console.log('전체 화면을 사용할 수 없습니다.');
    hideFullscreenButton();
}

// fullscreenEnabled가 false인 경우:
// - 브라우저가 Fullscreen API를 지원하지 않음
// - iframe에 allowfullscreen 속성이 없음
// - 문서의 특성 정책(Feature Policy)이 전체 화면을 허용하지 않음
// - 브라우저 설정에서 전체 화면이 비활성화됨
```

### 4.3 전체 화면 상태 감지 유틸리티

```javascript
// 크로스 브라우저 전체 화면 상태 확인
const FullscreenAPI = {
    get element() {
        return document.fullscreenElement ||
               document.webkitFullscreenElement ||    // Safari
               document.mozFullScreenElement ||        // Firefox (구버전)
               document.msFullscreenElement;           // IE/Edge (구버전)
    },

    get enabled() {
        return document.fullscreenEnabled ||
               document.webkitFullscreenEnabled ||
               document.mozFullScreenEnabled ||
               document.msFullscreenEnabled;
    },

    get isFullscreen() {
        return !!this.element;
    },

    request(element) {
        const el = element || document.documentElement;
        const fn = el.requestFullscreen ||
                   el.webkitRequestFullscreen ||
                   el.mozRequestFullScreen ||
                   el.msRequestFullscreen;

        if (fn) {
            return fn.call(el);
        }
        return Promise.reject(new Error('Fullscreen API not supported'));
    },

    exit() {
        const fn = document.exitFullscreen ||
                   document.webkitExitFullscreen ||
                   document.mozCancelFullScreen ||
                   document.msExitFullscreen;

        if (fn) {
            return fn.call(document);
        }
        return Promise.reject(new Error('Fullscreen API not supported'));
    },

    toggle(element) {
        if (this.isFullscreen) {
            return this.exit();
        }
        return this.request(element);
    },

    onChange(callback) {
        document.addEventListener('fullscreenchange', callback);
        document.addEventListener('webkitfullscreenchange', callback);
        document.addEventListener('mozfullscreenchange', callback);
        document.addEventListener('MSFullscreenChange', callback);
    },

    onError(callback) {
        document.addEventListener('fullscreenerror', callback);
        document.addEventListener('webkitfullscreenerror', callback);
        document.addEventListener('mozfullscreenerror', callback);
        document.addEventListener('MSFullscreenError', callback);
    }
};
```

---

## 5. 이벤트

### 5.1 fullscreenchange

```javascript
// Document 레벨 이벤트
document.addEventListener('fullscreenchange', function(event) {
    if (document.fullscreenElement) {
        console.log('전체 화면 진입:', document.fullscreenElement);
    } else {
        console.log('전체 화면 종료');
    }
});

// Element 레벨 이벤트
const videoContainer = document.getElementById('videoContainer');
videoContainer.addEventListener('fullscreenchange', function(event) {
    console.log('이 요소의 전체 화면 상태 변경');
    console.log('현재 전체 화면:', document.fullscreenElement === videoContainer);
});
```

### 5.2 fullscreenerror

```javascript
// 전체 화면 진입 실패 시
document.addEventListener('fullscreenerror', function(event) {
    console.error('전체 화면 진입 실패');
    console.error('대상 요소:', event.target);

    // 사용자에게 에러 메시지 표시
    showErrorMessage('전체 화면으로 전환할 수 없습니다.');
});

// Element 레벨
const element = document.getElementById('content');
element.addEventListener('fullscreenerror', function(event) {
    console.error('이 요소를 전체 화면으로 만들 수 없습니다.');
});
```

### 5.3 이벤트 버블링

```javascript
// fullscreenchange와 fullscreenerror는 버블링됨

// DOM 구조:
// <div id="outer">
//   <div id="inner">
//     <video id="video"></video>
//   </div>
// </div>

// video를 전체 화면으로 만들면:
// 1. video에서 fullscreenchange 발생
// 2. inner로 버블링
// 3. outer로 버블링
// 4. document로 버블링

document.getElementById('video').addEventListener('fullscreenchange', (e) => {
    console.log('video에서 이벤트 발생');
});

document.getElementById('outer').addEventListener('fullscreenchange', (e) => {
    console.log('outer에서 이벤트 수신 (버블링)');
    console.log('실제 대상:', e.target);  // video 요소
});

document.addEventListener('fullscreenchange', (e) => {
    console.log('document에서 이벤트 수신 (버블링)');
});
```

### 5.4 이벤트를 활용한 UI 업데이트

```javascript
// 전체 화면 상태에 따른 UI 업데이트
class FullscreenUI {
    constructor(container, toggleButton) {
        this.container = container;
        this.toggleButton = toggleButton;

        this.setupEvents();
        this.updateUI();
    }

    setupEvents() {
        // 전체 화면 상태 변경 감지
        document.addEventListener('fullscreenchange', () => {
            this.updateUI();
        });

        // 전체 화면 오류 감지
        document.addEventListener('fullscreenerror', () => {
            this.showError();
        });

        // 토글 버튼 클릭
        this.toggleButton.addEventListener('click', () => {
            this.toggle();
        });

        // 키보드 단축키
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && document.fullscreenElement) {
                // Esc는 브라우저가 자동 처리하지만,
                // 추가 정리 작업이 필요한 경우
            }
        });
    }

    updateUI() {
        const isFullscreen = !!document.fullscreenElement;

        // 버튼 텍스트 변경
        this.toggleButton.textContent = isFullscreen
            ? '전체 화면 종료'
            : '전체 화면';

        // 버튼 아이콘 변경
        this.toggleButton.setAttribute('aria-label',
            isFullscreen ? '전체 화면 종료' : '전체 화면으로 보기'
        );

        // 컨테이너 클래스 토글
        this.container.classList.toggle('is-fullscreen', isFullscreen);

        // 추가 UI 조정
        if (isFullscreen) {
            this.showControlsOverlay();
        } else {
            this.hideControlsOverlay();
        }
    }

    async toggle() {
        try {
            if (document.fullscreenElement) {
                await document.exitFullscreen();
            } else {
                await this.container.requestFullscreen();
            }
        } catch (error) {
            this.showError();
        }
    }

    showError() {
        console.error('전체 화면 오류');
    }

    showControlsOverlay() {
        // 전체 화면에서의 컨트롤 오버레이 표시
    }

    hideControlsOverlay() {
        // 컨트롤 오버레이 숨기기
    }
}
```

---

## 6. CSS 의사 클래스와 의사 요소

### 6.1 :fullscreen 의사 클래스

`:fullscreen` 의사 클래스는 현재 전체 화면 모드인 요소에 스타일을 적용한다.

```css
/* 전체 화면일 때의 스타일 */
:fullscreen {
    /* 전체 화면 요소에 적용 */
}

/* 특정 요소가 전체 화면일 때 */
.video-container:fullscreen {
    /* 전체 화면 모드의 비디오 컨테이너 스타일 */
    background-color: #000;
}

/* 전체 화면일 때 비디오를 화면에 맞추기 */
.video-container:fullscreen video {
    width: 100%;
    height: 100%;
    object-fit: contain;
}

/* 전체 화면일 때 컨트롤 위치 조정 */
.video-container:fullscreen .controls {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    z-index: 2147483647; /* 최상위 z-index */
    background: linear-gradient(transparent, rgba(0, 0, 0, 0.7));
    padding: 20px;
}

/* 전체 화면이 아닐 때 */
.video-container:not(:fullscreen) {
    max-width: 800px;
    margin: 0 auto;
}

/* 크로스 브라우저 지원 */
:-webkit-full-screen {
    /* WebKit/Blink (구버전) */
}

:-moz-full-screen {
    /* Firefox (구버전) */
}

:-ms-fullscreen {
    /* IE/Edge (구버전) */
}

:fullscreen {
    /* 표준 */
}
```

### 6.2 ::backdrop 의사 요소

`::backdrop`은 전체 화면 요소 뒤에 표시되는 배경 레이어를 스타일링한다.

```css
/* 전체 화면 배경 스타일 */
:fullscreen::backdrop {
    background-color: #000;  /* 기본: 검은색 */
}

/* 커스텀 배경 */
.presentation:fullscreen::backdrop {
    background: linear-gradient(135deg, #1a1a2e, #16213e);
}

/* 반투명 배경 */
.modal-like:fullscreen::backdrop {
    background-color: rgba(0, 0, 0, 0.85);
    backdrop-filter: blur(10px);
}

/* 애니메이션이 있는 배경 */
.fancy:fullscreen::backdrop {
    background: linear-gradient(
        45deg,
        #1a1a2e,
        #16213e,
        #0f3460,
        #533483
    );
    background-size: 400% 400%;
    animation: gradientShift 15s ease infinite;
}

@keyframes gradientShift {
    0% { background-position: 0% 50%; }
    50% { background-position: 100% 50%; }
    100% { background-position: 0% 50%; }
}
```

### 6.3 전체 화면에서의 레이아웃 스타일링

```css
/* 전체 화면 모드에서의 완전한 레이아웃 */

/* 기본 상태 */
.player {
    position: relative;
    width: 100%;
    max-width: 800px;
    aspect-ratio: 16 / 9;
    background: #000;
    border-radius: 8px;
    overflow: hidden;
}

.player video {
    width: 100%;
    height: 100%;
    object-fit: contain;
}

.player .controls {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 10px;
    background: linear-gradient(transparent, rgba(0, 0, 0, 0.7));
    display: flex;
    align-items: center;
    gap: 10px;
    opacity: 0;
    transition: opacity 0.3s;
}

.player:hover .controls {
    opacity: 1;
}

/* 전체 화면 상태 */
.player:fullscreen {
    max-width: none;
    border-radius: 0;
}

.player:fullscreen video {
    width: 100vw;
    height: 100vh;
}

.player:fullscreen .controls {
    position: fixed;
    padding: 20px 30px;
    font-size: 1.2em;
}

.player:fullscreen::backdrop {
    background: #000;
}

/* 전체 화면에서 마우스 숨기기 */
.player:fullscreen.hide-cursor {
    cursor: none;
}

.player:fullscreen.hide-cursor .controls {
    opacity: 0;
    pointer-events: none;
}
```

---

## 7. 전체 화면 순서와 스택

### 7.1 전체 화면 요소 스택

Fullscreen API는 전체 화면 요소의 스택(stack)을 관리한다. 하나의 문서에서 여러 요소가 순차적으로 전체 화면을 요청할 수 있다.

```
[전체 화면 스택 예시]

초기 상태: 스택 비어있음
    []

1단계: <div id="outer">가 전체 화면 요청
    [<div#outer>]
    → document.fullscreenElement = <div#outer>

2단계: <div#outer> 내부의 <video>가 전체 화면 요청
    [<div#outer>, <video>]
    → document.fullscreenElement = <video>

3단계: exitFullscreen() 호출
    [<div#outer>]
    → document.fullscreenElement = <div#outer>
    → <video>는 더 이상 전체 화면이 아님

4단계: exitFullscreen() 다시 호출
    []
    → document.fullscreenElement = null
    → 전체 화면 모드 종료
```

### 7.2 스택 동작 규칙

```javascript
// 전체 화면 스택 동작 시연

async function demonstrateStack() {
    const outer = document.getElementById('outer');
    const inner = document.getElementById('inner');

    // 1. outer를 전체 화면으로
    await outer.requestFullscreen();
    console.log('스택:', document.fullscreenElement.id); // "outer"

    // 2. inner를 전체 화면으로 (outer 위에 추가)
    await inner.requestFullscreen();
    console.log('스택:', document.fullscreenElement.id); // "inner"

    // 3. 전체 화면 종료 (inner가 스택에서 제거)
    await document.exitFullscreen();
    console.log('스택:', document.fullscreenElement?.id); // "outer"

    // 4. 다시 전체 화면 종료 (outer가 스택에서 제거)
    await document.exitFullscreen();
    console.log('스택:', document.fullscreenElement); // null
}
```

### 7.3 중요한 제약: 조상 관계

```javascript
// 전체 화면 스택에 추가할 수 있는 요소는
// 현재 전체 화면 요소의 자손(descendant)이어야 함

// DOM 구조:
// <div id="A">
//   <div id="B">
//     <div id="C"></div>
//   </div>
// </div>
// <div id="D"></div>

// A가 전체 화면인 상태에서:
await A.requestFullscreen();  // 성공

// B를 전체 화면으로 (A의 자손 → 가능)
await B.requestFullscreen();  // 성공

// D를 전체 화면으로 (A의 자손이 아님 → 에러)
// → 먼저 전체 화면을 종료하고 D를 요청해야 함
await D.requestFullscreen();  // fullscreenerror 발생
```

---

## 8. iframe과 전체 화면

### 8.1 allowfullscreen 속성

`<iframe>` 내부의 콘텐츠가 전체 화면을 사용하려면, iframe에 `allowfullscreen` 속성이 필요하다.

```html
<!-- 전체 화면 허용 -->
<iframe
    src="https://example.com/video"
    allowfullscreen
></iframe>

<!-- 전체 화면 차단 (기본) -->
<iframe src="https://example.com/video"></iframe>

<!-- allow 속성으로 더 세밀한 제어 -->
<iframe
    src="https://example.com/video"
    allow="fullscreen"
></iframe>

<!-- 특정 출처에만 전체 화면 허용 -->
<iframe
    src="https://player.example.com/video"
    allow="fullscreen https://player.example.com"
></iframe>
```

### 8.2 중첩 iframe에서의 전체 화면

```html
<!-- 상위 페이지 -->
<iframe id="level1" allowfullscreen src="frame1.html"></iframe>

<!-- frame1.html -->
<iframe id="level2" allowfullscreen src="frame2.html"></iframe>

<!-- frame2.html -->
<button onclick="document.body.requestFullscreen()">전체 화면</button>

<!--
전체 화면이 동작하려면 모든 상위 iframe에 allowfullscreen이 필요:
상위 페이지 → iframe#level1 (allowfullscreen) → iframe#level2 (allowfullscreen)
하나라도 빠지면 전체 화면 불가
-->
```

### 8.3 Feature Policy와 전체 화면

```html
<!-- Feature Policy (Permissions Policy)로 전체 화면 제어 -->

<!-- HTTP 헤더로 설정 -->
<!-- Permissions-Policy: fullscreen=(self "https://player.example.com") -->

<!-- HTML 속성으로 설정 -->
<iframe
    src="https://player.example.com"
    allow="fullscreen"
></iframe>

<!-- 전체 화면 완전 차단 -->
<iframe
    src="https://untrusted.example.com"
    allow="fullscreen 'none'"
></iframe>
```

### 8.4 iframe 전체 화면 감지

```javascript
// 부모 페이지에서 iframe의 전체 화면 상태 감지
const iframe = document.querySelector('iframe');

document.addEventListener('fullscreenchange', () => {
    if (document.fullscreenElement === iframe) {
        console.log('iframe이 전체 화면입니다.');
    }
});

// iframe 내부에서 전체 화면 상태 감지
// (iframe 내부의 JavaScript)
document.addEventListener('fullscreenchange', () => {
    if (document.fullscreenElement) {
        console.log('전체 화면 진입 (iframe 내부)');
    } else {
        console.log('전체 화면 종료');
    }
});
```

---

## 9. 보안 고려사항

### 9.1 사용자 활성화 요구

```javascript
// requestFullscreen()은 사용자 활성화(user activation)가 필요

// [작동하지 않음] 스크립트에서 직접 호출
window.addEventListener('load', () => {
    document.body.requestFullscreen(); // 거부됨!
});

// [작동하지 않음] setTimeout 내부에서 호출
setTimeout(() => {
    document.body.requestFullscreen(); // 거부됨!
}, 1000);

// [작동함] 사용자 이벤트 핸들러 내에서 호출
button.addEventListener('click', () => {
    document.body.requestFullscreen(); // 허용됨
});

// [작동함] 키보드 이벤트 핸들러 내에서 호출
document.addEventListener('keydown', (e) => {
    if (e.key === 'f') {
        document.body.requestFullscreen(); // 허용됨
    }
});

// [주의] 일부 이벤트는 사용자 활성화로 간주되지 않을 수 있음
// mousemove, scroll 등은 보통 사용자 활성화가 아님
```

### 9.2 전체 화면에서의 보안 위험

```
[보안 위험: 전체 화면 스푸핑]

공격 시나리오:
1. 공격자가 전체 화면으로 전환
2. 가짜 브라우저 UI(주소 표시줄)를 표시
3. 사용자가 합법적인 사이트라고 착각
4. 민감한 정보를 입력

대응:
- 브라우저는 전체 화면 진입 시 "전체 화면입니다" 경고 메시지 표시
- Esc 키로 항상 전체 화면을 종료할 수 있음
- 일부 브라우저는 키보드 입력 시 추가 경고 표시
```

### 9.3 키보드 입력 제한

```javascript
// 전체 화면에서 키보드 제한 (보안상의 이유)
// 일부 브라우저는 전체 화면에서 특정 키 입력 시 경고를 표시

// 게임이나 앱에서 키보드 입력을 받아야 하는 경우:
// Keyboard Lock API를 사용할 수 있음

document.addEventListener('keydown', (event) => {
    if (document.fullscreenElement) {
        // 전체 화면에서의 키 입력 처리
        // Esc 키는 전체 화면 종료에 예약되어 있음

        if (event.key === 'Escape') {
            // 이 이벤트는 발생하지만,
            // 브라우저가 전체 화면을 종료함
            console.log('Esc 키 - 전체 화면 종료');
        }
    }
});

// Keyboard Lock API (Fullscreen과 함께 사용)
async function enterFullscreenWithKeyboardLock(element) {
    await element.requestFullscreen();

    if ('keyboard' in navigator && 'lock' in navigator.keyboard) {
        // 특정 키를 잠금 (Esc 등)
        await navigator.keyboard.lock(['Escape']);
        console.log('키보드 잠금 활성화');
    }
}
```

### 9.4 Permissions Policy

```html
<!-- Permissions Policy로 전체 화면 기능 제어 -->

<!-- 자신의 출처에서만 전체 화면 허용 -->
<meta http-equiv="Permissions-Policy" content="fullscreen=(self)">

<!-- 특정 출처 허용 -->
<!-- Permissions-Policy: fullscreen=(self "https://trusted.example.com") -->

<!-- 모든 출처 허용 -->
<!-- Permissions-Policy: fullscreen=* -->

<!-- 전체 화면 완전 비활성화 -->
<!-- Permissions-Policy: fullscreen=() -->
```

---

## 10. 실용 예제

### 10.1 비디오 플레이어

```html
<div class="video-player" id="videoPlayer">
    <video id="video" src="movie.mp4"></video>
    <div class="video-controls">
        <button id="playPauseBtn" aria-label="재생/일시정지">▶</button>
        <input type="range" id="progress" min="0" max="100" value="0">
        <span id="timeDisplay">0:00 / 0:00</span>
        <button id="muteBtn" aria-label="음소거">🔊</button>
        <input type="range" id="volume" min="0" max="100" value="100">
        <button id="fullscreenBtn" aria-label="전체 화면">⛶</button>
    </div>
</div>
```

```css
.video-player {
    position: relative;
    max-width: 800px;
    background: #000;
    border-radius: 8px;
    overflow: hidden;
}

.video-player video {
    width: 100%;
    display: block;
}

.video-controls {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 15px;
    background: linear-gradient(transparent, rgba(0, 0, 0, 0.8));
    transition: opacity 0.3s;
}

/* 전체 화면 스타일 */
.video-player:fullscreen {
    max-width: none;
    border-radius: 0;
}

.video-player:fullscreen video {
    width: 100vw;
    height: 100vh;
    object-fit: contain;
}

.video-player:fullscreen .video-controls {
    padding: 20px 30px;
}

.video-player:fullscreen::backdrop {
    background: #000;
}

/* 마우스 비활동 시 컨트롤과 커서 숨기기 */
.video-player:fullscreen.idle {
    cursor: none;
}

.video-player:fullscreen.idle .video-controls {
    opacity: 0;
    pointer-events: none;
}
```

```javascript
class VideoPlayer {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.video = this.container.querySelector('video');
        this.fullscreenBtn = this.container.querySelector('#fullscreenBtn');
        this.idleTimer = null;

        this.setupFullscreen();
        this.setupIdleDetection();
    }

    setupFullscreen() {
        // 전체 화면 버튼
        this.fullscreenBtn.addEventListener('click', () => {
            this.toggleFullscreen();
        });

        // 비디오 더블 클릭으로 전체 화면 토글
        this.video.addEventListener('dblclick', () => {
            this.toggleFullscreen();
        });

        // 전체 화면 상태 변경 감지
        document.addEventListener('fullscreenchange', () => {
            this.onFullscreenChange();
        });

        // 전체 화면 불가 시 버튼 숨기기
        if (!document.fullscreenEnabled) {
            this.fullscreenBtn.style.display = 'none';
        }
    }

    async toggleFullscreen() {
        try {
            if (document.fullscreenElement === this.container) {
                await document.exitFullscreen();
            } else {
                await this.container.requestFullscreen();
            }
        } catch (error) {
            console.error('전체 화면 전환 실패:', error);
        }
    }

    onFullscreenChange() {
        const isFullscreen = document.fullscreenElement === this.container;
        this.fullscreenBtn.textContent = isFullscreen ? '⛶' : '⛶';
        this.fullscreenBtn.setAttribute('aria-label',
            isFullscreen ? '전체 화면 종료' : '전체 화면'
        );
    }

    setupIdleDetection() {
        const resetIdle = () => {
            this.container.classList.remove('idle');
            clearTimeout(this.idleTimer);

            if (document.fullscreenElement === this.container) {
                this.idleTimer = setTimeout(() => {
                    this.container.classList.add('idle');
                }, 3000); // 3초 비활동 후 숨김
            }
        };

        this.container.addEventListener('mousemove', resetIdle);
        this.container.addEventListener('mousedown', resetIdle);
        this.container.addEventListener('keydown', resetIdle);
    }
}

const player = new VideoPlayer('videoPlayer');
```

### 10.2 프레젠테이션 모드

```html
<div id="presentation" class="presentation">
    <div class="slide active" data-slide="1">
        <h1>웹 개발의 미래</h1>
        <p>WHATWG Living Standards</p>
    </div>
    <div class="slide" data-slide="2">
        <h2>Fullscreen API</h2>
        <ul>
            <li>전체 화면 진입/종료</li>
            <li>이벤트 처리</li>
            <li>CSS 스타일링</li>
        </ul>
    </div>
    <div class="slide" data-slide="3">
        <h2>감사합니다</h2>
        <p>Q&A</p>
    </div>
    <div class="presentation-controls">
        <button id="prevSlide">이전</button>
        <span id="slideNumber">1 / 3</span>
        <button id="nextSlide">다음</button>
        <button id="presentBtn">프레젠테이션 시작</button>
    </div>
</div>
```

```javascript
class Presentation {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.slides = this.container.querySelectorAll('.slide');
        this.currentSlide = 0;
        this.totalSlides = this.slides.length;

        this.setupControls();
        this.setupKeyboard();
        this.setupFullscreen();
    }

    setupControls() {
        document.getElementById('prevSlide').addEventListener('click', () => this.prevSlide());
        document.getElementById('nextSlide').addEventListener('click', () => this.nextSlide());
        document.getElementById('presentBtn').addEventListener('click', () => this.startPresentation());
    }

    setupKeyboard() {
        document.addEventListener('keydown', (e) => {
            if (document.fullscreenElement !== this.container) return;

            switch (e.key) {
                case 'ArrowRight':
                case 'ArrowDown':
                case ' ':
                case 'PageDown':
                    e.preventDefault();
                    this.nextSlide();
                    break;
                case 'ArrowLeft':
                case 'ArrowUp':
                case 'PageUp':
                    e.preventDefault();
                    this.prevSlide();
                    break;
                case 'Home':
                    e.preventDefault();
                    this.goToSlide(0);
                    break;
                case 'End':
                    e.preventDefault();
                    this.goToSlide(this.totalSlides - 1);
                    break;
            }
        });
    }

    setupFullscreen() {
        document.addEventListener('fullscreenchange', () => {
            if (!document.fullscreenElement) {
                // 프레젠테이션 종료
                document.getElementById('presentBtn').textContent = '프레젠테이션 시작';
            }
        });
    }

    async startPresentation() {
        try {
            await this.container.requestFullscreen({ navigationUI: 'hide' });
            document.getElementById('presentBtn').textContent = '프레젠테이션 종료';
        } catch (error) {
            console.error('프레젠테이션 시작 실패:', error);
        }
    }

    goToSlide(index) {
        if (index < 0 || index >= this.totalSlides) return;

        this.slides[this.currentSlide].classList.remove('active');
        this.currentSlide = index;
        this.slides[this.currentSlide].classList.add('active');

        document.getElementById('slideNumber').textContent =
            `${this.currentSlide + 1} / ${this.totalSlides}`;
    }

    nextSlide() {
        this.goToSlide(this.currentSlide + 1);
    }

    prevSlide() {
        this.goToSlide(this.currentSlide - 1);
    }
}

const presentation = new Presentation('presentation');
```

### 10.3 게임 전체 화면

```javascript
class GameFullscreen {
    constructor(canvasId) {
        this.canvas = document.getElementById(canvasId);
        this.container = this.canvas.parentElement;

        this.setupFullscreen();
        this.setupPointerLock();
    }

    setupFullscreen() {
        document.addEventListener('fullscreenchange', () => {
            if (document.fullscreenElement === this.container) {
                this.onEnterFullscreen();
            } else {
                this.onExitFullscreen();
            }
        });
    }

    async enterFullscreen() {
        try {
            await this.container.requestFullscreen({ navigationUI: 'hide' });
        } catch (error) {
            console.error('전체 화면 실패:', error);
        }
    }

    onEnterFullscreen() {
        // 캔버스 크기를 화면에 맞추기
        this.canvas.width = screen.width;
        this.canvas.height = screen.height;

        // Pointer Lock 요청 (마우스 커서 숨기고 마우스 캡처)
        this.canvas.requestPointerLock();

        // Keyboard Lock 요청 (Esc 등 시스템 키 캡처)
        if ('keyboard' in navigator && 'lock' in navigator.keyboard) {
            navigator.keyboard.lock(['Escape', 'F11']);
        }

        console.log('게임 전체 화면 모드 활성화');
    }

    onExitFullscreen() {
        // 캔버스 크기 복원
        this.canvas.width = 800;
        this.canvas.height = 600;

        // Pointer Lock 해제
        document.exitPointerLock();

        // Keyboard Lock 해제
        if ('keyboard' in navigator && 'unlock' in navigator.keyboard) {
            navigator.keyboard.unlock();
        }

        console.log('게임 전체 화면 모드 해제');
    }

    setupPointerLock() {
        document.addEventListener('pointerlockchange', () => {
            if (document.pointerLockElement === this.canvas) {
                console.log('마우스 캡처 활성화');
            } else {
                console.log('마우스 캡처 해제');
            }
        });
    }
}

const game = new GameFullscreen('gameCanvas');
```

---

## 11. 브라우저 호환성

### 11.1 지원 현황

| 기능 | Chrome | Firefox | Safari | Edge |
|------|--------|---------|--------|------|
| `requestFullscreen()` | 71+ | 64+ | 16.4+ | 79+ |
| `exitFullscreen()` | 71+ | 64+ | 16.4+ | 79+ |
| `fullscreenElement` | 71+ | 64+ | 16.4+ | 79+ |
| `fullscreenEnabled` | 71+ | 64+ | 16.4+ | 79+ |
| `fullscreenchange` | 71+ | 64+ | 16.4+ | 79+ |
| `fullscreenerror` | 71+ | 64+ | 16.4+ | 79+ |
| `:fullscreen` | 71+ | 64+ | 16.4+ | 79+ |
| `::backdrop` | 37+ | 47+ | 15.4+ | 79+ |
| `FullscreenOptions` | 71+ | 64+ | 16.4+ | 79+ |
| `allowfullscreen` (iframe) | 27+ | 9+ | 7+ | 12+ |

### 11.2 접두사 버전 (레거시)

```javascript
// 구형 브라우저를 위한 접두사 버전

// requestFullscreen
element.requestFullscreen();           // 표준
element.webkitRequestFullscreen();     // Chrome, Safari (구버전)
element.mozRequestFullScreen();        // Firefox (구버전, 대문자 S 주의)
element.msRequestFullscreen();         // IE11, Edge (구버전)

// exitFullscreen
document.exitFullscreen();             // 표준
document.webkitExitFullscreen();       // Chrome, Safari (구버전)
document.mozCancelFullScreen();        // Firefox (구버전, Cancel + 대문자 S)
document.msExitFullscreen();           // IE11, Edge (구버전)

// fullscreenElement
document.fullscreenElement;            // 표준
document.webkitFullscreenElement;      // Chrome, Safari (구버전)
document.mozFullScreenElement;         // Firefox (구버전)
document.msFullscreenElement;          // IE11, Edge (구버전)

// fullscreenEnabled
document.fullscreenEnabled;            // 표준
document.webkitFullscreenEnabled;      // Chrome, Safari (구버전)
document.mozFullScreenEnabled;         // Firefox (구버전)
document.msFullscreenEnabled;          // IE11, Edge (구버전)
```

---

## 12. 일반적인 문제와 해결 방법

### 12.1 전체 화면이 작동하지 않는 경우

```javascript
// 문제 진단 체크리스트
function diagnoseFullscreenIssue(element) {
    const issues = [];

    // 1. API 지원 확인
    if (!document.fullscreenEnabled) {
        issues.push('Fullscreen API가 지원되지 않거나 비활성화되어 있습니다.');
    }

    // 2. 요소가 문서에 연결되어 있는지 확인
    if (!element.isConnected) {
        issues.push('요소가 DOM에 연결되어 있지 않습니다.');
    }

    // 3. iframe 내부인 경우
    if (window !== window.top) {
        issues.push('iframe 내부입니다. allowfullscreen 속성을 확인하세요.');
    }

    // 4. 사용자 활성화 확인 (간접적)
    issues.push('사용자 클릭/터치 이벤트 핸들러 내에서 호출되고 있는지 확인하세요.');

    return issues;
}
```

### 12.2 전체 화면에서의 스크롤 문제

```css
/* 전체 화면에서 스크롤이 필요한 경우 */
.scrollable-content:fullscreen {
    overflow-y: auto;
    -webkit-overflow-scrolling: touch; /* iOS */
}

/* 전체 화면에서 스크롤 비활성화 */
body:has(:fullscreen) {
    overflow: hidden;
}
```

### 12.3 접근성 고려사항

```javascript
// 접근성을 고려한 전체 화면 구현
function setupAccessibleFullscreen(element, button) {
    // ARIA 속성 설정
    button.setAttribute('role', 'button');
    button.setAttribute('aria-pressed', 'false');
    button.setAttribute('aria-label', '전체 화면으로 보기');

    document.addEventListener('fullscreenchange', () => {
        const isFullscreen = document.fullscreenElement === element;
        button.setAttribute('aria-pressed', String(isFullscreen));
        button.setAttribute('aria-label',
            isFullscreen ? '전체 화면 종료' : '전체 화면으로 보기'
        );

        // 스크린 리더 알림
        const announcement = document.createElement('div');
        announcement.setAttribute('role', 'status');
        announcement.setAttribute('aria-live', 'polite');
        announcement.textContent = isFullscreen
            ? '전체 화면 모드가 활성화되었습니다. Esc 키로 종료할 수 있습니다.'
            : '전체 화면 모드가 종료되었습니다.';
        document.body.appendChild(announcement);

        setTimeout(() => announcement.remove(), 3000);
    });
}
```

---

## 참고 자료

- [Fullscreen API Standard (WHATWG)](https://fullscreen.spec.whatwg.org/)
- [MDN - Fullscreen API](https://developer.mozilla.org/ko/docs/Web/API/Fullscreen_API)
- [MDN - :fullscreen](https://developer.mozilla.org/ko/docs/Web/CSS/:fullscreen)
- [MDN - ::backdrop](https://developer.mozilla.org/ko/docs/Web/CSS/::backdrop)
- [Can I Use - Fullscreen API](https://caniuse.com/fullscreen)

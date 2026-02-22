# DOM Living Standard - 이벤트 시스템 및 고급 기능 (Part 2)

> 이 문서는 [DOM Living Standard 기본](dom.md)의 후속 문서입니다.

---

## 목차

1. [이벤트 시스템 개요](#1-이벤트-시스템-개요)
2. [EventTarget](#2-eventtarget)
3. [Event 인터페이스](#3-event-인터페이스)
4. [이벤트 전파](#4-이벤트-전파)
5. [CustomEvent](#5-customevent)
6. [주요 이벤트 타입](#6-주요-이벤트-타입)
7. [MutationObserver](#7-mutationobserver)
8. [Range와 Selection](#8-range와-selection)
9. [NodeIterator와 TreeWalker](#9-nodeiterator와-treewalker)
10. [AbortController와 AbortSignal](#10-abortcontroller와-abortsignal)
11. [Shadow DOM](#11-shadow-dom)
12. [Mutation Algorithms](#12-mutation-algorithms)

---

## 1. 이벤트 시스템 개요

### DOM 이벤트 모델의 역사

웹 초창기에는 표준화된 이벤트 모델이 없었다. Netscape는 이벤트 캡처링(Event Capturing) 모델을, Internet Explorer는 이벤트 버블링(Event Bubbling) 모델을 각각 독자적으로 구현했다.

| 시기 | 모델 | 특징 |
|------|------|------|
| ~1997 | 인라인 이벤트 핸들러 | `onclick="..."` HTML 속성 |
| ~1997 | DOM Level 0 | `element.onclick = fn` 프로퍼티 방식 |
| 2000 | DOM Level 2 Events | `addEventListener` 도입, 캡처+버블 통합 |
| 현재 | Living Standard | `options` 객체, `passive`, `signal` 등 추가 |

DOM Level 2에서 두 모델을 통합하여 캡처 → 타겟 → 버블 3단계 전파 모델을 확립했다. 현재의 Living Standard는 이를 기반으로 성능 최적화(passive)와 생명주기 관리(signal) 기능을 추가했다.

---

## 2. EventTarget

`EventTarget`은 DOM 이벤트 시스템의 근간이 되는 인터페이스다. `Node`, `Window`, `XMLHttpRequest` 등 이벤트를 수신할 수 있는 모든 객체가 이를 상속한다.

```webidl
interface EventTarget {
  constructor();
  undefined addEventListener(DOMString type, EventListener? callback,
                             optional (AddEventListenerOptions or boolean) options = {});
  undefined removeEventListener(DOMString type, EventListener? callback,
                                optional (EventListenerOptions or boolean) options = {});
  boolean dispatchEvent(Event event);
};
```

### addEventListener

```js
// 기본 사용
button.addEventListener('click', handleClick);

// options 객체를 통한 세밀한 제어
button.addEventListener('click', handleClick, {
  capture: false,   // 캡처 단계에서 실행 여부 (기본: false)
  once: true,       // 한 번 실행 후 자동 제거
  passive: true,    // preventDefault() 호출하지 않겠다고 선언
  signal: controller.signal  // AbortSignal로 리스너 제거 제어
});
```

각 옵션의 역할:

- `capture`: `true`이면 캡처 단계에서 리스너가 실행된다. 같은 핸들러라도 `capture` 값이 다르면 별개의 리스너로 등록된다.
- `once`: 리스너가 최초 1회 호출된 후 자동으로 제거된다. 초기화 로직이나 일회성 애니메이션에 유용하다.
- `passive`: 브라우저에게 이 리스너가 `preventDefault()`를 호출하지 않을 것임을 알린다. 스크롤/터치 이벤트에서 브라우저가 기본 동작을 즉시 실행할 수 있어 성능이 향상된다.
- `signal`: `AbortSignal`을 전달하면, 해당 시그널이 중단될 때 리스너가 자동 제거된다.

```js
// passive 리스너 - 스크롤 성능 최적화
document.addEventListener('touchmove', onTouchMove, { passive: true });

// signal을 이용한 리스너 일괄 제거
const controller = new AbortController();

element.addEventListener('click', onClick, { signal: controller.signal });
element.addEventListener('keydown', onKeyDown, { signal: controller.signal });
element.addEventListener('mouseover', onHover, { signal: controller.signal });

// 세 리스너를 한 번에 제거
controller.abort();
```

### removeEventListener

리스너를 제거할 때는 등록 시와 동일한 함수 참조와 동일한 `capture` 값을 전달해야 한다.

```js
// 올바른 제거
const handler = (e) => console.log(e);
el.addEventListener('click', handler);
el.removeEventListener('click', handler); // 동일 참조 -> 제거 성공

// 실패하는 제거
el.addEventListener('click', (e) => console.log(e));
el.removeEventListener('click', (e) => console.log(e)); // 다른 참조 -> 제거 실패
```

### dispatchEvent

프로그래밍 방식으로 이벤트를 발생시킨다. 동기적으로 실행되며 `isTrusted`는 `false`가 된다.

```js
const event = new Event('build', { bubbles: true, cancelable: true });
const canceled = !element.dispatchEvent(event);
if (canceled) {
  console.log('이벤트가 preventDefault()로 취소됨');
}
```

---

## 3. Event 인터페이스

모든 이벤트 객체의 기반 인터페이스다.

```webidl
interface Event {
  constructor(DOMString type, optional EventInit eventInitDict = {});
  readonly attribute DOMString type;
  readonly attribute EventTarget? target;
  readonly attribute EventTarget? currentTarget;
  readonly attribute unsigned short eventPhase;
  readonly attribute boolean bubbles;
  readonly attribute boolean cancelable;
  readonly attribute boolean composed;
  readonly attribute boolean isTrusted;
  readonly attribute DOMHighResTimeStamp timeStamp;

  undefined stopPropagation();
  undefined stopImmediatePropagation();
  undefined preventDefault();
  readonly attribute boolean defaultPrevented;
};
```

### 핵심 속성

| 속성 | 설명 |
|------|------|
| `type` | 이벤트 이름 문자열 (`"click"`, `"input"` 등) |
| `target` | 이벤트가 최초로 디스패치된 객체 (이벤트 원점) |
| `currentTarget` | 현재 실행 중인 리스너가 등록된 객체 |
| `eventPhase` | 현재 전파 단계 (0: None, 1: Capture, 2: Target, 3: Bubble) |
| `bubbles` | 이벤트가 버블링되는지 여부 |
| `cancelable` | `preventDefault()`로 취소 가능한지 여부 |
| `composed` | Shadow DOM 경계를 넘어 전파되는지 여부 |
| `isTrusted` | 브라우저가 생성한 이벤트이면 `true`, 스크립트가 생성하면 `false` |

### target vs currentTarget

```js
// <div id="parent"><button id="child">Click</button></div>
document.getElementById('parent').addEventListener('click', (e) => {
  console.log(e.target);        // button#child (클릭한 실제 요소)
  console.log(e.currentTarget); // div#parent   (리스너가 등록된 요소)
});
```

### 전파 제어 메서드

```js
element.addEventListener('click', (e) => {
  e.stopPropagation();          // 다음 노드로의 전파 중단, 같은 노드의 다른 리스너는 실행
  e.stopImmediatePropagation(); // 같은 노드의 나머지 리스너도 실행 중단
  e.preventDefault();           // 브라우저 기본 동작 취소 (링크 이동, 폼 제출 등)
});
```

---

## 4. 이벤트 전파

### 3단계 전파 모델

이벤트가 발생하면 DOM 트리를 따라 3단계로 전파된다.

```
Phase 1: Capture (Window → Document → ... → Target의 부모)
Phase 2: Target  (Target 노드 자체)
Phase 3: Bubble  (Target의 부모 → ... → Document → Window)
```

```
         ① Capture ↓               ③ Bubble ↑
Window ─────────────────────────────────────── Window
  Document ───────────────────────────────── Document
    <html> ─────────────────────────────── <html>
      <body> ──────────────────────────── <body>
        <div> ────────────────────────── <div>
          <button>  ② Target Phase  </button>
```

```js
// 캡처 단계에서 실행
parent.addEventListener('click', handler, true);
// 또는
parent.addEventListener('click', handler, { capture: true });

// 버블 단계에서 실행 (기본값)
parent.addEventListener('click', handler, false);
```

### 이벤트 위임 패턴

이벤트 버블링을 활용하여 부모 요소에 단일 리스너를 등록하는 패턴이다. 동적으로 추가되는 자식 요소도 자동으로 처리할 수 있고, 메모리 효율이 높다.

```js
// 비효율적: 각 항목에 리스너 등록
document.querySelectorAll('li').forEach(li => {
  li.addEventListener('click', handleItem);
});

// 효율적: 이벤트 위임
document.querySelector('ul').addEventListener('click', (e) => {
  const li = e.target.closest('li');
  if (!li) return; // li 외부 클릭 무시
  handleItem(li);
});
```

`closest()` 메서드를 사용하면 클릭된 요소가 `<li>` 내부의 `<span>`이라 하더라도 올바르게 `<li>`를 찾아낼 수 있다.

---

## 5. CustomEvent

애플리케이션 고유의 이벤트를 정의할 수 있다. `detail` 속성으로 임의의 데이터를 전달한다.

```js
// 커스텀 이벤트 생성 및 디스패치
const event = new CustomEvent('user:login', {
  bubbles: true,
  composed: true,   // Shadow DOM 경계 통과
  detail: {
    userId: 42,
    username: 'alice',
    timestamp: Date.now()
  }
});
element.dispatchEvent(event);

// 수신
document.addEventListener('user:login', (e) => {
  console.log(e.detail.username); // 'alice'
});
```

### 컴포넌트 간 통신

CustomEvent는 느슨하게 결합된 컴포넌트 간 통신에 적합하다.

```js
// 컴포넌트 A: 장바구니에 상품 추가 알림
class ProductCard extends HTMLElement {
  addToCart(product) {
    this.dispatchEvent(new CustomEvent('cart:add', {
      bubbles: true,
      detail: { product }
    }));
  }
}

// 컴포넌트 B: 장바구니 상태 업데이트
class ShoppingCart extends HTMLElement {
  connectedCallback() {
    // 문서 레벨에서 이벤트 수신 (어디서든 발생 가능)
    document.addEventListener('cart:add', (e) => {
      this.items.push(e.detail.product);
      this.render();
    });
  }
}
```

---

## 6. 주요 이벤트 타입

### MouseEvent

```js
element.addEventListener('click', (e) => {
  e.clientX, e.clientY   // 뷰포트 기준 좌표
  e.pageX, e.pageY       // 문서 기준 좌표 (스크롤 포함)
  e.offsetX, e.offsetY   // 타겟 요소 기준 좌표
  e.button                // 0: 좌클릭, 1: 중간, 2: 우클릭
  e.buttons               // 동시에 눌린 버튼 비트마스크
  e.altKey, e.ctrlKey, e.shiftKey, e.metaKey // 수정키 상태
});
// 관련 이벤트: click, dblclick, mousedown, mouseup, mousemove, mouseenter, mouseleave
```

### KeyboardEvent

```js
document.addEventListener('keydown', (e) => {
  e.key       // 'a', 'Enter', 'ArrowUp' 등 논리적 키 값
  e.code      // 'KeyA', 'Enter' 등 물리적 키 코드
  e.repeat    // 키를 계속 누르고 있으면 true

  // Ctrl+S 단축키 구현
  if (e.ctrlKey && e.key === 's') {
    e.preventDefault();
    save();
  }
});
// key vs code: 'key'는 입력 언어/레이아웃에 따라 달라지고, 'code'는 물리적 위치 기반
```

### FocusEvent

```js
// focusin/focusout: 버블링 O, focus/blur: 버블링 X
input.addEventListener('focusin', (e) => {
  e.relatedTarget; // 포커스를 잃은 이전 요소
});
input.addEventListener('focusout', (e) => {
  e.relatedTarget; // 포커스를 받을 다음 요소
});
```

### InputEvent

```js
input.addEventListener('input', (e) => {
  e.data          // 삽입된 문자 (삭제 시 null)
  e.inputType     // 'insertText', 'deleteContentBackward', 'insertFromPaste' 등
  e.isComposing   // IME 조합 중인지 여부 (한글/일본어 입력)
});
// 'input'은 값이 변경된 후, 'beforeinput'은 변경 전에 발생 (취소 가능)
```

### WheelEvent / TouchEvent / PointerEvent

```js
// WheelEvent - 스크롤 휠
element.addEventListener('wheel', (e) => {
  e.deltaX, e.deltaY, e.deltaZ  // 스크롤 양
  e.deltaMode  // 0: 픽셀, 1: 라인, 2: 페이지
}, { passive: true });

// TouchEvent - 터치 스크린
element.addEventListener('touchstart', (e) => {
  const touch = e.touches[0];      // 현재 화면 위의 모든 터치
  touch.clientX, touch.clientY
  e.changedTouches  // 이번 이벤트에서 변경된 터치
  e.targetTouches   // 현재 요소 위의 터치
});

// PointerEvent - 마우스+터치+펜 통합 API
element.addEventListener('pointerdown', (e) => {
  e.pointerId      // 고유 포인터 ID
  e.pointerType    // 'mouse', 'touch', 'pen'
  e.pressure       // 필압 (0.0 ~ 1.0)
  e.width, e.height // 접촉 영역 크기
  e.isPrimary      // 기본 포인터 여부
});
```

### CompositionEvent

IME(Input Method Editor)로 한글, 일본어, 중국어 등을 입력할 때 발생한다.

```js
input.addEventListener('compositionstart', (e) => {
  // IME 조합 시작 (예: 한글 'ㅎ' 입력 시작)
});
input.addEventListener('compositionupdate', (e) => {
  e.data; // 현재 조합 중인 문자열 (예: '하', '한')
});
input.addEventListener('compositionend', (e) => {
  e.data; // 확정된 문자열 (예: '한글')
});
```

---

## 7. MutationObserver

DOM 변경을 비동기적으로 감시한다. 이전의 `MutationEvents`(DOM Level 2)는 동기적 실행으로 인한 성능 문제로 폐기되었고, `MutationObserver`가 대체제로 도입되었다.

```js
const observer = new MutationObserver((mutations, observer) => {
  for (const mutation of mutations) {
    switch (mutation.type) {
      case 'childList':
        console.log('추가:', mutation.addedNodes);
        console.log('제거:', mutation.removedNodes);
        break;
      case 'attributes':
        console.log(`${mutation.attributeName}: ${mutation.oldValue} → 새 값`);
        break;
      case 'characterData':
        console.log(`텍스트 변경: ${mutation.oldValue}`);
        break;
    }
  }
});
```

### observe 옵션

```js
observer.observe(targetNode, {
  childList: true,              // 자식 노드 추가/제거 감시
  attributes: true,             // 속성 변경 감시
  characterData: true,          // 텍스트 내용 변경 감시
  subtree: true,                // 하위 트리 전체 감시
  attributeOldValue: true,      // 변경 전 속성 값 기록
  characterDataOldValue: true,  // 변경 전 텍스트 기록
  attributeFilter: ['class', 'style'] // 특정 속성만 감시
});
```

### MutationRecord 구조

| 속성 | 설명 |
|------|------|
| `type` | `'childList'`, `'attributes'`, `'characterData'` |
| `target` | 변경이 발생한 노드 |
| `addedNodes` | 추가된 노드 목록 (NodeList) |
| `removedNodes` | 제거된 노드 목록 (NodeList) |
| `previousSibling` | 추가/제거된 노드의 이전 형제 |
| `nextSibling` | 추가/제거된 노드의 다음 형제 |
| `attributeName` | 변경된 속성 이름 |
| `oldValue` | 변경 전 값 |

### disconnect와 takeRecords

```js
// 아직 콜백에 전달되지 않은 대기 중인 레코드 수동 회수
const pendingRecords = observer.takeRecords();

// 감시 중단
observer.disconnect();
```

---

## 8. Range와 Selection

### Range

`Range`는 문서 내 연속된 영역을 표현한다. 텍스트 선택, DOM 조작, 에디터 구현에 핵심적이다.

```js
const range = new Range();
// 또는
const range = document.createRange();

// 범위 설정
range.setStart(startNode, startOffset);
range.setEnd(endNode, endOffset);

// 편의 메서드
range.selectNode(node);          // 노드 자체를 범위로
range.selectNodeContents(node);  // 노드의 내용을 범위로
range.collapse(toStart);         // 범위를 시작점(true) 또는 끝점(false)으로 축소
```

### Range 조작

```js
// 콘텐츠 추출 (원본에서 제거)
const fragment = range.extractContents();

// 콘텐츠 복제 (원본 유지)
const clone = range.cloneContents();

// 콘텐츠 삭제
range.deleteContents();

// 범위에 노드 삽입
const newNode = document.createElement('span');
range.insertNode(newNode);

// 범위를 노드로 감싸기
const wrapper = document.createElement('mark');
range.surroundContents(wrapper);
```

### Selection API

```js
const selection = window.getSelection();

// 현재 선택 정보
selection.anchorNode;     // 선택 시작 노드
selection.anchorOffset;   // 선택 시작 오프셋
selection.focusNode;      // 선택 끝 노드
selection.focusOffset;    // 선택 끝 오프셋
selection.isCollapsed;    // 선택 영역이 0인지 (커서만 있는 상태)
selection.rangeCount;     // Range 개수

// Range 조작
const range = selection.getRangeAt(0);
selection.addRange(range);
selection.removeAllRanges();
selection.collapse(node, offset);
selection.selectAllChildren(node);

// 선택된 텍스트
const text = selection.toString();
```

---

## 9. NodeIterator와 TreeWalker

DOM 트리를 순회하기 위한 전용 인터페이스다.

### NodeIterator

순차적(flat) 순회에 적합하다. `nextNode()`와 `previousNode()`로 이동한다.

```js
const iterator = document.createNodeIterator(
  document.body,         // 루트 노드
  NodeFilter.SHOW_TEXT,  // 텍스트 노드만
  {
    acceptNode(node) {
      // 빈 텍스트 노드 건너뛰기
      return node.textContent.trim()
        ? NodeFilter.FILTER_ACCEPT
        : NodeFilter.FILTER_REJECT;
    }
  }
);

let node;
while (node = iterator.nextNode()) {
  console.log(node.textContent);
}
```

### TreeWalker

트리 구조 탐색에 적합하다. 부모/자식/형제 방향으로 자유롭게 이동할 수 있다.

```js
const walker = document.createTreeWalker(
  document.body,
  NodeFilter.SHOW_ELEMENT,
  {
    acceptNode(node) {
      if (node.matches('.hidden')) return NodeFilter.FILTER_REJECT;  // 하위도 건너뜀
      if (node.matches('.skip'))   return NodeFilter.FILTER_SKIP;    // 자신만 건너뜀
      return NodeFilter.FILTER_ACCEPT;
    }
  }
);

// 탐색 메서드
walker.parentNode();      // 부모로 이동
walker.firstChild();      // 첫 자식으로 이동
walker.lastChild();       // 마지막 자식으로 이동
walker.previousSibling(); // 이전 형제로 이동
walker.nextSibling();     // 다음 형제로 이동
walker.nextNode();        // 문서 순서상 다음 노드
walker.previousNode();    // 문서 순서상 이전 노드
walker.currentNode;       // 현재 노드 (읽기/쓰기 가능)
```

### NodeFilter 상수

| 상수 | 값 | 대상 |
|------|------|------|
| `SHOW_ALL` | `0xFFFFFFFF` | 모든 노드 |
| `SHOW_ELEMENT` | `0x1` | Element 노드 |
| `SHOW_TEXT` | `0x4` | Text 노드 |
| `SHOW_COMMENT` | `0x80` | Comment 노드 |
| `SHOW_DOCUMENT` | `0x100` | Document 노드 |
| `SHOW_DOCUMENT_FRAGMENT` | `0x400` | DocumentFragment |

FILTER_REJECT vs FILTER_SKIP: `REJECT`는 해당 노드와 그 하위 트리 전체를 건너뛰고, `SKIP`은 해당 노드만 건너뛰고 자식은 계속 순회한다. (NodeIterator에서는 둘 다 동일하게 동작한다.)

---

## 10. AbortController와 AbortSignal

비동기 작업의 취소를 위한 범용 메커니즘이다. fetch 요청뿐 아니라 이벤트 리스너, 스트림, 커스텀 비동기 작업 등에 널리 사용된다.

### 기본 사용

```js
const controller = new AbortController();
const signal = controller.signal;

// fetch 취소
fetch('/api/data', { signal })
  .then(res => res.json())
  .catch(err => {
    if (err.name === 'AbortError') {
      console.log('요청이 취소됨');
    }
  });

// 3초 후 취소
setTimeout(() => controller.abort(), 3000);

// 취소 이유 전달 (선택)
controller.abort(new Error('사용자가 취소함'));
signal.reason; // Error: 사용자가 취소함
```

### AbortSignal 정적 메서드

```js
// 타임아웃 시그널 - 지정된 시간 후 자동으로 중단
const signal = AbortSignal.timeout(5000);
fetch('/api/slow', { signal }); // 5초 후 자동 취소

// 이미 중단된 시그널 생성
const aborted = AbortSignal.abort('이유');

// 여러 시그널 중 하나라도 중단되면 중단되는 복합 시그널
const combined = AbortSignal.any([
  AbortSignal.timeout(5000),
  controller.signal,
  otherController.signal
]);
```

### 이벤트 리스너 관리

```js
// 컴포넌트 생명주기에 맞춘 리스너 관리
class MyComponent {
  #controller = new AbortController();

  mount() {
    const { signal } = this.#controller;

    window.addEventListener('resize', this.onResize, { signal });
    window.addEventListener('scroll', this.onScroll, { signal, passive: true });
    document.addEventListener('keydown', this.onKeyDown, { signal });
    this.element.addEventListener('click', this.onClick, { signal });
  }

  unmount() {
    // 모든 리스너를 한 번에 정리
    this.#controller.abort();
  }
}
```

---

## 11. Shadow DOM

Shadow DOM은 DOM의 캡슐화(encapsulation) 메커니즘이다. 컴포넌트 내부의 DOM 구조와 스타일을 외부로부터 격리한다.

### attachShadow

```js
class MyCard extends HTMLElement {
  constructor() {
    super();
    // mode: 'open' -> element.shadowRoot로 접근 가능
    // mode: 'closed' -> 외부에서 접근 불가
    const shadow = this.attachShadow({ mode: 'open' });

    shadow.innerHTML = `
      <style>
        /* Shadow DOM 내부에서만 적용 */
        :host {
          display: block;
          border: 1px solid #ccc;
          padding: 16px;
        }
        :host(.featured) {
          border-color: gold;
        }
        :host-context(.dark-theme) {
          background: #333;
          color: white;
        }
        h2 { color: navy; }
      </style>
      <h2><slot name="title">기본 제목</slot></h2>
      <div class="content">
        <slot>기본 콘텐츠</slot>
      </div>
    `;
  }
}
customElements.define('my-card', MyCard);
```

```html
<my-card class="featured">
  <span slot="title">카드 제목</span>
  <p>카드 본문 내용</p>
</my-card>
```

### Slot

`<slot>`은 외부 콘텐츠(Light DOM)가 Shadow DOM 내부에 투영(projection)되는 지점이다.

```js
// slotchange 이벤트로 슬롯 변경 감지
shadow.querySelector('slot').addEventListener('slotchange', (e) => {
  const assigned = e.target.assignedNodes({ flatten: true });
  console.log('슬롯에 할당된 노드:', assigned);
});
```

### Shadow DOM CSS 셀렉터

| 셀렉터 | 적용 대상 |
|--------|----------|
| `:host` | Shadow Root의 호스트 요소 |
| `:host(selector)` | 조건부 호스트 스타일링 |
| `:host-context(selector)` | 호스트의 조상이 조건에 맞을 때 |
| `::slotted(selector)` | 슬롯에 분배된 Light DOM 요소 (직계만) |
| `::part(name)` | 외부에서 Shadow DOM 내부 요소를 스타일링 |

```css
/* 외부 CSS에서 part를 통해 Shadow DOM 내부 스타일링 */
my-card::part(header) {
  background: linear-gradient(to right, #667eea, #764ba2);
  color: white;
}
```

```js
// Shadow DOM 내부에서 part 노출
shadow.innerHTML = `<header part="header">...</header>`;
```

### 이벤트와 Shadow DOM

`composed: true`인 이벤트만 Shadow DOM 경계를 넘어 전파된다. 네이티브 UI 이벤트(click, input 등)는 대부분 composed이다.

```js
// Shadow DOM 내부에서 발생한 이벤트
// composed: true  -> Shadow 경계 밖으로 전파됨 (target은 호스트로 리타게팅)
// composed: false -> Shadow 내부에서만 전파됨
shadowChild.dispatchEvent(new CustomEvent('internal', {
  bubbles: true,
  composed: false  // Shadow 내부에만 전파
}));
```

---

## 12. Mutation Algorithms

DOM 명세는 노드 조작 시 내부적으로 실행되는 알고리즘을 정의한다. 이 알고리즘들은 `appendChild`, `removeChild`, `replaceChild` 등의 API 호출 시 브라우저가 실제로 수행하는 단계를 기술한다.

### Insert (삽입)

`parent.appendChild(node)` 또는 `parent.insertBefore(node, child)` 호출 시 실행된다.

```
1. 삽입할 노드가 DocumentFragment이면 그 자식 노드들을 삽입 대상으로 설정
2. 노드가 이미 트리에 존재하면 기존 위치에서 제거 (adopt)
3. 대상 노드를 부모의 자식 목록에 삽입
4. 각 삽입된 노드에 대해:
   a. 노드의 ownerDocument 업데이트
   b. 연결(connected) 상태가 변경되면 connectedCallback 호출 (Custom Element)
   c. MutationObserver에 childList 변경 통지
```

```js
const fragment = document.createDocumentFragment();
fragment.append(el1, el2, el3);
// 한 번의 insert로 세 노드 삽입 -> MutationRecord 1개, 리플로우 1회
parent.appendChild(fragment);
```

### Remove (제거)

```
1. 부모의 자식 목록에서 노드 제거
2. 노드가 connected 상태였으면 disconnectedCallback 호출 (Custom Element)
3. MutationObserver에 childList 변경 통지 (removedNodes에 포함)
4. 제거된 노드의 모든 하위 노드도 연결 해제
```

```js
// 자기 자신 제거 (모던 API)
element.remove();

// 부모를 통한 제거 (레거시 API)
parent.removeChild(element);
```

### Replace (교체)

`parent.replaceChild(newChild, oldChild)` 호출 시 실행된다.

```
1. 새 노드의 유효성 검증 (Document 구조 위반 확인)
2. 새 노드가 DocumentFragment이면 자식 노드들 추출
3. 기존 노드 제거 (Remove 알고리즘)
4. 새 노드 삽입 (Insert 알고리즘)
5. MutationObserver 통지 (addedNodes + removedNodes 모두 포함)
```

```js
// 모던 API
oldElement.replaceWith(newElement);

// 여러 노드로 교체도 가능
oldElement.replaceWith(span, ' 텍스트 ', anotherSpan);
```

### Adopt (입양)

노드를 다른 Document로 이동시키는 알고리즘이다. `document.adoptNode(node)` 호출 시 또는 다른 Document의 노드를 삽입할 때 자동으로 실행된다.

```
1. 노드의 기존 부모가 있으면 해당 부모에서 제거
2. 노드와 모든 하위 노드의 ownerDocument를 새 Document로 변경
3. 각 노드에 대해 adopting steps 실행 (Custom Element의 adoptedCallback 호출)
```

```js
// iframe의 노드를 현재 문서로 가져오기
const iframe = document.querySelector('iframe');
const foreignNode = iframe.contentDocument.querySelector('.widget');

// adoptNode: ownerDocument 변경
const adopted = document.adoptNode(foreignNode);
document.body.appendChild(adopted);

// importNode: 복제 + ownerDocument 변경 (원본 유지)
const imported = document.importNode(foreignNode, true); // true = deep clone
document.body.appendChild(imported);
```

### 주요 유효성 검사 규칙

DOM 트리의 무결성을 보장하기 위해 삽입/교체 시 다음 규칙이 적용된다.

```
- Document는 최대 하나의 Element 자식(document element)을 가질 수 있다
- Document는 최대 하나의 DocumentType 자식을 가질 수 있다
- DocumentType은 Document의 자식이어야 한다
- 노드를 자기 자신의 하위에 삽입할 수 없다 (순환 방지)
- 위반 시 HierarchyRequestError DOMException 발생
```

```js
try {
  const child = document.querySelector('.child');
  const parent = document.querySelector('.parent');
  child.appendChild(parent); // parent가 child의 조상이면 에러
} catch (e) {
  console.error(e); // HierarchyRequestError
}
```

---

## 참고 자료

- [DOM Living Standard (WHATWG)](https://dom.spec.whatwg.org/)
- [UI Events Specification (W3C)](https://w3c.github.io/uievents/)
- [Pointer Events (W3C)](https://w3c.github.io/pointerevents/)
- [Selection API (W3C)](https://w3c.github.io/selection-api/)

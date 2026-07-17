# DOM 라이브러리 기초와 Event/EventTarget 타입

TypeScript는 브라우저 DOM API를 직접 설계하지 않는다. 대신 `lib.dom.d.ts`라는 하나의 거대한 선언 파일에 `document`, `window`, `HTMLElement`, `Event` 같은 전역 타입을 미리 정의해두고, `tsconfig.json`의 `lib` 옵션으로 이 정의를 프로젝트에 포함시킬지 말지를 결정한다. 이 문서는 DOM 타입이 어디서 오는지, `lib` 옵션이 무엇을 바꾸는지, 그리고 이벤트 시스템의 핵심인 `EventTarget`/`Event`가 어떻게 타입으로 표현되는지를 정리한다.

## lib.dom.d.ts: DOM 타입은 어디서 오는가

> **원문:** https://www.typescriptlang.org/docs/handbook/dom-manipulation.html

- TypeScript 배포판에는 2만 줄이 넘는 `lib.dom.d.ts`가 포함되어 있고, 기본 설정의 프로젝트라면 별도 설치 없이 `document`, `HTMLElement` 등을 바로 쓸 수 있다.
- 이 파일의 타입 정의는 MDN 문서와 거의 1:1로 대응하도록 관리되기 때문에, 어떤 DOM API의 타입 시그니처가 궁금하면 MDN에서 같은 이름을 찾아보면 된다.
- `Document.getElementById`는 요소를 못 찾을 수도 있다는 사실을 반영해 `HTMLElement | null`을 반환한다.

```ts
const app = document.getElementById("app");
// app: HTMLElement | null
app?.append("hello"); // null일 수 있으므로 옵셔널 체이닝 필요
```

- `createElement`, `querySelector`, `querySelectorAll`은 태그 이름을 제네릭 키로 받아 `HTMLElementTagNameMap`을 조회하는 오버로드를 갖고 있다. 그래서 `"a"`를 넘기면 `HTMLAnchorElement`, `"div"`를 넘기면 `HTMLDivElement`가 나오고, 맵에 없는 임의 문자열을 넘기면 범용 `HTMLElement`로 떨어진다.

```ts
const link = document.createElement("a"); // HTMLAnchorElement
const box = document.querySelector("div.box"); // HTMLDivElement | null
const custom = document.createElement("my-widget"); // HTMLElement (맵에 없음)
```

- `Node.appendChild<T extends Node>(newChild: T): T`처럼 매개변수 타입 그대로를 반환 타입으로 돌려주는 제네릭도 자주 보인다. 인자로 넘긴 구체 타입이 그대로 반환값의 타입이 된다.
- `children`(HTMLCollection, 요소 노드만)과 `childNodes`(NodeList, 텍스트 노드 포함 모든 노드)는 타입도 다르고 담기는 값도 다르므로 혼동하지 않아야 한다.
- `HTMLElement`는 `Element`를 상속하고 `Element`는 `Node`를 상속하는 계층 구조를 가지므로, 어떤 요소든 `Node`가 제공하는 메서드(`appendChild`, `removeChild` 등)를 그대로 쓸 수 있다.

## tsconfig의 lib 옵션

> **원문:** https://www.typescriptlang.org/tsconfig/#lib

- `lib`은 컴파일 시 어떤 내장 타입 선언 묶음을 불러올지 정하는 옵션이다. ECMAScript 버전별 타입(`es2015`, `es2020`, …)과 런타임 환경별 타입(`dom`, `dom.iterable`, `webworker` 등)을 조합해서 지정한다.
- `lib`을 생략하면 `target`에 따라 기본값이 자동으로 채워진다. 예를 들어 `target: "es2020"`이면 `lib`은 기본적으로 `["dom", "es2020", "dom.iterable"]`이 된다. 즉 별도 설정 없이도 브라우저 DOM 타입은 기본으로 포함되어 있다.
- Node.js처럼 브라우저 DOM이 없는 런타임을 대상으로 한다면 `"dom"`을 명시적으로 빼고 ES 버전만 남기는 것이 안전하다. 그래야 `window`, `document` 같은 존재하지 않는 전역을 실수로 참조했을 때 컴파일 타임에 바로 걸러진다.

```json
{
  "compilerOptions": {
    "target": "es2020",
    "lib": ["es2020"] // "dom"을 빼서 브라우저 전역 사용을 막는다
  }
}
```

```json
{
  "compilerOptions": {
    "target": "es2020",
    "lib": ["es2020", "dom", "dom.iterable"] // 브라우저용 프로젝트
  }
}
```

- `lib`을 지정하면 `target`이 정해주던 기본값 전체가 덮어써진다는 점에 유의해야 한다. 즉 `lib: ["dom"]`처럼 ES 버전을 빼먹으면 `Promise`, `Map` 같은 최신 내장 타입도 함께 사라진다.

## EventTarget: 이벤트를 주고받는 객체의 공통 인터페이스

> **원문:** https://developer.mozilla.org/en-US/docs/Web/API/EventTarget

- `EventTarget`은 이벤트를 받고 리스너를 등록할 수 있는 모든 객체가 구현하는 인터페이스다. `Element`, `Document`, `Window`뿐 아니라 `XMLHttpRequest`, `AudioNode` 같은 DOM 외부 API도 여기 포함된다.
- 핵심 메서드는 세 개다.
  - `addEventListener(type, listener, options?)` — 리스너 등록
  - `removeEventListener(type, listener, options?)` — 리스너 해제
  - `dispatchEvent(event)` — 이벤트를 직접 발생시킴
- `lib.dom.d.ts`에서 `addEventListener`는 이벤트 종류에 따라 리스너의 콜백 매개변수 타입이 달라지도록 오버로드되어 있다. `"click"`을 등록하면 콜백의 인자가 `MouseEvent`로, `"keydown"`을 등록하면 `KeyboardEvent`로 좁혀진다.

```ts
const button = document.querySelector("button");

button?.addEventListener("click", (e) => {
  // e: MouseEvent — clientX, clientY 등 마우스 전용 속성 사용 가능
  console.log(e.clientX, e.clientY);
});

button?.addEventListener("keydown", (e) => {
  // e: KeyboardEvent
  console.log(e.key);
});
```

- 세 번째 인자인 `options` 객체(`AddEventListenerOptions`)의 필드들도 타입으로 표현되어 있다.
  - `capture`: 버블링이 아닌 캡처링 단계에서 리스너 실행
  - `once`: 첫 실행 후 자동으로 리스너 제거
  - `passive`: `preventDefault`를 호출하지 않겠다는 선언(스크롤 성능 최적화)
  - `signal`: `AbortSignal`이 abort되면 리스너를 자동 해제

```ts
const controller = new AbortController();

document.addEventListener(
  "scroll",
  () => console.log("scrolling"),
  { passive: true, signal: controller.signal }
);

controller.abort(); // 위 리스너가 자동으로 제거됨
```

- 커스텀 클래스에 이벤트 기능을 붙이고 싶다면 `EventTarget`을 직접 상속하면 된다. 브라우저 전용 API가 아니라 Web Workers에서도 동작하는 범용 인터페이스라 순수 로직 클래스에도 적용할 수 있다.

```ts
class Ticker extends EventTarget {
  tick() {
    this.dispatchEvent(new Event("tick"));
  }
}

const ticker = new Ticker();
ticker.addEventListener("tick", () => console.log("tick!"));
ticker.tick();
```

## Event: 이벤트 객체 자체의 타입

> **원문:** https://developer.mozilla.org/en-US/docs/Web/API/Event

- `Event`는 이벤트 하나를 나타내는 값이다. `MouseEvent`, `KeyboardEvent`, `CustomEvent` 등은 모두 `Event`를 확장한 하위 타입이고, `lib.dom.d.ts`에서도 이 상속 관계가 그대로 인터페이스 계층으로 표현되어 있다.
- 주요 읽기 전용 속성:
  - `type: string` — 이벤트 이름 (`"click"` 등)
  - `target: EventTarget | null` — 이벤트가 실제로 발생한 객체
  - `currentTarget: EventTarget | null` — 현재 핸들러가 붙어 있는 객체(버블링 중에는 target과 다를 수 있음)
  - `bubbles: boolean` / `cancelable: boolean` — 전파 가능 여부 / 취소 가능 여부
  - `defaultPrevented: boolean` — `preventDefault()` 호출 여부
  - `eventPhase: number` — `NONE`, `CAPTURING_PHASE`, `AT_TARGET`, `BUBBLING_PHASE` 중 현재 단계
- 주요 메서드:
  - `preventDefault()` — 브라우저 기본 동작 취소 (`cancelable`이 true일 때만 의미 있음)
  - `stopPropagation()` — 캡처링/버블링 전파 중단
  - `stopImmediatePropagation()` — 전파 중단에 더해 같은 요소에 남은 다른 리스너 실행도 막음

```ts
document.addEventListener("click", function handler(e: Event) {
  if (e.target instanceof HTMLButtonElement) {
    e.stopPropagation();
    console.log(e.eventPhase === Event.AT_TARGET);
  }
});
```

- `target`, `currentTarget`은 타입이 `EventTarget | null`로 넓게 잡혀 있어서 구체적인 요소 메서드(`.value`, `.classList` 등)를 쓰려면 `instanceof`로 좁히거나 타입 단언이 필요하다.

```ts
function onInput(e: Event) {
  const input = e.target as HTMLInputElement;
  console.log(input.value);
}
```

- 이벤트를 직접 만들 때는 생성자에 `bubbles`, `cancelable`, `composed` 옵션을 넘길 수 있다. `CustomEvent<T>`는 여기에 더해 임의의 데이터를 담는 `detail: T` 필드를 제네릭으로 타입 지정할 수 있다.

```ts
interface OrderPlacedDetail {
  orderId: string;
  total: number;
}

const orderEvent = new CustomEvent<OrderPlacedDetail>("order-placed", {
  detail: { orderId: "A-1", total: 39900 },
  bubbles: true,
});
document.dispatchEvent(orderEvent);

document.addEventListener("order-placed", (e) => {
  // e는 Event로 추론되므로 detail을 쓰려면 CustomEvent<OrderPlacedDetail>로 단언이 필요하다
  const detail = (e as CustomEvent<OrderPlacedDetail>).detail;
  console.log(detail.orderId, detail.total);
});
```

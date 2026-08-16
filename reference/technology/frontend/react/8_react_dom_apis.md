# React DOM APIs

## react-dom APIs

### createPortal

> 원문: https://react.dev/reference/react-dom/createPortal

- React 트리 외부의 DOM 노드에 자식을 렌더링할 수 있는 API임

```js
import { createPortal } from 'react-dom';
```

**시그니처**

```js
createPortal(children, domNode, key?)
```

**매개변수**

- `children`: React로 렌더링할 수 있는 모든 것. JSX(`<div />`, `<SomeComponent />`), Fragment(`<>...</>`), 문자열, 숫자, 배열 등
- `domNode`: `document.getElementById()` 등이 반환하는 DOM 노드. 이미 존재해야 함. 업데이트 중 다른 DOM 노드를 전달하면 포탈 콘텐츠가 재생성됨
- `key`(선택): 포탈의 키로 사용할 고유 문자열 또는 숫자

**반환값**

- JSX에 포함하거나 React 컴포넌트에서 반환할 수 있는 React 노드를 반환함
- React가 렌더 출력에서 이 노드를 만나면 `children`을 `domNode` 안에 배치함

**주의사항**

- 포탈의 이벤트는 DOM 트리가 아닌 React 트리에 따라 전파됨. 포탈 내부를 클릭했을 때 포탈이 `<div onClick>`으로 감싸져 있으면 해당 `onClick` 핸들러가 실행됨
- 포탈은 DOM 노드의 물리적 배치만 변경함
- 자식은 부모 트리가 제공하는 context에 접근 가능함
- 모달, 툴팁 등 `overflow` 제약을 벗어나야 하는 UI에 유용함
- 비-React 마크업 및 서드파티 위젯과 통합 가능함

**코드 예시**

기본 사용:

```js
import { createPortal } from 'react-dom';

// ...

<div>
  <p>This child is placed in the parent div.</p>
  {createPortal(
    <p>This child is placed in the document body.</p>,
    document.body
  )}
</div>
```

다른 DOM 위치에 렌더링:

```js
import { createPortal } from 'react-dom';

function MyComponent() {
  return (
    <div style={{ border: '2px solid black' }}>
      <p>This child is placed in the parent div.</p>
      {createPortal(
        <p>This child is placed in the document body.</p>,
        document.body
      )}
    </div>
  );
}
```

결과 DOM 구조:

```html
<body>
  <div id="root">
    ...
      <div style="border: 2px solid black">
        <p>This child is placed inside the parent div.</p>
      </div>
    ...
  </div>
  <p>This child is placed in the document body.</p>
</body>
```

모달 다이얼로그 렌더링:

```js
import { useState } from 'react';
import { createPortal } from 'react-dom';
import ModalContent from './ModalContent.js';

export default function PortalExample() {
  const [showModal, setShowModal] = useState(false);
  return (
    <>
      <button onClick={() => setShowModal(true)}>
        Show modal using a portal
      </button>
      {showModal && createPortal(
        <ModalContent onClose={() => setShowModal(false)} />,
        document.body
      )}
    </>
  );
}
```

```js
export default function ModalContent({ onClose }) {
  return (
    <div className="modal">
      <div>I'm a modal dialog</div>
      <button onClick={onClose}>Close</button>
    </div>
  );
}
```

비-React 서버 마크업에 렌더링:

```js
import { createPortal } from 'react-dom';

const sidebarContentEl = document.getElementById('sidebar-content');

export default function App() {
  return (
    <>
      <MainContent />
      {createPortal(
        <SidebarContent />,
        sidebarContentEl
      )}
    </>
  );
}
```

서드파티 위젯과 통합:

```js
import { useRef, useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { createMapWidget, addPopupToMapWidget } from './map-widget.js';

export default function Map() {
  const containerRef = useRef(null);
  const mapRef = useRef(null);
  const [popupContainer, setPopupContainer] = useState(null);

  useEffect(() => {
    if (mapRef.current === null) {
      const map = createMapWidget(containerRef.current);
      mapRef.current = map;
      const popupDiv = addPopupToMapWidget(map);
      setPopupContainer(popupDiv);
    }
  }, []);

  return (
    <div style={{ width: 250, height: 250 }} ref={containerRef}>
      {popupContainer !== null && createPortal(
        <p>Hello from React!</p>,
        popupContainer
      )}
    </div>
  );
}
```

- 접근성: 포탈 사용 시 키보드 포커스 관리가 중요함. WAI-ARIA Modal Authoring Practices를 따를 것

---

### flushSync

> 원문: https://react.dev/reference/react-dom/flushSync

- React의 상태 업데이트를 동기적으로 강제 플러시하는 API임
- 성능에 악영향을 줄 수 있으므로 드물게 사용해야 함

```js
import { flushSync } from 'react-dom';
```

**시그니처**

```js
flushSync(callback)
```

**매개변수**

- `callback`: 함수. React가 이 콜백을 즉시 호출하고 포함된 모든 업데이트를 동기적으로 플러시함. 보류 중인 업데이트, Effect, Effect 내부의 업데이트도 플러시할 수 있음. 이 호출로 인해 업데이트가 서스펜드되면 폴백이 다시 표시될 수 있음

**반환값**

- `undefined`를 반환함

**주의사항**

- 성능을 크게 저하시킬 수 있음. 드물게 사용할 것
- 보류 중인 Suspense 경계가 `fallback` 상태를 표시하도록 강제할 수 있음
- 보류 중인 Effect를 실행하고 포함된 업데이트를 동기적으로 적용한 후 반환할 수 있음
- 콜백 내부의 업데이트를 플러시하기 위해 콜백 외부의 보류 중인 업데이트도 플러시할 수 있음

**코드 예시**

기본 사용:

```js
flushSync(() => {
  setSomething(123);
});
// By this line, the DOM is updated.
```

Print API 통합:

```js
import { useState, useEffect } from 'react';
import { flushSync } from 'react-dom';

export default function PrintApp() {
  const [isPrinting, setIsPrinting] = useState(false);

  useEffect(() => {
    function handleBeforePrint() {
      flushSync(() => {
        setIsPrinting(true);
      })
    }

    function handleAfterPrint() {
      setIsPrinting(false);
    }

    window.addEventListener('beforeprint', handleBeforePrint);
    window.addEventListener('afterprint', handleAfterPrint);
    return () => {
      window.removeEventListener('beforeprint', handleBeforePrint);
      window.removeEventListener('afterprint', handleAfterPrint);
    }
  }, []);

  return (
    <>
      <h1>isPrinting: {isPrinting ? 'yes' : 'no'}</h1>
      <button onClick={() => window.print()}>
        Print
      </button>
    </>
  );
}
```

- `flushSync` 없이는 print 다이얼로그가 `isPrinting`을 "no"로 표시함. React가 업데이트를 비동기적으로 배치하기 때문에 상태 업데이트 전에 print 다이얼로그가 표시됨

**트러블슈팅: "flushSync was called from inside a lifecycle method"**

- 렌더링 중간에 `flushSync`를 호출할 수 없음
- 컴포넌트 렌더링, `useLayoutEffect`, `useEffect`, 클래스 컴포넌트 라이프사이클 메서드 내부에서 호출하면 경고가 발생하고 무시됨

해결 방법 1 - 이벤트 핸들러로 이동:

```js
function handleClick() {
  flushSync(() => {
    setSomething(newValue);
  });
}
```

해결 방법 2 - 마이크로태스크로 지연:

```js
useEffect(() => {
  queueMicrotask(() => {
    flushSync(() => {
      setSomething(newValue);
    });
  });
}, []);
```

- 마이크로태스크에서 `flushSync` 호출은 성능이 더 나빠짐. 다른 모든 방법을 먼저 시도할 것

---

### preconnect

> 원문: https://react.dev/reference/react-dom/preconnect

- 리소스를 로드할 서버에 미리 연결할 수 있는 API임

```js
import { preconnect } from 'react-dom';
```

**시그니처**

```js
preconnect(href)
```

**매개변수**

- `href`: 문자열. 연결할 서버의 URL

**반환값**

- 반환값 없음

**주의사항**

- 같은 서버에 대한 여러 번의 호출은 단일 호출과 동일한 효과임
- 브라우저에서는 렌더링 중, Effect 내부, 이벤트 핸들러 등 어디서든 호출 가능함
- SSR 또는 Server Components 렌더링 시에는 컴포넌트 렌더링 중이거나 렌더링에서 비롯된 async context에서만 효과가 있음. 그 외 호출은 무시됨
- 웹페이지 자체가 호스팅된 서버에 대한 preconnect는 이미 연결되어 있으므로 이점이 없음
- 필요한 리소스를 알고 있다면 즉시 로딩을 시작하는 다른 함수를 사용할 것

**코드 예시**

렌더링 시 preconnect:

```js
import { preconnect } from 'react-dom';

function AppRoot() {
  preconnect("https://example.com");
  // ...
}
```

이벤트 핸들러에서 preconnect:

```js
import { preconnect } from 'react-dom';

function CallToAction() {
  const onClick = () => {
    preconnect('http://example.com');
    startWizard();
  }
  return (
    <button onClick={onClick}>Start Wizard</button>
  );
}
```

---

### prefetchDNS

> 원문: https://react.dev/reference/react-dom/prefetchDNS

- 주어진 서버의 IP 주소를 미리 조회하도록 브라우저에 힌트를 제공하는 API임

```js
import { prefetchDNS } from 'react-dom';
```

**시그니처**

```js
prefetchDNS(href)
```

**매개변수**

- `href`: 문자열. 연결할 서버의 URL

**반환값**

- 반환값 없음

**주의사항**

- 같은 서버에 대한 여러 번의 호출은 단일 호출과 동일한 효과임
- 브라우저에서는 렌더링 중, Effect 내부, 이벤트 핸들러 등 어디서든 호출 가능함
- SSR 또는 Server Components 렌더링 시에는 컴포넌트 렌더링 중이거나 렌더링에서 비롯된 async context에서만 효과가 있음
- 필요한 리소스를 알고 있다면 즉시 로딩을 시작하는 다른 함수를 사용할 것
- 웹페이지 자체가 호스팅된 서버에 대한 prefetch는 이미 조회되었으므로 이점이 없음
- `preconnect`와 비교: 대량의 도메인에 투기적으로 연결할 때는 `prefetchDNS`가 더 나을 수 있음. preconnection의 오버헤드가 이점을 상쇄할 수 있기 때문임

**코드 예시**

렌더링 시 DNS prefetch:

```js
import { prefetchDNS } from 'react-dom';

function AppRoot() {
  prefetchDNS("https://example.com");
  return ...;
}
```

이벤트 핸들러에서 DNS prefetch:

```js
import { prefetchDNS } from 'react-dom';

function CallToAction() {
  const onClick = () => {
    prefetchDNS('http://example.com');
    startWizard();
  }
  return (
    <button onClick={onClick}>Start Wizard</button>
  );
}
```

---

### preinit

> 원문: https://react.dev/reference/react-dom/preinit

- 리소스를 다운로드하고 즉시 실행/삽입하는 API임

```js
import { preinit } from 'react-dom';
```

**시그니처**

```js
preinit(href, options)
```

**매개변수**

- `href`: 문자열. 다운로드하고 실행할 리소스의 URL
- `options`: 객체
  - `as`(필수, 문자열): 리소스 타입. 값: `script`, `style`
  - `precedence`(문자열): 스타일시트에 필수. 다른 스타일시트 대비 삽입 순서 제어. 값: `reset`, `low`, `medium`, `high`
  - `crossOrigin`(문자열): CORS 정책. 값: `anonymous`, `use-credentials`
  - `integrity`(문자열): 무결성 검증용 암호화 해시
  - `nonce`(문자열): 엄격한 CSP용 암호화 논스
  - `fetchPriority`(문자열): 상대적 우선순위 힌트. 값: `auto`(기본값), `high`, `low`

**반환값**

- 반환값 없음

**주의사항**

- 같은 `href`로 여러 번 호출해도 단일 호출과 동일한 효과임
- 브라우저에서는 렌더링 중, Effect 내부, 이벤트 핸들러 등 어디서든 호출 가능함
- SSR 또는 Server Components에서는 컴포넌트 렌더링 중이거나 렌더링에서 비롯된 async context에서만 효과가 있음

**동작**

- 스크립트: 다운로드 완료 시 즉시 실행됨
- 스타일시트: 문서에 즉시 삽입됨(바로 적용)
- `precedence`는 스타일시트에 필수이며 문서 내 순서를 제어함

**코드 예시**

외부 스크립트 preinit:

```js
import { preinit } from 'react-dom';

function AppRoot() {
  preinit("https://example.com/script.js", {as: "script"});
  return ...;
}
```

스타일시트 preinit:

```js
import { preinit } from 'react-dom';

function AppRoot() {
  preinit("https://example.com/style.css", {as: "style", precedence: "medium"});
  return ...;
}
```

이벤트 핸들러에서 preinit:

```js
import { preinit } from 'react-dom';

function CallToAction() {
  const onClick = () => {
    preinit("https://example.com/wizardStyles.css", {as: "style"});
    startWizard();
  }
  return (
    <button onClick={onClick}>Start Wizard</button>
  );
}
```

**관련 API**

- 스크립트 다운로드만 하고 실행하지 않으려면 `preload` 사용
- ESM 모듈의 경우 `preinitModule` 사용
- 스타일시트 다운로드만 하고 삽입하지 않으려면 `preload` 사용

---

### preload

> 원문: https://react.dev/reference/react-dom/preload

- 사용할 것으로 예상되는 리소스를 미리 다운로드하는 API임

```js
import { preload } from 'react-dom';
```

**시그니처**

```js
preload(href, options)
```

**매개변수**

- `href`(문자열): 다운로드할 리소스의 URL
- `options`(객체):
  - `as`(필수, 문자열): 리소스 타입. 값: `audio`, `document`, `embed`, `fetch`, `font`, `image`, `object`, `script`, `style`, `track`, `video`, `worker`
  - `crossOrigin`(문자열): CORS 정책. 값: `anonymous`, `use-credentials`. `as: "fetch"`일 때 필수
  - `referrerPolicy`(문자열): Referrer 헤더. 값: `no-referrer-when-downgrade`(기본값), `no-referrer`, `origin`, `origin-when-cross-origin`, `unsafe-url`
  - `integrity`(문자열): 무결성 검증용 암호화 해시
  - `type`(문자열): 리소스의 MIME 타입
  - `nonce`(문자열): 엄격한 CSP용 암호화 논스
  - `fetchPriority`(문자열): 상대적 우선순위. 값: `auto`(기본값), `high`, `low`
  - `imageSrcSet`(문자열): `as: "image"`에서만 사용. 이미지의 소스 세트
  - `imageSizes`(문자열): `as: "image"`에서만 사용. 이미지 크기

**반환값**

- 반환값 없음

**주의사항**

- 동일한 호출의 동등성 규칙:
  - 기본적으로 같은 `href`이면 동등함
  - `as: "image"`인 경우 같은 `href`, `imageSrcSet`, `imageSizes`이면 동등함
- 브라우저에서는 어디서든 호출 가능함
- SSR 또는 Server Components에서는 컴포넌트 렌더링 중이거나 렌더링에서 비롯된 async context에서만 효과가 있음

**코드 예시**

기본 사용:

```js
import { preload } from 'react-dom';

function AppRoot() {
  preload("https://example.com/font.woff2", {as: "font"});
  // ...
}
```

스타일시트와 폰트 함께 preload:

```js
import { preload } from 'react-dom';

function AppRoot() {
  preload("https://example.com/style.css", {as: "style"});
  preload("https://example.com/font.woff2", {as: "font"});
  return ...;
}
```

반응형 이미지 preload:

```js
import { preload } from 'react-dom';

function AppRoot() {
  preload("/banner.png", {
    as: "image",
    imageSrcSet: "/banner512.png 512w, /banner1024.png 1024w",
    imageSizes: "(max-width: 512px) 512px, 1024px",
  });
  return ...;
}
```

이벤트 핸들러에서 preload:

```js
import { preload } from 'react-dom';

function CallToAction() {
  const onClick = () => {
    preload("https://example.com/wizardStyles.css", {as: "style"});
    startWizard();
  }
  return (
    <button onClick={onClick}>Start Wizard</button>
  );
}
```

---

### preloadModule

> 원문: https://react.dev/reference/react-dom/preloadModule

- 사용할 것으로 예상되는 ESM 모듈을 미리 다운로드하는 API임

```js
import { preloadModule } from 'react-dom';
```

**시그니처**

```js
preloadModule(href, options)
```

**매개변수**

- `href`(문자열): 다운로드할 모듈의 URL
- `options`(객체):
  - `as`(필수, 문자열): `'script'`여야 함
  - `crossOrigin`(문자열): CORS 정책. 값: `anonymous`, `use-credentials`
  - `integrity`(문자열): 무결성 검증용 암호화 해시
  - `nonce`(문자열): 엄격한 CSP용 암호화 논스

**반환값**

- 반환값 없음

**주의사항**

- 같은 `href`로 여러 번 호출해도 단일 호출과 동일한 효과임
- 브라우저에서는 어디서든 호출 가능함
- SSR 또는 Server Components에서는 컴포넌트 렌더링 중이거나 렌더링에서 비롯된 async context에서만 효과가 있음

**코드 예시**

렌더링 시:

```js
import { preloadModule } from 'react-dom';

function AppRoot() {
  preloadModule("https://example.com/module.js", {as: "script"});
  return ...;
}
```

이벤트 핸들러에서:

```js
import { preloadModule } from 'react-dom';

function CallToAction() {
  const onClick = () => {
    preloadModule("https://example.com/module.js", {as: "script"});
    startWizard();
  }
  return (
    <button onClick={onClick}>Start Wizard</button>
  );
}
```

**관련 API**: 모듈을 즉시 실행하려면 `preinitModule`, 비-ESM 스크립트는 `preload` 사용

---

### preinitModule

> 원문: https://react.dev/reference/react-dom/preinitModule

- ESM 모듈을 미리 다운로드하고 즉시 평가(실행)하는 API임

```js
import { preinitModule } from 'react-dom';
```

**시그니처**

```js
preinitModule(href, options)
```

**매개변수**

- `href`(문자열): 다운로드하고 실행할 모듈의 URL
- `options`(객체):
  - `as`(필수, 문자열): `'script'`여야 함
  - `crossOrigin`(문자열): CORS 정책. 값: `anonymous`, `use-credentials`
  - `integrity`(문자열): 무결성 검증용 암호화 해시
  - `nonce`(문자열): 엄격한 CSP용 암호화 논스

**반환값**

- 반환값 없음

**주의사항**

- 같은 `href`로 여러 번 호출해도 단일 호출과 동일한 효과임
- 브라우저에서는 어디서든 호출 가능함
- SSR 또는 Server Components에서는 컴포넌트 렌더링 중이거나 렌더링에서 비롯된 async context에서만 효과가 있음

**코드 예시**

렌더링 시:

```js
import { preinitModule } from 'react-dom';

function AppRoot() {
  preinitModule("https://example.com/module.js", {as: "script"});
  return ...;
}
```

이벤트 핸들러에서:

```js
import { preinitModule } from 'react-dom';

function CallToAction() {
  const onClick = () => {
    preinitModule("https://example.com/module.js", {as: "script"});
    startWizard();
  }
  return (
    <button onClick={onClick}>Start Wizard</button>
  );
}
```

**관련 API**: 다운로드만 하고 실행하지 않으려면 `preloadModule`, 비-ESM 스크립트는 `preinit` 사용

---

## react-dom/client APIs

### createRoot

> 원문: https://react.dev/reference/react-dom/client/createRoot

- 브라우저 DOM 노드 안에 React 컴포넌트를 표시하기 위한 루트를 생성하는 API임

```js
import { createRoot } from 'react-dom/client';
```

**시그니처**

```js
const root = createRoot(domNode, options?)
```

**매개변수**

- `domNode`(필수): DOM 엘리먼트. React가 이 DOM 엘리먼트에 대한 루트를 생성함
- `options`(선택): 객체
  - `onCaughtError`: Error Boundary가 에러를 잡았을 때 호출되는 콜백. `error`와 `componentStack`이 포함된 `errorInfo` 객체가 전달됨
  - `onUncaughtError`: 에러가 발생하고 Error Boundary에 잡히지 않았을 때 호출되는 콜백. `error`와 `componentStack`이 포함된 `errorInfo` 객체가 전달됨
  - `onRecoverableError`: React가 에러에서 자동 복구했을 때 호출되는 콜백. 일부 복구 가능한 에러는 `error.cause`에 원래 에러를 포함할 수 있음
  - `identifierPrefix`: `useId`가 생성하는 ID의 접두사 문자열. 같은 페이지에 여러 루트가 있을 때 충돌 방지에 유용함

**반환값**

두 개의 메서드를 가진 객체:

- `root.render(reactNode)`: React 루트의 브라우저 DOM 노드에 JSX를 표시함
  - `reactNode`: 표시할 React 노드(보통 `<App />`과 같은 JSX)
  - 반환값: `undefined`
- `root.unmount()`: React 루트 내부의 렌더 트리를 파괴함
  - 매개변수 없음
  - 반환값: `undefined`

**주의사항**

- 서버 렌더링된 앱에서는 `createRoot()` 대신 `hydrateRoot()` 사용
- 앱에서 `createRoot` 호출은 보통 하나만 있음. 프레임워크 사용 시 프레임워크가 자동으로 호출할 수 있음
- DOM 트리의 다른 위치(자식이 아닌 곳)에 JSX를 렌더링하려면 `createRoot` 대신 `createPortal` 사용

**root.render 주의사항**

- 첫 번째 `root.render` 호출 시 React가 루트 내부의 기존 HTML 콘텐츠를 모두 제거한 후 렌더링함
- 서버에서 생성된 HTML이 있으면 `hydrateRoot()` 사용
- 같은 루트에 `render`를 여러 번 호출하면 React가 이전에 렌더링된 트리와 비교하여 필요한 부분만 DOM을 업데이트함
- `root.render(...)`는 동기적이지 않음. 렌더 이후 코드가 Effect 발생 전에 실행될 수 있음. 필요 시 `flushSync`로 감쌀 것

**root.unmount 주의사항**

- 모든 컴포넌트를 언마운트하고 루트 DOM 노드에서 React를 분리함
- 언마운트 후 같은 루트에서 `root.render`를 다시 호출할 수 없음("Cannot update an unmounted root" 에러). 언마운트 후 같은 DOM 노드에 새 루트를 생성하는 것은 가능함

**코드 예시**

완전한 React 앱 렌더링:

```js
import { createRoot } from 'react-dom/client';
const root = createRoot(document.getElementById('root'));
root.render(<App />);
```

부분적으로 React로 구축된 페이지:

```js
import { createRoot } from 'react-dom/client';

const navDomNode = document.getElementById('navigation');
const navRoot = createRoot(navDomNode);
navRoot.render(<Navigation />);

const commentDomNode = document.getElementById('comments');
const commentRoot = createRoot(commentDomNode);
commentRoot.render(<Comments />);
```

루트 컴포넌트 업데이트:

```js
import { createRoot } from 'react-dom/client';

const root = createRoot(document.getElementById('root'));
let i = 0;
setInterval(() => {
  root.render(<App counter={i} />);
  i++;
}, 1000);
```

에러 처리:

```js
import { createRoot } from "react-dom/client";
import {
  onCaughtErrorProd,
  onRecoverableErrorProd,
  onUncaughtErrorProd,
} from "./reportError";

const container = document.getElementById("root");
const root = createRoot(container, {
  onCaughtError: onCaughtErrorProd,
  onRecoverableError: onRecoverableErrorProd,
  onUncaughtError: onUncaughtErrorProd,
});
root.render(<App />);
```

```js
function reportError({ type, error, errorInfo }) {
  console.error(type, error, "Component Stack: ");
  console.error("Component Stack: ", errorInfo.componentStack);
}
export function onCaughtErrorProd(error, errorInfo) {
  if (error.message !== "Known error") {
    reportError({ type: "Caught", error, errorInfo });
  }
}
export function onUncaughtErrorProd(error, errorInfo) {
  reportError({ type: "Uncaught", error, errorInfo });
}
export function onRecoverableErrorProd(error, errorInfo) {
  reportError({ type: "Recoverable", error, errorInfo });
}
```

**트러블슈팅**

- 아무것도 표시되지 않음: `root.render(<App />)` 호출을 잊은 경우
- "You passed a second argument to root.render": 옵션은 `createRoot(...)`에 전달해야 함
- "Target container is not a DOM element": `createRoot`에 전달한 값이 DOM 노드가 아닌 경우
- "Functions are not valid as a React child": `App` 대신 `<App />`을 전달해야 함
- 서버 렌더링된 HTML이 처음부터 재생성됨: `createRoot` 대신 `hydrateRoot` 사용

---

### hydrateRoot

> 원문: https://react.dev/reference/react-dom/client/hydrateRoot

- 서버에서 렌더링된 HTML에 React를 붙이는(hydrate) API임

```js
import { hydrateRoot } from 'react-dom/client';
```

**시그니처**

```js
const root = hydrateRoot(domNode, reactNode, options?)
```

**매개변수**

- `domNode`: 서버에서 루트 엘리먼트로 렌더링된 DOM 엘리먼트
- `reactNode`: 기존 HTML을 렌더링하는 데 사용된 React 노드. 보통 `renderToPipeableStream(<App />)`으로 렌더링된 `<App />`과 같은 JSX
- `options`(선택): 객체
  - `onCaughtError`: Error Boundary가 에러를 잡았을 때 호출되는 콜백
  - `onUncaughtError`: Error Boundary에 잡히지 않은 에러 발생 시 호출되는 콜백
  - `onRecoverableError`: React가 에러에서 자동 복구했을 때 호출되는 콜백. 일부는 `error.cause`에 원래 에러를 포함함
  - `identifierPrefix`: `useId`가 생성하는 ID의 접두사 문자열. 서버에서 사용한 접두사와 동일해야 함

**반환값**

`render`와 `unmount` 두 메서드를 가진 객체:

- `root.render(reactNode)`: 하이드레이션된 루트 내부의 React 컴포넌트를 업데이트함
  - 하이드레이션 완료 전에 호출하면 기존 서버 렌더링 HTML을 제거하고 클라이언트 렌더링으로 전환함
  - 반환값: `undefined`
- `root.unmount()`: React 루트 내부의 렌더 트리를 파괴함
  - 언마운트 후 같은 루트에서 `root.render`를 다시 호출할 수 없음
  - 반환값: `undefined`

**주의사항**

- `hydrateRoot()`는 렌더링된 콘텐츠가 서버 렌더링 콘텐츠와 동일할 것을 기대함. 불일치를 버그로 취급해야 함
- 개발 모드에서 하이드레이션 중 불일치를 경고함. 속성 차이가 수정되는 것은 보장하지 않음
- 앱에서 `hydrateRoot` 호출은 보통 하나만 있음
- 서버 렌더링 없이 클라이언트에서만 렌더링하는 앱이면 `createRoot()` 사용

**코드 예시**

기본 하이드레이션:

```js
import { hydrateRoot } from 'react-dom/client';
hydrateRoot(document.getElementById('root'), <App />);
```

전체 문서 하이드레이션:

```js
function App() {
  return (
    <html>
      <head>
        <meta charSet="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <link rel="stylesheet" href="/styles.css"></link>
        <title>My app</title>
      </head>
      <body>
        <Router />
      </body>
    </html>
  );
}
```

```js
import { hydrateRoot } from 'react-dom/client';
import App from './App.js';
hydrateRoot(document, <App />);
```

불가피한 하이드레이션 불일치 억제:

```js
export default function App() {
  return (
    <h1 suppressHydrationWarning={true}>
      Current Date: {new Date().toLocaleDateString()}
    </h1>
  );
}
```

- `suppressHydrationWarning`은 한 수준 깊이에서만 동작함. 탈출구 용도로만 사용할 것

클라이언트와 서버의 콘텐츠가 다른 경우(2-pass 렌더링):

```js
import { useState, useEffect } from "react";
export default function App() {
  const [isClient, setIsClient] = useState(false);
  useEffect(() => {
    setIsClient(true);
  }, []);
  return (
    <h1>
      {isClient ? 'Is Client' : 'Is Server'}
    </h1>
  );
}
```

- 이 방법은 컴포넌트가 두 번 렌더링되므로 하이드레이션이 느려짐

하이드레이션된 루트 업데이트:

```js
import { hydrateRoot } from 'react-dom/client';

const root = hydrateRoot(
  document.getElementById('root'),
  <App counter={0} />
);

let i = 0;
setInterval(() => {
  root.render(<App counter={i} />);
  i++;
}, 1000);
```

에러 처리:

```js
import { hydrateRoot } from "react-dom/client";
import App from "./App.js";
import { reportCaughtError } from "./reportError";

const container = document.getElementById("root");
const root = hydrateRoot(container, <App />, {
  onCaughtError: (error, errorInfo) => {
    if (error.message !== "Known error") {
      reportCaughtError({
        error,
        componentStack: errorInfo.componentStack,
      });
    }
  },
});
```

**일반적인 하이드레이션 에러 원인**

- 루트 노드 내부의 React 생성 HTML 주변에 여분의 공백(줄바꿈 등)이 있는 경우
- 렌더링 로직에서 `typeof window !== 'undefined'` 같은 분기를 사용하는 경우
- 렌더링 로직에서 `window.matchMedia` 같은 브라우저 전용 API를 사용하는 경우
- 서버와 클라이언트에서 다른 데이터를 렌더링하는 경우

---

## react-dom/server APIs

### renderToPipeableStream

> 원문: https://react.dev/reference/react-dom/server/renderToPipeableStream

- React 트리를 파이프 가능한 Node.js Stream으로 렌더링하는 API임
- Node.js 전용. Web Streams 환경(Deno, edge runtime)에서는 `renderToReadableStream` 사용

```js
import { renderToPipeableStream } from 'react-dom/server';
```

**시그니처**

```js
const { pipe, abort } = renderToPipeableStream(reactNode, options?)
```

**매개변수**

- `reactNode`(필수): HTML로 렌더링할 React 노드. 예: `<App />`. 전체 문서를 나타내야 하므로 App 컴포넌트가 `<html>` 태그를 렌더링해야 함
- `options`(선택): 객체
  - `bootstrapScriptContent`(문자열): 인라인 `<script>` 태그에 배치될 문자열
  - `bootstrapScripts`(문자열 배열): 페이지에 생성할 `<script>` 태그의 URL 배열. `hydrateRoot`를 호출하는 스크립트 포함용. 클라이언트에서 React를 실행하지 않으려면 생략
  - `bootstrapModules`(문자열 배열): `bootstrapScripts`와 동일하나 `<script type="module">`을 생성
  - `identifierPrefix`(문자열): `useId`가 생성하는 ID의 접두사. 같은 페이지에 여러 루트가 있을 때 충돌 방지용. `hydrateRoot`에 전달한 것과 동일해야 함
  - `namespaceURI`(문자열): 스트림의 루트 namespace URI. 기본값은 HTML. SVG: `'http://www.w3.org/2000/svg'`, MathML: `'http://www.w3.org/1998/Math/MathML'`
  - `nonce`(문자열): `script-src` Content-Security-Policy용 nonce
  - `onAllReady`(콜백): 셸과 추가 콘텐츠를 포함한 모든 렌더링이 완료되었을 때 호출. 크롤러/정적 생성용. 여기서 스트리밍을 시작하면 프로그레시브 로딩 없음
  - `onError`(콜백): 서버 에러 발생 시(복구 가능/불가능 모두) 호출. 기본은 `console.error`만 호출. 커스텀 구현 시에도 `console.error` 유지 필요
  - `onShellReady`(콜백): 초기 셸 렌더링 직후 호출. 여기서 상태 코드 설정 및 `pipe` 호출로 스트리밍 시작
  - `onShellError`(콜백): 초기 셸 렌더링 중 에러 시 호출. 아직 바이트가 전송되지 않은 상태이므로 폴백 HTML 셸 출력 가능
  - `progressiveChunkSize`(숫자): 청크 바이트 수

**반환값**

두 메서드를 가진 객체:

- `pipe`: Writable Node.js Stream에 HTML 출력. `onShellReady`에서 호출하면 스트리밍, `onAllReady`에서 호출하면 크롤러/정적 생성용
- `abort`: 서버 렌더링을 중단하고 나머지를 클라이언트에서 렌더링

**코드 예시**

기본 사용:

```js
import { renderToPipeableStream } from 'react-dom/server';

app.use('/', (request, response) => {
  const { pipe } = renderToPipeableStream(<App />, {
    bootstrapScripts: ['/main.js'],
    onShellReady() {
      response.setHeader('content-type', 'text/html');
      pipe(response);
    }
  });
});
```

App 컴포넌트:

```js
export default function App() {
  return (
    <html>
      <head>
        <meta charSet="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <link rel="stylesheet" href="/styles.css"></link>
        <title>My app</title>
      </head>
      <body>
        <Router />
      </body>
    </html>
  );
}
```

클라이언트 하이드레이션:

```js
import { hydrateRoot } from 'react-dom/client';
import App from './App.js';

hydrateRoot(document, <App />);
```

Suspense 스트리밍:

```js
function ProfilePage() {
  return (
    <ProfileLayout>
      <ProfileCover />
      <Sidebar>
        <Friends />
        <Photos />
      </Sidebar>
      <Suspense fallback={<PostsGlimmer />}>
        <Posts />
      </Suspense>
    </ProfileLayout>
  );
}
```

에셋 경로 전달:

```js
const assetMap = {
  'styles.css': '/styles.123456.css',
  'main.js': '/main.123456.js'
};

app.use('/', (request, response) => {
  const { pipe } = renderToPipeableStream(<App assetMap={assetMap} />, {
    bootstrapScriptContent: `window.assetMap = ${JSON.stringify(assetMap)};`,
    bootstrapScripts: [assetMap['main.js']],
    onShellReady() {
      response.setHeader('content-type', 'text/html');
      pipe(response);
    }
  });
});
```

셸 에러 복구:

```js
const { pipe } = renderToPipeableStream(<App />, {
  bootstrapScripts: ['/main.js'],
  onShellReady() {
    response.setHeader('content-type', 'text/html');
    pipe(response);
  },
  onShellError(error) {
    response.statusCode = 500;
    response.setHeader('content-type', 'text/html');
    response.send('<h1>Something went wrong</h1>');
  },
  onError(error) {
    console.error(error);
    logServerCrashReport(error);
  }
});
```

**셸 외부 에러 복구 동작**

- 가장 가까운 `<Suspense>` 경계의 로딩 폴백을 HTML로 출력함
- 서버에서 해당 콘텐츠 렌더링을 포기함
- 클라이언트에서 JavaScript 로드 후 렌더링 재시도함

에러 기반 상태 코드 설정:

```js
let didError = false;

const { pipe } = renderToPipeableStream(<App />, {
  bootstrapScripts: ['/main.js'],
  onShellReady() {
    response.statusCode = didError ? 500 : 200;
    response.setHeader('content-type', 'text/html');
    pipe(response);
  },
  onShellError(error) {
    response.statusCode = 500;
    response.setHeader('content-type', 'text/html');
    response.send('<h1>Something went wrong</h1>');
  },
  onError(error) {
    didError = true;
    console.error(error);
    logServerCrashReport(error);
  }
});
```

에러 유형별 상태 코드:

```js
let didError = false;
let caughtError = null;

function getStatusCode() {
  if (didError) {
    if (caughtError instanceof NotFoundError) {
      return 404;
    } else {
      return 500;
    }
  } else {
    return 200;
  }
}

const { pipe } = renderToPipeableStream(<App />, {
  bootstrapScripts: ['/main.js'],
  onShellReady() {
    response.statusCode = getStatusCode();
    response.setHeader('content-type', 'text/html');
    pipe(response);
  },
  onShellError(error) {
   response.statusCode = getStatusCode();
   response.setHeader('content-type', 'text/html');
   response.send('<h1>Something went wrong</h1>');
  },
  onError(error) {
    didError = true;
    caughtError = error;
    console.error(error);
    logServerCrashReport(error);
  }
});
```

크롤러/정적 생성용 전체 콘텐츠 대기:

```js
let didError = false;
let isCrawler = // ... depends on your bot detection strategy ...

const { pipe } = renderToPipeableStream(<App />, {
  bootstrapScripts: ['/main.js'],
  onShellReady() {
    if (!isCrawler) {
      response.statusCode = didError ? 500 : 200;
      response.setHeader('content-type', 'text/html');
      pipe(response);
    }
  },
  onShellError(error) {
    response.statusCode = 500;
    response.setHeader('content-type', 'text/html');
    response.send('<h1>Something went wrong</h1>');
  },
  onAllReady() {
    if (isCrawler) {
      response.statusCode = didError ? 500 : 200;
      response.setHeader('content-type', 'text/html');
      pipe(response);
    }
  },
  onError(error) {
    didError = true;
    console.error(error);
    logServerCrashReport(error);
  }
});
```

서버 렌더링 중단:

```js
const { pipe, abort } = renderToPipeableStream(<App />, {
  // ...
});

setTimeout(() => {
  abort();
}, 10000);
```

---

### renderToReadableStream

> 원문: https://react.dev/reference/react-dom/server/renderToReadableStream

- React 트리를 Readable Web Stream으로 렌더링하는 API임
- Web Streams 기반 API. Node.js에서는 `renderToPipeableStream` 사용

```js
import { renderToReadableStream } from 'react-dom/server';
```

**시그니처**

```js
const stream = await renderToReadableStream(reactNode, options?)
```

**매개변수**

- `reactNode`: HTML로 렌더링할 React 노드. App 컴포넌트가 `<html>` 태그를 렌더링해야 함
- `options`(선택): 객체
  - `bootstrapScriptContent`(문자열): 인라인 `<script>` 태그에 배치될 문자열
  - `bootstrapScripts`(문자열 배열): `<script>` 태그의 URL 배열
  - `bootstrapModules`(문자열 배열): `<script type="module">` 생성
  - `identifierPrefix`(문자열): `useId` ID 접두사
  - `namespaceURI`(문자열): 루트 namespace URI
  - `nonce`(문자열): CSP용 nonce
  - `onError`(함수): 서버 에러 콜백
  - `progressiveChunkSize`(숫자): 청크 바이트 수
  - `signal`(AbortSignal): 서버 렌더링 중단용 abort signal

**반환값**

Promise:

- 성공: Readable Web Stream으로 resolve됨
- 실패: 셸 렌더링 실패 시 reject됨

**Stream 속성**

- `allReady`(Promise): 모든 렌더링 완료 시 resolve됨. 크롤러/정적 생성용으로 `await stream.allReady` 후 응답 반환 시 프로그레시브 로딩 없이 최종 HTML 포함

**주요 참고사항**

- Suspense 경계를 활성화하는 소스에서 읽은 데이터만 서스펜드함. Effect나 이벤트 핸들러에서 fetch한 데이터는 감지 안 됨
- 셸 = `<Suspense>` 경계 밖의 앱 부분
- 셸 에러: `onError`와 `catch` 모두 호출됨
- 셸 외부 에러: `onError`만 호출됨, 클라이언트에서 복구 시도함
- 스트리밍 시작 후 상태 코드 변경 불가

**코드 예시**

기본 사용:

```js
import { renderToReadableStream } from 'react-dom/server';

async function handler(request) {
  const stream = await renderToReadableStream(<App />, {
    bootstrapScripts: ['/main.js']
  });
  return new Response(stream, {
    headers: { 'content-type': 'text/html' },
  });
}
```

셸 에러 복구:

```js
async function handler(request) {
  try {
    const stream = await renderToReadableStream(<App />, {
      bootstrapScripts: ['/main.js'],
      onError(error) {
        console.error(error);
        logServerCrashReport(error);
      }
    });
    return new Response(stream, {
      headers: { 'content-type': 'text/html' },
    });
  } catch (error) {
    return new Response('<h1>Something went wrong</h1>', {
      status: 500,
      headers: { 'content-type': 'text/html' },
    });
  }
}
```

에러 기반 상태 코드:

```js
async function handler(request) {
  try {
    let didError = false;
    const stream = await renderToReadableStream(<App />, {
      bootstrapScripts: ['/main.js'],
      onError(error) {
        didError = true;
        console.error(error);
        logServerCrashReport(error);
      }
    });
    return new Response(stream, {
      status: didError ? 500 : 200,
      headers: { 'content-type': 'text/html' },
    });
  } catch (error) {
    return new Response('<h1>Something went wrong</h1>', {
      status: 500,
      headers: { 'content-type': 'text/html' },
    });
  }
}
```

크롤러/정적 생성:

```js
async function handler(request) {
  try {
    let didError = false;
    const stream = await renderToReadableStream(<App />, {
      bootstrapScripts: ['/main.js'],
      onError(error) {
        didError = true;
        console.error(error);
        logServerCrashReport(error);
      }
    });
    let isCrawler = // ... depends on your bot detection strategy ...
    if (isCrawler) {
      await stream.allReady;
    }
    return new Response(stream, {
      status: didError ? 500 : 200,
      headers: { 'content-type': 'text/html' },
    });
  } catch (error) {
    return new Response('<h1>Something went wrong</h1>', {
      status: 500,
      headers: { 'content-type': 'text/html' },
    });
  }
}
```

서버 렌더링 중단:

```js
async function handler(request) {
  try {
    const controller = new AbortController();
    setTimeout(() => {
      controller.abort();
    }, 10000);

    const stream = await renderToReadableStream(<App />, {
      signal: controller.signal,
      bootstrapScripts: ['/main.js'],
      onError(error) {
        didError = true;
        console.error(error);
        logServerCrashReport(error);
      }
    });
    // ...
  }
}
```

---

### renderToStaticMarkup

> 원문: https://react.dev/reference/react-dom/server/renderToStaticMarkup

- 비인터랙티브 React 트리를 HTML 문자열로 렌더링하는 API임
- 하이드레이션 불가. 정적 페이지 생성기나 이메일 등 완전히 정적인 콘텐츠에 유용함

```js
import { renderToStaticMarkup } from 'react-dom/server';
```

**시그니처**

```js
const html = renderToStaticMarkup(reactNode, options?)
```

**매개변수**

- `reactNode`: HTML로 렌더링할 React 노드. 예: `<Page />`
- `options`(선택): 객체
  - `identifierPrefix`(문자열): `useId`가 생성하는 ID의 접두사

**반환값**

- HTML 문자열

**주의사항**

- `renderToStaticMarkup` 출력은 하이드레이션 불가
- 제한된 Suspense 지원. 컴포넌트 서스펜드 시 즉시 폴백을 HTML로 전송
- 브라우저에서 동작하지만 클라이언트 코드에서의 사용은 비권장
- 인터랙티브 앱은 서버에서 `renderToString` + 클라이언트에서 `hydrateRoot` 사용

**코드 예시**

기본 사용:

```js
import { renderToStaticMarkup } from 'react-dom/server';

const html = renderToStaticMarkup(<Page />);
```

서버 라우트 핸들러:

```js
import { renderToStaticMarkup } from 'react-dom/server';

app.use('/', (request, response) => {
  const html = renderToStaticMarkup(<Page />);
  response.send(html);
});
```

---

### renderToString

> 원문: https://react.dev/reference/react-dom/server/renderToString

- React 트리를 HTML 문자열로 렌더링하는 API임
- 스트리밍 미지원. 즉시 문자열을 반환함

```js
import { renderToString } from 'react-dom/server';
```

**시그니처**

```js
const html = renderToString(reactNode, options?)
```

**매개변수**

- `reactNode`: HTML로 렌더링할 React 노드. 예: `<App />`
- `options`(선택): 객체
  - `identifierPrefix`(문자열): `useId`가 생성하는 ID의 접두사. `hydrateRoot`에 전달한 것과 동일해야 함

**반환값**

- HTML 문자열

**주의사항**

- 제한된 Suspense 지원. 컴포넌트 서스펜드 시 즉시 폴백을 HTML로 전송
- 브라우저에서 동작하지만 클라이언트 코드에서의 사용은 비권장
- 스트리밍 미지원: 즉시 문자열 반환
- 데이터 대기 미지원: 정적 HTML 생성을 위한 데이터 로드 대기 불가
- `lazy` 또는 데이터 fetch 컴포넌트 서스펜드 시, 가장 가까운 `<Suspense>` 경계의 `fallback` prop을 HTML로 렌더링

**코드 예시**

기본 사용:

```js
import { renderToString } from 'react-dom/server';

const html = renderToString(<App />);
```

서버 라우트 핸들러:

```js
import { renderToString } from 'react-dom/server';

app.use('/', (request, response) => {
  const html = renderToString(<App />);
  response.send(html);
});
```

클라이언트 사용(비권장):

```js
import { renderToString } from 'react-dom/server';

const html = renderToString(<MyIcon />);
console.log(html); // For example, "<svg>...</svg>"
```

권장 클라이언트 대안:

```js
import { createRoot } from 'react-dom/client';
import { flushSync } from 'react-dom';

const div = document.createElement('div');
const root = createRoot(div);
flushSync(() => {
  root.render(<MyIcon />);
});
console.log(div.innerHTML); // For example, "<svg>...</svg>"
```

**권장 대안**

- 스트리밍(Node.js): `renderToPipeableStream`
- 스트리밍(Deno/Edge): `renderToReadableStream`
- 정적 프리렌더링(Node.js): `prerenderToNodeStream`
- 정적 프리렌더링(Deno/Edge): `prerender`
- 클라이언트에서 `react-dom/server` 임포트 시 불필요한 번들 크기가 증가함

---

## react-dom/static APIs

### prerender

> 원문: https://react.dev/reference/react-dom/static/prerender

- React 트리를 정적 HTML로 렌더링하는 API임. Web Stream 기반
- 정적 서버 사이드 생성(SSG)용으로 설계됨. `renderToString`과 달리 모든 데이터 로드를 기다린 후 resolve함
- Web Streams 의존. Node.js에서는 `prerenderToNodeStream` 사용

```js
import { prerender } from 'react-dom/static';
```

**시그니처**

```js
const {prelude, postponed} = await prerender(reactNode, options?)
```

**매개변수**

- `reactNode`: HTML로 렌더링할 React 노드. 예: `<App />`. 전체 문서를 나타내야 하므로 App 컴포넌트가 `<html>` 태그를 렌더링해야 함
- `options`(선택): 정적 생성 옵션 객체
  - `bootstrapScriptContent`(선택, 문자열): 인라인 `<script>` 태그에 배치될 문자열
  - `bootstrapScripts`(선택, 문자열 배열): `<script>` 태그의 URL 배열. `hydrateRoot`를 호출하는 스크립트 포함용
  - `bootstrapModules`(선택, 문자열 배열): `<script type="module">` 생성
  - `identifierPrefix`(선택, 문자열): `useId`가 생성하는 ID의 접두사. `hydrateRoot`에 전달한 것과 동일해야 함
  - `namespaceURI`(선택, 문자열): 루트 namespace URI. 기본값은 HTML. SVG: `'http://www.w3.org/2000/svg'`, MathML: `'http://www.w3.org/1998/Math/MathML'`
  - `onError`(선택, 함수): 서버 에러 콜백. 기본값: `console.error`
  - `progressiveChunkSize`(선택, 숫자): 청크 바이트 수
  - `signal`(선택, AbortSignal): 프리렌더링 중단용 abort signal

**반환값**

다음을 포함하는 객체로 resolve되는 Promise:

- `prelude`: HTML의 Web Stream. 응답을 청크로 보내거나 전체 스트림을 문자열로 읽을 수 있음
- `postponed`: `prerender`가 완료되지 않은 경우 `resume`에 전달할 수 있는 JSON 직렬화 가능한 불투명 객체. 모든 콘텐츠가 포함된 경우 `null`

렌더링 실패 시 Promise가 reject됨

**주의사항**

- `nonce`는 사용 불가. nonce는 요청별로 고유해야 하므로 프리렌더에 포함하는 것은 보안상 위험함

**코드 예시**

기본 사용:

```js
import { prerender } from 'react-dom/static';

async function handler(request) {
  const {prelude} = await prerender(<App />, {
    bootstrapScripts: ['/main.js']
  });
  return new Response(prelude, {
    headers: { 'content-type': 'text/html' },
  });
}
```

문자열로 렌더링:

```js
import { prerender } from 'react-dom/static';

async function renderToString() {
  const {prelude} = await prerender(<App />, {
    bootstrapScripts: ['/main.js']
  });

  const reader = prelude.getReader();
  let content = '';
  while (true) {
    const {done, value} = await reader.read();
    if (done) {
      return content;
    }
    content += Buffer.from(value).toString('utf8');
  }
}
```

에셋 맵 사용:

```js
const assetMap = {
  'styles.css': '/styles.123456.css',
  'main.js': '/main.123456.js'
};

async function handler(request) {
  const {prelude} = await prerender(<App assetMap={assetMap} />, {
    bootstrapScriptContent: `window.assetMap = ${JSON.stringify(assetMap)};`,
    bootstrapScripts: [assetMap['/main.js']],
  });
  return new Response(prelude, {
    headers: { 'content-type': 'text/html' },
  });
}
```

프리렌더링 중단:

```js
async function renderToString() {
  const controller = new AbortController();
  setTimeout(() => {
    controller.abort()
  }, 10000);

  try {
    const {prelude} = await prerender(<App />, {
      signal: controller.signal,
    });
    //...
  }
}
```

- 중단 시 미완료 Suspense 경계의 자식은 폴백 상태로 prelude에 포함됨
- `resume` 또는 `resumeAndPrerender`를 사용한 부분 프리렌더링에 활용 가능

---

### prerenderToNodeStream

> 원문: https://react.dev/reference/react-dom/static/prerenderToNodeStream

- React 트리를 정적 HTML로 렌더링하는 Node.js Stream 기반 API임
- 정적 서버 사이드 생성(SSG)용으로 설계됨. `renderToString`과 달리 모든 데이터 로드를 기다린 후 resolve함
- Node.js 전용. Web Streams 환경(Deno, edge runtime)에서는 `prerender` 사용

```js
import { prerenderToNodeStream } from 'react-dom/static';
```

**시그니처**

```js
const {prelude, postponed} = await prerenderToNodeStream(reactNode, options?)
```

**매개변수**

- `reactNode`(필수): HTML로 렌더링할 React 노드. 전체 문서를 나타내야 하므로 App 컴포넌트가 `<html>` 태그를 렌더링해야 함
- `options`(선택): 정적 생성 옵션 객체
  - `bootstrapScriptContent`(선택, 문자열): 인라인 `<script>` 태그에 배치될 문자열
  - `bootstrapScripts`(선택, 문자열 배열): `<script>` 태그의 URL 배열
  - `bootstrapModules`(선택, 문자열 배열): `<script type="module">` 생성
  - `identifierPrefix`(선택, 문자열): `useId`가 생성하는 ID의 접두사. `hydrateRoot`에 전달한 것과 동일해야 함
  - `namespaceURI`(선택, 문자열): 루트 namespace URI. 기본값은 HTML. SVG: `'http://www.w3.org/2000/svg'`, MathML: `'http://www.w3.org/1998/Math/MathML'`
  - `onError`(선택, 함수): 서버 에러 콜백. 기본값: `console.error`
  - `progressiveChunkSize`(선택, 숫자): 청크 바이트 수
  - `signal`(선택, AbortSignal): 프리렌더링 중단용 abort signal

**반환값**

다음을 포함하는 객체로 resolve되는 Promise:

- `prelude`: HTML의 Node.js Stream. 응답에 pipe하거나 문자열로 읽을 수 있음
- `postponed`: 완료되지 않은 경우 `resumeToPipeableStream`에 전달할 수 있는 JSON 직렬화 가능한 불투명 객체. 모든 콘텐츠가 포함된 경우 `null`

렌더링 실패 시 Promise가 reject됨

**주의사항**

- `nonce`는 사용 불가. nonce는 요청별로 고유해야 하므로 프리렌더에 포함하는 것은 보안상 위험함
- Suspense 경계를 활성화하는 소스에서 읽은 데이터만 서스펜드함. Effect나 이벤트 핸들러에서 fetch한 데이터는 감지 안 됨

**코드 예시**

기본 사용:

```js
import { prerenderToNodeStream } from 'react-dom/static';

app.use('/', async (request, response) => {
  const { prelude } = await prerenderToNodeStream(<App />, {
    bootstrapScripts: ['/main.js'],
  });

  response.setHeader('Content-Type', 'text/plain');
  prelude.pipe(response);
});
```

클라이언트 하이드레이션:

```js
import { hydrateRoot } from 'react-dom/client';
import App from './App.js';

hydrateRoot(document, <App />);
```

문자열로 렌더링:

```js
import { prerenderToNodeStream } from 'react-dom/static';

async function renderToString() {
  const {prelude} = await prerenderToNodeStream(<App />, {
    bootstrapScripts: ['/main.js']
  });

  return new Promise((resolve, reject) => {
    let data = '';
    prelude.on('data', chunk => {
      data += chunk;
    });
    prelude.on('end', () => resolve(data));
    prelude.on('error', reject);
  });
}
```

에셋 맵 사용:

```js
const assetMap = {
  'styles.css': '/styles.123456.css',
  'main.js': '/main.123456.js'
};

app.use('/', async (request, response) => {
  const { prelude } = await prerenderToNodeStream(<App />, {
    bootstrapScriptContent: `window.assetMap = ${JSON.stringify(assetMap)};`,
    bootstrapScripts: [assetMap['/main.js']],
  });

  response.setHeader('Content-Type', 'text/html');
  prelude.pipe(response);
});
```

프리렌더링 중단:

```js
async function renderToString() {
  const controller = new AbortController();
  setTimeout(() => {
    controller.abort()
  }, 10000);

  try {
    const {prelude} = await prerenderToNodeStream(<App />, {
      signal: controller.signal,
    });
    //...
  }
}
```

- 중단 시 미완료 Suspense 경계는 폴백 상태로 포함됨
- `resumeToPipeableStream` 또는 `resumeAndPrerenderToNodeStream`으로 부분 프리렌더링 활용 가능

**트러블슈팅: Stream이 전체 앱 렌더링 전까지 시작되지 않음**

- `prerenderToNodeStream`은 모든 Suspense 경계를 포함한 전체 앱 렌더링이 완료될 때까지 대기함
- SSG용으로 설계되었으며 스트리밍을 지원하지 않음
- 스트리밍이 필요하면 `renderToPipeableStream` 사용

---

## react-dom/components (메타데이터 컴포넌트)

### `<link>`

> 원문: https://react.dev/reference/react-dom/components/link

- React의 내장 `<link>` 컴포넌트. HTML `<link>` 엘리먼트의 래퍼로, 외부 리소스(스타일시트 등)를 사용하거나 문서에 링크 메타데이터를 추가할 수 있음

```js
<link rel="icon" href="favicon.ico" />
```

**Props**

공통 Props:

- `rel`(문자열, 필수): 리소스와의 관계 지정
- `href`(문자열): 링크된 리소스 URL
- `crossOrigin`(문자열): CORS 정책. 값: `anonymous`, `use-credentials`. `as="fetch"`일 때 필수
- `referrerPolicy`(문자열): Referrer 헤더. 값: `no-referrer-when-downgrade`(기본값), `no-referrer`, `origin`, `origin-when-cross-origin`, `unsafe-url`
- `fetchPriority`(문자열): 상대적 우선순위. 값: `auto`(기본값), `high`, `low`
- `hrefLang`(문자열): 링크된 리소스의 언어
- `integrity`(문자열): 리소스의 암호화 해시(무결성 검증)
- `type`(문자열): 링크된 리소스의 MIME 타입

`rel="stylesheet"` Props:

- `precedence`(문자열): `<head>` 내 스타일시트의 상대적 순서. React는 먼저 발견된 값을 "lower", 나중 값을 "higher"로 추론. 같은 precedence 값의 스타일시트는 함께 그룹화됨
- `media`(문자열): 미디어 쿼리로 스타일시트 제한
- `title`(문자열): 대체 스타일시트 이름

`rel="stylesheet"` 특수 동작 비활성화 Props:

- `disabled`(boolean): 스타일시트 비활성화
- `onError`(함수): 스타일시트 로드 실패 시 호출
- `onLoad`(함수): 스타일시트 로드 완료 시 호출

`rel="preload"` 또는 `rel="modulepreload"` Props:

- `as`(문자열): 리소스 타입. 값: `audio`, `document`, `embed`, `fetch`, `font`, `image`, `object`, `script`, `style`, `track`, `video`, `worker`
- `imageSrcSet`(문자열): `as="image"`일 때만 사용. 이미지의 소스 세트
- `imageSizes`(문자열): `as="image"`일 때만 사용. 이미지 크기

`rel="icon"` 또는 `rel="apple-touch-icon"` Props:

- `sizes`(문자열): 아이콘 크기

비권장 Props:

- `blocking`(문자열): `"render"` 설정 시 스타일시트 로드까지 렌더 차단. React는 Suspense로 더 세밀한 제어를 제공함

**특수 렌더링 동작**

- React는 React 트리 내 어디에서 렌더링하든 `<link>` DOM 엘리먼트를 항상 `<head>`에 배치함
- 예외: `rel="stylesheet"`인 경우 `precedence` prop 필수(없으면 특수 동작 없음)
- 예외: `itemProp` prop이 있으면 특수 동작 없음(문서가 아닌 특정 항목에 대한 메타데이터)
- 예외: `onLoad`/`onError` prop이 있으면 특수 동작 없음

**스타일시트 특수 동작** (`rel="stylesheet"` + `precedence` prop 제공 시):

- 스타일시트 로딩 중 해당 컴포넌트가 Suspend됨
- 같은 `href`의 중복 링크 자동 제거(하나만 DOM에 삽입)
- 렌더링 후 props 변경 무시됨(개발 모드에서 경고)
- 렌더링한 컴포넌트가 언마운트되어도 link가 DOM에 남을 수 있음

**코드 예시**

관련 리소스 링크:

```js
import ShowRenderedHTML from './ShowRenderedHTML.js';

export default function BlogPage() {
  return (
    <ShowRenderedHTML>
      <link rel="icon" href="favicon.ico" />
      <link rel="pingback" href="http://www.example.com/xmlrpc.php" />
      <h1>My Blog</h1>
      <p>...</p>
    </ShowRenderedHTML>
  );
}
```

스타일시트 링크:

```js
import ShowRenderedHTML from './ShowRenderedHTML.js';

export default function SiteMapPage() {
  return (
    <ShowRenderedHTML>
      <link rel="stylesheet" href="sitemap.css" precedence="medium" />
      <p>...</p>
    </ShowRenderedHTML>
  );
}
```

스타일시트 우선순위 제어:

```js
import ShowRenderedHTML from './ShowRenderedHTML.js';

export default function HomePage() {
  return (
    <ShowRenderedHTML>
      <FirstComponent />
      <SecondComponent />
      <ThirdComponent/>
      ...
    </ShowRenderedHTML>
  );
}

function FirstComponent() {
  return <link rel="stylesheet" href="first.css" precedence="first" />;
}

function SecondComponent() {
  return <link rel="stylesheet" href="second.css" precedence="second" />;
}

function ThirdComponent() {
  return <link rel="stylesheet" href="third.css" precedence="first" />;
}
```

중복 스타일시트 제거:

```js
import ShowRenderedHTML from './ShowRenderedHTML.js';

export default function HomePage() {
  return (
    <ShowRenderedHTML>
      <Component />
      <Component />
      ...
    </ShowRenderedHTML>
  );
}

function Component() {
  return <link rel="stylesheet" href="styles.css" precedence="medium" />;
}
```

---

### `<meta>`

> 원문: https://react.dev/reference/react-dom/components/meta

- React의 내장 `<meta>` 컴포넌트. 문서에 메타데이터를 추가할 수 있음

```js
<meta name="keywords" content="React, JavaScript, semantic markup, html" />
```

**Props**

`name`, `charset`, `httpEquiv`, `itemProp` 중 정확히 하나가 필수:

- `name`(문자열): 문서에 첨부할 메타데이터 종류 지정
- `charset`(문자열): 문서 문자셋 지정. 유효한 값은 `"utf-8"`만 가능
- `httpEquiv`(문자열): 문서 처리 지시문 지정
- `itemProp`(문자열): 문서 전체가 아닌 특정 항목에 대한 메타데이터 지정
- `content`(문자열): `name` 또는 `itemProp`과 함께 사용 시 첨부할 메타데이터, `httpEquiv`와 함께 사용 시 지시문의 동작 지정

**특수 렌더링 동작**

- React는 `<meta>` 컴포넌트를 항상 `<head>`에 배치함
- 예외: `itemProp` prop이 있으면 특수 동작 없음(인라인 렌더링)

**코드 예시**

문서 메타데이터:

```js
import ShowRenderedHTML from './ShowRenderedHTML.js';

export default function SiteMapPage() {
  return (
    <ShowRenderedHTML>
      <meta name="keywords" content="React" />
      <meta name="description" content="A site map for the React website" />
      <h1>Site Map</h1>
      <p>...</p>
    </ShowRenderedHTML>
  );
}
```

특정 항목 주석:

```js
<section itemScope>
  <h3>Annotating specific items</h3>
  <meta itemProp="description" content="API reference for using <meta> with itemProp" />
  <p>...</p>
</section>
```

---

### `<script>`

> 원문: https://react.dev/reference/react-dom/components/script

- React의 내장 `<script>` 컴포넌트. 문서에 인라인 또는 외부 스크립트를 추가할 수 있음
- 특정 조건에서 자동으로 `<head>`에 배치하고 동일 스크립트를 중복 제거함

```js
<script> alert("hi!") </script>
<script src="script.js" />
```

**Props**

필수(둘 중 하나):

- `children`(문자열): 인라인 스크립트 소스 코드
- `src`(문자열): 외부 스크립트 URL

선택:

- `async`(boolean): 나머지 문서 처리 후까지 실행 지연 허용(성능에 권장)
- `crossOrigin`(문자열): CORS 정책. 값: `anonymous`, `use-credentials`
- `fetchPriority`(문자열): 여러 스크립트 동시 패칭 시 우선순위. 값: `"high"`, `"low"`, `"auto"`(기본값)
- `integrity`(문자열): 스크립트 무결성 검증용 암호화 해시
- `noModule`(boolean): ES 모듈 지원 브라우저에서 스크립트 비활성화(폴백용)
- `nonce`(문자열): 엄격한 CSP에서 리소스 허용하는 암호화 논스
- `referrer`(문자열): Referer 헤더 지정
- `type`(문자열): 클래식 스크립트, ES 모듈, import map 여부 지정

특수 처리 비활성화 Props:

- `onError`(함수): 스크립트 로드 실패 시 호출
- `onLoad`(함수): 스크립트 로드 완료 시 호출

비권장 Props:

- `blocking`(문자열): `"render"` 시 스크립트 로드까지 렌더 차단. React는 Suspense로 더 세밀한 제어 제공
- `defer`(문자열): 문서 로드 완료까지 실행 방지. 스트리밍 SSR과 호환 안 됨. `async` 사용 권장

**특수 렌더링 동작** (`src` + `async={true}` 제공 시):

- React가 `<script>`를 `<head>`로 이동 가능
- 같은 `src`의 스크립트 중복 제거
- 렌더링 후 props 변경 무시됨
- 컴포넌트 언마운트 후에도 script가 DOM에 남을 수 있음(스크립트는 삽입 시 한 번만 실행되므로 영향 없음)

**코드 예시**

외부 스크립트 렌더링:

```js
import ShowRenderedHTML from './ShowRenderedHTML.js';

function Map({lat, long}) {
  return (
    <>
      <script async src="map-api.js" />
      <div id="map" data-lat={lat} data-long={long} />
    </>
  );
}

export default function Page() {
  return (
    <ShowRenderedHTML>
      <Map />
    </ShowRenderedHTML>
  );
}
```

인라인 스크립트 렌더링:

```js
import ShowRenderedHTML from './ShowRenderedHTML.js';

function Tracking() {
  return (
    <script>
      ga('send', 'pageview');
    </script>
  );
}

export default function Page() {
  return (
    <ShowRenderedHTML>
      <h1>My Website</h1>
      <Tracking />
      <p>Welcome</p>
    </ShowRenderedHTML>
  );
}
```

---

### `<style>`

> 원문: https://react.dev/reference/react-dom/components/style

- React의 내장 `<style>` 컴포넌트. 문서에 인라인 CSS 스타일시트를 추가할 수 있음

```js
<style>{` p { color: red; } `}</style>
```

**Props**

- `children`(문자열, 필수): 스타일시트 내용
- `precedence`(문자열): `<head>` 내 `<style>` DOM 노드의 상대적 순서. 먼저 발견된 값이 "lower", 나중 값이 "higher". 같은 precedence의 스타일시트(`<link>`, 인라인 `<style>`, `preinit` 함수로 로드)는 함께 그룹화됨
- `href`(문자열): 같은 `href`를 가진 스타일 중복 제거
- `media`(문자열): 미디어 쿼리로 스타일시트 제한
- `nonce`(문자열): 엄격한 CSP에서 리소스 허용하는 암호화 논스
- `title`(문자열): 대체 스타일시트 이름

비권장 Props:

- `blocking`(문자열): `"render"` 시 스타일시트 로드까지 렌더 차단. React는 Suspense로 더 세밀한 제어 제공

**특수 렌더링 동작** (`href` + `precedence` prop 제공 시):

- React가 `<style>`을 `<head>`로 이동함
- 같은 `href`의 스타일 중복 제거
- 로딩 중 Suspend
- 렌더링 후 props 변경 무시됨
- `precedence` prop 사용 시 `href`와 `precedence` 외의 불필요한 props 제거
- 컴포넌트 언마운트 후에도 style이 DOM에 남을 수 있음

**코드 예시**

```js
import ShowRenderedHTML from './ShowRenderedHTML.js';
import { useId } from 'react';

function PieChart({data, colors}) {
  const id = useId();
  const stylesheet = colors.map((color, index) =>
    `#${id} .color-${index}: \{ color: "${color}"; \}`
  ).join();
  return (
    <>
      <style href={"PieChart-" + JSON.stringify(colors)} precedence="medium">
        {stylesheet}
      </style>
      <svg id={id}>
        …
      </svg>
    </>
  );
}

export default function App() {
  return (
    <ShowRenderedHTML>
      <PieChart data="..." colors={['red', 'green', 'blue']} />
    </ShowRenderedHTML>
  );
}
```

---

### `<title>`

> 원문: https://react.dev/reference/react-dom/components/title

- React의 내장 `<title>` 컴포넌트. 문서 제목을 지정할 수 있음

```js
<title>My Blog</title>
```

**Props**

- `children`: 텍스트만 허용. 이 텍스트가 문서 제목이 됨. 텍스트만 렌더링하는 커스텀 컴포넌트도 전달 가능

**특수 렌더링 동작**

- React는 React 트리 내 어디에서 렌더링하든 `<title>` DOM 엘리먼트를 항상 `<head>`에 배치함
- 예외: `<svg>` 내부의 `<title>`은 특수 동작 없음(SVG 접근성 주석)
- 예외: `itemProp` prop이 있으면 특수 동작 없음

**주의사항**

- 한 번에 하나의 `<title>`만 렌더링할 것. 여러 컴포넌트가 동시에 `<title>`을 렌더링하면 모두 `<head>`에 들어가며, 브라우저와 검색 엔진의 동작이 정의되지 않음

**코드 예시**

문서 제목 설정:

```js
import ShowRenderedHTML from './ShowRenderedHTML.js';

export default function ContactUsPage() {
  return (
    <ShowRenderedHTML>
      <title>My Site: Contact Us</title>
      <h1>Contact Us</h1>
      <p>Email us at support@example.com</p>
    </ShowRenderedHTML>
  );
}
```

제목에 변수 사용:

```js
// 잘못된 예 - 단일 문자열이 아님
<title>Results page {pageNumber}</title>

// 해결: 문자열 보간 사용
<title>{`Results page ${pageNumber}`}</title>
```

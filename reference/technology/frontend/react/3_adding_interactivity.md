# 상호작용 추가하기 (Adding Interactivity)

## 개요

> 원문: https://react.dev/learn/adding-interactivity

- 화면의 일부 요소는 사용자 입력에 반응하여 갱신됨
- React에서 시간에 따라 변하는 데이터를 **state**라 부름
- 어떤 컴포넌트에든 state를 추가하고 필요에 따라 갱신할 수 있음

### 이 장에서 다루는 내용

- 사용자 이벤트 처리 방법
- 컴포넌트가 state로 정보를 "기억"하는 방법
- React가 UI를 갱신하는 두 단계(렌더 / 커밋)
- state를 변경한 직후에 값이 바뀌지 않는 이유
- 여러 state 갱신을 대기열에 넣는 방법
- state 안의 객체를 갱신하는 방법
- state 안의 배열을 갱신하는 방법

### 개요 예제 -- 이벤트 핸들러를 props로 전달하기

```js
export default function App() {
  return (
    <Toolbar
      onPlayMovie={() => alert('Playing!')}
      onUploadImage={() => alert('Uploading!')}
    />
  );
}

function Toolbar({ onPlayMovie, onUploadImage }) {
  return (
    <div>
      <Button onClick={onPlayMovie}>
        Play Movie
      </Button>
      <Button onClick={onUploadImage}>
        Upload Image
      </Button>
    </div>
  );
}

function Button({ onClick, children }) {
  return (
    <button onClick={onClick}>
      {children}
    </button>
  );
}
```

### 개요 예제 -- useState로 이미지 갤러리 구현하기

```js
import { useState } from 'react';
import { sculptureList } from './data.js';

export default function Gallery() {
  const [index, setIndex] = useState(0);
  const [showMore, setShowMore] = useState(false);
  const hasNext = index < sculptureList.length - 1;

  function handleNextClick() {
    if (hasNext) {
      setIndex(index + 1);
    } else {
      setIndex(0);
    }
  }

  function handleMoreClick() {
    setShowMore(!showMore);
  }

  let sculpture = sculptureList[index];
  return (
    <>
      <button onClick={handleNextClick}>
        Next
      </button>
      <h2>
        <i>{sculpture.name} </i>
        by {sculpture.artist}
      </h2>
      <h3>
        ({index + 1} of {sculptureList.length})
      </h3>
      <button onClick={handleMoreClick}>
        {showMore ? 'Hide' : 'Show'} details
      </button>
      {showMore && <p>{sculpture.description}</p>}
      <img
        src={sculpture.url}
        alt={sculpture.alt}
      />
    </>
  );
}
```

### 개요 예제 -- 스냅샷으로서의 state

```js
console.log(count);  // 0
setCount(count + 1); // 1로 리렌더 요청
console.log(count);  // 여전히 0
```

### 개요 예제 -- state 갱신 대기열(updater function)

```js
// 버그: +3 버튼이 1만 증가함
function increment() {
  setScore(score + 1);
}

// 수정: updater function 사용
function increment() {
  setScore(s => s + 1);
}
```

---

## 이벤트에 응답하기 (Responding to Events)

> 원문: https://react.dev/learn/responding-to-events

### 이벤트 핸들러 추가하기

- 이벤트 핸들러는 클릭, 호버, 폼 입력 포커스 등 사용자 상호작용에 반응하여 실행되는 함수임
- `<button>` 같은 내장 컴포넌트는 `onClick` 등 브라우저 이벤트만 지원함
- 커스텀 컴포넌트에는 애플리케이션 고유 이름으로 이벤트 핸들러 props를 정의할 수 있음

이벤트 핸들러를 추가하는 세 단계:

1. 컴포넌트 내부에 함수를 선언함
2. 함수 안에 로직을 구현함
3. JSX 태그에 props로 전달함

```js
export default function Button() {
  function handleClick() {
    alert('You clicked me!');
  }

  return (
    <button onClick={handleClick}>
      Click me
    </button>
  );
}
```

이벤트 핸들러 함수의 특성:

- 보통 컴포넌트 내부에서 정의됨
- 이름이 `handle`로 시작하고 뒤에 이벤트 이름이 옴 (관례)

### 인라인 이벤트 핸들러

- 함수 표현식이나 화살표 함수로 JSX 안에 직접 정의할 수 있음
- 짧은 함수에 편리함

```jsx
<button onClick={function handleClick() {
  alert('You clicked me!');
}}>
```

```jsx
<button onClick={() => {
  alert('You clicked me!');
}}>
```

### 함수 전달 vs. 함수 호출 -- 흔한 실수

- 함수를 전달해야 함 (호출하면 안 됨)
- `onClick={handleClick}` -- 올바름 (함수를 전달)
- `onClick={handleClick()}` -- 잘못됨 (렌더링 중 즉시 호출됨)
- `onClick={() => alert('...')}` -- 올바름 (함수를 전달)
- `onClick={alert('...')}` -- 잘못됨 (렌더링 시마다 실행됨)

```jsx
// 잘못된 예: 렌더링 때 실행됨
<button onClick={alert('You clicked me!')}>

// 올바른 예: 클릭 시 실행됨
<button onClick={() => alert('You clicked me!')}>
```

### 이벤트 핸들러에서 props 읽기

- 이벤트 핸들러는 컴포넌트 내부에 선언되므로 해당 컴포넌트의 props에 접근 가능함

```js
function AlertButton({ message, children }) {
  return (
    <button onClick={() => alert(message)}>
      {children}
    </button>
  );
}

export default function Toolbar() {
  return (
    <div>
      <AlertButton message="Playing!">
        Play Movie
      </AlertButton>
      <AlertButton message="Uploading!">
        Upload Image
      </AlertButton>
    </div>
  );
}
```

### 이벤트 핸들러를 props로 전달하기

- 부모 컴포넌트가 자식의 이벤트 핸들러를 지정하는 패턴이 일반적임
- 같은 Button 컴포넌트를 다른 동작으로 사용할 수 있음

```js
function Button({ onClick, children }) {
  return (
    <button onClick={onClick}>
      {children}
    </button>
  );
}

function PlayButton({ movieName }) {
  function handlePlayClick() {
    alert(`Playing ${movieName}!`);
  }

  return (
    <Button onClick={handlePlayClick}>
      Play "{movieName}"
    </Button>
  );
}

function UploadButton() {
  return (
    <Button onClick={() => alert('Uploading!')}>
      Upload Image
    </Button>
  );
}

export default function Toolbar() {
  return (
    <div>
      <PlayButton movieName="Kiki's Delivery Service" />
      <UploadButton />
    </div>
  );
}
```

- 디자인 시스템에서는 버튼 같은 컴포넌트가 스타일만 포함하고 동작은 지정하지 않는 것이 일반적임
- `PlayButton`, `UploadButton` 같은 컴포넌트가 이벤트 핸들러를 전달함

### 이벤트 핸들러 props 이름 짓기

- 내장 컴포넌트(`<button>`, `<div>`)는 `onClick` 같은 브라우저 이벤트 이름만 지원함
- 커스텀 컴포넌트의 이벤트 핸들러 props는 자유롭게 이름을 지을 수 있음
- 관례상 `on`으로 시작하고 대문자가 뒤따름

```js
function Button({ onSmash, children }) {
  return (
    <button onClick={onSmash}>
      {children}
    </button>
  );
}

export default function App() {
  return (
    <div>
      <Button onSmash={() => alert('Playing!')}>
        Play Movie
      </Button>
      <Button onSmash={() => alert('Uploading!')}>
        Upload Image
      </Button>
    </div>
  );
}
```

- 여러 상호작용을 지원할 때는 앱 고유 개념에 맞게 이름을 지음
  - 예: `onPlayMovie`, `onUploadImage`
- 이렇게 하면 나중에 사용 방식을 변경할 유연성이 생김

```js
export default function App() {
  return (
    <Toolbar
      onPlayMovie={() => alert('Playing!')}
      onUploadImage={() => alert('Uploading!')}
    />
  );
}

function Toolbar({ onPlayMovie, onUploadImage }) {
  return (
    <div>
      <Button onClick={onPlayMovie}>
        Play Movie
      </Button>
      <Button onClick={onUploadImage}>
        Upload Image
      </Button>
    </div>
  );
}

function Button({ onClick, children }) {
  return (
    <button onClick={onClick}>
      {children}
    </button>
  );
}
```

### 접근성 참고사항

- 클릭 처리에는 `<div onClick>` 대신 `<button onClick>`을 사용해야 함
- 실제 `<button>`은 키보드 내비게이션 등 내장 브라우저 동작을 지원함
- 기본 버튼 스타일이 마음에 들지 않으면 CSS로 변경할 수 있음

### 이벤트 전파 (Event Propagation)

- 이벤트 핸들러는 자식 컴포넌트의 이벤트도 잡아냄
- 이벤트가 트리 위로 "버블링(전파)"됨: 이벤트 발생 위치에서 시작해 트리 위로 올라감

```js
export default function Toolbar() {
  return (
    <div className="Toolbar" onClick={() => {
      alert('You clicked on the toolbar!');
    }}>
      <button onClick={() => alert('Playing!')}>
        Play Movie
      </button>
      <button onClick={() => alert('Uploading!')}>
        Upload Image
      </button>
    </div>
  );
}
```

- 버튼을 클릭하면 해당 버튼의 `onClick`이 먼저 실행되고, 그다음 부모 `<div>`의 `onClick`이 실행됨
- 두 개의 메시지가 표시됨
- 툴바 자체를 클릭하면 부모 `<div>`의 `onClick`만 실행됨

주의사항:

- React에서 모든 이벤트는 전파됨. 단, `onScroll`은 부착된 JSX 태그에서만 작동함

### 전파 중단하기

- 이벤트 핸들러는 이벤트 객체(`e`)를 인수로 받음
- `e.stopPropagation()`을 호출하면 이벤트가 부모 컴포넌트로 전파되지 않음

```js
function Button({ onClick, children }) {
  return (
    <button onClick={e => {
      e.stopPropagation();
      onClick();
    }}>
      {children}
    </button>
  );
}

export default function Toolbar() {
  return (
    <div className="Toolbar" onClick={() => {
      alert('You clicked on the toolbar!');
    }}>
      <Button onClick={() => alert('Playing!')}>
        Play Movie
      </Button>
      <Button onClick={() => alert('Uploading!')}>
        Upload Image
      </Button>
    </div>
  );
}
```

버튼 클릭 시 동작 순서:

1. React가 `<button>`에 전달된 `onClick` 핸들러를 호출함
2. `Button`에 정의된 핸들러가 다음을 수행함:
   - `e.stopPropagation()`으로 추가 전파를 막음
   - `Toolbar` 컴포넌트에서 전달된 `onClick` 함수를 호출함
3. `Toolbar`에서 정의된 함수가 버튼 자체의 alert를 표시함
4. 전파가 중단되었으므로 부모 `<div>`의 `onClick`은 실행되지 않음

### 캡처 단계 이벤트 (Capture Phase Events)

- 드물지만, 전파가 중단된 경우에도 자식 요소의 모든 이벤트를 잡아야 할 때가 있음
  - 예: 분석을 위해 전파 로직과 무관하게 모든 클릭을 기록할 때
- 이벤트 이름 끝에 `Capture`를 붙이면 됨

```js
<div onClickCapture={() => { /* 먼저 실행됨 */ }}>
  <button onClick={e => e.stopPropagation()} />
  <button onClick={e => e.stopPropagation()} />
</div>
```

이벤트 전파의 세 단계:

1. 아래로 이동하며 모든 `onClickCapture` 핸들러를 호출함
2. 클릭된 요소의 `onClick` 핸들러를 실행함
3. 위로 이동하며 모든 `onClick` 핸들러를 호출함

- 캡처 이벤트는 라우터나 분석 코드에 유용하지만, 앱 코드에서는 거의 사용하지 않음

### 전파 대안 -- 핸들러에서 명시적으로 부모 핸들러 호출하기

- 자식 핸들러에서 부모의 이벤트 핸들러 prop을 명시적으로 호출하는 패턴임
- 전파와 달리 자동으로 일어나지 않지만, 이벤트 결과로 실행되는 코드 체인을 명확히 추적할 수 있음

```js
function Button({ onClick, children }) {
  return (
    <button onClick={e => {
      e.stopPropagation();
      onClick();
    }}>
      {children}
    </button>
  );
}
```

### 기본 동작 방지 (Preventing Default Behavior)

- 일부 브라우저 이벤트에는 기본 동작이 있음
  - 예: `<form>`의 submit 이벤트는 페이지를 새로고침함
- `e.preventDefault()`를 호출하면 기본 동작을 막을 수 있음

```js
export default function Signup() {
  return (
    <form onSubmit={e => {
      e.preventDefault();
      alert('Submitting!');
    }}>
      <input />
      <button>Send</button>
    </form>
  );
}
```

`stopPropagation`과 `preventDefault`의 차이:

- `e.stopPropagation()`: 상위 태그의 이벤트 핸들러 실행을 막음
- `e.preventDefault()`: 해당 이벤트의 브라우저 기본 동작을 막음
- 두 메서드는 서로 관련 없음

### 이벤트 핸들러에 부수 효과가 있어도 되는가

- 이벤트 핸들러는 부수 효과를 넣기에 가장 적합한 장소임
- 렌더링 함수와 달리 이벤트 핸들러는 순수할 필요가 없음
- 입력값 변경, 리스트 수정 등을 처리할 수 있음
- 정보를 변경하려면 먼저 저장할 방법이 필요하며, React에서는 state를 사용함

### 요약

- 이벤트 핸들러는 `<button>` 같은 요소에 함수를 props로 전달하여 처리함
- 이벤트 핸들러는 전달해야 하며, 호출하면 안 됨: `onClick={handleClick}` (O), `onClick={handleClick()}` (X)
- 이벤트 핸들러 함수는 별도로 정의하거나 인라인으로 정의할 수 있음
- 이벤트 핸들러는 컴포넌트 내부에 정의되므로 props에 접근 가능함
- 부모에서 이벤트 핸들러를 선언하고 자식에 props로 전달할 수 있음
- 애플리케이션 고유 이름으로 이벤트 핸들러 props를 정의할 수 있음
- 이벤트는 위쪽으로 전파됨. `e.stopPropagation()`으로 막을 수 있음
- 이벤트에 원하지 않는 기본 동작이 있을 수 있음. `e.preventDefault()`로 막을 수 있음
- 자식 핸들러에서 부모 이벤트 핸들러 prop을 명시적으로 호출하는 것은 전파의 좋은 대안임

---

## State: 컴포넌트의 메모리 (State: A Component's Memory)

> 원문: https://react.dev/learn/state-a-components-memory

### 일반 변수로는 부족한 이유

- 일반 변수를 변경해도 화면에 반영되지 않음
- 두 가지 이유가 있음:
  1. **지역 변수는 렌더 간에 유지되지 않음** -- React가 컴포넌트를 다시 렌더링할 때 처음부터 렌더링하므로 지역 변수 변경사항을 고려하지 않음
  2. **지역 변수 변경은 렌더를 촉발하지 않음** -- React가 새 데이터로 컴포넌트를 다시 렌더링해야 한다는 것을 인지하지 못함

```js
import { sculptureList } from './data.js';

export default function Gallery() {
  let index = 0;

  function handleClick() {
    index = index + 1;
  }

  let sculpture = sculptureList[index];
  return (
    <>
      <button onClick={handleClick}>
        Next
      </button>
      <h2>
        <i>{sculpture.name} </i>
        by {sculpture.artist}
      </h2>
      <h3>
        ({index + 1} of {sculptureList.length})
      </h3>
      <img
        src={sculpture.url}
        alt={sculpture.alt}
      />
      <p>
        {sculpture.description}
      </p>
    </>
  );
}
```

컴포넌트를 새 데이터로 갱신하려면 두 가지가 필요함:

1. 렌더 간에 데이터를 **유지**함
2. React가 새 데이터로 컴포넌트를 **다시 렌더링하도록 촉발**함

`useState` Hook이 이 두 가지를 제공함:

1. 렌더 간에 데이터를 유지하는 **state 변수**
2. 변수를 갱신하고 React가 컴포넌트를 다시 렌더링하도록 촉발하는 **state setter 함수**

### state 변수 추가하기

```js
import { useState } from 'react';
```

```js
// let index = 0; 대신:
const [index, setIndex] = useState(0);
```

- `index`는 state 변수이고, `setIndex`는 setter 함수임
- `[`와 `]` 구문은 배열 구조 분해(array destructuring)임
- `useState`가 반환하는 배열에는 항상 정확히 두 개의 항목이 있음

```js
function handleClick() {
  setIndex(index + 1);
}
```

동작하는 완성된 예제:

```js
import { useState } from 'react';
import { sculptureList } from './data.js';

export default function Gallery() {
  const [index, setIndex] = useState(0);

  function handleClick() {
    setIndex(index + 1);
  }

  let sculpture = sculptureList[index];
  return (
    <>
      <button onClick={handleClick}>
        Next
      </button>
      <h2>
        <i>{sculpture.name} </i>
        by {sculpture.artist}
      </h2>
      <h3>
        ({index + 1} of {sculptureList.length})
      </h3>
      <img
        src={sculpture.url}
        alt={sculpture.alt}
      />
      <p>
        {sculpture.description}
      </p>
    </>
  );
}
```

### 첫 번째 Hook

- React에서 `useState`를 비롯해 `use`로 시작하는 모든 함수를 Hook이라 부름
- Hook은 React가 렌더링하는 동안에만 사용 가능한 특별한 함수임
- Hook으로 다양한 React 기능에 "연결(hook into)"할 수 있음

Hook 사용 규칙:

- Hook은 컴포넌트의 최상위 레벨이나 커스텀 Hook에서만 호출할 수 있음
- 조건문, 반복문, 기타 중첩 함수 안에서는 호출할 수 없음
- 컴포넌트의 필요사항에 대한 무조건적인 선언으로 생각하면 도움이 됨

### `useState`의 구조

```js
const [index, setIndex] = useState(0);
```

- `useState`의 유일한 인수는 state 변수의 **초기값**임
- 관례상 `const [something, setSomething]`과 같이 이름을 지음
- 컴포넌트가 렌더링될 때마다 `useState`는 두 값이 담긴 배열을 반환함:
  1. 저장된 값을 가진 **state 변수** (`index`)
  2. state 변수를 갱신하고 React가 컴포넌트를 다시 렌더링하도록 촉발하는 **state setter 함수** (`setIndex`)

동작 흐름:

1. **첫 렌더링**: `useState(0)`에 `0`을 전달했으므로 `[0, setIndex]`를 반환함. React가 `0`을 최신 state 값으로 기억함
2. **state 갱신**: 사용자가 버튼을 클릭하면 `setIndex(index + 1)`이 호출됨. `index`가 `0`이므로 `setIndex(1)`이 됨. React가 `index`를 `1`로 기억하고 다시 렌더링을 촉발함
3. **두 번째 렌더링**: React는 여전히 `useState(0)`을 보지만, `index`를 `1`로 설정했다는 것을 기억하므로 `[1, setIndex]`를 반환함
4. 이 과정이 반복됨

### 여러 state 변수 사용하기

- 한 컴포넌트에 여러 타입의 state 변수를 원하는 만큼 가질 수 있음
- 서로 관련 없는 state라면 별도의 state 변수로 관리하는 것이 좋음
- 두 state를 항상 함께 변경한다면 하나로 합치는 것이 편리함
  - 예: 필드가 많은 폼은 필드별 state 변수보다 객체 하나가 편리함

```js
import { useState } from 'react';
import { sculptureList } from './data.js';

export default function Gallery() {
  const [index, setIndex] = useState(0);
  const [showMore, setShowMore] = useState(false);

  function handleNextClick() {
    setIndex(index + 1);
  }

  function handleMoreClick() {
    setShowMore(!showMore);
  }

  let sculpture = sculptureList[index];
  return (
    <>
      <button onClick={handleNextClick}>
        Next
      </button>
      <h2>
        <i>{sculpture.name} </i>
        by {sculpture.artist}
      </h2>
      <h3>
        ({index + 1} of {sculptureList.length})
      </h3>
      <button onClick={handleMoreClick}>
        {showMore ? 'Hide' : 'Show'} details
      </button>
      {showMore && <p>{sculpture.description}</p>}
      <img
        src={sculpture.url}
        alt={sculpture.alt}
      />
    </>
  );
}
```

### React가 어떤 state를 반환할지 아는 방법 (심화)

- `useState`에는 어떤 state 변수를 참조하는지 식별자를 전달하지 않음
- Hook은 **매 렌더링에서 안정적인 호출 순서**에 의존함
- "최상위에서만 Hook을 호출하라"는 규칙을 따르면 Hook은 항상 같은 순서로 호출됨
- 내부적으로 React는 각 컴포넌트에 대해 state 쌍의 배열을 유지함
- 현재 쌍 인덱스를 관리하며, 렌더링 전에 `0`으로 설정함
- `useState`를 호출할 때마다 다음 state 쌍을 제공하고 인덱스를 증가시킴

간소화된 내부 동작 예시:

```js
let componentHooks = [];
let currentHookIndex = 0;

// React 내부의 useState 동작 (간소화)
function useState(initialState) {
  let pair = componentHooks[currentHookIndex];
  if (pair) {
    // 첫 렌더링이 아니면 기존 state 쌍을 반환함
    currentHookIndex++;
    return pair;
  }

  // 첫 렌더링이면 state 쌍을 생성하고 저장함
  pair = [initialState, setState];

  function setState(nextState) {
    pair[0] = nextState;
    updateDOM();
  }

  componentHooks[currentHookIndex] = pair;
  currentHookIndex++;
  return pair;
}
```

### state는 격리되고 비공개임

- state는 화면 위의 컴포넌트 인스턴스에 대해 지역적임
- **같은 컴포넌트를 두 번 렌더링하면 각 복사본이 완전히 격리된 state를 가짐**
- 하나를 변경해도 다른 하나에 영향을 주지 않음

```js
import Gallery from './Gallery.js';

export default function Page() {
  return (
    <div className="Page">
      <Gallery />
      <Gallery />
    </div>
  );
}
```

- props와 달리 **state는 선언한 컴포넌트에 완전히 비공개**임
- 부모 컴포넌트가 변경할 수 없음
- 두 갤러리의 state를 동기화하려면 자식에서 state를 제거하고 가장 가까운 공통 부모에 추가하는 것이 올바른 방법임

### 요약

- 컴포넌트가 렌더 간에 정보를 "기억"해야 할 때 state 변수를 사용함
- state 변수는 `useState` Hook으로 선언함
- Hook은 `use`로 시작하는 특별한 함수이며, state 같은 React 기능에 연결할 수 있음
- Hook은 import처럼 무조건적으로 호출해야 함. 컴포넌트나 다른 Hook의 최상위에서만 호출이 유효함
- `useState` Hook은 현재 state와 그것을 갱신하는 함수, 두 값의 쌍을 반환함
- 여러 개의 state 변수를 가질 수 있음. 내부적으로 React는 순서로 매칭함
- state는 컴포넌트에 비공개임. 두 곳에서 렌더링하면 각 복사본이 자체 state를 가짐

---

## 렌더와 커밋 (Render and Commit)

> 원문: https://react.dev/learn/render-and-commit

- 컴포넌트가 화면에 표시되기 전에 React에 의해 렌더링되어야 함
- 이 과정을 이해하면 코드가 어떻게 실행되는지 파악하고 동작을 설명할 수 있음

### UI 제공의 3단계

- 컴포넌트를 주방의 요리사, React를 웨이터에 비유할 수 있음

1. **촉발(Trigger)**: 렌더를 촉발함 (손님의 주문을 주방에 전달)
2. **렌더(Render)**: 컴포넌트를 렌더링함 (주방에서 주문을 준비)
3. **커밋(Commit)**: DOM에 반영함 (주문을 테이블에 놓음)

### 1단계: 렌더 촉발

컴포넌트가 렌더링되는 두 가지 이유:

1. 컴포넌트의 **초기 렌더링**
2. 컴포넌트(또는 조상)의 **state가 갱신**됨

#### 초기 렌더링

- 앱이 시작될 때 초기 렌더링을 촉발해야 함
- `createRoot`를 대상 DOM 노드와 함께 호출하고, `render` 메서드를 컴포넌트와 함께 호출함

```js
import Image from './Image.js';
import { createRoot } from 'react-dom/client';

const root = createRoot(document.getElementById('root'))
root.render(<Image />);
```

```js
export default function Image() {
  return (
    <img
      src="https://react.dev/images/docs/scientists/ZF6s192.jpg"
      alt="'Floralis Generica' by Eduardo Catalano: a gigantic metallic flower sculpture with reflective petals"
    />
  );
}
```

#### state 갱신 시 리렌더링

- 초기 렌더링 후에는 state를 `set` 함수로 갱신하여 추가 렌더링을 촉발할 수 있음
- state 갱신이 자동으로 렌더를 대기열에 넣음

### 2단계: React가 컴포넌트를 렌더링함

- 렌더를 촉발한 후 React가 컴포넌트를 호출하여 화면에 표시할 내용을 파악함
- **"렌더링"이란 React가 컴포넌트를 호출하는 것**임

렌더링 규칙:

- **초기 렌더링**: React가 루트 컴포넌트를 호출함
- **후속 렌더링**: state 갱신이 렌더를 촉발한 함수 컴포넌트를 호출함

- 이 과정은 재귀적임: 갱신된 컴포넌트가 다른 컴포넌트를 반환하면 React가 그 컴포넌트도 렌더링함
- 중첩 컴포넌트가 없을 때까지 계속됨

```js
export default function Gallery() {
  return (
    <section>
      <h1>Inspiring Sculptures</h1>
      <Image />
      <Image />
      <Image />
    </section>
  );
}

function Image() {
  return (
    <img
      src="https://react.dev/images/docs/scientists/ZF6s192.jpg"
      alt="'Floralis Generica' by Eduardo Catalano: a gigantic metallic flower sculpture with reflective petals"
    />
  );
}
```

- **초기 렌더링**: `<section>`, `<h1>`, 세 개의 `<img>` 태그에 대한 DOM 노드를 생성함
- **리렌더링**: 이전 렌더링 이후 변경된 속성이 있는지 계산함. 커밋 단계까지는 그 정보로 아무 작업도 하지 않음

렌더링의 핵심 원칙 -- 순수해야 함:

- **같은 입력, 같은 출력**: 같은 입력이 주어지면 컴포넌트는 항상 같은 JSX를 반환해야 함
- **자기 일에만 관여**: 렌더링 전에 존재하던 객체나 변수를 변경하지 않아야 함

성능 최적화 참고:

- 갱신된 컴포넌트 안에 중첩된 모든 컴포넌트를 렌더링하는 기본 동작은 성능에 최적이 아닐 수 있음
- 성능 문제가 발생하면 여러 가지 opt-in 최적화 방법이 있음
- 성급하게 최적화하지 말 것

### 3단계: React가 DOM에 변경사항을 커밋함

- 컴포넌트를 렌더링(호출)한 후 React가 DOM을 수정함
- **초기 렌더링**: `appendChild()` DOM API를 사용하여 생성한 모든 DOM 노드를 화면에 배치함
- **리렌더링**: 렌더링 중에 계산된 최소한의 필수 연산만 적용하여 DOM이 최신 렌더링 출력과 일치하도록 함

**React는 렌더 간에 차이가 있는 DOM 노드만 변경함**

```js
export default function Clock({ time }) {
  return (
    <>
      <h1>{time}</h1>
      <input />
    </>
  );
}
```

- 매초 리렌더링되어도 `<h1>`의 내용만 갱신하고 `<input>`과 그 `value`는 그대로 둠

### 에필로그: 브라우저 페인팅

- 렌더링이 완료되고 React가 DOM을 갱신한 후 브라우저가 화면을 다시 칠함(repaint)
- 이 과정을 "브라우저 렌더링"이라 부르지만, 혼동을 피하기 위해 문서에서는 "페인팅"이라 부름

### 요약

- React 앱의 모든 화면 갱신은 세 단계로 이루어짐:
  1. 촉발 (Trigger)
  2. 렌더 (Render)
  3. 커밋 (Commit)
- Strict Mode를 사용하면 컴포넌트의 실수를 찾을 수 있음
- 렌더링 결과가 이전과 같으면 React는 DOM을 건드리지 않음

---

## 스냅샷으로서의 State (State as a Snapshot)

> 원문: https://react.dev/learn/state-as-a-snapshot

- state 변수는 일반 JavaScript 변수처럼 보이지만, 스냅샷에 더 가깝게 동작함
- state를 설정해도 이미 가지고 있는 state 변수가 변경되지 않고, 대신 리렌더링을 촉발함

### state 설정은 렌더를 촉발함

- 사용자 이벤트에 직접 반응하여 UI가 변경된다고 생각할 수 있음
- React에서는 약간 다르게 동작함: state를 설정하면 React에 리렌더링을 요청함
- 인터페이스가 이벤트에 반응하려면 state를 갱신해야 함

```js
import { useState } from 'react';

export default function Form() {
  const [isSent, setIsSent] = useState(false);
  const [message, setMessage] = useState('Hi!');
  if (isSent) {
    return <h1>Your message is on its way!</h1>
  }
  return (
    <form onSubmit={(e) => {
      e.preventDefault();
      setIsSent(true);
      sendMessage(message);
    }}>
      <textarea
        placeholder="Message"
        value={message}
        onChange={e => setMessage(e.target.value)}
      />
      <button type="submit">Send</button>
    </form>
  );
}

function sendMessage(message) {
  // ...
}
```

버튼 클릭 시 동작:

1. `onSubmit` 이벤트 핸들러가 실행됨
2. `setIsSent(true)`가 `isSent`를 `true`로 설정하고 새 렌더를 대기열에 넣음
3. React가 새 `isSent` 값에 따라 컴포넌트를 리렌더링함

### 렌더링은 시점의 스냅샷을 찍음

- "렌더링"이란 React가 컴포넌트(함수)를 호출하는 것임
- 반환되는 JSX는 해당 시점 UI의 스냅샷과 같음
- props, 이벤트 핸들러, 지역 변수 모두 **렌더링 시점의 state를 사용하여** 계산됨

- state는 함수가 반환된 후 사라지는 일반 변수와 달리, React 자체 안에 "존재"함
- React가 컴포넌트를 호출할 때 해당 렌더링에 대한 state의 스냅샷을 제공함
- 컴포넌트는 **해당 렌더의 state 값을 사용하여** 계산된 새로운 props와 이벤트 핸들러가 포함된 UI 스냅샷을 반환함

React가 컴포넌트를 리렌더링할 때:

1. React가 함수를 다시 호출함
2. 함수가 새 JSX 스냅샷을 반환함
3. React가 반환된 스냅샷에 맞게 화면을 갱신함

### "+3" 버튼 실험

```js
import { useState } from 'react';

export default function Counter() {
  const [number, setNumber] = useState(0);

  return (
    <>
      <h1>{number}</h1>
      <button onClick={() => {
        setNumber(number + 1);
        setNumber(number + 1);
        setNumber(number + 1);
      }}>+3</button>
    </>
  )
}
```

- `+3` 버튼을 클릭해도 `number`는 클릭당 1만 증가함
- **state 설정은 다음 렌더링에 대해서만 변경됨**
- 첫 렌더링에서 `number`는 `0`이므로, 해당 렌더의 `onClick` 핸들러에서 `setNumber(number + 1)`을 호출한 후에도 `number`의 값은 여전히 `0`임

```js
// 첫 렌더링의 이벤트 핸들러 (값을 대입해서 보면):
<button onClick={() => {
  setNumber(0 + 1);
  setNumber(0 + 1);
  setNumber(0 + 1);
}}>+3</button>
```

- 세 번 모두 `setNumber(1)`을 호출한 것이므로, 리렌더링 후 `number`는 `3`이 아닌 `1`이 됨

### 시간에 따른 State

#### 즉시 alert 예제

```js
import { useState } from 'react';

export default function Counter() {
  const [number, setNumber] = useState(0);

  return (
    <>
      <h1>{number}</h1>
      <button onClick={() => {
        setNumber(number + 5);
        alert(number);
      }}>+5</button>
    </>
  )
}
```

- 값 대입법을 적용하면: `setNumber(0 + 5); alert(0);`
- alert는 "0"을 표시함

#### 타이머를 사용한 지연 alert

```js
import { useState } from 'react';

export default function Counter() {
  const [number, setNumber] = useState(0);

  return (
    <>
      <h1>{number}</h1>
      <button onClick={() => {
        setNumber(number + 5);
        setTimeout(() => {
          alert(number);
        }, 3000);
      }}>+5</button>
    </>
  )
}
```

- 값 대입법 적용: `setNumber(0 + 5); setTimeout(() => { alert(0); }, 3000);`
- React에 저장된 state는 alert가 실행될 때 변경되었을 수 있지만, 사용자가 상호작용한 시점의 state 스냅샷을 사용하여 예약되었음
- **state 변수의 값은 하나의 렌더 안에서 절대 변하지 않음** (이벤트 핸들러 코드가 비동기여도 마찬가지)
- 해당 렌더의 `onClick` 안에서 `setNumber(number + 5)` 호출 후에도 `number`는 계속 `0`임
- React가 컴포넌트를 호출하여 UI의 "스냅샷을 찍을" 때 값이 "고정"되었음

#### 메시지 전송 폼 예제

```js
import { useState } from 'react';

export default function Form() {
  const [to, setTo] = useState('Alice');
  const [message, setMessage] = useState('Hello');

  function handleSubmit(e) {
    e.preventDefault();
    setTimeout(() => {
      alert(`You said ${message} to ${to}`);
    }, 5000);
  }

  return (
    <form onSubmit={handleSubmit}>
      <label>
        To:{' '}
        <select
          value={to}
          onChange={e => setTo(e.target.value)}>
          <option value="Alice">Alice</option>
          <option value="Bob">Bob</option>
        </select>
      </label>
      <textarea
        placeholder="Message"
        value={message}
        onChange={e => setMessage(e.target.value)}
      />
      <button type="submit">Send</button>
    </form>
  );
}
```

- "Send"를 누른 후 수신자를 Bob으로 변경해도, 5초 뒤 alert는 "You said Hello to Alice"를 표시함
- **React는 하나의 렌더의 이벤트 핸들러 안에서 state 값을 "고정"해 둠**
- 코드 실행 중 state가 변경되었는지 걱정할 필요 없음

### 요약

- state 설정은 새 렌더를 요청함
- React는 컴포넌트 외부에, 마치 선반 위에 놓인 것처럼 state를 저장함
- `useState`를 호출하면 React가 **해당 렌더에 대한** state의 스냅샷을 제공함
- 변수와 이벤트 핸들러는 리렌더를 "살아남지" 못함. 매 렌더가 자체 이벤트 핸들러를 가짐
- 모든 렌더(와 그 안의 함수)는 항상 React가 **해당** 렌더에 제공한 state 스냅샷을 "봄"
- 렌더링된 JSX를 생각하듯이, 이벤트 핸들러 안의 state도 값을 대입하여 이해할 수 있음
- 과거에 생성된 이벤트 핸들러는 그것이 생성된 렌더의 state 값을 가짐

---

## state 갱신 대기열 (Queueing a Series of State Updates)

> 원문: https://react.dev/learn/queueing-a-series-of-state-updates

- state 변수를 설정하면 다음 렌더가 대기열에 들어감
- 다음 렌더를 대기열에 넣기 전에 값에 여러 연산을 수행하고 싶을 때가 있음
- 이를 위해 React가 state 갱신을 일괄 처리(batching)하는 방식을 이해해야 함

### React는 state 갱신을 일괄 처리함

- 같은 이벤트 핸들러 안에서 `setNumber(number + 1)`을 세 번 호출해도, 각 렌더의 state 값은 고정되어 있으므로 `number`는 1만 증가함

```js
import { useState } from 'react';

export default function Counter() {
  const [number, setNumber] = useState(0);

  return (
    <>
      <h1>{number}</h1>
      <button onClick={() => {
        setNumber(number + 1);
        setNumber(number + 1);
        setNumber(number + 1);
      }}>+3</button>
    </>
  )
}
```

- 첫 렌더의 이벤트 핸들러에서 `number`는 항상 `0`이므로 세 번 모두 `setNumber(0 + 1)`이 됨

추가 요인:

- **React는 이벤트 핸들러 안의 모든 코드가 실행된 후에 state 갱신을 처리함**
- 리렌더링은 모든 `setNumber()` 호출이 완료된 **후에** 발생함
- 레스토랑에서 웨이터가 첫 번째 요리를 듣자마자 주방으로 달려가지 않고, 주문을 다 받은 후에 가는 것과 같음

일괄 처리의 이점:

- 여러 컴포넌트에서 여러 state 변수를 갱신해도 너무 많은 리렌더링을 촉발하지 않음
- UI가 이벤트 핸들러와 그 안의 코드가 모두 완료될 때까지 갱신되지 않음
- **배칭(batching)**이라 불리며, React 앱을 훨씬 빠르게 만듦
- 일부 변수만 갱신된 "중간 완료" 렌더를 다루지 않아도 됨

참고:

- **React는 여러 의도적 이벤트(예: 클릭)에 걸쳐 배칭하지 않음** -- 각 클릭은 별도로 처리됨
- 일반적으로 안전한 경우에만 배칭함

### 다음 렌더 전에 같은 state를 여러 번 갱신하기

- 흔한 경우는 아니지만, 다음 렌더 전에 같은 state 변수를 여러 번 갱신하고 싶을 때가 있음
- `setNumber(number + 1)` 대신 `setNumber(n => n + 1)` 같은 **updater function**을 전달함
- React에 "state 값으로 무언가를 하라"고 지시하는 방법임 (단순 교체가 아님)

```js
import { useState } from 'react';

export default function Counter() {
  const [number, setNumber] = useState(0);

  return (
    <>
      <h1>{number}</h1>
      <button onClick={() => {
        setNumber(n => n + 1);
        setNumber(n => n + 1);
        setNumber(n => n + 1);
      }}>+3</button>
    </>
  )
}
```

- `n => n + 1`은 **updater function**이라 불림
- state setter에 전달하면:
  1. React가 이벤트 핸들러의 다른 모든 코드가 실행된 후 처리할 대기열에 이 함수를 넣음
  2. 다음 렌더링에서 React가 대기열을 순회하며 최종 갱신된 state를 제공함

대기열 처리 과정 (`number`가 `0`일 때):

- `setNumber(n => n + 1)`: `n => n + 1` 함수를 대기열에 추가
- `setNumber(n => n + 1)`: `n => n + 1` 함수를 대기열에 추가
- `setNumber(n => n + 1)`: `n => n + 1` 함수를 대기열에 추가

다음 렌더에서 `useState` 호출 시 React가 대기열을 순회함:

- `n => n + 1`: `n`이 `0`이므로 `1`을 반환
- `n => n + 1`: `n`이 `1`이므로 `2`를 반환
- `n => n + 1`: `n`이 `2`이므로 `3`을 반환

React가 `3`을 최종 결과로 저장하고 `useState`에서 반환함

### state를 교체한 후 갱신하면 어떻게 되는가

```js
<button onClick={() => {
  setNumber(number + 5);
  setNumber(n => n + 1);
}}>
```

이벤트 핸들러가 React에 지시하는 내용:

1. `setNumber(number + 5)`: `number`가 `0`이므로 `setNumber(0 + 5)`. React가 "`5`로 교체"를 대기열에 추가함
2. `setNumber(n => n + 1)`: `n => n + 1`이 updater function. React가 이 함수를 대기열에 추가함

대기열 처리:

- "`5`로 교체": `n`이 `0`(미사용), `5`를 반환
- `n => n + 1`: `n`이 `5`, `6`을 반환

React가 `6`을 최종 결과로 저장함

### state를 갱신한 후 교체하면 어떻게 되는가

```js
<button onClick={() => {
  setNumber(number + 5);
  setNumber(n => n + 1);
  setNumber(42);
}}>
```

대기열 처리:

- "`5`로 교체": `5`를 반환
- `n => n + 1`: `n`이 `5`, `6`을 반환
- "`42`로 교체": `42`를 반환

React가 `42`를 최종 결과로 저장함

정리:

- **updater function** (예: `n => n + 1`): 대기열에 추가됨
- **그 외 값** (예: 숫자 `5`): "N으로 교체"가 대기열에 추가되며, 이미 대기열에 있는 것을 무시함

이벤트 핸들러가 완료된 후 React가 리렌더링을 촉발하고, 리렌더링 중에 대기열을 처리함. updater function은 렌더링 중에 실행되므로 **순수해야** 하며, 결과만 반환해야 함. 내부에서 state를 설정하거나 다른 부수 효과를 실행하면 안 됨.

### 네이밍 관례

- updater function 인수를 해당 state 변수의 첫 글자로 이름 짓는 것이 일반적임:

```js
setEnabled(e => !e);
setLastName(ln => ln.reverse());
setFriendCount(fc => fc * 2);
```

- 더 상세하게 쓰려면: `setEnabled(enabled => !enabled)` 또는 `setEnabled(prevEnabled => !prevEnabled)`

### 요약

- state를 설정해도 기존 렌더의 변수는 변경되지 않으며, 새 렌더를 요청함
- React는 이벤트 핸들러가 완료된 후에 state 갱신을 처리함. 이를 배칭이라 부름
- 한 이벤트에서 같은 state를 여러 번 갱신하려면 `setNumber(n => n + 1)` updater function을 사용함

### 챌린지 -- 요청 카운터 수정

```js
import { useState } from 'react';

export default function RequestTracker() {
  const [pending, setPending] = useState(0);
  const [completed, setCompleted] = useState(0);

  async function handleClick() {
    setPending(p => p + 1);
    await delay(3000);
    setPending(p => p - 1);
    setCompleted(c => c + 1);
  }

  return (
    <>
      <h3>
        Pending: {pending}
      </h3>
      <h3>
        Completed: {completed}
      </h3>
      <button onClick={handleClick}>
        Buy
      </button>
    </>
  );
}

function delay(ms) {
  return new Promise(resolve => {
    setTimeout(resolve, ms);
  });
}
```

- updater function을 사용하면 클릭 시점의 state가 아닌 **최신** state에 대해 증감 연산을 수행함

### 챌린지 -- state 대기열 직접 구현하기

```js
export function getFinalState(baseState, queue) {
  let finalState = baseState;

  for (let update of queue) {
    if (typeof update === 'function') {
      // updater function 적용
      finalState = update(finalState);
    } else {
      // state 교체
      finalState = update;
    }
  }

  return finalState;
}
```

- 이것이 React가 최종 state를 계산하는 데 사용하는 정확한 알고리즘임

---

## state에서 객체 갱신하기 (Updating Objects in State)

> 원문: https://react.dev/learn/updating-objects-in-state

- state는 객체를 포함한 모든 종류의 JavaScript 값을 담을 수 있음
- 하지만 React state에 있는 객체를 직접 변경하면 안 됨
- 객체를 갱신하려면 새 객체를 생성하고(또는 기존 객체의 복사본을 만들고), state가 그 복사본을 사용하도록 설정해야 함

### 변이(mutation)란 무엇인가

- 숫자, 문자열, 불리언 같은 JavaScript 원시값은 "불변(immutable)"임 -- 변경 불가능하거나 "읽기 전용"임
- 리렌더링을 촉발하여 값을 교체할 수 있음

```js
const [x, setX] = useState(0);
setX(5); // x state가 0에서 5로 변경됨. 숫자 0 자체는 변하지 않음
```

- state의 객체는 기술적으로 변경 가능하지만, 불변인 것처럼 취급해야 함
- 객체의 내용을 직접 변경하는 것을 **변이(mutation)**라 부름

```js
const [position, setPosition] = useState({ x: 0, y: 0 });
position.x = 5; // 변이 -- 하지 말아야 함
```

### state를 읽기 전용으로 취급하기

- state에 넣는 모든 JavaScript 객체를 읽기 전용으로 취급해야 함

잘못된 예 -- 직접 변이:

```js
onPointerMove={e => {
  position.x = e.clientX;
  position.y = e.clientY;
}}
```

- 이전 렌더에서 `position`에 할당된 객체를 수정함
- state 설정 함수를 사용하지 않아 React가 객체가 변경되었음을 인지하지 못함
- 이미 식사를 마친 후 주문을 변경하려는 것과 같음

올바른 예 -- 새 객체 생성:

```js
onPointerMove={e => {
  setPosition({
    x: e.clientX,
    y: e.clientY
  });
}}
```

- `setPosition`으로 React에 지시하는 내용:
  - `position`을 이 새 객체로 교체함
  - 이 컴포넌트를 다시 렌더링함

완전한 동작 예제:

```js
import { useState } from 'react';

export default function MovingDot() {
  const [position, setPosition] = useState({
    x: 0,
    y: 0
  });
  return (
    <div
      onPointerMove={e => {
        setPosition({
          x: e.clientX,
          y: e.clientY
        });
      }}
      style={{
        position: 'relative',
        width: '100vw',
        height: '100vh',
      }}>
      <div style={{
        position: 'absolute',
        backgroundColor: 'red',
        borderRadius: '50%',
        transform: `translate(${position.x}px, ${position.y}px)`,
        left: -10,
        top: -10,
        width: 20,
        height: 20,
      }} />
    </div>
  );
}
```

### 지역 변이는 괜찮음 (심화)

- **기존** state 객체를 수정하는 것이 문제임
- **방금 생성한** 새 객체를 변경하는 것은 완전히 괜찮음 -- 아직 다른 코드가 참조하지 않기 때문임

```js
const nextPosition = {};
nextPosition.x = e.clientX;
nextPosition.y = e.clientY;
setPosition(nextPosition);
```

- 위 코드는 아래와 완전히 동일함:

```js
setPosition({
  x: e.clientX,
  y: e.clientY
});
```

- 이미 state에 있는 기존 객체를 변경할 때만 문제가 됨
- 방금 생성한 객체를 변경하는 것은 "지역 변이"라 불리며, 렌더링 중에도 가능함

### 전개 구문으로 객체 복사하기

- 기존 데이터를 새 객체에 포함시키고 싶을 때가 많음
- 폼에서 하나의 필드만 갱신하고 나머지는 이전 값을 유지해야 하는 경우

잘못된 예 -- 직접 변이:

```js
function handleFirstNameChange(e) {
  person.firstName = e.target.value;
}
```

올바른 예 -- 전개 구문 사용:

```js
setPerson({
  ...person, // 이전 필드를 복사
  firstName: e.target.value // 이 필드만 재정의
});
```

동작하는 폼 예제:

```js
import { useState } from 'react';

export default function Form() {
  const [person, setPerson] = useState({
    firstName: 'Barbara',
    lastName: 'Hepworth',
    email: 'bhepworth@sculpture.com'
  });

  function handleFirstNameChange(e) {
    setPerson({
      ...person,
      firstName: e.target.value
    });
  }

  function handleLastNameChange(e) {
    setPerson({
      ...person,
      lastName: e.target.value
    });
  }

  function handleEmailChange(e) {
    setPerson({
      ...person,
      email: e.target.value
    });
  }

  return (
    <>
      <label>
        First name:
        <input
          value={person.firstName}
          onChange={handleFirstNameChange}
        />
      </label>
      <label>
        Last name:
        <input
          value={person.lastName}
          onChange={handleLastNameChange}
        />
      </label>
      <label>
        Email:
        <input
          value={person.email}
          onChange={handleEmailChange}
        />
      </label>
      <p>
        {person.firstName}{' '}
        {person.lastName}{' '}
        ({person.email})
      </p>
    </>
  );
}
```

참고:

- `...` 전개 구문은 "얕은(shallow)" 복사임 -- 한 단계 깊이만 복사함
- 빠르지만, 중첩 속성을 갱신하려면 여러 번 사용해야 함

### 동적 이름으로 단일 이벤트 핸들러 사용하기 (심화)

- `[` `]` 대괄호를 사용하여 동적 이름의 속성을 지정할 수 있음

```js
function handleChange(e) {
  setPerson({
    ...person,
    [e.target.name]: e.target.value
  });
}
```

- `e.target.name`이 `<input>` DOM 요소의 `name` 속성을 참조함

### 중첩 객체 갱신하기

```js
const [person, setPerson] = useState({
  name: 'Niki de Saint Phalle',
  artwork: {
    title: 'Blue Nana',
    city: 'Hamburg',
    image: 'https://react.dev/images/docs/scientists/Sd1AgUOm.jpg',
  }
});
```

변이로 하면 간단하지만 React에서는 해서는 안 됨:

```js
person.artwork.city = 'New Delhi'; // 변이 -- 금지
```

올바른 방법 -- 안쪽부터 바깥쪽까지 새 객체를 생성함:

```js
setPerson({
  ...person,
  artwork: {
    ...person.artwork,
    city: 'New Delhi'
  }
});
```

완전한 중첩 객체 갱신 예제:

```js
import { useState } from 'react';

export default function Form() {
  const [person, setPerson] = useState({
    name: 'Niki de Saint Phalle',
    artwork: {
      title: 'Blue Nana',
      city: 'Hamburg',
      image: 'https://react.dev/images/docs/scientists/Sd1AgUOm.jpg',
    }
  });

  function handleNameChange(e) {
    setPerson({
      ...person,
      name: e.target.value
    });
  }

  function handleTitleChange(e) {
    setPerson({
      ...person,
      artwork: {
        ...person.artwork,
        title: e.target.value
      }
    });
  }

  function handleCityChange(e) {
    setPerson({
      ...person,
      artwork: {
        ...person.artwork,
        city: e.target.value
      }
    });
  }

  function handleImageChange(e) {
    setPerson({
      ...person,
      artwork: {
        ...person.artwork,
        image: e.target.value
      }
    });
  }

  return (
    <>
      <label>
        Name:
        <input
          value={person.name}
          onChange={handleNameChange}
        />
      </label>
      <label>
        Title:
        <input
          value={person.artwork.title}
          onChange={handleTitleChange}
        />
      </label>
      <label>
        City:
        <input
          value={person.artwork.city}
          onChange={handleCityChange}
        />
      </label>
      <label>
        Image:
        <input
          value={person.artwork.image}
          onChange={handleImageChange}
        />
      </label>
      <p>
        <i>{person.artwork.title}</i>
        {' by '}
        {person.name}
        <br />
        (located in {person.artwork.city})
      </p>
      <img
        src={person.artwork.image}
        alt={person.artwork.title}
      />
    </>
  );
}
```

### 객체는 실제로 중첩되어 있지 않음 (심화)

- 코드에서는 "중첩"으로 보이지만, 실행 시에는 두 개의 별개 객체임

```js
let obj1 = {
  title: 'Blue Nana',
  city: 'Hamburg',
  image: 'https://react.dev/images/docs/scientists/Sd1AgUOm.jpg',
};

let obj2 = {
  name: 'Niki de Saint Phalle',
  artwork: obj1
};
```

- `obj1`은 `obj2` "안에" 있지 않음
- `obj3`도 `obj1`을 "가리킬" 수 있음

```js
let obj3 = {
  name: 'Copycat',
  artwork: obj1
};
```

- `obj3.artwork.city`를 변이하면 `obj2.artwork.city`와 `obj1.city` 모두 영향을 받음
- 이 세 참조가 같은 객체를 가리키기 때문임
- "중첩"이 아니라 속성으로 서로를 "가리키는" 별개의 객체로 생각하는 것이 정확함

### Immer로 간결한 갱신 로직 작성하기

- state가 깊게 중첩되어 있으면 평탄화를 고려할 수 있음
- state 구조를 바꾸고 싶지 않으면 중첩 전개의 단축어로 Immer를 사용할 수 있음
- Immer는 변이 구문으로 작성하되 자동으로 복사본을 생성해주는 라이브러리임

```js
updatePerson(draft => {
  draft.artwork.city = 'Lagos';
});
```

- 일반 변이와 달리 이전 state를 덮어쓰지 않음

Immer 동작 원리:

- Immer가 제공하는 `draft`는 Proxy라는 특별한 객체로, 수행한 작업을 "기록"함
- 내부적으로 `draft`의 어떤 부분이 변경되었는지 파악하고, 편집 내용을 포함한 완전히 새로운 객체를 생성함

Immer 사용 방법:

1. `npm install use-immer`로 의존성 추가
2. `import { useState } from 'react'`를 `import { useImmer } from 'use-immer'`로 교체

```js
import { useImmer } from 'use-immer';

export default function Form() {
  const [person, updatePerson] = useImmer({
    name: 'Niki de Saint Phalle',
    artwork: {
      title: 'Blue Nana',
      city: 'Hamburg',
      image: 'https://react.dev/images/docs/scientists/Sd1AgUOm.jpg',
    }
  });

  function handleNameChange(e) {
    updatePerson(draft => {
      draft.name = e.target.value;
    });
  }

  function handleTitleChange(e) {
    updatePerson(draft => {
      draft.artwork.title = e.target.value;
    });
  }

  function handleCityChange(e) {
    updatePerson(draft => {
      draft.artwork.city = e.target.value;
    });
  }

  function handleImageChange(e) {
    updatePerson(draft => {
      draft.artwork.image = e.target.value;
    });
  }

  return (
    <>
      <label>
        Name:
        <input
          value={person.name}
          onChange={handleNameChange}
        />
      </label>
      <label>
        Title:
        <input
          value={person.artwork.title}
          onChange={handleTitleChange}
        />
      </label>
      <label>
        City:
        <input
          value={person.artwork.city}
          onChange={handleCityChange}
        />
      </label>
      <label>
        Image:
        <input
          value={person.artwork.image}
          onChange={handleImageChange}
        />
      </label>
      <p>
        <i>{person.artwork.title}</i>
        {' by '}
        {person.name}
        <br />
        (located in {person.artwork.city})
      </p>
      <img
        src={person.artwork.image}
        alt={person.artwork.title}
      />
    </>
  );
}
```

- `useState`와 `useImmer`를 한 컴포넌트에서 자유롭게 섞어 쓸 수 있음
- 중첩이 있고 객체 복사로 반복 코드가 생길 때 이벤트 핸들러를 간결하게 유지하는 좋은 방법임

### React에서 state 변이가 권장되지 않는 이유 (심화)

- **디버깅**: `console.log`를 사용할 때 state를 변이하지 않으면 과거 로그가 최근 state 변경으로 덮어씌워지지 않아 렌더 간 state 변화를 명확히 볼 수 있음
- **최적화**: React의 일반적인 최적화 전략은 이전 props/state가 다음 것과 같은지 확인하여 작업을 건너뜀. state를 변이하지 않으면 변경 여부 확인이 매우 빠름. `prevObj === obj`이면 내부가 변경되지 않았음을 확신할 수 있음
- **새 기능**: 개발 중인 새 React 기능들은 state가 스냅샷처럼 취급되는 것에 의존함
- **요구사항 변경**: 실행 취소/다시 실행, 변경 내역 표시, 폼을 이전 값으로 리셋 같은 기능은 아무것도 변이하지 않을 때 구현이 쉬움
- **더 단순한 구현**: React는 변이에 의존하지 않으므로 객체에 특별한 처리가 필요 없음. Proxy로 감싸거나 초기화 시 다른 작업을 할 필요 없음

### 요약

- React의 모든 state를 불변으로 취급함
- 객체를 state에 저장할 때 변이해도 렌더가 촉발되지 않으며, 이전 렌더 "스냅샷"의 state가 변경됨
- 객체를 변이하는 대신 새 버전을 생성하고 state를 설정하여 리렌더링을 촉발함
- `{...obj, something: 'newValue'}` 객체 전개 구문으로 객체 복사본을 만들 수 있음
- 전개 구문은 얕은 복사임: 한 단계 깊이만 복사함
- 중첩 객체를 갱신하려면 갱신하는 위치에서 위로 올라가며 복사본을 만들어야 함
- 반복적인 복사 코드를 줄이려면 Immer를 사용함

---

## state에서 배열 갱신하기 (Updating Arrays in State)

> 원문: https://react.dev/learn/updating-arrays-in-state

- 배열은 JavaScript에서 변경 가능하지만, state에 저장할 때는 불변으로 취급해야 함
- 객체와 마찬가지로 state에 저장된 배열을 갱신하려면 새 배열을 생성하고(또는 기존 배열의 복사본을 만들고) state가 새 배열을 사용하도록 설정해야 함

### 변이 없이 배열 갱신하기

- **React state의 배열을 읽기 전용으로 취급해야 함**
- `arr[0] = 'bird'` 같은 재할당이나 `push()`, `pop()` 같은 변이 메서드를 사용하면 안 됨
- 배열을 갱신할 때마다 state 설정 함수에 새 배열을 전달해야 함
- 원본 배열의 `filter()`, `map()` 같은 비변이 메서드를 호출하여 새 배열을 만들 수 있음

일반적인 배열 연산 참조:

- 추가:
  - 피할 것 (변이): `push`, `unshift`
  - 선호할 것 (새 배열 반환): `concat`, `[...arr]` 전개 구문
- 삭제:
  - 피할 것: `pop`, `shift`, `splice`
  - 선호할 것: `filter`, `slice`
- 교체:
  - 피할 것: `splice`, `arr[i] = ...` 할당
  - 선호할 것: `map`
- 정렬:
  - 피할 것: `reverse`, `sort`
  - 선호할 것: 배열을 먼저 복사

- Immer를 사용하면 양쪽 열의 메서드를 모두 사용할 수 있음

`slice`와 `splice`의 차이:

- `slice`: 배열 또는 그 일부를 복사함
- `splice`: 배열을 **변이**함 (항목 삽입 또는 삭제)
- React에서는 `slice`(p 없음)를 훨씬 자주 사용함

### 배열에 추가하기

잘못된 예 -- `push()` 사용:

```js
<button onClick={() => {
  artists.push({
    id: nextId++,
    name: name,
  });
}}>Add</button>
```

- `push()`가 원본 배열을 변이하므로 동작하지 않음

올바른 예 -- 전개 구문 사용:

```js
setArtists([
  ...artists,
  { id: nextId++, name: name }
]);
```

완전한 예제:

```js
import { useState } from 'react';

let nextId = 0;

export default function List() {
  const [name, setName] = useState('');
  const [artists, setArtists] = useState([]);

  return (
    <>
      <h1>Inspiring sculptors:</h1>
      <input
        value={name}
        onChange={e => setName(e.target.value)}
      />
      <button onClick={() => {
        setArtists([
          ...artists,
          { id: nextId++, name: name }
        ]);
      }}>Add</button>
      <ul>
        {artists.map(artist => (
          <li key={artist.id}>{artist.name}</li>
        ))}
      </ul>
    </>
  );
}
```

앞에 추가하기:

```js
setArtists([
  { id: nextId++, name: name },
  ...artists // 기존 항목을 뒤에 배치
]);
```

- 전개 구문으로 `push()`(끝에 추가)와 `unshift()`(앞에 추가) 역할을 모두 수행할 수 있음

### 배열에서 삭제하기

- 배열에서 항목을 제거하는 가장 쉬운 방법은 해당 항목을 **필터링**하는 것임
- 해당 항목을 포함하지 않는 새 배열을 생성함

```js
import { useState } from 'react';

let initialArtists = [
  { id: 0, name: 'Marta Colvin Andrade' },
  { id: 1, name: 'Lamidi Olonade Fakeye'},
  { id: 2, name: 'Louise Nevelson'},
];

export default function List() {
  const [artists, setArtists] = useState(
    initialArtists
  );

  return (
    <>
      <h1>Inspiring sculptors:</h1>
      <ul>
        {artists.map(artist => (
          <li key={artist.id}>
            {artist.name}{' '}
            <button onClick={() => {
              setArtists(
                artists.filter(a =>
                  a.id !== artist.id
                )
              );
            }}>
              Delete
            </button>
          </li>
        ))}
      </ul>
    </>
  );
}
```

- `artists.filter(a => a.id !== artist.id)`는 "해당 `artist`의 ID와 다른 ID를 가진 `artists`로 구성된 배열을 생성하라"는 의미임
- `filter`는 원본 배열을 수정하지 않음

### 배열 변환하기

- 배열의 일부 또는 전체 항목을 변경하려면 `map()`을 사용하여 새 배열을 생성함

```js
import { useState } from 'react';

let initialShapes = [
  { id: 0, type: 'circle', x: 50, y: 100 },
  { id: 1, type: 'square', x: 150, y: 100 },
  { id: 2, type: 'circle', x: 250, y: 100 },
];

export default function ShapeEditor() {
  const [shapes, setShapes] = useState(
    initialShapes
  );

  function handleClick() {
    const nextShapes = shapes.map(shape => {
      if (shape.type === 'square') {
        return shape;
      } else {
        return {
          ...shape,
          y: shape.y + 50,
        };
      }
    });
    setShapes(nextShapes);
  }

  return (
    <>
      <button onClick={handleClick}>
        Move circles down!
      </button>
      {shapes.map(shape => (
        <div
          key={shape.id}
          style={{
          background: 'purple',
          position: 'absolute',
          left: shape.x,
          top: shape.y,
          borderRadius:
            shape.type === 'circle'
              ? '50%' : '',
          width: 20,
          height: 20,
        }} />
      ))}
    </>
  );
}
```

### 배열에서 항목 교체하기

- 배열의 하나 이상의 항목을 교체하는 것이 특히 흔함
- `arr[0] = 'bird'` 같은 할당은 원본 배열을 변이하므로, 이것도 `map`을 사용해야 함
- `map` 호출 안에서 두 번째 인수로 항목의 인덱스를 받을 수 있음

```js
import { useState } from 'react';

let initialCounters = [
  0, 0, 0
];

export default function CounterList() {
  const [counters, setCounters] = useState(
    initialCounters
  );

  function handleIncrementClick(index) {
    const nextCounters = counters.map((c, i) => {
      if (i === index) {
        return c + 1;
      } else {
        return c;
      }
    });
    setCounters(nextCounters);
  }

  return (
    <ul>
      {counters.map((counter, i) => (
        <li key={i}>
          {counter}
          <button onClick={() => {
            handleIncrementClick(i);
          }}>+1</button>
        </li>
      ))}
    </ul>
  );
}
```

### 배열에 삽입하기

- 시작이나 끝이 아닌 특정 위치에 항목을 삽입하고 싶을 때가 있음
- `...` 배열 전개 구문과 `slice()` 메서드를 함께 사용함
- `slice()`로 배열의 "조각"을 잘라낼 수 있음

```js
import { useState } from 'react';

let nextId = 3;
const initialArtists = [
  { id: 0, name: 'Marta Colvin Andrade' },
  { id: 1, name: 'Lamidi Olonade Fakeye'},
  { id: 2, name: 'Louise Nevelson'},
];

export default function List() {
  const [name, setName] = useState('');
  const [artists, setArtists] = useState(
    initialArtists
  );

  function handleClick() {
    const insertAt = 1;
    const nextArtists = [
      // 삽입 지점 앞의 항목들:
      ...artists.slice(0, insertAt),
      // 새 항목:
      { id: nextId++, name: name },
      // 삽입 지점 뒤의 항목들:
      ...artists.slice(insertAt)
    ];
    setArtists(nextArtists);
    setName('');
  }

  return (
    <>
      <h1>Inspiring sculptors:</h1>
      <input
        value={name}
        onChange={e => setName(e.target.value)}
      />
      <button onClick={handleClick}>
        Insert
      </button>
      <ul>
        {artists.map(artist => (
          <li key={artist.id}>{artist.name}</li>
        ))}
      </ul>
    </>
  );
}
```

### 배열에 기타 변경 가하기

- 전개 구문과 `map()`, `filter()` 같은 비변이 메서드만으로는 할 수 없는 작업이 있음
  - 예: 배열 뒤집기, 정렬
- JavaScript의 `reverse()`와 `sort()` 메서드는 원본 배열을 변이함
- **먼저 배열을 복사한 다음** 변경하면 됨

```js
import { useState } from 'react';

const initialList = [
  { id: 0, title: 'Big Bellies' },
  { id: 1, title: 'Lunar Landscape' },
  { id: 2, title: 'Terracotta Army' },
];

export default function List() {
  const [list, setList] = useState(initialList);

  function handleClick() {
    const nextList = [...list];
    nextList.reverse();
    setList(nextList);
  }

  return (
    <>
      <button onClick={handleClick}>
        Reverse
      </button>
      <ul>
        {list.map(artwork => (
          <li key={artwork.id}>{artwork.title}</li>
        ))}
      </ul>
    </>
  );
}
```

- `[...list]` 전개 구문으로 원본 배열의 복사본을 먼저 생성함
- 복사본이 있으면 `nextList.reverse()`나 `nextList.sort()` 같은 변이 메서드를 사용하거나 `nextList[0] = "something"`으로 개별 항목을 할당할 수 있음

얕은 복사 제한사항:

- **배열을 복사해도 내부의 기존 항목을 직접 변이할 수는 없음**
- 복사가 얕기 때문임 -- 새 배열이 원본과 같은 항목을 포함함

```js
const nextList = [...list];
nextList[0].seen = true; // 문제: list[0]을 변이함
setList(nextList);
```

- `nextList`와 `list`는 다른 배열이지만, `nextList[0]`과 `list[0]`은 같은 객체를 가리킴
- 변경하고 싶은 개별 항목을 변이 대신 복사해야 함

### 배열 안의 객체 갱신하기

- 객체는 배열 "안에" 위치한 것이 아님. 코드에서는 "안에" 있는 것처럼 보이지만, 배열의 각 객체는 별개의 값이며 배열이 그것을 "가리킴"
- `list[0]` 같은 중첩 필드를 변경할 때 주의해야 함

잘못된 예 -- 공유된 state의 변이:

```js
const myNextList = [...myList];
const artwork = myNextList.find(a => a.id === artworkId);
artwork.seen = nextSeen; // 문제: 기존 항목을 변이함
setMyList(myNextList);
```

- `myNextList` 배열 자체는 새것이지만 **항목 자체**는 원본 `myList` 배열의 것과 같음
- `artwork.seen`을 변경하면 **원본** 작품 항목이 변경됨

올바른 예 -- `map()`과 객체 전개 사용:

```js
setMyList(myList.map(artwork => {
  if (artwork.id === artworkId) {
    // 변경사항이 있는 *새* 객체 생성
    return { ...artwork, seen: nextSeen };
  } else {
    // 변경 없음
    return artwork;
  }
}));
```

완전한 예제:

```js
import { useState } from 'react';

let nextId = 3;
const initialList = [
  { id: 0, title: 'Big Bellies', seen: false },
  { id: 1, title: 'Lunar Landscape', seen: false },
  { id: 2, title: 'Terracotta Army', seen: true },
];

export default function BucketList() {
  const [myList, setMyList] = useState(initialList);
  const [yourList, setYourList] = useState(
    initialList
  );

  function handleToggleMyList(artworkId, nextSeen) {
    setMyList(myList.map(artwork => {
      if (artwork.id === artworkId) {
        return { ...artwork, seen: nextSeen };
      } else {
        return artwork;
      }
    }));
  }

  function handleToggleYourList(artworkId, nextSeen) {
    setYourList(yourList.map(artwork => {
      if (artwork.id === artworkId) {
        return { ...artwork, seen: nextSeen };
      } else {
        return artwork;
      }
    }));
  }

  return (
    <>
      <h1>Art Bucket List</h1>
      <h2>My list of art to see:</h2>
      <ItemList
        artworks={myList}
        onToggle={handleToggleMyList} />
      <h2>Your list of art to see:</h2>
      <ItemList
        artworks={yourList}
        onToggle={handleToggleYourList} />
    </>
  );
}

function ItemList({ artworks, onToggle }) {
  return (
    <ul>
      {artworks.map(artwork => (
        <li key={artwork.id}>
          <label>
            <input
              type="checkbox"
              checked={artwork.seen}
              onChange={e => {
                onToggle(
                  artwork.id,
                  e.target.checked
                );
              }}
            />
            {artwork.title}
          </label>
        </li>
      ))}
    </ul>
  );
}
```

### Immer로 간결한 갱신 로직 작성하기

- 변이 없이 중첩 배열을 갱신하는 것은 다소 반복적일 수 있음
- state 객체가 매우 깊으면 평탄하게 재구성하는 것을 고려할 수 있음
- state 구조를 바꾸고 싶지 않으면 Immer를 사용할 수 있음

```js
import { useState } from 'react';
import { useImmer } from 'use-immer';

let nextId = 3;
const initialList = [
  { id: 0, title: 'Big Bellies', seen: false },
  { id: 1, title: 'Lunar Landscape', seen: false },
  { id: 2, title: 'Terracotta Army', seen: true },
];

export default function BucketList() {
  const [myList, updateMyList] = useImmer(
    initialList
  );
  const [yourList, updateYourList] = useImmer(
    initialList
  );

  function handleToggleMyList(id, nextSeen) {
    updateMyList(draft => {
      const artwork = draft.find(a =>
        a.id === id
      );
      artwork.seen = nextSeen;
    });
  }

  function handleToggleYourList(artworkId, nextSeen) {
    updateYourList(draft => {
      const artwork = draft.find(a =>
        a.id === artworkId
      );
      artwork.seen = nextSeen;
    });
  }

  return (
    <>
      <h1>Art Bucket List</h1>
      <h2>My list of art to see:</h2>
      <ItemList
        artworks={myList}
        onToggle={handleToggleMyList} />
      <h2>Your list of art to see:</h2>
      <ItemList
        artworks={yourList}
        onToggle={handleToggleYourList} />
    </>
  );
}

function ItemList({ artworks, onToggle }) {
  return (
    <ul>
      {artworks.map(artwork => (
        <li key={artwork.id}>
          <label>
            <input
              type="checkbox"
              checked={artwork.seen}
              onChange={e => {
                onToggle(
                  artwork.id,
                  e.target.checked
                );
              }}
            />
            {artwork.title}
          </label>
        </li>
      ))}
    </ul>
  );
}
```

- Immer를 사용하면 `artwork.seen = nextSeen` 같은 변이가 허용됨
- 원본 state를 변이하는 것이 아니라 Immer가 제공하는 특별한 `draft` 객체를 변이하기 때문임
- `push()`나 `pop()` 같은 변이 메서드도 `draft` 내용에 적용할 수 있음
- 내부적으로 Immer는 `draft`에 가해진 변경에 따라 항상 다음 state를 처음부터 구성함
- 이벤트 핸들러를 매우 간결하게 유지하면서 state를 절대 변이하지 않음

Immer에서 변이와 비변이 접근법을 섞어 쓸 수도 있음:

```js
function handleAddTodo(title) {
  updateTodos(draft => {
    draft.push({
      id: nextId++,
      title: title,
      done: false
    });
  });
}

function handleChangeTodo(nextTodo) {
  updateTodos(todos.map(todo => {
    if (todo.id === nextTodo.id) {
      return nextTodo;
    } else {
      return todo;
    }
  }));
}

function handleDeleteTodo(todoId) {
  updateTodos(
    todos.filter(t => t.id !== todoId)
  );
}
```

### 요약

- 배열을 state에 넣을 수 있지만, 변경할 수 없음
- 배열을 변이하는 대신 새 버전을 만들고 state를 갱신함
- `[...arr, newItem]` 배열 전개 구문으로 새 항목이 포함된 배열을 만들 수 있음
- `filter()`와 `map()`으로 필터링되거나 변환된 항목이 있는 새 배열을 만들 수 있음
- Immer를 사용하면 코드를 간결하게 유지할 수 있음

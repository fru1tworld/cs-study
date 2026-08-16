# 탈출구 (Escape Hatches)

## 개요

> 원문: https://react.dev/learn/escape-hatches

- 일부 컴포넌트는 React 외부 시스템과 제어/동기화가 필요함
  - 브라우저 API로 input 포커싱
  - React 없이 구현된 비디오 플레이어 재생/일시정지
  - 원격 서버 연결 및 메시지 수신
- 이 장에서 다루는 "탈출구"는 React 바깥으로 나가 외부 시스템과 연결하는 방법임
- 애플리케이션 로직과 데이터 흐름 대부분은 이 기능에 의존하지 않아야 함

### 이 장에서 배울 내용

- 리렌더링 없이 정보를 "기억"하는 방법
- React가 관리하는 DOM 요소에 접근하는 방법
- 컴포넌트를 외부 시스템과 동기화하는 방법
- 컴포넌트에서 불필요한 Effect를 제거하는 방법
- Effect의 생명주기가 컴포넌트와 다른 점
- 값이 Effect를 다시 트리거하지 않도록 방지하는 방법
- Effect 재실행 빈도를 줄이는 방법
- 컴포넌트 간 로직을 공유하는 방법

---

## Ref로 값 참조하기 (Referencing Values with Refs)

> 원문: https://react.dev/learn/referencing-values-with-refs

### ref 추가하기

- 컴포넌트가 정보를 "기억"하되 새로운 렌더링을 트리거하지 않으려면 ref를 사용함

```js
import { useRef } from 'react';
```

- 컴포넌트 내에서 `useRef` Hook을 호출하고 초기값을 인자로 전달함

```js
const ref = useRef(0);
```

- `useRef`는 다음과 같은 객체를 반환함

```js
{
  current: 0 // useRef에 전달한 값
}
```

- `ref.current` 속성으로 현재 값에 접근 가능함
- 이 값은 의도적으로 변경 가능(mutable)하여 읽기와 쓰기가 모두 가능함
- React가 추적하지 않는 컴포넌트의 "비밀 주머니"와 같음 (React의 단방향 데이터 흐름에서의 탈출구)

### 예제: ref를 사용한 카운터

```js
import { useRef } from 'react';

export default function Counter() {
  let ref = useRef(0);

  function handleClick() {
    ref.current = ref.current + 1;
    alert('You clicked ' + ref.current + ' times!');
  }

  return (
    <button onClick={handleClick}>
      Click me!
    </button>
  );
}
```

- ref는 숫자뿐 아니라 문자열, 객체, 함수 등 무엇이든 가리킬 수 있음
- state와 달리 ref는 읽고 수정할 수 있는 `current` 속성을 가진 일반 JavaScript 객체임
- 증가할 때마다 컴포넌트가 리렌더링되지 않음

### 예제: 스톱워치 만들기

- ref와 state를 하나의 컴포넌트에서 함께 사용할 수 있음
- 렌더링에 사용되는 정보는 state에 보관함
- 이벤트 핸들러만 필요하고 변경 시 리렌더링이 불필요한 정보는 ref가 더 효율적임

```js
import { useState, useRef } from 'react';

export default function Stopwatch() {
  const [startTime, setStartTime] = useState(null);
  const [now, setNow] = useState(null);
  const intervalRef = useRef(null);

  function handleStart() {
    setStartTime(Date.now());
    setNow(Date.now());

    clearInterval(intervalRef.current);
    intervalRef.current = setInterval(() => {
      setNow(Date.now());
    }, 10);
  }

  function handleStop() {
    clearInterval(intervalRef.current);
  }

  let secondsPassed = 0;
  if (startTime != null && now != null) {
    secondsPassed = (now - startTime) / 1000;
  }

  return (
    <>
      <h1>Time passed: {secondsPassed.toFixed(3)}</h1>
      <button onClick={handleStart}>
        Start
      </button>
      <button onClick={handleStop}>
        Stop
      </button>
    </>
  );
}
```

### ref와 state의 차이

- `useRef(initialValue)`는 `{ current: initialValue }`를 반환함. `useState(initialValue)`는 state 변수의 현재 값과 setter 함수 `[value, setValue]`를 반환함
- ref는 변경해도 리렌더링을 트리거하지 않음. state는 변경하면 리렌더링을 트리거함
- ref는 변경 가능(mutable)하여 렌더링 과정 밖에서 `current` 값을 수정하고 업데이트할 수 있음. state는 "불변"이어서 state 변수를 수정하려면 setter 함수를 사용해 리렌더링을 큐에 넣어야 함
- ref의 `current` 값은 렌더링 중에 읽거나 쓰면 안 됨. state는 언제든 읽을 수 있지만, 각 렌더링에는 변경되지 않는 자체 state 스냅샷이 있음

### state로 구현한 카운터 (비교)

```js
import { useState } from 'react';

export default function Counter() {
  const [count, setCount] = useState(0);

  function handleClick() {
    setCount(count + 1);
  }

  return (
    <button onClick={handleClick}>
      You clicked {count} times
    </button>
  );
}
```

- `count` 값이 화면에 표시되므로 state 값을 사용하는 것이 적절함
- `setCount()`로 값을 설정하면 React가 컴포넌트를 리렌더링하고 화면이 새 카운트를 반영함

### ref로 구현하면 업데이트가 되지 않음

```js
import { useRef } from 'react';

export default function Counter() {
  let countRef = useRef(0);

  function handleClick() {
    // 컴포넌트를 리렌더링하지 않음!
    countRef.current = countRef.current + 1;
  }

  return (
    <button onClick={handleClick}>
      You clicked {countRef.current} times
    </button>
  );
}
```

- 렌더링 중에 `ref.current`를 읽으면 신뢰할 수 없는 코드가 됨

### useRef의 내부 작동 원리

- `useState`와 `useRef` 모두 React에서 제공하지만, 원칙적으로 `useRef`는 `useState` 위에 구현할 수 있음

```js
// React 내부
function useRef(initialValue) {
  const [ref, unused] = useState({ current: initialValue });
  return ref;
}
```

- 첫 렌더링에서 `{ current: initialValue }`를 반환하고, 이 객체는 React에 의해 저장되어 다음 렌더링에서 같은 객체가 반환됨
- setter는 사용되지 않음. `useRef`는 항상 같은 객체를 반환해야 하기 때문임

### ref를 사용해야 하는 경우

- 컴포넌트가 React "바깥으로 나가" 외부 API와 통신해야 할 때 (컴포넌트 외관에 영향을 미치지 않는 브라우저 API)
  - timeout ID 저장
  - DOM 요소 저장 및 조작
  - JSX 계산에 필요하지 않은 다른 객체 저장

### ref 사용 모범 사례

- ref를 탈출구로 취급할 것. 외부 시스템이나 브라우저 API로 작업할 때 유용함. 애플리케이션 로직과 데이터 흐름 대부분이 ref에 의존한다면 접근 방식을 재고해야 함
- 렌더링 중에 `ref.current`를 읽거나 쓰지 말 것. 렌더링 중에 정보가 필요하면 state를 사용할 것. 유일한 예외는 `if (!ref.current) ref.current = new Thing()`처럼 첫 렌더링에서만 ref를 설정하는 코드임
- ref는 일반 JavaScript 객체이므로 즉시 변경됨

```js
ref.current = 5;
console.log(ref.current); // 5
```

- ref를 사용할 때 변이를 피할 필요가 없음. 변이되는 객체가 렌더링에 사용되지 않는 한 React는 ref나 그 내용을 어떻게 하든 관여하지 않음

### ref와 DOM

- ref의 가장 일반적인 사용 사례는 DOM 요소에 접근하는 것임
- JSX에서 `<div ref={myRef}>`처럼 ref를 `ref` 속성에 전달하면, React는 해당 DOM 요소를 `myRef.current`에 넣음
- 요소가 DOM에서 제거되면 React는 `myRef.current`를 `null`로 업데이트함

### 도전 과제 1: 깨진 채팅 입력 수정

- 문제: `let timeoutID`는 리렌더링 사이에 "살아남지" 못함. 매 렌더링이 컴포넌트를 처음부터 실행하고 변수를 초기화하기 때문임
- 해결: timeout ID를 ref에 저장

```js
import { useState, useRef } from 'react';

export default function Chat() {
  const [text, setText] = useState('');
  const [isSending, setIsSending] = useState(false);
  const timeoutRef = useRef(null);

  function handleSend() {
    setIsSending(true);
    timeoutRef.current = setTimeout(() => {
      alert('Sent!');
      setIsSending(false);
    }, 3000);
  }

  function handleUndo() {
    setIsSending(false);
    clearTimeout(timeoutRef.current);
  }

  return (
    <>
      <input
        disabled={isSending}
        value={text}
        onChange={e => setText(e.target.value)}
      />
      <button
        disabled={isSending}
        onClick={handleSend}>
        {isSending ? 'Sending...' : 'Send'}
      </button>
      {isSending &&
        <button onClick={handleUndo}>
          Undo
        </button>
      }
    </>
  );
}
```

### 도전 과제 2: 리렌더링되지 않는 컴포넌트 수정

- 문제: ref의 현재 값이 렌더링 출력을 계산하는 데 사용되고 있음
- 해결: ref를 제거하고 state를 대신 사용

```js
import { useState } from 'react';

export default function Toggle() {
  const [isOn, setIsOn] = useState(false);

  return (
    <button onClick={() => {
      setIsOn(!isOn);
    }}>
      {isOn ? 'On' : 'Off'}
    </button>
  );
}
```

### 도전 과제 3: 디바운싱 수정

- 문제: `let timeoutID` 변수가 모든 `DebouncedButton` 컴포넌트에서 공유됨
- 해결: 각 버튼이 자체 ref를 갖도록 timeout을 ref에 저장

```js
import { useRef } from 'react';

function DebouncedButton({ onClick, children }) {
  const timeoutRef = useRef(null);
  return (
    <button onClick={() => {
      clearTimeout(timeoutRef.current);
      timeoutRef.current = setTimeout(() => {
        onClick();
      }, 1000);
    }}>
      {children}
    </button>
  );
}
```

### 도전 과제 4: 최신 state 읽기

- 문제: state는 스냅샷처럼 동작하므로 비동기 작업에서 최신 state를 읽을 수 없음
- 해결: state 변수(렌더링용)와 ref(timeout에서 읽기용) 모두 필요함

```js
import { useState, useRef } from 'react';

export default function Chat() {
  const [text, setText] = useState('');
  const textRef = useRef(text);

  function handleChange(e) {
    setText(e.target.value);
    textRef.current = e.target.value;
  }

  function handleSend() {
    setTimeout(() => {
      alert('Sending: ' + textRef.current);
    }, 3000);
  }

  return (
    <>
      <input
        value={text}
        onChange={handleChange}
      />
      <button
        onClick={handleSend}>
        Send
      </button>
    </>
  );
}
```

---

## Ref로 DOM 조작하기 (Manipulating the DOM with Refs)

> 원문: https://react.dev/learn/manipulating-the-dom-with-refs

- React는 렌더링 출력에 맞게 DOM을 자동으로 업데이트하므로, 컴포넌트가 직접 DOM을 조작할 일이 드묾
- 다만 React가 관리하는 DOM 요소에 접근이 필요한 경우가 있음
  - 노드에 포커스 맞추기
  - 노드로 스크롤하기
  - 노드의 크기와 위치 측정하기
- React에는 이런 작업을 위한 내장 방법이 없으므로 DOM 노드에 대한 ref가 필요함

### DOM 노드에 ref 가져오기

```js
import { useRef } from 'react';

const myRef = useRef(null);

// JSX 태그에 ref 전달
<div ref={myRef}>
```

- `useRef` Hook은 `current`라는 단일 속성을 가진 객체를 반환함
- 처음에 `myRef.current`는 `null`임
- React가 이 `<div>`에 대한 DOM 노드를 생성하면, `myRef.current`에 이 노드에 대한 참조를 넣음
- 이벤트 핸들러에서 이 DOM 노드에 접근하여 내장 브라우저 API를 사용할 수 있음

```js
myRef.current.scrollIntoView();
```

### 예제: 텍스트 입력에 포커스 맞추기

```js
import { useRef } from 'react';

export default function Form() {
  const inputRef = useRef(null);

  function handleClick() {
    inputRef.current.focus();
  }

  return (
    <>
      <input ref={inputRef} />
      <button onClick={handleClick}>
        Focus the input
      </button>
    </>
  );
}
```

구현 단계:
1. `useRef` Hook으로 `inputRef`를 선언
2. `<input ref={inputRef}>`로 전달하여 React가 이 `<input>`의 DOM 노드를 `inputRef.current`에 넣도록 지시
3. `handleClick` 함수에서 `inputRef.current`로 input DOM 노드를 읽고 `focus()`를 호출
4. `handleClick` 이벤트 핸들러를 `<button>`의 `onClick`에 전달

### 예제: 요소로 스크롤하기

- 하나의 컴포넌트에 여러 ref를 가질 수 있음

```js
import { useRef } from 'react';

export default function CatFriends() {
  const firstCatRef = useRef(null);
  const secondCatRef = useRef(null);
  const thirdCatRef = useRef(null);

  function handleScrollToFirstCat() {
    firstCatRef.current.scrollIntoView({
      behavior: 'smooth',
      block: 'nearest',
      inline: 'center'
    });
  }

  function handleScrollToSecondCat() {
    secondCatRef.current.scrollIntoView({
      behavior: 'smooth',
      block: 'nearest',
      inline: 'center'
    });
  }

  function handleScrollToThirdCat() {
    thirdCatRef.current.scrollIntoView({
      behavior: 'smooth',
      block: 'nearest',
      inline: 'center'
    });
  }

  return (
    <>
      <nav>
        <button onClick={handleScrollToFirstCat}>Neo</button>
        <button onClick={handleScrollToSecondCat}>Millie</button>
        <button onClick={handleScrollToThirdCat}>Bella</button>
      </nav>
      <div>
        <ul>
          <li><img src="https://placecats.com/neo/300/200" alt="Neo" ref={firstCatRef} /></li>
          <li><img src="https://placecats.com/millie/200/200" alt="Millie" ref={secondCatRef} /></li>
          <li><img src="https://placecats.com/bella/199/200" alt="Bella" ref={thirdCatRef} /></li>
        </ul>
      </div>
    </>
  );
}
```

### ref 콜백으로 ref 목록 관리하기

- ref 개수가 미리 정해져 있지 않은 경우, Hook을 루프/조건/`map()` 안에서 호출할 수 없으므로 다른 방법이 필요함
- `ref` 속성에 함수를 전달하는 "ref 콜백" 방식을 사용함
- React는 ref를 설정할 때 DOM 노드와 함께 ref 콜백을 호출하고, 정리할 때 콜백이 반환한 정리 함수를 호출함

```js
import { useRef, useState } from "react";

export default function CatFriends() {
  const itemsRef = useRef(null);
  const [catList, setCatList] = useState(setupCatList);

  function scrollToCat(cat) {
    const map = getMap();
    const node = map.get(cat);
    node.scrollIntoView({
      behavior: "smooth",
      block: "nearest",
      inline: "center",
    });
  }

  function getMap() {
    if (!itemsRef.current) {
      itemsRef.current = new Map();
    }
    return itemsRef.current;
  }

  return (
    <>
      <nav>
        <button onClick={() => scrollToCat(catList[0])}>Neo</button>
        <button onClick={() => scrollToCat(catList[5])}>Millie</button>
        <button onClick={() => scrollToCat(catList[8])}>Bella</button>
      </nav>
      <div>
        <ul>
          {catList.map((cat) => (
            <li
              key={cat.id}
              ref={(node) => {
                const map = getMap();
                map.set(cat, node);
                return () => {
                  map.delete(cat);
                };
              }}
            >
              <img src={cat.imageUrl} />
            </li>
          ))}
        </ul>
      </div>
    </>
  );
}
```

- `itemsRef`는 단일 DOM 노드가 아닌 항목 ID에서 DOM 노드로의 Map을 보유함
- 리스트 항목의 ref 콜백이 Map을 관리함

### 다른 컴포넌트의 DOM 노드에 접근하기

- 주의: 다른 컴포넌트의 DOM 노드를 수동으로 조작하면 코드가 취약해짐
- 부모 컴포넌트에서 자식 컴포넌트로 ref를 다른 prop처럼 전달할 수 있음

```js
import { useRef } from 'react';

function MyInput({ ref }) {
  return <input ref={ref} />;
}

export default function MyForm() {
  const inputRef = useRef(null);

  function handleClick() {
    inputRef.current.focus();
  }

  return (
    <>
      <MyInput ref={inputRef} />
      <button onClick={handleClick}>
        Focus the input
      </button>
    </>
  );
}
```

### useImperativeHandle로 API의 일부만 노출하기

- 부모 컴포넌트가 할 수 있는 것을 제한하고 싶은 경우 `useImperativeHandle`을 사용함

```js
import { useRef, useImperativeHandle } from "react";

function MyInput({ ref }) {
  const realInputRef = useRef(null);
  useImperativeHandle(ref, () => ({
    // focus만 노출하고 나머지는 노출하지 않음
    focus() {
      realInputRef.current.focus();
    },
  }));
  return <input ref={realInputRef} />;
};

export default function Form() {
  const inputRef = useRef(null);

  function handleClick() {
    inputRef.current.focus();
  }

  return (
    <>
      <MyInput ref={inputRef} />
      <button onClick={handleClick}>Focus the input</button>
    </>
  );
}
```

- `MyInput` 내부의 `realInputRef`가 실제 input DOM 노드를 보유함
- `useImperativeHandle`은 React에 부모 컴포넌트의 ref 값으로 직접 만든 특수 객체를 제공하도록 지시함
- `Form` 컴포넌트의 `inputRef.current`는 `focus` 메서드만 가짐

### React가 ref를 연결하는 시점

- React의 모든 업데이트는 두 단계로 나뉨
  - **렌더** 단계: React가 컴포넌트를 호출하여 화면에 무엇이 나타나야 하는지 파악함
  - **커밋** 단계: React가 DOM에 변경 사항을 적용함
- 일반적으로 렌더링 중에 ref에 접근하면 안 됨
- React는 커밋 중에 `ref.current`를 설정함. DOM을 업데이트하기 전에 영향받는 `ref.current` 값을 `null`로 설정하고, DOM 업데이트 후 즉시 해당 DOM 노드로 설정함
- 보통 이벤트 핸들러에서 ref에 접근함

### flushSync로 state 업데이트를 동기적으로 플러시하기

- 새 todo를 추가하고 목록의 마지막 자식으로 스크롤하는 코드에서, 항상 마지막으로 추가된 것의 바로 앞 todo로 스크롤되는 문제가 발생함
- React에서 state 업데이트는 큐에 넣어짐. `setTodos`가 DOM을 즉시 업데이트하지 않기 때문임

```js
import { useState, useRef } from 'react';
import { flushSync } from 'react-dom';

export default function TodoList() {
  const listRef = useRef(null);
  const [text, setText] = useState('');
  const [todos, setTodos] = useState(initialTodos);

  function handleAdd() {
    const newTodo = { id: nextId++, text: text };
    flushSync(() => {
      setText('');
      setTodos([ ...todos, newTodo]);
    });
    listRef.current.lastChild.scrollIntoView({
      behavior: 'smooth',
      block: 'nearest'
    });
  }

  return (
    <>
      <button onClick={handleAdd}>Add</button>
      <input value={text} onChange={e => setText(e.target.value)} />
      <ul ref={listRef}>
        {todos.map(todo => (
          <li key={todo.id}>{todo.text}</li>
        ))}
      </ul>
    </>
  );
}
```

- `flushSync`로 감싼 코드가 실행된 직후 React가 DOM을 동기적으로 업데이트하도록 강제함
- 그 결과 스크롤을 시도할 때 마지막 todo가 이미 DOM에 있음

### ref를 이용한 DOM 조작 모범 사례

- ref는 탈출구임. React "바깥으로 나가야" 할 때만 사용할 것
- 포커싱, 스크롤 같은 비파괴적 작업은 문제가 없음
- DOM을 수동으로 수정하려 하면 React가 만드는 변경 사항과 충돌할 위험이 있음
- React가 관리하는 DOM 노드를 변경하지 말 것. React가 업데이트할 이유가 없는 DOM 부분은 안전하게 수정할 수 있음

---

## Effect와 동기화하기 (Synchronizing with Effects)

> 원문: https://react.dev/learn/synchronizing-with-effects

### Effect란 무엇이며 이벤트와 어떻게 다른가

React 컴포넌트 내부의 두 가지 로직 유형:

- **렌더링 코드**: 컴포넌트 최상위에 위치함. props와 state를 가져와 변환하고 화면에 보고 싶은 JSX를 반환함. 순수해야 함
- **이벤트 핸들러**: 컴포넌트 내부의 중첩 함수로, 계산만 하는 것이 아니라 작업을 수행함. 특정 사용자 작업(버튼 클릭, 입력 등)에 의해 발생하는 "부수 효과"를 포함함
- **Effect**: 렌더링 자체에 의해 발생하는 부수 효과를 지정함. 특정 이벤트가 아닌 렌더링이 원인임. 커밋 후 화면 업데이트 이후에 실행됨
  - 채팅에서 메시지를 보내는 것은 이벤트 (사용자가 특정 버튼을 클릭하여 직접 발생함)
  - 서버 연결을 설정하는 것은 Effect (어떤 상호작용이 컴포넌트를 표시하게 했는지와 관계없이 발생해야 함)

### Effect가 필요하지 않을 수 있음

- Effect를 컴포넌트에 서둘러 추가하지 말 것
- Effect는 보통 React 코드 "밖으로 나가" 외부 시스템과 동기화할 때 사용됨 (브라우저 API, 서드파티 위젯, 네트워크 등)
- Effect가 다른 state에 기반해 일부 state만 조정한다면 Effect가 필요하지 않을 수 있음

### Effect 작성법 3단계

**1단계: Effect 선언**

```js
import { useEffect } from 'react';

function MyComponent() {
  useEffect(() => {
    // 모든 렌더링 후에 실행되는 코드
  });
  return <div />;
}
```

- 컴포넌트가 렌더링될 때마다 React가 화면을 업데이트하고 나서 `useEffect` 내부의 코드를 실행함
- `useEffect`는 해당 렌더링이 화면에 반영될 때까지 코드 실행을 "지연"시킴

**비디오 플레이어 예제 (잘못된 접근):**

```js
function VideoPlayer({ src, isPlaying }) {
  const ref = useRef(null);

  if (isPlaying) {
    ref.current.play();  // 렌더링 중에 호출하면 안 됨
  } else {
    ref.current.pause(); // 크래시 발생
  }

  return <video ref={ref} src={src} loop playsInline />;
}
```

- 렌더링 중에 DOM 노드를 조작하려 하기 때문에 잘못됨
- 첫 번째 호출 시 DOM이 아직 존재하지 않음

**올바른 접근: useEffect 사용**

```js
import { useState, useRef, useEffect } from 'react';

function VideoPlayer({ src, isPlaying }) {
  const ref = useRef(null);

  useEffect(() => {
    if (isPlaying) {
      ref.current.play();
    } else {
      ref.current.pause();
    }
  });

  return <video ref={ref} src={src} loop playsInline />;
}

export default function App() {
  const [isPlaying, setIsPlaying] = useState(false);
  return (
    <>
      <button onClick={() => setIsPlaying(!isPlaying)}>
        {isPlaying ? 'Pause' : 'Play'}
      </button>
      <VideoPlayer
        isPlaying={isPlaying}
        src="https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4"
      />
    </>
  );
}
```

**무한 루프 주의:**

```js
const [count, setCount] = useState(0);
useEffect(() => {
  setCount(count + 1); // 무한 루프 발생!
});
```

- Effect는 렌더링의 결과로 실행됨. state 설정은 렌더링을 트리거함
- Effect에서 즉시 state를 설정하는 것은 전원 콘센트를 자기 자신에 꽂는 것과 같음

**2단계: Effect 의존성 지정**

- 기본적으로 Effect는 모든 렌더링 후에 실행됨. 대부분 이것은 원하는 동작이 아님
- 의존성 배열을 두 번째 인자로 추가하여 불필요한 재실행을 건너뛸 수 있음

```js
useEffect(() => {
  // ...
}, []);  // 빈 배열: 마운트 시에만 실행

useEffect(() => {
  if (isPlaying) {
    ref.current.play();
  } else {
    ref.current.pause();
  }
}, [isPlaying]); // isPlaying이 변경될 때만 재실행
```

의존성 배열 동작 차이:
- `useEffect(() => { ... })`: 모든 렌더링 후에 실행됨
- `useEffect(() => { ... }, [])`: 마운트(컴포넌트가 나타날 때)에만 실행됨
- `useEffect(() => { ... }, [a, b])`: 마운트 시, 그리고 a 또는 b가 이전 렌더링과 달라졌을 때 실행됨

의존성을 "선택"할 수 없음. Effect 내부 코드에서 기대하는 것과 의존성이 일치하지 않으면 린트 오류가 발생함

**ref가 의존성 배열에서 제외된 이유:**
- `ref` 객체는 안정적인 정체성(stable identity)을 가짐. React는 같은 `useRef` 호출에서 항상 같은 객체를 반환함을 보장함
- `useState`에서 반환된 `set` 함수도 안정적인 정체성을 가지므로 의존성에서 흔히 생략됨
- 린터가 오류 없이 의존성 생략을 허용한다면 안전함

**3단계: 필요시 정리(cleanup) 추가**

```js
useEffect(() => {
  const connection = createConnection();
  connection.connect();
  return () => connection.disconnect();
}, []);
```

- React는 Effect가 다시 실행되기 전마다, 그리고 컴포넌트가 언마운트될 때 정리 함수를 호출함
- 개발 모드에서 React는 모든 컴포넌트를 한 번 즉시 다시 마운트함. 정리 함수를 올바르게 구현했는지 확인하기 위함임
- 개발 모드에서 3개의 콘솔 로그가 보이는 것이 정상임
  1. "Connecting..."
  2. "Disconnected."
  3. "Connecting..."
- 프로덕션에서는 "Connecting..."만 한 번 출력됨

### 개발 모드에서 Effect가 두 번 실행되는 것을 다루는 방법

- 올바른 질문은 "Effect를 한 번만 실행하는 방법"이 아니라 "다시 마운트된 후에도 작동하도록 Effect를 수정하는 방법"임
- 보통 정리 함수를 구현하는 것이 답임
- 사용자가 Effect가 한 번 실행되는 것(프로덕션)과 설정 -> 정리 -> 설정 순서(개발)를 구분할 수 없어야 함

**주의: ref로 Effect 이중 실행을 방지하려 하지 말 것**

```js
const connectionRef = useRef(null);
useEffect(() => {
  // 이렇게 하면 버그가 해결되지 않음!
  if (!connectionRef.current) {
    connectionRef.current = createConnection();
    connectionRef.current.connect();
  }
}, []);
```

- 이렇게 하면 개발에서 "Connecting..."이 한 번만 보이지만 버그가 해결되지 않음
- 사용자가 다른 곳으로 이동해도 연결이 닫히지 않고, 돌아오면 새 연결이 생성됨

### 일반적인 Effect 패턴

**비-React 위젯 제어:**

```js
useEffect(() => {
  const map = mapRef.current;
  map.setZoomLevel(zoomLevel);
}, [zoomLevel]);
```

- 같은 값으로 두 번 호출해도 아무 일도 하지 않으므로 정리가 필요 없음
- `showModal`처럼 두 번 호출할 수 없는 API는 정리 함수를 구현해야 함

```js
useEffect(() => {
  const dialog = dialogRef.current;
  dialog.showModal();
  return () => dialog.close();
}, []);
```

**이벤트 구독:**

```js
useEffect(() => {
  function handleScroll(e) {
    console.log(window.scrollX, window.scrollY);
  }
  window.addEventListener('scroll', handleScroll);
  return () => window.removeEventListener('scroll', handleScroll);
}, []);
```

**애니메이션 트리거:**

```js
useEffect(() => {
  const node = ref.current;
  node.style.opacity = 1;
  return () => {
    node.style.opacity = 0;
  };
}, []);
```

**데이터 가져오기:**

```js
useEffect(() => {
  let ignore = false;

  async function startFetching() {
    const json = await fetchTodos(userId);
    if (!ignore) {
      setTodos(json);
    }
  }

  startFetching();

  return () => {
    ignore = true;
  };
}, [userId]);
```

- 이미 발생한 네트워크 요청을 "취소"할 수는 없지만, 정리 함수는 더 이상 관련 없는 fetch가 애플리케이션에 계속 영향을 미치지 않도록 보장함
- 개발에서 Network 탭에 두 개의 fetch가 보이는 것은 문제가 없음

**Effect에서 직접 데이터를 가져오는 것의 단점:**
- Effect는 서버에서 실행되지 않음. 초기 서버 렌더링된 HTML에는 데이터 없이 로딩 상태만 포함됨
- 직접 가져오면 "네트워크 워터폴"을 만들기 쉬움
- 직접 가져오면 데이터를 미리 로드하거나 캐시하지 않음
- 경쟁 상태(race condition) 같은 버그에 취약한 보일러플레이트 코드가 많음

**대안:**
- 프레임워크 사용 시 내장 데이터 가져오기 메커니즘 사용
- 클라이언트 측 캐시 사용 또는 구축 (TanStack Query, useSWR, React Router 6.4+ 등)

**분석 데이터 전송:**

```js
useEffect(() => {
  logVisit(url);
}, [url]);
```

- 개발에서 `logVisit`이 모든 URL에 대해 두 번 호출되지만, 이 코드를 그대로 유지하는 것을 권장함
- 프로덕션에서는 중복 방문 로그가 없음

**Effect가 아닌 것: 애플리케이션 초기화**

```js
if (typeof window !== 'undefined') {
  checkAuthToken();
  loadDataFromLocalStorage();
}

function App() {
  // ...
}
```

- 최상위 코드는 컴포넌트가 임포트될 때 한 번 실행됨

**Effect가 아닌 것: 제품 구매**

```js
// 잘못된 방법
useEffect(() => {
  fetch('/api/buy', { method: 'POST' });
}, []);

// 올바른 방법
function handleClick() {
  fetch('/api/buy', { method: 'POST' });
}
```

- 구매는 렌더링이 아닌 특정 상호작용에 의해 발생함. 이벤트 핸들러에 넣어야 함

---

## Effect가 필요하지 않을 수 있음 (You Might Not Need an Effect)

> 원문: https://react.dev/learn/you-might-not-need-an-effect

- Effect는 React 패러다임에서의 탈출구임
- React "바깥으로 나가" 비-React 위젯, 네트워크, 브라우저 DOM 같은 외부 시스템과 동기화하게 해줌
- 외부 시스템이 관련되지 않으면 Effect가 필요 없음
- 불필요한 Effect를 제거하면 코드가 더 따라가기 쉽고, 빠르게 실행되며, 오류가 적어짐

### Effect가 필요하지 않은 경우 1: 렌더링을 위한 데이터 변환

```js
// 나쁜 예: 불필요한 state와 Effect
function Form() {
  const [firstName, setFirstName] = useState('Taylor');
  const [lastName, setLastName] = useState('Swift');
  const [fullName, setFullName] = useState('');
  useEffect(() => {
    setFullName(firstName + ' ' + lastName);
  }, [firstName, lastName]);
}

// 좋은 예: 렌더링 중에 계산
function Form() {
  const [firstName, setFirstName] = useState('Taylor');
  const [lastName, setLastName] = useState('Swift');
  const fullName = firstName + ' ' + lastName;
}
```

- 기존 props나 state에서 계산할 수 있는 것은 state에 넣지 말 것. 렌더링 중에 계산할 것

### Effect가 필요하지 않은 경우 2: 사용자 이벤트 처리

```js
// 나쁜 예
useEffect(() => {
  if (product.isInCart) {
    showNotification(`Added ${product.name} to the shopping cart!`);
  }
}, [product]);

// 좋은 예
function handleBuyClick() {
  addToCart(product);
  showNotification(`Added ${product.name} to the shopping cart!`);
}
```

### 비싼 계산 캐싱: useMemo 사용

```js
// 나쁜 예: Effect에서 state 업데이트
function TodoList({ todos, filter }) {
  const [newTodo, setNewTodo] = useState('');
  const [visibleTodos, setVisibleTodos] = useState([]);
  useEffect(() => {
    setVisibleTodos(getFilteredTodos(todos, filter));
  }, [todos, filter]);
}

// 좋은 예: useMemo 사용
function TodoList({ todos, filter }) {
  const [newTodo, setNewTodo] = useState('');
  const visibleTodos = useMemo(
    () => getFilteredTodos(todos, filter),
    [todos, filter]
  );
}
```

- `useMemo`는 첫 렌더링을 빠르게 하지 않고, 업데이트 시 불필요한 작업을 건너뛰는 것임
- React Compiler가 많은 경우 자동으로 비싼 계산을 메모이제이션할 수 있어 수동 `useMemo`가 불필요해질 수 있음

**계산이 비싼지 확인하는 방법:**

```js
console.time('filter array');
const visibleTodos = getFilteredTodos(todos, filter);
console.timeEnd('filter array');
```

- 기록된 시간이 1ms 이상이면 메모이제이션이 가치 있을 수 있음

### prop이 변경될 때 모든 state 재설정하기

```js
// 나쁜 예: Effect에서 state 재설정
export default function ProfilePage({ userId }) {
  const [comment, setComment] = useState('');
  useEffect(() => {
    setComment('');
  }, [userId]);
}

// 좋은 예: key를 사용하여 state 재설정
export default function ProfilePage({ userId }) {
  return (
    <Profile
      userId={userId}
      key={userId}
    />
  );
}

function Profile({ userId }) {
  const [comment, setComment] = useState('');
}
```

- `userId`를 `key`로 전달하면, React가 다른 `userId` 값을 다른 컴포넌트로 취급하여 DOM을 다시 만들고 모든 state를 자동으로 재설정함

### prop이 변경될 때 일부 state 조정하기

```js
// 괜찮지만 복잡한 방법: 이전 렌더링의 정보 저장
function List({ items }) {
  const [isReverse, setIsReverse] = useState(false);
  const [selection, setSelection] = useState(null);
  const [prevItems, setPrevItems] = useState(items);
  if (items !== prevItems) {
    setPrevItems(items);
    setSelection(null);
  }
}

// 최선의 방법: 렌더링 중에 모든 것을 계산
function List({ items }) {
  const [isReverse, setIsReverse] = useState(false);
  const [selectedId, setSelectedId] = useState(null);
  const selection = items.find(item => item.id === selectedId) ?? null;
}
```

### 이벤트 핸들러 간 로직 공유

```js
// 나쁜 예: Effect에서 이벤트 특정 로직
function ProductPage({ product, addToCart }) {
  useEffect(() => {
    if (product.isInCart) {
      showNotification(`Added ${product.name} to the shopping cart!`);
    }
  }, [product]);

  function handleBuyClick() {
    addToCart(product);
  }
}

// 좋은 예: 공유 로직을 함수로 추출
function ProductPage({ product, addToCart }) {
  function buyProduct() {
    addToCart(product);
    showNotification(`Added ${product.name} to the shopping cart!`);
  }

  function handleBuyClick() {
    buyProduct();
  }

  function handleCheckoutClick() {
    buyProduct();
    navigateTo('/checkout');
  }
}
```

- 핵심 원칙: 사용자에게 컴포넌트가 표시되었기 때문에 실행해야 하는 코드에만 Effect를 사용할 것

### POST 요청 보내기

```js
// 분석 데이터 (Effect에 적합 - 폼이 표시되어 트리거됨)
useEffect(() => {
  post('/analytics/event', { eventName: 'visit_form' });
}, []);

// 폼 제출 (이벤트 핸들러에 적합 - 사용자 작업에 의해 트리거됨)
function handleSubmit(e) {
  e.preventDefault();
  post('/api/register', { firstName, lastName });
}
```

### 계산의 연쇄

```js
// 나쁜 예: 여러 Effect가 state를 조정
function Game() {
  const [card, setCard] = useState(null);
  const [goldCardCount, setGoldCardCount] = useState(0);
  const [round, setRound] = useState(1);
  const [isGameOver, setIsGameOver] = useState(false);

  useEffect(() => {
    if (card !== null && card.gold) {
      setGoldCardCount(c => c + 1);
    }
  }, [card]);

  useEffect(() => {
    if (goldCardCount > 3) {
      setRound(r => r + 1);
      setGoldCardCount(0);
    }
  }, [goldCardCount]);

  useEffect(() => {
    if (round > 5) {
      setIsGameOver(true);
    }
  }, [round]);

  useEffect(() => {
    alert('Good game!');
  }, [isGameOver]);
}

// 좋은 예: 이벤트 핸들러에서 계산하고 업데이트
function Game() {
  const [card, setCard] = useState(null);
  const [goldCardCount, setGoldCardCount] = useState(0);
  const [round, setRound] = useState(1);

  const isGameOver = round > 5;

  function handlePlaceCard(nextCard) {
    if (isGameOver) {
      throw Error('Game already ended.');
    }

    setCard(nextCard);
    if (nextCard.gold) {
      if (goldCardCount < 3) {
        setGoldCardCount(goldCardCount + 1);
      } else {
        setGoldCardCount(0);
        setRound(round + 1);
        if (round === 5) {
          alert('Good game!');
        }
      }
    }
  }
}
```

- 이벤트 핸들러 내에서 state는 스냅샷처럼 동작함. 다음 값이 필요하면 수동으로 정의해야 함: `const nextRound = round + 1`

### 애플리케이션 초기화

```js
// 나쁜 예: Strict Mode에서 두 번 실행됨
function App() {
  useEffect(() => {
    loadDataFromLocalStorage();
    checkAuthToken();
  }, []);
}

// 좋은 예: 변수로 초기화 추적
let didInit = false;

function App() {
  useEffect(() => {
    if (!didInit) {
      didInit = true;
      loadDataFromLocalStorage();
      checkAuthToken();
    }
  }, []);
}

// 좋은 예: 모듈 수준에서 초기화
if (typeof window !== 'undefined') {
  checkAuthToken();
  loadDataFromLocalStorage();
}

function App() {
  // ...
}
```

### 부모 컴포넌트에 state 변경 알리기

```js
// 나쁜 예: Effect에서 늦은 알림
function Toggle({ onChange }) {
  const [isOn, setIsOn] = useState(false);
  useEffect(() => {
    onChange(isOn);
  }, [isOn, onChange]);

  function handleClick() {
    setIsOn(!isOn);
  }
}

// 좋은 예: 이벤트에서 두 컴포넌트 모두 업데이트
function Toggle({ onChange }) {
  const [isOn, setIsOn] = useState(false);

  function updateToggle(nextIsOn) {
    setIsOn(nextIsOn);
    onChange(nextIsOn);
  }

  function handleClick() {
    updateToggle(!isOn);
  }
}

// 최선의 예: 제어 컴포넌트 (state 끌어올리기)
function Toggle({ isOn, onChange }) {
  function handleClick() {
    onChange(!isOn);
  }
}
```

### 외부 저장소 구독하기

```js
// 나쁜 예: Effect에서 수동 구독
function useOnlineStatus() {
  const [isOnline, setIsOnline] = useState(true);
  useEffect(() => {
    function updateState() {
      setIsOnline(navigator.onLine);
    }
    updateState();
    window.addEventListener('online', updateState);
    window.addEventListener('offline', updateState);
    return () => {
      window.removeEventListener('online', updateState);
      window.removeEventListener('offline', updateState);
    };
  }, []);
  return isOnline;
}

// 좋은 예: useSyncExternalStore 사용
function subscribe(callback) {
  window.addEventListener('online', callback);
  window.addEventListener('offline', callback);
  return () => {
    window.removeEventListener('online', callback);
    window.removeEventListener('offline', callback);
  };
}

function useOnlineStatus() {
  return useSyncExternalStore(
    subscribe,
    () => navigator.onLine,  // 클라이언트에서 값 가져오는 방법
    () => true               // 서버에서 값 가져오는 방법
  );
}
```

### 데이터 가져오기

```js
// 경쟁 상태가 있는 코드
function SearchResults({ query }) {
  const [results, setResults] = useState([]);
  const [page, setPage] = useState(1);

  useEffect(() => {
    fetchResults(query, page).then(json => {
      setResults(json);
    });
  }, [query, page]);
}

// 정리 함수로 오래된 응답 무시
function SearchResults({ query }) {
  const [results, setResults] = useState([]);
  const [page, setPage] = useState(1);

  useEffect(() => {
    let ignore = false;
    fetchResults(query, page).then(json => {
      if (!ignore) {
        setResults(json);
      }
    });
    return () => {
      ignore = true;
    };
  }, [query, page]);
}
```

**커스텀 Hook으로 재사용 가능한 데이터 가져오기:**

```js
function useData(url) {
  const [data, setData] = useState(null);
  useEffect(() => {
    let ignore = false;
    fetch(url)
      .then(response => response.json())
      .then(json => {
        if (!ignore) {
          setData(json);
        }
      });
    return () => {
      ignore = true;
    };
  }, [url]);
  return data;
}

function SearchResults({ query }) {
  const [page, setPage] = useState(1);
  const params = new URLSearchParams({ query, page });
  const results = useData(`/api/search?${params}`);

  function handleNextPageClick() {
    setPage(page + 1);
  }
}
```

### 핵심 원칙 정리

- 렌더링 중에 계산할 수 있으면 Effect가 필요 없음
- 비싼 계산을 캐시하려면 `useEffect` 대신 `useMemo`를 사용할 것
- 전체 컴포넌트 트리의 state를 재설정하려면 다른 `key`를 전달할 것
- prop 변경에 대응하여 특정 state를 재설정하려면 렌더링 중에 설정할 것
- 컴포넌트가 표시되었기 때문에 실행해야 하는 코드는 Effect에, 나머지는 이벤트에 넣을 것
- 여러 컴포넌트의 state를 업데이트해야 하면 단일 이벤트에서 할 것
- 서로 다른 컴포넌트의 state 변수를 동기화하려 할 때마다 state 끌어올리기를 고려할 것
- Effect로 데이터를 가져올 수 있지만, 경쟁 상태를 피하기 위해 정리를 구현해야 함

---

## 반응형 Effect의 생명주기 (Lifecycle of Reactive Effects)

> 원문: https://react.dev/learn/lifecycle-of-reactive-effects

### Effect의 생명주기는 컴포넌트와 다름

- 컴포넌트: 마운트, 업데이트, 언마운트
- Effect: 동기화를 시작하고, 나중에 동기화를 중지하는 것만 할 수 있음. 이 주기는 props와 state가 시간에 따라 변경되면 여러 번 반복될 수 있음

### 동기화 시작과 중지

```js
const serverUrl = 'https://localhost:1234';

function ChatRoom({ roomId }) {
  useEffect(() => {
    const connection = createConnection(serverUrl, roomId);
    connection.connect();
    return () => {
      connection.disconnect();
    };
  }, [roomId]);
  // ...
}
```

- Effect의 본문은 동기화를 시작하는 방법을 지정함
- 반환된 정리 함수는 동기화를 중지하는 방법을 지정함

### 동기화가 여러 번 필요한 이유

- `roomId`가 `"general"`에서 `"travel"`로 변경되면:
  1. 이전 `roomId`("general")와의 동기화를 중지 (정리 함수 호출, "general" 방에서 연결 해제)
  2. 새 `roomId`("travel")와의 동기화를 시작 (Effect 본문 실행, "travel" 방에 연결)
- 컴포넌트가 언마운트될 때 마지막으로 동기화를 중지함

### Effect 관점에서 생각하기

- 컴포넌트 관점: "마운트 후 콜백" 또는 "생명주기 이벤트"로 생각하면 복잡해짐
- Effect 관점: 항상 한 번의 시작/중지 주기에 집중할 것. 컴포넌트가 마운트, 업데이트, 언마운트 중인지는 중요하지 않음. 동기화를 시작하는 방법과 중지하는 방법만 설명하면 됨

### React가 Effect가 재동기화 가능한지 확인하는 방법

- 개발 모드에서 React는 즉시 Effect를 시작하고 중지하여 한 번 더 강제로 재동기화함
- 개발 모드에서 세 개의 로그가 보임:
  1. "Connecting to general room..." (개발 전용)
  2. "Disconnected from general room." (개발 전용)
  3. "Connecting to general room..."

### React가 재동기화가 필요함을 아는 방법

- 의존성 배열에 `roomId`를 포함했기 때문임
- `["general"]`을 전달했다가 나중에 `["travel"]`을 전달하면, React가 `"general"`과 `"travel"`을 비교함
- `Object.is`로 비교하여 다르면 Effect를 재동기화함

### 각 Effect는 별도의 동기화 프로세스를 나타냄

- 같은 시점에 실행되어야 한다는 이유만으로 관련 없는 로직을 Effect에 추가하지 말 것

```js
// 나쁜 예: 관련 없는 로직을 하나의 Effect에 결합
function ChatRoom({ roomId }) {
  useEffect(() => {
    logVisit(roomId);
    const connection = createConnection(serverUrl, roomId);
    connection.connect();
    return () => {
      connection.disconnect();
    };
  }, [roomId]);
}

// 좋은 예: 별도의 Effect로 분리
function ChatRoom({ roomId }) {
  useEffect(() => {
    logVisit(roomId);
  }, [roomId]);

  useEffect(() => {
    const connection = createConnection(serverUrl, roomId);
    // ...
  }, [roomId]);
}
```

- 하나의 Effect를 삭제해도 다른 Effect의 로직이 깨지지 않으면, 서로 다른 것을 동기화하고 있다는 좋은 징표임

### Effect는 반응형 값에 "반응"함

- props, state, 컴포넌트 본문에서 선언된 다른 값들은 렌더링 중에 계산되고 React 데이터 흐름에 참여하기 때문에 반응형임
- 반응형 값은 의존성에 포함해야 함

```js
function ChatRoom({ roomId }) {
  const [serverUrl, setServerUrl] = useState('https://localhost:1234');

  useEffect(() => {
    const connection = createConnection(serverUrl, roomId);
    connection.connect();
    return () => {
      connection.disconnect();
    };
  }, [roomId, serverUrl]); // 두 반응형 값 모두 포함
}
```

- `serverUrl`이 state 변수이면 반응형임. 반응형 값은 의존성에 포함해야 함

### 빈 의존성 배열의 의미

- 두 변수를 컴포넌트 밖으로 이동하면 반응형 값을 사용하지 않게 되어 의존성이 비어 있을 수 있음

```js
const serverUrl = 'https://localhost:1234';
const roomId = 'general';

function ChatRoom() {
  useEffect(() => {
    const connection = createConnection(serverUrl, roomId);
    connection.connect();
    return () => {
      connection.disconnect();
    };
  }, []); // 모든 의존성 선언됨
}
```

- 컴포넌트 관점: 빈 `[]` 의존성 배열은 컴포넌트가 마운트될 때만 연결하고 언마운트될 때만 연결을 끊는다는 의미임
- Effect 관점: 마운트와 언마운트를 생각할 필요 없음. 동기화 시작과 중지 방법만 설명한 것임

### 컴포넌트 본문에서 선언된 모든 변수는 반응형임

- props와 state뿐 아니라 그것들로부터 계산된 값도 반응형임

```js
function ChatRoom({ roomId, selectedServerUrl }) {
  const settings = useContext(SettingsContext); // settings는 반응형
  const serverUrl = selectedServerUrl ?? settings.defaultServerUrl; // serverUrl은 반응형
  useEffect(() => {
    const connection = createConnection(serverUrl, roomId);
    connection.connect();
    return () => {
      connection.disconnect();
    };
  }, [roomId, serverUrl]); // 둘 중 하나가 변경되면 재동기화
}
```

### 전역 또는 변경 가능한 값은 의존성이 될 수 없음

- `location.pathname` 같은 변경 가능한 값은 의존성이 될 수 없음. React 렌더링 데이터 흐름 외부에서 변경될 수 있기 때문임. `useSyncExternalStore`로 외부 변경 가능 값을 읽고 구독해야 함
- `ref.current`나 그로부터 읽은 값도 의존성이 될 수 없음. `useRef`가 반환한 ref 객체 자체는 의존성이 될 수 있지만, `current` 속성은 의도적으로 변경 가능함

### 의존성을 "선택"할 수 없음

- 의존성은 Effect 내에서 읽는 모든 반응형 값을 포함해야 함. 린터가 이를 강제함
- 린터를 억제하지 말 것. 대신:
  - Effect가 독립적인 동기화 프로세스를 나타내는지 확인할 것
  - "반응"하지 않고 최신 값을 읽고 싶으면 Effect Event로 추출할 것
  - 객체와 함수에 의존하지 말 것

```js
// 린터를 억제하지 말 것
useEffect(() => {
  // ...
  // eslint-ignore-next-line react-hooks/exhaustive-deps
}, []);
```

---

## 이벤트와 Effect 분리하기 (Separating Events from Effects)

> 원문: https://react.dev/learn/separating-events-from-effects

### 이벤트 핸들러와 Effect 중 선택하기

- 코드가 실행되어야 하는 이유를 생각할 것
- **이벤트 핸들러**: 특정 상호작용에 대한 응답으로 실행됨. 같은 상호작용을 다시 수행할 때만 다시 실행됨
- **Effect**: 동기화가 필요할 때마다 실행됨. 읽는 값이 변경되면 다시 실행됨

### 반응형 값과 반응형 로직

```js
const serverUrl = 'https://localhost:1234'; // 반응형 값이 아님

function ChatRoom({ roomId }) {  // roomId는 반응형
  const [message, setMessage] = useState(''); // message는 반응형
  // ...
}
```

- **이벤트 핸들러 내부의 로직은 반응형이 아님**: 사용자가 같은 상호작용을 다시 수행하지 않는 한 다시 실행되지 않음. 반응형 값의 변경에 "반응"하지 않고 읽을 수 있음
- **Effect 내부의 로직은 반응형임**: Effect가 반응형 값을 읽으면 의존성으로 지정해야 함. 리렌더링으로 그 값이 변경되면 React가 새 값으로 Effect의 로직을 다시 실행함

### Effect에서 비반응형 로직 추출하기

- 문제: 사용자가 채팅방에 연결될 때 알림을 표시하고, props에서 현재 테마(어두움/밝음)를 읽는 경우

```js
function ChatRoom({ roomId, theme }) {
  useEffect(() => {
    const connection = createConnection(serverUrl, roomId);
    connection.on('connected', () => {
      showNotification('Connected!', theme);
    });
    connection.connect();
    return () => {
      connection.disconnect();
    };
  }, [roomId, theme]); // theme이 변경될 때마다 채팅이 재연결됨!
}
```

- `theme`이 변경되면 Effect가 재동기화되어 채팅이 재연결됨. 이것은 원치 않는 동작임

### Effect Event 선언하기

- `useEffectEvent` Hook을 사용하여 비반응형 로직을 Effect에서 추출함

```js
import { useEffect, useEffectEvent } from 'react';

function ChatRoom({ roomId, theme }) {
  const onConnected = useEffectEvent(() => {
    showNotification('Connected!', theme);
  });

  useEffect(() => {
    const connection = createConnection(serverUrl, roomId);
    connection.on('connected', () => {
      onConnected();
    });
    connection.connect();
    return () => connection.disconnect();
  }, [roomId]); // theme은 더 이상 의존성이 아님
}
```

- `onConnected`는 Effect Event임. Effect 로직의 일부이지만 이벤트 핸들러처럼 동작함
- 내부 로직은 반응형이 아니며, 항상 props와 state의 최신 값을 "봄"
- Effect Event는 의존성에서 제외해야 함

### Effect Event로 최신 props와 state 읽기

- 페이지 방문 로깅 예제:

```js
function Page({ url }) {
  const { items } = useContext(ShoppingCartContext);
  const numberOfItems = items.length;

  const onVisit = useEffectEvent(visitedUrl => {
    logVisit(visitedUrl, numberOfItems);
  });

  useEffect(() => {
    onVisit(url);
  }, [url]); // url에만 반응, numberOfItems에는 반응하지 않음
}
```

- `url`을 Effect Event에 명시적으로 인자로 전달하는 것이 좋음. "이벤트"의 일부이기 때문임
- 비동기 로직 내에서도 올바르게 작동함

### 린터를 억제하면 안 되는 이유

```js
// 나쁜 예: 린터 억제
useEffect(() => {
  // ...
  // eslint-disable-next-line react-hooks/exhaustive-deps
}, []);
```

- 린터를 억제하면 새로운 반응형 의존성을 추가했을 때 React가 경고하지 않음
- "오래된 값(stale values)" 문제를 초래함
- `useEffectEvent`를 사용하면 린터에 "거짓말"할 필요가 없음

### Effect Event의 제한 사항

- Effect 내부에서만 호출할 수 있음
- 다른 컴포넌트나 Hook에 전달하면 안 됨

```js
// 나쁜 예: Effect Event를 다른 컴포넌트에 전달
function Timer() {
  const onTick = useEffectEvent(() => {
    setCount(count + 1);
  });
  useTimer(onTick, 1000); // Effect Event 전달 금지
}

// 좋은 예: Hook 내부에서 Effect Event 선언
function useTimer(callback, delay) {
  const onTick = useEffectEvent(() => {
    callback();
  });

  useEffect(() => {
    const id = setInterval(() => {
      onTick();
    }, delay);
    return () => {
      clearInterval(id);
    };
  }, [delay]);
}
```

### 도전 과제: 업데이트되지 않는 변수 수정

```js
// 문제: 린터 억제로 increment가 항상 1
useEffect(() => {
  const id = setInterval(() => {
    setCount(c => c + increment);
  }, 1000);
  return () => {
    clearInterval(id);
  };
  // eslint-disable-next-line react-hooks/exhaustive-deps
}, []);

// 해결: increment를 의존성에 추가
useEffect(() => {
  const id = setInterval(() => {
    setCount(c => c + increment);
  }, 1000);
  return () => {
    clearInterval(id);
  };
}, [increment]);
```

### 도전 과제: 멈추는 카운터 수정

```js
// 문제: increment 변경 시 interval이 재설정되어 일시 정지됨
// 해결: Effect Event 추출
const onTick = useEffectEvent(() => {
  setCount(c => c + increment);
});

useEffect(() => {
  const id = setInterval(() => {
    onTick();
  }, 1000);
  return () => {
    clearInterval(id);
  };
}, []);
```

### 도전 과제: 조정 불가능한 지연 수정

```js
// 문제: delay가 Effect Event 안에 있어 반응하지 않음
// 해결: delay는 반응형으로 유지하고, increment만 Effect Event로 추출
const onTick = useEffectEvent(() => {
  setCount(c => c + increment);
});

useEffect(() => {
  const id = setInterval(() => {
    onTick();
  }, delay);
  return () => {
    clearInterval(id);
  };
}, [delay]); // delay에만 반응
```

- Effect Event는 특정 이유가 있을 때만 코드의 일부를 비반응형으로 만들기 위해 추출해야 함

### 도전 과제: 지연된 알림 수정

```js
// 문제: 빠르게 방을 전환하면 두 알림 모두 "music"으로 표시됨
// 해결: roomId를 Effect Event에 인자로 전달하고, timeout을 정리 함수에서 취소
const onConnected = useEffectEvent(connectedRoomId => {
  showNotification('Welcome to ' + connectedRoomId, theme);
});

useEffect(() => {
  const connection = createConnection(serverUrl, roomId);
  let notificationTimeoutId;
  connection.on('connected', () => {
    notificationTimeoutId = setTimeout(() => {
      onConnected(roomId);
    }, 2000);
  });
  connection.connect();
  return () => {
    connection.disconnect();
    if (notificationTimeoutId !== undefined) {
      clearTimeout(notificationTimeoutId);
    }
  };
}, [roomId]);
```

---

## Effect 의존성 제거하기 (Removing Effect Dependencies)

> 원문: https://react.dev/learn/removing-effect-dependencies

### 의존성은 코드와 일치해야 함

- Effect를 작성할 때 린터가 Effect가 읽는 모든 반응형 값(props와 state)을 의존성 목록에 포함했는지 확인함
- 불필요한 의존성은 Effect가 너무 자주 실행되거나 무한 루프를 만들 수 있음

### 의존성을 변경하려면 코드를 먼저 변경할 것

작업 흐름:
1. Effect의 코드 또는 반응형 값 선언 방식을 변경함
2. 린터를 따라 의존성을 변경된 코드에 맞게 조정함
3. 만족스럽지 않으면 1단계로 돌아감

- 의존성 목록은 코드를 설명하는 것이지 선택하는 것이 아님

### 린터를 억제하면 안 되는 이유

```js
// 나쁜 예: 린터 억제
useEffect(() => {
  // ...
  // eslint-ignore-next-line react-hooks/exhaustive-deps
}, []);
```

- 린터 오류를 컴파일 오류처럼 취급할 것. 억제하지 않으면 이런 버그를 만나지 않음

### 불필요한 의존성 제거하기

#### 질문 1: 이 코드가 이벤트 핸들러로 이동해야 하는가?

```js
// 나쁜 예: 이벤트 로직이 Effect에 있음
function Form() {
  const [submitted, setSubmitted] = useState(false);
  const theme = useContext(ThemeContext);

  useEffect(() => {
    if (submitted) {
      post('/api/register');
      showNotification('Successfully registered!', theme);
    }
  }, [submitted, theme]); // theme 변경 시 알림이 다시 표시됨
}

// 좋은 예: 이벤트 핸들러로 이동
function Form() {
  const theme = useContext(ThemeContext);

  function handleSubmit() {
    post('/api/register');
    showNotification('Successfully registered!', theme);
  }
}
```

#### 질문 2: Effect가 관련 없는 여러 가지를 하고 있는가?

```js
// 나쁜 예: 독립적인 두 프로세스가 하나의 Effect에 있음
function ShippingForm({ country }) {
  const [cities, setCities] = useState(null);
  const [city, setCity] = useState(null);
  const [areas, setAreas] = useState(null);

  useEffect(() => {
    let ignore = false;
    fetch(`/api/cities?country=${country}`)
      .then(response => response.json())
      .then(json => {
        if (!ignore) setCities(json);
      });
    if (city) {
      fetch(`/api/areas?city=${city}`)
        .then(response => response.json())
        .then(json => {
          if (!ignore) setAreas(json);
        });
    }
    return () => { ignore = true; };
  }, [country, city]);
}

// 좋은 예: 독립적인 프로세스를 별도 Effect로 분리
function ShippingForm({ country }) {
  const [cities, setCities] = useState(null);
  useEffect(() => {
    let ignore = false;
    fetch(`/api/cities?country=${country}`)
      .then(response => response.json())
      .then(json => {
        if (!ignore) setCities(json);
      });
    return () => { ignore = true; };
  }, [country]);

  const [city, setCity] = useState(null);
  const [areas, setAreas] = useState(null);
  useEffect(() => {
    if (city) {
      let ignore = false;
      fetch(`/api/areas?city=${city}`)
        .then(response => response.json())
        .then(json => {
          if (!ignore) setAreas(json);
        });
      return () => { ignore = true; };
    }
  }, [city]);
}
```

#### 질문 3: 다음 state를 계산하기 위해 일부 state를 읽고 있는가?

```js
// 나쁜 예: messages가 의존성이 되어 재연결됨
function ChatRoom({ roomId }) {
  const [messages, setMessages] = useState([]);
  useEffect(() => {
    const connection = createConnection();
    connection.connect();
    connection.on('message', (receivedMessage) => {
      setMessages([...messages, receivedMessage]);
    });
    return () => connection.disconnect();
  }, [roomId, messages]);
}

// 좋은 예: 업데이터 함수 사용
function ChatRoom({ roomId }) {
  const [messages, setMessages] = useState([]);
  useEffect(() => {
    const connection = createConnection();
    connection.connect();
    connection.on('message', (receivedMessage) => {
      setMessages(msgs => [...msgs, receivedMessage]);
    });
    return () => connection.disconnect();
  }, [roomId]); // messages가 더 이상 의존성이 아님
}
```

#### 질문 4: 변경에 "반응"하지 않고 값을 읽고 싶은가?

```js
// 나쁜 예: isMuted 변경 시 재연결됨
function ChatRoom({ roomId }) {
  const [messages, setMessages] = useState([]);
  const [isMuted, setIsMuted] = useState(false);

  useEffect(() => {
    const connection = createConnection();
    connection.connect();
    connection.on('message', (receivedMessage) => {
      setMessages(msgs => [...msgs, receivedMessage]);
      if (!isMuted) {
        playSound();
      }
    });
    return () => connection.disconnect();
  }, [roomId, isMuted]);
}

// 좋은 예: Effect Event 사용
import { useEffectEvent } from 'react';

function ChatRoom({ roomId }) {
  const [messages, setMessages] = useState([]);
  const [isMuted, setIsMuted] = useState(false);

  const onMessage = useEffectEvent(receivedMessage => {
    setMessages(msgs => [...msgs, receivedMessage]);
    if (!isMuted) {
      playSound();
    }
  });

  useEffect(() => {
    const connection = createConnection();
    connection.connect();
    connection.on('message', (receivedMessage) => {
      onMessage(receivedMessage);
    });
    return () => connection.disconnect();
  }, [roomId]);
}
```

**props로 전달된 이벤트 핸들러 래핑:**

```js
function ChatRoom({ roomId, onReceiveMessage }) {
  const onMessage = useEffectEvent(onReceiveMessage);

  useEffect(() => {
    const connection = createConnection();
    connection.connect();
    connection.on('message', (receivedMessage) => {
      onMessage(receivedMessage);
    });
    return () => connection.disconnect();
  }, [roomId]); // onReceiveMessage에 더 이상 의존하지 않음
}
```

#### 질문 5: 반응형 값이 의도치 않게 변경되는가?

- JavaScript에서 객체와 함수는 다른 시점에 생성되면 서로 다른 것으로 간주됨

```js
// 문제: options 객체가 매 렌더링마다 새로 생성됨
function ChatRoom({ roomId }) {
  const [message, setMessage] = useState('');

  const options = {
    serverUrl: serverUrl,
    roomId: roomId
  };

  useEffect(() => {
    const connection = createConnection(options);
    connection.connect();
    return () => connection.disconnect();
  }, [options]); // 매 렌더링마다 새 객체이므로 항상 재동기화됨
}
```

**해결 방법 1: 정적 객체/함수를 컴포넌트 외부로 이동**

```js
const options = {
  serverUrl: 'https://localhost:1234',
  roomId: 'music'
};

function ChatRoom() {
  useEffect(() => {
    const connection = createConnection(options);
    connection.connect();
    return () => connection.disconnect();
  }, []);
}
```

**해결 방법 2: 동적 객체/함수를 Effect 내부로 이동**

```js
function ChatRoom({ roomId }) {
  useEffect(() => {
    const options = {
      serverUrl: serverUrl,
      roomId: roomId
    };
    const connection = createConnection(options);
    connection.connect();
    return () => connection.disconnect();
  }, [roomId]); // roomId 원시값만 의존성
}
```

**해결 방법 3: 객체에서 원시 값 읽기**

```js
function ChatRoom({ options }) {
  const { roomId, serverUrl } = options;
  useEffect(() => {
    const connection = createConnection({
      roomId: roomId,
      serverUrl: serverUrl
    });
    connection.connect();
    return () => connection.disconnect();
  }, [roomId, serverUrl]); // 객체 대신 원시값
}
```

---

## 커스텀 Hook으로 로직 재사용하기 (Reusing Logic with Custom Hooks)

> 원문: https://react.dev/learn/reusing-logic-with-custom-hooks

### 커스텀 Hook: 컴포넌트 간 로직 공유

- React에는 `useState`, `useContext`, `useEffect` 같은 내장 Hook이 있음
- 데이터 가져오기, 사용자 온라인 상태 추적, 채팅방 연결 등 특정 목적을 위한 자체 Hook을 만들 수 있음

### 문제: 중복된 네트워크 상태 로직

```js
// StatusBar와 SaveButton 모두에 동일한 로직이 중복됨
function StatusBar() {
  const [isOnline, setIsOnline] = useState(true);
  useEffect(() => {
    function handleOnline() { setIsOnline(true); }
    function handleOffline() { setIsOnline(false); }
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);
  return <h1>{isOnline ? 'Online' : 'Disconnected'}</h1>;
}
```

### 해결: 커스텀 Hook 추출

```js
function useOnlineStatus() {
  const [isOnline, setIsOnline] = useState(true);
  useEffect(() => {
    function handleOnline() { setIsOnline(true); }
    function handleOffline() { setIsOnline(false); }
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);
  return isOnline;
}

// 간소화된 컴포넌트
function StatusBar() {
  const isOnline = useOnlineStatus();
  return <h1>{isOnline ? 'Online' : 'Disconnected'}</h1>;
}

function SaveButton() {
  const isOnline = useOnlineStatus();

  function handleSaveClick() {
    console.log('Progress saved');
  }

  return (
    <button disabled={!isOnline} onClick={handleSaveClick}>
      {isOnline ? 'Save progress' : 'Reconnecting...'}
    </button>
  );
}
```

### Hook 이름 규칙

1. React 컴포넌트 이름은 대문자로 시작해야 하고 JSX를 반환함
2. Hook 이름은 `use`로 시작하고 대문자가 뒤따라야 함 (예: `useState`, `useOnlineStatus`)
3. Hook을 호출하지 않는 함수에는 `use` 접두사를 사용하지 말 것

```js
// 나쁜 예: Hook을 사용하지 않는 Hook
function useSorted(items) {
  return items.slice().sort();
}

// 좋은 예: Hook을 사용하지 않는 일반 함수
function getSorted(items) {
  return items.slice().sort();
}
```

- 일반 함수는 조건부로 호출할 수 있음

```js
function List({ items, shouldSort }) {
  let displayedItems = items;
  if (shouldSort) {
    displayedItems = getSorted(items); // 조건부 호출 가능
  }
}
```

### 커스텀 Hook은 상태 로직을 공유하지만 state 자체를 공유하지 않음

- 여러 컴포넌트가 같은 커스텀 Hook을 사용해도 각 호출은 독립적인 state를 가짐

### 폼 입력 예제

```js
import { useState } from 'react';

export function useFormInput(initialValue) {
  const [value, setValue] = useState(initialValue);

  function handleChange(e) {
    setValue(e.target.value);
  }

  const inputProps = {
    value: value,
    onChange: handleChange
  };

  return inputProps;
}

// 사용
import { useFormInput } from './useFormInput.js';

export default function Form() {
  const firstNameProps = useFormInput('Mary');
  const lastNameProps = useFormInput('Poppins');

  return (
    <>
      <label>
        First name:
        <input {...firstNameProps} />
      </label>
      <label>
        Last name:
        <input {...lastNameProps} />
      </label>
      <p><b>Good morning, {firstNameProps.value} {lastNameProps.value}.</b></p>
    </>
  );
}
```

### Hook 간 반응형 값 전달

- 커스텀 Hook 코드는 컴포넌트가 리렌더링될 때마다 다시 실행되어 최신 props와 state를 받음

```js
export function useChatRoom({ serverUrl, roomId }) {
  useEffect(() => {
    const options = {
      serverUrl: serverUrl,
      roomId: roomId
    };
    const connection = createConnection(options);
    connection.connect();
    connection.on('message', (msg) => {
      showNotification('New message: ' + msg);
    });
    return () => connection.disconnect();
  }, [roomId, serverUrl]);
}

// 사용
export default function ChatRoom({ roomId }) {
  const [serverUrl, setServerUrl] = useState('https://localhost:1234');

  useChatRoom({
    roomId: roomId,
    serverUrl: serverUrl
  });

  return (
    <>
      <label>
        Server URL:
        <input value={serverUrl} onChange={e => setServerUrl(e.target.value)} />
      </label>
      <h1>Welcome to the {roomId} room!</h1>
    </>
  );
}
```

### 커스텀 Hook에 이벤트 핸들러 전달하기

```js
export function useChatRoom({ serverUrl, roomId, onReceiveMessage }) {
  const onMessage = useEffectEvent(onReceiveMessage);

  useEffect(() => {
    const options = {
      serverUrl: serverUrl,
      roomId: roomId
    };
    const connection = createConnection(options);
    connection.connect();
    connection.on('message', (msg) => {
      onMessage(msg);
    });
    return () => connection.disconnect();
  }, [roomId, serverUrl]); // 모든 의존성 선언됨
}
```

### 커스텀 Hook을 추출해야 하는 시점

- 사소한 중복은 괜찮음. 모든 중복 코드에 추출이 필요한 것은 아님
- Effect를 작성할 때 추출하는 것이 좋음:
  - Effect가 자주 필요하지 않기 때문임
  - Hook으로 감싸면 의도가 명확하게 전달됨
  - 코드를 통해 데이터 흐름이 어떻게 되는지 보여줌

### 구체적인 고수준 사용 사례에 집중할 것

- 좋은 커스텀 Hook 이름: `useData(url)`, `useImpressionLog(eventName, extraData)`, `useChatRoom(options)`, `useMediaQuery(query)`, `useSocket(url)`, `useIntersectionObserver(ref, options)`
- 나쁜 커스텀 Hook (일반적인 "생명주기" Hook): `useMount(fn)`, `useEffectOnce(fn)`, `useUpdateEffect(fn)`

```js
// 나쁜 예: useMount는 의존성 변경에 반응하지 않음
function ChatRoom({ roomId }) {
  useMount(() => {
    const connection = createConnection({ roomId });
    connection.connect();
    post('/analytics/event', { eventName: 'visit_chat' });
  });
}

function useMount(fn) {
  useEffect(() => {
    fn();
  }, []); // 의존성 누락: 'fn'
}

// 좋은 예: 목적별로 분리된 raw Effect 또는 도메인 특화 Hook
function ChatRoom({ roomId }) {
  useChatRoom({ roomId });
  useImpressionLog('visit_chat', { roomId });
}
```

### 커스텀 Hook은 더 나은 패턴으로의 마이그레이션을 도움

- Effect는 탈출구임. React가 개선되면 커스텀 Hook이 업그레이드를 쉽게 만듦

```js
// 이전 접근: useState + useEffect
export function useOnlineStatus() {
  const [isOnline, setIsOnline] = useState(true);
  useEffect(() => {
    function handleOnline() { setIsOnline(true); }
    function handleOffline() { setIsOnline(false); }
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);
  return isOnline;
}

// 새로운 접근: useSyncExternalStore
import { useSyncExternalStore } from 'react';

function subscribe(callback) {
  window.addEventListener('online', callback);
  window.addEventListener('offline', callback);
  return () => {
    window.removeEventListener('online', callback);
    window.removeEventListener('offline', callback);
  };
}

export function useOnlineStatus() {
  return useSyncExternalStore(
    subscribe,
    () => navigator.onLine,
    () => true
  );
}
```

- Hook을 사용하는 컴포넌트는 변경되지 않음. 같은 방식으로 계속 작동함

### 도전 과제: useCounter Hook 추출

```js
// useCounter.js
import { useState, useEffect } from 'react';

export function useCounter(delay) {
  const [count, setCount] = useState(0);
  useEffect(() => {
    const id = setInterval(() => {
      setCount(c => c + 1);
    }, delay);
    return () => clearInterval(id);
  }, [delay]);
  return count;
}
```

### 도전 과제: useInterval 추출

```js
// useInterval.js
import { useEffect } from 'react';
import { useEffectEvent } from 'react';

export function useInterval(callback, delay) {
  const onTick = useEffectEvent(callback);
  useEffect(() => {
    const id = setInterval(onTick, delay);
    return () => clearInterval(id);
  }, [delay]);
}

// useCounter.js
import { useState } from 'react';
import { useInterval } from './useInterval.js';

export function useCounter(delay) {
  const [count, setCount] = useState(0);
  useInterval(() => {
    setCount(c => c + 1);
  }, delay);
  return count;
}
```

### 도전 과제: useDelayedValue 구현

```js
import { useState, useEffect } from 'react';

function useDelayedValue(value, delay) {
  const [delayedValue, setDelayedValue] = useState(value);

  useEffect(() => {
    const timeout = setTimeout(() => {
      setDelayedValue(value);
    }, delay);
    return () => clearTimeout(timeout);
  }, [value, delay]);

  return delayedValue;
}
```

사용 예제 (마우스 포인터를 따라가는 점들):

```js
import { usePointerPosition } from './usePointerPosition.js';
import { useDelayedValue } from './useDelayedValue.js';

export default function Canvas() {
  const pos1 = usePointerPosition();
  const pos2 = useDelayedValue(pos1, 100);
  const pos3 = useDelayedValue(pos2, 200);
  const pos4 = useDelayedValue(pos3, 100);
  const pos5 = useDelayedValue(pos4, 50);
  return (
    <>
      <Dot position={pos1} opacity={1} />
      <Dot position={pos2} opacity={0.8} />
      <Dot position={pos3} opacity={0.6} />
      <Dot position={pos4} opacity={0.4} />
      <Dot position={pos5} opacity={0.2} />
    </>
  );
}

function Dot({ position, opacity }) {
  return (
    <div style={{
      position: 'absolute',
      backgroundColor: 'pink',
      borderRadius: '50%',
      opacity,
      transform: `translate(${position.x}px, ${position.y}px)`,
      pointerEvents: 'none',
      left: -20,
      top: -20,
      width: 40,
      height: 40,
    }} />
  );
}
```

```js
// usePointerPosition.js
import { useState, useEffect } from 'react';

export function usePointerPosition() {
  const [position, setPosition] = useState({ x: 0, y: 0 });
  useEffect(() => {
    function handleMove(e) {
      setPosition({ x: e.clientX, y: e.clientY });
    }
    window.addEventListener('pointermove', handleMove);
    return () => window.removeEventListener('pointermove', handleMove);
  }, []);
  return position;
}
```

### 요약

- 커스텀 Hook으로 컴포넌트 간 로직을 공유할 수 있음
- 커스텀 Hook은 `use`로 시작하고 대문자가 뒤따라야 함
- 커스텀 Hook은 상태 로직을 공유하지 state 자체를 공유하지 않음
- 한 Hook에서 다른 Hook으로 반응형 값을 전달할 수 있고, 최신 상태를 유지함
- 모든 Hook은 컴포넌트가 리렌더링될 때마다 다시 실행됨
- 커스텀 Hook의 코드는 컴포넌트 코드처럼 순수해야 함
- 커스텀 Hook이 받은 이벤트 핸들러를 Effect Event로 감쌀 것
- `useMount` 같은 커스텀 Hook을 만들지 말 것. 목적을 구체적으로 유지할 것
- 코드의 경계를 어디에 그을지는 개발자가 결정함

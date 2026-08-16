# React Hooks 레퍼런스

## 개요 (Hooks Overview)

> 원문: https://react.dev/reference/react/hooks

### 상태 훅 (State Hooks)

- 컴포넌트가 사용자 입력 등의 정보를 "기억"하게 해주는 훅임
- `useState` -- 직접 업데이트 가능한 상태 변수를 선언함
- `useReducer` -- 리듀서 함수 내부에 업데이트 로직을 담은 상태 변수를 선언함

```js
function ImageGallery() {
  const [index, setIndex] = useState(0);
  // ...
```

### 컨텍스트 훅 (Context Hooks)

- 컴포넌트가 먼 부모로부터 props 전달 없이 정보를 수신하게 해주는 훅임
- `useContext` -- 컨텍스트를 읽고 구독함

```js
function Button() {
  const theme = useContext(ThemeContext);
  // ...
```

### Ref 훅 (Ref Hooks)

- 렌더링에 사용되지 않는 정보(DOM 노드, 타임아웃 ID 등)를 보유하게 해주는 훅임
- ref 갱신은 리렌더를 유발하지 않음
- `useRef` -- ref를 선언함. 모든 타입의 값을 보유 가능하나, 주로 DOM 노드를 보유하는 데 사용됨
- `useImperativeHandle` -- 컴포넌트가 노출하는 ref를 커스터마이즈함. 거의 사용되지 않음

```js
function Form() {
  const inputRef = useRef(null);
  // ...
```

### Effect 훅 (Effect Hooks)

- 외부 시스템에 연결하고 동기화하는 훅임
- `useEffect` -- 컴포넌트를 외부 시스템에 연결함
- `useLayoutEffect` -- 브라우저가 화면을 다시 그리기 전에 실행됨. 레이아웃 측정에 사용함
- `useInsertionEffect` -- React가 DOM을 변경하기 전에 실행됨. 라이브러리에서 동적 CSS를 삽입하는 데 사용함
- `useEffectEvent` -- 모든 Effect 훅에서 발생시킬 비반응형 이벤트를 생성함

```js
function ChatRoom({ roomId }) {
  useEffect(() => {
    const connection = createConnection(roomId);
    connection.connect();
    return () => connection.disconnect();
  }, [roomId]);
  // ...
```

### 성능 훅 (Performance Hooks)

- 불필요한 작업을 건너뛰어 리렌더링 성능을 최적화하는 훅임

캐싱 관련:
- `useMemo` -- 비용이 큰 계산 결과를 캐싱함
- `useCallback` -- 최적화된 컴포넌트에 전달하기 전에 함수 정의를 캐싱함

우선순위 관련:
- `useTransition` -- 상태 전환을 비차단(non-blocking)으로 표시하여 다른 업데이트가 중간에 끼어들 수 있게 함
- `useDeferredValue` -- UI의 비핵심 부분 업데이트를 지연시키고 다른 부분이 먼저 업데이트되게 함

```js
function TodoList({ todos, tab, theme }) {
  const visibleTodos = useMemo(() => filterTodos(todos, tab), [todos, tab]);
  // ...
}
```

### 기타 훅 (Other Hooks)

- 주로 라이브러리 작성자에게 유용하며, 애플리케이션 코드에서는 일반적으로 사용되지 않음
- `useDebugValue` -- React DevTools에 커스텀 훅의 레이블을 표시함
- `useId` -- 컴포넌트에 고유 ID를 연결함. 접근성 API와 함께 사용됨
- `useSyncExternalStore` -- 외부 스토어를 구독함
- `useActionState` -- 액션의 상태를 관리함

---

## useState

> 원문: https://react.dev/reference/react/useState

- 컴포넌트에 상태 변수를 추가하는 훅임

```js
const [state, setState] = useState(initialState)
```

### 시그니처 (Signature)

```js
import { useState } from 'react';

function MyComponent() {
  const [age, setAge] = useState(28);
  const [name, setName] = useState('Taylor');
  const [todos, setTodos] = useState(() => createTodos());
  // ...
```

- 네이밍 규칙: 배열 구조분해로 `[something, setSomething]` 형태를 사용함

### 매개변수 (Parameters)

- `initialState`: 상태의 초기값. 모든 타입 가능. 함수를 전달하면 초기화 함수(initializer function)로 취급됨
  - 초기화 함수는 순수해야 하며, 인수를 받지 않고, 어떤 타입의 값이든 반환해야 함
  - React는 컴포넌트 초기화 시 초기화 함수를 호출하고, 반환값을 초기 상태로 저장함
  - 이 인수는 초기 렌더 이후 무시됨

### 반환값 (Returns)

- 정확히 두 개의 값을 가진 배열을 반환함:
  1. 현재 상태. 첫 번째 렌더 시 `initialState`와 동일함
  2. 상태를 다른 값으로 업데이트하고 리렌더를 트리거하는 `set` 함수

### 주의사항 (Caveats)

- 훅이므로 컴포넌트 최상위 수준 또는 커스텀 훅 내부에서만 호출 가능함. 루프나 조건문 내부에서 호출 불가함
- Strict Mode에서 React는 초기화 함수를 두 번 호출함(개발 모드에서만). 순수 함수라면 동작에 영향 없음

### set 함수

- `useState`가 반환하는 `set` 함수는 상태를 다른 값으로 업데이트하고 리렌더를 트리거함
- 다음 상태를 직접 전달하거나, 이전 상태에서 계산하는 함수를 전달할 수 있음

```js
const [name, setName] = useState('Edward');

function handleClick() {
  setName('Taylor');
  setAge(a => a + 1);
  // ...
```

#### set 함수 매개변수

- `nextState`: 상태가 될 값. 모든 타입 가능
  - 함수를 전달하면 업데이터 함수(updater function)로 취급됨
  - 업데이터 함수는 순수해야 하며, 대기 중인 상태를 유일한 인수로 받고, 다음 상태를 반환해야 함
  - React는 업데이터 함수를 큐에 넣고, 다음 렌더 시 모든 대기 중인 업데이터를 이전 상태에 순서대로 적용함

#### set 함수 반환값

- 반환값 없음

#### set 함수 주의사항

- `set` 함수는 다음 렌더의 상태 변수만 업데이트함. 호출 후에도 실행 중인 코드에서는 이전 값이 유지됨
- 새 값이 현재 `state`와 `Object.is` 비교로 동일하면 컴포넌트와 자식의 리렌더를 건너뜀(최적화)
- React는 상태 업데이트를 일괄 처리(batch)함. 모든 이벤트 핸들러가 실행되고 `set` 함수를 호출한 후 화면을 업데이트함. 즉시 업데이트가 필요하면 `flushSync`를 사용함
- `set` 함수는 안정적인 정체성(identity)을 가짐. Effect 의존성에서 생략해도 안전함
- 렌더링 중 `set` 함수 호출은 현재 렌더 중인 컴포넌트 내에서만, 조건문 안에서만 허용됨
- Strict Mode에서 업데이터 함수를 두 번 호출함(개발 모드에서만)

### 사용법: 이전 상태 기반 업데이트

```js
function handleClick() {
  setAge(age + 1); // setAge(42 + 1)
  setAge(age + 1); // setAge(42 + 1)
  setAge(age + 1); // setAge(42 + 1)
}
```

- 위 코드에서 클릭 후 `age`는 45가 아니라 43이 됨. `set` 함수가 실행 중인 코드의 상태를 변경하지 않기 때문임
- 해결: 업데이터 함수를 전달함

```js
function handleClick() {
  setAge(a => a + 1); // setAge(42 => 43)
  setAge(a => a + 1); // setAge(43 => 44)
  setAge(a => a + 1); // setAge(44 => 45)
}
```

- React는 업데이터 함수를 큐에 넣고, 다음 렌더 시 같은 순서로 호출함
- 관례적으로 대기 상태 인수에 상태 변수명의 첫 글자(`a`는 `age`)를 사용하거나, `prevAge` 같은 이름을 사용함

### 사용법: 객체 및 배열 상태 업데이트

- React에서 상태는 읽기 전용으로 취급됨. 기존 객체를 변이(mutate)하지 않고, 새 객체로 교체(replace)해야 함

```js
// 잘못된 방식: 객체 변이
form.firstName = 'Taylor';

// 올바른 방식: 새 객체로 교체
setForm({
  ...form,
  firstName: 'Taylor'
});
```

### 사용법: 초기 상태 재생성 방지

```js
// 비효율적: 매 렌더마다 createInitialTodos() 호출
function TodoList() {
  const [todos, setTodos] = useState(createInitialTodos());
  // ...

// 효율적: 초기화 함수로 전달 (함수 자체를 전달, 호출하지 않음)
function TodoList() {
  const [todos, setTodos] = useState(createInitialTodos);
  // ...
```

### 사용법: key로 상태 초기화

- 컴포넌트에 다른 `key`를 전달하면 상태가 초기화됨

```js
export default function App() {
  const [version, setVersion] = useState(0);

  function handleReset() {
    setVersion(version + 1);
  }

  return (
    <>
      <button onClick={handleReset}>Reset</button>
      <Form key={version} />
    </>
  );
}
```

### 사용법: 이전 렌더 정보 저장

- 렌더링 중 `set` 함수를 호출하는 패턴이 있으나 드물게 사용됨
- `prevCount !== count` 같은 조건 안에서만 호출해야 하며, 조건 내부에 `setPrevCount(count)` 같은 호출이 있어야 함
- 그렇지 않으면 무한 루프에 빠짐

```js
export default function CountLabel({ count }) {
  const [prevCount, setPrevCount] = useState(count);
  const [trend, setTrend] = useState(null);
  if (prevCount !== count) {
    setPrevCount(count);
    setTrend(count > prevCount ? 'increasing' : 'decreasing');
  }
  return (
    <>
      <h1>{count}</h1>
      {trend && <p>The count is {trend}</p>}
    </>
  );
}
```

### 문제 해결 (Troubleshooting)

- 상태를 업데이트했지만 로그에 이전 값이 출력됨: 상태는 스냅숏처럼 동작함. `set` 함수는 실행 중인 코드의 상태를 변경하지 않음
- 상태를 업데이트했지만 화면이 갱신되지 않음: `Object.is` 비교로 동일하면 업데이트 무시됨. 기존 객체를 변이하지 않고 새 객체를 생성해야 함
- "Too many re-renders" 오류: 렌더링 중 무조건적으로 상태를 설정하면 발생함. 이벤트 핸들러를 호출하지 않고 전달해야 함

```js
// 잘못됨: 렌더 중 핸들러를 호출
return <button onClick={handleClick()}>Click me</button>

// 올바름: 핸들러를 전달
return <button onClick={handleClick}>Click me</button>

// 올바름: 인라인 함수 전달
return <button onClick={(e) => handleClick(e)}>Click me</button>
```

- 함수를 상태로 저장하려면 `() =>`을 앞에 붙여야 함:

```js
const [fn, setFn] = useState(() => someFunction);

function handleClick() {
  setFn(() => someOtherFunction);
}
```

---

## useReducer

> 원문: https://react.dev/reference/react/useReducer

- 컴포넌트에 리듀서를 추가하여 복잡한 상태 로직을 관리하는 훅임

```js
const [state, dispatch] = useReducer(reducer, initialArg, init?)
```

### 매개변수

- `reducer`: 상태 업데이트 방식을 지정하는 리듀서 함수. 순수해야 하며, 상태와 액션을 인수로 받아 다음 상태를 반환함
- `initialArg`: 초기 상태가 계산되는 값. 모든 타입 가능. `init` 인수에 따라 초기 상태 계산 방식이 달라짐
- `init?` (선택): 초기 상태를 반환하는 초기화 함수. 지정하지 않으면 초기 상태는 `initialArg`가 됨. 지정하면 `init(initialArg)` 결과가 초기 상태가 됨

### 반환값

- 정확히 두 개의 값을 가진 배열:
  1. 현재 상태. 첫 번째 렌더 시 `init(initialArg)` 또는 `initialArg`
  2. 상태를 업데이트하고 리렌더를 트리거하는 `dispatch` 함수

### 주의사항

- 훅이므로 컴포넌트 최상위 수준 또는 커스텀 훅 내부에서만 호출 가능함
- `dispatch` 함수는 안정적인 정체성을 가짐. Effect 의존성에서 생략해도 안전함
- Strict Mode에서 리듀서와 초기화 함수를 두 번 호출함(개발 모드에서만)

### dispatch 함수

- `dispatch`에 액션을 전달하면 React가 현재 `state`와 액션으로 `reducer`를 호출하고, 결과를 다음 상태로 저장함

```js
const [state, dispatch] = useReducer(reducer, { age: 42 });

function handleClick() {
  dispatch({ type: 'incremented_age' });
}
```

#### dispatch 매개변수

- `action`: 사용자가 수행한 액션. 모든 타입 가능. 관례적으로 `type` 속성이 있는 객체를 사용함

#### dispatch 주의사항

- 다음 렌더의 상태 변수만 업데이트함
- `Object.is` 비교로 새 값이 현재 상태와 동일하면 리렌더를 건너뜀
- 상태 업데이트를 일괄 처리(batch)함

### 리듀서 함수 작성

```js
function reducer(state, action) {
  switch (action.type) {
    case 'incremented_age': {
      return {
        ...state,
        age: state.age + 1
      };
    }
    case 'changed_name': {
      return {
        ...state,
        name: action.nextName
      };
    }
  }
  throw Error('Unknown action: ' + action.type);
}
```

- 상태는 읽기 전용임. 기존 객체를 변이하지 않고 새 객체를 반환해야 함

```js
// 잘못됨: 기존 객체 변이
state.age = state.age + 1;
return state;

// 올바름: 새 객체 반환
return { ...state, age: state.age + 1 };
```

### 초기 상태 재생성 방지

```js
// 비효율적: 매 렌더마다 createInitialState 호출
const [state, dispatch] = useReducer(reducer, createInitialState(username));

// 효율적: 세 번째 인수로 초기화 함수 전달
const [state, dispatch] = useReducer(reducer, username, createInitialState);
```

### 문제 해결

- dispatch 후 로그에 이전 상태가 출력됨: 상태는 스냅숏임. 다음 상태를 알고 싶으면 직접 리듀서를 호출함

```js
const action = { type: 'incremented_age' };
dispatch(action);
const nextState = reducer(state, action);
```

- dispatch 후 화면이 갱신되지 않음: 기존 상태 객체를 변이하고 반환하면 `Object.is`가 동일하다고 판단하여 무시됨
- 상태 일부가 undefined가 됨: `case` 분기에서 `...state`로 기존 필드를 모두 복사해야 함

---

## useContext

> 원문: https://react.dev/reference/react/useContext

- 컴포넌트에서 컨텍스트를 읽고 구독하는 훅임

```js
const value = useContext(SomeContext)
```

### 매개변수

- `SomeContext`: `createContext`로 생성한 컨텍스트. 컨텍스트 자체는 정보를 보유하지 않으며, 컴포넌트에서 제공하거나 읽을 수 있는 정보의 종류를 나타냄

### 반환값

- 호출 컴포넌트에 대한 컨텍스트 값을 반환함
- 트리에서 가장 가까운 `SomeContext` 프로바이더의 `value`로 결정됨
- 프로바이더가 없으면 `createContext`에 전달한 `defaultValue`가 반환됨
- 반환값은 항상 최신임. 컨텍스트가 변경되면 React가 자동으로 리렌더함

### 주의사항

- 같은 컴포넌트에서 반환하는 프로바이더의 영향을 받지 않음. `<Context>`는 `useContext()` 호출 컴포넌트 위에 있어야 함
- 특정 컨텍스트를 사용하는 모든 자식을 자동으로 리렌더함. 이전 값과 다음 값을 `Object.is`로 비교함. `memo`로 리렌더를 건너뛰어도 자식은 새로운 컨텍스트 값을 받음
- 빌드 시스템이 출력에 중복 모듈을 생성하면(심볼릭 링크 등) 컨텍스트가 깨질 수 있음. `===` 비교로 정확히 동일한 객체여야 함

### 사용법: 트리에 데이터 전달

```js
function MyPage() {
  return (
    <ThemeContext value="dark">
      <Form />
    </ThemeContext>
  );
}
```

- 프로바이더와 컴포넌트 사이에 몇 개의 레이어가 있든 상관없음

### 사용법: 컨텍스트를 통한 데이터 업데이트

- 상태와 결합하여 사용함

```js
function MyPage() {
  const [theme, setTheme] = useState('dark');
  return (
    <ThemeContext value={theme}>
      <Form />
      <Button onClick={() => setTheme('light')}>
        Switch to light theme
      </Button>
    </ThemeContext>
  );
}
```

### 사용법: 트리 일부에 대한 컨텍스트 오버라이드

- 프로바이더를 중첩하여 트리 일부에 다른 값을 제공할 수 있음

```js
<ThemeContext value="dark">
  ...
  <ThemeContext value="light">
    <Footer />
  </ThemeContext>
  ...
</ThemeContext>
```

### 사용법: 객체 및 함수 전달 시 리렌더 최적화

- 컨텍스트에 객체와 함수를 전달할 때, `useMemo`와 `useCallback`으로 감싸면 불필요한 리렌더를 방지함

```js
const login = useCallback((response) => {
  storeCredentials(response.credentials);
  setCurrentUser(response.user);
}, []);

const contextValue = useMemo(() => ({
  currentUser,
  login
}), [currentUser, login]);

return (
  <AuthContext value={contextValue}>
    <Page />
  </AuthContext>
);
```

### 사용법: context와 reducer 결합

- 큰 앱에서는 컨텍스트와 리듀서를 결합하여 상태 관련 로직을 컴포넌트 밖으로 추출함

```js
export function TasksProvider({ children }) {
  const [tasks, dispatch] = useReducer(tasksReducer, initialTasks);
  return (
    <TasksContext value={tasks}>
      <TasksDispatchContext value={dispatch}>
        {children}
      </TasksDispatchContext>
    </TasksContext>
  );
}
```

### 문제 해결

- 프로바이더의 값이 보이지 않음: 같은 컴포넌트나 아래에서 `<Context>`를 렌더하는 경우, `<Context>`를 위로 이동해야 함
- `undefined`가 반환됨: `value` prop 없이 `<ThemeContext>`를 사용하면 `value={undefined}`를 전달한 것과 같음. `value` prop을 명시해야 함

---

## useRef

> 원문: https://react.dev/reference/react/useRef

- 렌더링에 필요하지 않은 값을 참조하는 훅임

```js
const ref = useRef(initialValue)
```

### 매개변수

- `initialValue`: ref 객체의 `current` 속성의 초기값. 모든 타입 가능. 초기 렌더 이후 무시됨

### 반환값

- 단일 속성을 가진 객체:
  - `current`: 초기에 전달한 `initialValue`로 설정됨. 이후 변경 가능함. JSX 노드의 `ref` 속성으로 전달하면 React가 `current`를 해당 DOM 노드로 설정함
- 이후 렌더에서 `useRef`는 동일한 객체를 반환함

### 주의사항

- `ref.current` 속성은 변이 가능함. 상태와 달리 변이해도 리렌더가 발생하지 않음
- 렌더링 중 `ref.current`를 읽거나 쓰지 말 것(초기화 제외). 동작이 예측 불가능해짐
- Strict Mode에서 컴포넌트 함수를 두 번 호출함(개발 모드에서만)

### 사용법: ref로 값 참조

- ref 변경은 리렌더를 유발하지 않으므로, 화면에 표시되지 않는 정보를 저장하는 데 적합함

```js
function Stopwatch() {
  const intervalRef = useRef(0);
  // ...

function handleStartClick() {
  const intervalId = setInterval(() => { /* ... */ }, 1000);
  intervalRef.current = intervalId;
}

function handleStopClick() {
  clearInterval(intervalRef.current);
}
```

- ref의 장점:
  - 리렌더 간 정보를 저장함(일반 변수와 달리 매 렌더마다 초기화되지 않음)
  - 변경해도 리렌더를 유발하지 않음(상태 변수와 달리)
  - 각 컴포넌트 복사본에 로컬함(외부 변수와 달리 공유되지 않음)

### 사용법: ref로 DOM 조작

```js
import { useRef } from 'react';

function MyComponent() {
  const inputRef = useRef(null);
  // ...
  return <input ref={inputRef} />;
```

- React가 DOM 노드를 생성하고 화면에 배치한 후 ref 객체의 `current`를 해당 DOM 노드로 설정함
- 노드가 화면에서 제거되면 `current`를 `null`로 설정함

```js
function handleClick() {
  inputRef.current.focus();
}
```

### 사용법: ref 내용 재생성 방지

```js
// 비효율적: 매 렌더마다 new VideoPlayer() 호출
const playerRef = useRef(new VideoPlayer());

// 효율적: 초기화 시에만 생성
const playerRef = useRef(null);
if (playerRef.current === null) {
  playerRef.current = new VideoPlayer();
}
```

- 렌더링 중 `ref.current` 쓰기는 일반적으로 허용되지 않지만, 결과가 항상 같고 초기화 시에만 실행되는 조건이라면 가능함

### 주의: 렌더링 중 ref 읽기/쓰기 금지

```js
// 잘못됨
function MyComponent() {
  myRef.current = 123;           // 렌더 중 쓰기 금지
  return <h1>{myOtherRef.current}</h1>;  // 렌더 중 읽기 금지
}

// 올바름
function MyComponent() {
  useEffect(() => {
    myRef.current = 123;  // Effect에서 읽기/쓰기 가능
  });
  function handleClick() {
    doSomething(myOtherRef.current);  // 이벤트 핸들러에서 가능
  }
}
```

### 커스텀 컴포넌트에 ref 전달

- 자식 컴포넌트에서 `ref`를 props로 받아 내부 DOM 요소에 전달함

```js
function MyInput({ value, onChange, ref }) {
  return (
    <input
      value={value}
      onChange={onChange}
      ref={ref}
    />
  );
}
```

---

## useEffect

> 원문: https://react.dev/reference/react/useEffect

- 컴포넌트를 외부 시스템에 연결하고 동기화하는 훅임

```js
useEffect(setup, dependencies?)
```

### 매개변수

- `setup`: Effect 로직을 포함하는 함수. 선택적으로 클린업(cleanup) 함수를 반환할 수 있음
  - 컴포넌트가 DOM에 커밋된 후 실행됨
  - 의존성이 변경된 매 커밋 후, 클린업이 먼저(이전 값으로) 실행되고, 그 다음 setup이(새 값으로) 실행됨
  - 컴포넌트가 DOM에서 제거된 후 클린업이 마지막으로 한 번 실행됨

- `dependencies` (선택): setup 코드 내부에서 참조하는 모든 반응형(reactive) 값의 목록
  - props, state, 컴포넌트 본문에서 직접 선언된 변수와 함수를 포함함
  - `[dep1, dep2, dep3]` 형태로 인라인 작성해야 함
  - `Object.is`로 각 의존성을 이전 값과 비교함
  - 생략하면: 매 커밋 후 Effect 재실행
  - 빈 배열 `[]`이면: 초기 커밋 후에만 실행
  - 값이 있으면: 의존성이 변경될 때만 재실행

### 반환값

- `undefined`

### 주의사항

- 훅이므로 컴포넌트 최상위 수준에서만 호출 가능함
- 외부 시스템과 동기화할 필요가 없으면 Effect가 필요 없을 수 있음
- Strict Mode에서 첫 번째 실제 setup 전에 추가 setup+cleanup 사이클을 실행함(스트레스 테스트)
- 의존성에 객체/함수가 있으면 불필요하게 자주 실행될 수 있음. Effect 내부에서 객체/함수를 생성하거나, 불필요한 의존성을 제거해야 함
- 시각적 작업이고 지연이 눈에 보이면 `useLayoutEffect`를 사용함
- Effect는 클라이언트에서만 실행됨. 서버 렌더링 중에는 실행되지 않음

### 사용 패턴: 외부 시스템 연결

```js
useEffect(() => {
  const connection = createConnection(serverUrl, roomId);
  connection.connect();
  return () => connection.disconnect();
}, [serverUrl, roomId]);
```

### 사용 패턴: 이전 상태 기반 상태 업데이트

- 상태를 의존성에 나열하면 무한 재실행 발생 가능. 업데이터 함수를 사용함

```js
useEffect(() => {
  const intervalId = setInterval(() => {
    setCount(c => c + 1);  // 업데이터 함수 사용
  }, 1000);
  return () => clearInterval(intervalId);
}, []);  // 의존성 비움
```

### 사용 패턴: 불필요한 객체 의존성 제거

```js
// 잘못됨: 매 렌더마다 새 객체 생성
const options = { serverUrl, roomId };
useEffect(() => { /* ... */ }, [options]);

// 올바름: Effect 내부에서 객체 생성
useEffect(() => {
  const options = { serverUrl, roomId };
  const connection = createConnection(options);
  // ...
}, [roomId, serverUrl]);
```

### 사용 패턴: 불필요한 함수 의존성 제거

```js
// 올바름: Effect 내부에서 함수 선언
useEffect(() => {
  function createOptions() {
    return { serverUrl, roomId };
  }
  const options = createOptions();
  // ...
}, [roomId]);
```

### 사용 패턴: 데이터 가져오기(fetching)

```js
useEffect(() => {
  let ignore = false;

  fetchBio(person).then(result => {
    if (!ignore) {
      setBio(result);
    }
  });

  return () => { ignore = true; };  // 경쟁 조건 방지
}, [person]);
```

### 커스텀 훅으로 Effect 래핑

```js
function useChatRoom({ serverUrl, roomId }) {
  useEffect(() => {
    const connection = createConnection(serverUrl, roomId);
    connection.connect();
    return () => connection.disconnect();
  }, [roomId, serverUrl]);
}

// 사용:
function ChatRoom({ roomId }) {
  useChatRoom({ roomId, serverUrl });
}
```

---

## useLayoutEffect

> 원문: https://react.dev/reference/react/useLayoutEffect

- 브라우저가 화면을 다시 그리기 전에 실행되는 `useEffect` 버전임
- 성능에 부정적 영향을 줄 수 있으므로, 가능하면 `useEffect`를 사용해야 함

```js
useLayoutEffect(setup, dependencies?)
```

### 매개변수

- `setup`: Effect 로직 함수. 선택적으로 클린업 함수를 반환함
  - 컴포넌트가 DOM에 커밋된 후, 브라우저가 화면을 다시 그리기 전에 실행됨
  - 의존성 변경 시 클린업(이전 값)을 먼저 실행한 후 setup(새 값)을 실행함
  - 컴포넌트 제거 전에 클린업을 실행함

- `dependencies` (선택): `useEffect`와 동일함

### 반환값

- `undefined`

### 주의사항

- `useLayoutEffect` 내부 코드와 예약된 상태 업데이트는 브라우저의 화면 리페인트를 차단함. 과도하게 사용하면 앱이 느려짐
- `useLayoutEffect` 내부에서 상태 업데이트를 트리거하면 `useEffect`를 포함한 모든 나머지 Effect를 즉시 실행함
- Effect는 클라이언트에서만 실행됨

### 사용법: 브라우저 리페인트 전에 레이아웃 측정

- 일반적인 컴포넌트는 위치와 크기를 알 필요가 없음
- 툴팁처럼 위치 결정이 필요한 경우 2단계 렌더링이 필요함:
  1. 임의 위치에 렌더링
  2. 높이를 측정하고 올바른 위치 결정
  3. 올바른 위치에 다시 렌더링
- 이 모든 과정이 브라우저 리페인트 전에 완료되어야 함

```js
function Tooltip() {
  const ref = useRef(null);
  const [tooltipHeight, setTooltipHeight] = useState(0);

  useLayoutEffect(() => {
    const { height } = ref.current.getBoundingClientRect();
    setTooltipHeight(height);
  }, []);

  // ...tooltipHeight를 렌더링 로직에 사용...
}
```

- `useLayoutEffect`는 브라우저 리페인트를 차단하므로, 2단계 렌더링이 있어도 사용자는 최종 결과만 봄
- `useEffect`를 사용하면 느린 장치에서 툴팁이 깜빡일 수 있음

### 서버 렌더링 문제

- 서버에는 레이아웃 정보가 없음. 서버 렌더링 시 `useLayoutEffect`를 사용하면 오류 발생
- 해결 방법:
  - `useEffect`로 대체함
  - 컴포넌트를 클라이언트 전용으로 표시함
  - `isMounted` 상태로 하이드레이션 후에만 `useLayoutEffect`가 포함된 컴포넌트를 렌더링함
  - `useSyncExternalStore`를 사용함(레이아웃 측정이 아닌 경우)

---

## useInsertionEffect

> 원문: https://react.dev/reference/react/useInsertionEffect

- 레이아웃 Effect가 실행되기 전에 DOM에 요소를 삽입하는 훅임
- CSS-in-JS 라이브러리 작성자를 위해 설계됨
- CSS-in-JS 라이브러리를 작업하지 않는 한 `useEffect`나 `useLayoutEffect`를 사용해야 함

```js
useInsertionEffect(setup, dependencies?)
```

### 매개변수

- `setup`: Effect 로직 함수. 선택적으로 클린업 함수를 반환함
  - 컴포넌트가 DOM에 추가될 때, 레이아웃 Effect가 실행되기 전에 실행됨
  - 의존성 변경 시 클린업(이전 값) 후 setup(새 값)을 실행함
  - 컴포넌트 제거 시 클린업을 실행함

- `dependencies` (선택): `useEffect`와 동일함

### 반환값

- `undefined`

### 주의사항

- 클라이언트에서만 실행됨
- `useInsertionEffect` 내부에서 상태를 업데이트할 수 없음
- 실행 시점에 ref가 아직 연결되지 않음
- DOM이 업데이트되기 전이나 후에 실행될 수 있음. 특정 시점의 DOM 상태에 의존하면 안 됨
- 다른 Effect와 달리 클린업과 setup을 컴포넌트별로 실행하여 "인터리빙(interleaving)"이 발생함

### 사용법: CSS-in-JS 라이브러리에서 동적 스타일 삽입

```js
let isInserted = new Set();

function useCSS(rule) {
  useInsertionEffect(() => {
    if (!isInserted.has(rule)) {
      isInserted.add(rule);
      document.head.appendChild(getStyleForRule(rule));
    }
  });
  return rule;
}

function Button() {
  const className = useCSS('...');
  return <div className={className} />;
}
```

- 런타임 `<style>` 태그 삽입은 브라우저가 스타일을 훨씬 더 자주 재계산하게 만듦
- `useInsertionEffect`는 다른 Effect가 실행되기 전에 스타일을 삽입하여 레이아웃 계산이 올바르게 됨

---

## useMemo

> 원문: https://react.dev/reference/react/useMemo

- 리렌더 간 계산 결과를 캐싱하는 훅임
- React Compiler가 자동으로 값과 함수를 메모이제이션하여 수동 `useMemo` 호출 필요성을 줄임

```js
const cachedValue = useMemo(calculateValue, dependencies)
```

### 매개변수

- `calculateValue`: 캐싱할 값을 계산하는 함수. 순수해야 하고, 인수를 받지 않으며, 모든 타입의 값을 반환해야 함
  - 초기 렌더에서 함수를 호출함
  - 이후 렌더에서 의존성이 변경되지 않으면 같은 값을 반환하고, 변경되면 다시 호출함

- `dependencies`: `calculateValue` 코드 내부에서 참조하는 모든 반응형 값의 목록
  - `[dep1, dep2, dep3]` 형태로 인라인 작성
  - `Object.is`로 각 의존성을 이전 값과 비교함

### 반환값

- 초기 렌더: `calculateValue` 호출 결과
- 이후 렌더: 의존성이 변경되지 않으면 저장된 값, 변경되면 `calculateValue`를 다시 호출한 결과

### 주의사항

- 훅이므로 컴포넌트 최상위 수준에서만 호출 가능함
- Strict Mode에서 계산 함수를 두 번 호출함(개발 모드에서만)
- React는 특정 이유가 없으면 캐시된 값을 폐기하지 않음. 단, 개발 모드에서 파일 편집 시 캐시를 폐기하며, 초기 마운트 중 서스펜스 시에도 폐기함

### 사용법: 비용이 큰 재계산 건너뛰기

```js
function TodoList({ todos, tab, theme }) {
  const visibleTodos = useMemo(
    () => filterTodos(todos, tab),
    [todos, tab]
  );
  // ...
}
```

- 성능 최적화로만 사용해야 함. 없이 코드가 동작하지 않으면 근본 문제를 해결해야 함

#### 계산이 비용이 큰지 판단하는 방법

```js
console.time('filter array');
const visibleTodos = filterTodos(todos, tab);
console.timeEnd('filter array');
```

- 기록된 시간이 1ms 이상이면 메모이제이션이 유의미할 수 있음
- Chrome DevTools의 CPU Throttling으로 테스트 가능

#### useMemo를 모든 곳에 추가해야 하는가

- 아님. 다음 경우에만 유용함:
  1. 계산이 눈에 띄게 느리고 의존성이 드물게 변경됨
  2. `memo`로 감싼 컴포넌트에 props로 전달함
  3. 값이 다른 훅의 의존성으로 사용됨

- 메모이제이션 필요성을 줄이는 원칙:
  1. JSX를 children으로 받아들이게 하면 래퍼 상태 변경 시 자식 리렌더 불필요
  2. 로컬 상태를 선호하고 불필요하게 끌어올리지 않음
  3. 렌더링 로직을 순수하게 유지함
  4. 상태를 업데이트하는 불필요한 Effect를 피함
  5. Effect에서 불필요한 의존성을 제거함

### 사용법: 컴포넌트 리렌더링 건너뛰기

- `memo`로 감싼 컴포넌트에 전달하는 값을 `useMemo`로 캐싱하면 props가 동일할 때 리렌더를 건너뜀

```js
export default function TodoList({ todos, tab, theme }) {
  const visibleTodos = useMemo(
    () => filterTodos(todos, tab),
    [todos, tab]
  );
  return (
    <div className={theme}>
      <List items={visibleTodos} />
    </div>
  );
}
```

### 사용법: Effect가 너무 자주 실행되는 것 방지

- 더 나은 방법은 객체를 Effect 내부로 이동하는 것임

```js
useEffect(() => {
  const options = {
    serverUrl: 'https://localhost:1234',
    roomId: roomId
  };
  const connection = createConnection(options);
  connection.connect();
  return () => connection.disconnect();
}, [roomId]);
```

### 사용법: 함수 메모이제이션

- `useMemo`로 함수를 캐싱할 수 있으나, `useCallback`이 더 편리함

```js
// useMemo로 함수 캐싱
const handleSubmit = useMemo(() => {
  return (orderDetails) => { post('/product/' + productId + '/buy', { referrer, orderDetails }); };
}, [productId, referrer]);

// useCallback으로 동일하게 (더 간결)
const handleSubmit = useCallback((orderDetails) => {
  post('/product/' + productId + '/buy', { referrer, orderDetails });
}, [productId, referrer]);
```

### 문제 해결

- 의존성 배열을 누락하면 매번 재계산됨
- 의존성 중 하나가 매 렌더마다 다르면 메모이제이션이 깨짐
- 화살표 함수에서 객체를 반환할 때 `() => ({})` 형태로 괄호를 사용하거나 명시적 `return`을 사용해야 함

---

## useCallback

> 원문: https://react.dev/reference/react/useCallback

- 리렌더 간 함수 정의를 캐싱하는 훅임
- React Compiler가 자동으로 값과 함수를 메모이제이션하여 수동 호출 필요성을 줄임

```js
const cachedFn = useCallback(fn, dependencies)
```

### 매개변수

- `fn`: 캐싱할 함수 값. 모든 인수를 받고 모든 값을 반환할 수 있음
  - 초기 렌더에서 함수를 반환함(호출하지 않음)
  - 이후 렌더에서 의존성이 변경되지 않으면 같은 함수를 반환하고, 변경되면 현재 렌더에서 전달한 함수를 반환함

- `dependencies`: `fn` 코드 내부에서 참조하는 모든 반응형 값의 목록

### 반환값

- 초기 렌더: 전달한 `fn` 함수
- 이후 렌더: 의존성 변경 시 현재 렌더의 `fn`, 변경 안 됐으면 이전에 저장된 `fn`

### useMemo와의 관계

- `useMemo`는 함수 호출 결과를 캐싱하고, `useCallback`은 함수 자체를 캐싱함

```js
// 단순화된 구현
function useCallback(fn, dependencies) {
  return useMemo(() => fn, dependencies);
}
```

### 사용법: 컴포넌트 리렌더링 건너뛰기

- `memo`로 감싼 컴포넌트에 함수를 전달할 때, `useCallback`으로 감싸면 의존성이 변경되지 않는 한 같은 함수 참조가 유지됨

```js
function ProductPage({ productId, referrer, theme }) {
  const handleSubmit = useCallback((orderDetails) => {
    post('/product/' + productId + '/buy', { referrer, orderDetails });
  }, [productId, referrer]);

  return (
    <div className={theme}>
      <ShippingForm onSubmit={handleSubmit} />
    </div>
  );
}
```

### 사용법: 메모이된 콜백에서 상태 업데이트

- 의존성을 줄이려면 업데이터 함수를 사용함

```js
const handleAddTodo = useCallback((text) => {
  const newTodo = { id: nextId++, text };
  setTodos(todos => [...todos, newTodo]);  // 업데이터 함수
}, []);  // todos 의존성 제거
```

### 사용법: 커스텀 훅 최적화

- 커스텀 훅이 반환하는 함수는 `useCallback`으로 감싸는 것이 권장됨

```js
function useRouter() {
  const { dispatch } = useContext(RouterStateContext);
  const navigate = useCallback((url) => {
    dispatch({ type: 'navigate', url });
  }, [dispatch]);
  const goBack = useCallback(() => {
    dispatch({ type: 'back' });
  }, [dispatch]);
  return { navigate, goBack };
}
```

---

## useTransition

> 원문: https://react.dev/reference/react/useTransition

- UI의 일부를 백그라운드에서 렌더링하게 해주는 훅임

```js
const [isPending, startTransition] = useTransition()
```

### 매개변수

- 없음

### 반환값

- 정확히 두 개의 값을 가진 배열:
  1. `isPending`: 대기 중인 트랜지션이 있는지 여부
  2. `startTransition`: 업데이트를 트랜지션으로 표시하는 함수

### startTransition(action)

- `startTransition`에 전달하는 함수를 "액션(Action)"이라 함
- 액션 내에서 상태를 업데이트하고, 부수 효과를 수행할 수 있으며, 작업은 백그라운드에서 이루어져 사용자 상호작용을 차단하지 않음

#### startTransition 매개변수

- `action`: 하나 이상의 `set` 함수를 호출하여 상태를 업데이트하는 함수. React는 즉시 `action`을 호출하고, 동기적으로 예약된 모든 상태 업데이트를 트랜지션으로 표시함. `await` 이후의 상태 업데이트는 추가 `startTransition`으로 감싸야 함

#### startTransition 반환값

- 없음

#### startTransition 주의사항

- `useTransition`은 훅이므로 컴포넌트나 커스텀 훅 내부에서만 호출 가능함. 다른 곳에서는 독립형 `startTransition`을 사용함
- 해당 상태의 `set` 함수에 접근할 수 있어야 업데이트를 트랜지션으로 감쌀 수 있음. prop이나 커스텀 훅 값에 대해서는 `useDeferredValue`를 사용함
- `startTransition`에 전달한 함수는 즉시 호출됨. `setTimeout` 안의 상태 업데이트는 트랜지션으로 표시되지 않음
- `await` 이후의 상태 업데이트는 별도의 `startTransition`으로 감싸야 함
- `startTransition` 함수는 안정적인 정체성을 가짐
- 트랜지션으로 표시된 상태 업데이트는 다른 상태 업데이트에 의해 중단됨
- 트랜지션 업데이트는 텍스트 입력을 제어하는 데 사용할 수 없음
- 여러 진행 중인 트랜지션이 있으면 React가 일괄 처리함

### 사용법: 비차단 업데이트

```js
function CheckoutForm() {
  const [isPending, startTransition] = useTransition();
  const [quantity, setQuantity] = useState(1);

  function onSubmit(newQuantity) {
    startTransition(async function () {
      const savedQuantity = await updateQuantity(newQuantity);
      startTransition(() => {
        setQuantity(savedQuantity);
      });
    });
  }
}
```

### 사용법: 대기 상태 시각적 표시

```js
function TabButton({ action, children, isActive }) {
  const [isPending, startTransition] = useTransition();
  if (isActive) return <b>{children}</b>;
  if (isPending) return <b className="pending">{children}</b>;
  return (
    <button onClick={() => {
      startTransition(async () => { await action(); });
    }}>
      {children}
    </button>
  );
}
```

### 사용법: Suspense 지원 라우터

```js
function Router() {
  const [page, setPage] = useState('/');
  const [isPending, startTransition] = useTransition();

  function navigate(url) {
    startTransition(() => { setPage(url); });
  }
}
```

- 트랜지션은 중단 가능하여 사용자가 리렌더 완료를 기다리지 않고 다른 곳을 클릭할 수 있음
- 이미 표시된 콘텐츠를 숨기는 것을 방지함

### 사용법: 오류 경계를 통한 오류 표시

- `startTransition`에 전달한 함수가 오류를 던지면 오류 경계(Error Boundary)로 표시할 수 있음

### 문제 해결

- 트랜지션 내에서 입력 업데이트가 작동하지 않음: 제어된 입력 상태에 트랜지션을 사용할 수 없음. 두 개의 상태 변수를 분리하거나 `useDeferredValue`를 사용함
- `await` 이후 상태 업데이트가 트랜지션으로 처리되지 않음: 추가 `startTransition`으로 감싸야 함
- 컴포넌트 외부에서 `useTransition`을 호출하려면 독립형 `startTransition`을 사용함

---

## useDeferredValue

> 원문: https://react.dev/reference/react/useDeferredValue

- UI의 일부 업데이트를 지연시키는 훅임

```js
const deferredValue = useDeferredValue(value)
```

### 매개변수

- `value` (필수): 지연시킬 값. 모든 타입 가능
- `initialValue` (선택): 컴포넌트 초기 렌더에 사용할 값. 생략하면 초기 렌더에서 지연하지 않음

### 반환값

- 초기 렌더: `initialValue`(있으면) 또는 전달한 `value`와 동일
- 업데이트 시: React가 먼저 이전 값으로 리렌더를 시도하고(이전 값 반환), 백그라운드에서 새 값으로 다시 리렌더를 시도함(업데이트된 값 반환)

### 주의사항

- 트랜지션 내부의 업데이트에서는 항상 새 `value`를 반환함(이미 지연됨)
- `useDeferredValue`에 전달하는 값은 원시 값(문자열, 숫자) 또는 렌더링 외부에서 생성된 객체여야 함. 렌더링 중 새 객체를 생성하여 전달하면 매번 달라져서 불필요한 백그라운드 리렌더가 발생함
- 다른 값을 받으면(`Object.is` 비교) 현재 렌더에 더해 백그라운드 리렌더를 예약함. 백그라운드 리렌더는 중단 가능하여, `value`에 새 업데이트가 있으면 처음부터 다시 시작함
- `<Suspense>`와 통합됨. 새 값으로 인한 백그라운드 업데이트가 서스펜드되면 폴백이 아닌 이전 지연 값을 계속 표시함
- 자체적으로 추가 네트워크 요청을 방지하지 않음
- 고정 지연이 없음. React가 원래 리렌더를 마치는 즉시 새 지연 값으로 백그라운드 리렌더 작업을 시작함
- 백그라운드 리렌더로 인한 Effect는 화면에 커밋될 때까지 실행되지 않음

### 사용법: 지연 렌더링으로 성능 최적화

```js
function App() {
  const [text, setText] = useState('');
  const deferredText = useDeferredValue(text);
  return (
    <>
      <input value={text} onChange={e => setText(e.target.value)} />
      <SlowList text={deferredText} />
    </>
  );
}
```

- `SlowList`는 반드시 `memo`로 감싸야 최적화가 동작함
- 입력은 즉시 반응하고, 목록은 "뒤처져서" 업데이트됨

### 사용법: 오래된 콘텐츠 표시 중임을 나타내기

```js
const isStale = query !== deferredQuery;
// ...
<div style={{
  opacity: isStale ? 0.5 : 1,
  transition: isStale ? 'opacity 0.2s 0.2s linear' : 'opacity 0s 0s linear'
}}>
  <SearchResults query={deferredQuery} />
</div>
```

### 디바운싱/쓰로틀링과의 차이

- `useDeferredValue`는 고정 지연이 없음. 사용자 장치가 빠르면 거의 즉시 완료됨
- 장치 성능에 자동 적응함
- 지연 리렌더는 기본적으로 중단 가능함. 디바운싱/쓰로틀링은 차단적(blocking)임
- 렌더링 최적화에 적합함. 네트워크 요청 감소에는 디바운싱/쓰로틀링이 여전히 유용하며, 함께 사용할 수 있음

---

## useId

> 원문: https://react.dev/reference/react/useId

- 접근성 속성에 전달할 고유 ID를 생성하는 훅임

```js
const id = useId()
```

### 매개변수

- 없음

### 반환값

- 특정 컴포넌트의 특정 `useId` 호출과 연결된 고유 ID 문자열

### 주의사항

- 훅이므로 컴포넌트 최상위 수준에서만 호출 가능함
- `use()`의 캐시 키를 생성하는 데 사용하면 안 됨. ID는 마운트 시 안정적이지만 렌더링 중 변경될 수 있음
- 리스트의 key를 생성하는 데 사용하면 안 됨. 키는 데이터에서 생성해야 함
- 비동기 서버 컴포넌트에서는 현재 사용 불가함

### 사용법: 접근성 속성에 고유 ID 생성

```js
function PasswordField() {
  const passwordHintId = useId();
  return (
    <>
      <label>
        Password:
        <input type="password" aria-describedby={passwordHintId} />
      </label>
      <p id={passwordHintId}>
        The password should contain at least 18 characters
      </p>
    </>
  );
}
```

### 사용법: 관련 요소 여러 개에 ID 생성

- 하나의 `useId` 호출에 접미사를 붙여 여러 ID를 생성함

```js
export default function Form() {
  const id = useId();
  return (
    <form>
      <label htmlFor={id + '-firstName'}>First Name:</label>
      <input id={id + '-firstName'} type="text" />
      <hr />
      <label htmlFor={id + '-lastName'}>Last Name:</label>
      <input id={id + '-lastName'} type="text" />
    </form>
  );
}
```

### 사용법: 공유 접두사 지정

- 한 페이지에 여러 독립 React 앱이 있으면 `createRoot`나 `hydrateRoot`에 `identifierPrefix`를 전달하여 ID 충돌을 방지함

```js
const root1 = createRoot(document.getElementById('root1'), {
  identifierPrefix: 'my-first-app-'
});
const root2 = createRoot(document.getElementById('root2'), {
  identifierPrefix: 'my-second-app-'
});
```

### 증분 카운터보다 useId가 나은 이유

- 서버 렌더링에서 동작을 보장함
- 클라이언트 컴포넌트의 하이드레이션 순서가 서버 HTML 출력 순서와 다를 수 있지만, `useId`는 "부모 경로"에서 생성되므로 순서에 관계없이 일치함

---

## useImperativeHandle

> 원문: https://react.dev/reference/react/useImperativeHandle

- ref로 노출되는 핸들을 커스터마이즈하는 훅임

```js
useImperativeHandle(ref, createHandle, dependencies?)
```

### 매개변수

- `ref`: 컴포넌트가 prop으로 받은 ref
- `createHandle`: 인수 없이 노출할 ref 핸들을 반환하는 함수. 보통 노출할 메서드가 있는 객체를 반환함
- `dependencies?` (선택): `createHandle` 코드 내부에서 참조하는 모든 반응형 값의 목록. `Object.is`로 비교하며, 의존성이 변경되거나 생략되면 `createHandle`을 재실행하고 새 핸들을 ref에 할당함

### 반환값

- `undefined`

### React 19 참고

- React 19부터 `ref`를 prop으로 사용 가능함. React 18 이하에서는 `forwardRef`가 필요했음

### 사용법: 선택적 DOM 메서드 노출

```js
import { useRef, useImperativeHandle } from 'react';

function MyInput({ ref }) {
  const inputRef = useRef(null);

  useImperativeHandle(ref, () => {
    return {
      focus() {
        inputRef.current.focus();
      },
      scrollIntoView() {
        inputRef.current.scrollIntoView();
      },
    };
  }, []);

  return <input ref={inputRef} />;
}
```

- 부모 컴포넌트가 `ref.current.focus()`와 `ref.current.scrollIntoView()`만 호출 가능함
- 전체 DOM 노드에 접근할 수 없음

### 사용법: 커스텀 명령형 메서드 노출

```js
function Post({ ref }) {
  const commentsRef = useRef(null);
  const addCommentRef = useRef(null);

  useImperativeHandle(ref, () => {
    return {
      scrollAndFocusAddComment() {
        commentsRef.current.scrollToBottom();
        addCommentRef.current.focus();
      }
    };
  }, []);
  // ...
}
```

### 주의: ref를 과도하게 사용하지 말 것

- ref는 스크롤, 포커스, 애니메이션 트리거, 텍스트 선택 등 props로 표현할 수 없는 명령형 동작에만 사용해야 함
- props로 표현 가능한 것은 ref 대신 props를 사용해야 함. 예를 들어 `{ open, close }` 대신 `<Modal isOpen={isOpen} />`을 사용함

---

## useSyncExternalStore

> 원문: https://react.dev/reference/react/useSyncExternalStore

- 외부 스토어를 구독하는 훅임

```js
const snapshot = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot?)
```

### 매개변수

- `subscribe`: 단일 `callback` 인수를 받아 스토어를 구독하는 함수. 스토어가 변경되면 `callback`을 호출해야 함. 구독 해제 함수를 반환해야 함
- `getSnapshot`: 스토어에서 데이터의 스냅숏을 반환하는 함수. 스토어가 변경되지 않으면 반복 호출 시 같은 값을 반환해야 함. 변경되어 다른 값을 반환하면(`Object.is` 비교) React가 컴포넌트를 리렌더함
- `getServerSnapshot` (선택): 스토어 데이터의 초기 스냅숏을 반환하는 함수. 서버 렌더링과 클라이언트의 서버 렌더 콘텐츠 하이드레이션에서만 사용됨. 서버와 클라이언트 간 동일해야 함. 생략하면 서버 렌더링 시 오류가 발생함

### 반환값

- 렌더링 로직에 사용할 수 있는 스토어의 현재 스냅숏

### 주의사항

- `getSnapshot`이 반환하는 스냅숏은 불변이어야 함. 변이 가능한 데이터가 있으면 변경 시 새 불변 스냅숏을 반환해야 함
- 리렌더 시 다른 `subscribe` 함수가 전달되면 React가 새 함수로 재구독함. 컴포넌트 외부에서 `subscribe`를 선언하면 방지할 수 있음
- 비차단 트랜지션 업데이트 중 스토어가 변이되면 React가 해당 업데이트를 차단 업데이트로 폴백함
- `useSyncExternalStore`가 반환한 스토어 값으로 렌더를 서스펜드하는 것은 권장하지 않음

### 사용법: 외부 스토어 구독

```js
import { useSyncExternalStore } from 'react';
import { todosStore } from './todoStore.js';

function TodosApp() {
  const todos = useSyncExternalStore(todosStore.subscribe, todosStore.getSnapshot);
  // ...
}
```

- 가능하면 React 내장 상태(`useState`, `useReducer`)를 사용하는 것이 권장됨
- `useSyncExternalStore`는 기존 비React 코드와 통합해야 할 때 유용함

### 사용법: 브라우저 API 구독

```js
function ChatIndicator() {
  const isOnline = useSyncExternalStore(subscribe, getSnapshot);
  return <h1>{isOnline ? 'Online' : 'Disconnected'}</h1>;
}

function getSnapshot() {
  return navigator.onLine;
}

function subscribe(callback) {
  window.addEventListener('online', callback);
  window.addEventListener('offline', callback);
  return () => {
    window.removeEventListener('online', callback);
    window.removeEventListener('offline', callback);
  };
}
```

### 사용법: 커스텀 훅으로 추출

```js
export function useOnlineStatus() {
  const isOnline = useSyncExternalStore(subscribe, getSnapshot);
  return isOnline;
}
```

### 사용법: 서버 렌더링 지원

- 세 번째 인수 `getServerSnapshot`을 전달함

```js
export function useOnlineStatus() {
  const isOnline = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);
  return isOnline;
}

function getServerSnapshot() {
  return true;  // 서버에서는 항상 "Online" 표시
}
```

### 문제 해결

- "`getSnapshot` 결과가 캐시되어야 합니다" 오류: `getSnapshot`이 매 호출마다 새 객체를 반환하면 발생함. 데이터가 변경되지 않으면 같은 객체를 반환해야 함
- `subscribe` 함수가 매 리렌더마다 호출됨: 컴포넌트 내부에 정의하면 매 렌더마다 다른 함수가 되므로, 컴포넌트 외부로 이동하거나 `useCallback`으로 감싸야 함

---

## useDebugValue

> 원문: https://react.dev/reference/react/useDebugValue

- 커스텀 훅에 React DevTools에서 표시할 레이블을 추가하는 훅임

```js
useDebugValue(value, format?)
```

### 매개변수

- `value`: React DevTools에 표시할 값. 모든 타입 가능
- `format` (선택): 포맷팅 함수. 컴포넌트를 검사할 때 React DevTools가 `value`를 인수로 이 함수를 호출하고, 반환된 포맷된 값을 표시함

### 반환값

- 없음

### 사용법

```js
import { useDebugValue } from 'react';

function useOnlineStatus() {
  // ...
  useDebugValue(isOnline ? 'Online' : 'Offline');
  // ...
}
```

### 포맷팅 지연

- 비용이 큰 포맷팅 로직을 두 번째 인수로 전달하여 실제 검사할 때만 실행함

```js
useDebugValue(date, date => date.toDateString());
```

### 사용 지침

- 모든 커스텀 훅에 추가하지 말 것. 공유 라이브러리의 복잡한 내부 데이터 구조를 가진 커스텀 훅에 가장 유용함

---

## useOptimistic

> 원문: https://react.dev/reference/react/useOptimistic

- 비동기 액션이 진행 중일 때 UI를 낙관적으로 업데이트하는 훅임

```js
const [optimisticState, setOptimistic] = useOptimistic(value, reducer?)
```

### 매개변수

- `value`: 대기 중인 액션이 없을 때 반환할 값
- `reducer(currentState, action)` (선택): 낙관적 상태의 업데이트 방식을 지정하는 순수 리듀서 함수

### 반환값

- 정확히 두 개의 값을 가진 배열:
  1. `optimisticState`: 현재 낙관적 상태. 액션 대기 중이 아니면 `value`와 같고, 대기 중이면 `reducer`(또는 리듀서 없을 때 set 함수에 전달한 값)의 반환값
  2. `setOptimistic`: 액션 중 낙관적 상태를 업데이트하는 set 함수

### set 함수 주의사항

- 반드시 `startTransition` 또는 액션 prop 내부에서 호출해야 함. 외부에서 호출하면 낙관적 상태가 잠시 표시된 후 즉시 되돌아감

### 낙관적 상태 동작 원리

1. `setOptimistic('b')` 호출 시 React가 즉시 `'b'`로 렌더함
2. (선택) 액션에서 `await`하면 React가 계속 `'b'`를 표시함
3. `setValue(newValue)`가 실제 상태 업데이트를 예약함
4. (선택) `newValue`가 서스펜드되면 React가 계속 `'b'`를 표시함
5. 최종적으로 `newValue`가 `value`와 `optimistic` 모두에 커밋됨
- 낙관적 상태를 "지우는" 별도의 렌더가 없음. 트랜지션 완료 시 낙관적 상태와 실제 상태가 같은 렌더에서 수렴함

### 오류 처리

- 액션이 오류를 던지면 트랜지션이 종료되고 React가 현재 `value`로 렌더함
- 부모는 성공 시에만 `value`를 업데이트하므로, 실패 시 `value`는 변경되지 않아 UI가 낙관적 업데이트 전 상태로 돌아감

### 사용법: 기본 사용

```js
const [isLiked, setIsLiked] = useState(false);
const [optimisticIsLiked, setOptimisticIsLiked] = useOptimistic(isLiked);

function handleClick() {
  startTransition(async () => {
    const newValue = !optimisticIsLiked;
    setOptimisticIsLiked(newValue);
    const updatedValue = await toggleLike(newValue);
    startTransition(() => {
      setIsLiked(updatedValue);
    });
  });
}
```

### 사용법: 리듀서로 여러 값 함께 업데이트

```js
const [optimisticState, updateOptimistic] = useOptimistic(
  { isFollowing: user.isFollowing, followerCount: user.followerCount },
  (current, isFollowing) => ({
    isFollowing,
    followerCount: current.followerCount + (isFollowing ? 1 : -1)
  })
);
```

### 사용법: 낙관적 삭제와 오류 복구

- 삭제 실패 시 항목이 자동으로 목록에 다시 나타남. 낙관적 상태가 원래 값으로 되돌아가기 때문임

### 업데이터와 리듀서 선택 기준

- 업데이터 함수: 설정 호출이 자연스럽게 업데이트를 설명하는 경우. `useState`의 `prev => ...`와 유사
- 리듀서: 업데이트에 데이터를 전달해야 하거나 여러 유형의 업데이트를 처리해야 하는 경우
- 리듀서의 중요한 장점: 트랜지션 대기 중 기반 상태가 변경되면 React가 새 상태로 리듀서를 재실행하여 최신 데이터에 낙관적 변경을 적용함

### 대기 상태를 위한 사용

```js
const [isPending, setIsPending] = useOptimistic(false);

return (
  <button
    disabled={isPending}
    onClick={() => {
      startTransition(async () => {
        setIsPending(true);
        await action();
      });
    }}
  >
    {isPending ? 'Submitting...' : children}
  </button>
);
```

### 문제 해결

- "An optimistic state update occurred outside a Transition or Action" 오류: `startTransition` 내부에서 set 함수를 호출해야 함
- 낙관적 업데이트가 오래된 값을 표시함: 업데이터 함수나 리듀서를 사용하여 현재 상태 기준으로 계산해야 함

---

## useActionState

> 원문: https://react.dev/reference/react/useActionState

- 액션을 사용하여 부수 효과와 함께 상태를 업데이트하는 훅임

```js
const [state, dispatchAction, isPending] = useActionState(reducerAction, initialState, permalink?)
```

### 매개변수

- `reducerAction`: 액션이 트리거될 때 호출되는 함수. 이전 상태(첫 호출 시 `initialState`)를 첫 번째 인수로, `dispatchAction`에 전달된 `actionPayload`를 후속 인수로 받음. 비동기 가능하며 부수 효과 수행 가능
- `initialState`: 상태의 초기값. `dispatchAction`이 처음 호출된 후 무시됨
- `permalink?` (선택): 이 폼이 수정하는 고유 페이지 URL 문자열. React 서버 컴포넌트와 점진적 향상(progressive enhancement)에 사용됨

### 반환값

- 정확히 세 개의 값을 가진 배열:
  1. 현재 상태. 첫 렌더 시 `initialState`와 동일함
  2. `dispatchAction` 함수. 액션 내부에서 호출함
  3. `isPending` 플래그. 이 훅에 대한 dispatch된 액션이 대기 중인지 여부

### 주의사항

- 훅이므로 컴포넌트 최상위 수준에서만 호출 가능함
- React는 여러 `dispatchAction` 호출을 순차적으로 큐에 넣고 실행함. 각 `reducerAction` 호출은 이전 호출의 결과를 받음
- `dispatchAction` 함수는 안정적인 정체성을 가짐
- `dispatchAction`이 오류를 던지면 React는 큐에 있는 모든 액션을 취소하고 가장 가까운 Error Boundary를 표시함
- `dispatchAction`은 액션에서 호출해야 함. `startTransition`으로 감싸거나 액션 prop에 전달해야 함

### reducerAction 함수

- `useReducer`의 리듀서와 달리, 비동기 가능하고 부수 효과를 수행할 수 있음

```js
async function reducerAction(previousState, actionPayload) {
  const newState = await post(actionPayload);
  return newState;
}
```

- `useReducer`와의 차이:
  - `useReducer`는 UI 상태 관리. 리듀서가 순수해야 함
  - `useActionState`는 액션의 상태 관리. 리듀서에서 부수 효과 수행 가능
- Strict Mode에서 두 번 호출되지 않음(부수 효과를 허용하도록 설계되었으므로)
- 서버 함수 사용 시 `actionPayload`는 직렬화 가능해야 함

### 사용법: 기본 사용

```js
import { useActionState, startTransition } from 'react';

export default function Checkout() {
  const [count, dispatchAction, isPending] = useActionState(async (prevCount) => {
    return await addToCart(prevCount);
  }, 0);

  function handleClick() {
    startTransition(() => {
      dispatchAction();
    });
  }

  return (
    <div>
      <span>Qty: {count}</span>
      <button onClick={handleClick}>Add Ticket{isPending ? ' ...' : ''}</button>
    </div>
  );
}
```

### 사용법: 여러 액션 유형

```js
async function updateCartAction(prevCount, actionPayload) {
  switch (actionPayload.type) {
    case 'ADD': return await addToCart(prevCount);
    case 'REMOVE': return await removeFromCart(prevCount);
  }
  return prevCount;
}
```

### 사용법: useOptimistic과 결합

```js
const [count, dispatchAction, isPending] = useActionState(updateCartAction, 0);
const [optimisticCount, setOptimisticCount] = useOptimistic(count);

function handleAdd() {
  startTransition(() => {
    setOptimisticCount(c => c + 1);
    dispatchAction({ type: 'ADD' });
  });
}
```

### 사용법: `<form>` action prop과 함께 사용

- `dispatchAction`을 `<form>`의 `action` prop으로 전달하면 React가 자동으로 트랜지션으로 감쌈
- `reducerAction`은 이전 상태를 첫 번째 인수로, FormData를 두 번째 인수로 받음

### 사용법: 대기 중인 액션 취소

- `AbortController`를 사용하여 대기 중인 액션을 취소할 수 있음

### 오류 처리

- 알려진 오류: `reducerAction` 상태의 일부로 반환하여 UI에 표시함
- 알 수 없는 오류: 던지면 React가 모든 큐의 액션을 취소하고 가장 가까운 Error Boundary를 표시함

### 문제 해결

- `isPending`이 업데이트되지 않음: `dispatchAction`을 `startTransition`으로 감싸야 함
- FormData를 읽을 수 없음: `useActionState` 사용 시 `reducerAction`의 첫 번째 인수가 이전 상태이고, 두 번째 인수가 FormData임
- 액션이 건너뛰어짐: 이전 `dispatchAction` 호출이 오류를 던지면 이후 큐의 액션이 취소됨. `reducerAction` 내부에서 오류를 잡아 상태로 반환해야 함

---

## useEffectEvent

> 원문: https://react.dev/reference/react/useEffectEvent

- Effect에서 이벤트를 분리하는 훅임

```js
const onEvent = useEffectEvent(callback)
```

### 매개변수

- `callback`: Effect Event의 로직을 포함하는 함수. 모든 인수를 받고 모든 값을 반환할 수 있음. 호출 시점에 항상 렌더에서 커밋된 최신 값에 접근함

### 반환값

- `callback`과 동일한 타입 시그니처를 가진 Effect Event 함수

### 주의사항

- 훅이므로 컴포넌트 최상위 수준에서만 호출 가능함
- Effect Event는 Effect나 다른 Effect Event 내부에서만 호출할 수 있음. 렌더링 중 호출하거나 다른 컴포넌트나 훅에 전달하면 안 됨
- 의존성 배열에서 의존성을 피하기 위해 사용하면 안 됨. 진정으로 Effect를 다시 트리거하지 않아야 하는 로직에만 사용해야 함
- Effect Event 함수는 안정적인 정체성을 갖지 않음. 매 렌더마다 의도적으로 정체성이 변경됨

### 왜 Effect Event가 안정적이지 않은가

- 의도적인 설계 선택임. Effect 내부에서만 로컬로 호출되도록 설계됨
- 비안정적 정체성은 런타임 어설션(assertion)으로 작동함: 코드가 함수 정체성에 잘못 의존하면 Effect가 매 렌더마다 재실행되어 버그가 명확해짐

### 사용법: Effect에서 이벤트 사용

```js
function ChatRoom({ roomId, theme }) {
  const onConnected = useEffectEvent(() => {
    showNotification('Connected!', theme);
  });

  useEffect(() => {
    const connection = createConnection(roomId);
    connection.on('connected', onConnected);
    connection.connect();
    return () => connection.disconnect();
  }, [roomId]);  // theme은 의존성에 포함하지 않음
}
```

- `onConnected`는 Effect Event이므로 `theme`과 `onConnected`는 Effect 의존성에 포함하지 않음
- `theme`이 변경되어도 채팅 연결이 재연결되지 않음

### 사용법: 타이머에서 최신 값 사용

```js
const onTick = useEffectEvent(() => {
  setCount(count + increment);
});

useEffect(() => {
  const id = setInterval(() => { onTick(); }, 1000);
  return () => clearInterval(id);
}, []);
```

- `increment` 값이 변경되어도 인터벌이 재시작되지 않으면서 최신 `increment` 값을 사용함

### 사용법: 외부 시스템 재연결 방지

```js
const onConnected = useEffectEvent((roomId) => {
  if (!muted) {
    showNotification('Connected to ' + roomId);
  }
});

useEffect(() => {
  const connection = createConnection(roomId);
  connection.on('connected', () => { onConnected(roomId); });
  connection.connect();
  return () => connection.disconnect();
}, [roomId]);
```

- `muted`가 변경되어도 채팅 연결이 유지됨

### 사용법: 커스텀 훅에서 Effect Event 사용

```js
function useInterval(callback, delay) {
  const onTick = useEffectEvent(callback);
  useEffect(() => {
    if (delay === null) return;
    const id = setInterval(() => { onTick(); }, delay);
    return () => clearInterval(id);
  }, [delay]);
}
```

### 주의: 의존성을 피하기 위해 Effect Event를 사용하지 말 것

```js
// 잘못됨: 의존성을 숨기기 위해 사용
const logVisit = useEffectEvent(() => { log(pageUrl); });
useEffect(() => { logVisit(); }, []);  // pageUrl 변경 시 로그 누락
```

- 값이 Effect를 다시 실행해야 한다면 의존성에 유지해야 함

---

## useFormStatus

> 원문: https://react.dev/reference/react-dom/hooks/useFormStatus

- 마지막 폼 제출의 상태 정보를 제공하는 훅임
- `react-dom`에서 가져옴

```js
const { pending, data, method, action } = useFormStatus();
```

### 매개변수

- 없음

### 반환값

- 다음 속성을 가진 `status` 객체:
  - `pending`: 불리언. `true`이면 부모 `<form>`이 제출 대기 중임
  - `data`: `FormData` 인터페이스를 구현하는 객체. 부모 `<form>`이 제출 중인 데이터를 포함함. 활성 제출이 없거나 부모 `<form>`이 없으면 `null`
  - `method`: `'get'` 또는 `'post'` 문자열. 부모 `<form>`이 GET 또는 POST HTTP 메서드로 제출하는지 나타냄
  - `action`: 부모 `<form>`의 `action` prop에 전달된 함수 참조. 부모 `<form>`이 없거나, URI 값이 제공되었거나, `action` prop이 지정되지 않으면 `null`

### 주의사항

- `<form>` 내부에서 렌더되는 컴포넌트에서 호출해야 함
- 부모 `<form>`의 상태 정보만 반환함. 같은 컴포넌트나 자식 컴포넌트에서 렌더된 `<form>`의 상태는 반환하지 않음

### 사용법: 폼 제출 중 대기 상태 표시

```js
import { useFormStatus } from "react-dom";

function Submit() {
  const { pending } = useFormStatus();
  return (
    <button type="submit" disabled={pending}>
      {pending ? "Submitting..." : "Submit"}
    </button>
  );
}

function Form({ action }) {
  return (
    <form action={action}>
      <Submit />
    </form>
  );
}
```

### 주의: 같은 컴포넌트의 `<form>` 상태를 추적하지 않음

```js
// 잘못됨: 같은 컴포넌트의 form은 추적하지 않음
function Form() {
  const { pending } = useFormStatus();  // pending은 항상 false
  return <form action={submit}></form>;
}

// 올바름: 자식 컴포넌트에서 호출
function Submit() {
  const { pending } = useFormStatus();  // 부모 form을 추적함
  return <button disabled={pending}>...</button>;
}

function Form() {
  return (
    <form action={submit}>
      <Submit />
    </form>
  );
}
```

### 사용법: 제출 중인 폼 데이터 읽기

```js
export default function UsernameForm() {
  const { pending, data } = useFormStatus();
  return (
    <div>
      <input type="text" name="username" disabled={pending} />
      <button type="submit" disabled={pending}>Submit</button>
      <p>{data ? `Requesting ${data?.get("username")}...` : ''}</p>
    </div>
  );
}
```

### 문제 해결

- `status.pending`이 `true`가 되지 않음:
  - `useFormStatus`를 호출하는 컴포넌트가 `<form>` 내부에 중첩되어 있는지 확인함
  - 같은 컴포넌트에서 렌더된 `<form>`의 상태는 추적하지 않음

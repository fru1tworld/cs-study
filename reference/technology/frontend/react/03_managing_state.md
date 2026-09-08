# 상태 관리 (Managing State)

> 원문: https://react.dev/learn/managing-state

앱이 커지면 중복되거나 불필요한 상태가 버그의 주요 원인이 된다. 이를 줄이려면 어떤 값을 상태로 보관할지, 컴포넌트 사이에서 그 값을 어떻게 전달할지 함께 설계해야 한다.

# 입력에 상태로 반응하기 (Reacting to Input with State)

> 원문: https://react.dev/learn/reacting-to-input-with-state

## 선언형 UI와 명령형 UI 비교

React는 선언형(declarative) 방식으로 UI를 조작한다. 명령형(imperative) 방식은 "버튼을 비활성화해라", "메시지를 보여라"처럼 각 요소에 직접 명령한다. 택시를 타고 교차로마다 방향을 지시하는 것과 같아서, 단순한 경우에는 동작하지만 UI가 복잡해질수록 관리가 기하급수적으로 어려워진다.

선언형 방식에서는 각 시각적 상태에서 보여 주고 싶은 UI를 기술하고, 사용자 입력에 따라 상태를 전환한다. 택시 기사에게 목적지만 알려주는 것과 비슷하게, UI를 어떻게 갱신할지는 React가 알아서 처리한다.

### 명령형 예시 (React 없이)

```js
async function handleFormSubmit(e) {
  e.preventDefault();
  disable(textarea);
  disable(button);
  show(loadingMessage);
  hide(errorMessage);
  try {
    await submitForm(textarea.value);
    show(successMessage);
    hide(form);
  } catch (err) {
    show(errorMessage);
    errorMessage.textContent = err.message;
  } finally {
    hide(loadingMessage);
    enable(textarea);
    enable(button);
  }
}

function handleTextareaChange() {
  if (textarea.value.length === 0) {
    disable(button);
  } else {
    enable(button);
  }
}

function hide(el) {
  el.style.display = 'none';
}

function show(el) {
  el.style.display = '';
}

function enable(el) {
  el.disabled = false;
}

function disable(el) {
  el.disabled = true;
}

function submitForm(answer) {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      if (answer.toLowerCase() === 'istanbul') {
        resolve();
      } else {
        reject(new Error('Good guess but a wrong answer. Try again!'));
      }
    }, 1500);
  });
}

let form = document.getElementById('form');
let textarea = document.getElementById('textarea');
let button = document.getElementById('button');
let loadingMessage = document.getElementById('loading');
let errorMessage = document.getElementById('error');
let successMessage = document.getElementById('success');
form.onsubmit = handleFormSubmit;
textarea.oninput = handleTextareaChange;
```

이 방식은 UI 요소나 상호작용을 추가할 때마다 기존 코드를 살펴봐야 한다. 필요한 요소를 보여 주거나 숨기는 처리를 하나라도 놓치면 화면이 잘못된 상태로 남기 때문이다. React의 선언형 UI는 각 상태에서 보여 줄 화면을 기술하도록 해 이 문제를 해결한다.

## 선언형 UI 사고법

선언형 방식으로 컴포넌트를 구현할 때는 다음 다섯 단계를 거친다.

### 1단계: 시각적 상태 식별

- 컴포넌트가 가질 수 있는 모든 시각적 상태를 나열함
  - Empty: 비활성화된 Submit 버튼
  - Typing: 활성화된 Submit 버튼
  - Submitting: 폼이 완전히 비활성화됨, 스피너가 표시됨
  - Success: 폼 대신 "감사합니다" 메시지 표시
  - Error: Typing 상태와 동일하나 추가 에러 메시지 표시

```js
export default function Form({
  // Try 'submitting', 'error', 'success':
  status = 'empty'
}) {
  if (status === 'success') {
    return <h1>That's right!</h1>
  }
  return (
    <>
      <h2>City quiz</h2>
      <p>
        In which city is there a billboard that turns air into drinkable water?
      </p>
      <form>
        <textarea disabled={
          status === 'submitting'
        } />
        <br />
        <button disabled={
          status === 'empty' ||
          status === 'submitting'
        }>
          Submit
        </button>
        {status === 'error' &&
          <p className="Error">
            Good guess but a wrong answer. Try again!
          </p>
        }
      </form>
      </>
  );
}
```

- 모든 시각적 상태를 한 페이지에 표시하는 것도 유용함 ("living styleguide" 또는 "storybook"이라 불림)

```js
import Form from './Form.js';

let statuses = [
  'empty',
  'typing',
  'submitting',
  'success',
  'error',
];

export default function App() {
  return (
    <>
      {statuses.map(status => (
        <section key={status}>
          <h4>Form ({status}):</h4>
          <Form status={status} />
        </section>
      ))}
    </>
  );
}
```

### 2단계: 상태 변경 트리거 결정

상태 갱신을 촉발하는 입력은 두 종류다. 두 경우 모두 상태 변수를 설정해서 UI를 갱신해야 한다.

- 사람 입력: 버튼 클릭, 필드 입력, 링크 이동
- 컴퓨터 입력: 네트워크 응답 도착, 타임아웃 완료, 이미지 로딩

폼의 상태 흐름은 다음과 같다.

- Empty -> (타이핑 시작) -> Typing
- Typing -> (Submit 클릭) -> Submitting
- Submitting -> (네트워크 에러) -> Error
- Submitting -> (네트워크 성공) -> Success

이 흐름을 종이에 원과 화살표로 그려 보면 구현 전에 버그를 걸러낼 수 있다.

### 3단계: 메모리에 상태 표현 (`useState` 사용)

핵심 원칙은 "움직이는 조각"을 최소화하는 것이다. 복잡성이 높을수록 버그가 많아지므로, 반드시 필요한 상태부터 시작한다.

```js
const [answer, setAnswer] = useState('');
const [error, setError] = useState(null);
```

그다음에는 어떤 시각적 상태를 표시할지 결정하는 변수가 필요하다. 확실하지 않으면 우선 가능한 모든 상태를 커버할 만큼 충분한 상태를 추가한다.

```js
const [isEmpty, setIsEmpty] = useState(true);
const [isTyping, setIsTyping] = useState(false);
const [isSubmitting, setIsSubmitting] = useState(false);
const [isSuccess, setIsSuccess] = useState(false);
const [isError, setIsError] = useState(false);
```

### 4단계: 불필요한 상태 변수 제거

앞서 만든 변수 중 `isTyping`과 `isSubmitting`은 동시에 `true`가 되어서는 안 된다. 두 값을 각각 관리하면 이런 모순이 생길 수 있으므로 하나의 `status`로 합친다.

다른 값에서 계산할 수 있는 상태도 제거한다. `isEmpty`는 `answer.length === 0`으로, `isError`는 `error !== null`로 확인할 수 있다. 이렇게 정리하면 7개의 상태 변수 중 직접 보관해야 할 것은 다음 3개만 남는다.

```js
const [answer, setAnswer] = useState('');
const [error, setError] = useState(null);
const [status, setStatus] = useState('typing'); // 'typing', 'submitting', or 'success'
```

이 중 하나라도 제거하면 기능이 깨지므로 세 변수는 모두 핵심적이다. "불가능한 상태"를 더 정밀하게 모델링하려면 리듀서를 사용할 수 있다.

### 5단계: 이벤트 핸들러를 상태 설정에 연결

```js
import { useState } from 'react';

export default function Form() {
  const [answer, setAnswer] = useState('');
  const [error, setError] = useState(null);
  const [status, setStatus] = useState('typing');

  if (status === 'success') {
    return <h1>That's right!</h1>
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setStatus('submitting');
    try {
      await submitForm(answer);
      setStatus('success');
    } catch (err) {
      setStatus('typing');
      setError(err);
    }
  }

  function handleTextareaChange(e) {
    setAnswer(e.target.value);
  }

  return (
    <>
      <h2>City quiz</h2>
      <p>
        In which city is there a billboard that turns air into drinkable water?
      </p>
      <form onSubmit={handleSubmit}>
        <textarea
          value={answer}
          onChange={handleTextareaChange}
          disabled={status === 'submitting'}
        />
        <br />
        <button disabled={
          answer.length === 0 ||
          status === 'submitting'
        }>
          Submit
        </button>
        {error !== null &&
          <p className="Error">
            {error.message}
          </p>
        }
      </form>
    </>
  );
}

function submitForm(answer) {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      let shouldError = answer.toLowerCase() !== 'lima'
      if (shouldError) {
        reject(new Error('Good guess but a wrong answer. Try again!'));
      } else {
        resolve();
      }
    }, 1500);
  });
}
```

명령형 예시보다 코드는 길지만 훨씬 덜 깨진다. 모든 상호작용을 상태 변경으로 표현하므로 기존 상태를 깨뜨리지 않고 새로운 시각적 상태를 추가할 수 있고, 상호작용 로직을 건드리지 않고도 각 상태에서 표시할 내용을 바꿀 수 있다.

# 상태 구조 선택하기 (Choosing the State Structure)

> 원문: https://react.dev/learn/choosing-the-state-structure

## 상태 구조화 원칙

상태를 잘 구조화하면 수정하고 디버깅하기 좋은 컴포넌트가 되지만, 상태 구조가 나쁘면 끊임없이 버그가 생긴다.

### 1. 관련 상태를 그룹화함 (Group related state)

- 항상 두 개 이상의 상태 변수를 동시에 갱신한다면 하나의 상태 변수로 합치는 것을 고려

```js
// 나쁜 예
const [x, setX] = useState(0);
const [y, setY] = useState(0);

// 좋은 예
const [position, setPosition] = useState({ x: 0, y: 0 });
```

- 사용자가 커스텀 필드를 추가할 수 있는 폼 등 얼마나 많은 상태가 필요할지 모를 때 유용함
- 주의: 상태 변수가 객체일 때 한 필드만 갱신하려면 다른 필드를 명시적으로 복사해야 함
  - `setPosition({ x: 100 })`은 `y` 속성을 잃게 됨
  - `setPosition({ ...position, x: 100 })`으로 사용하거나 두 개의 상태 변수로 분리

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
  )
}
```

### 2. 상태의 모순을 피함 (Avoid contradictions in state)

- 여러 상태 조각이 서로 모순될 수 있는 구조는 실수의 여지를 남김

```js
// 나쁜 예: isSending과 isSent가 동시에 true가 될 수 있음
const [isSending, setIsSending] = useState(false);
const [isSent, setIsSent] = useState(false);

// 좋은 예: 하나의 status 변수로 세 가지 유효한 상태를 표현
const [status, setStatus] = useState('typing'); // 'typing', 'sending', 'sent'
const isSending = status === 'sending';
const isSent = status === 'sent';
```

### 3. 불필요한 상태를 피함 (Avoid redundant state)

- 렌더링 중에 컴포넌트의 props나 기존 상태 변수에서 계산할 수 있는 정보는 상태에 넣지 않음

```js
// 나쁜 예: fullName을 상태로 관리
const [firstName, setFirstName] = useState('');
const [lastName, setLastName] = useState('');
const [fullName, setFullName] = useState('');

// 좋은 예: 렌더링 중에 계산
const [firstName, setFirstName] = useState('');
const [lastName, setLastName] = useState('');
const fullName = firstName + ' ' + lastName;
```

- props를 상태에 복사하지 않기

```js
// 나쁜 예: 부모가 다른 messageColor를 전달하면 상태가 갱신되지 않음
function Message({ messageColor }) {
  const [color, setColor] = useState(messageColor);
}

// 좋은 예: prop을 직접 사용
function Message({ messageColor }) {
  const color = messageColor;
}

// 초기값 전용 prop이라면 이름으로 그 의도를 명시
function Message({ initialColor }) {
  const [color, setColor] = useState(initialColor);
}
```

### 4. 상태의 중복을 피함 (Avoid duplication in state)

- 같은 데이터가 여러 상태 변수 간에 또는 중첩 객체 내에 중복되면 동기화가 어려움

```js
// 나쁜 예: selectedItem이 items의 객체를 복제하여 저장
const [items, setItems] = useState(initialItems);
const [selectedItem, setSelectedItem] = useState(items[0]);

// 좋은 예: ID만 저장하고 렌더링 시 계산
const [items, setItems] = useState(initialItems);
const [selectedId, setSelectedId] = useState(0);
const selectedItem = items.find(item => item.id === selectedId);
```

### 5. 깊이 중첩된 상태를 피함 (Avoid deeply nested state)

깊게 계층화된 상태는 갱신하기 불편하므로 중첩 구조 대신 평탄화(정규화)된 구조를 사용한다.

```js
// 나쁜 예: 깊이 중첩된 트리 구조
export const initialTravelPlan = {
  id: 0,
  title: '(Root)',
  childPlaces: [{
    id: 1,
    title: 'Earth',
    childPlaces: [{
      id: 2,
      title: 'Africa',
      childPlaces: [/* ... */]
    }]
  }]
};

// 좋은 예: 데이터베이스 테이블처럼 평탄화
export const initialTravelPlan = {
  0: {
    id: 0,
    title: '(Root)',
    childIds: [1, 42, 46],
  },
  1: {
    id: 1,
    title: 'Earth',
    childIds: [2, 10, 19, 26, 34]
  },
  2: {
    id: 2,
    title: 'Africa',
    childIds: [3, 4, 5, 6, 7, 8, 9]
  },
};
```

평탄화된 상태에서 항목을 제거할 때는 두 수준의 상태만 갱신하면 된다.

- 부모의 `childIds`에서 제거된 ID를 필터링
- 루트 객체에서 갱신된 부모를 포함

```js
function handleComplete(parentId, childId) {
  const parent = plan[parentId];
  const nextParent = {
    ...parent,
    childIds: parent.childIds
      .filter(id => id !== childId)
  };
  setPlan({
    ...plan,
    [parentId]: nextParent
  });
}
```

Immer를 사용하면 더 간결하게 작성할 수 있다.

```js
import { useImmer } from 'use-immer';

function handleComplete(parentId, childId) {
  updatePlan(draft => {
    const parent = draft[parentId];
    parent.childIds = parent.childIds
      .filter(id => id !== childId);

    deleteAllChildren(childId);
    function deleteAllChildren(id) {
      const place = draft[id];
      place.childIds.forEach(deleteAllChildren);
      delete draft[id];
    }
  });
}
```

상태 구조를 정하는 목표는 실수 없이 쉽게 갱신할 수 있게 만드는 것이다. 중복되고 불필요한 데이터를 제거하면 모든 조각이 동기화된 상태를 유지하며, 이는 데이터베이스 정규화와 유사한 원리다.

# 컴포넌트 간 상태 공유 (Sharing State Between Components)

> 원문: https://react.dev/learn/sharing-state-between-components

## 상태 끌어올리기 (Lifting State Up)

두 컴포넌트의 상태가 항상 함께 변해야 한다면 각자 보관하던 상태를 가장 가까운 공통 부모로 옮기고, 부모가 props로 전달하도록 만든다. 이를 상태 끌어올리기라고 하며, React에서 컴포넌트를 조율할 때 자주 사용하는 방법이다.

### 끌어올리기 전: 독립적인 패널

```js
import { useState } from 'react';

function Panel({ title, children }) {
  const [isActive, setIsActive] = useState(false);
  return (
    <section className="panel">
      <h3>{title}</h3>
      {isActive ? (
        <p>{children}</p>
      ) : (
        <button onClick={() => setIsActive(true)}>
          Show
        </button>
      )}
    </section>
  );
}

export default function Accordion() {
  return (
    <>
      <h2>Almaty, Kazakhstan</h2>
      <Panel title="About">
        With a population of about 2 million, Almaty is Kazakhstan's largest city. From 1929 to 1997, it was its capital city.
      </Panel>
      <Panel title="Etymology">
        The name comes from <span lang="kk-KZ">алма</span>, the Kazakh word for "apple" and is often translated as "full of apples". In fact, the region surrounding Almaty is thought to be the ancestral home of the apple, and the wild <i lang="la">Malus sieversii</i> is considered a likely candidate for the ancestor of the modern domestic apple.
      </Panel>
    </>
  );
}
```

각 패널은 독립적이어서 한 패널의 버튼을 눌러도 다른 패널에 영향을 주지 않는다.

### 세 단계로 끌어올리기

1단계에서는 자식 컴포넌트에서 상태를 제거한다. `Panel`의 `isActive`에 대한 제어를 부모 컴포넌트에 넘기고, `isActive`를 `Panel`의 props로 추가한다.

```js
function Panel({ title, children, isActive }) {
```

2단계에서는 공통 부모에서 하드코딩된 데이터를 전달한다. 두 자식 컴포넌트를 모두 조율하려는 가장 가까운 공통 부모를 찾으면, 이 부모가 "진실의 원천(source of truth)"이 된다.

3단계에서는 공통 부모에 상태를 추가한다. 한 번에 하나의 패널만 활성화해야 하므로 `boolean` 대신 활성 패널의 인덱스를 사용한다. 자식에서 부모 상태를 변경하려면 이벤트 핸들러를 prop으로 전달해야 한다.

```js
import { useState } from 'react';

export default function Accordion() {
  const [activeIndex, setActiveIndex] = useState(0);
  return (
    <>
      <h2>Almaty, Kazakhstan</h2>
      <Panel
        title="About"
        isActive={activeIndex === 0}
        onShow={() => setActiveIndex(0)}
      >
        With a population of about 2 million, Almaty is Kazakhstan's largest city. From 1929 to 1997, it was its capital city.
      </Panel>
      <Panel
        title="Etymology"
        isActive={activeIndex === 1}
        onShow={() => setActiveIndex(1)}
      >
        The name comes from <span lang="kk-KZ">алма</span>, the Kazakh word for "apple" and is often translated as "full of apples". In fact, the region surrounding Almaty is thought to be the ancestral home of the apple, and the wild <i lang="la">Malus sieversii</i> is considered a likely candidate for the ancestor of the modern domestic apple.
      </Panel>
    </>
  );
}

function Panel({
  title,
  children,
  isActive,
  onShow
}) {
  return (
    <section className="panel">
      <h3>{title}</h3>
      {isActive ? (
        <p>{children}</p>
      ) : (
        <button onClick={onShow}>
          Show
        </button>
      )}
    </section>
  );
}
```

## 제어 컴포넌트와 비제어 컴포넌트 (Controlled and Uncontrolled)

- 비제어 컴포넌트(uncontrolled): 로컬 상태를 가진 컴포넌트
  - 부모가 동작에 영향을 줄 수 없음
  - 설정이 적어 사용이 쉽지만 함께 조율하기 어려움
- 제어 컴포넌트(controlled): 핵심 정보가 로컬 상태가 아닌 props로 구동되는 컴포넌트
  - 부모가 동작을 완전히 지정함
  - 최대한 유연하지만 부모에서 props로 완전히 구성해야 함

다만 이 둘은 엄격한 기술 용어가 아니며, 각 컴포넌트는 보통 로컬 상태와 props를 섞어서 사용한다. 컴포넌트를 설계할 때는 어떤 정보를 props로 제어하고 어떤 정보를 상태로 제어할지 고려해야 한다. `<input>` 같은 DOM 폼 요소를 `value`와 `defaultValue`로 제어하는 규칙은 [「input」](06_components_and_apis.md#input)을 참고한다.

## 각 상태에 대한 단일 진실의 원천 (Single Source of Truth)

각 상태에는 그 값을 보유하고 갱신할 컴포넌트가 하나씩 있어야 한다. 모든 상태를 한곳에 모으라는 뜻은 아니며, 상태마다 소유자를 정하라는 의미다. 여러 컴포넌트가 같은 값을 사용한다면 각자 복제해 보관하지 않고, 공통 부모가 가진 상태를 필요한 자식에게 전달한다.

# 상태 보존과 초기화 (Preserving and Resetting State)

> 원문: https://react.dev/learn/preserving-and-resetting-state

## 상태는 렌더 트리의 위치에 연결됨

React는 컴포넌트 구조를 렌더 트리로 만들고, 자신이 보관하는 상태를 각 컴포넌트의 트리 위치에 연결한다. 상태가 컴포넌트 함수 자체 안에 저장되는 것은 아니므로, 같은 `<Counter />`를 두 곳에 렌더링해도 각각 독립된 상태를 가진다.

```js
import { useState } from 'react';

export default function App() {
  return (
    <div>
      <Counter />
      <Counter />
    </div>
  );
}

function Counter() {
  const [score, setScore] = useState(0);
  const [hover, setHover] = useState(false);

  let className = 'counter';
  if (hover) {
    className += ' hover';
  }

  return (
    <div
      className={className}
      onPointerEnter={() => setHover(true)}
      onPointerLeave={() => setHover(false)}
    >
      <h1>{score}</h1>
      <button onClick={() => setScore(score + 1)}>
        Add one
      </button>
    </div>
  );
}
```

하나의 카운터를 갱신해도 해당 컴포넌트의 상태만 갱신된다.

### 상태 보존 규칙

React는 같은 위치에서 같은 컴포넌트가 렌더링되는 한 상태를 보존한다. 컴포넌트가 제거되면 상태는 완전히 소멸되고, 다시 추가되면 처음부터 초기화된다(`score = 0`).

### 같은 위치의 같은 컴포넌트는 상태를 보존함

```js
import { useState } from 'react';

export default function App() {
  const [isFancy, setIsFancy] = useState(false);
  return (
    <div>
      {isFancy ? (
        <Counter isFancy={true} />
      ) : (
        <Counter isFancy={false} />
      )}
      <label>
        <input
          type="checkbox"
          checked={isFancy}
          onChange={e => {
            setIsFancy(e.target.checked)
          }}
        />
        Use fancy styling
      </label>
    </div>
  );
}
```

체크박스를 바꿔도 `Counter`의 상태는 초기화되지 않는다. `isFancy`가 `true`든 `false`든 `App`이 반환하는 `div`의 첫 번째 자식은 같은 `Counter`이기 때문이다. React는 JSX가 작성된 위치가 아니라 UI 트리에서의 위치를 기준으로 같은 카운터인지 판단한다.

### 같은 위치의 다른 컴포넌트는 상태를 초기화함

```js
import { useState } from 'react';

export default function App() {
  const [isPaused, setIsPaused] = useState(false);
  return (
    <div>
      {isPaused ? (
        <p>See you later!</p>
      ) : (
        <Counter />
      )}
      <label>
        <input
          type="checkbox"
          checked={isPaused}
          onChange={e => {
            setIsPaused(e.target.checked)
          }}
        />
        Take a break
      </label>
    </div>
  );
}
```

같은 위치에서 다른 컴포넌트 타입으로 전환하면 React는 UI 트리에서 기존 컴포넌트를 제거하고 그 상태를 소멸시킨다. 이때 전체 하위 트리의 상태가 함께 초기화된다. 예를 들어 `<div>` 안의 `<section>`이 `<div>`로 바뀌면 그 안의 `Counter`가 같더라도 상태가 초기화된다.

### 규칙: 리렌더링 간에 상태를 보존하려면 트리 구조가 "일치"해야 함

React는 트리에서 컴포넌트를 제거할 때 상태를 소멸시키므로, 리렌더링 사이에 트리 구조가 달라지면 상태가 사라진다.

### 주의: 컴포넌트 함수를 중첩 정의하지 말 것

```js
import { useState } from 'react';

export default function MyComponent() {
  const [counter, setCounter] = useState(0);

  function MyTextField() {
    const [text, setText] = useState('');
    return (
      <input
        value={text}
        onChange={e => setText(e.target.value)}
      />
    );
  }

  return (
    <>
      <MyTextField />
      <button onClick={() => {
        setCounter(counter + 1)
      }}>Clicked {counter} times</button>
    </>
  );
}
```

버튼을 클릭하면 `MyComponent`가 다시 실행되면서 새 `MyTextField` 함수가 생성된다. React는 이를 같은 위치에 다른 컴포넌트를 렌더링한 것으로 판단하므로, 그 아래 상태를 모두 초기화해 입력 내용이 사라진다. 이를 막으려면 컴포넌트 함수를 최상위에 선언해야 한다.

## 같은 위치에서 상태 초기화하기

기본적으로 React는 같은 위치에 유지되는 컴포넌트의 상태를 보존한다. 대개는 이것이 기대하는 동작이지만, 때로는 상태를 초기화하고 싶을 때가 있다.

### 방법 1: 다른 위치에 컴포넌트 렌더링

```js
import { useState } from 'react';

export default function Scoreboard() {
  const [isPlayerA, setIsPlayerA] = useState(true);
  return (
    <div>
      {isPlayerA &&
        <Counter person="Taylor" />
      }
      {!isPlayerA &&
        <Counter person="Sarah" />
      }
      <button onClick={() => {
        setIsPlayerA(!isPlayerA);
      }}>
        Next player!
      </button>
    </div>
  );
}
```

이 방법은 같은 장소에 렌더링하는 독립 컴포넌트가 적을 때 편리하다.

### 방법 2: key로 상태 초기화 (더 범용적)

`key`는 리스트에서만 사용하는 것이 아니며, React가 컴포넌트를 구분하도록 하는 데에도 쓸 수 있다. 기본적으로 React는 부모 안에서의 순서("첫 번째 카운터", "두 번째 카운터")로 컴포넌트를 구분하는데, `key`를 사용하면 이것이 특정 카운터라는 사실을 React에 알릴 수 있다.

```js
import { useState } from 'react';

export default function Scoreboard() {
  const [isPlayerA, setIsPlayerA] = useState(true);
  return (
    <div>
      {isPlayerA ? (
        <Counter key="Taylor" person="Taylor" />
      ) : (
        <Counter key="Sarah" person="Sarah" />
      )}
      <button onClick={() => {
        setIsPlayerA(!isPlayerA);
      }}>
        Next player!
      </button>
    </div>
  );
}
```

`key`를 지정하면 React는 순서 대신 `key` 자체를 위치의 일부로 사용한다. 그래서 JSX에서 같은 자리에 렌더링하더라도 React는 두 카운터를 서로 다른 카운터로 인식하고, 카운터가 화면에 나타날 때마다 상태를 생성하며 제거될 때마다 상태를 소멸시킨다. key는 부모 안에서의 위치만 지정하므로 전역적으로 고유할 필요는 없다. 리스트에서 key를 정하는 규칙은 [「key로 리스트 항목 순서 유지하기」](01_describing_the_ui.md#key로-리스트-항목-순서-유지하기)를 참고한다.

### key로 폼 초기화

key로 상태를 초기화하는 방법은 채팅 앱에서 수신자를 변경할 때 입력 상태를 초기화하는 데 유용하다.

```js
// key 없이: 수신자를 변경해도 입력 상태가 유지됨 (버그)
<Chat contact={to} />

// key 사용: 수신자를 변경하면 Chat 컴포넌트가 처음부터 다시 생성됨
<Chat key={to.id} contact={to} />
```

데이터와 무관하게 폼을 비우는 리셋 버튼이 필요하다면 key로 쓸 카운터를 state로 두고, 버튼을 누를 때마다 값을 올리면 된다. key가 바뀌므로 `Form`과 그 하위 상태가 모두 처음부터 다시 만들어진다.

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

### 제거된 컴포넌트의 상태 보존 방법

- 모든 채팅을 렌더링하되 CSS로 나머지를 숨기기: 간단한 UI에 적합
  - 숨겨진 트리가 크고 DOM 노드가 많으면 느려질 수 있음
  - `<Activity mode="hidden">`으로 감싸면 DOM과 상태를 보존한 채 숨길 수 있음. 자세한 내용은 [「Activity」](06_components_and_apis.md#activity)를 참고한다
- 상태를 부모로 끌어올리기: 가장 일반적인 해결책
  - 자식이 제거되어도 부모가 중요한 정보를 유지
- React 상태 외의 소스 사용: 예를 들어 `localStorage`에서 메시지 초안을 읽고 저장

# 상태 로직을 리듀서로 추출하기 (Extracting State Logic into a Reducer)

> 원문: https://react.dev/learn/extracting-state-logic-into-a-reducer

## 리듀서로 상태 로직 통합

상태 갱신 로직이 여러 이벤트 핸들러에 흩어지면 어떤 동작이 상태를 어떻게 바꾸는지 파악하기 어려워진다. 이때 컴포넌트 밖의 함수인 "리듀서(reducer)"로 갱신 로직을 모을 수 있다.

### 1단계: 상태 설정에서 액션 디스패치로 전환

이벤트 핸들러에서 "무엇을 할지" 직접 지정하는 대신, "사용자가 무엇을 했는지"를 기술하는 "액션"을 디스패치한다.

```js
// 기존 (setState 직접 사용)
function handleAddTask(text) {
  setTasks([
    ...tasks,
    {
      id: nextId++,
      text: text,
      done: false,
    },
  ]);
}

function handleChangeTask(task) {
  setTasks(
    tasks.map((t) => {
      if (t.id === task.id) {
        return task;
      } else {
        return t;
      }
    })
  );
}

function handleDeleteTask(taskId) {
  setTasks(tasks.filter((t) => t.id !== taskId));
}
```

```js
// 개선 (dispatch 사용)
function handleAddTask(text) {
  dispatch({
    type: 'added',
    id: nextId++,
    text: text,
  });
}

function handleChangeTask(task) {
  dispatch({
    type: 'changed',
    task: task,
  });
}

function handleDeleteTask(taskId) {
  dispatch({
    type: 'deleted',
    id: taskId,
  });
}
```

액션 객체는 일반 JavaScript 객체이며 어떤 형태든 될 수 있다. 관례상 문자열 `type`으로 무슨 일이 일어났는지 기술하고, 추가 정보는 다른 필드에 담아 전달한다.

### 2단계: 리듀서 함수 작성

리듀서 함수는 현재 상태(state)와 액션 객체(action)라는 두 인수를 받아 다음 상태를 반환한다.

```js
function tasksReducer(tasks, action) {
  switch (action.type) {
    case 'added': {
      return [
        ...tasks,
        {
          id: action.id,
          text: action.text,
          done: false,
        },
      ];
    }
    case 'changed': {
      return tasks.map((t) => {
        if (t.id === action.task.id) {
          return action.task;
        } else {
          return t;
        }
      });
    }
    case 'deleted': {
      return tasks.filter((t) => t.id !== action.id);
    }
    default: {
      throw Error('Unknown action: ' + action.type);
    }
  }
}
```

리듀서 함수는 상태를 인수로 받으므로 컴포넌트 외부에 선언할 수 있고, 그만큼 들여쓰기 수준이 줄어 가독성이 높아진다. 관례상 switch 문을 사용하며, 각 `case` 블록을 중괄호 `{ }`로 감싸 변수 충돌을 막고 `return`으로 끝낸다. if/else를 써도 완전히 유효하다.

### 리듀서라는 이름의 유래

리듀서라는 이름은 배열의 `reduce()` 연산에서 왔다.

```js
const arr = [1, 2, 3, 4, 5];
const sum = arr.reduce(
  (result, number) => result + number
); // 1 + 2 + 3 + 4 + 5
```

`reduce`에 전달하는 함수가 "리듀서"이며, "지금까지의 결과"와 "현재 항목"을 받아 "다음 결과"를 반환한다. React 리듀서도 같은 아이디어로, "지금까지의 상태"와 "액션"을 받아 "다음 상태"를 반환한다.

### 3단계: 컴포넌트에서 리듀서 사용

```js
import { useReducer } from 'react';
```

그리고 `useState`를 `useReducer`로 교체한다.

```js
// 기존
const [tasks, setTasks] = useState(initialTasks);

// 개선
const [tasks, dispatch] = useReducer(tasksReducer, initialTasks);
```

`useReducer`는 리듀서 함수와 초기 상태를 받아 상태 값과 디스패치 함수를 반환한다. 세 번째 인수 `init`을 포함한 전체 명세는 [「useReducer」](05_hooks_reference.md#usereducer)를 참고한다.

리듀서는 별도 파일로 분리할 수도 있다. 이벤트 핸들러는 "무엇이 일어났는지"만 지정하고 리듀서 함수가 "어떻게 상태가 갱신되는지" 결정하므로 컴포넌트 로직을 읽기 쉬워진다.

## useState와 useReducer 비교

- 코드 크기: `useState`는 초기 코드가 적음
  - `useReducer`는 리듀서 함수와 디스패치 양쪽을 작성해야 하지만, 많은 이벤트 핸들러가 비슷한 방식으로 상태를 수정하면 코드를 줄이는 데 도움됨
- 가독성: `useState`는 간단한 갱신에 매우 쉬움
  - 복잡한 갱신에서는 코드가 비대해질 수 있음
  - `useReducer`는 "어떻게"(갱신 로직)와 "무엇이 일어났는지"(이벤트 핸들러)를 깔끔하게 분리함
- 디버깅: `useState`에서는 상태가 어디서 잘못 설정되었는지, 왜 그런지 파악이 어려움
  - `useReducer`에서는 리듀서에 console.log를 추가하여 모든 상태 갱신과 어떤 액션이 원인인지 볼 수 있음
- 테스트: 리듀서는 순수 함수이므로 독립적으로 export하여 테스트 가능
- 개인 선호: 둘 다 동등하고 서로 교환 가능함
- 권장: 잘못된 상태 갱신으로 인한 버그가 자주 발생하고 코드에 더 많은 구조가 필요할 때 리듀서 사용
  - 한 컴포넌트에서 `useState`와 `useReducer`를 혼합 사용 가능

## 리듀서 작성 모범 사례

### 1. 리듀서는 순수해야 함

리듀서는 상태 갱신자 함수처럼 렌더링 중에 실행되므로, 같은 입력에 항상 같은 출력을 반환해야 한다. 요청 전송이나 타임아웃 예약 같은 부수 효과를 수행해서는 안 되며, 객체와 배열도 기존 값을 변이(mutation)하지 않고 갱신해야 한다.

### 2. 각 액션은 단일 사용자 상호작용을 기술함

액션이 데이터를 여러 곳 바꾸더라도, 각 액션은 사용자가 수행한 동작 하나를 기술해야 한다. 예를 들어 5개 필드가 있는 폼을 "리셋"할 때는 5개의 개별 `set_field` 액션이 아니라 하나의 `reset_form` 액션을 디스패치한다. 리듀서의 모든 액션을 로깅했을 때 어떤 상호작용이나 응답이 어떤 순서로 발생했는지 재구성할 수 있어야 한다.

## Immer로 간결한 리듀서 작성

`useImmerReducer`를 사용하면 `push`나 `arr[i] =` 할당으로 상태를 변이하듯 작성할 수 있다.

```js
import { useImmerReducer } from 'use-immer';

function tasksReducer(draft, action) {
  switch (action.type) {
    case 'added': {
      draft.push({
        id: action.id,
        text: action.text,
        done: false,
      });
      break;
    }
    case 'changed': {
      const index = draft.findIndex((t) => t.id === action.task.id);
      draft[index] = action.task;
      break;
    }
    case 'deleted': {
      return draft.filter((t) => t.id !== action.id);
    }
    default: {
      throw Error('Unknown action: ' + action.type);
    }
  }
}
```

Immer가 제공하는 특별한 `draft` 객체는 안전하게 변이할 수 있다. 내부적으로 Immer가 `draft`에 대한 변경 사항을 적용한 상태의 복사본을 생성하기 때문이다.

# Context로 데이터 깊게 전달하기 (Passing Data Deeply with Context)

> 원문: https://react.dev/learn/passing-data-deeply-with-context

## Props 전달의 문제점

props 전달은 얕은 컴포넌트 트리에서는 직관적이지만, 다음과 같은 경우에는 문제가 된다.

- props를 많은 중간 계층을 통해 전달해야 하는 경우
- 많은 컴포넌트가 같은 prop을 필요로 하는 경우
- 가장 가까운 공통 조상이 데이터가 필요한 컴포넌트와 먼 경우

이런 상황을 "프롭 드릴링(prop drilling)"이라 한다.

## Context: Props 전달의 대안

Context를 사용하면 부모 컴포넌트가 명시적으로 prop을 전달하지 않고도 아래 트리의 모든 컴포넌트에 데이터를 제공할 수 있다.

전달하는 값은 정적 값에 한정되지 않는다. 다음 렌더링에서 다른 값을 제공하면 React가 아래에서 그 값을 읽는 모든 컴포넌트를 갱신하므로, Context를 state와 결합해 사용할 수 있다.

### Context 사용 3단계

1단계에서는 Context를 생성한다.

```js
import { createContext } from 'react';

export const LevelContext = createContext(0);
```

`createContext()`의 인수는 기본값이며, 어떤 provider도 감싸지 않았을 때 사용된다.

2단계에서는 `useContext`로 Context를 사용한다.

```js
import { useContext } from 'react';
import { LevelContext } from './LevelContext.js';

export default function Heading({ children }) {
  const level = useContext(LevelContext);
  switch (level) {
    case 0:
      throw Error('Heading must be inside a Section!');
    case 1:
      return <h1>{children}</h1>;
    case 2:
      return <h2>{children}</h2>;
    case 3:
      return <h3>{children}</h3>;
    case 4:
      return <h4>{children}</h4>;
    case 5:
      return <h5>{children}</h5>;
    case 6:
      return <h6>{children}</h6>;
    default:
      throw Error('Unknown level: ' + level);
  }
}
```

`useContext`는 Hook이므로 컴포넌트 최상위에서만 호출할 수 있고, 루프나 조건문 안에서는 호출할 수 없다.

3단계에서는 Provider로 Context를 제공한다.

```js
import { useContext } from 'react';
import { LevelContext } from './LevelContext.js';

export default function Section({ children }) {
  const level = useContext(LevelContext);
  return (
    <section className="section">
      <LevelContext value={level + 1}>
        {children}
      </LevelContext>
    </section>
  );
}
```

이 코드는 "이 `<Section>` 안에서 `LevelContext`를 요청하는 모든 컴포넌트에 이 `level` 값을 제공하라"고 React에 알린다.

### 같은 컴포넌트에서 Context 사용과 제공을 동시에 할 수 있음

Section은 부모 Section에서 level을 읽고 `level + 1`을 자식에게 제공한다. 이렇게 하면 레벨이 자동으로 증가한다.

```js
import Heading from './Heading.js';
import Section from './Section.js';

export default function Page() {
  return (
    <Section>
      <Heading>Title</Heading>
      <Section>
        <Heading>Heading</Heading>
        <Heading>Heading</Heading>
        <Heading>Heading</Heading>
        <Section>
          <Heading>Sub-heading</Heading>
          <Heading>Sub-heading</Heading>
          <Heading>Sub-heading</Heading>
          <Section>
            <Heading>Sub-sub-heading</Heading>
            <Heading>Sub-sub-heading</Heading>
            <Heading>Sub-sub-heading</Heading>
          </Section>
        </Section>
      </Section>
    </Section>
  );
}
```

## Context는 중간 컴포넌트를 관통함

Context는 provider와 consumer 사이에 있는 모든 컴포넌트를 관통한다.

- `<div>` 같은 빌트인 컴포넌트
- context를 사용하지 않는 커스텀 컴포넌트

그래서 Context를 사용하면 컴포넌트가 "주변 환경에 적응"하도록 작성할 수 있다. 이는 CSS 속성 상속과 비슷하다. CSS에서 `<div>`에 `color: blue`를 설정하면 중간에서 재정의하지 않는 한 모든 중첩 요소가 그 값을 상속한다. React에서 위에서 오는 context를 재정의하는 유일한 방법은 다른 값을 가진 provider로 감싸는 것이다. 서로 다른 React context는 완전히 독립적이므로, 하나의 컴포넌트가 여러 context를 사용하거나 제공해도 문제없다.

## Context 사용 전 고려할 것

props가 여러 수준을 통과해야 한다고 해서 곧바로 context를 사용해야 하는 것은 아니다.

### 먼저 고려할 대안

- 1\. props 전달로 시작: 12개의 props를 12개의 컴포넌트를 통해 전달하더라도 괜찮음
  - 데이터 흐름이 명시적이어서 유지보수자가 어떤 데이터가 어디로 흐르는지 이해할 수 있음
- 2\. 컴포넌트를 추출하고 JSX를 `children`으로 전달: 데이터를 사용하지 않는 중간 컴포넌트를 거치는 대신, 예를 들어 `<Layout posts={posts} />` 대신 `<Layout><Posts posts={posts} /></Layout>`으로 작성
  - 데이터 제공자와 소비자 사이의 계층이 줄어듦

### Context 활용 사례

- 테마: 앱의 외관 변경 (예: 다크 모드). 최상위에 context provider를 두고 시각적 조정이 필요한 컴포넌트에서 사용
- 현재 계정: 현재 로그인한 사용자 정보
  - 트리 어디서나 읽기 편리
  - 동시 다중 계정 지원 시 중첩 provider로 UI 일부를 다른 계정 값으로 감쌀 수 있음
- 라우팅: 대부분의 라우팅 솔루션이 내부적으로 context를 사용하여 현재 라우트를 보유
- 복잡한 상태 관리: 앱이 커지면 리듀서와 context를 결합하여 복잡한 상태를 관리하고 과도한 프롭 드릴링 없이 먼 컴포넌트에 전달

# Reducer와 Context로 확장하기 (Scaling Up with Reducer and Context)

> 원문: https://react.dev/learn/scaling-up-with-reducer-and-context

## Reducer와 Context의 결합

리듀서로 상태 갱신 로직을 모으고 Context로 그 상태와 갱신 함수를 깊은 하위 컴포넌트에 전달하면, 복잡한 화면에서도 상태를 관리하고 공유할 수 있다.

### 문제: props 전달의 한계

리듀서를 사용해도 `tasks`와 `dispatch`는 이를 선언한 `TaskApp`에서만 바로 사용할 수 있다. 다른 컴포넌트가 상태를 읽거나 바꾸려면 현재 상태와 이벤트 핸들러를 props로 전달해야 하므로, 중간에 수십, 수백 개의 컴포넌트가 있으면 전달 코드가 번거로워진다. 다음 예시는 상태와 dispatch를 Context에 담아 이 과정을 줄인다.

### 해결: Context에 상태와 dispatch 넣기

1단계에서는 Context를 생성한다.

```js
import { createContext } from 'react';

export const TasksContext = createContext(null);
export const TasksDispatchContext = createContext(null);
```

- `TasksContext`: 현재 작업 목록 제공
- `TasksDispatchContext`: 컴포넌트가 액션을 디스패치할 수 있는 함수 제공

2단계에서는 상태와 dispatch를 Context에 넣는다.

```js
import { TasksContext, TasksDispatchContext } from './TasksContext.js';

export default function TaskApp() {
  const [tasks, dispatch] = useReducer(tasksReducer, initialTasks);

  return (
    <TasksContext value={tasks}>
      <TasksDispatchContext value={dispatch}>
        <h1>Day off in Kyoto</h1>
        <AddTask />
        <TaskList />
      </TasksDispatchContext>
    </TasksContext>
  );
}
```

3단계에서는 트리 어디서든 Context를 사용한다. 작업 목록이 필요한 컴포넌트는 `TasksContext`에서 읽고, 작업 목록을 갱신하려면 `TasksDispatchContext`에서 `dispatch`를 읽어 호출한다.

```js
// AddTask.js
import { useState, useContext } from 'react';
import { TasksDispatchContext } from './TasksContext.js';

export default function AddTask() {
  const [text, setText] = useState('');
  const dispatch = useContext(TasksDispatchContext);
  return (
    <>
      <input
        placeholder="Add task"
        value={text}
        onChange={e => setText(e.target.value)}
      />
      <button onClick={() => {
        setText('');
        dispatch({
          type: 'added',
          id: nextId++,
          text: text,
        });
      }}>Add</button>
    </>
  );
}

let nextId = 3;
```

```js
// TaskList.js
import { useState, useContext } from 'react';
import { TasksContext, TasksDispatchContext } from './TasksContext.js';

export default function TaskList() {
  const tasks = useContext(TasksContext);
  return (
    <ul>
      {tasks.map(task => (
        <li key={task.id}>
          <Task task={task} />
        </li>
      ))}
    </ul>
  );
}

function Task({ task }) {
  const [isEditing, setIsEditing] = useState(false);
  const dispatch = useContext(TasksDispatchContext);
  let taskContent;
  if (isEditing) {
    taskContent = (
      <>
        <input
          value={task.text}
          onChange={e => {
            dispatch({
              type: 'changed',
              task: {
                ...task,
                text: e.target.value
              }
            });
          }} />
        <button onClick={() => setIsEditing(false)}>
          Save
        </button>
      </>
    );
  } else {
    taskContent = (
      <>
        {task.text}
        <button onClick={() => setIsEditing(true)}>
          Edit
        </button>
      </>
    );
  }
  return (
    <label>
      <input
        type="checkbox"
        checked={task.done}
        onChange={e => {
          dispatch({
            type: 'changed',
            task: {
              ...task,
              done: e.target.checked
            }
          });
        }}
      />
      {taskContent}
      <button onClick={() => {
        dispatch({
          type: 'deleted',
          id: task.id
        });
      }}>
        Delete
      </button>
    </label>
  );
}
```

`TaskApp`은 이벤트 핸들러를 아래로 전달하지 않고, `TaskList`도 `Task` 컴포넌트에 이벤트 핸들러를 전달하지 않는다. 각 컴포넌트가 필요한 context를 직접 읽는다.

## 모든 배선을 하나의 파일로 이동

리듀서와 context를 하나의 파일에 모으면 컴포넌트를 더 깔끔하게 정리할 수 있다. 다음 `TasksProvider` 컴포넌트는 모든 조각을 결합한다.

- 1\. 리듀서로 상태 관리
- 2\. 두 context를 아래 컴포넌트에 제공
- 3\. `children`을 prop으로 받아 JSX를 전달받음

```js
// TasksContext.js
import { createContext, useContext, useReducer } from 'react';

const TasksContext = createContext(null);
const TasksDispatchContext = createContext(null);

export function TasksProvider({ children }) {
  const [tasks, dispatch] = useReducer(
    tasksReducer,
    initialTasks
  );

  return (
    <TasksContext value={tasks}>
      <TasksDispatchContext value={dispatch}>
        {children}
      </TasksDispatchContext>
    </TasksContext>
  );
}

export function useTasks() {
  return useContext(TasksContext);
}

export function useTasksDispatch() {
  return useContext(TasksDispatchContext);
}

function tasksReducer(tasks, action) {
  switch (action.type) {
    case 'added': {
      return [...tasks, {
        id: action.id,
        text: action.text,
        done: false
      }];
    }
    case 'changed': {
      return tasks.map(t => {
        if (t.id === action.task.id) {
          return action.task;
        } else {
          return t;
        }
      });
    }
    case 'deleted': {
      return tasks.filter(t => t.id !== action.id);
    }
    default: {
      throw Error('Unknown action: ' + action.type);
    }
  }
}

const initialTasks = [
  { id: 0, text: 'Philosopher\'s Path', done: true },
  { id: 1, text: 'Visit the temple', done: false },
  { id: 2, text: 'Drink matcha', done: false }
];
```

그러면 `TaskApp` 컴포넌트가 깔끔해진다.

```js
// App.js
import AddTask from './AddTask.js';
import TaskList from './TaskList.js';
import { TasksProvider } from './TasksContext.js';

export default function TaskApp() {
  return (
    <TasksProvider>
      <h1>Day off in Kyoto</h1>
      <AddTask />
      <TaskList />
    </TasksProvider>
  );
}
```

`TasksContext.js`는 context를 읽는 `useTasks`와 `useTasksDispatch` 함수도 export하므로, 컴포넌트에서는 다음처럼 사용한다.

```js
const tasks = useTasks();
const dispatch = useTasksDispatch();
```

`useTasks`와 `useTasksDispatch` 같은 함수를 커스텀 Hook이라 한다. 함수 이름이 `use`로 시작하면 커스텀 Hook으로 간주되며, 내부에서 `useContext` 같은 다른 Hook을 사용할 수 있다.

앱이 커지면 이런 context-reducer 쌍이 많아질 수 있다. 이 방식은 트리 깊은 곳에서 데이터에 접근해야 할 때마다 큰 작업 없이 앱을 확장하고 상태를 끌어올릴 수 있는 강력한 방법이다.

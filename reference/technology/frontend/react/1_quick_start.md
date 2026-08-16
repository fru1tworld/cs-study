# React 빠른 시작 (Quick Start)

> 원문: https://react.dev/learn

- React 앱은 컴포넌트로 이루어짐
- 컴포넌트는 자체 로직과 외형을 가진 UI의 한 조각으로, 버튼처럼 작을 수도 있고 페이지 전체처럼 클 수도 있음
- React 컴포넌트는 마크업을 반환하는 자바스크립트 함수임

### 이 장에서 다루는 내용

- 컴포넌트를 만들고 중첩하는 방법
- JSX로 마크업과 자바스크립트를 함께 작성하는 방법
- 중괄호로 JSX에서 자바스크립트 기능을 사용하는 방법
- props로 컴포넌트를 구성하는 방법
- 조건부로 컴포넌트를 렌더링하는 방법
- 여러 컴포넌트를 리스트로 렌더링하는 방법
- 이벤트에 응답하고 화면을 업데이트하는 방법
- 컴포넌트 간에 데이터를 공유하는 방법

---

## 컴포넌트 만들고 중첩하기

- React 앱은 컴포넌트로 구성됨. 컴포넌트는 마크업을 반환하는 자바스크립트 함수임

```js
function MyButton() {
  return (
    <button>I'm a button</button>
  );
}
```

- `MyButton`을 선언한 뒤 다른 컴포넌트 안에 중첩할 수 있음

```js
export default function MyApp() {
  return (
    <div>
      <h1>Welcome to my app</h1>
      <MyButton />
    </div>
  );
}
```

- `<MyButton />`은 대문자로 시작함. React 컴포넌트는 항상 대문자로 시작해야 하고, HTML 태그는 소문자여야 함
- `export default`는 파일에서 주된 컴포넌트가 무엇인지 표준 자바스크립트 문법으로 지정함

## JSX로 마크업 작성하기

- 위에서 본 마크업 문법을 JSX라고 함. 선택 사항이지만 대부분의 React 프로젝트는 편의성 때문에 JSX를 사용함
- JSX는 HTML보다 더 엄격함. `<br />`처럼 태그를 반드시 닫아야 함
- 컴포넌트는 여러 개의 JSX 태그를 반환할 수 없으므로, 공통 부모로 감싸야 함. 이미 `<div>`가 있다면 JSX는 빈 태그 `<>`와 `</>`로도 감쌀 수 있음

```js
function AboutPage() {
  return (
    <>
      <h1>About</h1>
      <p>Hello there.<br />How do you do?</p>
    </>
  );
}
```

- 변환할 HTML이 많다면 온라인 [HTML-to-JSX 변환기](https://transform.tools/html-to-jsx)를 사용할 수 있음

## 스타일 추가하기

- React에서는 `className`으로 CSS 클래스를 지정함. HTML의 `class` 속성과 동일하게 동작함

```js
<img className="avatar" />
```

- CSS 규칙은 별도의 CSS 파일에 작성함

```css
.avatar {
  border-radius: 50%;
}
```

- React는 CSS 파일을 추가하는 방법을 규정하지 않음. 가장 단순한 경우 HTML에 [`<link>` 태그](https://developer.mozilla.org/en-US/docs/Web/HTML/Element/link)를 추가함. 빌드 도구나 프레임워크를 사용한다면 해당 문서에서 프로젝트에 CSS를 추가하는 방법을 확인해야 함

## 데이터 표시하기

- JSX에서는 중괄호를 사용해 자바스크립트로 돌아가서 코드의 변수를 삽입하고 사용자에게 보여줄 수 있음

```js
return (
  <h1>{user.name}</h1>
);
```

- JSX 속성값에도 중괄호를 사용해 자바스크립트로 "탈출"할 수 있음. 아래에서는 `src={user.imageUrl}`이 `user.imageUrl` 변수 값을 읽어 `src` 값으로 전달함

```js
return (
  <img
    className="avatar"
    src={user.imageUrl}
  />
);
```

- JSX 중괄호 안에는 문자열 연결 같은 더 복잡한 표현식도 넣을 수 있음

```js
const user = {
  name: 'Hedy Lamarr',
  imageUrl: 'https://i.imgur.com/yXOvdOSs.jpg',
  imageSize: 90,
};

export default function Profile() {
  return (
    <>
      <h1>{user.name}</h1>
      <img
        className="avatar"
        src={user.imageUrl}
        alt={'Photo of ' + user.name}
        style={{
          width: user.imageSize,
          height: user.imageSize
        }}
      />
    </>
  );
}
```

- 위 예시에서 `style={{}}`는 특별한 문법이 아니라 `style={ }` JSX 중괄호 안의 평범한 `{}` 객체임. `style`은 자바스크립트 변수에 의존할 때 사용할 수 있음

## 조건부 렌더링

- React에는 조건을 작성하기 위한 특별한 문법이 없음. 대신 일반 자바스크립트 코드를 작성할 때 사용하는 기법을 그대로 씀
- 예를 들어 `if` 문으로 JSX를 조건부로 포함할 수 있음

```js
let content;
if (isLoggedIn) {
  content = <AdminPanel />;
} else {
  content = <LoginForm />;
}
return (
  <div>
    {content}
  </div>
);
```

- 더 간결한 코드를 선호한다면 [조건부 `?` 연산자](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Conditional_Operator)를 사용할 수 있음. `if`와 달리 JSX 안에서 동작함

```js
<div>
  {isLoggedIn ? (
    <AdminPanel />
  ) : (
    <LoginForm />
  )}
</div>
```

- `else` 분기가 필요 없다면 더 짧은 [논리 `&&` 문법](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Logical_AND)도 사용할 수 있음

```js
<div>
  {isLoggedIn && <AdminPanel />}
</div>
```

- 이런 접근 방식은 속성을 조건부로 지정할 때도 동작함

## 리스트 렌더링하기

- 컴포넌트 리스트를 렌더링할 때는 자바스크립트의 `for` 문이나 [배열의 `map()` 함수](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/map)에 의존함
- 예를 들어 다음과 같은 상품 배열이 있다면

```js
const products = [
  { title: 'Cabbage', id: 1 },
  { title: 'Garlic', id: 2 },
  { title: 'Apple', id: 3 },
];
```

- 컴포넌트 안에서 `map()`으로 상품 배열을 `<li>` 항목 배열로 변환할 수 있음

```js
const listItems = products.map(product =>
  <li key={product.id}>
    {product.title}
  </li>
);

return (
  <ul>{listItems}</ul>
);
```

- `<li>`에 `key` 속성이 있는 점에 유의해야 함. 리스트의 각 항목에는 형제 사이에서 고유하게 식별되는 문자열이나 숫자를 `key`로 전달해야 함. 일반적으로 데이터베이스 ID처럼 데이터 자체에서 나온 값을 사용함. `key`는 나중에 항목을 삽입, 삭제, 재정렬하더라도 React가 어떤 일이 일어났는지 알 수 있게 해줌

## 이벤트에 응답하기

- 컴포넌트 안에 이벤트 핸들러 함수를 선언하여 이벤트에 응답할 수 있음

```js
function MyButton() {
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

- `onClick={handleClick}` 끝에 괄호가 없는 점에 유의해야 함. 이벤트 핸들러 함수를 호출하지 않고 전달하기만 하면 됨. 사용자가 버튼을 클릭할 때 React가 이 함수를 호출함

## 화면 업데이트하기

- 컴포넌트가 특정 정보를 "기억"하고 표시하기를 원할 때가 많음. 예를 들어 버튼이 클릭된 횟수를 세고 싶을 수 있음. 이를 위해서는 컴포넌트에 state를 추가해야 함
- 먼저 React에서 [`useState`](https://react.dev/reference/react/useState)를 import함

```js
import { useState } from 'react';
```

- 이제 컴포넌트 안에서 state 변수를 선언할 수 있음

```js
function MyButton() {
  const [count, setCount] = useState(0);
  // ...
```

- `useState`로부터 두 가지를 얻음. 현재 state인 `count`와, 이를 업데이트할 수 있는 함수(`setCount`)임. 이름은 무엇이든 지을 수 있지만 관례상 `[something, setSomething]`처럼 씀
- 버튼이 처음 표시될 때 `count`는 `useState()`에 전달한 초깃값인 `0`이 됨. state를 변경하고 싶다면 `setCount()`를 호출하고 새 값을 전달함. 이 버튼을 클릭하면 카운터가 증가함

```js
function MyButton() {
  const [count, setCount] = useState(0);

  function handleClick() {
    setCount(count + 1);
  }

  return (
    <button onClick={handleClick}>
      Clicked {count} times
    </button>
  );
}
```

- React는 컴포넌트 함수를 다시 호출함. 이번에는 `count`가 `1`이고, 그다음엔 `2`가 되는 식으로 진행됨
- 같은 컴포넌트를 여러 번 렌더링하면 각각 고유하고 독립적인 state를 갖게 됨. `MyButton`을 두 번 렌더링해보면 각 버튼이 독립적으로 자신만의 `count` state를 가지며 다른 버튼에 영향을 주지 않는 것을 확인할 수 있음

## Hook 사용하기

- `use`로 시작하는 함수를 Hook이라고 함. `useState`는 React가 제공하는 내장 Hook임. [`react` API 레퍼런스](https://react.dev/reference/react)에서 다른 내장 Hook을 찾을 수 있고, 기존 Hook을 조합해 자신만의 Hook을 작성할 수도 있음
- Hook은 일반 함수보다 더 제한적임. 컴포넌트(또는 다른 Hook)의 최상단에서만 Hook을 호출할 수 있음. 조건문이나 반복문에서 `useState`를 사용하고 싶다면 새 컴포넌트를 추출해서 그 안에 넣어야 함

## 컴포넌트 간 데이터 공유하기

- 앞의 예시에서 각 `MyButton`은 독립적인 `count` state를 가지고, 각 버튼을 클릭하면 클릭된 버튼의 `count`만 변경됨

```js
function MyButton() {
  const [count, setCount] = useState(0);
  // ...
}
```

- 하지만 데이터를 공유하고 항상 함께 업데이트되게 만들어야 하는 경우가 자주 있음
- 두 `MyButton` 컴포넌트가 같은 `count`를 표시하며 함께 업데이트되게 하려면, state를 개별 버튼에서 모든 버튼을 포함하는 가장 가까운 컴포넌트로 옮겨야 함
- 이 예시에서는 `MyApp`임

```js
export default function MyApp() {
  const [count, setCount] = useState(0);

  function handleClick() {
    setCount(count + 1);
  }

  return (
    <div>
      <h1>Counters that update together</h1>
      <MyButton count={count} onClick={handleClick} />
      <MyButton count={count} onClick={handleClick} />
    </div>
  );
}

function MyButton({ count, onClick }) {
  return (
    <button onClick={onClick}>
      Clicked {count} times
    </button>
  );
}
```

- 이제 `MyApp`에서 `handleClick`을 호출하면 `count` state 변수 자체가 변경됨. `MyApp` 안의 두 자식 컴포넌트 모두 새로운 값을 받도록 갱신됨. 이를 "state 끌어올리기(lifting state up)"라고 함. state를 위로 옮김으로써 컴포넌트 간에 공유하게 되는 것임

```js
function MyButton({ count, onClick }) {
  return (
    <button onClick={onClick}>
      Clicked {count} times
    </button>
  );
}
```

- `MyApp`에서 `MyButton`으로 전달하는 정보를 props라고 함. 이제 `MyApp` 컴포넌트는 `count` state와 `handleClick` 이벤트 핸들러를 담고 있으며, 이 둘을 각 버튼에 props로 전달함

## 다음 단계

- 지금까지 React 코드를 작성하는 기본기를 살펴봄
- 실습을 통해 실제로 React 앱을 만들어보는 [React로 사고하기(Thinking in React)]와 [자습서: 틱택토 게임(Tutorial: Tic-Tac-Toe)]을 이어서 학습할 수 있음

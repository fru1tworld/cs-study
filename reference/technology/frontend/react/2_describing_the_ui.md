# React UI 정의하기 (Describing the UI)

> 원문: https://react.dev/learn/describing-the-ui

- React는 UI를 렌더링하기 위한 자바스크립트 라이브러리임
- UI는 버튼, 텍스트, 이미지 같은 작은 단위로 구성됨
- React는 이들을 재사용 가능하고 중첩 가능한 컴포넌트로 결합할 수 있게 함
- 웹사이트부터 모바일 앱까지, 화면의 모든 요소는 컴포넌트로 분해할 수 있음

### 이 장에서 다루는 내용

- 첫 번째 React 컴포넌트 작성 방법
- 다중 컴포넌트 파일을 만드는 시점과 방법
- JSX로 자바스크립트에 마크업을 추가하는 방법
- JSX에서 중괄호를 사용하여 자바스크립트 기능에 접근하는 방법
- props로 컴포넌트를 설정하는 방법
- 조건부 렌더링
- 여러 컴포넌트를 한 번에 렌더링하는 방법
- 컴포넌트를 순수하게 유지하여 혼란스러운 버그를 피하는 방법
- UI를 트리로 이해하는 것이 유용한 이유

---

## 첫 번째 컴포넌트 (Your First Component)

> 원문: https://react.dev/learn/your-first-component

### 컴포넌트: UI 구성 요소

- 웹에서 HTML은 `<h1>`, `<li>` 같은 내장 태그 세트로 풍부한 구조화 문서를 만들 수 있게 함

```html
<article>
  <h1>My First Component</h1>
  <ol>
    <li>Components: UI Building Blocks</li>
    <li>Defining a Component</li>
    <li>Using a Component</li>
  </ol>
</article>
```

- 이 마크업은 CSS로 스타일을 입히고 자바스크립트로 상호작용을 추가할 수 있음
- 웹의 모든 사이드바, 아바타, 모달, 드롭다운 등 모든 UI 요소의 기반임
- React는 마크업, CSS, 자바스크립트를 커스텀 "컴포넌트"로 결합할 수 있게 함 -- 앱을 위한 재사용 가능한 UI 요소임
- 컴포넌트를 조합, 정렬, 중첩하여 전체 페이지를 설계할 수 있음

```js
<PageLayout>
  <NavigationHeader>
    <SearchBar />
    <Link to="/docs">Docs</Link>
  </NavigationHeader>
  <Sidebar />
  <PageContent>
    <TableOfContents />
    <DocumentationText />
  </PageContent>
</PageLayout>
```

- 프로젝트가 성장하면 이미 작성한 컴포넌트를 재사용하여 많은 디자인을 구성할 수 있어 개발 속도가 빨라짐
- React 오픈 소스 커뮤니티에서 공유하는 컴포넌트(Chakra UI, Material UI 등)로 프로젝트를 빠르게 시작할 수도 있음

### 컴포넌트 정의하기

- 전통적으로 웹 개발자는 콘텐츠를 마크업한 뒤 자바스크립트로 상호작용을 추가했음
- 이제 많은 사이트와 모든 앱에서 상호작용이 필수적임
- React는 같은 기술을 사용하면서도 상호작용을 최우선으로 둠
- React 컴포넌트는 마크업을 뿌릴 수 있는 자바스크립트 함수임

```js
export default function Profile() {
  return (
    <img
      src="https://react.dev/images/docs/scientists/MK3eW3Am.jpg"
      alt="Katherine Johnson"
    />
  )
}
```

#### 1단계: 컴포넌트 내보내기

- `export default` 접두사는 표준 자바스크립트 문법임 (React 고유가 아님)
- 파일의 주요 함수를 표시하여 다른 파일에서 import할 수 있게 함

#### 2단계: 함수 정의하기

- `function Profile() { }`로 `Profile`이라는 이름의 자바스크립트 함수를 정의함
- React 컴포넌트는 일반 자바스크립트 함수이지만, 이름이 반드시 대문자로 시작해야 함. 그렇지 않으면 동작하지 않음

#### 3단계: 마크업 추가하기

- 컴포넌트가 `src`와 `alt` 속성이 있는 `<img />` 태그를 반환함
- `<img />`는 HTML처럼 작성되지만 실제로는 자바스크립트임. 이 문법을 JSX라고 함
- 반환문은 한 줄로 작성할 수 있음:

```js
return <img src="https://react.dev/images/docs/scientists/MK3eW3As.jpg" alt="Katherine Johnson" />;
```

- 마크업이 `return` 키워드와 같은 줄에 있지 않으면 반드시 괄호로 감싸야 함:

```js
return (
  <div>
    <img src="https://react.dev/images/docs/scientists/MK3eW3As.jpg" alt="Katherine Johnson" />
  </div>
);
```

- 괄호 없이 작성하면 `return` 뒤의 코드가 무시됨 (자바스크립트 자동 세미콜론 삽입)

### 컴포넌트 사용하기

- `Profile` 컴포넌트를 정의한 후 다른 컴포넌트 안에 중첩할 수 있음

```js
function Profile() {
  return (
    <img
      src="https://react.dev/images/docs/scientists/MK3eW3As.jpg"
      alt="Katherine Johnson"
    />
  );
}

export default function Gallery() {
  return (
    <section>
      <h1>Amazing scientists</h1>
      <Profile />
      <Profile />
      <Profile />
    </section>
  );
}
```

#### 브라우저가 보는 것

- 대소문자의 차이에 주의해야 함
  - `<section>`은 소문자이므로 React가 HTML 태그로 인식함
  - `<Profile />`은 대문자 `P`로 시작하므로 React가 `Profile` 컴포넌트를 사용하려 한다고 인식함
- 최종적으로 브라우저가 보는 것:

```html
<section>
  <h1>Amazing scientists</h1>
  <img src="https://react.dev/images/docs/scientists/MK3eW3As.jpg" alt="Katherine Johnson" />
  <img src="https://react.dev/images/docs/scientists/MK3eW3As.jpg" alt="Katherine Johnson" />
  <img src="https://react.dev/images/docs/scientists/MK3eW3As.jpg" alt="Katherine Johnson" />
</section>
```

#### 컴포넌트 중첩과 구성

- 컴포넌트는 일반 자바스크립트 함수이므로 같은 파일에 여러 컴포넌트를 둘 수 있음
- 파일이 복잡해지면 `Profile`을 별도 파일로 옮길 수 있음
- `Gallery` 안에 `Profile` 컴포넌트가 렌더링되므로, `Gallery`는 부모 컴포넌트이고 각 `Profile`은 자식 컴포넌트임
- 컴포넌트를 한 번 정의하면 원하는 만큼 여러 곳에서 사용할 수 있음
- 절대 컴포넌트 정의를 중첩해서는 안 됨:

```js
export default function Gallery() {
  // 다른 컴포넌트 안에 컴포넌트를 정의하면 안 됨!
  function Profile() {
    // ...
  }
  // ...
}
```

- 이렇게 하면 매우 느리고 버그를 유발함. 대신 최상위 수준에서 모든 컴포넌트를 정의해야 함:

```js
export default function Gallery() {
  // ...
}

// 최상위 수준에서 컴포넌트를 선언함
function Profile() {
  // ...
}
```

- 자식 컴포넌트가 부모의 데이터를 필요로 할 때는 정의를 중첩하는 대신 props로 전달해야 함

### 처음부터 끝까지 컴포넌트 (Deep Dive)

- React 앱은 "루트" 컴포넌트에서 시작됨. 보통 새 프로젝트를 시작할 때 자동으로 생성됨
- CodeSandbox에서는 `App.js`에, Next.js 같은 프레임워크에서는 `pages/index.js`에 정의됨
- 대부분의 React 앱은 처음부터 끝까지 컴포넌트를 사용함
- 버튼 같은 재사용 가능한 조각뿐 아니라 사이드바, 목록, 완전한 페이지 같은 더 큰 조각에도 컴포넌트를 사용함
- React 기반 프레임워크는 빈 HTML 파일 대신 React 컴포넌트에서 HTML을 자동 생성하여, 자바스크립트 코드가 로드되기 전에 일부 콘텐츠를 보여줄 수 있음
- 일부 웹사이트는 기존 HTML 페이지에 상호작용을 추가하기 위해서만 React를 사용함. 전체 페이지에 하나의 루트 컴포넌트 대신 여러 루트 컴포넌트를 가질 수 있음

### 요약

- React는 앱을 위한 재사용 가능한 UI 요소인 컴포넌트를 만들 수 있게 함
- React 앱에서 모든 UI 조각은 컴포넌트임
- React 컴포넌트는 일반 자바스크립트 함수이지만 다음이 다름:
  - 이름이 항상 대문자로 시작함
  - JSX 마크업을 반환함

---

## 컴포넌트 가져오기와 내보내기 (Importing and Exporting Components)

> 원문: https://react.dev/learn/importing-and-exporting-components

### 루트 컴포넌트 파일

- 첫 번째 컴포넌트에서 `Profile`과 `Gallery` 컴포넌트가 루트 컴포넌트 파일에 함께 있었음

```js
function Profile() {
  return (
    <img
      src="https://react.dev/images/docs/scientists/MK3eW3As.jpg"
      alt="Katherine Johnson"
    />
  );
}

export default function Gallery() {
  return (
    <section>
      <h1>Amazing scientists</h1>
      <Profile />
      <Profile />
      <Profile />
    </section>
  );
}
```

- 이 예에서 루트 컴포넌트 파일은 `App.js`임
- 설정에 따라 루트 컴포넌트가 다른 파일에 있을 수 있음
- Next.js처럼 파일 기반 라우팅을 사용하는 프레임워크에서는 루트 컴포넌트가 페이지마다 다름

### 컴포넌트 내보내기와 가져오기

- 컴포넌트를 이동하는 3단계:
  1. 컴포넌트를 넣을 새 JS 파일을 만듦
  2. 해당 파일에서 함수 컴포넌트를 내보냄 (default 또는 named export 사용)
  3. 컴포넌트를 사용할 파일에서 가져옴 (해당하는 import 방식 사용)

- `Profile`과 `Gallery`를 `App.js`에서 `Gallery.js`로 이동한 예:

```js
// App.js
import Gallery from './Gallery.js';

export default function App() {
  return (
    <Gallery />
  );
}
```

```js
// Gallery.js
function Profile() {
  return (
    <img
      src="https://react.dev/images/docs/scientists/QIrZWGIs.jpg"
      alt="Alan L. Hart"
    />
  );
}

export default function Gallery() {
  return (
    <section>
      <h1>Amazing scientists</h1>
      <Profile />
      <Profile />
      <Profile />
    </section>
  );
}
```

- `Gallery.js`는 `Profile` 컴포넌트를 정의하고(파일 내에서만 사용, 내보내지 않음), `Gallery` 컴포넌트를 default export로 내보냄
- `App.js`는 `Gallery`를 default import로 가져오고, 루트 `App` 컴포넌트를 default export로 내보냄
- 파일 확장자 `.js`는 생략 가능함: `'./Gallery.js'`와 `'./Gallery'` 모두 동작함

### default export vs named export (Deep Dive)

- 자바스크립트에서 값을 내보내는 두 가지 주요 방법: default export와 named export
- 한 파일에 default export는 최대 하나만 가능함
- named export는 원하는 만큼 가질 수 있음
- 내보내는 방식에 따라 가져오는 방식이 결정됨

- default export:
  - 내보내기: `export default function Button() {}`
  - 가져오기: `import Button from './Button.js';`
  - 가져올 때 아무 이름이나 사용 가능: `import Banana from './Button.js'`도 동작함

- named export:
  - 내보내기: `export function Button() {}`
  - 가져오기: `import { Button } from './Button.js';`
  - 이름이 양쪽에서 일치해야 함

- 모범 사례:
  - 파일이 하나의 컴포넌트만 내보내면 default export를 사용함
  - 여러 컴포넌트와 값을 내보내면 named export를 사용함
  - 항상 의미 있는 이름을 컴포넌트 함수와 파일에 부여함
  - `export default () => {}`처럼 이름 없는 컴포넌트는 디버깅을 어렵게 하므로 피함

### 같은 파일에서 여러 컴포넌트 내보내기와 가져오기

- `Gallery.js`에 이미 default export가 있으므로 `Profile`에 두 번째 default export를 추가할 수 없음
- named export를 사용하여 `Profile`을 내보냄

- 한 파일에 default export는 하나만, named export는 여러 개 가질 수 있음

```js
// App.js
import Gallery from './Gallery.js';
import { Profile } from './Gallery.js';

export default function App() {
  return (
    <Profile />
  );
}
```

```js
// Gallery.js
export function Profile() {
  return (
    <img
      src="https://react.dev/images/docs/scientists/QIrZWGIs.jpg"
      alt="Alan L. Hart"
    />
  );
}

export default function Gallery() {
  return (
    <section>
      <h1>Amazing scientists</h1>
      <Profile />
      <Profile />
      <Profile />
    </section>
  );
}
```

- `Gallery.js`는 `Profile`을 named export로, `Gallery`를 default export로 내보냄
- `App.js`는 `Profile`을 named import로, `Gallery`를 default import로 가져옴
- default와 named export 간의 혼동을 줄이기 위해, 일부 팀은 한 가지 스타일만 사용하거나 한 파일에서 혼합하지 않음

### 요약

- 루트 컴포넌트 파일이 무엇인지 이해함
- 컴포넌트를 가져오고 내보내는 방법을 이해함
- default와 named import/export를 사용하는 시점과 방법을 이해함
- 같은 파일에서 여러 컴포넌트를 내보내는 방법을 이해함

---

## JSX로 마크업 작성하기 (Writing Markup with JSX)

> 원문: https://react.dev/learn/writing-markup-with-jsx

### JSX란

- JSX는 자바스크립트 안에서 HTML과 유사한 마크업을 작성할 수 있게 하는 자바스크립트 문법 확장임
- 대부분의 React 개발자는 간결함 때문에 JSX를 선호하며, 대부분의 코드베이스에서 사용함

### JSX: 자바스크립트에 마크업 넣기

- 역사적으로 웹 개발은 관심사를 분리했음:
  - HTML은 콘텐츠를 담당함
  - CSS는 디자인을 담당함
  - 자바스크립트는 로직을 담당함 (종종 별도 파일에)
- 그러나 웹이 더 상호작용적으로 변하면서 자바스크립트가 콘텐츠를 점점 더 결정하게 됨
- React에서는 렌더링 로직과 마크업이 같은 곳, 즉 컴포넌트에 함께 존재함
- 버튼의 렌더링 로직과 마크업을 함께 유지하면 편집할 때마다 동기화가 보장됨
- 관련 없는 세부 사항(버튼 마크업과 사이드바 마크업)은 서로 격리되어 변경이 안전해짐
- JSX와 React는 별개임. 독립적으로 사용할 수 있음. JSX는 문법 확장이고, React는 자바스크립트 라이브러리임

### HTML을 JSX로 변환하기

- 기존 HTML:

```html
<h1>Hedy Lamarr's Todos</h1>
<img
  src="https://react.dev/images/docs/scientists/yXOvdOSs.jpg"
  alt="Hedy Lamarr"
  class="photo"
>
<ul>
    <li>Invent new traffic lights
    <li>Rehearse a movie scene
    <li>Improve the spectrum technology
</ul>
```

- 이것을 React 컴포넌트에 직접 복사하면 동작하지 않음. JSX는 HTML보다 엄격하고 추가 규칙이 있음

### JSX의 규칙

#### 1. 단일 루트 엘리먼트를 반환해야 함

- 컴포넌트에서 여러 엘리먼트를 반환하려면 하나의 부모 태그로 감싸야 함

- 방법 1 -- `<div>` 사용:

```js
<div>
  <h1>Hedy Lamarr's Todos</h1>
  <img
    src="https://react.dev/images/docs/scientists/yXOvdOSs.jpg"
    alt="Hedy Lamarr"
    className="photo"
  />
  <ul>
    ...
  </ul>
</div>
```

- 방법 2 -- Fragment 사용:

```js
<>
  <h1>Hedy Lamarr's Todos</h1>
  <img
    src="https://react.dev/images/docs/scientists/yXOvdOSs.jpg"
    alt="Hedy Lamarr"
    className="photo"
  />
  <ul>
    ...
  </ul>
</>
```

- 빈 태그 `<>`와 `</>`를 Fragment라고 함. Fragment는 브라우저 HTML 트리에 흔적을 남기지 않고 그룹핑할 수 있게 함

- 여러 JSX 태그를 감싸야 하는 이유: JSX는 HTML처럼 보이지만 내부적으로 일반 자바스크립트 객체로 변환됨. 함수에서 두 객체를 배열로 감싸지 않고는 반환할 수 없음. 이것이 두 JSX 태그를 다른 태그나 Fragment로 감싸야 하는 이유임

#### 2. 모든 태그를 닫아야 함

- JSX는 태그를 명시적으로 닫아야 함
  - `<img>` 같은 자체 닫힘 태그는 `<img />`가 되어야 함
  - `<li>oranges` 같은 감싸는 태그는 `<li>oranges</li>`로 작성해야 함

```js
<>
  <img
    src="https://react.dev/images/docs/scientists/yXOvdOSs.jpg"
    alt="Hedy Lamarr"
    className="photo"
   />
  <ul>
    <li>Invent new traffic lights</li>
    <li>Rehearse a movie scene</li>
    <li>Improve the spectrum technology</li>
  </ul>
</>
```

#### 3. 대부분 camelCase로 작성함

- JSX는 자바스크립트로 변환되고 속성은 자바스크립트 객체의 키가 됨. 자바스크립트는 변수 이름에 제한이 있음 (대시 불가, 예약어 불가)
- 주요 변환:
  - `stroke-width` -> `strokeWidth`
  - `class` -> `className` (`class`가 예약어이므로)

```js
<img
  src="https://react.dev/images/docs/scientists/yXOvdOSs.jpg"
  alt="Hedy Lamarr"
  className="photo"
/>
```

- 예외: 역사적 이유로 `aria-*`와 `data-*` 속성은 HTML처럼 대시를 사용하여 작성함

- 팁: JSX 변환 도구를 사용하여 기존 HTML과 SVG를 JSX로 변환할 수 있음

### 최종 교정 결과

```js
export default function TodoList() {
  return (
    <>
      <h1>Hedy Lamarr's Todos</h1>
      <img
        src="https://react.dev/images/docs/scientists/yXOvdOSs.jpg"
        alt="Hedy Lamarr"
        className="photo"
      />
      <ul>
        <li>Invent new traffic lights</li>
        <li>Rehearse a movie scene</li>
        <li>Improve the spectrum technology</li>
      </ul>
    </>
  );
}
```

### 요약

- React 컴포넌트는 관련된 렌더링 로직과 마크업을 함께 그룹핑함
- JSX는 HTML과 유사하지만 몇 가지 차이가 있음. 필요하면 변환 도구를 사용할 수 있음
- 에러 메시지가 마크업 수정 방향을 안내해 줌

---

## JSX에서 중괄호로 자바스크립트 사용하기 (JavaScript in JSX with Curly Braces)

> 원문: https://react.dev/learn/javascript-in-jsx-with-curly-braces

### 따옴표로 문자열 전달하기

- JSX에 문자열 속성을 전달할 때 작은따옴표나 큰따옴표를 사용함:

```js
export default function Avatar() {
  return (
    <img
      className="avatar"
      src="https://react.dev/images/docs/scientists/7vQD0fPs.jpg"
      alt="Gregorio Y. Zara"
    />
  );
}
```

- 속성을 동적으로 지정하려면 따옴표를 중괄호로 대체함:

```js
export default function Avatar() {
  const avatar = 'https://react.dev/images/docs/scientists/7vQD0fPs.jpg';
  const description = 'Gregorio Y. Zara';
  return (
    <img
      className="avatar"
      src={avatar}
      alt={description}
    />
  );
}
```

- 핵심 차이: `className="avatar"`는 문자열 리터럴을 전달하고, `src={avatar}`는 자바스크립트 변수 값을 읽음

### 중괄호 사용하기: 자바스크립트 세계로의 창

- JSX는 중괄호 `{ }` 안에서 자바스크립트를 허용함. 마크업 안에 변수를 직접 삽입할 수 있음:

```js
export default function TodoList() {
  const name = 'Gregorio Y. Zara';
  return (
    <h1>{name}'s To Do List</h1>
  );
}
```

- 함수 호출을 포함하여 모든 자바스크립트 표현식이 중괄호 사이에서 동작함:

```js
const today = new Date();

function formatDate(date) {
  return new Intl.DateTimeFormat(
    'en-US',
    { weekday: 'long' }
  ).format(date);
}

export default function TodoList() {
  return (
    <h1>To Do List for {formatDate(today)}</h1>
  );
}
```

### 중괄호를 사용할 수 있는 위치

- JSX 안에서 중괄호를 사용할 수 있는 두 가지 방법만 있음:
  1. JSX 태그 안의 텍스트로 직접 사용: `<h1>{name}'s To Do List</h1>` -- 동작함. `<{tag}>Gregorio Y. Zara's To Do List</{tag}>` -- 동작하지 않음
  2. `=` 기호 바로 뒤의 속성으로 사용: `src={avatar}` -- `avatar` 변수를 읽음. `src="{avatar}"` -- 문자열 리터럴 `"{avatar}"`를 전달함

### "이중 중괄호": JSX 안의 CSS와 객체

- 객체는 중괄호로 표기됨. JSX에서 자바스크립트 객체를 전달하려면 객체를 또 다른 중괄호 쌍으로 감쌈: `person={{ name: "Hedy Lamarr", inventions: 5 }}`

- 인라인 CSS 스타일 예:

```js
export default function TodoList() {
  return (
    <ul style={{
      backgroundColor: 'black',
      color: 'pink'
    }}>
      <li>Improve the videophone</li>
      <li>Prepare aeronautics lectures</li>
      <li>Work on the alcohol-fuelled engine</li>
    </ul>
  );
}
```

- `{{ }}`는 특별한 문법이 아님 -- JSX 중괄호 안의 자바스크립트 객체일 뿐임
- 인라인 `style` 속성은 camelCase로 작성함:
  - HTML: `<ul style="background-color: black">`
  - JSX: `<ul style={{ backgroundColor: 'black' }}>`

### 자바스크립트 객체와 중괄호 활용

- 여러 표현식을 하나의 객체로 옮기고 JSX에서 참조할 수 있음:

```js
const person = {
  name: 'Gregorio Y. Zara',
  theme: {
    backgroundColor: 'black',
    color: 'pink'
  }
};

export default function TodoList() {
  return (
    <div style={person.theme}>
      <h1>{person.name}'s Todos</h1>
      <img
        className="avatar"
        src="https://react.dev/images/docs/scientists/7vQD0fPs.jpg"
        alt="Gregorio Y. Zara"
      />
      <ul>
        <li>Improve the videophone</li>
        <li>Prepare aeronautics lectures</li>
        <li>Work on the alcohol-fuelled engine</li>
      </ul>
    </div>
  );
}
```

- JSX는 자바스크립트를 사용하여 데이터와 로직을 구성할 수 있게 하므로 최소한의 템플릿 언어임

### 요약

- 따옴표 안의 JSX 속성은 문자열로 전달됨
- 중괄호는 자바스크립트 로직과 변수를 마크업에 가져올 수 있게 함
- JSX 태그 콘텐츠 안이나 속성의 `=` 바로 뒤에서 동작함
- `{{ }}`는 특별한 문법이 아님 -- JSX 중괄호 안의 자바스크립트 객체임

---

## 컴포넌트에 Props 전달하기 (Passing Props to a Component)

> 원문: https://react.dev/learn/passing-props-to-a-component

### 익숙한 props

- props는 JSX 태그에 전달되는 정보임. `<img>` 태그의 `className`, `src`, `alt`, `width`, `height` 같은 것들이 예시임:

```js
function Avatar() {
  return (
    <img
      className="avatar"
      src="https://react.dev/images/docs/scientists/1bX5QH6.jpg"
      alt="Lin Lanying"
      width={100}
      height={100}
    />
  );
}

export default function Profile() {
  return (
    <Avatar />
  );
}
```

- `<img>` 같은 HTML 태그의 props는 미리 정의되어 있음 (ReactDOM이 HTML 표준을 따름)
- 자신의 컴포넌트에는 어떤 props든 전달할 수 있음

### 컴포넌트에 props 전달하기

#### 1단계: 자식 컴포넌트에 props 전달하기

```js
export default function Profile() {
  return (
    <Avatar
      person={{ name: 'Lin Lanying', imageId: '1bX5QH6' }}
      size={100}
    />
  );
}
```

- `person={{ }}`의 이중 중괄호는 JSX 중괄호 안의 객체일 뿐임

#### 2단계: 자식 컴포넌트에서 props 읽기

- `function` 뒤의 `({`와 `})` 안에 쉼표로 구분하여 이름을 나열함:

```js
function Avatar({ person, size }) {
  // person과 size를 여기서 사용할 수 있음
}
```

- 전체 예:

```js
import { getImageUrl } from './utils.js';

function Avatar({ person, size }) {
  return (
    <img
      className="avatar"
      src={getImageUrl(person)}
      alt={person.name}
      width={size}
      height={size}
    />
  );
}

export default function Profile() {
  return (
    <div>
      <Avatar
        size={100}
        person={{
          name: 'Katsuko Saruhashi',
          imageId: 'YfeOqp2'
        }}
      />
      <Avatar
        size={80}
        person={{
          name: 'Aklilu Lemma',
          imageId: 'OKS67lh'
        }}
      />
      <Avatar
        size={50}
        person={{
          name: 'Lin Lanying',
          imageId: '1bX5QH6'
        }}
      />
    </div>
  );
}
```

- props를 사용하면 부모와 자식 컴포넌트를 독립적으로 생각할 수 있음
- React 컴포넌트 함수는 단일 인수인 `props` 객체를 받음:

```js
function Avatar(props) {
  let person = props.person;
  let size = props.size;
  // ...
}
```

- 보통 전체 `props` 객체 대신 구조 분해를 사용함:

```js
function Avatar({ person, size }) {
  // ...
}
```

- props를 선언할 때 `( )` 안의 `{ }` 중괄호 쌍을 놓치면 안 됨. 이것을 "구조 분해(destructuring)"라고 함

### prop의 기본값 지정하기

- 파라미터 뒤에 `=`와 기본값을 할당함:

```js
function Avatar({ person, size = 100 }) {
  // ...
}
```

- `<Avatar person={...} />`이 `size` prop 없이 렌더링되면 `size`는 `100`이 됨
- 기본값은 `size`가 누락되었거나 `undefined`일 때만 사용됨. `size={null}`이나 `size={0}`을 전달하면 기본값이 사용되지 않음

### JSX 전개 구문으로 props 전달하기

- props 전달이 반복적일 때:

```js
function Profile({ person, size, isSepia, thickBorder }) {
  return (
    <div className="card">
      <Avatar
        person={person}
        size={size}
        isSepia={isSepia}
        thickBorder={thickBorder}
      />
    </div>
  );
}
```

- 전개 구문을 사용하여 간결하게 작성할 수 있음:

```js
function Profile(props) {
  return (
    <div className="card">
      <Avatar {...props} />
    </div>
  );
}
```

- 전개 구문은 절제하여 사용해야 함. 모든 컴포넌트에서 사용한다면 무언가 잘못된 것임. 종종 컴포넌트를 분리하고 children으로 JSX를 전달해야 한다는 신호임

### children으로 JSX 전달하기

- 내장 브라우저 태그처럼 자신의 컴포넌트를 중첩할 수 있음:

```js
<Card>
  <Avatar />
</Card>
```

- JSX 태그 안에 콘텐츠를 중첩하면 부모 컴포넌트가 해당 콘텐츠를 `children`이라는 prop으로 받음:

```js
import Avatar from './Avatar.js';

function Card({ children }) {
  return (
    <div className="card">
      {children}
    </div>
  );
}

export default function Profile() {
  return (
    <Card>
      <Avatar
        size={100}
        person={{
          name: 'Katsuko Saruhashi',
          imageId: 'YfeOqp2'
        }}
      />
    </Card>
  );
}
```

- `children` prop이 있는 컴포넌트는 부모가 임의의 JSX로 "채울" 수 있는 "구멍"을 가진 것임
- 이 패턴은 패널, 그리드 같은 시각적 래퍼에서 흔히 사용됨

### props가 시간에 따라 변하는 방식

- 컴포넌트는 시간에 따라 다른 props를 받을 수 있음. props는 정적이지 않음 -- 특정 시점의 컴포넌트 데이터를 반영함
- 하지만 props는 불변(immutable)임. 컴포넌트가 props를 변경해야 할 때는 부모 컴포넌트에 다른 props, 즉 새 객체를 전달하도록 "요청"해야 함. 이전 props는 버려지고 결국 가비지 컬렉션됨
- props를 직접 "변경"하려 하면 안 됨. 사용자 입력에 반응해야 할 때는 state를 사용해야 함

### 요약

- props를 전달하려면 HTML 속성처럼 JSX에 추가함
- props를 읽으려면 `function Avatar({ person, size })` 구조 분해 문법을 사용함
- `size = 100`처럼 누락되거나 `undefined`인 props의 기본값을 지정할 수 있음
- `<Avatar {...props} />` 전개 구문으로 모든 props를 전달할 수 있지만 남용하면 안 됨
- `<Card><Avatar /></Card>` 같은 중첩 JSX는 `Card` 컴포넌트의 `children` prop으로 나타남
- props는 읽기 전용 스냅샷임: 모든 렌더에서 새 버전을 받음
- props를 변경할 수 없음; 상호작용에는 state를 사용함

---

## 조건부 렌더링 (Conditional Rendering)

> 원문: https://react.dev/learn/conditional-rendering

### 조건부로 JSX 반환하기

- 컴포넌트는 종종 다른 조건에 따라 다른 것을 표시해야 함
- React에서는 `if` 문, `&&`, `? :` 연산자 같은 자바스크립트 문법을 사용하여 JSX를 조건부로 렌더링할 수 있음

```js
function Item({ name, isPacked }) {
  return <li className="item">{name}</li>;
}

export default function PackingList() {
  return (
    <section>
      <h1>Sally Ride's Packing List</h1>
      <ul>
        <Item
          isPacked={true}
          name="Space suit"
        />
        <Item
          isPacked={true}
          name="Helmet with a golden leaf"
        />
        <Item
          isPacked={false}
          name="Photo of Tam"
        />
      </ul>
    </section>
  );
}
```

#### if/else 문 사용하기

```js
function Item({ name, isPacked }) {
  if (isPacked) {
    return <li className="item">{name} ✅</li>;
  }
  return <li className="item">{name}</li>;
}
```

- `isPacked` prop이 `true`이면 다른 JSX 트리를 반환함
- React에서 제어 흐름(조건 등)은 자바스크립트로 처리됨

#### null로 아무것도 반환하지 않기

- 어떤 상황에서는 아무것도 렌더링하고 싶지 않을 수 있음. 컴포넌트는 무언가를 반환해야 하므로 `null`을 반환할 수 있음:

```js
if (isPacked) {
  return null;
}
return <li className="item">{name}</li>;
```

- 컴포넌트에서 `null`을 반환하는 것은 흔하지 않음. 개발자가 렌더링하려 할 때 놀랄 수 있기 때문임. 더 자주 부모 컴포넌트의 JSX에서 조건부로 컴포넌트를 포함하거나 제외함

### 조건부로 JSX 포함하기

- if/else를 사용하면 렌더 출력에 중복이 발생하기 쉬움:

```js
if (isPacked) {
  return <li className="item">{name} ✅</li>;
}
return <li className="item">{name}</li>;
```

- 두 분기 모두 `<li className="item">...</li>`를 반환함. 이 중복을 피하기 위해 조건부로 약간의 JSX만 포함할 수 있음

#### 조건(삼항) 연산자 (`? :`)

```js
return (
  <li className="item">
    {isPacked ? name + ' ✅' : name}
  </li>
);
```

- "`isPacked`가 true이면 `name + ' ✅'`를 렌더링하고, 그렇지 않으면 `name`을 렌더링한다"로 읽을 수 있음

- JSX 엘리먼트는 "인스턴스"가 아님. 내부 상태를 보유하지 않고 실제 DOM 노드가 아님. 가벼운 설명, 즉 청사진 같은 것임. 그래서 두 예제는 완전히 동등함

- 더 복잡한 중첩도 가능함:

```js
function Item({ name, isPacked }) {
  return (
    <li className="item">
      {isPacked ? (
        <del>
          {name + ' ✅'}
        </del>
      ) : (
        name
      )}
    </li>
  );
}
```

- 단순한 조건에 잘 동작하지만 적당히 사용해야 함. 중첩된 조건부 마크업이 너무 많으면 자식 컴포넌트를 추출하여 정리하는 것을 고려함

#### 논리 AND 연산자 (`&&`)

- 조건이 true일 때 JSX를 렌더링하고, 그렇지 않으면 아무것도 렌더링하지 않으려 할 때 사용함:

```js
return (
  <li className="item">
    {name} {isPacked && '✅'}
  </li>
);
```

- "`isPacked`이면 체크마크를 렌더링하고, 그렇지 않으면 아무것도 렌더링하지 않는다"로 읽을 수 있음
- 자바스크립트 `&&` 표현식은 왼쪽이 `true`이면 오른쪽 값을 반환함. 조건이 `false`이면 전체 표현식이 `false`가 됨. React는 `false`를 JSX 트리의 "구멍"으로 간주하고 아무것도 렌더링하지 않음

- 주의: `&&`의 왼쪽에 숫자를 넣으면 안 됨
  - 자바스크립트는 왼쪽을 자동으로 boolean으로 변환함. 왼쪽이 `0`이면 전체 표현식이 `0`이 되고, React는 아무것도 안 렌더링하는 대신 `0`을 렌더링함
  - 흔한 실수: `messageCount && <p>New messages</p>`. `messageCount`가 `0`일 때 아무것도 안 렌더링할 것 같지만 실제로는 `0`을 렌더링함
  - 해결: 왼쪽을 boolean으로 만듦: `messageCount > 0 && <p>New messages</p>`

#### 변수에 조건부로 JSX 할당하기

- 단축 구문이 복잡해지면 `if` 문과 변수를 사용함:

```js
function Item({ name, isPacked }) {
  let itemContent = name;
  if (isPacked) {
    itemContent = name + " ✅";
  }
  return (
    <li className="item">
      {itemContent}
    </li>
  );
}
```

- 가장 장황하지만 가장 유연한 스타일임
- 텍스트뿐 아니라 임의의 JSX에도 동작함:

```js
function Item({ name, isPacked }) {
  let itemContent = name;
  if (isPacked) {
    itemContent = (
      <del>
        {name + " ✅"}
      </del>
    );
  }
  return (
    <li className="item">
      {itemContent}
    </li>
  );
}
```

### 요약

- React에서는 자바스크립트로 분기 로직을 제어함
- `if` 문으로 JSX 표현식을 조건부로 반환할 수 있음
- 중괄호를 사용하여 변수에 조건부로 JSX를 저장하고 다른 JSX에 포함할 수 있음
- JSX에서 `{cond ? <A /> : <B />}`는 "`cond`이면 `<A />`를 렌더링하고, 그렇지 않으면 `<B />`를 렌더링한다"는 의미임
- JSX에서 `{cond && <A />}`는 "`cond`이면 `<A />`를 렌더링하고, 그렇지 않으면 아무것도 렌더링하지 않는다"는 의미임
- 단축 구문은 흔하지만 선호에 따라 일반 `if`를 사용해도 됨

---

## 리스트 렌더링 (Rendering Lists)

> 원문: https://react.dev/learn/rendering-lists

### 배열에서 데이터 렌더링하기

- 데이터 컬렉션에서 여러 유사한 컴포넌트를 표시하고 싶을 때 자바스크립트의 `filter()`와 `map()`을 React와 함께 사용함

#### 기본 예제

```js
<ul>
  <li>Creola Katherine Johnson: mathematician</li>
  <li>Mario Jose Molina-Pasquel Henriquez: chemist</li>
  <li>Mohammad Abdus Salam: physicist</li>
  <li>Percy Lavon Julian: chemist</li>
  <li>Subrahmanyan Chandrasekhar: astrophysicist</li>
</ul>
```

- 리스트 항목 간의 유일한 차이는 내용/데이터임. 데이터를 자바스크립트 객체와 배열에 저장한 뒤 `map()`과 `filter()`를 사용하여 컴포넌트 리스트를 렌더링할 수 있음

#### 3단계 프로세스

1. 데이터를 배열로 옮기기:

```js
const people = [
  'Creola Katherine Johnson: mathematician',
  'Mario Jose Molina-Pasquel Henriquez: chemist',
  'Mohammad Abdus Salam: physicist',
  'Percy Lavon Julian: chemist',
  'Subrahmanyan Chandrasekhar: astrophysicist'
];
```

2. 배열 멤버를 JSX 노드의 새 배열로 map하기:

```js
const listItems = people.map(person => <li>{person}</li>);
```

3. `<ul>`로 감싸서 listItems를 반환하기:

```js
return <ul>{listItems}</ul>;
```

- 전체 예:

```js
const people = [
  'Creola Katherine Johnson: mathematician',
  'Mario Jose Molina-Pasquel Henriquez: chemist',
  'Mohammad Abdus Salam: physicist',
  'Percy Lavon Julian: chemist',
  'Subrahmanyan Chandrasekhar: astrophysicist'
];

export default function List() {
  const listItems = people.map(person =>
    <li>{person}</li>
  );
  return <ul>{listItems}</ul>;
}
```

- 이 경우 콘솔에 경고가 표시됨: `Warning: Each child in a list should have a unique "key" prop.`

### 배열 항목 필터링하기

- 데이터를 객체로 구조화할 수 있음:

```js
const people = [{
  id: 0,
  name: 'Creola Katherine Johnson',
  profession: 'mathematician',
}, {
  id: 1,
  name: 'Mario Jose Molina-Pasquel Henriquez',
  profession: 'chemist',
}, {
  id: 2,
  name: 'Mohammad Abdus Salam',
  profession: 'physicist',
}, {
  id: 3,
  name: 'Percy Lavon Julian',
  profession: 'chemist',
}, {
  id: 4,
  name: 'Subrahmanyan Chandrasekhar',
  profession: 'astrophysicist',
}];
```

- `filter()`를 사용하여 특정 조건의 항목만 표시할 수 있음:

```js
const chemists = people.filter(person =>
  person.profession === 'chemist'
);
```

- 필터링 후 map으로 렌더링하기:

```js
import { people } from './data.js';
import { getImageUrl } from './utils.js';

export default function List() {
  const chemists = people.filter(person =>
    person.profession === 'chemist'
  );
  const listItems = chemists.map(person =>
    <li>
      <img
        src={getImageUrl(person)}
        alt={person.name}
      />
      <p>
        <b>{person.name}:</b>
        {' ' + person.profession + ' '}
        known for {person.accomplishment}
      </p>
    </li>
  );
  return <ul>{listItems}</ul>;
}
```

### 화살표 함수 주의사항

- 화살표 함수는 `=>` 바로 뒤의 표현식을 암묵적으로 반환함:

```js
const listItems = chemists.map(person =>
  <li>...</li> // 암묵적 반환!
);
```

- `=>` 뒤에 `{` 중괄호가 오면 `return`을 명시적으로 작성해야 함:

```js
const listItems = chemists.map(person => { // 중괄호
  return <li>...</li>;
});
```

- `=> {`가 있는 화살표 함수는 "블록 본문"을 가지며 명시적 `return`문이 필요함. `return`을 잊으면 아무것도 반환되지 않음

### key로 리스트 항목 순서 유지하기

- 각 배열 항목에 `key`를 부여해야 함 -- 형제들 사이에서 고유하게 식별하는 문자열 또는 숫자임:

```js
<li key={person.id}>...</li>
```

- `map()` 호출 안에서 직접 사용되는 JSX 엘리먼트에는 항상 key가 필요함

#### key가 중요한 이유

- key는 각 배열 항목이 어느 컴포넌트에 대응하는지 React에 알려줌
- 배열 항목이 정렬로 이동하거나, 삽입되거나, 삭제될 때 올바른 DOM 업데이트를 위해 필수적임
- key를 데이터에 포함시키는 것이 좋으며, 즉석에서 생성하면 안 됨
- 비유: 데스크탑의 파일에 이름이 없이 순서로만 참조한다고 상상하면 됨. 파일을 삭제하면 순서가 바뀌어 혼란이 발생함. key는 파일 이름처럼 위치와 무관하게 항목을 식별함

```js
import { people } from './data.js';
import { getImageUrl } from './utils.js';

export default function List() {
  const listItems = people.map(person =>
    <li key={person.id}>
      <img
        src={getImageUrl(person)}
        alt={person.name}
      />
      <p>
        <b>{person.name}</b>
          {' ' + person.profession + ' '}
          known for {person.accomplishment}
      </p>
    </li>
  );
  return <ul>{listItems}</ul>;
}
```

### 각 리스트 항목에 여러 DOM 노드 표시하기 (Deep Dive)

- 각 항목이 여러 DOM 노드를 렌더링해야 할 때, `<>...</>` Fragment 약칭은 `key`를 받을 수 없음
- `<Fragment>` 문법을 명시적으로 사용해야 함:

```js
import { Fragment } from 'react';

const listItems = people.map(person =>
  <Fragment key={person.id}>
    <h1>{person.name}</h1>
    <p>{person.bio}</p>
  </Fragment>
);
```

- Fragment는 DOM에서 사라지므로, 래퍼 div 없이 평평한 엘리먼트 목록을 생성함

### key를 얻는 곳

- 데이터베이스에서: 본질적으로 고유한 데이터베이스 키/ID를 사용함
- 로컬 생성 데이터에서: 증가하는 카운터, `crypto.randomUUID()`, 또는 `uuid` 패키지를 사용함

### key의 규칙

1. key는 형제 간에 고유해야 함 -- 다른 배열의 JSX 노드에서는 같은 key를 사용해도 됨
2. key는 변경되면 안 됨 -- 렌더링 중에 생성하면 안 됨

### React에 key가 필요한 이유

- key는 형제 간에 항목을 고유하게 식별함
- 위치보다 더 많은 정보를 제공함
- 재정렬로 위치가 변해도 key로 항목의 생애 동안 식별할 수 있음

### 인덱스를 key로 사용하면 안 되는 이유

- 항목이 삽입, 삭제되거나 배열이 재정렬되면 인덱스가 바뀌어 React가 컴포넌트 상태를 추적하지 못함
- `key={Math.random()}`으로 즉석 생성하면 렌더 간에 key가 일치하지 않아 모든 컴포넌트와 DOM이 매번 재생성됨. 느리고 리스트 항목 안의 사용자 입력이 손실됨
- 대신 데이터 기반의 안정적인 ID를 사용해야 함
- 컴포넌트는 `key`를 prop으로 받지 않음 -- React가 힌트로만 사용함. 컴포넌트에 ID가 필요하면 별도 prop으로 전달해야 함:

```js
<Profile key={id} userId={id} />
```

### 요약

- 데이터를 컴포넌트에서 배열과 객체 같은 데이터 구조로 옮기는 방법을 배움
- `map()`으로 유사한 컴포넌트 집합을 생성하는 방법을 배움
- `filter()`로 필터링된 항목 배열을 만드는 방법을 배움
- 컬렉션의 각 컴포넌트에 `key`를 설정하는 이유와 방법을 배움. 위치나 데이터가 변해도 React가 각 항목을 추적할 수 있게 함

---

## 컴포넌트를 순수하게 유지하기 (Keeping Components Pure)

> 원문: https://react.dev/learn/keeping-components-pure

### 순수성: 공식으로서의 컴포넌트

#### 순수 함수의 정의

- 순수 함수는 두 가지 특성을 가짐:
  1. 자기 일에만 신경 씀 -- 호출 전에 존재하던 객체나 변수를 변경하지 않음
  2. 같은 입력, 같은 출력 -- 같은 입력이 주어지면 항상 같은 결과를 반환함

- 수학 공식 예: y = 2x
  - x = 2이면 y = 4 (항상)
  - x = 3이면 y = 6 (항상)

```js
function double(number) {
  return 2 * number;
}
```

- `double`은 순수 함수임 -- `3`을 전달하면 항상 `6`을 반환함

#### React와 순수 컴포넌트

- React는 작성하는 모든 컴포넌트가 순수 함수라고 가정함
- React 컴포넌트는 같은 입력이 주어지면 항상 같은 JSX를 반환해야 함

```js
function Recipe({ drinkers }) {
  return (
    <ol>
      <li>Boil {drinkers} cups of water.</li>
      <li>Add {drinkers} spoons of tea and {0.5 * drinkers} spoons of spice.</li>
      <li>Add {0.5 * drinkers} cups of milk to boil and sugar to taste.</li>
    </ol>
  );
}

export default function App() {
  return (
    <section>
      <h1>Spiced Chai Recipe</h1>
      <h2>For two</h2>
      <Recipe drinkers={2} />
      <h2>For a gathering</h2>
      <Recipe drinkers={4} />
    </section>
  );
}
```

- `drinkers={2}`를 전달하면 항상 `2 cups of water`를 포함하는 JSX를 반환함
- `drinkers={4}`를 전달하면 항상 `4 cups of water`를 포함하는 JSX를 반환함
- 비유: 컴포넌트를 레시피처럼 생각함 -- 요리 중에 새 재료를 도입하지 않고 따르면 매번 같은 요리를 만듦

### 부작용(Side Effects): 의도하지 않은 결과

- React의 렌더링 과정은 항상 순수해야 함. 컴포넌트는 JSX를 반환만 해야 하며, 렌더링 전에 존재하던 객체나 변수를 변경하면 안 됨

#### 잘못된 컴포넌트 예 (비순수)

```js
let guest = 0;

function Cup() {
  // 나쁨: 이미 존재하는 변수를 변경함!
  guest = guest + 1;
  return <h2>Tea cup for guest #{guest}</h2>;
}

export default function TeaSet() {
  return (
    <>
      <Cup />
      <Cup />
      <Cup />
    </>
  );
}
```

- 이 컴포넌트는 외부에 선언된 `guest` 변수를 읽고 씀. 여러 번 호출하면 다른 JSX를 생성하여 예측 불가능함

#### 수정: props 사용

```js
function Cup({ guest }) {
  return <h2>Tea cup for guest #{guest}</h2>;
}

export default function TeaSet() {
  return (
    <>
      <Cup guest={1} />
      <Cup guest={2} />
      <Cup guest={3} />
    </>
  );
}
```

- 이제 컴포넌트가 순수함 -- 반환되는 JSX는 `guest` prop에만 의존함

#### 핵심 원칙

- 컴포넌트는 서로의 렌더링 순서에 의존해서는 안 됨
- 각 컴포넌트는 "스스로 생각해야" 하며 렌더링 중에 다른 컴포넌트와 조율하려 해서는 안 됨
- 렌더링은 학교 시험과 같음: 각 컴포넌트가 스스로 JSX를 계산해야 함

### StrictMode로 비순수 계산 감지하기

- 렌더링 중에 읽을 수 있는 세 가지 입력:
  1. Props
  2. State
  3. Context
- 이 입력들은 항상 읽기 전용으로 취급해야 함
- 사용자 입력에 대응하여 무언가를 변경하고 싶을 때는 변수에 쓰는 대신 state를 설정해야 함

- React의 "Strict Mode"는 개발 중에 각 컴포넌트 함수를 두 번 호출하여 순수성 규칙을 깨는 컴포넌트를 찾아냄
- 비순수 컴포넌트 예에서 "Guest #2", "Guest #4", "Guest #6" 대신 "Guest #1", "Guest #2", "Guest #3"이 표시되어야 했음
- 순수 함수는 계산만 하므로 두 번 호출해도 아무것도 변하지 않음
- Strict Mode는 프로덕션에서는 효과가 없으며 앱 속도를 늦추지 않음. `<React.StrictMode>`로 루트 컴포넌트를 감싸서 활성화함

### 로컬 변이: 컴포넌트의 작은 비밀

- 이미 존재하는 변수를 변이하는 것은 비순수함
- 그러나 렌더링 중에 방금 생성한 변수와 객체를 변경하는 것은 완전히 괜찮음

```js
function Cup({ guest }) {
  return <h2>Tea cup for guest #{guest}</h2>;
}

export default function TeaGathering() {
  const cups = [];
  for (let i = 1; i <= 12; i++) {
    cups.push(<Cup key={i} guest={i} />);
  }
  return cups;
}
```

- `cups` 변수와 `[]` 배열이 `TeaGathering` 안에서 생성되었기 때문에 괜찮음
- `TeaGathering` 외부의 코드는 이 변경을 알지 못함
- 이것을 "로컬 변이(local mutation)"라고 함 -- 컴포넌트의 작은 비밀임

### 부작용을 둘 수 있는 곳

- 함수형 프로그래밍이 순수성에 의존하지만, 무언가는 변해야 함. 이러한 변화를 부작용(side effects)이라고 함 -- 렌더링 중이 아닌 "곁에서" 일어나는 것임

- 부작용에는 다음이 포함됨:
  - 화면 업데이트
  - 애니메이션 시작
  - 데이터 변경

#### 1. 이벤트 핸들러 (권장)

- 이벤트 핸들러는 작업(예: 버튼 클릭)을 수행할 때 실행되는 함수임
- 컴포넌트 안에 정의되지만 렌더링 중에 실행되지 않음
- 이벤트 핸들러는 순수할 필요가 없음

#### 2. useEffect (최후의 수단)

- 적절한 이벤트 핸들러를 찾을 수 없으면 `useEffect`를 사용하여 부작용을 붙일 수 있음
- 이것은 렌더링 후에 실행하도록 React에 지시하며, 그때 부작용이 허용됨
- 가능하면 렌더링만으로 로직을 표현하는 것이 최선임

### React가 순수성을 중시하는 이유

1. 서버 렌더링 -- 컴포넌트가 같은 입력에 같은 결과를 반환하므로 하나의 컴포넌트가 많은 사용자 요청을 처리할 수 있음
2. 성능 최적화 -- 입력이 변하지 않은 컴포넌트의 렌더링을 건너뛸 수 있음. 순수 함수는 항상 같은 결과를 반환하므로 캐싱해도 안전함
3. 안전한 렌더링 재시작 -- 깊은 컴포넌트 트리를 렌더링하는 중에 데이터가 변하면 React는 오래된 렌더를 낭비하지 않고 렌더링을 다시 시작할 수 있음. 순수성 덕분에 언제든 계산을 중단해도 안전함
4. 미래 React 기능 -- 데이터 가져오기, 애니메이션, 성능까지 모든 새 React 기능이 순수성을 활용함

### 요약

- 컴포넌트는 순수해야 함:
  - 자기 일에만 신경 씀 -- 렌더링 전에 존재하던 객체나 변수를 변경하면 안 됨
  - 같은 입력, 같은 출력 -- 같은 입력이 주어지면 항상 같은 JSX를 반환함
- 렌더링은 언제든 발생할 수 있음; 컴포넌트는 서로의 렌더링 순서에 의존하면 안 됨
- 렌더링에 사용하는 입력(props, state, context)을 변이하면 안 됨
- 화면을 업데이트하려면 기존 객체를 변이하는 대신 state를 "설정"함
- 컴포넌트 로직을 반환하는 JSX에 표현함
- 변경이 필요하면 이벤트 핸들러를 먼저 사용하고, `useEffect`는 최후의 수단으로 사용함
- 순수 함수를 작성하는 것은 연습이 필요하지만 React 패러다임의 힘을 열어줌

---

## UI를 트리로 이해하기 (Understanding Your UI as a Tree)

> 원문: https://react.dev/learn/understanding-your-ui-as-a-tree

### 트리로서의 UI

- 트리는 항목 간의 관계 모델임. UI는 종종 트리 구조로 표현됨
- 브라우저는 HTML(DOM)과 CSS(CSSOM)를 모델링하기 위해 트리 구조를 사용함
- 모바일 플랫폼도 뷰 계층 구조를 표현하기 위해 트리를 사용함
- React는 컴포넌트로부터 UI 트리를 생성함. 이 트리는 React 앱에서 데이터가 흐르는 방식을 이해하고 렌더링과 앱 크기를 최적화하는 데 유용한 도구임

### 렌더 트리 (The Render Tree)

- 컴포넌트의 주요 기능은 다른 컴포넌트를 조합하는 능력임. 컴포넌트를 중첩하면 부모와 자식 컴포넌트의 개념이 생기며, 각 부모 컴포넌트는 다른 컴포넌트의 자식일 수 있음
- React 앱을 렌더링할 때 이 관계를 트리로 모델링할 수 있으며, 이를 렌더 트리라고 함

- 예제 앱:

```js
// App.js
import FancyText from './FancyText';
import InspirationGenerator from './InspirationGenerator';
import Copyright from './Copyright';

export default function App() {
  return (
    <>
      <FancyText title text="Get Inspired App" />
      <InspirationGenerator>
        <Copyright year={2004} />
      </InspirationGenerator>
    </>
  );
}
```

```js
// FancyText.js
export default function FancyText({title, text}) {
  return title
    ? <h1 className='fancy title'>{text}</h1>
    : <h3 className='fancy cursive'>{text}</h3>
}
```

```js
// InspirationGenerator.js
import * as React from 'react';
import quotes from './quotes';
import FancyText from './FancyText';

export default function InspirationGenerator({children}) {
  const [index, setIndex] = React.useState(0);
  const quote = quotes[index];
  const next = () => setIndex((index + 1) % quotes.length);

  return (
    <>
      <p>Your inspirational quote is:</p>
      <FancyText text={quote} />
      <button onClick={next}>Inspire me again</button>
      {children}
    </>
  );
}
```

```js
// Copyright.js
export default function Copyright({year}) {
  return <p className='small'>&#169; {year}</p>;
}
```

```js
// quotes.js
export default [
  "Don't let yesterday take up too much of today. -- Will Rogers",
  "Ambition is putting a ladder against the sky.",
  "A joy that's shared is a joy made double.",
];
```

#### 렌더 트리 구조

- React는 렌더링된 컴포넌트로 구성된 UI 트리인 렌더 트리를 생성함
- 트리는 노드로 구성되며, 각 노드는 컴포넌트를 나타냄
- 렌더 트리의 루트 노드는 앱의 루트 컴포넌트임
- 트리의 각 화살표는 부모 컴포넌트에서 자식 컴포넌트를 가리킴

- 구조:
  - App (루트)
    - FancyText
    - InspirationGenerator
      - FancyText
      - Copyright

#### HTML 태그는 렌더 트리에 어디 있는가 (Deep Dive)

- 렌더 트리에는 각 컴포넌트가 렌더링하는 HTML 태그가 언급되지 않음. 렌더 트리는 React 컴포넌트로만 구성됨
- React는 UI 프레임워크로서 플랫폼에 구애받지 않음. react.dev에서는 HTML 마크업을 UI 프리미티브로 사용하는 웹 예제를 보여주지만, React 앱은 UIView(iOS)나 FrameworkElement(Windows) 같은 다른 UI 프리미티브를 사용하는 모바일이나 데스크탑 플랫폼에서도 렌더링할 수 있음
- 이러한 플랫폼 UI 프리미티브는 React의 일부가 아님. React 렌더 트리는 앱이 어떤 플랫폼에서 렌더링되든 상관없이 React 앱에 대한 통찰을 제공함

### 조건부 렌더링과 렌더 트리

- 렌더 트리는 React 앱의 단일 렌더 패스를 나타냄. 조건부 렌더링을 사용하면 부모 컴포넌트가 전달된 데이터에 따라 다른 자식을 렌더링할 수 있음

- 조건부 렌더링이 있는 예:

```js
// InspirationGenerator.js
import * as React from 'react';
import inspirations from './inspirations';
import FancyText from './FancyText';
import Color from './Color';

export default function InspirationGenerator({children}) {
  const [index, setIndex] = React.useState(0);
  const inspiration = inspirations[index];
  const next = () => setIndex((index + 1) % inspirations.length);

  return (
    <>
      <p>Your inspirational {inspiration.type} is:</p>
      {inspiration.type === 'quote'
      ? <FancyText text={inspiration.value} />
      : <Color value={inspiration.value} />}

      <button onClick={next}>Inspire me again</button>
      {children}
    </>
  );
}
```

```js
// Color.js
export default function Color({value}) {
  return <div className="colorbox" style={{backgroundColor: value}} />
}
```

```js
// inspirations.js
export default [
  {type: 'quote', value: "Don't let yesterday take up too much of today. -- Will Rogers"},
  {type: 'color', value: "#B73636"},
  {type: 'quote', value: "Ambition is putting a ladder against the sky."},
  {type: 'color', value: "#256266"},
  {type: 'quote', value: "A joy that's shared is a joy made double."},
  {type: 'color', value: "#F9F2B4"},
];
```

- `inspiration.type`에 따라 `<FancyText>` 또는 `<Color>`를 렌더링할 수 있음. 렌더 트리는 각 렌더 패스마다 다를 수 있음

- 렌더 패스마다 렌더 트리가 다를 수 있지만, 이 트리들은 일반적으로 React 앱의 최상위(top-level) 컴포넌트와 리프(leaf) 컴포넌트를 식별하는 데 유용함:
  - 최상위 컴포넌트: 루트 컴포넌트에 가장 가까운 컴포넌트로, 그 아래 모든 컴포넌트의 렌더링 성능에 영향을 미치며 가장 복잡한 경우가 많음
  - 리프 컴포넌트: 트리 하단에 위치하며 자식 컴포넌트가 없고 자주 재렌더링됨
- 이러한 컴포넌트 분류는 앱의 데이터 흐름과 성능을 이해하는 데 유용함

### 모듈 의존성 트리 (The Module Dependency Tree)

- React 앱에서 트리로 모델링할 수 있는 또 다른 관계는 앱의 모듈 의존성임
- 컴포넌트와 로직을 별도 파일로 분리하면 컴포넌트, 함수, 상수를 내보낼 수 있는 JS 모듈을 생성함
- 모듈 의존성 트리의 각 노드는 모듈이고 각 가지는 해당 모듈의 `import` 문을 나타냄

#### 모듈 의존성 트리 구조

- 구조:
  - App.js (루트)
    - InspirationGenerator.js를 import함
    - FancyText.js를 import함
    - Copyright.js를 import함
  - InspirationGenerator.js
    - FancyText.js를 import함
    - Color.js를 import함
    - inspirations.js를 import함

#### 렌더 트리와 모듈 의존성 트리 비교

- 트리를 구성하는 노드가 컴포넌트가 아닌 모듈을 나타냄
- `inspirations.js` 같은 비컴포넌트 모듈도 이 트리에 표현됨. 렌더 트리는 컴포넌트만 포함함
- `Copyright.js`는 `App.js` 아래에 나타나지만 렌더 트리에서 `Copyright` 컴포넌트는 `InspirationGenerator`의 자식으로 나타남. `InspirationGenerator`가 JSX를 children props로 받아 `Copyright`을 자식 컴포넌트로 렌더링하지만 모듈을 import하지는 않기 때문임

#### 의존성 트리와 빌드 도구

- 의존성 트리는 React 앱을 실행하는 데 필요한 모듈을 결정하는 데 유용함
- 프로덕션을 위해 React 앱을 빌드할 때 클라이언트에 전달할 모든 필요한 자바스크립트를 번들링하는 빌드 단계가 있음. 이를 담당하는 도구를 번들러라고 함
- 번들러는 의존성 트리를 사용하여 포함할 모듈을 결정함
- 앱이 성장하면 번들 크기도 커지는 경우가 많음. 큰 번들 크기는 클라이언트가 다운로드하고 실행하기에 비용이 많이 듦. UI가 그려지는 시간을 지연시킬 수 있음. 앱의 의존성 트리를 파악하면 이러한 문제를 디버깅하는 데 도움이 됨

### 요약

- 트리는 엔티티 간의 관계를 표현하는 일반적인 방법임. UI를 모델링하는 데 자주 사용됨
- 렌더 트리는 단일 렌더에서 React 컴포넌트 간의 중첩 관계를 나타냄
- 조건부 렌더링으로 렌더 트리는 다른 렌더마다 변할 수 있음. 다른 prop 값으로 컴포넌트가 다른 자식 컴포넌트를 렌더링할 수 있음
- 렌더 트리는 최상위 컴포넌트와 리프 컴포넌트가 무엇인지 식별하는 데 도움이 됨. 최상위 컴포넌트는 그 아래 모든 컴포넌트의 렌더링 성능에 영향을 미치며, 리프 컴포넌트는 자주 재렌더링됨. 이들을 식별하는 것은 렌더링 성능을 이해하고 디버깅하는 데 유용함
- 의존성 트리는 React 앱의 모듈 의존성을 나타냄
- 의존성 트리는 빌드 도구가 앱을 전달하는 데 필요한 코드를 번들링하는 데 사용됨
- 의존성 트리는 그리기 시간을 늦추는 큰 번들 크기를 디버깅하고 번들링되는 코드를 최적화할 기회를 드러내는 데 유용함

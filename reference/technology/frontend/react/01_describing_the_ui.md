# React UI 정의하기 (Describing the UI)

> 원문: https://react.dev/learn/describing-the-ui

React는 버튼, 텍스트, 이미지 같은 UI 요소를 재사용하고 중첩할 수 있는 컴포넌트로 구성해 렌더링하는 자바스크립트 라이브러리다. 웹사이트부터 모바일 앱까지 화면의 각 요소를 컴포넌트로 분해하고, 이들을 조합해 전체 UI를 만든다.

## 첫 번째 컴포넌트 (Your First Component)

> 원문: https://react.dev/learn/your-first-component

### 컴포넌트: UI 구성 요소

웹에서는 HTML이 제공하는 `<h1>`, `<li>` 같은 내장 태그로 문서의 구조를 만든다.

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

이 마크업에 CSS로 스타일을 입히고 자바스크립트로 상호작용을 추가하면 사이드바, 아바타, 모달, 드롭다운 같은 UI를 만들 수 있다. React에서는 이 세 가지를 재사용 가능한 커스텀 컴포넌트로 결합한다. 아래처럼 컴포넌트를 조합하고 중첩해 배치하면 전체 페이지를 설계할 수 있다.

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

프로젝트가 커져도 이미 작성한 컴포넌트를 재사용하면 다양한 화면을 빠르게 만들 수 있다. React 오픈 소스 커뮤니티에서 공유하는 컴포넌트(Chakra UI, Material UI 등)로 프로젝트를 빠르게 시작할 수도 있다.

### 컴포넌트 정의하기

전통적으로 웹 개발자는 콘텐츠를 마크업한 뒤 자바스크립트로 상호작용을 덧붙였다. 하지만 이제는 많은 사이트와 모든 앱에서 상호작용이 필수가 되었다. React는 같은 기술을 사용하면서도 상호작용을 최우선에 둔다. React 컴포넌트는 마크업을 포함할 수 있는 자바스크립트 함수다.

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

`export default` 접두사는 React 고유 문법이 아니라 표준 자바스크립트 문법이다. 이 접두사로 파일의 주요 함수를 표시하면 다른 파일에서 그 함수를 import할 수 있다.

#### 2단계: 함수 정의하기

`function Profile() { }`은 `Profile`이라는 이름의 자바스크립트 함수를 정의한다. React 컴포넌트는 일반 자바스크립트 함수이지만 이름이 반드시 대문자로 시작해야 하며, 그렇지 않으면 동작하지 않는다.

#### 3단계: 마크업 추가하기

이 컴포넌트는 `src`와 `alt` 속성이 있는 `<img />` 태그를 반환한다. `<img />`는 HTML처럼 보이지만 실제로는 자바스크립트이며, 이 문법을 JSX라고 한다. 반환문은 다음처럼 한 줄로 작성할 수 있다.

```js
return <img src="https://react.dev/images/docs/scientists/MK3eW3As.jpg" alt="Katherine Johnson" />;
```

마크업이 `return` 키워드와 같은 줄에 있지 않으면 반드시 괄호로 감싸야 한다.

```js
return (
  <div>
    <img src="https://react.dev/images/docs/scientists/MK3eW3As.jpg" alt="Katherine Johnson" />
  </div>
);
```

괄호 없이 작성하면 자바스크립트의 자동 세미콜론 삽입 때문에 `return` 뒤 줄의 코드가 모두 무시된다.

### 컴포넌트 사용하기

`Profile` 컴포넌트를 정의했으면 다른 컴포넌트 안에 중첩할 수 있다.

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

여기서는 대소문자의 차이에 주의해야 한다. `<section>`은 소문자로 시작하므로 React가 HTML 태그로 인식하고, `<Profile />`은 대문자 `P`로 시작하므로 React가 `Profile`이라는 컴포넌트를 사용하려는 것으로 인식한다. 최종적으로 브라우저가 보는 결과는 다음과 같다.

```html
<section>
  <h1>Amazing scientists</h1>
  <img src="https://react.dev/images/docs/scientists/MK3eW3As.jpg" alt="Katherine Johnson" />
  <img src="https://react.dev/images/docs/scientists/MK3eW3As.jpg" alt="Katherine Johnson" />
  <img src="https://react.dev/images/docs/scientists/MK3eW3As.jpg" alt="Katherine Johnson" />
</section>
```

#### 컴포넌트 중첩과 구성

컴포넌트는 일반 자바스크립트 함수이므로 같은 파일에 여러 개를 둘 수 있으며, 파일이 복잡해지면 `Profile`을 별도 파일로 옮겨도 된다. 위 예시에서는 `Gallery`가 `Profile`을 렌더링하므로 `Gallery`가 부모, 각 `Profile`이 자식 컴포넌트다. 한 번 정의한 컴포넌트는 여러 곳에서 원하는 만큼 사용할 수 있지만, 아래처럼 다른 컴포넌트 안에 정의해서는 안 된다.

```js
export default function Gallery() {
  // 다른 컴포넌트 안에 컴포넌트를 정의하면 안 됨!
  function Profile() {
    // ...
  }
  // ...
}
```

이렇게 하면 매우 느리고 버그가 생긴다. 대신 모든 컴포넌트를 최상위 수준에서 정의해야 한다.

```js
export default function Gallery() {
  // ...
}

// 최상위 수준에서 컴포넌트를 선언함
function Profile() {
  // ...
}
```

자식 컴포넌트가 부모의 데이터를 필요로 한다면 정의를 중첩하지 말고 props로 전달한다.

### 처음부터 끝까지 컴포넌트 (Deep Dive)

React 앱은 "루트" 컴포넌트에서 시작하며, 루트 컴포넌트는 보통 새 프로젝트를 시작할 때 자동으로 만들어진다. CodeSandbox에서는 `App.js`에, Next.js 같은 프레임워크에서는 `pages/index.js`에 정의된다.

대부분의 React 앱은 처음부터 끝까지 컴포넌트를 사용한다. 버튼처럼 재사용 가능한 조각뿐 아니라 사이드바, 목록, 완전한 페이지 같은 더 큰 조각도 컴포넌트로 만든다. React 기반 프레임워크는 빈 HTML 파일 대신 React 컴포넌트에서 HTML을 자동 생성하므로, 자바스크립트 코드가 로드되기 전에도 일부 콘텐츠를 보여줄 수 있다.

반면 일부 웹사이트는 기존 HTML 페이지에 상호작용을 추가하는 용도로만 React를 사용한다. 이런 사이트는 페이지 전체에 루트 컴포넌트 하나를 두는 대신 여러 루트 컴포넌트를 가질 수 있다.

## 컴포넌트 가져오기와 내보내기 (Importing and Exporting Components)

> 원문: https://react.dev/learn/importing-and-exporting-components

### 루트 컴포넌트 파일

첫 번째 컴포넌트 예제에서는 `Profile`과 `Gallery` 컴포넌트가 루트 컴포넌트 파일에 함께 있었다.

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

이 예에서 루트 컴포넌트 파일은 `App.js`지만, 설정에 따라 루트 컴포넌트가 다른 파일에 있을 수도 있다. Next.js처럼 파일 기반 라우팅을 사용하는 프레임워크에서는 페이지마다 루트 컴포넌트가 다르다.

### 컴포넌트 내보내기와 가져오기

- 컴포넌트를 이동하는 3단계:
  - 1\. 컴포넌트를 넣을 새 JS 파일을 만듦
  - 2\. 해당 파일에서 함수 컴포넌트를 내보냄 (default 또는 named export 사용)
  - 3\. 컴포넌트를 사용할 파일에서 가져옴 (해당하는 import 방식 사용)

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

`Gallery.js`는 파일 안에서만 쓰는 `Profile` 컴포넌트를 내보내지 않고 정의만 하며, `Gallery` 컴포넌트는 default export로 내보낸다. `App.js`는 `Gallery`를 default import로 가져오고, 루트 `App` 컴포넌트를 default export로 내보낸다. 파일 확장자 `.js`는 생략할 수 있어서 `'./Gallery.js'`와 `'./Gallery'` 모두 동작한다.

### default export vs named export (Deep Dive)

자바스크립트에서 값을 내보내는 주요 방법은 default export와 named export 두 가지다. 한 파일에 default export는 최대 하나만 둘 수 있지만 named export는 원하는 만큼 둘 수 있다. 어떤 방식으로 내보냈는지에 따라 가져오는 방식도 정해진다.

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

한 파일에는 default export를 하나만 둘 수 있으므로, 이미 `Gallery`를 default export로 내보내는 `Gallery.js`에서는 `Profile`을 named export로 내보낸다. named export는 한 파일에 여러 개를 둘 수 있다.

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

`Gallery.js`는 `Profile`을 named export로, `Gallery`를 default export로 내보내고, `App.js`는 `Profile`을 named import로, `Gallery`를 default import로 가져온다. default export와 named export를 혼동하지 않도록 일부 팀은 한 가지 스타일만 사용하거나 한 파일 안에서 두 방식을 섞지 않는다.

## JSX로 마크업 작성하기 (Writing Markup with JSX)

> 원문: https://react.dev/learn/writing-markup-with-jsx

### JSX란

JSX는 자바스크립트 파일 안에서 HTML과 유사한 마크업을 작성하는 문법 확장이다. 사용은 선택 사항이지만, 마크업을 간결하게 작성할 수 있어 대부분의 React 개발자가 선호하고 코드베이스에서도 널리 사용한다.

### JSX: 자바스크립트에 마크업 넣기

웹 개발은 전통적으로 HTML이 콘텐츠를, CSS가 디자인을, 자바스크립트가 로직을 맡도록 관심사를 나눴고, 자바스크립트는 종종 별도 파일에 두었다. 그러나 웹이 점점 상호작용적으로 바뀌면서 콘텐츠를 결정하는 역할도 자바스크립트가 더 많이 맡게 되었다.

그래서 React에서는 렌더링 로직과 마크업이 같은 곳, 즉 컴포넌트에 함께 있다. 버튼의 렌더링 로직과 마크업을 함께 두면 편집할 때마다 둘이 동기화된 상태로 유지된다. 반대로 버튼 마크업과 사이드바 마크업처럼 서로 관련 없는 세부 사항은 격리되므로 어느 쪽을 바꿔도 안전하다.

JSX와 React는 서로 별개이며 독립적으로 사용할 수 있다. JSX는 문법 확장이고, React는 자바스크립트 라이브러리다.

### HTML을 JSX로 변환하기

다음과 같은 기존 HTML이 있다고 하자.

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

이 HTML을 React 컴포넌트에 그대로 복사하면 동작하지 않는다. JSX는 HTML보다 엄격하고 몇 가지 규칙이 더 있기 때문이다. 대부분의 경우 React가 표시하는 에러 메시지가 마크업을 어떻게 고쳐야 할지 알려 준다.

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

빈 태그 `<>`와 `</>`를 Fragment라고 한다. Fragment를 사용하면 브라우저 HTML 트리에 흔적을 남기지 않고 엘리먼트를 묶을 수 있다. Fragment의 props와 주의사항은 [「Fragment」](06_components_and_apis.md#fragment)를 참고한다.

여러 JSX 태그를 이렇게 감싸야 하는 이유는 JSX가 HTML처럼 보여도 내부적으로는 일반 자바스크립트 객체로 변환되기 때문이다. 함수는 두 객체를 배열로 감싸지 않고는 반환할 수 없으므로, 두 JSX 태그도 다른 태그나 Fragment로 감싸야 한다.

#### 2. 모든 태그를 닫아야 함

JSX에서는 태그를 명시적으로 닫아야 한다. `<img>` 같은 자체 닫힘 태그는 `<img />`로, `<li>oranges` 같은 감싸는 태그는 `<li>oranges</li>`로 작성한다.

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

JSX는 자바스크립트로 변환되고, JSX에 작성한 속성은 자바스크립트 객체의 키가 된다. 그런데 자바스크립트 변수 이름에는 대시를 넣거나 예약어를 쓸 수 없다는 제한이 있으므로 속성 이름도 이에 맞춰 바꿔야 한다. 주요 변환은 다음과 같다.

- `stroke-width` -> `strokeWidth`
- `class` -> `className` (`class`가 예약어이므로)

```js
<img
  src="https://react.dev/images/docs/scientists/yXOvdOSs.jpg"
  alt="Hedy Lamarr"
  className="photo"
/>
```

단, 역사적인 이유로 `aria-*`와 `data-*` 속성은 HTML처럼 대시를 사용해 작성한다. 변환할 HTML과 SVG가 많다면 온라인 [HTML-to-JSX 변환기](https://transform.tools/html-to-jsx) 같은 변환 도구로 JSX로 바꿀 수 있다.

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

### 스타일 추가하기

앞의 예시처럼 JSX에서는 `className`으로 CSS 클래스를 지정하며, `className`은 HTML의 `class` 속성과 동일하게 동작한다. CSS 규칙 자체는 별도의 CSS 파일에 작성한다.

```css
.photo {
  border-radius: 50%;
}
```

React 자체는 CSS 파일을 추가하는 방법을 규정하지 않는다. 단순한 구성에서는 HTML에 [`<link>` 태그](https://developer.mozilla.org/en-US/docs/Web/HTML/Element/link)를 추가하면 되고, 빌드 도구나 프레임워크를 사용한다면 해당 문서의 CSS 추가 방법을 따른다.

## JSX에서 중괄호로 자바스크립트 사용하기 (JavaScript in JSX with Curly Braces)

> 원문: https://react.dev/learn/javascript-in-jsx-with-curly-braces

### 따옴표로 문자열 전달하기

JSX에 문자열 속성을 전달할 때는 작은따옴표나 큰따옴표로 감싼다.

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

속성을 동적으로 지정하려면 따옴표 대신 중괄호를 쓴다.

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

`className="avatar"`는 문자열 리터럴 `"avatar"`를 전달하고, `src={avatar}`는 자바스크립트 변수 `avatar`의 값을 읽는다는 점이 다르다.

### 중괄호 사용하기: 자바스크립트 세계로의 창

JSX는 중괄호 `{ }` 안에 자바스크립트를 쓸 수 있게 하므로, 마크업 안에 변수를 직접 넣을 수 있다.

```js
export default function TodoList() {
  const name = 'Gregorio Y. Zara';
  return (
    <h1>{name}'s To Do List</h1>
  );
}
```

함수 호출을 포함해 모든 자바스크립트 표현식이 중괄호 안에서 동작한다.

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
  - 1\. JSX 태그 안의 텍스트로 직접 사용: `<h1>{name}'s To Do List</h1>` -- 동작함
    - `<{tag}>Gregorio Y. Zara's To Do List</{tag}>` -- 동작하지 않음
  - 2\. `=` 기호 바로 뒤의 속성으로 사용: `src={avatar}` -- `avatar` 변수를 읽음
    - `src="{avatar}"` -- 문자열 리터럴 `"{avatar}"`를 전달함

### "이중 중괄호": JSX 안의 CSS와 객체

자바스크립트 객체도 중괄호로 표기하므로, JSX에서 객체를 전달하려면 객체를 또 다른 중괄호 쌍으로 감싼다(`person={{ name: "Hedy Lamarr", inventions: 5 }}`). 인라인 CSS 스타일을 지정할 때 이 형태를 자주 볼 수 있다.

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

`{{ }}`는 특별한 문법이 아니라 JSX 중괄호 안에 들어간 자바스크립트 객체일 뿐이다. 인라인 `style` 속성의 프로퍼티 이름은 camelCase로 작성한다.

- HTML: `<ul style="background-color: black">`
- JSX: `<ul style={{ backgroundColor: 'black' }}>`

`style` 속성은 스타일이 자바스크립트 변수에 의존할 때 유용하다. 속성값 중괄호 안에는 문자열 연결 같은 표현식도 넣을 수 있다.

```js
const user = {
  name: 'Hedy Lamarr',
  imageUrl: 'https://i.imgur.com/yXOvdOSs.jpg',
  imageSize: 90,
};

export default function Profile() {
  return (
    <img
      className="avatar"
      src={user.imageUrl}
      alt={'Photo of ' + user.name}
      style={{
        width: user.imageSize,
        height: user.imageSize
      }}
    />
  );
}
```

### 자바스크립트 객체와 중괄호 활용

여러 표현식을 하나의 객체로 모아 두고 JSX에서 참조할 수도 있다.

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

이처럼 JSX는 데이터와 로직을 자바스크립트로 구성할 수 있게 해 주므로 템플릿 언어로서는 최소한의 기능만 갖는다.

## 컴포넌트에 Props 전달하기 (Passing Props to a Component)

> 원문: https://react.dev/learn/passing-props-to-a-component

### 익숙한 props

props는 JSX 태그에 전달하는 정보다. 예를 들어 `<img>` 태그에 전달하는 `className`, `src`, `alt`, `width`, `height`가 모두 props다.

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

`<img>` 같은 HTML 태그에 전달할 수 있는 props는 ReactDOM이 HTML 표준에 맞춰 미리 정의해 두었다. 반면 직접 만든 컴포넌트에는 어떤 props든 전달할 수 있다.

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

`person={{ }}`의 이중 중괄호는 JSX 중괄호 안에 객체를 넣은 것일 뿐이다.

#### 2단계: 자식 컴포넌트에서 props 읽기

자식 컴포넌트에서는 `function` 뒤의 `({`와 `})` 안에 읽을 props 이름을 쉼표로 구분해 나열한다.

```js
function Avatar({ person, size }) {
  // person과 size를 여기서 사용할 수 있음
}
```

전체 예는 다음과 같다.

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

props를 사용하면 부모 컴포넌트와 자식 컴포넌트를 독립적으로 생각할 수 있다. React 컴포넌트 함수는 `props` 객체 하나를 인수로 받는다.

```js
function Avatar(props) {
  let person = props.person;
  let size = props.size;
  // ...
}
```

보통은 `props` 객체 전체를 받지 않고 구조 분해로 필요한 값만 꺼낸다.

```js
function Avatar({ person, size }) {
  // ...
}
```

props를 선언할 때 `( )` 안의 `{ }` 중괄호 쌍을 빠뜨리면 안 된다. 이 문법을 "구조 분해(destructuring)"라고 한다.

### prop의 기본값 지정하기

prop에 기본값을 지정하려면 구조 분해한 파라미터 뒤에 `=`와 기본값을 붙인다.

```js
function Avatar({ person, size = 100 }) {
  // ...
}
```

이렇게 지정하면 `<Avatar person={...} />`처럼 `size`를 생략하거나 `undefined`를 전달했을 때 `100`이 사용된다. 기본값은 이 두 경우에만 적용되므로, `size={null}`이나 `size={0}`을 전달하면 해당 값이 그대로 사용된다.

### JSX 전개 구문으로 props 전달하기

다음처럼 받은 props를 그대로 자식에게 넘기느라 코드가 반복될 때가 있다.

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

이때는 전개 구문으로 간결하게 작성할 수 있다.

```js
function Profile(props) {
  return (
    <div className="card">
      <Avatar {...props} />
    </div>
  );
}
```

다만 전개 구문은 절제해서 사용해야 한다. 모든 컴포넌트에서 전개 구문을 쓰고 있다면 무언가 잘못된 것이며, 대개 컴포넌트를 분리하고 children으로 JSX를 전달해야 한다는 신호다.

### children으로 JSX 전달하기

내장 브라우저 태그를 중첩하듯이 직접 만든 컴포넌트도 중첩할 수 있다.

```js
<Card>
  <Avatar />
</Card>
```

JSX 태그 안에 콘텐츠를 중첩하면 부모 컴포넌트는 그 콘텐츠를 `children`이라는 prop으로 받는다.

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

`children` prop을 받으면 컴포넌트 내부에 부모가 원하는 JSX를 넣을 자리를 마련할 수 있다. 그래서 패널이나 그리드처럼 다른 콘텐츠를 감싸는 컴포넌트에서 이 패턴을 자주 사용한다.

### props가 시간에 따라 변하는 방식

컴포넌트는 시간에 따라 다른 props를 받을 수 있다. 즉 props는 정적인 값이 아니라 특정 시점의 컴포넌트 데이터를 반영한다.

하지만 props 자체는 불변(immutable)이다. 컴포넌트의 props가 바뀌어야 한다면 부모 컴포넌트에 다른 props, 즉 새 객체를 전달하도록 "요청"해야 한다. 그러면 이전 props는 버려지고 결국 가비지 컬렉션된다.

따라서 props를 직접 "변경"하려 해서는 안 된다. 사용자 입력에 반응해야 할 때는 state를 사용한다.

## 조건부 렌더링 (Conditional Rendering)

> 원문: https://react.dev/learn/conditional-rendering

### 조건부로 JSX 반환하기

컴포넌트는 조건에 따라 서로 다른 것을 표시해야 할 때가 많다. React에서는 `if` 문, `&&`, `? :` 연산자 같은 자바스크립트 문법으로 JSX를 조건부로 렌더링할 수 있다.

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

이 코드는 `isPacked` prop이 `true`일 때 다른 JSX 트리를 반환한다. 이처럼 React에서는 조건 같은 제어 흐름을 자바스크립트로 처리한다.

#### null로 아무것도 반환하지 않기

어떤 상황에서는 아무것도 렌더링하고 싶지 않을 수 있다. 그래도 컴포넌트는 무언가를 반환해야 하므로 이때는 `null`을 반환한다.

```js
if (isPacked) {
  return null;
}
return <li className="item">{name}</li>;
```

다만 컴포넌트에서 `null`을 반환하는 방식은 흔하지 않다. 그 컴포넌트를 렌더링하려는 개발자가 예상치 못한 결과에 놀랄 수 있기 때문이다. 그보다는 부모 컴포넌트의 JSX에서 컴포넌트를 조건부로 포함하거나 제외하는 경우가 더 많다.

### 조건부로 JSX 포함하기

if/else를 사용하면 렌더 출력에 중복이 생기기 쉽다.

```js
if (isPacked) {
  return <li className="item">{name} ✅</li>;
}
return <li className="item">{name}</li>;
```

두 분기 모두 `<li className="item">...</li>`를 반환한다. 이런 중복을 피하려면 달라지는 일부 JSX만 조건부로 포함하면 된다.

#### 조건(삼항) 연산자 (`? :`)

```js
return (
  <li className="item">
    {isPacked ? name + ' ✅' : name}
  </li>
);
```

이 코드는 "`isPacked`가 true이면 `name + ' ✅'`를 렌더링하고, 그렇지 않으면 `name`을 렌더링한다"로 읽을 수 있다. `if` 문과 달리 삼항 연산자는 표현식이므로 JSX 중괄호 안에서 바로 쓸 수 있다.

앞의 if/else 예제와 이 예제는 완전히 동등하다. JSX 엘리먼트는 내부 상태를 가진 "인스턴스"도, 실제 DOM 노드도 아니며, 청사진 같은 가벼운 설명일 뿐이기 때문이다.

다음처럼 더 복잡하게 중첩할 수도 있다.

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

삼항 연산자는 단순한 조건에서 잘 동작하지만 적당히 사용해야 한다. 중첩된 조건부 마크업이 너무 많아지면 자식 컴포넌트를 추출해 정리하는 것을 고려한다.

#### 논리 AND 연산자 (`&&`)

`&&`는 조건이 true일 때 JSX를 렌더링하고, 그렇지 않으면 아무것도 렌더링하지 않으려 할 때 사용한다.

```js
return (
  <li className="item">
    {name} {isPacked && '✅'}
  </li>
);
```

이 코드는 "`isPacked`이면 체크마크를 렌더링하고, 그렇지 않으면 아무것도 렌더링하지 않는다"로 읽을 수 있다. 자바스크립트의 `&&` 표현식은 왼쪽이 `true`이면 오른쪽 값을 반환하고, 조건이 `false`이면 전체 표현식이 `false`가 된다. React는 `false`를 JSX 트리의 "구멍"으로 보고 아무것도 렌더링하지 않는다.

숫자를 조건으로 쓸 때는 `0`에 주의해야 한다. `messageCount && <p>New messages</p>`에서 `messageCount`가 `0`이면 전체 표현식의 값도 `0`이므로, React는 화면에 `0`을 표시한다. 아무것도 표시하지 않으려면 `messageCount > 0 && <p>New messages</p>`처럼 왼쪽을 boolean 조건으로 만든다.

#### 변수에 조건부로 JSX 할당하기

단축 구문이 복잡해지면 `if` 문과 변수를 사용한다.

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

가장 장황하지만 가장 유연한 방식이며, 텍스트뿐 아니라 임의의 JSX에도 쓸 수 있다.

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

지금까지 본 `if`, `? :`, `&&` 방식은 자식 콘텐츠뿐 아니라 속성값을 조건부로 지정할 때도 똑같이 동작한다.

## 리스트 렌더링 (Rendering Lists)

> 원문: https://react.dev/learn/rendering-lists

### 배열에서 데이터 렌더링하기

데이터 컬렉션으로 비슷한 컴포넌트 여러 개를 표시하고 싶을 때는 자바스크립트의 `filter()`와 `map()`을 React와 함께 사용한다. 일반 `for` 문으로 JSX 배열을 채워도 되지만, 이 절에서는 주로 `map()`을 사용한다.

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

이 리스트 항목들은 내용, 즉 데이터만 다르다. 이런 데이터를 자바스크립트 객체와 배열에 저장해 두면 `map()`과 `filter()`로 컴포넌트 리스트를 렌더링할 수 있다.

#### 3단계 프로세스

- 1\. 데이터를 배열로 옮기기:

```js
const people = [
  'Creola Katherine Johnson: mathematician',
  'Mario Jose Molina-Pasquel Henriquez: chemist',
  'Mohammad Abdus Salam: physicist',
  'Percy Lavon Julian: chemist',
  'Subrahmanyan Chandrasekhar: astrophysicist'
];
```

- 2\. 배열 멤버를 JSX 노드의 새 배열로 map하기:

```js
const listItems = people.map(person => <li>{person}</li>);
```

- 3\. `<ul>`로 감싸서 listItems를 반환하기:

```js
return <ul>{listItems}</ul>;
```

전체 예는 다음과 같다.

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

이 코드를 실행하면 콘솔에 `Warning: Each child in a list should have a unique "key" prop.` 경고가 표시된다.

### 배열 항목 필터링하기

데이터를 다음처럼 객체로 구조화할 수도 있다.

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

`filter()`를 사용하면 특정 조건에 맞는 항목만 표시할 수 있다.

```js
const chemists = people.filter(person =>
  person.profession === 'chemist'
);
```

필터링한 배열을 다시 `map()`으로 렌더링하면 다음과 같다.

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

화살표 함수는 `=>` 바로 뒤의 표현식을 암묵적으로 반환한다.

```js
const listItems = chemists.map(person =>
  <li>...</li> // 암묵적 반환!
);
```

하지만 `=>` 뒤에 `{` 중괄호가 오면 `return`을 명시적으로 작성해야 한다.

```js
const listItems = chemists.map(person => { // 중괄호
  return <li>...</li>;
});
```

`=> {`로 시작하는 화살표 함수는 "블록 본문"을 가지므로 명시적인 `return` 문이 필요하다. `return`을 빠뜨리면 아무것도 반환되지 않는다.

### key로 리스트 항목 순서 유지하기

각 배열 항목에는 형제 항목 사이에서 그 항목을 고유하게 식별하는 문자열 또는 숫자인 `key`를 부여해야 한다.

```js
<li key={person.id}>...</li>
```

`map()` 호출 안에서 직접 반환하는 JSX 엘리먼트에는 항상 key가 필요하다.

#### key가 중요한 이유

key는 각 배열 항목이 어느 컴포넌트에 대응하는지 React에 알려준다. 항목이 이동하거나 삽입, 삭제되어도 DOM을 올바르게 업데이트하려면 위치와 무관하게 항목을 식별할 수 있어야 하므로, key는 즉석에서 생성하지 않고 데이터에 포함시키는 것이 좋다. 파일을 이름 없이 순서로만 구분하면 하나를 삭제했을 때 나머지 순서가 바뀌어 혼란스러운 것처럼, 리스트에서도 위치가 바뀌어도 유지되는 식별자가 필요하다.

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

각 항목이 여러 DOM 노드를 렌더링해야 한다면 Fragment로 묶을 수 있다. 다만 `<>...</>` 약칭은 `key`를 받지 못하므로 `<Fragment key={person.id}>`처럼 명시적으로 써야 한다. Fragment는 DOM에서 사라지므로, 래퍼 div 없이 `<h1>`, `<p>`, `<h1>`, `<p>`처럼 평평한 엘리먼트 목록이 만들어진다. 예제는 [「리스트에서 키 사용」](06_components_and_apis.md#리스트에서-키-사용)을 참고한다.

### key를 얻는 곳

- 데이터베이스에서: 본질적으로 고유한 데이터베이스 키/ID를 사용함
- 로컬 생성 데이터에서: 증가하는 카운터, `crypto.randomUUID()`, 또는 `uuid` 패키지를 사용함

### key의 규칙

- 1\. key는 형제 간에 고유해야 함 -- 다른 배열의 JSX 노드에서는 같은 key를 사용해도 됨
- 2\. key는 변경되면 안 됨 -- 렌더링 중에 생성하면 안 됨

### React에 key가 필요한 이유

key는 형제 사이에서 항목을 고유하게 식별하므로 위치보다 더 많은 정보를 준다. 재정렬로 위치가 바뀌어도 React는 key를 통해 항목의 생애 내내 같은 항목을 식별할 수 있다.

### 인덱스를 key로 사용하면 안 되는 이유

- 항목이 삽입, 삭제되거나 배열이 재정렬되면 인덱스가 바뀌어 React가 컴포넌트 상태를 추적하지 못함
- `key={Math.random()}`으로 즉석 생성하면 렌더 간에 key가 일치하지 않아 모든 컴포넌트와 DOM이 매번 재생성됨
  - 느리고 리스트 항목 안의 사용자 입력이 손실됨
- 대신 데이터 기반의 안정적인 ID를 사용해야 함

컴포넌트는 `key`를 prop으로 받지 않는다. `key`는 React가 힌트로만 사용하므로, 컴포넌트 안에서 ID가 필요하면 별도 prop으로 전달해야 한다.

```js
<Profile key={id} userId={id} />
```

## 컴포넌트를 순수하게 유지하기 (Keeping Components Pure)

> 원문: https://react.dev/learn/keeping-components-pure

### 순수성: 공식으로서의 컴포넌트

#### 순수 함수의 정의

- 순수 함수는 두 가지 특성을 가짐:
  - 1\. 자기 일에만 신경 씀 -- 호출 전에 존재하던 객체나 변수를 변경하지 않음
  - 2\. 같은 입력, 같은 출력 -- 같은 입력이 주어지면 항상 같은 결과를 반환함

- 수학 공식 예: y = 2x
  - x = 2이면 y = 4 (항상)
  - x = 3이면 y = 6 (항상)

```js
function double(number) {
  return 2 * number;
}
```

`double`은 `3`을 전달하면 항상 `6`을 반환하는 순수 함수다. 이처럼 같은 입력에 항상 같은 결과를 내는 성질을 멱등성(idempotency)이라고 한다.

#### React와 순수 컴포넌트

React는 작성하는 모든 컴포넌트가 순수 함수라고 가정한다. 따라서 React 컴포넌트는 같은 입력(props, state, context)이 주어지면 항상 같은 JSX를 반환해야 한다. 이 규칙은 컴포넌트뿐 아니라 Hook에도, 렌더 중에 실행되는 모든 코드에도 똑같이 적용된다.

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

`Recipe`에 `drinkers={2}`를 전달하면 항상 `2 cups of water`를 포함하는 JSX를, `drinkers={4}`를 전달하면 항상 `4 cups of water`를 포함하는 JSX를 반환한다. 컴포넌트는 레시피와 비슷하다. 요리 도중 새 재료를 넣지 않고 레시피를 따르면 매번 같은 요리가 나온다.

반대로 호출할 때마다 다른 값을 돌려주는 함수를 렌더 중에 부르면 컴포넌트가 멱등성을 잃는다.

```js
function Clock() {
  const time = new Date(); // 나쁨: 호출할 때마다 다른 결과를 반환함!
  return <span>{time.toLocaleString()}</span>
}
```

`new Date()`와 `Math.random()`은 호출할 때마다 다른 값을 반환한다. 이런 함수를 아예 쓰지 말라는 것이 아니라 렌더 중에 쓰지 말라는 뜻이다. 최신 날짜가 필요하면 Effect로 동기화한다.

```js
import { useState, useEffect } from 'react';

function useTime() {
  const [time, setTime] = useState(() => new Date());

  useEffect(() => {
    const id = setInterval(() => {
      setTime(new Date());
    }, 1000);
    return () => clearInterval(id);
  }, []);

  return time;
}

export default function Clock() {
  const time = useTime();
  return <span>{time.toLocaleString()}</span>;
}
```

### 부작용(Side Effects): 의도하지 않은 결과

React의 렌더링 과정은 항상 순수해야 한다. 컴포넌트는 JSX를 반환하기만 해야 하며, 렌더링 전에 존재하던 객체나 변수를 변경해서는 안 된다.

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

이 컴포넌트는 외부에 선언된 `guest` 변수를 읽고 쓰기 때문에, 호출할 때마다 다른 JSX를 생성해 결과를 예측할 수 없다.

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

이제 반환되는 JSX가 `guest` prop에만 의존하므로 컴포넌트가 순수하다.

#### 핵심 원칙

- 컴포넌트는 서로의 렌더링 순서에 의존해서는 안 됨
- 각 컴포넌트는 "스스로 생각해야" 하며 렌더링 중에 다른 컴포넌트와 조율하려 해서는 안 됨
- 렌더링은 학교 시험과 같음: 각 컴포넌트가 스스로 JSX를 계산해야 함

### 렌더 중에 실행되는 코드 구분하기

React는 먼저 컴포넌트를 렌더링해 다음 UI가 어떻게 보여야 하는지 계산하고, 이전 UI와 비교해 필요한 최소한의 DOM 변경만 커밋한 뒤, Effect를 실행한다. 이 과정은 [「렌더와 커밋」](02_adding_interactivity.md#렌더와-커밋-render-and-commit)에서 자세히 다룬다. 순수해야 하는 것은 이 중 렌더 단계에서 실행되는 코드다.

컴포넌트 최상위에 작성한 코드는 렌더 중에 실행될 가능성이 높다.

```js
function Dropdown() {
  const selectedItems = new Set(); // 렌더 중에 생성됨
  // ...
}
```

반면 이벤트 핸들러와 Effect 안의 코드는 렌더 중에 실행되지 않는다.

```js
function Dropdown() {
  const selectedItems = new Set();
  const onSelect = (item) => {
    // 이벤트 핸들러 안이므로 사용자가 이벤트를 일으킬 때만 실행됨
    selectedItems.add(item);
  }
}
```

```js
function Dropdown() {
  const selectedItems = new Set();
  useEffect(() => {
    // Effect 안이므로 렌더링이 끝난 뒤에만 실행됨
    logForAnalytics(selectedItems);
  }, [selectedItems]);
}
```

### 렌더링 입력은 읽기 전용

렌더링 중에 읽을 수 있는 입력은 다음 세 가지다.

- 1\. Props
- 2\. State
- 3\. Context

이 입력들은 항상 읽기 전용으로 취급해야 한다. props와 state는 한 번의 렌더에 대한 불변 스냅샷이므로, 바꾸고 싶다면 새 props를 전달하거나 state setter를 호출한다. 사용자 입력에 대응해 무언가를 바꾸고 싶을 때도 변수에 쓰는 대신 state를 설정한다.

```js
function Post({ item }) {
  item.url = new Url(item.url, base); // 나쁨: props를 직접 변이하면 안 됨
  return <Link url={item.url}>{item.title}</Link>;
}
```

```js
function Post({ item }) {
  const url = new Url(item.url, base); // 좋음: 대신 복사본을 만듦
  return <Link url={url}>{item.title}</Link>;
}
```

#### Hook의 인수와 반환값도 불변임

JSX의 props처럼 Hook에 전달한 값도 불변이 되므로 수정하면 안 된다.

```js
function useIconStyle(icon) {
  const theme = useContext(ThemeContext);
  if (icon.enabled) {
    icon.className = computeStyle(icon, theme); // 나쁨: Hook 인수를 직접 변이하면 안 됨
  }
  return icon;
}
```

```js
function useIconStyle(icon) {
  const theme = useContext(ThemeContext);
  const newIcon = { ...icon }; // 좋음: 대신 복사본을 만듦
  if (icon.enabled) {
    newIcon.className = computeStyle(icon, theme);
  }
  return newIcon;
}
```

Hook의 인수를 변이하면 커스텀 Hook의 메모이제이션이 올바르게 동작하지 않는다.

```js
style = useIconStyle(icon);         // `style`은 `icon`을 기준으로 메모이제이션됨
icon.enabled = false;               // 나쁨: Hook 인수를 직접 변이하면 안 됨
style = useIconStyle(icon);         // 이전에 메모이제이션된 결과가 반환됨
```

```js
style = useIconStyle(icon);         // `style`은 `icon`을 기준으로 메모이제이션됨
icon = { ...icon, enabled: false }; // 좋음: 대신 복사본을 만듦
style = useIconStyle(icon);         // `style`의 새 값이 계산됨
```

#### JSX에 전달한 값은 변이하지 않음

JSX에 사용한 값은 그 뒤에 변이하면 안 된다. 다른 값이 필요하면 새 값을 만든다.

```js
function Page({ colour }) {
  const styles = { colour, size: "large" };
  const header = <Header styles={styles} />;
  styles.size = "small"; // 나쁨: styles는 이미 위의 JSX에 사용됨
  const footer = <Footer styles={styles} />;
  return (
    <>
      {header}
      <Content />
      {footer}
    </>
  );
}
```

```js
function Page({ colour }) {
  const headerStyles = { colour, size: "large" };
  const header = <Header styles={headerStyles} />;
  const footerStyles = { colour, size: "small" }; // 좋음: 새 값을 만듦
  const footer = <Footer styles={footerStyles} />;
  return (
    <>
      {header}
      <Content />
      {footer}
    </>
  );
}
```

### StrictMode로 비순수 계산 감지하기

React의 Strict Mode는 개발 중에 각 컴포넌트 함수를 두 번 호출해 순수성 규칙을 깨는 컴포넌트를 찾아낸다. 앞의 비순수 `Cup` 예제가 "Guest #1", "Guest #2", "Guest #3" 대신 "Guest #2", "Guest #4", "Guest #6"을 표시한 것도 이 때문이다. 반면 순수 함수는 계산만 하므로 두 번 호출해도 결과가 달라지지 않는다. 활성화 방법과 두 번 호출되는 함수 목록은 [「StrictMode」](06_components_and_apis.md#strictmode)를 참고한다.

### 로컬 변이: 컴포넌트의 작은 비밀

이미 존재하는 변수를 변이하는 것은 비순수하다. 그러나 렌더링 중에 방금 생성한 변수와 객체를 변경하는 것은 전혀 문제가 없다.

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

`cups` 변수와 배열은 `TeaGathering`을 실행하는 동안 새로 생성되므로, 여기에 항목을 추가해도 함수 외부의 코드에는 영향을 주지 않는다. 이렇게 이번 렌더링에서 만든 값만 변경하는 것을 "로컬 변이(local mutation)"라고 한다.

다른 컴포넌트에 영향을 주지 않는다면 렌더 중 지연 초기화도 허용된다.

```js
function ExpenseForm() {
  SuperCalculator.initializeIfNotReady(); // 좋음: 다른 컴포넌트에 영향을 주지 않는 경우
  // 렌더링을 계속함...
}
```

### 부작용을 둘 수 있는 곳

함수형 프로그래밍은 순수성에 크게 의존하지만, 결국 어딘가에서는 무언가가 변해야 한다. 이런 변화를 부작용(side effects)이라고 하며, 부작용은 렌더링 중이 아니라 렌더링 "곁에서" 일어난다. React는 더 나은 사용자 경험을 위해 컴포넌트를 여러 번 렌더링할 수 있으므로 부작용이 렌더 중에 실행되면 안 된다. 부작용에는 다음이 포함된다.

- 화면 업데이트
- 애니메이션 시작
- 데이터 변경

특히 사용자에게 직접 보이는 부작용, 예를 들어 DOM 변경은 렌더 로직에서 허용되지 않는다.

```js
function ProductDetailPage({ product }) {
  document.title = product.title; // 나쁨: DOM을 변경함
}
```

#### 1. 이벤트 핸들러 (권장)

이벤트 핸들러는 사용자가 버튼 클릭 같은 동작을 할 때 React가 실행하는 함수다. 컴포넌트 안에 정의되지만 렌더링 중에는 실행되지 않으므로 순수할 필요가 없다.

#### 2. useEffect (최후의 수단)

적절한 이벤트 핸들러를 찾을 수 없다면 `useEffect`로 부작용을 붙일 수 있다. `useEffect`는 렌더링이 끝난 뒤 해당 코드를 실행하도록 React에 지시하며, 그 시점에는 부작용이 허용된다. 다만 가능하면 렌더링만으로 로직을 표현하는 것이 가장 좋다.

### React가 순수성을 중시하는 이유

순수 함수는 계산만 수행하므로 코드를 이해하고 디버깅하기 쉬우며, React가 컴포넌트와 Hook을 자동으로 올바르게 최적화할 수 있다.

- 1\. 서버 렌더링 -- 컴포넌트가 같은 입력에 같은 결과를 반환하므로 하나의 컴포넌트가 많은 사용자 요청을 처리할 수 있음
- 2\. 성능 최적화 -- 입력이 변하지 않은 컴포넌트의 렌더링을 건너뛸 수 있음
  - 순수 함수는 항상 같은 결과를 반환하므로 캐싱해도 안전함
- 3\. 안전한 렌더링 재시작 -- 깊은 컴포넌트 트리를 렌더링하는 중에 데이터가 변하면 React는 오래된 렌더를 낭비하지 않고 렌더링을 다시 시작할 수 있음
  - 순수성 덕분에 언제든 계산을 중단하거나 여러 번 실행해도 안전함
  - 어떤 업데이트를 사용자에게 먼저 보여 줄지 우선순위를 정할 수 있음
- 4\. 미래 React 기능 -- 데이터 가져오기, 애니메이션, 성능까지 모든 새 React 기능이 순수성을 활용함

## UI를 트리로 이해하기 (Understanding Your UI as a Tree)

> 원문: https://react.dev/learn/understanding-your-ui-as-a-tree

### 트리로서의 UI

트리는 항목 간의 관계를 나타내는 모델이며, UI도 트리 구조로 표현하는 경우가 많다. 브라우저는 HTML(DOM)과 CSS(CSSOM)를 트리 구조로 모델링하고, 모바일 플랫폼도 뷰 계층 구조를 트리로 표현한다.

React 역시 컴포넌트로부터 UI 트리를 만든다. 이 트리는 React 앱에서 데이터가 흐르는 방식을 이해하고 렌더링과 앱 크기를 최적화하는 데 유용한 도구다.

### 렌더 트리 (The Render Tree)

컴포넌트의 주요 기능 중 하나는 다른 컴포넌트를 조합할 수 있다는 점이다. 컴포넌트를 중첩하면 부모 컴포넌트와 자식 컴포넌트라는 개념이 생기며, 각 부모 컴포넌트도 다른 컴포넌트의 자식일 수 있다. React 앱을 렌더링할 때 이 관계를 트리로 모델링할 수 있는데, 이를 렌더 트리라고 한다.

다음 예제 앱으로 렌더 트리를 살펴보자.

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

React는 렌더링된 컴포넌트로 구성된 UI 트리, 즉 렌더 트리를 만든다. 트리는 노드로 이루어지며 각 노드는 컴포넌트 하나를 나타낸다. 렌더 트리의 루트 노드는 앱의 루트 컴포넌트이고, 트리의 각 화살표는 부모 컴포넌트에서 자식 컴포넌트를 가리킨다. 예제 앱의 구조는 다음과 같다.

- App (루트)
  - FancyText
  - InspirationGenerator
    - FancyText
    - Copyright

#### HTML 태그는 렌더 트리에 어디 있는가 (Deep Dive)

렌더 트리에는 각 컴포넌트가 렌더링하는 HTML 태그가 나오지 않는다. 렌더 트리는 React 컴포넌트로만 구성되기 때문이다.

React는 UI 프레임워크로서 특정 플랫폼에 얽매이지 않는다. react.dev는 HTML 마크업을 UI 프리미티브로 사용하는 웹 예제를 보여주지만, React 앱은 UIView(iOS)나 FrameworkElement(Windows) 같은 다른 UI 프리미티브를 사용하는 모바일이나 데스크탑 플랫폼에서도 렌더링할 수 있다. 이런 플랫폼 UI 프리미티브는 React의 일부가 아니므로, React 렌더 트리는 앱이 어떤 플랫폼에서 렌더링되든 React 앱을 이해하는 데 도움을 준다.

### 조건부 렌더링과 렌더 트리

렌더 트리는 React 앱의 렌더 패스 한 번을 나타낸다. 조건부 렌더링을 사용하면 부모 컴포넌트가 전달받은 데이터에 따라 다른 자식을 렌더링할 수 있으므로, 렌더 패스마다 트리가 달라질 수 있다. 다음은 조건부 렌더링을 추가한 예다.

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

이 예제는 `inspiration.type`에 따라 `<FancyText>` 또는 `<Color>`를 렌더링하므로, 렌더 패스마다 렌더 트리가 달라질 수 있다.

그래도 이 트리들은 대체로 React 앱의 최상위(top-level) 컴포넌트와 리프(leaf) 컴포넌트를 식별하는 데 유용하다.

- 최상위 컴포넌트: 루트 컴포넌트에 가장 가까운 컴포넌트로, 그 아래 모든 컴포넌트의 렌더링 성능에 영향을 미치며 가장 복잡한 경우가 많음
- 리프 컴포넌트: 트리 하단에 위치하며 자식 컴포넌트가 없고 자주 재렌더링됨

이런 분류는 앱의 데이터 흐름과 성능을 이해하는 데 도움이 된다.

### 모듈 의존성 트리 (The Module Dependency Tree)

React 앱에서 트리로 모델링할 수 있는 또 다른 관계는 모듈 의존성이다. 컴포넌트와 로직을 별도 파일로 분리하면 컴포넌트, 함수, 상수를 내보낼 수 있는 JS 모듈이 만들어진다. 모듈 의존성 트리의 각 노드는 모듈이고, 각 가지는 그 모듈의 `import` 문을 나타낸다.

#### 모듈 의존성 트리 구조

앞의 예제 앱을 모듈 의존성 트리로 나타내면 다음과 같다.

- App.js (루트)
  - InspirationGenerator.js를 import함
  - FancyText.js를 import함
  - Copyright.js를 import함
- InspirationGenerator.js
  - FancyText.js를 import함
  - Color.js를 import함
  - inspirations.js를 import함

#### 렌더 트리와 모듈 의존성 트리 비교

모듈 의존성 트리의 노드는 모듈을 나타내므로 `inspirations.js`처럼 컴포넌트가 아닌 모듈도 포함한다. 컴포넌트만 포함하는 렌더 트리와는 노드 간 관계도 다르다. 예를 들어 `Copyright.js`를 import하는 곳은 `App.js`이므로 의존성 트리에서는 그 아래에 놓인다. 하지만 `InspirationGenerator`가 `children` prop으로 받은 JSX를 렌더링하므로, 렌더 트리의 `Copyright`은 `InspirationGenerator`의 자식으로 나타난다.

#### 의존성 트리와 빌드 도구

프로덕션 빌드에서 번들러는 의존성 트리를 따라 앱 실행에 필요한 모듈을 결정하고, 클라이언트에 전달할 자바스크립트를 묶는다. 앱이 성장하면서 번들이 커지면 다운로드와 실행 비용이 늘어 UI가 늦게 그려질 수 있다. 이때 의존성 트리를 살펴보면 어떤 모듈이 번들에 포함되는지 파악하고 지연 원인을 찾는 데 도움이 된다.

# JSX

[JSX](https://facebook.github.io/jsx/)는 임베드 가능한 XML 유사 구문입니다.
유효한 JavaScript로 변환되도록 의도되었지만, 해당 변환의 의미는 구현에 따라 다릅니다.
JSX는 [React](https://reactjs.org/) 프레임워크와 함께 인기를 얻었지만, 이후 다른 구현도 등장했습니다.
TypeScript는 JSX를 직접 JavaScript에 임베딩하고, 타입 검사하고, 컴파일하는 것을 지원합니다.

## 기본 사용법

JSX를 사용하려면 두 가지를 해야 합니다.

1. 파일 이름을 `.tsx` 확장자로 지정
2. [`jsx`](/tsconfig#jsx) 옵션 활성화

TypeScript는 여러 JSX 모드와 함께 제공됩니다: `preserve`, `react`(클래식 런타임), `react-jsx`(자동 런타임), `react-jsxdev`(자동 개발 런타임), 그리고 `react-native`.
`preserve` 모드는 다른 변환 단계(예: [Babel](https://babeljs.io/))에서 추가로 소비할 수 있도록 JSX를 출력의 일부로 유지합니다.
추가로 출력은 `.jsx` 파일 확장자를 갖습니다.
`react` 모드는 `React.createElement`를 방출하고, 사용 전에 JSX 변환을 거칠 필요가 없으며, 출력은 `.js` 파일 확장자를 갖습니다.
`react-native` 모드는 모든 JSX를 유지한다는 점에서 `preserve`와 동등하지만, 출력은 대신 `.js` 파일 확장자를 갖습니다.

| 모드           | 입력      | 출력                                              | 출력 파일 확장자 |
| -------------- | --------- | ------------------------------------------------- | ---------------- |
| `preserve`     | `<div />` | `<div />`                                         | `.jsx`           |
| `react`        | `<div />` | `React.createElement("div")`                      | `.js`            |
| `react-native` | `<div />` | `<div />`                                         | `.js`            |
| `react-jsx`    | `<div />` | `_jsx("div", {}, void 0);`                        | `.js`            |
| `react-jsxdev` | `<div />` | `_jsxDEV("div", {}, void 0, false, {...}, this);` | `.js`            |

[`jsx`](/tsconfig#jsx) 커맨드 라인 플래그 또는 [tsconfig.json 파일의 해당 `jsx` 옵션](/tsconfig#jsx)을 사용하여 이 모드를 지정할 수 있습니다.

> \*참고: react JSX 방출을 대상으로 할 때 사용할 JSX 팩토리 함수는 [`jsxFactory`](/tsconfig#jsxFactory) 옵션으로 지정할 수 있습니다(기본값은 `React.createElement`)

## `as` 연산자

타입 단언을 작성하는 방법을 떠올려 보세요:

```ts
const foo = <Foo>bar;
```

이것은 변수 `bar`가 `Foo` 타입을 갖도록 단언합니다.
TypeScript도 타입 단언에 꺾쇠 괄호를 사용하므로, JSX 구문과 결합하면 특정 파싱 어려움이 발생합니다. 결과적으로, TypeScript는 `.tsx` 파일에서 꺾쇠 괄호 타입 단언을 허용하지 않습니다.

위 구문은 `.tsx` 파일에서 사용할 수 없으므로, 대체 타입 단언 연산자를 사용해야 합니다: `as`.
예제는 `as` 연산자로 쉽게 다시 작성할 수 있습니다.

```ts
const foo = bar as Foo;
```

`as` 연산자는 `.ts`와 `.tsx` 파일 모두에서 사용할 수 있으며, 꺾쇠 괄호 타입 단언 스타일과 동작이 동일합니다.

## 타입 검사

JSX로 타입 검사를 이해하려면, 먼저 내재 요소와 값 기반 요소의 차이를 이해해야 합니다.
JSX 표현식 `<expr />`이 주어지면, `expr`은 환경에 내재된 것(예: DOM 환경의 `div` 또는 `span`)을 참조하거나 여러분이 만든 커스텀 컴포넌트를 참조할 수 있습니다.
이것은 두 가지 이유로 중요합니다:

1. React의 경우, 내재 요소는 문자열로 방출되지만(`React.createElement("div")`), 여러분이 만든 컴포넌트는 그렇지 않습니다(`React.createElement(MyComponent)`).
2. JSX 요소에 전달되는 속성 타입은 다르게 조회되어야 합니다.
   내재 요소 속성은 _내재적으로_ 알려져야 하는 반면, 컴포넌트는 자체 속성 세트를 지정하고 싶을 것입니다.

TypeScript는 [React가 하는 것과 같은 규칙](http://facebook.github.io/react/docs/jsx-in-depth.html#html-tags-vs.-react-components)을 사용하여 이들을 구별합니다.
내재 요소는 항상 소문자로 시작하고, 값 기반 요소는 항상 대문자로 시작합니다.

### `JSX` 네임스페이스

TypeScript의 JSX는 `JSX` 네임스페이스에 의해 타입이 지정됩니다. `JSX` 네임스페이스는 `jsx` 컴파일러 옵션에 따라 다양한 위치에 정의될 수 있습니다.

`jsx` 옵션 `preserve`, `react`, `react-native`는 클래식 런타임에 대한 타입 정의를 사용합니다. 이것은 `jsxFactory` 컴파일러 옵션에 의해 결정되는 변수가 스코프에 있어야 함을 의미합니다. `JSX` 네임스페이스는 JSX 팩토리의 최상위 식별자에 지정되어야 합니다. 예를 들어, React는 기본 팩토리 `React.createElement`를 사용합니다. 이것은 `JSX` 네임스페이스가 `React.JSX`로 정의되어야 함을 의미합니다.

```ts
export function createElement(): any;

export namespace JSX {
  // ...
}
```

그리고 사용자는 항상 React를 `React`로 임포트해야 합니다.

```ts
import * as React from 'react';
```

Preact는 JSX 팩토리 `h`를 사용합니다. 이것은 타입이 `h.JSX`로 정의되어야 함을 의미합니다.

```ts
export function h(props: any): any;

export namespace h.JSX {
  // ...
}
```

사용자는 `h`를 임포트하기 위해 명명된 임포트를 사용해야 합니다.

```ts
import { h } from 'preact';
```

`jsx` 옵션 `react-jsx`와 `react-jsxdev`의 경우, `JSX` 네임스페이스는 일치하는 진입점에서 내보내져야 합니다. `react-jsx`의 경우 이것은 `${jsxImportSource}/jsx-runtime`입니다. `react-jsxdev`의 경우 이것은 `${jsxImportSource}/jsx-dev-runtime`입니다. 이들은 파일 확장자를 사용하지 않으므로, ESM 사용자를 지원하려면 `package.json`의 [`exports`](https://nodejs.org/api/packages.html#exports) 필드 맵을 사용해야 합니다.

```json
{
  "exports": {
    "./jsx-runtime": "./jsx-runtime.js",
    "./jsx-dev-runtime": "./jsx-dev-runtime.js",
  }
}
```

그런 다음 `jsx-runtime.d.ts`와 `jsx-dev-runtime.d.ts`에서:

```ts
export namespace JSX {
  // ...
}
```

`JSX` 네임스페이스를 내보내는 것이 타입 검사에는 충분하지만, 프로덕션 런타임은 런타임에 `jsx`, `jsxs`, `Fragment` 내보내기가 필요하고, 개발 런타임은 `jsxDEV`와 `Fragment`가 필요합니다. 이상적으로는 이들에 대한 타입도 추가해야 합니다.

`JSX` 네임스페이스가 적절한 위치에서 사용할 수 없는 경우, 클래식과 자동 런타임 모두 전역 `JSX` 네임스페이스로 폴백합니다.

### 내재 요소

내재 요소는 특별한 인터페이스 `JSX.IntrinsicElements`에서 조회됩니다.
기본적으로, 이 인터페이스가 지정되지 않으면, 무엇이든 가능하고 내재 요소는 타입 검사되지 않습니다.
그러나 이 인터페이스가 _존재_하면, 내재 요소의 이름은 `JSX.IntrinsicElements` 인터페이스의 프로퍼티로 조회됩니다.
예를 들어:

```tsx
declare namespace JSX {
  interface IntrinsicElements {
    foo: any;
  }
}

<foo />; // ok
<bar />; // error
```

위 예제에서 `<foo />`는 잘 작동하지만 `<bar />`는 `JSX.IntrinsicElements`에 지정되지 않았으므로 오류가 발생합니다.

> 참고: 다음과 같이 `JSX.IntrinsicElements`에 포괄적인 문자열 인덱서를 지정할 수도 있습니다:

```ts
declare namespace JSX {
  interface IntrinsicElements {
    [elemName: string]: any;
  }
}
```

### 값 기반 요소

값 기반 요소는 스코프 내에 있는 식별자로 간단히 조회됩니다.

```tsx
import MyComponent from "./myComponent";

<MyComponent />; // ok
<SomeOtherComponent />; // error
```

값 기반 요소를 정의하는 두 가지 방법이 있습니다:

1. 함수 컴포넌트(FC)
2. 클래스 컴포넌트

이 두 유형의 값 기반 요소는 JSX 표현식에서 서로 구별할 수 없기 때문에, 먼저 TS는 오버로드 해석을 사용하여 표현식을 함수 컴포넌트로 해석하려고 합니다. 프로세스가 성공하면, TS는 표현식을 선언으로 해석하는 것을 완료합니다. 값이 함수 컴포넌트로 해석되지 않으면, TS는 클래스 컴포넌트로 해석하려고 합니다. 그것도 실패하면, TS는 오류를 보고합니다.

#### 함수 컴포넌트

이름에서 알 수 있듯이, 컴포넌트는 첫 번째 인수가 `props` 객체인 JavaScript 함수로 정의됩니다.
TS는 반환 타입이 `JSX.Element`에 할당 가능해야 함을 강제합니다.

```tsx
interface FooProp {
  name: string;
  X: number;
  Y: number;
}

declare function AnotherComponent(prop: { name: string });
function ComponentFoo(prop: FooProp) {
  return <AnotherComponent name={prop.name} />;
}

const Button = (prop: { value: string }, context: { color: string }) => (
  <button />
);
```

함수 컴포넌트가 단순히 JavaScript 함수이기 때문에, 여기서도 함수 오버로드를 사용할 수 있습니다:

```ts
interface ClickableProps {
  children: JSX.Element[] | JSX.Element;
}

interface HomeProps extends ClickableProps {
  home: JSX.Element;
}

interface SideProps extends ClickableProps {
  side: JSX.Element | string;
}

function MainButton(prop: HomeProps): JSX.Element;
function MainButton(prop: SideProps): JSX.Element;
function MainButton(prop: ClickableProps): JSX.Element {
  // ...
}
```

> 참고: 함수 컴포넌트는 이전에 상태 비저장 함수 컴포넌트(SFC)로 알려져 있었습니다. 최근 버전의 React에서 함수 컴포넌트가 더 이상 상태 비저장으로 간주될 수 없으므로, `SFC` 타입과 별칭 `StatelessComponent`는 사용되지 않게 되었습니다.

#### 클래스 컴포넌트

클래스 컴포넌트의 타입을 정의하는 것이 가능합니다.
그러나 그렇게 하려면 두 가지 새로운 용어를 이해하는 것이 가장 좋습니다: _요소 클래스 타입_과 _요소 인스턴스 타입_.

`<Expr />`이 주어지면, _요소 클래스 타입_은 `Expr`의 타입입니다.
따라서 위의 예제에서 `MyComponent`가 ES6 클래스라면 클래스 타입은 해당 클래스의 생성자와 정적 멤버가 됩니다.
`MyComponent`가 팩토리 함수라면, 클래스 타입은 해당 함수가 됩니다.

클래스 타입이 설정되면, 인스턴스 타입은 클래스 타입의 생성 또는 호출 시그니처(어느 것이든 존재하는 것)의 반환 타입의 유니온에 의해 결정됩니다.
따라서 다시, ES6 클래스의 경우, 인스턴스 타입은 해당 클래스의 인스턴스 타입이 되고, 팩토리 함수의 경우, 함수에서 반환된 값의 타입이 됩니다.

```ts
class MyComponent {
  render() {}
}

// 생성 시그니처 사용
const myComponent = new MyComponent();

// 요소 클래스 타입 => MyComponent
// 요소 인스턴스 타입 => { render: () => void }

function MyFactoryFunction() {
  return {
    render: () => {},
  };
}

// 호출 시그니처 사용
const myComponent = MyFactoryFunction();

// 요소 클래스 타입 => MyFactoryFunction
// 요소 인스턴스 타입 => { render: () => void }
```

요소 인스턴스 타입은 `JSX.ElementClass`에 할당 가능해야 하며, 그렇지 않으면 오류가 발생합니다.
기본적으로 `JSX.ElementClass`는 `{}`이지만, JSX 사용을 적절한 인터페이스를 따르는 타입으로만 제한하도록 확장할 수 있습니다.

```tsx
declare namespace JSX {
  interface ElementClass {
    render: any;
  }
}

class MyComponent {
  render() {}
}
function MyFactoryFunction() {
  return { render: () => {} };
}

<MyComponent />; // ok
<MyFactoryFunction />; // ok

class NotAValidComponent {}
function NotAValidFactoryFunction() {
  return {};
}

<NotAValidComponent />; // error
<NotAValidFactoryFunction />; // error
```

### 속성 타입 검사

속성을 타입 검사하는 첫 번째 단계는 _요소 속성 타입_을 결정하는 것입니다.
이것은 내재 요소와 값 기반 요소 사이에서 약간 다릅니다.

내재 요소의 경우, `JSX.IntrinsicElements`의 프로퍼티 타입입니다

```tsx
declare namespace JSX {
  interface IntrinsicElements {
    foo: { bar?: boolean };
  }
}

// 'foo'에 대한 요소 속성 타입은 '{bar?: boolean}'입니다
<foo bar />;
```

값 기반 요소의 경우, 조금 더 복잡합니다.
이전에 결정된 _요소 인스턴스 타입_의 프로퍼티 타입에 의해 결정됩니다.
사용할 프로퍼티는 `JSX.ElementAttributesProperty`에 의해 결정됩니다.
단일 프로퍼티로 선언되어야 합니다.
그런 다음 해당 프로퍼티의 이름이 사용됩니다.
TypeScript 2.8부터, `JSX.ElementAttributesProperty`가 제공되지 않으면, 클래스 요소의 생성자 또는 함수 컴포넌트 호출의 첫 번째 매개변수 타입이 대신 사용됩니다.

```tsx
declare namespace JSX {
  interface ElementAttributesProperty {
    props; // 사용할 프로퍼티 이름 지정
  }
}

class MyComponent {
  // 요소 인스턴스 타입에 프로퍼티 지정
  props: {
    foo?: string;
  };
}

// 'MyComponent'에 대한 요소 속성 타입은 '{foo?: string}'입니다
<MyComponent foo="bar" />;
```

요소 속성 타입은 JSX에서 속성을 타입 검사하는 데 사용됩니다.
선택적 및 필수 프로퍼티가 지원됩니다.

```tsx
declare namespace JSX {
  interface IntrinsicElements {
    foo: { requiredProp: string; optionalProp?: number };
  }
}

<foo requiredProp="bar" />; // ok
<foo requiredProp="bar" optionalProp={0} />; // ok
<foo />; // error, requiredProp이 누락됨
<foo requiredProp={0} />; // error, requiredProp은 문자열이어야 함
<foo requiredProp="bar" unknownProp />; // error, unknownProp이 존재하지 않음
<foo requiredProp="bar" some-unknown-prop />; // ok, 'some-unknown-prop'은 유효한 식별자가 아니므로
```

> 참고: 속성 이름이 유효한 JS 식별자가 아닌 경우(`data-*` 속성과 같은), 요소 속성 타입에서 찾을 수 없어도 오류로 간주되지 않습니다.

추가로, `JSX.IntrinsicAttributes` 인터페이스는 일반적으로 컴포넌트의 props나 인수에서 사용되지 않는 JSX 프레임워크에서 사용하는 추가 프로퍼티를 지정하는 데 사용할 수 있습니다 - 예를 들어 React의 `key`. 더 전문화하여, 제네릭 `JSX.IntrinsicClassAttributes<T>` 타입을 사용하여 클래스 컴포넌트에만(함수 컴포넌트 아님) 동일한 종류의 추가 속성을 지정할 수도 있습니다. 이 타입에서 제네릭 매개변수는 클래스 인스턴스 타입에 해당합니다. React에서 이것은 `Ref<T>` 타입의 `ref` 속성을 허용하는 데 사용됩니다. 일반적으로, 이러한 인터페이스의 모든 프로퍼티는 선택적이어야 합니다. JSX 프레임워크의 사용자가 모든 태그에 일부 속성을 제공해야 하는 것이 의도하지 않는 한.

스프레드 연산자도 작동합니다:

```tsx
const props = { requiredProp: "bar" };
<foo {...props} />; // ok

const badProps = {};
<foo {...badProps} />; // error
```

### 자식 타입 검사

TypeScript 2.3에서 TS는 _자식_의 타입 검사를 도입했습니다. _자식_은 _요소 속성 타입_의 특별한 프로퍼티로, 자식 *JSXExpression*이 속성에 삽입됩니다.
TS가 `JSX.ElementAttributesProperty`를 사용하여 _props_의 이름을 결정하는 것과 유사하게, TS는 `JSX.ElementChildrenAttribute`를 사용하여 해당 props 내의 _자식_의 이름을 결정합니다.
`JSX.ElementChildrenAttribute`는 단일 프로퍼티로 선언되어야 합니다.

```ts
declare namespace JSX {
  interface ElementChildrenAttribute {
    children: {}; // 사용할 자식 이름 지정
  }
}
```

```tsx
<div>
  <h1>Hello</h1>
</div>;

<div>
  <h1>Hello</h1>
  World
</div>;

const CustomComp = (props) => <div>{props.children}</div>
<CustomComp>
  <div>Hello World</div>
  {"This is just a JS expression..." + 1000}
</CustomComp>
```

다른 속성과 마찬가지로 _자식_의 타입을 지정할 수 있습니다. 이것은 예를 들어 [React 타이핑](https://github.com/DefinitelyTyped/DefinitelyTyped/tree/master/types/react)을 사용하는 경우 기본 타입을 재정의합니다.

```tsx
interface PropsType {
  children: JSX.Element
  name: string
}

class Component extends React.Component<PropsType, {}> {
  render() {
    return (
      <h2>
        {this.props.children}
      </h2>
    )
  }
}

// OK
<Component name="foo">
  <h1>Hello World</h1>
</Component>

// Error: children은 JSX.Element 타입이지 JSX.Element 배열이 아님
<Component name="bar">
  <h1>Hello World</h1>
  <h2>Hello World</h2>
</Component>

// Error: children은 JSX.Element 타입이지 JSX.Element 배열이나 문자열이 아님
<Component name="baz">
  <h1>Hello</h1>
  World
</Component>
```

## JSX 결과 타입

기본적으로 JSX 표현식의 결과는 `any`로 타입이 지정됩니다.
`JSX.Element` 인터페이스를 지정하여 타입을 커스터마이즈할 수 있습니다.
그러나 이 인터페이스에서 JSX의 요소, 속성 또는 자식에 대한 타입 정보를 검색할 수 없습니다.
이것은 블랙박스입니다.

## JSX 함수 반환 타입

기본적으로, 함수 컴포넌트는 `JSX.Element | null`을 반환해야 합니다. 그러나 이것이 항상 런타임 동작을 나타내는 것은 아닙니다. TypeScript 5.1부터, 유효한 JSX 컴포넌트 타입이 무엇인지 재정의하기 위해 `JSX.ElementType`을 지정할 수 있습니다. 이것은 어떤 props가 유효한지 정의하지 않습니다. props의 타입은 항상 전달되는 컴포넌트의 첫 번째 인수에 의해 정의됩니다. 기본값은 다음과 같습니다:

```ts
namespace JSX {
    export type ElementType =
        // 모든 유효한 소문자 태그
        | keyof IntrinsicElements
        // 함수 컴포넌트
        | (props: any) => Element
        // 클래스 컴포넌트
        | new (props: any) => ElementClass;
    export interface IntrinsicAttributes extends /*...*/ {}
    export type Element = /*...*/;
    export type ElementClass = /*...*/;
}
```

## 표현식 삽입

JSX는 중괄호(`{ }`)로 표현식을 둘러싸 태그 사이에 표현식을 삽입할 수 있게 합니다.

```tsx
const a = (
  <div>
    {["foo", "bar"].map((i) => (
      <span>{i / 2}</span>
    ))}
  </div>
);
```

위 코드는 문자열을 숫자로 나눌 수 없으므로 오류가 발생합니다.
`preserve` 옵션을 사용할 때 출력은 다음과 같습니다:

```tsx
const a = (
  <div>
    {["foo", "bar"].map(function (i) {
      return <span>{i / 2}</span>;
    })}
  </div>
);
```

## React 통합

React와 JSX를 사용하려면 [React 타이핑](https://github.com/DefinitelyTyped/DefinitelyTyped/tree/master/types/react)을 사용해야 합니다.
이러한 타이핑은 React와 함께 사용하기 위해 `JSX` 네임스페이스를 적절하게 정의합니다.

```tsx
/// <reference path="react.d.ts" />

interface Props {
  foo: string;
}

class MyComponent extends React.Component<Props, {}> {
  render() {
    return <span>{this.props.foo}</span>;
  }
}

<MyComponent foo="bar" />; // ok
<MyComponent foo={0} />; // error
```

### JSX 구성

JSX를 커스터마이즈하는 데 사용할 수 있는 여러 컴파일러 플래그가 있으며, 컴파일러 플래그와 파일별 인라인 프래그마 모두로 작동합니다. 자세한 내용은 tsconfig 참조 페이지를 참조하세요:

- [`jsxFactory`](/tsconfig#jsxFactory)
- [`jsxFragmentFactory`](/tsconfig#jsxFragmentFactory)
- [`jsxImportSource`](/tsconfig#jsxImportSource)

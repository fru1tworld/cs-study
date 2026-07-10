# JSX · 데코레이터 · 믹스인

# JSX

> **원문:** https://www.typescriptlang.org/docs/handbook/jsx.html

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
그러나 이 인터페이스가 _존재_하면, 내재 요소의 이름은 `JSX.IntrinsicElements` 인터페이스의 속성로 조회됩니다.
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

내재 요소의 경우, `JSX.IntrinsicElements`의 속성 타입입니다

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
이전에 결정된 _요소 인스턴스 타입_의 속성 타입에 의해 결정됩니다.
사용할 속성는 `JSX.ElementAttributesProperty`에 의해 결정됩니다.
단일 속성로 선언되어야 합니다.
그런 다음 해당 속성의 이름이 사용됩니다.
TypeScript 2.8부터, `JSX.ElementAttributesProperty`가 제공되지 않으면, 클래스 요소의 생성자 또는 함수 컴포넌트 호출의 첫 번째 매개변수 타입이 대신 사용됩니다.

```tsx
declare namespace JSX {
  interface ElementAttributesProperty {
    props; // 사용할 속성 이름 지정
  }
}

class MyComponent {
  // 요소 인스턴스 타입에 속성 지정
  props: {
    foo?: string;
  };
}

// 'MyComponent'에 대한 요소 속성 타입은 '{foo?: string}'입니다
<MyComponent foo="bar" />;
```

요소 속성 타입은 JSX에서 속성을 타입 검사하는 데 사용됩니다.
선택적 및 필수 속성가 지원됩니다.

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

추가로, `JSX.IntrinsicAttributes` 인터페이스는 일반적으로 컴포넌트의 props나 인수에서 사용되지 않는 JSX 프레임워크에서 사용하는 추가 속성를 지정하는 데 사용할 수 있습니다 - 예를 들어 React의 `key`. 더 전문화하여, 제네릭 `JSX.IntrinsicClassAttributes<T>` 타입을 사용하여 클래스 컴포넌트에만(함수 컴포넌트 아님) 동일한 종류의 추가 속성을 지정할 수도 있습니다. 이 타입에서 제네릭 매개변수는 클래스 인스턴스 타입에 해당합니다. React에서 이것은 `Ref<T>` 타입의 `ref` 속성을 허용하는 데 사용됩니다. 일반적으로, 이러한 인터페이스의 모든 속성는 선택적이어야 합니다. JSX 프레임워크의 사용자가 모든 태그에 일부 속성을 제공해야 하는 것이 의도하지 않는 한.

스프레드 연산자도 작동합니다:

```tsx
const props = { requiredProp: "bar" };
<foo {...props} />; // ok

const badProps = {};
<foo {...badProps} />; // error
```

### 자식 타입 검사

TypeScript 2.3에서 TS는 _자식_의 타입 검사를 도입했습니다. _자식_은 _요소 속성 타입_의 특별한 속성로, 자식 *JSXExpression*이 속성에 삽입됩니다.
TS가 `JSX.ElementAttributesProperty`를 사용하여 _props_의 이름을 결정하는 것과 유사하게, TS는 `JSX.ElementChildrenAttribute`를 사용하여 해당 props 내의 _자식_의 이름을 결정합니다.
`JSX.ElementChildrenAttribute`는 단일 속성로 선언되어야 합니다.

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

---

# 데코레이터 (Decorators)

> **원문:** https://www.typescriptlang.org/docs/handbook/decorators.html

> **참고**&nbsp; 이 문서는 실험적인 stage 2 데코레이터 구현을 다룹니다. Stage 3 데코레이터 지원은 TypeScript 5.0부터 사용할 수 있습니다.
> 참조: [TypeScript 5.0의 데코레이터](https://devblogs.microsoft.com/typescript/announcing-typescript-5-0/#decorators)

## 소개

TypeScript와 ES6에서 클래스가 도입되면서, 이제 클래스 및 클래스 멤버에 주석을 달거나 수정하는 것을 지원하기 위한 추가 기능이 필요한 특정 시나리오가 존재합니다.
데코레이터는 클래스 선언과 멤버에 대한 주석과 메타프로그래밍 구문을 추가하는 방법을 제공합니다.

> 추가 읽기 (stage 2): [TypeScript 데코레이터 완벽 가이드](https://saul-mirone.github.io/a-complete-guide-to-typescript-decorator/)

데코레이터에 대한 실험적 지원을 활성화하려면, 커맨드 라인이나 `tsconfig.json`에서 [`experimentalDecorators`](/tsconfig#experimentalDecorators) 컴파일러 옵션을 활성화해야 합니다:

**커맨드 라인**:

```shell
tsc --target ES5 --experimentalDecorators
```

**tsconfig.json**:

```json
{
  "compilerOptions": {
    "target": "ES5",
    "experimentalDecorators": true
  }
}
```

## 데코레이터

_데코레이터_는 [클래스 선언](#클래스-데코레이터), [메서드](#메서드-데코레이터), [접근자](#접근자-데코레이터), [속성](#속성-데코레이터), 또는 [매개변수](#매개변수-데코레이터)에 첨부할 수 있는 특별한 종류의 선언입니다.
데코레이터는 `@expression` 형식을 사용하며, 여기서 `expression`은 데코레이트된 선언에 대한 정보와 함께 런타임에 호출될 함수로 평가되어야 합니다.

예를 들어, 데코레이터 `@sealed`가 주어지면 다음과 같이 `sealed` 함수를 작성할 수 있습니다:

```ts
function sealed(target) {
  // 'target'으로 무언가를 합니다...
}
```

## 데코레이터 팩토리

데코레이터가 선언에 적용되는 방식을 커스터마이즈하려면, 데코레이터 팩토리를 작성할 수 있습니다.
_데코레이터 팩토리_는 런타임에 데코레이터에 의해 호출될 표현식을 반환하는 간단한 함수입니다.

다음과 같은 방식으로 데코레이터 팩토리를 작성할 수 있습니다:

```ts
function color(value: string) {
  // 이것은 데코레이터 팩토리이며,
  // 반환된 데코레이터 함수를 설정합니다
  return function (target) {
    // 이것이 데코레이터입니다
    // 'target'과 'value'로 무언가를 합니다...
  };
}
```

## 데코레이터 합성

여러 데코레이터를 선언에 적용할 수 있습니다. 예를 들어 한 줄에:

```ts
@f @g x
```

여러 줄에:

```ts
@f
@g
x
```

여러 데코레이터가 단일 선언에 적용될 때, 그 평가는 [수학에서의 함수 합성](https://wikipedia.org/wiki/Function_composition)과 유사합니다. 이 모델에서 함수 _f_와 _g_를 합성할 때, 결과 합성 (_f_ ∘ _g_)(_x_)는 _f_(_g_(_x_))와 동등합니다.

따라서, TypeScript에서 단일 선언에 여러 데코레이터를 평가할 때 다음 단계가 수행됩니다:

1. 각 데코레이터의 표현식이 위에서 아래로 평가됩니다.
2. 그런 다음 결과가 아래에서 위로 함수로 호출됩니다.

[데코레이터 팩토리](#데코레이터-팩토리)를 사용하면, 다음 예제로 이 평가 순서를 관찰할 수 있습니다:

```ts
function first() {
  console.log("first(): factory evaluated");
  return function (target: any, propertyKey: string, descriptor: PropertyDescriptor) {
    console.log("first(): called");
  };
}

function second() {
  console.log("second(): factory evaluated");
  return function (target: any, propertyKey: string, descriptor: PropertyDescriptor) {
    console.log("second(): called");
  };
}

class ExampleClass {
  @first()
  @second()
  method() {}
}
```

이것은 콘솔에 다음 출력을 출력합니다:

```shell
first(): factory evaluated
second(): factory evaluated
second(): called
first(): called
```

## 데코레이터 평가

클래스 내의 다양한 선언에 적용된 데코레이터가 어떻게 적용되는지에 대한 잘 정의된 순서가 있습니다:

1. _매개변수 데코레이터_, _메서드_, _접근자_, 또는 _속성 데코레이터_가 각 인스턴스 멤버에 적용됩니다.
2. _매개변수 데코레이터_, _메서드_, _접근자_, 또는 _속성 데코레이터_가 각 정적 멤버에 적용됩니다.
3. _매개변수 데코레이터_가 생성자에 적용됩니다.
4. _클래스 데코레이터_가 클래스에 적용됩니다.

## 클래스 데코레이터

_클래스 데코레이터_는 클래스 선언 바로 앞에 선언됩니다.
클래스 데코레이터는 클래스의 생성자에 적용되며 클래스 정의를 관찰, 수정 또는 대체하는 데 사용할 수 있습니다.
클래스 데코레이터는 선언 파일이나 다른 앰비언트 컨텍스트(예: `declare` 클래스)에서 사용할 수 없습니다.

클래스 데코레이터의 표현식은 런타임에 함수로 호출되며, 데코레이트된 클래스의 생성자가 유일한 인수로 전달됩니다.

클래스 데코레이터가 값을 반환하면, 제공된 생성자 함수로 클래스 선언을 대체합니다.

> **참고**&nbsp; 새로운 생성자 함수를 반환하기로 선택한 경우, 원래 프로토타입을 유지하도록 주의해야 합니다.
> 런타임에 데코레이터를 적용하는 로직은 이것을 대신 해주지 **않습니다**.

다음은 `BugReport` 클래스에 적용된 클래스 데코레이터(`@sealed`)의 예입니다:

```ts
@sealed
class BugReport {
  type = "report";
  title: string;

  constructor(t: string) {
    this.title = t;
  }
}
```

다음 함수 선언을 사용하여 `@sealed` 데코레이터를 정의할 수 있습니다:

```ts
function sealed(constructor: Function) {
  Object.seal(constructor);
  Object.seal(constructor.prototype);
}
```

`@sealed`가 실행되면, 생성자와 프로토타입을 모두 봉인하여, 런타임에 `BugReport.prototype`에 접근하거나 `BugReport` 자체에 속성를 정의하여 이 클래스에 추가 기능을 추가하거나 제거하는 것을 방지합니다(ES2015 클래스는 실제로 프로토타입 기반 생성자 함수에 대한 문법적 설탕일 뿐입니다). 이 데코레이터는 클래스가 `BugReport`를 서브클래싱하는 것을 방지하지 **않습니다**.

다음으로 새 기본값을 설정하기 위해 생성자를 재정의하는 방법의 예가 있습니다.

```ts
// @errors: 2339
function reportableClassDecorator<T extends { new (...args: any[]): {} }>(constructor: T) {
  return class extends constructor {
    reportingURL = "http://www...";
  };
}

@reportableClassDecorator
class BugReport {
  type = "report";
  title: string;

  constructor(t: string) {
    this.title = t;
  }
}

const bug = new BugReport("Needs dark mode");
console.log(bug.title); // "Needs dark mode" 출력
console.log(bug.type); // "report" 출력

// 데코레이터가 TypeScript 타입을 변경하지 _않으므로_
// 새 속성 `reportingURL`은 타입 시스템에서
// 알려지지 않습니다:
bug.reportingURL;
```

## 메서드 데코레이터

_메서드 데코레이터_는 메서드 선언 바로 앞에 선언됩니다.
데코레이터는 메서드의 _속성 설명자_에 적용되며, 메서드 정의를 관찰, 수정 또는 대체하는 데 사용할 수 있습니다.
메서드 데코레이터는 선언 파일, 오버로드, 또는 다른 앰비언트 컨텍스트(예: `declare` 클래스)에서 사용할 수 없습니다.

메서드 데코레이터의 표현식은 런타임에 다음 세 가지 인수와 함께 함수로 호출됩니다:

1. 정적 멤버의 경우 클래스의 생성자 함수, 또는 인스턴스 멤버의 경우 클래스의 프로토타입.
2. 멤버의 이름.
3. 멤버의 _속성 설명자_.

> **참고**&emsp; 스크립트 대상이 `ES5` 미만인 경우 _속성 설명자_는 `undefined`가 됩니다.

메서드 데코레이터가 값을 반환하면, 해당 메서드의 _속성 설명자_로 사용됩니다.

> **참고**&emsp; 스크립트 대상이 `ES5` 미만인 경우 반환 값은 무시됩니다.

다음은 `Greeter` 클래스의 메서드에 적용된 메서드 데코레이터(`@enumerable`)의 예입니다:

```ts
class Greeter {
  greeting: string;
  constructor(message: string) {
    this.greeting = message;
  }

  @enumerable(false)
  greet() {
    return "Hello, " + this.greeting;
  }
}
```

다음 함수 선언을 사용하여 `@enumerable` 데코레이터를 정의할 수 있습니다:

```ts
function enumerable(value: boolean) {
  return function (target: any, propertyKey: string, descriptor: PropertyDescriptor) {
    descriptor.enumerable = value;
  };
}
```

여기서 `@enumerable(false)` 데코레이터는 [데코레이터 팩토리](#데코레이터-팩토리)입니다.
`@enumerable(false)` 데코레이터가 호출되면, 속성 설명자의 `enumerable` 속성를 수정합니다.

## 접근자 데코레이터

_접근자 데코레이터_는 접근자 선언 바로 앞에 선언됩니다.
접근자 데코레이터는 접근자의 _속성 설명자_에 적용되며 접근자의 정의를 관찰, 수정 또는 대체하는 데 사용할 수 있습니다.
접근자 데코레이터는 선언 파일이나 다른 앰비언트 컨텍스트(예: `declare` 클래스)에서 사용할 수 없습니다.

> **참고**&emsp; TypeScript는 단일 멤버에 대해 `get`과 `set` 접근자를 모두 데코레이트하는 것을 허용하지 않습니다.
> 대신, 멤버에 대한 모든 데코레이터는 문서 순서로 지정된 첫 번째 접근자에 적용되어야 합니다.
> 이는 데코레이터가 `get`과 `set` 접근자를 별도로 결합하는 것이 아니라, _속성 설명자_에 적용되기 때문입니다.

접근자 데코레이터의 표현식은 런타임에 다음 세 가지 인수와 함께 함수로 호출됩니다:

1. 정적 멤버의 경우 클래스의 생성자 함수, 또는 인스턴스 멤버의 경우 클래스의 프로토타입.
2. 멤버의 이름.
3. 멤버의 _속성 설명자_.

> **참고**&emsp; 스크립트 대상이 `ES5` 미만인 경우 _속성 설명자_는 `undefined`가 됩니다.

접근자 데코레이터가 값을 반환하면, 해당 멤버의 _속성 설명자_로 사용됩니다.

> **참고**&emsp; 스크립트 대상이 `ES5` 미만인 경우 반환 값은 무시됩니다.

다음은 `Point` 클래스의 멤버에 적용된 접근자 데코레이터(`@configurable`)의 예입니다:

```ts
class Point {
  private _x: number;
  private _y: number;
  constructor(x: number, y: number) {
    this._x = x;
    this._y = y;
  }

  @configurable(false)
  get x() {
    return this._x;
  }

  @configurable(false)
  get y() {
    return this._y;
  }
}
```

다음 함수 선언을 사용하여 `@configurable` 데코레이터를 정의할 수 있습니다:

```ts
function configurable(value: boolean) {
  return function (target: any, propertyKey: string, descriptor: PropertyDescriptor) {
    descriptor.configurable = value;
  };
}
```

## 속성 데코레이터

_속성 데코레이터_는 속성 선언 바로 앞에 선언됩니다.
속성 데코레이터는 선언 파일이나 다른 앰비언트 컨텍스트(예: `declare` 클래스)에서 사용할 수 없습니다.

속성 데코레이터의 표현식은 런타임에 다음 두 가지 인수와 함께 함수로 호출됩니다:

1. 정적 멤버의 경우 클래스의 생성자 함수, 또는 인스턴스 멤버의 경우 클래스의 프로토타입.
2. 멤버의 이름.

> **참고**&emsp; TypeScript에서 속성 데코레이터가 초기화되는 방식 때문에 _속성 설명자_가 속성 데코레이터의 인수로 제공되지 않습니다.
> 이는 현재 프로토타입의 멤버를 정의할 때 인스턴스 속성를 설명하는 메커니즘이 없고, 속성의 이니셜라이저를 관찰하거나 수정할 방법이 없기 때문입니다. 반환 값도 무시됩니다.
> 따라서 속성 데코레이터는 특정 이름의 속성이 클래스에 선언되었음을 관찰하는 데에만 사용할 수 있습니다.

이 정보를 사용하여 다음 예제와 같이 속성에 대한 메타데이터를 기록할 수 있습니다:

```ts
class Greeter {
  @format("Hello, %s")
  greeting: string;

  constructor(message: string) {
    this.greeting = message;
  }

  greet() {
    let formatString = getFormat(this, "greeting");
    return formatString.replace("%s", this.greeting);
  }
}
```

그런 다음 다음 함수 선언을 사용하여 `@format` 데코레이터와 `getFormat` 함수를 정의할 수 있습니다:

```ts
import "reflect-metadata";

const formatMetadataKey = Symbol("format");

function format(formatString: string) {
  return Reflect.metadata(formatMetadataKey, formatString);
}

function getFormat(target: any, propertyKey: string) {
  return Reflect.getMetadata(formatMetadataKey, target, propertyKey);
}
```

여기서 `@format("Hello, %s")` 데코레이터는 [데코레이터 팩토리](#데코레이터-팩토리)입니다.
`@format("Hello, %s")`이 호출되면, `reflect-metadata` 라이브러리의 `Reflect.metadata` 함수를 사용하여 속성에 대한 메타데이터 항목을 추가합니다.
`getFormat`이 호출되면, 형식에 대한 메타데이터 값을 읽습니다.

> **참고**&emsp; 이 예제는 `reflect-metadata` 라이브러리가 필요합니다.
> `reflect-metadata` 라이브러리에 대한 자세한 내용은 [메타데이터](#메타데이터)를 참조하세요.

## 매개변수 데코레이터

_매개변수 데코레이터_는 매개변수 선언 바로 앞에 선언됩니다.
매개변수 데코레이터는 클래스 생성자 또는 메서드 선언의 함수에 적용됩니다.
매개변수 데코레이터는 선언 파일, 오버로드, 또는 다른 앰비언트 컨텍스트(예: `declare` 클래스)에서 사용할 수 없습니다.

매개변수 데코레이터의 표현식은 런타임에 다음 세 가지 인수와 함께 함수로 호출됩니다:

1. 정적 멤버의 경우 클래스의 생성자 함수, 또는 인스턴스 멤버의 경우 클래스의 프로토타입.
2. 멤버의 이름.
3. 함수의 매개변수 목록에서 매개변수의 서수 인덱스.

> **참고**&emsp; 매개변수 데코레이터는 매개변수가 메서드에 선언되었음을 관찰하는 데에만 사용할 수 있습니다.

매개변수 데코레이터의 반환 값은 무시됩니다.

다음은 `BugReport` 클래스 멤버의 매개변수에 적용된 매개변수 데코레이터(`@required`)의 예입니다:

```ts
class BugReport {
  type = "report";
  title: string;

  constructor(t: string) {
    this.title = t;
  }

  @validate
  print(@required verbose: boolean) {
    if (verbose) {
      return `type: ${this.type}\ntitle: ${this.title}`;
    } else {
     return this.title;
    }
  }
}
```

그런 다음 다음 함수 선언을 사용하여 `@required`와 `@validate` 데코레이터를 정의할 수 있습니다:

```ts
import "reflect-metadata";
const requiredMetadataKey = Symbol("required");

function required(target: Object, propertyKey: string | symbol, parameterIndex: number) {
  let existingRequiredParameters: number[] = Reflect.getOwnMetadata(requiredMetadataKey, target, propertyKey) || [];
  existingRequiredParameters.push(parameterIndex);
  Reflect.defineMetadata( requiredMetadataKey, existingRequiredParameters, target, propertyKey);
}

function validate(target: any, propertyName: string, descriptor: TypedPropertyDescriptor<Function>) {
  let method = descriptor.value!;

  descriptor.value = function () {
    let requiredParameters: number[] = Reflect.getOwnMetadata(requiredMetadataKey, target, propertyName);
    if (requiredParameters) {
      for (let parameterIndex of requiredParameters) {
        if (parameterIndex >= arguments.length || arguments[parameterIndex] === undefined) {
          throw new Error("Missing required argument.");
        }
      }
    }
    return method.apply(this, arguments);
  };
}
```

`@required` 데코레이터는 매개변수를 필수로 표시하는 메타데이터 항목을 추가합니다.
`@validate` 데코레이터는 기존 `print` 메서드를 원래 메서드를 호출하기 전에 인수의 유효성을 검사하는 함수로 래핑합니다.

> **참고**&emsp; 이 예제는 `reflect-metadata` 라이브러리가 필요합니다.
> `reflect-metadata` 라이브러리에 대한 자세한 내용은 [메타데이터](#메타데이터)를 참조하세요.

## 메타데이터

일부 예제는 [실험적 메타데이터 API](https://github.com/rbuckton/ReflectDecorators)에 대한 폴리필을 추가하는 `reflect-metadata` 라이브러리를 사용합니다.
이 라이브러리는 아직 ECMAScript (JavaScript) 표준의 일부가 아닙니다.
그러나 데코레이터가 공식적으로 ECMAScript 표준의 일부로 채택되면, 이러한 확장이 채택을 위해 제안될 것입니다.

이 라이브러리는 npm을 통해 설치할 수 있습니다:

```shell
npm i reflect-metadata --save
```

TypeScript는 데코레이터가 있는 선언에 대해 특정 유형의 메타데이터를 방출하는 실험적 지원을 포함합니다.
이 실험적 지원을 활성화하려면, 커맨드 라인이나 `tsconfig.json`에서 [`emitDecoratorMetadata`](/tsconfig#emitDecoratorMetadata) 컴파일러 옵션을 설정해야 합니다:

**커맨드 라인**:

```shell
tsc --target ES5 --experimentalDecorators --emitDecoratorMetadata
```

**tsconfig.json**:

```json
{
  "compilerOptions": {
    "target": "ES5",
    "experimentalDecorators": true,
    "emitDecoratorMetadata": true
  }
}
```

활성화되면, `reflect-metadata` 라이브러리가 임포트된 한, 추가 설계 시간 타입 정보가 런타임에 노출됩니다.

다음 예제에서 이것이 어떻게 작동하는지 볼 수 있습니다:

```ts
import "reflect-metadata";

class Point {
  constructor(public x: number, public y: number) {}
}

class Line {
  private _start: Point;
  private _end: Point;

  @validate
  set start(value: Point) {
    this._start = value;
  }

  get start() {
    return this._start;
  }

  @validate
  set end(value: Point) {
    this._end = value;
  }

  get end() {
    return this._end;
  }
}

function validate<T>(target: any, propertyKey: string, descriptor: TypedPropertyDescriptor<T>) {
  let set = descriptor.set!;

  descriptor.set = function (value: T) {
    let type = Reflect.getMetadata("design:type", target, propertyKey);

    if (!(value instanceof type)) {
      throw new TypeError(`Invalid type, got ${typeof value} not ${type.name}.`);
    }

    set.call(this, value);
  };
}

const line = new Line()
line.start = new Point(0, 0)

// @ts-ignore
// line.end = {}

// 런타임에 다음과 함께 실패합니다:
// > Invalid type, got object not Point
```

TypeScript 컴파일러는 `@Reflect.metadata` 데코레이터를 사용하여 설계 시간 타입 정보를 주입합니다.
다음 TypeScript와 동등하다고 생각할 수 있습니다:

```ts
class Line {
  private _start: Point;
  private _end: Point;

  @validate
  @Reflect.metadata("design:type", Point)
  set start(value: Point) {
    this._start = value;
  }
  get start() {
    return this._start;
  }

  @validate
  @Reflect.metadata("design:type", Point)
  set end(value: Point) {
    this._end = value;
  }
  get end() {
    return this._end;
  }
}
```

> **참고**&emsp; 데코레이터 메타데이터는 실험적 기능이며 향후 릴리스에서 호환성이 깨지는 변경 사항이 도입될 수 있습니다.

---

# 믹스인 (Mixins)

> **원문:** https://www.typescriptlang.org/docs/handbook/mixins.html

전통적인 OO 계층 구조와 함께, 재사용 가능한 컴포넌트로부터 클래스를 빌드하는 또 다른 인기 있는 방법은 더 간단한 부분 클래스를 결합하여 빌드하는 것입니다.
Scala와 같은 언어에서 믹스인이나 트레이트 아이디어에 익숙할 수 있으며, 이 패턴은 JavaScript 커뮤니티에서도 인기를 얻고 있습니다.

## 믹스인은 어떻게 작동하나요?

이 패턴은 클래스 상속과 함께 제네릭을 사용하여 베이스 클래스를 확장하는 것에 의존합니다.
TypeScript의 가장 좋은 믹스인 지원은 클래스 표현식 패턴을 통해 수행됩니다.
이 패턴이 JavaScript에서 어떻게 작동하는지에 대해 [여기](https://justinfagnani.com/2015/12/21/real-mixins-with-javascript-classes/)에서 더 읽을 수 있습니다.

시작하려면, 믹스인이 위에 적용될 클래스가 필요합니다:

```ts
class Sprite {
  name = "";
  x = 0;
  y = 0;

  constructor(name: string) {
    this.name = name;
  }
}
```

그런 다음 베이스 클래스를 확장하는 클래스 표현식을 반환하는 타입과 팩토리 함수가 필요합니다.

```ts
// 시작하려면, 다른 클래스에서 확장하는 데 사용할 타입이 필요합니다.
// 주요 책임은 전달되는 타입이 클래스임을 선언하는 것입니다.

type Constructor = new (...args: any[]) => {};

// 이 믹스인은 scale 속성와
// 캡슐화된 private 속성로 변경하기 위한 getter와 setter를 추가합니다:

function Scale<TBase extends Constructor>(Base: TBase) {
  return class Scaling extends Base {
    // 믹스인은 private/protected 속성를 선언할 수 없습니다
    // 그러나 ES2020 private 필드를 사용할 수 있습니다
    _scale = 1;

    setScale(scale: number) {
      this._scale = scale;
    }

    get scale(): number {
      return this._scale;
    }
  };
}
```

이것들이 모두 설정되면, 믹스인이 적용된 베이스 클래스를 나타내는 클래스를 만들 수 있습니다:

```ts
// 믹스인 Scale 적용자와 함께 Sprite 클래스에서
// 새 클래스를 구성합니다:
const EightBitSprite = Scale(Sprite);

const flappySprite = new EightBitSprite("Bird");
flappySprite.setScale(0.8);
console.log(flappySprite.scale);
```

## 제한된 믹스인

위 형식에서 믹스인은 클래스에 대한 기본 지식이 없어 원하는 디자인을 만들기 어려울 수 있습니다.

이를 모델링하기 위해, 원래 생성자 타입을 제네릭 인수를 받도록 수정합니다.

```ts
// 이것은 이전 생성자였습니다:
type Constructor = new (...args: any[]) => {};
// 이제 이 믹스인이 적용되는 클래스에 제약을 적용할 수 있는
// 제네릭 버전을 사용합니다
type GConstructor<T = {}> = new (...args: any[]) => T;
```

이를 통해 제한된 베이스 클래스에서만 작동하는 클래스를 만들 수 있습니다:

```ts
type Positionable = GConstructor<{ setPos: (x: number, y: number) => void }>;
type Spritable = GConstructor<Sprite>;
type Loggable = GConstructor<{ print: () => void }>;
```

그런 다음 빌드할 특정 베이스가 있을 때만 작동하는 믹스인을 만들 수 있습니다:

```ts
function Jumpable<TBase extends Positionable>(Base: TBase) {
  return class Jumpable extends Base {
    jump() {
      // 이 믹스인은 Positionable 제약으로 인해
      // setPos가 정의된 베이스 클래스가 전달된 경우에만 작동합니다.
      this.setPos(0, 20);
    }
  };
}
```

## 대안적 패턴

이 문서의 이전 버전에서는 런타임과 타입 계층 구조를 별도로 생성한 다음 마지막에 병합하는 방식으로 믹스인을 작성하는 방법을 권장했습니다:

```ts
// 각 믹스인은 전통적인 ES 클래스입니다
class Jumpable {
  jump() {}
}

class Duckable {
  duck() {}
}

// 베이스 포함
class Sprite {
  x = 0;
  y = 0;
}

// 그런 다음 예상되는 믹스인을
// 베이스와 같은 이름으로 병합하는 인터페이스를 만듭니다
interface Sprite extends Jumpable, Duckable {}
// 런타임에 JS를 통해 베이스 클래스에 믹스인을 적용합니다
applyMixins(Sprite, [Jumpable, Duckable]);

let player = new Sprite();
player.jump();
console.log(player.x, player.y);

// 이것은 코드베이스의 어디에나 있을 수 있습니다:
function applyMixins(derivedCtor: any, constructors: any[]) {
  constructors.forEach((baseCtor) => {
    Object.getOwnPropertyNames(baseCtor.prototype).forEach((name) => {
      Object.defineProperty(
        derivedCtor.prototype,
        name,
        Object.getOwnPropertyDescriptor(baseCtor.prototype, name) ||
          Object.create(null)
      );
    });
  });
}
```

이 패턴은 컴파일러에 덜 의존하고, 런타임과 타입 시스템이 올바르게 동기화되도록 코드베이스에 더 의존합니다.

## 제약 사항

믹스인 패턴은 TypeScript 컴파일러 내에서 코드 흐름 분석을 통해 네이티브로 지원됩니다.
네이티브 지원의 가장자리에 도달할 수 있는 몇 가지 경우가 있습니다.

#### 데코레이터와 믹스인 [`#4881`](https://github.com/microsoft/TypeScript/issues/4881)

데코레이터를 사용하여 코드 흐름 분석을 통해 믹스인을 제공할 수 없습니다:

```ts
// @experimentalDecorators
// @errors: 2339
// 믹스인 패턴을 복제하는 데코레이터 함수:
const Pausable = (target: typeof Player) => {
  return class Pausable extends target {
    shouldFreeze = false;
  };
};

@Pausable
class Player {
  x = 0;
  y = 0;
}

// Player 클래스에는 데코레이터의 타입이 병합되지 않습니다:
const player = new Player();
player.shouldFreeze;

// 런타임 측면은 타입 구성이나 인터페이스 병합을 통해
// 수동으로 복제할 수 있습니다.
type FreezablePlayer = Player & { shouldFreeze: boolean };

const playerTwo = (new Player() as unknown) as FreezablePlayer;
playerTwo.shouldFreeze;
```

#### 정적 속성 믹스인 [`#17829`](https://github.com/microsoft/TypeScript/issues/17829)

제약보다는 함정입니다.
클래스 표현식 패턴은 싱글톤을 생성하므로, 다른 변수 타입을 지원하기 위해 타입 시스템에서 매핑할 수 없습니다.

제네릭에 따라 다른 클래스를 반환하는 함수를 사용하여 이 문제를 해결할 수 있습니다:

```ts
function base<T>() {
  class Base {
    static prop: T;
  }
  return Base;
}

function derived<T>() {
  class Derived extends base<T>() {
    static anotherProp: T;
  }
  return Derived;
}

class Spec extends derived<string>() {}

Spec.prop; // string
Spec.anotherProp; // string
```

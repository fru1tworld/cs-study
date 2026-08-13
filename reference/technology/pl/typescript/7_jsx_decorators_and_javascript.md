# TypeScript JSX·데코레이터·믹스인과 JavaScript 연동

## JSX · 데코레이터 · 믹스인

## JSX

> **원문:** https://www.typescriptlang.org/docs/handbook/jsx.html

- [JSX](https://facebook.github.io/jsx/): 임베드 가능한 XML 유사 구문
  - 유효한 JavaScript로 변환되도록 의도됨 → 변환의 의미는 구현에 따라 다름
  - [React](https://reactjs.org/) 프레임워크와 함께 인기를 얻음 → 이후 다른 구현도 등장
  - TypeScript는 JSX를 직접 JavaScript에 임베딩·타입 검사·컴파일 지원

### 기본 사용법

- JSX 사용을 위한 필수 조건 두 가지
  - 파일 이름을 `.tsx` 확장자로 지정
  - [`jsx`](/tsconfig#jsx) 옵션 활성화
- TypeScript가 제공하는 JSX 모드: `preserve`, `react`(클래식 런타임), `react-jsx`(자동 런타임), `react-jsxdev`(자동 개발 런타임), `react-native`
  - `preserve`: 다른 변환 단계(예: [Babel](https://babeljs.io/))에서 추가로 소비할 수 있도록 JSX를 출력에 그대로 유지 → 출력 파일 확장자는 `.jsx`
  - `react`: `React.createElement`를 방출 → 사용 전 JSX 변환 불필요 → 출력 파일 확장자는 `.js`
  - `react-native`: 모든 JSX를 유지한다는 점은 `preserve`와 동등 → 출력 파일 확장자는 `.js`
- 모드별 입력·출력 예시
  - `preserve`: 입력 `<div />` → 출력 `<div />` (출력 확장자 `.jsx`)
  - `react`: 입력 `<div />` → 출력 `React.createElement("div")` (출력 확장자 `.js`)
  - `react-native`: 입력 `<div />` → 출력 `<div />` (출력 확장자 `.js`)
  - `react-jsx`: 입력 `<div />` → 출력 `_jsx("div", {}, void 0);` (출력 확장자 `.js`)
  - `react-jsxdev`: 입력 `<div />` → 출력 `_jsxDEV("div", {}, void 0, false, {...}, this);` (출력 확장자 `.js`)
- [`jsx`](/tsconfig#jsx) 커맨드 라인 플래그 또는 [tsconfig.json의 `jsx` 옵션](/tsconfig#jsx)으로 모드 지정 가능

> 참고: react JSX 방출을 대상으로 할 때 사용할 JSX 팩토리 함수는 [`jsxFactory`](/tsconfig#jsxFactory) 옵션으로 지정 가능(기본값은 `React.createElement`)

### `as` 연산자

- 타입 단언 작성 방법 예시:

```ts
const foo = <Foo>bar;
```

- 위 코드는 변수 `bar`가 `Foo` 타입을 갖도록 단언
- TypeScript도 타입 단언에 꺾쇠 괄호를 사용 → JSX 구문과 결합 시 파싱 어려움 발생 → `.tsx` 파일에서는 꺾쇠 괄호 타입 단언 금지
- 대체 타입 단언 연산자 `as` 사용 필요

```ts
const foo = bar as Foo;
```

- `as` 연산자는 `.ts`·`.tsx` 파일 모두에서 사용 가능 → 꺾쇠 괄호 타입 단언 스타일과 동작 동일

### 타입 검사

- JSX 타입 검사 이해의 전제: 내재 요소와 값 기반 요소의 차이 구분 필요
- JSX 표현식 `<expr />`에서 `expr`은 환경에 내재된 것(예: DOM 환경의 `div`, `span`) 또는 커스텀 컴포넌트를 참조 가능
- 이 구분이 중요한 이유
  - React의 경우 내재 요소는 문자열로 방출됨(`React.createElement("div")`) → 커스텀 컴포넌트는 그렇지 않음(`React.createElement(MyComponent)`)
  - JSX 요소에 전달되는 속성 타입은 조회 방식이 다름 → 내재 요소 속성은 _내재적으로_ 알려져야 함 · 컴포넌트는 자체 속성 세트를 지정
- TypeScript는 [React와 같은 규칙](http://facebook.github.io/react/docs/jsx-in-depth.html#html-tags-vs.-react-components)으로 구분: 내재 요소는 항상 소문자 시작, 값 기반 요소는 항상 대문자 시작

#### `JSX` 네임스페이스

- TypeScript의 JSX는 `JSX` 네임스페이스로 타입 지정 → `jsx` 컴파일러 옵션에 따라 정의 위치 다름
- `jsx` 옵션 `preserve`, `react`, `react-native`: 클래식 런타임 타입 정의 사용
  - `jsxFactory` 컴파일러 옵션이 결정하는 변수가 스코프에 있어야 함
  - `JSX` 네임스페이스는 JSX 팩토리의 최상위 식별자에 지정 필요
  - 예: React는 기본 팩토리 `React.createElement` 사용 → `JSX` 네임스페이스는 `React.JSX`로 정의 필요

```ts
export function createElement(): any;

export namespace JSX {
  // ...
}
```

- 사용자는 항상 React를 `React`로 임포트 필요

```ts
import * as React from 'react';
```

- Preact는 JSX 팩토리 `h` 사용 → 타입은 `h.JSX`로 정의 필요

```ts
export function h(props: any): any;

export namespace h.JSX {
  // ...
}
```

- 사용자는 `h`를 명명된 임포트로 사용 필요

```ts
import { h } from 'preact';
```

- `jsx` 옵션 `react-jsx`·`react-jsxdev`의 경우, `JSX` 네임스페이스는 일치하는 진입점에서 내보내야 함
  - `react-jsx`: 진입점은 `${jsxImportSource}/jsx-runtime`
  - `react-jsxdev`: 진입점은 `${jsxImportSource}/jsx-dev-runtime`
  - 두 진입점 모두 파일 확장자 미사용 → ESM 사용자 지원을 위해 `package.json`의 [`exports`](https://nodejs.org/api/packages.html#exports) 필드 맵 사용 필요

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

- `JSX` 네임스페이스 내보내기는 타입 검사에는 충분 → 단 프로덕션 런타임은 런타임에 `jsx`, `jsxs`, `Fragment` 내보내기 필요, 개발 런타임은 `jsxDEV`와 `Fragment` 필요 → 이들에 대한 타입 추가도 이상적
- `JSX` 네임스페이스를 적절한 위치에서 사용할 수 없는 경우 → 클래식·자동 런타임 모두 전역 `JSX` 네임스페이스로 폴백

#### 내재 요소

- 내재 요소는 특별한 인터페이스 `JSX.IntrinsicElements`에서 조회됨
  - 이 인터페이스가 지정되지 않으면 → 무엇이든 가능하고 내재 요소는 타입 검사 안 됨
  - 이 인터페이스가 _존재_하면 → 내재 요소의 이름은 `JSX.IntrinsicElements` 인터페이스의 속성으로 조회
- 예시:

```tsx
declare namespace JSX {
  interface IntrinsicElements {
    foo: any;
  }
}

<foo />; // ok
<bar />; // error
```

- 위 예제에서 `<foo />`는 정상 작동 · `<bar />`는 `JSX.IntrinsicElements`에 지정되지 않아 오류 발생

> 참고: 다음과 같이 `JSX.IntrinsicElements`에 포괄적인 문자열 인덱서를 지정할 수도 있음

```ts
declare namespace JSX {
  interface IntrinsicElements {
    [elemName: string]: any;
  }
}
```

#### 값 기반 요소

- 값 기반 요소는 스코프 내 식별자로 조회됨

```tsx
import MyComponent from "./myComponent";

<MyComponent />; // ok
<SomeOtherComponent />; // error
```

- 값 기반 요소를 정의하는 두 가지 방법
  1. 함수 컴포넌트(FC)
  2. 클래스 컴포넌트
- 두 유형은 JSX 표현식에서 서로 구별 불가 → TS의 해석 순서
  - 먼저 오버로드 해석으로 함수 컴포넌트 해석 시도 → 성공 시 해석 완료
  - 함수 컴포넌트로 해석 안 되면 → 클래스 컴포넌트로 해석 시도
  - 그것도 실패하면 → 오류 보고

##### 함수 컴포넌트

- 컴포넌트는 첫 번째 인수가 `props` 객체인 JavaScript 함수로 정의됨
- TS는 반환 타입이 `JSX.Element`에 할당 가능해야 함을 강제

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

- 함수 컴포넌트는 단순 JavaScript 함수 → 함수 오버로드도 사용 가능:

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

> 참고: 함수 컴포넌트는 이전에 상태 비저장 함수 컴포넌트(SFC)로 불림 → 최근 버전 React에서 함수 컴포넌트가 더 이상 상태 비저장으로 간주될 수 없어 `SFC` 타입과 별칭 `StatelessComponent`는 폐기됨

##### 클래스 컴포넌트

- 클래스 컴포넌트의 타입 정의 가능 → 필요 용어: _요소 클래스 타입_, _요소 인스턴스 타입_
- `<Expr />`이 주어지면 _요소 클래스 타입_은 `Expr`의 타입
  - `MyComponent`가 ES6 클래스라면 → 클래스 타입은 해당 클래스의 생성자·정적 멤버
  - `MyComponent`가 팩토리 함수라면 → 클래스 타입은 해당 함수
- 클래스 타입이 설정되면, 인스턴스 타입은 클래스 타입의 생성 또는 호출 시그니처(존재하는 것)의 반환 타입의 유니온으로 결정
  - ES6 클래스: 인스턴스 타입은 해당 클래스의 인스턴스 타입
  - 팩토리 함수: 인스턴스 타입은 함수에서 반환된 값의 타입

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

- 요소 인스턴스 타입은 `JSX.ElementClass`에 할당 가능해야 함 → 아니면 오류 발생
- 기본적으로 `JSX.ElementClass`는 `{}` → JSX 사용을 적절한 인터페이스를 따르는 타입으로만 제한하도록 확장 가능

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

#### 속성 타입 검사

- 속성 타입 검사의 첫 단계는 _요소 속성 타입_ 결정 → 내재 요소와 값 기반 요소 간 방식이 다름
- 내재 요소의 경우, `JSX.IntrinsicElements`의 속성 타입 사용

```tsx
declare namespace JSX {
  interface IntrinsicElements {
    foo: { bar?: boolean };
  }
}

// 'foo'에 대한 요소 속성 타입은 '{bar?: boolean}'입니다
<foo bar />;
```

- 값 기반 요소의 경우 더 복잡함
  - 이전에 결정된 _요소 인스턴스 타입_의 속성 타입으로 결정
  - 사용할 속성은 `JSX.ElementAttributesProperty`로 결정 → 단일 속성으로 선언 필요 → 해당 속성의 이름 사용
  - TypeScript 2.8부터, `JSX.ElementAttributesProperty`가 없으면 클래스 요소의 생성자 또는 함수 컴포넌트 호출의 첫 번째 매개변수 타입 대신 사용

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

- 요소 속성 타입은 JSX에서 속성 타입 검사에 사용 → 선택적·필수 속성 모두 지원

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

> 참고: 속성 이름이 유효한 JS 식별자가 아닌 경우(`data-*` 속성 등), 요소 속성 타입에서 찾을 수 없어도 오류로 간주하지 않음

- `JSX.IntrinsicAttributes` 인터페이스: 컴포넌트의 props나 인수에서 일반적으로 사용되지 않는 JSX 프레임워크 전용 추가 속성 지정에 사용 - 예: React의 `key`
- 제네릭 `JSX.IntrinsicClassAttributes<T>` 타입: 클래스 컴포넌트에만(함수 컴포넌트 제외) 동일 종류의 추가 속성 지정 가능 → 제네릭 매개변수는 클래스 인스턴스 타입에 해당
  - React에서는 `Ref<T>` 타입의 `ref` 속성 허용에 사용
- 이런 인터페이스의 모든 속성은 원칙적으로 선택적이어야 함 (JSX 프레임워크가 모든 태그에 특정 속성 제공을 의도적으로 강제하는 경우는 예외)
- 스프레드 연산자도 작동:

```tsx
const props = { requiredProp: "bar" };
<foo {...props} />; // ok

const badProps = {};
<foo {...badProps} />; // error
```

#### 자식 타입 검사

- TypeScript 2.3에서 _자식_ 타입 검사 도입
- _자식_: _요소 속성 타입_의 특별한 속성 → 자식 *JSXExpression*이 이 속성에 삽입됨
- TS가 `JSX.ElementAttributesProperty`로 _props_의 이름을 결정하듯, `JSX.ElementChildrenAttribute`로 해당 props 내 _자식_의 이름 결정
- `JSX.ElementChildrenAttribute`는 단일 속성으로 선언 필요

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

- 다른 속성과 마찬가지로 _자식_의 타입 지정 가능 → [React 타이핑](https://github.com/DefinitelyTyped/DefinitelyTyped/tree/master/types/react) 사용 시 기본 타입을 재정의

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

### JSX 결과 타입

- 기본적으로 JSX 표현식의 결과는 `any`로 타입 지정
- `JSX.Element` 인터페이스 지정으로 타입 커스터마이즈 가능 → 단 이 인터페이스에서 JSX의 요소·속성·자식에 대한 타입 정보 검색은 불가(블랙박스)

### JSX 함수 반환 타입

- 기본적으로 함수 컴포넌트는 `JSX.Element | null` 반환 필요 → 단 항상 런타임 동작을 나타내지는 않음
- TypeScript 5.1부터 `JSX.ElementType` 지정으로 유효한 JSX 컴포넌트 타입 재정의 가능
  - 어떤 props가 유효한지는 정의하지 않음 → props 타입은 항상 전달되는 컴포넌트의 첫 번째 인수로 정의
- 기본값:

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

### 표현식 삽입

- JSX는 중괄호(`{ }`)로 표현식을 둘러싸 태그 사이에 삽입 가능

```tsx
const a = (
  <div>
    {["foo", "bar"].map((i) => (
      <span>{i / 2}</span>
    ))}
  </div>
);
```

- 위 코드는 문자열을 숫자로 나눌 수 없어 오류 발생
- `preserve` 옵션 사용 시 출력:

```tsx
const a = (
  <div>
    {["foo", "bar"].map(function (i) {
      return <span>{i / 2}</span>;
    })}
  </div>
);
```

### React 통합

- React와 JSX 사용 시 [React 타이핑](https://github.com/DefinitelyTyped/DefinitelyTyped/tree/master/types/react) 사용 필요
- 이 타이핑은 React 사용을 위해 `JSX` 네임스페이스를 적절히 정의

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

#### JSX 구성

- JSX 커스터마이즈용 컴파일러 플래그 여러 개 존재 → 컴파일러 플래그·파일별 인라인 프래그마 모두로 작동 가능
- 상세 내용은 tsconfig 참조 페이지 참고:

- [`jsxFactory`](/tsconfig#jsxFactory)
- [`jsxFragmentFactory`](/tsconfig#jsxFragmentFactory)
- [`jsxImportSource`](/tsconfig#jsxImportSource)

---

## 데코레이터 (Decorators)

> **원문:** https://www.typescriptlang.org/docs/handbook/decorators.html

> 참고: 이 문서는 실험적인 stage 2 데코레이터 구현을 다룸. Stage 3 데코레이터 지원은 TypeScript 5.0부터 사용 가능
> 참조: [TypeScript 5.0의 데코레이터](https://devblogs.microsoft.com/typescript/announcing-typescript-5-0/#decorators)

### 소개

- TypeScript와 ES6에서 클래스 도입 → 클래스·클래스 멤버에 주석·수정을 지원할 추가 기능이 필요한 시나리오 존재
- 데코레이터는 클래스 선언과 멤버에 주석·메타프로그래밍 구문을 추가하는 방법 제공

> 추가 읽기 (stage 2): [TypeScript 데코레이터 완벽 가이드](https://saul-mirone.github.io/a-complete-guide-to-typescript-decorator/)

- 데코레이터의 실험적 지원 활성화: 커맨드 라인 또는 `tsconfig.json`에서 [`experimentalDecorators`](/tsconfig#experimentalDecorators) 컴파일러 옵션 활성화 필요

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

### 데코레이터

- _데코레이터_: [클래스 선언](#클래스-데코레이터)·[메서드](#메서드-데코레이터)·[접근자](#접근자-데코레이터)·[속성](#속성-데코레이터)·[매개변수](#매개변수-데코레이터)에 첨부 가능한 특별한 선언
- 형식은 `@expression` → `expression`은 데코레이트된 선언 정보와 함께 런타임에 호출될 함수로 평가 필요
- 예: 데코레이터 `@sealed`가 주어지면 다음처럼 `sealed` 함수 작성 가능

```ts
function sealed(target) {
  // 'target'으로 무언가를 합니다...
}
```

### 데코레이터 팩토리

- 데코레이터가 선언에 적용되는 방식을 커스터마이즈하려면 데코레이터 팩토리 작성 가능
- _데코레이터 팩토리_: 런타임에 데코레이터가 호출할 표현식을 반환하는 간단한 함수
- 작성 예:

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

### 데코레이터 합성

- 여러 데코레이터를 선언에 적용 가능. 한 줄 예:

```ts
@f @g x
```

- 여러 줄 예:

```ts
@f
@g
x
```

- 여러 데코레이터가 단일 선언에 적용될 때 평가는 [수학의 함수 합성](https://wikipedia.org/wiki/Function_composition)과 유사 → 함수 _f_와 _g_를 합성한 결과 (_f_ ∘ _g_)(_x_)는 _f_(_g_(_x_))와 동등
- TypeScript에서 단일 선언에 여러 데코레이터를 평가할 때 수행 단계
  1. 각 데코레이터의 표현식을 위에서 아래로 평가
  2. 그 결과를 아래에서 위로 함수로 호출
- [데코레이터 팩토리](#데코레이터-팩토리)로 이 평가 순서 관찰 가능:

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

- 콘솔 출력:

```shell
first(): factory evaluated
second(): factory evaluated
second(): called
first(): called
```

### 데코레이터 평가

- 클래스 내 다양한 선언에 데코레이터가 적용되는 순서
  1. _매개변수 데코레이터_·_메서드_·_접근자_·_속성 데코레이터_가 각 인스턴스 멤버에 적용
  2. _매개변수 데코레이터_·_메서드_·_접근자_·_속성 데코레이터_가 각 정적 멤버에 적용
  3. _매개변수 데코레이터_가 생성자에 적용
  4. _클래스 데코레이터_가 클래스에 적용

### 클래스 데코레이터

- _클래스 데코레이터_: 클래스 선언 바로 앞에 선언
- 클래스의 생성자에 적용 → 클래스 정의 관찰·수정·대체 용도
- 선언 파일이나 다른 앰비언트 컨텍스트(예: `declare` 클래스)에서 사용 불가
- 클래스 데코레이터의 표현식은 런타임에 함수로 호출 → 데코레이트된 클래스의 생성자가 유일한 인수로 전달
- 클래스 데코레이터가 값을 반환하면 → 제공된 생성자 함수로 클래스 선언 대체

> 참고: 새로운 생성자 함수를 반환하는 경우 원래 프로토타입 유지에 주의 필요
> 런타임에 데코레이터를 적용하는 로직은 이를 대신해주지 않음

- `BugReport` 클래스에 적용된 클래스 데코레이터(`@sealed`) 예:

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

- 다음 함수 선언으로 `@sealed` 데코레이터 정의 가능:

```ts
function sealed(constructor: Function) {
  Object.seal(constructor);
  Object.seal(constructor.prototype);
}
```

- `@sealed` 실행 시 생성자와 프로토타입을 모두 봉인 → 런타임에 `BugReport.prototype` 접근이나 `BugReport` 자체에 속성을 정의해 클래스에 기능을 추가·제거하는 것을 방지(ES2015 클래스는 프로토타입 기반 생성자 함수의 문법적 설탕일 뿐)
  - 이 데코레이터는 `BugReport`를 서브클래싱하는 것은 방지하지 않음
- 새 기본값 설정을 위해 생성자를 재정의하는 방법 예:

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

### 메서드 데코레이터

- _메서드 데코레이터_: 메서드 선언 바로 앞에 선언
- 메서드의 _속성 설명자_에 적용 → 메서드 정의 관찰·수정·대체 용도
- 선언 파일, 오버로드, 다른 앰비언트 컨텍스트(예: `declare` 클래스)에서 사용 불가
- 메서드 데코레이터의 표현식은 런타임에 다음 세 인수와 함께 함수로 호출
  1. 정적 멤버는 클래스의 생성자 함수, 인스턴스 멤버는 클래스의 프로토타입
  2. 멤버의 이름
  3. 멤버의 _속성 설명자_

> 참고: 스크립트 대상이 `ES5` 미만이면 _속성 설명자_는 `undefined`

- 메서드 데코레이터가 값을 반환하면 → 해당 메서드의 _속성 설명자_로 사용

> 참고: 스크립트 대상이 `ES5` 미만이면 반환 값 무시

- `Greeter` 클래스의 메서드에 적용된 메서드 데코레이터(`@enumerable`) 예:

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

- 다음 함수 선언으로 `@enumerable` 데코레이터 정의 가능:

```ts
function enumerable(value: boolean) {
  return function (target: any, propertyKey: string, descriptor: PropertyDescriptor) {
    descriptor.enumerable = value;
  };
}
```

- `@enumerable(false)` 데코레이터는 [데코레이터 팩토리](#데코레이터-팩토리) → 호출 시 속성 설명자의 `enumerable` 속성을 수정

### 접근자 데코레이터

- _접근자 데코레이터_: 접근자 선언 바로 앞에 선언
- 접근자의 _속성 설명자_에 적용 → 접근자 정의 관찰·수정·대체 용도
- 선언 파일이나 다른 앰비언트 컨텍스트(예: `declare` 클래스)에서 사용 불가

> 참고: TypeScript는 단일 멤버에 대해 `get`과 `set` 접근자를 모두 데코레이트하는 것을 허용하지 않음
> 대신 멤버에 대한 모든 데코레이터는 문서 순서상 첫 번째 접근자에 적용 필요
> 이유: 데코레이터가 `get`과 `set` 접근자를 별도로 결합하는 것이 아니라 _속성 설명자_에 적용되기 때문

- 접근자 데코레이터의 표현식은 런타임에 다음 세 인수와 함께 함수로 호출
  1. 정적 멤버는 클래스의 생성자 함수, 인스턴스 멤버는 클래스의 프로토타입
  2. 멤버의 이름
  3. 멤버의 _속성 설명자_

> 참고: 스크립트 대상이 `ES5` 미만이면 _속성 설명자_는 `undefined`

- 접근자 데코레이터가 값을 반환하면 → 해당 멤버의 _속성 설명자_로 사용

> 참고: 스크립트 대상이 `ES5` 미만이면 반환 값 무시

- `Point` 클래스의 멤버에 적용된 접근자 데코레이터(`@configurable`) 예:

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

- 다음 함수 선언으로 `@configurable` 데코레이터 정의 가능:

```ts
function configurable(value: boolean) {
  return function (target: any, propertyKey: string, descriptor: PropertyDescriptor) {
    descriptor.configurable = value;
  };
}
```

### 속성 데코레이터

- _속성 데코레이터_: 속성 선언 바로 앞에 선언
- 선언 파일이나 다른 앰비언트 컨텍스트(예: `declare` 클래스)에서 사용 불가
- 속성 데코레이터의 표현식은 런타임에 다음 두 인수와 함께 함수로 호출
  1. 정적 멤버는 클래스의 생성자 함수, 인스턴스 멤버는 클래스의 프로토타입
  2. 멤버의 이름

> 참고: TypeScript에서 속성 데코레이터가 초기화되는 방식 때문에 _속성 설명자_는 속성 데코레이터의 인수로 제공되지 않음
> 이유: 현재 프로토타입의 멤버를 정의할 때 인스턴스 속성을 설명하는 메커니즘이 없고, 속성의 이니셜라이저를 관찰·수정할 방법도 없음. 반환 값도 무시됨
> 따라서 속성 데코레이터는 특정 이름의 속성이 클래스에 선언되었음을 관찰하는 용도로만 사용 가능

- 이 정보로 다음 예제처럼 속성에 대한 메타데이터를 기록할 수 있음:

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

- 다음 함수 선언으로 `@format` 데코레이터와 `getFormat` 함수 정의 가능:

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

- `@format("Hello, %s")` 데코레이터는 [데코레이터 팩토리](#데코레이터-팩토리)
  - 호출 시 `reflect-metadata` 라이브러리의 `Reflect.metadata` 함수로 속성에 대한 메타데이터 항목 추가
  - `getFormat` 호출 시 형식에 대한 메타데이터 값을 읽음

> 참고: 이 예제는 `reflect-metadata` 라이브러리 필요
> 상세 내용은 [메타데이터](#메타데이터) 참고

### 매개변수 데코레이터

- _매개변수 데코레이터_: 매개변수 선언 바로 앞에 선언
- 클래스 생성자 또는 메서드 선언의 함수에 적용
- 선언 파일, 오버로드, 다른 앰비언트 컨텍스트(예: `declare` 클래스)에서 사용 불가
- 매개변수 데코레이터의 표현식은 런타임에 다음 세 인수와 함께 함수로 호출
  1. 정적 멤버는 클래스의 생성자 함수, 인스턴스 멤버는 클래스의 프로토타입
  2. 멤버의 이름
  3. 함수의 매개변수 목록에서 매개변수의 서수 인덱스

> 참고: 매개변수 데코레이터는 매개변수가 메서드에 선언되었음을 관찰하는 용도로만 사용 가능

- 매개변수 데코레이터의 반환 값은 무시됨
- `BugReport` 클래스 멤버의 매개변수에 적용된 매개변수 데코레이터(`@required`) 예:

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

- 다음 함수 선언으로 `@required`와 `@validate` 데코레이터 정의 가능:

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

- `@required` 데코레이터는 매개변수를 필수로 표시하는 메타데이터 항목 추가
- `@validate` 데코레이터는 기존 `print` 메서드를, 원래 메서드 호출 전 인수의 유효성을 검사하는 함수로 래핑

> 참고: 이 예제는 `reflect-metadata` 라이브러리 필요
> 상세 내용은 [메타데이터](#메타데이터) 참고

### 메타데이터

- 일부 예제는 [실험적 메타데이터 API](https://github.com/rbuckton/ReflectDecorators)의 폴리필을 추가하는 `reflect-metadata` 라이브러리 사용
- 이 라이브러리는 아직 ECMAScript(JavaScript) 표준의 일부가 아님 → 데코레이터가 공식적으로 ECMAScript 표준에 채택되면 이러한 확장도 채택 제안될 예정
- npm 설치:

```shell
npm i reflect-metadata --save
```

- TypeScript는 데코레이터가 있는 선언에 특정 유형의 메타데이터를 방출하는 실험적 지원 포함
- 활성화: 커맨드 라인이나 `tsconfig.json`에서 [`emitDecoratorMetadata`](/tsconfig#emitDecoratorMetadata) 컴파일러 옵션 설정 필요

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

- 활성화되면 `reflect-metadata` 라이브러리가 임포트된 한, 추가 설계 시간 타입 정보가 런타임에 노출됨
- 작동 예:

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

- TypeScript 컴파일러는 `@Reflect.metadata` 데코레이터로 설계 시간 타입 정보 주입 → 다음 TypeScript와 동등:

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

> 참고: 데코레이터 메타데이터는 실험적 기능 → 향후 릴리스에서 호환성이 깨지는 변경 가능

---

## 믹스인 (Mixins)

> **원문:** https://www.typescriptlang.org/docs/handbook/mixins.html

- 전통적인 OO 계층 구조와 함께, 더 간단한 부분 클래스를 결합해 재사용 가능한 컴포넌트로부터 클래스를 빌드하는 방법도 인기 있음
- Scala 등에서 믹스인·트레이트 아이디어에 익숙할 수 있음 → JavaScript 커뮤니티에서도 인기

### 믹스인은 어떻게 작동하나요?

- 이 패턴은 클래스 상속과 제네릭을 함께 사용해 베이스 클래스를 확장하는 것에 의존
- TypeScript의 가장 좋은 믹스인 지원은 클래스 표현식 패턴을 통해 수행
- JavaScript에서의 작동 방식은 [여기](https://justinfagnani.com/2015/12/21/real-mixins-with-javascript-classes/)에서 추가로 확인 가능
- 시작을 위해 믹스인이 위에 적용될 클래스 필요:

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

- 이어서 베이스 클래스를 확장하는 클래스 표현식을 반환하는 타입과 팩토리 함수 필요:

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

- 설정이 끝나면 믹스인이 적용된 베이스 클래스를 나타내는 클래스 생성 가능:

```ts
// 믹스인 Scale 적용자와 함께 Sprite 클래스에서
// 새 클래스를 구성합니다:
const EightBitSprite = Scale(Sprite);

const flappySprite = new EightBitSprite("Bird");
flappySprite.setScale(0.8);
console.log(flappySprite.scale);
```

### 제한된 믹스인

- 위 형식에서 믹스인은 클래스에 대한 기본 지식이 없어 원하는 디자인을 만들기 어려울 수 있음
- 해결: 원래 생성자 타입을 제네릭 인수를 받도록 수정

```ts
// 이것은 이전 생성자였습니다:
type Constructor = new (...args: any[]) => {};
// 이제 이 믹스인이 적용되는 클래스에 제약을 적용할 수 있는
// 제네릭 버전을 사용합니다
type GConstructor<T = {}> = new (...args: any[]) => T;
```

- 이를 통해 제한된 베이스 클래스에서만 작동하는 클래스 생성 가능:

```ts
type Positionable = GConstructor<{ setPos: (x: number, y: number) => void }>;
type Spritable = GConstructor<Sprite>;
type Loggable = GConstructor<{ print: () => void }>;
```

- 빌드할 특정 베이스가 있을 때만 작동하는 믹스인 생성 가능:

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

### 대안적 패턴

- 이 문서의 이전 버전에서는 런타임과 타입 계층 구조를 별도로 생성한 후 마지막에 병합하는 방식의 믹스인 작성을 권장:

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

- 이 패턴은 컴파일러에 덜 의존 → 런타임과 타입 시스템 동기화는 코드베이스가 더 책임

### 제약 사항

- 믹스인 패턴은 TypeScript 컴파일러 내 코드 흐름 분석으로 네이티브 지원됨
- 네이티브 지원의 한계에 도달하는 경우 존재

##### 데코레이터와 믹스인 [`#4881`](https://github.com/microsoft/TypeScript/issues/4881)

- 데코레이터로는 코드 흐름 분석을 통한 믹스인 제공 불가:

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

##### 정적 속성 믹스인 [`#17829`](https://github.com/microsoft/TypeScript/issues/17829)

- 제약보다는 함정에 해당
- 클래스 표현식 패턴은 싱글톤을 생성 → 다른 변수 타입을 지원하기 위해 타입 시스템에서 매핑 불가
- 해결: 제네릭에 따라 다른 클래스를 반환하는 함수 사용:

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

---

## JavaScript와 TypeScript

## TypeScript를 활용하는 JS 프로젝트

> **원문:** https://www.typescriptlang.org/docs/handbook/intro-to-js-ts.html

- TypeScript의 타입 시스템은 코드베이스 작업 시 다양한 엄격함 수준 지원
  - JavaScript 코드로만 추론에 기반한 타입 시스템
  - [JSDoc을 통한](/docs/handbook/jsdoc-supported-types.html) JavaScript에서의 점진적 타이핑
  - JavaScript 파일에서 `// @ts-check` 사용
  - TypeScript 코드
  - [`strict`](/tsconfig#strict)가 활성화된 TypeScript
- 각 단계는 더 안전한 타입 시스템으로의 이동을 의미 → 단 모든 프로젝트가 그 수준의 검증을 필요로 하지는 않음

### JavaScript와 함께하는 TypeScript

- 자동 완성, 심볼로 이동, 이름 바꾸기 같은 리팩토링 도구 제공을 위해 TypeScript를 사용하는 편집기를 쓰는 경우에 해당
- [홈페이지](/)에 TypeScript 플러그인을 가진 편집기 목록 존재

### JSDoc을 통한 JS에서의 타입 힌트 제공

- `.js` 파일에서 타입은 종종 추론 가능 → 추론 불가 시 JSDoc 구문으로 지정 가능
- 선언 앞에 오는 JSDoc 주석이 해당 선언의 타입을 설정. 예:

```js twoslash
/** @type {number} */
var x;

x = 0; // OK
x = false; // OK?!
```

- 지원되는 JSDoc 패턴 전체 목록: [JSDoc 지원 타입](/docs/handbook/jsdoc-supported-types.html)

### `@ts-check`

- 이전 코드 샘플의 마지막 줄은 TypeScript에서는 오류 발생 → JS 프로젝트에서는 기본적으로 오류 미발생
- JavaScript 파일에서 오류 활성화 방법: `.js` 파일 첫 줄에 `// @ts-check` 추가

```js twoslash
// @ts-check
// @errors: 2322
/** @type {number} */
var x;

x = 0; // OK
x = false; // Not OK
```

- 오류를 추가하려는 JavaScript 파일이 많다면 [`jsconfig.json`](/docs/handbook/tsconfig-json.html) 사용으로 전환 가능 → 특정 파일은 `// @ts-nocheck` 주석으로 검사 제외 가능
- TypeScript가 동의하기 어려운 오류를 낼 수도 있음 → 그런 경우 이전 줄에 `// @ts-ignore` 또는 `// @ts-expect-error` 추가로 해당 줄 오류 무시 가능

```js twoslash
// @ts-check
/** @type {number} */
var x;

x = 0; // OK
// @ts-expect-error
x = false; // Not OK
```

- TypeScript의 JavaScript 해석 방식 상세: [TS가 JS를 타입 체크하는 방법](/docs/handbook/type-checking-javascript-files.html)

---

## JavaScript 파일의 타입 검사

> **원문:** https://www.typescriptlang.org/docs/handbook/type-checking-javascript-files.html

- `.ts` 파일과 비교해 `.js` 파일에서 검사가 작동하는 방식에 몇 가지 주목할 차이점 존재

### 속성은 클래스 본문의 할당에서 추론됨

- ES2015에는 클래스에 속성을 선언하는 수단이 없음 → 속성은 객체 리터럴처럼 동적으로 할당
- `.js` 파일에서 컴파일러는 클래스 본문 내부의 속성 할당에서 속성을 추론
  - 속성의 타입: 생성자에서 주어진 타입 · 생성자에서 정의되지 않았거나 타입이 undefined/null인 경우 이러한 할당의 모든 오른쪽 값 타입의 유니온
  - 생성자에서 정의된 속성은 항상 존재한다고 가정 · 메서드/getter/setter에서만 정의된 것은 선택적으로 간주

```js twoslash
// @checkJs
// @errors: 2322
class C {
  constructor() {
    this.constructorOnly = 0;
    this.constructorUnknown = undefined;
  }
  method() {
    this.constructorOnly = false;
    this.constructorUnknown = "plunkbat"; // ok, constructorUnknown은 string | undefined
    this.methodOnly = "ok"; // ok, 하지만 methodOnly도 undefined일 수 있음
  }
  method2() {
    this.methodOnly = true; // 또한, ok, methodOnly의 타입은 string | boolean | undefined
  }
}
```

- 속성이 클래스 본문에서 절대 설정되지 않으면 unknown으로 간주
- 읽기만 하는 속성이 있는 경우, 타입 지정을 위해 JSDoc으로 생성자에서 선언을 추가하고 주석 필요 → 나중에 초기화될 경우 값을 줄 필요도 없음:

```js twoslash
// @checkJs
// @errors: 2322
class C {
  constructor() {
    /** @type {number | undefined} */
    this.prop = undefined;
    /** @type {number | undefined} */
    this.count;
  }
}

let c = new C();
c.prop = 0; // OK
c.count = "string";
```

### 생성자 함수는 클래스와 동등함

- ES2015 이전, JavaScript는 클래스 대신 생성자 함수 사용
- 컴파일러는 이 패턴을 지원 → 생성자 함수를 ES2015 클래스와 동등하게 이해 → 위 속성 추론 규칙이 동일하게 작동

```js twoslash
// @checkJs
// @errors: 2683 2322
function C() {
  this.constructorOnly = 0;
  this.constructorUnknown = undefined;
}
C.prototype.method = function () {
  this.constructorOnly = false;
  this.constructorUnknown = "plunkbat"; // OK, 타입은 string | undefined
};
```

### CommonJS 모듈 지원됨

- `.js` 파일에서 TypeScript는 CommonJS 모듈 형식 이해
  - `exports`, `module.exports`에 대한 할당은 내보내기 선언으로 인식
  - `require` 함수 호출은 모듈 가져오기로 인식
- 예:

```js
// `import module "fs"`와 동일
const fs = require("fs");

// `export function readFile`과 동일
module.exports.readFile = function (f) {
  return fs.readFileSync(f);
};
```

- JavaScript의 모듈 지원은 TypeScript보다 구문적으로 훨씬 관대 → 대부분의 할당·선언 조합 지원

### 클래스, 함수, 객체 리터럴은 네임스페이스임

- 클래스는 `.js` 파일에서 네임스페이스 → 예: 클래스 중첩에 사용 가능

```js twoslash
class C {}
C.D = class {};
```

- ES2015 이전 코드의 경우 정적 메서드 시뮬레이션에도 사용 가능:

```js twoslash
function Outer() {
  this.y = 2;
}

Outer.Inner = function () {
  this.yy = 2;
};

Outer.Inner();
```

- 간단한 네임스페이스 생성에도 사용 가능:

```js twoslash
var ns = {};
ns.C = class {};
ns.func = function () {};

ns;
```

- 다른 변형도 허용:

```js twoslash
// IIFE
var ns = (function (n) {
  return n || {};
})();
ns.CONST = 1;

// 전역으로 기본값
var assign =
  assign ||
  function () {
    // 코드가 여기 들어감
  };
assign.extra = 1;
```

### 객체 리터럴은 열린 구조임

- `.ts` 파일에서 변수 선언을 초기화하는 객체 리터럴은 선언에 타입을 부여 → 원래 리터럴에 없던 새 멤버 추가 불가
- `.js` 파일에서는 이 규칙이 완화 → 객체 리터럴은 원래 정의되지 않은 속성을 추가·조회할 수 있는 열린 구조 타입(인덱스 시그니처)을 가짐. 예:

```js twoslash
var obj = { a: 1 };
obj.b = 2; // 허용됨
```

- 객체 리터럴은 닫힌 객체 대신 열린 맵으로 취급 가능한 `[x:string]: any` 인덱스 시그니처를 가진 것처럼 동작
- 다른 특별한 JS 검사 동작과 마찬가지로, 변수에 JSDoc 타입을 지정해 이 동작 변경 가능. 예:

```js twoslash
// @checkJs
// @errors: 2339
/** @type {{a: number}} */
var obj = { a: 1 };
obj.b = 2;
```

### null, undefined, 빈 배열 초기화자는 any 또는 any[] 타입임

- null 또는 undefined로 초기화된 모든 변수·매개변수·속성은 strict null checks가 켜져 있어도 any 타입
- []로 초기화된 모든 변수·매개변수·속성은 strict null checks가 켜져 있어도 any[] 타입
- 유일한 예외: 위에서 설명한 여러 초기화자를 가진 속성

```js twoslash
function Foo(i = null) {
  if (!i) i = 1;
  var j = undefined;
  j = 2;
  this.l = [];
}

var foo = new Foo();
foo.l.push(foo.i);
foo.l.push("end");
```

### 함수 매개변수는 기본적으로 선택적임

- ES2015 이전 JavaScript에는 매개변수 선택성 지정 방법이 없음 → `.js` 파일의 모든 함수 매개변수는 선택적으로 간주 → 선언된 매개변수 수보다 적은 인수로 호출 허용
- 단 너무 많은 인수로 호출하는 것은 오류
- 예:

```js twoslash
// @checkJs
// @strict: false
// @errors: 7006 7006 2554
function bar(a, b) {
  console.log(a + " " + b);
}

bar(1); // OK, 두 번째 인수가 선택적으로 간주됨
bar(1, 2);
bar(1, 2, 3); // 오류, 인수가 너무 많음
```

- JSDoc으로 주석이 달린 함수는 이 규칙에서 제외 → JSDoc 선택적 매개변수 구문(`[` `]`)으로 선택성 표현. 예:

```js twoslash
/**
 * @param {string} [somebody] - 누군가의 이름.
 */
function sayHello(somebody) {
  if (!somebody) {
    somebody = "John Doe";
  }
  console.log("Hello " + somebody);
}

sayHello();
```

### `arguments` 사용에서 추론된 가변 인수 매개변수 선언

- 본문에 `arguments` 참조가 있는 함수는 암시적으로 가변 인수 매개변수(즉, `(...arg: any[]) => any`)를 가진 것으로 간주
- JSDoc 가변 인수 구문으로 인수 타입 지정 가능

```js twoslash
/** @param {...number} args */
function sum(/* numbers */) {
  var total = 0;
  for (var i = 0; i < arguments.length; i++) {
    total += arguments[i];
  }
  return total;
}
```

### 지정되지 않은 타입 매개변수는 기본적으로 `any`임

- JavaScript에는 제네릭 타입 매개변수를 지정하는 자연스러운 구문이 없음 → 지정되지 않은 타입 매개변수는 기본적으로 `any`

#### extends 절에서

- 예: `React.Component`는 `Props`, `State` 두 타입 매개변수를 가지도록 정의됨 → `.js` 파일에서는 extends 절에서 이를 지정할 합법적 방법이 없어 타입 인수는 기본적으로 `any`

```js
import { Component } from "react";

class MyComponent extends Component {
  render() {
    this.props.b; // 허용됨, this.props가 any 타입이므로
  }
}
```

- JSDoc `@augments`로 타입을 명시적으로 지정 가능. 예:

```js
import { Component } from "react";

/**
 * @augments {Component<{a: number}, State>}
 */
class MyComponent extends Component {
  render() {
    this.props.b; // 오류: b가 {a:number}에 존재하지 않음
  }
}
```

#### JSDoc 참조에서

- JSDoc의 지정되지 않은 타입 인수는 기본적으로 any:

```js twoslash
/** @type{Array} */
var x = [];

x.push(1); // OK
x.push("string"); // OK, x는 Array<any> 타입

/** @type{Array.<number>} */
var y = [];

y.push(1); // OK
y.push("string"); // 오류, string은 number에 할당할 수 없음
```

#### 함수 호출에서

- 제네릭 함수 호출은 인수로 타입 매개변수를 추론
- 추론 소스 부족 등으로 추론에 실패하는 경우 → 타입 매개변수는 기본적으로 `any`. 예:

```js
var p = new Promise((resolve, reject) => {
  reject();
});

p; // Promise<any>;
```

- JSDoc의 모든 기능은 [참조](/docs/handbook/jsdoc-supported-types.html) 참고

---

## JSDoc 참조

> **원문:** https://www.typescriptlang.org/docs/handbook/jsdoc-supported-types.html

- 아래 목록: JavaScript 파일에서 타입 정보 제공을 위한 JSDoc 주석의 현재 지원 구조
- 참고
  - 아래에 명시적으로 나열되지 않은 모든 태그(예: `@async`)는 아직 미지원
  - 문서 태그만 TypeScript 파일에서 지원 → 나머지 태그는 JavaScript 파일에서만 지원

##### 타입

- [`@type`](#type)
- [`@import`](#import)
- [`@param`](#param-및-returns) (또는 [`@arg`](#param-및-returns) 또는 [`@argument`](#param-및-returns))
- [`@returns`](#param-및-returns) (또는 [`@return`](#param-및-returns))
- [`@typedef`](#typedef-callback-및-param)
- [`@callback`](#typedef-callback-및-param)
- [`@template`](#template)
- [`@satisfies`](#satisfies)

##### 클래스

- [속성 수정자](#속성-수정자) `@public`, `@private`, `@protected`, `@readonly`
- [`@override`](#override)
- [`@extends`](#extends) (또는 [`@augments`](#extends))
- [`@implements`](#implements)
- [`@class`](#constructor) (또는 [`@constructor`](#constructor))
- [`@this`](#this)

##### 문서

- 문서 태그는 TypeScript와 JavaScript 모두에서 작동

- [`@deprecated`](#deprecated)
- [`@see`](#see)
- [`@link`](#link)

##### 기타

- [`@enum`](#enum)
- [`@author`](#author)
- [기타 지원되는 패턴](#기타-지원되는-패턴)
- [지원되지 않는 패턴](#지원되지-않는-패턴)
- [지원되지 않는 태그](#지원되지-않는-태그)

- 의미는 일반적으로 [jsdoc.app](https://jsdoc.app)의 해당 태그 의미와 동일하거나 그 상위 집합
- 아래 코드로 차이점 설명 및 태그별 사용 예 제공

> 참고: [플레이그라운드에서 JSDoc 지원을 탐색](/play?useJavaScript=truee=4#example/jsdoc-support) 가능

### 타입

#### `@type`

- `@type` 태그로 타입 참조 가능. 타입은 다음 중 하나
  1. `string`, `number` 같은 원시 타입
  2. TypeScript 선언에서 선언된 것(전역·가져온 것 모두 포함)
  3. JSDoc [`@typedef`](#typedef-callback-및-param) 태그에서 선언된 것
- 대부분의 JSDoc 타입 구문과 모든 TypeScript 구문 사용 가능 → [`string` 같은 기본](/docs/handbook/2/basic-types.html)부터 [조건부 타입 같은 고급](/docs/handbook/2/conditional-types.html)까지

```js twoslash
/**
 * @type {string}
 */
var s;

/** @type {Window} */
var win;

/** @type {PromiseLike<string>} */
var promisedString;

// DOM 속성가 있는 HTML 요소를 지정할 수 있습니다
/** @type {HTMLElement} */
var myElement = document.querySelector(selector);
element.dataset.myData = "";
```

- `@type`은 유니온 타입 지정 가능 &mdash; 예: string이거나 boolean일 수 있음

```js twoslash
/**
 * @type {string | boolean}
 */
var sb;
```

- 다양한 구문으로 배열 타입 지정 가능:

```js twoslash
/** @type {number[]} */
var ns;
/** @type {Array.<number>} */
var jsdoc;
/** @type {Array<number>} */
var nas;
```

- 객체 리터럴 타입도 지정 가능. 예: 'a'(string), 'b'(number) 속성이 있는 객체는 다음 구문 사용:

```js twoslash
/** @type {{ a: string, b: number }} */
var var9;
```

- 표준 JSDoc 구문 또는 TypeScript 구문으로, 문자열·숫자 인덱스 시그니처를 가진 맵 유사 객체·배열 유사 객체 지정 가능:

```js twoslash
/**
 * 임의의 `string` 속성를 `number`에 매핑하는 맵과 같은 객체.
 *
 * @type {Object.<string, number>}
 */
var stringToNumber;

/** @type {Object.<number, object>} */
var arrayLike;
```

- 위 두 타입은 TypeScript 타입 `{ [x: string]: number }`, `{ [x: number]: any }`와 동등 → 컴파일러는 두 구문 모두 이해
- TypeScript 또는 Google Closure 구문으로 함수 타입 지정 가능:

```js twoslash
/** @type {function(string, boolean): number} Closure 구문 */
var sbn;
/** @type {(s: string, b: boolean) => number} TypeScript 구문 */
var sbn2;
```

- 또는 지정되지 않은 `Function` 타입 그대로 사용 가능:

```js twoslash
/** @type {Function} */
var fn7;
/** @type {function} */
var fn6;
```

- Closure의 다른 타입도 작동:

```js twoslash
/**
 * @type {*} - 'any' 타입일 수 있음
 */
var star;
/**
 * @type {?} - 알 수 없는 타입 ('any'와 동일)
 */
var question;
```

##### 캐스트

- TypeScript는 Google Closure에서 캐스트 구문을 빌려옴
- 괄호로 묶인 표현식 앞에 `@type` 태그를 추가해 타입을 다른 타입으로 캐스팅 가능

```js twoslash
/**
 * @type {number | string}
 */
var numberOrString = Math.random() < 0.5 ? "hello" : 100;
var typeAssertedNumber = /** @type {number} */ (numberOrString);
```

- TypeScript처럼 `const`로 캐스팅도 가능:

```js twoslash
let one = /** @type {const} */(1);
```

##### Import 타입

- import 타입으로 다른 파일에서 선언을 가져올 수 있음
- 이 구문은 TypeScript 전용 → JSDoc 표준과 다름:

```js twoslash
// @filename: types.d.ts
export type Pet = {
  name: string,
};

// @filename: main.js
/**
 * @param {import("./types").Pet} p
 */
function walk(p) {
  console.log(`Walking ${p.name}...`);
}
```

- 타입을 모르거나 타이핑이 귀찮을 정도로 큰 타입이 있는 경우, import 타입으로 모듈에서 값의 타입을 가져올 수 있음:

```js twoslash
// @filename: accounts.d.ts
export const userAccount = {
  name: "Name",
  address: "An address",
  postalCode: "",
  country: "",
  planet: "",
  system: "",
  galaxy: "",
  universe: "",
};
// @filename: main.js
// ---cut---
/**
 * @type {typeof import("./accounts").userAccount}
 */
var x = require("./accounts").userAccount;
```

#### `@import`

- `@import` 태그로 다른 파일에서 내보내기를 참조 가능

```js twoslash
// @filename: types.d.ts
export type Pet = {
  name: string,
};
// @filename: main.js
// ---cut---
/**
 * @import {Pet} from "./types"
 */

/**
 * @type {Pet}
 */
var myPet;
myPet.name;
```

- 이 태그들은 실제로 런타임에 파일을 가져오지 않음 → 도입되는 심볼은 타입 검사를 위해 JSDoc 주석 내에서만 사용 가능

#### `@param` 및 `@returns`

- `@param`은 `@type`과 동일한 타입 구문 사용 + 매개변수 이름 추가
- 매개변수는 이름을 대괄호로 묶어 선택적으로 선언 가능:

```js twoslash
// 매개변수는 다양한 구문 형식으로 선언될 수 있습니다
/**
 * @param {string}  p1 - string 매개변수.
 * @param {string=} p2 - 선택적 매개변수 (Google Closure 구문)
 * @param {string} [p3] - 다른 선택적 매개변수 (JSDoc 구문).
 * @param {string} [p4="test"] - 기본값이 있는 선택적 매개변수
 * @returns {string} 이것은 결과입니다
 */
function stringsStringStrings(p1, p2, p3, p4) {
  // TODO
}
```

- 함수의 반환 타입도 마찬가지:

```js twoslash
/**
 * @return {PromiseLike<string>}
 */
function ps() {}

/**
 * @returns {{ a: string, b: number }} - '@return'도 '@returns'처럼 사용할 수 있음
 */
function ab() {}
```

#### `@typedef`, `@callback`, 및 `@param`

- `@typedef`로 복잡한 타입 정의 가능 → 유사한 구문이 `@param`에서도 작동

```js twoslash
/**
 * @typedef {Object} SpecialType - 'SpecialType'이라는 새 타입을 만듦
 * @property {string} prop1 - SpecialType의 string 속성
 * @property {number} prop2 - SpecialType의 number 속성
 * @property {number=} prop3 - SpecialType의 선택적 number 속성
 * @prop {number} [prop4] - SpecialType의 선택적 number 속성
 * @prop {number} [prop5=42] - 기본값이 있는 SpecialType의 선택적 number 속성
 */

/** @type {SpecialType} */
var specialTypeObject;
specialTypeObject.prop3;
```

- 첫 줄에 `object` 또는 `Object` 사용 가능
- `@callback`은 `@typedef`와 유사하나 객체 타입 대신 함수 타입 지정:

```js twoslash
/**
 * @callback Predicate
 * @param {string} data
 * @param {number} [index]
 * @returns {boolean}
 */

/** @type {Predicate} */
const ok = (s) => !(s.length % 2);
```

- 이러한 타입은 한 줄 `@typedef`에서 TypeScript 구문으로도 선언 가능:

```js
/** @typedef {{ prop1: string, prop2: string, prop3?: number }} SpecialType */
/** @typedef {(data: string, index?: number) => boolean} Predicate */
```

#### `@template`

- `@template` 태그로 타입 매개변수 선언 가능 → 제네릭 함수·클래스·타입 생성 가능:

```js twoslash
/**
 * @template T
 * @param {T} x - 반환 타입으로 흐르는 제네릭 매개변수
 * @returns {T}
 */
function id(x) {
  return x;
}

const a = id("string");
const b = id(123);
const c = id({});
```

- 쉼표 또는 여러 태그로 여러 타입 매개변수 선언:

```js
/**
 * @template T,U,V
 * @template W,X
 */
```

- 타입 매개변수 이름 앞에 타입 제약 조건 지정 가능 → 목록의 첫 번째 타입 매개변수만 제약됨:

```js twoslash
/**
 * @template {string} K - K는 문자열 또는 문자열 리터럴이어야 함
 * @template {{ serious(): string }} Seriousalizable - serious 메서드가 있어야 함
 * @param {K} key
 * @param {Seriousalizable} object
 */
function seriousalize(key, object) {
  // ????
}
```

- 타입 매개변수에 기본값 지정도 가능:

```js twoslash
/** @template [T=object] */
class Cache {
    /** @param {T} initial */
    constructor(initial) {
    }
}
let c = new Cache()
```

#### `@satisfies`

- `@satisfies`는 TypeScript의 후위 [연산자 `satisfies`](/docs/handbook/release-notes/typescript-4-9.html) 접근 제공
- Satisfies는 값이 타입을 구현한다고 선언 → 값의 타입 자체에는 영향 없음

```js twoslash
// @errors: 1360
// @ts-check
/**
 * @typedef {"hello world" | "Hello, world"} WelcomeMessage
 */

/** @satisfies {WelcomeMessage} */
const message = "hello world"
//     ^?

/** @satisfies {WelcomeMessage} */
const failingMessage = "Hello world!"

/** @type {WelcomeMessage} */
const messageUsingType = "hello world"
//     ^?
```

### 클래스

- 클래스는 ES6 클래스로 선언 가능

```js twoslash
class C {
  /**
   * @param {number} data
   */
  constructor(data) {
    // 속성 타입을 추론할 수 있음
    this.name = "foo";

    // 또는 명시적으로 설정
    /** @type {string | null} */
    this.title = null;

    // 또는 다른 곳에서 설정되는 경우 단순히 주석 달기
    /** @type {number} */
    this.size;

    this.initialize(data); // 오류가 발생해야 함, initializer는 string을 기대함
  }
  /**
   * @param {string} s
   */
  initialize = function (s) {
    this.size = s.length;
  };
}

var c = new C(0);

// C는 new로만 호출해야 하지만,
// JavaScript이므로 이것이 허용되고
// 'any'로 간주됨.
var result = C(1);
```

- 생성자 함수로도 선언 가능 → [`@constructor`](#constructor)와 [`@this`](#this) 함께 사용

#### 속성 수정자

- `@public`, `@private`, `@protected`는 TypeScript의 `public`, `private`, `protected`와 동일하게 작동:

```js twoslash
// @errors: 2341
// @ts-check

class Car {
  constructor() {
    /** @private */
    this.identifier = 100;
  }

  printIdentifier() {
    console.log(this.identifier);
  }
}

const c = new Car();
console.log(c.identifier);
```

- `@public`: 항상 암시되어 생략 가능 → 속성이 어디서나 접근 가능함을 의미
- `@private`: 속성이 포함 클래스 내에서만 사용 가능함을 의미
- `@protected`: 속성이 포함 클래스와 모든 파생 하위 클래스 내에서 사용 가능하나, 포함 클래스의 다른 인스턴스에서는 사용 불가함을 의미

#### `@readonly`

- `@readonly` 수정자는 속성이 초기화 중에만 쓰이도록 보장

```js twoslash
// @errors: 2540
// @ts-check

class Car {
  constructor() {
    /** @readonly */
    this.identifier = 100;
  }

  printIdentifier() {
    console.log(this.identifier);
  }
}

const c = new Car();
console.log(c.identifier);
```

#### `@override`

- `@override`는 TypeScript와 동일하게 작동 → 기본 클래스의 메서드를 재정의하는 메서드에 사용:

```js twoslash
export class C {
  m() { }
}
class D extends C {
  /** @override */
  m() { }
}
```

- 재정의 확인: tsconfig에서 `noImplicitOverride: true` 설정 필요

#### `@extends`

- JavaScript 클래스가 제네릭 기본 클래스를 확장할 때, 타입 인수 전달을 위한 JavaScript 구문이 없음 → `@extends` 태그가 이를 가능하게 함:

```js twoslash
/**
 * @template T
 * @extends {Set<T>}
 */
class SortableSet extends Set {
  // ...
}
```

- `@extends`는 클래스에서만 작동 → 현재 생성자 함수가 클래스를 확장하는 방법은 없음

#### `@implements`

- TypeScript 인터페이스 구현을 위한 JavaScript 구문도 없음 → `@implements` 태그가 TypeScript와 동일하게 작동:

```js twoslash
/** @implements {Print} */
class TextBook {
  print() {
    // TODO
  }
}
```

#### `@constructor`

- 컴파일러는 this-속성 할당 기반으로 생성자 함수를 추론 → `@constructor` 태그 추가 시 검사를 더 엄격히 하고 제안 품질도 향상:

```js twoslash
// @checkJs
// @errors: 2345 2348
/**
 * @constructor
 * @param {number} data
 */
function C(data) {
  // 속성 타입을 추론할 수 있음
  this.name = "foo";

  // 또는 명시적으로 설정
  /** @type {string | null} */
  this.title = null;

  // 또는 다른 곳에서 설정되는 경우 단순히 주석 달기
  /** @type {number} */
  this.size;

  this.initialize(data);
}
/**
 * @param {string} s
 */
C.prototype.initialize = function (s) {
  this.size = s.length;
};

var c = new C(0);
c.size;

var result = C(1);
```

- `@constructor` 사용 시 생성자 함수 `C` 내부의 `this`가 검사됨 → `initialize` 메서드에 대한 제안을 받고 숫자를 전달하면 오류 발생
  - 편집기는 `C`를 생성 대신 호출하면 경고를 표시할 수도 있음

#### `@this`

- 컴파일러는 일반적으로 작업 컨텍스트가 있으면 `this`의 타입을 파악 가능 → 그렇지 않은 경우 `@this`로 명시적 지정 가능:

```js twoslash
/**
 * @this {HTMLElement}
 * @param {*} e
 */
function callbackForLater(e) {
  this.clientHeight = parseInt(e); // 괜찮아야 함!
}
```

### 문서

#### `@deprecated`

- 함수·메서드·속성이 더 이상 사용되지 않을 때 `/** @deprecated */` JSDoc 주석으로 표시 → 사용자에게 알림
- 해당 정보는 완성 목록과 편집기가 특별히 처리 가능한 제안 진단으로 표시
- VS Code 등 편집기에서 더 이상 사용되지 않는 값은 일반적으로 ~~이것처럼~~ 취소선 스타일로 표시

```js twoslash
// @noErrors
/** @deprecated */
const apiV1 = {};
const apiV2 = {};

apiV;
// ^|
```

#### `@see`

- `@see`로 프로그램의 다른 이름에 링크 가능:

```ts twoslash
type Box<T> = { t: T }
/** @see Box 구현 세부사항은 Box 참조 */
type Boxify<T> = { [K in keyof T]: Box<T> };
```

- 일부 편집기는 `Box`를 링크로 바꿔 쉬운 이동·복귀를 지원

#### `@link`

- `@link`는 `@see`와 비슷하나 다른 태그 내부에서도 사용 가능:

```ts twoslash
type Box<T> = { t: T }
/** @returns 매개변수를 포함하는 {@link Box}. */
function box<U>(u: U): Box<U> {
  return { t: u };
}
```

### 기타

#### `@enum`

- `@enum` 태그로 멤버가 모두 지정된 타입인 객체 리터럴 생성 가능 → JavaScript의 대부분 객체 리터럴과 달리 다른 멤버는 허용 안 함
- `@enum`은 Google Closure의 `@enum` 태그와의 호환성을 위한 것

```js twoslash
/** @enum {number} */
const JSDocState = {
  BeginningOfLine: 0,
  SawAsterisk: 1,
  SavingComments: 2,
};

JSDocState.SawAsterisk;
```

- `@enum`은 TypeScript의 `enum`과 상당히 다르고 훨씬 간단 → 단 TypeScript 열거형과 달리 `@enum`은 어떤 타입이든 가질 수 있음:

```js twoslash
/** @enum {function(number): number} */
const MathFuncs = {
  add1: (n) => n + 1,
  id: (n) => -n,
  sub1: (n) => n - 1,
};

MathFuncs.add1;
```

#### `@author`

- `@author`로 항목의 저자 지정 가능:

```ts twoslash
/**
 * Welcome to awesome.ts
 * @author Ian Awesome <i.am.awesome@example.com>
 */
```

- 이메일 주소는 꺾쇠 괄호로 묶어야 함 → 아니면 `@example`이 새 태그로 파싱됨

#### 기타 지원되는 패턴

```js twoslash
var someObj = {
  /**
   * @param {string} param1 - 속성 할당의 JSDoc도 작동함
   */
  x: function (param1) {},
};

/**
 * 변수 할당의 jsdoc도 마찬가지
 * @return {Window}
 */
let someFunc = function () {};

/**
 * 클래스 메서드도
 * @param {string} greeting 사용할 인사말
 */
Foo.prototype.sayHi = (greeting) => console.log("Hi!");

/**
 * 화살표 함수 표현식도
 * @param {number} x - 곱셈기
 */
let myArrow = (x) => x * x;

/**
 * JSX의 함수 컴포넌트에도 작동함을 의미함
 * @param {{a: string, b: number}} props - 일부 매개변수
 */
var fc = (props) => <div>{props.a.charAt(0)}</div>;

/**
 * 매개변수는 Google Closure 구문을 사용하여 클래스 생성자일 수 있음.
 *
 * @param {{new(...args: any[]): object}} C - 등록할 클래스
 */
function registerClass(C) {}

/**
 * @param {...string} p1 - 문자열의 'rest' 인수 (배열). ('any'로 취급됨)
 */
function fn10(p1) {}

/**
 * @param {...string} p1 - 문자열의 'rest' 인수 (배열). ('any'로 취급됨)
 */
function fn9(p1) {
  return p1.join();
}
```

#### 지원되지 않는 패턴

- 객체 리터럴 타입의 속성 타입에 후위 equals는 선택적 속성을 지정하지 않음:

```js twoslash
/**
 * @type {{ a: string, b: number= }}
 */
var wrong;
/**
 * 대신 속성 이름에 후위 물음표를 사용하세요:
 * @type {{ a: string, b?: number }}
 */
var right;
```

- Nullable 타입은 [`strictNullChecks`](/tsconfig#strictNullChecks)가 켜져 있을 때만 의미 있음:

```js twoslash
/**
 * @type {?number}
 * strictNullChecks: true  -- number | null
 * strictNullChecks: false -- number
 */
var nullable;
```

- TypeScript 네이티브 구문은 유니온 타입:

```js twoslash
/**
 * @type {number | null}
 * strictNullChecks: true  -- number | null
 * strictNullChecks: false -- number
 */
var unionNullable;
```

- Non-nullable 타입은 의미가 없고 원래 타입으로 취급됨:

```js twoslash
/**
 * @type {!number}
 * number 타입만 가짐
 */
var normal;
```

- JSDoc의 타입 시스템과 달리 TypeScript는 타입이 null을 포함하는지 여부만 표시 가능 → 명시적 non-nullability는 없음
  - strictNullChecks가 켜져 있으면 `number`는 nullable 아님 · 꺼져 있으면 `number`는 nullable

#### 지원되지 않는 태그

- TypeScript는 지원되지 않는 JSDoc 태그를 무시
- 다음 태그들은 지원을 위한 열린 이슈 존재

- `@memberof` ([이슈 #7237](https://github.com/Microsoft/TypeScript/issues/7237))
- `@yields` ([이슈 #23857](https://github.com/Microsoft/TypeScript/issues/23857))
- `@member` ([이슈 #56674](https://github.com/microsoft/TypeScript/issues/56674))

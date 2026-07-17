# JavaScript와 TypeScript

# TypeScript를 활용하는 JS 프로젝트

> **원문:** https://www.typescriptlang.org/docs/handbook/intro-to-js-ts.html

TypeScript의 타입 시스템은 코드베이스로 작업할 때 다양한 수준의 엄격함을 가집니다:

- JavaScript 코드로만 추론에 기반한 타입 시스템
- [JSDoc을 통한](/docs/handbook/jsdoc-supported-types.html) JavaScript에서의 점진적 타이핑
- JavaScript 파일에서 `// @ts-check` 사용
- TypeScript 코드
- [`strict`](/tsconfig#strict)가 활성화된 TypeScript

각 단계는 더 안전한 타입 시스템을 향한 이동을 나타내지만, 모든 프로젝트가 그 수준의 검증을 필요로 하는 것은 아닙니다.

## JavaScript와 함께하는 TypeScript

이것은 자동 완성, 심볼로 이동, 이름 바꾸기와 같은 리팩토링 도구를 제공하기 위해 TypeScript를 사용하는 편집기를 사용할 때입니다. [홈페이지](/)에는 TypeScript 플러그인을 가진 편집기 목록이 있습니다.

## JSDoc을 통한 JS에서의 타입 힌트 제공

`.js` 파일에서 타입은 종종 추론될 수 있습니다. 타입을 추론할 수 없는 경우, JSDoc 구문을 사용하여 지정할 수 있습니다.

선언 앞에 오는 JSDoc 주석은 해당 선언의 타입을 설정하는 데 사용됩니다. 예를 들어:

```js twoslash
/** @type {number} */
var x;

x = 0; // OK
x = false; // OK?!
```

지원되는 JSDoc 패턴의 전체 목록은 [JSDoc 지원 타입](/docs/handbook/jsdoc-supported-types.html)에서 찾을 수 있습니다.

## `@ts-check`

이전 코드 샘플의 마지막 줄은 TypeScript에서 오류를 발생시키지만, JS 프로젝트에서는 기본적으로 그렇지 않습니다. JavaScript 파일에서 오류를 활성화하려면 `.js` 파일의 첫 번째 줄에 `// @ts-check`를 추가하여 TypeScript가 이를 오류로 발생시키도록 하세요.

```js twoslash
// @ts-check
// @errors: 2322
/** @type {number} */
var x;

x = 0; // OK
x = false; // Not OK
```

오류를 추가하려는 JavaScript 파일이 많다면 [`jsconfig.json`](/docs/handbook/tsconfig-json.html)을 사용하도록 전환할 수 있습니다. 파일에 `// @ts-nocheck` 주석을 추가하여 일부 파일의 검사를 건너뛸 수 있습니다.

TypeScript가 동의하지 않는 오류를 제공할 수 있으며, 그런 경우 이전 줄에 `// @ts-ignore` 또는 `// @ts-expect-error`를 추가하여 특정 줄의 오류를 무시할 수 있습니다.

```js twoslash
// @ts-check
/** @type {number} */
var x;

x = 0; // OK
// @ts-expect-error
x = false; // Not OK
```

TypeScript가 JavaScript를 어떻게 해석하는지 더 알아보려면 [TS가 JS를 타입 체크하는 방법](/docs/handbook/type-checking-javascript-files.html)을 읽어보세요.

---

# JavaScript 파일의 타입 검사

> **원문:** https://www.typescriptlang.org/docs/handbook/type-checking-javascript-files.html

`.ts` 파일과 비교하여 `.js` 파일에서 검사가 작동하는 방식에 대한 몇 가지 주목할 만한 차이점이 있습니다.

## 속성는 클래스 본문의 할당에서 추론됩니다

ES2015에는 클래스에 속성를 선언하는 수단이 없습니다. 속성는 객체 리터럴처럼 동적으로 할당됩니다.

`.js` 파일에서 컴파일러는 클래스 본문 내부의 속성 할당에서 속성를 추론합니다. 속성의 타입은 생성자에서 주어진 타입이거나, 생성자에서 정의되지 않았거나 생성자의 타입이 undefined 또는 null인 경우입니다. 그런 경우, 타입은 이러한 할당의 모든 오른쪽 값 타입의 유니온입니다. 생성자에서 정의된 속성는 항상 존재한다고 가정되는 반면, 메서드, getter, setter에서만 정의된 것은 선택적으로 간주됩니다.

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

속성가 클래스 본문에서 절대 설정되지 않으면 unknown으로 간주됩니다. 클래스에 읽기만 하는 속성가 있는 경우, 타입을 지정하기 위해 JSDoc으로 생성자에서 선언을 추가하고 주석을 달아야 합니다. 나중에 초기화될 경우 값을 줄 필요도 없습니다:

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

## 생성자 함수는 클래스와 동등합니다

ES2015 이전에, JavaScript는 클래스 대신 생성자 함수를 사용했습니다. 컴파일러는 이 패턴을 지원하고 생성자 함수를 ES2015 클래스와 동등하게 이해합니다. 위에서 설명한 속성 추론 규칙은 정확히 같은 방식으로 작동합니다.

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

## CommonJS 모듈이 지원됩니다

`.js` 파일에서 TypeScript는 CommonJS 모듈 형식을 이해합니다. `exports`와 `module.exports`에 대한 할당은 내보내기 선언으로 인식됩니다. 마찬가지로, `require` 함수 호출은 모듈 가져오기로 인식됩니다. 예를 들어:

```js
// `import module "fs"`와 동일
const fs = require("fs");

// `export function readFile`과 동일
module.exports.readFile = function (f) {
  return fs.readFileSync(f);
};
```

JavaScript의 모듈 지원은 TypeScript의 모듈 지원보다 구문적으로 훨씬 관대합니다. 대부분의 할당과 선언 조합이 지원됩니다.

## 클래스, 함수, 객체 리터럴은 네임스페이스입니다

클래스는 `.js` 파일에서 네임스페이스입니다. 이것은 예를 들어 클래스를 중첩하는 데 사용할 수 있습니다:

```js twoslash
class C {}
C.D = class {};
```

그리고, ES2015 이전 코드의 경우, 정적 메서드를 시뮬레이션하는 데 사용할 수 있습니다:

```js twoslash
function Outer() {
  this.y = 2;
}

Outer.Inner = function () {
  this.yy = 2;
};

Outer.Inner();
```

간단한 네임스페이스를 만드는 데에도 사용할 수 있습니다:

```js twoslash
var ns = {};
ns.C = class {};
ns.func = function () {};

ns;
```

다른 변형도 허용됩니다:

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

## 객체 리터럴은 열린 구조입니다

`.ts` 파일에서 변수 선언을 초기화하는 객체 리터럴은 선언에 타입을 부여합니다. 원래 리터럴에 지정되지 않은 새 멤버는 추가할 수 없습니다. 이 규칙은 `.js` 파일에서 완화됩니다; 객체 리터럴은 원래 정의되지 않은 속성를 추가하고 찾을 수 있게 하는 열린 구조 타입(인덱스 시그니처)을 가집니다. 예를 들어:

```js twoslash
var obj = { a: 1 };
obj.b = 2; // 허용됨
```

객체 리터럴은 닫힌 객체 대신 열린 맵으로 취급될 수 있게 하는 `[x:string]: any` 인덱스 시그니처를 가진 것처럼 동작합니다.

다른 특별한 JS 검사 동작과 마찬가지로, 이 동작은 변수에 대한 JSDoc 타입을 지정하여 변경할 수 있습니다. 예를 들어:

```js twoslash
// @checkJs
// @errors: 2339
/** @type {{a: number}} */
var obj = { a: 1 };
obj.b = 2;
```

## null, undefined, 빈 배열 초기화자는 any 또는 any[] 타입입니다

null 또는 undefined로 초기화된 모든 변수, 매개변수, 속성는 strict null checks가 켜져 있더라도 any 타입을 갖습니다. []로 초기화된 모든 변수, 매개변수, 속성는 strict null checks가 켜져 있더라도 any[] 타입을 갖습니다. 유일한 예외는 위에서 설명한 대로 여러 초기화자를 가진 속성입니다.

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

## 함수 매개변수는 기본적으로 선택적입니다

ES2015 이전 JavaScript에서는 매개변수에 선택성을 지정하는 방법이 없으므로, `.js` 파일의 모든 함수 매개변수는 선택적으로 간주됩니다. 선언된 매개변수 수보다 적은 인수로 호출하는 것이 허용됩니다.

너무 많은 인수로 함수를 호출하는 것은 오류라는 점에 유의하는 것이 중요합니다.

예를 들어:

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

JSDoc으로 주석이 달린 함수는 이 규칙에서 제외됩니다. JSDoc 선택적 매개변수 구문(`[` `]`)을 사용하여 선택성을 표현하세요. 예:

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

## `arguments` 사용에서 추론된 가변 인수 매개변수 선언

본문에 `arguments` 참조가 있는 함수는 암시적으로 가변 인수 매개변수(즉, `(...arg: any[]) => any`)를 가진 것으로 간주됩니다. JSDoc 가변 인수 구문을 사용하여 인수의 타입을 지정하세요.

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

## 지정되지 않은 타입 매개변수는 기본적으로 `any`입니다

JavaScript에서 제네릭 타입 매개변수를 지정하는 자연스러운 구문이 없으므로, 지정되지 않은 타입 매개변수는 기본적으로 `any`입니다.

### extends 절에서

예를 들어, `React.Component`는 `Props`와 `State`라는 두 개의 타입 매개변수를 가지도록 정의되어 있습니다. `.js` 파일에서는 extends 절에서 이를 지정할 합법적인 방법이 없습니다. 기본적으로 타입 인수는 `any`가 됩니다:

```js
import { Component } from "react";

class MyComponent extends Component {
  render() {
    this.props.b; // 허용됨, this.props가 any 타입이므로
  }
}
```

JSDoc `@augments`를 사용하여 타입을 명시적으로 지정하세요. 예:

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

### JSDoc 참조에서

JSDoc의 지정되지 않은 타입 인수는 기본적으로 any입니다:

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

### 함수 호출에서

제네릭 함수 호출은 인수를 사용하여 타입 매개변수를 추론합니다. 때때로 이 프로세스는 주로 추론 소스 부족 때문에 타입을 추론하지 못합니다; 이러한 경우, 타입 매개변수는 기본적으로 `any`가 됩니다. 예를 들어:

```js
var p = new Promise((resolve, reject) => {
  reject();
});

p; // Promise<any>;
```

JSDoc에서 사용 가능한 모든 기능을 알아보려면 [참조](/docs/handbook/jsdoc-supported-types.html)를 참조하세요.

---

# JSDoc 참조

> **원문:** https://www.typescriptlang.org/docs/handbook/jsdoc-supported-types.html

아래 목록은 JavaScript 파일에서 타입 정보를 제공하기 위해 JSDoc 주석을 사용할 때 현재 지원되는 구조를 설명합니다.

참고:
- 아래에 명시적으로 나열되지 않은 모든 태그(예: `@async`)는 아직 지원되지 않습니다.
- 문서 태그만 TypeScript 파일에서 지원됩니다. 나머지 태그는 JavaScript 파일에서만 지원됩니다.

#### 타입

- [`@type`](#type)
- [`@import`](#import)
- [`@param`](#param-및-returns) (또는 [`@arg`](#param-및-returns) 또는 [`@argument`](#param-및-returns))
- [`@returns`](#param-및-returns) (또는 [`@return`](#param-및-returns))
- [`@typedef`](#typedef-callback-및-param)
- [`@callback`](#typedef-callback-및-param)
- [`@template`](#template)
- [`@satisfies`](#satisfies)

#### 클래스

- [속성 수정자](#속성-수정자) `@public`, `@private`, `@protected`, `@readonly`
- [`@override`](#override)
- [`@extends`](#extends) (또는 [`@augments`](#extends))
- [`@implements`](#implements)
- [`@class`](#constructor) (또는 [`@constructor`](#constructor))
- [`@this`](#this)

#### 문서

문서 태그는 TypeScript와 JavaScript 모두에서 작동합니다.

- [`@deprecated`](#deprecated)
- [`@see`](#see)
- [`@link`](#link)

#### 기타

- [`@enum`](#enum)
- [`@author`](#author)
- [기타 지원되는 패턴](#기타-지원되는-패턴)
- [지원되지 않는 패턴](#지원되지-않는-패턴)
- [지원되지 않는 태그](#지원되지-않는-태그)

의미는 일반적으로 [jsdoc.app](https://jsdoc.app)에서 주어진 태그의 의미와 동일하거나 상위 집합입니다. 아래 코드는 차이점을 설명하고 각 태그의 사용 예를 제공합니다.

**참고:** [플레이그라운드에서 JSDoc 지원을 탐색](/play?useJavaScript=truee=4#example/jsdoc-support)할 수 있습니다.

## 타입

### `@type`

"@type" 태그로 타입을 참조할 수 있습니다. 타입은 다음일 수 있습니다:

1. `string`이나 `number`와 같은 원시 타입.
2. TypeScript 선언에서 선언된 것, 전역이든 가져온 것이든.
3. JSDoc [`@typedef`](#typedef-callback-및-param) 태그에서 선언된 것.

대부분의 JSDoc 타입 구문과 모든 TypeScript 구문을 사용할 수 있습니다. [`string`과 같은 가장 기본적인 것](/docs/handbook/2/basic-types.html)부터 [조건부 타입과 같은 가장 고급](/docs/handbook/2/conditional-types.html)까지.

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

`@type`은 유니온 타입을 지정할 수 있습니다 &mdash; 예를 들어, 무언가가 string이거나 boolean일 수 있습니다.

```js twoslash
/**
 * @type {string | boolean}
 */
var sb;
```

다양한 구문을 사용하여 배열 타입을 지정할 수 있습니다:

```js twoslash
/** @type {number[]} */
var ns;
/** @type {Array.<number>} */
var jsdoc;
/** @type {Array<number>} */
var nas;
```

객체 리터럴 타입도 지정할 수 있습니다.
예를 들어, 'a'(string) 및 'b'(number) 속성가 있는 객체는 다음 구문을 사용합니다:

```js twoslash
/** @type {{ a: string, b: number }} */
var var9;
```

표준 JSDoc 구문 또는 TypeScript 구문을 사용하여 문자열 및 숫자 인덱스 시그니처로 맵과 같은 객체와 배열과 같은 객체를 지정할 수 있습니다.

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

앞의 두 타입은 TypeScript 타입 `{ [x: string]: number }` 및 `{ [x: number]: any }`와 동등합니다. 컴파일러는 두 구문을 모두 이해합니다.

TypeScript 또는 Google Closure 구문을 사용하여 함수 타입을 지정할 수 있습니다:

```js twoslash
/** @type {function(string, boolean): number} Closure 구문 */
var sbn;
/** @type {(s: string, b: boolean) => number} TypeScript 구문 */
var sbn2;
```

또는 지정되지 않은 `Function` 타입을 그냥 사용할 수 있습니다:

```js twoslash
/** @type {Function} */
var fn7;
/** @type {function} */
var fn6;
```

Closure의 다른 타입도 작동합니다:

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

#### 캐스트

TypeScript는 Google Closure에서 캐스트 구문을 빌려옵니다.
이를 통해 괄호로 묶인 표현식 앞에 `@type` 태그를 추가하여 타입을 다른 타입으로 캐스팅할 수 있습니다.

```js twoslash
/**
 * @type {number | string}
 */
var numberOrString = Math.random() < 0.5 ? "hello" : 100;
var typeAssertedNumber = /** @type {number} */ (numberOrString);
```

TypeScript처럼 `const`로 캐스팅할 수도 있습니다:

```js twoslash
let one = /** @type {const} */(1);
```

#### Import 타입

import 타입을 사용하여 다른 파일에서 선언을 가져올 수 있습니다.
이 구문은 TypeScript 전용이며 JSDoc 표준과 다릅니다:

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

import 타입은 타입을 모르거나 타이핑하기 귀찮을 정도로 큰 타입이 있는 경우 모듈에서 값의 타입을 가져오는 데 사용할 수 있습니다:

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

### `@import`

`@import` 태그를 사용하면 다른 파일에서 내보내기를 참조할 수 있습니다.

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

이 태그들은 실제로 런타임에 파일을 가져오지 않으며, 도입되는 심볼은 타입 검사를 위해 JSDoc 주석 내에서만 사용할 수 있습니다.

### `@param` 및 `@returns`

`@param`은 `@type`과 동일한 타입 구문을 사용하지만 매개변수 이름을 추가합니다.
매개변수는 이름을 대괄호로 묶어 선택적으로 선언할 수도 있습니다:

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

마찬가지로, 함수의 반환 타입에 대해:

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

### `@typedef`, `@callback`, 및 `@param`

`@typedef`로 복잡한 타입을 정의할 수 있습니다.
유사한 구문이 `@param`에서도 작동합니다.

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

첫 번째 줄에 `object` 또는 `Object`를 사용할 수 있습니다.

`@callback`은 `@typedef`와 유사하지만, 객체 타입 대신 함수 타입을 지정합니다:

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

물론, 이러한 타입 중 하나라도 한 줄 `@typedef`에서 TypeScript 구문을 사용하여 선언할 수 있습니다:

```js
/** @typedef {{ prop1: string, prop2: string, prop3?: number }} SpecialType */
/** @typedef {(data: string, index?: number) => boolean} Predicate */
```

### `@template`

`@template` 태그로 타입 매개변수를 선언할 수 있습니다.
이를 통해 제네릭 함수, 클래스, 타입을 만들 수 있습니다:

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

쉼표 또는 여러 태그를 사용하여 여러 타입 매개변수를 선언합니다:

```js
/**
 * @template T,U,V
 * @template W,X
 */
```

타입 매개변수 이름 앞에 타입 제약 조건을 지정할 수도 있습니다.
목록의 첫 번째 타입 매개변수만 제약됩니다:

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

마지막으로, 타입 매개변수에 기본값을 지정할 수 있습니다:

```js twoslash
/** @template [T=object] */
class Cache {
    /** @param {T} initial */
    constructor(initial) {
    }
}
let c = new Cache()
```

### `@satisfies`

`@satisfies`는 TypeScript의 후위 [연산자 `satisfies`](/docs/handbook/release-notes/typescript-4-9.html)에 대한 접근을 제공합니다. Satisfies는 값이 타입을 구현한다고 선언하는 데 사용되지만 값의 타입에 영향을 미치지 않습니다.

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

## 클래스

클래스는 ES6 클래스로 선언할 수 있습니다.

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

생성자 함수로도 선언할 수 있습니다; 이를 위해 [`@constructor`](#constructor)와 [`@this`](#this)를 함께 사용하세요.

### 속성 수정자

`@public`, `@private`, `@protected`는 TypeScript의 `public`, `private`, `protected`와 정확히 같이 작동합니다:

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

- `@public`은 항상 암시되며 생략할 수 있지만, 속성가 어디에서나 접근할 수 있음을 의미합니다.
- `@private`는 속성가 포함 클래스 내에서만 사용할 수 있음을 의미합니다.
- `@protected`는 속성가 포함 클래스와 모든 파생 하위 클래스 내에서만 사용할 수 있지만, 포함 클래스의 서로 다른 인스턴스에서는 사용할 수 없음을 의미합니다.

### `@readonly`

`@readonly` 수정자는 속성가 초기화 중에만 쓰여지도록 보장합니다.

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

### `@override`

`@override`는 TypeScript에서와 동일하게 작동합니다; 기본 클래스의 메서드를 재정의하는 메서드에 사용하세요:

```js twoslash
export class C {
  m() { }
}
class D extends C {
  /** @override */
  m() { }
}
```

재정의를 확인하려면 tsconfig에서 `noImplicitOverride: true`를 설정하세요.

### `@extends`

JavaScript 클래스가 제네릭 기본 클래스를 확장할 때, 타입 인수를 전달하기 위한 JavaScript 구문이 없습니다. `@extends` 태그가 이것을 가능하게 합니다:

```js twoslash
/**
 * @template T
 * @extends {Set<T>}
 */
class SortableSet extends Set {
  // ...
}
```

`@extends`는 클래스에서만 작동합니다. 현재 생성자 함수가 클래스를 확장하는 방법은 없습니다.

### `@implements`

마찬가지로, TypeScript 인터페이스를 구현하기 위한 JavaScript 구문이 없습니다. `@implements` 태그는 TypeScript에서와 마찬가지로 작동합니다:

```js twoslash
/** @implements {Print} */
class TextBook {
  print() {
    // TODO
  }
}
```

### `@constructor`

컴파일러는 this-속성 할당을 기반으로 생성자 함수를 추론하지만, `@constructor` 태그를 추가하면 검사를 더 엄격하게 하고 제안을 더 잘 할 수 있습니다:

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

`@constructor`를 사용하면 생성자 함수 `C` 내부의 `this`가 검사되므로, `initialize` 메서드에 대한 제안을 받고 숫자를 전달하면 오류가 발생합니다. 편집기는 `C`를 생성하는 대신 호출하면 경고를 표시할 수도 있습니다.

### `@this`

컴파일러는 일반적으로 작업할 컨텍스트가 있을 때 `this`의 타입을 파악할 수 있습니다. 그렇지 않은 경우, `@this`로 `this`의 타입을 명시적으로 지정할 수 있습니다:

```js twoslash
/**
 * @this {HTMLElement}
 * @param {*} e
 */
function callbackForLater(e) {
  this.clientHeight = parseInt(e); // 괜찮아야 함!
}
```

## 문서

### `@deprecated`

함수, 메서드, 속성가 더 이상 사용되지 않을 때 `/** @deprecated */` JSDoc 주석으로 표시하여 사용자에게 알릴 수 있습니다. 해당 정보는 완성 목록과 편집기가 특별히 처리할 수 있는 제안 진단으로 표시됩니다. VS Code와 같은 편집기에서 더 이상 사용되지 않는 값은 일반적으로 ~~이것처럼~~ 취소선 스타일로 표시됩니다.

```js twoslash
// @noErrors
/** @deprecated */
const apiV1 = {};
const apiV2 = {};

apiV;
// ^|
```

### `@see`

`@see`를 사용하면 프로그램의 다른 이름에 링크할 수 있습니다:

```ts twoslash
type Box<T> = { t: T }
/** @see Box 구현 세부사항은 Box 참조 */
type Boxify<T> = { [K in keyof T]: Box<T> };
```

일부 편집기는 `Box`를 링크로 바꿔서 쉽게 이동하고 돌아올 수 있게 합니다.

### `@link`

`@link`는 `@see`와 비슷하지만, 다른 태그 내부에서 사용할 수 있습니다:

```ts twoslash
type Box<T> = { t: T }
/** @returns 매개변수를 포함하는 {@link Box}. */
function box<U>(u: U): Box<U> {
  return { t: u };
}
```

## 기타

### `@enum`

`@enum` 태그를 사용하면 멤버가 모두 지정된 타입인 객체 리터럴을 만들 수 있습니다. JavaScript의 대부분의 객체 리터럴과 달리 다른 멤버를 허용하지 않습니다.
`@enum`은 Google Closure의 `@enum` 태그와의 호환성을 위해 의도되었습니다.

```js twoslash
/** @enum {number} */
const JSDocState = {
  BeginningOfLine: 0,
  SawAsterisk: 1,
  SavingComments: 2,
};

JSDocState.SawAsterisk;
```

`@enum`은 TypeScript의 `enum`과 상당히 다르고 훨씬 간단합니다. 그러나 TypeScript의 열거형과 달리 `@enum`은 어떤 타입이든 가질 수 있습니다:

```js twoslash
/** @enum {function(number): number} */
const MathFuncs = {
  add1: (n) => n + 1,
  id: (n) => -n,
  sub1: (n) => n - 1,
};

MathFuncs.add1;
```

### `@author`

`@author`로 항목의 저자를 지정할 수 있습니다:

```ts twoslash
/**
 * Welcome to awesome.ts
 * @author Ian Awesome <i.am.awesome@example.com>
 */
```

이메일 주소를 꺾쇠 괄호로 묶어야 합니다. 그렇지 않으면 `@example`이 새 태그로 파싱됩니다.

### 기타 지원되는 패턴

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

### 지원되지 않는 패턴

객체 리터럴 타입의 속성 타입에 후위 equals는 선택적 속성를 지정하지 않습니다:

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

Nullable 타입은 [`strictNullChecks`](/tsconfig#strictNullChecks)가 켜져 있을 때만 의미가 있습니다:

```js twoslash
/**
 * @type {?number}
 * strictNullChecks: true  -- number | null
 * strictNullChecks: false -- number
 */
var nullable;
```

TypeScript 네이티브 구문은 유니온 타입입니다:

```js twoslash
/**
 * @type {number | null}
 * strictNullChecks: true  -- number | null
 * strictNullChecks: false -- number
 */
var unionNullable;
```

Non-nullable 타입은 의미가 없으며 원래 타입으로 취급됩니다:

```js twoslash
/**
 * @type {!number}
 * number 타입만 가짐
 */
var normal;
```

JSDoc의 타입 시스템과 달리, TypeScript는 타입이 null을 포함하는지 여부만 표시할 수 있습니다. 명시적 non-nullability가 없습니다 -- strictNullChecks가 켜져 있으면 `number`는 nullable이 아닙니다. 꺼져 있으면 `number`는 nullable입니다.

### 지원되지 않는 태그

TypeScript는 지원되지 않는 JSDoc 태그를 무시합니다.

다음 태그들에는 지원을 위한 열린 이슈가 있습니다:

- `@memberof` ([이슈 #7237](https://github.com/Microsoft/TypeScript/issues/7237))
- `@yields` ([이슈 #23857](https://github.com/Microsoft/TypeScript/issues/23857))
- `@member` ([이슈 #56674](https://github.com/microsoft/TypeScript/issues/56674))

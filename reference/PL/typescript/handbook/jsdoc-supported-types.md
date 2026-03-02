# JSDoc 참조

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

- [프로퍼티 수정자](#프로퍼티-수정자) `@public`, `@private`, `@protected`, `@readonly`
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

// DOM 프로퍼티가 있는 HTML 요소를 지정할 수 있습니다
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
예를 들어, 'a'(string) 및 'b'(number) 프로퍼티가 있는 객체는 다음 구문을 사용합니다:

```js twoslash
/** @type {{ a: string, b: number }} */
var var9;
```

표준 JSDoc 구문 또는 TypeScript 구문을 사용하여 문자열 및 숫자 인덱스 시그니처로 맵과 같은 객체와 배열과 같은 객체를 지정할 수 있습니다.

```js twoslash
/**
 * 임의의 `string` 프로퍼티를 `number`에 매핑하는 맵과 같은 객체.
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
 * @property {string} prop1 - SpecialType의 string 프로퍼티
 * @property {number} prop2 - SpecialType의 number 프로퍼티
 * @property {number=} prop3 - SpecialType의 선택적 number 프로퍼티
 * @prop {number} [prop4] - SpecialType의 선택적 number 프로퍼티
 * @prop {number} [prop5=42] - 기본값이 있는 SpecialType의 선택적 number 프로퍼티
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
    // 프로퍼티 타입을 추론할 수 있음
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

### 프로퍼티 수정자

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

- `@public`은 항상 암시되며 생략할 수 있지만, 프로퍼티가 어디에서나 접근할 수 있음을 의미합니다.
- `@private`는 프로퍼티가 포함 클래스 내에서만 사용할 수 있음을 의미합니다.
- `@protected`는 프로퍼티가 포함 클래스와 모든 파생 하위 클래스 내에서만 사용할 수 있지만, 포함 클래스의 서로 다른 인스턴스에서는 사용할 수 없음을 의미합니다.

### `@readonly`

`@readonly` 수정자는 프로퍼티가 초기화 중에만 쓰여지도록 보장합니다.

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

컴파일러는 this-프로퍼티 할당을 기반으로 생성자 함수를 추론하지만, `@constructor` 태그를 추가하면 검사를 더 엄격하게 하고 제안을 더 잘 할 수 있습니다:

```js twoslash
// @checkJs
// @errors: 2345 2348
/**
 * @constructor
 * @param {number} data
 */
function C(data) {
  // 프로퍼티 타입을 추론할 수 있음
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

함수, 메서드, 프로퍼티가 더 이상 사용되지 않을 때 `/** @deprecated */` JSDoc 주석으로 표시하여 사용자에게 알릴 수 있습니다. 해당 정보는 완성 목록과 편집기가 특별히 처리할 수 있는 제안 진단으로 표시됩니다. VS Code와 같은 편집기에서 더 이상 사용되지 않는 값은 일반적으로 ~~이것처럼~~ 취소선 스타일로 표시됩니다.

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
   * @param {string} param1 - 프로퍼티 할당의 JSDoc도 작동함
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

객체 리터럴 타입의 프로퍼티 타입에 후위 equals는 선택적 프로퍼티를 지정하지 않습니다:

```js twoslash
/**
 * @type {{ a: string, b: number= }}
 */
var wrong;
/**
 * 대신 프로퍼티 이름에 후위 물음표를 사용하세요:
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

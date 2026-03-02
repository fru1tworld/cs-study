# JavaScript 파일의 타입 검사

`.ts` 파일과 비교하여 `.js` 파일에서 검사가 작동하는 방식에 대한 몇 가지 주목할 만한 차이점이 있습니다.

## 프로퍼티는 클래스 본문의 할당에서 추론됩니다

ES2015에는 클래스에 프로퍼티를 선언하는 수단이 없습니다. 프로퍼티는 객체 리터럴처럼 동적으로 할당됩니다.

`.js` 파일에서 컴파일러는 클래스 본문 내부의 프로퍼티 할당에서 프로퍼티를 추론합니다. 프로퍼티의 타입은 생성자에서 주어진 타입이거나, 생성자에서 정의되지 않았거나 생성자의 타입이 undefined 또는 null인 경우입니다. 그런 경우, 타입은 이러한 할당의 모든 오른쪽 값 타입의 유니온입니다. 생성자에서 정의된 프로퍼티는 항상 존재한다고 가정되는 반면, 메서드, getter, setter에서만 정의된 것은 선택적으로 간주됩니다.

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

프로퍼티가 클래스 본문에서 절대 설정되지 않으면 unknown으로 간주됩니다. 클래스에 읽기만 하는 프로퍼티가 있는 경우, 타입을 지정하기 위해 JSDoc으로 생성자에서 선언을 추가하고 주석을 달아야 합니다. 나중에 초기화될 경우 값을 줄 필요도 없습니다:

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

ES2015 이전에, JavaScript는 클래스 대신 생성자 함수를 사용했습니다. 컴파일러는 이 패턴을 지원하고 생성자 함수를 ES2015 클래스와 동등하게 이해합니다. 위에서 설명한 프로퍼티 추론 규칙은 정확히 같은 방식으로 작동합니다.

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

`.ts` 파일에서 변수 선언을 초기화하는 객체 리터럴은 선언에 타입을 부여합니다. 원래 리터럴에 지정되지 않은 새 멤버는 추가할 수 없습니다. 이 규칙은 `.js` 파일에서 완화됩니다; 객체 리터럴은 원래 정의되지 않은 프로퍼티를 추가하고 찾을 수 있게 하는 열린 구조 타입(인덱스 시그니처)을 가집니다. 예를 들어:

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

null 또는 undefined로 초기화된 모든 변수, 매개변수, 프로퍼티는 strict null checks가 켜져 있더라도 any 타입을 갖습니다. []로 초기화된 모든 변수, 매개변수, 프로퍼티는 strict null checks가 켜져 있더라도 any[] 타입을 갖습니다. 유일한 예외는 위에서 설명한 대로 여러 초기화자를 가진 프로퍼티입니다.

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

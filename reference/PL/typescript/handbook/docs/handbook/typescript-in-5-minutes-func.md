# 함수형 프로그래머를 위한 TypeScript

TypeScript는 전통적인 객체 지향 타입을 JavaScript에 가져와서 Microsoft의 프로그래머들이 전통적인 객체 지향 프로그램을 웹에 가져올 수 있도록 하려는 시도로 시작했습니다. TypeScript가 발전하면서, 타입 시스템은 네이티브 JavaScripter들이 작성하는 코드를 모델링하도록 진화했습니다. 결과적인 시스템은 강력하고, 흥미롭고, 복잡합니다.

이 소개는 TypeScript를 배우고자 하는 Haskell이나 ML 프로그래머를 위해 설계되었습니다. TypeScript의 타입 시스템이 Haskell의 타입 시스템과 어떻게 다른지 설명합니다. 또한 JavaScript 코드 모델링에서 발생하는 TypeScript 타입 시스템의 고유한 특징도 설명합니다.

이 소개는 객체 지향 프로그래밍을 다루지 않습니다. 실제로 TypeScript의 객체 지향 프로그램은 OO 기능을 가진 다른 인기 있는 언어들과 유사합니다.

## 전제 조건

이 소개에서는 다음을 알고 있다고 가정합니다:

- JavaScript의 좋은 부분으로 프로그래밍하는 방법.

- C 계열 언어의 타입 문법.

JavaScript의 좋은 부분을 배워야 한다면, [JavaScript: The Good Parts](https://shop.oreilly.com/product/9780596517748.do)를 읽으세요.
많은 가변성과 그 외에는 별로 없는 값에 의한 호출, 어휘적 스코핑 언어로 프로그램을 작성하는 방법을 알고 있다면 책을 건너뛸 수 있을 것입니다.
[R4RS Scheme](https://people.csail.mit.edu/jaffer/r4rs.pdf)이 좋은 예입니다.

[The C++ Programming Language](http://www.stroustrup.com/4th.html)는 C 스타일 타입 문법을 배우기 좋은 곳입니다. C++과 달리, TypeScript는 접미사 타입을 사용합니다. 예: `string x` 대신 `x: string`.

## Haskell에 없는 개념

### 내장 타입

JavaScript는 8가지 내장 타입을 정의합니다:

| 타입 | 설명 |
|------|------|
| `Number` | 배정밀도 IEEE 754 부동소수점. |
| `String` | 불변 UTF-16 문자열. |
| `BigInt` | 임의 정밀도 형식의 정수. |
| `Boolean` | `true`와 `false`. |
| `Symbol` | 주로 키로 사용되는 고유한 값. |
| `Null` | 유닛 타입과 동등. |
| `Undefined` | 역시 유닛 타입과 동등. |
| `Object` | 레코드와 유사. |

[더 자세한 내용은 MDN 페이지를 참조하세요](https://developer.mozilla.org/docs/Web/JavaScript/Data_structures).

TypeScript는 내장 타입에 대응하는 원시 타입을 가지고 있습니다:

- `number`
- `string`
- `bigint`
- `boolean`
- `symbol`
- `null`
- `undefined`
- `object`

#### 다른 중요한 TypeScript 타입

| 타입 | 설명 |
|------|------|
| `unknown` | 최상위 타입. |
| `never` | 최하위 타입. |
| 객체 리터럴 | 예: `{ property: Type }` |
| `void` | 문서화된 반환 값이 없는 함수용 |
| `T[]` | 가변 배열, `Array<T>`로도 작성 |
| `[T, T]` | 튜플, 고정 길이지만 가변 |
| `(t: T) => U` | 함수 |

참고:

- 함수 문법은 매개변수 이름을 포함합니다. 이것은 익숙해지기 꽤 어렵습니다!

```ts
let fst: (a: any, b: any) => any = (a, b) => a;

// 또는 더 정확하게:
let fst: <T, U>(a: T, b: U) => T = (a, b) => a;
```

- 객체 리터럴 타입 문법은 객체 리터럴 값 문법을 거의 그대로 반영합니다:

```ts
let o: { n: number; xs: object[] } = { n: 1, xs: [] };
```

- `[T, T]`는 `T[]`의 하위 타입입니다. 이것은 튜플이 리스트와 관련이 없는 Haskell과 다릅니다.

#### 박싱된 타입

JavaScript에는 프로그래머들이 해당 타입과 연관시키는 메서드를 포함하는 원시 타입의 박싱된 등가물이 있습니다. TypeScript는 이것을 반영하여, 예를 들어 원시 타입 `number`와 박싱된 타입 `Number` 사이의 차이를 둡니다. 박싱된 타입은 메서드가 원시 타입을 반환하기 때문에 거의 필요하지 않습니다.

```ts
(1).toExponential();
// 다음과 동등
Number.prototype.toExponential.call(1);
```

숫자 리터럴에서 메서드를 호출하려면 파서를 돕기 위해 괄호 안에 넣어야 한다는 점에 주의하세요.

### 점진적 타이핑

TypeScript는 표현식의 타입이 무엇이어야 하는지 알 수 없을 때마다 `any` 타입을 사용합니다. `Dynamic`과 비교하여 `any`를 타입이라고 부르는 것은 과장입니다. 그것은 나타나는 곳마다 타입 검사기를 끄는 것입니다. 예를 들어, 값을 어떤 방식으로도 표시하지 않고 `any[]`에 어떤 값이든 푸시할 수 있습니다:

```ts
// tsconfig.json에서 "noImplicitAny": false인 경우, anys: any[]
const anys = [];
anys.push(1);
anys.push("oh no");
anys.push({ anything: "goes" });
```

그리고 `any` 타입의 표현식을 어디서든 사용할 수 있습니다:

```ts
anys.map(anys[1]); // oh no, "oh no"는 함수가 아닙니다
```

`any`는 전염성이 있습니다 - `any` 타입의 표현식으로 변수를 초기화하면, 변수도 `any` 타입을 가집니다.

```ts
let sepsis = anys[0] + anys[1]; // 이것은 무엇이든 될 수 있습니다
```

TypeScript가 `any`를 생성할 때 오류를 얻으려면, `tsconfig.json`에서 `"noImplicitAny": true` 또는 `"strict": true`를 사용하세요.

### 구조적 타이핑

구조적 타이핑은 대부분의 함수형 프로그래머에게 익숙한 개념이지만, Haskell과 대부분의 ML은 구조적으로 타이핑되지 않습니다. 기본 형태는 꽤 간단합니다:

```ts
// @strict: false
let o = { x: "hi", extra: 1 }; // ok
let o2: { x: string } = o; // ok
```

여기서 객체 리터럴 `{ x: "hi", extra: 1 }`은 일치하는 리터럴 타입 `{ x: string, extra: number }`를 가집니다. 그 타입은 필요한 모든 속성을 가지고 있고 해당 속성들이 할당 가능한 타입을 가지고 있기 때문에 `{ x: string }`에 할당 가능합니다. 추가 속성은 할당을 방해하지 않고, 단지 `{ x: string }`의 하위 타입으로 만듭니다.

명명된 타입은 단지 타입에 이름을 부여할 뿐입니다; 할당 가능성 측면에서 아래의 타입 별칭 `One`과 인터페이스 타입 `Two` 사이에는 차이가 없습니다. 둘 다 `p: string` 속성을 가집니다. (타입 별칭은 재귀적 정의와 타입 매개변수와 관련하여 인터페이스와 다르게 동작하지만.)

```ts
type One = { p: string };
interface Two {
  p: string;
}
class Three {
  p = "Hello";
}

let x: One = { p: "hi" };
let two: Two = x;
two = new Three();
```

### 유니온

TypeScript에서 유니온 타입은 태그가 없습니다. 다시 말해, Haskell의 `data`처럼 구별된 유니온이 아닙니다. 그러나 종종 내장 태그나 다른 속성을 사용하여 유니온의 타입을 구별할 수 있습니다.

```ts
function start(
  arg: string | string[] | (() => string) | { s: string }
): string {
  // 이것은 JavaScript에서 매우 일반적입니다
  if (typeof arg === "string") {
    return commonCase(arg);
  } else if (Array.isArray(arg)) {
    return arg.map(commonCase).join(",");
  } else if (typeof arg === "function") {
    return commonCase(arg());
  } else {
    return commonCase(arg.s);
  }

  function commonCase(s: string): string {
    // 마지막으로, 문자열을 다른 문자열로 변환
    return s;
  }
}
```

`string`, `Array`, `Function`은 내장 타입 조건자를 가지고 있어, `else` 분기에는 객체 타입이 편리하게 남습니다. 그러나 런타임에 구별하기 어려운 유니온을 생성하는 것이 가능합니다. 새 코드의 경우, 구별된 유니온만 만드는 것이 가장 좋습니다.

다음 타입들은 내장 조건자를 가지고 있습니다:

| 타입 | 조건자 |
|------|--------|
| string | `typeof s === "string"` |
| number | `typeof n === "number"` |
| bigint | `typeof m === "bigint"` |
| boolean | `typeof b === "boolean"` |
| symbol | `typeof g === "symbol"` |
| undefined | `typeof undefined === "undefined"` |
| function | `typeof f === "function"` |
| array | `Array.isArray(a)` |
| object | `typeof o === "object"` |

함수와 배열은 런타임에 객체이지만, 자체 조건자를 가지고 있다는 점에 주의하세요.

#### 교차

유니온 외에도, TypeScript는 교차도 가지고 있습니다:

```ts
type Combined = { a: number } & { b: string };
type Conflicting = { a: number } & { a: string };
```

`Combined`는 마치 하나의 객체 리터럴 타입으로 작성된 것처럼 `a`와 `b` 두 속성을 가집니다. 교차와 유니온은 충돌의 경우 재귀적이므로, `Conflicting.a: number & string`입니다.

### 유닛 타입

유닛 타입은 정확히 하나의 원시 값을 포함하는 원시 타입의 하위 타입입니다. 예를 들어, 문자열 `"foo"`는 `"foo"` 타입을 가집니다. JavaScript에는 내장 열거형이 없기 때문에, 잘 알려진 문자열 세트를 대신 사용하는 것이 일반적입니다. 문자열 리터럴 타입의 유니온으로 TypeScript는 이 패턴을 타입화할 수 있습니다:

```ts
declare function pad(s: string, n: number, direction: "left" | "right"): string;
pad("hi", 10, "left");
```

필요할 때, 컴파일러는 유닛 타입을 원시 타입으로 *확장*합니다. 예를 들어 `"foo"`를 `string`으로. 이것은 가변성을 사용할 때 발생하며, 가변 변수의 일부 사용을 방해할 수 있습니다:

```ts
let s = "right";
pad("hi", 10, s); // 오류: 'string'은 '"left" | "right"'에 할당할 수 없습니다
// Argument of type 'string' is not assignable to parameter of type '"left" | "right"'.
```

오류가 발생하는 방식은 다음과 같습니다:

- `"right": "right"`
- `s: string` 왜냐하면 `"right"`가 가변 변수에 할당될 때 `string`으로 확장되기 때문입니다.
- `string`은 `"left" | "right"`에 할당할 수 없습니다

`s`에 대한 타입 어노테이션으로 이 문제를 해결할 수 있지만, 그러면 `"left" | "right"` 타입이 아닌 변수를 `s`에 할당하는 것을 막습니다.

```ts
let s: "left" | "right" = "right";
pad("hi", 10, s);
```

## Haskell과 유사한 개념

### 문맥적 타이핑

TypeScript에는 변수 선언과 같이 타입을 추론할 수 있는 명백한 장소가 있습니다:

```ts
let s = "I'm a string!";
```

하지만 다른 C 문법 언어에서 작업했다면 예상하지 못할 수 있는 몇 가지 다른 장소에서도 타입을 추론합니다:

```ts
declare function map<T, U>(f: (t: T) => U, ts: T[]): U[];
let sns = map((n) => n.toString(), [1, 2, 3]);
```

여기서 `n: number`이기도 합니다. `T`와 `U`가 호출 전에 추론되지 않았음에도 불구하고요. 사실, `[1,2,3]`이 `T=number`를 추론하는 데 사용된 후, `n => n.toString()`의 반환 타입이 `U=string`을 추론하는 데 사용되어 `sns`가 `string[]` 타입을 가지게 됩니다.

추론은 어떤 순서로든 작동하지만, intellisense는 왼쪽에서 오른쪽으로만 작동하므로, TypeScript는 배열을 먼저 둔 `map`을 선언하는 것을 선호합니다:

```ts
declare function map<T, U>(ts: T[], f: (t: T) => U): U[];
```

문맥적 타이핑은 또한 객체 리터럴을 통해 재귀적으로 작동하고, `string`이나 `number`로 추론될 유닛 타입에도 작동합니다. 그리고 문맥에서 반환 타입을 추론할 수 있습니다:

```ts
declare function run<T>(thunk: (t: T) => void): T;
let i: { inference: string } = run((o) => {
  o.inference = "INSERT STATE HERE";
});
```

`o`의 타입은 `{ inference: string }`으로 결정됩니다. 왜냐하면:

- 선언 초기화자는 선언의 타입에 의해 문맥적으로 타이핑됩니다: `{ inference: string }`.
- 호출의 반환 타입은 추론을 위해 문맥적 타입을 사용하므로, 컴파일러는 `T={ inference: string }`을 추론합니다.
- 화살표 함수는 매개변수를 타이핑하기 위해 문맥적 타입을 사용하므로, 컴파일러는 `o: { inference: string }`을 제공합니다.

그리고 타이핑하는 동안 이것을 수행하므로, `o.`를 입력한 후 실제 프로그램에서 가질 수 있는 다른 속성과 함께 `inference` 속성에 대한 완성을 얻습니다.
전체적으로, 이 기능은 TypeScript의 추론을 통합 타입 추론 엔진처럼 보이게 할 수 있지만, 그렇지 않습니다.

### 타입 별칭

타입 별칭은 Haskell의 `type`처럼 단순한 별칭입니다. 컴파일러는 소스 코드에서 사용된 곳마다 별칭 이름을 사용하려고 시도하지만, 항상 성공하지는 않습니다.

```ts
type Size = [number, number];
let x: Size = [101.1, 999.9];
```

`newtype`에 가장 가까운 것은 *태그된 교차*입니다:

```ts
type FString = string & { __compileTimeOnly: any };
```

`FString`은 일반 문자열과 같지만, 컴파일러가 `__compileTimeOnly`라는 속성이 있다고 생각하고 실제로는 존재하지 않습니다. 이것은 `FString`이 여전히 `string`에 할당될 수 있지만, 그 반대는 안 된다는 것을 의미합니다.

### 구별된 유니온

`data`에 가장 가까운 것은 구별자 속성을 가진 타입들의 유니온이며, TypeScript에서는 보통 구별된 유니온이라고 불립니다:

```ts
type Shape =
  | { kind: "circle"; radius: number }
  | { kind: "square"; x: number }
  | { kind: "triangle"; x: number; y: number };
```

Haskell과 달리, 태그 또는 구별자는 각 객체 타입의 단순한 속성입니다. 각 변형은 다른 유닛 타입을 가진 동일한 속성을 가집니다. 이것은 여전히 일반적인 유니온 타입입니다; 맨 앞의 `|`는 유니온 타입 문법의 선택적 부분입니다. 일반적인 JavaScript 코드를 사용하여 유니온의 구성원을 구별할 수 있습니다:

```ts
type Shape =
  | { kind: "circle"; radius: number }
  | { kind: "square"; x: number }
  | { kind: "triangle"; x: number; y: number };

function area(s: Shape) {
  if (s.kind === "circle") {
    return Math.PI * s.radius * s.radius;
  } else if (s.kind === "square") {
    return s.x * s.x;
  } else {
    return (s.x * s.y) / 2;
  }
}
```

`area`의 반환 타입이 `number`로 추론된다는 점에 주목하세요. TypeScript가 함수가 전체 함수임을 알기 때문입니다. 일부 변형이 다루어지지 않으면, `area`의 반환 타입은 대신 `number | undefined`가 됩니다.

또한, Haskell과 달리, 공통 속성은 모든 유니온에 나타나므로, 유니온의 여러 구성원을 유용하게 구별할 수 있습니다:

```ts
function height(s: Shape) {
  if (s.kind === "circle") {
    return 2 * s.radius;
  } else {
    // s.kind: "square" | "triangle"
    return s.x;
  }
}
```

### 타입 매개변수

대부분의 C 계열 언어처럼, TypeScript는 타입 매개변수의 선언을 요구합니다:

```ts
function liftArray<T>(t: T): Array<T> {
  return [t];
}
```

대소문자 요구 사항은 없지만, 타입 매개변수는 관례적으로 단일 대문자입니다. 타입 매개변수는 타입 클래스 제약과 비슷하게 동작하는 타입으로 제한될 수도 있습니다:

```ts
function firstish<T extends { length: number }>(t1: T, t2: T): T {
  return t1.length > t2.length ? t1 : t2;
}
```

TypeScript는 보통 인수의 타입을 기반으로 호출에서 타입 인수를 추론할 수 있으므로, 타입 인수는 보통 필요하지 않습니다.

TypeScript가 구조적이기 때문에, 명목적 시스템만큼 타입 매개변수가 필요하지 않습니다. 구체적으로, 함수를 다형적으로 만드는 데 필요하지 않습니다. 타입 매개변수는 매개변수를 같은 타입으로 제한하는 것과 같이 타입 정보를 *전파*하는 데만 사용해야 합니다:

```ts
function length<T extends ArrayLike<unknown>>(t: T): number {}

function length(t: ArrayLike<unknown>): number {}
```

첫 번째 `length`에서 `T`는 필요하지 않습니다; 한 번만 참조되므로, 반환 값이나 다른 매개변수의 타입을 제한하는 데 사용되지 않습니다.

#### 상위 종류 타입

TypeScript에는 상위 종류 타입이 없으므로, 다음은 유효하지 않습니다:

```ts
function length<T extends ArrayLike<unknown>, U>(m: T<U>) {}
```

#### 포인트-프리 프로그래밍

포인트-프리 프로그래밍 - 커링과 함수 합성의 많은 사용 - 은 JavaScript에서 가능하지만, 장황할 수 있습니다.
TypeScript에서 포인트-프리 프로그램에 대한 타입 추론은 종종 실패하므로, 값 매개변수 대신 타입 매개변수를 지정하게 됩니다. 결과가 너무 장황해서 보통 포인트-프리 프로그래밍을 피하는 것이 좋습니다.

### 모듈 시스템

JavaScript의 현대 모듈 문법은 Haskell과 약간 비슷하지만, `import`나 `export`가 있는 모든 파일은 암묵적으로 모듈입니다:

```ts
import { value, Type } from "npm-package";
import { other, Types } from "./local-package";
import * as prefix from "../lib/third-package";
```

commonjs 모듈 - node.js의 모듈 시스템을 사용하여 작성된 모듈 - 도 가져올 수 있습니다:

```ts
import f = require("single-function-package");
```

export 목록으로 내보낼 수 있습니다:

```ts
export { f };

function f() {
  return g();
}

function g() {} // g는 내보내지지 않음
```

또는 각 export를 개별적으로 표시하여:

```ts
export function f() { return g() }

function g() { }
```

후자의 스타일이 더 일반적이지만 둘 다 허용되며, 같은 파일에서도 가능합니다.

### `readonly`와 `const`

JavaScript에서 가변성이 기본이지만, *참조*가 불변임을 선언하기 위해 `const`로 변수 선언을 허용합니다. 참조 대상은 여전히 가변입니다:

```ts
const a = [1, 2, 3];
a.push(102); // ):
a[0] = 101; // D:
```

TypeScript는 추가로 속성에 대한 `readonly` 수정자를 가지고 있습니다.

```ts
interface Rx {
  readonly x: number;
}
let rx: Rx = { x: 1 };
rx.x = 12; // 오류
```

또한 모든 속성을 `readonly`로 만드는 매핑된 타입 `Readonly<T>`가 함께 제공됩니다:

```ts
interface X {
  x: number;
}
let rx: Readonly<X> = { x: 1 };
rx.x = 12; // 오류
```

그리고 부작용이 있는 메서드를 제거하고 배열의 인덱스에 쓰기를 방지하는 특정 `ReadonlyArray<T>` 타입과 이 타입에 대한 특별한 문법이 있습니다:

```ts
let a: ReadonlyArray<number> = [1, 2, 3];
let b: readonly number[] = [1, 2, 3];
a.push(102); // 오류
b[0] = 101; // 오류
```

배열과 객체 리터럴에서 작동하는 const 단언도 사용할 수 있습니다:

```ts
let a = [1, 2, 3] as const;
a.push(102); // 오류
a[0] = 101; // 오류
```

그러나 이러한 옵션 중 어느 것도 기본값이 아니므로, TypeScript 코드에서 일관되게 사용되지 않습니다.

### 다음 단계

이 문서는 일상적인 코드에서 사용할 문법과 타입에 대한 높은 수준의 개요입니다. 여기서부터 다음을 할 수 있습니다:

- 전체 핸드북을 [처음부터 끝까지](/docs/handbook/intro.html) 읽으세요

- [플레이그라운드 예제](/play#show-examples)를 탐색하세요

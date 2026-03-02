# 열거형 (Enums)

열거형은 TypeScript가 JavaScript의 타입 수준 확장이 아닌 몇 안 되는 기능 중 하나입니다.

열거형을 사용하면 개발자가 명명된 상수 집합을 정의할 수 있습니다.
열거형을 사용하면 의도를 문서화하거나 구별되는 케이스 집합을 만드는 것이 더 쉬워집니다.
TypeScript는 숫자 및 문자열 기반 열거형을 모두 제공합니다.

## 숫자 열거형

먼저 숫자 열거형부터 시작하겠습니다. 다른 언어를 사용해본 경우 아마 더 익숙할 것입니다.
열거형은 `enum` 키워드를 사용하여 정의할 수 있습니다.

```ts
enum Direction {
  Up = 1,
  Down,
  Left,
  Right,
}
```

위에서 `Up`이 `1`로 초기화된 숫자 열거형이 있습니다.
그 이후의 모든 멤버는 그 시점부터 자동 증가됩니다.
다시 말해, `Direction.Up`은 `1`, `Down`은 `2`, `Left`는 `3`, `Right`는 `4` 값을 가집니다.

원한다면 이니셜라이저를 완전히 생략할 수 있습니다:

```ts
enum Direction {
  Up,
  Down,
  Left,
  Right,
}
```

여기서 `Up`은 `0`, `Down`은 `1` 등의 값을 가집니다.
이 자동 증가 동작은 멤버 값 자체는 신경 쓰지 않지만 각 값이 동일한 열거형의 다른 값과 구별되어야 하는 경우에 유용합니다.

열거형 사용은 간단합니다: 열거형 자체의 프로퍼티로 멤버에 접근하고, 열거형 이름을 사용하여 타입을 선언합니다:

```ts
enum UserResponse {
  No = 0,
  Yes = 1,
}

function respond(recipient: string, message: UserResponse): void {
  // ...
}

respond("Princess Caroline", UserResponse.Yes);
```

숫자 열거형은 [계산된 멤버와 상수 멤버(아래 참조)](#계산된-멤버와-상수-멤버)에 혼합될 수 있습니다.
간단히 말해서, 이니셜라이저가 없는 열거형은 첫 번째이거나, 숫자 상수 또는 다른 상수 열거형 멤버로 초기화된 숫자 열거형 뒤에 와야 합니다.
다시 말해 다음은 허용되지 않습니다:

```ts
// @errors: 1061
const getSomeValue = () => 23;
// ---cut---
enum E {
  A = getSomeValue(),
  B,
}
```

## 문자열 열거형

문자열 열거형은 비슷한 개념이지만 아래에 문서화된 것처럼 약간의 미묘한 [런타임 차이](#런타임에서의-열거형)가 있습니다.
문자열 열거형에서 각 멤버는 문자열 리터럴 또는 다른 문자열 열거형 멤버로 상수 초기화되어야 합니다.

```ts
enum Direction {
  Up = "UP",
  Down = "DOWN",
  Left = "LEFT",
  Right = "RIGHT",
}
```

문자열 열거형에는 자동 증가 동작이 없지만, "직렬화"가 잘 된다는 이점이 있습니다.
다시 말해서, 디버깅 중에 숫자 열거형의 런타임 값을 읽어야 하는 경우, 그 값은 종종 불투명합니다 - 그 자체로는 유용한 의미를 전달하지 않습니다([역방향 매핑](#역방향-매핑)이 종종 도움이 될 수 있지만). 문자열 열거형을 사용하면 열거형 멤버의 이름과 독립적으로 코드가 실행될 때 의미 있고 읽기 쉬운 값을 제공할 수 있습니다.

## 이기종 열거형

기술적으로 열거형은 문자열과 숫자 멤버를 혼합할 수 있지만, 왜 그렇게 하고 싶은지 명확하지 않습니다:

```ts
enum BooleanLikeHeterogeneousEnum {
  No = 0,
  Yes = "YES",
}
```

JavaScript의 런타임 동작을 영리하게 활용하려는 것이 아니라면, 이렇게 하지 않는 것이 좋습니다.

## 계산된 멤버와 상수 멤버

각 열거형 멤버에는 _상수_이거나 _계산된_ 값이 연관되어 있습니다.
열거형 멤버는 다음과 같은 경우 상수로 간주됩니다:

- 열거형의 첫 번째 멤버이고 이니셜라이저가 없는 경우, 이 경우 `0` 값이 할당됩니다:

  ```ts
  // E.X는 상수입니다:
  enum E {
    X,
  }
  ```

- 이니셜라이저가 없고 앞의 열거형 멤버가 _숫자_ 상수인 경우.
  이 경우 현재 열거형 멤버의 값은 앞의 열거형 멤버의 값에 1을 더한 값이 됩니다.

  ```ts
  // 'E1'과 'E2'의 모든 열거형 멤버는 상수입니다.

  enum E1 {
    X,
    Y,
    Z,
  }

  enum E2 {
    A = 1,
    B,
    C,
  }
  ```

- 열거형 멤버가 상수 열거형 표현식으로 초기화된 경우.
  상수 열거형 표현식은 컴파일 시간에 완전히 평가할 수 있는 TypeScript 표현식의 하위 집합입니다.
  표현식이 다음과 같은 경우 상수 열거형 표현식입니다:

  1. 리터럴 열거형 표현식(기본적으로 문자열 리터럴 또는 숫자 리터럴)
  2. 이전에 정의된 상수 열거형 멤버에 대한 참조(다른 열거형에서 유래할 수 있음)
  3. 괄호로 묶인 상수 열거형 표현식
  4. 상수 열거형 표현식에 적용된 `+`, `-`, `~` 단항 연산자 중 하나
  5. 상수 열거형 표현식을 피연산자로 하는 `+`, `-`, `*`, `/`, `%`, `<<`, `>>`, `>>>`, `&`, `|`, `^` 이항 연산자

  상수 열거형 표현식이 `NaN`이나 `Infinity`로 평가되면 컴파일 시간 오류입니다.

다른 모든 경우에 열거형 멤버는 계산된 것으로 간주됩니다.

```ts
enum FileAccess {
  // 상수 멤버
  None,
  Read = 1 << 1,
  Write = 1 << 2,
  ReadWrite = Read | Write,
  // 계산된 멤버
  G = "123".length,
}
```

## 유니온 열거형과 열거형 멤버 타입

계산되지 않는 상수 열거형 멤버의 특별한 하위 집합이 있습니다: 리터럴 열거형 멤버.
리터럴 열거형 멤버는 초기화된 값이 없거나, 다음으로 초기화된 상수 열거형 멤버입니다:

- 모든 문자열 리터럴(예: `"foo"`, `"bar"`, `"baz"`)
- 모든 숫자 리터럴(예: `1`, `100`)
- 모든 숫자 리터럴에 적용된 단항 마이너스(예: `-1`, `-100`)

열거형의 모든 멤버가 리터럴 열거형 값을 가지면, 몇 가지 특별한 의미가 적용됩니다.

첫 번째는 열거형 멤버가 타입이 된다는 것입니다!
예를 들어, 특정 멤버가 열거형 멤버의 값_만_ 가질 수 있다고 말할 수 있습니다:

```ts
// @errors: 2322
enum ShapeKind {
  Circle,
  Square,
}

interface Circle {
  kind: ShapeKind.Circle;
  radius: number;
}

interface Square {
  kind: ShapeKind.Square;
  sideLength: number;
}

let c: Circle = {
  kind: ShapeKind.Square,
  radius: 100,
};
```

다른 변경 사항은 열거형 타입 자체가 효과적으로 각 열거형 멤버의 _유니온_이 된다는 것입니다.
유니온 열거형을 사용하면, 타입 시스템이 열거형 자체에 존재하는 정확한 값 집합을 알고 있다는 사실을 활용할 수 있습니다.
그 때문에 TypeScript는 값을 잘못 비교하는 버그를 잡을 수 있습니다.
예를 들어:

```ts
// @errors: 2367
enum E {
  Foo,
  Bar,
}

function f(x: E) {
  if (x !== E.Foo || x !== E.Bar) {
    //
  }
}
```

이 예제에서 우리는 먼저 `x`가 `E.Foo`가 _아닌지_ 확인했습니다.
그 검사가 성공하면, `||`가 단락되어 'if'의 본문이 실행됩니다.
그러나 검사가 성공하지 않으면, `x`는 `E.Foo`_만_ 될 수 있으므로, `E.Bar`와 _같지 않은지_ 확인하는 것은 의미가 없습니다.

## 런타임에서의 열거형

열거형은 런타임에 존재하는 실제 객체입니다.
예를 들어, 다음 열거형

```ts
enum E {
  X,
  Y,
  Z,
}
```

은 실제로 함수에 전달될 수 있습니다

```ts
enum E {
  X,
  Y,
  Z,
}

function f(obj: { X: number }) {
  return obj.X;
}

// 'E'에 숫자인 'X'라는 프로퍼티가 있으므로 작동합니다.
f(E);
```

## 컴파일 시점의 열거형

열거형이 런타임에 존재하는 실제 객체이지만, `keyof` 키워드는 일반적인 객체에 대해 예상하는 것과 다르게 작동합니다. 대신, 모든 열거형 키를 문자열로 나타내는 타입을 얻으려면 `keyof typeof`를 사용합니다.

```ts
enum LogLevel {
  ERROR,
  WARN,
  INFO,
  DEBUG,
}

/**
 * 이것은 다음과 동일합니다:
 * type LogLevelStrings = 'ERROR' | 'WARN' | 'INFO' | 'DEBUG';
 */
type LogLevelStrings = keyof typeof LogLevel;

function printImportant(key: LogLevelStrings, message: string) {
  const num = LogLevel[key];
  if (num <= LogLevel.WARN) {
    console.log("Log level key is:", key);
    console.log("Log level value is:", num);
    console.log("Log level message is:", message);
  }
}
printImportant("ERROR", "This is a message");
```

### 역방향 매핑

멤버의 프로퍼티 이름을 가진 객체를 만드는 것 외에도, 숫자 열거형 멤버는 열거형 값에서 열거형 이름으로의 _역방향 매핑_도 얻습니다.
예를 들어, 이 예제에서:

```ts
enum Enum {
  A,
}

let a = Enum.A;
let nameOfA = Enum[a]; // "A"
```

TypeScript는 이것을 다음 JavaScript로 컴파일합니다:

```js
"use strict";
var Enum;
(function (Enum) {
    Enum[Enum["A"] = 0] = "A";
})(Enum || (Enum = {}));
let a = Enum.A;
let nameOfA = Enum[a]; // "A"
```

이 생성된 코드에서 열거형은 순방향(`name` -> `value`)과 역방향(`value` -> `name`) 매핑을 모두 저장하는 객체로 컴파일됩니다.
다른 열거형 멤버에 대한 참조는 항상 프로퍼티 접근으로 방출되고 인라인되지 않습니다.

문자열 열거형 멤버는 역방향 매핑이 전혀 생성되지 _않는다_는 것을 명심하세요.

### `const` 열거형

대부분의 경우 열거형은 완벽하게 유효한 솔루션입니다.
그러나 때때로 요구 사항이 더 엄격합니다.
열거형 값에 접근할 때 추가 생성 코드 비용과 추가 간접 참조 비용을 피하려면, `const` 열거형을 사용할 수 있습니다.
const 열거형은 열거형에 `const` 수정자를 사용하여 정의됩니다:

```ts
const enum Enum {
  A = 1,
  B = A * 2,
}
```

const 열거형은 상수 열거형 표현식만 사용할 수 있으며 일반 열거형과 달리 컴파일 중에 완전히 제거됩니다.
const 열거형 멤버는 사용 사이트에서 인라인됩니다.
const 열거형은 계산된 멤버를 가질 수 없기 때문에 이것이 가능합니다.

```ts
const enum Direction {
  Up,
  Down,
  Left,
  Right,
}

let directions = [
  Direction.Up,
  Direction.Down,
  Direction.Left,
  Direction.Right,
];
```

생성된 코드에서는 다음이 됩니다

```js
"use strict";
let directions = [
    0 /* Direction.Up */,
    1 /* Direction.Down */,
    2 /* Direction.Left */,
    3 /* Direction.Right */,
];
```

#### const 열거형의 함정

열거형 값 인라인은 처음에는 간단하지만 미묘한 의미가 있습니다.
이러한 함정은 _앰비언트_ const 열거형(기본적으로 `.d.ts` 파일의 const 열거형)에만 해당되며 프로젝트 간에 공유하는 것과 관련이 있지만, `.d.ts` 파일을 게시하거나 소비하는 경우, 이러한 함정이 적용될 가능성이 높습니다. `tsc --declaration`이 `.ts` 파일을 `.d.ts` 파일로 변환하기 때문입니다.

1. [`isolatedModules` 문서](/tsconfig#references-to-const-enum-members)에 설명된 이유로, 해당 모드는 앰비언트 const 열거형과 근본적으로 호환되지 않습니다.
   이것은 앰비언트 const 열거형을 게시하면, 다운스트림 소비자가 [`isolatedModules`](/tsconfig#isolatedModules)와 해당 열거형 값을 동시에 사용할 수 없음을 의미합니다.
2. 컴파일 시간에 종속성의 버전 A에서 값을 쉽게 인라인하고, 런타임에 버전 B를 가져올 수 있습니다.
   매우 주의하지 않으면 버전 A와 B의 열거형 값이 다를 수 있어, `if` 문의 잘못된 분기를 타는 것과 같은 [놀라운 버그](https://github.com/microsoft/TypeScript/issues/5219#issue-110947903)가 발생합니다.
   이러한 버그는 자동화된 테스트를 프로젝트 빌드와 거의 동시에 동일한 종속성 버전으로 실행하는 것이 일반적이므로 특히 악랄합니다. 이러한 버그를 완전히 놓치게 됩니다.
3. [`importsNotUsedAsValues: "preserve"`](/tsconfig#importsNotUsedAsValues)는 값으로 사용되는 const 열거형에 대한 임포트를 제거하지 않지만, 앰비언트 const 열거형은 런타임 `.js` 파일이 존재함을 보장하지 않습니다.
   해결할 수 없는 임포트는 런타임에 오류를 발생시킵니다.
   임포트를 명확하게 제거하는 일반적인 방법인 [타입 전용 임포트](/docs/handbook/modules/reference.html#type-only-imports-and-exports)는 현재 [const 열거형 값을 허용하지 않습니다](https://github.com/microsoft/TypeScript/issues/40344).

다음은 이러한 함정을 피하는 두 가지 접근 방식입니다:

1. const 열거형을 전혀 사용하지 않습니다.
   린터의 도움으로 [const 열거형을 금지](https://typescript-eslint.io/linting/troubleshooting#how-can-i-ban-specific-language-feature)할 수 있습니다.
   분명히 이것은 const 열거형의 모든 문제를 피하지만, 프로젝트가 자체 열거형을 인라인하는 것을 방지합니다.
   다른 프로젝트에서 열거형을 인라인하는 것과 달리, 프로젝트 자체 열거형을 인라인하는 것은 문제가 없으며 성능에 영향을 미칩니다.
2. [`preserveConstEnums`](/tsconfig#preserveConstEnums)의 도움으로 const 열거형을 해제하여 앰비언트 const 열거형을 게시하지 않습니다.
   이것은 [TypeScript 프로젝트 자체](https://github.com/microsoft/TypeScript/pull/5422)가 내부적으로 취하는 접근 방식입니다.
   [`preserveConstEnums`](/tsconfig#preserveConstEnums)는 const 열거형에 대해 일반 열거형과 동일한 JavaScript를 방출합니다.
   그런 다음 [빌드 단계](https://github.com/microsoft/TypeScript/blob/1a981d1df1810c868a66b3828497f049a944951c/Gulpfile.js#L144)에서 `.d.ts` 파일에서 `const` 수정자를 안전하게 제거할 수 있습니다.

   이 방법으로 다운스트림 소비자는 프로젝트에서 열거형을 인라인하지 않아 위의 함정을 피하지만, const 열거형을 완전히 금지하는 것과 달리 프로젝트는 여전히 자체 열거형을 인라인할 수 있습니다.

## 앰비언트 열거형

앰비언트 열거형은 이미 존재하는 열거형 타입의 형태를 설명하는 데 사용됩니다.

```ts
declare enum Enum {
  A = 1,
  B,
  C = 2,
}
```

앰비언트 열거형과 비앰비언트 열거형의 중요한 차이점은, 일반 열거형에서 이니셜라이저가 없는 멤버는 앞의 열거형 멤버가 상수로 간주되면 상수로 간주된다는 것입니다.
반면, 앰비언트(및 비const) 열거형 멤버가 이니셜라이저가 없으면 _항상_ 계산된 것으로 간주됩니다.

## 객체 vs 열거형

현대 TypeScript에서는 `as const`가 있는 객체로 충분할 때 열거형이 필요하지 않을 수 있습니다:

```ts
const enum EDirection {
  Up,
  Down,
  Left,
  Right,
}

const ODirection = {
  Up: 0,
  Down: 1,
  Left: 2,
  Right: 3,
} as const;

EDirection.Up;
//         ^? (enum member) EDirection.Up = 0

ODirection.Up;
//         ^? (property) Up: 0

// 열거형을 매개변수로 사용
function walk(dir: EDirection) {}

// 값을 가져오려면 추가 줄이 필요합니다
type Direction = typeof ODirection[keyof typeof ODirection];
function run(dir: Direction) {}

walk(EDirection.Left);
run(ODirection.Right);
```

TypeScript의 `enum`보다 이 형식을 선호하는 가장 큰 논거는 JavaScript의 상태와 코드베이스를 정렬하고, 열거형이 JavaScript에 추가될 [때/만약](https://github.com/rbuckton/proposal-enum) 추가 구문으로 이동할 수 있다는 것입니다.

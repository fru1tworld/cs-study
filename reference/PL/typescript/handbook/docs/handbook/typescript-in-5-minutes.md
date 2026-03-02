# JavaScript 프로그래머를 위한 TypeScript

TypeScript는 JavaScript와 특별한 관계에 있습니다. TypeScript는 JavaScript의 모든 기능을 제공하며, 그 위에 추가적인 계층인 TypeScript의 타입 시스템을 더합니다.

예를 들어, JavaScript는 `string`과 `number` 같은 언어 원시 타입을 제공하지만, 이들을 일관되게 할당했는지 검사하지 않습니다. TypeScript는 검사합니다.

이것은 기존에 작동하는 JavaScript 코드가 TypeScript 코드이기도 하다는 것을 의미합니다. TypeScript의 주요 이점은 코드에서 예상치 못한 동작을 강조하여 버그 가능성을 낮출 수 있다는 것입니다.

이 튜토리얼은 TypeScript의 타입 시스템에 초점을 맞춘 TypeScript의 간략한 개요를 제공합니다.

## 추론에 의한 타입

TypeScript는 JavaScript 언어를 알고 있으며 많은 경우에 타입을 자동으로 생성해줍니다.
예를 들어, 변수를 생성하고 특정 값을 할당할 때, TypeScript는 그 값을 타입으로 사용합니다.

```ts
let helloWorld = "Hello World";
//  let helloWorld: string
```

JavaScript가 어떻게 작동하는지 이해함으로써, TypeScript는 JavaScript 코드를 받아들이면서도 타입을 가지는 타입 시스템을 구축할 수 있습니다. 이것은 코드에서 타입을 명시적으로 만들기 위해 추가적인 문자를 추가할 필요 없이 타입 시스템을 제공합니다. 이것이 TypeScript가 위의 예제에서 `helloWorld`가 `string`이라는 것을 아는 방법입니다.

Visual Studio Code에서 JavaScript를 작성하면서 편집기 자동 완성을 경험해보셨을 수 있습니다. Visual Studio Code는 JavaScript 작업을 더 쉽게 만들기 위해 내부적으로 TypeScript를 사용합니다.

## 타입 정의하기

JavaScript에서는 다양한 디자인 패턴을 사용할 수 있습니다. 그러나 일부 디자인 패턴은 타입이 자동으로 추론되기 어렵게 만듭니다(예: 동적 프로그래밍을 사용하는 패턴). 이러한 경우를 다루기 위해 TypeScript는 JavaScript 언어의 확장을 지원하여 TypeScript에게 타입이 무엇이어야 하는지 알려줄 수 있는 장소를 제공합니다.

예를 들어, `name: string`과 `id: number`를 포함하는 추론된 타입을 가진 객체를 생성하려면 다음과 같이 작성할 수 있습니다:

```ts
const user = {
  name: "Hayes",
  id: 0,
};
```

`interface` 선언을 사용하여 이 객체의 모양을 명시적으로 설명할 수 있습니다:

```ts
interface User {
  name: string;
  id: number;
}
```

그런 다음 변수 선언 후에 `: TypeName` 같은 문법을 사용하여 JavaScript 객체가 새로운 `interface`의 모양을 따른다고 선언할 수 있습니다:

```ts
const user: User = {
  name: "Hayes",
  id: 0,
};
```

제공한 인터페이스와 일치하지 않는 객체를 제공하면 TypeScript가 경고합니다:

```ts
interface User {
  name: string;
  id: number;
}

const user: User = {
  username: "Hayes",
  // Object literal may only specify known properties, and 'username' does not exist in type 'User'.
  // 객체 리터럴은 알려진 속성만 지정할 수 있으며, 'username'은 'User' 타입에 존재하지 않습니다.
  id: 0,
};
```

JavaScript가 클래스와 객체 지향 프로그래밍을 지원하므로 TypeScript도 지원합니다. 클래스와 함께 인터페이스 선언을 사용할 수 있습니다:

```ts
interface User {
  name: string;
  id: number;
}

class UserAccount {
  name: string;
  id: number;

  constructor(name: string, id: number) {
    this.name = name;
    this.id = id;
  }
}

const user: User = new UserAccount("Murphy", 1);
```

인터페이스를 사용하여 함수의 매개변수와 반환 값에 어노테이션을 달 수 있습니다:

```ts
function deleteUser(user: User) {
  // ...
}

function getAdminUser(): User {
  //...
}
```

JavaScript에서 이미 사용할 수 있는 작은 원시 타입 세트가 있습니다: `boolean`, `bigint`, `null`, `number`, `string`, `symbol`, `undefined`. 이것들은 인터페이스에서 사용할 수 있습니다. TypeScript는 이 목록을 몇 가지로 확장합니다. 예를 들어 `any`(무엇이든 허용), `unknown`(이 타입을 사용하는 사람이 타입이 무엇인지 선언하도록 보장), `never`(이 타입이 발생할 수 없음), `void`(`undefined`를 반환하거나 반환 값이 없는 함수) 등이 있습니다.

타입을 구축하는 두 가지 문법이 있다는 것을 알게 될 것입니다: [인터페이스와 타입](/play/?e=83#example/types-vs-interfaces). `interface`를 선호해야 합니다. 특정 기능이 필요할 때 `type`을 사용하세요.

## 타입 조합하기

TypeScript를 사용하면 간단한 타입을 결합하여 복잡한 타입을 만들 수 있습니다. 이를 수행하는 두 가지 인기 있는 방법이 있습니다: 유니온과 제네릭.

### 유니온

유니온을 사용하면 타입이 여러 타입 중 하나일 수 있다고 선언할 수 있습니다. 예를 들어, `boolean` 타입을 `true` 또는 `false`로 설명할 수 있습니다:

```ts
type MyBool = true | false;
```

*참고:* 위의 `MyBool` 위에 마우스를 올리면 `boolean`으로 분류된다는 것을 알 수 있습니다. 이것은 구조적 타입 시스템의 속성입니다. 아래에서 더 자세히 설명합니다.

유니온 타입의 일반적인 사용 사례는 값이 될 수 있는 `string` 또는 `number` [리터럴](/docs/handbook/2/everyday-types.html#literal-types) 세트를 설명하는 것입니다:

```ts
type WindowStates = "open" | "closed" | "minimized";
type LockStates = "locked" | "unlocked";
type PositiveOddNumbersUnderTen = 1 | 3 | 5 | 7 | 9;
```

유니온은 다양한 타입을 처리하는 방법도 제공합니다. 예를 들어, `array` 또는 `string`을 받는 함수가 있을 수 있습니다:

```ts
function getLength(obj: string | string[]) {
  return obj.length;
}
```

변수의 타입을 알아내려면 `typeof`를 사용하세요:

| 타입 | 조건식 |
|------|--------|
| string | `typeof s === "string"` |
| number | `typeof n === "number"` |
| boolean | `typeof b === "boolean"` |
| undefined | `typeof undefined === "undefined"` |
| function | `typeof f === "function"` |
| array | `Array.isArray(a)` |

예를 들어, 문자열 또는 배열이 전달되었는지에 따라 다른 값을 반환하는 함수를 만들 수 있습니다:

```ts
function wrapInArray(obj: string | string[]) {
  if (typeof obj === "string") {
    return [obj];
    //      ^? (parameter) obj: string
  }
  return obj;
}
```

### 제네릭

제네릭은 타입에 변수를 제공합니다. 일반적인 예는 배열입니다. 제네릭이 없는 배열은 무엇이든 포함할 수 있습니다. 제네릭이 있는 배열은 배열이 포함하는 값을 설명할 수 있습니다.

```ts
type StringArray = Array<string>;
type NumberArray = Array<number>;
type ObjectWithNameArray = Array<{ name: string }>;
```

제네릭을 사용하는 자신만의 타입을 선언할 수 있습니다:

```ts
interface Backpack<Type> {
  add: (obj: Type) => void;
  get: () => Type;
}

// 이 줄은 TypeScript에게 `backpack`이라는 상수가 있고,
// 어디서 왔는지 걱정하지 말라고 알려주는 단축키입니다.
declare const backpack: Backpack<string>;

// 위에서 Backpack의 변수 부분으로 선언했기 때문에 object는 string입니다.
const object = backpack.get();

// backpack 변수가 string이므로 add 함수에 number를 전달할 수 없습니다.
backpack.add(23);
// Argument of type 'number' is not assignable to parameter of type 'string'.
// 'number' 타입의 인수는 'string' 타입의 매개변수에 할당할 수 없습니다.
```

## 구조적 타입 시스템

TypeScript의 핵심 원칙 중 하나는 타입 검사가 값이 가진 *모양*에 초점을 맞춘다는 것입니다. 이것은 때때로 "덕 타이핑" 또는 "구조적 타이핑"이라고 불립니다.

구조적 타입 시스템에서 두 객체가 같은 모양을 가지면 같은 타입으로 간주됩니다.

```ts
interface Point {
  x: number;
  y: number;
}

function logPoint(p: Point) {
  console.log(`${p.x}, ${p.y}`);
}

// "12, 26"을 로그로 출력
const point = { x: 12, y: 26 };
logPoint(point);
```

`point` 변수는 `Point` 타입으로 선언된 적이 없습니다. 그러나 TypeScript는 타입 검사에서 `point`의 모양을 `Point`의 모양과 비교합니다. 같은 모양을 가지므로 코드가 통과합니다.

모양 일치는 객체 필드의 하위 집합만 일치하면 됩니다.

```ts
const point3 = { x: 12, y: 26, z: 89 };
logPoint(point3); // "12, 26" 로그 출력

const rect = { x: 33, y: 3, width: 30, height: 80 };
logPoint(rect); // "33, 3" 로그 출력

const color = { hex: "#187ABF" };
logPoint(color);
// Argument of type '{ hex: string; }' is not assignable to parameter of type 'Point'.
//   Type '{ hex: string; }' is missing the following properties from type 'Point': x, y
// '{ hex: string; }' 타입의 인수는 'Point' 타입의 매개변수에 할당할 수 없습니다.
//   '{ hex: string; }' 타입에는 'Point' 타입의 다음 속성이 없습니다: x, y
```

클래스와 객체가 모양을 따르는 방식에는 차이가 없습니다:

```ts
class VirtualPoint {
  x: number;
  y: number;

  constructor(x: number, y: number) {
    this.x = x;
    this.y = y;
  }
}

const newVPoint = new VirtualPoint(13, 56);
logPoint(newVPoint); // "13, 56" 로그 출력
```

객체 또는 클래스가 필요한 모든 속성을 가지고 있으면, TypeScript는 구현 세부 사항에 관계없이 일치한다고 말합니다.

## 다음 단계

이것은 일상적인 TypeScript에서 사용되는 문법과 도구에 대한 간략한 개요였습니다. 여기서부터 다음을 할 수 있습니다:

- 전체 핸드북을 [처음부터 끝까지](/docs/handbook/intro.html) 읽으세요

- [플레이그라운드 예제](/play#show-examples)를 탐색하세요

# 객체 타입

JavaScript에서 데이터를 그룹화하고 전달하는 기본적인 방법은 객체를 통해서입니다.
TypeScript에서 이것들을 _객체 타입_으로 표현합니다.

보았듯이, 익명일 수 있습니다:

```ts twoslash
function greet(person: { name: string; age: number }) {
  //                   ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  return "Hello " + person.name;
}
```

또는 인터페이스를 사용하여 이름을 지정할 수 있습니다:

```ts twoslash
interface Person {
  //      ^^^^^^
  name: string;
  age: number;
}

function greet(person: Person) {
  return "Hello " + person.name;
}
```

또는 타입 별칭으로:

```ts twoslash
type Person = {
  // ^^^^^^
  name: string;
  age: number;
};

function greet(person: Person) {
  return "Hello " + person.name;
}
```

위의 세 가지 예제 모두에서 `name`(`string`이어야 함)과 `age`(`number`여야 함) 속성을 포함하는 객체를 받는 함수를 작성했습니다.

## 빠른 참조

일상적인 구문을 한눈에 빠르게 보고 싶다면 [`type`과 `interface`](https://www.typescriptlang.org/cheatsheets)에 대한 치트시트가 있습니다.

## 속성 수정자

객체 타입의 각 속성은 타입, 속성이 선택적인지 여부, 속성을 쓸 수 있는지 여부와 같은 몇 가지를 지정할 수 있습니다.

### 선택적 속성

많은 경우 속성이 설정되어 있을 _수도_ 있는 객체를 다루게 됩니다.
그러한 경우, 속성 이름 끝에 물음표(`?`)를 추가하여 해당 속성을 _선택적_으로 표시할 수 있습니다.

```ts twoslash
interface Shape {}
declare function getShape(): Shape;

// ---cut---
interface PaintOptions {
  shape: Shape;
  xPos?: number;
  //  ^
  yPos?: number;
  //  ^
}

function paintShape(opts: PaintOptions) {
  // ...
}

const shape = getShape();
paintShape({ shape });
paintShape({ shape, xPos: 100 });
paintShape({ shape, yPos: 100 });
paintShape({ shape, xPos: 100, yPos: 100 });
```

이 예제에서, `xPos`와 `yPos` 모두 선택적으로 간주됩니다.
둘 중 하나를 제공하도록 선택할 수 있으므로, 위의 `paintShape`에 대한 모든 호출이 유효합니다.
선택성이 실제로 말하는 것은 속성이 _설정되면_ 특정 타입을 가져야 한다는 것입니다.

해당 속성에서 읽을 수도 있습니다 - 하지만 [`strictNullChecks`](/tsconfig#strictNullChecks)에서 TypeScript는 잠재적으로 `undefined`라고 알려줍니다.

```ts twoslash
interface Shape {}
declare function getShape(): Shape;

interface PaintOptions {
  shape: Shape;
  xPos?: number;
  yPos?: number;
}

// ---cut---
function paintShape(opts: PaintOptions) {
  let xPos = opts.xPos;
  //              ^?
  let yPos = opts.yPos;
  //              ^?
  // ...
}
```

JavaScript에서 속성이 설정되지 않았더라도 여전히 접근할 수 있습니다 - 단지 값 `undefined`를 줄 것입니다.
특별히 `undefined`를 검사하여 처리할 수 있습니다.

```ts twoslash
interface Shape {}
declare function getShape(): Shape;

interface PaintOptions {
  shape: Shape;
  xPos?: number;
  yPos?: number;
}

// ---cut---
function paintShape(opts: PaintOptions) {
  let xPos = opts.xPos === undefined ? 0 : opts.xPos;
  //  ^?
  let yPos = opts.yPos === undefined ? 0 : opts.yPos;
  //  ^?
  // ...
}
```

지정되지 않은 값에 대해 기본값을 설정하는 이 패턴은 매우 일반적이어서 JavaScript에는 이를 지원하는 구문이 있습니다.

```ts twoslash
interface Shape {}
declare function getShape(): Shape;

interface PaintOptions {
  shape: Shape;
  xPos?: number;
  yPos?: number;
}

// ---cut---
function paintShape({ shape, xPos = 0, yPos = 0 }: PaintOptions) {
  console.log("x coordinate at", xPos);
  //                             ^?
  console.log("y coordinate at", yPos);
  //                             ^?
  // ...
}
```

여기서 `paintShape`의 매개변수에 [구조 분해 패턴](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Destructuring_assignment)을 사용하고, `xPos`와 `yPos`에 [기본값](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Destructuring_assignment#Default_values)을 제공했습니다.
이제 `xPos`와 `yPos`는 `paintShape`의 본문 내에서 확실히 존재하지만, `paintShape`의 모든 호출자에게는 선택 사항입니다.

> 구조 분해 패턴 내에 타입 어노테이션을 배치할 방법이 현재 없습니다.
> 이는 다음 구문이 JavaScript에서 이미 다른 의미를 가지기 때문입니다.
>
> ```ts twoslash
> // @noImplicitAny: false
> // @errors: 2552 2304
> interface Shape {}
> declare function render(x: unknown);
> // ---cut---
> function draw({ shape: Shape, xPos: number = 100 /*...*/ }) {
>   render(shape);
>   render(xPos);
> }
> ```
>
> 객체 구조 분해 패턴에서 `shape: Shape`는 "속성 `shape`를 가져와서 `Shape`라는 이름의 변수로 로컬에서 재정의"를 의미합니다.
> 마찬가지로 `xPos: number`는 매개변수의 `xPos`를 기반으로 값을 가진 `number`라는 이름의 변수를 만듭니다.

### `readonly` 속성

TypeScript에서 속성을 `readonly`로 표시할 수도 있습니다.
런타임에 동작을 변경하지는 않지만, `readonly`로 표시된 속성은 타입 검사 중에 쓸 수 없습니다.

```ts twoslash
// @errors: 2540
interface SomeType {
  readonly prop: string;
}

function doSomething(obj: SomeType) {
  // 'obj.prop'에서 읽을 수 있습니다.
  console.log(`prop has the value '${obj.prop}'.`);

  // 하지만 재할당할 수 없습니다.
  obj.prop = "hello";
}
```

`readonly` 수정자를 사용한다고 해서 값이 완전히 불변이라는 의미는 아닙니다 - 즉, 내부 내용을 변경할 수 없다는 의미가 아닙니다.
속성 자체를 다시 쓸 수 없다는 것만 의미합니다.

```ts twoslash
// @errors: 2540
interface Home {
  readonly resident: { name: string; age: number };
}

function visitForBirthday(home: Home) {
  // 'home.resident'에서 속성을 읽고 업데이트할 수 있습니다.
  console.log(`Happy birthday ${home.resident.name}!`);
  home.resident.age++;
}

function evict(home: Home) {
  // 하지만 'Home'의 'resident' 속성 자체에는 쓸 수 없습니다.
  home.resident = {
    name: "Victor the Evictor",
    age: 42,
  };
}
```

`readonly`가 의미하는 바에 대한 기대를 관리하는 것이 중요합니다.
TypeScript에서 객체가 어떻게 사용되어야 하는지에 대한 개발 시간 동안의 의도를 알리는 데 유용합니다.
TypeScript는 두 타입이 호환되는지 확인할 때 속성이 `readonly`인지 고려하지 않으므로, `readonly` 속성은 별칭을 통해 변경될 수도 있습니다.

```ts twoslash
interface Person {
  name: string;
  age: number;
}

interface ReadonlyPerson {
  readonly name: string;
  readonly age: number;
}

let writablePerson: Person = {
  name: "Person McPersonface",
  age: 42,
};

// 작동함
let readonlyPerson: ReadonlyPerson = writablePerson;

console.log(readonlyPerson.age); // '42'를 출력
writablePerson.age++;
console.log(readonlyPerson.age); // '43'을 출력
```

[매핑 수정자](/docs/handbook/2/mapped-types.html#mapping-modifiers)를 사용하여 `readonly` 속성을 제거할 수 있습니다.

### 인덱스 시그니처

때때로 타입의 모든 속성 이름을 미리 알지 못하지만, 값의 형태는 알고 있습니다.

이러한 경우 인덱스 시그니처를 사용하여 가능한 값의 타입을 설명할 수 있습니다. 예를 들어:

```ts twoslash
declare function getStringArray(): StringArray;
// ---cut---
interface StringArray {
  [index: number]: string;
}

const myArray: StringArray = getStringArray();
const secondItem = myArray[1];
//     ^?
```

위에서 인덱스 시그니처를 가진 `StringArray` 인터페이스가 있습니다.
이 인덱스 시그니처는 `StringArray`가 `number`로 인덱싱되면 `string`을 반환한다고 명시합니다.

인덱스 시그니처 속성에는 `string`, `number`, `symbol`, 템플릿 문자열 패턴, 이들로만 구성된 유니온 타입만 허용됩니다.

<details>
    <summary>여러 유형의 인덱서를 지원하는 것이 가능합니다...</summary>
    <p>여러 유형의 인덱서를 지원하는 것이 가능합니다. `number`와 `string` 인덱서를 모두 사용할 때, 숫자 인덱서에서 반환되는 타입은 문자열 인덱서에서 반환되는 타입의 하위 타입이어야 합니다. 이는 <code>number</code>로 인덱싱할 때 JavaScript가 실제로 객체에 인덱싱하기 전에 <code>string</code>으로 변환하기 때문입니다. 즉, <code>100</code>(<code>number</code>)으로 인덱싱하는 것은 <code>"100"</code>(<code>string</code>)으로 인덱싱하는 것과 같으므로, 두 가지가 일관성이 있어야 합니다.</p>

```ts twoslash
// @errors: 2413
// @strictPropertyInitialization: false
interface Animal {
  name: string;
}

interface Dog extends Animal {
  breed: string;
}

// 오류: 숫자 문자열로 인덱싱하면 완전히 다른 타입의 Animal을 얻을 수 있습니다!
interface NotOkay {
  [x: number]: Animal;
  [x: string]: Dog;
}
```

</details>

문자열 인덱스 시그니처가 "사전" 패턴을 설명하는 강력한 방법이지만, 모든 속성이 반환 타입과 일치하도록 강제합니다.
이는 문자열 인덱스가 `obj.property`도 `obj["property"]`로 사용 가능하다고 선언하기 때문입니다.
다음 예제에서 `name`의 타입은 문자열 인덱스의 타입과 일치하지 않으며, 타입 검사기가 오류를 제공합니다:

```ts twoslash
// @errors: 2411
// @errors: 2411
interface NumberDictionary {
  [index: string]: number;

  length: number; // ok
  name: string;
}
```

그러나 인덱스 시그니처가 속성 타입의 유니온이면 다른 타입의 속성도 허용됩니다:

```ts twoslash
interface NumberOrStringDictionary {
  [index: string]: number | string;
  length: number; // ok, length는 number
  name: string; // ok, name은 string
}
```

마지막으로, 인덱스에 할당을 방지하기 위해 인덱스 시그니처를 `readonly`로 만들 수 있습니다:

```ts twoslash
declare function getReadOnlyStringArray(): ReadonlyStringArray;
// ---cut---
// @errors: 2542
interface ReadonlyStringArray {
  readonly [index: number]: string;
}

let myArray: ReadonlyStringArray = getReadOnlyStringArray();
myArray[2] = "Mallory";
```

인덱스 시그니처가 `readonly`이기 때문에 `myArray[2]`를 설정할 수 없습니다.

## 초과 속성 검사

객체가 타입에 할당되는 위치와 방법은 타입 시스템에서 차이를 만들 수 있습니다.
이것의 핵심 예 중 하나는 초과 속성 검사로, 객체가 생성되고 생성 중에 객체 타입에 할당될 때 객체를 더 철저하게 검증합니다.

```ts twoslash
// @errors: 2345 2739
interface SquareConfig {
  color?: string;
  width?: number;
}

function createSquare(config: SquareConfig): { color: string; area: number } {
  return {
    color: config.color || "red",
    area: config.width ? config.width * config.width : 20,
  };
}

let mySquare = createSquare({ colour: "red", width: 100 });
```

`createSquare`에 주어진 인수가 `color` 대신 _`colour`_로 철자가 되어 있는 것에 주목하세요.
일반 JavaScript에서 이런 종류의 일은 조용히 실패합니다.

이 프로그램이 올바르게 타입화되었다고 주장할 수 있습니다. `width` 속성이 호환되고, `color` 속성이 없으며, 추가 `colour` 속성은 중요하지 않기 때문입니다.

그러나 TypeScript는 이 코드에 아마도 버그가 있다는 입장을 취합니다.
객체 리터럴은 특별한 처리를 받고 다른 변수에 할당하거나 인수로 전달할 때 _초과 속성 검사_를 받습니다.
객체 리터럴이 "대상 타입"에 없는 속성을 가지고 있으면, 오류가 발생합니다:

```ts twoslash
// @errors: 2345 2739
interface SquareConfig {
  color?: string;
  width?: number;
}

function createSquare(config: SquareConfig): { color: string; area: number } {
  return {
    color: config.color || "red",
    area: config.width ? config.width * config.width : 20,
  };
}
// ---cut---
let mySquare = createSquare({ colour: "red", width: 100 });
```

이러한 검사를 피하는 것은 실제로 매우 간단합니다.
가장 쉬운 방법은 타입 단언을 사용하는 것입니다:

```ts twoslash
// @errors: 2345 2739
interface SquareConfig {
  color?: string;
  width?: number;
}

function createSquare(config: SquareConfig): { color: string; area: number } {
  return {
    color: config.color || "red",
    area: config.width ? config.width * config.width : 20,
  };
}
// ---cut---
let mySquare = createSquare({ width: 100, opacity: 0.5 } as SquareConfig);
```

그러나 더 나은 접근 방식은 객체가 특별한 방식으로 사용되는 일부 추가 속성을 가질 수 있다고 확신하는 경우 문자열 인덱스 시그니처를 추가하는 것일 수 있습니다.
`SquareConfig`가 위의 타입으로 `color`와 `width` 속성을 가질 수 있지만, 다른 수의 속성도 가질 수 _있다면_, 다음과 같이 정의할 수 있습니다:

```ts twoslash
interface SquareConfig {
  color?: string;
  width?: number;
  [propName: string]: unknown;
}
```

여기서 `SquareConfig`가 원하는 수의 속성을 가질 수 있으며, `color`나 `width`가 아닌 한 타입은 중요하지 않다고 말하고 있습니다.

이러한 검사를 피하는 마지막 방법은 약간 놀랍게도 객체를 다른 변수에 할당하는 것입니다:
`squareOptions`는 초과 속성 검사를 받지 않으므로, 컴파일러가 오류를 제공하지 않습니다:

```ts twoslash
interface SquareConfig {
  color?: string;
  width?: number;
}

function createSquare(config: SquareConfig): { color: string; area: number } {
  return {
    color: config.color || "red",
    area: config.width ? config.width * config.width : 20,
  };
}
// ---cut---
let squareOptions = { colour: "red", width: 100 };
let mySquare = createSquare(squareOptions);
```

위의 해결 방법은 `squareOptions`와 `SquareConfig` 사이에 공통 속성이 있는 한 작동합니다.
이 예제에서는 `width` 속성이었습니다. 그러나 변수가 공통 객체 속성이 없으면 실패합니다. 예를 들어:

```ts twoslash
// @errors: 2559
interface SquareConfig {
  color?: string;
  width?: number;
}

function createSquare(config: SquareConfig): { color: string; area: number } {
  return {
    color: config.color || "red",
    area: config.width ? config.width * config.width : 20,
  };
}
// ---cut---
let squareOptions = { colour: "red" };
let mySquare = createSquare(squareOptions);
```

위와 같은 간단한 코드의 경우, 이러한 검사를 "피하려고" 시도해서는 안 될 것입니다.
메서드를 가지고 상태를 유지하는 더 복잡한 객체 리터럴의 경우, 이러한 기술을 염두에 두어야 할 수 있지만, 대부분의 초과 속성 오류는 실제로 버그입니다.

즉, 옵션 백과 같은 것에 대해 초과 속성 검사 문제가 발생하면, 일부 타입 선언을 수정해야 할 수 있습니다.
이 경우, `color`나 `colour` 속성을 가진 객체를 `createSquare`에 전달하는 것이 괜찮다면, 이를 반영하도록 `SquareConfig`의 정의를 수정해야 합니다.

## 타입 확장하기

다른 타입의 더 구체적인 버전일 수 있는 타입을 가지는 것은 꽤 일반적입니다.
예를 들어, 미국에서 편지와 소포를 보내는 데 필요한 필드를 설명하는 `BasicAddress` 타입이 있을 수 있습니다.

```ts twoslash
interface BasicAddress {
  name?: string;
  street: string;
  city: string;
  country: string;
  postalCode: string;
}
```

일부 상황에서는 이것으로 충분하지만, 주소의 건물에 여러 유닛이 있는 경우 종종 유닛 번호가 연관되어 있습니다.
그런 다음 `AddressWithUnit`을 설명할 수 있습니다.

```ts twoslash
interface AddressWithUnit {
  name?: string;
  unit: string;
//^^^^^^^^^^^^^
  street: string;
  city: string;
  country: string;
  postalCode: string;
}
```

이것은 작업을 수행하지만, 여기서 단점은 변경 사항이 순전히 추가적일 때 `BasicAddress`의 다른 모든 필드를 반복해야 했다는 것입니다.
대신, 원래 `BasicAddress` 타입을 확장하고 `AddressWithUnit`에 고유한 새 필드만 추가할 수 있습니다.

```ts twoslash
interface BasicAddress {
  name?: string;
  street: string;
  city: string;
  country: string;
  postalCode: string;
}

interface AddressWithUnit extends BasicAddress {
  unit: string;
}
```

`interface`의 `extends` 키워드를 사용하면 다른 명명된 타입에서 멤버를 효과적으로 복사하고 원하는 새 멤버를 추가할 수 있습니다.
이것은 작성해야 하는 타입 선언 상용구의 양을 줄이고, 같은 속성의 여러 다른 선언이 관련될 수 있다는 의도를 알리는 데 유용할 수 있습니다.
예를 들어, `AddressWithUnit`은 `street` 속성을 반복할 필요가 없었고, `street`가 `BasicAddress`에서 유래하므로 독자는 두 타입이 어떤 방식으로 관련되어 있다는 것을 알 것입니다.

`interface`는 여러 타입에서 확장할 수도 있습니다.

```ts twoslash
interface Colorful {
  color: string;
}

interface Circle {
  radius: number;
}

interface ColorfulCircle extends Colorful, Circle {}

const cc: ColorfulCircle = {
  color: "red",
  radius: 42,
};
```

## 교차 타입

`interface`를 사용하면 다른 타입을 확장하여 새 타입을 구축할 수 있었습니다.
TypeScript는 주로 기존 객체 타입을 결합하는 데 사용되는 _교차 타입_이라는 또 다른 구문을 제공합니다.

교차 타입은 `&` 연산자를 사용하여 정의됩니다.

```ts twoslash
interface Colorful {
  color: string;
}
interface Circle {
  radius: number;
}

type ColorfulCircle = Colorful & Circle;
```

여기서 `Colorful`과 `Circle`을 교차하여 `Colorful` _과_ `Circle`의 모든 멤버를 가진 새 타입을 생성했습니다.

```ts twoslash
// @errors: 2345
interface Colorful {
  color: string;
}
interface Circle {
  radius: number;
}
// ---cut---
function draw(circle: Colorful & Circle) {
  console.log(`Color was ${circle.color}`);
  console.log(`Radius was ${circle.radius}`);
}

// okay
draw({ color: "blue", radius: 42 });

// 이런
draw({ color: "red", raidus: 42 });
```

## 인터페이스 확장 vs. 교차

유사하지만 실제로는 미묘하게 다른 두 가지 타입 결합 방법을 살펴보았습니다.
인터페이스를 사용하면 `extends` 절을 사용하여 다른 타입에서 확장할 수 있었고, 교차와 함께 유사한 작업을 수행하고 타입 별칭으로 결과에 이름을 지정할 수 있었습니다.
둘 사이의 주요 차이점은 충돌이 처리되는 방식이며, 이 차이점이 일반적으로 인터페이스와 교차 타입의 타입 별칭 중 하나를 선택하는 주요 이유입니다.

인터페이스가 같은 이름으로 정의되면, TypeScript는 속성이 호환되면 병합하려고 시도합니다. 속성이 호환되지 않으면(즉, 같은 속성 이름이지만 다른 타입), TypeScript는 오류를 발생시킵니다.

교차 타입의 경우, 다른 타입의 속성이 자동으로 병합됩니다. 타입이 나중에 사용될 때, TypeScript는 속성이 두 타입을 동시에 만족하기를 기대하며, 이는 예상치 못한 결과를 생성할 수 있습니다.

예를 들어, 다음 코드는 속성이 호환되지 않기 때문에 오류를 발생시킵니다:

```ts
interface Person {
  name: string;
}

interface Person {
  name: number;
}
```

대조적으로, 다음 코드는 컴파일되지만, `never` 타입이 됩니다:

```ts twoslash
interface Person1 {
  name: string;
}

interface Person2 {
  name: number;
}

type Staff = Person1 & Person2

declare const staffer: Staff;
staffer.name;
//       ^?
```

이 경우, Staff는 name 속성이 string과 number 모두여야 하므로, 속성이 `never` 타입이 됩니다.

## 제네릭 객체 타입

어떤 값이든 포함할 수 있는 `Box` 타입을 상상해 봅시다 - `string`, `number`, `Giraffe` 등.

```ts twoslash
interface Box {
  contents: any;
}
```

현재 `contents` 속성은 `any`로 타입화되어 있어 작동하지만, 나중에 사고로 이어질 수 있습니다.

대신 `unknown`을 사용할 수 있지만, `contents`의 타입을 이미 알고 있는 경우에 예방 검사를 수행하거나 오류가 발생하기 쉬운 타입 단언을 사용해야 합니다.

```ts twoslash
interface Box {
  contents: unknown;
}

let x: Box = {
  contents: "hello world",
};

// 'x.contents'를 확인할 수 있음
if (typeof x.contents === "string") {
  console.log(x.contents.toLowerCase());
}

// 또는 타입 단언을 사용할 수 있음
console.log((x.contents as string).toLowerCase());
```

타입 안전 접근 방식 중 하나는 모든 `contents` 타입에 대해 다른 `Box` 타입을 스캐폴드하는 것입니다.

```ts twoslash
// @errors: 2322
interface NumberBox {
  contents: number;
}

interface StringBox {
  contents: string;
}

interface BooleanBox {
  contents: boolean;
}
```

하지만 이것은 이러한 타입에서 작동하기 위해 다른 함수 또는 함수의 오버로드를 만들어야 한다는 것을 의미합니다.

```ts twoslash
interface NumberBox {
  contents: number;
}

interface StringBox {
  contents: string;
}

interface BooleanBox {
  contents: boolean;
}
// ---cut---
function setContents(box: StringBox, newContents: string): void;
function setContents(box: NumberBox, newContents: number): void;
function setContents(box: BooleanBox, newContents: boolean): void;
function setContents(box: { contents: any }, newContents: any) {
  box.contents = newContents;
}
```

이것은 많은 상용구입니다. 더욱이, 나중에 새 타입과 오버로드를 도입해야 할 수 있습니다.
박스 타입과 오버로드가 모두 효과적으로 같기 때문에 이것은 실망스럽습니다.

대신, _타입 매개변수_를 선언하는 _제네릭_ `Box` 타입을 만들 수 있습니다.

```ts twoslash
interface Box<Type> {
  contents: Type;
}
```

이것을 "`Type`의 `Box`는 `contents`가 `Type` 타입인 것"으로 읽을 수 있습니다.
나중에 `Box`를 참조할 때, `Type` 대신 _타입 인수_를 제공해야 합니다.

```ts twoslash
interface Box<Type> {
  contents: Type;
}
// ---cut---
let box: Box<string>;
```

`Box`를 실제 타입에 대한 템플릿으로 생각하세요. `Type`은 다른 타입으로 대체될 플레이스홀더입니다.
TypeScript가 `Box<string>`을 보면, `Box<Type>`의 모든 `Type` 인스턴스를 `string`으로 대체하고, `{ contents: string }`과 같은 것으로 작업하게 됩니다.
다시 말해, `Box<string>`과 이전 `StringBox`는 동일하게 작동합니다.

```ts twoslash
interface Box<Type> {
  contents: Type;
}
interface StringBox {
  contents: string;
}

let boxA: Box<string> = { contents: "hello" };
boxA.contents;
//   ^?

let boxB: StringBox = { contents: "world" };
boxB.contents;
//   ^?
```

`Box`는 `Type`을 무엇으로든 대체할 수 있기 때문에 재사용 가능합니다. 즉, 새 타입에 대해 박스가 필요할 때, 새 `Box` 타입을 전혀 선언할 필요가 없습니다(원한다면 확실히 할 수 있지만).

```ts twoslash
interface Box<Type> {
  contents: Type;
}

interface Apple {
  // ....
}

// '{ contents: Apple }'과 같음.
type AppleBox = Box<Apple>;
```

이것은 또한 [제네릭 함수](/docs/handbook/2/functions.html#generic-functions)를 대신 사용하여 오버로드를 완전히 피할 수 있다는 것을 의미합니다.

```ts twoslash
interface Box<Type> {
  contents: Type;
}

// ---cut---
function setContents<Type>(box: Box<Type>, newContents: Type) {
  box.contents = newContents;
}
```

타입 별칭도 제네릭일 수 있다는 점에 주목할 가치가 있습니다. 새로운 `Box<Type>` 인터페이스를 정의할 수 있었는데:

```ts twoslash
interface Box<Type> {
  contents: Type;
}
```

타입 별칭을 대신 사용하여:

```ts twoslash
type Box<Type> = {
  contents: Type;
};
```

타입 별칭은 인터페이스와 달리 객체 타입 이상을 설명할 수 있으므로, 다른 종류의 제네릭 헬퍼 타입을 작성하는 데도 사용할 수 있습니다.

```ts twoslash
// @errors: 2575
type OrNull<Type> = Type | null;

type OneOrMany<Type> = Type | Type[];

type OneOrManyOrNull<Type> = OrNull<OneOrMany<Type>>;
//   ^?

type OneOrManyOrNullStrings = OneOrManyOrNull<string>;
//   ^?
```

잠시 후에 타입 별칭으로 돌아올 것입니다.

### `Array` 타입

제네릭 객체 타입은 종종 포함하는 요소의 타입과 독립적으로 작동하는 일종의 컨테이너 타입입니다.
데이터 구조가 이런 방식으로 작동하는 것이 다른 데이터 타입에서 재사용 가능하도록 이상적입니다.

이 핸드북 전체에서 바로 그런 타입으로 작업해 왔습니다: `Array` 타입입니다.
`number[]`나 `string[]`과 같은 타입을 작성할 때마다, 이것은 실제로 `Array<number>`와 `Array<string>`의 줄임말입니다.

```ts twoslash
function doSomething(value: Array<string>) {
  // ...
}

let myArray: string[] = ["hello", "world"];

// 이 둘 모두 작동!
doSomething(myArray);
doSomething(new Array("hello", "world"));
```

위의 `Box` 타입과 마찬가지로, `Array` 자체가 제네릭 타입입니다.

```ts twoslash
// @noLib: true
interface Number {}
interface String {}
interface Boolean {}
interface Symbol {}
// ---cut---
interface Array<Type> {
  /**
   * 배열의 길이를 가져오거나 설정합니다.
   */
  length: number;

  /**
   * 배열에서 마지막 요소를 제거하고 반환합니다.
   */
  pop(): Type | undefined;

  /**
   * 배열에 새 요소를 추가하고, 배열의 새 길이를 반환합니다.
   */
  push(...items: Type[]): number;

  // ...
}
```

현대 JavaScript는 `Map<K, V>`, `Set<T>`, `Promise<T>`와 같이 제네릭인 다른 데이터 구조도 제공합니다.
이것이 의미하는 바는 `Map`, `Set`, `Promise`가 동작하는 방식 때문에 모든 타입 세트와 함께 작동할 수 있다는 것입니다.

### `ReadonlyArray` 타입

`ReadonlyArray`는 변경되어서는 안 되는 배열을 설명하는 특별한 타입입니다.

```ts twoslash
// @errors: 2339
function doStuff(values: ReadonlyArray<string>) {
  // 'values'에서 읽을 수 있음...
  const copy = values.slice();
  console.log(`The first value is ${values[0]}`);

  // ...하지만 'values'를 변경할 수 없음.
  values.push("hello!");
}
```

속성의 `readonly` 수정자와 마찬가지로, 이것은 주로 의도를 위해 사용할 수 있는 도구입니다.
`ReadonlyArray`를 반환하는 함수를 볼 때, 내용을 전혀 변경해서는 안 된다는 것을 알려주며, `ReadonlyArray`를 사용하는 함수를 볼 때, 내용이 변경될 걱정 없이 해당 함수에 어떤 배열이든 전달할 수 있다는 것을 알려줍니다.

`Array`와 달리, 사용할 수 있는 `ReadonlyArray` 생성자가 없습니다.

```ts twoslash
// @errors: 2693
new ReadonlyArray("red", "green", "blue");
```

대신, 일반 `Array`를 `ReadonlyArray`에 할당할 수 있습니다.

```ts twoslash
const roArray: ReadonlyArray<string> = ["red", "green", "blue"];
```

TypeScript가 `Array<Type>`에 대해 `Type[]`으로 줄임말 구문을 제공하는 것처럼, `ReadonlyArray<Type>`에 대해 `readonly Type[]`으로 줄임말 구문도 제공합니다.

```ts twoslash
// @errors: 2339
function doStuff(values: readonly string[]) {
  //                     ^^^^^^^^^^^^^^^^^
  // 'values'에서 읽을 수 있음...
  const copy = values.slice();
  console.log(`The first value is ${values[0]}`);

  // ...하지만 'values'를 변경할 수 없음.
  values.push("hello!");
}
```

마지막으로 주목할 점은 `readonly` 속성 수정자와 달리, 일반 `Array`와 `ReadonlyArray` 사이의 할당 가능성이 양방향이 아니라는 것입니다.

```ts twoslash
// @errors: 4104
let x: readonly string[] = [];
let y: string[] = [];

x = y;
y = x;
```

### 튜플 타입

_튜플 타입_은 정확히 몇 개의 요소를 포함하고 특정 위치에 정확히 어떤 타입을 포함하는지 아는 또 다른 종류의 `Array` 타입입니다.

```ts twoslash
type StringNumberPair = [string, number];
//                      ^^^^^^^^^^^^^^^^
```

여기서 `StringNumberPair`는 `string`과 `number`의 튜플 타입입니다.
`ReadonlyArray`처럼 런타임에 표현이 없지만, TypeScript에 중요합니다.
타입 시스템에서 `StringNumberPair`는 `0` 인덱스에 `string`을 포함하고 `1` 인덱스에 `number`를 포함하는 배열을 설명합니다.

```ts twoslash
function doSomething(pair: [string, number]) {
  const a = pair[0];
  //    ^?
  const b = pair[1];
  //    ^?
  // ...
}

doSomething(["hello", 42]);
```

요소 수를 넘어서 인덱싱하려고 하면 오류가 발생합니다.

```ts twoslash
// @errors: 2493
function doSomething(pair: [string, number]) {
  // ...

  const c = pair[2];
}
```

JavaScript의 배열 구조 분해를 사용하여 [튜플을 구조 분해](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Destructuring_assignment#Array_destructuring)할 수도 있습니다.

```ts twoslash
function doSomething(stringHash: [string, number]) {
  const [inputString, hash] = stringHash;

  console.log(inputString);
  //          ^?

  console.log(hash);
  //          ^?
}
```

> 튜플 타입은 각 요소의 의미가 "명확한" 규약 기반 API에서 유용합니다.
> 이것은 구조 분해할 때 변수 이름을 원하는 대로 지정할 수 있는 유연성을 제공합니다.
> 위 예제에서 요소 `0`과 `1`에 원하는 대로 이름을 지정할 수 있었습니다.
>
> 그러나 모든 사용자가 무엇이 명확한지에 대해 같은 관점을 가지고 있지 않으므로, 설명적인 속성 이름을 가진 객체를 사용하는 것이 API에 더 좋을 수 있는지 재고할 가치가 있습니다.

길이 검사 외에, 이와 같은 단순한 튜플 타입은 특정 인덱스에 대한 속성을 선언하고 `length`를 숫자 리터럴 타입으로 선언하는 `Array` 버전의 타입과 동일합니다.

```ts twoslash
interface StringNumberPair {
  // 특수 속성
  length: 2;
  0: string;
  1: number;

  // 기타 'Array<string | number>' 멤버...
  slice(start?: number, end?: number): Array<string | number>;
}
```

튜플은 요소의 타입 뒤에 물음표(`?`)를 써서 선택적 속성을 가질 수 있습니다.
선택적 튜플 요소는 끝에만 올 수 있으며, `length` 타입에도 영향을 미칩니다.

```ts twoslash
type Either2dOr3d = [number, number, number?];

function setCoordinate(coord: Either2dOr3d) {
  const [x, y, z] = coord;
  //           ^?

  console.log(`Provided coordinates had ${coord.length} dimensions`);
  //                                            ^?
}
```

튜플은 배열/튜플 타입이어야 하는 나머지 요소도 가질 수 있습니다.

```ts twoslash
type StringNumberBooleans = [string, number, ...boolean[]];
type StringBooleansNumber = [string, ...boolean[], number];
type BooleansStringNumber = [...boolean[], string, number];
```

- `StringNumberBooleans`는 처음 두 요소가 각각 `string`과 `number`이지만, 그 뒤에 몇 개의 `boolean`이든 가질 수 있는 튜플을 설명합니다.
- `StringBooleansNumber`는 첫 번째 요소가 `string`이고, 그 다음에 몇 개의 `boolean`이든 있고 `number`로 끝나는 튜플을 설명합니다.
- `BooleansStringNumber`는 시작 요소가 몇 개의 `boolean`이든 있고 `string` 다음 `number`로 끝나는 튜플을 설명합니다.

나머지 요소가 있는 튜플은 "길이"가 설정되지 않습니다 - 다른 위치에 잘 알려진 요소 세트만 있습니다.

```ts twoslash
type StringNumberBooleans = [string, number, ...boolean[]];
// ---cut---
const a: StringNumberBooleans = ["hello", 1];
const b: StringNumberBooleans = ["beautiful", 2, true];
const c: StringNumberBooleans = ["world", 3, true, false, true, false, true];
```

선택적 및 나머지 요소가 왜 유용할까요?
글쎄요, TypeScript가 튜플을 매개변수 목록과 대응시킬 수 있기 때문입니다.
튜플 타입은 [나머지 매개변수와 인수](/docs/handbook/2/functions.html#rest-parameters-and-arguments)에서 사용할 수 있으므로:

```ts twoslash
function readButtonInput(...args: [string, number, ...boolean[]]) {
  const [name, version, ...input] = args;
  // ...
}
```

는 기본적으로 다음과 동일합니다:

```ts twoslash
function readButtonInput(name: string, version: number, ...input: boolean[]) {
  // ...
}
```

이것은 나머지 매개변수로 가변적인 수의 인수를 받고, 최소 수의 요소가 필요하지만 중간 변수를 도입하고 싶지 않을 때 편리합니다.

### `readonly` 튜플 타입

튜플 타입에 대한 마지막 참고 사항 - 튜플 타입은 `readonly` 변형을 가지며, 앞에 `readonly` 수정자를 붙여 지정할 수 있습니다 - 배열 줄임말 구문과 마찬가지로.

```ts twoslash
function doSomething(pair: readonly [string, number]) {
  //                       ^^^^^^^^^^^^^^^^^^^^^^^^^
  // ...
}
```

예상할 수 있듯이, TypeScript에서 `readonly` 튜플의 어떤 속성에도 쓰는 것이 허용되지 않습니다.

```ts twoslash
// @errors: 2540
function doSomething(pair: readonly [string, number]) {
  pair[0] = "hello!";
}
```

튜플은 대부분의 코드에서 생성되고 수정되지 않는 경향이 있으므로, 가능한 경우 타입을 `readonly` 튜플로 어노테이션하는 것이 좋은 기본값입니다.
이것은 `const` 단언이 있는 배열 리터럴이 `readonly` 튜플 타입으로 추론된다는 점에서도 중요합니다.

```ts twoslash
// @errors: 2345
let point = [3, 4] as const;

function distanceFromOrigin([x, y]: [number, number]) {
  return Math.sqrt(x ** 2 + y ** 2);
}

distanceFromOrigin(point);
```

여기서 `distanceFromOrigin`은 요소를 수정하지 않지만, 변경 가능한 튜플을 기대합니다.
`point`의 타입이 `readonly [3, 4]`로 추론되었으므로, 해당 타입이 `point`의 요소가 변경되지 않을 것을 보장할 수 없으므로 `[number, number]`와 호환되지 않습니다.

# 좁히기

좁히기는 TypeScript가 처음에 선언된 것보다 더 구체적인 타입으로 변수 타입을 정제하는 과정입니다. 이 문서는 타입을 좁히기 위한 다양한 TypeScript 메커니즘을 설명합니다.

## 타입 가드 메서드

### `typeof` 타입 가드

JavaScript의 `typeof` 연산자를 사용하여 기본 타입을 검사할 수 있습니다. 이 연산자는 `"string"`, `"number"`, `"boolean"` 등의 문자열을 반환합니다.

```ts twoslash
function padLeft(padding: number | string, input: string): string {
  if (typeof padding === "number") {
    return " ".repeat(padding) + input;
    //                 ^?
  }
  return padding + input;
  //       ^?
}
```

TypeScript는 `typeof`가 반환하는 다양한 값을 이해합니다:
- `"string"`
- `"number"`
- `"bigint"`
- `"boolean"`
- `"symbol"`
- `"undefined"`
- `"object"`
- `"function"`

JavaScript의 특이한 점으로 인해 `typeof null`은 실제로 `"object"`를 반환한다는 점을 주의하세요. 이것은 JavaScript의 오래된 버그입니다.

```ts twoslash
// @errors: 18047
function printAll(strs: string | string[] | null) {
  if (typeof strs === "object") {
    for (const s of strs) {
      console.log(s);
    }
  } else if (typeof strs === "string") {
    console.log(strs);
  } else {
    // 아무것도 하지 않음
  }
}
```

### 참 같은 값 좁히기

JavaScript에서 조건문, `&&`, `||`, `if` 문, 불리언 부정(`!`) 등에서 모든 표현식을 사용할 수 있습니다. 예를 들어, `if` 문은 조건이 항상 `boolean` 타입일 것을 기대하지 않습니다.

JavaScript에서 `if`와 같은 구문은 먼저 조건을 `boolean`으로 "강제 변환"하여 이해한 다음, 결과가 `true`인지 `false`인지에 따라 분기를 선택합니다.

다음 값들은 `false`로 강제 변환됩니다:
- `0`
- `NaN`
- `""` (빈 문자열)
- `0n` (bigint 버전의 0)
- `null`
- `undefined`

다른 값들은 `true`로 강제 변환됩니다. `Boolean` 함수를 통해 값을 실행하거나, 짧은 이중 불리언 부정을 사용하여 언제든 `boolean`으로 강제 변환할 수 있습니다. (후자는 TypeScript가 좁은 리터럴 불리언 타입 `true`를 추론하고, 첫 번째는 `boolean` 타입으로 추론한다는 장점이 있습니다.)

```ts twoslash
// 이 두 결과 모두 'true'
Boolean("hello"); // 타입: boolean, 값: true
!!"world";        // 타입: true,    값: true
```

이 동작을 활용하는 것은 꽤 인기가 있으며, 특히 `null`이나 `undefined`와 같은 값에 대한 가드로 사용됩니다.

```ts twoslash
function printAll(strs: string | string[] | null) {
  if (strs && typeof strs === "object") {
    for (const s of strs) {
      console.log(s);
    }
  } else if (typeof strs === "string") {
    console.log(strs);
  }
}
```

하지만 기본형에 대한 참 같은 값 검사는 종종 오류가 발생하기 쉽습니다. 예를 들어 빈 문자열 `""`은 falsy하므로 의도치 않게 걸러질 수 있습니다.

### 동등성 좁히기

TypeScript는 `===`, `!==`, `==`, `!=`와 같은 동등성 검사를 사용하여 타입을 좁힐 수도 있습니다.

```ts twoslash
function example(x: string | number, y: string | boolean) {
  if (x === y) {
    // 이제 두 타입 모두에서 'string'을 호출할 수 있음
    x.toUpperCase();
    // ^?
    y.toLowerCase();
    // ^?
  } else {
    console.log(x);
    //          ^?
    console.log(y);
    //          ^?
  }
}
```

`x`와 `y`가 둘 다 같다고 확인했을 때, TypeScript는 그들의 타입도 같아야 한다는 것을 알았습니다. `x`와 `y`가 비교될 수 있는 유일한 공통 타입은 `string`이므로, 첫 번째 분기에서 `x`와 `y`는 `string`이어야 한다는 것을 알 수 있습니다.

특정 리터럴 값(변수와 반대로)에 대한 검사도 작동합니다.

```ts twoslash
function printAll(strs: string | string[] | null) {
  if (strs !== null) {
    if (typeof strs === "object") {
      for (const s of strs) {
        //            ^?
        console.log(s);
      }
    } else if (typeof strs === "string") {
      console.log(strs);
      //          ^?
    }
  }
}
```

JavaScript의 느슨한 동등성 검사 `==`와 `!=`도 올바르게 좁혀집니다. `== null` 검사는 구체적으로 `null` 값인지만 확인하는 것이 아니라 `undefined`인지도 확인합니다. `== undefined`도 마찬가지입니다.

```ts twoslash
interface Container {
  value: number | null | undefined;
}

function multiplyValue(container: Container, factor: number) {
  // null과 undefined를 타입에서 모두 제거
  if (container.value != null) {
    console.log(container.value);
    //                    ^?

    // 이제 안전하게 'container.value'를 곱할 수 있음
    container.value *= factor;
  }
}
```

### `in` 연산자 좁히기

JavaScript에는 객체나 프로토타입 체인에 특정 이름의 속성이 있는지 확인하는 연산자 `in`이 있습니다. TypeScript는 이것을 잠재적인 타입을 좁히는 방법으로 고려합니다.

```ts twoslash
type Fish = { swim: () => void };
type Bird = { fly: () => void };

function move(animal: Fish | Bird) {
  if ("swim" in animal) {
    return animal.swim();
  }

  return animal.fly();
}
```

선택적 속성은 좁히기의 양쪽에 존재합니다. 예를 들어, 사람은 수영도 하고 날 수도 있으므로(적절한 장비를 가지고) 양쪽 검사에 나타나야 합니다:

```ts twoslash
type Fish = { swim: () => void };
type Bird = { fly: () => void };
type Human = { swim?: () => void; fly?: () => void };

function move(animal: Fish | Bird | Human) {
  if ("swim" in animal) {
    animal;
//  ^?
  } else {
    animal;
//  ^?
  }
}
```

### `instanceof` 좁히기

JavaScript에는 값이 다른 값의 "인스턴스"인지 확인하는 연산자가 있습니다. 더 구체적으로, JavaScript에서 `x instanceof Foo`는 `x`의 _프로토타입 체인_에 `Foo.prototype`이 포함되어 있는지 확인합니다.

`instanceof`도 타입 가드이며, TypeScript는 `instanceof`로 보호되는 분기에서 좁힙니다.

```ts twoslash
function logValue(x: Date | string) {
  if (x instanceof Date) {
    console.log(x.toUTCString());
    //          ^?
  } else {
    console.log(x.toUpperCase());
    //          ^?
  }
}
```

### 할당

앞서 언급했듯이, 변수에 할당할 때 TypeScript는 할당의 오른쪽을 보고 왼쪽을 적절하게 좁힙니다.

```ts twoslash
let x = Math.random() < 0.5 ? 10 : "hello world!";
//  ^?
x = 1;

console.log(x);
//          ^?
x = "goodbye!";

console.log(x);
//          ^?
```

이러한 각 할당이 유효하다는 점에 주목하세요. 첫 번째 할당 후에 `x`의 관찰된 타입이 `number`로 변경되었지만, 여전히 `x`에 `string`을 할당할 수 있었습니다. 이는 `x`의 _선언된 타입_ - `x`가 시작한 타입 - 이 `string | number`이고, 할당 가능성은 항상 선언된 타입에 대해 검사되기 때문입니다.

## 제어 흐름 분석

지금까지 TypeScript가 특정 분기 내에서 어떻게 좁히는지에 대한 몇 가지 기본 예제를 살펴보았습니다. 하지만 단순히 모든 변수를 올라가면서 `if`, `while`, 조건문 등에서 타입 가드를 찾는 것 이상의 일이 일어나고 있습니다. 예를 들어:

```ts twoslash
function padLeft(padding: number | string, input: string) {
  if (typeof padding === "number") {
    return " ".repeat(padding) + input;
  }
  return padding + input;
}
```

`padLeft`는 첫 번째 `if` 블록 내에서 반환합니다. TypeScript는 이 코드를 분석할 수 있었고, `padding`이 `number`인 경우 본문의 나머지 부분(`return padding + input;`)이 _도달 불가능_하다는 것을 확인할 수 있었습니다. 결과적으로, 함수의 나머지 부분에서 `padding`의 타입에서 `number`를 제거할 수 있었습니다(`string | number`에서 `string`으로 좁히기).

도달 가능성에 기반한 이러한 코드 분석을 _제어 흐름 분석_이라고 하며, TypeScript는 타입 가드와 할당을 만날 때 타입을 좁히기 위해 이 흐름 분석을 사용합니다. 변수가 분석될 때, 제어 흐름이 분할되고 다시 병합될 수 있으며, 해당 변수가 각 지점에서 다른 타입을 가지는 것이 관찰될 수 있습니다.

```ts twoslash
function example() {
  let x: string | number | boolean;

  x = Math.random() < 0.5;

  console.log(x);
  //          ^?

  if (Math.random() < 0.5) {
    x = "hello";
    console.log(x);
    //          ^?
  } else {
    x = 100;
    console.log(x);
    //          ^?
  }

  return x;
  //     ^?
}
```

## 타입 술어 사용하기

지금까지 기존 JavaScript 구문으로 좁히기를 다루었지만, 때때로 코드 전체에서 타입이 어떻게 변경되는지에 대해 더 직접적인 제어를 원합니다.

사용자 정의 타입 가드를 정의하려면, 반환 타입이 _타입 술어_인 함수를 정의하면 됩니다:

```ts twoslash
type Fish = { swim: () => void };
type Bird = { fly: () => void };
declare function getSmallPet(): Fish | Bird;
// ---cut---
function isFish(pet: Fish | Bird): pet is Fish {
  return (pet as Fish).swim !== undefined;
}
```

`pet is Fish`는 이 예제에서 타입 술어입니다. 술어는 `parameterName is Type` 형태를 취하며, `parameterName`은 현재 함수 시그니처의 매개변수 이름이어야 합니다.

`isFish`가 어떤 변수와 함께 호출될 때마다, TypeScript는 원래 타입이 호환 가능한 경우 해당 변수를 해당 특정 타입으로 _좁힐_ 것입니다.

```ts twoslash
type Fish = { swim: () => void };
type Bird = { fly: () => void };
declare function getSmallPet(): Fish | Bird;
function isFish(pet: Fish | Bird): pet is Fish {
  return (pet as Fish).swim !== undefined;
}
// ---cut---
// swim과 fly 호출 모두 이제 괜찮음
let pet = getSmallPet();

if (isFish(pet)) {
  pet.swim();
} else {
  pet.fly();
}
```

TypeScript가 `if` 분기에서 `pet`이 `Fish`라는 것을 알 뿐만 아니라, `else` 분기에서 `Fish`가 _없다_는 것을 알고 있으므로 `Bird`가 있어야 한다는 점에 주목하세요.

`isFish` 타입 가드를 사용하여 `Fish | Bird` 배열을 필터링하고 `Fish` 배열을 얻을 수 있습니다:

```ts twoslash
type Fish = { swim: () => void; name: string };
type Bird = { fly: () => void; name: string };
declare function getSmallPet(): Fish | Bird;
function isFish(pet: Fish | Bird): pet is Fish {
  return (pet as Fish).swim !== undefined;
}
// ---cut---
const zoo: (Fish | Bird)[] = [getSmallPet(), getSmallPet(), getSmallPet()];
const underWater1: Fish[] = zoo.filter(isFish);
// 또는, 동등하게
const underWater2: Fish[] = zoo.filter(isFish) as Fish[];

// 더 복잡한 예제에서는 술어를 반복해야 할 수 있음
const underWater3: Fish[] = zoo.filter((pet): pet is Fish => {
  if (pet.name === "sharkey") return false;
  return isFish(pet);
});
```

## 단언 함수

타입은 [단언 함수](https://www.typescriptlang.org/docs/handbook/release-notes/typescript-3-7.html#assertion-functions)를 사용하여 좁힐 수도 있습니다.

## 판별 유니온

지금까지 살펴본 대부분의 예제는 `string`, `boolean`, `number`와 같은 간단한 타입으로 단일 변수를 좁히는 것에 집중했습니다. 이것이 일반적이지만, JavaScript에서 대부분의 경우 조금 더 복잡한 구조를 다루게 됩니다.

동기 부여를 위해, 원과 사각형과 같은 도형을 인코딩하려고 한다고 상상해 봅시다. 원은 반지름을 추적하고 사각형은 변 길이를 추적합니다. 어떤 도형을 다루고 있는지 알려주기 위해 `kind`라는 필드를 사용할 것입니다. 다음은 `Shape`를 정의하는 첫 번째 시도입니다.

```ts twoslash
interface Shape {
  kind: "circle" | "square";
  radius?: number;
  sideLength?: number;
}
```

문자열 리터럴 타입의 유니온을 사용하고 있다는 점에 주목하세요: `"circle"`과 `"square"`를 사용하여 도형을 원으로 처리해야 하는지 사각형으로 처리해야 하는지 알려줍니다. `string` 대신 `"circle" | "square"`를 사용하면 철자 오류 문제를 피할 수 있습니다.

```ts twoslash
// @errors: 2367
interface Shape {
  kind: "circle" | "square";
  radius?: number;
  sideLength?: number;
}

// ---cut---
function handleShape(shape: Shape) {
  // 이런!
  if (shape.kind === "rect") {
    // ...
  }
}
```

원을 다루는지 사각형을 다루는지에 따라 올바른 논리를 적용하는 `getArea` 함수를 작성할 수 있습니다. 먼저 원을 다루려고 합니다.

```ts twoslash
// @errors: 2532
interface Shape {
  kind: "circle" | "square";
  radius?: number;
  sideLength?: number;
}

// ---cut---
function getArea(shape: Shape) {
  return Math.PI * shape.radius ** 2;
}
```

`strictNullChecks`에서 오류가 발생합니다 - `radius`가 정의되지 않았을 수 있기 때문에 적절합니다. 하지만 `kind` 속성에 적절한 검사를 수행하면 어떨까요?

```ts twoslash
// @errors: 2532
interface Shape {
  kind: "circle" | "square";
  radius?: number;
  sideLength?: number;
}

// ---cut---
function getArea(shape: Shape) {
  if (shape.kind === "circle") {
    return Math.PI * shape.radius ** 2;
  }
}
```

흠, TypeScript는 여전히 여기서 무엇을 해야 할지 모릅니다. 우리가 타입 검사기보다 값에 대해 더 많이 아는 지점에 도달했습니다. 널 아님 단언(`shape.radius` 뒤에 `!`)을 사용하여 `radius`가 확실히 존재한다고 말할 수 있습니다.

```ts twoslash
interface Shape {
  kind: "circle" | "square";
  radius?: number;
  sideLength?: number;
}

// ---cut---
function getArea(shape: Shape) {
  if (shape.kind === "circle") {
    return Math.PI * shape.radius! ** 2;
  }
}
```

하지만 이것은 이상적으로 느껴지지 않습니다. 타입 검사기에게 `shape.radius`가 정의되어 있다고 말하기 위해 널 아님 단언(`!`)으로 소리쳐야 했지만, 코드를 이동하기 시작하면 이러한 단언은 오류가 발생하기 쉽습니다. 또한, `strictNullChecks` 외부에서 우리는 실수로 해당 필드에 접근할 수 있습니다(선택적 속성은 읽을 때 항상 존재한다고 가정되기 때문입니다). 확실히 더 잘할 수 있습니다.

이 `Shape` 인코딩의 문제는 타입 검사기가 `radius`나 `sideLength`가 `kind` 속성에 기반하여 존재하는지 알 방법이 없다는 것입니다. 우리가 아는 것을 타입 검사기에게 전달해야 합니다. 그것을 염두에 두고, `Shape`를 정의하는 또 다른 시도를 해봅시다.

```ts twoslash
interface Circle {
  kind: "circle";
  radius: number;
}

interface Square {
  kind: "square";
  sideLength: number;
}

type Shape = Circle | Square;
```

여기서 `Shape`를 `kind` 속성에 대해 다른 값을 가진 두 타입으로 적절하게 분리했지만, `radius`와 `sideLength`는 각각의 타입에서 필수 속성으로 선언되었습니다.

`Shape`의 `radius`에 직접 접근하려고 하면 무슨 일이 일어나는지 봅시다.

```ts twoslash
// @errors: 2339
interface Circle {
  kind: "circle";
  radius: number;
}

interface Square {
  kind: "square";
  sideLength: number;
}

type Shape = Circle | Square;

// ---cut---
function getArea(shape: Shape) {
  return Math.PI * shape.radius ** 2;
}
```

첫 번째 `Shape` 정의와 마찬가지로, 이것은 여전히 오류입니다. `radius`가 선택적일 때, 오류가 발생했습니다(오직 `strictNullChecks`가 활성화된 경우) TypeScript는 속성이 존재하는지 알 수 없었기 때문입니다. 이제 `Shape`가 유니온이므로, TypeScript는 `shape`이 `Square`일 수 있고, `Square`는 `radius`가 정의되어 있지 않다고 말합니다! 두 해석 모두 올바르지만, `strictNullChecks`가 어떻게 구성되어 있든 `Shape`의 유니온 인코딩만 오류를 일으킵니다.

하지만 `kind` 속성을 다시 확인하면 어떨까요?

```ts twoslash
interface Circle {
  kind: "circle";
  radius: number;
}

interface Square {
  kind: "square";
  sideLength: number;
}

type Shape = Circle | Square;

// ---cut---
function getArea(shape: Shape) {
  if (shape.kind === "circle") {
    return Math.PI * shape.radius ** 2;
    //               ^?
  }
}
```

오류가 사라졌습니다! 유니온의 모든 타입이 리터럴 타입을 가진 공통 속성을 포함할 때, TypeScript는 이것을 _판별 유니온_으로 간주하고, 유니온의 멤버를 좁힐 수 있습니다.

이 경우, `kind`가 그 공통 속성이었습니다(이것이 `Shape`의 _판별_ 속성으로 간주되는 것입니다). `kind` 속성이 `"circle"`인지 확인하면 `kind` 속성이 `"circle"`이 아닌 `Shape`의 모든 타입이 제거되었습니다. 이것이 `shape`을 `Circle` 타입으로 좁혔습니다.

같은 검사가 `switch` 문에서도 작동합니다. 이제 성가신 `!` 널 아님 단언 없이 완전한 `getArea`를 작성할 수 있습니다.

```ts twoslash
interface Circle {
  kind: "circle";
  radius: number;
}

interface Square {
  kind: "square";
  sideLength: number;
}

type Shape = Circle | Square;

// ---cut---
function getArea(shape: Shape) {
  switch (shape.kind) {
    case "circle":
      return Math.PI * shape.radius ** 2;
      //               ^?
    case "square":
      return shape.sideLength ** 2;
      //     ^?
  }
}
```

여기서 중요한 것은 `Shape`의 인코딩이었습니다. TypeScript에 올바른 정보를 전달하는 것 - `Circle`과 `Square`가 실제로 특정 `kind` 필드를 가진 두 개의 별개 타입이라는 것 - 이 중요했습니다. 그렇게 하면 우리가 어차피 작성했을 것과 다르지 않은 타입 안전 TypeScript 코드를 작성할 수 있습니다. 거기서부터, 타입 시스템은 "올바른" 일을 하고 `switch` 문의 각 분기에서 타입을 알아낼 수 있었습니다.

> 참고로, 위 예제에서 반환 타입 추론이 어떻게 `switch` 문의 다른 분기에서 결정된 반환 타입을 통해 작동하는지 살펴보세요.

판별 유니온은 원과 사각형에 대해 이야기하는 것 이상에 유용합니다. 네트워크를 통해 메시지를 보낼 때(클라이언트/서버 통신)나 상태 관리 프레임워크에서 뮤테이션을 인코딩할 때와 같이 JavaScript에서 모든 종류의 메시징 스키마를 표현하는 데 좋습니다.

## `never` 타입

좁힐 때, 유니온의 옵션을 모든 가능성을 제거하고 아무것도 남지 않은 지점까지 줄일 수 있습니다. 그러한 경우, TypeScript는 존재해서는 안 되는 상태를 나타내기 위해 `never` 타입을 사용합니다.

## 철저한 검사

`never` 타입은 모든 타입에 할당 가능합니다; 그러나 어떤 타입도 `never`에 할당할 수 없습니다(`never` 자체를 제외하고). 이것은 `switch` 문에서 철저한 검사를 수행하기 위해 좁히기와 `never`에 의존할 수 있다는 것을 의미합니다.

예를 들어, 모든 가능한 케이스가 처리되었을 때 `shape`에 `never`를 할당하려고 하는 `getArea` 함수에 `default`를 추가하면 오류가 발생하지 않습니다.

```ts twoslash
interface Circle {
  kind: "circle";
  radius: number;
}

interface Square {
  kind: "square";
  sideLength: number;
}
// ---cut---
type Shape = Circle | Square;

function getArea(shape: Shape) {
  switch (shape.kind) {
    case "circle":
      return Math.PI * shape.radius ** 2;
    case "square":
      return shape.sideLength ** 2;
    default:
      const _exhaustiveCheck: never = shape;
      return _exhaustiveCheck;
  }
}
```

`Shape` 유니온에 새 멤버를 추가하면 TypeScript 오류가 발생합니다:

```ts twoslash
// @errors: 2322
interface Circle {
  kind: "circle";
  radius: number;
}

interface Square {
  kind: "square";
  sideLength: number;
}

interface Triangle {
  kind: "triangle";
  sideLength: number;
}

type Shape = Circle | Square | Triangle;

function getArea(shape: Shape) {
  switch (shape.kind) {
    case "circle":
      return Math.PI * shape.radius ** 2;
    case "square":
      return shape.sideLength ** 2;
    default:
      const _exhaustiveCheck: never = shape;
      return _exhaustiveCheck;
  }
}
```

이것은 `Triangle`이 아직 처리되지 않았음을 상기시켜주므로 유용합니다.

---
title: 조건부 타입
layout: docs
permalink: /docs/handbook/2/conditional-types.html
oneline: "타입 시스템에서 if 문처럼 동작하는 타입 만들기"
---

대부분의 유용한 프로그램의 핵심에서 우리는 입력에 따라 결정을 내려야 합니다.
JavaScript 프로그램도 다르지 않지만, 값을 쉽게 검사할 수 있다는 점을 감안하면 그러한 결정은 입력의 타입에도 기반합니다.
_조건부 타입_은 입력과 출력 타입 간의 관계를 설명하는 데 도움이 됩니다.

```ts twoslash
interface Animal {
  live(): void;
}
interface Dog extends Animal {
  woof(): void;
}

type Example1 = Dog extends Animal ? number : string;
//   ^?

type Example2 = RegExp extends Animal ? number : string;
//   ^?
```

조건부 타입은 JavaScript의 조건 표현식(`condition ? trueExpression : falseExpression`)과 약간 비슷한 형태를 취합니다:

```ts twoslash
type SomeType = any;
type OtherType = any;
type TrueType = any;
type FalseType = any;
type Stuff =
  // ---cut---
  SomeType extends OtherType ? TrueType : FalseType;
```

`extends` 왼쪽의 타입이 오른쪽의 타입에 할당 가능하면, 첫 번째 분기("true" 분기)의 타입을 얻게 됩니다; 그렇지 않으면 후자의 분기("false" 분기)의 타입을 얻게 됩니다.

위의 예제에서 조건부 타입은 즉시 유용해 보이지 않을 수 있습니다 - `Dog extends Animal`인지 여부를 스스로 알 수 있고 `number` 또는 `string`을 선택할 수 있습니다!
하지만 조건부 타입의 힘은 제네릭과 함께 사용할 때 나옵니다.

예를 들어, 다음 `createLabel` 함수를 살펴봅시다:

```ts twoslash
interface IdLabel {
  id: number /* 일부 필드 */;
}
interface NameLabel {
  name: string /* 다른 필드 */;
}

function createLabel(id: number): IdLabel;
function createLabel(name: string): NameLabel;
function createLabel(nameOrId: string | number): IdLabel | NameLabel;
function createLabel(nameOrId: string | number): IdLabel | NameLabel {
  throw "unimplemented";
}
```

createLabel에 대한 이러한 오버로드는 입력 타입에 따라 선택을 하는 단일 JavaScript 함수를 설명합니다. 몇 가지 사항에 주목하세요:

1. 라이브러리가 API 전체에서 동일한 종류의 선택을 반복해서 해야 한다면, 이것은 번거로워집니다.
2. 우리는 세 개의 오버로드를 만들어야 합니다: 타입을 _확신할_ 수 있는 각 경우에 대해 하나씩(`string`용 하나와 `number`용 하나), 그리고 가장 일반적인 경우(`string | number`를 받는 것)에 대해 하나. `createLabel`이 처리할 수 있는 모든 새로운 타입에 대해 오버로드 수가 기하급수적으로 증가합니다.

대신, 조건부 타입으로 해당 로직을 인코딩할 수 있습니다:

```ts twoslash
interface IdLabel {
  id: number /* 일부 필드 */;
}
interface NameLabel {
  name: string /* 다른 필드 */;
}
// ---cut---
type NameOrId<T extends number | string> = T extends number
  ? IdLabel
  : NameLabel;
```

그런 다음 해당 조건부 타입을 사용하여 오버로드를 오버로드 없는 단일 함수로 단순화할 수 있습니다.

```ts twoslash
interface IdLabel {
  id: number /* 일부 필드 */;
}
interface NameLabel {
  name: string /* 다른 필드 */;
}
type NameOrId<T extends number | string> = T extends number
  ? IdLabel
  : NameLabel;
// ---cut---
function createLabel<T extends number | string>(idOrName: T): NameOrId<T> {
  throw "unimplemented";
}

let a = createLabel("typescript");
//  ^?

let b = createLabel(2.8);
//  ^?

let c = createLabel(Math.random() ? "hello" : 42);
//  ^?
```

### 조건부 타입 제약

종종, 조건부 타입의 검사는 우리에게 새로운 정보를 제공합니다.
타입 가드로 좁히는 것이 더 구체적인 타입을 제공할 수 있는 것처럼, 조건부 타입의 true 분기는 검사하는 타입에 의해 제네릭을 추가로 제약합니다.

예를 들어, 다음을 살펴봅시다:

```ts twoslash
// @errors: 2536
type MessageOf<T> = T["message"];
```

이 예제에서 TypeScript는 `T`가 `message`라는 속성을 가지고 있다고 알려지지 않았기 때문에 오류를 발생시킵니다.
`T`를 제약할 수 있고, TypeScript는 더 이상 불평하지 않을 것입니다:

```ts twoslash
type MessageOf<T extends { message: unknown }> = T["message"];

interface Email {
  message: string;
}

type EmailMessageContents = MessageOf<Email>;
//   ^?
```

그러나 `MessageOf`가 모든 타입을 받고 `message` 속성을 사용할 수 없는 경우 `never`와 같은 것으로 기본 설정되도록 하려면 어떻게 해야 할까요?
제약을 밖으로 이동하고 조건부 타입을 도입하여 이를 수행할 수 있습니다:

```ts twoslash
type MessageOf<T> = T extends { message: unknown } ? T["message"] : never;

interface Email {
  message: string;
}

interface Dog {
  bark(): void;
}

type EmailMessageContents = MessageOf<Email>;
//   ^?

type DogMessageContents = MessageOf<Dog>;
//   ^?
```

true 분기 내에서, TypeScript는 `T`가 `message` 속성을 _가질 것_임을 압니다.

또 다른 예로, 배열 타입을 요소 타입으로 평탄화하지만 그렇지 않으면 그대로 두는 `Flatten`이라는 타입을 작성할 수 있습니다:

```ts twoslash
type Flatten<T> = T extends any[] ? T[number] : T;

// 요소 타입을 추출합니다.
type Str = Flatten<string[]>;
//   ^?

// 타입을 그대로 둡니다.
type Num = Flatten<number>;
//   ^?
```

`Flatten`에 배열 타입이 주어지면, `number`로 인덱스 접근을 사용하여 `string[]`의 요소 타입을 가져옵니다.
그렇지 않으면 주어진 타입을 그대로 반환합니다.

### 조건부 타입 내에서 추론

우리는 방금 조건부 타입을 사용하여 제약을 적용하고 타입을 추출하는 것을 발견했습니다.
이것은 너무나 일반적인 작업이어서 조건부 타입이 더 쉽게 만들어줍니다.

조건부 타입은 `infer` 키워드를 사용하여 true 분기에서 비교하는 타입에서 추론하는 방법을 제공합니다.
예를 들어, 인덱스 접근 타입으로 "수동으로" 가져오는 대신 `Flatten`에서 요소 타입을 추론할 수 있었습니다:

```ts twoslash
type Flatten<Type> = Type extends Array<infer Item> ? Item : Type;
```

여기서 `infer` 키워드를 사용하여 true 분기 내에서 `Type`의 요소 타입을 검색하는 방법을 지정하는 대신 `Item`이라는 새로운 제네릭 타입 변수를 선언적으로 도입했습니다.
이렇게 하면 관심 있는 타입의 구조를 파고들어 탐색하는 방법에 대해 생각할 필요가 없습니다.

`infer` 키워드를 사용하여 유용한 헬퍼 타입 별칭을 작성할 수 있습니다.
예를 들어, 간단한 경우에 함수 타입에서 반환 타입을 추출할 수 있습니다:

```ts twoslash
type GetReturnType<Type> = Type extends (...args: never[]) => infer Return
  ? Return
  : never;

type Num = GetReturnType<() => number>;
//   ^?

type Str = GetReturnType<(x: string) => string>;
//   ^?

type Bools = GetReturnType<(a: boolean, b: boolean) => boolean[]>;
//   ^?
```

여러 호출 시그니처가 있는 타입(오버로드된 함수의 타입과 같은)에서 추론할 때, 추론은 _마지막_ 시그니처(아마도 가장 허용적인 모든 경우를 포괄하는 케이스)에서 이루어집니다. 인자 타입 목록에 기반한 오버로드 해결을 수행하는 것은 불가능합니다.

```ts twoslash
declare function stringOrNum(x: string): number;
declare function stringOrNum(x: number): string;
declare function stringOrNum(x: string | number): string | number;

type T1 = ReturnType<typeof stringOrNum>;
//   ^?
```

## 분배적 조건부 타입

조건부 타입이 제네릭 타입에 작용할 때, 유니온 타입이 주어지면 _분배적_이 됩니다.
예를 들어, 다음을 살펴봅시다:

```ts twoslash
type ToArray<Type> = Type extends any ? Type[] : never;
```

`ToArray`에 유니온 타입을 넣으면, 조건부 타입이 해당 유니온의 각 멤버에 적용됩니다.

```ts twoslash
type ToArray<Type> = Type extends any ? Type[] : never;

type StrArrOrNumArr = ToArray<string | number>;
//   ^?
```

여기서 발생하는 것은 `ToArray`가 다음에 대해 분배된다는 것입니다:

```ts twoslash
type StrArrOrNumArr =
  // ---cut---
  string | number;
```

그리고 유니온의 각 멤버 타입에 대해 매핑하여 효과적으로 다음이 됩니다:

```ts twoslash
type ToArray<Type> = Type extends any ? Type[] : never;
type StrArrOrNumArr =
  // ---cut---
  ToArray<string> | ToArray<number>;
```

이것은 우리에게 다음을 남깁니다:

```ts twoslash
type StrArrOrNumArr =
  // ---cut---
  string[] | number[];
```

일반적으로 분배성이 원하는 동작입니다.
그 동작을 피하려면 `extends` 키워드의 각 측면을 대괄호로 둘러쌀 수 있습니다.

```ts twoslash
type ToArrayNonDist<Type> = [Type] extends [any] ? Type[] : never;

// 'ArrOfStrOrNum'은 더 이상 유니온이 아닙니다.
type ArrOfStrOrNum = ToArrayNonDist<string | number>;
//   ^?
```

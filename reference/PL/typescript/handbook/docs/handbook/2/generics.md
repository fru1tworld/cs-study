---
title: 제네릭
layout: docs
permalink: /docs/handbook/2/generics.html
oneline: 매개변수를 받는 타입
---

소프트웨어 엔지니어링의 주요 부분은 잘 정의되고 일관된 API를 가질 뿐만 아니라 재사용 가능한 컴포넌트를 만드는 것입니다.
오늘의 데이터뿐만 아니라 내일의 데이터에서도 작동할 수 있는 컴포넌트는 대규모 소프트웨어 시스템을 구축하는 데 가장 유연한 기능을 제공합니다.

C#이나 Java와 같은 언어에서 재사용 가능한 컴포넌트를 만들기 위한 도구 상자의 주요 도구 중 하나는 _제네릭_입니다. 즉, 단일 타입이 아닌 다양한 타입에서 작동할 수 있는 컴포넌트를 만들 수 있습니다.
이를 통해 사용자는 이러한 컴포넌트를 사용하고 자신만의 타입을 사용할 수 있습니다.

## 제네릭의 Hello World

시작하기 위해 제네릭의 "hello world"인 항등 함수를 만들어 봅시다.
항등 함수는 전달된 것을 그대로 반환하는 함수입니다.
`echo` 명령어와 유사한 방식으로 생각할 수 있습니다.

제네릭 없이는 항등 함수에 특정 타입을 지정해야 합니다:

```ts twoslash
function identity(arg: number): number {
  return arg;
}
```

또는 `any` 타입을 사용하여 항등 함수를 설명할 수 있습니다:

```ts twoslash
function identity(arg: any): any {
  return arg;
}
```

`any`를 사용하면 함수가 `arg`의 타입으로 모든 타입을 받아들이게 되어 확실히 제네릭하지만, 함수가 반환될 때 해당 타입이 무엇이었는지에 대한 정보를 잃게 됩니다.
만약 숫자를 전달하면, 어떤 타입이든 반환될 수 있다는 정보만 남게 됩니다.

대신, 반환되는 것을 나타내는 데도 사용할 수 있는 방식으로 인자의 타입을 캡처하는 방법이 필요합니다.
여기서 값이 아닌 타입에 작동하는 특별한 종류의 변수인 _타입 변수_를 사용합니다.

```ts twoslash
function identity<Type>(arg: Type): Type {
  return arg;
}
```

이제 항등 함수에 타입 변수 `Type`을 추가했습니다.
이 `Type`은 사용자가 제공하는 타입(예: `number`)을 캡처하여 나중에 해당 정보를 사용할 수 있게 합니다.
여기서 `Type`을 반환 타입으로도 다시 사용합니다. 검사해보면, 인자와 반환 타입에 동일한 타입이 사용되는 것을 볼 수 있습니다.
이를 통해 함수의 한쪽에서 다른 쪽으로 타입 정보를 전달할 수 있습니다.

이 버전의 `identity` 함수는 다양한 타입에서 작동하므로 제네릭하다고 말합니다.
`any`를 사용하는 것과 달리, 인자와 반환 타입에 숫자를 사용한 첫 번째 `identity` 함수만큼 정확합니다(즉, 정보를 잃지 않습니다).

제네릭 항등 함수를 작성한 후에는 두 가지 방법 중 하나로 호출할 수 있습니다.
첫 번째 방법은 타입 인자를 포함한 모든 인자를 함수에 전달하는 것입니다:

```ts twoslash
function identity<Type>(arg: Type): Type {
  return arg;
}
// ---cut---
let output = identity<string>("myString");
//       ^?
```

여기서 `()`가 아닌 `<>` 안에서 인자로 `Type`을 명시적으로 `string`으로 설정했습니다.

두 번째 방법은 아마도 가장 일반적인 방법입니다. 여기서 _타입 인자 추론_을 사용합니다 -- 즉, 전달하는 인자의 타입에 따라 컴파일러가 자동으로 `Type`의 값을 설정하도록 합니다:

```ts twoslash
function identity<Type>(arg: Type): Type {
  return arg;
}
// ---cut---
let output = identity("myString");
//       ^?
```

꺾쇠 괄호(`<>`) 안에 타입을 명시적으로 전달할 필요가 없었습니다; 컴파일러가 `"myString"` 값을 보고 `Type`을 해당 타입으로 설정했습니다.
타입 인자 추론은 코드를 더 짧고 읽기 쉽게 유지하는 데 유용한 도구가 될 수 있지만, 더 복잡한 예에서 발생할 수 있는 것처럼 컴파일러가 타입을 추론하지 못할 때는 이전 예제에서처럼 타입 인자를 명시적으로 전달해야 할 수도 있습니다.

## 제네릭 타입 변수 작업

제네릭을 사용하기 시작하면, `identity`와 같은 제네릭 함수를 만들 때 컴파일러가 함수 본문에서 제네릭으로 타입이 지정된 매개변수를 올바르게 사용하도록 강제하는 것을 알게 됩니다.
즉, 이러한 매개변수를 모든 타입이 될 수 있는 것처럼 실제로 취급해야 합니다.

앞서의 `identity` 함수를 살펴봅시다:

```ts twoslash
function identity<Type>(arg: Type): Type {
  return arg;
}
```

각 호출마다 인자 `arg`의 길이를 콘솔에 기록하고 싶다면 어떻게 해야 할까요?
다음과 같이 작성하고 싶을 수 있습니다:

```ts twoslash
// @errors: 2339
function loggingIdentity<Type>(arg: Type): Type {
  console.log(arg.length);
  return arg;
}
```

그렇게 하면 컴파일러가 `arg`의 `.length` 멤버를 사용하고 있지만, `arg`가 이 멤버를 가지고 있다고 어디에서도 말하지 않았다는 오류를 표시합니다.
앞서 이러한 타입 변수는 모든 타입을 대신한다고 말했으므로, 이 함수를 사용하는 사람이 `.length` 멤버가 없는 `number`를 대신 전달했을 수 있습니다.

이 함수가 `Type` 자체가 아니라 `Type`의 배열에서 작동하도록 의도했다고 가정해 봅시다. 배열로 작업하고 있으므로 `.length` 멤버를 사용할 수 있어야 합니다.
다른 타입의 배열을 만드는 것처럼 이것을 설명할 수 있습니다:

```ts twoslash {1}
function loggingIdentity<Type>(arg: Type[]): Type[] {
  console.log(arg.length);
  return arg;
}
```

`loggingIdentity`의 타입을 "제네릭 함수 `loggingIdentity`는 타입 매개변수 `Type`과 `Type` 배열인 인자 `arg`를 받아 `Type` 배열을 반환합니다"로 읽을 수 있습니다.
숫자 배열을 전달하면, `Type`이 `number`에 바인딩되므로 숫자 배열을 반환받습니다.
이를 통해 전체 타입이 아닌 작업 중인 타입의 일부로 제네릭 타입 변수 `Type`을 사용할 수 있어 더 큰 유연성을 제공합니다.

동일한 예제를 다음과 같이 작성할 수도 있습니다:

```ts twoslash {1}
function loggingIdentity<Type>(arg: Array<Type>): Array<Type> {
  console.log(arg.length); // 배열은 .length를 가지므로 더 이상 오류가 없습니다
  return arg;
}
```

다른 언어에서 이러한 스타일의 타입에 이미 익숙할 수 있습니다.
다음 섹션에서는 `Array<Type>`과 같은 자신만의 제네릭 타입을 만드는 방법을 다룹니다.

## 제네릭 타입

이전 섹션에서는 다양한 타입에서 작동하는 제네릭 항등 함수를 만들었습니다.
이 섹션에서는 함수 자체의 타입과 제네릭 인터페이스를 만드는 방법을 살펴봅니다.

제네릭 함수의 타입은 비제네릭 함수와 마찬가지로, 함수 선언과 유사하게 타입 매개변수가 먼저 나열됩니다:

```ts twoslash
function identity<Type>(arg: Type): Type {
  return arg;
}

let myIdentity: <Type>(arg: Type) => Type = identity;
```

타입 변수의 수와 타입 변수가 사용되는 방식이 일치하는 한, 타입에서 제네릭 타입 매개변수에 다른 이름을 사용할 수도 있습니다.

```ts twoslash
function identity<Type>(arg: Type): Type {
  return arg;
}

let myIdentity: <Input>(arg: Input) => Input = identity;
```

객체 리터럴 타입의 호출 시그니처로 제네릭 타입을 작성할 수도 있습니다:

```ts twoslash
function identity<Type>(arg: Type): Type {
  return arg;
}

let myIdentity: { <Type>(arg: Type): Type } = identity;
```

이것은 첫 번째 제네릭 인터페이스를 작성하는 것으로 이어집니다.
이전 예제의 객체 리터럴을 가져와 인터페이스로 이동해 봅시다:

```ts twoslash
interface GenericIdentityFn {
  <Type>(arg: Type): Type;
}

function identity<Type>(arg: Type): Type {
  return arg;
}

let myIdentity: GenericIdentityFn = identity;
```

유사한 예제에서, 제네릭 매개변수를 전체 인터페이스의 매개변수로 이동하고 싶을 수 있습니다.
이를 통해 어떤 타입에 대해 제네릭인지 확인할 수 있습니다(예: `Dictionary` 대신 `Dictionary<string>`).
이렇게 하면 인터페이스의 다른 모든 멤버에게 타입 매개변수가 표시됩니다.

```ts twoslash
interface GenericIdentityFn<Type> {
  (arg: Type): Type;
}

function identity<Type>(arg: Type): Type {
  return arg;
}

let myIdentity: GenericIdentityFn<number> = identity;
```

예제가 약간 다른 것으로 변경되었습니다.
제네릭 함수를 설명하는 대신, 이제 제네릭 타입의 일부인 비제네릭 함수 시그니처가 있습니다.
`GenericIdentityFn`을 사용할 때, 이제 해당 타입 인자(여기서는 `number`)도 지정해야 하며, 기본 호출 시그니처가 사용할 것을 효과적으로 고정합니다.
타입 매개변수를 호출 시그니처에 직접 넣을 때와 인터페이스 자체에 넣을 때를 이해하면 타입의 어떤 측면이 제네릭인지 설명하는 데 도움이 됩니다.

제네릭 인터페이스 외에도 제네릭 클래스를 만들 수 있습니다.
제네릭 열거형과 네임스페이스는 만들 수 없습니다.

## 제네릭 클래스

제네릭 클래스는 제네릭 인터페이스와 유사한 형태를 가집니다.
제네릭 클래스는 클래스 이름 뒤에 꺾쇠 괄호(`<>`) 안에 제네릭 타입 매개변수 목록을 가집니다.

```ts twoslash
// @strict: false
class GenericNumber<NumType> {
  zeroValue: NumType;
  add: (x: NumType, y: NumType) => NumType;
}

let myGenericNumber = new GenericNumber<number>();
myGenericNumber.zeroValue = 0;
myGenericNumber.add = function (x, y) {
  return x + y;
};
```

이것은 `GenericNumber` 클래스의 매우 직접적인 사용이지만, `number` 타입만 사용하도록 제한하는 것이 없다는 것을 알아차렸을 수 있습니다.
대신 `string`이나 더 복잡한 객체를 사용할 수도 있습니다.

```ts twoslash
// @strict: false
class GenericNumber<NumType> {
  zeroValue: NumType;
  add: (x: NumType, y: NumType) => NumType;
}
// ---cut---
let stringNumeric = new GenericNumber<string>();
stringNumeric.zeroValue = "";
stringNumeric.add = function (x, y) {
  return x + y;
};

console.log(stringNumeric.add(stringNumeric.zeroValue, "test"));
```

인터페이스와 마찬가지로, 클래스 자체에 타입 매개변수를 넣으면 클래스의 모든 속성이 동일한 타입으로 작동하도록 보장할 수 있습니다.

[클래스에 대한 섹션](/docs/handbook/2/classes.html)에서 다루듯이, 클래스는 타입에 두 가지 측면이 있습니다: 정적 측면과 인스턴스 측면.
제네릭 클래스는 정적 측면이 아닌 인스턴스 측면에서만 제네릭이므로, 클래스로 작업할 때 정적 멤버는 클래스의 타입 매개변수를 사용할 수 없습니다.

## 제네릭 제약 조건

이전 예제에서 기억하시겠지만, 때때로 해당 타입 집합이 어떤 기능을 가질지 _어느 정도_ 알고 있는 타입 집합에서 작동하는 제네릭 함수를 작성하고 싶을 수 있습니다.
`loggingIdentity` 예제에서 `arg`의 `.length` 속성에 접근하고 싶었지만, 컴파일러가 모든 타입이 `.length` 속성을 가지고 있다는 것을 증명할 수 없었으므로, 이러한 가정을 할 수 없다고 경고합니다.

```ts twoslash
// @errors: 2339
function loggingIdentity<Type>(arg: Type): Type {
  console.log(arg.length);
  return arg;
}
```

모든 타입에서 작동하는 대신, `.length` 속성을 *가지고 있는* 모든 타입에서 작동하도록 이 함수를 제한하고 싶습니다.
타입이 이 멤버를 가지고 있는 한 허용하지만, 최소한 이 멤버가 있어야 합니다.
그렇게 하려면 `Type`이 될 수 있는 것에 대한 제약으로 요구 사항을 나열해야 합니다.

그렇게 하기 위해 제약을 설명하는 인터페이스를 만들 것입니다.
여기서 단일 `.length` 속성을 가진 인터페이스를 만들고 이 인터페이스와 `extends` 키워드를 사용하여 제약을 나타냅니다:

```ts twoslash
interface Lengthwise {
  length: number;
}

function loggingIdentity<Type extends Lengthwise>(arg: Type): Type {
  console.log(arg.length); // 이제 .length 속성이 있다는 것을 알므로 더 이상 오류가 없습니다
  return arg;
}
```

제네릭 함수가 이제 제약되었으므로, 더 이상 모든 타입에서 작동하지 않습니다:

```ts twoslash
// @errors: 2345
interface Lengthwise {
  length: number;
}

function loggingIdentity<Type extends Lengthwise>(arg: Type): Type {
  console.log(arg.length);
  return arg;
}
// ---cut---
loggingIdentity(3);
```

대신, 필요한 모든 속성을 가진 타입의 값을 전달해야 합니다:

```ts twoslash
interface Lengthwise {
  length: number;
}

function loggingIdentity<Type extends Lengthwise>(arg: Type): Type {
  console.log(arg.length);
  return arg;
}
// ---cut---
loggingIdentity({ length: 10, value: 3 });
```

## 제네릭 제약 조건에서 타입 매개변수 사용

다른 타입 매개변수에 의해 제약되는 타입 매개변수를 선언할 수 있습니다.
예를 들어, 여기서 객체의 이름이 주어지면 해당 속성을 가져오고 싶습니다.
`obj`에 존재하지 않는 속성을 실수로 가져오지 않도록 두 타입 사이에 제약을 둡니다:

```ts twoslash
// @errors: 2345
function getProperty<Type, Key extends keyof Type>(obj: Type, key: Key) {
  return obj[key];
}

let x = { a: 1, b: 2, c: 3, d: 4 };

getProperty(x, "a");
getProperty(x, "m");
```

## 제네릭에서 클래스 타입 사용

제네릭을 사용하여 TypeScript에서 팩토리를 만들 때, 생성자 함수로 클래스 타입을 참조해야 합니다. 예를 들어,

```ts twoslash
function create<Type>(c: { new (): Type }): Type {
  return new c();
}
```

더 고급 예제는 prototype 속성을 사용하여 생성자 함수와 클래스 타입의 인스턴스 측면 사이의 관계를 추론하고 제약합니다.

```ts twoslash
// @strict: false
class BeeKeeper {
  hasMask: boolean = true;
}

class ZooKeeper {
  nametag: string = "Mikle";
}

class Animal {
  numLegs: number = 4;
}

class Bee extends Animal {
  numLegs = 6;
  keeper: BeeKeeper = new BeeKeeper();
}

class Lion extends Animal {
  keeper: ZooKeeper = new ZooKeeper();
}

function createInstance<A extends Animal>(c: new () => A): A {
  return new c();
}

createInstance(Lion).keeper.nametag;
createInstance(Bee).keeper.hasMask;
```

이 패턴은 [믹스인](/docs/handbook/mixins.html) 디자인 패턴에 사용됩니다.

## 제네릭 매개변수 기본값

제네릭 타입 매개변수에 기본값을 선언하면 해당 타입 인자를 지정하는 것이 선택 사항이 됩니다. 예를 들어, 새로운 `HTMLElement`를 만드는 함수입니다. 인자 없이 함수를 호출하면 `HTMLDivElement`가 생성되고; 첫 번째 인자로 요소를 전달하면 해당 인자 타입의 요소가 생성됩니다. 선택적으로 자식 목록도 전달할 수 있습니다. 이전에는 함수를 다음과 같이 정의해야 했습니다:

```ts twoslash
type Container<T, U> = {
  element: T;
  children: U;
};

// ---cut---
declare function create(): Container<HTMLDivElement, HTMLDivElement[]>;
declare function create<T extends HTMLElement>(element: T): Container<T, T[]>;
declare function create<T extends HTMLElement, U extends HTMLElement>(
  element: T,
  children: U[]
): Container<T, U[]>;
```

제네릭 매개변수 기본값을 사용하면 다음과 같이 줄일 수 있습니다:

```ts twoslash
type Container<T, U> = {
  element: T;
  children: U;
};

// ---cut---
declare function create<T extends HTMLElement = HTMLDivElement, U extends HTMLElement[] = T[]>(
  element?: T,
  children?: U
): Container<T, U>;

const div = create();
//    ^?

const p = create(new HTMLParagraphElement());
//    ^?
```

제네릭 매개변수 기본값은 다음 규칙을 따릅니다:

- 타입 매개변수는 기본값이 있으면 선택적으로 간주됩니다.
- 필수 타입 매개변수는 선택적 타입 매개변수 뒤에 올 수 없습니다.
- 타입 매개변수의 기본 타입은 해당 타입 매개변수의 제약이 있는 경우 이를 충족해야 합니다.
- 타입 인자를 지정할 때 필수 타입 매개변수에 대한 타입 인자만 지정하면 됩니다. 지정되지 않은 타입 매개변수는 기본 타입으로 해결됩니다.
- 기본 타입이 지정되고 추론이 후보를 선택할 수 없는 경우, 기본 타입이 추론됩니다.
- 기존 클래스 또는 인터페이스 선언과 병합되는 클래스 또는 인터페이스 선언은 기존 타입 매개변수에 대한 기본값을 도입할 수 있습니다.
- 기존 클래스 또는 인터페이스 선언과 병합되는 클래스 또는 인터페이스 선언은 기본값을 지정하는 한 새로운 타입 매개변수를 도입할 수 있습니다.

## 변성 어노테이션

> 이것은 매우 특정한 문제를 해결하기 위한 고급 기능이며, 사용해야 할 이유를 식별한 상황에서만 사용해야 합니다

[공변성과 반공변성](https://en.wikipedia.org/wiki/Covariance_and_contravariance_(computer_science))은 두 제네릭 타입 간의 관계를 설명하는 타입 이론 용어입니다.
다음은 이 개념에 대한 간략한 입문서입니다.

예를 들어, 특정 타입을 `make`할 수 있는 객체를 나타내는 인터페이스가 있다면:
```ts
interface Producer<T> {
  make(): T;
}
```
`Cat`은 `Animal`이므로 `Producer<Animal>`이 예상되는 곳에서 `Producer<Cat>`을 사용할 수 있습니다.
이 관계를 *공변성*이라고 합니다: `Producer<T>`에서 `Producer<U>`로의 관계는 `T`에서 `U`로의 관계와 동일합니다.

반대로, 특정 타입을 `consume`할 수 있는 인터페이스가 있다면:
```ts
interface Consumer<T> {
  consume: (arg: T) => void;
}
```
`Animal`을 받아들일 수 있는 모든 함수는 `Cat`도 받아들일 수 있어야 하므로 `Consumer<Cat>`이 예상되는 곳에서 `Consumer<Animal>`을 사용할 수 있습니다.
이 관계를 *반공변성*이라고 합니다: `Consumer<T>`에서 `Consumer<U>`로의 관계는 `U`에서 `T`로의 관계와 동일합니다.
공변성과 비교하여 방향이 반대인 것에 주목하세요! 이것이 반공변성이 "스스로 상쇄"되지만 공변성은 그렇지 않은 이유입니다.

TypeScript와 같은 구조적 타입 시스템에서 공변성과 반공변성은 타입 정의에서 따르는 자연스럽게 나타나는 동작입니다.
제네릭이 없더라도 공변(및 반공변) 관계를 볼 수 있습니다:
```ts
interface AnimalProducer {
  make(): Animal;
}

// CatProducer는 Animal 생산자가 예상되는
// 모든 곳에서 사용할 수 있습니다
interface CatProducer {
  make(): Cat;
}
```

TypeScript는 구조적 타입 시스템을 가지므로, 두 타입을 비교할 때, 예를 들어 `Producer<Cat>`이 `Producer<Animal>`이 예상되는 곳에서 사용될 수 있는지 확인할 때, 일반적인 알고리즘은 두 정의를 구조적으로 확장하고 해당 구조를 비교합니다.
그러나 변성은 매우 유용한 최적화를 허용합니다: `Producer<T>`가 `T`에 대해 공변이면, `Producer<Cat>`과 `Producer<Animal>`이 동일한 관계를 가지므로 단순히 `Cat`과 `Animal`만 확인할 수 있습니다.

이 로직은 동일한 타입의 두 인스턴스화를 검사할 때만 사용할 수 있습니다.
`Producer<T>`와 `FastProducer<U>`가 있다면, `T`와 `U`가 반드시 이러한 타입에서 동일한 위치를 참조한다는 보장이 없으므로, 이 검사는 항상 구조적으로 수행됩니다.

변성은 구조적 타입의 자연스럽게 나타나는 속성이므로, TypeScript는 모든 제네릭 타입의 변성을 자동으로 *추론*합니다.
**매우 드문 경우**에 특정 종류의 순환 타입과 관련하여 이 측정이 부정확할 수 있습니다.
이런 경우, 타입 매개변수에 변성 어노테이션을 추가하여 특정 변성을 강제할 수 있습니다:
```ts
// 반공변 어노테이션
interface Consumer<in T> {
  consume: (arg: T) => void;
}

// 공변 어노테이션
interface Producer<out T> {
  make(): T;
}

// 불변 어노테이션
interface ProducerConsumer<in out T> {
  consume: (arg: T) => void;
  make(): T;
}
```
구조적으로 *발생해야 하는* 동일한 변성을 작성하는 경우에만 이렇게 하세요.

> 구조적 변성과 일치하지 않는 변성 어노테이션을 절대 작성하지 마세요!

변성 어노테이션은 인스턴스화 기반 비교 중에만 적용된다는 점을 강화하는 것이 중요합니다.
구조적 비교 중에는 효과가 없습니다.
예를 들어, 변성 어노테이션을 사용하여 타입을 실제로 불변으로 "강제"할 수 없습니다:
```ts
// 이렇게 하지 마세요 - 변성 어노테이션이
// 구조적 동작과 일치하지 않습니다
interface Producer<in out T> {
  make(): T;
}

// 타입 오류가 아닙니다 -- 이것은 구조적
// 비교이므로, 변성 어노테이션이
// 적용되지 않습니다
const p: Producer<string | number> = {
    make(): number {
        return 42;
    }
}
```
여기서 객체 리터럴의 `make` 함수는 `number`를 반환하는데, `number`가 `string | number`가 아니기 때문에 오류가 발생할 것으로 예상할 수 있습니다.
그러나 객체 리터럴이 `Producer<string | number>`가 아닌 익명 타입이기 때문에 이것은 인스턴스화 기반 비교가 아닙니다.

> 변성 어노테이션은 구조적 동작을 변경하지 않으며 특정 상황에서만 참조됩니다

왜 그렇게 하는지, 제한 사항이 무엇인지, 언제 적용되지 않는지 절대적으로 알고 있는 경우에만 변성 어노테이션을 작성하는 것이 매우 중요합니다.
TypeScript가 인스턴스화 기반 비교를 사용하는지 구조적 비교를 사용하는지는 지정된 동작이 아니며 정확성이나 성능상의 이유로 버전마다 변경될 수 있으므로, 타입의 구조적 동작과 일치하는 경우에만 변성 어노테이션을 작성해야 합니다.
특정 변성을 "강제"하기 위해 변성 어노테이션을 사용하지 마세요; 이는 코드에서 예측할 수 없는 동작을 유발합니다.

> 타입의 구조적 동작과 일치하지 않는 변성 어노테이션을 작성하지 마세요

TypeScript는 제네릭 타입에서 변성을 자동으로 추론할 수 있습니다.
변성 어노테이션을 작성해야 하는 경우는 거의 없으며, 특정 필요성을 식별한 경우에만 그렇게 해야 합니다.
변성 어노테이션은 타입의 구조적 동작을 변경하지 *않으며*, 상황에 따라 인스턴스화 기반 비교가 예상될 때 구조적 비교가 이루어지는 것을 볼 수 있습니다.
변성 어노테이션은 이러한 구조적 컨텍스트에서 타입이 동작하는 방식을 수정하는 데 사용할 수 없으며, 어노테이션이 구조적 정의와 동일하지 않는 한 작성해서는 안 됩니다.
이것을 올바르게 하기 어렵고, TypeScript가 대부분의 경우 변성을 올바르게 추론할 수 있으므로, 일반 코드에서 변성 어노테이션을 작성하는 경우는 드물어야 합니다.

> 타입 검사 동작을 변경하기 위해 변성 어노테이션을 사용하려고 하지 마세요; 이것은 그 용도가 아닙니다

변성 어노테이션이 확인되기 때문에 "타입 디버깅" 상황에서 임시 변성 어노테이션이 유용할 *수* 있습니다.
TypeScript는 어노테이션된 변성이 식별 가능하게 잘못된 경우 오류를 발생시킵니다:
```ts
// 오류, 이 인터페이스는 확실히 T에 대해 반공변입니다
interface Foo<out T> {
  consume: (arg: T) => void;
}
```
그러나 변성 어노테이션은 더 엄격할 수 있습니다(예: 실제 변성이 공변인 경우 `in out`이 유효합니다).
디버깅이 끝나면 변성 어노테이션을 제거하세요.

마지막으로, 타입 검사 성능을 최대화하려고 하고, 프로파일러를 실행했으며, 느린 특정 타입을 식별했고, 변성 추론이 특히 느리다는 것을 식별했으며, 작성하려는 변성 어노테이션을 주의 깊게 검증한 경우, 변성 어노테이션을 추가하여 매우 복잡한 타입에서 약간의 성능 이점을 볼 *수* 있습니다.

> 타입 검사 동작을 변경하기 위해 변성 어노테이션을 사용하려고 하지 마세요; 이것은 그 용도가 아닙니다

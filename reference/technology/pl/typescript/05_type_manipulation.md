# 타입 조작 (Type Manipulation)

> **원문:** https://www.typescriptlang.org/docs/handbook/2/types-from-types.html

---
title: 타입으로부터 타입 만들기
layout: docs
permalink: /docs/handbook/2/types-from-types.html
oneline: "기존 타입에서 새로운 타입을 만드는 다양한 방법 개요"
---

TypeScript의 타입 시스템은 _다른 타입을 기반으로_ 타입을 표현할 수 있기 때문에 매우 강력합니다.

이 아이디어의 가장 단순한 형태는 제네릭입니다. 그 외에도 다양한 _타입 연산자_를 사용할 수 있습니다.
또한 이미 있는 _값_을 기반으로 타입을 표현하는 것도 가능합니다.

다양한 타입 연산자를 결합하여 복잡한 연산과 값을 간결하고 유지보수가 용이한 방식으로 표현할 수 있습니다.
이 섹션에서는 기존 타입이나 값을 기반으로 새로운 타입을 표현하는 방법을 다룹니다.

- [제네릭](/docs/handbook/2/generics.html) - 매개변수를 받는 타입
- [Keyof 타입 연산자](/docs/handbook/2/keyof-types.html) - `keyof` 연산자를 사용하여 새로운 타입 만들기
- [Typeof 타입 연산자](/docs/handbook/2/typeof-types.html) - `typeof` 연산자를 사용하여 새로운 타입 만들기
- [인덱스 접근 타입](/docs/handbook/2/indexed-access-types.html) - `Type['a']` 문법을 사용하여 타입의 일부에 접근하기
- [조건부 타입](/docs/handbook/2/conditional-types.html) - 타입 시스템에서 if 문처럼 동작하는 타입
- [매핑된 타입](/docs/handbook/2/mapped-types.html) - 기존 타입의 각 속성을 매핑하여 타입 만들기
- [템플릿 리터럴 타입](/docs/handbook/2/template-literal-types.html) - 템플릿 리터럴 문자열을 통해 속성을 변경하는 매핑된 타입

---

> **원문:** https://www.typescriptlang.org/docs/handbook/2/generics.html

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

그렇게 하면 컴파일러가 `arg`의 `.length` 멤버를 사용하고 있지만, `arg`가 이 멤버가 있다고 어디에서도 말하지 않았다는 오류를 표시합니다.
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
`loggingIdentity` 예제에서 `arg`의 `.length` 속성에 접근하고 싶었지만, 컴파일러가 모든 타입이 `.length` 속성이 있다는 것을 증명할 수 없었으므로, 이러한 가정을 할 수 없다고 경고합니다.

```ts twoslash
// @errors: 2339
function loggingIdentity<Type>(arg: Type): Type {
  console.log(arg.length);
  return arg;
}
```

모든 타입에서 작동하는 대신, `.length` 속성이 *있는* 모든 타입에서 작동하도록 이 함수를 제한하고 싶습니다.
타입이 이 멤버가 있는 한 허용하지만, 최소한 이 멤버가 있어야 합니다.
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

[공변성과 반공변성](https://en.wikipedia.org/wiki/Covariance_and_contravariance_%28computer_science%29)은 두 제네릭 타입 간의 관계를 설명하는 타입 이론 용어입니다.
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

---

> **원문:** https://www.typescriptlang.org/docs/handbook/2/keyof-types.html

---
title: Keyof 타입 연산자
layout: docs
permalink: /docs/handbook/2/keyof-types.html
oneline: "타입 컨텍스트에서 keyof 연산자 사용하기"
---

## `keyof` 타입 연산자

`keyof` 연산자는 객체 타입을 받아 해당 키의 문자열 또는 숫자 리터럴 유니온을 생성합니다.
다음 타입 `P`는 `type P = "x" | "y"`와 동일한 타입입니다:

```ts twoslash
type Point = { x: number; y: number };
type P = keyof Point;
//   ^?
```

타입에 `string` 또는 `number` 인덱스 시그니처가 있으면, `keyof`는 해당 타입을 대신 반환합니다:

```ts twoslash
type Arrayish = { [n: number]: unknown };
type A = keyof Arrayish;
//   ^?

type Mapish = { [k: string]: boolean };
type M = keyof Mapish;
//   ^?
```

이 예제에서 `M`은 `string | number`입니다 -- 이는 JavaScript 객체 키가 항상 문자열로 강제 변환되기 때문에, `obj[0]`은 항상 `obj["0"]`과 동일합니다.

`keyof` 타입은 나중에 자세히 배울 매핑된 타입과 결합될 때 특히 유용해집니다.

---

> **원문:** https://www.typescriptlang.org/docs/handbook/2/typeof-types.html

---
title: Typeof 타입 연산자
layout: docs
permalink: /docs/handbook/2/typeof-types.html
oneline: "타입 컨텍스트에서 typeof 연산자 사용하기"
---

## `typeof` 타입 연산자

JavaScript에는 이미 _표현식_ 컨텍스트에서 사용할 수 있는 `typeof` 연산자가 있습니다:

```ts twoslash
// "string"을 출력합니다
console.log(typeof "Hello world");
```

TypeScript는 _타입_ 컨텍스트에서 변수나 속성의 _타입_을 참조하는 데 사용할 수 있는 `typeof` 연산자를 추가합니다:

```ts twoslash
let s = "hello";
let n: typeof s;
//  ^?
```

이것은 기본 타입에는 그다지 유용하지 않지만, 다른 타입 연산자와 결합하면 `typeof`를 사용하여 많은 패턴을 편리하게 표현할 수 있습니다.
예를 들어, 미리 정의된 타입 `ReturnType<T>`부터 살펴봅시다.
이것은 _함수 타입_을 받아 반환 타입을 생성합니다:

```ts twoslash
type Predicate = (x: unknown) => boolean;
type K = ReturnType<Predicate>;
//   ^?
```

함수 이름에 `ReturnType`을 사용하려고 하면 유익한 오류가 표시됩니다:

```ts twoslash
// @errors: 2749
function f() {
  return { x: 10, y: 3 };
}
type P = ReturnType<f>;
```

_값_과 _타입_은 같은 것이 아닙니다.
_값 `f`_가 가진 _타입_을 참조하려면 `typeof`를 사용합니다:

```ts twoslash
function f() {
  return { x: 10, y: 3 };
}
type P = ReturnType<typeof f>;
//   ^?
```

### 제한 사항

TypeScript는 `typeof`에 사용할 수 있는 표현식의 종류를 의도적으로 제한합니다.

구체적으로, 식별자(즉, 변수 이름)나 그 속성에서만 `typeof`를 사용하는 것이 합법적입니다.
이것은 실행된다고 생각하지만 실행되지 않는 코드를 작성하는 혼란스러운 함정을 피하는 데 도움이 됩니다:

```ts twoslash
// @errors: 1005
declare const msgbox: (prompt: string) => boolean;
// type msgbox = any;
// ---cut---
// = ReturnType<typeof msgbox>를 사용하려고 했습니다
let shouldContinue: typeof msgbox("Are you sure you want to continue?");
```

---

> **원문:** https://www.typescriptlang.org/docs/handbook/2/indexed-access-types.html

---
title: 인덱스 접근 타입
layout: docs
permalink: /docs/handbook/2/indexed-access-types.html
oneline: "Type['a'] 문법을 사용하여 타입의 일부에 접근하기"
---

_인덱스 접근 타입_을 사용하여 다른 타입의 특정 속성을 조회할 수 있습니다:

```ts twoslash
type Person = { age: number; name: string; alive: boolean };
type Age = Person["age"];
//   ^?
```

인덱싱 타입 자체가 타입이므로, 유니온, `keyof` 또는 다른 타입을 완전히 사용할 수 있습니다:

```ts twoslash
type Person = { age: number; name: string; alive: boolean };
// ---cut---
type I1 = Person["age" | "name"];
//   ^?

type I2 = Person[keyof Person];
//   ^?

type AliveOrName = "alive" | "name";
type I3 = Person[AliveOrName];
//   ^?
```

존재하지 않는 속성을 인덱싱하려고 하면 오류가 표시됩니다:

```ts twoslash
// @errors: 2339
type Person = { age: number; name: string; alive: boolean };
// ---cut---
type I1 = Person["alve"];
```

임의의 타입으로 인덱싱하는 또 다른 예는 `number`를 사용하여 배열 요소의 타입을 가져오는 것입니다.
이것을 `typeof`와 결합하여 배열 리터럴의 요소 타입을 편리하게 캡처할 수 있습니다:

```ts twoslash
const MyArray = [
  { name: "Alice", age: 15 },
  { name: "Bob", age: 23 },
  { name: "Eve", age: 38 },
];

type Person = typeof MyArray[number];
//   ^?
type Age = typeof MyArray[number]["age"];
//   ^?
// 또는
type Age2 = Person["age"];
//   ^?
```

인덱싱할 때는 타입만 사용할 수 있으므로, `const`를 사용하여 변수 참조를 만들 수 없습니다:

```ts twoslash
// @errors: 2538 2749
type Person = { age: number; name: string; alive: boolean };
// ---cut---
const key = "age";
type Age = Person[key];
```

그러나 유사한 스타일의 리팩토링을 위해 타입 별칭을 사용할 수 있습니다:

```ts twoslash
type Person = { age: number; name: string; alive: boolean };
// ---cut---
type key = "age";
type Age = Person[key];
```

---

> **원문:** https://www.typescriptlang.org/docs/handbook/2/conditional-types.html

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

이 예제에서 TypeScript는 `T`가 `message`라는 속성이 있다고 알려지지 않았기 때문에 오류를 발생시킵니다.
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

---

> **원문:** https://www.typescriptlang.org/docs/handbook/2/mapped-types.html

---
title: 매핑된 타입
layout: docs
permalink: /docs/handbook/2/mapped-types.html
oneline: "기존 타입을 재사용하여 타입 생성하기"
---

반복하고 싶지 않을 때, 때때로 타입은 다른 타입을 기반으로 해야 합니다.

매핑된 타입은 미리 선언되지 않은 속성의 타입을 선언하는 데 사용되는 인덱스 시그니처 문법을 기반으로 합니다:

```ts twoslash
type Horse = {};
// ---cut---
type OnlyBoolsAndHorses = {
  [key: string]: boolean | Horse;
};

const conforms: OnlyBoolsAndHorses = {
  del: true,
  rodney: false,
};
```

매핑된 타입은 `PropertyKey`의 유니온(자주 [`keyof`를 통해](/docs/handbook/2/indexed-access-types.html) 생성됨)을 사용하여 키를 반복하여 타입을 생성하는 제네릭 타입입니다:

```ts twoslash
type OptionsFlags<Type> = {
  [Property in keyof Type]: boolean;
};
```

이 예제에서 `OptionsFlags`는 `Type` 타입의 모든 속성을 가져와 값을 불리언으로 변경합니다.

```ts twoslash
type OptionsFlags<Type> = {
  [Property in keyof Type]: boolean;
};
// ---cut---
type Features = {
  darkMode: () => void;
  newUserProfile: () => void;
};

type FeatureOptions = OptionsFlags<Features>;
//   ^?
```

### 매핑 수정자

매핑 중에 적용할 수 있는 두 가지 추가 수정자가 있습니다: `readonly`와 `?`는 각각 가변성과 선택성에 영향을 미칩니다.

`-` 또는 `+`를 접두사로 붙여 이러한 수정자를 제거하거나 추가할 수 있습니다. 접두사를 추가하지 않으면 `+`로 가정됩니다.

```ts twoslash
// 타입의 속성에서 'readonly' 속성을 제거합니다
type CreateMutable<Type> = {
  -readonly [Property in keyof Type]: Type[Property];
};

type LockedAccount = {
  readonly id: string;
  readonly name: string;
};

type UnlockedAccount = CreateMutable<LockedAccount>;
//   ^?
```

```ts twoslash
// 타입의 속성에서 'optional' 속성을 제거합니다
type Concrete<Type> = {
  [Property in keyof Type]-?: Type[Property];
};

type MaybeUser = {
  id: string;
  name?: string;
  age?: number;
};

type User = Concrete<MaybeUser>;
//   ^?
```

## `as`를 통한 키 재매핑

TypeScript 4.1 이상에서는 매핑된 타입의 `as` 절을 사용하여 매핑된 타입의 키를 다시 매핑할 수 있습니다:

```ts
type MappedTypeWithNewProperties<Type> = {
    [Properties in keyof Type as NewKeyType]: Type[Properties]
}
```

[템플릿 리터럴 타입](/docs/handbook/2/template-literal-types.html)과 같은 기능을 활용하여 이전 속성 이름에서 새 속성 이름을 만들 수 있습니다:

```ts twoslash
type Getters<Type> = {
    [Property in keyof Type as `get${Capitalize<string & Property>}`]: () => Type[Property]
};

interface Person {
    name: string;
    age: number;
    location: string;
}

type LazyPerson = Getters<Person>;
//   ^?
```

조건부 타입을 통해 `never`를 생성하여 키를 필터링할 수 있습니다:

```ts twoslash
// 'kind' 속성을 제거합니다
type RemoveKindField<Type> = {
    [Property in keyof Type as Exclude<Property, "kind">]: Type[Property]
};

interface Circle {
    kind: "circle";
    radius: number;
}

type KindlessCircle = RemoveKindField<Circle>;
//   ^?
```

`string | number | symbol`의 유니온뿐만 아니라 모든 타입의 유니온에 대해 매핑할 수 있습니다:

```ts twoslash
type EventConfig<Events extends { kind: string }> = {
    [E in Events as E["kind"]]: (event: E) => void;
}

type SquareEvent = { kind: "square", x: number, y: number };
type CircleEvent = { kind: "circle", radius: number };

type Config = EventConfig<SquareEvent | CircleEvent>
//   ^?
```

### 추가 탐구

매핑된 타입은 이 타입 조작 섹션의 다른 기능과 잘 작동합니다. 예를 들어 여기에 [조건부 타입을 사용하는 매핑된 타입](/docs/handbook/2/conditional-types.html)이 있으며, 객체에 `pii` 속성이 리터럴 `true`로 설정되어 있는지 여부에 따라 `true` 또는 `false`를 반환합니다:

```ts twoslash
type ExtractPII<Type> = {
  [Property in keyof Type]: Type[Property] extends { pii: true } ? true : false;
};

type DBFields = {
  id: { format: "incrementing" };
  name: { type: string; pii: true };
};

type ObjectsNeedingGDPRDeletion = ExtractPII<DBFields>;
//   ^?
```

---

> **원문:** https://www.typescriptlang.org/docs/handbook/2/template-literal-types.html

---
title: 템플릿 리터럴 타입
layout: docs
permalink: /docs/handbook/2/template-literal-types.html
oneline: "템플릿 리터럴 문자열을 통해 속성을 변경하는 매핑된 타입 생성하기"
---

템플릿 리터럴 타입은 [문자열 리터럴 타입](/docs/handbook/2/everyday-types.html#literal-types)을 기반으로 하며, 유니온을 통해 많은 문자열로 확장할 수 있는 기능이 있습니다.

[JavaScript의 템플릿 리터럴 문자열](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Template_literals)과 동일한 문법을 사용하지만, 타입 위치에서 사용됩니다.
구체적인 리터럴 타입과 함께 사용될 때, 템플릿 리터럴은 내용을 연결하여 새로운 문자열 리터럴 타입을 생성합니다.

```ts twoslash
type World = "world";

type Greeting = `hello ${World}`;
//   ^?
```

유니온이 보간된 위치에서 사용되면, 타입은 각 유니온 멤버가 나타낼 수 있는 모든 가능한 문자열 리터럴의 집합입니다:

```ts twoslash
type EmailLocaleIDs = "welcome_email" | "email_heading";
type FooterLocaleIDs = "footer_title" | "footer_sendoff";

type AllLocaleIDs = `${EmailLocaleIDs | FooterLocaleIDs}_id`;
//   ^?
```

템플릿 리터럴의 각 보간된 위치에 대해, 유니온은 교차 곱셈됩니다:

```ts twoslash
type EmailLocaleIDs = "welcome_email" | "email_heading";
type FooterLocaleIDs = "footer_title" | "footer_sendoff";
// ---cut---
type AllLocaleIDs = `${EmailLocaleIDs | FooterLocaleIDs}_id`;
type Lang = "en" | "ja" | "pt";

type LocaleMessageIDs = `${Lang}_${AllLocaleIDs}`;
//   ^?
```

일반적으로 큰 문자열 유니온에는 사전 생성을 사용하는 것이 좋지만, 작은 경우에는 이것이 유용합니다.

### 타입에서의 문자열 유니온

템플릿 리터럴의 힘은 타입 내부의 정보를 기반으로 새 문자열을 정의할 때 나타납니다.

함수(`makeWatchedObject`)가 전달된 객체에 `on()`이라는 새 함수를 추가하는 경우를 고려해 봅시다. JavaScript에서 호출은 다음과 같을 수 있습니다:
`makeWatchedObject(baseObject)`. 기본 객체는 다음과 같이 보일 수 있습니다:

```ts twoslash
// @noErrors
const passedObject = {
  firstName: "Saoirse",
  lastName: "Ronan",
  age: 26,
};
```

기본 객체에 추가될 `on` 함수는 두 개의 인자를 예상합니다: `eventName`(`string`)과 `callback`(`function`).

`eventName`은 `attributeInThePassedObject + "Changed"` 형식이어야 합니다; 따라서 기본 객체의 `firstName` 속성에서 파생된 `firstNameChanged`입니다.

`callback` 함수가 호출될 때:
  * `attributeInThePassedObject` 이름과 연관된 타입의 값이 전달되어야 합니다; 따라서 `firstName`이 `string`으로 타입이 지정되었으므로, `firstNameChanged` 이벤트의 콜백은 호출 시 `string`이 전달될 것으로 예상합니다. 마찬가지로 `age`와 연관된 이벤트는 `number` 인자로 호출될 것으로 예상해야 합니다
  * `void` 반환 타입을 가져야 합니다(시연의 단순성을 위해)

`on()`의 순진한 함수 시그니처는 다음과 같을 수 있습니다: `on(eventName: string, callback: (newValue: any) => void)`. 그러나 앞의 설명에서 코드에 문서화하고 싶은 중요한 타입 제약을 식별했습니다. 템플릿 리터럴 타입을 사용하면 이러한 제약을 코드에 가져올 수 있습니다.

```ts twoslash
// @noErrors
declare function makeWatchedObject(obj: any): any;
// ---cut---
const person = makeWatchedObject({
  firstName: "Saoirse",
  lastName: "Ronan",
  age: 26,
});

// makeWatchedObject가 익명 Object에 `on`을 추가했습니다

person.on("firstNameChanged", (newValue) => {
  console.log(`firstName was changed to ${newValue}!`);
});
```

`on`이 `"firstName"`이 아닌 이벤트 `"firstNameChanged"`를 수신한다는 점에 주목하세요. `on()`의 순진한 사양은 관찰된 객체의 속성 이름 유니온에 끝에 "Changed"가 추가된 것으로 적합한 이벤트 이름 집합이 제한되도록 보장했다면 더 견고해질 수 있습니다. JavaScript에서 그러한 계산을 수행하는 것이 편하지만, 예를 들어 ``Object.keys(passedObject).map(x => `${x}Changed`)``처럼, _타입 시스템 내부의_ 템플릿 리터럴은 문자열 조작에 대한 유사한 접근 방식을 제공합니다:

```ts twoslash
type PropEventSource<Type> = {
    on(eventName: `${string & keyof Type}Changed`, callback: (newValue: any) => void): void;
};

/// 속성의 변경 사항을 관찰할 수 있도록
/// `on` 메서드가 있는 "관찰된 객체"를 생성합니다.
declare function makeWatchedObject<Type>(obj: Type): Type & PropEventSource<Type>;
```

이를 통해, 잘못된 속성이 주어지면 오류가 발생하는 것을 만들 수 있습니다:

```ts twoslash
// @errors: 2345
type PropEventSource<Type> = {
    on(eventName: `${string & keyof Type}Changed`, callback: (newValue: any) => void): void;
};

declare function makeWatchedObject<T>(obj: T): T & PropEventSource<T>;
// ---cut---
const person = makeWatchedObject({
  firstName: "Saoirse",
  lastName: "Ronan",
  age: 26
});

person.on("firstNameChanged", () => {});

// 쉬운 인적 오류 방지(이벤트 이름 대신 키 사용)
person.on("firstName", () => {});

// 오타 방지
person.on("frstNameChanged", () => {});
```

### 템플릿 리터럴을 사용한 추론

원래 전달된 객체에서 제공된 모든 정보를 활용하지 않았다는 점에 주목하세요. `firstName`의 변경(즉, `firstNameChanged` 이벤트)이 주어지면, 콜백이 `string` 타입의 인자를 받을 것으로 예상해야 합니다. 마찬가지로 `age`의 변경에 대한 콜백은 `number` 인자를 받아야 합니다. 우리는 `callback`의 인자 타입으로 `any`를 순진하게 사용하고 있습니다. 다시 말해, 템플릿 리터럴 타입을 사용하면 속성의 데이터 타입이 해당 속성의 콜백의 첫 번째 인자와 동일한 타입이 되도록 보장할 수 있습니다.

이것을 가능하게 하는 핵심 통찰력은 다음과 같습니다: 우리는 제네릭이 있는 함수를 사용하여:

1. 첫 번째 인자에서 사용된 리터럴이 리터럴 타입으로 캡처됩니다
2. 해당 리터럴 타입이 제네릭의 유효한 속성 유니온에 있는지 검증될 수 있습니다
3. 검증된 속성의 타입을 인덱스 접근을 사용하여 제네릭의 구조에서 조회할 수 있습니다
4. 이 타입 정보가 콜백 함수의 인자가 동일한 타입인지 확인하는 데 _적용될 수_ 있습니다

```ts twoslash
type PropEventSource<Type> = {
    on<Key extends string & keyof Type>
        (eventName: `${Key}Changed`, callback: (newValue: Type[Key]) => void): void;
};

declare function makeWatchedObject<Type>(obj: Type): Type & PropEventSource<Type>;

const person = makeWatchedObject({
  firstName: "Saoirse",
  lastName: "Ronan",
  age: 26
});

person.on("firstNameChanged", newName => {
    //                        ^?
    console.log(`new name is ${newName.toUpperCase()}`);
});

person.on("ageChanged", newAge => {
    //                  ^?
    if (newAge < 0) {
        console.warn("warning! negative age");
    }
})
```

여기서 `on`을 제네릭 메서드로 만들었습니다.

사용자가 문자열 `"firstNameChanged"`로 호출할 때, TypeScript는 `Key`에 대한 올바른 타입을 추론하려고 합니다.
그렇게 하기 위해, `"Changed"` 앞의 내용에 대해 `Key`를 매치하고 문자열 `"firstName"`을 추론합니다.
TypeScript가 이를 파악하면, `on` 메서드는 원래 객체에서 `firstName`의 타입을 가져올 수 있으며, 이 경우 `string`입니다.
마찬가지로, `"ageChanged"`로 호출되면, TypeScript는 `number`인 속성 `age`의 타입을 찾습니다.

추론은 다양한 방식으로 결합될 수 있으며, 종종 문자열을 분해하고 다른 방식으로 재구성합니다.

## 내장 문자열 조작 타입

문자열 조작을 돕기 위해, TypeScript에는 문자열 조작에 사용할 수 있는 타입 집합이 포함되어 있습니다. 이러한 타입은 성능을 위해 컴파일러에 내장되어 있으며 TypeScript에 포함된 `.d.ts` 파일에서 찾을 수 없습니다.

### `Uppercase<StringType>`

문자열의 각 문자를 대문자 버전으로 변환합니다.

##### 예제

```ts twoslash
type Greeting = "Hello, world"
type ShoutyGreeting = Uppercase<Greeting>
//   ^?

type ASCIICacheKey<Str extends string> = `ID-${Uppercase<Str>}`
type MainID = ASCIICacheKey<"my_app">
//   ^?
```

### `Lowercase<StringType>`

문자열의 각 문자를 소문자에 해당하는 것으로 변환합니다.

##### 예제

```ts twoslash
type Greeting = "Hello, world"
type QuietGreeting = Lowercase<Greeting>
//   ^?

type ASCIICacheKey<Str extends string> = `id-${Lowercase<Str>}`
type MainID = ASCIICacheKey<"MY_APP">
//   ^?
```

### `Capitalize<StringType>`

문자열의 첫 번째 문자를 대문자에 해당하는 것으로 변환합니다.

##### 예제

```ts twoslash
type LowercaseGreeting = "hello, world";
type Greeting = Capitalize<LowercaseGreeting>;
//   ^?
```

### `Uncapitalize<StringType>`

문자열의 첫 번째 문자를 소문자에 해당하는 것으로 변환합니다.

##### 예제

```ts twoslash
type UppercaseGreeting = "HELLO WORLD";
type UncomfortableGreeting = Uncapitalize<UppercaseGreeting>;
//   ^?
```

<details>
    <summary>내장 문자열 조작 타입에 대한 기술적 세부 사항</summary>
    <p>TypeScript 4.1 기준으로, 이러한 내장 함수의 코드는 조작을 위해 JavaScript 문자열 런타임 함수를 직접 사용하며 로케일을 인식하지 않습니다.</p>
    <code><pre>
function applyStringMapping(symbol: Symbol, str: string) {
    switch (intrinsicTypeKinds.get(symbol.escapedName as string)) {
        case IntrinsicTypeKind.Uppercase: return str.toUpperCase();
        case IntrinsicTypeKind.Lowercase: return str.toLowerCase();
        case IntrinsicTypeKind.Capitalize: return str.charAt(0).toUpperCase() + str.slice(1);
        case IntrinsicTypeKind.Uncapitalize: return str.charAt(0).toLowerCase() + str.slice(1);
    }
    return str;
}</pre></code>
</details>

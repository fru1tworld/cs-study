# TypeScript 타입 조작과 모듈

## 타입 조작 (Type Manipulation)

> **원문:** https://www.typescriptlang.org/docs/handbook/2/types-from-types.html

---
title: 타입으로부터 타입 만들기
layout: docs
permalink: /docs/handbook/2/types-from-types.html
oneline: "기존 타입에서 새로운 타입을 만드는 다양한 방법 개요"
---

- TypeScript의 타입 시스템은 _다른 타입을 기반으로_ 타입을 표현 가능 → 매우 강력
- 가장 단순한 형태 = 제네릭
- 그 외에도 다양한 _타입 연산자_ 사용 가능 · 이미 있는 _값_을 기반으로 타입 표현도 가능
- 다양한 타입 연산자를 결합 → 복잡한 연산과 값을 간결하고 유지보수 용이한 방식으로 표현 가능
- 이 섹션은 기존 타입이나 값을 기반으로 새로운 타입을 표현하는 방법을 다룸

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

- 소프트웨어 엔지니어링의 주요 부분 = 잘 정의되고 일관된 API를 가지면서도 재사용 가능한 컴포넌트를 만드는 것
- 오늘의 데이터뿐 아니라 내일의 데이터에서도 작동 가능한 컴포넌트 → 대규모 소프트웨어 시스템 구축에 가장 유연
- C#·Java 같은 언어에서 재사용 가능한 컴포넌트를 만드는 주요 도구 중 하나 = _제네릭_ → 단일 타입이 아닌 다양한 타입에서 작동 가능한 컴포넌트를 만들 수 있음
- 이를 통해 사용자는 이러한 컴포넌트를 사용하고 자신만의 타입 사용 가능

### 제네릭의 Hello World

- 제네릭의 "hello world" = 항등 함수(전달된 것을 그대로 반환하는 함수). `echo` 명령어와 유사
- 제네릭 없이는 항등 함수에 특정 타입 지정 필요

```ts twoslash
function identity(arg: number): number {
  return arg;
}
```

- 또는 `any` 타입을 사용해도 항등 함수를 설명 가능

```ts twoslash
function identity(arg: any): any {
  return arg;
}
```

- `any` 사용 시 `arg`가 모든 타입을 받아들여 확실히 제네릭 → 대신 반환 시 원래 타입 정보 소실
  - 숫자를 전달해도 "어떤 타입이든 반환될 수 있다"는 정보만 남음
- 필요한 것: 반환값을 나타내는 데도 쓸 수 있는 방식으로 인자의 타입을 캡처하는 방법
  - → 값이 아닌 타입에 작동하는 특별한 변수인 _타입 변수_ 사용

```ts twoslash
function identity<Type>(arg: Type): Type {
  return arg;
}
```

- 항등 함수에 타입 변수 `Type` 추가
  - `Type`은 사용자가 제공하는 타입(예: `number`)을 캡처 → 나중에 해당 정보 재사용 가능
  - `Type`을 반환 타입으로도 재사용 → 인자와 반환 타입에 동일한 타입 사용 → 함수 한쪽에서 다른 쪽으로 타입 정보 전달 가능
- 이 버전의 `identity`는 다양한 타입에서 작동 → 제네릭하다고 표현
  - `any`와 달리 정보 손실 없음: 숫자를 쓴 첫 번째 `identity` 함수만큼 정확
- 제네릭 항등 함수 호출 방식 두 가지
  - 방법 1: 타입 인자를 포함한 모든 인자를 함수에 전달

```ts twoslash
function identity<Type>(arg: Type): Type {
  return arg;
}
// ---cut---
let output = identity<string>("myString");
//       ^?
```

- `()`가 아닌 `<>` 안에서 인자로 `Type`을 명시적으로 `string`으로 설정
- 방법 2(가장 일반적): _타입 인자 추론_ 사용 → 전달하는 인자의 타입에 따라 컴파일러가 자동으로 `Type` 값 설정

```ts twoslash
function identity<Type>(arg: Type): Type {
  return arg;
}
// ---cut---
let output = identity("myString");
//       ^?
```

- 꺾쇠 괄호(`<>`) 안에 타입을 명시적으로 전달할 필요 없음 → 컴파일러가 `"myString"` 값을 보고 `Type`을 해당 타입으로 설정
- 타입 인자 추론은 코드를 짧고 읽기 쉽게 유지하는 데 유용 → 다만 컴파일러가 타입을 추론하지 못하는 복잡한 예에서는 타입 인자를 명시적으로 전달해야 할 수 있음

### 제네릭 타입 변수 작업

- 제네릭 함수(`identity` 등)를 만들면, 컴파일러가 함수 본문에서 제네릭으로 타입이 지정된 매개변수를 올바르게 사용하도록 강제함
  - 즉 해당 매개변수를 모든 타입이 될 수 있는 것처럼 실제로 취급해야 함
- 앞서의 `identity` 함수

```ts twoslash
function identity<Type>(arg: Type): Type {
  return arg;
}
```

- 각 호출마다 인자 `arg`의 길이를 콘솔에 기록하고 싶다면 다음과 같이 작성하고 싶을 수 있음

```ts twoslash
// @errors: 2339
function loggingIdentity<Type>(arg: Type): Type {
  console.log(arg.length);
  return arg;
}
```

- 컴파일러는 `arg`의 `.length` 멤버를 사용하지만 `arg`에 이 멤버가 있다고 어디서도 명시하지 않았다는 오류 표시
  - 이유: 타입 변수는 모든 타입을 대신함 → 사용자가 `.length` 멤버가 없는 `number`를 전달했을 수도 있음
- 이 함수가 `Type` 자체가 아니라 `Type`의 배열에서 작동하도록 의도했다면 → 배열이므로 `.length` 사용 가능 → 다른 타입의 배열처럼 설명 가능

```ts twoslash {1}
function loggingIdentity<Type>(arg: Type[]): Type[] {
  console.log(arg.length);
  return arg;
}
```

- `loggingIdentity`의 타입 독해: "제네릭 함수 `loggingIdentity`는 타입 매개변수 `Type`과 `Type` 배열인 인자 `arg`를 받아 `Type` 배열을 반환"
  - 숫자 배열 전달 시 `Type`이 `number`에 바인딩 → 숫자 배열 반환
  - 전체 타입이 아닌 타입의 일부로 `Type` 사용 가능 → 유연성 확대
- 동일한 예제를 다음과 같이 작성 가능

```ts twoslash {1}
function loggingIdentity<Type>(arg: Array<Type>): Array<Type> {
  console.log(arg.length); // 배열은 .length를 가지므로 더 이상 오류가 없습니다
  return arg;
}
```

- 다른 언어에서 이러한 스타일의 타입을 이미 접했을 수 있음
- 다음 섹션은 `Array<Type>`과 같은 자신만의 제네릭 타입을 만드는 방법을 다룸

### 제네릭 타입

- 이전 섹션 = 다양한 타입에서 작동하는 제네릭 항등 함수
- 이 섹션 = 함수 자체의 타입과 제네릭 인터페이스를 만드는 방법
- 제네릭 함수의 타입은 비제네릭 함수와 마찬가지로 함수 선언과 유사하게 타입 매개변수가 먼저 나열됨

```ts twoslash
function identity<Type>(arg: Type): Type {
  return arg;
}

let myIdentity: <Type>(arg: Type) => Type = identity;
```

- 타입 변수의 수와 사용 방식이 일치하는 한, 타입에서 제네릭 타입 매개변수에 다른 이름 사용 가능

```ts twoslash
function identity<Type>(arg: Type): Type {
  return arg;
}

let myIdentity: <Input>(arg: Input) => Input = identity;
```

- 객체 리터럴 타입의 호출 시그니처로 제네릭 타입 작성도 가능

```ts twoslash
function identity<Type>(arg: Type): Type {
  return arg;
}

let myIdentity: { <Type>(arg: Type): Type } = identity;
```

- 첫 번째 제네릭 인터페이스: 이전 예제의 객체 리터럴을 인터페이스로 이동

```ts twoslash
interface GenericIdentityFn {
  <Type>(arg: Type): Type;
}

function identity<Type>(arg: Type): Type {
  return arg;
}

let myIdentity: GenericIdentityFn = identity;
```

- 제네릭 매개변수를 전체 인터페이스의 매개변수로 이동하는 경우도 있음
  - 어떤 타입에 대해 제네릭인지 확인 가능(예: `Dictionary` 대신 `Dictionary<string>`)
  - 인터페이스의 다른 모든 멤버에 타입 매개변수가 노출됨

```ts twoslash
interface GenericIdentityFn<Type> {
  (arg: Type): Type;
}

function identity<Type>(arg: Type): Type {
  return arg;
}

let myIdentity: GenericIdentityFn<number> = identity;
```

- 예제가 약간 달라짐: 제네릭 함수가 아니라 제네릭 타입의 일부인 비제네릭 함수 시그니처
- `GenericIdentityFn` 사용 시 타입 인자(여기서는 `number`)도 지정해야 하며, 기본 호출 시그니처가 사용할 타입이 고정됨
- 타입 매개변수를 호출 시그니처에 직접 넣을 때와 인터페이스 자체에 넣을 때의 차이 이해 → 타입의 어떤 측면이 제네릭인지 설명하는 데 도움
- 제네릭 인터페이스 외에 제네릭 클래스도 가능. 제네릭 열거형·네임스페이스는 불가

### 제네릭 클래스

- 제네릭 클래스는 제네릭 인터페이스와 유사한 형태
- 클래스 이름 뒤 꺾쇠 괄호(`<>`) 안에 제네릭 타입 매개변수 목록을 가짐

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

- 위는 `GenericNumber` 클래스의 직접적인 사용 예 → `number` 타입만 쓰도록 강제하는 요소는 없음
  - 대신 `string`이나 더 복잡한 객체 사용도 가능

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

- 인터페이스와 마찬가지로, 클래스 자체에 타입 매개변수를 넣으면 클래스의 모든 속성이 동일한 타입으로 작동하도록 보장 가능
- [클래스 섹션](/docs/handbook/2/classes.html)에서 다루듯 클래스 타입에는 정적 측면·인스턴스 측면 두 가지가 있음
  - 제네릭 클래스는 인스턴스 측면에서만 제네릭 → 정적 멤버는 클래스의 타입 매개변수 사용 불가

### 제네릭 제약 조건

- 때때로 어떤 기능을 가질지 _어느 정도_ 알고 있는 타입 집합에서 작동하는 제네릭 함수를 작성하고 싶을 수 있음
- `loggingIdentity` 예제: `arg`의 `.length`에 접근하려 했으나, 컴파일러는 모든 타입이 `.length`를 가진다고 증명할 수 없어 경고

```ts twoslash
// @errors: 2339
function loggingIdentity<Type>(arg: Type): Type {
  console.log(arg.length);
  return arg;
}
```

- 모든 타입 대신 `.length` 속성이 *있는* 모든 타입으로 이 함수를 제한하고 싶음
  - 이 멤버가 있는 한 허용하되 최소한 있어야 함 → `Type`이 될 수 있는 것에 대한 제약으로 요구 사항 명시 필요
- 제약을 설명하는 인터페이스 생성: 단일 `.length` 속성을 가진 인터페이스 + `extends` 키워드로 제약 표현

```ts twoslash
interface Lengthwise {
  length: number;
}

function loggingIdentity<Type extends Lengthwise>(arg: Type): Type {
  console.log(arg.length); // 이제 .length 속성이 있다는 것을 알므로 더 이상 오류가 없습니다
  return arg;
}
```

- 이제 제약된 제네릭 함수는 더 이상 모든 타입에서 작동하지 않음

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

- 대신 필요한 모든 속성을 가진 타입의 값을 전달해야 함

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

### 제네릭 제약 조건에서 타입 매개변수 사용

- 다른 타입 매개변수에 의해 제약되는 타입 매개변수 선언 가능
- 예: 객체의 이름이 주어지면 해당 속성을 가져오는 경우 → `obj`에 없는 속성을 실수로 가져오지 않도록 두 타입 사이에 제약을 둠

```ts twoslash
// @errors: 2345
function getProperty<Type, Key extends keyof Type>(obj: Type, key: Key) {
  return obj[key];
}

let x = { a: 1, b: 2, c: 3, d: 4 };

getProperty(x, "a");
getProperty(x, "m");
```

### 제네릭에서 클래스 타입 사용

- TypeScript에서 제네릭으로 팩토리를 만들 때는 생성자 함수로 클래스 타입을 참조해야 함

```ts twoslash
function create<Type>(c: { new (): Type }): Type {
  return new c();
}
```

- 더 고급 예제: prototype 속성으로 생성자 함수와 클래스 타입의 인스턴스 측면 사이의 관계를 추론·제약

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

- 이 패턴은 [믹스인](/docs/handbook/mixins.html) 디자인 패턴에 사용됨

### 제네릭 매개변수 기본값

- 제네릭 타입 매개변수에 기본값을 선언하면 해당 타입 인자 지정이 선택 사항이 됨
- 예: 새 `HTMLElement`를 만드는 함수
  - 인자 없이 호출 → `HTMLDivElement` 생성
  - 첫 번째 인자로 요소 전달 → 해당 인자 타입의 요소 생성
  - 선택적으로 자식 목록도 전달 가능
  - 이전에는 다음과 같이 정의해야 했음

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

- 제네릭 매개변수 기본값을 쓰면 다음과 같이 줄일 수 있음

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

제네릭 매개변수 기본값 규칙:

- 타입 매개변수는 기본값이 있으면 선택적으로 간주됨
- 필수 타입 매개변수는 선택적 타입 매개변수 뒤에 올 수 없음
- 타입 매개변수의 기본 타입은 해당 타입 매개변수의 제약이 있으면 이를 충족해야 함
- 타입 인자 지정 시 필수 타입 매개변수에 대한 타입 인자만 지정하면 됨 → 미지정 타입 매개변수는 기본 타입으로 해결
- 기본 타입이 지정되고 추론이 후보를 선택할 수 없으면 기본 타입이 추론됨
- 기존 클래스·인터페이스 선언과 병합되는 클래스·인터페이스 선언은 기존 타입 매개변수에 대한 기본값 도입 가능
- 기존 클래스·인터페이스 선언과 병합되는 클래스·인터페이스 선언은 기본값을 지정하는 한 새 타입 매개변수 도입 가능

### 변성 어노테이션

> 이것은 매우 특정한 문제를 해결하기 위한 고급 기능이며, 사용해야 할 이유를 식별한 상황에서만 사용해야 합니다

- [공변성과 반공변성](https://en.wikipedia.org/wiki/Covariance_and_contravariance_%28computer_science%29) = 두 제네릭 타입 간의 관계를 설명하는 타입 이론 용어
- 예: 특정 타입을 `make`할 수 있는 객체를 나타내는 인터페이스
```ts
interface Producer<T> {
  make(): T;
}
```
- `Cat`은 `Animal`이므로 `Producer<Animal>`이 예상되는 곳에서 `Producer<Cat>` 사용 가능
  - 이 관계 = *공변성*: `Producer<T>`에서 `Producer<U>`로의 관계는 `T`에서 `U`로의 관계와 동일
- 반대로 특정 타입을 `consume`할 수 있는 인터페이스
```ts
interface Consumer<T> {
  consume: (arg: T) => void;
}
```
- `Animal`을 받는 모든 함수는 `Cat`도 받아야 하므로 `Consumer<Cat>`이 예상되는 곳에서 `Consumer<Animal>` 사용 가능
  - 이 관계 = *반공변성*: `Consumer<T>`에서 `Consumer<U>`로의 관계는 `U`에서 `T`로의 관계와 동일
  - 공변성과 방향이 반대 → 반공변성은 "스스로 상쇄"되지만 공변성은 그렇지 않은 이유
- TypeScript 같은 구조적 타입 시스템에서 공변성·반공변성은 타입 정의에서 자연스럽게 나타나는 동작
  - 제네릭이 없어도 공변(및 반공변) 관계를 볼 수 있음
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

- TypeScript는 구조적 타입 시스템이므로, 두 타입 비교 시(예: `Producer<Cat>`이 `Producer<Animal>` 예상 위치에서 사용 가능한지) 일반 알고리즘은 두 정의를 구조적으로 확장해 구조를 비교
  - 변성은 유용한 최적화 제공: `Producer<T>`가 `T`에 대해 공변이면 `Producer<Cat>`과 `Producer<Animal>`이 동일한 관계 → 단순히 `Cat`과 `Animal`만 확인 가능
- 이 로직은 동일한 타입의 두 인스턴스화를 검사할 때만 사용 가능
  - `Producer<T>`와 `FastProducer<U>`처럼 다른 타입이면 `T`·`U`가 동일 위치를 참조한다는 보장이 없어 항상 구조적으로 검사
- 변성은 구조적 타입의 자연스러운 속성 → TypeScript는 모든 제네릭 타입의 변성을 자동으로 *추론*
  - **매우 드문 경우**에 순환 타입 관련해 이 추론이 부정확할 수 있음 → 타입 매개변수에 변성 어노테이션을 추가해 특정 변성 강제 가능
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
- 구조적으로 *발생해야 하는* 동일한 변성을 작성하는 경우에만 사용

> 구조적 변성과 일치하지 않는 변성 어노테이션을 절대 작성하지 마세요!

- 변성 어노테이션은 인스턴스화 기반 비교 중에만 적용 → 구조적 비교 중에는 효과 없음
  - 예: 변성 어노테이션으로 타입을 실제로 불변으로 "강제"할 수 없음
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
- 위 예에서 객체 리터럴의 `make`는 `number`를 반환 → `number`가 `string | number`가 아니므로 오류가 예상되지만, 객체 리터럴이 `Producer<string | number>`가 아닌 익명 타입이라 인스턴스화 기반 비교가 아님

> 변성 어노테이션은 구조적 동작을 변경하지 않으며 특정 상황에서만 참조됩니다

- 왜 그렇게 하는지, 제한 사항, 언제 적용되지 않는지를 확실히 아는 경우에만 변성 어노테이션 작성
  - TypeScript가 인스턴스화 기반/구조적 비교 중 무엇을 쓰는지는 지정된 동작이 아니며 버전마다 바뀔 수 있음 → 타입의 구조적 동작과 일치하는 경우에만 작성
  - 특정 변성을 "강제"하려는 용도로 사용 금지 → 예측 불가능한 동작 유발

> 타입의 구조적 동작과 일치하지 않는 변성 어노테이션을 작성하지 마세요

- TypeScript는 제네릭 타입의 변성을 자동으로 추론 가능 → 변성 어노테이션을 직접 작성할 필요는 거의 없음, 특정 필요성이 확인된 경우만 예외
  - 변성 어노테이션은 타입의 구조적 동작을 변경하지 *않음* → 인스턴스화 기반 비교가 예상되는 상황에서 구조적 비교가 이뤄지는 경우도 있음
  - 구조적 컨텍스트에서 타입 동작을 수정하는 용도로 쓸 수 없음 → 어노테이션이 구조적 정의와 동일하지 않으면 작성 금지
  - 올바르게 맞추기 어렵고 TypeScript가 대부분 변성을 올바르게 추론하므로, 일반 코드에서 변성 어노테이션 작성은 드물어야 함

> 타입 검사 동작을 변경하기 위해 변성 어노테이션을 사용하려고 하지 마세요; 이것은 그 용도가 아닙니다

- "타입 디버깅" 상황에서는 임시 변성 어노테이션이 유용할 *수* 있음(어노테이션이 검증되므로)
  - TypeScript는 어노테이션된 변성이 식별 가능하게 잘못되면 오류 발생
```ts
// 오류, 이 인터페이스는 확실히 T에 대해 반공변입니다
interface Foo<out T> {
  consume: (arg: T) => void;
}
```
- 다만 변성 어노테이션은 더 엄격할 수 있음(예: 실제 변성이 공변이면 `in out`도 유효). 디버깅이 끝나면 제거
- 마지막으로, 타입 검사 성능 최적화가 목적이고 프로파일러로 느린 특정 타입을 식별했으며 변성 추론이 특히 느리다는 것까지 확인하고 어노테이션을 주의 깊게 검증했다면, 변성 어노테이션 추가로 매우 복잡한 타입에서 약간의 성능 이점을 볼 *수* 있음

> 타입 검사 동작을 변경하기 위해 변성 어노테이션을 사용하려고 하지 마세요; 이것은 그 용도가 아닙니다

---

> **원문:** https://www.typescriptlang.org/docs/handbook/2/keyof-types.html

---
title: Keyof 타입 연산자
layout: docs
permalink: /docs/handbook/2/keyof-types.html
oneline: "타입 컨텍스트에서 keyof 연산자 사용하기"
---

### `keyof` 타입 연산자

- `keyof` 연산자는 객체 타입을 받아 해당 키의 문자열·숫자 리터럴 유니온을 생성
- 다음 타입 `P`는 `type P = "x" | "y"`와 동일한 타입

```ts twoslash
type Point = { x: number; y: number };
type P = keyof Point;
//   ^?
```

- 타입에 `string`·`number` 인덱스 시그니처가 있으면 `keyof`는 해당 타입을 대신 반환

```ts twoslash
type Arrayish = { [n: number]: unknown };
type A = keyof Arrayish;
//   ^?

type Mapish = { [k: string]: boolean };
type M = keyof Mapish;
//   ^?
```

- 이 예제에서 `M`은 `string | number` → JavaScript 객체 키는 항상 문자열로 강제 변환되므로 `obj[0]`은 `obj["0"]`과 동일
- `keyof` 타입은 매핑된 타입과 결합될 때 특히 유용(매핑된 타입은 나중에 자세히 다룸)

---

> **원문:** https://www.typescriptlang.org/docs/handbook/2/typeof-types.html

---
title: Typeof 타입 연산자
layout: docs
permalink: /docs/handbook/2/typeof-types.html
oneline: "타입 컨텍스트에서 typeof 연산자 사용하기"
---

### `typeof` 타입 연산자

- JavaScript에는 이미 _표현식_ 컨텍스트에서 쓸 수 있는 `typeof` 연산자가 있음

```ts twoslash
// "string"을 출력합니다
console.log(typeof "Hello world");
```

- TypeScript는 _타입_ 컨텍스트에서 변수·속성의 _타입_을 참조하는 `typeof` 연산자를 추가

```ts twoslash
let s = "hello";
let n: typeof s;
//  ^?
```

- 기본 타입에는 그다지 유용하지 않지만, 다른 타입 연산자와 결합하면 `typeof`로 많은 패턴을 편리하게 표현 가능
- 예: 미리 정의된 타입 `ReturnType<T>` → _함수 타입_을 받아 반환 타입을 생성

```ts twoslash
type Predicate = (x: unknown) => boolean;
type K = ReturnType<Predicate>;
//   ^?
```

- 함수 이름에 `ReturnType`을 사용하려 하면 유익한 오류가 표시됨

```ts twoslash
// @errors: 2749
function f() {
  return { x: 10, y: 3 };
}
type P = ReturnType<f>;
```

- _값_과 _타입_은 같은 것이 아님 → _값 `f`_가 가진 _타입_을 참조하려면 `typeof` 사용

```ts twoslash
function f() {
  return { x: 10, y: 3 };
}
type P = ReturnType<typeof f>;
//   ^?
```

#### 제한 사항

- TypeScript는 `typeof`에 쓸 수 있는 표현식 종류를 의도적으로 제한
- 구체적으로 식별자(변수 이름)나 그 속성에서만 `typeof` 사용이 합법 → 실행된다고 생각하지만 실행되지 않는 코드를 작성하는 혼란스러운 함정을 방지

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

- _인덱스 접근 타입_으로 다른 타입의 특정 속성 조회 가능

```ts twoslash
type Person = { age: number; name: string; alive: boolean };
type Age = Person["age"];
//   ^?
```

- 인덱싱 타입 자체가 타입 → 유니온, `keyof` 등 다른 타입을 그대로 사용 가능

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

- 존재하지 않는 속성을 인덱싱하려 하면 오류 표시

```ts twoslash
// @errors: 2339
type Person = { age: number; name: string; alive: boolean };
// ---cut---
type I1 = Person["alve"];
```

- 임의의 타입으로 인덱싱하는 또 다른 예: `number`로 배열 요소의 타입 가져오기
  - `typeof`와 결합하면 배열 리터럴의 요소 타입을 편리하게 캡처 가능

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

- 인덱싱 시 타입만 사용 가능 → `const`로 변수 참조를 만들 수는 없음

```ts twoslash
// @errors: 2538 2749
type Person = { age: number; name: string; alive: boolean };
// ---cut---
const key = "age";
type Age = Person[key];
```

- 다만 유사한 스타일의 리팩토링에는 타입 별칭 사용 가능

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

- 대부분의 유용한 프로그램은 핵심적으로 입력에 따라 결정을 내려야 함
- JavaScript 프로그램도 마찬가지 → 값을 쉽게 검사할 수 있으므로 결정이 입력의 타입에도 기반
- _조건부 타입_은 입력과 출력 타입 간의 관계를 설명하는 데 도움

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

- 조건부 타입은 JavaScript의 조건 표현식(`condition ? trueExpression : falseExpression`)과 비슷한 형태

```ts twoslash
type SomeType = any;
type OtherType = any;
type TrueType = any;
type FalseType = any;
type Stuff =
  // ---cut---
  SomeType extends OtherType ? TrueType : FalseType;
```

- `extends` 왼쪽 타입이 오른쪽 타입에 할당 가능 → 첫 번째 분기("true" 분기) 타입, 아니면 두 번째 분기("false" 분기) 타입
- 위 예제만 보면 조건부 타입이 즉시 유용해 보이지 않을 수 있음(`Dog extends Animal` 여부는 직접 판단해서 `number`/`string`을 고를 수 있으므로) → 하지만 조건부 타입의 힘은 제네릭과 함께 쓸 때 나옴
- 예: `createLabel` 함수

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

- `createLabel`의 오버로드들은 입력 타입에 따라 선택하는 단일 JavaScript 함수를 설명 → 주목할 점
  - 라이브러리가 API 전체에서 동일한 종류의 선택을 반복해야 한다면 번거로워짐
  - 세 개의 오버로드가 필요: 타입을 _확신할_ 수 있는 각 경우(`string`용, `number`용)와 가장 일반적인 경우(`string | number`) → `createLabel`이 처리할 수 있는 새 타입이 늘어날수록 오버로드 수가 기하급수적으로 증가
- 대신 조건부 타입으로 해당 로직을 인코딩 가능

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

- 이 조건부 타입을 이용해 오버로드를 오버로드 없는 단일 함수로 단순화 가능

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

#### 조건부 타입 제약

- 조건부 타입의 검사는 종종 새로운 정보를 제공
  - 타입 가드로 좁히면 더 구체적인 타입을 얻는 것처럼, 조건부 타입의 true 분기는 검사하는 타입으로 제네릭을 추가 제약
- 예

```ts twoslash
// @errors: 2536
type MessageOf<T> = T["message"];
```

- 이 예제는 `T`가 `message` 속성을 갖는다고 알려지지 않았으므로 TypeScript가 오류를 발생시킴
  - `T`를 제약하면 더 이상 오류 없음

```ts twoslash
type MessageOf<T extends { message: unknown }> = T["message"];

interface Email {
  message: string;
}

type EmailMessageContents = MessageOf<Email>;
//   ^?
```

- `MessageOf`가 모든 타입을 받으면서 `message`가 없을 때 `never`로 기본 설정되게 하려면? → 제약을 밖으로 빼고 조건부 타입을 도입하면 가능

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

- true 분기 내에서 TypeScript는 `T`가 `message` 속성을 _가질 것_임을 인지
- 또 다른 예: 배열 타입을 요소 타입으로 평탄화하되 그 외엔 그대로 두는 `Flatten` 타입

```ts twoslash
type Flatten<T> = T extends any[] ? T[number] : T;

// 요소 타입을 추출합니다.
type Str = Flatten<string[]>;
//   ^?

// 타입을 그대로 둡니다.
type Num = Flatten<number>;
//   ^?
```

- `Flatten`에 배열 타입이 주어지면 `number` 인덱스 접근으로 `string[]`의 요소 타입을 가져옴 → 아니면 주어진 타입을 그대로 반환

#### 조건부 타입 내에서 추론

- 방금 조건부 타입으로 제약을 적용하고 타입을 추출하는 방법을 살펴봄 → 매우 일반적인 작업이라 조건부 타입이 이를 더 쉽게 해줌
- 조건부 타입은 `infer` 키워드로 true 분기에서 비교 대상 타입을 추론하는 방법을 제공
- 예: 인덱스 접근 타입으로 "수동" 추출 대신 `Flatten`에서 요소 타입을 추론

```ts twoslash
type Flatten<Type> = Type extends Array<infer Item> ? Item : Type;
```

- true 분기 내에서 `Type`의 요소 타입을 검색하는 방법을 직접 지정하는 대신, `infer`로 `Item`이라는 새 제네릭 타입 변수를 선언적으로 도입
  - 관심 있는 타입의 구조를 파고들어 탐색하는 방법을 고민할 필요가 없어짐
- `infer` 키워드로 유용한 헬퍼 타입 별칭 작성 가능
- 예: 간단한 경우 함수 타입에서 반환 타입 추출

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

- 여러 호출 시그니처가 있는 타입(오버로드된 함수 타입 등)에서 추론할 때 추론은 _마지막_ 시그니처(대개 가장 허용적인 케이스)에서 이루어짐 → 인자 타입 목록 기반 오버로드 해결은 불가

```ts twoslash
declare function stringOrNum(x: string): number;
declare function stringOrNum(x: number): string;
declare function stringOrNum(x: string | number): string | number;

type T1 = ReturnType<typeof stringOrNum>;
//   ^?
```

### 분배적 조건부 타입

- 조건부 타입이 제네릭 타입에 작용할 때 유니온 타입이 주어지면 _분배적_이 됨

```ts twoslash
type ToArray<Type> = Type extends any ? Type[] : never;
```

- `ToArray`에 유니온 타입을 넣으면 조건부 타입이 해당 유니온의 각 멤버에 적용됨

```ts twoslash
type ToArray<Type> = Type extends any ? Type[] : never;

type StrArrOrNumArr = ToArray<string | number>;
//   ^?
```

- 여기서 실제로 일어나는 일: `ToArray`가 다음에 대해 분배됨

```ts twoslash
type StrArrOrNumArr =
  // ---cut---
  string | number;
```

- 유니온의 각 멤버 타입에 매핑되어 효과적으로 다음이 됨

```ts twoslash
type ToArray<Type> = Type extends any ? Type[] : never;
type StrArrOrNumArr =
  // ---cut---
  ToArray<string> | ToArray<number>;
```

- 결과적으로 다음이 남음

```ts twoslash
type StrArrOrNumArr =
  // ---cut---
  string[] | number[];
```

- 일반적으로 분배성이 원하는 동작
- 이를 피하려면 `extends` 키워드의 각 측면을 대괄호로 둘러싸면 됨

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

- 반복을 피하려면 때때로 타입이 다른 타입을 기반으로 해야 함
- 매핑된 타입은 미리 선언되지 않은 속성의 타입을 선언하는 인덱스 시그니처 문법을 기반으로 함

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

- 매핑된 타입은 `PropertyKey`의 유니온(주로 [`keyof`](/docs/handbook/2/indexed-access-types.html)로 생성)을 이용해 키를 반복하며 타입을 생성하는 제네릭 타입

```ts twoslash
type OptionsFlags<Type> = {
  [Property in keyof Type]: boolean;
};
```

- 위 예제에서 `OptionsFlags`는 `Type`의 모든 속성을 가져와 값을 불리언으로 변경

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

#### 매핑 수정자

- 매핑 중 적용 가능한 추가 수정자 2가지: `readonly`(가변성)·`?`(선택성)
- `-`·`+`를 접두사로 붙여 제거·추가 가능. 접두사 없으면 `+`로 간주

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

### `as`를 통한 키 재매핑

- TypeScript 4.1 이상에서는 매핑된 타입의 `as` 절로 키를 다시 매핑 가능

```ts
type MappedTypeWithNewProperties<Type> = {
    [Properties in keyof Type as NewKeyType]: Type[Properties]
}
```

- [템플릿 리터럴 타입](/docs/handbook/2/template-literal-types.html) 같은 기능을 활용해 이전 속성 이름에서 새 속성 이름을 만들 수 있음

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

- 조건부 타입으로 `never`를 생성해 키를 필터링할 수도 있음

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

- `string | number | symbol` 유니온뿐 아니라 모든 타입의 유니온에 대해 매핑 가능

```ts twoslash
type EventConfig<Events extends { kind: string }> = {
    [E in Events as E["kind"]]: (event: E) => void;
}

type SquareEvent = { kind: "square", x: number, y: number };
type CircleEvent = { kind: "circle", radius: number };

type Config = EventConfig<SquareEvent | CircleEvent>
//   ^?
```

#### 추가 탐구

- 매핑된 타입은 이 섹션의 다른 기능과 잘 어울림
- 예: [조건부 타입을 사용하는 매핑된 타입](/docs/handbook/2/conditional-types.html) → 객체의 `pii` 속성이 리터럴 `true`인지에 따라 `true`/`false` 반환

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

- 템플릿 리터럴 타입은 [문자열 리터럴 타입](/docs/handbook/2/everyday-types.html#literal-types) 기반 → 유니온을 통해 많은 문자열로 확장 가능
- [JavaScript의 템플릿 리터럴 문자열](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Template_literals)과 동일한 문법을 타입 위치에서 사용
  - 구체적인 리터럴 타입과 함께 쓰면 내용을 연결해 새 문자열 리터럴 타입을 생성

```ts twoslash
type World = "world";

type Greeting = `hello ${World}`;
//   ^?
```

- 보간된 위치에 유니온을 쓰면, 타입은 각 유니온 멤버가 나타낼 수 있는 모든 가능한 문자열 리터럴의 집합

```ts twoslash
type EmailLocaleIDs = "welcome_email" | "email_heading";
type FooterLocaleIDs = "footer_title" | "footer_sendoff";

type AllLocaleIDs = `${EmailLocaleIDs | FooterLocaleIDs}_id`;
//   ^?
```

- 템플릿 리터럴의 각 보간 위치마다 유니온은 교차 곱셈됨

```ts twoslash
type EmailLocaleIDs = "welcome_email" | "email_heading";
type FooterLocaleIDs = "footer_title" | "footer_sendoff";
// ---cut---
type AllLocaleIDs = `${EmailLocaleIDs | FooterLocaleIDs}_id`;
type Lang = "en" | "ja" | "pt";

type LocaleMessageIDs = `${Lang}_${AllLocaleIDs}`;
//   ^?
```

- 일반적으로 큰 문자열 유니온에는 사전 생성을 권장하지만, 작은 경우에는 교차 곱셈 방식도 유용

#### 타입에서의 문자열 유니온

- 템플릿 리터럴의 힘은 타입 내부 정보를 기반으로 새 문자열을 정의할 때 드러남
- 예: 함수(`makeWatchedObject`)가 전달된 객체에 `on()`이라는 새 함수를 추가하는 경우
  - JavaScript 호출 예: `makeWatchedObject(baseObject)`. 기본 객체는 다음과 같은 형태

```ts twoslash
// @noErrors
const passedObject = {
  firstName: "Saoirse",
  lastName: "Ronan",
  age: 26,
};
```

- 기본 객체에 추가될 `on` 함수는 인자 2개를 예상: `eventName`(`string`)·`callback`(`function`)
- `eventName`은 `attributeInThePassedObject + "Changed"` 형식 → 예: `firstName` 속성에서 파생된 `firstNameChanged`
- `callback` 호출 시 조건
  - `attributeInThePassedObject` 이름과 연관된 타입의 값이 전달돼야 함: `firstName`이 `string`이므로 `firstNameChanged` 콜백은 `string` 인자를 예상, `age` 이벤트 콜백은 `number` 인자를 예상
  - `void` 반환 타입 (시연 단순화 목적)
- `on()`의 순진한 시그니처: `on(eventName: string, callback: (newValue: any) => void)` → 하지만 앞서 식별한 중요한 타입 제약을 코드에 문서화하고 싶음 → 템플릿 리터럴 타입으로 이 제약을 코드에 반영 가능

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

- `on`은 `"firstName"`이 아닌 `"firstNameChanged"`를 수신 → `on()`의 사양이 관찰된 객체의 속성 이름 유니온 + "Changed" 접미사로 이벤트 이름 집합을 제한했다면 더 견고했을 것
  - JavaScript에서는 이런 계산이 손쉬움(예: ``Object.keys(passedObject).map(x => `${x}Changed`)``) → _타입 시스템 내부의_ 템플릿 리터럴도 유사한 문자열 조작 접근을 제공

```ts twoslash
type PropEventSource<Type> = {
    on(eventName: `${string & keyof Type}Changed`, callback: (newValue: any) => void): void;
};

/// 속성의 변경 사항을 관찰할 수 있도록
/// `on` 메서드가 있는 "관찰된 객체"를 생성합니다.
declare function makeWatchedObject<Type>(obj: Type): Type & PropEventSource<Type>;
```

- 이를 통해 잘못된 속성이 주어지면 오류가 발생하도록 만들 수 있음

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

#### 템플릿 리터럴을 사용한 추론

- 지금까지는 원래 전달된 객체가 제공한 정보를 다 활용하지 못함: `firstName` 변경(`firstNameChanged` 이벤트)이면 콜백이 `string` 인자를 받아야 하고, `age` 변경이면 `number` 인자를 받아야 하는데도 `callback` 인자 타입으로 `any`를 순진하게 사용 중
  - 템플릿 리터럴 타입으로 속성의 데이터 타입과 해당 콜백 첫 번째 인자의 타입을 동일하게 보장 가능
- 핵심 통찰: 제네릭이 있는 함수로 다음을 수행
  1. 첫 번째 인자에서 사용된 리터럴을 리터럴 타입으로 캡처
  2. 해당 리터럴 타입이 제네릭의 유효한 속성 유니온에 속하는지 검증
  3. 검증된 속성의 타입을 인덱스 접근으로 제네릭 구조에서 조회
  4. 이 타입 정보를 콜백 함수 인자의 타입 확인에 _적용_

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

- 위에서 `on`을 제네릭 메서드로 만듦
- 사용자가 `"firstNameChanged"`로 호출하면 TypeScript가 `Key`의 올바른 타입을 추론
  - `"Changed"` 앞 내용에 `Key`를 매치 → `"firstName"` 추론 → 원래 객체에서 `firstName`의 타입(`string`)을 가져옴
  - `"ageChanged"`로 호출하면 마찬가지로 `age`의 타입(`number`)을 찾음
- 추론은 다양한 방식으로 결합 가능 → 종종 문자열을 분해하고 다른 방식으로 재구성

### 내장 문자열 조작 타입

- TypeScript는 문자열 조작에 쓸 수 있는 타입 집합을 내장 제공
- 이 타입들은 성능을 위해 컴파일러에 내장되어 있어 TypeScript의 `.d.ts` 파일에서는 찾을 수 없음

#### `Uppercase<StringType>`

- 문자열의 각 문자를 대문자로 변환

###### 예제

```ts twoslash
type Greeting = "Hello, world"
type ShoutyGreeting = Uppercase<Greeting>
//   ^?

type ASCIICacheKey<Str extends string> = `ID-${Uppercase<Str>}`
type MainID = ASCIICacheKey<"my_app">
//   ^?
```

#### `Lowercase<StringType>`

- 문자열의 각 문자를 소문자로 변환

###### 예제

```ts twoslash
type Greeting = "Hello, world"
type QuietGreeting = Lowercase<Greeting>
//   ^?

type ASCIICacheKey<Str extends string> = `id-${Lowercase<Str>}`
type MainID = ASCIICacheKey<"MY_APP">
//   ^?
```

#### `Capitalize<StringType>`

- 문자열의 첫 번째 문자를 대문자로 변환

###### 예제

```ts twoslash
type LowercaseGreeting = "hello, world";
type Greeting = Capitalize<LowercaseGreeting>;
//   ^?
```

#### `Uncapitalize<StringType>`

- 문자열의 첫 번째 문자를 소문자로 변환

###### 예제

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

---

## 모듈과 네임스페이스

## 모듈

> **원문:** https://www.typescriptlang.org/docs/handbook/2/modules.html

- JavaScript는 코드를 모듈화하는 다양한 방법의 오랜 역사를 가짐
- 2012년부터 존재해 온 TypeScript는 이러한 형식 다수를 지원 → 시간이 지나며 커뮤니티와 JavaScript 명세는 ES Modules(ES6 modules)로 수렴. `import`/`export` 구문으로 알려짐
- ES Modules는 2015년 JavaScript 명세에 추가 → 2020년에는 대부분의 웹 브라우저·JavaScript 런타임에서 광범위한 지원
- 핸드북은 ES Modules와 그 선행자인 CommonJS `module.exports =` 구문을 모두 다룸. 다른 모듈 패턴은 [모듈](/docs/handbook/modules.html) 참조 섹션 참고

### JavaScript 모듈이 정의되는 방법

- TypeScript에서(ECMAScript 2015와 마찬가지로) 최상위 `import`·`export`를 포함하는 파일은 모듈로 간주됨
- 반대로 최상위 import·export 선언이 없는 파일은 내용이 전역 범위(모듈에서도 마찬가지)에서 사용 가능한 스크립트로 처리
- 모듈은 전역 범위가 아닌 자체 범위 내에서 실행
  - 모듈에서 선언된 변수·함수·클래스는 export로 명시적으로 내보내지 않으면 모듈 외부에서 보이지 않음
  - 다른 모듈에서 내보낸 것을 쓰려면 import로 가져와야 함

### 비모듈

- TypeScript가 모듈로 간주하는 대상 이해가 우선 필요
- JavaScript 명세: `import`·`export`·최상위 `await`이 없는 JavaScript 파일은 스크립트로 간주되며 모듈이 아님
- 스크립트 파일 내 변수·타입은 공유 전역 범위에서 선언 → 여러 입력 파일을 하나의 출력으로 합칠 때 [`outFile`](/tsconfig#outFile) 컴파일러 옵션을 쓰거나, HTML에서 여러 `<script>` 태그로 (올바른 순서로!) 로드한다고 가정
- `import`·`export`가 없는 파일을 모듈로 처리하고 싶다면 다음 줄을 추가

```ts twoslash
export {};
```

- 이것은 파일을 아무것도 내보내지 않는 모듈로 바꿈. 이 구문은 모듈 대상과 무관하게 작동

### TypeScript의 모듈

- TypeScript에서 모듈 기반 코드를 작성할 때 고려할 세 가지 주요 사항

- **구문**: import와 export를 위해 어떤 구문을 사용하고 싶은가?
- **모듈 해결**: 모듈 이름(또는 경로)과 디스크의 파일 사이의 관계는 무엇인가?
- **모듈 출력 대상**: 내보낸 JavaScript 모듈은 어떻게 보여야 하는가?

#### ES Module 구문

- 파일은 `export default`로 주요 내보내기 선언 가능

```ts twoslash
// @filename: hello.ts
export default function helloWorld() {
  console.log("Hello, world!");
}
```

- 다음처럼 가져올 수 있음

```ts twoslash
// @filename: hello.ts
export default function helloWorld() {
  console.log("Hello, world!");
}
// @filename: index.ts
// ---cut---
import helloWorld from "./hello.js";
helloWorld();
```

- 기본 내보내기 외에도 `default`를 생략한 `export`로 변수·함수를 둘 이상 내보낼 수 있음

```ts twoslash
// @filename: maths.ts
export var pi = 3.14;
export let squareTwo = 1.41;
export const phi = 1.61;

export class RandomNumberGenerator {}

export function absolute(num: number) {
  if (num < 0) return num * -1;
  return num;
}
```

- 이것들은 `import` 구문으로 다른 파일에서 사용 가능

```ts twoslash
// @filename: maths.ts
export var pi = 3.14;
export let squareTwo = 1.41;
export const phi = 1.61;
export class RandomNumberGenerator {}
export function absolute(num: number) {
  if (num < 0) return num * -1;
  return num;
}
// @filename: app.ts
// ---cut---
import { pi, phi, absolute } from "./maths.js";

console.log(pi);
const absPhi = absolute(phi);
//    ^?
```

#### 추가 Import 구문

- import는 `import {old as new}` 형식으로 이름을 바꿀 수 있음

```ts twoslash
// @filename: maths.ts
export var pi = 3.14;
// @filename: app.ts
// ---cut---
import { pi as π } from "./maths.js";

console.log(π);
//          ^?
```

- 위 구문들은 단일 `import`로 혼합해 사용 가능

```ts twoslash
// @filename: maths.ts
export const pi = 3.14;
export default class RandomNumberGenerator {}

// @filename: app.ts
import RandomNumberGenerator, { pi as π } from "./maths.js";

RandomNumberGenerator;
// ^?

console.log(π);
//          ^?
```

- `* as name`으로 내보낸 모든 객체를 단일 네임스페이스에 담아 가져올 수 있음

```ts twoslash
// @filename: maths.ts
export var pi = 3.14;
export let squareTwo = 1.41;
export const phi = 1.61;

export function absolute(num: number) {
  if (num < 0) return num * -1;
  return num;
}
// ---cut---
// @filename: app.ts
import * as math from "./maths.js";

console.log(math.pi);
const positivePhi = math.absolute(math.phi);
//    ^?
```

- `import "./file"`로 파일을 가져오면서 현재 모듈에 변수를 _전혀_ 담지 않을 수도 있음

```ts twoslash
// @filename: maths.ts
export var pi = 3.14;
// ---cut---
// @filename: app.ts
import "./maths.js";

console.log("3.14");
```

- 이 경우 `import` 자체는 아무것도 하지 않지만, `maths.ts`의 모든 코드가 평가되어 다른 객체에 영향을 미치는 부작용을 트리거할 수 있음

##### TypeScript 특정 ES Module 구문

- 타입도 JavaScript 값과 같은 구문으로 내보내고 가져올 수 있음

```ts twoslash
// @filename: animal.ts
export type Cat = { breed: string; yearOfBirth: number };

export interface Dog {
  breeds: string[];
  yearOfBirth: number;
}

// @filename: app.ts
import { Cat, Dog } from "./animal.js";
type Animals = Cat | Dog;
```

- TypeScript는 타입 import를 선언하는 두 가지 개념으로 `import` 구문을 확장

###### `import type`

- 타입_만_ 가져올 수 있는 import 문

```ts twoslash
// @filename: animal.ts
export type Cat = { breed: string; yearOfBirth: number };
export type Dog = { breeds: string[]; yearOfBirth: number };
export const createCatName = () => "fluffy";

// @filename: valid.ts
import type { Cat, Dog } from "./animal.js";
export type Animals = Cat | Dog;

// @filename: app.ts
// @errors: 1361
import type { createCatName } from "./animal.js";
const name = createCatName();
```

###### 인라인 `type` imports

- TypeScript 4.5에서는 개별 import에 `type`을 접두사로 붙여 가져온 참조가 타입임을 표시 가능

```ts twoslash
// @filename: animal.ts
export type Cat = { breed: string; yearOfBirth: number };
export type Dog = { breeds: string[]; yearOfBirth: number };
export const createCatName = () => "fluffy";
// ---cut---
// @filename: app.ts
import { createCatName, type Cat, type Dog } from "./animal.js";

export type Animals = Cat | Dog;
const name = createCatName();
```

- 이 두 방식은 함께, Babel·swc·esbuild 같은 비TypeScript 트랜스파일러가 안전하게 제거할 수 있는 import를 식별할 수 있게 함

##### CommonJS 동작을 가진 ES Module 구문

- TypeScript는 CommonJS·AMD `require`에 _직접_ 대응하는 ES Module 구문을 제공
- ES Module import는 _대부분의 경우_ 해당 환경의 `require`와 같지만, 이 구문은 TypeScript 파일에서 CommonJS 출력과 1대1 일치를 보장

```ts twoslash
/// <reference types="node" />
// @module: commonjs
// ---cut---
import fs = require("fs");
const code = fs.readFileSync("hello.ts", "utf8");
```

- 이 구문에 대한 자세한 내용은 [모듈 참조 페이지](/docs/handbook/modules.html#export--and-import--require) 참고

### CommonJS 구문

- CommonJS는 npm 대부분의 모듈이 전달되는 형식
- ES Modules 구문으로 작성하더라도, CommonJS 구문의 동작을 간단히 이해해두면 디버깅에 도움

##### 내보내기

- 식별자는 전역 `module`의 `exports` 속성을 설정해 내보냄

```ts twoslash
/// <reference types="node" />
// ---cut---
function absolute(num: number) {
  if (num < 0) return num * -1;
  return num;
}

module.exports = {
  pi: 3.14,
  squareTwo: 1.41,
  phi: 1.61,
  absolute,
};
```

- 이런 파일은 `require` 문으로 가져올 수 있음

```ts twoslash
// @module: commonjs
// @filename: maths.ts
/// <reference types="node" />
function absolute(num: number) {
  if (num < 0) return num * -1;
  return num;
}

module.exports = {
  pi: 3.14,
  squareTwo: 1.41,
  phi: 1.61,
  absolute,
};
// @filename: index.ts
// ---cut---
const maths = require("./maths");
maths.pi;
//    ^?
```

- JavaScript 구조 분해로 약간 단순화도 가능

```ts twoslash
// @module: commonjs
// @filename: maths.ts
/// <reference types="node" />
function absolute(num: number) {
  if (num < 0) return num * -1;
  return num;
}

module.exports = {
  pi: 3.14,
  squareTwo: 1.41,
  phi: 1.61,
  absolute,
};
// @filename: index.ts
// ---cut---
const { squareTwo } = require("./maths");
squareTwo;
// ^?
```

#### CommonJS와 ES Modules 상호 운용

- 기본 import와 모듈 네임스페이스 객체 import 구분에서 CommonJS·ES Modules 간 기능 불일치 존재
- TypeScript는 [`esModuleInterop`](/tsconfig#esModuleInterop) 플래그로 두 제약 집합 간 마찰을 줄임

### TypeScript의 모듈 해결 옵션

- 모듈 해결 = `import`·`require` 문의 문자열이 참조하는 파일을 결정하는 과정
- TypeScript는 두 가지 해결 전략: Classic·Node
  - Classic은 [`module`](/tsconfig#module)이 `commonjs`가 아닐 때의 기본값. 이전 버전 호환성 목적
  - Node 전략은 Node.js의 CommonJS 모드 동작을 복제하며 `.ts`·`.d.ts`에 대한 추가 검사 포함
- 모듈 전략에 영향을 주는 TSConfig 플래그: [`moduleResolution`](/tsconfig#moduleResolution), [`baseUrl`](/tsconfig#baseUrl), [`paths`](/tsconfig#paths), [`rootDirs`](/tsconfig#rootDirs)
- 각 전략의 상세 동작은 [모듈 해결](/docs/handbook/modules/reference.html#the-moduleresolution-compiler-option) 참조 페이지 참고

### TypeScript의 모듈 출력 옵션

- 내보낸 JavaScript 출력에 영향을 미치는 옵션 2가지

- [`target`](/tsconfig#target): 어떤 JS 기능을 다운레벨(이전 런타임용으로 변환)하고 어떤 것을 그대로 유지할지 결정
- [`module`](/tsconfig#module): 모듈이 서로 상호 작용하는 데 쓰는 코드를 결정

- 어떤 [`target`](/tsconfig#target)을 쓸지는 코드를 실행할 JavaScript 런타임의 가용 기능이 결정 → 예: 지원하는 가장 오래된 웹 브라우저, 실행될 가장 낮은 버전의 Node.js, Electron 같은 런타임의 고유 제약
- 모듈 간 모든 통신은 모듈 로더를 통함 → 어떤 로더를 쓸지는 [`module`](/tsconfig#module) 옵션이 결정
  - 런타임의 모듈 로더는 실행 전 모듈의 모든 종속성을 찾고 실행하는 역할
- 예: ES Modules 구문을 쓰는 TypeScript 파일로 [`module`](/tsconfig#module) 옵션별 차이를 보임

```ts twoslash
// @filename: constants.ts
export const valueOfPi = 3.142;
// @filename: index.ts
// ---cut---
import { valueOfPi } from "./constants.js";

export const twoPi = valueOfPi * 2;
```

##### `ES2020`

```ts twoslash
// @showEmit
// @module: es2020
// @noErrors
import { valueOfPi } from "./constants.js";

export const twoPi = valueOfPi * 2;
```

##### `CommonJS`

```ts twoslash
// @showEmit
// @module: commonjs
// @noErrors
import { valueOfPi } from "./constants.js";

export const twoPi = valueOfPi * 2;
```

##### `UMD`

```ts twoslash
// @showEmit
// @module: umd
// @noErrors
import { valueOfPi } from "./constants.js";

export const twoPi = valueOfPi * 2;
```

> ES2020은 원래 `index.ts`와 효과적으로 같습니다.

- 사용 가능한 모든 옵션과 출력 JavaScript 코드의 모습은 [`module`에 대한 TSConfig 참조](/tsconfig#module) 참고

### TypeScript 네임스페이스

- TypeScript는 ES Modules 표준보다 앞선 `namespaces`라는 자체 모듈 형식을 가짐
- 이 구문은 복잡한 정의 파일을 만드는 데 유용한 기능이 많아 [DefinitelyTyped](https://github.com/DefinitelyTyped/DefinitelyTyped)에서 여전히 활발히 사용됨
- 더 이상 사용되지 않는 것은 아니나, 네임스페이스 기능 대부분이 ES Modules에도 존재 → JavaScript 방향에 맞추기 위해 네임스페이스보다 ES Modules 권장
- 네임스페이스 상세는 [네임스페이스 참조 페이지](/docs/handbook/namespaces.html) 참고

---

## 모듈 - 소개

> **원문:** https://www.typescriptlang.org/docs/handbook/modules/introduction.html

이 문서는 네 섹션으로 구성:

1. [**이론**](/docs/handbook/modules/theory.html): TypeScript가 모듈에 접근하는 방식의 배경. 상황에 맞는 모듈 관련 컴파일러 옵션 작성, TypeScript를 다른 도구와 통합하는 방법 추론, TypeScript의 의존성 패키지 처리 방식 이해가 목적이면 여기서 시작 권장 → 가이드·참조 페이지를 읽기 쉽게 하고, 여기서 다루지 않는 실제 문제를 풀 때 쓸 정신적 프레임워크를 제공
2. [**가이드**](/docs/handbook/modules/guides/choosing-compiler-options.html): 새 프로젝트에 맞는 컴파일 설정을 고르는 것부터 시작해 실제 작업 방법을 보여줌 → 빠른 시작을 원하는 초보자와, 이론은 알지만 복잡한 작업의 구체적 지침이 필요한 전문가 모두에게 적합
3. [**참조**](/docs/handbook/modules/reference.html): 앞 섹션에서 제시된 구문·구성에 대한 상세 설명
4. [**부록**](/docs/handbook/modules/appendices/esm-cjs-interop.html): 이론·참조 섹션 범위를 넘어서는 상세 설명이 필요한 복잡한 주제

---

## 모듈 - 이론

> **원문:** https://www.typescriptlang.org/docs/handbook/modules/theory.html

### JavaScript에서의 스크립트와 모듈

- JavaScript 초기(언어가 브라우저에서만 실행되던 시절)에는 모듈이 없었지만, HTML의 여러 `script` 태그로 웹 페이지의 JavaScript를 여러 파일로 분할 가능

```html
<html>
  <head>
    <script src="a.js"></script>
    <script src="b.js"></script>
  </head>
  <body></body>
</html>
```

- 이 방식에는 단점이 있음(특히 웹 페이지가 커지고 복잡해질수록): 같은 페이지에 로드된 모든 스크립트가 같은 스코프("전역 스코프")를 공유 → 스크립트가 서로의 변수·함수를 덮어쓰지 않도록 매우 주의해야 함
- 파일에 자체 스코프를 주면서도 코드 조각을 다른 파일에서 쓸 수 있게 하는 시스템을 "모듈 시스템"이라 부름
  - (모듈 시스템의 각 파일을 "모듈"이라 부르는 게 당연해 보이지만, 이 용어는 모듈 시스템 외부에서 전역 스코프로 실행되는 _스크립트_ 파일과 대조하는 데 주로 쓰임)

> [많은 모듈 시스템](https://github.com/myshov/history-of-javascript/tree/master/4_evolution_of_js_modularity)이 있으며, TypeScript는 [여러 가지를 출력하도록 지원](https://www.typescriptlang.org/tsconfig/#module)하지만, 이 문서는 오늘날 가장 중요한 두 시스템인 ECMAScript 모듈(ESM)과 CommonJS(CJS)에 초점을 맞춥니다.
>
> ECMAScript 모듈(ESM)은 언어에 내장된 모듈 시스템으로, 최신 브라우저와 v12 이후의 Node.js에서 지원됩니다. 전용 `import` 및 `export` 구문을 사용합니다:
>
> ```js
> // a.js
> export default "Hello from a.js";
> ```
>
> ```js
> // b.js
> import a from "./a.js";
> console.log(a); // 'Hello from a.js'
> ```
>
> CommonJS(CJS)는 ESM이 언어 사양의 일부가 되기 전에 원래 Node.js에 제공된 모듈 시스템입니다. ESM과 함께 Node.js에서 여전히 지원됩니다. `exports`와 `require`라는 일반 JavaScript 객체와 함수를 사용합니다:
>
> ```js
> // a.js
> exports.message = "Hello from a.js";
> ```
>
> ```js
> // b.js
> const a = require("./a");
> console.log(a.message); // 'Hello from a.js'
> ```

- TypeScript는 파일이 CommonJS·ECMAScript 모듈임을 감지하면 해당 파일이 자체 스코프를 가질 것으로 가정 → 이후 컴파일러 작업이 좀 더 복잡해짐

### 모듈에 관한 TypeScript의 역할

- TypeScript 컴파일러의 주요 목표 = 컴파일 시간에 특정 종류의 런타임 오류를 잡아 방지
- 모듈 관련 여부와 무관하게, 컴파일러는 코드의 의도된 런타임 환경(예: 어떤 전역 변수가 사용 가능한지)을 알아야 함
- 모듈이 관련되면 컴파일러가 추가로 답해야 할 질문이 생김 → 예제 입력 코드로 확인

```ts
import sayHello from "greetings";
sayHello("world");
```

- 이 파일을 확인하려면 컴파일러가 `sayHello`의 타입(문자열 인자 하나를 받는 함수인가?)을 알아야 함 → 추가로 열리는 질문들
  1. 모듈 시스템이 이 TypeScript 파일을 직접 로드하는가, 아니면 이 파일로부터 생성되는 JavaScript 파일을 로드하는가?
  2. 로드할 파일 이름·디스크 위치를 고려할 때 모듈 시스템은 어떤 _종류_의 모듈을 기대하는가?
  3. 출력 JavaScript가 생성된다면, 이 파일의 모듈 구문은 출력 코드에서 어떻게 변환되는가?
  4. 모듈 시스템이 `"greetings"`로 지정된 모듈을 어디서 찾는가? 조회는 성공하는가?
  5. 해당 조회로 해결된 파일은 어떤 종류의 모듈인가?
  6. 모듈 시스템이 (2)의 모듈 종류가 (3)의 구문으로 (5)의 모듈 종류를 참조하도록 허용하는가?
  7. `"greetings"` 모듈이 분석되면, 그 모듈의 어떤 부분이 `sayHello`에 바인딩되는가?
- 이 질문들은 모두 _호스트_(출력 JavaScript나 원시 TypeScript를 소비해 모듈 로딩 동작을 지시하는 시스템 — 보통 Node.js 같은 런타임이나 Webpack 같은 번들러)의 특성에 따라 달라짐
- ECMAScript 사양은 ESM import·export의 연결 방식은 정의하지만, (4)의 _모듈 해결_이 어떻게 일어나는지는 지정하지 않고 CommonJS 같은 다른 모듈 시스템에 대해서는 아무것도 말하지 않음 → 런타임·번들러(특히 ESM·CJS를 모두 지원하려는 것들)는 자체 규칙을 설계할 자유가 큼
  - 따라서 TypeScript가 위 질문에 답하는 방식은 코드가 실행될 환경에 따라 크게 달라짐 → 단일 정답이 없으므로 구성 옵션으로 규칙을 알려줘야 함
- 다른 핵심 아이디어: TypeScript는 이 질문들을 거의 항상 _출력_ JavaScript 파일 관점에서 생각함(_입력_ TypeScript/JavaScript 파일 관점이 아님)
  - 오늘날 일부 런타임·번들러는 TypeScript 파일을 직접 로드 지원 → 이 경우 입력·출력 파일을 별도로 생각하는 게 무의미
  - 이 문서 대부분은 TypeScript → JavaScript 컴파일 후 런타임 모듈 시스템이 로드하는 경우를 다룸 → 이 경우를 먼저 이해하면 컴파일러 옵션·동작 파악에 필수적이고, esbuild·Bun 등 [TypeScript 우선 런타임·번들러](#번들러-typescript-런타임-및-nodejs-로더를-위한-모듈-해결)를 다룰 때도 단순화하기 쉬움
- 지금까지를 출력 파일 관점에서 요약: **호스트의 규칙**을 충분히 이해해
  1. 파일을 유효한 **출력 모듈 형식**으로 컴파일
  2. 해당 **출력**의 import가 **성공적으로 해결**되도록 보장
  3. **가져온 이름**에 어떤 **타입**을 할당할지 판단

### 호스트는 누구인가?

- _호스트_ = "출력 코드를 소비해 모듈 로딩 동작을 지시하는 시스템" → TypeScript 외부에서 TypeScript의 모듈 분석이 모델링하려는 대상
  - 출력 코드(`tsc`나 타사 트랜스파일러 생성)가 Node.js 같은 런타임에서 직접 실행되면 → 그 런타임이 호스트
  - 런타임이 TypeScript 파일을 직접 소비해 "출력 코드"가 없어도 → 그 런타임이 여전히 호스트
  - 번들러가 TypeScript 입력·출력을 소비해 번들을 생성하면 → 번들러가 호스트(원래 import/require를 보고 참조 파일을 찾아, import/require가 지워지거나 인식 불가로 변환된 새 파일/파일 세트를 만들기 때문). 해당 번들 자체가 모듈로 구성돼 이를 실행하는 런타임이 그 번들의 호스트가 될 수 있지만, TypeScript는 번들러 이후에 일어나는 일을 알지 못함
  - 다른 트랜스파일러·옵티마이저·포매터가 TypeScript 출력에서 실행되면 → import/export를 그대로 두는 한 TypeScript가 신경 쓰는 호스트가 _아님_
  - 웹 브라우저에서 모듈을 로드할 때는 TypeScript가 모델링해야 하는 동작이 웹 서버와 브라우저 실행 모듈 시스템 사이에 나뉨: 브라우저의 JS 엔진(또는 RequireJS 같은 프레임워크)이 허용 모듈 형식을 제어, 웹 서버는 요청 시 어떤 파일을 보낼지 결정
  - TypeScript 컴파일러 자체는 호스트가 아님 → 다른 호스트를 모델링하는 것 외에는 모듈 관련 동작을 직접 제공하지 않기 때문

### 모듈 출력 형식

- 모든 프로젝트가 답해야 할 첫 질문: 호스트가 어떤 종류의 모듈을 기대하는가 → 이에 맞춰 TypeScript가 각 파일의 출력 형식을 설정
  - 호스트가 한 종류만 _지원_하는 경우도 있음(예: 브라우저의 ESM, Node.js v11 이하의 CJS)
  - Node.js v12 이상은 CJS·ES 모듈을 모두 허용 → 파일 확장자와 `package.json`으로 각 파일의 형식을 결정, 내용이 예상과 다르면 오류
- `module` 컴파일러 옵션이 이 정보를 컴파일러에 전달
  - 주 목적: 컴파일 중 생성되는 JavaScript의 모듈 형식 제어
  - 추가 역할: 각 파일의 모듈 종류 감지 방법, 서로 다른 모듈 종류 간 import 가능 여부, `import.meta`·최상위 `await` 같은 기능의 사용 가능 여부를 컴파일러에 알림
  - 따라서 `noEmit`을 쓰는 프로젝트라도 올바른 `module` 설정 선택이 중요(컴파일러가 import 타입 체크·IntelliSense를 위해 모듈 시스템을 정확히 이해해야 하므로)
  - 프로젝트에 맞는 `module` 설정 선택 지침은 [_컴파일러 옵션 선택_](/docs/handbook/modules/guides/choosing-compiler-options.html) 참고

사용 가능한 `module` 설정:

- [**`node16`**](/docs/handbook/modules/reference.html#node16-node18-node20-nodenext): 특정 상호 운용성·감지 규칙으로 ES 모듈·CJS 모듈을 나란히 지원하는 Node.js v16+ 모듈 시스템 반영
- [**`node18`**](/docs/handbook/modules/reference.html#node16-node18-node20-nodenext): import 속성 지원을 추가한 Node.js v18+ 모듈 시스템 반영
- [**`nodenext`**](/docs/handbook/modules/reference.html#node16-node18-node20-nodenext): Node.js 모듈 시스템 발전을 따라가는 이동 목표. TypeScript 5.8 기준 ECMAScript 모듈의 `require` 지원
- [**`es2015`**](/docs/handbook/modules/reference.html#es2015-es2020-es2022-esnext): `import`·`export`를 처음 도입한 ES2015 언어 사양 반영
- [**`es2020`**](/docs/handbook/modules/reference.html#es2015-es2020-es2022-esnext): `es2015` + `import.meta`·`export * as ns from "mod"` 지원 추가
- [**`es2022`**](/docs/handbook/modules/reference.html#es2015-es2020-es2022-esnext): `es2020` + 최상위 `await` 지원 추가
- [**`esnext`**](/docs/handbook/modules/reference.html#es2015-es2020-es2022-esnext): 현재는 `es2022`와 동일. 향후 ECMAScript 사양의 모듈 관련 Stage 3+ 제안을 반영할 이동 목표
- **[`commonjs`](/docs/handbook/modules/reference.html#commonjs), [`system`](/docs/handbook/modules/reference.html#system), [`amd`](/docs/handbook/modules/reference.html#amd), [`umd`](/docs/handbook/modules/reference.html#umd)**: 각 모듈 시스템으로 모든 것을 출력하며 해당 시스템으로 성공적으로 가져올 수 있다고 가정. 새 프로젝트에는 더 이상 권장되지 않아 이 문서에서 자세히 다루지 않음

> Node.js의 모듈 형식 감지·상호 운용성 규칙 때문에, Node.js에서 실행되는 프로젝트에 `module`을 `esnext`나 `commonjs`로 지정하는 것은(`tsc`가 출력하는 각 파일이 실제로 ESM이나 CJS이더라도) 부정확함. Node.js에서 실행할 프로젝트의 유일한 올바른 `module` 설정은 `node16`과 `nodenext`. 전체 ESM Node.js 프로젝트라면 출력 JavaScript가 `esnext`·`nodenext` 컴파일 사이에 같아 보일 수 있지만 타입 검사는 다를 수 있음. 자세한 내용은 [`nodenext` 참조 섹션](/docs/handbook/modules/reference.html#node16-node18-node20-nodenext) 참고

#### 모듈 형식 감지

- Node.js는 ES 모듈·CJS 모듈을 모두 이해하지만, 각 파일의 형식은 파일 확장자와 (해당 디렉토리 및 상위 디렉토리에서 검색한) 가장 가까운 `package.json`의 `type` 필드로 결정됨
  - `.mjs`·`.cjs` 파일 = 항상 각각 ES 모듈·CJS 모듈
  - `.js` 파일: 가장 가까운 `package.json`에 `type: "module"`이면 ES 모듈, 아니면(파일 없음·필드 없음·다른 값) CJS 모듈
- ES 모듈로 결정된 파일 → Node.js가 평가 중 CommonJS `module`·`require` 객체를 스코프에 주입하지 않아, 이를 사용하면 충돌
- CJS 모듈로 결정된 파일 → `import`·`export` 선언이 구문 오류로 충돌
- `module` 컴파일러 옵션이 `node16`·`node18`·`nodenext`면, TypeScript는 프로젝트의 _입력_ 파일에 동일 알고리즘을 적용해 각 _출력_ 파일의 모듈 종류를 결정
- `--module nodenext` 예제 프로젝트의 모듈 형식 감지

- `/package.json`: 내용 `{}`
- `/main.mts` → 출력 `/main.mjs`
  - 모듈 종류: ESM
  - 이유: 파일 확장자
- `/utils.cts` → 출력 `/utils.cjs`
  - 모듈 종류: CJS
  - 이유: 파일 확장자
- `/example.ts` → 출력 `/example.js`
  - 모듈 종류: CJS
  - 이유: `package.json`에 `"type": "module"` 없음
- `/node_modules/pkg/package.json`: 내용 `{ "type": "module" }`
- `/node_modules/pkg/index.d.ts`
  - 모듈 종류: ESM
  - 이유: `package.json`의 `"type": "module"`
- `/node_modules/pkg/index.d.cts`
  - 모듈 종류: CJS
  - 이유: 파일 확장자

#### 입력 모듈 구문

- 입력 소스 파일의 _입력_ 모듈 구문은 출력 JS 파일의 모듈 구문과 다소 분리되어 있음에 유의
- 즉, ESM import가 있는 파일

```ts
import { sayHello } from "greetings";
sayHello("world");
```

- 는 `module` 컴파일러 옵션에 따라 ESM 형식 그대로 출력될 수도, CommonJS로 출력될 수도 있음

```ts
Object.defineProperty(exports, "__esModule", { value: true });
const greetings_1 = require("greetings");
(0, greetings_1.sayHello)("world");
```

- 즉 입력 파일 내용만 봐서는 ES 모듈인지 CJS 모듈인지 결정하기에 부족함

#### ESM과 CJS 상호 운용성

- 궁금한 지점: ES 모듈이 CommonJS 모듈을 `import`할 수 있는가? 그렇다면 기본 import가 `exports`에 연결되는가, `exports.default`에 연결되는가? CommonJS 모듈이 ES 모듈을 `require`할 수 있는가?
- CommonJS는 ECMAScript 사양의 일부가 아님 → ESM이 2015년 표준화된 이래 런타임·번들러·트랜스파일러가 이 질문들에 자유롭게 답을 만들어옴 → 표준 상호 운용성 규칙이 존재하지 않음
- 오늘날 대부분의 런타임·번들러는 세 범주 중 하나
  1. **ESM 전용**: 브라우저 엔진 등 일부 런타임은 언어에 속한 ECMAScript 모듈만 지원
  2. **번들러 스타일**: 주요 JS 엔진이 ES 모듈을 실행하기 전, Babel이 CommonJS 트랜스파일로 ES 모듈 작성을 지원 → ESM-트랜스파일-CJS와 수작업 CJS 간 상호작용 방식이 번들러·트랜스파일러의 사실상 표준 상호 운용성 규칙이 됨
  3. **Node.js**: Node.js v20.19.0까지 CommonJS는 ES 모듈을 동기적으로(`require`) 로드 불가 → 동적 `import()`로만 비동기 로드 가능. ES 모듈은 CJS 모듈을 기본 import 가능하며 항상 `exports`에 바인딩(→ `__esModule`이 있는 Babel 스타일 CJS 출력의 기본 import는 Node.js와 일부 번들러 사이에서 동작이 다름)
- TypeScript는 import(특히 `default`)에 올바른 타입을 주고 런타임에 충돌할 import에 오류를 내려면 이 규칙 중 무엇을 가정할지 알아야 함
  - `module`이 `node16`·`node18`·`nodenext`면 Node.js의 버전별 규칙 적용
  - 다른 모든 `module` 설정은 [`esModuleInterop`](/docs/handbook/modules/reference.html#esModuleInterop) 옵션과 결합해 번들러 스타일 상호 운용으로 처리

#### 모듈 지정자는 기본적으로 변환되지 않음

- `module` 컴파일러 옵션은 입력 파일의 import·export를 다른 출력 모듈 형식으로 변환할 수 있지만, 모듈 _지정자_(`import`하거나 `require`에 넘기는 문자열) 자체는 작성된 그대로 출력됨
- 예: 다음 입력

```ts
import { add } from "./math.mjs";
add(1, 2);
```

- 는 다음 중 하나로 출력될 수 있음

```ts
import { add } from "./math.mjs";
add(1, 2);
```

또는:

```ts
const math_1 = require("./math.mjs");
math_1.add(1, 2);
```

- `module` 옵션에 따라 다르지만 모듈 지정자는 어느 쪽이든 `"./math.mjs"`로 유지됨
- 기본적으로 모듈 지정자는 코드의 대상 런타임·번들러에서 작동하는 방식으로 작성해야 하며, TypeScript는 그 _출력_-상대적 지정자를 이해하는 역할을 함
- 모듈 지정자가 참조하는 파일을 찾는 과정 = _모듈 해결_

### 모듈 해결

- [첫 번째 예제](#모듈에-관한-typescripts-역할)로 돌아가 지금까지 배운 것을 검토

```ts
import sayHello from "greetings";
sayHello("world");
```

- 지금까지 호스트의 모듈 시스템과 TypeScript `module` 옵션이 이 코드에 미칠 수 있는 영향을 논의함
  - 입력 구문은 ESM처럼 보이지만 출력 형식은 `module` 옵션·파일 확장자·`package.json` `"type"` 필드에 따라 달라짐
  - `sayHello`가 어디에 바인딩되는지, 심지어 import 허용 여부까지 이 파일과 대상 파일의 모듈 종류에 따라 달라질 수 있음
  - 다만 대상 파일을 어떻게 _찾는지_는 아직 다루지 않음

#### 모듈 해결은 호스트가 정의함

- ECMAScript 사양은 `import`·`export` 문의 파싱·해석 방법은 정의하지만 모듈 해결은 호스트에 맡김
- 예: 새 JavaScript 런타임을 만든다면 다음과 같은 모듈 해결 체계도 만들 수 있음

```ts
import monkey from "🐒"; // './eats/bananas.js'를 찾음
import cow from "🐄";    // './eats/grass.js'를 찾음
import lion from "🦁";   // './eats/you.js'를 찾음
```

- 이렇게 하고도 "표준 호환 ESM"을 구현했다고 주장할 수 있음 → TypeScript는 이 런타임의 모듈 해결 알고리즘을 내장 지식으로 갖지 않는 한 `monkey`·`cow`·`lion`에 어떤 타입을 줘야 할지 알 수 없음
- `module`이 호스트의 예상 모듈 형식을 컴파일러에 알리듯, `moduleResolution`은 호스트가 모듈 지정자를 파일로 해결하는 알고리즘을 (몇 가지 사용자 정의 옵션과 함께) 지정
  - 이는 TypeScript가 emit 중 import 지정자를 수정하지 않는 이유이기도 함: import 지정자와 디스크 파일 사이 관계는 호스트가 정의하며 TypeScript는 호스트가 아님

사용 가능한 `moduleResolution` 옵션:

- [**`classic`**](/docs/handbook/modules/reference.html#classic): TypeScript의 가장 오래된 모듈 해결 모드. `module`이 `commonjs`·`node16`·`nodenext` 외의 값일 때 기본값(불행히도) → [RequireJS](https://requirejs.org/docs/api.html#packages) 구성 전반에 대한 최선의 해결을 위해 만들어졌던 듯함. 새 프로젝트(또는 RequireJS·AMD 모듈 로더를 안 쓰는 오래된 프로젝트)에는 사용 금지, TypeScript 6.0에서 폐지 예정
- [**`node10`**](/docs/handbook/modules/reference.html#node10-formerly-known-as-node): 이전 이름 `node`. `module`이 `commonjs`일 때의 기본값(불행히도). v12 이전 Node.js의 꽤 좋은 모델이며 대부분 번들러의 모듈 해결에 대한 합리적 근사치이기도 함. `node_modules` 패키지 조회, 디렉토리 `index.js` 로드, 상대 지정자의 `.js` 확장자 생략을 지원 → 다만 Node.js v12가 ES 모듈에 다른 해결 규칙을 도입했으므로 최신 Node.js에는 매우 나쁜 모델. 새 프로젝트에 사용 금지
- [**`node16`**](/docs/handbook/modules/reference.html#node16-nodenext-1): `--module node16`·`node18`의 대응이자 해당 설정의 기본값. Node.js v12+는 ESM·CJS를 모두 지원하며 각각 별도 해결 알고리즘 사용 → import 문·동적 `import()`의 모듈 지정자는 확장자·`/index.js` 접미사 생략 불가, `require` 호출의 지정자는 생략 가능 → 이 모드는 [모듈 형식 감지 규칙](#모듈-형식-감지)이 결정하는 위치에서 이 제한을 이해·적용
- [**`nodenext`**](/docs/handbook/modules/reference.html#node16-nodenext-1): 현재 `node16`과 동일. `--module nodenext`의 대응이자 기본값. Node.js의 새 모듈 해결 기능이 추가될 때마다 이를 반영하는 미래 지향 모드
- [**`bundler`**](/docs/handbook/modules/reference.html#bundler): Node.js v12가 도입한 새 모듈 해결 기능(`package.json`의 `"exports"`·`"imports"`)을 많은 번들러가 ESM import에 대한 더 엄격한 규칙 없이 채택 → 이 모드는 번들러 대상 코드의 기본 알고리즘 제공. 기본적으로 `"exports"`·`"imports"`를 지원하되 무시하도록 구성 가능. `module`을 `esnext`로 설정해야 함

#### TypeScript는 호스트의 모듈 해결을 모방하지만, 타입과 함께

- TypeScript [역할](#모듈에-관한-typescripts-역할)의 세 요소를 복기

1. 파일을 유효한 **출력 모듈 형식**으로 컴파일
2. 해당 **출력**의 import가 **성공적으로 해결**되도록 보장
3. **가져온 이름**에 어떤 **타입**을 할당할지 앎

- 모듈 해결은 마지막 두 항목에 필요
  - 입력 파일 작업 중에는 (2)를 잊기 쉬움 → 모듈 해결의 핵심은 [입력 파일과 동일한 모듈 지정자](#모듈-지정자는-기본적으로-변환되지-않음)를 포함하는 출력 파일의 import·`require` 호출이 실제로 런타임에서 작동하는지 검증하는 것

#### 선언 파일의 역할

- 앞선 예제는 입력·출력 파일 사이에서 작동하는 모듈 해결의 "재매핑" 부분
- 그런데 라이브러리 코드를 가져올 때는? 라이브러리가 TypeScript로 작성됐더라도 소스 코드를 게시하지 않았을 수 있음
  - JavaScript 파일을 TypeScript 파일로 매핑할 수 없으면 → import 런타임 동작 확인은 가능하지만, 타입 할당이라는 두 번째 목표는 어떻게?
- 이것이 선언 파일(`.d.ts`, `.d.mts` 등)이 필요한 이유
  - 선언 파일 해석 방식을 이해하려면 그 출처를 알아야 함: 입력 파일에서 `tsc --declaration`을 실행하면 출력 JavaScript 파일 하나와 출력 선언 파일 하나를 얻음
- 이 관계 때문에 컴파일러는 선언 파일을 볼 때마다, 그 타입 정보로 완벽히 설명되는 JavaScript 파일이 있다고 _가정_
  - 성능상 모든 모듈 해결 모드에서 컴파일러는 항상 TypeScript·선언 파일을 먼저 찾고, 찾으면 JavaScript 파일을 더 찾지 않음
  - TypeScript 입력 파일을 찾으면 컴파일 후 JavaScript 파일이 _존재할_ 것을 알고, 선언 파일을 찾으면 이미(누군가의) 컴파일이 일어나 선언 파일과 동시에 JavaScript 파일이 만들어졌음을 앎
- 선언 파일은 JavaScript 파일의 존재뿐 아니라 그 이름·확장자도 알려줌

- `.d.ts`: JavaScript `.js` / TypeScript `.ts`
- `.d.ts`: JavaScript `.js` / TypeScript `.tsx`
- `.d.mts`: JavaScript `.mjs` / TypeScript `.mts`
- `.d.cts`: JavaScript `.cjs` / TypeScript `.cts`
- `.d.*.ts`: JavaScript `.*`

- 마지막 행은 `allowArbitraryExtensions` 컴파일러 옵션으로 비-JS 파일에 타입을 지정해 모듈 시스템이 비-JS 파일을 JavaScript 객체로 가져오는 경우를 표현. 예: `styles.css`는 `styles.d.css.ts` 선언 파일로 표현 가능

#### 번들러, TypeScript 런타임 및 Node.js 로더를 위한 모듈 해결

- 지금까지 _입력 파일_과 _출력 파일_의 구별을 강조해 왔음
- 상대 모듈 지정자에 파일 확장자를 쓸 때 TypeScript는 보통 [_출력_ 파일 확장자 사용을 요구](#typescript는-호스트의-모듈-해결을-모방하지만-타입과-함께)

```ts
// @Filename: src/math.ts
export function add(a: number, b: number) {
  return a + b;
}

// @Filename: src/main.ts
import { add } from "./math.ts";
//                  ^^^^^^^^^^^
// 'allowImportingTsExtensions'가 활성화된 경우에만 import 경로는 '.ts' 확장자로 끝날 수 있습니다.
```

- TypeScript가 확장자를 `.js`로 [다시 쓰지 않으므로](#모듈-지정자는-기본적으로-변환되지-않음) 이 제한이 적용됨 → 출력 JS 파일에 `"./math.ts"`가 남으면 그 import는 런타임에 다른 JS 파일로 해결되지 않음. TypeScript는 안전하지 않은 출력 JS를 만들지 않으려 함
- 하지만 출력 JS 파일이 _없는_ 경우도 있음
  - 번들러가 메모리에서 TypeScript 파일을 트랜스파일하도록 구성되어 있어 작성한 모든 import를 소비·지워 번들을 생성하는 경우
  - Deno·Bun 같은 TypeScript 런타임에서 코드를 직접 실행하는 경우
  - `ts-node`·`tsx` 등 Node용 트랜스파일링 로더를 쓰는 경우
  - → 이럴 때는 `noEmit`(또는 `emitDeclarationOnly`)과 `allowImportingTsExtensions`를 켜서 안전하지 않은 JS 출력을 끄고 `.ts` 확장자 import 오류를 무음화 가능
- `allowImportingTsExtensions` 여부와 무관하게, 모듈 해결 호스트에 맞는 `moduleResolution` 설정 선택은 여전히 중요
  - 번들러·Bun 런타임 = `bundler`. 이 해결기들은 Node.js에서 영감을 받았지만 Node.js가 import에 적용하는 [확장자 검색 비활성화](#확장자-검색-및-디렉토리-인덱스-파일) 같은 엄격한 ESM 해결 알고리즘은 채택하지 않음
  - `bundler` 설정은 `node16`-`nodenext`처럼 `package.json` `"exports"`를 지원하면서도 항상 확장자 없는 import를 허용

#### 라이브러리를 위한 모듈 해결

- 앱 컴파일 시에는 모듈 해결 [호스트](#모듈-해결은-호스트가-정의함)가 누구인지에 따라 `moduleResolution`을 고름
- 라이브러리 컴파일 시에는 출력 코드가 어디서 실행될지 모르지만 가능한 많은 곳에서 실행되길 원함
  - `"module": "node18"`(암시적으로 [`"moduleResolution": "node16"`](/docs/handbook/modules/reference.html#node16-nodenext-1))을 쓰면 출력 JavaScript의 모듈 지정자 호환성이 최대화됨(Node.js `import` 해결의 더 엄격한 규칙을 강제하기 때문)
  - 라이브러리가 `"moduleResolution": "bundler"`(또는 더 나쁜 `"node10"`)로 컴파일되면 어떻게 되는지 확인

```ts
export * from "./utils";
```

- `./utils.ts`(또는 `./utils/index.ts`)가 존재한다고 가정하면 번들러는 이 코드에 문제없어 `"moduleResolution": "bundler"`도 불평하지 않음. `"module": "esnext"`로 컴파일하면 출력 JavaScript는 입력과 정확히 같은 모양 → 이 JavaScript를 npm에 게시하면 번들러 사용 프로젝트에서는 쓸 수 있지만 Node.js에서 실행하면 오류 발생

```
Error [ERR_MODULE_NOT_FOUND]: Cannot find module '.../node_modules/dependency/utils' imported from .../node_modules/dependency/index.js
Did you mean to import ./utils.js?
```

- 반면 다음과 같이 작성했다면

```ts
export * from "./utils.js";
```

- Node.js _와_ 번들러 모두에서 작동하는 출력이 만들어짐
- 요컨대 `"moduleResolution": "bundler"`는 전염성이 있어 번들러에서만 작동하는 코드를 만들 수 있음
  - 반면 `"moduleResolution": "nodenext"`는 출력이 Node.js에서 작동하는지 확인하는데, 대부분 Node.js에서 작동하는 모듈 코드는 다른 런타임·번들러에서도 작동함
- 이 지침은 라이브러리가 `tsc`의 출력을 그대로 제공하는 경우에만 적용됨
  - 제공 _전에_ 번들링된다면 `"moduleResolution": "bundler"`도 허용될 수 있음 → 이때 최종 빌드의 안전성·호환성 보장은 모듈 형식·지정자를 바꾸는 빌드 도구의 책임이며, `tsc`는 런타임에 어떤 모듈 코드가 존재할지 알 수 없어 더 기여할 수 없음

---

## 모듈 - 참조

> **원문:** https://www.typescriptlang.org/docs/handbook/modules/reference.html

### 모듈 구문

- TypeScript 컴파일러는 TypeScript·JavaScript 파일에서 표준 [ECMAScript 모듈 구문](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Modules)을, JavaScript 파일에서 여러 형태의 [CommonJS 구문](https://www.typescriptlang.org/docs/handbook/type-checking-javascript-files.html#commonjs-modules-are-supported)을 인식
- TypeScript 파일·JSDoc 주석에서 쓸 수 있는 TypeScript 전용 구문 확장도 있음

#### TypeScript 전용 선언의 가져오기 및 내보내기

- 타입 별칭·인터페이스·열거형·네임스페이스도 표준 JavaScript 선언처럼 `export` 수정자로 모듈에서 내보낼 수 있음

```ts
// 표준 JavaScript 구문...
export function f() {}
// ...타입 선언으로 확장됨
export type SomeType = /* ... */;
export interface SomeInterface { /* ... */ }
```

- 표준 JavaScript 선언 참조와 함께 명명된 내보내기에도 등장 가능

```ts
export { f, SomeType, SomeInterface };
```

- 내보낸 타입(및 기타 TypeScript 전용 선언)은 표준 ECMAScript import로 가져올 수 있음

```ts
import { f, SomeType, SomeInterface } from "./module.js";
```

- 네임스페이스 import·export를 쓸 때 내보낸 타입은 타입 위치에서 참조 시 네임스페이스로 접근 가능

```ts
import * as mod from "./module.js";
mod.f();
mod.SomeType; // 'SomeType' 속성이 'typeof import("./module.js")' 타입에 존재하지 않습니다
let x: mod.SomeType; // Ok
```

#### 타입 전용 가져오기 및 내보내기

- JavaScript로 emit할 때 TypeScript는 기본적으로 타입 위치에서만 쓰이는 import와 타입만 참조하는 export를 자동으로 생략
- 타입 전용 가져오기·내보내기로 이 동작을 강제하고 생략을 명시화할 수 있음: `import type`으로 쓴 import 선언, `export type { ... }`으로 쓴 export 선언, `type` 키워드를 접두사로 붙인 import/export 지정자는 모두 출력 JavaScript에서 생략됨이 보장됨

```ts
// @Filename: main.ts
import { f, type SomeInterface } from "./module.js";
import type { SomeType } from "./module.js";

class C implements SomeInterface {
  constructor(p: SomeType) {
    f();
  }
}

export type { C };

// @Filename: main.js
import { f } from "./module.js";

class C {
  constructor(p) {
    f();
  }
}
```

- 값도 `import type`으로 가져올 수 있지만 출력 JavaScript에 존재하지 않으므로 비출력 위치에서만 사용 가능

```ts
import type { f } from "./module.js";
f(); // 'import type'으로 가져왔기 때문에 'f'를 값으로 사용할 수 없습니다
let otherFunction: typeof f = () => {}; // Ok
```

- 타입 전용 import 선언은 기본 import와 명명된 바인딩을 동시에 선언할 수 없음(`type`이 기본 import에 적용되는지 전체 선언에 적용되는지 모호해지므로) → 대신 import 선언을 둘로 분할하거나 `default`를 명명된 바인딩으로 사용

```ts
import type fs, { BigIntOptions } from "fs";
//          ^^^^^^^^^^^^^^^^^^^^^
// 오류: 타입 전용 import는 기본 import 또는 명명된 바인딩을 지정할 수 있지만, 둘 다는 안 됩니다.

import type { default as fs, BigIntOptions } from "fs"; // Ok
```

#### `import()` 타입

- TypeScript는 import 선언 없이 모듈의 타입을 참조할 수 있게, JavaScript의 동적 `import`와 유사한 타입 구문을 제공

```ts
// 내보낸 타입에 접근:
type WriteFileOptions = import("fs").WriteFileOptions;
// 내보낸 값의 타입에 접근:
type WriteFileFunction = typeof import("fs").writeFile;
```

- 타입을 다른 방식으로 가져올 수 없는 JavaScript 파일의 JSDoc 주석에서 특히 유용

```ts
/** @type {import("webpack").Configuration} */
module.exports = {
  // ...
}
```

#### `export =`와 `import = require()`

- CommonJS 모듈 내보내기 시 TypeScript 파일은 `module.exports = ...`와 `const mod = require("...")`에 직접 대응하는 구문을 사용 가능

```ts
// @Filename: main.ts
import fs = require("fs");
export = fs.readFileSync("...");

// @Filename: main.js
"use strict";
const fs = require("fs");
module.exports = fs.readFileSync("...");
```

- 이 구문이 JavaScript 대응물 대신 쓰인 이유: 변수 선언·속성 할당은 TypeScript 타입을 참조할 수 없지만, 이 특별한 TypeScript 구문은 참조 가능

```ts
// @Filename: a.ts
interface Options { /* ... */ }
module.exports = Options; // 오류: 'Options'는 타입만 참조하지만, 여기서 값으로 사용되고 있습니다.
export = Options; // Ok

// @Filename: b.ts
const Options = require("./a");
const options: Options = { /* ... */ }; // 오류: 'Options'는 값을 참조하지만, 여기서 타입으로 사용되고 있습니다.

// @Filename: c.ts
import Options = require("./a");
const options: Options = { /* ... */ }; // Ok
```

#### 앰비언트 모듈

- TypeScript는 런타임에 존재하지만 해당 파일이 없는 모듈을 선언할 스크립트(비모듈) 파일용 구문을 지원
- 이 _앰비언트 모듈_은 보통 Node.js의 `"fs"`·`"path"`처럼 런타임이 제공하는 모듈을 나타냄

```ts
declare module "path" {
  export function normalize(p: string): string;
  export function join(...paths: any[]): string;
  export var sep: string;
}
```

- 앰비언트 모듈이 TypeScript 프로그램에 로드되면 TypeScript는 다른 파일에서 선언된 모듈의 import를 인식

```ts
// 👇 앰비언트 모듈이 로드되었는지 확인 -
//    path.d.ts가 프로젝트 tsconfig.json에 어떻게든
//    포함되어 있다면 불필요할 수 있습니다.
/// <reference path="path.d.ts" />

import { normalize, join } from "path";
```

- _패턴_ 앰비언트 모듈은 이름에 `*` 와일드카드 하나를 포함해 import 경로의 0개 이상 문자와 일치 → 커스텀 로더가 제공하는 모듈 선언에 유용

```ts
declare module "*.html" {
  const content: string;
  export default content;
}
```

### `module` 컴파일러 옵션

- 이 섹션은 각 `module` 옵션 값의 상세를 다룸. 배경은 [_모듈 출력 형식_](/docs/handbook/modules/theory.html#the-module-output-format) 이론 섹션 참고
- 간단히: `module` 옵션은 역사적으로 출력 JavaScript 파일의 모듈 형식만 제어했음
  - 최근의 `node16`·`node18`·`nodenext` 값은 어떤 모듈 형식이 지원되는지, 각 파일의 형식이 어떻게 결정되는지, 다른 형식들이 어떻게 상호 운용되는지까지 포함해 Node.js 모듈 시스템 전반을 설명

#### `node16`, `node18`, `node20`, `nodenext`

- Node.js는 CommonJS·ECMAScript 모듈을 모두 지원 → 각 형식의 조건과 상호 운용 방식에 특정 규칙이 있음
- `node16`·`node18`·`nodenext`는 Node.js의 이중 형식 모듈 시스템 전체 범위를 설명하며 **CommonJS 또는 ESM 형식으로 파일을 출력**
  - 다른 모든 `module` 옵션과 다른 점: 그 옵션들은 런타임과 무관하게 모든 출력 파일을 단일 형식으로 강제하고, 출력이 런타임에 유효한지 확인은 사용자에게 맡김

> 흔한 오해: `node16`-`nodenext`는 ES 모듈만 출력한다는 것 → 실제로는 ES 모듈을 _지원_하는 Node.js 버전을 설명할 뿐, ES 모듈을 _사용_하는 프로젝트만을 위한 것이 아님. ESM·CommonJS 출력 모두 각 파일의 [감지된 모듈 형식](#모듈-형식-감지)에 따라 지원됨. Node.js의 이중 모듈 시스템 복잡성을 반영하는 유일한 `module` 옵션이므로, ES 모듈 사용 여부와 무관하게 Node.js v12 이상에서 실행할 모든 앱·라이브러리에 대한 **유일한 올바른 `module` 옵션**

##### 모듈 형식 감지

- `.mts`/`.mjs`/`.d.mts` 파일 = 항상 ES 모듈
- `.cts`/`.cjs`/`.d.cts` 파일 = 항상 CommonJS 모듈
- `.ts`/`.tsx`/`.js`/`.jsx`/`.d.ts` 파일 = 가장 가까운 상위 package.json에 `"type": "module"`이 있으면 ES 모듈, 없으면 CommonJS 모듈

##### 상호 운용성 규칙

- **ES 모듈이 CommonJS 모듈을 참조할 때:**
  - CommonJS 모듈의 `module.exports`는 ES 모듈의 기본 import로 사용 가능
  - `module.exports`의 속성(`default` 제외)이 명명된 import로 쓰이는지는 상황에 따라 다름 → Node.js는 [정적 분석](https://github.com/nodejs/cjs-module-lexer)으로 이를 가능하게 하려 시도. TypeScript는 선언 파일에서 이 정적 분석의 성공 여부를 알 수 없어 낙관적으로 성공을 가정
- **CommonJS 모듈이 ES 모듈을 참조할 때:**
  - `node16`·`node18`에서는 `require`가 ES 모듈을 참조 불가
  - `nodenext`에서는 Node.js v22.12.0 이상의 동작을 반영해 `require`로 ES 모듈 참조 가능
  - 동적 `import()` 호출은 항상 ES 모듈을 가져올 수 있음

#### `preserve`

- `--module preserve`(TypeScript 5.4에서 [추가](https://www.typescriptlang.org/docs/handbook/release-notes/typescript-5-4.html#support-for-require-calls-in---moduleresolution-bundler-and---module-preserve))에서는 입력 파일의 ECMAScript import·export가 출력에서 그대로 보존되고, CommonJS 스타일 `import x = require("...")`·`export = ...`는 CommonJS `require`·`module.exports`로 출력됨
  - 즉 각 import·export 문의 형식이 전체 컴파일(또는 파일)에서 단일 형식으로 강제되지 않고 그대로 유지됨
- 같은 파일에서 import와 require를 섞어야 하는 경우는 드물지만, 이 모드는 최신 번들러 대부분과 Bun 런타임의 기능을 가장 잘 반영

##### 예시

```ts
// @Filename: main.ts
import x, { y, z } from "mod";
import mod = require("mod");
const dynamic = import("mod");

export const e1 = 0;
export default "default export";
```

```js
// @Filename: main.js
import x, { y, z } from "mod";
const mod = require("mod");
const dynamic = import("mod");

export const e1 = 0;
export default "default export";
```

#### `es2015`, `es2020`, `es2022`, `esnext`

##### 요약

- 번들러·Bun·tsx용으로는 `esnext`와 `--moduleResolution bundler`를 함께 사용 권장
- Node.js에는 사용 금지 → Node.js용 ES 모듈 출력은 package.json `"type": "module"`과 함께 `node16`·`node18`·`nodenext` 사용
- 비선언 파일에서는 `import mod = require("mod")` 불허
- `es2020`은 `import.meta` 속성 지원 추가
- `es2022`는 최상위 `await` 지원 추가
- `esnext`는 ECMAScript 모듈의 Stage 3 제안 지원을 포함할 수 있는 이동 목표
- 출력 파일은 ES 모듈이지만 의존성은 어떤 형식이든 가능

#### `commonjs`

##### 요약

- 사용 비권장 → Node.js용 CommonJS 모듈 출력은 `node16`·`node18`·`nodenext` 사용
- 출력 파일은 CommonJS 모듈이지만 의존성은 어떤 형식이든 가능
- 동적 `import()`는 `require()` 호출의 Promise로 변환됨
- `esModuleInterop`은 기본·네임스페이스 import의 출력 코드에 영향

### `moduleResolution` 컴파일러 옵션

- 이 섹션은 여러 `moduleResolution` 모드가 공유하는 기능·프로세스를 설명한 뒤 각 모드의 세부를 정리
- 배경 정보는 [_모듈 해결_](/docs/handbook/modules/theory.html#module-resolution) 이론 섹션 참고
- 간단히: `moduleResolution`은 TypeScript가 `import`/`export`/`require` 문의 _모듈 지정자_(문자열 리터럴)를 디스크 파일로 해결하는 방법을 제어 → 대상 런타임·번들러가 쓰는 모듈 해결기와 일치하게 설정해야 함

#### 공통 기능 및 프로세스

##### 파일 확장자 대체

- TypeScript는 항상 타입 정보를 제공할 파일로 내부적으로 해결하려 함 → 런타임·번들러는 같은 경로로 JavaScript 구현 파일을 해결할 수 있음
- 지정된 `moduleResolution` 알고리즘에서 런타임·번들러의 JavaScript 파일 조회를 트리거하는 모든 모듈 지정자에 대해, TypeScript는 먼저 같은 이름과 유사 확장자의 TypeScript 구현 파일이나 타입 선언 파일을 찾으려 시도

- 런타임 조회 `/mod.js` → TypeScript 조회 순서: `/mod.ts` → `/mod.tsx` → `/mod.d.ts` → `/mod.js` → `./mod.jsx`
- 런타임 조회 `/mod.mjs` → TypeScript 조회 순서: `/mod.mts` → `/mod.d.mts` → `/mod.mjs`
- 런타임 조회 `/mod.cjs` → TypeScript 조회 순서: `/mod.cts` → `/mod.d.cts` → `/mod.cjs`

##### 상대 파일 경로 해결

- TypeScript의 모든 `moduleResolution` 알고리즘은 파일 확장자를 포함한 상대 경로로 모듈을 참조하는 것을 지원(위 [규칙에 따라](#파일-확장자-대체) 대체됨)

```ts
// @Filename: a.ts
export {};

// @Filename: b.ts
import {} from "./a.js"; // ✅ 모든 `moduleResolution`에서 작동
```

##### 확장자 없는 상대 경로

- 경우에 따라 런타임·번들러는 상대 경로의 `.js` 확장자를 생략 가능 → TypeScript는 `moduleResolution` 설정·컨텍스트가 이를 지원한다고 나타내는 곳에서 이 동작을 지원

```ts
// @Filename: a.ts
export {};

// @Filename: b.ts
import {} from "./a";
```

##### 디렉토리 모듈 (인덱스 파일 해결)

- 경우에 따라 파일 대신 디렉토리를 모듈로 참조 가능 → 가장 흔한 경우는 런타임·번들러가 디렉토리에서 `index.js`를 찾는 것. TypeScript도 이를 지원하는 설정·컨텍스트에서 동일하게 지원

```ts
// @Filename: dir/index.ts
export {};

// @Filename: b.ts
import {} from "./dir";
```

##### `paths`

- `paths` 컴파일러 옵션은 베어 지정자에 대한 컴파일러의 모듈 해결을 재정의하는 방법을 제공
- 원래는 AMD 모듈 로더용(ESM·번들러가 보편화되기 전 브라우저에서 모듈을 실행하는 수단)으로 설계되었지만, TypeScript가 모델링하지 않는 해결 기능을 런타임·번들러가 지원할 때 오늘날에도 여전히 사용됨

###### `paths`는 emit에 영향을 미치지 않음

- `paths` 옵션은 TypeScript가 출력하는 코드의 import 경로를 변경하지 _않음_ → TypeScript에서는 작동하는 것처럼 보이지만 런타임에 충돌하는 경로 별칭을 만들기 매우 쉬움

##### `node_modules` 패키지 조회

- Node.js는 상대·절대 경로도 URL도 아닌 모듈 지정자를 `node_modules` 하위 디렉토리 패키지 참조로 취급
- 번들러도 Node.js와 같은(때로는 동일한) 종속성 관리 시스템을 쓸 수 있도록 이 동작을 채택
- `classic`을 제외한 TypeScript의 모든 `moduleResolution` 옵션은 `node_modules` 조회를 지원

##### package.json `"exports"`

- `moduleResolution`이 `node16`·`nodenext`·`bundler`이고 `resolvePackageJsonExports`가 비활성화되지 않았다면, [베어 지정자 `node_modules` 패키지 조회](#node_modules-패키지-조회)로 트리거된 패키지 디렉토리 해결 시 Node.js의 [package.json `"exports"` 사양](https://nodejs.org/api/packages.html#packages_package_entry_points)을 따름
- `"exports"`를 통한 모듈 지정자 → 파일 경로 해결 구현은 Node.js를 정확히 따름. 다만 파일 경로가 해결된 후에도 TypeScript는 타입을 찾기 위해 여러 파일 확장자를 [시도](#파일-확장자-대체)

#### `node16`, `nodenext`

- 이 모드들은 Node.js v12 이상의 모듈 해결 동작을 반영
  - `node16`·`nodenext`는 현재 동일하지만, Node.js가 향후 모듈 시스템에 중요한 변경을 하면 `node16`은 고정되고 `nodenext`가 새 동작을 반영하도록 업데이트됨
- Node.js에서 ECMAScript import의 해결 알고리즘은 CommonJS `require` 알고리즘과 크게 다름
  - 각 모듈 지정자 해결 시, 구문과 가져오는 파일의 [모듈 형식](#모듈-형식-감지)을 먼저 써서 출력 JavaScript에서 `import`가 될지 `require`가 될지 결정

##### 지원되는 기능

- [`paths`](#paths): import 지원 · require 지원
- [`baseUrl`](#baseurl): import 지원 · require 지원
- [`node_modules` 패키지 조회](#node_modules-패키지-조회): import 지원 · require 지원
- [package.json `"exports"`](#packagejson-exports): import는 `types`·`node`·`import` 일치 지원, require는 `types`·`node`·`require` 일치 지원
- [package.json `"imports"` 및 자기 이름 import](#packagejson-imports-및-자기-이름-imports): import는 `types`·`node`·`import` 일치 지원, require는 `types`·`node`·`require` 일치 지원
- [전체 상대 경로](#상대-파일-경로-해결): import 지원 · require 지원
- [확장자 없는 상대 경로](#확장자-없는-상대-경로): import 불가 · require 지원
- [디렉토리 모듈](#디렉토리-모듈-인덱스-파일-해결): import 불가 · require 지원

#### `bundler`

- `--moduleResolution bundler`는 대부분의 JavaScript 번들러에 공통된 모듈 해결 동작을 모델링
- 즉 Node.js CommonJS `require` 해결과 전통적으로 연관된 동작([`node_modules` 조회](#node_modules-패키지-조회), [디렉토리 모듈](#디렉토리-모듈-인덱스-파일-해결), [확장자 없는 경로](#확장자-없는-상대-경로))을 모두 지원하면서, [package.json `"exports"`](#packagejson-exports)·[`"imports"`](#packagejson-imports-및-자기-이름-imports) 같은 최신 Node.js 해결 기능도 지원

##### 지원되는 기능

- [`paths`](#paths): 지원
- [`baseUrl`](#baseurl): 지원
- [`node_modules` 패키지 조회](#node_modules-패키지-조회): 지원
- [package.json `"exports"`](#packagejson-exports): 지원, 구문에 따라 `types`·`import`/`require` 일치
- [package.json `"imports"` 및 자기 이름 import](#packagejson-imports-및-자기-이름-imports): 지원, 구문에 따라 `types`·`import`/`require` 일치
- [전체 상대 경로](#상대-파일-경로-해결): 지원
- [확장자 없는 상대 경로](#확장자-없는-상대-경로): 지원
- [디렉토리 모듈](#디렉토리-모듈-인덱스-파일-해결): 지원

#### `node10` (이전에 `node`로 알려짐)

- `--moduleResolution node`는 TypeScript 5.0에서 `node10`으로 개명(역호환을 위해 `node`는 별칭으로 유지)
- v12 이전 Node.js의 CommonJS 모듈 해결 알고리즘을 반영 → 사용 금지

#### `classic`

- `classic` 사용 금지

---

## 네임스페이스 (Namespaces)

> **원문:** https://www.typescriptlang.org/docs/handbook/namespaces.html

> **용어에 대한 참고:**
> TypeScript 1.5에서 명명법이 변경되었다는 것을 주목하는 것이 중요합니다.
> "내부 모듈"은 이제 "네임스페이스"입니다.
> "외부 모듈"은 이제 단순히 "모듈"로, [ECMAScript 2015](https://www.ecma-international.org/ecma-262/6.0/)의 용어에 맞춰졌습니다 (즉, `module X {`는 이제 선호되는 `namespace X {`와 동등합니다).

- 이 글은 TypeScript에서 네임스페이스(이전 "내부 모듈")로 코드를 구성하는 방법을 설명
- 위 용어 참고대로 "내부 모듈"은 이제 "네임스페이스"
- 내부 모듈 선언 시 쓰였던 `module` 키워드는 `namespace` 키워드로 대체해서 사용해야 함 → 유사한 용어로 새 사용자를 혼동시키는 것 방지

### 첫 번째 단계

- 이 페이지 전체 예제로 쓸 프로그램: 간단한 문자열 유효성 검사기 모음
  - 웹페이지 폼 입력 확인, 외부 데이터 파일 형식 확인 같은 용도로 작성할 만한 것

### 단일 파일의 유효성 검사기

```ts
interface StringValidator {
  isAcceptable(s: string): boolean;
}

let lettersRegexp = /^[A-Za-z]+$/;
let numberRegexp = /^[0-9]+$/;

class LettersOnlyValidator implements StringValidator {
  isAcceptable(s: string) {
    return lettersRegexp.test(s);
  }
}

class ZipCodeValidator implements StringValidator {
  isAcceptable(s: string) {
    return s.length === 5 && numberRegexp.test(s);
  }
}

// 시도할 몇 가지 샘플
let strings = ["Hello", "98052", "101"];

// 사용할 유효성 검사기
let validators: { [s: string]: StringValidator } = {};
validators["ZIP code"] = new ZipCodeValidator();
validators["Letters only"] = new LettersOnlyValidator();

// 각 문자열이 각 유효성 검사기를 통과했는지 표시
for (let s of strings) {
  for (let name in validators) {
    let isMatch = validators[name].isAcceptable(s);
    console.log(`'${s}' ${isMatch ? "matches" : "does not match"} '${name}'.`);
  }
}
```

### 네임스페이싱

- 유효성 검사기가 늘어나면 타입 추적·이름 충돌 방지를 위한 조직 체계 필요
- 여러 이름을 전역 네임스페이스에 두는 대신 객체를 네임스페이스로 래핑
- 예제: 모든 유효성 검사기 엔티티를 `Validation` 네임스페이스로 이동
  - 인터페이스·클래스를 네임스페이스 밖에서 보이게 하려면 `export`로 시작
  - `lettersRegexp`·`numberRegexp`는 구현 세부 사항이므로 내보내지 않아 외부에 보이지 않음
  - 파일 하단 테스트 코드에서는 네임스페이스 외부 사용 시 타입 이름을 한정해야 함(예: `Validation.LettersOnlyValidator`)

### 네임스페이스 유효성 검사기

```ts
namespace Validation {
  export interface StringValidator {
    isAcceptable(s: string): boolean;
  }

  const lettersRegexp = /^[A-Za-z]+$/;
  const numberRegexp = /^[0-9]+$/;

  export class LettersOnlyValidator implements StringValidator {
    isAcceptable(s: string) {
      return lettersRegexp.test(s);
    }
  }

  export class ZipCodeValidator implements StringValidator {
    isAcceptable(s: string) {
      return s.length === 5 && numberRegexp.test(s);
    }
  }
}

// 시도할 몇 가지 샘플
let strings = ["Hello", "98052", "101"];

// 사용할 유효성 검사기
let validators: { [s: string]: Validation.StringValidator } = {};
validators["ZIP code"] = new Validation.ZipCodeValidator();
validators["Letters only"] = new Validation.LettersOnlyValidator();

// 각 문자열이 각 유효성 검사기를 통과했는지 표시
for (let s of strings) {
  for (let name in validators) {
    console.log(
      `"${s}" - ${
        validators[name].isAcceptable(s) ? "matches" : "does not match"
      } ${name}`
    );
  }
}
```

### 여러 파일로 분할

- 애플리케이션이 커지면 코드를 여러 파일로 분할해 유지보수를 쉽게 하고 싶어짐

### 다중 파일 네임스페이스

- `Validation` 네임스페이스를 여러 파일로 분할하는 예
- 파일이 분리되어도 각각 같은 네임스페이스에 기여 가능 → 한 곳에서 정의된 것처럼 소비 가능
- 파일 간 종속성이 있으므로 참조 태그로 관계를 컴파일러에 알림
- 테스트 코드는 그 외에는 변경 없음

###### Validation.ts

```ts
namespace Validation {
  export interface StringValidator {
    isAcceptable(s: string): boolean;
  }
}
```

###### LettersOnlyValidator.ts

```ts
/// <reference path="Validation.ts" />
namespace Validation {
  const lettersRegexp = /^[A-Za-z]+$/;
  export class LettersOnlyValidator implements StringValidator {
    isAcceptable(s: string) {
      return lettersRegexp.test(s);
    }
  }
}
```

###### ZipCodeValidator.ts

```ts
/// <reference path="Validation.ts" />
namespace Validation {
  const numberRegexp = /^[0-9]+$/;
  export class ZipCodeValidator implements StringValidator {
    isAcceptable(s: string) {
      return s.length === 5 && numberRegexp.test(s);
    }
  }
}
```

###### Test.ts

```ts
/// <reference path="Validation.ts" />
/// <reference path="LettersOnlyValidator.ts" />
/// <reference path="ZipCodeValidator.ts" />

// 시도할 몇 가지 샘플
let strings = ["Hello", "98052", "101"];

// 사용할 유효성 검사기
let validators: { [s: string]: Validation.StringValidator } = {};
validators["ZIP code"] = new Validation.ZipCodeValidator();
validators["Letters only"] = new Validation.LettersOnlyValidator();

// 각 문자열이 각 유효성 검사기를 통과했는지 표시
for (let s of strings) {
  for (let name in validators) {
    console.log(
      `"${s}" - ${
        validators[name].isAcceptable(s) ? "matches" : "does not match"
      } ${name}`
    );
  }
}
```

- 여러 파일이 관련되면 모든 컴파일된 코드가 로드되도록 해야 함 → 방법 두 가지
- 방법 1: [`outFile`](/tsconfig#outFile) 옵션으로 모든 입력 파일을 연결된 단일 JavaScript 출력 파일로 컴파일

```Shell
tsc --outFile sample.js Test.ts
```

- 컴파일러는 파일의 참조 태그를 기반으로 출력 파일을 자동 정렬. 각 파일을 개별 지정도 가능

```Shell
tsc --outFile sample.js Validation.ts LettersOnlyValidator.ts ZipCodeValidator.ts Test.ts
```

- 방법 2: 파일별 컴파일(기본값)로 각 입력 파일당 JavaScript 파일 하나씩 방출
  - 여러 JS 파일이 생성되면 웹페이지에서 `<script>` 태그로 적절한 순서로 각 파일을 로드해야 함. 예

###### MyTestPage.html (발췌)

```html
<script src="Validation.js" type="text/javascript" />
<script src="LettersOnlyValidator.js" type="text/javascript" />
<script src="ZipCodeValidator.js" type="text/javascript" />
<script src="Test.js" type="text/javascript" />
```

### 별칭

- 네임스페이스 작업을 단순화하는 또 다른 방법: `import q = x.y.z`로 자주 쓰는 객체에 짧은 이름 부여
- 모듈 로드용 `import x = require("name")` 구문과는 다름 → 이 구문은 단순히 지정된 심볼의 별칭을 만듦
- 모듈 가져오기에서 생성된 객체를 포함해 모든 식별자에 이런 종류의 가져오기(보통 "별칭"이라 부름) 사용 가능

```ts
namespace Shapes {
  export namespace Polygons {
    export class Triangle {}
    export class Square {}
  }
}

import polygons = Shapes.Polygons;
let sq = new polygons.Square(); // 'new Shapes.Polygons.Square()'와 동일
```

- `require` 키워드는 쓰지 않음 → 대신 가져오는 심볼의 정규화된 이름에서 직접 할당
  - `var`와 유사하지만 가져온 심볼의 타입·네임스페이스 의미에서도 작동
  - 값의 경우 `import`는 원본 심볼과 구별되는 참조라서, 별칭 `var`의 변경이 원본 변수에 반영되지 않음

### 다른 JavaScript 라이브러리와 작업하기

- TypeScript로 작성되지 않은 라이브러리의 형태를 설명하려면 라이브러리가 노출하는 API를 선언해야 함
- 대부분의 JavaScript 라이브러리는 최상위 객체 몇 개만 노출 → 네임스페이스가 이를 나타내는 좋은 방법
- 구현을 정의하지 않는 선언 = "앰비언트" → 보통 `.d.ts` 파일에 정의(C/C++의 `.h` 파일과 유사)

### 앰비언트 네임스페이스

- 인기 라이브러리 D3는 `d3`라는 전역 객체에 기능을 정의
- 이 라이브러리는 (모듈 로더 대신) `<script>` 태그로 로드되므로 선언에서 네임스페이스로 형태를 정의
- TypeScript 컴파일러가 이 형태를 보게 하려면 앰비언트 네임스페이스 선언 사용. 예

###### D3.d.ts (단순화된 발췌)

```ts
declare namespace D3 {
  export interface Selectors {
    select: {
      (selector: string): Selection;
      (element: EventTarget): Selection;
    };
  }

  export interface Event {
    x: number;
    y: number;
  }

  export interface Base extends Selectors {
    event: Event;
  }
}

declare var d3: D3.Base;
```

---

## 네임스페이스와 모듈 (Namespaces and Modules)

> **원문:** https://www.typescriptlang.org/docs/handbook/namespaces-and-modules.html

- 이 글은 TypeScript에서 모듈·네임스페이스로 코드를 구성하는 방법과 고급 주제, 흔한 함정을 다룸
- ES 모듈 상세는 [모듈](/docs/handbook/modules.html) 문서, TypeScript 네임스페이스 상세는 [네임스페이스](/docs/handbook/namespaces.html) 문서 참고

- 참고: TypeScript의 _매우_ 오래된 버전에서는 네임스페이스를 '내부 모듈'이라 불렀음(JavaScript 모듈 시스템보다 앞선 명칭)

### 모듈 사용하기

- 모듈은 코드와 선언을 모두 포함 가능
- 모듈 로더(CommonJs/Require.js 등)나 ES 모듈 지원 런타임에 대한 종속성을 가짐
- 더 나은 코드 재사용, 더 강력한 격리, 더 나은 번들링 도구 지원을 제공
- Node.js 애플리케이션에서는 모듈이 기본값이며 **현대 코드에서는 네임스페이스보다 모듈 권장**
- ECMAScript 2015부터 모듈은 언어의 기본 부분 → 모든 호환 엔진에서 지원되어야 함 → 새 프로젝트의 권장 구성 메커니즘

### 네임스페이스 사용하기

- 네임스페이스는 코드를 구성하는 TypeScript 전용 방법
- 단순히 전역 네임스페이스의 명명된 JavaScript 객체 → 매우 간단한 구조
- 모듈과 달리 여러 파일에 걸칠 수 있고 [`outFile`](/tsconfig#outFile)로 연결 가능
- HTML `<script>` 태그로 모든 종속성을 포함하는 웹 애플리케이션 구조화에 좋은 방법일 수 있음
- 다만 모든 전역 네임스페이스 오염과 마찬가지로 대규모 애플리케이션에서는 컴포넌트 종속성 식별이 어려워질 수 있음

### 네임스페이스와 모듈의 함정

- 이 섹션은 네임스페이스·모듈 사용 시 흔한 함정과 회피 방법을 설명

#### 모듈을 `/// <reference>`하기

- 흔한 실수: `import` 문 대신 `/// <reference ... />` 구문으로 모듈 파일을 참조
- 이 구별의 이해를 위해서는, 컴파일러가 `import` 경로(`import x from "...";`, `import x = require("...");`의 `...`)로 모듈 타입 정보를 찾는 방식부터 알아야 함
- 컴파일러는 해당 경로로 `.ts`, `.tsx`, 다음 `.d.ts`를 찾음 → 특정 파일을 못 찾으면 _앰비언트 모듈 선언_을 찾음(이들은 `.d.ts` 파일에 선언되어야 함)

- `myModules.d.ts`

  ```ts
  // 모듈이 아닌 .d.ts 파일이나 .ts 파일에서:
  declare module "SomeModule" {
    export function fn(): string;
  }
  ```

- `myOtherModule.ts`

  ```ts
  /// <reference path="myModules.d.ts" />
  import * as m from "SomeModule";
  ```

- 참조 태그로 앰비언트 모듈 선언을 담은 선언 파일을 찾을 수 있음
- 여러 TypeScript 샘플이 쓰는 `node.d.ts` 파일도 이렇게 소비됨

#### 불필요한 네임스페이싱

- 네임스페이스에서 모듈로 프로그램을 변환할 때 다음과 같은 파일이 되기 쉬움

- `shapes.ts`

  ```ts
  export namespace Shapes {
    export class Triangle {
      /* ... */
    }
    export class Square {
      /* ... */
    }
  }
  ```

- 최상위 네임스페이스 `Shapes`가 아무 이유 없이 `Triangle`·`Square`를 래핑 → 모듈 소비자에게 혼란스럽고 성가심

- `shapeConsumer.ts`

  ```ts
  import * as shapes from "./shapes";
  let t = new shapes.Shapes.Triangle(); // shapes.Shapes?
  ```

- TypeScript 모듈의 핵심 기능: 서로 다른 두 모듈은 같은 스코프에 이름을 기여하지 않음
  - 모듈 소비자가 어떤 이름을 할당할지 결정하므로 네임스페이스로 내보낸 심볼을 능동적으로 래핑할 필요 없음
- 모듈 내용을 네임스페이스화하지 말아야 하는 이유 요약: 네임스페이싱의 목적은 논리적 그룹화와 이름 충돌 방지인데, 모듈 파일 자체가 이미 논리적 그룹화이고 최상위 이름은 가져오는 코드가 정의하므로 추가 모듈 레이어가 불필요
- 수정된 예제

- `shapes.ts`

  ```ts
  export class Triangle {
    /* ... */
  }
  export class Square {
    /* ... */
  }
  ```

- `shapeConsumer.ts`

  ```ts
  import * as shapes from "./shapes";
  let t = new shapes.Triangle();
  ```

#### 모듈의 트레이드오프

- JS 파일과 모듈 사이에 일대일 대응이 있듯, TypeScript는 모듈 소스 파일과 방출된 JS 파일 사이에 일대일 대응이 있음
- 이 때문에 대상 모듈 시스템에 따라 여러 모듈 소스 파일을 연결할 수 없는 경우가 생김
  - 예: `commonjs`·`umd` 대상 시 [`outFile`](/tsconfig#outFile) 옵션 사용 불가. 단 TypeScript 1.8 이상에서 `amd`·`system` 대상이면 [`outFile` 사용 가능](./release-notes/typescript-1-8.html#concatenate-amd-and-system-modules-with---outfile)

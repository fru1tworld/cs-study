# TypeScript 함수, 객체 타입, 클래스

## 함수와 객체 타입

## 함수에 대해 더 알아보기

> **원문:** https://www.typescriptlang.org/docs/handbook/2/functions.html

함수는 모든 애플리케이션의 기본 구성 요소입니다. 로컬 함수든, 다른 모듈에서 가져온 함수든, 클래스의 메서드든 상관없습니다.
함수도 값이며, 다른 값과 마찬가지로 TypeScript는 함수를 호출하는 방법을 설명하는 다양한 방식을 제공합니다.
함수를 설명하는 타입을 작성하는 방법을 배워봅시다.

### 함수 타입 표현식

함수를 설명하는 가장 간단한 방법은 _함수 타입 표현식_입니다.
이러한 타입은 화살표 함수와 문법적으로 유사합니다:

```ts twoslash
function greeter(fn: (a: string) => void) {
  fn("Hello, World");
}

function printToConsole(s: string) {
  console.log(s);
}

greeter(printToConsole);
```

`(a: string) => void` 구문은 "`a`라는 이름의 `string` 타입 매개변수가 하나 있고, 반환 값이 없는 함수"를 의미합니다.
함수 선언과 마찬가지로, 매개변수 타입이 지정되지 않으면 암시적으로 `any`입니다.

> 매개변수 이름이 **필수**라는 점에 주의하세요. 함수 타입 `(string) => void`는 "`any` 타입의 `string`이라는 이름의 매개변수가 있는 함수"를 의미합니다!

물론, 타입 별칭을 사용하여 함수 타입에 이름을 지정할 수 있습니다:

```ts twoslash
type GreetFunction = (a: string) => void;
function greeter(fn: GreetFunction) {
  // ...
}
```

### 호출 시그니처

JavaScript에서 함수는 호출 가능할 뿐만 아니라 속성을 가질 수 있습니다.
그러나 함수 타입 표현식 구문은 속성 선언을 허용하지 않습니다.
속성을 가진 호출 가능한 것을 설명하려면, 객체 타입에서 _호출 시그니처_를 작성할 수 있습니다:

```ts twoslash
type DescribableFunction = {
  description: string;
  (someArg: number): boolean;
};
function doSomething(fn: DescribableFunction) {
  console.log(fn.description + " returned " + fn(6));
}

function myFunc(someArg: number) {
  return someArg > 3;
}
myFunc.description = "default description";

doSomething(myFunc);
```

함수 타입 표현식과 비교하여 구문이 약간 다르다는 점에 주의하세요 - 매개변수 목록과 반환 타입 사이에 `=>`가 아닌 `:`를 사용합니다.

### 생성 시그니처

JavaScript 함수는 `new` 연산자로도 호출할 수 있습니다.
TypeScript는 이들을 보통 새 객체를 생성하기 때문에 _생성자_라고 합니다.
호출 시그니처 앞에 `new` 키워드를 추가하여 _생성 시그니처_를 작성할 수 있습니다:

```ts twoslash
type SomeObject = any;
// ---cut---
type SomeConstructor = {
  new (s: string): SomeObject;
};
function fn(ctor: SomeConstructor) {
  return new ctor("hello");
}
```

JavaScript의 `Date` 객체와 같은 일부 객체는 `new`와 함께 또는 없이 호출할 수 있습니다.
같은 타입에서 호출과 생성 시그니처를 임의로 결합할 수 있습니다:

```ts twoslash
interface CallOrConstruct {
  (n?: number): string;
  new (s: string): Date;
}

function fn(ctor: CallOrConstruct) {
  // `number` 타입의 인수를 `ctor`에 전달하면
  // `CallOrConstruct` 인터페이스의 첫 번째 정의와 일치합니다.
  console.log(ctor(10));
              // ^?

  // 마찬가지로, `string` 타입의 인수를 `ctor`에 전달하면
  // 인터페이스의 두 번째 정의와 일치합니다.
  console.log(new ctor("10"));
                  // ^?
}

fn(Date);
```

### 제네릭 함수

입력의 타입이 출력의 타입과 관련되거나, 두 입력의 타입이 어떤 방식으로 관련된 함수를 작성하는 것이 일반적입니다.
배열의 첫 번째 요소를 반환하는 함수를 잠시 생각해 봅시다:

```ts twoslash
function firstElement(arr: any[]) {
  return arr[0];
}
```

이 함수는 작업을 수행하지만, 불행히도 반환 타입이 `any`입니다.
함수가 배열 요소의 타입을 반환하면 더 좋을 것입니다.

TypeScript에서 _제네릭_은 두 값 사이의 대응을 설명하고 싶을 때 사용됩니다.
함수 시그니처에서 _타입 매개변수_를 선언하여 이렇게 합니다:

```ts twoslash
function firstElement<Type>(arr: Type[]): Type | undefined {
  return arr[0];
}
```

타입 매개변수 `Type`을 이 함수에 추가하고 두 곳에서 사용함으로써, 함수의 입력(배열)과 출력(반환 값) 사이에 링크를 만들었습니다.
이제 호출할 때, 더 구체적인 타입이 나옵니다:

```ts twoslash
declare function firstElement<Type>(arr: Type[]): Type | undefined;
// ---cut---
// s는 타입 'string'
const s = firstElement(["a", "b", "c"]);
// n은 타입 'number'
const n = firstElement([1, 2, 3]);
// u는 타입 undefined
const u = firstElement([]);
```

#### 추론

이 샘플에서 `Type`을 지정할 필요가 없었다는 점에 주목하세요.
TypeScript가 타입을 _추론_(자동으로 선택)했습니다.

여러 타입 매개변수를 사용할 수도 있습니다.
예를 들어, `map`의 독립형 버전은 다음과 같습니다:

```ts twoslash
// prettier-ignore
function map<Input, Output>(arr: Input[], func: (arg: Input) => Output): Output[] {
  return arr.map(func);
}

// 매개변수 'n'은 타입 'string'
// 'parsed'는 타입 'number[]'
const parsed = map(["1", "2", "3"], (n) => parseInt(n));
```

이 예제에서 TypeScript는 `Input` 타입 매개변수(주어진 `string` 배열에서)와 함수 표현식의 반환 값(`number`)을 기반으로 `Output` 타입 매개변수를 모두 추론할 수 있었습니다.

#### 제약 조건

_어떤_ 종류의 값에서도 작동할 수 있는 제네릭 함수를 작성했습니다.
때때로 두 값을 관련시키고 싶지만, 특정 값의 하위 집합에서만 작동할 수 있습니다.
이 경우, 타입 매개변수가 받아들일 수 있는 종류의 타입을 제한하기 위해 _제약 조건_을 사용할 수 있습니다.

두 값 중 더 긴 것을 반환하는 함수를 작성해 봅시다.
이를 위해, 숫자인 `length` 속성이 필요합니다.
`extends` 절을 작성하여 타입 매개변수를 해당 타입으로 _제약_합니다:

```ts twoslash
// @errors: 2345 2322
function longest<Type extends { length: number }>(a: Type, b: Type) {
  if (a.length >= b.length) {
    return a;
  } else {
    return b;
  }
}

// longerArray는 타입 'number[]'
const longerArray = longest([1, 2], [1, 2, 3]);
// longerString은 타입 'alice' | 'bob'
const longerString = longest("alice", "bob");
// 오류! 숫자는 'length' 속성이 없음
const notOK = longest(10, 100);
```

이 예제에서 몇 가지 흥미로운 점이 있습니다.
TypeScript가 `longest`의 반환 타입을 _추론_하도록 허용했습니다.
반환 타입 추론은 제네릭 함수에서도 작동합니다.

`Type`을 `{ length: number }`로 제약했기 때문에, `a`와 `b` 매개변수의 `.length` 속성에 접근할 수 있었습니다.
타입 제약이 없었다면, 값이 length 속성이 없는 다른 타입일 수 있었기 때문에 해당 속성에 접근할 수 없었을 것입니다.

`longerArray`와 `longerString`의 타입은 인수를 기반으로 추론되었습니다.
제네릭은 같은 타입으로 두 개 이상의 값을 관련시키는 것입니다!

마지막으로, 예상대로 `longest(10, 100)` 호출은 `number` 타입에 `.length` 속성이 없기 때문에 거부됩니다.

#### 제약된 값으로 작업하기

제네릭 제약 조건으로 작업할 때 일반적인 오류가 있습니다:

```ts twoslash
// @errors: 2322
function minimumLength<Type extends { length: number }>(
  obj: Type,
  minimum: number
): Type {
  if (obj.length >= minimum) {
    return obj;
  } else {
    return { length: minimum };
  }
}
```

이 함수가 괜찮아 보일 수 있습니다 - `Type`이 `{ length: number }`로 제약되어 있고, 함수가 `Type`이나 해당 제약과 일치하는 값을 반환합니다.
문제는 함수가 전달된 것과 _같은_ 종류의 객체를 반환할 것을 약속하지, 단지 제약과 일치하는 _어떤_ 객체가 아니라는 것입니다.
이 코드가 합법적이라면, 확실히 작동하지 않을 코드를 작성할 수 있습니다:

```ts twoslash
declare function minimumLength<Type extends { length: number }>(
  obj: Type,
  minimum: number
): Type;
// ---cut---
// 'arr'은 값 { length: 6 }을 얻음
const arr = minimumLength([1, 2, 3], 6);
// 배열은 'slice' 메서드가 있지만
// 반환된 객체에는 없어서 여기서 충돌!
console.log(arr.slice(0));
```

#### 타입 인수 지정하기

TypeScript는 일반적으로 제네릭 호출에서 의도된 타입 인수를 추론할 수 있지만, 항상 그런 것은 아닙니다.
예를 들어, 두 배열을 결합하는 함수를 작성했다고 가정합니다:

```ts twoslash
function combine<Type>(arr1: Type[], arr2: Type[]): Type[] {
  return arr1.concat(arr2);
}
```

일반적으로 일치하지 않는 배열로 이 함수를 호출하면 오류가 됩니다:

```ts twoslash
// @errors: 2322
declare function combine<Type>(arr1: Type[], arr2: Type[]): Type[];
// ---cut---
const arr = combine([1, 2, 3], ["hello"]);
```

그러나 이렇게 하려고 의도했다면, 수동으로 `Type`을 지정할 수 있습니다:

```ts twoslash
declare function combine<Type>(arr1: Type[], arr2: Type[]): Type[];
// ---cut---
const arr = combine<string | number>([1, 2, 3], ["hello"]);
```

#### 좋은 제네릭 함수 작성 가이드라인

제네릭 함수를 작성하는 것은 재미있고, 타입 매개변수에 쉽게 빠져들 수 있습니다.
너무 많은 타입 매개변수를 갖거나 필요하지 않은 곳에 제약 조건을 사용하면 추론이 덜 성공적이 되어 함수 호출자를 좌절시킬 수 있습니다.

##### 타입 매개변수를 밀어내리기

유사해 보이는 두 가지 함수 작성 방법이 있습니다:

```ts twoslash
function firstElement1<Type>(arr: Type[]) {
  return arr[0];
}

function firstElement2<Type extends any[]>(arr: Type) {
  return arr[0];
}

// a: number (좋음)
const a = firstElement1([1, 2, 3]);
// b: any (나쁨)
const b = firstElement2([1, 2, 3]);
```

처음에는 동일해 보일 수 있지만, `firstElement1`이 이 함수를 작성하는 훨씬 더 좋은 방법입니다.
추론된 반환 타입은 `Type`이지만, `firstElement2`의 추론된 반환 타입은 `any`입니다. TypeScript가 호출 중에 요소를 해결하기 위해 "기다리기"보다 제약 타입을 사용하여 `arr[0]` 표현식을 해결해야 하기 때문입니다.

> **규칙**: 가능하면 제약하기보다 타입 매개변수 자체를 사용하세요

##### 더 적은 타입 매개변수 사용하기

유사한 또 다른 함수 쌍이 있습니다:

```ts twoslash
function filter1<Type>(arr: Type[], func: (arg: Type) => boolean): Type[] {
  return arr.filter(func);
}

function filter2<Type, Func extends (arg: Type) => boolean>(
  arr: Type[],
  func: Func
): Type[] {
  return arr.filter(func);
}
```

두 값을 _관련시키지 않는_ 타입 매개변수 `Func`를 만들었습니다.
이것은 항상 빨간 플래그입니다. 타입 인수를 지정하려는 호출자가 이유 없이 추가 타입 인수를 수동으로 지정해야 하기 때문입니다.
`Func`는 함수를 읽고 추론하기 더 어렵게 만드는 것 외에는 아무것도 하지 않습니다!

> **규칙**: 항상 가능한 한 적은 타입 매개변수를 사용하세요

##### 타입 매개변수는 두 번 나타나야 함

때때로 함수가 제네릭일 필요가 없다는 것을 잊습니다:

```ts twoslash
function greet<Str extends string>(s: Str) {
  console.log("Hello, " + s);
}

greet("world");
```

더 간단한 버전을 쉽게 작성할 수 있었습니다:

```ts twoslash
function greet(s: string) {
  console.log("Hello, " + s);
}
```

타입 매개변수는 _여러 값의 타입을 관련시키기_ 위한 것입니다.
타입 매개변수가 함수 시그니처에서 한 번만 사용되면, 아무것도 관련시키지 않습니다.
여기에는 추론된 반환 타입이 포함됩니다; 예를 들어, `Str`이 `greet`의 추론된 반환 타입의 일부라면, 인수와 반환 타입을 관련시키므로 작성된 코드에서 한 번만 나타나더라도 _두 번_ 사용된 것입니다.

> **규칙**: 타입 매개변수가 한 위치에만 나타나면, 정말로 필요한지 강력하게 재고하세요

### 선택적 매개변수

JavaScript에서 함수는 종종 가변적인 수의 인수를 받습니다.
예를 들어, `number`의 `toFixed` 메서드는 선택적인 자릿수를 받습니다:

```ts twoslash
function f(n: number) {
  console.log(n.toFixed()); // 0개의 인수
  console.log(n.toFixed(3)); // 1개의 인수
}
```

TypeScript에서 `?`로 매개변수를 _선택적_으로 표시하여 이를 모델링할 수 있습니다:

```ts twoslash
function f(x?: number) {
  // ...
}
f(); // OK
f(10); // OK
```

매개변수가 타입 `number`로 지정되었지만, `x` 매개변수는 실제로 `number | undefined` 타입을 가집니다. JavaScript에서 지정되지 않은 매개변수는 값 `undefined`를 얻기 때문입니다.

매개변수 _기본값_을 제공할 수도 있습니다:

```ts twoslash
function f(x = 10) {
  // ...
}
```

이제 `f`의 본문에서 `x`는 `number` 타입을 가집니다. `undefined` 인수는 `10`으로 대체되기 때문입니다.
매개변수가 선택적일 때, 호출자는 항상 `undefined`를 전달할 수 있습니다. 이것은 단순히 "누락된" 인수를 시뮬레이션합니다:

```ts twoslash
declare function f(x?: number): void;
// ---cut---
// 모두 OK
f();
f(10);
f(undefined);
```

#### 콜백의 선택적 매개변수

선택적 매개변수와 함수 타입 표현식에 대해 배우면, 콜백을 호출하는 함수를 작성할 때 다음과 같은 실수를 저지르기 매우 쉽습니다:

```ts twoslash
function myForEach(arr: any[], callback: (arg: any, index?: number) => void) {
  for (let i = 0; i < arr.length; i++) {
    callback(arr[i], i);
  }
}
```

`index?`를 선택적 매개변수로 작성할 때 일반적으로 의도하는 것은 이 두 호출이 모두 합법적이기를 원하는 것입니다:

```ts twoslash
// @errors: 2532 18048
declare function myForEach(
  arr: any[],
  callback: (arg: any, index?: number) => void
): void;
// ---cut---
myForEach([1, 2, 3], (a) => console.log(a));
myForEach([1, 2, 3], (a, i) => console.log(a, i));
```

이것이 _실제로_ 의미하는 것은 _`callback`이 하나의 인수로 호출될 수 있다_는 것입니다.
다시 말해, 함수 정의는 구현이 다음과 같을 수 있다고 말합니다:

```ts twoslash
// @errors: 2532 18048
function myForEach(arr: any[], callback: (arg: any, index?: number) => void) {
  for (let i = 0; i < arr.length; i++) {
    // 오늘은 인덱스를 제공하고 싶지 않음
    callback(arr[i]);
  }
}
```

그러면 TypeScript는 이 의미를 적용하고 실제로 가능하지 않은 오류를 발생시킵니다:

```ts twoslash
// @errors: 2532 18048
declare function myForEach(
  arr: any[],
  callback: (arg: any, index?: number) => void
): void;
// ---cut---
myForEach([1, 2, 3], (a, i) => {
  console.log(i.toFixed());
});
```

JavaScript에서 매개변수보다 더 많은 인수로 함수를 호출하면, 추가 인수는 단순히 무시됩니다.
TypeScript도 같은 방식으로 동작합니다.
(같은 타입의) 더 적은 매개변수를 가진 함수는 항상 더 많은 매개변수를 가진 함수의 자리를 차지할 수 있습니다.

> **규칙**: 콜백에 대한 함수 타입을 작성할 때, 해당 인수를 전달하지 _않고_ 함수를 _호출_하려는 의도가 아니라면 선택적 매개변수를 _절대_ 작성하지 마세요

### 함수 오버로드

일부 JavaScript 함수는 다양한 인수 수와 타입으로 호출할 수 있습니다.
예를 들어, 타임스탬프(하나의 인수)나 월/일/연도 지정(세 개의 인수)을 받아 `Date`를 생성하는 함수를 작성할 수 있습니다.

TypeScript에서 _오버로드 시그니처_를 작성하여 다른 방식으로 호출할 수 있는 함수를 지정할 수 있습니다.
이를 위해, 몇 개의 함수 시그니처(보통 두 개 이상)를 작성한 다음, 함수 본문을 작성합니다:

```ts twoslash
// @errors: 2575
function makeDate(timestamp: number): Date;
function makeDate(m: number, d: number, y: number): Date;
function makeDate(mOrTimestamp: number, d?: number, y?: number): Date {
  if (d !== undefined && y !== undefined) {
    return new Date(y, mOrTimestamp, d);
  } else {
    return new Date(mOrTimestamp);
  }
}
const d1 = makeDate(12345678);
const d2 = makeDate(5, 5, 5);
const d3 = makeDate(1, 3);
```

이 예제에서, 두 개의 오버로드를 작성했습니다: 하나는 하나의 인수를 받고, 다른 하나는 세 개의 인수를 받습니다.
이 처음 두 시그니처를 _오버로드 시그니처_라고 합니다.

그런 다음, 호환 가능한 시그니처로 함수 구현을 작성했습니다.
함수에는 _구현_ 시그니처가 있지만, 이 시그니처는 직접 호출할 수 없습니다.
필수 매개변수 뒤에 두 개의 선택적 매개변수가 있는 함수를 작성했지만, 두 개의 매개변수로 호출할 수 없습니다!

#### 오버로드 시그니처와 구현 시그니처

이것은 일반적인 혼란의 원인입니다.
종종 사람들은 다음과 같은 코드를 작성하고 왜 오류가 있는지 이해하지 못합니다:

```ts twoslash
// @errors: 2554
function fn(x: string): void;
function fn() {
  // ...
}
// 0개의 인수로 호출할 수 있을 것으로 예상
fn();
```

다시 말해, 함수 본문을 작성하는 데 사용된 시그니처는 외부에서 "볼" 수 없습니다.

> _구현_의 시그니처는 외부에서 보이지 않습니다.
> 오버로드된 함수를 작성할 때, 항상 함수 구현 위에 _두 개_ 이상의 시그니처가 있어야 합니다.

구현 시그니처도 오버로드 시그니처와 _호환_되어야 합니다.
예를 들어, 이러한 함수에는 구현 시그니처가 올바른 방식으로 오버로드와 일치하지 않기 때문에 오류가 있습니다:

```ts twoslash
// @errors: 2394
function fn(x: boolean): void;
// 인수 타입이 올바르지 않음
function fn(x: string): void;
function fn(x: boolean) {}
```

```ts twoslash
// @errors: 2394
function fn(x: string): string;
// 반환 타입이 올바르지 않음
function fn(x: number): boolean;
function fn(x: string | number) {
  return "oops";
}
```

#### 좋은 오버로드 작성하기

제네릭과 마찬가지로, 함수 오버로드를 사용할 때 따라야 할 몇 가지 가이드라인이 있습니다.
이러한 원칙을 따르면 함수를 더 쉽게 호출하고, 이해하고, 구현할 수 있습니다.

문자열이나 배열의 길이를 반환하는 함수를 생각해 봅시다:

```ts twoslash
function len(s: string): number;
function len(arr: any[]): number;
function len(x: any) {
  return x.length;
}
```

이 함수는 괜찮습니다; 문자열이나 배열로 호출할 수 있습니다.
그러나 문자열 _또는_ 배열일 수 있는 값으로는 호출할 수 없습니다. TypeScript는 함수 호출을 단일 오버로드로만 해결할 수 있기 때문입니다:

```ts twoslash
// @errors: 2769
declare function len(s: string): number;
declare function len(arr: any[]): number;
// ---cut---
len(""); // OK
len([0]); // OK
len(Math.random() > 0.5 ? "hello" : [0]);
```

두 오버로드가 같은 인수 수와 같은 반환 타입이 있기 때문에, 대신 오버로드되지 않은 버전의 함수를 작성할 수 있습니다:

```ts twoslash
function len(x: any[] | string) {
  return x.length;
}
```

이것이 훨씬 낫습니다!
호출자는 어느 종류의 값으로든 이것을 호출할 수 있으며, 추가 보너스로 올바른 구현 시그니처를 알아낼 필요가 없습니다.

> 가능하면 항상 오버로드보다 유니온 타입의 매개변수를 선호하세요

### 함수에서 `this` 선언하기

TypeScript는 코드 흐름 분석을 통해 함수에서 `this`가 무엇이어야 하는지 추론합니다. 예를 들어:

```ts twoslash
const user = {
  id: 123,

  admin: false,
  becomeAdmin: function () {
    this.admin = true;
  },
};
```

TypeScript는 함수 `user.becomeAdmin`이 외부 객체 `user`에 해당하는 `this`를 가진다는 것을 이해합니다. `this`는 많은 경우에 충분할 수 있지만, `this`가 나타내는 객체에 대해 더 많은 제어가 필요한 경우가 많습니다. JavaScript 명세는 `this`라는 이름의 매개변수를 가질 수 없다고 명시하므로, TypeScript는 해당 구문 공간을 사용하여 함수 본문에서 `this`의 타입을 선언할 수 있도록 합니다.

```ts twoslash
interface User {
  id: number;
  admin: boolean;
}
declare const getDB: () => DB;
// ---cut---
interface DB {
  filterUsers(filter: (this: User) => boolean): User[];
}

const db = getDB();
const admins = db.filterUsers(function (this: User) {
  return this.admin;
});
```

이 패턴은 콜백 스타일 API에서 일반적이며, 다른 객체가 일반적으로 함수가 호출되는 시기를 제어합니다. 이 동작을 얻으려면 화살표 함수가 아닌 `function`을 사용해야 합니다:

```ts twoslash
// @errors: 7041 7017
interface User {
  id: number;
  admin: boolean;
}
declare const getDB: () => DB;
// ---cut---
interface DB {
  filterUsers(filter: (this: User) => boolean): User[];
}

const db = getDB();
const admins = db.filterUsers(() => this.admin);
```

### 알아두면 좋은 다른 타입들

함수 타입으로 작업할 때 자주 나타나는 몇 가지 추가 타입이 있습니다.
모든 타입과 마찬가지로, 어디서나 사용할 수 있지만, 이것들은 특히 함수의 맥락에서 관련이 있습니다.

#### `void`

`void`는 값을 반환하지 않는 함수의 반환 값을 나타냅니다.
함수에 `return` 문이 없거나, return 문에서 명시적 값을 반환하지 않을 때마다 추론되는 타입입니다:

```ts twoslash
// 추론된 반환 타입은 void
function noop() {
  return;
}
```

JavaScript에서 값을 반환하지 않는 함수는 암시적으로 값 `undefined`를 반환합니다.
그러나 TypeScript에서 `void`와 `undefined`는 같은 것이 아닙니다.
이 챕터 끝에 더 자세한 내용이 있습니다.

> `void`는 `undefined`와 같지 않습니다.

#### `object`

특수 타입 `object`는 기본형(`string`, `number`, `bigint`, `boolean`, `symbol`, `null`, `undefined`)이 아닌 모든 값을 참조합니다.
이것은 _빈 객체 타입_ `{ }`과 다르며, 전역 타입 `Object`와도 다릅니다.
`Object`는 아마도 사용하지 않을 것입니다.

> `object`는 `Object`가 아닙니다. **항상** `object`를 사용하세요!

JavaScript에서 함수 값은 객체입니다: 속성을 가지고, 프로토타입 체인에 `Object.prototype`이 있고, `instanceof Object`이고, `Object.keys`를 호출할 수 있는 등입니다.
이러한 이유로 함수 타입은 TypeScript에서 `object`로 간주됩니다.

#### `unknown`

`unknown` 타입은 _어떤_ 값이든 나타냅니다.
이것은 `any` 타입과 유사하지만, `unknown` 값으로 무언가를 하는 것이 합법적이지 않기 때문에 더 안전합니다:

```ts twoslash
// @errors: 2571 18046
function f1(a: any) {
  a.b(); // OK
}
function f2(a: unknown) {
  a.b();
}
```

이것은 함수 본문에 `any` 값이 없이 어떤 값이든 받는 함수를 설명할 수 있기 때문에 함수 타입을 설명할 때 유용합니다.

반대로, unknown 타입의 값을 반환하는 함수를 설명할 수 있습니다:

```ts twoslash
declare const someRandomString: string;
// ---cut---
function safeParse(s: string): unknown {
  return JSON.parse(s);
}

// 'obj'를 조심해야 함!
const obj = safeParse(someRandomString);
```

#### `never`

일부 함수는 _절대_ 값을 반환하지 않습니다:

```ts twoslash
function fail(msg: string): never {
  throw new Error(msg);
}
```

`never` 타입은 _절대_ 관찰되지 않는 값을 나타냅니다.
반환 타입에서 이것은 함수가 예외를 발생시키거나 프로그램 실행을 종료한다는 것을 의미합니다.

`never`는 TypeScript가 유니온에 아무것도 남지 않았다고 결정할 때도 나타납니다.

```ts twoslash
function fn(x: string | number) {
  if (typeof x === "string") {
    // 무언가를 함
  } else if (typeof x === "number") {
    // 다른 무언가를 함
  } else {
    x; // 타입 'never'를 가짐!
  }
}
```

#### `Function`

전역 타입 `Function`은 JavaScript의 모든 함수 값에 존재하는 `bind`, `call`, `apply` 및 기타 속성을 설명합니다.
또한 `Function` 타입의 값은 항상 호출할 수 있는 특별한 속성이 있습니다; 이러한 호출은 `any`를 반환합니다:

```ts twoslash
function doSomething(f: Function) {
  return f(1, 2, 3);
}
```

이것은 _타입이 지정되지 않은 함수 호출_이며, 안전하지 않은 `any` 반환 타입 때문에 일반적으로 피하는 것이 좋습니다.

임의의 함수를 받아들여야 하지만 호출할 의도가 없다면, `() => void` 타입이 일반적으로 더 안전합니다.

### 나머지 매개변수와 인수

#### 나머지 매개변수

선택적 매개변수나 오버로드를 사용하여 다양한 고정 인수 수를 받아들일 수 있는 함수를 만드는 것 외에도, _나머지 매개변수_를 사용하여 _무한한_ 수의 인수를 받는 함수를 정의할 수도 있습니다.

나머지 매개변수는 다른 모든 매개변수 뒤에 나타나며, `...` 구문을 사용합니다:

```ts twoslash
function multiply(n: number, ...m: number[]) {
  return m.map((x) => n * x);
}
// 'a'는 값 [10, 20, 30, 40]을 얻음
const a = multiply(10, 1, 2, 3, 4);
```

TypeScript에서 이러한 매개변수의 타입 어노테이션은 암시적으로 `any`가 아닌 `any[]`이며, 주어진 모든 타입 어노테이션은 `Array<T>` 또는 `T[]` 형태이거나, 튜플 타입(나중에 배울 것)이어야 합니다.

#### 나머지 인수

반대로, 스프레드 구문을 사용하여 반복 가능한 객체(예: 배열)에서 가변적인 수의 인수를 _제공_할 수 있습니다.
예를 들어, 배열의 `push` 메서드는 여러 인수를 받습니다:

```ts twoslash
const arr1 = [1, 2, 3];
const arr2 = [4, 5, 6];
arr1.push(...arr2);
```

일반적으로 TypeScript는 배열이 불변이라고 가정하지 않습니다.
이것은 놀라운 동작을 초래할 수 있습니다:

```ts twoslash
// @errors: 2556
// 추론된 타입은 number[] -- "0개 이상의 숫자를 가진 배열",
// 구체적으로 두 개의 숫자가 아님
const args = [8, 5];
const angle = Math.atan2(...args);
```

이 상황에 대한 최선의 수정은 코드에 따라 약간 다르지만, 일반적으로 `const` 컨텍스트가 가장 간단한 해결책입니다:

```ts twoslash
// 2-길이 튜플로 추론됨
const args = [8, 5] as const;
// OK
const angle = Math.atan2(...args);
```

나머지 인수를 사용하려면 이전 런타임을 대상으로 할 때 [`downlevelIteration`](/tsconfig#downlevelIteration)을 켜야 할 수 있습니다.

### 매개변수 구조 분해

매개변수 구조 분해를 사용하여 인수로 제공된 객체를 함수 본문에서 하나 이상의 로컬 변수로 편리하게 풀어낼 수 있습니다.
JavaScript에서는 다음과 같습니다:

```js
function sum({ a, b, c }) {
  console.log(a + b + c);
}
sum({ a: 10, b: 3, c: 9 });
```

객체에 대한 타입 어노테이션은 구조 분해 구문 뒤에 옵니다:

```ts twoslash
function sum({ a, b, c }: { a: number; b: number; c: number }) {
  console.log(a + b + c);
}
```

이것은 약간 장황해 보일 수 있지만, 여기서도 명명된 타입을 사용할 수 있습니다:

```ts twoslash
// 이전 예제와 동일
type ABC = { a: number; b: number; c: number };
function sum({ a, b, c }: ABC) {
  console.log(a + b + c);
}
```

### 함수의 할당 가능성

#### 반환 타입 `void`

함수의 `void` 반환 타입은 일부 특이하지만 예상되는 동작을 생성할 수 있습니다.

`void` 반환 타입을 가진 문맥적 타이핑은 함수가 무언가를 반환**하지 않도록** 강제하지 **않습니다**. 이것을 말하는 또 다른 방법은 `void` 반환 타입을 가진 문맥적 함수 타입(`type voidFunc = () => void`)이 구현될 때 _어떤_ 다른 값이든 반환할 수 있지만, 무시될 것입니다.

따라서, `() => void` 타입의 다음 구현은 유효합니다:

```ts twoslash
type voidFunc = () => void;

const f1: voidFunc = () => {
  return true;
};

const f2: voidFunc = () => true;

const f3: voidFunc = function () {
  return true;
};
```

그리고 이러한 함수 중 하나의 반환 값이 다른 변수에 할당될 때, `void` 타입을 유지합니다:

```ts twoslash
type voidFunc = () => void;

const f1: voidFunc = () => {
  return true;
};

const f2: voidFunc = () => true;

const f3: voidFunc = function () {
  return true;
};
// ---cut---
const v1 = f1();

const v2 = f2();

const v3 = f3();
```

이 동작이 존재하여 `Array.prototype.push`가 숫자를 반환하고 `Array.prototype.forEach` 메서드가 `void` 반환 타입을 가진 함수를 기대함에도 불구하고 다음 코드가 유효합니다.

```ts twoslash
const src = [1, 2, 3];
const dst = [0];

src.forEach((el) => dst.push(el));
```

알아야 할 또 다른 특별한 경우가 있습니다. 리터럴 함수 정의가 `void` 반환 타입을 가질 때, 해당 함수는 아무것도 반환하지 **않아야** 합니다.

```ts twoslash
function f2(): void {
  // @ts-expect-error
  return true;
}

const f3 = function (): void {
  // @ts-expect-error
  return true;
};
```

`void`에 대해 더 알아보려면 다음 문서 항목을 참조하세요:

- [FAQ - "왜 void가 아닌 것을 반환하는 함수가 void를 반환하는 함수에 할당 가능한가요?"](https://github.com/Microsoft/TypeScript/wiki/FAQ#why-are-functions-returning-non-void-assignable-to-function-returning-void)

---

## 객체 타입

> **원문:** https://www.typescriptlang.org/docs/handbook/2/objects.html

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

### 빠른 참조

일상적인 구문을 한눈에 빠르게 보고 싶다면 [`type`과 `interface`](https://www.typescriptlang.org/cheatsheets)에 대한 치트시트가 있습니다.

### 속성 수정자

객체 타입의 각 속성은 타입, 속성이 선택적인지 여부, 속성을 쓸 수 있는지 여부와 같은 몇 가지를 지정할 수 있습니다.

#### 선택적 속성

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

#### `readonly` 속성

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

#### 인덱스 시그니처

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

### 초과 속성 검사

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

`createSquare`에 주어진 인수가 `color` 대신 `colour`로 철자가 되어 있는 것에 주목하세요.
일반 JavaScript에서 이런 종류의 일은 조용히 실패합니다.

이 프로그램이 올바르게 타입화되었다고 주장할 수 있습니다. `width` 속성이 호환되고, `color` 속성이 없으며, 추가 `colour` 속성은 중요하지 않기 때문입니다.

그러나 TypeScript는 이 코드에 아마도 버그가 있다는 입장을 취합니다.
객체 리터럴은 특별한 처리를 받고 다른 변수에 할당하거나 인수로 전달할 때 _초과 속성 검사_를 받습니다.
객체 리터럴이 "대상 타입"에 없는 속성이 있으면, 오류가 발생합니다:

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

### 타입 확장하기

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

### 교차 타입

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

### 인터페이스 확장 vs. 교차

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

### 제네릭 객체 타입

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

#### `Array` 타입

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

#### `ReadonlyArray` 타입

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

#### 튜플 타입

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
> 그러나 모든 사용자가 무엇이 명확한지에 대해 같은 관점이 있지 않으므로, 설명적인 속성 이름을 가진 객체를 사용하는 것이 API에 더 좋을 수 있는지 재고할 가치가 있습니다.

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

#### `readonly` 튜플 타입

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

---

## 클래스

## 클래스

> **원문:** https://www.typescriptlang.org/docs/handbook/2/classes.html

TypeScript는 ES2015에서 도입된 `class` 키워드에 대한 완전한 지원을 제공합니다.

다른 JavaScript 언어 기능과 마찬가지로 TypeScript는 클래스와 다른 타입 간의 관계를 표현할 수 있도록 타입 어노테이션 및 기타 구문을 추가합니다.

### 클래스 멤버

다음은 가장 기본적인 클래스입니다 - 빈 클래스:

```ts twoslash
class Point {}
```

이 클래스는 아직 유용하지 않으므로, 몇 가지 멤버를 추가해 봅시다.

#### 필드

필드 선언은 클래스에 공개 쓰기 가능 속성을 생성합니다:

```ts twoslash
// @strictPropertyInitialization: false
class Point {
  x: number;
  y: number;
}

const pt = new Point();
pt.x = 0;
pt.y = 0;
```

다른 위치와 마찬가지로, 타입 어노테이션은 선택 사항이지만, 지정되지 않으면 암시적 `any`가 됩니다.

필드는 _초기화자_도 가질 수 있습니다; 이것들은 클래스가 인스턴스화될 때 자동으로 실행됩니다:

```ts twoslash
class Point {
  x = 0;
  y = 0;
}

const pt = new Point();
// 0, 0을 출력
console.log(`${pt.x}, ${pt.y}`);
```

`const`, `let`, `var`와 마찬가지로, 클래스 속성의 초기화자는 타입을 추론하는 데 사용됩니다:

```ts twoslash
// @errors: 2322
class Point {
  x = 0;
  y = 0;
}
// ---cut---
const pt = new Point();
pt.x = "0";
```

##### `--strictPropertyInitialization`

[`strictPropertyInitialization`](/tsconfig#strictPropertyInitialization) 설정은 클래스 필드가 생성자에서 초기화되어야 하는지를 제어합니다.

```ts twoslash
// @errors: 2564
class BadGreeter {
  name: string;
}
```

```ts twoslash
class GoodGreeter {
  name: string;

  constructor() {
    this.name = "hello";
  }
}
```

필드가 _생성자 자체에서_ 초기화되어야 한다는 점에 주의하세요.
TypeScript는 파생 클래스가 해당 메서드를 재정의하고 멤버를 초기화하지 못할 수 있으므로, 생성자에서 호출하는 메서드를 분석하여 초기화를 감지하지 않습니다.

생성자 외의 수단을 통해 필드를 확실히 초기화하려는 경우(예: 외부 라이브러리가 클래스의 일부를 채워주는 경우), _확정 할당 단언 연산자_ `!`를 사용할 수 있습니다:

```ts twoslash
class OKGreeter {
  // 초기화되지 않았지만, 오류 없음
  name!: string;
}
```

#### `readonly`

필드에 `readonly` 수정자를 접두사로 붙일 수 있습니다.
이것은 생성자 외부에서 필드에 대한 할당을 방지합니다.

```ts twoslash
// @errors: 2540 2540
class Greeter {
  readonly name: string = "world";

  constructor(otherName?: string) {
    if (otherName !== undefined) {
      this.name = otherName;
    }
  }

  err() {
    this.name = "not ok";
  }
}
const g = new Greeter();
g.name = "also not ok";
```

#### 생성자

클래스 생성자는 함수와 매우 유사합니다.
타입 어노테이션, 기본값 및 오버로드와 함께 매개변수를 추가할 수 있습니다:

```ts twoslash
class Point {
  x: number;
  y: number;

  // 기본값이 있는 일반 시그니처
  constructor(x = 0, y = 0) {
    this.x = x;
    this.y = y;
  }
}
```

```ts twoslash
class Point {
  x: number = 0;
  y: number = 0;

  // 생성자 오버로드
  constructor(x: number, y: number);
  constructor(xy: string);
  constructor(x: string | number, y: number = 0) {
    // 여기에 코드 로직
  }
}
```

클래스 생성자 시그니처와 함수 시그니처 사이에는 몇 가지 차이점이 있습니다:

- 생성자는 타입 매개변수를 가질 수 없습니다 - 이것들은 외부 클래스 선언에 속하며, 나중에 배울 것입니다
- 생성자는 반환 타입 어노테이션을 가질 수 없습니다 - 클래스 인스턴스 타입이 항상 반환되는 것입니다

##### Super 호출

JavaScript에서와 마찬가지로, 기본 클래스가 있으면 `this.` 멤버를 사용하기 전에 생성자 본문에서 `super();`를 호출해야 합니다:

```ts twoslash
// @errors: 17009
class Base {
  k = 4;
}

class Derived extends Base {
  constructor() {
    // ES5에서 잘못된 값을 출력; ES6에서 예외 발생
    console.log(this.k);
    super();
  }
}
```

`super`를 호출하는 것을 잊는 것은 JavaScript에서 쉬운 실수이지만, TypeScript는 필요할 때 알려줍니다.

#### 메서드

클래스의 함수 속성을 _메서드_라고 합니다.
메서드는 함수 및 생성자와 동일한 모든 타입 어노테이션을 사용할 수 있습니다:

```ts twoslash
class Point {
  x = 10;
  y = 10;

  scale(n: number): void {
    this.x *= n;
    this.y *= n;
  }
}
```

표준 타입 어노테이션 외에 TypeScript는 메서드에 새로운 것을 추가하지 않습니다.

메서드 본문 내에서 `this.`를 통해 필드 및 기타 메서드에 접근하는 것이 여전히 필수라는 점에 주의하세요.
메서드 본문의 비한정 이름은 항상 둘러싼 범위의 무언가를 참조합니다:

```ts twoslash
// @errors: 2322
let x: number = 0;

class C {
  x: string = "hello";

  m() {
    // 이것은 클래스 속성이 아닌 1행의 'x'를 수정하려고 함
    x = "world";
  }
}
```

#### 게터 / 세터

클래스도 _접근자_를 가질 수 있습니다:

```ts twoslash
class C {
  _length = 0;
  get length() {
    return this._length;
  }
  set length(value) {
    this._length = value;
  }
}
```

> 추가 로직이 없는 필드 기반 get/set 쌍은 JavaScript에서 거의 유용하지 않습니다.
> get/set 작업 중에 추가 로직을 추가할 필요가 없다면 공개 필드를 노출하는 것이 좋습니다.

TypeScript에는 접근자에 대한 몇 가지 특별한 추론 규칙이 있습니다:

- `get`이 있지만 `set`이 없으면, 속성이 자동으로 `readonly`입니다
- 세터 매개변수의 타입이 지정되지 않으면, 게터의 반환 타입에서 추론됩니다

[TypeScript 4.3](https://devblogs.microsoft.com/typescript/announcing-typescript-4-3/)부터 가져오기와 설정에 대해 다른 타입을 가진 접근자를 가질 수 있습니다.

```ts twoslash
class Thing {
  _size = 0;

  get size(): number {
    return this._size;
  }

  set size(value: string | number | boolean) {
    let num = Number(value);

    // NaN, Infinity 등을 허용하지 않음

    if (!Number.isFinite(num)) {
      this._size = 0;
      return;
    }

    this._size = num;
  }
}
```

#### 인덱스 시그니처

클래스는 인덱스 시그니처를 선언할 수 있습니다; 이것들은 [다른 객체 타입의 인덱스 시그니처](/docs/handbook/2/objects.html#index-signatures)와 같은 방식으로 작동합니다:

```ts twoslash
class MyClass {
  [s: string]: boolean | ((s: string) => boolean);

  check(s: string) {
    return this[s] as boolean;
  }
}
```

인덱스 시그니처 타입이 메서드의 타입도 캡처해야 하므로, 이러한 타입을 유용하게 사용하기는 쉽지 않습니다.
일반적으로 인덱싱된 데이터를 클래스 인스턴스 자체가 아닌 다른 곳에 저장하는 것이 좋습니다.

### 클래스 상속

객체 지향 기능을 가진 다른 언어와 마찬가지로, JavaScript의 클래스는 기본 클래스에서 상속할 수 있습니다.

#### `implements` 절

`implements` 절을 사용하여 클래스가 특정 `interface`를 만족하는지 확인할 수 있습니다.
클래스가 올바르게 구현하지 못하면 오류가 발생합니다:

```ts twoslash
// @errors: 2420
interface Pingable {
  ping(): void;
}

class Sonar implements Pingable {
  ping() {
    console.log("ping!");
  }
}

class Ball implements Pingable {
  pong() {
    console.log("pong!");
  }
}
```

클래스는 여러 인터페이스를 구현할 수도 있습니다. 예: `class C implements A, B {`.

##### 주의 사항

`implements` 절은 클래스가 인터페이스 타입으로 처리될 수 있는지에 대한 검사일 뿐이라는 것을 이해하는 것이 중요합니다.
클래스의 타입이나 메서드를 _전혀_ 변경하지 않습니다.
일반적인 오류 원인은 `implements` 절이 클래스 타입을 변경할 것이라고 가정하는 것입니다 - 변경하지 않습니다!

```ts twoslash
// @errors: 7006
interface Checkable {
  check(name: string): boolean;
}

class NameChecker implements Checkable {
  check(s) {
    // 여기서 오류 없음에 주목
    return s.toLowerCase() === "ok";
    //         ^?
  }
}
```

이 예제에서, 아마도 `s`의 타입이 `check`의 `name: string` 매개변수에 의해 영향을 받을 것으로 예상했을 것입니다.
그렇지 않습니다 - `implements` 절은 클래스 본문이 검사되거나 타입이 추론되는 방식을 변경하지 않습니다.

마찬가지로, 선택적 속성이 있는 인터페이스를 구현해도 해당 속성이 생성되지 않습니다:

```ts twoslash
// @errors: 2339
interface A {
  x: number;
  y?: number;
}
class C implements A {
  x = 0;
}
const c = new C();
c.y = 10;
```

#### `extends` 절

클래스는 기본 클래스에서 `extend`할 수 있습니다.
파생 클래스는 기본 클래스의 모든 속성과 메서드를 가지며, 추가 멤버도 정의할 수 있습니다.

```ts twoslash
class Animal {
  move() {
    console.log("Moving along!");
  }
}

class Dog extends Animal {
  woof(times: number) {
    for (let i = 0; i < times; i++) {
      console.log("woof!");
    }
  }
}

const d = new Dog();
// 기본 클래스 메서드
d.move();
// 파생 클래스 메서드
d.woof(3);
```

##### 메서드 재정의

파생 클래스는 기본 클래스의 필드나 속성을 재정의할 수도 있습니다.
`super.` 구문을 사용하여 기본 클래스 메서드에 접근할 수 있습니다.
JavaScript 클래스는 간단한 조회 객체이므로, "슈퍼 필드"라는 개념이 없습니다.

TypeScript는 파생 클래스가 항상 기본 클래스의 하위 타입이 되도록 강제합니다.

예를 들어, 다음은 메서드를 재정의하는 합법적인 방법입니다:

```ts twoslash
class Base {
  greet() {
    console.log("Hello, world!");
  }
}

class Derived extends Base {
  greet(name?: string) {
    if (name === undefined) {
      super.greet();
    } else {
      console.log(`Hello, ${name.toUpperCase()}`);
    }
  }
}

const d = new Derived();
d.greet();
d.greet("reader");
```

파생 클래스가 기본 클래스 계약을 따르는 것이 중요합니다.
기본 클래스 참조를 통해 파생 클래스 인스턴스를 참조하는 것은 매우 일반적(그리고 항상 합법적!)입니다:

```ts twoslash
class Base {
  greet() {
    console.log("Hello, world!");
  }
}
class Derived extends Base {}
const d = new Derived();
// ---cut---
// 기본 클래스 참조를 통해 파생 인스턴스를 별칭으로 지정
const b: Base = d;
// 문제 없음
b.greet();
```

`Derived`가 `Base`의 계약을 따르지 않으면 어떻게 될까요?

```ts twoslash
// @errors: 2416
class Base {
  greet() {
    console.log("Hello, world!");
  }
}

class Derived extends Base {
  // 이 매개변수를 필수로 만듦
  greet(name: string) {
    console.log(`Hello, ${name.toUpperCase()}`);
  }
}
```

오류에도 불구하고 이 코드를 컴파일하면, 이 샘플은 충돌합니다:

```ts twoslash
declare class Base {
  greet(): void;
}
declare class Derived extends Base {}
// ---cut---
const b: Base = new Derived();
// "name"이 undefined가 되어 충돌
b.greet();
```

##### 타입 전용 필드 선언

`target >= ES2022` 또는 [`useDefineForClassFields`](/tsconfig#useDefineForClassFields)가 `true`이면, 클래스 필드는 부모 클래스 생성자가 완료된 후 초기화되어 부모 클래스가 설정한 값을 덮어씁니다. 이것은 상속된 필드에 대해 더 정확한 타입만 다시 선언하고 싶을 때 문제가 될 수 있습니다. 이러한 경우를 처리하려면, 이 필드 선언에 대한 런타임 효과가 없어야 함을 TypeScript에 나타내기 위해 `declare`를 작성할 수 있습니다.

```ts twoslash
interface Animal {
  dateOfBirth: any;
}

interface Dog extends Animal {
  breed: any;
}

class AnimalHouse {
  resident: Animal;
  constructor(animal: Animal) {
    this.resident = animal;
  }
}

class DogHouse extends AnimalHouse {
  // JavaScript 코드를 내보내지 않음,
  // 타입이 올바른지만 확인
  declare resident: Dog;
  constructor(dog: Dog) {
    super(dog);
  }
}
```

##### 초기화 순서

JavaScript 클래스가 초기화되는 순서는 일부 경우에 놀라울 수 있습니다.
다음 코드를 생각해 봅시다:

```ts twoslash
class Base {
  name = "base";
  constructor() {
    console.log("My name is " + this.name);
  }
}

class Derived extends Base {
  name = "derived";
}

// "derived"가 아닌 "base"를 출력
const d = new Derived();
```

여기서 무슨 일이 일어났나요?

JavaScript가 정의하는 클래스 초기화 순서는:

- 기본 클래스 필드가 초기화됨
- 기본 클래스 생성자가 실행됨
- 파생 클래스 필드가 초기화됨
- 파생 클래스 생성자가 실행됨

이것은 파생 클래스 필드 초기화가 아직 실행되지 않았기 때문에 기본 클래스 생성자가 자체 생성자 중에 `name`에 대해 자체 값을 보았다는 것을 의미합니다.

##### 내장 타입 상속

> 참고: `Array`, `Error`, `Map` 등과 같은 내장 타입에서 상속할 계획이 없거나 컴파일 대상이 명시적으로 `ES6`/`ES2015` 이상으로 설정된 경우, 이 섹션을 건너뛸 수 있습니다.

ES2015에서 객체를 반환하는 생성자는 `super(...)`의 모든 호출자에 대해 `this` 값을 암시적으로 대체합니다.
생성된 생성자 코드가 `super(...)`의 잠재적 반환 값을 캡처하고 `this`로 대체해야 합니다.

결과적으로, `Error`, `Array` 등의 서브클래싱이 더 이상 예상대로 작동하지 않을 수 있습니다.
이것은 `Error`, `Array` 등의 생성자 함수가 ECMAScript 6의 `new.target`을 사용하여 프로토타입 체인을 조정하기 때문입니다;
그러나 ECMAScript 5에서 생성자를 호출할 때 `new.target`에 대한 값을 보장할 방법이 없습니다.
다른 다운레벨 컴파일러도 일반적으로 기본적으로 같은 제한이 있습니다.

다음과 같은 서브클래스의 경우:

```ts twoslash
class MsgError extends Error {
  constructor(m: string) {
    super(m);
  }
  sayHello() {
    return "hello " + this.message;
  }
}
```

다음을 발견할 수 있습니다:

- 이러한 서브클래스를 생성하여 반환된 객체에서 메서드가 `undefined`일 수 있으므로, `sayHello`를 호출하면 오류가 발생합니다.
- 서브클래스의 인스턴스와 해당 인스턴스 사이에서 `instanceof`가 깨지므로, `(new MsgError()) instanceof MsgError`가 `false`를 반환합니다.

권장 사항으로, 모든 `super(...)` 호출 직후에 프로토타입을 수동으로 조정할 수 있습니다.

```ts twoslash
class MsgError extends Error {
  constructor(m: string) {
    super(m);

    // 명시적으로 프로토타입 설정.
    Object.setPrototypeOf(this, MsgError.prototype);
  }

  sayHello() {
    return "hello " + this.message;
  }
}
```

그러나 `MsgError`의 모든 서브클래스도 프로토타입을 수동으로 설정해야 합니다.
[`Object.setPrototypeOf`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/setPrototypeOf)를 지원하지 않는 런타임의 경우, 대신 [`__proto__`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/proto)를 사용할 수 있습니다.

안타깝게도, [이러한 해결 방법은 Internet Explorer 10 및 이전 버전에서 작동하지 않습니다](https://msdn.microsoft.com/en-us/library/s4esdbwz(v=vs.94).aspx).
프로토타입에서 인스턴스 자체로 메서드를 수동으로 복사할 수 있지만(예: `MsgError.prototype`을 `this`로), 프로토타입 체인 자체는 수정할 수 없습니다.

### 멤버 가시성

TypeScript를 사용하여 특정 메서드나 속성이 클래스 외부의 코드에 표시되는지 여부를 제어할 수 있습니다.

#### `public`

클래스 멤버의 기본 가시성은 `public`입니다.
`public` 멤버는 어디서든 접근할 수 있습니다:

```ts twoslash
class Greeter {
  public greet() {
    console.log("hi!");
  }
}
const g = new Greeter();
g.greet();
```

`public`이 이미 기본 가시성 수정자이므로, 클래스 멤버에 작성할 _필요_가 없지만, 스타일/가독성을 위해 선택할 수 있습니다.

#### `protected`

`protected` 멤버는 선언된 클래스의 서브클래스에서만 볼 수 있습니다.

```ts twoslash
// @errors: 2445
class Greeter {
  public greet() {
    console.log("Hello, " + this.getName());
  }
  protected getName() {
    return "hi";
  }
}

class SpecialGreeter extends Greeter {
  public howdy() {
    // 여기서 protected 멤버에 접근 OK
    console.log("Howdy, " + this.getName());
    //                          ^^^^^^^^^^^^^^
  }
}
const g = new SpecialGreeter();
g.greet(); // OK
g.getName();
```

##### `protected` 멤버 노출

파생 클래스는 기본 클래스 계약을 따라야 하지만, 더 많은 기능을 가진 기본 클래스의 하위 타입을 노출하도록 선택할 수 있습니다.
여기에는 `protected` 멤버를 `public`으로 만드는 것이 포함됩니다:

```ts twoslash
class Base {
  protected m = 10;
}
class Derived extends Base {
  // 수정자 없음, 기본값은 'public'
  m = 15;
}
const d = new Derived();
console.log(d.m); // OK
```

`Derived`가 이미 `m`을 자유롭게 읽고 쓸 수 있었으므로, 이것은 이 상황의 "보안"을 의미 있게 변경하지 않습니다.
여기서 주목할 주요 사항은 파생 클래스에서 이 노출이 의도적이지 않은 경우 `protected` 수정자를 반복하도록 주의해야 한다는 것입니다.

##### 계층 간 `protected` 접근

TypeScript는 클래스 계층에서 형제 클래스의 `protected` 멤버에 접근하는 것을 허용하지 않습니다:

```ts twoslash
// @errors: 2446
class Base {
  protected x: number = 1;
}
class Derived1 extends Base {
  protected x: number = 5;
}
class Derived2 extends Base {
  f1(other: Derived2) {
    other.x = 10;
  }
  f2(other: Derived1) {
    other.x = 10;
  }
}
```

이것은 `Derived2`에서 `x`에 접근하는 것이 `Derived2`의 서브클래스에서만 합법적이어야 하고, `Derived1`은 그중 하나가 아니기 때문입니다.
또한, `Derived1` 참조를 통해 `x`에 접근하는 것이 불법이라면(확실히 그래야 합니다!), 기본 클래스 참조를 통해 접근하는 것도 상황을 개선하지 않아야 합니다.

#### `private`

`private`은 `protected`와 같지만, 서브클래스에서도 멤버에 대한 접근을 허용하지 않습니다:

```ts twoslash
// @errors: 2341
class Base {
  private x = 0;
}
const b = new Base();
// 클래스 외부에서 접근 불가
console.log(b.x);
```

```ts twoslash
// @errors: 2341
class Base {
  private x = 0;
}
// ---cut---
class Derived extends Base {
  showX() {
    // 서브클래스에서 접근 불가
    console.log(this.x);
  }
}
```

`private` 멤버는 파생 클래스에 표시되지 않으므로, 파생 클래스는 가시성을 높일 수 없습니다:

```ts twoslash
// @errors: 2415
class Base {
  private x = 0;
}
class Derived extends Base {
  x = 1;
}
```

##### 인스턴스 간 `private` 접근

다른 OOP 언어는 같은 클래스의 다른 인스턴스가 서로의 `private` 멤버에 접근할 수 있는지에 대해 의견이 다릅니다.
Java, C#, C++, Swift, PHP와 같은 언어는 이를 허용하지만, Ruby는 허용하지 않습니다.

TypeScript는 인스턴스 간 `private` 접근을 허용합니다:

```ts twoslash
class A {
  private x = 10;

  public sameAs(other: A) {
    // 오류 없음
    return other.x === this.x;
  }
}
```

##### 주의 사항

TypeScript 타입 시스템의 다른 측면과 마찬가지로, `private`과 `protected`는 [타입 검사 중에만 적용됩니다](https://www.typescriptlang.org/play?removeComments=true&target=99&ts=4.3.4#code/PTAEGMBsEMGddAEQPYHNQBMCmVoCcsEAHPASwDdoAXLUAM1K0gwQFdZSA7dAKWkoDK4MkSoByBAGJQJLAwAeAWABQIUH0HDSoiTLKUaoUggAW+DHorUsAOlABJcQlhUy4KpACeoLJzrI8cCwMGxU1ABVPIiwhESpMZEJQTmR4lxFQaQxWMm4IZABbIlIYKlJkTlDlXHgkNFAAbxVQTIAjfABrAEEC5FZOeIBeUAAGAG5mmSw8WAroSFIqb2GAIjMiIk8VieVJ8Ar01ncAgAoASkaAXxVr3dUwGoQAYWpMHBgCYn1rekZmNg4eUi0Vi2icoBWJCsNBWoA6WE8AHcAiEwmBgTEtDovtDaMZQLM6PEoQZbA5wSk0q5SO4vD4-AEghZoJwLGYEIRwNBoqAzFRwCZCFUIlFMXECdSiAhId8YZgclx0PsiiVqOVOAAaUAFLAsxWgKiC35MFigfC0FKgSAVVDTSyk+W5dB4fplHVVR6gF7xJrKFotEk-HXIRE9PoDUDDcaTAPTWaceaLZYQlmoPBbHYx-KcQ7HPDnK43FQqfY5+IMDDISPJLCIuqoc47UsuUCofAME3Vzi1r3URvF5QV5A2STtPDdXqunZDgDaYlHnTDrrEAF0dm28B3mDZg6HJwN1+2-hg57ulwNV2NQGoZbjYfNrYiENBwEFaojFiZQK08C-4fFKTVCozWfTgfFgLkeT5AUqiAA).

이것은 `in`이나 간단한 속성 조회와 같은 JavaScript 런타임 구문이 여전히 `private` 또는 `protected` 멤버에 접근할 수 있다는 것을 의미합니다:

```ts twoslash
class MySafe {
  private secretKey = 12345;
}
```

```js
// JavaScript 파일에서...
const s = new MySafe();
// 12345를 출력
console.log(s.secretKey);
```

`private`은 또한 타입 검사 중에 대괄호 표기법을 사용한 접근을 허용합니다. 이것은 `private`으로 선언된 필드를 유닛 테스트와 같은 것에 대해 잠재적으로 더 쉽게 접근할 수 있게 하지만, 이러한 필드가 _소프트 private_이며 프라이버시를 엄격하게 적용하지 않는다는 단점이 있습니다.

```ts twoslash
// @errors: 2341
class MySafe {
  private secretKey = 12345;
}

const s = new MySafe();

// 타입 검사 중 허용되지 않음
console.log(s.secretKey);

// OK
console.log(s["secretKey"]);
```

TypeScript의 `private`과 달리, JavaScript의 [private 필드](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Classes/Private_class_fields)(`#`)는 컴파일 후에도 private으로 유지되며 대괄호 표기법 접근과 같은 앞서 언급한 탈출구를 제공하지 않아 _하드 private_입니다.

```ts twoslash
class Dog {
  #barkAmount = 0;
  personality = "happy";

  constructor() {}
}
```

ES2021 이하로 컴파일할 때, TypeScript는 `#` 대신 WeakMap을 사용합니다.

악의적인 행위자로부터 클래스의 값을 보호해야 하는 경우, 클로저, WeakMap 또는 private 필드와 같은 하드 런타임 프라이버시를 제공하는 메커니즘을 사용해야 합니다. 이러한 추가된 프라이버시 검사는 런타임 중 성능에 영향을 미칠 수 있습니다.

### 정적 멤버

클래스는 `static` 멤버를 가질 수 있습니다.
이러한 멤버는 클래스의 특정 인스턴스와 연관되지 않습니다.
클래스 생성자 객체 자체를 통해 접근할 수 있습니다:

```ts twoslash
class MyClass {
  static x = 0;
  static printX() {
    console.log(MyClass.x);
  }
}
console.log(MyClass.x);
MyClass.printX();
```

정적 멤버도 같은 `public`, `protected`, `private` 가시성 수정자를 사용할 수 있습니다:

```ts twoslash
// @errors: 2341
class MyClass {
  private static x = 0;
}
console.log(MyClass.x);
```

정적 멤버도 상속됩니다:

```ts twoslash
class Base {
  static getGreeting() {
    return "Hello world";
  }
}
class Derived extends Base {
  myGreeting = Derived.getGreeting();
}
```

#### 특수 정적 이름

`Function` 프로토타입의 속성을 덮어쓰는 것은 일반적으로 안전하지 않거나 불가능합니다.
클래스 자체가 `new`로 호출할 수 있는 함수이므로, 특정 `static` 이름은 사용할 수 없습니다.
`name`, `length`, `call`과 같은 함수 속성은 `static` 멤버로 정의하기에 유효하지 않습니다:

```ts twoslash
// @errors: 2699
class S {
  static name = "S!";
}
```

#### 왜 정적 클래스가 없나요?

TypeScript(및 JavaScript)에는 예를 들어 C#처럼 `static class`라는 구문이 없습니다.

이러한 구문은 해당 언어가 모든 데이터와 함수를 클래스 내부에 강제하기 때문에 _존재_합니다; TypeScript에는 그러한 제한이 없으므로, 필요하지 않습니다.
단일 인스턴스만 있는 클래스는 일반적으로 JavaScript/TypeScript에서 일반 _객체_로 표현됩니다.

예를 들어, 일반 객체(또는 최상위 함수)가 동일하게 작업을 수행하므로 TypeScript에서 "정적 클래스" 구문이 필요하지 않습니다:

```ts twoslash
// 불필요한 "정적" 클래스
class MyStaticClass {
  static doSomething() {}
}

// 선호됨 (대안 1)
function doSomething() {}

// 선호됨 (대안 2)
const MyHelperObject = {
  dosomething() {},
};
```

### 클래스의 `static` 블록

정적 블록을 사용하면 포함하는 클래스 내의 private 필드에 접근할 수 있는 자체 범위를 가진 일련의 명령문을 작성할 수 있습니다. 이것은 명령문 작성의 모든 기능, 변수 누출 없음, 클래스 내부에 대한 완전한 접근으로 초기화 코드를 작성할 수 있다는 것을 의미합니다.

```ts twoslash
declare function loadLastInstances(): any[]
// ---cut---
class Foo {
    static #count = 0;

    get count() {
        return Foo.#count;
    }

    static {
        try {
            const lastInstances = loadLastInstances();
            Foo.#count += lastInstances.length;
        }
        catch {}
    }
}
```

### 제네릭 클래스

인터페이스와 마찬가지로 클래스도 제네릭일 수 있습니다.
제네릭 클래스가 `new`로 인스턴스화될 때, 타입 매개변수는 함수 호출에서와 같은 방식으로 추론됩니다:

```ts twoslash
class Box<Type> {
  contents: Type;
  constructor(value: Type) {
    this.contents = value;
  }
}

const b = new Box("hello!");
//    ^?
```

클래스는 인터페이스와 같은 방식으로 제네릭 제약 조건과 기본값을 사용할 수 있습니다.

#### 정적 멤버의 타입 매개변수

이 코드는 합법적이지 않으며, 왜 그런지 명확하지 않을 수 있습니다:

```ts twoslash
// @errors: 2302
class Box<Type> {
  static defaultValue: Type;
}
```

타입은 항상 완전히 지워진다는 것을 기억하세요!
런타임에 `Box.defaultValue` 속성 슬롯은 _하나_뿐입니다.
이것은 `Box<string>.defaultValue`를 설정하면(가능하다면) `Box<number>.defaultValue`도 _변경_한다는 것을 의미합니다 - 좋지 않습니다.
제네릭 클래스의 `static` 멤버는 클래스의 타입 매개변수를 참조할 수 없습니다.

### 클래스에서의 런타임 `this`

TypeScript가 JavaScript의 런타임 동작을 변경하지 않으며, JavaScript의 런타임 동작이 다소 독특하다는 것을 기억해야 합니다.

JavaScript의 `this` 처리는 실제로 특이합니다:

```ts twoslash
class MyClass {
  name = "MyClass";
  getName() {
    return this.name;
  }
}
const c = new MyClass();
const obj = {
  name: "obj",
  getName: c.getName,
};

// "MyClass"가 아닌 "obj"를 출력
console.log(obj.getName());
```

간단히 말해서, 기본적으로 함수 내부의 `this` 값은 _함수가 어떻게 호출되었는지_에 따라 달라집니다.
이 예제에서 함수가 `obj` 참조를 통해 호출되었으므로, `this` 값은 클래스 인스턴스가 아닌 `obj`였습니다.

이것은 거의 원하지 않는 일입니다!
TypeScript는 이런 종류의 오류를 완화하거나 방지하는 몇 가지 방법을 제공합니다.

#### 화살표 함수

`this` 컨텍스트를 잃는 방식으로 자주 호출되는 함수가 있다면, 메서드 정의 대신 화살표 함수 속성을 사용하는 것이 의미가 있을 수 있습니다:

```ts twoslash
class MyClass {
  name = "MyClass";
  getName = () => {
    return this.name;
  };
}
const c = new MyClass();
const g = c.getName;
// 충돌 대신 "MyClass"를 출력
console.log(g());
```

이것에는 몇 가지 트레이드오프가 있습니다:

- `this` 값은 TypeScript로 검사되지 않은 코드에서도 런타임에 올바름이 보장됩니다
- 각 클래스 인스턴스가 이런 방식으로 정의된 각 함수의 자체 복사본을 가지므로 더 많은 메모리를 사용합니다
- 파생 클래스에서 `super.getName`을 사용할 수 없습니다. 프로토타입 체인에 기본 클래스 메서드를 가져올 항목이 없기 때문입니다

#### `this` 매개변수

메서드나 함수 정의에서 `this`라는 이름의 초기 매개변수는 TypeScript에서 특별한 의미를 가집니다.
이러한 매개변수는 컴파일 중에 지워집니다:

```ts twoslash
type SomeType = any;
// ---cut---
// 'this' 매개변수가 있는 TypeScript 입력
function fn(this: SomeType, x: number) {
  /* ... */
}
```

```js
// JavaScript 출력
function fn(x) {
  /* ... */
}
```

TypeScript는 `this` 매개변수가 있는 함수가 올바른 컨텍스트로 호출되는지 검사합니다.
화살표 함수를 사용하는 대신, 메서드 정의에 `this` 매개변수를 추가하여 메서드가 올바르게 호출되도록 정적으로 적용할 수 있습니다:

```ts twoslash
// @errors: 2684
class MyClass {
  name = "MyClass";
  getName(this: MyClass) {
    return this.name;
  }
}
const c = new MyClass();
// OK
c.getName();

// 오류, 충돌할 것
const g = c.getName;
console.log(g());
```

이 메서드는 화살표 함수 접근 방식의 반대 트레이드오프를 만듭니다:

- JavaScript 호출자는 여전히 인식하지 못하고 클래스 메서드를 잘못 사용할 수 있습니다
- 클래스 인스턴스당 하나가 아닌 클래스 정의당 하나의 함수만 할당됩니다
- 기본 메서드 정의는 여전히 `super`를 통해 호출할 수 있습니다

### `this` 타입

클래스에서 `this`라는 특별한 타입은 현재 클래스의 타입을 _동적으로_ 참조합니다.
이것이 어떻게 유용한지 봅시다:

```ts twoslash
class Box {
  contents: string = "";
  set(value: string) {
//  ^?
    this.contents = value;
    return this;
  }
}
```

여기서 TypeScript는 `set`의 반환 타입을 `Box`가 아닌 `this`로 추론했습니다.
이제 `Box`의 서브클래스를 만들어 봅시다:

```ts twoslash
class Box {
  contents: string = "";
  set(value: string) {
    this.contents = value;
    return this;
  }
}
// ---cut---
class ClearableBox extends Box {
  clear() {
    this.contents = "";
  }
}

const a = new ClearableBox();
const b = a.set("hello");
//    ^?
```

매개변수 타입 어노테이션에서도 `this`를 사용할 수 있습니다:

```ts twoslash
class Box {
  content: string = "";
  sameAs(other: this) {
    return other.content === this.content;
  }
}
```

이것은 `other: Box`를 작성하는 것과 다릅니다 -- 파생 클래스가 있는 경우, `sameAs` 메서드는 이제 같은 파생 클래스의 다른 인스턴스만 받아들입니다:

```ts twoslash
// @errors: 2345
class Box {
  content: string = "";
  sameAs(other: this) {
    return other.content === this.content;
  }
}

class DerivedBox extends Box {
  otherContent: string = "?";
}

const base = new Box();
const derived = new DerivedBox();
derived.sameAs(base);
```

#### `this` 기반 타입 가드

클래스와 인터페이스의 메서드에서 반환 위치에 `this is Type`을 사용할 수 있습니다.
타입 좁히기(예: `if` 문)와 혼합하면 대상 객체의 타입이 지정된 `Type`으로 좁혀집니다.

```ts twoslash
// @strictPropertyInitialization: false
class FileSystemObject {
  isFile(): this is FileRep {
    return this instanceof FileRep;
  }
  isDirectory(): this is Directory {
    return this instanceof Directory;
  }
  isNetworked(): this is Networked & this {
    return this.networked;
  }
  constructor(public path: string, private networked: boolean) {}
}

class FileRep extends FileSystemObject {
  constructor(path: string, public content: string) {
    super(path, false);
  }
}

class Directory extends FileSystemObject {
  children: FileSystemObject[];
}

interface Networked {
  host: string;
}

const fso: FileSystemObject = new FileRep("foo/bar.txt", "foo");

if (fso.isFile()) {
  fso.content;
// ^?
} else if (fso.isDirectory()) {
  fso.children;
// ^?
} else if (fso.isNetworked()) {
  fso.host;
// ^?
}
```

this 기반 타입 가드의 일반적인 사용 사례는 특정 필드의 지연 유효성 검사를 허용하는 것입니다. 예를 들어, 이 경우 `hasValue`가 true로 확인되면 box 내부에 있는 값에서 `undefined`를 제거합니다:

```ts twoslash
class Box<T> {
  value?: T;

  hasValue(): this is { value: T } {
    return this.value !== undefined;
  }
}

const box = new Box<string>();
box.value = "Gameboy";

box.value;
//  ^?

if (box.hasValue()) {
  box.value;
  //  ^?
}
```

### 매개변수 속성

TypeScript는 생성자 매개변수를 같은 이름과 값을 가진 클래스 속성으로 바꾸는 특별한 구문을 제공합니다.
이것들을 _매개변수 속성_이라고 하며, 생성자 인수에 가시성 수정자 `public`, `private`, `protected`, 또는 `readonly` 중 하나를 접두사로 붙여 생성합니다.
결과 필드는 해당 수정자를 얻습니다:

```ts twoslash
// @errors: 2341
class Params {
  constructor(
    public readonly x: number,
    protected y: number,
    private z: number
  ) {
    // 본문 필요 없음
  }
}
const a = new Params(1, 2, 3);
console.log(a.x);
//            ^?
console.log(a.z);
```

### 클래스 표현식

클래스 표현식은 클래스 선언과 매우 유사합니다.
유일한 실제 차이점은 클래스 표현식은 이름이 필요하지 않지만, 결국 바인딩된 식별자를 통해 참조할 수 있다는 것입니다:

```ts twoslash
const someClass = class<Type> {
  content: Type;
  constructor(value: Type) {
    this.content = value;
  }
};

const m = new someClass("Hello, world");
//    ^?
```

### 생성자 시그니처

JavaScript 클래스는 `new` 연산자로 인스턴스화됩니다. 클래스 자체의 타입이 주어지면, [InstanceType](/docs/handbook/utility-types.html#instancetypetype) 유틸리티 타입이 이 작업을 모델링합니다.

```ts twoslash
class Point {
  createdAt: number;
  x: number;
  y: number
  constructor(x: number, y: number) {
    this.createdAt = Date.now()
    this.x = x;
    this.y = y;
  }
}
type PointInstance = InstanceType<typeof Point>

function moveRight(point: PointInstance) {
  point.x += 5;
}

const point = new Point(3, 4);
moveRight(point);
point.x; // => 8
```

### `abstract` 클래스와 멤버

TypeScript의 클래스, 메서드 및 필드는 _abstract_일 수 있습니다.

_추상 메서드_ 또는 _추상 필드_는 구현이 제공되지 않은 것입니다.
이러한 멤버는 직접 인스턴스화할 수 없는 _추상 클래스_ 내에 존재해야 합니다.

추상 클래스의 역할은 모든 추상 멤버를 구현하는 서브클래스의 기본 클래스 역할을 하는 것입니다.
클래스에 추상 멤버가 없으면 _구체적_이라고 합니다.

예제를 살펴봅시다:

```ts twoslash
// @errors: 2511
abstract class Base {
  abstract getName(): string;

  printName() {
    console.log("Hello, " + this.getName());
  }
}

const b = new Base();
```

`Base`가 추상이므로 `new`로 인스턴스화할 수 없습니다.
대신, 파생 클래스를 만들고 추상 멤버를 구현해야 합니다:

```ts twoslash
abstract class Base {
  abstract getName(): string;
  printName() {}
}
// ---cut---
class Derived extends Base {
  getName() {
    return "world";
  }
}

const d = new Derived();
d.printName();
```

기본 클래스의 추상 멤버를 구현하지 않으면 오류가 발생합니다:

```ts twoslash
// @errors: 2515
abstract class Base {
  abstract getName(): string;
  printName() {}
}
// ---cut---
class Derived extends Base {
  // 아무것도 하는 것을 잊음
}
```

#### 추상 생성 시그니처

때때로 어떤 추상 클래스에서 파생된 클래스의 인스턴스를 생성하는 일부 클래스 생성자 함수를 받아들이고 싶습니다.

예를 들어, 다음 코드를 작성하고 싶을 수 있습니다:

```ts twoslash
// @errors: 2511
abstract class Base {
  abstract getName(): string;
  printName() {}
}
class Derived extends Base {
  getName() {
    return "";
  }
}
// ---cut---
function greet(ctor: typeof Base) {
  const instance = new ctor();
  instance.printName();
}
```

TypeScript는 추상 클래스를 인스턴스화하려고 한다고 올바르게 알려줍니다.
결국, `greet`의 정의가 주어지면, 추상 클래스를 생성하게 될 이 코드를 작성하는 것이 완전히 합법적입니다:

```ts twoslash
declare const greet: any, Base: any;
// ---cut---
// 나쁨!
greet(Base);
```

대신, 생성 시그니처가 있는 것을 받아들이는 함수를 작성하고 싶습니다:

```ts twoslash
// @errors: 2345
abstract class Base {
  abstract getName(): string;
  printName() {}
}
class Derived extends Base {
  getName() {
    return "";
  }
}
// ---cut---
function greet(ctor: new () => Base) {
  const instance = new ctor();
  instance.printName();
}
greet(Derived);
greet(Base);
```

이제 TypeScript는 어떤 클래스 생성자 함수를 호출할 수 있는지 올바르게 알려줍니다 - `Derived`는 구체적이므로 가능하지만, `Base`는 불가능합니다.

### 클래스 간의 관계

대부분의 경우, TypeScript의 클래스는 다른 타입과 같이 구조적으로 비교됩니다.

예를 들어, 이 두 클래스는 동일하기 때문에 서로 대신 사용할 수 있습니다:

```ts twoslash
class Point1 {
  x = 0;
  y = 0;
}

class Point2 {
  x = 0;
  y = 0;
}

// OK
const p: Point1 = new Point2();
```

마찬가지로, 명시적 상속이 없어도 클래스 간의 하위 타입 관계가 존재합니다:

```ts twoslash
// @strict: false
class Person {
  name: string;
  age: number;
}

class Employee {
  name: string;
  age: number;
  salary: number;
}

// OK
const p: Person = new Employee();
```

이것은 간단하게 들리지만, 다른 것보다 더 이상하게 보이는 몇 가지 경우가 있습니다.

빈 클래스에는 멤버가 없습니다.
구조적 타입 시스템에서 멤버가 없는 타입은 일반적으로 다른 모든 것의 슈퍼타입입니다.
따라서 빈 클래스를 작성하면(하지 마세요!), 어떤 것이든 그 자리에 사용할 수 있습니다:

```ts twoslash
class Empty {}

function fn(x: Empty) {
  // 'x'로 아무것도 할 수 없으므로, 하지 않겠습니다
}

// 모두 OK!
fn(window);
fn({});
fn(fn);
```

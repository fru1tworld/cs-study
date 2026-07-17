# 함수와 객체 타입

# 함수에 대해 더 알아보기

> **원문:** https://www.typescriptlang.org/docs/handbook/2/functions.html

함수는 모든 애플리케이션의 기본 구성 요소입니다. 로컬 함수든, 다른 모듈에서 가져온 함수든, 클래스의 메서드든 상관없습니다.
함수도 값이며, 다른 값과 마찬가지로 TypeScript는 함수를 호출하는 방법을 설명하는 다양한 방식을 제공합니다.
함수를 설명하는 타입을 작성하는 방법을 배워봅시다.

## 함수 타입 표현식

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

## 호출 시그니처

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

## 생성 시그니처

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

## 제네릭 함수

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

### 추론

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

### 제약 조건

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

### 제약된 값으로 작업하기

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

### 타입 인수 지정하기

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

### 좋은 제네릭 함수 작성 가이드라인

제네릭 함수를 작성하는 것은 재미있고, 타입 매개변수에 쉽게 빠져들 수 있습니다.
너무 많은 타입 매개변수를 갖거나 필요하지 않은 곳에 제약 조건을 사용하면 추론이 덜 성공적이 되어 함수 호출자를 좌절시킬 수 있습니다.

#### 타입 매개변수를 밀어내리기

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

#### 더 적은 타입 매개변수 사용하기

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

#### 타입 매개변수는 두 번 나타나야 함

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

## 선택적 매개변수

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

### 콜백의 선택적 매개변수

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

## 함수 오버로드

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

### 오버로드 시그니처와 구현 시그니처

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

### 좋은 오버로드 작성하기

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

## 함수에서 `this` 선언하기

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

## 알아두면 좋은 다른 타입들

함수 타입으로 작업할 때 자주 나타나는 몇 가지 추가 타입이 있습니다.
모든 타입과 마찬가지로, 어디서나 사용할 수 있지만, 이것들은 특히 함수의 맥락에서 관련이 있습니다.

### `void`

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

### `object`

특수 타입 `object`는 기본형(`string`, `number`, `bigint`, `boolean`, `symbol`, `null`, `undefined`)이 아닌 모든 값을 참조합니다.
이것은 _빈 객체 타입_ `{ }`과 다르며, 전역 타입 `Object`와도 다릅니다.
`Object`는 아마도 사용하지 않을 것입니다.

> `object`는 `Object`가 아닙니다. **항상** `object`를 사용하세요!

JavaScript에서 함수 값은 객체입니다: 속성을 가지고, 프로토타입 체인에 `Object.prototype`이 있고, `instanceof Object`이고, `Object.keys`를 호출할 수 있는 등입니다.
이러한 이유로 함수 타입은 TypeScript에서 `object`로 간주됩니다.

### `unknown`

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

### `never`

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

### `Function`

전역 타입 `Function`은 JavaScript의 모든 함수 값에 존재하는 `bind`, `call`, `apply` 및 기타 속성을 설명합니다.
또한 `Function` 타입의 값은 항상 호출할 수 있는 특별한 속성이 있습니다; 이러한 호출은 `any`를 반환합니다:

```ts twoslash
function doSomething(f: Function) {
  return f(1, 2, 3);
}
```

이것은 _타입이 지정되지 않은 함수 호출_이며, 안전하지 않은 `any` 반환 타입 때문에 일반적으로 피하는 것이 좋습니다.

임의의 함수를 받아들여야 하지만 호출할 의도가 없다면, `() => void` 타입이 일반적으로 더 안전합니다.

## 나머지 매개변수와 인수

### 나머지 매개변수

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

### 나머지 인수

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

## 매개변수 구조 분해

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

## 함수의 할당 가능성

### 반환 타입 `void`

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

# 객체 타입

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

# 함수에 대해 더 알아보기

함수는 모든 애플리케이션의 기본 구성 요소입니다. 로컬 함수든, 다른 모듈에서 가져온 함수든, 클래스의 메서드든 상관없습니다.
그들은 또한 값이며, 다른 값들처럼 TypeScript는 함수가 어떻게 호출될 수 있는지 설명하는 많은 방법을 가지고 있습니다.
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
타입은 TypeScript에 의해 _추론_ - 자동으로 선택 - 되었습니다.

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

두 오버로드가 같은 인수 수와 같은 반환 타입을 가지고 있기 때문에, 대신 오버로드되지 않은 버전의 함수를 작성할 수 있습니다:

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
또한 `Function` 타입의 값은 항상 호출할 수 있는 특별한 속성을 가지고 있습니다; 이러한 호출은 `any`를 반환합니다:

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

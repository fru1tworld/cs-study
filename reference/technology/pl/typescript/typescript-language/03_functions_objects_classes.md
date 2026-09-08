# TypeScript 함수, 객체 타입, 클래스

## 함수와 객체 타입

## 함수에 대해 더 알아보기

> **원문:** https://www.typescriptlang.org/docs/handbook/2/functions.html

로컬 함수, 다른 모듈에서 가져온 함수, 클래스의 메서드는 모두 애플리케이션을 구성하는 기본 요소다. TypeScript에서는 함수도 값으로 다루며, 어떤 인자로 호출하고 어떤 결과를 반환하는지 여러 방식으로 표현할 수 있다.

### 함수 타입 표현식

함수 타입 표현식은 매개변수와 반환 타입을 화살표 함수와 비슷한 문법으로 작성한다. 아래 `greeter`는 문자열을 받는 함수를 인수로 받아 호출한다.

```ts
function greeter(fn: (a: string) => void) {
  fn("Hello, World");
}

function printToConsole(s: string) {
  console.log(s);
}

greeter(printToConsole);
```

- `(a: string) => void` 구문: "`a`라는 이름의 `string` 타입 매개변수가 하나 있고, 반환 값이 없는 함수"를 의미
- 함수 선언과 마찬가지로, 매개변수 타입이 지정되지 않으면 암시적으로 `any`가 됨

> 매개변수 이름이 **필수**라는 점에 주의하세요. 함수 타입 `(string) => void`는 "`any` 타입의 `string`이라는 이름의 매개변수가 있는 함수"를 의미합니다!

- 타입 별칭을 사용해 함수 타입에 이름을 지정 가능

```ts
type GreetFunction = (a: string) => void;
function greeter(fn: GreetFunction) {
  // ...
}
```

### 호출 시그니처

JavaScript 함수에는 속성을 붙일 수 있지만 함수 타입 표현식에는 그 속성을 선언할 자리가 없다. 호출 방법과 속성을 함께 설명하려면 아래처럼 객체 타입 안에 호출 시그니처를 작성한다.

```ts
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

- 함수 타입 표현식과 비교 시 구문이 약간 다름 → 매개변수 목록과 반환 타입 사이에 `=>`가 아닌 `:`를 사용

### 생성 시그니처

`new`로 호출해 객체를 만드는 생성자는 호출 시그니처 앞에 `new`를 붙여 표현한다. 아래 타입은 문자열을 받아 `SomeObject`를 생성하는 호출을 정의한다.

```ts
type SomeObject = any;
type SomeConstructor = {
  new (s: string): SomeObject;
};
function fn(ctor: SomeConstructor) {
  return new ctor("hello");
}
```

- JavaScript의 `Date` 객체와 같은 일부 객체는 `new`와 함께 또는 없이 호출 가능
- 같은 타입에서 호출과 생성 시그니처를 임의로 결합 가능

```ts
interface CallOrConstruct {
  (n?: number): string;
  new (s: string): Date;
}

function fn(ctor: CallOrConstruct) {
  // `number` 타입의 인수를 `ctor`에 전달하면
  // `CallOrConstruct` 인터페이스의 첫 번째 정의와 일치합니다.
  console.log(ctor(10));
              // ctor(10)의 반환 타입: string (호출 시그니처 (n?: number) => string).

  // 마찬가지로, `string` 타입의 인수를 `ctor`에 전달하면
  // 인터페이스의 두 번째 정의와 일치합니다.
  console.log(new ctor("10"));
                  // new ctor("10")의 타입: Date (생성 시그니처 new (s: string) => Date).
}

fn(Date);
```

### 제네릭 함수

배열의 첫 원소를 반환하는 함수라면 반환 타입은 배열 원소의 타입과 연결되어야 한다. 먼저 `any[]`로 작성했을 때 어떤 정보가 사라지는지 살펴보자.

```ts
function firstElement(arr: any[]) {
  return arr[0];
}
```

이 함수는 첫 요소를 반환하지만 반환 타입이 `any`라서 배열 요소의 타입 정보를 잃는다. 입력 배열의 요소 타입과 반환 타입을 연결하려면 제네릭을 사용한다. 아래처럼 함수 시그니처에 타입 매개변수를 선언하면 두 값 사이의 관계를 표현할 수 있다.

```ts
function firstElement<Type>(arr: Type[]): Type | undefined {
  return arr[0];
}
```

- 타입 매개변수 `Type`을 함수에 추가하고 두 곳에서 사용 → 함수의 입력(배열)과 출력(반환 값) 사이에 링크가 생성됨
- 호출 시 더 구체적인 타입이 도출됨

```ts
declare function firstElement<Type>(arr: Type[]): Type | undefined;
// s는 타입 'string'
const s = firstElement(["a", "b", "c"]);
// n은 타입 'number'
const n = firstElement([1, 2, 3]);
// u는 타입 undefined
const u = firstElement([]);
```

#### 추론

- 이 샘플에서 `Type`을 지정할 필요가 없었음 → TypeScript가 타입을 추론(자동으로 선택)함
- 여러 타입 매개변수 사용도 가능함
- 예: `map`의 독립형 버전

```ts
// prettier-ignore
function map<Input, Output>(arr: Input[], func: (arg: Input) => Output): Output[] {
  return arr.map(func);
}

// 매개변수 'n'은 타입 'string'
// 'parsed'는 타입 'number[]'
const parsed = map(["1", "2", "3"], (n) => parseInt(n));
```

- 이 예제에서 TypeScript는 `Input` 타입 매개변수(주어진 `string` 배열에서)와 함수 표현식의 반환 값(`number`)을 기반으로 `Output` 타입 매개변수까지 모두 추론 가능

#### 제약 조건

제네릭 함수가 특정 속성을 사용해야 한다면 받아들일 타입에도 제약이 필요하다. 두 값 중 더 긴 것을 반환하는 함수는 숫자 타입의 `length` 속성이 있어야 한다. `extends`로 이 조건을 명시하면 함수 본문에서 `.length`에 접근할 수 있다.

```ts
// 의도한 오류: number에는 length 속성이 없어 longest(10, 100)에 전달할 수 없음.
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

- 이 예제의 흥미로운 점
  - TypeScript가 `longest`의 반환 타입을 추론하도록 허용함 → 반환 타입 추론은 제네릭 함수에서도 작동함
  - `Type`을 `{ length: number }`로 제약했기 때문에, `a`와 `b` 매개변수의 `.length` 속성에 접근 가능해짐
    - 타입 제약이 없었다면, 값이 length 속성이 없는 다른 타입일 수 있어 해당 속성에 접근 불가
  - `longerArray`와 `longerString`의 타입은 인수를 기반으로 추론됨 → 제네릭은 같은 타입으로 두 개 이상의 값을 관련시키는 것임
  - `longest(10, 100)` 호출은 `number` 타입에 `.length` 속성이 없어 거부됨

#### 제약된 값으로 작업하기

- 제네릭 제약 조건으로 작업할 때 흔히 발생하는 오류

```ts
// 의도한 오류: { length: number }는 제약을 만족하지만 임의의 Type을 반환한다고 보장할 수 없음.
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

`{ length: minimum }`은 제약 조건을 만족하지만 반환 타입 `Type`을 만족한다고 보장할 수는 없다. 이 함수는 `length`가 있는 아무 객체가 아니라, 전달받은 객체와 같은 타입을 반환한다고 선언했기 때문이다. 아래처럼 배열을 전달하면 반환값도 배열이어야 하므로 `length`만 가진 객체를 반환해서는 안 된다.

```ts
declare function minimumLength<Type extends { length: number }>(
  obj: Type,
  minimum: number
): Type;
// 'arr'은 값 { length: 6 }을 얻음
const arr = minimumLength([1, 2, 3], 6);
// 배열은 'slice' 메서드가 있지만
// 반환된 객체에는 없어서 여기서 충돌!
console.log(arr.slice(0));
```

#### 타입 인수 지정하기

- TypeScript는 일반적으로 제네릭 호출에서 의도된 타입 인수를 추론 가능하나, 항상 그런 것은 아님
- 예: 두 배열을 결합하는 함수

```ts
function combine<Type>(arr1: Type[], arr2: Type[]): Type[] {
  return arr1.concat(arr2);
}
```

- 일반적으로 일치하지 않는 배열로 이 함수를 호출하면 오류가 발생함

```ts
// 의도한 오류: number로 추론된 Type에 string 원소를 전달할 수 없음.
declare function combine<Type>(arr1: Type[], arr2: Type[]): Type[];
const arr = combine([1, 2, 3], ["hello"]);
```

- 의도적으로 이렇게 호출하고 싶다면, 수동으로 `Type`을 지정 가능

```ts
declare function combine<Type>(arr1: Type[], arr2: Type[]): Type[];
const arr = combine<string | number>([1, 2, 3], ["hello"]);
```

#### 좋은 제네릭 함수 작성 가이드라인

타입 매개변수는 값 사이의 타입 관계를 표현하는 데 사용한다. 매개변수가 불필요하게 많거나 제약이 과하면 추론이 어려워져 호출자가 타입 인수를 직접 지정해야 할 수 있다. 다음 예제에서는 이런 선언을 줄이는 방법을 살펴본다.

- 타입 매개변수를 밀어내리기

  - 유사해 보이는 두 가지 함수 작성 방법

  ```ts
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

  - 처음에는 동일해 보일 수 있으나, `firstElement1`이 훨씬 더 좋은 작성 방법임
    - `firstElement1`의 추론된 반환 타입은 `Type`
    - `firstElement2`의 추론된 반환 타입은 `any` → TypeScript가 호출 중에 요소를 해결하기 위해 "기다리기"보다 제약 타입을 사용해 `arr[0]` 표현식을 해결해야 하기 때문

  > **규칙**: 가능하면 제약하기보다 타입 매개변수 자체를 사용하세요

- 더 적은 타입 매개변수 사용하기

  - 유사한 또 다른 함수 쌍

  ```ts
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

  `Func`는 다른 값의 타입과 연결되지 않는다. 호출자가 타입 인수를 명시하려면 불필요한 인수 하나를 더 적어야 하고, 선언을 읽기도 어려워진다. 이 경우 `filter1`처럼 함수 타입을 직접 쓰면 충분하다.

  > **규칙**: 항상 가능한 한 적은 타입 매개변수를 사용하세요

- 타입 매개변수는 두 번 나타나야 함

  - 함수가 제네릭일 필요가 없다는 것을 잊는 경우가 있음

  ```ts
  function greet<Str extends string>(s: Str) {
    console.log("Hello, " + s);
  }

  greet("world");
  ```

  - 더 간단한 버전으로 쉽게 작성 가능

  ```ts
  function greet(s: string) {
    console.log("Hello, " + s);
  }
  ```

  - 타입 매개변수는 여러 값의 타입을 관련시키기 위한 것임
  - 타입 매개변수가 함수 시그니처에서 한 번만 사용되면, 아무것도 관련시키지 않음
    - 추론된 반환 타입도 포함됨 → 예를 들어, `Str`이 `greet`의 추론된 반환 타입의 일부라면, 인수와 반환 타입을 관련시키므로 작성된 코드에서 한 번만 나타나더라도 두 번 사용된 것으로 봄

  > **규칙**: 타입 매개변수가 한 위치에만 나타나면, 정말로 필요한지 강력하게 재고하세요

### 선택적 매개변수

- JavaScript에서 함수는 종종 가변적인 수의 인수를 받음
- 예: `number`의 `toFixed` 메서드는 선택적인 자릿수를 받음

```ts
function f(n: number) {
  console.log(n.toFixed()); // 0개의 인수
  console.log(n.toFixed(3)); // 1개의 인수
}
```

- TypeScript에서 `?`로 매개변수를 선택적으로 표시해 모델링 가능

```ts
function f(x?: number) {
  // ...
}
f(); // OK
f(10); // OK
```

선택적 매개변수 `x`는 전달되지 않으면 `undefined`가 되므로 함수 안에서는 `number | undefined`로 다룬다. 기본값을 지정하면 이 경우 사용할 값을 정할 수 있다.

```ts
function f(x = 10) {
  // ...
}
```

- `f`의 본문에서 `x`는 `number` 타입을 가짐 → `undefined` 인수는 `10`으로 대체되기 때문
- 매개변수가 선택적일 때, 호출자는 항상 `undefined`를 전달 가능 → "누락된" 인수를 시뮬레이션하는 것과 동일

```ts
declare function f(x?: number): void;
// 모두 OK
f();
f(10);
f(undefined);
```

#### 콜백의 선택적 매개변수

- 선택적 매개변수와 함수 타입 표현식을 배우면, 콜백을 호출하는 함수를 작성할 때 다음과 같은 실수를 저지르기 쉬움

```ts
function myForEach(arr: any[], callback: (arg: any, index?: number) => void) {
  for (let i = 0; i < arr.length; i++) {
    callback(arr[i], i);
  }
}
```

- `index?`를 선택적 매개변수로 작성할 때 일반적으로 의도하는 것: 아래 두 호출이 모두 합법적이길 원하는 것

```ts
// 이 두 호출은 허용됨. 선택적 인덱스를 사용하지 않거나 그대로 출력하므로 오류가 없음.
declare function myForEach(
  arr: any[],
  callback: (arg: any, index?: number) => void
): void;
myForEach([1, 2, 3], (a) => console.log(a));
myForEach([1, 2, 3], (a, i) => console.log(a, i));
```

하지만 `index?`는 호출자가 인덱스를 사용하지 않아도 된다는 뜻이 아니라, `myForEach`가 인덱스를 전달하지 않고 콜백을 호출할 수 있다는 뜻이다. 이 타입만 보면 아래와 같은 구현도 허용된다.

```ts
// 이 구현은 허용됨. index가 선택적이므로 callback을 인덱스 없이 호출할 수 있음.
function myForEach(arr: any[], callback: (arg: any, index?: number) => void) {
  for (let i = 0; i < arr.length; i++) {
    // 오늘은 인덱스를 제공하고 싶지 않음
    callback(arr[i]);
  }
}
```

타입 검사기는 콜백이 인덱스 없이 호출될 가능성도 고려한다. 따라서 아래처럼 `i`를 바로 사용하면 오류가 난다.

```ts
// 의도한 오류: i가 undefined일 수 있으므로 바로 toFixed()를 호출할 수 없음.
declare function myForEach(
  arr: any[],
  callback: (arg: any, index?: number) => void
): void;
myForEach([1, 2, 3], (a, i) => {
  console.log(i.toFixed());
});
```

- JavaScript에서 매개변수보다 더 많은 인수로 함수를 호출하면, 추가 인수는 단순히 무시됨
- TypeScript도 같은 방식으로 동작함
- (같은 타입의) 더 적은 매개변수를 가진 함수는 항상 더 많은 매개변수를 가진 함수의 자리를 차지 가능

> **규칙**: 콜백에 대한 함수 타입을 작성할 때, 해당 인수를 전달하지 _않고_ 함수를 _호출_하려는 의도가 아니라면 선택적 매개변수를 _절대_ 작성하지 마세요

### 함수 오버로드

- 일부 JavaScript 함수는 다양한 인수 수와 타입으로 호출 가능
- 예: 타임스탬프(하나의 인수)나 월/일/연도 지정(세 개의 인수)을 받아 `Date`를 생성하는 함수
- TypeScript에서 오버로드 시그니처를 작성해 다른 방식으로 호출할 수 있는 함수를 지정 가능
  - 몇 개의 함수 시그니처(보통 두 개 이상)를 작성한 다음, 함수 본문을 작성

```ts
// 의도한 오류: makeDate는 인수 1개 또는 3개만 받으므로 인수 2개로 호출할 수 없음.
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

처음 두 줄이 오버로드 시그니처이며, 각각 인수 하나와 세 개를 받는 호출을 정의한다. 그 아래 구현 시그니처는 두 호출을 모두 처리할 수 있도록 작성하지만 외부에서 직접 호출할 수는 없다. 따라서 구현에 선택적 매개변수가 두 개 있더라도 인수 두 개를 전달하는 호출은 허용되지 않는다.

#### 오버로드 시그니처와 구현 시그니처

구현 본문에 매개변수가 없다고 해서 인수 없는 호출이 허용되는 것은 아니다. 호출 가능 여부는 외부에 선언한 오버로드로 결정된다.

```ts
// 의도한 오류: 외부에 공개한 fn 시그니처는 string 인수 1개를 요구함.
function fn(x: string): void;
function fn() {
  // ...
}
// 0개의 인수로 호출할 수 있을 것으로 예상
fn();
```

- 다시 말해, 함수 본문을 작성하는 데 사용된 시그니처는 외부에서 "볼" 수 없음

> _구현_의 시그니처는 외부에서 보이지 않습니다.

> 오버로드된 함수를 작성할 때, 항상 함수 구현 위에 _두 개_ 이상의 시그니처가 있어야 합니다.

- 구현 시그니처도 오버로드 시그니처와 호환돼야 함
- 예: 구현 시그니처가 올바른 방식으로 오버로드와 일치하지 않아 오류가 발생하는 함수

```ts
// 의도한 오류: string 인수를 받는 오버로드와 boolean만 받는 구현이 호환되지 않음.
function fn(x: boolean): void;
// 인수 타입이 올바르지 않음
function fn(x: string): void;
function fn(x: boolean) {}
```

```ts
// 의도한 오류: boolean 반환을 선언한 오버로드와 string을 반환하는 구현이 호환되지 않음.
function fn(x: string): string;
// 반환 타입이 올바르지 않음
function fn(x: number): boolean;
function fn(x: string | number) {
  return "oops";
}
```

#### 좋은 오버로드 작성하기

- 제네릭과 마찬가지로, 함수 오버로드를 사용할 때 따라야 할 몇 가지 가이드라인이 있음
- 이러한 원칙을 따르면 함수를 더 쉽게 호출, 이해, 구현 가능
- 문자열이나 배열의 길이를 반환하는 함수 예시

```ts
function len(s: string): number;
function len(arr: any[]): number;
function len(x: any) {
  return x.length;
}
```

문자열이나 배열을 각각 전달할 수는 있다. 하지만 인수의 타입이 `string | any[]`이면 어느 한 오버로드에도 맞지 않는다. TypeScript가 한 번의 호출을 하나의 오버로드로 해석하기 때문이다.

```ts
// 의도한 오류: string | number[] 인수 전체를 받는 len 오버로드가 없음.
declare function len(s: string): number;
declare function len(arr: any[]): number;
len(""); // OK
len([0]); // OK
len(Math.random() > 0.5 ? "hello" : [0]);
```

- 두 오버로드가 같은 인수 수와 같은 반환 타입을 가지므로, 대신 오버로드되지 않은 버전의 함수 작성 가능

```ts
function len(x: any[] | string) {
  return x.length;
}
```

- 이것이 훨씬 나음 → 호출자는 어느 종류의 값으로든 호출 가능하며, 올바른 구현 시그니처를 알아낼 필요도 없음

> 가능하면 항상 오버로드보다 유니온 타입의 매개변수를 선호하세요

### 함수에서 `this` 선언하기

- TypeScript는 코드 흐름 분석을 통해 함수에서 `this`가 무엇이어야 하는지 추론함

```ts
const user = {
  id: 123,

  admin: false,
  becomeAdmin: function () {
    this.admin = true;
  },
};
```

위 예제에서는 `this`가 `user`를 가리킨다고 추론한다. 콜백처럼 호출하는 쪽에서 `this`를 정하는 경우에는 타입을 직접 지정해야 할 수 있다. TypeScript는 JavaScript에서 매개변수 이름으로 사용할 수 없는 `this`를 타입 선언용 매개변수로 사용한다.

```ts
interface User {
  id: number;
  admin: boolean;
}
declare const getDB: () => DB;
interface DB {
  filterUsers(filter: (this: User) => boolean): User[];
}

const db = getDB();
const admins = db.filterUsers(function (this: User) {
  return this.admin;
});
```

- 이 패턴은 콜백 스타일 API에서 흔함 → 다른 객체가 일반적으로 함수가 호출되는 시기를 제어함
- 이 동작을 얻으려면 화살표 함수가 아닌 `function`을 사용해야 함

```ts
// 의도한 오류: 화살표 함수는 전역 this를 캡처하며, 그 타입에는 admin 속성 접근을 허용하는 인덱스 시그니처가 없음.
interface User {
  id: number;
  admin: boolean;
}
declare const getDB: () => DB;
interface DB {
  filterUsers(filter: (this: User) => boolean): User[];
}

const db = getDB();
const admins = db.filterUsers(() => this.admin);
```

### 알아두면 좋은 다른 타입들

- 함수 타입으로 작업할 때 자주 나타나는 몇 가지 추가 타입이 있음
- 모든 타입과 마찬가지로 어디서나 사용 가능하지만, 이것들은 특히 함수 맥락에서 관련이 있음

#### `void`

- `void`는 값을 반환하지 않는 함수의 반환 값을 나타냄
- 함수에 `return` 문이 없거나, return 문에서 명시적 값을 반환하지 않을 때마다 추론되는 타입임

```ts
// 추론된 반환 타입은 void
function noop() {
  return;
}
```

- JavaScript에서 값을 반환하지 않는 함수는 암시적으로 값 `undefined`를 반환함
- 그러나 TypeScript에서 `void`와 `undefined`는 같은 것이 아님 → 자세한 내용은 이 챕터 끝에서 다룸

> `void`는 `undefined`와 같지 않습니다.

#### `object`

- 특수 타입 `object`는 기본형(`string`, `number`, `bigint`, `boolean`, `symbol`, `null`, `undefined`)이 아닌 모든 값을 가리킴
- 이것은 빈 객체 타입 `{ }`과 다르며, 전역 타입 `Object`와도 다름
- `Object`는 사용하지 않는 것을 권장함

> `object`는 `Object`가 아닙니다. **항상** `object`를 사용하세요!

- JavaScript에서 함수 값은 객체임 → 속성을 가지고, 프로토타입 체인에 `Object.prototype`이 있고, `instanceof Object`이고, `Object.keys`를 호출할 수 있는 등
- 이러한 이유로 함수 타입은 TypeScript에서 `object`로 간주됨

#### `unknown`

- `unknown` 타입은 어떤 값이든 나타냄
- `any` 타입과 유사하지만, `unknown` 값으로 무언가를 하는 것이 합법적이지 않아 더 안전함

```ts
// 의도한 오류: unknown인 a의 타입을 좁히기 전에 b 속성에 접근할 수 없음.
function f1(a: any) {
  a.b(); // OK
}
function f2(a: unknown) {
  a.b();
}
```

- 함수 본문에 `any` 값이 없이 어떤 값이든 받는 함수를 설명 가능하므로 함수 타입을 설명할 때 유용함
- 반대로, unknown 타입의 값을 반환하는 함수도 설명 가능함

```ts
declare const someRandomString: string;
function safeParse(s: string): unknown {
  return JSON.parse(s);
}

// 'obj'를 조심해야 함!
const obj = safeParse(someRandomString);
```

#### `never`

- 일부 함수는 절대 값을 반환하지 않음

```ts
function fail(msg: string): never {
  throw new Error(msg);
}
```

- `never` 타입은 절대 관찰되지 않는 값을 나타냄
- 반환 타입에서 이것은 함수가 예외를 발생시키거나 프로그램 실행을 종료함을 의미함
- `never`는 TypeScript가 유니온에 아무것도 남지 않았다고 결정할 때도 나타남

```ts
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

- 전역 타입 `Function`은 JavaScript의 모든 함수 값에 존재하는 `bind`, `call`, `apply` 및 기타 속성을 설명함
- `Function` 타입의 값은 항상 호출 가능한 특별한 속성이 있음 → 이러한 호출은 `any`를 반환함

```ts
function doSomething(f: Function) {
  return f(1, 2, 3);
}
```

- 이것은 타입이 지정되지 않은 함수 호출이며, 안전하지 않은 `any` 반환 타입 때문에 일반적으로 회피 권장
- 임의의 함수를 받아들여야 하지만 호출할 의도가 없다면, `() => void` 타입이 일반적으로 더 안전함

### 나머지 매개변수와 인수

#### 나머지 매개변수

- 선택적 매개변수나 오버로드를 사용해 다양한 고정 인수 수를 받아들이는 함수를 만드는 것 외에도, 나머지 매개변수를 사용하면 무한한 수의 인수를 받는 함수도 정의 가능
- 나머지 매개변수는 다른 모든 매개변수 뒤에 나타나며, `...` 구문을 사용함

```ts
function multiply(n: number, ...m: number[]) {
  return m.map((x) => n * x);
}
// 'a'는 값 [10, 20, 30, 40]을 얻음
const a = multiply(10, 1, 2, 3, 4);
```

- TypeScript에서 이러한 매개변수의 타입 어노테이션은 암시적으로 `any`가 아닌 `any[]`이며, 주어진 모든 타입 어노테이션은 `Array<T>` 또는 `T[]` 형태이거나, 튜플 타입(나중에 다룸)이어야 함

#### 나머지 인수

- 반대로, 스프레드 구문을 사용해 반복 가능한 객체(예: 배열)에서 가변적인 수의 인수를 제공 가능
- 예: 배열의 `push` 메서드는 여러 인수를 받음

```ts
const arr1 = [1, 2, 3];
const arr2 = [4, 5, 6];
arr1.push(...arr2);
```

- TypeScript는 일반적으로 배열이 불변이라고 가정하지 않음 → 이는 놀라운 동작을 초래할 수 있음

```ts
// 의도한 오류: Math.atan2에 펼칠 인수는 길이가 정해진 튜플이어야 하며 number[]로는 인수 개수를 보장할 수 없음.
// 추론된 타입은 number[] -- "0개 이상의 숫자를 가진 배열",
// 구체적으로 두 개의 숫자가 아님
const args = [8, 5];
const angle = Math.atan2(...args);
```

- 이 상황에 대한 최선의 수정은 코드에 따라 약간 다르지만, 일반적으로 `const` 컨텍스트가 가장 간단한 해결책임

```ts
// 2-길이 튜플로 추론됨
const args = [8, 5] as const;
// OK
const angle = Math.atan2(...args);
```

- 나머지 인수를 사용하려면 이전 런타임을 대상으로 할 때 [`downlevelIteration`](https://www.typescriptlang.org/tsconfig#downlevelIteration)을 켜야 할 필요가 있을 수 있음

### 매개변수 구조 분해

- 매개변수 구조 분해를 사용하면 인수로 제공된 객체를 함수 본문에서 하나 이상의 로컬 변수로 편리하게 풀어낼 수 있음
- JavaScript에서는 다음과 같음

```js
function sum({ a, b, c }) {
  console.log(a + b + c);
}
sum({ a: 10, b: 3, c: 9 });
```

- 객체에 대한 타입 어노테이션은 구조 분해 구문 뒤에 옴

```ts
function sum({ a, b, c }: { a: number; b: number; c: number }) {
  console.log(a + b + c);
}
```

- 약간 장황해 보일 수 있지만, 여기서도 명명된 타입 사용 가능

```ts
// 이전 예제와 동일
type ABC = { a: number; b: number; c: number };
function sum({ a, b, c }: ABC) {
  console.log(a + b + c);
}
```

### 함수의 할당 가능성

#### 반환 타입 `void`

`() => void` 타입에 함수를 할당할 때는 그 함수가 값을 반환해도 된다. 이 타입으로 호출한 쪽에서 반환 값을 사용하지 않는다는 의미이므로, 다음 세 구현은 모두 유효하다.

```ts
type voidFunc = () => void;

const f1: voidFunc = () => {
  return true;
};

const f2: voidFunc = () => true;

const f3: voidFunc = function () {
  return true;
};
```

- 이러한 함수 중 하나의 반환 값이 다른 변수에 할당될 때도, `void` 타입을 유지함

```ts
type voidFunc = () => void;

const f1: voidFunc = () => {
  return true;
};

const f2: voidFunc = () => true;

const f3: voidFunc = function () {
  return true;
};
const v1 = f1();

const v2 = f2();

const v3 = f3();
```

이 규칙 덕분에 `push`를 `forEach` 콜백에서 바로 호출할 수 있다. `push`는 숫자를 반환하지만 `forEach`는 콜백의 반환 값을 사용하지 않는다.

```ts
const src = [1, 2, 3];
const dst = [0];

src.forEach((el) => dst.push(el));
```

함수 정의 자체에 반환 타입 `void`를 명시한 경우는 다르다. 아래처럼 직접 `: void`를 선언한 함수에서 `true`를 반환하면 오류가 난다.

```ts
function f2(): void {
  // @ts-expect-error
  return true;
}

const f3 = function (): void {
  // @ts-expect-error
  return true;
};
```

- `void`에 대해 더 알아보려면 다음 문서 항목 참조

- [FAQ - "왜 void가 아닌 것을 반환하는 함수가 void를 반환하는 함수에 할당 가능한가요?"](https://github.com/Microsoft/TypeScript/wiki/FAQ#why-are-functions-returning-non-void-assignable-to-function-returning-void)

## 객체 타입

> **원문:** https://www.typescriptlang.org/docs/handbook/2/objects.html

JavaScript 객체로 묶어 전달하는 데이터의 구조는 객체 타입으로 표현한다. 함수 매개변수에 직접 작성하거나, 인터페이스와 타입 별칭으로 이름을 붙일 수 있다.

```ts
function greet(person: { name: string; age: number }) {
  // person은 name: string과 age: number를 가진 객체를 받음.
  return "Hello " + person.name;
}
```

- 또는 인터페이스를 사용해 이름을 지정 가능

```ts
interface Person {
  // Person이라는 이름의 인터페이스 선언.
  name: string;
  age: number;
}

function greet(person: Person) {
  return "Hello " + person.name;
}
```

- 또는 타입 별칭으로도 가능

```ts
type Person = {
  // Person이라는 이름의 타입 별칭 선언.
  name: string;
  age: number;
};

function greet(person: Person) {
  return "Hello " + person.name;
}
```

- 위 세 가지 예제 모두, `name`(`string`이어야 함)과 `age`(`number`여야 함) 속성을 포함하는 객체를 받는 함수를 작성한 것임

### 빠른 참조

- 일상적인 구문을 한눈에 빠르게 보고 싶다면 [`type`과 `interface`](https://www.typescriptlang.org/cheatsheets)에 대한 치트시트 참고

### 속성 수정자

- 객체 타입의 각 속성은 타입, 속성이 선택적인지 여부, 속성을 쓸 수 있는지 여부와 같은 몇 가지 항목을 지정 가능

#### 선택적 속성

- 많은 경우 속성이 설정되어 있을 수도 있는 객체를 다루게 됨
- 그러한 경우, 속성 이름 끝에 물음표(`?`)를 추가해 해당 속성을 선택적으로 표시 가능

```ts
interface Shape {}
declare function getShape(): Shape;

interface PaintOptions {
  shape: Shape;
  xPos?: number;
  // xPos는 선택적 number 속성.
  yPos?: number;
  // yPos는 선택적 number 속성.
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

`xPos`와 `yPos`는 생략할 수 있지만, 전달한다면 `number`여야 한다. 따라서 위 호출은 모두 유효하다. 값을 읽을 때는 생략된 경우도 고려해야 하므로 [`strictNullChecks`](https://www.typescriptlang.org/tsconfig#strictNullChecks)가 켜져 있으면 `undefined` 가능성이 타입에 반영된다.

```ts
interface Shape {}
declare function getShape(): Shape;

interface PaintOptions {
  shape: Shape;
  xPos?: number;
  yPos?: number;
}

function paintShape(opts: PaintOptions) {
  let xPos = opts.xPos;
  // opts.xPos 및 xPos의 타입: number | undefined.
  let yPos = opts.yPos;
  // opts.yPos 및 yPos의 타입: number | undefined.
  // ...
}
```

- JavaScript에서 속성이 설정되지 않았더라도 여전히 접근 가능함 → 값 `undefined`를 얻을 뿐
- `undefined`를 특별히 검사해 처리 가능함

```ts
interface Shape {}
declare function getShape(): Shape;

interface PaintOptions {
  shape: Shape;
  xPos?: number;
  yPos?: number;
}

function paintShape(opts: PaintOptions) {
  let xPos = opts.xPos === undefined ? 0 : opts.xPos;
  // xPos의 추론 타입: number.
  let yPos = opts.yPos === undefined ? 0 : opts.yPos;
  // yPos의 추론 타입: number.
  // ...
}
```

- 지정되지 않은 값에 대해 기본값을 설정하는 이 패턴은 매우 흔해서 JavaScript에는 이를 지원하는 구문이 있음

```ts
interface Shape {}
declare function getShape(): Shape;

interface PaintOptions {
  shape: Shape;
  xPos?: number;
  yPos?: number;
}

function paintShape({ shape, xPos = 0, yPos = 0 }: PaintOptions) {
  console.log("x coordinate at", xPos);
  // 기본값 적용 후 xPos의 타입: number.
  console.log("y coordinate at", yPos);
  // 기본값 적용 후 yPos의 타입: number.
  // ...
}
```

- 여기서 `paintShape`의 매개변수에 [구조 분해 패턴](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Destructuring_assignment)을 사용하고, `xPos`와 `yPos`에 [기본값](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Destructuring_assignment#Default_values)을 제공함
- 이제 `xPos`와 `yPos`는 `paintShape`의 본문 내에서 확실히 존재하지만, `paintShape`의 모든 호출자에게는 선택 사항임

> 구조 분해 패턴 내에 타입 어노테이션을 배치할 방법이 현재 없습니다.

> 이는 다음 구문이 JavaScript에서 이미 다른 의미를 가지기 때문입니다.

>

> ```ts
> // 컴파일러 설정: noImplicitAny: false
> // 의도한 오류: 구조 분해로 만든 변수 이름은 Shape와 number이므로 shape와 xPos를 찾을 수 없음.
> interface Shape {}
> declare function render(x: unknown);
> function draw({ shape: Shape, xPos: number = 100 /*...*/ }) {
>   render(shape);
>   render(xPos);
> }
> ```

>

> 객체 구조 분해 패턴에서 `shape: Shape`는 "속성 `shape`를 가져와서 `Shape`라는 이름의 변수로 로컬에서 재정의"를 의미합니다.

> 마찬가지로 `xPos: number`는 매개변수의 `xPos`를 기반으로 값을 가진 `number`라는 이름의 변수를 만듭니다.

#### `readonly` 속성

- TypeScript에서 속성을 `readonly`로 표시 가능
- 런타임에 동작을 변경하지는 않지만, `readonly`로 표시된 속성은 타입 검사 중에 쓸 수 없음

```ts
// 의도한 오류: readonly 속성 obj.prop에 재할당할 수 없음.
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

`readonly`는 해당 속성에 다른 값을 다시 할당할 수 없다는 뜻이다. 속성이 가리키는 객체 전체를 불변으로 만들지는 않으므로, 아래처럼 내부 속성은 변경할 수 있다.

```ts
// 의도한 오류: readonly 속성 home.resident 자체를 교체할 수 없음.
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

`readonly`는 개발 중 객체를 어떻게 사용할지 표현하는 도구다. TypeScript는 두 타입의 호환성을 검사할 때 속성의 `readonly` 여부를 고려하지 않으므로, 같은 객체를 가리키는 다른 참조를 통해 값이 변경될 수도 있다.

```ts
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

- [매핑 수정자](https://www.typescriptlang.org/docs/handbook/2/mapped-types.html#mapping-modifiers)를 사용해 `readonly` 속성 제거 가능

#### 인덱스 시그니처

- 때때로 타입의 모든 속성 이름을 미리 알지 못하지만, 값의 형태는 알고 있는 경우가 있음
- 이러한 경우 인덱스 시그니처를 사용해 가능한 값의 타입을 설명 가능

```ts
declare function getStringArray(): StringArray;
interface StringArray {
  [index: number]: string;
}

const myArray: StringArray = getStringArray();
const secondItem = myArray[1];
// secondItem의 추론 타입: string.
```

- 위 예시: 인덱스 시그니처를 가진 `StringArray` 인터페이스
- 이 인덱스 시그니처는 `StringArray`가 `number`로 인덱싱되면 `string`을 반환한다고 명시함
- 인덱스 시그니처 속성에는 `string`, `number`, `symbol`, 템플릿 문자열 패턴, 이들로만 구성된 유니온 타입만 허용됨

- 여러 유형의 인덱서 지원

  - 여러 유형의 인덱서를 함께 지원 가능
  - `number`와 `string` 인덱서를 모두 사용하면 숫자 인덱서의 반환 타입은 문자열 인덱서의 반환 타입의 하위 타입이어야 함
    - JavaScript는 `number` 인덱스를 실제 객체에 적용하기 전에 `string`으로 변환함
    - `100`(`number`)으로 인덱싱하는 것은 `"100"`(`string`)으로 인덱싱하는 것과 같으므로 두 반환 타입이 일관돼야 함

  ```ts
  // 의도한 오류: 숫자 인덱서의 Animal은 문자열 인덱서의 Dog에 할당할 수 없음.
  // 컴파일러 설정: strictPropertyInitialization: false
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

  - 문자열 인덱스 시그니처가 "사전" 패턴을 설명하는 강력한 방법이지만, 모든 속성이 반환 타입과 일치하도록 강제함
    - 문자열 인덱스가 `obj.property`도 `obj["property"]`로 사용 가능하다고 선언하기 때문
  - 다음 예제에서 `name`의 타입은 문자열 인덱스의 타입과 일치하지 않아, 타입 검사기가 오류를 제공함

  ```ts
  // 의도한 오류: name의 string 타입은 문자열 인덱서가 요구하는 number와 호환되지 않음.
  interface NumberDictionary {
    [index: string]: number;

    length: number; // ok
    name: string;
  }
  ```

  - 그러나 인덱스 시그니처가 속성 타입의 유니온이면 다른 타입의 속성도 허용됨

  ```ts
  interface NumberOrStringDictionary {
    [index: string]: number | string;
    length: number; // ok, length는 number
    name: string; // ok, name은 string
  }
  ```

  - 마지막으로, 인덱스에 할당을 방지하기 위해 인덱스 시그니처를 `readonly`로 만들 수 있음

  ```ts
  declare function getReadOnlyStringArray(): ReadonlyStringArray;
  // 의도한 오류: readonly 인덱스 시그니처를 통해 myArray[2]에 쓸 수 없음.
  interface ReadonlyStringArray {
    readonly [index: number]: string;
  }

  let myArray: ReadonlyStringArray = getReadOnlyStringArray();
  myArray[2] = "Mallory";
  ```

  - 인덱스 시그니처가 `readonly`이기 때문에 `myArray[2]`를 설정 불가

### 초과 속성 검사

- 객체가 타입에 할당되는 위치와 방법은 타입 시스템에서 차이를 만들 수 있음
- 핵심 예 중 하나가 초과 속성 검사임 → 객체가 생성되고 생성 중에 객체 타입에 할당될 때 객체를 더 철저하게 검증함

```ts
// 의도한 오류: 객체 리터럴의 colour는 SquareConfig에 없는 속성임.
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

인수에는 `color` 대신 `colour`가 들어 있다. `width`의 타입은 맞고 `color`는 선택적이지만, 이 오타 때문에 의도한 색상이 적용되지 않는다. TypeScript는 객체 리터럴을 변수에 할당하거나 인수로 전달할 때 대상 타입에 없는 속성을 검사해 이런 실수를 잡는다.

```ts
// 의도한 오류: 객체 리터럴의 colour는 SquareConfig에 없는 속성임.
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

- 이러한 검사를 피하는 것은 실제로 매우 간단함
- 가장 쉬운 방법: 타입 단언 사용

```ts
// 이 예제는 타입 단언으로 초과 속성 검사를 피하므로 opacity 때문에 오류가 나지 않음.
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
let mySquare = createSquare({ width: 100, opacity: 0.5 } as SquareConfig);
```

- 더 나은 접근 방식: 객체가 특별한 방식으로 사용되는 일부 추가 속성을 가질 수 있다고 확신하는 경우 문자열 인덱스 시그니처를 추가하는 것
- `SquareConfig`가 위의 타입으로 `color`와 `width` 속성을 가질 수 있지만, 그 외의 속성도 개수 제한 없이 가질 수 있다면, 다음과 같이 정의 가능

```ts
interface SquareConfig {
  color?: string;
  width?: number;
  [propName: string]: unknown;
}
```

- 여기서 `SquareConfig`가 원하는 수의 속성을 가질 수 있으며, `color`나 `width`가 아닌 한 타입은 중요하지 않다고 명시하는 것임
- 이러한 검사를 피하는 마지막 방법: 약간 놀랍게도 객체를 다른 변수에 할당하는 것
  - `squareOptions`는 초과 속성 검사를 받지 않으므로, 컴파일러가 오류를 제공하지 않음

```ts
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
let squareOptions = { colour: "red", width: 100 };
let mySquare = createSquare(squareOptions);
```

- 위 해결 방법은 `squareOptions`와 `SquareConfig` 사이에 공통 속성이 있는 한 작동함(이 예제에서는 `width` 속성)
- 변수가 공통 객체 속성이 없으면 실패함

```ts
// 의도한 오류: squareOptions와 SquareConfig에 공통 속성이 없어 인수로 전달할 수 없음.
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
let squareOptions = { colour: "red" };
let mySquare = createSquare(squareOptions);
```

검사를 피할 수 있다는 것이 이 예제의 오타를 그대로 두어도 된다는 뜻은 아니다. 초과 속성 오류는 실제 버그를 가리키는 경우가 많다. 복잡한 객체에서 이런 기법이 필요할 수도 있지만, 옵션 객체라면 먼저 선언이 의도한 입력을 정확히 표현하는지 살펴본다. `color`와 `colour`를 모두 허용하려는 API라면 `SquareConfig`에 그 의도를 반영해야 한다.

### 타입 확장하기

- 다른 타입의 더 구체적인 버전일 수 있는 타입을 가지는 것은 흔한 패턴임
- 예: 미국에서 편지와 소포를 보내는 데 필요한 필드를 설명하는 `BasicAddress` 타입

```ts
interface BasicAddress {
  name?: string;
  street: string;
  city: string;
  country: string;
  postalCode: string;
}
```

- 일부 상황에서는 이것으로 충분하지만, 주소의 건물에 여러 유닛이 있는 경우 종종 유닛 번호가 연관됨
- 그런 다음 `AddressWithUnit`을 설명 가능함

```ts
interface AddressWithUnit {
  name?: string;
  unit: string;
// 기존 주소에 추가한 unit: string 속성.
  street: string;
  city: string;
  country: string;
  postalCode: string;
}
```

필드 하나를 추가하려고 주소의 모든 속성을 다시 선언할 필요는 없다. `BasicAddress`를 확장하면 `AddressWithUnit`에는 새 필드인 `unit`만 작성하면 된다.

```ts
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

- `interface`의 `extends` 키워드를 사용하면 다른 명명된 타입에서 멤버를 효과적으로 복사하고 원하는 새 멤버를 추가 가능
- 이것은 작성해야 하는 타입 선언 상용구의 양을 줄이고, 같은 속성의 여러 다른 선언이 관련될 수 있다는 의도를 알리는 데 유용함
  - 예: `AddressWithUnit`은 `street` 속성을 반복할 필요가 없었고, `street`가 `BasicAddress`에서 유래하므로 독자는 두 타입이 어떤 방식으로 관련되어 있다는 것을 알게 됨
- `interface`는 여러 타입에서 확장할 수도 있음

```ts
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

- `interface`를 사용하면 다른 타입을 확장해 새 타입을 구축 가능함
- TypeScript는 주로 기존 객체 타입을 결합하는 데 사용되는 교차 타입이라는 또 다른 구문도 제공함
- 교차 타입은 `&` 연산자를 사용해 정의됨

```ts
interface Colorful {
  color: string;
}
interface Circle {
  radius: number;
}

type ColorfulCircle = Colorful & Circle;
```

- 여기서 `Colorful`과 `Circle`을 교차하여 `Colorful` 과 `Circle`의 모든 멤버를 가진 새 타입을 생성함

```ts
// 의도한 오류: raidus는 Colorful & Circle에 없는 속성이며 필요한 속성 이름은 radius임.
interface Colorful {
  color: string;
}
interface Circle {
  radius: number;
}
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

- 유사하지만 실제로는 미묘하게 다른 두 가지 타입 결합 방법을 위에서 다룸
  - 인터페이스: `extends` 절을 사용해 다른 타입에서 확장 가능
  - 교차: 유사한 작업을 수행하고 타입 별칭으로 결과에 이름 지정 가능
- 둘 사이의 주요 차이점은 충돌이 처리되는 방식임 → 이 차이점이 일반적으로 인터페이스와 교차 타입의 타입 별칭 중 하나를 선택하는 주요 이유가 됨
- 인터페이스가 같은 이름으로 정의되면, TypeScript는 속성이 호환되면 병합을 시도함
  - 속성이 호환되지 않으면(즉, 같은 속성 이름이지만 다른 타입), TypeScript는 오류를 발생시킴
- 교차 타입의 경우, 다른 타입의 속성이 자동으로 병합됨
  - 타입이 나중에 사용될 때, TypeScript는 속성이 두 타입을 동시에 만족하기를 기대함 → 예상치 못한 결과를 생성할 수 있음
- 예: 다음 코드는 속성이 호환되지 않아 오류를 발생시킴

```ts
interface Person {
  name: string;
}

interface Person {
  name: number;
}
```

- 대조적으로, 다음 코드는 컴파일되지만, `never` 타입이 됨

```ts
interface Person1 {
  name: string;
}

interface Person2 {
  name: number;
}

type Staff = Person1 & Person2

declare const staffer: Staff;
staffer.name;
// staffer.name의 타입: never (string & number).
```

- 이 경우, Staff는 name 속성이 string과 number 모두여야 하므로, 속성이 `never` 타입이 됨

### 제네릭 객체 타입

- 어떤 값이든 포함할 수 있는 `Box` 타입 예시 → `string`, `number`, `Giraffe` 등

```ts
interface Box {
  contents: any;
}
```

- 현재 `contents` 속성은 `any`로 타입화되어 있어 작동하지만, 나중에 사고로 이어질 수 있음
- 대신 `unknown`을 사용 가능하지만, `contents`의 타입을 이미 알고 있는 경우 예방 검사를 수행하거나 오류가 발생하기 쉬운 타입 단언을 사용해야 함

```ts
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

- 타입 안전 접근 방식 중 하나: 모든 `contents` 타입에 대해 다른 `Box` 타입을 스캐폴드하는 것

```ts
// 각 인터페이스를 따로 선언하는 이 예제에는 타입 할당 오류가 없음.
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

- 하지만 이것은 이러한 타입에서 작동하기 위해 다른 함수 또는 함수의 오버로드를 만들어야 한다는 것을 의미함

```ts
interface NumberBox {
  contents: number;
}

interface StringBox {
  contents: string;
}

interface BooleanBox {
  contents: boolean;
}
function setContents(box: StringBox, newContents: string): void;
function setContents(box: NumberBox, newContents: number): void;
function setContents(box: BooleanBox, newContents: boolean): void;
function setContents(box: { contents: any }, newContents: any) {
  box.contents = newContents;
}
```

각 박스와 오버로드는 구조가 거의 같지만 담을 값의 타입이 늘어날 때마다 선언을 추가해야 한다. 이 반복을 줄이려면 아래처럼 타입 매개변수를 받는 제네릭 `Box`를 정의한다.

```ts
interface Box<Type> {
  contents: Type;
}
```

- 이것을 "`Type`의 `Box`는 `contents`가 `Type` 타입인 것"으로 읽을 수 있음
- 나중에 `Box`를 참조할 때, `Type` 대신 타입 인수를 제공해야 함

```ts
interface Box<Type> {
  contents: Type;
}
let box: Box<string>;
```

`Box<string>`에서 타입 인수 `string`은 선언의 `Type` 자리에 들어간다. 따라서 `contents`가 `string`인 객체 타입이 되어 앞서 정의한 `StringBox`와 같은 방식으로 사용한다.

```ts
interface Box<Type> {
  contents: Type;
}
interface StringBox {
  contents: string;
}

let boxA: Box<string> = { contents: "hello" };
boxA.contents;
// boxA의 타입: Box<string>, boxA.contents의 타입: string.

let boxB: StringBox = { contents: "world" };
boxB.contents;
// boxB의 타입: StringBox, boxB.contents의 타입: string.
```

- `Box`는 `Type`을 무엇으로든 대체할 수 있어 재사용 가능함 → 새 타입에 대해 박스가 필요할 때, 새 `Box` 타입을 전혀 선언할 필요 없음(원한다면 확실히 할 수 있지만)

```ts
interface Box<Type> {
  contents: Type;
}

interface Apple {
  // ....
}

// '{ contents: Apple }'과 같음.
type AppleBox = Box<Apple>;
```

- 이것은 또한 [제네릭 함수](https://www.typescriptlang.org/docs/handbook/2/functions.html#generic-functions)를 대신 사용해 오버로드를 완전히 피할 수 있다는 것을 의미함

```ts
interface Box<Type> {
  contents: Type;
}

function setContents<Type>(box: Box<Type>, newContents: Type) {
  box.contents = newContents;
}
```

- 타입 별칭도 제네릭일 수 있다는 점도 주목할 가치가 있음
- 새로운 `Box<Type>` 인터페이스를 정의할 수도 있었음

```ts
interface Box<Type> {
  contents: Type;
}
```

- 타입 별칭을 대신 사용할 수도 있음

```ts
type Box<Type> = {
  contents: Type;
};
```

- 타입 별칭은 인터페이스와 달리 객체 타입 이상을 설명 가능하므로, 다른 종류의 제네릭 헬퍼 타입을 작성하는 데도 사용 가능함

```ts
// 타입 별칭만 정의하는 이 예제에는 함수 호출 인수 개수 오류가 없음.
type OrNull<Type> = Type | null;

type OneOrMany<Type> = Type | Type[];

type OneOrManyOrNull<Type> = OrNull<OneOrMany<Type>>;
// OneOrManyOrNull<Type>의 타입: Type | Type[] | null.

type OneOrManyOrNullStrings = OneOrManyOrNull<string>;
// OneOrManyOrNullStrings의 타입: string | string[] | null.
```

- 타입 별칭은 이어지는 절에서 다시 다룸

#### `Array` 타입

배열도 원소 타입을 매개변수로 받는 제네릭 객체 타입이다. 지금까지 사용한 `number[]`와 `string[]`은 각각 `Array<number>`와 `Array<string>`의 줄임말이다. 같은 배열 연산을 여러 원소 타입에 재사용할 수 있는 이유다.

```ts
function doSomething(value: Array<string>) {
  // ...
}

let myArray: string[] = ["hello", "world"];

// 이 둘 모두 작동!
doSomething(myArray);
doSomething(new Array("hello", "world"));
```

- 위의 `Box` 타입과 마찬가지로, `Array` 자체가 제네릭 타입임

```ts
// 컴파일러 설정: noLib: true
interface Number {}
interface String {}
interface Boolean {}
interface Symbol {}
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

- 현대 JavaScript는 `Map<K, V>`, `Set<T>`, `Promise<T>`와 같이 제네릭인 다른 데이터 구조도 제공함
- 이것이 의미하는 바: `Map`, `Set`, `Promise`가 동작하는 방식 때문에 모든 타입 세트와 함께 작동 가능함

#### `ReadonlyArray` 타입

- `ReadonlyArray`는 변경되어서는 안 되는 배열을 설명하는 특별한 타입임

```ts
// 의도한 오류: ReadonlyArray<string>에는 배열을 변경하는 push 메서드가 없음.
function doStuff(values: ReadonlyArray<string>) {
  // 'values'에서 읽을 수 있음...
  const copy = values.slice();
  console.log(`The first value is ${values[0]}`);

  // ...하지만 'values'를 변경할 수 없음.
  values.push("hello!");
}
```

- 속성의 `readonly` 수정자와 마찬가지로, 이것은 주로 의도를 위해 사용할 수 있는 도구임
- `ReadonlyArray`를 반환하는 함수를 볼 때, 내용을 전혀 변경해서는 안 된다는 신호로 읽을 수 있음
- `ReadonlyArray`를 사용하는 함수를 볼 때, 내용이 변경될 걱정 없이 해당 함수에 어떤 배열이든 전달 가능함을 알 수 있음
- `Array`와 달리, 사용할 수 있는 `ReadonlyArray` 생성자는 없음

```ts
// 의도한 오류: ReadonlyArray는 타입이며 new로 호출할 런타임 생성자가 아님.
new ReadonlyArray("red", "green", "blue");
```

- 대신, 일반 `Array`를 `ReadonlyArray`에 할당 가능함

```ts
const roArray: ReadonlyArray<string> = ["red", "green", "blue"];
```

- TypeScript가 `Array<Type>`에 대해 `Type[]`으로 줄임말 구문을 제공하는 것처럼, `ReadonlyArray<Type>`에 대해 `readonly Type[]`으로 줄임말 구문도 제공함

```ts
// 의도한 오류: readonly string[]에는 배열을 변경하는 push 메서드가 없음.
function doStuff(values: readonly string[]) {
  // values의 타입은 읽기 전용 배열 readonly string[].
  // 'values'에서 읽을 수 있음...
  const copy = values.slice();
  console.log(`The first value is ${values[0]}`);

  // ...하지만 'values'를 변경할 수 없음.
  values.push("hello!");
}
```

- 마지막으로 주목할 점: `readonly` 속성 수정자와 달리, 일반 `Array`와 `ReadonlyArray` 사이의 할당 가능성은 양방향이 아님

```ts
// 의도한 오류: readonly string[]인 x를 변경 가능한 string[]인 y에 할당할 수 없음.
let x: readonly string[] = [];
let y: string[] = [];

x = y;
y = x;
```

#### 튜플 타입

- 튜플 타입은 정확히 몇 개의 요소를 포함하고 특정 위치에 정확히 어떤 타입을 포함하는지 아는 또 다른 종류의 `Array` 타입임

```ts
type StringNumberPair = [string, number];
// 두 요소의 순서와 타입을 지정한 튜플 [string, number].
```

- 여기서 `StringNumberPair`는 `string`과 `number`의 튜플 타입임
- `ReadonlyArray`처럼 런타임에 표현이 없지만, TypeScript에는 중요함
- 타입 시스템에서 `StringNumberPair`는 `0` 인덱스에 `string`을 포함하고 `1` 인덱스에 `number`를 포함하는 배열을 설명함

```ts
function doSomething(pair: [string, number]) {
  const a = pair[0];
  // a의 추론 타입: string.
  const b = pair[1];
  // b의 추론 타입: number.
  // ...
}

doSomething(["hello", 42]);
```

- 요소 수를 넘어서 인덱싱하려고 하면 오류가 발생함

```ts
// 의도한 오류: 길이 2인 튜플에는 인덱스 2의 요소가 없음.
function doSomething(pair: [string, number]) {
  // ...

  const c = pair[2];
}
```

- JavaScript의 배열 구조 분해를 사용해 [튜플을 구조 분해](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Destructuring_assignment#Array_destructuring)할 수도 있음

```ts
function doSomething(stringHash: [string, number]) {
  const [inputString, hash] = stringHash;

  console.log(inputString);
  // inputString의 타입: string.

  console.log(hash);
  // hash의 타입: number.
}
```

> 튜플 타입은 각 요소의 의미가 "명확한" 규약 기반 API에서 유용합니다.

> 이것은 구조 분해할 때 변수 이름을 원하는 대로 지정할 수 있는 유연성을 제공합니다.

> 위 예제에서 요소 `0`과 `1`에 원하는 대로 이름을 지정할 수 있었습니다.

>

> 요소의 의미가 명확한지에 대한 판단은 사용자마다 다를 수 있으므로, 의미를 드러내는 속성 이름을 가진 객체를 사용하는 편이 API에 더 적합한지 검토할 필요가 있음.

- 길이 검사 외에, 이와 같은 단순한 튜플 타입은 특정 인덱스에 대한 속성을 선언하고 `length`를 숫자 리터럴 타입으로 선언하는 `Array` 버전의 타입과 동일함

```ts
interface StringNumberPair {
  // 특수 속성
  length: 2;
  0: string;
  1: number;

  // 기타 'Array<string | number>' 멤버...
  slice(start?: number, end?: number): Array<string | number>;
}
```

- 튜플은 요소의 타입 뒤에 물음표(`?`)를 써서 선택적 속성을 가질 수 있음
- 선택적 튜플 요소는 끝에만 올 수 있으며, `length` 타입에도 영향을 미침

```ts
type Either2dOr3d = [number, number, number?];

function setCoordinate(coord: Either2dOr3d) {
  const [x, y, z] = coord;
  // z의 타입: number | undefined.

  console.log(`Provided coordinates had ${coord.length} dimensions`);
  // coord.length의 타입: 2 | 3.
}
```

- 튜플은 배열/튜플 타입이어야 하는 나머지 요소도 가질 수 있음

```ts
type StringNumberBooleans = [string, number, ...boolean[]];
type StringBooleansNumber = [string, ...boolean[], number];
type BooleansStringNumber = [...boolean[], string, number];
```

- `StringNumberBooleans`: 처음 두 요소가 각각 `string`과 `number`이지만, 그 뒤에 몇 개의 `boolean`이든 가질 수 있는 튜플
- `StringBooleansNumber`: 첫 번째 요소가 `string`이고, 그 다음에 몇 개의 `boolean`이든 있고 `number`로 끝나는 튜플
- `BooleansStringNumber`: 시작 요소가 몇 개의 `boolean`이든 있고 `string` 다음 `number`로 끝나는 튜플

- 나머지 요소가 있는 튜플은 "길이"가 설정되지 않음 → 다른 위치에 잘 알려진 요소 세트만 있음

```ts
type StringNumberBooleans = [string, number, ...boolean[]];
const a: StringNumberBooleans = ["hello", 1];
const b: StringNumberBooleans = ["beautiful", 2, true];
const c: StringNumberBooleans = ["world", 3, true, false, true, false, true];
```

선택적 요소와 나머지 요소는 튜플을 함수의 매개변수 목록에 대응시킬 때 유용하다. [나머지 매개변수와 인수](https://www.typescriptlang.org/docs/handbook/2/functions.html#rest-parameters-and-arguments)에 튜플 타입을 쓰면 다음과 같이 선언할 수 있다.

```ts
function readButtonInput(...args: [string, number, ...boolean[]]) {
  const [name, version, ...input] = args;
  // ...
}
```

이 선언은 아래처럼 매개변수를 나누어 작성한 것과 같은 방식으로 호출한다.

```ts
function readButtonInput(name: string, version: number, ...input: boolean[]) {
  // ...
}
```

- 이것은 나머지 매개변수로 가변적인 수의 인수를 받고, 최소 수의 요소가 필요하지만 중간 변수를 도입하고 싶지 않을 때 편리함

#### `readonly` 튜플 타입

- 튜플 타입에 대한 마지막 참고 사항: 튜플 타입은 `readonly` 변형을 가지며, 앞에 `readonly` 수정자를 붙여 지정 가능함(배열 줄임말 구문과 마찬가지)

```ts
function doSomething(pair: readonly [string, number]) {
  // pair의 타입은 읽기 전용 튜플 readonly [string, number].
  // ...
}
```

- 예상할 수 있듯이, TypeScript에서 `readonly` 튜플의 어떤 속성에도 쓰는 것은 허용되지 않음

```ts
// 의도한 오류: readonly 튜플의 인덱스 0에 재할당할 수 없음.
function doSomething(pair: readonly [string, number]) {
  pair[0] = "hello!";
}
```

- 튜플은 대부분의 코드에서 생성되고 수정되지 않는 경향이 있으므로, 가능한 경우 타입을 `readonly` 튜플로 어노테이션하는 것이 좋은 기본값임
- 이것은 `const` 단언이 있는 배열 리터럴이 `readonly` 튜플 타입으로 추론된다는 점에서도 중요함

```ts
// 의도한 오류: readonly [3, 4]를 변경 가능한 [number, number] 매개변수에 전달할 수 없음.
let point = [3, 4] as const;

function distanceFromOrigin([x, y]: [number, number]) {
  return Math.sqrt(x ** 2 + y ** 2);
}

distanceFromOrigin(point);
```

`distanceFromOrigin`의 구현은 요소를 바꾸지 않지만 매개변수 타입 `[number, number]`는 변경을 허용한다. 반면 `point`는 `readonly [3, 4]`로 추론된다. 함수의 매개변수 타입만으로는 요소를 바꾸지 않는다고 보장할 수 없으므로 이 인수를 전달하면 오류가 난다.

## 클래스

> **원문:** https://www.typescriptlang.org/docs/handbook/2/classes.html

TypeScript는 ES2015의 `class` 문법을 지원한다. 여기에 타입 어노테이션 등을 더해 클래스의 멤버와 다른 타입 사이의 관계를 표현한다.

### 클래스 멤버

- 다음은 가장 기본적인 클래스임 → 빈 클래스

```ts
class Point {}
```

- 이 클래스는 아직 유용하지 않으므로, 몇 가지 멤버를 추가함

#### 필드

- 필드 선언은 클래스에 공개 쓰기 가능 속성을 생성함

```ts
// 컴파일러 설정: strictPropertyInitialization: false
class Point {
  x: number;
  y: number;
}

const pt = new Point();
pt.x = 0;
pt.y = 0;
```

- 다른 위치와 마찬가지로, 타입 어노테이션은 선택 사항이지만, 지정되지 않으면 암시적 `any`가 됨
- 필드는 초기화자도 가질 수 있음 → 클래스가 인스턴스화될 때 자동으로 실행됨

```ts
class Point {
  x = 0;
  y = 0;
}

const pt = new Point();
// 0, 0을 출력
console.log(`${pt.x}, ${pt.y}`);
```

- `const`, `let`, `var`와 마찬가지로, 클래스 속성의 초기화자는 타입을 추론하는 데 사용됨

```ts
// 의도한 오류: number로 추론된 pt.x에 string을 할당할 수 없음.
class Point {
  x = 0;
  y = 0;
}
const pt = new Point();
pt.x = "0";
```

- `--strictPropertyInitialization`

  - [`strictPropertyInitialization`](https://www.typescriptlang.org/tsconfig#strictPropertyInitialization) 설정은 클래스 필드가 생성자에서 초기화되어야 하는지를 제어함

  ```ts
  // 의도한 오류: name에 초기값이 없고 생성자에서도 확실하게 할당되지 않음.
  class BadGreeter {
    name: string;
  }
  ```

  ```ts
  class GoodGreeter {
    name: string;

    constructor() {
      this.name = "hello";
    }
  }
  ```

  초기화 여부는 생성자 자체에서 확인할 수 있어야 한다. 생성자에서 다른 메서드를 호출해 값을 넣더라도, 파생 클래스가 그 메서드를 재정의할 수 있어 초기화한 것으로 인정하지 않는다. 외부 라이브러리처럼 생성자 밖에서 확실히 초기화하는 경우에는 확정 할당 단언 `!`를 사용할 수 있다.

  ```ts
  class OKGreeter {
    // 초기화되지 않았지만, 오류 없음
    name!: string;
  }
  ```

#### `readonly`

- 필드에 `readonly` 수정자를 접두사로 붙일 수 있음
- 이것은 생성자 외부에서 필드에 대한 할당을 방지함

```ts
// 의도한 오류: err()와 클래스 외부에서는 readonly 필드 name에 재할당할 수 없음.
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

- 클래스 생성자는 함수와 매우 유사함
- 타입 어노테이션, 기본값 및 오버로드와 함께 매개변수를 추가 가능함

```ts
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

```ts
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

- 클래스 생성자 시그니처와 함수 시그니처 사이의 차이점

- 생성자는 타입 매개변수를 가질 수 없음 → 이것들은 외부 클래스 선언에 속하며, 나중에 다룸
- 생성자는 반환 타입 어노테이션을 가질 수 없음 → 클래스 인스턴스 타입이 항상 반환되기 때문

- Super 호출

  - JavaScript에서와 마찬가지로, 기본 클래스가 있으면 `this.` 멤버를 사용하기 전에 생성자 본문에서 `super();`를 호출해야 함

  ```ts
  // 의도한 오류: 파생 클래스 생성자는 this에 접근하기 전에 super()를 호출해야 함.
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

  - `super`를 호출하는 것을 잊는 것은 JavaScript에서 쉬운 실수이지만, TypeScript는 필요할 때 알려줌

#### 메서드

- 클래스의 함수 속성을 메서드라고 함
- 메서드는 함수 및 생성자와 동일한 모든 타입 어노테이션을 사용 가능함

```ts
class Point {
  x = 10;
  y = 10;

  scale(n: number): void {
    this.x *= n;
    this.y *= n;
  }
}
```

- 표준 타입 어노테이션 외에 TypeScript는 메서드에 새로운 것을 추가하지 않음
- 메서드 본문 내에서 `this.`를 통해 필드 및 기타 메서드에 접근하는 것이 여전히 필수라는 점에 주의
- 메서드 본문의 비한정 이름은 항상 둘러싼 범위의 무언가를 참조함

```ts
// 의도한 오류: 바깥 범위의 number 변수 x에 string을 할당할 수 없음.
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

- 클래스도 접근자를 가질 수 있음

```ts
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

- TypeScript에는 접근자에 대한 몇 가지 특별한 추론 규칙이 있음

- `get`이 있지만 `set`이 없으면, 속성이 자동으로 `readonly`가 됨
- 세터 매개변수의 타입이 지정되지 않으면, 게터의 반환 타입에서 추론됨

- [TypeScript 4.3](https://devblogs.microsoft.com/typescript/announcing-typescript-4-3/)부터 가져오기와 설정에 대해 다른 타입을 가진 접근자를 가질 수 있음

```ts
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

- 클래스는 인덱스 시그니처를 선언 가능함 → [다른 객체 타입의 인덱스 시그니처](https://www.typescriptlang.org/docs/handbook/2/objects.html#index-signatures)와 같은 방식으로 작동함

```ts
class MyClass {
  [s: string]: boolean | ((s: string) => boolean);

  check(s: string) {
    return this[s] as boolean;
  }
}
```

클래스의 인덱스 시그니처에는 데이터뿐 아니라 메서드의 타입도 들어가야 한다. 위 예제에서 함수 타입이 유니온에 포함된 이유다. 이 제약 때문에 인덱싱할 데이터는 보통 클래스 인스턴스와 별도로 저장하는 편이 낫다.

### 클래스 상속

- 객체 지향 기능을 가진 다른 언어와 마찬가지로, JavaScript의 클래스는 기본 클래스에서 상속 가능함

#### `implements` 절

- `implements` 절을 사용해 클래스가 특정 `interface`를 만족하는지 확인 가능함
- 클래스가 올바르게 구현하지 못하면 오류가 발생함

```ts
// 의도한 오류: Ball에는 Pingable이 요구하는 ping 메서드가 없음.
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

- 클래스는 여러 인터페이스를 구현할 수도 있음
  - 예: `class C implements A, B {`

- 주의 사항

  `implements`는 클래스가 인터페이스의 요구를 만족하는지 검사할 뿐, 클래스나 메서드의 타입을 바꾸지 않는다. 따라서 인터페이스를 구현한다고 선언해도 메서드 매개변수의 타입이 자동으로 정해지지는 않는다.

  ```ts
  // 의도한 오류: 타입을 명시하지 않은 매개변수 s가 암시적 any임.
  interface Checkable {
    check(name: string): boolean;
  }

  class NameChecker implements Checkable {
    check(s) {
      // 여기서 오류 없음에 주목
      return s.toLowerCase() === "ok";
      // s.toLowerCase의 타입: any (s가 any이므로 멤버 접근도 검사되지 않음).
    }
  }
  ```

  따라서 `s`의 타입은 인터페이스의 `name: string`에서 추론되지 않는다. 같은 이유로 인터페이스의 선택적 속성이 클래스에 자동으로 생기는 것도 아니다.

  ```ts
  // 의도한 오류: 클래스 C에는 y 속성이 선언되어 있지 않음.
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

- 클래스는 기본 클래스에서 `extend` 가능함
- 파생 클래스는 기본 클래스의 모든 속성과 메서드를 가지며, 추가 멤버도 정의 가능함

```ts
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

- 메서드 재정의

  - 파생 클래스는 기본 클래스의 필드나 속성을 재정의할 수도 있음
  - `super.` 구문을 사용해 기본 클래스 메서드에 접근 가능함
  - JavaScript 클래스는 간단한 조회 객체이므로, "슈퍼 필드"라는 개념은 없음
  - TypeScript는 파생 클래스가 항상 기본 클래스의 하위 타입이 되도록 강제함
  - 예: 메서드를 재정의하는 합법적인 방법

  ```ts
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

  - 파생 클래스가 기본 클래스 계약을 따르는 것이 중요함
  - 기본 클래스 참조를 통해 파생 클래스 인스턴스를 참조하는 것은 매우 흔함(그리고 항상 합법적임)

  ```ts
  class Base {
    greet() {
      console.log("Hello, world!");
    }
  }
  class Derived extends Base {}
  const d = new Derived();
  // 기본 클래스 참조를 통해 파생 인스턴스를 별칭으로 지정
  const b: Base = d;
  // 문제 없음
  b.greet();
  ```

  - `Derived`가 `Base`의 계약을 따르지 않으면 어떻게 되는지 아래에서 확인

  ```ts
  // 의도한 오류: 필수 name 인수를 추가한 Derived.greet는 인수 없이 호출 가능한 Base.greet와 호환되지 않음.
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

  - 오류에도 불구하고 이 코드를 컴파일하면, 이 샘플은 충돌함

  ```ts
  declare class Base {
    greet(): void;
  }
  declare class Derived extends Base {}
  const b: Base = new Derived();
  // "name"이 undefined가 되어 충돌
  b.greet();
  ```

- 타입 전용 필드 선언

  - `target >= ES2022` 또는 [`useDefineForClassFields`](https://www.typescriptlang.org/tsconfig#useDefineForClassFields)가 `true`이면, 클래스 필드는 부모 클래스 생성자가 완료된 후 초기화되어 부모 클래스가 설정한 값을 덮어씀
  - 이것은 상속된 필드에 대해 더 정확한 타입만 다시 선언하고 싶을 때 문제가 될 수 있음
  - 이러한 경우를 처리하려면, 이 필드 선언에 대한 런타임 효과가 없어야 함을 TypeScript에 나타내기 위해 `declare`를 작성 가능함

  ```ts
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

- 초기화 순서

  - JavaScript 클래스가 초기화되는 순서는 일부 경우에 놀라울 수 있음
  - 다음 코드로 확인

  ```ts
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

  - 이 결과의 원인

  - JavaScript가 정의하는 클래스 초기화 순서

    - 기본 클래스 필드가 초기화됨
    - 기본 클래스 생성자가 실행됨
    - 파생 클래스 필드가 초기화됨
    - 파생 클래스 생성자가 실행됨

  기본 클래스 생성자가 실행될 때는 파생 클래스의 필드가 아직 초기화되지 않았다. 그래서 생성자에서 읽은 `name`은 기본 클래스가 설정한 `"base"`다.

- 내장 타입 상속

  > 참고: `Array`, `Error`, `Map` 등과 같은 내장 타입에서 상속할 계획이 없거나 컴파일 대상이 명시적으로 `ES6`/`ES2015` 이상으로 설정된 경우, 이 섹션을 건너뛸 수 있습니다.

  - ES2015에서 객체를 반환하는 생성자는 `super(...)`의 모든 호출자에 대해 `this` 값을 암시적으로 대체함
  - 생성된 생성자 코드가 `super(...)`의 잠재적 반환 값을 캡처하고 `this`로 대체해야 함
  - 결과적으로, `Error`, `Array` 등의 서브클래싱이 더 이상 예상대로 작동하지 않을 수 있음
    - `Error`, `Array` 등의 생성자 함수가 ECMAScript 6의 `new.target`을 사용해 프로토타입 체인을 조정하기 때문
    - 그러나 ECMAScript 5에서 생성자를 호출할 때 `new.target`에 대한 값을 보장할 방법이 없음
  - 다른 다운레벨 컴파일러도 일반적으로 기본적으로 같은 제한이 있음
  - 다음과 같은 서브클래스의 경우

  ```ts
  class MsgError extends Error {
    constructor(m: string) {
      super(m);
    }
    sayHello() {
      return "hello " + this.message;
    }
  }
  ```

  - 다음과 같은 문제가 발생함

  - 이러한 서브클래스를 생성하여 반환된 객체에서 메서드가 `undefined`일 수 있음 → `sayHello`를 호출하면 오류가 발생함
  - 서브클래스의 인스턴스와 해당 인스턴스 사이에서 `instanceof`가 깨짐 → `(new MsgError()) instanceof MsgError`가 `false`를 반환함

  - 권장 사항: 모든 `super(...)` 호출 직후에 프로토타입을 수동으로 조정할 것

  ```ts
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

  - 그러나 `MsgError`의 모든 서브클래스도 프로토타입을 수동으로 설정해야 함
  - [`Object.setPrototypeOf`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/setPrototypeOf)를 지원하지 않는 런타임의 경우, 대신 [`__proto__`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/proto)를 사용 가능함
  - 안타깝게도, [이러한 해결 방법은 Internet Explorer 10 및 이전 버전에서 작동하지 않음](https://msdn.microsoft.com/en-us/library/s4esdbwz(v=vs.94).aspx)
  - 프로토타입에서 인스턴스 자체로 메서드를 수동으로 복사할 수 있지만(예: `MsgError.prototype`을 `this`로), 프로토타입 체인 자체는 수정 불가

### 멤버 가시성

- TypeScript를 사용해 특정 메서드나 속성이 클래스 외부의 코드에 표시되는지 여부를 제어 가능함

#### `public`

- 클래스 멤버의 기본 가시성은 `public`임
- `public` 멤버는 어디서든 접근 가능함

```ts
class Greeter {
  public greet() {
    console.log("hi!");
  }
}
const g = new Greeter();
g.greet();
```

- `public`이 이미 기본 가시성 수정자이므로, 클래스 멤버에 작성할 필요는 없지만, 스타일/가독성을 위해 선택 가능함

#### `protected`

- `protected` 멤버는 선언된 클래스의 서브클래스에서만 볼 수 있음

```ts
// 의도한 오류: protected 메서드 getName을 클래스 계층 외부에서 호출할 수 없음.
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
    // 파생 클래스 내부에서는 protected 메서드 this.getName()에 접근 가능.
  }
}
const g = new SpecialGreeter();
g.greet(); // OK
g.getName();
```

- `protected` 멤버 노출

  - 파생 클래스는 기본 클래스 계약을 따라야 하지만, 더 많은 기능을 가진 기본 클래스의 하위 타입을 노출하도록 선택 가능함
  - 여기에는 `protected` 멤버를 `public`으로 만드는 것도 포함됨

  ```ts
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

  - `Derived`가 이미 `m`을 자유롭게 읽고 쓸 수 있었으므로, 이것은 이 상황의 "보안"을 의미 있게 변경하지 않음
  - 여기서 주목할 주요 사항: 파생 클래스에서 이 노출이 의도적이지 않은 경우 `protected` 수정자를 반복하도록 주의해야 함

- 계층 간 `protected` 접근

  - TypeScript는 클래스 계층에서 형제 클래스의 `protected` 멤버에 접근하는 것을 허용하지 않음

  ```ts
  // 의도한 오류: Derived2에서는 Derived1 인스턴스를 통해 protected 속성 x에 접근할 수 없음.
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

  - 이것은 `Derived2`에서 `x`에 접근하는 것이 `Derived2`의 서브클래스에서만 합법적이어야 하고, `Derived1`은 그중 하나가 아니기 때문
  - 또한, `Derived1` 참조를 통해 `x`에 접근하는 것이 불법이라면(확실히 그래야 함), 기본 클래스 참조를 통해 접근하는 것도 상황을 개선해서는 안 됨

#### `private`

- `private`은 `protected`와 같지만, 서브클래스에서도 멤버에 대한 접근을 허용하지 않음

```ts
// 의도한 오류: private 속성 Base.x에 클래스 외부에서 접근할 수 없음.
class Base {
  private x = 0;
}
const b = new Base();
// 클래스 외부에서 접근 불가
console.log(b.x);
```

```ts
// 의도한 오류: private 속성 Base.x에 파생 클래스에서도 접근할 수 없음.
class Base {
  private x = 0;
}
class Derived extends Base {
  showX() {
    // 서브클래스에서 접근 불가
    console.log(this.x);
  }
}
```

- `private` 멤버는 파생 클래스에 표시되지 않으므로, 파생 클래스는 가시성을 높일 수 없음

```ts
// 의도한 오류: Base의 private 속성 x를 Derived에서 public으로 재선언할 수 없음.
class Base {
  private x = 0;
}
class Derived extends Base {
  x = 1;
}
```

- 인스턴스 간 `private` 접근

  - 다른 OOP 언어는 같은 클래스의 다른 인스턴스가 서로의 `private` 멤버에 접근할 수 있는지에 대해 의견이 다름
    - Java, C#, C++, Swift, PHP와 같은 언어는 이를 허용
    - Ruby는 허용하지 않음
  - TypeScript는 인스턴스 간 `private` 접근을 허용함

  ```ts
  class A {
    private x = 10;

    public sameAs(other: A) {
      // 오류 없음
      return other.x === this.x;
    }
  }
  ```

- 주의 사항

  - TypeScript 타입 시스템의 다른 측면과 마찬가지로, `private`과 `protected`는 [타입 검사 중에만 적용됨](https://www.typescriptlang.org/play?removeComments=true&target=99&ts=4.3.4#code/PTAEGMBsEMGddAEQPYHNQBMCmVoCcsEAHPASwDdoAXLUAM1K0gwQFdZSA7dAKWkoDK4MkSoByBAGJQJLAwAeAWABQIUH0HDSoiTLKUaoUggAW+DHorUsAOlABJcQlhUy4KpACeoLJzrI8cCwMGxU1ABVPIiwhESpMZEJQTmR4lxFQaQxWMm4IZABbIlIYKlJkTlDlXHgkNFAAbxVQTIAjfABrAEEC5FZOeIBeUAAGAG5mmSw8WAroSFIqb2GAIjMiIk8VieVJ8Ar01ncAgAoASkaAXxVr3dUwGoQAYWpMHBgCYn1rekZmNg4eUi0Vi2icoBWJCsNBWoA6WE8AHcAiEwmBgTEtDovtDaMZQLM6PEoQZbA5wSk0q5SO4vD4-AEghZoJwLGYEIRwNBoqAzFRwCZCFUIlFMXECdSiAhId8YZgclx0PsiiVqOVOAAaUAFLAsxWgKiC35MFigfC0FKgSAVVDTSyk+W5dB4fplHVVR6gF7xJrKFotEk-HXIRE9PoDUDDcaTAPTWaceaLZYQlmoPBbHYx-KcQ7HPDnK43FQqfY5+IMDDISPJLCIuqoc47UsuUCofAME3Vzi1r3URvF5QV5A2STtPDdXqunZDgDaYlHnTDrrEAF0dm28B3mDZg6HJwN1+2-hg57ulwNV2NQGoZbjYfNrYiENBwEFaojFiZQK08C-4fFKTVCozWfTgfFgLkeT5AUqiAA)
  - 이것은 `in`이나 간단한 속성 조회와 같은 JavaScript 런타임 구문이 여전히 `private` 또는 `protected` 멤버에 접근 가능함을 의미함

  ```ts
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

  - `private`은 또한 타입 검사 중에 대괄호 표기법을 사용한 접근을 허용함
  - 이것은 `private`으로 선언된 필드를 유닛 테스트와 같은 것에 대해 잠재적으로 더 쉽게 접근할 수 있게 하지만, 이러한 필드가 소프트 private이며 프라이버시를 엄격하게 적용하지 않는다는 단점이 있음

  ```ts
  // 의도한 오류: 점 표기법 s.secretKey로 MySafe의 private 속성에 접근할 수 없음.
  class MySafe {
    private secretKey = 12345;
  }

  const s = new MySafe();

  // 타입 검사 중 허용되지 않음
  console.log(s.secretKey);

  // OK
  console.log(s["secretKey"]);
  ```

  - TypeScript의 `private`과 달리, JavaScript의 [private 필드](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Classes/Private_class_fields)(`#`)는 컴파일 후에도 private으로 유지되며 대괄호 표기법 접근과 같은 앞서 언급한 탈출구를 제공하지 않아 하드 private임

  ```ts
  class Dog {
    #barkAmount = 0;
    personality = "happy";

    constructor() {}
  }
  ```

  - ES2021 이하로 컴파일할 때, TypeScript는 `#` 대신 WeakMap을 사용함
  - 악의적인 행위자로부터 클래스의 값을 보호해야 하는 경우, 클로저, WeakMap 또는 private 필드와 같은 하드 런타임 프라이버시를 제공하는 메커니즘을 사용해야 함
  - 이러한 추가된 프라이버시 검사는 런타임 중 성능에 영향을 미칠 수 있음

### 정적 멤버

- 클래스는 `static` 멤버를 가질 수 있음
- 이러한 멤버는 클래스의 특정 인스턴스와 연관되지 않음
- 클래스 생성자 객체 자체를 통해 접근 가능함

```ts
class MyClass {
  static x = 0;
  static printX() {
    console.log(MyClass.x);
  }
}
console.log(MyClass.x);
MyClass.printX();
```

- 정적 멤버도 같은 `public`, `protected`, `private` 가시성 수정자를 사용 가능함

```ts
// 의도한 오류: private 정적 속성 MyClass.x에 클래스 외부에서 접근할 수 없음.
class MyClass {
  private static x = 0;
}
console.log(MyClass.x);
```

- 정적 멤버도 상속됨

```ts
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

- `Function` 프로토타입의 속성을 덮어쓰는 것은 일반적으로 안전하지 않거나 불가능함
- 클래스 자체가 `new`로 호출할 수 있는 함수이므로, 특정 `static` 이름은 사용 불가함
- `name`, `length`, `call`과 같은 함수 속성은 `static` 멤버로 정의하기에 유효하지 않음

```ts
// 의도한 오류: 정적 속성 name이 생성자 함수의 기본 제공 name 속성과 충돌함.
class S {
  static name = "S!";
}
```

#### 왜 정적 클래스가 없나요?

- TypeScript(및 JavaScript)에는 예를 들어 C#처럼 `static class`라는 구문이 없음
- 이러한 구문은 해당 언어가 모든 데이터와 함수를 클래스 내부에 강제하기 때문에 존재함 → TypeScript에는 그러한 제한이 없으므로, 필요 없음
- 단일 인스턴스만 있는 클래스는 일반적으로 JavaScript/TypeScript에서 일반 객체로 표현됨
- 예: 일반 객체(또는 최상위 함수)가 동일하게 작업을 수행하므로 TypeScript에서 "정적 클래스" 구문이 불필요함

```ts
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

- 정적 블록을 사용하면 포함하는 클래스 내의 private 필드에 접근할 수 있는 자체 범위를 가진 일련의 명령문을 작성 가능함
- 이것은 명령문 작성의 모든 기능, 변수 누출 없음, 클래스 내부에 대한 완전한 접근으로 초기화 코드를 작성할 수 있다는 것을 의미함

```ts
declare function loadLastInstances(): any[]
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

- 인터페이스와 마찬가지로 클래스도 제네릭일 수 있음
- 제네릭 클래스가 `new`로 인스턴스화될 때, 타입 매개변수는 함수 호출에서와 같은 방식으로 추론됨

```ts
class Box<Type> {
  contents: Type;
  constructor(value: Type) {
    this.contents = value;
  }
}

const b = new Box("hello!");
// b의 추론 타입: Box<string>.
```

- 클래스는 인터페이스와 같은 방식으로 제네릭 제약 조건과 기본값을 사용 가능함

#### 정적 멤버의 타입 매개변수

- 이 코드는 합법적이지 않으며, 왜 그런지 명확하지 않을 수 있음

```ts
// 의도한 오류: 정적 멤버는 클래스의 타입 매개변수 Type을 참조할 수 없음.
class Box<Type> {
  static defaultValue: Type;
}
```

타입 정보가 지워진 런타임에는 `Box.defaultValue`가 하나만 존재한다. 만약 `Box<string>.defaultValue`를 설정할 수 있다면 `Box<number>.defaultValue`도 함께 바뀌어 각 타입의 기대를 충족할 수 없다. 그래서 제네릭 클래스의 `static` 멤버는 클래스의 타입 매개변수를 참조할 수 없다.

### 클래스에서의 런타임 `this`

클래스에 타입을 붙여도 JavaScript의 `this` 동작은 바뀌지 않는다. 같은 메서드를 다른 객체의 속성으로 호출하면 무엇을 읽는지 다음 예제로 확인할 수 있다.

```ts
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

함수 내부의 `this`는 기본적으로 호출 방식에 따라 정해진다. 위에서는 `obj.getName()`으로 호출했으므로 `this`가 `obj`를 가리켜 `"obj"`가 출력된다. 클래스 인스턴스의 이름을 읽으려던 의도와 달라질 수 있으므로, 아래에서는 이런 오류를 줄이는 방법을 살펴본다.

#### 화살표 함수

- `this` 컨텍스트를 잃는 방식으로 자주 호출되는 함수가 있다면, 메서드 정의 대신 화살표 함수 속성을 사용하는 것이 유리할 수 있음

```ts
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

- 이것의 트레이드오프

- `this` 값은 TypeScript로 검사되지 않은 코드에서도 런타임에 올바름이 보장됨
- 각 클래스 인스턴스가 이런 방식으로 정의된 각 함수의 자체 복사본을 가지므로 더 많은 메모리를 사용함
- 파생 클래스에서 `super.getName`을 사용할 수 없음 → 프로토타입 체인에 기본 클래스 메서드를 가져올 항목이 없기 때문

#### `this` 매개변수

메서드나 함수의 첫 매개변수에 `this`를 선언하면 호출에 필요한 `this` 타입을 지정한다. 타입 검사에만 사용하며 컴파일한 JavaScript에서는 제거된다.

```ts
type SomeType = any;
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

- TypeScript는 `this` 매개변수가 있는 함수가 올바른 컨텍스트로 호출되는지 검사함
- 화살표 함수를 사용하는 대신, 메서드 정의에 `this` 매개변수를 추가해 메서드가 올바르게 호출되도록 정적으로 적용 가능함

```ts
// 의도한 오류: g()를 독립 함수로 호출하면 getName이 요구하는 MyClass 타입의 this가 없음.
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

- 이 메서드는 화살표 함수 접근 방식의 반대 트레이드오프를 만듦

- JavaScript 호출자는 여전히 인식하지 못하고 클래스 메서드를 잘못 사용할 수 있음
- 클래스 인스턴스당 하나가 아닌 클래스 정의당 하나의 함수만 할당됨
- 기본 메서드 정의는 여전히 `super`를 통해 호출 가능함

### `this` 타입

- 클래스에서 `this`라는 특별한 타입은 현재 클래스의 타입을 동적으로 참조함
- 이것이 어떻게 유용한지 아래에서 확인

```ts
class Box {
  contents: string = "";
  set(value: string) {
// set의 시그니처: (value: string) => this.
    this.contents = value;
    return this;
  }
}
```

- 여기서 TypeScript는 `set`의 반환 타입을 `Box`가 아닌 `this`로 추론함
- 이제 `Box`의 서브클래스를 만들어 확인

```ts
class Box {
  contents: string = "";
  set(value: string) {
    this.contents = value;
    return this;
  }
}
class ClearableBox extends Box {
  clear() {
    this.contents = "";
  }
}

const a = new ClearableBox();
const b = a.set("hello");
// b의 추론 타입: ClearableBox.
```

- 매개변수 타입 어노테이션에서도 `this`를 사용 가능함

```ts
class Box {
  content: string = "";
  sameAs(other: this) {
    return other.content === this.content;
  }
}
```

- 이것은 `other: Box`를 작성하는 것과 다름 → 파생 클래스가 있는 경우, `sameAs` 메서드는 이제 같은 파생 클래스의 다른 인스턴스만 받아들임

```ts
// 의도한 오류: base에는 DerivedBox의 otherContent가 없어 derived.sameAs에 전달할 수 없음.
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

- 클래스와 인터페이스의 메서드에서 반환 위치에 `this is Type`을 사용 가능함
- 타입 좁히기(예: `if` 문)와 혼합하면 대상 객체의 타입이 지정된 `Type`으로 좁혀짐

```ts
// 컴파일러 설정: strictPropertyInitialization: false
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
// 이 분기의 fso 타입: FileRep, fso.content의 타입: string.
} else if (fso.isDirectory()) {
  fso.children;
// 이 분기의 fso 타입: Directory, fso.children의 타입: FileSystemObject[].
} else if (fso.isNetworked()) {
  fso.host;
// 이 분기의 fso 타입: Networked & FileSystemObject, fso.host의 타입: string.
}
```

- this 기반 타입 가드의 흔한 사용 사례: 특정 필드의 지연 유효성 검사를 허용하는 것
- 예: 이 경우 `hasValue`가 true로 확인되면 box 내부에 있는 값에서 `undefined`를 제거함

```ts
class Box<T> {
  value?: T;

  hasValue(): this is { value: T } {
    return this.value !== undefined;
  }
}

const box = new Box<string>();
box.value = "Gameboy";

box.value;
// box.value의 타입: string (바로 앞의 문자열 할당으로 좁혀짐; 선언 타입은 string | undefined).

if (box.hasValue()) {
  box.value;
  // hasValue() 확인 후 box.value의 타입: string.
}
```

### 매개변수 속성

- TypeScript는 생성자 매개변수를 같은 이름과 값을 가진 클래스 속성으로 바꾸는 특별한 구문을 제공함
- 이것들을 매개변수 속성이라고 하며, 생성자 인수에 가시성 수정자 `public`, `private`, `protected`, 또는 `readonly` 중 하나를 접두사로 붙여 생성함
- 결과 필드는 해당 수정자를 얻음

```ts
// 의도한 오류: private 매개변수 속성 z에 클래스 외부에서 접근할 수 없음.
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
// a.x의 타입: number (readonly 속성).
console.log(a.z);
```

### 클래스 표현식

- 클래스 표현식은 클래스 선언과 매우 유사함
- 유일한 실제 차이점: 클래스 표현식은 이름이 필요하지 않지만, 결국 바인딩된 식별자를 통해 참조 가능함

```ts
const someClass = class<Type> {
  content: Type;
  constructor(value: Type) {
    this.content = value;
  }
};

const m = new someClass("Hello, world");
// m의 추론된 인스턴스 구조: { content: string }.
```

### 생성자 시그니처

- JavaScript 클래스는 `new` 연산자로 인스턴스화됨
- 클래스 자체의 타입이 주어지면, [InstanceType](https://www.typescriptlang.org/docs/handbook/utility-types.html#instancetypetype) 유틸리티 타입이 이 작업을 모델링함

```ts
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

메서드나 필드의 구현을 파생 클래스에 맡기려면 `abstract`로 선언한다. 추상 멤버는 추상 클래스 안에 있어야 하며, 그 클래스를 직접 인스턴스화할 수는 없다. 모든 추상 멤버를 구현한 구체 클래스에서 인스턴스를 만든다.

```ts
// 의도한 오류: 추상 클래스 Base를 직접 인스턴스화할 수 없음.
abstract class Base {
  abstract getName(): string;

  printName() {
    console.log("Hello, " + this.getName());
  }
}

const b = new Base();
```

- `Base`가 추상이므로 `new`로 인스턴스화 불가함
- 대신, 파생 클래스를 만들고 추상 멤버를 구현해야 함

```ts
abstract class Base {
  abstract getName(): string;
  printName() {}
}
class Derived extends Base {
  getName() {
    return "world";
  }
}

const d = new Derived();
d.printName();
```

- 기본 클래스의 추상 멤버를 구현하지 않으면 오류가 발생함

```ts
// 의도한 오류: 구체 클래스 Derived가 추상 메서드 getName을 구현하지 않음.
abstract class Base {
  abstract getName(): string;
  printName() {}
}
class Derived extends Base {
  // 아무것도 하는 것을 잊음
}
```

#### 추상 생성 시그니처

추상 클래스의 파생 클래스를 인수로 받아 인스턴스를 만드는 함수를 생각해 보자. 매개변수를 `typeof Base`로 선언하면 어떤 문제가 생기는지 다음 예제에서 확인할 수 있다.

```ts
// 의도한 오류: typeof Base는 추상 생성자이므로 new ctor()로 인스턴스화할 수 없음.
abstract class Base {
  abstract getName(): string;
  printName() {}
}
class Derived extends Base {
  getName() {
    return "";
  }
}
function greet(ctor: typeof Base) {
  const instance = new ctor();
  instance.printName();
}
```

- TypeScript는 추상 클래스를 인스턴스화하려고 한다고 올바르게 알려줌
- 결국, `greet`의 정의가 주어지면, 추상 클래스를 생성하게 될 이 코드를 작성하는 것이 완전히 합법적임

```ts
declare const greet: any, Base: any;
// 나쁨!
greet(Base);
```

- 대신, 생성 시그니처가 있는 것을 받아들이는 함수를 작성해야 함

```ts
// 의도한 오류: 추상 생성자 Base는 구체 생성자를 요구하는 new () => Base에 전달할 수 없음.
abstract class Base {
  abstract getName(): string;
  printName() {}
}
class Derived extends Base {
  getName() {
    return "";
  }
}
function greet(ctor: new () => Base) {
  const instance = new ctor();
  instance.printName();
}
greet(Derived);
greet(Base);
```

- 이제 TypeScript는 어떤 클래스 생성자 함수를 호출할 수 있는지 올바르게 알려줌 → `Derived`는 구체적이므로 가능하지만, `Base`는 불가능함

### 클래스 간의 관계

- 대부분의 경우, TypeScript의 클래스는 다른 타입과 같이 구조적으로 비교됨
- 예: 이 두 클래스는 동일하기 때문에 서로 대신 사용 가능함

```ts
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

- 마찬가지로, 명시적 상속이 없어도 클래스 간의 하위 타입 관계가 존재함

```ts
// 컴파일러 설정: strict: false
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

- 이것은 간단하게 들리지만, 다른 것보다 더 이상하게 보이는 경우도 있음
- 빈 클래스에는 멤버가 없음
- 구조적 타입 시스템에서 멤버가 없는 타입은 일반적으로 다른 모든 것의 슈퍼타입임
- 따라서 빈 클래스를 작성하면(권장하지 않음!), 어떤 것이든 그 자리에 사용 가능함

```ts
class Empty {}

function fn(x: Empty) {
  // 'x'로 아무것도 할 수 없으므로, 하지 않겠습니다
}

// 모두 OK!
fn(window);
fn({});
fn(fn);
```

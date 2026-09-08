# TypeScript 언어 레퍼런스

## 언어 레퍼런스: 변수 선언, 열거형, 심벌, 이터레이터

## 변수 선언 (Variable Declaration)

> 원문: https://www.typescriptlang.org/docs/handbook/variable-declarations.html

TypeScript는 JavaScript의 `let`과 `const` 선언을 지원한다. `let`은 `var`에서 생기는 [스코프 관련 문제](https://www.typescriptlang.org/docs/handbook/basic-types.html#a-note-about-let)를 피할 수 있고, `const`는 여기에 재할당 제한을 더한다. 먼저 `var`의 동작을 살펴보면 두 선언을 사용하는 이유를 이해하기 쉽다.

### `var` 선언

- JavaScript에서 변수 선언은 전통적으로 `var` 키워드로 수행

```ts
var a = 10;
```

- 값 `10`을 가진 변수 `a` 선언
- 함수 내부에서도 변수 선언 가능

```ts
function f() {
  var message = "Hello, world!";

  return message;
}
```

- 다른 함수 내에서 동일한 변수에 접근 가능

```ts
function f() {
  var a = 10;
  return function g() {
    var b = a + 1;
    return b;
  };
}

var g = f();
g(); // '11'을 반환
```

위 예제에서 `g`는 `f`가 선언한 변수 `a`를 캡처한다. `g`가 참조하는 것은 호출 시점의 `a`이므로, `f`의 실행이 끝난 뒤에도 `g`를 통해 이 변수에 접근하거나 값을 바꿀 수 있다.

```ts
function f() {
  var a = 1;

  a = 2;
  var b = g();
  a = 3;

  return b;

  function g() {
    return a;
  }
}

f(); // '2'를 반환
```

#### 스코핑 규칙

- `var` 선언: 다른 언어에 익숙한 사람에게 이상한 스코핑 규칙

```ts
function f(shouldInitialize: boolean) {
  if (shouldInitialize) {
    var x = 10;
  }

  return x;
}

f(true); // '10'을 반환
f(false); // 'undefined'를 반환
```

`x`는 `if` 블록 안에서 선언했지만 블록 밖에서도 접근할 수 있다. `var`의 범위는 해당 블록이 아니라 자신을 포함하는 함수, 모듈, 네임스페이스 또는 전역 스코프이기 때문이다. 이를 `var` 스코핑 또는 함수 스코핑이라고 부르며, 매개변수도 함수 스코프에 속한다.

같은 변수를 여러 번 선언해도 오류가 나지 않는다는 점까지 겹치면 실수하기 쉽다. 다음 예제에서는 안쪽 루프와 바깥쪽 루프가 같은 `i`를 사용한다.

```ts
function sumMatrix(matrix: number[][]) {
  var sum = 0;
  for (var i = 0; i < matrix.length; i++) {
    var currentRow = matrix[i];
    for (var i = 0; i < currentRow.length; i++) {
      sum += currentRow[i];
    }
  }

  return sum;
}
```

안쪽 `for` 루프가 바깥쪽 루프와 같은 `i`를 덮어쓰므로 계산이 잘못된다. 코드 리뷰에서도 놓칠 수 있는 실수다.

#### 변수 캡처의 특이점

```ts
for (var i = 0; i < 10; i++) {
  setTimeout(function () {
    console.log(i);
  }, 100 * i);
}
```

- `setTimeout`: 특정 밀리초 후 함수 실행 시도(다른 것이 실행을 멈추기를 기다리며)
- 출력 결과

```
10
10
10
10
10
10
10
10
10
10
```

- 기대했던 출력(대부분의 기대)

```
0
1
2
3
4
5
6
7
8
9
```

`setTimeout`에 전달한 함수들은 모두 같은 스코프의 `i`를 참조한다. 콜백은 `for` 루프가 끝난 뒤 실행되고, 그때 `i`는 이미 `10`이므로 매번 `10`을 출력한다. 각 반복의 값을 보관하려면 IIFE(즉시 호출 함수 표현식)의 매개변수로 `i`를 전달할 수 있다.

```ts
for (var i = 0; i < 10; i++) {
  // 현재 'i' 상태를 캡처
  // 현재 값으로 함수를 호출
  (function (i) {
    setTimeout(function () {
      console.log(i);
    }, 100 * i);
  })(i);
}
```

IIFE의 매개변수 `i`는 바깥 루프의 `i`를 가린다. 이름을 그대로 쓰므로 루프 본문을 거의 바꾸지 않고 반복마다 다른 값을 캡처할 수 있다.

### `let` 선언

`let`은 앞에서 본 `var`의 스코프 문제를 해결하기 위해 도입되었다. 선언 형태는 키워드만 다르지만, 변수가 유효한 범위가 달라진다.

```ts
let hello = "Hello!";
```

#### 블록-스코핑

- `let` 사용 → 렉시컬-스코핑 또는 블록-스코핑
- `var`(포함하는 함수로 스코프 누출)와 달리, 블록 스코프 변수는 가장 가까운 포함 블록, `for` 루프 외부에서 볼 수 없음

```ts
function f(input: boolean) {
  let a = 100;

  if (input) {
    // 여전히 'a'를 참조 가능
    let b = a + 1;
    return b;
  }

  // 오류: 'b'가 여기에 존재하지 않음
  return b;
}
```

- 지역 변수 `a`, `b`
  - `a`: 스코프가 `f`의 본문으로 제한
  - `b`: 스코프가 포함하는 `if` 문의 블록으로 제한
- `catch` 절에서 선언된 변수도 유사한 스코핑 규칙

```ts
try {
  throw "oh no!";
} catch (e) {
  console.log("Oh well.");
}

// 오류: 'e'가 여기에 존재하지 않음
console.log(e);
```

블록 스코프 변수는 선언 전에 읽거나 쓸 수 없다. 변수의 스코프에 들어온 시점부터 선언에 도달하기 전까지를 일시적 데드 존이라고 하며, 이 구간에서 접근하면 TypeScript가 오류를 보고한다.

```ts
a++; // 선언되기 전에 'a'를 사용하는 것은 불법
let a;
```

- 블록 스코프 변수는 선언 전에도 여전히 캡처 가능
  - 유일한 함정: 선언 전에 해당 함수를 호출하는 것은 불법
  - ES2015 대상 시 현대 런타임은 오류 발생 → 현재 TypeScript는 관대하며 오류로 보고하지 않음

```ts
function foo() {
  // 'a'를 캡처 가능
  return a;
}

// 'a'가 선언되기 전에 'foo'를 불법으로 호출
// 런타임에서 여기서 오류가 발생해야 함
foo();

let a;
```

- 일시적 데드 존 상세: [Mozilla Developer Network](https://developer.mozilla.org/docs/Web/JavaScript/Reference/Statements/let#Temporal_dead_zone_and_errors_with_let) 참고

#### 재선언과 섀도잉

- `var` 선언: 변수를 몇 번 선언하든 하나만 얻음

```ts
function f(x) {
  var x;
  var x;

  if (true) {
    var x;
  }
}
```

위 예제의 선언들은 모두 같은 `x`를 가리킨다. 유효한 코드지만 버그의 원인이 되기 쉽다. `let`은 같은 스코프에서 변수를 다시 선언하면 오류를 낸다.

```ts
let x = 10;
let x = 20; // 오류: 같은 스코프에서 'x'를 재선언할 수 없음
```

- 변수가 모두 블록 스코프일 필요 없음 → TypeScript가 문제를 알려줌

```ts
function f(x) {
  let x = 100; // 오류: 매개변수 선언과 간섭
}

function g() {
  let x = 100;
  var x = 100; // 오류: 'x'의 두 선언을 모두 가질 수 없음
}
```

- 블록 스코프 변수를 함수 스코프 변수로 선언할 수 없다는 뜻은 아님 → 명확히 다른 블록 내에서 선언돼야 함

```ts
function f(condition, x) {
  if (condition) {
    let x = 100;
    return x;
  }

  return x;
}

f(false, 0); // '0'을 반환
f(true, 0); // '100'을 반환
```

안쪽 스코프에서 같은 이름의 변수를 새로 선언해 바깥 변수를 가리는 것을 섀도잉이라고 한다. 의도하지 않은 섀도잉은 버그를 만들 수 있지만, 앞의 `sumMatrix`에서는 두 루프의 `i`를 분리하는 데 도움이 된다.

```ts
function sumMatrix(matrix: number[][]) {
  let sum = 0;
  for (let i = 0; i < matrix.length; i++) {
    var currentRow = matrix[i];
    for (let i = 0; i < currentRow.length; i++) {
      sum += currentRow[i];
    }
  }

  return sum;
}
```

- 이 버전: 내부 루프의 `i`가 외부 루프의 `i`를 섀도잉 → 합계가 올바르게 수행됨
- 섀도잉은 명확한 코드 작성 관점에서 보통 피해야 함(적합한 시나리오도 존재 → 최선의 판단 필요)

#### 블록 스코프 변수 캡처

- 스코프 실행 시마다 변수의 "환경" 생성
  - 해당 환경, 캡처된 변수는 스코프 내 모든 것이 실행 완료된 후에도 존재 가능

```ts
function theCityThatAlwaysSleeps() {
  let getCity;

  if (true) {
    let city = "Seattle";
    getCity = function () {
      return city;
    };
  }

  return getCity();
}
```

`getCity`가 `city`를 캡처하므로 `if` 블록이 끝난 뒤에도 그 값에 접근할 수 있다. 앞의 `setTimeout` 예제에서는 IIFE로 반복마다 새 변수 환경을 만들었다. 루프에 `let`을 사용하면 반복마다 새 스코프가 생기므로 IIFE 없이도 같은 효과를 얻는다.

```ts
for (let i = 0; i < 10; i++) {
  setTimeout(function () {
    console.log(i);
  }, 100 * i);
}
```

- 출력

```
0
1
2
3
4
5
6
7
8
9
```

### `const` 선언

```ts
const numLivesForCat = 9;
```

`const`는 `let`과 같은 스코프 규칙을 따르지만, 선언한 변수에 다른 값을 재할당할 수 없다. 다만 변수가 참조하는 객체 자체가 불변이 되는 것은 아니다.

```ts
const numLivesForCat = 9;
const kitty = {
  name: "Aurora",
  numLives: numLivesForCat,
};

// 오류
kitty = {
  name: "Danielle",
  numLives: numLivesForCat,
};

// 모두 "괜찮음"
kitty.name = "Rory";
kitty.name = "Kitty";
kitty.name = "Cat";
kitty.numLives--;
```

- 별도 조치 없으면 `const` 변수의 내부 상태는 여전히 수정 가능
  - TypeScript는 객체 멤버를 `readonly`로 지정 가능([인터페이스 챕터](https://www.typescriptlang.org/docs/handbook/interfaces.html) 참고)

### `let` vs. `const`

두 선언의 스코프 규칙은 같으므로, 재할당이 필요한지를 기준으로 선택한다. [최소 권한 원칙](https://wikipedia.org/wiki/Principle_of_least_privilege)에 따라 재할당할 계획이 없는 변수에는 `const`를 쓰면 데이터 흐름을 예측하기 쉬워진다. 구체적인 기준은 팀과 조율할 수 있다. 이 핸드북의 예제는 대부분 `let`을 사용한다.

### 구조 분해

- TypeScript의 또 다른 ECMAScript 2015 기능
- 전체 참조: [Mozilla Developer Network 글](https://developer.mozilla.org/docs/Web/JavaScript/Reference/Operators/Destructuring_assignment)
- 이 섹션은 간략한 개요 제공

#### 배열 구조 분해

```ts
let input = [1, 2];
let [first, second] = input;
console.log(first); // 1을 출력
console.log(second); // 2를 출력
```

- `first`, `second` 두 새 변수 생성 → 인덱싱과 동일하지만 더 편리

```ts
first = input[0];
second = input[1];
```

- 구조 분해는 이미 선언된 변수에서도 작동

```ts
// 변수 스왑
[first, second] = [second, first];
```

- 함수 매개변수와 함께 사용 가능

```ts
function f([first, second]: [number, number]) {
  console.log(first);
  console.log(second);
}
f([1, 2]);
```

- `...` 구문으로 목록의 나머지 항목에 대한 변수 생성 가능

```ts
let [first, ...rest] = [1, 2, 3, 4];
console.log(first); // 1을 출력
console.log(rest); // [ 2, 3, 4 ]를 출력
```

- 신경 쓰지 않는 후행 요소 무시 가능

```ts
let [first] = [1, 2, 3, 4];
console.log(first); // 1을 출력
```

- 또는 다른 요소

```ts
let [, second, , fourth] = [1, 2, 3, 4];
console.log(second); // 2를 출력
console.log(fourth); // 4를 출력
```

#### 튜플 구조 분해

- 튜플은 배열처럼 구조 분해 가능 → 구조 분해 변수는 해당 튜플 요소의 타입을 얻음

```ts
let tuple: [number, string, boolean] = [7, "hello", true];

let [a, b, c] = tuple; // a: number, b: string, c: boolean
```

- 튜플의 요소 범위를 넘어서 구조 분해하면 오류

```ts
let [a, b, c, d] = tuple; // 오류, 인덱스 3에 요소가 없음
```

- 배열과 마찬가지로 `...`로 튜플의 나머지를 구조 분해해 더 짧은 튜플 획득 가능

```ts
let [a, ...bc] = tuple; // bc: [string, boolean]
let [a, b, c, ...d] = tuple; // d: [], 빈 튜플
```

- 또는 후행 요소나 다른 요소 무시

```ts
let [a] = tuple; // a: number
let [, b] = tuple; // b: string
```

#### 객체 구조 분해

```ts
let o = {
  a: "foo",
  b: 12,
  c: "bar",
};
let { a, b } = o;
```

- `o.a`, `o.b`에서 새 변수 `a`, `b` 생성 → 필요 없으면 `c` 건너뛸 수 있음
- 배열 구조 분해처럼 선언 없이 할당 가능

```ts
({ a, b } = { a: "baz", b: 101 });
```

JavaScript는 문장 앞의 `{`를 블록의 시작으로 해석하므로 이 할당문을 괄호로 묶어야 한다. 일부 속성을 꺼내고 나머지를 별도 변수에 담으려면 다음처럼 `...`를 사용한다.

```ts
let { a, ...passthrough } = o;
let total = passthrough.b + passthrough.c.length;
```

- 속성 이름 바꾸기

  ```ts
  let { a: newName1, b: newName2 } = o;
  ```

  - `a: newName1`을 "`a`를 `newName1`로"라고 읽음 → 방향은 왼쪽에서 오른쪽

  ```ts
  let newName1 = o.a;
  let newName2 = o.b;
  ```

  - 주의: 여기서 콜론은 타입을 나타내지 않음
    - 타입 지정 시 전체 구조 분해 후에 별도로 작성 필요

  ```ts
  let { a: newName1, b: newName2 }: { a: string; b: number } = o;
  ```

- 기본값

  - 속성이 undefined인 경우 기본값 지정 가능

  ```ts
  function keepWholeObject(wholeObject: { a: string; b?: number }) {
    let { a, b = 1001 } = wholeObject;
  }
  ```

  - `b?`: `b`가 선택적 → `undefined`일 수 있음을 나타냄
  - `keepWholeObject`: `b`가 undefined인 경우에도 `wholeObject`뿐 아니라 속성 `a`, `b`에 대한 변수를 가짐

### 함수 선언

- 구조 분해는 함수 선언에서도 작동
  - 간단한 경우는 직관적

```ts
type C = { a: string; b?: number };
function f({ a, b }: C): void {
  // ...
}
```

- 매개변수 기본값 지정이 더 일반적 → 구조 분해로 기본값을 올바르게 가져오는 것은 까다로울 수 있음
  - 기본값 전에 패턴을 넣어야 함

```ts
function f({ a = "", b = 0 } = {}): void {
  // ...
}
f();
```

> 위 스니펫: 핸드북에서 앞서 설명한 타입 추론의 예

- 주 이니셜라이저가 아닌 구조 분해된 속성의 선택적 속성에 대해 기본값 제공 필요
  - `C`가 `b`를 선택적으로 정의

```ts
function f({ a, b = 0 } = { a: "" }): void {
  // ...
}
f({ a: "yes" }); // ok, 기본값 b = 0
f(); // ok, 기본값 { a: "" }, 그런 다음 기본값 b = 0
f({}); // 오류, 인수를 제공하면 'a'가 필요함
```

구조 분해에 깊은 중첩, 이름 변경, 기본값, 타입 주석까지 한꺼번에 넣으면 읽기 어렵다. 표현식은 작고 단순하게 유지하고, 복잡해지면 각각의 할당문으로 나누어 작성한다.

### 스프레드

스프레드 연산자는 구조 분해와 반대로 배열이나 객체의 내용을 다른 배열이나 객체 안에 펼친다.

```ts
let first = [1, 2];
let second = [3, 4];
let bothPlus = [0, ...first, ...second, 5];
```

- `bothPlus` 값: `[0, 1, 2, 3, 4, 5]`
- 스프레딩은 `first`, `second`의 얕은 복사본 생성 → 원본 배열은 변경되지 않음
- 객체도 스프레드 가능

```ts
let defaults = { food: "spicy", price: "$$", ambiance: "noisy" };
let search = { ...defaults, food: "rich" };
```

- `search` 값: `{ food: "rich", price: "$$", ambiance: "noisy" }`
- 객체 스프레딩은 배열 스프레딩보다 복잡
  - 배열처럼 왼쪽에서 오른쪽으로 진행하지만 결과는 여전히 객체
  - 스프레드 객체에서 나중에 오는 속성이 이전 속성을 덮어씀

```ts
let defaults = { food: "spicy", price: "$$", ambiance: "noisy" };
let search = { food: "rich", ...defaults };
```

- 이 경우 `defaults`의 `food` 속성이 `food: "rich"`를 덮어씀 → 의도하지 않은 결과
- 객체 스프레드의 추가 제한
  - 객체의 [자체, 열거 가능한 속성](https://developer.mozilla.org/docs/Web/JavaScript/Enumerability_and_ownership_of_properties)만 포함 → 인스턴스 스프레드 시 메서드 손실

```ts
class C {
  p = 12;
  m() {}
}
let c = new C();
let clone = { ...c };
clone.p; // ok
clone.m(); // 오류!
```

  - TypeScript 컴파일러는 제네릭 함수에서 타입 매개변수의 스프레드 허용 안 함(향후 언어 버전에서 예상)

### `using` 선언

- [Stage 3 명시적 리소스 관리](https://github.com/tc39/proposal-explicit-resource-management) 제안의 일부인 JavaScript의 다가오는 기능
- `const` 선언과 유사하지만 선언에 바인딩된 값의 수명을 변수의 스코프와 결합
- `using` 선언을 포함하는 블록에서 제어가 벗어나면 선언된 값의 `[Symbol.dispose]()` 메서드가 실행 → 값이 정리 수행 가능

```ts
function f() {
  using x = new C();
  doSomethingWith(x);
} // `x[Symbol.dispose]()`가 호출됨
```

- 런타임에서 대략 동등한 효과

```ts
function f() {
  const x = new C();
  try {
    doSomethingWith(x);
  }
  finally {
    x[Symbol.dispose]();
  }
}
```

- 파일 핸들 같은 네이티브 참조를 보유하는 JavaScript 객체 작업 시 메모리 누수 회피에 유용

```ts
{
  using file = await openFile();
  file.write(text);
  doSomethingThatMayThrow();
} // `file`은 오류가 발생해도 dispose됨
```

- 또는 추적 같은 스코프 작업

```ts
function f() {
  using activity = new TraceActivity("f"); // 함수 진입 추적
  // ...
} // 함수 종료 추적
```

- `var`, `let`, `const`와 달리 `using` 선언은 구조 분해 지원 안 함

#### `null`과 `undefined`

- 값이 `null`이나 `undefined`일 수 있음 → 이 경우 블록 끝에서 아무것도 dispose되지 않음

```ts
{
  using x = b ? new C() : null;
  // ...
}
```

- 대략 다음과 동등

```ts
{
  const x = b ? new C() : null;
  try {
    // ...
  }
  finally {
    x?.[Symbol.dispose]();
  }
}
```

- 이를 통해 복잡한 분기, 반복 없이 `using` 선언에서 조건부 리소스 획득 가능

#### disposable 리소스 정의

- 클래스, 객체가 disposable임을 나타내려면 `Disposable` 인터페이스 구현

```ts
// 기본 lib에서:
interface Disposable {
  [Symbol.dispose](): void;
}

// 사용법:
class TraceActivity implements Disposable {
  readonly name: string;
  constructor(name: string) {
    this.name = name;
    console.log(`Entering: ${name}`);
  }

  [Symbol.dispose](): void {
    console.log(`Exiting: ${name}`);
  }
}

function f() {
  using _activity = new TraceActivity("f");
  console.log("Hello world!");
}

f();
// 출력:
//   Entering: f
//   Hello world!
//   Exiting: f
```

### `await using` 선언

정리 작업을 비동기로 수행해야 하는 리소스도 있다. 이를 위해 [명시적 리소스 관리](https://github.com/tc39/proposal-explicit-resource-management) 제안은 `await using` 선언을 도입했다.

```ts
async function f() {
  await using x = new C();
} // `await x[Symbol.asyncDispose]()`가 호출됨
```

- `await using` 선언: 제어가 포함하는 블록을 벗어날 때 값의 `[Symbol.asyncDispose]()` 메서드를 호출하고 await
  - 데이터베이스 트랜잭션의 롤백, 커밋, 파일 스트림 닫기 전 보류 중인 쓰기 플러시 같은 비동기 정리 허용
- `await`와 마찬가지로 `await using`은 `async` 함수, 메서드, 모듈의 최상위 수준에서만 사용 가능

#### 비동기적으로 disposable한 리소스 정의

- `using`이 `Disposable` 객체에 의존하듯, `await using`은 `AsyncDisposable` 객체에 의존

```ts
// 기본 lib에서:
interface AsyncDisposable {
  [Symbol.asyncDispose]: PromiseLike<void>;
}

// 사용법:
class DatabaseTransaction implements AsyncDisposable {
  public success = false;
  private db: Database | undefined;

  private constructor(db: Database) {
    this.db = db;
  }

  static async create(db: Database) {
    await db.execAsync("BEGIN TRANSACTION");
    return new DatabaseTransaction(db);
  }

  async [Symbol.asyncDispose]() {
    if (this.db) {
      const db = this.db;
      this.db = undefined;
      if (this.success) {
        await db.execAsync("COMMIT TRANSACTION");
      }
      else {
        await db.execAsync("ROLLBACK TRANSACTION");
      }
    }
  }
}

async function transfer(db: Database, account1: Account, account2: Account, amount: number) {
  using tx = await DatabaseTransaction.create(db);
  if (await debitAccount(db, account1, amount)) {
    await creditAccount(db, account2, amount);
  }
  // 이 줄 전에 예외가 발생하면 트랜잭션이 롤백됨
  tx.success = true;
  // 이제 트랜잭션이 커밋됨
}
```

#### `await using` vs `await`

`await using`의 `await`는 리소스의 정리 작업을 기다린다는 뜻이다. 리소스를 얻는 과정도 비동기라면 할당할 값 앞에 별도로 `await`를 붙여야 한다.

```ts
{
  await using x = getResourceSynchronously();
} // `await x[Symbol.asyncDispose]()`를 수행

{
  await using y = await getResourceAsynchronously();
} // `await y[Symbol.asyncDispose]()`를 수행
```

#### `await using`과 `return`

- 먼저 `await`하지 않고 `Promise`를 반환하는 `async` 함수에서 `await using` 선언 사용 시 주의 사항 존재

```ts
function g() {
  return Promise.reject("error!");
}

async function f() {
  await using x = new C();
  return g(); // `await`가 누락됨
}
```

반환할 프로미스를 `await`하지 않으면, 런타임이 그 프로미스의 결과를 구독하기 전에 `x`의 비동기 정리를 기다리며 실행을 멈춘다. 그 사이 프로미스가 거부되면 처리되지 않은 거부로 보고될 수 있다. `try..finally`를 사용하는 `async` 함수에서도 같은 문제가 생길 수 있다.

```ts
async function f() {
  try {
    return g(); // 처리되지 않은 거부도 보고
  }
  finally {
    await somethingElse();
  }
}
```

- 회피 방법: 반환 값이 `Promise`일 수 있는 경우 `await`

```ts
async function f() {
  await using x = new C();
  return await g();
}
```

### `for` 및 `for..of` 문에서 `using`과 `await using`

- `using`, `await using` 모두 `for` 문에서 사용 가능

```ts
for (using x = getReader(); !x.eof; x.next()) {
  // ...
}
```

- 이 경우 `x`의 수명은 전체 `for` 문으로 스코프 → `break`, `return`, `throw`로 제어가 루프를 벗어나거나 루프 조건이 false일 때만 dispose
- `for` 문 외에도 두 선언 모두 `for..of` 문에서 사용 가능

```ts
function * g() {
  yield createResource1();
  yield createResource2();
}

for (using x of g()) {
  // ...
}
```

`for..of`에서는 각 반복이 끝날 때 `x`를 정리하고 다음 값으로 다시 초기화한다. 제너레이터가 리소스를 하나씩 생성한다면, 이 방식으로 각 리소스의 사용과 정리를 같은 반복 안에서 마칠 수 있다.

### 이전 런타임에서 `using`과 `await using`

- `Symbol.dispose`/`Symbol.asyncDispose`에 대한 호환 가능한 폴리필(예: 최근 버전 NodeJS 기본 제공) 사용 시 → 이전 ECMAScript 에디션 대상으로도 `using`, `await using` 선언 사용 가능

## 열거형 (Enums)

> 원문: https://www.typescriptlang.org/docs/handbook/enums.html

열거형은 이름이 있는 상수 집합을 정의하는 기능이다. 서로 구별되는 경우에 이름을 붙여 코드의 의도를 드러낼 수 있다. TypeScript는 숫자와 문자열 기반 열거형을 제공하며, 열거형은 타입 수준을 넘어 JavaScript의 실행 코드에도 영향을 주는 기능이다.

### 숫자 열거형

- `enum` 키워드로 정의

```ts
enum Direction {
  Up = 1,
  Down,
  Left,
  Right,
}
```

- `Up`이 `1`로 초기화된 숫자 열거형 → 그 이후 모든 멤버는 그 시점부터 자동 증가
  - `Direction.Up`은 `1`, `Down`은 `2`, `Left`는 `3`, `Right`는 `4`
- 이니셜라이저를 완전히 생략 가능

```ts
enum Direction {
  Up,
  Down,
  Left,
  Right,
}
```

- `Up`은 `0`, `Down`은 `1` 등
  - 자동 증가 동작: 멤버 값 자체는 신경 쓰지 않지만 각 값이 동일 열거형의 다른 값과 구별되어야 하는 경우에 유용
- 열거형 사용: 열거형 자체의 속성으로 멤버에 접근, 열거형 이름으로 타입 선언

```ts
enum UserResponse {
  No = 0,
  Yes = 1,
}

function respond(recipient: string, message: UserResponse): void {
  // ...
}

respond("Princess Caroline", UserResponse.Yes);
```

- 숫자 열거형은 [계산된 멤버와 상수 멤버(아래 참조)](#계산된-멤버와-상수-멤버)에 혼합 가능
  - 이니셜라이저가 없는 열거형은 첫 번째이거나, 숫자 상수 또는 다른 상수 열거형 멤버로 초기화된 숫자 열거형 뒤에 와야 함
  - 다음은 허용되지 않음

```ts
// 오류: 계산된 멤버 A 다음의 B에는 초기값을 지정해야 한다.
const getSomeValue = () => 23;
enum E {
  A = getSomeValue(),
  B,
}
```

### 문자열 열거형

- 비슷한 개념이지만 [런타임 차이](#런타임에서의-열거형) 존재
- 문자열 열거형에서 각 멤버는 문자열 리터럴 또는 다른 문자열 열거형 멤버로 상수 초기화돼야 함

```ts
enum Direction {
  Up = "UP",
  Down = "DOWN",
  Left = "LEFT",
  Right = "RIGHT",
}
```

문자열 열거형에는 자동 증가가 없지만, 직렬화한 값을 읽기 쉽다. 숫자 열거형은 디버깅할 때 값만 보고 의미를 알기 어려워 [역방향 매핑](#역방향-매핑)이 필요할 수 있다. 문자열 열거형은 멤버 이름과 별개로 실행 중에도 의미를 알 수 있는 값을 제공한다.

### 이기종 열거형

- 기술적으로 열거형은 문자열, 숫자 멤버 혼합 가능(권장 이유는 불명확)

```ts
enum BooleanLikeHeterogeneousEnum {
  No = 0,
  Yes = "YES",
}
```

- JavaScript 런타임 동작을 영리하게 활용하려는 것이 아니면 권장하지 않음

<a id="계산된-멤버와-상수-멤버"></a>

### 계산된 멤버와 상수 멤버

- 각 열거형 멤버는 상수이거나 계산된 값과 연관
- 열거형 멤버가 상수로 간주되는 경우
  - 열거형의 첫 번째 멤버이고 이니셜라이저가 없는 경우 → `0` 값 할당

```ts
// E.X는 상수:
enum E {
  X,
}
```

  - 이니셜라이저가 없고 앞의 열거형 멤버가 숫자 상수인 경우 → 현재 멤버 값은 앞 멤버 값에 1을 더한 값

```ts
// 'E1'과 'E2'의 모든 열거형 멤버는 상수

enum E1 {
  X,
  Y,
  Z,
}

enum E2 {
  A = 1,
  B,
  C,
}
```

  - 열거형 멤버가 상수 열거형 표현식으로 초기화된 경우
    - 상수 열거형 표현식: 컴파일 시간에 완전히 평가 가능한 TypeScript 표현식의 하위 집합
    - 상수 열거형 표현식으로 간주되는 경우
      - 리터럴 열거형 표현식(문자열 리터럴 또는 숫자 리터럴)
      - 이전에 정의된 상수 열거형 멤버에 대한 참조(다른 열거형에서 유래 가능)
      - 괄호로 묶인 상수 열거형 표현식
      - 상수 열거형 표현식에 적용된 `+`, `-`, `~` 단항 연산자 중 하나
      - 상수 열거형 표현식을 피연산자로 하는 `+`, `-`, `*`, `/`, `%`, `<<`, `>>`, `>>>`, `&`, `|`, `^` 이항 연산자
    - 상수 열거형 표현식이 `NaN`이나 `Infinity`로 평가되면 컴파일 시간 오류
- 다른 모든 경우 열거형 멤버는 계산된 것으로 간주

```ts
enum FileAccess {
  // 상수 멤버
  None,
  Read = 1 << 1,
  Write = 1 << 2,
  ReadWrite = Read | Write,
  // 계산된 멤버
  G = "123".length,
}
```

### 유니온 열거형과 열거형 멤버 타입

- 계산되지 않는 상수 열거형 멤버의 특별한 하위 집합: 리터럴 열거형 멤버
  - 초기화된 값이 없거나, 다음으로 초기화된 상수 열거형 멤버
    - 모든 문자열 리터럴(예: `"foo"`, `"bar"`, `"baz"`)
    - 모든 숫자 리터럴(예: `1`, `100`)
    - 모든 숫자 리터럴에 적용된 단항 마이너스(예: `-1`, `-100`)
- 열거형의 모든 멤버가 리터럴 열거형 값을 가지면 특별한 의미 적용
  - 열거형 멤버가 타입이 됨 → 특정 멤버가 열거형 멤버 값_만_ 가질 수 있다고 명시 가능

```ts
// 오류: Circle.kind에는 ShapeKind.Square를 할당할 수 없다.
enum ShapeKind {
  Circle,
  Square,
}

interface Circle {
  kind: ShapeKind.Circle;
  radius: number;
}

interface Square {
  kind: ShapeKind.Square;
  sideLength: number;
}

let c: Circle = {
  kind: ShapeKind.Square,
  radius: 100,
};
```

  - 열거형 타입 자체가 효과적으로 각 열거형 멤버의 유니온이 됨
    - 유니온 열거형 → 타입 시스템이 열거형 자체에 존재하는 정확한 값 집합을 안다는 사실 활용 가능
    - TypeScript가 값을 잘못 비교하는 버그를 잡을 수 있음

```ts
// 오류: 두 번째 비교에서 x는 E.Foo이므로 E.Bar와 겹치는 값이 없다.
enum E {
  Foo,
  Bar,
}

function f(x: E) {
  if (x !== E.Foo || x !== E.Bar) {
    //
  }
}
```

먼저 `x !== E.Foo`가 참이면 `||`의 단락 평가로 바로 `if` 본문에 들어간다. 거짓이면 `x`는 이미 `E.Foo`이므로 `E.Bar`와 다르다는 두 번째 검사도 항상 참이다. TypeScript는 열거형의 값 집합을 알고 있어 이런 비교를 찾아낼 수 있다.

<a id="런타임에서의-열거형"></a>

### 런타임에서의 열거형

- 열거형은 런타임에 존재하는 실제 객체
- 예: 다음 열거형

```ts
enum E {
  X,
  Y,
  Z,
}
```

- 실제로 함수에 전달 가능

```ts
enum E {
  X,
  Y,
  Z,
}

function f(obj: { X: number }) {
  return obj.X;
}

// 'E'에 숫자인 'X'라는 속성이 있으므로 작동
f(E);
```

### 컴파일 시점의 열거형

- 열거형이 런타임에 존재하는 실제 객체지만 `keyof` 키워드는 일반적인 객체와 다르게 작동
  - 모든 열거형 키를 문자열로 나타내는 타입을 얻으려면 `keyof typeof` 사용

```ts
enum LogLevel {
  ERROR,
  WARN,
  INFO,
  DEBUG,
}

/**
 * 이것은 다음과 동일:
 * type LogLevelStrings = 'ERROR' | 'WARN' | 'INFO' | 'DEBUG';
 */
type LogLevelStrings = keyof typeof LogLevel;

function printImportant(key: LogLevelStrings, message: string) {
  const num = LogLevel[key];
  if (num <= LogLevel.WARN) {
    console.log("Log level key is:", key);
    console.log("Log level value is:", num);
    console.log("Log level message is:", message);
  }
}
printImportant("ERROR", "This is a message");
```

<a id="역방향-매핑"></a>

#### 역방향 매핑

- 멤버의 속성 이름을 가진 객체를 만드는 것 외에도, 숫자 열거형 멤버는 열거형 값에서 열거형 이름으로의 역방향 매핑도 얻음

```ts
enum Enum {
  A,
}

let a = Enum.A;
let nameOfA = Enum[a]; // "A"
```

- TypeScript는 이것을 다음 JavaScript로 컴파일

```js
"use strict";
var Enum;
(function (Enum) {
    Enum[Enum["A"] = 0] = "A";
})(Enum || (Enum = {}));
let a = Enum.A;
let nameOfA = Enum[a]; // "A"
```

- 생성된 코드에서 열거형은 순방향(`name` -> `value`)과 역방향(`value` -> `name`) 매핑을 모두 저장하는 객체로 컴파일
- 다른 열거형 멤버에 대한 참조는 항상 속성 접근으로 방출되고 인라인되지 않음
- 문자열 열거형 멤버는 역방향 매핑이 전혀 생성되지 않음

#### `const` 열거형

- 대부분의 경우 열거형은 유효한 솔루션이지만 요구 사항이 더 엄격한 경우 존재
- 열거형 값 접근 시 추가 생성 코드 비용, 추가 간접 참조 비용 회피 목적 → `const` 열거형 사용
  - `const` 수정자로 정의

```ts
const enum Enum {
  A = 1,
  B = A * 2,
}
```

- const 열거형은 상수 열거형 표현식만 사용 가능 → 일반 열거형과 달리 컴파일 중에 완전히 제거
  - const 열거형 멤버는 사용 사이트에서 인라인
  - const 열거형은 계산된 멤버를 가질 수 없기 때문에 가능

```ts
const enum Direction {
  Up,
  Down,
  Left,
  Right,
}

let directions = [
  Direction.Up,
  Direction.Down,
  Direction.Left,
  Direction.Right,
];
```

- 생성된 코드

```js
"use strict";
let directions = [
    0 /* Direction.Up */,
    1 /* Direction.Down */,
    2 /* Direction.Left */,
    3 /* Direction.Right */,
];
```

- const 열거형의 함정

  - 열거형 값 인라인은 처음엔 간단하지만 미묘한 의미 존재
  - 이러한 함정은 앰비언트 const 열거형(기본적으로 `.d.ts` 파일의 const 열거형)에만 해당 → 프로젝트 간 공유와 관련
    - `.d.ts` 파일 게시, 소비 시 함정이 적용될 가능성 높음 → `tsc --declaration`이 `.ts` 파일을 `.d.ts` 파일로 변환하기 때문
  - 함정 목록
    - [`isolatedModules` 문서](https://www.typescriptlang.org/tsconfig#references-to-const-enum-members)에 설명된 이유로 해당 모드는 앰비언트 const 열거형과 근본적으로 호환 불가
      - 앰비언트 const 열거형 게시 → 다운스트림 소비자가 [`isolatedModules`](https://www.typescriptlang.org/tsconfig#isolatedModules)와 해당 열거형 값을 동시에 사용 불가
    - 컴파일 시간에 종속성 버전 A의 값을 인라인하고, 런타임에 버전 B를 가져올 수 있음
      - 매우 주의하지 않으면 버전 A, B의 열거형 값이 달라짐 → `if` 문의 잘못된 분기를 타는 [놀라운 버그](https://github.com/microsoft/TypeScript/issues/5219#issue-110947903) 발생
      - 자동화된 테스트를 프로젝트 빌드와 거의 동시에 동일한 종속성 버전으로 실행하는 것이 일반적 → 이런 버그를 완전히 놓치기 쉬움
    - [`importsNotUsedAsValues: "preserve"`](https://www.typescriptlang.org/tsconfig#importsNotUsedAsValues)는 값으로 사용되는 const 열거형에 대한 임포트를 제거하지 않지만, 앰비언트 const 열거형은 런타임 `.js` 파일 존재를 보장하지 않음
      - 해결할 수 없는 임포트는 런타임에 오류 발생
      - 임포트를 명확히 제거하는 일반적 방법인 [타입 전용 임포트](https://www.typescriptlang.org/docs/handbook/modules/reference.html#type-only-imports-and-exports)는 현재 [const 열거형 값을 허용하지 않음](https://github.com/microsoft/TypeScript/issues/40344)
  - 함정을 피하는 두 가지 접근 방식
    - const 열거형을 전혀 사용하지 않음
      - 린터로 [const 열거형 금지](https://typescript-eslint.io/linting/troubleshooting#how-can-i-ban-specific-language-feature) 가능
      - const 열거형의 모든 문제를 피하지만 프로젝트가 자체 열거형을 인라인하는 것을 방지
      - 다른 프로젝트의 열거형 인라인과 달리, 프로젝트 자체 열거형 인라인은 문제없고 성능에도 영향
    - [`preserveConstEnums`](https://www.typescriptlang.org/tsconfig#preserveConstEnums)로 const 열거형을 해제해 앰비언트 const 열거형을 게시하지 않음
      - [TypeScript 프로젝트 자체](https://github.com/microsoft/TypeScript/pull/5422)가 내부적으로 취하는 접근 방식
      - [`preserveConstEnums`](https://www.typescriptlang.org/tsconfig#preserveConstEnums)는 const 열거형에 대해 일반 열거형과 동일한 JavaScript를 방출
      - [빌드 단계](https://github.com/microsoft/TypeScript/blob/1a981d1df1810c868a66b3828497f049a944951c/Gulpfile.js#L144)에서 `.d.ts` 파일의 `const` 수정자를 안전하게 제거 가능
      - 다운스트림 소비자는 프로젝트에서 열거형을 인라인하지 않아 함정을 피하지만, const 열거형을 완전히 금지하는 것과 달리 프로젝트는 여전히 자체 열거형을 인라인 가능

### 앰비언트 열거형

- 앰비언트 열거형: 이미 존재하는 열거형 타입의 형태를 설명하는 데 사용

```ts
declare enum Enum {
  A = 1,
  B,
  C = 2,
}
```

- 앰비언트, 비앰비언트 열거형의 중요한 차이
  - 일반 열거형: 이니셜라이저가 없는 멤버는 앞의 멤버가 상수로 간주되면 상수로 간주
  - 앰비언트(및 비const) 열거형 멤버: 이니셜라이저가 없으면 항상 계산된 것으로 간주

### 객체 vs 열거형

- 현대 TypeScript에서는 `as const`가 있는 객체로 충분할 때 열거형이 필요하지 않을 수 있음

```ts
const enum EDirection {
  Up,
  Down,
  Left,
  Right,
}

const ODirection = {
  Up: 0,
  Down: 1,
  Left: 2,
  Right: 3,
} as const;

EDirection.Up;
// EDirection.Up: 열거형 멤버 타입 EDirection.Up, 값은 0

ODirection.Up;
// ODirection.Up의 타입: 0

// 열거형을 매개변수로 사용
function walk(dir: EDirection) {}

// 값을 가져오려면 추가 줄이 필요
type Direction = typeof ODirection[keyof typeof ODirection];
function run(dir: Direction) {}

walk(EDirection.Left);
run(ODirection.Right);
```

이 형식을 사용하면 JavaScript에 있는 문법으로 코드를 유지할 수 있다. 나중에 JavaScript에도 [열거형 문법이 도입된다면](https://github.com/rbuckton/proposal-enum) 그 문법으로 옮길 수 있다.

## 심볼 (Symbols)

> 원문: https://www.typescriptlang.org/docs/handbook/symbols.html

- ECMAScript 2015부터 `symbol`은 `number`, `string`처럼 원시 데이터 타입
- `symbol` 값은 `Symbol` 생성자를 호출해 생성

```ts
let sym1 = Symbol();

let sym2 = Symbol("key"); // 선택적 문자열 키
```

- 심볼은 불변이며 고유

```ts
let sym2 = Symbol("key");
let sym3 = Symbol("key");

sym2 === sym3; // false, 심볼은 고유
```

- 문자열처럼 심볼은 객체 속성의 키로 사용 가능

```ts
const sym = Symbol();

let obj = {
  [sym]: "value",
};

console.log(obj[sym]); // "value"
```

- 심볼은 계산된 속성 선언과 결합해 객체 속성, 클래스 멤버 선언에도 사용 가능

```ts
const getClassNameSymbol = Symbol();

class C {
  [getClassNameSymbol]() {
    return "C";
  }
}

let c = new C();
let className = c[getClassNameSymbol](); // "C"
```

### `unique symbol`

- 심볼을 고유한 리터럴로 처리하기 위한 특별한 타입: `unique symbol`
  - `symbol`의 하위 타입, `Symbol()` 또는 `Symbol.for()` 호출이나 명시적 타입 주석에서만 생성
  - `const` 선언과 `readonly static` 속성에만 허용
  - 특정 고유 심볼 참조 시 `typeof` 연산자 필요
  - 고유 심볼에 대한 각 참조는 주어진 선언에 연결된 완전히 고유한 정체성을 의미

```ts
// 오류: unique symbol 타입의 변수 sym2는 const로 선언해야 한다.
declare const sym1: unique symbol;

// sym2는 상수 참조만 가능
let sym2: unique symbol = Symbol();

// 작동함 - 고유 심볼을 참조하지만, 정체성은 'sym1'에 연결됨
let sym3: typeof sym1 = sym1;

// 이것도 작동
class C {
  static readonly StaticSymbol: unique symbol = Symbol();
}
```

- 각 `unique symbol`은 완전히 별개의 정체성을 가짐 → 두 `unique symbol` 타입은 서로 할당, 비교 불가

```ts
// 오류: sym2와 sym3는 서로 다른 고유 심볼 타입이므로 겹치는 값이 없다.
const sym2 = Symbol();
const sym3 = Symbol();

if (sym2 === sym3) {
  // ...
}
```

### 잘 알려진 심볼

사용자가 만드는 심볼 외에도 언어의 내부 동작에 쓰이는 내장 심볼이 있다. 다음은 각 심볼이 관여하는 동작이다.

#### `Symbol.asyncIterator`

- for await..of 루프와 함께 사용 가능한 객체에 대한 비동기 이터레이터를 반환하는 메서드

#### `Symbol.hasInstance`

- 생성자 객체가 객체를 생성자의 인스턴스 중 하나로 인식하는지 여부를 결정하는 메서드
- instanceof 연산자의 의미론에 의해 호출

#### `Symbol.isConcatSpreadable`

- Array.prototype.concat에 의해 객체가 배열 요소로 평탄화돼야 함을 나타내는 불리언 값

#### `Symbol.iterator`

- 객체의 기본 이터레이터를 반환하는 메서드
- for-of 문의 의미론에 의해 호출

#### `Symbol.match`

- 정규 표현식을 문자열과 매치하는 정규 표현식 메서드
- `String.prototype.match` 메서드에 의해 호출

#### `Symbol.replace`

- 문자열의 매치된 부분 문자열을 대체하는 정규 표현식 메서드
- `String.prototype.replace` 메서드에 의해 호출

#### `Symbol.search`

- 정규 표현식과 일치하는 문자열 내의 인덱스를 반환하는 정규 표현식 메서드
- `String.prototype.search` 메서드에 의해 호출

#### `Symbol.species`

- 파생 객체를 생성하는 데 사용되는 생성자 함수인 함수 값 속성

#### `Symbol.split`

- 정규 표현식과 일치하는 인덱스에서 문자열을 분할하는 정규 표현식 메서드
- `String.prototype.split` 메서드에 의해 호출

#### `Symbol.toPrimitive`

- 객체를 해당하는 원시 값으로 변환하는 메서드
- `ToPrimitive` 추상 연산에 의해 호출

#### `Symbol.toStringTag`

- 객체의 기본 문자열 설명 생성에 사용되는 문자열 값
- 내장 메서드 `Object.prototype.toString`에 의해 호출

#### `Symbol.unscopables`

- 자체 속성 이름이 연관된 객체의 'with' 환경 바인딩에서 제외되는 속성 이름인 객체

## 이터레이터와 제너레이터 (Iterators and Generators)

> 원문: https://www.typescriptlang.org/docs/handbook/iterators-and-generators.html

### 이터러블

- 객체가 [`Symbol.iterator`](https://www.typescriptlang.org/docs/handbook/symbols.html#symboliterator) 속성에 대한 구현이 있으면 이터러블로 간주
- `Array`, `Map`, `Set`, `String`, `Int32Array`, `Uint32Array` 등 일부 내장 타입에는 이미 `Symbol.iterator` 속성이 구현되어 있음
- 객체의 `Symbol.iterator` 함수는 반복할 값 목록을 반환하는 역할

#### `Iterable` 인터페이스

- `Iterable`: 이터러블인 위 나열 타입을 받고 싶을 때 사용할 수 있는 타입

```ts
function toArray<X>(xs: Iterable<X>): X[] {
  return [...xs]
}
```

#### `for..of` 문

- `for..of`: 이터러블 객체를 반복하며 객체의 `Symbol.iterator` 속성을 호출
- 배열에 대한 간단한 `for..of` 루프

```ts
let someArray = [1, "string", false];

for (let entry of someArray) {
  console.log(entry); // 1, "string", false
}
```

#### `for..of` vs. `for..in` 문

- `for..of`, `for..in` 문 모두 리스트를 반복하지만 반복되는 값이 다름
  - `for..in`: 반복되는 객체의 키 목록을 반환
  - `for..of`: 반복되는 객체의 숫자 속성의 값 목록을 반환

```ts
let list = [4, 5, 6];

for (let i in list) {
  console.log(i); // "0", "1", "2",
}

for (let i of list) {
  console.log(i); // 4, 5, 6
}
```

- 또 다른 구별
  - `for..in`은 어떤 객체에서도 작동 → 객체의 속성을 검사하는 방법 역할
  - `for..of`는 주로 이터러블 객체의 값에 관심 → `Map`, `Set` 같은 내장 객체는 저장된 값에 접근 가능하도록 `Symbol.iterator` 속성 구현

```ts
let pets = new Set(["Cat", "Dog", "Hamster"]);
pets["species"] = "mammals";

for (let pet in pets) {
  console.log(pet); // "species"
}

for (let pet of pets) {
  console.log(pet); // "Cat", "Dog", "Hamster"
}
```

#### 코드 생성

- ES5 대상

  - ES5 호환 엔진 대상 시 이터레이터는 `Array` 타입의 값에만 허용
    - 비Array 값이 `Symbol.iterator` 속성을 구현하더라도 비Array 값에 `for..of` 루프 사용은 오류
  - 컴파일러는 `for..of` 루프에 대해 간단한 `for` 루프 생성

  ```ts
  let numbers = [1, 2, 3];
  for (let num of numbers) {
    console.log(num);
  }
  ```

  - 생성 결과

  ```js
  var numbers = [1, 2, 3];
  for (var _i = 0; _i < numbers.length; _i++) {
    var num = numbers[_i];
    console.log(num);
  }
  ```

- ECMAScript 2015 이상 대상

  - ECMAScript 2015 호환 엔진 대상 시 컴파일러는 엔진의 내장 이터레이터 구현을 대상으로 `for..of` 루프 생성

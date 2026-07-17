# 언어 레퍼런스: 변수 선언 · 열거형 · 심벌 · 이터레이터

# 변수 선언 (Variable Declaration)

> **원문:** https://www.typescriptlang.org/docs/handbook/variable-declarations.html

`let`과 `const`는 JavaScript에서 변수 선언에 대한 비교적 새로운 두 가지 개념입니다.
[앞서 언급했듯이](/docs/handbook/basic-types.html#a-note-about-let), `let`은 일부 면에서 `var`와 유사하지만, 사용자가 JavaScript에서 겪는 일반적인 "함정"을 피할 수 있게 해줍니다.

`const`는 변수에 재할당을 방지한다는 점에서 `let`의 확장입니다.

TypeScript는 JavaScript의 확장이므로, 언어는 자연스럽게 `let`과 `const`를 지원합니다.
여기서 이러한 새로운 선언과 왜 `var`보다 선호되는지 더 자세히 설명하겠습니다.

JavaScript를 대충 사용해왔다면, 다음 섹션이 기억을 새롭게 하는 좋은 방법일 수 있습니다.
JavaScript에서 `var` 선언의 모든 특이점에 대해 잘 알고 있다면, 건너뛰어도 됩니다.

## `var` 선언

JavaScript에서 변수 선언은 전통적으로 `var` 키워드로 수행되었습니다.

```ts
var a = 10;
```

짐작하셨겠지만, 우리는 `10` 값을 가진 `a`라는 변수를 선언했습니다.

함수 내부에서도 변수를 선언할 수 있습니다:

```ts
function f() {
  var message = "Hello, world!";

  return message;
}
```

그리고 다른 함수 내에서 동일한 변수에 접근할 수도 있습니다:

```ts
function f() {
  var a = 10;
  return function g() {
    var b = a + 1;
    return b;
  };
}

var g = f();
g(); // '11'을 반환합니다
```

위 예제에서 `g`는 `f`에서 선언된 변수 `a`를 캡처했습니다.
`g`가 호출되는 어느 시점에서든, `a`의 값은 `f`의 `a` 값에 연결됩니다.
`f`가 실행을 완료한 후에도 `g`가 호출되면, `a`에 접근하고 수정할 수 있습니다.

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

f(); // '2'를 반환합니다
```

### 스코핑 규칙

`var` 선언은 다른 언어에 익숙한 사람들에게 이상한 스코핑 규칙이 있습니다.
다음 예제를 보세요:

```ts
function f(shouldInitialize: boolean) {
  if (shouldInitialize) {
    var x = 10;
  }

  return x;
}

f(true); // '10'을 반환합니다
f(false); // 'undefined'를 반환합니다
```

일부 독자들은 이 예제에서 두 번 놀랄 수 있습니다.
변수 `x`는 _`if` 블록 내에서_ 선언되었지만, 해당 블록 외부에서 접근할 수 있었습니다.
이것은 `var` 선언이 포함하는 블록에 관계없이 포함하는 함수, 모듈, 네임스페이스 또는 전역 스코프 내 어디에서나 접근할 수 있기 때문입니다 - 이 모든 것은 나중에 다룰 것입니다.
일부 사람들은 이것을 _`var`-스코핑_ 또는 _함수-스코핑_이라고 부릅니다.
매개변수도 함수 스코프입니다.

이러한 스코핑 규칙은 여러 유형의 실수를 유발할 수 있습니다.
악화시키는 한 가지 문제는 동일한 변수를 여러 번 선언하는 것이 오류가 아니라는 것입니다:

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

아마도 일부 경험 많은 JavaScript 개발자들에게는 쉽게 발견될 수 있었지만, 내부 `for` 루프는 `i`가 동일한 함수 스코프 변수를 참조하기 때문에 변수 `i`를 실수로 덮어씁니다.
이제 경험 많은 개발자들이 알고 있듯이, 유사한 종류의 버그가 코드 리뷰를 통과하고 끝없는 좌절의 원천이 될 수 있습니다.

### 변수 캡처의 특이점

다음 스니펫의 출력이 무엇인지 빠르게 추측해 보세요:

```ts
for (var i = 0; i < 10; i++) {
  setTimeout(function () {
    console.log(i);
  }, 100 * i);
}
```

익숙하지 않은 분들을 위해, `setTimeout`은 특정 밀리초 후에 함수를 실행하려고 합니다(다른 것이 실행을 멈추기를 기다리면서).

준비되셨나요? 확인해 보세요:

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

많은 JavaScript 개발자들이 이 동작에 익숙하지만, 놀랐다면 확실히 혼자가 아닙니다.
대부분의 사람들은 출력이 다음과 같을 것으로 기대합니다

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

앞서 변수 캡처에 대해 언급한 것을 기억하시나요?
`setTimeout`에 전달하는 모든 함수 표현식은 실제로 동일한 스코프의 동일한 `i`를 참조합니다.

이것이 무엇을 의미하는지 잠시 생각해 봅시다.
`setTimeout`은 몇 밀리초 후에 함수를 실행하지만, _오직_ `for` 루프가 실행을 멈춘 후에만;
`for` 루프가 실행을 멈출 때, `i`의 값은 `10`입니다.
따라서 주어진 함수가 호출될 때마다, `10`을 출력합니다!

일반적인 해결 방법은 IIFE - 즉시 호출 함수 표현식 - 을 사용하여 각 반복에서 `i`를 캡처하는 것입니다:

```ts
for (var i = 0; i < 10; i++) {
  // 현재 'i' 상태를 캡처합니다
  // 현재 값으로 함수를 호출하여
  (function (i) {
    setTimeout(function () {
      console.log(i);
    }, 100 * i);
  })(i);
}
```

이 이상해 보이는 패턴은 실제로 꽤 일반적입니다.
매개변수 목록의 `i`는 실제로 `for` 루프에서 선언된 `i`를 가리지만, 같은 이름을 지정했기 때문에 루프 본문을 너무 많이 수정할 필요가 없었습니다.

## `let` 선언

이제 `var`에 몇 가지 문제가 있다는 것을 알게 되었으며, 이것이 바로 `let` 문이 도입된 이유입니다.
사용된 키워드 외에, `let` 문은 `var` 문과 동일한 방식으로 작성됩니다.

```ts
let hello = "Hello!";
```

핵심 차이점은 구문이 아니라 의미론에 있으며, 이제 살펴보겠습니다.

### 블록-스코핑

`let`을 사용하여 변수를 선언하면, 일부에서 _렉시컬-스코핑_ 또는 _블록-스코핑_이라고 부르는 것을 사용합니다.
포함하는 함수로 스코프가 누출되는 `var`로 선언된 변수와 달리, 블록 스코프 변수는 가장 가까운 포함 블록 또는 `for` 루프 외부에서 볼 수 없습니다.

```ts
function f(input: boolean) {
  let a = 100;

  if (input) {
    // 여전히 'a'를 참조해도 됩니다
    let b = a + 1;
    return b;
  }

  // 오류: 'b'가 여기에 존재하지 않습니다
  return b;
}
```

여기에 두 개의 지역 변수 `a`와 `b`가 있습니다.
`a`의 스코프는 `f`의 본문으로 제한되고, `b`의 스코프는 포함하는 `if` 문의 블록으로 제한됩니다.

`catch` 절에서 선언된 변수도 유사한 스코핑 규칙을 가집니다.

```ts
try {
  throw "oh no!";
} catch (e) {
  console.log("Oh well.");
}

// 오류: 'e'가 여기에 존재하지 않습니다
console.log(e);
```

블록 스코프 변수의 또 다른 속성는 실제로 선언되기 전에 읽거나 쓸 수 없다는 것입니다.
이러한 변수가 스코프 전체에 "존재"하지만, 선언까지의 모든 지점은 _일시적 데드 존_의 일부입니다.
이것은 `let` 문 전에 접근할 수 없다는 세련된 표현이며, 다행히도 TypeScript가 알려줄 것입니다.

```ts
a++; // 선언되기 전에 'a'를 사용하는 것은 불법입니다;
let a;
```

주목할 점은 블록 스코프 변수가 선언되기 전에 여전히 _캡처_될 수 있다는 것입니다.
유일한 함정은 선언 전에 해당 함수를 호출하는 것이 불법이라는 것입니다.
ES2015를 대상으로 하면, 현대 런타임은 오류를 발생시킵니다; 그러나 현재 TypeScript는 관대하며 이것을 오류로 보고하지 않습니다.

```ts
function foo() {
  // 'a'를 캡처해도 됩니다
  return a;
}

// 'a'가 선언되기 전에 'foo'를 불법으로 호출
// 런타임에서 여기서 오류를 발생시켜야 합니다
foo();

let a;
```

일시적 데드 존에 대한 자세한 내용은 [Mozilla Developer Network](https://developer.mozilla.org/docs/Web/JavaScript/Reference/Statements/let#Temporal_dead_zone_and_errors_with_let)의 관련 콘텐츠를 참조하세요.

### 재선언과 섀도잉

`var` 선언에서, 변수를 몇 번 선언하든 상관없이 하나만 얻는다고 언급했습니다.

```ts
function f(x) {
  var x;
  var x;

  if (true) {
    var x;
  }
}
```

위 예제에서 `x`의 모든 선언은 실제로 _동일한_ `x`를 참조하며, 이것은 완벽하게 유효합니다.
이것은 종종 버그의 원인이 됩니다.
다행히, `let` 선언은 그렇게 관대하지 않습니다.

```ts
let x = 10;
let x = 20; // 오류: 같은 스코프에서 'x'를 재선언할 수 없습니다
```

변수가 모두 블록 스코프일 필요는 없으며 TypeScript가 문제가 있음을 알려줍니다.

```ts
function f(x) {
  let x = 100; // 오류: 매개변수 선언과 간섭합니다
}

function g() {
  let x = 100;
  var x = 100; // 오류: 'x'의 두 선언을 모두 가질 수 없습니다
}
```

이것은 블록 스코프 변수가 함수 스코프 변수로 절대 선언될 수 없다는 것을 의미하지 않습니다.
블록 스코프 변수는 명확히 다른 블록 내에서 선언되어야 합니다.

```ts
function f(condition, x) {
  if (condition) {
    let x = 100;
    return x;
  }

  return x;
}

f(false, 0); // '0'을 반환합니다
f(true, 0); // '100'을 반환합니다
```

더 중첩된 스코프에서 새 이름을 도입하는 행위를 _섀도잉_이라고 합니다.
우발적인 섀도잉의 경우 특정 버그를 도입할 수 있는 동시에 특정 버그를 방지할 수 있다는 점에서 양날의 검입니다.
예를 들어, `let` 변수를 사용하여 이전의 `sumMatrix` 함수를 작성했다고 상상해 보세요.

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

이 버전의 루프는 내부 루프의 `i`가 외부 루프의 `i`를 섀도잉하기 때문에 실제로 합계를 올바르게 수행합니다.

섀도잉은 더 명확한 코드를 작성하는 데 있어서 _보통_ 피해야 합니다.
이점을 활용하는 것이 적합한 일부 시나리오가 있을 수 있지만, 최선의 판단을 사용해야 합니다.

### 블록 스코프 변수 캡처

`var` 선언에서 변수 캡처 아이디어를 처음 접할 때, 캡처된 후 변수가 어떻게 동작하는지 간단히 살펴보았습니다.
이것에 대한 더 나은 직관을 제공하기 위해, 스코프가 실행될 때마다 변수의 "환경"을 생성합니다.
해당 환경과 캡처된 변수는 스코프 내의 모든 것이 실행을 완료한 후에도 존재할 수 있습니다.

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

`city`를 환경 내에서 캡처했기 때문에, `if` 블록이 실행을 완료했음에도 불구하고 여전히 접근할 수 있습니다.

이전 `setTimeout` 예제에서, `for` 루프의 모든 반복에 대한 변수 상태를 캡처하기 위해 IIFE를 사용해야 했던 것을 기억하세요.
실제로 우리가 한 것은 캡처된 변수에 대한 새로운 변수 환경을 만드는 것이었습니다.
그것은 약간 고통스러웠지만, 다행히도 TypeScript에서 더 이상 그렇게 할 필요가 없습니다.

`let` 선언은 루프의 일부로 선언될 때 크게 다른 동작을 합니다.
루프 자체에 새 환경을 도입하는 대신, 이러한 선언은 _반복당_ 새 스코프를 생성합니다.
어쨌든 IIFE로 이것을 하고 있었으므로, 이전 `setTimeout` 예제를 `let` 선언만 사용하도록 변경할 수 있습니다.

```ts
for (let i = 0; i < 10; i++) {
  setTimeout(function () {
    console.log(i);
  }, 100 * i);
}
```

그리고 예상대로, 이것은 다음을 출력합니다

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

## `const` 선언

`const` 선언은 변수를 선언하는 또 다른 방법입니다.

```ts
const numLivesForCat = 9;
```

`const`는 `let` 선언과 같지만, 이름에서 알 수 있듯이, 바인딩되면 값을 변경할 수 없습니다.
다시 말해, `let`과 동일한 스코핑 규칙을 가지지만, 재할당할 수 없습니다.

이것을 참조하는 값이 _불변_이라는 아이디어와 혼동해서는 안 됩니다.

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

이것을 피하기 위한 특별한 조치를 취하지 않는 한, `const` 변수의 내부 상태는 여전히 수정 가능합니다.
다행히, TypeScript는 객체의 멤버가 `readonly`임을 지정할 수 있습니다.
[인터페이스 챕터](/docs/handbook/interfaces.html)에 자세한 내용이 있습니다.

## `let` vs. `const`

유사한 스코핑 의미론을 가진 두 가지 유형의 선언이 있으므로, 어떤 것을 사용해야 하는지 자연스럽게 묻게 됩니다.
대부분의 광범위한 질문과 마찬가지로, 답은: 상황에 따라 다릅니다.

[최소 권한 원칙](https://wikipedia.org/wiki/Principle_of_least_privilege)을 적용하면, 수정할 계획인 것 외의 모든 선언은 `const`를 사용해야 합니다.
논리는 변수에 쓸 필요가 없는 경우, 같은 코드베이스에서 작업하는 다른 사람들이 자동으로 객체에 쓸 수 없어야 하며, 변수에 정말로 재할당해야 하는지 고려해야 한다는 것입니다.
`const`를 사용하면 데이터 흐름에 대해 추론할 때 코드를 더 예측 가능하게 만듭니다.

최선의 판단을 사용하고, 해당하는 경우 팀의 나머지와 문제를 상의하세요.

이 핸드북의 대부분은 `let` 선언을 사용합니다.

## 구조 분해

TypeScript에 있는 또 다른 ECMAScript 2015 기능은 구조 분해입니다.
전체 참조는 [Mozilla Developer Network의 글](https://developer.mozilla.org/docs/Web/JavaScript/Reference/Operators/Destructuring_assignment)을 참조하세요.
이 섹션에서는 간략한 개요를 제공합니다.

### 배열 구조 분해

가장 간단한 형태의 구조 분해는 배열 구조 분해 할당입니다:

```ts
let input = [1, 2];
let [first, second] = input;
console.log(first); // 1을 출력합니다
console.log(second); // 2를 출력합니다
```

이것은 `first`와 `second`라는 두 개의 새 변수를 생성합니다.
이것은 인덱싱을 사용하는 것과 동일하지만, 훨씬 더 편리합니다:

```ts
first = input[0];
second = input[1];
```

구조 분해는 이미 선언된 변수에서도 작동합니다:

```ts
// 변수 스왑
[first, second] = [second, first];
```

그리고 함수의 매개변수와 함께:

```ts
function f([first, second]: [number, number]) {
  console.log(first);
  console.log(second);
}
f([1, 2]);
```

`...` 구문을 사용하여 목록의 나머지 항목에 대한 변수를 만들 수 있습니다:

```ts
let [first, ...rest] = [1, 2, 3, 4];
console.log(first); // 1을 출력합니다
console.log(rest); // [ 2, 3, 4 ]를 출력합니다
```

물론, 이것이 JavaScript이므로, 신경 쓰지 않는 후행 요소를 무시할 수 있습니다:

```ts
let [first] = [1, 2, 3, 4];
console.log(first); // 1을 출력합니다
```

또는 다른 요소:

```ts
let [, second, , fourth] = [1, 2, 3, 4];
console.log(second); // 2를 출력합니다
console.log(fourth); // 4를 출력합니다
```

### 튜플 구조 분해

튜플은 배열처럼 구조 분해할 수 있습니다; 구조 분해 변수는 해당 튜플 요소의 타입을 얻습니다:

```ts
let tuple: [number, string, boolean] = [7, "hello", true];

let [a, b, c] = tuple; // a: number, b: string, c: boolean
```

튜플의 요소 범위를 넘어서 구조 분해하는 것은 오류입니다:

```ts
let [a, b, c, d] = tuple; // 오류, 인덱스 3에 요소가 없습니다
```

배열과 마찬가지로, `...`로 튜플의 나머지를 구조 분해하여 더 짧은 튜플을 얻을 수 있습니다:

```ts
let [a, ...bc] = tuple; // bc: [string, boolean]
let [a, b, c, ...d] = tuple; // d: [], 빈 튜플
```

또는 후행 요소나 다른 요소를 무시합니다:

```ts
let [a] = tuple; // a: number
let [, b] = tuple; // b: string
```

### 객체 구조 분해

객체도 구조 분해할 수 있습니다:

```ts
let o = {
  a: "foo",
  b: 12,
  c: "bar",
};
let { a, b } = o;
```

이것은 `o.a`와 `o.b`에서 새 변수 `a`와 `b`를 생성합니다.
필요하지 않으면 `c`를 건너뛸 수 있습니다.

배열 구조 분해처럼, 선언 없이 할당할 수 있습니다:

```ts
({ a, b } = { a: "baz", b: 101 });
```

이 문을 괄호로 묶어야 했음에 주목하세요.
JavaScript는 일반적으로 `{`를 블록의 시작으로 파싱합니다.

`...` 구문을 사용하여 객체의 나머지 항목에 대한 변수를 만들 수 있습니다:

```ts
let { a, ...passthrough } = o;
let total = passthrough.b + passthrough.c.length;
```

#### 속성 이름 바꾸기

속성에 다른 이름을 줄 수도 있습니다:

```ts
let { a: newName1, b: newName2 } = o;
```

여기서 구문이 혼란스러워지기 시작합니다.
`a: newName1`을 "`a`를 `newName1`로"라고 읽을 수 있습니다.
방향은 왼쪽에서 오른쪽으로이며, 마치 다음과 같이 작성한 것과 같습니다:

```ts
let newName1 = o.a;
let newName2 = o.b;
```

혼란스럽게도, 여기서 콜론은 타입을 나타내지 _않습니다_.
타입을 지정하면, 전체 구조 분해 후에 여전히 작성해야 합니다:

```ts
let { a: newName1, b: newName2 }: { a: string; b: number } = o;
```

#### 기본값

기본값을 사용하면 속성가 undefined인 경우 기본값을 지정할 수 있습니다:

```ts
function keepWholeObject(wholeObject: { a: string; b?: number }) {
  let { a, b = 1001 } = wholeObject;
}
```

이 예제에서 `b?`는 `b`가 선택적이므로 `undefined`일 수 있음을 나타냅니다.
`keepWholeObject`는 이제 `b`가 undefined인 경우에도 `wholeObject`뿐만 아니라 속성 `a`와 `b`에 대한 변수를 가집니다.

## 함수 선언

구조 분해는 함수 선언에서도 작동합니다.
간단한 경우에는 직관적입니다:

```ts
type C = { a: string; b?: number };
function f({ a, b }: C): void {
  // ...
}
```

그러나 매개변수에 대한 기본값 지정이 더 일반적이며, 구조 분해로 기본값을 올바르게 가져오는 것은 까다로울 수 있습니다.
우선, 기본값 전에 패턴을 넣어야 한다는 것을 기억해야 합니다.

```ts
function f({ a = "", b = 0 } = {}): void {
  // ...
}
f();
```

> 위의 스니펫은 핸드북에서 앞서 설명한 타입 추론의 예입니다.

그런 다음, 주 이니셜라이저가 아닌 구조 분해된 속성의 선택적 속성에 대한 기본값을 제공해야 한다는 것을 기억해야 합니다.
`C`가 `b`를 선택적으로 정의했음을 기억하세요:

```ts
function f({ a, b = 0 } = { a: "" }): void {
  // ...
}
f({ a: "yes" }); // ok, 기본값 b = 0
f(); // ok, 기본값 { a: "" }, 그런 다음 기본값 b = 0
f({}); // 오류, 인수를 제공하면 'a'가 필요합니다
```

구조 분해를 주의해서 사용하세요.
이전 예제에서 보여주듯이, 가장 간단한 구조 분해 표현식을 제외하고는 혼란스럽습니다.
이것은 특히 깊게 중첩된 구조 분해의 경우에 사실이며, 이름 바꾸기, 기본값, 타입 주석을 쌓아 올리지 않아도 이해하기 _정말_ 어려워집니다.
구조 분해 표현식을 작고 단순하게 유지하려고 노력하세요.
구조 분해가 생성할 할당을 항상 직접 작성할 수 있습니다.

## 스프레드

스프레드 연산자는 구조 분해의 반대입니다.
배열을 다른 배열로, 또는 객체를 다른 객체로 펼칠 수 있게 해줍니다.
예를 들어:

```ts
let first = [1, 2];
let second = [3, 4];
let bothPlus = [0, ...first, ...second, 5];
```

이것은 bothPlus에 `[0, 1, 2, 3, 4, 5]` 값을 제공합니다.
스프레딩은 `first`와 `second`의 얕은 복사본을 만듭니다.
원본 배열은 스프레드로 인해 변경되지 않습니다.

객체도 스프레드할 수 있습니다:

```ts
let defaults = { food: "spicy", price: "$$", ambiance: "noisy" };
let search = { ...defaults, food: "rich" };
```

이제 `search`는 `{ food: "rich", price: "$$", ambiance: "noisy" }`입니다.
객체 스프레딩은 배열 스프레딩보다 더 복잡합니다.
배열 스프레딩처럼 왼쪽에서 오른쪽으로 진행하지만, 결과는 여전히 객체입니다.
이것은 스프레드 객체에서 나중에 오는 속성가 이전에 오는 속성를 덮어쓴다는 것을 의미합니다.
따라서 이전 예제를 마지막에 스프레드하도록 수정하면:

```ts
let defaults = { food: "spicy", price: "$$", ambiance: "noisy" };
let search = { food: "rich", ...defaults };
```

그러면 `defaults`의 `food` 속성가 `food: "rich"`를 덮어쓰는데, 이 경우 우리가 원하는 것이 아닙니다.

객체 스프레드에는 몇 가지 다른 놀라운 제한도 있습니다.
첫째, 객체의 [자체, 열거 가능한 속성](https://developer.mozilla.org/docs/Web/JavaScript/Enumerability_and_ownership_of_properties)만 포함합니다.
기본적으로, 객체의 인스턴스를 스프레드할 때 메서드를 잃는다는 것을 의미합니다:

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

둘째, TypeScript 컴파일러는 제네릭 함수에서 타입 매개변수의 스프레드를 허용하지 않습니다.
이 기능은 향후 버전의 언어에서 예상됩니다.

## `using` 선언

`using` 선언은 [Stage 3 명시적 리소스 관리](https://github.com/tc39/proposal-explicit-resource-management) 제안의 일부인 JavaScript의 다가오는 기능입니다.
`using` 선언은 `const` 선언과 매우 유사하지만, 선언에 바인딩된 값의 _수명_을 변수의 _스코프_와 결합합니다.

`using` 선언을 포함하는 블록에서 제어가 벗어나면, 선언된 값의 `[Symbol.dispose]()` 메서드가 실행되어, 해당 값이 정리를 수행할 수 있게 합니다:

```ts
function f() {
  using x = new C();
  doSomethingWith(x);
} // `x[Symbol.dispose]()`가 호출됩니다
```

런타임에서 이것은 다음과 _대략_ 동등한 효과를 가집니다:

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

`using` 선언은 파일 핸들과 같은 네이티브 참조를 보유하는 JavaScript 객체로 작업할 때 메모리 누수를 피하는 데 매우 유용합니다

```ts
{
  using file = await openFile();
  file.write(text);
  doSomethingThatMayThrow();
} // `file`은 오류가 발생해도 dispose됩니다
```

또는 추적과 같은 스코프 작업

```ts
function f() {
  using activity = new TraceActivity("f"); // 함수 진입을 추적합니다
  // ...
} // 함수 종료를 추적합니다
```

`var`, `let`, `const`와 달리, `using` 선언은 구조 분해를 지원하지 않습니다.

### `null`과 `undefined`

값이 `null`이나 `undefined`일 수 있으며, 이 경우 블록 끝에서 아무것도 dispose되지 않는다는 것이 중요합니다:

```ts
{
  using x = b ? new C() : null;
  // ...
}
```

이것은 _대략_ 다음과 동등합니다:

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

이것을 통해 복잡한 분기나 반복 없이 `using` 선언을 선언할 때 조건부로 리소스를 획득할 수 있습니다.

### disposable 리소스 정의

생성하는 클래스나 객체가 disposable임을 나타내려면 `Disposable` 인터페이스를 구현하면 됩니다:

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

## `await using` 선언

일부 리소스나 작업에는 비동기적으로 수행해야 하는 정리가 있을 수 있습니다. 이를 수용하기 위해,
[명시적 리소스 관리](https://github.com/tc39/proposal-explicit-resource-management) 제안은 `await using` 선언도 도입합니다:

```ts
async function f() {
  await using x = new C();
} // `await x[Symbol.asyncDispose]()`가 호출됩니다
```

`await using` 선언은 제어가 포함하는 블록을 벗어날 때 값의 `[Symbol.asyncDispose]()` 메서드를 호출하고 _await_합니다. 이것은 데이터베이스 트랜잭션이 롤백이나 커밋을 수행하거나, 파일 스트림이 닫히기 전에 보류 중인 쓰기를 스토리지로 플러시하는 것과 같은 비동기 정리를 허용합니다.

`await`와 마찬가지로, `await using`은 `async` 함수나 메서드, 또는 모듈의 최상위 수준에서만 사용할 수 있습니다.

### 비동기적으로 disposable한 리소스 정의

`using`이 `Disposable`인 객체에 의존하는 것처럼, `await using`은 `AsyncDisposable`인 객체에 의존합니다:

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
  // 이 줄 전에 예외가 발생하면 트랜잭션이 롤백됩니다
  tx.success = true;
  // 이제 트랜잭션이 커밋됩니다
}
```

### `await using` vs `await`

`await using` 선언의 일부인 `await` 키워드는 리소스의 _disposal_만 `await`된다는 것을 나타냅니다. 값 자체를 `await`하지 *않습니다*:

```ts
{
  await using x = getResourceSynchronously();
} // `await x[Symbol.asyncDispose]()`를 수행합니다

{
  await using y = await getResourceAsynchronously();
} // `await y[Symbol.asyncDispose]()`를 수행합니다
```

### `await using`과 `return`

먼저 `await`하지 않고 `Promise`를 반환하는 `async` 함수에서 `await using` 선언을 사용하는 경우 이 동작에 약간의 주의 사항이 있다는 점을 기억하는 것이 중요합니다:

```ts
function g() {
  return Promise.reject("error!");
}

async function f() {
  await using x = new C();
  return g(); // `await`가 누락됨
}
```

반환된 프로미스가 `await`되지 않으므로, JavaScript 런타임이 반환된 프로미스를 구독하지 않고 `x`의 비동기 disposal을 `await`하는 동안 실행이 일시 중지되어 처리되지 않은 거부를 보고할 수 있습니다. 그러나 이것은 `await using`에 고유한 문제가 아니며, `try..finally`를 사용하는 `async` 함수에서도 발생할 수 있습니다:

```ts
async function f() {
  try {
    return g(); // 처리되지 않은 거부도 보고합니다
  }
  finally {
    await somethingElse();
  }
}
```

이 상황을 피하려면, 반환 값이 `Promise`일 수 있는 경우 `await`하는 것이 좋습니다:

```ts
async function f() {
  await using x = new C();
  return await g();
}
```

## `for` 및 `for..of` 문에서 `using`과 `await using`

`using`과 `await using` 모두 `for` 문에서 사용할 수 있습니다:

```ts
for (using x = getReader(); !x.eof; x.next()) {
  // ...
}
```

이 경우 `x`의 수명은 전체 `for` 문으로 스코프되며, `break`, `return`, `throw`로 인해 제어가 루프를 벗어나거나 루프 조건이 false일 때만 dispose됩니다.

`for` 문 외에도, 두 선언 모두 `for..of` 문에서 사용할 수 있습니다:

```ts
function * g() {
  yield createResource1();
  yield createResource2();
}

for (using x of g()) {
  // ...
}
```

여기서 `x`는 _루프의 각 반복 끝에서_ dispose되고, 그런 다음 다음 값으로 다시 초기화됩니다. 이것은 제너레이터에 의해 한 번에 하나씩 생성되는 리소스를 소비할 때 특히 유용합니다.

## 이전 런타임에서 `using`과 `await using`

`Symbol.dispose`/`Symbol.asyncDispose`에 대한 호환 가능한 폴리필(예: 최근 버전의 NodeJS에서 기본으로 제공하는 것)을 사용하는 한, 이전 ECMAScript 에디션을 대상으로 할 때 `using`과 `await using` 선언을 사용할 수 있습니다.

---

# 열거형 (Enums)

> **원문:** https://www.typescriptlang.org/docs/handbook/enums.html

열거형은 TypeScript가 JavaScript의 타입 수준 확장이 아닌 몇 안 되는 기능 중 하나입니다.

열거형을 사용하면 개발자가 명명된 상수 집합을 정의할 수 있습니다.
열거형을 사용하면 의도를 문서화하거나 구별되는 케이스 집합을 만드는 것이 더 쉬워집니다.
TypeScript는 숫자 및 문자열 기반 열거형을 모두 제공합니다.

## 숫자 열거형

먼저 숫자 열거형부터 시작하겠습니다. 다른 언어를 사용해본 경우 아마 더 익숙할 것입니다.
열거형은 `enum` 키워드를 사용하여 정의할 수 있습니다.

```ts
enum Direction {
  Up = 1,
  Down,
  Left,
  Right,
}
```

위에서 `Up`이 `1`로 초기화된 숫자 열거형이 있습니다.
그 이후의 모든 멤버는 그 시점부터 자동 증가됩니다.
다시 말해, `Direction.Up`은 `1`, `Down`은 `2`, `Left`는 `3`, `Right`는 `4` 값을 가집니다.

원한다면 이니셜라이저를 완전히 생략할 수 있습니다:

```ts
enum Direction {
  Up,
  Down,
  Left,
  Right,
}
```

여기서 `Up`은 `0`, `Down`은 `1` 등의 값을 가집니다.
이 자동 증가 동작은 멤버 값 자체는 신경 쓰지 않지만 각 값이 동일한 열거형의 다른 값과 구별되어야 하는 경우에 유용합니다.

열거형 사용은 간단합니다: 열거형 자체의 속성로 멤버에 접근하고, 열거형 이름을 사용하여 타입을 선언합니다:

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

숫자 열거형은 [계산된 멤버와 상수 멤버(아래 참조)](#계산된-멤버와-상수-멤버)에 혼합될 수 있습니다.
간단히 말해서, 이니셜라이저가 없는 열거형은 첫 번째이거나, 숫자 상수 또는 다른 상수 열거형 멤버로 초기화된 숫자 열거형 뒤에 와야 합니다.
다시 말해 다음은 허용되지 않습니다:

```ts
// @errors: 1061
const getSomeValue = () => 23;
// ---cut---
enum E {
  A = getSomeValue(),
  B,
}
```

## 문자열 열거형

문자열 열거형은 비슷한 개념이지만 아래에 문서화된 것처럼 약간의 미묘한 [런타임 차이](#런타임에서의-열거형)가 있습니다.
문자열 열거형에서 각 멤버는 문자열 리터럴 또는 다른 문자열 열거형 멤버로 상수 초기화되어야 합니다.

```ts
enum Direction {
  Up = "UP",
  Down = "DOWN",
  Left = "LEFT",
  Right = "RIGHT",
}
```

문자열 열거형에는 자동 증가 동작이 없지만, "직렬화"가 잘 된다는 이점이 있습니다.
다시 말해서, 디버깅 중에 숫자 열거형의 런타임 값을 읽어야 하는 경우, 그 값은 종종 불투명합니다 - 그 자체로는 유용한 의미를 전달하지 않습니다([역방향 매핑](#역방향-매핑)이 종종 도움이 될 수 있지만). 문자열 열거형을 사용하면 열거형 멤버의 이름과 독립적으로 코드가 실행될 때 의미 있고 읽기 쉬운 값을 제공할 수 있습니다.

## 이기종 열거형

기술적으로 열거형은 문자열과 숫자 멤버를 혼합할 수 있지만, 왜 그렇게 하고 싶은지 명확하지 않습니다:

```ts
enum BooleanLikeHeterogeneousEnum {
  No = 0,
  Yes = "YES",
}
```

JavaScript의 런타임 동작을 영리하게 활용하려는 것이 아니라면, 이렇게 하지 않는 것이 좋습니다.

## 계산된 멤버와 상수 멤버

각 열거형 멤버에는 _상수_이거나 _계산된_ 값이 연관되어 있습니다.
열거형 멤버는 다음과 같은 경우 상수로 간주됩니다:

- 열거형의 첫 번째 멤버이고 이니셜라이저가 없는 경우, 이 경우 `0` 값이 할당됩니다:

  ```ts
  // E.X는 상수입니다:
  enum E {
    X,
  }
  ```

- 이니셜라이저가 없고 앞의 열거형 멤버가 _숫자_ 상수인 경우.
  이 경우 현재 열거형 멤버의 값은 앞의 열거형 멤버의 값에 1을 더한 값이 됩니다.

  ```ts
  // 'E1'과 'E2'의 모든 열거형 멤버는 상수입니다.

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

- 열거형 멤버가 상수 열거형 표현식으로 초기화된 경우.
  상수 열거형 표현식은 컴파일 시간에 완전히 평가할 수 있는 TypeScript 표현식의 하위 집합입니다.
  표현식이 다음과 같은 경우 상수 열거형 표현식입니다:

  1. 리터럴 열거형 표현식(기본적으로 문자열 리터럴 또는 숫자 리터럴)
  2. 이전에 정의된 상수 열거형 멤버에 대한 참조(다른 열거형에서 유래할 수 있음)
  3. 괄호로 묶인 상수 열거형 표현식
  4. 상수 열거형 표현식에 적용된 `+`, `-`, `~` 단항 연산자 중 하나
  5. 상수 열거형 표현식을 피연산자로 하는 `+`, `-`, `*`, `/`, `%`, `<<`, `>>`, `>>>`, `&`, `|`, `^` 이항 연산자

  상수 열거형 표현식이 `NaN`이나 `Infinity`로 평가되면 컴파일 시간 오류입니다.

다른 모든 경우에 열거형 멤버는 계산된 것으로 간주됩니다.

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

## 유니온 열거형과 열거형 멤버 타입

계산되지 않는 상수 열거형 멤버의 특별한 하위 집합이 있습니다: 리터럴 열거형 멤버.
리터럴 열거형 멤버는 초기화된 값이 없거나, 다음으로 초기화된 상수 열거형 멤버입니다:

- 모든 문자열 리터럴(예: `"foo"`, `"bar"`, `"baz"`)
- 모든 숫자 리터럴(예: `1`, `100`)
- 모든 숫자 리터럴에 적용된 단항 마이너스(예: `-1`, `-100`)

열거형의 모든 멤버가 리터럴 열거형 값을 가지면, 몇 가지 특별한 의미가 적용됩니다.

첫 번째는 열거형 멤버가 타입이 된다는 것입니다!
예를 들어, 특정 멤버가 열거형 멤버의 값_만_ 가질 수 있다고 말할 수 있습니다:

```ts
// @errors: 2322
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

다른 변경 사항은 열거형 타입 자체가 효과적으로 각 열거형 멤버의 _유니온_이 된다는 것입니다.
유니온 열거형을 사용하면, 타입 시스템이 열거형 자체에 존재하는 정확한 값 집합을 알고 있다는 사실을 활용할 수 있습니다.
그 때문에 TypeScript는 값을 잘못 비교하는 버그를 잡을 수 있습니다.
예를 들어:

```ts
// @errors: 2367
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

이 예제에서 우리는 먼저 `x`가 `E.Foo`가 _아닌지_ 확인했습니다.
그 검사가 성공하면, `||`가 단락되어 'if'의 본문이 실행됩니다.
그러나 검사가 성공하지 않으면, `x`는 `E.Foo`_만_ 될 수 있으므로, `E.Bar`와 _같지 않은지_ 확인하는 것은 의미가 없습니다.

## 런타임에서의 열거형

열거형은 런타임에 존재하는 실제 객체입니다.
예를 들어, 다음 열거형

```ts
enum E {
  X,
  Y,
  Z,
}
```

은 실제로 함수에 전달될 수 있습니다

```ts
enum E {
  X,
  Y,
  Z,
}

function f(obj: { X: number }) {
  return obj.X;
}

// 'E'에 숫자인 'X'라는 속성가 있으므로 작동합니다.
f(E);
```

## 컴파일 시점의 열거형

열거형이 런타임에 존재하는 실제 객체이지만, `keyof` 키워드는 일반적인 객체에 대해 예상하는 것과 다르게 작동합니다. 대신, 모든 열거형 키를 문자열로 나타내는 타입을 얻으려면 `keyof typeof`를 사용합니다.

```ts
enum LogLevel {
  ERROR,
  WARN,
  INFO,
  DEBUG,
}

/**
 * 이것은 다음과 동일합니다:
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

### 역방향 매핑

멤버의 속성 이름을 가진 객체를 만드는 것 외에도, 숫자 열거형 멤버는 열거형 값에서 열거형 이름으로의 _역방향 매핑_도 얻습니다.
예를 들어, 이 예제에서:

```ts
enum Enum {
  A,
}

let a = Enum.A;
let nameOfA = Enum[a]; // "A"
```

TypeScript는 이것을 다음 JavaScript로 컴파일합니다:

```js
"use strict";
var Enum;
(function (Enum) {
    Enum[Enum["A"] = 0] = "A";
})(Enum || (Enum = {}));
let a = Enum.A;
let nameOfA = Enum[a]; // "A"
```

이 생성된 코드에서 열거형은 순방향(`name` -> `value`)과 역방향(`value` -> `name`) 매핑을 모두 저장하는 객체로 컴파일됩니다.
다른 열거형 멤버에 대한 참조는 항상 속성 접근으로 방출되고 인라인되지 않습니다.

문자열 열거형 멤버는 역방향 매핑이 전혀 생성되지 _않는다_는 것을 명심하세요.

### `const` 열거형

대부분의 경우 열거형은 완벽하게 유효한 솔루션입니다.
그러나 때때로 요구 사항이 더 엄격합니다.
열거형 값에 접근할 때 추가 생성 코드 비용과 추가 간접 참조 비용을 피하려면, `const` 열거형을 사용할 수 있습니다.
const 열거형은 열거형에 `const` 수정자를 사용하여 정의됩니다:

```ts
const enum Enum {
  A = 1,
  B = A * 2,
}
```

const 열거형은 상수 열거형 표현식만 사용할 수 있으며 일반 열거형과 달리 컴파일 중에 완전히 제거됩니다.
const 열거형 멤버는 사용 사이트에서 인라인됩니다.
const 열거형은 계산된 멤버를 가질 수 없기 때문에 이것이 가능합니다.

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

생성된 코드에서는 다음이 됩니다

```js
"use strict";
let directions = [
    0 /* Direction.Up */,
    1 /* Direction.Down */,
    2 /* Direction.Left */,
    3 /* Direction.Right */,
];
```

#### const 열거형의 함정

열거형 값 인라인은 처음에는 간단하지만 미묘한 의미가 있습니다.
이러한 함정은 _앰비언트_ const 열거형(기본적으로 `.d.ts` 파일의 const 열거형)에만 해당되며 프로젝트 간에 공유하는 것과 관련이 있지만, `.d.ts` 파일을 게시하거나 소비하는 경우, 이러한 함정이 적용될 가능성이 높습니다. `tsc --declaration`이 `.ts` 파일을 `.d.ts` 파일로 변환하기 때문입니다.

1. [`isolatedModules` 문서](/tsconfig#references-to-const-enum-members)에 설명된 이유로, 해당 모드는 앰비언트 const 열거형과 근본적으로 호환되지 않습니다.
   이것은 앰비언트 const 열거형을 게시하면, 다운스트림 소비자가 [`isolatedModules`](/tsconfig#isolatedModules)와 해당 열거형 값을 동시에 사용할 수 없음을 의미합니다.
2. 컴파일 시간에 종속성의 버전 A에서 값을 쉽게 인라인하고, 런타임에 버전 B를 가져올 수 있습니다.
   매우 주의하지 않으면 버전 A와 B의 열거형 값이 다를 수 있어, `if` 문의 잘못된 분기를 타는 것과 같은 [놀라운 버그](https://github.com/microsoft/TypeScript/issues/5219#issue-110947903)가 발생합니다.
   이러한 버그는 자동화된 테스트를 프로젝트 빌드와 거의 동시에 동일한 종속성 버전으로 실행하는 것이 일반적이므로 특히 악랄합니다. 이러한 버그를 완전히 놓치게 됩니다.
3. [`importsNotUsedAsValues: "preserve"`](/tsconfig#importsNotUsedAsValues)는 값으로 사용되는 const 열거형에 대한 임포트를 제거하지 않지만, 앰비언트 const 열거형은 런타임 `.js` 파일이 존재함을 보장하지 않습니다.
   해결할 수 없는 임포트는 런타임에 오류를 발생시킵니다.
   임포트를 명확하게 제거하는 일반적인 방법인 [타입 전용 임포트](/docs/handbook/modules/reference.html#type-only-imports-and-exports)는 현재 [const 열거형 값을 허용하지 않습니다](https://github.com/microsoft/TypeScript/issues/40344).

다음은 이러한 함정을 피하는 두 가지 접근 방식입니다:

1. const 열거형을 전혀 사용하지 않습니다.
   린터의 도움으로 [const 열거형을 금지](https://typescript-eslint.io/linting/troubleshooting#how-can-i-ban-specific-language-feature)할 수 있습니다.
   분명히 이것은 const 열거형의 모든 문제를 피하지만, 프로젝트가 자체 열거형을 인라인하는 것을 방지합니다.
   다른 프로젝트에서 열거형을 인라인하는 것과 달리, 프로젝트 자체 열거형을 인라인하는 것은 문제가 없으며 성능에 영향을 미칩니다.
2. [`preserveConstEnums`](/tsconfig#preserveConstEnums)의 도움으로 const 열거형을 해제하여 앰비언트 const 열거형을 게시하지 않습니다.
   이것은 [TypeScript 프로젝트 자체](https://github.com/microsoft/TypeScript/pull/5422)가 내부적으로 취하는 접근 방식입니다.
   [`preserveConstEnums`](/tsconfig#preserveConstEnums)는 const 열거형에 대해 일반 열거형과 동일한 JavaScript를 방출합니다.
   그런 다음 [빌드 단계](https://github.com/microsoft/TypeScript/blob/1a981d1df1810c868a66b3828497f049a944951c/Gulpfile.js#L144)에서 `.d.ts` 파일에서 `const` 수정자를 안전하게 제거할 수 있습니다.

   이 방법으로 다운스트림 소비자는 프로젝트에서 열거형을 인라인하지 않아 위의 함정을 피하지만, const 열거형을 완전히 금지하는 것과 달리 프로젝트는 여전히 자체 열거형을 인라인할 수 있습니다.

## 앰비언트 열거형

앰비언트 열거형은 이미 존재하는 열거형 타입의 형태를 설명하는 데 사용됩니다.

```ts
declare enum Enum {
  A = 1,
  B,
  C = 2,
}
```

앰비언트 열거형과 비앰비언트 열거형의 중요한 차이점은, 일반 열거형에서 이니셜라이저가 없는 멤버는 앞의 열거형 멤버가 상수로 간주되면 상수로 간주된다는 것입니다.
반면, 앰비언트(및 비const) 열거형 멤버가 이니셜라이저가 없으면 _항상_ 계산된 것으로 간주됩니다.

## 객체 vs 열거형

현대 TypeScript에서는 `as const`가 있는 객체로 충분할 때 열거형이 필요하지 않을 수 있습니다:

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
//         ^? (enum member) EDirection.Up = 0

ODirection.Up;
//         ^? (property) Up: 0

// 열거형을 매개변수로 사용
function walk(dir: EDirection) {}

// 값을 가져오려면 추가 줄이 필요합니다
type Direction = typeof ODirection[keyof typeof ODirection];
function run(dir: Direction) {}

walk(EDirection.Left);
run(ODirection.Right);
```

TypeScript의 `enum`보다 이 형식을 선호하는 가장 큰 논거는 JavaScript의 상태와 코드베이스를 정렬하고, 열거형이 JavaScript에 추가될 [때/만약](https://github.com/rbuckton/proposal-enum) 추가 구문으로 이동할 수 있다는 것입니다.

---

# 심볼 (Symbols)

> **원문:** https://www.typescriptlang.org/docs/handbook/symbols.html

ECMAScript 2015부터 `symbol`은 `number`와 `string`처럼 원시 데이터 타입입니다.

`symbol` 값은 `Symbol` 생성자를 호출하여 생성됩니다.

```ts
let sym1 = Symbol();

let sym2 = Symbol("key"); // 선택적 문자열 키
```

심볼은 불변이며 고유합니다.

```ts
let sym2 = Symbol("key");
let sym3 = Symbol("key");

sym2 === sym3; // false, 심볼은 고유합니다
```

문자열처럼, 심볼은 객체 속성의 키로 사용할 수 있습니다.

```ts
const sym = Symbol();

let obj = {
  [sym]: "value",
};

console.log(obj[sym]); // "value"
```

심볼은 계산된 속성 선언과 결합하여 객체 속성와 클래스 멤버를 선언하는 데에도 사용할 수 있습니다.

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

## `unique symbol`

심볼을 고유한 리터럴로 처리하기 위해 `unique symbol`이라는 특별한 타입을 사용할 수 있습니다. `unique symbol`은 `symbol`의 하위 타입이며, `Symbol()` 또는 `Symbol.for()` 호출이나 명시적 타입 주석에서만 생성됩니다. 이 타입은 `const` 선언과 `readonly static` 속성에만 허용되며, 특정 고유 심볼을 참조하려면 `typeof` 연산자를 사용해야 합니다. 고유 심볼에 대한 각 참조는 주어진 선언에 연결된 완전히 고유한 정체성을 의미합니다.

```ts
// @errors: 1332
declare const sym1: unique symbol;

// sym2는 상수 참조만 될 수 있습니다.
let sym2: unique symbol = Symbol();

// 작동함 - 고유 심볼을 참조하지만, 정체성은 'sym1'에 연결됨.
let sym3: typeof sym1 = sym1;

// 이것도 작동합니다.
class C {
  static readonly StaticSymbol: unique symbol = Symbol();
}
```

각 `unique symbol`은 완전히 별개의 정체성을 가지므로, 두 `unique symbol` 타입은 서로 할당하거나 비교할 수 없습니다.

```ts
// @errors: 2367
const sym2 = Symbol();
const sym3 = Symbol();

if (sym2 === sym3) {
  // ...
}
```

## 잘 알려진 심볼

사용자 정의 심볼 외에도 잘 알려진 내장 심볼이 있습니다.
내장 심볼은 내부 언어 동작을 나타내는 데 사용됩니다.

다음은 잘 알려진 심볼 목록입니다:

### `Symbol.asyncIterator`

for await..of 루프와 함께 사용할 수 있는 객체에 대한 비동기 이터레이터를 반환하는 메서드입니다.

### `Symbol.hasInstance`

생성자 객체가 객체를 생성자의 인스턴스 중 하나로 인식하는지 여부를 결정하는 메서드입니다. instanceof 연산자의 의미론에 의해 호출됩니다.

### `Symbol.isConcatSpreadable`

Array.prototype.concat에 의해 객체가 배열 요소로 평탄화되어야 함을 나타내는 불리언 값입니다.

### `Symbol.iterator`

객체의 기본 이터레이터를 반환하는 메서드입니다. for-of 문의 의미론에 의해 호출됩니다.

### `Symbol.match`

정규 표현식을 문자열과 매치하는 정규 표현식 메서드입니다. `String.prototype.match` 메서드에 의해 호출됩니다.

### `Symbol.replace`

문자열의 매치된 부분 문자열을 대체하는 정규 표현식 메서드입니다. `String.prototype.replace` 메서드에 의해 호출됩니다.

### `Symbol.search`

정규 표현식과 일치하는 문자열 내의 인덱스를 반환하는 정규 표현식 메서드입니다. `String.prototype.search` 메서드에 의해 호출됩니다.

### `Symbol.species`

파생 객체를 생성하는 데 사용되는 생성자 함수인 함수 값 속성입니다.

### `Symbol.split`

정규 표현식과 일치하는 인덱스에서 문자열을 분할하는 정규 표현식 메서드입니다.
`String.prototype.split` 메서드에 의해 호출됩니다.

### `Symbol.toPrimitive`

객체를 해당하는 원시 값으로 변환하는 메서드입니다.
`ToPrimitive` 추상 연산에 의해 호출됩니다.

### `Symbol.toStringTag`

객체의 기본 문자열 설명 생성에 사용되는 문자열 값입니다.
내장 메서드 `Object.prototype.toString`에 의해 호출됩니다.

### `Symbol.unscopables`

자체 속성 이름이 연관된 객체의 'with' 환경 바인딩에서 제외되는 속성 이름인 객체입니다.

---

# 이터레이터와 제너레이터 (Iterators and Generators)

> **원문:** https://www.typescriptlang.org/docs/handbook/iterators-and-generators.html

## 이터러블

객체가 [`Symbol.iterator`](symbols.html#symboliterator) 속성에 대한 구현이 있으면 이터러블로 간주됩니다.
`Array`, `Map`, `Set`, `String`, `Int32Array`, `Uint32Array` 등과 같은 일부 내장 타입에는 이미 `Symbol.iterator` 속성가 구현되어 있습니다.
객체의 `Symbol.iterator` 함수는 반복할 값 목록을 반환하는 역할을 합니다.

### `Iterable` 인터페이스

`Iterable`은 이터러블인 위에 나열된 타입을 받고 싶을 때 사용할 수 있는 타입입니다. 다음은 예제입니다:

```ts
function toArray<X>(xs: Iterable<X>): X[] {
  return [...xs]
}
```

### `for..of` 문

`for..of`는 이터러블 객체를 반복하며, 객체의 `Symbol.iterator` 속성를 호출합니다.
배열에 대한 간단한 `for..of` 루프는 다음과 같습니다:

```ts
let someArray = [1, "string", false];

for (let entry of someArray) {
  console.log(entry); // 1, "string", false
}
```

### `for..of` vs. `for..in` 문

`for..of`와 `for..in` 문 모두 리스트를 반복합니다; 반복되는 값은 다르지만, `for..in`은 반복되는 객체의 _키_ 목록을 반환하는 반면, `for..of`는 반복되는 객체의 숫자 속성의 _값_ 목록을 반환합니다.

다음은 이 구별을 보여주는 예제입니다:

```ts
let list = [4, 5, 6];

for (let i in list) {
  console.log(i); // "0", "1", "2",
}

for (let i of list) {
  console.log(i); // 4, 5, 6
}
```

또 다른 구별은 `for..in`은 어떤 객체에서도 작동하며, 이 객체의 속성를 검사하는 방법 역할을 한다는 것입니다.
반면 `for..of`는 주로 이터러블 객체의 값에 관심이 있습니다. `Map`과 `Set`과 같은 내장 객체는 저장된 값에 접근할 수 있도록 `Symbol.iterator` 속성를 구현합니다.

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

### 코드 생성

#### ES5 대상

ES5 호환 엔진을 대상으로 할 때, 이터레이터는 `Array` 타입의 값에만 허용됩니다.
비Array 값이 `Symbol.iterator` 속성를 구현하더라도 비Array 값에 `for..of` 루프를 사용하는 것은 오류입니다.

컴파일러는 `for..of` 루프에 대해 간단한 `for` 루프를 생성합니다. 예를 들어:

```ts
let numbers = [1, 2, 3];
for (let num of numbers) {
  console.log(num);
}
```

는 다음과 같이 생성됩니다:

```js
var numbers = [1, 2, 3];
for (var _i = 0; _i < numbers.length; _i++) {
  var num = numbers[_i];
  console.log(num);
}
```

#### ECMAScript 2015 이상 대상

ECMAScript 2015 호환 엔진을 대상으로 할 때, 컴파일러는 엔진의 내장 이터레이터 구현을 대상으로 `for..of` 루프를 생성합니다.

# 변수 선언 (Variable Declaration)

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

`var` 선언은 다른 언어에 익숙한 사람들에게 이상한 스코핑 규칙을 가지고 있습니다.
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

블록 스코프 변수의 또 다른 프로퍼티는 실제로 선언되기 전에 읽거나 쓸 수 없다는 것입니다.
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

그들은 `let` 선언과 같지만, 이름에서 알 수 있듯이, 바인딩되면 값을 변경할 수 없습니다.
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

#### 프로퍼티 이름 바꾸기

프로퍼티에 다른 이름을 줄 수도 있습니다:

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

기본값을 사용하면 프로퍼티가 undefined인 경우 기본값을 지정할 수 있습니다:

```ts
function keepWholeObject(wholeObject: { a: string; b?: number }) {
  let { a, b = 1001 } = wholeObject;
}
```

이 예제에서 `b?`는 `b`가 선택적이므로 `undefined`일 수 있음을 나타냅니다.
`keepWholeObject`는 이제 `b`가 undefined인 경우에도 `wholeObject`뿐만 아니라 프로퍼티 `a`와 `b`에 대한 변수를 가집니다.

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

그런 다음, 주 이니셜라이저가 아닌 구조 분해된 프로퍼티의 선택적 프로퍼티에 대한 기본값을 제공해야 한다는 것을 기억해야 합니다.
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
그들은 스프레드에 의해 변경되지 않습니다.

객체도 스프레드할 수 있습니다:

```ts
let defaults = { food: "spicy", price: "$$", ambiance: "noisy" };
let search = { ...defaults, food: "rich" };
```

이제 `search`는 `{ food: "rich", price: "$$", ambiance: "noisy" }`입니다.
객체 스프레딩은 배열 스프레딩보다 더 복잡합니다.
배열 스프레딩처럼 왼쪽에서 오른쪽으로 진행하지만, 결과는 여전히 객체입니다.
이것은 스프레드 객체에서 나중에 오는 프로퍼티가 이전에 오는 프로퍼티를 덮어쓴다는 것을 의미합니다.
따라서 이전 예제를 마지막에 스프레드하도록 수정하면:

```ts
let defaults = { food: "spicy", price: "$$", ambiance: "noisy" };
let search = { food: "rich", ...defaults };
```

그러면 `defaults`의 `food` 프로퍼티가 `food: "rich"`를 덮어쓰는데, 이 경우 우리가 원하는 것이 아닙니다.

객체 스프레드에는 몇 가지 다른 놀라운 제한도 있습니다.
첫째, 객체의 [자체, 열거 가능한 프로퍼티](https://developer.mozilla.org/docs/Web/JavaScript/Enumerability_and_ownership_of_properties)만 포함합니다.
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

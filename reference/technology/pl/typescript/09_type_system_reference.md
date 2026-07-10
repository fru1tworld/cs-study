# 타입 시스템 레퍼런스: 추론 · 호환성 · 유틸리티 타입 · 선언 병합 · 트리플 슬래시

# 타입 추론 (Type Inference)

> **원문:** https://www.typescriptlang.org/docs/handbook/type-inference.html

TypeScript에서 명시적인 타입 주석이 없을 때 타입 정보를 제공하기 위해 타입 추론이 사용되는 여러 곳이 있습니다. 예를 들어, 이 코드에서

```ts
let x = 3;
//  ^? let x: number
```

`x` 변수의 타입은 `number`로 추론됩니다.
이러한 종류의 추론은 변수와 멤버를 초기화하고, 매개변수 기본값을 설정하고, 함수 반환 타입을 결정할 때 발생합니다.

대부분의 경우, 타입 추론은 간단합니다.
다음 섹션에서는 타입이 추론되는 방식의 일부 뉘앙스를 살펴보겠습니다.

## 최적 공통 타입

여러 표현식에서 타입 추론이 이루어질 때, 해당 표현식의 타입을 사용하여 "최적 공통 타입"을 계산합니다. 예를 들어,

```ts
let x = [0, 1, null];
//  ^? let x: (number | null)[]
```

위 예제에서 `x`의 타입을 추론하려면, 각 배열 요소의 타입을 고려해야 합니다.
여기서 배열 타입에 대해 두 가지 선택이 주어집니다: `number`와 `null`.
최적 공통 타입 알고리즘은 각 후보 타입을 고려하고, 다른 모든 후보와 호환되는 타입을 선택합니다.

최적 공통 타입은 제공된 후보 타입에서 선택해야 하므로, 타입이 공통 구조를 공유하지만 모든 후보 타입의 슈퍼 타입인 타입이 없는 경우가 있습니다. 예를 들어:

```ts
// @strict: false
class Animal {}
class Rhino extends Animal {
  hasHorn: true;
}
class Elephant extends Animal {
  hasTrunk: true;
}
class Snake extends Animal {
  hasLegs: false;
}
// ---cut---
let zoo = [new Rhino(), new Elephant(), new Snake()];
//    ^? let zoo: (Rhino | Elephant | Snake)[]
```

이상적으로, `zoo`가 `Animal[]`로 추론되기를 원할 수 있지만, 배열에 `Animal` 타입의 객체가 엄격하게 없기 때문에, 배열 요소 타입에 대한 추론을 하지 않습니다.
이것을 수정하려면, 모든 다른 후보의 슈퍼 타입인 타입이 없을 때 명시적으로 타입을 제공하세요:

```ts
// @strict: false
class Animal {}
class Rhino extends Animal {
  hasHorn: true;
}
class Elephant extends Animal {
  hasTrunk: true;
}
class Snake extends Animal {
  hasLegs: false;
}
// ---cut---
let zoo: Animal[] = [new Rhino(), new Elephant(), new Snake()];
//    ^? let zoo: Animal[]
```

최적 공통 타입을 찾을 수 없으면, 결과 추론은 유니온 배열 타입 `(Rhino | Elephant | Snake)[]`입니다.

## 컨텍스트적 타이핑

타입 추론은 TypeScript의 일부 경우에 "반대 방향"으로도 작동합니다.
이것을 "컨텍스트적 타이핑"이라고 합니다. 컨텍스트적 타이핑은 표현식의 타입이 해당 위치에 의해 암시될 때 발생합니다. 예를 들어:

```ts
// @errors: 2339
window.onmousedown = function (mouseEvent) {
  console.log(mouseEvent.button);
  console.log(mouseEvent.kangaroo);
};
```

여기서 TypeScript 타입 체커는 `Window.onmousedown` 함수의 타입을 사용하여 할당의 오른쪽에 있는 함수 표현식의 타입을 추론했습니다.
그렇게 하면, `button` 속성를 포함하지만 `kangaroo` 속성는 포함하지 않는 `mouseEvent` 매개변수의 [타입](https://developer.mozilla.org/docs/Web/API/MouseEvent)을 추론할 수 있었습니다.

이것은 window에 이미 타입에 `onmousedown`이 선언되어 있기 때문에 작동합니다:

```ts
// 'window'라는 전역 변수가 있음을 선언합니다
declare var window: Window & typeof globalThis;

// 이것은 (단순화되어) 다음과 같이 선언됩니다:
interface Window extends GlobalEventHandlers {
  // ...
}

// 많은 알려진 핸들러 이벤트를 정의합니다
interface GlobalEventHandlers {
  onmousedown: ((this: GlobalEventHandlers, ev: MouseEvent) => any) | null;
  // ...
}
```

TypeScript는 다른 컨텍스트에서도 타입을 추론할 만큼 똑똑합니다:

```ts
// @errors: 2339
window.onscroll = function (uiEvent) {
  console.log(uiEvent.button);
};
```

위의 함수가 `Window.onscroll`에 할당되고 있다는 사실을 기반으로, TypeScript는 `uiEvent`가 이전 예제의 [MouseEvent](https://developer.mozilla.org/docs/Web/API/MouseEvent)가 아닌 [UIEvent](https://developer.mozilla.org/docs/Web/API/UIEvent)임을 알고 있습니다. `UIEvent` 객체에는 `button` 속성가 없으므로, TypeScript가 오류를 발생시킵니다.

이 함수가 컨텍스트적으로 타입이 지정된 위치에 없었다면, 함수의 인수는 암시적으로 `any` 타입을 가지며, 오류가 발생하지 않습니다([`noImplicitAny`](/tsconfig#noImplicitAny) 옵션을 사용하지 않는 한):

```ts
// @noImplicitAny: false
const handler = function (uiEvent) {
  console.log(uiEvent.button); // <- OK
};
```

함수의 인수에 명시적으로 타입 정보를 제공하여 컨텍스트적 타입을 재정의할 수도 있습니다:

```ts
window.onscroll = function (uiEvent: any) {
  console.log(uiEvent.button); // <- 이제 오류가 없습니다
};
```

그러나 `uiEvent`에 `button`이라는 속성가 없으므로 이 코드는 `undefined`를 로깅합니다.

컨텍스트적 타이핑은 많은 경우에 적용됩니다.
일반적인 경우에는 함수 호출에 대한 인수, 할당의 오른쪽, 타입 단언, 객체 및 배열 리터럴의 멤버, 반환 문이 포함됩니다.
컨텍스트적 타입은 최적 공통 타입에서 후보 타입으로도 작동합니다. 예를 들어:

```ts
// @strict: false
class Animal {}
class Rhino extends Animal {
  hasHorn: true;
}
class Elephant extends Animal {
  hasTrunk: true;
}
class Snake extends Animal {
  hasLegs: false;
}
// ---cut---
function createZoo(): Animal[] {
  return [new Rhino(), new Elephant(), new Snake()];
}
```

이 예제에서 최적 공통 타입은 네 가지 후보 집합을 가집니다: `Animal`, `Rhino`, `Elephant`, `Snake`.
이 중에서 `Animal`이 최적 공통 타입 알고리즘에 의해 선택될 수 있습니다.

---

# 타입 호환성 (Type Compatibility)

> **원문:** https://www.typescriptlang.org/docs/handbook/type-compatibility.html

TypeScript의 타입 호환성은 구조적 서브타이핑을 기반으로 합니다.
구조적 타이핑은 멤버만을 기반으로 타입을 관련짓는 방법입니다.
이것은 명목적 타이핑과 대조됩니다.
다음 코드를 고려하세요:

```ts
interface Pet {
  name: string;
}

class Dog {
  name: string;
}

let pet: Pet;
// 구조적 타이핑 때문에 OK
pet = new Dog();
```

C#이나 Java와 같은 명목적 타입 언어에서는, `Dog` 클래스가 `Pet` 인터페이스의 구현자로 명시적으로 자신을 설명하지 않기 때문에 동등한 코드가 오류가 됩니다.

TypeScript의 구조적 타입 시스템은 JavaScript 코드가 일반적으로 작성되는 방식을 기반으로 설계되었습니다.
JavaScript가 함수 표현식과 객체 리터럴과 같은 익명 객체를 널리 사용하기 때문에, JavaScript 라이브러리에서 발견되는 관계 종류를 명목적 타입 시스템 대신 구조적 타입 시스템으로 표현하는 것이 훨씬 더 자연스럽습니다.

## 건전성에 대한 참고

TypeScript의 타입 시스템은 컴파일 시간에 안전하다고 알 수 없는 특정 연산을 허용합니다. 타입 시스템이 이러한 특성을 가지면, "건전"하지 않다고 합니다. TypeScript가 건전하지 않은 동작을 허용하는 곳은 신중하게 고려되었으며, 이 문서 전체에서 이러한 일이 발생하는 곳과 그 뒤의 동기 부여 시나리오를 설명합니다.

## 시작하기

TypeScript의 구조적 타입 시스템의 기본 규칙은 `y`가 최소한 `x`와 같은 멤버가 있으면 `x`가 `y`와 호환된다는 것입니다. 예를 들어 `name` 속성를 가진 `Pet`이라는 인터페이스가 있는 다음 코드를 고려하세요:

```ts
interface Pet {
  name: string;
}

let pet: Pet;
// dog의 추론된 타입은 { name: string; owner: string; }입니다
let dog = { name: "Lassie", owner: "Rudd Weatherwax" };
pet = dog;
```

`dog`가 `pet`에 할당될 수 있는지 확인하기 위해, 컴파일러는 `pet`의 각 속성를 확인하여 `dog`에서 해당하는 호환 가능한 속성를 찾습니다.
이 경우, `dog`는 문자열인 `name`이라는 멤버가 있어야 합니다. 있으므로 할당이 허용됩니다.

함수 호출 인수를 확인할 때도 동일한 할당 규칙이 사용됩니다:

```ts
interface Pet {
  name: string;
}

let dog = { name: "Lassie", owner: "Rudd Weatherwax" };

function greet(pet: Pet) {
  console.log("Hello, " + pet.name);
}
greet(dog); // OK
```

`dog`에 추가 `owner` 속성가 있지만, 이것이 오류를 생성하지 않는다는 점에 유의하세요.
호환성을 확인할 때는 대상 타입(`Pet`의 경우)의 멤버만 고려됩니다. 이 비교 프로세스는 각 멤버와 하위 멤버의 타입을 탐색하면서 재귀적으로 진행됩니다.

그러나 객체 리터럴은 [알려진 속성만 지정할 수 있습니다](/docs/handbook/2/objects.html#excess-property-checks).
예를 들어, `dog`의 타입이 명시적으로 `Pet`으로 지정되었기 때문에 다음 코드는 유효하지 않습니다:

```ts
let dog: Pet = { name: "Lassie", owner: "Rudd Weatherwax" }; // 오류
```

## 두 함수 비교

원시 타입과 객체 타입을 비교하는 것은 비교적 간단하지만, 어떤 종류의 함수가 호환 가능한 것으로 간주되어야 하는지에 대한 질문은 조금 더 복잡합니다.
매개변수 목록만 다른 두 함수의 기본 예제부터 시작하겠습니다:

```ts
let x = (a: number) => 0;
let y = (b: number, s: string) => 0;

y = x; // OK
x = y; // 오류
```

`x`가 `y`에 할당 가능한지 확인하기 위해, 먼저 매개변수 목록을 봅니다.
`x`의 각 매개변수는 호환 가능한 타입의 해당 매개변수가 `y`에 있어야 합니다.
매개변수의 이름은 고려되지 않으며, 타입만 고려됩니다.
이 경우, `x`의 모든 매개변수에 해당하는 호환 가능한 매개변수가 `y`에 있으므로 할당이 허용됩니다.

두 번째 할당은 `y`에 `x`에 없는 필수 두 번째 매개변수가 있으므로 오류입니다. 따라서 할당이 허용되지 않습니다.

예제 `y = x`처럼 매개변수를 '버리는' 것이 왜 허용되는지 궁금할 수 있습니다.
이 할당이 허용되는 이유는 JavaScript에서 추가 함수 매개변수를 무시하는 것이 실제로 매우 일반적이기 때문입니다.
예를 들어, `Array#forEach`는 콜백 함수에 배열 요소, 인덱스, 포함하는 배열의 세 가지 매개변수를 제공합니다.
그럼에도 불구하고, 첫 번째 매개변수만 사용하는 콜백을 제공하는 것은 매우 유용합니다:

```ts
let items = [1, 2, 3];

// 이러한 추가 매개변수를 강제하지 마세요
items.forEach((item, index, array) => console.log(item));

// 괜찮아야 합니다!
items.forEach((item) => console.log(item));
```

이제 반환 타입만 다른 두 함수를 사용하여 반환 타입이 어떻게 처리되는지 살펴봅시다:

```ts
let x = () => ({ name: "Alice" });
let y = () => ({ name: "Alice", location: "Seattle" });

x = y; // OK
y = x; // 오류, x()에 location 속성가 없습니다
```

타입 시스템은 소스 함수의 반환 타입이 대상 타입의 반환 타입의 하위 타입이어야 함을 강제합니다.

### 함수 매개변수 이변성

함수 매개변수의 타입을 비교할 때, 소스 매개변수가 대상 매개변수에 할당 가능하거나 그 반대인 경우 할당이 성공합니다.
이것은 건전하지 않습니다. 왜냐하면 호출자가 더 특수화된 타입을 취하는 함수가 주어지지만, 덜 특수화된 타입으로 함수를 호출할 수 있기 때문입니다.
실제로 이러한 종류의 오류는 드물며, 이를 허용하면 많은 일반적인 JavaScript 패턴이 가능해집니다. 간단한 예:

```ts
enum EventType {
  Mouse,
  Keyboard,
}

interface Event {
  timestamp: number;
}
interface MyMouseEvent extends Event {
  x: number;
  y: number;
}
interface MyKeyEvent extends Event {
  keyCode: number;
}

function listenEvent(eventType: EventType, handler: (n: Event) => void) {
  /* ... */
}

// 건전하지 않지만, 유용하고 일반적
listenEvent(EventType.Mouse, (e: MyMouseEvent) => console.log(e.x + "," + e.y));

// 건전성이 있을 때 바람직하지 않은 대안
listenEvent(EventType.Mouse, (e: Event) =>
  console.log((e as MyMouseEvent).x + "," + (e as MyMouseEvent).y)
);
listenEvent(EventType.Mouse, ((e: MyMouseEvent) =>
  console.log(e.x + "," + e.y)) as (e: Event) => void);

// 여전히 허용되지 않음 (명확한 오류). 완전히 호환되지 않는 타입에 대해 타입 안전성 강제
listenEvent(EventType.Mouse, (e: number) => console.log(e));
```

컴파일러 플래그 [`strictFunctionTypes`](/tsconfig#strictFunctionTypes)를 통해 이런 일이 발생할 때 TypeScript가 오류를 발생시키도록 할 수 있습니다.

### 선택적 매개변수와 나머지 매개변수

호환성을 위해 함수를 비교할 때, 선택적 매개변수와 필수 매개변수는 상호 교환 가능합니다.
소스 타입의 추가 선택적 매개변수는 오류가 아니며, 소스 타입에 해당 매개변수가 없는 대상 타입의 선택적 매개변수도 오류가 아닙니다.

함수에 나머지 매개변수가 있으면, 마치 무한한 일련의 선택적 매개변수인 것처럼 처리됩니다.

이것은 타입 시스템 관점에서 건전하지 않지만, 런타임 관점에서 선택적 매개변수의 아이디어는 일반적으로 잘 적용되지 않습니다. 해당 위치에 `undefined`를 전달하는 것이 대부분의 함수에서 동등하기 때문입니다.

동기 부여 예제는 콜백을 받고 프로그래머에게는 예측 가능하지만 타입 시스템에는 알려지지 않은 일부 인수로 호출하는 함수의 일반적인 패턴입니다:

```ts
function invokeLater(args: any[], callback: (...args: any[]) => void) {
  /* ... 'args'로 callback 호출 ... */
}

// 건전하지 않음 - invokeLater가 임의의 수의 인수를 제공"할 수 있음"
invokeLater([1, 2], (x, y) => console.log(x + ", " + y));

// 혼란스러움 (x와 y는 실제로 필수임) 그리고 발견하기 어려움
invokeLater([1, 2], (x?, y?) => console.log(x + ", " + y));
```

### 오버로드가 있는 함수

함수에 오버로드가 있으면, 대상 타입의 각 오버로드는 소스 타입의 호환 가능한 시그니처와 매치되어야 합니다.
이것은 소스 함수가 대상 함수와 동일한 모든 경우에 호출될 수 있음을 보장합니다.

## 열거형

열거형은 숫자와 호환되고, 숫자는 열거형과 호환됩니다. 다른 열거형 타입의 열거형 값은 호환되지 않는 것으로 간주됩니다. 예를 들어,

```ts
enum Status {
  Ready,
  Waiting,
}
enum Color {
  Red,
  Blue,
  Green,
}

let status = Status.Ready;
status = Color.Green; // 오류
```

## 클래스

클래스는 한 가지 예외를 제외하고 객체 리터럴 타입 및 인터페이스와 유사하게 작동합니다: 정적 타입과 인스턴스 타입이 모두 있습니다.
클래스 타입의 두 객체를 비교할 때, 인스턴스의 멤버만 비교됩니다.
정적 멤버와 생성자는 호환성에 영향을 미치지 않습니다.

```ts
class Animal {
  feet: number;
  constructor(name: string, numFeet: number) {}
}

class Size {
  feet: number;
  constructor(numFeet: number) {}
}

let a: Animal;
let s: Size;

a = s; // OK
s = a; // OK
```

### 클래스의 private 및 protected 멤버

클래스의 private 및 protected 멤버는 호환성에 영향을 미칩니다.
클래스의 인스턴스가 호환성을 확인할 때, 대상 타입에 private 멤버가 포함된 경우, 소스 타입에도 동일한 클래스에서 유래한 private 멤버가 포함되어야 합니다.
마찬가지로, protected 멤버가 있는 인스턴스에도 동일하게 적용됩니다.
이것은 클래스가 슈퍼 클래스와 할당 호환되게 하지만, 그렇지 않으면 동일한 형태를 가진 다른 상속 계층의 클래스와는 호환되지 _않습니다_.

## 제네릭

TypeScript는 구조적 타입 시스템이므로, 타입 매개변수는 멤버 타입의 일부로 소비될 때만 결과 타입에 영향을 미칩니다. 예를 들어,

```ts
interface Empty<T> {}
let x: Empty<number>;
let y: Empty<string>;

x = y; // OK, y가 x의 구조와 일치하기 때문
```

위에서 `x`와 `y`는 구조가 타입 인수를 차별화하는 방식으로 사용하지 않기 때문에 호환됩니다.
`Empty<T>`에 멤버를 추가하여 이 예제를 변경하면 이것이 어떻게 작동하는지 보여줍니다:

```ts
interface NotEmpty<T> {
  data: T;
}
let x: NotEmpty<number>;
let y: NotEmpty<string>;

x = y; // 오류, x와 y가 호환되지 않기 때문
```

이 방식으로, 타입 인수가 지정된 제네릭 타입은 비제네릭 타입처럼 동작합니다.

타입 인수가 지정되지 않은 제네릭 타입의 경우, 지정되지 않은 모든 타입 인수 대신 `any`를 지정하여 호환성을 확인합니다.
그런 다음 결과 타입이 비제네릭 경우와 마찬가지로 호환성을 확인합니다.

예를 들어,

```ts
let identity = function <T>(x: T): T {
  // ...
};

let reverse = function <U>(y: U): U {
  // ...
};

identity = reverse; // OK, (x: any) => any가 (y: any) => any와 일치하기 때문
```

## 고급 주제

### 서브타입 vs 할당

지금까지 "호환"을 사용했는데, 이것은 언어 명세에 정의된 용어가 아닙니다.
TypeScript에는 두 종류의 호환성이 있습니다: 서브타입과 할당.
이들은 `any`와의 상호 할당, 그리고 같은 숫자 값을 가진 `enum` 간의 할당을 허용하는 규칙만큼만 서브타입 호환성을 확장한다는 점에서 다릅니다.

언어의 다른 위치에서는 상황에 따라 두 호환성 메커니즘 중 하나를 사용합니다.
실용적인 목적으로, 타입 호환성은 `implements` 및 `extends` 절의 경우에도 할당 호환성에 의해 지시됩니다.

## `any`, `unknown`, `object`, `void`, `undefined`, `null`, `never` 할당 가능성

다음 표는 일부 추상 타입 간의 할당 가능성을 요약합니다.
행은 각각이 무엇에 할당 가능한지를 나타내고, 열은 무엇이 할당 가능한지를 나타냅니다.
"<span class='black-tick'>&#10003;</span>"는 [`strictNullChecks`](/tsconfig#strictNullChecks)가 꺼져 있을 때만 호환되는 조합을 나타냅니다.

|              |  any  | unknown | object |  void   | undefined |  null   | never |
| ------------ | :---: | :-----: | :----: | :-----: | :-------: | :-----: | :---: |
| any ->       |       |   &#10003;    |  &#10003;   |   &#10003;    |    &#10003;     |   &#10003;    |  &#10007;  |
| unknown ->   |  &#10003;   |         |   &#10007;   |   &#10007;    |     &#10007;     |    &#10007;    |  &#10007;  |
| object ->    |  &#10003;   |   &#10003;    |        |   &#10007;    |     &#10007;     |    &#10007;    |  &#10007;  |
| void ->      |  &#10003;   |   &#10003;    |   &#10007;   |         |     &#10007;     |    &#10007;    |  &#10007;  |
| undefined -> |  &#10003;   |   &#10003;    |   &#10003;*   |   &#10003;    |           |    &#10003;*    |  &#10007;  |
| null ->      |  &#10003;   |   &#10003;    |   &#10003;*   |   &#10003;*    |    &#10003;*     |         |  &#10007;  |
| never ->     |  &#10003;   |   &#10003;    |  &#10003;   |   &#10003;    |    &#10003;     |   &#10003;    |       |

*: [`strictNullChecks`](/tsconfig#strictNullChecks)가 꺼져 있을 때만

[기초](/docs/handbook/2/basic-types.html)를 반복하면:

- 모든 것은 자기 자신에게 할당 가능합니다.
- `any`와 `unknown`은 무엇이 할당 가능한지에 관해서는 동일하지만, `unknown`은 `any`를 제외한 어떤 것에도 할당 가능하지 않습니다.
- `unknown`과 `never`는 서로의 역과 같습니다.
  모든 것이 `unknown`에 할당 가능하고, `never`는 모든 것에 할당 가능합니다.
  어떤 것도 `never`에 할당 가능하지 않고, `unknown`은 (`any`를 제외하고) 어떤 것에도 할당 가능하지 않습니다.
- `void`는 다음 예외를 제외하고 무엇에도 할당 가능하지 않고 무엇으로부터도 할당 가능하지 않습니다: `any`, `unknown`, `never`, `undefined`, 그리고 `null` ([`strictNullChecks`](/tsconfig#strictNullChecks)가 꺼져 있으면, 자세한 내용은 표 참조).
- [`strictNullChecks`](/tsconfig#strictNullChecks)가 꺼져 있으면, `null`과 `undefined`는 `never`와 유사합니다: 대부분의 타입에 할당 가능하지만, 대부분의 타입은 `null`과 `undefined`에 할당할 수 없습니다.
  `null`과 `undefined`는 서로 할당 가능합니다.
- [`strictNullChecks`](/tsconfig#strictNullChecks)가 켜져 있으면, `null`과 `undefined`는 `void`처럼 더 동작합니다: `any`, `unknown`, `void`를 제외하고 어떤 것에도 할당 가능하지 않고 어떤 것으로부터도 할당 가능하지 않습니다 (`undefined`는 항상 `void`에 할당 가능).

---

# 유틸리티 타입 (Utility Types)

> **원문:** https://www.typescriptlang.org/docs/handbook/utility-types.html

TypeScript는 일반적인 타입 변환을 용이하게 하기 위한 여러 유틸리티 타입을 제공합니다. 이러한 유틸리티들은 전역적으로 사용할 수 있습니다.

## `Awaited<Type>`

> 릴리스: [4.5](/docs/handbook/release-notes/typescript-4-5.html#the-awaited-type-and-promise-improvements)

이 타입은 `async` 함수의 `await`나 `Promise`의 `.then()` 메서드와 같은 연산을 모델링하기 위한 것입니다 - 특히, `Promise`를 재귀적으로 언래핑하는 방식을 다룹니다.

##### 예제

```ts
type A = Awaited<Promise<string>>;
//   ^? type A = string

type B = Awaited<Promise<Promise<number>>>;
//   ^? type B = number

type C = Awaited<boolean | Promise<number>>;
//   ^? type C = number | boolean
```

## `Partial<Type>`

> 릴리스: [2.1](/docs/handbook/release-notes/typescript-2-1.html#partial-readonly-record-and-pick)

`Type`의 모든 속성를 선택적으로 설정한 타입을 생성합니다. 이 유틸리티는 주어진 타입의 모든 하위 집합을 나타내는 타입을 반환합니다.

##### 예제

```ts
interface Todo {
  title: string;
  description: string;
}

function updateTodo(todo: Todo, fieldsToUpdate: Partial<Todo>) {
  return { ...todo, ...fieldsToUpdate };
}

const todo1 = {
  title: "organize desk",
  description: "clear clutter",
};

const todo2 = updateTodo(todo1, {
  description: "throw out trash",
});
```

## `Required<Type>`

> 릴리스: [2.8](/docs/handbook/release-notes/typescript-2-8.html#improved-control-over-mapped-type-modifiers)

`Type`의 모든 속성를 필수로 설정한 타입을 생성합니다. [`Partial`](#partialtype)의 반대입니다.

##### 예제

```ts
// @errors: 2741
interface Props {
  a?: number;
  b?: string;
}

const obj: Props = { a: 5 };

const obj2: Required<Props> = { a: 5 };
```

## `Readonly<Type>`

> 릴리스: [2.1](/docs/handbook/release-notes/typescript-2-1.html#partial-readonly-record-and-pick)

`Type`의 모든 속성를 `readonly`로 설정한 타입을 생성합니다. 이는 생성된 타입의 속성를 재할당할 수 없음을 의미합니다.

##### 예제

```ts
// @errors: 2540
interface Todo {
  title: string;
}

const todo: Readonly<Todo> = {
  title: "Delete inactive users",
};

todo.title = "Hello";
```

이 유틸리티는 런타임에 실패할 할당 표현식을 표현하는 데 유용합니다 (예: [동결된 객체](https://developer.mozilla.org/docs/Web/JavaScript/Reference/Global_Objects/Object/freeze)의 속성를 재할당하려고 할 때).

##### `Object.freeze`

```ts
function freeze<Type>(obj: Type): Readonly<Type>;
```

## `Record<Keys, Type>`

> 릴리스: [2.1](/docs/handbook/release-notes/typescript-2-1.html#partial-readonly-record-and-pick)

속성 키가 `Keys`이고 속성 값이 `Type`인 객체 타입을 생성합니다. 이 유틸리티는 타입의 속성를 다른 타입으로 매핑하는 데 사용할 수 있습니다.

##### 예제

```ts
type CatName = "miffy" | "boris" | "mordred";

interface CatInfo {
  age: number;
  breed: string;
}

const cats: Record<CatName, CatInfo> = {
  miffy: { age: 10, breed: "Persian" },
  boris: { age: 5, breed: "Maine Coon" },
  mordred: { age: 16, breed: "British Shorthair" },
};

cats.boris;
// ^? const cats.boris: CatInfo
```

## `Pick<Type, Keys>`

> 릴리스: [2.1](/docs/handbook/release-notes/typescript-2-1.html#partial-readonly-record-and-pick)

`Type`에서 속성 `Keys`의 집합(문자열 리터럴 또는 문자열 리터럴의 유니온)을 선택하여 타입을 생성합니다.

##### 예제

```ts
interface Todo {
  title: string;
  description: string;
  completed: boolean;
}

type TodoPreview = Pick<Todo, "title" | "completed">;

const todo: TodoPreview = {
  title: "Clean room",
  completed: false,
};

todo;
// ^? const todo: TodoPreview
```

## `Omit<Type, Keys>`

> 릴리스: [3.5](/docs/handbook/release-notes/typescript-3-5.html#the-omit-helper-type)

`Type`에서 모든 속성를 선택한 다음 `Keys`(문자열 리터럴 또는 문자열 리터럴의 유니온)를 제거하여 타입을 생성합니다. [`Pick`](#picktype-keys)의 반대입니다.

##### 예제

```ts
interface Todo {
  title: string;
  description: string;
  completed: boolean;
  createdAt: number;
}

type TodoPreview = Omit<Todo, "description">;

const todo: TodoPreview = {
  title: "Clean room",
  completed: false,
  createdAt: 1615544252770,
};

todo;
// ^? const todo: TodoPreview

type TodoInfo = Omit<Todo, "completed" | "createdAt">;

const todoInfo: TodoInfo = {
  title: "Pick up kids",
  description: "Kindergarten closes at 5pm",
};

todoInfo;
// ^? const todoInfo: TodoInfo
```

## `Exclude<UnionType, ExcludedMembers>`

> 릴리스: [2.8](/docs/handbook/release-notes/typescript-2-8.html#predefined-conditional-types)

`UnionType`에서 `ExcludedMembers`에 할당 가능한 모든 유니온 멤버를 제외하여 타입을 생성합니다.

##### 예제

```ts
type T0 = Exclude<"a" | "b" | "c", "a">;
//    ^? type T0 = "b" | "c"
type T1 = Exclude<"a" | "b" | "c", "a" | "b">;
//    ^? type T1 = "c"
type T2 = Exclude<string | number | (() => void), Function>;
//    ^? type T2 = string | number

type Shape =
  | { kind: "circle"; radius: number }
  | { kind: "square"; x: number }
  | { kind: "triangle"; x: number; y: number };

type T3 = Exclude<Shape, { kind: "circle" }>
//    ^? type T3 = { kind: "square"; x: number } | { kind: "triangle"; x: number; y: number }
```

## `Extract<Type, Union>`

> 릴리스: [2.8](/docs/handbook/release-notes/typescript-2-8.html#predefined-conditional-types)

`Type`에서 `Union`에 할당 가능한 모든 유니온 멤버를 추출하여 타입을 생성합니다.

##### 예제

```ts
type T0 = Extract<"a" | "b" | "c", "a" | "f">;
//    ^? type T0 = "a"
type T1 = Extract<string | number | (() => void), Function>;
//    ^? type T1 = () => void

type Shape =
  | { kind: "circle"; radius: number }
  | { kind: "square"; x: number }
  | { kind: "triangle"; x: number; y: number };

type T2 = Extract<Shape, { kind: "circle" }>
//    ^? type T2 = { kind: "circle"; radius: number }
```

## `NonNullable<Type>`

> 릴리스: [2.8](/docs/handbook/release-notes/typescript-2-8.html#predefined-conditional-types)

`Type`에서 `null`과 `undefined`를 제외하여 타입을 생성합니다.

##### 예제

```ts
type T0 = NonNullable<string | number | undefined>;
//    ^? type T0 = string | number
type T1 = NonNullable<string[] | null | undefined>;
//    ^? type T1 = string[]
```

## `Parameters<Type>`

> 릴리스: [3.1](https://github.com/microsoft/TypeScript/pull/26243)

함수 타입 `Type`의 매개변수에 사용된 타입들로 튜플 타입을 생성합니다.

오버로드된 함수의 경우, 이것은 _마지막_ 시그니처의 매개변수가 됩니다; [조건부 타입 내에서 추론하기](/docs/handbook/2/conditional-types.html#inferring-within-conditional-types)를 참조하세요.

##### 예제

```ts
// @errors: 2344
declare function f1(arg: { a: number; b: string }): void;

type T0 = Parameters<() => string>;
//    ^? type T0 = []
type T1 = Parameters<(s: string) => void>;
//    ^? type T1 = [s: string]
type T2 = Parameters<<T>(arg: T) => T>;
//    ^? type T2 = [arg: unknown]
type T3 = Parameters<typeof f1>;
//    ^? type T3 = [arg: { a: number; b: string }]
type T4 = Parameters<any>;
//    ^? type T4 = unknown[]
type T5 = Parameters<never>;
//    ^? type T5 = never
type T6 = Parameters<string>;
//    ^? 오류
type T7 = Parameters<Function>;
//    ^? 오류
```

## `ConstructorParameters<Type>`

> 릴리스: [3.1](https://github.com/microsoft/TypeScript/pull/26243)

생성자 함수 타입의 타입들로 튜플 또는 배열 타입을 생성합니다. 모든 매개변수 타입을 가진 튜플 타입을 생성합니다 (또는 `Type`이 함수가 아닌 경우 `never` 타입).

##### 예제

```ts
// @errors: 2344
// @strict: false
type T0 = ConstructorParameters<ErrorConstructor>;
//    ^? type T0 = [message?: string]
type T1 = ConstructorParameters<FunctionConstructor>;
//    ^? type T1 = string[]
type T2 = ConstructorParameters<RegExpConstructor>;
//    ^? type T2 = [pattern: string | RegExp, flags?: string]
class C {
  constructor(a: number, b: string) {}
}
type T3 = ConstructorParameters<typeof C>;
//    ^? type T3 = [a: number, b: string]
type T4 = ConstructorParameters<any>;
//    ^? type T4 = unknown[]

type T5 = ConstructorParameters<Function>;
//    ^? 오류
```

## `ReturnType<Type>`

> 릴리스: [2.8](/docs/handbook/release-notes/typescript-2-8.html#predefined-conditional-types)

함수 `Type`의 반환 타입으로 구성된 타입을 생성합니다.

오버로드된 함수의 경우, 이것은 _마지막_ 시그니처의 반환 타입이 됩니다; [조건부 타입 내에서 추론하기](/docs/handbook/2/conditional-types.html#inferring-within-conditional-types)를 참조하세요.

##### 예제

```ts
// @errors: 2344 2344
declare function f1(): { a: number; b: string };

type T0 = ReturnType<() => string>;
//    ^? type T0 = string
type T1 = ReturnType<(s: string) => void>;
//    ^? type T1 = void
type T2 = ReturnType<<T>() => T>;
//    ^? type T2 = unknown
type T3 = ReturnType<<T extends U, U extends number[]>() => T>;
//    ^? type T3 = number[]
type T4 = ReturnType<typeof f1>;
//    ^? type T4 = { a: number; b: string }
type T5 = ReturnType<any>;
//    ^? type T5 = any
type T6 = ReturnType<never>;
//    ^? type T6 = never
type T7 = ReturnType<string>;
//    ^? 오류
type T8 = ReturnType<Function>;
//    ^? 오류
```

## `InstanceType<Type>`

> 릴리스: [2.8](/docs/handbook/release-notes/typescript-2-8.html#predefined-conditional-types)

`Type`에서 생성자 함수의 인스턴스 타입으로 구성된 타입을 생성합니다.

##### 예제

```ts
// @errors: 2344 2344
// @strict: false
class C {
  x = 0;
  y = 0;
}

type T0 = InstanceType<typeof C>;
//    ^? type T0 = C
type T1 = InstanceType<any>;
//    ^? type T1 = any
type T2 = InstanceType<never>;
//    ^? type T2 = never
type T3 = InstanceType<string>;
//    ^? 오류
type T4 = InstanceType<Function>;
//    ^? 오류
```

## `NoInfer<Type>`

> 릴리스: [5.4](/docs/handbook/release-notes/typescript-5-4.html#the-noinfer-utility-type)

포함된 타입에 대한 추론을 차단합니다. 추론을 차단하는 것 외에, `NoInfer<Type>`은 `Type`과 동일합니다.

##### 예제

```ts
function createStreetLight<C extends string>(
  colors: C[],
  defaultColor?: NoInfer<C>,
) {
  // ...
}

createStreetLight(["red", "yellow", "green"], "red");  // OK
createStreetLight(["red", "yellow", "green"], "blue");  // 오류
```

## `ThisParameterType<Type>`

> 릴리스: [3.3](https://github.com/microsoft/TypeScript/pull/28920)

함수 타입에 대한 [this](/docs/handbook/functions.html#this-parameters) 매개변수의 타입을 추출하거나, 함수 타입에 `this` 매개변수가 없는 경우 [unknown](/docs/handbook/release-notes/typescript-3-0.html#new-unknown-top-type)을 반환합니다.

##### 예제

```ts
function toHex(this: Number) {
  return this.toString(16);
}

function numberToString(n: ThisParameterType<typeof toHex>) {
  return toHex.apply(n);
}
```

## `OmitThisParameter<Type>`

> 릴리스: [3.3](https://github.com/microsoft/TypeScript/pull/28920)

`Type`에서 [`this`](/docs/handbook/functions.html#this-parameters) 매개변수를 제거합니다. `Type`에 명시적으로 선언된 `this` 매개변수가 없는 경우, 결과는 단순히 `Type`입니다. 그렇지 않으면, `Type`에서 `this` 매개변수가 없는 새로운 함수 타입이 생성됩니다. 제네릭은 지워지고 마지막 오버로드 시그니처만 새로운 함수 타입으로 전파됩니다.

##### 예제

```ts
function toHex(this: Number) {
  return this.toString(16);
}

const fiveToHex: OmitThisParameter<typeof toHex> = toHex.bind(5);

console.log(fiveToHex());
```

## `ThisType<Type>`

> 릴리스: [2.3](https://github.com/microsoft/TypeScript/pull/14141)

이 유틸리티는 변환된 타입을 반환하지 않습니다. 대신, 컨텍스트적 [`this`](/docs/handbook/functions.html#this) 타입의 마커 역할을 합니다. 이 유틸리티를 사용하려면 [`noImplicitThis`](/tsconfig#noImplicitThis) 플래그가 활성화되어야 합니다.

##### 예제

```ts
// @noImplicitThis: true
type ObjectDescriptor<D, M> = {
  data?: D;
  methods?: M & ThisType<D & M>; // methods에서 'this'의 타입은 D & M입니다
};

function makeObject<D, M>(desc: ObjectDescriptor<D, M>): D & M {
  let data: object = desc.data || {};
  let methods: object = desc.methods || {};
  return { ...data, ...methods } as D & M;
}

let obj = makeObject({
  data: { x: 0, y: 0 },
  methods: {
    moveBy(dx: number, dy: number) {
      this.x += dx; // 강력하게 타입이 지정된 this
      this.y += dy; // 강력하게 타입이 지정된 this
    },
  },
});

obj.x = 10;
obj.y = 20;
obj.moveBy(5, 5);
```

위의 예제에서, `makeObject`에 대한 인수의 `methods` 객체는 `ThisType<D & M>`을 포함하는 컨텍스트적 타입을 가지므로, `methods` 객체 내의 메서드에서 [this](/docs/handbook/functions.html#this)의 타입은 `{ x: number, y: number } & { moveBy(dx: number, dy: number): void }`입니다. `methods` 속성의 타입이 동시에 추론 대상이자 메서드 내의 `this` 타입의 소스임을 주목하세요.

`ThisType<T>` 마커 인터페이스는 단순히 `lib.d.ts`에 선언된 빈 인터페이스입니다. 객체 리터럴의 컨텍스트적 타입에서 인식되는 것 외에, 이 인터페이스는 다른 빈 인터페이스처럼 동작합니다.

## 내장 문자열 조작 타입

### `Uppercase<StringType>`

### `Lowercase<StringType>`

### `Capitalize<StringType>`

### `Uncapitalize<StringType>`

템플릿 문자열 리터럴을 둘러싼 문자열 조작을 돕기 위해, TypeScript는 타입 시스템 내에서 문자열 조작에 사용할 수 있는 타입 집합을 포함합니다. 이러한 타입은 [템플릿 리터럴 타입](/docs/handbook/2/template-literal-types.html#uppercasestringtype) 문서에서 찾을 수 있습니다.

---

# 선언 병합 (Declaration Merging)

> **원문:** https://www.typescriptlang.org/docs/handbook/declaration-merging.html

## 소개

TypeScript의 독특한 개념 중 일부는 타입 수준에서 JavaScript 객체의 형태를 설명합니다.
TypeScript에 특히 고유한 예는 '선언 병합' 개념입니다.
이 개념을 이해하면 기존 JavaScript로 작업할 때 이점을 얻을 수 있습니다.
또한 더 고급 추상화 개념으로의 문을 열어줍니다.

이 글의 목적상, "선언 병합"은 컴파일러가 동일한 이름으로 선언된 두 개의 별도 선언을 단일 정의로 병합한다는 것을 의미합니다.
이 병합된 정의는 원래 두 선언의 기능이 모두 있습니다.
병합할 수 있는 선언의 수에는 제한이 없습니다; 두 선언에만 국한되지 않습니다.

## 기본 개념

TypeScript에서 선언은 네임스페이스, 타입 또는 값의 세 그룹 중 하나 이상에 엔티티를 생성합니다.
네임스페이스 생성 선언은 점 표기법을 사용하여 접근하는 이름을 포함하는 네임스페이스를 생성합니다.
타입 생성 선언은 바로 그것을 수행합니다: 선언된 형태로 표시되고 주어진 이름에 바인딩된 타입을 생성합니다.
마지막으로, 값 생성 선언은 출력 JavaScript에서 볼 수 있는 값을 생성합니다.

| 선언 타입      | 네임스페이스 | 타입 |  값  |
| -------------- | :----------: | :--: | :--: |
| Namespace      |      X       |      |  X   |
| Class          |              |  X   |  X   |
| Enum           |              |  X   |  X   |
| Interface      |              |  X   |      |
| Type Alias     |              |  X   |      |
| Function       |              |      |  X   |
| Variable       |              |      |  X   |

각 선언으로 무엇이 생성되는지 이해하면 선언 병합을 수행할 때 무엇이 병합되는지 이해하는 데 도움이 됩니다.

## 인터페이스 병합

가장 단순하고 아마도 가장 일반적인 선언 병합 유형은 인터페이스 병합입니다.
가장 기본적인 수준에서 병합은 기계적으로 두 선언의 멤버를 동일한 이름의 단일 인터페이스로 결합합니다.

```ts
interface Box {
  height: number;
  width: number;
}

interface Box {
  scale: number;
}

let box: Box = { height: 5, width: 6, scale: 10 };
```

인터페이스의 비함수 멤버는 고유해야 합니다.
고유하지 않은 경우 동일한 타입이어야 합니다.
인터페이스가 동일한 이름의 비함수 멤버를 선언하지만 다른 타입인 경우 컴파일러가 오류를 발생시킵니다.

함수 멤버의 경우 동일한 이름의 각 함수 멤버는 동일한 함수의 오버로드를 설명하는 것으로 처리됩니다.
또한 인터페이스 `A`가 나중의 인터페이스 `A`와 병합되는 경우, 두 번째 인터페이스가 첫 번째 인터페이스보다 더 높은 우선순위를 갖는다는 점도 주목할 만합니다.

즉, 예제에서:

```ts
interface Cloner {
  clone(animal: Animal): Animal;
}

interface Cloner {
  clone(animal: Sheep): Sheep;
}

interface Cloner {
  clone(animal: Dog): Dog;
  clone(animal: Cat): Cat;
}
```

세 인터페이스가 병합되어 다음과 같은 단일 선언이 생성됩니다:

```ts
interface Cloner {
  clone(animal: Dog): Dog;
  clone(animal: Cat): Cat;
  clone(animal: Sheep): Sheep;
  clone(animal: Animal): Animal;
}
```

각 그룹의 요소가 동일한 순서를 유지하지만, 그룹 자체는 나중의 오버로드 세트가 먼저 정렬되어 병합됩니다.

이 규칙의 한 가지 예외는 특수화된 시그니처입니다.
시그니처에 _단일_ 문자열 리터럴 타입(예: 문자열 리터럴의 유니온이 아닌)인 매개변수가 있는 경우, 병합된 오버로드 목록의 맨 위로 버블링됩니다.

예를 들어, 다음 인터페이스들이 함께 병합됩니다:

```ts
interface Document {
  createElement(tagName: any): Element;
}
interface Document {
  createElement(tagName: "div"): HTMLDivElement;
  createElement(tagName: "span"): HTMLSpanElement;
}
interface Document {
  createElement(tagName: string): HTMLElement;
  createElement(tagName: "canvas"): HTMLCanvasElement;
}
```

결과적으로 `Document`의 병합된 선언은 다음과 같습니다:

```ts
interface Document {
  createElement(tagName: "canvas"): HTMLCanvasElement;
  createElement(tagName: "div"): HTMLDivElement;
  createElement(tagName: "span"): HTMLSpanElement;
  createElement(tagName: string): HTMLElement;
  createElement(tagName: any): Element;
}
```

## 네임스페이스 병합

인터페이스와 마찬가지로 동일한 이름의 네임스페이스도 멤버를 병합합니다.
네임스페이스는 네임스페이스와 값을 모두 생성하므로, 둘 다 어떻게 병합되는지 이해해야 합니다.

네임스페이스를 병합하려면, 각 네임스페이스에서 선언된 내보낸 인터페이스의 타입 정의가 자체적으로 병합되어, 병합된 인터페이스 정의가 내부에 있는 단일 네임스페이스를 형성합니다.

네임스페이스 값을 병합하려면, 각 선언 사이트에서 주어진 이름의 네임스페이스가 이미 존재하는 경우, 기존 네임스페이스를 가져와서 두 번째 네임스페이스의 내보낸 멤버를 첫 번째에 추가하여 더 확장됩니다.

이 예제에서 `Animals`의 선언 병합:

```ts
namespace Animals {
  export class Zebra {}
}

namespace Animals {
  export interface Legged {
    numberOfLegs: number;
  }
  export class Dog {}
}
```

은 다음과 같습니다:

```ts
namespace Animals {
  export interface Legged {
    numberOfLegs: number;
  }

  export class Zebra {}
  export class Dog {}
}
```

이 네임스페이스 병합 모델은 유용한 출발점이지만, 내보내지 않은 멤버에 어떤 일이 발생하는지도 이해해야 합니다.
내보내지 않은 멤버는 원래의 (병합되지 않은) 네임스페이스에서만 볼 수 있습니다. 이것은 병합 후, 다른 선언에서 온 병합된 멤버가 내보내지 않은 멤버를 볼 수 없음을 의미합니다.

이 예제에서 이것을 더 명확하게 볼 수 있습니다:

```ts
namespace Animal {
  let haveMuscles = true;

  export function animalsHaveMuscles() {
    return haveMuscles;
  }
}

namespace Animal {
  export function doAnimalsHaveMuscles() {
    return haveMuscles; // 오류, haveMuscles는 여기서 접근할 수 없습니다
  }
}
```

`haveMuscles`가 내보내지지 않았기 때문에, 동일한 병합되지 않은 네임스페이스를 공유하는 `animalsHaveMuscles` 함수만 이 심볼을 볼 수 있습니다.
`doAnimalsHaveMuscles` 함수는 병합된 `Animal` 네임스페이스의 일부이지만, 이 내보내지 않은 멤버를 볼 수 없습니다.

## 네임스페이스와 클래스, 함수, 열거형 병합

네임스페이스는 다른 유형의 선언과도 병합할 수 있을 만큼 유연합니다.
그렇게 하려면, 네임스페이스 선언이 병합할 선언 뒤에 와야 합니다. 결과 선언은 두 선언 타입의 속성를 모두 가집니다.
TypeScript는 이 기능을 사용하여 JavaScript와 다른 프로그래밍 언어의 일부 패턴을 모델링합니다.

### 네임스페이스와 클래스 병합

이것은 사용자에게 내부 클래스를 설명하는 방법을 제공합니다.

```ts
class Album {
  label: Album.AlbumLabel;
}
namespace Album {
  export class AlbumLabel {}
}
```

병합된 멤버의 가시성 규칙은 [네임스페이스 병합](./declaration-merging.html#네임스페이스-병합) 섹션에 설명된 것과 동일하므로, 병합된 클래스가 볼 수 있도록 `AlbumLabel` 클래스를 내보내야 합니다.
최종 결과는 다른 클래스 내부에서 관리되는 클래스입니다.
네임스페이스를 사용하여 기존 클래스에 더 많은 정적 멤버를 추가할 수도 있습니다.

내부 클래스 패턴 외에도, 함수를 만든 다음 함수에 속성를 추가하여 함수를 더 확장하는 JavaScript 관행에 익숙할 수도 있습니다.
TypeScript는 선언 병합을 사용하여 이와 같은 정의를 타입 안전한 방식으로 구축합니다.

```ts
function buildLabel(name: string): string {
  return buildLabel.prefix + name + buildLabel.suffix;
}

namespace buildLabel {
  export let suffix = "";
  export let prefix = "Hello, ";
}

console.log(buildLabel("Sam Smith"));
```

마찬가지로, 네임스페이스는 열거형을 정적 멤버로 확장하는 데 사용할 수 있습니다:

```ts
enum Color {
  red = 1,
  green = 2,
  blue = 4,
}

namespace Color {
  export function mixColor(colorName: string) {
    if (colorName == "yellow") {
      return Color.red + Color.green;
    } else if (colorName == "white") {
      return Color.red + Color.green + Color.blue;
    } else if (colorName == "magenta") {
      return Color.red + Color.blue;
    } else if (colorName == "cyan") {
      return Color.green + Color.blue;
    }
  }
}
```

## 허용되지 않는 병합

TypeScript에서 모든 병합이 허용되는 것은 아닙니다.
현재 클래스는 다른 클래스나 변수와 병합할 수 없습니다.
클래스 병합을 모방하는 방법에 대한 정보는 [TypeScript의 믹스인](/docs/handbook/mixins.html) 섹션을 참조하세요.

## 모듈 확장

JavaScript 모듈은 병합을 지원하지 않지만, 기존 객체를 가져온 다음 업데이트하여 패치할 수 있습니다.
장난감 Observable 예제를 살펴봅시다:

```ts
// observable.ts
export class Observable<T> {
  // ... 구현은 독자를 위한 연습으로 남깁니다 ...
}

// map.ts
import { Observable } from "./observable";
Observable.prototype.map = function (f) {
  // ... 독자를 위한 또 다른 연습
};
```

이것은 TypeScript에서도 잘 작동하지만, 컴파일러는 `Observable.prototype.map`에 대해 알지 못합니다.
모듈 확장을 사용하여 컴파일러에게 이에 대해 알릴 수 있습니다:

```ts
// observable.ts
export class Observable<T> {
  // ... 구현은 독자를 위한 연습으로 남깁니다 ...
}

// map.ts
import { Observable } from "./observable";
declare module "./observable" {
  interface Observable<T> {
    map<U>(f: (x: T) => U): Observable<U>;
  }
}
Observable.prototype.map = function (f) {
  // ... 독자를 위한 또 다른 연습
};

// consumer.ts
import { Observable } from "./observable";
import "./map";
let o: Observable<number>;
o.map((x) => x.toFixed());
```

모듈 이름은 `import`/`export`의 모듈 지정자와 동일한 방식으로 해석됩니다.
자세한 내용은 [모듈](/docs/handbook/modules.html)을 참조하세요.
그런 다음 확장의 선언은 원본 파일에서 선언된 것처럼 병합됩니다.

그러나 두 가지 제한 사항을 명심해야 합니다:

1. 확장에서 새 최상위 선언을 선언할 수 없습니다 -- 기존 선언에 대한 패치만 가능합니다.
2. 기본 내보내기도 확장할 수 없습니다. 명명된 내보내기만 가능합니다(내보낸 이름으로 내보내기를 확장해야 하며, `default`는 예약어이기 때문입니다 - 자세한 내용은 [#14080](https://github.com/Microsoft/TypeScript/issues/14080)를 참조하세요)

### 전역 확장

모듈 내부에서 전역 범위에 선언을 추가할 수도 있습니다:

```ts
// observable.ts
export class Observable<T> {
  // ... 아직 구현이 없습니다 ...
}

declare global {
  interface Array<T> {
    toObservable(): Observable<T>;
  }
}

Array.prototype.toObservable = function () {
  // ...
};
```

전역 확장은 모듈 확장과 동일한 동작 및 제한을 가집니다.

---

# 삼중 슬래시 지시어 (Triple-Slash Directives)

> **원문:** https://www.typescriptlang.org/docs/handbook/triple-slash-directives.html

삼중 슬래시 지시어는 단일 XML 태그를 포함하는 한 줄 주석입니다.
주석의 내용은 컴파일러 지시어로 사용됩니다.

삼중 슬래시 지시어는 포함하는 파일의 맨 위에서**만** 유효합니다.
삼중 슬래시 지시어는 다른 삼중 슬래시 지시어를 포함한 한 줄 또는 여러 줄 주석만 앞에 올 수 있습니다.
문이나 선언 뒤에 나오면 일반적인 한 줄 주석으로 처리되며, 특별한 의미를 갖지 않습니다.

TypeScript 5.5부터, 컴파일러는 참조 지시어를 생성하지 않으며, 해당 지시어가 [`preserve="true"`](#preservetrue)로 표시되지 않는 한 수동으로 작성된 삼중 슬래시 지시어를 출력 파일에 방출하지 _않습니다_.

## `/// <reference path="..." />`

`/// <reference path="..." />` 지시어가 이 그룹에서 가장 일반적입니다.
파일 간의 _종속성_ 선언 역할을 합니다.

삼중 슬래시 참조는 컴파일러에게 컴파일 프로세스에 추가 파일을 포함하도록 지시합니다.

또한 [`out`](/tsconfig#out) 또는 [`outFile`](/tsconfig#outFile)을 사용할 때 출력을 정렬하는 방법 역할도 합니다.
파일은 전처리 단계 후 입력과 동일한 순서로 출력 파일 위치에 방출됩니다.

### 입력 파일 전처리

컴파일러는 모든 삼중 슬래시 참조 지시어를 해석하기 위해 입력 파일에 대한 전처리 단계를 수행합니다.
이 과정에서 추가 파일이 컴파일에 추가됩니다.

프로세스는 _루트 파일_ 집합으로 시작합니다;
이들은 커맨드 라인이나 `tsconfig.json` 파일의 [`files`](/tsconfig#files) 목록에 지정된 파일 이름입니다.
이러한 루트 파일은 지정된 순서와 동일한 순서로 전처리됩니다.
파일이 목록에 추가되기 전에, 파일의 모든 삼중 슬래시 참조가 처리되고 해당 대상이 포함됩니다.
삼중 슬래시 참조는 파일에서 보이는 순서대로 깊이 우선 방식으로 해석됩니다.

삼중 슬래시 참조 경로는 상대 경로가 사용된 경우 포함하는 파일을 기준으로 해석됩니다.

### 오류

존재하지 않는 파일을 참조하면 오류입니다.
파일이 자체에 대한 삼중 슬래시 참조를 갖는 것은 오류입니다.

### `--noResolve` 사용하기

컴파일러 플래그 [`noResolve`](/tsconfig#noResolve)가 지정되면, 삼중 슬래시 참조가 무시됩니다; 새 파일을 추가하거나 제공된 파일의 순서를 변경하지 않습니다.

## `/// <reference types="..." />`

_종속성_ 선언 역할을 하는 `/// <reference path="..." />` 지시어와 마찬가지로, `/// <reference types="..." />` 지시어는 패키지에 대한 종속성을 선언합니다.

이러한 패키지 이름을 해석하는 프로세스는 `import` 문에서 모듈 이름을 해석하는 프로세스와 유사합니다.
삼중 슬래시 참조 타입 지시어를 선언 패키지에 대한 `import`로 생각하면 쉽습니다.

예를 들어, 선언 파일에 `/// <reference types="node" />`를 포함하면 이 파일이 `@types/node/index.d.ts`에 선언된 이름을 사용함을 선언합니다;
따라서 이 패키지는 선언 파일과 함께 컴파일에 포함되어야 합니다.

`.ts` 파일에서 `@types` 패키지에 대한 종속성을 선언하려면, 커맨드 라인이나 `tsconfig.json`에서 [`types`](/tsconfig#types)를 대신 사용하세요.
자세한 내용은 [`tsconfig.json` 파일에서 `@types`, `typeRoots` 및 `types` 사용하기](/docs/handbook/tsconfig-json.html#types-typeroots-and-types)를 참조하세요.

## `/// <reference lib="..." />`

이 지시어를 사용하면 파일이 기존 내장 _lib_ 파일을 명시적으로 포함할 수 있습니다.

내장 _lib_ 파일은 _tsconfig.json_의 [`lib`](/tsconfig#lib) 컴파일러 옵션과 동일한 방식으로 참조됩니다 (예: `lib="lib.es2015.d.ts"`가 아닌 `lib="es2015"` 사용 등).

DOM API나 `Symbol` 또는 `Iterable`과 같은 내장 JS 런타임 생성자와 같은 내장 타입에 의존하는 선언 파일 작성자의 경우, 삼중 슬래시 참조 lib 지시어가 권장됩니다. 이전에 이러한 .d.ts 파일은 해당 타입의 순방향/중복 선언을 추가해야 했습니다.

예를 들어, 컴파일의 파일 중 하나에 `/// <reference lib="es2017.string" />`을 추가하는 것은 `--lib es2017.string`으로 컴파일하는 것과 동일합니다.

```ts
/// <reference lib="es2017.string" />

"foo".padStart(4);
```

## `/// <reference no-default-lib="true"/>`

이 지시어는 파일을 _기본 라이브러리_로 표시합니다.
`lib.d.ts` 및 다양한 변형의 맨 위에서 이 주석을 볼 수 있습니다.

이 지시어는 컴파일러에게 기본 라이브러리(즉, `lib.d.ts`)를 컴파일에 포함하지 _않도록_ 지시합니다.
여기서의 영향은 커맨드 라인에서 [`noLib`](/tsconfig#noLib)를 전달하는 것과 유사합니다.

또한 [`skipDefaultLibCheck`](/tsconfig#skipDefaultLibCheck)를 전달할 때, 컴파일러는 `/// <reference no-default-lib="true"/>`가 있는 파일의 검사만 건너뜁니다.

## `/// <amd-module />`

기본적으로 AMD 모듈은 익명으로 생성됩니다.
이로 인해 번들러(예: `r.js`)와 같은 다른 도구를 사용하여 결과 모듈을 처리할 때 문제가 발생할 수 있습니다.

`amd-module` 지시어를 사용하면 컴파일러에 선택적 모듈 이름을 전달할 수 있습니다:

##### amdModule.ts

```ts
/// <amd-module name="NamedModule"/>
export class C {}
```

AMD `define`을 호출하는 일부로 모듈에 `NamedModule` 이름이 할당됩니다:

##### amdModule.js

```js
define("NamedModule", ["require", "exports"], function (require, exports) {
  var C = (function () {
    function C() {}
    return C;
  })();
  exports.C = C;
});
```

## `/// <amd-dependency />`

> **참고**: 이 지시어는 사용되지 않습니다. 대신 `import "moduleName";` 문을 사용하세요.

`/// <amd-dependency path="x" />`는 결과 모듈의 require 호출에 주입되어야 하는 비TS 모듈 종속성에 대해 컴파일러에 알립니다.

`amd-dependency` 지시어는 선택적 `name` 속성도 가질 수 있습니다; 이를 통해 amd-dependency에 대한 선택적 이름을 전달할 수 있습니다:

```ts
/// <amd-dependency path="legacy/moduleA" name="moduleA"/>
declare var moduleA: MyType;
moduleA.callStuff();
```

생성된 JS 코드:

```js
define(["require", "exports", "legacy/moduleA"], function (
  require,
  exports,
  moduleA
) {
  moduleA.callStuff();
});
```

## `preserve="true"`

삼중 슬래시 지시어는 컴파일러가 출력에서 제거하지 못하도록 `preserve="true"`로 표시할 수 있습니다.

예를 들어, 다음은 출력에서 지워집니다:

```ts
/// <reference path="..." />
/// <reference types="..." />
/// <reference lib="..." />
```

그러나 다음은 유지됩니다:

```ts
/// <reference path="..." preserve="true" />
/// <reference types="..." preserve="true" />
/// <reference lib="..." preserve="true" />
```

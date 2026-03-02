# 타입 호환성 (Type Compatibility)

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

TypeScript의 구조적 타입 시스템의 기본 규칙은 `y`가 최소한 `x`와 같은 멤버를 가지고 있으면 `x`가 `y`와 호환된다는 것입니다. 예를 들어 `name` 프로퍼티를 가진 `Pet`이라는 인터페이스가 있는 다음 코드를 고려하세요:

```ts
interface Pet {
  name: string;
}

let pet: Pet;
// dog의 추론된 타입은 { name: string; owner: string; }입니다
let dog = { name: "Lassie", owner: "Rudd Weatherwax" };
pet = dog;
```

`dog`가 `pet`에 할당될 수 있는지 확인하기 위해, 컴파일러는 `pet`의 각 프로퍼티를 확인하여 `dog`에서 해당하는 호환 가능한 프로퍼티를 찾습니다.
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

`dog`에 추가 `owner` 프로퍼티가 있지만, 이것이 오류를 생성하지 않는다는 점에 유의하세요.
호환성을 확인할 때는 대상 타입(`Pet`의 경우)의 멤버만 고려됩니다. 이 비교 프로세스는 각 멤버와 하위 멤버의 타입을 탐색하면서 재귀적으로 진행됩니다.

그러나 객체 리터럴은 [알려진 프로퍼티만 지정할 수 있습니다](/docs/handbook/2/objects.html#excess-property-checks).
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
y = x; // 오류, x()에 location 프로퍼티가 없습니다
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
이들은 할당이 `any`로부터와 `any`로의 할당, 그리고 해당 숫자 값을 가진 `enum`으로부터와 `enum`으로의 할당을 허용하는 규칙으로 서브타입 호환성을 확장한다는 점에서만 다릅니다.

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
- [`strictNullChecks`](/tsconfig#strictNullChecks)가 꺼져 있으면, `null`과 `undefined`는 `never`와 유사합니다: 대부분의 타입에 할당 가능하고, 대부분의 타입은 그들에게 할당 가능하지 않습니다.
  그들은 서로에게 할당 가능합니다.
- [`strictNullChecks`](/tsconfig#strictNullChecks)가 켜져 있으면, `null`과 `undefined`는 `void`처럼 더 동작합니다: `any`, `unknown`, `void`를 제외하고 어떤 것에도 할당 가능하지 않고 어떤 것으로부터도 할당 가능하지 않습니다 (`undefined`는 항상 `void`에 할당 가능).

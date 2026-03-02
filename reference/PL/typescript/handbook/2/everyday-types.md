# 일상적인 타입

이 챕터에서는 JavaScript 코드에서 찾을 수 있는 가장 일반적인 값의 타입 중 일부를 다루고, TypeScript에서 해당 타입을 설명하는 방법을 설명합니다.
이것은 완전한 목록이 아니며, 이후 챕터에서 다른 타입을 이름 짓고 사용하는 더 많은 방법을 설명할 것입니다.

타입은 타입 어노테이션 외에도 많은 _곳_에 나타날 수 있습니다.
타입 자체에 대해 배우면서, 새로운 구문을 형성하기 위해 이러한 타입을 참조할 수 있는 곳에 대해서도 배울 것입니다.

JavaScript나 TypeScript 코드를 작성할 때 만날 수 있는 가장 기본적이고 일반적인 타입을 검토하는 것부터 시작하겠습니다.
이것들은 나중에 더 복잡한 타입의 핵심 구성 요소가 될 것입니다.

## 기본형: `string`, `number`, `boolean`

JavaScript에는 매우 일반적으로 사용되는 세 가지 [기본형](https://developer.mozilla.org/en-US/docs/Glossary/Primitive)이 있습니다: `string`, `number`, `boolean`.
각각은 TypeScript에서 해당하는 타입을 가집니다.
예상할 수 있듯이, 이것들은 해당 타입의 값에 JavaScript `typeof` 연산자를 사용했을 때 볼 수 있는 것과 같은 이름입니다:

- `string`은 `"Hello, world"`와 같은 문자열 값을 나타냅니다
- `number`는 `42`와 같은 숫자입니다. JavaScript에는 정수에 대한 특별한 런타임 값이 없으므로, `int`나 `float`에 해당하는 것이 없습니다 - 모든 것이 단순히 `number`입니다
- `boolean`은 `true`와 `false` 두 값입니다

> 타입 이름 `String`, `Number`, `Boolean`(대문자로 시작)은 합법적이지만, 코드에서 매우 드물게 나타나는 특수 내장 타입을 참조합니다. 타입에는 _항상_ `string`, `number`, `boolean`을 사용하세요.

## 배열

`[1, 2, 3]`과 같은 배열의 타입을 지정하려면 `number[]` 구문을 사용할 수 있습니다; 이 구문은 모든 타입에 작동합니다(예: `string[]`은 문자열 배열 등).
`Array<number>`로 작성된 것도 볼 수 있으며, 같은 의미입니다.
_제네릭_을 다룰 때 `T<U>` 구문에 대해 더 배울 것입니다.

> `[number]`는 다른 것입니다; [튜플](/docs/handbook/2/objects.html#tuple-types) 섹션을 참조하세요.

## `any`

TypeScript는 또한 특정 값이 타입 검사 오류를 일으키지 않기를 원할 때 사용할 수 있는 특별한 타입 `any`를 가지고 있습니다.

값이 `any` 타입일 때, 어떤 속성에든 접근할 수 있고(그것도 `any` 타입이 됩니다), 함수처럼 호출할 수 있고, 어떤 타입의 값에든 할당하거나 할당받을 수 있으며, 문법적으로 합법적인 거의 모든 것을 할 수 있습니다:

```ts twoslash
let obj: any = { x: 0 };
// 다음 코드 줄 중 어느 것도 컴파일러 오류를 발생시키지 않습니다.
// `any`를 사용하면 모든 추가 타입 검사가 비활성화되며,
// TypeScript보다 환경을 더 잘 알고 있다고 가정합니다.
obj.foo();
obj();
obj.bar = 100;
obj = "hello";
const n: number = obj;
```

`any` 타입은 TypeScript에게 특정 코드 줄이 괜찮다고 설득하기 위해 긴 타입을 작성하고 싶지 않을 때 유용합니다.

### `noImplicitAny`

타입을 지정하지 않고 TypeScript가 컨텍스트에서 추론할 수 없을 때, 컴파일러는 일반적으로 `any`로 기본 설정합니다.

그러나 `any`는 타입 검사되지 않으므로 보통 이것을 피하고 싶습니다.
컴파일러 플래그 [`noImplicitAny`](/tsconfig#noImplicitAny)를 사용하여 암시적 `any`를 오류로 표시하세요.

## 변수의 타입 어노테이션

`const`, `var`, `let`을 사용하여 변수를 선언할 때, 선택적으로 타입 어노테이션을 추가하여 변수의 타입을 명시적으로 지정할 수 있습니다:

```ts twoslash
let myName: string = "Alice";
//        ^^^^^^^^ 타입 어노테이션
```

> TypeScript는 `int x = 0;`과 같은 "왼쪽 타입" 스타일 선언을 사용하지 않습니다.
> 타입 어노테이션은 항상 타입화되는 것 _다음에_ 옵니다.

그러나 대부분의 경우 이것은 필요하지 않습니다.
가능한 경우, TypeScript는 코드의 타입을 자동으로 _추론_하려고 합니다.
예를 들어, 변수의 타입은 초기화 값의 타입을 기반으로 추론됩니다:

```ts twoslash
// 타입 어노테이션 필요 없음 -- 'myName'은 타입 'string'으로 추론됨
let myName = "Alice";
```

대부분의 경우 추론 규칙을 명시적으로 배울 필요가 없습니다.
시작한다면, 생각하는 것보다 적은 타입 어노테이션을 사용해 보세요 - TypeScript가 무슨 일이 일어나고 있는지 완전히 이해하는 데 얼마나 적게 필요한지 놀랄 수 있습니다.

## 함수

함수는 JavaScript에서 데이터를 전달하는 주요 수단입니다.
TypeScript를 사용하면 함수의 입력값과 출력값의 타입을 모두 지정할 수 있습니다.

### 매개변수 타입 어노테이션

함수를 선언할 때, 함수가 받아들이는 매개변수의 타입을 선언하기 위해 각 매개변수 뒤에 타입 어노테이션을 추가할 수 있습니다.
매개변수 타입 어노테이션은 매개변수 이름 뒤에 옵니다:

```ts twoslash
// 매개변수 타입 어노테이션
function greet(name: string) {
  //                 ^^^^^^^^
  console.log("Hello, " + name.toUpperCase() + "!!");
}
```

매개변수에 타입 어노테이션이 있으면, 해당 함수에 대한 인수가 검사됩니다:

```ts twoslash
// @errors: 2345
declare function greet(name: string): void;
// ---cut---
// 실행하면 런타임 오류가 될 것입니다!
greet(42);
```

> 매개변수에 타입 어노테이션이 없더라도, TypeScript는 여전히 올바른 수의 인수를 전달했는지 검사합니다.

### 반환 타입 어노테이션

반환 타입 어노테이션도 추가할 수 있습니다.
반환 타입 어노테이션은 매개변수 목록 뒤에 나타납니다:

```ts twoslash
function getFavoriteNumber(): number {
  //                        ^^^^^^^^
  return 26;
}
```

변수 타입 어노테이션과 마찬가지로, TypeScript가 `return` 문을 기반으로 함수의 반환 타입을 추론하므로 일반적으로 반환 타입 어노테이션이 필요하지 않습니다.
위 예제의 타입 어노테이션은 아무것도 변경하지 않습니다.
일부 코드베이스는 문서화 목적, 실수로 인한 변경 방지, 또는 단순히 개인 취향을 위해 명시적으로 반환 타입을 지정합니다.

#### 프로미스를 반환하는 함수

프로미스를 반환하는 함수의 반환 타입에 어노테이션을 달고 싶다면, `Promise` 타입을 사용해야 합니다:

```ts twoslash
async function getFavoriteNumber(): Promise<number> {
  return 26;
}
```

### 익명 함수

익명 함수는 함수 선언과 약간 다릅니다.
함수가 TypeScript가 어떻게 호출될지 결정할 수 있는 곳에 나타나면, 해당 함수의 매개변수에 자동으로 타입이 주어집니다.

다음은 예제입니다:

```ts twoslash
// @errors: 2551
const names = ["Alice", "Bob", "Eve"];

// 함수에 대한 문맥적 타이핑 - 매개변수 s는 타입 string으로 추론됨
names.forEach(function (s) {
  console.log(s.toUpperCase());
});

// 화살표 함수에도 문맥적 타이핑이 적용됨
names.forEach((s) => {
  console.log(s.toUpperCase());
});
```

매개변수 `s`에 타입 어노테이션이 없었지만, TypeScript는 `forEach` 함수의 타입과 배열의 추론된 타입을 사용하여 `s`가 가질 타입을 결정했습니다.

이 과정을 _문맥적 타이핑_이라고 하는데, 함수가 발생한 _문맥_이 가져야 할 타입을 알려주기 때문입니다.

추론 규칙과 마찬가지로, 이것이 어떻게 일어나는지 명시적으로 배울 필요는 없지만, 그것이 _일어난다_는 것을 이해하면 타입 어노테이션이 필요하지 않을 때를 알아차리는 데 도움이 됩니다.
나중에, 값이 발생하는 문맥이 타입에 어떻게 영향을 미칠 수 있는지에 대한 더 많은 예제를 볼 것입니다.

## 객체 타입

기본형 외에, 가장 일반적으로 만나는 타입은 _객체 타입_입니다.
이것은 속성을 가진 모든 JavaScript 값을 참조하며, 거의 모든 것이 해당됩니다!
객체 타입을 정의하려면, 단순히 속성과 타입을 나열합니다.

예를 들어, 다음은 점과 같은 객체를 받는 함수입니다:

```ts twoslash
// 매개변수의 타입 어노테이션은 객체 타입입니다
function printCoord(pt: { x: number; y: number }) {
  //                      ^^^^^^^^^^^^^^^^^^^^^^^^
  console.log("좌표의 x 값은 " + pt.x);
  console.log("좌표의 y 값은 " + pt.y);
}
printCoord({ x: 3, y: 7 });
```

여기서, 두 개의 속성 - 둘 다 `number` 타입인 `x`와 `y`를 가진 타입으로 매개변수에 어노테이션을 달았습니다.
`,`나 `;`를 사용하여 속성을 분리할 수 있으며, 마지막 구분자는 어느 쪽이든 선택 사항입니다.

각 속성의 타입 부분도 선택 사항입니다.
타입을 지정하지 않으면 `any`로 가정됩니다.

### 선택적 속성

객체 타입은 일부 또는 전체 속성이 _선택적_임을 지정할 수도 있습니다.
이렇게 하려면 속성 이름 뒤에 `?`를 추가하세요:

```ts twoslash
function printName(obj: { first: string; last?: string }) {
  // ...
}
// 둘 다 OK
printName({ first: "Bob" });
printName({ first: "Alice", last: "Alisson" });
```

JavaScript에서 존재하지 않는 속성에 접근하면, 런타임 오류가 아니라 `undefined` 값을 얻습니다.
이 때문에, 선택적 속성에서 _읽을_ 때, 사용하기 전에 `undefined`를 확인해야 합니다.

```ts twoslash
// @errors: 18048
function printName(obj: { first: string; last?: string }) {
  // 오류 - 'obj.last'가 제공되지 않으면 충돌할 수 있습니다!
  console.log(obj.last.toUpperCase());
  if (obj.last !== undefined) {
    // OK
    console.log(obj.last.toUpperCase());
  }

  // 현대 JavaScript 구문을 사용한 안전한 대안:
  console.log(obj.last?.toUpperCase());
}
```

## 유니온 타입

TypeScript의 타입 시스템을 사용하면 다양한 연산자를 사용하여 기존 타입에서 새 타입을 구축할 수 있습니다.
이제 몇 가지 타입을 작성하는 방법을 알았으므로, 흥미로운 방식으로 _결합_을 시작할 때입니다.

### 유니온 타입 정의

타입을 결합하는 첫 번째 방법은 _유니온_ 타입입니다.
유니온 타입은 두 개 이상의 다른 타입으로 형성되며, 해당 타입 중 _하나_일 수 있는 값을 나타냅니다.
이러한 각 타입을 유니온의 _멤버_라고 합니다.

문자열이나 숫자에서 작동할 수 있는 함수를 작성해 봅시다:

```ts twoslash
// @errors: 2345
function printId(id: number | string) {
  console.log("Your ID is: " + id);
}
// OK
printId(101);
// OK
printId("202");
// 오류
printId({ myID: 22342 });
```

### 유니온 타입으로 작업하기

유니온 타입과 일치하는 값을 _제공_하는 것은 쉽습니다 - 유니온의 멤버 중 하나와 일치하는 타입을 제공하면 됩니다.
유니온 타입의 값을 _가지고 있다면_, 어떻게 작업합니까?

TypeScript는 유니온의 _모든_ 멤버에 유효한 경우에만 연산을 허용합니다.
예를 들어, `string | number` 유니온이 있으면, `string`에서만 사용 가능한 메서드를 사용할 수 없습니다:

```ts twoslash
// @errors: 2339
function printId(id: number | string) {
  console.log(id.toUpperCase());
}
```

해결책은 타입 어노테이션 없이 JavaScript에서 하는 것처럼 코드로 유니온을 _좁히는_ 것입니다.
_좁히기_는 TypeScript가 코드 구조를 기반으로 값에 대해 더 구체적인 타입을 추론할 수 있을 때 발생합니다.

예를 들어, TypeScript는 `string` 값만 `typeof` 값 `"string"`을 가진다는 것을 알고 있습니다:

```ts twoslash
function printId(id: number | string) {
  if (typeof id === "string") {
    // 이 분기에서, id는 타입 'string'입니다
    console.log(id.toUpperCase());
  } else {
    // 여기서, id는 타입 'number'입니다
    console.log(id);
  }
}
```

또 다른 예는 `Array.isArray`와 같은 함수를 사용하는 것입니다:

```ts twoslash
function welcomePeople(x: string[] | string) {
  if (Array.isArray(x)) {
    // 여기: 'x'는 'string[]'입니다
    console.log("Hello, " + x.join(" and "));
  } else {
    // 여기: 'x'는 'string'입니다
    console.log("Welcome lone traveler " + x);
  }
}
```

`else` 분기에서는 특별한 것을 할 필요가 없습니다 - `x`가 `string[]`이 아니었다면, `string`이었어야 합니다.

때때로 모든 멤버가 공통점을 가진 유니온을 가질 것입니다.
예를 들어, 배열과 문자열 모두 `slice` 메서드를 가지고 있습니다.
유니온의 모든 멤버가 공통 속성을 가지고 있다면, 좁히기 없이 해당 속성을 사용할 수 있습니다:

```ts twoslash
// 반환 타입은 number[] | string으로 추론됨
function getFirstThree(x: number[] | string) {
  return x.slice(0, 3);
}
```

> 타입의 _유니온_이 해당 타입 속성의 _교집합_을 가진 것처럼 보이는 것이 혼란스러울 수 있습니다.
> 이것은 우연이 아닙니다 - _유니온_이라는 이름은 타입 이론에서 유래합니다.
> _유니온_ `number | string`은 각 타입의 _값의 유니온_을 취하여 구성됩니다.
> 각 집합에 대한 해당 사실이 있는 두 집합이 있을 때, 집합의 _유니온_에는 해당 사실의 _교집합_만 적용됩니다.
> 예를 들어, 모자를 쓴 키가 큰 사람들의 방이 있고, 모자를 쓴 스페인어 사용자들의 또 다른 방이 있다면, 그 방들을 결합한 후 _모든_ 사람에 대해 알 수 있는 유일한 것은 모자를 쓰고 있어야 한다는 것입니다.

## 타입 별칭

객체 타입과 유니온 타입을 타입 어노테이션에 직접 작성하여 사용해 왔습니다.
이것은 편리하지만, 같은 타입을 두 번 이상 사용하고 단일 이름으로 참조하고 싶은 것이 일반적입니다.

_타입 별칭_은 정확히 그것입니다 - 모든 _타입_에 대한 _이름_입니다.
타입 별칭의 구문은 다음과 같습니다:

```ts twoslash
type Point = {
  x: number;
  y: number;
};

// 이전 예제와 정확히 같음
function printCoord(pt: Point) {
  console.log("좌표의 x 값은 " + pt.x);
  console.log("좌표의 y 값은 " + pt.y);
}

printCoord({ x: 100, y: 100 });
```

실제로 타입 별칭을 사용하여 객체 타입뿐만 아니라 모든 타입에 이름을 지정할 수 있습니다.
예를 들어, 타입 별칭은 유니온 타입에 이름을 지정할 수 있습니다:

```ts twoslash
type ID = number | string;
```

별칭은 _오직_ 별칭입니다 - 타입 별칭을 사용하여 같은 타입의 다른/구별되는 "버전"을 만들 수 없습니다.
별칭을 사용할 때, 별칭이 지정된 타입을 작성한 것과 정확히 같습니다.
다시 말해서, 이 코드는 _불법_으로 보일 수 있지만, 두 타입이 같은 타입의 별칭이기 때문에 TypeScript에 따르면 OK입니다:

```ts twoslash
declare function getInput(): string;
declare function sanitize(str: string): string;
// ---cut---
type UserInputSanitizedString = string;

function sanitizeInput(str: string): UserInputSanitizedString {
  return sanitize(str);
}

// 정제된 입력 생성
let userInput = sanitizeInput(getInput());

// 여전히 문자열로 재할당 가능
userInput = "new input";
```

## 인터페이스

_인터페이스 선언_은 객체 타입에 이름을 지정하는 또 다른 방법입니다:

```ts twoslash
interface Point {
  x: number;
  y: number;
}

function printCoord(pt: Point) {
  console.log("좌표의 x 값은 " + pt.x);
  console.log("좌표의 y 값은 " + pt.y);
}

printCoord({ x: 100, y: 100 });
```

위에서 타입 별칭을 사용했을 때처럼, 예제는 익명 객체 타입을 사용한 것처럼 작동합니다.
TypeScript는 `printCoord`에 전달한 값의 _구조_에만 관심이 있습니다 - 예상되는 속성을 가지고 있는지만 신경 씁니다.
타입의 구조와 기능에만 관심이 있는 것이 TypeScript를 _구조적으로 타입화된_ 타입 시스템이라고 부르는 이유입니다.

### 타입 별칭과 인터페이스의 차이점

타입 별칭과 인터페이스는 매우 유사하며, 많은 경우 자유롭게 선택할 수 있습니다.
`interface`의 거의 모든 기능은 `type`에서 사용 가능하며, 핵심 차이점은 타입은 새 속성을 추가하기 위해 다시 열 수 없지만 인터페이스는 항상 확장 가능하다는 것입니다.

<div class='table-container'>
<table class='full-width-table'>
  <tbody>
    <tr>
      <th><code>인터페이스</code></th>
      <th><code>타입</code></th>
    </tr>
    <tr>
      <td>
        <p>인터페이스 확장</p>
        <code><pre>
interface Animal {
  name: string;
}<br/>
interface Bear extends Animal {
  honey: boolean;
}<br/>
const bear = getBear();
bear.name;
bear.honey;
        </pre></code>
      </td>
      <td>
        <p>교차를 통한 타입 확장</p>
        <code><pre>
type Animal = {
  name: string;
}<br/>
type Bear = Animal & {
  honey: boolean;
}<br/>
const bear = getBear();
bear.name;
bear.honey;
        </pre></code>
      </td>
    </tr>
    <tr>
      <td>
        <p>기존 인터페이스에 새 필드 추가</p>
        <code><pre>
interface Window {
  title: string;
}<br/>
interface Window {
  ts: TypeScriptAPI;
}<br/>
const src = 'const a = "Hello World"';
window.ts.transpileModule(src, {});
        </pre></code>
      </td>
      <td>
        <p>타입은 생성 후 변경 불가</p>
        <code><pre>
type Window = {
  title: string;
}<br/>
type Window = {
  ts: TypeScriptAPI;
}<br/>
<span style="color: #A31515"> // 오류: 중복 식별자 'Window'.</span><br/>
        </pre></code>
      </td>
    </tr>
    </tbody>
</table>
</div>

이러한 개념에 대해 이후 챕터에서 더 배우게 될 것이므로, 지금 당장 모든 것을 이해하지 못해도 걱정하지 마세요.

- TypeScript 버전 4.2 이전에는, 타입 별칭 이름이 오류 메시지에 동등한 익명 타입 대신 나타날 _수_ 있었습니다(바람직할 수도 그렇지 않을 수도 있음). 인터페이스는 오류 메시지에서 항상 이름이 지정됩니다.
- 타입 별칭은 선언 병합에 참여하지 않지만, 인터페이스는 참여합니다.
- 인터페이스는 객체의 형태를 선언하는 데만 사용할 수 있으며, 기본형의 이름을 바꾸는 데 사용할 수 없습니다.
- 인터페이스 이름은 오류 메시지에서 _항상_ 원래 형태로 나타나지만, 이름으로 사용될 때만 그렇습니다.
- `extends`와 함께 인터페이스를 사용하는 것이 교차를 사용하는 타입 별칭보다 컴파일러 성능이 더 좋을 수 있습니다.

대부분의 경우 개인 취향에 따라 선택할 수 있으며, TypeScript는 다른 종류의 선언이 필요할 때 알려줍니다. 휴리스틱을 원한다면, `type`의 기능이 필요할 때까지 `interface`를 사용하세요.

## 타입 단언

때때로 TypeScript가 알 수 없는 값의 타입에 대한 정보를 가지고 있을 것입니다.

예를 들어, `document.getElementById`를 사용하는 경우, TypeScript는 이것이 _어떤_ 종류의 `HTMLElement`를 반환할 것이라는 것만 알지만, 페이지에 주어진 ID를 가진 `HTMLCanvasElement`가 항상 있을 것이라는 것을 알 수 있습니다.

이 상황에서, _타입 단언_을 사용하여 더 구체적인 타입을 지정할 수 있습니다:

```ts twoslash
const myCanvas = document.getElementById("main_canvas") as HTMLCanvasElement;
```

타입 어노테이션과 마찬가지로, 타입 단언은 컴파일러에 의해 제거되며 코드의 런타임 동작에 영향을 미치지 않습니다.

코드가 `.tsx` 파일에 있지 않다면, 동등한 꺾쇠 괄호 구문도 사용할 수 있습니다:

```ts twoslash
const myCanvas = <HTMLCanvasElement>document.getElementById("main_canvas");
```

> 알림: 타입 단언은 컴파일 타임에 제거되므로, 타입 단언과 관련된 런타임 검사가 없습니다.
> 타입 단언이 잘못되어도 예외나 `null`이 생성되지 않습니다.

TypeScript는 타입을 _더 구체적_이거나 _덜 구체적_인 버전으로 변환하는 타입 단언만 허용합니다.
이 규칙은 다음과 같은 "불가능한" 강제 변환을 방지합니다:

```ts twoslash
// @errors: 2352
const x = "hello" as number;
```

때때로 이 규칙이 너무 보수적이어서 유효할 수 있는 더 복잡한 강제 변환을 허용하지 않을 수 있습니다.
이런 일이 발생하면, 먼저 `any`(또는 나중에 소개할 `unknown`)로, 그 다음 원하는 타입으로 두 번의 단언을 사용할 수 있습니다:

```ts twoslash
declare const expr: any;
type T = { a: 1; b: 2; c: 3 };
// ---cut---
const a = expr as any as T;
```

## 리터럴 타입

일반 타입 `string`과 `number` 외에도, 타입 위치에서 _특정_ 문자열과 숫자를 참조할 수 있습니다.

이것에 대해 생각하는 한 가지 방법은 JavaScript가 변수를 선언하는 다양한 방법을 가지고 있다는 것을 고려하는 것입니다. `var`와 `let` 모두 변수 내부에 있는 것을 변경할 수 있도록 허용하고, `const`는 허용하지 않습니다. 이것은 TypeScript가 리터럴에 대한 타입을 만드는 방법에 반영됩니다.

```ts twoslash
let changingString = "Hello World";
changingString = "Olá Mundo";
// `changingString`이 가능한 모든 문자열을 나타낼 수 있기 때문에,
// TypeScript는 타입 시스템에서 그렇게 설명합니다
changingString;
// ^?

const constantString = "Hello World";
// `constantString`은 하나의 가능한 문자열만 나타낼 수 있기 때문에,
// 리터럴 타입 표현을 가집니다
constantString;
// ^?
```

그 자체로, 리터럴 타입은 그다지 가치가 없습니다:

```ts twoslash
// @errors: 2322
let x: "hello" = "hello";
// OK
x = "hello";
// ...
x = "howdy";
```

하나의 값만 가질 수 있는 변수를 갖는 것은 별로 쓸모가 없습니다!

하지만 리터럴을 유니온으로 _결합_하면, 훨씬 더 유용한 개념을 표현할 수 있습니다 - 예를 들어, 특정 알려진 값 세트만 받아들이는 함수:

```ts twoslash
// @errors: 2345
function printText(s: string, alignment: "left" | "right" | "center") {
  // ...
}
printText("Hello, world", "left");
printText("G'day, mate", "centre");
```

숫자 리터럴 타입도 같은 방식으로 작동합니다:

```ts twoslash
function compare(a: string, b: string): -1 | 0 | 1 {
  return a === b ? 0 : a > b ? 1 : -1;
}
```

물론, 이것들을 비리터럴 타입과 결합할 수 있습니다:

```ts twoslash
// @errors: 2345
interface Options {
  width: number;
}
function configure(x: Options | "auto") {
  // ...
}
configure({ width: 100 });
configure("auto");
configure("automatic");
```

한 가지 더 종류의 리터럴 타입이 있습니다: 불리언 리터럴.
두 개의 불리언 리터럴 타입만 있으며, 짐작할 수 있듯이 `true`와 `false` 타입입니다.
`boolean` 타입 자체는 실제로 `true | false` 유니온의 별칭일 뿐입니다.

### 리터럴 추론

객체로 변수를 초기화할 때, TypeScript는 해당 객체의 속성이 나중에 값을 변경할 수 있다고 가정합니다.
예를 들어, 다음과 같은 코드를 작성했다면:

```ts twoslash
declare const someCondition: boolean;
// ---cut---
const obj = { counter: 0 };
if (someCondition) {
  obj.counter = 1;
}
```

TypeScript는 이전에 `0`이었던 필드에 `1`을 할당하는 것이 오류라고 가정하지 않습니다.
이것을 말하는 또 다른 방법은 `obj.counter`가 `0`이 아닌 `number` 타입을 가져야 한다는 것입니다. 타입은 _읽기_와 _쓰기_ 동작을 모두 결정하는 데 사용되기 때문입니다.

문자열에도 같은 것이 적용됩니다:

```ts twoslash
// @errors: 2345
declare function handleRequest(url: string, method: "GET" | "POST"): void;

const req = { url: "https://example.com", method: "GET" };
handleRequest(req.url, req.method);
```

위 예제에서 `req.method`는 `"GET"`이 아닌 `string`으로 추론됩니다. `req` 생성과 `handleRequest` 호출 사이에 `"GUESS"`와 같은 새 문자열을 `req.method`에 할당할 수 있는 코드가 평가될 수 있기 때문에, TypeScript는 이 코드에 오류가 있다고 간주합니다.

이것을 해결하는 두 가지 방법이 있습니다.

1. 어느 위치에서든 타입 단언을 추가하여 추론을 변경할 수 있습니다:

   ```ts twoslash
   declare function handleRequest(url: string, method: "GET" | "POST"): void;
   // ---cut---
   // 변경 1:
   const req = { url: "https://example.com", method: "GET" as "GET" };
   // 변경 2
   handleRequest(req.url, req.method as "GET");
   ```

   변경 1은 "`req.method`가 항상 _리터럴 타입_ `"GET"`을 가지도록 의도합니다"를 의미하여, 그 필드에 `"GUESS"`가 할당되는 것을 방지합니다.
   변경 2는 "다른 이유로 `req.method`가 `"GET"` 값을 가진다는 것을 알고 있습니다"를 의미합니다.

2. `as const`를 사용하여 전체 객체를 타입 리터럴로 변환할 수 있습니다:

   ```ts twoslash
   declare function handleRequest(url: string, method: "GET" | "POST"): void;
   // ---cut---
   const req = { url: "https://example.com", method: "GET" } as const;
   handleRequest(req.url, req.method);
   ```

`as const` 접미사는 타입 시스템에서 `const`처럼 작동하여, 모든 속성이 `string`이나 `number`와 같은 더 일반적인 버전이 아닌 리터럴 타입으로 할당되도록 보장합니다.

## `null`과 `undefined`

JavaScript에는 없거나 초기화되지 않은 값을 나타내는 데 사용되는 두 가지 기본 값이 있습니다: `null`과 `undefined`.

TypeScript에는 같은 이름의 해당하는 두 가지 _타입_이 있습니다. 이러한 타입이 어떻게 동작하는지는 [`strictNullChecks`](/tsconfig#strictNullChecks) 옵션이 켜져 있는지에 따라 달라집니다.

### `strictNullChecks` 꺼짐

[`strictNullChecks`](/tsconfig#strictNullChecks)가 _꺼져_ 있으면, `null`이거나 `undefined`일 수 있는 값에 여전히 정상적으로 접근할 수 있으며, `null`과 `undefined` 값은 모든 타입의 속성에 할당할 수 있습니다.
이것은 null 검사가 없는 언어(예: C#, Java)의 동작과 유사합니다.
이러한 값에 대한 검사 부재는 버그의 주요 원인이 되는 경향이 있습니다; 코드베이스에서 실용적이라면 항상 [`strictNullChecks`](/tsconfig#strictNullChecks)를 켜는 것을 권장합니다.

### `strictNullChecks` 켜짐

[`strictNullChecks`](/tsconfig#strictNullChecks)가 _켜져_ 있으면, 값이 `null`이거나 `undefined`일 때, 해당 값에 메서드나 속성을 사용하기 전에 해당 값을 테스트해야 합니다.
선택적 속성을 사용하기 전에 `undefined`를 확인하는 것처럼, _좁히기_를 사용하여 `null`일 수 있는 값을 확인할 수 있습니다:

```ts twoslash
function doSomething(x: string | null) {
  if (x === null) {
    // 아무것도 하지 않음
  } else {
    console.log("Hello, " + x.toUpperCase());
  }
}
```

### 널 아님 단언 연산자 (접미사 `!`)

TypeScript는 또한 명시적 검사 없이 타입에서 `null`과 `undefined`를 제거하는 특별한 구문을 가지고 있습니다.
어떤 표현식 뒤에 `!`를 쓰면 효과적으로 값이 `null`이거나 `undefined`가 아니라는 타입 단언이 됩니다:

```ts twoslash
function liveDangerously(x?: number | null) {
  // 오류 없음
  console.log(x!.toFixed());
}
```

다른 타입 단언과 마찬가지로, 이것은 코드의 런타임 동작을 변경하지 않으므로, 값이 `null`이거나 `undefined`가 _아닐 수_ 있다는 것을 알 때만 `!`를 사용하는 것이 중요합니다.

## 열거형

열거형은 TypeScript가 JavaScript에 추가한 기능으로, 가능한 명명된 상수 세트 중 하나일 수 있는 값을 설명할 수 있게 합니다. 대부분의 TypeScript 기능과 달리, 이것은 JavaScript에 대한 타입 수준 추가가 _아니라_ 언어와 런타임에 추가된 것입니다. 이 때문에, 존재한다는 것을 알아야 하지만 확신이 서지 않는 한 사용을 보류하는 것이 좋습니다. [열거형 참조 페이지](/docs/handbook/enums.html)에서 열거형에 대해 더 읽을 수 있습니다.

## 덜 일반적인 기본형

타입 시스템에서 나타나는 JavaScript의 나머지 기본형을 언급할 가치가 있습니다.
여기서 깊이 다루지는 않겠습니다.

#### `bigint`

ES2020부터, JavaScript에는 매우 큰 정수를 위한 기본형 `BigInt`가 있습니다:

```ts twoslash
// @target: es2020

// BigInt 함수를 통해 bigint 생성
const oneHundred: bigint = BigInt(100);

// 리터럴 구문을 통해 BigInt 생성
const anotherHundred: bigint = 100n;
```

BigInt에 대해 [TypeScript 3.2 릴리스 노트](/docs/handbook/release-notes/typescript-3-2.html#bigint)에서 더 배울 수 있습니다.

#### `symbol`

JavaScript에는 `Symbol()` 함수를 통해 전역적으로 고유한 참조를 만드는 데 사용되는 기본형이 있습니다:

```ts twoslash
// @errors: 2367
const firstName = Symbol("name");
const secondName = Symbol("name");

if (firstName === secondName) {
  // 절대 일어날 수 없음
}
```

[심볼 참조 페이지](/docs/handbook/symbols.html)에서 더 배울 수 있습니다.

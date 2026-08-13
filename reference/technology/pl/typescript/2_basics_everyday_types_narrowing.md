# TypeScript 기본 사항, 일상적인 타입, 내로잉

## 기본 사항 · 일상적인 타입 · 내로잉

## 기본 사항

> **원문:** https://www.typescriptlang.org/docs/handbook/2/basic-types.html

- JavaScript의 모든 값은 다양한 연산을 실행해 관찰 가능한 일련의 동작을 가짐
- 예: `message`라는 변수에 대해 실행할 수 있는 연산

```js
// 'message'의 'toLowerCase' 속성에 접근한 다음
// 호출합니다
message.toLowerCase();

// 'message'를 호출합니다
message();
```

- 분석
  - 첫 번째 줄: `toLowerCase`라는 속성에 접근한 다음 호출
  - 두 번째 줄: `message`를 직접 호출 시도
- `message`의 값을 모르는 상태(흔한 경우)라면 실행 결과를 확실하게 예측 불가
  - 각 연산의 동작은 전적으로 초기 값에 의존
- 확인이 필요한 질문
  - `message`는 호출 가능한가
  - `toLowerCase`라는 속성이 있는가
  - 있다면 `toLowerCase`는 호출 가능한가
  - 둘 다 호출 가능하다면 무엇을 반환하는가
- 이런 질문의 답은 JavaScript 작성 시 통상 머릿속에 담아두는 것들이며, 모든 세부 사항을 제대로 이해했기를 바라야 하는 상황

`message`가 다음과 같이 정의되었다고 가정:

```js
const message = "Hello World!";
```

- `message.toLowerCase()` 실행 → 동일 문자열을 소문자로 반환
- 두 번째 코드 줄: JavaScript에 익숙하면 예외 발생을 예상 가능

```txt
TypeError: message is not a function
```

- 이런 실수는 피하는 것이 이상적

- 코드 실행 시 JavaScript 런타임의 동작 방식 → 값의 _타입_(어떤 종류의 동작·기능이 있는지)을 파악하는 것에 해당
  - 위 `TypeError`가 암시하는 것: 문자열 `"Hello World!"`는 함수로 호출 불가
- `string`·`number` 같은 기본형: `typeof` 연산자로 런타임에 타입 식별 가능
- 함수 등 다른 값: 타입 식별용 런타임 메커니즘 부재

예시:

```js
function fn(x) {
  return x.flip();
}
```

- 코드를 읽으면 이 함수가 호출 가능한 `flip` 속성을 가진 객체에서만 작동한다는 것을 _관찰_ 가능
  - 다만 JavaScript는 실행 중 확인 가능한 방식으로 이 정보를 표면화하지 않음
- 순수 JavaScript에서 `fn`이 특정 값으로 무엇을 하는지 아는 유일한 방법: 호출 후 결과 확인
- 이런 동작 방식 → 실행 전 예측 어려움 → 작성 중 코드 동작을 알기 어려움

- _타입_: 어떤 값이 `fn`에 전달될 수 있고 어떤 값이 충돌을 일으킬지 설명하는 개념
- JavaScript는 _동적_ 타이핑만 제공(코드를 실행해 결과를 보는 방식)
- 대안: _정적_ 타입 시스템으로 실행 _전_에 예상 동작 예측

### 정적 타입 검사

- 문자열을 함수로 호출할 때 발생한 `TypeError` 재검토
- 대부분의 사람은 코드 실행 중 어떤 오류도 원치 않음 → 오류는 버그로 간주
- 새 코드를 작성할 때는 새 버그를 도입하지 않도록 최선을 다함
- 코드 추가 → 파일 저장 → 재실행 → 즉시 오류 확인 흐름이면 문제를 빠르게 분리 가능하나, 항상 그렇지는 않음
  - 기능을 충분히 테스트하지 않으면 잠재 오류를 실제로 만나지 못할 수 있음
  - 운 좋게 오류를 목격해도, 그사이 대규모 리팩터링과 추가 코드를 파헤쳐야 할 수 있음
- 이상적으로는 코드 실행 _전_에 이런 버그를 찾는 도구가 필요 → TypeScript 같은 정적 타입 검사기의 역할
- _정적 타입 시스템_: 프로그램 실행 시 값이 가질 형태와 동작을 설명
- TypeScript 같은 타입 검사기는 그 정보로 문제가 생길 수 있는 지점을 알려줌

```ts twoslash
// @errors: 2349
const message = "hello!";

message();
```

- TypeScript로 위 샘플을 실행하면 코드를 처음 실행하기 전에 오류 메시지를 받음

### 예외를 던지지 않는 실패

- 지금까지 논의한 것은 런타임 오류 - JavaScript 런타임이 무언가 말이 안 된다고 판단해 알려주는 경우
- 이런 동작은 [ECMAScript 명세](https://tc39.github.io/ecma262/)에 언어가 예상치 못한 상황을 만났을 때의 동작이 명시되어 있기 때문에 발생
- 예: 명세는 호출할 수 없는 것을 호출하려 하면 오류를 발생시켜야 한다고 규정
  - "당연한 동작"처럼 들리지만, 존재하지 않는 속성 접근도 오류를 발생시켜야 한다고 상상할 수 있음
  - 실제로는 JavaScript가 다른 동작을 제공 → `undefined` 값 반환

```js
const user = {
  name: "Daniel",
  age: 26,
};

user.location; // undefined를 반환
```

- 정적 타입 시스템은 "유효한" JavaScript로서 즉시 오류를 던지지 않는 코드라도, 시스템 안에서 어떤 코드를 오류로 표시할지 결정 필요
- TypeScript에서 다음 코드는 `location`이 정의되지 않았다는 오류 생성

```ts twoslash
// @errors: 2339
const user = {
  name: "Daniel",
  age: 26,
};

user.location;
```

- 때로는 이것이 표현력의 트레이드오프를 의미하지만, 의도는 프로그램의 합법적인 버그를 잡는 것
- TypeScript는 _많은_ 합법적 버그를 잡음

예: 오타

```ts twoslash
// @noErrors
const announcement = "Hello World!";

// 오타를 얼마나 빨리 발견할 수 있나요?
announcement.toLocaleLowercase();
announcement.toLocalLowerCase();

// 아마도 이것을 쓰려고 했을 것입니다...
announcement.toLocaleLowerCase();
```

호출되지 않은 함수

```ts twoslash
// @noUnusedLocals
// @errors: 2365
function flipCoin() {
  // Math.random()이어야 했음
  return Math.random < 0.5;
}
```

또는 기본 논리 오류

```ts twoslash
// @errors: 2367
const value = Math.random() < 0.5 ? "a" : "b";
if (value !== "a") {
  // ...
} else if (value === "b") {
  // 이런, 도달 불가능
}
```

### 도구를 위한 타입

- TypeScript는 코드 작성 중 실수를 하면 버그를 잡을 수 있음
- 나아가 처음부터 그런 실수를 하는 것 자체를 _방지_할 수도 있음
- 타입 검사기는 변수 및 기타 속성에서 올바른 속성에 접근하고 있는지 확인할 정보를 보유
  - 그 정보를 기반으로 사용할 만한 속성을 _제안_하는 것도 가능
- TypeScript를 코드 편집에도 활용 가능 → 핵심 타입 검사기가 에디터에서 입력 중 오류 메시지·코드 완성 제공
  - 사람들이 TypeScript의 도구 지원을 이야기할 때 자주 언급하는 부분

```ts twoslash
// @noErrors
// @esModuleInterop
import express from "express";
const app = express();

app.get("/", function (req, res) {
  res.sen
//       ^|
});

app.listen(3000);
```

- TypeScript는 도구 지원을 진지하게 다루며, 입력 중 완성·오류 표시를 넘어섬
- TypeScript를 지원하는 에디터가 제공 가능한 기능
  - 오류를 자동 수정하는 "빠른 수정"
  - 코드를 쉽게 재구성하는 리팩터링
  - 변수의 정의로 이동하거나 특정 변수의 모든 참조를 찾는 탐색 기능
- 이 모든 기능은 타입 검사기 위에 구축되어 완전히 크로스 플랫폼 → [좋아하는 에디터에 TypeScript 지원이 있을](https://github.com/Microsoft/TypeScript/wiki/TypeScript-Editor-Support) 가능성 높음

### `tsc`, TypeScript 컴파일러

- 타입 검사를 이야기했지만 아직 타입 _검사기_를 사용하지 않은 상태
- 새로운 친구 `tsc`, TypeScript 컴파일러 필요 → npm으로 설치

```sh
npm install -g typescript
```

> 이것은 TypeScript 컴파일러 `tsc`를 전역으로 설치합니다.
> 로컬 `node_modules` 패키지에서 `tsc`를 실행하려면 `npx`나 유사한 도구를 사용할 수 있습니다.

- 빈 폴더로 이동해 첫 TypeScript 프로그램 `hello.ts` 작성

```ts twoslash
// 세계에 인사합니다.
console.log("Hello world!");
```

- 특별한 장식 없음 - JavaScript로 작성했을 "hello world"와 동일한 모습
- `typescript` 패키지가 설치한 `tsc` 명령으로 타입 검사 실행

```sh
tsc hello.ts
```

- 실행 결과: 콘솔 출력 없음
  - 타입 오류가 없어서 보고할 것이 없었기 때문
- 대신 _파일_ 출력이 생성됨 - 현재 디렉터리에 `hello.ts` 옆에 `hello.js` 파일 생성
  - `tsc`가 `.ts` 파일을 일반 JavaScript로 _컴파일_(_변환_)한 결과
  - 내용 확인 시 TypeScript가 `.ts` 파일 처리 후 내보내는 것을 확인 가능

```js
// 세계에 인사합니다.
console.log("Hello world!");
```

- 이 경우 TypeScript가 변환할 것이 거의 없어 원본과 동일한 모습
- 컴파일러는 사람이 작성한 것 같은 깔끔하고 읽기 쉬운 코드를 출력하려 함
  - 항상 쉬운 일은 아니지만, TypeScript는 일관된 들여쓰기·줄바꿈 처리·주석 유지에 신경 씀

- 타입 검사 오류를 _도입_하는 경우 확인 - `hello.ts` 재작성

```ts twoslash
// @noErrors
// 이것은 산업용 범용 인사 함수입니다:
function greet(person, date) {
  console.log(`Hello ${person}, today is ${date}!`);
}

greet("Brendan");
```

- `tsc hello.ts` 재실행 시 명령줄에서 오류 발생

```txt
Expected 2 arguments, but got 1.
```

- TypeScript가 `greet` 함수에 인수 전달을 잊었다고 알려줌
- 표준 JavaScript만 작성했음에도 타입 검사가 코드 문제를 찾아낸 사례

### 오류가 있어도 내보내기

- 앞선 예제에서 눈치채지 못했을 수 있는 점: `hello.js` 파일이 다시 변경됨
  - 파일을 열어보면 내용이 입력 파일과 기본적으로 동일
- `tsc`가 코드에 오류를 보고했음에도 이런 결과가 나오는 것은 TypeScript의 핵심 가치 중 하나에 기반: 대부분의 경우 _작성자_가 TypeScript보다 더 잘 알 것이라는 전제
- 타입 검사 코드는 실행 가능한 프로그램의 범위를 제한 → 타입 검사기가 허용하는 것에 대한 트레이드오프 존재
  - 대부분 괜찮으나, 이런 검사가 방해가 되는 시나리오도 있음
  - 예: JavaScript 코드를 TypeScript로 마이그레이션하는 중 타입 검사 오류가 도입되는 경우
    - 결국 타입 검사기를 위해 정리하게 되지만, 원래 JavaScript 코드는 이미 작동 중이었음
    - TypeScript로 변환한다고 실행이 중지되어야 할 이유는 없음
- 그래서 TypeScript는 기본적으로 실행을 방해하지 않음
  - 시간이 지나며 실수에 더 방어적이 되고 싶으면 TypeScript를 더 엄격하게 설정 가능
  - 이 경우 [`noEmitOnError`](/tsconfig#noEmitOnError) 컴파일러 옵션 사용
- `hello.ts` 파일을 변경하고 해당 플래그와 함께 `tsc` 실행

```sh
tsc --noEmitOnError hello.ts
```

- 이번에는 `hello.js`가 업데이트되지 않음

### 명시적 타입

- 지금까지 TypeScript에 `person`이나 `date`가 무엇인지 알려주지 않은 상태
- 코드를 편집해 `person`은 `string`, `date`는 `Date` 객체임을 TypeScript에 알림
  - `date`에는 `toDateString()` 메서드 사용

```ts twoslash
function greet(person: string, date: Date) {
  console.log(`Hello ${person}, today is ${date.toDateString()}!`);
}
```

- `person`과 `date`에 _타입 어노테이션_을 추가해 `greet`가 어떤 타입의 값으로 호출될 수 있는지 설명한 것
  - 시그니처 해석: "`greet`는 `string` 타입의 `person`과 `Date` 타입의 `date`를 받는다"
- 이제 TypeScript는 `greet`가 잘못 호출된 경우를 알려줄 수 있음

```ts twoslash
// @errors: 2345
function greet(person: string, date: Date) {
  console.log(`Hello ${person}, today is ${date.toDateString()}!`);
}

greet("Maddison", Date());
```

- TypeScript가 두 번째 인수에 오류를 보고한 이유
  - JavaScript에서 `Date()`를 호출하면 `string`이 반환됨(놀라운 부분)
  - 반면 `new Date()`로 `Date`를 생성하면 기대했던 결과를 얻음
- 오류 수정 예시

```ts twoslash {4}
function greet(person: string, date: Date) {
  console.log(`Hello ${person}, today is ${date.toDateString()}!`);
}

greet("Maddison", new Date());
```

- 명시적 타입 어노테이션을 항상 작성할 필요는 없음
  - 많은 경우 TypeScript가 타입을 생략해도 스스로 _추론_(또는 "파악") 가능

```ts twoslash
let msg = "hello there!";
//  ^?
```

- `msg`가 `string` 타입이라고 알려주지 않았지만 TypeScript가 스스로 알아냄
  - 이는 하나의 기능 - 타입 시스템이 어차피 같은 타입을 추론할 경우 어노테이션을 추가하지 않는 것이 권장

> 참고: 이전 코드 샘플 내의 메시지 버블은 해당 단어 위에 마우스를 올렸을 때 에디터가 표시하는 것입니다.

### 지워지는 타입

- 위 함수 `greet`를 `tsc`로 컴파일해 JavaScript를 출력할 때의 결과 확인

```ts twoslash
// @showEmit
// @target: es5
function greet(person: string, date: Date) {
  console.log(`Hello ${person}, today is ${date.toDateString()}!`);
}

greet("Maddison", new Date());
```

- 주목할 두 가지
  1. `person`과 `date` 매개변수에 더 이상 타입 어노테이션이 없음
  2. "템플릿 문자열"(백틱(`` ` ``) 사용 문자열)이 연결 방식의 일반 문자열로 변환됨
- 두 번째 항목은 후술, 여기서는 첫 번째 항목에 집중
  - 타입 어노테이션은 JavaScript(정확히는 ECMAScript)의 일부가 아님 → 수정 없이 TypeScript를 실행할 수 있는 브라우저·런타임은 없음
  - 그래서 TypeScript는 처음부터 컴파일러가 필요 - TypeScript 전용 코드를 제거·변환해 실행 가능하게 만드는 과정 필요
  - 대부분의 TypeScript 전용 코드는 지워지며, 타입 어노테이션도 완전히 지워짐

> **기억하세요**: 타입 어노테이션은 프로그램의 런타임 동작을 절대 변경하지 않습니다.

### 다운레벨링

- 또 다른 차이점: 템플릿 문자열이

```js
`Hello ${person}, today is ${date.toDateString()}!`;
```

에서

```js
"Hello ".concat(person, ", today is ").concat(date.toDateString(), "!");
```

로 다시 작성됨

- 이유
  - 템플릿 문자열은 ECMAScript 2015(ECMAScript 6, ES2015, ES6 등으로도 불림)의 기능
  - TypeScript는 코드를 더 새로운 버전의 ECMAScript에서 더 오래된 버전(ECMAScript 3, ES5 등)으로 다시 작성하는 기능을 가짐
  - 더 새롭거나 "높은" 버전에서 더 오래되거나 "낮은" 버전으로 이동하는 과정을 _다운레벨링_이라 부름
- 기본적으로 TypeScript는 매우 오래된 ECMAScript 버전인 ES5를 대상으로 함
  - [`target`](/tsconfig#target) 옵션으로 더 최근 버전 선택 가능
  - `--target es2015` 실행 시 TypeScript가 ECMAScript 2015를 대상으로 변경 → ECMAScript 2015 지원 환경 어디서든 실행 가능해야 함
- `tsc --target es2015 hello.ts` 실행 결과

```js
function greet(person, date) {
  console.log(`Hello ${person}, today is ${date.toDateString()}!`);
}
greet("Maddison", new Date());
```

> 기본 대상이 ES5이지만, 현재 브라우저의 대다수가 ES2015를 지원합니다.
> 따라서 대부분의 개발자는 특정 고대 브라우저와의 호환성이 중요하지 않는 한 ES2015 이상을 대상으로 안전하게 지정할 수 있습니다.

### 엄격성

- 다양한 사용자가 타입 검사기에서 다양한 것을 찾으며 TypeScript에 접근
  - 일부는 프로그램 일부만 검증하며 괜찮은 도구를 얻는 느슨한 옵트인 경험을 원함
    - TypeScript의 기본 경험: 타입 선택적, 추론이 가장 관대한 타입을 취함, `null`/`undefined` 값 검사 없음
    - `tsc`가 오류가 있어도 내보내는 것처럼, 이런 기본값도 방해가 되지 않도록 설정됨
    - 기존 JavaScript 마이그레이션 시 바람직한 첫 단계일 수 있음
  - 반면 많은 사용자는 TypeScript가 가능한 한 즉시 검증하기를 선호 → 언어는 엄격성 설정도 제공
    - 이 설정은 정적 타입 검사를 스위치(검사/미검사)에서 다이얼에 가까운 형태로 전환
    - 다이얼을 높일수록 TypeScript가 더 많이 검사
    - 약간의 추가 작업이 필요하지만 장기적으로 가치 있음 - 더 철저한 검사와 더 정확한 도구 확보
    - 가능하면 새 코드베이스는 항상 이런 엄격성 검사를 켜야 함
- TypeScript에는 켜거나 끌 수 있는 여러 타입 검사 엄격성 플래그 존재
  - 별도 명시가 없는 한 모든 예제는 모두 활성화된 상태로 작성
  - CLI의 [`strict`](/tsconfig#strict) 플래그, `tsconfig.json`의 `"strict": true` → 모두 동시에 켜지지만 개별 옵트아웃 가능
  - 알아야 할 가장 중요한 두 가지: [`noImplicitAny`](/tsconfig#noImplicitAny), [`strictNullChecks`](/tsconfig#strictNullChecks)

### `noImplicitAny`

- 일부 위치에서 TypeScript는 타입을 추론하지 않고 가장 관대한 타입인 `any`로 대체
  - 최악의 상황은 아님 - `any`로 대체하는 것은 어차피 일반 JavaScript 경험과 동일
- 다만 `any`를 사용하면 TypeScript를 사용하는 목적이 무색해지는 경우가 많음
  - 프로그램이 더 많이 타입화될수록 더 많은 검증과 도구를 얻음 → 코딩 중 버그가 더 적어짐
- [`noImplicitAny`](/tsconfig#noImplicitAny) 플래그: 타입이 암시적으로 `any`로 추론되는 모든 변수에 오류 발생

### `strictNullChecks`

- 기본적으로 `null`·`undefined` 같은 값은 다른 모든 타입에 할당 가능
  - 일부 코드를 작성하기 쉽게 만들지만, `null`·`undefined` 처리를 잊는 것은 수많은 버그의 원인
  - 일부는 이를 [십억 달러의 실수](https://www.youtube.com/watch?v=ybrQvs4x0Ps)라고 부름
- [`strictNullChecks`](/tsconfig#strictNullChecks) 플래그: `null`·`undefined` 처리를 더 명시적으로 만들고, 처리를 _잊었는지_ 걱정하는 부담에서 벗어나게 함

---

## 일상적인 타입

> **원문:** https://www.typescriptlang.org/docs/handbook/2/everyday-types.html

- 이 챕터에서 다루는 내용: JavaScript 코드에서 흔히 찾을 수 있는 값의 타입 일부와, TypeScript에서 해당 타입을 설명하는 방법
  - 완전한 목록은 아니며, 이후 챕터에서 다른 타입을 명명·사용하는 방법을 추가로 설명
- 타입은 타입 어노테이션 외에도 많은 _곳_에 나타날 수 있음
  - 타입 자체를 배우면서, 새로운 구문 형성을 위해 타입을 참조할 수 있는 곳도 함께 학습
- 시작점: JavaScript·TypeScript 코드 작성 시 만나는 가장 기본적이고 일반적인 타입 검토
  - 이후 더 복잡한 타입의 핵심 구성 요소가 됨

### 기본형: `string`, `number`, `boolean`

- JavaScript의 매우 일반적인 세 가지 [기본형](https://developer.mozilla.org/en-US/docs/Glossary/Primitive): `string`, `number`, `boolean`
  - 각각 TypeScript에서 해당 타입을 가짐
  - JavaScript `typeof` 연산자로 확인했을 때 보는 이름과 동일
- `string`: `"Hello, world"`와 같은 문자열 값
- `number`: `42`와 같은 숫자
  - JavaScript는 정수용 특별한 런타임 값이 없음 → `int`·`float`에 해당하는 것이 없고 모두 `number`
- `boolean`: `true`와 `false` 두 값

> 타입 이름 `String`, `Number`, `Boolean`(대문자로 시작)은 합법적이지만, 코드에서 매우 드물게 나타나는 특수 내장 타입을 참조합니다. 타입에는 _항상_ `string`, `number`, `boolean`을 사용하세요.

### 배열

- `[1, 2, 3]`과 같은 배열의 타입 지정: `number[]` 구문 사용
  - 모든 타입에 작동(예: `string[]`은 문자열 배열)
- `Array<number>` 표기도 가능하며 동일한 의미
  - `T<U>` 구문은 _제네릭_에서 추가로 설명

> `[number]`는 다른 것입니다; [튜플](/docs/handbook/2/objects.html#tuple-types) 섹션을 참조하세요.

### `any`

- TypeScript는 특정 값이 타입 검사 오류를 일으키지 않기를 원할 때 사용할 수 있는 특별한 타입 `any`를 제공
- `any` 타입 값의 특징
  - 어떤 속성에든 접근 가능(그 결과도 `any` 타입)
  - 함수처럼 호출 가능
  - 어떤 타입의 값에든 할당·할당받기 가능
  - 문법적으로 합법적인 거의 모든 것을 실행 가능

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

- `any` 타입의 용도: TypeScript에게 특정 코드 줄이 괜찮다고 설득하기 위해 긴 타입을 작성하고 싶지 않은 경우

#### `noImplicitAny`

- 타입을 지정하지 않고 TypeScript가 컨텍스트에서 추론할 수 없을 때, 컴파일러는 보통 `any`로 기본 설정
- `any`는 타입 검사되지 않으므로 대부분 회피 대상
  - 컴파일러 플래그 [`noImplicitAny`](/tsconfig#noImplicitAny)로 암시적 `any`를 오류로 표시 가능

### 변수의 타입 어노테이션

- `const`, `var`, `let`으로 변수 선언 시 선택적으로 타입 어노테이션을 추가해 타입을 명시 가능

```ts twoslash
let myName: string = "Alice";
//        ^^^^^^^^ 타입 어노테이션
```

> TypeScript는 `int x = 0;`과 같은 "왼쪽 타입" 스타일 선언을 사용하지 않습니다.
> 타입 어노테이션은 항상 타입화되는 것 _다음에_ 옵니다.

- 대부분의 경우 이 표기는 불필요
  - 가능한 경우 TypeScript는 코드의 타입을 자동으로 _추론_
  - 예: 변수의 타입은 초기화 값의 타입을 기반으로 추론

```ts twoslash
// 타입 어노테이션 필요 없음 -- 'myName'은 타입 'string'으로 추론됨
let myName = "Alice";
```

- 대부분의 경우 추론 규칙을 명시적으로 배울 필요 없음
  - 시작 단계라면 생각보다 적은 타입 어노테이션 사용을 권장 - TypeScript가 얼마나 적은 정보로도 상황을 이해하는지 확인 가능

### 함수

- 함수는 JavaScript에서 데이터를 전달하는 주요 수단
- TypeScript로 함수의 입력값·출력값 타입을 모두 지정 가능

#### 매개변수 타입 어노테이션

- 함수 선언 시 각 매개변수 뒤에 타입 어노테이션을 추가해 받아들이는 타입을 선언 가능
  - 매개변수 타입 어노테이션은 매개변수 이름 뒤에 위치

```ts twoslash
// 매개변수 타입 어노테이션
function greet(name: string) {
  //                 ^^^^^^^^
  console.log("Hello, " + name.toUpperCase() + "!!");
}
```

- 매개변수에 타입 어노테이션이 있으면 해당 함수 호출 시 인수가 검사됨

```ts twoslash
// @errors: 2345
declare function greet(name: string): void;
// ---cut---
// 실행하면 런타임 오류가 될 것입니다!
greet(42);
```

> 매개변수에 타입 어노테이션이 없더라도, TypeScript는 여전히 올바른 수의 인수를 전달했는지 검사합니다.

#### 반환 타입 어노테이션

- 반환 타입 어노테이션도 추가 가능
  - 매개변수 목록 뒤에 위치

```ts twoslash
function getFavoriteNumber(): number {
  //                        ^^^^^^^^
  return 26;
}
```

- 변수 타입 어노테이션과 마찬가지로, TypeScript가 `return` 문 기반으로 반환 타입을 추론하므로 대체로 불필요
  - 위 예제의 타입 어노테이션은 아무것도 변경하지 않음
  - 일부 코드베이스는 문서화 목적·실수 방지·개인 취향으로 명시적 반환 타입을 지정

##### 프로미스를 반환하는 함수

- 프로미스를 반환하는 함수의 반환 타입에 어노테이션을 달려면 `Promise` 타입 사용 필요

```ts twoslash
async function getFavoriteNumber(): Promise<number> {
  return 26;
}
```

#### 익명 함수

- 익명 함수는 함수 선언과 약간 다름
  - 함수가 TypeScript가 호출 방식을 결정할 수 있는 위치에 나타나면 해당 함수의 매개변수에 자동으로 타입이 부여됨

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

- 매개변수 `s`에 타입 어노테이션이 없었지만, TypeScript는 `forEach` 함수의 타입과 배열의 추론된 타입을 사용해 `s`의 타입을 결정
- 이 과정을 _문맥적 타이핑_이라 부름 - 함수가 발생한 _문맥_이 가져야 할 타입을 알려주기 때문
- 추론 규칙과 마찬가지로 동작 방식을 명시적으로 배울 필요는 없으나, "일어난다"는 사실을 이해하면 타입 어노테이션이 필요 없는 시점을 알아차리는 데 도움

### 객체 타입

- 기본형 외에 가장 흔히 만나는 타입은 _객체 타입_
  - 속성을 가진 모든 JavaScript 값을 참조 - 거의 모든 것이 해당
- 객체 타입 정의 방법: 속성과 타입을 나열

예: 점과 같은 객체를 받는 함수

```ts twoslash
// 매개변수의 타입 어노테이션은 객체 타입입니다
function printCoord(pt: { x: number; y: number }) {
  //                      ^^^^^^^^^^^^^^^^^^^^^^^^
  console.log("좌표의 x 값은 " + pt.x);
  console.log("좌표의 y 값은 " + pt.y);
}
printCoord({ x: 3, y: 7 });
```

- 위 예제: 둘 다 `number` 타입인 `x`, `y` 두 속성을 가진 타입으로 매개변수에 어노테이션
  - 속성 구분자는 `,`나 `;` 사용 가능, 마지막 구분자는 선택 사항
  - 각 속성의 타입 부분도 선택 사항 - 지정하지 않으면 `any`로 가정

#### 선택적 속성

- 객체 타입은 일부·전체 속성을 _선택적_으로 지정 가능
  - 방법: 속성 이름 뒤에 `?` 추가

```ts twoslash
function printName(obj: { first: string; last?: string }) {
  // ...
}
// 둘 다 OK
printName({ first: "Bob" });
printName({ first: "Alice", last: "Alisson" });
```

- JavaScript에서 존재하지 않는 속성 접근 시 런타임 오류가 아닌 `undefined` 값을 얻음
  - 이 때문에 선택적 속성을 _읽을_ 때는 사용 전 `undefined` 확인 필요

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

### 유니온 타입

- TypeScript의 타입 시스템으로 다양한 연산자를 사용해 기존 타입에서 새 타입을 구축 가능
  - 이제 몇 가지 타입 작성법을 익혔으므로 흥미로운 방식으로 _결합_해 볼 차례

#### 유니온 타입 정의

- 타입을 결합하는 첫 번째 방법: _유니온_ 타입
  - 두 개 이상의 다른 타입으로 형성되며, 해당 타입 중 _하나_일 수 있는 값을 나타냄
  - 각 타입을 유니온의 _멤버_라 부름

문자열이나 숫자에서 작동할 수 있는 함수 예시:

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

#### 유니온 타입으로 작업하기

- 유니온 타입과 일치하는 값을 _제공_하는 것은 쉬움 - 멤버 중 하나와 일치하는 타입만 제공하면 됨
- 유니온 타입 값이 _있을 때_ 작업 방법
  - TypeScript는 유니온의 _모든_ 멤버에 유효한 경우에만 연산을 허용
  - 예: `string | number` 유니온에서 `string`에서만 사용 가능한 메서드는 사용 불가

```ts twoslash
// @errors: 2339
function printId(id: number | string) {
  console.log(id.toUpperCase());
}
```

- 해결책: 타입 어노테이션 없이 JavaScript에서 하는 것처럼 코드로 유니온을 _좁히기_
  - _좁히기_: TypeScript가 코드 구조를 기반으로 값에 대해 더 구체적인 타입을 추론할 수 있을 때 발생

예: TypeScript는 `string` 값만 `typeof` 값 `"string"`을 가진다는 것을 알고 있음

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

또 다른 예: `Array.isArray`와 같은 함수 사용

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

- `else` 분기: 특별한 처리 불필요 - `x`가 `string[]`이 아니었다면 `string`이었어야 함

- 때로는 모든 멤버가 공통점을 가진 유니온이 존재
  - 예: 배열과 문자열 모두 `slice` 메서드 보유
  - 유니온의 모든 멤버가 공통 속성을 가지면 좁히기 없이 해당 속성 사용 가능

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

### 타입 별칭

- 지금까지 객체 타입·유니온 타입을 타입 어노테이션에 직접 작성해 사용
  - 편리하지만, 같은 타입을 두 번 이상 사용해 단일 이름으로 참조하고 싶은 경우가 흔함
- _타입 별칭_: 모든 _타입_에 대한 _이름_
  - 구문:

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

- 타입 별칭은 객체 타입뿐 아니라 모든 타입에 이름을 지정 가능
  - 예: 유니온 타입에 이름 지정

```ts twoslash
type ID = number | string;
```

- 별칭은 _오직_ 별칭 - 타입 별칭으로 같은 타입의 다른/구별되는 "버전"을 만들 수는 없음
  - 별칭 사용은 별칭이 지정된 타입을 작성한 것과 정확히 동일
  - 다음 코드는 _불법_처럼 보일 수 있지만, 두 타입이 같은 타입의 별칭이므로 TypeScript 기준으로 OK

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

### 인터페이스

- _인터페이스 선언_: 객체 타입에 이름을 지정하는 또 다른 방법

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

- 앞서 타입 별칭을 사용했을 때와 동일하게 작동 - 예제는 익명 객체 타입을 사용한 것처럼 동작
- TypeScript는 `printCoord`에 전달한 값의 _구조_에만 관심 - 예상되는 속성이 있는지만 확인
  - 이 점 때문에 TypeScript를 _구조적으로 타입화된_ 타입 시스템이라 부름

#### 타입 별칭과 인터페이스의 차이점

- 타입 별칭과 인터페이스는 매우 유사 - 대부분 자유롭게 선택 가능
- `interface`의 거의 모든 기능이 `type`에서도 사용 가능
  - 핵심 차이: 타입은 새 속성 추가를 위해 다시 열 수 없지만, 인터페이스는 항상 확장 가능

- 인터페이스: 인터페이스 확장
  - 예:
    ```ts
    interface Animal {
      name: string;
    }
    interface Bear extends Animal {
      honey: boolean;
    }
    const bear = getBear();
    bear.name;
    bear.honey;
    ```
- 타입: 교차를 통한 타입 확장
  - 예:
    ```ts
    type Animal = {
      name: string;
    }
    type Bear = Animal & {
      honey: boolean;
    }
    const bear = getBear();
    bear.name;
    bear.honey;
    ```
- 인터페이스: 기존 인터페이스에 새 필드 추가 가능
  - 예:
    ```ts
    interface Window {
      title: string;
    }
    interface Window {
      ts: TypeScriptAPI;
    }
    const src = 'const a = "Hello World"';
    window.ts.transpileModule(src, {});
    ```
- 타입: 생성 후 변경 불가
  - 예:
    ```ts
    type Window = {
      title: string;
    }
    type Window = {
      ts: TypeScriptAPI;
    }
    // 오류: 중복 식별자 'Window'.
    ```

- 이런 개념은 이후 챕터에서 더 다루므로 지금 전부 이해하지 못해도 무방
- 추가 세부 차이
  - TypeScript 4.2 이전에는 타입 별칭 이름이 오류 메시지에 동등한 익명 타입 대신 나타날 수 있었음(바람직할 수도, 아닐 수도 있음) - 인터페이스는 오류 메시지에서 항상 이름이 지정됨
  - 타입 별칭은 선언 병합에 참여 불가, 인터페이스는 참여 가능
  - 인터페이스는 객체의 형태를 선언하는 데만 사용 가능 - 기본형의 이름을 바꾸는 용도로는 불가
  - 인터페이스 이름은 오류 메시지에서 _항상_ 원래 형태로 나타남 - 다만 이름으로 사용될 때만 해당
  - `extends`와 함께 인터페이스를 사용하는 것이 교차를 사용하는 타입 별칭보다 컴파일러 성능이 더 좋을 수 있음
- 대부분의 경우 개인 취향에 따라 선택 가능 - TypeScript는 다른 종류의 선언이 필요할 때 알려줌
  - 휴리스틱이 필요하면: `type`의 기능이 필요할 때까지 `interface` 사용 권장

### 타입 단언

- 때때로 TypeScript가 알 수 없는 값의 타입에 대한 정보를 작성자가 이미 가지고 있는 경우가 있음
- 예: `document.getElementById` 사용 시 TypeScript는 이것이 _어떤_ 종류의 `HTMLElement`를 반환한다는 것만 알지만, 작성자는 페이지에 특정 ID의 `HTMLCanvasElement`가 항상 있다는 것을 알 수 있음
- 이런 상황에서 _타입 단언_으로 더 구체적인 타입을 지정 가능

```ts twoslash
const myCanvas = document.getElementById("main_canvas") as HTMLCanvasElement;
```

- 타입 어노테이션과 마찬가지로, 타입 단언은 컴파일러에 의해 제거되며 코드의 런타임 동작에 영향 없음
- `.tsx` 파일이 아니라면 동등한 꺾쇠 괄호 구문도 사용 가능

```ts twoslash
const myCanvas = <HTMLCanvasElement>document.getElementById("main_canvas");
```

> 알림: 타입 단언은 컴파일 타임에 제거되므로, 타입 단언과 관련된 런타임 검사가 없습니다.
> 타입 단언이 잘못되어도 예외나 `null`이 생성되지 않습니다.

- TypeScript는 타입을 _더 구체적_이거나 _덜 구체적_인 버전으로 변환하는 타입 단언만 허용
  - 이 규칙으로 다음과 같은 "불가능한" 강제 변환을 방지

```ts twoslash
// @errors: 2352
const x = "hello" as number;
```

- 이 규칙이 너무 보수적이어서 유효할 수 있는 더 복잡한 강제 변환을 막는 경우도 있음
  - 이럴 때는 먼저 `any`(또는 후술할 `unknown`)로, 그다음 원하는 타입으로 두 번 단언 가능

```ts twoslash
declare const expr: any;
type T = { a: 1; b: 2; c: 3 };
// ---cut---
const a = expr as any as T;
```

### 리터럴 타입

- 일반 타입 `string`·`number` 외에도, 타입 위치에서 _특정_ 문자열과 숫자를 참조 가능
- 이해 방법: JavaScript의 변수 선언 방식 - `var`·`let`은 변수 내부 값을 변경 가능, `const`는 불가
  - 이 차이가 TypeScript가 리터럴에 대한 타입을 만드는 방식에 반영됨

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

- 그 자체로는 리터럴 타입의 가치가 크지 않음

```ts twoslash
// @errors: 2322
let x: "hello" = "hello";
// OK
x = "hello";
// ...
x = "howdy";
```

- 하나의 값만 가질 수 있는 변수는 쓸모가 제한적

- 다만 리터럴을 유니온으로 _결합_하면 훨씬 유용한 개념을 표현 가능
  - 예: 특정 알려진 값 세트만 받는 함수

```ts twoslash
// @errors: 2345
function printText(s: string, alignment: "left" | "right" | "center") {
  // ...
}
printText("Hello, world", "left");
printText("G'day, mate", "centre");
```

- 숫자 리터럴 타입도 동일한 방식으로 동작

```ts twoslash
function compare(a: string, b: string): -1 | 0 | 1 {
  return a === b ? 0 : a > b ? 1 : -1;
}
```

- 비리터럴 타입과의 결합도 가능

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

- 한 가지 더: 불리언 리터럴 타입
  - 두 개의 불리언 리터럴 타입만 존재: `true`와 `false`
  - `boolean` 타입 자체는 실제로 `true | false` 유니온의 별칭

#### 리터럴 추론

- 객체로 변수를 초기화할 때, TypeScript는 해당 객체의 속성이 나중에 값을 변경할 수 있다고 가정

```ts twoslash
declare const someCondition: boolean;
// ---cut---
const obj = { counter: 0 };
if (someCondition) {
  obj.counter = 1;
}
```

- TypeScript는 이전에 `0`이었던 필드에 `1`을 할당하는 것을 오류로 가정하지 않음
  - 다른 표현: `obj.counter`가 `0`이 아닌 `number` 타입을 가져야 함 - 타입은 _읽기_와 _쓰기_ 동작 모두를 결정하는 데 사용되기 때문

- 문자열에도 같은 원리 적용

```ts twoslash
// @errors: 2345
declare function handleRequest(url: string, method: "GET" | "POST"): void;

const req = { url: "https://example.com", method: "GET" };
handleRequest(req.url, req.method);
```

- 위 예제에서 `req.method`는 `"GET"`이 아닌 `string`으로 추론됨
  - 이유: `req` 생성과 `handleRequest` 호출 사이에 `"GUESS"`와 같은 새 문자열을 `req.method`에 할당하는 코드가 평가될 수 있음 → TypeScript는 이 코드를 오류로 간주

- 해결 방법 두 가지

1. 어느 위치에서든 타입 단언을 추가해 추론을 변경

   ```ts twoslash
   declare function handleRequest(url: string, method: "GET" | "POST"): void;
   // ---cut---
   // 변경 1:
   const req = { url: "https://example.com", method: "GET" as "GET" };
   // 변경 2
   handleRequest(req.url, req.method as "GET");
   ```

   - 변경 1: "`req.method`가 항상 _리터럴 타입_ `"GET"`을 가지도록 의도" → 그 필드에 `"GUESS"`가 할당되는 것을 방지
   - 변경 2: "다른 이유로 `req.method`가 `"GET"` 값을 가진다는 것을 알고 있음"을 의미

2. `as const`를 사용해 전체 객체를 타입 리터럴로 변환

   ```ts twoslash
   declare function handleRequest(url: string, method: "GET" | "POST"): void;
   // ---cut---
   const req = { url: "https://example.com", method: "GET" } as const;
   handleRequest(req.url, req.method);
   ```

- `as const` 접미사: 타입 시스템에서 `const`처럼 작동 - 모든 속성이 `string`·`number` 같은 일반적인 버전이 아닌 리터럴 타입으로 할당되도록 보장

### `null`과 `undefined`

- JavaScript에는 없거나 초기화되지 않은 값을 나타내는 두 가지 기본 값: `null`과 `undefined`
- TypeScript에는 같은 이름의 해당 _타입_ 두 가지가 존재
  - 이런 타입의 동작은 [`strictNullChecks`](/tsconfig#strictNullChecks) 옵션 활성화 여부에 따라 달라짐

#### `strictNullChecks` 꺼짐

- [`strictNullChecks`](/tsconfig#strictNullChecks)가 _꺼져_ 있으면 `null`·`undefined`일 수 있는 값에 여전히 정상적으로 접근 가능
  - `null`·`undefined` 값은 모든 타입의 속성에 할당 가능
- null 검사가 없는 언어(예: C#, Java)의 동작과 유사
- 이런 값에 대한 검사 부재는 버그의 주요 원인 → 실용적이라면 코드베이스에서 항상 [`strictNullChecks`](/tsconfig#strictNullChecks) 활성화 권장

#### `strictNullChecks` 켜짐

- [`strictNullChecks`](/tsconfig#strictNullChecks)가 _켜져_ 있으면, 값이 `null`이거나 `undefined`일 수 있을 때 메서드·속성 사용 전 해당 값을 테스트 필요
  - 선택적 속성 사용 전 `undefined` 확인과 마찬가지로, _좁히기_로 `null`일 수 있는 값을 확인 가능

```ts twoslash
function doSomething(x: string | null) {
  if (x === null) {
    // 아무것도 하지 않음
  } else {
    console.log("Hello, " + x.toUpperCase());
  }
}
```

#### 널 아님 단언 연산자 (접미사 `!`)

- TypeScript는 명시적 검사 없이 타입에서 `null`·`undefined`를 제거하는 특별한 구문도 제공
  - 표현식 뒤에 `!`를 쓰면 값이 `null`·`undefined`가 아니라는 타입 단언으로 작동

```ts twoslash
function liveDangerously(x?: number | null) {
  // 오류 없음
  console.log(x!.toFixed());
}
```

- 다른 타입 단언과 마찬가지로 코드의 런타임 동작을 변경하지 않음 → 값이 `null`·`undefined`가 _아닐 수_ 있다는 확신이 있을 때만 `!` 사용 필요

### 열거형

- 열거형: TypeScript가 JavaScript에 추가한 기능 - 가능한 명명된 상수 세트 중 하나일 수 있는 값을 설명
- 대부분의 TypeScript 기능과 달리 이것은 JavaScript에 대한 타입 수준 추가가 _아니라_ 언어·런타임에 추가된 기능
  - 이 때문에 존재를 인지할 필요는 있으나, 확신이 서지 않으면 사용을 보류하는 것이 권장
- [열거형 참조 페이지](/docs/handbook/enums.html)에서 추가 학습 가능

### 덜 일반적인 기본형

- 타입 시스템에 나타나는 JavaScript의 나머지 기본형도 언급 필요 - 여기서는 깊게 다루지 않음

##### `bigint`

- ES2020부터 JavaScript는 매우 큰 정수를 위한 기본형 `BigInt` 제공

```ts twoslash
// @target: es2020

// BigInt 함수를 통해 bigint 생성
const oneHundred: bigint = BigInt(100);

// 리터럴 구문을 통해 BigInt 생성
const anotherHundred: bigint = 100n;
```

- BigInt 관련 추가 학습: [TypeScript 3.2 릴리스 노트](/docs/handbook/release-notes/typescript-3-2.html#bigint)

##### `symbol`

- JavaScript에는 `Symbol()` 함수를 통해 전역적으로 고유한 참조를 만드는 기본형 존재

```ts twoslash
// @errors: 2367
const firstName = Symbol("name");
const secondName = Symbol("name");

if (firstName === secondName) {
  // 절대 일어날 수 없음
}
```

- 추가 학습: [심볼 참조 페이지](/docs/handbook/symbols.html)

---

## 좁히기

> **원문:** https://www.typescriptlang.org/docs/handbook/2/narrowing.html

- 좁히기: TypeScript가 처음에 선언된 것보다 더 구체적인 타입으로 변수 타입을 정제하는 과정
- 이 문서에서 다루는 내용: 타입을 좁히기 위한 다양한 TypeScript 메커니즘

### 타입 가드 메서드

#### `typeof` 타입 가드

- JavaScript의 `typeof` 연산자로 기본 타입 검사 가능
  - 반환값: `"string"`, `"number"`, `"boolean"` 등의 문자열

```ts twoslash
function padLeft(padding: number | string, input: string): string {
  if (typeof padding === "number") {
    return " ".repeat(padding) + input;
    //                 ^?
  }
  return padding + input;
  //       ^?
}
```

- TypeScript가 이해하는 `typeof` 반환값
  - `"string"`
  - `"number"`
  - `"bigint"`
  - `"boolean"`
  - `"symbol"`
  - `"undefined"`
  - `"object"`
  - `"function"`

- JavaScript의 특이점: `typeof null`은 실제로 `"object"`를 반환 - JavaScript의 오래된 버그

```ts twoslash
// @errors: 18047
function printAll(strs: string | string[] | null) {
  if (typeof strs === "object") {
    for (const s of strs) {
      console.log(s);
    }
  } else if (typeof strs === "string") {
    console.log(strs);
  } else {
    // 아무것도 하지 않음
  }
}
```

#### 참 같은 값 좁히기

- JavaScript에서 조건문, `&&`, `||`, `if` 문, 불리언 부정(`!`) 등에서 모든 표현식 사용 가능
  - `if` 문은 조건이 항상 `boolean` 타입일 것을 요구하지 않음
- `if`와 같은 구문의 동작: 조건을 `boolean`으로 먼저 "강제 변환"한 후, 결과가 `true`인지 `false`인지에 따라 분기 선택

- `false`로 강제 변환되는 값
  - `0`
  - `NaN`
  - `""` (빈 문자열)
  - `0n` (bigint 버전의 0)
  - `null`
  - `undefined`

- 다른 값은 `true`로 강제 변환됨
  - `Boolean` 함수로 값을 실행하거나, 짧은 이중 불리언 부정으로 언제든 `boolean`으로 강제 변환 가능
  - 후자는 TypeScript가 좁은 리터럴 불리언 타입 `true`로 추론, 전자는 `boolean` 타입으로 추론하는 차이가 있음

```ts twoslash
// 이 두 결과 모두 'true'
Boolean("hello"); // 타입: boolean, 값: true
!!"world";        // 타입: true,    값: true
```

- 이 동작을 활용하는 방식이 꽤 흔함 - 특히 `null`·`undefined` 같은 값에 대한 가드로 사용

```ts twoslash
function printAll(strs: string | string[] | null) {
  if (strs && typeof strs === "object") {
    for (const s of strs) {
      console.log(s);
    }
  } else if (typeof strs === "string") {
    console.log(strs);
  }
}
```

- 다만 기본형에 대한 참 같은 값 검사는 오류가 발생하기 쉬움
  - 예: 빈 문자열 `""`은 falsy하므로 의도치 않게 걸러질 수 있음

#### 동등성 좁히기

- TypeScript는 `===`, `!==`, `==`, `!=` 같은 동등성 검사로도 타입을 좁힐 수 있음

```ts twoslash
function example(x: string | number, y: string | boolean) {
  if (x === y) {
    // 이제 두 타입 모두에서 'string'을 호출할 수 있음
    x.toUpperCase();
    // ^?
    y.toLowerCase();
    // ^?
  } else {
    console.log(x);
    //          ^?
    console.log(y);
    //          ^?
  }
}
```

- `x`와 `y`가 둘 다 같다고 확인되면, TypeScript는 두 값의 타입도 같아야 한다는 것을 인지
  - `x`와 `y`가 비교될 수 있는 유일한 공통 타입이 `string`이므로, 첫 번째 분기에서 둘 다 `string`으로 확정

- 특정 리터럴 값(변수와 대비)에 대한 검사도 동일하게 작동

```ts twoslash
function printAll(strs: string | string[] | null) {
  if (strs !== null) {
    if (typeof strs === "object") {
      for (const s of strs) {
        //            ^?
        console.log(s);
      }
    } else if (typeof strs === "string") {
      console.log(strs);
      //          ^?
    }
  }
}
```

- JavaScript의 느슨한 동등성 검사 `==`·`!=`도 올바르게 좁혀짐
  - `== null` 검사: `null` 값뿐 아니라 `undefined`인지도 함께 확인
  - `== undefined`도 동일

```ts twoslash
interface Container {
  value: number | null | undefined;
}

function multiplyValue(container: Container, factor: number) {
  // null과 undefined를 타입에서 모두 제거
  if (container.value != null) {
    console.log(container.value);
    //                    ^?

    // 이제 안전하게 'container.value'를 곱할 수 있음
    container.value *= factor;
  }
}
```

#### `in` 연산자 좁히기

- JavaScript에는 객체·프로토타입 체인에 특정 이름의 속성이 있는지 확인하는 `in` 연산자 존재
  - TypeScript는 이를 잠재적인 타입 좁히기 방법으로 고려

```ts twoslash
type Fish = { swim: () => void };
type Bird = { fly: () => void };

function move(animal: Fish | Bird) {
  if ("swim" in animal) {
    return animal.swim();
  }

  return animal.fly();
}
```

- 선택적 속성은 좁히기의 양쪽에 존재
  - 예: 사람은 수영도 하고 날 수도 있으므로(적절한 장비 하에) 양쪽 검사에 모두 나타나야 함

```ts twoslash
type Fish = { swim: () => void };
type Bird = { fly: () => void };
type Human = { swim?: () => void; fly?: () => void };

function move(animal: Fish | Bird | Human) {
  if ("swim" in animal) {
    animal;
//  ^?
  } else {
    animal;
//  ^?
  }
}
```

#### `instanceof` 좁히기

- JavaScript에는 값이 다른 값의 "인스턴스"인지 확인하는 연산자 존재
  - 구체적으로 `x instanceof Foo`는 `x`의 _프로토타입 체인_에 `Foo.prototype`이 포함되어 있는지 확인
- `instanceof`도 타입 가드 - TypeScript는 `instanceof`로 보호되는 분기에서 좁히기 수행

```ts twoslash
function logValue(x: Date | string) {
  if (x instanceof Date) {
    console.log(x.toUTCString());
    //          ^?
  } else {
    console.log(x.toUpperCase());
    //          ^?
  }
}
```

#### 할당

- 변수에 할당할 때 TypeScript는 할당의 오른쪽을 보고 왼쪽을 적절히 좁힘

```ts twoslash
let x = Math.random() < 0.5 ? 10 : "hello world!";
//  ^?
x = 1;

console.log(x);
//          ^?
x = "goodbye!";

console.log(x);
//          ^?
```

- 각 할당은 모두 유효
  - 첫 번째 할당 후 `x`의 관찰된 타입이 `number`로 변경되었지만, 여전히 `x`에 `string` 할당이 가능
  - 이유: `x`의 _선언된 타입_(`x`가 시작한 타입)이 `string | number`이며, 할당 가능성은 항상 선언된 타입에 대해 검사되기 때문

### 제어 흐름 분석

- 지금까지 특정 분기 내에서 TypeScript가 좁히는 기본 예제를 확인
  - 다만 단순히 모든 변수를 훑으며 `if`, `while`, 조건문 등에서 타입 가드를 찾는 것 이상의 작업이 이루어짐

```ts twoslash
function padLeft(padding: number | string, input: string) {
  if (typeof padding === "number") {
    return " ".repeat(padding) + input;
  }
  return padding + input;
}
```

- `padLeft`는 첫 번째 `if` 블록 내에서 반환
  - TypeScript는 이 코드를 분석해, `padding`이 `number`인 경우 본문의 나머지(`return padding + input;`)가 _도달 불가능_함을 확인
  - 결과적으로 함수의 나머지 부분에서 `padding`의 타입에서 `number`를 제거 가능(`string | number`에서 `string`으로 좁히기)

- 도달 가능성에 기반한 이런 코드 분석을 _제어 흐름 분석_이라 부름
  - TypeScript는 타입 가드·할당을 만날 때 이 흐름 분석으로 타입을 좁힘
  - 변수 분석 중 제어 흐름이 분할·재병합될 수 있으며, 해당 변수가 각 지점에서 다른 타입을 가지는 것이 관찰될 수 있음

```ts twoslash
function example() {
  let x: string | number | boolean;

  x = Math.random() < 0.5;

  console.log(x);
  //          ^?

  if (Math.random() < 0.5) {
    x = "hello";
    console.log(x);
    //          ^?
  } else {
    x = 100;
    console.log(x);
    //          ^?
  }

  return x;
  //     ^?
}
```

### 타입 술어 사용하기

- 지금까지 기존 JavaScript 구문으로 좁히기를 다뤘으나, 때로는 코드 전체에서 타입이 어떻게 변경되는지 더 직접적으로 제어하고 싶은 경우가 있음
- 사용자 정의 타입 가드 정의 방법: 반환 타입이 _타입 술어_인 함수를 정의

```ts twoslash
type Fish = { swim: () => void };
type Bird = { fly: () => void };
declare function getSmallPet(): Fish | Bird;
// ---cut---
function isFish(pet: Fish | Bird): pet is Fish {
  return (pet as Fish).swim !== undefined;
}
```

- `pet is Fish`가 이 예제의 타입 술어
  - 술어는 `parameterName is Type` 형태 - `parameterName`은 현재 함수 시그니처의 매개변수 이름이어야 함
- `isFish`가 어떤 변수와 함께 호출될 때마다, TypeScript는 원래 타입이 호환 가능하면 해당 변수를 그 특정 타입으로 _좁힘_

```ts twoslash
type Fish = { swim: () => void };
type Bird = { fly: () => void };
declare function getSmallPet(): Fish | Bird;
function isFish(pet: Fish | Bird): pet is Fish {
  return (pet as Fish).swim !== undefined;
}
// ---cut---
// swim과 fly 호출 모두 이제 괜찮음
let pet = getSmallPet();

if (isFish(pet)) {
  pet.swim();
} else {
  pet.fly();
}
```

- TypeScript는 `if` 분기에서 `pet`이 `Fish`임을 알 뿐만 아니라, `else` 분기에서 `Fish`가 _없다_는 것을 알기에 `Bird`여야 한다는 점까지 파악

- `isFish` 타입 가드로 `Fish | Bird` 배열을 필터링해 `Fish` 배열을 얻는 예시

```ts twoslash
type Fish = { swim: () => void; name: string };
type Bird = { fly: () => void; name: string };
declare function getSmallPet(): Fish | Bird;
function isFish(pet: Fish | Bird): pet is Fish {
  return (pet as Fish).swim !== undefined;
}
// ---cut---
const zoo: (Fish | Bird)[] = [getSmallPet(), getSmallPet(), getSmallPet()];
const underWater1: Fish[] = zoo.filter(isFish);
// 또는, 동등하게
const underWater2: Fish[] = zoo.filter(isFish) as Fish[];

// 더 복잡한 예제에서는 술어를 반복해야 할 수 있음
const underWater3: Fish[] = zoo.filter((pet): pet is Fish => {
  if (pet.name === "sharkey") return false;
  return isFish(pet);
});
```

### 단언 함수

- 타입은 [단언 함수](https://www.typescriptlang.org/docs/handbook/release-notes/typescript-3-7.html#assertion-functions)로도 좁힐 수 있음

### 판별 유니온

- 지금까지 살펴본 대부분의 예제는 `string`, `boolean`, `number` 같은 간단한 타입으로 단일 변수를 좁히는 데 집중
  - 흔한 패턴이지만, JavaScript에서는 대부분 조금 더 복잡한 구조를 다루게 됨

- 예시 상황: 원과 사각형 같은 도형 인코딩
  - 원은 반지름 추적, 사각형은 변 길이 추적
  - 어떤 도형인지 알려주는 `kind` 필드 사용
  - `Shape` 정의 첫 시도:

```ts twoslash
interface Shape {
  kind: "circle" | "square";
  radius?: number;
  sideLength?: number;
}
```

- 문자열 리터럴 타입의 유니온 사용 중 - `"circle"`과 `"square"`로 원인지 사각형인지 구분
  - `string` 대신 `"circle" | "square"` 사용 → 철자 오류 문제 방지

```ts twoslash
// @errors: 2367
interface Shape {
  kind: "circle" | "square";
  radius?: number;
  sideLength?: number;
}

// ---cut---
function handleShape(shape: Shape) {
  // 이런!
  if (shape.kind === "rect") {
    // ...
  }
}
```

- 원·사각형에 따라 올바른 로직을 적용하는 `getArea` 함수 작성 시도 - 먼저 원 처리

```ts twoslash
// @errors: 2532
interface Shape {
  kind: "circle" | "square";
  radius?: number;
  sideLength?: number;
}

// ---cut---
function getArea(shape: Shape) {
  return Math.PI * shape.radius ** 2;
}
```

- `strictNullChecks`에서 오류 발생 - `radius`가 정의되지 않았을 수 있어 적절한 반응
  - `kind` 속성에 적절한 검사를 추가하면 어떨지 확인

```ts twoslash
// @errors: 2532
interface Shape {
  kind: "circle" | "square";
  radius?: number;
  sideLength?: number;
}

// ---cut---
function getArea(shape: Shape) {
  if (shape.kind === "circle") {
    return Math.PI * shape.radius ** 2;
  }
}
```

- TypeScript는 여전히 여기서 무엇을 해야 할지 모르는 상태
  - 타입 검사기보다 값에 대해 더 많이 아는 지점에 도달
  - 널 아님 단언(`shape.radius` 뒤에 `!`)으로 `radius`가 확실히 존재한다고 표현 가능

```ts twoslash
interface Shape {
  kind: "circle" | "square";
  radius?: number;
  sideLength?: number;
}

// ---cut---
function getArea(shape: Shape) {
  if (shape.kind === "circle") {
    return Math.PI * shape.radius! ** 2;
  }
}
```

- 다만 이는 이상적이지 않음
  - 타입 검사기에 `shape.radius`가 정의되어 있다고 말하려면 널 아님 단언(`!`)이 필요했으나, 코드를 이동하기 시작하면 이런 단언은 오류가 발생하기 쉬움
  - 또한 `strictNullChecks` 외부에서는 해당 필드에 실수로 접근할 수 있음(선택적 속성은 읽을 때 항상 존재한다고 가정되기 때문)
  - 개선이 필요한 지점

- 이 `Shape` 인코딩의 문제: 타입 검사기가 `radius`·`sideLength`가 `kind` 속성 기반으로 존재하는지 알 방법이 없음
  - 우리가 아는 것을 타입 검사기에 전달 필요
  - `Shape` 재정의 시도:

```ts twoslash
interface Circle {
  kind: "circle";
  radius: number;
}

interface Square {
  kind: "square";
  sideLength: number;
}

type Shape = Circle | Square;
```

- 여기서 `Shape`를 `kind` 속성 값이 다른 두 타입으로 적절히 분리 - `radius`·`sideLength`는 각 타입에서 필수 속성으로 선언

- `Shape`의 `radius`에 직접 접근하려고 할 때의 결과

```ts twoslash
// @errors: 2339
interface Circle {
  kind: "circle";
  radius: number;
}

interface Square {
  kind: "square";
  sideLength: number;
}

type Shape = Circle | Square;

// ---cut---
function getArea(shape: Shape) {
  return Math.PI * shape.radius ** 2;
}
```

- 첫 번째 `Shape` 정의와 마찬가지로 여전히 오류
  - `radius`가 선택적일 때는 `strictNullChecks` 활성화 시에만 오류가 발생했으나, 이제 `Shape`가 유니온이므로 TypeScript는 `shape`이 `Square`일 수 있고 `Square`에는 `radius`가 없다고 판단
  - 두 해석 모두 올바르지만, `Shape`의 유니온 인코딩은 `strictNullChecks` 설정과 무관하게 오류를 일으킴

- `kind` 속성을 다시 확인하면 어떻게 되는지 확인

```ts twoslash
interface Circle {
  kind: "circle";
  radius: number;
}

interface Square {
  kind: "square";
  sideLength: number;
}

type Shape = Circle | Square;

// ---cut---
function getArea(shape: Shape) {
  if (shape.kind === "circle") {
    return Math.PI * shape.radius ** 2;
    //               ^?
  }
}
```

- 오류가 사라짐
  - 유니온의 모든 타입이 리터럴 타입을 가진 공통 속성을 포함하면, TypeScript는 이를 _판별 유니온_으로 간주하고 멤버를 좁힐 수 있음
  - 이 경우 `kind`가 그 공통 속성(= `Shape`의 _판별_ 속성)
  - `kind`가 `"circle"`인지 확인 → `kind`가 `"circle"`이 아닌 `Shape`의 모든 타입이 제거됨 → `shape`이 `Circle` 타입으로 좁혀짐

- 동일한 검사가 `switch` 문에서도 작동 - 성가신 `!` 널 아님 단언 없이 완전한 `getArea` 작성 가능

```ts twoslash
interface Circle {
  kind: "circle";
  radius: number;
}

interface Square {
  kind: "square";
  sideLength: number;
}

type Shape = Circle | Square;

// ---cut---
function getArea(shape: Shape) {
  switch (shape.kind) {
    case "circle":
      return Math.PI * shape.radius ** 2;
      //               ^?
    case "square":
      return shape.sideLength ** 2;
      //     ^?
  }
}
```

- 핵심은 `Shape`의 인코딩 방식
  - TypeScript에 올바른 정보(= `Circle`과 `Square`가 특정 `kind` 필드를 가진 두 개의 별개 타입이라는 사실)를 전달하는 것이 중요
  - 이렇게 하면 어차피 작성했을 것과 다르지 않은 타입 안전 TypeScript 코드 작성 가능
  - 그 결과 타입 시스템이 "올바른" 일을 수행 - `switch` 문의 각 분기에서 타입을 알아냄

> 참고로, 위 예제에서 반환 타입 추론이 어떻게 `switch` 문의 다른 분기에서 결정된 반환 타입을 통해 작동하는지 살펴보세요.

- 판별 유니온의 활용 범위: 원·사각형 이야기를 넘어섬
  - 네트워크를 통한 메시지 전송(클라이언트/서버 통신), 상태 관리 프레임워크의 뮤테이션 인코딩 등 JavaScript의 모든 종류의 메시징 스키마 표현에 유용

### `never` 타입

- 좁히기 과정에서 유니온의 옵션을 모든 가능성을 제거해 아무것도 남지 않는 지점까지 줄일 수 있음
  - 이런 경우 TypeScript는 존재해서는 안 되는 상태를 나타내기 위해 `never` 타입을 사용

### 철저한 검사

- `never` 타입은 모든 타입에 할당 가능하나, 어떤 타입도 `never`에 할당 불가(`never` 자체는 예외)
  - 이 특성으로 `switch` 문에서 좁히기와 `never`를 활용한 철저한 검사가 가능

- 예: 모든 가능한 케이스가 처리되었을 때 `shape`에 `never`를 할당하려는 `getArea` 함수에 `default`를 추가해도 오류가 발생하지 않음

```ts twoslash
interface Circle {
  kind: "circle";
  radius: number;
}

interface Square {
  kind: "square";
  sideLength: number;
}
// ---cut---
type Shape = Circle | Square;

function getArea(shape: Shape) {
  switch (shape.kind) {
    case "circle":
      return Math.PI * shape.radius ** 2;
    case "square":
      return shape.sideLength ** 2;
    default:
      const _exhaustiveCheck: never = shape;
      return _exhaustiveCheck;
  }
}
```

- `Shape` 유니온에 새 멤버를 추가하면 TypeScript 오류 발생

```ts twoslash
// @errors: 2322
interface Circle {
  kind: "circle";
  radius: number;
}

interface Square {
  kind: "square";
  sideLength: number;
}

interface Triangle {
  kind: "triangle";
  sideLength: number;
}

type Shape = Circle | Square | Triangle;

function getArea(shape: Shape) {
  switch (shape.kind) {
    case "circle":
      return Math.PI * shape.radius ** 2;
    case "square":
      return shape.sideLength ** 2;
    default:
      const _exhaustiveCheck: never = shape;
      return _exhaustiveCheck;
  }
}
```

- 이 오류는 `Triangle`이 아직 처리되지 않았음을 상기시켜주므로 유용

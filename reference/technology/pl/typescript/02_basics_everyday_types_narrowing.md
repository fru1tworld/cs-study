# 기본 사항 · 일상적인 타입 · 내로잉

# 기본 사항

> **원문:** https://www.typescriptlang.org/docs/handbook/2/basic-types.html

JavaScript의 모든 값은 다양한 연산을 실행하여 관찰할 수 있는 일련의 동작이 있습니다.
이것이 추상적으로 들릴 수 있지만, 간단한 예로 `message`라는 변수에 대해 실행할 수 있는 몇 가지 연산을 고려해 보겠습니다.

```js
// 'message'의 'toLowerCase' 속성에 접근한 다음
// 호출합니다
message.toLowerCase();

// 'message'를 호출합니다
message();
```

이것을 분석해 보면, 첫 번째 실행 가능한 코드 줄은 `toLowerCase`라는 속성에 접근한 다음 호출합니다.
두 번째 줄은 `message`를 직접 호출하려고 합니다.

하지만 `message`의 값을 모른다고 가정하면 - 이것은 매우 일반적입니다 - 이 코드를 실행하려고 할 때 어떤 결과를 얻을지 확실하게 말할 수 없습니다.
각 연산의 동작은 전적으로 처음에 어떤 값을 가졌느냐에 달려 있습니다.

- `message`는 호출 가능한가?
- `toLowerCase`라는 속성이 있는가?
- 있다면, `toLowerCase`는 호출 가능한가?
- 이 두 값이 모두 호출 가능하다면, 무엇을 반환하는가?

이러한 질문에 대한 답은 보통 JavaScript를 작성할 때 머릿속에 담아두는 것들이며, 모든 세부 사항을 제대로 이해했기를 바라야 합니다.

`message`가 다음과 같이 정의되었다고 가정해 봅시다.

```js
const message = "Hello World!";
```

아마도 짐작할 수 있듯이, `message.toLowerCase()`를 실행하려고 하면 동일한 문자열이 소문자로만 반환됩니다.

두 번째 코드 줄은 어떨까요?
JavaScript에 익숙하다면, 이것이 예외와 함께 실패한다는 것을 알 것입니다:

```txt
TypeError: message is not a function
```

이런 실수를 피할 수 있다면 좋겠습니다.

코드를 실행할 때, JavaScript 런타임이 무엇을 할지 선택하는 방법은 값의 _타입_ - 어떤 종류의 동작과 기능이 있는지 - 을 파악하는 것입니다.
이것이 그 `TypeError`가 암시하는 것의 일부입니다 - 문자열 `"Hello World!"`는 함수로 호출할 수 없다고 말하고 있습니다.

`string`과 `number`와 같은 기본형에 대해서는 `typeof` 연산자를 사용하여 런타임에 타입을 식별할 수 있습니다.
하지만 함수와 같은 다른 것들에 대해서는 타입을 식별할 수 있는 해당하는 런타임 메커니즘이 없습니다.
예를 들어, 이 함수를 고려해 보세요:

```js
function fn(x) {
  return x.flip();
}
```

코드를 읽어보면 이 함수가 호출 가능한 `flip` 속성을 가진 객체가 주어진 경우에만 작동한다는 것을 _관찰_할 수 있지만, JavaScript는 코드가 실행되는 동안 확인할 수 있는 방식으로 이 정보를 표면화하지 않습니다.
순수 JavaScript에서 `fn`이 특정 값으로 무엇을 하는지 알 수 있는 유일한 방법은 호출하고 무슨 일이 일어나는지 보는 것입니다.
이런 종류의 동작은 코드가 실행되기 전에 무엇을 할지 예측하기 어렵게 만들며, 이는 코드를 작성하는 동안 코드가 무엇을 할지 알기 어렵다는 것을 의미합니다.

이런 방식으로 보면, _타입_은 어떤 값이 `fn`에 전달될 수 있고 어떤 값이 충돌을 일으킬지 설명하는 개념입니다.
JavaScript는 오직 _동적_ 타이핑만 제공합니다 - 코드를 실행하여 무슨 일이 일어나는지 보는 것입니다.

대안은 _정적_ 타입 시스템을 사용하여 코드가 실행되기 _전에_ 예상되는 것을 예측하는 것입니다.

## 정적 타입 검사

문자열을 함수로 호출하려고 할 때 발생했던 `TypeError`를 다시 생각해 보세요.
_대부분의 사람들_은 코드를 실행할 때 어떤 종류의 오류도 받기 싫어합니다 - 이것들은 버그로 간주됩니다!
그리고 새 코드를 작성할 때, 우리는 새로운 버그를 도입하지 않기 위해 최선을 다합니다.

약간의 코드를 추가하고, 파일을 저장하고, 코드를 다시 실행하고, 즉시 오류를 보면, 문제를 빠르게 분리할 수 있을 것입니다; 하지만 항상 그런 것은 아닙니다.
기능을 충분히 철저하게 테스트하지 않아서, 발생할 수 있는 잠재적 오류를 실제로 만나지 못할 수도 있습니다!
또는 운이 좋게 오류를 목격했더라도, 대규모 리팩토링을 하고 파헤쳐야 하는 많은 다른 코드를 추가했을 수도 있습니다.

이상적으로는, 코드가 실행되기 _전에_ 이러한 버그를 찾는 데 도움이 되는 도구가 있을 수 있습니다.
이것이 TypeScript와 같은 정적 타입 검사기가 하는 일입니다.
_정적 타입 시스템_은 프로그램을 실행할 때 값이 가질 형태와 동작을 설명합니다.
TypeScript와 같은 타입 검사기는 그 정보를 사용하여 일이 잘못될 수 있을 때 알려줍니다.

```ts twoslash
// @errors: 2349
const message = "hello!";

message();
```

TypeScript로 마지막 샘플을 실행하면 코드를 처음 실행하기 전에 오류 메시지를 받게 됩니다.

## 예외를 던지지 않는 실패

지금까지 런타임 오류와 같은 특정 사항에 대해 논의해 왔습니다 - JavaScript 런타임이 무언가가 말이 안 된다고 생각하여 알려주는 경우입니다.
이러한 경우는 [ECMAScript 명세](https://tc39.github.io/ecma262/)에 언어가 예상치 못한 것을 만났을 때 어떻게 동작해야 하는지에 대한 명시적인 지침이 있기 때문에 발생합니다.

예를 들어, 명세는 호출할 수 없는 것을 호출하려고 하면 오류를 발생시켜야 한다고 말합니다.
아마도 "당연한 동작"처럼 들리겠지만, 객체에 존재하지 않는 속성에 접근하는 것도 오류를 발생시켜야 한다고 상상할 수 있습니다.
대신, JavaScript는 다른 동작을 제공하고 `undefined` 값을 반환합니다:

```js
const user = {
  name: "Daniel",
  age: 26,
};

user.location; // undefined를 반환
```

궁극적으로, 정적 타입 시스템은 "유효한" JavaScript로서 즉시 오류를 발생시키지 않더라도, 시스템에서 어떤 코드가 오류로 표시되어야 하는지 결정해야 합니다.
TypeScript에서, 다음 코드는 `location`이 정의되지 않았다는 오류를 생성합니다:

```ts twoslash
// @errors: 2339
const user = {
  name: "Daniel",
  age: 26,
};

user.location;
```

때때로 이것이 표현할 수 있는 것에서 트레이드오프를 의미하지만, 의도는 프로그램에서 합법적인 버그를 잡는 것입니다.
그리고 TypeScript는 _많은_ 합법적인 버그를 잡습니다.

예를 들어: 오타,

```ts twoslash
// @noErrors
const announcement = "Hello World!";

// 오타를 얼마나 빨리 발견할 수 있나요?
announcement.toLocaleLowercase();
announcement.toLocalLowerCase();

// 아마도 이것을 쓰려고 했을 것입니다...
announcement.toLocaleLowerCase();
```

호출되지 않은 함수,

```ts twoslash
// @noUnusedLocals
// @errors: 2365
function flipCoin() {
  // Math.random()이어야 했음
  return Math.random < 0.5;
}
```

또는 기본 논리 오류.

```ts twoslash
// @errors: 2367
const value = Math.random() < 0.5 ? "a" : "b";
if (value !== "a") {
  // ...
} else if (value === "b") {
  // 이런, 도달 불가능
}
```

## 도구를 위한 타입

TypeScript는 코드에서 실수를 할 때 버그를 잡을 수 있습니다.
좋습니다, 하지만 TypeScript는 또한 처음부터 그러한 실수를 하는 것을 _방지_할 수 있습니다.

타입 검사기는 변수 및 기타 속성에서 올바른 속성에 접근하고 있는지 확인할 정보가 있습니다.
그 정보를 가지면, 사용하고 싶을 수 있는 속성을 _제안_하기 시작할 수도 있습니다.

이는 TypeScript를 코드 편집에도 활용할 수 있다는 것을 의미하며, 핵심 타입 검사기는 에디터에서 입력할 때 오류 메시지와 코드 완성을 제공할 수 있습니다.
이것이 사람들이 TypeScript의 도구에 대해 이야기할 때 자주 언급하는 것의 일부입니다.

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

TypeScript는 도구를 진지하게 다루며, 이는 입력할 때의 완성과 오류를 넘어섭니다.
TypeScript를 지원하는 에디터는 오류를 자동으로 수정하는 "빠른 수정", 코드를 쉽게 재구성하는 리팩토링, 변수의 정의로 이동하거나 주어진 변수에 대한 모든 참조를 찾는 유용한 탐색 기능을 제공할 수 있습니다.
이 모든 것은 타입 검사기 위에 구축되어 있으며 완전히 크로스 플랫폼이므로, [좋아하는 에디터에 TypeScript 지원이 있을](https://github.com/Microsoft/TypeScript/wiki/TypeScript-Editor-Support) 가능성이 높습니다.

## `tsc`, TypeScript 컴파일러

타입 검사에 대해 이야기해 왔지만, 아직 타입 _검사기_를 사용하지 않았습니다.
새로운 친구 `tsc`, TypeScript 컴파일러와 친해집시다.
먼저 npm을 통해 설치해야 합니다.

```sh
npm install -g typescript
```

> 이것은 TypeScript 컴파일러 `tsc`를 전역으로 설치합니다.
> 로컬 `node_modules` 패키지에서 `tsc`를 실행하려면 `npx`나 유사한 도구를 사용할 수 있습니다.

이제 빈 폴더로 이동하여 첫 번째 TypeScript 프로그램 `hello.ts`를 작성해 봅시다:

```ts twoslash
// 세계에 인사합니다.
console.log("Hello world!");
```

여기에는 장식이 없습니다; 이 "hello world" 프로그램은 JavaScript에서 "hello world" 프로그램에 쓸 것과 동일하게 보입니다.
이제 `typescript` 패키지가 설치해 준 `tsc` 명령을 실행하여 타입 검사해 봅시다.

```sh
tsc hello.ts
```

짜잔!

잠깐, 정확히 "짜잔" _뭐_라고요?
`tsc`를 실행했는데 아무 일도 일어나지 않았습니다!
글쎄요, 타입 오류가 없었으므로, 보고할 것이 없어서 콘솔에 출력이 없었습니다.

하지만 다시 확인해 보세요 - 대신 _파일_ 출력을 받았습니다.
현재 디렉터리를 보면, `hello.ts` 옆에 `hello.js` 파일이 있을 것입니다.
이것은 `tsc`가 `.ts` 파일을 일반 JavaScript 파일로 _컴파일_하거나 _변환_한 후 `hello.ts` 파일의 출력입니다.
그리고 내용을 확인하면, TypeScript가 `.ts` 파일을 처리한 후 내보내는 것을 볼 수 있습니다:

```js
// 세계에 인사합니다.
console.log("Hello world!");
```

이 경우, TypeScript가 변환할 것이 거의 없었으므로, 우리가 쓴 것과 동일하게 보입니다.
컴파일러는 사람이 쓸 것 같은 깔끔하고 읽기 쉬운 코드를 내보내려고 합니다.
항상 그렇게 쉽지는 않지만, TypeScript는 일관되게 들여쓰기하고, 코드가 다른 줄에 걸쳐 있을 때를 신경 쓰며, 주석을 유지하려고 합니다.

타입 검사 오류를 _도입하면_ 어떨까요?
`hello.ts`를 다시 작성합시다:

```ts twoslash
// @noErrors
// 이것은 산업용 범용 인사 함수입니다:
function greet(person, date) {
  console.log(`Hello ${person}, today is ${date}!`);
}

greet("Brendan");
```

`tsc hello.ts`를 다시 실행하면, 명령줄에서 오류가 발생합니다!

```txt
Expected 2 arguments, but got 1.
```

TypeScript는 `greet` 함수에 인수를 전달하는 것을 잊었다고 알려주며, 당연히 그렇습니다.
지금까지 표준 JavaScript만 작성했지만, 타입 검사는 여전히 코드의 문제를 찾을 수 있었습니다.
TypeScript 감사합니다!

## 오류가 있어도 내보내기

마지막 예제에서 눈치채지 못했을 수 있는 한 가지는 `hello.js` 파일이 다시 변경되었다는 것입니다.
그 파일을 열어보면 내용이 기본적으로 입력 파일과 동일하게 보이는 것을 알 수 있습니다.
`tsc`가 코드에 대한 오류를 보고했다는 사실을 고려하면 이것은 약간 놀라울 수 있지만, 이것은 TypeScript의 핵심 가치 중 하나에 기반합니다: 대부분의 경우, _당신_이 TypeScript보다 더 잘 알 것입니다.

앞서 다시 말하자면, 타입 검사 코드는 실행할 수 있는 프로그램의 종류를 제한하므로, 타입 검사기가 허용 가능하다고 찾는 것들에 대한 트레이드오프가 있습니다.
대부분의 경우 괜찮지만, 이러한 검사가 방해가 되는 시나리오가 있습니다.
예를 들어, JavaScript 코드를 TypeScript로 마이그레이션하면서 타입 검사 오류를 도입한다고 상상해 보세요.
결국 타입 검사기를 위해 정리하게 되겠지만, 원래 JavaScript 코드는 이미 작동하고 있었습니다!
왜 TypeScript로 변환하면 실행이 중지되어야 합니까?

그래서 TypeScript는 방해하지 않습니다.
물론, 시간이 지나면서 실수에 대해 좀 더 방어적이 되고 싶을 수 있으며, TypeScript가 좀 더 엄격하게 행동하도록 만들 수 있습니다.
그 경우, [`noEmitOnError`](/tsconfig#noEmitOnError) 컴파일러 옵션을 사용할 수 있습니다.
`hello.ts` 파일을 변경하고 해당 플래그와 함께 `tsc`를 실행해 보세요:

```sh
tsc --noEmitOnError hello.ts
```

`hello.js`가 업데이트되지 않는 것을 알 수 있습니다.

## 명시적 타입

지금까지 TypeScript에게 `person`이나 `date`가 무엇인지 알려주지 않았습니다.
코드를 편집하여 TypeScript에게 `person`이 `string`이고, `date`가 `Date` 객체여야 한다고 알려줍시다.
또한 `date`에 `toDateString()` 메서드를 사용합니다.

```ts twoslash
function greet(person: string, date: Date) {
  console.log(`Hello ${person}, today is ${date.toDateString()}!`);
}
```

우리가 한 것은 `person`과 `date`에 _타입 어노테이션_을 추가하여 `greet`가 어떤 타입의 값으로 호출될 수 있는지 설명한 것입니다.
그 시그니처를 "`greet`는 `string` 타입의 `person`과 `Date` 타입의 `date`를 받는다"라고 읽을 수 있습니다.

이것으로, TypeScript는 `greet`가 잘못 호출되었을 수 있는 다른 경우에 대해 알려줄 수 있습니다.
예를 들어...

```ts twoslash
// @errors: 2345
function greet(person: string, date: Date) {
  console.log(`Hello ${person}, today is ${date.toDateString()}!`);
}

greet("Maddison", Date());
```

응?
TypeScript가 두 번째 인수에 대해 오류를 보고했는데, 왜죠?

아마도 놀랍게도, JavaScript에서 `Date()`를 호출하면 `string`이 반환됩니다.
반면에, `new Date()`로 `Date`를 생성하면 실제로 우리가 기대했던 것을 얻습니다.

어쨌든, 오류를 빠르게 수정할 수 있습니다:

```ts twoslash {4}
function greet(person: string, date: Date) {
  console.log(`Hello ${person}, today is ${date.toDateString()}!`);
}

greet("Maddison", new Date());
```

명시적 타입 어노테이션을 항상 작성할 필요는 없다는 것을 명심하세요.
많은 경우, TypeScript는 타입을 생략해도 타입을 _추론_(또는 "파악")할 수 있습니다.

```ts twoslash
let msg = "hello there!";
//  ^?
```

TypeScript에게 `msg`가 `string` 타입이라고 알려주지 않았지만 그것을 알아낼 수 있었습니다.
이것은 기능이며, 타입 시스템이 어차피 같은 타입을 추론할 때는 어노테이션을 추가하지 않는 것이 가장 좋습니다.

> 참고: 이전 코드 샘플 내의 메시지 버블은 해당 단어 위에 마우스를 올렸을 때 에디터가 표시하는 것입니다.

## 지워지는 타입

위의 함수 `greet`를 `tsc`로 컴파일하여 JavaScript를 출력할 때 무슨 일이 일어나는지 살펴봅시다:

```ts twoslash
// @showEmit
// @target: es5
function greet(person: string, date: Date) {
  console.log(`Hello ${person}, today is ${date.toDateString()}!`);
}

greet("Maddison", new Date());
```

여기서 두 가지를 주목하세요:

1. `person`과 `date` 매개변수에 더 이상 타입 어노테이션이 없습니다.
2. "템플릿 문자열" - 백틱(`` ` `` 문자)을 사용한 문자열 - 이 연결이 있는 일반 문자열로 변환되었습니다.

두 번째 점에 대해서는 나중에 더 설명하고, 이제 첫 번째 점에 집중합시다.
타입 어노테이션은 JavaScript(또는 정확히 말하면 ECMAScript)의 일부가 아니므로, 수정 없이 TypeScript를 실행할 수 있는 브라우저나 기타 런타임이 없습니다.
그래서 TypeScript는 처음에 컴파일러가 필요합니다 - TypeScript 전용 코드를 제거하거나 변환하여 실행할 수 있는 방법이 필요합니다.
대부분의 TypeScript 전용 코드는 지워지며, 마찬가지로 여기서 타입 어노테이션은 완전히 지워졌습니다.

> **기억하세요**: 타입 어노테이션은 프로그램의 런타임 동작을 절대 변경하지 않습니다.

## 다운레벨링

위에서 또 다른 차이점은 템플릿 문자열이

```js
`Hello ${person}, today is ${date.toDateString()}!`;
```

에서

```js
"Hello ".concat(person, ", today is ").concat(date.toDateString(), "!");
```

로 다시 작성되었다는 것입니다.

왜 이런 일이 발생했을까요?

템플릿 문자열은 ECMAScript 2015(일명 ECMAScript 6, ES2015, ES6 등 - _물어보지 마세요_)라는 ECMAScript 버전의 기능입니다.
TypeScript는 코드를 ECMAScript 3이나 ECMAScript 5(일명 ES5)와 같은 더 새로운 버전의 ECMAScript에서 더 오래된 버전으로 다시 작성하는 기능이 있습니다.
더 새롭거나 "더 높은" 버전의 ECMAScript에서 더 오래되거나 "더 낮은" 버전으로 이동하는 이 과정을 때때로 _다운레벨링_이라고 합니다.

기본적으로 TypeScript는 매우 오래된 ECMAScript 버전인 ES5를 대상으로 합니다.
[`target`](/tsconfig#target) 옵션을 사용하여 조금 더 최근의 것을 선택할 수 있었습니다.
`--target es2015`로 실행하면 TypeScript가 ECMAScript 2015를 대상으로 변경되어, ECMAScript 2015가 지원되는 곳이면 어디서든 코드가 실행될 수 있어야 합니다.
따라서 `tsc --target es2015 hello.ts`를 실행하면 다음 출력을 제공합니다:

```js
function greet(person, date) {
  console.log(`Hello ${person}, today is ${date.toDateString()}!`);
}
greet("Maddison", new Date());
```

> 기본 대상이 ES5이지만, 현재 브라우저의 대다수가 ES2015를 지원합니다.
> 따라서 대부분의 개발자는 특정 고대 브라우저와의 호환성이 중요하지 않는 한 ES2015 이상을 대상으로 안전하게 지정할 수 있습니다.

## 엄격성

다양한 사용자들이 타입 검사기에서 다양한 것을 찾으며 TypeScript에 옵니다.
어떤 사람들은 프로그램의 일부만 검증하는 데 도움이 되면서도 괜찮은 도구를 가질 수 있는 더 느슨한 옵트인 경험을 찾고 있습니다.
이것이 TypeScript의 기본 경험이며, 타입이 선택적이고, 추론이 가장 관대한 타입을 취하고, 잠재적으로 `null`/`undefined` 값에 대한 검사가 없습니다.
`tsc`가 오류가 있어도 내보내는 것과 마찬가지로, 이러한 기본값은 방해가 되지 않도록 설정됩니다.
기존 JavaScript를 마이그레이션하는 경우, 이것이 바람직한 첫 단계일 수 있습니다.

대조적으로, 많은 사용자들은 TypeScript가 가능한 한 많이 즉시 검증하는 것을 선호하며, 그래서 언어는 엄격성 설정도 제공합니다.
이러한 엄격성 설정은 정적 타입 검사를 스위치(코드가 검사되거나 되지 않거나)에서 다이얼에 더 가까운 것으로 바꿉니다.
이 다이얼을 더 높이 돌릴수록, TypeScript가 더 많이 검사합니다.
이것은 약간의 추가 작업이 필요할 수 있지만, 일반적으로 장기적으로 그만한 가치가 있으며, 더 철저한 검사와 더 정확한 도구를 가능하게 합니다.
가능한 경우, 새 코드베이스는 항상 이러한 엄격성 검사를 켜야 합니다.

TypeScript에는 켜거나 끌 수 있는 여러 타입 검사 엄격성 플래그가 있으며, 달리 명시되지 않는 한 모든 예제는 모두 활성화된 상태로 작성됩니다.
CLI의 [`strict`](/tsconfig#strict) 플래그 또는 [`tsconfig.json`](https://www.typescriptlang.org/docs/handbook/tsconfig-json.html)의 `"strict": true`는 모두 동시에 켜지만, 개별적으로 옵트아웃할 수 있습니다.
알아야 할 가장 큰 두 가지는 [`noImplicitAny`](/tsconfig#noImplicitAny)와 [`strictNullChecks`](/tsconfig#strictNullChecks)입니다.

## `noImplicitAny`

일부 장소에서 TypeScript는 타입을 추론하려고 하지 않고 대신 가장 관대한 타입인 `any`로 대체한다는 것을 기억하세요.
이것이 일어날 수 있는 최악의 일은 아닙니다 - 결국, `any`로 대체하는 것은 어차피 일반 JavaScript 경험입니다.

그러나, `any`를 사용하면 처음에 TypeScript를 사용하는 목적이 무색해지는 경우가 많습니다.
프로그램이 더 많이 타입화될수록, 더 많은 검증과 도구를 얻을 수 있으며, 이는 코딩할 때 더 적은 버그를 만나게 된다는 것을 의미합니다.
[`noImplicitAny`](/tsconfig#noImplicitAny) 플래그를 켜면 타입이 암시적으로 `any`로 추론되는 모든 변수에 대해 오류가 발생합니다.

## `strictNullChecks`

기본적으로, `null`과 `undefined`와 같은 값은 다른 모든 타입에 할당 가능합니다.
이것은 일부 코드를 쓰기 쉽게 만들 수 있지만, `null`과 `undefined`를 처리하는 것을 잊는 것은 세상에서 수많은 버그의 원인입니다 - 일부는 이것을 [십억 달러의 실수](https://www.youtube.com/watch?v=ybrQvs4x0Ps)라고 여깁니다!
[`strictNullChecks`](/tsconfig#strictNullChecks) 플래그는 `null`과 `undefined`를 처리하는 것을 더 명시적으로 만들고, `null`과 `undefined`를 처리하는 것을 _잊었는지_ 걱정하는 것에서 우리를 _해방시킵니다_.

---

# 일상적인 타입

> **원문:** https://www.typescriptlang.org/docs/handbook/2/everyday-types.html

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

TypeScript는 또한 특정 값이 타입 검사 오류를 일으키지 않기를 원할 때 사용할 수 있는 특별한 타입 `any`가 있습니다.

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
유니온 타입의 값이 _있다면_, 어떻게 작업합니까?

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
예를 들어, 배열과 문자열 모두 `slice` 메서드가 있습니다.
유니온의 모든 멤버가 공통 속성이 있다면, 좁히기 없이 해당 속성을 사용할 수 있습니다:

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
TypeScript는 `printCoord`에 전달한 값의 _구조_에만 관심이 있습니다 - 예상되는 속성이 있는지만 신경 씁니다.
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

때때로 TypeScript가 알 수 없는 값의 타입에 대한 정보가 있을 것입니다.

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

이것에 대해 생각하는 한 가지 방법은 JavaScript가 변수를 선언하는 다양한 방법이 있다는 것을 고려하는 것입니다. `var`와 `let` 모두 변수 내부에 있는 것을 변경할 수 있도록 허용하고, `const`는 허용하지 않습니다. 이것은 TypeScript가 리터럴에 대한 타입을 만드는 방법에 반영됩니다.

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

TypeScript는 또한 명시적 검사 없이 타입에서 `null`과 `undefined`를 제거하는 특별한 구문이 있습니다.
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

---

# 좁히기

> **원문:** https://www.typescriptlang.org/docs/handbook/2/narrowing.html

좁히기는 TypeScript가 처음에 선언된 것보다 더 구체적인 타입으로 변수 타입을 정제하는 과정입니다. 이 문서는 타입을 좁히기 위한 다양한 TypeScript 메커니즘을 설명합니다.

## 타입 가드 메서드

### `typeof` 타입 가드

JavaScript의 `typeof` 연산자를 사용하여 기본 타입을 검사할 수 있습니다. 이 연산자는 `"string"`, `"number"`, `"boolean"` 등의 문자열을 반환합니다.

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

TypeScript는 `typeof`가 반환하는 다양한 값을 이해합니다:
- `"string"`
- `"number"`
- `"bigint"`
- `"boolean"`
- `"symbol"`
- `"undefined"`
- `"object"`
- `"function"`

JavaScript의 특이한 점으로 인해 `typeof null`은 실제로 `"object"`를 반환한다는 점을 주의하세요. 이것은 JavaScript의 오래된 버그입니다.

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

### 참 같은 값 좁히기

JavaScript에서 조건문, `&&`, `||`, `if` 문, 불리언 부정(`!`) 등에서 모든 표현식을 사용할 수 있습니다. 예를 들어, `if` 문은 조건이 항상 `boolean` 타입일 것을 기대하지 않습니다.

JavaScript에서 `if`와 같은 구문은 먼저 조건을 `boolean`으로 "강제 변환"하여 이해한 다음, 결과가 `true`인지 `false`인지에 따라 분기를 선택합니다.

다음 값들은 `false`로 강제 변환됩니다:
- `0`
- `NaN`
- `""` (빈 문자열)
- `0n` (bigint 버전의 0)
- `null`
- `undefined`

다른 값들은 `true`로 강제 변환됩니다. `Boolean` 함수를 통해 값을 실행하거나, 짧은 이중 불리언 부정을 사용하여 언제든 `boolean`으로 강제 변환할 수 있습니다. (후자는 TypeScript가 좁은 리터럴 불리언 타입 `true`를 추론하고, 첫 번째는 `boolean` 타입으로 추론한다는 장점이 있습니다.)

```ts twoslash
// 이 두 결과 모두 'true'
Boolean("hello"); // 타입: boolean, 값: true
!!"world";        // 타입: true,    값: true
```

이 동작을 활용하는 것은 꽤 인기가 있으며, 특히 `null`이나 `undefined`와 같은 값에 대한 가드로 사용됩니다.

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

하지만 기본형에 대한 참 같은 값 검사는 종종 오류가 발생하기 쉽습니다. 예를 들어 빈 문자열 `""`은 falsy하므로 의도치 않게 걸러질 수 있습니다.

### 동등성 좁히기

TypeScript는 `===`, `!==`, `==`, `!=`와 같은 동등성 검사를 사용하여 타입을 좁힐 수도 있습니다.

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

`x`와 `y`가 둘 다 같다고 확인했을 때, TypeScript는 두 값의 타입도 같아야 한다는 것을 알았습니다. `x`와 `y`가 비교될 수 있는 유일한 공통 타입은 `string`이므로, 첫 번째 분기에서 `x`와 `y`는 `string`이어야 한다는 것을 알 수 있습니다.

특정 리터럴 값(변수와 반대로)에 대한 검사도 작동합니다.

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

JavaScript의 느슨한 동등성 검사 `==`와 `!=`도 올바르게 좁혀집니다. `== null` 검사는 구체적으로 `null` 값인지만 확인하는 것이 아니라 `undefined`인지도 확인합니다. `== undefined`도 마찬가지입니다.

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

### `in` 연산자 좁히기

JavaScript에는 객체나 프로토타입 체인에 특정 이름의 속성이 있는지 확인하는 연산자 `in`이 있습니다. TypeScript는 이것을 잠재적인 타입을 좁히는 방법으로 고려합니다.

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

선택적 속성은 좁히기의 양쪽에 존재합니다. 예를 들어, 사람은 수영도 하고 날 수도 있으므로(적절한 장비를 가지고) 양쪽 검사에 나타나야 합니다:

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

### `instanceof` 좁히기

JavaScript에는 값이 다른 값의 "인스턴스"인지 확인하는 연산자가 있습니다. 더 구체적으로, JavaScript에서 `x instanceof Foo`는 `x`의 _프로토타입 체인_에 `Foo.prototype`이 포함되어 있는지 확인합니다.

`instanceof`도 타입 가드이며, TypeScript는 `instanceof`로 보호되는 분기에서 좁힙니다.

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

### 할당

앞서 언급했듯이, 변수에 할당할 때 TypeScript는 할당의 오른쪽을 보고 왼쪽을 적절하게 좁힙니다.

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

이러한 각 할당이 유효하다는 점에 주목하세요. 첫 번째 할당 후에 `x`의 관찰된 타입이 `number`로 변경되었지만, 여전히 `x`에 `string`을 할당할 수 있었습니다. 이는 `x`의 _선언된 타입_ - `x`가 시작한 타입 - 이 `string | number`이고, 할당 가능성은 항상 선언된 타입에 대해 검사되기 때문입니다.

## 제어 흐름 분석

지금까지 TypeScript가 특정 분기 내에서 어떻게 좁히는지에 대한 몇 가지 기본 예제를 살펴보았습니다. 하지만 단순히 모든 변수를 올라가면서 `if`, `while`, 조건문 등에서 타입 가드를 찾는 것 이상의 일이 일어나고 있습니다. 예를 들어:

```ts twoslash
function padLeft(padding: number | string, input: string) {
  if (typeof padding === "number") {
    return " ".repeat(padding) + input;
  }
  return padding + input;
}
```

`padLeft`는 첫 번째 `if` 블록 내에서 반환합니다. TypeScript는 이 코드를 분석할 수 있었고, `padding`이 `number`인 경우 본문의 나머지 부분(`return padding + input;`)이 _도달 불가능_하다는 것을 확인할 수 있었습니다. 결과적으로, 함수의 나머지 부분에서 `padding`의 타입에서 `number`를 제거할 수 있었습니다(`string | number`에서 `string`으로 좁히기).

도달 가능성에 기반한 이러한 코드 분석을 _제어 흐름 분석_이라고 하며, TypeScript는 타입 가드와 할당을 만날 때 타입을 좁히기 위해 이 흐름 분석을 사용합니다. 변수가 분석될 때, 제어 흐름이 분할되고 다시 병합될 수 있으며, 해당 변수가 각 지점에서 다른 타입을 가지는 것이 관찰될 수 있습니다.

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

## 타입 술어 사용하기

지금까지 기존 JavaScript 구문으로 좁히기를 다루었지만, 때때로 코드 전체에서 타입이 어떻게 변경되는지에 대해 더 직접적인 제어를 원합니다.

사용자 정의 타입 가드를 정의하려면, 반환 타입이 _타입 술어_인 함수를 정의하면 됩니다:

```ts twoslash
type Fish = { swim: () => void };
type Bird = { fly: () => void };
declare function getSmallPet(): Fish | Bird;
// ---cut---
function isFish(pet: Fish | Bird): pet is Fish {
  return (pet as Fish).swim !== undefined;
}
```

`pet is Fish`는 이 예제에서 타입 술어입니다. 술어는 `parameterName is Type` 형태를 취하며, `parameterName`은 현재 함수 시그니처의 매개변수 이름이어야 합니다.

`isFish`가 어떤 변수와 함께 호출될 때마다, TypeScript는 원래 타입이 호환 가능한 경우 해당 변수를 해당 특정 타입으로 _좁힐_ 것입니다.

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

TypeScript가 `if` 분기에서 `pet`이 `Fish`라는 것을 알 뿐만 아니라, `else` 분기에서 `Fish`가 _없다_는 것을 알고 있으므로 `Bird`가 있어야 한다는 점에 주목하세요.

`isFish` 타입 가드를 사용하여 `Fish | Bird` 배열을 필터링하고 `Fish` 배열을 얻을 수 있습니다:

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

## 단언 함수

타입은 [단언 함수](https://www.typescriptlang.org/docs/handbook/release-notes/typescript-3-7.html#assertion-functions)를 사용하여 좁힐 수도 있습니다.

## 판별 유니온

지금까지 살펴본 대부분의 예제는 `string`, `boolean`, `number`와 같은 간단한 타입으로 단일 변수를 좁히는 것에 집중했습니다. 이것이 일반적이지만, JavaScript에서 대부분의 경우 조금 더 복잡한 구조를 다루게 됩니다.

동기 부여를 위해, 원과 사각형과 같은 도형을 인코딩하려고 한다고 상상해 봅시다. 원은 반지름을 추적하고 사각형은 변 길이를 추적합니다. 어떤 도형을 다루고 있는지 알려주기 위해 `kind`라는 필드를 사용할 것입니다. 다음은 `Shape`를 정의하는 첫 번째 시도입니다.

```ts twoslash
interface Shape {
  kind: "circle" | "square";
  radius?: number;
  sideLength?: number;
}
```

문자열 리터럴 타입의 유니온을 사용하고 있다는 점에 주목하세요: `"circle"`과 `"square"`를 사용하여 도형을 원으로 처리해야 하는지 사각형으로 처리해야 하는지 알려줍니다. `string` 대신 `"circle" | "square"`를 사용하면 철자 오류 문제를 피할 수 있습니다.

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

원을 다루는지 사각형을 다루는지에 따라 올바른 논리를 적용하는 `getArea` 함수를 작성할 수 있습니다. 먼저 원을 다루려고 합니다.

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

`strictNullChecks`에서 오류가 발생합니다 - `radius`가 정의되지 않았을 수 있기 때문에 적절합니다. 하지만 `kind` 속성에 적절한 검사를 수행하면 어떨까요?

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

흠, TypeScript는 여전히 여기서 무엇을 해야 할지 모릅니다. 우리가 타입 검사기보다 값에 대해 더 많이 아는 지점에 도달했습니다. 널 아님 단언(`shape.radius` 뒤에 `!`)을 사용하여 `radius`가 확실히 존재한다고 말할 수 있습니다.

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

하지만 이것은 이상적으로 느껴지지 않습니다. 타입 검사기에게 `shape.radius`가 정의되어 있다고 말하기 위해 널 아님 단언(`!`)으로 소리쳐야 했지만, 코드를 이동하기 시작하면 이러한 단언은 오류가 발생하기 쉽습니다. 또한, `strictNullChecks` 외부에서 우리는 실수로 해당 필드에 접근할 수 있습니다(선택적 속성은 읽을 때 항상 존재한다고 가정되기 때문입니다). 확실히 더 잘할 수 있습니다.

이 `Shape` 인코딩의 문제는 타입 검사기가 `radius`나 `sideLength`가 `kind` 속성에 기반하여 존재하는지 알 방법이 없다는 것입니다. 우리가 아는 것을 타입 검사기에게 전달해야 합니다. 그것을 염두에 두고, `Shape`를 정의하는 또 다른 시도를 해봅시다.

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

여기서 `Shape`를 `kind` 속성에 대해 다른 값을 가진 두 타입으로 적절하게 분리했지만, `radius`와 `sideLength`는 각각의 타입에서 필수 속성으로 선언되었습니다.

`Shape`의 `radius`에 직접 접근하려고 하면 무슨 일이 일어나는지 봅시다.

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

첫 번째 `Shape` 정의와 마찬가지로, 이것은 여전히 오류입니다. `radius`가 선택적일 때, 오류가 발생했습니다(오직 `strictNullChecks`가 활성화된 경우) TypeScript는 속성이 존재하는지 알 수 없었기 때문입니다. 이제 `Shape`가 유니온이므로, TypeScript는 `shape`이 `Square`일 수 있고, `Square`는 `radius`가 정의되어 있지 않다고 말합니다! 두 해석 모두 올바르지만, `strictNullChecks`가 어떻게 구성되어 있든 `Shape`의 유니온 인코딩만 오류를 일으킵니다.

하지만 `kind` 속성을 다시 확인하면 어떨까요?

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

오류가 사라졌습니다! 유니온의 모든 타입이 리터럴 타입을 가진 공통 속성을 포함할 때, TypeScript는 이것을 _판별 유니온_으로 간주하고, 유니온의 멤버를 좁힐 수 있습니다.

이 경우, `kind`가 그 공통 속성이었습니다(이것이 `Shape`의 _판별_ 속성으로 간주되는 것입니다). `kind` 속성이 `"circle"`인지 확인하면 `kind` 속성이 `"circle"`이 아닌 `Shape`의 모든 타입이 제거되었습니다. 이것이 `shape`을 `Circle` 타입으로 좁혔습니다.

같은 검사가 `switch` 문에서도 작동합니다. 이제 성가신 `!` 널 아님 단언 없이 완전한 `getArea`를 작성할 수 있습니다.

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

여기서 중요한 것은 `Shape`의 인코딩이었습니다. TypeScript에 올바른 정보를 전달하는 것 - `Circle`과 `Square`가 실제로 특정 `kind` 필드를 가진 두 개의 별개 타입이라는 것 - 이 중요했습니다. 그렇게 하면 우리가 어차피 작성했을 것과 다르지 않은 타입 안전 TypeScript 코드를 작성할 수 있습니다. 거기서부터, 타입 시스템은 "올바른" 일을 하고 `switch` 문의 각 분기에서 타입을 알아낼 수 있었습니다.

> 참고로, 위 예제에서 반환 타입 추론이 어떻게 `switch` 문의 다른 분기에서 결정된 반환 타입을 통해 작동하는지 살펴보세요.

판별 유니온은 원과 사각형에 대해 이야기하는 것 이상에 유용합니다. 네트워크를 통해 메시지를 보낼 때(클라이언트/서버 통신)나 상태 관리 프레임워크에서 뮤테이션을 인코딩할 때와 같이 JavaScript에서 모든 종류의 메시징 스키마를 표현하는 데 좋습니다.

## `never` 타입

좁힐 때, 유니온의 옵션을 모든 가능성을 제거하고 아무것도 남지 않은 지점까지 줄일 수 있습니다. 그러한 경우, TypeScript는 존재해서는 안 되는 상태를 나타내기 위해 `never` 타입을 사용합니다.

## 철저한 검사

`never` 타입은 모든 타입에 할당 가능합니다; 그러나 어떤 타입도 `never`에 할당할 수 없습니다(`never` 자체를 제외하고). 이것은 `switch` 문에서 철저한 검사를 수행하기 위해 좁히기와 `never`에 의존할 수 있다는 것을 의미합니다.

예를 들어, 모든 가능한 케이스가 처리되었을 때 `shape`에 `never`를 할당하려고 하는 `getArea` 함수에 `default`를 추가하면 오류가 발생하지 않습니다.

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

`Shape` 유니온에 새 멤버를 추가하면 TypeScript 오류가 발생합니다:

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

이것은 `Triangle`이 아직 처리되지 않았음을 상기시켜주므로 유용합니다.

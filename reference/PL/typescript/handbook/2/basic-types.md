# 기본 사항

JavaScript의 모든 값은 다양한 연산을 실행하여 관찰할 수 있는 일련의 동작을 가지고 있습니다.
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
- `toLowerCase`라는 속성을 가지고 있는가?
- 가지고 있다면, `toLowerCase`는 호출 가능한가?
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

코드를 실행할 때, JavaScript 런타임이 무엇을 할지 선택하는 방법은 값의 _타입_ - 어떤 종류의 동작과 기능을 가지고 있는지 - 을 파악하는 것입니다.
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

## 비예외 실패

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

타입 검사기는 변수 및 기타 속성에서 올바른 속성에 접근하고 있는지 확인할 정보를 가지고 있습니다.
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
TypeScript는 코드를 ECMAScript 3이나 ECMAScript 5(일명 ES5)와 같은 더 새로운 버전의 ECMAScript에서 더 오래된 버전으로 다시 작성하는 기능을 가지고 있습니다.
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

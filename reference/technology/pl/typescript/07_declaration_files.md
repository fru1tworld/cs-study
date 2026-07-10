# 선언 파일 (Declaration Files)

# 타입 선언

> **원문:** https://www.typescriptlang.org/docs/handbook/2/type-declarations.html

지금까지 읽은 섹션 전체에서 모든 JavaScript 런타임에 있는 내장 함수를 사용하여 기본 TypeScript 개념을 시연해 왔습니다.
그러나 오늘날 거의 모든 JavaScript에는 일반적인 작업을 수행하기 위한 많은 라이브러리가 포함되어 있습니다.
애플리케이션에서 _당신의_ 코드가 아닌 부분에 대한 타입을 갖는 것은 TypeScript 경험을 크게 향상시킬 것입니다.
이러한 타입은 어디에서 오나요?

## 타입 선언은 어떻게 생겼나요?

다음과 같은 코드를 작성한다고 가정합시다:

```ts twoslash
// @errors: 2339
const k = Math.max(5, 6);
const j = Math.mix(7, 8);
```

TypeScript는 `Math`의 구현이 코드의 일부가 아닌데도 `max`는 있지만 `mix`는 없다는 것을 어떻게 알았을까요?

정답은 이러한 내장 객체를 설명하는 _선언 파일_이 있다는 것입니다.
선언 파일은 해당 값에 대한 구현을 실제로 제공하지 않고 일부 타입이나 값의 존재를 _선언_하는 방법을 제공합니다.

## `.d.ts` 파일

TypeScript는 두 가지 주요 종류의 파일이 있습니다.
`.ts` 파일은 타입과 실행 가능한 코드를 포함하는 _구현_ 파일입니다.
이것들은 `.js` 출력을 생성하는 파일이며, 일반적으로 코드를 작성하는 곳입니다.

`.d.ts` 파일은 _오직_ 타입 정보만 포함하는 _선언_ 파일입니다.
이러한 파일은 `.js` 출력을 생성하지 않습니다; 타입 검사에만 사용됩니다.
나중에 자체 선언 파일을 작성하는 방법에 대해 더 배울 것입니다.

## 내장 타입 정의

TypeScript는 JavaScript 런타임에서 사용 가능한 모든 표준화된 내장 API에 대한 선언 파일을 포함합니다.
여기에는 `string`이나 `function`과 같은 내장 타입의 메서드와 속성, `Math`와 `Object`와 같은 최상위 이름 및 관련 타입이 포함됩니다.
기본적으로 TypeScript는 브라우저 내부에서 실행할 때 사용 가능한 것들에 대한 타입도 포함합니다. 예를 들어 `window`와 `document`; 이것들을 통칭하여 DOM API라고 합니다.

TypeScript는 이러한 선언 파일을 `lib.[something].d.ts` 패턴으로 이름 짓습니다.
해당 이름을 가진 파일로 이동하면, 사용자 코드가 아니라 플랫폼의 일부 내장 부분을 다루고 있다는 것을 알 수 있습니다.

### `target` 설정

사용 가능한 메서드, 속성 및 함수는 실제로 코드가 실행되는 JavaScript _버전_에 따라 다릅니다.
예를 들어, 문자열의 `startsWith` 메서드는 _ECMAScript 6_으로 알려진 JavaScript 버전부터만 사용 가능합니다.

코드가 궁극적으로 실행되는 JavaScript 버전을 알고 있는 것이 중요합니다. 배포하는 플랫폼보다 최신 버전의 API를 사용하고 싶지 않기 때문입니다.
이것이 [`target`](/tsconfig#target) 컴파일러 설정의 한 기능입니다.

TypeScript는 [`target`](/tsconfig#target) 설정에 따라 기본적으로 포함되는 `lib` 파일을 다르게 하여 이 문제를 돕습니다.
예를 들어, [`target`](/tsconfig#target)이 `ES5`이면 `ES6` 이상에서만 사용 가능한 `startsWith` 메서드를 사용하려고 하면 오류가 표시됩니다.

### `lib` 설정

[`lib`](/tsconfig#lib) 설정을 사용하면 프로그램에서 사용 가능하다고 간주되는 내장 선언 파일을 더 세밀하게 제어할 수 있습니다.
자세한 내용은 [`lib`](/tsconfig#lib)에 대한 문서 페이지를 참조하세요.

## 외부 정의

비내장 API의 경우, 선언 파일을 얻는 다양한 방법이 있습니다.
어떻게 하는지는 정확히 어떤 라이브러리에 대한 타입을 얻으려는지에 따라 다릅니다.

### 번들된 타입

사용 중인 라이브러리가 npm 패키지로 게시된 경우, 이미 배포의 일부로 타입 선언 파일을 포함하고 있을 수 있습니다.
프로젝트의 문서를 읽어서 알아보거나, 단순히 패키지를 가져와서 TypeScript가 자동으로 타입을 해결할 수 있는지 확인해 볼 수 있습니다.

패키지에 타입 정의를 번들하려는 패키지 작성자라면, [타입 정의 번들하기](/docs/handbook/declaration-files/publishing.html#including-declarations-in-your-npm-package)에 대한 가이드를 읽을 수 있습니다.

### DefinitelyTyped / `@types`

[DefinitelyTyped 저장소](https://github.com/DefinitelyTyped/DefinitelyTyped/)는 수천 개의 라이브러리에 대한 선언 파일을 저장하는 중앙 저장소입니다.
일반적으로 사용되는 대부분의 라이브러리에는 DefinitelyTyped에서 사용 가능한 선언 파일이 있습니다.

DefinitelyTyped의 정의는 npm의 `@types` 범위 아래에 자동으로 게시됩니다.
타입 패키지의 이름은 항상 기본 패키지 자체의 이름과 같습니다.
예를 들어, `react` npm 패키지를 설치했다면, 다음을 실행하여 해당 타입을 설치할 수 있습니다.

```sh
npm install --save-dev @types/react
```

TypeScript는 `node_modules/@types` 아래의 타입 정의를 자동으로 찾으므로, 프로그램에서 이러한 타입을 사용 가능하게 하는 데 다른 단계가 필요하지 않습니다.

### 자체 정의

드문 경우로 라이브러리가 자체 타입을 번들하지 않았고 DefinitelyTyped에도 정의가 없는 경우, 선언 파일을 직접 작성할 수 있습니다.
가이드는 부록 [선언 파일 작성하기](/docs/handbook/declaration-files/introduction.html)를 참조하세요.

선언 파일을 작성하지 않고 특정 모듈에 대한 경고를 끄고 싶다면, 프로젝트의 `.d.ts` 파일에 빈 선언을 넣어 모듈을 타입 `any`로 빠르게 선언할 수도 있습니다.
예를 들어, `some-untyped-module`이라는 모듈을 정의 없이 사용하고 싶다면, 다음을 작성합니다:

```ts twoslash
declare module "some-untyped-module";
```

---

# 소개

> **원문:** https://www.typescriptlang.org/docs/handbook/declaration-files/introduction.html

선언 파일 섹션은 고품질의 TypeScript 선언 파일(Declaration File)을 작성하는 방법을 알려주기 위해 설계되었습니다. 시작하기 위해서는 TypeScript 언어에 대한 기본적인 친숙함이 필요합니다.

아직 읽지 않았다면, [TypeScript 핸드북](/docs/handbook/2/basic-types.html)을 읽고 기본 개념, 특히 타입과 모듈에 대해 익숙해지시기 바랍니다.

.d.ts 파일이 어떻게 작동하는지 배우는 가장 일반적인 경우는 타입이 없는 npm 패키지에 타입을 추가하려는 경우입니다. 그런 경우에는 [모듈 .d.ts](/docs/handbook/declaration-files/templates/module-d-ts.html)로 바로 이동하실 수 있습니다.

선언 파일 섹션은 다음 섹션들로 구성되어 있습니다.

## [선언 참조](/docs/handbook/declaration-files/by-example.html)

우리는 종종 기저 라이브러리의 예제만을 가이드로 삼아 선언 파일을 작성해야 하는 상황에 직면합니다. [선언 참조](/docs/handbook/declaration-files/by-example.html) 섹션은 많은 일반적인 API 패턴과 각각에 대한 선언을 작성하는 방법을 보여줍니다. 이 가이드는 TypeScript의 모든 언어 구조에 아직 익숙하지 않을 수 있는 TypeScript 초보자를 대상으로 합니다.

## [라이브러리 구조](/docs/handbook/declaration-files/library-structures.html)

[라이브러리 구조](/docs/handbook/declaration-files/library-structures.html) 가이드는 일반적인 라이브러리 형식을 이해하고 각 형식에 대한 적절한 선언 파일을 작성하는 방법을 도와줍니다. 기존 파일을 편집하는 경우에는 이 섹션을 읽을 필요가 없을 것입니다. 새 선언 파일을 작성하는 작성자는 라이브러리의 형식이 선언 파일 작성에 어떤 영향을 미치는지 제대로 이해하기 위해 이 섹션을 읽는 것이 강력히 권장됩니다.

템플릿 섹션에서는 새 파일을 작성할 때 유용한 시작점으로 사용할 수 있는 여러 선언 파일을 찾을 수 있습니다. 이미 구조를 알고 있다면 사이드바의 d.ts 템플릿 섹션을 참조하세요.

## [해야 할 것과 하지 말아야 할 것](/docs/handbook/declaration-files/do-s-and-don-ts.html)

선언 파일의 많은 일반적인 실수는 쉽게 피할 수 있습니다. [해야 할 것과 하지 말아야 할 것](/docs/handbook/declaration-files/do-s-and-don-ts.html) 섹션은 일반적인 오류를 식별하고, 이를 감지하는 방법, 그리고 수정하는 방법을 설명합니다. 일반적인 실수를 피하기 위해 모든 사람이 이 섹션을 읽어야 합니다.

## [심층 분석](/docs/handbook/declaration-files/deep-dive.html)

선언 파일이 어떻게 작동하는지 기저 메커니즘에 관심 있는 숙련된 작성자를 위해, [심층 분석](/docs/handbook/declaration-files/deep-dive.html) 섹션은 선언 작성의 많은 고급 개념을 설명하고, 이러한 개념을 활용하여 더 깔끔하고 직관적인 선언 파일을 만드는 방법을 보여줍니다.

## [npm에 배포하기](/docs/handbook/declaration-files/publishing.html)

[배포](/docs/handbook/declaration-files/publishing.html) 섹션은 선언 파일을 npm 패키지에 배포하는 방법과 종속 패키지를 관리하는 방법을 설명합니다.

## [선언 파일 찾기 및 설치하기](/docs/handbook/declaration-files/consumption.html)

JavaScript 라이브러리 사용자를 위해, [사용](/docs/handbook/declaration-files/consumption.html) 섹션은 해당 선언 파일을 찾고 설치하는 간단한 몇 단계를 제공합니다.

---

# 선언 참조

> **원문:** https://www.typescriptlang.org/docs/handbook/declaration-files/by-example.html

이 가이드의 목적은 고품질의 정의 파일을 작성하는 방법을 알려주는 것입니다. 이 가이드는 일부 API에 대한 문서와 해당 API의 샘플 사용법을 보여주고, 해당하는 선언을 작성하는 방법을 설명하는 방식으로 구성되어 있습니다.

이 예제들은 대략적으로 복잡성이 증가하는 순서로 정렬되어 있습니다.

## 속성를 가진 객체

_문서_

> 전역 변수 `myLib`는 인사말을 생성하는 `makeGreeting` 함수와 지금까지 만들어진 인사말의 수를 나타내는 `numberOfGreetings` 속성가 있습니다.

_코드_

```ts
let result = myLib.makeGreeting("hello, world");
console.log("The computed greeting is:" + result);

let count = myLib.numberOfGreetings;
```

_선언_

`declare namespace`를 사용하여 점 표기법으로 접근하는 타입이나 값을 설명합니다.

```ts
declare namespace myLib {
  function makeGreeting(s: string): string;
  let numberOfGreetings: number;
}
```

## 오버로드된 함수

_문서_

`getWidget` 함수는 숫자를 받아 Widget을 반환하거나, 문자열을 받아 Widget 배열을 반환합니다.

_코드_

```ts
let x: Widget = getWidget(43);

let arr: Widget[] = getWidget("all of them");
```

_선언_

```ts
declare function getWidget(n: number): Widget;
declare function getWidget(s: string): Widget[];
```

## 재사용 가능한 타입 (인터페이스)

_문서_

> 인사말을 지정할 때, `GreetingSettings` 객체를 전달해야 합니다.
> 이 객체는 다음 속성를 가집니다:
>
> 1 - greeting: 필수 문자열
>
> 2 - duration: 선택적 시간 길이 (밀리초)
>
> 3 - color: 선택적 문자열, 예: '#ff00ff'

_코드_

```ts
greet({
  greeting: "hello world",
  duration: 4000
});
```

_선언_

`interface`를 사용하여 속성를 가진 타입을 정의합니다.

```ts
interface GreetingSettings {
  greeting: string;
  duration?: number;
  color?: string;
}

declare function greet(setting: GreetingSettings): void;
```

## 재사용 가능한 타입 (타입 별칭)

_문서_

> 인사말이 예상되는 곳 어디서나, `string`, `string`을 반환하는 함수, 또는 `Greeter` 인스턴스를 제공할 수 있습니다.

_코드_

```ts
function getGreeting() {
  return "howdy";
}
class MyGreeter extends Greeter {}

greet("hello");
greet(getGreeting);
greet(new MyGreeter());
```

_선언_

타입 별칭을 사용하여 타입에 대한 약어를 만들 수 있습니다:

```ts
type GreetingLike = string | (() => string) | MyGreeter;

declare function greet(g: GreetingLike): void;
```

## 타입 구성하기

_문서_

> `greeter` 객체는 파일에 로그를 남기거나 알림을 표시할 수 있습니다.
> `.log(...)`에 LogOptions을 제공하고 `.alert(...)`에 alert 옵션을 제공할 수 있습니다.

_코드_

```ts
const g = new Greeter("Hello");
g.log({ verbose: true });
g.alert({ modal: false, title: "Current Greeting" });
```

_선언_

네임스페이스를 사용하여 타입을 구성합니다.

```ts
declare namespace GreetingLib {
  interface LogOptions {
    verbose?: boolean;
  }
  interface AlertOptions {
    modal: boolean;
    title?: string;
    color?: string;
  }
}
```

하나의 선언에서 중첩된 네임스페이스를 만들 수도 있습니다:

```ts
declare namespace GreetingLib.Options {
  // GreetingLib.Options.Log를 통해 참조
  interface Log {
    verbose?: boolean;
  }
  interface Alert {
    modal: boolean;
    title?: string;
    color?: string;
  }
}
```

## 클래스

_문서_

> `Greeter` 객체를 인스턴스화하여 greeter를 만들거나, 이를 확장하여 커스텀 greeter를 만들 수 있습니다.

_코드_

```ts
const myGreeter = new Greeter("hello, world");
myGreeter.greeting = "howdy";
myGreeter.showGreeting();

class SpecialGreeter extends Greeter {
  constructor() {
    super("Very special greetings");
  }
}
```

_선언_

`declare class`를 사용하여 클래스 또는 클래스와 유사한 객체를 설명합니다.
클래스는 생성자뿐만 아니라 속성와 메서드를 가질 수 있습니다.

```ts
declare class Greeter {
  constructor(greeting: string);

  greeting: string;
  showGreeting(): void;
}
```

## 전역 변수

_문서_

> 전역 변수 `foo`는 현재 존재하는 위젯의 수를 포함합니다.

_코드_

```ts
console.log("Half the number of widgets is " + foo / 2);
```

_선언_

`declare var`를 사용하여 변수를 선언합니다.
변수가 읽기 전용인 경우 `declare const`를 사용할 수 있습니다.
변수가 블록 스코프인 경우 `declare let`을 사용할 수도 있습니다.

```ts
/** 현재 존재하는 위젯의 수 */
declare var foo: number;
```

## 전역 함수

_문서_

> 사용자에게 인사말을 표시하기 위해 문자열과 함께 `greet` 함수를 호출할 수 있습니다.

_코드_

```ts
greet("hello, world");
```

_선언_

`declare function`을 사용하여 함수를 선언합니다.

```ts
declare function greet(greeting: string): void;
```

---

# 라이브러리 구조

> **원문:** https://www.typescriptlang.org/docs/handbook/declaration-files/library-structures.html

대체로 말해서, 선언 파일을 _구조화_하는 방법은 라이브러리가 어떻게 소비되는지에 따라 달라집니다. JavaScript에서 소비를 위해 라이브러리를 제공하는 방법은 많으며, 그에 맞게 선언 파일을 작성해야 합니다. 이 가이드는 일반적인 라이브러리 패턴을 식별하는 방법과 해당 패턴에 대응하는 선언 파일을 작성하는 방법을 다룹니다.

주요 라이브러리 구조화 패턴의 각 유형에는 [템플릿](/docs/handbook/declaration-files/templates.html) 섹션에 해당하는 파일이 있습니다. 이 템플릿으로 시작하면 더 빠르게 진행할 수 있습니다.

## 라이브러리 종류 식별하기

먼저, TypeScript 선언 파일이 나타낼 수 있는 라이브러리의 종류를 검토합니다. 각 종류의 라이브러리가 어떻게 _사용_되고, 어떻게 _작성_되는지 간략히 보여주고, 실제 세계에서의 예시 라이브러리들을 나열합니다.

라이브러리의 구조를 식별하는 것은 선언 파일을 작성하는 첫 번째 단계입니다. _사용법_과 _코드_를 기반으로 구조를 식별하는 방법에 대한 힌트를 제공합니다. 라이브러리의 문서와 구성에 따라 둘 중 하나가 더 쉬울 수 있습니다. 더 편한 것을 사용하는 것을 권장합니다.

### 무엇을 찾아야 하나요?

타입을 작성하려는 라이브러리를 살펴볼 때 스스로에게 물어볼 질문입니다.

1. 라이브러리를 어떻게 얻나요?

   예를 들어, npm을 통해서만 얻을 수 있나요 아니면 CDN에서만 얻을 수 있나요?

2. 어떻게 가져오나요?

   전역 객체를 추가하나요? `require` 또는 `import`/`export` 문을 사용하나요?

### 다양한 유형의 라이브러리를 위한 작은 샘플

### 모듈 라이브러리

거의 모든 최신 Node.js 라이브러리는 모듈 계열에 속합니다. 이러한 유형의 라이브러리는 모듈 로더가 있는 JS 환경에서만 작동합니다. 예를 들어, `express`는 Node.js에서만 작동하며 CommonJS `require` 함수를 사용하여 로드해야 합니다.

ECMAScript 2015(ES2015, ECMAScript 6, ES6으로도 알려짐), CommonJS, RequireJS는 _모듈_을 _가져오는_ 것에 대해 유사한 개념이 있습니다. JavaScript CommonJS(Node.js)에서는 예를 들어 다음과 같이 작성합니다:

```js
var fs = require("fs");
```

TypeScript나 ES6에서는 `import` 키워드가 같은 목적을 수행합니다:

```ts
import * as fs from "fs";
```

일반적으로 모듈 라이브러리의 문서에서 다음 중 하나의 줄을 볼 수 있습니다:

```js
var someLib = require("someLib");
```

또는

```js
define(..., ['someLib'], function(someLib) {

});
```

전역 모듈과 마찬가지로, [UMD](#umd) 모듈의 문서에서 이러한 예제를 볼 수 있으므로, 코드나 문서를 확인하세요.

#### 코드에서 모듈 라이브러리 식별하기

모듈 라이브러리는 일반적으로 다음 중 적어도 일부가 있습니다:

- `require` 또는 `define`에 대한 무조건적 호출
- `import * as a from 'b';` 또는 `export c;`와 같은 선언
- `exports` 또는 `module.exports`에 대한 할당

드물게 다음을 가집니다:

- `window` 또는 `global`의 속성에 대한 할당

#### 모듈을 위한 템플릿

모듈을 위해 네 가지 템플릿을 사용할 수 있습니다: [`module.d.ts`](/docs/handbook/declaration-files/templates/module-d-ts.html), [`module-class.d.ts`](/docs/handbook/declaration-files/templates/module-class-d-ts.html), [`module-function.d.ts`](/docs/handbook/declaration-files/templates/module-function-d-ts.html), [`module-plugin.d.ts`](/docs/handbook/declaration-files/templates/module-plugin-d-ts.html).

먼저 [`module.d.ts`](/docs/handbook/declaration-files/templates/module-d-ts.html)를 읽어 모든 것이 어떻게 작동하는지 개요를 파악하세요.

그런 다음 모듈이 함수처럼 _호출_될 수 있는 경우 [`module-function.d.ts`](/docs/handbook/declaration-files/templates/module-function-d-ts.html) 템플릿을 사용하세요:

```js
const x = require("foo");
// 참고: 'x'를 함수로 호출
const y = x(42);
```

모듈이 `new`를 사용하여 _생성_될 수 있는 경우 [`module-class.d.ts`](/docs/handbook/declaration-files/templates/module-class-d-ts.html) 템플릿을 사용하세요:

```js
const x = require("bar");
// 참고: 가져온 변수에 'new' 연산자 사용
const y = new x("hello");
```

가져올 때 다른 모듈에 변경을 가하는 모듈이 있는 경우 [`module-plugin.d.ts`](/docs/handbook/declaration-files/templates/module-plugin-d-ts.html) 템플릿을 사용하세요:

```js
const jest = require("jest");
require("jest-matchers-files");
```

### 전역 라이브러리

_전역_ 라이브러리는 전역 스코프에서 접근할 수 있는 라이브러리입니다(즉, 어떤 형태의 `import`도 사용하지 않음). 많은 라이브러리는 단순히 사용할 수 있도록 하나 이상의 전역 변수를 노출합니다. 예를 들어, [jQuery](https://jquery.com/)를 사용하는 경우, `$` 변수는 단순히 참조하여 사용할 수 있습니다:

```ts
$(() => {
  console.log("hello!");
});
```

일반적으로 전역 라이브러리의 문서에서 HTML script 태그에서 라이브러리를 사용하는 방법에 대한 안내를 볼 수 있습니다:

```html
<script src="http://a.great.cdn.for/someLib.js"></script>
```

오늘날 대부분의 인기 있는 전역 접근 가능 라이브러리는 실제로 UMD 라이브러리로 작성됩니다(아래 참조). UMD 라이브러리 문서는 전역 라이브러리 문서와 구별하기 어렵습니다. 전역 선언 파일을 작성하기 전에, 라이브러리가 실제로 UMD가 아닌지 확인하세요.

#### 코드에서 전역 라이브러리 식별하기

전역 라이브러리 코드는 일반적으로 매우 간단합니다. 전역 "Hello, world" 라이브러리는 다음과 같을 수 있습니다:

```js
function createGreeting(s) {
  return "Hello, " + s;
}
```

또는 다음과 같습니다:

```js
// 웹
window.createGreeting = function (s) {
  return "Hello, " + s;
};

// Node
global.createGreeting = function (s) {
  return "Hello, " + s;
};

// 잠재적으로 모든 런타임
globalThis.createGreeting = function (s) {
  return "Hello, " + s;
};
```

전역 라이브러리의 코드를 볼 때, 일반적으로 다음을 볼 수 있습니다:

- 최상위 `var` 문 또는 `function` 선언
- `window.someName`에 대한 하나 이상의 할당
- `document`나 `window`와 같은 DOM 프리미티브가 존재한다고 가정

다음은 볼 수 _없습니다_:

- `require`나 `define`과 같은 모듈 로더의 확인 또는 사용
- `var fs = require("fs");` 형태의 CommonJS/Node.js 스타일 가져오기
- `define(...)`에 대한 호출
- 라이브러리를 `require`하거나 import하는 방법을 설명하는 문서

#### 전역 라이브러리 예시

전역 라이브러리를 UMD 라이브러리로 전환하는 것이 일반적으로 쉽기 때문에, 전역 스타일로 작성된 인기 있는 라이브러리는 거의 없습니다. 그러나 작고 DOM이 필요하거나(또는 종속성이 _없는_) 라이브러리는 여전히 전역일 수 있습니다.

#### 전역 라이브러리 템플릿

템플릿 파일 [`global.d.ts`](/docs/handbook/declaration-files/templates/global-d-ts.html)는 예시 라이브러리 `myLib`를 정의합니다. ["이름 충돌 방지" 각주](#이름-충돌-방지)를 꼭 읽어보세요.

### _UMD_

_UMD_ 모듈은 모듈로 사용하거나(import를 통해) 전역으로 사용할 수 있는 모듈입니다(모듈 로더가 없는 환경에서 실행될 때). [Moment.js](https://momentjs.com/)와 같은 많은 인기 있는 라이브러리가 이 방식으로 작성됩니다. 예를 들어, Node.js에서 또는 RequireJS를 사용할 때 다음과 같이 작성합니다:

```ts
import moment = require("moment");
console.log(moment.format());
```

반면 바닐라 브라우저 환경에서는 다음과 같이 작성합니다:

```js
console.log(moment.format());
```

#### UMD 라이브러리 식별하기

[UMD 모듈](https://github.com/umdjs/umd)은 모듈 로더 환경의 존재를 확인합니다. 이것은 다음과 같이 보이는 쉽게 발견할 수 있는 패턴입니다:

```js
(function (root, factory) {
    if (typeof define === "function" && define.amd) {
        define(["libName"], factory);
    } else if (typeof module === "object" && module.exports) {
        module.exports = factory(require("libName"));
    } else {
        root.returnExports = factory(root.libName);
    }
}(this, function (b) {
```

라이브러리 코드에서, 특히 파일 상단에서 `typeof define`, `typeof window`, 또는 `typeof module`에 대한 테스트를 보면, 거의 항상 UMD 라이브러리입니다.

UMD 라이브러리의 문서는 종종 `require`를 보여주는 "Node.js에서 사용하기" 예제와 `<script>` 태그를 사용하여 스크립트를 로드하는 "브라우저에서 사용하기" 예제를 보여줍니다.

#### UMD 라이브러리 예시

대부분의 인기 있는 라이브러리는 이제 UMD 패키지로 제공됩니다. [jQuery](https://jquery.com/), [Moment.js](https://momentjs.com/), [lodash](https://lodash.com/) 등을 예로 들 수 있습니다.

#### 템플릿

[`module-plugin.d.ts`](/docs/handbook/declaration-files/templates/module-plugin-d-ts.html) 템플릿을 사용하세요.

## 종속성 사용하기

라이브러리가 가질 수 있는 여러 종류의 종속성이 있습니다. 이 섹션에서는 선언 파일에서 이들을 가져오는 방법을 보여줍니다.

### 전역 라이브러리에 대한 종속성

라이브러리가 전역 라이브러리에 종속되어 있는 경우, `/// <reference types="..." />` 지시문을 사용하세요:

```ts
/// <reference types="someLib" />

function getThing(): someLib.thing;
```

### 모듈에 대한 종속성

라이브러리가 모듈에 종속되어 있는 경우, `import` 문을 사용하세요:

```ts
import * as moment from "moment";

function getThing(): moment;
```

### UMD 라이브러리에 대한 종속성

#### 전역 라이브러리에서

전역 라이브러리가 UMD 모듈에 종속되어 있는 경우, `/// <reference types` 지시문을 사용하세요:

```ts
/// <reference types="moment" />

function getThing(): moment;
```

#### 모듈 또는 UMD 라이브러리에서

모듈 또는 UMD 라이브러리가 UMD 라이브러리에 종속되어 있는 경우, `import` 문을 사용하세요:

```ts
import * as someLib from "someLib";
```

UMD 라이브러리에 대한 종속성을 선언하기 위해 `/// <reference` 지시문을 사용하지 _마세요_!

## 각주

### 이름 충돌 방지

전역 선언 파일을 작성할 때 전역 스코프에서 많은 타입을 정의할 수 있다는 점에 유의하세요. 많은 선언 파일이 프로젝트에 있을 때 해결할 수 없는 이름 충돌이 발생할 수 있으므로 이를 강력히 권장하지 않습니다.

따를 수 있는 간단한 규칙은 라이브러리가 정의하는 전역 변수로 타입을 _네임스페이스화_하여 선언하는 것입니다. 예를 들어, 라이브러리가 전역 값 'cats'를 정의하는 경우 다음과 같이 작성해야 합니다:

```ts
declare namespace cats {
  interface KittySettings {}
}
```

다음처럼 작성하면 _안 됩니다_:

```ts
// 최상위 레벨에서
interface CatsKittySettings {}
```

이 지침은 또한 선언 파일 사용자를 깨뜨리지 않고 라이브러리를 UMD로 전환할 수 있도록 보장합니다.

### 모듈 호출 시그니처에 대한 ES6의 영향

Express와 같은 많은 인기 있는 라이브러리는 가져올 때 호출 가능한 함수로 자신을 노출합니다. 예를 들어, 일반적인 Express 사용법은 다음과 같습니다:

```ts
import exp = require("express");
var app = exp();
```

ES6 호환 모듈 로더에서 최상위 객체(여기서는 `exp`로 가져온)는 속성만 가질 수 있습니다; 최상위 모듈 객체는 _절대로_ 호출 가능하지 않습니다.

여기서 가장 일반적인 해결책은 호출 가능/생성 가능 객체에 대해 `default` 내보내기를 정의하는 것입니다; 모듈 로더는 일반적으로 이 상황을 자동으로 감지하고 최상위 객체를 `default` 내보내기로 대체합니다. tsconfig.json에 [`"esModuleInterop": true`](/tsconfig/#esModuleInterop)가 있으면 TypeScript가 이를 처리해줍니다.

---

# 해야 할 것과 하지 말아야 할 것

> **원문:** https://www.typescriptlang.org/docs/handbook/declaration-files/do-s-and-don-ts.html

## 일반 타입

### `Number`, `String`, `Boolean`, `Symbol`, `Object`

:x: **하지 마세요** `Number`, `String`, `Boolean`, `Symbol`, 또는 `Object` 타입을 절대 사용하지 마세요. 이 타입들은 JavaScript 코드에서 거의 적절하게 사용되지 않는 비원시 박스 객체를 나타냅니다.

```ts
/* 잘못됨 */
function reverse(s: String): String;
```

:white_check_mark: **하세요** `number`, `string`, `boolean`, `symbol` 타입을 사용하세요.

```ts
/* 올바름 */
function reverse(s: string): string;
```

`Object` 대신 비원시 `object` 타입([TypeScript 2.2에서 추가됨](../release-notes/typescript-2-2.html#object-type))을 사용하세요.

### 제네릭

:x: **하지 마세요** 타입 매개변수를 사용하지 않는 제네릭 타입을 만들지 마세요. [TypeScript FAQ 페이지](https://github.com/Microsoft/TypeScript/wiki/FAQ#why-doesnt-type-inference-work-on-this-interface-interface-foot--)에서 자세한 내용을 확인하세요.

### any

:x: **하지 마세요** JavaScript 프로젝트를 TypeScript로 마이그레이션하는 과정이 아니라면 `any`를 타입으로 사용하지 마세요. 컴파일러는 _효과적으로_ `any`를 "이것에 대한 타입 검사를 꺼주세요"로 취급합니다. 이는 변수의 모든 사용에 `@ts-ignore` 주석을 넣는 것과 비슷합니다. JavaScript 프로젝트를 TypeScript로 처음 마이그레이션할 때 아직 마이그레이션하지 않은 것들의 타입을 `any`로 설정할 수 있어 매우 유용할 수 있지만, 전체 TypeScript 프로젝트에서는 이를 사용하는 프로그램의 모든 부분에 대해 타입 검사를 비활성화하는 것입니다.

어떤 타입을 받아야 하는지 모르거나, 상호작용 없이 그냥 통과시킬 것이기 때문에 무엇이든 받고 싶은 경우에는 [`unknown`](/play/#example/unknown-and-never)을 사용할 수 있습니다.

## 콜백 타입

### 콜백의 반환 타입

:x: **하지 마세요** 값이 무시될 콜백에 대해 반환 타입 `any`를 사용하지 마세요:

```ts
/* 잘못됨 */
function fn(x: () => any) {
  x();
}
```

:white_check_mark: **하세요** 값이 무시될 콜백에 대해 반환 타입 `void`를 사용하세요:

```ts
/* 올바름 */
function fn(x: () => void) {
  x();
}
```

:grey_question: **왜:** `void`를 사용하는 것이 더 안전합니다. 확인되지 않은 방식으로 `x`의 반환 값을 실수로 사용하는 것을 방지하기 때문입니다:

```ts
function fn(x: () => void) {
  var k = x(); // 다른 것을 하려고 했지만 실수!
  k.doSomething(); // 오류, 하지만 반환 타입이 'any'였다면 괜찮았을 것
}
```

### 콜백의 선택적 매개변수

:x: **하지 마세요** 정말로 의도한 것이 아니라면 콜백에서 선택적 매개변수를 사용하지 마세요:

```ts
/* 잘못됨 */
interface Fetcher {
  getObject(done: (data: unknown, elapsedTime?: number) => void): void;
}
```

이것은 매우 구체적인 의미를 가집니다: `done` 콜백이 1개의 인수로 호출되거나 2개의 인수로 호출될 수 있습니다. 작성자는 아마도 콜백이 `elapsedTime` 매개변수에 관심이 없을 수 있다는 것을 말하려고 했을 것이지만, 이를 달성하기 위해 매개변수를 선택적으로 만들 필요는 없습니다 -- 더 적은 인수를 받는 콜백을 제공하는 것은 항상 합법적입니다.

:white_check_mark: **하세요** 콜백 매개변수를 비선택적으로 작성하세요:

```ts
/* 올바름 */
interface Fetcher {
  getObject(done: (data: unknown, elapsedTime: number) => void): void;
}
```

### 오버로드와 콜백

:x: **하지 마세요** 콜백 매개변수 개수만 다른 별도의 오버로드를 작성하지 마세요:

```ts
/* 잘못됨 */
declare function beforeAll(action: () => void, timeout?: number): void;
declare function beforeAll(
  action: (done: DoneFn) => void,
  timeout?: number
): void;
```

:white_check_mark: **하세요** 최대 매개변수 개수를 사용하는 단일 오버로드를 작성하세요:

```ts
/* 올바름 */
declare function beforeAll(
  action: (done: DoneFn) => void,
  timeout?: number
): void;
```

:grey_question: **왜:** 콜백이 매개변수를 무시하는 것은 항상 합법적이므로 더 짧은 오버로드가 필요하지 않습니다. 더 짧은 콜백을 먼저 제공하면 첫 번째 오버로드와 일치하기 때문에 잘못 타입이 지정된 함수가 전달될 수 있습니다.

## 함수 오버로드

### 순서

:x: **하지 마세요** 더 구체적인 오버로드보다 더 일반적인 오버로드를 앞에 두지 마세요:

```ts
/* 잘못됨 */
declare function fn(x: unknown): unknown;
declare function fn(x: HTMLElement): number;
declare function fn(x: HTMLDivElement): string;

var myElem: HTMLDivElement;
var x = fn(myElem); // x: unknown, 뭐라고?
```

:white_check_mark: **하세요** 더 구체적인 시그니처를 더 일반적인 시그니처 뒤에 두어 오버로드를 정렬하세요:

```ts
/* 올바름 */
declare function fn(x: HTMLDivElement): string;
declare function fn(x: HTMLElement): number;
declare function fn(x: unknown): unknown;

var myElem: HTMLDivElement;
var x = fn(myElem); // x: string, :)
```

:grey_question: **왜:** TypeScript는 함수 호출을 해결할 때 _첫 번째로 일치하는 오버로드_를 선택합니다. 이전 오버로드가 이후 오버로드보다 "더 일반적"이면, 이후 오버로드는 효과적으로 숨겨지고 호출될 수 없습니다.

### 선택적 매개변수 사용

:x: **하지 마세요** 후행 매개변수만 다른 여러 오버로드를 작성하지 마세요:

```ts
/* 잘못됨 */
interface Example {
  diff(one: string): number;
  diff(one: string, two: string): number;
  diff(one: string, two: string, three: boolean): number;
}
```

:white_check_mark: **하세요** 가능한 경우 선택적 매개변수를 사용하세요:

```ts
/* 올바름 */
interface Example {
  diff(one: string, two?: string, three?: boolean): number;
}
```

이 축소는 모든 오버로드가 동일한 반환 타입을 가질 때만 발생해야 합니다.

:grey_question: **왜:** 이것은 두 가지 이유로 중요합니다.

TypeScript는 대상의 어떤 시그니처가 소스의 인수로 호출될 수 있는지 확인하여 시그니처 호환성을 해결하며, _추가 인수는 허용됩니다_. 예를 들어, 이 코드는 시그니처가 선택적 매개변수를 사용하여 올바르게 작성된 경우에만 버그를 노출합니다:

```ts
function fn(x: (a: string, b: number, c: number) => void) {}
var x: Example;
// 오버로드로 작성된 경우, OK -- 첫 번째 오버로드 사용
// 선택적으로 작성된 경우, 올바르게 오류
fn(x.diff);
```

두 번째 이유는 사용자가 TypeScript의 "엄격한 null 검사" 기능을 사용할 때입니다. 지정되지 않은 매개변수는 JavaScript에서 `undefined`로 나타나므로, 일반적으로 선택적 인수가 있는 함수에 명시적 `undefined`를 전달해도 괜찮습니다. 예를 들어, 이 코드는 엄격한 null에서 OK여야 합니다:

```ts
var x: Example;
// 오버로드로 작성된 경우, 'string'에 'undefined'를 전달하므로 잘못된 오류
// 선택적으로 작성된 경우, 올바르게 OK
x.diff("something", true ? undefined : "hour");
```

### 유니온 타입 사용

:x: **하지 마세요** 하나의 인수 위치에서만 타입이 다른 오버로드를 작성하지 마세요:

```ts
/* 잘못됨 */
interface Moment {
  utcOffset(): number;
  utcOffset(b: number): Moment;
  utcOffset(b: string): Moment;
}
```

:white_check_mark: **하세요** 가능한 경우 유니온 타입을 사용하세요:

```ts
/* 올바름 */
interface Moment {
  utcOffset(): number;
  utcOffset(b: number | string): Moment;
}
```

여기서 `b`를 선택적으로 만들지 않았다는 점에 유의하세요. 시그니처의 반환 타입이 다르기 때문입니다.

:grey_question: **왜:** 이것은 값을 함수에 "통과"시키는 사람들에게 중요합니다:

```ts
function fn(x: string): Moment;
function fn(x: number): Moment;
function fn(x: number | string) {
  // 별도의 오버로드로 작성된 경우, 잘못된 오류
  // 유니온 타입으로 작성된 경우, 올바르게 OK
  return moment().utcOffset(x);
}
```

---

# 심층 분석

> **원문:** https://www.typescriptlang.org/docs/handbook/declaration-files/deep-dive.html

## 선언 파일 이론: 심층 분석

원하는 정확한 API 형태를 제공하도록 모듈을 구조화하는 것은 까다로울 수 있습니다. 예를 들어, `new`를 사용하거나 사용하지 않고 호출하여 다른 타입을 생성할 수 있는 모듈을 원할 수 있고, 계층 구조로 노출되는 다양한 명명된 타입이 있으며, 모듈 객체에 일부 속성도 있을 수 있습니다.

이 가이드를 읽으면 친숙한 API 표면을 노출하는 복잡한 선언 파일을 작성할 수 있는 도구를 갖추게 됩니다. 이 가이드는 옵션이 더 다양한 모듈(또는 UMD) 라이브러리에 초점을 맞춥니다.

## 핵심 개념

TypeScript가 어떻게 작동하는지에 대한 몇 가지 핵심 개념을 이해하면 어떤 형태의 선언도 만드는 방법을 완전히 이해할 수 있습니다.

### 타입

이 가이드를 읽고 있다면, TypeScript에서 타입이 무엇인지 대략적으로 이미 알고 있을 것입니다. 하지만 더 명시적으로 말하면, _타입_은 다음으로 도입됩니다:

- 타입 별칭 선언 (`type sn = number | string;`)
- 인터페이스 선언 (`interface I { x: number[]; }`)
- 클래스 선언 (`class C { }`)
- 열거형 선언 (`enum E { A, B, C }`)
- 타입을 참조하는 `import` 선언

이러한 선언 형식 각각은 새로운 타입 이름을 만듭니다.

### 값

타입과 마찬가지로, 값이 무엇인지 이미 이해하고 있을 것입니다. 값은 표현식에서 참조할 수 있는 런타임 이름입니다. 예를 들어 `let x = 5;`는 `x`라는 값을 만듭니다.

다시, 명시적으로 말하면, 다음은 값을 만듭니다:

- `let`, `const`, `var` 선언
- 값을 포함하는 `namespace` 또는 `module` 선언
- `enum` 선언
- `class` 선언
- 값을 참조하는 `import` 선언
- `function` 선언

### 네임스페이스

타입은 _네임스페이스_에 존재할 수 있습니다. 예를 들어, `let x: A.B.C` 선언이 있다면, 타입 `C`가 `A.B` 네임스페이스에서 온다고 말합니다.

이 구별은 미묘하지만 중요합니다 -- 여기서 `A.B`가 반드시 타입이나 값은 아닙니다.

## 간단한 조합: 하나의 이름, 여러 의미

이름 `A`가 주어지면, `A`에 대해 타입, 값, 또는 네임스페이스라는 최대 세 가지 다른 의미를 찾을 수 있습니다. 이름이 해석되는 방식은 사용되는 컨텍스트에 따라 다릅니다. 예를 들어, `let m: A.A = A;` 선언에서 `A`는 먼저 네임스페이스로, 그 다음 타입 이름으로, 그 다음 값으로 사용됩니다. 이러한 의미는 완전히 다른 선언을 참조할 수 있습니다!

이것은 혼란스러워 보일 수 있지만, 과도하게 오버로드하지 않는 한 실제로 매우 편리합니다. 이 조합 동작의 유용한 측면을 살펴보겠습니다.

### 내장 조합

예민한 독자는 예를 들어 `class`가 _타입_과 _값_ 목록 모두에 나타났다는 것을 알아차렸을 것입니다. `class C { }` 선언은 두 가지를 만듭니다: 클래스의 인스턴스 형태를 참조하는 _타입_ `C`와 클래스의 생성자 함수를 참조하는 _값_ `C`입니다. 열거형 선언도 비슷하게 동작합니다.

### 사용자 조합

모듈 파일 `foo.d.ts`를 작성했다고 가정해봅시다:

```ts
export var SomeVar: { a: SomeType };
export interface SomeType {
  count: number;
}
```

그런 다음 이를 사용합니다:

```ts
import * as foo from "./foo";
let x: foo.SomeType = foo.SomeVar.a;
console.log(x.count);
```

이것은 충분히 잘 작동하지만, `SomeType`과 `SomeVar`가 매우 밀접하게 관련되어 있어서 같은 이름을 갖고 싶을 수 있습니다. 조합을 사용하여 이 두 개의 다른 객체(값과 타입)를 같은 이름 `Bar`로 표현할 수 있습니다:

```ts
export var Bar: { a: Bar };
export interface Bar {
  count: number;
}
```

이것은 소비 코드에서 구조 분해를 위한 매우 좋은 기회를 제공합니다:

```ts
import { Bar } from "./foo";
let x: Bar = Bar.a;
console.log(x.count);
```

다시, 여기서 `Bar`를 타입과 값 모두로 사용했습니다. `Bar` 값을 `Bar` 타입으로 선언할 필요가 없었다는 점에 유의하세요 -- 이들은 독립적입니다.

## 고급 조합

일부 종류의 선언은 여러 선언에 걸쳐 조합될 수 있습니다. 예를 들어, `class C { }`와 `interface C { }`가 공존할 수 있으며 둘 다 `C` 타입에 속성를 기여합니다.

이것은 충돌을 만들지 않는 한 합법적입니다. 일반적인 경험 법칙은 값이 `namespace`로 선언되지 않는 한 같은 이름의 다른 값과 항상 충돌하고, 타입 별칭 선언(`type s = string`)으로 선언된 경우 타입이 충돌하며, 네임스페이스는 절대 충돌하지 않는다는 것입니다.

어떻게 사용할 수 있는지 살펴보겠습니다.

### `interface`를 사용한 추가

다른 `interface` 선언으로 `interface`에 추가 멤버를 추가할 수 있습니다:

```ts
interface Foo {
  x: number;
}
// ... 다른 곳에서 ...
interface Foo {
  y: number;
}
let a: Foo = ...;
console.log(a.x + a.y); // OK
```

이것은 클래스에서도 작동합니다:

```ts
class Foo {
  x: number;
}
// ... 다른 곳에서 ...
interface Foo {
  y: number;
}
let a: Foo = ...;
console.log(a.x + a.y); // OK
```

인터페이스를 사용하여 타입 별칭(`type s = string;`)에 추가할 수는 없습니다.

### `namespace`를 사용한 추가

`namespace` 선언은 충돌을 만들지 않는 방식으로 새 타입, 값, 네임스페이스를 추가하는 데 사용할 수 있습니다.

예를 들어, 클래스에 정적 멤버를 추가할 수 있습니다:

```ts
class C {}
// ... 다른 곳에서 ...
namespace C {
  export let x: number;
}
let y = C.x; // OK
```

이 예제에서 `C`의 _정적_ 측면(생성자 함수)에 값을 추가했습니다. 값을 추가했고, 모든 값의 컨테이너는 또 다른 값이기 때문입니다(타입은 네임스페이스에 포함되고, 네임스페이스는 다른 네임스페이스에 포함됨).

클래스에 네임스페이스화된 타입을 추가할 수도 있습니다:

```ts
class C {}
// ... 다른 곳에서 ...
namespace C {
  export interface D {}
}
let y: C.D; // OK
```

이 예제에서는 `namespace` 선언을 작성할 때까지 네임스페이스 `C`가 없었습니다. 네임스페이스로서의 `C` 의미는 클래스에 의해 생성된 `C`의 값 또는 타입 의미와 충돌하지 않습니다.

마지막으로, `namespace` 선언을 사용하여 많은 다른 병합을 수행할 수 있습니다. 이것은 특별히 현실적인 예제는 아니지만, 모든 종류의 흥미로운 동작을 보여줍니다:

```ts
namespace X {
  export interface Y {}
  export class Z {}
}

// ... 다른 곳에서 ...
namespace X {
  export var Y: number;
  export namespace Z {
    export class C {}
  }
}
type X = string;
```

이 예제에서 첫 번째 블록은 다음 이름 의미를 만듭니다:

- 값 `X` (`namespace` 선언이 값 `Z`를 포함하므로)
- 네임스페이스 `X` (`namespace` 선언이 타입 `Y`를 포함하므로)
- `X` 네임스페이스 내의 타입 `Y`
- `X` 네임스페이스 내의 타입 `Z` (클래스의 인스턴스 형태)
- `X` 값의 속성인 값 `Z` (클래스의 생성자 함수)

두 번째 블록은 다음 이름 의미를 만듭니다:

- `X` 값의 속성인 값 `Y` (`number` 타입)
- 네임스페이스 `Z`
- `X` 값의 속성인 값 `Z`
- `X.Z` 네임스페이스 내의 타입 `C`
- `X.Z` 값의 속성인 값 `C`
- 타입 `X`

---

# 배포하기

> **원문:** https://www.typescriptlang.org/docs/handbook/declaration-files/publishing.html

이 가이드의 단계에 따라 선언 파일을 작성했으니, 이제 npm에 배포할 차례입니다. 선언 파일을 npm에 배포하는 두 가지 주요 방법이 있습니다:

1. npm 패키지와 함께 번들링
2. npm의 [@types 조직](https://www.npmjs.com/~types)에 배포

타입이 소스 코드에서 생성되는 경우, 소스 코드와 함께 타입을 배포하세요. TypeScript와 JavaScript 프로젝트 모두 [`declaration`](/tsconfig#declaration)을 통해 타입을 생성할 수 있습니다.

그렇지 않으면, DefinitelyTyped에 타입을 제출하는 것을 권장합니다. 이렇게 하면 npm의 `@types` 조직에 배포됩니다.

## npm 패키지에 선언 포함하기

패키지에 메인 `.js` 파일이 있는 경우, `package.json` 파일에도 메인 선언 파일을 표시해야 합니다. `types` 속성를 설정하여 번들된 선언 파일을 가리키세요. 예를 들어:

```json
{
  "name": "awesome",
  "author": "Vandelay Industries",
  "version": "1.0.0",
  "main": "./lib/main.js",
  "types": "./lib/main.d.ts"
}
```

`"typings"` 필드는 `types`와 동의어이며 사용할 수도 있습니다.

## 의존성

모든 의존성은 npm에 의해 관리됩니다. 의존하는 모든 선언 패키지가 `package.json`의 `"dependencies"` 섹션에 적절하게 표시되어 있는지 확인하세요. 예를 들어, Browserify와 TypeScript를 사용하는 패키지를 작성했다고 가정해봅시다.

```json
{
  "name": "browserify-typescript-extension",
  "author": "Vandelay Industries",
  "version": "1.0.0",
  "main": "./lib/main.js",
  "types": "./lib/main.d.ts",
  "dependencies": {
    "browserify": "latest",
    "@types/browserify": "latest",
    "typescript": "next"
  }
}
```

여기서 패키지는 `browserify`와 `typescript` 패키지에 의존합니다. `browserify`는 npm 패키지와 함께 선언 파일을 번들하지 않으므로, 선언을 위해 `@types/browserify`에 의존해야 했습니다. 반면 `typescript`는 선언 파일을 패키지하므로 추가 의존성이 필요하지 않았습니다.

우리 패키지는 각각의 선언을 노출하므로, `browserify-typescript-extension` 패키지의 모든 사용자도 이러한 의존성을 가져야 합니다. 이러한 이유로 `"devDependencies"`가 아닌 `"dependencies"`를 사용했습니다. 그렇지 않으면 소비자가 이러한 패키지를 수동으로 설치해야 했을 것입니다. 명령줄 애플리케이션만 작성하고 패키지가 라이브러리로 사용될 것으로 예상하지 않았다면 `devDependencies`를 사용했을 것입니다.

## 주의 사항

### `/// <reference path="..." />`

선언 파일에서 `/// <reference path="..." />`를 사용하지 _마세요_.

```ts
/// <reference path="../typescript/lib/typescriptServices.d.ts" />
....
```

대신 `/// <reference types="..." />`를 사용하세요.

```ts
/// <reference types="typescript" />
....
```

자세한 내용은 [의존성 사용하기](/docs/handbook/declaration-files/library-structures.html#consuming-dependencies) 섹션을 다시 참조하세요.

### 의존 선언 패키징

타입 정의가 다른 패키지에 의존하는 경우:

- 자신의 것과 결합하지 _마세요_, 각각을 자체 파일에 유지하세요.
- 패키지에 선언을 복사하지도 _마세요_.
- 선언 파일을 패키지하지 않는 경우 npm 타입 선언 패키지에 의존_하세요_.

## `typesVersions`를 사용한 버전 선택

TypeScript가 읽어야 할 파일을 파악하기 위해 `package.json` 파일을 열 때, 먼저 `typesVersions`라는 필드를 확인합니다.

#### 폴더 리디렉션 (`*` 사용)

`typesVersions` 필드가 있는 `package.json`은 다음과 같을 수 있습니다:

```json
{
  "name": "package-name",
  "version": "1.0.0",
  "types": "./index.d.ts",
  "typesVersions": {
    ">=3.1": { "*": ["ts3.1/*"] }
  }
}
```

이 `package.json`은 TypeScript에게 먼저 현재 TypeScript 버전을 확인하라고 알려줍니다. 3.1 이상이면 TypeScript는 패키지에 대해 가져온 경로를 파악하고 패키지의 `ts3.1` 폴더에서 읽습니다.

`{ "*": ["ts3.1/*"] }`이 의미하는 것은 - [경로 매핑](/tsconfig#paths)에 익숙하다면 정확히 그렇게 작동합니다.

위의 예제에서, `"package-name"`에서 가져오는 경우, TypeScript 3.1에서 실행할 때 `[...]/node_modules/package-name/ts3.1/index.d.ts`(및 다른 관련 경로)에서 해결하려고 시도합니다. `package-name/foo`에서 가져오면 `[...]/node_modules/package-name/ts3.1/foo.d.ts` 및 `[...]/node_modules/package-name/ts3.1/foo/index.d.ts`를 찾으려고 시도합니다.

이 예제에서 TypeScript 3.1에서 실행하지 않는다면 어떻게 될까요? `typesVersions`의 필드 중 일치하는 것이 없으면 TypeScript는 `types` 필드로 폴백하므로, 여기서 TypeScript 3.0 이하는 `[...]/node_modules/package-name/index.d.ts`로 리디렉션됩니다.

#### 파일 리디렉션

한 번에 하나의 파일에 대해서만 해결을 변경하려면, 정확한 파일 이름을 전달하여 TypeScript에게 파일을 다르게 해결하도록 알릴 수 있습니다:

```json
{
  "name": "package-name",
  "version": "1.0.0",
  "types": "./index.d.ts",
  "typesVersions": {
    "<4.0": { "index.d.ts": ["index.v3.d.ts"] }
  }
}
```

TypeScript 4.0 이상에서 `"package-name"`의 import는 `./index.d.ts`로 해결되고, 3.9 이하에서는 `"./index.v3.d.ts`로 해결됩니다.

리디렉션은 패키지의 _외부_ API에만 영향을 미칩니다; 프로젝트 내의 import 해결은 `typesVersions`의 영향을 받지 않습니다. 예를 들어, 이전 예제에서 `import * as foo from "./index"`를 포함하는 `d.ts` 파일은 여전히 `index.d.ts`에 매핑되고, `index.v3.d.ts`가 아닙니다. 반면 `import * as foo from "package-name"`를 가져오는 다른 패키지는 `index.v3.d.ts`를 _가져옵니다_.

## 일치 동작

TypeScript가 컴파일러와 언어의 버전이 일치하는지 결정하는 방법은 Node의 [semver 범위](https://github.com/npm/node-semver#ranges)를 사용하는 것입니다.

## 다중 필드

`typesVersions`는 여러 필드를 지원할 수 있으며, 각 필드 이름은 일치시킬 범위로 지정됩니다.

```json
{
  "name": "package-name",
  "version": "1.0",
  "types": "./index.d.ts",
  "typesVersions": {
    ">=3.2": { "*": ["ts3.2/*"] },
    ">=3.1": { "*": ["ts3.1/*"] }
  }
}
```

범위가 겹칠 가능성이 있으므로, 어떤 리디렉션이 적용되는지 결정하는 것은 순서에 따라 다릅니다. 즉, 위의 예제에서 `>=3.2`와 `>=3.1` 매처 모두 TypeScript 3.2 이상을 지원하더라도, 순서를 바꾸면 다른 동작이 발생할 수 있으므로 위의 샘플은 다음과 동등하지 않습니다.

```jsonc
{
  "name": "package-name",
  "version": "1.0",
  "types": "./index.d.ts",
  "typesVersions": {
    // 참고: 이것은 작동하지 않습니다!
    ">=3.1": { "*": ["ts3.1/*"] },
    ">=3.2": { "*": ["ts3.2/*"] }
  }
}
```

## [@types](https://www.npmjs.com/~types)에 배포하기

[@types](https://www.npmjs.com/~types) 조직의 패키지는 [types-publisher 도구](https://github.com/microsoft/DefinitelyTyped-tools/tree/master/packages/publisher)를 사용하여 [DefinitelyTyped](https://github.com/DefinitelyTyped/DefinitelyTyped)에서 자동으로 배포됩니다. 선언을 @types 패키지로 배포하려면 [DefinitelyTyped](https://github.com/DefinitelyTyped/DefinitelyTyped)에 풀 리퀘스트를 제출하세요. [기여 가이드라인 페이지](https://definitelytyped.github.io/guides/contributing.html)에서 자세한 내용을 찾을 수 있습니다.

---

# 사용하기

> **원문:** https://www.typescriptlang.org/docs/handbook/declaration-files/consumption.html

## 다운로드

타입 선언을 가져오는 데는 npm 외에 다른 도구가 필요하지 않습니다.

예를 들어, lodash와 같은 라이브러리의 선언을 가져오려면 다음 명령만 필요합니다:

```cmd
npm install --save-dev @types/lodash
```

npm 패키지가 이미 [배포하기](/docs/handbook/declaration-files/publishing.html)에서 설명한 대로 선언 파일을 포함하고 있다면, 해당 `@types` 패키지를 다운로드할 필요가 없다는 점을 알아두세요.

## 사용하기

그런 다음 TypeScript 코드에서 lodash를 문제없이 사용할 수 있습니다. 이것은 모듈과 전역 코드 모두에서 작동합니다.

예를 들어, 타입 선언을 `npm install`한 후에는 import를 사용하여 작성할 수 있습니다:

```ts
import * as _ from "lodash";
_.padStart("Hello TypeScript!", 20, " ");
```

또는 모듈을 사용하지 않는 경우, 전역 변수 `_`를 그냥 사용할 수 있습니다.

```ts
_.padStart("Hello TypeScript!", 20, " ");
```

## 검색

대부분의 경우, 타입 선언 패키지는 항상 `npm`의 패키지 이름과 동일한 이름을 가지지만 `@types/` 접두사가 붙습니다. 하지만 필요한 경우 [Yarn 패키지 검색](https://yarnpkg.com/)을 사용하여 좋아하는 라이브러리의 패키지를 찾을 수 있습니다.

> 참고: 찾고 있는 선언 파일이 없는 경우, 언제든지 하나를 기여하고 다음에 찾는 개발자를 도울 수 있습니다. 자세한 내용은 DefinitelyTyped [기여 가이드라인 페이지](https://definitelytyped.org/guides/contributing.html)를 참조하세요.

# TypeScript 선언 파일과 타입 시스템 레퍼런스

## 선언 파일 (Declaration Files)

## 타입 선언

> **원문:** https://www.typescriptlang.org/docs/handbook/2/type-declarations.html

- 지금까지는 모든 JavaScript 런타임에 있는 내장 함수로 기본 TypeScript 개념을 시연함
- 오늘날 거의 모든 JavaScript는 일반적인 작업을 위한 많은 라이브러리를 포함함
- 애플리케이션에서 _당신의_ 코드가 아닌 부분에 대한 타입을 갖추면 TypeScript 경험이 크게 향상됨
- 이러한 타입의 출처는 아래에서 설명

### 타입 선언은 어떻게 생겼나요?

- 다음과 같은 코드를 작성한다고 가정

```ts twoslash
// @errors: 2339
const k = Math.max(5, 6);
const j = Math.mix(7, 8);
```

- TypeScript는 `Math`의 구현이 코드의 일부가 아닌데도 `max`는 있지만 `mix`는 없다는 것을 어떻게 알았는가
  - 정답: 이러한 내장 객체를 설명하는 _선언 파일_이 존재
  - 선언 파일 = 해당 값에 대한 구현을 실제로 제공하지 않고 일부 타입이나 값의 존재를 _선언_하는 방법

### `.d.ts` 파일

- TypeScript에는 두 가지 주요 종류의 파일이 있음
  - `.ts` 파일: 타입과 실행 가능한 코드를 포함하는 _구현_ 파일 → `.js` 출력을 생성 → 일반적으로 코드를 작성하는 곳
  - `.d.ts` 파일: _오직_ 타입 정보만 포함하는 _선언_ 파일 → `.js` 출력을 생성하지 않음 → 타입 검사에만 사용
- 자체 선언 파일을 작성하는 방법은 뒤에서 설명

### 내장 타입 정의

- TypeScript는 JavaScript 런타임에서 사용 가능한 모든 표준화된 내장 API에 대한 선언 파일을 포함
  - `string`이나 `function`과 같은 내장 타입의 메서드와 속성
  - `Math`와 `Object`와 같은 최상위 이름 및 관련 타입
  - 기본적으로 브라우저 내부에서 실행할 때 사용 가능한 것들에 대한 타입도 포함 (예: `window`, `document`) → 통칭 DOM API
- TypeScript는 이러한 선언 파일을 `lib.[something].d.ts` 패턴으로 명명
  - 해당 이름을 가진 파일로 이동하면 → 사용자 코드가 아니라 플랫폼의 일부 내장 부분을 다루고 있다는 것을 알 수 있음

#### `target` 설정

- 사용 가능한 메서드·속성·함수는 실제로 코드가 실행되는 JavaScript _버전_에 따라 달라짐
  - 예: 문자열의 `startsWith` 메서드는 _ECMAScript 6_부터만 사용 가능
- 코드가 궁극적으로 실행되는 JavaScript 버전을 알아야 함 → 배포하는 플랫폼보다 최신 버전의 API 사용을 피해야 하기 때문
  - 이것이 [`target`](/tsconfig#target) 컴파일러 설정의 한 기능
- TypeScript는 [`target`](/tsconfig#target) 설정에 따라 기본적으로 포함되는 `lib` 파일을 다르게 함으로써 이 문제를 보완
  - 예: [`target`](/tsconfig#target)이 `ES5`이면 `ES6` 이상에서만 사용 가능한 `startsWith` 메서드 사용 시 오류 표시

#### `lib` 설정

- [`lib`](/tsconfig#lib) 설정 → 프로그램에서 사용 가능하다고 간주되는 내장 선언 파일을 더 세밀하게 제어 가능
- 자세한 내용은 [`lib`](/tsconfig#lib) 문서 페이지 참조

### 외부 정의

- 비내장 API의 경우 선언 파일을 얻는 방법이 다양함
  - 어떤 방법을 쓸지는 정확히 어떤 라이브러리의 타입을 얻으려는지에 따라 다름

#### 번들된 타입

- 사용 중인 라이브러리가 npm 패키지로 게시된 경우 → 이미 배포의 일부로 타입 선언 파일을 포함하고 있을 수 있음
  - 프로젝트 문서를 읽거나, 패키지를 가져와서 TypeScript가 자동으로 타입을 해결할 수 있는지 확인
- 패키지에 타입 정의를 번들하려는 패키지 작성자는 [타입 정의 번들하기](/docs/handbook/declaration-files/publishing.html#including-declarations-in-your-npm-package) 가이드 참고

#### DefinitelyTyped / `@types`

- [DefinitelyTyped 저장소](https://github.com/DefinitelyTyped/DefinitelyTyped/): 수천 개의 라이브러리에 대한 선언 파일을 저장하는 중앙 저장소
  - 일반적으로 사용되는 대부분의 라이브러리는 DefinitelyTyped에서 선언 파일 제공
- DefinitelyTyped의 정의는 npm의 `@types` 범위 아래에 자동으로 게시됨
  - 타입 패키지 이름 = 항상 기본 패키지 자체의 이름과 동일
  - 예: `react` npm 패키지를 설치했다면 다음을 실행하여 해당 타입 설치 가능

```sh
npm install --save-dev @types/react
```

- TypeScript는 `node_modules/@types` 아래의 타입 정의를 자동으로 탐색 → 프로그램에서 이러한 타입을 사용 가능하게 하는 데 다른 단계 불필요

#### 자체 정의

- 드문 경우로 라이브러리가 자체 타입을 번들하지 않았고 DefinitelyTyped에도 정의가 없는 경우 → 선언 파일을 직접 작성 가능
  - 가이드는 부록 [선언 파일 작성하기](/docs/handbook/declaration-files/introduction.html) 참조
- 선언 파일을 작성하지 않고 특정 모듈에 대한 경고를 끄고 싶다면 → 프로젝트의 `.d.ts` 파일에 빈 선언을 넣어 모듈을 타입 `any`로 빠르게 선언 가능
  - 예: `some-untyped-module`이라는 모듈을 정의 없이 사용하고 싶다면 다음을 작성

```ts twoslash
declare module "some-untyped-module";
```

---

## 소개

> **원문:** https://www.typescriptlang.org/docs/handbook/declaration-files/introduction.html

- 선언 파일 섹션: 고품질의 TypeScript 선언 파일(Declaration File)을 작성하는 방법을 안내
  - 시작 전제조건: TypeScript 언어에 대한 기본적인 친숙함
- 아직 읽지 않았다면 [TypeScript 핸드북](/docs/handbook/2/basic-types.html)을 읽고 기본 개념, 특히 타입과 모듈에 익숙해질 것
- .d.ts 파일이 어떻게 작동하는지 배우는 가장 일반적인 경우는 타입이 없는 npm 패키지에 타입을 추가하려는 경우 → [모듈 .d.ts](/docs/handbook/declaration-files/templates/module-d-ts.html)로 바로 이동 가능

선언 파일 섹션은 다음 섹션들로 구성됨.

### [선언 참조](/docs/handbook/declaration-files/by-example.html)

- 기저 라이브러리의 예제만을 가이드로 삼아 선언 파일을 작성해야 하는 상황이 흔함
- [선언 참조](/docs/handbook/declaration-files/by-example.html) 섹션: 많은 일반적인 API 패턴과 각각에 대한 선언 작성법을 제시
  - 대상: TypeScript의 모든 언어 구조에 아직 익숙하지 않을 수 있는 TypeScript 초보자

### [라이브러리 구조](/docs/handbook/declaration-files/library-structures.html)

- [라이브러리 구조](/docs/handbook/declaration-files/library-structures.html) 가이드: 일반적인 라이브러리 형식을 이해하고 각 형식에 대한 적절한 선언 파일을 작성하는 방법을 안내
  - 기존 파일을 편집하는 경우에는 읽을 필요 없음
  - 새 선언 파일을 작성하는 작성자는 라이브러리 형식이 선언 파일 작성에 어떤 영향을 미치는지 이해하기 위해 읽는 것을 강력히 권장
- 템플릿 섹션에서 새 파일 작성 시 유용한 시작점이 되는 여러 선언 파일 확인 가능 → 구조를 이미 안다면 사이드바의 d.ts 템플릿 섹션 참조

### [해야 할 것과 하지 말아야 할 것](/docs/handbook/declaration-files/do-s-and-don-ts.html)

- 선언 파일의 많은 일반적인 실수는 쉽게 피할 수 있음
- [해야 할 것과 하지 말아야 할 것](/docs/handbook/declaration-files/do-s-and-don-ts.html) 섹션: 일반적인 오류를 식별하고 감지·수정 방법을 설명
  - 일반적인 실수를 피하기 위해 모든 사람이 읽어야 함

### [심층 분석](/docs/handbook/declaration-files/deep-dive.html)

- 선언 파일이 작동하는 기저 메커니즘에 관심 있는 숙련된 작성자를 위한 섹션
- [심층 분석](/docs/handbook/declaration-files/deep-dive.html): 선언 작성의 고급 개념을 설명하고 이를 활용해 더 깔끔하고 직관적인 선언 파일을 만드는 방법을 제시

### [npm에 배포하기](/docs/handbook/declaration-files/publishing.html)

- [배포](/docs/handbook/declaration-files/publishing.html) 섹션: 선언 파일을 npm 패키지에 배포하는 방법과 종속 패키지 관리 방법을 설명

### [선언 파일 찾기 및 설치하기](/docs/handbook/declaration-files/consumption.html)

- JavaScript 라이브러리 사용자 대상
- [사용](/docs/handbook/declaration-files/consumption.html) 섹션: 해당 선언 파일을 찾고 설치하는 간단한 절차 제공

---

## 선언 참조

> **원문:** https://www.typescriptlang.org/docs/handbook/declaration-files/by-example.html

- 이 가이드의 목적: 고품질의 정의 파일을 작성하는 방법 안내
  - 구성 방식: 일부 API에 대한 문서와 해당 API의 샘플 사용법을 보여주고, 해당하는 선언을 작성하는 방법을 설명
- 이 예제들은 대략적으로 복잡성이 증가하는 순서로 정렬

### 속성을 가진 객체

_문서_

> 전역 변수 `myLib`는 인사말을 생성하는 `makeGreeting` 함수와 지금까지 만들어진 인사말의 수를 나타내는 `numberOfGreetings` 속성이 있습니다.

_코드_

```ts
let result = myLib.makeGreeting("hello, world");
console.log("The computed greeting is:" + result);

let count = myLib.numberOfGreetings;
```

_선언_

- `declare namespace`를 사용하여 점 표기법으로 접근하는 타입이나 값을 설명

```ts
declare namespace myLib {
  function makeGreeting(s: string): string;
  let numberOfGreetings: number;
}
```

### 오버로드된 함수

_문서_

- `getWidget` 함수: 숫자를 받아 Widget을 반환하거나, 문자열을 받아 Widget 배열을 반환

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

### 재사용 가능한 타입 (인터페이스)

_문서_

> 인사말을 지정할 때, `GreetingSettings` 객체를 전달해야 합니다.
> 이 객체는 다음 속성을 가집니다:
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

- `interface`를 사용하여 속성을 가진 타입을 정의

```ts
interface GreetingSettings {
  greeting: string;
  duration?: number;
  color?: string;
}

declare function greet(setting: GreetingSettings): void;
```

### 재사용 가능한 타입 (타입 별칭)

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

- 타입 별칭을 사용하여 타입에 대한 약어를 만들 수 있음

```ts
type GreetingLike = string | (() => string) | MyGreeter;

declare function greet(g: GreetingLike): void;
```

### 타입 구성하기

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

- 네임스페이스를 사용하여 타입을 구성

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

- 하나의 선언에서 중첩된 네임스페이스도 만들 수 있음

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

### 클래스

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

- `declare class`를 사용하여 클래스 또는 클래스와 유사한 객체를 설명
  - 클래스는 생성자뿐만 아니라 속성과 메서드를 가질 수 있음

```ts
declare class Greeter {
  constructor(greeting: string);

  greeting: string;
  showGreeting(): void;
}
```

### 전역 변수

_문서_

> 전역 변수 `foo`는 현재 존재하는 위젯의 수를 포함합니다.

_코드_

```ts
console.log("Half the number of widgets is " + foo / 2);
```

_선언_

- `declare var`를 사용하여 변수를 선언
  - 변수가 읽기 전용인 경우 `declare const` 사용 가능
  - 변수가 블록 스코프인 경우 `declare let` 사용 가능

```ts
/** 현재 존재하는 위젯의 수 */
declare var foo: number;
```

### 전역 함수

_문서_

> 사용자에게 인사말을 표시하기 위해 문자열과 함께 `greet` 함수를 호출할 수 있습니다.

_코드_

```ts
greet("hello, world");
```

_선언_

- `declare function`을 사용하여 함수를 선언

```ts
declare function greet(greeting: string): void;
```

---

## 라이브러리 구조

> **원문:** https://www.typescriptlang.org/docs/handbook/declaration-files/library-structures.html

- 선언 파일을 _구조화_하는 방법은 라이브러리가 어떻게 소비되는지에 따라 달라짐
  - JavaScript에서 소비를 위해 라이브러리를 제공하는 방법이 다양 → 그에 맞게 선언 파일을 작성해야 함
- 이 가이드는 일반적인 라이브러리 패턴을 식별하는 방법과 해당 패턴에 대응하는 선언 파일 작성법을 다룸
- 주요 라이브러리 구조화 패턴의 각 유형에는 [템플릿](/docs/handbook/declaration-files/templates.html) 섹션에 해당하는 파일이 있음 → 이 템플릿으로 시작하면 더 빠르게 진행 가능

### 라이브러리 종류 식별하기

- 먼저 TypeScript 선언 파일이 나타낼 수 있는 라이브러리의 종류를 검토
  - 각 종류의 라이브러리가 어떻게 _사용_되고 어떻게 _작성_되는지 간략히 제시, 실제 세계 예시 라이브러리 나열
- 라이브러리의 구조를 식별하는 것이 선언 파일 작성의 첫 번째 단계
  - _사용법_과 _코드_를 기반으로 구조를 식별하는 방법에 대한 힌트 제공
  - 라이브러리의 문서와 구성에 따라 둘 중 하나가 더 쉬울 수 있음 → 더 편한 쪽을 사용 권장

#### 무엇을 찾아야 하나요?

- 타입을 작성하려는 라이브러리를 살펴볼 때 자문할 질문
  1. 라이브러리를 어떻게 얻는가 → 예: npm을 통해서만 얻을 수 있는지, CDN에서만 얻을 수 있는지
  2. 어떻게 가져오는가 → 전역 객체를 추가하는지, `require` 또는 `import`/`export` 문을 사용하는지

#### 다양한 유형의 라이브러리를 위한 작은 샘플

#### 모듈 라이브러리

- 거의 모든 최신 Node.js 라이브러리는 모듈 계열에 속함
  - 이러한 유형의 라이브러리는 모듈 로더가 있는 JS 환경에서만 작동
  - 예: `express`는 Node.js에서만 작동하며 CommonJS `require` 함수로 로드해야 함
- ECMAScript 2015(ES2015, ECMAScript 6, ES6으로도 알려짐), CommonJS, RequireJS는 _모듈_을 _가져오는_ 것에 대해 유사한 개념을 가짐
  - JavaScript CommonJS(Node.js)에서는 예를 들어 다음과 같이 작성

```js
var fs = require("fs");
```

- TypeScript나 ES6에서는 `import` 키워드가 같은 목적을 수행

```ts
import * as fs from "fs";
```

- 일반적으로 모듈 라이브러리의 문서에서 다음 중 하나의 줄을 확인 가능

```js
var someLib = require("someLib");
```

또는

```js
define(..., ['someLib'], function(someLib) {

});
```

- 전역 모듈과 마찬가지로 [UMD](#umd) 모듈의 문서에서도 이러한 예제를 볼 수 있음 → 코드나 문서를 확인 필요

##### 코드에서 모듈 라이브러리 식별하기

- 모듈 라이브러리는 일반적으로 다음 중 적어도 일부를 포함
  - `require` 또는 `define`에 대한 무조건적 호출
  - `import * as a from 'b';` 또는 `export c;`와 같은 선언
  - `exports` 또는 `module.exports`에 대한 할당
- 드물게 다음을 가짐
  - `window` 또는 `global`의 속성에 대한 할당

##### 모듈을 위한 템플릿

- 모듈을 위해 네 가지 템플릿 사용 가능: [`module.d.ts`](/docs/handbook/declaration-files/templates/module-d-ts.html), [`module-class.d.ts`](/docs/handbook/declaration-files/templates/module-class-d-ts.html), [`module-function.d.ts`](/docs/handbook/declaration-files/templates/module-function-d-ts.html), [`module-plugin.d.ts`](/docs/handbook/declaration-files/templates/module-plugin-d-ts.html)
- 먼저 [`module.d.ts`](/docs/handbook/declaration-files/templates/module-d-ts.html)를 읽고 전체 작동 개요를 파악
- 모듈이 함수처럼 _호출_될 수 있는 경우 [`module-function.d.ts`](/docs/handbook/declaration-files/templates/module-function-d-ts.html) 템플릿 사용

```js
const x = require("foo");
// 참고: 'x'를 함수로 호출
const y = x(42);
```

- 모듈이 `new`를 사용하여 _생성_될 수 있는 경우 [`module-class.d.ts`](/docs/handbook/declaration-files/templates/module-class-d-ts.html) 템플릿 사용

```js
const x = require("bar");
// 참고: 가져온 변수에 'new' 연산자 사용
const y = new x("hello");
```

- 가져올 때 다른 모듈에 변경을 가하는 모듈이 있는 경우 [`module-plugin.d.ts`](/docs/handbook/declaration-files/templates/module-plugin-d-ts.html) 템플릿 사용

```js
const jest = require("jest");
require("jest-matchers-files");
```

#### 전역 라이브러리

- _전역_ 라이브러리: 전역 스코프에서 접근할 수 있는 라이브러리 (어떤 형태의 `import`도 사용하지 않음)
  - 많은 라이브러리가 단순히 사용할 수 있도록 하나 이상의 전역 변수를 노출
  - 예: [jQuery](https://jquery.com/)를 사용하는 경우 `$` 변수를 단순히 참조하여 사용 가능

```ts
$(() => {
  console.log("hello!");
});
```

- 일반적으로 전역 라이브러리의 문서에서 HTML script 태그에서 라이브러리를 사용하는 방법에 대한 안내를 확인 가능

```html
<script src="http://a.great.cdn.for/someLib.js"></script>
```

- 오늘날 대부분의 인기 있는 전역 접근 가능 라이브러리는 실제로 UMD 라이브러리로 작성됨 (아래 참조)
  - UMD 라이브러리 문서는 전역 라이브러리 문서와 구별하기 어려움 → 전역 선언 파일을 작성하기 전에 라이브러리가 실제로 UMD가 아닌지 확인 필요

##### 코드에서 전역 라이브러리 식별하기

- 전역 라이브러리 코드는 일반적으로 매우 간단함. 전역 "Hello, world" 라이브러리 예시

```js
function createGreeting(s) {
  return "Hello, " + s;
}
```

또는

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

- 전역 라이브러리의 코드를 볼 때 일반적으로 다음을 확인 가능
  - 최상위 `var` 문 또는 `function` 선언
  - `window.someName`에 대한 하나 이상의 할당
  - `document`나 `window`와 같은 DOM 프리미티브가 존재한다고 가정
- 다음은 볼 수 _없음_
  - `require`나 `define`과 같은 모듈 로더의 확인 또는 사용
  - `var fs = require("fs");` 형태의 CommonJS/Node.js 스타일 가져오기
  - `define(...)`에 대한 호출
  - 라이브러리를 `require`하거나 import하는 방법을 설명하는 문서

##### 전역 라이브러리 예시

- 전역 라이브러리를 UMD 라이브러리로 전환하는 것이 일반적으로 쉬움 → 전역 스타일로 작성된 인기 있는 라이브러리는 거의 없음
- 다만 작고 DOM이 필요하거나(또는 종속성이 _없는_) 라이브러리는 여전히 전역일 수 있음

##### 전역 라이브러리 템플릿

- 템플릿 파일 [`global.d.ts`](/docs/handbook/declaration-files/templates/global-d-ts.html)는 예시 라이브러리 `myLib`를 정의
  - ["이름 충돌 방지" 각주](#이름-충돌-방지) 필독

#### _UMD_

- _UMD_ 모듈: 모듈로 사용하거나(import를 통해) 전역으로 사용할 수 있는 모듈 (모듈 로더가 없는 환경에서 실행될 때)
  - [Moment.js](https://momentjs.com/)와 같은 많은 인기 있는 라이브러리가 이 방식으로 작성
  - 예: Node.js에서 또는 RequireJS를 사용할 때 다음과 같이 작성

```ts
import moment = require("moment");
console.log(moment.format());
```

- 반면 바닐라 브라우저 환경에서는 다음과 같이 작성

```js
console.log(moment.format());
```

##### UMD 라이브러리 식별하기

- [UMD 모듈](https://github.com/umdjs/umd)은 모듈 로더 환경의 존재를 확인 → 쉽게 발견할 수 있는 패턴

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

- 라이브러리 코드에서, 특히 파일 상단에서 `typeof define`, `typeof window`, `typeof module`에 대한 테스트를 발견하면 → 거의 항상 UMD 라이브러리
- UMD 라이브러리의 문서는 종종 `require`를 보여주는 "Node.js에서 사용하기" 예제와 `<script>` 태그를 사용하는 "브라우저에서 사용하기" 예제를 함께 제시

##### UMD 라이브러리 예시

- 대부분의 인기 있는 라이브러리는 이제 UMD 패키지로 제공 (예: [jQuery](https://jquery.com/), [Moment.js](https://momentjs.com/), [lodash](https://lodash.com/))

##### 템플릿

- [`module-plugin.d.ts`](/docs/handbook/declaration-files/templates/module-plugin-d-ts.html) 템플릿 사용

### 종속성 사용하기

- 라이브러리가 가질 수 있는 여러 종류의 종속성이 있음. 이 섹션에서는 선언 파일에서 이들을 가져오는 방법을 설명

#### 전역 라이브러리에 대한 종속성

- 라이브러리가 전역 라이브러리에 종속된 경우 → `/// <reference types="..." />` 지시문 사용

```ts
/// <reference types="someLib" />

function getThing(): someLib.thing;
```

#### 모듈에 대한 종속성

- 라이브러리가 모듈에 종속된 경우 → `import` 문 사용

```ts
import * as moment from "moment";

function getThing(): moment;
```

#### UMD 라이브러리에 대한 종속성

##### 전역 라이브러리에서

- 전역 라이브러리가 UMD 모듈에 종속된 경우 → `/// <reference types` 지시문 사용

```ts
/// <reference types="moment" />

function getThing(): moment;
```

##### 모듈 또는 UMD 라이브러리에서

- 모듈 또는 UMD 라이브러리가 UMD 라이브러리에 종속된 경우 → `import` 문 사용

```ts
import * as someLib from "someLib";
```

- UMD 라이브러리에 대한 종속성을 선언하기 위해 `/// <reference` 지시문 사용 금지

### 각주

#### 이름 충돌 방지

- 전역 선언 파일을 작성할 때 전역 스코프에서 많은 타입을 정의할 수 있음에 유의
  - 많은 선언 파일이 프로젝트에 있을 때 해결할 수 없는 이름 충돌이 발생할 수 있으므로 강력히 비권장
- 따를 수 있는 간단한 규칙: 라이브러리가 정의하는 전역 변수로 타입을 _네임스페이스화_하여 선언
  - 예: 라이브러리가 전역 값 `cats`를 정의하는 경우 다음과 같이 작성

```ts
declare namespace cats {
  interface KittySettings {}
}
```

- 다음처럼 작성 금지

```ts
// 최상위 레벨에서
interface CatsKittySettings {}
```

- 이 지침은 선언 파일 사용자를 깨뜨리지 않고 라이브러리를 UMD로 전환할 수 있도록 보장

#### 모듈 호출 시그니처에 대한 ES6의 영향

- Express와 같은 많은 인기 있는 라이브러리는 가져올 때 호출 가능한 함수로 자신을 노출
  - 예: 일반적인 Express 사용법

```ts
import exp = require("express");
var app = exp();
```

- ES6 호환 모듈 로더에서 최상위 객체(여기서는 `exp`로 가져온)는 속성만 가질 수 있음 → 최상위 모듈 객체는 _절대로_ 호출 가능하지 않음
- 가장 일반적인 해결책: 호출 가능/생성 가능 객체에 대해 `default` 내보내기를 정의
  - 모듈 로더는 일반적으로 이 상황을 자동으로 감지하고 최상위 객체를 `default` 내보내기로 대체
  - tsconfig.json에 [`"esModuleInterop": true`](/tsconfig/#esModuleInterop)가 있으면 TypeScript가 이를 처리

---

## 해야 할 것과 하지 말아야 할 것

> **원문:** https://www.typescriptlang.org/docs/handbook/declaration-files/do-s-and-don-ts.html

### 일반 타입

#### `Number`, `String`, `Boolean`, `Symbol`, `Object`

- 하지 말 것: `Number`, `String`, `Boolean`, `Symbol`, `Object` 타입을 절대 사용하지 않음
  - 이 타입들은 JavaScript 코드에서 거의 적절하게 사용되지 않는 비원시 박스 객체를 나타냄

```ts
/* 잘못됨 */
function reverse(s: String): String;
```

- 권장: `number`, `string`, `boolean`, `symbol` 타입을 사용

```ts
/* 올바름 */
function reverse(s: string): string;
```

- `Object` 대신 비원시 `object` 타입([TypeScript 2.2에서 추가됨](../release-notes/typescript-2-2.html#object-type)) 사용

#### 제네릭

- 하지 말 것: 타입 매개변수를 사용하지 않는 제네릭 타입을 만들지 않음
  - 자세한 내용은 [TypeScript FAQ 페이지](https://github.com/Microsoft/TypeScript/wiki/FAQ#why-doesnt-type-inference-work-on-this-interface-interface-foot--) 참조

#### any

- 하지 말 것: JavaScript 프로젝트를 TypeScript로 마이그레이션하는 과정이 아니라면 `any`를 타입으로 사용하지 않음
  - 컴파일러는 _효과적으로_ `any`를 "이것에 대한 타입 검사를 꺼주세요"로 취급 → 변수의 모든 사용에 `@ts-ignore` 주석을 넣는 것과 비슷
  - JavaScript 프로젝트를 TypeScript로 처음 마이그레이션할 때 아직 마이그레이션하지 않은 것들의 타입을 `any`로 설정하면 유용
  - 하지만 전체 TypeScript 프로젝트에서는 이를 사용하는 프로그램의 모든 부분에 대해 타입 검사를 비활성화하는 것과 같음
- 어떤 타입을 받아야 하는지 모르거나 상호작용 없이 그냥 통과시킬 것이라 무엇이든 받고 싶은 경우 → [`unknown`](/play/#example/unknown-and-never) 사용

### 콜백 타입

#### 콜백의 반환 타입

- 하지 말 것: 값이 무시될 콜백에 대해 반환 타입 `any` 사용 금지

```ts
/* 잘못됨 */
function fn(x: () => any) {
  x();
}
```

- 권장: 값이 무시될 콜백에 대해 반환 타입 `void` 사용

```ts
/* 올바름 */
function fn(x: () => void) {
  x();
}
```

- 이유: `void`를 사용하는 것이 더 안전 → 확인되지 않은 방식으로 `x`의 반환 값을 실수로 사용하는 것을 방지

```ts
function fn(x: () => void) {
  var k = x(); // 다른 것을 하려고 했지만 실수!
  k.doSomething(); // 오류, 하지만 반환 타입이 'any'였다면 괜찮았을 것
}
```

#### 콜백의 선택적 매개변수

- 하지 말 것: 정말로 의도한 것이 아니라면 콜백에서 선택적 매개변수 사용 금지

```ts
/* 잘못됨 */
interface Fetcher {
  getObject(done: (data: unknown, elapsedTime?: number) => void): void;
}
```

- 위 예시는 매우 구체적인 의미를 가짐: `done` 콜백이 1개의 인수로 호출되거나 2개의 인수로 호출될 수 있음
  - 작성자는 아마도 콜백이 `elapsedTime` 매개변수에 관심이 없을 수 있다는 것을 말하려고 했을 것
  - 하지만 이를 달성하기 위해 매개변수를 선택적으로 만들 필요는 없음 → 더 적은 인수를 받는 콜백을 제공하는 것은 항상 합법적

- 권장: 콜백 매개변수를 비선택적으로 작성

```ts
/* 올바름 */
interface Fetcher {
  getObject(done: (data: unknown, elapsedTime: number) => void): void;
}
```

#### 오버로드와 콜백

- 하지 말 것: 콜백 매개변수 개수만 다른 별도의 오버로드 작성 금지

```ts
/* 잘못됨 */
declare function beforeAll(action: () => void, timeout?: number): void;
declare function beforeAll(
  action: (done: DoneFn) => void,
  timeout?: number
): void;
```

- 권장: 최대 매개변수 개수를 사용하는 단일 오버로드 작성

```ts
/* 올바름 */
declare function beforeAll(
  action: (done: DoneFn) => void,
  timeout?: number
): void;
```

- 이유: 콜백이 매개변수를 무시하는 것은 항상 합법적 → 더 짧은 오버로드가 불필요
  - 더 짧은 콜백을 먼저 제공하면 첫 번째 오버로드와 일치하기 때문에 잘못 타입이 지정된 함수가 전달될 수 있음

### 함수 오버로드

#### 순서

- 하지 말 것: 더 구체적인 오버로드보다 더 일반적인 오버로드를 앞에 두지 않음

```ts
/* 잘못됨 */
declare function fn(x: unknown): unknown;
declare function fn(x: HTMLElement): number;
declare function fn(x: HTMLDivElement): string;

var myElem: HTMLDivElement;
var x = fn(myElem); // x: unknown, 뭐라고?
```

- 권장: 더 구체적인 시그니처를 더 일반적인 시그니처 앞에 두어 오버로드를 정렬

```ts
/* 올바름 */
declare function fn(x: HTMLDivElement): string;
declare function fn(x: HTMLElement): number;
declare function fn(x: unknown): unknown;

var myElem: HTMLDivElement;
var x = fn(myElem); // x: string, :)
```

- 이유: TypeScript는 함수 호출을 해결할 때 _첫 번째로 일치하는 오버로드_를 선택
  - 이전 오버로드가 이후 오버로드보다 "더 일반적"이면 이후 오버로드는 효과적으로 숨겨져 호출될 수 없음

#### 선택적 매개변수 사용

- 하지 말 것: 후행 매개변수만 다른 여러 오버로드 작성 금지

```ts
/* 잘못됨 */
interface Example {
  diff(one: string): number;
  diff(one: string, two: string): number;
  diff(one: string, two: string, three: boolean): number;
}
```

- 권장: 가능한 경우 선택적 매개변수 사용

```ts
/* 올바름 */
interface Example {
  diff(one: string, two?: string, three?: boolean): number;
}
```

- 이 축소는 모든 오버로드가 동일한 반환 타입을 가질 때만 적용
- 이유: 두 가지가 있음
  - TypeScript는 대상의 어떤 시그니처가 소스의 인수로 호출될 수 있는지 확인하여 시그니처 호환성을 해결 → _추가 인수는 허용됨_
    - 예: 이 코드는 시그니처가 선택적 매개변수를 사용하여 올바르게 작성된 경우에만 버그를 노출

```ts
function fn(x: (a: string, b: number, c: number) => void) {}
var x: Example;
// 오버로드로 작성된 경우, OK -- 첫 번째 오버로드 사용
// 선택적으로 작성된 경우, 올바르게 오류
fn(x.diff);
```

  - 두 번째 이유는 사용자가 TypeScript의 "엄격한 null 검사" 기능을 사용할 때
    - 지정되지 않은 매개변수는 JavaScript에서 `undefined`로 나타남 → 일반적으로 선택적 인수가 있는 함수에 명시적 `undefined`를 전달해도 무방
    - 예: 이 코드는 엄격한 null에서 OK여야 함

```ts
var x: Example;
// 오버로드로 작성된 경우, 'string'에 'undefined'를 전달하므로 잘못된 오류
// 선택적으로 작성된 경우, 올바르게 OK
x.diff("something", true ? undefined : "hour");
```

#### 유니온 타입 사용

- 하지 말 것: 하나의 인수 위치에서만 타입이 다른 오버로드 작성 금지

```ts
/* 잘못됨 */
interface Moment {
  utcOffset(): number;
  utcOffset(b: number): Moment;
  utcOffset(b: string): Moment;
}
```

- 권장: 가능한 경우 유니온 타입 사용

```ts
/* 올바름 */
interface Moment {
  utcOffset(): number;
  utcOffset(b: number | string): Moment;
}
```

- 여기서 `b`를 선택적으로 만들지 않았음에 유의 → 시그니처의 반환 타입이 다르기 때문
- 이유: 값을 함수에 "통과"시키는 사람들에게 중요

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

## 심층 분석

> **원문:** https://www.typescriptlang.org/docs/handbook/declaration-files/deep-dive.html

### 선언 파일 이론: 심층 분석

- 원하는 정확한 API 형태를 제공하도록 모듈을 구조화하는 것은 까다로울 수 있음
  - 예: `new`를 사용하거나 사용하지 않고 호출하여 다른 타입을 생성할 수 있는 모듈, 계층 구조로 노출되는 다양한 명명된 타입, 모듈 객체의 일부 속성 등
- 이 가이드를 읽으면 친숙한 API 표면을 노출하는 복잡한 선언 파일을 작성할 수 있는 도구 확보 가능
  - 옵션이 더 다양한 모듈(또는 UMD) 라이브러리에 초점

### 핵심 개념

- TypeScript가 어떻게 작동하는지에 대한 핵심 개념 이해 → 어떤 형태의 선언도 만드는 방법을 완전히 이해 가능

#### 타입

- _타입_은 다음으로 도입됨
  - 타입 별칭 선언 (`type sn = number | string;`)
  - 인터페이스 선언 (`interface I { x: number[]; }`)
  - 클래스 선언 (`class C { }`)
  - 열거형 선언 (`enum E { A, B, C }`)
  - 타입을 참조하는 `import` 선언
- 이러한 선언 형식 각각은 새로운 타입 이름을 만듦

#### 값

- 값 = 표현식에서 참조할 수 있는 런타임 이름 (예: `let x = 5;`는 `x`라는 값을 만듦)
- 다음은 값을 만듦
  - `let`, `const`, `var` 선언
  - 값을 포함하는 `namespace` 또는 `module` 선언
  - `enum` 선언
  - `class` 선언
  - 값을 참조하는 `import` 선언
  - `function` 선언

#### 네임스페이스

- 타입은 _네임스페이스_에 존재할 수 있음
  - 예: `let x: A.B.C` 선언이 있다면 타입 `C`가 `A.B` 네임스페이스에서 온다고 표현
- 이 구별은 미묘하지만 중요 → 여기서 `A.B`가 반드시 타입이나 값은 아님

### 간단한 조합: 하나의 이름, 여러 의미

- 이름 `A`가 주어지면 `A`에 대해 타입, 값, 네임스페이스라는 최대 세 가지 다른 의미를 찾을 수 있음
  - 이름이 해석되는 방식은 사용되는 컨텍스트에 따라 다름
  - 예: `let m: A.A = A;` 선언에서 `A`는 먼저 네임스페이스로, 그 다음 타입 이름으로, 그 다음 값으로 사용 → 이러한 의미는 완전히 다른 선언을 참조할 수 있음
- 혼란스러워 보일 수 있지만, 과도하게 오버로드하지 않는 한 실제로 매우 편리함

#### 내장 조합

- 예: `class`가 _타입_과 _값_ 목록 모두에 나타남
  - `class C { }` 선언은 두 가지를 만듦: 클래스의 인스턴스 형태를 참조하는 _타입_ `C`와 클래스의 생성자 함수를 참조하는 _값_ `C`
  - 열거형 선언도 비슷하게 동작

#### 사용자 조합

- 모듈 파일 `foo.d.ts`를 작성했다고 가정

```ts
export var SomeVar: { a: SomeType };
export interface SomeType {
  count: number;
}
```

- 이를 사용

```ts
import * as foo from "./foo";
let x: foo.SomeType = foo.SomeVar.a;
console.log(x.count);
```

- 이것은 충분히 잘 작동하지만, `SomeType`과 `SomeVar`가 매우 밀접하게 관련되어 같은 이름을 갖고 싶을 수 있음
  - 조합을 사용하여 이 두 개의 다른 객체(값과 타입)를 같은 이름 `Bar`로 표현 가능

```ts
export var Bar: { a: Bar };
export interface Bar {
  count: number;
}
```

- 이것은 소비 코드에서 구조 분해를 위한 매우 좋은 기회를 제공

```ts
import { Bar } from "./foo";
let x: Bar = Bar.a;
console.log(x.count);
```

- 여기서 `Bar`를 타입과 값 모두로 사용 → `Bar` 값을 `Bar` 타입으로 선언할 필요는 없었음 (이들은 독립적)

### 고급 조합

- 일부 종류의 선언은 여러 선언에 걸쳐 조합될 수 있음
  - 예: `class C { }`와 `interface C { }`가 공존 가능 → 둘 다 `C` 타입에 속성을 기여
- 충돌을 만들지 않는 한 합법적
  - 일반적인 경험 법칙: 값이 `namespace`로 선언되지 않는 한 같은 이름의 다른 값과 항상 충돌, 타입 별칭 선언(`type s = string`)으로 선언된 경우 타입이 충돌, 네임스페이스는 절대 충돌하지 않음

#### `interface`를 사용한 추가

- 다른 `interface` 선언으로 `interface`에 추가 멤버를 추가 가능

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

- 이것은 클래스에서도 작동

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

- 인터페이스를 사용하여 타입 별칭(`type s = string;`)에 추가하는 것은 불가

#### `namespace`를 사용한 추가

- `namespace` 선언은 충돌을 만들지 않는 방식으로 새 타입, 값, 네임스페이스를 추가하는 데 사용 가능
- 예: 클래스에 정적 멤버를 추가

```ts
class C {}
// ... 다른 곳에서 ...
namespace C {
  export let x: number;
}
let y = C.x; // OK
```

- 위 예제에서 `C`의 _정적_ 측면(생성자 함수)에 값을 추가
  - 값을 추가한 이유: 모든 값의 컨테이너는 또 다른 값이기 때문 (타입은 네임스페이스에 포함되고, 네임스페이스는 다른 네임스페이스에 포함됨)
- 클래스에 네임스페이스화된 타입을 추가할 수도 있음

```ts
class C {}
// ... 다른 곳에서 ...
namespace C {
  export interface D {}
}
let y: C.D; // OK
```

- 위 예제에서는 `namespace` 선언을 작성할 때까지 네임스페이스 `C`가 없었음
  - 네임스페이스로서의 `C` 의미는 클래스에 의해 생성된 `C`의 값 또는 타입 의미와 충돌하지 않음
- `namespace` 선언을 사용하여 다양한 병합을 수행 가능. 특별히 현실적인 예제는 아니지만 다양한 동작을 보여줌

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

- 이 예제에서 첫 번째 블록은 다음 이름 의미를 만듦
  - 값 `X` (`namespace` 선언이 값 `Z`를 포함하므로)
  - 네임스페이스 `X` (`namespace` 선언이 타입 `Y`를 포함하므로)
  - `X` 네임스페이스 내의 타입 `Y`
  - `X` 네임스페이스 내의 타입 `Z` (클래스의 인스턴스 형태)
  - `X` 값의 속성인 값 `Z` (클래스의 생성자 함수)
- 두 번째 블록은 다음 이름 의미를 만듦
  - `X` 값의 속성인 값 `Y` (`number` 타입)
  - 네임스페이스 `Z`
  - `X` 값의 속성인 값 `Z`
  - `X.Z` 네임스페이스 내의 타입 `C`
  - `X.Z` 값의 속성인 값 `C`
  - 타입 `X`

---

## 배포하기

> **원문:** https://www.typescriptlang.org/docs/handbook/declaration-files/publishing.html

- 이 가이드의 단계에 따라 선언 파일을 작성했으니 이제 npm에 배포할 차례
- 선언 파일을 npm에 배포하는 두 가지 주요 방법
  1. npm 패키지와 함께 번들링
  2. npm의 [@types 조직](https://www.npmjs.com/~types)에 배포
- 타입이 소스 코드에서 생성되는 경우 → 소스 코드와 함께 타입을 배포
  - TypeScript와 JavaScript 프로젝트 모두 [`declaration`](/tsconfig#declaration)을 통해 타입 생성 가능
- 그렇지 않으면 DefinitelyTyped에 타입을 제출하는 것을 권장
  - 이렇게 하면 npm의 `@types` 조직에 배포됨

### npm 패키지에 선언 포함하기

- 패키지에 메인 `.js` 파일이 있는 경우 → `package.json` 파일에도 메인 선언 파일을 표시 필요
  - `types` 속성을 설정하여 번들된 선언 파일을 가리킴

```json
{
  "name": "awesome",
  "author": "Vandelay Industries",
  "version": "1.0.0",
  "main": "./lib/main.js",
  "types": "./lib/main.d.ts"
}
```

- `"typings"` 필드는 `types`와 동의어이며 사용 가능

### 의존성

- 모든 의존성은 npm에 의해 관리됨
  - 의존하는 모든 선언 패키지가 `package.json`의 `"dependencies"` 섹션에 적절하게 표시되어 있는지 확인 필요
- 예: Browserify와 TypeScript를 사용하는 패키지를 작성했다고 가정

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

- 여기서 패키지는 `browserify`와 `typescript` 패키지에 의존
  - `browserify`는 npm 패키지와 함께 선언 파일을 번들하지 않으므로 선언을 위해 `@types/browserify`에 의존 필요
  - 반면 `typescript`는 선언 파일을 패키지하므로 추가 의존성 불필요
- 우리 패키지는 각각의 선언을 노출하므로 `browserify-typescript-extension` 패키지의 모든 사용자도 이러한 의존성을 가져야 함
  - 이러한 이유로 `"devDependencies"`가 아닌 `"dependencies"`를 사용 → 그렇지 않으면 소비자가 이러한 패키지를 수동으로 설치해야 함
  - 명령줄 애플리케이션만 작성하고 패키지가 라이브러리로 사용될 것으로 예상하지 않았다면 `devDependencies` 사용

### 주의 사항

#### `/// <reference path="..." />`

- 선언 파일에서 `/// <reference path="..." />` 사용 금지

```ts
/// <reference path="../typescript/lib/typescriptServices.d.ts" />
....
```

- 대신 `/// <reference types="..." />` 사용

```ts
/// <reference types="typescript" />
....
```

- 자세한 내용은 [의존성 사용하기](/docs/handbook/declaration-files/library-structures.html#consuming-dependencies) 섹션 다시 참조

#### 의존 선언 패키징

- 타입 정의가 다른 패키지에 의존하는 경우
  - 자신의 것과 결합 금지, 각각을 자체 파일에 유지
  - 패키지에 선언을 복사하는 것도 금지
  - 선언 파일을 패키지하지 않는 경우 npm 타입 선언 패키지에 의존 필요

### `typesVersions`를 사용한 버전 선택

- TypeScript가 읽어야 할 파일을 파악하기 위해 `package.json` 파일을 열 때 먼저 `typesVersions` 필드를 확인

##### 폴더 리디렉션 (`*` 사용)

- `typesVersions` 필드가 있는 `package.json` 예시

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

- 이 `package.json`은 TypeScript에게 먼저 현재 TypeScript 버전을 확인하라고 지시
  - 3.1 이상이면 TypeScript는 패키지에 대해 가져온 경로를 파악하고 패키지의 `ts3.1` 폴더에서 읽음
- `{ "*": ["ts3.1/*"] }`의 의미: [경로 매핑](/tsconfig#paths)에 익숙하다면 정확히 그렇게 작동
- 위의 예제에서 `"package-name"`에서 가져오는 경우, TypeScript 3.1에서 실행할 때 `[...]/node_modules/package-name/ts3.1/index.d.ts`(및 다른 관련 경로)에서 해결 시도
  - `package-name/foo`에서 가져오면 `[...]/node_modules/package-name/ts3.1/foo.d.ts` 및 `[...]/node_modules/package-name/ts3.1/foo/index.d.ts`를 탐색
- TypeScript 3.1에서 실행하지 않는다면 → `typesVersions`의 필드 중 일치하는 것이 없으면 TypeScript는 `types` 필드로 폴백
  - 여기서 TypeScript 3.0 이하는 `[...]/node_modules/package-name/index.d.ts`로 리디렉션됨

##### 파일 리디렉션

- 한 번에 하나의 파일에 대해서만 해결을 변경하려면 → 정확한 파일 이름을 전달하여 TypeScript에게 파일을 다르게 해결하도록 지시

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

- TypeScript 4.0 이상에서 `"package-name"`의 import는 `./index.d.ts`로 해결되고, 3.9 이하에서는 `"./index.v3.d.ts`로 해결됨
- 리디렉션은 패키지의 _외부_ API에만 영향을 미침
  - 프로젝트 내의 import 해결은 `typesVersions`의 영향을 받지 않음
  - 예: 이전 예제에서 `import * as foo from "./index"`를 포함하는 `d.ts` 파일은 여전히 `index.d.ts`에 매핑되고 `index.v3.d.ts`가 아님
  - 반면 `import * as foo from "package-name"`를 가져오는 다른 패키지는 `index.v3.d.ts`를 _가져옴_

### 일치 동작

- TypeScript가 컴파일러와 언어의 버전이 일치하는지 결정하는 방법: Node의 [semver 범위](https://github.com/npm/node-semver#ranges) 사용

### 다중 필드

- `typesVersions`는 여러 필드를 지원 가능. 각 필드 이름은 일치시킬 범위로 지정

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

- 범위가 겹칠 가능성이 있으므로 어떤 리디렉션이 적용되는지는 순서에 따라 달라짐
  - 위의 예제에서 `>=3.2`와 `>=3.1` 매처 모두 TypeScript 3.2 이상을 지원하더라도, 순서를 바꾸면 다른 동작이 발생 → 아래 샘플은 위와 동등하지 않음

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

### [@types](https://www.npmjs.com/~types)에 배포하기

- [@types](https://www.npmjs.com/~types) 조직의 패키지는 [types-publisher 도구](https://github.com/microsoft/DefinitelyTyped-tools/tree/master/packages/publisher)를 사용하여 [DefinitelyTyped](https://github.com/DefinitelyTyped/DefinitelyTyped)에서 자동으로 배포됨
  - 선언을 @types 패키지로 배포하려면 [DefinitelyTyped](https://github.com/DefinitelyTyped/DefinitelyTyped)에 풀 리퀘스트 제출
  - [기여 가이드라인 페이지](https://definitelytyped.github.io/guides/contributing.html)에서 자세한 내용 확인 가능

---

## 사용하기

> **원문:** https://www.typescriptlang.org/docs/handbook/declaration-files/consumption.html

### 다운로드

- 타입 선언을 가져오는 데는 npm 외에 다른 도구 불필요
- 예: lodash와 같은 라이브러리의 선언을 가져오려면 다음 명령만 필요

```cmd
npm install --save-dev @types/lodash
```

- npm 패키지가 이미 [배포하기](/docs/handbook/declaration-files/publishing.html)에서 설명한 대로 선언 파일을 포함하고 있다면 → 해당 `@types` 패키지를 다운로드할 필요 없음

### 사용하기

- 이후 TypeScript 코드에서 lodash를 문제없이 사용 가능 (모듈과 전역 코드 모두에서 작동)
- 예: 타입 선언을 `npm install`한 후에는 import를 사용하여 작성 가능

```ts
import * as _ from "lodash";
_.padStart("Hello TypeScript!", 20, " ");
```

- 또는 모듈을 사용하지 않는 경우, 전역 변수 `_`를 그냥 사용 가능

```ts
_.padStart("Hello TypeScript!", 20, " ");
```

### 검색

- 대부분의 경우 타입 선언 패키지는 항상 `npm`의 패키지 이름과 동일한 이름을 가지되 `@types/` 접두사가 붙음
  - 필요한 경우 [Yarn 패키지 검색](https://yarnpkg.com/)을 사용하여 원하는 라이브러리의 패키지를 찾을 수 있음

> 참고: 찾고 있는 선언 파일이 없는 경우, 언제든지 하나를 기여하고 다음에 찾는 개발자를 도울 수 있습니다. 자세한 내용은 DefinitelyTyped [기여 가이드라인 페이지](https://definitelytyped.org/guides/contributing.html)를 참조하세요.

---

## 타입 시스템 레퍼런스: 추론 · 호환성 · 유틸리티 타입 · 선언 병합 · 트리플 슬래시

## 타입 추론 (Type Inference)

> **원문:** https://www.typescriptlang.org/docs/handbook/type-inference.html

- TypeScript에서 명시적인 타입 주석이 없을 때 타입 정보를 제공하기 위해 타입 추론이 사용되는 곳이 여러 존재
  - 예: 다음 코드에서

```ts
let x = 3;
//  ^? let x: number
```

- `x` 변수의 타입은 `number`로 추론됨
  - 이러한 종류의 추론은 변수와 멤버를 초기화하고, 매개변수 기본값을 설정하고, 함수 반환 타입을 결정할 때 발생
- 대부분의 경우 타입 추론은 간단함. 다음 섹션에서는 타입이 추론되는 방식의 일부 뉘앙스를 다룸

### 최적 공통 타입

- 여러 표현식에서 타입 추론이 이루어질 때 → 해당 표현식의 타입을 사용하여 "최적 공통 타입"을 계산

```ts
let x = [0, 1, null];
//  ^? let x: (number | null)[]
```

- 위 예제에서 `x`의 타입을 추론하려면 각 배열 요소의 타입을 고려해야 함
  - 배열 타입에 대해 두 가지 선택이 주어짐: `number`와 `null`
  - 최적 공통 타입 알고리즘은 각 후보 타입을 고려하고 다른 모든 후보와 호환되는 타입을 선택
- 최적 공통 타입은 제공된 후보 타입에서 선택해야 하므로, 타입이 공통 구조를 공유하지만 모든 후보 타입의 슈퍼 타입인 타입이 없는 경우가 있음

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

- 이상적으로는 `zoo`가 `Animal[]`로 추론되기를 원할 수 있지만, 배열에 `Animal` 타입의 객체가 엄격하게 없기 때문에 배열 요소 타입에 대한 추론을 하지 않음
  - 이것을 수정하려면 모든 다른 후보의 슈퍼 타입인 타입이 없을 때 명시적으로 타입을 제공

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

- 최적 공통 타입을 찾을 수 없으면 결과 추론은 유니온 배열 타입 `(Rhino | Elephant | Snake)[]`

### 컨텍스트적 타이핑

- 타입 추론은 TypeScript의 일부 경우에 "반대 방향"으로도 작동 → "컨텍스트적 타이핑"
  - 표현식의 타입이 해당 위치에 의해 암시될 때 발생

```ts
// @errors: 2339
window.onmousedown = function (mouseEvent) {
  console.log(mouseEvent.button);
  console.log(mouseEvent.kangaroo);
};
```

- 여기서 TypeScript 타입 체커는 `Window.onmousedown` 함수의 타입을 사용하여 할당의 오른쪽에 있는 함수 표현식의 타입을 추론
  - 그렇게 하여 `button` 속성을 포함하지만 `kangaroo` 속성는 포함하지 않는 `mouseEvent` 매개변수의 [타입](https://developer.mozilla.org/docs/Web/API/MouseEvent)을 추론 가능
- 이것은 window에 이미 타입에 `onmousedown`이 선언되어 있기 때문에 작동

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

- TypeScript는 다른 컨텍스트에서도 타입을 추론할 만큼 정교함

```ts
// @errors: 2339
window.onscroll = function (uiEvent) {
  console.log(uiEvent.button);
};
```

- 위의 함수가 `Window.onscroll`에 할당되고 있다는 사실을 기반으로, TypeScript는 `uiEvent`가 이전 예제의 [MouseEvent](https://developer.mozilla.org/docs/Web/API/MouseEvent)가 아닌 [UIEvent](https://developer.mozilla.org/docs/Web/API/UIEvent)임을 인지
  - `UIEvent` 객체에는 `button` 속성이 없으므로 TypeScript가 오류를 발생
- 이 함수가 컨텍스트적으로 타입이 지정된 위치에 없었다면, 함수의 인수는 암시적으로 `any` 타입을 가지며 오류 미발생 ([`noImplicitAny`](/tsconfig#noImplicitAny) 옵션을 사용하지 않는 한)

```ts
// @noImplicitAny: false
const handler = function (uiEvent) {
  console.log(uiEvent.button); // <- OK
};
```

- 함수의 인수에 명시적으로 타입 정보를 제공하여 컨텍스트적 타입을 재정의할 수도 있음

```ts
window.onscroll = function (uiEvent: any) {
  console.log(uiEvent.button); // <- 이제 오류가 없습니다
};
```

- 다만 `uiEvent`에 `button`이라는 속성이 없으므로 이 코드는 `undefined`를 로깅
- 컨텍스트적 타이핑은 많은 경우에 적용
  - 일반적인 경우: 함수 호출에 대한 인수, 할당의 오른쪽, 타입 단언, 객체 및 배열 리터럴의 멤버, 반환 문
  - 컨텍스트적 타입은 최적 공통 타입에서 후보 타입으로도 작동

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

- 이 예제에서 최적 공통 타입은 네 가지 후보 집합을 가짐: `Animal`, `Rhino`, `Elephant`, `Snake`
  - 이 중에서 `Animal`이 최적 공통 타입 알고리즘에 의해 선택됨

---

## 타입 호환성 (Type Compatibility)

> **원문:** https://www.typescriptlang.org/docs/handbook/type-compatibility.html

- TypeScript의 타입 호환성은 구조적 서브타이핑을 기반
  - 구조적 타이핑: 멤버만을 기반으로 타입을 관련짓는 방법 (명목적 타이핑과 대조)

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

- C#이나 Java와 같은 명목적 타입 언어에서는, `Dog` 클래스가 `Pet` 인터페이스의 구현자로 명시적으로 자신을 설명하지 않기 때문에 동등한 코드가 오류가 됨
- TypeScript의 구조적 타입 시스템은 JavaScript 코드가 일반적으로 작성되는 방식을 기반으로 설계됨
  - JavaScript가 함수 표현식과 객체 리터럴과 같은 익명 객체를 널리 사용 → JavaScript 라이브러리에서 발견되는 관계 종류를 명목적 타입 시스템 대신 구조적 타입 시스템으로 표현하는 것이 훨씬 더 자연스러움

### 건전성에 대한 참고

- TypeScript의 타입 시스템은 컴파일 시간에 안전하다고 알 수 없는 특정 연산을 허용
  - 타입 시스템이 이러한 특성을 가지면 "건전"하지 않다고 표현
- TypeScript가 건전하지 않은 동작을 허용하는 곳은 신중하게 고려됨 → 이 문서 전체에서 이러한 일이 발생하는 곳과 그 동기 부여 시나리오를 설명

### 시작하기

- TypeScript의 구조적 타입 시스템의 기본 규칙: `y`가 최소한 `x`와 같은 멤버가 있으면 `x`가 `y`와 호환
  - 예: `name` 속성을 가진 `Pet`이라는 인터페이스가 있는 다음 코드

```ts
interface Pet {
  name: string;
}

let pet: Pet;
// dog의 추론된 타입은 { name: string; owner: string; }입니다
let dog = { name: "Lassie", owner: "Rudd Weatherwax" };
pet = dog;
```

- `dog`가 `pet`에 할당될 수 있는지 확인하기 위해 컴파일러는 `pet`의 각 속성을 확인하여 `dog`에서 해당하는 호환 가능한 속성을 찾음
  - 이 경우 `dog`는 문자열인 `name`이라는 멤버가 있어야 함 → 있으므로 할당이 허용됨
- 함수 호출 인수를 확인할 때도 동일한 할당 규칙이 사용됨

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

- `dog`에 추가 `owner` 속성이 있지만 이것이 오류를 생성하지 않음에 유의
  - 호환성을 확인할 때는 대상 타입(`Pet`의 경우)의 멤버만 고려 → 이 비교 프로세스는 각 멤버와 하위 멤버의 타입을 탐색하며 재귀적으로 진행
- 그러나 객체 리터럴은 [알려진 속성만 지정할 수 있음](/docs/handbook/2/objects.html#excess-property-checks)
  - 예: `dog`의 타입이 명시적으로 `Pet`으로 지정되었기 때문에 다음 코드는 유효하지 않음

```ts
let dog: Pet = { name: "Lassie", owner: "Rudd Weatherwax" }; // 오류
```

### 두 함수 비교

- 원시 타입과 객체 타입을 비교하는 것은 비교적 간단하지만, 어떤 종류의 함수가 호환 가능한 것으로 간주되어야 하는지는 조금 더 복잡
- 매개변수 목록만 다른 두 함수의 기본 예제부터 시작

```ts
let x = (a: number) => 0;
let y = (b: number, s: string) => 0;

y = x; // OK
x = y; // 오류
```

- `x`가 `y`에 할당 가능한지 확인하기 위해 먼저 매개변수 목록을 확인
  - `x`의 각 매개변수는 호환 가능한 타입의 해당 매개변수가 `y`에 있어야 함
  - 매개변수의 이름은 고려되지 않으며 타입만 고려됨
  - 이 경우 `x`의 모든 매개변수에 해당하는 호환 가능한 매개변수가 `y`에 있으므로 할당이 허용됨
- 두 번째 할당은 `y`에 `x`에 없는 필수 두 번째 매개변수가 있으므로 오류 → 할당이 허용되지 않음
- 예제 `y = x`처럼 매개변수를 '버리는' 것이 허용되는 이유
  - JavaScript에서 추가 함수 매개변수를 무시하는 것이 실제로 매우 일반적이기 때문
  - 예: `Array#forEach`는 콜백 함수에 배열 요소, 인덱스, 포함하는 배열의 세 가지 매개변수를 제공
  - 그럼에도 첫 번째 매개변수만 사용하는 콜백을 제공하는 것은 매우 유용함

```ts
let items = [1, 2, 3];

// 이러한 추가 매개변수를 강제하지 마세요
items.forEach((item, index, array) => console.log(item));

// 괜찮아야 합니다!
items.forEach((item) => console.log(item));
```

- 이제 반환 타입만 다른 두 함수를 사용하여 반환 타입 처리 방식을 확인

```ts
let x = () => ({ name: "Alice" });
let y = () => ({ name: "Alice", location: "Seattle" });

x = y; // OK
y = x; // 오류, x()에 location 속성가 없습니다
```

- 타입 시스템은 소스 함수의 반환 타입이 대상 타입의 반환 타입의 하위 타입이어야 함을 강제

#### 함수 매개변수 이변성

- 함수 매개변수의 타입을 비교할 때, 소스 매개변수가 대상 매개변수에 할당 가능하거나 그 반대인 경우 할당이 성공
  - 이것은 건전하지 않음: 호출자가 더 특수화된 타입을 취하는 함수가 주어지지만 덜 특수화된 타입으로 함수를 호출할 수 있기 때문
  - 실제로 이러한 종류의 오류는 드물며, 이를 허용하면 많은 일반적인 JavaScript 패턴이 가능해짐

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

- 컴파일러 플래그 [`strictFunctionTypes`](/tsconfig#strictFunctionTypes)를 통해 이런 일이 발생할 때 TypeScript가 오류를 발생시키도록 설정 가능

#### 선택적 매개변수와 나머지 매개변수

- 호환성을 위해 함수를 비교할 때, 선택적 매개변수와 필수 매개변수는 상호 교환 가능
  - 소스 타입의 추가 선택적 매개변수는 오류가 아니며, 소스 타입에 해당 매개변수가 없는 대상 타입의 선택적 매개변수도 오류가 아님
- 함수에 나머지 매개변수가 있으면 마치 무한한 일련의 선택적 매개변수인 것처럼 처리
- 이것은 타입 시스템 관점에서 건전하지 않지만, 런타임 관점에서 선택적 매개변수의 아이디어는 일반적으로 잘 적용되지 않음
  - 이유: 해당 위치에 `undefined`를 전달하는 것이 대부분의 함수에서 동등하기 때문
- 동기 부여 예제: 콜백을 받고 프로그래머에게는 예측 가능하지만 타입 시스템에는 알려지지 않은 일부 인수로 호출하는 함수의 일반적인 패턴

```ts
function invokeLater(args: any[], callback: (...args: any[]) => void) {
  /* ... 'args'로 callback 호출 ... */
}

// 건전하지 않음 - invokeLater가 임의의 수의 인수를 제공"할 수 있음"
invokeLater([1, 2], (x, y) => console.log(x + ", " + y));

// 혼란스러움 (x와 y는 실제로 필수임) 그리고 발견하기 어려움
invokeLater([1, 2], (x?, y?) => console.log(x + ", " + y));
```

#### 오버로드가 있는 함수

- 함수에 오버로드가 있으면, 대상 타입의 각 오버로드는 소스 타입의 호환 가능한 시그니처와 매치되어야 함
  - 이것은 소스 함수가 대상 함수와 동일한 모든 경우에 호출될 수 있음을 보장

### 열거형

- 열거형은 숫자와 호환되고 숫자는 열거형과 호환됨
  - 다른 열거형 타입의 열거형 값은 호환되지 않는 것으로 간주

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

### 클래스

- 클래스는 한 가지 예외를 제외하고 객체 리터럴 타입 및 인터페이스와 유사하게 작동: 정적 타입과 인스턴스 타입이 모두 있음
  - 클래스 타입의 두 객체를 비교할 때 인스턴스의 멤버만 비교됨 → 정적 멤버와 생성자는 호환성에 영향을 미치지 않음

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

#### 클래스의 private 및 protected 멤버

- 클래스의 private 및 protected 멤버는 호환성에 영향을 미침
  - 클래스의 인스턴스가 호환성을 확인할 때, 대상 타입에 private 멤버가 포함된 경우 → 소스 타입에도 동일한 클래스에서 유래한 private 멤버가 포함되어야 함
  - protected 멤버가 있는 인스턴스에도 동일하게 적용
- 이것은 클래스가 슈퍼 클래스와 할당 호환되게 하지만, 그렇지 않으면 동일한 형태를 가진 다른 상속 계층의 클래스와는 호환되지 _않음_

### 제네릭

- TypeScript는 구조적 타입 시스템이므로, 타입 매개변수는 멤버 타입의 일부로 소비될 때만 결과 타입에 영향을 미침

```ts
interface Empty<T> {}
let x: Empty<number>;
let y: Empty<string>;

x = y; // OK, y가 x의 구조와 일치하기 때문
```

- 위에서 `x`와 `y`는 구조가 타입 인수를 차별화하는 방식으로 사용하지 않기 때문에 호환됨
- `Empty<T>`에 멤버를 추가하여 이 예제를 변경하면 작동 방식이 드러남

```ts
interface NotEmpty<T> {
  data: T;
}
let x: NotEmpty<number>;
let y: NotEmpty<string>;

x = y; // 오류, x와 y가 호환되지 않기 때문
```

- 이 방식으로 타입 인수가 지정된 제네릭 타입은 비제네릭 타입처럼 동작
- 타입 인수가 지정되지 않은 제네릭 타입의 경우, 지정되지 않은 모든 타입 인수 대신 `any`를 지정하여 호환성을 확인
  - 그런 다음 결과 타입이 비제네릭 경우와 마찬가지로 호환성을 확인

```ts
let identity = function <T>(x: T): T {
  // ...
};

let reverse = function <U>(y: U): U {
  // ...
};

identity = reverse; // OK, (x: any) => any가 (y: any) => any와 일치하기 때문
```

### 고급 주제

#### 서브타입 vs 할당

- 지금까지 "호환"을 사용했는데, 이것은 언어 명세에 정의된 용어가 아님
- TypeScript에는 두 종류의 호환성이 있음: 서브타입과 할당
  - 이들은 `any`와의 상호 할당, 그리고 같은 숫자 값을 가진 `enum` 간의 할당을 허용하는 규칙만큼만 서브타입 호환성을 확장한다는 점에서 다름
- 언어의 다른 위치에서는 상황에 따라 두 호환성 메커니즘 중 하나를 사용
  - 실용적인 목적으로, 타입 호환성은 `implements` 및 `extends` 절의 경우에도 할당 호환성에 의해 지시됨

### `any`, `unknown`, `object`, `void`, `undefined`, `null`, `never` 할당 가능성

- 일부 추상 타입 간의 할당 가능성 (행 = 출발 타입, 열 = 도착 타입 기준 할당 가능 여부)
  - `any` ->: unknown 가능 · object 가능 · void 가능 · undefined 가능 · null 가능 · never 불가
  - `unknown` ->: any 가능 · object 불가 · void 불가 · undefined 불가 · null 불가 · never 불가
  - `object` ->: any 가능 · unknown 가능 · void 불가 · undefined 불가 · null 불가 · never 불가
  - `void` ->: any 가능 · unknown 가능 · object 불가 · undefined 불가 · null 불가 · never 불가
  - `undefined` ->: any 가능 · unknown 가능 · object 가능(단, [`strictNullChecks`](/tsconfig#strictNullChecks) 꺼져 있을 때만) · void 가능 · null 가능(단, strictNullChecks 꺼져 있을 때만) · never 불가
  - `null` ->: any 가능 · unknown 가능 · object 가능(단, strictNullChecks 꺼져 있을 때만) · void 가능(단, strictNullChecks 꺼져 있을 때만) · undefined 가능(단, strictNullChecks 꺼져 있을 때만) · never 불가
  - `never` ->: any 가능 · unknown 가능 · object 가능 · void 가능 · undefined 가능 · null 가능 (never는 모든 것에 할당 가능)
- [기초](/docs/handbook/2/basic-types.html) 요약
  - 모든 것은 자기 자신에게 할당 가능
  - `any`와 `unknown`은 무엇이 할당 가능한지에 관해서는 동일하지만, `unknown`은 `any`를 제외한 어떤 것에도 할당 가능하지 않음
  - `unknown`과 `never`는 서로의 역과 같음: 모든 것이 `unknown`에 할당 가능하고 `never`는 모든 것에 할당 가능
    - 어떤 것도 `never`에 할당 가능하지 않고, `unknown`은 (`any`를 제외하고) 어떤 것에도 할당 가능하지 않음
  - `void`는 다음 예외를 제외하고 무엇에도 할당 가능하지 않고 무엇으로부터도 할당 가능하지 않음: `any`, `unknown`, `never`, `undefined`, `null` ([`strictNullChecks`](/tsconfig#strictNullChecks)가 꺼져 있으면 예외 확대, 자세한 내용은 위 목록 참고)
  - [`strictNullChecks`](/tsconfig#strictNullChecks)가 꺼져 있으면, `null`과 `undefined`는 `never`와 유사: 대부분의 타입에 할당 가능하지만 대부분의 타입은 `null`과 `undefined`에 할당 불가
    - `null`과 `undefined`는 서로 할당 가능
  - [`strictNullChecks`](/tsconfig#strictNullChecks)가 켜져 있으면, `null`과 `undefined`는 `void`처럼 더 동작: `any`, `unknown`, `void`를 제외하고 어떤 것에도 할당 가능하지 않고 어떤 것으로부터도 할당 가능하지 않음 (`undefined`는 항상 `void`에 할당 가능)

---

## 유틸리티 타입 (Utility Types)

> **원문:** https://www.typescriptlang.org/docs/handbook/utility-types.html

- TypeScript는 일반적인 타입 변환을 용이하게 하기 위한 여러 유틸리티 타입을 제공
  - 이러한 유틸리티들은 전역적으로 사용 가능

### `Awaited<Type>`

> 릴리스: [4.5](/docs/handbook/release-notes/typescript-4-5.html#the-awaited-type-and-promise-improvements)

- `async` 함수의 `await`나 `Promise`의 `.then()` 메서드와 같은 연산을 모델링하기 위한 타입
  - `Promise`를 재귀적으로 언래핑하는 방식을 다룸

###### 예제

```ts
type A = Awaited<Promise<string>>;
//   ^? type A = string

type B = Awaited<Promise<Promise<number>>>;
//   ^? type B = number

type C = Awaited<boolean | Promise<number>>;
//   ^? type C = number | boolean
```

### `Partial<Type>`

> 릴리스: [2.1](/docs/handbook/release-notes/typescript-2-1.html#partial-readonly-record-and-pick)

- `Type`의 모든 속성을 선택적으로 설정한 타입을 생성
  - 주어진 타입의 모든 하위 집합을 나타내는 타입을 반환

###### 예제

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

### `Required<Type>`

> 릴리스: [2.8](/docs/handbook/release-notes/typescript-2-8.html#improved-control-over-mapped-type-modifiers)

- `Type`의 모든 속성을 필수로 설정한 타입을 생성. [`Partial`](#partialtype)의 반대

###### 예제

```ts
// @errors: 2741
interface Props {
  a?: number;
  b?: string;
}

const obj: Props = { a: 5 };

const obj2: Required<Props> = { a: 5 };
```

### `Readonly<Type>`

> 릴리스: [2.1](/docs/handbook/release-notes/typescript-2-1.html#partial-readonly-record-and-pick)

- `Type`의 모든 속성을 `readonly`로 설정한 타입을 생성
  - 생성된 타입의 속성을 재할당할 수 없음을 의미

###### 예제

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

- 이 유틸리티는 런타임에 실패할 할당 표현식을 표현하는 데 유용 (예: [동결된 객체](https://developer.mozilla.org/docs/Web/JavaScript/Reference/Global_Objects/Object/freeze)의 속성을 재할당하려고 할 때)

###### `Object.freeze`

```ts
function freeze<Type>(obj: Type): Readonly<Type>;
```

### `Record<Keys, Type>`

> 릴리스: [2.1](/docs/handbook/release-notes/typescript-2-1.html#partial-readonly-record-and-pick)

- 속성 키가 `Keys`이고 속성 값이 `Type`인 객체 타입을 생성
  - 타입의 속성을 다른 타입으로 매핑하는 데 사용 가능

###### 예제

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

### `Pick<Type, Keys>`

> 릴리스: [2.1](/docs/handbook/release-notes/typescript-2-1.html#partial-readonly-record-and-pick)

- `Type`에서 속성 `Keys`의 집합(문자열 리터럴 또는 문자열 리터럴의 유니온)을 선택하여 타입을 생성

###### 예제

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

### `Omit<Type, Keys>`

> 릴리스: [3.5](/docs/handbook/release-notes/typescript-3-5.html#the-omit-helper-type)

- `Type`에서 모든 속성을 선택한 다음 `Keys`(문자열 리터럴 또는 문자열 리터럴의 유니온)를 제거하여 타입을 생성. [`Pick`](#picktype-keys)의 반대

###### 예제

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

### `Exclude<UnionType, ExcludedMembers>`

> 릴리스: [2.8](/docs/handbook/release-notes/typescript-2-8.html#predefined-conditional-types)

- `UnionType`에서 `ExcludedMembers`에 할당 가능한 모든 유니온 멤버를 제외하여 타입을 생성

###### 예제

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

### `Extract<Type, Union>`

> 릴리스: [2.8](/docs/handbook/release-notes/typescript-2-8.html#predefined-conditional-types)

- `Type`에서 `Union`에 할당 가능한 모든 유니온 멤버를 추출하여 타입을 생성

###### 예제

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

### `NonNullable<Type>`

> 릴리스: [2.8](/docs/handbook/release-notes/typescript-2-8.html#predefined-conditional-types)

- `Type`에서 `null`과 `undefined`를 제외하여 타입을 생성

###### 예제

```ts
type T0 = NonNullable<string | number | undefined>;
//    ^? type T0 = string | number
type T1 = NonNullable<string[] | null | undefined>;
//    ^? type T1 = string[]
```

### `Parameters<Type>`

> 릴리스: [3.1](https://github.com/microsoft/TypeScript/pull/26243)

- 함수 타입 `Type`의 매개변수에 사용된 타입들로 튜플 타입을 생성
- 오버로드된 함수의 경우 이것은 _마지막_ 시그니처의 매개변수가 됨. [조건부 타입 내에서 추론하기](/docs/handbook/2/conditional-types.html#inferring-within-conditional-types) 참조

###### 예제

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

### `ConstructorParameters<Type>`

> 릴리스: [3.1](https://github.com/microsoft/TypeScript/pull/26243)

- 생성자 함수 타입의 타입들로 튜플 또는 배열 타입을 생성
  - 모든 매개변수 타입을 가진 튜플 타입을 생성 (또는 `Type`이 함수가 아닌 경우 `never` 타입)

###### 예제

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

### `ReturnType<Type>`

> 릴리스: [2.8](/docs/handbook/release-notes/typescript-2-8.html#predefined-conditional-types)

- 함수 `Type`의 반환 타입으로 구성된 타입을 생성
- 오버로드된 함수의 경우 이것은 _마지막_ 시그니처의 반환 타입이 됨. [조건부 타입 내에서 추론하기](/docs/handbook/2/conditional-types.html#inferring-within-conditional-types) 참조

###### 예제

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

### `InstanceType<Type>`

> 릴리스: [2.8](/docs/handbook/release-notes/typescript-2-8.html#predefined-conditional-types)

- `Type`에서 생성자 함수의 인스턴스 타입으로 구성된 타입을 생성

###### 예제

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

### `NoInfer<Type>`

> 릴리스: [5.4](/docs/handbook/release-notes/typescript-5-4.html#the-noinfer-utility-type)

- 포함된 타입에 대한 추론을 차단
  - 추론을 차단하는 것 외에 `NoInfer<Type>`은 `Type`과 동일

###### 예제

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

### `ThisParameterType<Type>`

> 릴리스: [3.3](https://github.com/microsoft/TypeScript/pull/28920)

- 함수 타입에 대한 [this](/docs/handbook/functions.html#this-parameters) 매개변수의 타입을 추출
  - 함수 타입에 `this` 매개변수가 없는 경우 [unknown](/docs/handbook/release-notes/typescript-3-0.html#new-unknown-top-type)을 반환

###### 예제

```ts
function toHex(this: Number) {
  return this.toString(16);
}

function numberToString(n: ThisParameterType<typeof toHex>) {
  return toHex.apply(n);
}
```

### `OmitThisParameter<Type>`

> 릴리스: [3.3](https://github.com/microsoft/TypeScript/pull/28920)

- `Type`에서 [`this`](/docs/handbook/functions.html#this-parameters) 매개변수를 제거
  - `Type`에 명시적으로 선언된 `this` 매개변수가 없는 경우, 결과는 단순히 `Type`
  - 그렇지 않으면 `Type`에서 `this` 매개변수가 없는 새로운 함수 타입이 생성됨
  - 제네릭은 지워지고 마지막 오버로드 시그니처만 새로운 함수 타입으로 전파됨

###### 예제

```ts
function toHex(this: Number) {
  return this.toString(16);
}

const fiveToHex: OmitThisParameter<typeof toHex> = toHex.bind(5);

console.log(fiveToHex());
```

### `ThisType<Type>`

> 릴리스: [2.3](https://github.com/microsoft/TypeScript/pull/14141)

- 이 유틸리티는 변환된 타입을 반환하지 않음
  - 대신 컨텍스트적 [`this`](/docs/handbook/functions.html#this) 타입의 마커 역할
  - 이 유틸리티를 사용하려면 [`noImplicitThis`](/tsconfig#noImplicitThis) 플래그가 활성화되어야 함

###### 예제

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

- 위의 예제에서 `makeObject`에 대한 인수의 `methods` 객체는 `ThisType<D & M>`을 포함하는 컨텍스트적 타입을 가짐
  - 따라서 `methods` 객체 내의 메서드에서 [this](/docs/handbook/functions.html#this)의 타입은 `{ x: number, y: number } & { moveBy(dx: number, dy: number): void }`
  - `methods` 속성의 타입이 동시에 추론 대상이자 메서드 내의 `this` 타입의 소스임에 유의
- `ThisType<T>` 마커 인터페이스는 단순히 `lib.d.ts`에 선언된 빈 인터페이스
  - 객체 리터럴의 컨텍스트적 타입에서 인식되는 것 외에, 이 인터페이스는 다른 빈 인터페이스처럼 동작

### 내장 문자열 조작 타입

#### `Uppercase<StringType>`

#### `Lowercase<StringType>`

#### `Capitalize<StringType>`

#### `Uncapitalize<StringType>`

- 템플릿 문자열 리터럴을 둘러싼 문자열 조작을 돕기 위해, TypeScript는 타입 시스템 내에서 문자열 조작에 사용할 수 있는 타입 집합을 포함
  - 이러한 타입은 [템플릿 리터럴 타입](/docs/handbook/2/template-literal-types.html#uppercasestringtype) 문서에서 확인 가능

---

## 선언 병합 (Declaration Merging)

> **원문:** https://www.typescriptlang.org/docs/handbook/declaration-merging.html

### 소개

- TypeScript의 독특한 개념 중 일부는 타입 수준에서 JavaScript 객체의 형태를 설명
  - 특히 고유한 예: '선언 병합' 개념
  - 이 개념을 이해하면 기존 JavaScript로 작업할 때 이점을 얻을 수 있고, 더 고급 추상화 개념으로의 문도 열림
- 이 글에서 "선언 병합"은 컴파일러가 동일한 이름으로 선언된 두 개의 별도 선언을 단일 정의로 병합한다는 의미
  - 이 병합된 정의는 원래 두 선언의 기능이 모두 있음
  - 병합할 수 있는 선언의 수에는 제한이 없음 (두 선언에만 국한되지 않음)

### 기본 개념

- TypeScript에서 선언은 네임스페이스, 타입, 값의 세 그룹 중 하나 이상에 엔티티를 생성
  - 네임스페이스 생성 선언: 점 표기법을 사용하여 접근하는 이름을 포함하는 네임스페이스를 생성
  - 타입 생성 선언: 선언된 형태로 표시되고 주어진 이름에 바인딩된 타입을 생성
  - 값 생성 선언: 출력 JavaScript에서 볼 수 있는 값을 생성
- 각 선언 타입이 만드는 것(네임스페이스/타입/값 여부)은 다음과 같음
  - Namespace: 네임스페이스 생성, 값 생성
  - Class: 타입 생성, 값 생성
  - Enum: 타입 생성, 값 생성
  - Interface: 타입 생성
  - Type Alias: 타입 생성
  - Function: 값 생성
  - Variable: 값 생성
- 각 선언으로 무엇이 생성되는지 이해하면 선언 병합 시 무엇이 병합되는지 이해하는 데 도움이 됨

### 인터페이스 병합

- 가장 단순하고 아마도 가장 일반적인 선언 병합 유형은 인터페이스 병합
  - 가장 기본적인 수준에서 병합은 기계적으로 두 선언의 멤버를 동일한 이름의 단일 인터페이스로 결합

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

- 인터페이스의 비함수 멤버는 고유해야 함
  - 고유하지 않은 경우 동일한 타입이어야 함
  - 인터페이스가 동일한 이름의 비함수 멤버를 선언하지만 다른 타입인 경우 컴파일러가 오류를 발생
- 함수 멤버의 경우 동일한 이름의 각 함수 멤버는 동일한 함수의 오버로드를 설명하는 것으로 처리
  - 인터페이스 `A`가 나중의 인터페이스 `A`와 병합되는 경우, 두 번째 인터페이스가 첫 번째 인터페이스보다 더 높은 우선순위를 가짐

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

- 세 인터페이스가 병합되어 다음과 같은 단일 선언이 생성됨

```ts
interface Cloner {
  clone(animal: Dog): Dog;
  clone(animal: Cat): Cat;
  clone(animal: Sheep): Sheep;
  clone(animal: Animal): Animal;
}
```

- 각 그룹의 요소가 동일한 순서를 유지하지만, 그룹 자체는 나중의 오버로드 세트가 먼저 정렬되어 병합
- 이 규칙의 한 가지 예외는 특수화된 시그니처
  - 시그니처에 _단일_ 문자열 리터럴 타입(문자열 리터럴의 유니온이 아닌 경우)인 매개변수가 있으면 병합된 오버로드 목록의 맨 위로 버블링됨
- 예: 다음 인터페이스들이 함께 병합됨

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

- 결과적으로 `Document`의 병합된 선언은 다음과 같음

```ts
interface Document {
  createElement(tagName: "canvas"): HTMLCanvasElement;
  createElement(tagName: "div"): HTMLDivElement;
  createElement(tagName: "span"): HTMLSpanElement;
  createElement(tagName: string): HTMLElement;
  createElement(tagName: any): Element;
}
```

### 네임스페이스 병합

- 인터페이스와 마찬가지로 동일한 이름의 네임스페이스도 멤버를 병합
  - 네임스페이스는 네임스페이스와 값을 모두 생성하므로 둘 다 어떻게 병합되는지 이해 필요
- 네임스페이스를 병합하려면, 각 네임스페이스에서 선언된 내보낸 인터페이스의 타입 정의가 자체적으로 병합되어 병합된 인터페이스 정의가 내부에 있는 단일 네임스페이스를 형성
- 네임스페이스 값을 병합하려면, 각 선언 사이트에서 주어진 이름의 네임스페이스가 이미 존재하는 경우, 기존 네임스페이스를 가져와서 두 번째 네임스페이스의 내보낸 멤버를 첫 번째에 추가하여 확장
- 예: 이 예제에서 `Animals`의 선언 병합

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

은 다음과 같음

```ts
namespace Animals {
  export interface Legged {
    numberOfLegs: number;
  }

  export class Zebra {}
  export class Dog {}
}
```

- 이 네임스페이스 병합 모델은 유용한 출발점이지만, 내보내지 않은 멤버에 어떤 일이 발생하는지도 이해 필요
  - 내보내지 않은 멤버는 원래의 (병합되지 않은) 네임스페이스에서만 볼 수 있음
  - 병합 후, 다른 선언에서 온 병합된 멤버는 내보내지 않은 멤버를 볼 수 없음
- 다음 예제에서 이것을 더 명확하게 확인 가능

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

- `haveMuscles`가 내보내지지 않았기 때문에, 동일한 병합되지 않은 네임스페이스를 공유하는 `animalsHaveMuscles` 함수만 이 심볼을 볼 수 있음
  - `doAnimalsHaveMuscles` 함수는 병합된 `Animal` 네임스페이스의 일부이지만 이 내보내지 않은 멤버를 볼 수 없음

### 네임스페이스와 클래스, 함수, 열거형 병합

- 네임스페이스는 다른 유형의 선언과도 병합할 수 있을 만큼 유연함
  - 그렇게 하려면 네임스페이스 선언이 병합할 선언 뒤에 와야 함
  - 결과 선언은 두 선언 타입의 속성을 모두 가짐
- TypeScript는 이 기능을 사용하여 JavaScript와 다른 프로그래밍 언어의 일부 패턴을 모델링

#### 네임스페이스와 클래스 병합

- 이것은 사용자에게 내부 클래스를 설명하는 방법을 제공

```ts
class Album {
  label: Album.AlbumLabel;
}
namespace Album {
  export class AlbumLabel {}
}
```

- 병합된 멤버의 가시성 규칙은 [네임스페이스 병합](./declaration-merging.html#네임스페이스-병합) 섹션에 설명된 것과 동일
  - 병합된 클래스가 볼 수 있도록 `AlbumLabel` 클래스를 내보내야 함
  - 최종 결과는 다른 클래스 내부에서 관리되는 클래스
  - 네임스페이스를 사용하여 기존 클래스에 더 많은 정적 멤버를 추가할 수도 있음
- 내부 클래스 패턴 외에도, 함수를 만든 다음 함수에 속성을 추가하여 함수를 더 확장하는 JavaScript 관행이 있음
  - TypeScript는 선언 병합을 사용하여 이와 같은 정의를 타입 안전한 방식으로 구축

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

- 마찬가지로 네임스페이스는 열거형을 정적 멤버로 확장하는 데 사용 가능

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

### 허용되지 않는 병합

- TypeScript에서 모든 병합이 허용되는 것은 아님
  - 현재 클래스는 다른 클래스나 변수와 병합할 수 없음
  - 클래스 병합을 모방하는 방법은 [TypeScript의 믹스인](/docs/handbook/mixins.html) 섹션 참조

### 모듈 확장

- JavaScript 모듈은 병합을 지원하지 않지만, 기존 객체를 가져온 다음 업데이트하여 패치 가능
- 장난감 Observable 예제

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

- 이것은 TypeScript에서도 잘 작동하지만, 컴파일러는 `Observable.prototype.map`에 대해 알지 못함
  - 모듈 확장을 사용하여 컴파일러에게 이에 대해 알릴 수 있음

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

- 모듈 이름은 `import`/`export`의 모듈 지정자와 동일한 방식으로 해석됨
  - 자세한 내용은 [모듈](/docs/handbook/modules.html) 참조
- 확장의 선언은 원본 파일에서 선언된 것처럼 병합됨
- 다만 두 가지 제한 사항이 있음
  1. 확장에서 새 최상위 선언을 선언할 수 없음 (기존 선언에 대한 패치만 가능)
  2. 기본 내보내기도 확장할 수 없음. 명명된 내보내기만 가능 (내보낸 이름으로 내보내기를 확장해야 하며, `default`는 예약어이기 때문 — 자세한 내용은 [#14080](https://github.com/Microsoft/TypeScript/issues/14080) 참조)

#### 전역 확장

- 모듈 내부에서 전역 범위에 선언을 추가할 수도 있음

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

- 전역 확장은 모듈 확장과 동일한 동작 및 제한을 가짐

---

## 삼중 슬래시 지시어 (Triple-Slash Directives)

> **원문:** https://www.typescriptlang.org/docs/handbook/triple-slash-directives.html

- 삼중 슬래시 지시어는 단일 XML 태그를 포함하는 한 줄 주석
  - 주석의 내용은 컴파일러 지시어로 사용됨
- 삼중 슬래시 지시어는 포함하는 파일의 맨 위에서만 유효
  - 다른 삼중 슬래시 지시어를 포함한 한 줄 또는 여러 줄 주석만 앞에 올 수 있음
  - 문이나 선언 뒤에 나오면 일반적인 한 줄 주석으로 처리되며 특별한 의미를 갖지 않음
- TypeScript 5.5부터, 컴파일러는 참조 지시어를 생성하지 않으며, 해당 지시어가 [`preserve="true"`](#preservetrue)로 표시되지 않는 한 수동으로 작성된 삼중 슬래시 지시어를 출력 파일에 방출하지 않음

### `/// <reference path="..." />`

- `/// <reference path="..." />` 지시어가 이 그룹에서 가장 일반적
  - 파일 간의 _종속성_ 선언 역할
- 삼중 슬래시 참조는 컴파일러에게 컴파일 프로세스에 추가 파일을 포함하도록 지시
- [`out`](/tsconfig#out) 또는 [`outFile`](/tsconfig#outFile)을 사용할 때 출력을 정렬하는 방법 역할도 수행
  - 파일은 전처리 단계 후 입력과 동일한 순서로 출력 파일 위치에 방출됨

#### 입력 파일 전처리

- 컴파일러는 모든 삼중 슬래시 참조 지시어를 해석하기 위해 입력 파일에 대한 전처리 단계를 수행
  - 이 과정에서 추가 파일이 컴파일에 추가됨
- 프로세스는 _루트 파일_ 집합으로 시작
  - 이들은 커맨드 라인이나 `tsconfig.json` 파일의 [`files`](/tsconfig#files) 목록에 지정된 파일 이름
  - 이러한 루트 파일은 지정된 순서와 동일한 순서로 전처리됨
  - 파일이 목록에 추가되기 전에, 파일의 모든 삼중 슬래시 참조가 처리되고 해당 대상이 포함됨
  - 삼중 슬래시 참조는 파일에서 보이는 순서대로 깊이 우선 방식으로 해석됨
- 삼중 슬래시 참조 경로는 상대 경로가 사용된 경우 포함하는 파일을 기준으로 해석됨

#### 오류

- 존재하지 않는 파일을 참조하면 오류
- 파일이 자체에 대한 삼중 슬래시 참조를 갖는 것은 오류

#### `--noResolve` 사용하기

- 컴파일러 플래그 [`noResolve`](/tsconfig#noResolve)가 지정되면 삼중 슬래시 참조가 무시됨
  - 새 파일을 추가하거나 제공된 파일의 순서를 변경하지 않음

### `/// <reference types="..." />`

- _종속성_ 선언 역할을 하는 `/// <reference path="..." />` 지시어와 마찬가지로, `/// <reference types="..." />` 지시어는 패키지에 대한 종속성을 선언
- 이러한 패키지 이름을 해석하는 프로세스는 `import` 문에서 모듈 이름을 해석하는 프로세스와 유사
  - 삼중 슬래시 참조 타입 지시어를 선언 패키지에 대한 `import`로 생각하면 이해가 쉬움
- 예: 선언 파일에 `/// <reference types="node" />`를 포함하면 이 파일이 `@types/node/index.d.ts`에 선언된 이름을 사용함을 선언
  - 따라서 이 패키지는 선언 파일과 함께 컴파일에 포함되어야 함
- `.ts` 파일에서 `@types` 패키지에 대한 종속성을 선언하려면, 커맨드 라인이나 `tsconfig.json`에서 [`types`](/tsconfig#types)를 대신 사용
  - 자세한 내용은 [`tsconfig.json` 파일에서 `@types`, `typeRoots` 및 `types` 사용하기](/docs/handbook/tsconfig-json.html#types-typeroots-and-types) 참조

### `/// <reference lib="..." />`

- 이 지시어를 사용하면 파일이 기존 내장 _lib_ 파일을 명시적으로 포함할 수 있음
- 내장 _lib_ 파일은 _tsconfig.json_의 [`lib`](/tsconfig#lib) 컴파일러 옵션과 동일한 방식으로 참조됨 (예: `lib="lib.es2015.d.ts"`가 아닌 `lib="es2015"` 사용)
- DOM API나 `Symbol` 또는 `Iterable`과 같은 내장 JS 런타임 생성자와 같은 내장 타입에 의존하는 선언 파일 작성자의 경우 삼중 슬래시 참조 lib 지시어가 권장됨
  - 이전에는 이러한 .d.ts 파일이 해당 타입의 순방향/중복 선언을 추가해야 했음
- 예: 컴파일의 파일 중 하나에 `/// <reference lib="es2017.string" />`을 추가하는 것은 `--lib es2017.string`으로 컴파일하는 것과 동일

```ts
/// <reference lib="es2017.string" />

"foo".padStart(4);
```

### `/// <reference no-default-lib="true"/>`

- 이 지시어는 파일을 _기본 라이브러리_로 표시
  - `lib.d.ts` 및 다양한 변형의 맨 위에서 이 주석을 확인 가능
- 이 지시어는 컴파일러에게 기본 라이브러리(즉, `lib.d.ts`)를 컴파일에 포함하지 않도록 지시
  - 커맨드 라인에서 [`noLib`](/tsconfig#noLib)를 전달하는 것과 영향이 유사
- [`skipDefaultLibCheck`](/tsconfig#skipDefaultLibCheck)를 전달할 때, 컴파일러는 `/// <reference no-default-lib="true"/>`가 있는 파일의 검사만 건너뜀

### `/// <amd-module />`

- 기본적으로 AMD 모듈은 익명으로 생성됨
  - 이로 인해 번들러(예: `r.js`)와 같은 다른 도구를 사용하여 결과 모듈을 처리할 때 문제가 발생할 수 있음
- `amd-module` 지시어를 사용하면 컴파일러에 선택적 모듈 이름을 전달 가능

###### amdModule.ts

```ts
/// <amd-module name="NamedModule"/>
export class C {}
```

- AMD `define`을 호출하는 일부로 모듈에 `NamedModule` 이름이 할당됨

###### amdModule.js

```js
define("NamedModule", ["require", "exports"], function (require, exports) {
  var C = (function () {
    function C() {}
    return C;
  })();
  exports.C = C;
});
```

### `/// <amd-dependency />`

> **참고**: 이 지시어는 사용되지 않습니다. 대신 `import "moduleName";` 문을 사용하세요.

- `/// <amd-dependency path="x" />`는 결과 모듈의 require 호출에 주입되어야 하는 비TS 모듈 종속성에 대해 컴파일러에 알림
- `amd-dependency` 지시어는 선택적 `name` 속성도 가질 수 있음
  - 이를 통해 amd-dependency에 대한 선택적 이름을 전달 가능

```ts
/// <amd-dependency path="legacy/moduleA" name="moduleA"/>
declare var moduleA: MyType;
moduleA.callStuff();
```

- 생성된 JS 코드

```js
define(["require", "exports", "legacy/moduleA"], function (
  require,
  exports,
  moduleA
) {
  moduleA.callStuff();
});
```

### `preserve="true"`

- 삼중 슬래시 지시어는 컴파일러가 출력에서 제거하지 못하도록 `preserve="true"`로 표시 가능
- 예: 다음은 출력에서 지워짐

```ts
/// <reference path="..." />
/// <reference types="..." />
/// <reference lib="..." />
```

- 반면 다음은 유지됨

```ts
/// <reference path="..." preserve="true" />
/// <reference types="..." preserve="true" />
/// <reference lib="..." preserve="true" />
```

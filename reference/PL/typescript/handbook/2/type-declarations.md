# 타입 선언

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

TypeScript는 두 가지 주요 종류의 파일을 가지고 있습니다.
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

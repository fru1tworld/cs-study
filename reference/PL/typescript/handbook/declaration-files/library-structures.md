# 라이브러리 구조

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

ECMAScript 2015(ES2015, ECMAScript 6, ES6으로도 알려짐), CommonJS, RequireJS는 _모듈_을 _가져오는_ 것에 대해 유사한 개념을 가지고 있습니다. JavaScript CommonJS(Node.js)에서는 예를 들어 다음과 같이 작성합니다:

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

모듈 라이브러리는 일반적으로 다음 중 적어도 일부를 가지고 있습니다:

- `require` 또는 `define`에 대한 무조건적 호출
- `import * as a from 'b';` 또는 `export c;`와 같은 선언
- `exports` 또는 `module.exports`에 대한 할당

드물게 다음을 가집니다:

- `window` 또는 `global`의 프로퍼티에 대한 할당

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

ES6 호환 모듈 로더에서 최상위 객체(여기서는 `exp`로 가져온)는 프로퍼티만 가질 수 있습니다; 최상위 모듈 객체는 _절대로_ 호출 가능하지 않습니다.

여기서 가장 일반적인 해결책은 호출 가능/생성 가능 객체에 대해 `default` 내보내기를 정의하는 것입니다; 모듈 로더는 일반적으로 이 상황을 자동으로 감지하고 최상위 객체를 `default` 내보내기로 대체합니다. tsconfig.json에 [`"esModuleInterop": true`](/tsconfig/#esModuleInterop)가 있으면 TypeScript가 이를 처리해줍니다.

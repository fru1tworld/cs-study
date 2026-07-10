# 모듈과 네임스페이스

# 모듈

> **원문:** https://www.typescriptlang.org/docs/handbook/2/modules.html

JavaScript는 코드를 모듈화하는 다양한 방법의 오랜 역사가 있습니다.
2012년부터 존재해 온 TypeScript는 이러한 형식 중 많은 것을 지원해 왔지만, 시간이 지나면서 커뮤니티와 JavaScript 명세는 ES Modules(또는 ES6 modules)라는 형식으로 수렴했습니다. `import`/`export` 구문으로 알고 있을 수 있습니다.

ES Modules는 2015년에 JavaScript 명세에 추가되었고, 2020년에는 대부분의 웹 브라우저와 JavaScript 런타임에서 광범위한 지원을 받게 되었습니다.

핸드북은 ES Modules와 인기 있는 선행자인 CommonJS `module.exports =` 구문을 모두 다룰 것이며, 다른 모듈 패턴에 대한 정보는 [모듈](/docs/handbook/modules.html) 아래의 참조 섹션에서 찾을 수 있습니다.

## JavaScript 모듈이 정의되는 방법

TypeScript에서, ECMAScript 2015에서와 마찬가지로, 최상위 `import` 또는 `export`를 포함하는 모든 파일은 모듈로 간주됩니다.

반대로, 최상위 import 또는 export 선언이 없는 파일은 내용이 전역 범위(따라서 모듈에서도)에서 사용 가능한 스크립트로 처리됩니다.

모듈은 전역 범위가 아닌 자체 범위 내에서 실행됩니다.
이것은 모듈에서 선언된 변수, 함수, 클래스 등이 export 형식 중 하나를 사용하여 명시적으로 내보내지 않는 한 모듈 외부에서 볼 수 없다는 것을 의미합니다.
반대로, 다른 모듈에서 내보낸 변수, 함수, 클래스, 인터페이스 등을 사용하려면 import 형식 중 하나를 사용하여 가져와야 합니다.

## 비모듈

시작하기 전에, TypeScript가 모듈로 간주하는 것을 이해하는 것이 중요합니다.
JavaScript 명세는 `import` 선언, `export`, 또는 최상위 `await`이 없는 모든 JavaScript 파일은 스크립트로 간주되어야 하고 모듈이 아니라고 선언합니다.

스크립트 파일 내에서 변수와 타입은 공유 전역 범위에서 선언되며, 여러 입력 파일을 하나의 출력 파일로 결합하기 위해 [`outFile`](/tsconfig#outFile) 컴파일러 옵션을 사용하거나, HTML에서 여러 `<script>` 태그를 사용하여 이러한 파일을 (올바른 순서로!) 로드한다고 가정합니다.

현재 `import`나 `export`가 없는 파일이 있지만 모듈로 처리되기를 원한다면, 다음 줄을 추가하세요:

```ts twoslash
export {};
```

이것은 파일을 아무것도 내보내지 않는 모듈로 변경합니다. 이 구문은 모듈 대상에 관계없이 작동합니다.

## TypeScript의 모듈

TypeScript에서 모듈 기반 코드를 작성할 때 고려해야 할 세 가지 주요 사항이 있습니다:

- **구문**: import와 export를 위해 어떤 구문을 사용하고 싶은가?
- **모듈 해결**: 모듈 이름(또는 경로)과 디스크의 파일 사이의 관계는 무엇인가?
- **모듈 출력 대상**: 내보낸 JavaScript 모듈은 어떻게 보여야 하는가?

### ES Module 구문

파일은 `export default`를 통해 주요 내보내기를 선언할 수 있습니다:

```ts twoslash
// @filename: hello.ts
export default function helloWorld() {
  console.log("Hello, world!");
}
```

이것은 다음을 통해 가져옵니다:

```ts twoslash
// @filename: hello.ts
export default function helloWorld() {
  console.log("Hello, world!");
}
// @filename: index.ts
// ---cut---
import helloWorld from "./hello.js";
helloWorld();
```

기본 내보내기 외에, `default`를 생략한 `export`를 통해 변수와 함수를 둘 이상 내보낼 수 있습니다:

```ts twoslash
// @filename: maths.ts
export var pi = 3.14;
export let squareTwo = 1.41;
export const phi = 1.61;

export class RandomNumberGenerator {}

export function absolute(num: number) {
  if (num < 0) return num * -1;
  return num;
}
```

이것들은 `import` 구문을 통해 다른 파일에서 사용할 수 있습니다:

```ts twoslash
// @filename: maths.ts
export var pi = 3.14;
export let squareTwo = 1.41;
export const phi = 1.61;
export class RandomNumberGenerator {}
export function absolute(num: number) {
  if (num < 0) return num * -1;
  return num;
}
// @filename: app.ts
// ---cut---
import { pi, phi, absolute } from "./maths.js";

console.log(pi);
const absPhi = absolute(phi);
//    ^?
```

### 추가 Import 구문

import는 `import {old as new}`와 같은 형식을 사용하여 이름을 바꿀 수 있습니다:

```ts twoslash
// @filename: maths.ts
export var pi = 3.14;
// @filename: app.ts
// ---cut---
import { pi as π } from "./maths.js";

console.log(π);
//          ^?
```

위의 구문을 단일 `import`로 혼합하여 사용할 수 있습니다:

```ts twoslash
// @filename: maths.ts
export const pi = 3.14;
export default class RandomNumberGenerator {}

// @filename: app.ts
import RandomNumberGenerator, { pi as π } from "./maths.js";

RandomNumberGenerator;
// ^?

console.log(π);
//          ^?
```

`* as name`을 사용하여 내보낸 모든 객체를 가져와 단일 네임스페이스에 넣을 수 있습니다:

```ts twoslash
// @filename: maths.ts
export var pi = 3.14;
export let squareTwo = 1.41;
export const phi = 1.61;

export function absolute(num: number) {
  if (num < 0) return num * -1;
  return num;
}
// ---cut---
// @filename: app.ts
import * as math from "./maths.js";

console.log(math.pi);
const positivePhi = math.absolute(math.phi);
//    ^?
```

`import "./file"`을 통해 파일을 가져오고 현재 모듈에 어떤 변수도 포함하지 _않을_ 수 있습니다:

```ts twoslash
// @filename: maths.ts
export var pi = 3.14;
// ---cut---
// @filename: app.ts
import "./maths.js";

console.log("3.14");
```

이 경우, `import`는 아무것도 하지 않습니다. 그러나 `maths.ts`의 모든 코드가 평가되었으며, 이는 다른 객체에 영향을 미치는 부작용을 트리거할 수 있습니다.

#### TypeScript 특정 ES Module 구문

타입은 JavaScript 값과 같은 구문을 사용하여 내보내고 가져올 수 있습니다:

```ts twoslash
// @filename: animal.ts
export type Cat = { breed: string; yearOfBirth: number };

export interface Dog {
  breeds: string[];
  yearOfBirth: number;
}

// @filename: app.ts
import { Cat, Dog } from "./animal.js";
type Animals = Cat | Dog;
```

TypeScript는 타입의 import를 선언하기 위한 두 가지 개념으로 `import` 구문을 확장했습니다:

##### `import type`

타입_만_ 가져올 수 있는 import 문입니다:

```ts twoslash
// @filename: animal.ts
export type Cat = { breed: string; yearOfBirth: number };
export type Dog = { breeds: string[]; yearOfBirth: number };
export const createCatName = () => "fluffy";

// @filename: valid.ts
import type { Cat, Dog } from "./animal.js";
export type Animals = Cat | Dog;

// @filename: app.ts
// @errors: 1361
import type { createCatName } from "./animal.js";
const name = createCatName();
```

##### 인라인 `type` imports

TypeScript 4.5에서는 개별 imports에 `type`을 접두사로 붙여 가져온 참조가 타입임을 나타낼 수 있습니다:

```ts twoslash
// @filename: animal.ts
export type Cat = { breed: string; yearOfBirth: number };
export type Dog = { breeds: string[]; yearOfBirth: number };
export const createCatName = () => "fluffy";
// ---cut---
// @filename: app.ts
import { createCatName, type Cat, type Dog } from "./animal.js";

export type Animals = Cat | Dog;
const name = createCatName();
```

이것들은 함께 Babel, swc 또는 esbuild와 같은 비TypeScript 트랜스파일러가 안전하게 제거할 수 있는 import를 알 수 있게 합니다.

#### CommonJS 동작을 가진 ES Module 구문

TypeScript는 CommonJS 및 AMD `require`에 _직접_ 대응하는 ES Module 구문이 있습니다. ES Module을 사용한 imports는 _대부분의 경우_ 해당 환경의 `require`와 같지만, 이 구문은 TypeScript 파일에서 CommonJS 출력과 1대1 일치를 보장합니다:

```ts twoslash
/// <reference types="node" />
// @module: commonjs
// ---cut---
import fs = require("fs");
const code = fs.readFileSync("hello.ts", "utf8");
```

이 구문에 대해 [모듈 참조 페이지](/docs/handbook/modules.html#export--and-import--require)에서 더 배울 수 있습니다.

## CommonJS 구문

CommonJS는 npm의 대부분의 모듈이 전달되는 형식입니다. 위의 ES Modules 구문을 사용하여 작성하더라도, CommonJS 구문이 어떻게 작동하는지에 대한 간단한 이해가 있으면 더 쉽게 디버깅하는 데 도움이 됩니다.

#### 내보내기

식별자는 전역 `module`의 `exports` 속성을 설정하여 내보냅니다.

```ts twoslash
/// <reference types="node" />
// ---cut---
function absolute(num: number) {
  if (num < 0) return num * -1;
  return num;
}

module.exports = {
  pi: 3.14,
  squareTwo: 1.41,
  phi: 1.61,
  absolute,
};
```

그런 다음 이러한 파일은 `require` 문을 통해 가져올 수 있습니다:

```ts twoslash
// @module: commonjs
// @filename: maths.ts
/// <reference types="node" />
function absolute(num: number) {
  if (num < 0) return num * -1;
  return num;
}

module.exports = {
  pi: 3.14,
  squareTwo: 1.41,
  phi: 1.61,
  absolute,
};
// @filename: index.ts
// ---cut---
const maths = require("./maths");
maths.pi;
//    ^?
```

또는 JavaScript의 구조 분해 기능을 사용하여 약간 단순화할 수 있습니다:

```ts twoslash
// @module: commonjs
// @filename: maths.ts
/// <reference types="node" />
function absolute(num: number) {
  if (num < 0) return num * -1;
  return num;
}

module.exports = {
  pi: 3.14,
  squareTwo: 1.41,
  phi: 1.61,
  absolute,
};
// @filename: index.ts
// ---cut---
const { squareTwo } = require("./maths");
squareTwo;
// ^?
```

### CommonJS와 ES Modules 상호 운용

기본 import와 모듈 네임스페이스 객체 import의 구분에 관해 CommonJS와 ES Modules 사이에 기능 불일치가 있습니다. TypeScript에는 [`esModuleInterop`](/tsconfig#esModuleInterop)으로 두 가지 다른 제약 조건 집합 간의 마찰을 줄이기 위한 컴파일러 플래그가 있습니다.

## TypeScript의 모듈 해결 옵션

모듈 해결은 `import` 또는 `require` 문에서 문자열을 가져와서 해당 문자열이 참조하는 파일을 결정하는 과정입니다.

TypeScript는 두 가지 해결 전략을 포함합니다: Classic과 Node. 컴파일러 옵션 [`module`](/tsconfig#module)이 `commonjs`가 아닐 때 기본값인 Classic은 이전 버전과의 호환성을 위해 포함됩니다.
Node 전략은 Node.js가 CommonJS 모드에서 작동하는 방식을 복제하며, `.ts` 및 `.d.ts`에 대한 추가 검사를 포함합니다.

TypeScript 내에서 모듈 전략에 영향을 미치는 많은 TSConfig 플래그가 있습니다: [`moduleResolution`](/tsconfig#moduleResolution), [`baseUrl`](/tsconfig#baseUrl), [`paths`](/tsconfig#paths), [`rootDirs`](/tsconfig#rootDirs).

이러한 전략이 어떻게 작동하는지에 대한 전체 세부 정보는 [모듈 해결](/docs/handbook/modules/reference.html#the-moduleresolution-compiler-option) 참조 페이지를 참조하세요.

## TypeScript의 모듈 출력 옵션

내보낸 JavaScript 출력에 영향을 미치는 두 가지 옵션이 있습니다:

- [`target`](/tsconfig#target)은 어떤 JS 기능이 다운레벨되고(이전 JavaScript 런타임에서 실행되도록 변환됨) 어떤 것이 그대로 유지되는지 결정합니다
- [`module`](/tsconfig#module)은 모듈이 서로 상호 작용하는 데 사용되는 코드를 결정합니다

어떤 [`target`](/tsconfig#target)을 사용할지는 TypeScript 코드를 실행할 것으로 예상하는 JavaScript 런타임에서 사용 가능한 기능이 결정합니다. 예를 들어 지원하는 가장 오래된 웹 브라우저, 실행될 것으로 예상하는 가장 낮은 버전의 Node.js, 또는 Electron과 같은 런타임의 고유한 제약에서 옵니다.

모듈 간의 모든 통신은 모듈 로더를 통해 이루어지며, 컴파일러 옵션 [`module`](/tsconfig#module)이 어떤 것을 사용할지 결정합니다.
런타임에 모듈 로더는 실행하기 전에 모듈의 모든 종속성을 찾고 실행하는 역할을 합니다.

예를 들어, 다음은 ES Modules 구문을 사용하는 TypeScript 파일로, [`module`](/tsconfig#module)에 대한 몇 가지 다른 옵션을 보여줍니다:

```ts twoslash
// @filename: constants.ts
export const valueOfPi = 3.142;
// @filename: index.ts
// ---cut---
import { valueOfPi } from "./constants.js";

export const twoPi = valueOfPi * 2;
```

#### `ES2020`

```ts twoslash
// @showEmit
// @module: es2020
// @noErrors
import { valueOfPi } from "./constants.js";

export const twoPi = valueOfPi * 2;
```

#### `CommonJS`

```ts twoslash
// @showEmit
// @module: commonjs
// @noErrors
import { valueOfPi } from "./constants.js";

export const twoPi = valueOfPi * 2;
```

#### `UMD`

```ts twoslash
// @showEmit
// @module: umd
// @noErrors
import { valueOfPi } from "./constants.js";

export const twoPi = valueOfPi * 2;
```

> ES2020은 원래 `index.ts`와 효과적으로 같습니다.

사용 가능한 모든 옵션과 내보낸 JavaScript 코드가 어떻게 보이는지는 [`module`에 대한 TSConfig 참조](/tsconfig#module)에서 볼 수 있습니다.

## TypeScript 네임스페이스

TypeScript는 ES Modules 표준보다 앞선 `namespaces`라는 자체 모듈 형식이 있습니다. 이 구문은 복잡한 정의 파일을 만드는 데 유용한 많은 기능이 있으며, [DefinitelyTyped](https://github.com/DefinitelyTyped/DefinitelyTyped)에서 여전히 활발하게 사용되고 있습니다. 더 이상 사용되지 않는 것은 아니지만, 네임스페이스의 대부분의 기능은 ES Modules에 존재하며, JavaScript의 방향에 맞추기 위해 네임스페이스 대신 ES Modules를 사용하는 것을 권장합니다. [네임스페이스 참조 페이지](/docs/handbook/namespaces.html)에서 네임스페이스에 대해 더 배울 수 있습니다.

---

# 모듈 - 소개

> **원문:** https://www.typescriptlang.org/docs/handbook/modules/introduction.html

이 문서는 네 개의 섹션으로 나뉩니다:

1. 첫 번째 섹션은 TypeScript가 모듈에 접근하는 방식 뒤의 [**이론**](/docs/handbook/modules/theory.html)을 개발합니다. 어떤 상황에서든 올바른 모듈 관련 컴파일러 옵션을 작성하고, TypeScript를 다른 도구와 통합하는 방법을 추론하거나, TypeScript가 의존성 패키지를 처리하는 방식을 이해하고 싶다면 여기서 시작하는 것이 좋습니다. 이러한 주제에 대한 가이드와 참조 페이지가 있지만, 이러한 기본 사항에 대한 이해를 쌓으면 가이드를 읽기가 더 쉬워지고, 여기서 구체적으로 다루지 않은 실제 문제를 다루기 위한 정신적 프레임워크를 제공합니다.

2. [**가이드**](/docs/handbook/modules/guides/choosing-compiler-options.html)는 새 프로젝트에 적합한 컴파일 설정을 선택하는 것부터 시작하여 특정 실제 작업을 수행하는 방법을 보여줍니다. 가이드는 가능한 한 빨리 시작하고 싶은 초보자와 이론에 대한 충분한 이해가 있지만 복잡한 작업에 대한 구체적인 지침을 원하는 전문가 모두에게 시작하기 좋은 곳입니다.

3. [**참조**](/docs/handbook/modules/reference.html) 섹션은 이전 섹션에서 제시된 구문과 구성에 대한 더 자세한 설명을 제공합니다.

4. [**부록**](/docs/handbook/modules/appendices/esm-cjs-interop.html)은 이론이나 참조 섹션이 허용하는 것보다 더 자세한 설명이 필요한 복잡한 주제를 다룹니다.

---

# 모듈 - 이론

> **원문:** https://www.typescriptlang.org/docs/handbook/modules/theory.html

## JavaScript에서의 스크립트와 모듈

JavaScript의 초기 시절, 언어가 브라우저에서만 실행되었을 때는 모듈이 없었지만, HTML의 여러 `script` 태그를 사용하여 웹 페이지의 JavaScript를 여러 파일로 분할할 수 있었습니다:

```html
<html>
  <head>
    <script src="a.js"></script>
    <script src="b.js"></script>
  </head>
  <body></body>
</html>
```

이 접근 방식에는 몇 가지 단점이 있었는데, 특히 웹 페이지가 더 크고 복잡해짐에 따라 그러했습니다. 특히 같은 페이지에 로드된 모든 스크립트가 같은 스코프를 공유합니다 - 적절하게 "전역 스코프"라고 불립니다 - 이는 스크립트가 서로의 변수와 함수를 덮어쓰지 않도록 매우 주의해야 한다는 것을 의미합니다.

파일에 자체 스코프를 제공하면서도 코드 조각을 다른 파일에서 사용할 수 있게 하는 방법을 제공하여 이 문제를 해결하는 모든 시스템을 "모듈 시스템"이라고 부를 수 있습니다. (모듈 시스템의 각 파일을 "모듈"이라고 부르는 것은 당연해 보일 수 있지만, 이 용어는 종종 전역 스코프에서 모듈 시스템 외부에서 실행되는 _스크립트_ 파일과 대조하여 사용됩니다.)

> [많은 모듈 시스템](https://github.com/myshov/history-of-javascript/tree/master/4_evolution_of_js_modularity)이 있으며, TypeScript는 [여러 가지를 출력하도록 지원](https://www.typescriptlang.org/tsconfig/#module)하지만, 이 문서는 오늘날 가장 중요한 두 시스템인 ECMAScript 모듈(ESM)과 CommonJS(CJS)에 초점을 맞춥니다.
>
> ECMAScript 모듈(ESM)은 언어에 내장된 모듈 시스템으로, 최신 브라우저와 v12 이후의 Node.js에서 지원됩니다. 전용 `import` 및 `export` 구문을 사용합니다:
>
> ```js
> // a.js
> export default "Hello from a.js";
> ```
>
> ```js
> // b.js
> import a from "./a.js";
> console.log(a); // 'Hello from a.js'
> ```
>
> CommonJS(CJS)는 ESM이 언어 사양의 일부가 되기 전에 원래 Node.js에 제공된 모듈 시스템입니다. ESM과 함께 Node.js에서 여전히 지원됩니다. `exports`와 `require`라는 일반 JavaScript 객체와 함수를 사용합니다:
>
> ```js
> // a.js
> exports.message = "Hello from a.js";
> ```
>
> ```js
> // b.js
> const a = require("./a");
> console.log(a.message); // 'Hello from a.js'
> ```

따라서 TypeScript는 파일이 CommonJS 또는 ECMAScript 모듈임을 감지하면, 해당 파일이 자체 스코프를 가질 것이라고 가정하는 것으로 시작합니다. 그 이후로 컴파일러의 작업은 조금 더 복잡해집니다.

## 모듈에 관한 TypeScript의 역할

TypeScript 컴파일러의 주요 목표는 컴파일 시간에 특정 종류의 런타임 오류를 잡아서 방지하는 것입니다. 모듈이 관련되든 안 되든, 컴파일러는 코드의 의도된 런타임 환경에 대해 알아야 합니다 - 예를 들어 어떤 전역 변수가 사용 가능한지. 모듈이 관련된 경우, 컴파일러가 작업을 수행하기 위해 답해야 할 몇 가지 추가 질문이 있습니다. 분석에 필요한 모든 정보를 생각하기 위해 예제로 몇 줄의 입력 코드를 사용해 봅시다:

```ts
import sayHello from "greetings";
sayHello("world");
```

이 파일을 확인하려면, 컴파일러는 `sayHello`의 타입(하나의 문자열 인수를 받을 수 있는 함수인가?)을 알아야 하며, 이는 꽤 많은 추가 질문을 열어둡니다:

1. 모듈 시스템이 이 TypeScript 파일을 직접 로드할 것인가, 아니면 내가 (또는 다른 컴파일러가) 이 TypeScript 파일에서 생성하는 JavaScript 파일을 로드할 것인가?
2. 로드할 파일 이름과 디스크 위치를 고려할 때, 모듈 시스템이 어떤 _종류_의 모듈을 찾을 것으로 예상하는가?
3. 출력 JavaScript가 생성되는 경우, 이 파일에 있는 모듈 구문이 출력 코드에서 어떻게 변환될 것인가?
4. 모듈 시스템이 `"greetings"`로 지정된 모듈을 찾기 위해 어디를 볼 것인가? 조회가 성공할 것인가?
5. 해당 조회로 해결된 파일은 어떤 종류의 모듈인가?
6. 모듈 시스템이 (2)에서 감지된 종류의 모듈이 (3)에서 결정된 구문으로 (5)에서 감지된 종류의 모듈을 참조할 수 있도록 허용하는가?
7. `"greetings"` 모듈이 분석되면, 해당 모듈의 어떤 부분이 `sayHello`에 바인딩되는가?

이러한 모든 질문은 _호스트_ - 출력 JavaScript(또는 경우에 따라 원시 TypeScript)를 소비하여 모듈 로딩 동작을 지시하는 시스템, 일반적으로 Node.js와 같은 런타임이나 Webpack과 같은 번들러 - 의 특성에 따라 달라집니다.

ECMAScript 사양은 ESM import와 export가 서로 어떻게 연결되는지 정의하지만, (4)에서 알려진 _모듈 해결_이 어떻게 발생하는지는 지정하지 않으며, CommonJS와 같은 다른 모듈 시스템에 대해서는 아무것도 말하지 않습니다. 따라서 런타임과 번들러, 특히 ESM과 CJS를 모두 지원하려는 것들은 자체 규칙을 설계할 자유가 많습니다. 결과적으로, TypeScript가 위의 질문에 답하는 방식은 코드가 실행되도록 의도된 곳에 따라 극적으로 달라질 수 있습니다. 단일 정답이 없으므로, 컴파일러에게 구성 옵션을 통해 규칙을 알려야 합니다.

명심해야 할 다른 핵심 아이디어는 TypeScript가 이러한 질문을 거의 항상 _출력_ JavaScript 파일의 관점에서 생각한다는 것입니다, _입력_ TypeScript(또는 JavaScript!) 파일이 아니라. 오늘날 일부 런타임과 번들러는 TypeScript 파일을 직접 로드하는 것을 지원하며, 이러한 경우 별도의 입력 및 출력 파일에 대해 생각하는 것은 의미가 없습니다. 이 문서의 대부분은 TypeScript 파일이 JavaScript 파일로 컴파일되고, 이것이 런타임 모듈 시스템에 의해 로드되는 경우를 논의합니다. 이러한 경우를 검토하는 것은 컴파일러의 옵션과 동작을 이해하는 데 필수적입니다 - 거기서 시작하고 esbuild, Bun 및 기타 [TypeScript 우선 런타임과 번들러](#번들러-typescript-런타임-및-nodejs-로더를-위한-모듈-해결)에 대해 생각할 때 단순화하는 것이 더 쉽습니다. 따라서 지금은 모듈과 관련하여 TypeScript의 역할을 출력 파일의 관점에서 요약할 수 있습니다:

**호스트의 규칙**을 충분히 이해하여

1. 파일을 유효한 **출력 모듈 형식**으로 컴파일하고,
2. 해당 **출력**의 import가 **성공적으로 해결**되도록 보장하며,
3. **가져온 이름**에 어떤 **타입**을 할당할지 알아야 합니다.

## 호스트는 누구인가?

계속하기 전에, _호스트_라는 용어에 대해 같은 페이지에 있는지 확인하는 것이 좋습니다. 이 용어는 자주 등장할 것이기 때문입니다. 이전에 "출력 코드를 소비하여 모듈 로딩 동작을 지시하는 시스템"으로 정의했습니다. 다시 말해, TypeScript의 모듈 분석이 모델링하려는 TypeScript 외부의 시스템입니다:

- 출력 코드(`tsc` 또는 타사 트랜스파일러에 의해 생성됨)가 Node.js와 같은 런타임에서 직접 실행되는 경우, 런타임이 호스트입니다.
- 런타임이 TypeScript 파일을 직접 소비하기 때문에 "출력 코드"가 없는 경우에도, 런타임이 여전히 호스트입니다.
- 번들러가 TypeScript 입력 또는 출력을 소비하고 번들을 생성하는 경우, 번들러가 호스트입니다. 번들러가 원래 import/require 세트를 보고, 참조하는 파일을 찾고, 원래 import와 require가 지워지거나 인식 불가능하게 변환된 새 파일 또는 파일 세트를 생성했기 때문입니다. (해당 번들 자체가 모듈로 구성될 수 있으며, 이를 실행하는 런타임이 그 번들의 호스트가 되지만, TypeScript는 번들러 이후에 일어나는 일에 대해 알지 못합니다.)
- 다른 트랜스파일러, 옵티마이저, 또는 포매터가 TypeScript의 출력에서 실행되는 경우, import와 export를 그대로 두는 한 TypeScript가 신경 쓰는 호스트가 _아닙니다_.
- 웹 브라우저에서 모듈을 로드할 때, TypeScript가 모델링해야 하는 동작은 실제로 웹 서버와 브라우저에서 실행되는 모듈 시스템 사이에 분할됩니다. 브라우저의 JavaScript 엔진(또는 RequireJS와 같은 스크립트 기반 모듈 로딩 프레임워크)이 어떤 모듈 형식이 허용되는지 제어하고, 웹 서버는 한 모듈이 다른 모듈을 로드하는 요청을 트리거할 때 어떤 파일을 보낼지 결정합니다.
- TypeScript 컴파일러 자체는 호스트가 아닙니다. 다른 호스트를 모델링하려는 것 외에 모듈과 관련된 동작을 제공하지 않기 때문입니다.

## 모듈 출력 형식

모든 프로젝트에서 모듈에 대해 답해야 할 첫 번째 질문은 호스트가 어떤 종류의 모듈을 기대하는지입니다. 그래야 TypeScript가 각 파일의 출력 형식을 일치하도록 설정할 수 있습니다. 때때로 호스트는 한 종류의 모듈만 _지원_합니다 - 예를 들어 브라우저의 ESM 또는 Node.js v11 이하의 CJS. Node.js v12 이상은 CJS와 ES 모듈을 모두 허용하지만, 파일 확장자와 `package.json` 파일을 사용하여 각 파일이 어떤 형식이어야 하는지 결정하고, 파일 내용이 예상 형식과 일치하지 않으면 오류를 발생시킵니다.

`module` 컴파일러 옵션은 이 정보를 컴파일러에 제공합니다. 주요 목적은 컴파일 중에 생성되는 JavaScript의 모듈 형식을 제어하는 것이지만, 각 파일의 모듈 종류를 어떻게 감지해야 하는지, 서로 다른 모듈 종류가 서로 어떻게 가져올 수 있는지, `import.meta` 및 최상위 `await`과 같은 기능이 사용 가능한지 여부를 컴파일러에 알리는 역할도 합니다. 따라서 TypeScript 프로젝트가 `noEmit`을 사용하더라도 올바른 `module` 설정을 선택하는 것이 여전히 중요합니다. 앞서 설명했듯이, 컴파일러는 import를 타입 체크(및 IntelliSense 제공)하기 위해 모듈 시스템에 대한 정확한 이해가 필요합니다. 프로젝트에 적합한 `module` 설정을 선택하는 방법에 대한 지침은 [_컴파일러 옵션 선택_](/docs/handbook/modules/guides/choosing-compiler-options.html)을 참조하세요.

사용 가능한 `module` 설정은 다음과 같습니다:

- [**`node16`**](/docs/handbook/modules/reference.html#node16-node18-node20-nodenext): 특정 상호 운용성 및 감지 규칙으로 ES 모듈과 CJS 모듈을 나란히 지원하는 Node.js v16+의 모듈 시스템을 반영합니다.
- [**`node18`**](/docs/handbook/modules/reference.html#node16-node18-node20-nodenext): import 속성 지원을 추가하는 Node.js v18+의 모듈 시스템을 반영합니다.
- [**`nodenext`**](/docs/handbook/modules/reference.html#node16-node18-node20-nodenext): Node.js의 모듈 시스템이 발전함에 따라 최신 Node.js 버전을 반영하는 이동 목표입니다. TypeScript 5.8 기준으로 `nodenext`는 ECMAScript 모듈의 `require`를 지원합니다.
- [**`es2015`**](/docs/handbook/modules/reference.html#es2015-es2020-es2022-esnext): JavaScript 모듈에 대한 ES2015 언어 사양(`import` 및 `export`를 언어에 처음 도입한 버전)을 반영합니다.
- [**`es2020`**](/docs/handbook/modules/reference.html#es2015-es2020-es2022-esnext): `es2015`에 `import.meta` 및 `export * as ns from "mod"` 지원을 추가합니다.
- [**`es2022`**](/docs/handbook/modules/reference.html#es2015-es2020-es2022-esnext): `es2020`에 최상위 `await` 지원을 추가합니다.
- [**`esnext`**](/docs/handbook/modules/reference.html#es2015-es2020-es2022-esnext): 현재 `es2022`와 동일하지만, 최신 ECMAScript 사양과 향후 사양 버전에 포함될 것으로 예상되는 모듈 관련 Stage 3+ 제안을 반영하는 이동 목표가 될 것입니다.
- **[`commonjs`](/docs/handbook/modules/reference.html#commonjs), [`system`](/docs/handbook/modules/reference.html#system), [`amd`](/docs/handbook/modules/reference.html#amd), [`umd`](/docs/handbook/modules/reference.html#umd)**: 각각 명명된 모듈 시스템에서 모든 것을 출력하고, 모든 것이 해당 모듈 시스템으로 성공적으로 가져올 수 있다고 가정합니다. 이들은 더 이상 새 프로젝트에 권장되지 않으며 이 문서에서 자세히 다루지 않습니다.

> Node.js의 모듈 형식 감지 및 상호 운용성 규칙은 `tsc`가 출력하는 모든 파일이 각각 ESM 또는 CJS이더라도 Node.js에서 실행되는 프로젝트에 대해 `module`을 `esnext` 또는 `commonjs`로 지정하는 것이 부정확하게 만듭니다. Node.js에서 실행하려는 프로젝트에 대한 유일한 올바른 `module` 설정은 `node16`과 `nodenext`입니다. 전체 ESM Node.js 프로젝트에 대해 출력된 JavaScript가 `esnext`와 `nodenext`를 사용한 컴파일 사이에 동일하게 보일 수 있지만, 타입 검사는 다를 수 있습니다. 자세한 내용은 [`nodenext`에 대한 참조 섹션](/docs/handbook/modules/reference.html#node16-node18-node20-nodenext)을 참조하세요.

### 모듈 형식 감지

Node.js는 ES 모듈과 CJS 모듈을 모두 이해하지만, 각 파일의 형식은 파일 확장자와 첫 번째 `package.json` 파일의 `type` 필드에 의해 결정됩니다. 이 파일은 파일의 디렉토리와 모든 상위 디렉토리를 검색하여 찾습니다:

- `.mjs` 및 `.cjs` 파일은 항상 각각 ES 모듈과 CJS 모듈로 해석됩니다.
- `.js` 파일은 가장 가까운 `package.json` 파일에 값이 `"module"`인 `type` 필드가 포함되어 있으면 ES 모듈로 해석됩니다. `package.json` 파일이 없거나 `type` 필드가 없거나 다른 값을 가지면, `.js` 파일은 CJS 모듈로 해석됩니다.

파일이 이러한 규칙에 의해 ES 모듈로 결정되면, Node.js는 평가 중에 파일의 스코프에 CommonJS `module`과 `require` 객체를 주입하지 않으므로, 이를 사용하려는 파일은 충돌을 일으킵니다. 반대로, 파일이 CJS 모듈로 결정되면, 파일의 `import` 및 `export` 선언은 구문 오류 충돌을 일으킵니다.

`module` 컴파일러 옵션이 `node16`, `node18`, 또는 `nodenext`로 설정되면, TypeScript는 프로젝트의 _입력_ 파일에 이 동일한 알고리즘을 적용하여 해당 각 _출력_ 파일의 모듈 종류를 결정합니다. `--module nodenext`를 사용하는 예제 프로젝트에서 모듈 형식이 어떻게 감지되는지 살펴보겠습니다:

| 입력 파일 이름                   | 내용                     | 출력 파일 이름   | 모듈 종류 | 이유                                      |
| -------------------------------- | ---------------------- | ---------------- | --------- | ----------------------------------------- |
| `/package.json`                  | `{}`                   |                  |           |                                           |
| `/main.mts`                      |                        | `/main.mjs`      | ESM       | 파일 확장자                               |
| `/utils.cts`                     |                        | `/utils.cjs`     | CJS       | 파일 확장자                               |
| `/example.ts`                    |                        | `/example.js`    | CJS       | `package.json`에 `"type": "module"` 없음  |
| `/node_modules/pkg/package.json` | `{ "type": "module" }` |                  |           |                                           |
| `/node_modules/pkg/index.d.ts`   |                        |                  | ESM       | `package.json`의 `"type": "module"`       |
| `/node_modules/pkg/index.d.cts`  |                        |                  | CJS       | 파일 확장자                               |

### 입력 모듈 구문

입력 소스 파일에서 보이는 _입력_ 모듈 구문은 JS 파일에 출력되는 출력 모듈 구문과 다소 분리되어 있다는 점에 유의하는 것이 중요합니다. 즉, ESM import가 있는 파일:

```ts
import { sayHello } from "greetings";
sayHello("world");
```

은 `module` 컴파일러 옵션에 따라 ESM 형식 그대로 출력될 수도 있고, CommonJS로 출력될 수도 있습니다:

```ts
Object.defineProperty(exports, "__esModule", { value: true });
const greetings_1 = require("greetings");
(0, greetings_1.sayHello)("world");
```

일반적으로 이것은 입력 파일의 내용을 보는 것만으로는 ES 모듈인지 CJS 모듈인지 결정하기에 충분하지 않다는 것을 의미합니다.

### ESM과 CJS 상호 운용성

ES 모듈이 CommonJS 모듈을 `import`할 수 있나요? 그렇다면, 기본 import가 `exports`에 연결되나요 아니면 `exports.default`에 연결되나요? CommonJS 모듈이 ES 모듈을 `require`할 수 있나요? CommonJS는 ECMAScript 사양의 일부가 아니므로, 런타임, 번들러, 트랜스파일러는 ESM이 2015년에 표준화된 이후로 이러한 질문에 대한 자체 답변을 자유롭게 만들어왔고, 따라서 표준 상호 운용성 규칙 세트가 존재하지 않습니다. 오늘날 대부분의 런타임과 번들러는 크게 세 가지 범주 중 하나에 속합니다:

1. **ESM 전용.** 브라우저 엔진과 같은 일부 런타임은 실제로 언어의 일부인 ECMAScript 모듈만 지원합니다.
2. **번들러 스타일.** 주요 JavaScript 엔진이 ES 모듈을 실행할 수 있기 전에, Babel은 개발자가 CommonJS로 트랜스파일하여 ES 모듈을 작성할 수 있게 해주었습니다. ESM-으로-트랜스파일된-CJS 파일이 수작업으로-작성된-CJS 파일과 상호작용하는 방식은 번들러와 트랜스파일러의 사실상 표준이 된 허용적인 상호 운용성 규칙 세트를 암시했습니다.
3. **Node.js.** Node.js v20.19.0까지 CommonJS 모듈은 ES 모듈을 동기적으로(`require`로) 로드할 수 없었습니다; 동적 `import()` 호출로만 비동기적으로 로드할 수 있었습니다. ES 모듈은 CJS 모듈을 기본 import할 수 있으며, 이는 항상 `exports`에 바인딩됩니다. (이것은 `__esModule`이 있는 Babel 스타일 CJS 출력의 기본 import가 Node.js와 일부 번들러 사이에서 다르게 동작한다는 것을 의미합니다.)

TypeScript는 import(특히 `default`)에 올바른 타입을 제공하고 런타임에 충돌할 import에 오류를 발생시키기 위해 이러한 규칙 세트 중 어느 것을 가정해야 하는지 알아야 합니다. `module` 컴파일러 옵션이 `node16`, `node18`, 또는 `nodenext`로 설정되면, Node.js의 버전별 규칙이 적용됩니다. 다른 모든 `module` 설정은 [`esModuleInterop`](/docs/handbook/modules/reference.html#esModuleInterop) 옵션과 결합하여 TypeScript에서 번들러 스타일 상호 운용을 결과로 합니다.

### 모듈 지정자는 기본적으로 변환되지 않음

`module` 컴파일러 옵션은 입력 파일의 import와 export를 출력 파일의 다른 모듈 형식으로 변환할 수 있지만, 모듈 _지정자_(당신이 `import`하는 문자열 또는 `require`에 전달하는 문자열)는 작성된 그대로 출력됩니다. 예를 들어, 다음과 같은 입력:

```ts
import { add } from "./math.mjs";
add(1, 2);
```

은 다음 중 하나로 출력될 수 있습니다:

```ts
import { add } from "./math.mjs";
add(1, 2);
```

또는:

```ts
const math_1 = require("./math.mjs");
math_1.add(1, 2);
```

`module` 컴파일러 옵션에 따라 다르지만, 모듈 지정자는 어느 쪽이든 `"./math.mjs"`가 됩니다. 기본적으로 모듈 지정자는 코드의 대상 런타임 또는 번들러에서 작동하는 방식으로 작성되어야 하며, TypeScript가 해당 _출력_-상대적 지정자를 이해하는 것이 TypeScript의 역할입니다. 모듈 지정자가 참조하는 파일을 찾는 프로세스를 _모듈 해결_이라고 합니다.

## 모듈 해결

[첫 번째 예제](#모듈에-관한-typescripts-역할)로 돌아가서 지금까지 배운 것을 검토해봅시다:

```ts
import sayHello from "greetings";
sayHello("world");
```

지금까지 호스트의 모듈 시스템과 TypeScript의 `module` 컴파일러 옵션이 이 코드에 어떤 영향을 미칠 수 있는지 논의했습니다. 입력 구문이 ESM처럼 보이지만, 출력 형식은 `module` 컴파일러 옵션, 잠재적으로 파일 확장자, `package.json` `"type"` 필드에 따라 다르다는 것을 알고 있습니다. 또한 `sayHello`가 어디에 바인딩되는지, 심지어 import가 허용되는지 여부가 이 파일과 대상 파일의 모듈 종류에 따라 달라질 수 있다는 것도 알고 있습니다. 하지만 대상 파일을 어떻게 _찾는지_는 아직 논의하지 않았습니다.

### 모듈 해결은 호스트가 정의함

ECMAScript 사양은 `import` 및 `export` 문을 파싱하고 해석하는 방법을 정의하지만, 모듈 해결은 호스트에 맡깁니다. 멋진 새 JavaScript 런타임을 만들고 있다면, 다음과 같은 모듈 해결 체계를 만들 수 있습니다:

```ts
import monkey from "🐒"; // './eats/bananas.js'를 찾음
import cow from "🐄";    // './eats/grass.js'를 찾음
import lion from "🦁";   // './eats/you.js'를 찾음
```

그리고 여전히 "표준 호환 ESM"을 구현한다고 주장할 수 있습니다. 말할 필요도 없이, TypeScript는 이 런타임의 모듈 해결 알고리즘에 대한 내장 지식 없이는 `monkey`, `cow`, `lion`에 어떤 타입을 할당해야 하는지 알 수 없습니다. `module`이 컴파일러에게 호스트의 예상 모듈 형식을 알려주는 것처럼, `moduleResolution`은 몇 가지 사용자 정의 옵션과 함께 호스트가 모듈 지정자를 파일로 해결하는 데 사용하는 알고리즘을 지정합니다. 이것은 또한 TypeScript가 emit 중에 import 지정자를 수정하지 않는 이유를 명확히 합니다: import 지정자와 디스크의 파일 사이의 관계(존재한다면)는 호스트가 정의하며, TypeScript는 호스트가 아닙니다.

사용 가능한 `moduleResolution` 옵션은 다음과 같습니다:

- [**`classic`**](/docs/handbook/modules/reference.html#classic): TypeScript의 가장 오래된 모듈 해결 모드로, 불행히도 `module`이 `commonjs`, `node16`, 또는 `nodenext` 이외의 것으로 설정될 때 기본값입니다. [RequireJS](https://requirejs.org/docs/api.html#packages) 구성의 넓은 범위에 대해 최선의 해결을 제공하기 위해 만들어졌을 것입니다. 새 프로젝트(또는 RequireJS나 다른 AMD 모듈 로더를 사용하지 않는 오래된 프로젝트)에 사용해서는 안 되며, TypeScript 6.0에서 더 이상 사용되지 않을 예정입니다.
- [**`node10`**](/docs/handbook/modules/reference.html#node10-formerly-known-as-node): 이전에 `node`로 알려졌으며, `module`이 `commonjs`로 설정될 때 불행한 기본값입니다. v12보다 오래된 Node.js 버전에 대한 꽤 좋은 모델이며, 때때로 대부분의 번들러가 모듈 해결을 수행하는 방식에 대한 합리적인 근사치입니다. `node_modules`에서 패키지 조회, 디렉토리 `index.js` 파일 로드, 상대 모듈 지정자에서 `.js` 확장자 생략을 지원합니다. 그러나 Node.js v12는 ES 모듈에 대해 다른 모듈 해결 규칙을 도입했기 때문에, 최신 버전의 Node.js에 대한 매우 나쁜 모델입니다. 새 프로젝트에 사용해서는 안 됩니다.
- [**`node16`**](/docs/handbook/modules/reference.html#node16-nodenext-1): `--module node16` 및 `--module node18`의 대응물이며 해당 `module` 설정과 함께 기본으로 설정됩니다. Node.js v12 이상은 ESM과 CJS를 모두 지원하며, 각각 자체 모듈 해결 알고리즘을 사용합니다. Node.js에서 import 문과 동적 `import()` 호출의 모듈 지정자는 파일 확장자나 `/index.js` 접미사를 생략할 수 없지만, `require` 호출의 모듈 지정자는 그렇게 할 수 있습니다. 이 모듈 해결 모드는 `--module node16`/`node18`에 의해 설정된 [모듈 형식 감지 규칙](#모듈-형식-감지)에 의해 결정되는 필요한 곳에서 이 제한을 이해하고 적용합니다.
- [**`nodenext`**](/docs/handbook/modules/reference.html#node16-nodenext-1): 현재 `node16`과 동일하며, `--module nodenext`의 대응물이고 해당 `module` 설정과 함께 기본으로 설정됩니다. 새로운 Node.js 모듈 해결 기능이 추가됨에 따라 이를 지원하는 미래 지향적 모드가 되도록 의도되었습니다.
- [**`bundler`**](/docs/handbook/modules/reference.html#bundler): Node.js v12는 npm 패키지를 가져오기 위한 몇 가지 새로운 모듈 해결 기능을 도입했습니다 - `package.json`의 `"exports"` 및 `"imports"` 필드 - 그리고 많은 번들러가 ESM import에 대한 더 엄격한 규칙 없이 이러한 기능을 채택했습니다. 이 모듈 해결 모드는 번들러를 대상으로 하는 코드에 대한 기본 알고리즘을 제공합니다. 기본적으로 `package.json` `"exports"` 및 `"imports"`를 지원하지만 이를 무시하도록 구성할 수 있습니다. `module`을 `esnext`로 설정해야 합니다.

### TypeScript는 호스트의 모듈 해결을 모방하지만, 타입과 함께

모듈과 관련한 TypeScript [역할](#모듈에-관한-typescripts-역할)의 세 가지 구성 요소를 기억하세요?

1. 파일을 유효한 **출력 모듈 형식**으로 컴파일
2. 해당 **출력**의 import가 **성공적으로 해결**되도록 보장
3. **가져온 이름**에 어떤 **타입**을 할당할지 앎

모듈 해결은 마지막 두 가지를 수행하는 데 필요합니다. 하지만 대부분의 시간을 입력 파일에서 작업할 때, (2)를 잊기 쉽습니다 - 모듈 해결의 핵심 구성 요소는 [입력 파일과 동일한 모듈 지정자](#모듈-지정자는-기본적으로-변환되지-않음)를 포함하는 출력 파일의 import 또는 `require` 호출이 실제로 런타임에 작동하는지 검증하는 것입니다.

### 선언 파일의 역할

이전 예제에서 입력 및 출력 파일 사이에서 작동하는 모듈 해결의 "재매핑" 부분을 보았습니다. 하지만 라이브러리 코드를 가져올 때 어떻게 될까요? 라이브러리가 TypeScript로 작성되었더라도 소스 코드를 게시하지 않았을 수 있습니다. 라이브러리의 JavaScript 파일을 TypeScript 파일로 매핑하는 것에 의존할 수 없다면, import가 런타임에 작동하는지 확인할 수 있지만, 타입을 할당하는 두 번째 목표는 어떻게 달성할까요?

이것이 선언 파일(`.d.ts`, `.d.mts` 등)이 필요한 곳입니다. 선언 파일이 어떻게 해석되는지 이해하는 가장 좋은 방법은 그것들이 어디서 오는지 이해하는 것입니다. 입력 파일에서 `tsc --declaration`을 실행하면, 하나의 출력 JavaScript 파일과 하나의 출력 선언 파일을 얻습니다.

이러한 관계 때문에, 컴파일러는 선언 파일을 볼 때마다 선언 파일의 타입 정보에 의해 완벽하게 설명되는 해당 JavaScript 파일이 있다고 _가정_합니다. 성능상의 이유로, 모든 모듈 해결 모드에서 컴파일러는 항상 TypeScript 및 선언 파일을 먼저 찾고, 하나를 찾으면 해당 JavaScript 파일을 계속 찾지 않습니다. TypeScript 입력 파일을 찾으면 컴파일 후 JavaScript 파일이 _존재할_ 것이라는 것을 알고, 선언 파일을 찾으면 컴파일(아마도 다른 사람의)이 이미 발생했고 선언 파일과 동시에 JavaScript 파일을 만들었다는 것을 압니다.

선언 파일은 컴파일러에게 JavaScript 파일이 존재한다는 것뿐만 아니라 그 파일의 이름과 확장자가 무엇인지도 알려줍니다:

| 선언 파일 확장자 | JavaScript 파일 확장자 | TypeScript 파일 확장자 |
| ---------------- | ---------------------- | ---------------------- |
| `.d.ts`          | `.js`                  | `.ts`                  |
| `.d.ts`          | `.js`                  | `.tsx`                 |
| `.d.mts`         | `.mjs`                 | `.mts`                 |
| `.d.cts`         | `.cjs`                 | `.cts`                 |
| `.d.*.ts`        | `.*`                   |                        |

마지막 행은 `allowArbitraryExtensions` 컴파일러 옵션으로 비-JS 파일에 타입을 지정하여 모듈 시스템이 비-JS 파일을 JavaScript 객체로 가져오는 것을 지원하는 경우를 표현합니다. 예를 들어, `styles.css`라는 파일은 `styles.d.css.ts`라는 선언 파일로 표현될 수 있습니다.

### 번들러, TypeScript 런타임 및 Node.js 로더를 위한 모듈 해결

지금까지 _입력 파일_과 _출력 파일_ 사이의 구별을 정말 강조했습니다. 상대 모듈 지정자에 파일 확장자를 지정할 때, TypeScript는 일반적으로 [_출력_ 파일 확장자를 사용하도록](#typescript는-호스트의-모듈-해결을-모방하지만-타입과-함께) 요구한다는 것을 기억하세요:

```ts
// @Filename: src/math.ts
export function add(a: number, b: number) {
  return a + b;
}

// @Filename: src/main.ts
import { add } from "./math.ts";
//                  ^^^^^^^^^^^
// 'allowImportingTsExtensions'가 활성화된 경우에만 import 경로는 '.ts' 확장자로 끝날 수 있습니다.
```

TypeScript가 확장자를 `.js`로 [다시 작성하지 않으므로](#모듈-지정자는-기본적으로-변환되지-않음) 이 제한이 적용되며, 출력 JS 파일에 `"./math.ts"`가 나타나면 해당 import는 런타임에 다른 JS 파일로 해결되지 않습니다. TypeScript는 안전하지 않은 출력 JS 파일을 생성하는 것을 정말로 방지하고 싶어합니다. 하지만 출력 JS 파일이 _없다면_ 어떨까요? 다음 상황 중 하나에 있다면:

- 이 코드를 번들링하고 있고, 번들러가 메모리에서 TypeScript 파일을 트랜스파일하도록 구성되어 있으며, 결국 작성한 모든 import를 소비하고 지워 번들을 생성합니다.
- Deno나 Bun과 같은 TypeScript 런타임에서 이 코드를 직접 실행하고 있습니다.
- `ts-node`, `tsx`, 또는 Node용 다른 트랜스파일링 로더를 사용하고 있습니다.

이러한 경우, `noEmit`(또는 `emitDeclarationOnly`)과 `allowImportingTsExtensions`를 켜서 안전하지 않은 JavaScript 파일 출력을 비활성화하고 `.ts` 확장자 import에 대한 오류를 무음으로 만들 수 있습니다.

`allowImportingTsExtensions` 유무에 관계없이, 모듈 해결 호스트에 가장 적합한 `moduleResolution` 설정을 선택하는 것은 여전히 중요합니다. 번들러와 Bun 런타임의 경우 `bundler`입니다. 이러한 모듈 해결기는 Node.js에서 영감을 받았지만, Node.js가 import에 적용하는 [확장자 검색을 비활성화](#확장자-검색-및-디렉토리-인덱스-파일)하는 엄격한 ESM 해결 알고리즘을 채택하지 않았습니다. `bundler` 모듈 해결 설정은 `node16`-`nodenext`처럼 `package.json` `"exports"` 지원을 활성화하면서도 항상 확장자 없는 import를 허용하는 이것을 반영합니다.

### 라이브러리를 위한 모듈 해결

앱을 컴파일할 때, 모듈 해결 [호스트](#모듈-해결은-호스트가-정의함)가 누구인지에 따라 TypeScript 프로젝트에 대한 `moduleResolution` 옵션을 선택합니다. 라이브러리를 컴파일할 때는 출력 코드가 어디에서 실행될지 모르지만, 가능한 많은 곳에서 실행되기를 원합니다. `"module": "node18"`(암시적 [`"moduleResolution": "node16"`](/docs/handbook/modules/reference.html#node16-nodenext-1)과 함께)을 사용하면 출력 JavaScript의 모듈 지정자 호환성을 최대화할 수 있습니다. Node.js의 `import` 모듈 해결에 대한 더 엄격한 규칙을 따르도록 강제하기 때문입니다. 라이브러리가 `"moduleResolution": "bundler"`(또는 더 나쁜, `"node10"`)로 컴파일하면 어떻게 되는지 살펴봅시다:

```ts
export * from "./utils";
```

`./utils.ts`(또는 `./utils/index.ts`)가 존재한다고 가정하면, 번들러는 이 코드에 문제가 없으므로 `"moduleResolution": "bundler"`는 불평하지 않습니다. `"module": "esnext"`로 컴파일하면, 이 export 문에 대한 출력 JavaScript는 입력과 정확히 같아 보일 것입니다. 해당 JavaScript가 npm에 게시되면 번들러를 사용하는 프로젝트에서 사용할 수 있지만, Node.js에서 실행하면 오류가 발생합니다:

```
Error [ERR_MODULE_NOT_FOUND]: Cannot find module '.../node_modules/dependency/utils' imported from .../node_modules/dependency/index.js
Did you mean to import ./utils.js?
```

반면, 다음과 같이 작성했다면:

```ts
export * from "./utils.js";
```

이것은 Node.js _와_ 번들러 모두에서 작동하는 출력을 생성합니다.

요컨대, `"moduleResolution": "bundler"`는 전염성이 있어서 번들러에서만 작동하는 코드를 생성할 수 있습니다. 마찬가지로, `"moduleResolution": "nodenext"`는 출력이 Node.js에서만 작동하는지 확인하지만, 대부분의 경우 Node.js에서 작동하는 모듈 코드는 다른 런타임과 번들러에서도 작동합니다.

물론, 이 지침은 라이브러리가 `tsc`의 출력을 제공하는 경우에만 적용될 수 있습니다. 라이브러리가 제공하기 _전에_ 번들링되면, `"moduleResolution": "bundler"`가 허용될 수 있습니다. 최종 빌드의 안전성과 호환성을 보장하는 것은 라이브러리의 최종 빌드를 생성하기 위해 모듈 형식이나 모듈 지정자를 변경하는 모든 빌드 도구의 책임이며, `tsc`는 런타임에 어떤 모듈 코드가 존재할지 알 수 없으므로 더 이상 해당 작업에 기여할 수 없습니다.

---

# 모듈 - 참조

> **원문:** https://www.typescriptlang.org/docs/handbook/modules/reference.html

## 모듈 구문

TypeScript 컴파일러는 TypeScript와 JavaScript 파일에서 표준 [ECMAScript 모듈 구문](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Modules)과 JavaScript 파일에서 많은 형태의 [CommonJS 구문](https://www.typescriptlang.org/docs/handbook/type-checking-javascript-files.html#commonjs-modules-are-supported)을 인식합니다.

TypeScript 파일 및/또는 JSDoc 주석에서 사용할 수 있는 몇 가지 TypeScript 전용 구문 확장도 있습니다.

### TypeScript 전용 선언의 가져오기 및 내보내기

타입 별칭, 인터페이스, 열거형, 네임스페이스는 표준 JavaScript 선언처럼 `export` 수정자와 함께 모듈에서 내보낼 수 있습니다:

```ts
// 표준 JavaScript 구문...
export function f() {}
// ...타입 선언으로 확장됨
export type SomeType = /* ... */;
export interface SomeInterface { /* ... */ }
```

표준 JavaScript 선언에 대한 참조와 함께 명명된 내보내기에서도 참조할 수 있습니다:

```ts
export { f, SomeType, SomeInterface };
```

내보낸 타입(및 기타 TypeScript 전용 선언)은 표준 ECMAScript import로 가져올 수 있습니다:

```ts
import { f, SomeType, SomeInterface } from "./module.js";
```

네임스페이스 가져오기 또는 내보내기를 사용할 때, 내보낸 타입은 타입 위치에서 참조될 때 네임스페이스에서 사용할 수 있습니다:

```ts
import * as mod from "./module.js";
mod.f();
mod.SomeType; // 'SomeType' 속성이 'typeof import("./module.js")' 타입에 존재하지 않습니다
let x: mod.SomeType; // Ok
```

### 타입 전용 가져오기 및 내보내기

JavaScript로 import와 export를 내보낼 때, 기본적으로 TypeScript는 타입 위치에서만 사용되는 import와 타입만 참조하는 export를 자동으로 생략(출력하지 않음)합니다. 타입 전용 가져오기와 내보내기를 사용하여 이 동작을 강제하고 생략을 명시적으로 만들 수 있습니다. `import type`으로 작성된 import 선언, `export type { ... }`으로 작성된 export 선언, `type` 키워드로 접두사가 붙은 import 또는 export 지정자는 모두 출력 JavaScript에서 생략되는 것이 보장됩니다.

```ts
// @Filename: main.ts
import { f, type SomeInterface } from "./module.js";
import type { SomeType } from "./module.js";

class C implements SomeInterface {
  constructor(p: SomeType) {
    f();
  }
}

export type { C };

// @Filename: main.js
import { f } from "./module.js";

class C {
  constructor(p) {
    f();
  }
}
```

값도 `import type`으로 가져올 수 있지만, 출력 JavaScript에 존재하지 않으므로 비출력 위치에서만 사용할 수 있습니다:

```ts
import type { f } from "./module.js";
f(); // 'import type'으로 가져왔기 때문에 'f'를 값으로 사용할 수 없습니다
let otherFunction: typeof f = () => {}; // Ok
```

타입 전용 import 선언은 기본 import와 명명된 바인딩을 모두 선언할 수 없습니다. `type`이 기본 import에 적용되는지 전체 import 선언에 적용되는지 모호해 보이기 때문입니다. 대신 import 선언을 두 개로 분할하거나 `default`를 명명된 바인딩으로 사용하세요:

```ts
import type fs, { BigIntOptions } from "fs";
//          ^^^^^^^^^^^^^^^^^^^^^
// 오류: 타입 전용 import는 기본 import 또는 명명된 바인딩을 지정할 수 있지만, 둘 다는 안 됩니다.

import type { default as fs, BigIntOptions } from "fs"; // Ok
```

### `import()` 타입

TypeScript는 import 선언을 작성하지 않고 모듈의 타입을 참조하기 위해 JavaScript의 동적 `import`와 유사한 타입 구문을 제공합니다:

```ts
// 내보낸 타입에 접근:
type WriteFileOptions = import("fs").WriteFileOptions;
// 내보낸 값의 타입에 접근:
type WriteFileFunction = typeof import("fs").writeFile;
```

이것은 타입을 다른 방식으로 가져올 수 없는 JavaScript 파일의 JSDoc 주석에서 특히 유용합니다:

```ts
/** @type {import("webpack").Configuration} */
module.exports = {
  // ...
}
```

### `export =`와 `import = require()`

CommonJS 모듈을 내보낼 때, TypeScript 파일은 `module.exports = ...`와 `const mod = require("...")` JavaScript 구문의 직접적인 대응물을 사용할 수 있습니다:

```ts
// @Filename: main.ts
import fs = require("fs");
export = fs.readFileSync("...");

// @Filename: main.js
"use strict";
const fs = require("fs");
module.exports = fs.readFileSync("...");
```

이 구문은 변수 선언과 속성 할당이 TypeScript 타입을 참조할 수 없는 반면, 특별한 TypeScript 구문은 가능하기 때문에 JavaScript 대응물 대신 사용되었습니다:

```ts
// @Filename: a.ts
interface Options { /* ... */ }
module.exports = Options; // 오류: 'Options'는 타입만 참조하지만, 여기서 값으로 사용되고 있습니다.
export = Options; // Ok

// @Filename: b.ts
const Options = require("./a");
const options: Options = { /* ... */ }; // 오류: 'Options'는 값을 참조하지만, 여기서 타입으로 사용되고 있습니다.

// @Filename: c.ts
import Options = require("./a");
const options: Options = { /* ... */ }; // Ok
```

### 앰비언트 모듈

TypeScript는 런타임에 존재하지만 해당 파일이 없는 모듈을 선언하기 위한 스크립트(비모듈) 파일의 구문을 지원합니다. 이러한 _앰비언트 모듈_은 일반적으로 Node.js의 `"fs"` 또는 `"path"`와 같이 런타임에서 제공하는 모듈을 나타냅니다:

```ts
declare module "path" {
  export function normalize(p: string): string;
  export function join(...paths: any[]): string;
  export var sep: string;
}
```

앰비언트 모듈이 TypeScript 프로그램에 로드되면, TypeScript는 다른 파일에서 선언된 모듈의 import를 인식합니다:

```ts
// 👇 앰비언트 모듈이 로드되었는지 확인 -
//    path.d.ts가 프로젝트 tsconfig.json에 어떻게든
//    포함되어 있다면 불필요할 수 있습니다.
/// <reference path="path.d.ts" />

import { normalize, join } from "path";
```

_패턴_ 앰비언트 모듈은 이름에 단일 `*` 와일드카드 문자를 포함하여 import 경로에서 0개 이상의 문자와 일치합니다. 이는 커스텀 로더가 제공하는 모듈을 선언하는 데 유용할 수 있습니다:

```ts
declare module "*.html" {
  const content: string;
  export default content;
}
```

## `module` 컴파일러 옵션

이 섹션에서는 각 `module` 컴파일러 옵션 값의 세부 사항을 논의합니다. 옵션이 무엇인지와 전체 컴파일 프로세스에 어떻게 맞는지에 대한 자세한 배경은 [_모듈 출력 형식_](/docs/handbook/modules/theory.html#the-module-output-format) 이론 섹션을 참조하세요. 간략히 말해서, `module` 컴파일러 옵션은 역사적으로 출력된 JavaScript 파일의 출력 모듈 형식을 제어하는 데만 사용되었습니다. 그러나 더 최근의 `node16`, `node18`, `nodenext` 값은 어떤 모듈 형식이 지원되는지, 각 파일의 모듈 형식이 어떻게 결정되는지, 다른 모듈 형식이 어떻게 상호 운용되는지를 포함하여 Node.js의 모듈 시스템의 광범위한 특성을 설명합니다.

### `node16`, `node18`, `node20`, `nodenext`

Node.js는 CommonJS와 ECMAScript 모듈을 모두 지원하며, 각 형식이 어떤 형식이 될 수 있는지와 두 형식이 어떻게 상호 운용될 수 있는지에 대한 특정 규칙이 있습니다. `node16`, `node18`, `nodenext`는 Node.js의 이중 형식 모듈 시스템의 전체 범위를 설명하며, **CommonJS 또는 ESM 형식으로 파일을 출력**합니다. 이것은 다른 모든 `module` 옵션과 다르며, 다른 옵션들은 런타임에 구애받지 않고 모든 출력 파일을 단일 형식으로 강제하여 출력이 런타임에 유효한지 확인하는 것을 사용자에게 맡깁니다.

> 흔한 오해는 `node16`-`nodenext`가 ES 모듈만 출력한다는 것입니다. 실제로 이 모드들은 ES 모듈을 _지원_하는 Node.js 버전을 설명하며, ES 모듈을 _사용_하는 프로젝트만이 아닙니다. ESM과 CommonJS 출력 모두 각 파일의 [감지된 모듈 형식](#모듈-형식-감지)에 따라 지원됩니다. 이들은 Node.js의 이중 모듈 시스템의 복잡성을 반영하는 유일한 `module` 옵션이므로, ES 모듈 사용 여부에 관계없이 Node.js v12 이상에서 실행하려는 모든 앱과 라이브러리에 대한 **유일한 올바른 `module` 옵션**입니다.

#### 모듈 형식 감지

- `.mts`/`.mjs`/`.d.mts` 파일은 항상 ES 모듈입니다.
- `.cts`/`.cjs`/`.d.cts` 파일은 항상 CommonJS 모듈입니다.
- `.ts`/`.tsx`/`.js`/`.jsx`/`.d.ts` 파일은 가장 가까운 상위 package.json 파일에 `"type": "module"`이 포함되어 있으면 ES 모듈이고, 그렇지 않으면 CommonJS 모듈입니다.

#### 상호 운용성 규칙

- **ES 모듈이 CommonJS 모듈을 참조할 때:**
  - CommonJS 모듈의 `module.exports`는 ES 모듈에 대한 기본 import로 사용할 수 있습니다.
  - CommonJS 모듈의 `module.exports`의 속성(`default` 제외)은 ES 모듈에 대한 명명된 import로 사용 가능할 수도 있고 사용 불가능할 수도 있습니다. Node.js는 [정적 분석](https://github.com/nodejs/cjs-module-lexer)을 통해 이를 사용 가능하게 하려고 시도합니다. TypeScript는 선언 파일에서 해당 정적 분석이 성공할지 알 수 없으며, 낙관적으로 성공할 것이라고 가정합니다.
- **CommonJS 모듈이 ES 모듈을 참조할 때:**
  - `node16` 및 `node18`에서 `require`는 ES 모듈을 참조할 수 없습니다.
  - `nodenext`에서 Node.js v22.12.0 이상의 동작을 반영하여 `require`는 ES 모듈을 참조할 수 있습니다.
  - 동적 `import()` 호출은 항상 ES 모듈을 가져오는 데 사용할 수 있습니다.

### `preserve`

`--module preserve`에서(TypeScript 5.4에서 [추가됨](https://www.typescriptlang.org/docs/handbook/release-notes/typescript-5-4.html#support-for-require-calls-in---moduleresolution-bundler-and---module-preserve)) 입력 파일에 작성된 ECMAScript import와 export는 출력에서 보존되고, CommonJS 스타일 `import x = require("...")`와 `export = ...` 문은 CommonJS `require`와 `module.exports`로 출력됩니다. 다시 말해, 각 개별 import 또는 export 문의 형식은 전체 컴파일(또는 전체 파일)에 대해 단일 형식으로 강제되지 않고 보존됩니다.

같은 파일에서 import와 require 호출을 혼합해야 하는 경우는 드물지만, 이 `module` 모드는 대부분의 최신 번들러와 Bun 런타임의 기능을 가장 잘 반영합니다.

#### 예시

```ts
// @Filename: main.ts
import x, { y, z } from "mod";
import mod = require("mod");
const dynamic = import("mod");

export const e1 = 0;
export default "default export";
```

```js
// @Filename: main.js
import x, { y, z } from "mod";
const mod = require("mod");
const dynamic = import("mod");

export const e1 = 0;
export default "default export";
```

### `es2015`, `es2020`, `es2022`, `esnext`

#### 요약

- 번들러, Bun, tsx를 위해 `esnext`와 `--moduleResolution bundler`를 함께 사용하세요.
- Node.js에는 사용하지 마세요. Node.js용 ES 모듈을 출력하려면 package.json의 `"type": "module"`과 함께 `node16`, `node18`, 또는 `nodenext`를 사용하세요.
- 비선언 파일에서는 `import mod = require("mod")`가 허용되지 않습니다.
- `es2020`은 `import.meta` 속성 지원을 추가합니다.
- `es2022`는 최상위 `await` 지원을 추가합니다.
- `esnext`는 ECMAScript 모듈에 대한 Stage 3 제안 지원을 포함할 수 있는 이동 목표입니다.
- 출력된 파일은 ES 모듈이지만, 의존성은 어떤 형식이든 될 수 있습니다.

### `commonjs`

#### 요약

- 이것을 사용하지 않는 것이 좋습니다. Node.js용 CommonJS 모듈을 출력하려면 `node16`, `node18`, 또는 `nodenext`를 사용하세요.
- 출력된 파일은 CommonJS 모듈이지만, 의존성은 어떤 형식이든 될 수 있습니다.
- 동적 `import()`는 `require()` 호출의 Promise로 변환됩니다.
- `esModuleInterop`은 기본 및 네임스페이스 import의 출력 코드에 영향을 미칩니다.

## `moduleResolution` 컴파일러 옵션

이 섹션에서는 여러 `moduleResolution` 모드에서 공유되는 모듈 해결 기능과 프로세스를 설명한 다음, 각 모드의 세부 사항을 지정합니다. 옵션이 무엇인지와 전체 컴파일 프로세스에 어떻게 맞는지에 대한 자세한 배경은 [_모듈 해결_](/docs/handbook/modules/theory.html#module-resolution) 이론 섹션을 참조하세요. 간략히 말해서, `moduleResolution`은 TypeScript가 `import`/`export`/`require` 문의 _모듈 지정자_(문자열 리터럴)를 디스크의 파일로 해결하는 방법을 제어하며, 대상 런타임이나 번들러가 사용하는 모듈 해결기와 일치하도록 설정해야 합니다.

### 공통 기능 및 프로세스

#### 파일 확장자 대체

TypeScript는 항상 타입 정보를 제공할 수 있는 파일로 내부적으로 해결하려고 하며, 런타임이나 번들러가 같은 경로를 사용하여 JavaScript 구현을 제공하는 파일로 해결할 수 있도록 합니다. 지정된 `moduleResolution` 알고리즘에 따라 런타임이나 번들러에서 JavaScript 파일 조회를 트리거하는 모든 모듈 지정자에 대해, TypeScript는 먼저 같은 이름과 유사한 파일 확장자를 가진 TypeScript 구현 파일 또는 타입 선언 파일을 찾으려고 시도합니다.

| 런타임 조회    | TypeScript 조회 #1 | TypeScript 조회 #2 | TypeScript 조회 #3 | TypeScript 조회 #4 | TypeScript 조회 #5 |
| -------------- | ------------------ | ------------------ | ------------------ | ------------------ | ------------------ |
| `/mod.js`      | `/mod.ts`          | `/mod.tsx`         | `/mod.d.ts`        | `/mod.js`          | `./mod.jsx`        |
| `/mod.mjs`     | `/mod.mts`         | `/mod.d.mts`       | `/mod.mjs`         |                    |                    |
| `/mod.cjs`     | `/mod.cts`         | `/mod.d.cts`       | `/mod.cjs`         |                    |                    |

#### 상대 파일 경로 해결

TypeScript의 모든 `moduleResolution` 알고리즘은 파일 확장자를 포함하는 상대 경로로 모듈을 참조하는 것을 지원합니다(위 [규칙에 따라](#파일-확장자-대체) 대체됨):

```ts
// @Filename: a.ts
export {};

// @Filename: b.ts
import {} from "./a.js"; // ✅ 모든 `moduleResolution`에서 작동
```

#### 확장자 없는 상대 경로

일부 경우, 런타임이나 번들러는 상대 경로에서 `.js` 파일 확장자를 생략할 수 있습니다. TypeScript는 `moduleResolution` 설정과 컨텍스트가 런타임이나 번들러가 이를 지원한다고 나타내는 곳에서 이 동작을 지원합니다:

```ts
// @Filename: a.ts
export {};

// @Filename: b.ts
import {} from "./a";
```

#### 디렉토리 모듈 (인덱스 파일 해결)

일부 경우, 파일 대신 디렉토리를 모듈로 참조할 수 있습니다. 가장 간단하고 일반적인 경우, 이것은 런타임이나 번들러가 디렉토리에서 `index.js` 파일을 찾는 것을 포함합니다. TypeScript는 `moduleResolution` 설정과 컨텍스트가 런타임이나 번들러가 이를 지원한다고 나타내는 곳에서 이 동작을 지원합니다:

```ts
// @Filename: dir/index.ts
export {};

// @Filename: b.ts
import {} from "./dir";
```

#### `paths`

TypeScript는 `paths` 컴파일러 옵션으로 베어 지정자에 대한 컴파일러의 모듈 해결을 재정의하는 방법을 제공합니다. 이 기능은 원래 AMD 모듈 로더와 함께 사용하도록 설계되었지만(ESM이 존재하거나 번들러가 널리 사용되기 전에 브라우저에서 모듈을 실행하는 수단), TypeScript가 모델링하지 않는 모듈 해결 기능을 런타임이나 번들러가 지원할 때 오늘날에도 여전히 사용됩니다.

##### `paths`는 emit에 영향을 미치지 않음

`paths` 옵션은 TypeScript가 출력하는 코드에서 import 경로를 변경하지 _않습니다_. 따라서 TypeScript에서는 작동하는 것처럼 보이지만 런타임에 충돌하는 경로 별칭을 만드는 것이 매우 쉽습니다.

#### `node_modules` 패키지 조회

Node.js는 상대 경로, 절대 경로, URL이 아닌 모듈 지정자를 `node_modules` 하위 디렉토리에서 찾는 패키지에 대한 참조로 취급합니다. 번들러는 사용자가 Node.js에서와 동일한 종속성 관리 시스템, 종종 같은 종속성까지도 사용할 수 있도록 이 동작을 편리하게 채택했습니다. `classic`을 제외한 TypeScript의 모든 `moduleResolution` 옵션은 `node_modules` 조회를 지원합니다.

#### package.json `"exports"`

`moduleResolution`이 `node16`, `nodenext`, 또는 `bundler`로 설정되고 `resolvePackageJsonExports`가 비활성화되지 않으면, TypeScript는 [베어 지정자 `node_modules` 패키지 조회](#node_modules-패키지-조회)에 의해 트리거된 패키지 디렉토리에서 해결할 때 Node.js의 [package.json `"exports"` 사양](https://nodejs.org/api/packages.html#packages_package_entry_points)을 따릅니다.

`"exports"`를 통해 모듈 지정자를 파일 경로로 해결하기 위한 TypeScript의 구현은 Node.js를 정확히 따릅니다. 그러나 파일 경로가 해결되면 TypeScript는 타입을 찾기 위해 여전히 여러 파일 확장자를 [시도](#파일-확장자-대체)합니다.

### `node16`, `nodenext`

이 모드들은 Node.js v12 이상의 모듈 해결 동작을 반영합니다. (`node16`과 `nodenext`는 현재 동일하지만, Node.js가 향후 모듈 시스템에 중요한 변경을 하면 `node16`은 고정되고 `nodenext`는 새로운 동작을 반영하도록 업데이트됩니다.) Node.js에서 ECMAScript import에 대한 해결 알고리즘은 CommonJS `require` 호출에 대한 알고리즘과 크게 다릅니다. 해결되는 각 모듈 지정자에 대해, 구문과 가져오는 파일의 [모듈 형식](#모듈-형식-감지)이 먼저 사용되어 모듈 지정자가 출력된 JavaScript에서 `import`에 있을지 `require`에 있을지 결정됩니다.

#### 지원되는 기능

|      | `import` | `require` |
| ---- | -------- | --------- |
| [`paths`](#paths) | ✅ | ✅ |
| [`baseUrl`](#baseurl) | ✅ | ✅ |
| [`node_modules` 패키지 조회](#node_modules-패키지-조회) | ✅ | ✅ |
| [package.json `"exports"`](#packagejson-exports) | ✅ `types`, `node`, `import` 일치 | ✅ `types`, `node`, `require` 일치 |
| [package.json `"imports"` 및 자기 이름 import](#packagejson-imports-및-자기-이름-imports) | ✅ `types`, `node`, `import` 일치 | ✅ `types`, `node`, `require` 일치 |
| [전체 상대 경로](#상대-파일-경로-해결) | ✅ | ✅ |
| [확장자 없는 상대 경로](#확장자-없는-상대-경로) | ❌ | ✅ |
| [디렉토리 모듈](#디렉토리-모듈-인덱스-파일-해결) | ❌ | ✅ |

### `bundler`

`--moduleResolution bundler`는 대부분의 JavaScript 번들러에 공통적인 모듈 해결 동작을 모델링하려고 시도합니다. 간략히 말해서, 이것은 [`node_modules` 조회](#node_modules-패키지-조회), [디렉토리 모듈](#디렉토리-모듈-인덱스-파일-해결), [확장자 없는 경로](#확장자-없는-상대-경로)와 같이 전통적으로 Node.js의 CommonJS `require` 해결 알고리즘과 연관된 모든 동작을 지원하면서도 [package.json `"exports"`](#packagejson-exports)와 [package.json `"imports"`](#packagejson-imports-및-자기-이름-imports)와 같은 최신 Node.js 해결 기능도 지원하는 것을 의미합니다.

#### 지원되는 기능

- [`paths`](#paths) ✅
- [`baseUrl`](#baseurl) ✅
- [`node_modules` 패키지 조회](#node_modules-패키지-조회) ✅
- [package.json `"exports"`](#packagejson-exports) ✅ 구문에 따라 `types`, `import`/`require` 일치
- [package.json `"imports"` 및 자기 이름 import](#packagejson-imports-및-자기-이름-imports) ✅ 구문에 따라 `types`, `import`/`require` 일치
- [전체 상대 경로](#상대-파일-경로-해결) ✅
- [확장자 없는 상대 경로](#확장자-없는-상대-경로) ✅
- [디렉토리 모듈](#디렉토리-모듈-인덱스-파일-해결) ✅

### `node10` (이전에 `node`로 알려짐)

`--moduleResolution node`는 TypeScript 5.0에서 `node10`으로 이름이 변경되었습니다(역호환성을 위해 `node`를 별칭으로 유지). 이것은 v12 이전의 Node.js 버전에 존재했던 CommonJS 모듈 해결 알고리즘을 반영합니다. 더 이상 사용해서는 안 됩니다.

### `classic`

`classic`을 사용하지 마세요.

---

# 네임스페이스 (Namespaces)

> **원문:** https://www.typescriptlang.org/docs/handbook/namespaces.html

> **용어에 대한 참고:**
> TypeScript 1.5에서 명명법이 변경되었다는 것을 주목하는 것이 중요합니다.
> "내부 모듈"은 이제 "네임스페이스"입니다.
> "외부 모듈"은 이제 단순히 "모듈"로, [ECMAScript 2015](https://www.ecma-international.org/ecma-262/6.0/)의 용어에 맞춰졌습니다 (즉, `module X {`는 이제 선호되는 `namespace X {`와 동등합니다).

이 글에서는 TypeScript에서 네임스페이스(이전에는 "내부 모듈")를 사용하여 코드를 구성하는 다양한 방법을 설명합니다.
용어에 대한 참고에서 언급했듯이, "내부 모듈"은 이제 "네임스페이스"라고 합니다.
또한, 내부 모듈을 선언할 때 `module` 키워드가 사용되었던 모든 곳에서 `namespace` 키워드를 대신 사용할 수 있고 사용해야 합니다.
이렇게 하면 유사하게 명명된 용어로 새 사용자를 과부하시켜 혼동을 주는 것을 방지합니다.

## 첫 번째 단계

이 페이지 전체에서 예제로 사용할 프로그램부터 시작하겠습니다.
웹페이지의 폼에서 사용자의 입력을 확인하거나 외부에서 제공된 데이터 파일의 형식을 확인하기 위해 작성할 수 있는 것처럼 간단한 문자열 유효성 검사기의 작은 집합을 작성했습니다.

## 단일 파일의 유효성 검사기

```ts
interface StringValidator {
  isAcceptable(s: string): boolean;
}

let lettersRegexp = /^[A-Za-z]+$/;
let numberRegexp = /^[0-9]+$/;

class LettersOnlyValidator implements StringValidator {
  isAcceptable(s: string) {
    return lettersRegexp.test(s);
  }
}

class ZipCodeValidator implements StringValidator {
  isAcceptable(s: string) {
    return s.length === 5 && numberRegexp.test(s);
  }
}

// 시도할 몇 가지 샘플
let strings = ["Hello", "98052", "101"];

// 사용할 유효성 검사기
let validators: { [s: string]: StringValidator } = {};
validators["ZIP code"] = new ZipCodeValidator();
validators["Letters only"] = new LettersOnlyValidator();

// 각 문자열이 각 유효성 검사기를 통과했는지 표시
for (let s of strings) {
  for (let name in validators) {
    let isMatch = validators[name].isAcceptable(s);
    console.log(`'${s}' ${isMatch ? "matches" : "does not match"} '${name}'.`);
  }
}
```

## 네임스페이싱

더 많은 유효성 검사기를 추가하면, 타입을 추적하고 다른 객체와의 이름 충돌을 걱정하지 않도록 어떤 종류의 조직 체계가 필요합니다.
많은 다른 이름을 전역 네임스페이스에 넣는 대신, 객체를 네임스페이스로 래핑합시다.

이 예제에서는 모든 유효성 검사기 관련 엔티티를 `Validation`이라는 네임스페이스로 이동합니다.
여기서 인터페이스와 클래스가 네임스페이스 외부에서 보이도록 하려면 `export`로 시작합니다.
반대로, 변수 `lettersRegexp`와 `numberRegexp`는 구현 세부 사항이므로, 내보내지 않아 네임스페이스 외부의 코드에 보이지 않습니다.
파일 하단의 테스트 코드에서, 네임스페이스 외부에서 사용될 때 타입의 이름을 한정해야 합니다. 예: `Validation.LettersOnlyValidator`.

## 네임스페이스 유효성 검사기

```ts
namespace Validation {
  export interface StringValidator {
    isAcceptable(s: string): boolean;
  }

  const lettersRegexp = /^[A-Za-z]+$/;
  const numberRegexp = /^[0-9]+$/;

  export class LettersOnlyValidator implements StringValidator {
    isAcceptable(s: string) {
      return lettersRegexp.test(s);
    }
  }

  export class ZipCodeValidator implements StringValidator {
    isAcceptable(s: string) {
      return s.length === 5 && numberRegexp.test(s);
    }
  }
}

// 시도할 몇 가지 샘플
let strings = ["Hello", "98052", "101"];

// 사용할 유효성 검사기
let validators: { [s: string]: Validation.StringValidator } = {};
validators["ZIP code"] = new Validation.ZipCodeValidator();
validators["Letters only"] = new Validation.LettersOnlyValidator();

// 각 문자열이 각 유효성 검사기를 통과했는지 표시
for (let s of strings) {
  for (let name in validators) {
    console.log(
      `"${s}" - ${
        validators[name].isAcceptable(s) ? "matches" : "does not match"
      } ${name}`
    );
  }
}
```

## 여러 파일로 분할

애플리케이션이 커지면, 코드를 여러 파일로 분할하여 유지보수하기 쉽게 하고 싶을 것입니다.

## 다중 파일 네임스페이스

여기서, `Validation` 네임스페이스를 여러 파일로 분할합니다.
파일이 분리되어 있어도, 각각 동일한 네임스페이스에 기여할 수 있으며 모두 한 곳에서 정의된 것처럼 소비할 수 있습니다.
파일 간에 종속성이 있으므로, 파일 간의 관계를 컴파일러에 알려주기 위해 참조 태그를 추가합니다.
테스트 코드는 그 외에는 변경되지 않습니다.

##### Validation.ts

```ts
namespace Validation {
  export interface StringValidator {
    isAcceptable(s: string): boolean;
  }
}
```

##### LettersOnlyValidator.ts

```ts
/// <reference path="Validation.ts" />
namespace Validation {
  const lettersRegexp = /^[A-Za-z]+$/;
  export class LettersOnlyValidator implements StringValidator {
    isAcceptable(s: string) {
      return lettersRegexp.test(s);
    }
  }
}
```

##### ZipCodeValidator.ts

```ts
/// <reference path="Validation.ts" />
namespace Validation {
  const numberRegexp = /^[0-9]+$/;
  export class ZipCodeValidator implements StringValidator {
    isAcceptable(s: string) {
      return s.length === 5 && numberRegexp.test(s);
    }
  }
}
```

##### Test.ts

```ts
/// <reference path="Validation.ts" />
/// <reference path="LettersOnlyValidator.ts" />
/// <reference path="ZipCodeValidator.ts" />

// 시도할 몇 가지 샘플
let strings = ["Hello", "98052", "101"];

// 사용할 유효성 검사기
let validators: { [s: string]: Validation.StringValidator } = {};
validators["ZIP code"] = new Validation.ZipCodeValidator();
validators["Letters only"] = new Validation.LettersOnlyValidator();

// 각 문자열이 각 유효성 검사기를 통과했는지 표시
for (let s of strings) {
  for (let name in validators) {
    console.log(
      `"${s}" - ${
        validators[name].isAcceptable(s) ? "matches" : "does not match"
      } ${name}`
    );
  }
}
```

여러 파일이 관련되면, 모든 컴파일된 코드가 로드되도록 해야 합니다.
이를 수행하는 두 가지 방법이 있습니다.

첫째, [`outFile`](/tsconfig#outFile) 옵션을 사용하여 연결된 출력으로 모든 입력 파일을 단일 JavaScript 출력 파일로 컴파일할 수 있습니다:

```Shell
tsc --outFile sample.js Test.ts
```

컴파일러는 파일에 있는 참조 태그를 기반으로 출력 파일을 자동으로 정렬합니다. 각 파일을 개별적으로 지정할 수도 있습니다:

```Shell
tsc --outFile sample.js Validation.ts LettersOnlyValidator.ts ZipCodeValidator.ts Test.ts
```

또는, 파일별 컴파일(기본값)을 사용하여 각 입력 파일에 대해 하나의 JavaScript 파일을 방출할 수 있습니다.
여러 JS 파일이 생성되면, 웹페이지에서 `<script>` 태그를 사용하여 적절한 순서로 각 방출된 파일을 로드해야 합니다. 예를 들어:

##### MyTestPage.html (발췌)

```html
<script src="Validation.js" type="text/javascript" />
<script src="LettersOnlyValidator.js" type="text/javascript" />
<script src="ZipCodeValidator.js" type="text/javascript" />
<script src="Test.js" type="text/javascript" />
```

## 별칭

네임스페이스 작업을 단순화할 수 있는 또 다른 방법은 `import q = x.y.z`를 사용하여 일반적으로 사용되는 객체에 대해 더 짧은 이름을 만드는 것입니다.
모듈을 로드하는 데 사용되는 `import x = require("name")` 구문과 혼동하지 마세요, 이 구문은 단순히 지정된 심볼에 대한 별칭을 만듭니다.
모듈 가져오기에서 생성된 객체를 포함하여 모든 종류의 식별자에 대해 이러한 종류의 가져오기(일반적으로 별칭이라고 함)를 사용할 수 있습니다.

```ts
namespace Shapes {
  export namespace Polygons {
    export class Triangle {}
    export class Square {}
  }
}

import polygons = Shapes.Polygons;
let sq = new polygons.Square(); // 'new Shapes.Polygons.Square()'와 동일
```

`require` 키워드를 사용하지 않는다는 것에 주목하세요; 대신 가져오는 심볼의 정규화된 이름에서 직접 할당합니다.
이것은 `var`를 사용하는 것과 유사하지만, 가져온 심볼의 타입 및 네임스페이스 의미에서도 작동합니다.
중요하게도, 값의 경우, `import`는 원본 심볼과 구별되는 참조이므로, 별칭이 지정된 `var`에 대한 변경 사항은 원본 변수에 반영되지 않습니다.

## 다른 JavaScript 라이브러리와 작업하기

TypeScript로 작성되지 않은 라이브러리의 형태를 설명하려면, 라이브러리가 노출하는 API를 선언해야 합니다.
대부분의 JavaScript 라이브러리는 몇 가지 최상위 객체만 노출하기 때문에, 네임스페이스는 이를 나타내는 좋은 방법입니다.

구현을 정의하지 않는 선언을 "앰비언트"라고 합니다.
일반적으로 이들은 `.d.ts` 파일에 정의됩니다.
C/C++에 익숙하다면, 이들을 `.h` 파일로 생각할 수 있습니다.
몇 가지 예를 살펴봅시다.

## 앰비언트 네임스페이스

인기 있는 라이브러리 D3는 `d3`라는 전역 객체에 기능을 정의합니다.
이 라이브러리는 (모듈 로더 대신) `<script>` 태그를 통해 로드되기 때문에, 선언에서 네임스페이스를 사용하여 형태를 정의합니다.
TypeScript 컴파일러가 이 형태를 보려면, 앰비언트 네임스페이스 선언을 사용합니다.
예를 들어, 다음과 같이 작성을 시작할 수 있습니다:

##### D3.d.ts (단순화된 발췌)

```ts
declare namespace D3 {
  export interface Selectors {
    select: {
      (selector: string): Selection;
      (element: EventTarget): Selection;
    };
  }

  export interface Event {
    x: number;
    y: number;
  }

  export interface Base extends Selectors {
    event: Event;
  }
}

declare var d3: D3.Base;
```

---

# 네임스페이스와 모듈 (Namespaces and Modules)

> **원문:** https://www.typescriptlang.org/docs/handbook/namespaces-and-modules.html

이 글에서는 TypeScript에서 모듈과 네임스페이스를 사용하여 코드를 구성하는 다양한 방법을 설명합니다.
또한 네임스페이스와 모듈 사용에 대한 고급 주제를 살펴보고, TypeScript에서 사용할 때 흔히 겪는 함정을 다룹니다.

ES 모듈에 대한 자세한 내용은 [모듈](/docs/handbook/modules.html) 문서를 참조하세요.
TypeScript 네임스페이스에 대한 자세한 내용은 [네임스페이스](/docs/handbook/namespaces.html) 문서를 참조하세요.

참고: TypeScript의 _매우_ 오래된 버전에서는 네임스페이스를 '내부 모듈'이라고 불렀으며, 이는 JavaScript 모듈 시스템보다 앞선 것입니다.

## 모듈 사용하기

모듈은 코드와 선언을 모두 포함할 수 있습니다.

모듈은 또한 모듈 로더(CommonJs/Require.js 등) 또는 ES 모듈을 지원하는 런타임에 대한 종속성이 있습니다.
모듈은 더 나은 코드 재사용, 더 강력한 격리, 번들링을 위한 더 나은 도구 지원을 제공합니다.

Node.js 애플리케이션의 경우, 모듈이 기본값이며 **현대 코드에서는 네임스페이스보다 모듈을 권장합니다**.

ECMAScript 2015부터 모듈은 언어의 기본 부분이며, 모든 호환 엔진 구현에서 지원되어야 합니다.
따라서 새 프로젝트의 경우 모듈이 권장되는 코드 구성 메커니즘입니다.

## 네임스페이스 사용하기

네임스페이스는 코드를 구성하는 TypeScript 전용 방법입니다.
네임스페이스는 단순히 전역 네임스페이스의 명명된 JavaScript 객체입니다.
이것은 네임스페이스를 사용하기 매우 간단한 구조로 만듭니다.
모듈과 달리, 여러 파일에 걸쳐 있을 수 있으며 [`outFile`](/tsconfig#outFile)을 사용하여 연결할 수 있습니다.
네임스페이스는 HTML 페이지에서 `<script>` 태그로 모든 종속성이 포함된 웹 애플리케이션에서 코드를 구조화하는 좋은 방법일 수 있습니다.

모든 전역 네임스페이스 오염과 마찬가지로, 특히 대규모 애플리케이션에서 컴포넌트 종속성을 식별하기 어려울 수 있습니다.

## 네임스페이스와 모듈의 함정

이 섹션에서는 네임스페이스와 모듈 사용 시 다양한 일반적인 함정과 이를 피하는 방법을 설명합니다.

### 모듈을 `/// <reference>`하기

흔한 실수는 `import` 문 대신 `/// <reference ... />` 구문을 사용하여 모듈 파일을 참조하려고 하는 것입니다.
이 구별을 이해하려면, 먼저 컴파일러가 `import`의 경로(예: `import x from "...";`, `import x = require("...");` 등의 `...`)를 기반으로 모듈의 타입 정보를 찾는 방법을 이해해야 합니다.

컴파일러는 적절한 경로로 `.ts`, `.tsx`, 그런 다음 `.d.ts`를 찾으려고 합니다.
특정 파일을 찾을 수 없으면, 컴파일러는 _앰비언트 모듈 선언_을 찾습니다.
이들은 `.d.ts` 파일에 선언되어야 함을 기억하세요.

- `myModules.d.ts`

  ```ts
  // 모듈이 아닌 .d.ts 파일이나 .ts 파일에서:
  declare module "SomeModule" {
    export function fn(): string;
  }
  ```

- `myOtherModule.ts`

  ```ts
  /// <reference path="myModules.d.ts" />
  import * as m from "SomeModule";
  ```

여기서 참조 태그를 통해 앰비언트 모듈에 대한 선언을 포함하는 선언 파일을 찾을 수 있습니다.
이것은 여러 TypeScript 샘플이 사용하는 `node.d.ts` 파일이 소비되는 방식입니다.

### 불필요한 네임스페이싱

네임스페이스에서 모듈로 프로그램을 변환하는 경우, 다음과 같은 파일이 되기 쉽습니다:

- `shapes.ts`

  ```ts
  export namespace Shapes {
    export class Triangle {
      /* ... */
    }
    export class Square {
      /* ... */
    }
  }
  ```

여기서 최상위 네임스페이스 `Shapes`는 아무 이유 없이 `Triangle`과 `Square`를 래핑합니다.
이것은 모듈의 소비자에게 혼란스럽고 성가십니다:

- `shapeConsumer.ts`

  ```ts
  import * as shapes from "./shapes";
  let t = new shapes.Shapes.Triangle(); // shapes.Shapes?
  ```

TypeScript에서 모듈의 핵심 기능은 두 개의 다른 모듈이 동일한 스코프에 이름을 기여하지 않는다는 것입니다.
모듈의 소비자가 어떤 이름을 할당할지 결정하기 때문에, 네임스페이스에 내보낸 심볼을 능동적으로 래핑할 필요가 없습니다.

모듈 내용을 네임스페이스화하면 안 되는 이유를 반복하면, 네임스페이싱의 일반적인 아이디어는 구조의 논리적 그룹화를 제공하고 이름 충돌을 방지하는 것입니다.
모듈 파일 자체가 이미 논리적 그룹화이고, 최상위 이름은 가져오는 코드에 의해 정의되므로, 내보낸 객체에 대한 추가 모듈 레이어를 사용할 필요가 없습니다.

다음은 수정된 예제입니다:

- `shapes.ts`

  ```ts
  export class Triangle {
    /* ... */
  }
  export class Square {
    /* ... */
  }
  ```

- `shapeConsumer.ts`

  ```ts
  import * as shapes from "./shapes";
  let t = new shapes.Triangle();
  ```

### 모듈의 트레이드오프

JS 파일과 모듈 사이에 일대일 대응이 있는 것처럼, TypeScript는 모듈 소스 파일과 방출된 JS 파일 사이에 일대일 대응이 있습니다.
이것의 한 가지 효과는 대상으로 하는 모듈 시스템에 따라 여러 모듈 소스 파일을 연결할 수 없다는 것입니다.
예를 들어, `commonjs`나 `umd`를 대상으로 할 때 [`outFile`](/tsconfig#outFile) 옵션을 사용할 수 없지만, TypeScript 1.8 이상에서는 `amd`나 `system`을 대상으로 할 때 [`outFile`을 사용할 수 있습니다](./release-notes/typescript-1-8.html#concatenate-amd-and-system-modules-with---outfile).

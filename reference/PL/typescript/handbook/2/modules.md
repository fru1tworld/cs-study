# 모듈

JavaScript는 코드를 모듈화하는 다양한 방법의 오랜 역사를 가지고 있습니다.
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

TypeScript는 CommonJS 및 AMD `require`에 _직접_ 대응하는 ES Module 구문을 가지고 있습니다. ES Module을 사용한 imports는 _대부분의 경우_ 해당 환경의 `require`와 같지만, 이 구문은 TypeScript 파일에서 CommonJS 출력과 1대1 일치를 보장합니다:

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

어떤 [`target`](/tsconfig#target)을 사용할지는 TypeScript 코드를 실행할 것으로 예상하는 JavaScript 런타임에서 사용 가능한 기능에 의해 결정됩니다. 그것은: 지원하는 가장 오래된 웹 브라우저, 실행될 것으로 예상하는 가장 낮은 버전의 Node.js, 또는 예를 들어 Electron과 같은 런타임의 고유한 제약에서 올 수 있습니다.

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

TypeScript는 ES Modules 표준보다 앞선 `namespaces`라는 자체 모듈 형식을 가지고 있습니다. 이 구문은 복잡한 정의 파일을 만드는 데 유용한 많은 기능을 가지고 있으며, [DefinitelyTyped](https://github.com/DefinitelyTyped/DefinitelyTyped)에서 여전히 활발하게 사용되고 있습니다. 더 이상 사용되지 않는 것은 아니지만, 네임스페이스의 대부분의 기능은 ES Modules에 존재하며, JavaScript의 방향에 맞추기 위해 그것을 사용하는 것을 권장합니다. [네임스페이스 참조 페이지](/docs/handbook/namespaces.html)에서 네임스페이스에 대해 더 배울 수 있습니다.

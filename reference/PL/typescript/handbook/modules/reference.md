# 모듈 - 참조

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

# 모듈 - 이론

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
- 번들러가 TypeScript 입력 또는 출력을 소비하고 번들을 생성하는 경우, 번들러가 호스트입니다. 번들러가 원래 import/require 세트를 보고, 참조하는 파일을 찾고, 원래 import와 require가 지워지거나 인식 불가능하게 변환된 새 파일 또는 파일 세트를 생성했기 때문입니다. (해당 번들 자체가 모듈로 구성될 수 있으며, 이를 실행하는 런타임이 그것의 호스트가 되지만, TypeScript는 번들러 이후에 일어나는 일에 대해 알지 못합니다.)
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

선언 파일은 컴파일러에게 JavaScript 파일이 존재한다는 것뿐만 아니라 그것의 이름과 확장자가 무엇인지도 알려줍니다:

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

# 네임스페이스와 모듈 (Namespaces and Modules)

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
모듈 파일 자체가 이미 논리적 그룹화이고, 최상위 이름은 임포트하는 코드에 의해 정의되므로, 내보낸 객체에 대한 추가 모듈 레이어를 사용할 필요가 없습니다.

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

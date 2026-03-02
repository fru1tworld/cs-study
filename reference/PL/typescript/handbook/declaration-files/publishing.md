# 배포하기

이 가이드의 단계에 따라 선언 파일을 작성했으니, 이제 npm에 배포할 차례입니다. 선언 파일을 npm에 배포하는 두 가지 주요 방법이 있습니다:

1. npm 패키지와 함께 번들링
2. npm의 [@types 조직](https://www.npmjs.com/~types)에 배포

타입이 소스 코드에서 생성되는 경우, 소스 코드와 함께 타입을 배포하세요. TypeScript와 JavaScript 프로젝트 모두 [`declaration`](/tsconfig#declaration)을 통해 타입을 생성할 수 있습니다.

그렇지 않으면, DefinitelyTyped에 타입을 제출하는 것을 권장합니다. 이렇게 하면 npm의 `@types` 조직에 배포됩니다.

## npm 패키지에 선언 포함하기

패키지에 메인 `.js` 파일이 있는 경우, `package.json` 파일에도 메인 선언 파일을 표시해야 합니다. `types` 프로퍼티를 설정하여 번들된 선언 파일을 가리키세요. 예를 들어:

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

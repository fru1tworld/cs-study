# 삼중 슬래시 지시어 (Triple-Slash Directives)

삼중 슬래시 지시어는 단일 XML 태그를 포함하는 한 줄 주석입니다.
주석의 내용은 컴파일러 지시어로 사용됩니다.

삼중 슬래시 지시어는 포함하는 파일의 맨 위에서**만** 유효합니다.
삼중 슬래시 지시어는 다른 삼중 슬래시 지시어를 포함한 한 줄 또는 여러 줄 주석만 앞에 올 수 있습니다.
문이나 선언 뒤에 나오면 일반적인 한 줄 주석으로 처리되며, 특별한 의미를 갖지 않습니다.

TypeScript 5.5부터, 컴파일러는 참조 지시어를 생성하지 않으며, 해당 지시어가 [`preserve="true"`](#preservetrue)로 표시되지 않는 한 수동으로 작성된 삼중 슬래시 지시어를 출력 파일에 방출하지 _않습니다_.

## `/// <reference path="..." />`

`/// <reference path="..." />` 지시어가 이 그룹에서 가장 일반적입니다.
파일 간의 _종속성_ 선언 역할을 합니다.

삼중 슬래시 참조는 컴파일러에게 컴파일 프로세스에 추가 파일을 포함하도록 지시합니다.

또한 [`out`](/tsconfig#out) 또는 [`outFile`](/tsconfig#outFile)을 사용할 때 출력을 정렬하는 방법 역할도 합니다.
파일은 전처리 단계 후 입력과 동일한 순서로 출력 파일 위치에 방출됩니다.

### 입력 파일 전처리

컴파일러는 모든 삼중 슬래시 참조 지시어를 해석하기 위해 입력 파일에 대한 전처리 단계를 수행합니다.
이 과정에서 추가 파일이 컴파일에 추가됩니다.

프로세스는 _루트 파일_ 집합으로 시작합니다;
이들은 커맨드 라인이나 `tsconfig.json` 파일의 [`files`](/tsconfig#files) 목록에 지정된 파일 이름입니다.
이러한 루트 파일은 지정된 순서와 동일한 순서로 전처리됩니다.
파일이 목록에 추가되기 전에, 파일의 모든 삼중 슬래시 참조가 처리되고 해당 대상이 포함됩니다.
삼중 슬래시 참조는 파일에서 보이는 순서대로 깊이 우선 방식으로 해석됩니다.

삼중 슬래시 참조 경로는 상대 경로가 사용된 경우 포함하는 파일을 기준으로 해석됩니다.

### 오류

존재하지 않는 파일을 참조하면 오류입니다.
파일이 자체에 대한 삼중 슬래시 참조를 갖는 것은 오류입니다.

### `--noResolve` 사용하기

컴파일러 플래그 [`noResolve`](/tsconfig#noResolve)가 지정되면, 삼중 슬래시 참조가 무시됩니다; 새 파일을 추가하거나 제공된 파일의 순서를 변경하지 않습니다.

## `/// <reference types="..." />`

_종속성_ 선언 역할을 하는 `/// <reference path="..." />` 지시어와 마찬가지로, `/// <reference types="..." />` 지시어는 패키지에 대한 종속성을 선언합니다.

이러한 패키지 이름을 해석하는 프로세스는 `import` 문에서 모듈 이름을 해석하는 프로세스와 유사합니다.
삼중 슬래시 참조 타입 지시어를 선언 패키지에 대한 `import`로 생각하면 쉽습니다.

예를 들어, 선언 파일에 `/// <reference types="node" />`를 포함하면 이 파일이 `@types/node/index.d.ts`에 선언된 이름을 사용함을 선언합니다;
따라서 이 패키지는 선언 파일과 함께 컴파일에 포함되어야 합니다.

`.ts` 파일에서 `@types` 패키지에 대한 종속성을 선언하려면, 커맨드 라인이나 `tsconfig.json`에서 [`types`](/tsconfig#types)를 대신 사용하세요.
자세한 내용은 [`tsconfig.json` 파일에서 `@types`, `typeRoots` 및 `types` 사용하기](/docs/handbook/tsconfig-json.html#types-typeroots-and-types)를 참조하세요.

## `/// <reference lib="..." />`

이 지시어를 사용하면 파일이 기존 내장 _lib_ 파일을 명시적으로 포함할 수 있습니다.

내장 _lib_ 파일은 _tsconfig.json_의 [`lib`](/tsconfig#lib) 컴파일러 옵션과 동일한 방식으로 참조됩니다 (예: `lib="lib.es2015.d.ts"`가 아닌 `lib="es2015"` 사용 등).

DOM API나 `Symbol` 또는 `Iterable`과 같은 내장 JS 런타임 생성자와 같은 내장 타입에 의존하는 선언 파일 작성자의 경우, 삼중 슬래시 참조 lib 지시어가 권장됩니다. 이전에 이러한 .d.ts 파일은 해당 타입의 순방향/중복 선언을 추가해야 했습니다.

예를 들어, 컴파일의 파일 중 하나에 `/// <reference lib="es2017.string" />`을 추가하는 것은 `--lib es2017.string`으로 컴파일하는 것과 동일합니다.

```ts
/// <reference lib="es2017.string" />

"foo".padStart(4);
```

## `/// <reference no-default-lib="true"/>`

이 지시어는 파일을 _기본 라이브러리_로 표시합니다.
`lib.d.ts` 및 다양한 변형의 맨 위에서 이 주석을 볼 수 있습니다.

이 지시어는 컴파일러에게 기본 라이브러리(즉, `lib.d.ts`)를 컴파일에 포함하지 _않도록_ 지시합니다.
여기서의 영향은 커맨드 라인에서 [`noLib`](/tsconfig#noLib)를 전달하는 것과 유사합니다.

또한 [`skipDefaultLibCheck`](/tsconfig#skipDefaultLibCheck)를 전달할 때, 컴파일러는 `/// <reference no-default-lib="true"/>`가 있는 파일의 검사만 건너뜁니다.

## `/// <amd-module />`

기본적으로 AMD 모듈은 익명으로 생성됩니다.
이로 인해 번들러(예: `r.js`)와 같은 다른 도구를 사용하여 결과 모듈을 처리할 때 문제가 발생할 수 있습니다.

`amd-module` 지시어를 사용하면 컴파일러에 선택적 모듈 이름을 전달할 수 있습니다:

##### amdModule.ts

```ts
/// <amd-module name="NamedModule"/>
export class C {}
```

AMD `define`을 호출하는 일부로 모듈에 `NamedModule` 이름이 할당됩니다:

##### amdModule.js

```js
define("NamedModule", ["require", "exports"], function (require, exports) {
  var C = (function () {
    function C() {}
    return C;
  })();
  exports.C = C;
});
```

## `/// <amd-dependency />`

> **참고**: 이 지시어는 사용되지 않습니다. 대신 `import "moduleName";` 문을 사용하세요.

`/// <amd-dependency path="x" />`는 결과 모듈의 require 호출에 주입되어야 하는 비TS 모듈 종속성에 대해 컴파일러에 알립니다.

`amd-dependency` 지시어는 선택적 `name` 프로퍼티도 가질 수 있습니다; 이를 통해 amd-dependency에 대한 선택적 이름을 전달할 수 있습니다:

```ts
/// <amd-dependency path="legacy/moduleA" name="moduleA"/>
declare var moduleA: MyType;
moduleA.callStuff();
```

생성된 JS 코드:

```js
define(["require", "exports", "legacy/moduleA"], function (
  require,
  exports,
  moduleA
) {
  moduleA.callStuff();
});
```

## `preserve="true"`

삼중 슬래시 지시어는 컴파일러가 출력에서 제거하지 못하도록 `preserve="true"`로 표시할 수 있습니다.

예를 들어, 다음은 출력에서 지워집니다:

```ts
/// <reference path="..." />
/// <reference types="..." />
/// <reference lib="..." />
```

그러나 다음은 유지됩니다:

```ts
/// <reference path="..." preserve="true" />
/// <reference types="..." preserve="true" />
/// <reference lib="..." preserve="true" />
```

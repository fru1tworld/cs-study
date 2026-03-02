# TypeScript를 활용하는 JS 프로젝트

TypeScript의 타입 시스템은 코드베이스로 작업할 때 다양한 수준의 엄격함을 가집니다:

- JavaScript 코드로만 추론에 기반한 타입 시스템
- [JSDoc을 통한](/docs/handbook/jsdoc-supported-types.html) JavaScript에서의 점진적 타이핑
- JavaScript 파일에서 `// @ts-check` 사용
- TypeScript 코드
- [`strict`](/tsconfig#strict)가 활성화된 TypeScript

각 단계는 더 안전한 타입 시스템을 향한 이동을 나타내지만, 모든 프로젝트가 그 수준의 검증을 필요로 하는 것은 아닙니다.

## JavaScript와 함께하는 TypeScript

이것은 자동 완성, 심볼로 이동, 이름 바꾸기와 같은 리팩토링 도구를 제공하기 위해 TypeScript를 사용하는 편집기를 사용할 때입니다. [홈페이지](/)에는 TypeScript 플러그인을 가진 편집기 목록이 있습니다.

## JSDoc을 통한 JS에서의 타입 힌트 제공

`.js` 파일에서 타입은 종종 추론될 수 있습니다. 타입을 추론할 수 없는 경우, JSDoc 구문을 사용하여 지정할 수 있습니다.

선언 앞에 오는 JSDoc 주석은 해당 선언의 타입을 설정하는 데 사용됩니다. 예를 들어:

```js twoslash
/** @type {number} */
var x;

x = 0; // OK
x = false; // OK?!
```

지원되는 JSDoc 패턴의 전체 목록은 [JSDoc 지원 타입](/docs/handbook/jsdoc-supported-types.html)에서 찾을 수 있습니다.

## `@ts-check`

이전 코드 샘플의 마지막 줄은 TypeScript에서 오류를 발생시키지만, JS 프로젝트에서는 기본적으로 그렇지 않습니다. JavaScript 파일에서 오류를 활성화하려면 `.js` 파일의 첫 번째 줄에 `// @ts-check`를 추가하여 TypeScript가 이를 오류로 발생시키도록 하세요.

```js twoslash
// @ts-check
// @errors: 2322
/** @type {number} */
var x;

x = 0; // OK
x = false; // Not OK
```

오류를 추가하려는 JavaScript 파일이 많다면 [`jsconfig.json`](/docs/handbook/tsconfig-json.html)을 사용하도록 전환할 수 있습니다. 파일에 `// @ts-nocheck` 주석을 추가하여 일부 파일의 검사를 건너뛸 수 있습니다.

TypeScript가 동의하지 않는 오류를 제공할 수 있으며, 그런 경우 이전 줄에 `// @ts-ignore` 또는 `// @ts-expect-error`를 추가하여 특정 줄의 오류를 무시할 수 있습니다.

```js twoslash
// @ts-check
/** @type {number} */
var x;

x = 0; // OK
// @ts-expect-error
x = false; // Not OK
```

TypeScript가 JavaScript를 어떻게 해석하는지 더 알아보려면 [TS가 JS를 타입 체크하는 방법](/docs/handbook/type-checking-javascript-files.html)을 읽어보세요.

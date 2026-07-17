# lib.d.ts와 표준 라이브러리 타입 개요

> **원문:** https://www.typescriptlang.org/tsconfig/#lib

---

## lib.d.ts란

TypeScript는 `Array`, `Promise`, `Map`, `Math`, `JSON` 같은 자바스크립트 내장 객체와, 브라우저 환경의 `document`, `window`, `fetch` 같은 DOM API에 대한 타입 정보를 별도의 소스 코드 분석 없이도 알고 있다. 이 정보는 TypeScript 설치 패키지 안에 함께 들어 있는 `lib.*.d.ts` 선언 파일들에서 온다.

- `tsc`를 설치하면 `node_modules/typescript/lib` 아래에 `lib.dom.d.ts`, `lib.es5.d.ts`, `lib.es2015.d.ts` 같은 파일이 다수 포함되어 있다.
- 컴파일러는 프로젝트를 컴파일할 때 이 중 필요한 파일들을 자동으로 전역 스코프에 로드한다.
- 어떤 파일들을 로드할지 결정하는 옵션이 `target`과 `lib`이다.

즉 `Array.prototype.map`을 쓸 때 타입 오류 없이 자동완성이 뜨는 이유, 반대로 `Array.prototype.flatMap`처럼 비교적 최근 메서드를 쓸 때 오류가 나는 이유가 모두 어떤 `lib.*.d.ts`가 로드됐는지에 달려 있다.

## target과 lib의 관계

`lib`을 명시하지 않으면 TypeScript는 `target` 값에 맞춰 기본 라이브러리 세트를 자동으로 골라준다. 이때 단순히 같은 버전의 ECMAScript 타입만 들어가는 게 아니라, 브라우저 환경을 가정해 `DOM`, `DOM.Iterable`, `ScriptHost`가 함께 따라붙는다.

| target | 기본 lib |
|---|---|
| `ES5` | `DOM`, `DOM.Iterable`, `ScriptHost`, `ES5` |
| `ES6` / `ES2015` | `DOM`, `DOM.Iterable`, `ScriptHost`, `ES2015` |
| `ES2017` | `DOM`, `DOM.Iterable`, `ScriptHost`, `ES2017` |
| `ESNext` | `DOM`, `DOM.Iterable`, `ScriptHost`, `ESNext` |

`lib`을 직접 지정하면 이 기본값은 완전히 무시되고, 지정한 목록만 로드된다. 즉 필요한 항목을 빠짐없이 나열해야 한다.

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "lib": ["ES2020", "DOM", "DOM.Iterable"]
  }
}
```

위 설정은 `ES2020` 표준 내장 객체, `DOM` API(`document`, `window` 등), 그리고 `DOM.Iterable`(예: `NodeList`를 `for...of`로 순회)을 포함한다.

### 자주 쓰는 lib 값

- ECMAScript 버전: `ES5`, `ES6`(=`ES2015`), `ES2016` ~ `ES2024`, `ESNext`
- 환경별 API: `DOM`, `DOM.Iterable`, `WebWorker`, `ScriptHost`
- 세분화된 ES2015 조각: `ES2015.Core`, `ES2015.Collection`, `ES2015.Promise`, `ES2015.Proxy`, `ES2015.Symbol` 등

예를 들어 Node.js 서버 코드처럼 브라우저 API가 필요 없는 환경이라면 `DOM`을 아예 빼서 `document`, `window` 같은 이름이 실수로 쓰이는 것을 막을 수 있다.

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "lib": ["ES2020"]
  }
}
```

---

## --lib 옵션이 세분화된 이유 (TypeScript 2.0)

> **원문:** https://www.typescriptlang.org/docs/handbook/release-notes/typescript-2-0.html

TypeScript 2.0 이전에는 최신 내장 API 타입(예: `Promise`, `Map`)을 쓰려면 `target`을 `ES6`로 올리는 수밖에 없었다. 문제는 `target`이 컴파일 결과물의 문법 수준(화살표 함수를 그대로 둘지 `function`으로 다운레벨링할지 등)까지 함께 결정한다는 점이다. 즉 "타입은 최신 걸 쓰고 싶지만 결과 코드는 ES5로 내보내고 싶다"는 요구를 만족시킬 수 없었다.

TypeScript 2.0은 `--lib` 옵션을 도입해 이 둘을 분리했다.

```bash
tsc --target es5 --lib es5,es2015.promise
```

이렇게 하면 출력 코드는 ES5 문법을 유지하면서도, `Promise` 타입만 골라서 전역에 추가할 수 있다. 실행 환경에 polyfill(`core-js` 등)만 채워 넣으면 타입 체크와 실제 런타임이 어긋나지 않는다.

이 변경으로 라이브러리 그룹이 `dom`, `webworker`, `scripthost`, `es5`, `es6`, 그리고 `es2015.core`/`collection`/`iterable`/`promise`/`proxy`/`reflect`/`generator`/`symbol` 등으로 잘게 쪼개졌고, 이후 `es2016`, `es2017` 등도 같은 방식으로 계속 추가되고 있다.

---

## 전역 선언 파일 작성 패턴

> **원문:** https://www.typescriptlang.org/docs/handbook/declaration-files/templates/global-d-ts.html

`lib.d.ts`도 결국 하나의 거대한 **전역(global) 선언 파일**이다. 이 방식이 어떻게 동작하는지 이해하면 `lib.d.ts` 자체를 읽거나, 전역으로 노출되는 라이브러리(jQuery의 `$`처럼 `import` 없이 바로 쓰는 것들)의 타입을 직접 작성할 때 도움이 된다.

### 전역 스크립트 vs 모듈

- 어떤 파일이든 최상단에 `import`나 `export`가 하나라도 있으면 TypeScript는 그 파일을 **모듈**로 취급하고, 그 안의 선언은 해당 모듈 스코프에만 갇힌다.
- 반대로 `import`/`export`가 전혀 없는 `.d.ts` 파일은 **전역 스크립트**로 취급되어, 안에 있는 `declare` 선언이 프로젝트 전체에서 아무 `import` 없이 바로 보인다.

`lib.dom.d.ts`를 보면 `export` 없이 `declare var document: Document;` 같은 형태로만 되어 있는데, 그래서 우리가 어느 파일에서든 `import`를 쓰지 않고도 그냥 `document`를 쓸 수 있다.

```ts
// 전역 스크립트로 인식되는 my-global.d.ts
declare function myLib(input: string): string;

declare namespace myLib {
  const version: string;
  function checkVersion(): boolean;
}
```

이렇게 선언해두면 다른 어떤 `.ts` 파일에서도 `import` 없이 `myLib("x")`나 `myLib.version`을 바로 참조할 수 있다.

### 함수 오버로드와 네임스페이스 병합

전역 선언에서는 같은 이름을 함수, 인터페이스, 네임스페이스로 동시에 선언해서 겹쳐 쓰는 패턴이 흔하다. `lib.es5.d.ts`의 `Array` 인터페이스와 `ArrayConstructor` 인터페이스, 전역 `Array` 변수가 바로 이런 구조다.

```ts
declare function identify(a: number): "number";
declare function identify(a: string): "string";

declare namespace identify {
  const cacheSize: number;
}
```

`identify`는 호출 가능한 함수이면서 동시에 `identify.cacheSize`라는 속성도 갖는다. 이 병합 규칙 덕분에 `lib.d.ts` 안에서 `Array(3)`처럼 함수로 호출하는 것과 `Array.isArray([])`처럼 정적 메서드를 쓰는 것이 동시에 가능해진다.

---

## 표준 라이브러리에 포함된 유틸리티 타입

`lib.es5.d.ts`에는 브라우저나 Node.js 내장 객체 타입뿐 아니라, 다른 타입을 변형해서 새 타입을 만드는 **전역 유틸리티 타입**도 함께 정의되어 있다. `Partial<T>`, `Readonly<T>`, `Pick<T, K>`, `Record<K, T>` 같은 것들이 대표적이며, 별도 `import` 없이 어디서나 쓸 수 있는 이유도 이들이 `lib.d.ts`의 전역 스크립트 영역에 선언되어 있기 때문이다.

```ts
interface Todo {
  title: string;
  description: string;
}

// Partial<T>: 모든 프로퍼티를 선택적으로 바꾼 타입
function updateTodo(todo: Todo, patch: Partial<Todo>) {
  return { ...todo, ...patch };
}

// Pick<T, K>: 일부 프로퍼티만 골라낸 타입
type TodoPreview = Pick<Todo, "title">;

// Record<K, T>: 키 집합 K에 대해 값 타입 T를 매핑한 타입
type TodoMap = Record<string, Todo>;
```

이 유틸리티 타입들은 조건부 타입, 매핑된 타입 같은 언어 기능으로 구현되어 있고, `lib.d.ts` 안에 미리 정의되어 있을 뿐 언어 문법 자체는 아니다. 따라서 원한다면 같은 방식으로 직접 유틸리티 타입을 만들어 쓸 수도 있다.

---

## 정리

- `lib.d.ts`는 자바스크립트 내장 객체와 DOM API, 그리고 `Partial`/`Pick` 같은 유틸리티 타입까지 담고 있는 TypeScript 내장 선언 파일 묶음이다.
- `target`이 어떤 `lib.*.d.ts`를 로드할지 기본값을 정하고, `lib` 옵션으로 이를 세밀하게 재정의할 수 있다.
- `lib`을 직접 지정하면 기본값은 사라지므로 필요한 항목을 모두 나열해야 한다.
- `import`/`export`가 없는 `.d.ts` 파일은 전역 스크립트로 취급되어 프로젝트 전체에 선언이 노출되며, `lib.d.ts`와 서드파티 전역 라이브러리 타입 정의 모두 이 원리를 따른다.

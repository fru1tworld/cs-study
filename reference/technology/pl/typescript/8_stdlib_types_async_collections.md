# TypeScript 표준 라이브러리, Promise, 컬렉션, 이터레이터 타입

## lib.d.ts와 표준 라이브러리 타입 개요

> **원문:** https://www.typescriptlang.org/tsconfig/#lib

---

### lib.d.ts란

TypeScript는 `Array`, `Promise`, `Map`, `Math`, `JSON` 같은 자바스크립트 내장 객체와, 브라우저 환경의 `document`, `window`, `fetch` 같은 DOM API에 대한 타입 정보를 별도의 소스 코드 분석 없이도 알고 있다. 이 정보는 TypeScript 설치 패키지 안에 함께 들어 있는 `lib.*.d.ts` 선언 파일들에서 온다.

- `tsc`를 설치하면 `node_modules/typescript/lib` 아래에 `lib.dom.d.ts`, `lib.es5.d.ts`, `lib.es2015.d.ts` 같은 파일이 다수 포함되어 있다.
- 컴파일러는 프로젝트를 컴파일할 때 이 중 필요한 파일들을 자동으로 전역 스코프에 로드한다.
- 어떤 파일들을 로드할지 결정하는 옵션이 `target`과 `lib`이다.

즉 `Array.prototype.map`을 쓸 때 타입 오류 없이 자동완성이 뜨는 이유, 반대로 `Array.prototype.flatMap`처럼 비교적 최근 메서드를 쓸 때 오류가 나는 이유가 모두 어떤 `lib.*.d.ts`가 로드됐는지에 달려 있다.

### target과 lib의 관계

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

#### 자주 쓰는 lib 값

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

### --lib 옵션이 세분화된 이유 (TypeScript 2.0)

> **원문:** https://www.typescriptlang.org/docs/handbook/release-notes/typescript-2-0.html

TypeScript 2.0 이전에는 최신 내장 API 타입(예: `Promise`, `Map`)을 쓰려면 `target`을 `ES6`로 올리는 수밖에 없었다. 문제는 `target`이 컴파일 결과물의 문법 수준(화살표 함수를 그대로 둘지 `function`으로 다운레벨링할지 등)까지 함께 결정한다는 점이다. 즉 "타입은 최신 걸 쓰고 싶지만 결과 코드는 ES5로 내보내고 싶다"는 요구를 만족시킬 수 없었다.

TypeScript 2.0은 `--lib` 옵션을 도입해 이 둘을 분리했다.

```bash
tsc --target es5 --lib es5,es2015.promise
```

이렇게 하면 출력 코드는 ES5 문법을 유지하면서도, `Promise` 타입만 골라서 전역에 추가할 수 있다. 실행 환경에 polyfill(`core-js` 등)만 채워 넣으면 타입 체크와 실제 런타임이 어긋나지 않는다.

이 변경으로 라이브러리 그룹이 `dom`, `webworker`, `scripthost`, `es5`, `es6`, 그리고 `es2015.core`/`collection`/`iterable`/`promise`/`proxy`/`reflect`/`generator`/`symbol` 등으로 잘게 쪼개졌고, 이후 `es2016`, `es2017` 등도 같은 방식으로 계속 추가되고 있다.

---

### 전역 선언 파일 작성 패턴

> **원문:** https://www.typescriptlang.org/docs/handbook/declaration-files/templates/global-d-ts.html

`lib.d.ts`도 결국 하나의 거대한 **전역(global) 선언 파일**이다. 이 방식이 어떻게 동작하는지 이해하면 `lib.d.ts` 자체를 읽거나, 전역으로 노출되는 라이브러리(jQuery의 `$`처럼 `import` 없이 바로 쓰는 것들)의 타입을 직접 작성할 때 도움이 된다.

#### 전역 스크립트 vs 모듈

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

#### 함수 오버로드와 네임스페이스 병합

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

### 표준 라이브러리에 포함된 유틸리티 타입

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

### 정리

- `lib.d.ts`는 자바스크립트 내장 객체와 DOM API, 그리고 `Partial`/`Pick` 같은 유틸리티 타입까지 담고 있는 TypeScript 내장 선언 파일 묶음이다.
- `target`이 어떤 `lib.*.d.ts`를 로드할지 기본값을 정하고, `lib` 옵션으로 이를 세밀하게 재정의할 수 있다.
- `lib`을 직접 지정하면 기본값은 사라지므로 필요한 항목을 모두 나열해야 한다.
- `import`/`export`가 없는 `.d.ts` 파일은 전역 스크립트로 취급되어 프로젝트 전체에 선언이 노출되며, `lib.d.ts`와 서드파티 전역 라이브러리 타입 정의 모두 이 원리를 따른다.

---

## Promise와 비동기 코드의 타입

`Promise`, `async`/`await`, 비동기 이터레이터 같은 문법 자체는 JavaScript(ECMAScript)가 정의하며, TypeScript는 이를 타입 시스템으로 얼마나 정교하게 표현할지를 버전마다 개선해왔다. 이 문서는 다운레벨 컴파일부터 `Awaited<T>` 유틸리티 타입까지, 비동기 코드에 타입을 입히는 과정에서 등장한 주요 개념을 정리한다.

### 구버전 타겟에서의 async/await 컴파일

> **원문:** https://www.typescriptlang.org/docs/handbook/release-notes/typescript-2-1.html

- `async`/`await`는 원래 ES2015(ES6) 이상을 타겟으로 할 때만 쓸 수 있었다. TypeScript 2.1부터는 ES3, ES5 타겟에서도 사용할 수 있게 됐다.
- 컴파일러가 `async` 함수를 상태 기계(state machine) 형태의 일반 함수로 변환해주기 때문에 가능한 일이다. 다만 변환된 코드가 내부적으로 `Promise`를 사용하므로, 실행 환경에 표준을 만족하는 `Promise` 구현체가 있어야 한다(없으면 폴리필 필요).
- 타입 체크를 위해서는 `lib` 옵션에 `Promise` 타입 정의를 포함해야 한다.

```json
// tsconfig.json
{
  "compilerOptions": {
    "target": "es5",
    "lib": ["dom", "es5", "es2015.promise"]
  }
}
```

```ts
function delay(ms: number) {
  return new Promise<void>((resolve) => setTimeout(resolve, ms));
}

async function greet() {
  await delay(300);
  console.log("hello");
}
```

- `es2015.promise` 라이브러리를 추가해야 `Promise`, `Promise.all`, `Promise.race` 등의 타입을 인식한다. 빠뜨리면 "Cannot find name 'Promise'" 같은 에러가 난다.

#### tslib과 헬퍼 함수 공유

- ES3/ES5로 다운레벨 컴파일하면 파일마다 `__awaiter` 같은 헬퍼 함수 코드가 반복 삽입된다.
- `--importHelpers` 옵션과 `tslib` 패키지를 함께 쓰면 헬퍼를 매번 인라인하지 않고 하나의 모듈에서 가져와 쓰므로 번들 크기를 줄일 수 있다.

```bash
npm install tslib
tsc --module commonjs --importHelpers app.ts
```

#### 새로운 target 값

- `ES2016`, `ES2017`, `ESNext` 타겟이 추가됐다. 예를 들어 `--target ES2017`을 쓰면 `async`/`await` 같은 ES2017 문법을 그대로 두고 하위 문법만 변환한다.

### 비동기 이터레이터, 비동기 제너레이터, for-await-of

> **원문:** https://www.typescriptlang.org/docs/handbook/release-notes/typescript-2-3.html

TypeScript 2.3에서는 TC39의 비동기 반복(async iteration) 제안을 타입으로 지원하기 시작했다.

#### AsyncIterator

- 일반 `Iterator`와 모양은 같지만 `next`, `return`, `throw`가 결과 값을 `Promise`로 감싸서 반환한다.

```ts
interface AsyncIterator<T> {
  next(value?: any): Promise<IteratorResult<T>>;
  return?(value?: any): Promise<IteratorResult<T>>;
  throw?(e?: any): Promise<IteratorResult<T>>;
}
```

- 어떤 객체가 비동기 반복을 지원한다는 것은 `Symbol.asyncIterator` 메서드를 구현해서 위 `AsyncIterator`를 반환한다는 뜻이다.

#### 비동기 제너레이터

- `async function*`으로 선언하며, 매 `yield`마다 값을 즉시 반환하지 않고 `Promise`로 감싸서 반환한다. 화살표 함수로는 만들 수 없고 함수 선언식, 함수 표현식, 클래스/객체 메서드로만 작성할 수 있다.

```ts
async function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function* countSlowly() {
  yield 1;
  await sleep(100);
  yield* [2, 3]; // 일반 이터러블에도 위임 가능
}
```

#### for-await-of

- 비동기 이터러블을 순회하기 위한 문법으로, `async` 함수나 비동기 제너레이터 내부에서만 쓸 수 있다.

```ts
async function main() {
  for await (const n of countSlowly()) {
    console.log(n);
  }
}
```

#### 실행 환경 요구 사항

- 런타임에 `Symbol.asyncIterator`가 없으면 직접 채워 넣어야 한다.

  ```ts
  (Symbol as any).asyncIterator =
    Symbol.asyncIterator || Symbol.for("Symbol.asyncIterator");
  ```

- `AsyncIterator` 등의 타입 선언을 쓰려면 `lib`에 `esnext`를 포함해야 하고, ES5/ES3 타겟에서는 `--downlevelIterators` 플래그도 함께 켜야 한다.

### Awaited\<T\>: Promise 중첩을 풀어내는 타입

> **원문:** https://www.typescriptlang.org/docs/handbook/release-notes/typescript-4-5.html

#### 왜 필요했나

`await`나 `Promise.prototype.then`은 값이 `Promise`이면 그 안의 값을 꺼내고, `Promise`가 아니면 값을 그대로 반환한다. 그런데 `PromiseLike<T>`처럼 프로미스와 유사하지만 `Promise`는 아닌 값, 혹은 프로미스 안에 프로미스가 중첩된 경우(`Promise<Promise<T>>`)까지 고려하면 이 "풀어내기" 동작을 타입 수준에서 정확히 표현하기가 까다로웠다. 특히 `Promise.all`처럼 여러 값을 한꺼번에 다루는 API에서 타입 추론이 어긋나는 문제가 자주 발생했다.

#### 동작 방식

`Awaited<T>`는 `T`가 프로미스면 그 내부 타입을 꺼내고, 내부 타입이 또 프로미스면 재귀적으로 계속 풀어낸다. 프로미스가 아닌 값이 섞여 있으면 그 값은 그대로 유지된다.

```ts
type A = Awaited<Promise<string>>; // string
type B = Awaited<Promise<Promise<number>>>; // number (중첩도 재귀적으로 해제)
type C = Awaited<boolean | Promise<number>>; // boolean | number
```

#### Promise.all이 좋아진 예

TypeScript 4.5 이전에는 값이 `T | Promise<T>`처럼 프로미스일 수도 아닐 수도 있는 형태이면 `Promise.all` 결과 타입이 원래 의도와 어긋나는 경우가 있었다.

```ts
declare function maybeAsync<T>(v: T): T | Promise<T>;

async function run(): Promise<[number, number]> {
  const [a, b] = await Promise.all([maybeAsync(1), maybeAsync(2)]);
  return [a, b]; // 4.5 이전에는 타입 불일치로 에러가 나던 패턴
}
```

`lib.es5.d.ts`의 `Promise.all`, `Promise.race` 등의 시그니처가 `Awaited`를 사용하도록 다시 작성되면서, 이런 코드에서도 결과 타입이 기대한 대로 추론된다.

#### 정리

| 시점 | 발전 내용 |
| --- | --- |
| TS 2.1 | ES3/ES5에서도 `async`/`await` 사용 가능(다운레벨 컴파일 + `Promise` 타입/폴리필 필요) |
| TS 2.3 | `AsyncIterator`, 비동기 제너레이터, `for await...of` 타입 지원 |
| TS 4.5 | `Awaited<T>`로 중첩된 프로미스 타입을 재귀적으로 풀어내고, `Promise.all`류 API의 추론 정확도 개선 |

---

## Map·Set·WeakMap·WeakSet 타입

TypeScript는 `Map`, `Set`, `WeakMap`, `WeakSet`을 직접 만들지 않는다. 이들은 자바스크립트 런타임(엔진)이 제공하는 내장 객체이고, TypeScript는 `lib.es2015.collection.d.ts` 같은 선언 파일을 통해 여기에 제네릭 타입을 입혀줄 뿐이다. 그래서 이 네 가지를 제대로 쓰려면 (1) 자바스크립트 런타임 동작 자체와 (2) TypeScript가 `tsconfig.json`의 `lib` 옵션으로 그 동작에 타입을 매핑하는 방식을 함께 알아야 한다.

### Map

> **원문:** https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Map

- 키-값 쌍을 저장하며 **삽입 순서**를 기억하는 컬렉션이다.
- 일반 객체와 달리 키로 문자열/심벌뿐 아니라 객체, 함수, 숫자 등 어떤 값이든 쓸 수 있고, 프로토타입 체인에서 오는 의도치 않은 기본 키가 없어 프로토타입 오염 걱정도 없다.
- 키 비교는 `SameValueZero` 알고리즘을 쓴다. `===`와 거의 같지만 `NaN`을 자기 자신과 같다고 취급한다는 점만 다르다. 객체 키는 값이 아니라 **참조 동일성**으로 비교된다.

```ts
const userAge = new Map<string, number>();
userAge.set("bora", 28).set("minsu", 31);

userAge.get("bora");        // number | undefined
userAge.has("minsu");       // boolean
userAge.size;                // number

const objKeyMap = new Map<object, string>();
const key = {};
objKeyMap.set(key, "value");
objKeyMap.get({});           // undefined — 다른 참조라서 못 찾음
objKeyMap.get(key);          // "value"
```

TypeScript 관점에서 중요한 점은 `get`의 반환 타입이 `V | undefined`라는 것이다. 키가 없을 수 있다는 사실을 타입 시스템이 강제로 알려주므로, 사용하기 전에 `undefined` 체크나 널 병합 연산자(`??`)로 좁혀야 한다.

```ts
const value = userAge.get("bora") ?? 0; // number로 좁혀짐
```

`Map`은 이터러블 프로토콜을 구현하므로 `for...of`, 스프레드, `Array.from`과 자연스럽게 어울리고, TypeScript는 이 순회 결과의 타입까지 추론해 준다.

```ts
for (const [name, age] of userAge) {
  //          ^? string, number
}
const entries: [string, number][] = [...userAge];
```

### Set

> **원문:** https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Set

- 중복 없는 값의 컬렉션이며 삽입 순서를 유지한다.
- `Map`과 마찬가지로 `SameValueZero`로 동등성을 판단하므로 `NaN`은 한 번만 저장되지만, 내용이 같은 두 객체 리터럴은 참조가 다르면 별개의 원소로 취급된다.
- 배열의 `includes()`보다 `has()`가 평균적으로 더 빠르므로 "존재 여부 확인"이 주 목적이라면 배열 대신 `Set`을 쓰는 편이 낫다.

```ts
const tags = new Set<string>(["ts", "js"]);
tags.add("go");
tags.has("ts");   // boolean
tags.size;         // number

const nums = new Set<number>();
nums.add(NaN).add(NaN);
nums.size; // 1
```

`Set<T>`도 이터러블이라 배열로 변환할 때 흔히 `[...new Set(arr)]` 형태로 중복 제거 용도로 쓰인다. 이때 결과 타입은 `T[]`로 그대로 유지된다.

```ts
function unique<T>(arr: T[]): T[] {
  return [...new Set(arr)];
}
```

### WeakMap

> **원문:** https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/WeakMap

`WeakMap<K, V>`은 `Map<K, V>`과 겉보기엔 비슷하지만 키를 다루는 방식이 근본적으로 다르다.

- 키로 **객체(또는 등록되지 않은 심벌)만** 허용한다. 원시값은 가비지 컬렉션 대상이 될 수 없어서 애초에 "약한 참조"라는 개념이 성립하지 않기 때문이다.
- 키에 대한 참조가 **약한 참조**(weak reference)다. 다른 곳에서 그 객체를 더 이상 참조하지 않으면 GC가 객체를 회수할 수 있고, 그러면 `WeakMap`의 해당 엔트리도 함께 사라진다.
- 이 비결정적인 특성 때문에 `size`, `keys()`, `values()`, `entries()`, `forEach()`가 **없다**. 순회 결과가 GC 타이밍에 따라 달라지면 안 되기 때문이다. `get`/`set`/`has`/`delete`만 존재한다.

```ts
class Widget {}
const privateData = new WeakMap<Widget, { secret: string }>();

function attach(w: Widget, secret: string) {
  privateData.set(w, { secret });
}
function reveal(w: Widget) {
  return privateData.get(w)?.secret; // string | undefined
}
```

TypeScript 타입 선언에서도 이 제약이 그대로 드러난다. `WeakMap`의 키 타입 파라미터는 `object` 계열로 한정되어 있어서, 문자열이나 숫자를 키로 넣으려 하면 컴파일 타임에 바로 오류가 난다.

```ts
const wm = new WeakMap<object, number>();
// wm.set("id", 1); // 오류: string은 WeakMap의 키가 될 수 없다
```

주 용도는 세 가지다.

- 클래스 인스턴스에 딸린 **비공개 데이터**를 클로저 없이 저장하기
- DOM 노드 같은 객체에 **부가 메타데이터**를 붙이되, 노드가 사라지면 메타데이터도 자동으로 정리되게 하기
- 객체를 키로 하는 **캐시**를 만들되 메모리 누수 없이 하기

### WeakSet

> **원문:** https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/WeakSet

`WeakSet<T>`은 `WeakMap`의 값 없는 버전, 즉 "이 객체를 봤는지 여부"만 기억하고 싶을 때 쓰는 컬렉션이다.

- 저장할 수 있는 값은 `WeakMap`의 키와 동일하게 객체(또는 등록되지 않은 심벌)로 한정된다.
- 참조는 약하며, `size`나 순회 메서드가 없다. `add`/`has`/`delete`만 제공한다.
- 대표적인 용도는 객체 그래프를 재귀적으로 순회할 때 **순환 참조를 탐지**하는 것이다. 이미 방문한 객체를 `WeakSet`에 표시해 두고, 순회가 끝나면 특별히 정리하지 않아도 GC가 알아서 처리해 준다.

```ts
function walk(obj: object, seen = new WeakSet<object>()): void {
  if (seen.has(obj)) return; // 순환 참조 방지
  seen.add(obj);
  for (const value of Object.values(obj)) {
    if (typeof value === "object" && value !== null) {
      walk(value, seen);
    }
  }
}
```

### tsconfig의 lib 옵션과 컬렉션 타입

> **원문:** https://www.typescriptlang.org/tsconfig/#lib

`Map`, `Set`, `WeakMap`, `WeakSet`의 타입 선언은 코드가 아니라 **`lib` 옵션이 어떤 선언 파일을 프로젝트에 포함시키는지**에 달려 있다.

- `target`은 코드가 컴파일될 자바스크립트 버전을 정하고, `lib`은 타입 검사에 쓸 **전역 타입 선언 목록**을 정한다. 둘은 독립적이다. 예를 들어 `target: "es5"`로 낮게 컴파일하면서도 `lib`에 `es2015`를 넣으면 `Promise`, `Map`, `Set` 같은 타입을 타입 체크에서만 사용할 수 있다(런타임에 실제로 존재하는지는 별개 문제다).
- `lib`을 명시하지 않으면 TypeScript가 `target` 값에 맞춰 적절한 기본 라이브러리 집합을 자동으로 골라 준다. `target`이 `es2015` 이상이면 `Map`/`Set`/`WeakMap`/`WeakSet` 타입은 기본으로 포함된다.
- `lib`을 한 번이라도 직접 지정하면 TypeScript는 자동 추가를 멈추고 **명시한 목록만** 사용하므로, 구버전 `target`에서 이 컬렉션 타입을 쓰려면 `es2015.collection`을 직접 추가해야 한다.

```json
{
  "compilerOptions": {
    "target": "es5",
    "lib": ["es5", "dom", "es2015.collection", "es2015.iterable"]
  }
}
```

`es2015.collection`이 빠지면 `new Map()`이나 `new Set()`을 쓰는 순간 "`Map` 이름을 찾을 수 없습니다" 같은 컴파일 오류가 난다. 이터레이터로 순회(`for...of`, 스프레드)까지 하려면 `es2015.iterable`도 함께 있어야 한다. `lib`은 타입 검사 전용 설정이므로 이 값을 바꿔도 실제로 번들에 폴리필이 추가되거나 런타임 동작이 바뀌지는 않는다 — 대상 런타임에 해당 API가 실제로 존재하는지는 여전히 별도로 챙겨야 한다.

---

## 이터레이터·제너레이터 타입 심화와 다운레벨 반복

### 2.3: ES5/ES3에서의 제너레이터·이터레이터 지원 — `downlevelIteration`

`for...of`, 배열 구조 분해, 스프레드 연산자는 원래 ES6 이상 타깃에서만 `Symbol.iterator` 프로토콜을 온전히 활용했다. `--downlevelIteration` 플래그를 켜면 ES5/ES3로 컴파일할 때도 다음이 가능해진다.

- 순회 대상 객체에 `[Symbol.iterator]()`가 있으면 그것을 호출해서 순회
- `Symbol.iterator`가 없는 유사 배열 객체는 합성 배열 이터레이터로 대체 순회

단, 이 옵션은 **런타임에 네이티브 혹은 폴리필된 `Symbol.iterator`가 존재해야만** 의미가 있다. 런타임 지원이 없으면 컴파일러는 그냥 인덱스 기반 순회로 되돌아가며 플래그 자체는 아무 이득도 주지 못한다.

> **원문:** https://www.typescriptlang.org/docs/handbook/release-notes/typescript-2-3.html

### 2.3: 비동기 반복 — `AsyncIterator`, 비동기 제너레이터, `for await...of`

`Iterator`가 동기적으로 `{ value, done }`을 반환한다면, `AsyncIterator`는 매 스텝마다 `Promise<IteratorResult<T>>`를 반환한다.

```ts
interface AsyncIterator<T> {
  next(value?: any): Promise<IteratorResult<T>>;
  return?(value?: any): Promise<IteratorResult<T>>;
  throw?(e?: any): Promise<IteratorResult<T>>;
}
```

이를 구현하는 객체는 `Symbol.asyncIterator` 메서드를 노출한다. `async function*`로 선언하는 비동기 제너레이터는 `yield`뿐 아니라 `await`, `yield*`(다른 (비동기) 이터러블 위임)를 함께 쓸 수 있다.

```ts
async function* countSlowly(n: number) {
  for (let i = 1; i <= n; i++) {
    await delay(100);
    yield i;
  }
}
```

이 결과는 `for await (const x of ...)`로만 소비할 수 있고, 이 문법은 async 함수나 async 제너레이터 본문 안에서만 허용된다.

```ts
async function run() {
  for await (const n of countSlowly(3)) {
    console.log(n);
  }
}
```

런타임 요건은 두 가지다.

- 네이티브(또는 폴리필된) `Symbol.asyncIterator`와 유효한 `Promise` 구현
- `tsconfig`의 `lib`에 `esnext`(또는 이에 상응하는 라이브러리)를 포함해야 `AsyncIterator` 관련 타입 선언을 인식하며, ES5/ES3 타깃이면 여기에 더해 `--downlevelIteration`도 켜야 한다

> **원문:** https://www.typescriptlang.org/docs/handbook/release-notes/typescript-2-3.html

### 3.6: 더 엄격해진 제너레이터 타입 — yield·return·next를 구분하기

3.6 이전에는 제너레이터가 `yield`한 값과 `return`한 값이 같은 유니언 타입으로 뭉뚱그려졌다. 그래서 `done`이 `true`인지 확인한 뒤에도 `value`의 타입이 정확히 좁혀지지 않는 문제가 있었다.

```ts
function* foo() {
  if (Math.random() < 0.5) yield 100;
  return "finished";
}

const it = foo();
const cur = it.next();
if (cur.done) {
  cur.value; // 3.5 이하: string | number, 3.6부터: string
}
```

3.6은 이를 해결하기 위해 `Iterator`/`Generator`에 타입 매개변수 세 개(`T`, `TReturn`, `TNext`)를 도입하고, `IteratorResult`도 `TReturn`을 받도록 확장했다.

```ts
interface Iterator<T, TReturn = any, TNext = undefined> {
  next(...args: [] | [TNext]): IteratorResult<T, TReturn>;
  return?(value?: TReturn): IteratorResult<T, TReturn>;
  throw?(e?: any): IteratorResult<T, TReturn>;
}

interface Generator<T = unknown, TReturn = any, TNext = unknown>
  extends Iterator<T, TReturn, TNext> {
  [Symbol.iterator](): Generator<T, TReturn, TNext>;
}
```

`IteratorResult<T, TReturn>` 자체도 판별 유니언(discriminated union)으로 바뀌어 `done`이 `true`인 쪽과 `false`인 쪽의 `value` 타입이 서로 다르게 추론된다.

```ts
type IteratorResult<T, TReturn = any> =
  | { done?: false; value: T }
  | { done: true; value: TReturn };
```

`yield`로 값을 받는 위치(`x = yield`)의 타입도 이제 `next()`에 넘기는 인자 타입 검사에 반영된다.

```ts
function* ask() {
  const answer: number = yield "무엇을 원하나요?";
  console.log(answer * 2);
}

const g = ask();
g.next();
g.next("문자열"); // 에러: number 자리에 string
```

세 타입 매개변수는 함수 선언에서 `Generator<Yield, Return, Next>` 형태로 명시할 수도 있다.

```ts
function* counter(): Generator<number, string, boolean> {
  let i = 0;
  while (true) {
    if (yield i++) break;
  }
  return "stopped";
}
```

여기서 `counter`는 `number`를 `yield`하고, `boolean`을 `next()`로 받으며, 끝나면 `string`을 반환한다.

> **원문:** https://www.typescriptlang.org/docs/handbook/release-notes/typescript-3-6.html

### `downlevelIteration` 컴파일러 옵션 — 무엇을, 왜 다르게 내보내는가

`target`이 ES5 이하일 때 `for...of`, 배열/인자 스프레드는 기본적으로 인덱스 기반 반복으로 다운레벨링된다. 문제는 이 방식이 서로게이트 쌍으로 이루어진 문자(이모지 등)처럼 `.length`가 여러 개인 "한 글자"를 제대로 순회하지 못한다는 점이다.

```ts
const str = "Hi 😜";
for (const ch of str) {
  console.log(ch);
}
```

`downlevelIteration` 없이 컴파일하면 위 코드는 문자열 인덱싱 기반 `for` 루프로 변환되어 이모지를 반쪽씩 잘라 순회한다. 옵션을 켜면 컴파일러는 `Symbol.iterator`를 확인하는 헬퍼(`__values` 등)를 사용해 실제 이터레이터 프로토콜대로 순회하는 코드를 생성한다.

배열 스프레드의 구멍(hole) 처리도 달라진다.

```ts
const withHole = ["a", , "c"];
const spread = [...withHole];
// downlevelIteration 없이: 구멍이 그대로 유지될 수 있음
// downlevelIteration 적용 시: ES6 스펙대로 구멍을 undefined로 채움
```

정리하면 이 옵션이 손대는 대상은 `for...of` 루프, 배열 스프레드(`[...arr]`), 함수 호출 인자 스프레드(`fn(...args)`), 그리고 이들이 기대는 `Symbol.iterator` 구현이다.

주의할 점은 이 옵션이 컴파일 타임 변환일 뿐이라는 것이다. 대상 런타임에 `Symbol.iterator`가 네이티브로 있거나 폴리필돼 있지 않으면 아무리 플래그를 켜도 실제로는 이전과 동일하게 인덱스 기반으로 되돌아간다. 함께 자주 쓰는 옵션으로 `importHelpers`가 있는데, 이건 매 파일마다 `__values` 같은 헬퍼 코드를 인라인으로 반복해서 넣는 대신 `tslib`에서 가져오도록 해서 번들 크기를 줄여준다.

> **원문:** https://www.typescriptlang.org/tsconfig/#downlevelIteration

### 요약

| 시점 | 핵심 내용 |
| --- | --- |
| 2.3 | `--downlevelIteration`으로 ES5/ES3에서도 이터레이터 프로토콜 지원, `AsyncIterator`·`async function*`·`for await...of`로 비동기 반복 도입 |
| 3.6 | `Iterator`/`Generator`/`IteratorResult`에 `<T, TReturn, TNext>` 세 매개변수를 도입해 yield/return/next 값을 각각 정확히 타이핑 |
| tsconfig | `downlevelIteration`은 어디까지나 트랜스파일 방식의 변경이며, 런타임 `Symbol.iterator` 지원이 전제조건 |

# Promise와 비동기 코드의 타입

`Promise`, `async`/`await`, 비동기 이터레이터 같은 문법 자체는 JavaScript(ECMAScript)가 정의하며, TypeScript는 이를 타입 시스템으로 얼마나 정교하게 표현할지를 버전마다 개선해왔다. 이 문서는 다운레벨 컴파일부터 `Awaited<T>` 유틸리티 타입까지, 비동기 코드에 타입을 입히는 과정에서 등장한 주요 개념을 정리한다.

## 구버전 타겟에서의 async/await 컴파일

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

### tslib과 헬퍼 함수 공유

- ES3/ES5로 다운레벨 컴파일하면 파일마다 `__awaiter` 같은 헬퍼 함수 코드가 반복 삽입된다.
- `--importHelpers` 옵션과 `tslib` 패키지를 함께 쓰면 헬퍼를 매번 인라인하지 않고 하나의 모듈에서 가져와 쓰므로 번들 크기를 줄일 수 있다.

```bash
npm install tslib
tsc --module commonjs --importHelpers app.ts
```

### 새로운 target 값

- `ES2016`, `ES2017`, `ESNext` 타겟이 추가됐다. 예를 들어 `--target ES2017`을 쓰면 `async`/`await` 같은 ES2017 문법을 그대로 두고 하위 문법만 변환한다.

## 비동기 이터레이터, 비동기 제너레이터, for-await-of

> **원문:** https://www.typescriptlang.org/docs/handbook/release-notes/typescript-2-3.html

TypeScript 2.3에서는 TC39의 비동기 반복(async iteration) 제안을 타입으로 지원하기 시작했다.

### AsyncIterator

- 일반 `Iterator`와 모양은 같지만 `next`, `return`, `throw`가 결과 값을 `Promise`로 감싸서 반환한다.

```ts
interface AsyncIterator<T> {
  next(value?: any): Promise<IteratorResult<T>>;
  return?(value?: any): Promise<IteratorResult<T>>;
  throw?(e?: any): Promise<IteratorResult<T>>;
}
```

- 어떤 객체가 비동기 반복을 지원한다는 것은 `Symbol.asyncIterator` 메서드를 구현해서 위 `AsyncIterator`를 반환한다는 뜻이다.

### 비동기 제너레이터

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

### for-await-of

- 비동기 이터러블을 순회하기 위한 문법으로, `async` 함수나 비동기 제너레이터 내부에서만 쓸 수 있다.

```ts
async function main() {
  for await (const n of countSlowly()) {
    console.log(n);
  }
}
```

### 실행 환경 요구 사항

- 런타임에 `Symbol.asyncIterator`가 없으면 직접 채워 넣어야 한다.

  ```ts
  (Symbol as any).asyncIterator =
    Symbol.asyncIterator || Symbol.for("Symbol.asyncIterator");
  ```

- `AsyncIterator` 등의 타입 선언을 쓰려면 `lib`에 `esnext`를 포함해야 하고, ES5/ES3 타겟에서는 `--downlevelIterators` 플래그도 함께 켜야 한다.

## Awaited\<T\>: Promise 중첩을 풀어내는 타입

> **원문:** https://www.typescriptlang.org/docs/handbook/release-notes/typescript-4-5.html

### 왜 필요했나

`await`나 `Promise.prototype.then`은 값이 `Promise`이면 그 안의 값을 꺼내고, `Promise`가 아니면 값을 그대로 반환한다. 그런데 `PromiseLike<T>`처럼 프로미스와 유사하지만 `Promise`는 아닌 값, 혹은 프로미스 안에 프로미스가 중첩된 경우(`Promise<Promise<T>>`)까지 고려하면 이 "풀어내기" 동작을 타입 수준에서 정확히 표현하기가 까다로웠다. 특히 `Promise.all`처럼 여러 값을 한꺼번에 다루는 API에서 타입 추론이 어긋나는 문제가 자주 발생했다.

### 동작 방식

`Awaited<T>`는 `T`가 프로미스면 그 내부 타입을 꺼내고, 내부 타입이 또 프로미스면 재귀적으로 계속 풀어낸다. 프로미스가 아닌 값이 섞여 있으면 그 값은 그대로 유지된다.

```ts
type A = Awaited<Promise<string>>; // string
type B = Awaited<Promise<Promise<number>>>; // number (중첩도 재귀적으로 해제)
type C = Awaited<boolean | Promise<number>>; // boolean | number
```

### Promise.all이 좋아진 예

TypeScript 4.5 이전에는 값이 `T | Promise<T>`처럼 프로미스일 수도 아닐 수도 있는 형태이면 `Promise.all` 결과 타입이 원래 의도와 어긋나는 경우가 있었다.

```ts
declare function maybeAsync<T>(v: T): T | Promise<T>;

async function run(): Promise<[number, number]> {
  const [a, b] = await Promise.all([maybeAsync(1), maybeAsync(2)]);
  return [a, b]; // 4.5 이전에는 타입 불일치로 에러가 나던 패턴
}
```

`lib.es5.d.ts`의 `Promise.all`, `Promise.race` 등의 시그니처가 `Awaited`를 사용하도록 다시 작성되면서, 이런 코드에서도 결과 타입이 기대한 대로 추론된다.

### 정리

| 시점 | 발전 내용 |
| --- | --- |
| TS 2.1 | ES3/ES5에서도 `async`/`await` 사용 가능(다운레벨 컴파일 + `Promise` 타입/폴리필 필요) |
| TS 2.3 | `AsyncIterator`, 비동기 제너레이터, `for await...of` 타입 지원 |
| TS 4.5 | `Awaited<T>`로 중첩된 프로미스 타입을 재귀적으로 풀어내고, `Promise.all`류 API의 추론 정확도 개선 |

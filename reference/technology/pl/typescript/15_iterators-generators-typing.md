# 이터레이터·제너레이터 타입 심화와 다운레벨 반복

## 2.3: ES5/ES3에서의 제너레이터·이터레이터 지원 — `downlevelIteration`

`for...of`, 배열 구조 분해, 스프레드 연산자는 원래 ES6 이상 타깃에서만 `Symbol.iterator` 프로토콜을 온전히 활용했다. `--downlevelIteration` 플래그를 켜면 ES5/ES3로 컴파일할 때도 다음이 가능해진다.

- 순회 대상 객체에 `[Symbol.iterator]()`가 있으면 그것을 호출해서 순회
- `Symbol.iterator`가 없는 유사 배열 객체는 합성 배열 이터레이터로 대체 순회

단, 이 옵션은 **런타임에 네이티브 혹은 폴리필된 `Symbol.iterator`가 존재해야만** 의미가 있다. 런타임 지원이 없으면 컴파일러는 그냥 인덱스 기반 순회로 되돌아가며 플래그 자체는 아무 이득도 주지 못한다.

> **원문:** https://www.typescriptlang.org/docs/handbook/release-notes/typescript-2-3.html

## 2.3: 비동기 반복 — `AsyncIterator`, 비동기 제너레이터, `for await...of`

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

## 3.6: 더 엄격해진 제너레이터 타입 — yield·return·next를 구분하기

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

## `downlevelIteration` 컴파일러 옵션 — 무엇을, 왜 다르게 내보내는가

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

## 요약

| 시점 | 핵심 내용 |
| --- | --- |
| 2.3 | `--downlevelIteration`으로 ES5/ES3에서도 이터레이터 프로토콜 지원, `AsyncIterator`·`async function*`·`for await...of`로 비동기 반복 도입 |
| 3.6 | `Iterator`/`Generator`/`IteratorResult`에 `<T, TReturn, TNext>` 세 매개변수를 도입해 yield/return/next 값을 각각 정확히 타이핑 |
| tsconfig | `downlevelIteration`은 어디까지나 트랜스파일 방식의 변경이며, 런타임 `Symbol.iterator` 지원이 전제조건 |

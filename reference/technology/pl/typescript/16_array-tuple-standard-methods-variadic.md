# 배열·튜플 표준 메서드 타입과 가변 튜플

## 배열 타입의 두 가지 표기법

- `Array<T>`와 `T[]`는 완전히 같은 타입을 가리키는 두 표기법이다. 실무에서는 짧은 `T[]`를 훨씬 자주 쓴다.
- 배열 타입은 제네릭이므로 원소 타입만 바뀌면 `push`, `slice`, `map` 같은 표준 메서드의 시그니처도 함께 그 타입으로 치환된다.

```ts
function sum(values: number[]): number {
  return values.reduce((acc, v) => acc + v, 0);
}

// 아래와 완전히 동일한 의미
function sum2(values: Array<number>): number {
  return values.reduce((acc, v) => acc + v, 0);
}
```

> **원문:** https://www.typescriptlang.org/docs/handbook/2/objects.html

## readonly 배열: ReadonlyArray와 readonly T[]

- `ReadonlyArray<T>`는 배열을 변경하는 메서드(`push`, `pop`, `splice`, 인덱스 대입 등)를 타입 시스템에서 제거한 배열 타입이다. `slice`처럼 새 배열을 반환하는 읽기 전용 메서드는 그대로 남는다.
- `readonly T[]`는 `ReadonlyArray<T>`의 축약 표기이며, 3.4 이전에는 이 짧은 문법이 없어서 항상 제네릭 형태로 써야 했다.
- 일반 배열은 `readonly` 배열에 대입할 수 있지만, 반대로 `readonly` 배열을 일반 배열 자리에 넣을 수는 없다. "읽기 전용"이라는 제약이 더 강한 쪽에서 약한 쪽으로만 자연스럽게 흘러가기 때문이다.

```ts
function printAll(items: readonly string[]) {
  items.slice(); // OK, 새 배열을 반환할 뿐 원본은 그대로
  items.push("x"); // 오류: readonly 배열에는 push가 없음
}

let ro: readonly number[] = [1, 2, 3];
let mut: number[] = [1, 2, 3];
ro = mut; // OK: 일반 -> readonly
mut = ro; // 오류: readonly -> 일반은 불가
```

> **원문:** https://www.typescriptlang.org/docs/handbook/release-notes/typescript-3-4.html, https://www.typescriptlang.org/docs/handbook/2/objects.html

## const 단언과 readonly 튜플 추론

- 값 뒤에 `as const`를 붙이면 리터럴 타입이 넓혀지지(widening) 않고, 배열 리터럴은 `readonly` 튜플로, 객체 리터럴은 각 속성이 `readonly`인 타입으로 추론된다.
- 좌표나 설정값처럼 "이 값은 이후에도 바뀌지 않는다"는 것을 타입으로 못 박고 싶을 때 유용하다.

```ts
let point = [3, 4] as const;
// point의 타입: readonly [3, 4]

function distance([x, y]: [number, number]): number {
  return Math.sqrt(x * x + y * y);
}

distance(point); // 오류: readonly [3, 4] 는 [number, number]에 대입 불가
```

> **원문:** https://www.typescriptlang.org/docs/handbook/release-notes/typescript-3-4.html

## 튜플 타입: 길이와 위치가 고정된 배열

- 튜플은 원소 개수와 각 위치의 타입이 고정된 배열이다. 인덱스별로 다른 타입을 정확히 표현할 수 있어 `[string, number]`처럼 쓰면 0번은 문자열, 1번은 숫자로 강제된다.
- 고정된 길이를 벗어난 인덱스에 접근하면 컴파일 타임 오류가 난다.

```ts
type NameAge = [name: string, age: number];

function describe(person: NameAge): string {
  return `${person[0]} (${person[1]})`;
}

describe(["Alice", 30]); // OK
```

> **원문:** https://www.typescriptlang.org/docs/handbook/2/objects.html

## 선택적 원소와 length의 유니온화

- 튜플 끝쪽 원소에 `?`를 붙이면 선택적 원소가 되며, 이때 `length`의 타입도 가능한 길이들의 유니온으로 계산된다.
- 선택적 원소는 반드시 필수 원소 뒤, 그리고 나머지 선택적 원소들의 앞쪽에 와야 한다.

```ts
type Point = [x: number, y: number, z?: number];

function coordCount(p: Point): void {
  console.log(p.length); // 타입: 2 | 3
}
```

> **원문:** https://www.typescriptlang.org/docs/handbook/2/objects.html

## 튜플의 rest 원소

- 튜플에도 `...T[]` 형태의 rest 원소를 넣어 가변 길이 구간을 표현할 수 있다. rest 구간이 있으면 그 튜플은 더 이상 고정 길이가 아니고, `length`도 특정 숫자로 좁혀지지 않는다.
- 4.0 이전에는 rest 원소가 튜플의 맨 끝에만 올 수 있었지만, 이후로는 중간이나 앞쪽에도 놓을 수 있게 되었다.

```ts
type LogEntry = [level: string, ...args: unknown[]];

const e1: LogEntry = ["info"];
const e2: LogEntry = ["error", "실패", 404];
```

> **원문:** https://www.typescriptlang.org/docs/handbook/2/objects.html, https://www.typescriptlang.org/docs/handbook/release-notes/typescript-4-0.html

## readonly 튜플

- 배열과 마찬가지로 튜플 앞에도 `readonly`를 붙여 원소 재할당을 막을 수 있다. `readonly [string, number]`처럼 쓰면 특정 인덱스에 값을 대입하는 순간 오류가 난다.
- 매핑된 타입에 `readonly` 수식자를 적용하면 배열·튜플 타입도 올바르게 readonly 버전으로 바뀐다. 즉 `Readonly<[string, boolean]>`은 `readonly [string, boolean]`이 된다.

```ts
function freeze(pair: readonly [string, number]) {
  pair[0] = "changed"; // 오류: readonly 튜플의 원소는 재할당 불가
}

type Frozen = Readonly<[string, boolean]>;
// readonly [string, boolean]
```

> **원문:** https://www.typescriptlang.org/docs/handbook/release-notes/typescript-3-4.html

## 가변 튜플 타입 (Variadic Tuple Types)

TypeScript 4.0 이전에는 배열/튜플을 다루는 함수(`concat`, `tail`, 커링 등)를 정확히 타이핑하려면 인자 개수별로 오버로드를 끝없이 나열해야 했다. 가변 튜플은 튜플의 스프레드 부분을 제네릭으로 남겨두고 나중에 실제 타입으로 치환할 수 있게 해서 이 문제를 해결한다.

- 튜플 타입 안의 `...T` 자리에 제네릭 타입 매개변수를 그대로 쓸 수 있다. 실제 호출 시 `T`가 구체적인 튜플로 추론되면서 나머지 원소들의 타입이 함께 결정된다.
- 스프레드는 튜플 중간에도 놓을 수 있어서, 길이가 확정된 여러 튜플을 이어 붙인 결과 타입도 표현할 수 있다.
- 스프레드 대상이 길이를 알 수 없는 일반 배열(`T[]`)이면, 그 뒤로 이어지는 결과 타입도 길이가 고정되지 않은 튜플이 된다.

```ts
type Tail<T extends readonly unknown[]> =
  T extends readonly [unknown, ...infer Rest] ? Rest : never;

type T1 = Tail<[1, 2, 3]>; // [2, 3]

function concat<T extends readonly unknown[], U extends readonly unknown[]>(
  a: T,
  b: U
): [...T, ...U] {
  return [...a, ...b];
}

const merged = concat([1, 2] as const, ["a", "b"] as const);
// 타입: [1, 2, "a", "b"]
```

이 기능 덕분에 부분 적용(partial application)처럼 앞부분 인자와 뒷부분 인자의 타입을 각각 튜플로 캡처했다가 다시 합치는 고급 패턴도 오버로드 없이 하나의 시그니처로 표현할 수 있다.

> **원문:** https://www.typescriptlang.org/docs/handbook/release-notes/typescript-4-0.html

## 레이블이 있는 튜플 원소 (Labeled Tuple Elements)

- 4.0부터는 튜플의 각 위치에 이름을 붙일 수 있다. `[start: number, end: number]`처럼 쓰면 함수 매개변수 목록과 비슷한 형태가 되어 가독성과 에디터의 시그니처 도움말이 좋아진다.
- 레이블은 순수하게 문서화 목적이며, 구조 분해 할당 시 실제 변수 이름과는 무관하다.
- 한 원소에 레이블을 붙이면 같은 튜플의 나머지 원소도 모두 레이블을 붙여야 한다.

```ts
type Range = [start: number, end: number];

function inRange([start, end]: Range, value: number): boolean {
  return value >= start && value <= end;
}
```

> **원문:** https://www.typescriptlang.org/docs/handbook/release-notes/typescript-4-0.html

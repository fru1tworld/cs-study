# TypeScript 배열·튜플, 문자열·숫자, JSON·정규식, DOM 타입

## 배열·튜플 표준 메서드 타입과 가변 튜플

### 배열 타입의 두 가지 표기법

- `Array<T>`와 `T[]`는 완전히 같은 타입을 가리키는 두 표기법
  - 실무에서는 짧은 `T[]`를 훨씬 자주 사용
- 배열 타입은 제네릭 → 원소 타입만 바뀌면 `push`·`slice`·`map` 같은 표준 메서드의 시그니처도 함께 그 타입으로 치환됨

```ts
function sum(values: number[]): number {
  return values.reduce((acc, v) => acc + v, 0);
}

// 아래와 완전히 동일한 의미
function sum2(values: Array<number>): number {
  return values.reduce((acc, v) => acc + v, 0);
}
```

> 원문: https://www.typescriptlang.org/docs/handbook/2/objects.html

### readonly 배열: ReadonlyArray와 readonly T[]

- `ReadonlyArray<T>`: 배열을 변경하는 메서드(`push`, `pop`, `splice`, 인덱스 대입 등)를 타입 시스템에서 제거한 배열 타입
  - `slice`처럼 새 배열을 반환하는 읽기 전용 메서드는 그대로 남음
- `readonly T[]`: `ReadonlyArray<T>`의 축약 표기
  - 3.4 이전에는 이 짧은 문법이 없어 항상 제네릭 형태로 써야 함
- 일반 배열은 `readonly` 배열에 대입 가능, 반대로 `readonly` 배열을 일반 배열 자리에 넣는 것은 불가
  - "읽기 전용"이라는 제약이 더 강한 쪽에서 약한 쪽으로만 자연스럽게 흘러감 → 역방향은 금지

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

> 원문: https://www.typescriptlang.org/docs/handbook/release-notes/typescript-3-4.html, https://www.typescriptlang.org/docs/handbook/2/objects.html

### const 단언과 readonly 튜플 추론

- 값 뒤에 `as const`를 붙이면 리터럴 타입이 넓혀지지(widening) 않음
  - 배열 리터럴 → `readonly` 튜플로 추론
  - 객체 리터럴 → 각 속성이 `readonly`인 타입으로 추론
- 좌표·설정값처럼 "이 값은 이후에도 바뀌지 않는다"를 타입으로 못 박고 싶을 때 유용

```ts
let point = [3, 4] as const;
// point의 타입: readonly [3, 4]

function distance([x, y]: [number, number]): number {
  return Math.sqrt(x * x + y * y);
}

distance(point); // 오류: readonly [3, 4] 는 [number, number]에 대입 불가
```

> 원문: https://www.typescriptlang.org/docs/handbook/release-notes/typescript-3-4.html

### 튜플 타입: 길이와 위치가 고정된 배열

- 튜플: 원소 개수와 각 위치의 타입이 고정된 배열
  - 인덱스별로 다른 타입을 정확히 표현 가능 → `[string, number]`처럼 쓰면 0번은 문자열, 1번은 숫자로 강제
- 고정된 길이를 벗어난 인덱스 접근 시 컴파일 타임 오류

```ts
type NameAge = [name: string, age: number];

function describe(person: NameAge): string {
  return `${person[0]} (${person[1]})`;
}

describe(["Alice", 30]); // OK
```

> 원문: https://www.typescriptlang.org/docs/handbook/2/objects.html

### 선택적 원소와 length의 유니온화

- 튜플 끝쪽 원소에 `?`를 붙이면 선택적 원소가 됨 → 이때 `length`의 타입도 가능한 길이들의 유니온으로 계산됨
- 선택적 원소는 반드시 필수 원소 뒤, 나머지 선택적 원소들의 앞쪽에 위치

```ts
type Point = [x: number, y: number, z?: number];

function coordCount(p: Point): void {
  console.log(p.length); // 타입: 2 | 3
}
```

> 원문: https://www.typescriptlang.org/docs/handbook/2/objects.html

### 튜플의 rest 원소

- 튜플에도 `...T[]` 형태의 rest 원소를 넣어 가변 길이 구간을 표현 가능
  - rest 구간이 있으면 그 튜플은 더 이상 고정 길이가 아니고, `length`도 특정 숫자로 좁혀지지 않음
- 4.0 이전에는 rest 원소가 튜플의 맨 끝에만 가능 → 이후로는 중간·앞쪽에도 배치 가능

```ts
type LogEntry = [level: string, ...args: unknown[]];

const e1: LogEntry = ["info"];
const e2: LogEntry = ["error", "실패", 404];
```

> 원문: https://www.typescriptlang.org/docs/handbook/2/objects.html, https://www.typescriptlang.org/docs/handbook/release-notes/typescript-4-0.html

### readonly 튜플

- 배열과 마찬가지로 튜플 앞에도 `readonly`를 붙여 원소 재할당을 막을 수 있음
  - `readonly [string, number]`처럼 쓰면 특정 인덱스에 값을 대입하는 순간 오류
- 매핑된 타입에 `readonly` 수식자를 적용하면 배열·튜플 타입도 올바르게 readonly 버전으로 변환됨
  - 즉 `Readonly<[string, boolean]>`은 `readonly [string, boolean]`이 됨

```ts
function freeze(pair: readonly [string, number]) {
  pair[0] = "changed"; // 오류: readonly 튜플의 원소는 재할당 불가
}

type Frozen = Readonly<[string, boolean]>;
// readonly [string, boolean]
```

> 원문: https://www.typescriptlang.org/docs/handbook/release-notes/typescript-3-4.html

### 가변 튜플 타입 (Variadic Tuple Types)

- TypeScript 4.0 이전: 배열/튜플을 다루는 함수(`concat`, `tail`, 커링 등)를 정확히 타이핑하려면 인자 개수별로 오버로드를 끝없이 나열해야 함
- 가변 튜플: 튜플의 스프레드 부분을 제네릭으로 남겨두고 나중에 실제 타입으로 치환 가능 → 이 문제를 해결

- 튜플 타입 안의 `...T` 자리에 제네릭 타입 매개변수를 그대로 사용 가능
  - 실제 호출 시 `T`가 구체적인 튜플로 추론되면서 나머지 원소들의 타입이 함께 결정됨
- 스프레드는 튜플 중간에도 배치 가능 → 길이가 확정된 여러 튜플을 이어 붙인 결과 타입도 표현 가능
- 스프레드 대상이 길이를 알 수 없는 일반 배열(`T[]`)이면, 그 뒤로 이어지는 결과 타입도 길이가 고정되지 않은 튜플이 됨

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

- 이 기능 덕분에 부분 적용(partial application)처럼 앞부분 인자와 뒷부분 인자의 타입을 각각 튜플로 캡처했다가 다시 합치는 고급 패턴도 오버로드 없이 하나의 시그니처로 표현 가능

> 원문: https://www.typescriptlang.org/docs/handbook/release-notes/typescript-4-0.html

### 레이블이 있는 튜플 원소 (Labeled Tuple Elements)

- 4.0부터 튜플의 각 위치에 이름을 붙일 수 있음
  - `[start: number, end: number]`처럼 쓰면 함수 매개변수 목록과 비슷한 형태 → 가독성과 에디터의 시그니처 도움말 향상
- 레이블은 순수하게 문서화 목적 → 구조 분해 할당 시 실제 변수 이름과는 무관
- 한 원소에 레이블을 붙이면 같은 튜플의 나머지 원소도 모두 레이블 필요

```ts
type Range = [start: number, end: number];

function inRange([start, end]: Range, value: number): boolean {
  return value >= start && value <= end;
}
```

> 원문: https://www.typescriptlang.org/docs/handbook/release-notes/typescript-4-0.html

---

## 문자열·숫자·심볼 래퍼 타입과 템플릿 리터럴 타입 심화

### 원시 타입과 래퍼 객체 타입은 다름

> 원문: https://www.typescriptlang.org/docs/handbook/2/everyday-types.html

- JS의 `string`, `number`, `boolean`은 원시값, `String`, `Number`, `Boolean`은 이를 감싸는 내장 객체(래퍼) 생성자
- TypeScript는 소문자로 시작하는 `string` / `number` / `boolean`을 타입으로 쓰길 강제하다시피 권장
  - 대문자 버전(`String`, `Number`, `Boolean`)도 문법상 사용 가능하지만 실무 코드에서 쓸 일은 거의 없음
- 이유: `typeof` 연산자가 실제로 반환하는 값이 `"string"`, `"number"`, `"boolean"` → 소문자 타입이 JS 런타임의 실체와 정확히 대응
- `symbol`, `bigint`도 마찬가지로 소문자만 존재 → 이 둘은 애초에 대문자 래퍼 타입을 타입 표기 위치에 쓰는 일이 자연스럽지 않음

```ts
// 권장
let title: string = "타입스크립트";
let count: number = 3;
let done: boolean = false;

// 비권장 (동작은 하지만 의미상 다른 타입)
let title2: String = "타입스크립트";
```

- 여기서 `String`은 "문자열처럼 보이는 값"이 아니라 "`new String(...)`으로 만들어질 수 있는 객체"를 뜻하는 타입 → 원시 문자열을 담는 용도로는 부적합

### JS 런타임에서 원시값과 래퍼 객체의 실제 차이

> 원문: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String

- `String(1)`처럼 함수로 호출하면 원시 문자열을 반환, `new String(1)`처럼 생성자로 호출하면 `String` 객체를 반환
  - 겉보기엔 비슷해도 `typeof`로 구분하면 전자는 `"string"`, 후자는 `"object"`

```js
typeof String(1);      // "string"
typeof new String(1);  // "object"
```

- 문자열 원시값에 `.length`나 `.toUpperCase()` 같은 메서드를 호출하면, JS 엔진이 순간적으로 원시값을 `String` 객체로 감쌌다가(오토박싱) 메서드 실행 후 다시 버림
  - 이 과정 덕분에 원시값도 마치 객체처럼 메서드 사용 가능
- 원시값과 래퍼 객체를 `===`로 비교하면 타입 자체가 다르므로 항상 `false`
  - `==`는 타입 강제 변환이 일어나 `true`가 됨

```js
const a = "hi";
const b = new String("hi");

a === b; // false, string vs object
a == b;  // true, 값만 비교하도록 강제 변환됨
```

- 이런 함정 → `new String(...)`으로 문자열을 만드는 것은 실무에서 지양
  - TypeScript가 `String` 타입 사용을 말리는 이유도 결국 이 런타임 동작과 맞닿아 있음
  - `string` 타입은 "원시 문자열"만 가리키므로, `new String(...)`이 만든 객체를 `string` 타입 변수에 넣으면 타입 에러

### 템플릿 리터럴 타입: 문자열 리터럴의 조합

> 원문: https://www.typescriptlang.org/docs/handbook/release-notes/typescript-4-1.html

- TypeScript 4.1부터 JS의 템플릿 리터럴 문법을 타입 위치에서 그대로 사용 가능
  - 문자열 리터럴 타입들을 이어 붙여 새 리터럴 타입을 만드는 방식

```ts
type Lang = "ko" | "en";
type Greeting = `hello-${Lang}`;
// "hello-ko" | "hello-en"
```

- 유니온 타입을 템플릿 안에 넣으면 가능한 모든 조합이 자동으로 전개됨

```ts
type Size = "sm" | "md" | "lg";
type Color = "red" | "blue";

type ButtonClass = `btn-${Size}-${Color}`;
// "btn-sm-red" | "btn-sm-blue" | "btn-md-red" | "btn-md-blue"
// | "btn-lg-red" | "btn-lg-blue"
```

- 이 성질을 이용하면 오타·잘못된 조합을 컴파일 타임에 검출 가능

```ts
declare function setPosition(pos: `${"top" | "bottom"}-${"left" | "right"}`): void;

setPosition("top-left");   // OK
setPosition("top-lfet");   // 에러: 오타
```

### 문자열 대소문자 변환 유틸리티 타입

> 원문: https://www.typescriptlang.org/docs/handbook/release-notes/typescript-4-1.html

- 템플릿 리터럴 타입과 함께 문자열 리터럴의 대소문자를 변환하는 유틸리티 타입 4종 추가
  - 모두 컴파일 타임에만 존재하며 런타임 함수가 아님
- 유틸리티 타입별 동작
  - `Uppercase<T>`: 전부 대문자로
  - `Lowercase<T>`: 전부 소문자로
  - `Capitalize<T>`: 첫 글자만 대문자로
  - `Uncapitalize<T>`: 첫 글자만 소문자로

```ts
type A = Uppercase<"hi">;      // "HI"
type B = Lowercase<"HI">;      // "hi"
type C = Capitalize<"hi">;     // "Hi"
type D = Uncapitalize<"Hi">;   // "hi"
```

### 매핑된 타입의 키 재매핑(`as` 절)과 조합

> 원문: https://www.typescriptlang.org/docs/handbook/release-notes/typescript-4-1.html

- 기존 매핑된 타입은 원본 타입의 키를 그대로 쓸 수밖에 없었음
- 4.1부터 `as` 절로 키 자체를 새 문자열로 바꿔치기 가능
  - 이를 템플릿 리터럴 타입·`Capitalize` 같은 유틸리티와 엮으면 접근자(getter) 타입을 자동 생성하는 패턴 가능

```ts
interface Book {
  title: string;
  pages: number;
}

type Getters<T> = {
  [K in keyof T as `get${Capitalize<string & K>}`]: () => T[K];
};

type BookGetters = Getters<Book>;
// { getTitle: () => string; getPages: () => number }
```

- `as` 절에서 결과를 `never`로 매핑하면 해당 키를 결과 타입에서 완전히 제외 가능 → 특정 필드를 걸러내는 유틸리티 타입도 직접 제작 가능

```ts
type OmitKind<T> = {
  [K in keyof T as Exclude<K, "kind">]: T[K];
};
```

### 템플릿 리터럴 패턴으로부터 타입 추론하기

> 원문: https://www.typescriptlang.org/docs/handbook/release-notes/typescript-4-1.html

- 템플릿 리터럴 타입은 값을 만들어내는 용도뿐 아니라, 함수 매개변수 위치에서 문자열 패턴을 분석해 관련 타입을 역으로 추론하는 데도 활용
  - 이벤트 이름 문자열에서 원본 속성 키를 뽑아내고, 그 키에 대응하는 값 타입을 콜백 인자 타입으로 자동 연결하는 패턴이 대표적

```ts
type Watcher<T> = {
  on<K extends string & keyof T>(
    event: `${K}Changed`,
    handler: (value: T[K]) => void
  ): void;
};

declare function watch<T>(obj: T): Watcher<T>;

const w = watch({ age: 10, name: "hi" });

w.on("ageChanged", (v) => v.toFixed());   // v: number
w.on("nameChanged", (v) => v.toUpperCase()); // v: string
```

- `event` 문자열이 `"ageChanged"`로 들어오면 컴파일러가 템플릿 패턴 `${K}Changed`를 역산해 `K`를 `"age"`로 추론 → 콜백의 `value` 매개변수 타입까지 `T["age"]`, 즉 `number`로 자동 결정
  - 문자열 하나로 여러 오버로드를 대체할 수 있다는 것이 이 기능의 핵심 가치

---

## JSON과 정규식 타입

- TypeScript는 JS 내장 객체인 `JSON`과 `RegExp`에도 타입을 붙여야 함
  - 문제는 두 객체 모두 런타임에 값의 "모양"이 고정되어 있지 않다는 점
  - `JSON.parse`는 문자열만 보고 아무 값이나 만들어낼 수 있고, 정규식의 캡처 그룹 개수와 이름은 패턴 문자열 안에 들어 있어 컴파일러가 미리 알 방법이 없음
- 이 문서는 이 둘을 TypeScript가 어떻게 타입으로 다루는지, TS 4.1의 템플릿 리터럴 타입이 이 한계를 어떻게 완화하는지 정리

### JSON 타입: `any`로 열려 있는 이유

> 원문: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/JSON

- `lib.es5.d.ts`에 정의된 시그니처는 대략 다음과 같음

```ts
interface JSON {
  parse(text: string, reviver?: (key: string, value: any) => any): any;
  stringify(value: any, replacer?: ..., space?: string | number): string;
}
```

- `JSON.parse`의 반환 타입은 `any`
  - JSON 문자열 안에 무엇이 들어 있는지는 실행 시점에만 알 수 있음 → 컴파일러가 미리 구조를 검증해줄 수 없음
- 따라서 파싱 결과를 곧바로 어떤 인터페이스에 대입해 쓰는 코드는 타입 체크를 통과하더라도 실제 값이 그 모양이라는 보장이 없음
  - 검증이 필요하면 별도의 스키마 검사(zod, io-ts 등)나 사용자 정의 타입 가드를 거쳐야 함

```ts
interface User {
  id: number;
  name: string;
}

function parseUser(text: string): User {
  const value = JSON.parse(text); // any
  if (typeof value.id !== "number" || typeof value.name !== "string") {
    throw new Error("invalid user json");
  }
  return value; // 검증을 통과했으므로 User로 취급
}
```

- `JSON.stringify`도 `value`를 `any`로 받음
  - `undefined`, 함수, `Symbol`은 JSON으로 직렬화될 수 없는 값이라 객체 속성에서는 통째로 생략되고, 배열 원소로 들어가면 `null`로 바뀜
  - 이 규칙은 타입 시스템이 아니라 런타임 동작 → 타입만 보고는 어떤 속성이 사라질지 알 수 없음을 기억해야 함

```ts
const payload = { name: "a", onClick: () => {}, tag: undefined };
JSON.stringify(payload); // '{"name":"a"}' — onClick, tag 모두 사라짐
```

- `reviver`(파싱 시)와 `replacer`(직렬화 시) 콜백도 타입 상으로는 `(key, value) => any` 수준으로만 제약
  - 콜백 내부에서 어떤 타입이 오는지는 개발자가 직접 좁혀야 함

### 정규식 타입: `RegExp`, `RegExpMatchArray`, `RegExpExecArray`

> 원문: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp

- `RegExp`는 리터럴(`/ab+c/i`)이나 생성자(`new RegExp("ab+c", "i")`)로 생성 가능, 다음과 같은 읽기 전용 속성 보유
  - `source`: 플래그를 뺀 패턴 문자열
  - `flags`: 적용된 플래그 전체 문자열
  - `global` / `ignoreCase` / `multiline` / `sticky` / `unicode` / `dotAll` / `hasIndices`: 각 플래그(`g i m y u s d`)의 개별 boolean
  - `lastIndex`: `g`, `y` 플래그가 있을 때 다음 검색 시작 위치(읽기/쓰기 가능)
- 이 속성들은 패턴이 컴파일 시점에 문자열로만 존재 → 리터럴로 작성해도 `flags`나 `global` 같은 값이 리터럴 타입으로 좁혀지지 않고 `string`, `boolean`으로만 잡힘

```ts
const re = /\d+/g;
re.global; // 타입은 boolean (실제 값은 true지만 리터럴로 좁혀지지 않음)
```

- `test`는 `boolean`을 반환 → 타입 추론에 특별한 문제 없음
- `exec`와 문자열의 `match`는 매치 결과를 배열 형태로 반환 → 이 결과 타입이 `RegExpExecArray` / `RegExpMatchArray`

### `exec()`의 반환 타입: 인덱스 시그니처 배열 + 부가 속성

> 원문: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp/exec

- `exec()`는 매치가 없으면 `null`, 있으면 배열 하나를 반환
  - 이 배열은 `string[]`이 아니라 다음 필드를 덧붙인 특수한 형태
  - `[0]`: 전체 매치 문자열, `[1], [2], ...`: 캡처 그룹
  - `index`: 매치가 시작된 위치
  - `input`: 검색 대상이 된 원본 문자열
  - `groups`: 이름 붙은 캡처 그룹 객체(없으면 `undefined`)
- TypeScript는 이를 `interface RegExpExecArray extends Array<string> { index: number; input: string; groups?: { [key: string]: string } }`로 정의
  - 중요한 지점: `groups`의 타입이 패턴과 무관하게 항상 `{ [key: string]: string } | undefined`
  - 즉 `(?<year>\d+)-(?<month>\d+)` 같은 패턴을 써도 TypeScript는 `groups.year`가 존재한다는 것을 알지 못하고, 인덱스 시그니처를 통한 `string | undefined` 접근만 허용

```ts
const re = /(?<year>\d{4})-(?<month>\d{2})/;
const m = re.exec("2024-05");
if (m?.groups) {
  const year = m.groups.year; // string (컴파일러 입장에서는 인덱스 시그니처 값)
}
```

- `g` 또는 `y` 플래그가 있으면 `exec`는 호출할 때마다 `lastIndex`를 이어서 진행하는 상태 저장(stateful) 메서드가 됨
  - 매치가 더 없으면 `null`을 반환하며 `lastIndex`가 `0`으로 리셋
  - 이 상태 전이는 타입에 드러나지 않음 → `while ((m = re.exec(str)))` 같은 반복문에서 무한 루프에 빠지지 않도록 `g` 플래그 사용은 직접 관리해야 함

### TypeScript 4.1과의 접점: 템플릿 리터럴 타입으로 문자열 구조를 타입화하기

> 원문: https://www.typescriptlang.org/docs/handbook/release-notes/typescript-4-1.html

- 앞서 본 것처럼 `JSON.parse`의 반환 타입과 정규식 캡처 그룹의 `groups` 타입은 둘 다 "문자열 안에 있는 구조를 컴파일 타임에 알 수 없다"는 같은 한계를 공유
- TypeScript 4.1에서 도입된 템플릿 리터럴 타입(template literal types)은 이 한계를 부분적으로 메움
  - 문자열 리터럴 타입을 문자열 그대로가 아니라 하나의 패턴처럼 조합 가능 → 정규식이 하던 일의 일부(문자열 형태 검증)를 타입 레벨에서 흉내 낼 수 있음

```ts
type EventName<T extends string> = `on${Capitalize<T>}`;
type ClickEvent = EventName<"click">; // "onClick"

// 정규식으로 "YYYY-MM-DD" 형태를 검사하는 대신, 형태 자체를 타입으로 표현
type ISODate = `${number}-${number}-${number}`;
const d: ISODate = "2024-05-01"; // OK
```

- 물론 템플릿 리터럴 타입은 실제 정규식처럼 자릿수·값의 범위를 세밀하게 제약하지는 못함(`${number}`는 자릿수 제한이 없음)
- 그래도 `RegExpExecArray.groups`가 항상 `string` 인덱스 시그니처로만 잡히는 것과 달리, 문자열의 정적인 형태를 타입으로 표현하고 싶을 때는 정규식 대신 템플릿 리터럴 타입을 쓰는 편이 컴파일 타임 검증을 받을 수 있어 유리
- 반대로 `JSON.parse`처럼 아예 런타임 입력에 의존하는 값은 템플릿 리터럴 타입으로도 좁힐 수 없음 → 여전히 별도의 런타임 검증 필요

### 정리

- `JSON.parse`/`JSON.stringify`는 값의 실제 구조를 알 수 없으므로 각각 `any` 입력·출력으로 열려 있음 → 타입 안전성은 스키마 검증이나 타입 가드로 직접 확보해야 함
- `RegExp`의 인스턴스 속성(`flags`, `global` 등)은 패턴 문자열을 분석하지 않으므로 일반적인 `string`/`boolean`으로만 타입이 잡힘
- `exec()`/`match()`가 반환하는 `RegExpExecArray`/`RegExpMatchArray`는 배열에 `index`, `input`, `groups`를 얹은 구조, `groups`는 패턴과 무관하게 `{ [key: string]: string } | undefined`로 고정
- TS 4.1의 템플릿 리터럴 타입은 문자열의 정적인 형태를 타입 레벨에서 표현할 수 있게 해주지만, `JSON.parse`처럼 순수한 런타임 값까지 좁혀주지는 못함

---

## DOM 라이브러리 기초와 Event/EventTarget 타입

- TypeScript는 브라우저 DOM API를 직접 설계하지 않음
- 대신 `lib.dom.d.ts`라는 하나의 거대한 선언 파일에 `document`, `window`, `HTMLElement`, `Event` 같은 전역 타입을 미리 정의해두고, `tsconfig.json`의 `lib` 옵션으로 이 정의를 프로젝트에 포함시킬지 결정
- 이 문서는 DOM 타입이 어디서 오는지, `lib` 옵션이 무엇을 바꾸는지, 이벤트 시스템의 핵심인 `EventTarget`/`Event`가 어떻게 타입으로 표현되는지를 정리

### lib.dom.d.ts: DOM 타입은 어디서 오는가

> 원문: https://www.typescriptlang.org/docs/handbook/dom-manipulation.html

- TypeScript 배포판에는 2만 줄이 넘는 `lib.dom.d.ts`가 포함
  - 기본 설정의 프로젝트라면 별도 설치 없이 `document`, `HTMLElement` 등을 바로 사용 가능
- 이 파일의 타입 정의는 MDN 문서와 거의 1:1로 대응하도록 관리됨 → 어떤 DOM API의 타입 시그니처가 궁금하면 MDN에서 같은 이름을 찾아보면 됨
- `Document.getElementById`는 요소를 못 찾을 수도 있다는 사실을 반영해 `HTMLElement | null`을 반환

```ts
const app = document.getElementById("app");
// app: HTMLElement | null
app?.append("hello"); // null일 수 있으므로 옵셔널 체이닝 필요
```

- `createElement`, `querySelector`, `querySelectorAll`은 태그 이름을 제네릭 키로 받아 `HTMLElementTagNameMap`을 조회하는 오버로드 보유
  - `"a"`를 넘기면 `HTMLAnchorElement`, `"div"`를 넘기면 `HTMLDivElement`가 나오고, 맵에 없는 임의 문자열을 넘기면 범용 `HTMLElement`로 떨어짐

```ts
const link = document.createElement("a"); // HTMLAnchorElement
const box = document.querySelector("div.box"); // HTMLDivElement | null
const custom = document.createElement("my-widget"); // HTMLElement (맵에 없음)
```

- `Node.appendChild<T extends Node>(newChild: T): T`처럼 매개변수 타입 그대로를 반환 타입으로 돌려주는 제네릭도 자주 보임
  - 인자로 넘긴 구체 타입이 그대로 반환값의 타입이 됨
- `children`(HTMLCollection, 요소 노드만)과 `childNodes`(NodeList, 텍스트 노드 포함 모든 노드)는 타입도 다르고 담기는 값도 다름 → 혼동하지 않아야 함
- `HTMLElement`는 `Element`를 상속하고 `Element`는 `Node`를 상속하는 계층 구조 → 어떤 요소든 `Node`가 제공하는 메서드(`appendChild`, `removeChild` 등)를 그대로 사용 가능

### tsconfig의 lib 옵션

> 원문: https://www.typescriptlang.org/tsconfig/#lib

- `lib`: 컴파일 시 어떤 내장 타입 선언 묶음을 불러올지 정하는 옵션
  - ECMAScript 버전별 타입(`es2015`, `es2020`, …)과 런타임 환경별 타입(`dom`, `dom.iterable`, `webworker` 등)을 조합해서 지정
- `lib`을 생략하면 `target`에 따라 기본값이 자동으로 채워짐
  - 예: `target: "es2020"`이면 `lib`은 기본적으로 `["dom", "es2020", "dom.iterable"]`
  - 즉 별도 설정 없이도 브라우저 DOM 타입은 기본으로 포함
- Node.js처럼 브라우저 DOM이 없는 런타임을 대상으로 한다면 `"dom"`을 명시적으로 빼고 ES 버전만 남기는 것이 안전
  - 그래야 `window`, `document` 같은 존재하지 않는 전역을 실수로 참조했을 때 컴파일 타임에 바로 걸러짐

```json
{
  "compilerOptions": {
    "target": "es2020",
    "lib": ["es2020"] // "dom"을 빼서 브라우저 전역 사용을 막는다
  }
}
```

```json
{
  "compilerOptions": {
    "target": "es2020",
    "lib": ["es2020", "dom", "dom.iterable"] // 브라우저용 프로젝트
  }
}
```

- `lib`을 지정하면 `target`이 정해주던 기본값 전체가 덮어써짐에 유의
  - 즉 `lib: ["dom"]`처럼 ES 버전을 빼먹으면 `Promise`, `Map` 같은 최신 내장 타입도 함께 사라짐

### EventTarget: 이벤트를 주고받는 객체의 공통 인터페이스

> 원문: https://developer.mozilla.org/en-US/docs/Web/API/EventTarget

- `EventTarget`: 이벤트를 받고 리스너를 등록할 수 있는 모든 객체가 구현하는 인터페이스
  - `Element`, `Document`, `Window`뿐 아니라 `XMLHttpRequest`, `AudioNode` 같은 DOM 외부 API도 포함
- 핵심 메서드
  - `addEventListener(type, listener, options?)`: 리스너 등록
  - `removeEventListener(type, listener, options?)`: 리스너 해제
  - `dispatchEvent(event)`: 이벤트를 직접 발생시킴
- `lib.dom.d.ts`에서 `addEventListener`는 이벤트 종류에 따라 리스너의 콜백 매개변수 타입이 달라지도록 오버로드
  - `"click"`을 등록하면 콜백의 인자가 `MouseEvent`로, `"keydown"`을 등록하면 `KeyboardEvent`로 좁혀짐

```ts
const button = document.querySelector("button");

button?.addEventListener("click", (e) => {
  // e: MouseEvent — clientX, clientY 등 마우스 전용 속성 사용 가능
  console.log(e.clientX, e.clientY);
});

button?.addEventListener("keydown", (e) => {
  // e: KeyboardEvent
  console.log(e.key);
});
```

- 세 번째 인자인 `options` 객체(`AddEventListenerOptions`)의 필드들도 타입으로 표현
  - `capture`: 버블링이 아닌 캡처링 단계에서 리스너 실행
  - `once`: 첫 실행 후 자동으로 리스너 제거
  - `passive`: `preventDefault`를 호출하지 않겠다는 선언(스크롤 성능 최적화)
  - `signal`: `AbortSignal`이 abort되면 리스너를 자동 해제

```ts
const controller = new AbortController();

document.addEventListener(
  "scroll",
  () => console.log("scrolling"),
  { passive: true, signal: controller.signal }
);

controller.abort(); // 위 리스너가 자동으로 제거됨
```

- 커스텀 클래스에 이벤트 기능을 붙이고 싶다면 `EventTarget`을 직접 상속하면 됨
  - 브라우저 전용 API가 아니라 Web Workers에서도 동작하는 범용 인터페이스 → 순수 로직 클래스에도 적용 가능

```ts
class Ticker extends EventTarget {
  tick() {
    this.dispatchEvent(new Event("tick"));
  }
}

const ticker = new Ticker();
ticker.addEventListener("tick", () => console.log("tick!"));
ticker.tick();
```

### Event: 이벤트 객체 자체의 타입

> 원문: https://developer.mozilla.org/en-US/docs/Web/API/Event

- `Event`: 이벤트 하나를 나타내는 값
  - `MouseEvent`, `KeyboardEvent`, `CustomEvent` 등은 모두 `Event`를 확장한 하위 타입 → `lib.dom.d.ts`에서도 이 상속 관계가 그대로 인터페이스 계층으로 표현
- 주요 읽기 전용 속성
  - `type: string`: 이벤트 이름(`"click"` 등)
  - `target: EventTarget | null`: 이벤트가 실제로 발생한 객체
  - `currentTarget: EventTarget | null`: 현재 핸들러가 붙어 있는 객체(버블링 중에는 target과 다를 수 있음)
  - `bubbles: boolean` / `cancelable: boolean`: 전파 가능 여부 / 취소 가능 여부
  - `defaultPrevented: boolean`: `preventDefault()` 호출 여부
  - `eventPhase: number`: `NONE`, `CAPTURING_PHASE`, `AT_TARGET`, `BUBBLING_PHASE` 중 현재 단계
- 주요 메서드
  - `preventDefault()`: 브라우저 기본 동작 취소(`cancelable`이 true일 때만 의미 있음)
  - `stopPropagation()`: 캡처링/버블링 전파 중단
  - `stopImmediatePropagation()`: 전파 중단에 더해 같은 요소에 남은 다른 리스너 실행도 막음

```ts
document.addEventListener("click", function handler(e: Event) {
  if (e.target instanceof HTMLButtonElement) {
    e.stopPropagation();
    console.log(e.eventPhase === Event.AT_TARGET);
  }
});
```

- `target`, `currentTarget`은 타입이 `EventTarget | null`로 넓게 잡혀 있어 구체적인 요소 메서드(`.value`, `.classList` 등)를 쓰려면 `instanceof`로 좁히거나 타입 단언 필요

```ts
function onInput(e: Event) {
  const input = e.target as HTMLInputElement;
  console.log(input.value);
}
```

- 이벤트를 직접 만들 때는 생성자에 `bubbles`, `cancelable`, `composed` 옵션 전달 가능
  - `CustomEvent<T>`는 여기에 더해 임의의 데이터를 담는 `detail: T` 필드를 제네릭으로 타입 지정 가능

```ts
interface OrderPlacedDetail {
  orderId: string;
  total: number;
}

const orderEvent = new CustomEvent<OrderPlacedDetail>("order-placed", {
  detail: { orderId: "A-1", total: 39900 },
  bubbles: true,
});
document.dispatchEvent(orderEvent);

document.addEventListener("order-placed", (e) => {
  // e는 Event로 추론되므로 detail을 쓰려면 CustomEvent<OrderPlacedDetail>로 단언이 필요하다
  const detail = (e as CustomEvent<OrderPlacedDetail>).detail;
  console.log(detail.orderId, detail.total);
});
```

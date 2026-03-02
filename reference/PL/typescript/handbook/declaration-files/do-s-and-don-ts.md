# 해야 할 것과 하지 말아야 할 것

## 일반 타입

### `Number`, `String`, `Boolean`, `Symbol`, `Object`

:x: **하지 마세요** `Number`, `String`, `Boolean`, `Symbol`, 또는 `Object` 타입을 절대 사용하지 마세요. 이 타입들은 JavaScript 코드에서 거의 적절하게 사용되지 않는 비원시 박스 객체를 나타냅니다.

```ts
/* 잘못됨 */
function reverse(s: String): String;
```

:white_check_mark: **하세요** `number`, `string`, `boolean`, `symbol` 타입을 사용하세요.

```ts
/* 올바름 */
function reverse(s: string): string;
```

`Object` 대신 비원시 `object` 타입([TypeScript 2.2에서 추가됨](../release-notes/typescript-2-2.html#object-type))을 사용하세요.

### 제네릭

:x: **하지 마세요** 타입 매개변수를 사용하지 않는 제네릭 타입을 만들지 마세요. [TypeScript FAQ 페이지](https://github.com/Microsoft/TypeScript/wiki/FAQ#why-doesnt-type-inference-work-on-this-interface-interface-foot--)에서 자세한 내용을 확인하세요.

### any

:x: **하지 마세요** JavaScript 프로젝트를 TypeScript로 마이그레이션하는 과정이 아니라면 `any`를 타입으로 사용하지 마세요. 컴파일러는 _효과적으로_ `any`를 "이것에 대한 타입 검사를 꺼주세요"로 취급합니다. 이는 변수의 모든 사용에 `@ts-ignore` 주석을 넣는 것과 비슷합니다. JavaScript 프로젝트를 TypeScript로 처음 마이그레이션할 때 아직 마이그레이션하지 않은 것들의 타입을 `any`로 설정할 수 있어 매우 유용할 수 있지만, 전체 TypeScript 프로젝트에서는 이를 사용하는 프로그램의 모든 부분에 대해 타입 검사를 비활성화하는 것입니다.

어떤 타입을 받아야 하는지 모르거나, 상호작용 없이 그냥 통과시킬 것이기 때문에 무엇이든 받고 싶은 경우에는 [`unknown`](/play/#example/unknown-and-never)을 사용할 수 있습니다.

## 콜백 타입

### 콜백의 반환 타입

:x: **하지 마세요** 값이 무시될 콜백에 대해 반환 타입 `any`를 사용하지 마세요:

```ts
/* 잘못됨 */
function fn(x: () => any) {
  x();
}
```

:white_check_mark: **하세요** 값이 무시될 콜백에 대해 반환 타입 `void`를 사용하세요:

```ts
/* 올바름 */
function fn(x: () => void) {
  x();
}
```

:grey_question: **왜:** `void`를 사용하는 것이 더 안전합니다. 확인되지 않은 방식으로 `x`의 반환 값을 실수로 사용하는 것을 방지하기 때문입니다:

```ts
function fn(x: () => void) {
  var k = x(); // 다른 것을 하려고 했지만 실수!
  k.doSomething(); // 오류, 하지만 반환 타입이 'any'였다면 괜찮았을 것
}
```

### 콜백의 선택적 매개변수

:x: **하지 마세요** 정말로 의도한 것이 아니라면 콜백에서 선택적 매개변수를 사용하지 마세요:

```ts
/* 잘못됨 */
interface Fetcher {
  getObject(done: (data: unknown, elapsedTime?: number) => void): void;
}
```

이것은 매우 구체적인 의미를 가집니다: `done` 콜백이 1개의 인수로 호출되거나 2개의 인수로 호출될 수 있습니다. 작성자는 아마도 콜백이 `elapsedTime` 매개변수에 관심이 없을 수 있다는 것을 말하려고 했을 것이지만, 이를 달성하기 위해 매개변수를 선택적으로 만들 필요는 없습니다 -- 더 적은 인수를 받는 콜백을 제공하는 것은 항상 합법적입니다.

:white_check_mark: **하세요** 콜백 매개변수를 비선택적으로 작성하세요:

```ts
/* 올바름 */
interface Fetcher {
  getObject(done: (data: unknown, elapsedTime: number) => void): void;
}
```

### 오버로드와 콜백

:x: **하지 마세요** 콜백 매개변수 개수만 다른 별도의 오버로드를 작성하지 마세요:

```ts
/* 잘못됨 */
declare function beforeAll(action: () => void, timeout?: number): void;
declare function beforeAll(
  action: (done: DoneFn) => void,
  timeout?: number
): void;
```

:white_check_mark: **하세요** 최대 매개변수 개수를 사용하는 단일 오버로드를 작성하세요:

```ts
/* 올바름 */
declare function beforeAll(
  action: (done: DoneFn) => void,
  timeout?: number
): void;
```

:grey_question: **왜:** 콜백이 매개변수를 무시하는 것은 항상 합법적이므로 더 짧은 오버로드가 필요하지 않습니다. 더 짧은 콜백을 먼저 제공하면 첫 번째 오버로드와 일치하기 때문에 잘못 타입이 지정된 함수가 전달될 수 있습니다.

## 함수 오버로드

### 순서

:x: **하지 마세요** 더 구체적인 오버로드보다 더 일반적인 오버로드를 앞에 두지 마세요:

```ts
/* 잘못됨 */
declare function fn(x: unknown): unknown;
declare function fn(x: HTMLElement): number;
declare function fn(x: HTMLDivElement): string;

var myElem: HTMLDivElement;
var x = fn(myElem); // x: unknown, 뭐라고?
```

:white_check_mark: **하세요** 더 구체적인 시그니처를 더 일반적인 시그니처 뒤에 두어 오버로드를 정렬하세요:

```ts
/* 올바름 */
declare function fn(x: HTMLDivElement): string;
declare function fn(x: HTMLElement): number;
declare function fn(x: unknown): unknown;

var myElem: HTMLDivElement;
var x = fn(myElem); // x: string, :)
```

:grey_question: **왜:** TypeScript는 함수 호출을 해결할 때 _첫 번째로 일치하는 오버로드_를 선택합니다. 이전 오버로드가 이후 오버로드보다 "더 일반적"이면, 이후 오버로드는 효과적으로 숨겨지고 호출될 수 없습니다.

### 선택적 매개변수 사용

:x: **하지 마세요** 후행 매개변수만 다른 여러 오버로드를 작성하지 마세요:

```ts
/* 잘못됨 */
interface Example {
  diff(one: string): number;
  diff(one: string, two: string): number;
  diff(one: string, two: string, three: boolean): number;
}
```

:white_check_mark: **하세요** 가능한 경우 선택적 매개변수를 사용하세요:

```ts
/* 올바름 */
interface Example {
  diff(one: string, two?: string, three?: boolean): number;
}
```

이 축소는 모든 오버로드가 동일한 반환 타입을 가질 때만 발생해야 합니다.

:grey_question: **왜:** 이것은 두 가지 이유로 중요합니다.

TypeScript는 대상의 어떤 시그니처가 소스의 인수로 호출될 수 있는지 확인하여 시그니처 호환성을 해결하며, _추가 인수는 허용됩니다_. 예를 들어, 이 코드는 시그니처가 선택적 매개변수를 사용하여 올바르게 작성된 경우에만 버그를 노출합니다:

```ts
function fn(x: (a: string, b: number, c: number) => void) {}
var x: Example;
// 오버로드로 작성된 경우, OK -- 첫 번째 오버로드 사용
// 선택적으로 작성된 경우, 올바르게 오류
fn(x.diff);
```

두 번째 이유는 사용자가 TypeScript의 "엄격한 null 검사" 기능을 사용할 때입니다. 지정되지 않은 매개변수는 JavaScript에서 `undefined`로 나타나므로, 일반적으로 선택적 인수가 있는 함수에 명시적 `undefined`를 전달해도 괜찮습니다. 예를 들어, 이 코드는 엄격한 null에서 OK여야 합니다:

```ts
var x: Example;
// 오버로드로 작성된 경우, 'string'에 'undefined'를 전달하므로 잘못된 오류
// 선택적으로 작성된 경우, 올바르게 OK
x.diff("something", true ? undefined : "hour");
```

### 유니온 타입 사용

:x: **하지 마세요** 하나의 인수 위치에서만 타입이 다른 오버로드를 작성하지 마세요:

```ts
/* 잘못됨 */
interface Moment {
  utcOffset(): number;
  utcOffset(b: number): Moment;
  utcOffset(b: string): Moment;
}
```

:white_check_mark: **하세요** 가능한 경우 유니온 타입을 사용하세요:

```ts
/* 올바름 */
interface Moment {
  utcOffset(): number;
  utcOffset(b: number | string): Moment;
}
```

여기서 `b`를 선택적으로 만들지 않았다는 점에 유의하세요. 시그니처의 반환 타입이 다르기 때문입니다.

:grey_question: **왜:** 이것은 값을 함수에 "통과"시키는 사람들에게 중요합니다:

```ts
function fn(x: string): Moment;
function fn(x: number): Moment;
function fn(x: number | string) {
  // 별도의 오버로드로 작성된 경우, 잘못된 오류
  // 유니온 타입으로 작성된 경우, 올바르게 OK
  return moment().utcOffset(x);
}
```

# 문자열·숫자·심볼 래퍼 타입과 템플릿 리터럴 타입 심화

## 원시 타입과 래퍼 객체 타입은 다르다

> **원문:** https://www.typescriptlang.org/docs/handbook/2/everyday-types.html

- JS의 `string`, `number`, `boolean`은 **원시값**이고, `String`, `Number`, `Boolean`은 이를 감싸는 **내장 객체(래퍼)** 생성자다.
- TypeScript는 소문자로 시작하는 `string` / `number` / `boolean`을 타입으로 쓰길 강제하다시피 권장한다. 대문자 버전(`String`, `Number`, `Boolean`)도 문법상 쓸 수는 있지만 실무 코드에서 쓸 일은 거의 없다.
- 이유는 단순하다. `typeof` 연산자가 실제로 반환하는 값이 `"string"`, `"number"`, `"boolean"`이기 때문에, 소문자 타입이 JS 런타임의 실체와 정확히 대응한다.
- `symbol`, `bigint`도 마찬가지로 소문자만 존재한다. 이 둘은 애초에 대문자 래퍼 타입을 타입 표기 위치에 쓰는 일이 자연스럽지 않다.

```ts
// 권장
let title: string = "타입스크립트";
let count: number = 3;
let done: boolean = false;

// 비권장 (동작은 하지만 의미상 다른 타입)
let title2: String = "타입스크립트";
```

여기서 `String`은 "문자열처럼 보이는 값"이 아니라 "`new String(...)`으로 만들어질 수 있는 객체"를 뜻하는 타입이라, 원시 문자열을 담는 용도로는 부적합하다.

## JS 런타임에서 원시값과 래퍼 객체의 실제 차이

> **원문:** https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String

- `String(1)`처럼 **함수로 호출**하면 원시 문자열을 반환하고, `new String(1)`처럼 **생성자로 호출**하면 `String` 객체를 반환한다. 겉보기엔 비슷해도 `typeof`로 구분하면 전자는 `"string"`, 후자는 `"object"`다.

```js
typeof String(1);      // "string"
typeof new String(1);  // "object"
```

- 문자열 원시값에 `.length`나 `.toUpperCase()` 같은 메서드를 호출하면, JS 엔진이 순간적으로 원시값을 `String` 객체로 감쌌다가(오토박싱) 메서드 실행 후 다시 버린다. 이 과정 덕분에 원시값도 마치 객체처럼 메서드를 쓸 수 있다.
- 원시값과 래퍼 객체를 `===`로 비교하면 타입 자체가 다르므로 항상 `false`다. `==`는 타입 강제 변환이 일어나서 `true`가 된다.

```js
const a = "hi";
const b = new String("hi");

a === b; // false, string vs object
a == b;  // true, 값만 비교하도록 강제 변환됨
```

- 이런 함정 때문에 `new String(...)`으로 문자열을 만드는 건 실무에서 지양된다. TypeScript가 `String` 타입 사용을 말리는 이유도 결국 이 런타임 동작과 맞닿아 있다. `string` 타입은 "원시 문자열"만 가리키므로, `new String(...)`이 만든 객체를 `string` 타입 변수에 넣으면 타입 에러가 난다.

## 템플릿 리터럴 타입: 문자열 리터럴의 조합

> **원문:** https://www.typescriptlang.org/docs/handbook/release-notes/typescript-4-1.html

TypeScript 4.1부터 JS의 템플릿 리터럴 문법을 **타입 위치**에서 그대로 쓸 수 있게 됐다. 문자열 리터럴 타입들을 이어 붙여 새 리터럴 타입을 만드는 방식이다.

```ts
type Lang = "ko" | "en";
type Greeting = `hello-${Lang}`;
// "hello-ko" | "hello-en"
```

유니온 타입을 템플릿 안에 넣으면 **가능한 모든 조합**이 자동으로 전개된다.

```ts
type Size = "sm" | "md" | "lg";
type Color = "red" | "blue";

type ButtonClass = `btn-${Size}-${Color}`;
// "btn-sm-red" | "btn-sm-blue" | "btn-md-red" | "btn-md-blue"
// | "btn-lg-red" | "btn-lg-blue"
```

이 성질을 이용하면 오타나 잘못된 조합을 컴파일 타임에 잡아낼 수 있다.

```ts
declare function setPosition(pos: `${"top" | "bottom"}-${"left" | "right"}`): void;

setPosition("top-left");   // OK
setPosition("top-lfet");   // 에러: 오타
```

## 문자열 대소문자 변환 유틸리티 타입

> **원문:** https://www.typescriptlang.org/docs/handbook/release-notes/typescript-4-1.html

템플릿 리터럴 타입과 함께 문자열 리터럴의 대소문자를 변환하는 유틸리티 타입 4종이 추가됐다. 모두 컴파일 타임에만 존재하며 런타임 함수가 아니다.

| 유틸리티 타입 | 동작 |
| --- | --- |
| `Uppercase<T>` | 전부 대문자로 |
| `Lowercase<T>` | 전부 소문자로 |
| `Capitalize<T>` | 첫 글자만 대문자로 |
| `Uncapitalize<T>` | 첫 글자만 소문자로 |

```ts
type A = Uppercase<"hi">;      // "HI"
type B = Lowercase<"HI">;      // "hi"
type C = Capitalize<"hi">;     // "Hi"
type D = Uncapitalize<"Hi">;   // "hi"
```

## 매핑된 타입의 키 재매핑(`as` 절)과 조합

> **원문:** https://www.typescriptlang.org/docs/handbook/release-notes/typescript-4-1.html

기존 매핑된 타입은 원본 타입의 키를 그대로 쓸 수밖에 없었다. 4.1부터 `as` 절로 **키 자체를 새 문자열로 바꿔치기**할 수 있게 됐고, 이걸 템플릿 리터럴 타입·`Capitalize` 같은 유틸리티와 엮으면 접근자(getter) 타입을 자동 생성하는 식의 패턴이 가능해진다.

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

`as` 절에서 결과를 `never`로 매핑하면 해당 키를 결과 타입에서 완전히 제외할 수 있다. 이 방식으로 특정 필드를 걸러내는 유틸리티 타입도 직접 만들 수 있다.

```ts
type OmitKind<T> = {
  [K in keyof T as Exclude<K, "kind">]: T[K];
};
```

## 템플릿 리터럴 패턴으로부터 타입 추론하기

> **원문:** https://www.typescriptlang.org/docs/handbook/release-notes/typescript-4-1.html

템플릿 리터럴 타입은 값을 만들어내는 용도뿐 아니라, 함수 매개변수 위치에서 **문자열 패턴을 분석해 관련 타입을 역으로 추론**하는 데도 쓰인다. 이벤트 이름 문자열에서 원본 속성 키를 뽑아내고, 그 키에 대응하는 값 타입을 콜백 인자 타입으로 자동 연결하는 패턴이 대표적이다.

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

`event` 문자열이 `"ageChanged"`로 들어오면 컴파일러가 템플릿 패턴 `${K}Changed`를 역산해 `K`를 `"age"`로 추론하고, 그 결과 콜백의 `value` 매개변수 타입까지 `T["age"]`, 즉 `number`로 자동 결정된다. 문자열 하나로 여러 오버로드를 대체할 수 있다는 게 이 기능의 핵심 가치다.

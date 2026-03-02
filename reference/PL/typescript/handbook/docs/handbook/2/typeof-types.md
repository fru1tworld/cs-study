---
title: Typeof 타입 연산자
layout: docs
permalink: /docs/handbook/2/typeof-types.html
oneline: "타입 컨텍스트에서 typeof 연산자 사용하기"
---

## `typeof` 타입 연산자

JavaScript에는 이미 _표현식_ 컨텍스트에서 사용할 수 있는 `typeof` 연산자가 있습니다:

```ts twoslash
// "string"을 출력합니다
console.log(typeof "Hello world");
```

TypeScript는 _타입_ 컨텍스트에서 변수나 속성의 _타입_을 참조하는 데 사용할 수 있는 `typeof` 연산자를 추가합니다:

```ts twoslash
let s = "hello";
let n: typeof s;
//  ^?
```

이것은 기본 타입에는 그다지 유용하지 않지만, 다른 타입 연산자와 결합하면 `typeof`를 사용하여 많은 패턴을 편리하게 표현할 수 있습니다.
예를 들어, 미리 정의된 타입 `ReturnType<T>`부터 살펴봅시다.
이것은 _함수 타입_을 받아 반환 타입을 생성합니다:

```ts twoslash
type Predicate = (x: unknown) => boolean;
type K = ReturnType<Predicate>;
//   ^?
```

함수 이름에 `ReturnType`을 사용하려고 하면 유익한 오류가 표시됩니다:

```ts twoslash
// @errors: 2749
function f() {
  return { x: 10, y: 3 };
}
type P = ReturnType<f>;
```

_값_과 _타입_은 같은 것이 아닙니다.
_값 `f`_가 가진 _타입_을 참조하려면 `typeof`를 사용합니다:

```ts twoslash
function f() {
  return { x: 10, y: 3 };
}
type P = ReturnType<typeof f>;
//   ^?
```

### 제한 사항

TypeScript는 `typeof`에 사용할 수 있는 표현식의 종류를 의도적으로 제한합니다.

구체적으로, 식별자(즉, 변수 이름)나 그 속성에서만 `typeof`를 사용하는 것이 합법적입니다.
이것은 실행된다고 생각하지만 실행되지 않는 코드를 작성하는 혼란스러운 함정을 피하는 데 도움이 됩니다:

```ts twoslash
// @errors: 1005
declare const msgbox: (prompt: string) => boolean;
// type msgbox = any;
// ---cut---
// = ReturnType<typeof msgbox>를 사용하려고 했습니다
let shouldContinue: typeof msgbox("Are you sure you want to continue?");
```

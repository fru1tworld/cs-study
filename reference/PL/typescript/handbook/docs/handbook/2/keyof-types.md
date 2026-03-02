---
title: Keyof 타입 연산자
layout: docs
permalink: /docs/handbook/2/keyof-types.html
oneline: "타입 컨텍스트에서 keyof 연산자 사용하기"
---

## `keyof` 타입 연산자

`keyof` 연산자는 객체 타입을 받아 해당 키의 문자열 또는 숫자 리터럴 유니온을 생성합니다.
다음 타입 `P`는 `type P = "x" | "y"`와 동일한 타입입니다:

```ts twoslash
type Point = { x: number; y: number };
type P = keyof Point;
//   ^?
```

타입에 `string` 또는 `number` 인덱스 시그니처가 있으면, `keyof`는 해당 타입을 대신 반환합니다:

```ts twoslash
type Arrayish = { [n: number]: unknown };
type A = keyof Arrayish;
//   ^?

type Mapish = { [k: string]: boolean };
type M = keyof Mapish;
//   ^?
```

이 예제에서 `M`은 `string | number`입니다 -- 이는 JavaScript 객체 키가 항상 문자열로 강제 변환되기 때문에, `obj[0]`은 항상 `obj["0"]`과 동일합니다.

`keyof` 타입은 나중에 자세히 배울 매핑된 타입과 결합될 때 특히 유용해집니다.

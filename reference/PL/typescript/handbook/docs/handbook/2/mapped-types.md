---
title: 매핑된 타입
layout: docs
permalink: /docs/handbook/2/mapped-types.html
oneline: "기존 타입을 재사용하여 타입 생성하기"
---

반복하고 싶지 않을 때, 때때로 타입은 다른 타입을 기반으로 해야 합니다.

매핑된 타입은 미리 선언되지 않은 속성의 타입을 선언하는 데 사용되는 인덱스 시그니처 문법을 기반으로 합니다:

```ts twoslash
type Horse = {};
// ---cut---
type OnlyBoolsAndHorses = {
  [key: string]: boolean | Horse;
};

const conforms: OnlyBoolsAndHorses = {
  del: true,
  rodney: false,
};
```

매핑된 타입은 `PropertyKey`의 유니온(자주 [`keyof`를 통해](/docs/handbook/2/indexed-access-types.html) 생성됨)을 사용하여 키를 반복하여 타입을 생성하는 제네릭 타입입니다:

```ts twoslash
type OptionsFlags<Type> = {
  [Property in keyof Type]: boolean;
};
```

이 예제에서 `OptionsFlags`는 `Type` 타입의 모든 속성을 가져와 값을 불리언으로 변경합니다.

```ts twoslash
type OptionsFlags<Type> = {
  [Property in keyof Type]: boolean;
};
// ---cut---
type Features = {
  darkMode: () => void;
  newUserProfile: () => void;
};

type FeatureOptions = OptionsFlags<Features>;
//   ^?
```

### 매핑 수정자

매핑 중에 적용할 수 있는 두 가지 추가 수정자가 있습니다: `readonly`와 `?`는 각각 가변성과 선택성에 영향을 미칩니다.

`-` 또는 `+`를 접두사로 붙여 이러한 수정자를 제거하거나 추가할 수 있습니다. 접두사를 추가하지 않으면 `+`로 가정됩니다.

```ts twoslash
// 타입의 속성에서 'readonly' 속성을 제거합니다
type CreateMutable<Type> = {
  -readonly [Property in keyof Type]: Type[Property];
};

type LockedAccount = {
  readonly id: string;
  readonly name: string;
};

type UnlockedAccount = CreateMutable<LockedAccount>;
//   ^?
```

```ts twoslash
// 타입의 속성에서 'optional' 속성을 제거합니다
type Concrete<Type> = {
  [Property in keyof Type]-?: Type[Property];
};

type MaybeUser = {
  id: string;
  name?: string;
  age?: number;
};

type User = Concrete<MaybeUser>;
//   ^?
```

## `as`를 통한 키 재매핑

TypeScript 4.1 이상에서는 매핑된 타입의 `as` 절을 사용하여 매핑된 타입의 키를 다시 매핑할 수 있습니다:

```ts
type MappedTypeWithNewProperties<Type> = {
    [Properties in keyof Type as NewKeyType]: Type[Properties]
}
```

[템플릿 리터럴 타입](/docs/handbook/2/template-literal-types.html)과 같은 기능을 활용하여 이전 속성 이름에서 새 속성 이름을 만들 수 있습니다:

```ts twoslash
type Getters<Type> = {
    [Property in keyof Type as `get${Capitalize<string & Property>}`]: () => Type[Property]
};

interface Person {
    name: string;
    age: number;
    location: string;
}

type LazyPerson = Getters<Person>;
//   ^?
```

조건부 타입을 통해 `never`를 생성하여 키를 필터링할 수 있습니다:

```ts twoslash
// 'kind' 속성을 제거합니다
type RemoveKindField<Type> = {
    [Property in keyof Type as Exclude<Property, "kind">]: Type[Property]
};

interface Circle {
    kind: "circle";
    radius: number;
}

type KindlessCircle = RemoveKindField<Circle>;
//   ^?
```

`string | number | symbol`의 유니온뿐만 아니라 모든 타입의 유니온에 대해 매핑할 수 있습니다:

```ts twoslash
type EventConfig<Events extends { kind: string }> = {
    [E in Events as E["kind"]]: (event: E) => void;
}

type SquareEvent = { kind: "square", x: number, y: number };
type CircleEvent = { kind: "circle", radius: number };

type Config = EventConfig<SquareEvent | CircleEvent>
//   ^?
```

### 추가 탐구

매핑된 타입은 이 타입 조작 섹션의 다른 기능과 잘 작동합니다. 예를 들어 여기에 [조건부 타입을 사용하는 매핑된 타입](/docs/handbook/2/conditional-types.html)이 있으며, 객체에 `pii` 속성이 리터럴 `true`로 설정되어 있는지 여부에 따라 `true` 또는 `false`를 반환합니다:

```ts twoslash
type ExtractPII<Type> = {
  [Property in keyof Type]: Type[Property] extends { pii: true } ? true : false;
};

type DBFields = {
  id: { format: "incrementing" };
  name: { type: string; pii: true };
};

type ObjectsNeedingGDPRDeletion = ExtractPII<DBFields>;
//   ^?
```

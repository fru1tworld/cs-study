---
title: 템플릿 리터럴 타입
layout: docs
permalink: /docs/handbook/2/template-literal-types.html
oneline: "템플릿 리터럴 문자열을 통해 속성을 변경하는 매핑 타입 생성하기"
---

템플릿 리터럴 타입은 [문자열 리터럴 타입](/docs/handbook/2/everyday-types.html#literal-types)을 기반으로 하며, 유니온을 통해 많은 문자열로 확장할 수 있는 기능을 가지고 있습니다.

[JavaScript의 템플릿 리터럴 문자열](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Template_literals)과 동일한 문법을 사용하지만, 타입 위치에서 사용됩니다.
구체적인 리터럴 타입과 함께 사용될 때, 템플릿 리터럴은 내용을 연결하여 새로운 문자열 리터럴 타입을 생성합니다.

```ts twoslash
type World = "world";

type Greeting = `hello ${World}`;
//   ^?
```

유니온이 보간된 위치에서 사용되면, 타입은 각 유니온 멤버가 나타낼 수 있는 모든 가능한 문자열 리터럴의 집합입니다:

```ts twoslash
type EmailLocaleIDs = "welcome_email" | "email_heading";
type FooterLocaleIDs = "footer_title" | "footer_sendoff";

type AllLocaleIDs = `${EmailLocaleIDs | FooterLocaleIDs}_id`;
//   ^?
```

템플릿 리터럴의 각 보간된 위치에 대해, 유니온은 교차 곱셈됩니다:

```ts twoslash
type EmailLocaleIDs = "welcome_email" | "email_heading";
type FooterLocaleIDs = "footer_title" | "footer_sendoff";
// ---cut---
type AllLocaleIDs = `${EmailLocaleIDs | FooterLocaleIDs}_id`;
type Lang = "en" | "ja" | "pt";

type LocaleMessageIDs = `${Lang}_${AllLocaleIDs}`;
//   ^?
```

일반적으로 큰 문자열 유니온에는 사전 생성을 사용하는 것이 좋지만, 작은 경우에는 이것이 유용합니다.

### 타입에서의 문자열 유니온

템플릿 리터럴의 힘은 타입 내부의 정보를 기반으로 새 문자열을 정의할 때 나타납니다.

함수(`makeWatchedObject`)가 전달된 객체에 `on()`이라는 새 함수를 추가하는 경우를 고려해 봅시다. JavaScript에서 호출은 다음과 같을 수 있습니다:
`makeWatchedObject(baseObject)`. 기본 객체는 다음과 같이 보일 수 있습니다:

```ts twoslash
// @noErrors
const passedObject = {
  firstName: "Saoirse",
  lastName: "Ronan",
  age: 26,
};
```

기본 객체에 추가될 `on` 함수는 두 개의 인자를 예상합니다: `eventName`(`string`)과 `callback`(`function`).

`eventName`은 `attributeInThePassedObject + "Changed"` 형식이어야 합니다; 따라서 기본 객체의 `firstName` 속성에서 파생된 `firstNameChanged`입니다.

`callback` 함수가 호출될 때:
  * `attributeInThePassedObject` 이름과 연관된 타입의 값이 전달되어야 합니다; 따라서 `firstName`이 `string`으로 타입이 지정되었으므로, `firstNameChanged` 이벤트의 콜백은 호출 시 `string`이 전달될 것으로 예상합니다. 마찬가지로 `age`와 연관된 이벤트는 `number` 인자로 호출될 것으로 예상해야 합니다
  * `void` 반환 타입을 가져야 합니다(시연의 단순성을 위해)

`on()`의 순진한 함수 시그니처는 다음과 같을 수 있습니다: `on(eventName: string, callback: (newValue: any) => void)`. 그러나 앞의 설명에서 코드에 문서화하고 싶은 중요한 타입 제약을 식별했습니다. 템플릿 리터럴 타입을 사용하면 이러한 제약을 코드에 가져올 수 있습니다.

```ts twoslash
// @noErrors
declare function makeWatchedObject(obj: any): any;
// ---cut---
const person = makeWatchedObject({
  firstName: "Saoirse",
  lastName: "Ronan",
  age: 26,
});

// makeWatchedObject가 익명 Object에 `on`을 추가했습니다

person.on("firstNameChanged", (newValue) => {
  console.log(`firstName was changed to ${newValue}!`);
});
```

`on`이 `"firstName"`이 아닌 이벤트 `"firstNameChanged"`를 수신한다는 점에 주목하세요. `on()`의 순진한 사양은 관찰된 객체의 속성 이름 유니온에 끝에 "Changed"가 추가된 것으로 적합한 이벤트 이름 집합이 제한되도록 보장했다면 더 견고해질 수 있습니다. JavaScript에서 그러한 계산을 수행하는 것이 편하지만, 예를 들어 ``Object.keys(passedObject).map(x => `${x}Changed`)``처럼, _타입 시스템 내부의_ 템플릿 리터럴은 문자열 조작에 대한 유사한 접근 방식을 제공합니다:

```ts twoslash
type PropEventSource<Type> = {
    on(eventName: `${string & keyof Type}Changed`, callback: (newValue: any) => void): void;
};

/// 속성의 변경 사항을 관찰할 수 있도록
/// `on` 메서드가 있는 "관찰된 객체"를 생성합니다.
declare function makeWatchedObject<Type>(obj: Type): Type & PropEventSource<Type>;
```

이를 통해, 잘못된 속성이 주어지면 오류가 발생하는 것을 만들 수 있습니다:

```ts twoslash
// @errors: 2345
type PropEventSource<Type> = {
    on(eventName: `${string & keyof Type}Changed`, callback: (newValue: any) => void): void;
};

declare function makeWatchedObject<T>(obj: T): T & PropEventSource<T>;
// ---cut---
const person = makeWatchedObject({
  firstName: "Saoirse",
  lastName: "Ronan",
  age: 26
});

person.on("firstNameChanged", () => {});

// 쉬운 인적 오류 방지(이벤트 이름 대신 키 사용)
person.on("firstName", () => {});

// 오타 방지
person.on("frstNameChanged", () => {});
```

### 템플릿 리터럴을 사용한 추론

원래 전달된 객체에서 제공된 모든 정보를 활용하지 않았다는 점에 주목하세요. `firstName`의 변경(즉, `firstNameChanged` 이벤트)이 주어지면, 콜백이 `string` 타입의 인자를 받을 것으로 예상해야 합니다. 마찬가지로 `age`의 변경에 대한 콜백은 `number` 인자를 받아야 합니다. 우리는 `callback`의 인자 타입으로 `any`를 순진하게 사용하고 있습니다. 다시 말해, 템플릿 리터럴 타입을 사용하면 속성의 데이터 타입이 해당 속성의 콜백의 첫 번째 인자와 동일한 타입이 되도록 보장할 수 있습니다.

이것을 가능하게 하는 핵심 통찰력은 다음과 같습니다: 우리는 제네릭이 있는 함수를 사용하여:

1. 첫 번째 인자에서 사용된 리터럴이 리터럴 타입으로 캡처됩니다
2. 해당 리터럴 타입이 제네릭의 유효한 속성 유니온에 있는지 검증될 수 있습니다
3. 검증된 속성의 타입을 인덱스 접근을 사용하여 제네릭의 구조에서 조회할 수 있습니다
4. 이 타입 정보가 콜백 함수의 인자가 동일한 타입인지 확인하는 데 _적용될 수_ 있습니다

```ts twoslash
type PropEventSource<Type> = {
    on<Key extends string & keyof Type>
        (eventName: `${Key}Changed`, callback: (newValue: Type[Key]) => void): void;
};

declare function makeWatchedObject<Type>(obj: Type): Type & PropEventSource<Type>;

const person = makeWatchedObject({
  firstName: "Saoirse",
  lastName: "Ronan",
  age: 26
});

person.on("firstNameChanged", newName => {
    //                        ^?
    console.log(`new name is ${newName.toUpperCase()}`);
});

person.on("ageChanged", newAge => {
    //                  ^?
    if (newAge < 0) {
        console.warn("warning! negative age");
    }
})
```

여기서 `on`을 제네릭 메서드로 만들었습니다.

사용자가 문자열 `"firstNameChanged"`로 호출할 때, TypeScript는 `Key`에 대한 올바른 타입을 추론하려고 합니다.
그렇게 하기 위해, `"Changed"` 앞의 내용에 대해 `Key`를 매치하고 문자열 `"firstName"`을 추론합니다.
TypeScript가 이를 파악하면, `on` 메서드는 원래 객체에서 `firstName`의 타입을 가져올 수 있으며, 이 경우 `string`입니다.
마찬가지로, `"ageChanged"`로 호출되면, TypeScript는 `number`인 속성 `age`의 타입을 찾습니다.

추론은 다양한 방식으로 결합될 수 있으며, 종종 문자열을 분해하고 다른 방식으로 재구성합니다.

## 내장 문자열 조작 타입

문자열 조작을 돕기 위해, TypeScript에는 문자열 조작에 사용할 수 있는 타입 집합이 포함되어 있습니다. 이러한 타입은 성능을 위해 컴파일러에 내장되어 있으며 TypeScript에 포함된 `.d.ts` 파일에서 찾을 수 없습니다.

### `Uppercase<StringType>`

문자열의 각 문자를 대문자 버전으로 변환합니다.

##### 예제

```ts twoslash
type Greeting = "Hello, world"
type ShoutyGreeting = Uppercase<Greeting>
//   ^?

type ASCIICacheKey<Str extends string> = `ID-${Uppercase<Str>}`
type MainID = ASCIICacheKey<"my_app">
//   ^?
```

### `Lowercase<StringType>`

문자열의 각 문자를 소문자에 해당하는 것으로 변환합니다.

##### 예제

```ts twoslash
type Greeting = "Hello, world"
type QuietGreeting = Lowercase<Greeting>
//   ^?

type ASCIICacheKey<Str extends string> = `id-${Lowercase<Str>}`
type MainID = ASCIICacheKey<"MY_APP">
//   ^?
```

### `Capitalize<StringType>`

문자열의 첫 번째 문자를 대문자에 해당하는 것으로 변환합니다.

##### 예제

```ts twoslash
type LowercaseGreeting = "hello, world";
type Greeting = Capitalize<LowercaseGreeting>;
//   ^?
```

### `Uncapitalize<StringType>`

문자열의 첫 번째 문자를 소문자에 해당하는 것으로 변환합니다.

##### 예제

```ts twoslash
type UppercaseGreeting = "HELLO WORLD";
type UncomfortableGreeting = Uncapitalize<UppercaseGreeting>;
//   ^?
```

<details>
    <summary>내장 문자열 조작 타입에 대한 기술적 세부 사항</summary>
    <p>TypeScript 4.1 기준으로, 이러한 내장 함수의 코드는 조작을 위해 JavaScript 문자열 런타임 함수를 직접 사용하며 로케일을 인식하지 않습니다.</p>
    <code><pre>
function applyStringMapping(symbol: Symbol, str: string) {
    switch (intrinsicTypeKinds.get(symbol.escapedName as string)) {
        case IntrinsicTypeKind.Uppercase: return str.toUpperCase();
        case IntrinsicTypeKind.Lowercase: return str.toLowerCase();
        case IntrinsicTypeKind.Capitalize: return str.charAt(0).toUpperCase() + str.slice(1);
        case IntrinsicTypeKind.Uncapitalize: return str.charAt(0).toLowerCase() + str.slice(1);
    }
    return str;
}</pre></code>
</details>

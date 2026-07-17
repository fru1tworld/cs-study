# JSON과 정규식 타입

TypeScript는 JS 내장 객체인 `JSON`과 `RegExp`에도 타입을 붙여야 한다. 문제는 두 객체 모두 런타임에 값의 "모양"이 고정되어 있지 않다는 점이다. `JSON.parse`는 문자열만 보고 아무 값이나 만들어낼 수 있고, 정규식의 캡처 그룹 개수와 이름은 패턴 문자열 안에 들어 있어 컴파일러가 미리 알 방법이 없다. 이 문서는 이 둘을 TypeScript가 어떻게 타입으로 다루는지, 그리고 TS 4.1의 템플릿 리터럴 타입이 이 한계를 어떻게 완화하는지 정리한다.

## JSON 타입: `any`로 열려 있는 이유

> **원문:** https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/JSON

`lib.es5.d.ts`에 정의된 시그니처는 대략 다음과 같다.

```ts
interface JSON {
  parse(text: string, reviver?: (key: string, value: any) => any): any;
  stringify(value: any, replacer?: ..., space?: string | number): string;
}
```

- `JSON.parse`의 반환 타입은 **`any`** 다. JSON 문자열 안에 무엇이 들어 있는지는 실행 시점에만 알 수 있으므로, 컴파일러가 미리 구조를 검증해줄 수 없다.
- 따라서 파싱 결과를 곧바로 어떤 인터페이스에 대입해 쓰는 코드는 타입 체크를 통과하더라도 실제 값이 그 모양이라는 보장이 없다. 검증이 필요하면 별도의 스키마 검사(zod, io-ts 등)나 사용자 정의 타입 가드를 거쳐야 한다.

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

- `JSON.stringify`도 `value`를 `any`로 받는다. `undefined`, 함수, `Symbol`은 JSON으로 직렬화될 수 없는 값이라 객체 속성에서는 통째로 생략되고, 배열 원소로 들어가면 `null`로 바뀐다. 이 규칙은 타입 시스템이 아니라 런타임 동작이므로, 타입만 보고는 어떤 속성이 사라질지 알 수 없다는 점을 기억해야 한다.

```ts
const payload = { name: "a", onClick: () => {}, tag: undefined };
JSON.stringify(payload); // '{"name":"a"}' — onClick, tag 모두 사라짐
```

- `reviver`(파싱 시)와 `replacer`(직렬화 시) 콜백도 타입 상으로는 `(key, value) => any` 수준으로만 제약된다. 콜백 내부에서 어떤 타입이 오는지는 개발자가 직접 좁혀야 한다.

## 정규식 타입: `RegExp`, `RegExpMatchArray`, `RegExpExecArray`

> **원문:** https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp

`RegExp`는 리터럴(`/ab+c/i`)이나 생성자(`new RegExp("ab+c", "i")`)로 만들 수 있고, 다음과 같은 읽기 전용 속성을 갖는다.

| 속성 | 의미 |
|---|---|
| `source` | 플래그를 뺀 패턴 문자열 |
| `flags` | 적용된 플래그 전체 문자열 |
| `global` / `ignoreCase` / `multiline` / `sticky` / `unicode` / `dotAll` / `hasIndices` | 각 플래그(`g i m y u s d`)의 개별 boolean |
| `lastIndex` | `g`, `y` 플래그가 있을 때 다음 검색 시작 위치 (읽기/쓰기 가능) |

이 속성들은 패턴이 컴파일 시점에 문자열로만 존재하기 때문에, 리터럴로 작성해도 `flags`나 `global` 같은 값이 리터럴 타입으로 좁혀지지 않고 `string`, `boolean`으로만 잡힌다.

```ts
const re = /\d+/g;
re.global; // 타입은 boolean (실제 값은 true지만 리터럴로 좁혀지지 않음)
```

`test`는 `boolean`을 반환하므로 타입 추론에 특별한 문제가 없다. 반면 `exec`와 문자열의 `match`는 매치 결과를 배열 형태로 돌려주는데, 이 결과 타입이 `RegExpExecArray` / `RegExpMatchArray`다.

## `exec()`의 반환 타입: 인덱스 시그니처 배열 + 부가 속성

> **원문:** https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp/exec

`exec()`는 매치가 없으면 `null`, 있으면 배열 하나를 반환한다. 이 배열은 `string[]`이 아니라 다음 필드를 덧붙인 특수한 형태다.

- `[0]`: 전체 매치 문자열, `[1], [2], ...`: 캡처 그룹
- `index`: 매치가 시작된 위치
- `input`: 검색 대상이 된 원본 문자열
- `groups`: 이름 붙은 캡처 그룹 객체 (없으면 `undefined`)

TypeScript는 이를 `interface RegExpExecArray extends Array<string> { index: number; input: string; groups?: { [key: string]: string } }`로 정의해 둔다. 여기서 중요한 지점은 **`groups`의 타입이 패턴과 무관하게 항상 `{ [key: string]: string } | undefined`라는 것**이다. 즉 `(?<year>\d+)-(?<month>\d+)` 같은 패턴을 써도 TypeScript는 `groups.year`가 존재한다는 걸 알지 못하고, 인덱스 시그니처를 통한 `string | undefined` 접근만 허용한다.

```ts
const re = /(?<year>\d{4})-(?<month>\d{2})/;
const m = re.exec("2024-05");
if (m?.groups) {
  const year = m.groups.year; // string (컴파일러 입장에서는 인덱스 시그니처 값)
}
```

`g` 또는 `y` 플래그가 있으면 `exec`는 호출할 때마다 `lastIndex`를 이어서 진행하는 상태 저장(stateful) 메서드가 되고, 매치가 더 없으면 `null`을 반환하며 `lastIndex`가 `0`으로 리셋된다. 이 상태 전이는 타입에 드러나지 않으므로, `while ((m = re.exec(str)))` 같은 반복문에서 무한 루프에 빠지지 않도록 `g` 플래그 사용은 직접 관리해야 한다.

## TypeScript 4.1과의 접점: 템플릿 리터럴 타입으로 문자열 구조를 타입화하기

> **원문:** https://www.typescriptlang.org/docs/handbook/release-notes/typescript-4-1.html

앞서 본 것처럼 `JSON.parse`의 반환 타입과 정규식 캡처 그룹의 `groups` 타입은 둘 다 "문자열 안에 있는 구조를 컴파일 타임에 알 수 없다"는 같은 한계를 공유한다. TypeScript 4.1에서 도입된 **템플릿 리터럴 타입**(template literal types)은 이 한계를 부분적으로 메운다. 문자열 리터럴 타입을 문자열 그대로가 아니라 하나의 패턴처럼 조합할 수 있게 되면서, 정규식이 하던 일의 일부(문자열 형태 검증)를 타입 레벨에서 흉내 낼 수 있게 됐다.

```ts
type EventName<T extends string> = `on${Capitalize<T>}`;
type ClickEvent = EventName<"click">; // "onClick"

// 정규식으로 "YYYY-MM-DD" 형태를 검사하는 대신, 형태 자체를 타입으로 표현
type ISODate = `${number}-${number}-${number}`;
const d: ISODate = "2024-05-01"; // OK
```

물론 템플릿 리터럴 타입은 실제 정규식처럼 자릿수나 값의 범위를 세밀하게 제약하지는 못한다(`${number}`는 자릿수 제한이 없다). 그래도 `RegExpExecArray.groups`가 항상 `string` 인덱스 시그니처로만 잡히는 것과 달리, 문자열의 정적인 형태를 타입으로 표현하고 싶을 때는 정규식 대신 템플릿 리터럴 타입을 쓰는 편이 컴파일 타임 검증을 받을 수 있어 유리하다. 반대로 `JSON.parse`처럼 아예 런타임 입력에 의존하는 값은 템플릿 리터럴 타입으로도 좁힐 수 없고, 여전히 별도의 런타임 검증이 필요하다.

## 정리

- `JSON.parse`/`JSON.stringify`는 값의 실제 구조를 알 수 없으므로 각각 `any` 입력·출력으로 열려 있다. 타입 안전성은 스키마 검증이나 타입 가드로 직접 확보해야 한다.
- `RegExp`의 인스턴스 속성(`flags`, `global` 등)은 패턴 문자열을 분석하지 않으므로 일반적인 `string`/`boolean`으로만 타입이 잡힌다.
- `exec()`/`match()`가 반환하는 `RegExpExecArray`/`RegExpMatchArray`는 배열에 `index`, `input`, `groups`를 얹은 구조이며, `groups`는 패턴과 무관하게 `{ [key: string]: string } | undefined`로 고정된다.
- TS 4.1의 템플릿 리터럴 타입은 문자열의 정적인 형태를 타입 레벨에서 표현할 수 있게 해주지만, `JSON.parse`처럼 순수한 런타임 값까지 좁혀주지는 못한다.

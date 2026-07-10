# Map·Set·WeakMap·WeakSet 타입

TypeScript는 `Map`, `Set`, `WeakMap`, `WeakSet`을 직접 만들지 않는다. 이들은 자바스크립트 런타임(엔진)이 제공하는 내장 객체이고, TypeScript는 `lib.es2015.collection.d.ts` 같은 선언 파일을 통해 여기에 제네릭 타입을 입혀줄 뿐이다. 그래서 이 네 가지를 제대로 쓰려면 (1) 자바스크립트 런타임 동작 자체와 (2) TypeScript가 `tsconfig.json`의 `lib` 옵션으로 그 동작에 타입을 매핑하는 방식을 함께 알아야 한다.

## Map

> **원문:** https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Map

- 키-값 쌍을 저장하며 **삽입 순서**를 기억하는 컬렉션이다.
- 일반 객체와 달리 키로 문자열/심벌뿐 아니라 객체, 함수, 숫자 등 어떤 값이든 쓸 수 있고, 프로토타입 체인에서 오는 의도치 않은 기본 키가 없어 프로토타입 오염 걱정도 없다.
- 키 비교는 `SameValueZero` 알고리즘을 쓴다. `===`와 거의 같지만 `NaN`을 자기 자신과 같다고 취급한다는 점만 다르다. 객체 키는 값이 아니라 **참조 동일성**으로 비교된다.

```ts
const userAge = new Map<string, number>();
userAge.set("bora", 28).set("minsu", 31);

userAge.get("bora");        // number | undefined
userAge.has("minsu");       // boolean
userAge.size;                // number

const objKeyMap = new Map<object, string>();
const key = {};
objKeyMap.set(key, "value");
objKeyMap.get({});           // undefined — 다른 참조라서 못 찾음
objKeyMap.get(key);          // "value"
```

TypeScript 관점에서 중요한 점은 `get`의 반환 타입이 `V | undefined`라는 것이다. 키가 없을 수 있다는 사실을 타입 시스템이 강제로 알려주므로, 사용하기 전에 `undefined` 체크나 널 병합 연산자(`??`)로 좁혀야 한다.

```ts
const value = userAge.get("bora") ?? 0; // number로 좁혀짐
```

`Map`은 이터러블 프로토콜을 구현하므로 `for...of`, 스프레드, `Array.from`과 자연스럽게 어울리고, TypeScript는 이 순회 결과의 타입까지 추론해 준다.

```ts
for (const [name, age] of userAge) {
  //          ^? string, number
}
const entries: [string, number][] = [...userAge];
```

## Set

> **원문:** https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Set

- 중복 없는 값의 컬렉션이며 삽입 순서를 유지한다.
- `Map`과 마찬가지로 `SameValueZero`로 동등성을 판단하므로 `NaN`은 한 번만 저장되지만, 내용이 같은 두 객체 리터럴은 참조가 다르면 별개의 원소로 취급된다.
- 배열의 `includes()`보다 `has()`가 평균적으로 더 빠르므로 "존재 여부 확인"이 주 목적이라면 배열 대신 `Set`을 쓰는 편이 낫다.

```ts
const tags = new Set<string>(["ts", "js"]);
tags.add("go");
tags.has("ts");   // boolean
tags.size;         // number

const nums = new Set<number>();
nums.add(NaN).add(NaN);
nums.size; // 1
```

`Set<T>`도 이터러블이라 배열로 변환할 때 흔히 `[...new Set(arr)]` 형태로 중복 제거 용도로 쓰인다. 이때 결과 타입은 `T[]`로 그대로 유지된다.

```ts
function unique<T>(arr: T[]): T[] {
  return [...new Set(arr)];
}
```

## WeakMap

> **원문:** https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/WeakMap

`WeakMap<K, V>`은 `Map<K, V>`과 겉보기엔 비슷하지만 키를 다루는 방식이 근본적으로 다르다.

- 키로 **객체(또는 등록되지 않은 심벌)만** 허용한다. 원시값은 가비지 컬렉션 대상이 될 수 없어서 애초에 "약한 참조"라는 개념이 성립하지 않기 때문이다.
- 키에 대한 참조가 **약한 참조**(weak reference)다. 다른 곳에서 그 객체를 더 이상 참조하지 않으면 GC가 객체를 회수할 수 있고, 그러면 `WeakMap`의 해당 엔트리도 함께 사라진다.
- 이 비결정적인 특성 때문에 `size`, `keys()`, `values()`, `entries()`, `forEach()`가 **없다**. 순회 결과가 GC 타이밍에 따라 달라지면 안 되기 때문이다. `get`/`set`/`has`/`delete`만 존재한다.

```ts
class Widget {}
const privateData = new WeakMap<Widget, { secret: string }>();

function attach(w: Widget, secret: string) {
  privateData.set(w, { secret });
}
function reveal(w: Widget) {
  return privateData.get(w)?.secret; // string | undefined
}
```

TypeScript 타입 선언에서도 이 제약이 그대로 드러난다. `WeakMap`의 키 타입 파라미터는 `object` 계열로 한정되어 있어서, 문자열이나 숫자를 키로 넣으려 하면 컴파일 타임에 바로 오류가 난다.

```ts
const wm = new WeakMap<object, number>();
// wm.set("id", 1); // 오류: string은 WeakMap의 키가 될 수 없다
```

주 용도는 세 가지다.

- 클래스 인스턴스에 딸린 **비공개 데이터**를 클로저 없이 저장하기
- DOM 노드 같은 객체에 **부가 메타데이터**를 붙이되, 노드가 사라지면 메타데이터도 자동으로 정리되게 하기
- 객체를 키로 하는 **캐시**를 만들되 메모리 누수 없이 하기

## WeakSet

> **원문:** https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/WeakSet

`WeakSet<T>`은 `WeakMap`의 값 없는 버전, 즉 "이 객체를 봤는지 여부"만 기억하고 싶을 때 쓰는 컬렉션이다.

- 저장할 수 있는 값은 `WeakMap`의 키와 동일하게 객체(또는 등록되지 않은 심벌)로 한정된다.
- 참조는 약하며, `size`나 순회 메서드가 없다. `add`/`has`/`delete`만 제공한다.
- 대표적인 용도는 객체 그래프를 재귀적으로 순회할 때 **순환 참조를 탐지**하는 것이다. 이미 방문한 객체를 `WeakSet`에 표시해 두고, 순회가 끝나면 특별히 정리하지 않아도 GC가 알아서 처리해 준다.

```ts
function walk(obj: object, seen = new WeakSet<object>()): void {
  if (seen.has(obj)) return; // 순환 참조 방지
  seen.add(obj);
  for (const value of Object.values(obj)) {
    if (typeof value === "object" && value !== null) {
      walk(value, seen);
    }
  }
}
```

## tsconfig의 lib 옵션과 컬렉션 타입

> **원문:** https://www.typescriptlang.org/tsconfig/#lib

`Map`, `Set`, `WeakMap`, `WeakSet`의 타입 선언은 코드가 아니라 **`lib` 옵션이 어떤 선언 파일을 프로젝트에 포함시키는지**에 달려 있다.

- `target`은 코드가 컴파일될 자바스크립트 버전을 정하고, `lib`은 타입 검사에 쓸 **전역 타입 선언 목록**을 정한다. 둘은 독립적이다. 예를 들어 `target: "es5"`로 낮게 컴파일하면서도 `lib`에 `es2015`를 넣으면 `Promise`, `Map`, `Set` 같은 타입을 타입 체크에서만 사용할 수 있다(런타임에 실제로 존재하는지는 별개 문제다).
- `lib`을 명시하지 않으면 TypeScript가 `target` 값에 맞춰 적절한 기본 라이브러리 집합을 자동으로 골라 준다. `target`이 `es2015` 이상이면 `Map`/`Set`/`WeakMap`/`WeakSet` 타입은 기본으로 포함된다.
- `lib`을 한 번이라도 직접 지정하면 TypeScript는 자동 추가를 멈추고 **명시한 목록만** 사용하므로, 구버전 `target`에서 이 컬렉션 타입을 쓰려면 `es2015.collection`을 직접 추가해야 한다.

```json
{
  "compilerOptions": {
    "target": "es5",
    "lib": ["es5", "dom", "es2015.collection", "es2015.iterable"]
  }
}
```

`es2015.collection`이 빠지면 `new Map()`이나 `new Set()`을 쓰는 순간 "`Map` 이름을 찾을 수 없습니다" 같은 컴파일 오류가 난다. 이터레이터로 순회(`for...of`, 스프레드)까지 하려면 `es2015.iterable`도 함께 있어야 한다. `lib`은 타입 검사 전용 설정이므로 이 값을 바꿔도 실제로 번들에 폴리필이 추가되거나 런타임 동작이 바뀌지는 않는다 — 대상 런타임에 해당 API가 실제로 존재하는지는 여전히 별도로 챙겨야 한다.

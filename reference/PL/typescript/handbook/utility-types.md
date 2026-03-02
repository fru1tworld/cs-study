# 유틸리티 타입 (Utility Types)

TypeScript는 일반적인 타입 변환을 용이하게 하기 위한 여러 유틸리티 타입을 제공합니다. 이러한 유틸리티들은 전역적으로 사용할 수 있습니다.

## `Awaited<Type>`

> 릴리스: [4.5](/docs/handbook/release-notes/typescript-4-5.html#the-awaited-type-and-promise-improvements)

이 타입은 `async` 함수의 `await`나 `Promise`의 `.then()` 메서드와 같은 연산을 모델링하기 위한 것입니다 - 특히, `Promise`를 재귀적으로 언래핑하는 방식을 다룹니다.

##### 예제

```ts
type A = Awaited<Promise<string>>;
//   ^? type A = string

type B = Awaited<Promise<Promise<number>>>;
//   ^? type B = number

type C = Awaited<boolean | Promise<number>>;
//   ^? type C = number | boolean
```

## `Partial<Type>`

> 릴리스: [2.1](/docs/handbook/release-notes/typescript-2-1.html#partial-readonly-record-and-pick)

`Type`의 모든 프로퍼티를 선택적으로 설정한 타입을 생성합니다. 이 유틸리티는 주어진 타입의 모든 하위 집합을 나타내는 타입을 반환합니다.

##### 예제

```ts
interface Todo {
  title: string;
  description: string;
}

function updateTodo(todo: Todo, fieldsToUpdate: Partial<Todo>) {
  return { ...todo, ...fieldsToUpdate };
}

const todo1 = {
  title: "organize desk",
  description: "clear clutter",
};

const todo2 = updateTodo(todo1, {
  description: "throw out trash",
});
```

## `Required<Type>`

> 릴리스: [2.8](/docs/handbook/release-notes/typescript-2-8.html#improved-control-over-mapped-type-modifiers)

`Type`의 모든 프로퍼티를 필수로 설정한 타입을 생성합니다. [`Partial`](#partialtype)의 반대입니다.

##### 예제

```ts
// @errors: 2741
interface Props {
  a?: number;
  b?: string;
}

const obj: Props = { a: 5 };

const obj2: Required<Props> = { a: 5 };
```

## `Readonly<Type>`

> 릴리스: [2.1](/docs/handbook/release-notes/typescript-2-1.html#partial-readonly-record-and-pick)

`Type`의 모든 프로퍼티를 `readonly`로 설정한 타입을 생성합니다. 이는 생성된 타입의 프로퍼티를 재할당할 수 없음을 의미합니다.

##### 예제

```ts
// @errors: 2540
interface Todo {
  title: string;
}

const todo: Readonly<Todo> = {
  title: "Delete inactive users",
};

todo.title = "Hello";
```

이 유틸리티는 런타임에 실패할 할당 표현식을 표현하는 데 유용합니다 (예: [동결된 객체](https://developer.mozilla.org/docs/Web/JavaScript/Reference/Global_Objects/Object/freeze)의 프로퍼티를 재할당하려고 할 때).

##### `Object.freeze`

```ts
function freeze<Type>(obj: Type): Readonly<Type>;
```

## `Record<Keys, Type>`

> 릴리스: [2.1](/docs/handbook/release-notes/typescript-2-1.html#partial-readonly-record-and-pick)

프로퍼티 키가 `Keys`이고 프로퍼티 값이 `Type`인 객체 타입을 생성합니다. 이 유틸리티는 타입의 프로퍼티를 다른 타입으로 매핑하는 데 사용할 수 있습니다.

##### 예제

```ts
type CatName = "miffy" | "boris" | "mordred";

interface CatInfo {
  age: number;
  breed: string;
}

const cats: Record<CatName, CatInfo> = {
  miffy: { age: 10, breed: "Persian" },
  boris: { age: 5, breed: "Maine Coon" },
  mordred: { age: 16, breed: "British Shorthair" },
};

cats.boris;
// ^? const cats.boris: CatInfo
```

## `Pick<Type, Keys>`

> 릴리스: [2.1](/docs/handbook/release-notes/typescript-2-1.html#partial-readonly-record-and-pick)

`Type`에서 프로퍼티 `Keys`의 집합(문자열 리터럴 또는 문자열 리터럴의 유니온)을 선택하여 타입을 생성합니다.

##### 예제

```ts
interface Todo {
  title: string;
  description: string;
  completed: boolean;
}

type TodoPreview = Pick<Todo, "title" | "completed">;

const todo: TodoPreview = {
  title: "Clean room",
  completed: false,
};

todo;
// ^? const todo: TodoPreview
```

## `Omit<Type, Keys>`

> 릴리스: [3.5](/docs/handbook/release-notes/typescript-3-5.html#the-omit-helper-type)

`Type`에서 모든 프로퍼티를 선택한 다음 `Keys`(문자열 리터럴 또는 문자열 리터럴의 유니온)를 제거하여 타입을 생성합니다. [`Pick`](#picktype-keys)의 반대입니다.

##### 예제

```ts
interface Todo {
  title: string;
  description: string;
  completed: boolean;
  createdAt: number;
}

type TodoPreview = Omit<Todo, "description">;

const todo: TodoPreview = {
  title: "Clean room",
  completed: false,
  createdAt: 1615544252770,
};

todo;
// ^? const todo: TodoPreview

type TodoInfo = Omit<Todo, "completed" | "createdAt">;

const todoInfo: TodoInfo = {
  title: "Pick up kids",
  description: "Kindergarten closes at 5pm",
};

todoInfo;
// ^? const todoInfo: TodoInfo
```

## `Exclude<UnionType, ExcludedMembers>`

> 릴리스: [2.8](/docs/handbook/release-notes/typescript-2-8.html#predefined-conditional-types)

`UnionType`에서 `ExcludedMembers`에 할당 가능한 모든 유니온 멤버를 제외하여 타입을 생성합니다.

##### 예제

```ts
type T0 = Exclude<"a" | "b" | "c", "a">;
//    ^? type T0 = "b" | "c"
type T1 = Exclude<"a" | "b" | "c", "a" | "b">;
//    ^? type T1 = "c"
type T2 = Exclude<string | number | (() => void), Function>;
//    ^? type T2 = string | number

type Shape =
  | { kind: "circle"; radius: number }
  | { kind: "square"; x: number }
  | { kind: "triangle"; x: number; y: number };

type T3 = Exclude<Shape, { kind: "circle" }>
//    ^? type T3 = { kind: "square"; x: number } | { kind: "triangle"; x: number; y: number }
```

## `Extract<Type, Union>`

> 릴리스: [2.8](/docs/handbook/release-notes/typescript-2-8.html#predefined-conditional-types)

`Type`에서 `Union`에 할당 가능한 모든 유니온 멤버를 추출하여 타입을 생성합니다.

##### 예제

```ts
type T0 = Extract<"a" | "b" | "c", "a" | "f">;
//    ^? type T0 = "a"
type T1 = Extract<string | number | (() => void), Function>;
//    ^? type T1 = () => void

type Shape =
  | { kind: "circle"; radius: number }
  | { kind: "square"; x: number }
  | { kind: "triangle"; x: number; y: number };

type T2 = Extract<Shape, { kind: "circle" }>
//    ^? type T2 = { kind: "circle"; radius: number }
```

## `NonNullable<Type>`

> 릴리스: [2.8](/docs/handbook/release-notes/typescript-2-8.html#predefined-conditional-types)

`Type`에서 `null`과 `undefined`를 제외하여 타입을 생성합니다.

##### 예제

```ts
type T0 = NonNullable<string | number | undefined>;
//    ^? type T0 = string | number
type T1 = NonNullable<string[] | null | undefined>;
//    ^? type T1 = string[]
```

## `Parameters<Type>`

> 릴리스: [3.1](https://github.com/microsoft/TypeScript/pull/26243)

함수 타입 `Type`의 매개변수에 사용된 타입들로 튜플 타입을 생성합니다.

오버로드된 함수의 경우, 이것은 _마지막_ 시그니처의 매개변수가 됩니다; [조건부 타입 내에서 추론하기](/docs/handbook/2/conditional-types.html#inferring-within-conditional-types)를 참조하세요.

##### 예제

```ts
// @errors: 2344
declare function f1(arg: { a: number; b: string }): void;

type T0 = Parameters<() => string>;
//    ^? type T0 = []
type T1 = Parameters<(s: string) => void>;
//    ^? type T1 = [s: string]
type T2 = Parameters<<T>(arg: T) => T>;
//    ^? type T2 = [arg: unknown]
type T3 = Parameters<typeof f1>;
//    ^? type T3 = [arg: { a: number; b: string }]
type T4 = Parameters<any>;
//    ^? type T4 = unknown[]
type T5 = Parameters<never>;
//    ^? type T5 = never
type T6 = Parameters<string>;
//    ^? 오류
type T7 = Parameters<Function>;
//    ^? 오류
```

## `ConstructorParameters<Type>`

> 릴리스: [3.1](https://github.com/microsoft/TypeScript/pull/26243)

생성자 함수 타입의 타입들로 튜플 또는 배열 타입을 생성합니다. 모든 매개변수 타입을 가진 튜플 타입을 생성합니다 (또는 `Type`이 함수가 아닌 경우 `never` 타입).

##### 예제

```ts
// @errors: 2344
// @strict: false
type T0 = ConstructorParameters<ErrorConstructor>;
//    ^? type T0 = [message?: string]
type T1 = ConstructorParameters<FunctionConstructor>;
//    ^? type T1 = string[]
type T2 = ConstructorParameters<RegExpConstructor>;
//    ^? type T2 = [pattern: string | RegExp, flags?: string]
class C {
  constructor(a: number, b: string) {}
}
type T3 = ConstructorParameters<typeof C>;
//    ^? type T3 = [a: number, b: string]
type T4 = ConstructorParameters<any>;
//    ^? type T4 = unknown[]

type T5 = ConstructorParameters<Function>;
//    ^? 오류
```

## `ReturnType<Type>`

> 릴리스: [2.8](/docs/handbook/release-notes/typescript-2-8.html#predefined-conditional-types)

함수 `Type`의 반환 타입으로 구성된 타입을 생성합니다.

오버로드된 함수의 경우, 이것은 _마지막_ 시그니처의 반환 타입이 됩니다; [조건부 타입 내에서 추론하기](/docs/handbook/2/conditional-types.html#inferring-within-conditional-types)를 참조하세요.

##### 예제

```ts
// @errors: 2344 2344
declare function f1(): { a: number; b: string };

type T0 = ReturnType<() => string>;
//    ^? type T0 = string
type T1 = ReturnType<(s: string) => void>;
//    ^? type T1 = void
type T2 = ReturnType<<T>() => T>;
//    ^? type T2 = unknown
type T3 = ReturnType<<T extends U, U extends number[]>() => T>;
//    ^? type T3 = number[]
type T4 = ReturnType<typeof f1>;
//    ^? type T4 = { a: number; b: string }
type T5 = ReturnType<any>;
//    ^? type T5 = any
type T6 = ReturnType<never>;
//    ^? type T6 = never
type T7 = ReturnType<string>;
//    ^? 오류
type T8 = ReturnType<Function>;
//    ^? 오류
```

## `InstanceType<Type>`

> 릴리스: [2.8](/docs/handbook/release-notes/typescript-2-8.html#predefined-conditional-types)

`Type`에서 생성자 함수의 인스턴스 타입으로 구성된 타입을 생성합니다.

##### 예제

```ts
// @errors: 2344 2344
// @strict: false
class C {
  x = 0;
  y = 0;
}

type T0 = InstanceType<typeof C>;
//    ^? type T0 = C
type T1 = InstanceType<any>;
//    ^? type T1 = any
type T2 = InstanceType<never>;
//    ^? type T2 = never
type T3 = InstanceType<string>;
//    ^? 오류
type T4 = InstanceType<Function>;
//    ^? 오류
```

## `NoInfer<Type>`

> 릴리스: [5.4](/docs/handbook/release-notes/typescript-5-4.html#the-noinfer-utility-type)

포함된 타입에 대한 추론을 차단합니다. 추론을 차단하는 것 외에, `NoInfer<Type>`은 `Type`과 동일합니다.

##### 예제

```ts
function createStreetLight<C extends string>(
  colors: C[],
  defaultColor?: NoInfer<C>,
) {
  // ...
}

createStreetLight(["red", "yellow", "green"], "red");  // OK
createStreetLight(["red", "yellow", "green"], "blue");  // 오류
```

## `ThisParameterType<Type>`

> 릴리스: [3.3](https://github.com/microsoft/TypeScript/pull/28920)

함수 타입에 대한 [this](/docs/handbook/functions.html#this-parameters) 매개변수의 타입을 추출하거나, 함수 타입에 `this` 매개변수가 없는 경우 [unknown](/docs/handbook/release-notes/typescript-3-0.html#new-unknown-top-type)을 반환합니다.

##### 예제

```ts
function toHex(this: Number) {
  return this.toString(16);
}

function numberToString(n: ThisParameterType<typeof toHex>) {
  return toHex.apply(n);
}
```

## `OmitThisParameter<Type>`

> 릴리스: [3.3](https://github.com/microsoft/TypeScript/pull/28920)

`Type`에서 [`this`](/docs/handbook/functions.html#this-parameters) 매개변수를 제거합니다. `Type`에 명시적으로 선언된 `this` 매개변수가 없는 경우, 결과는 단순히 `Type`입니다. 그렇지 않으면, `Type`에서 `this` 매개변수가 없는 새로운 함수 타입이 생성됩니다. 제네릭은 지워지고 마지막 오버로드 시그니처만 새로운 함수 타입으로 전파됩니다.

##### 예제

```ts
function toHex(this: Number) {
  return this.toString(16);
}

const fiveToHex: OmitThisParameter<typeof toHex> = toHex.bind(5);

console.log(fiveToHex());
```

## `ThisType<Type>`

> 릴리스: [2.3](https://github.com/microsoft/TypeScript/pull/14141)

이 유틸리티는 변환된 타입을 반환하지 않습니다. 대신, 컨텍스트적 [`this`](/docs/handbook/functions.html#this) 타입의 마커 역할을 합니다. 이 유틸리티를 사용하려면 [`noImplicitThis`](/tsconfig#noImplicitThis) 플래그가 활성화되어야 합니다.

##### 예제

```ts
// @noImplicitThis: true
type ObjectDescriptor<D, M> = {
  data?: D;
  methods?: M & ThisType<D & M>; // methods에서 'this'의 타입은 D & M입니다
};

function makeObject<D, M>(desc: ObjectDescriptor<D, M>): D & M {
  let data: object = desc.data || {};
  let methods: object = desc.methods || {};
  return { ...data, ...methods } as D & M;
}

let obj = makeObject({
  data: { x: 0, y: 0 },
  methods: {
    moveBy(dx: number, dy: number) {
      this.x += dx; // 강력하게 타입이 지정된 this
      this.y += dy; // 강력하게 타입이 지정된 this
    },
  },
});

obj.x = 10;
obj.y = 20;
obj.moveBy(5, 5);
```

위의 예제에서, `makeObject`에 대한 인수의 `methods` 객체는 `ThisType<D & M>`을 포함하는 컨텍스트적 타입을 가지므로, `methods` 객체 내의 메서드에서 [this](/docs/handbook/functions.html#this)의 타입은 `{ x: number, y: number } & { moveBy(dx: number, dy: number): void }`입니다. `methods` 프로퍼티의 타입이 동시에 추론 대상이자 메서드 내의 `this` 타입의 소스임을 주목하세요.

`ThisType<T>` 마커 인터페이스는 단순히 `lib.d.ts`에 선언된 빈 인터페이스입니다. 객체 리터럴의 컨텍스트적 타입에서 인식되는 것 외에, 이 인터페이스는 다른 빈 인터페이스처럼 동작합니다.

## 내장 문자열 조작 타입

### `Uppercase<StringType>`

### `Lowercase<StringType>`

### `Capitalize<StringType>`

### `Uncapitalize<StringType>`

템플릿 문자열 리터럴을 둘러싼 문자열 조작을 돕기 위해, TypeScript는 타입 시스템 내에서 문자열 조작에 사용할 수 있는 타입 집합을 포함합니다. 이러한 타입은 [템플릿 리터럴 타입](/docs/handbook/2/template-literal-types.html#uppercasestringtype) 문서에서 찾을 수 있습니다.

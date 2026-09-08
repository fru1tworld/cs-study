# Zod 메타데이터와 JSON Schema

> 원문: https://zod.dev/metadata
> 원문: https://zod.dev/json-schema

스키마에는 검증 규칙 말고도 추가 정보를 붙여 둘 수 있다. 이런 정보를 메타데이터(metadata)라고 한다. 메타데이터는 스키마가 검사하는 값이 아니라 스키마 자체에 대한 설명으로, 필드의 제목이나 설명문, 예시 값 같은 것이 여기에 들어간다. 메타데이터를 붙여 두면 문서화, 코드 생성, 폼 검증, AI 구조화 출력(structured output, AI 모델이 정해진 JSON 형식에 맞춰 답하게 하는 기능) 같은 곳에 활용할 수 있다.

이 문서는 Zod 4 (4.6.5 기준)를 대상으로 먼저 메타데이터를 담아 두는 레지스트리(registry)를 설명한다. 이어서 레지스트리의 메타데이터를 반영해 Zod 스키마를 [JSON Schema](#json-schema-개요-json-schema)로 바꾸는 `z.toJSONSchema()`와, 그 반대 방향인 `z.fromJSONSchema()`를 살펴본다.

## 레지스트리 (Registries)

Zod는 메타데이터를 레지스트리에 담는다. 레지스트리는 스키마를 모아 두는 컬렉션인데, 스키마마다 메타데이터를 하나씩 짝지어 저장한다는 점에서 스키마를 키로 쓰는 맵(Map)에 가깝다. 레지스트리를 만들 때 메타데이터의 타입을 정해 두면, 등록하는 메타데이터가 그 타입을 따르는지 엄격하게 검사된다. 다음은 `description` 문자열을 메타데이터로 받는 레지스트리다.

```ts
import * as z from "zod";

const myRegistry = z.registry<{ description: string }>();
```

레지스트리에 스키마를 등록하고, 조회하고, 제거하는 방법은 다음과 같다.

```ts
const mySchema = z.string();

myRegistry.add(mySchema, { description: "A cool schema!"});
myRegistry.has(mySchema); // => true
myRegistry.get(mySchema); // => { description: "A cool schema!" }
myRegistry.remove(mySchema);
myRegistry.clear(); // 레지스트리 전체 비우기
```

이때 TypeScript는 각 스키마의 메타데이터가 레지스트리를 만들 때 지정한 메타데이터 타입과 일치하는지 검사한다.

```ts
myRegistry.add(mySchema, { description: "A cool schema!" }); // ✅
myRegistry.add(mySchema, { description: 123 }); // ❌
```

> **주의:** Zod 레지스트리는 `id` 속성을 특별하게 취급한다. 같은 `id` 값으로 여러 스키마를 등록하면 `Error`가 발생한다. 전역 레지스트리를 포함한 모든 레지스트리가 그렇다. `id`는 뒤에서 JSON Schema로 변환할 때 스키마를 구별하는 이름으로 쓰인다([「레지스트리로 여러 스키마 변환하기」](#레지스트리로-여러-스키마-변환하기-registries) 참고).

메타데이터 타입을 지정하지 않고 레지스트리를 만들면, 메타데이터 없이 스키마만 모아 두는 범용 컬렉션으로 쓸 수 있다.

```ts
const myRegistry = z.registry();

myRegistry.add(z.string());
myRegistry.add(z.number());
```

### `.register()`

레지스트리의 `.add()` 대신 스키마의 `.register()` 메서드를 호출해도 스키마가 등록된다.

```ts
const mySchema = z.string();

mySchema.register(myRegistry, { description: "A cool schema!" });
// => mySchema
```

> **참고:** 이 메서드는 새 스키마가 아니라 원래 스키마를 그대로 반환한다. Zod 메서드 가운데 이렇게 동작하는 것은 이것 하나뿐이고, 아래에서 설명하는 `.meta()`와 `.describe()`를 포함한 나머지 메서드는 새 인스턴스를 반환한다.

원래 스키마가 그대로 돌아오므로 스키마를 정의하는 자리에서 메타데이터도 바로 붙일 수 있다.

```ts
const mySchema = z.object({
  name: z.string().register(myRegistry, { description: "The user's name" }),
  age: z.number().register(myRegistry, { description: "The user's age" }),
})
```

## 메타데이터 (Metadata)

### `z.globalRegistry`

레지스트리를 매번 직접 만들지 않아도 되도록 Zod는 전역 레지스트리 `z.globalRegistry`를 기본으로 제공한다. JSON Schema 생성 등에 쓸 메타데이터를 담아 두는 곳으로, `z.toJSONSchema()`도 따로 지정하지 않으면 이 레지스트리에서 메타데이터를 읽는다([`metadata`](#metadata) 참고). 전역 레지스트리가 받는 메타데이터 타입은 다음과 같다.

```ts
export interface GlobalMeta {
  id?: string ;
  title?: string ;
  description?: string;
  deprecated?: boolean;
  [k: string]: unknown;
}
```

마지막 줄의 인덱스 시그니처(`[k: string]: unknown`) 덕분에 인터페이스에 없는 필드도 넣을 수 있다. 다음 예의 `examples`가 그런 필드다.

```ts
import * as z from "zod";

const emailSchema = z.email().register(z.globalRegistry, { 
  id: "email_address",
  title: "Email address",
  description: "Your email address",
  examples: ["first.last@example.com"]
});
```

다만 인덱스 시그니처로 들어온 필드는 타입이 `unknown`이다. 이런 필드의 타입을 정해 두려면 `GlobalMeta` 인터페이스를 전역으로 확장한다. 이때 쓰는 기능이 [선언 병합(declaration merging)](https://www.typescriptlang.org/docs/handbook/declaration-merging.html)으로, 같은 이름의 인터페이스를 다시 선언하면 TypeScript가 두 선언의 필드를 하나로 합친다. 다음 코드처럼 `declare module "zod"` 블록 안에서 `GlobalMeta`를 다시 선언하면 Zod의 `GlobalMeta`에 필드가 추가된다. 이 코드는 코드베이스 어디에 두어도 되지만, 프로젝트 루트에 `zod.d.ts` 파일을 만들어 두는 것이 일반적인 관례다.

```ts
declare module "zod" {
  interface GlobalMeta {
    // 새 필드를 여기에 추가
    examples?: unknown[];
  }
}

// TypeScript가 이 파일을 모듈로 인식하게 한다
export {}
```

### `.meta()`

`.meta()` 메서드는 스키마를 `z.globalRegistry`에 등록하는 더 간편한 방법이다.

```ts
const emailSchema = z.email().meta({ 
  id: "email_address",
  title: "Email address",
  description: "Please enter a valid email address",
});
```

[Zod Mini](07_packages_and_compile.md#zod-mini)(같은 기능을 함수형 API로 제공하는 경량 패키지)에는 `.meta()` 메서드가 없으므로, `.check()`에 `z.meta()`를 넘긴다.

```ts
// Zod Mini
const emailSchema = z.email().check(
  z.meta({ 
    id: "email_address",
    title: "Email address",
    description: "Please enter a valid email address",
  })
);
```

인자 없이 `.meta()`를 호출하면 스키마의 메타데이터를 조회한다.

```ts
emailSchema.meta();
// => { id: "email_address", title: "Email address", ... }
```

메타데이터는 등록한 그 스키마 인스턴스에만 연결된다. 그런데 Zod 메서드는 불변(immutable)이어서 기존 스키마를 고치지 않고 항상 새 인스턴스를 반환한다(앞에서 본 `.register()`만 예외다). 그래서 다음 예처럼 `A`에 [`.refine()`](04_refinements_transforms_codecs.md#refine)을 호출해 만든 `B`는 `A`와 다른 인스턴스이고, `B`에서는 `A`의 메타데이터를 조회할 수 없다.

```ts
const A = z.string().meta({ description: "A cool string" });
A.meta(); // => { description: "A cool string" }

const B = A.refine(_ => true);
B.meta(); // => undefined
```

### `.describe()`

> **참고:** `.describe()` 메서드는 계속 사용할 수 있지만 `.meta()` 사용을 권장한다.

`.describe()`는 `description` 필드 하나만으로 스키마를 `z.globalRegistry`에 등록하는 축약형이다.

```ts
const emailSchema = z.email();
emailSchema.describe("An email address");

// 다음과 같다
emailSchema.meta({ description: "An email address" });
```

```ts
// Zod Mini
const emailSchema = z.email().check(z.describe("An email address"));

// 다음과 같다
z.email().check(z.meta({ description: "An email address" }));
```

## 커스텀 레지스트리 (Custom registries)

`z.registry<{ description: string }>()`로 만드는 간단한 커스텀 레지스트리는 [앞 절](#레지스트리-registries)에서 이미 보았다. 여기서는 좀 더 발전된 패턴을 살펴본다.

### 추론된 타입 참조하기 (Referencing inferred types)

메타데이터 타입이 스키마에서 추론된 타입([「타입 추론」](01_intro_and_basics.md#타입-추론-inferring-types) 참고)을 참조하게 만들면 유용할 때가 많다. 예를 들어 `examples` 필드에 스키마 출력값의 예시를 담는다면, 문자열 스키마에는 문자열 예시가, 숫자 스키마에는 숫자 예시가 들어가야 한다.

```ts
import * as z from "zod";

type MyMeta = { examples: z.$output[] };
const myRegistry = z.registry<MyMeta>();

myRegistry.add(z.string(), { examples: ["hello", "world"] });
myRegistry.add(z.number(), { examples: [1, 2, 3] });
```

특수 심볼 `z.$output`은 등록하는 스키마의 추론된 출력 타입(`z.infer<typeof schema>`)을 가리킨다. 그래서 위 예에서 `z.string()`에는 문자열 배열을, `z.number()`에는 숫자 배열을 `examples`로 넘긴다. 마찬가지로 `z.$input`은 입력 타입을 가리킨다.

### 스키마 타입 제한하기 (Constraining schema types)

`z.registry()`에 두 번째 제네릭 타입 인자를 넘기면 레지스트리에 추가할 수 있는 스키마 타입이 제한된다. 다음 레지스트리는 문자열 스키마만 받는다.

```ts
import * as z from "zod";

const myRegistry = z.registry<{ description: string }, z.ZodString>();

myRegistry.add(z.string(), { description: "A number" }); // ✅
myRegistry.add(z.number(), { description: "A number" }); // ❌ 
//             ^ 'ZodNumber' is not assignable to parameter of type 'ZodString' 
```

## JSON Schema 개요 (JSON Schema)

[JSON Schema](https://json-schema.org/)는 JSON 데이터가 어떤 모양이어야 하는지를 JSON으로 기술하는 표준이다. 예를 들어 `{ "type": "string" }`은 값이 문자열이어야 한다는 뜻이다. 객체라면 `properties`에 속성별 스키마를, `required`에 필수 속성 목록을 적는다. 이 문서에서는 `type`, `properties`처럼 JSON Schema 객체에 쓰는 속성 이름을 키워드(keyword)라고 부른다.

JSON Schema는 특정 언어에 묶이지 않는 형식이라 [OpenAPI](https://www.openapis.org/)(HTTP API 명세를 기술하는 표준) 정의나 AI의 [구조화 출력](https://platform.openai.com/docs/guides/structured-outputs?api-mode=chat)을 정의하는 데 널리 쓰인다. Zod는 `zod@4.0`부터 Zod 스키마와 JSON Schema 사이의 변환을 기본으로 지원한다.

## `z.fromJSONSchema()`

> **주의:** `z.fromJSONSchema()`는 실험적(experimental) 기능이며 Zod의 안정 API에 속하지 않는다. 이후 릴리스에서 구현이 바뀔 가능성이 크다.

`z.fromJSONSchema()`는 JSON Schema를 Zod 스키마로 변환한다.

```ts
import * as z from "zod";

const jsonSchema = {
  type: "object",
  properties: {
    name: { type: "string" },
    age: { type: "number" },
  },
  required: ["name", "age"],
};

const zodSchema = z.fromJSONSchema(jsonSchema);
```

## `z.toJSONSchema()`

Zod 스키마를 JSON Schema로 변환하려면 `z.toJSONSchema()` 함수를 사용한다.

```ts
import * as z from "zod";

const schema = z.object({
  name: z.string(),
  age: z.number(),
});

z.toJSONSchema(schema)
// => {
//   type: 'object',
//   properties: { name: { type: 'string' }, age: { type: 'number' } },
//   required: [ 'name', 'age' ],
//   additionalProperties: false,
// }
```

모든 스키마와 검사(check)는 가장 가까운 JSON Schema 표현으로 변환된다. 검사는 `z.string().min(5)`의 `.min(5)`처럼 추론된 타입은 그대로 두고 값에 조건만 더하는 제약을 말한다([「검사」](07_packages_and_compile.md#검사-checks) 참고). 다만 JSON Schema에 대응하는 개념이 없어 합리적으로 표현할 수 없는 타입도 있다. 그런 타입의 목록과 처리 방법은 [`unrepresentable`](#unrepresentable) 절에서 다룬다.

두 번째 인자에 옵션 객체를 넘기면 변환 방식을 조정할 수 있다.

```ts
z.toJSONSchema(schema, {
  // ...params
})
```

지원하는 파라미터를 한눈에 보면 다음과 같다. 각 파라미터는 아래에서 자세히 설명한다.

```ts
interface ToJSONSchemaParams {
  /** 대상 JSON Schema 버전
   * - `"draft-2020-12"` — 기본값. JSON Schema Draft 2020-12
   * - `"draft-07"` — JSON Schema Draft 7
   * - `"draft-04"` — JSON Schema Draft 4
   * - `"openapi-3.0"` — OpenAPI 3.0 Schema Object */
  target?:
    | "draft-04"
    | "draft-4"
    | "draft-07"
    | "draft-7"
    | "draft-2020-12"
    | "openapi-3.0"
    | ({} & string)
    | undefined;

  /** 각 스키마의 메타데이터를 조회할 레지스트리.
   * `id` 속성이 있는 스키마는 $def로 추출된다. */
  metadata?: $ZodRegistry<Record<string, any>>;

  /** 표현할 수 없는 타입의 처리 방식
   * - `"throw"` — 기본값. 표현할 수 없는 타입이 있으면 에러를 던진다
   * - `"any"` — 표현할 수 없는 타입은 `{}`가 된다
   * - 함수 — 사용할 JSON Schema 또는 `"any"`/`"throw"`를 반환한다 */
  unrepresentable?:
    | "throw"
    | "any"
    | ((ctx: {
        zodSchema: $ZodTypes;
        path: (string | number)[];
        message: string;
      }) => JSONSchema | "throw" | "any" | undefined);

  /** 순환 참조 처리 방식
   * - `"ref"` — 기본값. $defs로 순환을 끊는다
   * - `"throw"` — 순환을 만나면 에러를 던진다 */
  cycles?: "ref" | "throw";

  /* 재사용된 스키마 처리 방식
   * - `"inline"` — 기본값. 재사용된 스키마를 인라인한다
   * - `"ref"` — 재사용된 스키마를 $defs로 추출한다 */
  reused?: "ref" | "inline";

  /** *외부* $ref에 쓸 URI로 `id` 값을 변환하는 함수
   *
   * 기본값은 `(id) => id`다.
   */
  uri?: (id: string) => string;
}
```

### `io`

스키마 중에는 파싱할 때 받는 값(입력)과 돌려주는 값(출력)의 타입이 다른 것이 있다. [파이프](04_refinements_transforms_codecs.md#파이프-pipes)로 이은 `ZodPipe`, [기본값](04_refinements_transforms_codecs.md#기본값-defaults)을 채우는 `ZodDefault`, [강제 변환(coercion)](02_schemas_primitives.md#강제-변환-coercion)을 거치는 원시 타입이 그렇다. 이런 스키마를 변환하면 `z.toJSONSchema`의 결과는 기본적으로 출력 타입을 나타낸다. 입력 타입을 추출하려면 `"io": "input"`을 지정한다.

```ts
const mySchema = z.string().transform(val => val.length).pipe(z.number());
// ZodPipe

const jsonSchema = z.toJSONSchema(mySchema); 
// => { type: "number" }

const jsonSchema = z.toJSONSchema(mySchema, { io: "input" }); 
// => { type: "string" }
```

### `target`

JSON Schema 명세는 버전마다 Draft 7, Draft 2020-12처럼 draft 이름으로 불리며, 버전에 따라 쓸 수 있는 키워드가 조금씩 다르다. 결과를 넘길 도구가 요구하는 버전은 `target` 파라미터로 지정한다. 기본값은 Draft 2020-12다.

```ts
z.toJSONSchema(schema, { target: "draft-07" });
z.toJSONSchema(schema, { target: "draft-2020-12" });
z.toJSONSchema(schema, { target: "draft-04" });
z.toJSONSchema(schema, { target: "openapi-3.0" });
```

### `metadata`

Zod의 메타데이터는 레지스트리에 저장된다([「레지스트리 (Registries)」](#레지스트리-registries), [「메타데이터 (Metadata)」](#메타데이터-metadata) 참고). `id`, `title`, `description`, `examples` 같은 공통 메타데이터 필드는 전역 레지스트리 `z.globalRegistry`에 담고, `z.toJSONSchema()`는 기본적으로 이 레지스트리의 메타데이터를 결과에 반영한다. `metadata` 옵션에 다른 레지스트리를 넘기면 그 레지스트리를 대신 읽는다.

```ts
import * as z from "zod";

// `.meta()`는 스키마를 `z.globalRegistry`에 등록하는 편의 메서드다
const emailSchema = z.string().meta({ 
  title: "Email address",
  description: "Your email address",
});

z.toJSONSchema(emailSchema);
// => { type: "string", title: "Email address", description: "Your email address", ... } 
```

```ts
// Zod Mini
import * as z from "zod";

// `.meta()`는 스키마를 `z.globalRegistry`에 등록하는 편의 메서드다
const emailSchema = z.string().register(z.globalRegistry, { 
  title: "Email address",
  description: "Your email address",
});

z.toJSONSchema(emailSchema);
// => { type: "string", title: "Email address", description: "Your email address", ... } 
```

모든 메타데이터 필드는 결과 JSON Schema에 그대로 복사된다.

```ts
const schema = z.string().meta({
  whatever: 1234
});

z.toJSONSchema(schema);
// => { type: "string", whatever: 1234 }
```

메타데이터의 필드가 Zod가 생성한 키워드와 겹치면 메타데이터 쪽이 우선한다.

```ts
z.toJSONSchema(z.string().meta({ type: "number" }));
// => { type: "number" }

z.toJSONSchema(z.date().meta({ type: "string", format: "date-time" }), { unrepresentable: "any" });
// => { type: "string", format: "date-time" }
```

메타데이터를 모두 빼려면 `z.toJSONSchema(schema, { metadata: z.registry() })`처럼 빈 레지스트리를 넘긴다. 생성된 스키마를 수정하려면 [`override`](#override)를 사용한다.

### `unrepresentable`

다음 API는 JSON Schema로 표현할 수 없다. JSON에는 이들에 대응하는 개념이 없으므로 JSON Schema로 바꾸려는 시도 자체가 무리다. 이런 경우에는 스키마 쪽을 고치는 편이 맞다.

```ts
z.bigint(); // ❌
z.int64(); // ❌
z.symbol(); // ❌
z.undefined(); // ❌
z.void(); // ❌
z.date(); // ❌
z.map(); // ❌
z.set(); // ❌
z.transform(); // ❌
z.nan(); // ❌
z.custom(); // ❌
z.number().multipleOf(0); // ❌ 제수(divisor)는 유한하고 0이 아니어야 한다
```

기본적으로 이런 타입을 만나면 Zod는 에러를 던진다.

```ts
z.toJSONSchema(z.bigint());
// => throws Error
```

`unrepresentable` 옵션을 `"any"`로 설정하면 이 동작을 바꿀 수 있다. 표현할 수 없는 타입은 `{}`(아무 값이나 허용하는 JSON Schema로, TypeScript의 `unknown`에 해당)로 변환되고, 표현할 수 없는 검사는 그 검사를 가진 스키마에서 제거된다.

```ts
z.toJSONSchema(z.bigint(), { unrepresentable: "any" });
// => {}

z.toJSONSchema(z.number().multipleOf(0), { unrepresentable: "any" });
// => { type: "number" }
```

경우마다 다르게 처리하려면 함수(핸들러)를 넘긴다. Zod는 표현할 수 없는 스키마를 만날 때마다 이 핸들러를 호출하고, 핸들러는 대신 사용할 JSON Schema나 `"any"`, `"throw"`를 반환한다. 아무것도 반환하지 않으면 `"throw"`로 처리된다. 다음 예에서는 `z.date()`만 날짜 문자열로 바꾸고 `z.bigint()`는 그대로 에러를 내게 했다.

```ts
z.toJSONSchema(z.object({ createdAt: z.date(), id: z.bigint() }), {
  unrepresentable: ({ zodSchema }) =>
    zodSchema._zod.def.type === "date" ? { type: "string", format: "date-time" } : "throw",
});
// => throws Error (BigInt cannot be represented in JSON Schema)
```

핸들러는 해당 스키마의 위치(`path`)와, 핸들러가 없었다면 Zod가 던졌을 에러의 `message`도 받는다. 핸들러가 던진 에러는 그대로 전파되므로, 표현할 수 없는 타입을 원하는 문구로 보고할 수 있다.

```ts
z.toJSONSchema(schema, {
  unrepresentable: ({ path, message }) => {
    throw new Error(`${message} (at /${path.join("/")})`);
  },
});
```

표현할 수 없는 원인이 서로 달라도 같은 `zodSchema`로 들어올 때가 있다. 예를 들어 한 리터럴 안의 `undefined` 멤버와 `bigint` 멤버는 둘 다 그 리터럴 스키마로 전달된다. 이때도 `message`는 원인마다 다르므로, `message`로 분기해 구분한다.

JSON으로 직렬화(serialize, 값을 JSON 문자열로 바꾸는 것)할 수 없는 기본값(default value)도 같은 핸들러를 거치며, 이때는 `.default()` 스키마가 `zodSchema`로 전달된다. `"any"`이면 기본값만 빠지고 나머지 스키마는 평소대로 출력된다. 직접 JSON Schema를 반환해 기본값을 지정할 수도 있다.

```ts
z.toJSONSchema(z.bigint().default(0n), {
  unrepresentable: ({ zodSchema }) => {
    const def = zodSchema._zod.def;
    if (def.type === "bigint") return { type: "integer", format: "int64" };
    if (def.type === "default") return { default: String(def.defaultValue) };
    return "throw";
  },
});
// => { type: "integer", format: "int64", default: "0" }
```

리터럴에 대해 JSON Schema를 반환하면 리터럴 전체가 교체되어 표현 가능한 멤버까지 사라진다. `z.literal(["a", 1n])`은 반환한 스키마만 남고 `"a"`는 없어진다. 기존처럼 값 단위로 처리하려면 `"any"`를 반환한다.

### `cycles`

스키마가 자기 자신을 참조하면 순환 참조(cycle)가 생긴다. `cycles`는 이런 순환을 처리하는 방식을 정한다. 기본적으로 `z.toJSONSchema()`는 스키마를 순회하다 순환을 만나면 그 자리를 `$ref`로 표현한다. `$ref`는 다른 곳에 정의된 스키마를 가리키는 JSON Schema 키워드로, 값에 가리킬 위치를 적는다. 아래 결과에서 `friend`의 `'#'`은 문서의 루트, 즉 `User` 스키마 자신을 가리킨다. 게터를 이용한 재귀 스키마 정의는 [「재귀 객체」](03_schemas_objects_collections.md#재귀-객체-recursive-objects)를 참고한다.

```ts
const User = z.object({
  name: z.string(),
  get friend() {
    return User;
  },
});

z.toJSONSchema(User);
// => {
//   type: 'object',
//   properties: { name: { type: 'string' }, friend: { '$ref': '#' } },
//   required: [ 'name', 'friend' ],
//   additionalProperties: false,
// }
```

대신 에러를 던지게 하려면 `cycles` 옵션을 `"throw"`로 설정한다.

```ts
z.toJSONSchema(User, { cycles: "throw" });
// => throws Error
```

### `reused`

한 스키마 안에서 여러 번 등장하는 스키마의 처리 방식이다. 기본적으로 Zod는 이런 스키마를 인라인(inline)한다. 즉 같은 정의를 쓰이는 자리마다 그대로 복사해 넣는다.

```ts
const name = z.string();
const User = z.object({
  firstName: name,
  lastName: name,
});

z.toJSONSchema(User);
// => {
//   type: 'object',
//   properties: { 
//     firstName: { type: 'string' }, 
//     lastName: { type: 'string' } 
//   },
//   required: [ 'firstName', 'lastName' ],
//   additionalProperties: false,
// }
```

`reused` 옵션을 `"ref"`로 설정하면 이런 스키마를 `$defs`로 추출한다. `$defs`는 여러 곳에서 참조할 스키마 정의를 모아 두는 키워드다. 정의는 `$defs`에 한 번만 두고, 쓰이는 자리에는 그 정의를 가리키는 [`$ref`](#cycles)만 남는다. 아래 결과에서 `firstName`과 `lastName`은 둘 다 `#/$defs/__schema0`를 참조한다.

```ts
z.toJSONSchema(User, { reused: "ref" });
// => {
//   type: 'object',
//   properties: {
//     firstName: { '$ref': '#/$defs/__schema0' },
//     lastName: { '$ref': '#/$defs/__schema0' }
//   },
//   required: [ 'firstName', 'lastName' ],
//   additionalProperties: false,
//   '$defs': { __schema0: { type: 'string' } }
// }
```

### `override`

Zod가 생성한 결과를 원하는 대로 고치려면 `override`에 콜백을 넘긴다. 콜백은 `ctx`로 원래 Zod 스키마와 기본 생성된 JSON Schema를 받으며, 결과를 바꾸려면 `ctx.jsonSchema`를 직접 수정해야 한다.

```ts
const mySchema = /* ... */
z.toJSONSchema(mySchema, {
  override: (ctx)=>{
    ctx.zodSchema; // 원래 Zod 스키마
    ctx.jsonSchema; // 기본 생성된 JSON Schema

    // 직접 수정한다
    ctx.jsonSchema.whatever = "sup";
  }
});
```

주의할 점은 표현할 수 없는 타입이 `override`가 호출되기 전에 이미 `Error`를 던진다는 것이다. 그래서 그런 타입은 `override`만으로는 고칠 수 없고 [`unrepresentable`](#unrepresentable)을 함께 써야 한다. `unrepresentable`에 핸들러를 넘기면 다른 타입의 에러는 그대로 둔 채 특정 타입만 대체할 수 있다. 다음 예처럼 `override`와 함께 `unrepresentable: "any"`를 설정해도 동작하지만, 이렇게 하면 표현할 수 없는 타입이 에러 없이 모두 지워진다.

```ts
// z.date()를 ISO datetime 문자열로 지원한다
const result = z.toJSONSchema(z.date(), {
  unrepresentable: "any",
  override: (ctx) => {
    const def = ctx.zodSchema._zod.def;
    if(def.type ==="date"){
      ctx.jsonSchema.type = "string";
      ctx.jsonSchema.format = "date-time";
    }
  },
});
```

## 변환 규칙 (Conversion)

Zod가 JSON Schema로 변환할 때 적용하는 세부 규칙은 다음과 같다. 각 스키마 자체의 설명은 [「Zod 스키마 정의 1: 원시 타입과 문자열」](02_schemas_primitives.md)과 [「Zod 스키마 정의 2: 객체와 컬렉션」](03_schemas_objects_collections.md)을 참고한다.

### 문자열 포맷 (String formats)

JSON Schema에서 문자열의 형식을 제한하는 방법은 몇 가지가 있다. `format`은 `"email"`처럼 미리 정해진 형식을 이름으로 지정하는 키워드이고, `pattern`은 정규 표현식으로 지정하는 키워드다. 다음 스키마는 대응하는 `format`으로 변환된다.

```ts
// `format`으로 지원
z.email(); // => { type: "string", format: "email" }
z.iso.datetime(); // => { type: "string", format: "date-time" }
z.iso.date(); // => { type: "string", format: "date" }
z.iso.duration(); // => { type: "string", format: "duration" }
z.ipv4(); // => { type: "string", format: "ipv4" }
z.ipv6(); // => { type: "string", format: "ipv6" }
z.uuid(); // => { type: "string", format: "uuid" }
z.guid(); // => { type: "string", format: "uuid" }
z.url(); // => { type: "string", format: "uri" }
```

`z.iso.datetime()`의 `local: true`와 `precision: -1` 변형은 `pattern`으로 대체된다. `date-time` 포맷이 따르는 RFC 3339의 `date-time`은 오프셋과 초를 모두 요구하기 때문이다.

다음 스키마는 문자열이 어떤 인코딩으로 된 데이터인지 나타내는 `contentEncoding`으로 지원한다.

```ts
z.base64(); // => { type: "string", contentEncoding: "base64" }
```

그 밖의 문자열 포맷은 모두 `pattern`으로 지원한다.

```ts
z.iso.time();
z.base64url();
z.cuid();
z.emoji();
z.nanoid();
z.cuid2();
z.ulid();
z.cidrv4();
z.cidrv6();
z.mac();
z.currencyCode();
```

### 숫자 타입 (Numeric types)

숫자 타입은 다음과 같이 변환된다.

```ts
// number
z.number(); // => { type: "number" }
z.float32(); // => { type: "number", exclusiveMinimum: ..., exclusiveMaximum: ... }
z.float64(); // => { type: "number", exclusiveMinimum: ..., exclusiveMaximum: ... }

// integer
z.int(); // => { type: "integer" }
z.int32(); // => { type: "integer", exclusiveMinimum: ..., exclusiveMaximum: ... }
```

### 객체 스키마 (Object schemas)

기본적으로 `z.object()` 스키마에는 정의하지 않은 속성(추가 속성)을 허용하지 않는다는 뜻의 `additionalProperties: false`가 붙는다. 일반 `z.object()` 스키마는 추가 속성을 파싱 결과에서 제거(strip)하므로([「객체」](03_schemas_objects_collections.md#객체-objects) 참고), 출력에는 추가 속성이 남지 않는다. 이 설정은 그 동작을 정확히 반영한다.

```ts
import * as z from "zod";

const schema = z.object({
  name: z.string(),
  age: z.number(),
});

z.toJSONSchema(schema)
// => {
//   type: 'object',
//   properties: { name: { type: 'string' }, age: { type: 'number' } },
//   required: [ 'name', 'age' ],
//   additionalProperties: false,
// }
```

`"input"` 모드로 변환하면 `additionalProperties`를 설정하지 않는다. 자세한 내용은 [`io`](#io) 절을 참고한다.

```ts
import * as z from "zod";

const schema = z.object({
  name: z.string(),
  age: z.number(),
});

z.toJSONSchema(schema, { io: "input" });
// => {
//   type: 'object',
//   properties: { name: { type: 'string' }, age: { type: 'number' } },
//   required: [ 'name', 'age' ],
// }
```

반면 다음 두 스키마는 모드와 관계없이 동작이 정해져 있다.

- `z.looseObject()`: `additionalProperties: false`를 절대 설정하지 않음
- `z.strictObject()`: `additionalProperties: false`를 항상 설정함

### 파일 스키마 (File schemas)

`z.file()`은 OpenAPI 친화적인 다음 스키마로 변환된다.

```ts
z.file();
// => { type: "string", format: "binary", contentEncoding: "binary" }
```

크기와 MIME 검사도 함께 표현된다.

```ts
z.file().min(1).max(1024 * 1024).mime("image/png");
// => {
//   type: "string",
//   format: "binary",
//   contentEncoding: "binary",
//   contentMediaType: "image/png",
//   minLength: 1,
//   maxLength: 1048576,
// }
```

### null 허용 (Nullability)

`z.null()`은 JSON Schema에서 `{ type: "null" }`로 변환된다.

```ts
z.null();
// => { type: "null" }
```

`z.undefined()`는 JSON Schema로 표현할 수 없다는 점에 주의한다([`unrepresentable`](#unrepresentable) 참고).

`nullable`은 내부 스키마의 허용 타입 집합(`type`)에 `"null"`을 추가한다. 아래 두 번째 예의 `.min(5)`처럼 다른 키워드가 붙어 단순한 `type`으로 쓸 수 없으면, 나열한 스키마 중 하나만 만족하면 된다는 뜻의 `anyOf`로 대체된다.

```ts
z.nullable(z.string());
// => { type: ["string", "null"] }

z.nullable(z.string().min(5));
// => { anyOf: [{ type: "string", minLength: 5 }, { type: "null" }] }
```

optional 스키마는 그대로 표현되며, `optional` 표시만 덧붙는다.

```ts
z.optional(z.string());
// => { type: "string" }
```

## 레지스트리로 여러 스키마 변환하기 (Registries)

스키마 하나를 `z.toJSONSchema()`에 넘기면 자기 완결적인(self-contained) JSON Schema, 즉 필요한 정의를 모두 안에 담아 다른 문서를 참조하지 않는 JSON Schema가 반환된다. 그런데 여러 Zod 스키마를 서로 참조하는 여러 개의 JSON Schema로 나누고 싶을 때도 있다. 예를 들어 각각을 `.json` 파일로 저장해 웹 서버에서 제공하는 경우다. 다음 예의 `User`와 `Post`는 서로를 참조하며, 각각 `id`와 함께 전역 레지스트리에 등록되어 있다.

```ts
import * as z from "zod";

const User = z.object({
  name: z.string(),
  get posts(){
    return z.array(Post);
  }
});

const Post = z.object({
  title: z.string(),
  content: z.string(),
  get author(){
    return User;
  }
});

z.globalRegistry.add(User, {id: "User"});
z.globalRegistry.add(Post, {id: "Post"});
```

이럴 때는 `z.toJSONSchema()`에 스키마 대신 [레지스트리](#레지스트리-registries)를 넘긴다. 그러면 `id`가 있는 스키마마다 JSON Schema가 하나씩 만들어지고, 다른 스키마를 가리키는 자리는 그 스키마의 `id`를 값으로 하는 `$ref`가 된다.

> **주의:** 모든 스키마는 레지스트리에 `id` 속성이 등록되어 있어야 한다. `id`가 없는 스키마는 무시된다.

```ts
z.toJSONSchema(z.globalRegistry);
// => {
//   schemas: {
//     User: {
//       id: 'User',
//       type: 'object',
//       properties: {
//         name: { type: 'string' },
//         posts: { type: 'array', items: { '$ref': 'Post' } }
//       },
//       required: [ 'name', 'posts' ],
//       additionalProperties: false,
//     },
//     Post: {
//       id: 'Post',
//       type: 'object',
//       properties: {
//         title: { type: 'string' },
//         content: { type: 'string' },
//         author: { '$ref': 'User' }
//       },
//       required: [ 'title', 'content', 'author' ],
//       additionalProperties: false,
//     }
//   }
// }
```

기본적으로 `$ref` URI는 `"User"` 같은 단순한 상대 경로다. 절대 URI로 만들려면 `uri` 옵션에 `id`를 완전한 URI로 바꾸는 함수를 넘긴다.

```ts
z.toJSONSchema(z.globalRegistry, {
  uri: (id) => `https://example.com/${id}.json`
});
// => {
//   schemas: {
//     User: {
//       id: 'User',
//       type: 'object',
//       properties: {
//         name: { type: 'string' },
//         posts: {
//           type: 'array',
//           items: { '$ref': 'https://example.com/Post.json' }
//         }
//       },
//       required: [ 'name', 'posts' ],
//       additionalProperties: false,
//     },
//     Post: {
//       id: 'Post',
//       type: 'object',
//       properties: {
//         title: { type: 'string' },
//         content: { type: 'string' },
//         author: { '$ref': 'https://example.com/User.json' }
//       },
//       required: [ 'title', 'content', 'author' ],
//       additionalProperties: false,
//     }
//   }
// }
```

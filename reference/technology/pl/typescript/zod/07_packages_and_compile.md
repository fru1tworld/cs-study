# Zod 패키지 구성, 컴파일, 생태계

> 원문: https://zod.dev/packages/zod
> 원문: https://zod.dev/packages/mini
> 원문: https://zod.dev/packages/core
> 원문: https://zod.dev/compile
> 원문: https://zod.dev/library-authors
> 원문: https://zod.dev/ecosystem

이 문서는 Zod 4 (4.6.5 기준)의 패키지 구성을 다룬다. Zod 4는 하나의 패키지 안에서 세 갈래로 나뉜다. 일반 애플리케이션에서 쓰는 `zod`, 메서드 대신 함수를 써서 번들 크기를 줄인 `zod/mini`(Zod Mini), 그리고 두 패키지가 함께 기반으로 삼는 `zod/v4/core`(Zod Core)다. 먼저 이 셋이 어떻게 나뉘는지 보고, 이어서 스키마를 미리 컴파일해 파싱을 빠르게 하는 AOT 컴파일, Zod 위에 라이브러리를 만들 때 지켜야 할 규칙, Zod를 지원하는 생태계 라이브러리 목록을 차례로 정리한다.

## 패키지 구성 (Zod)

`zod/v4` 패키지는 Zod 생태계의 대표(flagship) 라이브러리다. 이 문서에서 "일반 Zod"라고 부르는 것이 이 패키지이며, 평소 `import * as z from "zod"`로 가져오는 것도 이것이다. 개발 경험과 번들 크기 사이에서 균형을 잡았기 때문에 대부분의 애플리케이션에 가장 알맞다.

> **참고:** 번들 크기 제약이 유난히 엄격하다면 [Zod Mini](#zod-mini)를 고려한다.

Zod는 TypeScript의 타입 시스템과 일대일로 대응하는 스키마 API를 목표로 한다.

```ts
import * as z from "zod";

const schema = z.object({
  name: z.string(),
  age: z.number().int().positive(),
  email: z.email(),
});
```

API가 메서드 중심이라 복잡한 타입도 메서드를 이어 붙이는 체이닝으로 간결하게 정의할 수 있고, 에디터 자동 완성도 잘 된다.

```ts
z.string()
  .min(5)
  .max(10)
  .toLowerCase();
```

모든 스키마는 `z.ZodType` 기반 클래스를 상속하고, `z.ZodType`은 다시 [`zod/v4/core`](#zod-core)의 `z.$ZodType`을 상속한다. 모든 `ZodType` 인스턴스는 다음 메서드를 구현한다.

```ts
import * as z from "zod";

const mySchema = z.string();

// 파싱
mySchema.parse(data);
mySchema.safeParse(data);
mySchema.parseAsync(data);
mySchema.safeParseAsync(data);


// 정제(refinement)
mySchema.refine(refinementFunc);
mySchema.superRefine(refinementFunc); // 폐기 예정(deprecated), `.check()`를 사용한다
mySchema.overwrite(overwriteFunc);

// 래퍼
mySchema.optional();
mySchema.nonoptional();
mySchema.nullable();
mySchema.nullish();
mySchema.default(defaultValue);
mySchema.array();
mySchema.or(otherSchema);
mySchema.transform(transformFunc);
mySchema.catch(catchValue);
mySchema.pipe(otherSchema);
mySchema.readonly();

// 메타데이터와 레지스트리
mySchema.register(registry, metadata);
mySchema.describe(description);
mySchema.meta(metadata);

// 유틸리티
mySchema.check(checkOrFunction);
mySchema.clone(def);
mySchema.brand<T>();
mySchema.isOptional(); // boolean
mySchema.isNullable(); // boolean
```

각 메서드의 동작은 주제별 문서에서 설명한다. 래퍼와 정제, 변환은 [「Zod 스키마 정의 3: 정제, 변환, 코덱」](04_refinements_transforms_codecs.md), 레지스트리와 메타데이터는 [「Zod 메타데이터와 JSON Schema」](06_metadata_and_json_schema.md)를 참고한다.

## Zod Mini

> **참고:** Zod Mini 사용법은 일반 Zod 문서 곳곳에 Zod/Zod Mini 코드 쌍으로 함께 실려 있다. 이 절은 Zod Mini가 왜 존재하는지, 언제 쓰는지, 일반 Zod와 무엇이 다른지를 설명한다.

Zod Mini는 트리 셰이킹(tree-shaking)이 가능한 Zod 변형이다. 트리 셰이킹은 번들러가 실제로 쓰지 않는 코드를 최종 번들에서 빼 주는 기법으로, Zod Mini가 이 기법에 맞춰 설계된 이유는 [아래 「트리 셰이킹」](#트리-셰이킹-tree-shaking)에서 다룬다. 별도 패키지가 아니라 `zod` 패키지에 함께 들어 있으므로 설치는 일반 Zod와 같다.

```sh
npm install zod@^4.0.0
```

가져올 때는 `zod/mini` 경로를 쓴다.

```ts
import * as z from "zod/mini";
```

같은 API가 독립 패키지 `@zod/mini`로도 배포된다. 이 패키지는 `zod`를 피어 의존성(peer dependency)으로 둔다. 피어 의존성은 패키지가 `zod`를 직접 내장하지 않고, 사용하는 프로젝트에 이미 설치된 `zod`를 가져다 쓰겠다는 선언이다. 그래서 `@zod/mini`는 프로젝트의 `zod` 안에 있는 `zod/mini`를 그대로 다시 내보내기만 하고, 버전도 `zod`를 따라간다. 설치할 때도 두 패키지를 함께 설치한다.

```sh
npm install zod @zod/mini
```

```ts
import * as z from "@zod/mini";
```

Zod Mini의 기능은 `zod`와 완전히 같고, API 모양만 다르다. 스키마에 메서드를 이어 붙이는 대신 스키마를 *함수*의 인수로 감싼다. 아래 예제에서 `.optional().nullable()` 체인이 `z.nullable(z.optional(...))` 중첩 호출로 바뀌는 것을 보면 된다.

```ts
// 일반 Zod
const mySchema = z.string().optional().nullable();

// Zod Mini
const mySchema = z.nullable(z.optional(z.string()));
```

### 트리 셰이킹 (Tree-shaking)

트리 셰이킹은 최신 번들러가 최종 번들에서 쓰지 않는 코드를 제거하는 기법이며, *데드 코드 제거(dead-code elimination)*라고도 한다. 번들러는 `import` 문을 따라가며 실제로 참조되는 함수만 남기고, 아무도 부르지 않는 함수는 번들에 넣지 않는다.

문제는 메서드다. 일반 Zod의 스키마는 자주 쓰는 작업을 위한 편의 메서드(예: 문자열 스키마의 `.min()`)를 여럿 제공하는데, 메서드는 클래스에 붙어 있어서 클래스를 쓰는 순간 함께 딸려 온다. 번들러는 이렇게 쓰지 않는 메서드 구현은 번들에서 제거하지 못하지만, 쓰지 않는 최상위 함수는 제거할 수 있다. 그래서 Zod Mini의 API는 메서드보다 함수를 더 많이 쓴다. 아래 예제에서 `.min()`, `.max()`, `.trim()` 메서드가 `z.minLength()` 같은 최상위 함수로 바뀌었다.

```ts
// 일반 Zod
z.string().min(5).max(10).trim()

// Zod Mini
z.string().check(z.minLength(5), z.maxLength(10), z.trim());
```

번들 크기가 얼마나 줄어드는지 감을 잡기 위해 다음 스크립트를 보자.

```ts
z.boolean().parse(true)
```

이 코드를 Zod와 Zod Mini로 각각 번들링하면 크기는 다음과 같다. Zod Mini 쪽이 64% 작다.

- Zod Mini: 번들 크기(gzip): `2.12kb`
- Zod: 번들 크기(gzip): `5.91kb`

객체 타입이 들어간 조금 더 복잡한 스키마로 측정하면 다음과 같다.

```ts
const schema = z.object({ a: z.string(), b: z.number(), c: z.boolean() });

schema.parse({
  a: "asdf",
  b: 123,
  c: true,
});
```

- Zod Mini: 번들 크기(gzip): `4.0kb`
- Zod: 번들 크기(gzip): `13.1kb`

이 수치는 차이가 어느 정도인지 보여 주는 참고값일 뿐이다. Zod Mini가 프로젝트에 맞는지는 실제 사용 조건에서 직접 벤치마크를 돌려 판단한다.

### Zod Mini를 쓸 때와 쓰지 않을 때 (When (not) to use Zod Mini)

번들 크기 제약이 유난히 엄격한 경우가 아니라면 일반 Zod를 쓰는 편이 대체로 낫다. 많은 개발자가 번들 크기가 애플리케이션 성능에 미치는 영향을 크게 과대평가하기 때문이다. Zod 정도의 번들 크기(보통 `5-10kb`)가 실제로 문제가 되는 경우는 농촌이나 개발도상 지역처럼 모바일 네트워크가 느린 사용자를 대상으로 프런트엔드 번들을 최적화할 때뿐이다.

이렇게 판단하는 근거를 개발 경험, 백엔드, 인터넷 속도 순으로 살펴본다.

#### 개발 경험 (DX)

Zod Mini의 API는 더 장황하고 찾아 쓰기 어렵다. 일반 Zod에서는 스키마 뒤에 `.`만 찍으면 에디터의 자동 완성(Intellisense)이 쓸 수 있는 메서드를 보여 주지만, Zod Mini의 최상위 함수는 이름을 알고 있어야 찾을 수 있다. 체이닝으로 스키마를 빠르게 만들 수도 없다. (Zod 제작자는 Zod Mini API를 최대한 쓰기 편하게 설계하려고 많은 시간을 들였지만, 여전히 표준 Zod API를 강하게 선호한다고 밝힌다.)

#### 백엔드 개발 (Backend development)

백엔드에서 Zod를 쓴다면 Zod 규모의 번들 크기는 의미가 없다. Lambda처럼 자원이 제한된 환경에서도 마찬가지다. 이런 환경에서 번들 크기가 영향을 주는 지점은 콜드 스타트(cold start)인데, 콜드 스타트는 실행 중인 인스턴스가 없을 때 새 실행 환경을 띄우고 코드를 불러와 첫 요청을 처리하기까지 걸리는 시간이다. [이 글](https://medium.com/@adtanasa/size-is-almost-all-that-matters-for-optimizing-aws-lambda-cold-starts-cad54f65cbb)은 번들 크기별 콜드 스타트 시간을 측정했는데, 그 결과 일부는 다음과 같다.

- `1kb`: Lambda 콜드 스타트 시간: `171ms`
- `17kb` (Mini가 아닌 Zod의 gzip 크기): Lambda 콜드 스타트 시간: `171.6ms` (보간값)
- `128kb`: Lambda 콜드 스타트 시간: `176ms`
- `256kb`: Lambda 콜드 스타트 시간: `182ms`
- `512kb`: Lambda 콜드 스타트 시간: `279ms`
- `1mb`: Lambda 콜드 스타트 시간: `557ms`

크기를 무시해도 될 `1kb` 번들도 콜드 스타트에 최소 `171ms`가 걸리고, 그다음 측정한 `128kb` 번들은 여기서 `5ms`만 더 걸렸다. 일반 Zod 전체를 gzip으로 압축하면 약 `17kb`이므로, Zod 때문에 늘어나는 시작 시간은 `0.6ms` 정도다.

#### 인터넷 속도 (Internet speed)

프런트엔드에서도 사정은 비슷하다. 일반적으로 서버 왕복 시간(`100-200ms`)이 `10kb`를 더 내려받는 시간보다 훨씬 길어서, `10kb` 추가 다운로드가 눈에 띄는 차이를 만드는 경우는 느린 3G 연결(`1Mbps` 미만)뿐이다. 농촌이나 개발도상 지역 사용자를 위해 특별히 최적화하는 상황이 아니라면, 그 시간을 다른 최적화에 쓰는 편이 낫다.

### `ZodMiniType`

모든 Zod Mini 스키마는 `z.ZodMiniType` 기반 클래스를 상속하고, 이 클래스는 다시 [`zod/v4/core`](#zod-core)의 `z.core.$ZodType`을 상속한다. `zod`의 `ZodType`보다 구현한 메서드가 훨씬 적지만, 특히 유용한 몇 가지는 남아 있다.

#### `.parse`

스키마를 조합하는 API는 달라도, 모든 Zod Mini 스키마는 `zod`와 같은 파싱 메서드를 제공한다.

```ts
import * as z from "zod/mini"

const mySchema = z.string();

mySchema.parse('asdf')
await mySchema.parseAsync('asdf')
mySchema.safeParse('asdf')
await mySchema.safeParseAsync('asdf')
```

#### `.check()`

일반 Zod에서는 스키마 하위 클래스마다 자주 쓰는 검사(check)를 위한 전용 메서드가 있다.

```ts
import * as z from "zod";

z.string()
  .min(5)
  .max(10)
  .refine(val => val.includes("@"))
  .trim()
```

Zod Mini에는 이런 메서드가 없다. 대신 `.check()` 메서드로 검사를 스키마에 넘긴다.

```ts
import * as z from "zod/mini"

z.string().check(
  z.minLength(5), 
  z.maxLength(10),
  z.refine(val => val.includes("@")),
  z.trim()
);
```

구현된 검사는 다음과 같다. 일부 검사는 특정 타입(예: 문자열이나 숫자) 스키마에만 적용된다. 모든 API가 타입 안전하므로, 지원하지 않는 검사를 스키마에 추가하면 TypeScript가 막아 준다.

```ts
z.lt(value);
z.lte(value); // 별칭: z.maximum()
z.gt(value);
z.gte(value); // 별칭: z.minimum()
z.positive();
z.negative();
z.nonpositive();
z.nonnegative();
z.multipleOf(value);
z.maxSize(value);
z.minSize(value);
z.size(value);
z.maxLength(value);
z.minLength(value);
z.length(value);
z.regex(regex);
z.lowercase();
z.uppercase();
z.includes(value);
z.startsWith(value);
z.endsWith(value);
z.property(key, schema);
z.mime(value);

// 사용자 정의 검사
z.refine()
z.check()   // .superRefine()을 대체한다

// 변경(mutation) (추론된 타입은 바뀌지 않는다)
z.overwrite(value => newValue);
z.normalize();
z.trim();
z.toLowerCase();
z.toUpperCase();

// 메타데이터 (스키마를 z.globalRegistry에 등록한다)
z.meta({ title: "...", description: "..." });
z.describe("...");
```

#### `.register()`

스키마를 레지스트리에 등록한다. 레지스트리는 [「레지스트리」](06_metadata_and_json_schema.md#레지스트리-registries)에서 설명한다.

```ts
const myReg = z.registry<{title: string}>();

z.string().register(myReg, { title: "My cool string schema" });
```

#### `.brand()`

스키마에 *브랜드*를 붙인다. 브랜드 타입은 [「브랜드 타입」](04_refinements_transforms_codecs.md#브랜드-타입-branded-types)을 참고한다.

```ts
import * as z from "zod/mini"

const USD = z.string().brand("USD");
```

#### `.clone(def)`

주어진 `def`로 현재 스키마와 똑같은 복제본을 반환한다.

```ts
const mySchema = z.string()

mySchema.clone(mySchema._zod.def);
```

### 기본 로케일 없음 (No default locale)

로케일(locale)은 에러 메시지를 특정 언어로 만들어 주는 메시지 묶음이다. 일반 Zod는 영어(`en`) 로케일을 자동으로 불러오지만 Zod Mini는 그렇지 않다. 에러 메시지가 필요 없거나, 영어가 아닌 언어로 현지화하거나, 메시지를 직접 커스터마이징하는 경우라면 영어 로케일은 번들 크기만 늘리기 때문이다.

그래서 Zod Mini에서는 기본적으로 모든 이슈의 `message` 속성이 `"Invalid input"`으로만 나온다. 영어 로케일을 불러오려면 다음과 같이 한다.

```ts
import * as z from "zod/mini"
import { en } from "zod/locales";

z.config(en());
```

현지화는 [「국제화」](05_errors.md#국제화-internationalization)를 참고한다.

## Zod Core

이 하위 패키지는 Zod와 Zod Mini가 사용하는 핵심 클래스와 유틸리티를 내보낸다. 애플리케이션에서 직접 쓰라고 만든 것이 아니라 Zod, Zod Mini, 그리고 Zod 위에 만든 라이브러리가 확장하도록 설계되었다. 일반 Zod의 `.min()` 같은 편의 메서드는 Zod와 Zod Mini가 이 위에 얹은 것이고, Zod Core의 스키마 클래스에는 이런 메서드가 없다([「파싱」](#파싱-parsing) 참고). 구현 내용은 다음과 같다.

```ts
import * as z from "zod/v4/core";

// 모든 Zod 스키마의 기반 클래스
z.$ZodType;

// 자주 쓰는 파서를 구현한 $ZodType의 하위 클래스
z.$ZodString
z.$ZodObject
z.$ZodArray
// ...

// 모든 Zod 검사의 기반 클래스
z.$ZodCheck;

// 자주 쓰는 검사를 구현한 $ZodCheck의 하위 클래스
z.$ZodCheckMinLength
z.$ZodCheckMaxLength

// 모든 Zod 에러의 기반 클래스
z.$ZodError;

// 이슈 형식 (타입만 존재)
{} as z.$ZodIssue;

// 유틸리티
z.util.isValidJWT(...);
```

### 스키마 (Schemas)

모든 Zod 스키마의 기반 클래스는 `$ZodType`이다. 제네릭 매개변수 `Output`과 `Input` 두 개를 받는다.

```ts
export class $ZodType<Output = unknown, Input = unknown> {
  _zod: { /* 내부 구현 */}
}
```

`zod/v4/core`는 자주 쓰는 파서를 구현한 여러 하위 클래스를 내보낸다. 공식(first-party) 하위 클래스 전체의 유니언은 `z.$ZodTypes`로 내보낸다.

```ts
export type $ZodTypes =
  | $ZodString
  | $ZodNumber
  | $ZodBigInt
  | $ZodBoolean
  | $ZodDate
  | $ZodSymbol
  | $ZodUndefined
  | $ZodNullable
  | $ZodNull
  | $ZodAny
  | $ZodUnknown
  | $ZodNever
  | $ZodVoid
  | $ZodArray
  | $ZodObject
  | $ZodUnion // $ZodDiscriminatedUnion이 이것을 상속한다
  | $ZodIntersection
  | $ZodTuple
  | $ZodRecord
  | $ZodMap
  | $ZodSet
  | $ZodLiteral
  | $ZodEnum
  | $ZodPromise
  | $ZodLazy
  | $ZodOptional
  | $ZodDefault
  | $ZodTemplateLiteral
  | $ZodCustom
  | $ZodTransform
  | $ZodNonOptional
  | $ZodReadonly
  | $ZodNaN
  | $ZodPipe // $ZodCodec과 $ZodPreprocess가 이것을 상속한다
  | $ZodSuccess
  | $ZodCatch
  | $ZodFile;
```

핵심 스키마 클래스의 전체 상속 구조는 다음과 같다.

```txt
- $ZodType
    - $ZodString
        - $ZodStringFormat
            - $ZodGUID
            - $ZodUUID
            - $ZodEmail
            - $ZodURL
            - $ZodEmoji
            - $ZodNanoID
            - $ZodCUID
            - $ZodCUID2
            - $ZodULID
            - $ZodXID
            - $ZodKSUID
            - $ZodISODateTime
            - $ZodISODate
            - $ZodISOTime
            - $ZodISODuration
            - $ZodIPv4
            - $ZodIPv6
            - $ZodCIDRv4
            - $ZodCIDRv6
            - $ZodBase64
            - $ZodBase64URL
            - $ZodE164
            - $ZodJWT
    - $ZodNumber
        - $ZodNumberFormat
    - $ZodBigInt
        - $ZodBigIntFormat
    - $ZodBoolean
    - $ZodSymbol
    - $ZodUndefined
    - $ZodNull
    - $ZodAny
    - $ZodUnknown
    - $ZodNever
    - $ZodVoid
    - $ZodDate
    - $ZodArray
    - $ZodObject
    - $ZodUnion
        - $ZodDiscriminatedUnion
    - $ZodIntersection
    - $ZodTuple
    - $ZodRecord
    - $ZodMap
    - $ZodSet
    - $ZodEnum
    - $ZodLiteral
    - $ZodFile
    - $ZodTransform
    - $ZodOptional
    - $ZodNullable
    - $ZodDefault
    - $ZodPrefault
    - $ZodNonOptional
    - $ZodSuccess
    - $ZodCatch
    - $ZodNaN
    - $ZodPipe
        - $ZodCodec
        - $ZodPreprocess
    - $ZodReadonly
    - $ZodTemplateLiteral
    - $ZodCustom
```

### 내부 구조 (Internals)

`zod/v4/core`의 모든 하위 클래스는 `_zod` 속성 하나만 가지며, 이 속성에 스키마의 *내부 구현*을 담은 객체가 들어 있다. 속성을 `_zod` 하나로 줄인 것은 `zod/v4/core`를 확장하기 쉽고 특정 방식을 강요하지 않게 만들려는 설계다. 덕분에 다른 라이브러리는 자기 인터페이스가 `zod/v4/core`의 속성으로 어지러워질 걱정 없이 이 클래스들 위에 "자기만의 Zod"를 만들 수 있다. 클래스를 확장하는 예시는 `zod`와 `zod/mini`의 구현을 참고한다.

`_zod` 내부 속성에서 눈여겨볼 속성은 다음과 같다.

* `.def`: 스키마의 *정의(definition)*. 인스턴스를 만들 때 클래스 생성자에 넘기는 객체이며, 스키마를 완전히 기술하고 JSON으로 직렬화할 수 있다.
  * `.def.type`: 스키마의 타입을 나타내는 문자열. 예: `"string"`, `"object"`, `"array"` 등
  * `.def.checks`: 파싱 후 스키마가 실행하는 *검사*의 배열
* `.input`: 스키마의 *추론된 입력 타입*을 "저장"하는 가상 속성
* `.output`: 스키마의 *추론된 출력 타입*을 "저장"하는 가상 속성
* `.run()`: 스키마의 내부 파서 구현

Zod 스키마를 순회해야 하는 도구(예: 코드 생성기)를 만든다면, 어떤 스키마든 `$ZodTypes`로 캐스팅한 뒤 `def` 속성으로 클래스를 구별할 수 있다.

```ts
export function walk(_schema: z.$ZodType) {
  const schema = _schema as z.$ZodTypes;
  const def = schema._zod.def;
  switch (def.type) {
    case "string": {
      // ...
      break;
    }
    case "object": {
      // ...
      break;
    }
  }
}
```

`$ZodString`에는 여러 *문자열 포맷*을 구현한 하위 클래스가 있으며, `z.$ZodStringFormatTypes`로 내보낸다.

```ts
export type $ZodStringFormatTypes =
  | $ZodGUID
  | $ZodUUID
  | $ZodEmail
  | $ZodURL
  | $ZodEmoji
  | $ZodNanoID
  | $ZodCUID
  | $ZodCUID2
  | $ZodULID
  | $ZodXID
  | $ZodKSUID
  | $ZodISODateTime
  | $ZodISODate
  | $ZodISOTime
  | $ZodISODuration
  | $ZodIPv4
  | $ZodIPv6
  | $ZodCIDRv4
  | $ZodCIDRv6
  | $ZodBase64
  | $ZodBase64URL
  | $ZodE164
  | $ZodJWT
```

### 파싱 (Parsing)

Zod Core 스키마 클래스에는 메서드가 없으므로, 데이터 파싱은 최상위 함수로 한다.

```ts
import * as z from "zod/v4/core";

const schema = new z.$ZodString({ type: "string" });
z.parse(schema, "hello");
z.safeParse(schema, "hello");
await z.parseAsync(schema, "hello");
await z.safeParseAsync(schema, "hello");
```

### 검사 (Checks)

모든 Zod 스키마는 *검사*의 배열을 가진다. 검사는 파싱이 끝난 뒤 실행되는 정제로, 길이나 형식처럼 값을 추가로 확인할 뿐 추론된 타입은 *바꾸지 않는다*. `z.trim()`처럼 값을 바꾸는 검사도 가끔 있지만 이때도 타입은 그대로다.

```ts
const schema = z.string().check(z.email()).check(z.min(5));
// => $ZodString

schema._zod.def.checks;
// => [$ZodCheckEmail, $ZodCheckMinLength]
```

모든 Zod 검사의 기반 클래스는 `$ZodCheck`이며, 제네릭 매개변수 `T` 하나를 받는다.

```ts
export class $ZodCheck<in T = unknown> {
  _zod: { /* 내부 구현 */}
}
```

`_zod` 내부 속성에서 눈여겨볼 속성은 다음과 같다.

* `.def`: 검사의 *정의*. 검사를 만들 때 클래스 생성자에 넘기는 객체이며, 검사를 완전히 기술하고 JSON으로 직렬화할 수 있다.
  * `.def.check`: 검사의 종류를 나타내는 문자열. 예: `"min_length"`, `"less_than"`, `"string_format"` 등
* `.check()`: 검사의 검증 로직

`zod/v4/core`는 자주 쓰는 정제를 수행하는 여러 하위 클래스를 내보낸다. 공식 하위 클래스 전체는 `z.$ZodChecks`라는 유니언으로 내보낸다.

```ts
export type $ZodChecks =
  | $ZodCheckLessThan
  | $ZodCheckGreaterThan
  | $ZodCheckMultipleOf
  | $ZodCheckNumberFormat
  | $ZodCheckBigIntFormat
  | $ZodCheckMaxSize
  | $ZodCheckMinSize
  | $ZodCheckSizeEquals
  | $ZodCheckMaxLength
  | $ZodCheckMinLength
  | $ZodCheckLengthEquals
  | $ZodCheckProperty
  | $ZodCheckMimeType
  | $ZodCheckOverwrite
  | $ZodCheckStringFormat
```

`._zod.def.check` 속성으로 이 클래스들을 구별할 수 있다.

```ts
const check = {} as z.$ZodChecks;
const def = check._zod.def;

switch (def.check) {
  case "less_than":
  case "greater_than":
    // ...
    break;
}
```

스키마 타입과 마찬가지로, `$ZodCheckStringFormat`에도 여러 *문자열 포맷*을 구현한 하위 클래스가 있다.

```ts
export type $ZodStringFormatChecks =
  | $ZodCheckRegex
  | $ZodCheckLowerCase
  | $ZodCheckUpperCase
  | $ZodCheckIncludes
  | $ZodCheckStartsWith
  | $ZodCheckEndsWith
  | $ZodGUID
  | $ZodUUID
  | $ZodEmail
  | $ZodURL
  | $ZodEmoji
  | $ZodNanoID
  | $ZodCUID
  | $ZodCUID2
  | $ZodULID
  | $ZodXID
  | $ZodKSUID
  | $ZodISODateTime
  | $ZodISODate
  | $ZodISOTime
  | $ZodISODuration
  | $ZodIPv4
  | $ZodIPv6
  | $ZodCIDRv4
  | $ZodCIDRv6
  | $ZodBase64
  | $ZodBase64URL
  | $ZodE164
  | $ZodJWT;
```

문자열 포맷 검사끼리 구별하려면 `switch`를 중첩한다.

```ts
const check = {} as z.$ZodChecks;
const def = check._zod.def;

switch (def.check) {
  case "less_than":
  case "greater_than":
  // ...
  case "string_format":
    {
      const formatCheck = check as z.$ZodStringFormatChecks;
      const formatCheckDef = formatCheck._zod.def;

      switch (formatCheckDef.format) {
        case "email":
        case "url":
          // 필요한 처리
      }
    }
    break;
}
```

위 목록에서 `$ZodGUID`부터 아래쪽 클래스는 앞에서 본 문자열 포맷 *타입*과 같은 이름이다. 이 클래스들은 `$ZodCheck`와 `$ZodType` 인터페이스를 모두 구현하므로 검사로도 타입으로도 쓸 수 있다. 이렇게 겹치는 클래스로 파싱하면 `._zod.parse`(스키마 파서)와 `._zod.check`(검사 검증)가 모두 실행되므로, 인스턴스가 자기 자신의 `checks` 배열 맨 앞에 추가된 것처럼 동작한다(실제로 `._zod.def.checks`에 들어가지는 않는다). 아래 두 줄은 같은 `z.email()`을 각각 타입과 검사로 쓴 예다.

```ts
// 타입으로 사용
z.email().parse("user@example.com");

// 검사로 사용
z.string().check(z.email()).parse("user@example.com")
```

### 에러 (Errors)

Zod의 모든 에러의 기반 클래스는 `$ZodError`다.

> **주의:** 성능상의 이유로 `$ZodError`는 내장 `Error` 클래스를 상속하지 *않는다*. 따라서 `instanceof Error`는 `false`를 반환한다.

* `zod` 패키지는 `$ZodError`의 하위 클래스인 `ZodError`를 구현해 편의 메서드를 몇 가지 더한다.
* `zod/mini` 하위 패키지는 `$ZodError`를 그대로 사용한다.

```ts
export class $ZodError<T = unknown> implements Error {
 public issues: $ZodIssue[];
}
```

### 이슈 (Issues)

`issues` 속성은 `$ZodIssue` 객체의 배열이다. 모든 이슈는 `z.$ZodIssueBase` 인터페이스를 확장한다.

```ts
export interface $ZodIssueBase {
  readonly code?: string;
  readonly input?: unknown;
  readonly path: PropertyKey[];
  readonly message: string;
}
```

Zod가 정의하는 이슈 하위 타입은 다음과 같다.

```ts
export type $ZodIssue =
  | $ZodIssueInvalidType
  | $ZodIssueTooBig
  | $ZodIssueTooSmall
  | $ZodIssueInvalidStringFormat
  | $ZodIssueNotMultipleOf
  | $ZodIssueUnrecognizedKeys
  | $ZodIssueInvalidUnion
  | $ZodIssueInvalidKey
  | $ZodIssueInvalidElement
  | $ZodIssueInvalidValue
  | $ZodIssueCustom;
```

각 타입의 세부 내용은 [구현 코드](https://github.com/colinhacks/zod/blob/main/packages/zod/src/v4/core/errors.ts)를 참고한다. 에러와 이슈를 다루고 포맷팅하는 방법은 [「Zod 에러 커스터마이징과 포맷팅」](05_errors.md)에서 설명한다.

## AOT 컴파일 (AOT compilation)

AOT 컴파일(ahead-of-time compilation)은 실제 작업을 하기 전에 미리 코드를 만들어 두는 방식을 말한다. 표준 파서는 파싱할 때마다 스키마 구조를 따라가며 "이 키는 문자열이어야 하고, 저 키는 숫자여야 한다"를 해석한다. 컴파일을 하면 Zod가 이 해석을 한 번만 해서, 스키마 전용 검증 코드를 미리 만들어 둔다. 이렇게 만든 코드는 키마다 도는 반복문 대신 검사 문장이 한 줄씩 일직선으로 펼쳐져 있어서, 원문은 이를 반복문 없이 평평한(flat, loop-free) 검증기라고 부른다. 실제로 어떤 코드가 만들어지는지는 [「동작 원리」](#동작-원리-how-it-works)에서 볼 수 있다.

이 검증기는 표준 파서보다 몇 배 빠르게 동작하며, 결과와 에러는 똑같다. 다만 여기서 "미리"는 빌드 단계가 아니라 파싱하기 전을 뜻한다. 코드 생성은 애플리케이션이 실행되는 중에 같은 프로세스 안에서 일어나므로 빌드 도구와 연동할 필요가 없다.

```ts
import * as z from "zod";

const Player = z.object({
  username: z.string(),
  bio: z.string(),
  xp: z.number(),
  // ...속성 20개 더...
});

const CompiledPlayer = z.compile(Player);
```

사용법은 `Player`와 완전히 같다.

```ts
Player.parse({ ... });
CompiledPlayer.parse({ ... }); // 약 9배 빠르다
```

`CompiledPlayer` 같은 컴파일된 스키마도 다른 Zod 스키마와 다를 것이 없다. *컴파일된 스키마에만 적용되는 특별한 규칙은 없다.*

* 메서드가 같다: `.parse()`, `.safeParse()`, `.extend()`, `.optional()` 등
* 추론된 입력 타입과 출력 타입이 같다
* 이슈와 에러 메시지가 같다

Zod는 완전히 같은 결과를 보장하기 위해 전체 테스트 스위트를 두 번 돌린다. 한 번은 평소대로, 한 번은 전역 자동 컴파일([`import "zod/compile"`](#import-zodcompile))을 켠 상태로 실행한다.

가장 큰 이득을 보는 것은 객체나 튜플 같은 컨테이너다. 표준 파서가 키마다 순회하던 과정을 컴파일이 반복문 없는 평평한 검증 로직으로 펼쳐 놓기 때문에, JS 엔진이 이 코드를 최적화할 수 있다.

원문의 벤치마크 차트([benchmark](https://github.com/colinhacks/zod/blob/main/packages/bench/compile-matrix.ts))에 따르면 파싱 1회당 시간은 다음과 같이 줄어든다(낮을수록 좋다).

- 객체 10개의 배열
  - 표준 파서: 377 ns
  - 컴파일 후: 68 ns
  - 배율: 5.5x
- 키 20개 객체
  - 표준 파서: 301 ns
  - 컴파일 후: 38 ns
  - 배율: 7.8x
- 문자열 10개의 배열
  - 표준 파서: 241 ns
  - 컴파일 후: 33 ns
  - 배율: 7.3x
- 객체 3개의 유니언
  - 표준 파서: 190 ns
  - 컴파일 후: 36 ns
  - 배율: 5.3x
- 요소 3개 튜플
  - 표준 파서: 119 ns
  - 컴파일 후: 33 ns
  - 배율: 3.6x
- 키 5개 strict 객체
  - 표준 파서: 117 ns
  - 컴파일 후: 32 ns
  - 배율: 3.7x
- 판별 유니언
  - 표준 파서: 92 ns
  - 컴파일 후: 27 ns
  - 배율: 3.4x
- 키 5개 객체
  - 표준 파서: 76 ns
  - 컴파일 후: 28 ns
  - 배율: 2.8x

컴파일을 켜는 방법은 두 가지다.

### `z.compile()`

스키마 하나를 컴파일해 컴파일된 복사본을 반환한다. 원래 스키마는 바뀌지 않는다.

주의할 점은 컴파일된 스키마에서 *새* 스키마를 파생하는 경우다. `.refine()`, `.extend()`, `.optional()`, `.meta()` 같은 메서드는 컴파일되지 않은 새 스키마를 반환하므로, 중간 단계가 아니라 최종 스키마를 컴파일해야 한다.

```ts
// ❌ .refine()의 결과는 컴파일되지 않는다
const schema = z.compile(z.string()).refine((val) => val.length > 1);

// ✅ 마지막에 컴파일한다
const schema2 = z.compile(z.string().refine((val) => val.length > 1));
```

### `import "zod/compile"`

컴파일을 전역으로 켠다. 이 import *이후에* 생성된 모든 스키마는 처음 파싱할 때 자동으로 컴파일된다.

```ts
import "zod/compile"; // 스키마를 정의하는 모듈보다 먼저 와야 한다
import * as z from "zod";

const schema = z.object({ name: z.string() });
schema.parse({ name: "ok" }); // 첫 파싱 때 컴파일된다
```

컴파일은 지연(lazy) 방식이다. 스키마를 만들 때가 아니라 처음 파싱할 때 컴파일하므로, 정의만 하고 파싱에 쓰지 않은 스키마는 컴파일 비용을 치르지 않는다.

`import` 순서를 신경 쓰기 싫다면 Node.js CLI 플래그로 불러와도 된다. 이렇게 하면 `zod/compile`이 어떤 모듈이 스키마를 정의하는 것보다도 반드시 먼저 실행된다.

```sh
node --import zod/compile app.js   # ESM
node --require zod/compile app.cjs # CommonJS
```

[`bunfig.toml`](https://bun.com/docs/runtime/bunfig#preload)이나 [`nub.jsonc`](https://nubjs.com/docs/config#preload)의 `preload`에 지정해도 된다.

```jsonc title="nub.jsonc"
{
  "preload": ["zod/compile"]
}
```

이 import는 전역 설정을 바꾸므로 라이브러리가 아니라 애플리케이션에서 쓴다.

### 동작 원리 (How it works)

`z.compile()`은 내부적으로 스키마 전체를 한 번 순회해 반복문 없이 평평하고 고도로 최적화된 JavaScript 코드 조각을 만든다. 이 코드 조각은 문자열 상태로 만들어지며, `new Function()`(문자열로 된 코드를 함수로 만들어 주는 생성자로, 사실상 더 강력한 `eval`이다)으로 실행 가능한 함수가 된다.

이 함수는 일반 런타임 검증기보다 훨씬 빠르게 입력을 검증하며, 빠른 경로(fast path) 검증기 역할을 한다. 빠른 경로란 특정한 경우만 최소한의 작업으로 처리하고 나머지는 원래 경로에 맡기는 지름길 코드를 말한다. Zod의 빠른 경로가 맡는 경우는 "입력이 유효한 경우"다. 스키마는 먼저 이 함수로 입력이 유효한지 빠르게 확인하고, 검증에 실패하면 자세한 에러 정보를 만들기 위해 일반 런타임 로직으로 되돌아가는데, 이를 폴백(fallback)이라고 한다.

간단한 `Point` 스키마를 예로 들어 보자.

```ts
const Point = z.object({
  x: z.number(),
  y: z.number()
});
```

이 스키마로 생성되는 코드 조각은 다음과 같다.

```ts
const isPoint = new Function("input", `
  if (typeof input !== "object" || input === null) return false;
  if (typeof input.x !== "number") return false;
  if (typeof input.y !== "number") return false;
  return true;
`);

isPoint({ x: 1, y: 2 }); // true
isPoint({ x: "1" });     // false
```

`x`와 `y`를 키마다 순회하는 반복문이 없고, `typeof` 검사가 한 줄씩 나열되어 있다. 앞에서 말한 "평평한" 검증기가 바로 이런 모양이다. 대부분의 입력에서 생성된 함수는 JavaScript로 표현할 수 있는 가장 빠른 로직, 즉 스키마 구조를 해석하는 중간 단계 없이 일직선으로 늘어선 `typeof` 검사와 속성 읽기로 데이터를 검증한다. 이 함수가 처리하지 못하는 입력이면 Zod는 표준 파서로 되돌아간다.

앞의 `Player` 스키마로 Zod가 생성하는 함수는 다음과 같다.

```js
if (typeof input !== "object" || input === null || Array.isArray(input)) return INVALID;
const v0 = input["username"];
if (typeof v0 !== "string") return INVALID;
const v1 = input["bio"];
if (typeof v1 !== "string") return INVALID;
const v2 = input["xp"];
if (typeof v2 !== "number" || !Number.isFinite(v2)) return INVALID;
const v3 = { "username": v0, "bio": v1, "xp": v2 };
return v3;
```

이 함수는 검증을 통과한 값으로 새 객체(`v3`)를 만들어 반환하고, 어느 하나라도 맞지 않으면 `INVALID` 심벌을 반환한다. `INVALID`는 컴파일되지 않은 파서로 되돌아가라는 신호다. 기존 파서를 유지한 채 유효한 입력을 처리하는 경로만 추가하므로, 에러 보고도 기존 파서가 맡는다. 이 함수의 생성과 실행은 `new Function()`을 통해 런타임의 같은 프로세스 안에서 이루어져 빌드 시스템과 연동할 필요가 없다.

유효하지 않은 입력이면 컴파일되지 않은 스키마가 대신 실행되므로 에러도 컴파일되지 않은 스키마의 에러가 그대로 나온다. 여기서 두 가지 결과가 따라온다.

* 유효하지 않은 입력은 빠른 경로와 폴백 비용을 모두 치르므로, 실패하는 경우에는 컴파일해도 거의 빨라지지 않는다. 그 비용의 대부분은 에러를 만들어 내는 폴백에서 나온다.
* 정제와 변환은 유효한 입력에서 한 번, 유효하지 않은 입력에서 최대 두 번 실행된다. 빠른 경로에서 실행된 뒤, 폴백한 표준 파서가 처음부터 다시 파싱하면서 한 번 더 실행될 수 있기 때문이다. 따라서 정제나 변환 함수에 외부 상태를 바꾸는 부수 효과가 있다면 이 점을 고려해야 한다.

### 지원하지 않는 스키마 (Unsupported schemas)

일부 기능은 컴파일할 수 없거나 컴파일해도 이득이 없다. 이런 경우 `z.compile()`은 컴파일을 포기하고 원래 스키마를 그대로 반환한다.

```ts
const Schema = z.string().refine(async (val) => isAvailable(val));

z.compile(Schema); // 컴파일되지 않은 Schema 자체를 반환한다
```

* `async` 정제, 변환, 검사
* `z.xor()`
* 재귀 스키마
* `z.coerce.*`
* 사용자 정의 `when`(검사의 실행 조건을 직접 정하는 옵션, [「when」](04_refinements_transforms_codecs.md#when) 참고)이 있는 검사
* 콜백을 받는 `.catch()` (상수를 받는 `.catch(value)`는 정상적으로 컴파일된다)

이런 기능이 스키마 안쪽에 섞여 있다고 해서 항상 스키마 전체가 컴파일되지 않는 것은 아니다. 객체, 배열, 튜플, 레코드, 교차 타입 안에 지원하지 않는 자식이 있으면, 그 자식만 표준 파서로 실행되고 바깥 구조는 컴파일된 상태를 유지한다. 반면 유니언의 멤버 중 하나가 지원되지 않거나, `.catch()` 콜백이 있거나, 하위 트리 어디에든 비동기 요소가 있으면 스키마 전체가 폴백된다.

스키마와 상관없이 항상 표준 파서를 쓰는 작업도 있다. 인코딩(`z.encode()`, 코덱의 `"backward"` 방향)과 비동기 파싱이 그렇다.

컴파일할 수 없을 때 조용히 원래 스키마를 쓰는 대신 예외를 던지게 하려면 `strict`를 넘긴다. 예를 들어 자주 실행되어 성능이 중요한 경로(hot path)의 스키마가 정말 컴파일되었는지 확인할 때 쓴다.

```ts
z.compile(Schema, { strict: true }); // ZodCompileAsyncError를 던진다
```

비동기 스키마에서는 `ZodCompileAsyncError`, 그 밖의 경우에는 `ZodCompileUnsupportedError`가 발생한다. 두 에러 모두 `strict`일 때만 던져진다.

### 콘텐츠 보안 정책 (Content Security Policy)

콘텐츠 보안 정책(CSP)은 웹 페이지가 실행할 수 있는 스크립트를 제한하는 브라우저 보안 장치다. CSP는 보통 문자열을 코드로 실행하는 `eval` 계열 기능을 막는데, 컴파일은 바로 이 기능인 `new Function`을 쓰므로 CSP나 `eval` 금지 환경에서는 동작하지 않는다. 이런 환경에서는 `jitless`(런타임 코드 생성 없이 동작하라는 설정)를 켠다. 그러면 전역 모드는 컴파일을 건너뛴다.

```ts
z.config({ jitless: true });
```

반면 `z.compile()`을 직접 호출하는 건 명시적인 선택이므로 `jitless`와 상관없이 코드 생성을 시도한다. 환경이 `new Function`을 거부하면 [지원하지 않는 스키마](#지원하지-않는-스키마-unsupported-schemas)와 마찬가지로 컴파일되지 않은 스키마가 반환된다.

### 번들 크기 (Bundle size)

컴파일러는 코드 양이 많은 편이다. `z.compile()`을 호출하거나 `"zod/compile"`을 가져오면 컴파일러가 번들에 포함되어 gzip 기준 약 7 KB(minify 기준 28 KB)가 늘어난다. 둘 다 하지 않는 번들에서는 컴파일러가 [트리 셰이킹](#트리-셰이킹-tree-shaking)으로 완전히 제거되므로 비용이 전혀 없다.

키가 4개인 객체 스키마의 번들 크기는 다음과 같다.

- Zod
  - 컴파일러 없음: 24.1 KB
  - 컴파일러 포함: 31.1 KB
- Zod Mini
  - 컴파일러 없음: 4.6 KB
  - 컴파일러 포함: 13.2 KB

### 벤치마크 (Benchmarks)

이득은 스키마가 복잡할수록 커진다. 아래 목록의 각 스키마는 다른 작업 없이 파싱만 반복하는 루프에서 단독으로 측정했다. 이 조건은 표준 파서에 가장 유리하므로, 이 절 앞부분의 측정 결과보다 배율이 낮게 나온다([benchmark](https://github.com/colinhacks/zod/blob/main/packages/bench/compile-scaling.ts)).

- 객체, 키 5개: 속도 향상: 1.8x
- 객체, 키 10개: 속도 향상: 2.2x
- 객체, 키 20개: 속도 향상: 5.0x
- 객체, 키 50개: 속도 향상: 10.2x
- 튜플, 항목 1개: 속도 향상: 2.2x
- 튜플, 항목 3개: 속도 향상: 2.5x
- 튜플, 항목 5개: 속도 향상: 3.0x
- 튜플, 항목 10개: 속도 향상: 3.7x

[Moltar 벤치마크](https://github.com/moltar/typescript-runtime-type-benchmarks) 픽스처에서 컴파일한 Zod, 컴파일하지 않은 Zod, 다른 라이브러리를 비교한 결과도 있다([benchmark](https://github.com/moltar/typescript-runtime-type-benchmarks/pull/2329)). 초당 연산 수이며 높을수록 좋다.

`parseSafe` 범주는 알 수 없는 키를 제거한 새 객체를 반환한다.

- Zod 4 (컴파일): 초당 연산 수: 47.5M
- typia: 초당 연산 수: 45.3M
- Zod 4: 초당 연산 수: 11.6M
- valibot: 초당 연산 수: 1.8M
- effect: 초당 연산 수: 1.7M
- Zod 3: 초당 연산 수: 1.2M
- arktype: 초당 연산 수: 152k
- yup: 초당 연산 수: 121k

`assertLoose` 범주는 불리언을 반환하고 알 수 없는 키를 허용한다. Zod는 이 범주를 `z.validate()`로 실행한다(`z.validate()`는 [「`.validate()`」](01_intro_and_basics.md#validate) 참고).

- typia: 초당 연산 수: 74.9M
- arktype: 초당 연산 수: 66.2M
- Zod 4 (컴파일): 초당 연산 수: 60.6M
- Zod 4: 초당 연산 수: 6.5M
- valibot: 초당 연산 수: 1.9M
- effect: 초당 연산 수: 1.7M
- Zod 3: 초당 연산 수: 1.2M
- yup: 초당 연산 수: 124k

## 라이브러리 작성자 가이드 (For library authors)

이 절은 주로 Zod 위에 도구를 만드는 *라이브러리 작성자*를 위한 내용이다.

> **참고:** 원문은 라이브러리 작성자에게, 이 가이드에 더 들어가야 할 내용이 있다면 이슈를 열어 달라고 요청한다.

> **참고:** Zod 3는 사실상 지원이 끝났다(end-of-life). 보안 수정과 명백한 버그 수정은 계속 받지만 `zod@3.x`에 새 기능은 더 들어가지 않는다. 새 라이브러리나 기존 라이브러리의 새 메이저 버전은 Zod 4만 대상으로 한다. 기존 라이브러리를 확장하는 유지 관리자를 위한 이중 지원 방법은 [「Zod 3와 Zod 4를 함께 지원하기」](#zod-3와-zod-4를-함께-지원하기-how-to-support-zod-3-alongside-zod-4)를 참고한다.

### Zod에 의존해야 하는가 (Do I need to depend on Zod?)

가장 먼저 정말로 Zod에 의존해야 하는지 확인한다.

사용자가 정의한 스키마를 받아 블랙박스처럼 검증만 하는 라이브러리라면, 굳이 Zod와 직접 연동할 필요가 없을 수도 있다. 대신 [Standard Schema](https://standardschema.dev/)를 살펴본다. Standard Schema는 Zod를 비롯해 TypeScript 생태계의 인기 있는 검증 라이브러리 대부분이 구현하는 공통 인터페이스다([전체 목록](https://standardschema.dev/#what-schema-libraries-implement-the-spec)).

사용자 정의 스키마를 "블랙박스" 검증기로만 다룬다면 이 명세로 충분하다. 명세를 따르는 라이브러리의 스키마라면 무엇이든 받아서 추론된 입력/출력 타입을 뽑아내고, 입력을 검증하고, 표준화된 에러를 돌려받을 수 있다.

Zod 고유의 기능이 필요하다면 계속 읽는다.

### 피어 의존성 설정 (How to configure peer dependencies?)

Zod 위에 만든 라이브러리는 `"peerDependencies"`에 `"zod"`를 넣어야 한다. 앞의 [`@zod/mini`](#zod-mini)에서 본 것처럼, 피어 의존성으로 두면 라이브러리가 Zod를 따로 내장하지 않고 사용자 프로젝트에 설치된 Zod를 쓴다. 사용자가 "자기 Zod를 가져와서(bring their own Zod)" 쓰는 셈이다.

```json
// package.json
{
  // ...
  "peerDependencies": {
    "zod": "^4.0.0"
  }
}
```

라이브러리를 개발하는 동안에는 라이브러리 자신이 피어 의존성 요구를 충족해야 하므로 `"devDependencies"`에도 `"zod"`를 추가한다.

```ts
// package.json
{
  "peerDependencies": {
    "zod": "^4.0.0"
  },
  "devDependencies": {
    "zod": "^4.0.0"
  }
}
```

기존 라이브러리를 확장하면서 Zod 3 사용자도 계속 지원하려면, 더 넓은 피어 의존성 범위를 다루는 [「Zod 3와 Zod 4를 함께 지원하기」](#zod-3와-zod-4를-함께-지원하기-how-to-support-zod-3-alongside-zod-4)를 참고한다.

### 어떤 하위 경로에서 가져와야 하는가 (Which subpaths should I import from?)

Zod 4 코어 패키지는 `"zod/v4/core"` 하위 경로에서 가져온다.

```ts
import * as z4 from "zod/v4/core";
```

이 하위 경로는 Zod 4를 가리키는 "고정 링크(permalink)"라고 생각하면 된다. 앞으로 `zod` 패키지의 메이저 버전이 바뀌어도 이 경로는 계속 Zod 4를 가리킨다. 다른 고정 링크 하위 경로는 `"zod/v3"`뿐이며, [Zod 3도 함께 지원](#zod-3와-zod-4를-함께-지원하기-how-to-support-zod-3-alongside-zod-4)할 때만 필요하다.

그 밖의 경로에서는 가져오지 않는 것이 원칙이다. Zod Core는 Zod 4 Classic과 Zod 4 Mini를 모두 떠받치는 공유 라이브러리다(Classic은 이 문서에서 "일반 Zod"라고 부른 `zod` 패키지를 Mini와 구분해 부르는 이름이다). 그러니 라이브러리가 둘 중 한쪽에만 해당하는 기능을 구현하는 것은 대체로 좋지 않다. 특히 다음 하위 경로에서는 가져오지 않는다.

* `"zod"`: ❌ 3.x 릴리스에서는 Zod 3를, 4.x 릴리스에서는 Zod 4를 내보낸다. 대신 고정 링크를 쓴다.
* `"zod/v4"`와 `"zod/v4/mini"`: ❌ 각각 Zod 4 Classic과 Mini가 있는 경로다. `"zod/v4"` 모듈의 클래스를 참조하면 Zod Mini에서 동작하지 않고, 반대도 마찬가지이므로 강하게 권장하지 않는다. 라이브러리가 Zod와 Zod Mini 모두에서 동작하게 하려면 `"zod/v4/core"`에 정의된 기반 클래스, 즉 Zod Classic과 Zod Mini가 확장하는 `$` 접두사 클래스를 대상으로 만들어야 한다. Classic과 Mini 하위 클래스의 내부 구조는 똑같고, 어떤 헬퍼 메서드를 구현하는지만 다르다.

이 버전 관리 방식의 배경은 [Versioning in Zod 4](https://github.com/colinhacks/zod/issues/4371) 글을 참고한다. 버전 정책 자체는 [「버전 정책」](08_zod4_migration.md#버전-정책-versioning)에서 다룬다.

### Zod 3와 Zod 4를 함께 지원하기 (How to support Zod 3 alongside Zod 4?)

> **참고:** 이 절은 Zod 3 사용자가 있는 기존 라이브러리의 유지 관리자를 위한 내용이다. 새 라이브러리(또는 기존 라이브러리의 새 메이저 버전)는 `^4.0.0`만 지원한다. Zod 3는 사실상 지원이 끝났고 새 기능을 받지 않는다.

Zod 3 사용자가 있는 라이브러리를 유지 관리한다면, 그 사용자들을 버리지 않고 Zod 4 지원을 추가할 수 있다. 피어 의존성 범위를 두 버전에 걸치도록 넓히면 된다. `"zod/v4"` 하위 경로는 `3.25.0`부터 제공된다.

```json
// package.json
{
  // ...
  "peerDependencies": {
    "zod": "^3.25.0 || ^4.0.0"
  }
}
```

이렇게 해도 라이브러리의 메이저 버전을 올릴 필요는 없다. 피어 의존성을 올리면 사용자가 `npm upgrade zod`를 해야 하지만, `zod@3.24`와 `zod@3.25` 사이에는 호환성을 깨는 변경이 없었다(사실 코드 변경 자체가 없었다). 그래서 Zod 4 지원은 마이너 버전으로 내보낼 수 있다. 어차피 메이저 버전을 올릴 때가 되었다면, Zod 3를 버리고 피어 범위를 `^4.0.0`으로 좁힌 메이저를 내는 편이 더 깔끔하다.

`v3.25.0`부터 `zod` 패키지는 Zod 3와 Zod 4의 사본을 각각의 하위 경로에 함께 담고 있으므로, 둘을 나란히 가져올 수 있다.

```ts
import * as z3 from "zod/v3";
import * as z4 from "zod/v4/core";

type Schema = z3.ZodTypeAny | z4.$ZodType;

function acceptUserSchema(schema: z3.ZodTypeAny | z4.$ZodType) {
  // ...
}
```

런타임에 Zod 3 스키마와 Zod 4 스키마를 구별하려면 `"_zod"` 속성이 있는지 확인한다. 이 속성은 Zod 4 스키마에만 정의되어 있다.

```ts
import type * as z3 from "zod/v3";
import type * as z4 from "zod/v4/core";

declare const schema: z3.ZodTypeAny | z4.$ZodType;

if ("_zod" in schema) {
  schema._zod.def; // Zod 4 스키마
} else {
  schema._def; // Zod 3 스키마
}
```

### Zod와 Zod Mini를 동시에 지원하기 (How to support Zod and Zod Mini simultaneously?)

라이브러리 코드는 `"zod/v4/core"`에서만 가져온다. 이 하위 패키지는 Zod와 Zod Mini가 공유하는 인터페이스, 클래스, 유틸리티를 정의한다. 이 규칙만 지키면 Zod와 Zod Mini 모두 자동으로 동작한다.

```ts
// 라이브러리 코드
import * as z4 from "zod/v4/core";

export function acceptObjectSchema<T extends z4.$ZodObject>(schema: T){
  // 데이터 파싱
  z4.parse(schema, { /* somedata */});
  // 내부 구조 조회
  schema._zod.def.shape;
}
```

공유 기반 인터페이스를 대상으로 만들면 두 하위 패키지를 동시에 안정적으로 지원할 수 있다. 이 함수는 Zod 스키마와 Zod Mini 스키마를 모두 받는다.

```ts
// 사용자 코드
import { acceptObjectSchema } from "your-library";

// Zod 4
import * as z from "zod";
acceptObjectSchema(z.object({ name: z.string() }));

// Zod 4 Mini
import * as zm from "zod/mini";
acceptObjectSchema(zm.object({ name: zm.string() }))
```

코어 하위 라이브러리의 내용은 앞의 [Zod Core](#zod-core) 절을 참고한다.

### 사용자 정의 스키마 받기 (How to accept user-defined schemas?)

사용자 정의 스키마를 받는 것은 Zod 위에 만든 라이브러리의 가장 기본적인 작업이다. 이 절은 그 모범 사례를 정리한다.

처음에는 Zod 스키마를 받는 함수를 다음처럼 작성하고 싶어진다.

```ts
import * as z4 from "zod/v4/core";

function inferSchema<T>(schema: z4.$ZodType<T>) {
  return schema;
}
```

하지만 이 방식은 TypeScript가 인수 타입을 제대로 추론하지 못하게 만든다. 무엇을 넘기든 `schema`의 타입은 `$ZodType`의 인스턴스가 된다.

```ts
inferSchema(z.string());
// => $ZodType<string>
```

이렇게 하면 입력이 실제로 *어느 하위 클래스*인지(이 경우 `ZodString`)를 알려 주는 타입 정보를 잃는다. 그래서 `inferSchema`의 결과에 `.min()` 같은 문자열 전용 메서드를 호출할 수 없다. 대신 제네릭 매개변수가 Zod 코어 스키마 인터페이스를 확장하도록 만든다.

```ts
function inferSchema<T extends z4.$ZodType>(schema: T) {
  return schema;
}

inferSchema(z.string());
// => ZodString ✅
```

입력 스키마를 특정 하위 클래스로 제한하려면 다음과 같이 한다.

```ts

import * as z4 from "zod/v4/core";

// 객체 스키마만 받는다
function inferSchema<T extends z4.$ZodObject>(schema: T) {
  return schema;
}
```

입력 스키마의 추론된 출력 타입을 제한하려면 다음과 같이 한다.

```ts

import * as z4 from "zod/v4/core";

// 문자열 스키마만 받는다
function inferSchema<T extends z4.$ZodType<string>>(schema: T) {
  return schema;
}

inferSchema(z.string()); // ✅ 

inferSchema(z.number()); 
// ❌ The types of '_zod.output' are incompatible between these types. 
// // Type 'number' is not assignable to type 'string'
```

스키마로 데이터를 파싱할 때는 최상위 함수 `z4.parse`/`z4.safeParse`/`z4.parseAsync`/`z4.safeParseAsync`를 쓴다. [「파싱」](#파싱-parsing)에서 본 것처럼 `z4.$ZodType` 하위 클래스에는 메서드가 없기 때문이다. `.parse()` 같은 파싱 메서드는 Zod와 Zod Mini가 구현하며 Zod Core에는 없다.

```ts
function parseData<T extends z4.$ZodType>(data: unknown, schema: T): z4.output<T> {
  return z.parse(schema, data);
}

parseData("sup", z.string());
// => string
```

## 생태계 (Ecosystem)

> **참고:** 이 절은 Zod 4를 지원하는 라이브러리를 다룬다. Zod 4를 지원하는 라이브러리를 만들었다면 PR로 추가를 요청할 수 있다. Zod 3용 라이브러리는 [v3.zod.dev](https://v3.zod.dev/?id=ecosystem)를 참고한다.

Zod 위에 만들어졌거나 Zod를 기본 지원하는 도구가 계속 늘고 있다. Zod 기반 도구나 라이브러리를 만들었다면 [Twitter](https://x.com/colinhacks)로 알리거나 [Discussion](https://github.com/colinhacks/zod/discussions)을 열면 목록에 추가된다.

원문 페이지의 목록은 React 컴포넌트로 렌더링되므로, 아래 목록은 Zod 저장소의 [`packages/docs/components/ecosystem.tsx`](https://github.com/colinhacks/zod/blob/main/packages/docs/components/ecosystem.tsx)에서 옮겼다. 설명은 원문의 한 줄 소개를 번역한 것이다.

### 학습 자료 (Resources)

* [Total TypeScript Zod Tutorial](https://www.totaltypescript.com/tutorials/zod) - [@mattpocockuk](https://x.com/mattpocockuk)
* [Fixing TypeScript's Blindspot: Runtime Typechecking](https://www.youtube.com/watch?v=rY_XqfSHock) - [@jherr](https://x.com/jherr)
* [Validate Environment Variables With Zod](https://catalins.tech/validate-environment-variables-with-zod/) - [@catalinmpit](https://x.com/catalinmpit)

### API 라이브러리 (API Libraries)

* [tRPC](https://github.com/trpc/trpc): GraphQL 없이 종단 간 타입 안전한 API를 만든다.
* [GQLoom](https://gqloom.dev/): Zod로 GraphQL 스키마와 리졸버를 엮는다.
* [oRPC](https://orpc.unnoq.com/): 타입 안전한 API를 간단하게 만든다.
* [Express Zod API](https://github.com/RobinTail/express-zod-api): 입출력 검증과 미들웨어, OpenAPI 문서, 타입 안전 클라이언트를 갖춘 Express 기반 API를 만든다.
* [nestjs-zod](https://github.com/BenLorantfy/nestjs-zod): NestJS와 Zod를 통합한다. Zod로 NestJS DTO를 만들고, 직렬화하고, OpenAPI 문서를 생성한다.
* [Zod Sockets](https://github.com/RobinTail/zod-sockets): 입출력 검증, AsyncAPI 생성기, 타입 안전한 이벤트 맵을 갖춘 Socket.IO 솔루션.
* [Zod JSON-RPC](https://github.com/danscan/zod-jsonrpc): Zod를 쓰는 타입 안전한 JSON-RPC 2.0 클라이언트/서버 라이브러리.
* [upfetch](https://github.com/L-Blondy/up-fetch): 고급 fetch 클라이언트 빌더.
* [zodql](https://github.com/mattiasahlsen/zodql): Zod 스키마 하나를 GraphQL 쿼리, 추론된 응답 타입, 런타임 검증의 단일 출처로 쓴다.

### 폼 통합 (Form Integrations)

* [regle](https://github.com/victorgarciaesgi/regle): Vue.js용 헤드리스 폼 검증 라이브러리.
* [conform](https://conform.guide/api/zod/parseWithZod): 웹 기본기를 활용해 HTML 폼을 점진적으로 향상하는 타입 안전한 폼 검증 라이브러리. Remix, Next.js 같은 서버 프레임워크를 완전히 지원한다.
* [Superforms](https://superforms.rocks): SvelteKit 폼을 즐겁게 쓸 수 있게 해 준다.
* [zod-validation-error](https://github.com/causaly/zod-validation-error): ZodError 인스턴스에서 사용자 친화적인 에러 메시지를 만든다.
* [svelte-jsonschema-form](https://x0k.dev/svelte-jsonschema-form/validators/zod4/): JSON 스키마를 기반으로 폼을 만드는 Svelte 5 라이브러리.
* [frrm](https://www.npmjs.com/package/frrm): 0.5kb짜리 Zod 기반 HTML 폼 추상화.
* [react-f3](https://www.npmjs.com/package/react-f3): React에서 단순한 폼 경험을 만들고 관리하기 위한 컴포넌트, 훅, 유틸리티.
* [Attaform](https://attaform.dev): Vue 3와 Nuxt를 위한 타입 안전한 Zod 우선 폼 라이브러리.
* [zod-form-action](https://github.com/Vish05/zod-form-action): React의 `useActionState`에 Zod 검증을 연결하는 작은 헬퍼. 필드별 에러가 타입으로 잡히고 보일러플레이트가 없다.

### Zod에서 다른 형식으로 (Zod to X)

* [zod-openapi](https://github.com/samchungy/zod-openapi): Zod 스키마로 OpenAPI v3.x 문서를 만든다.
* [fastify-zod-openapi](https://github.com/samchungy/fastify-zod-openapi): Zod 스키마를 위한 Fastify 타입 프로바이더, 검증, 직렬화, `@fastify/swagger` 지원.
* [zod2md](https://github.com/matejchalk/zod2md): Zod 스키마로 Markdown 문서를 생성한다.
* [prisma-zod-generator](https://github.com/omar-dulaimi/prisma-zod-generator): Prisma 스키마로 ZodObject 메서드를 모두 지원하는 Zod 스키마를 생성한다.
* [@traversable/zod](https://github.com/traversable/schema/tree/main/packages/zod): 직접 "Zod to x" 라이브러리를 만들거나 25개 이상의 기성 변환기 중에서 고른다.
* [zod-mongo-schema](https://github.com/udohjeremiah/zod-mongo-schema): Zod 스키마를 MongoDB 호환 JSON 스키마로 변환한다.
* [convex-helpers](https://github.com/get-convex/convex-helpers/blob/main/packages/convex-helpers/README.md#zod-validation): Zod로 Convex 함수의 인수와 반환값을 검증하고 Convex 데이터베이스 스키마를 만든다.
* [@nullix/zod-mongoose](https://zodmongoose.com/): Zod 스키마를 mongoose 옵션과 헬퍼를 그대로 쓸 수 있는 타입 안전한 mongoose 스키마로 변환한다.

### 다른 형식에서 Zod로 (X to Zod)

* [orval](https://github.com/orval-labs/orval): OpenAPI 스키마로 Zod 스키마를 생성한다.
* [kubb](https://github.com/kubb-labs/kubb): API 작업을 위한 종합 툴킷.
* [Hey API](https://heyapi.dev/openapi-ts/plugins/zod): OpenAPI에서 TypeScript 코드를 생성한다. 프로덕션용 SDK, Zod 스키마, TanStack Query 훅과 20개 이상의 플러그인을 제공하며 Vercel, OpenCode, PayPal이 사용한다.
* [valype](https://github.com/yuzheng14/valype): TypeScript 타입 정의를 런타임 검증기(Zod 포함)로 변환한다.
* [Prisma Zod Generator](https://github.com/omar-dulaimi/prisma-zod-generator): input/result/pure 변형, minimal/full/custom 모드, 선택적 생성과 필터링, 단일/다중 파일 출력, `@zod` 규칙, 관계 깊이 제한을 지원하는 Zod 스키마 생성기.
* [DRZL](https://github.com/use-drzl/drzl): 스키마에서 Zod 검증기와 타입이 지정된 서비스, 강타입 라우터(oRPC/tRPC 등)를 생성하는 Drizzle ORM 툴킷.
* [convex-helpers](https://github.com/get-convex/convex-helpers/blob/main/packages/convex-helpers/README.md#zod-validation): Convex 검증기로 Zod 스키마를 생성한다.
* [Hono Takibi](https://github.com/nakita628/hono-takibi): OpenAPI에서 `@hono/zod-openapi` 코드를 생성한다.
* [tauri-typegen](https://github.com/thwbh/tauri-typegen): Rust 기반 크로스 플랫폼 프레임워크 `@tauri-apps/tauri`용 Zod 스키마와 검증 훅을 생성한다.
* [@apical-ts/craft](https://gunzip.github.io/apical-ts/): Zod로 타입 안전한 클라이언트와 서버를 만드는 OpenAPI-to-TypeScript 생성기.

### 모킹 라이브러리 (Mocking Libraries)

* [zod-schema-faker](https://github.com/soc221b/zod-schema-faker): Zod 스키마로 목 데이터를 생성한다. `@faker-js/faker`와 randexp.js 기반.
* [zocker](https://zocker.sigrist.dev): Zod 스키마에 맞는 유효하고 의미 있는 데이터를 생성한다.
* [@traversable/zod-test](https://github.com/traversable/schema/tree/main/packages/zod-test): 퍼즈 테스트용 무작위 Zod 스키마 생성기. 유효한 데이터와 유효하지 않은 데이터 생성기를 모두 포함한다.

### Zod 기반 프로젝트 (Powered by Zod)

* [zod-config](https://github.com/alexmarqs/zod-config): 유연한 어댑터로 여러 출처의 설정을 불러오고 Zod로 타입 안전성을 보장한다.
* [Composable Functions](https://github.com/seasonedcc/composable-functions): 함수 합성을 쉽고 안전하게 해 주는 타입과 함수.
* [zod-xlsx](https://github.com/sidwebworks/zod-xlsx): Zod 스키마로 xlsx 기반 리소스를 검증한다. 데이터 가져오기 등에 쓴다.
* [bupkis](https://github.com/boneskull/bupkis): 확장성이 매우 높은 단언(assertion) 라이브러리.
* [Fn Sphere](https://github.com/lawvs/fn-sphere): 웹 앱에서 강력하고 타입 안전한 필터 경험을 만드는 Zod 우선 툴킷.
* [zodgres](https://github.com/endel/zodgres): Postgres.js와 Zod를 결합해 정적 타입 추론과 자동 마이그레이션을 갖춘 데이터베이스 컬렉션을 제공한다.
* [validex](https://github.com/chiptoma/validex): 흔한 필드(이메일, 전화번호, 비밀번호 등)를 위한 트리 셰이킹 가능한 검증 규칙 25개. 구조화된 에러 코드, i18n, 프레임워크 어댑터를 제공한다.
* [json-up](https://github.com/Nano-Collective/json-up): Zod 스키마 검증을 쓰는 빠르고 타입 안전한 JSON 마이그레이션 도구.
* [@chrock-studio/overload](https://jsr.io/@chrock-studio/overload): 인수 타입에 따라 런타임에 분기하는 가볍고 타입 안전한 함수 오버로딩 라이브러리.
* [ArkEnv](https://github.com/yamcodes/arkenv): 에디터부터 런타임까지 환경 변수를 검증한다. Next.js, Nuxt, Node.js, Vite, Bun 등을 지원한다.
* [fullproduct.dev](https://fullproduct.dev): 스키마를 단일 출처로 삼아 Expo와 Next.js 유니버설 앱을 만드는 Zod·TS 우선 접근법.

### Zod 유틸리티 (Zod Utilities)

* [zod-compiler](https://github.com/gajus/zod-compiler): 빌드 시점에 Zod 스키마를 오버헤드 없는 검증 함수로 컴파일한다. Vite, webpack, esbuild, Rollup 등에서 동작한다.
* [babel-plugin-zod-hoist](https://github.com/gajus/babel-plugin-zod-hoist): 스키마 정의를 파일 최상단으로 끌어올려 반복 초기화 비용을 없애는 Babel 플러그인.
* [eslint-plugin-import-zod](https://github.com/samchungy/eslint-plugin-import-zod): Zod를 네임스페이스 import로 가져오도록 강제하는 ESLint 플러그인.
* [zod-playground](https://github.com/marilari88/zod-playground): Zod와 Zod Mini 스키마를 실시간으로 시험해 보는 대화형 플레이그라운드.
* [eslint-zod](https://github.com/marcalexiei/eslint-zod): Zod와 Zod Mini 사용 모범 사례를 강제하는 규칙을 추가하는 ESLint 플러그인.
* [Zod Compare](https://github.com/lawvs/zod-compare): Zod 스키마를 재귀적으로 비교하는 유틸리티 라이브러리.
* [zod-ir](https://github.com/Reza-kh80/zod-ir): 이란 데이터 구조(국민 코드, 은행 카드, Sheba, 암호화폐 등)를 검증하고 은행 이름, 로고 같은 메타데이터를 추출한다. 의존성이 없다.
* [Zod AOT](https://github.com/wakita181009/zod-aot): 빌드 시점에 Zod 스키마를 오버헤드 없는 검증 함수로 컴파일한다. 코드 변경 없이 검증이 2-64배 빨라진다.
* [@chrock-studio/zod-utils](https://jsr.io/@chrock-studio/zod-utils): 기능 확장에 초점을 둔 Zod 유틸리티 모음(예: 순수 데이터 구조를 유지하는 클래스 없는 리치 도메인 모델).
* [oxlint-plugin-import-zod](https://github.com/samchungy/oxlint-plugin-import-zod): Zod를 네임스페이스 import로 가져오도록 강제하는 Oxlint 플러그인.
* [shorn](https://shorn.dev): Zod 스키마를 전송 형식으로 쓰는 간결한 바이너리 직렬화. IDL이나 코드 생성이 필요 없다.

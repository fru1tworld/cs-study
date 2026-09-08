# Zod 스키마 정의 1: 원시 타입과 문자열

> 원문: https://zod.dev/api

데이터를 검증하려면 먼저 스키마(schema)를 정의해야 한다. 스키마는 단순한 원시 값부터 복잡하게 중첩된 객체와 배열까지 여러 타입을 표현한다. 이 문서는 Zod 4 (4.6.5 기준) API 페이지 가운데 원시 타입, 문자열, 숫자, 열거형, optional/nullable, unknown/never를 다룬다. 객체와 컬렉션은 [「Zod 스키마 정의 2」](03_schemas_objects_collections.md)에서, 정제와 변환은 [「Zod 스키마 정의 3: 정제, 변환, 코덱」](04_refinements_transforms_codecs.md)에서 설명한다.

코드 예제 중 Zod와 Zod Mini의 표기가 다른 경우에는 두 코드 블록을 함께 둔다. Zod Mini 자체는 [「Zod Mini」](07_packages_and_compile.md#zod-mini)에서 설명한다.

## 원시 타입 (Primitives)

원시 타입(primitive)은 문자열, 숫자, 불리언처럼 객체가 아닌 JavaScript 기본 값의 타입이다. Zod는 원시 타입마다 그 타입의 값만 통과시키는 스키마를 하나씩 제공한다.

```ts
import * as z from "zod";

// 원시 타입
z.string();
z.number();
z.bigint();
z.boolean();
z.symbol();
z.undefined();
z.null();
```

### 강제 변환 (Coercion)

`z.string()`이나 `z.number()`는 타입이 다른 입력을 그대로 거부한다. 숫자가 문자열로 들어오는 경우처럼 입력을 먼저 원하는 타입으로 바꾼 뒤 검증하려면 `z.coerce`를 쓴다. 이렇게 입력의 타입을 강제로 바꾸는 것을 강제 변환(coercion)이라고 한다.

```ts
z.coerce.string();    // String(input)
z.coerce.number();    // Number(input)
z.coerce.boolean();   // Boolean(input)
z.coerce.bigint();    // BigInt(input)
```

아래 예제에서 `z.coerce.string()`은 숫자, 불리언, `null`까지 모두 문자열로 바꿔 통과시킨다.

```ts
const schema = z.coerce.string();

schema.parse("tuna");    // => "tuna"
schema.parse(42);        // => "42"
schema.parse(true);      // => "true"
schema.parse(null);      // => "null"
```

#### 강제 변환의 동작 방식

Zod는 입력이 무엇이든 JavaScript 내장 생성자에 그대로 넘겨 변환한다. 스키마별로 쓰는 생성자는 다음과 같다.

- `z.coerce.string()`: `String(value)`
- `z.coerce.number()`: `Number(value)`
- `z.coerce.boolean()`: `Boolean(value)`
- `z.coerce.bigint()`: `BigInt(value)`
- `z.coerce.date()`: `new Date(value)`

이 방식 때문에 `z.coerce.boolean()`은 예상과 다르게 동작할 수 있다. `Boolean()`은 문자열의 내용을 해석하지 않고 조건문에서 참으로 취급되는지만 따진다. 그래서 [truthy](https://developer.mozilla.org/en-US/docs/Glossary/Truthy) 값은 모두 `true`로, [falsy](https://developer.mozilla.org/en-US/docs/Glossary/Falsy) 값(`0`, 빈 문자열, `undefined`, `null` 등)은 모두 `false`로 바뀐다. 빈 문자열이 아닌 `"false"`도 truthy라서 `true`가 된다.

```ts
const schema = z.coerce.boolean(); // Boolean(input)

schema.parse("tuna"); // => true
schema.parse("true"); // => true
schema.parse("false"); // => true
schema.parse(1); // => true
schema.parse([]); // => true

schema.parse(0); // => false
schema.parse(""); // => false
schema.parse(undefined); // => false
schema.parse(null); // => false
```

`"true"`/`"false"` 같은 문자열을 뜻에 맞게 불리언으로 해석하려면 아래 [`z.stringbool()`](#문자열-불리언-stringbools)을 쓴다. 변환 로직을 완전히 직접 제어하려면 `z.transform()`이나 `z.pipe()`를 고려한다. 자세한 내용은 [「변환」](04_refinements_transforms_codecs.md#변환-transforms)을 참고한다.

#### 입력 타입 지정하기

`z.coerce` 스키마는 어떤 값이든 받아 변환하므로, 입력(input) 타입, 즉 파싱 전에 받는 값의 타입이 기본적으로 `unknown`이다. 더 구체적인 입력 타입이 필요하면 제네릭 매개변수로 지정한다. 아래 예제의 `z.input`과 `z.output`은 [「타입 추론」](01_intro_and_basics.md#타입-추론-inferring-types)에서 설명한 대로 스키마의 입력 타입과 출력 타입을 꺼낸다.

```ts
const A = z.coerce.number();
type AInput = z.input<typeof A>; // => unknown

const B = z.coerce.number<number>();
type BInput = z.input<typeof B>; // => number
```

제네릭은 입력 타입만 바꾼다. 출력 타입은 제네릭 지정 여부와 관계없이 같다.

```ts
const regularCoerce = z.coerce.string();
type RegularInput = z.input<typeof regularCoerce>; // => unknown
type RegularOutput = z.output<typeof regularCoerce>; // => string

const customInput = z.coerce.string<string>();
type CustomInput = z.input<typeof customInput>; // => string
type CustomOutput = z.output<typeof customInput>; // => string
```

## 리터럴 (Literals)

리터럴 스키마는 `"hello world"`나 `5`처럼 값 하나로 정해진 [리터럴 타입](https://www.typescriptlang.org/docs/handbook/2/everyday-types.html#literal-types)을 표현한다. 이 스키마는 지정한 값과 정확히 같은 입력만 통과시킨다.

```ts
const tuna = z.literal("tuna");
const twelve = z.literal(12);
const twobig = z.literal(2n);
const tru = z.literal(true);
```

`null`과 `undefined` 리터럴은 다음 스키마로 표현한다.

```ts
z.null();
z.undefined();
z.void(); // z.undefined()와 동일
```

여러 리터럴 값을 허용하려면 배열을 넘긴다.

```ts
const colors = z.literal(["red", "green", "blue"]);

colors.parse("green"); // ✅
colors.parse("yellow"); // ❌
```

리터럴 스키마에서 허용 값 집합을 꺼내려면 `.values`를 쓴다.

```ts
colors.values; // => Set<"red" | "green" | "blue">
```

```ts
// Zod Mini
// 대응하는 API 없음
```

## 문자열 (Strings)

Zod는 몇 가지 내장 문자열 검증 API와 변환 API를 제공한다. 자주 쓰는 문자열 검증은 다음과 같다.

```ts
z.string().max(5);
z.string().min(5);
z.string().length(5);
z.string().nonempty(); // .min(1)의 별칭
z.string().regex(/^[a-z]+$/);
z.string().startsWith("aaa");
z.string().endsWith("zzz");
z.string().includes("---");
z.string().uppercase();
z.string().lowercase();
```

Zod Mini에는 이런 검사 메서드가 없다. 대신 같은 검사를 함수로 만들어 `.check()`에 넘긴다. 자세한 내용은 [「`.check()`」](07_packages_and_compile.md#check)에서 설명한다.

```ts
// Zod Mini
z.string().check(z.maxLength(5));
z.string().check(z.minLength(5));
z.string().check(z.length(5));
z.string().check(z.minLength(1)); // .nonempty()의 별칭
z.string().check(z.regex(/^[a-z]+$/));
z.string().check(z.startsWith("aaa"));
z.string().check(z.endsWith("zzz"));
z.string().check(z.includes("---"));
z.string().check(z.uppercase());
z.string().check(z.lowercase());
```

길이는 UTF-16 코드 단위가 아니라 유니코드 코드 포인트를 기준으로 센다. 코드 포인트는 유니코드가 문자마다 붙인 번호이고, UTF-16 코드 단위는 JavaScript 문자열이 그 번호를 저장하는 16비트 조각이다. 기본 다국어 평면(BMP, U+0000~U+FFFF) 밖의 이모지는 코드 단위 두 개로 저장되어 JavaScript의 `"😀".length`는 2지만, Zod는 이를 1로 센다.

반대로 화면에서는 한 글자로 보여도 코드 포인트 여러 개로 이루어진 문자는 여러 개로 센다. 앞 글자에 붙어 악센트 같은 부호를 더하는 결합 문자(combining character)나, 이모지 여러 개를 하나로 이어 주는 보이지 않는 문자 ZWJ(zero-width joiner)가 들어간 시퀀스가 그렇다.

```ts
z.string().length(1).parse("😀"); // 코드 포인트 1개, UTF-16 단위 2개
z.string().length(2).parse("é"); // "é" — e와 결합용 양음 부호
z.string().length(3).parse("🧑‍🍼"); // 사람 + ZWJ + 젖병
```

앞의 검사는 값을 확인만 하지만, 다음 변환 API는 값 자체를 바꿔서 반환한다.

```ts
z.string().trim(); // 앞뒤 공백 제거
z.string().toLowerCase(); // 소문자로 변환
z.string().toUpperCase(); // 대문자로 변환
z.string().normalize(); // 유니코드 정규화
```

```ts
// Zod Mini
z.string().check(z.trim()); // 앞뒤 공백 제거
z.string().check(z.toLowerCase()); // 소문자로 변환
z.string().check(z.toUpperCase()); // 대문자로 변환
z.string().check(z.normalize()); // 유니코드 정규화
```

유니코드 정규화(normalization)는 같은 글자를 나타내는 여러 코드 포인트 조합을 한 가지 형태로 맞추는 작업이다. 앞 예제처럼 e와 결합용 부호로 이루어진 "é"와 코드 포인트 하나짜리 "é"는 화면에서는 같아 보이지만, 정규화하기 전에는 서로 다른 문자열이다.

## 문자열 포맷 (String formats)

자주 쓰는 문자열 포맷은 다음 API로 검증한다. 이 가운데 몇 가지는 아래 소절에서 따로 설명한다.

```ts
z.email();
z.uuid();
z.url();
z.httpUrl();       // http 또는 https URL만 허용
z.hostname();
z.e164();          // E.164 전화번호
z.emoji();         // 이모지 한 글자 검증
z.base64();
z.base64url();
z.hex();
z.jwt();
z.nanoid();
z.cuid();
z.cuid2();
z.ulid();
z.ipv4();
z.ipv6();
z.mac();
z.cidrv4();        // IPv4 CIDR 블록
z.cidrv6();        // IPv6 CIDR 블록
z.creditCard();    // 신용카드 번호 (Luhn 체크섬)
z.currencyCode();  // ISO 4217 통화 코드
z.iban();          // IBAN (ISO 7064 mod-97 체크섬)
z.hash("sha256");  // 또는 "sha1", "sha384", "sha512", "md5"
z.iso.date();
z.iso.time();
z.iso.datetime();
z.iso.duration();
```

### 이메일 (Emails)

이메일 주소는 `z.email()`로 검증한다.

```ts
z.email();
```

Zod는 기본적으로 비교적 엄격한 정규식을 쓴다. 이 정규식은 흔한 문자로 이루어진 일반적인 이메일 주소를 검증하도록 설계되었고, 규칙은 Gmail과 대체로 같다. 이 정규식의 자세한 배경은 [이 글](https://colinhacks.com/essays/reasonable-email-regex)을 참고한다.

```ts
/^(?!\.)(?!.*\.\.)([a-z0-9_'+\-\.]*)[a-z0-9_+-]@([a-z0-9][a-z0-9\-]*\.)+[a-z]{2,}$/i
```

검증 동작을 바꾸려면 `pattern` 매개변수에 원하는 정규식을 넘긴다.

```ts
z.email({ pattern: /your regex here/ });
```

`pattern`에 넘길 만한 정규식 몇 가지는 Zod가 `z.regexes`로 내보낸다.

```ts
// Zod의 기본 이메일 정규식
z.email();
z.email({ pattern: z.regexes.email }); // 위와 동일

// 브라우저가 input[type=email] 필드를 검증할 때 쓰는 정규식
// https://developer.mozilla.org/en-US/docs/Web/HTML/Element/input/email
z.email({ pattern: z.regexes.html5Email });

// emailregex.com의 고전적인 정규식 (RFC 5322)
z.email({ pattern: z.regexes.rfc5322Email });

// 유니코드를 허용하는 느슨한 정규식 (국제화 이메일에 적합)
z.email({ pattern: z.regexes.unicodeEmail });
```

### UUID (UUIDs)

UUID는 `z.uuid()`로 검증한다.

```ts
z.uuid();
```

특정 UUID 버전만 허용하려면 `version`을 지정한다.

```ts
// "v1", "v2", "v3", "v4", "v5", "v6", "v7", "v8" 지원
z.uuid({ version: "v4" });

// 편의 API
z.uuidv4();
z.uuidv6();
z.uuidv7();
```

RFC 9562/4122 UUID 명세는 8번째 바이트의 앞 두 비트가 `10`이어야 한다고 규정한다. 생김새만 UUID와 같은 다른 식별자는 이 제약을 따르지 않는 경우가 있다. 이런 식별자까지 모두 허용하려면 `z.guid()`를 쓴다.

```ts
z.guid();
```

### URL (URLs)

브라우저와 Node.js의 `URL` 클래스가 따르는 WHATWG URL 표준에 맞는 URL은 `z.url()`로 검증한다.

```ts
const schema = z.url();

schema.parse("https://example.com"); // ✅
schema.parse("http://localhost"); // ✅
schema.parse("mailto:noreply@zod.dev"); // ✅
```

위 예제처럼 `z.url()`은 `localhost`나 `mailto:` 주소까지 받을 만큼 꽤 관대하다. 내부적으로 `new URL()` 생성자가 입력을 해석할 수 있는지로 검증하기 때문이다. 그래서 결과가 플랫폼과 런타임에 따라 다를 수 있지만, 해당 JS 런타임이나 엔진에서 URI/URL을 검증하는 방법으로는 이 방식이 가장 엄밀한 편이다.

호스트 이름을 특정 정규식으로 검증하려면 `hostname` 매개변수를 쓴다.

```ts
const schema = z.url({ hostname: /^example\.com$/ });

schema.parse("https://example.com"); // ✅
schema.parse("https://zombo.com"); // ❌
```

프로토콜을 특정 정규식으로 검증하려면 `protocol` 매개변수를 쓴다.

```ts
const schema = z.url({ protocol: /^https$/ });

schema.parse("https://example.com"); // ✅
schema.parse("http://example.com"); // ❌
```

> **참고:** 웹 URL만 검증해야 하는 경우가 많다. 이때는 `z.httpUrl()`을 쓴다.
>
> ```ts
> z.httpUrl();
>
> // 다음과 동일
> z.url({
>   protocol: /^https?$/,
>   hostname: z.regexes.domain
> });
> ```
>
> 프로토콜을 `http`/`https`로 제한하고, 호스트 이름이 `z.regexes.domain` 정규식에 맞는 유효한 도메인 이름인지 확인한다.
>
> ```ts
> /^(?=.{1,253}$)([a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,63}$/
> ```

URL을 정규화하려면 `normalize` 플래그를 쓴다. 이 플래그를 켜면 입력 값을 `new URL()`이 반환하는 정규화된 URL로 덮어쓴다. 아래 예제처럼 `new URL()`은 스킴과 호스트를 소문자로 바꾸고, 기본 포트와 `./`, `../` 경로 조각을 정리하며, 공백 같은 문자를 퍼센트 인코딩한다.

```ts
new URL("HTTP://ExAmPle.com:80/./a/../b?X=1#f oo").href
// => "http://example.com/b?X=1#f%20oo"
```

### 전화번호 (Phone numbers)

E.164는 ITU-T가 정한 국제 전화번호 표기 형식으로, `+`와 국가 코드 뒤에 번호를 구분자 없이 붙여 쓴다. 이 형식의 전화번호는 `z.e164()`로 검증한다.

```ts
const phone = z.e164();

phone.parse("+15555555555"); // ✅
phone.parse("555-555-5555"); // ❌
```

`z.e164()`는 `+`로 시작하고, 0이 아닌 국가 코드를 포함해 숫자가 모두 7~15자리인 문자열을 통과시킨다.

### ISO 날짜시간 (ISO datetimes)

Zod의 문자열 검증에는 날짜/시간 관련 검증이 몇 가지 있다. 정규식 기반이라 완전한 날짜/시간 라이브러리만큼 엄격하지는 않지만, 사용자 입력을 검증하기에는 매우 편리하다.

`z.iso.datetime()`은 날짜와 시간 표기에 관한 국제 표준인 ISO 8601 가운데 엄격한 부분집합만 받는다. 기본적으로 UTC를 뜻하는 `Z`만 받고, `+02:00`처럼 UTC와의 시차를 덧붙이는 시간대 오프셋(offset)은 허용하지 않는다.

```ts
const datetime = z.iso.datetime();

datetime.parse("2020-01-01T06:15:00Z"); // ✅
datetime.parse("2020-01-01T06:15:00.123Z"); // ✅
datetime.parse("2020-01-01T06:15:00.123456Z"); // ✅ (임의 정밀도)
datetime.parse("2020-01-01T06:15:00+02:00"); // ❌ (오프셋 불허)
datetime.parse("2020-01-01T06:15:00"); // ❌ (로컬 시간 불허)
```

시간대 오프셋을 허용하려면 `offset: true`를 지정한다.

```ts
const datetime = z.iso.datetime({ offset: true });

// 시간대 오프셋 허용
datetime.parse("2020-01-01T06:15:00+02:00"); // ✅

// 기본 형식(basic format) 오프셋은 불허
datetime.parse("2020-01-01T06:15:00+02");    // ❌
datetime.parse("2020-01-01T06:15:00+0200");  // ❌

// Z는 여전히 지원
datetime.parse("2020-01-01T06:15:00Z"); // ✅ 
```

위 주석의 기본 형식(basic format)은 ISO 8601에서 `+0200`처럼 콜론 없이 쓰는 표기를 말한다. `offset: true`를 지정해도 `+02:00`처럼 시와 분을 콜론으로 구분한 오프셋만 받는다.

앞에서 거부된 로컬 시간처럼 시간대 정보가 없는(unqualified) 날짜시간을 허용하려면 `local: true`를 지정한다.

```ts
const schema = z.iso.datetime({ local: true });
schema.parse("2020-01-01T06:15:01"); // ✅
schema.parse("2020-01-01T06:15"); // ✅ 초는 생략 가능
schema.parse("2020-01-01T06:15:00Z"); // ✅
schema.parse("2020-01-01T06:15Z"); // ❌ (`Z`가 있으면 초가 필요)
```

허용할 시간 정밀도는 `precision`으로 제한한다. 값은 초 아래 소수 자릿수를 뜻하며, `0`이면 초까지, `3`이면 밀리초까지, `-1`이면 초 없이 분까지만 받는다. 기본적으로 초는 필수이고 초 미만 정밀도는 제한이 없다. RFC 3339는 `Z`나 오프셋이 있으면 초를 반드시 요구하므로, 초를 생략할 수 있는 것은 `local`이 추가하는 시간대 없는 형식뿐이다.

```ts
const a = z.iso.datetime();
a.parse("2020-01-01T06:15Z"); // ❌ (초 필수)
a.parse("2020-01-01T06:15:00Z"); // ✅
a.parse("2020-01-01T06:15:00.123Z"); // ✅

const b = z.iso.datetime({ precision: -1 }); // 분 정밀도 (초 없음)
b.parse("2020-01-01T06:15Z"); // ✅
b.parse("2020-01-01T06:15:00Z"); // ❌
b.parse("2020-01-01T06:15:00.123Z"); // ❌

const c = z.iso.datetime({ precision: 0 }); // 초 정밀도만
c.parse("2020-01-01T06:15Z"); // ❌
c.parse("2020-01-01T06:15:00Z"); // ✅
c.parse("2020-01-01T06:15:00.123Z"); // ❌

const d = z.iso.datetime({ precision: 3 }); // 밀리초 정밀도만
d.parse("2020-01-01T06:15Z"); // ❌
d.parse("2020-01-01T06:15:00Z"); // ❌
d.parse("2020-01-01T06:15:00.123Z"); // ✅
```

`precision` 하나로는 분 정밀도와 나머지 형식을 모두 덮을 수 없다. 둘을 함께 받으려면 두 스키마를 [유니언](03_schemas_objects_collections.md#유니언-unions)으로 묶는다. 유니언은 여러 스키마 가운데 하나라도 통과하면 입력을 받아들이는 스키마다.

```ts
const mixed = z.union([z.iso.datetime(), z.iso.datetime({ precision: -1 })]);
mixed.parse("2020-01-01T06:15Z"); // ✅
mixed.parse("2020-01-01T06:15:00.123Z"); // ✅
mixed.parse("2020-01-01T06:15"); // ❌ (여전히 시간대 정보 필요)
```

### ISO 날짜 (ISO dates)

`z.iso.date()`는 `YYYY-MM-DD` 형식의 문자열을 검증한다.

```ts
const date = z.iso.date();

date.parse("2020-01-01"); // ✅
date.parse("2020-1-1"); // ❌
date.parse("2020-01-32"); // ❌
```

### ISO 시간 (ISO times)

`z.iso.time()`은 `HH:MM[:SS[.s+]]` 형식의 문자열을 검증한다. 대괄호로 묶은 초와 초 미만 소수부는 기본적으로 생략할 수 있다.

```ts
const time = z.iso.time();

time.parse("03:15"); // ✅
time.parse("03:15:00"); // ✅
time.parse("03:15:00.9999999"); // ✅ (임의 정밀도)
```

`z.iso.datetime()`과 달리 `Z`를 포함해 어떤 오프셋도 허용하지 않는다.

```ts
time.parse("03:15:00Z"); // ❌ (`Z` 불허)
time.parse("03:15:00+02:00"); // ❌ (오프셋 불허)
```

소수부 정밀도는 `precision` 매개변수로 제한한다.

```ts
z.iso.time({ precision: -1 }); // HH:MM (분 정밀도)
z.iso.time({ precision: 0 });  // HH:MM:SS (초 정밀도)
z.iso.time({ precision: 1 });  // HH:MM:SS.s (0.1초 정밀도)
z.iso.time({ precision: 2 });  // HH:MM:SS.ss (0.01초 정밀도)
z.iso.time({ precision: 3 });  // HH:MM:SS.sss (밀리초 정밀도)
```

### IP 주소 (IP addresses)

```ts
const ipv4 = z.ipv4();
ipv4.parse("192.168.0.0"); // ✅

const ipv6 = z.ipv6();
ipv6.parse("2001:db8:85a3::8a2e:370:7334"); // ✅
```

### IP 블록 (IP blocks (CIDR))

[CIDR 표기법](https://en.wikipedia.org/wiki/Classless_Inter-Domain_Routing)으로 지정한 IP 주소 범위를 검증한다. CIDR 표기법은 `192.168.0.0/24`처럼 주소 뒤에 `/`와 네트워크 부분의 비트 수를 붙여 범위를 나타낸다.

```ts
const cidrv4 = z.cidrv4();
cidrv4.parse("192.168.0.0/24"); // ✅

const cidrv6 = z.cidrv6();
cidrv6.parse("2001:db8::/32"); // ✅
```

### MAC 주소 (MAC Addresses)

표준 48비트 [IEEE 802](https://en.wikipedia.org/wiki/MAC_address) MAC 주소를 검증한다.

```ts
const mac = z.mac(); 
mac.parse("00:1A:2B:3C:4D:5E");  // ✅
mac.parse("00-1a-2b-3c-4d-5e");  // ❌ 기본 구분자는 콜론
mac.parse("001A:2B3C:4D5E");     // ❌ 표준 형식만 허용
mac.parse("00:1A:2b:3C:4d:5E");  // ❌ 대소문자 혼용 불허

// 구분자 지정
const dashMac = z.mac({ delimiter: "-" });
dashMac.parse("00-1A-2B-3C-4D-5E"); // ✅
```

### 신용카드 번호 (Credit card numbers)

12~19자리 숫자이면서 [Luhn](https://en.wikipedia.org/wiki/Luhn_algorithm) 체크섬이 유효한 카드 번호를 검증한다. Luhn 체크섬은 번호의 마지막 자리를 나머지 자리에서 계산해 두는 방식으로, 숫자 하나를 잘못 입력한 경우를 잡아낸다. 발급사는 식별하지 않으므로 어느 카드 브랜드든 통과한다.

```ts
const card = z.creditCard();
card.parse("4111111111111111");     // ✅
card.parse("4111 1111 1111 1111");  // ✅ 공백 하나씩
card.parse("4111-1111-1111-1111");  // ✅ 하이픈 하나씩
card.parse("4111  1111 1111 1111"); // ❌ 구분자 반복 불허
card.parse(" 4111111111111111");    // ❌ 앞뒤 공백 불허
card.parse("4111.1111.1111.1111");  // ❌ 구분자는 공백과 하이픈만
card.parse("4111111111111112");     // ❌ 체크섬 실패
```

### 통화 코드 (Currency codes)

`z.currencyCode()`는 세 글자 ISO 4217 통화 코드를 검증한다.

```ts
const currency = z.currencyCode();

currency.parse("USD"); // ✅
currency.parse("usd"); // ❌
currency.parse("ANG"); // ❌ ISO 4217에서 폐지됨
```

### IBAN (IBANs)

국제 은행 계좌 번호(IBAN)를 검증한다. 입력은 2글자 국가 코드, 2자리 검증 숫자, 11~30자의 영숫자로 이루어져야 하고, [ISO 7064 MOD 97-10](https://en.wikipedia.org/wiki/International_Bank_Account_Number#Validating_the_IBAN) 체크섬도 맞아야 한다. 다만 국가 레지스트리는 조회하지 않으므로 어떤 국가 코드든 받고, 국가별 계좌 길이도 강제하지 않는다. 표기는 공백 없이 붙여 쓰는 전자 형식(electronic format)만 허용한다.

```ts
const iban = z.iban();
iban.parse("DE89370400440532013000");      // ✅
iban.parse("NO9386011117947");             // ✅
iban.parse("DE89 3704 0044 0532 0130 00"); // ❌ 공백 불허
iban.parse("de89370400440532013000");      // ❌ 대문자만 허용
iban.parse("DE89370400440532013001");      // ❌ 체크섬 실패
iban.parse("DE00000000000000000066");      // ❌ 검증 숫자는 항상 02–98
```

### JWT (JWTs)

[JSON Web Token](https://jwt.io/)을 검증한다.

```ts
z.jwt();
z.jwt({ alg: "HS256" });
```

### 해시 (Hashes)

암호학적 해시 값을 검증한다.

```ts
z.hash("md5");
z.hash("sha1");
z.hash("sha256");
z.hash("sha384");
z.hash("sha512");
```

`z.hash()`는 관례에 따라 기본적으로 16진수(hex) 인코딩을 기대한다. 다른 인코딩은 `enc` 매개변수로 지정한다.

```ts
z.hash("sha256", { enc: "hex" });       // 기본값
z.hash("sha256", { enc: "base64" });    // base64 인코딩
z.hash("sha256", { enc: "base64url" }); // base64url 인코딩 (패딩 없음)
```

알고리즘과 인코딩별 기대 길이와 패딩은 다음과 같다.

- `"md5"`
  - `"hex"`: 32
  - `"base64"`: 24 (22 + "==")
  - `"base64url"`: 22
- `"sha1"`
  - `"hex"`: 40
  - `"base64"`: 28 (27 + "=")
  - `"base64url"`: 27
- `"sha256"`
  - `"hex"`: 64
  - `"base64"`: 44 (43 + "=")
  - `"base64url"`: 43
- `"sha384"`
  - `"hex"`: 96
  - `"base64"`: 64 (패딩 없음)
  - `"base64url"`: 64
- `"sha512"`
  - `"hex"`: 128
  - `"base64"`: 88 (86 + "==")
  - `"base64url"`: 86

### 사용자 정의 포맷 (Custom formats)

직접 문자열 포맷을 정의하려면 `z.stringFormat()`을 쓴다.

```ts
const coolId = z.stringFormat("cool-id", (val)=>{
  // 임의의 검증 로직
  return val.length === 100 && val.startsWith("cool-");
});

// 정규식도 받는다
z.stringFormat("cool-id", /^cool-[a-z0-9]{95}$/);
```

`z.stringFormat()`으로 만든 스키마는 검증에 실패하면 `"invalid_format"` 이슈(`ZodError`에 담기는 개별 검증 실패 항목)를 만든다. 이 이슈는 `format` 필드에 포맷 이름을 담으므로, 정제(refinement)나 `z.custom()`이 만드는 `"custom"` 에러보다 내용이 구체적이다.

```ts
myFormat.parse("invalid input!");
// ZodError: [
//   {
//     "code": "invalid_format",
//     "format": "cool-id",
//     "path": [],
//     "message": "Invalid cool-id"
//   }
// ]
```

정제와 `z.custom()`은 [「정제」](04_refinements_transforms_codecs.md#정제-refinements)와 [「사용자 정의 스키마」](04_refinements_transforms_codecs.md#사용자-정의-스키마-custom)에서, 이슈 구조는 [「ZodError와 이슈」](05_errors.md#zoderror와-이슈-zoderror-and-issues)에서 설명한다.

## 템플릿 리터럴 (Template literals)

> `zod@4.0`에서 도입되었다.

템플릿 리터럴 스키마는 TypeScript의 템플릿 리터럴 타입처럼 정해진 패턴의 문자열만 허용한다. 고정 문자열과 스키마를 배열에 차례로 넣어 정의하며, 아래 예제의 주석이 추론되는 타입이다.

```ts
const schema = z.templateLiteral([ "hello, ", z.string(), "!" ]);
// `hello, ${string}!`
```

`z.templateLiteral`은 문자열 리터럴(예: `"hello"`)과 스키마를 개수 제한 없이 받는다. 추론된 타입이 `string | number | bigint | boolean | null | undefined`에 할당 가능한 스키마라면 무엇이든 넘길 수 있다.

```ts
z.templateLiteral([ "hi there" ]);
// `hi there`

z.templateLiteral([ "email: ", z.string() ]);
// `email: ${string}`

z.templateLiteral([ "high", z.literal(5) ]);
// `high5`

z.templateLiteral([ z.nullable(z.literal("grassy")) ]);
// `grassy` | `null`

z.templateLiteral([ z.number(), z.enum(["px", "em", "rem"]) ]);
// `${number}px` | `${number}em` | `${number}rem`
```

## 숫자 (Numbers)

숫자는 `z.number()`로 검증한다. `NaN`과 `Infinity`는 거부하고 유한한 숫자만 허용한다.

```ts
const schema = z.number();

schema.parse(3.14);      // ✅
schema.parse(NaN);       // ❌
schema.parse(Infinity);  // ❌
```

숫자 전용 검증은 다음과 같다.

```ts
z.number().gt(5);
z.number().gte(5);                     // 별칭 .min(5)
z.number().lt(5);
z.number().lte(5);                     // 별칭 .max(5)
z.number().positive();                 // 별칭 .gt(0)
z.number().nonnegative();    
z.number().negative(); 
z.number().nonpositive(); 
z.number().multipleOf(5);              // 별칭 .step(5)
```

```ts
// Zod Mini
z.number().check(z.gt(5));
z.number().check(z.gte(5));            // 별칭 .minimum(5)
z.number().check(z.lt(5));
z.number().check(z.lte(5));            // 별칭 .maximum(5)
z.number().check(z.positive());        // 별칭 .gt(0)
z.number().check(z.nonnegative()); 
z.number().check(z.negative()); 
z.number().check(z.nonpositive()); 
z.number().check(z.multipleOf(5));     // 별칭 .step(5)
```

어떤 이유로 `NaN`을 검증해야 한다면 `z.nan()`을 쓴다.

```ts
z.nan().parse(NaN);              // ✅
z.nan().parse("anything else");  // ❌
```

## 정수 (Integers)

정수는 다음 API로 검증한다.

```ts
z.int();     // 안전한 정수(safe integer) 범위로 제한
z.int32();   // int32 범위로 제한
```

안전한 정수는 JavaScript `number`가 정밀도 손실 없이 표현할 수 있는 정수로, `Number.MIN_SAFE_INTEGER`(-(2^53 - 1))부터 `Number.MAX_SAFE_INTEGER`(2^53 - 1)까지다. int32 범위는 32비트 부호 있는 정수의 범위인 -2^31부터 2^31 - 1까지다.

## BigInt (BigInts)

BigInt는 `number`로 정확히 표현할 수 없는 큰 정수를 다루는 JavaScript 원시 타입으로, `5n`처럼 숫자 뒤에 `n`을 붙여 쓴다. BigInt 값은 `z.bigint()`로 검증한다.

```ts
z.bigint();
```

BigInt 전용 검증은 다음과 같다.

```ts
z.bigint().gt(5n);
z.bigint().gte(5n);                    // 별칭 `.min(5n)`
z.bigint().lt(5n);
z.bigint().lte(5n);                    // 별칭 `.max(5n)`
z.bigint().positive();                 // 별칭 `.gt(0n)`
z.bigint().nonnegative(); 
z.bigint().negative(); 
z.bigint().nonpositive(); 
z.bigint().multipleOf(5n);             // 별칭 `.step(5n)`
```

```ts
// Zod Mini
z.bigint().check(z.gt(5n));
z.bigint().check(z.gte(5n));           // 별칭 `.minimum(5n)`
z.bigint().check(z.lt(5n));
z.bigint().check(z.lte(5n));           // 별칭 `.maximum(5n)`
z.bigint().check(z.positive());        // 별칭 `.gt(0n)`
z.bigint().check(z.nonnegative());
z.bigint().check(z.negative());
z.bigint().check(z.nonpositive());
z.bigint().check(z.multipleOf(5n));    // 별칭 `.step(5n)`
```

## 불리언 (Booleans)

불리언 값은 `z.boolean()`으로 검증한다.

```ts
z.boolean().parse(true); // => true
z.boolean().parse(false); // => false
```

## 날짜 (Dates)

`Date` 인스턴스는 `z.date()`로 검증한다. 날짜처럼 생긴 문자열은 `Date` 인스턴스가 아니므로 통과하지 못한다.

```ts
z.date().safeParse(new Date()); // success: true
z.date().safeParse("2022-01-12T06:15:00.000Z"); // success: false
```

에러 메시지는 `error` 매개변수로 바꾼다. 아래 예제는 입력이 없을 때(`undefined`)와 날짜가 아닐 때 서로 다른 메시지를 돌려준다. `error` 매개변수 전반은 [「`error` 파라미터」](05_errors.md#error-파라미터-the-error-param)를 참고한다.

```ts
z.date({
  error: issue => issue.input === undefined ? "Required" : "Invalid date"
});
```

날짜 전용 검증은 다음과 같다.

```ts
z.date().min(new Date("1900-01-01"), { error: "Too old!" });
z.date().max(new Date(), { error: "Too young!" });
```

```ts
// Zod Mini
z.date().check(z.minimum(new Date("1900-01-01"), { error: "Too old!" }));
z.date().check(z.maximum(new Date(), { error: "Too young!" }));
```

## 열거형 (Enums)

`z.enum`은 입력이 정해진 문자열 값 집합에 속하는지 검증한다.

```ts
const FishEnum = z.enum(["Salmon", "Tuna", "Trout"]);

FishEnum.parse("Salmon"); // => "Salmon"
FishEnum.parse("Swordfish"); // => ❌
```

> **주의:** 문자열 배열을 변수로 먼저 선언하면 TypeScript가 그 변수의 타입을 `string[]`으로 넓혀 추론하므로, Zod도 각 원소의 정확한 값을 알 수 없다.
>
> ```ts
> const fish = ["Salmon", "Tuna", "Trout"];
>
> const FishEnum = z.enum(fish);
> type FishEnum = z.infer<typeof FishEnum>; // string
> ```
>
> 배열을 항상 `z.enum()`에 직접 넘기거나, 배열의 타입을 읽기 전용 리터럴 타입으로 고정하는 [`as const`](https://www.typescriptlang.org/docs/handbook/release-notes/typescript-3-4.html#const-assertions) 단언을 붙인다.
>
> ```ts
> const fish = ["Salmon", "Tuna", "Trout"] as const;
>
> const FishEnum = z.enum(fish);
> type FishEnum = z.infer<typeof FishEnum>; // "Salmon" | "Tuna" | "Trout"
> ```

열거형 형태의 객체 리터럴(`{ [key: string]: string | number }`)도 받는다.

```ts
const Fish = {
  Salmon: 0,
  Tuna: 1
} as const

const FishEnum = z.enum(Fish)
FishEnum.parse(Fish.Salmon); // => ✅
FishEnum.parse(0); // => ✅
FishEnum.parse(2); // => ❌
```

외부에서 선언한 TypeScript enum도 넘길 수 있다.

```ts
enum Fish {
  Salmon = 0,
  Tuna = 1
}

const FishEnum = z.enum(Fish);
FishEnum.parse(Fish.Salmon); // => ✅
FishEnum.parse(0); // => ✅
FishEnum.parse(2); // => ❌
```

> **참고:** 외부에서 선언한 TypeScript enum에도 `z.enum()`을 쓴다. `z.nativeEnum()` API는 폐기 예정(deprecated)이다.
>
> TypeScript의 `enum` 키워드 자체는 [권장하지 않는다](https://www.totaltypescript.com/why-i-dont-like-typescript-enums).

```ts
enum Fish {
  Salmon = "Salmon",
  Tuna = "Tuna",
  Trout = "Trout",
}

const FishEnum = z.enum(Fish);
```

### `.enum`

스키마의 값을 열거형 형태의 객체로 꺼낸다.

```ts
const FishEnum = z.enum(["Salmon", "Tuna", "Trout"]);

FishEnum.enum;
// => { Salmon: "Salmon", Tuna: "Tuna", Trout: "Trout" }
```

```ts
// Zod Mini
const FishEnum = z.enum(["Salmon", "Tuna", "Trout"]);

FishEnum.def.entries;
// => { Salmon: "Salmon", Tuna: "Tuna", Trout: "Trout" }
```

### `.exclude()`

특정 값을 제외한 새 열거형 스키마를 만든다.

```ts
const FishEnum = z.enum(["Salmon", "Tuna", "Trout"]);
const TunaOnly = FishEnum.exclude(["Salmon", "Trout"]);
```

```ts
// Zod Mini
// 대응하는 API 없음
```

### `.extract()`

특정 값만 추려 새 열거형 스키마를 만든다.

```ts
const FishEnum = z.enum(["Salmon", "Tuna", "Trout"]);
const SalmonAndTroutOnly = FishEnum.extract(["Salmon", "Trout"]);
```

```ts
// Zod Mini
// 대응하는 API 없음
```

## 문자열 불리언 (Stringbools)

> `zod@4.0`에서 도입되었다.

환경 변수를 파싱할 때처럼 `"true"`, `"yes"` 같은 불리언 모양의 문자열을 일반 `boolean`으로 바꿔야 하는 경우가 있다. 이때 `z.stringbool()`을 쓴다. 앞의 [`z.coerce.boolean()`](#강제-변환의-동작-방식)과 달리 정해진 문자열만 불리언으로 바꾸고, 목록에 없는 값은 에러로 처리한다.

```ts
const strbool = z.stringbool();

strbool.parse("true")         // => true
strbool.parse("1")            // => true
strbool.parse("yes")          // => true
strbool.parse("on")           // => true
strbool.parse("y")            // => true
strbool.parse("enabled")      // => true

strbool.parse("false");       // => false
strbool.parse("0");           // => false
strbool.parse("no");          // => false
strbool.parse("off");         // => false
strbool.parse("n");           // => false
strbool.parse("disabled");    // => false

strbool.parse(/* 그 밖의 값 */); // ZodError<[{ code: "invalid_value" }]>
```

`true`와 `false`로 바꿀 문자열 목록은 `truthy`, `falsy` 옵션으로 직접 지정할 수 있다. 아래 값이 기본값이다.

```ts
// 기본값
z.stringbool({
  truthy: ["true", "1", "yes", "on", "y", "enabled"],
  falsy: ["false", "0", "no", "off", "n", "disabled"],
});
```

기본적으로 대소문자를 구분하지 않는다. 모든 입력을 소문자로 바꾼 뒤 `truthy`/`falsy` 값과 비교한다. 대소문자를 구분하려면 `case: "sensitive"`를 지정한다.

```ts
z.stringbool({
  case: "sensitive"
});
```

## 옵셔널 (Optionals)

스키마를 옵셔널(optional)로 만들어 `undefined` 입력을 허용하려면 다음과 같이 쓴다.

```ts
z.optional(z.literal("yoda")); // 또는 z.literal("yoda").optional()
```

```ts
// Zod Mini
z.optional(z.literal("yoda"));
```

이 API는 원래 스키마를 감싼 `ZodOptional` 인스턴스를 반환한다. 안쪽 스키마는 다음과 같이 꺼낸다.

```ts
optionalYoda.unwrap(); // ZodLiteral<"yoda">
```

```ts
// Zod Mini
optionalYoda.def.innerType; // ZodMiniLiteral<"yoda">
```

> **참고:** 객체에서 키가 아예 없는 것은 허용하되, 키는 있고 값이 `undefined`인 경우는 막으려면 `.exactOptional()`을 쓴다. TypeScript의 [`exactOptionalPropertyTypes`](https://www.typescriptlang.org/tsconfig/#exactOptionalPropertyTypes)와 같은 의미다.
>
> ```ts
> const User = z.object({
>   name: z.string().optional(),
>   nick: z.string().exactOptional(),
> });
> // { name?: string | undefined; nick?: string }
>
> User.parse({ name: undefined }); // ✅
> User.parse({ nick: undefined }); // ❌
> ```
>
> ```ts
> // Zod Mini
> const User = z.object({
>   name: z.optional(z.string()),
>   nick: z.exactOptional(z.string()),
> });
> // { name?: string | undefined; nick?: string }
>
> z.parse(User, { name: undefined }); // ✅
> z.parse(User, { nick: undefined }); // ❌
> ```

## 널러블 (Nullables)

스키마를 널러블(nullable)로 만들어 `null` 입력을 허용하려면 다음과 같이 쓴다.

```ts
z.nullable(z.literal("yoda")); // 또는 z.literal("yoda").nullable()
```

```ts
// Zod Mini
const nullableYoda = z.nullable(z.literal("yoda"));
```

이 API는 원래 스키마를 감싼 `ZodNullable` 인스턴스를 반환한다. 안쪽 스키마는 다음과 같이 꺼낸다.

```ts
nullableYoda.unwrap(); // ZodLiteral<"yoda">
```

```ts
// Zod Mini
nullableYoda.def.innerType; // ZodMiniLiteral<"yoda">
```

## 널리시 (Nullish)

옵셔널이면서 널러블인, 즉 `undefined`와 `null`을 모두 허용하는 널리시(nullish) 스키마는 다음과 같이 만든다. Zod와 Zod Mini의 표기가 같다.

```ts
const nullishYoda = z.nullish(z.literal("yoda"));
```

nullish 개념은 [TypeScript 매뉴얼](https://www.typescriptlang.org/docs/handbook/release-notes/typescript-3-7.html#nullish-coalescing)을 참고한다.

## 알 수 없는 값 (Unknown)

Zod는 TypeScript 타입 시스템을 일대일로 반영하는 것을 목표로 한다. 그래서 `any`, `unknown`, `never` 같은 특수 타입에 대응하는 API도 제공한다.

```ts
// 모든 값 허용
z.any(); // 추론 타입: `any`
z.unknown(); // 추론 타입: `unknown`
```

두 스키마 모두 어떤 입력이든 통과시키고, 차이는 추론되는 타입에 있다. `any`는 타입 검사를 꺼 버리므로 결과를 어떻게 써도 컴파일되지만, `unknown`은 타입을 좁히기 전에는 속성 접근 같은 연산을 할 수 없다.

객체 속성으로 쓰면 TypeScript의 `{ a: any }`와 마찬가지로 키가 필수다. 값이 `undefined`여도 키는 있어야 하며, 키를 생략하게 하려면 `.optional()`을 붙인다.

```ts
z.object({ a: z.any() }).parse({}); // ❌
z.object({ a: z.any() }).parse({ a: undefined }); // ✅
z.object({ a: z.any().optional() }).parse({}); // ✅
```

## 절대 불가 (Never)

`z.never()`는 어떤 값도 통과시키지 않는다.

```ts
z.never(); // 추론 타입: `never`
```

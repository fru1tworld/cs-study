# Zod 에러 커스터마이징과 포맷팅

> 원문: https://zod.dev/error-customization
> 원문: https://zod.dev/error-formatting

이 문서는 Zod 4(4.6.5 기준)에서 검증 에러 메시지를 바꾸는 방법과, 에러 객체를 다루기 쉬운 형태로 바꿔 주는 유틸리티를 정리한다. `parse`/`safeParse`로 에러를 받는 기초는 [「기본 사용법 (Basic usage)」](01_intro_and_basics.md#기본-사용법-basic-usage)을 참고한다.

## ZodError와 이슈 (ZodError and issues)

Zod에서 검증 에러는 `z.core.$ZodError` 클래스의 인스턴스로 나타난다. `z.core`는 Zod와 Zod Mini가 함께 쓰는 핵심 클래스를 모아 둔 하위 패키지이고, `$`로 시작하는 이름은 이 코어 쪽 클래스를 가리킨다([「Zod Core」](07_packages_and_compile.md#zod-core) 참고).

> **참고:** `zod` 패키지의 `ZodError` 클래스는 `$ZodError`의 하위 클래스이며, 편의 메서드 몇 가지를 추가로 구현한다.

검증에서 발견한 문제 하나하나를 Zod는 이슈(issue)라고 부른다. 입력 하나에 문제가 여러 개 있으면 이슈도 여러 개 생기고, 이 이슈들이 `$ZodError` 인스턴스의 `.issues` 배열에 담긴다. 각 이슈에는 사람이 읽을 수 있는 `message`와, 어떤 종류의 문제가(`code`) 어느 위치에서(`path`) 났는지 같은 구조화된 정보가 함께 들어 있다.

```ts
import * as z from "zod";

const result = z.string().safeParse(12); // { success: false, error: ZodError }
result.error.issues;
// [
//   {
//     expected: 'string',
//     code: 'invalid_type',
//     path: [],
//     message: 'Invalid input: expected string, received number'
//   }
// ]
```

이 문서의 예제에는 `// Zod Mini`로 시작하는 코드가 자주 함께 나온다. Zod Mini는 기능은 같지만 편의 메서드 대신 함수를 쓰도록 API를 바꿔, 쓰지 않는 코드를 번들에서 덜어 낼 수 있게 만든 변형이다. 예를 들어 일반 Zod의 `.min(5)`를 Zod Mini에서는 `.check(z.minLength(5))`로 쓴다. 자세한 차이는 [「Zod Mini」](07_packages_and_compile.md#zod-mini)에서 다룬다. 에러 쪽에서 보면 Zod Mini는 `ZodError`가 아니라 `$ZodError`를 그대로 쓰고, 기본 메시지도 `Invalid input`으로 짧다([「국제화」](#국제화-internationalization) 참고).

```ts
// Zod Mini
import * as z from "zod/mini";

const result = z.string().safeParse(12); // { success: false, error: z.core.$ZodError }
result.error.issues;
// [
//   {
//     expected: 'string',
//     code: 'invalid_type',
//     path: [],
//     message: 'Invalid input'
//   }
// ]
```

이슈의 `message`는 Zod가 기본으로 채워 주지만, 아래에서 설명하는 여러 방법으로 바꿀 수 있다.

## `error` 파라미터 (The `error` param)

가장 직접적인 방법은 스키마를 만들 때 메시지를 함께 넘기는 것이다. 사실상 모든 Zod API가 에러 메시지를 선택 인자로 받는다.

```ts
z.string("Not a string!");
```

이렇게 지정한 커스텀 메시지는 이 스키마에서 발생한 모든 검증 이슈의 `message`에 들어간다.

```ts
z.string("Not a string!").parse(12);
// ❌ ZodError를 던진다 {
//   issues: [
//     {
//       expected: 'string',
//       code: 'invalid_type',
//       path: [],
//       message: 'Not a string!'   <-- 👀 커스텀 에러 메시지
//     }
//   ]
// }
```

스키마를 만드는 `z` 함수뿐 아니라 `.min()` 같은 스키마 메서드도 커스텀 에러를 받는다.

```ts
z.string("Bad!");
z.string().min(5, "Too short!");
z.uuid("Bad UUID!");
z.iso.date("Bad date!");
z.array(z.string(), "Not an array!");
z.array(z.string()).min(5, "Too few items!");
z.set(z.string(), "Bad set!");
```

```ts
// Zod Mini
z.string("Bad!");
z.string().check(z.minLength(5, "Too short!"));
z.uuid("Bad UUID!");
z.iso.date("Bad date!");
z.array(z.string(), "Bad array!");
z.array(z.string()).check(z.minLength(5, "Too few items!"));
z.set(z.string(), "Bad set!");
```

문자열 대신 `{ error: ... }` 형태의 옵션 객체(params)를 넘겨도 결과는 같다.

```ts
z.string({ error: "Bad!" });
z.string().min(5, { error: "Too short!" });
z.uuid({ error: "Bad UUID!" });
z.iso.date({ error: "Bad date!" });
z.array(z.string(), { error: "Bad array!" });
z.array(z.string()).min(5, { error: "Too few items!" });
z.set(z.string(), { error: "Bad set!" });
```

```ts
// Zod Mini
z.string({ error: "Bad!" });
z.string().check(z.minLength(5, { error: "Too short!" }));
z.uuid({ error: "Bad UUID!" });
z.iso.date({ error: "Bad date!" });
z.array(z.string(), { error: "Bad array!" });
z.array(z.string()).check(z.minLength(5, { error: "Too few items!" }));
z.set(z.string(), { error: "Bad set!" });
```

`error` 파라미터에는 문자열 대신 함수를 넘길 수도 있다. Zod는 이렇게 메시지를 만들어 내는 함수를 **에러 맵(error map)** 이라고 부른다. 에러 맵은 스키마를 정의할 때가 아니라 파싱 중 검증 이슈가 생길 때마다 실행된다. 아래 예제라면 메시지에 들어가는 시각은 검증이 실패한 순간의 시각이다.

```ts
z.string({ error: ()=>`[${Date.now()}]: Validation failure.` });
```

에러 맵은 방금 생긴 이슈의 정보를 담은 객체(예제에서는 `iss`)를 인자로 받는다. 그래서 이슈 내용에 따라 메시지를 다르게 만들 수 있다. 아래 예제는 값이 아예 없을 때와 타입이 틀렸을 때 메시지를 나눈다.

```ts
z.string({
  error: (iss) => iss.input === undefined ? "Field is required." : "Invalid input."
});
```

더 세밀하게 분기해야 하면 `iss` 객체의 다른 속성도 쓸 수 있다. 아래 주석에 나오는 검사(check)는 `.min(5)`처럼 스키마에 덧붙이는 개별 규칙을 말한다. `z.string()`이 "문자열인가"를 검사하는 스키마라면, `.min(5)`는 그 스키마에 붙은 "5글자 이상인가" 검사다([「Zod Core: 검사」](07_packages_and_compile.md#검사-checks) 참고).

```ts
z.string({
  error: (iss) => {
    iss.code; // 이슈 코드
    iss.input; // 입력 데이터
    iss.inst; // 이 이슈를 발생시킨 스키마 또는 검사
    iss.schema; // 이 이슈를 소유한 스키마
    iss.path; // 에러 경로
  },
});
```

`iss.inst`와 `iss.schema`는 검사가 실패했을 때 달라진다. `z.string().min(5)`에서 `.min(5)`가 실패하면 `iss.inst`는 그 검사를 가리키지만, `iss.schema`는 검사가 붙어 있는 문자열 스키마를 가리킨다. 이처럼 `iss.schema`는 항상 스키마이므로, 이를 이용하면 이슈가 난 스키마에 붙인 메타데이터를 읽을 수 있다. 메타데이터 자체는 [「메타데이터와 레지스트리」](06_metadata_and_json_schema.md#메타데이터-metadata)를 참고한다.

아래 예제는 `.meta()`로 붙인 `title`을 전역 레지스트리(`z.globalRegistry`)에서 꺼내 메시지에 넣는다. 여기서 쓴 `z.config({ customError })`는 모든 스키마에 적용되는 전역 에러 맵을 지정하는 방법으로, [「전역 에러 커스터마이징」](#전역-에러-커스터마이징-global-error-customization)에서 다시 다룬다.

```ts
z.config({
  customError: (iss) => {
    const meta = iss.schema && z.globalRegistry.get(iss.schema);
    return `${meta?.title ?? "Field"} is invalid.`;
  },
});

z.string().min(5).meta({ title: "Password" }).safeParse("abc");
// => "Password is invalid."
```

사용하는 API에 따라 추가 속성이 더 있을 수 있다. 어떤 속성이 있는지는 TypeScript 자동 완성으로 확인한다.

```ts
z.string().min(5, {
  error: (iss) => {
    // ...위와 동일
    iss.minimum; // 최솟값
    iss.inclusive; // 최솟값 포함 여부
    return `Password must have ${iss.minimum} characters or more`;
  },
});
```

에러 맵이 `undefined`를 반환하면 메시지를 바꾸지 않고 기본 메시지를 쓴다. 더 정확히 말하면 [우선순위 체인](#에러-우선순위-error-precedence)에서 다음 순서의 에러 맵에 결정을 넘긴다. 그래서 일부 이슈의 메시지만 골라서 바꾸고 나머지는 그대로 두고 싶을 때 쓸 수 있다. 아래 예제처럼 문자열 대신 `{ message }` 객체를 반환해도 된다.

```ts
z.int64({
  error: (issue) => {
    // too_big 에러 메시지만 덮어쓴다
    if (issue.code === "too_big") {
      return { message: `Value must be <${issue.maximum}` };
    }

    // 나머지는 기본값에 맡긴다
    return undefined;
  },
});
```

## 파싱별 에러 커스터마이징 (Per-parse error customization)

같은 스키마라도 어디서 파싱하느냐에 따라 메시지를 다르게 하고 싶을 수 있다. 이럴 때는 스키마가 아니라 `.parse()`/`.safeParse()` 호출의 두 번째 인자로 에러 맵을 넘긴다. 이렇게 넘긴 에러 맵은 그 호출에만 적용된다.

```ts
const schema = z.string();

schema.parse(12, {
  error: iss => "per-parse custom error"
});
```

다만 파싱별 에러 맵은 스키마에 직접 지정한 커스텀 메시지보다 우선순위가 *낮다*. 스키마에 메시지가 있으면 그 메시지가 이긴다.

```ts
const schema = z.string({ error: "highest priority" });
const result = schema.safeParse(12, {
  error: (iss) => "lower priority",
});

result.error.issues;
// [{ message: "highest priority", ... }]
```

파싱별 에러 맵은 특정 검사에 묶여 있지 않으므로 어떤 종류의 이슈든 받을 수 있다. 그래서 `iss`의 타입은 가능한 모든 이슈 타입의 [판별 유니언(discriminated union)](https://www.typescriptlang.org/docs/handbook/2/narrowing.html#discriminated-unions)이다. 판별 유니언은 공통 속성(여기서는 `code`)의 값으로 구분되는 여러 타입의 합집합이다. `if (iss.code === "too_small")`처럼 `code`를 확인하면 TypeScript가 그 분기 안의 `iss`를 해당 이슈 타입으로 좁혀 주므로, `iss.minimum`처럼 그 이슈에만 있는 속성을 쓸 수 있다. Zod의 전체 이슈 코드 목록은 [「Zod Core: 이슈」](07_packages_and_compile.md#이슈-issues)를 참고한다.

```ts
const result = schema.safeParse(12, {
  error: (iss) => {
    if (iss.code === "invalid_type") {
      return `invalid type, expected ${iss.expected}`;
    }
    if (iss.code === "too_small") {
      return `minimum is ${iss.minimum}`;
    }
    // ...
  }
});
```

### 이슈에 입력 포함하기 (Include input in issues)

Zod는 기본적으로 이슈에 입력 데이터를 넣지 않는다. 비밀번호처럼 민감할 수 있는 입력이 에러와 함께 로그에 남는 일을 막기 위해서다. 디버깅 등으로 입력값이 필요하면 파싱 옵션에 `reportInput` 플래그를 켠다. 그러면 각 이슈에 `input` 속성이 추가된다.

```ts
z.string().parse(12, {
  reportInput: true
})

// ZodError: [
//   {
//     "expected": "string",
//     "code": "invalid_type",
//     "input": 12, // 👀
//     "path": [],
//     "message": "Invalid input: expected string, received number"
//   }
// ]
```

## 전역 에러 커스터마이징 (Global error customization)

앱 전체의 메시지 규칙을 한곳에서 정하고 싶다면 전역 에러 맵을 쓴다. `z.config()`는 Zod의 전역 설정을 바꾸는 함수이고, 여기에 `customError`로 에러 맵을 넘기면 모든 스키마의 파싱에 적용된다.

```ts
z.config({
  customError: (iss) => {
    return "globally modified error";
  },
});
```

전역 에러 메시지는 스키마 수준이나 파싱별 에러 메시지보다 우선순위가 *낮다*. 스키마나 파싱 호출에서 메시지를 정하지 않았을 때 비로소 쓰인다.

전역 에러 맵의 `iss`도 [파싱별 에러 맵](#파싱별-에러-커스터마이징-per-parse-error-customization)과 마찬가지로 모든 이슈 타입의 판별 유니언이므로, `code`로 분기한다.

```ts
z.config({
  customError: (iss) => {
    if (iss.code === "invalid_type") {
      return `invalid type, expected ${iss.expected}`;
    }
    if (iss.code === "too_small") {
      return `minimum is ${iss.minimum}`;
    }
    // ...
  },
});
```

## 국제화 (Internationalization)

국제화(internationalization, i18n)는 메시지를 사용자의 언어에 맞춰 보여 줄 수 있게 만드는 작업이다. Zod는 이를 위해 여러 내장 **로케일(locale)** 을 제공한다. 여기서 로케일은 특정 언어로 된 기본 에러 메시지 묶음이다. 아래 예제처럼 `en()` 같은 로케일 함수의 반환값을 `z.config()`에 넘기면, 그 언어의 메시지를 만드는 [로케일 에러 맵](#에러-우선순위-error-precedence)이 등록된다. 로케일은 `zod/v4/core` 패키지에서 내보낸다.

> **참고:** 일반 `zod` 라이브러리는 `en` 로케일을 자동으로 불러온다. Zod Mini는 기본으로 아무 로케일도 불러오지 않으며, 모든 에러 메시지가 `Invalid input`이 된다.

```ts
import * as z from "zod";
import { en } from "zod/locales"

z.config(en());
```

```ts
// Zod Mini
import * as z from "zod/mini"
import { en } from "zod/locales";

z.config(en());
```

모든 로케일을 처음부터 번들에 넣을 필요는 없다. 사용자의 언어가 정해진 뒤에 필요한 로케일만 불러오는 것을 지연 로딩(lazy loading)이라고 하는데, 이때는 실행 중에 모듈을 불러오는 동적 `import()`를 쓰면 된다.

```ts
import * as z from "zod";

async function loadLocale(locale: string) {
  const { default: locale } = await import(`zod/v4/locales/${locale}.js`);
  z.config(locale());
};

await loadLocale("fr");
```

> **참고:** 원문 예제는 매개변수 `locale`과 같은 이름으로 `const`를 선언하므로 그대로 실행하면 `Identifier 'locale' has already been declared` 문법 에러가 난다. 실제로 쓸 때는 `const { default: loadedLocale } = ...`처럼 이름을 바꾼다.

모든 로케일은 `z.locales`로도 내보낸다. 이 경로로 가져올 때는 번들러의 트리 셰이킹(tree-shaking), 즉 쓰지 않는 코드를 최종 번들에서 빼는 기능이 중요해진다([「트리 셰이킹」](07_packages_and_compile.md#트리-셰이킹-tree-shaking) 참고). Rollup과 Webpack은 실제로 사용하는 로케일만 남기지만, esbuild([evanw/esbuild#1420](https://github.com/evanw/esbuild/issues/1420))와 Turbopack은 그러지 못한다.

내장 로케일 대신 i18n 라이브러리의 번역 함수(아래 예제의 `t`)를 직접 쓸 수도 있다. 이때 번역을 언제 하느냐에 따라 두 가지 방법이 있다.

첫째는 `error` 파라미터 안에서 번역하는 방법이다. [앞에서 본 것처럼](#error-파라미터-the-error-param) 에러 맵은 스키마를 만들 때가 아니라 `.parse()` 중에 호출되므로, 한 번 정의한 스키마도 파싱할 때마다 그 시점의 언어로 메시지를 만든다.

둘째는 메시지가 아니라 이슈 자체를 나중에 번역하는 방법이다. 파싱 결과를 화면에 그리는 렌더링 시점까지 번역을 미루고 싶다면, 이슈의 `code`를 번역 키로 쓰고 `minimum` 같은 나머지 속성을 메시지 템플릿에 끼워 넣을 값(보간 값)으로 넘긴다.

```ts
z.string().min(5, { error: (iss) => t("too_short", iss) }); // 파싱 시점
result.error?.issues.map((iss) => t(iss.code, iss)); // 렌더링 시점
```

### 로케일 목록 (Locales)

사용할 수 있는 로케일은 다음과 같다.

- `ar`: Arabic
- `az`: Azerbaijani
- `be`: Belarusian
- `bg`: Bulgarian
- `bn`: Bengali
- `ca`: Catalan
- `ckb`: Kurdish (Central)
- `cs`: Czech
- `da`: Danish
- `de`: German
- `el`: Greek
- `en`: English
- `eo`: Esperanto
- `es`: Spanish
- `fa`: Farsi
- `fi`: Finnish
- `fr`: French
- `frCA`: Canadian French
- `gu`: Gujarati
- `he`: Hebrew
- `hi`: Hindi
- `hr`: Croatian
- `hu`: Hungarian
- `hy`: Armenian
- `id`: Indonesian
- `is`: Icelandic
- `it`: Italian
- `ja`: Japanese
- `ka`: Georgian
- `km`: Khmer
- `kn`: Kannada
- `ko`: Korean
- `lt`: Lithuanian
- `mk`: Macedonian
- `ms`: Malay
- `ne`: Nepali
- `nl`: Dutch
- `nn`: Norwegian Nynorsk
- `no`: Norwegian
- `ota`: Türkî
- `ps`: Pashto
- `pl`: Polish
- `pt`: Portuguese
- `ptBR`: Brazilian Portuguese
- `ro`: Romanian
- `ru`: Russian
- `sk`: Slovak
- `sl`: Slovenian
- `sv`: Swedish
- `ta`: Tamil
- `tg`: Tajik
- `th`: Thai
- `tk`: Türkmen
- `tr`: Türkçe
- `uk`: Ukrainian
- `ur`: Urdu
- `uz`: Uzbek
- `vi`: Tiếng Việt
- `zhCN`: Simplified Chinese
- `zhTW`: Traditional Chinese
- `yo`: Yorùbá

## 에러 우선순위 (Error precedence)

지금까지 검사, 스키마, 파싱 호출, 전역 설정, 로케일까지 메시지를 정하는 자리를 다섯 군데 보았다. 한 이슈에 대해 여러 곳에서 메시지를 정했다면 아래 목록에서 위에 있는 것이 이긴다. 우선순위가 *높은 것부터 낮은 것* 순이다.

1. **검사 수준 에러(check-level error)**: 실패한 개별 검사에 정의한 에러.

```ts
z.string().min(5, "Too short!");
```

2. **스키마 수준 에러(schema-level error)**: 스키마 정의에 "하드코딩"한 에러 메시지. 스키마 자신의 검사가 일으킨 이슈에도 적용되므로, 검사에 에러가 따로 없으면 이 메시지가 대신 쓰인다.

```ts
z.string("Invalid name").safeParse(12);          // => "Invalid name"
z.string("Invalid name").min(5).safeParse("ab"); // => "Invalid name"
```

3. **파싱별 에러(per-parse error)**: `.parse()` 메서드에 넘긴 커스텀 에러 맵.

```ts
z.string().parse(12, {
  error: (iss) => "My custom error"
});
```

4. **전역 에러 맵(global error map)**: `z.config()`에 넘긴 커스텀 에러 맵.

```ts
z.config({
  customError: (iss) => "My custom error"
});
```

5. **로케일 에러 맵(locale error map)**: `z.config()`에 넘긴 로케일 에러 맵.

```ts
import { en } from "zod/locales";

z.config(en());
```

## 에러 포맷팅 (Formatting errors)

Zod는 에러를 보고할 때 *완전성*과 *정확성*을 중시한다. 이슈 배열에는 문제가 빠짐없이 정확한 위치와 함께 담기지만, 그 배열을 그대로 화면에 보여 주거나 필드별로 꺼내 쓰기에는 불편할 때가 많다. 그래서 Zod는 `$ZodError`를 쓰기 편한 형태로 바꾸는 유틸리티를 제공한다.

다음 객체 스키마를 예로 든다. `z.strictObject`는 스키마에 정의하지 않은 키가 들어오면 실패하는 객체 스키마다([「`z.strictObject`」](03_schemas_objects_collections.md#zstrictobject) 참고).

```ts
import * as z from "zod";

const schema = z.strictObject({
  username: z.string(),
  favoriteNumbers: z.array(z.number()),
});
```

여기에 잘못된 데이터를 넣으면 이슈가 세 개 생긴다. `username`은 타입이 틀렸고, `favoriteNumbers`의 두 번째 원소는 숫자가 아니며, `extraKey`는 정의하지 않은 키다. 각 이슈의 `path`를 보면 문제가 난 위치를 알 수 있는데, 객체 전체에 대한 이슈인 `unrecognized_keys`는 `path`가 빈 배열이다. 아래 절의 예제는 모두 이 `result`를 사용한다.

```ts
const result = schema.safeParse({
  username: 1234,
  favoriteNumbers: [1234, "4567"],
  extraKey: 1234,
});

result.error!.issues;
[
  {
    expected: 'string',
    code: 'invalid_type',
    path: [ 'username' ],
    message: 'Invalid input: expected string, received number'
  },
  {
    expected: 'number',
    code: 'invalid_type',
    path: [ 'favoriteNumbers', 1 ],
    message: 'Invalid input: expected number, received string'
  },
  {
    code: 'unrecognized_keys',
    keys: [ 'extraKey' ],
    path: [],
    message: 'Unrecognized key: "extraKey"'
  }
];
```

### `z.treeifyError()`

`z.treeifyError()`는 평평한 이슈 배열을 스키마 모양을 따르는 중첩 객체(트리)로 바꾼다. 원문은 이를 "treeify(트리화)"라고 부른다.

```ts
const tree = z.treeifyError(result.error);

// =>
{
  errors: [ 'Unrecognized key: "extraKey"' ],
  properties: {
    username: { errors: [ 'Invalid input: expected string, received number' ] },
    favoriteNumbers: {
      errors: [],
      items: [
        undefined,
        {
          errors: [ 'Invalid input: expected number, received string' ]
        }
      ]
    }
  }
}
```

트리의 각 노드에서 `errors`에는 그 위치에서 난 에러 메시지가 담긴다. 더 깊이 내려갈 때는 객체 필드면 `properties`, 배열 원소면 `items`를 거친다. 그래서 `favoriteNumbers`의 두 번째 원소 에러는 `items[1]`에 있고, 에러가 없는 첫 번째 원소 자리는 `undefined`다. 경로만 알면 원하는 위치의 에러를 바로 꺼낼 수 있다.

```ts
tree.properties?.username?.errors;
// => ["Invalid input: expected string, received number"]

tree.properties?.favoriteNumbers?.items?.[1]?.errors;
// => ["Invalid input: expected number, received string"];
```

> **참고:** 에러가 없는 경로는 노드 자체가 없을 수 있다. 그래서 중첩 속성에 접근할 때는 중간 값이 `undefined`여도 에러 없이 `undefined`를 돌려주는 옵셔널 체이닝(`?.`)을 쓴다.

### `z.prettifyError()`

로그나 콘솔에 에러를 바로 찍고 싶다면 `z.prettifyError()`로 사람이 읽기 좋은 문자열을 만든다.

```ts
const pretty = z.prettifyError(result.error);
```

이슈마다 메시지를 한 줄씩 쓰고, 경로가 있으면 다음 줄에 `→ at` 뒤로 위치를 붙인다.

```
✖ Unrecognized key: "extraKey"
✖ Invalid input: expected string, received number
  → at username
✖ Invalid input: expected number, received string
  → at favoriteNumbers[1]
```

### `z.formatError()`

> **주의:** 폐기 예정(deprecated)이다. 대신 `z.treeifyError()`를 쓴다.

`z.treeifyError()`처럼 에러를 중첩 객체로 바꾸지만, 모양이 조금 다르다. `properties`나 `items`를 거치지 않고 필드 이름과 배열 인덱스가 곧바로 키가 되며, 각 위치의 메시지는 `_errors`에 담긴다.

```ts
const formatted = z.formatError(result.error);

// 반환값:
{
 _errors: [ 'Unrecognized key: "extraKey"' ],
 username: { _errors: [ 'Invalid input: expected string, received number' ] },
 favoriteNumbers: {
   '1': { _errors: [ 'Invalid input: expected number, received string' ] },
   _errors: []
 }
}
```

이 결과도 스키마 구조를 따르므로 경로를 따라가면 특정 위치의 에러를 꺼낼 수 있다.

```ts
formatted?.username?._errors;
// => ["Invalid input: expected string, received number"]

formatted?.favoriteNumbers?.[1]?._errors;
// => ["Invalid input: expected number, received string"]
```

> **참고:** 여기서도 중첩 속성에 접근할 때는 옵셔널 체이닝(`?.`)을 쓴다.

### `z.flattenError()`

`z.treeifyError()`는 깊게 중첩된 구조를 탐색할 때 유용하다. 하지만 폼 입력처럼 대부분의 스키마는 필드가 한 단계뿐인 *평평한* 구조이고, 이때는 트리를 따라 내려가는 것이 번거롭다. `z.flattenError()`는 에러를 한 단계 깊이의 얕은 객체로 정리해 준다.

```ts
const flattened = z.flattenError(result.error);
// { errors: string[], properties: { [key: string]: string[] } }

{
  formErrors: [ 'Unrecognized key: "extraKey"' ],
  fieldErrors: {
    username: [ 'Invalid input: expected string, received number' ],
    favoriteNumbers: [ 'Invalid input: expected number, received string' ]
  }
}
```

`formErrors` 배열에는 특정 필드가 아니라 객체 전체에 대한 최상위 에러(`path`가 `[]`인 에러)가 담긴다. `fieldErrors` 객체에는 최상위 필드 이름별로 에러 메시지 배열이 담긴다. `favoriteNumbers[1]`처럼 더 깊은 곳에서 난 에러도 최상위 필드인 `favoriteNumbers` 아래로 모인다.

```ts
flattened.fieldErrors.username; // => [ 'Invalid input: expected string, received number' ]
flattened.fieldErrors.favoriteNumbers; // => [ 'Invalid input: expected number, received string' ]
```

# Zod 스키마 정의 3: 정제, 변환, 코덱

> 원문: https://zod.dev/api
> 원문: https://zod.dev/codecs

이 문서는 Zod 4 (4.6.5 기준)의 스키마 정의 API 가운데 뒷부분을 다룬다. 정제(refinement)로 사용자 정의 검증을 붙이는 방법과 파이프와 변환(transform)으로 값을 다른 형태로 바꾸는 방법을 먼저 설명한다. 이어서 기본값과 폴백 값, 브랜드 타입과 읽기 전용 타입, 함수 스키마 같은 보조 API를 차례로 살펴보고, 마지막으로 양방향 변환을 정의하는 코덱(codec)을 정리한다.

코드 예제는 일반 Zod 코드와 Zod Mini 코드를 함께 싣는다. 두 코드가 다를 때는 `// Zod Mini` 주석이 붙은 두 번째 블록이 Zod Mini 버전이다. Zod Mini 자체에 대한 설명은 [「Zod Mini」](07_packages_and_compile.md#zod-mini)를 참고한다.

## 정제 (Refinements)

정제(refinement)는 Zod가 기본 API로 제공하지 않는 검증 규칙을 직접 함수로 작성해 스키마에 덧붙이는 것이다. 예를 들어 `z.string()`은 값이 문자열인지만 확인하므로, "비밀번호와 비밀번호 확인 값이 같아야 한다" 같은 조건은 정제로 붙인다. 정제 함수는 값을 받아 통과하면 truthy 값을, 실패하면 falsy 값을 반환한다. 모든 Zod 스키마는 이런 정제를 배열로 저장해 두고 파싱할 때 차례로 실행한다.

### .refine()

```ts
const myString = z.string().refine((val) => val.length <= 255);
```

```ts
// Zod Mini
const myString = z.string().check(z.refine((val) => val.length <= 255));
```

> **주의:** 정제 함수는 절대 예외를 던지면 안 된다. 정제 함수가 던진 예외는 Zod가 잡지 않으므로 검증 에러로 보고되지 않는다. 실패는 falsy 값을 반환해서 알린다.

#### error

에러 메시지를 바꾸려면 `error` 옵션을 넘긴다.

```ts
const myString = z.string().refine((val) => val.length > 8, { 
  error: "Too short!" 
});
```

```ts
// Zod Mini
const myString = z.string().check(
  z.refine((val) => val.length > 8, { error: "Too short!" })
);
```

`error` 옵션에는 함수도 넘길 수 있다. 이 함수는 이슈(issue), 즉 검증 실패 하나를 설명하는 객체를 인자로 받아 메시지를 만든다. 아래 예제에서는 이슈의 `input`에 담긴 입력값을 메시지에 넣는다.

```ts
const myString = z.string().refine((val) => val.length > 8, {
  error: (iss) => `Too short: "${iss.input}"`
});

myString.parse("OH NO"); // ❌ Too short: "OH NO"
```

```ts
// Zod Mini
const myString = z.string().check(
  z.refine((val) => val.length > 8, { error: (iss) => `Too short: "${iss.input}"` })
);

z.parse(myString, "OH NO"); // ❌ Too short: "OH NO"
```

에러 메시지를 지정하는 여러 방식과 우선순위는 [「`error` 파라미터」](05_errors.md#error-파라미터-the-error-param)와 [「에러 우선순위」](05_errors.md#에러-우선순위-error-precedence)를 참고한다.

#### abort

검사(check)는 `.refine()`이나 `.min()`처럼 스키마에 덧붙인 검증 하나하나를 가리킨다. 기본적으로 검사에서 나온 이슈는 계속 진행 가능한(continuable) 이슈로 취급하므로, 어느 한 검사가 실패해도 Zod는 나머지 검사를 순서대로 모두 실행한다. 한 번의 파싱으로 에러를 최대한 많이 모을 수 있어서 대개는 이쪽이 바람직하다. 아래 예제에서도 두 정제가 모두 실패해 이슈가 두 개 나온다.

```ts
const myString = z.string()
  .refine((val) => val.length > 8, { error: "Too short!" })
  .refine((val) => val === val.toLowerCase(), { error: "Must be lowercase" });
  

const result = myString.safeParse("OH NO");
result.error?.issues;
/* [
  { "code": "custom", "message": "Too short!" },
  { "code": "custom", "message": "Must be lowercase" }
] */
```

```ts
// Zod Mini
const myString = z.string().check(
  z.refine((val) => val.length > 8, { error: "Too short!" }),
  z.refine((val) => val === val.toLowerCase(), { error: "Must be lowercase" })
);

const result = z.safeParse(myString, "OH NO");
result.error?.issues;
/* [
  { "code": "custom", "message": "Too short!" },
  { "code": "custom", "message": "Must be lowercase" }
] */
```

앞의 정제가 실패하면 뒤의 정제를 실행할 의미가 없을 때도 있다. 이럴 때는 `abort` 파라미터로 해당 정제를 계속 진행할 수 없는(non-continuable) 정제로 만든다. 이 정제가 실패하면 검증이 그 자리에서 끝나므로, 아래 예제에서는 첫 번째 이슈만 남는다.

```ts
const myString = z.string()
  .refine((val) => val.length > 8, { error: "Too short!", abort: true })
  .refine((val) => val === val.toLowerCase(), { error: "Must be lowercase", abort: true });


const result = myString.safeParse("OH NO");
result.error?.issues;
// => [{ "code": "custom", "message": "Too short!" }]
```

```ts
// Zod Mini
const myString = z.string().check(
  z.refine((val) => val.length > 8, { error: "Too short!", abort: true }),
  z.refine((val) => val === val.toLowerCase(), { error: "Must be lowercase", abort: true })
);

const result = z.safeParse(myString, "OH NO");
result.error?.issues;
// [ { "code": "custom", "message": "Too short!" }]
```

#### path

객체 전체에 붙인 정제가 실패하면 이슈는 기본적으로 객체 자체에 걸린다. 비밀번호 확인처럼 특정 필드의 문제로 보고하고 싶다면 `path` 파라미터로 에러 경로를 지정한다. 그래서 보통은 객체 스키마에서만 쓸모가 있다.

```ts
const passwordForm = z
  .object({
    password: z.string(),
    confirm: z.string(),
  })
  .refine((data) => data.password === data.confirm, {
    error: "Passwords don't match",
    path: ["confirm"], // 에러 경로
  });
```

```ts
// Zod Mini
const passwordForm = z
  .object({
    password: z.string(),
    confirm: z.string(),
  })
  .check(z.refine((data) => data.password === data.confirm, {
    error: "Passwords don't match",
    path: ["confirm"], // 에러 경로
  }));
```

이렇게 하면 해당 이슈의 `path`에 지정한 값이 들어간다.

```ts
const result = passwordForm.safeParse({ password: "asdf", confirm: "qwer" });
result.error.issues;
/* [{
  "code": "custom",
  "path": [ "confirm" ],
  "message": "Passwords don't match"
}] */
```

```ts
// Zod Mini
const result = z.safeParse(passwordForm, { password: "asdf", confirm: "qwer" });
result.error.issues;
/* [{
  "code": "custom",
  "path": [ "confirm" ],
  "message": "Passwords don't match"
}] */
```

#### 비동기 정제

정제 함수로 `async` 함수를 넘기면 비동기 정제가 된다. 데이터베이스나 외부 API를 조회해야 판정할 수 있는 조건, 예를 들어 ID가 실제로 존재하는지 확인할 때 쓴다. 아래 예제는 조회 부분을 주석으로 대신하고 항상 `true`를 반환한다.

```ts
const userId = z.string().refine(async (id) => {
  // 데이터베이스에 ID가 있는지 확인
  return true;
});
```

`async` 함수는 판정 결과를 바로 반환하지 않고 Promise로 감싸서 반환한다. 그런데 `.parse()`는 결과를 곧바로 반환하는 동기 메서드라서 이 Promise가 끝나기를 기다릴 수 없다. 그래서 비동기 정제가 들어간 스키마는 반드시 `.parseAsync()`로 파싱해야 하며, `.parse()`로 파싱하면 Zod가 에러를 던진다(배경은 [「비동기 정제와 변환」](01_intro_and_basics.md#비동기-정제와-변환)에서도 다룬다).

```ts
const result = await userId.parseAsync("abc123");
```

```ts
// Zod Mini
const result = await z.parseAsync(userId, "abc123");
```

#### when

> **참고:** 고급 사용자를 위한 기능이다. 잘못 쓰면 정제 함수 내부에서 잡히지 않는 에러가 날 가능성이 확실히 커진다.

기본적으로 [계속 진행할 수 없는 이슈](#abort)가 이미 하나라도 발생했다면 정제는 실행되지 않는다. 타입 불일치(`invalid_type`)가 대표적인 예로, Zod는 값의 타입이 올바르다는 것을 확인한 뒤에만 정제 함수에 값을 넘긴다. 그래서 정제 함수는 `val`이 문자열이라고 믿고 `val.length`를 읽을 수 있다.

```ts
const schema = z.string().refine((val) => {
  return val.length > 8
});

schema.parse(1234); // invalid_type: 정제는 실행되지 않는다
```

정제가 실행되는 시점을 더 세밀하게 조절하고 싶을 때가 있다. 다음 "비밀번호 확인" 검사를 보자.

```ts
const schema = z
  .object({
    password: z.string().min(8),
    confirmPassword: z.string(),
    anotherField: z.string(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Passwords do not match",
    path: ["confirmPassword"],
  });

schema.parse({
  password: "asdf",
  confirmPassword: "asdf",
  anotherField: 1234 // ❌ 이 에러 때문에 비밀번호 검사가 실행되지 않는다
});
```

```ts
// Zod Mini
const schema = z
  .object({
    password: z.string().check(z.minLength(8)),
    confirmPassword: z.string(),
    anotherField: z.string(),
  })
  .check(z.refine((data) => data.password === data.confirmPassword, {
    message: "Passwords do not match",
    path: ["confirmPassword"],
  }));

schema.parse({
  password: "asdf",
  confirmPassword: "asdf",
  anotherField: 1234 // ❌ 이 에러 때문에 비밀번호 검사가 실행되지 않는다
});
```

비밀번호 확인 검사는 `anotherField`와 무관한데도 `anotherField`의 에러 때문에 실행되지 않는다. 정제의 실행 시점을 직접 정하려면 `when` 파라미터를 쓴다.

```ts
const baseSchema = z.object({
  password: z.string().min(8),
  confirmPassword: z.string(),
  anotherField: z.string(),
});

const schema = baseSchema
  .refine((data) => data.password === data.confirmPassword, {
    message: "Passwords do not match",
    path: ["confirmPassword"],

    // password와 confirmPassword가 유효하면 실행
    when(payload) {
      return baseSchema
        .pick({ password: true, confirmPassword: true })
        .safeParse(payload.value).success;
    },
  });

schema.parse({
  password: "asdf",
  confirmPassword: "asdf",
  anotherField: 1234 // ❌ 이 에러가 있어도 비밀번호 검사는 실행된다
});
```

```ts
// Zod Mini
const schema = z
  .object({
    password: z.string().min(8),
    confirmPassword: z.string(),
    anotherField: z.string(),
  })
  .check(z.refine((data) => data.password === data.confirmPassword, {
    message: "Passwords do not match",
    path: ["confirmPassword"],

    when(payload) {
      // `password`나 `confirmPassword`에 이슈가 없음
      return payload.issues.every((iss) => {
        const firstPathEl = iss.path?.[0];
        return firstPathEl !== "password" && firstPathEl !== "confirmPassword";
      });
    },
  }));

schema.parse({
  password: "asdf",
  confirmPassword: "asdf",
  anotherField: 1234 // ❌ 이 에러 때문에 비밀번호 검사가 실행되지 않는다
});
```

> **참고:** 위 Zod Mini 예제의 마지막 주석은 원문 그대로 옮긴 것이다. `when`을 지정했으므로 실제로는 Zod 예제와 마찬가지로 `anotherField`의 에러가 비밀번호 검사를 막지 않는다고 보는 편이 맞다.

### .superRefine()

일반 `.refine` API는 `"custom"` 에러 코드를 가진 이슈를 하나만 만든다. `.superRefine()`을 쓰면 Zod의 [내부 이슈 타입](https://github.com/colinhacks/zod/blob/main/packages/zod/src/v4/core/errors.ts) 중 무엇이든 골라 여러 이슈를 만들 수 있다.

```ts
const UniqueStringArray = z.array(z.string()).superRefine((val, ctx) => {
  if (val.length > 3) {
    ctx.addIssue({
      code: "too_big",
      maximum: 3,
      origin: "array",
      inclusive: true,
      message: "Too many items 😡",
      input: val,
    });
  }

  if (val.length !== new Set(val).size) {
    ctx.addIssue({
      code: "custom",
      message: `No duplicates allowed.`,
      input: val,
    });
  }
});
```

```ts
// Zod Mini
const UniqueStringArray = z.array(z.string()).check(
  z.superRefine((val, ctx) => {
    if (val.length > 3) {
      ctx.addIssue({
        code: "too_big",
        maximum: 3,
        origin: "array",
        inclusive: true,
        message: "Too many items 😡",
        input: val,
      });
    }

    if (val.length !== new Set(val).size) {
      ctx.addIssue({
        code: "custom",
        message: `No duplicates allowed.`,
        input: val,
      });
    }
  })
);
```

이슈 객체의 구조는 [「ZodError와 이슈」](05_errors.md#zoderror와-이슈-zoderror-and-issues)와 [「에러 포맷팅」](05_errors.md#에러-포맷팅-formatting-errors)을 참고한다.

### .check()

> **참고:** `.check()`는 저수준 API라서 대체로 `.superRefine()`보다 복잡하다. 성능이 중요한 코드 경로에서는 더 빠를 수 있지만, 그만큼 코드가 장황해진다.

`.refine()` API는 더 범용적이고 장황한 `.check()` API 위에 얹은 문법 설탕(syntactic sugar)이다. `.check()`를 쓰면 정제 하나에서 여러 이슈를 만들거나, 생성되는 이슈 객체를 완전히 제어할 수 있다.

```ts
const UniqueStringArray = z.array(z.string()).check((ctx) => {
  if (ctx.value.length > 3) {
    // 이슈 객체를 완전히 제어
    ctx.issues.push({
      code: "too_big",
      maximum: 3,
      origin: "array",
      inclusive: true,
      message: "Too many items 😡",
      input: ctx.value
    });
  }

  // 정제 하나에서 여러 이슈 생성
  if (ctx.value.length !== new Set(ctx.value).size) {
    ctx.issues.push({
      code: "custom",
      message: `No duplicates allowed.`,
      input: ctx.value,
      continue: true // 이 이슈를 계속 진행 가능하게 만든다 (기본값: false)
    });
  }
});
```

```ts
// Zod Mini
const UniqueStringArray = z.array(z.string()).check((ctx) => {
  // 이슈 객체를 완전히 제어
  if (ctx.value.length > 3) {
    ctx.issues.push({
      code: "too_big",
      maximum: 3,
      origin: "array",
      inclusive: true,
      message: "Too many items 😡",
      input: ctx.value
    });
  }

// 정제 하나에서 여러 이슈 생성
  if (ctx.value.length !== new Set(ctx.value).size) {
    ctx.issues.push({
      code: "custom",
      message: `No duplicates allowed.`,
      input: ctx.value,
      continue: true // 이 이슈를 계속 진행 가능하게 만든다 (기본값: false)
    });
  }
});
```

## 파이프 (Pipes)

파이프(pipe)는 두 스키마를 이어서, 앞 스키마가 파싱한 결과를 뒤 스키마의 입력으로 넘기는 스키마다. 셸의 `|`처럼 값이 앞 단계에서 뒤 단계로 흘러간다. 파이프는 주로 [변환](#변환-transforms)과 함께 쓸 때 쓸모가 있다. 아래 예제에서는 `z.string()`이 먼저 값이 문자열인지 검증하고, 통과한 문자열을 `z.transform()`이 길이로 바꾼다.

```ts
const stringToLength = z.string().pipe(z.transform(val => val.length));

stringToLength.parse("hello"); // => 5
```

```ts
// Zod Mini
const stringToLength = z.pipe(z.string(), z.transform(val => val.length));

z.parse(stringToLength, "hello"); // => 5
```

### z.input()과 z.output() (z.input() and z.output())

파이프는 입력 쪽 스키마와 출력 쪽 스키마를 함께 갖는다. `z.input()`과 `z.output()`은 타입 수준의 `z.input<T>` / `z.output<T>`에 대응하는 런타임 함수로, 스키마 안의 모든 파이프를 입력 쪽 또는 출력 쪽 스키마 하나로 바꾼 새 스키마를 돌려준다. 객체나 레코드, 맵 안에 중첩되어 `.in` / `.out`으로는 닿지 않는 [코덱](#코덱-codecs)에 접근할 때 유용하다. 타입 수준의 `z.input<T>` / `z.output<T>`는 [「타입 추론」](01_intro_and_basics.md#타입-추론-inferring-types)을 참고한다.

아래 예제의 `stringToDate`는 뒤의 [순방향과 역방향](#순방향과-역방향-forward-and-backward)에서 정의하는 코덱으로, 입력 쪽은 ISO 날짜 문자열이고 출력 쪽은 `Date` 객체다. 그래서 `z.input(Event)`는 `at`에 문자열을, `z.output(Event)`는 `Date`를 받는다.

```ts
const Event = z.object({ name: z.string(), at: stringToDate });

z.input(Event).parse({ name: "launch", at: "2024-01-01T00:00:00Z" }); // ✅
z.output(Event).parse({ name: "launch", at: new Date() });            // ✅
```

입력 쪽과 출력 쪽이 실제로 따로 있는 스키마는 코덱뿐이다. 단방향 [변환](#변환-transforms)에 `z.output()`을 적용하면 아무것도 검증하지 않는 변환 자체가 반환되고, `z.preprocess()`에 `z.input()`을 적용하면 전처리 함수가 값을 넘겨주는 대상 스키마가 반환된다.

`.default()`, `.catch()`, `.prefault()`처럼 스키마를 감싸서 값을 하나 저장해 두는 래퍼(wrapper)도 주의해야 한다. 래퍼가 저장한 값은 그 값이 속한 쪽에서만 살아남는다. [기본값](#기본값-defaults)과 [폴백 값](#폴백-값-catch)은 출력 쪽 값이고 사전 기본값은 입력 쪽 값이다. 따라서 안쪽에 코덱이 있으면 `z.input()`은 `.default()`나 `.catch()`를 버리고, `z.output()`은 `.prefault()`를 버린다.

## 변환 (Transforms)

> **참고:** 양방향 변환이 필요하면 [코덱](#코덱-codecs)을 쓴다.

변환(transform)은 받은 값을 함수에 넣어 다른 값으로 바꾸는 특별한 종류의 스키마다. 입력을 검증하지 않고 무엇이든 받아서, 함수가 반환한 값을 그대로 출력으로 내보낸다. 여기서 단방향이라는 말은 입력을 출력으로 바꾸는 방법만 정의하고, 출력을 원래 입력으로 되돌리는 방법은 정의하지 않는다는 뜻이다. 변환은 다음과 같이 정의한다.

```ts
const castToString = z.transform((val) => String(val));

castToString.parse("asdf"); // => "asdf"
castToString.parse(123); // => "123"
castToString.parse(true); // => "true"
```

```ts
// Zod Mini
const castToString = z.transform((val) => String(val));

z.parse(castToString, "asdf"); // => "asdf"
z.parse(castToString, 123); // => "123"
z.parse(castToString, true); // => "true"
```

> **주의:** 변환 함수는 절대 예외를 던지면 안 된다. 변환 함수가 던진 예외는 Zod가 잡지 않는다.

변환 도중 실패를 알려야 한다면 예외를 던지는 대신 두 번째 인자 `ctx`를 쓴다. [`.check()`](#check) API와 비슷하게 `ctx.issues`에 새 이슈를 넣으면 검증 이슈로 보고된다.

```ts
const coercedInt = z.transform((val, ctx) => {
  try {
    const parsed = Number.parseInt(String(val));
    return parsed;
  } catch (e) {
    ctx.issues.push({
      code: "custom",
      message: "Not a number",
      input: val,
    });

    // `never` 타입을 가진 특별한 상수다
    // 이 값을 반환하면 추론되는 반환 타입에 영향을 주지 않고 변환을 빠져나올 수 있다
    return z.NEVER;
  }
});
```

변환 자체는 입력을 검증하지 않으므로 [파이프](#파이프-pipes)와 함께 쓰는 경우가 가장 많다. 앞에 놓인 스키마가 기본 검증을 먼저 하고, 검증을 통과한 데이터를 변환이 다른 형태로 바꾼다. 앞의 파이프 절에 있는 `stringToLength`가 바로 이 형태다.

### .transform()

어떤 스키마를 변환으로 파이프하는 패턴은 흔하기 때문에 Zod는 편의 메서드 `.transform()`을 제공한다.

```ts
const stringToLength = z.string().transform(val => val.length); 
```

```ts
// Zod Mini
// 대응하는 API 없음
```

정제와 마찬가지로 변환 함수에 `async` 함수를 넘기면 비동기 변환이 된다. 아래 예제는 ID 문자열을 받아 데이터베이스에서 사용자를 조회한 결과로 바꾼다(`db.getUserById`는 설명을 위한 가상의 함수다).

```ts
const idToUser = z
  .string()
  .transform(async (id) => {
    // 데이터베이스에서 사용자 조회
    return db.getUserById(id); 
  });

const user = await idToUser.parseAsync("abc123");
```

```ts
// Zod Mini
const idToUser = z.pipe(
  z.string(),
  z.transform(async (id) => {
    // 데이터베이스에서 사용자 조회
    return db.getUserById(id); 
  }));

const user = await idToUser.parse("abc123");
```

이 변환 함수는 사용자 객체가 아니라 Promise를 반환한다. [비동기 정제](#비동기-정제)와 같은 이유로 동기 메서드인 `.parse()`는 이 Promise가 끝나기를 기다릴 수 없으므로, 비동기 변환이 들어간 스키마는 반드시 `.parseAsync`나 `.safeParseAsync`로 파싱해야 한다. 동기 메서드로 파싱하면 Zod가 에러를 던진다.

### .preprocess()

`.transform()`이 검증 뒤에 변환을 붙인다면, 전처리(preprocess)는 순서를 뒤집어 변환을 먼저 실행하고 그 결과를 다른 스키마로 검증한다. 문자열로 들어온 숫자를 `number`로 바꾼 뒤 정수인지 검사하는 식이다. 이렇게 변환을 다른 스키마로 파이프하는 것도 흔한 패턴이라서 Zod는 편의 함수 `z.preprocess()`를 제공한다.

```ts
const coercedInt = z.preprocess((val) => {
  if (typeof val === "string") {
    return Number.parseInt(val);
  }
  return val;
}, z.int());
```

전처리 함수는 임의의 입력을 처리한다고 가정하므로 `z.preprocess()` 스키마의 입력 타입은 기본적으로 `unknown`이다. 입력 타입을 좁히려면 전처리 함수의 파라미터에 직접 타입을 붙인다.

```ts
const trimmed = z.preprocess(
  (val: string | null | undefined) => val?.trim() ?? "",
  z.string()
);

type Input = z.input<typeof trimmed>;  // string | null | undefined
type Output = z.output<typeof trimmed>; // string
```

이렇게 입력 타입을 좁혀 두면 `react-hook-form`처럼 `z.input<>`에서 폼 값 타입을 끌어내는 라이브러리와 함께 쓰기 좋다.

## 기본값 (Defaults)

기본값(default)은 입력이 `undefined`일 때 대신 쓸 값이다. 스키마에 기본값을 지정하는 방법은 다음과 같다.

```ts
const defaultTuna = z.string().default("tuna");

defaultTuna.parse(undefined); // => "tuna"
```

```ts
// Zod Mini
const defaultTuna = z._default(z.string(), "tuna");

defaultTuna.parse(undefined); // => "tuna"
```

함수를 넘길 수도 있다. 이 함수는 기본값이 필요할 때마다 다시 실행된다.

```ts
const randomDefault = z.number().default(Math.random);

randomDefault.parse(undefined);    // => 0.4413456736055323
randomDefault.parse(undefined);    // => 0.1871840107401901
randomDefault.parse(undefined);    // => 0.7223408162401552
```

```ts
// Zod Mini
const randomDefault = z._default(z.number(), Math.random);

z.parse(randomDefault, undefined); // => 0.4413456736055323
z.parse(randomDefault, undefined); // => 0.1871840107401901
z.parse(randomDefault, undefined); // => 0.7223408162401552
```

## 사전 기본값 (Prefaults)

Zod에서 기본값을 지정하면 파싱 과정이 단락(short-circuit)된다. 단락은 뒤에 남은 단계를 건너뛴다는 뜻으로, 입력이 `undefined`이면 Zod는 나머지 파싱 없이 기본값을 곧바로 결과로 반환한다. 기본값이 곧 파싱 결과가 되므로 기본값은 스키마의 출력 타입에 할당할 수 있어야 한다. 아래 예제에서 기본값 `0`은 변환을 거치지 않고 그대로 나온다.

```ts
const schema = z.string().transform(val => val.length).default(0);
schema.parse(undefined); // => 0
```

반대로 기본값도 다른 입력처럼 검증과 변환을 거치게 하고 싶을 때가 있다. 이때 쓰는 것이 사전 기본값(prefault, "pre-parse default"), 즉 파싱하기 전에 채워 넣는 기본값이다. 입력이 `undefined`이면 Zod는 사전 기본값을 원래 입력인 것처럼 처음부터 파싱하며, 파싱 과정은 단락되지 않는다. 그래서 사전 기본값은 스키마의 입력 타입에 할당할 수 있어야 한다. 아래 예제에서는 사전 기본값 `"tuna"`가 변환을 거쳐 길이 `4`가 된다.

```ts
const schema = z.string().transform(val => val.length).prefault("tuna");
schema.parse(undefined); // => 4
```

`.trim()`이나 `.toUpperCase()`처럼 검증하면서 값 자체를 바꾸는 정제(mutating refinement)를 기본값에도 적용하고 싶을 때도 사전 기본값을 쓴다. 아래에서 `a`의 사전 기본값은 공백 제거와 대문자 변환을 거치지만, `b`의 기본값은 단락되어 그대로 반환된다.

```ts
const a = z.string().trim().toUpperCase().prefault("  tuna  ");
a.parse(undefined); // => "TUNA"

const b = z.string().trim().toUpperCase().default("  tuna  ");
b.parse(undefined); // => "  tuna  "
```

## 폴백 값 (Catch)

`.catch()`는 폴백(fallback) 값, 즉 검증 에러가 났을 때 에러를 던지는 대신 반환할 대체 값을 정의한다. 기본값이 입력이 `undefined`일 때만 쓰이는 것과 달리, 폴백 값은 아래 `"tuna"`처럼 어떤 이유로든 검증에 실패하면 쓰인다.

```ts
const numberWithCatch = z.number().catch(42);

numberWithCatch.parse(5); // => 5
numberWithCatch.parse("tuna"); // => 42
```

```ts
// Zod Mini
const numberWithCatch = z.catch(z.number(), 42);

numberWithCatch.parse(5); // => 5
numberWithCatch.parse("tuna"); // => 42
```

함수를 넘길 수도 있다. 이 함수는 폴백 값이 필요할 때마다 다시 실행된다.

```ts
const numberWithRandomCatch = z.number().catch((ctx) => {
  ctx.error; // 잡힌 ZodError

  return Math.random();
});

numberWithRandomCatch.parse("sup"); // => 0.4413456736055323
numberWithRandomCatch.parse("sup"); // => 0.1871840107401901
numberWithRandomCatch.parse("sup"); // => 0.7223408162401552
```

```ts
// Zod Mini
const numberWithRandomCatch = z.catch(z.number(), (ctx) => {
  ctx.value;   // 입력 값
  ctx.issues;  // 잡힌 검증 이슈
  return Math.random();
});

z.parse(numberWithRandomCatch, "sup"); // => 0.4413456736055323
z.parse(numberWithRandomCatch, "sup"); // => 0.1871840107401901
z.parse(numberWithRandomCatch, "sup"); // => 0.7223408162401552
```

## 브랜드 타입 (Branded types)

TypeScript의 타입 시스템은 [구조적(structural)](https://www.typescriptlang.org/docs/handbook/type-compatibility.html)이다. 구조가 같은 두 타입은 같은 타입으로 취급한다.

```ts
type Cat = { name: string };
type Dog = { name: string };

const pluto: Dog = { name: "pluto" };
const simba: Cat = pluto; // 문제없이 동작한다
```

그런데 구조가 같아도 의미가 다른 값은 섞이지 않게 막고 싶을 때가 있다. 구조가 아니라 이름으로 타입을 구분하는 방식을 [명목적 타이핑(nominal typing)](https://en.wikipedia.org/wiki/Nominal_type_system)이라고 하는데, TypeScript에서 이를 흉내 내는 방법이 브랜드 타입(branded type, "불투명 타입(opaque type)"이라고도 한다)이다. 타입에 `"Cat"`, `"Dog"` 같은 꼬리표(브랜드)를 덧붙여서, 구조가 같아도 꼬리표가 다르면 서로 할당할 수 없게 만든다.

```ts
const Cat = z.object({ name: z.string() }).brand<"Cat">();
const Dog = z.object({ name: z.string() }).brand<"Dog">();

type Cat = z.infer<typeof Cat>; // { name: string } & z.$brand<"Cat">
type Dog = z.infer<typeof Dog>; // { name: string } & z.$brand<"Dog">

const pluto = Dog.parse({ name: "pluto" });
const simba: Cat = pluto; // ❌ 허용되지 않는다
```

내부적으로는 스키마의 추론 타입에 "브랜드"를 붙이는 방식으로 동작한다.

```ts
const Cat = z.object({ name: z.string() }).brand<"Cat">();
type Cat = z.output<typeof Cat>; // { name: string } & z.$brand<"Cat">
```

브랜드가 붙으면 브랜드가 없는 일반 데이터 구조는 추론 타입에 할당할 수 없다. 브랜드가 붙은 데이터를 얻으려면 스키마로 데이터를 파싱해야 하므로, `Cat` 타입의 값은 `Cat` 스키마의 검증을 거쳤다고 믿을 수 있다.

> **참고:** 브랜드 타입은 `.parse`의 런타임 결과에 영향을 주지 않는다. 순수하게 정적 타입 수준의 구성 요소다.

기본적으로는 출력 타입에만 브랜드가 붙는다.

```ts
const USD = z.string().brand<"USD">();

type USDOutput = z.output<typeof USD>; // string & z.$brand<"USD">
type USDInput = z.input<typeof USD>; // string
```

브랜드를 붙일 방향을 바꾸려면 `.brand()`에 두 번째 제네릭 인자를 넘긴다.

```ts
// Zod 4.2 이상 필요
z.string().brand<"Cat", "out">(); // 출력에 브랜드를 붙인다 (기본값)
z.string().brand<"Cat", "in">(); // 입력에 브랜드를 붙인다
z.string().brand<"Cat", "inout">(); // 양쪽 모두 브랜드를 붙인다
```

## 읽기 전용 (Readonly)

스키마를 읽기 전용으로 표시하는 방법은 다음과 같다.

```ts
const ReadonlyUser = z.object({ name: z.string() }).readonly();
type ReadonlyUser = z.infer<typeof ReadonlyUser>;
// Readonly<{ name: string }>
```

```ts
// Zod Mini
const ReadonlyUser = z.readonly(z.object({ name: z.string() }));
type ReadonlyUser = z.infer<typeof ReadonlyUser>;
// Readonly<{ name: string }>
```

추론 타입이 `readonly`로 표시된다. TypeScript에서 이 표시는 객체, 배열, 튜플, `Set`, `Map`에만 영향을 준다.

```ts
z.object({ name: z.string() }).readonly(); // { readonly name: string }
z.array(z.string()).readonly(); // readonly string[]
z.tuple([z.string(), z.number()]).readonly(); // readonly [string, number]
z.map(z.string(), z.date()).readonly(); // ReadonlyMap<string, Date>
z.set(z.string()).readonly(); // ReadonlySet<string>
```

```ts
// Zod Mini
z.readonly(z.object({ name: z.string() })); // { readonly name: string }
z.readonly(z.array(z.string())); // readonly string[]
z.readonly(z.tuple([z.string(), z.number()])); // readonly [string, number]
z.readonly(z.map(z.string(), z.date())); // ReadonlyMap<string, Date>
z.readonly(z.set(z.string())); // ReadonlySet<string>
```

입력은 평소처럼 파싱되고, 결과는 수정할 수 없도록 [`Object.freeze()`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/freeze)로 동결된다.

```ts
const result = ReadonlyUser.parse({ name: "fido" });
result.name = "simba"; // TypeError를 던진다
```

```ts
// Zod Mini
const result = z.parse(ReadonlyUser, { name: "fido" });
result.name = "simba"; // TypeError를 던진다
```

## JSON

JSON으로 인코딩할 수 있는 모든 값을 검증하려면 다음과 같이 한다.

```ts
const jsonSchema = z.json();
```

이 API는 다음 유니언 스키마를 돌려주는 편의 API다.

```ts
const jsonSchema = z.lazy(() => {
  return z.union([
    z.string(params), 
    z.number(), 
    z.boolean(), 
    z.null(), 
    z.array(jsonSchema), 
    z.record(z.string(), jsonSchema)
  ]);
});
```

JSON 문자열을 파싱하고 다시 직렬화하는 양방향 변환은 뒤의 [json(schema) 코덱](#jsonschema)을 참고한다.

## 함수 (Functions)

Zod는 Zod로 검증되는 함수를 정의하는 `z.function()` 유틸리티를 제공한다. 이 유틸리티를 쓰면 검증 코드가 비즈니스 로직과 뒤섞이지 않는다.

```ts
const MyFunction = z.function({
  input: [z.string()], // 파라미터 (배열이나 ZodTuple이어야 한다)
  output: z.number()  // 반환 타입
});

type MyFunction = z.infer<typeof MyFunction>;
// (input: string) => number
```

함수 스키마에는 `.implement()` 메서드가 있다. 이 메서드는 함수를 받아서, 입력과 출력을 자동으로 검증하는 새 함수를 돌려준다.

```ts
const computeTrimmedLength = MyFunction.implement((input) => {
  // TypeScript는 input이 문자열이라는 것을 안다!
  return input.trim().length;
});

computeTrimmedLength("sandwich"); // => 8
computeTrimmedLength(" asdf "); // => 4
```

입력이 유효하지 않으면 이 함수는 `ZodError`를 던진다.

```ts
computeTrimmedLength(42); // ZodError를 던진다
```

입력 검증만 필요하다면 `output` 필드를 생략해도 된다.

```ts
const MyFunction = z.function({
  input: [z.string()], // 파라미터 (배열이나 ZodTuple이어야 한다)
});

const computeTrimmedLength = MyFunction.implement((input) => input.trim.length);
```

비동기 함수를 만들려면 `.implementAsync()` 메서드를 쓴다.

```ts
const computeTrimmedLengthAsync = MyFunction.implementAsync(
  async (input) => input.trim().length
);

computeTrimmedLengthAsync("sandwich"); // => Promise<8>
```

## 사용자 정의 스키마 (Custom)

`z.custom()`을 쓰면 어떤 TypeScript 타입이든 Zod 스키마로 만들 수 있다. 서드파티 라이브러리의 타입이나 내장 스키마가 다루지 않는 타입을 검증할 때 유용하다. 클래스 인스턴스라면 `z.instanceof()`를, 템플릿 리터럴 타입이라면 `z.templateLiteral()`을 쓰는 편이 낫다. 각각 [「Instanceof」](03_schemas_objects_collections.md#instanceof)와 [「템플릿 리터럴」](02_schemas_primitives.md#템플릿-리터럴-template-literals)을 참고한다.

```ts
import { Decimal } from "decimal.js";

const decimalSchema = z.custom<Decimal>((val) => Decimal.isDecimal(val));

decimalSchema.parse(new Decimal("1.5")); // 통과
decimalSchema.parse("1.5");              // 예외 발생
```

검증 함수를 넘기지 않으면 Zod는 어떤 값이든 허용한다. 위험할 수 있다!

```ts
z.custom<{ arg: string }>(); // 아무 검증도 하지 않는다
```

두 번째 인자로 에러 메시지와 기타 옵션을 지정할 수 있다. 이 파라미터는 [`.refine`](#refine)의 파라미터와 같은 방식으로 동작한다.

```ts
z.custom<...>((val) => ..., "custom error message");
```

## .apply()

`.apply()`를 쓰면 외부 함수를 Zod 메서드 체인 안에 끼워 넣을 수 있다.

```ts
function setCommonNumberChecks<T extends z.ZodNumber>(schema: T) {
  return schema
    .min(0)
    .max(100);
}

const schema = z.number()
  .apply(setCommonNumberChecks)
  .nullable();

schema.parse(0);  // => 0
schema.parse(-1); // ❌ 예외 발생
schema.parse(101); // ❌ 예외 발생
schema.parse(null); // => null
```

```ts
// Zod Mini
function setCommonNumberChecks<T extends z.ZodMiniNumber>(schema: T) {
  return schema.check(
    z.minimum(0), z.maximum(100)
  );
}

const schema = z.nullable(
  z.number().apply(setCommonNumberChecks)
);

z.parse(schema, 0);   // => 0
z.parse(schema, -1);  // ❌ 예외 발생
z.parse(schema, 101); // ❌ 예외 발생
z.parse(schema, null); // => null
```

추가 인자는 함수에 그대로 전달된다.

```ts
function withDefault<T extends z.ZodType>(schema: T, value: z.output<T>) {
  return schema.nullish().transform((val) => val ?? value);
}

const schema = z.string().apply(withDefault, "anonymous");

schema.parse(undefined); // => "anonymous"
schema.parse("sandwich"); // => "sandwich"
```

## 기존 타입에 맞추기 (Matching an existing type)

타입이 먼저 있는 경우가 있다. 데이터베이스 모델이나 생성된 클라이언트의 타입, 직접 관리하지 않는 인터페이스가 그렇다. 이런 타입을 `z.toZod<T>()`에 넘기면 TypeScript가 스키마의 출력 타입이 정확히 `T`인지 검사한다.

```ts
type Player = {
  username: string;
  xp: number;
};

const Player = z.toZod<Player>()(
  z.object({
    username: z.string(),
    xp: z.number(),
  })
);

Player.shape.username; // ZodString — 스키마는 바뀌지 않고 그대로 반환된다
```

검사 기준은 타입의 정확한 일치이므로, 타입과 조금이라도 어긋나면 컴파일 에러가 난다.

```ts
z.toZod<Player>()(
  z.object({
    username: z.string(),
    xp: z.number(),
    admin: z.boolean(), // ❌ 추가 키
  })
);
```

흔히 쓰는 대안은 `satisfies z.ZodType<Player>`이지만, 이 방식은 할당 가능성만 검사한다. 필수 키가 빠진 경우는 잡아내지만, 추가 키나 생략된 선택적 키, 맨 `z.any()`는 모두 통과한다.

```ts
z.object({
  username: z.string(),
  xp: z.number(),
  admin: z.boolean(),
}) satisfies z.ZodType<Player>; // ✅ 에러 없음

z.any() satisfies z.ZodType<Player>; // ✅ 에러 없음
```

enum 타입은 `z.enum(...)`과 일치하고 명목성을 유지하므로, 값이 같은 리터럴 유니언으로는 만족시킬 수 없다.

```ts
enum Level {
  Noob = "noob",
  Pro = "pro",
}

z.toZod<{ level: Level }>()(z.object({ level: z.enum(Level) })); // ✅
z.toZod<{ level: Level }>()(z.object({ level: z.enum(["noob", "pro"]) })); // ❌
```

## 코덱 (Codecs)

> 코덱은 `zod@4.1`에서 도입되었다.

앞의 [변환](#변환-transforms)은 입력을 출력으로 바꾸는 한 방향만 정의한다. 하지만 서버에서 받은 ISO 날짜 문자열을 `Date`로 바꿨다면, 다시 서버로 보낼 때는 `Date`를 문자열로 되돌려야 한다. 코덱(codec)은 이 두 방향의 변환을 한 쌍으로 묶은 스키마다. 코덱을 이해하려면 먼저 Zod 스키마가 값을 처리하는 두 방향부터 알아야 한다.

### 순방향과 역방향 (Forward and backward)

모든 Zod 스키마는 입력을 순방향과 역방향 두 방향으로 처리할 수 있다. 순방향 처리를 디코딩(decode), 역방향 처리를 인코딩(encode)이라고 부른다.

- **순방향(forward)**: `Input`에서 `Output`으로
  - `.parse()`
  - `.decode()`
- **역방향(backward)**: `Output`에서 `Input`으로
  - `.encode()`

대부분의 스키마는 입력 타입과 출력 타입이 같아서 "순방향"과 "역방향"이 실질적으로 다르지 않다.

```ts
const schema = z.string();

type Input = z.input<typeof schema>;    // string
type Output = z.output<typeof schema>;  // string

schema.parse("asdf");   // => "asdf"
schema.decode("asdf");  // => "asdf"
schema.encode("asdf");  // => "asdf"
```

```ts
// Zod Mini
const schema = z.string();

type Input = z.input<typeof schema>;    // string
type Output = z.output<typeof schema>;  // string

z.parse(schema, "asdf");   // => "asdf"
z.decode(schema, "asdf");  // => "asdf"
z.encode(schema, "asdf");  // => "asdf"
```

하지만 일부 스키마 타입은 입력 타입과 출력 타입을 다르게 만든다. 대표적인 것이 `z.codec()`이다. 코덱은 입력 스키마와 출력 스키마 사이의 양방향 변환(bi-directional transformation)을 정의하는 특별한 종류의 스키마로, 입력을 출력으로 바꾸는 `decode` 함수와 출력을 입력으로 되돌리는 `encode` 함수를 함께 받는다.

```ts
const stringToDate = z.codec(
  z.iso.datetime(),  // 입력 스키마: ISO 날짜 문자열
  z.date(),          // 출력 스키마: Date 객체
  {
    decode: (isoString) => new Date(isoString), // ISO 문자열 → Date
    encode: (date) => date.toISOString(),       // Date → ISO 문자열
  }
);
```

이런 스키마에서는 디코딩과 인코딩의 결과가 확연히 달라진다. 순방향인 `.parse()`와 `.decode()`는 코덱의 `decode` 함수를 호출해 문자열을 `Date`로 바꾸고, 역방향인 `.encode()`는 `encode` 함수를 호출해 `Date`를 문자열로 되돌린다. 메서드 대신 최상위 함수 `z.decode()` / `z.encode()`를 써도 된다.

```ts
stringToDate.decode("2024-01-15T10:30:00.000Z")
// => Date

stringToDate.encode(new Date("2024-01-15T10:30:00.000Z"))
// => string
```

```ts
// Zod Mini
z.decode(stringToDate, "2024-01-15T10:30:00.000Z")
// => Date

z.encode(stringToDate, new Date("2024-01-15T10:30:00.000Z"))
// => string
```

> **참고:** 방향이나 용어에 특별한 의미가 있는 것은 아니다. `A -> B` 코덱으로 인코딩하는 대신 `B -> A` 코덱으로 디코딩해도 된다. "decode"와 "encode"라는 용어는 관례일 뿐이다.

코덱은 네트워크 경계에서 데이터를 파싱할 때 특히 유용하다. 클라이언트와 서버가 Zod 스키마 하나를 공유하면서, 그 스키마로 네트워크 전송에 적합한 형식(예: JSON)과 더 풍부한 JavaScript 표현 사이를 오갈 수 있다.

### 코덱 뒤집기 (Inverting codecs)

`z.invertCodec()`을 쓰면 기존 코덱에서 반대 방향 코덱을 얻을 수 있다. 반환된 코덱은 입력 스키마와 출력 스키마를 맞바꾸고, `decode`와 `encode` 변환도 맞바꾼다.

```ts
const dateToString = z.invertCodec(stringToDate);

dateToString.decode(new Date("2024-01-15T10:30:00.000Z"));
// => string

dateToString.encode("2024-01-15T10:30:00.000Z");
// => Date
```

```ts
// Zod Mini
const dateToString = z.invertCodec(stringToDate);

z.decode(dateToString, new Date("2024-01-15T10:30:00.000Z"));
// => string

z.encode(dateToString, "2024-01-15T10:30:00.000Z");
// => Date
```

`z.invertCodec()`은 인자로 넘긴 코덱만 뒤집는다. 다른 스키마 안에 중첩된 코덱까지 재귀적으로 뒤집지는 않는다. 중첩된 코덱은 뒤집힌 스키마를 정의하는 곳에서 직접 뒤집어야 한다.

### 조합성 (Composability)

> **참고:** `z.encode()`와 `z.decode()`는 어떤 스키마에든 쓸 수 있다. 꼭 ZodCodec일 필요는 없다.

코덱도 다른 스키마와 똑같은 스키마다. 객체, 배열, 파이프 등 어디에든 중첩할 수 있으며, 쓰는 위치에 제약이 없다.

```ts
const payloadSchema = z.object({ 
  startDate: stringToDate 
});

payloadSchema.decode({
  startDate: "2024-01-15T10:30:00.000Z"
}); // => { startDate: Date }
```

객체나 레코드, 맵 안에 중첩된 코덱의 한쪽 측면만 꺼내려면 앞의 [z.input()과 z.output()](#zinput과-zoutput-zinput-and-zoutput)을 쓴다.

### 타입 안전한 입력 (Type-safe inputs)

`.parse()`와 `.decode()`는 런타임에는 똑같이 동작하지만 타입 시그니처가 다르다. `.parse()` 메서드는 `unknown`을 입력으로 받고, 스키마의 추론 출력 타입과 일치하는 값을 반환한다. 반면 `z.decode()`와 `z.encode()` 함수는 입력 타입이 엄격하게 지정되어 있다.

```ts
stringToDate.parse(12345); 
// TypeScript는 아무 불평도 하지 않는다 (런타임에 실패)

stringToDate.decode(12345); 
// ❌ TypeScript error: Argument of type 'number' is not assignable to parameter of type 'string'.

stringToDate.encode(12345); 
// ❌ TypeScript error: Argument of type 'number' is not assignable to parameter of type 'Date'.
```

왜 이런 차이를 둘까? 인코딩과 디코딩은 변환을 전제로 한다. 이 메서드들의 입력은 대개 애플리케이션 코드 안에서 이미 타입이 정해져 있으므로, `z.decode` / `z.encode`는 엄격한 타입의 입력을 받아 실수를 컴파일 시점에 드러낸다. 정리하면 `parse()`는 `unknown`을 받아 `Output`을, `decode()`는 `Input`을 받아 `Output`을, `encode()`는 `Output`을 받아 `Input`을 반환한다.

### 비동기 변형과 safe 변형 (Async and safe variants)

`.transform()`, `.refine()`과 마찬가지로 코덱도 비동기 변환을 지원한다.

```ts
const asyncCodec = z.codec(z.string(), z.number(), {
  decode: async (str) => Number(str),
  encode: async (num) => num.toString(),
});
```

일반 `parse()`처럼 `decode()`와 `encode()`에도 "safe" 변형과 "async" 변형이 있다.

```ts
stringToDate.decode("2024-01-15T10:30:00.000Z"); 
// => Date

stringToDate.decodeAsync("2024-01-15T10:30:00.000Z"); 
// => Promise<Date>

stringToDate.safeDecode("2024-01-15T10:30:00.000Z"); 
// => { success: true, data: Date } | { success: false, error: ZodError }

stringToDate.safeDecodeAsync("2024-01-15T10:30:00.000Z"); 
// => Promise<{ success: true, data: Date } | { success: false, error: ZodError }>
```

### 인코딩 동작 방식 (How encoding works)

인코딩은 파싱을 "거꾸로" 수행하는 것인데, 스키마 종류에 따라 거꾸로 한다는 것의 의미가 조금씩 다르다. 아래에서 종류별로 살펴본다.

#### 코덱 (Codecs)

코덱은 설명이 거의 필요 없다. 두 타입 사이의 양방향 변환을 캡슐화한다. `z.decode()`는 `decode` 변환을 실행해 입력을 파싱된 값으로 바꾸고, `z.encode()`는 `encode` 변환을 실행해 그 값을 다시 직렬화한다. 앞의 `stringToDate`로 예를 들면 다음과 같다.

```ts
stringToDate.decode("2024-01-15T10:30:00.000Z"); 
// => Date

stringToDate.encode(new Date("2024-01-15")); 
// => string
```

#### 파이프 (Pipes)

> **참고:** 코덱은 내부적으로 파이프의 하위 클래스로 구현되어 있으며, 중간에 끼어드는 변환 로직을 추가로 가진다.

일반 디코딩에서 `ZodPipe<A, B>` 스키마는 먼저 `A`로 데이터를 파싱한 뒤 그 결과를 `B`에 넘긴다. 예상할 수 있듯이 인코딩에서는 먼저 `B`로 데이터를 인코딩한 뒤 그 결과를 `A`에 넘긴다.

#### 정제 (Refinements)

모든 검사(`.refine()`, `.min()`, `.max()` 등)는 양방향 모두에서 실행된다.

```ts
const schema = stringToDate.refine((date) => date.getFullYear() >= 2000, "Must be this millennium");

schema.encode(new Date("2000-01-01"));
// => Date

schema.encode(new Date("1999-01-01"));
// => ❌ ZodError: [
//   {
//     "code": "custom",
//     "path": [],
//     "message": "Must be this millennium"
//   }
// ]
```

사용자 정의 `.refine()` 로직에서 예상치 못한 에러가 나지 않도록 Zod는 `z.encode()` 중에 두 번의 패스(pass)를 수행한다. 첫 번째 패스에서는 입력이 기대하는 타입에 맞는지(`invalid_type` 에러가 없는지) 확인한다. 이 패스를 통과하면 두 번째 패스에서 정제 로직을 실행한다.

이 방식 덕분에 `z.string().trim()`이나 `z.string().toLowerCase()` 같은 "값을 바꾸는 정제"도 쓸 수 있다.

```ts
const schema = z.string().trim();

schema.decode("  hello  ");
// => "hello"

schema.encode("  hello  ");
// => "hello"
```

#### 기본값과 사전 기본값 (Defaults and prefaults)

기본값과 사전 기본값은 순방향에서만 적용된다.

```ts
const stringWithDefault = z.string().default("hello");

stringWithDefault.decode(undefined); 
// => "hello"

stringWithDefault.encode(undefined); 
// => ZodError: Expected string, received undefined
```

스키마에 기본값을 붙이면 입력 타입은 선택적(`| undefined`)이 되지만 출력 타입은 그렇지 않다. 따라서 `undefined`는 `z.encode()`의 유효한 입력이 아니며, 기본값과 사전 기본값도 적용되지 않는다.

#### 폴백 값 (Catch)

마찬가지로 `.catch()`도 순방향에서만 적용된다.

```ts
const stringWithCatch = z.string().catch("hello");

stringWithCatch.decode(1234); 
// => "hello"

stringWithCatch.encode(1234); 
// => ZodError: Expected string, received number
```

#### Stringbool

> **참고:** `z.stringbool()`은 Zod에 코덱이 도입되기 전부터 있던 API다. 이후 내부적으로 코덱으로 다시 구현되었다. API 자체는 [「문자열 불리언」](02_schemas_primitives.md#문자열-불리언-stringbools)을 참고한다.

`z.stringbool()` API는 문자열 값(`"true"`, `"false"`, `"yes"`, `"no"` 등)을 `boolean`으로 바꾼다. `z.encode()`에서는 기본적으로 `true`를 `"true"`로, `false`를 `"false"`로 바꾼다.

```ts
const stringbool = z.stringbool();

stringbool.decode("true");  // => true
stringbool.decode("false"); // => false

stringbool.encode(true);    // => "true"
stringbool.encode(false);   // => "false"
```

`truthy`와 `falsy` 값 목록을 직접 지정했다면 각 배열의 첫 번째 원소가 대신 쓰인다.

```ts
const stringbool = z.stringbool({ truthy: ["yes", "y"], falsy: ["no", "n"] });

stringbool.encode(true);    // => "yes"
stringbool.encode(false);   // => "no"
```

#### 변환 (Transforms)

> **주의:** `.transform()` API는 단방향 변환을 구현한다. 스키마 어딘가에 `.transform()`이 하나라도 있으면 `z.encode()` 연산은 `ZodError`가 아닌 런타임 에러를 던진다.

```ts
const schema = z.string().transform(val => val.length);

schema.encode(1234); 
// ❌ Error: Encountered unidirectional transform during encode: ZodTransform
```

### 유용한 코덱 (Useful codecs)

아래는 자주 필요한 코덱의 구현이다. 사용자가 자유롭게 고칠 수 있도록 Zod 자체의 일급 API에는 넣지 않았다. 프로젝트에 복사해 붙여 넣은 뒤 필요에 맞게 수정해서 쓴다.

> **참고:** 아래 코덱 구현은 정확성 테스트를 거쳤다.

문자열과 불리언 사이의 코덱(`stringToBoolean`)은 따로 구현할 필요 없이 앞의 [Stringbool](#stringbool)을 쓴다.

#### stringToNumber

`parseFloat()`로 숫자를 나타내는 문자열을 JavaScript `number` 타입으로 바꾼다.

```ts
const stringToNumber = z.codec(z.string().regex(z.regexes.number), z.number(), {
  decode: (str) => Number.parseFloat(str),
  encode: (num) => num.toString(),
});

stringToNumber.decode("42.5");  // => 42.5
stringToNumber.encode(42.5);    // => "42.5"
```

#### stringToInt

`parseInt()`로 정수를 나타내는 문자열을 JavaScript `number` 타입으로 바꾼다.

```ts
const stringToInt = z.codec(z.string().regex(z.regexes.integer), z.int(), {
  decode: (str) => Number.parseInt(str, 10),
  encode: (num) => num.toString(),
});

stringToInt.decode("42");  // => 42
stringToInt.encode(42);    // => "42"
```

#### stringToBigInt

문자열 표현을 JavaScript `bigint` 타입으로 바꾼다.

```ts
const stringToBigInt = z.codec(z.string(), z.bigint(), {
  decode: (str) => BigInt(str),
  encode: (bigint) => bigint.toString(),
});

stringToBigInt.decode("12345");  // => 12345n
stringToBigInt.encode(12345n);   // => "12345"
```

#### numberToBigInt

JavaScript `number`를 `bigint` 타입으로 바꾼다.

```ts
const numberToBigInt = z.codec(z.int(), z.bigint(), {
  decode: (num) => BigInt(num),
  encode: (bigint) => Number(bigint),
});

numberToBigInt.decode(42);   // => 42n
numberToBigInt.encode(42n);  // => 42
```

#### isoDatetimeToDate

ISO 날짜시간 문자열을 JavaScript `Date` 객체로 바꾼다.

```ts
const isoDatetimeToDate = z.codec(z.iso.datetime(), z.date(), {
  decode: (isoString) => new Date(isoString),
  encode: (date) => date.toISOString(),
});

isoDatetimeToDate.decode("2024-01-15T10:30:00.000Z");  // => Date 객체
isoDatetimeToDate.encode(new Date("2024-01-15"));       // => "2024-01-15T00:00:00.000Z"
```

#### epochSecondsToDate

Unix 타임스탬프(epoch 이후 경과한 초)를 JavaScript `Date` 객체로 바꾼다.

```ts
const epochSecondsToDate = z.codec(z.int().min(0), z.date(), {
  decode: (seconds) => new Date(seconds * 1000),
  encode: (date) => Math.floor(date.getTime() / 1000),
});

epochSecondsToDate.decode(1705314600);  // => Date 객체
epochSecondsToDate.encode(new Date());  // => 초 단위 Unix 타임스탬프
```

#### epochMillisToDate

Unix 타임스탬프(epoch 이후 경과한 밀리초)를 JavaScript `Date` 객체로 바꾼다.

```ts
const epochMillisToDate = z.codec(z.int().min(0), z.date(), {
  decode: (millis) => new Date(millis),
  encode: (date) => date.getTime(),
});

epochMillisToDate.decode(1705314600000);  // => Date 객체
epochMillisToDate.encode(new Date());     // => 밀리초 단위 Unix 타임스탬프
```

#### json(schema)

JSON 문자열을 구조화된 데이터로 파싱하고, 다시 JSON으로 직렬화한다. 이 제네릭 함수는 파싱한 JSON 데이터를 검증할 출력 스키마를 인자로 받는다.

```ts
const jsonCodec = <T extends z.core.$ZodType>(schema: T) =>
  z.codec(z.string(), schema, {
    decode: (jsonString, ctx) => {
      try {
        return JSON.parse(jsonString);
      } catch (err: any) {
        ctx.issues.push({
          code: "invalid_format",
          format: "json",
          input: jsonString,
          message: err.message,
        });
        return z.NEVER;
      }
    },
    encode: (value) => JSON.stringify(value),
  });
```

특정 스키마와 함께 쓰는 예는 다음과 같다.

```ts
const jsonToObject = jsonCodec(z.object({ name: z.string(), age: z.number() }));

jsonToObject.decode('{"name":"Alice","age":30}');  
// => { name: "Alice", age: 30 }

jsonToObject.encode({ name: "Bob", age: 25 });     
// => '{"name":"Bob","age":25}'

jsonToObject.decode('~~invalid~~'); 
// ZodError: [
//   {
//     "code": "invalid_format",
//     "format": "json",
//     "path": [],
//     "message": "Unexpected token '~', \"~~invalid~~\" is not valid JSON"
//   }
// ]
```

#### utf8ToBytes

UTF-8 문자열을 `Uint8Array` 바이트 배열로 바꾼다.

```ts
const utf8ToBytes = z.codec(z.string(), z.instanceof(Uint8Array), {
  decode: (str) => new TextEncoder().encode(str),
  encode: (bytes) => new TextDecoder().decode(bytes),
});

utf8ToBytes.decode("Hello, 世界!");  // => Uint8Array
utf8ToBytes.encode(bytes);          // => "Hello, 世界!"
```

#### bytesToUtf8

`Uint8Array` 바이트 배열을 UTF-8 문자열로 바꾼다.

```ts
const bytesToUtf8 = z.codec(z.instanceof(Uint8Array), z.string(), {
  decode: (bytes) => new TextDecoder().decode(bytes),
  encode: (str) => new TextEncoder().encode(str),
});

bytesToUtf8.decode(bytes);          // => "Hello, 世界!"
bytesToUtf8.encode("Hello, 世界!");  // => Uint8Array
```

#### base64ToBytes

base64 문자열과 `Uint8Array` 바이트 배열을 서로 바꾼다.

```ts
const base64ToBytes = z.codec(z.base64(), z.instanceof(Uint8Array), {
  decode: (base64String) => z.util.base64ToUint8Array(base64String),
  encode: (bytes) => z.util.uint8ArrayToBase64(bytes),
});

base64ToBytes.decode("SGVsbG8=");  // => Uint8Array([72, 101, 108, 108, 111])
base64ToBytes.encode(bytes);       // => "SGVsbG8="
```

#### base64urlToBytes

base64url 문자열(URL에 안전한 base64)을 `Uint8Array` 바이트 배열로 바꾼다.

```ts
const base64urlToBytes = z.codec(z.base64url(), z.instanceof(Uint8Array), {
  decode: (base64urlString) => z.util.base64urlToUint8Array(base64urlString),
  encode: (bytes) => z.util.uint8ArrayToBase64url(bytes),
});

base64urlToBytes.decode("SGVsbG8");  // => Uint8Array([72, 101, 108, 108, 111])
base64urlToBytes.encode(bytes);      // => "SGVsbG8"
```

#### hexToBytes

16진수 문자열과 `Uint8Array` 바이트 배열을 서로 바꾼다.

```ts
const hexToBytes = z.codec(z.hex(), z.instanceof(Uint8Array), {
  decode: (hexString) => z.util.hexToUint8Array(hexString),
  encode: (bytes) => z.util.uint8ArrayToHex(bytes),
});

hexToBytes.decode("48656c6c6f");     // => Uint8Array([72, 101, 108, 108, 111])
hexToBytes.encode(bytes);            // => "48656c6c6f"
```

#### stringToURL

URL 문자열을 JavaScript `URL` 객체로 바꾼다.

```ts
const stringToURL = z.codec(z.url(), z.instanceof(URL), {
  decode: (urlString) => new URL(urlString),
  encode: (url) => url.href,
});

stringToURL.decode("https://example.com/path");  // => URL 객체
stringToURL.encode(new URL("https://example.com"));  // => "https://example.com/"
```

#### stringToHttpURL

HTTP/HTTPS URL 문자열을 JavaScript `URL` 객체로 바꾼다.

```ts
const stringToHttpURL = z.codec(z.httpUrl(), z.instanceof(URL), {
  decode: (urlString) => new URL(urlString),
  encode: (url) => url.href,
});

stringToHttpURL.decode("https://api.example.com/v1");  // => URL 객체
stringToHttpURL.encode(url);                           // => "https://api.example.com/v1"
```

#### uriComponent

`encodeURIComponent()`와 `decodeURIComponent()`로 URI 컴포넌트를 인코딩하고 디코딩한다.

```ts
const uriComponent = z.codec(z.string(), z.string(), {
  decode: (encodedString) => decodeURIComponent(encodedString),
  encode: (decodedString) => encodeURIComponent(decodedString),
});

uriComponent.decode("Hello%20World%21");  // => "Hello World!"
uriComponent.encode("Hello World!");      // => "Hello%20World!"
```

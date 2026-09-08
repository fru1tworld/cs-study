# Zod 소개와 기본 사용법

> 원문: https://zod.dev/
> 원문: https://zod.dev/basics

Zod는 TypeScript를 우선으로 설계한 스키마 검증 라이브러리로, 정적 타입 추론(static type inference)을 함께 제공한다. 스키마를 한 번 정의하면 런타임에 데이터를 검사하는 데도 쓰고, 같은 스키마에서 TypeScript 타입을 뽑아 쓸 수도 있다는 뜻이다([「타입 추론」](#타입-추론-inferring-types)). 이 문서는 Zod 4(4.6.5 기준)를 대상으로 한다. Zod 4는 안정 버전이며, 이전 버전과 달라진 점은 [릴리스 노트와 마이그레이션 가이드](08_zod4_migration.md)에 정리되어 있다.

## 소개 (Introduction)

Zod에서는 데이터 검증에 쓸 *스키마*(schema)를 정의한다. 스키마는 데이터가 어떤 모양이어야 하는지 적어 둔 규칙으로, 단순한 `string`부터 복잡하게 중첩된 객체까지 표현할 수 있다.

```ts
import * as z from "zod";

const User = z.object({
  name: z.string(),
});

// 신뢰할 수 없는 데이터...
const input = { /* stuff */ };

// 파싱 결과는 검증을 거쳤고 타입도 안전하다!
const data = User.parse(input);

// 그러니 안심하고 쓰면 된다 :)
console.log(data.name);
```

## 특징 (Features)

- 외부 의존성 없음
- Node.js와 모든 최신 브라우저에서 동작
- 작은 크기: 코어 번들 2kb (gzip 기준)
- 불변(immutable) API: 메서드는 기존 스키마를 바꾸지 않고 새 인스턴스를 반환
- 간결한 인터페이스
- TypeScript와 일반 JavaScript 모두에서 사용 가능
- JSON Schema 변환 기능 내장([「JSON Schema 개요」](06_metadata_and_json_schema.md#json-schema-개요-json-schema))
- 폭넓은 생태계

## 설치 (Installation)

```sh
npm install zod
```

jsr.io에서는 [`@zod/zod`](https://jsr.io/@zod/zod)라는 이름으로도 배포된다.

AI 도구에서 Zod 문서를 참고할 수 있는 자료도 있다. Zod는 에이전트가 Zod 문서를 검색할 수 있게 해 주는 MCP 서버를 제공한다. MCP(Model Context Protocol)는 AI 에이전트가 외부 도구나 자료에 접근할 때 쓰는 프로토콜이다. 에디터에 추가하려면 [안내 문서](https://share.inkeep.com/zod/mcp)를 따르면 된다. LLM이 읽기 좋게 문서를 텍스트로 정리한 [llms.txt](https://zod.dev/llms.txt) 파일도 제공한다.

## 요구 사항 (Requirements)

Zod는 *TypeScript v5.5* 이상을 기준으로 테스트된다. 그보다 오래된 버전에서도 동작할 수는 있지만 공식적으로 지원하지 않는다.

### `"strict"`

`tsconfig.json`에서 `strict` 모드를 반드시 켜야 한다. Zod와 관계없이 모든 TypeScript 프로젝트에 권장하는 설정이기도 하다.

```ts
// tsconfig.json
{
  // ...
  "compilerOptions": {
    // ...
    "strict": true
  }
}
```

## 생태계 (Ecosystem)

Zod를 지원하거나 Zod를 기반으로 만든 라이브러리와 도구가 많고, 다른 도구와의 연동도 지원한다. 전체 목록은 원문의 [Ecosystem 페이지](https://zod.dev/ecosystem)에 있다. 이 페이지는 리소스(Resources), API 라이브러리(API Libraries), 폼 통합(Form Integrations), 목(mock) 라이브러리(Mocking Libraries), Zod 기반 프로젝트(Powered by Zod) 같은 항목으로 나뉜다. Zod to X는 Zod 스키마를 OpenAPI 문서 같은 다른 형식으로 바꾸는 도구를, X to Zod는 반대로 다른 형식에서 Zod 스키마를 만들어 내는 도구를 모은 항목이다. 자세한 내용은 [「생태계 (Ecosystem)」](07_packages_and_compile.md#생태계-ecosystem)를 참고한다.

원문은 Zod 저자가 직접 기여하는 프로젝트로 다음 세 가지를 소개한다.

- [tRPC](https://trpc.io): 종단 간 타입 안전한(end-to-end typesafe) API, 즉 서버에서 정의한 타입을 클라이언트 코드까지 그대로 이어 쓰는 API를 만드는 라이브러리로, Zod 스키마를 지원한다.
- [React Hook Form](https://react-hook-form.com): 훅 기반 폼 검증 라이브러리다. 폼 값을 Zod 스키마로 검증하도록 연결해 주는 [Zod resolver](https://react-hook-form.com/docs/useform#resolver)를 제공한다.
- [zshy](https://github.com/colinhacks/zshy): 필요한 기능을 모두 갖춘 TypeScript 라이브러리용 빌드 도구로, 번들러 없이 `tsc`로 동작한다. 원래 Zod 내부 빌드 도구로 만들어졌다.

## 기본 사용법 (Basic usage)

여기서부터는 스키마를 만들고, 데이터를 파싱하고, 추론된 타입을 사용하는 기본 흐름을 다룬다. 스키마 API 전체는 [원시 타입 스키마](02_schemas_primitives.md)와 [객체·컬렉션 스키마](03_schemas_objects_collections.md) 문서에서 이어서 설명한다.

### 스키마 정의 (Defining a schema)

데이터를 검증하려면 먼저 스키마를 정의해야 한다. 여기서는 간단한 객체 스키마를 예로 든다.

```ts
import * as z from "zod"; 

const Player = z.object({ 
  username: z.string(),
  xp: z.number()
});
```

```ts
// Zod Mini
import * as z from "zod/mini"

const Player = z.object({ 
  username: z.string(),
  xp: z.number()
});
```

Zod Mini는 같은 기능을 함수형 API로 제공하는 경량 패키지다. 자세한 내용은 [「Zod Mini」](07_packages_and_compile.md#zod-mini)를 참고한다.

### 데이터 파싱 (Parsing data)

어떤 Zod 스키마든 `.parse`로 입력을 검증할 수 있다. Zod에서 파싱(parsing)은 입력이 스키마에 맞는지 검사하고, 맞으면 타입이 확정된 값을 돌려주는 과정이다. 입력이 유효하면 Zod는 입력의 *깊은 복사본*(deep clone)을 반환한다. 중첩된 객체까지 새로 만든 값이라서 반환값은 원래 입력과 다른 객체이며, 반환값의 타입은 스키마에서 추론한다. 입력이 유효하지 않을 때의 동작은 [「에러 처리」](#에러-처리-handling-errors)에서 다룬다.

```ts
Player.parse({ username: "billie", xp: 100 }); 
// => { username: "billie", xp: 100 } 반환
```

#### 비동기 정제와 변환

스키마에는 타입 검사 말고도 직접 만든 규칙을 붙일 수 있다. `.refine()`으로 붙이는 사용자 정의 검사를 [정제(refinement)](04_refinements_transforms_codecs.md#정제-refinements), `.transform()`으로 검증을 통과한 값을 다른 값으로 바꾸는 단계를 [변환(transform)](04_refinements_transforms_codecs.md#변환-transforms)이라고 한다. 이때 넘기는 함수가 `async` 함수이면 비동기 정제나 비동기 변환이 된다.

예를 들어 사용자 이름이 이미 쓰이고 있는지 확인하려면 DB를 조회해야 하므로, 정제 함수가 Promise를 반환한다(`db.userExists`는 설명을 위한 가상의 함수다).

```ts
const Username = z.string().refine(async (name) => {
  return !(await db.userExists(name));
}, "이미 사용 중인 이름");
```

`.parse()`는 결과를 곧바로 반환하는 동기 메서드라서 이 Promise가 끝나기를 기다릴 수 없다. 그래서 비동기 정제나 변환이 하나라도 들어간 스키마는 `.parseAsync()`로 파싱해야 하며, `.parse()`로 파싱하면 Zod가 에러를 던진다.

```ts
await Username.parseAsync("billie");
```

### 에러 처리 (Handling errors)

검증에 실패하면 `.parse()`는 `ZodError` 인스턴스를 던진다. 이 인스턴스의 `issues` 배열에는 검증 이슈(issue)가 담긴다. 이슈 하나는 실패 한 건을 뜻하며, 어느 위치(`path`)의 값이 어떤 이유(`code`, `message`)로 실패했는지 기록한다. 아래 예에서는 `username`과 `xp`가 모두 잘못되어 이슈가 두 개 생긴다.

```ts
try {
  Player.parse({ username: 42, xp: "100" });
} catch(error){
  if(error instanceof z.ZodError){
    error.issues; 
    /* [
      {
        expected: 'string',
        code: 'invalid_type',
        path: [ 'username' ],
        message: 'Invalid input: expected string, received number'
      },
      {
        expected: 'number',
        code: 'invalid_type',
        path: [ 'xp' ],
        message: 'Invalid input: expected number, received string'
      }
    ] */
  }
}
```

Zod Mini에서는 `z.core.$ZodError`로 검사하며, 기본 메시지가 `'Invalid input'`으로 더 짧다. `ZodError`와 이슈의 구조는 [「ZodError와 이슈」](05_errors.md#zoderror와-이슈-zoderror-and-issues)에서, 에러 메시지를 바꾸거나 보기 좋게 출력하는 방법은 [「Zod 에러 커스터마이징과 포맷팅」](05_errors.md)에서 다룬다.

#### `.safeParse()`

`try/catch` 블록을 쓰고 싶지 않다면 `.safeParse()` 메서드를 쓴다. 이 메서드는 에러를 던지는 대신, 파싱에 성공한 데이터나 `ZodError` 중 하나를 담은 일반 객체를 반환한다. 결과 타입은 [판별 유니언(discriminated union)](https://www.typescriptlang.org/docs/handbook/2/narrowing.html#discriminated-unions)이다. 판별 유니언은 공통 필드의 값으로 어느 쪽 타입인지 구분하는 유니언 타입으로, 여기서는 `success` 필드가 그 역할을 한다. `result.success`를 검사하면 TypeScript가 각 분기에서 `result`의 타입을 좁혀 주므로, 실패 분기에서는 `error`를, 성공 분기에서는 `data`를 바로 꺼내 쓸 수 있다.

```ts
const result = Player.safeParse({ username: 42, xp: "100" });
if (!result.success) {
  result.error;   // ZodError 인스턴스
} else {
  result.data;    // { username: string; xp: number }
}
```

> **참고:** `.safeParse()`도 동기 메서드라서, [비동기 정제나 변환](#비동기-정제와-변환)이 들어간 스키마에는 `.safeParseAsync()`를 대신 써야 한다.
>
> ```ts
> await schema.safeParseAsync("hello");
> ```

#### `.validate()`

입력이 허용되는지만 알면 될 때는 `.validate()`를 쓴다. 이 메서드는 불리언을 반환할 뿐 에러 객체를 전혀 만들지 않는다. 또 스키마의 입력 타입에 대한 타입 가드(type guard)로 동작한다. `if (Player.validate(input))`처럼 조건문에 쓰면, 그 블록 안에서 TypeScript가 `input`의 타입을 스키마의 입력 타입(변환 전 타입, [「타입 추론」](#타입-추론-inferring-types) 참고)으로 좁혀 준다는 뜻이다. 유효하지 않은 입력에서는 `.safeParse().success`보다 최대 16배 빠르다. 최상위 함수 `z.validate(schema, data)`도 있으며, `zod/mini`에서는 이 형태를 쓴다.

```ts
Player.validate({ username: "billie", xp: 100 }); // true
Player.validate({ username: 42, xp: "100" });     // false
```

`.validate()` 역시 결과를 곧바로 반환하므로, [비동기 정제나 변환](#비동기-정제와-변환)이 들어간 스키마에는 `.validateAsync()`를 쓴다. 스키마를 미리 최적화된 검증 코드로 바꿔 두는 [AOT 컴파일](07_packages_and_compile.md#aot-컴파일-aot-compilation)을 거친 스키마에서는, `.validate()`가 그 컴파일된 빠른 경로(fast path)에서 바로 결과를 돌려준다.

### 타입 추론 (Inferring types)

Zod는 스키마 정의로부터 정적 타입을 추론한다. 이 타입은 `z.infer<>` 유틸리티로 꺼내 원하는 곳에 쓸 수 있다.

```ts
const Player = z.object({ 
  username: z.string(),
  xp: z.number()
});

// 추론된 타입 추출
type Player = z.infer<typeof Player>;

// 코드에서 사용
const player: Player = { username: "billie", xp: 100 };
```

스키마의 입력 타입과 출력 타입이 달라지는 경우도 있다. 앞에서 본 [변환](#비동기-정제와-변환)이 그렇다. 아래 스키마는 문자열을 받아 길이(숫자)를 내보내므로, `.parse()`에 넘기는 값과 돌려받는 값의 타입이 다르다. 이럴 때는 입력 타입과 출력 타입을 따로 추출한다.

```ts
const mySchema = z.string().transform((val) => val.length);

type MySchemaIn = z.input<typeof mySchema>;
// => string

type MySchemaOut = z.output<typeof mySchema>; // z.infer<typeof mySchema>와 같음
// number
```

기본기는 여기까지다. 다음 문서부터 스키마 API를 하나씩 살펴본다.

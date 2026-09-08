# Zod 4 릴리스와 마이그레이션

> 원문: https://zod.dev/v4/versioning
> 원문: https://zod.dev/v4
> 원문: https://zod.dev/v4/changelog

이 문서는 Zod 4 (4.6.5 기준)의 버전 정책, 릴리스 노트의 요지, Zod 3에서 Zod 4로 옮길 때의 호환성이 깨지는 변경(breaking change)을 다룬다. 릴리스 노트에서 소개한 새 기능은 대부분 다른 문서에서 자세히 설명하므로 여기서는 Zod 3과 무엇이 달라졌는지만 짚고 해당 문서로 연결한다.

## 버전 정책 (Versioning)

### 2025년 7월 8일 업데이트 (Update — July 8th, 2025)

`zod@4.0.0`이 npm에 배포되었다. 여기서 패키지 루트는 `import * as z from "zod"`처럼 패키지 이름만으로 가져오는 경로를, 서브패스(subpath)는 `"zod/v4"`, `"zod/v3"`처럼 패키지 이름 뒤에 경로를 붙여 가져오는 진입점을 말한다. 이번 배포로 패키지 루트(`"zod"`)는 Zod 4를 내보내고, 나머지 서브패스는 그대로 앞으로도 계속 제공된다.

업그레이드는 다음 명령 하나면 된다.

```
npm install zod@^4.0.0
```

서브패스로 이미 Zod 4를 쓰고 있었다면 기존 import 경로(`"zod/v4"`, `"zod/v4-mini"`)도 계속 동작하므로 당장 고칠 코드는 없다. 업그레이드한 뒤 원한다면 import 경로를 다음처럼 바꿀 수 있다. Zod 3을 계속 써야 하는 코드는 반대로 `"zod"`에서 `"zod/v3"`로 옮겨야 한다는 점에 주의한다.

- Zod 4
  - 이전: `"zod/v4"`
  - 이후: `"zod"`
- Zod 4 Mini
  - 이전: `"zod/v4-mini"`
  - 이후: `"zod/mini"`
- Zod 3
  - 이전: `"zod"`
  - 이후: `"zod/v3"`

라이브러리 쪽 사정은 조금 다르다. Zod 스키마를 받는 라이브러리는 보통 Zod를 피어 의존성(peer dependency)으로 둔다. 피어 의존성은 라이브러리가 Zod를 직접 설치해 들고 다니지 않고, 사용자 프로젝트에 설치된 Zod를 함께 쓰겠다고 `package.json`에 선언하는 의존성이다. 라이브러리 작성자 가이드의 권장 방식대로 이미 Zod 4 지원을 구현한 라이브러리라면 이 peer dependency 범위에 `zod@^4.0.0`만 추가하면 된다. 가이드 내용은 [「라이브러리 작성자 가이드」](07_packages_and_compile.md#라이브러리-작성자-가이드-for-library-authors)를 참고한다.

```json
// package.json
{
  "peerDependencies": {
    "zod": "^3.25.0 || ^4.0.0"
  }
}
```

그 밖의 코드 변경은 필요 없다. 최신 `3.25.x` 릴리스와 `4.0.0` 사이에는 코드 변경이 없었으므로 라이브러리 쪽에서 메이저 버전을 올릴 필요도 없다.

#### 서브패스 버전 관리에 대한 메모 (Some notes on subpath versioning)

서브패스 방식은 생태계가 호환성을 깨지 않고 업그레이드하도록 만들기 위한 어쩔 수 없는 선택이었다. 처음부터 `zod@4.0.0`을 배포했다면 대부분의 라이브러리가 peer dependency를 단순히 `^4.0.0`으로 올렸을 것이다. 그러면 그 라이브러리를 쓰는 사용자도 Zod를 4로 올려야 하므로 라이브러리 자신도 메이저 버전을 올려야 하고, 그 라이브러리에 기대는 다른 라이브러리도 줄줄이 따라 올려야 한다. 원문은 이렇게 생태계 전체로 번지는 연쇄 업그레이드를 "버전 올림 눈사태(version bump avalanche)"라고 부른다.

다행히 우려했던 눈사태는 일어나지 않았다. 고통이 전혀 없는 마이그레이션은 없지만, 현재 생태계 전반에서 Zod 4를 [폭넓게 지원](https://x.com/colinhacks/status/1932323805705482339)한다. Hono, LangChain, React Hook Form 등 대부분의 라이브러리가 Zod 3과 Zod 4를 동시에 지원할 수 있었다. 여러 생태계 메인테이너가 보통은 메이저 버전을 올려야 했을 Zod 4 지원을 점진적으로 추가할 수 있어 편했다고 직접 알려 오기도 했다. 원저자는 이 방식이 잘 통했다고 평가하며, Zod와 같은 제약을 가진 라이브러리는 드물지만 큰 생태계를 거느린 라이브러리라면 비슷한 방식을 검토해 보라고 권한다.

### Zod 4의 버전 관리 방식 (Versioning in Zod 4)

위 업데이트는 처음부터 계획된 2단계 전환의 마지막 단계다. 사용자와 생태계 라이브러리가 Zod 4로 쉽게 옮겨 갈 수 있도록, 첫 단계에서는 다음 방침을 세웠다.

* Zod 4는 처음에는 npm에 `zod@4.0.0`으로 배포하지 않고, `zod@3.25.0`에 서브패스(`"zod/v4"`)로 함께 실어 내보낸다.
* 그렇더라도 Zod 4는 안정적이고 프로덕션에 쓸 수 있는 상태로 간주한다.
* Zod 3은 계속 패키지 루트(`"zod"`)와 새 서브패스 `"zod/v3"`에서 내보내며, 버그 수정과 안정성 개선도 계속 받는다.

> 이 방식은 Go가 메이저 버전 변경을 다루는 방식과 비슷하다: [https://go.dev/doc/modules/major-version](https://go.dev/doc/modules/major-version)

생태계가 충분히 따라온 뒤의 두 번째 단계는 다음과 같다.

* 패키지 루트(`"zod"`)가 내보내는 대상을 Zod 3에서 Zod 4로 전환한다.
* 이 시점에 npm에 `zod@4.0.0`을 배포한다.
* `"zod/v4"` 서브패스는 영구히 유지한다.

### 서브패스 방식을 택한 이유 (Why?)

Zod는 생태계에서 독특한 위치에 있다. 많은 라이브러리와 프레임워크가 사용자가 정의한 Zod 스키마를 입력으로 받으므로, 이들의 공개 API는 Zod의 여러 클래스, 인터페이스, 유틸리티에 강하게 결합되어 있다. 그래서 Zod의 호환성이 깨지면 이런 라이브러리를 쓰는 사용자에게까지 그 여파가 전파된다. 예를 들어 Zod 3의 `ZodType`은 Zod 4의 `ZodType`에 할당할 수 없으므로, 파라미터 타입을 Zod 4의 `ZodType`으로 선언한 라이브러리 함수에는 Zod 3 스키마를 넘길 수 없다.

#### 라이브러리가 v3와 v4를 동시에 지원하기 어려운 이유 (Why can't libraries just support v3 and v4 simultaneously?)

peerDependencies의 한계와 패키지 매니저 간의 동작 차이 때문에 한 라이브러리의 두 메이저 버전을 동시에 깔끔하게 지원하기는 매우 어렵다.

`zod@4.0.0`을 그냥 npm에 배포했다면, Zod 생태계의 라이브러리 대부분이 Zod 4를 제대로 지원하기 위해 새 메이저 버전을 내야 했을 것이다. AI SDK 같은 유명 라이브러리도 예외가 아니다. 결국 [앞에서 말한](#서브패스-버전-관리에-대한-메모-some-notes-on-subpath-versioning) 버전 올림 눈사태가 생태계 전체에 일어나 엄청난 불만과 작업량을 낳았을 것이다.

서브패스 버전 관리는 라이브러리가 Zod 3과 Zod 4(Zod Mini 포함)를 동시에 지원하는 단순한 경로를 제공해 이 문제를 해결한다. 라이브러리는 `"zod"`에 대한 peerDependency 하나만 유지하면 되고, npm 별칭(alias), 선택적 peer dependency, `"zod-compat"` 패키지 같은 우회책은 필요 없다. 이 우회책들이 왜 통하지 않는지는 [아래](#peer-dependency로는-해결되지-않는-이유)에서 하나씩 다룬다.

구체적으로는 라이브러리가 `"zod"` peer dependency의 최소 버전을 `zod@^3.25.0`으로 올리면 된다. `zod@3.25.0`부터는 한 패키지 안에 두 버전이 서브패스로 함께 들어 있으므로, 구현 안에서 Zod 3과 Zod 4를 모두 참조할 수 있다.

```ts
import * as z3 from "zod/v3"
import * as z4 from "zod/v4"
```

v4 지원이 충분히 퍼진 뒤에는 npm의 메이저 버전을 올리고 패키지 루트에서 Zod 4를 내보내 전환을 마무리한다. (이 단계는 이미 이루어졌다. 이 절 맨 위의 업데이트를 참고한다.)

라이브러리가 루트가 아닌 해당 서브패스에서만 import한다면, 메이저 버전이 바뀌어도 코드 수정 없이 계속 동작한다.

Go를 쓰지 않는 사람에게는 낯설어 보일 수 있지만, 원저자가 아는 한 Zod 사용자와 생태계 라이브러리 모두에게 깔끔하고 점진적인 마이그레이션 경로를 주는 방법은 이것뿐이다.

#### peer dependency로는 해결되지 않는 이유

Zod 스키마를 받는 `acceptSchema` 함수를 만드는 라이브러리를 가정한다. 이 함수는 Zod 3 스키마와 Zod 4 스키마를 모두 받아야 한다. 그리고 Zod 4가 서브패스 없이 npm에 `zod@4`로 배포되었다고 가정한다. 선택지는 다음과 같다.

1. npm 별칭을 써서 zod\@3과 zod\@4를 동시에 `dependencies`로 설치한다. npm 별칭은 같은 패키지의 다른 버전을 다른 이름으로 설치하는 기능으로, `package.json`에 `"zod3": "npm:zod@3"`처럼 적는다. 동작은 하지만 라이브러리가 Zod 3과 Zod 4의 사본을 각각 포함하게 된다. 사용자의 Zod 스키마가 라이브러리가 의존성으로 가져온 `z.ZodType` 클래스의 인스턴스라는 보장이 없으므로 `instanceof` 검사는 대개 실패한다.

2. 여러 메이저 버전에 걸친 peer dependency(`"zod@>=3.0.0"`)를 쓴다. 하지만 라이브러리를 개발할 때는 결국 기준 버전 하나를 골라 보통 dev dependency로 설치해야 한다. 코드가 두 버전에서 한 글자도 어긋남 없이 동작하는지는 라이브러리 작성자가 일일이 확인해야 한다. Zod 3과 Zod 4는 여러 핵심 클래스의 제네릭이 단순해지거나 달라졌기 때문에 이것이 불가능하다.

3. 선택적 peer dependency를 쓴다. 선택적 peer dependency는 사용자가 설치하지 않아도 경고나 설치 실패가 나지 않는 peer dependency로, 라이브러리는 런타임에 그 패키지가 있는지 확인해 쓴다. 문제는 런타임에 어떤 peer dependency가 설치되었는지 모든 플랫폼에서 안정적으로 판단하는 방법을 찾지 못했다는 점이다. 흔히 "try/catch 안에서 동적 import로 패키지 존재 여부를 확인하라"는 답이 있지만, 이는 백엔드 환경을 가정한 것이다. 프론트엔드 번들러에는 이를 위한 장치가 없어서 설치되지 않은 의존성을 번들링하려는 순간 실패한다. 빌드 단계에서는 try/catch 안에 있든 없든 상관이 없다. 게다가 같은 라이브러리의 여러 버전을 다루므로 `package.json`에서 두 버전을 구분하려면 npm 별칭이 필요한데, v10 같은 최근 npm도 peer dependency와 npm 별칭의 조합을 처리하지 못한다.

4. `zod-compat`을 쓴다. 온라인에서 보이는 이 모호한 해법은 "각 버전의 기본 기능을 나타내는 인터페이스를 정의하라"는 것이다. 라이브러리가 실제 구현을 흉내 내는 유틸리티 타입을 쓰는 셈이다. 오류가 생기기 쉽고, 작업량이 많고, 실제 구현과 계속 동기화해야 하며, 결국 라이브러리는 세부 사항이 빠진 그림자 버전을 대상으로 개발하게 된다. 또한 타입에만 통한다. 라이브러리가 Zod의 런타임 코드에 조금이라도 의존하면 무너진다.

그래서 서브패스를 택했다.

## 왜 Zod 4인가 (Release notes)

1년에 걸친 개발 끝에 Zod 4가 안정 버전이 되었다. 더 빠르고, 더 가볍고, `tsc`에 더 효율적이며, 오래 요청받았던 기능을 여럿 구현했다. 호환성이 깨지는 변경의 전체 목록은 아래 [마이그레이션 가이드](#마이그레이션-가이드-migration-guide)에서 다루고, 이 절은 새 기능과 개선점을 요약한다.

업그레이드 방법은 다음과 같다.

```
npm install zod@^4.0.0
```

### 새 메이저 버전이 필요했던 이유 (Why a new major version?)

Zod v3.0은 2021년 5월에 나왔다. 당시 Zod는 GitHub 스타 2,700개, 주간 다운로드 60만 회였다. 릴리스 노트 작성 시점에는 스타 37,800개, 주간 다운로드 3,100만 회로 늘었다(6주 전 베타가 나왔을 때는 2,300만 회였다). 24번의 마이너 버전을 거치며 Zod 3 코드베이스는 한계에 부딪혔다. 가장 많이 요청된 기능과 개선은 호환성을 깨야만 가능했다.

Zod 4는 Zod 3의 오래된 설계 한계를 한꺼번에 해결하고, 오래 요청받던 기능과 큰 폭의 성능 향상을 가능하게 한다. Zod에서 [추천 수가 가장 많은 열린 이슈 10개](https://github.com/colinhacks/zod/issues?q=is%3Aissue%20state%3Aopen%20sort%3Areactions-%2B1-desc) 중 9개를 해결한다. 원저자는 Zod 4가 앞으로 오랫동안 새 기반이 되기를 기대한다.

### 벤치마크 (Benchmarks)

벤치마크는 Zod 저장소에서 직접 돌려 볼 수 있다.

```sh
$ git clone git@github.com:colinhacks/zod.git
$ cd zod
$ git switch v4
$ nub install
```

특정 벤치마크를 실행하려면 다음과 같이 한다.

```sh
$ nub run bench <name>
```

#### 문자열 파싱 14배 향상 (14x faster string parsing)

```sh
$ nub run bench string
runtime: node v22.13.0 (arm64-darwin)

benchmark      time (avg)             (min … max)       p75       p99      p999
------------------------------------------------- -----------------------------
• z.string().parse
------------------------------------------------- -----------------------------
zod3          363 µs/iter       (338 µs … 683 µs)    351 µs    467 µs    572 µs
zod4       24'674 ns/iter    (21'083 ns … 235 µs) 24'209 ns 76'125 ns    120 µs

summary for z.string().parse
  zod4
   14.71x faster than zod3
```

#### 배열 파싱 7배 향상 (7x faster array parsing)

```sh
$ nub run bench array
runtime: node v22.13.0 (arm64-darwin)

benchmark      time (avg)             (min … max)       p75       p99      p999
------------------------------------------------- -----------------------------
• z.array() parsing
------------------------------------------------- -----------------------------
zod3          147 µs/iter       (137 µs … 767 µs)    140 µs    246 µs    520 µs
zod4       19'817 ns/iter    (18'125 ns … 436 µs) 19'125 ns 44'500 ns    137 µs

summary for z.array() parsing
  zod4
   7.43x faster than zod3
```

#### 객체 파싱 6.5배 향상 (6.5x faster object parsing)

[Moltar 검증 라이브러리 벤치마크](https://moltar.github.io/typescript-runtime-type-benchmarks/)를 실행한 결과다.

```sh
$ nub run bench object-moltar
benchmark      time (avg)             (min … max)       p75       p99      p999
------------------------------------------------- -----------------------------
• z.object() safeParse
------------------------------------------------- -----------------------------
zod3          805 µs/iter     (771 µs … 2'802 µs)    804 µs    928 µs  2'802 µs
zod4          124 µs/iter     (118 µs … 1'236 µs)    119 µs    231 µs    329 µs

summary for z.object() safeParse
  zod4
   6.5x faster than zod3
```

### `tsc` 타입 인스턴스화 100배 감소 (100x reduction in `tsc` instantiations)

타입 인스턴스화(type instantiation)는 TypeScript 컴파일러가 `ZodObject<...>` 같은 제네릭 타입에 실제 타입 인자를 채워 구체적인 타입을 만들어 내는 작업이다. Zod 스키마는 `.extend()`처럼 스키마의 모양을 바꾸는 메서드를 호출할 때마다 새 제네릭 타입을 만들어 내므로, 인스턴스화 횟수가 많을수록 `tsc` 컴파일과 에디터의 자동 완성, 타입 표시가 느려진다. 다음과 같은 간단한 파일로 차이를 확인해 보자.

```ts
import * as z from "zod";

export const A = z.object({
  a: z.string(),
  b: z.string(),
  c: z.string(),
  d: z.string(),
  e: z.string(),
});

export const B = A.extend({
  f: z.string(),
  g: z.string(),
  h: z.string(),
});
```

이 파일을 `tsc --extendedDiagnostics`로 컴파일하면 `"zod/v3"`에서는 타입 인스턴스화가 25,000회를 넘는다. `"zod/v4"`에서는 약 175회에 그친다.

> **참고:** Zod 저장소의 `packages/tsc`에는 `tsc` 벤치마크 환경이 있어 직접 확인해 볼 수 있다. 정확한 수치는 구현이 바뀌면서 달라질 수 있다.
>
> ```sh
> $ cd packages/tsc
> $ nub run bench object-with-extend
> ```

횟수 자체보다 더 중요한 점은 Zod 4가 `ZodObject`를 비롯한 스키마 클래스의 제네릭을 다시 설계하고 단순화해 골치 아픈 "인스턴스화 폭발(instantiation explosion)"을 피한다는 것이다. 인스턴스화 폭발은 타입을 한 단계 쌓을 때마다 컴파일러가 처리할 인스턴스화가 급격히 불어나, 결국 컴파일이 극도로 느려지거나 실패하는 현상이다. 예전에는 아래처럼 `.extend()`와 `.omit()`을 반복해 이어 붙이면 컴파일러 문제가 생겼다.

```ts
import * as z from "zod";

export const a = z.object({
  a: z.string(),
  b: z.string(),
  c: z.string(),
});

export const b = a.omit({
  a: true,
  b: true,
  c: true,
});

export const c = b.extend({
  a: z.string(),
  b: z.string(),
  c: z.string(),
});

export const d = c.omit({
  a: true,
  b: true,
  c: true,
});

export const e = d.extend({
  a: z.string(),
  b: z.string(),
  c: z.string(),
});

export const f = e.omit({
  a: true,
  b: true,
  c: true,
});

export const g = f.extend({
  a: z.string(),
  b: z.string(),
  c: z.string(),
});

export const h = g.omit({
  a: true,
  b: true,
  c: true,
});

export const i = h.extend({
  a: z.string(),
  b: z.string(),
  c: z.string(),
});

export const j = i.omit({
  a: true,
  b: true,
  c: true,
});

export const k = j.extend({
  a: z.string(),
  b: z.string(),
  c: z.string(),
});

export const l = k.omit({
  a: true,
  b: true,
  c: true,
});

export const m = l.extend({
  a: z.string(),
  b: z.string(),
  c: z.string(),
});

export const n = m.omit({
  a: true,
  b: true,
  c: true,
});

export const o = n.extend({
  a: z.string(),
  b: z.string(),
  c: z.string(),
});

export const p = o.omit({
  a: true,
  b: true,
  c: true,
});

export const q = p.extend({
  a: z.string(),
  b: z.string(),
  c: z.string(),
});
```

Zod 3에서는 이 코드를 컴파일하는 데 `4000ms`가 걸렸고, `.extend()` 호출을 더 추가하면 "Possibly infinite" 에러가 났다. Zod 4에서는 `400ms`로 `10x` 빨라졌다.

> 곧 나올 [`tsgo`](https://github.com/microsoft/typescript-go) 컴파일러와 함께라면, Zod 4의 에디터 성능은 훨씬 큰 스키마와 코드베이스에서도 유지될 것이다.

### 코어 번들 크기 2배 감소 (2x reduction in core bundle size)

다음 스크립트를 생각해 보자.

```ts
import * as z from "zod";

const schema = z.boolean();

schema.parse(true);
```

이 검증 코드는 최소한으로 구성했다. 가장 단순한 스키마를 사용해도 번들에 포함되는 코드, 즉 코어 번들 크기(core bundle size)를 측정하기 위해서다. 이 코드를 Zod 3과 Zod 4로 각각 `rollup` 번들링해 최종 번들을 비교한 결과는 다음과 같다.

- Zod 3: 번들 (gzip): `12.47kb`
- Zod 4: 번들 (gzip): `5.36kb`

Zod 4의 코어 번들은 약 57% 작다(2.3배).

### Zod Mini 소개 (Introducing Zod Mini)

코어 번들을 절반 넘게 줄였어도 남는 한계가 있다. 메서드 중심 API는 본질적으로 트리 셰이킹(tree-shaking)이 어렵기 때문이다. 트리 셰이킹은 번들러가 최종 번들에서 쓰지 않는 코드를 빼는 최적화인데, 번들러는 쓰지 않는 최상위 함수는 제거해도 쓰지 않는 메서드 구현은 제거하지 못한다(자세한 내용은 [「트리 셰이킹」](07_packages_and_compile.md#트리-셰이킹-tree-shaking) 참고). 그래서 위의 단순한 `z.boolean()` 스크립트도 쓰지 않은 `.optional()`, `.array()` 같은 메서드 구현까지 끌어온다. 구현을 가볍게 만드는 것만으로는 이 문제를 풀 수 없어서, 메서드 대신 래퍼 함수를 쓰는 함수형 API인 Zod Mini(`zod/mini`)를 도입했다. 같은 `z.boolean()` 스크립트를 `"zod/mini"`로 바꿔 `rollup`으로 빌드하면 gzip 번들 크기가 `1.88kb`로, `zod@3` 대비 85%(6.6배) 줄어든다.

```ts
import * as z from "zod/mini";

const schema = z.boolean();
schema.parse(false);
```

- Zod 3: 번들 (gzip): `12.47kb`
- Zod 4 (regular): 번들 (gzip): `5.36kb`
- Zod 4 (mini): 번들 (gzip): `1.88kb`

대부분의 경우에는 일반 Zod를 권장하고, 번들 크기 제약이 유난히 엄격한 프로젝트라면 Zod Mini를 고려한다. Zod Mini의 API와 `.check()`, 최상위 검사 함수 목록은 [「Zod Mini」](07_packages_and_compile.md#zod-mini)를 참고한다.

### 새 기능 개요

릴리스 노트가 소개한 나머지 기능은 각 주제를 담당하는 문서에서 자세히 설명한다. 여기서는 Zod 3 대비 달라진 점만 적는다.

- **메타데이터(Metadata)**: 스키마 자체가 아니라 스키마 레지스트리(schema registry)에 강타입 메타데이터를 저장하는 체계를 새로 도입했다. `z.registry()`, `z.globalRegistry`, `.meta()`를 제공하며, Zod 3 호환을 위해 `.describe()`도 남아 있지만 `.meta()`를 권장한다. 자세한 내용은 [「메타데이터」](06_metadata_and_json_schema.md#메타데이터-metadata)를 참고한다.
- **JSON Schema 변환(JSON Schema conversion)**: `z.toJSONSchema()`로 공식 JSON Schema 변환을 지원하며, `z.globalRegistry`의 메타데이터가 결과에 자동으로 포함된다. 자세한 내용은 [「`z.toJSONSchema()`」](06_metadata_and_json_schema.md#ztojsonschema)를 참고한다.
- **재귀 객체(Recursive objects)**: 원저자가 수년간 풀지 못했던 재귀 객체 타입 추론을 getter 문법으로 해결했다. Zod 3의 재귀 타입 패턴과 달리 타입 캐스팅이 필요 없고, 결과가 일반 `ZodObject`여서 `.pick()`, `.partial()`, `.extend()` 등을 그대로 쓸 수 있다. 자세한 내용은 [「재귀 객체」](03_schemas_objects_collections.md#재귀-객체-recursive-objects)를 참고한다.
- **파일 스키마(File schemas)**: `z.file()`로 `File` 인스턴스의 크기와 MIME 타입을 검증한다. 자세한 내용은 [「파일」](03_schemas_objects_collections.md#파일-files)을 참고한다.
- **국제화(Internationalization)**: `zod/locales`와 `z.config()`로 에러 메시지를 전역에서 다른 언어로 바꾸는 `locales` API를 새로 도입했다. 자세한 내용은 [「국제화」](05_errors.md#국제화-internationalization)를 참고한다.
- **에러 보기 좋게 출력(Error pretty-printing)**: [`zod-validation-error`](https://www.npmjs.com/package/zod-validation-error) 패키지의 인기가 보여 주듯 공식 API에 대한 수요가 컸고, 이에 `z.prettifyError()`를 추가했다. 이미 그 패키지를 쓰고 있다면 계속 써도 된다. 출력 형식은 현재 설정할 수 없지만 바뀔 수 있다. 자세한 내용은 [「`z.prettifyError()`」](05_errors.md#zprettifyerror)를 참고한다.
- **최상위 문자열 포맷(Top-level string formats)**: 모든 문자열 포맷을 `z` 모듈의 최상위 함수(`z.email()`, `z.uuidv4()`, `z.jwt()`, `z.iso.datetime()` 등)로 올렸다. 더 간결하고 트리 셰이킹에도 유리하다. `z.string().email()` 같은 메서드 형태는 아직 쓸 수 있지만 지원 중단(deprecated)되었고, 다음 메이저 버전에서 제거된다. `z.email()`은 이제 커스텀 정규식(`pattern`)도 받는다. 자세한 내용은 [「문자열 포맷」](02_schemas_primitives.md#문자열-포맷-string-formats)을 참고한다.
- **템플릿 리터럴 타입(Template literal types)**: TypeScript 타입 시스템에서 그동안 표현할 수 없었던 가장 큰 기능인 템플릿 리터럴 타입을 `z.templateLiteral()`로 구현했다. 자세한 내용은 [「템플릿 리터럴」](02_schemas_primitives.md#템플릿-리터럴-template-literals)을 참고한다.
- **숫자 포맷(Number formats)**: 고정 폭 정수와 부동소수 타입을 나타내는 `z.int()`, `z.float32()`, `z.float64()`, `z.int32()`, `z.uint32()`와 `bigint`용 `z.int64()`, `z.uint64()`를 추가했다. 자세한 내용은 [「정수」](02_schemas_primitives.md#정수-integers)를 참고한다.
- **Stringbool**: `z.coerce.boolean()`은 falsy 값을 `false`, truthy 값을 `true`로 바꾸는 단순한 API이고 앞으로도 유효하다. 다만 환경 변수 스타일의 불리언 변환을 원하는 요청이 있어 `z.stringbool()`을 새로 도입했다. 자세한 내용은 [「문자열 불리언」](02_schemas_primitives.md#문자열-불리언-stringbools)을 참고한다.
- **에러 커스터마이징 단순화(Simplified error customization)**: Zod 4의 호환성 깨짐 대부분은 에러 커스터마이징 API에 몰려 있다. Zod 3에서 흩어져 있던 API를 하나의 `error` 파라미터로 통일했으며, 자세한 전환 방법은 아래 [에러 커스터마이징](#에러-커스터마이징-error-customization)에서 다룬다.
- **개선된 `z.discriminatedUnion()`(Upgraded `z.discriminatedUnion()`)**: 판별자로 유니언과 파이프 등 예전에 지원하지 않던 스키마 타입을 쓸 수 있고, 판별 유니언을 다른 판별 유니언의 멤버로 중첩할 수 있다. 자세한 내용은 [「판별 유니언」](03_schemas_objects_collections.md#판별-유니언-discriminated-unions)을 참고한다.
- **`z.literal()`의 여러 값(Multiple values in `z.literal()`)**: `z.literal([200, 201, ...])`처럼 여러 값을 받을 수 있어, Zod 3에서 `z.union()`에 `z.literal()`을 나열하던 코드를 대체한다. 자세한 내용은 [「리터럴」](02_schemas_primitives.md#리터럴-literals)을 참고한다.
- **스키마 안에 저장되는 정제(Refinements live inside schemas)**: Zod 3에서는 정제(refinement)가 원래 스키마를 감싸는 `ZodEffects`에 저장되어 `.refine()` 뒤에 `.min()` 같은 메서드를 이어 쓸 수 없었다(`Property 'min' does not exist on type ZodEffects<...>`). Zod 4에서는 정제가 스키마 안에 저장되므로 자유롭게 섞어 쓸 수 있다. 내부 구조가 어떻게 바뀌었는지는 아래 [`ZodEffects` 제거](#zodeffects-제거-drops-zodeffects)에서 다룬다. 자세한 사용법은 [「정제」](04_refinements_transforms_codecs.md#정제-refinements)를 참고한다.
- **`.overwrite()`**: `.transform()`은 출력 타입을 런타임에 들여다볼 수 없어 JSON Schema로 안전하게 변환할 방법이 없다. 추론 타입을 바꾸지 않는 변환을 표현하는 `.overwrite()`를 새로 도입했고, 기존 `.trim()`, `.toLowerCase()`, `.toUpperCase()`도 이것으로 다시 구현했다. 자세한 내용은 [「Zod 스키마 정의 3: 정제, 변환, 코덱」](04_refinements_transforms_codecs.md)을 참고한다.
- **확장 가능한 기반 `zod/v4/core`(An extensible foundation)**: Zod Mini를 추가하면서 Zod와 Zod Mini가 공유하는 핵심 기능을 담은 하위 패키지 `zod/v4/core`가 생겼다. 원저자는 처음에는 이를 꺼렸지만, 이제는 Zod를 단순한 라이브러리에서 다른 라이브러리에 녹여 쓸 수 있는 빠른 검증 기반(substrate)으로 끌어올린 Zod 4의 가장 중요한 기능 중 하나로 본다. 스키마 라이브러리를 만든다면 Zod와 Zod Mini의 구현을 참고하고, 도움이나 피드백이 필요하면 GitHub discussions나 [X](https://x.com/colinhacks)/[Bluesky](https://bsky.app/profile/colinhacks.com)로 연락하면 된다. 자세한 내용은 [「Zod Core」](07_packages_and_compile.md#zod-core)를 참고한다.

### 마무리 (Wrapping up)

원저자는 Zod Mini 같은 주요 기능의 설계 과정을 설명하는 글을 이어서 쓸 계획이다.

라이브러리 작성자를 위한 별도 가이드도 있다. Zod 위에 무언가를 만들 때의 모범 사례와, Zod 3과 Zod 4(Mini 포함)를 동시에 지원하는 방법에 대한 흔한 질문을 다룬다. 자세한 내용은 [「라이브러리 작성자 가이드」](07_packages_and_compile.md#라이브러리-작성자-가이드-for-library-authors)를 참고한다.

```sh
pnpm upgrade zod@latest
```

## 마이그레이션 가이드 (Migration guide)

이 가이드는 Zod 4의 호환성이 깨지는 변경을 영향이 큰 순서대로 정리한다. 성능 개선과 새 기능은 위의 [왜 Zod 4인가](#왜-zod-4인가-release-notes)를 참고한다.

```
npm install zod@^4.0.0
```

Zod의 많은 동작과 API가 더 직관적이고 일관되게 바뀌었다. 여기서 설명하는 호환성 깨짐은 대개 사용성을 크게 개선하는 변화이므로, 원저자는 이 가이드를 꼼꼼히 읽기를 강력히 권한다.

> **참고:** Zod 3은 공개 API로 간주하지 않는, 문서화되지 않은 준내부(quasi-internal) 유틸리티 타입과 함수를 여럿 내보냈다. 이들의 변경은 여기서 다루지 않는다.

> **참고:** 커뮤니티가 관리하는 비공식 codemod [`zod-v3-to-v4`](https://github.com/nicoespeon/zod-v3-to-v4)가 있다.

### 에러 커스터마이징 (Error customization)

Zod 4는 흩어져 있고 일관성이 없던 예전 에러 커스터마이징 API를 하나의 `error` 파라미터로 표준화했다. `error` 파라미터 자체의 사용법은 [「`error` 파라미터」](05_errors.md#error-파라미터-the-error-param)를 참고한다.

#### `message` 파라미터 지원 중단 (deprecates `message` parameter)

`message` 파라미터를 `error`로 대체한다. 기존 `message` 파라미터도 여전히 동작하지만 지원 중단되었다.

```ts
// Zod 4
z.string().min(5, { error: "Too short." });
```

```ts
// Zod 3
z.string().min(5, { message: "Too short." });
```

#### `invalid_type_error`와 `required_error` 제거 (drops `invalid_type_error` and `required_error`)

`invalid_type_error`와 `required_error` 파라미터가 제거되었다. 이들은 수년 전 `errorMap`보다 덜 장황하게 에러를 커스터마이징하려고 급히 추가한 파라미터였다. `errorMap`은 실패한 이슈를 받아 메시지를 돌려주는 함수로, 바로 [다음 절](#errormap-제거-drops-errormap)에서 다룬다. `errorMap`과 함께 쓸 수 없는 등 함정이 많았고, Zod의 실제 이슈 코드와도 맞지 않았다(`required`라는 이슈 코드는 없다).

Zod 4에서는 두 경우를 `error` 함수 하나에서 나눈다. 값이 아예 없어 실패했다면 이슈의 `input`이 `undefined`이므로, 이를 보고 "필수 값 누락"과 "타입 불일치"를 구분하면 된다.

```ts
// Zod 4
z.string({ 
  error: (issue) => issue.input === undefined 
    ? "This field is required" 
    : "Not a string" 
});
```

```ts
// Zod 3
z.string({ 
  required_error: "This field is required",
  invalid_type_error: "Not a string", 
});
```

#### `errorMap` 제거 (drops `errorMap`)

에러 맵(error map)은 이슈 객체를 받아 그 이슈에 쓸 에러 메시지를 돌려주는 함수다. Zod 3에서 이 함수를 넘기던 `errorMap` 파라미터는 Zod 4에서 `error`로 이름이 바뀌었다.

반환 형식도 단순해졌다. 에러 맵은 이제 `{message: string}` 대신 일반 `string`을 반환할 수 있다. `undefined`를 반환하면 이 에러 맵은 메시지를 정하지 않고, 우선순위상 다음 에러 맵에 결정을 넘긴다. 그래서 Zod 3처럼 마지막에 `ctx.defaultError`를 돌려줄 필요가 없다. 에러 맵 사이의 우선순위는 [「에러 우선순위」](05_errors.md#에러-우선순위-error-precedence)를 참고한다.

```ts
// Zod 4
z.string().min(5, {
  error: (issue) => {
    if (issue.code === "too_small") {
      return `Value must be >${issue.minimum}`
    }
  },
});
```

```ts
// Zod 3
z.string({
  errorMap: (issue, ctx) => {
    if (issue.code === "too_small") {
      return { message: `Value must be >${issue.minimum}` };
    }
    return { message: ctx.defaultError };
  },
});
```

### `ZodError`

`ZodError`와 이슈 구조 자체의 설명은 [「ZodError와 이슈」](05_errors.md#zoderror와-이슈-zoderror-and-issues)를 참고한다.

#### 이슈 형식 변경 (updates issue formats)

이슈 형식이 크게 간소화되었다.

```ts
import * as z from "zod"; // v4

type IssueFormats = 
  | z.core.$ZodIssueInvalidType
  | z.core.$ZodIssueTooBig
  | z.core.$ZodIssueTooSmall
  | z.core.$ZodIssueInvalidStringFormat
  | z.core.$ZodIssueNotMultipleOf
  | z.core.$ZodIssueUnrecognizedKeys
  | z.core.$ZodIssueInvalidValue
  | z.core.$ZodIssueInvalidUnion
  | z.core.$ZodIssueInvalidKey // 신규: z.record/z.map에서 사용
  | z.core.$ZodIssueInvalidElement // 신규: z.map/z.set에서 사용
  | z.core.$ZodIssueCustom;
```

다음은 Zod 3의 이슈 타입과 그에 대응하는 Zod 4 타입이다.

```ts
import * as z from "zod"; // v3

export type IssueFormats =
  | z.ZodInvalidTypeIssue // ♻️ z.core.$ZodIssueInvalidType으로 이름 변경
  | z.ZodTooBigIssue  // ♻️ z.core.$ZodIssueTooBig으로 이름 변경
  | z.ZodTooSmallIssue // ♻️ z.core.$ZodIssueTooSmall로 이름 변경
  | z.ZodInvalidStringIssue // ♻️ z.core.$ZodIssueInvalidStringFormat
  | z.ZodNotMultipleOfIssue // ♻️ z.core.$ZodIssueNotMultipleOf로 이름 변경
  | z.ZodUnrecognizedKeysIssue // ♻️ z.core.$ZodIssueUnrecognizedKeys로 이름 변경
  | z.ZodInvalidUnionIssue // ♻️ z.core.$ZodIssueInvalidUnion으로 이름 변경
  | z.ZodCustomIssue // ♻️ z.core.$ZodIssueCustom으로 이름 변경
  | z.ZodInvalidEnumValueIssue // ❌ z.core.$ZodIssueInvalidValue로 통합
  | z.ZodInvalidLiteralIssue // ❌ z.core.$ZodIssueInvalidValue로 통합
  | z.ZodInvalidUnionDiscriminatorIssue // ❌ 스키마 생성 시점에 Error를 던짐
  | z.ZodInvalidArgumentsIssue // ❌ z.function이 ZodError를 직접 던짐
  | z.ZodInvalidReturnTypeIssue // ❌ z.function이 ZodError를 직접 던짐
  | z.ZodInvalidDateIssue // ❌ invalid_type으로 통합
  | z.ZodInvalidIntersectionTypesIssue // ❌ 제거됨 (일반 Error를 던짐)
  | z.ZodNotFiniteIssue // ❌ 무한대 값을 더 이상 허용하지 않음 (invalid_type)
```

일부 이슈 타입이 통합, 제거, 수정되었지만 각 이슈의 구조는 Zod 3의 대응 타입과 비슷하다(대부분은 같다). 모든 이슈가 Zod 3과 같은 기본 인터페이스를 따르므로 흔한 에러 처리 로직은 대부분 수정 없이 동작한다.

```ts
export interface $ZodIssueBase {
  readonly code?: string;
  readonly input?: unknown;
  readonly path: PropertyKey[];
  readonly message: string;
}
```

#### 에러 맵 우선순위 변경 (changes error map precedence)

에러 맵을 여러 곳에 정의했을 때 어느 것이 이기는지, 즉 에러 맵 우선순위가 더 일관되게 바뀌었다. 특히 `.parse()`에 넘긴 에러 맵이 스키마를 정의할 때 넣은 스키마 수준 에러 맵보다 *더 이상* 우선하지 않는다. 호출하는 쪽에서 `.parse()`에 에러 맵을 넘겨 메시지를 덮어쓰던 코드라면, 스키마에 에러 맵이 있는 경우 결과 메시지가 달라진다. 전체 우선순위는 [「에러 우선순위」](05_errors.md#에러-우선순위-error-precedence)를 참고한다.

```ts
const mySchema = z.string({ error: () => "Schema-level error" });

// Zod 3
mySchema.parse(12, { error: () => "Contextual error" }); // => "Contextual error"

// Zod 4
mySchema.parse(12, { error: () => "Contextual error" }); // => "Schema-level error"
```

#### `.format()` 지원 중단 (deprecates `.format()`)

`ZodError`의 `.format()` 메서드는 지원 중단되었다. 대신 최상위 함수 `z.treeifyError()`를 쓴다. 자세한 내용은 [「`z.treeifyError()`」](05_errors.md#ztreeifyerror)를 참고한다.

#### `.flatten()` 지원 중단 (deprecates `.flatten()`)

`ZodError`의 `.flatten()` 메서드도 지원 중단되었다. 대신 최상위 함수 `z.treeifyError()`를 쓴다. 자세한 내용은 [「`z.treeifyError()`」](05_errors.md#ztreeifyerror)를 참고한다.

#### `.formErrors` 제거 (drops `.formErrors`)

`.formErrors`는 `.flatten()`과 똑같은 API였다. 역사적인 이유로 남아 있었을 뿐 문서화되지 않았다.

#### `.errors` 제거 (drops `.errors`)

`.errors`는 Zod v3에서 `.issues`의 별칭이었고, Zod 4에서 제거되었다. 대신 `.issues`를 쓴다.

#### `.addIssue()`와 `.addIssues()` 지원 중단 (deprecates `.addIssue()` and `.addIssues()`)

`ZodError`에 이슈를 추가해야 한다면 이 메서드 대신 `err.issues` 배열에 직접 push한다.

```ts
myError.issues.push({ 
  // 새 이슈
});
```

### `z.number()`

#### 무한대 값 불허 (no infinite values)

`POSITIVE_INFINITY`와 `NEGATIVE_INFINITY`는 더 이상 `z.number()`의 유효한 값이 아니다.

#### `.safe()`가 부동소수를 거부 (`.safe()` no longer accepts floats)

`z.number().safe()`는 지원 중단되었다. 이제 `.int()`와 똑같이 동작하므로(아래 참고) 부동소수를 더 이상 허용하지 않는다.

#### `.int()`는 안전한 정수만 허용 (`.int()` accepts safe integers only)

`z.number().int()`는 더 이상 안전하지 않은 정수(`Number.MIN_SAFE_INTEGER`~`Number.MAX_SAFE_INTEGER` 범위 밖)를 허용하지 않는다. 이 범위를 벗어난 정수는 예기치 않은 반올림 오류를 일으킨다. (참고로 `z.int()`로 바꾸는 편이 좋다.)

### `z.string()` 변경 (`z.string()` updates)

#### `.email()` 등 지원 중단 (deprecates `.email()` etc)

문자열 포맷은 이제 단순한 내부 정제가 아니라 `ZodString`의 *하위 클래스*로 표현한다. 그래서 이 API들을 최상위 `z` 네임스페이스로 옮겼다. 최상위 API는 덜 장황하고 트리 셰이킹에도 유리하다.

```ts
z.email();
z.uuid();
z.url();
z.emoji();         // 이모지 한 글자를 검증
z.base64();
z.base64url();
z.nanoid();
z.cuid();
z.cuid2();
z.ulid();
z.ipv4();
z.ipv6();
z.cidrv4();          // IP 범위
z.cidrv6();          // IP 범위
z.iso.date();
z.iso.time();
z.iso.datetime();
z.iso.duration();
```

메서드 형태(`z.string().email()`)는 여전히 존재하고 예전처럼 동작하지만 지원 중단되었다.

```ts
z.string().email(); // ❌ 지원 중단
z.email(); // ✅ 
```

각 포맷의 옵션은 [「문자열 포맷」](02_schemas_primitives.md#문자열-포맷-string-formats)을 참고한다.

#### 더 엄격해진 `.uuid()` (stricter `.uuid()`)

`z.uuid()`는 이제 RFC 9562/4122 명세에 따라 UUID를 더 엄격하게 검증한다. 특히 명세대로 variant 비트가 `10`이어야 한다. variant 비트는 UUID가 어떤 레이아웃 규격을 따르는지 나타내는 비트로, 문자열로 보면 네 번째 묶음의 첫 글자가 `8`, `9`, `a`, `b` 중 하나여야 한다는 뜻이다. 그래서 형태만 UUID처럼 생긴 임의의 16진수 문자열은 Zod 3에서 통과했더라도 Zod 4에서는 실패할 수 있다. 더 느슨한 "UUID 비슷한" 검증이 필요하면 `z.guid()`를 쓴다.

```ts
z.uuid(); // RFC 9562/4122를 따르는 UUID
z.guid(); // 8-4-4-4-12 형태의 16진수 패턴이면 모두 허용
```

#### `.base64url()`의 패딩 불허 (no padding in `.base64url()`)

`z.base64url()`(이전의 `z.string().base64url()`)은 이제 패딩을 허용하지 않는다. 패딩은 base64 인코딩 결과의 길이를 4의 배수로 맞추려고 끝에 붙이는 `=` 문자다. 일반적으로 base64url 문자열은 이 패딩이 없는 URL 안전 형태여야 하므로, 끝에 `=`가 붙은 값은 이제 실패한다.

#### `z.string().ip()` 제거 (drops `z.string().ip()`)

별도의 `.ipv4()`와 `.ipv6()` 메서드로 대체되었다. 둘 다 허용해야 하면 `z.union()`으로 합친다.

```ts
z.string().ip() // ❌
z.ipv4() // ✅
z.ipv6() // ✅
```

#### `z.string().ipv6()` 변경 (updates `z.string().ipv6()`)

이제 예전의 정규식 방식보다 훨씬 견고한 `new URL()` 생성자로 검증한다. 예전에는 통과하던 일부 잘못된 값이 이제는 실패할 수 있다.

#### `z.string().cidr()` 제거 (drops `z.string().cidr()`)

마찬가지로 별도의 `.cidrv4()`와 `.cidrv6()` 메서드로 대체되었다. 둘 다 허용해야 하면 `z.union()`으로 합친다.

```ts
z.string().cidr() // ❌
z.cidrv4() // ✅
z.cidrv6() // ✅
```

### `z.coerce` 변경 (`z.coerce` updates)

`z.coerce`는 입력을 `String(input)`, `Number(input)`처럼 내장 생성자로 강제 변환(coercion)한 뒤 검증하는 스키마다(자세한 내용은 [「강제 변환」](02_schemas_primitives.md#강제-변환-coercion) 참고). Zod 4에서는 모든 `z.coerce` 스키마의 입력 타입이 `unknown`이다. 입력 타입은 `z.input<typeof schema>`로 꺼낼 수 있으며, 이 타입에 기대던 코드라면 추론 결과가 달라진다.

```ts
const schema = z.coerce.string();
type schemaInput = z.input<typeof schema>;

// Zod 3: string;
// Zod 4: unknown;
```

런타임 동작도 바뀌었다. Zod 3은 객체 스키마에서 `z.coerce.*` 필드의 키가 없으면 `undefined`를 강제 변환한 값을 채웠지만, Zod 4에서는 에러가 난다. 키가 없을 때 쓸 값이 필요하면 `.default()`로 명시한다.

```ts
const schema = z.object({ foo: z.coerce.boolean() });
schema.parse({});

// Zod 3: { foo: false }
// Zod 4: ZodError: Invalid input: expected nonoptional, received undefined
```

### `.default()` 변경 (`.default()` updates)

`.default()`의 적용 방식이 미묘하게 바뀌었다. 입력이 `undefined`이면 `ZodDefault`(`.default()`가 만드는 래퍼 스키마)는 안쪽 스키마의 파싱을 건너뛰고(short-circuit) 기본값을 바로 반환한다. 기본값이 변환을 거치지 않고 그대로 결과가 되므로, 기본값은 스키마의 *출력 타입*에 할당 가능해야 한다. 아래 예제에서 `.transform()`은 문자열을 길이(`number`)로 바꾸므로 출력 타입은 `number`이고, 기본값도 `number`여야 한다. 입력 타입과 출력 타입의 구분은 [「`z.input()`과 `z.output()`」](04_refinements_transforms_codecs.md#zinput과-zoutput-zinput-and-zoutput)을 참고한다.

```ts
const schema = z.string()
  .transform(val => val.length)
  .default(0); // number여야 함
schema.parse(undefined); // => 0
```

Zod 3의 `.default()`는 반대로 *입력 타입*에 맞는 값을 기대했다. `ZodDefault`가 파싱을 건너뛰지 않고 기본값을 안쪽 스키마로 파싱했기 때문에, 기본값은 스키마의 *입력 타입*에 할당 가능해야 했다. 아래 예제에서 `"tuna"`가 변환을 거쳐 `4`가 되는 것도 그 때문이다.

```ts
// Zod 3
const schema = z.string()
  .transform(val => val.length)
  .default("tuna");
schema.parse(undefined); // => 4
```

예전 동작을 재현하려면 새 `.prefault()` API를 쓴다. 이름은 "pre-parse default(파싱 전 기본값)"의 줄임말로, 이름 그대로 입력이 `undefined`일 때 기본값을 입력 자리에 넣고 안쪽 스키마로 파싱한다. 기존 코드에서 `.default()`에 입력 타입의 값을 넘기고 있었다면 `.prefault()`로 바꾸면 된다.

```ts
// Zod 3
const schema = z.string()
  .transform(val => val.length)
  .prefault("tuna");
schema.parse(undefined); // => 4
```

> **참고:** 원문은 위 `.prefault()` 예제에도 `// Zod 3` 주석을 달았지만, `.prefault()`는 Zod 4의 API이므로 실제로는 Zod 4에서 Zod 3의 동작을 재현하는 코드다.

`.default()`와 `.prefault()`의 자세한 사용법은 [「기본값」](04_refinements_transforms_codecs.md#기본값-defaults)과 [「사전 기본값」](04_refinements_transforms_codecs.md#사전-기본값-prefaults)을 참고한다.

### `z.object()`

#### optional 필드 안의 기본값 적용 (defaults applied within optional fields)

Zod 4에서는 속성에 지정한 기본값이 `.optional()`로 감싼 필드 안에 있어도 적용된다. 아래 예제에서 Zod 3은 키 `a`가 없는 입력을 그대로 두었지만, Zod 4는 기본값 `"tuna"`를 채운다. 이 편이 기대에 더 부합하고 Zod 3의 오래된 사용성 문제도 해결한다. 다만 미묘한 변경이라, 파싱 결과에 키가 있는지 없는지로 분기하던 코드는 깨질 수 있다.

```ts
const schema = z.object({
  a: z.string().default("tuna").optional(),
});

schema.parse({});
// Zod 4: { a: "tuna" }
// Zod 3: {}
```

#### `.strict()`와 `.passthrough()` 지원 중단 (deprecates `.strict()` and `.passthrough()`)

`z.object()`는 기본적으로 스키마에 없는 키를 결과에서 제거(strip)한다. Zod 3에서는 알 수 없는 키가 있으면 에러를 내려면 `.strict()`를, 그대로 통과시키려면 `.passthrough()`를 붙였다. Zod 4에서는 이 메서드들이 대체로 더 이상 필요 없다. 대신 같은 동작을 하는 최상위 함수 `z.strictObject()`와 `z.looseObject()`를 쓴다(자세한 내용은 [「`z.strictObject`」](03_schemas_objects_collections.md#zstrictobject), [「`z.looseObject`」](03_schemas_objects_collections.md#zlooseobject) 참고).

```ts
// Zod 3
z.object({ name: z.string() }).strict();
z.object({ name: z.string() }).passthrough();

// Zod 4
z.strictObject({ name: z.string() });
z.looseObject({ name: z.string() });
```

> 이 메서드들은 하위 호환을 위해 계속 제공되며 제거되지 않는다. 레거시로 간주한다.

#### `.strip()` 지원 중단 (deprecates `.strip()`)

`.strip()`은 알 수 없는 키를 제거하는 메서드인데, 이는 `z.object()`의 기본 동작이어서 그다지 쓸모가 없었다. strict 객체를 "일반" 객체로 바꾸려면 `z.object(A.shape)`를 쓴다.

#### `.nonstrict()` 제거 (drops `.nonstrict()`)

오래전에 지원 중단된 `.strip()`의 별칭이 제거되었다.

#### `.deepPartial()` 제거 (drops `.deepPartial()`)

Zod 3에서 오래전에 지원 중단되었고 Zod 4에서 제거되었다. 직접적인 대안은 없다. 구현에 함정이 많았고, 이를 쓰는 것 자체가 대개 안티패턴이다.

#### `z.unknown()`의 optional 여부 변경 (changes `z.unknown()` optionality)

추론 타입에서 `z.unknown()`과 `z.any()`가 더 이상 "키 optional"로 표시되지 않는다.

```ts
const mySchema = z.object({
  a: z.any(),
  b: z.unknown()
});
// Zod 3: { a?: any; b?: unknown };
// Zod 4: { a: any; b: unknown };
```

`v4.4.0`부터는 파싱 시점에도 키가 필수다. Zod 4.0~4.3은 추론 타입으로는 키가 필수라고 해 놓고 실제 파싱에서는 키가 없어도 허용했다. 이렇게 타입이 약속한 것과 런타임 결과가 어긋나지 않도록 맞추는 것을 건전성(soundness) 수정이라고 하며, 키를 필수로 요구하는 변경이 바로 그런 수정이다. 키 자체가 빠질 수 있는 필드라면 `.optional()`을 명시해야 한다.

```ts
mySchema.parse({}); // ❌ (v4.3 이하에서는 ✅)
mySchema.parse({ a: undefined, b: undefined }); // ✅

// 없을 수 있는 키에는 .optional()을 사용
z.object({ a: z.any().optional() }).parse({}); // ✅
```

#### `.merge()` 지원 중단 (deprecates `.merge()`)

`ZodObject`의 `.merge()` 메서드는 `.extend()`로 대체되어 지원 중단되었다. `.extend()`는 같은 기능을 제공하면서 strict 여부 상속의 모호함을 피하고, TypeScript 성능도 더 좋다.

```ts
// .merge (지원 중단)
const ExtendedSchema = BaseSchema.merge(AdditionalSchema);

// .extend (권장)
const ExtendedSchema = BaseSchema.extend(AdditionalSchema.shape);

// 또는 구조 분해 사용 (tsc 성능이 가장 좋음)
const ExtendedSchema = z.object({
  ...BaseSchema.shape,
  ...AdditionalSchema.shape,
});
```

> **참고:** TypeScript 성능을 더 높이려면 `.extend()` 대신 객체 구조 분해를 고려한다. 자세한 내용은 [「`.extend()`」](03_schemas_objects_collections.md#extend)를 참고한다.

### `z.nativeEnum()` 지원 중단 (`z.nativeEnum()` deprecated)

`z.nativeEnum()`은 지원 중단되고 `z.enum()`으로 대체되었다. `z.enum()`이 enum 형태의 입력도 받도록 오버로드되었다.

```ts
enum Color {
  Red = "red",
  Green = "green",
  Blue = "blue",
}

const ColorSchema = z.enum(Color); // ✅
```

`ZodEnum`을 리팩터링하면서 오래전에 지원 중단된 중복 기능 몇 가지를 제거했다. 모두 같은 기능이었고 역사적 이유로만 남아 있었다.

```ts
ColorSchema.enum.Red; // ✅ => "Red" (표준 API)
ColorSchema.Enum.Red; // ❌ 제거됨
ColorSchema.Values.Red; // ❌ 제거됨
```

### `z.array()`

#### `.nonempty()` 타입 변경 (changes `.nonempty()` type)

`.nonempty()`는 이제 `z.array().min(1)`과 똑같이 동작한다. 런타임 검사는 그대로지만, 추론 타입이 "원소가 하나 이상인 튜플" 형태로 바뀌지 않고 일반 배열로 남는다. 그래서 Zod 3의 추론 타입에 기대어 `arr[0]`을 `string`으로 다루던 코드는 Zod 4에서 타입이 달라질 수 있다.

```ts
const NonEmpty = z.array(z.string()).nonempty();

type NonEmpty = z.infer<typeof NonEmpty>; 
// Zod 3: [string, ...string[]]
// Zod 4: string[]
```

예전 타입이 필요하면 `z.tuple()`의 두 번째 인자로 나머지(rest) 원소 스키마를 넘긴다. 첫 원소는 반드시 있고 그 뒤로 같은 타입이 이어지는 구조를 그대로 적는 방식이라 TypeScript 타입 시스템과도 더 가깝다.

```ts
z.tuple([z.string()], z.string());
// => [string, ...string[]]
```

### `z.promise()` 지원 중단 (`z.promise()` deprecated)

`z.promise()`를 쓸 이유는 거의 없다. 입력이 `Promise`일 수 있다면 Zod로 파싱하기 전에 `await`하면 된다.

> `z.function()`으로 비동기 함수를 정의하려고 `z.promise`를 쓰고 있었다면 그것도 더 이상 필요 없다. 아래 [`z.function()`](#zfunction) 절을 참고한다.

### `z.function()`

`z.function()`의 결과는 더 이상 Zod 스키마가 아니다. Zod로 검증되는 함수를 정의하는 독립적인 "함수 팩토리(function factory)"로 동작한다. API도 바뀌어서, `args()`와 `.returns()` 메서드 대신 `input`과 `output` 스키마를 처음에 정의한다.

```ts
// Zod 4
const myFunction = z.function({
  input: [z.object({
    name: z.string(),
    age: z.number().int(),
  })],
  output: z.string(),
});

myFunction.implement((input) => {
  return `Hello ${input.name}, you are ${input.age} years old.`;
});
```

```ts
// Zod 3
const myFunction = z.function()
  .args(z.object({
    name: z.string(),
    age: z.number().int(),
  }))
  .returns(z.string());

myFunction.implement((input) => {
  return `Hello ${input.name}, you are ${input.age} years old.`;
});
```

함수 타입의 Zod 스키마가 꼭 필요하다면 [이 우회 방법](https://github.com/colinhacks/zod/issues/4143#issuecomment-2845134912)을 고려한다. `z.function()`의 자세한 사용법은 [「함수」](04_refinements_transforms_codecs.md#함수-functions)를 참고한다.

#### `.implementAsync()` 추가 (adds `.implementAsync()`)

비동기 함수를 정의하려면 `implement()` 대신 `implementAsync()`를 쓴다.

```ts
myFunction.implementAsync(async (input) => {
  return `Hello ${input.name}, you are ${input.age} years old.`;
});
```

### `.refine()`

#### 타입 서술어 무시 (ignores type predicates)

[타입 서술어(type predicate)](https://www.typescriptlang.org/docs/handbook/2/narrowing.html#using-type-predicates)는 반환 타입을 `val is string`처럼 적어, 함수가 `true`를 반환하면 인자가 그 타입이라고 TypeScript에 알려 주는 문법이다. Zod 3에서는 정제 함수로 타입 서술어를 넘기면 스키마의 추론 타입이 그 타입으로 좁혀졌다. 문서화되지는 않았지만 몇몇 이슈에서 논의된 동작이다. Zod 4에서는 그렇지 않으므로, 이 동작에 기대던 코드는 추론 타입이 원래 스키마의 타입으로 돌아간다.

```ts
const mySchema = z.unknown().refine((val): val is string => {
  return typeof val === "string"
});

type MySchema = z.infer<typeof mySchema>; 
// Zod 3: `string`
// Zod 4: 여전히 `unknown`
```

#### `ctx.path` 제거 (drops `ctx.path`)

Zod 3에서는 `.superRefine()` 등의 정제 함수가 받는 `ctx`에서 `ctx.path`로 현재 검사 중인 값의 경로를 읽을 수 있었다. Zod 4의 새 파싱 구조는 이 `path` 배열을 미리 계산하지 않으므로 `ctx.path`도 사라졌다. Zod 4의 큰 성능 향상을 위해 필요한 변경이었다.

```ts
z.string().superRefine((val, ctx) => {
  ctx.path; // ❌ 더 이상 사용할 수 없음
});
```

#### 두 번째 인자로 함수를 받는 오버로드 제거 (drops function as second argument)

`.refine()`의 두 번째 인자로 입력값을 받아 에러 옵션을 돌려주는 함수를 넘기던, 다음과 같은 끔찍한 오버로드가 제거되었다.

```ts
const longString = z.string().refine(
  (val) => val.length > 10,
  (val) => ({ message: `${val} is not more than 10 characters` })
);
```

### `z.ostring()` 등 제거 (`z.ostring()`, etc dropped)

문서화되지 않은 편의 메서드 `z.ostring()`, `z.onumber()` 등이 제거되었다. optional 문자열 스키마 등을 정의하는 축약형이었다.

### `z.literal()`

#### `symbol` 지원 제거 (drops `symbol` support)

심볼은 리터럴 값으로 보지 않으며, `===`로 단순 비교할 수도 없다. Zod 3의 실수였다.

### 정적 `.create()` 팩토리 제거 (static `.create()` factories dropped)

예전에는 모든 Zod 클래스에 정적 `.create()` 메서드가 있었다. 이제는 독립적인 팩토리 함수로 구현한다.

```ts
z.ZodString.create(); // ❌ 
```

### `z.record()`

#### 인자 하나만 쓰는 방식 제거 (drops single argument usage)

예전에는 `z.record()`에 인자 하나만 줄 수 있었다. 이제는 지원하지 않는다.

```ts
// Zod 3
z.record(z.string()); // ✅

// Zod 4
z.record(z.string()); // ❌
z.record(z.string(), z.string()); // ✅
```

#### enum 지원 개선 (improves enum support)

`z.record()`의 키 스키마로 enum을 넘겼을 때 필수 키를 판단하는 방식이 달라졌다. Zod 3에서는 enum에 나열된 키를 모두 선택적으로 취급해 partial 타입으로 추론했다.

```ts
const myRecord = z.record(z.enum(["a", "b", "c"]), z.number()); 
// { a?: number; b?: number; c?: number; }
```

Zod 4에서는 enum에 나열된 모든 키를 필수로 추론한다. 파싱할 때도 해당 키가 입력에 모두 있는지 확인해 완전성(exhaustiveness)을 보장한다.

```ts
const myRecord = z.record(z.enum(["a", "b", "c"]), z.number());
// { a: number; b: number; c: number; }
```

optional 키를 쓰는 예전 동작을 재현하려면 `z.partialRecord()`를 쓴다.

```ts
const myRecord = z.partialRecord(z.enum(["a", "b", "c"]), z.number());
// { a?: number; b?: number; c?: number; }
```

### `z.intersection()`

#### 병합 충돌 시 `Error`를 던짐 (throws `Error` on merge conflict)

Zod의 교차(intersection)는 입력을 두 스키마로 각각 파싱한 뒤 결과를 병합한다. Zod 3에서는 결과를 병합할 수 없으면 특수한 `"invalid_intersection_types"` 이슈를 담은 `ZodError`를 던졌다.

Zod 4에서는 일반 `Error`를 던진다. 병합할 수 없는 결과가 나온다는 것은 호환되지 않는 두 타입을 교차시켰다는 뜻이므로 스키마의 구조적 문제다. 따라서 검증 에러보다 일반 에러가 더 적절하다.

### 내부 변경 (Internal changes)

> 일반적인 Zod 사용자는 이 아래 내용을 대부분 무시해도 된다. 이 변경들은 사용자가 쓰는 `z` API에 영향을 주지 않는다.

내부 변경은 너무 많아 모두 나열할 수 없다. 다만 그중 일부는 의도했든 아니든 특정 구현 세부 사항에 기대는 일반 사용자와도 관련이 있고, Zod 위에 도구를 만드는 라이브러리 작성자에게는 특히 중요하다.

#### 제네릭 변경 (updates generics)

여러 클래스의 제네릭 구조가 바뀌었다. 가장 중요한 것은 `ZodType` 기본 클래스의 변경이다.

```ts
// Zod 3
class ZodType<Output, Def extends z.ZodTypeDef, Input = Output> {
  // ...
}

// Zod 4
class ZodType<Output = unknown, Input = unknown> {
  // ...
}
```

두 번째 제네릭 `Def`는 완전히 제거되었다. 기본 클래스는 이제 `Output`과 `Input`만 추적한다. 예전에는 `Input`의 기본값이 `Output`이었지만 이제는 `unknown`이다. 덕분에 `z.ZodType`을 다루는 제네릭 함수가 많은 경우 더 직관적으로 동작한다.

```ts
function inferSchema<T extends z.ZodType>(schema: T): T {
  return schema;
};

inferSchema(z.string()); // z.ZodString
```

`z.ZodTypeAny`는 더 이상 필요 없다. 그냥 `z.ZodType`을 쓴다.

#### `z.core` 추가 (adds `z.core`)

Zod와 Zod Mini가 코드를 공유할 수 있도록 많은 유틸리티 함수와 타입을 새 하위 패키지 `zod/v4/core`로 옮겼다.

```ts
import * as z from "zod/v4/core";

function handleError(iss: z.$ZodError) {
  // 처리
}
```

편의를 위해 `zod/v4/core`의 내용은 `zod`와 `zod/mini`에서도 `z.core` 네임스페이스로 다시 내보낸다.

```ts
import * as z from "zod";

function handleError(iss: z.core.$ZodError) {
  // 처리
}
```

core 하위 라이브러리의 내용은 [「Zod Core」](07_packages_and_compile.md#zod-core)를 참고한다.

#### `._def` 이동 (moves `._def`)

`._def` 속성이 `._zod.def`로 옮겨졌다. 모든 내부 def의 구조는 바뀔 수 있다. 라이브러리 작성자에게 관련 있지만 여기서 전부 문서화하지는 않는다.

#### `ZodEffects` 제거 (drops `ZodEffects`)

사용자가 쓰는 API에는 영향이 없지만 짚어 둘 만한 내부 변경이다. Zod가 정제를 다루는 방식을 크게 재구성한 일의 일부다.

예전에는 정제와 변환(transform) 모두 `ZodEffects`라는 래퍼 클래스 안에 있었다. 즉 스키마에 둘 중 하나를 추가하면 원래 스키마가 `ZodEffects` 인스턴스로 감싸졌다. Zod 4에서는 정제가 스키마 자체 안에 있다. 정확히는 각 스키마에 "검사(check)" 배열이 있다. 검사는 Zod 4에서 새로 생긴 개념으로, 정제를 일반화해 `z.toLowerCase()`처럼 부수 효과가 있을 수 있는 변환까지 포함한다.

이 점은 여러 검증을 `.check()` 메서드로 조합하는 Zod Mini API에서 특히 잘 드러난다.

```ts
import * as z from "zod/mini";

z.string().check(
  z.minLength(10),
  z.maxLength(100),
  z.toLowerCase(),
  z.trim(),
);
```

#### `ZodTransform` 추가 (adds `ZodTransform`)

한편 변환은 전용 `ZodTransform` 클래스로 옮겨졌다. 이 스키마 클래스는 입력 변환을 나타내며, 이제 변환을 독립적으로 정의할 수도 있다.

```ts
import * as z from "zod";

const schema = z.transform(input => String(input));

schema.parse(12); // => "12"
```

`ZodTransform`은 주로 `ZodPipe`와 함께 쓴다. `ZodPipe`는 두 스키마를 이어, 앞 스키마의 출력을 뒤 스키마의 입력으로 넘기는 [파이프](04_refinements_transforms_codecs.md#파이프-pipes) 스키마다. `.transform()` 메서드는 이제 원래 스키마와 `ZodTransform`을 이은 `ZodPipe` 인스턴스를 반환한다. Zod 3에서 `.transform()`의 반환 타입을 `ZodEffects`로 적어 두었던 코드라면 이 타입으로 바꿔야 한다.

```ts
z.string().transform(val => val); // ZodPipe<ZodString, ZodTransform>
```

#### `ZodPreprocess` 제거 (drops `ZodPreprocess`)

`.transform()`과 마찬가지로 `z.preprocess()` 함수도 전용 `ZodPreprocess` 인스턴스 대신 `ZodPipe` 인스턴스를 반환한다. 순서만 반대여서, 전처리 변환이 앞에 오고 대상 스키마가 뒤에 온다.

```ts
z.preprocess(val => val, z.string()); // ZodPipe<ZodTransform, ZodString>
```

#### `ZodBranded` 제거 (drops `ZodBranded`)

브랜딩은 `.brand()`로 추론 타입에 이름표를 붙여, 구조가 같아도 TypeScript가 서로 다른 타입으로 구분하게 만드는 기능이다(자세한 내용은 [「브랜드 타입」](04_refinements_transforms_codecs.md#브랜드-타입-branded-types) 참고). Zod 4는 이를 전용 `ZodBranded` 클래스 대신 추론 타입을 직접 수정하는 방식으로 처리한다. 사용자가 쓰는 API는 그대로다.

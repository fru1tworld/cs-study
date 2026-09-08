# Biome 포매터와 린터

## 포매터 (Formatter)

> 원문: https://biomejs.dev/formatter

### 개요

Biome 포매터는 Prettier의 철학을 따르는 의견 기반(opinionated) 도구다. 설정 옵션을 의도적으로 제한해 스타일을 결정하는 논쟁을 줄이고, 팀이 실제 개발 작업에 집중하도록 한다.

### 기본 사용법

파일을 수정하지 않고 포매팅을 검사하려면 다음 명령을 실행한다.

```bash
npx @biomejs/biome format ./src
pnpx @biomejs/biome format ./src
bunx --bun @biomejs/biome format ./src
deno run -A npm:@biomejs/biome format ./src
```

포매팅을 파일에 적용하려면 `--write`를 붙인다.

```bash
npx @biomejs/biome format --write ./src
```

`./src/**/*.test.{js,ts}` 같은 글로브 패턴은 셸이 확장한다. 셸마다 재귀 글로브나 교대 패턴 지원 여부가 다르고, 성능 비용과 파일 수 제한도 있다.

### 기본 설정값

#### 언어 공통 옵션

- `indentStyle`: `"tab"` (기본값)
- `indentWidth`: `2` (기본값)
- `lineWidth`: `80` (기본값)
- `lineEnding`: `"lf"`
- `formatWithErrors`: `false`
- `enabled`: `true`
- `attributePosition`: `"auto"`

#### JavaScript 전용 옵션

- `arrowParentheses`: `"always"` -- 화살표 함수 매개변수 괄호
- `bracketSameLine`: `false` -- JSX 닫는 괄호 위치
- `bracketSpacing`: `true` -- 객체 리터럴 내 공백
- `delimiterSpacing`: `false` -- 구분자 내부 공백
- `jsxQuoteStyle`: `"double"` -- JSX 따옴표 스타일
- `quoteProperties`: `"asNeeded"` -- 객체 속성 따옴표
- `semicolons`: `"always"` -- 세미콜론 삽입
- `trailingCommas`: `"all"` -- 후행 쉼표

#### JSON 전용 옵션

- `trailingCommas`: `"none"` (기본값)

#### CSS 전용 옵션

- `quoteStyle`: `"double"` (기본값)

### 설정 예시

```json
{
  "formatter": {
    "indentStyle": "space",
    "indentWidth": 4,
    "lineWidth": 120
  },
  "javascript": {
    "formatter": {
      "quoteStyle": "single",
      "semicolons": "asNeeded",
      "trailingCommas": "es5"
    }
  }
}
```

### `.editorconfig` 지원

v1.9부터 `.editorconfig` 파일을 읽을 수 있다. CLI에서 `--use-editorconfig=true`를 지정하거나 설정 파일에서 활성화한다.

### 포매팅 억제

파일 전체의 포매팅을 억제하려면 다음 주석을 쓴다.

```javascript
// biome-ignore-all format: reason
```

특정 노드만 억제할 수도 있다.

```javascript
// biome-ignore format: reason
const x = { a:1, b:2 }
```

## 린터 (Linter)

> 원문: https://biomejs.dev/linter

### 개요

Biome 린터는 여러 언어의 코드를 정적으로 분석한다. 519개 이상의 규칙으로 오류와 코드 품질 문제를 찾으며, 코드 배치와 같은 포매팅 작업은 앞에서 살펴본 포매터에 맡긴다.

### 명명 규칙

- `use*`: 특정 관행 사용을 강제하는 규칙 (예: `useConst`)
- `no*`: 특정 패턴 사용을 금지하는 규칙 (예: `noVar`)

### 규칙 그룹

- `accessibility`: 접근성 관련 규칙
- `complexity`: 복잡도 관련 규칙
- `correctness`: 정확성/버그 방지 규칙
- `performance`: 성능 관련 규칙
- `security`: 보안 관련 규칙
- `style`: 코드 스타일 규칙
- `suspicious`: 의심스러운 패턴 감지 규칙

### 기본 사용법

```bash
npx @biomejs/biome lint
pnpx @biomejs/biome lint
bunx --bun @biomejs/biome lint
```

파일과 디렉토리를 인자로 전달할 수 있지만, CLI 자체는 글로브 패턴을 지원하지 않는다.

### 수정 유형

- 안전한 수정(safe fix): 코드 의미를 보존하는 변경
  - 저장 시 자동 적용 가능
- 안전하지 않은 수정(unsafe fix): 의미가 달라질 수 있는 변경
  - 수동 검토 필요

```bash
biome lint --write           # 안전한 수정만 적용
biome lint --write --unsafe  # 안전하지 않은 수정까지 적용
```

### 규칙 설정

개별 규칙마다 심각도를 바꾸거나, 규칙을 끄거나, 수정 동작을 제어할 수 있다.

```json
{
  "linter": {
    "enabled": true,
    "rules": {
      "recommended": true,
      "style": {
        "useConst": "warn",
        "noVar": {
          "level": "error",
          "fix": "none"
        }
      },
      "suspicious": {
        "noDebugger": "off"
      }
    }
  }
}
```

- 심각도 수준: `error`, `warn`, `info`, `off`

- 수정 동작: `none`(수정 비활성), `safe`(안전한 수정만), `unsafe`(모두 허용)

### 규칙 필터링 (CLI)

```bash
biome lint --only=correctness     # correctness 그룹만 실행
biome lint --skip=style           # style 그룹 제외
biome lint --only=style/useConst  # 특정 규칙만 실행
```

### 도메인 (Domains)

도메인은 기술 스택별 규칙 모음으로, `package.json`의 의존성을 감지해 자동으로 활성화된다.

- React 도메인: React 관련 규칙
- Solid 도메인: SolidJS 관련 규칙
- 테스트 도메인: 테스트 프레임워크 관련 규칙

### 린팅 억제

파일 전체의 린팅을 억제하는 주석은 다음과 같다.

```javascript
// biome-ignore-all lint: reason
```

특정 규칙만 억제할 때는 규칙 이름을 지정한다.

```javascript
// biome-ignore lint/suspicious/noDebugger: 디버깅 목적
debugger;
```

### 에디터 통합

LSP 호환 에디터에서는 진단과 다음 코드 액션을 제공한다.

- `source.fixAll.biome`: 저장 시 안전한 수정 적용
- `source.suppressRule.inline.biome`: 인라인 억제 주석 추가
- `source.suppressRule.topLevel.biome`: 파일 상단 억제 주석 추가

### ESLint에서 마이그레이션

```bash
biome migrate eslint   # ESLint 설정을 Biome 형식으로 변환
```

마이그레이션 기간에는 기존 위반을 일괄 억제할 수 있다.

```bash
biome lint --suppress --reason "suppressed due to migration"
```

### 프로젝트 도메인과 스캐너

v2 아키텍처에서는 스캐너가 타입 추론과 모듈 분석을 수행한다. 스캐너는 프로젝트 도메인 규칙에 필요하며, 프로젝트 크기에 따라 린팅 시간이 1~7초 늘어난다.

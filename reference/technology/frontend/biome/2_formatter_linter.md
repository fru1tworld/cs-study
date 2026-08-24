# Biome 포매터와 린터

## 포매터 (Formatter)

> 원문: https://biomejs.dev/formatter

### 개요

Biome의 포매터는 Prettier 철학을 따르는 의견 기반(opinionated) 도구임. 설정 옵션을 의도적으로 제한하여 스타일 논쟁을 줄이고, 팀이 실질적인 작업에 집중하도록 함.

### 기본 사용법

포매팅 검사(파일 수정 없음):

```bash
npx @biomejs/biome format ./src
pnpx @biomejs/biome format ./src
bunx --bun @biomejs/biome format ./src
deno run -A npm:@biomejs/biome format ./src
```

포매팅 적용(파일 수정):

```bash
npx @biomejs/biome format --write ./src
```

`./src/**/*.test.{js,ts}` 같은 글로브 패턴은 셸이 확장함. 셸에 따라 재귀 글로브나 교대 패턴 지원이 다르고, 성능 비용과 파일 수 제한이 있음.

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

v1.9부터 `.editorconfig` 파일을 읽을 수 있음. CLI에서 `--use-editorconfig=true` 또는 설정 파일에서 활성화함.

### 포매팅 억제

파일 전체 억제:

```javascript
// biome-ignore-all format: reason
```

특정 노드 억제:

```javascript
// biome-ignore format: reason
const x = { a:1, b:2 }
```

---

## 린터 (Linter)

> 원문: https://biomejs.dev/linter

### 개요

Biome의 린터는 여러 언어에 대해 정적 코드 분석을 수행함. 519개 이상의 규칙으로 오류 감지와 코드 품질 향상을 지원함. 코드 이슈만 다루고, 포매팅은 포매터에 위임함.

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

파일과 디렉토리를 인자로 전달 가능함. CLI에서 직접 글로브 패턴은 지원하지 않음.

### 수정 유형

- 안전한 수정(safe fix): 코드 의미를 보존하는 변경. 저장 시 자동 적용 가능
- 안전하지 않은 수정(unsafe fix): 의미가 달라질 수 있는 변경. 수동 검토 필요

```bash
biome lint --write           # 안전한 수정만 적용
biome lint --write --unsafe  # 안전하지 않은 수정까지 적용
```

### 규칙 설정

개별 규칙의 심각도 변경, 비활성화, 수정 동작 제어가 가능함.

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

심각도 수준: `error`, `warn`, `info`, `off`

수정 동작: `none`(수정 비활성), `safe`(안전한 수정만), `unsafe`(모두 허용)

### 규칙 필터링 (CLI)

```bash
biome lint --only=correctness     # correctness 그룹만 실행
biome lint --skip=style           # style 그룹 제외
biome lint --only=style/useConst  # 특정 규칙만 실행
```

### 도메인 (Domains)

기술 스택별 규칙 모음으로, `package.json`의 의존성을 감지하여 자동 활성화됨.

- React 도메인: React 관련 규칙
- Solid 도메인: SolidJS 관련 규칙
- 테스트 도메인: 테스트 프레임워크 관련 규칙

### 린팅 억제

파일 전체:

```javascript
// biome-ignore-all lint: reason
```

특정 규칙:

```javascript
// biome-ignore lint/suspicious/noDebugger: 디버깅 목적
debugger;
```

### 에디터 통합

LSP 호환 에디터에서 진단과 코드 액션을 제공함.

- `source.fixAll.biome`: 저장 시 안전한 수정 적용
- `source.suppressRule.inline.biome`: 인라인 억제 주석 추가
- `source.suppressRule.topLevel.biome`: 파일 상단 억제 주석 추가

### ESLint에서 마이그레이션

```bash
biome migrate eslint   # ESLint 설정을 Biome 형식으로 변환
```

마이그레이션 기간 동안 기존 위반을 일괄 억제할 수 있음:

```bash
biome lint --suppress --reason "suppressed due to migration"
```

### 프로젝트 도메인과 스캐너

v2 아키텍처에서 스캐너 도구가 타입 추론과 모듈 분석을 수행함. 프로젝트 도메인 규칙에 필요하며, 프로젝트 크기에 따라 린팅 시간에 1~7초 추가됨.

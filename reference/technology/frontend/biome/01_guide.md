# Biome 가이드: 설정과 통합

## 시작하기 (Getting Started)

> 원문: https://biomejs.dev/guides/getting-started

### 개요

Biome는 웹 프로젝트에서 포매팅과 린팅, 코드 어시스트를 함께 처리하는 도구 체인이다. Rust로 작성됐으며 빠른 실행 속도를 특징으로 한다. 각 기능이 맡는 작업은 다음과 같다.

- 포매터: Prettier와 호환되는 의견 기반(opinionated) 포매팅 제공
- 린터: 519개 이상의 규칙으로 정적 분석 수행, ESLint, typescript-eslint 및 기타 소스의 규칙 포함
- 코드 어시스트: import 정렬 등 코드 변환 액션 제공

### 설치

패키지 매니저별 설치 명령은 다음과 같다.

```bash
npm i -D -E @biomejs/biome
pnpm add -D -E @biomejs/biome
yarn add -D -E @biomejs/biome
bun add -D -E @biomejs/biome
deno add -D npm:@biomejs/biome
```

`-E` 플래그는 정확한 버전을 고정한다. Biome는 시맨틱 버전 관리를 따르므로 재현 가능한 빌드를 위해 이 플래그가 권장된다. Node.js 없이 독립 실행형 바이너리로 사용할 수도 있다.

### 설정 파일 초기화

```bash
biome init
```

이 명령을 실행하면 프로젝트 루트에 `biome.json` 파일이 생성된다. Biome는 설정 파일 없이도 동작하지만, 프로젝트별로 커스터마이징이 필요하면 설정 파일을 만든다.

### 기본 명령

```bash
biome format --write          # 모든 파일 포매팅
biome format --write <files>  # 특정 파일 포매팅
biome lint --write            # 린팅 및 안전한 수정 적용
biome check --write           # 포매팅 + 린팅 + import 정렬을 한 번에 실행
```

`biome check`는 세 가지 도구를 모두 실행하는 가장 포괄적인 명령이다.

### 에디터 통합

공식 지원 에디터는 다음과 같다.

- VS Code: 공식 확장 제공
- IntelliJ: 공식 플러그인 제공
- Zed: 내장 지원

커뮤니티에서 지원하는 에디터도 있다.

- Vim/Neovim
- Sublime Text
- Helix

### CI/CD

`biome ci` 명령은 CI 환경에 최적화되어 있으며, 파일을 수정하지 않고 검사만 수행한다.

```bash
biome ci
```

## 설정 (Configure Biome)

> 원문: https://biomejs.dev/guides/configure-biome

### 설정 파일 구조

설정 파일로는 `biome.json` 또는 `biome.jsonc`를 사용한다. 설정은 세 가지 도구(포매터, 린터, 어시스트)를 중심으로 구성하며, 세 도구 모두 기본으로 활성화되어 있다. 도구별로 끄려면 다음처럼 설정한다.

```json
{
  "$schema": "https://biomejs.dev/schemas/2.4.13/schema.json",
  "formatter": { "enabled": false },
  "linter": { "enabled": false },
  "assist": { "enabled": false }
}
```

언어별로 설정을 오버라이드할 수도 있다.

```json
{
  "formatter": {
    "indentStyle": "space",
    "lineWidth": 100
  },
  "javascript": {
    "formatter": {
      "quoteStyle": "single",
      "lineWidth": 120
    }
  },
  "json": {
    "formatter": { "enabled": false }
  }
}
```

Biome에서 `javascript`는 TypeScript, JSX, TSX를 모두 포함하는 범주다.

### 설정 파일 탐색 순서

파일명 우선순위는 다음과 같다.

- 1\. `biome.json`
- 2\. `biome.jsonc`
- 3\. `.biome.json`
- 4\. `.biome.jsonc`

탐색 위치 순서는 다음과 같다.

- 1\. 현재 작업 디렉토리
- 2\. 상위 폴더(재귀적)
- 3\. 홈 디렉토리
  - Linux: `$XDG_CONFIG_HOME` 또는 `$HOME/.config/biome`
  - macOS: `/Users/$USER/Library/Application Support/biome`
  - Windows: `C:\Users\$USER\AppData\Roaming\biome\config`

### 파일 범위 지정

#### 보호 파일

다음 파일은 기본적으로 무시된다.

- `composer.lock`
- `npm-shrinkwrap.json`
- `package-lock.json`
- `yarn.lock`

#### CLI에서 파일 지정

처리할 파일과 폴더는 명령에 직접 전달한다. 이때 글로브 패턴은 Biome가 아니라 셸이 확장한다.

```bash
biome format file1.js src/
```

#### 설정으로 파일 제어

설정 파일에서 처리 범위를 정하려면 `files.includes`에 글로브 패턴을 지정한다. `!` 접두사는 해당 경로를 제외하고, `!!` 접두사는 프로젝트 인덱싱에서도 완전히 제외한다.

```json
{
  "files": {
    "includes": ["src/**/*.js", "test/**/*.js", "!**/*.min.js"]
  },
  "linter": {
    "includes": ["**", "!test/**"]
  }
}
```

### Well-Known 파일

Biome는 특정 파일명을 인식해 확장자와 무관하게 적절한 파서 설정을 적용한다.

- JSON(주석과 후행 쉼표 불허): `.all-contributorsrc`, `.bowerrc`, `.nycrc`, `mcmod.info` 등
- JSONC(주석 허용, 후행 쉼표 불허): `.eslintrc.json`, `.jshintrc`, `tslint.json`, `turbo.json` 등
- JSONC(주석과 후행 쉼표 모두 허용): `.babelrc`, `tsconfig.json`, `jsconfig.json`, `deno.json`, `.devcontainer.json`, `.vscode/*.json` 등

## VCS 통합 (Integrate in VCS)

> 원문: https://biomejs.dev/guides/integrate-in-vcs

### 개요

VCS 기능은 Git만 지원하며, 필요할 때 선택적으로 활성화한다.

```json
{
  "vcs": {
    "enabled": true,
    "clientKind": "git"
  }
}
```

이 설정만으로는 아무 동작도 하지 않으므로, 아래 기능을 개별적으로 활성화해야 한다.

### .gitignore 연동

```json
{
  "vcs": {
    "enabled": true,
    "clientKind": "git",
    "useIgnoreFile": true
  }
}
```

`useIgnoreFile`을 켜면 `.gitignore`와 `.git/info/exclude` 파일을 모두 읽는다. 링크된 워크트리에서는 공유 Git 디렉토리의 exclude 파일을 읽는다.

### 변경된 파일만 처리

```json
{
  "vcs": {
    "enabled": true,
    "clientKind": "git",
    "useIgnoreFile": true,
    "defaultBranch": "main"
  }
}
```

```bash
biome check --changed            # 기본 브랜치 대비 변경 파일만 처리
biome check --changed --since=next  # 특정 브랜치 기준으로 변경 파일 처리
```

이 옵션은 코드의 의미가 달라졌는지까지 판단하지 않는다. 공백이나 줄바꿈만 추가해도 변경 파일로 처리하며, 그 파일을 import하는 다운스트림 파일까지 처리 범위를 넓히지는 않는다.

### 스테이지된 파일만 처리

```bash
biome check --staged
```

`--staged`는 `ci` 명령에서는 사용할 수 없다.

## 대규모 프로젝트 (Big Projects)

> 원문: https://biomejs.dev/guides/big-projects

### 중첩 설정 파일

Biome는 현재 작업 디렉토리에서 상위로 올라가며 `biome.json`을 탐색하므로, 폴더별로 다른 설정을 적용할 수 있다.

```
app/
├── backend/
│   ├── biome.json
│   └── package.json
└── frontend/
    ├── biome.json
    ├── legacy-app/
    │   └── package.json
    └── new-app/
        └── package.json
```

위 구조에서 `app/backend`에서 실행하면 `app/backend/biome.json`을 사용하고, `app/frontend/legacy-app`에서 실행하면 `app/frontend/biome.json`을 사용한다.

### 모노레포 설정 (v2+)

#### 1단계: 루트 설정

모노레포 루트에 기본 `biome.json`을 생성한다.

```json
{
  "linter": {
    "enabled": true,
    "rules": { "recommended": true }
  },
  "formatter": {
    "lineWidth": 120,
    "indentStyle": "space",
    "indentWidth": 4
  }
}
```

#### 2단계: 패키지별 설정 (상속)

패키지에서는 `"extends": "//"`로 루트 설정을 상속하고 필요한 부분만 오버라이드한다.

```json
{
  "extends": "//",
  "linter": {
    "rules": {
      "suspicious": { "noConsole": "off" }
    }
  }
}
```

`extends: "//"`를 사용하면 `"root": false`는 생략할 수 있다.

#### 3단계: 독립 패키지 설정

다른 표준을 따르는 패키지는 `extends` 없이 설정한다.

```json
{
  "root": false,
  "formatter": { "lineWidth": 100 }
}
```

#### 4단계: 실행

루트와 개별 패키지 중 어디서 실행해도 모든 설정이 올바르게 적용된다.

### extends로 설정 공유

`extends` 필드에는 배열로 여러 파일을 지정할 수 있다.

```json
{
  "extends": ["./common.json"]
}
```

파일은 나열한 순서대로 처리되며, 뒤의 설정이 앞의 설정을 오버라이드한다. 단, 확장 대상 파일이 다시 다른 파일을 확장할 수는 없다.

### npm 패키지로 설정 배포

Biome는 `node_modules/`에서도 설정 파일을 해석할 수 있다. 공유 설정 패키지는 `package.json`의 `exports`로 설정 파일을 내보낸다.

```json
{
  "name": "@org/shared-configs",
  "type": "module",
  "exports": {
    "./biome": "./biome.json"
  }
}
```

사용하는 프로젝트의 `biome.json`에서 이를 확장한다.

```json
{
  "extends": ["@org/shared-configs/biome"]
}
```

`.`으로 시작하거나 `.json` 또는 `.jsonc`로 끝나는 경로는 상대 경로로 취급되므로 `node_modules/`에서 해석하지 않는다.

## 언어 지원 (Language Support)

> 원문: https://biomejs.dev/internals/language-support

### 완전 지원

- JavaScript: ES2024 기준, 공식 구문만 지원, Stage 3 이상 제안 채택
- TypeScript: 5.9 버전 지원
- JSX/TSX: 파싱, 포매팅, 린팅, 플러그인 모두 지원
- JSON/JSONC: 파싱, 포매팅, 린팅, 플러그인 모두 지원
- CSS: 파싱, 포매팅, 린팅, 플러그인 모두 지원
- GraphQL: 파싱, 포매팅, 린팅 지원, 플러그인 미지원

### 실험적 지원

- HTML: 포매팅 실험적, 린팅 지원
- SVG: 포매팅 실험적, 린팅 지원
- Vue/Svelte/Astro: v2.3.0부터 기본 지원, 파싱/포매팅/린팅 모두 실험적

### 개발 중

- SCSS, YAML, Markdown: 파싱과 포매팅 개발 중

### 임베디드 언어 (실험적)

JavaScript 템플릿 리터럴 안의 CSS(`css`, `styled` 태그)와 GraphQL(`gql`, `graphql` 태그)을 지원하며, `javascript.experimentalEmbeddedSnippetsEnabled` 옵션으로 활성화한다.

### JSONC 자동 설정

`.jsonc` 파일은 자동으로 주석과 후행 쉼표를 허용한다. `tsconfig.json`, `.babelrc`, `.eslintrc.json` 같은 Well-Known 파일에도 적절한 파싱 설정이 적용된다.

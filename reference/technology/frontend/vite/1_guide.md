# Vite 가이드: 핵심 개념과 기능

## 시작하기 (Getting Started)

> 원문: https://vite.dev/guide

### 개요

Vite(프랑스어로 "빠른"이라는 뜻, 발음 `/viːt/`)는 현대 웹 프로젝트를 위한 빠르고 가벼운 개발 경험을 제공하는 빌드 도구임. 두 가지 핵심 구성 요소로 이루어짐.

- 개발 서버: 네이티브 ES 모듈 위에 구축, 극도로 빠른 Hot Module Replacement(HMR) 등 향상된 기능 제공
- 빌드 명령: Rolldown을 사용하여 코드를 번들링, 고도로 최적화된 정적 에셋을 프로덕션용으로 생성함

### 주요 특징

- 합리적인 기본 설정을 즉시 제공함
- 플러그인과 플러그인 API를 통해 확장 가능함
- 완전한 타이핑을 갖춘 JavaScript API 지원
- 프레임워크 지원은 플러그인을 통해 제공됨

### 브라우저 지원

- 개발 환경: 최신 JavaScript 및 CSS 기능을 지원하는 모던 브라우저를 전제로 함. 변환 대상을 `esnext`로 설정함
- 프로덕션 빌드: 각 메이저 릴리스에 고정된 날짜 기준의 Baseline Widely Available 브라우저 버전을 대상으로 함. 현재 메이저 버전은 2023년 중반 기준 브라우저 버전에 해당함
- 레거시 브라우저 지원이 필요하면 `@vitejs/plugin-legacy` 플러그인 사용 가능

### 온라인에서 Vite 체험

StackBlitz에서 `vite.new/`로 바로 테스트 가능함. 템플릿을 지정하려면 `vite.new/{template}` 형식 사용.

지원 템플릿 목록:

- JavaScript: vanilla, vue, react, preact, lit, svelte, solid, qwik
- TypeScript: vanilla-ts, vue-ts, react-ts, preact-ts, lit-ts, svelte-ts, solid-ts, qwik-ts

### 첫 Vite 프로젝트 스캐폴딩

패키지 매니저별 기본 명령:

```bash
npm create vite@latest
yarn create vite
pnpm create vite
bun create vite
deno init --npm vite
```

프로젝트 이름과 템플릿을 지정하는 경우:

```bash
npm create vite@latest my-vue-app -- --template vue
yarn create vite my-vue-app --template vue
pnpm create vite my-vue-app --template vue
bun create vite my-vue-app --template vue
deno init --npm vite my-vue-app --template vue
```

- 호환성 요구사항: Node.js 버전 20.19+ 또는 22.12+ 필요. 일부 템플릿은 더 높은 Node.js 버전을 요구함
- `--no-interactive` 플래그로 프롬프트를 건너뛸 수 있음

### 커뮤니티 템플릿

Awesome Vite 저장소에서 추가 템플릿을 찾을 수 있음. GitHub URL을 `github.stackblitz.com`으로 변경하면 미리보기 가능.

`tiged`를 사용한 스캐폴딩:

```bash
npx tiged user/project my-project
cd my-project
npm install
npm run dev
```

### 수동 설치

Vite CLI 설치:

```bash
npm install -D vite
yarn add -D vite
pnpm add -D vite
bun add -D vite
deno add -D npm:vite
```

`index.html` 생성:

```html
<p>Hello Vite!</p>
```

개발 서버 실행:

```bash
npx vite
yarn vite
pnpm vite
bunx vite
deno run -A npm:vite
```

기본 서빙 주소는 `http://localhost:5173`임.

### index.html과 프로젝트 루트

- Vite는 `index.html`을 소스 코드의 일부이자 모듈 그래프의 진입점으로 취급함. 다른 빌드 도구처럼 숨겨진 파일이 아님
- `<script type="module" src="...">` 참조를 JavaScript 소스 코드로 해석함
- 인라인 `<script type="module">`과 `<link href>`를 통한 CSS도 지원함
- URL을 자동으로 리베이스하므로 특별한 플레이스홀더가 불필요함
- 여러 `.html` 진입점을 사용하는 멀티 페이지 앱도 지원함

대체 루트 지정:

```bash
vite serve some/sub/dir
```

루트를 변경하면 설정 파일(`vite.config.js`)도 함께 이동해야 함.

### 커맨드 라인 인터페이스

스캐폴딩된 프로젝트의 기본 npm 스크립트:

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  }
}
```

`--port`, `--open` 등 추가 CLI 옵션 사용 가능. 전체 목록은 `npx vite --help`로 확인.

### 미릴리스 커밋 사용

pkg.pr.new을 통해 특정 커밋을 설치할 수 있음:

```bash
npm install -D https://pkg.pr.new/vite@SHA
yarn add -D https://pkg.pr.new/vite@SHA
pnpm add -D https://pkg.pr.new/vite@SHA
bun add -D https://pkg.pr.new/vite@SHA
```

최근 한 달 이내의 커밋만 사용 가능함.

로컬 개발 방법:

```bash
git clone https://github.com/vitejs/vite.git
cd vite
pnpm install
cd packages/vite
pnpm run build
pnpm link
```

프로젝트에서 `pnpm link vite`로 연결. 전이 의존성에서 사용하는 Vite 버전 교체 시 npm 또는 pnpm의 overrides 기능을 활용함.

---

## 프로젝트 철학 (Project Philosophy)

> 원문: https://vite.dev/guide/philosophy

### 가볍고 확장 가능한 코어 (Lean Extendable Core)

- 일반적인 웹 앱 패턴을 지원하면서 장기적으로 가볍고 유지보수 가능한 코어를 유지함
- Rollup의 플러그인 API를 기반으로 구축된 플러그인 시스템을 통해 확장성을 확보함
- `vite-plugin-pwa` 같은 커뮤니티 플러그인을 활용할 수 있음
- 번들러 Rolldown은 Rollup의 플러그인 인터페이스와 호환성을 유지하여, 두 도구 모두에서 플러그인이 동작함

### 모던 웹 표준 추진 (Pushing the Modern Web)

- 소스 코드는 반드시 ESM으로 작성해야 함. 비ESM 의존성은 사전 번들링됨
- 웹 워커는 현대 표준에 맞게 `new Worker` 구문을 사용함
- Node.js 모듈은 브라우저에서 실행 불가능함
- 이러한 패턴은 미래 호환성을 보장하지만 다른 빌드 도구와 다를 수 있음

### 성능에 대한 실용적 접근 (A Pragmatic Approach to Performance)

- 개발 서버 아키텍처가 규모 확장 시에도 빠른 HMR을 지원함
- Oxc 툴체인과 Rolldown을 포함한 네이티브 도구를 활용하여 집약적 작업을 처리함
- JavaScript는 속도와 유연성의 균형을 위해 유지함
- 프레임워크 플러그인은 사용자 코드 컴파일에 Babel을 사용 가능함
- Rolldown의 Rollup 호환성으로 광범위한 플러그인 생태계에 접근 가능함

### 프레임워크의 기반으로서의 Vite (Building Frameworks on Top of Vite)

- 직접 사용도 가능하지만 프레임워크의 기반으로서 탁월함
- 프레임워크에 종속되지 않는 코어에 다양한 UI 프레임워크용 플러그인을 제공함
- JavaScript API를 통해 프레임워크 작성자가 맞춤형 경험을 만들 수 있음
- SSR 프리미티브를 제공하고 Ruby, Laravel 등 백엔드 프레임워크와 잘 연동됨

### 활발한 생태계 (An Active Ecosystem)

- 프레임워크 유지보수자, 플러그인 제작자, 사용자, Vite 팀 간의 협력으로 발전함
- vite-ecosystem-ci 도구가 Vite를 사용하는 주요 프로젝트에 대해 CI 검사를 실행하여 릴리스 전 회귀를 방지함

---

## 왜 Vite인가 (Why Vite)

> 원문: https://vite.dev/guide/why

### 기원

- 웹 애플리케이션이 복잡해지면서 기존 개발 도구의 느린 서버 시작, 느린 핫 업데이트, 긴 프로덕션 빌드가 문제가 됨
- Vite는 기존 접근 방식을 점진적으로 개선하는 대신 개발 시 코드를 제공하는 방식을 근본적으로 재고함
- 브라우저가 ES 모듈(ESM)을 지원하게 되면서 사전 번들링 없이 직접 JavaScript 파일 로딩이 가능해짐

### 두 단계로 분리된 개발 작업

- 의존성(Dependencies): 자주 변경되지 않는 라이브러리. 네이티브 도구로 한 번 사전 번들링하여 즉시 사용 가능하게 함
- 소스 코드(Source code): 자주 수정되는 애플리케이션 코드. 네이티브 ESM을 통해 요청 시 전달하며, 현재 페이지에 필요한 모듈만 브라우저가 로드함

이 접근 방식으로 애플리케이션 크기와 무관하게 거의 즉각적인 개발 서버 시작이 가능해짐. 파일 편집 시 HMR이 네이티브 ESM 위에서 변경된 모듈만 업데이트하여 전체 페이지 리로드나 리빌드 지연이 없음.

### 영향과 맥락

- Snowpack이 번들 없는 개발을 개척했고 Vite의 의존성 사전 번들링에 영감을 줌
- WMR은 개발과 빌드 양쪽에서 작동하는 범용 플러그인 API에 영향을 줌
- @web/dev-server는 Vite 1.0의 서버 설계에 영감을 줌

번들 없는 ESM이 개발 중에는 효과적이지만, 프로덕션 배포에서는 중첩 임포트로 인한 네트워크 왕복 때문에 비효율적이므로 최적화된 프로덕션 빌드를 위한 번들링이 여전히 필요함.

### 생태계와 함께 성장

- Nuxt, SvelteKit, Astro, React Router, Analog, SolidStart 등 다수의 프레임워크가 Vite를 빌드 레이어로 채택함
- Vitest와 Storybook 같은 도구가 Vite의 활용 범위를 앱 번들링 너머로 확장함
- Laravel, Ruby on Rails 같은 백엔드 프레임워크가 프론트엔드 에셋 파이프라인에 Vite를 통합함
- vite-ecosystem-ci로 모든 Vite 변경 사항에 대해 주요 생태계 프로젝트를 테스트하여 생태계 건강성을 릴리스 프로세스의 일부로 관리함

### 통합 툴체인

- 원래 Vite는 개발 컴파일 속도를 위한 esbuild와 프로덕션 최적화를 위한 Rollup으로 별도의 도구를 사용했음. 이 이중 파이프라인 방식은 변환 동작과 플러그인 시스템에서 불일치를 야기함
- Rolldown이 두 가지를 Rust 기반 단일 번들러로 통합함. 기존 플러그인 API와 호환 가능함
- Oxc를 파싱, 변환, 축소에 통합하여 빌드 도구, 번들러, 컴파일러가 함께 발전하는 엔드투엔드 툴체인을 제공함

### 미래 방향

- 풀 번들 모드: Rolldown으로 프로덕션과 유사한 개발 서버 번들링 모드 탐색 가능. 번들링 없는 요청이 많은 대규모 코드베이스의 네트워크 오버헤드를 줄일 수 있음
- Environment API: "클라이언트"와 "SSR"만의 빌드 대상을 넘어, 프레임워크가 엣지 런타임, 서비스 워커, 배포 대상 등 커스텀 환경을 정의할 수 있게 함. 각 환경마다 고유한 모듈 해석과 실행 규칙을 가짐
- JavaScript와 함께 발전: Oxc와 Rolldown이 Vite와 협업하여 새로운 언어 기능을 업스트림 의존성 지연 없이 전체 툴체인에 빠르게 도입 가능하게 함

---

## 기능 (Features)

> 원문: https://vite.dev/guide/features

### 의존성 관리

- Vite는 베어 모듈 임포트(`import { someMethod } from 'my-dep'` 형태)를 자동으로 감지함
- Rolldown을 사용해 사전 번들링을 수행하여 CommonJS/UMD 모듈을 ESM으로 변환하고 임포트를 유효한 브라우저 URL로 재작성함

### Hot Module Replacement (HMR)

- Vue와 React에 대한 공식 통합이 있는 HMR API를 제공함
- 페이지 리로드나 상태 손실 없이 즉각적인 업데이트가 가능함

### TypeScript 지원

- `.ts` 파일의 네이티브 임포트와 Oxc Transformer를 통한 트랜스파일링 지원(바닐라 `tsc`보다 빠름)
- 트랜스파일링만 수행하며 타입 검사는 하지 않음. 타입 검사는 IDE 힌트 또는 별도 프로세스로 처리해야 함

### TypeScript `tsconfig.json` 옵션 특별 처리

- `isolatedModules`: 반드시 `true`여야 함 (Oxc 제한)
- `useDefineForClassFields`: 대상 버전에 따라 기본값이 결정됨
- `target`: 무시됨. 대신 `oxc.target` 또는 `build.target` 사용
- `emitDecoratorMetadata`: 부분적 지원
- `paths`: `resolve.tsconfigPaths: true` 설정 필요

### HTML 처리

- HTML 파일이 프로젝트의 진입점 역할을 함
- `<script>`, `<img>`, `<link>`, 미디어 태그 등의 에셋을 자동으로 처리함
- `vite-ignore` 속성으로 특정 요소의 처리를 비활성화 가능

### 프레임워크 통합

- Vue와 React에 대한 공식 플러그인이 존재함
- 기타 프레임워크는 플러그인 생태계를 통한 커뮤니티 지원을 받음

### JSX/TSX

- Oxc Transformer를 통한 트랜스파일링으로 즉시 사용 가능함
- `jsxFactory`와 `jsxFragment` 옵션을 설정할 수 있음

### CSS

- 임포트한 CSS는 `<style>` 태그를 통해 주입되며 HMR을 지원함
- `@import` 인라이닝과 리베이스를 자동으로 수행함
- PostCSS 설정이 존재하면 자동으로 적용됨
- CSS Modules: `.module.css` 파일 사용
- 전처리기 지원: Sass, Less, Stylus
- 프로덕션에서 Lightning CSS를 사용한 축소 가능

### 정적 에셋

- 임포트 시 해석된 공개 URL을 반환함
- 특수 쿼리:
  - `?url`: 명시적 URL 로딩
  - `?raw`: 문자열 임포트
  - `?worker`, `?worker&inline`: 웹 워커

### JSON

- 직접 임포트 가능하며 트리 셰이킹을 위한 이름 있는 내보내기(named export)를 지원함

### Glob 임포트

- `import.meta.glob()` 함수로 패턴에 맞는 여러 모듈을 임포트함

```javascript
const modules = import.meta.glob('./dir/*.js')
```

- 지연 로딩, 즉시 임포트(eager), 이름 있는 임포트, 커스텀 쿼리, 부정 패턴, 기본 경로 등의 옵션을 지원함

### 동적 임포트 (Dynamic Import)

- 임포트 경로에서 변수 사용이 한 단계 깊이까지 가능하며, 번들링 가능한 코드를 보장하는 제한이 있음

### WebAssembly

- 두 가지 방식 지원:
  - 직접 ESM 임포트로 바이너리 내보내기 노출
  - `?init` 쿼리로 수동 인스턴스화 제어
- TypeScript 지원 시 `allowArbitraryExtensions` 설정 필요

### 웹 워커 (Web Workers)

- `new Worker(new URL('./worker.js', import.meta.url))` 생성자 구문 사용을 권장함
- 또는 쿼리 접미사(`?worker`, `?sharedworker`) 사용 가능

### 콘텐츠 보안 정책 (CSP)

- `html.cspNonce` 설정을 통한 nonce 기반 CSP를 지원하며 자동 메타 태그 주입을 수행함

### 라이선스 생성

- `build.license` 옵션으로 의존성 라이선스를 문서화하는 라이선스 파일을 생성함

### 빌드 최적화 (자동)

- CSS 코드 스플리팅과 자동 로딩
- 진입 청크를 위한 프리로드 디렉티브 생성
- 비동기 청크 로딩 최적화로 네트워크 왕복을 제거함
- 캐시 적중률 향상을 위한 실험적 청크 임포트 맵 기능

---

## 커맨드 라인 인터페이스 (Command Line Interface)

> 원문: https://vite.dev/guide/cli

### 개발 서버 (Dev Server)

`vite` 명령(별칭: `vite dev`, `vite serve`)으로 개발 서버를 시작함. 루트 디렉터리 매개변수를 받으며 다음 옵션을 포함함:

- 네트워크 설정: `--host`, `--port`, `--strictPort`
- 브라우저 자동 열기: `--open [path]`
- CORS 지원: `--cors` 플래그
- 캐시 우회: `--force` 옵션으로 옵티마이저 캐시 무시
- 설정 파일 지정: `-c, --config <file>`
- 에셋 처리: `--base <path>` (공개 기본 경로)
- 로깅: `-l, --logLevel` (info/warn/error/silent), `--clearScreen`
- 디버깅: `--profile` (Node.js 인스펙터), `-d, --debug [feat]`, `-f, --filter`
- 환경: `-m, --mode <mode>`

### 빌드 명령 (Build)

`vite build` 명령으로 프로덕션 빌드를 생성함. 주요 옵션:

- 대상 지정: `--target <target>`
- 출력 제어: `--outDir`, `--assetsDir`, `--assetsInlineLimit`
- 서버 사이드 렌더링: `--ssr [entry]`
- 소스맵: `--sourcemap [output]`
- 축소: `--minify [minifier]` (oxc/terser/esbuild)
- 매니페스트: `--manifest`, `--ssrManifest`
- 감시 모드: `-w, --watch` (증분 리빌드)
- 디렉터리 관리: `--emptyOutDir`

### 기타 명령

- `vite optimize`: 의존성을 사전 번들링함 (자동 실행되므로 deprecated로 표시됨)
- `vite preview`: dist 디렉터리에서 프로덕션 빌드를 로컬 미리보기함. 프로덕션 서버로 사용하는 것이 아님

모든 명령은 설정 파일 선택, 기본 경로, 로깅 레벨, 디버깅 기능의 공통 옵션을 공유함.

---

## 플러그인 사용 (Using Plugins)

> 원문: https://vite.dev/guide/using-plugins

### 개요

- Vite는 Rollup의 잘 설계된 플러그인 인터페이스를 기반으로 하며, 몇 가지 Vite 전용 옵션이 추가된 플러그인을 통해 확장 가능함

### 플러그인 추가 방법

1. 플러그인을 개발 의존성으로 설치
2. `vite.config.js`의 `plugins` 배열에 포함

설치 예시:

```bash
$ npm add -D @vitejs/plugin-legacy
```

설정 예시:

```javascript
import legacy from '@vitejs/plugin-legacy'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [
    legacy({
      targets: ['defaults', 'not IE 11'],
    }),
  ],
})
```

- `plugins` 배열은 여러 플러그인을 포함하는 프리셋을 단일 요소로 받을 수 있음. 배열은 내부적으로 평탄화되며, falsy 값의 플러그인은 무시되므로 활성화/비활성화 전환이 쉬움

### 플러그인 찾기

- 플러그인을 검색하기 전에 기능 가이드를 먼저 확인하는 것이 좋음. Vite는 많은 일반적인 웹 개발 패턴을 즉시 지원함
- 공식 플러그인은 플러그인 섹션에 문서화되어 있고, 커뮤니티 플러그인은 Vite Plugin Registry에 등록되어 있음

### 플러그인 순서 강제 (Enforcing Plugin Ordering)

`enforce` 수정자로 플러그인 실행 순서를 제어함:

- `pre`: Vite 코어 플러그인보다 먼저 실행
- 기본값: Vite 코어 플러그인 이후에 실행
- `post`: Vite 빌드 플러그인 이후에 실행

```javascript
import image from '@rollup/plugin-image'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [
    {
      ...image(),
      enforce: 'pre',
    },
  ],
})
```

### 조건부 적용 (Conditional Application)

`apply` 속성을 사용하여 플러그인을 특정 컨텍스트(`'build'` 또는 `'serve'`)로 제한함:

```javascript
import typescript2 from 'rollup-plugin-typescript2'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [
    {
      ...typescript2(),
      apply: 'build',
    },
  ],
})
```

### 플러그인 빌드

커스텀 플러그인 작성에 대한 자세한 내용은 플러그인 API 가이드를 참조.

---

## 의존성 사전 번들링 (Dependency Pre-Bundling)

> 원문: https://vite.dev/guide/dep-pre-bundling

### 개요

- `vite`를 처음 실행하면 Vite가 사이트를 로컬로 로드하기 전에 프로젝트 의존성을 사전 번들링함

### 두 가지 주요 목적

**1. CommonJS 및 UMD 호환성**

- 비ESM 의존성을 개발 중에 네이티브 ESM 형식으로 변환함
- 지능적인 임포트 분석을 수행하여 동적으로 할당되는 내보내기(React 등)에서도 이름 있는 임포트가 올바르게 작동함

```js
// 기대대로 작동함
import React, { useState } from 'react'
```

**2. 성능 향상**

- 다수의 내부 모듈을 가진 ESM 의존성을 단일 모듈로 통합함
- 예시: lodash-es는 600개 이상의 내부 모듈을 가짐. 사전 번들링으로 600개 이상의 HTTP 요청 대신 하나로 줄임

### 자동 의존성 탐지

- Vite가 소스 코드를 자동으로 스캔하여 베어 임포트를 식별하고 사전 번들 진입점으로 사용함
- Rolldown을 사용하여 빠르게 처리함
- 서버 시작 후 새로운 의존성 임포트가 발견되면 사전 번들링을 다시 실행하고 페이지를 리로드함

### 모노레포와 링크된 의존성

- 같은 저장소의 링크된 패키지를 감지하여 번들된 의존성이 아닌 소스 코드로 취급함
- CommonJS 링크 의존성의 경우 `optimizeDeps.include`에 추가해야 함

```js
export default defineConfig({
  optimizeDeps: {
    include: ['linked-dep'],
  },
})
```

- 링크된 의존성을 수정한 후 개발 서버를 재시작할 때 `--force` 플래그를 사용함

### 커스터마이징 옵션

- `optimizeDeps.include`: 소스 코드에서 직접 탐지되지 않는 임포트나 대규모/CommonJS 의존성에 사용
- `optimizeDeps.exclude`: 작고 유효한 ESM 패키지를 제외할 때 사용
- `optimizeDeps.rolldownOptions`: Rolldown 추가 커스터마이징

### 캐싱 메커니즘

**파일 시스템 캐시**

- `node_modules/.vite`에 저장됨
- 다음 변경 시 재실행: 패키지 매니저 lockfile 변경, patch 폴더 수정, vite.config.js 업데이트, NODE_ENV 값 변경

**브라우저 캐시**

- 의존성은 `max-age=31536000,immutable` 헤더를 사용함
- 비활성화하려면 브라우저 개발자 도구를 사용하거나 `--force`로 재시작하고 `node_modules/.vite`를 삭제하여 강제로 재번들링함

---

## 정적 에셋 처리 (Static Asset Handling)

> 원문: https://vite.dev/guide/assets

### 에셋을 URL로 임포트

- 정적 에셋을 임포트하면 서빙될 때 해석된 공개 URL을 반환함
- 개발 중: 소스 구조를 반영하는 경로(예: `/src/img.png`)
- 프로덕션 빌드: 해시된 파일명(예: `/assets/img.2d8efhg.png`)

```javascript
import imgUrl from './img.png'
document.getElementById('hero-img').src = imgUrl
```

주요 동작:

- CSS `url()` 참조도 동일한 처리를 따름
- Vue SFC 템플릿이 에셋 참조를 자동으로 임포트로 변환함
- 일반적인 이미지, 미디어, 폰트 타입이 자동으로 감지됨
- 에셋 참조가 빌드 그래프에 포함되어 해시된 이름을 받음
- `assetsInlineLimit` 임계값 이하의 에셋은 base64 데이터 URL로 변환됨
- Git LFS 플레이스홀더는 인라이닝에서 자동 제외됨
- TypeScript에서는 적절한 모듈 인식을 위해 `vite/client` 포함이 필요함

SVG URL 처리 시 주의: JavaScript로 SVG URL을 수동 구성할 때 변수를 큰따옴표로 감싸야 함 - `url("${imgUrl}")`

### 명시적 에셋 임포트 수정자

**URL 임포트 (`?url`)**

자동 감지 목록에 없는 에셋의 명시적 URL 임포트에 사용. Houdini Paint Worklet 로딩 등에 유용함:

```javascript
import workletURL from 'extra-scalloped-border/worklet.js?url'
CSS.paintWorklet.addModule(workletURL)
```

**인라인 제어 (`?inline` / `?no-inline`)**

인라이닝 동작을 명시적으로 제어함:

```javascript
import imgUrl1 from './img.svg?no-inline'
import imgUrl2 from './img.png?inline'
```

**Raw 문자열 임포트 (`?raw`)**

파일 내용을 문자열로 임포트함:

```javascript
import shaderString from './shader.glsl?raw'
```

**워커 임포트 (`?worker` / `?sharedworker`)**

스크립트를 웹 워커로 임포트함:

```javascript
// 프로덕션에서 별도 청크
import Worker from './shader.js?worker'
const worker = new Worker()

// 공유 워커 변형
import SharedWorker from './shader.js?sharedworker'
const sharedWorker = new SharedWorker()

// base64로 인라인됨
import InlineWorker from './shader.js?worker&inline'
```

### public 디렉터리

`public` 디렉터리(publicDir 옵션으로 설정 가능)는 다음 조건의 파일을 서빙함:

- 소스 코드에서 참조되지 않는 파일
- 해싱 없이 정확한 파일명 유지가 필요한 파일
- 명시적 임포트가 불필요한 파일

이 디렉터리의 파일은 개발 중에는 루트 경로 `/`에서 서빙되고, 배포 루트에 변경 없이 복사됨. 공개 에셋은 절대 루트 경로를 사용하여 참조함. 예를 들어 `public/icon.png`은 소스 코드에서 `/icon.png`으로 참조해야 함.

특별한 `public` 디렉터리 보장이 필요하지 않다면 에셋 임포트를 권장함.

### 네이티브 ESM 패턴: `new URL(url, import.meta.url)`

정적 에셋 URL 해석을 위한 네이티브 ESM 기능을 지원함:

```javascript
const imgUrl = new URL('./img.png', import.meta.url).href
document.getElementById('hero-img').src = imgUrl
```

모던 브라우저에서 개발 중 별도의 처리가 불필요함. 템플릿 리터럴을 통한 동적 URL도 지원함:

```javascript
function getImageUrl(name) {
  return new URL(`./dir/${name}.png`, import.meta.url).href
}
```

**프로덕션 빌드 변환**

번들링 후에도 올바른 URL을 유지하기 위해 동적 패턴을 모듈 매핑으로 변환함:

```javascript
import __img0png from './dir/img0.png'
import __img1png from './dir/img1.png'

function getImageUrl(name) {
  const modules = {
    './dir/img0.png': __img0png,
    './dir/img1.png': __img1png,
  }
  return new URL(modules[`./dir/${name}.png`], import.meta.url).href
}
```

제한사항:

- 정적이 아닌 URL 문자열은 변환을 방지함
- 서버 사이드 렌더링과는 호환되지 않음 (브라우저와 Node.js의 `import.meta.url` 의미 차이)
- 서버 번들은 클라이언트 호스트 URL을 사전에 결정할 수 없음

---

## 프로덕션 빌드 (Building for Production)

> 원문: https://vite.dev/guide/build

### 핵심 개념

- `vite build` 명령으로 빌드 프로세스를 시작함
- 기본 진입점은 `<root>/index.html`이며, 정적 호스팅에 적합한 최적화된 번들을 생성함

### 브라우저 호환성

기본 타겟으로 모던 버전을 지정함:

- Chrome 111 이상, Edge 111 이상, Firefox 114 이상, Safari 16.4 이상

`build.target`을 통해 커스텀 타겟 지정 가능. 최소 ESM 지원은 Chrome 64 이상, Firefox 67 이상, Safari 11.1 이상, Edge 79 이상을 요구함.

Vite는 구문 변환만 수행하고 폴리필은 다루지 않음. 레거시 지원이 필요하면 Cloudflare의 폴리필 서비스 등 외부 솔루션을 사용함.

### 공개 기본 경로 (Public Base Path)

- `base` 설정 옵션이 에셋 경로를 자동으로 재작성함
- JavaScript 임포트, CSS 참조, HTML 에셋 링크에 적용됨
- 동적 URL 구성 시 `import.meta.env.BASE_URL` 변수를 사용할 수 있으며, 빌드 시 정적으로 대체됨
- `"./"` 또는 `""`를 사용한 상대 기본 경로는 상대 URL을 생성하지만 `import.meta` 지원이 필요함

### 커스터마이징 옵션

- `build.rolldownOptions`를 통해 하위 Rolldown 설정에 접근 가능
- `build.rolldownOptions.output.codeSplitting`으로 청킹 전략을 관리함

### 로드 에러 처리

- 동적 임포트 실패 시 `vite:preloadError` 이벤트가 발생함
- 오래된 사용자 에셋이 삭제된 청크를 참조하는 배포 시나리오를 해결함

### 개발 기능

- 감시 모드(Watch mode): `vite build --watch`로 파일 변경을 모니터링함
- 멀티 페이지 앱: 여러 HTML 파일이 진입점으로 사용 가능
- 라이브러리 모드: ES 및 UMD/CJS 형식으로 배포용 패키지를 생성함

### 라이브러리 모드 상세

- `build.lib`으로 설정하며 의존성을 외부화(externalize)함
- 권장 `package.json`은 module과 require 경로를 모두 내보냄
- CSS를 임포트하면 별도의 파일(예: `dist/my-lib.css`)로 자동 번들링됨

### 고급 기본 경로 옵션 (실험적)

- `experimental.renderBuiltUrl` 함수로 진입 HTML, 해시된 에셋, 공개 파일에 대한 별도 배포 경로를 설정 가능
- 서로 다른 캐싱 전략이 필요한 시나리오를 위한 기능임

---

## 정적 사이트 배포 (Deploying a Static Site)

> 원문: https://vite.dev/guide/static-deploy

### 개요

기본 `dist` 출력 디렉터리, npm 패키지 매니저 사용을 전제하며, 다음 npm 스크립트가 설정되어 있다고 가정함:

```json
{
  "scripts": {
    "build": "vite build",
    "preview": "vite preview"
  }
}
```

`vite preview`는 빌드를 로컬에서 미리보기 위한 것이며 프로덕션 서버로 사용하지 않음.

### 빌드 및 테스트

빌드 명령:

```bash
$ npm run build
```

출력은 기본적으로 `dist` 폴더로 가며 `build.outDir` 설정으로 변경 가능.

로컬 미리보기:

```bash
$ npm run preview
```

`http://localhost:4173`에서 로컬 정적 웹 서버를 시작함. 포트 커스터마이징:

```json
{
  "scripts": {
    "preview": "vite preview --port 8080"
  }
}
```

### GitHub Pages

1단계 - vite.config.js 업데이트:

- `https://<USERNAME>.github.io/` 또는 커스텀 도메인: `base`를 `'/'`로 설정(또는 생략)
- `https://<USERNAME>.github.io/<REPO>/`: `base`를 `'/<REPO>/'`로 설정

2단계 - GitHub Actions 활성화:

- 저장소 Settings -> Pages로 이동
- "Build and deployment"에서 Source 드롭다운에서 "GitHub Actions" 선택

3단계 - 워크플로 파일 생성 (`.github/workflows/deploy.yml`):

```yaml
name: Deploy static content to Pages

on:
  push:
    branches: ['main']
  workflow_dispatch:

permissions:
  contents: read
  pages: write
  id-token: write

concurrency:
  group: 'pages'
  cancel-in-progress: true

jobs:
  deploy:
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7
      - name: Set up Node
        uses: actions/setup-node@820762786026740c76f36085b0efc47a31fe5020 # v7
        with:
          node-version: lts/*
          cache: 'npm'
      - name: Install dependencies
        run: npm ci
      - name: Build
        run: npm run build
      - name: Setup Pages
        uses: actions/configure-pages@45bfe0192ca1faeb007ade9deae92b16b8254a0d # v6
      - name: Upload artifact
        uses: actions/upload-pages-artifact@fc324d3547104276b827a68afc52ff2a11cc49c9 # v5
        with:
          path: './dist'
      - name: Deploy to GitHub Pages
        id: deployment
        uses: actions/deploy-pages@cd2ce8fcbc39b97be8ca5fce6e763baed58fa128 # v5
```

### GitLab Pages 및 GitLab CI

1단계 - 기본 URL 설정:

- `https://<USERNAME or GROUP>.gitlab.io/`: `base` 생략 (기본값 `'/'`)
- `https://<USERNAME or GROUP>.gitlab.io/<REPO>/`: `base`를 `'/<REPO>/'`로 설정

2단계 - `.gitlab-ci.yml` 생성:

```yaml
image: node:lts
pages:
  stage: deploy
  cache:
    key:
      files:
        - package-lock.json
      prefix: npm
    paths:
      - node_modules/
  script:
    - npm install
    - npm run build
    - cp -a dist/. public/
  artifacts:
    paths:
      - public
  rules:
    - if: $CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH
```

### Netlify

CLI를 통한 방법:

```bash
npm install -g netlify-cli
netlify init
netlify deploy
netlify deploy --prod  # 프로덕션 배포
```

Git 통합을 통한 방법:

1. GitHub, GitLab, BitBucket, 또는 Azure DevOps에 코드를 푸시
2. netlify.com에서 프로젝트를 임포트
3. 브랜치, 출력 디렉터리(`dist`), 환경 변수를 선택
4. Deploy를 클릭
5. 브랜치에 대해 Preview Deployment가 생성되고, main 브랜치에 대해 Production Deployment가 생성됨

### Vercel

CLI를 통한 방법:

```bash
npm i -g vercel
vercel
```

Vercel이 자동으로 Vite를 감지하고 올바른 설정을 적용함.

Git 통합을 통한 방법:

1. GitHub, GitLab, 또는 Bitbucket에 코드를 푸시
2. vercel.com/new에서 임포트
3. Vercel이 Vite 설정을 자동 감지
4. 브랜치에 대해 Preview Deployment, main 브랜치에 대해 Production Deployment 생성

### Cloudflare Workers

플러그인 설치:

```bash
$ npm install --save-dev @cloudflare/vite-plugin
```

vite.config.js 업데이트:

```javascript
import { defineConfig } from 'vite'
import { cloudflare } from '@cloudflare/vite-plugin'

export default defineConfig({
  plugins: [cloudflare()],
})
```

wrangler.jsonc 생성/업데이트:

```jsonc
{
  "name": "my-vite-app",
}
```

`npx wrangler deploy`로 배포. Cloudflare 리소스와 안전하게 통신하는 백엔드 API를 추가할 수 있음.

### Cloudflare Pages

Git 통합을 통한 방법:

1. GitHub 또는 GitLab에 코드를 푸시
2. Cloudflare Dashboard -> Account Home -> Workers & Pages로 이동
3. Create a new Project -> Pages -> Git 선택
4. git 프로젝트를 선택하고 설정 시작
5. 프레임워크 프리셋을 선택하거나 커스텀 빌드 명령과 출력 디렉터리를 입력
6. 저장 후 배포
7. `https://<PROJECTNAME>.pages.dev/`로 배포됨

### Google Firebase

```bash
npm i -g firebase-tools
```

firebase.json 생성:

```json
{
  "hosting": {
    "public": "dist",
    "ignore": [],
    "rewrites": [
      {
        "source": "**",
        "destination": "/index.html"
      }
    ]
  }
}
```

.firebaserc 생성:

```json
{
  "projects": {
    "default": "<YOUR_FIREBASE_ID>"
  }
}
```

`npm run build` 실행 후 `firebase deploy`로 배포.

### Surge

```bash
npm i -g surge
npm run build
surge dist
surge dist yourdomain.com  # 커스텀 도메인
```

### Azure Static Web Apps

요구사항: Azure 계정 및 구독 키, GitHub에 푸시된 코드, VS Code의 SWA 확장.

1. VS Code에 SWA 확장 설치
2. 앱 루트로 이동
3. Static Web Apps 확장을 열고 로그인
4. '+'를 클릭하여 새 Static Web App 생성
5. 마법사를 따라 앱 이름 지정, 프레임워크 프리셋 선택, 루트(`/`)와 빌드 위치(`/dist`) 지정
6. 마법사가 `.github` 폴더에 GitHub action을 생성
7. GitHub action이 앱을 배포함

### Render

1. Render에서 계정 생성
2. Dashboard에서 New -> Static Site 클릭
3. GitHub/GitLab 계정을 연결하거나 공개 저장소 사용
4. 프로젝트 이름과 브랜치 지정
5. 빌드 명령: `npm install && npm run build`, 게시 디렉터리: `dist`
6. Create Static Site 클릭
7. `https://<PROJECTNAME>.onrender.com/`으로 배포됨

기본적으로 지정된 브랜치에 새 커밋이 있으면 자동 배포가 실행됨.

### 기타 플랫폼

- Flightcontrol: Flightcontrol Vite 문서를 참고
- xmit: xmit Vite 빠른 시작 가이드를 참고
- Zephyr Cloud: 빌드 프로세스에 직접 통합되며 글로벌 엣지 배포를 제공. 빌드하거나 개발 서버를 실행할 때 자동으로 배포됨
- EdgeOne Pages: EdgeOne Pages Vite 안내를 참고

### 추가 참고

이 가이드는 정적 배포를 다룸. Vite는 Node.js에서 실행하고 HTML로 사전 렌더링한 후 클라이언트에서 하이드레이션하는 프레임워크를 위한 서버 사이드 렌더링(SSR)도 지원함. 전통적인 서버 측 프레임워크와의 통합은 백엔드 통합 가이드를 참조.

---

## 환경 변수와 모드 (Env Variables and Modes)

> 원문: https://vite.dev/guide/env-and-mode

### 개요

- Vite는 `import.meta.env` 객체를 통해 상수를 노출함
- 개발 중에는 전역적으로 사용 가능하고, 빌드 시에는 효과적인 트리 셰이킹을 위해 정적으로 대체됨

### 내장 상수

- `import.meta.env.MODE`: 작동 모드 문자열
- `import.meta.env.BASE_URL`: `base` 설정 옵션으로 결정되는 기본 URL
- `import.meta.env.PROD`: 프로덕션 환경 여부를 나타내는 불리언
- `import.meta.env.DEV`: 개발 환경 여부를 나타내는 불리언 (PROD의 반대)
- `import.meta.env.SSR`: 서버 사이드 렌더링 감지를 위한 불리언

### 환경 변수

- `VITE_` 접두사가 붙은 변수는 번들링 후 클라이언트 측 코드에 자동으로 노출됨
- 이 접두사가 없는 변수는 클라이언트에서 숨겨져 데이터베이스 비밀번호 같은 민감한 정보를 보호함
- 모든 노출된 변수는 문자열로 파싱되므로 타입 변환이 필요함
- 민감한 데이터(API 키, 비밀번호)를 `VITE_` 변수에 저장하지 말 것
- 프로덕션 비밀 관리에는 백엔드 서비스를 사용함

### .env 파일 시스템

Vite는 dotenv을 사용하여 다음 우선순위 구조로 환경 파일을 로드함:

```
.env                    # 모든 컨텍스트
.env.local              # 모든 컨텍스트, git 무시됨
.env.[mode]             # 특정 모드 전용
.env.[mode].local       # 특정 모드 전용, git 무시됨
```

- 모드별 파일이 일반 파일보다 우선함
- 이미 존재하는 환경 변수가 최우선이며 덮어쓰여지지 않음

**변수 확장**

- `NEW_KEY=test$foo` 구문은 "test"로 확장됨
- `NEW_KEY=test\$foo`는 리터럴 "test$foo"가 됨

### 모드 (Modes)

- 기본적으로 `dev`는 development 모드, `build`는 production 모드로 실행됨
- `--mode` 플래그로 오버라이드 가능:

```bash
vite build --mode staging
```

**NODE_ENV와 Mode의 구분**

이 둘은 별개의 개념임. 하나의 명령에서 서로 다른 NODE_ENV와 mode 값을 가질 수 있음:

- `vite build`: NODE_ENV "production", Mode "production"
- `vite build --mode development`: NODE_ENV "production", Mode "development"
- `NODE_ENV=development vite build`: NODE_ENV "development", Mode "production"

### HTML 상수 대체

HTML 파일은 환경 변수 대체를 위한 `%CONST_NAME%` 구문을 지원함:

```html
<h1>Vite is running in %MODE%</h1>
<p>Using data from %VITE_API_URL%</p>
```

존재하지 않는 변수는 대체 없이 무시됨.

### TypeScript IntelliSense

`src` 디렉터리에 `vite-env.d.ts`를 생성하여 타입을 보강함:

```typescript
interface ImportMetaEnv {
  readonly VITE_APP_TITLE: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
```

이 파일에서 `import` 문을 사용하면 타입 보강이 깨지므로 주의할 것.

---

## 서버 사이드 렌더링 (Server-Side Rendering)

> 원문: https://vite.dev/guide/ssr

### 개요

- Vite의 SSR은 React, Preact, Vue, Svelte 등의 프론트엔드 프레임워크를 Node.js에서 실행하고, HTML로 사전 렌더링한 후 클라이언트에서 하이드레이션하는 것을 의미함
- 이 가이드는 라이브러리 및 프레임워크 작성자를 위한 Vite의 저수준 SSR API에 집중함
- 애플리케이션 개발에는 Awesome Vite SSR 섹션의 상위 레벨 SSR 플러그인과 도구를 먼저 살펴볼 것을 권장함

### 예제 프로젝트

`create-vite-extra`를 통한 참조 구현 제공: Vanilla, Vue, React, Preact, Svelte, Solid

### 소스 구조

일반적인 SSR 애플리케이션 구조:

```
- index.html
- server.js              # 메인 애플리케이션 서버
- src/
  - main.js              # 환경에 구애받지 않는(범용) 앱 코드를 내보냄
  - entry-client.js      # 앱을 DOM 요소에 마운트
  - entry-server.js      # 프레임워크의 SSR API를 사용하여 앱을 렌더링
```

`index.html`은 `entry-client.js`를 참조하고 서버 렌더링 마크업의 플레이스홀더를 포함해야 함:

```html
<div id="app"><!--ssr-outlet--></div>
<script type="module" src="/src/entry-client.js"></script>
```

`<!--ssr-outlet-->` 대신 정확하게 대체 가능한 어떤 플레이스홀더든 사용 가능함.

### 조건부 로직

서버 전용 코드를 실행하기 위한 환경 변수 사용:

```javascript
if (import.meta.env.SSR) {
  // ... 서버 전용 로직
}
```

빌드 시 정적으로 대체되어 사용되지 않는 분기의 트리 셰이킹이 가능함.

### 개발 서버 설정

완전한 서버 제어를 위해 Vite를 미들웨어 모드로 사용(Express 등):

```javascript
import fs from 'node:fs'
import path from 'node:path'
import express from 'express'
import { createServer as createViteServer } from 'vite'

async function createServer() {
  const app = express()

  const vite = await createViteServer({
    server: { middlewareMode: true },
    appType: 'custom'
  })

  app.use(vite.middlewares)

  app.use('*all', async (req, res) => {
    // index.html 서빙 - 다음에서 다룸
  })

  app.listen(5173)
}

createServer()
```

`vite` 인스턴스는 `ViteDevServer`이며 `vite.middlewares`는 Node.js 프레임워크와 호환되는 Connect 인스턴스임.

### 핸들러 구현

```javascript
app.use('*all', async (req, res, next) => {
  const url = req.originalUrl

  try {
    // 1. index.html 읽기
    let template = fs.readFileSync(
      path.resolve(import.meta.dirname, 'index.html'),
      'utf-8',
    )

    // 2. Vite HTML 변환 적용. Vite HMR 클라이언트를 주입하고,
    //    @vitejs/plugin-react의 글로벌 프리앰블 등
    //    Vite 플러그인의 HTML 변환도 적용함
    template = await vite.transformIndexHtml(url, template)

    // 3. 서버 진입점 로드. ssrLoadModule은 ESM 소스 코드를
    //    Node.js에서 사용할 수 있도록 자동 변환함.
    //    번들링이 불필요하고 HMR과 유사한 효율적 무효화를 제공함
    const { render } = await vite.ssrLoadModule('/src/entry-server.js')

    // 4. 앱 HTML 렌더링. entry-server.js의 내보낸 `render` 함수가
    //    적절한 프레임워크 SSR API를 호출한다고 가정함.
    //    예: ReactDOMServer.renderToString()
    const appHtml = await render(url)

    // 5. 앱이 렌더링한 HTML을 템플릿에 주입
    const html = template.replace(`<!--ssr-outlet-->`, () => appHtml)

    // 6. 렌더링된 HTML을 반환
    res.status(200).set({ 'Content-Type': 'text/html' }).end(html)
  } catch (e) {
    // 에러가 잡히면 Vite가 스택 트레이스를 수정하여
    // 실제 소스 코드에 매핑되도록 함
    vite.ssrFixStacktrace(e)
    next(e)
  }
})
```

`package.json`의 `dev` 스크립트 업데이트:

```json
{
  "scripts": {
    "dev": "node server"
  }
}
```

### 프로덕션 빌드

두 개의 빌드가 필요함:

1. 표준 클라이언트 빌드
2. `import()`로 로드 가능한 SSR 빌드

스크립트 설정:

```json
{
  "scripts": {
    "dev": "node server",
    "build:client": "vite build --outDir dist/client",
    "build:server": "vite build --outDir dist/server --ssr src/entry-server.js"
  }
}
```

`--ssr` 플래그는 SSR 빌드를 나타내며 진입점을 지정함.

프로덕션용 `server.js` 업데이트 시 `process.env.NODE_ENV`를 확인하여:

- `dist/client/index.html`을 템플릿으로 읽음 (올바른 에셋 링크 포함)
- `vite.ssrLoadModule()` 대신 `import('./dist/server/entry-server.js')`를 사용
- Vite 개발 서버 생성을 개발 전용 조건 뒤로 이동
- `dist/client`에 대한 정적 파일 서빙을 추가

### 프리로드 디렉티브 생성

`--ssrManifest` 플래그를 사용함:

```
"build:client": "vite build --outDir dist/client --ssrManifest"
```

`dist/client/.vite/ssr-manifest.json`을 생성하여 모듈 ID를 청크와 에셋에 매핑함.

`@vitejs/plugin-vue` 같은 프레임워크는 사용된 컴포넌트 모듈 ID를 자동으로 등록함:

```javascript
const ctx = {}
const html = await vueServerRenderer.renderToString(app, ctx)
// ctx.modules는 이제 렌더링 중 사용된 모듈 ID의 Set임
```

매니페스트를 render 함수에 전달하여 비동기 라우트 의존성에 대한 프리로드 디렉티브를 생성하고 103 Early Hints 응답을 지원함.

### 사전 렌더링 / SSG

동일한 프로덕션 SSR 로직을 사용하여 알려진 라우트를 정적 HTML로 사전 렌더링함. 이 형태의 정적 사이트 생성(SSG)은 사전 생성된 HTML을 서빙하여 성능을 향상시킴.

### SSR 외부화 (SSR Externals)

- 의존성은 SSR 중에 기본적으로 외부화되어 개발과 빌드 속도를 향상시킴
- Vite 파이프라인 변환이 필요한 의존성은 `ssr.noExternal`에 추가함
- 링크된 의존성은 HMR을 활용하기 위해 기본적으로 외부화를 피함. 이를 비활성화하려면 `ssr.external`에 추가함

별칭(alias)이 있는 패키지의 경우, 실제 `node_modules` 패키지를 별칭으로 지정함. Yarn과 pnpm 모두 `npm:` 접두사를 통한 별칭을 지원함.

### SSR 전용 플러그인 로직

Vue와 Svelte 같은 프레임워크는 클라이언트와 SSR에서 컴포넌트를 다르게 컴파일함. Vite는 다음 플러그인 훅의 `options` 객체에 `ssr` 속성을 전달함:

- `resolveId`
- `load`
- `transform`

```javascript
export function mySSRPlugin() {
  return {
    name: 'my-ssr',
    transform(code, id, options) {
      if (options?.ssr) {
        // SSR 전용 변환 수행...
      }
    },
  }
}
```

### SSR 타겟

- 기본 타겟은 Node.js이지만 Web Worker로 설정 가능
- `ssr.target`을 `'webworker'`로 설정하여 패키지 진입 해석을 조정함

### SSR 번들

- `webworker` 런타임 같은 단일 파일 번들의 경우 `ssr.noExternal`을 `true`로 설정
- 모든 의존성을 `noExternal`로 취급하며 Node.js 빌트인이 임포트되면 에러를 던짐

### SSR 해석 조건

- 패키지 해석은 기본적으로 `resolve.conditions`를 사용
- `ssr.resolve.conditions`와 `ssr.resolve.externalConditions`로 커스터마이징 가능

### Vite CLI

- `vite dev`와 `vite preview` 명령이 SSR 애플리케이션을 지원함
- `configureServer`로 개발에, `configurePreviewServer`로 프리뷰에 SSR 미들웨어를 추가함
- SSR 미들웨어가 Vite의 미들웨어 이후에 실행되도록 포스트 훅을 사용해야 함

---

## 백엔드 통합 (Backend Integration)

> 원문: https://vite.dev/guide/backend-integration

### 개요

전통적인 백엔드 서버(Rails, Laravel 등)가 HTML을 서빙하면서 에셋 관리에 Vite를 사용하는 통합 방법을 안내함.

### 1. Vite 설정

진입점과 빌드 매니페스트를 활성화하는 설정:

```javascript
export default defineConfig({
  input: '/path/to/main.js',
  server: {
    cors: {
      origin: 'http://my-backend.example.com',
    },
  },
  build: {
    manifest: true,
  },
})
```

비활성화하지 않았다면 앱 진입점에서 modulepreload 폴리필을 임포트해야 함.

### 2. 개발 통합

개발 중에는 HTML 템플릿에 두 개의 스크립트 태그를 주입함:

```html
<script type="module" src="http://localhost:5173/@vite/client"></script>
<script type="module" src="http://localhost:5173/main.js"></script>
```

정적 에셋 요청은 서버 프록시 설정이 필요하거나 적절한 해석을 위해 `server.origin`을 설정해야 함.

### 3. 프로덕션 매니페스트

빌드 후 `.vite/manifest.json` 파일이 생성되며, 소스 파일을 빌드 출력에 매핑하는 레코드를 포함함. 각 항목은 `ManifestChunk` 인터페이스를 따르며 `file`, `css`, `imports`, `dynamicImports` 속성과 `isEntry`, `isDynamicEntry` 같은 플래그를 가짐.

### 4. HTML 템플릿 렌더링

백엔드에서 다음 순서로 태그를 포함해야 함:

1. 진입점 CSS의 스타일시트 링크
2. 임포트된 청크의 스타일시트
3. 진입점 파일의 스크립트 또는 스타일시트 태그
4. 임포트된 청크의 선택적 modulepreload 링크

### React 전용 설정

`@vitejs/plugin-react`를 사용하는 React 프로젝트의 경우, 표준 Vite 스크립트 앞에 `RefreshRuntime`을 주입하는 추가 스크립트 블록이 필요함.

---

## 문제 해결 (Troubleshooting)

> 원문: https://vite.dev/guide/troubleshooting

### CLI 문제

**Windows 경로의 앰퍼샌드(&) 문자**

- `&` 기호가 Windows 프로젝트 경로에 있으면 npm이 실패함
- 해결: 대체 패키지 매니저(pnpm, yarn) 사용 또는 경로에서 `&` 제거

### 설정 문제

**ESM 전용 패키지 에러**

- `require`로 ESM 패키지를 임포트할 때 Node.js 22 이하에서는 기본적으로 차단됨
- 권장 해결:
  - package.json에 `"type": "module"` 추가
  - 설정 파일 확장자를 `.mjs`/`.mts`로 변경
  - Node.js 22 이상으로 업그레이드 고려

### 개발 서버 문제 해결

**멈춘 요청 (Linux)**

- 파일 디스크립터와 inotify 한도 초과 가능
- 해결:
  - `ulimit -Sn 10000`으로 파일 디스크립터 증가
  - `sysctl` 명령으로 inotify 한도 조정
  - systemd 설정 파일 수정
  - Ubuntu에서 `/etc/security/limits.conf` 수정

**ENOSPC 에러**

- 파일 감시자 한도 초과 시 발생
- 해결:
  - `fs.inotify.max_user_watches`를 524288로 증가
  - `server.watch.ignored`로 디렉터리 제외
  - `server.watch.usePolling`으로 폴링 활성화 (CPU 사용 증가)

**자체 서명 SSL 인증서 문제**

- Chrome이 자체 서명 인증서로 캐싱 디렉티브를 무시함
- 신뢰할 수 있는 인증서를 설치하거나 적절한 SSL 설정을 사용함

**431 Request Header Fields Too Large**

- Node.js가 CVE-2018-12121 방지를 위해 헤더를 제한함
- 헤더 크기를 줄이거나 `--max-http-header-size` 플래그를 사용

**VS Code Dev Containers**

- IPv6 포트 포워딩 제한을 지원하기 위해 `server.host`를 `127.0.0.1`로 설정함

### HMR 문제

**파일 변경이 HMR을 트리거하지 않음**

- 임포트 경로가 실제 파일 케이스와 일치하는지 확인 (대소문자 구분 시스템)
- WSL2에서는 `server.watch` 설정을 확인

**HMR 대신 전체 리로드**

- 순환 의존성이 있을 때 발생함
- `vite --debug hmr`으로 문제가 되는 루프를 식별

### 빌드 문제

**file:// 프로토콜에서 CORS 에러**

- 빌드된 파일은 `file://` 프로토콜로 실행되지 않음
- `npx vite preview`를 사용하여 HTTP로 서빙함

**대소문자 구분 에러**

- 대소문자를 구분하지 않는 파일시스템(Windows/macOS)에서 개발하고 대소문자를 구분하는 파일시스템(Linux)에서 빌드할 때 모듈 해석 실패가 발생함
- 임포트 대소문자를 확인할 것

**실패한 동적 임포트 에러**

세 가지 주요 원인:

1. 버전 스큐: 캐시된 HTML이 배포 후 삭제된 청크 이름을 참조함. 해결: 이전 청크를 임시로 유지, 서비스 워커 구현, 우아한 폴백 처리 추가
2. 불안정한 네트워크: 불안정한 연결이 요청을 실패시킬 수 있고 브라우저가 재시도를 방지함
3. 브라우저 확장 프로그램: 광고 차단기가 요청을 차단할 수 있음. `build.rolldownOptions.output.chunkFileNames`로 청크 파일명을 변경하여 차단 패턴을 회피함

### 최적화된 의존성 문제

**npm link로 인한 오래된 사전 번들링 deps**

- Vite는 오버라이드를 자동으로 감지하지만 `npm link`는 감지하지 못함
- `vite --force`로 재최적화하거나, npm/pnpm/yarn 오버라이드를 사용하는 것을 권장함

### 성능 분석

**병목 현상 식별**

- `vite --profile --open` (개발) 또는 `vite build --profile` (빌드) 실행
- `p`를 눌러 검사를 중단하고 `q`를 눌러 종료
- 생성된 `vite-profile-0.cpuprofile`을 speedscope.app에 업로드
- 플러그인 분석을 위해 `vite-plugin-inspect`를 설치함

### 기타 문제

- Node.js 모듈의 브라우저 사용: Vite는 Node 모듈을 자동으로 폴리필하지 않음. 브라우저 코드에서 사용을 피하거나 수동 폴리필을 추가할 것
- 엄격 모드 에러: Vite는 ESM 엄격 모드만 사용함. 비엄격 코드가 실패하면 `patch-package`, `yarn patch`, 또는 `pnpm patch`를 사용하여 의존성을 수정
- 브라우저 확장 프로그램 간섭: 확장 프로그램이 Vite 클라이언트 요청을 차단하여 흰 화면을 야기할 수 있음. 확장 프로그램을 비활성화하여 테스트
- Windows 크로스 드라이브 링크: `subst`나 `mklink`를 통한 드라이브 간 심볼릭 링크가 실패를 야기할 수 있음
- CJS 기본 임포트 혼동: 기본 임포트는 `module.exports` 객체를 반환하며 `module.exports.default`가 아님. 예상되는 내보내기 구조를 확인할 것

---

## 성능 (Performance)

> 원문: https://vite.dev/guide/performance

### 개요

- Vite는 기본적으로 좋은 성능을 유지하지만 프로젝트가 성장하면서 문제가 나타날 수 있음
- 느린 서버 시작, 느린 페이지 로드, 느린 빌드 등 일반적인 문제를 다룸

### 브라우저 설정 검토

- 브라우저 확장 프로그램이 요청을 방해하여 대규모 애플리케이션의 시작/리로드 시간을 늦출 수 있음
- 권장:
  - 확장 프로그램 없는 개발 전용 프로필 생성
  - Vite 개발 서버 작업 시 시크릿 모드 사용
  - 브라우저 개발자 도구에서 "Disable Cache"가 활성화되어 있지 않은지 확인. 이는 시작 및 전체 페이지 리로드 시간에 크게 영향을 미침
- Vite 개발 서버는 사전 번들링된 의존성에 대한 강력한 캐싱과 소스 코드에 대한 빠른 304 응답을 구현함

### 설정된 Vite 플러그인 감사

커뮤니티 플러그인 성능은 Vite의 제어 범위 밖임. 주요 고려사항:

1. 대규모 의존성의 동적 임포트: 무거운 의존성을 필요할 때만 임포트하여 Node.js 시작 시간을 줄임
2. 훅 최적화: `buildStart`, `config`, `configResolved` 훅은 긴 작업을 피해야 함. 개발 서버 시작을 차단하기 때문임
3. 변환 작업: `resolveId`, `load`, `transform` 훅을 최적화함. 전체 변환을 수행하기 전에 파일 조건을 확인할 것

`vite --debug plugin-transform` 또는 `vite-plugin-inspect` 도구로 변환을 프로파일링 가능.

### 프로파일링

`vite --profile`을 실행하고 사이트를 방문한 후 터미널에서 `p + enter`를 눌러 `.cpuprofile`을 기록함. speedscope 같은 도구로 병목을 분석함. 프로필을 Discord 채팅을 통해 Vite 팀과 공유 가능.

### 해석 작업 줄이기 (Reduce Resolve Operations)

- 경로 해석이 비용이 클 수 있음
- Vite의 기본 `resolve.extensions`는 `['.mjs', '.js', '.mts', '.ts', '.jsx', '.tsx', '.json']`임
- `'./Component'`를 임포트할 때 `Component.jsx` 파일의 경우, Vite가 일치를 찾을 때까지 각 확장자를 순차적으로 확인하여 6번의 파일시스템 검사가 필요함

해결:

- 명시적 임포트 경로 사용: `import './Component.jsx'`
- `resolve.extensions`를 좁혀 파일시스템 검사를 줄임
- 플러그인 작성자는 `this.resolve`를 필요할 때만 호출할 것

TypeScript 최적화: `tsconfig.json`에서 `"moduleResolution": "bundler"`와 `"allowImportingTsExtensions": true`를 활성화하여 `.ts`와 `.tsx` 확장자를 직접 사용 가능.

### 배럴 파일 피하기 (Avoid Barrel Files)

배럴 파일은 같은 디렉터리의 여러 파일에서 API를 재내보내기함:

```javascript
// src/utils/index.js
export * from './color.js'
export * from './dom.js'
export * from './slash.js'
```

`import { slash } from './utils'`로 임포트하면 불필요한 파일과 잠재적 부작용을 포함하여 배럴 파일의 모든 내용을 로드함. 이는 초기 페이지 로드를 느리게 함.

해결: 배럴을 통하지 않고 직접 API를 임포트함 - `import { slash } from './utils/slash.js'`

### 자주 사용하는 파일 사전 준비 (Warm Up Frequently Used Files)

- Vite 개발 서버는 요청 시 파일을 변환하여 빠른 시작을 가능하게 하지만, 일부 파일의 변환이 오래 걸리면 임포트 워터폴이 발생할 수 있음
- 의존성 체인 예시: `main.js -> BigComponent.vue -> big-utils.js -> large-data.json`
- 파일 관계는 변환 후에만 발견되므로 `BigComponent.vue`의 지연이 하위로 전파됨

해결: `server.warmup`을 사용하여 자주 사용하는 파일을 사전 변환함:

```javascript
// vite.config.js
export default defineConfig({
  server: {
    warmup: {
      clientFiles: [
        './src/components/BigComponent.vue',
        './src/utils/big-utils.js',
      ],
    },
  },
})
```

`vite --debug transform` 로그로 후보를 식별함:

```
vite:transform 28.72ms /@vite/client +1ms
vite:transform 62.95ms /src/components/BigComponent.vue +1ms
vite:transform 102.54ms /src/utils/big-utils.js +1ms
```

시작 과부하를 피하기 위해 자주 사용하는 파일만 사전 준비함. `--open` 또는 `server.open` 사용도 앱 진입점을 자동으로 사전 준비하여 성능 이점을 제공함.

### 더 가볍거나 네이티브 도구 사용 (Use Lesser or Native Tooling)

코드베이스가 성장할수록 소스 파일 처리 작업을 줄여 속도를 유지함.

**더 적은 작업 하기:**

- Sass/Less/Stylus 대신 CSS 선호 (PostCSS/Lightning CSS가 중첩을 처리함)
- SVG를 UI 프레임워크 컴포넌트로 변환하는 대신 문자열이나 URL로 임포트

**네이티브 도구 사용:**

- Vite가 네이티브 도구를 사용하지만 일부 기능은 호환성을 위해 비네이티브 도구를 기본으로 함
- 대규모 애플리케이션에서는 LightningCSS 지원 같은 실험적 대안을 탐색하는 것을 고려함

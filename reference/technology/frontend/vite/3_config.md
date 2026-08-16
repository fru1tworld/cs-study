# Vite 설정

## 설정 파일

> 원문: https://vite.dev/config

### 설정 파일 해석

- Vite는 프로젝트 루트의 `vite.config.js` 파일을 자동으로 해석함
- Node.js가 ESM으로 인식하는 경우(`.mjs` 확장자 또는 `package.json`에 `"type": "module"`) ES 모듈 구문을 지원함
- `--config` CLI 옵션으로 설정 파일을 명시적으로 지정할 수 있음

```bash
vite --config my-config.js
```

### 설정 로딩 방식

- 기본적으로 Rolldown을 사용해 설정 파일을 임시 파일로 번들한 뒤 로딩함
- `--configLoader native` 옵션으로 네이티브 런타임을 사용할 수 있음 (Node 22.18 이상에서 TypeScript 지원)
- 향후 메이저 버전에서 네이티브 로더가 기본값이 될 예정임

### 설정 인텔리센스

- JSDoc 타입 힌트 사용

```javascript
/** @type {import('vite').UserConfig} */
export default {
  // ...
}
```

- `defineConfig` 헬퍼 사용

```javascript
import { defineConfig } from 'vite'

export default defineConfig({
  // ...
})
```

- TypeScript 설정 파일에서 `satisfies` 사용

```typescript
import type { UserConfig } from 'vite'

export default {
  // ...
} satisfies UserConfig
```

### 조건부 설정

- `command`, `mode`, `isSsrBuild`, `isPreview`에 따라 조건부로 옵션을 결정할 수 있음
- `command` 값: 개발 시 `serve`, 프로덕션 빌드 시 `build`
- `isSsrBuild`, `isPreview`는 일부 도구에서 `undefined`를 전달할 수 있으므로 `true`/`false`와 명시적으로 비교해야 함

```javascript
export default defineConfig(({ command, mode, isSsrBuild, isPreview }) => {
  if (command === 'serve') {
    return {
      // dev specific config
    }
  } else {
    // command === 'build'
    return {
      // build specific config
    }
  }
})
```

### 비동기 설정

```javascript
export default defineConfig(async ({ command, mode }) => {
  const data = await asyncFunction()
  return {
    // vite config
  }
})
```

### 설정에서 환경 변수 사용

- `.env` 파일의 환경 변수는 설정 평가 시점에 자동으로 사용 가능하지 않음
- `process.env`에 이미 있는 변수만 접근 가능함
- 애플리케이션 코드에서는 설정 해석 후 `import.meta.env`를 통해 사용 가능함 (기본 `VITE_` 접두사 필터 적용)
- `loadEnv` 헬퍼로 `.env` 파일을 수동 로딩할 수 있음

```javascript
import { defineConfig, loadEnv } from 'vite'

export default defineConfig(({ mode }) => {
  // Load env file based on `mode` in the current working directory.
  // Set the third parameter to '' to load all env regardless of the
  // `VITE_` prefix.
  const env = loadEnv(mode, process.cwd(), '')
  return {
    define: {
      // Provide an explicit app-level constant derived from an env var.
      __APP_ENV__: JSON.stringify(env.APP_ENV),
    },
    // Example: use an env var to set the dev server port conditionally.
    server: {
      port: env.APP_PORT ? Number(env.APP_PORT) : 5173,
    },
  }
})
```

### VS Code 디버깅

- 네이티브 설정 로더 사용 시 `vite --configLoader native`로 실행하면 브레이크포인트가 원본 소스에 매핑됨
- 번들 로더 사용 시 `.vscode/settings.json`에 다음을 추가함

```json
{
  "debug.javascript.terminalOptions": {
    "resolveSourceMapLocations": [
      "${workspaceFolder}/**",
      "!**/node_modules/**",
      "**/node_modules/.vite-temp/**"
    ]
  }
}
```

- 실행 설정 예시 (`.vscode/launch.json`)

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "node",
      "request": "launch",
      "name": "Vite",
      "runtimeExecutable": "npm",
      "runtimeArgs": ["exec", "vite", "--configLoader", "bundle"],
      "console": "integratedTerminal",
      "sourceMaps": true,
      "resolveSourceMapLocations": [
        "${workspaceFolder}/**",
        "!**/node_modules/**",
        "**/node_modules/.vite-temp/**"
      ]
    }
  ]
}
```

---

## 공유 옵션

> 원문: https://vite.dev/config/shared-options

### root

- **타입:** `string`
- **기본값:** `process.cwd()`
- 프로젝트 루트 디렉터리 (`index.html` 위치)
- 절대 경로 또는 상대 경로를 받음

### base

- **타입:** `string`
- **기본값:** `/`
- 개발 및 프로덕션에서 서빙할 때의 공개 기본 경로
- 절대 URL 경로명 (예: `/foo/`), 전체 URL (예: `https://bar.com/foo/`), 빈 문자열, `./` 등을 사용할 수 있음

### mode

- **타입:** `string`
- **기본값:** serve 시 `'development'`, build 시 `'production'`
- `--mode` CLI 옵션으로 오버라이드할 수 있음

### input

- **타입:** `string | string[] | { [entryAlias: string]: string }`
- 애플리케이션의 진입점으로 프로젝트 루트 기준 상대 경로로 해석됨
- `build.rolldownOptions.input`, `build.lib.entry`, `build.ssr`, `optimizeDeps.entries`의 기본값으로 사용됨

```javascript
import { defineConfig } from 'vite'

export default defineConfig({
  input: 'src/main.ts',
})
```

### define

- **타입:** `Record<string, any>`
- 전역 상수 치환을 정의함
- 값은 JSON 직렬화 가능하거나 단일 식별자여야 함
- 문자열이 아닌 값은 `JSON.stringify`로 변환됨

```javascript
export default defineConfig({
  define: {
    __APP_VERSION__: JSON.stringify('v1.0.0'),
    __API_URL__: 'window.__backend_api_url',
  },
})
```

```typescript
// vite-env.d.ts
declare const __APP_VERSION__: string
```

### plugins

- **타입:** `(Plugin | Plugin[] | Promise<Plugin | Plugin[]>)[]`
- 사용할 플러그인 배열
- falsy 플러그인은 무시되고 배열은 평탄화됨
- Promise는 실행 전에 해결됨

### publicDir

- **타입:** `string | false`
- **기본값:** `"public"`
- 순수 정적 에셋을 제공하는 디렉터리
- 개발 시 `/`에서 서빙되고 빌드 시 `outDir` 루트에 복사됨
- `false`로 설정하면 비활성화됨

### cacheDir

- **타입:** `string`
- **기본값:** `"node_modules/.vite"`
- 사전 번들된 의존성 등 캐시 파일 디렉터리
- `package.json`이 없으면 `.vite`가 기본값임

### resolve.alias

- **타입:** `Record<string, string> | Array<{ find: string | RegExp, replacement: string }>`
- `import` 또는 `require` 구문에 대한 별칭 설정
- 순서가 중요하며, 첫 번째 규칙이 먼저 적용됨
- 파일 시스템 경로에는 항상 절대 경로를 사용해야 함

```javascript
// 객체 형식
resolve: {
  alias: {
    utils: '../../../utils',
    'batman-1.0.0': './joker-1.5.0'
  }
}
```

```javascript
// 배열 형식
resolve: {
  alias: [
    { find: 'utils', replacement: '../../../utils' },
    { find: 'batman-1.0.0', replacement: './joker-1.5.0' },
  ]
}
```

```javascript
// 정규식 패턴
{ find:/^(.*)\.js$/, replacement: '$1.alias' }
```

### resolve.dedupe

- **타입:** `string[]`
- 나열된 의존성을 프로젝트 루트에서 동일한 복사본으로 강제 해석함
- 모노레포나 링크된 패키지에서 중복을 해결할 때 사용함

### resolve.conditions

- **타입:** `string[]`
- **기본값:** `['module', 'browser', 'development|production']`
- 패키지의 조건부 내보내기를 해석할 때 허용되는 추가 조건
- `development|production`은 `NODE_ENV`에 따라 대체됨
- 스타일 임포트에는 `style` 조건이 적용됨

### resolve.mainFields

- **타입:** `string[]`
- **기본값:** `['browser', 'module', 'jsnext:main', 'jsnext']`
- 진입점 해석 시 시도할 `package.json` 필드 목록
- 조건부 내보내기보다 낮은 우선순위를 가짐

### resolve.extensions

- **타입:** `string[]`
- **기본값:** `['.mjs', '.js', '.mts', '.ts', '.jsx', '.tsx', '.json']`
- 확장자를 생략한 임포트에서 시도할 파일 확장자
- 커스텀 타입의 확장자 생략은 권장하지 않음

### resolve.preserveSymlinks

- **타입:** `boolean`
- **기본값:** `false`
- 심볼릭 링크를 따라간 실제 경로가 아닌 원래 파일 경로로 파일 ID를 결정함

### resolve.tsconfigPaths

- **타입:** `boolean`
- **기본값:** `false`
- tsconfig 경로 해석을 활성화함
- `tsconfig.json`의 `paths` 옵션으로 임포트를 해석함
- JS가 아닌 파일은 명시적으로 나열해야 함

```json
{
  "include": ["src", "src/**/*.css", "src/**/*.scss"]
}
```

### html.cspNonce

- **타입:** `string`
- script/style 태그에 대한 nonce 값 플레이스홀더로, nonce가 포함된 메타 태그를 생성함

### html.additionalAssetSources

- **타입:** `Record<string, HtmlAssetSource>`
- 에셋 소스로 취급할 추가 HTML 요소와 속성을 정의함
- 커스텀 웹 컴포넌트에 사용함

```typescript
interface HtmlAssetSource {
  srcAttributes?: string[]
  srcsetAttributes?: string[]
  filter?: (data: {
    key: string
    value: string
    attributes: Record<string, string>
  }) => boolean
}
```

```javascript
export default defineConfig({
  html: {
    additionalAssetSources: {
      'html-import': { srcAttributes: ['src'] },
      img: { srcAttributes: ['data-src-dark', 'data-src-light'] },
      'my-picture': { srcsetAttributes: ['data-srcset'] },
      'my-component': {
        srcAttributes: ['asset'],
        filter: ({ attributes }) => attributes.type === 'image',
      },
    },
  },
})
```

### css.modules

- **타입:** 설정 객체
- CSS 모듈 동작을 설정하며, postcss-modules에 전달됨
- `getJSON`, `scopeBehaviour`, `globalModulePaths`, `exportGlobals`, `generateScopedName`, `hashPrefix`, `localsConvention` 등의 옵션을 포함함

### css.postcss

- **타입:** `string | (postcss.ProcessOptions & { plugins?: postcss.AcceptedPlugin[] })`
- 인라인 PostCSS 설정 또는 PostCSS 설정 파일을 검색할 커스텀 디렉터리 (기본값: 프로젝트 루트)

### css.preprocessorOptions

- **타입:** `Record<string, object>`
- CSS 전처리기에 전달할 옵션
- 키는 파일 확장자이며 `sass`/`scss`, `less`, `styl`/`stylus`를 지원함

```javascript
export default defineConfig({
  css: {
    preprocessorOptions: {
      less: {
        math: 'parens-division',
      },
      styl: {
        define: {
          $specialColor: new stylus.nodes.RGBA(51, 197, 255, 1),
        },
      },
      scss: {
        importers: [
          // ...
        ],
      },
    },
  },
})
```

### css.preprocessorOptions[extension].additionalData

- **타입:** `string | ((source: string, filename: string) => (string | { content: string; map?: SourceMap }))`
- 각 스타일 콘텐츠에 추가 코드를 주입함
- 최종 번들에서 스타일이 중복될 수 있으니 주의해야 함

```javascript
export default defineConfig({
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `$injectedColor: orange;`,
      },
    },
  },
})
```

### css.preprocessorMaxWorkers

- **타입:** `number | true`
- **기본값:** `true`
- CSS 전처리기의 최대 스레드 수
- `true`는 CPU 수 - 1을 사용함
- `0`은 메인 스레드에서 실행함

### css.devSourcemap

- **타입:** `boolean`
- **기본값:** `false`
- **상태:** 실험적
- 개발 중 소스맵을 활성화함

### css.transformer

- **타입:** `'postcss' | 'lightningcss'`
- **기본값:** `'postcss'`
- **상태:** 실험적
- CSS 처리 엔진을 선택함

### css.lightningcss

- **타입:** 설정 객체
- **상태:** 실험적
- Lightning CSS를 설정함
- `targets`, `include`, `exclude`, `drafts`, `nonStandard`, `pseudoClasses`, `unusedSymbols`, `cssModules` 등의 변환 옵션을 포함함

### json.namedExports

- **타입:** `boolean`
- **기본값:** `true`
- `.json` 파일에서 named import를 활성화함

### json.stringify

- **타입:** `boolean | 'auto'`
- **기본값:** `'auto'`
- JSON을 `JSON.parse()`로 변환하여 성능을 향상시킴
- `'auto'`는 10kB보다 큰 데이터를 문자열화함

### oxc

- **타입:** `OxcOptions | false`
- Oxc Transformer를 설정함
- 주로 JSX의 `runtime`, `pragma`, `pragmaFrag`를 커스터마이즈할 때 사용함

```javascript
export default defineConfig({
  oxc: {
    jsx: {
      runtime: 'classic',
      pragma: 'h',
      pragmaFrag: 'Fragment',
    },
  },
})
```

```javascript
// JSX 주입
export default defineConfig({
  oxc: {
    jsxInject: `import React from 'react'`,
  },
})
```

### esbuild

- **타입:** `ESBuildOptions | false`
- **상태:** 더 이상 사용하지 않음 (deprecated)
- 내부적으로 `oxc` 옵션으로 변환됨. `oxc`를 대신 사용할 것

### assetsInclude

- **타입:** `string | RegExp | (string | RegExp)[]`
- 파일을 정적 에셋으로 취급하는 추가 picomatch 패턴
- 플러그인 변환에서 제외되며, 임포트 시 URL 문자열을 반환함

```javascript
export default defineConfig({
  assetsInclude: ['**/*.gltf'],
})
```

### logLevel

- **타입:** `'info' | 'warn' | 'error' | 'silent'`
- **기본값:** `'info'`
- 콘솔 출력의 상세 수준을 조정함

### customLogger

- **타입:** Logger 인터페이스 객체
- `info`, `warn`, `warnOnce`, `error`, `clearScreen`, `hasErrorLogged` 메서드와 `hasWarned` 속성을 가진 커스텀 로거

```typescript
import { createLogger, defineConfig } from 'vite'

const logger = createLogger()
const loggerWarn = logger.warn

logger.warn = (msg, options) => {
  if (msg.includes('vite:css') && msg.includes(' is empty')) return
  loggerWarn(msg, options)
}

export default defineConfig({
  customLogger: logger,
})
```

### clearScreen

- **타입:** `boolean`
- **기본값:** `true`
- 로그 메시지 출력 시 Vite가 터미널 화면을 지우는 것을 방지함
- `--clearScreen false`로 비활성화할 수 있음

### envDir

- **타입:** `string | false`
- **기본값:** `root`
- `.env` 파일을 로딩할 디렉터리
- 절대 경로 또는 상대 경로를 받으며, `false`로 설정하면 로딩을 비활성화함

### envPrefix

- **타입:** `string | string[]`
- **기본값:** `VITE_`
- 이 접두사를 가진 환경 변수가 `import.meta.env`를 통해 클라이언트 코드에 노출됨

```javascript
define: {
  'import.meta.env.ENV_VARIABLE': JSON.stringify(process.env.ENV_VARIABLE)
}
```

### appType

- **타입:** `'spa' | 'mpa' | 'custom'`
- **기본값:** `'spa'`
- 애플리케이션 유형
  - `'spa'`: HTML 미들웨어와 SPA 폴백을 포함함
  - `'mpa'`: HTML 미들웨어를 포함함
  - `'custom'`: HTML 미들웨어를 포함하지 않음

### devtools

- **타입:** `boolean | DevToolsConfig`
- **기본값:** `false`
- **상태:** 실험적
- 내부 상태 시각화 및 빌드 분석을 위한 devtools 통합을 활성화함
- `@vitejs/devtools` 의존성이 필요함

### future

- **타입:** `Record<string, 'warn' | undefined>`
- 다음 Vite 메이저 버전으로의 원활한 마이그레이션을 위해 향후 호환성 변경을 미리 활성화함

---

## 서버 옵션

> 원문: https://vite.dev/config/server-options

### server.host

- **타입:** `string | boolean`
- **기본값:** `'localhost'`
- 서버가 수신 대기할 IP 주소를 지정함
- `0.0.0.0` 또는 `true`로 설정하면 LAN 및 공용 주소를 포함한 모든 주소에서 수신함
- CLI: `--host 0.0.0.0` 또는 `--host`

### server.allowedHosts

- **타입:** `string[] | true`
- **기본값:** `[]`
- Vite가 응답할 수 있는 호스트명
- localhost, `.localhost` 도메인, 모든 IP 주소는 기본으로 허용됨
- `.`으로 시작하는 문자열은 해당 호스트명과 모든 서브도메인을 허용함
- `true`로 설정하면 모든 호스트를 허용함 (보안 위험)

### server.port

- **타입:** `number`
- **기본값:** `5173`
- 서버 포트 지정
- 이미 사용 중이면 `server.strictPort`가 활성화되지 않은 한 다음 가용 포트를 자동으로 시도함

### server.strictPort

- **타입:** `boolean`
- `true`로 설정하면 지정된 포트가 이미 사용 중일 때 대체 포트를 시도하지 않고 종료함

### server.https

- **타입:** `https.ServerOptions`
- TLS + HTTP/2를 활성화하며, `https.createServer()`에 전달할 옵션 객체를 받음
- 유효한 인증서가 필요하며, `@vitejs/plugin-basic-ssl`로 자체 서명 인증서를 생성할 수 있음

### server.open

- **타입:** `boolean | string`
- 서버 시작 시 브라우저에서 앱을 자동으로 열음
- 문자열 값은 URL 경로명이 됨
- `process.env.BROWSER`로 특정 브라우저를, `process.env.BROWSER_ARGS`로 추가 인수를 설정할 수 있음

```js
export default defineConfig({
  server: {
    open: '/docs/index.html',
  },
})
```

### server.proxy

- **타입:** `Record<string, string | ProxyOptions>`
- 개발 서버의 프록시 규칙을 설정함
- `^`로 시작하는 키는 RegExp 패턴으로 해석됨
- `http-proxy-3`를 확장함

```js
export default defineConfig({
  server: {
    proxy: {
      // string shorthand:
      // http://localhost:5173/foo
      //   -> http://localhost:4567/foo
      '/foo': 'http://localhost:4567',
      // with options:
      // http://localhost:5173/api/bar
      //   -> http://jsonplaceholder.typicode.com/bar
      '/api': {
        target: 'http://jsonplaceholder.typicode.com',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      // with RegExp:
      // http://localhost:5173/fallback/
      //   -> http://jsonplaceholder.typicode.com/
      '^/fallback/.*': {
        target: 'http://jsonplaceholder.typicode.com',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/fallback/, ''),
      },
      // Using the proxy instance
      '/api': {
        target: 'http://jsonplaceholder.typicode.com',
        changeOrigin: true,
        configure: (proxy, options) => {
          // proxy will be an instance of 'http-proxy-3'
        },
      },
      // Proxying websockets or socket.io:
      // ws://localhost:5173/socket.io
      //   -> ws://localhost:5174/socket.io
      // Exercise caution using `rewriteWsOrigin` as it can leave the
      // proxying open to CSRF attacks.
      '/socket.io': {
        target: 'ws://localhost:5174',
        ws: true,
        rewriteWsOrigin: true,
      },
    },
  },
})
```

### server.cors

- **타입:** `boolean | CorsOptions`
- **기본값:** `{ origin: /^https?:\/\/(?:(?:[^:]+\.)?localhost|127\.0\.0\.1|\[::1\])(?::\d+)?$/ }`
- 개발 서버의 CORS를 설정함
- `true`로 설정하면 모든 출처를 허용함

### server.headers

- **타입:** `OutgoingHttpHeaders`
- 서버 응답 헤더를 지정함

### server.hmr

- **타입:** `boolean | { overlay?: boolean }`
- HMR 동작을 비활성화하거나 설정함
- `server.hmr.overlay`를 `false`로 설정하면 에러 오버레이를 비활성화함

### server.ws

- **타입:** `false | { protocol?: string, host?: string, port?: number, path?: string, timeout?: number, clientPort?: number, server?: Server }`
- WebSocket 연결 옵션을 설정함
- `false`로 설정하면 WebSocket을 완전히 비활성화함
- `protocol`: `ws` 또는 `wss`
- `timeout`: 기본값 30000ms
- `clientPort`: 클라이언트 측 포트 오버라이드
- `server`: 커스텀 서버

```js
export default defineConfig({
  server: {
    ws: {
      protocol: 'wss',
      host: 'localhost',
      port: 3001,
    },
  },
})
```

### server.forwardConsole

- **타입:** `boolean | { unhandledErrors?: boolean, logLevels?: ('error' | 'warn' | 'info' | 'log' | 'debug')[] }`
- **기본값:** AI 코딩 에이전트 감지 시 자동으로 `true`, 그 외 `false`
- 브라우저 런타임 이벤트를 Vite 서버 콘솔로 전달함
- `true`로 설정하면 처리되지 않은 에러와 `console.error`/`console.warn` 전달을 활성화함

```js
export default defineConfig({
  server: {
    forwardConsole: {
      unhandledErrors: true,
      logLevels: ['warn', 'error'],
    },
  },
})
```

### server.warmup

- **타입:** `{ clientFiles?: string[], ssrFiles?: string[] }`
- 파일을 미리 변환하고 캐시하여 워밍업함
- `clientFiles`: 클라이언트 전용 파일
- `ssrFiles`: SSR 전용 파일
- 파일 경로 또는 tinyglobby 패턴을 루트 기준 상대 경로로 받음

```js
export default defineConfig({
  server: {
    warmup: {
      clientFiles: ['./src/components/*.vue', './src/utils/big-utils.js'],
      ssrFiles: ['./src/server/modules/*.js'],
    },
  },
})
```

### server.watch

- **타입:** `object | null`
- chokidar에 전달되는 파일 시스템 감시 옵션
- 기본적으로 루트를 감시하며 `.git/`, `node_modules/`, `test-results/`, 캐시, 출력 디렉터리를 건너뜀
- `null`로 설정하면 감시를 비활성화함

### server.middlewareMode

- **타입:** `boolean`
- **기본값:** `false`
- 커스텀 서버 통합을 위해 Vite 서버를 미들웨어 모드로 생성함

```js
import express from 'express'
import { createServer as createViteServer } from 'vite'

async function createServer() {
  const app = express()

  // Create Vite server in middleware mode
  const vite = await createViteServer({
    server: { middlewareMode: true },
    // don't include Vite's default HTML handling middlewares
    appType: 'custom',
  })
  // Use vite's connect instance as middleware
  app.use(vite.middlewares)

  app.use('*', async (req, res) => {
    // Since `appType` is `'custom'`, should serve response here.
    // Note: if `appType` is `'spa'` or `'mpa'`, Vite includes middlewares
    // to handle HTML requests and 404s so user middlewares should be added
    // before Vite's middlewares to take effect instead
  })
}

createServer()
```

### server.fs.strict

- **타입:** `boolean`
- **기본값:** `true`
- 워크스페이스 루트 외부의 파일 서빙을 제한함

### server.fs.allow

- **타입:** `string[]`
- `/@fs/`를 통해 서빙되는 파일을 제한함
- `server.fs.strict`가 `true`일 때 이 목록 외부에서 허용된 파일에서 임포트되지 않은 파일에 접근하면 403이 반환됨
- 디렉터리와 파일을 받음

```js
export default defineConfig({
  server: {
    fs: {
      // Allow serving files from one level up to the project root
      allow: ['..'],
    },
  },
})
```

```js
import { defineConfig, searchForWorkspaceRoot } from 'vite'

export default defineConfig({
  server: {
    fs: {
      allow: [
        // search up for workspace root
        searchForWorkspaceRoot(process.cwd()),
        // your custom rules
        '/path/to/custom/allow_directory',
        '/path/to/custom/allow_file.demo',
      ],
    },
  },
})
```

### server.fs.deny

- **타입:** `string[]`
- **기본값:** `['.env', '.env.*', '*.{crt,pem,key,p12,pfx,cer,der}', '.npmrc', '.yarnrc.yml', '**/.git/**']`
- Vite 개발 서버에서 제한되는 민감한 파일의 차단 목록
- `server.fs.allow`보다 높은 우선순위를 가짐
- picomatch 패턴을 지원함

### server.origin

- **타입:** `string`
- 개발 중 생성되는 에셋 URL의 출처를 정의함

```js
export default defineConfig({
  server: {
    origin: 'http://127.0.0.1:8080',
  },
})
```

### server.sourcemapIgnoreList

- **타입:** `false | (sourcePath: string, sourcemapPath: string) => boolean`
- **기본값:** `(sourcePath) => sourcePath.includes('node_modules')`
- 서버 소스맵에서 소스 파일을 무시할지 결정하며, `x_google_ignoreList` 소스맵 확장을 채움
- 기본적으로 `node_modules`가 포함된 경로를 제외함

```js
export default defineConfig({
  server: {
    // This is the default value, and will add all files with node_modules
    // in their paths to the ignore list.
    sourcemapIgnoreList(sourcePath, sourcemapPath) {
      return sourcePath.includes('node_modules')
    },
  },
})
```

---

## 빌드 옵션

> 원문: https://vite.dev/config/build-options

### build.target

- **타입:** `string | string[]`
- **기본값:** `'baseline-widely-available'`
- 최종 번들의 브라우저 호환성 대상
- 기본값은 2026-01-01 기준 Baseline Widely Available 호환 최소 브라우저 버전 (`['chrome111', 'edge111', 'firefox114', 'safari16.4', 'ios16.4']`)
- `'esnext'`는 네이티브 동적 임포트를 가정하고 최소한의 트랜스파일만 수행함
- ES 버전 (예: `es2015`), 브라우저 + 버전 (예: `chrome58`), 대상 문자열 배열을 지원함
- 변환에는 Oxc Transformer를 사용함

### build.modulePreload

- **타입:** `boolean | { polyfill?: boolean, resolveDependencies?: ResolveModulePreloadDependenciesFn }`
- **기본값:** `{ polyfill: true }`
- 기본적으로 각 `index.html` 진입점의 프록시 모듈에 모듈 프리로드 폴리필이 자동 주입됨
- HTML이 아닌 커스텀 진입점에서는 수동으로 임포트해야 함: `import 'vite/modulepreload-polyfill'`
- 라이브러리 모드에서는 폴리필이 적용되지 않음
- `{ polyfill: false }`로 비활성화할 수 있음
- `resolveDependencies` 함수로 세밀한 제어가 가능함 (실험적)

```typescript
type ResolveModulePreloadDependenciesFn = (
  url: string,
  deps: string[],
  context: {
    hostId: string
    hostType: 'html' | 'js'
  },
) => string[]
```

```javascript
modulePreload: {
  resolveDependencies: (filename, deps, { hostId, hostType }) => {
    return deps.filter(condition)
  },
},
```

### build.polyfillModulePreload

- **타입:** `boolean`
- **기본값:** `true`
- **상태:** 더 이상 사용하지 않음. `build.modulePreload.polyfill`을 대신 사용할 것

### build.outDir

- **타입:** `string`
- **기본값:** `dist`
- 프로젝트 루트 기준 상대 경로로 출력 디렉터리를 지정함

### build.assetsDir

- **타입:** `string`
- **기본값:** `assets`
- 생성된 에셋을 중첩할 디렉터리로 `build.outDir` 기준 상대 경로임
- 라이브러리 모드에서는 사용되지 않음

### build.assetsInlineLimit

- **타입:** `number | ((filePath: string, content: Buffer) => boolean | undefined)`
- **기본값:** `4096` (4 KiB)
- 이 임계값보다 작은 임포트/참조 에셋은 base64 URL로 인라인되어 추가 HTTP 요청을 줄임
- `0`으로 설정하면 인라인을 비활성화함
- 콜백 함수로 옵트인/옵트아웃을 제어할 수 있으며, `undefined`는 기본 로직을 적용함
- Git LFS 플레이스홀더는 자동으로 제외됨
- `build.lib`이 지정되면 이 옵션은 무시되고 항상 인라인됨

### build.cssCodeSplit

- **타입:** `boolean`
- **기본값:** `true`
- CSS 코드 분할을 활성화/비활성화함
- 활성화 시 비동기 JS 청크에서 임포트된 CSS가 청크로 보존되어 함께 로드됨
- 비활성화 시 모든 CSS가 단일 파일로 추출됨
- `build.lib`이 지정되면 기본값이 `false`임

### build.cssTarget

- **타입:** `string | string[]`
- **기본값:** `build.target`과 동일
- CSS 축소에 대해 JavaScript 트랜스파일과 다른 브라우저 대상을 설정함
- 비주류 브라우저를 대상으로 할 때 사용함
- 예: Android WeChat WebView는 `rgba()`에서 `#RGBA` 16진수 변환을 방지하기 위해 `chrome61`이 필요함

### build.cssMinify

- **타입:** `boolean | 'lightningcss' | 'esbuild'`
- **기본값:** 클라이언트 빌드 시 `'lightningcss'`, `build.minify`가 비활성화된 경우 `false`
- `build.minify`와 별도로 CSS 축소를 오버라이드함
- 기본적으로 Lightning CSS를 사용하며 `css.lightningcss`로 설정 가능함
- `'esbuild'`로 설정하면 esbuild를 대신 사용하며, esbuild 설치가 필요함

```sh
npm add -D esbuild
```

### build.sourcemap

- **타입:** `boolean | 'inline' | 'hidden'`
- **기본값:** `false`
- 프로덕션 소스맵을 생성함
- `true`: 별도의 소스맵 파일 생성
- `'inline'`: 소스맵을 출력 파일에 data URI로 추가
- `'hidden'`: `true`와 동일하지만 번들 파일의 소스맵 주석을 숨김

### build.chunkImportMap

- **타입:** `boolean`
- **기본값:** `false`
- **상태:** 실험적
- 청크 캐싱 효율성을 최적화하기 위해 import map 기능을 사용할지 여부
- `import.meta.resolve` 지원이 필요하며, 구형 브라우저에는 `@vitejs/plugin-legacy`를 사용할 것

### build.rolldownOptions

- **타입:** `RolldownOptions`
- 기본 Rolldown 번들을 직접 커스터마이즈함
- Vite의 내부 Rolldown 옵션과 병합됨
- 개발 및 빌드 일관성을 위해 `build.rolldownOptions.input` 대신 최상위 `input` 옵션을 설정하는 것을 권장함

### build.rollupOptions

- **타입:** `RolldownOptions`
- **상태:** 더 이상 사용하지 않음. `build.rolldownOptions`를 대신 사용할 것

### build.dynamicImportVarsOptions

- **타입:** `{ include?: string | RegExp | (string | RegExp)[], exclude?: string | RegExp | (string | RegExp)[] }`
- 변수를 사용한 동적 임포트의 변환을 설정함

### build.lib

- **타입:** `{ entry?: string | string[] | { [entryAlias: string]: string }, name?: string, formats?: ('es' | 'cjs' | 'umd' | 'iife')[], fileName?: string | ((format: ModuleFormat, entryName: string) => string), cssFileName?: string }`
- 라이브러리로 빌드함
- `entry`: 최상위 `input` 옵션이 기본값이며, 라이브러리는 HTML을 진입점으로 사용할 수 없으므로 하나는 반드시 필요함
- `name`: 노출되는 전역 변수 이름으로, `formats`에 `'umd'` 또는 `'iife'`가 포함될 때 필수임
- `formats`: 기본값은 `['es', 'umd']` 또는 다중 진입점 시 `['es', 'cjs']`
- `fileName`: 기본값은 package.json의 `"name"`이며, `format`과 `entryName`을 받는 함수도 가능함
- `cssFileName`: CSS 출력 파일명으로, 문자열인 `fileName`이나 package.json의 `"name"`이 기본값임

```javascript
import { defineConfig } from 'vite'

export default defineConfig({
  build: {
    lib: {
      entry: ['src/main.js'],
      fileName: (format, entryName) => `my-lib-${entryName}.${format}.js`,
      cssFileName: 'my-lib-style',
    },
  },
})
```

### build.license

- **타입:** `boolean | { fileName?: string }`
- **기본값:** `false`
- `true`로 설정하면 번들된 의존성의 라이선스 정보가 담긴 `.vite/license.md` 파일을 생성함
- `fileName`으로 `outDir` 기준 라이선스 파일명을 지정할 수 있음
- `.json`으로 끝나면 원시 JSON 메타데이터를 생성함

```json
[
  {
    "name": "dep-1",
    "version": "1.2.3",
    "identifier": "CC0-1.0",
    "text": "CC0 1.0 Universal\n\n..."
  },
  {
    "name": "dep-2",
    "version": "4.5.6",
    "identifier": "MIT",
    "text": "MIT License\n\n..."
  }
]
```

- `build.rolldownOptions.output.postBanner`로 빌드 코드에서 라이선스 파일을 참조할 수 있음

```javascript
import { defineConfig } from 'vite'

export default defineConfig({
  build: {
    license: true,
    rolldownOptions: {
      output: {
        postBanner:
          '/* See licenses of bundled dependencies at https://example.com/license.md */',
      },
    },
  },
})
```

### build.manifest

- **타입:** `boolean | string`
- **기본값:** `false`
- 비해시 에셋 파일명을 해시 버전에 매핑하는 매니페스트 파일을 생성함
- 서버 프레임워크의 에셋 링크 렌더링에 사용됨
- 문자열이면 `build.outDir` 기준 매니페스트 파일 경로로 사용됨
- `true`이면 경로가 `.vite/manifest.json`임

### build.ssrManifest

- **타입:** `boolean | string`
- **기본값:** `false`
- 프로덕션에서 스타일 링크 및 에셋 프리로드 지시자를 결정하기 위한 SSR 매니페스트 파일을 생성함
- 문자열이면 `build.outDir` 기준 매니페스트 파일 경로로 사용됨
- `true`이면 경로가 `.vite/ssr-manifest.json`임

### build.ssr

- **타입:** `boolean | string`
- **기본값:** `false`
- SSR 지향 빌드를 생성함
- 문자열 값은 SSR 진입점을 직접 지정함
- `true`인 경우 `input` 또는 `build.rolldownOptions.input`으로 SSR 진입점을 지정해야 함

### build.emitAssets

- **타입:** `boolean`
- **기본값:** `false`
- 비 클라이언트 빌드 시 정적 에셋은 클라이언트 빌드에서 내보낸다고 가정하여 내보내지 않음
- 이 옵션으로 다른 환경에서도 에셋 내보내기를 강제할 수 있음
- 프레임워크가 빌드 후 단계에서 에셋을 병합해야 함

### build.ssrEmitAssets

- **타입:** `boolean`
- **기본값:** `false`
- SSR 빌드 시 정적 에셋 내보내기를 강제함
- Environment API가 안정되면 `build.emitAssets`로 대체될 예정임

### build.minify

- **타입:** `boolean | 'oxc' | 'terser' | 'esbuild'`
- **기본값:** 클라이언트 빌드 시 `'oxc'`, SSR 빌드 시 `false`
- `false`로 설정하면 축소를 비활성화하거나, 축소기를 지정함
- 기본값은 Oxc Minifier로, terser보다 30-90배 빠르고 0.5-2% 낮은 압축률을 가짐
- `'es'` 포맷 lib 모드에서는 순수 어노테이션과 트리 셰이킹을 유지하기 위해 공백을 축소하지 않음
- esbuild 또는 Terser 지정 시 해당 패키지 설치가 필요함

```sh
npm add -D esbuild
npm add -D terser
```

### build.terserOptions

- **타입:** `TerserOptions`
- Terser에 전달할 추가 축소 옵션
- `maxWorkers: number` 옵션으로 생성할 최대 워커 수를 지정할 수 있으며, 기본값은 CPU 수 - 1임

### build.write

- **타입:** `boolean`
- **기본값:** `true`
- `false`로 설정하면 번들을 디스크에 쓰지 않음
- 주로 디스크 쓰기 전 후처리가 필요한 프로그래밍 방식의 `build()` 호출에서 사용됨

### build.emptyOutDir

- **타입:** `boolean`
- **기본값:** `outDir`이 `root` 내부에 있으면 `true`
- 빌드 시 `outDir`을 비움
- `outDir`이 루트 외부에 있으면 중요한 파일을 실수로 삭제하지 않도록 경고를 표시함
- 명시적으로 설정하여 경고를 억제할 수 있음
- CLI: `--emptyOutDir`

### build.copyPublicDir

- **타입:** `boolean`
- **기본값:** `true`
- 빌드 시 `publicDir`의 파일을 `outDir`에 복사함
- `false`로 설정하면 비활성화됨

### build.reportCompressedSize

- **타입:** `boolean`
- **기본값:** `true`
- gzip 압축 크기 보고를 활성화/비활성화함
- 대형 프로젝트에서 비활성화하면 빌드 성능이 향상될 수 있음

### build.chunkSizeWarningLimit

- **타입:** `number`
- **기본값:** `500`
- 청크 크기 경고 제한 (킬로바이트 단위)
- 비압축 청크 크기와 비교됨

### build.watch

- **타입:** `WatcherOptions | null`
- **기본값:** `null`
- `{}`로 설정하면 Rolldown 감시자를 활성화함
- 빌드 전용 플러그인이나 통합에 사용됨
- Windows Subsystem for Linux 2에서는 파일 시스템 감시가 작동하지 않을 수 있음

---

## 미리보기 옵션

> 원문: https://vite.dev/config/preview-options

### preview.host

- **타입:** `string | boolean`
- **기본값:** `server.host`
- 서버가 수신 대기할 IP 주소를 지정함
- `0.0.0.0` 또는 `true`로 설정하면 LAN 및 공용 주소를 포함한 모든 주소에서 수신함
- CLI: `--host 0.0.0.0` 또는 `--host`

### preview.allowedHosts

- **타입:** `string[] | true`
- **기본값:** `server.allowedHosts`
- Vite가 응답할 수 있는 호스트명

### preview.port

- **타입:** `number`
- **기본값:** `4173`
- 서버 포트 지정
- 이미 사용 중이면 다음 가용 포트를 자동으로 시도함

```js
export default defineConfig({
  server: {
    port: 3030,
  },
  preview: {
    port: 8080,
  },
})
```

### preview.strictPort

- **타입:** `boolean`
- **기본값:** `server.strictPort`
- `true`로 설정하면 포트가 이미 사용 중일 때 다음 가용 포트를 시도하지 않고 종료함

### preview.https

- **타입:** `https.ServerOptions`
- **기본값:** `server.https`
- TLS + HTTP/2를 활성화함

### preview.open

- **타입:** `boolean | string`
- **기본값:** `server.open`
- 서버 시작 시 브라우저에서 앱을 자동으로 열음
- 문자열 값은 URL 경로명으로 사용됨
- `process.env.BROWSER`로 특정 브라우저를, `process.env.BROWSER_ARGS`로 추가 인수를 설정할 수 있음
- 환경 변수 `BROWSER`와 `BROWSER_ARGS`는 `.env` 파일에서 설정 가능함

### preview.proxy

- **타입:** `Record<string, string | ProxyOptions>`
- **기본값:** `server.proxy`
- 미리보기 서버의 커스텀 프록시 규칙을 설정함
- `^`로 시작하는 키는 `RegExp`로 해석됨
- `configure` 옵션으로 프록시 인스턴스에 접근할 수 있음
- `http-proxy-3` 라이브러리를 사용함

### preview.cors

- **타입:** `boolean | CorsOptions`
- **기본값:** `server.cors`
- 미리보기 서버의 CORS를 설정함

### preview.headers

- **타입:** `OutgoingHttpHeaders`
- 서버 응답 헤더를 지정함

---

## 의존성 최적화 옵션

> 원문: https://vite.dev/config/dep-optimization-options

### optimizeDeps.entries

- **타입:** `string | string[]`
- **기본값:** `.html` 파일에서 자동 감지
- 의존성 사전 번들링을 위한 커스텀 진입점을 지정함
- 프로젝트 루트 기준 상대 경로의 tinyglobby 패턴을 받음
- 기본 진입점 추론을 덮어씀
- 기본적으로 `.html` 파일을 크롤링하며 `node_modules`, `build.outDir`, `__tests__`, `coverage`를 무시함
- `!`로 시작하는 무시 패턴을 포함할 수 있음

### optimizeDeps.exclude

- **타입:** `string[]`
- 사전 번들링에서 제외할 의존성
- CommonJS 의존성은 제외해서는 안 됨
- ESM 의존성을 제외했지만 중첩된 CommonJS 의존성이 있는 경우 해당 CommonJS 의존성을 `optimizeDeps.include`에 추가해야 함

```js
export default defineConfig({
  optimizeDeps: {
    include: ['esm-dep > cjs-dep'],
  },
})
```

### optimizeDeps.include

- **타입:** `string[]`
- `node_modules` 외부의 링크된 패키지를 강제로 사전 번들링함
- 딥 임포트에 대한 실험적 glob 패턴 지원 (예: `my-lib/components/**/*.vue`)으로 지속적인 재번들링을 방지함

### optimizeDeps.rolldownOptions

- **타입:** `Omit<RolldownOptions, 'input' | 'logLevel' | 'output'> & { output?: Omit<RolldownOutputOptions, 'format' | 'sourcemap' | 'dir' | 'banner'> }`
- 의존성 스캐닝 및 최적화 시 Rolldown에 전달할 옵션
- 호환성을 위해 특정 옵션이 제외됨
- 플러그인은 Vite의 의존성 플러그인과 병합됨

### optimizeDeps.esbuildOptions

- **타입:** `Omit<EsbuildBuildOptions, 'bundle' | 'entryPoints' | 'external' | 'write' | 'watch' | 'outdir' | 'outfile' | 'outbase' | 'outExtension' | 'metafile'>`
- **상태:** 더 이상 사용하지 않음
- `optimizeDeps.rolldownOptions`를 대신 사용할 것
- 내부적으로 Rolldown 옵션으로 변환됨

### optimizeDeps.force

- **타입:** `boolean`
- **기본값:** `false`
- 이전에 캐시된 최적화 의존성을 무시하고 의존성 사전 번들링을 강제함

### optimizeDeps.noDiscovery

- **타입:** `boolean`
- **기본값:** `false`
- 자동 의존성 발견을 비활성화함
- `optimizeDeps.include`에 나열된 의존성만 최적화됨
- CommonJS 전용 의존성은 개발 중 명시적으로 포함해야 함

### optimizeDeps.holdUntilCrawlEnd

- **타입:** `boolean`
- **기본값:** `true`
- **상태:** 실험적
- 콜드 스타트 시 모든 정적 임포트가 크롤링될 때까지 첫 번째 최적화 의존성 결과를 보류함
- 새 의존성이 발견될 때 전체 페이지 리로드를 방지함
- 모든 의존성이 이미 알려진 경우 비활성화하면 브라우저의 병렬 요청을 허용함

### optimizeDeps.disabled

- **타입:** `boolean | 'build' | 'dev'`
- **기본값:** `'build'`
- **상태:** 더 이상 사용하지 않음 (실험적)
- 빌드 중 사전 번들링은 Vite 5.1에서 제거됨
- `true` 또는 `'dev'`는 최적화기를 비활성화함
- `false` 또는 `'build'`는 개발 중 활성화함
- 완전히 비활성화하려면 `optimizeDeps.noDiscovery: true`를 사용할 것

### optimizeDeps.needsInterop

- **타입:** `string[]`
- **상태:** 실험적
- 지정된 의존성을 임포트할 때 ESM 인터롭을 강제함
- Vite가 인터롭 필요성을 자동 감지하므로 일반적으로 불필요함
- 복잡한 의존성 시나리오에서 전체 페이지 리로드를 방지하여 콜드 스타트를 가속할 수 있음

---

## SSR 옵션

> 원문: https://vite.dev/config/ssr-options

### ssr.external

- **타입:** `string[] | true`
- **기본값:** 링크된 의존성을 제외한 모든 의존성이 외부화됨
- 지정된 의존성과 그 전이 의존성을 SSR용으로 외부화함
- `true`로 설정하면 링크된 의존성을 포함한 모든 의존성을 외부화함
- `ssr.external`과 `ssr.noExternal`에 동일한 의존성이 있으면 `ssr.external`이 우선함

### ssr.noExternal

- **타입:** `string | RegExp | (string | RegExp)[] | true`
- **기본값:** 링크된 의존성만 외부화되지 않음
- 나열된 의존성이 SSR 중 외부화되지 않도록 하여 번들에 포함시킴
- `true`로 설정하면 모든 외부화를 방지하지만, `ssr.external`에 명시적으로 있는 의존성은 오버라이드할 수 있음
- `ssr.target: 'node'`일 때 Node.js 내장 모듈도 기본으로 외부화됨
- `ssr.noExternal: true`와 `ssr.external: true`가 모두 설정되면 `ssr.noExternal`이 우선함

### ssr.target

- **타입:** `'node' | 'webworker'`
- **기본값:** `'node'`
- SSR 서버의 빌드 대상 환경을 지정함

### ssr.resolve.conditions

- **타입:** `string[]`
- **기본값:** node 대상 시 `['module', 'node', 'development|production']`, webworker 대상 시 `['module', 'browser', 'development|production']`
- 플러그인 파이프라인에서 사용되며 SSR 빌드 중 외부화되지 않은 의존성에만 영향을 줌
- 외부화된 임포트를 수정하려면 `ssr.resolve.externalConditions`를 사용할 것

### ssr.resolve.externalConditions

- **타입:** `string[]`
- **기본값:** `['node', 'module-sync']`
- 외부화된 직접 의존성의 SSR 임포트에 적용되는 조건
- 일관성을 위해 개발과 빌드 모두에서 일치하는 `--conditions` 플래그로 Node를 실행하는 것을 권장함

### ssr.resolve.mainFields

- **타입:** `string[]`
- **기본값:** `['module', 'jsnext:main', 'jsnext']`
- 패키지 진입점 해석 시 확인할 `package.json` 필드
- `exports` 필드의 조건부 내보내기보다 낮은 우선순위를 가짐
- 외부화되지 않은 의존성에만 영향을 줌

---

## 워커 옵션

> 원문: https://vite.dev/config/worker-options

### worker.format

- **타입:** `'es' | 'iife'`
- **기본값:** `'iife'`
- 워커 번들의 출력 포맷을 제어함

### worker.plugins

- **타입:** `() => (Plugin | Plugin[])[]`
- 워커 번들에 Vite 플러그인을 적용함
- `config.plugins`와 달리 빌드와 미리보기 모두에서 적용됨 (`config.plugins`는 개발 중에만 워커에 영향을 줌)
- 병렬 Rolldown 워커 빌드에서 실행되므로 함수가 새로운 플러그인 인스턴스를 반환해야 함
- `config` 훅 내의 `config.worker` 옵션 수정은 무시됨

### worker.rolldownOptions

- **타입:** `RolldownOptions`
- 워커 번들 구성을 위한 Rolldown 옵션을 제공함

### worker.rollupOptions

- **타입:** `RolldownOptions`
- **상태:** 더 이상 사용하지 않음
- `worker.rolldownOptions`의 별칭이며, `worker.rolldownOptions`를 대신 사용할 것

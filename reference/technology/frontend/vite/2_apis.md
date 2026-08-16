# Vite APIs

## Plugin API

> 원문: https://vite.dev/guide/api-plugin

### 개요

- Vite 플러그인은 Rolldown의 플러그인 인터페이스를 확장하며 Vite 전용 옵션이 추가됨
- 개발과 빌드 환경 모두에서 동작함
- `vite-plugin-inspect` 패키지로 `localhost:5173/__inspect/`에서 중간 플러그인 상태를 검사할 수 있음

### 네이밍 규칙

- Rolldown 플러그인(Rolldown/Rollup 호환): 접두사 `rolldown-plugin-`, 키워드 `rolldown-plugin`과 `vite-plugin`
- Vite 전용 플러그인: 접두사 `vite-plugin-`, 키워드 `vite-plugin`
- 프레임워크 전용 플러그인
  - Vue: `vite-plugin-vue-`
  - React: `vite-plugin-react-`
  - Svelte: `vite-plugin-svelte-`

### 플러그인 설정

- `vite.config.js`의 `plugins` 배열에 추가함
- falsy 플러그인은 무시됨
- 플러그인 배열(프리셋)은 내부적으로 평탄화됨

```javascript
import vitePlugin from 'vite-plugin-feature'
export default defineConfig({
  plugins: [vitePlugin()],
})
```

### 플러그인 예시: 커스텀 파일 타입 변환

```javascript
const fileRegex = /\.(my-file-ext)$/

export default function myPlugin() {
  return {
    name: 'transform-file',
    transform: {
      filter: { id: fileRegex },
      handler(src, id) {
        return {
          code: compileFileToJS(src),
          map: null,
        }
      },
    },
  }
}
```

### 플러그인 예시: 가상 모듈 패턴

- 가상 모듈 ID에는 `\0` 접두사를 붙여 resolved ID로 변환함
- `@rolldown/pluginutils`의 `exactRegex`를 사용하여 정확한 매칭을 수행함

```javascript
import { exactRegex } from '@rolldown/pluginutils'

export default function myPlugin() {
  const virtualModuleId = 'virtual:my-module'
  const resolvedVirtualModuleId = '\0' + virtualModuleId

  return {
    name: 'my-plugin',
    resolveId: {
      filter: { id: exactRegex(virtualModuleId) },
      handler() {
        return resolvedVirtualModuleId
      },
    },
    load: {
      filter: { id: exactRegex(resolvedVirtualModuleId) },
      handler() {
        return `export const msg = "from virtual module"`
      },
    },
  }
}
```

### Rolldown 훅

- 서버 시작 시 한 번 호출: `options`, `buildStart`
- 각 모듈 요청 시 호출: `resolveId`, `load`, `transform`
- 서버 종료 시 호출: `buildEnd`, `closeBundle`
- `moduleParsed`는 dev 중 호출되지 않음
- Output Generation 훅(`closeBundle` 제외)은 dev 중 호출되지 않음

### Vite 전용 훅

#### config

- 타입: `(config: UserConfig, env: { mode, command, isSsrBuild?, isPreview? }) => UserConfig | null | void`
- 종류: async, sequential
- 범위: Global
- 설정 해석 전에 Vite config를 수정함
- 부분 config를 반환하거나 기존 config를 직접 변경할 수 있음
- 사용자 플러그인이 이 훅 전에 해석되므로, 여기서 플러그인을 주입해도 효과 없음

#### configResolved

- 타입: `(config: ResolvedConfig) => void | Promise<void>`
- 종류: async, parallel
- 범위: Global
- config 해석 후 호출됨
- 최종 해석된 config를 저장하거나 실행 중인 command를 확인하는 데 유용함
- dev에서 `command` 값은 `serve`임

#### configureServer

- 타입: `(server: ViteDevServer) => (() => void) | void | Promise<(() => void) | void>`
- 종류: async, sequential
- 범위: Global
- dev 서버에 커스텀 미들웨어를 추가함

```javascript
configureServer(server) {
  server.middlewares.use((req, res, next) => {
    // custom handle request...
  })
}
```

- 내부 미들웨어 이후에 미들웨어를 설치하려면 함수를 반환함
- 서버 인스턴스를 저장하여 다른 훅에서 사용할 수 있음

#### configurePreviewServer

- 타입: `(server: PreviewServer) => (() => void) | void | Promise<(() => void) | void>`
- 종류: async, sequential
- 범위: Global
- `configureServer`와 유사하지만 프리뷰 서버 대상임
- 내부 미들웨어 이후에 주입하려면 함수를 반환함

#### transformIndexHtml

- 타입: `IndexHtmlTransformHook | { order?: 'pre' | 'post', handler: IndexHtmlTransformHook }`
- 종류: async, sequential
- 범위: Per-environment
- `index.html` 같은 HTML 엔트리 파일을 변환함
- 반환값
  - 변환된 HTML 문자열
  - 태그 디스크립터 배열: `{ tag, attrs, children, injectTo? }`
  - 둘 다 포함하는 객체: `{ html, tags }`

```typescript
type IndexHtmlTransformHook = (
  html: string,
  ctx: {
    path: string
    filename: string
    server?: ViteDevServer
    bundle?: OutputBundle
    chunk?: OutputChunk
    originalUrl?: string
  },
) => IndexHtmlTransformResult | void | Promise<IndexHtmlTransformResult | void>

interface HtmlTagDescriptor {
  tag: string
  attrs?: Record<string, string | boolean>
  children?: string | HtmlTagDescriptor[]
  injectTo?: 'head' | 'body' | 'head-prepend' | 'body-prepend'
}
```

- order 기본값은 undefined (HTML 변환 후 적용)
- `pre`: HTML 처리 전 적용
- `post`: 마지막에 적용

#### handleHotUpdate

- 타입: `(ctx: HmrContext) => Array<ModuleNode> | void | Promise<Array<ModuleNode> | void>`
- 종류: async, sequential
- 범위: Per-environment
- 커스텀 HMR 업데이트 처리를 수행함

```typescript
interface HmrContext {
  file: string
  timestamp: number
  modules: Array<ModuleNode>
  read: () => string | Promise<string>
  server: ViteDevServer
}
```

- 영향 받는 모듈을 필터링하여 정밀한 HMR 수행 가능
- 빈 배열을 반환하고 전체 리로드를 트리거할 수 있음
- 클라이언트에 커스텀 WebSocket 이벤트를 전송할 수 있음

```javascript
handleHotUpdate({ server, modules, timestamp }) {
  const invalidatedModules = new Set()
  for (const mod of modules) {
    server.moduleGraph.invalidateModule(
      mod,
      invalidatedModules,
      timestamp,
      true
    )
  }
  server.ws.send({ type: 'full-reload' })
  return []
}
```

### 플러그인 컨텍스트 메타

- `this.meta.viteVersion`: 현재 Vite 버전 문자열
- `this.meta.rolldownVersion`: Rolldown 기반 Vite(v8+)에서만 사용 가능

```typescript
if (this.meta.rolldownVersion) {
  // Rolldown 기반 Vite
} else {
  // Rollup 기반 Vite
}
```

### Output Bundle 메타데이터

- 빌드 중 Vite는 `RenderedChunk`, `OutputChunk`, `OutputAsset`에 `viteMetadata` 필드를 추가함
- `importedCss: Set<string>`: 가져온 CSS 파일
- `importedAssets: Set<string>`: 가져온 에셋 파일

```typescript
generateBundle(_, bundle) {
  for (const output of Object.values(bundle)) {
    const css = output.viteMetadata?.importedCss
    const assets = output.viteMetadata?.importedAssets
  }
}
```

### 플러그인 순서

- 해석 순서
  1. Alias
  2. `enforce: 'pre'`인 사용자 플러그인
  3. Vite 핵심 플러그인
  4. enforce 없는 사용자 플러그인
  5. Vite 빌드 플러그인
  6. `enforce: 'post'`인 사용자 플러그인
  7. Vite 포스트 빌드 플러그인
- 훅 순서는 Rolldown의 `order` 속성으로 제어함

### 조건부 적용

```javascript
function myPlugin() {
  return {
    name: 'build-only',
    apply: 'build', // 또는 'serve'
  }
}
```

- 함수 기반 제어도 가능함

```javascript
apply(config, { command }) {
  return command === 'build' && !config.build.ssr
}
```

### Rolldown 플러그인 호환성

- 다음 조건을 충족하면 Rolldown/Rollup 플러그인을 Vite 플러그인으로 사용 가능함
  - `moduleParsed` 훅을 사용하지 않을 것
  - Rolldown 전용 옵션(`transform.inject` 등)에 의존하지 않을 것
  - 번들 단계와 출력 단계 훅 간의 강한 결합이 없을 것
- 빌드 전용 플러그인은 `build.rolldownOptions.plugins`에 지정함

```javascript
{
  ...example(),
  enforce: 'post',
  apply: 'build',
}
```

### 경로 정규화

- Vite는 경로를 POSIX 구분자(`/`)로 정규화함

```javascript
import { normalizePath } from 'vite'

normalizePath('foo\\bar') // 'foo/bar'
```

### 필터링 및 Include/Exclude 패턴

- `@rollup/pluginutils`의 `createFilter`를 사용함
- 훅 필터(Rollup 4.38.0+, Vite 6.3.0+)로 오버헤드를 줄일 수 있음

```javascript
transform: {
  filter: {
    id: jsFileRegex,
  },
  handler(code, id) {
    if (!jsFileRegex.test(id)) return null
    return { code: transformCode(code), map: null }
  },
}
```

- `@rolldown/pluginutils`에서 `exactRegex`, `prefixRegex` 유틸리티를 내보냄 (`rolldown/filter`에서도 사용 가능)

### Chunk Import Map 정보

- `build.chunkImportMap` 활성화 시, import 문이 파일 경로 대신 고유 ID를 사용함
- `generateBundle` 또는 `writeBundle` 훅에서 매핑에 접근 가능함

```typescript
function accessImportMap() {
  let config: ResolvedConfig
  return {
    name: 'access-import-map',
    configResolved(resolvedConfig) {
      config = resolvedConfig
    },
    generateBundle(options, bundle) {
      const chunkImportMap = config.build.rolldownOptions.experimental?.chunkImportMap
      if (chunkImportMap) {
        const importMapFilename =
          typeof chunkImportMap === 'object' && chunkImportMap.fileName
            ? chunkImportMap.fileName
            : 'importmap.json'
        const importMap = bundle[importMapFilename]
        const mapping = JSON.parse(importMap.source).imports
      }
    },
  }
}
```

### 클라이언트-서버 통신

#### 서버에서 클라이언트로

- 이벤트 이름에 항상 접두사를 붙여 충돌을 방지해야 함

```javascript
configureServer(server) {
  server.ws.on('connection', () => {
    server.ws.send('my:greetings', { msg: 'hello' })
  })
}
```

- 클라이언트 측 리스닝

```typescript
if (import.meta.hot) {
  import.meta.hot.on('my:greetings', (data) => {
    console.log(data.msg)
  })
}
```

#### 클라이언트에서 서버로

```typescript
if (import.meta.hot) {
  import.meta.hot.send('my:from-client', { msg: 'Hey!' })
}
```

- 서버 측 리스닝

```javascript
server.ws.on('my:from-client', (data, client) => {
  console.log('Message from client:', data.msg)
  client.send('my:ack', { msg: 'Hi! I got your message!' })
})
```

#### 커스텀 이벤트 TypeScript 타이핑

- `.d.ts` 파일에서 `CustomEventMap` 인터페이스를 확장함

```typescript
import 'vite/types/customEvent.d.ts'

declare module 'vite/types/customEvent.d.ts' {
  interface CustomEventMap {
    'custom:foo': { msg: string }
  }
}
```

- `InferCustomEventPayload<T>`로 타입 추론 가능함

```typescript
type CustomFooPayload = InferCustomEventPayload<'custom:foo'>
import.meta.hot?.on('custom:foo', (payload) => {
  // payload type: { msg: string }
})
```

---

## HMR API

> 원문: https://vite.dev/guide/api-hmr

### 개요

- Vite는 `import.meta.hot` 특수 객체를 통해 수동 HMR API를 노출함
- 주로 프레임워크 및 도구 작성자를 대상으로 함

### 타입 정의

```typescript
interface ImportMeta {
  readonly hot?: ViteHotContext
}

interface ViteHotContext {
  readonly data: any

  accept(): void
  accept(cb: (mod: ModuleNamespace | undefined) => void): void
  accept(dep: string, cb: (mod: ModuleNamespace | undefined) => void): void
  accept(
    deps: readonly string[],
    cb: (mods: Array<ModuleNamespace | undefined>) => void,
  ): void

  dispose(cb: (data: any) => void): void
  prune(cb: (data: any) => void): void
  invalidate(message?: string): void

  on<T extends CustomEventName>(
    event: T,
    cb: (payload: InferCustomEventPayload<T>) => void,
  ): void
  off<T extends CustomEventName>(
    event: T,
    cb: (payload: InferCustomEventPayload<T>) => void,
  ): void
  send<T extends CustomEventName>(
    event: T,
    data?: InferCustomEventPayload<T>,
  ): void
}
```

### 필수 조건부 가드

- 모든 HMR 코드는 프로덕션에서 트리셰이킹을 위해 조건부 가드로 감싸야 함

```javascript
if (import.meta.hot) {
  // HMR code
}
```

### TypeScript 설정

```json
{
  "compilerOptions": {
    "types": ["vite/client"]
  }
}
```

### hot.accept(cb)

- 모듈이 자기 자신의 업데이트를 수락(self-accept)하는 데 사용함
- 업데이트를 수락하는 모듈은 HMR 경계(boundary)가 됨
- Vite는 소스 코드에서 `import.meta.hot.accept(`가 정확히 나타나야 함 (공백 민감)

```javascript
export const count = 1

if (import.meta.hot) {
  import.meta.hot.accept((newModule) => {
    if (newModule) {
      // newModule is undefined when SyntaxError happened
      console.log('updated: count is now ', newModule.count)
    }
  })
}
```

### hot.accept(deps, cb)

- 직접 의존성의 업데이트를 수락할 수 있음 (자신을 리로드하지 않음)

```javascript
import { foo } from './foo.js'

foo()

if (import.meta.hot) {
  import.meta.hot.accept('./foo.js', (newFoo) => {
    // the callback receives the updated './foo.js' module
    newFoo?.foo()
  })

  // Can also accept an array of dep modules:
  import.meta.hot.accept(
    ['./foo.js', './bar.js'],
    ([newFooModule, newBarModule]) => {
      // The callback receives an array where only the updated module is
      // non null. If the update was not successful (syntax error for ex.),
      // the array is empty
    },
  )
}
```

### hot.dispose(cb)

- self-accepting 모듈 또는 수락될 것으로 예상되는 모듈이 지속적인 부수효과를 정리하는 데 사용함

```javascript
function setupSideEffect() {}

setupSideEffect()

if (import.meta.hot) {
  import.meta.hot.dispose((data) => {
    // cleanup side effect
  })
}
```

### hot.prune(cb)

- 모듈이 페이지에서 더 이상 import 되지 않을 때 호출될 콜백을 등록함
- `dispose`와 달리, 소스 코드가 업데이트 시 자체적으로 부수효과를 정리하는 경우에 사용함

```javascript
function setupOrReuseSideEffect() {}

setupOrReuseSideEffect()

if (import.meta.hot) {
  import.meta.hot.prune((data) => {
    // cleanup side effect
  })
}
```

### hot.data

- Vite는 각 모듈 경로마다 하나의 `import.meta.hot.data` 객체를 생성함
- 이 객체는 HMR 중 동일 모듈의 연속 인스턴스 간에 유지됨
- 프루닝 시 콜백이 현재 data 객체를 받고, 콜백 완료 후 Vite가 data를 초기화함
- `data` 자체의 재할당은 지원되지 않으며, 속성을 변경해야 함

```javascript
// ok
import.meta.hot.data.someValue = 'hello'

// not supported
import.meta.hot.data = { someValue: 'hello' }
```

### hot.decline()

- 현재 noop이며 하위 호환성을 위해 존재함
- 업데이트 불가능한 모듈을 나타내려면 `hot.invalidate()`를 사용함

### hot.invalidate(message?: string)

- self-accepting 모듈이 런타임 중 HMR 업데이트를 처리할 수 없다고 판단할 때 사용함
- 호출 시 호출자가 self-accepting이 아닌 것처럼 importer에게 무효화를 전파함
- 브라우저 콘솔과 터미널 양쪽에 메시지를 기록함

```javascript
import.meta.hot.accept((module) => {
  // You may use the new module instance to decide whether to invalidate.
  if (cannotHandleUpdate(module)) {
    import.meta.hot.invalidate()
  }
})
```

### hot.on(event, cb)

- HMR 이벤트를 리스닝함
- Vite가 자동으로 디스패치하는 이벤트
  - `'vite:beforeUpdate'`: 업데이트 적용 직전
  - `'vite:afterUpdate'`: 업데이트 적용 직후
  - `'vite:beforeFullReload'`: 전체 리로드 직전
  - `'vite:beforePrune'`: 더 이상 필요 없는 모듈 프루닝 직전
  - `'vite:invalidate'`: `hot.invalidate()`로 모듈 무효화 시
  - `'vite:error'`: 에러 발생 시 (구문 에러 등)
  - `'vite:ws:disconnect'`: WebSocket 연결 끊김
  - `'vite:ws:connect'`: WebSocket 연결 (재)수립
- 플러그인에서 커스텀 이벤트를 전송할 수도 있음

### hot.off(event, cb)

- 이벤트 리스너에서 콜백을 제거함

### hot.send(event, data)

- Vite dev 서버로 커스텀 이벤트를 전송함
- 연결 전에 호출하면 데이터가 버퍼링되어 연결 수립 후 전송됨

---

## JavaScript API

> 원문: https://vite.dev/guide/api-javascript

### 개요

- Vite의 JavaScript API는 완전히 타입이 지정되어 있음
- TypeScript 사용 또는 VS Code에서 JS 타입 체크 활성화를 권장함

### createServer

- 시그니처: `async function createServer(inlineConfig?: InlineConfig): Promise<ViteDevServer>`
- Vite dev 서버 인스턴스를 생성함

```typescript
import { createServer } from 'vite'

const server = await createServer({
  configFile: false,
  root: import.meta.dirname,
  server: {
    port: 1337,
  },
})
await server.listen()
server.printUrls()
server.bindCLIShortcuts({ print: true })
```

- 동일 Node.js 프로세스에서 `createServer`와 `build`를 사용할 때 `process.env.NODE_ENV` 또는 `mode`를 설정해야 충돌을 방지할 수 있음
- 미들웨어 모드에서 WebSocket 프록시를 사용할 때는 부모 HTTP 서버를 제공해야 함

### InlineConfig

- `UserConfig`를 확장함
- `configFile`: config 파일 위치를 지정함 (자동 해석을 비활성화하려면 `false`로 설정)

### ResolvedConfig

- 모든 `UserConfig` 속성과 유틸리티 속성을 포함함
- `config.assetsInclude`: ID가 에셋인지 확인하는 함수
- `config.logger`: Vite 내부 로거

### ViteDevServer 인터페이스

- 주요 속성 및 메서드
  - `config`: 해석된 Vite 설정
  - `middlewares`: 커스텀 미들웨어용 Connect 앱 인스턴스
  - `httpServer`: 네이티브 Node HTTP 서버
  - `watcher`: Chokidar 파일 감시자
  - `ws`: WebSocket 서버
  - `transformRequest()`: HTTP 파이프라인 없이 URL을 변환함
  - `transformIndexHtml()`: HTML 변환을 적용함
  - `ssrLoadModule()`: SSR용 모듈을 로드함
  - `listen()`: 서버를 시작함
  - `restart()`: 서버를 재시작함
  - `close()`: 서버를 종료함
  - `waitForRequestsIdle()`: 정적 import 처리 완료 대기 (실험적)

### build

- 시그니처: `async function build(inlineConfig?: InlineConfig): Promise<RolldownOutput | RolldownOutput[] | RolldownWatcher>`
- 프로덕션 빌드를 수행함

### preview

- 시그니처: `async function preview(inlineConfig?: InlineConfig): Promise<PreviewServer>`
- 프로덕션 빌드를 로컬에서 프리뷰함

### PreviewServer 인터페이스

- `config`: 해석된 Vite 설정
- `middlewares`: Connect 앱 인스턴스
- `httpServer`: 네이티브 Node HTTP 서버
- `resolvedUrls`: 서버 URL (리스닝 전이면 null)
- `printUrls()`: 서버 URL을 표시함
- `bindCLIShortcuts()`: CLI 키보드 단축키를 바인딩함

### resolveConfig

- 시그니처: `async function resolveConfig(inlineConfig: InlineConfig, command: 'build' | 'serve', defaultMode = 'development', defaultNodeEnv = 'development', isPreview = false): Promise<ResolvedConfig>`
- Vite 설정을 프로그래밍 방식으로 해석함

### mergeConfig

- 시그니처: `function mergeConfig(defaults: Record<string, any>, overrides: Record<string, any>, isRoot = true): Record<string, any>`
- 두 Vite 설정을 깊은 병합함
- overrides의 `null`과 `undefined` 값은 무시됨

### searchForWorkspaceRoot

- 시그니처: `function searchForWorkspaceRoot(current: string, root = searchForPackageRoot(current)): string`
- 다음을 감지하여 워크스페이스 루트를 찾음
  - package.json의 `workspaces` 필드
  - `lerna.json`, `pnpm-workspace.yaml`

### loadEnv

- 시그니처: `function loadEnv(mode: string, envDir: string, prefixes: string | string[] = 'VITE_'): Record<string, string>`
- `.env` 파일에서 환경 변수를 로드함

### normalizePath

- 시그니처: `function normalizePath(id: string): string`
- Vite 플러그인 상호운용성을 위해 경로를 정규화함

### transformWithOxc

- 시그니처: `async function transformWithOxc(code: string, filename: string, options?: OxcTransformOptions, inMap?: object): Promise<Omit<OxcTransformResult, 'errors'> & { warnings: string[] }>`
- Oxc Transformer를 사용하여 JavaScript/TypeScript를 변환함

### transformWithEsbuild (deprecated)

- 시그니처: `async function transformWithEsbuild(code: string, filename: string, options?: EsbuildTransformOptions, inMap?: object): Promise<ESBuildTransformResult>`
- esbuild를 사용하여 JavaScript/TypeScript를 변환함
- `transformWithOxc` 사용을 권장함

### loadConfigFromFile

- 시그니처: `async function loadConfigFromFile(configEnv: ConfigEnv, configFile?: string, configRoot: string = process.cwd(), logLevel?: LogLevel, customLogger?: Logger): Promise<{path: string, config: UserConfig, dependencies: string[]} | null>`
- Vite 설정 파일을 수동으로 로드함

### preprocessCSS (experimental)

- 시그니처: `async function preprocessCSS(code: string, filename: string, config: ResolvedConfig): Promise<PreprocessCSSResult>`
- CSS 및 CSS 전처리기 파일을 일반 CSS로 전처리함
- 반환값: `code`, `map`, `modules` (CSS 모듈용), `deps`

### 버전 속성

- `version`: 현재 Vite 버전 문자열
- `rolldownVersion`: Vite가 사용하는 Rolldown 버전
- `esbuildVersion`: 하위 호환성을 위해 유지됨
- `rollupVersion`: 하위 호환성을 위해 유지됨

---

## Environment API

> 원문: https://vite.dev/guide/api-environment

### 개요

- Vite 6에서 환경(Environment) 개념을 공식화함
- Vite 5까지는 `client`와 선택적 `ssr` 두 가지 암묵적 환경만 존재했음
- 현재 릴리스 후보(release candidate) 단계이며 향후 메이저 릴리스에서 안정화 예정임

### 핵심 개념

- 여러 환경(브라우저, Node 서버, 엣지 서버)을 동시에 구성하여 개발 중 프로덕션 설정과 밀접하게 일치시킬 수 있음
- 단일 Vite dev 서버가 여러 환경에서 코드를 동시에 실행할 수 있음

### 기본 설정 (SPA/MPA)

- Vite 5와 유사한 설정을 유지함

```javascript
export default defineConfig({
  build: {
    sourcemap: false,
  },
  optimizeDeps: {
    include: ['lib'],
  },
})
```

### 다중 환경 설정

```javascript
export default {
  build: {
    sourcemap: false,
  },
  optimizeDeps: {
    include: ['lib'],
  },
  environments: {
    server: {},
    edge: {
      resolve: {
        noExternal: true,
      },
    },
  },
}
```

- 새 환경은 최상위 config를 상속하며, 오버라이드하지 않는 한 유지됨
- `optimizeDeps` 같은 일부 최상위 옵션은 `client` 환경에만 적용됨

### 타입 인터페이스

```typescript
interface EnvironmentOptions {
  define?: Record<string, any>
  resolve?: EnvironmentResolveOptions
  optimizeDeps: DepOptimizationOptions
  consumer?: 'client' | 'server'
  dev: DevOptions
  build: BuildOptions
}

interface UserConfig extends EnvironmentOptions {
  environments: Record<string, EnvironmentOptions>
}
```

### 커스텀 환경

- 런타임 제공자가 특화된 환경을 제공할 수 있음

```javascript
import { customEnvironment } from 'vite-environment-provider'

export default {
  build: {
    outDir: '/dist/client',
  },
  environments: {
    ssr: customEnvironment({
      build: {
        outDir: '/dist/ssr',
      },
    }),
  },
}
```

### 하위 호환성

- 현재 Vite 5 서버 API는 사용 중단되지 않았으며 Vite 5와 하위 호환됨
- 향후 메이저 버전에서 사용 중단이 계획되어 있으며 즉시 제거는 아님

### 대상 독자

- 최종 사용자: 기본 환경 개념
- 플러그인 작성자: 환경 상호작용을 위한 확장 API
- 프레임워크 작성자: 프로그래밍 방식의 환경 설정
- 런타임 제공자: 커스텀 환경 구현

---

## Environment Instances 사용

> 원문: https://vite.dev/guide/api-environment-instances

### 개요

- Environment Instances API는 릴리스 후보 단계임
- dev 서버에서 다양한 환경(client, SSR 등)에 접근하고 상호작용할 수 있음

### 환경 접근

- 개발 중 `server.environments`를 통해 환경에 접근함

```javascript
const server = await createServer(/* options */)
const clientEnvironment = server.environments.client
clientEnvironment.transformRequest(url)
console.log(server.environments.ssr.moduleGraph)
```

### DevEnvironment 클래스

- 각 dev 환경은 `DevEnvironment` 인스턴스임
- 주요 멤버
  - `name`: 고유 식별자 (기본값 "client" 또는 "ssr")
  - `hot`: 메시지 송수신용 채널
  - `moduleGraph`: import 관계를 가진 모듈 노드의 그래프
  - `plugins`: 환경에 대해 해석된 플러그인
  - `pluginContainer`: resolve, load, transform 작업을 처리함
  - `config`: 해석된 설정 옵션

### 핵심 메서드

#### transformRequest(url)

- URL을 id로 해석하고, 로드한 뒤, 플러그인 파이프라인을 사용하여 코드를 처리함
- 모듈 그래프도 업데이트됨

#### warmupRequest(url)

- 낮은 우선순위로 처리할 요청을 등록함
- 워터폴을 방지하는 데 유용함

#### fetchModule(id, importer?, options?)

- 모듈 러너가 지정된 모듈에 대한 정보를 가져오기 위해 호출함

### 모듈 그래프

- 각 환경은 격리된 모듈 그래프(`EnvironmentModuleGraph`)를 유지함
- 모듈 노드는 `EnvironmentModuleNode` 인스턴스이며 다음을 포함함
  - URL, ID, 파일 정보
  - Import/importer 관계
  - 변환 결과
  - HMR 수락 데이터
- 주요 메서드: `getModuleByUrl()`, `getModuleById()`, `invalidateModule()`, `invalidateAll()`

### Fetch 결과 타입

- `fetchModule()` 메서드는 세 가지 결과 타입 중 하나를 반환함
  - `CachedFetchResult`: 수정되지 않은 모듈 상태를 확인함
  - `ExternalFetchResult`: 네이티브 런타임 import 사용을 지시함
  - `ViteFetchResult`: 코드, 파일 경로, 모듈 ID, 무효화 상태를 포함함

---

## Environment API for Plugins

> 원문: https://vite.dev/guide/api-environment-plugins

### Per-environment 훅과 Global 훅

- Global 훅: 전체 서버에 대해 한 번 실행되며, 구성된 환경과 무관함. `this.environment` 참조가 무의미함. 앱 전체 관심사(config 해석, 서버 설정 등)를 처리함
- Per-environment 훅: 환경마다 한 번 실행되며, `this.environment`를 통해 현재 환경에 접근 가능함. 모든 Rolldown 훅과 모듈을 처리하는 Vite 전용 훅이 해당됨
- `buildStart`와 `buildEnd`는 `perEnvironmentStartEndDuringDev: true` 플래그 없이는 client 환경만 대상으로 함

### 훅에서 현재 환경 접근

- Vite 5에서는 `ssr` 불리언으로 환경을 구분했음 (client와 ssr만 가능)
- 구성 가능한 환경에서는 `this.environment`로 환경 옵션과 인스턴스에 균일하게 접근함
- 플러그인 훅에서 `this.environment.config.resolve.conditions` 등에 접근 가능함

### 새 환경 등록

- 플러그인은 `config` 훅을 통해 환경을 추가함

```typescript
config(config: UserConfig) {
  return {
    environments: {
      rsc: {
        resolve: {
          conditions: ['react-server', ...defaultServerConditions],
        },
      },
    },
  }
}
```

- 빈 객체로 환경을 등록하면 기본 최상위 설정 값을 사용함

### configEnvironment 훅

- 타입: `(name: string, config: EnvironmentOptions, env: { mode: string, command: 'build' | 'serve', isSsrBuild?: boolean, isPreview?: boolean, isSsrTargetWebworker?: boolean }) => EnvironmentOptions | null | void`
- 종류: async, sequential
- 범위: Per-environment
- `config` 훅은 모든 환경이 해석되기 전에 실행됨
- `configEnvironment` 훅은 기본값을 포함한 부분 해석 설정으로 각 환경에 대해 실행됨

```typescript
configEnvironment(name: string, options: EnvironmentOptions) {
  if (name === 'rsc') {
    return {
      resolve: {
        conditions: ['workerd'],
      },
    }
  }
}
```

### hotUpdate 훅

- 타입: `(this: { environment: DevEnvironment }, options: HotUpdateOptions) => Array<EnvironmentModuleNode> | void | Promise<Array<EnvironmentModuleNode> | void>`
- 종류: async, sequential
- 범위: Per-environment
- 환경별로 커스텀 HMR 처리를 수행함
- 파일 변경 시, `server.environments` 순서에 따라 각 환경에서 순차적으로 HMR 알고리즘이 실행됨

```typescript
interface HotUpdateOptions {
  type: 'create' | 'update' | 'delete'
  file: string
  timestamp: number
  modules: Array<EnvironmentModuleNode>
  read: () => string | Promise<string>
  server: ViteDevServer
}
```

- 컨텍스트 속성
  - `this.environment`: 업데이트를 처리하는 모듈 실행 환경
  - `modules`: 이 환경에서 영향을 받는 모듈 (단일 파일이 여러 서빙 모듈에 매핑될 수 있으므로 배열)
  - `read`: 파일 내용을 반환하는 async 함수

- 훅의 동작
  1. 영향 받는 모듈을 필터링하여 보다 정확한 HMR 수행
  2. 빈 배열을 반환하여 전체 리로드
  3. 빈 배열을 반환한 후 `this.environment.hot.send()`로 커스텀 HMR 처리

- 전체 리로드 예시

```javascript
hotUpdate({ modules, timestamp }) {
  if (this.environment.name !== 'client')
    return

  const invalidatedModules = new Set()
  for (const mod of modules) {
    this.environment.moduleGraph.invalidateModule(
      mod,
      invalidatedModules,
      timestamp,
      true
    )
  }
  this.environment.hot.send({ type: 'full-reload' })
  return []
}
```

- 커스텀 이벤트 예시

```javascript
hotUpdate() {
  if (this.environment.name !== 'client')
    return

  this.environment.hot.send({
    type: 'custom',
    event: 'special-update',
    data: {}
  })
  return []
}
```

- 클라이언트 측 핸들러

```javascript
if (import.meta.hot) {
  import.meta.hot.on('special-update', (data) => {
    // perform custom update
  })
}
```

### Per-environment 플러그인 상태

- 동일 플러그인 인스턴스가 다른 환경에서 사용되므로 키 기반 상태가 필요함
- `Map<Environment, State>`를 사용하여 환경별 별도 상태를 유지함

```javascript
function PerEnvironmentCountTransformedModulesPlugin() {
  const state = new Map<Environment, { count: number }>()
  return {
    name: 'count-transformed-modules',
    perEnvironmentStartEndDuringDev: true,
    buildStart() {
      state.set(this.environment, { count: 0 })
    },
    transform(id) {
      state.get(this.environment).count++
    },
    buildEnd() {
      console.log(this.environment.name, state.get(this.environment).count)
    }
  }
}
```

- 하위 호환성을 위해 `perEnvironmentStartEndDuringDev: true` 없이 `buildStart`와 `buildEnd`는 client 환경만 대상으로 함
- `watchChange`는 `perEnvironmentWatchChangeDuringDev: true`가 필요함

### applyToEnvironment 훅

- 타입: `(environment: PartialEnvironment) => boolean | PluginOption | Promise<boolean>`
- 종류: async, sequential
- 범위: Per-environment
- 플러그인이 적용될 환경을 정의함

```javascript
const UnoCssPlugin = () => {
  return {
    buildStart() {
      // init per-environment state with WeakMap<Environment,Data>
    },
    configureServer() {
      // use global hooks normally
    },
    applyToEnvironment(environment) {
      // return true if active in this environment,
      // or return a new plugin to replace it
    },
    resolveId(id, importer) {
      // only called for applicable environments
    },
  }
}
```

- 환경 비인식 플러그인에 대한 per-environment 처리

```javascript
import { nonShareablePlugin } from 'non-shareable-plugin'

export default defineConfig({
  plugins: [
    {
      name: 'per-environment-plugin',
      applyToEnvironment(environment) {
        return nonShareablePlugin({ outputName: environment.name })
      },
    },
  ],
})
```

- `perEnvironmentPlugin` 헬퍼로 간소화 가능함

```javascript
import { nonShareablePlugin } from 'non-shareable-plugin'

export default defineConfig({
  plugins: [
    perEnvironmentPlugin('per-environment-plugin', (environment) =>
      nonShareablePlugin({ outputName: environment.name }),
    ),
  ],
})
```

- `applyToEnvironment` 훅은 config 시점에 실행되며, 현재 `configResolved` 이후에 실행됨

### 애플리케이션-플러그인 통신

- `environment.hot`으로 주어진 환경에서 플러그인-애플리케이션 통신을 활성화함 (비클라이언트 환경도 지원)
- HMR을 지원하는 환경에서만 사용 가능함

#### 애플리케이션 인스턴스 관리

- 동일 환경에서 여러 애플리케이션 인스턴스가 동시에 실행될 수 있음 (여러 브라우저 탭 등)
- `vite:client:connect` (새 연결)와 `vite:client:disconnect` (닫힌 연결) 이벤트가 `environment.hot`에서 발생함
- 이벤트 핸들러는 인스턴스별 메시지 전송을 위한 `send` 메서드가 있는 `NormalizedHotChannelClient`를 받음

#### 사용 예시

- 플러그인 측

```javascript
configureServer(server) {
  server.environments.ssr.hot.on('my:greetings', (data, client) => {
    client.send('my:foo:reply', `Hello from server! You said: ${data}`)
  })

  server.environments.ssr.hot.send('my:foo', 'Hello from server!')
}
```

- 애플리케이션 측에서는 `import.meta.hot`을 통한 표준 클라이언트-서버 통신을 사용함

### 빌드 훅에서의 환경

- 빌드 중 플러그인 훅은 `ssr` 불리언 대신 환경 인스턴스를 받음
- `renderChunk`, `generateBundle` 등 빌드 전용 훅에 적용됨

### 빌드 중 공유 플러그인

- Vite 6 이전: dev에서는 공유 플러그인, 빌드에서는 환경별 격리 (별도 프로세스)
- Vite 6: 모든 환경이 단일 프로세스에서 빌드되어 dev와 빌드 플러그인 파이프라인이 정렬됨
- 기본적으로 하위 호환성을 위해 환경별로 새 `ResolvedConfig`를 생성함
- `builder.sharedConfigBuild: true`로 전체 config/파이프라인 공유에 옵트인 가능함

```javascript
function myPlugin() {
  const sharedState = ...
  return {
    name: 'shared-plugin',
    transform(code, id) { ... },
    sharedDuringBuild: true,
  }
}
```

---

## Environment API for Frameworks

> 원문: https://vite.dev/guide/api-environment-frameworks

### 개요

- 릴리스 후보 단계의 API임
- 프레임워크가 다양한 런타임에서 Vite 환경과 통신하는 데 도움을 줌

### 세 가지 통신 레벨

#### RunnableDevEnvironment

- Vite 서버와 동일한 프로세스에서 실행되어 임의의 JavaScript 값을 통신할 수 있음

```typescript
export class RunnableDevEnvironment extends DevEnvironment {
  public readonly runner: ModuleRunner
}

class ModuleRunner {
  public async import(url: string): Promise<Record<string, any>>
}
```

- SSR 미들웨어 사용 예시

```javascript
import fs from 'node:fs'
import path from 'node:path'
import { createServer } from 'vite'

const viteServer = await createServer({
  server: { middlewareMode: true },
  appType: 'custom',
  environments: {
    server: {
      // modules run in same process as vite server
    },
  },
})

const serverEnvironment = viteServer.environments.server

app.use('*', async (req, res, next) => {
  const url = req.originalUrl
  const indexHtmlPath = path.resolve(import.meta.dirname, 'index.html')
  let template = fs.readFileSync(indexHtmlPath, 'utf-8')

  template = await viteServer.transformIndexHtml(url, template)

  const { render } = await serverEnvironment.runner.import(
    '/src/entry-server.js',
  )

  const appHtml = await render(url)
  const html = template.replace(`<!--ssr-outlet-->`, appHtml)

  res.status(200).set({ 'Content-Type': 'text/html' }).end(html)
})
```

- 서버 엔트리 파일 베스트 프랙티스

```javascript
export function render(...) { ... }

if (import.meta.hot) {
  import.meta.hot.accept()
}
```

#### FetchableDevEnvironment

- Fetch API 표준을 사용하며, Vite를 직접 실행할 수 없는 런타임(Cloudflare Workers 등)을 지원함

```typescript
import {
  createServer,
  createFetchableDevEnvironment,
  isFetchableDevEnvironment,
} from 'vite'

const server = await createServer({
  server: { middlewareMode: true },
  appType: 'custom',
  environments: {
    custom: {
      dev: {
        createEnvironment(name, config) {
          return createFetchableDevEnvironment(name, config, {
            handleRequest(request: Request): Promise<Response> | Response {
              // handle Request and return a Response
            },
          })
        },
      },
    },
  },
})

if (isFetchableDevEnvironment(server.environments.custom)) {
  const response: Response = await server.environments.custom.dispatchFetch(
    new Request('http://example.com/request-to-handle'),
  )
}
```

#### Raw DevEnvironment

- 커스텀 구현을 위한 두 가지 접근 방식이 있음

- 가상 모듈 접근 방식

```typescript
import { createServer } from 'vite'

const server = createServer({
  plugins: [
    {
      name: 'virtual-module',
      /* plugin implementation */
    },
  ],
})
const ssrEnvironment = server.environment.ssr

if (ssrEnvironment instanceof CustomDevEnvironment) {
  ssrEnvironment.runEntrypoint('virtual:entrypoint')
}
```

- transformIndexHtml을 위한 가상 모듈 플러그인

```typescript
function vitePluginVirtualIndexHtml(): Plugin {
  let server: ViteDevServer | undefined
  return {
    name: vitePluginVirtualIndexHtml.name,
    configureServer(server_) {
      server = server_
    },
    resolveId(source) {
      return source === 'virtual:index-html' ? '\0' + source : undefined
    },
    async load(id) {
      if (id === '\0' + 'virtual:index-html') {
        let html: string
        if (server) {
          this.addWatchFile('index.html')
          html = fs.readFileSync('index.html', 'utf-8')
          html = await server.transformIndexHtml('/', html)
        } else {
          html = fs.readFileSync('dist/client/index.html', 'utf-8')
        }
        return `export default ${JSON.stringify(html)}`
      }
      return
    },
  }
}
```

- Hot 모듈 메시지 접근 방식 (Node.js API용)

```typescript
import { createServer } from 'vite'

const server = createServer({
  plugins: [
    {
      name: 'virtual-module',
    },
  ],
})
const ssrEnvironment = server.environment.ssr

if (ssrEnvironment instanceof RunnableDevEnvironment) {
  ssrEnvironment.runner.import('virtual:entrypoint')
}

const req = new Request('http://example.com/')
const uniqueId = 'a-unique-id'
ssrEnvironment.send('request', serialize({ req, uniqueId }))
const response = await new Promise((resolve) => {
  ssrEnvironment.on('response', (data) => {
    data = deserialize(data)
    if (data.uniqueId === uniqueId) {
      resolve(data.res)
    }
  })
})
```

### 빌드 시점 환경 처리

#### 레거시 동작

- `vite build`와 `vite build --ssr`은 하위 호환성을 유지하며 각각 클라이언트 전용과 SSR 전용 빌드를 수행함

#### App Build 모드

- `builder`가 설정되면 `vite build`가 모든 구성된 환경을 빌드함

```javascript
import { defineConfig } from 'vite'

export default defineConfig({
  builder: {
    buildApp: async (builder) => {
      const environments = Object.values(builder.environments)
      await Promise.all(
        environments.map((environment) => builder.build(environment)),
      )
    },
  },
})
```

#### buildApp 플러그인 훅

- 타입: `(this: MinimalPluginContextWithoutEnvironment, builder: ViteBuilder) => Promise<void>`
- 설정 옵션과 함께 앱 빌드에 참여함
- 실행 순서: pre 훅 -> config 옵션 -> post 훅
- 중복 빌드를 방지하려면 `environment.isBuilt`를 사용함

#### 프로그래밍 방식 빌드

```javascript
import { createBuilder } from 'vite'

const builder = await createBuilder()
await builder.buildApp()
```

- `createBuilder`는 `createServer`의 빌드 시점 대응물이며, 환경 인식 빌드를 위해 레거시 `build` 함수를 대체함

### 환경 비의존적 코드

- 대부분의 코드는 컨텍스트(플러그인 훅의 `this.environment` 등)를 통해 현재 환경에 접근할 수 있으므로, `server.environments`에 직접 접근하는 경우는 드묾

---

## Environment API for Runtimes

> 원문: https://vite.dev/guide/api-environment-runtimes

### 개요

- 런타임 제공자를 위한 API임
- 런타임이란 변환된 코드가 실행되는 JavaScript 엔진(Node.js, 브라우저, Cloudflare workerd, Worker 스레드 등)을 의미함

### 환경 팩토리

- 런타임 제공자가 `EnvironmentOptions`를 반환하는 팩토리를 생성함
- dev 및 빌드 환경 모두를 설정하며 최종 사용자 설정이 필요 없음
- 최종 사용자가 아닌 런타임 제공자가 구현하도록 의도됨

```typescript
function createWorkerdEnvironment(
  userConfig: EnvironmentOptions,
): EnvironmentOptions {
  return mergeConfig(
    {
      resolve: {
        conditions: [/*...*/],
      },
      dev: {
        createEnvironment(name, config) {
          return createWorkerdDevEnvironment(name, config, {
            hot: true,
            transport: customHotChannel(),
          })
        },
      },
      build: {
        createEnvironment(name, config) {
          return createWorkerdBuildEnvironment(name, config)
        },
      },
    },
    userConfig,
  )
}
```

### 새 환경 팩토리 생성

- Vite dev 서버는 기본적으로 `client` (브라우저 기반)와 `ssr` (Node 런타임) 두 가지 환경을 노출함
- 아키텍처 구성 요소
  - Module: 변환된 소스 코드
  - Module Graph: 처리된 모듈 간의 관계
  - Module Runner: Vite 플러그인 처리 후 코드를 실행하도록 하는 컴포넌트
- Module Runner는 `server.ssrLoadModule`과 달리 러너 구현이 서버에서 분리되어 있음

#### 커스텀 DevEnvironment 생성

```typescript
import { DevEnvironment, HotChannel } from 'vite'

function createWorkerdDevEnvironment(
  name: string,
  config: ResolvedConfig,
  context: DevEnvironmentContext
) {
  const connection = /* ... */
  const transport: HotChannel = {
    on: (listener) => { connection.on('message', listener) },
    send: (data) => connection.send(data),
  }

  const workerdDevEnvironment = new DevEnvironment(name, config, {
    options: {
      resolve: { conditions: ['custom'] },
      ...context.options,
    },
    hot: true,
    transport,
  })
  return workerdDevEnvironment
}
```

- 기본적으로 `HotChannel` 전송에는 `server.fs` 제한이 적용됨
- 네트워크를 통해 노출되지 않는 전송(워커 스레드, 인프로세스 호출 등)에서는 `skipFsCheck: true`를 설정하여 제한을 우회할 수 있음

### ModuleRunner

- 대상 런타임에서 인스턴스화됨
- API는 `vite/module-runner`에서 import함

#### 타입 시그니처

```typescript
export class ModuleRunner {
  constructor(
    public options: ModuleRunnerOptions,
    public evaluator: ModuleEvaluator = new ESModulesEvaluator(),
    private debug?: ModuleRunnerDebugger,
  ) {}

  /**
   * URL to execute.
   * Accepts file path, server path, or id relative to the root.
   */
  public async import<T = any>(url: string): Promise<T>

  /**
   * Clear all caches including HMR listeners.
   */
  public clearCache(): void

  /**
   * Clear all caches, remove all HMR listeners, reset sourcemap support.
   * This method doesn't stop the HMR connection.
   */
  public async close(): Promise<void>

  /**
   * Returns `true` if the runner has been closed by calling `close()`.
   */
  public isClosed(): boolean
}
```

- `ESModulesEvaluator`는 `new AsyncFunction`을 사용하여 코드를 평가함
- JavaScript 런타임이 unsafe evaluation을 지원하지 않으면 자체 구현을 제공할 수 있음

#### 사용 예시

```javascript
import {
  ModuleRunner,
  ESModulesEvaluator,
  createNodeImportMeta,
} from 'vite/module-runner'
import { transport } from './rpc-implementation.js'

const moduleRunner = new ModuleRunner(
  {
    transport,
    createImportMeta: createNodeImportMeta, // if the module runner runs in Node.js
  },
  new ESModulesEvaluator(),
)

await moduleRunner.import('/src/entry-point.js')
```

### ModuleRunnerOptions

```typescript
interface ModuleRunnerOptions {
  /**
   * A set of methods to communicate with the server.
   */
  transport: ModuleRunnerTransport

  /**
   * Configure how source maps are resolved.
   * Prefers `node` if `process.setSourceMapsEnabled` is available.
   * Otherwise it will use `prepareStackTrace` by default which overrides
   * `Error.prepareStackTrace` method.
   * You can provide an object to configure how file contents and
   * source maps are resolved for files that were not processed by Vite.
   */
  sourcemapInterceptor?:
    false | 'node' | 'prepareStackTrace' | InterceptorOptions

  /**
   * Disable HMR or configure HMR options.
   *
   * @default true
   */
  hmr?: boolean | ModuleRunnerHmr

  /**
   * Custom module cache. If not provided, it creates a separate module
   * cache for each module runner instance.
   */
  evaluatedModules?: EvaluatedModules
}
```

### ModuleEvaluator

```typescript
export interface ModuleEvaluator {
  /**
   * Number of prefixed lines in the transformed code.
   */
  startOffset?: number

  /**
   * Evaluate code that was transformed by Vite.
   * @param context Function context
   * @param code Transformed code
   * @param id ID that was used to fetch the module
   */
  runInlinedModule(
    context: ModuleRunnerContext,
    code: string,
    id: string,
  ): Promise<any>

  /**
   * evaluate externalized module.
   * @param file File URL to the external module
   */
  runExternalModule(file: string): Promise<any>
}
```

- `ESModulesEvaluator`는 `new AsyncFunction`을 사용하여 코드를 평가하므로, 인라인 소스맵이 있으면 추가된 새 줄에 맞춰 2줄 오프셋이 필요함

### ModuleRunnerTransport

```typescript
interface ModuleRunnerTransport {
  connect?(handlers: ModuleRunnerTransportHandlers): Promise<void> | void
  disconnect?(): Promise<void> | void
  send?(data: HotPayload): Promise<void> | void
  invoke?(data: HotPayload): Promise<{ result: any } | { error: any }>
  timeout?: number
}
```

- `invoke` 메서드가 구현되지 않으면 `send` 메서드와 `connect` 메서드가 반드시 구현되어야 함
- Vite가 내부적으로 `invoke`를 구성함

#### Worker 스레드 구현 예시

- worker.js

```javascript
import { parentPort } from 'node:worker_threads'
import { fileURLToPath } from 'node:url'
import {
  ESModulesEvaluator,
  ModuleRunner,
  createNodeImportMeta,
} from 'vite/module-runner'

/** @type {import('vite/module-runner').ModuleRunnerTransport} */
const transport = {
  connect({ onMessage, onDisconnection }) {
    parentPort.on('message', onMessage)
    parentPort.on('close', onDisconnection)
  },
  send(data) {
    parentPort.postMessage(data)
  },
}

const runner = new ModuleRunner(
  {
    transport,
    createImportMeta: createNodeImportMeta,
  },
  new ESModulesEvaluator(),
)
```

- server.js

```javascript
import { BroadcastChannel } from 'node:worker_threads'
import { createServer, DevEnvironment } from 'vite'

function createWorkerEnvironment(name, config, context) {
  const worker = new Worker('./worker.js')
  const handlerToWorkerListener = new WeakMap()
  const client = {
    send(payload: HotPayload) {
      worker.postMessage(payload)
    },
  }

  const workerHotChannel = {
    skipFsCheck: true,
    send: (data) => worker.postMessage(data),
    on: (event, handler) => {
      if (event === 'vite:client:connect') return
      if (event === 'vite:client:disconnect') {
        const listener = () => {
          handler(undefined, client)
        }
        handlerToWorkerListener.set(handler, listener)
        worker.on('exit', listener)
        return
      }

      const listener = (value) => {
        if (value.type === 'custom' && value.event === event) {
          handler(value.data, client)
        }
      }
      handlerToWorkerListener.set(handler, listener)
      worker.on('message', listener)
    },
    off: (event, handler) => {
      if (event === 'vite:client:connect') return
      if (event === 'vite:client:disconnect') {
        const listener = handlerToWorkerListener.get(handler)
        if (listener) {
          worker.off('exit', listener)
          handlerToWorkerListener.delete(handler)
        }
        return
      }

      const listener = handlerToWorkerListener.get(handler)
      if (listener) {
        worker.off('message', listener)
        handlerToWorkerListener.delete(handler)
      }
    },
  }

  return new DevEnvironment(name, config, {
    transport: workerHotChannel,
  })
}

await createServer({
  environments: {
    worker: {
      dev: {
        createEnvironment: createWorkerEnvironment,
      },
    },
  },
})
```

- `on`/`off` 메서드에서 `vite:client:connect`/`vite:client:disconnect` 이벤트를 반드시 구현해야 함

#### HTTP 요청 구현 예시

```typescript
import { ESModulesEvaluator, ModuleRunner } from 'vite/module-runner'

export const runner = new ModuleRunner(
  {
    transport: {
      async invoke(data) {
        const response = await fetch(`http://my-vite-server/invoke`, {
          method: 'POST',
          body: JSON.stringify(data),
        })
        return response.json()
      },
    },
    hmr: false, // disable HMR as HMR requires transport.connect
  },
  new ESModulesEvaluator(),
)

await runner.import('/entry.js')
```

- 서버 측 핸들러

```typescript
const customEnvironment = new DevEnvironment(name, config, context)

server.onRequest((request: Request) => {
  const url = new URL(request.url)
  if (url.pathname === '/invoke') {
    const payload = (await request.json()) as HotPayload
    const result = customEnvironment.hot.handleInvoke(payload)
    return new Response(JSON.stringify(result))
  }
  return Response.error()
})
```

#### createServerHotChannel을 사용한 SSR 환경

- Vite 서버와 동일한 Node.js 프로세스에서 실행되는 SSR 환경용

```javascript
import { createServerHotChannel, DevEnvironment } from 'vite'

new DevEnvironment(name, config, {
  hot: true,
  transport: createServerHotChannel(),
})
```

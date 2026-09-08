# Vue 3 애플리케이션, 컴포지션 및 반응성 API

애플리케이션 생성, 반응형 상태, 라이프사이클과 의존성 주입에 사용하는 API를 모았습니다. 각 항목에서 타입과 반환값, 사용 예제 및 호출 조건을 함께 확인할 수 있습니다.

<a id="api-index-composition-api"></a>

## 목차

- [애플리케이션 API](#api-application)
- [글로벌 API: 일반](#api-general)
- [컴포지션 API: setup()](#api-composition-api-setup)
- [반응성 API: 코어](#api-reactivity-core)
- [반응성 API: 유틸리티](#api-reactivity-utilities)
- [반응성 API: 고급](#api-reactivity-advanced)
- [컴포지션 API: 라이프사이클 훅](#api-composition-api-lifecycle)
- [Composition API: <br>의존성 주입(dependency injection)](#api-composition-api-dependency-injection)
- [Composition API: Helpers](#api-composition-api-helpers)

---

<a id="api-application"></a>

<a id="api-application-application-api"></a>

## 애플리케이션 API

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/application.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/application.md

<a id="api-application-createapp"></a>

### createApp()
애플리케이션 인스턴스(instance)를 생성합니다.

- **타입**

  ```ts
  function createApp(rootComponent: Component, rootProps?: object): App
  ```

- **세부사항**

  첫 번째 인자는 루트 컴포넌트입니다. 두 번째 선택적 인자는 루트 컴포넌트에 전달할 props입니다.

- **예시**

  인라인 루트 컴포넌트 사용:

  ```js
  import { createApp } from 'vue'

  const app = createApp({
    /* 루트 컴포넌트 옵션 */
  })
  ```

  임포트한 컴포넌트(component) 사용:

  ```js
  import { createApp } from 'vue'
  import App from './App.vue'

  const app = createApp(App)
  ```

- **관련 문서** [가이드 - Vue 애플리케이션 생성](02_essentials.md#guide-essentials-application)

<a id="api-application-createssrapp"></a>

### createSSRApp()
[SSR 하이드레이션(hydration)](05_scaling_typescript_and_best_practices.md#guide-scaling-up-ssr-client-hydration) 모드에서 애플리케이션 인스턴스를 생성합니다. 사용법은 `createApp()`과 완전히 동일합니다.

<a id="api-application-app-mount"></a>

### app.mount()
애플리케이션 인스턴스를 컨테이너 엘리먼트에 마운트(mount)합니다.

- **타입**

  ```ts
  interface App {
    mount(rootContainer: Element | string): ComponentPublicInstance
  }
  ```

- **세부사항**

  인자는 실제 DOM 엘리먼트이거나 CSS 선택자일 수 있습니다(첫 번째로 일치하는 엘리먼트가 사용됩니다). 루트 컴포넌트 인스턴스를 반환합니다.

  컴포넌트에 템플릿(template)이나 렌더 함수가 정의되어 있으면, 컨테이너 내부의 기존 DOM 노드를 대체합니다. 그렇지 않고 런타임 컴파일러가 사용 가능한 경우, 컨테이너의 `innerHTML`이 템플릿으로 사용됩니다.

  SSR 하이드레이션 모드에서는 컨테이너 내부의 기존 DOM 노드를 하이드레이트합니다. [불일치](05_scaling_typescript_and_best_practices.md#guide-scaling-up-ssr-hydration-mismatch)가 있는 경우, 기존 DOM 노드가 예상 출력에 맞게 변형됩니다.

  각 앱 인스턴스마다 `mount()`는 한 번만 호출할 수 있습니다.

- **예시**

  ```js
  import { createApp } from 'vue'
  const app = createApp(/* ... */)

  app.mount('#app')
  ```

  실제 DOM 엘리먼트에 마운트할 수도 있습니다:

  ```js
  app.mount(document.body.firstChild)
  ```

<a id="api-application-app-unmount"></a>

### app.unmount()
마운트된 애플리케이션 인스턴스를 언마운트(unmount)하며, 애플리케이션의 컴포넌트 트리 내 모든 컴포넌트에 대한 언마운트 라이프사이클(lifecycle) 훅을 트리거합니다.

- **타입**

  ```ts
  interface App {
    unmount(): void
  }
  ```

<a id="api-application-app-onunmount"></a>

### app.onUnmount()  (3.5+)
앱이 언마운트될 때 호출될 콜백(callback)을 등록합니다.

- **타입**

  ```ts
  interface App {
    onUnmount(callback: () => any): void
  }
  ```

<a id="api-application-app-component"></a>

### app.component()
이름 문자열과 컴포넌트 정의를 모두 전달하면 전역 컴포넌트를 등록하고, 이름만 전달하면 이미 등록된 컴포넌트를 반환합니다.

- **타입**

  ```ts
  interface App {
    component(name: string): Component | undefined
    component(name: string, component: Component): this
  }
  ```

- **예시**

  ```js
  import { createApp } from 'vue'

  const app = createApp({})

  // 옵션 객체 등록
  app.component('MyComponent', {
    /* ... */
  })

  // 등록된 컴포넌트 가져오기
  const MyComponent = app.component('MyComponent')
  ```

- **관련 문서** [컴포넌트 등록](03_components_and_reusability.md#guide-components-registration)

<a id="api-application-app-directive"></a>

### app.directive()
이름 문자열과 디렉티브(directive) 정의를 모두 전달하면 전역 커스텀 디렉티브를 등록하고, 이름만 전달하면 이미 등록된 디렉티브를 반환합니다.

- **타입**

  ```ts
  interface App {
    directive(name: string): Directive | undefined
    directive(name: string, directive: Directive): this
  }
  ```

- **예시**

  ```js
  import { createApp } from 'vue'

  const app = createApp({
    /* ... */
  })

  // 등록 (객체 디렉티브)
  app.directive('myDirective', {
    /* 커스텀 디렉티브 훅 */
  })

  // 등록 (함수 디렉티브 단축형)
  app.directive('myDirective', () => {
    /* ... */
  })

  // 등록된 디렉티브 가져오기
  const myDirective = app.directive('myDirective')
  ```

- **관련 문서** [커스텀 디렉티브](03_components_and_reusability.md#guide-reusability-custom-directives)

<a id="api-application-app-use"></a>

### app.use()
[플러그인(plugin)](03_components_and_reusability.md#guide-reusability-plugins)을 설치합니다.

- **타입**

  ```ts
  interface App {
    use(plugin: Plugin, ...options: any[]): this
  }
  ```

- **세부사항**

  첫 번째 인자로 플러그인을, 두 번째 인자로 선택적 플러그인 옵션을 받습니다.

  플러그인은 `install()` 메서드를 가진 객체이거나, `install()` 메서드로 사용할 함수일 수 있습니다. 옵션(`app.use()`의 두 번째 인자)은 플러그인의 `install()` 메서드에 전달됩니다.

  동일한 플러그인에 대해 `app.use()`가 여러 번 호출되어도 플러그인은 한 번만 설치됩니다.

- **예시**

  ```js
  import { createApp } from 'vue'
  import MyPlugin from './plugins/MyPlugin'

  const app = createApp({
    /* ... */
  })

  app.use(MyPlugin)
  ```

- **관련 문서** [플러그인](03_components_and_reusability.md#guide-reusability-plugins)

<a id="api-application-app-mixin"></a>

### app.mixin()
전역 믹스인(애플리케이션 범위)을 적용합니다. 전역 믹스인은 포함된 옵션을 애플리케이션 내 모든 컴포넌트 인스턴스에 적용합니다.

**권장하지 않음**
믹스인(mixin)은 생태계 라이브러리에서 널리 사용되어 왔기 때문에, Vue 3에서는 주로 하위 호환성을 위해 지원됩니다. 애플리케이션 코드에서 믹스인, 특히 전역 믹스인 사용은 피해야 합니다.

로직 재사용을 위해서는 [컴포저블(composable)](03_components_and_reusability.md#guide-reusability-composables)을 사용하는 것이 더 좋습니다.


- **타입**

  ```ts
  interface App {
    mixin(mixin: ComponentOptions): this
  }
  ```

<a id="api-application-app-provide"></a>

### app.provide()
애플리케이션 내 모든 하위 컴포넌트에서 주입할 수 있는 값을 제공합니다.

- **타입**

  ```ts
  interface App {
    provide<T>(key: InjectionKey<T> | symbol | string, value: T): this
  }
  ```

- **세부사항**

  첫 번째 인자로 주입 키를, 두 번째 인자로 제공할 값을 받습니다. 애플리케이션 인스턴스 자체를 반환합니다.

- **예시**

  ```js
  import { createApp } from 'vue'

  const app = createApp(/* ... */)

  app.provide('message', 'hello')
  ```

  애플리케이션 내 컴포넌트에서:


**컴포지션 API**


  ```js
  import { inject } from 'vue'

  export default {
    setup() {
      console.log(inject('message')) // 'hello'
    }
  }
  ```



**옵션 API**


  ```js
  export default {
    inject: ['message'],
    created() {
      console.log(this.message) // 'hello'
    }
  }
  ```



- **관련 문서**
  - [Provide / Inject](03_components_and_reusability.md#guide-components-provide-inject)
  - [앱 레벨 Provide](03_components_and_reusability.md#guide-components-provide-inject-app-level-provide)
  - [app.runWithContext()](07_composition_and_reactivity_apis.md#api-application-app-runwithcontext)

<a id="api-application-app-runwithcontext"></a>

### app.runWithContext()
- 3.3+에서만 지원

현재 앱을 주입 컨텍스트로 하여 콜백을 실행합니다.

- **타입**

  ```ts
  interface App {
    runWithContext<T>(fn: () => T): T
  }
  ```

- **세부사항**

  콜백 함수를 받아서 즉시 실행합니다. 콜백의 동기 호출 중에는, 현재 활성 컴포넌트 인스턴스가 없어도 `inject()` 호출이 현재 앱에서 제공한 값을 조회할 수 있습니다. 콜백의 반환값도 그대로 반환됩니다.

- **예시**

  ```js
  import { inject } from 'vue'

  app.provide('id', 1)

  const injected = app.runWithContext(() => {
    return inject('id')
  })

  console.log(injected) // 1
  ```

<a id="api-application-app-version"></a>

### app.version
애플리케이션이 생성된 Vue의 버전을 제공합니다. 이는 [플러그인](03_components_and_reusability.md#guide-reusability-plugins) 내부에서, 다양한 Vue 버전에 따라 조건부 로직이 필요할 때 유용합니다.

- **타입**

  ```ts
  interface App {
    version: string
  }
  ```

- **예시**

  플러그인 내부에서 버전 체크 수행:

  ```js
  export default {
    install(app) {
      const version = Number(app.version.split('.')[0])
      if (version < 3) {
        console.warn('이 플러그인은 Vue 3이 필요합니다')
      }
    }
  }
  ```

- **관련 문서** [글로벌 API - version](07_composition_and_reactivity_apis.md#api-general-version)

<a id="api-application-app-config"></a>

### app.config
모든 애플리케이션 인스턴스는 해당 애플리케이션의 설정을 담고 있는 `config` 객체를 노출합니다. 애플리케이션을 마운트하기 전에 이 객체의 속성(아래에 문서화됨)을 수정할 수 있습니다.

```js
import { createApp } from 'vue'

const app = createApp(/* ... */)

console.log(app.config)
```

<a id="api-application-app-config-errorhandler"></a>

### app.config.errorHandler
애플리케이션 내에서 전파되는 잡히지 않은 에러에 대한 전역 핸들러를 지정합니다.

- **타입**

  ```ts
  interface AppConfig {
    errorHandler?: (
      err: unknown,
      instance: ComponentPublicInstance | null,
      // `info`는 Vue 전용 에러 정보입니다.
      // 예: 어떤 라이프사이클 훅에서 에러가 발생했는지 등
      info: string
    ) => void
  }
  ```

- **세부사항**

  에러 핸들러는 세 개의 인자를 받습니다: 에러, 에러를 발생시킨 컴포넌트 인스턴스, 에러 소스 타입을 지정하는 정보 문자열.

  다음 소스에서 발생한 에러를 포착할 수 있습니다:

  - 컴포넌트 렌더링(rendering)
  - 이벤트 핸들러
  - 라이프사이클 훅(hook)
  - `setup()` 함수
  - 감시자(watchers)
  - 커스텀 디렉티브 훅
  - 트랜지션(transition) 훅

  **참고**
  프로덕션에서는 3번째 인자(`info`)가 전체 정보 문자열 대신 축약된 코드가 됩니다. 코드와 문자열 매핑은 [프로덕션 에러 코드 참조](09_style_guide_examples_and_reference.md#error-reference-index-runtime-errors)에서 확인할 수 있습니다.


- **예시**

  ```js
  app.config.errorHandler = (err, instance, info) => {
    // 에러 처리, 예: 서비스에 리포트
  }
  ```

- **기본값**

  기본 에러 핸들러는 개발 환경에서는 에러를 다시 throw하고, 프로덕션 환경에서는 에러를 로그로 출력합니다.
  이 동작은 [throwUnhandledErrorInProduction](07_composition_and_reactivity_apis.md#api-application-app-config-throwunhandlederrorinproduction) 속성으로 설정할 수 있습니다.

<a id="api-application-app-config-warnhandler"></a>

### app.config.warnHandler
Vue의 런타임 경고에 대한 커스텀 핸들러를 지정합니다.

- **타입**

  ```ts
  interface AppConfig {
    warnHandler?: (
      msg: string,
      instance: ComponentPublicInstance | null,
      trace: string
    ) => void
  }
  ```

- **세부사항**

  경고 핸들러는 첫 번째 인자로 경고 메시지, 두 번째 인자로 소스 컴포넌트 인스턴스, 세 번째 인자로 컴포넌트 트레이스 문자열을 받습니다.

  특정 경고를 필터링하여 콘솔 출력이 장황해지는 것을 줄이는 데 사용할 수 있습니다. 다만 모든 Vue 경고는 개발 중에 해결되어야 하므로, 이 설정은 많은 경고 중 일부에 집중하고자 하는 디버그 세션에서만 권장되며, 디버깅이 끝나면 제거해야 합니다.

  **참고**
  경고는 개발 중에만 동작하므로, 이 설정은 프로덕션 모드에서는 무시됩니다.


- **예시**

  ```js
  app.config.warnHandler = (msg, instance, trace) => {
    // `trace`는 컴포넌트 계층 트레이스입니다
  }
  ```

<a id="api-application-app-config-performance"></a>

### app.config.performance
이 값을 `true`로 설정하면 브라우저 개발자 도구의 performance/timeline 패널에서 컴포넌트 초기화, 컴파일, 렌더, 패치 성능 추적이 활성화됩니다. 개발 모드에서만 동작하며, [performance.mark](https://developer.mozilla.org/en-US/docs/Web/API/Performance/mark) API를 지원하는 브라우저에서만 사용할 수 있습니다.

- **타입:** `boolean`

- **관련 문서** [가이드 - 성능](05_scaling_typescript_and_best_practices.md#guide-best-practices-performance)

<a id="api-application-app-config-compileroptions"></a>

### app.config.compilerOptions
런타임 컴파일러 옵션을 설정합니다. 이 객체에 설정된 값은 브라우저 내 템플릿 컴파일러에 전달되며, 해당 앱의 모든 컴포넌트에 영향을 미칩니다. 또한 [`compilerOptions` 옵션](08_component_and_advanced_apis.md#api-options-rendering-compileroptions)을 사용하여 컴포넌트별로 이 옵션을 오버라이드할 수도 있습니다.

**중요**
이 설정 옵션은 전체 빌드(즉, 브라우저에서 템플릿을 컴파일할 수 있는 독립형 `vue.js`)를 사용할 때만 적용됩니다. 빌드 설정과 함께 런타임 전용 빌드를 사용하는 경우, 컴파일러 옵션은 빌드 도구 설정을 통해 `@vue/compiler-dom`에 전달해야 합니다.

- `vue-loader`의 경우: [loader 옵션의 `compilerOptions`를 통해 전달](https://vue-loader.vuejs.org/options.html#compileroptions). [`vue-cli`에서 설정하는 방법도 참고](https://cli.vuejs.org/guide/webpack.html#modifying-options-of-a-loader).

- `vite`의 경우: [`@vitejs/plugin-vue` 옵션을 통해 전달](https://github.com/vitejs/vite-plugin-vue/tree/main/packages/plugin-vue#options).


<a id="api-application-app-config-compileroptions-iscustomelement"></a>

#### app.config.compilerOptions.isCustomElement
네이티브 커스텀 엘리먼트를 인식하는 체크 메서드를 지정합니다.

- **타입:** `(tag: string) => boolean`

- **세부사항**

  태그를 네이티브 커스텀 엘리먼트로 처리해야 한다면 `true`를 반환해야 합니다. 일치하는 태그에 대해 Vue는 해당 태그를 Vue 컴포넌트로 해석하지 않고 네이티브 엘리먼트로 렌더링합니다.

  네이티브 HTML 및 SVG 태그는 이 함수에서 일치시킬 필요가 없습니다. Vue의 파서는 이를 자동으로 인식합니다.

- **예시**

  ```js
  // 'ion-'으로 시작하는 모든 태그를 커스텀 엘리먼트로 처리
  app.config.compilerOptions.isCustomElement = (tag) => {
    return tag.startsWith('ion-')
  }
  ```

- **관련 문서** [Vue와 웹 컴포넌트](06_reactivity_and_rendering_in_depth.md#guide-extras-web-components)

<a id="api-application-app-config-compileroptions-whitespace"></a>

#### app.config.compilerOptions.whitespace
템플릿 공백 처리 동작을 조정합니다.

- **타입:** `'condense' | 'preserve'`

- **기본값:** `'condense'`

- **세부사항**

  Vue는 더 효율적인 컴파일 결과를 위해 템플릿의 공백 문자를 제거/축약합니다. 기본 전략은 "condense"이며, 다음과 같이 동작합니다:

  1. 엘리먼트 내부의 앞/뒤 공백 문자는 하나의 공백으로 축약됩니다.
  2. 줄바꿈이 포함된 엘리먼트 사이의 공백 문자는 제거됩니다.
  3. 텍스트 노드 내 연속된 공백 문자는 하나의 공백으로 축약됩니다.

  이 옵션을 `'preserve'`로 설정하면 (2)와 (3)이 비활성화됩니다.

- **예시**

  ```js
  app.config.compilerOptions.whitespace = 'preserve'
  ```

<a id="api-application-app-config-compileroptions-delimiters"></a>

#### app.config.compilerOptions.delimiters
템플릿 내 텍스트 보간(interpolation)에 사용되는 구분자를 조정합니다.

- **타입:** `[string, string]`

- **기본값:** `{{ "['\u007b\u007b', '\u007d\u007d']" }}`

- **세부사항**

  이는 이중 중괄호(mustache) 구문을 사용하는 서버 사이드 프레임워크와의 충돌을 피하기 위해 주로 사용됩니다.

- **예시**

  ```js
  // 구분자를 ES6 템플릿 문자열 스타일로 변경
  app.config.compilerOptions.delimiters = ['${', '}']
  ```

<a id="api-application-app-config-compileroptions-comments"></a>

#### app.config.compilerOptions.comments
템플릿 내 HTML 주석 처리 방식을 조정합니다.

- **타입:** `boolean`

- **기본값:** `false`

- **세부사항**

  기본적으로 Vue는 프로덕션에서 주석을 제거합니다. 이 옵션을 `true`로 설정하면 프로덕션에서도 주석이 유지됩니다. 개발 중에는 항상 주석이 유지됩니다. 이 옵션은 주로 HTML 주석에 의존하는 다른 라이브러리와 함께 Vue를 사용할 때 사용됩니다.

- **예시**

  ```js
  app.config.compilerOptions.comments = true
  ```

<a id="api-application-app-config-globalproperties"></a>

### app.config.globalProperties
애플리케이션 내 모든 컴포넌트 인스턴스에서 접근할 수 있는 전역 속성을 등록할 수 있는 객체입니다.

- **타입**

  ```ts
  interface AppConfig {
    globalProperties: Record<string, any>
  }
  ```

- **세부사항**

  이는 Vue 3에서 더 이상 존재하지 않는 Vue 2의 `Vue.prototype`을 대체합니다. 모든 전역 설정과 마찬가지로, 신중하게 사용해야 합니다.

  전역 속성이 컴포넌트의 자체 속성과 충돌하는 경우, 컴포넌트의 자체 속성이 우선합니다.

- **사용법**

  ```js
  app.config.globalProperties.msg = 'hello'
  ```

  이렇게 하면 애플리케이션 내 모든 컴포넌트 템플릿과 컴포넌트 인스턴스의 `this`에서 `msg`를 사용할 수 있습니다:

  ```js
  export default {
    mounted() {
      console.log(this.msg) // 'hello'
    }
  }
  ```

- **관련 문서** [가이드 - 전역 속성 확장](05_scaling_typescript_and_best_practices.md#guide-typescript-options-api-augmenting-global-properties)  (TypeScript)

<a id="api-application-app-config-optionmergestrategies"></a>

### app.config.optionMergeStrategies
커스텀 컴포넌트 옵션에 대한 병합 전략을 정의하는 객체입니다.

- **타입**

  ```ts
  interface AppConfig {
    optionMergeStrategies: Record<string, OptionMergeFunction>
  }

  type OptionMergeFunction = (to: unknown, from: unknown) => any
  ```

- **세부사항**

  일부 플러그인/라이브러리는 커스텀 컴포넌트 옵션 지원을 추가합니다(전역 믹스인 주입 등). 동일한 옵션이 여러 소스(예: 믹스인 또는 컴포넌트 상속)에서 "병합"되어야 할 때, 이러한 옵션에는 특별한 병합 로직이 필요할 수 있습니다.

  커스텀 옵션의 병합 전략 함수는, 옵션 이름을 키로 사용해 `app.config.optionMergeStrategies` 객체에 할당하는 방식으로 등록할 수 있습니다.

  병합 전략 함수는 부모와 자식 인스턴스에 정의된 해당 옵션의 값을 각각 첫 번째, 두 번째 인자로 받습니다.

- **예시**

  ```js
  const app = createApp({
    // 자체 옵션
    msg: 'Vue',
    // 믹스인에서 온 옵션
    mixins: [
      {
        msg: 'Hello '
      }
    ],
    mounted() {
      // 병합된 옵션은 this.$options에 노출됨
      console.log(this.$options.msg)
    }
  })

  // `msg`에 대한 커스텀 병합 전략 정의
  app.config.optionMergeStrategies.msg = (parent, child) => {
    return (parent || '') + (child || '')
  }

  app.mount('#app')
  // 'Hello Vue'가 출력됨
  ```

- **관련 문서** [컴포넌트 인스턴스 - `$options`](08_component_and_advanced_apis.md#api-component-instance-options)

<a id="api-application-app-config-idprefix"></a>

### app.config.idPrefix  (3.5+)
이 애플리케이션 내에서 [useId()](07_composition_and_reactivity_apis.md#api-composition-api-helpers-useid)를 통해 생성된 모든 ID에 대한 접두사를 설정합니다.

- **타입:** `string`

- **기본값:** `undefined`

- **예시**

  ```js
  app.config.idPrefix = 'myApp'
  ```

  ```js
  // 컴포넌트 내에서:
  const id1 = useId() // 'myApp:0'
  const id2 = useId() // 'myApp:1'
  ```

<a id="api-application-app-config-throwunhandlederrorinproduction"></a>

### app.config.throwUnhandledErrorInProduction  (3.5+)
프로덕션 모드에서 처리되지 않은 에러를 강제로 throw합니다.

- **타입:** `boolean`

- **기본값:** `false`

- **세부사항**

  기본적으로 Vue 애플리케이션 내에서 throw되었지만 명시적으로 처리되지 않은 에러는 개발 모드와 프로덕션 모드에서 서로 다르게 동작합니다:

  - 개발 모드에서는 에러가 throw되어 애플리케이션이 중단될 수 있습니다. 이는 에러를 더 눈에 띄게 하여 개발 중에 발견하고 수정할 수 있도록 하기 위함입니다.

  - 프로덕션 모드에서는 에러가 콘솔에만 기록되어 최종 사용자에게 미치는 영향을 최소화합니다. 그러나 이로 인해 프로덕션에서만 발생하는 에러가 에러 모니터링 서비스에 포착되지 않을 수 있습니다.

  `app.config.throwUnhandledErrorInProduction`을 `true`로 설정하면, 프로덕션 모드에서도 처리되지 않은 에러가 throw됩니다.

---

<a id="api-general"></a>

<a id="api-general-global-api-general"></a>

## 글로벌 API: 일반

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/general.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/general.md

<a id="api-general-version"></a>

### version
현재 Vue의 버전을 노출합니다.

- **타입:** `string`

- **예시**

  ```js
  import { version } from 'vue'

  console.log(version)
  ```

<a id="api-general-nexttick"></a>

### nextTick()
다음 DOM 업데이트 플러시를 기다리기 위한 유틸리티입니다.

- **타입**

  ```ts
  function nextTick(callback?: () => void): Promise<void>
  ```

- **세부사항**

  Vue에서 반응형 상태를 변경하면, 그에 따른 DOM 업데이트는 동기적으로 적용되지 않습니다. 대신 Vue는 "다음 틱"까지 이를 버퍼링하여, 여러 번 상태를 변경해도 각 컴포넌트(component)가 한 번만 업데이트되도록 보장합니다.

  `nextTick()`은 상태 변경 직후에 DOM 업데이트가 완료될 때까지 기다릴 때 사용할 수 있습니다. 콜백(callback)을 인자로 전달하거나, 반환된 Promise를 await할 수 있습니다.

- **예시**


**컴포지션 API**


  ```vue
  <script setup>
  import { ref, nextTick } from 'vue'

  const count = ref(0)

  async function increment() {
    count.value++

    // DOM이 아직 업데이트되지 않음
    console.log(document.getElementById('counter').textContent) // 0

    await nextTick()
    // DOM이 이제 업데이트됨
    console.log(document.getElementById('counter').textContent) // 1
  }
  </script>

  <template>
    <button id="counter" @click="increment">{{ count }}</button>
  </template>
  ```



**옵션 API**


  ```vue
  <script>
  import { nextTick } from 'vue'

  export default {
    data() {
      return {
        count: 0
      }
    },
    methods: {
      async increment() {
        this.count++

        // DOM이 아직 업데이트되지 않음
        console.log(document.getElementById('counter').textContent) // 0

        await nextTick()
        // DOM이 이제 업데이트됨
        console.log(document.getElementById('counter').textContent) // 1
      }
    }
  }
  </script>

  <template>
    <button id="counter" @click="increment">{{ count }}</button>
  </template>
  ```



- **관련 항목** [`this.$nextTick()`](08_component_and_advanced_apis.md#api-component-instance-nexttick)

<a id="api-general-definecomponent"></a>

### defineComponent()
타입 추론과 함께 Vue 컴포넌트를 정의하기 위한 타입 헬퍼입니다.

- **타입**

  ```ts
  // 옵션 문법
  function defineComponent(
    component: ComponentOptions
  ): ComponentConstructor

  // 함수 문법 (3.3+ 필요)
  function defineComponent(
    setup: ComponentOptions['setup'],
    extraOptions?: ComponentOptions
  ): () => any
  ```

  > 가독성을 위해 타입이 단순화되었습니다.

- **세부사항**

  첫 번째 인자로 컴포넌트 옵션 객체를 받습니다. 반환값은 동일한 옵션 객체이며, 이 함수는 타입 추론을 위한 런타임 no-op입니다.

  반환 타입은 약간 특별합니다: 옵션을 기반으로 추론된 컴포넌트 인스턴스(instance) 타입의 생성자 타입이 됩니다. 이는 반환 타입이 TSX에서 태그로 사용될 때 타입 추론에 사용됩니다.

  컴포넌트의 인스턴스 타입(옵션 내에서의 `this` 타입과 동일)을 `defineComponent()`의 반환 타입에서 다음과 같이 추출할 수 있습니다:

  ```ts
  const Foo = defineComponent(/* ... */)

  type FooInstance = InstanceType<typeof Foo>
  ```

  <a id="api-general-function-signature"></a>

  #### 함수 시그니처

  - 3.3+에서만 지원

  `defineComponent()`는 컴포지션 API 및 [렌더 함수 또는 JSX](06_reactivity_and_rendering_in_depth.md#guide-extras-render-function)와 함께 사용하기 위한 대체 시그니처도 제공합니다.

  옵션 객체를 전달하는 대신, 함수를 전달해야 합니다. 이 함수는 컴포지션 API의 [`setup()`](07_composition_and_reactivity_apis.md#api-composition-api-setup-composition-api-setup) 함수와 동일하게 동작하며, props와 setup context를 받습니다. 반환값은 렌더 함수여야 하며, `h()`와 JSX 모두 지원됩니다:

  ```js
  import { ref, h } from 'vue'

  const Comp = defineComponent(
    (props) => {
      // <script setup>에서처럼 Composition API 사용
      const count = ref(0)

      return () => {
        // 렌더 함수 또는 JSX
        return h('div', count.value)
      }
    },
    // 추가 옵션, 예: props와 emits 선언
    {
      props: {
        /* ... */
      }
    }
  )
  ```

  이 시그니처의 주요 사용 사례는 TypeScript(특히 TSX)와 함께 사용하는 것입니다. 제네릭을 지원하기 때문입니다:

  ```tsx
  const Comp = defineComponent(
    <T extends string | number>(props: { msg: T; list: T[] }) => {
      // <script setup>에서처럼 Composition API 사용
      const count = ref(0)

      return () => {
        // 렌더 함수 또는 JSX
        return <div>{count.value}</div>
      }
    },
    // 현재는 런타임 props 선언이 수동으로 필요합니다.
    {
      props: ['msg', 'list']
    }
  )
  ```

  앞으로는 (마치 SFC의 `defineProps`처럼) 런타임 props를 자동으로 추론하고 주입하는 Babel 플러그인(plugin)을 제공하여, 런타임 props 선언을 생략할 수 있도록 할 계획입니다.

  <a id="api-general-note-on-webpack-treeshaking"></a>

  #### webpack 트리 셰이킹에 대한 참고

  `defineComponent()`는 함수 호출이기 때문에, 일부 빌드 도구(예: webpack)에서는 부수 효과이 있는 것으로 간주할 수 있습니다. 이로 인해 컴포넌트가 실제로 사용되지 않더라도 트리 셰이킹되지 않을 수 있습니다.

  이 함수 호출이 트리 셰이킹에 안전하다는 것을 webpack에 알리려면, 함수 호출 앞에 `/*#__PURE__*/` 주석을 추가할 수 있습니다:

  ```js
  export default /*#__PURE__*/ defineComponent(/* ... */)
  ```

  Vite를 사용하는 경우에는 이 주석이 필요하지 않습니다. Vite의 기본 프로덕션 번들러(bundler)인 Rollup은 수동 주석 없이도 `defineComponent()`가 실제로 부수 효과이 없다는 것을 충분히 판단할 수 있습니다.

- **관련 항목** [가이드 - TypeScript와 함께 Vue 사용하기](05_scaling_typescript_and_best_practices.md#guide-typescript-overview-general-usage-notes)

<a id="api-general-defineasynccomponent"></a>

### defineAsyncComponent()
렌더링(rendering)될 때에만 지연 로드되는 비동기 컴포넌트를 정의합니다. 인자는 로더 함수이거나, 로딩 동작을 더 세밀하게 제어할 수 있는 옵션 객체일 수 있습니다.

- **타입**

  ```ts
  function defineAsyncComponent(
    source: AsyncComponentLoader | AsyncComponentOptions
  ): Component

  type AsyncComponentLoader = () => Promise<Component>

  interface AsyncComponentOptions {
    loader: AsyncComponentLoader
    loadingComponent?: Component
    errorComponent?: Component
    delay?: number
    timeout?: number
    suspensible?: boolean
    onError?: (
      error: Error,
      retry: () => void,
      fail: () => void,
      attempts: number
    ) => any
  }
  ```

- **관련 항목** [가이드 - 비동기 컴포넌트](03_components_and_reusability.md#guide-components-async)

---

<a id="api-composition-api-setup"></a>

<a id="api-composition-api-setup-composition-api-setup"></a>

## 컴포지션 API: setup()

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/composition-api-setup.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/composition-api-setup.md

<a id="api-composition-api-setup-basic-usage"></a>

### 기본 사용법
`setup()` 훅(hook)은 다음과 같은 경우 컴포지션 API를 컴포넌트(component)에서 사용할 수 있는 진입점 역할을 합니다:

1. 빌드 단계 없이 컴포지션 API를 사용할 때
2. 옵션 API 컴포넌트에서 컴포지션 API 기반 코드와 통합할 때

**참고**
싱글 파일 컴포넌트에서 컴포지션 API를 사용하는 경우, 더 간결하고 사용하기 쉬운 문법을 위해 [`<script setup>`](08_component_and_advanced_apis.md#api-sfc-script-setup) 사용을 강력히 권장합니다.


[반응성 API](07_composition_and_reactivity_apis.md#api-reactivity-core)를 사용하여 반응형 상태를 선언하고, `setup()`에서 객체를 반환하여 템플릿(template)에 노출할 수 있습니다. 반환된 객체의 속성들은 (다른 옵션이 사용되는 경우) 컴포넌트 인스턴스(instance)에서도 사용할 수 있습니다:

```vue
<script>
import { ref } from 'vue'

export default {
  setup() {
    const count = ref(0)

    // 템플릿과 다른 옵션 API 훅에 노출
    return {
      count
    }
  },

  mounted() {
    console.log(this.count) // 0
  }
}
</script>

<template>
  <button @click="count++">{{ count }}</button>
</template>
```

`setup`에서 반환된 [ref](07_composition_and_reactivity_apis.md#api-reactivity-core-ref)는 템플릿에서 접근할 때 [자동으로 얕게 언래핑](02_essentials.md#guide-essentials-reactivity-fundamentals-deep-reactivity)되므로 `.value`를 사용할 필요가 없습니다. `this`에서 접근할 때도 동일하게 언래핑됩니다.

`setup()` 자체는 컴포넌트 인스턴스에 접근할 수 없습니다. `setup()` 내부에서 `this`는 `undefined` 값을 가집니다. 옵션 API에서는 컴포지션 API로 노출된 값에 접근할 수 있지만, 그 반대는 불가능합니다.

`setup()`은 _동기적으로_ 객체를 반환해야 합니다. `async setup()`은 컴포넌트가 [Suspense](04_built_ins_and_animation.md#guide-built-ins-suspense) 컴포넌트의 하위 컴포넌트인 경우에만 사용할 수 있습니다.

<a id="api-composition-api-setup-accessing-props"></a>

### Props 접근하기
`setup` 함수의 첫 번째 인자는 `props`입니다. 일반 컴포넌트에서 기대하는 것처럼, `setup` 함수 내부의 `props`는 반응형이며 새로운 props가 전달될 때 업데이트됩니다.

```js
export default {
  props: {
    title: String
  },
  setup(props) {
    console.log(props.title)
  }
}
```

`props` 객체를 구조 분해 할당하면, 구조 분해된 변수는 반응성(reactivity)을 잃게 됩니다. 따라서 항상 `props.xxx` 형태로 props에 접근하는 것이 권장됩니다.

정말로 props를 구조 분해해야 하거나, 반응성을 유지한 채로 외부 함수에 prop을 전달해야 하는 경우, [toRefs()](07_composition_and_reactivity_apis.md#api-reactivity-utilities-torefs) 및 [toRef()](07_composition_and_reactivity_apis.md#api-reactivity-utilities-toref) 유틸리티 API를 사용할 수 있습니다:

```js
import { toRefs, toRef } from 'vue'

export default {
  setup(props) {
    // `props`를 ref 객체로 변환한 후 구조 분해
    const { title } = toRefs(props)
    // `title`은 `props.title`을 추적하는 ref입니다
    console.log(title.value)

    // 또는, `props`의 단일 속성을 ref로 변환
    const title = toRef(props, 'title')
  }
}
```

<a id="api-composition-api-setup-setup-context"></a>

### Setup Context
`setup` 함수에 전달되는 두 번째 인자는 **Setup Context** 객체입니다. 컨텍스트 객체는 `setup` 내부에서 유용할 수 있는 다른 값들을 노출합니다:

```js
export default {
  setup(props, context) {
    // 속성 (비반응형 객체, $attrs와 동일)
    console.log(context.attrs)

    // 슬롯 (비반응형 객체, $slots와 동일)
    console.log(context.slots)

    // 이벤트 발생 (함수, $emit과 동일)
    console.log(context.emit)

    // 공개 속성 노출 (함수)
    console.log(context.expose)
  }
}
```

컨텍스트 객체는 반응형이 아니며 안전하게 구조 분해할 수 있습니다:

```js
export default {
  setup(props, { attrs, slots, emit, expose }) {
    ...
  }
}
```

`attrs`와 `slots`는 상태를 가지는 객체로, 컴포넌트 자체가 업데이트될 때마다 항상 업데이트됩니다. 따라서 이들을 구조 분해하지 말고 항상 `attrs.x` 또는 `slots.x`와 같이 속성에 접근해야 합니다. 또한, `props`와 달리 `attrs`와 `slots`의 속성은 **반응형이 아닙니다**. `attrs`나 `slots`의 변경에 따라 부수 효과를 적용하려면 `onBeforeUpdate` 라이프사이클(lifecycle) 훅 내부에서 처리해야 합니다.

<a id="api-composition-api-setup-exposing-public-properties"></a>

#### 공개 속성 노출하기
`expose`는 부모 컴포넌트가 [템플릿 ref](02_essentials.md#guide-essentials-template-refs-ref-on-component)를 통해 컴포넌트 인스턴스에 접근할 때 노출되는 속성을 명시적으로 제한할 수 있는 함수입니다:

```js{5,10}
export default {
  setup(props, { expose }) {
    // 인스턴스를 "닫힌" 상태로 만듭니다 -
    // 즉, 부모에게 아무것도 노출하지 않음
    expose()

    const publicCount = ref(0)
    const privateCount = ref(0)
    // 로컬 상태를 선택적으로 노출
    expose({ count: publicCount })
  }
}
```

<a id="api-composition-api-setup-usage-with-render-functions"></a>

### 렌더 함수와 함께 사용하기
`setup`은 [렌더 함수](06_reactivity_and_rendering_in_depth.md#guide-extras-render-function)를 반환할 수도 있으며, 이 렌더 함수는 동일한 스코프에서 선언된 반응형 상태를 직접 사용할 수 있습니다:

```js{6}
import { h, ref } from 'vue'

export default {
  setup() {
    const count = ref(0)
    return () => h('div', count.value)
  }
}
```

렌더 함수를 반환하는 `setup()`은 메서드를 담은 객체를 함께 반환할 수 없습니다. 부모 컴포넌트가 템플릿 ref로 이 메서드에 접근해야 한다면, [`expose()`](07_composition_and_reactivity_apis.md#api-composition-api-setup-exposing-public-properties)로 따로 공개합니다. 아래에서는 렌더 함수가 사용할 `count`를 내부에 두고, 부모에는 `increment`만 노출합니다:

```js{8-10}
import { h, ref } from 'vue'

export default {
  setup(props, { expose }) {
    const count = ref(0)
    const increment = () => ++count.value

    expose({
      increment
    })

    return () => h('div', count.value)
  }
}
```

이제 `increment` 메서드는 템플릿 ref를 통해 부모 컴포넌트에서 사용할 수 있습니다.

---

<a id="api-reactivity-core"></a>

<a id="api-reactivity-core-reactivity-api-core"></a>

## 반응성 API: 코어

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/reactivity-core.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/reactivity-core.md

**참고**
반응성 API를 더 잘 이해하려면 다음 가이드 챕터를 읽는 것이 좋습니다:

- [반응성 기본](02_essentials.md#guide-essentials-reactivity-fundamentals) (API 선호도를 컴포지션 API로 설정)
- [반응성 심층 분석](06_reactivity_and_rendering_in_depth.md#guide-extras-reactivity-in-depth)


<a id="api-reactivity-core-ref"></a>

### ref()
내부 값을 받아서 반응형이며 변경 가능한 ref 객체를 반환합니다. 이 객체는 내부 값을 가리키는 단일 속성 `.value`를 가집니다.

- **타입**

  ```ts
  function ref<T>(value: T): Ref<UnwrapRef<T>>

  interface Ref<T> {
    value: T
  }
  ```

- **세부사항**

  ref 객체는 변경 가능합니다. 즉, `.value`에 새로운 값을 할당할 수 있습니다. 또한 반응형이기도 하여, `.value`에 대한 읽기 작업은 추적되고, 쓰기 작업은 관련된 효과를 트리거합니다.

  객체가 ref의 값으로 할당되면, 해당 객체는 [reactive()](07_composition_and_reactivity_apis.md#api-reactivity-core-reactive)로 깊게 반응형이 됩니다. 이는 객체에 중첩된 ref가 있을 경우, 이들도 깊게 언랩된다는 의미입니다.

  깊은 변환을 피하려면 [`shallowRef()`](07_composition_and_reactivity_apis.md#api-reactivity-advanced-shallowref)를 사용하세요.

- **예시**

  ```js
  const count = ref(0)
  console.log(count.value) // 0

  count.value = 1
  console.log(count.value) // 1
  ```

- **참고**
  - [가이드 - `ref()`와 함께하는 반응성 기본](02_essentials.md#guide-essentials-reactivity-fundamentals-ref)
  - [가이드 - `ref()` 타입 지정](05_scaling_typescript_and_best_practices.md#guide-typescript-composition-api-typing-ref)  (TypeScript)

<a id="api-reactivity-core-computed"></a>

### computed()
[getter 함수](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Functions/get#description)를 받아, getter에서 반환된 값에 대한 읽기 전용 반응형 [ref](07_composition_and_reactivity_apis.md#api-reactivity-core-ref) 객체를 반환합니다. 또한 `get`과 `set` 함수가 포함된 객체를 받아서 쓰기 가능한 ref 객체를 생성할 수도 있습니다.

- **타입**

  ```ts
  // 읽기 전용
  function computed<T>(
    getter: (oldValue: T | undefined) => T,
    // 아래 "Computed Debugging" 링크 참고
    debuggerOptions?: DebuggerOptions
  ): Readonly<Ref<Readonly<T>>>

  // 쓰기 가능
  function computed<T>(
    options: {
      get: (oldValue: T | undefined) => T
      set: (value: T) => void
    },
    debuggerOptions?: DebuggerOptions
  ): Ref<T>
  ```

- **예시**

  읽기 전용 computed ref 생성:

  ```js
  const count = ref(1)
  const plusOne = computed(() => count.value + 1)

  console.log(plusOne.value) // 2

  plusOne.value++ // 에러
  ```

  쓰기 가능한 computed ref 생성:

  ```js
  const count = ref(1)
  const plusOne = computed({
    get: () => count.value + 1,
    set: (val) => {
      count.value = val - 1
    }
  })

  plusOne.value = 1
  console.log(count.value) // 0
  ```

  디버깅:

  ```js
  const plusOne = computed(() => count.value + 1, {
    onTrack(e) {
      debugger
    },
    onTrigger(e) {
      debugger
    }
  })
  ```

- **참고**
  - [가이드 - 계산된 속성](02_essentials.md#guide-essentials-computed)
  - [가이드 - Computed 디버깅](06_reactivity_and_rendering_in_depth.md#guide-extras-reactivity-in-depth-computed-debugging)
  - [가이드 - `computed()` 타입 지정](05_scaling_typescript_and_best_practices.md#guide-typescript-composition-api-typing-computed)  (TypeScript)
  - [가이드 - 성능 - Computed 안정성](05_scaling_typescript_and_best_practices.md#guide-best-practices-performance-computed-stability)

<a id="api-reactivity-core-reactive"></a>

### reactive()
객체의 반응형 프록시(proxy)를 반환합니다.

- **타입**

  ```ts
  function reactive<T extends object>(target: T): UnwrapNestedRefs<T>
  ```

- **세부사항**

  반응형 변환은 모든 중첩 속성에 재귀적으로 적용됩니다. 반응형 객체는 [ref](07_composition_and_reactivity_apis.md#api-reactivity-core-ref) 속성도 깊게 언랩하면서 반응성(reactivity)을 유지합니다.

  또한 ref가 반응형 배열이나 `Map`과 같은 네이티브 컬렉션 타입의 요소로 접근될 때는 ref 언래핑이 수행되지 않는다는 점에 유의해야 합니다.

  깊은 변환을 피하고 루트 레벨에서만 반응성을 유지하려면 [shallowReactive()](07_composition_and_reactivity_apis.md#api-reactivity-advanced-shallowreactive)를 사용하세요.

  반환된 객체와 그 중첩 객체들은 [ES Proxy](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Proxy)로 래핑되며, **원본 객체와 동일하지 않습니다**. 반응형 프록시만을 사용하고 원본 객체에 의존하지 않는 것이 권장됩니다.

- **예시**

  반응형 객체 생성:

  ```js
  const obj = reactive({ count: 0 })
  obj.count++
  ```

  ref 언래핑:

  ```ts
  const count = ref(1)
  const obj = reactive({ count })

  // ref가 언랩됩니다
  console.log(obj.count === count.value) // true

  // `obj.count`가 업데이트됩니다
  count.value++
  console.log(count.value) // 2
  console.log(obj.count) // 2

  // `count` ref도 업데이트됩니다
  obj.count++
  console.log(obj.count) // 3
  console.log(count.value) // 3
  ```

  ref가 배열이나 컬렉션 요소로 접근될 때는 **언랩되지 않습니다**:

  ```js
  const books = reactive([ref('Vue 3 Guide')])
  // 여기서는 .value가 필요합니다
  console.log(books[0].value)

  const map = reactive(new Map([['count', ref(0)]]))
  // 여기서도 .value가 필요합니다
  console.log(map.get('count').value)
  ```

  [ref](07_composition_and_reactivity_apis.md#api-reactivity-core-ref)를 `reactive` 속성에 할당하면, 해당 ref도 자동으로 언랩됩니다:

  ```ts
  const count = ref(1)
  const obj = reactive({})

  obj.count = count

  console.log(obj.count) // 1
  console.log(obj.count === count.value) // true
  ```

- **참고**
  - [가이드 - 반응성 기본](02_essentials.md#guide-essentials-reactivity-fundamentals)
  - [가이드 - `reactive()` 타입 지정](05_scaling_typescript_and_best_practices.md#guide-typescript-composition-api-typing-reactive)  (TypeScript)

<a id="api-reactivity-core-readonly"></a>

### readonly()
객체(반응형 또는 일반 객체)나 [ref](07_composition_and_reactivity_apis.md#api-reactivity-core-ref)를 받아 원본에 대한 읽기 전용 프록시를 반환합니다.

- **타입**

  ```ts
  function readonly<T extends object>(
    target: T
  ): DeepReadonly<UnwrapNestedRefs<T>>
  ```

- **세부사항**

  읽기 전용 프록시는 깊게 적용됩니다: 중첩된 모든 속성도 읽기 전용이 됩니다. 또한 `reactive()`와 동일한 ref 언래핑 동작을 가지지만, 언랩된 값도 읽기 전용이 됩니다.

  깊은 변환을 피하려면 [shallowReadonly()](07_composition_and_reactivity_apis.md#api-reactivity-advanced-shallowreadonly)를 사용하세요.

- **예시**

  ```js
  const original = reactive({ count: 0 })

  const copy = readonly(original)

  watchEffect(() => {
    // 반응성 추적에 사용 가능
    console.log(copy.count)
  })

  // 원본을 변경하면 copy를 참조하는 watcher가 트리거됨
  original.count++

  // copy를 변경하려고 하면 실패하고 경고가 발생함
  copy.count++ // 경고!
  ```

<a id="api-reactivity-core-watcheffect"></a>

### watchEffect()
함수를 즉시 실행하면서 그 의존성을 반응적으로 추적하고, 의존성이 변경될 때마다 다시 실행합니다.

- **타입**

  ```ts
  function watchEffect(
    effect: (onCleanup: OnCleanup) => void,
    options?: WatchEffectOptions
  ): WatchHandle

  type OnCleanup = (cleanupFn: () => void) => void

  interface WatchEffectOptions {
    flush?: 'pre' | 'post' | 'sync' // 기본값: 'pre'
    onTrack?: (event: DebuggerEvent) => void
    onTrigger?: (event: DebuggerEvent) => void
  }

  interface WatchHandle {
    (): void // 호출 가능, `stop`과 동일
    pause: () => void
    resume: () => void
    stop: () => void
  }
  ```

- **세부사항**

  첫 번째 인자는 실행할 effect 함수입니다. effect 함수는 정리 콜백(callback)을 등록할 수 있는 함수를 인자로 받습니다. 정리 콜백은 effect가 다음에 다시 실행되기 직전에 호출되며, 예를 들어 대기 중인 비동기 요청과 같은 무효화된 부수 효과를 정리하는 데 사용할 수 있습니다(아래 예시 참고).

  두 번째 인자는 선택적 옵션 객체로, effect의 flush 타이밍을 조정하거나 의존성 디버깅에 사용할 수 있습니다.

  기본적으로 watcher는 컴포넌트(component) 렌더링 직전에 실행됩니다. `flush: 'post'`로 설정하면 watcher가 컴포넌트 렌더링(rendering) 후에 실행됩니다. 자세한 내용은 [콜백 flush 타이밍](02_essentials.md#guide-essentials-watchers-callback-flush-timing)을 참고하세요. 드물게, 반응형 의존성이 변경될 때 watcher를 즉시 트리거해야 하는 경우도 있습니다(예: 캐시 무효화). 이 경우 `flush: 'sync'`를 사용할 수 있습니다. 단, 이 설정은 여러 속성이 동시에 업데이트될 때 성능 및 데이터 일관성 문제를 일으킬 수 있으므로 주의해서 사용해야 합니다.

  반환값은 effect의 재실행을 중지할 수 있는 핸들 함수입니다.

- **예시**

  ```js
  const count = ref(0)

  watchEffect(() => console.log(count.value))
  // -> 0 출력

  count.value++
  // -> 1 출력
  ```

  watcher 중지:

  ```js
  const stop = watchEffect(() => {})

  // watcher가 더 이상 필요 없을 때:
  stop()
  ```

  watcher 일시정지 / 재개:  (3.5+)

  ```js
  const { stop, pause, resume } = watchEffect(() => {})

  // watcher를 일시적으로 정지
  pause()

  // 나중에 재개
  resume()

  // 중지
  stop()
  ```

  부수 효과 정리:

  ```js
  watchEffect(async (onCleanup) => {
    const { response, cancel } = doAsyncWork(newId)
    // `cancel`은 `id`가 변경되면 호출되어,
    // 이전 요청이 아직 완료되지 않았다면 취소합니다
    onCleanup(cancel)
    data.value = await response
  })
  ```

  3.5+에서의 부수 효과 정리:

  ```js
  import { onWatcherCleanup } from 'vue'

  watchEffect(async () => {
    const { response, cancel } = doAsyncWork(newId)
    // `cancel`은 `id`가 변경되면 호출되어,
    // 이전 요청이 아직 완료되지 않았다면 취소합니다
    onWatcherCleanup(cancel)
    data.value = await response
  })
  ```

  옵션:

  ```js
  watchEffect(() => {}, {
    flush: 'post',
    onTrack(e) {
      debugger
    },
    onTrigger(e) {
      debugger
    }
  })
  ```

- **참고**
  - [가이드 - Watcher](02_essentials.md#guide-essentials-watchers-watcheffect)
  - [가이드 - Watcher 디버깅](06_reactivity_and_rendering_in_depth.md#guide-extras-reactivity-in-depth-watcher-debugging)

<a id="api-reactivity-core-watchposteffect"></a>

### watchPostEffect()
[`watchEffect()`](07_composition_and_reactivity_apis.md#api-reactivity-core-watcheffect)에서 `flush: 'post'` 옵션을 사용한 별칭입니다.

<a id="api-reactivity-core-watchsynceffect"></a>

### watchSyncEffect()
[`watchEffect()`](07_composition_and_reactivity_apis.md#api-reactivity-core-watcheffect)에서 `flush: 'sync'` 옵션을 사용한 별칭입니다.

<a id="api-reactivity-core-watch"></a>

### watch()
하나 이상의 반응형 데이터 소스를 감시하고, 소스가 변경될 때 콜백 함수를 호출합니다.

- **타입**

  ```ts
  // 단일 소스 감시
  function watch<T>(
    source: WatchSource<T>,
    callback: WatchCallback<T>,
    options?: WatchOptions
  ): WatchHandle

  // 다중 소스 감시
  function watch<T>(
    sources: WatchSource<T>[],
    callback: WatchCallback<T[]>,
    options?: WatchOptions
  ): WatchHandle

  type WatchCallback<T> = (
    value: T,
    oldValue: T,
    onCleanup: (cleanupFn: () => void) => void
  ) => void

  type WatchSource<T> =
    | Ref<T> // ref
    | (() => T) // getter
    | (T extends object ? T : never) // 반응형 객체

  interface WatchOptions extends WatchEffectOptions {
    immediate?: boolean // 기본값: false
    deep?: boolean | number // 기본값: false
    flush?: 'pre' | 'post' | 'sync' // 기본값: 'pre'
    onTrack?: (event: DebuggerEvent) => void
    onTrigger?: (event: DebuggerEvent) => void
    once?: boolean // 기본값: false (3.4+)
  }

  interface WatchHandle {
    (): void // 호출 가능, `stop`과 동일
    pause: () => void
    resume: () => void
    stop: () => void
  }
  ```

  > 타입은 가독성을 위해 단순화되었습니다.

- **세부사항**

  `watch()`는 기본적으로 lazy(지연) 방식입니다. 즉, 감시하는 소스가 변경될 때만 콜백이 호출됩니다.

  첫 번째 인자는 watcher의 **소스**입니다. 소스는 다음 중 하나일 수 있습니다:

  - 값을 반환하는 getter 함수
  - ref
  - 반응형 객체
  - ...또는 위의 항목들의 배열

  두 번째 인자는 소스가 변경될 때 호출되는 콜백입니다. 콜백은 새로운 값, 이전 값, 그리고 부수 효과 정리 콜백을 등록하는 함수, 이렇게 세 개의 인자를 받습니다. 정리 콜백은 effect가 다음에 다시 실행되기 직전에 호출되며, 예를 들어 대기 중인 비동기 요청과 같은 무효화된 부수 효과를 정리하는 데 사용할 수 있습니다.

  여러 소스를 감시할 때, 콜백은 소스 배열에 대응하는 새로운 값/이전 값의 두 배열을 받습니다.

  세 번째 선택적 인자는 옵션 객체로, 다음과 같은 옵션을 지원합니다:

  - **`immediate`**: watcher 생성 시 콜백을 즉시 트리거합니다. 첫 호출 시 이전 값은 `undefined`입니다.
  - **`deep`**: 소스가 객체일 경우 깊은 탐색을 강제하여, 깊은 변경에도 콜백이 실행됩니다. 3.5+에서는 최대 탐색 깊이를 나타내는 숫자도 허용됩니다. [깊은 Watcher](02_essentials.md#guide-essentials-watchers-deep-watchers) 참고.
  - **`flush`**: 콜백의 flush 타이밍을 조정합니다. [콜백 flush 타이밍](02_essentials.md#guide-essentials-watchers-callback-flush-timing) 및 [`watchEffect()`](07_composition_and_reactivity_apis.md#api-reactivity-core-watcheffect) 참고.
  - **`onTrack / onTrigger`**: watcher의 의존성 디버깅. [Watcher 디버깅](06_reactivity_and_rendering_in_depth.md#guide-extras-reactivity-in-depth-watcher-debugging) 참고.
  - **`once`**: (3.4+) 콜백을 한 번만 실행합니다. 첫 콜백 실행 후 watcher가 자동으로 중지됩니다.

  [`watchEffect()`](07_composition_and_reactivity_apis.md#api-reactivity-core-watcheffect)와 비교했을 때, `watch()`는 다음을 할 수 있습니다:

  - 부수 효과를 lazy하게 수행
  - watcher를 재실행시킬 상태를 더 구체적으로 지정
  - 감시하는 상태의 이전 값과 현재 값 모두에 접근

- **예시**

  getter 감시:

  ```js
  const state = reactive({ count: 0 })
  watch(
    () => state.count,
    (count, prevCount) => {
      /* ... */
    }
  )
  ```

  ref 감시:

  ```js
  const count = ref(0)
  watch(count, (count, prevCount) => {
    /* ... */
  })
  ```

  여러 소스를 감시할 때, 콜백은 소스 배열에 대응하는 새로운 값/이전 값의 배열을 받습니다:

  ```js
  watch([fooRef, barRef], ([foo, bar], [prevFoo, prevBar]) => {
    /* ... */
  })
  ```

  getter 소스를 사용할 때, getter의 반환값이 변경된 경우에만 watcher가 실행됩니다. 깊은 변경에도 콜백이 실행되길 원한다면, `{ deep: true }`로 watcher를 명시적으로 깊은 모드로 설정해야 합니다. 깊은 모드에서는, 콜백이 깊은 변경에 의해 트리거된 경우 새 값과 이전 값이 동일한 객체가 됩니다:

  ```js
  const state = reactive({ count: 0 })
  watch(
    () => state,
    (newValue, oldValue) => {
      // newValue === oldValue
    },
    { deep: true }
  )
  ```

  반응형 객체를 직접 감시할 때는 watcher가 자동으로 깊은 모드가 됩니다:

  ```js
  const state = reactive({ count: 0 })
  watch(state, () => {
    /* state의 깊은 변경에도 트리거됨 */
  })
  ```

  `watch()`는 [`watchEffect()`](07_composition_and_reactivity_apis.md#api-reactivity-core-watcheffect)와 동일한 flush 타이밍 및 디버깅 옵션을 공유합니다:

  ```js
  watch(source, callback, {
    flush: 'post',
    onTrack(e) {
      debugger
    },
    onTrigger(e) {
      debugger
    }
  })
  ```

  watcher 중지:

  ```js
  const stop = watch(source, callback)

  // watcher가 더 이상 필요 없을 때:
  stop()
  ```

  watcher 일시정지 / 재개:  (3.5+)

  ```js
  const { stop, pause, resume } = watch(() => {})

  // watcher를 일시적으로 정지
  pause()

  // 나중에 재개
  resume()

  // 중지
  stop()
  ```

  부수 효과 정리:

  ```js
  watch(id, async (newId, oldId, onCleanup) => {
    const { response, cancel } = doAsyncWork(newId)
    // `cancel`은 `id`가 변경되면 호출되어,
    // 이전 요청이 아직 완료되지 않았다면 취소합니다
    onCleanup(cancel)
    data.value = await response
  })
  ```

  3.5+에서의 부수 효과 정리:

  ```js
  import { onWatcherCleanup } from 'vue'

  watch(id, async (newId) => {
    const { response, cancel } = doAsyncWork(newId)
    onWatcherCleanup(cancel)
    data.value = await response
  })
  ```

- **참고**

  - [가이드 - Watcher](02_essentials.md#guide-essentials-watchers)
  - [가이드 - Watcher 디버깅](06_reactivity_and_rendering_in_depth.md#guide-extras-reactivity-in-depth-watcher-debugging)

<a id="api-reactivity-core-onwatchercleanup"></a>

### onWatcherCleanup()  (3.5+)
현재 watcher가 다시 실행되기 직전에 실행할 정리 함수를 등록합니다. `watchEffect` 효과 함수나 `watch` 콜백 함수의 동기 실행 중에만 호출할 수 있습니다(즉, async 함수에서 `await` 이후에는 호출할 수 없습니다).

- **타입**

  ```ts
  function onWatcherCleanup(
    cleanupFn: () => void,
    failSilently?: boolean
  ): void
  ```

- **예시**

  ```ts
  import { watch, onWatcherCleanup } from 'vue'

  watch(id, (newId) => {
    const { response, cancel } = doAsyncWork(newId)
    // `cancel`은 `id`가 변경되면 호출되어,
    // 이전 요청이 아직 완료되지 않았다면 취소합니다
    onWatcherCleanup(cancel)
  })
  ```

---

<a id="api-reactivity-utilities"></a>

<a id="api-reactivity-utilities-reactivity-api-utilities"></a>

## 반응성 API: 유틸리티

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/reactivity-utilities.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/reactivity-utilities.md

<a id="api-reactivity-utilities-isref"></a>

### isRef()
값이 ref 객체인지 확인합니다.

- **타입**

  ```ts
  function isRef<T>(r: Ref<T> | unknown): r is Ref<T>
  ```

  반환 타입이 [타입 프레디케이트](https://www.typescriptlang.org/docs/handbook/2/narrowing.html#using-type-predicates)임에 유의하세요. 즉, `isRef`는 타입 가드로 사용할 수 있습니다:

  ```ts
  let foo: unknown
  if (isRef(foo)) {
    // foo의 타입이 Ref<unknown>으로 좁혀집니다
    foo.value
  }
  ```

<a id="api-reactivity-utilities-unref"></a>

### unref()
인자가 ref이면 내부 값을 반환하고, 그렇지 않으면 인자 자체를 반환합니다. 이는 `val = isRef(val) ? val.value : val`의 축약 함수입니다.

- **타입**

  ```ts
  function unref<T>(ref: T | Ref<T>): T
  ```

- **예시**

  ```ts
  function useFoo(x: number | Ref<number>) {
    const unwrapped = unref(x)
    // unwrapped는 이제 number임이 보장됩니다
  }
  ```

<a id="api-reactivity-utilities-toref"></a>

### toRef()
값 / ref / getter를 ref로 정규화하는 데 사용할 수 있습니다 (3.3+).

또한 소스 반응형 객체의 속성에 대한 ref를 생성하는 데 사용할 수도 있습니다. 생성된 ref는 소스 속성과 동기화됩니다: 소스 속성을 변경하면 ref가 업데이트되고, 그 반대도 마찬가지입니다.

- **타입**

  ```ts
  // 정규화 시그니처 (3.3+)
  function toRef<T>(
    value: T
  ): T extends () => infer R
    ? Readonly<Ref<R>>
    : T extends Ref
    ? T
    : Ref<UnwrapRef<T>>

  // 객체 속성 시그니처
  function toRef<T extends object, K extends keyof T>(
    object: T,
    key: K,
    defaultValue?: T[K]
  ): ToRef<T[K]>

  type ToRef<T> = T extends Ref ? T : Ref<T>
  ```

- **예시**

  정규화 시그니처 (3.3+):

  ```js
  // 기존 ref는 그대로 반환합니다
  toRef(existingRef)

  // getter를 .value 접근 시 호출하는 readonly ref를 생성합니다
  toRef(() => props.foo)

  // 함수가 아닌 값에서 일반 ref를 생성합니다
  // ref(1)과 동일합니다
  toRef(1)
  ```

  객체 속성 시그니처:

  ```js
  const state = reactive({
    foo: 1,
    bar: 2
  })

  // 원본 속성과 동기화되는 양방향 ref
  const fooRef = toRef(state, 'foo')

  // ref를 변경하면 원본도 업데이트됩니다
  fooRef.value++
  console.log(state.foo) // 2

  // 원본을 변경해도 ref가 업데이트됩니다
  state.foo++
  console.log(fooRef.value) // 3
  ```

  이는 다음과 다릅니다:

  ```js
  const fooRef = ref(state.foo)
  ```

  위의 ref는 `state.foo`와 **동기화되지 않습니다**. `ref()`가 단순 숫자 값을 받기 때문입니다.

  `toRef()`는 prop의 ref를 컴포저블(composable) 함수에 전달하고 싶을 때 유용합니다:

  ```vue
  <script setup>
  import { toRef } from 'vue'

  const props = defineProps(/* ... */)

  // `props.foo`를 ref로 변환한 후
  // 컴포저블에 전달
  useSomeFeature(toRef(props, 'foo'))

  // getter 문법 - 3.3+에서 권장
  useSomeFeature(toRef(() => props.foo))
  </script>
  ```

  `toRef`를 컴포넌트(component) props와 함께 사용할 때는 props 변경에 대한 일반적인 제한이 여전히 적용됩니다. ref에 새 값을 할당하려고 하면 prop을 직접 수정하려는 것과 동일하며 허용되지 않습니다. 이 경우 [`computed`](07_composition_and_reactivity_apis.md#api-reactivity-core-computed)의 `get`과 `set`을 사용하는 것을 고려할 수 있습니다. 자세한 내용은 [컴포넌트에서 `v-model` 사용하기](03_components_and_reusability.md#guide-components-v-model) 가이드를 참고하세요.

  객체 속성 시그니처를 사용할 때, `toRef()`는 소스 속성이 현재 존재하지 않더라도 사용 가능한 ref를 반환합니다. 이를 통해 [`toRefs`](07_composition_and_reactivity_apis.md#api-reactivity-utilities-torefs)로는 감지되지 않는 선택적 속성도 다룰 수 있습니다.

<a id="api-reactivity-utilities-tovalue"></a>

### toValue()
- 3.3+에서만 지원

값 / ref / getter를 값으로 정규화합니다. 이는 [unref()](07_composition_and_reactivity_apis.md#api-reactivity-utilities-unref)와 유사하지만, getter도 정규화한다는 점이 다릅니다. 인자가 getter라면 getter를 호출하고 그 결과를 반환합니다.

이 함수는 [컴포저블](03_components_and_reusability.md#guide-reusability-composables)에서 값, ref, getter 중 어떤 것이든 받을 수 있는 인자를 정규화할 때 사용할 수 있습니다.

- **타입**

  ```ts
  function toValue<T>(source: T | Ref<T> | (() => T)): T
  ```

- **예시**

  ```js
  toValue(1) //       --> 1
  toValue(ref(1)) //  --> 1
  toValue(() => 1) // --> 1
  ```

  컴포저블에서 인자 정규화하기:

  ```ts
  import type { MaybeRefOrGetter } from 'vue'

  function useFeature(id: MaybeRefOrGetter<number>) {
    watch(() => toValue(id), id => {
      // id 변경에 반응
    })
  }

  // 이 컴포저블은 다음 중 어떤 것도 지원합니다:
  useFeature(1)
  useFeature(ref(1))
  useFeature(() => 1)
  ```

<a id="api-reactivity-utilities-torefs"></a>

### toRefs()
반응형 객체를 변환하여, 결과 객체의 각 속성이 원본 객체의 해당 속성을 가리키는 ref가 되도록 합니다. 각 ref는 [`toRef()`](07_composition_and_reactivity_apis.md#api-reactivity-utilities-toref)를 사용해 생성됩니다.

- **타입**

  ```ts
  function toRefs<T extends object>(
    object: T
  ): {
    [K in keyof T]: ToRef<T[K]>
  }

  type ToRef = T extends Ref ? T : Ref<T>
  ```

- **예시**

  ```js
  const state = reactive({
    foo: 1,
    bar: 2
  })

  const stateAsRefs = toRefs(state)
  /*
  stateAsRefs의 타입: {
    foo: Ref<number>,
    bar: Ref<number>
  }
  */

  // ref와 원본 속성이 "연결"되어 있습니다
  state.foo++
  console.log(stateAsRefs.foo.value) // 2

  stateAsRefs.foo.value++
  console.log(state.foo) // 3
  ```

  `toRefs`는 컴포저블 함수에서 반응형 객체를 반환하면서, 소비하는 컴포넌트가 반환된 객체를 구조 분해/스프레드해도 반응성(reactivity)을 잃지 않게 하고 싶을 때 유용합니다:

  ```js
  function useFeatureX() {
    const state = reactive({
      foo: 1,
      bar: 2
    })

    // ...state에 대한 로직

    // 반환 시 ref로 변환
    return toRefs(state)
  }

  // 구조 분해해도 반응성을 잃지 않습니다
  const { foo, bar } = useFeatureX()
  ```

  `toRefs`는 호출 시점에 소스 객체에서 열거 가능한 속성에 대해서만 ref를 생성합니다. 아직 존재하지 않을 수 있는 속성에 대한 ref를 만들려면 [`toRef`](07_composition_and_reactivity_apis.md#api-reactivity-utilities-toref)를 사용하세요.

<a id="api-reactivity-utilities-isproxy"></a>

### isProxy()
객체가 [`reactive()`](07_composition_and_reactivity_apis.md#api-reactivity-core-reactive), [`readonly()`](07_composition_and_reactivity_apis.md#api-reactivity-core-readonly), [`shallowReactive()`](07_composition_and_reactivity_apis.md#api-reactivity-advanced-shallowreactive) 또는 [`shallowReadonly()`](07_composition_and_reactivity_apis.md#api-reactivity-advanced-shallowreadonly)로 생성된 프록시(proxy)인지 확인합니다.

- **타입**

  ```ts
  function isProxy(value: any): boolean
  ```

<a id="api-reactivity-utilities-isreactive"></a>

### isReactive()
객체가 [`reactive()`](07_composition_and_reactivity_apis.md#api-reactivity-core-reactive) 또는 [`shallowReactive()`](07_composition_and_reactivity_apis.md#api-reactivity-advanced-shallowreactive)로 생성된 프록시인지 확인합니다.

- **타입**

  ```ts
  function isReactive(value: unknown): boolean
  ```

<a id="api-reactivity-utilities-isreadonly"></a>

### isReadonly()
전달된 값이 readonly 객체인지 확인합니다. readonly 객체의 속성은 변경될 수 있지만, 전달된 객체를 통해 직접 할당할 수는 없습니다.

[`readonly()`](07_composition_and_reactivity_apis.md#api-reactivity-core-readonly)와 [`shallowReadonly()`](07_composition_and_reactivity_apis.md#api-reactivity-advanced-shallowreadonly)로 생성된 프록시는 모두 readonly로 간주되며, `set` 함수가 없는 [`computed()`](07_composition_and_reactivity_apis.md#api-reactivity-core-computed) ref도 마찬가지입니다.

- **타입**

  ```ts
  function isReadonly(value: unknown): boolean
  ```

<a id="api-reactivity-utilities-isshallow"></a>

### isShallow()
객체가 [`shallowRef`](07_composition_and_reactivity_apis.md#api-reactivity-advanced-shallowref), [`shallowReactive()`](07_composition_and_reactivity_apis.md#api-reactivity-advanced-shallowreactive) 또는 [`shallowReadonly()`](07_composition_and_reactivity_apis.md#api-reactivity-advanced-shallowreadonly)로 생성된 프록시인지 확인합니다.

- **타입**

  ```ts
  function isShallow(value: unknown): boolean
  ```

---

<a id="api-reactivity-advanced"></a>

<a id="api-reactivity-advanced-reactivity-api-advanced"></a>

## 반응성 API: 고급

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/reactivity-advanced.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/reactivity-advanced.md

<a id="api-reactivity-advanced-shallowref"></a>

### shallowRef()
[`ref()`](07_composition_and_reactivity_apis.md#api-reactivity-core-ref)의 얕은(shallow) 버전입니다.

- **타입**

  ```ts
  function shallowRef<T>(value: T): ShallowRef<T>

  interface ShallowRef<T> {
    value: T
  }
  ```

- **세부사항**

  `ref()`와 달리, 얕은 ref의 내부 값은 그대로 저장되고 노출되며, 깊은 반응성(reactivity)으로 변환되지 않습니다. 오직 `.value` 접근만 반응성을 가집니다.

  `shallowRef()`는 일반적으로 대용량 데이터 구조의 성능 최적화나 외부 상태 관리 시스템과의 통합에 사용됩니다.

- **예시**

  ```js
  const state = shallowRef({ count: 1 })

  // 변경을 트리거하지 않음
  state.value.count = 2

  // 변경을 트리거함
  state.value = { count: 2 }
  ```

- **관련 문서**
  - [가이드 - 대용량 불변 구조의 반응성 오버헤드 줄이기](05_scaling_typescript_and_best_practices.md#guide-best-practices-performance-reduce-reactivity-overhead-for-large-immutable-structures)
  - [가이드 - 외부 상태 시스템과의 통합](06_reactivity_and_rendering_in_depth.md#guide-extras-reactivity-in-depth-integration-with-external-state-systems)

<a id="api-reactivity-advanced-triggerref"></a>

### triggerRef()
[shallow ref](07_composition_and_reactivity_apis.md#api-reactivity-advanced-shallowref)에 의존하는 효과를 강제로 트리거합니다. 이는 일반적으로 shallow ref의 내부 값을 깊게 변경한 후에 사용됩니다.

- **타입**

  ```ts
  function triggerRef(ref: ShallowRef): void
  ```

- **예시**

  ```js
  const shallow = shallowRef({
    greet: 'Hello, world'
  })

  // 첫 실행 시 "Hello, world"를 한 번 출력함
  watchEffect(() => {
    console.log(shallow.value.greet)
  })

  // ref가 shallow이기 때문에 효과를 트리거하지 않음
  shallow.value.greet = 'Hello, universe'

  // "Hello, universe"를 출력함
  triggerRef(shallow)
  ```

<a id="api-reactivity-advanced-customref"></a>

### customRef()
의존성 추적과 업데이트 트리거를 명시적으로 제어할 수 있는 커스텀 ref를 생성합니다.

- **타입**

  ```ts
  function customRef<T>(factory: CustomRefFactory<T>): Ref<T>

  type CustomRefFactory<T> = (
    track: () => void,
    trigger: () => void
  ) => {
    get: () => T
    set: (value: T) => void
  }
  ```

- **세부사항**

  `customRef()`는 팩토리 함수를 받으며, 이 함수는 `track`과 `trigger` 함수를 인자로 받아 `get`과 `set` 메서드를 가진 객체를 반환해야 합니다.

  일반적으로 `track()`은 `get()` 내부에서, `trigger()`는 `set()` 내부에서 호출되어야 합니다. 하지만 언제 호출할지, 혹은 호출하지 않을지에 대한 완전한 제어권이 있습니다.

- **예시**

  마지막 set 호출로부터 일정 시간이 지난 후에만 값을 업데이트하는 디바운스(debounce) ref를 생성합니다:

  ```js
  import { customRef } from 'vue'

  export function useDebouncedRef(value, delay = 200) {
    let timeout
    return customRef((track, trigger) => {
      return {
        get() {
          track()
          return value
        },
        set(newValue) {
          clearTimeout(timeout)
          timeout = setTimeout(() => {
            value = newValue
            trigger()
          }, delay)
        }
      }
    })
  }
  ```

  컴포넌트(component)에서의 사용 예시:

  ```vue
  <script setup>
  import { useDebouncedRef } from './debouncedRef'
  const text = useDebouncedRef('hello')
  </script>

  <template>
    <input v-model="text" />
  </template>
  ```

  [Playground에서 시도해보기](https://play.vuejs.org/#eNplUkFugzAQ/MqKC1SiIekxIpEq9QVV1BMXCguhBdsyaxqE/PcuGAhNfYGd3Z0ZDwzeq1K7zqB39OI205UiaJGMOieiapTUBAOYFt/wUxqRYf6OBVgotGzA30X5Bt59tX4iMilaAsIbwelxMfCvWNfSD+Gw3++fEhFHTpLFuCBsVJ0ScgUQjw6Az+VatY5PiroHo3IeaeHANlkrh7Qg1NBL43cILUmlMAfqVSXK40QUOSYmHAZHZO0KVkIZgu65kTnWp8Qb+4kHEXfjaDXkhd7DTTmuNZ7MsGyzDYbz5CgSgbdppOBFqqT4l0eX1gZDYOm057heOBQYRl81coZVg9LQWGr+IlrchYKAdJp9h0C6KkvUT3A6u8V1dq4ASqRgZnVnWg04/QWYNyYzC2rD5Y3/hkDgz8fY/cOT1ZjqizMZzGY3rDPC12KGZYyd3J26M8ny1KKx7c3X25q1c1wrZN3L9LCMWs/+AmeG6xI=)

  **주의해서 사용하세요**
  customRef의 getter가 호출될 때마다 새 객체를 반환한다면, 그 값을 자식 컴포넌트에 prop으로 전달할 때 주의해야 합니다.

  부모가 다른 반응형 상태의 변경으로 다시 렌더링되면 customRef의 getter도 다시 평가되어 새 객체를 반환합니다. 자식은 새 prop이 이전 객체와 다르다고 판단하므로 관련 반응성 의존성을 실행합니다. 하지만 customRef의 setter는 호출되지 않았으므로, 부모에서 이 ref에 의존하는 효과는 실행되지 않습니다.

  [Playground에서 확인하기](https://play.vuejs.org/#eNqFVEtP3DAQ/itTS9Vm1ZCt1J6WBZUiDvTQIsoNcwiOkzU4tmU7+9Aq/71jO1mCWuhlN/PyfPP45kAujCk2HSdLsnLMCuPBcd+Zc6pEa7T1cADWOa/bW17nYMPPtvRsDT3UVrcww+DZ0flStybpKSkWQQqPU0IVVUwr58FYvdvDWXgpu6ek1pqSHL0fS0vJw/z0xbN1jUPHY/Ys87Zkzzl4K5qG2zmcnUN2oAqg4T6bQ/wENKNXNk+CxWKsSlmLTSk7XlhedYxnWclYDiK+MkQCoK4wnVtnIiBJuuEJNA2qPof7hzkEoc8DXgg9yzYTBBFgNr4xyY4FbaK2p6qfI0iqFgtgulOe27HyQRy69Dk1JXY9C03JIeQ6wg4xWvJCqFpnlNytOcyC2wzYulQNr0Ao+Mhw0KnTTEttl/CIaIJiMz8NGBHFtYetVrPwa58/IL48Zag4N0ssquNYLYBoW16J0vOkC3VQtVqk7cG9QcHz1kj0QAlgVYkNMFk6d0bJ1pbGYKUkmtD42HmvFfi94WhOEiXwjUnBnlEz9OLTJwy5qCo44D4O7en71SIFjI/F9VuG4jEy/GHQKq5hQrJAKOc4uNVighBF5/cygS0GgOMoK+HQb7+EWvLdMM7weVIJy5kXWi0Rj+xaNRhLKRp1IvB9hxYegA6WJ1xkUe9PcF4e9a+suA3YwYiC5MQ79KlFUzw5rZCZEUtoRWuE5PaXCXmxtuWIkpJSSr39EXXHQcWYNWfP/9A/uV3QUXJjueN2E1ZhtPnSIqGS+er3T77D76Ox1VUn0fsd4y3HfewCxuT2vVMVwp74RbTX8WQI1dy5qx12xI1Fpa1K5AreeEHCCN8q/QXul+LrSC3s4nh93jltkVPDIYt5KJkcIKStCReo4rVQ/CZI6dyEzToCCJu7hAtry/1QH/qXncQB400KJwqPxZHxEyona0xS/E3rt1m9Ld1rZl+uhaxecRtP3EjtgddCyimtXyj9H/Ii3eId7uOGTkyk/wOEbQ9h)


<a id="api-reactivity-advanced-shallowreactive"></a>

### shallowReactive()
[`reactive()`](07_composition_and_reactivity_apis.md#api-reactivity-core-reactive)의 얕은(shallow) 버전입니다.

- **타입**

  ```ts
  function shallowReactive<T extends object>(target: T): T
  ```

- **세부사항**

  `reactive()`와 달리, 깊은 변환이 없습니다: 얕은 반응성 객체에서는 루트 레벨 속성만 반응성을 가집니다. 속성 값은 그대로 저장되고 노출됩니다. 즉, ref 값을 가진 속성은 **자동으로 언래핑되지 않습니다**.

  **주의해서 사용하세요**
  얕은 데이터 구조는 컴포넌트의 루트 레벨 상태에만 사용해야 합니다. 깊은 반응성 객체 내부에 중첩해서 사용하면 일관성 없는 반응성 트리 구조가 만들어져, 동작을 이해하고 디버깅하기 어려워질 수 있습니다.


- **예시**

  ```js
  const state = shallowReactive({
    foo: 1,
    nested: {
      bar: 2
    }
  })

  // state의 자체 속성 변경은 반응성을 가짐
  state.foo++

  // ...하지만 중첩 객체는 변환하지 않음
  isReactive(state.nested) // false

  // 반응성 없음
  state.nested.bar++
  ```

<a id="api-reactivity-advanced-shallowreadonly"></a>

### shallowReadonly()
[`readonly()`](07_composition_and_reactivity_apis.md#api-reactivity-core-readonly)의 얕은(shallow) 버전입니다.

- **타입**

  ```ts
  function shallowReadonly<T extends object>(target: T): Readonly<T>
  ```

- **세부사항**

  `readonly()`와 달리, 깊은 변환이 없습니다: 루트 레벨 속성만 읽기 전용으로 만들어집니다. 속성 값은 그대로 저장되고 노출됩니다. 즉, ref 값을 가진 속성은 **자동으로 언래핑되지 않습니다**.

  **주의해서 사용하세요**
  얕은 데이터 구조는 컴포넌트의 루트 레벨 상태에만 사용해야 합니다. 깊은 반응성 객체 내부에 중첩해서 사용하면 일관성 없는 반응성 트리 구조가 만들어져, 동작을 이해하고 디버깅하기 어려워질 수 있습니다.


- **예시**

  ```js
  const state = shallowReadonly({
    foo: 1,
    nested: {
      bar: 2
    }
  })

  // state의 자체 속성 변경은 실패함
  state.foo++

  // ...하지만 중첩 객체에는 적용되지 않음
  isReadonly(state.nested) // false

  // 동작함
  state.nested.bar++
  ```

<a id="api-reactivity-advanced-toraw"></a>

### toRaw()
Vue에서 생성된 프록시(proxy)의 원본, 즉 가공되지 않은 객체를 반환합니다.

- **타입**

  ```ts
  function toRaw<T>(proxy: T): T
  ```

- **세부사항**

  `toRaw()`는 [`reactive()`](07_composition_and_reactivity_apis.md#api-reactivity-core-reactive), [`readonly()`](07_composition_and_reactivity_apis.md#api-reactivity-core-readonly), [`shallowReactive()`](07_composition_and_reactivity_apis.md#api-reactivity-advanced-shallowreactive), [`shallowReadonly()`](07_composition_and_reactivity_apis.md#api-reactivity-advanced-shallowreadonly)로 생성된 프록시에서 원본 객체를 반환할 수 있습니다.

  이는 프록시 접근/추적 오버헤드 없이 임시로 읽거나, 변경을 트리거하지 않고 쓸 수 있는 탈출구입니다. 원본 객체에 대한 지속적인 참조를 유지하는 것은 **권장되지 않습니다**. 주의해서 사용하세요.

- **예시**

  ```js
  const foo = {}
  const reactiveFoo = reactive(foo)

  console.log(toRaw(reactiveFoo) === foo) // true
  ```

<a id="api-reactivity-advanced-markraw"></a>

### markRaw()
객체가 프록시로 변환되지 않도록 표시합니다. 객체 자체를 반환합니다.

- **타입**

  ```ts
  function markRaw<T extends object>(value: T): T
  ```

- **예시**

  ```js
  const foo = markRaw({})
  console.log(isReactive(reactive(foo))) // false

  // 다른 반응성 객체 내부에 중첩되어 있어도 동작함
  const bar = reactive({ foo })
  console.log(isReactive(bar.foo)) // false
  ```

  **주의해서 사용하세요**
  `markRaw()`와 `shallowReactive()`와 같은 얕은 API를 사용하면, 기본으로 적용되는 깊은 반응성/읽기 전용 변환에서 선택적으로 벗어나, 상태 그래프에 가공되지 않은(non-proxied) 객체를 삽입할 수 있습니다. 다양한 이유로 사용할 수 있습니다:

  - 일부 값은 반응성으로 만들면 안 됩니다. 예를 들어 복잡한 3rd party 클래스 인스턴스(instance)나 Vue 컴포넌트 객체 등입니다.

  - 프록시 변환을 건너뛰면 불변 데이터 소스를 가진 대용량 리스트 렌더링(rendering) 시 성능 향상을 얻을 수 있습니다.

  프록시 변환에서 제외되는 것은 루트 객체뿐입니다. 내부의 중첩 객체까지 `markRaw`로 표시하지 않았다면, 그 중첩 객체를 다른 반응형 객체에 넣고 다시 읽을 때는 프록시를 얻게 됩니다. 이때 원본과 프록시를 섞어 쓰면 같은 객체인지 비교하는 연산에서 예상과 다른 결과가 나올 수 있습니다. 이를 **아이덴티티 위험**이라고 합니다:

  ```js
  const foo = markRaw({
    nested: {}
  })

  const bar = reactive({
    // `foo`는 raw로 표시되었지만, foo.nested는 그렇지 않음.
    nested: foo.nested
  })

  console.log(foo.nested === bar.nested) // false
  ```

  아이덴티티 위험은 일반적으로 드뭅니다. 하지만 이러한 위험을 안전하게 피하면서 API를 제대로 활용하려면, 반응성 시스템의 동작 원리를 확실하게 이해하고 있어야 합니다.


<a id="api-reactivity-advanced-effectscope"></a>

### effectScope()
효과 스코프 객체를 생성하며, 그 안에서 만들어진 반응성 효과(즉, computed와 watcher)를 함께 캡처하고 일괄적으로 해제(dispose)할 수 있습니다. 이 API의 자세한 사용 사례는 해당 [RFC](https://github.com/vuejs/rfcs/blob/master/active-rfcs/0041-reactivity-effect-scope.md)를 참고하세요.

- **타입**

  ```ts
  function effectScope(detached?: boolean): EffectScope

  interface EffectScope {
    run<T>(fn: () => T): T | undefined // 스코프가 비활성화된 경우 undefined
    stop(): void
  }
  ```

- **예시**

  ```js
  const scope = effectScope()

  scope.run(() => {
    const doubled = computed(() => counter.value * 2)

    watch(doubled, () => console.log(doubled.value))

    watchEffect(() => console.log('Count: ', doubled.value))
  })

  // 스코프 내의 모든 효과를 해제하려면
  scope.stop()
  ```

<a id="api-reactivity-advanced-getcurrentscope"></a>

### getCurrentScope()
현재 활성화된 [effect scope](07_composition_and_reactivity_apis.md#api-reactivity-advanced-effectscope)가 있다면 반환합니다.

- **타입**

  ```ts
  function getCurrentScope(): EffectScope | undefined
  ```

<a id="api-reactivity-advanced-onscopedispose"></a>

### onScopeDispose()
현재 활성화된 [effect scope](07_composition_and_reactivity_apis.md#api-reactivity-advanced-effectscope)에 dispose 콜백(callback)을 등록합니다. 해당 effect scope가 중지될 때 콜백이 호출됩니다.

이 메서드는 재사용 가능한 컴포지션 함수에서 컴포넌트에 종속되지 않는 `onUnmounted`의 대안으로 사용할 수 있습니다. 각 Vue 컴포넌트의 `setup()` 함수도 effect scope 내에서 호출되기 때문입니다.

활성화된 effect scope 없이 이 함수를 호출하면 경고가 발생합니다. 3.5+에서는 두 번째 인자로 `true`를 전달하여 이 경고를 억제할 수 있습니다.

- **타입**

  ```ts
  function onScopeDispose(fn: () => void, failSilently?: boolean): void
  ```

---

<a id="api-composition-api-lifecycle"></a>

<a id="api-composition-api-lifecycle-composition-api-lifecycle-hooks"></a>

## 컴포지션 API: 라이프사이클 훅

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/composition-api-lifecycle.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/composition-api-lifecycle.md

**사용 참고**
이 페이지에 나열된 모든 API는 컴포넌트(component)의 `setup()` 단계에서 동기적으로 호출되어야 합니다. 자세한 내용은 [가이드 - 라이프사이클(lifecycle) 훅](02_essentials.md#guide-essentials-lifecycle)을 참고하세요.


<a id="api-composition-api-lifecycle-onmounted"></a>

### onMounted()
컴포넌트가 마운트(mount)된 후 호출될 콜백(callback)을 등록합니다.

- **타입**

  ```ts
  function onMounted(callback: () => void, target?: ComponentInternalInstance | null): void
  ```

- **상세 설명**

  컴포넌트는 다음과 같은 경우 마운트된 것으로 간주됩니다:

  - 모든 동기 자식 컴포넌트가 마운트된 후 (비동기 컴포넌트나 `<Suspense>` 트리 내의 컴포넌트는 포함하지 않음).

  - 자신의 DOM 트리가 생성되어 부모 컨테이너에 삽입된 후. 애플리케이션의 루트 컨테이너가 문서 내에 있는 경우에만 컴포넌트의 DOM 트리가 문서 내에 있음이 보장됩니다.

  이 훅(hook)은 일반적으로 컴포넌트의 렌더링(rendering)된 DOM에 접근해야 하는 부수 효과를 수행하거나, [서버 렌더링 애플리케이션](05_scaling_typescript_and_best_practices.md#guide-scaling-up-ssr)에서 DOM 관련 코드를 클라이언트로 제한할 때 사용됩니다.

  **이 훅은 서버 사이드 렌더링 중에는 호출되지 않습니다.**

- **예시**

  템플릿(template) ref를 통해 엘리먼트에 접근하기:

  ```vue
  <script setup>
  import { ref, onMounted } from 'vue'

  const el = ref()

  onMounted(() => {
    el.value // <div>
  })
  </script>

  <template>
    <div ref="el"></div>
  </template>
  ```

<a id="api-composition-api-lifecycle-onupdated"></a>

### onUpdated()
반응형 상태 변경으로 인해 컴포넌트의 DOM 트리가 업데이트된 후 호출될 콜백을 등록합니다.

- **타입**

  ```ts
  function onUpdated(callback: () => void, target?: ComponentInternalInstance | null): void
  ```

- **상세 설명**

  부모 컴포넌트의 updated 훅은 자식 컴포넌트의 훅이 호출된 후에 호출됩니다.

  이 훅은 컴포넌트의 모든 DOM 업데이트 후에 호출되며, 이 업데이트는 서로 다른 상태 변경들로 인해 발생할 수 있습니다. 여러 상태 변경이 성능상의 이유로 하나의 렌더 사이클로 묶일 수 있기 때문입니다. 특정 상태 변경 후에 업데이트된 DOM에 접근해야 한다면 [nextTick()](07_composition_and_reactivity_apis.md#api-general-nexttick)을 대신 사용하세요.

  **이 훅은 서버 사이드 렌더링 중에는 호출되지 않습니다.**

  **주의**
  updated 훅 내에서 컴포넌트 상태를 변경하지 마세요. 무한 업데이트 루프가 발생할 수 있습니다!


- **예시**

  업데이트된 DOM에 접근하기:

  ```vue
  <script setup>
  import { ref, onUpdated } from 'vue'

  const count = ref(0)

  onUpdated(() => {
    // 텍스트 내용이 현재 `count.value`와 같아야 합니다.
    console.log(document.getElementById('count').textContent)
  })
  </script>

  <template>
    <button id="count" @click="count++">{{ count }}</button>
  </template>
  ```

<a id="api-composition-api-lifecycle-onunmounted"></a>

### onUnmounted()
컴포넌트가 언마운트(unmount)된 후 호출될 콜백을 등록합니다.

- **타입**

  ```ts
  function onUnmounted(callback: () => void, target?: ComponentInternalInstance | null): void
  ```

- **상세 설명**

  컴포넌트는 다음과 같은 경우 언마운트된 것으로 간주됩니다:

  - 모든 자식 컴포넌트가 언마운트된 후.

  - 모든 관련 반응형 효과(렌더 효과 및 `setup()` 중에 생성된 computed / watcher)가 중지된 후.

  이 훅은 타이머, DOM 이벤트 리스너(listener), 서버 연결 등 수동으로 생성한 부수 효과를 정리할 때 사용하세요.

  **이 훅은 서버 사이드 렌더링 중에는 호출되지 않습니다.**

- **예시**

  ```vue
  <script setup>
  import { onMounted, onUnmounted } from 'vue'

  let intervalId
  onMounted(() => {
    intervalId = setInterval(() => {
      // ...
    })
  })

  onUnmounted(() => clearInterval(intervalId))
  </script>
  ```

<a id="api-composition-api-lifecycle-onbeforemount"></a>

### onBeforeMount()
컴포넌트가 마운트되기 직전에 호출될 훅을 등록합니다.

- **타입**

  ```ts
  function onBeforeMount(callback: () => void, target?: ComponentInternalInstance | null): void
  ```

- **상세 설명**

  이 훅이 호출될 때, 컴포넌트는 반응형 상태 설정을 마쳤지만 아직 DOM 노드가 생성되지 않았습니다. 곧 처음으로 DOM 렌더 효과를 실행할 예정입니다.

  **이 훅은 서버 사이드 렌더링 중에는 호출되지 않습니다.**

<a id="api-composition-api-lifecycle-onbeforeupdate"></a>

### onBeforeUpdate()
반응형 상태 변경으로 인해 컴포넌트의 DOM 트리가 업데이트되기 직전에 호출될 훅을 등록합니다.

- **타입**

  ```ts
  function onBeforeUpdate(callback: () => void, target?: ComponentInternalInstance | null): void
  ```

- **상세 설명**

  이 훅은 Vue가 DOM을 업데이트하기 전에 DOM 상태에 접근할 때 사용할 수 있습니다. 이 훅 내에서 컴포넌트 상태를 변경해도 안전합니다.

  **이 훅은 서버 사이드 렌더링 중에는 호출되지 않습니다.**

<a id="api-composition-api-lifecycle-onbeforeunmount"></a>

### onBeforeUnmount()
컴포넌트 인스턴스(instance)가 언마운트되기 직전에 호출될 훅을 등록합니다.

- **타입**

  ```ts
  function onBeforeUnmount(callback: () => void, target?: ComponentInternalInstance | null): void
  ```

- **상세 설명**

  이 훅이 호출될 때, 컴포넌트 인스턴스는 여전히 완전히 동작 가능한 상태입니다.

  **이 훅은 서버 사이드 렌더링 중에는 호출되지 않습니다.**

<a id="api-composition-api-lifecycle-onerrorcaptured"></a>

### onErrorCaptured()
하위 컴포넌트에서 전파된 오류가 포착되었을 때 호출될 훅을 등록합니다.

- **타입**

  ```ts
  function onErrorCaptured(callback: ErrorCapturedHook): void

  type ErrorCapturedHook = (
    err: unknown,
    instance: ComponentPublicInstance | null,
    info: string
  ) => boolean | void
  ```

- **상세 설명**

  오류는 다음과 같은 소스에서 포착될 수 있습니다:

  - 컴포넌트 렌더링
  - 이벤트 핸들러
  - 라이프사이클 훅
  - `setup()` 함수
  - watcher
  - 커스텀 디렉티브(directive) 훅
  - 트랜지션(transition) 훅

  이 훅은 오류, 오류를 발생시킨 컴포넌트 인스턴스, 오류 소스 타입을 지정하는 정보 문자열, 이렇게 세 개의 인자를 받습니다.

  **참고**
  프로덕션 환경에서는 세 번째 인자(`info`)가 전체 정보 문자열 대신 축약된 코드로 제공됩니다. 코드와 문자열 매핑은 [프로덕션 오류 코드 참조](09_style_guide_examples_and_reference.md#error-reference-index-runtime-errors)에서 확인할 수 있습니다.


  `onErrorCaptured()`에서 컴포넌트 상태를 수정하여 사용자에게 오류 상태를 표시할 수 있습니다. 하지만 오류 상태가 오류를 발생시킨 원래 콘텐츠를 렌더링하지 않도록 주의해야 합니다. 그렇지 않으면 컴포넌트가 무한 렌더 루프에 빠질 수 있습니다.

  이 훅은 `false`를 반환하여 오류의 추가 전파를 중단할 수 있습니다. 아래의 오류 전파 규칙을 참고하세요.

  **오류 전파 규칙**

  - 기본적으로, 애플리케이션 레벨의 [`app.config.errorHandler`](07_composition_and_reactivity_apis.md#api-application-app-config-errorhandler)가 정의되어 있다면 모든 오류는 여전히 해당 핸들러로 전달됩니다. 이를 통해 오류를 한 곳에서 분석 서비스로 보고할 수 있습니다.

  - 컴포넌트의 상속 체인 또는 부모 체인에 여러 개의 `errorCaptured` 훅이 존재하는 경우, 동일한 오류에 대해 모두 하위에서 상위 순서로 호출됩니다. 이는 네이티브 DOM 이벤트의 버블링 메커니즘과 유사합니다.

  - `errorCaptured` 훅 자체에서 오류가 발생하면, 이 오류와 원래 포착된 오류 모두 `app.config.errorHandler`로 전달됩니다.

  - `errorCaptured` 훅이 `false`를 반환하면 오류의 추가 전파가 중단됩니다. 이는 "이 오류는 처리되었으니 무시해야 한다"는 의미입니다. 이 경우 추가적인 `errorCaptured` 훅이나 `app.config.errorHandler`는 이 오류에 대해 호출되지 않습니다.

<a id="api-composition-api-lifecycle-onrendertracked"></a>

### onRenderTracked()  (개발 모드 전용)
컴포넌트의 렌더 효과에 의해 반응형 의존성이 추적될 때 호출되는 디버그 훅을 등록합니다.

**이 훅은 개발 모드에서만 동작하며, 서버 사이드 렌더링 중에는 호출되지 않습니다.**

- **타입**

  ```ts
  function onRenderTracked(callback: DebuggerHook): void

  type DebuggerHook = (e: DebuggerEvent) => void

  type DebuggerEvent = {
    effect: ReactiveEffect
    target: object
    type: TrackOpTypes /* 'get' | 'has' | 'iterate' */
    key: any
  }
  ```

- **참고** [반응성 심층 가이드](06_reactivity_and_rendering_in_depth.md#guide-extras-reactivity-in-depth)

<a id="api-composition-api-lifecycle-onrendertriggered"></a>

### onRenderTriggered()  (개발 모드 전용)
반응형 의존성이 컴포넌트의 렌더 효과를 다시 실행하도록 트리거할 때 호출되는 디버그 훅을 등록합니다.

**이 훅은 개발 모드에서만 동작하며, 서버 사이드 렌더링 중에는 호출되지 않습니다.**

- **타입**

  ```ts
  function onRenderTriggered(callback: DebuggerHook): void

  type DebuggerHook = (e: DebuggerEvent) => void

  type DebuggerEvent = {
    effect: ReactiveEffect
    target: object
    type: TriggerOpTypes /* 'set' | 'add' | 'delete' | 'clear' */
    key: any
    newValue?: any
    oldValue?: any
    oldTarget?: Map<any, any> | Set<any>
  }
  ```

- **참고** [반응성 심층 가이드](06_reactivity_and_rendering_in_depth.md#guide-extras-reactivity-in-depth)

<a id="api-composition-api-lifecycle-onactivated"></a>

### onActivated()
[`<KeepAlive>`](08_component_and_advanced_apis.md#api-built-in-components-keepalive)로 캐시된 트리의 일부로 컴포넌트 인스턴스가 DOM에 삽입된 후 호출될 콜백을 등록합니다.

**이 훅은 서버 사이드 렌더링 중에는 호출되지 않습니다.**

- **타입**

  ```ts
  function onActivated(callback: () => void, target?: ComponentInternalInstance | null): void
  ```

- **참고** [가이드 - 캐시된 인스턴스의 라이프사이클](04_built_ins_and_animation.md#guide-built-ins-keep-alive-lifecycle-of-cached-instance)

<a id="api-composition-api-lifecycle-ondeactivated"></a>

### onDeactivated()
[`<KeepAlive>`](08_component_and_advanced_apis.md#api-built-in-components-keepalive)로 캐시된 트리의 일부로 컴포넌트 인스턴스가 DOM에서 제거된 후 호출될 콜백을 등록합니다.

**이 훅은 서버 사이드 렌더링 중에는 호출되지 않습니다.**

- **타입**

  ```ts
  function onDeactivated(callback: () => void, target?: ComponentInternalInstance | null): void
  ```

- **참고** [가이드 - 캐시된 인스턴스의 라이프사이클](04_built_ins_and_animation.md#guide-built-ins-keep-alive-lifecycle-of-cached-instance)

<a id="api-composition-api-lifecycle-onserverprefetch"></a>

### onServerPrefetch()  (SSR only)
컴포넌트 인스턴스가 서버에서 렌더링되기 전에 해결되어야 하는 비동기 함수를 등록합니다.

- **타입**

  ```ts
  function onServerPrefetch(callback: () => Promise<any>): void
  ```

- **상세 설명**

  콜백이 Promise를 반환하면, 서버 렌더러는 컴포넌트를 렌더링하기 전에 해당 Promise가 해결될 때까지 대기합니다.

  이 훅은 서버 사이드 렌더링 중에만 호출되며, 서버 전용 데이터 패칭에 사용할 수 있습니다.

- **예시**

  ```vue
  <script setup>
  import { ref, onServerPrefetch, onMounted } from 'vue'

  const data = ref(null)

  onServerPrefetch(async () => {
    // 컴포넌트가 초기 요청의 일부로 렌더링됩니다.
    // 서버에서 데이터를 미리 패칭하면 클라이언트보다 더 빠릅니다.
    data.value = await fetchOnServer(/* ... */)
  })

  onMounted(async () => {
    if (!data.value) {
      // 마운트 시 data가 null이면,
      // 컴포넌트가 클라이언트에서 동적으로 렌더링된 것입니다.
      // 대신 클라이언트에서 데이터를 패칭합니다.
      data.value = await fetchOnClient(/* ... */)
    }
  })
  </script>
  ```

- **참고** [서버 사이드 렌더링](05_scaling_typescript_and_best_practices.md#guide-scaling-up-ssr)

---

<a id="api-composition-api-dependency-injection"></a>

<a id="api-composition-api-dependency-injection-composition-api-dependency-injection"></a>

## Composition API: <br>의존성 주입(dependency injection)

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/composition-api-dependency-injection.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/composition-api-dependency-injection.md

<a id="api-composition-api-dependency-injection-provide"></a>

### provide()
하위 컴포넌트에서 주입할 수 있는 값을 제공합니다.

- **타입**

  ```ts
  function provide<T>(key: InjectionKey<T> | string, value: T): void
  ```

- **세부사항**

  `provide()`는 두 개의 인자를 받습니다. 첫 번째는 문자열 또는 심볼이 될 수 있는 key이고, 두 번째는 주입할 값입니다.

  TypeScript를 사용할 때, key는 `InjectionKey`로 캐스팅된 심볼일 수 있습니다. `InjectionKey`는 Vue에서 제공하는 유틸리티 타입으로, `Symbol`을 확장하며 `provide()`와 `inject()` 간의 값 타입을 동기화하는 데 사용할 수 있습니다.

  라이프사이클(lifecycle) 훅 등록 API와 유사하게, `provide()`는 컴포넌트(component)의 `setup()` 단계에서 동기적으로 호출되어야 합니다.

- **예시**

  ```vue
  <script setup>
  import { ref, provide } from 'vue'
  import { countSymbol } from './injectionSymbols'

  // 정적 값 제공
  provide('path', '/project/')

  // 반응형 값 제공
  const count = ref(0)
  provide('count', count)

  // Symbol 키로 제공
  provide(countSymbol, count)
  </script>
  ```

- **관련 문서**
  - [가이드 - Provide / Inject](03_components_and_reusability.md#guide-components-provide-inject)
  - [가이드 - Provide / Inject 타입 지정](05_scaling_typescript_and_best_practices.md#guide-typescript-composition-api-typing-provide-inject)  (TypeScript)

<a id="api-composition-api-dependency-injection-inject"></a>

### inject()
상위 컴포넌트 또는 애플리케이션(`app.provide()`를 통해)에서 제공한 값을 주입합니다.

- **타입**

  ```ts
  // 기본값 없이
  function inject<T>(key: InjectionKey<T> | string): T | undefined

  // 기본값과 함께
  function inject<T>(key: InjectionKey<T> | string, defaultValue: T): T

  // 팩토리와 함께
  function inject<T>(
    key: InjectionKey<T> | string,
    defaultValue: () => T,
    treatDefaultAsFactory: true
  ): T
  ```

- **세부사항**

  첫 번째 인자는 주입 키입니다. Vue는 부모 체인을 따라 올라가며 일치하는 키로 제공된 값을 찾습니다. 부모 체인에서 여러 컴포넌트가 동일한 키를 제공하는 경우, 주입하는 컴포넌트에 가장 가까운 컴포넌트가 체인 상위의 값들을 "가리며(shadow)", 그 컴포넌트의 값이 사용됩니다. 일치하는 키의 값이 없고 기본값도 제공되지 않았다면, `inject()`는 `undefined`를 반환합니다.

  두 번째 인자는 선택 사항이며, 일치하는 값이 없을 때 사용할 기본값입니다.

  두 번째 인자는 값 생성 비용이 큰 경우 값을 반환하는 팩토리 함수가 될 수도 있습니다. 이 경우, 세 번째 인자로 `true`를 전달하여 해당 함수가 값 자체가 아닌 팩토리로 사용되어야 함을 나타내야 합니다.

  라이프사이클 훅(hook) 등록 API와 유사하게, `inject()`는 컴포넌트의 `setup()` 단계에서 동기적으로 호출되어야 합니다.

  TypeScript를 사용할 때, key는 `InjectionKey` 타입일 수 있습니다. `InjectionKey`는 Vue에서 제공하는 유틸리티 타입으로, `Symbol`을 확장하며 `provide()`와 `inject()` 간의 값 타입을 동기화하는 데 사용할 수 있습니다.

- **예시**

  상위 컴포넌트가 이전 `provide()` 예시와 같이 값을 제공했다고 가정합니다:

  ```vue
  <script setup>
  import { inject } from 'vue'
  import { countSymbol } from './injectionSymbols'

  // 기본값 없이 정적 값 주입
  const path = inject('path')

  // 반응형 값 주입
  const count = inject('count')

  // Symbol 키로 주입
  const count2 = inject(countSymbol)

  // 기본값과 함께 주입
  const bar = inject('path', '/default-path')

  // 함수 기본값으로 주입
  const fn = inject('function', () => {})

  // 기본값 팩토리로 주입
  const baz = inject('factory', () => new ExpensiveObject(), true)
  </script>
  ```
  
- **관련 문서**
  - [가이드 - Provide / Inject](03_components_and_reusability.md#guide-components-provide-inject)
  - [가이드 - Provide / Inject 타입 지정](05_scaling_typescript_and_best_practices.md#guide-typescript-composition-api-typing-provide-inject)  (TypeScript)

<a id="api-composition-api-dependency-injection-has-injection-context"></a>

### hasInjectionContext()
- 3.3+에서만 지원

잘못된 위치(예: `setup()` 외부)에서 호출했다는 경고 없이 [inject()](07_composition_and_reactivity_apis.md#api-composition-api-dependency-injection-inject)를 사용할 수 있다면 true를 반환합니다. 이 메서드는 내부적으로 `inject()`를 사용하되 최종 사용자에게 경고를 발생시키지 않으려는 라이브러리에서 사용하도록 설계되었습니다.

- **타입**

  ```ts
  function hasInjectionContext(): boolean
  ```

---

<a id="api-composition-api-helpers"></a>

<a id="api-composition-api-helpers-composition-api-helpers"></a>

## Composition API: Helpers

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/composition-api-helpers.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/composition-api-helpers.md

<a id="api-composition-api-helpers-useattrs"></a>

### useAttrs()
[Setup Context](07_composition_and_reactivity_apis.md#api-composition-api-setup-setup-context)에서 `attrs` 객체를 반환하며, 이 객체에는 현재 컴포넌트(component)의 [폴스루 속성(fallthrough attributes)](03_components_and_reusability.md#guide-components-attrs-fallthrough-attributes)이 포함되어 있습니다. 이 함수는 setup 컨텍스트 객체를 사용할 수 없는 `<script setup>`에서 사용하도록 설계되었습니다.

- **타입**

  ```ts
  function useAttrs(): Record<string, unknown>
  ```

<a id="api-composition-api-helpers-useslots"></a>

### useSlots()
[Setup Context](07_composition_and_reactivity_apis.md#api-composition-api-setup-setup-context)에서 `slots` 객체를 반환하며, 이 객체에는 부모로부터 전달된 슬롯(slot)이 Virtual DOM 노드를 반환하는 호출 가능한 함수로 포함되어 있습니다. 이 함수는 setup 컨텍스트 객체를 사용할 수 없는 `<script setup>`에서 사용하도록 설계되었습니다.

TypeScript를 사용하는 경우, [`defineSlots()`](08_component_and_advanced_apis.md#api-sfc-script-setup-defineslots)를 대신 사용하는 것이 더 좋습니다.

- **타입**

  ```ts
  function useSlots(): Record<string, (...args: any[]) => VNode[]>
  ```

<a id="api-composition-api-helpers-usemodel"></a>

### useModel()
이 함수는 [`defineModel()`](08_component_and_advanced_apis.md#api-sfc-script-setup-definemodel)을 지원하는 기본 헬퍼입니다. `<script setup>`을 사용하는 경우, `defineModel()`을 대신 사용하는 것이 더 좋습니다.

- 3.4+에서만 사용 가능합니다.

- **타입**

  ```ts
  function useModel(
    props: Record<string, any>,
    key: string,
    options?: DefineModelOptions
  ): ModelRef

  type DefineModelOptions<T = any> = {
    get?: (v: T) => any
    set?: (v: T) => any
  }

  type ModelRef<T, M extends PropertyKey = string, G = T, S = T> = Ref<G, S> & [
    ModelRef<T, M, G, S>,
    Record<M, true | undefined>
  ]
  ```

- **예시**

  ```js
  export default {
    props: ['count'],
    emits: ['update:count'],
    setup(props) {
      const msg = useModel(props, 'count')
      msg.value = 1
    }
  }
  ```

- **상세 설명**

  `useModel()`은 SFC를 쓰지 않고 일반 `setup()` 함수로 컴포넌트를 작성할 때 사용할 수 있습니다. 첫 번째 인자는 `props` 객체, 두 번째 인자는 모델 이름입니다. 필요하면 세 번째 인자로 반환되는 모델 ref의 커스텀 getter와 setter를 지정합니다. `defineModel()`과 달리 props와 emits는 직접 선언해야 합니다.

<a id="api-composition-api-helpers-usetemplateref"></a>

### useTemplateRef()  (3.5+)
일치하는 ref 속성을 가진 템플릿(template) 요소 또는 컴포넌트와 동기화되는 얕은 ref를 반환합니다.

- **타입**

  ```ts
  function useTemplateRef<T>(key: string): Readonly<ShallowRef<T | null>>
  ```

- **예시**

  ```vue
  <script setup>
  import { useTemplateRef, onMounted } from 'vue'

  const inputRef = useTemplateRef('input')

  onMounted(() => {
    inputRef.value.focus()
  })
  </script>

  <template>
    <input ref="input" />
  </template>
  ```

- **더 알아보기**
  - [가이드 - 템플릿 ref](02_essentials.md#guide-essentials-template-refs)
  - [가이드 - 템플릿 ref 타입 지정](05_scaling_typescript_and_best_practices.md#guide-typescript-composition-api-typing-template-refs)  (TypeScript)
  - [가이드 - 컴포넌트 템플릿 ref 타입 지정](05_scaling_typescript_and_best_practices.md#guide-typescript-composition-api-typing-component-template-refs)  (TypeScript)

<a id="api-composition-api-helpers-useid"></a>

### useId()  (3.5+)
접근성 속성이나 폼 요소를 위한 애플리케이션별 고유 ID를 생성하는 데 사용됩니다.

- **타입**

  ```ts
  function useId(): string
  ```

- **예시**

  ```vue
  <script setup>
  import { useId } from 'vue'

  const id = useId()
  </script>

  <template>
    <form>
      <label :for="id">Name:</label>
      <input :id="id" type="text" />
    </form>
  </template>
  ```

- **상세 설명**

  `useId()`로 생성된 ID는 애플리케이션별로 고유합니다. 폼 요소와 접근성 속성의 ID를 생성하는 데 사용할 수 있습니다. 동일한 컴포넌트 내에서 여러 번 호출하면 서로 다른 ID가 생성되며, 동일한 컴포넌트의 여러 인스턴스(instance)가 `useId()`를 호출해도 각각 다른 ID가 생성됩니다.

  `useId()`로 생성된 ID는 서버와 클라이언트 렌더링(rendering) 간에도 안정적으로 유지되므로, SSR 애플리케이션에서 하이드레이션(hydration) 불일치 없이 사용할 수 있습니다.

  동일한 페이지에 여러 Vue 애플리케이션 인스턴스가 있는 경우, [`app.config.idPrefix`](07_composition_and_reactivity_apis.md#api-application-app-config-idprefix)를 통해 각 앱에 ID 접두사를 지정하여 ID 충돌을 방지할 수 있습니다.

  **주의**
  `useId()`는 인스턴스 충돌을 일으킬 수 있으므로 `computed()` 속성 내부에서 호출하지 않아야 합니다. 대신 ID를 `computed()` 바깥에서 선언하고, 계산 함수 내부에서 이를 참조하세요.

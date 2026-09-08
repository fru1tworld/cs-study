# Vue 3 컴포넌트와 고급 API

옵션 API와 컴포넌트 인스턴스, 내장 디렉티브, SFC 문법 및 서버 렌더링 등 나머지 API를 다룹니다. 타입 시그니처와 버전별 조건은 원문에 맞춰 보존했습니다.

## 목차

- [내장 컴포넌트](#api-built-in-components)
- [내장 디렉티브](#api-built-in-directives)
- [내장 특수 속성](#api-built-in-special-attributes)
- [내장 특수 엘리먼트](#api-built-in-special-elements)
- [컴파일 타임 플래그](#api-compile-time-flags)
- [컴포넌트 인스턴스](#api-component-instance)
- [커스텀 엘리먼트 API](#api-custom-elements)
- [커스텀 렌더러 API](#api-custom-renderer)
- [API 참조](#api-index)
- [옵션: Composition](#api-options-composition)
- [옵션: 라이프사이클](#api-options-lifecycle)
- [옵션: 기타](#api-options-misc)
- [옵션: 렌더링](#api-options-rendering)
- [옵션: 상태](#api-options-state)
- [렌더 함수 API](#api-render-function)
- [SFC CSS 기능](#api-sfc-css-features)
- [\<script setup>](#api-sfc-script-setup)
- [SFC 구문 명세](#api-sfc-spec)
- [서버 사이드 렌더링 API](#api-ssr)
- [유틸리티 타입](#api-utility-types)

---

<a id="api-built-in-components"></a>

<a id="api-built-in-components-built-in-components"></a>

## 내장 컴포넌트

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/built-in-components.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/built-in-components.md

**등록 및 사용법**
내장 컴포넌트는 등록 없이 템플릿(template)에서 바로 사용할 수 있습니다. 또한 트리 셰이킹(tree-shaking)이 가능합니다. 즉, 사용된 경우에만 빌드에 포함됩니다.

[렌더 함수](06_reactivity_and_rendering_in_depth.md#guide-extras-render-function)에서 사용할 때는 명시적으로 import 해야 합니다. 예시:

```js
import { h, Transition } from 'vue'

h(Transition, {
  /* props */
})
```



<a id="api-built-in-components-transition"></a>

### `<Transition>`
**하나의** 요소 또는 컴포넌트(component)에 애니메이션 전환 효과를 제공합니다.

- **Props**

  ```ts
  interface TransitionProps {
    /**
     * 전환 CSS 클래스 이름을 자동으로 생성하는 데 사용됩니다.
     * 예: `name: 'fade'`를 지정하면 `.fade-enter`,
     * `.fade-enter-active` 등으로 자동 확장됩니다.
     */
    name?: string
    /**
     * CSS 전환 클래스를 적용할지 여부입니다.
     * 기본값: true
     */
    css?: boolean
    /**
     * 전환 종료 타이밍을 결정하기 위해
     * 대기할 전환 이벤트의 종류를 지정합니다.
     * 기본 동작은 더 긴 지속 시간을 가진
     * 타입을 자동 감지합니다.
     */
    type?: 'transition' | 'animation'
    /**
     * 전환의 명시적 지속 시간을 지정합니다.
     * 기본 동작은 루트 전환 요소에서 첫 번째 `transitionend`
     * 또는 `animationend` 이벤트를 대기합니다.
     */
    duration?: number | { enter: number; leave: number }
    /**
     * 나가기/들어오기 전환의 타이밍 시퀀스를 제어합니다.
     * 기본 동작은 동시에 실행됩니다.
     */
    mode?: 'in-out' | 'out-in' | 'default'
    /**
     * 초기 렌더 시 전환을 적용할지 여부입니다.
     * 기본값: false
     */
    appear?: boolean

    /**
     * 전환 클래스를 커스터마이즈하기 위한 props입니다.
     * 템플릿에서는 케밥 케이스를 사용하세요. 예: enter-from-class="xxx"
     */
    enterFromClass?: string
    enterActiveClass?: string
    enterToClass?: string
    appearFromClass?: string
    appearActiveClass?: string
    appearToClass?: string
    leaveFromClass?: string
    leaveActiveClass?: string
    leaveToClass?: string
  }
  ```

- **이벤트**

  - `@before-enter`
  - `@before-leave`
  - `@enter`
  - `@leave`
  - `@appear`
  - `@after-enter`
  - `@after-leave`
  - `@after-appear`
  - `@enter-cancelled`
  - `@leave-cancelled` (`v-show`에서만)
  - `@appear-cancelled`

- **예시**

  단순 요소:

  ```vue-html
  <Transition>
    <div v-if="ok">토글된 내용</div>
  </Transition>
  ```

  `key` 속성을 변경하여 전환 강제 적용:

  ```vue-html
  <Transition>
    <div :key="text">{{ text }}</div>
  </Transition>
  ```

  동적 컴포넌트, 전환 모드 + appear 시 애니메이션:

  ```vue-html
  <Transition name="fade" mode="out-in" appear>
    <component :is="view"></component>
  </Transition>
  ```

  전환 이벤트 리스닝:

  ```vue-html
  <Transition @after-enter="onTransitionComplete">
    <div v-show="ok">토글된 내용</div>
  </Transition>
  ```

- **더 알아보기** [가이드 - Transition](04_built_ins_and_animation.md#guide-built-ins-transition)

<a id="api-built-in-components-transitiongroup"></a>

### `<TransitionGroup>`
목록 내 **여러** 요소 또는 컴포넌트에 전환 효과를 제공합니다.

- **Props**

  `<TransitionGroup>`은 `mode`를 제외하면 `<Transition>`과 동일한 props를 받으며, 두 가지 props를 추가로 받습니다:

  ```ts
  interface TransitionGroupProps extends Omit<TransitionProps, 'mode'> {
    /**
     * 정의하지 않으면 fragment로 렌더링됩니다.
     */
    tag?: string
    /**
     * 이동 전환 중 적용되는 CSS 클래스를 커스터마이즈합니다.
     * 템플릿에서는 케밥 케이스를 사용하세요. 예: move-class="xxx"
     */
    moveClass?: string
  }
  ```

- **이벤트**

  `<TransitionGroup>`은 `<Transition>`과 동일한 이벤트를 발생시킵니다.

- **상세 설명**

  기본적으로 `<TransitionGroup>`은 래퍼 DOM 요소를 렌더링(rendering)하지 않지만, `tag` prop으로 래퍼 요소를 렌더링하도록 지정할 수 있습니다.

  애니메이션이 제대로 동작하려면 `<transition-group>` 내의 모든 자식에 [**고유한 key**](02_essentials.md#guide-essentials-list-maintaining-state-with-key)가 있어야 합니다.

  `<TransitionGroup>`은 CSS transform을 통한 이동 전환을 지원합니다. 업데이트 후 자식의 화면 위치가 변경되면, 이동 CSS 클래스(`name` 속성에서 자동 생성되거나 `move-class` prop으로 지정됨)가 적용됩니다. 이동 클래스가 적용될 때 CSS `transform` 속성이 "전환 가능"하다면, [FLIP 기법](https://aerotwist.com/blog/flip-your-animations/)을 사용하여 목적지까지 부드럽게 이동하는 애니메이션이 적용됩니다.

- **예시**

  ```vue-html
  <TransitionGroup tag="ul" name="slide">
    <li v-for="item in items" :key="item.id">
      {{ item.text }}
    </li>
  </TransitionGroup>
  ```

- **더 알아보기** [가이드 - TransitionGroup](04_built_ins_and_animation.md#guide-built-ins-transition-group)

<a id="api-built-in-components-keepalive"></a>

### `<KeepAlive>`
내부에 감싸인, 동적으로 토글되는 컴포넌트를 캐시합니다.

- **Props**

  ```ts
  interface KeepAliveProps {
    /**
     * 지정하면, `include`에 일치하는 이름의
     * 컴포넌트만 캐시됩니다.
     */
    include?: MatchPattern
    /**
     * `exclude`에 일치하는 이름의
     * 컴포넌트는 캐시되지 않습니다.
     */
    exclude?: MatchPattern
    /**
     * 캐시할 컴포넌트 인스턴스의 최대 개수입니다.
     */
    max?: number | string
  }

  type MatchPattern = string | RegExp | (string | RegExp)[]
  ```

- **상세 설명**

  동적 컴포넌트를 감쌀 때, `<KeepAlive>`는 비활성 컴포넌트 인스턴스(instance)를 파괴하지 않고 캐시합니다.

  한 번에 `<KeepAlive>`의 직접 자식으로는 하나의 활성 컴포넌트 인스턴스만 존재할 수 있습니다.

  `<KeepAlive>` 내부에서 컴포넌트가 토글될 때, 해당 컴포넌트의 `activated` 및 `deactivated` 라이프사이클(lifecycle) 훅(hook)이 호출됩니다. 이는 `mounted`와 `unmounted`의 대안으로, 이 두 훅은 호출되지 않습니다. 이 동작은 `<KeepAlive>`의 직접 자식뿐만 아니라 모든 하위 컴포넌트에도 적용됩니다.

- **예시**

  기본 사용법:

  ```vue-html
  <KeepAlive>
    <component :is="view"></component>
  </KeepAlive>
  ```

  `v-if` / `v-else` 분기와 함께 사용할 때는 한 번에 하나의 컴포넌트만 렌더링되어야 합니다:

  ```vue-html
  <KeepAlive>
    <comp-a v-if="a > 1"></comp-a>
    <comp-b v-else></comp-b>
  </KeepAlive>
  ```

  `<Transition>`과 함께 사용:

  ```vue-html
  <Transition>
    <KeepAlive>
      <component :is="view"></component>
    </KeepAlive>
  </Transition>
  ```

  `include` / `exclude` 사용:

  ```vue-html
  <!-- 콤마로 구분된 문자열 -->
  <KeepAlive include="a,b">
    <component :is="view"></component>
  </KeepAlive>

  <!-- 정규식 (v-bind 사용) -->
  <KeepAlive :include="/a|b/">
    <component :is="view"></component>
  </KeepAlive>

  <!-- 배열 (v-bind 사용) -->
  <KeepAlive :include="['a', 'b']">
    <component :is="view"></component>
  </KeepAlive>
  ```

  `max`와 함께 사용:

  ```vue-html
  <KeepAlive :max="10">
    <component :is="view"></component>
  </KeepAlive>
  ```

- **더 알아보기** [가이드 - KeepAlive](04_built_ins_and_animation.md#guide-built-ins-keep-alive)

<a id="api-built-in-components-teleport"></a>

### `<Teleport>`
슬롯(slot) 콘텐츠를 DOM의 다른 위치에 렌더링합니다.

- **Props**

  ```ts
  interface TeleportProps {
    /**
     * 필수. 대상 컨테이너를 지정합니다.
     * 선택자 또는 실제 요소가 될 수 있습니다.
     */
    to: string | HTMLElement
    /**
     * `true`이면, 콘텐츠가 대상 컨테이너로 이동하지 않고
     * 원래 위치에 남아 있습니다.
     * 동적으로 변경할 수 있습니다.
     */
    disabled?: boolean
    /**
     * `true`이면, Teleport는
     * 애플리케이션의 다른 부분이 마운트된 후
     * 대상 해석을 지연합니다. (3.5+)
     */
    defer?: boolean
  }
  ```

- **예시**

  대상 컨테이너 지정:

  ```vue-html
  <Teleport to="#some-id" />
  <Teleport to=".some-class" />
  <Teleport to="[data-teleport]" />
  ```

  조건부 비활성화:

  ```vue-html
  <Teleport to="#popup" :disabled="displayVideoInline">
    <video src="./my-movie.mp4">
  </Teleport>
  ```

  대상 해석 지연  (3.5+):

  ```vue-html
  <Teleport defer to="#late-div">...</Teleport>

  <!-- 템플릿의 다른 위치에 -->
  <div id="late-div"></div>
  ```

- **더 알아보기** [가이드 - Teleport](04_built_ins_and_animation.md#guide-built-ins-teleport)

<a id="api-built-in-components-suspense"></a>

### `<Suspense>`  (실험적 기능)
컴포넌트 트리 내에서 중첩된 비동기 의존성을 조율하는 데 사용됩니다.

- **Props**

  ```ts
  interface SuspenseProps {
    timeout?: string | number
    suspensible?: boolean
  }
  ```

- **이벤트**

  - `@resolve`
  - `@pending`
  - `@fallback`

- **상세 설명**

  `<Suspense>`는 `#default` 슬롯과 `#fallback` 슬롯, 두 개의 슬롯을 받습니다. 기본 슬롯을 메모리에서 렌더링하는 동안 fallback 슬롯의 내용을 표시합니다.

  기본 슬롯을 렌더링하는 동안 비동기 의존성([비동기 컴포넌트](03_components_and_reusability.md#guide-components-async) 및 [`async setup()`](04_built_ins_and_animation.md#guide-built-ins-suspense-async-setup)이 있는 컴포넌트)을 만나면, 모든 의존성이 해결될 때까지 기본 슬롯을 표시하지 않습니다.

  Suspense를 `suspensible`로 설정하면, 모든 비동기 의존성이 부모 Suspense에서 처리됩니다. [구현 세부사항](https://github.com/vuejs/core/pull/6736)을 참고하세요.

- **더 알아보기** [가이드 - Suspense](04_built_ins_and_animation.md#guide-built-ins-suspense)

---

<a id="api-built-in-directives"></a>

<a id="api-built-in-directives-built-in-directives"></a>

## 내장 디렉티브

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/built-in-directives.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/built-in-directives.md

<a id="api-built-in-directives-v-text"></a>

### v-text
요소의 텍스트 콘텐츠를 업데이트합니다.

- **기대값:** `string`

- **세부사항**

  `v-text`는 요소의 [textContent](https://developer.mozilla.org/en-US/docs/Web/API/Node/textContent) 속성을 설정하여 동작하므로, 요소 내부의 기존 콘텐츠를 모두 덮어씁니다. `textContent`의 일부만 업데이트해야 한다면 [이중 중괄호(mustache) 보간](02_essentials.md#guide-essentials-template-syntax-text-interpolation)을 대신 사용해야 합니다(예: <span v-pre>`<span>Keep this but update a {{dynamicPortion}}</span>`</span>).

- **예시**

  ```vue-html
  <span v-text="msg"></span>
  <!-- 아래와 동일 -->
  <span>{{msg}}</span>
  ```

- **참고** [템플릿 문법 - 텍스트 보간(interpolation)](02_essentials.md#guide-essentials-template-syntax-text-interpolation)

<a id="api-built-in-directives-v-html"></a>

### v-html
요소의 [innerHTML](https://developer.mozilla.org/en-US/docs/Web/API/Element/innerHTML)을 업데이트합니다.

- **기대값:** `string`

- **세부사항**

  `v-html`의 내용은 일반 HTML로 삽입되며, Vue 템플릿 문법은 처리되지 않습니다. `v-html`을 사용해 템플릿(template)을 조합하려고 한다면, 그 대신 컴포넌트(component)를 사용하는 방향으로 해결 방법을 다시 검토해 보세요.

  **보안 주의**
  웹사이트에서 임의의 HTML을 동적으로 렌더링(rendering)하는 것은 매우 위험할 수 있습니다. 이는 쉽게 [XSS 공격](https://en.wikipedia.org/wiki/Cross-site_scripting)으로 이어질 수 있기 때문입니다. 신뢰할 수 있는 콘텐츠에만 `v-html`을 사용하고, 사용자가 제공한 콘텐츠에는 **절대** 사용하지 마세요.


  [싱글 파일 컴포넌트](01_getting_started_and_tutorial.md#guide-scaling-up-sfc)에서는, `scoped` 스타일이 `v-html` 내부의 콘텐츠에는 적용되지 않습니다. 이는 해당 HTML이 Vue의 템플릿 컴파일러에 의해 처리되지 않기 때문입니다. `v-html` 콘텐츠에 scoped CSS를 적용하려면 [CSS 모듈](08_component_and_advanced_apis.md#api-sfc-css-features-css-modules)을 사용하거나, BEM처럼 수동 스코핑 전략을 적용한 별도의 전역 `<style>` 요소를 사용할 수 있습니다.

- **예시**

  ```vue-html
  <div v-html="html"></div>
  ```

- **참고** [템플릿 문법 - Raw HTML](02_essentials.md#guide-essentials-template-syntax-raw-html)

<a id="api-built-in-directives-v-show"></a>

### v-show
표현식 값의 참/거짓에 따라 요소의 표시 여부를 토글합니다.

- **기대값:** `any`

- **세부사항**

  `v-show`는 인라인 스타일을 통해 `display` CSS 속성을 설정하여 동작하며, 요소가 보일 때 초기 `display` 값을 최대한 존중합니다. 또한 조건이 변경될 때 트랜지션(transition)을 트리거합니다.

- **참고** [조건부 렌더링 - v-show](02_essentials.md#guide-essentials-conditional-v-show)

<a id="api-built-in-directives-v-if"></a>

### v-if
표현식 값의 참/거짓에 따라 요소 또는 템플릿 조각을 조건부로 렌더링합니다.

- **기대값:** `any`

- **세부사항**

  `v-if` 요소가 토글될 때, 해당 요소와 그 안의 디렉티브/컴포넌트는 파괴되고 다시 생성됩니다. 초기 조건이 거짓이라면 내부 콘텐츠는 전혀 렌더링되지 않습니다.

  `<template>`에 사용하여, 텍스트만 포함하거나 여러 요소를 포함하는 조건부 블록을 나타낼 수 있습니다.

  이 디렉티브(directive)는 조건이 변경될 때 트랜지션을 트리거합니다.

  `v-for`와 함께 사용할 때는 `v-if`가 더 높은 우선순위를 가집니다. 이 두 디렉티브를 하나의 요소에 함께 사용하는 것은 권장하지 않습니다. 자세한 내용은 [리스트 렌더링 가이드](02_essentials.md#guide-essentials-list-v-for-with-v-if)를 참고하세요.

- **참고** [조건부 렌더링 - v-if](02_essentials.md#guide-essentials-conditional-v-if)

<a id="api-built-in-directives-v-else"></a>

### v-else
`v-if` 또는 `v-if` / `v-else-if` 체인의 "else 블록"을 나타냅니다.

- **표현식 없음**

- **세부사항**

  - 제한: 이전 형제 요소에 `v-if` 또는 `v-else-if`가 있어야 합니다.

  - `<template>`에 사용하여, 텍스트만 포함하거나 여러 요소를 포함하는 조건부 블록을 나타낼 수 있습니다.

- **예시**

  ```vue-html
  <div v-if="Math.random() > 0.5">
    Now you see me
  </div>
  <div v-else>
    Now you don't
  </div>
  ```

- **참고** [조건부 렌더링 - v-else](02_essentials.md#guide-essentials-conditional-v-else)

<a id="api-built-in-directives-v-else-if"></a>

### v-else-if
`v-if`의 "else if 블록"을 나타냅니다. 체이닝이 가능합니다.

- **기대값:** `any`

- **세부사항**

  - 제한: 이전 형제 요소에 `v-if` 또는 `v-else-if`가 있어야 합니다.

  - `<template>`에 사용하여, 텍스트만 포함하거나 여러 요소를 포함하는 조건부 블록을 나타낼 수 있습니다.

- **예시**

  ```vue-html
  <div v-if="type === 'A'">
    A
  </div>
  <div v-else-if="type === 'B'">
    B
  </div>
  <div v-else-if="type === 'C'">
    C
  </div>
  <div v-else>
    Not A/B/C
  </div>
  ```

- **참고** [조건부 렌더링 - v-else-if](02_essentials.md#guide-essentials-conditional-v-else-if)

<a id="api-built-in-directives-v-for"></a>

### v-for
소스 데이터를 기반으로 요소 또는 템플릿 블록을 여러 번 렌더링합니다.

- **기대값:** `Array | Object | number | string | Iterable`

- **세부사항**

  디렉티브의 값은 현재 반복 중인 요소에 대한 별칭을 제공하기 위해 `alias in expression`이라는 특수 문법을 사용해야 합니다:

  ```vue-html
  <div v-for="item in items">
    {{ item.text }}
  </div>
  ```

  또는, 인덱스(또는 객체에서 사용할 경우 키)에 대한 별칭도 지정할 수 있습니다:

  ```vue-html
  <div v-for="(item, index) in items"></div>
  <div v-for="(value, key) in object"></div>
  <div v-for="(value, name, index) in object"></div>
  ```

  `v-for`는 기본적으로 요소를 이동시키지 않고 제자리에서 패치하려고 시도합니다. 요소의 순서를 강제로 재정렬하려면 `key` 특수 속성으로 정렬 힌트를 제공해야 합니다:

  ```vue-html
  <div v-for="item in items" :key="item.id">
    {{ item.text }}
  </div>
  ```

  `v-for`는 [이터러블 프로토콜](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Iteration_protocols#The_iterable_protocol)을 구현한 값(네이티브 `Map` 및 `Set` 포함)에도 사용할 수 있습니다.

- **참고**
  - [리스트 렌더링](02_essentials.md#guide-essentials-list)

<a id="api-built-in-directives-v-on"></a>

### v-on
요소에 이벤트 리스너(listener)를 연결합니다.

- **축약형:** `@`

- **기대값:** `Function | 인라인 문장 | Object (인자 없이)`

- **인자:** `event` (Object 문법 사용 시 선택)

- **수식어**

  - `.stop` - `event.stopPropagation()` 호출
  - `.prevent` - `event.preventDefault()` 호출
  - `.capture` - 캡처 모드로 이벤트 리스너 추가
  - `.self` - 이벤트가 이 요소에서 발생한 경우에만 핸들러 트리거
  - `.{keyAlias}` - 특정 키에서만 핸들러 트리거
  - `.once` - 핸들러를 최대 한 번만 트리거
  - `.left` - 마우스 왼쪽 버튼 이벤트에서만 핸들러 트리거
  - `.right` - 마우스 오른쪽 버튼 이벤트에서만 핸들러 트리거
  - `.middle` - 마우스 가운데 버튼 이벤트에서만 핸들러 트리거
  - `.passive` - `{ passive: true }`로 DOM 이벤트 연결

- **세부사항**

  이벤트 타입은 인자로 표시됩니다. 표현식은 메서드 이름이나 인라인 문장일 수 있으며, 수식어(modifiers)가 있으면 생략할 수도 있습니다.

  일반 요소에 사용하면 [**네이티브 DOM 이벤트**](https://developer.mozilla.org/en-US/docs/Web/Events)만 리스닝합니다. 커스텀 엘리먼트 컴포넌트에 사용하면 해당 자식 컴포넌트에서 발생한 **커스텀 이벤트**를 리스닝합니다.

  네이티브 DOM 이벤트를 리스닝할 때, 메서드는 네이티브 이벤트를 유일한 인자로 받습니다. 인라인 문장을 사용할 경우, 특별한 `$event` 속성에 접근할 수 있습니다: `v-on:click="handle('ok', $event)"`.

  `v-on`은 인자 없이 이벤트/리스너 쌍의 객체에 바인딩(binding)하는 것도 지원합니다. 객체 문법을 사용할 때는 수식어를 지원하지 않습니다.

- **예시**

  ```vue-html
  <!-- 메서드 핸들러 -->
  <button v-on:click="doThis"></button>

  <!-- 동적 이벤트 -->
  <button v-on:[event]="doThis"></button>

  <!-- 인라인 문장 -->
  <button v-on:click="doThat('hello', $event)"></button>

  <!-- 축약형 -->
  <button @click="doThis"></button>

  <!-- 축약형 동적 이벤트 -->
  <button @[event]="doThis"></button>

  <!-- 이벤트 전파 중지 -->
  <button @click.stop="doThis"></button>

  <!-- 기본 동작 방지 -->
  <button @click.prevent="doThis"></button>

  <!-- 표현식 없이 기본 동작 방지 -->
  <form @submit.prevent></form>

  <!-- 수식어 체이닝 -->
  <button @click.stop.prevent="doThis"></button>

  <!-- keyAlias를 사용한 키 수식어 -->
  <input @keyup.enter="onEnter" />

  <!-- 클릭 이벤트가 최대 한 번만 트리거됨 -->
  <button v-on:click.once="doThis"></button>

  <!-- 객체 문법 -->
  <button v-on="{ mousedown: doThis, mouseup: doThat }"></button>
  ```

  자식 컴포넌트에서 커스텀 이벤트를 리스닝(자식에서 "my-event"가 emit될 때 핸들러가 호출됨):

  ```vue-html
  <MyComponent @my-event="handleThis" />

  <!-- 인라인 문장 -->
  <MyComponent @my-event="handleThis(123, $event)" />
  ```

- **참고**
  - [이벤트 핸들링](02_essentials.md#guide-essentials-event-handling)
  - [컴포넌트 - 커스텀 이벤트](02_essentials.md#guide-essentials-component-basics-listening-to-events)

<a id="api-built-in-directives-v-bind"></a>

### v-bind
하나 이상의 속성 또는 컴포넌트 prop을 표현식에 동적으로 바인딩합니다.

- **축약형:**
  - `:` 또는 `.` (`.prop` 수식어 사용 시)
  - 값 생략(속성과 바인딩 값의 이름이 같을 때, 3.4+ 필요)

- **기대값:** `any (인자 사용 시) | Object (인자 없이)`

- **인자:** `attrOrProp (선택)`

- **수식어**

  - `.camel` - 케밥 케이스 속성명을 camelCase로 변환
  - `.prop` - 바인딩을 DOM 속성(property)으로 강제 설정 (3.2+)
  - `.attr` - 바인딩을 DOM 속성(attribute)으로 강제 설정 (3.2+)

- **사용법**

  `class` 또는 `style` 속성에 바인딩할 때, `v-bind`는 Array 또는 Object와 같은 추가 값 타입을 지원합니다. 자세한 내용은 아래 가이드 섹션을 참고하세요.

  요소에 바인딩을 설정할 때, Vue는 기본적으로 `in` 연산자 체크를 통해 해당 키가 속성(property)으로 정의되어 있는지 확인합니다. 속성이 정의되어 있으면, Vue는 값을 HTML 속성(attribute)이 아닌 DOM 속성(property)으로 설정합니다. 대부분의 경우 이 방식이 잘 동작하지만, `.prop` 또는 `.attr` 수식어를 명시적으로 사용하여 이 동작을 오버라이드할 수 있습니다. 특히 [커스텀 엘리먼트 작업 시](06_reactivity_and_rendering_in_depth.md#guide-extras-web-components-passing-dom-properties) 필요할 수 있습니다.

  컴포넌트 prop 바인딩에 사용할 때, prop은 자식 컴포넌트에서 올바르게 선언되어 있어야 합니다.

  인자 없이 사용할 경우, 속성명-값 쌍을 포함하는 객체를 바인딩할 수 있습니다.

- **예시**

  ```vue-html
  <!-- 속성 바인딩 -->
  <img v-bind:src="imageSrc" />

  <!-- 동적 속성명 -->
  <button v-bind:[key]="value"></button>

  <!-- 축약형 -->
  <img :src="imageSrc" />

  <!-- 동일 이름 축약형 (3.4+), :src="src"로 확장됨 -->
  <img :src />

  <!-- 축약형 동적 속성명 -->
  <button :[key]="value"></button>

  <!-- 인라인 문자열 연결 -->
  <img :src="'/path/to/images/' + fileName" />

  <!-- 클래스 바인딩 -->
  <div :class="{ red: isRed }"></div>
  <div :class="[classA, classB]"></div>
  <div :class="[classA, { classB: isB, classC: isC }]"></div>

  <!-- 스타일 바인딩 -->
  <div :style="{ fontSize: size + 'px' }"></div>
  <div :style="[styleObjectA, styleObjectB]"></div>

  <!-- 속성 객체 바인딩 -->
  <div v-bind="{ id: someProp, 'other-attr': otherProp }"></div>

  <!-- prop 바인딩. "prop"은 자식 컴포넌트에서 선언되어야 함 -->
  <MyComponent :prop="someThing" />

  <!-- 부모와 자식 컴포넌트에서 공통된 prop 전달 -->
  <MyComponent v-bind="$props" />

  <!-- XLink -->
  <svg><a :xlink:special="foo"></a></svg>
  ```

  `.prop` 수식어에는 전용 축약형 `.`도 있습니다:

  ```vue-html
  <div :someProperty.prop="someObject"></div>

  <!-- 아래와 동일 -->
  <div .someProperty="someObject"></div>
  ```

  `.camel` 수식어는 in-DOM 템플릿에서 `v-bind` 속성명을 camelCase로 변환할 수 있습니다. 예: SVG `viewBox` 속성

  ```vue-html
  <svg :view-box.camel="viewBox"></svg>
  ```

  `.camel`은 문자열 템플릿을 사용하거나, 빌드 단계에서 템플릿을 미리 컴파일하는 경우 필요하지 않습니다.

- **참고**
  - [클래스 및 스타일 바인딩](02_essentials.md#guide-essentials-class-and-style)
  - [컴포넌트 - Prop 전달 세부사항](03_components_and_reusability.md#guide-components-props-prop-passing-details)

<a id="api-built-in-directives-v-model"></a>

### v-model
폼 입력 요소 또는 컴포넌트에서 양방향 바인딩을 생성합니다.

- **기대값:** 폼 입력 요소의 값 또는 컴포넌트의 출력에 따라 다름

- **제한:**

  - `<input>`
  - `<select>`
  - `<textarea>`
  - 컴포넌트

- **수식어**

  - [`.lazy`](02_essentials.md#guide-essentials-forms-lazy) - `input` 대신 `change` 이벤트 리스닝
  - [`.number`](02_essentials.md#guide-essentials-forms-number) - 유효한 입력 문자열을 숫자로 변환
  - [`.trim`](02_essentials.md#guide-essentials-forms-trim) - 입력값 트림

- **참고**

  - [폼 입력 바인딩](02_essentials.md#guide-essentials-forms)
  - [컴포넌트 이벤트 - `v-model`과 함께 사용](03_components_and_reusability.md#guide-components-v-model)

<a id="api-built-in-directives-v-slot"></a>

### v-slot
props를 받을 것으로 예상되는 명명된 슬롯(named slots) 또는 스코프 슬롯(scoped slots)을 나타냅니다.

- **축약형:** `#`

- **기대값:** 함수 인자 위치에서 유효한 JavaScript 표현식(구조 분해 지원 포함). 선택 사항 - 슬롯에 props가 전달될 것으로 예상될 때만 필요.

- **인자:** 슬롯 이름(선택, 기본값은 `default`)

- **제한:**

  - `<template>`
  - [컴포넌트](03_components_and_reusability.md#guide-components-slots-scoped-slots) (props가 있는 단일 기본 슬롯의 경우)

- **예시**

  ```vue-html
  <!-- 명명된 슬롯 -->
  <BaseLayout>
    <template v-slot:header>
      Header content
    </template>

    <template v-slot:default>
      Default slot content
    </template>

    <template v-slot:footer>
      Footer content
    </template>
  </BaseLayout>

  <!-- props를 받는 명명된 슬롯 -->
  <InfiniteScroll>
    <template v-slot:item="slotProps">
      <div class="item">
        {{ slotProps.item.text }}
      </div>
    </template>
  </InfiniteScroll>

  <!-- 구조 분해와 함께 props를 받는 기본 슬롯 -->
  <Mouse v-slot="{ x, y }">
    Mouse position: {{ x }}, {{ y }}
  </Mouse>
  ```

- **참고**
  - [컴포넌트 - 슬롯](03_components_and_reusability.md#guide-components-slots)

<a id="api-built-in-directives-v-pre"></a>

### v-pre
이 요소와 모든 자식에 대한 컴파일을 건너뜁니다.

- **표현식 없음**

- **세부사항**

  `v-pre`가 있는 요소 내부에서는 모든 Vue 템플릿 문법이 그대로 보존되어 렌더링됩니다. 가장 일반적인 사용 사례는 원시 이중 중괄호 태그를 표시하는 것입니다.

- **예시**

  ```vue-html
  <span v-pre>{{ this will not be compiled }}</span>
  ```

<a id="api-built-in-directives-v-once"></a>

### v-once
요소와 컴포넌트를 한 번만 렌더링하고, 이후 업데이트를 건너뜁니다.

- **표현식 없음**

- **세부사항**

  이후 다시 렌더링될 때, 해당 요소/컴포넌트와 모든 자식은 정적 콘텐츠로 간주되어 건너뛰게 됩니다. 이는 업데이트 성능을 최적화하는 데 사용할 수 있습니다.

  ```vue-html
  <!-- 단일 요소 -->
  <span v-once>This will never change: {{msg}}</span>
  <!-- 자식이 있는 요소 -->
  <div v-once>
    <h1>Comment</h1>
    <p>{{msg}}</p>
  </div>
  <!-- 컴포넌트 -->
  <MyComponent v-once :comment="msg"></MyComponent>
  <!-- `v-for` 디렉티브 -->
  <ul>
    <li v-for="i in list" v-once>{{i}}</li>
  </ul>
  ```

  3.2부터는 [`v-memo`](08_component_and_advanced_apis.md#api-built-in-directives-v-memo)를 사용해 무효화 조건과 함께 템플릿의 일부를 메모이즈할 수도 있습니다.

- **참고**
  - [데이터 바인딩 문법 - 보간](02_essentials.md#guide-essentials-template-syntax-text-interpolation)
  - [v-memo](08_component_and_advanced_apis.md#api-built-in-directives-v-memo)

<a id="api-built-in-directives-v-memo"></a>

### v-memo
- 3.2+에서만 지원

- **기대값:** `any[]`

- **세부사항**

  템플릿의 서브 트리를 메모이즈합니다. 요소와 컴포넌트 모두에 사용할 수 있습니다. 디렉티브는 메모이제이션(memoization)을 위해 비교할 고정 길이의 의존성 값 배열을 받습니다. 배열의 모든 값이 마지막 렌더와 동일하다면, 전체 서브 트리에 대한 업데이트를 건너뜁니다. 예를 들어:

  ```vue-html
  <div v-memo="[valueA, valueB]">
    ...
  </div>
  ```

  컴포넌트가 다시 렌더링될 때, `valueA`와 `valueB`가 모두 동일하다면 이 `<div>`와 그 자식에 대한 모든 업데이트를 건너뜁니다. 실제로, 메모이즈된 서브 트리의 복사본을 재사용할 수 있으므로 Virtual DOM VNode 생성조차도 건너뜁니다.

  메모이제이션 배열을 올바르게 지정하는 것이 중요합니다. 그렇지 않으면 실제로 적용되어야 할 업데이트를 건너뛸 수 있습니다. 의존성 배열이 비어있는 `v-memo="[]"`는 기능적으로 `v-once`와 동일합니다.

  **`v-for`와 함께 사용하기**

  `v-memo`는 성능이 중요한 시나리오에서 마이크로 최적화를 위해 제공되며, 거의 필요하지 않습니다. 가장 일반적인 활용 사례는 대용량 `v-for` 리스트(길이 > 1000)를 렌더링할 때입니다:

  ```vue-html
  <div v-for="item in list" :key="item.id" v-memo="[item.id === selected]">
    <p>ID: {{ item.id }} - selected: {{ item.id === selected }}</p>
    <p>...more child nodes</p>
  </div>
  ```

  컴포넌트의 `selected` 상태가 변경될 때, 대부분의 항목이 동일하더라도 많은 VNode가 생성됩니다. 여기서 `v-memo` 사용은 "이 항목이 선택됨/해제됨으로 변경된 경우에만 업데이트하라"는 의미입니다. 영향을 받지 않은 항목은 이전 VNode를 재사용하여 diffing을 완전히 건너뜁니다. 이때, Vue가 항목의 `:key`에서 이를 자동으로 추론하므로 memo 의존성 배열에 `item.id`를 포함할 필요는 없습니다.

  **주의**
  `v-memo`를 `v-for`와 함께 사용할 때는 반드시 같은 요소에 사용해야 합니다. **`v-memo`는 `v-for` 내부에서는 동작하지 않습니다.**


  `v-memo`는 자식 컴포넌트 업데이트 체크가 비최적화된 특정 엣지 케이스에서 원치 않는 업데이트를 수동으로 방지하는 데도 사용할 수 있습니다. 하지만, 필요한 업데이트가 건너뛰어지지 않도록 올바른 의존성 배열을 지정하는 것은 개발자의 책임입니다.

- **참고**
  - [v-once](08_component_and_advanced_apis.md#api-built-in-directives-v-once)

<a id="api-built-in-directives-v-cloak"></a>

### v-cloak
컴파일되지 않은 템플릿이 준비되기 전까지 화면에서 숨기는 데 사용됩니다.

- **표현식 없음**

- **세부사항**

  **이 디렉티브는 빌드 단계가 없는 환경에서만 필요합니다.**

  in-DOM 템플릿을 사용할 때, "컴파일되지 않은 템플릿의 깜빡임"이 발생할 수 있습니다. 즉, 마운트(mount)된 컴포넌트가 렌더링된 콘텐츠로 교체되기 전까지 사용자가 원시 이중 중괄호 태그를 볼 수 있습니다.

  `v-cloak`는 관련 컴포넌트 인스턴스(instance)가 마운트될 때까지 요소에 남아 있습니다. `[v-cloak] { display: none }`과 같은 CSS 규칙과 결합하여, 컴포넌트가 준비될 때까지 원시 템플릿을 숨기는 데 사용할 수 있습니다.

- **예시**

  ```css
  [v-cloak] {
    display: none;
  }
  ```

  ```vue-html
  <div v-cloak>
    {{ message }}
  </div>
  ```

  컴파일이 완료될 때까지 `<div>`는 보이지 않습니다.

---

<a id="api-built-in-special-attributes"></a>

<a id="api-built-in-special-attributes-built-in-special-attributes"></a>

## 내장 특수 속성

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/built-in-special-attributes.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/built-in-special-attributes.md

<a id="api-built-in-special-attributes-key"></a>

### key
`key` 특수 속성은 주로 Vue의 가상 DOM 알고리즘이 새로운 노드 목록과 이전 노드 목록을 비교(diffing)할 때 vnode를 식별하는 힌트로 사용됩니다.

- **기대값:** `number | string | symbol`

- **세부사항**

  key가 없으면, Vue는 요소의 이동을 최소화하고 같은 타입의 요소를 최대한 제자리에서 패치/재사용하려는 알고리즘을 사용합니다. key가 있으면, key의 순서 변경에 따라 요소가 재정렬되고, 더 이상 존재하지 않는 key를 가진 요소는 항상 제거/파괴됩니다.

  같은 부모를 둔 자식들은 **고유한 key**를 가져야 합니다. 중복된 key는 렌더링(rendering) 오류를 일으킵니다.

  가장 일반적인 사용 사례는 `v-for`와 결합하는 것입니다:

  ```vue-html
  <ul>
    <li v-for="item in items" :key="item.id">...</li>
  </ul>
  ```

  또한 요소/컴포넌트(component)를 재사용하는 대신 교체하도록 강제할 때도 사용할 수 있습니다. 이는 다음과 같은 경우에 유용할 수 있습니다:

  - 컴포넌트의 라이프사이클(lifecycle) 훅(hook)을 제대로 트리거하고 싶을 때
  - 트랜지션(transition)을 트리거하고 싶을 때

  예를 들어:

  ```vue-html
  <transition>
    <span :key="text">{{ text }}</span>
  </transition>
  ```

  `text`가 변경되면, `<span>`은 패치되는 대신 항상 교체되므로 트랜지션이 트리거됩니다.

- **관련 문서** [가이드 - 리스트 렌더링 - `key`로 상태 유지하기](02_essentials.md#guide-essentials-list-maintaining-state-with-key)

<a id="api-built-in-special-attributes-ref"></a>

### ref
[템플릿(template) ref](02_essentials.md#guide-essentials-template-refs)를 나타냅니다.

- **기대값:** `string | Function`

- **세부사항**

  `ref`는 요소나 자식 컴포넌트에 대한 참조를 등록하는 데 사용됩니다.

  옵션 API에서는 참조가 컴포넌트의 `this.$refs` 객체에 등록됩니다:

  ```vue-html
  <!-- this.$refs.p로 저장됨 -->
  <p ref="p">hello</p>
  ```

  컴포지션 API에서는 참조가 동일한 이름의 ref에 저장됩니다:

  ```vue
  <script setup>
  import { useTemplateRef } from 'vue'

  const pRef = useTemplateRef('p')
  </script>

  <template>
    <p ref="p">hello</p>
  </template>
  ```

  일반 DOM 요소에 사용하면 참조는 해당 요소가 되고, 자식 컴포넌트에 사용하면 참조는 자식 컴포넌트 인스턴스(instance)가 됩니다.

  또는 `ref`에 함수 값을 전달하여, 참조를 어디에 저장할지 완전히 제어할 수도 있습니다:

  ```vue-html
  <ChildComponent :ref="(el) => child = el" />
  ```

  ref 등록 타이밍에 대한 중요한 참고 사항: ref 자체는 렌더 함수의 결과로 생성되기 때문에, 컴포넌트가 마운트(mount)될 때까지 참조에 접근해서는 안 됩니다.

  `this.$refs`는 반응형이 아니므로, 데이터 바인딩(binding)을 위해 템플릿에서 사용하려고 해서는 안 됩니다.

- **관련 문서**
  - [가이드 - 템플릿 ref](02_essentials.md#guide-essentials-template-refs)
  - [가이드 - 템플릿 ref 타입 지정](05_scaling_typescript_and_best_practices.md#guide-typescript-composition-api-typing-template-refs)  (TypeScript)
  - [가이드 - 컴포넌트 템플릿 ref 타입 지정](05_scaling_typescript_and_best_practices.md#guide-typescript-composition-api-typing-component-template-refs)  (TypeScript)

<a id="api-built-in-special-attributes-is"></a>

### is
[동적 컴포넌트](02_essentials.md#guide-essentials-component-basics-dynamic-components) 바인딩에 사용됩니다.

- **기대값:** `string | Component`

- **네이티브 요소에서의 사용**
 
  - 3.1+에서만 지원됩니다

  `is` 속성이 네이티브 HTML 요소에 사용되면, [커스터마이즈드 내장 요소](https://html.spec.whatwg.org/multipage/custom-elements.html#custom-elements-customized-builtin-example)로 해석되며, 이는 웹 플랫폼의 네이티브 기능입니다.

  하지만 [in-DOM 템플릿 파싱 주의사항](02_essentials.md#guide-essentials-component-basics-in-dom-template-parsing-caveats)에서 설명한 것처럼, 네이티브 요소를 Vue 컴포넌트로 대체할 필요가 있을 수 있습니다. 이 경우 `is` 속성 값 앞에 `vue:`를 붙이면 Vue가 해당 요소를 Vue 컴포넌트로 렌더링합니다:

  ```vue-html
  <table>
    <tr is="vue:my-row-component"></tr>
  </table>
  ```

- **관련 문서**

  - [내장 특수 요소 - `<component>`](08_component_and_advanced_apis.md#api-built-in-special-elements-component)
  - [동적 컴포넌트](02_essentials.md#guide-essentials-component-basics-dynamic-components)

---

<a id="api-built-in-special-elements"></a>

<a id="api-built-in-special-elements-built-in-special-elements"></a>

## 내장 특수 엘리먼트

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/built-in-special-elements.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/built-in-special-elements.md

**컴포넌트가 아님**
`<component>`, `<slot>`, `<template>`는 컴포넌트(component)와 유사한 기능을 하며 템플릿(template) 문법의 일부입니다. 이들은 실제 컴포넌트가 아니며 템플릿 컴파일 시 사라집니다. 따라서 템플릿에서는 관례적으로 소문자로 작성합니다.


<a id="api-built-in-special-elements-component"></a>

### `<component>`
동적 컴포넌트 또는 엘리먼트를 렌더링(rendering)하기 위한 "메타 컴포넌트"입니다.

- **Props**

  ```ts
  interface DynamicComponentProps {
    is: string | Component
  }
  ```

- **상세 설명**

  실제로 렌더링할 컴포넌트는 `is` prop에 따라 결정됩니다.

  - `is`가 문자열일 경우, HTML 태그 이름이거나 등록된 컴포넌트의 이름일 수 있습니다.

  - 또는, `is`에 컴포넌트의 정의 자체를 직접 바인딩(binding)할 수도 있습니다.

- **예시**

  등록된 이름으로 컴포넌트 렌더링하기 (옵션 API):

  ```vue
  <script>
  import Foo from './Foo.vue'
  import Bar from './Bar.vue'

  export default {
    components: { Foo, Bar },
    data() {
      return {
        view: 'Foo'
      }
    }
  }
  </script>

  <template>
    <component :is="view" />
  </template>
  ```

  컴포넌트 정의로 렌더링하기 (`<script setup>`을 사용하는 컴포지션 API):

  ```vue
  <script setup>
  import Foo from './Foo.vue'
  import Bar from './Bar.vue'
  </script>

  <template>
    <component :is="Math.random() > 0.5 ? Foo : Bar" />
  </template>
  ```

  HTML 엘리먼트 렌더링하기:

  ```vue-html
  <component :is="href ? 'a' : 'span'"></component>
  ```

  [내장 컴포넌트](08_component_and_advanced_apis.md#api-built-in-components)도 모두 `is`에 전달할 수 있지만, 이름으로 전달하려면 반드시 등록해야 합니다. 예를 들어:

  ```vue
  <script>
  import { Transition, TransitionGroup } from 'vue'

  export default {
    components: {
      Transition,
      TransitionGroup
    }
  }
  </script>

  <template>
    <component :is="isGroup ? 'TransitionGroup' : 'Transition'">
      ...
    </component>
  </template>
  ```

  컴포넌트 자체를 `is`에 전달하는 경우(예: `<script setup>`에서)는 등록이 필요하지 않습니다.

  `<component>` 태그에 `v-model`을 사용할 경우, 템플릿 컴파일러는 이를 `modelValue` prop과 `update:modelValue` 이벤트 리스너(listener)로 확장합니다. 이는 다른 컴포넌트와 마찬가지입니다. 하지만 이 동작은 `<input>`이나 `<select>`와 같은 네이티브 HTML 엘리먼트와는 호환되지 않습니다. 따라서 동적으로 생성된 네이티브 엘리먼트에 `v-model`을 사용하는 것은 동작하지 않습니다:

  ```vue
  <script setup>
  import { ref } from 'vue'

  const tag = ref('input')
  const username = ref('')
  </script>

  <template>
    <!-- 'input'이 네이티브 HTML 엘리먼트이므로 동작하지 않습니다 -->
    <component :is="tag" v-model="username" />
  </template>
  ```

  이와 같은 예외적인 경우는 드물며, 실제 애플리케이션에서 네이티브 폼 필드는 보통 컴포넌트로 감싸서 사용합니다. 만약 네이티브 엘리먼트를 직접 사용해야 한다면, `v-model`을 속성과 이벤트로 수동 분리하여 사용할 수 있습니다.

- **관련 문서** [동적 컴포넌트](02_essentials.md#guide-essentials-component-basics-dynamic-components)

<a id="api-built-in-special-elements-slot"></a>

### `<slot>`
템플릿에서 슬롯(slot) 콘텐츠의 출력 위치를 나타냅니다.

- **Props**

  ```ts
  interface SlotProps {
    /**
     * <slot>에 전달된 모든 prop은
     * 스코프 슬롯의 인자로 전달됩니다
     */
    [key: string]: any
    /**
     * 슬롯 이름을 지정할 때 사용됩니다.
     */
    name?: string
  }
  ```

- **상세 설명**

  `<slot>` 엘리먼트는 `name` 속성을 사용하여 슬롯 이름을 지정할 수 있습니다. `name`이 지정되지 않으면 기본 슬롯이 렌더링됩니다. 슬롯 엘리먼트에 지정한 추가 속성들은 부모에서 정의된 스코프 슬롯(scoped slots)에 슬롯 prop으로 전달됩니다.

  해당 엘리먼트 자체는 일치하는 슬롯 콘텐츠로 대체됩니다.

  Vue 템플릿의 `<slot>` 엘리먼트는 JavaScript로 컴파일되므로, [네이티브 `<slot>` 엘리먼트](https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot)와 혼동하지 마세요.

- **관련 문서** [컴포넌트 - 슬롯](03_components_and_reusability.md#guide-components-slots)

<a id="api-built-in-special-elements-template"></a>

### `<template>`
`<template>` 태그는 DOM에 엘리먼트를 렌더링하지 않으면서 내장 디렉티브를 사용하고 싶을 때 플레이스홀더 역할을 합니다.

- **상세 설명**

  `<template>`에 대한 특별한 처리는 다음 디렉티브(directive) 중 하나와 함께 사용될 때만 적용됩니다:

  - `v-if`, `v-else-if`, 또는 `v-else`
  - `v-for`
  - `v-slot`

  이들 디렉티브가 없으면 [네이티브 `<template>` 엘리먼트](https://developer.mozilla.org/en-US/docs/Web/HTML/Element/template)로 렌더링됩니다.

  `v-for`가 있는 `<template>`에는 [`key` 속성](08_component_and_advanced_apis.md#api-built-in-special-attributes-key)을 사용할 수 있습니다. 그 외의 모든 속성과 디렉티브는, 대응하는 엘리먼트가 없으면 의미가 없으므로 무시됩니다.

  싱글 파일 컴포넌트는 전체 템플릿을 감싸기 위해 [최상위 `<template>` 태그](08_component_and_advanced_apis.md#api-sfc-spec-language-blocks)를 사용합니다. 이 사용법은 위에서 설명한 `<template>`의 사용과는 별개입니다. 최상위 태그는 템플릿 자체의 일부가 아니며, 디렉티브와 같은 템플릿 문법을 지원하지 않습니다.

- **관련 문서**
  - [가이드 - `<template>`에서의 `v-if`](02_essentials.md#guide-essentials-conditional-v-if-on-template)
  - [가이드 - `<template>`에서의 `v-for`](02_essentials.md#guide-essentials-list-v-for-on-template)
  - [가이드 - 명명된 슬롯](03_components_and_reusability.md#guide-components-slots-named-slots)

---

<a id="api-compile-time-flags"></a>

<a id="api-compile-time-flags-compile-time-flags"></a>

## 컴파일 타임 플래그

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/compile-time-flags.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/compile-time-flags.md

**참고**
컴파일 타임 플래그는 `esm-bundler` 빌드의 Vue(즉, `vue/dist/vue.esm-bundler.js`)를 사용할 때만 적용됩니다.


Vue를 빌드 단계와 함께 사용할 때, 여러 컴파일 타임 플래그를 설정하여 특정 기능을 활성화/비활성화할 수 있습니다. 컴파일 타임 플래그를 사용하면 비활성화된 기능이 트리 셰이킹(tree-shaking)을 통해 최종 번들에서 제거될 수 있다는 이점이 있습니다.

이 플래그들이 명시적으로 설정되지 않아도 Vue는 정상적으로 동작합니다. 그러나 가능한 경우 관련 기능이 제대로 제거될 수 있도록, 항상 플래그를 설정하는 것이 권장됩니다.

빌드 도구에 따라 플래그를 설정하는 방법은 [설정 가이드](08_component_and_advanced_apis.md#api-compile-time-flags-configuration-guides)를 참고하세요.

<a id="api-compile-time-flags-VUE_OPTIONS_API"></a>

### `__VUE_OPTIONS_API__`
- **기본값:** `true`

  옵션 API 지원을 활성화/비활성화합니다. 비활성화하면 번들 크기가 더 작아지지만, 옵션 API에 의존하는 서드파티 라이브러리와의 호환성에 영향을 줄 수 있습니다.

<a id="api-compile-time-flags-VUE_PROD_DEVTOOLS"></a>

### `__VUE_PROD_DEVTOOLS__`
- **기본값:** `false`

  프로덕션 빌드에서 devtools 지원을 활성화/비활성화합니다. 활성화하면 번들에 더 많은 코드가 포함되므로, 디버깅 목적일 때만 활성화하는 것이 권장됩니다.

<a id="api-compile-time-flags-VUE_PROD_HYDRATION_MISMATCH_DETAILS"></a>

### `__VUE_PROD_HYDRATION_MISMATCH_DETAILS__`
- **기본값:** `false`

  프로덕션 빌드에서 hydration 불일치에 대한 상세 경고를 활성화/비활성화합니다. 활성화하면 번들에 더 많은 코드가 포함되므로, 디버깅 목적일 때만 활성화하는 것이 권장됩니다.

- 3.4+ 버전에서만 사용 가능합니다.

<a id="api-compile-time-flags-configuration-guides"></a>

### 설정 가이드
<a id="api-compile-time-flags-vite"></a>

#### Vite
`@vitejs/plugin-vue`는 이 플래그들에 대한 기본값을 자동으로 제공합니다. 기본값을 변경하려면 Vite의 [`define` 설정 옵션](https://vite.dev/config/shared-options.html#define)을 사용하세요:

```js [vite.config.js]
import { defineConfig } from 'vite'

export default defineConfig({
  define: {
    // 프로덕션 빌드에서 hydration 불일치 상세 정보 활성화
    __VUE_PROD_HYDRATION_MISMATCH_DETAILS__: 'true'
  }
})
```

<a id="api-compile-time-flags-vue-cli"></a>

#### vue-cli
`@vue/cli-service`는 이 플래그들 중 일부에 대한 기본값을 자동으로 제공합니다. 값을 설정/변경하려면 다음과 같이 하세요:

```js [vue.config.js]
module.exports = {
  chainWebpack: (config) => {
    config.plugin('define').tap((definitions) => {
      Object.assign(definitions[0], {
        __VUE_OPTIONS_API__: 'true',
        __VUE_PROD_DEVTOOLS__: 'false',
        __VUE_PROD_HYDRATION_MISMATCH_DETAILS__: 'false'
      })
      return definitions
    })
  }
}
```

<a id="api-compile-time-flags-webpack"></a>

#### webpack
플래그는 webpack의 [DefinePlugin](https://webpack.js.org/plugins/define-plugin/)을 사용하여 정의해야 합니다:

```js [webpack.config.js]
module.exports = {
  // ...
  plugins: [
    new webpack.DefinePlugin({
      __VUE_OPTIONS_API__: 'true',
      __VUE_PROD_DEVTOOLS__: 'false',
      __VUE_PROD_HYDRATION_MISMATCH_DETAILS__: 'false'
    })
  ]
}
```

<a id="api-compile-time-flags-rollup"></a>

#### Rollup
플래그는 [@rollup/plugin-replace](https://github.com/rollup/plugins/tree/master/packages/replace)를 사용하여 정의해야 합니다:

```js [rollup.config.js]
import replace from '@rollup/plugin-replace'

export default {
  plugins: [
    replace({
      __VUE_OPTIONS_API__: 'true',
      __VUE_PROD_DEVTOOLS__: 'false',
      __VUE_PROD_HYDRATION_MISMATCH_DETAILS__: 'false'
    })
  ]
}
```

---

<a id="api-component-instance"></a>

<a id="api-component-instance-component-instance"></a>

## 컴포넌트 인스턴스

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/component-instance.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/component-instance.md

**안내**
이 페이지는 컴포넌트 공개 인스턴스(instance), 즉 `this`에 노출되는 내장 속성과 메서드를 설명합니다.

이 페이지에 나열된 모든 속성은 읽기 전용입니다(`$data`의 중첩 속성 제외).


<a id="api-component-instance-data"></a>

### $data
[`data`](08_component_and_advanced_apis.md#api-options-state-data) 옵션에서 반환된 객체로, 컴포넌트(component)가 이를 반응형으로 만듭니다. 컴포넌트 인스턴스는 데이터 객체 속성에 대한 접근을 프록시(proxy)합니다.

- **타입**

  ```ts
  interface ComponentPublicInstance {
    $data: object
  }
  ```

<a id="api-component-instance-props"></a>

### $props
컴포넌트의 현재 해석된 props를 나타내는 객체입니다.

- **타입**

  ```ts
  interface ComponentPublicInstance {
    $props: object
  }
  ```

- **세부사항**

  [`props`](08_component_and_advanced_apis.md#api-options-state-props) 옵션을 통해 선언된 props만 포함됩니다. 컴포넌트 인스턴스는 props 객체 속성에 대한 접근을 프록시합니다.

<a id="api-component-instance-el"></a>

### $el
컴포넌트 인스턴스가 관리하는 루트 DOM 노드입니다.

- **타입**

  ```ts
  interface ComponentPublicInstance {
    $el: any
  }
  ```

- **세부사항**

  컴포넌트가 [마운트(mount)](08_component_and_advanced_apis.md#api-options-lifecycle-mounted)되기 전까지 `$el`은 `undefined`입니다.

  - 단일 루트 엘리먼트를 가진 컴포넌트의 경우, `$el`은 해당 엘리먼트를 가리킵니다.
  - 텍스트 루트를 가진 컴포넌트의 경우, `$el`은 텍스트 노드를 가리킵니다.
  - 여러 루트 노드를 가진 컴포넌트의 경우, `$el`은 Vue가 DOM 내 컴포넌트의 위치를 추적하기 위해 사용하는 플레이스홀더 DOM 노드(텍스트 노드 또는 SSR 하이드레이션(hydration) 모드의 주석 노드)입니다.

  **참고**
  일관성을 위해 `$el`에 의존하는 대신 [템플릿(template) ref](02_essentials.md#guide-essentials-template-refs)를 사용하여 엘리먼트에 직접 접근하는 것이 권장됩니다.


<a id="api-component-instance-options"></a>

### $options
현재 컴포넌트 인스턴스를 인스턴스화하는 데 사용된 해석된 컴포넌트 옵션입니다.

- **타입**

  ```ts
  interface ComponentPublicInstance {
    $options: ComponentOptions
  }
  ```

- **세부사항**

  `$options` 객체는 현재 컴포넌트의 해석된 옵션을 노출하며, 다음과 같은 가능한 소스들이 병합된 결과입니다:

  - 전역 믹스인(mixin)
  - 컴포넌트 `extends` 기반
  - 컴포넌트 믹스인

  일반적으로 사용자 정의 컴포넌트 옵션을 지원하는 데 사용됩니다:

  ```js
  const app = createApp({
    customOption: 'foo',
    created() {
      console.log(this.$options.customOption) // => 'foo'
    }
  })
  ```

- **참고** [`app.config.optionMergeStrategies`](07_composition_and_reactivity_apis.md#api-application-app-config-optionmergestrategies)

<a id="api-component-instance-parent"></a>

### $parent
현재 인스턴스에 부모가 있다면 부모 인스턴스입니다. 루트 인스턴스 자체의 경우 `null`입니다.

- **타입**

  ```ts
  interface ComponentPublicInstance {
    $parent: ComponentPublicInstance | null
  }
  ```

<a id="api-component-instance-root"></a>

### $root
현재 컴포넌트 트리의 루트 컴포넌트 인스턴스입니다. 현재 인스턴스에 부모가 없다면 이 값은 자기 자신입니다.

- **타입**

  ```ts
  interface ComponentPublicInstance {
    $root: ComponentPublicInstance
  }
  ```

<a id="api-component-instance-slots"></a>

### $slots
부모 컴포넌트가 전달한 [슬롯(slot)](03_components_and_reusability.md#guide-components-slots)을 나타내는 객체입니다.

- **타입**

  ```ts
  interface ComponentPublicInstance {
    $slots: { [name: string]: Slot }
  }

  type Slot = (...args: any[]) => VNode[]
  ```

- **세부사항**

  일반적으로 [렌더 함수](06_reactivity_and_rendering_in_depth.md#guide-extras-render-function)를 수동으로 작성할 때 사용되지만, 슬롯이 존재하는지 감지하는 데에도 사용할 수 있습니다.

  각 슬롯은 슬롯 이름을 키로 하여 `this.$slots`에 함수로 노출되며, 이 함수는 vnode 배열을 반환합니다. 기본 슬롯은 `this.$slots.default`로 노출됩니다.

  슬롯이 [스코프 슬롯(scoped slots)](03_components_and_reusability.md#guide-components-slots-scoped-slots)인 경우, 슬롯 함수에 전달된 인자를 해당 슬롯에서 슬롯 props로 사용할 수 있습니다.

- **참고** [렌더 함수 - 슬롯 렌더링(rendering)](06_reactivity_and_rendering_in_depth.md#guide-extras-render-function-rendering-slots)

<a id="api-component-instance-refs"></a>

### $refs
[템플릿 ref](02_essentials.md#guide-essentials-template-refs)를 통해 등록된 DOM 엘리먼트와 컴포넌트 인스턴스를 담은 객체입니다.

- **타입**

  ```ts
  interface ComponentPublicInstance {
    $refs: { [name: string]: Element | ComponentPublicInstance | null }
  }
  ```

- **참고**

  - [템플릿 ref](02_essentials.md#guide-essentials-template-refs)
  - [특수 속성 - ref](08_component_and_advanced_apis.md#api-built-in-special-attributes-ref)

<a id="api-component-instance-attrs"></a>

### $attrs
컴포넌트의 폴스루 속성(fallthrough attributes)을 포함하는 객체입니다.

- **타입**

  ```ts
  interface ComponentPublicInstance {
    $attrs: object
  }
  ```

- **세부사항**

  [폴스루 속성](03_components_and_reusability.md#guide-components-attrs)은 부모 컴포넌트가 전달했지만, 자식에서 prop이나 emit 이벤트로 선언되지 않은 속성과 이벤트 핸들러입니다.

  기본적으로, `$attrs`의 모든 내용은 컴포넌트에 단일 루트 엘리먼트가 있을 경우 해당 루트 엘리먼트에 자동으로 상속됩니다. 컴포넌트에 여러 루트 노드가 있으면 이 동작은 비활성화되며, [`inheritAttrs`](08_component_and_advanced_apis.md#api-options-misc-inheritattrs) 옵션으로 명시적으로 비활성화할 수 있습니다.

- **참고**

  - [폴스루 속성](03_components_and_reusability.md#guide-components-attrs)

<a id="api-component-instance-watch"></a>

### $watch()
감시자(watcher)를 생성하는 명령형 API입니다.

- **타입**

  ```ts
  interface ComponentPublicInstance {
    $watch(
      source: string | (() => any),
      callback: WatchCallback,
      options?: WatchOptions
    ): StopHandle
  }

  type WatchCallback<T> = (
    value: T,
    oldValue: T,
    onCleanup: (cleanupFn: () => void) => void
  ) => void

  interface WatchOptions {
    immediate?: boolean // 기본값: false
    deep?: boolean // 기본값: false
    flush?: 'pre' | 'post' | 'sync' // 기본값: 'pre'
    onTrack?: (event: DebuggerEvent) => void
    onTrigger?: (event: DebuggerEvent) => void
  }

  type StopHandle = () => void
  ```

- **세부사항**

  첫 번째 인자는 감시할 소스입니다. 컴포넌트 속성 이름 문자열, 점(.)으로 구분된 단순 경로 문자열, 또는 [getter 함수](https://developer.mozilla.org/ko/docs/Web/JavaScript/Reference/Functions/get#description)일 수 있습니다.

  두 번째 인자는 콜백(callback) 함수입니다. 콜백은 감시 대상의 새 값과 이전 값을 받습니다.

  - **`immediate`**: 감시자 생성 시 즉시 콜백을 트리거합니다. 첫 호출 시 이전 값은 `undefined`입니다.
  - **`deep`**: 소스가 객체일 경우 깊은 탐색을 강제하여, 깊은 변경에도 콜백이 실행됩니다. [깊은 감시자](02_essentials.md#guide-essentials-watchers-deep-watchers) 참고.
  - **`flush`**: 콜백의 실행 타이밍을 조정합니다. [콜백 실행 타이밍](02_essentials.md#guide-essentials-watchers-callback-flush-timing) 및 [`watchEffect()`](07_composition_and_reactivity_apis.md#api-reactivity-core-watcheffect) 참고.
  - **`onTrack / onTrigger`**: 감시자의 의존성 디버깅에 사용합니다. [감시자 디버깅](06_reactivity_and_rendering_in_depth.md#guide-extras-reactivity-in-depth-watcher-debugging) 참고.

- **예시**

  속성 이름 감시:

  ```js
  this.$watch('a', (newVal, oldVal) => {})
  ```

  점(.)으로 구분된 경로 감시:

  ```js
  this.$watch('a.b', (newVal, oldVal) => {})
  ```

  더 복잡한 표현식에 getter 사용:

  ```js
  this.$watch(
    // 표현식 `this.a + this.b`의 결과가
    // 달라질 때마다 핸들러가 호출됩니다.
    // 마치 계산된 속성을 정의하지 않고
    // 계산된 속성을 감시하는 것과 같습니다.
    () => this.a + this.b,
    (newVal, oldVal) => {}
  )
  ```

  감시자 중지:

  ```js
  const unwatch = this.$watch('a', cb)

  // 나중에...
  unwatch()
  ```

- **참고**
  - [옵션 - `watch`](08_component_and_advanced_apis.md#api-options-state-watch)
  - [가이드 - 감시자](02_essentials.md#guide-essentials-watchers)

<a id="api-component-instance-emit"></a>

### $emit()
현재 인스턴스에서 커스텀 이벤트를 트리거합니다. 추가 인자는 리스너(listener)의 콜백 함수로 전달됩니다.

- **타입**

  ```ts
  interface ComponentPublicInstance {
    $emit(event: string, ...args: any[]): void
  }
  ```

- **예시**

  ```js
  export default {
    created() {
      // 이벤트만
      this.$emit('foo')
      // 추가 인자와 함께
      this.$emit('bar', 1, 2, 3)
    }
  }
  ```

- **참고**

  - [컴포넌트 - 이벤트](03_components_and_reusability.md#guide-components-events)
  - [`emits` 옵션](08_component_and_advanced_apis.md#api-options-state-emits)

<a id="api-component-instance-forceupdate"></a>

### $forceUpdate()
컴포넌트 인스턴스를 강제로 다시 렌더링합니다.

- **타입**

  ```ts
  interface ComponentPublicInstance {
    $forceUpdate(): void
  }
  ```

- **세부사항**

  Vue의 완전 자동 반응성(reactivity) 시스템 덕분에 이 기능이 필요할 일은 거의 없습니다. 고급 반응성 API를 사용하여 명시적으로 비반응성 컴포넌트 상태를 생성한 경우에만 필요할 수 있습니다.

<a id="api-component-instance-nexttick"></a>

### $nextTick()
전역 [`nextTick()`](07_composition_and_reactivity_apis.md#api-general-nexttick)의 인스턴스 바인딩(binding) 버전입니다.

- **타입**

  ```ts
  interface ComponentPublicInstance {
    $nextTick(callback?: (this: ComponentPublicInstance) => void): Promise<void>
  }
  ```

- **세부사항**

  `nextTick()`의 전역 버전과의 유일한 차이점은, `this.$nextTick()`에 전달된 콜백의 `this` 컨텍스트가 현재 컴포넌트 인스턴스에 바인딩된다는 점입니다.

- **참고** [`nextTick()`](07_composition_and_reactivity_apis.md#api-general-nexttick)

---

<a id="api-custom-elements"></a>

<a id="api-custom-elements-custom-elements-api"></a>

## 커스텀 엘리먼트 API

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/custom-elements.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/custom-elements.md

<a id="api-custom-elements-definecustomelement"></a>

### defineCustomElement()
이 메서드는 [`defineComponent`](07_composition_and_reactivity_apis.md#api-general-definecomponent)와 동일한 인자를 받지만, 대신 네이티브 [커스텀 엘리먼트](https://developer.mozilla.org/ko/docs/Web/Web_Components/Using_custom_elements) 클래스 생성자를 반환합니다.

- **타입**

  ```ts
  function defineCustomElement(
    component:
      | (ComponentOptions & CustomElementsOptions)
      | ComponentOptions['setup'],
    options?: CustomElementsOptions
  ): {
    new (props?: object): HTMLElement
  }

  interface CustomElementsOptions {
    styles?: string[]

    // 아래 옵션들은 3.5+ 버전에서 지원됩니다.
    configureApp?: (app: App) => void
    shadowRoot?: boolean
    nonce?: string
  }
  ```

  > 가독성을 위해 타입이 단순화되었습니다.

- **세부사항**

  일반 컴포넌트(component) 옵션 외에도, `defineCustomElement()`는 커스텀 엘리먼트 전용 옵션들을 추가로 지원합니다:

  - **`styles`**: 엘리먼트의 섀도우 루트에 주입할 CSS를 제공하는 인라인 CSS 문자열 배열입니다.

  - **`configureApp`**  (3.5+): 커스텀 엘리먼트의 Vue 앱 인스턴스(instance)를 구성하는 데 사용할 수 있는 함수입니다.

  - **`shadowRoot`**  (3.5+): `boolean`, 기본값은 `true`입니다. `false`로 설정하면 커스텀 엘리먼트가 섀도우 루트 없이 렌더링(rendering)됩니다. 이 경우 커스텀 엘리먼트 SFC 내의 `<style>`은 더 이상 캡슐화되지 않습니다.

  - **`nonce`**  (3.5+): `string`, 값을 제공하면 섀도우 루트에 주입되는 style 태그의 `nonce` 속성으로 설정됩니다.

  이러한 옵션들은 컴포넌트 자체에 포함하는 대신, 두 번째 인자를 통해서도 전달할 수 있습니다:

  ```js
  import Element from './MyElement.ce.vue'

  defineCustomElement(Element, {
    configureApp(app) {
      // ...
    }
  })
  ```

  반환값은 [`customElements.define()`](https://developer.mozilla.org/ko/docs/Web/API/CustomElementRegistry/define)을 사용하여 등록할 수 있는 커스텀 엘리먼트 생성자입니다.

- **예시**

  ```js
  import { defineCustomElement } from 'vue'

  const MyVueElement = defineCustomElement({
    /* 컴포넌트 옵션 */
  })

  // 커스텀 엘리먼트 등록
  customElements.define('my-vue-element', MyVueElement)
  ```

- **관련 문서**

  - [가이드 - Vue로 커스텀 엘리먼트 빌드하기](06_reactivity_and_rendering_in_depth.md#guide-extras-web-components-building-custom-elements-with-vue)

  - 또한, Single-File Component와 함께 `defineCustomElement()`를 사용할 때는 [특별한 설정](06_reactivity_and_rendering_in_depth.md#guide-extras-web-components-sfc-as-custom-element)이 필요함을 참고하세요.

<a id="api-custom-elements-usehost"></a>

### useHost()  (3.5+)
현재 Vue 커스텀 엘리먼트의 호스트 엘리먼트를 반환하는 컴포지션 API 헬퍼입니다.

<a id="api-custom-elements-useshadowroot"></a>

### useShadowRoot()  (3.5+)
현재 Vue 커스텀 엘리먼트의 섀도우 루트를 반환하는 컴포지션 API 헬퍼입니다.

<a id="api-custom-elements-this-host"></a>

### this.$host  (3.5+)
현재 Vue 커스텀 엘리먼트의 호스트 엘리먼트를 노출하는 옵션 API 속성입니다.

---

<a id="api-custom-renderer"></a>

<a id="api-custom-renderer-custom-renderer-api"></a>

## 커스텀 렌더러 API

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/custom-renderer.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/custom-renderer.md

<a id="api-custom-renderer-createrenderer"></a>

### createRenderer()
커스텀 렌더러를 생성합니다. 플랫폼별 노드 생성 및 조작 API를 제공함으로써, Vue의 코어 런타임을 활용하여 DOM이 아닌 환경을 대상으로 할 수 있습니다.

- **타입**

  ```ts
  function createRenderer<HostNode, HostElement>(
    options: RendererOptions<HostNode, HostElement>
  ): Renderer<HostElement>

  interface Renderer<HostElement> {
    render: RootRenderFunction<HostElement>
    createApp: CreateAppFunction<HostElement>
  }

  interface RendererOptions<HostNode, HostElement> {
    patchProp(
      el: HostElement,
      key: string,
      prevValue: any,
      nextValue: any,
      namespace?: ElementNamespace,
      parentComponent?: ComponentInternalInstance | null,
    ): void
    insert(el: HostNode, parent: HostElement, anchor?: HostNode | null): void
    remove(el: HostNode): void
    createElement(
      type: string,
      namespace?: ElementNamespace,
      isCustomizedBuiltIn?: string,
      vnodeProps?: (VNodeProps & { [key: string]: any }) | null,
    ): HostElement
    createText(text: string): HostNode
    createComment(text: string): HostNode
    setText(node: HostNode, text: string): void
    setElementText(node: HostElement, text: string): void
    parentNode(node: HostNode): HostElement | null
    nextSibling(node: HostNode): HostNode | null
    querySelector?(selector: string): HostElement | null
    setScopeId?(el: HostElement, id: string): void
    cloneNode?(node: HostNode): HostNode
    insertStaticContent?(
      content: string,
      parent: HostElement,
      anchor: HostNode | null,
      namespace: ElementNamespace,
      start?: HostNode | null,
      end?: HostNode | null,
    ): [HostNode, HostNode]
  }
  ```

- **예시**

  ```js
  import { createRenderer } from '@vue/runtime-core'

  const { render, createApp } = createRenderer({
    patchProp,
    insert,
    remove,
    createElement
    // ...
  })

  // `render`는 저수준 API입니다
  // `createApp`은 앱 인스턴스를 반환합니다
  export { render, createApp }

  // Vue 코어 API를 다시 내보냅니다
  export * from '@vue/runtime-core'
  ```

  Vue의 자체 `@vue/runtime-dom`은 [동일한 API를 사용하여 구현되었습니다](https://github.com/vuejs/core/blob/main/packages/runtime-dom/src/index.ts). 더 간단한 구현을 원한다면, Vue가 자체 단위 테스트에 사용하는 비공개 패키지인 [`@vue/runtime-test`](https://github.com/vuejs/core/blob/main/packages/runtime-test/src/index.ts)를 참고하세요.

---

<a id="api-index"></a>

## API 참조

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/index.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/index.md

### [애플리케이션 API](07_composition_and_reactivity_apis.md#api-application)

- [createApp()](07_composition_and_reactivity_apis.md#api-application-createapp)
- [createSSRApp()](07_composition_and_reactivity_apis.md#api-application-createssrapp)
- [app.mount()](07_composition_and_reactivity_apis.md#api-application-app-mount)
- [app.unmount()](07_composition_and_reactivity_apis.md#api-application-app-unmount)
- [app.onUnmount()  (3.5+)](07_composition_and_reactivity_apis.md#api-application-app-onunmount)
- [app.component()](07_composition_and_reactivity_apis.md#api-application-app-component)
- [app.directive()](07_composition_and_reactivity_apis.md#api-application-app-directive)
- [app.use()](07_composition_and_reactivity_apis.md#api-application-app-use)
- [app.mixin()](07_composition_and_reactivity_apis.md#api-application-app-mixin)
- [app.provide()](07_composition_and_reactivity_apis.md#api-application-app-provide)
- [app.runWithContext()](07_composition_and_reactivity_apis.md#api-application-app-runwithcontext)
- [app.version](07_composition_and_reactivity_apis.md#api-application-app-version)
- [app.config](07_composition_and_reactivity_apis.md#api-application-app-config)
- [app.config.errorHandler](07_composition_and_reactivity_apis.md#api-application-app-config-errorhandler)
- [app.config.warnHandler](07_composition_and_reactivity_apis.md#api-application-app-config-warnhandler)
- [app.config.performance](07_composition_and_reactivity_apis.md#api-application-app-config-performance)
- [app.config.compilerOptions](07_composition_and_reactivity_apis.md#api-application-app-config-compileroptions)
- [app.config.globalProperties](07_composition_and_reactivity_apis.md#api-application-app-config-globalproperties)
- [app.config.optionMergeStrategies](07_composition_and_reactivity_apis.md#api-application-app-config-optionmergestrategies)
- [app.config.idPrefix  (3.5+)](07_composition_and_reactivity_apis.md#api-application-app-config-idprefix)
- [app.config.throwUnhandledErrorInProduction  (3.5+)](07_composition_and_reactivity_apis.md#api-application-app-config-throwunhandlederrorinproduction)

### [내장 컴포넌트](08_component_and_advanced_apis.md#api-built-in-components)

- [`<Transition>`](08_component_and_advanced_apis.md#api-built-in-components-transition)
- [`<TransitionGroup>`](08_component_and_advanced_apis.md#api-built-in-components-transitiongroup)
- [`<KeepAlive>`](08_component_and_advanced_apis.md#api-built-in-components-keepalive)
- [`<Teleport>`](08_component_and_advanced_apis.md#api-built-in-components-teleport)
- [`<Suspense>`  (실험적 기능)](08_component_and_advanced_apis.md#api-built-in-components-suspense)

### [내장 디렉티브](08_component_and_advanced_apis.md#api-built-in-directives)

- [v-text](08_component_and_advanced_apis.md#api-built-in-directives-v-text)
- [v-html](08_component_and_advanced_apis.md#api-built-in-directives-v-html)
- [v-show](08_component_and_advanced_apis.md#api-built-in-directives-v-show)
- [v-if](08_component_and_advanced_apis.md#api-built-in-directives-v-if)
- [v-else](08_component_and_advanced_apis.md#api-built-in-directives-v-else)
- [v-else-if](08_component_and_advanced_apis.md#api-built-in-directives-v-else-if)
- [v-for](08_component_and_advanced_apis.md#api-built-in-directives-v-for)
- [v-on](08_component_and_advanced_apis.md#api-built-in-directives-v-on)
- [v-bind](08_component_and_advanced_apis.md#api-built-in-directives-v-bind)
- [v-model](08_component_and_advanced_apis.md#api-built-in-directives-v-model)
- [v-slot](08_component_and_advanced_apis.md#api-built-in-directives-v-slot)
- [v-pre](08_component_and_advanced_apis.md#api-built-in-directives-v-pre)
- [v-once](08_component_and_advanced_apis.md#api-built-in-directives-v-once)
- [v-memo](08_component_and_advanced_apis.md#api-built-in-directives-v-memo)
- [v-cloak](08_component_and_advanced_apis.md#api-built-in-directives-v-cloak)

### [내장 특수 속성](08_component_and_advanced_apis.md#api-built-in-special-attributes)

- [key](08_component_and_advanced_apis.md#api-built-in-special-attributes-key)
- [ref](08_component_and_advanced_apis.md#api-built-in-special-attributes-ref)
- [is](08_component_and_advanced_apis.md#api-built-in-special-attributes-is)

### [내장 특수 엘리먼트](08_component_and_advanced_apis.md#api-built-in-special-elements)

- [`<component>`](08_component_and_advanced_apis.md#api-built-in-special-elements-component)
- [`<slot>`](08_component_and_advanced_apis.md#api-built-in-special-elements-slot)
- [`<template>`](08_component_and_advanced_apis.md#api-built-in-special-elements-template)

### [컴파일 타임 플래그](08_component_and_advanced_apis.md#api-compile-time-flags)

- [`__VUE_OPTIONS_API__`](08_component_and_advanced_apis.md#api-compile-time-flags-VUE_OPTIONS_API)
- [`__VUE_PROD_DEVTOOLS__`](08_component_and_advanced_apis.md#api-compile-time-flags-VUE_PROD_DEVTOOLS)
- [`__VUE_PROD_HYDRATION_MISMATCH_DETAILS__`](08_component_and_advanced_apis.md#api-compile-time-flags-VUE_PROD_HYDRATION_MISMATCH_DETAILS)
- [설정 가이드](08_component_and_advanced_apis.md#api-compile-time-flags-configuration-guides)

### [컴포넌트 인스턴스](08_component_and_advanced_apis.md#api-component-instance)

- [$data](08_component_and_advanced_apis.md#api-component-instance-data)
- [$props](08_component_and_advanced_apis.md#api-component-instance-props)
- [$el](08_component_and_advanced_apis.md#api-component-instance-el)
- [$options](08_component_and_advanced_apis.md#api-component-instance-options)
- [$parent](08_component_and_advanced_apis.md#api-component-instance-parent)
- [$root](08_component_and_advanced_apis.md#api-component-instance-root)
- [$slots](08_component_and_advanced_apis.md#api-component-instance-slots)
- [$refs](08_component_and_advanced_apis.md#api-component-instance-refs)
- [$attrs](08_component_and_advanced_apis.md#api-component-instance-attrs)
- [$watch()](08_component_and_advanced_apis.md#api-component-instance-watch)
- [$emit()](08_component_and_advanced_apis.md#api-component-instance-emit)
- [$forceUpdate()](08_component_and_advanced_apis.md#api-component-instance-forceupdate)
- [$nextTick()](08_component_and_advanced_apis.md#api-component-instance-nexttick)

### [Composition API: <br>의존성 주입(dependency injection)](07_composition_and_reactivity_apis.md#api-composition-api-dependency-injection)

- [provide()](07_composition_and_reactivity_apis.md#api-composition-api-dependency-injection-provide)
- [inject()](07_composition_and_reactivity_apis.md#api-composition-api-dependency-injection-inject)
- [hasInjectionContext()](07_composition_and_reactivity_apis.md#api-composition-api-dependency-injection-has-injection-context)

### [Composition API: Helpers](07_composition_and_reactivity_apis.md#api-composition-api-helpers)

- [useAttrs()](07_composition_and_reactivity_apis.md#api-composition-api-helpers-useattrs)
- [useSlots()](07_composition_and_reactivity_apis.md#api-composition-api-helpers-useslots)
- [useModel()](07_composition_and_reactivity_apis.md#api-composition-api-helpers-usemodel)
- [useTemplateRef()  (3.5+)](07_composition_and_reactivity_apis.md#api-composition-api-helpers-usetemplateref)
- [useId()  (3.5+)](07_composition_and_reactivity_apis.md#api-composition-api-helpers-useid)

### [컴포지션 API: 라이프사이클 훅](07_composition_and_reactivity_apis.md#api-composition-api-lifecycle)

- [onMounted()](07_composition_and_reactivity_apis.md#api-composition-api-lifecycle-onmounted)
- [onUpdated()](07_composition_and_reactivity_apis.md#api-composition-api-lifecycle-onupdated)
- [onUnmounted()](07_composition_and_reactivity_apis.md#api-composition-api-lifecycle-onunmounted)
- [onBeforeMount()](07_composition_and_reactivity_apis.md#api-composition-api-lifecycle-onbeforemount)
- [onBeforeUpdate()](07_composition_and_reactivity_apis.md#api-composition-api-lifecycle-onbeforeupdate)
- [onBeforeUnmount()](07_composition_and_reactivity_apis.md#api-composition-api-lifecycle-onbeforeunmount)
- [onErrorCaptured()](07_composition_and_reactivity_apis.md#api-composition-api-lifecycle-onerrorcaptured)
- [onRenderTracked()  (개발 모드 전용)](07_composition_and_reactivity_apis.md#api-composition-api-lifecycle-onrendertracked)
- [onRenderTriggered()  (개발 모드 전용)](07_composition_and_reactivity_apis.md#api-composition-api-lifecycle-onrendertriggered)
- [onActivated()](07_composition_and_reactivity_apis.md#api-composition-api-lifecycle-onactivated)
- [onDeactivated()](07_composition_and_reactivity_apis.md#api-composition-api-lifecycle-ondeactivated)
- [onServerPrefetch()  (SSR only)](07_composition_and_reactivity_apis.md#api-composition-api-lifecycle-onserverprefetch)

### [컴포지션 API: setup()](07_composition_and_reactivity_apis.md#api-composition-api-setup)

- [기본 사용법](07_composition_and_reactivity_apis.md#api-composition-api-setup-basic-usage)
- [Props 접근하기](07_composition_and_reactivity_apis.md#api-composition-api-setup-accessing-props)
- [Setup Context](07_composition_and_reactivity_apis.md#api-composition-api-setup-setup-context)
- [렌더 함수와 함께 사용하기](07_composition_and_reactivity_apis.md#api-composition-api-setup-usage-with-render-functions)

### [커스텀 엘리먼트 API](08_component_and_advanced_apis.md#api-custom-elements)

- [defineCustomElement()](08_component_and_advanced_apis.md#api-custom-elements-definecustomelement)
- [useHost()  (3.5+)](08_component_and_advanced_apis.md#api-custom-elements-usehost)
- [useShadowRoot()  (3.5+)](08_component_and_advanced_apis.md#api-custom-elements-useshadowroot)
- [this.$host  (3.5+)](08_component_and_advanced_apis.md#api-custom-elements-this-host)

### [커스텀 렌더러 API](08_component_and_advanced_apis.md#api-custom-renderer)

- [createRenderer()](08_component_and_advanced_apis.md#api-custom-renderer-createrenderer)

### [글로벌 API: 일반](07_composition_and_reactivity_apis.md#api-general)

- [version](07_composition_and_reactivity_apis.md#api-general-version)
- [nextTick()](07_composition_and_reactivity_apis.md#api-general-nexttick)
- [defineComponent()](07_composition_and_reactivity_apis.md#api-general-definecomponent)
- [defineAsyncComponent()](07_composition_and_reactivity_apis.md#api-general-defineasynccomponent)

### [옵션: Composition](08_component_and_advanced_apis.md#api-options-composition)

- [provide](08_component_and_advanced_apis.md#api-options-composition-provide)
- [inject](08_component_and_advanced_apis.md#api-options-composition-inject)
- [mixins](08_component_and_advanced_apis.md#api-options-composition-mixins)
- [extends](08_component_and_advanced_apis.md#api-options-composition-extends)

### [옵션: 라이프사이클](08_component_and_advanced_apis.md#api-options-lifecycle)

- [beforeCreate](08_component_and_advanced_apis.md#api-options-lifecycle-beforecreate)
- [created](08_component_and_advanced_apis.md#api-options-lifecycle-created)
- [beforeMount](08_component_and_advanced_apis.md#api-options-lifecycle-beforemount)
- [mounted](08_component_and_advanced_apis.md#api-options-lifecycle-mounted)
- [beforeUpdate](08_component_and_advanced_apis.md#api-options-lifecycle-beforeupdate)
- [updated](08_component_and_advanced_apis.md#api-options-lifecycle-updated)
- [beforeUnmount](08_component_and_advanced_apis.md#api-options-lifecycle-beforeunmount)
- [unmounted](08_component_and_advanced_apis.md#api-options-lifecycle-unmounted)
- [errorCaptured](08_component_and_advanced_apis.md#api-options-lifecycle-errorcaptured)
- [renderTracked  (개발 모드 전용)](08_component_and_advanced_apis.md#api-options-lifecycle-rendertracked)
- [renderTriggered  (개발 모드 전용)](08_component_and_advanced_apis.md#api-options-lifecycle-rendertriggered)
- [activated](08_component_and_advanced_apis.md#api-options-lifecycle-activated)
- [deactivated](08_component_and_advanced_apis.md#api-options-lifecycle-deactivated)
- [serverPrefetch  (SSR only)](08_component_and_advanced_apis.md#api-options-lifecycle-serverprefetch)

### [옵션: 기타](08_component_and_advanced_apis.md#api-options-misc)

- [name](08_component_and_advanced_apis.md#api-options-misc-name)
- [inheritAttrs](08_component_and_advanced_apis.md#api-options-misc-inheritattrs)
- [components](08_component_and_advanced_apis.md#api-options-misc-components)
- [directives](08_component_and_advanced_apis.md#api-options-misc-directives)

### [옵션: 렌더링](08_component_and_advanced_apis.md#api-options-rendering)

- [template](08_component_and_advanced_apis.md#api-options-rendering-template)
- [render](08_component_and_advanced_apis.md#api-options-rendering-render)
- [compilerOptions](08_component_and_advanced_apis.md#api-options-rendering-compileroptions)
- [slots (TypeScript)](08_component_and_advanced_apis.md#api-options-rendering-slots)

### [옵션: 상태](08_component_and_advanced_apis.md#api-options-state)

- [data](08_component_and_advanced_apis.md#api-options-state-data)
- [props](08_component_and_advanced_apis.md#api-options-state-props)
- [computed](08_component_and_advanced_apis.md#api-options-state-computed)
- [methods](08_component_and_advanced_apis.md#api-options-state-methods)
- [watch](08_component_and_advanced_apis.md#api-options-state-watch)
- [emits](08_component_and_advanced_apis.md#api-options-state-emits)
- [expose](08_component_and_advanced_apis.md#api-options-state-expose)

### [반응성 API: 고급](07_composition_and_reactivity_apis.md#api-reactivity-advanced)

- [shallowRef()](07_composition_and_reactivity_apis.md#api-reactivity-advanced-shallowref)
- [triggerRef()](07_composition_and_reactivity_apis.md#api-reactivity-advanced-triggerref)
- [customRef()](07_composition_and_reactivity_apis.md#api-reactivity-advanced-customref)
- [shallowReactive()](07_composition_and_reactivity_apis.md#api-reactivity-advanced-shallowreactive)
- [shallowReadonly()](07_composition_and_reactivity_apis.md#api-reactivity-advanced-shallowreadonly)
- [toRaw()](07_composition_and_reactivity_apis.md#api-reactivity-advanced-toraw)
- [markRaw()](07_composition_and_reactivity_apis.md#api-reactivity-advanced-markraw)
- [effectScope()](07_composition_and_reactivity_apis.md#api-reactivity-advanced-effectscope)
- [getCurrentScope()](07_composition_and_reactivity_apis.md#api-reactivity-advanced-getcurrentscope)
- [onScopeDispose()](07_composition_and_reactivity_apis.md#api-reactivity-advanced-onscopedispose)

### [반응성 API: 코어](07_composition_and_reactivity_apis.md#api-reactivity-core)

- [ref()](07_composition_and_reactivity_apis.md#api-reactivity-core-ref)
- [computed()](07_composition_and_reactivity_apis.md#api-reactivity-core-computed)
- [reactive()](07_composition_and_reactivity_apis.md#api-reactivity-core-reactive)
- [readonly()](07_composition_and_reactivity_apis.md#api-reactivity-core-readonly)
- [watchEffect()](07_composition_and_reactivity_apis.md#api-reactivity-core-watcheffect)
- [watchPostEffect()](07_composition_and_reactivity_apis.md#api-reactivity-core-watchposteffect)
- [watchSyncEffect()](07_composition_and_reactivity_apis.md#api-reactivity-core-watchsynceffect)
- [watch()](07_composition_and_reactivity_apis.md#api-reactivity-core-watch)
- [onWatcherCleanup()  (3.5+)](07_composition_and_reactivity_apis.md#api-reactivity-core-onwatchercleanup)

### [반응성 API: 유틸리티](07_composition_and_reactivity_apis.md#api-reactivity-utilities)

- [isRef()](07_composition_and_reactivity_apis.md#api-reactivity-utilities-isref)
- [unref()](07_composition_and_reactivity_apis.md#api-reactivity-utilities-unref)
- [toRef()](07_composition_and_reactivity_apis.md#api-reactivity-utilities-toref)
- [toValue()](07_composition_and_reactivity_apis.md#api-reactivity-utilities-tovalue)
- [toRefs()](07_composition_and_reactivity_apis.md#api-reactivity-utilities-torefs)
- [isProxy()](07_composition_and_reactivity_apis.md#api-reactivity-utilities-isproxy)
- [isReactive()](07_composition_and_reactivity_apis.md#api-reactivity-utilities-isreactive)
- [isReadonly()](07_composition_and_reactivity_apis.md#api-reactivity-utilities-isreadonly)
- [isShallow()](07_composition_and_reactivity_apis.md#api-reactivity-utilities-isshallow)

### [렌더 함수 API](08_component_and_advanced_apis.md#api-render-function)

- [h()](08_component_and_advanced_apis.md#api-render-function-h)
- [mergeProps()](08_component_and_advanced_apis.md#api-render-function-mergeprops)
- [cloneVNode()](08_component_and_advanced_apis.md#api-render-function-clonevnode)
- [isVNode()](08_component_and_advanced_apis.md#api-render-function-isvnode)
- [resolveComponent()](08_component_and_advanced_apis.md#api-render-function-resolvecomponent)
- [resolveDirective()](08_component_and_advanced_apis.md#api-render-function-resolvedirective)
- [withDirectives()](08_component_and_advanced_apis.md#api-render-function-withdirectives)
- [withModifiers()](08_component_and_advanced_apis.md#api-render-function-withmodifiers)

### [SFC CSS 기능](08_component_and_advanced_apis.md#api-sfc-css-features)

- [Scoped CSS](08_component_and_advanced_apis.md#api-sfc-css-features-scoped-css)
- [CSS 모듈](08_component_and_advanced_apis.md#api-sfc-css-features-css-modules)
- [CSS에서 `v-bind()`](08_component_and_advanced_apis.md#api-sfc-css-features-v-bind-in-css)

### [\<script setup>](08_component_and_advanced_apis.md#api-sfc-script-setup)

- [기본 문법](08_component_and_advanced_apis.md#api-sfc-script-setup-basic-syntax)
- [반응성(reactivity)](08_component_and_advanced_apis.md#api-sfc-script-setup-reactivity)
- [컴포넌트 사용하기](08_component_and_advanced_apis.md#api-sfc-script-setup-using-components)
- [커스텀 디렉티브 사용하기](08_component_and_advanced_apis.md#api-sfc-script-setup-using-custom-directives)
- [defineProps() & defineEmits()](08_component_and_advanced_apis.md#api-sfc-script-setup-defineprops-defineemits)
- [defineModel()](08_component_and_advanced_apis.md#api-sfc-script-setup-definemodel)
- [defineExpose()](08_component_and_advanced_apis.md#api-sfc-script-setup-defineexpose)
- [defineOptions()](08_component_and_advanced_apis.md#api-sfc-script-setup-defineoptions)
- [defineSlots() (TypeScript)](08_component_and_advanced_apis.md#api-sfc-script-setup-defineslots)
- [`useSlots()` & `useAttrs()`](08_component_and_advanced_apis.md#api-sfc-script-setup-useslots-useattrs)
- [일반 `<script>`와 함께 사용하기](08_component_and_advanced_apis.md#api-sfc-script-setup-usage-alongside-normal-script)
- [최상위 `await`](08_component_and_advanced_apis.md#api-sfc-script-setup-top-level-await)
- [import 구문](08_component_and_advanced_apis.md#api-sfc-script-setup-imports-statements)
- [제네릭  (TypeScript)](08_component_and_advanced_apis.md#api-sfc-script-setup-generics)
- [제약 사항](08_component_and_advanced_apis.md#api-sfc-script-setup-restrictions)

### [SFC 구문 명세](08_component_and_advanced_apis.md#api-sfc-spec)

- [개요](08_component_and_advanced_apis.md#api-sfc-spec-overview)
- [언어 블록](08_component_and_advanced_apis.md#api-sfc-spec-language-blocks)
- [자동 이름 추론](08_component_and_advanced_apis.md#api-sfc-spec-automatic-name-inference)
- [프리프로세서](08_component_and_advanced_apis.md#api-sfc-spec-pre-processors)
- [`src` 임포트](08_component_and_advanced_apis.md#api-sfc-spec-src-imports)
- [주석](08_component_and_advanced_apis.md#api-sfc-spec-comments)

### [서버 사이드 렌더링 API](08_component_and_advanced_apis.md#api-ssr)

- [renderToString()](08_component_and_advanced_apis.md#api-ssr-rendertostring)
- [renderToNodeStream()](08_component_and_advanced_apis.md#api-ssr-rendertonodestream)
- [pipeToNodeWritable()](08_component_and_advanced_apis.md#api-ssr-pipetonodewritable)
- [renderToWebStream()](08_component_and_advanced_apis.md#api-ssr-rendertowebstream)
- [pipeToWebWritable()](08_component_and_advanced_apis.md#api-ssr-pipetowebwritable)
- [renderToSimpleStream()](08_component_and_advanced_apis.md#api-ssr-rendertosimplestream)
- [useSSRContext()](08_component_and_advanced_apis.md#api-ssr-usessrcontext)
- [data-allow-mismatch  (3.5+)](08_component_and_advanced_apis.md#api-ssr-data-allow-mismatch)

### [유틸리티 타입](08_component_and_advanced_apis.md#api-utility-types)

- [PropType\<T>](08_component_and_advanced_apis.md#api-utility-types-proptype-t)
- [MaybeRef\<T>](08_component_and_advanced_apis.md#api-utility-types-mayberef)
- [MaybeRefOrGetter\<T>](08_component_and_advanced_apis.md#api-utility-types-maybereforgetter)
- [ExtractPropTypes\<T>](08_component_and_advanced_apis.md#api-utility-types-extractproptypes)
- [ExtractPublicPropTypes\<T>](08_component_and_advanced_apis.md#api-utility-types-extractpublicproptypes)
- [ComponentCustomProperties](08_component_and_advanced_apis.md#api-utility-types-componentcustomproperties)
- [ComponentCustomOptions](08_component_and_advanced_apis.md#api-utility-types-componentcustomoptions)
- [ComponentCustomProps](08_component_and_advanced_apis.md#api-utility-types-componentcustomprops)
- [CSSProperties](08_component_and_advanced_apis.md#api-utility-types-cssproperties)

---

<a id="api-options-composition"></a>

<a id="api-options-composition-options-composition"></a>

## 옵션: Composition

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/options-composition.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/options-composition.md

<a id="api-options-composition-provide"></a>

### provide
하위 컴포넌트에서 주입할 수 있는 값을 제공합니다.

- **타입**

  ```ts
  interface ComponentOptions {
    provide?: object | ((this: ComponentPublicInstance) => object)
  }
  ```

- **세부사항**

  `provide`와 [`inject`](08_component_and_advanced_apis.md#api-options-composition-inject)는 함께 사용되어, 상위 컴포넌트가 모든 하위 컴포넌트의 의존성 주입자로 동작할 수 있게 해줍니다. 컴포넌트(component) 계층 구조가 얼마나 깊든, 같은 부모 체인 내에 있는 한 주입이 가능합니다.

  `provide` 옵션은 객체이거나 객체를 반환하는 함수여야 합니다. 이 객체는 하위 컴포넌트에서 주입받을 수 있는 속성들을 포함하며, 키로 Symbol을 사용할 수 있습니다.

- **예시**

  기본 사용법:

  ```js
  const s = Symbol()

  export default {
    provide: {
      foo: 'foo',
      [s]: 'bar'
    }
  }
  ```

  컴포넌트별 상태를 제공하기 위해 함수를 사용하는 방법:

  ```js
  export default {
    data() {
      return {
        msg: 'foo'
      }
    }
    provide() {
      return {
        msg: this.msg
      }
    }
  }
  ```

  위 예시에서 제공된 `msg`는 반응형이 **아님**에 유의하세요. 자세한 내용은 [반응형과 함께 사용하기](03_components_and_reusability.md#guide-components-provide-inject-working-with-reactivity)를 참고하세요.

- **관련 문서** [Provide / Inject](03_components_and_reusability.md#guide-components-provide-inject)

<a id="api-options-composition-inject"></a>

### inject
상위 제공자에서 찾아 현재 컴포넌트에 주입할 속성을 선언합니다.

- **타입**

  ```ts
  interface ComponentOptions {
    inject?: ArrayInjectOptions | ObjectInjectOptions
  }

  type ArrayInjectOptions = string[]

  type ObjectInjectOptions = {
    [key: string | symbol]:
      | string
      | symbol
      | { from?: string | symbol; default?: any }
  }
  ```

- **세부사항**

  `inject` 옵션은 다음 중 하나여야 합니다:

  - 문자열 배열, 또는
  - 객체로, 키는 로컬 바인딩(binding) 이름이고 값은 다음 중 하나입니다:
    - 사용 가능한 주입에서 찾을 키(문자열 또는 Symbol), 또는
    - 객체로,
      - `from` 속성은 사용 가능한 주입에서 찾을 키(문자열 또는 Symbol)이고,
      - `default` 속성은 기본값으로 사용됩니다. props의 기본값과 유사하게, 객체 타입의 경우 여러 컴포넌트 인스턴스(instance) 간에 값이 공유되지 않도록 팩토리 함수가 필요합니다.

  일치하는 속성이나 기본값이 제공되지 않은 경우, 주입된 속성은 `undefined`가 됩니다.

  주입된 바인딩은 **반응형이 아님**에 유의하세요. 이는 의도된 동작입니다. 하지만 주입된 값이 반응형 객체라면, 그 객체의 속성은 계속 반응형입니다. 자세한 내용은 [반응형과 함께 사용하기](03_components_and_reusability.md#guide-components-provide-inject-working-with-reactivity)를 참고하세요.

- **예시**

  기본 사용법:

  ```js
  export default {
    inject: ['foo'],
    created() {
      console.log(this.foo)
    }
  }
  ```

  주입된 값을 prop의 기본값으로 사용하는 방법:

  ```js
  const Child = {
    inject: ['foo'],
    props: {
      bar: {
        default() {
          return this.foo
        }
      }
    }
  }
  ```

  주입된 값을 data 항목으로 사용하는 방법:

  ```js
  const Child = {
    inject: ['foo'],
    data() {
      return {
        bar: this.foo
      }
    }
  }
  ```

  기본값이 있는 선택적 주입:

  ```js
  const Child = {
    inject: {
      foo: { default: 'foo' }
    }
  }
  ```

  다른 이름의 속성에서 주입해야 하는 경우, `from`을 사용하여 소스 속성을 지정할 수 있습니다:

  ```js
  const Child = {
    inject: {
      foo: {
        from: 'bar',
        default: 'foo'
      }
    }
  }
  ```

  prop의 기본값과 마찬가지로, 비원시 값의 경우 팩토리 함수를 사용해야 합니다:

  ```js
  const Child = {
    inject: {
      foo: {
        from: 'bar',
        default: () => [1, 2, 3]
      }
    }
  }
  ```

- **관련 문서** [Provide / Inject](03_components_and_reusability.md#guide-components-provide-inject)

<a id="api-options-composition-mixins"></a>

### mixins
현재 컴포넌트에 혼합될 옵션 객체의 배열입니다.

- **타입**

  ```ts
  interface ComponentOptions {
    mixins?: ComponentOptions[]
  }
  ```

- **세부사항**

  `mixins` 옵션은 믹스인(mixin) 객체의 배열을 받습니다. 이 믹스인 객체들은 일반 인스턴스 객체처럼 인스턴스 옵션을 포함할 수 있으며, 옵션별 병합 로직에 따라 최종 옵션과 병합됩니다. 예를 들어, 믹스인에 `created` 훅(hook)이 있고 컴포넌트 자체에도 있다면, 두 함수가 모두 호출됩니다.

  믹스인 훅은 제공된 순서대로 호출되며, 컴포넌트 자체의 훅보다 먼저 호출됩니다.

  **더 이상 권장되지 않음**
  Vue 2에서는 믹스인이 컴포넌트 로직의 재사용 가능한 조각을 만드는 주요 메커니즘이었습니다. Vue 3에서도 믹스인은 계속 지원되지만, 컴포넌트 간 코드 재사용에는 [컴포지션 API를 이용한 컴포저블(composable) 함수](03_components_and_reusability.md#guide-reusability-composables)가 이제 더 권장되는 방식입니다.


- **예시**

  ```js
  const mixin = {
    created() {
      console.log(1)
    }
  }

  createApp({
    created() {
      console.log(2)
    },
    mixins: [mixin]
  })

  // => 1
  // => 2
  ```

<a id="api-options-composition-extends"></a>

### extends
확장할 "기본 클래스" 컴포넌트입니다.

- **타입**

  ```ts
  interface ComponentOptions {
    extends?: ComponentOptions
  }
  ```

- **세부사항**

  한 컴포넌트가 다른 컴포넌트를 확장하여, 그 컴포넌트의 옵션을 상속받을 수 있게 합니다.

  구현 관점에서 `extends`는 `mixins`와 거의 동일합니다. `extends`로 지정된 컴포넌트는 첫 번째 믹스인처럼 취급됩니다.

  하지만 `extends`와 `mixins`는 의도가 다릅니다. `mixins` 옵션은 주로 기능 조각을 조합하는 데 사용되고, `extends`는 주로 상속에 초점을 둡니다.

  `mixins`와 마찬가지로, 모든 옵션(`setup()` 제외)은 관련 병합 전략을 사용하여 병합됩니다.

- **예시**

  ```js
  const CompA = { ... }

  const CompB = {
    extends: CompA,
    ...
  }
  ```

  **컴포지션 API에서는 권장되지 않음**
  `extends`는 옵션 API를 위해 설계되었으며, `setup()` 훅의 병합을 처리하지 않습니다.

  컴포지션 API에서는 로직 재사용을 위해 "상속"보다는 "조합"이 더 권장되는 사고방식입니다. 한 컴포넌트의 로직을 다른 컴포넌트에서 재사용해야 한다면, 관련 로직을 [컴포저블](03_components_and_reusability.md#guide-reusability-composables-composables)로 추출하는 것을 고려하세요.

  그래도 컴포지션 API에서 컴포넌트를 "확장"하고자 한다면, 확장 컴포넌트의 `setup()`에서 기본 컴포넌트의 `setup()`을 호출할 수 있습니다:

  ```js
  import Base from './Base.js'
  export default {
    extends: Base,
    setup(props, ctx) {
      return {
        ...Base.setup(props, ctx),
        // 로컬 바인딩
      }
    }
  }
  ```

---

<a id="api-options-lifecycle"></a>

<a id="api-options-lifecycle-options-lifecycle"></a>

## 옵션: 라이프사이클

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/options-lifecycle.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/options-lifecycle.md

**참고**
라이프사이클(lifecycle) 훅의 공통 사용법에 대해서는 [가이드 - 라이프사이클 훅](02_essentials.md#guide-essentials-lifecycle)을 참고하세요.


<a id="api-options-lifecycle-beforecreate"></a>

### beforeCreate
인스턴스(instance)가 초기화될 때 호출됩니다.

- **타입**

  ```ts
  interface ComponentOptions {
    beforeCreate?(this: ComponentPublicInstance): void
  }
  ```

- **세부사항**

  인스턴스가 초기화되고 props가 해석된 직후에 호출됩니다.

  이후 props는 반응형 속성으로 정의되고, `data()`나 `computed`와 같은 상태가 설정됩니다.

  컴포지션 API의 `setup()` 훅(hook)은 모든 옵션 API 훅보다 먼저, 심지어 `beforeCreate()`보다도 먼저 호출된다는 점에 유의하세요.

<a id="api-options-lifecycle-created"></a>

### created
인스턴스가 모든 상태 관련 옵션 처리를 마친 후 호출됩니다.

- **타입**

  ```ts
  interface ComponentOptions {
    created?(this: ComponentPublicInstance): void
  }
  ```

- **세부사항**

  이 훅이 호출될 때는 반응형 데이터, 계산된 속성(computed property), 메서드, 감시자(watchers)가 모두 설정되어 있습니다. 하지만 마운트(mount) 단계는 아직 시작되지 않았으며, `$el` 속성도 아직 사용할 수 없습니다.

<a id="api-options-lifecycle-beforemount"></a>

### beforeMount
컴포넌트(component)가 마운트되기 직전에 호출됩니다.

- **타입**

  ```ts
  interface ComponentOptions {
    beforeMount?(this: ComponentPublicInstance): void
  }
  ```

- **세부사항**

  이 훅이 호출될 때, 컴포넌트는 반응형 상태 설정을 마쳤지만 아직 DOM 노드가 생성되지 않았습니다. 이제 컴포넌트가 처음으로 DOM 렌더 효과를 실행하려는 참입니다.

  **이 훅은 서버 사이드 렌더링(rendering) 중에는 호출되지 않습니다.**

<a id="api-options-lifecycle-mounted"></a>

### mounted
컴포넌트가 마운트된 후 호출됩니다.

- **타입**

  ```ts
  interface ComponentOptions {
    mounted?(this: ComponentPublicInstance): void
  }
  ```

- **세부사항**

  컴포넌트가 마운트된 것으로 간주되는 시점은 다음과 같습니다:

  - 모든 동기 자식 컴포넌트가 마운트된 후 (비동기 컴포넌트나 `<Suspense>` 트리 내의 컴포넌트는 포함하지 않음)

  - 자신의 DOM 트리가 생성되어 부모 컨테이너에 삽입된 후. 단, 애플리케이션의 루트 컨테이너가 문서 내에 있을 때만 컴포넌트의 DOM 트리가 문서 내에 있음을 보장합니다.

  이 훅은 일반적으로 컴포넌트의 렌더링된 DOM에 접근해야 하는 부수 효과를 수행하거나, [서버 렌더링 애플리케이션](05_scaling_typescript_and_best_practices.md#guide-scaling-up-ssr)에서 DOM 관련 코드를 클라이언트로 제한할 때 사용됩니다.

  **이 훅은 서버 사이드 렌더링 중에는 호출되지 않습니다.**

<a id="api-options-lifecycle-beforeupdate"></a>

### beforeUpdate
반응형 상태 변경으로 인해 컴포넌트의 DOM 트리가 업데이트되기 직전에 호출됩니다.

- **타입**

  ```ts
  interface ComponentOptions {
    beforeUpdate?(this: ComponentPublicInstance): void
  }
  ```

- **세부사항**

  이 훅에서는 Vue가 DOM을 업데이트하기 전의 DOM 상태에 접근할 수 있습니다. 또한 이 훅 내에서 컴포넌트 상태를 수정해도 안전합니다.

  **이 훅은 서버 사이드 렌더링 중에는 호출되지 않습니다.**

<a id="api-options-lifecycle-updated"></a>

### updated
반응형 상태 변경으로 인해 컴포넌트의 DOM 트리가 업데이트된 후 호출됩니다.

- **타입**

  ```ts
  interface ComponentOptions {
    updated?(this: ComponentPublicInstance): void
  }
  ```

- **세부사항**

  부모 컴포넌트의 updated 훅은 자식 컴포넌트의 updated 훅이 호출된 후에 호출됩니다.

  이 훅은 컴포넌트의 모든 DOM 업데이트 후에 호출되며, 이는 다양한 상태 변경에 의해 발생할 수 있습니다. 특정 상태 변경 후에 업데이트된 DOM에 접근해야 한다면 [nextTick()](07_composition_and_reactivity_apis.md#api-general-nexttick)을 대신 사용하세요.

  **이 훅은 서버 사이드 렌더링 중에는 호출되지 않습니다.**

  **주의**
  updated 훅 내에서 컴포넌트 상태를 변경하지 마세요. 그러면 무한 업데이트 루프를 유발할 수 있습니다!


<a id="api-options-lifecycle-beforeunmount"></a>

### beforeUnmount
컴포넌트 인스턴스가 언마운트(unmount)되기 직전에 호출됩니다.

- **타입**

  ```ts
  interface ComponentOptions {
    beforeUnmount?(this: ComponentPublicInstance): void
  }
  ```

- **세부사항**

  이 훅이 호출될 때 컴포넌트 인스턴스는 여전히 온전하게 동작하는 상태입니다.

  **이 훅은 서버 사이드 렌더링 중에는 호출되지 않습니다.**

<a id="api-options-lifecycle-unmounted"></a>

### unmounted
컴포넌트가 언마운트된 후 호출됩니다.

- **타입**

  ```ts
  interface ComponentOptions {
    unmounted?(this: ComponentPublicInstance): void
  }
  ```

- **세부사항**

  컴포넌트가 언마운트된 것으로 간주되는 시점은 다음과 같습니다:

  - 모든 자식 컴포넌트가 언마운트된 후

  - 모든 관련 반응형 효과(렌더 효과 및 `setup()` 중에 생성된 computed/감시자)가 중지된 후

  이 훅은 타이머, DOM 이벤트 리스너(listener), 서버 연결 등 수동으로 생성한 부수 효과를 정리하는 데 사용하세요.

  **이 훅은 서버 사이드 렌더링 중에는 호출되지 않습니다.**

<a id="api-options-lifecycle-errorcaptured"></a>

### errorCaptured
하위 컴포넌트에서 전파된 오류가 포착되었을 때 호출됩니다.

- **타입**

  ```ts
  interface ComponentOptions {
    errorCaptured?(
      this: ComponentPublicInstance,
      err: unknown,
      instance: ComponentPublicInstance | null,
      info: string
    ): boolean | void
  }
  ```

- **세부사항**

  다음과 같은 소스에서 오류를 포착할 수 있습니다:

  - 컴포넌트 렌더링
  - 이벤트 핸들러
  - 라이프사이클 훅
  - `setup()` 함수
  - 감시자
  - 커스텀 디렉티브(directive) 훅
  - 트랜지션(transition) 훅

  이 훅은 세 개의 인자를 받습니다: 오류, 오류를 발생시킨 컴포넌트 인스턴스, 오류 소스 타입을 지정하는 정보 문자열.

  **참고**
  프로덕션 환경에서는 세 번째 인자(`info`)가 전체 정보 문자열 대신 축약된 코드로 제공됩니다. 코드와 문자열의 매핑은 [프로덕션 오류 코드 참조](09_style_guide_examples_and_reference.md#error-reference-index-runtime-errors)에서 확인할 수 있습니다.


  `errorCaptured()`에서 컴포넌트 상태를 수정하여 사용자에게 오류 상태를 표시할 수 있습니다. 단, 오류 상태가 오류를 유발한 원래 콘텐츠를 렌더링하지 않도록 해야 합니다. 그렇지 않으면 컴포넌트가 무한 렌더 루프에 빠질 수 있습니다.

  이 훅에서 `false`를 반환하면 오류의 추가 전파를 중단할 수 있습니다. 아래의 오류 전파 세부사항을 참고하세요.

  **오류 전파 규칙**

  - 기본적으로, 애플리케이션 레벨의 [`app.config.errorHandler`](07_composition_and_reactivity_apis.md#api-application-app-config-errorhandler)가 정의되어 있다면 모든 오류는 여전히 해당 핸들러로 전달되므로, 이러한 오류를 한 곳에서 분석 서비스로 보고할 수 있습니다.

  - 컴포넌트의 상속 체인 또는 부모 체인에 여러 개의 `errorCaptured` 훅이 존재하는 경우, 동일한 오류에 대해 모두 하위에서 상위 순서로 호출됩니다. 이는 네이티브 DOM 이벤트의 버블링 메커니즘과 유사합니다.

  - `errorCaptured` 훅 자체에서 오류가 발생하면, 이 오류와 원래 포착된 오류 모두 `app.config.errorHandler`로 전달됩니다.

  - `errorCaptured` 훅에서 `false`를 반환하면 오류의 추가 전파를 막을 수 있습니다. 이는 "이 오류는 처리되었으니 무시해야 한다"는 의미입니다. 이 경우 추가적인 `errorCaptured` 훅이나 `app.config.errorHandler`는 이 오류에 대해 호출되지 않습니다.

  **오류 포착 주의사항**
  
  - 비동기 `setup()` 함수(최상위 `await` 사용)를 가진 컴포넌트에서는, `setup()`에서 오류가 발생하더라도 Vue가 **항상** 컴포넌트 템플릿(template)을 렌더링하려고 시도합니다. 이로 인해 렌더링 과정에서, 실패한 `setup()` 컨텍스트에 존재하지 않는 속성에 접근하게 되어 추가 오류가 발생할 수 있습니다. 이러한 컴포넌트에서 오류를 포착할 때는 실패한 비동기 `setup()`(항상 먼저 발생)과 실패한 렌더 프로세스 양쪽의 오류를 모두 처리할 준비가 되어 있어야 합니다.

  -  (SSR only) 부모 컴포넌트에서 `<Suspense>` 내부 깊은 곳의 오류가 발생한 자식 컴포넌트를 대체하면 SSR에서 하이드레이션(hydration) 불일치가 발생할 수 있습니다. 대신, 오류가 발생할 수 있는 로직을 자식의 `setup()`에서 별도의 함수로 분리하고, 부모 컴포넌트의 `setup()`에서 `try/catch`로 안전하게 실행하세요. 그러면 실제 자식 컴포넌트를 렌더링하기 전에 필요하다면 대체 처리를 할 수 있습니다.

<a id="api-options-lifecycle-rendertracked"></a>

### renderTracked  (개발 모드 전용)
컴포넌트의 렌더 효과에 의해 반응형 의존성이 추적될 때 호출됩니다.

**이 훅은 개발 모드에서만 호출되며, 서버 사이드 렌더링 중에는 호출되지 않습니다.**

- **타입**

  ```ts
  interface ComponentOptions {
    renderTracked?(this: ComponentPublicInstance, e: DebuggerEvent): void
  }

  type DebuggerEvent = {
    effect: ReactiveEffect
    target: object
    type: TrackOpTypes /* 'get' | 'has' | 'iterate' */
    key: any
  }
  ```

- **참고** [반응성 심층 가이드](06_reactivity_and_rendering_in_depth.md#guide-extras-reactivity-in-depth)

<a id="api-options-lifecycle-rendertriggered"></a>

### renderTriggered  (개발 모드 전용)
반응형 의존성이 컴포넌트의 렌더 효과를 다시 실행하도록 트리거할 때 호출됩니다.

**이 훅은 개발 모드에서만 호출되며, 서버 사이드 렌더링 중에는 호출되지 않습니다.**

- **타입**

  ```ts
  interface ComponentOptions {
    renderTriggered?(this: ComponentPublicInstance, e: DebuggerEvent): void
  }

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

<a id="api-options-lifecycle-activated"></a>

### activated
[`<KeepAlive>`](08_component_and_advanced_apis.md#api-built-in-components-keepalive)에 의해 캐시된 트리의 일부로 컴포넌트 인스턴스가 DOM에 삽입된 후 호출됩니다.

**이 훅은 서버 사이드 렌더링 중에는 호출되지 않습니다.**

- **타입**

  ```ts
  interface ComponentOptions {
    activated?(this: ComponentPublicInstance): void
  }
  ```

- **참고** [가이드 - 캐시된 인스턴스의 라이프사이클](04_built_ins_and_animation.md#guide-built-ins-keep-alive-lifecycle-of-cached-instance)

<a id="api-options-lifecycle-deactivated"></a>

### deactivated
[`<KeepAlive>`](08_component_and_advanced_apis.md#api-built-in-components-keepalive)에 의해 캐시된 트리의 일부로 컴포넌트 인스턴스가 DOM에서 제거된 후 호출됩니다.

**이 훅은 서버 사이드 렌더링 중에는 호출되지 않습니다.**

- **타입**

  ```ts
  interface ComponentOptions {
    deactivated?(this: ComponentPublicInstance): void
  }
  ```

- **참고** [가이드 - 캐시된 인스턴스의 라이프사이클](04_built_ins_and_animation.md#guide-built-ins-keep-alive-lifecycle-of-cached-instance)

<a id="api-options-lifecycle-serverprefetch"></a>

### serverPrefetch  (SSR only)
컴포넌트 인스턴스가 서버에서 렌더링되기 전에 해결되어야 하는 비동기 함수입니다.

- **타입**

  ```ts
  interface ComponentOptions {
    serverPrefetch?(this: ComponentPublicInstance): Promise<any>
  }
  ```

- **세부사항**

  이 훅이 Promise를 반환하면, 서버 렌더러는 Promise가 해결될 때까지 컴포넌트 렌더링을 대기합니다.

  이 훅은 서버 사이드 렌더링 중에만 호출되며, 서버 전용 데이터 패칭에 사용할 수 있습니다.

- **예시**

  ```js
  export default {
    data() {
      return {
        data: null
      }
    },
    async serverPrefetch() {
      // 컴포넌트가 초기 요청의 일부로 렌더링됨
      // 서버에서 데이터를 미리 패칭 (클라이언트보다 빠름)
      this.data = await fetchOnServer(/* ... */)
    },
    async mounted() {
      if (!this.data) {
        // 마운트 시 data가 null이면,
        // 컴포넌트가 클라이언트에서 동적으로 렌더링된 것임.
        // 대신 클라이언트에서 데이터를 패칭함.
        this.data = await fetchOnClient(/* ... */)
      }
    }
  }
  ```

- **참고** [서버 사이드 렌더링](05_scaling_typescript_and_best_practices.md#guide-scaling-up-ssr)

---

<a id="api-options-misc"></a>

<a id="api-options-misc-options-misc"></a>

## 옵션: 기타

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/options-misc.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/options-misc.md

<a id="api-options-misc-name"></a>

### name
컴포넌트(component)의 표시 이름을 명시적으로 선언합니다.

- **타입**

  ```ts
  interface ComponentOptions {
    name?: string
  }
  ```

- **세부 정보**

  컴포넌트의 이름은 다음과 같은 용도로 사용됩니다:

  - 컴포넌트 자신의 템플릿(template)에서 재귀적으로 자기 자신을 참조할 때
  - Vue DevTools의 컴포넌트 검사 트리에서 표시할 때
  - 경고 메시지의 컴포넌트 추적 정보에 표시할 때

  싱글 파일 컴포넌트(SFC)를 사용할 때, 컴포넌트는 이미 파일 이름에서 자신의 이름을 추론합니다. 예를 들어, `MyComponent.vue`라는 파일은 "MyComponent"라는 표시 이름을 자동으로 갖게 됩니다.

  또 다른 경우로, 컴포넌트가 [`app.component`](07_composition_and_reactivity_apis.md#api-application-app-component)로 전역 등록될 때, 전역 ID가 이름으로 자동 설정됩니다.

  `name` 옵션을 사용하면 추론된 이름을 덮어쓸 수 있으며, 이름을 추론할 수 없는 경우(예: 빌드 도구를 사용하지 않거나 인라인된 비-SFC 컴포넌트 등) 명시적으로 이름을 지정할 수 있습니다.

  `name`이 명시적으로 필요한 한 가지 경우가 있습니다: [`<KeepAlive>`](04_built_ins_and_animation.md#guide-built-ins-keep-alive)의 `include / exclude` props를 통해 캐시 가능한 컴포넌트와 매칭할 때입니다.

  **참고**
  3.2.34 버전부터 `<script setup>`을 사용하는 싱글 파일 컴포넌트는 파일 이름을 기반으로 `name` 옵션을 자동으로 추론하므로, `<KeepAlive>`와 함께 사용할 때도 이름을 수동으로 선언할 필요가 없습니다.


<a id="api-options-misc-inheritattrs"></a>

### inheritAttrs
기본 컴포넌트 속성 전달(fallthrough) 동작을 활성화할지 제어합니다.

- **타입**

  ```ts
  interface ComponentOptions {
    inheritAttrs?: boolean // 기본값: true
  }
  ```

- **세부 정보**

  부모가 전달한 속성 바인딩(binding)이 props로 인식되지 않으면 기본적으로 루트 엘리먼트에 전달(fallthrough)됩니다. 루트가 하나인 컴포넌트에서는 이 바인딩이 해당 엘리먼트의 일반 HTML 속성으로 적용됩니다.

  다른 엘리먼트나 컴포넌트를 감싸는 컴포넌트에서는 속성을 루트가 아닌 내부 요소에 적용해야 할 수 있습니다. 이때 `inheritAttrs: false`로 자동 전달을 끄고, `$attrs` 인스턴스 프로퍼티(property)에서 속성을 가져와 원하는 요소에 `v-bind`로 연결합니다. 아래 예제는 `<label>` 안의 `<input>`에 속성을 전달합니다.

- **예시**


**옵션 API**


  ```vue
  <script>
  export default {
    inheritAttrs: false,
    props: ['label', 'value'],
    emits: ['input']
  }
  </script>

  <template>
    <label>
      {{ label }}
      <input
        v-bind="$attrs"
        v-bind:value="value"
        v-on:input="$emit('input', $event.target.value)"
      />
    </label>
  </template>
  ```



**컴포지션 API**


  `<script setup>`을 사용하는 컴포넌트에서 이 옵션을 선언할 때는 [`defineOptions`](08_component_and_advanced_apis.md#api-sfc-script-setup-defineoptions) 매크로를 사용할 수 있습니다:

  ```vue
  <script setup>
  defineProps(['label', 'value'])
  defineEmits(['input'])
  defineOptions({
    inheritAttrs: false
  })
  </script>

  <template>
    <label>
      {{ label }}
      <input
        v-bind="$attrs"
        v-bind:value="value"
        v-on:input="$emit('input', $event.target.value)"
      />
    </label>
  </template>
  ```



- **관련 문서**

  - [속성 전달(Fallthrough Attributes)](03_components_and_reusability.md#guide-components-attrs)

**컴포지션 API**


  - [일반 `<script>`에서 `inheritAttrs` 사용하기](08_component_and_advanced_apis.md#api-sfc-script-setup-usage-alongside-normal-script)


<a id="api-options-misc-components"></a>

### components
컴포넌트 인스턴스(instance)에서 사용할 수 있도록 컴포넌트를 등록하는 객체입니다.

- **타입**

  ```ts
  interface ComponentOptions {
    components?: { [key: string]: Component }
  }
  ```

- **예시**

  ```js
  import Foo from './Foo.vue'
  import Bar from './Bar.vue'

  export default {
    components: {
      // 축약형
      Foo,
      // 다른 이름으로 등록
      RenamedBar: Bar
    }
  }
  ```

- **관련 문서** [컴포넌트 등록](03_components_and_reusability.md#guide-components-registration)

<a id="api-options-misc-directives"></a>

### directives
컴포넌트 인스턴스에서 사용할 수 있도록 디렉티브(directive)를 등록하는 객체입니다.

- **타입**

  ```ts
  interface ComponentOptions {
    directives?: { [key: string]: Directive }
  }
  ```

- **예시**

  ```js
  export default {
    directives: {
      // 템플릿에서 v-focus 사용 가능
      focus: {
        mounted(el) {
          el.focus()
        }
      }
    }
  }
  ```

  ```vue-html
  <input v-focus>
  ```

- **관련 문서** [커스텀 디렉티브](03_components_and_reusability.md#guide-reusability-custom-directives)

---

<a id="api-options-rendering"></a>

<a id="api-options-rendering-options-rendering"></a>

## 옵션: 렌더링

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/options-rendering.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/options-rendering.md

<a id="api-options-rendering-template"></a>

### template
컴포넌트(component)의 문자열 템플릿입니다.

- **타입**

  ```ts
  interface ComponentOptions {
    template?: string
  }
  ```

- **세부사항**

  `template` 옵션을 통해 제공된 템플릿(template)은 런타임에 즉석에서 컴파일됩니다. 이는 템플릿 컴파일러가 포함된 Vue 빌드를 사용할 때만 지원됩니다. 템플릿 컴파일러는 이름에 `runtime`이 포함된 Vue 빌드(예: `vue.runtime.esm-bundler.js`)에는 **포함되어 있지 않습니다**. 다양한 빌드에 대한 자세한 내용은 [dist 파일 가이드](https://github.com/vuejs/core/tree/main/packages/vue#which-dist-file-to-use)를 참고하세요.

  문자열이 `#`로 시작하면 `querySelector`의 선택자로 사용되며, 선택된 요소의 `innerHTML`이 템플릿 문자열이 됩니다. 이를 통해 네이티브 `<template>` 요소로 소스 템플릿을 작성할 수 있습니다.

  동일한 컴포넌트에 `render` 옵션도 존재하는 경우, `template`은 무시됩니다.

  애플리케이션의 루트 컴포넌트에 `template` 또는 `render` 옵션이 지정되지 않은 경우, Vue는 대신 마운트(mount)된 요소의 `innerHTML`을 템플릿으로 사용하려고 시도합니다.

  **보안 참고**
  신뢰할 수 있는 템플릿 소스만 사용하세요. 사용자로부터 제공된 콘텐츠를 템플릿으로 사용하지 마세요. 자세한 내용은 [보안 가이드](05_scaling_typescript_and_best_practices.md#guide-best-practices-security-rule-no-1-never-use-non-trusted-templates)를 참고하세요.


<a id="api-options-rendering-render"></a>

### render
컴포넌트의 가상 DOM 트리를 프로그래밍 방식으로 반환하는 함수입니다.

- **타입**

  ```ts
  interface ComponentOptions {
    render?(this: ComponentPublicInstance) => VNodeChild
  }

  type VNodeChild = VNodeChildAtom | VNodeArrayChildren

  type VNodeChildAtom =
    | VNode
    | string
    | number
    | boolean
    | null
    | undefined
    | void

  type VNodeArrayChildren = (VNodeArrayChildren | VNodeChildAtom)[]
  ```

- **세부사항**

  `render`는 문자열 템플릿의 대안으로, 컴포넌트의 렌더 출력을 선언할 때 JavaScript의 완전한 프로그래밍 기능을 활용할 수 있게 해줍니다.

  예를 들어 싱글 파일 컴포넌트의 사전 컴파일된 템플릿은 빌드 시점에 `render` 옵션으로 컴파일됩니다. 컴포넌트에 `render`와 `template`이 모두 존재하는 경우, `render`가 더 높은 우선순위를 가집니다.

- **관련 문서**
  - [렌더링(rendering) 메커니즘](06_reactivity_and_rendering_in_depth.md#guide-extras-rendering-mechanism)
  - [렌더 함수](06_reactivity_and_rendering_in_depth.md#guide-extras-render-function)

<a id="api-options-rendering-compileroptions"></a>

### compilerOptions
컴포넌트의 템플릿에 대한 런타임 컴파일러 옵션을 설정합니다.

- **타입**

  ```ts
  interface ComponentOptions {
    compilerOptions?: {
      isCustomElement?: (tag: string) => boolean
      whitespace?: 'condense' | 'preserve' // 기본값: 'condense'
      delimiters?: [string, string] // 기본값: ['{{', '}}']
      comments?: boolean // 기본값: false
    }
  }
  ```

- **세부사항**

  이 구성 옵션은 전체 빌드(즉, 브라우저에서 템플릿을 컴파일할 수 있는 독립형 `vue.js`)를 사용할 때만 적용됩니다. 앱 레벨의 [app.config.compilerOptions](07_composition_and_reactivity_apis.md#api-application-app-config-compileroptions)와 동일한 옵션을 지원하며, 현재 컴포넌트에 대해 더 높은 우선순위를 가집니다.

- **관련 문서** [app.config.compilerOptions](07_composition_and_reactivity_apis.md#api-application-app-config-compileroptions)

<a id="api-options-rendering-slots"></a>

### slots (TypeScript)
- 3.3+에서만 지원

렌더 함수에서 슬롯(slot)을 프로그래밍 방식으로 사용할 때 타입 추론을 돕기 위한 옵션입니다.

- **세부사항**

  이 옵션의 런타임 값은 사용되지 않습니다. 실제 타입은 `SlotsType` 타입 헬퍼를 사용한 타입 캐스팅을 통해 선언해야 합니다:

  ```ts
  import { SlotsType } from 'vue'

  defineComponent({
    slots: Object as SlotsType<{
      default: { foo: string; bar: number }
      item: { data: number }
    }>,
    setup(props, { slots }) {
      expectType<
        undefined | ((scope: { foo: string; bar: number }) => any)
      >(slots.default)
      expectType<undefined | ((scope: { data: number }) => any)>(
        slots.item
      )
    }
  })
  ```

---

<a id="api-options-state"></a>

<a id="api-options-state-options-state"></a>

## 옵션: 상태

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/options-state.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/options-state.md

<a id="api-options-state-data"></a>

### data
컴포넌트 인스턴스(instance)의 초기 반응형 상태를 반환하는 함수입니다.

- **타입**

  ```ts
  interface ComponentOptions {
    data?(
      this: ComponentPublicInstance,
      vm: ComponentPublicInstance
    ): object
  }
  ```

- **세부 정보**

  이 함수는 일반 JavaScript 객체를 반환해야 하며, 반환된 객체는 Vue가 반응형으로 만듭니다. 인스턴스가 생성된 후에는 반응형 데이터 객체에 `this.$data`로 접근할 수 있습니다. 컴포넌트 인스턴스는 데이터 객체에 있는 모든 속성을 프록시(proxy)하므로, `this.a`는 `this.$data.a`와 동일합니다.

  반환된 데이터 객체에는 모든 최상위 데이터 속성이 포함되어야 합니다. `this.$data`에 새로운 속성을 추가하는 것은 가능하지만, **권장되지 않습니다**. 속성에 넣을 값이 아직 준비되지 않았다면, `undefined`나 `null`과 같은 빈 값을 플레이스홀더로 포함시켜 Vue가 해당 속성이 존재함을 알 수 있도록 해야 합니다.

  `_` 또는 `$`로 시작하는 속성은 Vue의 내부 속성 및 API 메서드와 충돌할 수 있으므로 컴포넌트 인스턴스에서 **프록시되지 않습니다**. 이러한 속성은 `this.$data._property`와 같이 접근해야 합니다.

  자체 상태를 가진 브라우저 API 객체나 프로토타입 속성과 같은 객체를 반환하는 것은 **권장되지 않습니다**. 이상적으로, 반환되는 객체는 컴포넌트(component)의 상태만을 나타내는 일반 객체여야 합니다.

- **예시**

  ```js
  export default {
    data() {
      return { a: 1 }
    },
    created() {
      console.log(this.a) // 1
      console.log(this.$data) // { a: 1 }
    }
  }
  ```

  `data` 속성에 화살표 함수를 사용할 경우, `this`는 컴포넌트 인스턴스를 가리키지 않지만, 함수의 첫 번째 인자로 인스턴스에 접근할 수 있습니다:

  ```js
  data: (vm) => ({ a: vm.myProp })
  ```

- **관련 문서** [반응성 심층 안내](06_reactivity_and_rendering_in_depth.md#guide-extras-reactivity-in-depth)

<a id="api-options-state-props"></a>

### props
컴포넌트의 props를 선언합니다.

- **타입**

  ```ts
  interface ComponentOptions {
    props?: ArrayPropsOptions | ObjectPropsOptions
  }

  type ArrayPropsOptions = string[]

  type ObjectPropsOptions = { [key: string]: Prop }

  type Prop<T = any> = PropOptions<T> | PropType<T> | null

  interface PropOptions<T> {
    type?: PropType<T>
    required?: boolean
    default?: T | ((rawProps: object) => T)
    validator?: (value: unknown, rawProps: object) => boolean
  }

  type PropType<T> = { new (): T } | { new (): T }[]
  ```

  > 가독성을 위해 타입이 단순화되었습니다.

- **세부 정보**

  Vue에서는 모든 컴포넌트 props를 명시적으로 선언해야 합니다. 컴포넌트 props는 두 가지 형태로 선언할 수 있습니다:

  - 문자열 배열을 사용하는 간단한 형태
  - 각 속성 키가 prop의 이름이고 값이 prop의 타입(생성자 함수) 또는 고급 옵션인 객체 형태

  객체 기반 문법에서는 각 prop에 대해 다음과 같은 옵션을 추가로 정의할 수 있습니다:

  - **`type`**: 다음과 같은 기본 생성자 중 하나일 수 있습니다: `String`, `Number`, `Boolean`, `Array`, `Object`, `Date`, `Function`, `Symbol`, 임의의 커스텀 생성자 함수 또는 이들의 배열. 개발 모드에서 Vue는 prop의 값이 선언된 타입과 일치하는지 확인하고, 일치하지 않으면 경고를 표시합니다. 자세한 내용은 [Prop 유효성 검사](03_components_and_reusability.md#guide-components-props-prop-validation)를 참고하세요.

    또한, `Boolean` 타입의 prop은 개발 및 프로덕션 모두에서 값의 캐스팅 동작에 영향을 미칩니다. 자세한 내용은 [Boolean 캐스팅](03_components_and_reusability.md#guide-components-props-boolean-casting)을 참고하세요.

  - **`default`**: 부모에서 전달되지 않거나 값이 `undefined`인 경우 prop의 기본값을 지정합니다. 객체나 배열의 기본값은 팩토리 함수를 사용해 반환해야 합니다. 팩토리 함수는 인자로 원시 props 객체를 받습니다.

  - **`required`**: prop이 필수인지 정의합니다. 프로덕션 환경이 아닌 경우, 이 값이 참이고 prop이 전달되지 않으면 콘솔 경고가 발생합니다.

  - **`validator`**: prop 값과 props 객체를 인자로 받는 커스텀 유효성 검사 함수입니다. 개발 모드에서 이 함수가 거짓 값을 반환하면(즉, 유효성 검사가 실패하면) 콘솔 경고가 발생합니다.

- **예시**

  간단한 선언:

  ```js
  export default {
    props: ['size', 'myMessage']
  }
  ```

  유효성 검사가 포함된 객체 선언:

  ```js
  export default {
    props: {
      // 타입 검사
      height: Number,
      // 타입 검사 및 기타 유효성 검사
      age: {
        type: Number,
        default: 0,
        required: true,
        validator: (value) => {
          return value >= 0
        }
      }
    }
  }
  ```

- **관련 문서**
  - [가이드 - Props](03_components_and_reusability.md#guide-components-props)
  - [가이드 - 컴포넌트 Props 타입 지정](05_scaling_typescript_and_best_practices.md#guide-typescript-options-api-typing-component-props)  (TypeScript)

<a id="api-options-state-computed"></a>

### computed
컴포넌트 인스턴스에 노출할 계산된 속성(computed property)을 선언합니다.

- **타입**

  ```ts
  interface ComponentOptions {
    computed?: {
      [key: string]: ComputedGetter<any> | WritableComputedOptions<any>
    }
  }

  type ComputedGetter<T> = (
    this: ComponentPublicInstance,
    vm: ComponentPublicInstance,
    previous?: T
  ) => T

  type ComputedSetter<T> = (
    this: ComponentPublicInstance,
    value: T
  ) => void

  type WritableComputedOptions<T> = {
    get: ComputedGetter<T>
    set: ComputedSetter<T>
  }
  ```

- **세부 정보**

  이 옵션은 객체를 받으며, 키는 계산된 속성의 이름이고 값은 계산된 getter 또는 `get`과 `set` 메서드가 있는 객체(쓰기 가능한 계산된 속성)입니다.

  모든 getter와 setter는 `this` 컨텍스트가 자동으로 컴포넌트 인스턴스에 바인딩(binding)됩니다.

  계산된 속성에 화살표 함수를 사용할 경우, `this`는 컴포넌트 인스턴스를 가리키지 않지만, 함수의 첫 번째 인자로 인스턴스에 접근할 수 있습니다:

  ```js
  export default {
    computed: {
      aDouble: (vm) => vm.a * 2
    }
  }
  ```

- **예시**

  ```js
  export default {
    data() {
      return { a: 1 }
    },
    computed: {
      // 읽기 전용
      aDouble() {
        return this.a * 2
      },
      // 쓰기 가능
      aPlus: {
        get() {
          return this.a + 1
        },
        set(v) {
          this.a = v - 1
        }
      }
    },
    created() {
      console.log(this.aDouble) // => 2
      console.log(this.aPlus) // => 2

      this.aPlus = 3
      console.log(this.a) // => 2
      console.log(this.aDouble) // => 4
    }
  }
  ```

- **관련 문서**
  - [가이드 - 계산된 속성](02_essentials.md#guide-essentials-computed)
  - [가이드 - 계산된 속성 타입 지정](05_scaling_typescript_and_best_practices.md#guide-typescript-options-api-typing-computed-properties)  (TypeScript)

<a id="api-options-state-methods"></a>

### methods
컴포넌트 인스턴스에 혼합될 메서드를 선언합니다.

- **타입**

  ```ts
  interface ComponentOptions {
    methods?: {
      [key: string]: (this: ComponentPublicInstance, ...args: any[]) => any
    }
  }
  ```

- **세부 정보**

  선언된 메서드는 컴포넌트 인스턴스에서 직접 접근하거나 템플릿(template) 표현식에서 사용할 수 있습니다. 모든 메서드는 `this` 컨텍스트가 자동으로 컴포넌트 인스턴스에 바인딩되며, 메서드를 다른 곳으로 전달해도 이 바인딩은 유지됩니다.

  메서드를 선언할 때 화살표 함수 사용은 피해야 합니다. 화살표 함수는 `this`를 통해 컴포넌트 인스턴스에 접근할 수 없습니다.

- **예시**

  ```js
  export default {
    data() {
      return { a: 1 }
    },
    methods: {
      plus() {
        this.a++
      }
    },
    created() {
      this.plus()
      console.log(this.a) // => 2
    }
  }
  ```

- **관련 문서** [이벤트 처리](02_essentials.md#guide-essentials-event-handling)

<a id="api-options-state-watch"></a>

### watch
데이터 변경 시 호출될 watch 콜백(callback)을 선언합니다.

- **타입**

  ```ts
  interface ComponentOptions {
    watch?: {
      [key: string]: WatchOptionItem | WatchOptionItem[]
    }
  }

  type WatchOptionItem = string | WatchCallback | ObjectWatchOptionItem

  type WatchCallback<T> = (
    value: T,
    oldValue: T,
    onCleanup: (cleanupFn: () => void) => void
  ) => void

  type ObjectWatchOptionItem = {
    handler: WatchCallback | string
    immediate?: boolean // 기본값: false
    deep?: boolean // 기본값: false
    flush?: 'pre' | 'post' | 'sync' // 기본값: 'pre'
    onTrack?: (event: DebuggerEvent) => void
    onTrigger?: (event: DebuggerEvent) => void
  }
  ```

  > 가독성을 위해 타입이 단순화되었습니다.

- **세부 정보**

  `watch` 옵션은 객체를 받으며, 키는 감시할 반응형 컴포넌트 인스턴스 속성(예: `data`나 `computed`로 선언된 속성)이고, 값은 해당 콜백입니다. 콜백은 감시 대상의 새 값과 이전 값을 받습니다.

  루트 레벨 속성 외에도, 키는 점(.)으로 구분된 경로(`a.b.c`)일 수도 있습니다. 이 사용법은 **복잡한 표현식은 지원하지 않으며**, 점으로 구분된 경로만 사용할 수 있습니다. 복잡한 데이터 소스를 감시해야 한다면, 명령형 [`$watch()`](08_component_and_advanced_apis.md#api-component-instance-watch) API를 사용하세요.

  값은 (`methods`를 통해 선언된) 메서드 이름의 문자열이거나, 추가 옵션이 포함된 객체일 수도 있습니다. 객체 문법을 사용할 때 콜백은 `handler` 필드에 선언해야 합니다. 추가 옵션은 다음과 같습니다:

  - **`immediate`**: 감시자(watcher)가 생성될 때 즉시 콜백을 트리거합니다. 첫 호출 시 이전 값은 `undefined`입니다.
  - **`deep`**: 소스가 객체나 배열인 경우 깊은 탐색을 강제하여, 깊은 변경에도 콜백이 실행됩니다. [깊은 감시자](02_essentials.md#guide-essentials-watchers-deep-watchers) 참고.
  - **`flush`**: 콜백의 실행 타이밍을 조정합니다. [콜백 플러시 타이밍](02_essentials.md#guide-essentials-watchers-callback-flush-timing) 및 [`watchEffect()`](07_composition_and_reactivity_apis.md#api-reactivity-core-watcheffect) 참고.
  - **`onTrack / onTrigger`**: 감시자의 의존성 디버깅. [감시자 디버깅](06_reactivity_and_rendering_in_depth.md#guide-extras-reactivity-in-depth-watcher-debugging) 참고.

  watch 콜백을 선언할 때 화살표 함수 사용은 피해야 합니다. 화살표 함수는 `this`를 통해 컴포넌트 인스턴스에 접근할 수 없습니다.

- **예시**

  ```js
  export default {
    data() {
      return {
        a: 1,
        b: 2,
        c: {
          d: 4
        },
        e: 5,
        f: 6
      }
    },
    watch: {
      // 최상위 속성 감시
      a(val, oldVal) {
        console.log(`new: ${val}, old: ${oldVal}`)
      },
      // 문자열 메서드 이름
      b: 'someMethod',
      // 감시하는 객체의 모든 속성이 변경될 때마다 콜백이 호출됨 (중첩 깊이와 무관)
      c: {
        handler(val, oldVal) {
          console.log('c changed')
        },
        deep: true
      },
      // 단일 중첩 속성 감시:
      'c.d': function (val, oldVal) {
        // 작업 수행
      },
      // 감시 시작 직후 콜백이 즉시 호출됨
      e: {
        handler(val, oldVal) {
          console.log('e changed')
        },
        immediate: true
      },
      // 콜백 배열을 전달할 수 있으며, 순차적으로 호출됨
      f: [
        'handle1',
        function handle2(val, oldVal) {
          console.log('handle2 triggered')
        },
        {
          handler: function handle3(val, oldVal) {
            console.log('handle3 triggered')
          }
          /* ... */
        }
      ]
    },
    methods: {
      someMethod() {
        console.log('b changed')
      },
      handle1() {
        console.log('handle 1 triggered')
      }
    },
    created() {
      this.a = 3 // => new: 3, old: 1
    }
  }
  ```

- **관련 문서** [감시자](02_essentials.md#guide-essentials-watchers)

<a id="api-options-state-emits"></a>

### emits
컴포넌트에서 발생시키는 커스텀 이벤트를 선언합니다.

- **타입**

  ```ts
  interface ComponentOptions {
    emits?: ArrayEmitsOptions | ObjectEmitsOptions
  }

  type ArrayEmitsOptions = string[]

  type ObjectEmitsOptions = { [key: string]: EmitValidator | null }

  type EmitValidator = (...args: unknown[]) => boolean
  ```

- **세부 정보**

  발생시키는 이벤트는 두 가지 형태로 선언할 수 있습니다:

  - 문자열 배열을 사용하는 간단한 형태
  - 각 속성 키가 이벤트 이름이고 값이 `null` 또는 유효성 검사 함수인 객체 형태

  유효성 검사 함수는 컴포넌트의 `$emit` 호출에 전달된 추가 인자를 받습니다. 예를 들어, `this.$emit('foo', 1)`이 호출되면, `foo`에 대한 유효성 검사기는 인자 `1`을 받게 됩니다. 유효성 검사 함수는 이벤트 인자가 유효한지 여부를 나타내는 불리언 값을 반환해야 합니다.

  `emits` 옵션은 어떤 이벤트 리스너(listener)를 네이티브 DOM 이벤트 리스너가 아닌 컴포넌트 이벤트 리스너로 간주할지에 영향을 미칩니다. 선언된 이벤트의 리스너는 컴포넌트의 `$attrs` 객체에서 제거되어, 컴포넌트의 루트 엘리먼트로 전달되지 않습니다. 자세한 내용은 [속성 전달](03_components_and_reusability.md#guide-components-attrs)을 참고하세요.

- **예시**

  배열 문법:

  ```js
  export default {
    emits: ['check'],
    created() {
      this.$emit('check')
    }
  }
  ```

  객체 문법:

  ```js
  export default {
    emits: {
      // 유효성 검사 없음
      click: null,

      // 유효성 검사 포함
      submit: (payload) => {
        if (payload.email && payload.password) {
          return true
        } else {
          console.warn(`Invalid submit event payload!`)
          return false
        }
      }
    }
  }
  ```

- **관련 문서**
  - [가이드 - 속성 전달](03_components_and_reusability.md#guide-components-attrs)
  - [가이드 - 컴포넌트 Emits 타입 지정](05_scaling_typescript_and_best_practices.md#guide-typescript-options-api-typing-component-emits)  (TypeScript)

<a id="api-options-state-expose"></a>

### expose
템플릿 ref를 통해 부모가 컴포넌트 인스턴스에 접근할 때 노출할 공개 속성을 선언합니다.

- **타입**

  ```ts
  interface ComponentOptions {
    expose?: string[]
  }
  ```

- **세부 정보**

  기본적으로 컴포넌트 인스턴스는 `$parent`, `$root`, 또는 템플릿 ref를 통해 부모가 접근할 때 모든 인스턴스 속성을 노출합니다. 컴포넌트에 내부 상태나 메서드가 있을 경우, 강한 결합을 피하려면 이러한 노출이 바람직하지 않을 수 있습니다.

  `expose` 옵션은 속성 이름 문자열의 목록을 받습니다. `expose`를 사용하면, 명시적으로 나열된 속성만 컴포넌트의 공개 인스턴스에 노출됩니다.

  `expose`는 사용자 정의 속성에만 영향을 미치며, 내장 컴포넌트 인스턴스 속성은 필터링하지 않습니다.

- **예시**

  ```js
  export default {
    // `publicMethod`만 공개 인스턴스에서 사용할 수 있습니다
    expose: ['publicMethod'],
    methods: {
      publicMethod() {
        // ...
      },
      privateMethod() {
        // ...
      }
    }
  }
  ```

---

<a id="api-render-function"></a>

<a id="api-render-function-render-function-apis"></a>

## 렌더 함수 API

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/render-function.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/render-function.md

<a id="api-render-function-h"></a>

### h()
가상 DOM 노드(vnode)를 생성합니다.

- **타입**

  ```ts
  // 전체 시그니처
  function h(
    type: string | Component,
    props?: object | null,
    children?: Children | Slot | Slots
  ): VNode

  // props 생략
  function h(type: string | Component, children?: Children | Slot): VNode

  type Children = string | number | boolean | VNode | null | Children[]

  type Slot = () => Children

  type Slots = { [name: string]: Slot }
  ```

  > 타입은 가독성을 위해 단순화되었습니다.

- **세부사항**

  첫 번째 인자는 문자열(네이티브 엘리먼트용) 또는 Vue 컴포넌트(component) 정의가 될 수 있습니다. 두 번째 인자는 전달할 props이고, 세 번째 인자는 자식(children)입니다.

  컴포넌트 vnode를 생성할 때, 자식은 슬롯(slot) 함수로 전달되어야 합니다. 컴포넌트가 기본 슬롯만 받는다면 단일 슬롯 함수를 전달할 수 있습니다. 그렇지 않으면 슬롯을 슬롯 함수 객체로 전달해야 합니다.

  편의를 위해, children이 슬롯 객체가 아닐 때는 props 인자를 생략할 수 있습니다.

- **예시**

  네이티브 엘리먼트 생성:

  ```js
  import { h } from 'vue'

  // type을 제외한 모든 인자는 선택 사항입니다.
  h('div')
  h('div', { id: 'foo' })

  // props에서 속성과 프로퍼티 모두 사용할 수 있습니다.
  // Vue는 자동으로 올바른 할당 방식을 선택합니다.
  h('div', { class: 'bar', innerHTML: 'hello' })

  // class와 style은 템플릿에서처럼
  // 객체/배열 값을 지원합니다.
  h('div', { class: [foo, { bar }], style: { color: 'red' } })

  // 이벤트 리스너는 onXxx로 전달해야 합니다.
  h('div', { onClick: () => {} })

  // children은 문자열이 될 수 있습니다.
  h('div', { id: 'foo' }, 'hello')

  // props가 없을 때는 생략할 수 있습니다.
  h('div', 'hello')
  h('div', [h('span', 'hello')])

  // children 배열에는 vnode와 문자열이 혼합될 수 있습니다.
  h('div', ['hello', h('span', 'hello')])
  ```

  컴포넌트 생성:

  ```js
  import Foo from './Foo.vue'

  // props 전달
  h(Foo, {
    // some-prop="hello"와 동일
    someProp: 'hello',
    // @update="() => {}"와 동일
    onUpdate: () => {}
  })

  // 단일 기본 슬롯 전달
  h(Foo, () => 'default slot')

  // 명명된 슬롯 전달
  // 슬롯 객체가 props로 처리되지 않도록
  // `null`이 필요합니다.
  h(MyComponent, null, {
    default: () => 'default slot',
    foo: () => h('div', 'foo'),
    bar: () => [h('span', 'one'), h('span', 'two')]
  })
  ```

- **참고** [가이드 - 렌더 함수 - VNode 생성](06_reactivity_and_rendering_in_depth.md#guide-extras-render-function-creating-vnodes)

<a id="api-render-function-mergeprops"></a>

### mergeProps()
특정 props에 대해 특별한 처리를 하며 여러 props 객체를 병합합니다.

- **타입**

  ```ts
  function mergeProps(...args: object[]): object
  ```

- **세부사항**

  `mergeProps()`는 다음과 같은 props에 대해 특별한 처리를 하며 여러 props 객체를 병합할 수 있습니다:

  - `class`
  - `style`
  - `onXxx` 이벤트 리스너(listener) - 동일한 이름의 여러 리스너는 배열로 병합됩니다.

  병합 동작이 필요 없고 단순히 덮어쓰기를 원한다면, 네이티브 객체 스프레드를 대신 사용할 수 있습니다.

- **예시**

  ```js
  import { mergeProps } from 'vue'

  const one = {
    class: 'foo',
    onClick: handlerA
  }

  const two = {
    class: { bar: true },
    onClick: handlerB
  }

  const merged = mergeProps(one, two)
  /**
   {
     class: 'foo bar',
     onClick: [handlerA, handlerB]
   }
   */
  ```

<a id="api-render-function-clonevnode"></a>

### cloneVNode()
vnode를 복제합니다.

- **타입**

  ```ts
  function cloneVNode(vnode: VNode, extraProps?: object): VNode
  ```

- **세부사항**

  복제된 vnode를 반환하며, 원본과 병합할 추가 props를 함께 지정할 수 있습니다.

  vnode는 한 번 생성되면 불변으로 간주되어야 하며, 기존 vnode의 props를 변경해서는 안 됩니다. 대신, 다른 props나 추가 props를 사용해 복제해야 합니다.

  vnode에는 특수한 내부 속성이 있어 객체 스프레드만으로 복제할 수 없습니다. `cloneVNode()`가 복제에 필요한 내부 로직 대부분을 처리합니다.

- **예시**

  ```js
  import { h, cloneVNode } from 'vue'

  const original = h('div')
  const cloned = cloneVNode(original, { id: 'foo' })
  ```

<a id="api-render-function-isvnode"></a>

### isVNode()
값이 vnode인지 확인합니다.

- **타입**

  ```ts
  function isVNode(value: unknown): boolean
  ```

<a id="api-render-function-resolvecomponent"></a>

### resolveComponent()
등록된 컴포넌트를 이름을 사용해 수동으로 해석할 때 사용합니다.

- **타입**

  ```ts
  function resolveComponent(name: string): Component | string
  ```

- **세부사항**

  **참고: 컴포넌트를 직접 import할 수 있다면 이 함수를 사용할 필요가 없습니다.**

  `resolveComponent()`는 올바른 컴포넌트 컨텍스트에서 해석하기 위해 (컴포지션 API:  `setup()` 또는) 렌더 함수 내부에서 호출되어야 합니다.

  컴포넌트를 찾을 수 없으면 런타임 경고가 발생하고, 이름 문자열이 반환됩니다.

- **예시**


**컴포지션 API**


  ```js
  import { h, resolveComponent } from 'vue'

  export default {
    setup() {
      const ButtonCounter = resolveComponent('ButtonCounter')

      return () => {
        return h(ButtonCounter)
      }
    }
  }
  ```



**옵션 API**


  ```js
  import { h, resolveComponent } from 'vue'

  export default {
    render() {
      const ButtonCounter = resolveComponent('ButtonCounter')
      return h(ButtonCounter)
    }
  }
  ```



- **참고** [가이드 - 렌더 함수 - 컴포넌트](06_reactivity_and_rendering_in_depth.md#guide-extras-render-function-components)

<a id="api-render-function-resolvedirective"></a>

### resolveDirective()
등록된 디렉티브(directive)를 이름을 사용해 수동으로 해석할 때 사용합니다.

- **타입**

  ```ts
  function resolveDirective(name: string): Directive | undefined
  ```

- **세부사항**

  **참고: 디렉티브를 직접 import할 수 있다면 이 함수를 사용할 필요가 없습니다.**

  `resolveDirective()`는 올바른 컴포넌트 컨텍스트에서 해석하기 위해 (컴포지션 API:  `setup()` 또는) 렌더 함수 내부에서 호출되어야 합니다.

  디렉티브를 찾을 수 없으면 런타임 경고가 발생하고, 함수는 `undefined`를 반환합니다.

- **참고** [가이드 - 렌더 함수 - 커스텀 디렉티브](06_reactivity_and_rendering_in_depth.md#guide-extras-render-function-custom-directives)

<a id="api-render-function-withdirectives"></a>

### withDirectives()
vnode에 커스텀 디렉티브를 추가할 때 사용합니다.

- **타입**

  ```ts
  function withDirectives(
    vnode: VNode,
    directives: DirectiveArguments
  ): VNode

  // [Directive, value, argument, modifiers]
  type DirectiveArguments = Array<
    | [Directive]
    | [Directive, any]
    | [Directive, any, string]
    | [Directive, any, string, DirectiveModifiers]
  >
  ```

- **세부사항**

  기존 vnode를 커스텀 디렉티브로 감쌉니다. 두 번째 인자는 커스텀 디렉티브의 배열입니다. 각 커스텀 디렉티브는 `[Directive, value, argument, modifiers]` 형태의 배열로 표현됩니다. 배열의 마지막 요소들은 필요하지 않으면 생략할 수 있습니다.

- **예시**

  ```js
  import { h, withDirectives } from 'vue'

  // 커스텀 디렉티브
  const pin = {
    mounted() {
      /* ... */
    },
    updated() {
      /* ... */
    }
  }

  // <div v-pin:top.animate="200"></div>
  const vnode = withDirectives(h('div'), [
    [pin, 200, 'top', { animate: true }]
  ])
  ```

- **참고** [가이드 - 렌더 함수 - 커스텀 디렉티브](06_reactivity_and_rendering_in_depth.md#guide-extras-render-function-custom-directives)

<a id="api-render-function-withmodifiers"></a>

### withModifiers()
이벤트 핸들러 함수에 내장 [`v-on` 수식어(modifiers)](02_essentials.md#guide-essentials-event-handling-event-modifiers)를 추가할 때 사용합니다.

- **타입**

  ```ts
  function withModifiers(fn: Function, modifiers: ModifierGuardsKeys[]): Function
  ```

- **예시**

  ```js
  import { h, withModifiers } from 'vue'

  const vnode = h('button', {
    // v-on:click.stop.prevent와 동일
    onClick: withModifiers(() => {
      // ...
    }, ['stop', 'prevent'])
  })
  ```

- **참고** [가이드 - 렌더 함수 - 이벤트 수식어](06_reactivity_and_rendering_in_depth.md#guide-extras-render-function-event-modifiers)

---

<a id="api-sfc-css-features"></a>

<a id="api-sfc-css-features-sfc-css-features"></a>

## SFC CSS 기능

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/sfc-css-features.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/sfc-css-features.md

<a id="api-sfc-css-features-scoped-css"></a>

### Scoped CSS
`<style>` 태그에 `scoped` 속성이 있으면, 해당 CSS는 현재 컴포넌트(component)의 요소에만 적용됩니다. 이는 Shadow DOM에서 볼 수 있는 스타일 캡슐화와 유사합니다. 몇 가지 주의사항이 있지만, 별도의 폴리필(polyfill)이 필요하지 않습니다. 이 기능은 PostCSS를 사용해 다음 코드를 변환하는 방식으로 구현됩니다:

```vue
<style scoped>
.example {
  color: red;
}
</style>

<template>
  <div class="example">hi</div>
</template>
```

위 코드는 다음과 같이 변환됩니다:

```vue
<style>
.example[data-v-f3f3eg9] {
  color: red;
}
</style>

<template>
  <div class="example" data-v-f3f3eg9>hi</div>
</template>
```

<a id="api-sfc-css-features-child-component-root-elements"></a>

#### 자식 컴포넌트 루트 요소
`scoped`를 사용하면, 부모 컴포넌트의 스타일이 자식 컴포넌트로 누출되지 않습니다. 하지만, 자식 컴포넌트의 루트 노드는 부모의 scoped CSS와 자식의 scoped CSS 모두의 영향을 받습니다. 이는 부모가 레이아웃 목적으로 자식의 루트 요소를 스타일링할 수 있도록 의도된 동작입니다.

<a id="api-sfc-css-features-deep-selectors"></a>

#### 딥 셀렉터
`scoped` 스타일에서 셀렉터가 "딥"하게, 즉 자식 컴포넌트에까지 영향을 미치게 하려면 `:deep()` 의사 클래스(pseudo-class)를 사용할 수 있습니다:

```vue
<style scoped>
.a :deep(.b) {
  /* ... */
}
</style>
```

위 코드는 다음과 같이 컴파일됩니다:

```css
.a[data-v-f3f3eg9] .b {
  /* ... */
}
```

**참고**
`v-html`로 생성된 DOM 콘텐츠는 scoped 스타일의 영향을 받지 않지만, 딥 셀렉터를 사용하여 여전히 스타일링할 수 있습니다.


<a id="api-sfc-css-features-slotted-selectors"></a>

#### 슬롯 셀렉터
기본적으로, scoped 스타일은 `<slot/>`으로 렌더링(rendering)된 콘텐츠에 영향을 주지 않습니다. 슬롯 콘텐츠는 그것을 전달한 부모 컴포넌트의 소유로 간주되기 때문입니다. 슬롯(slot) 콘텐츠를 명시적으로 타겟팅하려면 `:slotted` 의사 클래스를 사용하세요:

```vue
<style scoped>
:slotted(div) {
  color: red;
}
</style>
```

<a id="api-sfc-css-features-global-selectors"></a>

#### 글로벌 셀렉터
단일 규칙만 전역적으로 적용하고 싶다면, 별도의 `<style>`을 만들지 않고 `:global` 의사 클래스를 사용할 수 있습니다(아래 참고):

```vue
<style scoped>
:global(.red) {
  color: red;
}
</style>
```

<a id="api-sfc-css-features-mixing-local-and-global-styles"></a>

#### 로컬 및 글로벌 스타일 혼합
동일한 컴포넌트 내에서 scoped 스타일과 non-scoped 스타일을 모두 포함할 수도 있습니다:

```vue
<style>
/* 글로벌 스타일 */
</style>

<style scoped>
/* 로컬 스타일 */
</style>
```

<a id="api-sfc-css-features-scoped-style-tips"></a>

#### Scoped 스타일 팁
- **Scoped 스타일이 클래스의 필요성을 없애지는 않습니다.** 브라우저가 다양한 CSS 셀렉터를 렌더링하는 방식 때문에, `p { color: red }`와 같은 셀렉터는 scoped 상태(즉, 속성 셀렉터와 결합된 상태)에서 훨씬 느려집니다. 대신 `.example { color: red }`와 같이 클래스나 id를 사용하면 성능 저하를 사실상 없앨 수 있습니다.

- **재귀 컴포넌트에서 자손 셀렉터를 사용할 때 주의하세요!** `.a .b`와 같은 셀렉터가 있는 CSS 규칙에서, `.a`에 해당하는 요소가 재귀 자식 컴포넌트를 포함하면, 해당 자식 컴포넌트 내의 모든 `.b`가 이 규칙에 의해 매칭됩니다.

<a id="api-sfc-css-features-css-modules"></a>

### CSS 모듈
`<style module>` 태그는 [CSS Modules](https://github.com/css-modules/css-modules)로 컴파일되며, 결과 CSS 클래스가 `$style` 키 아래의 객체로 컴포넌트에 노출됩니다:

```vue
<template>
  <p :class="$style.red">This should be red</p>
</template>

<style module>
.red {
  color: red;
}
</style>
```

결과 클래스는 충돌을 방지하기 위해 해시 처리되어, CSS가 현재 컴포넌트에만 적용되는 것과 동일한 효과를 얻습니다.

[CSS Modules 명세](https://github.com/css-modules/css-modules)에서 [글로벌 예외](https://github.com/css-modules/css-modules/blob/master/docs/composition.md#exceptions) 및 [컴포지션](https://github.com/css-modules/css-modules/blob/master/docs/composition.md#composition) 등 자세한 내용을 참고하세요.

<a id="api-sfc-css-features-custom-inject-name"></a>

#### 커스텀 주입 이름
`module` 속성에 값을 지정하여 주입된 클래스 객체의 프로퍼티(property) 키를 커스터마이즈할 수 있습니다:

```vue
<template>
  <p :class="classes.red">red</p>
</template>

<style module="classes">
.red {
  color: red;
}
</style>
```

<a id="api-sfc-css-features-usage-with-composition-api"></a>

#### Composition API와 함께 사용하기
주입된 클래스는 `setup()` 및 `<script setup>`에서 `useCssModule` API를 통해 접근할 수 있습니다. 커스텀 주입 이름이 있는 `<style module>` 블록의 경우, `useCssModule`의 첫 번째 인자로 일치하는 `module` 속성 값을 전달합니다:

```js
import { useCssModule } from 'vue'

// setup() 범위 내에서...
// 기본값, <style module>의 클래스를 반환
useCssModule()

// 이름 지정, <style module="classes">의 클래스를 반환
useCssModule('classes')
```

- **예시**

```vue
<script setup lang="ts">
import { useCssModule } from 'vue'

const classes = useCssModule()
</script>

<template>
  <p :class="classes.red">red</p>
</template>

<style module>
.red {
  color: red;
}
</style>
```

<a id="api-sfc-css-features-v-bind-in-css"></a>

### CSS에서 `v-bind()`
SFC `<style>` 태그에서는 `v-bind` CSS 함수를 사용하여 CSS 값을 컴포넌트의 동적 상태에 연결할 수 있습니다:

```vue
<template>
  <div class="text">hello</div>
</template>

<script>
export default {
  data() {
    return {
      color: 'red'
    }
  }
}
</script>

<style>
.text {
  color: v-bind(color);
}
</style>
```

이 문법은 [`<script setup>`](08_component_and_advanced_apis.md#api-sfc-script-setup)에서도 동작하며, JavaScript 표현식도 지원합니다(따옴표로 감싸야 함):

```vue
<script setup>
import { ref } from 'vue'
const theme = ref({
    color: 'red',
})
</script>

<template>
  <p>hello</p>
</template>

<style scoped>
p {
  color: v-bind('theme.color');
}
</style>
```

실제 값은 해시 처리된 CSS 커스텀 프로퍼티로 컴파일되므로 CSS는 여전히 정적입니다. 커스텀 프로퍼티는 인라인 스타일을 통해 컴포넌트의 루트 요소에 적용되며, 소스 값이 변경되면 반응적으로 업데이트됩니다.

---

<a id="api-sfc-script-setup"></a>

<a id="api-sfc-script-setup-script-setup"></a>

## \<script setup>

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/sfc-script-setup.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/sfc-script-setup.md

`<script setup>`은 싱글 파일 컴포넌트(SFC) 내에서 컴포지션 API를 사용할 때의 컴파일 타임 문법 설탕입니다. SFC와 컴포지션 API를 모두 사용하는 경우 권장되는 문법입니다. 일반 `<script>` 문법에 비해 여러 가지 장점이 있습니다:

- 보일러플레이트(boilerplate)가 적고 더 간결한 코드
- 순수 TypeScript로 props와 emit 이벤트 선언 가능
- 더 나은 런타임 성능(템플릿이 중간 프록시(proxy) 없이 동일한 스코프의 렌더 함수로 컴파일됨)
- 더 나은 IDE 타입 추론 성능(코드에서 타입을 추출하는 언어 서버의 작업량 감소)

<a id="api-sfc-script-setup-basic-syntax"></a>

### 기본 문법
이 문법을 사용하려면 `<script>` 블록에 `setup` 속성을 추가하세요:

```vue
<script setup>
console.log('hello script setup')
</script>
```

내부 코드는 컴포넌트(component)의 `setup()` 함수의 내용으로 컴파일됩니다. 즉, 일반 `<script>`와 달리, `<script setup>` 내부의 코드는 **컴포넌트 인스턴스(instance)가 생성될 때마다 실행**됩니다(일반 `<script>`는 컴포넌트가 처음 import될 때 한 번만 실행됨).

<a id="api-sfc-script-setup-top-level-bindings-are-exposed-to-template"></a>

#### 최상위 바인딩은 템플릿에 노출됨
`<script setup>`을 사용할 때, `<script setup>` 내부에 선언된 모든 최상위 바인딩(변수, 함수 선언, import 등)은 템플릿(template)에서 직접 사용할 수 있습니다:

```vue
<script setup>
// 변수
const msg = 'Hello!'

// 함수
function log() {
  console.log(msg)
}
</script>

<template>
  <button @click="log">{{ msg }}</button>
</template>
```

import도 동일하게 노출됩니다. 즉, import한 헬퍼 함수를 `methods` 옵션을 통해 노출하지 않고도 템플릿 표현식에서 직접 사용할 수 있습니다:

```vue
<script setup>
import { capitalize } from './helpers'
</script>

<template>
  <div>{{ capitalize('hello') }}</div>
</template>
```

<a id="api-sfc-script-setup-reactivity"></a>

### 반응성(reactivity)
반응형 상태는 [반응성 API](07_composition_and_reactivity_apis.md#api-reactivity-core)를 사용해 명시적으로 생성해야 합니다. `setup()` 함수에서 반환된 값과 마찬가지로, ref는 템플릿에서 참조할 때 자동으로 언래핑됩니다:

```vue
<script setup>
import { ref } from 'vue'

const count = ref(0)
</script>

<template>
  <button @click="count++">{{ count }}</button>
</template>
```

<a id="api-sfc-script-setup-using-components"></a>

### 컴포넌트 사용하기
`<script setup>`의 스코프 내 값은 커스텀 컴포넌트 태그 이름으로도 직접 사용할 수 있습니다:

```vue
<script setup>
import MyComponent from './MyComponent.vue'
</script>

<template>
  <MyComponent />
</template>
```

`MyComponent`를 변수로 참조한다고 생각하면 됩니다. JSX를 사용해본 적이 있다면 비슷한 개념입니다. 케밥 케이스의 `<my-component>`도 템플릿에서 동작하지만, 일관성을 위해 PascalCase 컴포넌트 태그 사용을 강력히 권장합니다. PascalCase를 사용하면 네이티브 커스텀 엘리먼트와 구분하는 데에도 도움이 됩니다.

<a id="api-sfc-script-setup-dynamic-components"></a>

#### 동적 컴포넌트
컴포넌트가 문자열 키로 등록되는 것이 아니라 변수로 참조되기 때문에, `<script setup>` 내부에서 동적 컴포넌트를 사용할 때는 동적 `:is` 바인딩(binding)을 사용해야 합니다:

```vue
<script setup>
import Foo from './Foo.vue'
import Bar from './Bar.vue'
</script>

<template>
  <component :is="Foo" />
  <component :is="someCondition ? Foo : Bar" />
</template>
```

이처럼 컴포넌트도 변수이므로 삼항 연산자로 선택할 수 있습니다.

<a id="api-sfc-script-setup-recursive-components"></a>

#### 재귀 컴포넌트
SFC는 파일 이름을 통해 암묵적으로 자신을 참조할 수 있습니다. 예를 들어, `FooBar.vue`라는 파일은 템플릿에서 `<FooBar/>`로 자신을 참조할 수 있습니다.

이 기능은 import된 컴포넌트보다 우선순위가 낮습니다. 컴포넌트의 추론된 이름과 충돌하는 이름의 import가 있다면, import에 별칭을 지정할 수 있습니다:

```js
import { FooBar as FooBarChild } from './components'
```

<a id="api-sfc-script-setup-namespaced-components"></a>

#### 네임스페이스 컴포넌트
`<Foo.Bar>`처럼 점이 포함된 컴포넌트 태그를 사용해 객체 속성에 중첩된 컴포넌트를 참조할 수 있습니다. 이는 하나의 파일에서 여러 컴포넌트를 import할 때 유용합니다:

```vue
<script setup>
import * as Form from './form-components'
</script>

<template>
  <Form.Input>
    <Form.Label>label</Form.Label>
  </Form.Input>
</template>
```

<a id="api-sfc-script-setup-using-custom-directives"></a>

### 커스텀 디렉티브 사용하기
전역 등록된 커스텀 디렉티브(directive)는 평소처럼 동작합니다. 로컬 커스텀 디렉티브는 `<script setup>`에서 명시적으로 등록할 필요가 없지만, `vNameOfDirective`라는 네이밍 규칙을 따라야 합니다:

```vue
<script setup>
const vMyDirective = {
  beforeMount: (el) => {
    // 엘리먼트로 무언가를 수행
  }
}
</script>
<template>
  <h1 v-my-directive>이것은 제목입니다</h1>
</template>
```

다른 곳에서 디렉티브를 import하는 경우, 필요한 네이밍 규칙에 맞게 이름을 변경할 수 있습니다:

```vue
<script setup>
import { myDirective as vMyDirective } from './MyDirective.js'
</script>
```

<a id="api-sfc-script-setup-defineprops-defineemits"></a>

### defineProps() & defineEmits()
`props`와 `emits` 같은 옵션을 완전한 타입 추론을 지원받으면서 선언하려면, `<script setup>` 내부에서 자동으로 사용할 수 있는 `defineProps`와 `defineEmits` API를 사용할 수 있습니다:

```vue
<script setup>
const props = defineProps({
  foo: String
})

const emit = defineEmits(['change', 'delete'])
// setup 코드
</script>
```

- `defineProps`와 `defineEmits`는 **컴파일러 매크로**로, `<script setup>` 내부에서만 사용할 수 있습니다. import할 필요가 없으며, `<script setup>`이 처리될 때 컴파일 과정에서 제거됩니다.

- `defineProps`는 `props` 옵션과 동일한 값을, `defineEmits`는 `emits` 옵션과 동일한 값을 받습니다.

- `defineProps`와 `defineEmits`는 전달된 옵션을 기반으로 올바른 타입 추론을 제공합니다.

- `defineProps`와 `defineEmits`에 전달된 옵션은 setup 바깥의 모듈 스코프로 호이스팅(hoisting)됩니다. 따라서 옵션은 setup 스코프에서 선언된 로컬 변수를 참조할 수 없습니다. 그렇게 하면 컴파일 에러가 발생합니다. 하지만 import된 바인딩은 모듈 스코프에 있으므로 참조할 수 있습니다.

<a id="api-sfc-script-setup-type-only-props-emit-declarations"></a>

#### 타입 전용 props/emit 선언 (TypeScript)
props와 emits는 `defineProps` 또는 `defineEmits`에 리터럴 타입 인자를 전달하여 순수 타입 문법으로도 선언할 수 있습니다:

```ts
const props = defineProps<{
  foo: string
  bar?: number
}>()

const emit = defineEmits<{
  (e: 'change', id: number): void
  (e: 'update', value: string): void
}>()

// 3.3+: 더 간결한 대안 문법
const emit = defineEmits<{
  change: [id: number] // 명명된 튜플 문법
  update: [value: string]
}>()
```

- `defineProps`와 `defineEmits`는 런타임 선언과 타입 선언 중 하나만 사용할 수 있습니다. 둘을 동시에 사용하면 컴파일 에러가 발생합니다.

- 타입 선언을 사용할 때, 정적 분석을 통해 동등한 런타임 선언이 자동으로 생성되어 중복 선언 없이도 올바른 런타임 동작을 보장합니다.

  - 개발 모드에서는 컴파일러가 타입에서 해당 런타임 유효성 검사를 추론하려고 시도합니다. 예를 들어 위 예시에서 `foo: string` 타입은 `foo: String`으로 추론됩니다. TypeScript가 peer dependency로 설치되어 있다면, import된 타입도 해석됩니다.

  - 프로덕션 모드에서는 번들 크기를 줄이기 위해 배열 형식 선언이 생성됩니다(위 예시에서 props는 `['foo', 'bar']`로 컴파일됨).

- 3.2 이하 버전에서는 `defineProps()`의 제네릭 타입 파라미터가 타입 리터럴 또는 로컬 인터페이스 참조로 제한되었습니다.

  이 제한은 3.3에서 해결되었습니다. 최신 Vue 버전은 타입 파라미터 위치에서 import된 타입과 제한된 복합 타입 참조를 지원합니다. 하지만 타입에서 런타임으로의 변환이 여전히 AST 기반이기 때문에, 조건부 타입 등 실제 타입 분석이 필요한 일부 복합 타입은 지원되지 않습니다. 단일 prop의 타입으로 조건부 타입을 사용할 수는 있지만, 전체 props 객체에는 사용할 수 없습니다.

<a id="api-sfc-script-setup-reactive-props-destructure"></a>

#### 반응형 props 구조 분해  (3.5+)
Vue 3.5 이상에서는 `defineProps`의 반환값에서 구조 분해된 변수들이 반응형이 됩니다. Vue의 컴파일러는 동일한 `<script setup>` 블록 내에서 `defineProps`로 구조 분해된 변수에 접근할 때 자동으로 `props.`를 앞에 붙입니다:

```ts
const { foo } = defineProps(['foo'])

watchEffect(() => {
  // 3.5 이전에는 한 번만 실행됨
  // 3.5+에서는 "foo" prop이 변경될 때마다 재실행됨
  console.log(foo)
})
```

위 코드는 다음과 동등한 코드로 컴파일됩니다:

```js {5}
const props = defineProps(['foo'])

watchEffect(() => {
  // 컴파일러가 `foo`를 `props.foo`로 변환
  console.log(props.foo)
})
```

또한, JavaScript의 기본값 문법을 사용해 props의 기본값을 선언할 수 있습니다. 타입 기반 props 선언을 사용할 때 특히 유용합니다:

```ts
interface Props {
  msg?: string
  labels?: string[]
}

const { msg = 'hello', labels = ['one', 'two'] } = defineProps<Props>()
```

<a id="api-sfc-script-setup-default-props-values-when-using-type-declaration"></a>

#### 타입 선언 사용 시 props 기본값  (TypeScript)
3.5 이상에서는 반응형 props 구조 분해를 사용할 때 기본값을 자연스럽게 선언할 수 있습니다. 하지만 3.4 이하에서는 반응형 props 구조 분해가 기본적으로 활성화되어 있지 않습니다. 타입 기반 선언으로 props 기본값을 선언하려면 `withDefaults` 컴파일러 매크로가 필요합니다:

```ts
interface Props {
  msg?: string
  labels?: string[]
}

const props = withDefaults(defineProps<Props>(), {
  msg: 'hello',
  labels: () => ['one', 'two']
})
```

이 코드는 동등한 런타임 props `default` 옵션으로 컴파일됩니다. 또한, `withDefaults` 헬퍼는 기본값에 대한 타입 검사를 제공하고, 기본값이 선언된 속성에 대해 반환된 `props` 타입에서 선택적 플래그를 제거합니다.

**안내**
`withDefaults`에서 배열이나 객체를 기본값으로 지정할 때는 함수로 감싸야 합니다. 그래야 각 컴포넌트 인스턴스가 별도의 기본값을 갖고, 한 인스턴스의 수정이 다른 인스턴스에 영향을 주지 않습니다. 구조 분해로 기본값을 지정할 때는 함수로 감쌀 **필요가 없습니다**.


<a id="api-sfc-script-setup-definemodel"></a>

### defineModel()
- 3.4+에서만 사용 가능

이 매크로는 부모 컴포넌트에서 `v-model`로 사용할 수 있는 양방향 바인딩 prop을 선언합니다. 예시 사용법은 [컴포넌트 `v-model`](03_components_and_reusability.md#guide-components-v-model) 가이드에서도 다룹니다.

내부적으로 이 매크로는 모델 prop과 해당 값 업데이트 이벤트를 선언합니다. 첫 번째 인자가 리터럴 문자열이면 prop 이름으로 사용되고, 그렇지 않으면 prop 이름은 기본값 `"modelValue"`가 됩니다. 두 경우 모두 prop 옵션과 모델 ref의 값 변환 옵션을 포함하는 추가 객체를 전달할 수 있습니다.

```js
// "modelValue" prop을 선언, 부모에서 v-model로 사용
const model = defineModel()
// 또는: 옵션이 있는 "modelValue" prop 선언
const model = defineModel({ type: String })

// 변경 시 "update:modelValue"를 emit
model.value = 'hello'

// "count" prop을 선언, 부모에서 v-model:count로 사용
const count = defineModel('count')
// 또는: 옵션이 있는 "count" prop 선언
const count = defineModel('count', { type: Number, default: 0 })

function inc() {
  // 변경 시 "update:count"를 emit
  count.value++
}
```

**주의**
`defineModel` prop에 `default` 값을 지정하고, 부모 컴포넌트에서 이 prop에 값을 제공하지 않으면 부모와 자식 컴포넌트 간 동기화가 깨질 수 있습니다. 아래 예시에서 부모의 `myRef`는 undefined이지만, 자식의 `model`은 1입니다:

```vue [Child.vue]
<script setup>
const model = defineModel({ default: 1 })
</script>
```

```vue [Parent.vue]
<script setup>
const myRef = ref()
</script>

<template>
  <Child v-model="myRef"></Child>
</template>
```

배열이나 객체를 `defineModel`의 기본값으로 지정할 때도 앞의 `withDefaults` 예제처럼 함수로 감쌉니다. 각 인스턴스가 같은 기본값 객체를 공유해 의도하지 않은 영향을 주는 일을 막기 위해서입니다.


<a id="api-sfc-script-setup-modifiers-and-transformers"></a>

#### 수정자와 변환기
`v-model` 디렉티브와 함께 사용된 수정자에 접근하려면, `defineModel()`의 반환값을 구조 분해할 수 있습니다:

```js
const [modelValue, modelModifiers] = defineModel()

// v-model.trim에 해당
if (modelModifiers.trim) {
  // ...
}
```

수정자가 있을 때는 값을 읽거나 부모에 동기화하는 시점에 변환이 필요할 수 있습니다. `get`과 `set` 변환기 옵션을 사용해 이를 구현할 수 있습니다:

```js
const [modelValue, modelModifiers] = defineModel({
  // get()은 여기서 필요 없으므로 생략
  set(value) {
    // .trim 수정자가 사용된 경우, trim된 값을 반환
    if (modelModifiers.trim) {
      return value.trim()
    }
    // 그렇지 않으면 값을 그대로 반환
    return value
  }
})
```

<a id="api-sfc-script-setup-usage-with-typescript"></a>

#### TypeScript와 함께 사용하기  (TypeScript)
`defineProps`와 `defineEmits`처럼, `defineModel`도 모델 값과 수정자의 타입을 지정하는 타입 인자를 받을 수 있습니다:

```ts
const modelValue = defineModel<string>()
//    ^? Ref<string | undefined>

// 옵션이 있는 기본 모델, required는 undefined 가능성을 제거
const modelValue = defineModel<string>({ required: true })
//    ^? Ref<string>

const [modelValue, modifiers] = defineModel<string, 'trim' | 'uppercase'>()
//                 ^? Record<'trim' | 'uppercase', true | undefined>
```

<a id="api-sfc-script-setup-defineexpose"></a>

### defineExpose()
`<script setup>`을 사용하는 컴포넌트는 **기본적으로 닫혀 있습니다**. 즉, 템플릿 ref나 `$parent` 체인을 통해 가져온 컴포넌트의 public 인스턴스는 `<script setup>` 내부에 선언된 바인딩을 **노출하지 않습니다**.

`<script setup>` 컴포넌트에서 속성을 명시적으로 노출하려면 `defineExpose` 컴파일러 매크로를 사용하세요:

```vue
<script setup>
import { ref } from 'vue'

const a = 1
const b = ref(2)

defineExpose({
  a,
  b
})
</script>
```

부모가 템플릿 ref를 통해 이 컴포넌트의 인스턴스를 가져오면, 반환된 인스턴스는 `{ a: number, b: number }` 형태가 됩니다(ref는 일반 인스턴스처럼 자동으로 언래핑됨).

<a id="api-sfc-script-setup-defineoptions"></a>

### defineOptions()
- 3.3+에서만 지원

이 매크로를 사용하면 별도의 `<script>` 블록 없이 `<script setup>` 내부에서 컴포넌트 옵션을 직접 선언할 수 있습니다:

```vue
<script setup>
defineOptions({
  inheritAttrs: false,
  customOptions: {
    /* ... */
  }
})
</script>
```

- 이 매크로는 옵션을 모듈 스코프로 호이스팅하며, 리터럴 상수가 아닌 `<script setup>` 내 로컬 변수에는 접근할 수 없습니다.

<a id="api-sfc-script-setup-defineslots"></a>

### defineSlots() (TypeScript)
- 3.3+에서만 지원

이 매크로는 슬롯(slot) 이름과 props 타입 체크를 위한 IDE 타입 힌트를 제공하는 데 사용할 수 있습니다.

`defineSlots()`는 타입 파라미터만 받고 런타임 인자는 받지 않습니다. 타입 파라미터는 속성 키가 슬롯 이름이고, 값 타입이 슬롯 함수인 타입 리터럴이어야 합니다. 함수의 첫 번째 인자는 슬롯이 받을 props이며, 이 타입이 템플릿에서 슬롯 props로 사용됩니다. 반환 타입은 현재 무시되며 any가 될 수 있지만, 향후 슬롯 내용 체크에 활용될 수 있습니다.

또한, `setup` 컨텍스트에 노출되거나 `useSlots()`로 반환되는 `slots` 객체와 동일한 `slots` 객체를 반환합니다.

```vue
<script setup lang="ts">
const slots = defineSlots<{
  default(props: { msg: string }): any
}>()
</script>
```

<a id="api-sfc-script-setup-useslots-useattrs"></a>

### `useSlots()` & `useAttrs()`
템플릿에서는 `$slots`와 `$attrs`로 직접 접근할 수 있으므로, `<script setup>` 내부에서 `slots`와 `attrs`를 사용할 일은 비교적 드뭅니다. 드물게 필요할 경우 각각 `useSlots`와 `useAttrs` 헬퍼를 사용하세요:

```vue
<script setup>
import { useSlots, useAttrs } from 'vue'

const slots = useSlots()
const attrs = useAttrs()
</script>
```

`useSlots`와 `useAttrs`는 실제 런타임 함수로, 각각 `setupContext.slots` 및 `setupContext.attrs`와 동일한 값을 반환합니다. 일반 컴포지션 API 함수에서도 사용할 수 있습니다.

<a id="api-sfc-script-setup-usage-alongside-normal-script"></a>

### 일반 `<script>`와 함께 사용하기
`<script setup>`은 일반 `<script>`와 함께 사용할 수 있습니다. 일반 `<script>`가 필요한 경우는 다음과 같습니다:

- `<script setup>`에서 표현할 수 없는 옵션 선언(예: `inheritAttrs` 또는 플러그인(plugin)으로 활성화된 커스텀 옵션, 3.3+에서는 [`defineOptions`](08_component_and_advanced_apis.md#api-sfc-script-setup-defineoptions)로 대체 가능)
- 명명된 export 선언
- 한 번만 실행되어야 하는 부수 효과 실행 또는 객체 생성

```vue
<script>
// 일반 <script>, 모듈 스코프에서 한 번만 실행됨
runSideEffectOnce()

// 추가 옵션 선언
export default {
  inheritAttrs: false,
  customOptions: {}
}
</script>

<script setup>
// setup() 스코프에서 실행(각 인스턴스마다)
</script>
```

동일 컴포넌트에서 `<script setup>`과 `<script>`를 조합하는 것은 위에서 설명한 시나리오에 한정됩니다. 구체적으로:

- 이미 `<script setup>`에서 정의할 수 있는 옵션(예: `props`, `emits`)을 별도의 `<script>` 섹션에서 선언하지 마세요.
- `<script setup>` 내부에서 생성된 변수는 컴포넌트 인스턴스의 속성으로 추가되지 않으므로 옵션 API에서 접근할 수 없습니다. 이런 방식의 API 혼용은 강력히 권장하지 않습니다.

지원되지 않는 시나리오에 해당한다면, `<script setup>` 대신 명시적인 [`setup()`](07_composition_and_reactivity_apis.md#api-composition-api-setup) 함수를 사용하는 것을 고려하세요.

<a id="api-sfc-script-setup-top-level-await"></a>

### 최상위 `await`
최상위 `await`는 `<script setup>` 내부에서 사용할 수 있습니다. 결과 코드는 `async setup()`으로 컴파일됩니다:

```vue
<script setup>
const post = await fetch(`/api/post/1`).then((r) => r.json())
</script>
```

또한, await된 표현식은 `await` 이후에도 현재 컴포넌트 인스턴스 컨텍스트가 유지되는 형식으로 자동 컴파일됩니다.

**참고**
`async setup()`은 [`Suspense`](04_built_ins_and_animation.md#guide-built-ins-suspense)와 함께 사용해야 하며, 현재는 실험적 기능입니다. 향후 릴리스에서 공식화 및 문서화할 예정이지만, 지금 궁금하다면 [테스트](https://github.com/vuejs/core/blob/main/packages/runtime-core/__tests__/components/Suspense.spec.ts)를 참고해 동작 방식을 확인할 수 있습니다.


<a id="api-sfc-script-setup-imports-statements"></a>

### import 구문
Vue의 import 구문은 [ECMAScript 모듈 명세](https://nodejs.org/api/esm.html)를 따릅니다.
또한, 빌드 도구 설정에 정의된 별칭을 사용할 수 있습니다:

```vue
<script setup>
import { ref } from 'vue'
import { componentA } from './Components'
import { componentB } from '@/Components'
import { componentC } from '~/Components'
</script>
```

<a id="api-sfc-script-setup-generics"></a>

### 제네릭  (TypeScript)
제네릭 타입 파라미터는 `<script>` 태그의 `generic` 속성을 사용해 선언할 수 있습니다:

```vue
<script setup lang="ts" generic="T">
defineProps<{
  items: T[]
  selected: T
}>()
</script>
```

`generic`의 값은 TypeScript에서 `<...>` 사이의 파라미터 리스트와 동일하게 동작합니다. 예를 들어, 여러 파라미터, `extends` 제약, 기본 타입, import된 타입 참조 등을 사용할 수 있습니다:

```vue
<script
  setup
  lang="ts"
  generic="T extends string | number, U extends Item"
>
import type { Item } from './types'
defineProps<{
  id: T
  list: U[]
}>()
</script>
```

타입을 추론할 수 없는 경우, `@vue-generic` 디렉티브를 사용해 명시적으로 타입을 전달할 수 있습니다:

```vue
<template>
  <!-- @vue-generic {import('@/api').Actor} -->
  <ApiSelect v-model="peopleIds" endpoint="/api/actors" id-prop="actorId" />

  <!-- @vue-generic {import('@/api').Genre} -->
  <ApiSelect v-model="genreIds" endpoint="/api/genres" id-prop="genreId" />
</template>
```

제네릭 컴포넌트 참조를 `ref`에서 사용하려면 [`vue-component-type-helpers`](https://www.npmjs.com/package/vue-component-type-helpers) 라이브러리를 사용해야 하며, `InstanceType`은 동작하지 않습니다.

```vue
<script
  setup
  lang="ts"
>
import componentWithoutGenerics from '../component-without-generics.vue';
import genericComponent from '../generic-component.vue';

import type { ComponentExposed } from 'vue-component-type-helpers';

// 제네릭이 없는 컴포넌트에는 동작함
ref<InstanceType<typeof componentWithoutGenerics>>();

ref<ComponentExposed<typeof genericComponent>>();
```

<a id="api-sfc-script-setup-restrictions"></a>

### 제약 사항
- 모듈 실행 방식의 차이로 인해, `<script setup>` 내부 코드는 SFC의 컨텍스트에 의존합니다. 외부 `.js` 또는 `.ts` 파일로 이동하면 개발자와 도구 모두에게 혼란을 초래할 수 있습니다. 따라서 **`<script setup>`**은 `src` 속성과 함께 사용할 수 없습니다.
- `<script setup>`은 In-DOM 루트 컴포넌트 템플릿을 지원하지 않습니다.([관련 논의](https://github.com/vuejs/core/issues/8391))

---

<a id="api-sfc-spec"></a>

<a id="api-sfc-spec-sfc-syntax-specification"></a>

## SFC 구문 명세

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/sfc-spec.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/sfc-spec.md

<a id="api-sfc-spec-overview"></a>

### 개요
Vue Single-File Component(SFC)는 관례적으로 `*.vue` 파일 확장자를 사용하며, Vue 컴포넌트(component)를 설명하기 위해 HTML과 유사한 구문을 사용하는 커스텀 파일 형식입니다. Vue SFC는 문법적으로 HTML과 호환됩니다.

각 `*.vue` 파일은 세 가지 유형의 최상위 언어 블록(`<template>`, `<script>`, `<style>`)과 선택적으로 추가되는 커스텀 블록으로 구성됩니다:

```vue
<template>
  <div class="example">{{ msg }}</div>
</template>

<script>
export default {
  data() {
    return {
      msg: 'Hello world!'
    }
  }
}
</script>

<style>
.example {
  color: red;
}
</style>

<custom1>
  이곳에는 예를 들어 컴포넌트에 대한 문서가 들어갈 수 있습니다.
</custom1>
```

<a id="api-sfc-spec-language-blocks"></a>

### 언어 블록
<a id="api-sfc-spec-template"></a>

#### `<template>`
- 각 `*.vue` 파일에는 최상위 `<template>` 블록이 최대 한 개만 포함될 수 있습니다.

- 내용은 추출되어 `@vue/compiler-dom`에 전달되고, JavaScript 렌더 함수로 사전 컴파일되어 내보내지는 컴포넌트의 `render` 옵션에 연결됩니다.

<a id="api-sfc-spec-script"></a>

#### `<script>`
- 각 `*.vue` 파일에는 `<script>` 블록이 최대 한 개만 포함될 수 있습니다([`<script setup>`](08_component_and_advanced_apis.md#api-sfc-script-setup)은 제외).

- 스크립트는 ES 모듈로 실행됩니다.

- **기본 내보내기(default export)**는 Vue 컴포넌트 옵션 객체여야 하며, 일반 객체이거나 [defineComponent](07_composition_and_reactivity_apis.md#api-general-definecomponent)의 반환값일 수 있습니다.

<a id="api-sfc-spec-script-setup"></a>

#### `<script setup>`
- 각 `*.vue` 파일에는 `<script setup>` 블록이 최대 한 개만 포함될 수 있습니다(일반 `<script>`는 제외).

- 이 스크립트는 사전 처리되어 컴포넌트의 `setup()` 함수로 사용됩니다. 즉, **컴포넌트의 각 인스턴스(instance)마다 실행**됩니다. `<script setup>`의 최상위 바인딩(binding)은 템플릿에 자동으로 노출됩니다. 자세한 내용은 [`<script setup>` 전용 문서](08_component_and_advanced_apis.md#api-sfc-script-setup)를 참고하세요.

<a id="api-sfc-spec-style"></a>

#### `<style>`
- 하나의 `*.vue` 파일에는 여러 개의 `<style>` 태그를 포함할 수 있습니다.

- `<style>` 태그는 `scoped` 또는 `module` 속성(자세한 내용은 [SFC 스타일 기능](08_component_and_advanced_apis.md#api-sfc-css-features) 참고)을 가질 수 있어, 스타일을 현재 컴포넌트에 캡슐화하는 데 도움이 됩니다. 서로 다른 캡슐화 모드를 가진 여러 `<style>` 태그를 하나의 컴포넌트에 혼합할 수 있습니다.

<a id="api-sfc-spec-custom-blocks"></a>

#### 커스텀 블록
프로젝트별 필요에 따라 추가적인 커스텀 블록을 `*.vue` 파일에 포함할 수 있습니다. 예를 들어 `<docs>` 블록이 있습니다. 실제 커스텀 블록의 예시는 다음과 같습니다:

- [Gridsome: `<page-query>`](https://gridsome.org/docs/querying-data/)
- [vite-plugin-vue-gql: `<gql>`](https://github.com/wheatjs/vite-plugin-vue-gql)
- [vue-i18n: `<i18n>`](https://github.com/intlify/bundle-tools/tree/main/packages/unplugin-vue-i18n#i18n-custom-block)

커스텀 블록의 처리는 도구에 따라 달라집니다. 커스텀 블록 통합을 직접 구축하고 싶다면 [SFC 커스텀 블록 통합 도구 섹션](05_scaling_typescript_and_best_practices.md#guide-scaling-up-tooling-sfc-custom-block-integrations)을 참고하세요.

<a id="api-sfc-spec-automatic-name-inference"></a>

### 자동 이름 추론
SFC는 다음과 같은 경우 **파일명**에서 컴포넌트의 이름을 자동으로 추론합니다:

- 개발 경고 포맷팅
- DevTools 검사
- 재귀적 자기 참조, 예를 들어 `FooBar.vue`라는 파일은 템플릿(template)에서 `<FooBar/>`로 자신을 참조할 수 있습니다. 이는 명시적으로 등록/임포트된 컴포넌트보다 우선순위가 낮습니다.

<a id="api-sfc-spec-pre-processors"></a>

### 프리프로세서
블록은 `lang` 속성을 사용하여 프리프로세서 언어를 선언할 수 있습니다. 가장 일반적인 예는 `<script>` 블록에서 TypeScript를 사용하는 경우입니다:

```vue-html
<script lang="ts">
  // TypeScript 사용
</script>
```

`lang`은 모든 블록에 적용할 수 있습니다. 예를 들어, `<style>`에는 [Sass](https://sass-lang.com/)를, `<template>`에는 [Pug](https://pugjs.org/api/getting-started.html)를 사용할 수 있습니다:

```vue-html
<template lang="pug">
p {{ msg }}
</template>

<style lang="scss">
  $primary-color: #333;
  body {
    color: $primary-color;
  }
</style>
```

다양한 프리프로세서와의 통합은 도구 체인에 따라 다를 수 있습니다. 예시는 각 문서를 참고하세요:

- [Vite](https://vite.dev/guide/features.html#css-pre-processors)
- [Vue CLI](https://cli.vuejs.org/guide/css.html#pre-processors)
- [webpack + vue-loader](https://vue-loader.vuejs.org/guide/pre-processors.html#using-pre-processors)

<a id="api-sfc-spec-src-imports"></a>

### `src` 임포트
`*.vue` 컴포넌트를 여러 파일로 분리하고 싶다면, `src` 속성을 사용하여 언어 블록에 외부 파일을 임포트할 수 있습니다:

```vue
<template src="./template.html"></template>
<style src="./style.css"></style>
<script src="./script.js"></script>
```

`src` 임포트는 webpack 모듈 요청과 동일한 경로 해석 규칙을 따르므로 주의하세요:

- 상대 경로는 `./`로 시작해야 합니다.
- npm 의존성에서 리소스를 임포트할 수 있습니다:

```vue
<!-- 설치된 "todomvc-app-css" npm 패키지에서 파일 임포트 -->
<style src="todomvc-app-css/index.css" />
```

`src` 임포트는 커스텀 블록에도 사용할 수 있습니다. 예:

```vue
<unit-test src="./unit-test.js">
</unit-test>
```

**참고**
`src`에서 별칭을 사용할 때는 `~`로 시작하지 마세요. 그 뒤의 모든 것은 모듈 요청으로 해석됩니다. 즉, node 모듈 내부의 에셋을 참조할 수 있습니다:
```vue
<img src="~some-npm-package/foo.png">
```


<a id="api-sfc-spec-comments"></a>

### 주석
각 블록 내부에서는 사용 중인 언어(HTML, CSS, JavaScript, Pug 등)의 주석 구문을 사용해야 합니다. 최상위 주석에는 HTML 주석 구문을 사용하세요: `<!-- 여기에 주석 내용을 입력 -->`

---

<a id="api-ssr"></a>

<a id="api-ssr-server-side-rendering-api"></a>

## 서버 사이드 렌더링 API

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/ssr.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/ssr.md

<a id="api-ssr-rendertostring"></a>

### renderToString()
- **`vue/server-renderer`에서 내보냄**

- **타입**

  ```ts
  function renderToString(
    input: App | VNode,
    context?: SSRContext
  ): Promise<string>
  ```

- **예시**

  ```js
  import { createSSRApp } from 'vue'
  import { renderToString } from 'vue/server-renderer'

  const app = createSSRApp({
    data: () => ({ msg: 'hello' }),
    template: `<div>{{ msg }}</div>`
  })

  ;(async () => {
    const html = await renderToString(app)
    console.log(html)
  })()
  ```

  ### SSR 컨텍스트 {#ssr-context}

  선택적으로 컨텍스트 객체를 전달할 수 있으며, 렌더링(rendering) 중에 추가 데이터를 기록하는 데 사용할 수 있습니다. 예를 들어 [Teleport의 내용 접근](05_scaling_typescript_and_best_practices.md#guide-scaling-up-ssr-teleports)에 활용할 수 있습니다:

  ```js
  const ctx = {}
  const html = await renderToString(app, ctx)

  console.log(ctx.teleports) // { '#teleported': 'teleported content' }
  ```

  이 페이지에 있는 다른 SSR API 대부분도 선택적으로 컨텍스트 객체를 받을 수 있습니다. 컨텍스트 객체는 컴포넌트(component) 코드에서 [useSSRContext](08_component_and_advanced_apis.md#api-ssr-usessrcontext) 헬퍼를 통해 접근할 수 있습니다.

- **관련 문서** [가이드 - 서버 사이드 렌더링](05_scaling_typescript_and_best_practices.md#guide-scaling-up-ssr)

<a id="api-ssr-rendertonodestream"></a>

### renderToNodeStream()
입력을 [Node.js Readable 스트림](https://nodejs.org/api/stream.html#stream_class_stream_readable)으로 렌더링합니다.

- **`vue/server-renderer`에서 내보냄**

- **타입**

  ```ts
  function renderToNodeStream(
    input: App | VNode,
    context?: SSRContext
  ): Readable
  ```

- **예시**

  ```js
  // Node.js http 핸들러 내부
  renderToNodeStream(app).pipe(res)
  ```

  **참고**
  이 메서드는 Node.js 환경과 분리된 `vue/server-renderer`의 ESM 빌드에서는 지원되지 않습니다. 대신 [`pipeToNodeWritable`](08_component_and_advanced_apis.md#api-ssr-pipetonodewritable)을 사용하세요.


<a id="api-ssr-pipetonodewritable"></a>

### pipeToNodeWritable()
입력을 렌더링하여 기존 [Node.js Writable 스트림](https://nodejs.org/api/stream.html#stream_writable_streams) 인스턴스(instance)에 파이프합니다.

- **`vue/server-renderer`에서 내보냄**

- **타입**

  ```ts
  function pipeToNodeWritable(
    input: App | VNode,
    context: SSRContext = {},
    writable: Writable
  ): void
  ```

- **예시**

  ```js
  // Node.js http 핸들러 내부
  pipeToNodeWritable(app, {}, res)
  ```

<a id="api-ssr-rendertowebstream"></a>

### renderToWebStream()
입력을 [Web ReadableStream](https://developer.mozilla.org/en-US/docs/Web/API/Streams_API)으로 렌더링합니다.

- **`vue/server-renderer`에서 내보냄**

- **타입**

  ```ts
  function renderToWebStream(
    input: App | VNode,
    context?: SSRContext
  ): ReadableStream
  ```

- **예시**

  ```js
  // ReadableStream을 지원하는 환경에서
  return new Response(renderToWebStream(app))
  ```

  **참고**
  전역 범위에 `ReadableStream` 생성자가 노출되지 않은 환경에서는 [`pipeToWebWritable()`](08_component_and_advanced_apis.md#api-ssr-pipetowebwritable)을 대신 사용해야 합니다.


<a id="api-ssr-pipetowebwritable"></a>

### pipeToWebWritable()
입력을 렌더링하여 기존 [Web WritableStream](https://developer.mozilla.org/en-US/docs/Web/API/WritableStream) 인스턴스에 파이프합니다.

- **`vue/server-renderer`에서 내보냄**

- **타입**

  ```ts
  function pipeToWebWritable(
    input: App | VNode,
    context: SSRContext = {},
    writable: WritableStream
  ): void
  ```

- **예시**

  일반적으로 [`TransformStream`](https://developer.mozilla.org/en-US/docs/Web/API/TransformStream)과 함께 사용됩니다:

  ```js
  // TransformStream은 CloudFlare workers와 같은 환경에서 사용 가능합니다.
  // Node.js에서는 TransformStream을 'stream/web'에서 명시적으로 import해야 합니다.
  const { readable, writable } = new TransformStream()
  pipeToWebWritable(app, {}, writable)

  return new Response(readable)
  ```

<a id="api-ssr-rendertosimplestream"></a>

### renderToSimpleStream()
간단한 읽기 인터페이스를 사용하여 스트리밍 모드로 입력을 렌더링합니다.

- **`vue/server-renderer`에서 내보냄**

- **타입**

  ```ts
  function renderToSimpleStream(
    input: App | VNode,
    context: SSRContext,
    options: SimpleReadable
  ): SimpleReadable

  interface SimpleReadable {
    push(content: string | null): void
    destroy(err: any): void
  }
  ```

- **예시**

  ```js
  let res = ''

  renderToSimpleStream(
    app,
    {},
    {
      push(chunk) {
        if (chunk === null) {
          // 완료
          console(`렌더링 완료: ${res}`)
        } else {
          res += chunk
        }
      },
      destroy(err) {
        // 에러 발생
      }
    }
  )
  ```

<a id="api-ssr-usessrcontext"></a>

### useSSRContext()
`renderToString()` 또는 기타 서버 렌더 API에 전달된 컨텍스트 객체를 가져오는 런타임 API입니다.

- **타입**

  ```ts
  function useSSRContext<T = Record<string, any>>(): T | undefined
  ```

- **예시**

  가져온 컨텍스트는 최종 HTML 렌더링에 필요한 정보(예: head 메타데이터)를 첨부하는 데 사용할 수 있습니다.

  ```vue
  <script setup>
  import { useSSRContext } from 'vue'

  // 반드시 SSR 중에만 호출해야 합니다.
  // https://vite.dev/guide/ssr.html#conditional-logic
  if (import.meta.env.SSR) {
    const ctx = useSSRContext()
    // ...컨텍스트에 속성 추가
  }
  </script>
  ```

<a id="api-ssr-data-allow-mismatch"></a>

### data-allow-mismatch  (3.5+)
[하이드레이션(hydration) 불일치](05_scaling_typescript_and_best_practices.md#guide-scaling-up-ssr-hydration-mismatch) 경고를 억제하는 데 사용할 수 있는 특수 속성입니다.

- **예시**

  ```html
  <div data-allow-mismatch="text">{{ data.toLocaleString() }}</div>
  ```

  값을 지정하면 허용할 불일치를 특정 유형으로 제한할 수 있습니다. 사용 가능한 값은 다음과 같습니다:

  - `text`
  - `children` (직접 자식에 대한 불일치만 허용)
  - `class`
  - `style`
  - `attribute`

  값을 제공하지 않으면 모든 유형의 불일치가 허용됩니다.

---

<a id="api-utility-types"></a>

<a id="api-utility-types-utility-types"></a>

## 유틸리티 타입

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/api/utility-types.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/api/utility-types.md

**안내**
이 페이지에는 일반적으로 사용되는 유틸리티 타입 중 사용법 설명이 필요한 몇 가지만 나열되어 있습니다. 내보내는 타입의 전체 목록은 [소스 코드](https://github.com/vuejs/core/blob/main/packages/runtime-core/src/index.ts#L131)를 참고하세요.


<a id="api-utility-types-proptype-t"></a>

### PropType\<T>
런타임 props 선언을 사용할 때, prop에 더 고급 타입을 주석으로 달기 위해 사용합니다.

- **예시**

  ```ts
  import type { PropType } from 'vue'

  interface Book {
    title: string
    author: string
    year: number
  }

  export default {
    props: {
      book: {
        // `Object`에 더 구체적인 타입을 제공합니다
        type: Object as PropType<Book>,
        required: true
      }
    }
  }
  ```

- **관련 문서** [가이드 - 컴포넌트 Props 타입 지정](05_scaling_typescript_and_best_practices.md#guide-typescript-options-api-typing-component-props)

<a id="api-utility-types-mayberef"></a>

### MaybeRef\<T>
- 3.3+에서만 지원

`T | Ref<T>`의 별칭입니다. [컴포저블(composable)](03_components_and_reusability.md#guide-reusability-composables) 인자의 타입을 주석으로 달 때 유용합니다.

<a id="api-utility-types-maybereforgetter"></a>

### MaybeRefOrGetter\<T>
- 3.3+에서만 지원

`T | Ref<T> | (() => T)`의 별칭입니다. [컴포저블](03_components_and_reusability.md#guide-reusability-composables) 인자의 타입을 주석으로 달 때 유용합니다.

<a id="api-utility-types-extractproptypes"></a>

### ExtractPropTypes\<T>
런타임 props 옵션 객체에서 컴포넌트(component) 내부가 사용하는 prop 타입을 추출합니다. 기본값과 불리언 변환이 적용된 뒤의 타입이므로, 불리언 props와 기본값이 있는 props는 필수 여부와 관계없이 항상 정의된 것으로 취급합니다.

외부에서 전달 가능한 props, 즉 부모가 전달할 수 있는 props를 추출하려면 [`ExtractPublicPropTypes`](08_component_and_advanced_apis.md#api-utility-types-extractpublicproptypes)를 사용하세요.

- **예시**

  ```ts
  const propsOptions = {
    foo: String,
    bar: Boolean,
    baz: {
      type: Number,
      required: true
    },
    qux: {
      type: Number,
      default: 1
    }
  } as const

  type Props = ExtractPropTypes<typeof propsOptions>
  // {
  //   foo?: string,
  //   bar: boolean,
  //   baz: number,
  //   qux: number
  // }
  ```

<a id="api-utility-types-extractpublicproptypes"></a>

### ExtractPublicPropTypes\<T>
- 3.3+에서만 지원

런타임 props 옵션 객체에서 prop 타입을 추출합니다. 추출된 타입은 외부에서 사용되는 타입입니다. 즉, 부모가 전달할 수 있는 props입니다.

- **예시**

  ```ts
  const propsOptions = {
    foo: String,
    bar: Boolean,
    baz: {
      type: Number,
      required: true
    },
    qux: {
      type: Number,
      default: 1
    }
  } as const

  type Props = ExtractPublicPropTypes<typeof propsOptions>
  // {
  //   foo?: string,
  //   bar?: boolean,
  //   baz: number,
  //   qux?: number
  // }
  ```

<a id="api-utility-types-componentcustomproperties"></a>

### ComponentCustomProperties
커스텀 전역 속성을 지원하기 위해 컴포넌트 인스턴스(instance) 타입을 확장할 때 사용합니다.

- **예시**

  ```ts
  import axios from 'axios'

  declare module 'vue' {
    interface ComponentCustomProperties {
      $http: typeof axios
      $translate: (key: string) => string
    }
  }
  ```

  **참고**
  확장은 모듈 `.ts` 또는 `.d.ts` 파일에 작성해야 합니다. 자세한 내용은 [타입 확장 위치](05_scaling_typescript_and_best_practices.md#guide-typescript-options-api-augmenting-global-properties)를 참고하세요.


- **관련 문서** [가이드 - 전역 속성 확장](05_scaling_typescript_and_best_practices.md#guide-typescript-options-api-augmenting-global-properties)

<a id="api-utility-types-componentcustomoptions"></a>

### ComponentCustomOptions
커스텀 옵션을 지원하기 위해 컴포넌트 옵션 타입을 확장할 때 사용합니다.

- **예시**

  ```ts
  import { Route } from 'vue-router'

  declare module 'vue' {
    interface ComponentCustomOptions {
      beforeRouteEnter?(to: any, from: any, next: () => void): void
    }
  }
  ```

  **참고**
  확장은 모듈 `.ts` 또는 `.d.ts` 파일에 작성해야 합니다. 자세한 내용은 [타입 확장 위치](05_scaling_typescript_and_best_practices.md#guide-typescript-options-api-augmenting-global-properties)를 참고하세요.


- **관련 문서** [가이드 - 커스텀 옵션 확장](05_scaling_typescript_and_best_practices.md#guide-typescript-options-api-augmenting-custom-options)

<a id="api-utility-types-componentcustomprops"></a>

### ComponentCustomProps
TSX 요소에서 선언되지 않은 props를 사용하기 위해, 허용되는 TSX props를 확장할 때 사용합니다.

- **예시**

  ```ts
  declare module 'vue' {
    interface ComponentCustomProps {
      hello?: string
    }
  }

  export {}
  ```

  ```tsx
  // hello가 선언된 prop이 아니어도 이제 동작합니다
  <MyComponent hello="world" />
  ```

  **참고**
  확장은 모듈 `.ts` 또는 `.d.ts` 파일에 작성해야 합니다. 자세한 내용은 [타입 확장 위치](05_scaling_typescript_and_best_practices.md#guide-typescript-options-api-augmenting-global-properties)를 참고하세요.


<a id="api-utility-types-cssproperties"></a>

### CSSProperties
style 속성 바인딩(binding)에서 허용되는 값을 확장할 때 사용합니다.

- **예시**

  커스텀 CSS 속성 허용

  ```ts
  declare module 'vue' {
    interface CSSProperties {
      [key: `--${string}`]: string
    }
  }
  ```

  ```tsx
  <div style={ { '--bg-color': 'blue' } }>
  ```

  ```html
  <div :style="{ '--bg-color': 'blue' }"></div>
  ```

**참고**
확장은 모듈 `.ts` 또는 `.d.ts` 파일에 작성해야 합니다. 자세한 내용은 [타입 확장 위치](05_scaling_typescript_and_best_practices.md#guide-typescript-options-api-augmenting-global-properties)를 참고하세요.


**관련 문서**
SFC `<style>` 태그는 `v-bind` CSS 함수를 사용하여 CSS 값을 동적인 컴포넌트 상태에 연결하는 것을 지원합니다. 이를 통해 타입 확장 없이도 커스텀 속성을 사용할 수 있습니다.

- [CSS에서 v-bind()](08_component_and_advanced_apis.md#api-sfc-css-features-v-bind-in-css)

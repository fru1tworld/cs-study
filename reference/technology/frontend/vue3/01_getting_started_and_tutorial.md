# Vue 3 시작하기와 튜토리얼

Vue가 화면을 구성하는 방식부터 시작해 프로젝트를 만들고, 튜토리얼에서 상태와 이벤트, 컴포넌트를 직접 연결해 봅니다. 먼저 전체 흐름을 익힌 뒤 다음 장에서 각 기능의 동작과 제약을 살펴보면 됩니다.

## 목차

- [소개](#guide-introduction)
- [빠른 시작](#guide-quick-start)
- [Vue 사용 방법](#guide-extras-ways-of-using-vue)
- [싱글 파일 컴포넌트](#guide-scaling-up-sfc)
- [Vue 3 튜토리얼](#tutorial-index)
- [시작하기](#tutorial-src-step-1-description)
- [선언적 렌더링(rendering)](#tutorial-src-step-2-description)
- [속성 바인딩(binding)](#tutorial-src-step-3-description)
- [이벤트 리스너(listener)](#tutorial-src-step-4-description)
- [폼 바인딩(binding)](#tutorial-src-step-5-description)
- [조건부 렌더링(rendering)](#tutorial-src-step-6-description)
- [리스트 렌더링(rendering)](#tutorial-src-step-7-description)
- [계산된 속성(Computed Property)](#tutorial-src-step-8-description)
- [라이프사이클(lifecycle)과 템플릿 ref](#tutorial-src-step-9-description)
- [감시자(Watchers)](#tutorial-src-step-10-description)
- [컴포넌트(component)](#tutorial-src-step-11-description)
- [Props](#tutorial-src-step-12-description)
- [Emits](#tutorial-src-step-13-description)
- [슬롯(slot)](#tutorial-src-step-14-description)
- [해냈어요!](#tutorial-src-step-15-description)

---

<a id="guide-introduction"></a>

<a id="guide-introduction-introduction"></a>

## 소개

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/guide/introduction.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/guide/introduction.md

**당신은 Vue 3 문서를 읽고 있습니다!**

- Vue 2 지원은 **2023년 12월 31일**에 종료되었습니다. [Vue 2 EOL](https://v2.vuejs.org/eol/)에 대해 자세히 알아보세요.
- Vue 2에서 업그레이드하시나요? [마이그레이션 가이드](https://v3-migration.vuejs.org/)를 확인하세요.


```vue
<style src="@theme/styles/vue-mastery.css"></style>
```


참고 강의: https://www.vuemastery.com/courses/


<a id="guide-introduction-what-is-vue"></a>

### Vue란 무엇인가요?
Vue(발음: /vjuː/, **view**와 비슷함)는 사용자 인터페이스를 구축하기 위한 JavaScript 프레임워크입니다. 표준 HTML, CSS, JavaScript를 기반으로 하며, 선언적인 컴포넌트(component) 프로그래밍 모델을 제공합니다. 이를 바탕으로 단순한 화면부터 복잡한 애플리케이션까지 효율적으로 개발할 수 있습니다.

다음은 최소한의 예시입니다:


**옵션 API**


```js
import { createApp } from 'vue'

createApp({
  data() {
    return {
      count: 0
    }
  }
}).mount('#app')
```



**컴포지션 API**


```js
import { createApp, ref } from 'vue'

createApp({
  setup() {
    return {
      count: ref(0)
    }
  }
}).mount('#app')
```



```vue-html
<div id="app">
  <button @click="count++">
    Count is: {{ count }}
  </button>
</div>
```

**결과**


**문서 데모 설정 코드**

```vue
<script setup>
import { ref } from 'vue'
const count = ref(0)
</script>
```



```vue-html
<div class="demo">
  <button @click="count++">
    Count is: {{ count }}
  </button>
</div>
```



위 예시는 Vue의 두 가지 핵심 기능을 보여줍니다:

- **선언적 렌더링(rendering)**: Vue는 표준 HTML을 확장한 템플릿(template) 문법을 제공하여, JavaScript 상태에 따라 HTML 출력을 선언적으로 기술할 수 있게 해줍니다.

- **반응성(reactivity)**: Vue는 JavaScript 상태 변화를 자동으로 추적하고, 변화가 발생하면 DOM을 효율적으로 업데이트합니다.

버튼을 누르면 `count`가 바뀌고, Vue가 그 값에 맞춰 화면을 갱신합니다. 이후 튜토리얼에서 상태 선언과 이벤트 처리를 나눠 살펴봅니다.

**사전 지식**
이후 문서는 독자가 HTML, CSS, JavaScript에 기본적으로 익숙하다는 것을 전제로 합니다. 프론트엔드 개발이 완전히 처음이라면, 프레임워크부터 바로 시작하는 것은 좋은 선택이 아닐 수 있습니다. 기본기를 익힌 후 다시 돌아오세요! 필요하다면 [JavaScript](https://developer.mozilla.org/en-US/docs/Web/JavaScript/A_re-introduction_to_JavaScript), [HTML](https://developer.mozilla.org/en-US/docs/Learn/HTML/Introduction_to_HTML), [CSS](https://developer.mozilla.org/en-US/docs/Learn/CSS/First_steps) 개요를 통해 본인의 지식 수준을 확인할 수 있습니다. 다른 프레임워크 경험이 있다면 도움이 되지만, 필수는 아닙니다.


<a id="guide-introduction-the-progressive-framework"></a>

### 점진적 프레임워크
Vue는 프론트엔드 개발에 흔히 필요한 기능을 제공하는 프레임워크이자 생태계입니다. 웹 애플리케이션은 형태와 규모가 다양하므로, Vue도 필요한 부분부터 점진적으로 도입할 수 있도록 설계되었습니다. 용도에 따라 다음과 같이 사용할 수 있습니다:

- 빌드 단계 없이 정적 HTML 강화
- 어떤 페이지에도 웹 컴포넌트로 임베딩
- 싱글 페이지 애플리케이션(SPA)
- 풀스택 / 서버 사이드 렌더링(SSR)
- Jamstack / 정적 사이트 생성(SSG)
- 데스크탑, 모바일, WebGL, 심지어 터미널까지 타겟팅

이러한 개념들이 어렵게 느껴진다면 걱정하지 마세요! 튜토리얼과 가이드는 기본적인 HTML과 JavaScript 지식만 있으면 따라올 수 있으며, 이 중 어느 분야에서도 전문가일 필요는 없습니다.

경험이 많은 개발자라면 Vue를 스택에 어떻게 통합하는 것이 최적인지, 또는 이러한 용어들이 무엇을 의미하는지 궁금할 수 있습니다. 이에 대해서는 [Vue 사용 방법](01_getting_started_and_tutorial.md#guide-extras-ways-of-using-vue)에서 더 자세히 다룹니다.

사용 방식이 달라져도 상태, 템플릿, 컴포넌트에 대한 기본 개념은 공통으로 적용됩니다. 작은 화면에 Vue를 도입한 뒤 더 큰 애플리케이션으로 확장할 때도 같은 지식을 이어서 사용할 수 있습니다. Vue를 "점진적 프레임워크"라고 부르는 이유입니다.

<a id="guide-introduction-single-file-components"></a>

### 싱글 파일 컴포넌트
대부분의 빌드 도구 기반 Vue 프로젝트에서는 **싱글 파일 컴포넌트**(일명 `*.vue` 파일, **SFC**로 약칭)라는 HTML과 유사한 파일 형식을 사용하여 Vue 컴포넌트를 작성합니다. Vue SFC는 이름 그대로 컴포넌트의 로직(JavaScript), 템플릿(HTML), 스타일(CSS)을 하나의 파일에 캡슐화합니다. 아래는 앞서 본 예시를 SFC 형식으로 작성한 것입니다:


**옵션 API**


```vue
<script>
export default {
  data() {
    return {
      count: 0
    }
  }
}
</script>

<template>
  <button @click="count++">Count is: {{ count }}</button>
</template>

<style scoped>
button {
  font-weight: bold;
}
</style>
```



**컴포지션 API**


```vue
<script setup>
import { ref } from 'vue'
const count = ref(0)
</script>

<template>
  <button @click="count++">Count is: {{ count }}</button>
</template>

<style scoped>
button {
  font-weight: bold;
}
</style>
```



SFC는 Vue의 대표적인 기능이며, **빌드 설정이 필요한 경우** Vue 컴포넌트를 작성하는 권장 방식입니다. [SFC의 사용 방법과 이유](01_getting_started_and_tutorial.md#guide-scaling-up-sfc)에 대해 더 자세히 알아볼 수 있지만, 지금은 Vue가 모든 빌드 도구 설정을 대신 처리해준다는 것만 알아두세요.

<a id="guide-introduction-api-styles"></a>

### API 스타일
Vue 컴포넌트는 **옵션 API**와 **컴포지션 API**라는 두 가지 다른 API 스타일로 작성할 수 있습니다.

<a id="guide-introduction-options-api"></a>

#### 옵션 API
옵션 API에서는 `data`, `methods`, `mounted`와 같은 옵션 객체를 사용하여 컴포넌트의 로직을 정의합니다. 옵션으로 정의된 속성들은 함수 내부에서 `this`로 노출되는데, 이 `this`는 컴포넌트 인스턴스(instance)를 가리킵니다:

```vue
<script>
export default {
  // data()에서 반환된 속성들은 반응형 상태가 되며
  // `this`로 노출됩니다.
  data() {
    return {
      count: 0
    }
  },

  // methods는 상태를 변경하고 업데이트를 트리거하는 함수입니다.
  // 템플릿에서 이벤트 핸들러로 바인딩할 수 있습니다.
  methods: {
    increment() {
      this.count++
    }
  },

  // 라이프사이클 훅은 컴포넌트의 생명주기
  // 각 단계에서 호출됩니다.
  // 이 함수는 컴포넌트가 마운트될 때 호출됩니다.
  mounted() {
    console.log(`The initial count is ${this.count}.`)
  }
}
</script>

<template>
  <button @click="increment">Count is: {{ count }}</button>
</template>
```

[플레이그라운드에서 실행해보기](https://play.vuejs.org/#eNptkMFqxCAQhl9lkB522ZL0HNKlpa/Qo4e1ZpLIGhUdl5bgu9es2eSyIMio833zO7NP56pbRNawNkivHJ25wV9nPUGHvYiaYOYGoK7Bo5CkbgiBBOFy2AkSh2N5APmeojePCkDaaKiBt1KnZUuv3Ky0PppMsyYAjYJgigu0oEGYDsirYUAP0WULhqVrQhptF5qHQhnpcUJD+wyQaSpUd/Xp9NysVY/yT2qE0dprIS/vsds5Mg9mNVbaDofL94jZpUgJXUKBCvAy76ZUXY53CTd5tfX2k7kgnJzOCXIF0P5EImvgQ2olr++cbRE4O3+t6JxvXj0ptXVpye1tvbFY+ge/NJZt)

<a id="guide-introduction-composition-api"></a>

#### 컴포지션 API
컴포지션 API에서는 가져온 API 함수들을 사용하여 컴포넌트의 로직을 정의합니다. SFC에서는 컴포지션 API를 [`<script setup>`](08_component_and_advanced_apis.md#api-sfc-script-setup)과 함께 사용하는 것이 일반적입니다. `setup` 속성은 Vue가 컴파일 타임 변환을 수행하도록 힌트를 주어, 컴포지션 API를 더 적은 보일러플레이트(boilerplate)로 사용할 수 있게 해줍니다. 예를 들어, `<script setup>`에서 선언된 import와 최상위 변수/함수는 템플릿에서 바로 사용할 수 있습니다.

아래는 같은 템플릿을 그대로 사용하되, 컴포지션 API와 `<script setup>`으로 작성한 동일한 컴포넌트입니다:

```vue
<script setup>
import { ref, onMounted } from 'vue'

// 반응형 상태
const count = ref(0)

// 상태를 변경하고 업데이트를 트리거하는 함수
function increment() {
  count.value++
}

// 라이프사이클 훅
onMounted(() => {
  console.log(`The initial count is ${count.value}.`)
})
</script>

<template>
  <button @click="increment">Count is: {{ count }}</button>
</template>
```

[플레이그라운드에서 실행해보기](https://play.vuejs.org/#eNpNkMFqwzAQRH9lMYU4pNg9Bye09NxbjzrEVda2iLwS0spQjP69a+yYHnRYad7MaOfiw/tqSliciybqYDxDRE7+qsiM3gWGGQJ2r+DoyyVivEOGLrgRDkIdFCmqa1G0ms2EELllVKQdRQa9AHBZ+PLtuEm7RCKVd+ChZRjTQqwctHQHDqbvMUDyd7mKip4AGNIBRyQujzArgtW/mlqb8HRSlLcEazrUv9oiDM49xGGvXgp5uT5his5iZV1f3r4HFHvDprVbaxPhZf4XkKub/CDLaep1T7IhGRhHb6WoTADNT2KWpu/aGv24qGKvrIrr5+Z7hnneQnJu6hURvKl3ryL/ARrVkuI=)

<a id="guide-introduction-which-to-choose"></a>

#### 무엇을 선택해야 할까요?
두 API 스타일 모두 일반적인 사용 사례를 완벽하게 지원할 수 있습니다. 이들은 동일한 기반 시스템 위에서 동작하는 서로 다른 인터페이스입니다. 실제로 옵션 API는 컴포지션 API 위에 구현되어 있습니다! Vue에 대한 기본 개념과 지식은 두 스타일 모두에 공통적으로 적용됩니다.

옵션 API는 "컴포넌트 인스턴스"(예시에서 본 `this`) 개념을 중심으로 하며, OOP 언어 배경을 가진 사용자에게는 클래스 기반의 사고방식과 더 잘 맞을 수 있습니다. 또한 반응성의 세부 사항을 추상화하고 옵션 그룹을 통한 코드 구조화를 강제하여 초보자에게 더 친숙합니다.

컴포지션 API는 함수 스코프 내에서 반응형 상태 변수를 직접 선언하고, 여러 함수에서 상태를 조합하여 복잡성을 다루는 데 중점을 둡니다. 더 자유로운 형태이며, 효과적으로 사용하려면 Vue의 반응성 동작에 대한 이해가 필요합니다. 그 대신, 더 강력한 로직 구성 및 재사용 패턴을 가능하게 합니다.

두 스타일의 비교와 컴포지션 API의 잠재적 이점에 대해서는 [컴포지션 API FAQ](06_reactivity_and_rendering_in_depth.md#guide-extras-composition-api-faq)에서 더 알아볼 수 있습니다.

Vue가 처음이라면, 다음과 같이 권장합니다:

- 학습 목적이라면, 본인에게 더 이해하기 쉬워 보이는 스타일로 시작하세요. 대부분의 핵심 개념은 두 스타일에 공통적입니다. 나중에 언제든 다른 스타일을 익힐 수 있습니다.

- 실제 프로젝트에서는:

  - 빌드 도구를 사용하지 않거나, Vue를 주로 복잡도가 낮은 시나리오(예: 점진적 향상)에 사용할 계획이라면 옵션 API를 사용하세요.

  - Vue로 전체 애플리케이션을 구축할 계획이라면 컴포지션 API + 싱글 파일 컴포넌트를 사용하세요.

학습 단계에서 한 가지 스타일에만 얽매일 필요는 없습니다. 이후 문서에서는 상황에 따라 두 스타일 모두의 코드 샘플을 제공하며, 공식 사이트에서는 왼쪽 사이드바 상단의 **API 선호도 스위치**로 전환할 수 있습니다. 이 문서에서는 각 예제에 API 방식을 표시합니다.

<a id="guide-introduction-still-got-questions"></a>

### 아직 궁금한 점이 있으신가요?
[FAQ](09_style_guide_examples_and_reference.md#about-faq)를 확인해보세요.

<a id="guide-introduction-pick-your-learning-path"></a>

### 학습 경로 선택하기
개발자마다 학습 스타일이 다릅니다. 본인에게 맞는 학습 경로를 자유롭게 선택하세요. 물론 가능하다면 모든 내용을 한 번씩 훑어보는 것을 추천합니다!

  [튜토리얼 해보기     직접 실습하며 배우는 것을 선호하는 분들을 위한 경로입니다.](01_getting_started_and_tutorial.md#tutorial-index)
  [가이드 읽기     가이드는 프레임워크의 모든 측면을 자세히 안내합니다.](01_getting_started_and_tutorial.md#guide-quick-start)
  [예제 살펴보기     핵심 기능과 일반적인 UI 작업 예제를 탐색해보세요.](09_style_guide_examples_and_reference.md#examples-index)

---

<a id="guide-quick-start"></a>

**문서 데모 설정 코드**

```vue
<script setup>
import { VTCodeGroup, VTCodeGroupTab } from '@vue/theme'
</script>
```



<a id="guide-quick-start-quick-start"></a>

## 빠른 시작

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/guide/quick-start.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/guide/quick-start.md

<a id="guide-quick-start-try-vue-online"></a>

### 온라인에서 Vue 체험하기
- Vue를 빠르게 체험해보고 싶다면 [Playground](https://play.vuejs.org/#eNo9jcEKwjAMhl/lt5fpQYfXUQfefAMvvRQbddC1pUuHUPrudg4HIcmXjyRZXEM4zYlEJ+T0iEPgXjn6BB8Zhp46WUZWDjCa9f6w9kAkTtH9CRinV4fmRtZ63H20Ztesqiylphqy3R5UYBqD1UyVAPk+9zkvV1CKbCv9poMLiTEfR2/IXpSoXomqZLtti/IFwVtA9A==)에서 바로 사용해볼 수 있습니다.

- 빌드 과정 없이 순수 HTML 환경을 선호한다면, 이 [JSFiddle](https://jsfiddle.net/yyx990803/2ke1ab0z/)을 시작점으로 사용할 수 있습니다.

- Node.js와 빌드 도구 개념에 익숙하다면, [StackBlitz](https://vite.new/vue)를 이용해 브라우저 안에서 완전한 빌드 환경을 바로 체험할 수 있습니다.

- 권장 설정에 대한 안내가 필요하다면, 첫 Vue 앱을 실행, 수정, 배포하는 방법을 보여주는 대화형 [Scrimba](http://scrimba.com/links/vue-quickstart) 튜토리얼을 시청하세요.

<a id="guide-quick-start-creating-a-vue-application"></a>

### Vue 애플리케이션 생성하기
**사전 준비 사항**

- 커맨드 라인 사용에 익숙할 것
- [Node.js](https://nodejs.org/) `^22.18.0 || >=24.12.0` 버전 설치


이 섹션에서는 로컬 컴퓨터에서 Vue [싱글 페이지 애플리케이션](01_getting_started_and_tutorial.md#guide-extras-ways-of-using-vue-single-page-application-spa)을 스캐폴딩(scaffolding)하는 방법을 소개합니다. 생성된 프로젝트는 [Vite](https://vite.dev/)를 기반으로 한 빌드 환경을 갖추며, Vue [싱글 파일 컴포넌트](01_getting_started_and_tutorial.md#guide-scaling-up-sfc) (SFC)를 사용할 수 있습니다.

최신 버전의 [Node.js](https://nodejs.org/)가 설치되어 있는지, 그리고 현재 작업 디렉터리가 프로젝트를 생성하려는 위치인지 확인하세요. 커맨드 라인에서 다음 명령어를 실행하세요(`$` 기호는 입력하지 않습니다):


```sh [npm]
$ npm create vue@latest
```

```sh [pnpm]
$ pnpm create vue@latest
```

```sh [yarn]
# Yarn (v1+)용
$ yarn create vue

# Yarn Modern (v2+)용
$ yarn create vue@latest

# Yarn ^v4.11 용
$ yarn dlx create-vue@latest
```

```sh [bun]
$ bun create vue@latest
```


이 명령어는 공식 Vue 프로젝트 스캐폴딩 도구인 [create-vue](https://github.com/vuejs/create-vue)를 설치하고 실행합니다. TypeScript 및 테스트 지원과 같은 여러 선택적 기능에 대한 프롬프트가 표시됩니다:


```text
✔ 프로젝트 이름: … <your-project-name>
✔ TypeScript 추가? … 아니오 / 예
✔ JSX 지원 추가? … 아니오 / 예
✔ 싱글 페이지 애플리케이션 개발을 위한 Vue Router 추가? … 아니오 / 예
✔ 상태 관리를 위한 Pinia 추가? … 아니오 / 예
✔ 단위 테스트를 위한 Vitest 추가? … 아니오 / 예
✔ 엔드 투 엔드 테스트 솔루션 추가? … 아니오 / Cypress / Nightwatch / Playwright
✔ 코드 품질을 위한 ESLint 추가? … 아니오 / 예
✔ 코드 포매팅을 위한 Prettier 추가? … 아니오 / 예
✔ 디버깅을 위한 Vue DevTools 7 확장 프로그램 추가? (실험적) … 아니오 / 예

./<your-project-name>에 프로젝트 스캐폴딩 중...
완료.
```



옵션이 확실하지 않다면, 일단 엔터를 눌러 `No`를 선택하세요. 프로젝트가 생성되면, 의존성 설치 및 개발 서버 실행을 위한 안내에 따라 진행하세요:


```sh-vue [npm]
$ cd {{'<your-project-name>'}}
$ npm install
$ npm run dev
```

```sh-vue [pnpm]
$ cd {{'<your-project-name>'}}
$ pnpm install
$ pnpm run dev
```

```sh-vue [yarn]
$ cd {{'<your-project-name>'}}
$ yarn
$ yarn dev
```

```sh-vue [bun]
$ cd {{'<your-project-name>'}}
$ bun install
$ bun run dev
```



개발 서버가 시작되면 첫 번째 Vue 프로젝트를 확인할 수 있습니다. 생성된 프로젝트의 예제 컴포넌트(component)는 [옵션 API](01_getting_started_and_tutorial.md#guide-introduction-options-api)가 아닌 [컴포지션 API](01_getting_started_and_tutorial.md#guide-introduction-composition-api)와 `<script setup>`을 사용하여 작성되어 있습니다. 추가 팁은 다음과 같습니다:

- 권장 IDE 설정은 [Visual Studio Code](https://code.visualstudio.com/) + [Vue - 공식 확장 프로그램](https://marketplace.visualstudio.com/items?itemName=Vue.volar)입니다. 다른 에디터를 사용한다면 [IDE 지원 섹션](05_scaling_typescript_and_best_practices.md#guide-scaling-up-tooling-ide-support)을 참고하세요.
- 백엔드 프레임워크와의 통합 등 더 많은 도구 관련 정보는 [도구 가이드](05_scaling_typescript_and_best_practices.md#guide-scaling-up-tooling)에서 다룹니다.
- 빌드 도구 Vite에 대해 더 알고 싶다면 [Vite 문서](https://vite.dev/)를 참고하세요.
- TypeScript를 사용하기로 했다면 [TypeScript 사용 가이드](05_scaling_typescript_and_best_practices.md#guide-typescript-overview)를 참고하세요.

앱을 프로덕션에 배포할 준비가 되면 다음 명령어를 실행하세요:


```sh [npm]
$ npm run build
```

```sh [pnpm]
$ pnpm run build
```

```sh [yarn]
$ yarn build
```

```sh [bun]
$ bun run build
```



이 명령어는 프로젝트의 `./dist` 디렉터리에 프로덕션용 빌드를 생성합니다. 앱을 프로덕션에 배포하는 방법에 대해서는 [프로덕션 배포 가이드](05_scaling_typescript_and_best_practices.md#guide-best-practices-production-deployment)를 참고하세요.

[다음 단계 >](01_getting_started_and_tutorial.md#guide-quick-start-next-steps)

<a id="guide-quick-start-using-vue-from-cdn"></a>

### CDN에서 Vue 사용하기
스크립트 태그를 통해 CDN에서 직접 Vue를 사용할 수 있습니다:

```html
<script src="https://unpkg.com/vue@3/dist/vue.global.js"></script>
```

여기서는 [unpkg](https://unpkg.com/)를 사용했지만, [jsdelivr](https://www.jsdelivr.com/package/npm/vue)나 [cdnjs](https://cdnjs.com/libraries/vue) 등 npm 패키지를 제공하는 다른 CDN도 사용할 수 있습니다. 물론 이 파일을 다운로드하여 직접 서비스할 수도 있습니다.

CDN에서 Vue를 사용할 때는 "빌드 단계"가 필요하지 않습니다. 이로 인해 설정이 훨씬 간단해지며, 정적 HTML을 보강하거나 백엔드 프레임워크와 통합할 때 적합합니다. 하지만 싱글 파일 컴포넌트(SFC) 문법은 사용할 수 없습니다.

<a id="guide-quick-start-using-the-global-build"></a>

#### 글로벌 빌드 사용하기
위 링크는 Vue의 _글로벌 빌드_를 로드하며, 모든 최상위 API가 전역 `Vue` 객체의 속성으로 노출됩니다. 다음은 글로벌 빌드를 사용하는 전체 예제입니다:


**옵션 API**


```html
<script src="https://unpkg.com/vue@3/dist/vue.global.js"></script>

<div id="app">{{ message }}</div>

<script>
  const { createApp } = Vue

  createApp({
    data() {
      return {
        message: 'Hello Vue!'
      }
    }
  }).mount('#app')
</script>
```

[CodePen 데모 >](https://codepen.io/vuejs-examples/pen/QWJwJLp)


**컴포지션 API**


```html
<script src="https://unpkg.com/vue@3/dist/vue.global.js"></script>

<div id="app">{{ message }}</div>

<script>
  const { createApp, ref } = Vue

  createApp({
    setup() {
      const message = ref('Hello vue!')
      return {
        message
      }
    }
  }).mount('#app')
</script>
```

[CodePen 데모 >](https://codepen.io/vuejs-examples/pen/eYQpQEG)

**참고**
가이드 전반에 걸쳐 많은 컴포지션 API 예제가 `<script setup>` 문법을 사용할 예정인데, 이 문법을 사용하려면 빌드 도구가 필요합니다. 빌드 단계 없이 컴포지션 API를 사용하려면 [`setup()` 옵션](07_composition_and_reactivity_apis.md#api-composition-api-setup) 사용법을 참고하세요.


<a id="guide-quick-start-using-the-es-module-build"></a>

#### ES 모듈 빌드 사용하기
이후 문서에서는 주로 [ES 모듈](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Modules) 문법을 사용할 것입니다. 대부분의 최신 브라우저는 ES 모듈을 기본적으로 지원하므로, 다음과 같이 CDN에서 네이티브 ES 모듈로 Vue를 사용할 수 있습니다:


**옵션 API**


```html{3,4}
<div id="app">{{ message }}</div>

<script type="module">
  import { createApp } from 'https://unpkg.com/vue@3/dist/vue.esm-browser.js'

  createApp({
    data() {
      return {
        message: 'Hello Vue!'
      }
    }
  }).mount('#app')
</script>
```



**컴포지션 API**


```html{3,4}
<div id="app">{{ message }}</div>

<script type="module">
  import { createApp, ref } from 'https://unpkg.com/vue@3/dist/vue.esm-browser.js'

  createApp({
    setup() {
      const message = ref('Hello Vue!')
      return {
        message
      }
    }
  }).mount('#app')
</script>
```



여기서는 `<script type="module">`을 사용하고, 가져오는 CDN URL이 Vue의 **ES 모듈 빌드**를 가리키고 있다는 점에 주의하세요.


**옵션 API**


[CodePen 데모 >](https://codepen.io/vuejs-examples/pen/VwVYVZO)


**컴포지션 API**


[CodePen 데모 >](https://codepen.io/vuejs-examples/pen/MWzazEv)


<a id="guide-quick-start-enabling-import-maps"></a>

#### Import maps 활성화하기
위 예제에서는 전체 CDN URL에서 import하고 있지만, 이후 문서에서는 다음과 같은 코드를 자주 보게 될 것입니다:

```js
import { createApp } from 'vue'
```

[Import Maps](https://caniuse.com/import-maps)를 사용하여 브라우저에 `vue` import 위치를 알려줄 수 있습니다:


**옵션 API**


```html{1-7,12}
<script type="importmap">
  {
    "imports": {
      "vue": "https://unpkg.com/vue@3/dist/vue.esm-browser.js"
    }
  }
</script>

<div id="app">{{ message }}</div>

<script type="module">
  import { createApp } from 'vue'

  createApp({
    data() {
      return {
        message: 'Hello Vue!'
      }
    }
  }).mount('#app')
</script>
```

[CodePen 데모 >](https://codepen.io/vuejs-examples/pen/wvQKQyM)


**컴포지션 API**


```html{1-7,12}
<script type="importmap">
  {
    "imports": {
      "vue": "https://unpkg.com/vue@3/dist/vue.esm-browser.js"
    }
  }
</script>

<div id="app">{{ message }}</div>

<script type="module">
  import { createApp, ref } from 'vue'

  createApp({
    setup() {
      const message = ref('Hello Vue!')
      return {
        message
      }
    }
  }).mount('#app')
</script>
```

[CodePen 데모 >](https://codepen.io/vuejs-examples/pen/YzRyRYM)


다른 의존성도 import map에 추가할 수 있지만, 반드시 해당 라이브러리의 ES 모듈 버전을 가리키도록 해야 합니다.

**Import Maps 브라우저 지원**
Import Maps는 비교적 새로운 브라우저 기능입니다. [지원 범위](https://caniuse.com/import-maps) 내의 브라우저를 사용해야 합니다. 특히 Safari는 16.4 이상에서만 지원됩니다.


**프로덕션 사용 시 주의사항**
지금까지의 예제는 Vue의 개발용 빌드를 사용하고 있습니다. CDN에서 Vue를 프로덕션에 사용할 계획이라면 [프로덕션 배포 가이드](05_scaling_typescript_and_best_practices.md#guide-best-practices-production-deployment-without-build-tools)를 반드시 참고하세요.

빌드 시스템 없이 Vue를 사용하는 것도 가능하지만, 예전 같으면 [`jquery/jquery`](https://github.com/jquery/jquery)를, 요즘이라면 [`alpinejs/alpine`](https://github.com/alpinejs/alpine)을 골랐을 만한 상황에서는 대안인 [`vuejs/petite-vue`](https://github.com/vuejs/petite-vue)가 더 적합할 수 있습니다.


<a id="guide-quick-start-splitting-up-the-modules"></a>

#### 모듈 분리하기
가이드를 따라 더 깊이 들어갈수록, 코드를 관리하기 쉽게 여러 JavaScript 파일로 분리해야 할 수도 있습니다. 예를 들어:

```html [index.html]
<div id="app"></div>

<script type="module">
  import { createApp } from 'vue'
  import MyComponent from './my-component.js'

  createApp(MyComponent).mount('#app')
</script>
```


**옵션 API**


```js [my-component.js]
export default {
  data() {
    return { count: 0 }
  },
  template: `<div>Count is: {{ count }}</div>`
}
```



**컴포지션 API**


```js [my-component.js]
import { ref } from 'vue'
export default {
  setup() {
    const count = ref(0)
    return { count }
  },
  template: `<div>Count is: {{ count }}</div>`
}
```



위의 `index.html`을 브라우저에서 직접 열면, ES 모듈은 `file://` 프로토콜에서는 동작하지 않기 때문에 오류가 발생합니다. 브라우저가 로컬 파일을 열 때 사용하는 프로토콜이 바로 `file://`입니다.

보안상의 이유로, ES 모듈은 `http://` 프로토콜에서만 동작합니다. 즉, 브라우저가 웹에서 페이지를 열 때 사용하는 프로토콜입니다. 로컬 컴퓨터에서 ES 모듈을 사용하려면, 반드시 `index.html`을 `http://` 프로토콜로 제공해야 하며, 이를 위해 로컬 HTTP 서버가 필요합니다.

로컬 HTTP 서버를 시작하려면, 먼저 [Node.js](https://nodejs.org/en/)가 설치되어 있는지 확인한 후, HTML 파일이 있는 디렉터리에서 커맨드 라인으로 `npx serve`를 실행하세요. 정적 파일을 올바른 MIME 타입으로 제공할 수 있는 다른 HTTP 서버를 사용해도 됩니다.

가져온 컴포넌트의 템플릿(template)이 JavaScript 문자열로 인라인되어 있다는 점을 눈치챘을 수도 있습니다. VS Code를 사용한다면 [es6-string-html](https://marketplace.visualstudio.com/items?itemName=Tobermory.es6-string-html) 확장 프로그램을 설치하고, 문자열 앞에 `/*html*/` 주석을 붙이면 문법 하이라이팅을 받을 수 있습니다.

<a id="guide-quick-start-frameworks"></a>

### 프레임워크
[SSR](05_scaling_typescript_and_best_practices.md#guide-scaling-up-ssr)을 비롯한 다양한 기능을 기본으로 지원하는 Vue 프레임워크들이 있습니다:
- [Nuxt](https://nuxt.com/)
- [Vike](https://vike.dev/)
- [Astro](https://astro.build/)
- [Quasar](https://quasar.dev/)

**참고**
일반적으로 SSR이 필요한 경우에만 프레임워크를 사용하는 것을 권장합니다.

SSR이 필요하지 않다면 [Vite](https://vite.dev/)만 사용해도 충분합니다(위의 [Vue 애플리케이션 생성하기](01_getting_started_and_tutorial.md#guide-quick-start-creating-a-vue-application) 섹션에서 스캐폴딩하는 것이 바로 이 구성입니다).


**안내**
Vue 프레임워크는 일반적으로 내부에서 Vite를 사용하므로, SSR이 필요하지 않다면 Vue 프레임워크 대신 Vite를 직접 사용하는 편이 설정이 더 간단합니다. 다만 프레임워크는 UI 테마와 같은 기능을 추가로 지원하므로, 이런 기능이 Vite만 사용하는 대신 Vue 프레임워크를 선택할 이유가 될 수도 있습니다.


<a id="guide-quick-start-next-steps"></a>

### 다음 단계
[소개](01_getting_started_and_tutorial.md#guide-introduction)를 건너뛰었다면, 나머지 문서를 읽기 전에 꼭 읽어보시길 강력히 권장합니다.

  [가이드 계속하기     가이드는 프레임워크의 모든 측면을 자세히 안내합니다.](02_essentials.md#guide-essentials-application)
  [튜토리얼 체험하기     직접 실습하며 배우는 것을 선호하는 분들을 위한 코스입니다.](01_getting_started_and_tutorial.md#tutorial-index)
  [예제 살펴보기     핵심 기능과 일반적인 UI 작업 예제를 탐색해보세요.](09_style_guide_examples_and_reference.md#examples-index)

---

<a id="guide-extras-ways-of-using-vue"></a>

<a id="guide-extras-ways-of-using-vue-ways-of-using-vue"></a>

## Vue 사용 방법

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/guide/extras/ways-of-using-vue.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/guide/extras/ways-of-using-vue.md

우리는 웹에 "모두에게 맞는 하나의 정답"이 있다고 생각하지 않습니다. 그래서 Vue는 유연하고 점진적으로 도입할 수 있도록 설계되었습니다. 사용 사례에 따라 Vue는 스택의 복잡성, 개발자 경험, 최종 성능 사이에서 최적의 균형을 이룰 수 있도록 다양한 방식으로 사용할 수 있습니다.

<a id="guide-extras-ways-of-using-vue-standalone-script"></a>

### 독립형 스크립트
Vue는 독립형 스크립트 파일로 사용할 수 있습니다. 빌드 단계가 필요하지 않습니다! 이미 백엔드 프레임워크가 대부분의 HTML을 렌더링(rendering)하고 있거나, 프론트엔드 로직이 빌드 단계를 둘 만큼 복잡하지 않다면, Vue를 스택에 통합하기에는 이 방법이 가장 쉽습니다. 이런 경우 Vue를 jQuery의 더 선언적인 대체재로 생각할 수 있습니다.

이전에는 기존 HTML을 점진적으로 향상시키는 데 특화된 [petite-vue](https://github.com/vuejs/petite-vue)라는 대체 배포판을 제공했지만, petite-vue는 더 이상 적극적으로 유지 관리되지 않으며, 마지막 버전은 Vue 3.2.27에서 배포되었습니다.

<a id="guide-extras-ways-of-using-vue-embedded-web-components"></a>

### 내장 웹 컴포넌트
Vue를 사용하여 [표준 웹 컴포넌트](06_reactivity_and_rendering_in_depth.md#guide-extras-web-components)를 만들 수 있으며, 이 컴포넌트(component)는 렌더링 방식에 상관없이 어떤 HTML 페이지에도 삽입할 수 있습니다. 이 옵션을 사용하면 Vue를 소비자와 완전히 독립적인 방식으로 활용할 수 있습니다. 이렇게 만든 웹 컴포넌트는 레거시 애플리케이션, 정적 HTML, 또는 다른 프레임워크로 구축된 애플리케이션에도 삽입할 수 있습니다.

<a id="guide-extras-ways-of-using-vue-single-page-application-spa"></a>

### 싱글 페이지 애플리케이션(SPA)
일부 애플리케이션은 사용자가 한 세션에서 여러 단계를 거치며 상호작용하고, 프론트엔드에서도 복잡한 상태 로직을 처리해야 합니다. 이러한 애플리케이션을 구축하는 가장 좋은 방법은 Vue가 전체 페이지를 제어할 뿐만 아니라, 데이터 업데이트와 내비게이션도 페이지를 새로 고침하지 않고 처리하는 아키텍처를 사용하는 것입니다. 이러한 유형의 애플리케이션을 일반적으로 싱글 페이지 애플리케이션(SPA)이라고 합니다.

Vue는 현대적인 SPA를 구축하기 위한 핵심 라이브러리와 [포괄적인 툴링 지원](05_scaling_typescript_and_best_practices.md#guide-scaling-up-tooling)을 갖추고 있으며, 뛰어난 개발자 경험을 제공합니다. 여기에는 다음이 포함됩니다:

- 클라이언트 사이드 라우터(router)
- 매우 빠른 빌드 도구 체인
- IDE 지원
- 브라우저 개발자 도구
- TypeScript 통합
- 테스트 유틸리티

SPA는 일반적으로 백엔드가 API 엔드포인트를 노출해야 하지만, [Inertia.js](https://inertiajs.com)와 같은 솔루션과 Vue를 결합하여 서버 중심 개발 모델을 유지하면서도 SPA의 이점을 누릴 수 있습니다.

<a id="guide-extras-ways-of-using-vue-fullstack-ssr"></a>

### 풀스택 / SSR
순수 클라이언트 사이드 SPA는 앱이 SEO와 콘텐츠 표시 시간에 민감할 때 문제가 될 수 있습니다. 이는 브라우저가 대부분 비어 있는 HTML 페이지를 받게 되고, JavaScript가 로드될 때까지 아무것도 렌더링하지 못하기 때문입니다.

Vue는 Vue 앱을 서버에서 HTML 문자열로 "렌더링"할 수 있는 일급 API를 제공합니다. 이를 통해 서버는 이미 렌더링된 HTML을 반환할 수 있어, 최종 사용자는 JavaScript가 다운로드되는 동안에도 즉시 콘텐츠를 볼 수 있습니다. 이후 Vue는 클라이언트 측에서 애플리케이션을 "하이드레이트"하여 상호작용이 가능하게 만듭니다. 이를 [서버 사이드 렌더링(SSR)](05_scaling_typescript_and_best_practices.md#guide-scaling-up-ssr)이라고 하며, [Largest Contentful Paint (LCP)](https://web.dev/lcp/)와 같은 Core Web Vital 지표를 크게 개선합니다.

이 패러다임 위에 구축된 더 높은 수준의 [Vue 프레임워크](01_getting_started_and_tutorial.md#guide-quick-start-frameworks)도 있으며, 이러한 프레임워크는 풀스택 애플리케이션을 개발할 수 있도록 SSR을 기본적으로 지원합니다.

<a id="guide-extras-ways-of-using-vue-jamstack-ssg"></a>

### JAMStack / SSG
필요한 데이터가 정적이라면 서버 사이드 렌더링을 미리 수행할 수 있습니다. 즉, 전체 애플리케이션을 HTML로 미리 렌더링하여 정적 파일로 제공할 수 있습니다. 이렇게 하면 사이트 성능이 향상되고, 각 요청마다 동적으로 페이지를 렌더링할 필요가 없으므로 배포도 훨씬 간단해집니다. Vue는 이러한 애플리케이션도 하이드레이트하여 클라이언트에서 풍부한 상호작용을 제공할 수 있습니다. 이 기술은 일반적으로 정적 사이트 생성(SSG)이라고 하며, [JAMStack](https://jamstack.org/what-is-jamstack/)이라고도 불립니다.

SSG에는 싱글 페이지와 멀티 페이지 두 가지 방식이 있습니다. 두 방식 모두 사이트를 정적 HTML로 미리 렌더링하지만, 차이점은 다음과 같습니다:

- 초기 페이지 로드 후, 싱글 페이지 SSG는 페이지를 SPA로 "하이드레이트"합니다. 이 방식은 초기 JS 페이로드가 더 크고 하이드레이션(hydration) 비용도 들지만, 이후 내비게이션은 더 빨라집니다. 전체 페이지를 다시 로드하는 대신 페이지 콘텐츠만 부분적으로 업데이트하면 되기 때문입니다.

- 멀티 페이지 SSG는 내비게이션할 때마다 새 페이지를 로드합니다. 장점은 최소한의 JS만 제공하거나, 상호작용이 필요 없는 경우 JS를 아예 제공하지 않아도 된다는 점입니다! [Astro](https://astro.build/)와 같은 일부 멀티 페이지 SSG 프레임워크는 "부분 하이드레이션"도 지원합니다. 이를 통해 정적 HTML 내부에 Vue 컴포넌트를 사용하여 상호작용이 가능한 "아일랜드"를 만들 수 있습니다.

단순하지 않은 상호작용, 긴 세션, 내비게이션 간에 유지되는 요소/상태가 필요하다면 싱글 페이지 SSG가 더 적합합니다. 그렇지 않다면 멀티 페이지 SSG가 더 나은 선택이 될 수 있습니다.

Vue 팀은 [VitePress](https://vitepress.dev/)라는 정적 사이트 생성기를 유지 관리하고 있습니다. 지금 읽고 있는 이 웹사이트도 VitePress로 구동되며, VitePress는 두 가지 SSG 방식을 모두 지원합니다! 또한 다른 [Vue 프레임워크](01_getting_started_and_tutorial.md#guide-quick-start-frameworks)도 대부분 SSG를 지원하니 확인해보세요.

<a id="guide-extras-ways-of-using-vue-beyond-the-web"></a>

### 웹을 넘어서
Vue는 주로 웹 애플리케이션을 구축하기 위해 설계되었지만, 브라우저에만 국한되지 않습니다. 다음과 같은 작업이 가능합니다:

- [Electron](https://www.electronjs.org/) 또는 [Wails](https://wails.io)로 데스크톱 앱 만들기
- [Ionic Vue](https://ionicframework.com/docs/vue/overview)로 모바일 앱 만들기
- [Quasar](https://quasar.dev/) 또는 [Tauri](https://tauri.app)로 동일한 코드베이스에서 데스크톱 및 모바일 앱 만들기
- [TresJS](https://tresjs.org/)로 3D WebGL 경험 만들기
- Vue의 [Custom Renderer API](08_component_and_advanced_apis.md#api-custom-renderer)를 사용하여 [터미널](https://github.com/vue-terminal/vue-termui)용과 같은 커스텀 렌더러 만들기!

---

<a id="guide-scaling-up-sfc"></a>

<a id="guide-scaling-up-sfc-single-file-components"></a>

## 싱글 파일 컴포넌트

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/guide/scaling-up/sfc.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/guide/scaling-up/sfc.md

<a id="guide-scaling-up-sfc-introduction"></a>

### 소개
Vue 싱글 파일 컴포넌트(**SFC**, `*.vue` 파일)는 컴포넌트(component)의 템플릿(template), 로직, 스타일을 하나의 파일에 담는 형식입니다. 다음 예제에서 세 부분이 어떻게 구성되는지 볼 수 있습니다:


**옵션 API**


```vue
<script>
export default {
  data() {
    return {
      greeting: 'Hello World!'
    }
  }
}
</script>

<template>
  <p class="greeting">{{ greeting }}</p>
</template>

<style>
.greeting {
  color: red;
  font-weight: bold;
}
</style>
```



**컴포지션 API**


```vue
<script setup>
import { ref } from 'vue'
const greeting = ref('Hello World!')
</script>

<template>
  <p class="greeting">{{ greeting }}</p>
</template>

<style>
.greeting {
  color: red;
  font-weight: bold;
}
</style>
```



보시다시피, Vue SFC는 고전적인 HTML, CSS, JavaScript 조합의 자연스러운 확장입니다. `<template>`, `<script>`, `<style>` 블록은 컴포넌트의 뷰, 로직, 스타일링을 동일한 파일에 캡슐화하고 함께 배치합니다. 전체 문법은 [SFC 문법 명세](08_component_and_advanced_apis.md#api-sfc-spec)에 정의되어 있습니다.

<a id="guide-scaling-up-sfc-why-sfc"></a>

### SFC를 사용하는 이유
SFC는 빌드 단계가 필요하지만, 그에 따른 많은 이점이 있습니다:

- 익숙한 HTML, CSS, JavaScript 문법으로 모듈화된 컴포넌트 작성
- [본질적으로 결합된 관심사의 함께 배치](01_getting_started_and_tutorial.md#guide-scaling-up-sfc-what-about-separation-of-concerns)
- 런타임 컴파일 비용 없는 사전 컴파일된 템플릿
- [컴포넌트 범위의 CSS](08_component_and_advanced_apis.md#api-sfc-css-features)
- [컴포지션 API 사용 시 더 편리한 문법](08_component_and_advanced_apis.md#api-sfc-script-setup)
- 템플릿과 스크립트를 교차 분석하여 더 많은 컴파일 타임 최적화
- 템플릿 표현식에 대한 자동 완성 및 타입 체크가 가능한 [IDE 지원](05_scaling_typescript_and_best_practices.md#guide-scaling-up-tooling-ide-support)
- 기본적으로 핫 모듈 교체(HMR) 지원

SFC는 Vue 프레임워크의 대표적인 기능이며, 다음과 같은 상황에서 Vue를 사용할 때 권장되는 접근 방식입니다:

- 싱글 페이지 애플리케이션(SPA)
- 정적 사이트 생성(SSG)
- 더 나은 개발 경험(DX)을 위해 빌드 단계가 정당화될 수 있는, 단순하지 않은 프론트엔드

그렇다고 해도, SFC가 과하다고 느껴질 수 있는 상황이 있다는 점은 저희도 잘 알고 있습니다. 그래서 Vue는 여전히 빌드 단계 없이 순수 JavaScript로도 사용할 수 있습니다. 주로 정적인 HTML에 가벼운 상호작용만 추가하려는 경우, 점진적 향상을 위해 최적화된 6kB의 Vue 서브셋인 [petite-vue](https://github.com/vuejs/petite-vue)도 참고하실 수 있습니다.

<a id="guide-scaling-up-sfc-how-it-works"></a>

### 동작 방식
Vue SFC는 프레임워크 전용 파일 형식이며, [@vue/compiler-sfc](https://github.com/vuejs/core/tree/main/packages/compiler-sfc)에 의해 표준 JavaScript와 CSS로 사전 컴파일되어야 합니다. 컴파일된 SFC는 표준 JavaScript(ES) 모듈이므로, 적절한 빌드 설정이 있다면 SFC를 모듈처럼 import할 수 있습니다:

```js
import MyComponent from './MyComponent.vue'

export default {
  components: {
    MyComponent
  }
}
```

SFC 내부의 `<style>` 태그는 개발 중에는 핫 업데이트를 지원하기 위해 일반 `<style>` 태그로 주입됩니다. 프로덕션에서는 추출되어 하나의 CSS 파일로 병합될 수 있습니다.

[SFC Playground](https://play.vuejs.org/)에서 SFC를 실험해보고 컴파일 과정을 탐색할 수 있습니다.

실제 프로젝트에서는 보통 SFC 컴파일러를 [Vite](https://vite.dev/)나 [Vue CLI](http://cli.vuejs.org/)([webpack](https://webpack.js.org/) 기반) 같은 빌드 도구와 통합해 사용합니다. 그리고 Vue는 SFC를 최대한 빠르게 시작할 수 있도록 공식 스캐폴딩(scaffolding) 도구를 제공합니다. 자세한 내용은 [SFC 도구](05_scaling_typescript_and_best_practices.md#guide-scaling-up-tooling) 섹션을 참고하세요.

<a id="guide-scaling-up-sfc-what-about-separation-of-concerns"></a>

### 관심사의 분리는 어떻게 되나요?
전통적인 웹 개발 배경을 가진 일부 사용자는 SFC가 서로 다른 관심사를 한 곳에 섞는 것에 대해 걱정할 수 있습니다. HTML/CSS/JS가 분리되어야 한다고 배웠기 때문입니다!

여기서 **관심사의 분리와 파일 형식의 분리**를 구분해야 합니다. 관심사를 나누는 목적은 코드를 유지보수하기 쉽게 만드는 데 있습니다. 프론트엔드 애플리케이션이 복잡해질수록 HTML, CSS, JavaScript를 파일 형식에 따라 나누는 것만으로는 이 목적을 달성하기 어렵습니다.

현대 UI 개발에서는 코드베이스를 서로 얽혀 있는 세 개의 거대한 레이어로 나누기보다, 느슨하게 결합된 컴포넌트로 나누어 조합하는 편이 훨씬 더 합리적이라는 사실을 알게 되었습니다. 컴포넌트 내부에서는 템플릿, 로직, 스타일이 본질적으로 결합되어 있으며, 이들을 함께 배치하는 것이 오히려 컴포넌트를 더 응집력 있고 유지보수하기 쉽게 만듭니다.

싱글 파일 컴포넌트의 아이디어가 마음에 들지 않더라도, [Src Imports](08_component_and_advanced_apis.md#api-sfc-spec-src-imports)를 사용하여 JavaScript와 CSS를 별도의 파일로 분리함으로써 핫 리로딩 및 사전 컴파일 기능을 여전히 활용할 수 있습니다.

---

<a id="tutorial-index"></a>

## Vue 3 튜토리얼

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/tutorial/index.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/tutorial/index.md

각 단계의 설명과 시작 코드를 함께 읽으세요. `App/composition.js`는 컴포지션 API, `App/options.js`는 옵션 API 예제입니다. 공통 템플릿은 `App/template.html`에 있습니다. `_hint/`에는 정답에서 달라지는 파일만 있으며, 나머지 파일은 시작 코드와 같습니다.

브라우저 실습: https://ko.vuejs.org/tutorial/

1. [시작하기](01_getting_started_and_tutorial.md#tutorial-src-step-1-description) · [시작 코드](assets/tutorial/src/step-1/App)
2. [선언적 렌더링(rendering)](01_getting_started_and_tutorial.md#tutorial-src-step-2-description) · [시작 코드](assets/tutorial/src/step-2/App)
3. [속성 바인딩(binding)](01_getting_started_and_tutorial.md#tutorial-src-step-3-description) · [시작 코드](assets/tutorial/src/step-3/App)
4. [이벤트 리스너(listener)](01_getting_started_and_tutorial.md#tutorial-src-step-4-description) · [시작 코드](assets/tutorial/src/step-4/App)
5. [폼 바인딩(binding)](01_getting_started_and_tutorial.md#tutorial-src-step-5-description) · [시작 코드](assets/tutorial/src/step-5/App)
6. [조건부 렌더링(rendering)](01_getting_started_and_tutorial.md#tutorial-src-step-6-description) · [시작 코드](assets/tutorial/src/step-6/App)
7. [리스트 렌더링(rendering)](01_getting_started_and_tutorial.md#tutorial-src-step-7-description) · [시작 코드](assets/tutorial/src/step-7/App)
8. [계산된 속성(Computed Property)](01_getting_started_and_tutorial.md#tutorial-src-step-8-description) · [시작 코드](assets/tutorial/src/step-8/App)
9. [라이프사이클(lifecycle)과 템플릿 ref](01_getting_started_and_tutorial.md#tutorial-src-step-9-description) · [시작 코드](assets/tutorial/src/step-9/App)
10. [감시자(Watchers)](01_getting_started_and_tutorial.md#tutorial-src-step-10-description) · [시작 코드](assets/tutorial/src/step-10/App)
11. [컴포넌트(component)](01_getting_started_and_tutorial.md#tutorial-src-step-11-description) · [시작 코드](assets/tutorial/src/step-11/App)
12. [Props](01_getting_started_and_tutorial.md#tutorial-src-step-12-description) · [시작 코드](assets/tutorial/src/step-12/App)
13. [Emits](01_getting_started_and_tutorial.md#tutorial-src-step-13-description) · [시작 코드](assets/tutorial/src/step-13/App)
14. [슬롯(slot)](01_getting_started_and_tutorial.md#tutorial-src-step-14-description) · [시작 코드](assets/tutorial/src/step-14/App)
15. [해냈어요!](01_getting_started_and_tutorial.md#tutorial-src-step-15-description) · [시작 코드](assets/tutorial/src/step-15/App)

---

<a id="tutorial-src-step-1-description"></a>

<a id="tutorial-src-step-1-description-getting-started"></a>

## 시작하기

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/tutorial/src/step-1/description.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/tutorial/src/step-1/description.md

Vue 튜토리얼에 오신 것을 환영합니다!

이 튜토리얼에서는 브라우저에서 직접 코드를 바꾸며 Vue의 기본 기능을 익힙니다. 모든 기능을 다루는 과정은 아니므로, 각 내용을 완벽히 이해하지 못했더라도 다음 단계로 넘어가도 됩니다. 하지만 튜토리얼을 완료한 후에는 각 주제를 더 자세히 다루는 [가이드](01_getting_started_and_tutorial.md#guide-introduction)도 꼭 읽어보시기 바랍니다.

<a id="tutorial-src-step-1-description-prerequisites"></a>

### 사전 준비 사항
이 튜토리얼은 HTML, CSS, JavaScript에 대한 기본적인 이해를 전제로 합니다. 프론트엔드 개발이 완전히 처음이라면, 프레임워크를 바로 시작하는 것보다는 기초를 먼저 익히고 다시 돌아오는 것이 좋습니다! 다른 프레임워크 경험이 있다면 도움이 되겠지만, 필수는 아닙니다.

<a id="tutorial-src-step-1-description-how-to-use-this-tutorial"></a>

### 이 튜토리얼을 사용하는 방법
공식 브라우저 튜토리얼에서는 넓은 화면의 오른쪽, 좁은 화면의 아래에 있는 코드를 수정하면 결과가 즉시 바뀝니다. 각 단계에서 소개하는 기능을 사용해 데모를 완성해 보세요. 막히면 "Show me!" 버튼으로 동작하는 코드를 확인할 수 있습니다.

Vue 2나 다른 프레임워크를 다뤄 본 경험 많은 개발자라면, 이 튜토리얼을 최대한 활용할 수 있도록 몇 가지 설정을 조정할 수 있습니다. 초보자라면 기본 설정을 사용하는 것이 좋습니다.

<details>
<summary>튜토리얼 설정 상세</summary>

- Vue는 두 가지 API 스타일을 제공합니다: 옵션 API와 컴포지션 API. 이 튜토리얼은 두 가지 모두에 맞춰 설계되어 있으며, 상단의 **API Preference** 스위치를 통해 원하는 스타일을 선택할 수 있습니다. [API 스타일에 대해 더 알아보기](01_getting_started_and_tutorial.md#guide-introduction-api-styles).

- SFC 모드와 HTML 모드 간 전환도 가능합니다. SFC 모드는 [싱글 파일 컴포넌트(component)](01_getting_started_and_tutorial.md#guide-introduction-single-file-components) (SFC) 형식의 코드 예제를 보여주며, 대부분의 개발자는 빌드 단계를 거쳐 Vue를 사용할 때 이 방식으로 작업합니다. HTML 모드는 빌드 단계 없이 사용하는 방법을 보여줍니다.


**HTML**


**참고**
직접 만든 애플리케이션에서 빌드 단계 없이 HTML 모드를 사용하려면, import를 다음과 같이 변경해야 합니다:

```js
import { ... } from 'vue/dist/vue.esm-bundler.js'
```

스크립트 내부에서 위와 같이 작성하거나, 빌드 도구에서 `vue`를 올바르게 해석하도록 설정해야 합니다. [Vite](https://vite.dev/)의 예시 설정:

```js [vite.config.js]
export default {
  resolve: {
    alias: {
      vue: 'vue/dist/vue.esm-bundler.js'
    }
  }
}
```

자세한 내용은 [Tooling 가이드의 해당 섹션](05_scaling_typescript_and_best_practices.md#guide-scaling-up-tooling-note-on-in-browser-template-compilation)을 참고하세요.


</details>

준비되셨나요? "Next"를 클릭하여 시작하세요.

---

<a id="tutorial-src-step-2-description"></a>

<a id="tutorial-src-step-2-description-declarative-rendering"></a>

## 선언적 렌더링(rendering)

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/tutorial/src/step-2/description.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/tutorial/src/step-2/description.md

**SFC**


에디터에서 보이는 것은 Vue 싱글 파일 컴포넌트(SFC)입니다. SFC는 함께 묶여야 하는 HTML, CSS, JavaScript를 캡슐화한 재사용 가능한 독립형 코드 블록으로, `.vue` 파일 안에 작성됩니다.


Vue의 핵심 기능은 **선언적 렌더링**입니다. HTML을 확장한 템플릿(template) 문법을 사용하여, JavaScript 상태에 따라 HTML이 어떻게 보여야 하는지 설명할 수 있습니다. 상태가 변경되면 HTML도 자동으로 업데이트됩니다.


**컴포지션 API**


변경될 때 업데이트를 트리거할 수 있는 상태를 **반응형**이라고 간주합니다. Vue의 `reactive()` API를 사용하여 반응형 상태를 선언할 수 있습니다. `reactive()`로 생성된 객체는 일반 객체처럼 동작하는 JavaScript [프록시(Proxy)](https://developer.mozilla.org/ko/docs/Web/JavaScript/Reference/Global_Objects/Proxy)입니다:

```js
import { reactive } from 'vue'

const counter = reactive({
  count: 0
})

console.log(counter.count) // 0
counter.count++
```

`reactive()`는 객체(배열 및 `Map`, `Set`과 같은 내장 타입 포함)에만 동작합니다. 반면, `ref()`는 어떤 값 타입이든 받아 내부 값을 `.value` 속성으로 노출하는 객체를 생성할 수 있습니다:

```js
import { ref } from 'vue'

const message = ref('Hello World!')

console.log(message.value) // "Hello World!"
message.value = 'Changed'
```

`reactive()`와 `ref()`에 대한 자세한 내용은 [가이드 - 반응성(reactivity) 기초](02_essentials.md#guide-essentials-reactivity-fundamentals)에서 다룹니다.


**SFC**


컴포넌트의 `<script setup>` 블록에서 선언된 반응형 상태는 템플릿에서 직접 사용할 수 있습니다. 이렇게 하면 `counter` 객체와 `message` ref의 값을 기반으로 동적 텍스트를 이중 중괄호(mustache) 문법으로 렌더링할 수 있습니다:


**HTML**


`createApp()`에 전달되는 객체는 Vue 컴포넌트입니다. 컴포넌트의 상태는 `setup()` 함수 내부에서 선언하고, 객체로 반환해야 합니다:

```js{2,5}
setup() {
  const counter = reactive({ count: 0 })
  const message = ref('Hello World!')
  return {
    counter,
    message
  }
}
```

반환된 객체의 속성들은 템플릿에서 사용할 수 있게 됩니다. 이렇게 하면 `message`의 값을 기반으로 이중 중괄호(mustache) 문법을 사용해 동적 텍스트를 렌더링할 수 있습니다:


```vue-html
<h1>{{ message }}</h1>
<p>Count is: {{ counter.count }}</p>
```

템플릿에서 `message` ref를 읽을 때는 `.value`를 붙이지 않습니다. 템플릿이 ref를 자동으로 언래핑하기 때문입니다.


**옵션 API**


변경될 때 업데이트를 트리거할 수 있는 상태를 **반응형**이라고 간주합니다. Vue에서 반응형 상태는 컴포넌트에 저장됩니다.  (HTML: 예제 코드에서 `createApp()`에 전달되는 객체는 컴포넌트입니다.)

`data` 컴포넌트 옵션을 사용하여 반응형 상태를 선언할 수 있습니다. 이 옵션은 객체를 반환하는 함수여야 합니다:


**SFC**


```js{3-5}
export default {
  data() {
    return {
      message: 'Hello World!'
    }
  }
}
```



**HTML**


```js{3-5}
createApp({
  data() {
    return {
      message: 'Hello World!'
    }
  }
})
```



`message` 속성은 템플릿에서 사용할 수 있게 됩니다. 이렇게 하면 `message`의 값을 기반으로 이중 중괄호(mustache) 문법을 사용해 동적 텍스트를 렌더링할 수 있습니다:

```vue-html
<h1>{{ message }}</h1>
```



이중 중괄호 내부의 내용은 식별자나 경로에만 국한되지 않습니다. 어떤 유효한 JavaScript 표현식도 사용할 수 있습니다:

```vue-html
<h1>{{ message.split('').reverse().join('') }}</h1>
```


**컴포지션 API**


이제 직접 반응형 상태를 만들어 보고, 이를 템플릿의 `<h1>`에 동적 텍스트 콘텐츠로 사용해 보세요.


**옵션 API**


이제 직접 data 속성을 만들어 보고, 이를 템플릿의 `<h1>` 텍스트 콘텐츠로 사용해 보세요.

---

<a id="tutorial-src-step-3-description"></a>

<a id="tutorial-src-step-3-description-attribute-bindings"></a>

## 속성 바인딩(binding)

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/tutorial/src/step-3/description.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/tutorial/src/step-3/description.md

Vue에서 이중 중괄호(mustache)는 텍스트 보간(interpolation)에만 사용됩니다. 속성을 동적 값에 바인딩하려면 `v-bind` 디렉티브(directive)를 사용합니다:

```vue-html
<div v-bind:id="dynamicId"></div>
```

**디렉티브**는 `v-` 접두사로 시작하는 특별한 속성으로, Vue 템플릿(template) 문법의 일부입니다. 텍스트 보간과 마찬가지로, 디렉티브의 값은 컴포넌트(component)의 상태에 접근할 수 있는 JavaScript 표현식입니다. `v-bind`와 디렉티브 문법의 전체 내용은 [가이드 - 템플릿 문법](02_essentials.md#guide-essentials-template-syntax)에서 다룹니다.

콜론(`:id`) 뒤의 부분은 디렉티브의 "인자"입니다. 여기서, 엘리먼트의 `id` 속성은 컴포넌트 상태의 `dynamicId` 속성과 동기화됩니다.

`v-bind`는 매우 자주 사용되기 때문에, 전용 축약 문법이 있습니다:

```vue-html
<div :id="dynamicId"></div>
```

이제 `<h1>`의 `class`를 `titleClass`에 바인딩해 보세요. 옵션 API에서는 data 속성, 컴포지션 API에서는 ref를 사용합니다. 바인딩이 올바르면 텍스트가 빨간색으로 바뀝니다.

---

<a id="tutorial-src-step-4-description"></a>

<a id="tutorial-src-step-4-description-event-listeners"></a>

## 이벤트 리스너(listener)

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/tutorial/src/step-4/description.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/tutorial/src/step-4/description.md

`v-on` 디렉티브(directive)를 사용하여 DOM 이벤트를 감지할 수 있습니다:

```vue-html
<button v-on:click="increment">{{ count }}</button>
```

자주 사용되기 때문에, `v-on`에는 축약 문법도 있습니다:

```vue-html
<button @click="increment">{{ count }}</button>
```


**옵션 API**


여기서 `increment`는 `methods` 옵션을 사용하여 선언된 함수를 참조합니다:


**SFC**


```js{7-12}
export default {
  data() {
    return {
      count: 0
    }
  },
  methods: {
    increment() {
      // 컴포넌트 상태 업데이트
      this.count++
    }
  }
}
```



**HTML**


```js{7-12}
createApp({
  data() {
    return {
      count: 0
    }
  },
  methods: {
    increment() {
      // 컴포넌트 상태 업데이트
      this.count++
    }
  }
})
```



메서드 내부에서 `this`를 사용하여 컴포넌트(component) 인스턴스(instance)에 접근할 수 있습니다. 컴포넌트 인스턴스는 `data`에서 선언된 데이터 속성을 노출하며, 이 속성들을 변경하여 컴포넌트 상태를 업데이트할 수 있습니다.


**컴포지션 API**


**SFC**


여기서 `increment`는 `<script setup>`에서 선언된 함수를 참조합니다:

```vue{6-9}
<script setup>
import { ref } from 'vue'

const count = ref(0)

function increment() {
  // 컴포넌트 상태 업데이트
  count.value++
}
</script>
```



**HTML**


여기서 `increment`는 `setup()`에서 반환된 객체의 메서드를 참조합니다:

```js{$}
setup() {
  const count = ref(0)

  function increment(e) {
    // 컴포넌트 상태 업데이트
    count.value++
  }

  return {
    count,
    increment
  }
}
```



함수 내부에서 ref를 변경하여 컴포넌트 상태를 업데이트할 수 있습니다.


이벤트 핸들러는 인라인 표현식도 사용할 수 있으며, 수식어(modifiers)를 통해 일반적인 작업을 간소화할 수 있습니다. 이러한 세부 사항은 [가이드 - 이벤트 핸들링](02_essentials.md#guide-essentials-event-handling)에서 다룹니다.

이제 `increment`를 구현하고 `v-on`으로 버튼에 연결해 보세요. 옵션 API에서는 `methods` 안에, 컴포지션 API에서는 함수로 정의합니다.

---

<a id="tutorial-src-step-5-description"></a>

<a id="tutorial-src-step-5-description-form-bindings"></a>

## 폼 바인딩(binding)

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/tutorial/src/step-5/description.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/tutorial/src/step-5/description.md

`v-bind`와 `v-on`을 함께 사용하면 폼 입력 요소에서 양방향 바인딩을 만들 수 있습니다:

```vue-html
<input :value="text" @input="onInput">
```


**옵션 API**


```js
methods: {
  onInput(e) {
    // v-on 핸들러는 네이티브 DOM 이벤트를
    // 인자로 받습니다.
    this.text = e.target.value
  }
}
```



**컴포지션 API**


```js
function onInput(e) {
  // v-on 핸들러는 네이티브 DOM 이벤트를
  // 인자로 받습니다.
  text.value = e.target.value
}
```



입력 상자에 타이핑해 보세요. 입력할 때마다 `<p>`의 텍스트가 업데이트되는 것을 볼 수 있습니다.

양방향 바인딩을 더 간단하게 하기 위해, Vue는 `v-model`이라는 디렉티브(directive)를 제공합니다. 이는 본질적으로 위 예시에 대한 문법적 설탕(syntactic sugar)입니다:

```vue-html
<input v-model="text">
```

`v-model`은 `<input>`의 값을 바인딩된 상태와 자동으로 동기화하므로, 더 이상 이벤트 핸들러를 사용할 필요가 없습니다.

`v-model`은 텍스트 입력뿐만 아니라 체크박스, 라디오 버튼, 셀렉트 드롭다운 등 다른 입력 타입에서도 동작합니다. 더 자세한 내용은 [가이드 - 폼 바인딩](02_essentials.md#guide-essentials-forms)에서 다룹니다.

이제 코드를 `v-model`을 사용하도록 리팩터링해 보세요.

---

<a id="tutorial-src-step-6-description"></a>

<a id="tutorial-src-step-6-description-conditional-rendering"></a>

## 조건부 렌더링(rendering)

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/tutorial/src/step-6/description.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/tutorial/src/step-6/description.md

`v-if` 디렉티브(directive)를 사용하여 요소를 조건부로 렌더링할 수 있습니다:

```vue-html
<h1 v-if="awesome">Vue는 멋져요!</h1>
```

이 `<h1>`은 `awesome`의 값이 [참 같은 값(truthy)](https://developer.mozilla.org/ko/docs/Glossary/Truthy)일 때만 렌더링됩니다. 만약 `awesome`이 [거짓 같은 값(falsy)](https://developer.mozilla.org/ko/docs/Glossary/Falsy)으로 변경되면, DOM에서 제거됩니다.

또한, `v-else`와 `v-else-if`를 사용하여 조건의 다른 분기를 나타낼 수 있습니다:

```vue-html
<h1 v-if="awesome">Vue는 멋져요!</h1>
<h1 v-else>오 안돼 😢</h1>
```

현재 데모에서는 두 개의 `<h1>`이 동시에 표시되고, 버튼은 아무 동작도 하지 않습니다. 이 두 `<h1>`에 `v-if`와 `v-else` 디렉티브를 추가하고, 버튼으로 두 요소를 전환할 수 있도록 `toggle()` 메서드를 구현해 보세요.

`v-if`에 대한 자세한 내용: [가이드 - 조건부 렌더링](02_essentials.md#guide-essentials-conditional)

---

<a id="tutorial-src-step-7-description"></a>

<a id="tutorial-src-step-7-description-list-rendering"></a>

## 리스트 렌더링(rendering)

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/tutorial/src/step-7/description.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/tutorial/src/step-7/description.md

`v-for` 디렉티브(directive)를 사용하여 소스 배열을 기반으로 요소 목록을 렌더링할 수 있습니다:

```vue-html
<ul>
  <li v-for="todo in todos" :key="todo.id">
    {{ todo.text }}
  </li>
</ul>
```

여기서 `todo`는 현재 반복 중인 배열 요소를 나타내는 지역 변수입니다. 함수의 스코프와 유사하게, `v-for`가 선언된 요소 자체 또는 그 내부에서만 접근할 수 있습니다.

각 todo 객체에 고유한 `id`를 부여하고, 이를 각 `<li>`의 [특수 `key` 속성](08_component_and_advanced_apis.md#api-built-in-special-attributes-key)으로 바인딩(binding)하는 것에 주목하세요. `key`는 Vue가 각 `<li>`를 배열에서 해당 객체의 위치에 맞게 정확하게 이동시킬 수 있도록 해줍니다.

목록을 업데이트하는 방법에는 두 가지가 있습니다:

1. 소스 배열의 [변경 메서드](https://stackoverflow.com/questions/9009879/which-javascript-array-functions-are-mutating)를 호출하는 방법:


**컴포지션 API**


   ```js
   todos.value.push(newTodo)
   ```



**옵션 API**


   ```js
   this.todos.push(newTodo)
   ```



2. 배열을 새 배열로 교체하는 방법:


**컴포지션 API**


   ```js
   todos.value = todos.value.filter(/* ... */)
   ```



**옵션 API**


   ```js
   this.todos = this.todos.filter(/* ... */)
   ```



여기 간단한 할 일 목록이 있습니다. 목록이 작동하도록 `addTodo()`와 `removeTodo()` 메서드의 로직을 구현해 보세요!

`v-for`에 대한 자세한 내용: [가이드 - 리스트 렌더링](02_essentials.md#guide-essentials-list)

---

<a id="tutorial-src-step-8-description"></a>

<a id="tutorial-src-step-8-description-computed-property"></a>

## 계산된 속성(Computed Property)

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/tutorial/src/step-8/description.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/tutorial/src/step-8/description.md

이전 단계의 할 일 목록을 계속 확장해 봅시다. 여기서는 이미 각 할 일에 토글 기능을 추가했습니다. 이 기능은 각 할 일 객체에 `done` 속성을 추가하고, 이를 체크박스에 `v-model`로 바인딩(binding)하는 방식으로 구현되어 있습니다:

```vue-html{2}
<li v-for="todo in todos">
  <input type="checkbox" v-model="todo.done">
  ...
</li>
```

다음으로 추가할 개선점은 이미 완료된 할 일을 숨길 수 있는 기능입니다. 데모에는 `hideCompleted` 상태를 토글하는 버튼이 미리 준비되어 있습니다. 하지만 그 상태에 따라 다른 목록 항목을 어떻게 렌더링(rendering)할 수 있을까요?


**옵션 API**


[계산된 속성](02_essentials.md#guide-essentials-computed)을 소개합니다. `computed` 옵션을 사용하여 다른 속성으로부터 반응적으로 계산되는 속성을 선언할 수 있습니다:


**SFC**


```js
export default {
  // ...
  computed: {
    filteredTodos() {
      // `this.hideCompleted`에 따라 필터링된 할 일 목록을 반환합니다.
    }
  }
}
```



**HTML**


```js
createApp({
  // ...
  computed: {
    filteredTodos() {
      // `this.hideCompleted`에 따라 필터링된 할 일 목록을 반환합니다.
    }
  }
})
```



**컴포지션 API**


[`computed()`](02_essentials.md#guide-essentials-computed)를 소개합니다. 다른 반응형 데이터 소스를 기반으로 `.value`를 계산하는 계산 ref를 만들 수 있습니다:


**SFC**


```js{8-11}
import { ref, computed } from 'vue'

const hideCompleted = ref(false)
const todos = ref([
  /* ... */
])

const filteredTodos = computed(() => {
  // `todos.value`와 `hideCompleted.value`에 따라
  // 필터링된 할 일 목록을 반환합니다.
})
```



**HTML**


```js{10-13}
import { createApp, ref, computed } from 'vue'

createApp({
  setup() {
    const hideCompleted = ref(false)
    const todos = ref([
      /* ... */
    ])

    const filteredTodos = computed(() => {
      // `todos.value`와 `hideCompleted.value`에 따라
      // 필터링된 할 일 목록을 반환합니다.
    })

    return {
      // ...
    }
  }
})
```



```diff
- <li v-for="todo in todos">
+ <li v-for="todo in filteredTodos">
```

계산된 속성은 계산에 사용된 다른 반응형 상태를 의존성으로 추적합니다. 결과를 캐시하고, 의존성이 변경될 때 자동으로 업데이트합니다.

이제 `filteredTodos` 계산된 속성을 추가하고, 그 계산 로직을 구현해 보세요! 올바르게 구현했다면, 완료된 항목 숨기기 상태에서 할 일을 체크하면 즉시 해당 항목이 숨겨져야 합니다.

---

<a id="tutorial-src-step-9-description"></a>

<a id="tutorial-src-step-9-description-lifecycle-and-template-refs"></a>

## 라이프사이클(lifecycle)과 템플릿 ref

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/tutorial/src/step-9/description.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/tutorial/src/step-9/description.md

지금까지 Vue는 반응성(reactivity)과 선언적 렌더링(rendering) 덕분에 모든 DOM 업데이트를 자동으로 처리해주었습니다. 하지만, 결국에는 DOM을 수동으로 다루어야 하는 경우가 생기기 마련입니다.

템플릿(template) 내의 요소를 가리키는 참조인 **템플릿 ref**를 요청하려면 [특수 `ref` 속성](08_component_and_advanced_apis.md#api-built-in-special-attributes-ref)을 사용할 수 있습니다:

```vue-html
<p ref="pElementRef">hello</p>
```


**컴포지션 API**


요소에 접근하려면 `pElementRef`라는 이름의 ref를 선언합니다. HTML 방식에서는 `setup()`이 반환하는 객체에도 포함해야 합니다.


**SFC**


```js
const pElementRef = ref(null)
```



**HTML**


```js
setup() {
  const pElementRef = ref(null)

  return {
    pElementRef
  }
}
```



`<script setup>`이나 `setup()`이 실행될 때는 요소가 아직 없으므로 ref의 초기값은 `null`입니다. 컴포넌트가 **마운트(mount)**된 뒤에 템플릿 ref로 요소에 접근할 수 있습니다.

마운트 후에 코드를 실행하려면 `onMounted()` 함수를 사용할 수 있습니다:


**SFC**


```js
import { onMounted } from 'vue'

onMounted(() => {
  // 컴포넌트가 이제 마운트되었습니다.
})
```



**HTML**


```js
import { onMounted } from 'vue'

createApp({
  setup() {
    onMounted(() => {
      // 컴포넌트가 이제 마운트되었습니다.
    })
  }
})
```



**옵션 API**


해당 요소는 `this.$refs`에 `this.$refs.pElementRef`로 노출됩니다. 하지만, 컴포넌트가 **마운트**된 후에만 접근할 수 있습니다.

마운트 후에 코드를 실행하려면 `mounted` 옵션을 사용할 수 있습니다:


**SFC**


```js
export default {
  mounted() {
    // 컴포넌트가 이제 마운트되었습니다.
  }
}
```



**HTML**


```js
createApp({
  mounted() {
    // 컴포넌트가 이제 마운트되었습니다.
  }
})
```



이처럼 컴포넌트의 특정 시점에 콜백을 등록하는 API를 **라이프사이클 훅(hook)**이라고 부릅니다. 옵션 API의 `created`, `updated`와 컴포지션 API의 `onUpdated`, `onUnmounted`도 다른 시점에 실행되는 훅입니다. 각 시점은 [라이프사이클 다이어그램](02_essentials.md#guide-essentials-lifecycle-lifecycle-diagram)에서 확인할 수 있습니다.

이제 마운트 훅에서 `<p>`의 `textContent`를 바꿔 보세요. 옵션 API에서는 `mounted` 안에서 `this.$refs.pElementRef`로, 컴포지션 API에서는 `onMounted` 안에서 `pElementRef.value`로 요소에 접근합니다.

---

<a id="tutorial-src-step-10-description"></a>

<a id="tutorial-src-step-10-description-watchers"></a>

## 감시자(Watchers)

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/tutorial/src/step-10/description.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/tutorial/src/step-10/description.md

때때로 "부수 효과(side effects)"를 반응적으로 수행해야 하는 경우가 있습니다. 숫자가 변경될 때마다 콘솔에 로그를 남기는 것이 그런 예입니다. 이러한 작업은 감시자(watcher)로 처리할 수 있습니다:


**컴포지션 API**


```js
import { ref, watch } from 'vue'

const count = ref(0)

watch(count, (newCount) => {
  // 네, console.log()는 부수 효과입니다.
  console.log(`new count is: ${newCount}`)
})
```

`watch()`는 ref를 직접 감시할 수 있으며, `count`의 값이 변경될 때마다 콜백(callback)이 실행됩니다. `watch()`는 다른 유형의 데이터 소스도 감시할 수 있습니다. 자세한 내용은 [가이드 - 감시자](02_essentials.md#guide-essentials-watchers)에서 확인할 수 있습니다.


**옵션 API**


```js
export default {
  data() {
    return {
      count: 0
    }
  },
  watch: {
    count(newCount) {
      // 네, console.log()는 부수 효과입니다.
      console.log(`new count is: ${newCount}`)
    }
  }
}
```

여기서는 `watch` 옵션을 사용하여 `count` 속성의 변화를 감시하고 있습니다. watch 콜백은 `count`가 변경될 때 호출되며, 새로운 값이 인자로 전달됩니다. 자세한 내용은 [가이드 - 감시자](02_essentials.md#guide-essentials-watchers)에서 확인할 수 있습니다.


콘솔에 로그를 남기는 것보다 더 실용적인 예시는 ID가 변경될 때마다 새로운 데이터를 가져오는 것입니다. 아래 코드는 컴포넌트(component)가 마운트(mount)될 때 mock API에서 todos 데이터를 가져옵니다. 또한, 가져올 todo ID를 증가시키는 버튼도 있습니다. 버튼을 클릭할 때마다 새로운 todo를 가져오도록 감시자를 구현해 보세요.

---

<a id="tutorial-src-step-11-description"></a>

<a id="tutorial-src-step-11-description-components"></a>

## 컴포넌트(component)

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/tutorial/src/step-11/description.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/tutorial/src/step-11/description.md

지금까지는 단일 컴포넌트만 다루었습니다. 실제 Vue 애플리케이션은 일반적으로 중첩된 컴포넌트로 만들어집니다.

부모 컴포넌트는 템플릿(template)에서 다른 컴포넌트를 자식 컴포넌트로 렌더링(rendering)할 수 있습니다. 자식 컴포넌트를 사용하려면 먼저 이를 import해야 합니다:


**컴포지션 API**


**SFC**


```js
import ChildComp from './ChildComp.vue'
```



**옵션 API**


**SFC**


```js
import ChildComp from './ChildComp.vue'

export default {
  components: {
    ChildComp
  }
}
```

또한 `components` 옵션을 사용하여 컴포넌트를 등록해야 합니다. 여기서는 객체 속성 단축 표기법으로 `ChildComp` 컴포넌트를 `ChildComp` 키로 등록하고 있습니다.


**SFC**


그런 다음, 템플릿에서 다음과 같이 컴포넌트를 사용할 수 있습니다:

```vue-html
<ChildComp />
```



**HTML**


```js
import ChildComp from './ChildComp.js'

createApp({
  components: {
    ChildComp
  }
})
```

또한 `components` 옵션을 사용하여 컴포넌트를 등록해야 합니다. 여기서는 객체 속성 단축 표기법으로 `ChildComp` 컴포넌트를 `ChildComp` 키로 등록하고 있습니다.

템플릿을 DOM에서 작성하고 있기 때문에 브라우저의 파싱 규칙이 적용되는데, 이 규칙은 태그 이름의 대소문자를 구분하지 않습니다. 따라서 자식 컴포넌트를 참조할 때는 케밥 케이스(kebab-case) 이름을 사용해야 합니다:

```vue-html
<child-comp></child-comp>
```



이제 직접 시도해 보세요. 자식 컴포넌트를 import하고 템플릿에서 렌더링하면 됩니다.

---

<a id="tutorial-src-step-12-description"></a>

<a id="tutorial-src-step-12-description-props"></a>

## Props

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/tutorial/src/step-12/description.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/tutorial/src/step-12/description.md

자식 컴포넌트(component)는 **props**를 통해 부모로부터 입력을 받을 수 있습니다. 먼저, 자식 컴포넌트는 자신이 받을 props를 선언해야 합니다:


**컴포지션 API**


**SFC**


```vue [ChildComp.vue]
<script setup>
const props = defineProps({
  msg: String
})
</script>
```

`defineProps()`는 컴파일 타임 매크로이므로 import할 필요가 없습니다. 선언이 완료되면 `msg` prop을 자식 컴포넌트의 템플릿(template)에서 사용할 수 있습니다. 또한 `defineProps()`가 반환하는 객체를 통해 JavaScript에서도 접근할 수 있습니다.


**HTML**


```js
// 자식 컴포넌트에서
export default {
  props: {
    msg: String
  },
  setup(props) {
    // props.msg에 접근
  }
}
```

선언이 완료되면, `msg` prop은 `this`에 노출되며 자식 컴포넌트의 템플릿에서 사용할 수 있습니다. 받은 props는 첫 번째 인자로 `setup()`에 전달됩니다.


**옵션 API**


```js
// 자식 컴포넌트에서
export default {
  props: {
    msg: String
  }
}
```

선언이 완료되면, `msg` prop은 `this`에 노출되며 자식 컴포넌트의 템플릿에서 사용할 수 있습니다.


부모는 props를 속성처럼 자식에게 전달할 수 있습니다. 동적 값을 전달하려면 `v-bind` 문법을 사용할 수도 있습니다:


**SFC**


```vue-html
<ChildComp :msg="greeting" />
```



**HTML**


```vue-html
<child-comp :msg="greeting"></child-comp>
```



이제 에디터에서 직접 시도해 보세요.

---

<a id="tutorial-src-step-13-description"></a>

<a id="tutorial-src-step-13-description-emits"></a>

## Emits

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/tutorial/src/step-13/description.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/tutorial/src/step-13/description.md

자식 컴포넌트(component)는 props를 받을 뿐만 아니라 부모에게 이벤트를 발생시킬 수도 있습니다:


**컴포지션 API**


**SFC**


```vue
<script setup>
// 발생시킬 이벤트 선언
const emit = defineEmits(['response'])

// 인자를 포함하여 이벤트 발생
emit('response', '자식으로부터 hello')
</script>
```



**HTML**


```js
export default {
  // 발생시킬 이벤트 선언
  emits: ['response'],
  setup(props, { emit }) {
    // 인자를 포함하여 이벤트 발생
    emit('response', '자식으로부터 hello')
  }
}
```



**옵션 API**


```js
export default {
  // 발생시킬 이벤트 선언
  emits: ['response'],
  created() {
    // 인자를 포함하여 이벤트 발생
    this.$emit('response', '자식으로부터 hello')
  }
}
```



옵션 API의 `this.$emit()`과 컴포지션 API의 `emit()`은 모두 첫 번째 인자로 이벤트 이름을 받습니다. 그 뒤의 인자는 이벤트 리스너에 전달됩니다.

부모는 `v-on`을 사용하여 자식이 발생시킨 이벤트를 리스닝할 수 있습니다. 아래 예시에서 핸들러는 자식의 emit 호출에서 전달된 추가 인자를 받아 로컬 상태에 할당합니다:


**SFC**


```vue-html
<ChildComp @response="(msg) => childMsg = msg" />
```



**HTML**


```vue-html
<child-comp @response="(msg) => childMsg = msg"></child-comp>
```



이제 에디터에서 직접 시도해 보세요.

---

<a id="tutorial-src-step-14-description"></a>

<a id="tutorial-src-step-14-description-slots"></a>

## 슬롯(slot)

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/tutorial/src/step-14/description.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/tutorial/src/step-14/description.md

props를 통해 데이터를 전달하는 것 외에도, 부모 컴포넌트(component)는 **슬롯**을 통해 템플릿(template) 조각을 자식에게 전달할 수 있습니다:


**SFC**


```vue-html
<ChildComp>
  이것은 슬롯 콘텐츠입니다!
</ChildComp>
```



**HTML**


```vue-html
<child-comp>
  이것은 슬롯 콘텐츠입니다!
</child-comp>
```



자식 컴포넌트에서는 `<slot>` 엘리먼트를 아웃렛으로 사용하여 부모로부터 전달된 슬롯 콘텐츠를 렌더링(rendering)할 수 있습니다:


**SFC**


```vue-html
<!-- 자식 템플릿에서 -->
<slot/>
```



**HTML**


```vue-html
<!-- 자식 템플릿에서 -->
<slot></slot>
```



`<slot>` 아웃렛 내부의 콘텐츠는 "폴백(fallback)" 콘텐츠로 간주되어, 부모가 슬롯 콘텐츠를 전달하지 않은 경우에 표시됩니다:

```vue-html
<slot>폴백 콘텐츠</slot>
```

아직 `<ChildComp>`에 슬롯 콘텐츠를 전달하지 않았으므로 폴백 콘텐츠가 표시됩니다. 이제 부모의 `msg`를 자식의 슬롯 콘텐츠로 전달해 보세요.

---

<a id="tutorial-src-step-15-description"></a>

<a id="tutorial-src-step-15-description-you-did-it"></a>

## 해냈어요!

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/tutorial/src/step-15/description.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/tutorial/src/step-15/description.md

튜토리얼을 완료하셨습니다!

튜토리얼에서는 기본 기능을 빠르게 살펴봤습니다. 로컬 프로젝트를 만들거나 각 기능의 세부 동작을 익히려면 다음 자료로 이어갈 수 있습니다.

- [빠른 시작](01_getting_started_and_tutorial.md#guide-quick-start)을 따라 실제 Vue 프로젝트를 내 컴퓨터에 설정해 보세요.

- 지금까지 배운 모든 주제를 더 자세하게 다루는 [메인 가이드](02_essentials.md#guide-essentials-application)를 살펴보세요. 더 많은 내용도 포함되어 있습니다.

- 좀 더 실용적인 [예제들](09_style_guide_examples_and_reference.md#examples-index)도 확인해 보세요.

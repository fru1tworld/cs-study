# Vue 3 스타일 가이드, 예제와 참고 자료

프로젝트에서 참고할 스타일 규칙과 실행 예제, FAQ, 릴리스 정책을 모았습니다. 용어집은 본문에서 쓰인 개념을 다시 확인할 때, 에러 코드 표는 프로덕션 로그를 해석할 때 사용할 수 있습니다.

## 목차

- [스타일 가이드](#style-guide-index)
- [우선 순위 A 규칙: 필수](#style-guide-rules-essential)
- [우선순위 B 규칙: 강력히 권장](#style-guide-rules-strongly-recommended)
- [우선 순위 C 규칙: 권장](#style-guide-rules-recommended)
- [우선 순위 D 규칙: 주의해서 사용하기](#style-guide-rules-use-with-caution)
- [Vue 3 예제](#examples-index)
- [자주 묻는 질문](#about-faq)
- [릴리스](#about-releases)
- [용어집](#glossary-index)
- [프로덕션 에러 코드 참조](#error-reference-index)

---

<a id="style-guide-index"></a>

<a id="style-guide-index-style-guide"></a>

## 스타일 가이드

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/style-guide/index.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/style-guide/index.md

Vue 공식 스타일 가이드는 Vue 코드에서 오류와 안티패턴을 피하고 팀의 작성 규칙을 정할 때 참고할 수 있습니다. 모든 팀과 프로젝트에 똑같이 맞는 규칙은 아니므로, 경험과 기술 스택, 중요하게 여기는 기준에 따라 조정해 사용합니다.

이 가이드는 세미콜론, 후행 쉼표, HTML 속성값의 따옴표처럼 일반적인 JavaScript와 HTML 스타일은 대부분 다루지 않습니다. 다만 Vue 코드에서 도움이 되는 패턴은 예외적으로 포함합니다.

규칙은 필요성과 적용 범위에 따라 네 가지로 나뉩니다.

<a id="style-guide-index-rule-categories"></a>

### 규칙 범주
<a id="style-guide-index-priority-a-essential-error-prevention"></a>

#### 우선 순위 A: 필수 (오류 방지)
이 규칙들은 오류를 방지하는 데 도움이 되므로, 반드시 숙지하고 따라야 합니다. 예외는 있을 수 있지만, 매우 드물어야 하며 JavaScript와 Vue에 대한 전문 지식을 가진 사람만이 만들어야 합니다.

- [모든 우선 순위 A 규칙 보기](09_style_guide_examples_and_reference.md#style-guide-rules-essential)

<a id="style-guide-index-priority-b-strongly-recommended"></a>

#### 우선 순위 B: 강력히 권장
이 규칙들은 대부분의 프로젝트에서 가독성 및/또는 개발자 경험을 향상시키는 것으로 밝혀졌습니다. 이 규칙들을 위반해도 코드는 여전히 실행되지만, 위반하는 경우는 드물어야 하며 정당한 이유가 있어야 합니다.

- [모든 우선 순위 B 규칙 보기](09_style_guide_examples_and_reference.md#style-guide-rules-strongly-recommended)

<a id="style-guide-index-priority-c-recommended"></a>

#### 우선 순위 C: 권장
여러 가지 동등하게 좋은 옵션이 존재할 때, 일관성을 유지하기 위해 임의적인 선택을 할 수 있습니다. 이 규칙에서는 각각의 허용 가능한 옵션을 설명하고 기본 선택을 제안합니다. 즉, 일관성을 유지하고 좋은 이유가 있다면 코드베이스에서 다른 선택을 자유롭게 할 수 있습니다. 하지만 좋은 이유가 있어야 합니다! 커뮤니티 표준에 적응함으로써 다음과 같은 이점이 있습니다:

1. 대부분의 커뮤니티 코드를 더 쉽게 파악할 수 있도록 두뇌를 훈련시킵니다.
2. 대부분의 커뮤니티 코드 예제를 수정 없이 복사 및 붙여넣을 수 있습니다.
3. 새로운 직원이 Vue에 관해 선호되는 코딩 스타일에 이미 익숙할 가능성이 높습니다.

- [모든 우선 순위 C 규칙 보기](09_style_guide_examples_and_reference.md#style-guide-rules-recommended)

<a id="style-guide-index-priority-d-use-with-caution"></a>

#### 우선 순위 D: 주의해서 사용하기
Vue의 일부 기능은 드문 에지 케이스에 대응하거나 레거시 코드베이스에서 원활하게 마이그레이션하도록 돕기 위해 존재합니다. 그러나 과도하게 사용되면 코드를 유지 관리하기 어렵게 만들거나 버그의 원인이 될 수 있습니다. 이 규칙들은 잠재적으로 위험한 기능이 무엇인지 밝히고, 언제, 왜 피해야 하는지 설명합니다.

- [모든 우선 순위 D 규칙 보기](09_style_guide_examples_and_reference.md#style-guide-rules-use-with-caution)

---

<a id="style-guide-rules-essential"></a>

<a id="style-guide-rules-essential-priority-a-rules-essential"></a>

## 우선 순위 A 규칙: 필수

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/style-guide/rules-essential.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/style-guide/rules-essential.md

이 규칙들은 오류를 방지하는 데 도움이 되므로, 반드시 숙지하고 따라야 합니다. 예외는 있을 수 있지만, 매우 드물어야 하며 JavaScript와 Vue에 대한 전문 지식을 가진 사람만이 만들어야 합니다.

<a id="style-guide-rules-essential-use-multi-word-component-names"></a>

### 멀티 워드 컴포넌트 이름 사용
사용자 컴포넌트(component) 이름은 루트 `App` 컴포넌트를 제외하면 항상 멀티 워드여야 합니다. 이는 모든 HTML 요소가 단어 하나로 구성되어 있으므로, 기존 및 미래의 HTML 요소와의 [충돌을 방지](https://html.spec.whatwg.org/multipage/custom-elements.html#valid-custom-element-name)합니다.

**잘못된 예**

```vue-html
<!-- 사전 컴파일된 템플릿에서 -->
<Item />

<!-- in-DOM 템플릿에서 -->
<item></item>
```

**좋은 예**

```vue-html
<!-- 사전 컴파일된 템플릿에서 -->
<TodoItem />

<!-- in-DOM 템플릿에서 -->
<todo-item></todo-item>
```



<a id="style-guide-rules-essential-use-detailed-prop-definitions"></a>

### 상세한 prop 정의 사용
커밋된 코드에서는 prop 정의가 가능한 한 상세해야 하며, 최소한 타입을 명시해야 합니다.

**상세한 설명**
상세한 [prop 정의](03_components_and_reusability.md#guide-components-props-prop-validation)는 두 가지 장점이 있습니다:

- 컴포넌트의 API를 문서화하여 컴포넌트의 사용 방법을 쉽게 파악할 수 있습니다.
- 개발 중에 Vue는 잘못된 형식의 props가 컴포넌트에 제공될 경우 경고를 표시하여 오류의 잠재적 원인을 잡을 수 있도록 도와줍니다.


**옵션 API**

**잘못된 예**

```js
// 이것은 프로토타이핑할 때만 괜찮습니다
props: ['status']
```

**좋은 예**

```js
props: {
  status: String
}
```

```js
// 더 좋은 예!
props: {
  status: {
    type: String,
    required: true,

    validator: value => {
      return [
        'syncing',
        'synced',
        'version-conflict',
        'error'
      ].includes(value)
    }
  }
}
```



**컴포지션 API**

**잘못된 예**

```js
// 이것은 프로토타이핑할 때만 괜찮습니다
const props = defineProps(['status'])
```

**좋은 예**

```js
const props = defineProps({
  status: String
})
```

```js
// 더 좋은 예!

const props = defineProps({
  status: {
    type: String,
    required: true,

    validator: (value) => {
      return ['syncing', 'synced', 'version-conflict', 'error'].includes(
        value
      )
    }
  }
})
```



<a id="style-guide-rules-essential-use-keyed-v-for"></a>

### `v-for`에 `key` 사용하기
하위 트리의 내부 컴포넌트 상태를 유지하기 위해, 컴포넌트에 `v-for`를 사용할 때는 `key`가 _항상_ 필요합니다. 심지어 요소에 대해서도, 애니메이션에서의 [객체의 일관성](https://bost.ocks.org/mike/constancy/)과 같은 예측 가능한 동작을 유지하는 것이 좋은 관행입니다.

**상세한 설명**
할 일 목록이 있다고 가정해 봅시다:


**옵션 API**


```js
data() {
  return {
    todos: [
      {
        id: 1,
        text: 'v-for 사용법 배우기'
      },
      {
        id: 2,
        text: 'key 사용법 배우기'
      }
    ]
  }
}
```



**컴포지션 API**


```js
const todos = ref([
  {
    id: 1,
    text: 'v-for 사용법 배우기'
  },
  {
    id: 2,
    text: 'key 사용법 배우기'
  }
])
```



이 할 일들을 알파벳순으로 정렬하면 Vue는 DOM 변경 비용을 줄이는 방향으로 렌더링(rendering)을 최적화합니다. 그 과정에서 첫 번째 할 일 요소를 삭제한 뒤 목록 끝에 다시 추가할 수도 있습니다.

그런데 요소를 삭제하지 않고 유지해야 할 때도 있습니다. `<transition-group>`으로 정렬 과정을 애니메이션으로 보여 주거나, `<input>`의 포커스를 유지해야 하는 경우입니다. 각 항목에 `:key="todo.id"`처럼 고유한 키를 지정하면 Vue가 같은 항목을 구분할 수 있어 이런 동작을 예측하기 쉬워집니다.

가이드는 이런 경우를 매번 판단하지 않아도 되도록 _항상_ 고유한 키를 추가할 것을 권장합니다. 객체의 일관성이 필요 없고 성능이 중요한 드문 경우에만 예외를 고려합니다.

**잘못된 예**

```vue-html
<ul>
  <li v-for="todo in todos">
    {{ todo.text }}
  </li>
</ul>
```

**좋은 예**

```vue-html
<ul>
  <li
    v-for="todo in todos"
    :key="todo.id"
  >
    {{ todo.text }}
  </li>
</ul>
```



<a id="style-guide-rules-essential-avoid-v-if-with-v-for"></a>

### `v-if`와 `v-for`를 함께 사용하지 않기
**`v-for`가 있는 같은 요소에 `v-if`를 사용하지 마세요.**

두 디렉티브를 함께 쓰고 싶어지는 경우는 주로 다음 두 가지입니다.

- 목록의 항목을 필터링하기 위해 (예: `v-for="user in users" v-if="user.isActive"`). 이 경우에는 `users`를 새로운 계산된 속성(computed property)으로 대체하여 필터링된 목록을 반환하도록 합니다 (예: `activeUsers`).

- 목록이 숨겨져야 할 경우 목록을 렌더링하지 않기 위해 (예: `v-for="user in users" v-if="shouldShowUsers"`). 이 경우에는 `v-if`를 컨테이너 요소 (예: `ul`, `ol`)로 이동시킵니다.

**상세한 설명**
Vue는 `v-if`를 `v-for`보다 먼저 평가합니다. 따라서 다음 템플릿에서는 `v-if`를 평가하는 시점에 반복 변수 `user`가 아직 없어 오류가 발생합니다.

```vue-html
<ul>
  <li
    v-for="user in users"
    v-if="user.isActive"
    :key="user.id"
  >
    {{ user.name }}
  </li>
</ul>
```

먼저 계산된 속성에서 목록을 필터링한 뒤 그 결과를 반복하면 문제를 해결할 수 있습니다.


**옵션 API**


```js
computed: {
  activeUsers() {
    return this.users.filter(user => user.isActive)
  }
}
```



**컴포지션 API**


```js
const activeUsers = computed(() => {
  return users.filter((user) => user.isActive)
})
```



```vue-html
<ul>
  <li
    v-for="user in activeUsers"
    :key="user.id"
  >
    {{ user.name }}
  </li>
</ul>
```

또는 `<template>` 태그를 사용하여 `v-for`로 `<li>` 요소를 감싸는 것도 가능합니다:

```vue-html
<ul>
  <template v-for="user in users" :key="user.id">
    <li v-if="user.isActive">
      {{ user.name }}
    </li>
  </template>
</ul>
```

**잘못된 예**

```vue-html
<ul>
  <li
    v-for="user in users"
    v-if="user.isActive"
    :key="user.id"
  >
    {{ user.name }}
  </li>
</ul>
```

**좋은 예**

```vue-html
<ul>
  <li
    v-for="user in activeUsers"
    :key="user.id"
  >
    {{ user.name }}
  </li>
</ul>
```

```vue-html
<ul>
  <template v-for="user in users" :key="user.id">
    <li v-if="user.isActive">
      {{ user.name }}
    </li>
  </template>
</ul>
```



<a id="style-guide-rules-essential-use-component-scoped-styling"></a>

### 컴포넌트 범위 스타일 사용하기
애플리케이션에서는 최상위 `App` 컴포넌트와 레이아웃 컴포넌트의 스타일이 전역적일 수 있지만, 다른 모든 컴포넌트는 항상 범위가 지정되어야 합니다.

이 규칙은 [싱글 파일 컴포넌트](01_getting_started_and_tutorial.md#guide-scaling-up-sfc)에만 관련이 있습니다. 그렇다고 [`scoped` 속성](08_component_and_advanced_apis.md#api-sfc-css-features-scoped-css)을 사용해야 한다는 뜻은 _아닙니다_. 범위 지정은 [CSS 모듈](08_component_and_advanced_apis.md#api-sfc-css-features-css-modules), [BEM](https://getbem.com/)과 같은 클래스 기반 전략 또는 다른 라이브러리/관례를 통해 이루어질 수 있습니다.

**하지만, 컴포넌트 라이브러리는 `scoped` 속성을 사용하기보다는 클래스 기반 전략을 선호해야 합니다.**

이렇게 하면 내부 스타일을 오버라이딩하기 쉬워지며, 특이성이 지나치게 높지 않으면서도 충돌 가능성이 매우 낮고 사람이 읽기 쉬운 클래스 이름을 제공합니다.

**상세한 설명**
큰 프로젝트를 개발하거나 다른 개발자와 함께 작업하거나 때때로 타사의 HTML/CSS (예: Auth0에서)를 포함하는 경우, 일관된 범위 지정은 스타일이 의도한 컴포넌트에만 적용되도록 보장합니다.

`scoped` 속성 외에도, 고유한 클래스 이름을 사용하면 타사 CSS가 자신의 HTML에 적용되지 않도록 도와줍니다. 예를 들어, 많은 프로젝트는 `button`, `btn`, 또는 `icon` 클래스 이름을 사용하므로, BEM과 같은 전략을 사용하지 않더라도 앱 특화 및/또는 컴포넌트 특화 접두사(예: `ButtonClose-icon`)를 추가하면 어느 정도 보호가 될 수 있습니다.

**잘못된 예**

```vue-html
<template>
  <button class="btn btn-close">×</button>
</template>

<style>
.btn-close {
  background-color: red;
}
</style>
```

**좋은 예**

```vue-html
<template>
  <button class="button button-close">×</button>
</template>

<!-- `scoped` 속성 사용하기 -->
<style scoped>
.button {
  border: none;
  border-radius: 2px;
}

.button-close {
  background-color: red;
}
</style>
```

```vue-html
<template>
  <button :class="[$style.button, $style.buttonClose]">×</button>
</template>

<!-- CSS 모듈 사용하기 -->
<style module>
.button {
  border: none;
  border-radius: 2px;
}

.buttonClose {
  background-color: red;
}
</style>
```

```vue-html
<template>
  <button class="c-Button c-Button--close">×</button>
</template>

<!-- BEM 관례 사용하기 -->
<style>
.c-Button {
  border: none;
  border-radius: 2px;
}

.c-Button--close {
  background-color: red;
}
</style>
```

---

<a id="style-guide-rules-strongly-recommended"></a>

<a id="style-guide-rules-strongly-recommended-priority-b-rules-strongly-recommended"></a>

## 우선순위 B 규칙: 강력히 권장

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/style-guide/rules-strongly-recommended.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/style-guide/rules-strongly-recommended.md

이 규칙은 대부분의 프로젝트에서 가독성 및/또는 개발자 경험을 개선하는 것으로 밝혀졌습니다. 이 규칙을 위반해도 코드는 계속 실행되지만, 위반하는 경우는 드물어야 하며 정당한 이유가 있어야 합니다.

<a id="style-guide-rules-strongly-recommended-component-files"></a>

### 컴포넌트(component) 파일
**빌드 시스템에서 파일을 연결할 수 있는 경우 각 컴포넌트는 자체 파일에 있어야 합니다.**

이렇게 하면 컴포넌트를 편집하거나 사용 방법을 검토해야 할 때 더 빠르게 찾을 수 있습니다.

<h3>Bad</h3>

```js
app.component('TodoList', {
  // ...
})

app.component('TodoItem', {
  // ...
})
```

<h3>Good</h3>

```
components/
|- TodoList.js
|- TodoItem.js
```

```
components/
|- TodoList.vue
|- TodoItem.vue
```



<a id="style-guide-rules-strongly-recommended-single-file-component-filename-casing"></a>

### 싱글 파일 컴포넌트 파일명 대/소문자
**[싱글 파일 컴포넌트](01_getting_started_and_tutorial.md#guide-scaling-up-sfc)의 파일명은 항상 파스칼 케이스(PascalCase)이거나 항상 케밥 케이스(kebab-case)여야 합니다.**

파스칼 케이스는 코드 편집기의 자동 완성 기능에서 가장 잘 작동하는데, 가능한 경우 JSX 및 템플릿(template)에서 컴포넌트를 참조하는 방식과 일치하기 때문입니다. 그러나 대소문자를 구분하지 않는 파일 시스템에서는 대소문자가 혼합된 파일 이름으로 인해 문제가 발생할 수 있으므로 케밥 케이스도 얼마든지 사용할 수 있습니다.

<h3>Bad</h3>

```
components/
|- mycomponent.vue
```

```
components/
|- myComponent.vue
```

<h3>Good</h3>

```
components/
|- MyComponent.vue
```

```
components/
|- my-component.vue
```



<a id="style-guide-rules-strongly-recommended-base-component-names"></a>

### 기본 컴포넌트 이름
**앱별 스타일과 규칙을 적용하는 기본 컴포넌트(프레젠테이션, 덤 또는 순수 컴포넌트라고도 함)는 모두 `Base`, `App` 또는 `V`와 같은 특정 접두사로 시작해야 합니다.**

**자세한 설명**
이러한 컴포넌트는 애플리케이션에서 일관된 스타일과 동작을 위한 토대를 마련합니다. 다음과 같은 앨리먼트만 포함할 수 있습니다:

- HTML 앨리먼트,
- 기타 기본 컴포넌트, 그리고
- 타사 UI 컴포넌트.

그러나 글로벌 상태(예: [Pinia](https://pinia.vuejs.org/) 스토어(store))는 **절대로** 포함하지 않습니다.

이러한 컴포넌트의 이름에는 래핑하는 앨리먼트의 이름이 포함되는 경우가 많지만(예: `BaseButton`, `BaseTable`), 특정 목적에 맞는 앨리먼트가 존재하지 않는 경우는 예외입니다(예: `BaseIcon`). 보다 구체적인 컨텍스트에 대해 유사한 컴포넌트를 빌드하는 경우 거의 항상 이러한 컴포넌트를 사용하게 됩니다(예: `BaseButton`은 `ButtonSubmit`에서 사용될 수 있음).

이 규칙에는 몇 가지 장점이 있습니다:

- 편집기에서 알파벳순으로 구성하면 앱의 기본 컴포넌트가 모두 함께 나열되므로 쉽게 식별할 수 있습니다.

- 컴포넌트 이름은 항상 여러 단어로 구성해야 하므로 이 규칙을 사용하면 간단한 컴포넌트 래퍼에 임의의 접두사(예: `MyButton`, `VueButton`)를 선택하지 않아도 됩니다.

- 이러한 컴포넌트는 자주 사용되기 때문에 모든 곳에서 임포트하는 대신 전역으로 만들고 싶을 수 있습니다. 접두사를 사용하면 Vite에서 이것이 가능해집니다:

  ```js
  const modules = import.meta.glob('./src/**/Base*.vue', { eager: true })
  for (const path in modules) {
    const config = modules[path].default
    const name = config.name || path.match(/Base[A-Z]\w+/)[0]
    app.component(name, config)
  }
  ```

<h3>Bad</h3>

```
components/
|- MyButton.vue
|- VueTable.vue
|- Icon.vue
```

<h3>Good</h3>

```
components/
|- BaseButton.vue
|- BaseTable.vue
|- BaseIcon.vue
```

```
components/
|- AppButton.vue
|- AppTable.vue
|- AppIcon.vue
```

```
components/
|- VButton.vue
|- VTable.vue
|- VIcon.vue
```



<a id="style-guide-rules-strongly-recommended-tightly-coupled-component-names"></a>

### 긴밀하게 결합된 컴포넌트 이름
**부모 컴포넌트와 긴밀하게 결합된 자식 컴포넌트는 부모 컴포넌트 이름을 접두사로 포함해야 합니다.**

컴포넌트가 단일 부모 컴포넌트의 컨텍스트에서만 의미가 있는 경우, 그 관계가 이름에 명확히 드러나야 합니다. 편집기는 일반적으로 파일을 알파벳순으로 정리하므로 이렇게 하면 관련 파일이 서로 나란히 정렬됩니다.

**상세 설명**
부모 컴포넌트의 이름을 딴 디렉터리에 자식 컴포넌트를 중첩하여 이 문제를 해결하고 싶을 수 있습니다. 예를 들어

```
components/
|- TodoList/
   |- Item/
      |- index.vue
      |- Button.vue
   |- index.vue
```

또는:

```
components/
|- TodoList/
   |- Item/
      |- Button.vue
   |- Item.vue
|- TodoList.vue
```

이 방법은 권장하지 않습니다:

- 이름이 비슷한 파일이 많아 코드 편집기에서 파일을 빠르게 전환하기가 더 어려워집니다.
- 중첩된 하위 디렉터리가 많아 에디터 사이드바에서 컴포넌트를 탐색하는 데 걸리는 시간이 늘어납니다.

<h3>Bad</h3>

```
components/
|- TodoList.vue
|- TodoItem.vue
|- TodoButton.vue
```

```
components/
|- SearchSidebar.vue
|- NavigationForSearchSidebar.vue
```

<h3>Good</h3>

```
components/
|- TodoList.vue
|- TodoListItem.vue
|- TodoListItemButton.vue
```

```
components/
|- SearchSidebar.vue
|- SearchSidebarNavigation.vue
```



<a id="style-guide-rules-strongly-recommended-order-of-words-in-component-names"></a>

### 컴포넌트 이름 내 단어 순서
**컴포넌트 이름은 가장 높은 수준의 단어(보통 가장 일반적인 단어)로 시작하고 설명적인 수정 단어로 끝나야 합니다.**


**자세한 설명**
궁금하실 수 있습니다:

> "왜 컴포넌트 이름에 자연스럽지 않은 언어를 사용하도록 강제할까요?"

자연스러운 영어에서는 형용사 및 기타 설명어가 명사 앞에 오는 것이 일반적이지만, 예외적인 경우에는 연결어가 필요합니다. 예를 들어

- 커피 _와_ 우유(Coffee _with_ milk)
- 오늘 _의_ 수프(Soup _of the_ day)
- 박물관 _으로 가는_ 방문자(Visitor _to the_ museum)

원한다면 이러한 연결어를 컴포넌트 이름에 포함할 수 있지만 순서는 여전히 중요합니다.

또한 **"최상위 수준"으로 간주되는 것은 앱의 컨텍스트에 따라 달라질 수 있습니다**. 예를 들어 검색 양식이 있는 앱을 상상해 보세요. 이 앱에는 다음과 같은 컴포넌트가 포함될 수 있습니다:

```
components/
|- ClearSearchButton.vue
|- ExcludeFromSearchInput.vue
|- LaunchOnStartupCheckbox.vue
|- RunSearchButton.vue
|- SearchInput.vue
|- TermsCheckbox.vue
```
아시다시피 어떤 컴포넌트가 검색과 관련이 있는지 확인하는 것은 매우 어렵습니다. 이제 규칙에 따라 컴포넌트의 이름을 변경해 보겠습니다:

```
components/
|- SearchButtonClear.vue
|- SearchButtonRun.vue
|- SearchInputExcludeGlob.vue
|- SearchInputQuery.vue
|- SettingsCheckboxLaunchOnStartup.vue
|- SettingsCheckboxTerms.vue
```

편집기는 일반적으로 파일을 알파벳순으로 정리하기 때문에 이제 컴포넌트 간의 모든 중요한 관계를 한눈에 알 수 있습니다.

모든 검색 컴포넌트를 "검색" 디렉터리 아래에 중첩하고 모든 설정 컴포넌트를 "설정" 디렉터리 아래에 중첩하는 등 이 문제를 다른 방식으로 해결하고 싶을 수도 있습니다. 다음과 같은 이유로 이 접근 방식은 매우 큰 앱(예: 100개 이상의 컴포넌트)에서만 고려하는 것이 좋습니다:

- 일반적으로 단일 `components` 디렉터리를 스크롤하는 것보다 중첩된 하위 디렉터리를 탐색하는 데 더 많은 시간이 걸립니다.
- 이름 충돌(예: 여러 개의 `ButtonDelete.vue` 컴포넌트)로 인해 코드 편집기에서 특정 컴포넌트로 빠르게 이동하기가 더 어려워집니다.
- 찾기 및 바꾸기만으로는 이동된 컴포넌트에 대한 상대 참조를 업데이트하기에 충분하지 않은 경우가 많으므로 리팩터링이 더 어려워집니다.

<h3>Bad</h3>

```
components/
|- ClearSearchButton.vue
|- ExcludeFromSearchInput.vue
|- LaunchOnStartupCheckbox.vue
|- RunSearchButton.vue
|- SearchInput.vue
|- TermsCheckbox.vue
```

<h3>Good</h3>

```
components/
|- SearchButtonClear.vue
|- SearchButtonRun.vue
|- SearchInputQuery.vue
|- SearchInputExcludeGlob.vue
|- SettingsCheckboxTerms.vue
|- SettingsCheckboxLaunchOnStartup.vue
```



<a id="style-guide-rules-strongly-recommended-self-closing-components"></a>

### 셀프 클로징 컴포넌트
**콘텐츠가 없는 컴포넌트는 [싱글 파일 컴포넌트](01_getting_started_and_tutorial.md#guide-scaling-up-sfc), 문자열 템플릿, [JSX](06_reactivity_and_rendering_in_depth.md#guide-extras-render-function-jsx-tsx)에서 자체 닫혀야 하지만, in-DOM 템플릿에서는 절대 자체 닫혀서는 안 됩니다.**

자체 닫히는 컴포넌트는 콘텐츠가 없을 뿐만 아니라 콘텐츠가 없는 것으로 **의미**된다는 것을 알립니다. 이는 책에서 빈 페이지와 "이 페이지는 의도적으로 비워 두었습니다."라고 표시된 페이지의 차이와 같습니다. 불필요한 닫는 태그가 없는 코드도 더 깔끔해집니다.

안타깝게도 HTML에서는 사용자 정의 앨리먼트가 자체적으로 닫히는 것을 허용하지 않으며, [공식적인 "무효" 앨리먼트](https://html.spec.whatwg.org/multipage/syntax.html#void-elements)만 허용합니다. 그렇기 때문에 이 전략은 Vue의 템플릿 컴파일러가 DOM보다 먼저 템플릿에 도달한 다음 DOM 사양을 준수하는 HTML을 제공할 수 있을 때만 가능합니다.

<h3>Bad</h3>

```vue-html
<!-- In Single-File Components, string templates, and JSX -->
<MyComponent></MyComponent>
```

```vue-html
<!-- In in-DOM templates -->
<my-component/>
```

<h3>Good</h3>

```vue-html
<!-- In Single-File Components, string templates, and JSX -->
<MyComponent/>
```

```vue-html
<!-- In in-DOM templates -->
<my-component></my-component>
```



<a id="style-guide-rules-strongly-recommended-component-name-casing-in-templates"></a>

### 템플릿에서의 컴포넌트 이름 표기법
**대부분의 프로젝트에서 컴포넌트 이름은 [싱글 파일 컴포넌트](01_getting_started_and_tutorial.md#guide-scaling-up-sfc)와 문자열 템플릿에서는 항상 파스칼 케이스(PascalCase)를 사용해야 하지만, in-DOM 템플릿에서는 케밥 케이스(kebab-case)를 사용해야 합니다.**

파스칼 케이스는 케밥 케이스에 비해 몇 가지 장점이 있습니다:

- 파스칼 케이스는 JavaScript에서도 사용되기 때문에 편집기는 템플릿에서 컴포넌트 이름을 자동 완성할 수 있습니다.
- `<MyComponent>`는 한 글자(하이픈)가 아닌 두 글자(대문자 두 개)의 차이가 있기 때문에 `<my-component>`보다 단일 단어 HTML 앨리먼트와 시각적으로 더 잘 구분됩니다.
- 템플릿에서 웹 컴포넌트와 같이 Vue가 아닌 사용자 정의 앨리먼트를 사용하는 경우 파스칼 케이스를 사용하면 Vue 컴포넌트가 명확하게 표시됩니다.

안타깝게도 HTML은 대소문자를 구분하지 않기 때문에 in-DOM 템플릿은 여전히 케밥 케이스를 사용해야 합니다.

또한 이미 케밥 케이스에 많은 투자를 했다면 HTML 규칙과의 일관성 및 모든 프로젝트에서 동일한 대소문자를 사용할 수 있는 것이 위에 나열된 장점보다 더 중요할 수 있습니다. 이러한 경우에는 **모든 곳에 케밥 케이스를 사용하는 것도 허용됩니다**.

<h3>Bad</h3>

```vue-html
<!-- In Single-File Components and string templates -->
<mycomponent/>
```

```vue-html
<!-- In Single-File Components and string templates -->
<myComponent/>
```

```vue-html
<!-- In in-DOM templates -->
<MyComponent></MyComponent>
```

<h3>Good</h3>

```vue-html
<!-- In Single-File Components and string templates -->
<MyComponent/>
```

```vue-html
<!-- In in-DOM templates -->
<my-component></my-component>
```

또는

```vue-html
<!-- Everywhere -->
<my-component></my-component>
```



<a id="style-guide-rules-strongly-recommended-component-name-casing-in-js-jsx"></a>

### JS/JSX에서의 컴포넌트 이름 표기법
**JS/[JSX](06_reactivity_and_rendering_in_depth.md#guide-extras-render-function-jsx-tsx)의 컴포넌트 이름은 항상 파스칼 케이스(PascalCase)를 사용해야 하지만, `app.component`를 통한 전역 컴포넌트 등록만 사용하는 간단한 애플리케이션의 경우 문자열 내부에 케밥 케이스(kebab-case)를 사용할 수 있습니다.**

**자세한 설명**
JavaScript에서 파스칼 케이스는 클래스 및 프로토타입 생성자, 즉 본질적으로 별개의 인스턴스(instance)를 가질 수 있는 모든 것에 대한 규칙입니다. Vue 컴포넌트에도 인스턴스가 있으므로 파스칼 케이스도 사용하는 것이 합리적입니다. 추가적인 이점으로, JSX(및 템플릿) 내에서 파스칼 케이스를 사용하면 코드 독자가 컴포넌트와 HTML 앨리먼트를 더 쉽게 구분할 수 있습니다.

그러나 `app.component`를 통해 전역 컴포넌트 정의만 사용하는 애플리케이션의 경우 케밥 케이스를 사용하는 것이 좋습니다. 그 이유는 다음과 같습니다:

- JavaScript에서 전역 컴포넌트를 참조하는 경우는 거의 없으므로 JavaScript 규칙을 따르는 것이 덜 합리적입니다.
- 이러한 애플리케이션에는 항상 많은 in-DOM 템플릿이 포함되며, 여기에서는 [케밥 케이스를 **반드시** 사용](09_style_guide_examples_and_reference.md#style-guide-rules-strongly-recommended-component-name-casing-in-templates)해야 합니다.

<h3>Bad</h3>

```js
app.component('myComponent', {
  // ...
})
```

```js
import myComponent from './MyComponent.vue'
```

```js
export default {
  name: 'myComponent'
  // ...
}
```

```js
export default {
  name: 'my-component'
  // ...
}
```

<h3>Good</h3>

```js
app.component('MyComponent', {
  // ...
})
```

```js
app.component('my-component', {
  // ...
})
```

```js
import MyComponent from './MyComponent.vue'
```

```js
export default {
  name: 'MyComponent'
  // ...
}
```



<a id="style-guide-rules-strongly-recommended-full-word-component-names"></a>

### 전체 단어를 사용한 컴포넌트 이름
**컴포넌트 이름은 약어보다 완전한 단어를 사용하는 것이 좋습니다.**

편집기의 자동 완성을 쓰면 긴 이름도 쉽게 입력할 수 있습니다. 완전한 단어를 쓰면 이름의 뜻을 파악하기 쉬우므로, 특히 익숙하지 않은 약어는 피합니다.

<h3>Bad</h3>

```
components/
|- SdSettings.vue
|- UProfOpts.vue
```

<h3>Good</h3>

```
components/
|- StudentDashboardSettings.vue
|- UserProfileOptions.vue
```



<a id="style-guide-rules-strongly-recommended-prop-name-casing"></a>

### prop 이름 표기법
**Prop 이름은 선언 시 항상 camelCase를 사용해야 합니다. DOM 내에서 직접 사용하는 템플릿에서는 props를 kebab-case로 작성해야 합니다. 반면, Single-File Component(SFC) 템플릿과 [JSX](06_reactivity_and_rendering_in_depth.md#guide-extras-render-function-jsx-tsx)에서는 kebab-case 또는 camelCase 중 하나를 선택하여 사용할 수 있습니다. 일관성을 유지하는 것이 중요합니다. camelCase를 선택했다면, 애플리케이션 전체에서 kebab-case props를 혼용하지 않도록 주의해야 합니다.**

<h3>Bad</h3>


**옵션 API**


```js
props: {
  'greeting-text': String
}
```



**컴포지션 API**


```js
const props = defineProps({
  'greeting-text': String
})
```



```vue-html
// for in-DOM templates
<welcome-message greetingText="hi"></welcome-message>
```

<h3>Good</h3>


**옵션 API**


```js
props: {
  greetingText: String
}
```



**컴포지션 API**


```js
const props = defineProps({
  greetingText: String
})
```



```vue-html
// SFC의 경우 - 프로젝트 전체에서 대소문자가 일관성 있게 유지되도록 하세요. 
// 두 가지 규칙 중 하나를 사용할 수 있지만 두 가지 다른 대소문자 스타일을 혼합하는 것은 권장하지 않습니다.
<WelcomeMessage greeting-text="hi"/>
// or
<WelcomeMessage greetingText="hi"/>
```

```vue-html
// for in-DOM templates
<welcome-message greeting-text="hi"></welcome-message>
```



<a id="style-guide-rules-strongly-recommended-multi-attribute-elements"></a>

### 다중 속성 앨리먼트
**여러 속성을 가진 앨리먼트는 여러 줄에 걸쳐 있어야 하며, 한 줄당 하나의 속성을 사용해야 합니다.**

JavaScript에서는 여러 속성을 가진 객체를 여러 줄에 걸쳐 분할하는 것이 훨씬 읽기 쉽기 때문에 좋은 관습으로 널리 알려져 있습니다. 우리의 템플릿과 [JSX](06_reactivity_and_rendering_in_depth.md#guide-extras-render-function-jsx-tsx)도 이와 동일하게 고려할 가치가 있습니다.

<h3>Bad</h3>

```vue-html
<img src="https://vuejs.org/images/logo.png" alt="Vue Logo">
```

```vue-html
<MyComponent foo="a" bar="b" baz="c"/>
```

<h3>Good</h3>

```vue-html
<img
  src="https://vuejs.org/images/logo.png"
  alt="Vue Logo"
>
```

```vue-html
<MyComponent
  foo="a"
  bar="b"
  baz="c"
/>
```



<a id="style-guide-rules-strongly-recommended-simple-expressions-in-templates"></a>

### 템플릿의 간단한 표현식
**컴포넌트 템플릿에는 단순한 표현식만 포함해야 하며, 복잡한 표현식은 계산된 속성(computed property)이나 메서드로 리팩터링해야 합니다.**

템플릿에 계산 과정까지 길게 적으면 화면에 무엇을 표시하는지 파악하기 어려워집니다. 계산을 프로퍼티(property)나 메서드로 옮기면 템플릿에서는 결과의 이름만 읽으면 되고, 같은 계산을 다른 곳에서도 재사용할 수 있습니다.

<h3>Bad</h3>

```vue-html
{{
  fullName.split(' ').map((word) => {
    return word[0].toUpperCase() + word.slice(1)
  }).join(' ')
}}
```

<h3>Good</h3>

```vue-html
<!-- In a template -->
{{ normalizedFullName }}
```


**옵션 API**


```js
// The complex expression has been moved to a computed property
computed: {
  normalizedFullName() {
    return this.fullName.split(' ')
      .map(word => word[0].toUpperCase() + word.slice(1))
      .join(' ')
  }
}
```



**컴포지션 API**


```js
// The complex expression has been moved to a computed property
const normalizedFullName = computed(() =>
  fullName.value
    .split(' ')
    .map((word) => word[0].toUpperCase() + word.slice(1))
    .join(' ')
)
```



<a id="style-guide-rules-strongly-recommended-simple-computed-properties"></a>

### 단순 계산 프로퍼티
**복잡한 계산 프로퍼티는 가능한 한 많은 단순한 프로퍼티로 분할해야 합니다.**

**자세한 설명**
더 간단하고 이름이 잘 지정된 계산된 프로퍼티는 다음과 같은 장점이 있습니다:

- **테스트하기 쉬움**

  각 계산된 프로퍼티에 종속성이 거의 없는 매우 간단한 표현식만 포함되어 있으면 올바르게 작동하는지 확인하는 테스트를 작성하기가 훨씬 쉽습니다.

- **읽기 쉬움**

  계산된 프로퍼티를 단순화하면 재사용되지 않더라도 각 값에 설명이 포함된 이름을 지정할 수 있습니다. 이렇게 하면 다른 개발자(그리고 미래의 개발자)가 관심 있는 코드에 집중하고 무슨 일이 일어나고 있는지 파악하기가 훨씬 쉬워집니다.

- **변화하는 요구 사항에 더 쉽게 적응**

  이름을 지정할 수 있는 모든 값은 뷰에 유용할 수 있습니다. 예를 들어, 사용자가 얼마나 많은 돈을 절약했는지 알려주는 메시지를 표시하기로 결정할 수 있습니다. 또한 판매세를 계산하되 최종 가격의 일부가 아닌 별도로 표시하기로 결정할 수도 있습니다.

  작고 집중적인 계산된 프로퍼티는 정보 사용 방식에 대한 가정이 적기 때문에 요구 사항 변경에 따른 리팩터링이 덜 필요합니다.

<h3>Bad</h3>


**옵션 API**


```js
computed: {
  price() {
    const basePrice = this.manufactureCost / (1 - this.profitMargin)
    return (
      basePrice -
      basePrice * (this.discountPercent || 0)
    )
  }
}
```



**컴포지션 API**


```js
const price = computed(() => {
  const basePrice = manufactureCost.value / (1 - profitMargin.value)
  return basePrice - basePrice * (discountPercent.value || 0)
})
```

<h3>Good</h3>


**옵션 API**


```js
computed: {
  basePrice() {
    return this.manufactureCost / (1 - this.profitMargin)
  },

  discount() {
    return this.basePrice * (this.discountPercent || 0)
  },

  finalPrice() {
    return this.basePrice - this.discount
  }
}
```



**컴포지션 API**


```js
const basePrice = computed(
  () => manufactureCost.value / (1 - profitMargin.value)
)

const discount = computed(
  () => basePrice.value * (discountPercent.value || 0)
)

const finalPrice = computed(() => basePrice.value - discount.value)
```



<a id="style-guide-rules-strongly-recommended-quoted-attribute-values"></a>

### 따옴표로 묶인 속성 값
**비어 있지 않은 HTML 속성 값은 항상 따옴표 안에 넣어야 합니다(JS에서 사용되지 않는 단일 또는 이중 중 하나).**

공백이 없는 속성 값은 HTML에서 따옴표로 묶을 필요가 없지만, 이 관행은 종종 공백을 _회피_하게 만들어 속성 값의 가독성을 떨어뜨립니다.

<h3>Bad</h3>

```vue-html
<input type=text>
```

```vue-html
<AppSidebar :style={width:sidebarWidth+'px'}>
```

<h3>Good</h3>

```vue-html
<input type="text">
```

```vue-html
<AppSidebar :style="{ width: sidebarWidth + 'px' }">
```



<a id="style-guide-rules-strongly-recommended-directive-shorthands"></a>

### 디렉티브(directive) 단축 표기법
**디렉티브 단축(`v-bind:`의 경우 `:`, `v-on:`의 경우 `@`, `v-slot`의 경우 `#`)은 항상 사용하거나 절대 사용하지 않아야 합니다.**

<h3>Bad</h3>

```vue-html
<input
  v-bind:value="newTodoText"
  :placeholder="newTodoInstructions"
>
```

```vue-html
<input
  v-on:input="onInput"
  @focus="onFocus"
>
```

```vue-html
<template v-slot:header>
  <h1>Here might be a page title</h1>
</template>

<template #footer>
  <p>Here's some contact info</p>
</template>
```

<h3>Good</h3>

```vue-html
<input
  :value="newTodoText"
  :placeholder="newTodoInstructions"
>
```

```vue-html
<input
  v-bind:value="newTodoText"
  v-bind:placeholder="newTodoInstructions"
>
```

```vue-html
<input
  @input="onInput"
  @focus="onFocus"
>
```

```vue-html
<input
  v-on:input="onInput"
  v-on:focus="onFocus"
>
```

```vue-html
<template v-slot:header>
  <h1>Here might be a page title</h1>
</template>

<template v-slot:footer>
  <p>Here's some contact info</p>
</template>
```

```vue-html
<template #header>
  <h1>Here might be a page title</h1>
</template>

<template #footer>
  <p>Here's some contact info</p>
</template>
```

---

<a id="style-guide-rules-recommended"></a>

<a id="style-guide-rules-recommended-priority-c-rules-recommended"></a>

## 우선 순위 C 규칙: 권장

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/style-guide/rules-recommended.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/style-guide/rules-recommended.md

여러 가지 동등하게 좋은 옵션이 존재할 때, 일관성을 유지하기 위해 임의적인 선택을 할 수 있습니다. 이 규칙에서는 각각의 허용 가능한 옵션을 설명하고 기본 선택을 제안합니다. 즉, 일관성을 유지하고 좋은 이유가 있다면 코드베이스에서 다른 선택을 자유롭게 할 수 있습니다. 하지만 좋은 이유가 있어야 합니다! 커뮤니티 표준에 적응함으로써 다음과 같은 이점이 있습니다:

1. 대부분의 커뮤니티 코드를 더 쉽게 파악할 수 있도록 두뇌를 훈련시킵니다.
2. 대부분의 커뮤니티 코드 예제를 수정 없이 복사 및 붙여넣을 수 있습니다.
3. 새로운 직원이 Vue에 관해 선호되는 코딩 스타일에 이미 익숙할 가능성이 높습니다.

<a id="style-guide-rules-recommended-component-instance-options-order"></a>

### 컴포넌트(component)/인스턴스(instance) 옵션 순서
**컴포넌트/인스턴스 옵션은 일관되게 정렬되어야 합니다.**

다음은 컴포넌트 옵션의 권장 순서입니다. 역할별로 나누었으므로 플러그인(plugin)이 추가하는 속성도 해당 역할에 맞는 위치에 둘 수 있습니다.

1. **글로벌 인지도** (컴포넌트를 넘어서는 지식 필요)

  - `name`

2. **템플릿 컴파일러 옵션** (템플릿(template) 컴파일 방식 변경)

  - `compilerOptions`

3. **템플릿 의존성** (템플릿에서 사용되는 자산)

  - `components`
  - `directives`

4. **구성** (옵션에 속성 결합)

  - `extends`
  - `mixins`
  - `provide`/`inject`

5. **인터페이스** (컴포넌트의 인터페이스)

  - `inheritAttrs`
  - `props`
  - `emits`
  - `expose`

6. **컴포지션 API** (컴포지션 API 사용을 위한 진입점)

  - `setup`

7. **로컬 상태** (로컬 반응형 속성)

  - `data`
  - `computed`

8. **이벤트** (반응형 이벤트에 의해 트리거되는 콜백(callback))

  - `watch`
  - 라이프사이클(lifecycle) 이벤트 (호출되는 순서대로)
    - `beforeCreate`
    - `created`
    - `beforeMount`
    - `mounted`
    - `beforeUpdate`
    - `updated`
    - `activated`
    - `deactivated`
    - `beforeUnmount`
    - `unmounted`
    - `errorCaptured`
    - `renderTracked`
    - `renderTriggered`
    - `serverPrefetch` (SSR 전용)

9. **비반응형 속성** (반응형 시스템과 독립적인 인스턴스 속성)

  - `methods`

10. **렌더링(rendering)** (컴포넌트 출력의 선언적 설명)
  - `template`/`render`

<a id="style-guide-rules-recommended-element-attribute-order"></a>

### 요소 속성 순서
**요소(컴포넌트를 포함)의 속성은 일관되게 정렬되어야 합니다.**

다음은 요소 속성의 권장 순서입니다. 역할별 분류를 기준으로 커스텀 속성과 디렉티브(directive)를 배치하면 됩니다.

1. **정의** (컴포넌트 옵션 제공)

  - `is`

2. **목록 렌더링** (동일한 요소의 여러 변형 생성)

  - `v-for`

3. **조건부** (요소가 렌더링/표시되는지 여부)

  - `v-if`
  - `v-else-if`
  - `v-else`
  - `v-show`
  - `v-cloak`

4. **렌더링 수정자** (요소의 렌더링 방식 변경)

  - `v-pre`
  - `v-once`

5. **글로벌 인지도** (컴포넌트를 넘어서는 지식 필요)

  - `id`

6. **고유 속성** (고유한 값이 필요한 속성)

  - `ref`
  - `key`

7. **양방향 바인딩** (바인딩(binding)과 이벤트 결합)

  - `v-model`

8. **기타 속성** (모든 지정되지 않은 바인딩 및 바인딩되지 않은 속성)

9. **이벤트** (컴포넌트 이벤트 리스너(listener))

  - `v-on`

10. **내용** (요소의 내용을 오버라이드함)
  - `v-html`
  - `v-text`

<a id="style-guide-rules-recommended-empty-lines-in-component-instance-options"></a>

### 컴포넌트/인스턴스 옵션에서의 빈 줄
**여러 줄로 된 속성 사이에 빈 줄을 추가하고 싶을 수도 있습니다. 특히 스크롤 없이는 옵션이 화면에 다 들어오지 않을 경우에는 더욱 그렇습니다.**

컴포넌트가 읽기 어렵거나 혼잡해질 때, 여러 줄로 된 속성 사이에 공간을 추가하면 다시 살펴보기 쉬워질 수 있습니다. Vim과 같은 일부 편집기에서는 키보드로 탐색하기 쉽도록 이러한 형식의 옵션을 사용할 수도 있습니다.


**옵션 API**

**잘못된 예**

```js
props: {
  value: {
    type: String,
    required: true
  },

  focused: {
    type: Boolean,
    default: false
  },

  label: String,
  icon: String
},

computed: {
  formattedValue() {
    // ...
  },

  inputClasses() {
    // ...
  }
}
```

**좋은 예**

```js
// 컴포넌트가 여전히 읽기 쉽고 탐색하기 쉬운 경우
// 공백이 없어도 괜찮습니다.
props: {
  value: {
    type: String,
    required: true
  },
  focused: {
    type: Boolean,
    default: false
  },
  label: String,
  icon: String
},
computed: {
  formattedValue() {
    // ...
  },
  inputClasses() {
    // ...
  }
}
```



**컴포지션 API**

**잘못된 예**

```js
defineProps({
  value: {
    type: String,
    required: true
  },
  focused: {
    type: Boolean,
    default: false
  },
  label: String,
  icon: String
})
const formattedValue = computed(() => {
  // ...
})
const inputClasses = computed(() => {
  // ...
})
```

**좋은 예**

```js
defineProps({
  value: {
    type: String,
    required: true
  },

  focused: {
    type: Boolean,
    default: false
  },

  label: String,
  icon: String
})

const formattedValue = computed(() => {
  // ...
})

const inputClasses = computed(() => {
  // ...
})
```



<a id="style-guide-rules-recommended-single-file-component-top-level-element-order"></a>

### 싱글 파일 컴포넌트 최상위 요소 순서
**[싱글 파일 컴포넌트](01_getting_started_and_tutorial.md#guide-scaling-up-sfc)는 항상 `<script>`, `<template>`, `<style>` 태그를 일관되게 정렬해야 하며, `<style>`은 다른 두 태그 중 적어도 하나가 항상 필요하기 때문에 마지막에 와야 합니다.**

**잘못된 예**

```vue-html [ComponentX.vue]
<style>/* ... */</style>
<script>/* ... */</script>
<template>...</template>
```

```vue-html [ComponentA.vue]
<script>/* ... */</script>
<template>...</template>
<style>/* ... */</style>
```

```vue-html [ComponentB.vue]
<template>...</template>
<script>/* ... */</script>
<style>/* ... */</style>
```

**좋은 예**

```vue-html [ComponentA.vue]
<script>/* ... */</script>
<template>...</template>
<style>/* ... */</style>
```

```vue-html [ComponentB.vue]
<script>/* ... */</script>
<template>...</template>
<style>/* ... */</style>
```

또는

```vue-html  [ComponentA.vue]
<template>...</template>
<script>/* ... */</script>
<style>/* ... */</style>
```

```vue-html [ComponentB.vue]
<template>...</template>
<script>/* ... */</script>
<style>/* ... */</style>
```

---

<a id="style-guide-rules-use-with-caution"></a>

<a id="style-guide-rules-use-with-caution-priority-d-rules-use-with-caution"></a>

## 우선 순위 D 규칙: 주의해서 사용하기

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/style-guide/rules-use-with-caution.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/style-guide/rules-use-with-caution.md

Vue의 일부 기능은 드문 에지 케이스에 대응하거나 레거시 코드베이스에서 원활하게 마이그레이션하도록 돕기 위해 존재합니다. 그러나 과도하게 사용되면 코드를 유지 관리하기 어렵게 만들거나 심지어 버그의 원인이 될 수 있습니다. 이 규칙들은 잠재적으로 위험한 기능이 무엇인지 밝히고, 언제, 왜 피해야 하는지 설명합니다.

<a id="style-guide-rules-use-with-caution-element-selectors-with-scoped"></a>

### `scoped`에서의 요소 선택자
**`scoped`에서는 요소 선택자를 피해야 합니다.**

`scoped` 스타일에서는 요소 선택자보다 클래스 선택자를 선호합니다. 왜냐하면 많은 수의 요소 선택자는 처리 속도가 느리기 때문입니다.

**상세한 설명**
스타일을 범위 지정하기 위해, Vue는 컴포넌트(component) 앨리먼트에 `data-v-f3f3eg9`와 같은 고유한 속성을 추가합니다. 그런 다음 선택자가 수정되어 이 속성을 가진 일치하는 앨리먼트만 선택되도록 합니다 (예: `button[data-v-f3f3eg9]`).

문제는 많은 수의 앨리먼트-속성 선택자(예: `button[data-v-f3f3eg9]`)가 클래스-속성 선택자(예: `.btn-close[data-v-f3f3eg9]`)보다 상당히 느리다는 것이므로, 가능한 한 클래스 선택자를 선호해야 합니다.

**잘못된 예**

```vue-html
<template>
  <button>×</button>
</template>

<style scoped>
button {
  background-color: red;
}
</style>
```

**좋은 예**

```vue-html
<template>
  <button class="btn btn-close">×</button>
</template>

<style scoped>
.btn-close {
  background-color: red;
}
</style>
```



<a id="style-guide-rules-use-with-caution-implicit-parent-child-communication"></a>

### 암시적인 부모-자식 커뮤니케이션
**부모-자식 컴포넌트 간의 커뮤니케이션에는 `this.$parent`를 사용하거나 prop을 변형하는 대신, prop과 이벤트를 사용하는 것을 선호해야 합니다.**

이상적인 Vue 애플리케이션은 prop을 통해 아래로 전달하고, 이벤트를 통해 위로 전달합니다. 이 관례를 따르면 컴포넌트를 이해하기 훨씬 쉬워집니다. 그러나 prop 변형이나 `this.$parent`가 이미 깊게 결합된 두 컴포넌트를 단순화하는 에지 케이스가 있습니다.

단순한 상황에서는 이런 패턴이 편리할 수도 있습니다. 다만 코드를 조금 덜 쓰기 위해 상태 흐름을 이해하기 어렵게 만들지는 않는지 살펴봐야 합니다.


**옵션 API**

**잘못된 예**

```js
app.component('TodoItem', {
  props: {
    todo: {
      type: Object,
      required: true
    }
  },

  template: '<input v-model="todo.text">'
})
```

```js
app.component('TodoItem', {
  props: {
    todo: {
      type: Object,
      required: true
    }
  },

  methods: {
    removeTodo() {
      this.$parent.todos = this.$parent.todos.filter(
        (todo) => todo.id !== vm.todo.id
      )
    }
  },

  template: `
    <span>
      {{ todo.text }}
      <button @click="removeTodo">
        ×
      </button>
    </span>
  `
})
```

**좋은 예**

```js
app.component('TodoItem', {
  props: {
    todo: {
      type: Object,
      required: true
    }
  },

  emits: ['input'],

  template: `
    <input
      :value="todo.text"
      @input="$emit('input', $event.target.value)"
    >
  `
})
```

```js
app.component('TodoItem', {
  props: {
    todo: {
      type: Object,
      required: true
    }
  },

  emits: ['delete'],

  template: `
    <span>
      {{ todo.text }}
      <button @click="$emit('delete')">
        ×
      </button>
    </span>
  `
})
```



**컴포지션 API**

**잘못된 예**

```vue
<script setup>
defineProps({
  todo: {
    type: Object,
    required: true
  }
})
</script>

<template>
  <input v-model="todo.text" />
</template>
```

```vue
<script setup>
const props = defineProps({
  todo: {
    type: Object,
    required: true
  }
})

function renameTodo() {
  // prop을 통해 부모의 반응형 객체를 변경합니다.
  // 다시 말해, 자식이 부모가 소유한 상태에 접근하여 변경하는 것입니다.
  props.todo.text = 'renamed by child'
}
</script>

<template>
  <span>
    {{ todo.text }}
    <button @click="renameTodo">rename</button>
  </span>
</template>
```

**좋은 예**

```vue
<script setup>
defineProps({
  todo: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['input'])
</script>

<template>
  <input :value="todo.text" @input="emit('input', $event.target.value)" />
</template>
```

```vue
<script setup>
const props = defineProps({
  todo: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['update:todo'])

function renameTodo() {
  // 새로운 객체를 emit합니다. 업데이트는 부모가 소유합니다.
  emit('update:todo', { ...props.todo, text: 'renamed by parent' })
}
</script>

<template>
  <span>
    {{ todo.text }}
    <button @click="renameTodo">rename</button>
  </span>
</template>
```

---

<a id="examples-index"></a>

## Vue 3 예제

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/examples/index.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/examples/index.md

각 예제 폴더에 컴포지션 API와 옵션 API 소스, 공통 HTML 템플릿 및 스타일이 있습니다. `description.txt`에서 예제 설명을 확인할 수 있습니다.

브라우저 실행: https://ko.vuejs.org/examples/

### Hello World

[예제 소스](assets/examples/src/hello-world)

Vue로 Hello World를 말해보세요!

### 속성 바인딩

[예제 소스](assets/examples/src/attribute-bindings)

여기서는 요소의 속성/프로퍼티를 상태에 반응적으로 바인딩하고 있습니다.
:title 구문은 v-bind:title의 축약형입니다.

### 조건부 렌더링과 반복

[예제 소스](assets/examples/src/conditionals-and-loops)

우리는 v-if 및 v-for 지시어를 사용하여 조건부로 또는 반복적으로 콘텐츠를 렌더링할 수 있습니다.

### 사용자 입력 처리

[예제 소스](assets/examples/src/handling-input)

이 예제는 v-on 디렉티브로 사용자 입력을 처리하는 방법을 보여줍니다.

### 폼 바인딩

[예제 소스](assets/examples/src/form-bindings)

v-model 지시어를 사용하여 상태와 폼 입력값 사이에 양방향 바인딩을 만들 수 있습니다.

### 간단한 컴포넌트

[예제 소스](assets/examples/src/simple-component)

여기에서는 prop을 받아서 렌더링하는 가장 간단한 컴포넌트를 보여줍니다.
가이드에서 컴포넌트에 대해 더 알아보세요!

<a id="examples-index-fetching-data"></a>

### 데이터 가져오기

[예제 소스](assets/examples/src/fetching-data)

이 예제는 GitHub의 API에서 최신 Vue Core 커밋 데이터를 가져와 목록으로 표시합니다.
두 개의 주요 브랜치 간에 전환할 수 있습니다.


### 정렬과 필터링이 가능한 그리드

[예제 소스](assets/examples/src/grid)

재사용 가능한 그리드 컴포넌트를 생성하고 외부 데이터와 함께 사용하는 예제.

### Markdown 편집기

[예제 소스](assets/examples/src/markdown)

간단한 마크다운 에디터.

<a id="examples-index-modal"></a>

### 모달 컴포넌트

[예제 소스](assets/examples/src/modal)

사용자 지정 슬롯과 CSS 전환 효과가 있는 모달 컴포넌트.

<a id="examples-index-list-transition"></a>

### 리스트 트랜지션

[예제 소스](assets/examples/src/list-transition)

내장된 `<TransitionGroup>`로 FLIP 리스트 전환하기.
https://aerotwist.com/blog/flip-your-animations/

### 트리 뷰

[예제 소스](assets/examples/src/tree)

자기 자신을 재귀적으로 렌더링하는 중첩 트리 컴포넌트입니다.
항목을 더블 클릭하면 폴더로 변환할 수 있습니다.

### SVG 그래프

[예제 소스](assets/examples/src/svg)

SVG 그래프

### 카운터

[예제 소스](assets/examples/src/counter)

https://eugenkiss.github.io/7guis/tasks/#counter

### 온도 변환기

[예제 소스](assets/examples/src/temperature-converter)

https://eugenkiss.github.io/7guis/tasks/#temp

### 항공편 예약

[예제 소스](assets/examples/src/flight-booker)

https://eugenkiss.github.io/7guis/tasks/#flight

### 타이머

[예제 소스](assets/examples/src/timer)

https://eugenkiss.github.io/7guis/tasks/#timer

### CRUD

[예제 소스](assets/examples/src/crud)

https://eugenkiss.github.io/7guis/tasks/#crud

### 원 그리기

[예제 소스](assets/examples/src/circle-drawer)

https://eugenkiss.github.io/7guis/tasks/#circle

### 스프레드시트

[예제 소스](assets/examples/src/cells)

https://eugenkiss.github.io/7guis/tasks/#cells

---

<a id="about-faq"></a>

<a id="about-faq-frequently-asked-questions"></a>

## 자주 묻는 질문

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/about/faq.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/about/faq.md

<a id="about-faq-who-maintains-vue"></a>

### Vue는 누가 유지 관리하나요?
Vue는 독립적인 커뮤니티 중심 프로젝트입니다. 2014년에 [Evan You](https://x.com/evanyou)가 개인 부업 프로젝트로 만들었습니다. 현재는 [전 세계의 정규직 및 자원 봉사자들로 구성된 팀](https://ko.vuejs.org/about/team)이 Vue를 활발하게 유지 관리하고 있으며, Evan은 프로젝트 리더로 활동하고 있습니다. 이 [다큐멘터리](https://www.youtube.com/watch?v=OrxmtDw4pVI)에서 Vue에 대한 자세한 이야기를 확인할 수 있습니다.

Vue의 개발은 주로 후원을 통해 이루어지며, 2016년부터 재정적으로 지속 가능한 상태로 유지되고 있습니다. 여러분 또는 여러분의 비즈니스가 Vue의 혜택을 받고 있다면 [후원](https://ko.vuejs.org/sponsor/)을 통해 Vue의 개발을 지원하는 것을 고려해 보세요!

<a id="about-faq-what-s-the-difference-between-vue-2-and-vue-3"></a>

### Vue 2와 Vue 3의 차이점은 무엇인가요?
Vue 3는 Vue의 최신 주요 버전입니다. 여기에는 텔레포트, 서스펜스 및 템플릿(template)당 여러 루트 앨리먼트와 같이 Vue 2에는 없는 새로운 기능이 포함되어 있습니다. 또한 Vue 2와 호환되지 않는 중요한 변경 사항도 포함되어 있습니다. 자세한 내용은 [Vue 3 마이그레이션 가이드](https://v3-migration.vuejs.org/)에 문서화되어 있습니다.

차이점에도 불구하고 Vue API의 대부분은 두 주요 버전 간에 공유되므로, Vue 2에 대한 지식은 대부분 Vue 3에서도 그대로 유효합니다. 특히 컴포지션 API는 원래 Vue 3 전용 기능이었지만 이제 Vue 2로 백포트되어 [Vue 2.7](https://github.com/vuejs/vue/blob/main/CHANGELOG.md#270-2022-07-01)에서 사용할 수 있습니다.

일반적으로 Vue 3는 더 작은 번들 크기와 더 나은 성능, 확장성, TypeScript/IDE 지원을 제공합니다. 지금 새 프로젝트를 시작하는 경우 Vue 3를 권장합니다. 현재로서는 Vue 2를 고려해야 하는 몇 가지 이유가 있습니다:

- IE11을 지원해야 합니다. Vue 3는 최신 JavaScript 기능을 활용하며 IE11을 지원하지 않습니다.

기존 Vue 2 앱을 Vue 3로 마이그레이션하려는 경우 [마이그레이션 가이드](https://v3-migration.vuejs.org/)를 참조하세요.

<a id="about-faq-is-vue-2-still-supported"></a>

### Vue 2가 계속 지원되나요?
2022년 7월에 출시된 Vue 2.7은 Vue 2 버전 범위의 마지막 마이너 릴리스입니다. Vue 2는 이제 유지 관리 모드로 전환되어 더 이상 새로운 기능을 제공하지 않지만 2.7 릴리스 날짜부터 18개월 동안 중요한 버그 수정 및 보안 업데이트가 계속 제공됩니다. 즉, **Vue 2는 2023년 12월 31일부로 수명이 종료(EOL)되었습니다**.

이를 통해 대부분의 생태계가 Vue 3로 마이그레이션할 수 있는 충분한 시간을 확보할 수 있을 것으로 생각합니다. 하지만 보안 및 규정 준수 요건을 충족해야 하는 상황에서 이 일정까지 업그레이드할 수 없는 팀이나 프로젝트가 있을 수 있다는 점도 잘 알고 있습니다. 이러한 요구 사항이 있는 팀을 위해 업계 전문가와 협력하여 Vue 2에 대한 연장 지원을 제공하고 있습니다. 2023년 말 이후에도 Vue 2를 사용해야 하는 팀이라면 미리 계획을 세우고 [Vue 2 Extended LTS](https://v2.vuejs.org/lts/)에 대해 자세히 알아보세요.

<a id="about-faq-what-license-does-vue-use"></a>

### Vue는 어떤 라이선스를 사용하나요?
Vue는 [MIT 라이선스](https://opensource.org/licenses/MIT)에 따라 공개된 무료 오픈소스 프로젝트입니다.

<a id="about-faq-what-browsers-does-vue-support"></a>

### Vue는 어떤 브라우저를 지원하나요?
최신 버전의 Vue(3.x)는 [기본 ES2016을 지원하는 브라우저](https://caniuse.com/es2016)만 지원합니다. IE11은 제외됩니다. Vue 3.x는 레거시 브라우저에서 폴리필링(polyfilling)할 수 없는 ES2016 기능을 사용하므로 레거시 브라우저를 지원해야 하는 경우 대신 Vue 2.x를 사용해야 합니다.

<a id="about-faq-is-vue-reliable"></a>

### Vue는 신뢰할 수 있나요?
Vue는 성숙하고 수많은 테스트를 거친 프레임워크입니다. 오늘날 프로덕션 환경에서 가장 널리 쓰이는 JavaScript 프레임워크 중 하나로, 전 세계에 150만 명 이상의 사용자가 있으며 npm에서 한 달에 천만 번 가까이 다운로드됩니다.

Vue는 위키미디어 재단, NASA, Apple, Google, Microsoft, GitLab, Zoom, Tencent, Weibo, Bilibili, Kuaishou 등 전 세계의 유명 조직에서 다양한 규모로 프로덕션에 사용되고 있습니다.

<a id="about-faq-is-vue-fast"></a>

### Vue가 빠르나요?
Vue 3는 가장 성능이 뛰어난 메인스트림 프론트엔드 프레임워크 중 하나이며 대부분의 웹 애플리케이션 사용 사례를 수동으로 최적화할 필요 없이 쉽게 처리합니다.

스트레스 테스트 시나리오에서 Vue는 [js-framework-benchmark](https://krausest.github.io/js-framework-benchmark/current.html)에서 React 및 Angular를 상당한 차이로 능가합니다. 또한 벤치마크에서 가장 빠른 프로덕션 수준의 비-Virtual-DOM 프레임워크와도 나란히 경쟁합니다.

위와 같은 합성 벤치마크는 전용 최적화가 적용된 원시 렌더링(rendering) 성능에 중점을 두므로 실제 성능 결과를 완전히 대표하지 못할 수 있습니다. 페이지 로드 성능에 대해 더 자세히 알고 싶으시다면 [WebPageTest](https://www.webpagetest.org/lighthouse) 또는 [PageSpeed Insights](https://pagespeed.web.dev/)를 사용하여 바로 이 웹사이트를 테스트해 보시기 바랍니다. 이 웹사이트는 Vue 자체로 구동되며, SSG 사전 렌더링과 전체 페이지 하이드레이션(hydration), SPA 클라이언트 측 탐색 기능을 갖추고 있습니다. 느린 4G 네트워크에서 4배 CPU 스로틀링(throttling)으로 에뮬레이트된 Moto G4에서 성능 100점을 받았습니다.

[렌더링 메커니즘](06_reactivity_and_rendering_in_depth.md#guide-extras-rendering-mechanism) 섹션에서 Vue가 런타임 성능을 자동으로 최적화하는 방법에 대해 자세히 알아볼 수 있으며, 특히 까다로운 경우 Vue 앱을 최적화하는 방법은 [성능 최적화 가이드](05_scaling_typescript_and_best_practices.md#guide-best-practices-performance)에서 확인할 수 있습니다.

<a id="about-faq-is-vue-lightweight"></a>

### Vue는 가볍나요?
빌드 도구를 사용할 때 Vue의 많은 API는 ["트리 셰이킹(tree-shaking)"](https://developer.mozilla.org/en-US/docs/Glossary/Tree_shaking)이 가능합니다. 예를 들어, 기본 제공 `<Transition>` 컴포넌트(component)를 사용하지 않으면 최종 프로덕션 번들에 포함되지 않습니다.

최소한의 API만 사용하는 헬로 월드 Vue 앱의 기본 크기는 축소 및 브로틀리 압축을 통해 약 **16KB**에 불과합니다. 애플리케이션의 실제 크기는 프레임워크에서 사용하는 선택적 기능의 수에 따라 달라집니다. 드물지만 앱이 Vue가 제공하는 모든 기능을 사용하는 경우 총 런타임 크기는 약 **27KB**입니다.

빌드 도구 없이 Vue를 사용하면 트리 셰이킹이 불가능할 뿐만 아니라 템플릿 컴파일러를 브라우저로 전송해야 합니다. 이렇게 하면 크기가 약 **41KB**로 늘어납니다. 따라서 빌드 단계 없이 주로 점진적 개선을 위해 Vue를 사용하는 경우 [petite-vue](https://github.com/vuejs/petite-vue)(**6KB**에 불과)를 대신 사용하는 것이 좋습니다.

Svelte와 같은 일부 프레임워크는 단일 컴포넌트 시나리오에서 매우 가벼운 출력을 생성하는 컴파일 전략을 사용합니다. 그러나 [우리의 연구](https://github.com/yyx990803/vue-svelte-size-analysis)에 따르면 애플리케이션의 컴포넌트 수에 따라 크기 차이가 크게 달라지는 것으로 나타났습니다. Vue는 기준 크기가 더 무겁지만 컴포넌트당 생성되는 코드가 더 적습니다. 실제 시나리오에서는 Vue 앱이 더 가벼워질 수 있습니다.

<a id="about-faq-does-vue-scale"></a>

### Vue는 확장되나요?
예. Vue는 단순한 사용 사례에만 적합하다는 일반적인 오해에도 불구하고 대규모 애플리케이션을 완벽하게 처리할 수 있습니다:

- [싱글 파일 컴포넌트](01_getting_started_and_tutorial.md#guide-scaling-up-sfc)는 애플리케이션의 여러 부분을 개별적으로 개발할 수 있는 모듈화된 개발 모델을 제공합니다.

- [컴포지션 API](03_components_and_reusability.md#guide-reusability-composables)는 최고 수준의 TypeScript 통합을 제공하며 복잡한 로직을 구성, 추출 및 재사용할 수 있는 깔끔한 패턴을 지원합니다.

- [포괄적인 툴링 지원](05_scaling_typescript_and_best_practices.md#guide-scaling-up-tooling)은 애플리케이션이 성장함에 따라 원활한 개발 환경을 보장합니다.

- 진입 장벽이 낮고 문서화가 우수하여 신규 개발자의 온보딩 및 교육 비용을 절감할 수 있습니다.

<a id="about-faq-how-do-i-contribute-to-vue"></a>

### Vue에 기여하려면 어떻게 해야 하나요?
관심을 가져 주셔서 감사합니다! [커뮤니티 가이드](https://ko.vuejs.org/about/community-guide)를 확인하시기 바랍니다.

<a id="about-faq-should-i-use-options-api-or-composition-api"></a>

### 옵션 API와 컴포지션 API 중 어떤 것을 사용해야 하나요?
Vue를 처음 사용하는 경우, [여기](01_getting_started_and_tutorial.md#guide-introduction-which-to-choose)에서 두 스타일 간의 개략적인 비교를 확인할 수 있습니다.

이전에 옵션 API를 사용했고 현재 컴포지션 API를 평가 중인 경우 [이 FAQ](06_reactivity_and_rendering_in_depth.md#guide-extras-composition-api-faq)를 확인하세요.

<a id="about-faq-should-i-use-javascript-or-typescript-with-vue"></a>

### Vue에서 JavaScript 또는 타입스크립트를 사용해야 하나요?
Vue 자체는 TypeScript로 구현되어 있고 최고 수준의 TypeScript 지원을 제공하지만, 사용자가 TypeScript를 사용해야 하는지에 대한 의견을 강요하지는 않습니다.

TypeScript 지원은 Vue에 새로운 기능이 추가될 때 중요한 고려 사항입니다. TypeScript를 염두에 두고 설계된 API는 일반적으로 TypeScript를 직접 사용하지 않더라도 IDE와 린터가 더 쉽게 이해할 수 있습니다. 모두에게 이득입니다. 또한 Vue API는 JavaScript와 TypeScript 모두에서 가능한 한 동일한 방식으로 작동하도록 설계되었습니다.

TypeScript를 채택하려면 온보딩 복잡성과 장기적인 유지보수성 향상 사이에서 절충점을 찾아야 합니다. 이러한 절충안이 정당화될 수 있는지 여부는 팀의 배경과 프로젝트 규모에 따라 달라질 수 있지만 Vue는 실제로 이러한 결정을 내리는 데 영향을 미치는 요인이 아닙니다.

<a id="about-faq-how-does-vue-compare-to-web-components"></a>

### Vue는 웹 컴포넌트와 어떻게 비교하나요?
Vue는 웹 컴포넌트가 기본적으로 제공되기 전에 만들어졌으며, Vue 디자인의 일부 측면(예: 슬롯(slot))은 웹 컴포넌트 모델에서 영감을 얻었습니다.

웹 컴포넌트 사양은 사용자 정의 앨리먼트를 정의하는 데 중점을 두기 때문에 상대적으로 낮은 수준입니다. 프레임워크인 Vue는 효율적인 DOM 렌더링, 반응형 상태 관리, 툴링, 클라이언트 측 라우팅(routing) 및 서버 측 렌더링과 같은 추가적인 상위 수준의 문제를 해결합니다.

Vue는 네이티브 사용자 정의 앨리먼트를 사용하거나 내보내는 기능도 완벽하게 지원합니다. 자세한 내용은 [Vue 및 웹 컴포넌트 가이드](06_reactivity_and_rendering_in_depth.md#guide-extras-web-components)를 참조하세요.

<!-- ## TODO How does Vue compare to React? -->

<!-- ## TODO How does Vue compare to Angular? -->

---

<a id="about-releases"></a>

**문서 데모 설정 코드**

```vue
<script setup>
import { ref, onMounted } from 'vue'

const version = ref()

onMounted(async () => {
  const res = await fetch('https://api.github.com/repos/vuejs/core/releases/latest')
  version.value = (await res.json()).name
})
</script>
```



<a id="about-releases-releases"></a>

## 릴리스

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/about/releases.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/about/releases.md

최신 안정 버전은 https://github.com/vuejs/core/releases/latest 에서 확인할 수 있습니다. 공식 사이트의 버전 조회 코드는 위에 보존했습니다.

지난 릴리스의 전체 변경 로그는 [GitHub](https://github.com/vuejs/core/blob/main/CHANGELOG.md)에서 확인할 수 있습니다.

<a id="about-releases-3-5"></a>

### 버전 3.5 (현재 버전)
- Vue 3.5는 2024년 9월 3일에 릴리스되었습니다.
- 실제 3.5 릴리스 내역은 [여기](https://github.com/vuejs/core/blob/main/CHANGELOG.md)에서 확인할 수 있습니다.
- Reactivity: 버전 카운팅·이중 연결 리스트로 구조를 재설계해 성능을 높이고, 배열 추적을 최적화했습니다. 새 API(예: `onEffectCleanup`)와 워치(`watch`) 일시중지·재개 기능도 추가되었습니다.
- SSR: `useId()`와 `app.config.idPrefix`로 ID 관리가 유연해졌으며, 일부 컴포넌트(component)의 지연(lazy) 하이드레이션(hydration)과 `data-allow-mismatch` 지원이 도입되었습니다.
- Custom Element: `useHost()`, `useShadowRoot()` 등으로 커스텀 엘리먼트 호스트·섀도우 루트 접근이 쉬워졌으며, `defineCustomElement()` 관련 설정 옵션이 크게 확장되었습니다.
- Teleport: 텔레포트 시점을 늦출 수 있는 Deferred Teleport가 추가되고, Transition 안에 Teleport를 직접 배치해 트랜지션(transition)을 결합할 수 있게 되었습니다.
- Misc: `useTemplateRef()`, `app.onUnmount()` 등이 새로 도입되었고, `app.config.throwUnhandledErrorInProduction`으로 프로덕션 에러 처리를 세밀하게 제어할 수 있습니다.
- Internals: `CustomRefs`의 값 캐시 로직 도입으로 내부 성능이 향상되었고, 언어 툴을 위한 추가 타입 옵션이 마련되었습니다.

<a id="about-releases-3-4"></a>

### 버전 3.4
- Vue 3.4는 2023년 12월 29일에 릴리스되었습니다.
- 실제 3.4 릴리스 내역은 [여기](https://github.com/vuejs/core/blob/main/changelogs/CHANGELOG-3.4.md)에서 확인할 수 있습니다.
- `defineModel`의 `propDefaults` 지원. `defineModel`에서 `propDefaults`를 사용할 수 있도록 개선되어, 기본값을 명시적으로 지정할 수 있습니다. 컴파일러에서 `defineProps`를 위한 `$default` 추가
- `defineProps`의 기본값을 보다 직관적으로 설정할 수 있도록 `$default` 키워드가 추가되었습니다.
- `useAttrs` 반환 값이 Proxy로 변경. `useAttrs()`의 반환 값이 Proxy로 변경되어, 반응형 속성 감지를 개선하고 확장성을 높였습니다.
- 서버 사이드 렌더링(SSR)에서 `v-once` 최적화 지원. SSR에서 `v-once`를 사용할 경우, 정적인 렌더링(rendering)을 더욱 효과적으로 활용할 수 있도록 개선되었습니다.
- `@vue/reactivity-transform` 제거.

<a id="about-releases-3-3"></a>

### 버전 3.3
- Vue 3.3은 2023년 5월 1일에 릴리스되었습니다.
- 실제 3.3 릴리스 내역은 [여기](https://github.com/vuejs/core/blob/main/changelogs/CHANGELOG-3.3.md)에서 확인할 수 있습니다.
- `defineModel` 매크로가 추가되어, `v-model`을 더욱 간편하게 선언하고 사용할 수 있습니다.
- `defineProps`, `defineEmits`, `defineExpose`, `defineSlots` 등 SFC Script Setup 매크로들이 전반적으로 개선되었습니다.
- `<script setup>` 사용 시 타입 추론 및 정의가 보다 유연해졌습니다.
- 새로운 매크로와 기능들이 DevTools와 더 긴밀하게 연동될 수 있도록 업데이트되었습니다.
- SSR 환경에서의 동작 방식을 좀 더 유연하게 다룰 수 있는 개선 사항들이 반영되었습니다.

<a id="about-releases-3-2"></a>

### 버전 3.2
- Vue 3.2는 2021년 8월 9일에 릴리스되었습니다.
- 실제 3.2 릴리스 내역은 [여기](https://github.com/vuejs/core/blob/main/changelogs/CHANGELOG-3.2.md)에서 확인할 수 있습니다.
- `<script setup>` 정식 지원. SFC 내부에서 setup() 로직을 보다 간결하게 작성할 수 있으며, 자동으로 컴파일 단계에서 분리되는 방식을 제공합니다.
- SFC Script Setup 매크로. `defineProps`, `defineEmits`, `defineExpose` 등의 매크로를 통해, props, 이벤트, 컴포넌트 인터페이스를 명시적으로 선언할 수 있습니다.
- 동적 스타일 바인딩(binding)(`v-bind`). `<style>` 블록 안에서 변수를 바인딩해 테마 변경이나 조건부 스타일을 간편하게 적용할 수 있도록 지원합니다.
- 새로운 디렉티브(directive) `v-memo`. 동일한 값(또는 props)인 경우 렌더링을 최소화하여, 컴포넌트의 불필요한 재렌더링을 줄입니다.
- `v-model` 확장. 여러 개의 v-model을 한 컴포넌트에서 동시에 제어할 수 있게 되어, 복합 폼 요소나 사용자 입력 처리 시 유연성이 높아졌습니다.
- DevTools 개선. 새롭게 추가된 `<script setup>` 구조나 매크로들이 DevTools에서 구분되어 표시되며, 컴포넌트 상태 추적이 더욱 직관적입니다.
- SSR(서버 사이드 렌더링) 기능 강화. Suspense, Teleport 같은 고급 기능의 SSR 적용이 더 수월해졌으며, 부분적인 하이드레이션 등 추가 기능도 점진적으로 지원하고 있습니다.
- `defineAsyncComponent` 개선. 비동기 컴포넌트를 선언할 때 로딩 지연 처리, 에러 핸들링이 간소화되어, 코드 분할(코드 스플리팅, code splitting)이 좀 더 유연해졌습니다.
- TypeScript 호환성 강화. SFC의 `<script setup>`에서 타입 정의, 컴포넌트 옵션 추론 등이 개선되어, IDE 자동 완성과 타입 검증 정확도가 높아졌습니다.

<a id="about-releases-3-1"></a>

### 버전 3.1
- Vue 3.1은 2021년 6월 7일에 릴리스되었습니다.
- 실제 3.1 릴리스 내역은 [여기](https://github.com/vuejs/core/blob/main/changelogs/CHANGELOG-3.1.md)에서 확인할 수 있습니다.
- 컴포지션 API 개선: 기존 컴포지션 API의 활용성을 높이는 기능들이 추가되어, setup() 내부에서 상태를 다루고 재사용하는 방식이 한층 편리해졌습니다.
- `emits` 옵션 확장: 컴포넌트에서 `props`처럼 명시적으로 `emits`를 선언할 수 있으며, 이벤트 타입을 보다 직관적으로 관리할 수 있게 되었습니다.
- DevTools 연동 향상: 새롭게 적용된 API 변경 사항이 DevTools에서도 원활히 표시되도록 기능이 보강되었습니다.
- SSR 측면 업데이트: 서버사이드 렌더링 시 일부 새로운 기능 및 옵션이 제공되어, 동적 콘텐츠 처리와 성능 제어가 더욱 유연해졌습니다.
- TypeScript 지원 강화: Vue 내부 타입 정의가 개선되어 에디터 자동 완성 및 타입 체킹 환경이 보다 정교해졌습니다.

<a id="about-releases-3-0"></a>

### 버전 3.0
- Vue 3.0은 2020년 9월 18일에 릴리스되었습니다.
- 실제 3.0 릴리스 내역은 [여기](https://github.com/vuejs/core/blob/main/changelogs/CHANGELOG-3.0.md)에서 확인할 수 있습니다.
- 컴포지션 API 도입: `setup()` 함수를 통해 더 유연한 컴포넌트 구성 및 코드 재사용이 가능해졌습니다.
- 새로운 반응성(reactivity) 시스템: 내부 구조가 개선되어 더 가볍고 빠른 반응성 처리가 제공됩니다.
- Fragments 지원: 컴포넌트에서 루트 요소 없이 여러 자식을 반환할 수 있습니다.
- Teleport: 특정 DOM 위치로 컴포넌트 콘텐츠를 렌더링하는 기능이 추가되었습니다.
- Suspense: 비동기 컴포넌트 렌더링 시 로딩 상태를 보다 쉽게 처리할 수 있습니다.
- 새로운 전역 API(createApp): 전역 설정과 마운트(mount) 과정을 보다 명확하게 분리할 수 있게 되었습니다.
- `v-model` 개선: 인자 방식이 간소화되는 등 양방향 데이터 바인딩의 사용성이 높아졌습니다.
- TypeScript 통합 강화: Vue 3.0 구조에 맞춘 타입 정의로 개발자 경험이 향상되었습니다.


<a id="about-releases-release-cycle"></a>

### 릴리스 주기
Vue에는 고정된 릴리스 주기가 없습니다.

- 패치 릴리스는 필요에 따라 이루어집니다.

- 마이너 릴리스에는 항상 새로운 기능이 포함되며, 일반적으로 3~6개월의 간격이 있습니다. 마이너 릴리스는 항상 베타 사전 릴리스 단계를 거칩니다.

- 주요 릴리스는 미리 발표되며, 초기 논의 단계와 알파/베타 사전 릴리스 단계를 거칩니다.

<a id="about-releases-semantic-versioning-edge-cases"></a>

### 시맨틱 버전 관리 예외 사례
Vue 릴리스는 [시맨틱 버전 관리](https://semver.org/)를 따르되, 몇 가지 예외 사례가 있습니다.

<a id="about-releases-typescript-definitions"></a>

#### 타입스크립트 정의
**마이너** 버전 간에 TypeScript 정의에 호환되지 않는 변경 사항이 적용될 수 있습니다. 그 이유는 다음과 같습니다:

1. TypeScript 자체에서 마이너 버전 간에 호환되지 않는 변경 사항을 제공하는 경우가 있으며, 최신 버전의 TypeScript를 지원하기 위해 타입을 조정해야 할 수도 있습니다.

2. 최신 버전의 TypeScript에서만 사용할 수 있는 기능을 도입해야 하는 경우가 있어 최소 요구되는 TypeScript 버전이 높아질 수 있습니다.

TypeScript를 사용하는 경우 현재 마이너 버전을 잠그는 semver 범위를 사용하고 새로운 마이너 버전의 Vue가 릴리스될 때 수동으로 업그레이드할 수 있습니다.

<a id="about-releases-compiled-code-compatibility-with-older-runtime"></a>

#### 이전 런타임과의 컴파일된 코드 호환성
최신 **마이너** 버전의 Vue 컴파일러는 이전 마이너 버전의 Vue 런타임과 호환되지 않는 코드를 생성할 수 있습니다. 예를 들어, Vue 3.2 컴파일러에서 생성된 코드가 Vue 3.1의 런타임에서 사용되는 경우 완전히 호환되지는 않을 수 있습니다.

이는 애플리케이션에서 컴파일러 버전과 런타임 버전이 항상 동일하기 때문에 라이브러리 작성자에게만 해당되는 문제입니다. 버전 불일치는 사전 컴파일된 Vue 컴포넌트 코드를 패키지로 출시하고 소비자가 이전 버전의 Vue를 사용하는 프로젝트에서 해당 코드를 사용하는 경우에만 발생할 수 있습니다. 따라서 패키지에 필요한 최소 마이너 버전을 명시적으로 선언해야 할 수 있습니다.

<a id="about-releases-pre-releases"></a>

### 사전 릴리스
마이너 릴리스와 주요 릴리스는 일반적으로 **알파(alpha)**, **베타(beta)**, **릴리스 후보(release candidate, RC)**라는 일련의 사전 릴리스 단계를 거칩니다. 사전 릴리스의 수와 종류는 변경 사항의 범위에 따라 달라집니다. 예를 들어, 변경 사항이 적은 마이너 릴리스는 베타 단계만 거칠 수 있지만, 주요 릴리스는 충분한 테스트와 커뮤니티 피드백을 위해 보통 세 단계를 모두 거칩니다.

최신 사전 릴리스는 `npx install-vue@alpha`, `npx install-vue@beta`, `npx install-vue@rc` 명령으로 npm에서 설치할 수 있습니다. 태그가 지정된 사전 릴리스에 아직 포함되지 않은 변경 사항을 테스트하고 싶다면, [vuejs/core](https://github.com/vuejs/core) 저장소의 모든 커밋이 임시 지속 릴리스(continuous-release) 프리뷰로 발행되므로, `npx install-vue@edge` 명령으로 설치할 수 있습니다.

사전 릴리스는 통합과 안정성을 테스트하고, 먼저 사용해 보는 개발자에게 불안정한 기능의 피드백을 받기 위해 제공합니다. 사전 릴리스는 프로덕션 환경에서 사용하지 마세요. 모든 사전 릴리스는 불안정한 것으로 간주되며 그 사이에 중요한 변경 사항이 적용될 수 있으므로, 사용할 때는 항상 정확한 버전으로 고정하세요.

<a id="about-releases-deprecations"></a>

### 사용 중단
더 나은 새 대체재가 있는 기능은 마이너 릴리스에서 주기적으로 사용 중단될 수 있습니다. 사용 중단된 기능은 계속 작동하며 사용 중단 상태로 전환된 후 다음 주요 릴리스에서 제거됩니다.

<a id="about-releases-rfcs"></a>

### RFC
API 표면이 상당히 넓고 Vue에 주요 변경 사항이 있는 새로운 기능은 **의견 요청**(RFC) 프로세스를 거치게 됩니다. RFC 프로세스는 새로운 기능이 프레임워크에 진입하는 일관되고 통제된 경로를 마련하고, 사용자가 디자인 프로세스에 참여해 피드백을 남길 기회를 주기 위한 것입니다.

RFC 프로세스는 GitHub의 [vuejs/rfcs](https://github.com/vuejs/rfcs) 리포지토리에서 진행됩니다.

<a id="about-releases-experimental-features"></a>

### 실험적 기능
일부 기능은 안정된 버전의 Vue에서 제공되고 문서화되지만 실험적이라고 표시되어 있습니다. 실험적 기능은 일반적으로 관련 RFC 토론이 있는 기능으로, 문서상으로는 대부분의 디자인 문제가 해결되었지만 실제 사용에 대한 피드백이 아직 부족한 상태입니다.

실험적 기능의 목표는 사용자가 불안정한 버전의 Vue를 사용하지 않고도 프로덕션 환경에서 테스트하여 피드백을 제공할 수 있도록 하는 것입니다. 실험적 기능 자체는 불안정한 것으로 간주되며, 기능이 릴리스 유형 간에 변경될 수 있음을 예상하고 통제된 방식으로만 사용해야 합니다.

---

<a id="glossary-index"></a>

<a id="glossary-index-glossary"></a>

## 용어집

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/glossary/index.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/glossary/index.md

이 용어집은 Vue 문서와 개발 과정에서 자주 쓰는 기술 용어를 설명합니다. 일반적인 쓰임을 정리한 것이므로 모든 문맥에 적용되는 규정으로 볼 필요는 없습니다. 같은 용어도 문맥에 따라 의미나 뉘앙스가 조금씩 달라질 수 있습니다.

[[TOC]]

<a id="glossary-index-async-component"></a>

### 비동기 컴포넌트
*비동기 컴포넌트*는 다른 컴포넌트를 감싸는 래퍼로, 감싸진 컴포넌트를 지연 로딩할 수 있게 해줍니다. 이는 일반적으로 빌드된 `.js` 파일의 크기를 줄이고, 필요할 때만 더 작은 청크로 분할하여 로드할 수 있도록 하는 방법으로 사용됩니다.

Vue Router에도 [라우트 컴포넌트의 지연 로딩](https://router.vuejs.org/guide/advanced/lazy-loading.html)을 위한 유사한 기능이 있지만, 이는 Vue의 비동기 컴포넌트 기능을 사용하지 않습니다.

자세한 내용은 다음을 참고하세요:
- [가이드 - 비동기 컴포넌트](03_components_and_reusability.md#guide-components-async)

<a id="glossary-index-compiler-macro"></a>

### 컴파일러 매크로
*컴파일러 매크로*는 컴파일러가 처리하여 다른 것으로 변환하는 특별한 코드입니다. 이는 사실상 문자열 치환의 영리한 형태입니다.

Vue의 [SFC](09_style_guide_examples_and_reference.md#glossary-index-single-file-component) 컴파일러는 `defineProps()`, `defineEmits()`, `defineExpose()`와 같은 다양한 매크로를 지원합니다. 이러한 매크로는 의도적으로 일반 JavaScript 함수처럼 보이도록 설계되어, JavaScript/TypeScript에 사용되는 것과 동일한 파서와 타입 추론 도구를 활용할 수 있습니다. 하지만 이들은 실제로 브라우저에서 실행되는 함수가 아닙니다. 이 매크로들은 컴파일러가 감지하여 실제로 실행될 진짜 JavaScript 코드로 대체하는 특별한 문자열입니다.

매크로는 일반 JavaScript 코드에는 적용되지 않는 사용상의 제한이 있습니다. 예를 들어, `const dp = defineProps`로 `defineProps`의 별칭을 만들 수 있다고 생각할 수 있지만, 실제로는 오류가 발생합니다. 또한 `defineProps()`에 전달할 수 있는 값에도 제한이 있는데, '인자'는 런타임이 아니라 컴파일러에서 처리되어야 합니다.

자세한 내용은 다음을 참고하세요:
- [`<script setup>` - `defineProps()` & `defineEmits()`](08_component_and_advanced_apis.md#api-sfc-script-setup-defineprops-defineemits)
- [`<script setup>` - `defineExpose()`](08_component_and_advanced_apis.md#api-sfc-script-setup-defineexpose)

<a id="glossary-index-component"></a>

### 컴포넌트
*컴포넌트*라는 용어는 Vue에만 국한된 것이 아닙니다. 이는 많은 UI 프레임워크에서 공통적으로 사용되며, 버튼이나 체크박스와 같은 UI의 일부를 설명합니다. 컴포넌트는 더 큰 컴포넌트로 결합될 수도 있습니다.

컴포넌트는 UI를 더 작은 조각으로 분할하여 유지보수성을 높이고 코드 재사용을 가능하게 하는 Vue의 주요 메커니즘입니다.

Vue 컴포넌트는 객체입니다. 모든 속성은 선택 사항이지만, 컴포넌트가 렌더링되려면 템플릿이나 렌더 함수 중 하나가 필요합니다. 예를 들어, 다음 객체는 유효한 컴포넌트가 됩니다:

```js
const HelloWorldComponent = {
  render() {
    return 'Hello world!'
  }
}
```

실제로 대부분의 Vue 애플리케이션은 [싱글 파일 컴포넌트](09_style_guide_examples_and_reference.md#glossary-index-single-file-component) (`.vue` 파일)를 사용하여 작성됩니다. 이러한 컴포넌트는 처음에는 객체처럼 보이지 않을 수 있지만, SFC 컴파일러가 이를 객체로 변환하여 파일의 기본 내보내기로 사용합니다. 외부 관점에서 보면 `.vue` 파일은 컴포넌트 객체를 내보내는 ES 모듈일 뿐입니다.

컴포넌트 객체의 속성은 일반적으로 *옵션*이라고 불립니다. [옵션 API](09_style_guide_examples_and_reference.md#glossary-index-options-api)의 이름도 여기서 유래합니다.

컴포넌트의 옵션은 해당 컴포넌트의 인스턴스가 어떻게 생성되어야 하는지 정의합니다. 컴포넌트는 개념적으로 클래스와 유사하지만, Vue는 실제 JavaScript 클래스를 사용하여 컴포넌트를 정의하지는 않습니다.

컴포넌트라는 용어는 컴포넌트 인스턴스를 느슨하게 지칭하는 데에도 사용될 수 있습니다.

자세한 내용은 다음을 참고하세요:
- [가이드 - 컴포넌트 기본](02_essentials.md#guide-essentials-component-basics)

'컴포넌트'라는 단어는 다음과 같은 여러 용어에도 등장합니다:
- [비동기 컴포넌트](09_style_guide_examples_and_reference.md#glossary-index-async-component)
- [동적 컴포넌트](09_style_guide_examples_and_reference.md#glossary-index-dynamic-component)
- [함수형 컴포넌트](09_style_guide_examples_and_reference.md#glossary-index-functional-component)
- [웹 컴포넌트](09_style_guide_examples_and_reference.md#glossary-index-web-component)

<a id="glossary-index-composable"></a>

### 컴포저블
*컴포저블*이라는 용어는 Vue에서 일반적으로 사용되는 패턴을 설명합니다. 이는 Vue의 [컴포지션 API](09_style_guide_examples_and_reference.md#glossary-index-composition-api)를 사용하는 방법일 뿐, 별도의 기능은 아닙니다.

* 컴포저블은 함수입니다.
* 컴포저블은 상태가 있는 로직을 캡슐화하고 재사용하는 데 사용됩니다.
* 함수 이름은 일반적으로 `use`로 시작하여, 다른 개발자들이 이것이 컴포저블임을 알 수 있게 합니다.
* 이 함수는 일반적으로 컴포넌트의 `setup()` 함수(또는 그와 동등한 `<script setup>` 블록)에서 동기적으로 호출되는 것이 기대됩니다. 이는 `provide()`, `inject()`, `onMounted()` 호출 등을 통해 컴포저블의 호출을 현재 컴포넌트 컨텍스트에 연결합니다.
* 컴포저블은 일반적으로 반응형 객체가 아닌 일반 객체를 반환합니다. 이 객체는 보통 ref와 함수들을 포함하며, 호출 코드 내에서 구조 분해 할당되는 것이 기대됩니다.

많은 패턴과 마찬가지로, 특정 코드가 컴포저블로 분류될 수 있는지에 대해 의견이 다를 수 있습니다. 모든 JavaScript 유틸리티 함수가 컴포저블인 것은 아닙니다. 함수가 컴포지션 API를 사용하지 않는다면 아마도 컴포저블이 아닐 것입니다. `setup()`의 동기 실행 중에 호출되는 것이 기대되지 않는다면 역시 컴포저블이 아닐 것입니다. 컴포저블은 상태가 있는 로직을 캡슐화하는 데에만 사용되며, 단순히 함수의 명명 규칙이 아닙니다.

컴포저블 작성에 대한 자세한 내용은 [가이드 - 컴포저블](03_components_and_reusability.md#guide-reusability-composables)을 참고하세요.

<a id="glossary-index-composition-api"></a>

### 컴포지션 API
*컴포지션 API*는 Vue에서 컴포넌트와 컴포저블을 작성하는 데 사용되는 함수들의 모음입니다.

이 용어는 컴포넌트를 작성하는 두 가지 주요 스타일 중 하나를 설명하는 데에도 사용됩니다. 다른 하나는 [옵션 API](09_style_guide_examples_and_reference.md#glossary-index-options-api)입니다. 컴포지션 API를 사용하여 작성된 컴포넌트는 `<script setup>` 또는 명시적인 `setup()` 함수를 사용합니다.

자세한 내용은 [컴포지션 API FAQ](06_reactivity_and_rendering_in_depth.md#guide-extras-composition-api-faq)를 참고하세요.

<a id="glossary-index-custom-element"></a>

### 커스텀 엘리먼트
*커스텀 엘리먼트*는 최신 웹 브라우저에 구현된 [웹 컴포넌트](09_style_guide_examples_and_reference.md#glossary-index-web-component) 표준의 기능입니다. 이는 HTML 마크업에서 커스텀 HTML 엘리먼트를 사용하여 해당 위치에 웹 컴포넌트를 포함할 수 있는 기능을 의미합니다.

Vue는 커스텀 엘리먼트 렌더링을 기본적으로 지원하며, Vue 컴포넌트 템플릿에서 직접 사용할 수 있습니다.

커스텀 엘리먼트를 다른 Vue 컴포넌트의 템플릿 내에서 Vue 컴포넌트를 태그로 포함하는 기능과 혼동해서는 안 됩니다. 커스텀 엘리먼트는 Vue 컴포넌트가 아니라 웹 컴포넌트를 만들 때 사용됩니다.

자세한 내용은 다음을 참고하세요:
- [가이드 - Vue와 웹 컴포넌트](06_reactivity_and_rendering_in_depth.md#guide-extras-web-components)

<a id="glossary-index-directive"></a>

### 디렉티브
*디렉티브*라는 용어는 `v-` 접두사로 시작하는 템플릿 속성이나 그에 해당하는 축약형을 의미합니다.

내장 디렉티브에는 `v-if`, `v-for`, `v-bind`, `v-on`, `v-slot` 등이 있습니다.

Vue는 커스텀 디렉티브 생성도 지원하지만, 일반적으로 DOM 노드를 직접 조작해야 할 때 '탈출구'로만 사용됩니다. 커스텀 디렉티브로 내장 디렉티브의 기능을 재현하는 것은 일반적으로 불가능합니다.

자세한 내용은 다음을 참고하세요:
- [가이드 - 템플릿 문법 - 디렉티브](02_essentials.md#guide-essentials-template-syntax-directives)
- [가이드 - 커스텀 디렉티브](03_components_and_reusability.md#guide-reusability-custom-directives)

<a id="glossary-index-dynamic-component"></a>

### 동적 컴포넌트
*동적 컴포넌트*라는 용어는 어떤 자식 컴포넌트를 렌더링할지 동적으로 결정해야 하는 경우를 설명할 때 사용됩니다. 일반적으로 `<component :is="type">`를 사용하여 구현합니다.

동적 컴포넌트는 특별한 유형의 컴포넌트가 아닙니다. 모든 컴포넌트는 동적 컴포넌트로 사용될 수 있습니다. 동적인 것은 컴포넌트 자체가 아니라, 선택되는 컴포넌트입니다.

자세한 내용은 다음을 참고하세요:
- [가이드 - 컴포넌트 기본 - 동적 컴포넌트](02_essentials.md#guide-essentials-component-basics-dynamic-components)

<a id="glossary-index-effect"></a>

### 이펙트
[반응형 이펙트](09_style_guide_examples_and_reference.md#glossary-index-reactive-effect) 및 [사이드 이펙트](09_style_guide_examples_and_reference.md#glossary-index-side-effect)를 참고하세요.

<a id="glossary-index-event"></a>

### 이벤트
프로그램의 여러 부분 간 통신을 위해 이벤트를 사용하는 것은 다양한 프로그래밍 영역에서 공통적으로 나타납니다. Vue 내에서는 이 용어가 네이티브 HTML 엘리먼트 이벤트와 Vue 컴포넌트 이벤트 모두에 일반적으로 적용됩니다. `v-on` 디렉티브는 템플릿에서 두 가지 유형의 이벤트를 모두 수신하는 데 사용됩니다.

자세한 내용은 다음을 참고하세요:
- [가이드 - 이벤트 처리](02_essentials.md#guide-essentials-event-handling)
- [가이드 - 컴포넌트 이벤트](03_components_and_reusability.md#guide-components-events)

<a id="glossary-index-fragment"></a>

### 프래그먼트
*프래그먼트*라는 용어는 다른 [VNode](09_style_guide_examples_and_reference.md#glossary-index-vnode)의 부모로 사용되지만, 자체적으로는 어떤 엘리먼트도 렌더링하지 않는 특별한 유형의 VNode를 의미합니다.

이 이름은 네이티브 DOM API의 [`DocumentFragment`](https://developer.mozilla.org/en-US/docs/Web/API/DocumentFragment)와 유사한 개념에서 유래했습니다.

프래그먼트는 여러 루트 노드를 가진 컴포넌트를 지원하기 위해 사용됩니다. 이러한 컴포넌트는 여러 루트를 가진 것처럼 보일 수 있지만, 내부적으로는 프래그먼트 노드를 단일 루트로 사용하여 '루트' 노드들의 부모로 삼습니다.

프래그먼트는 또한 템플릿 컴파일러가 여러 동적 노드를 감싸는 방법으로 사용됩니다. `v-for`나 `v-if`로 생성된 노드들이 그 예입니다. 이를 통해 [VDOM](09_style_guide_examples_and_reference.md#glossary-index-virtual-dom) 패칭 알고리즘에 추가 힌트를 전달할 수 있습니다. 대부분은 내부적으로 처리되지만, `v-for`가 있는 `<template>` 태그에 `key`를 사용하는 경우 이를 직접 접할 수 있습니다. 이 경우 `key`는 프래그먼트 VNode의 [prop](09_style_guide_examples_and_reference.md#glossary-index-prop)으로 추가됩니다.

프래그먼트 노드는 현재 DOM에 빈 텍스트 노드로 렌더링되지만, 이는 구현 세부사항입니다. `$el`을 사용하거나 내장 브라우저 API로 DOM을 순회하려고 할 때 이러한 텍스트 노드를 볼 수 있습니다.

<a id="glossary-index-functional-component"></a>

### 함수형 컴포넌트
컴포넌트 정의는 일반적으로 옵션을 포함하는 객체입니다. `<script setup>`을 사용하는 경우에는 그렇게 보이지 않을 수 있지만, `.vue` 파일에서 내보내는 컴포넌트는 여전히 객체입니다.

*함수형 컴포넌트*는 객체 대신 함수로 선언되는 대안적인 형태의 컴포넌트입니다. 이 함수는 컴포넌트의 [렌더 함수](09_style_guide_examples_and_reference.md#glossary-index-render-function) 역할을 합니다.

함수형 컴포넌트는 자체 상태를 가질 수 없습니다. 또한 일반적인 컴포넌트 라이프사이클을 거치지 않으므로 라이프사이클 훅을 사용할 수 없습니다. 이로 인해 상태가 있는 일반적인 컴포넌트보다 약간 더 가볍습니다.

자세한 내용은 다음을 참고하세요:
- [가이드 - 렌더 함수 & JSX - 함수형 컴포넌트](06_reactivity_and_rendering_in_depth.md#guide-extras-render-function-functional-components)

<a id="glossary-index-hoisting"></a>

### 호이스팅
*호이스팅*이라는 용어는 코드의 특정 부분에 도달하기 전에, 다른 코드보다 먼저 실행하는 것을 설명할 때 사용됩니다. 실행이 더 이른 시점으로 '끌어올려집니다'.

JavaScript는 `var`, `import`, 함수 선언과 같은 일부 구조에 대해 호이스팅을 사용합니다.

Vue에서는 컴파일러가 성능 향상을 위해 *호이스팅*을 적용합니다. 컴포넌트를 컴파일할 때, 정적인 값들은 컴포넌트의 스코프 밖으로 이동됩니다. 이러한 정적 값들은 컴포넌트 외부에서 생성되기 때문에 '호이스팅'되었다고 표현합니다.

<a id="glossary-index-cache-static"></a>

### 정적 캐시
*캐시*라는 용어는 성능 향상을 위해 자주 접근하는 데이터를 임시로 저장하는 것을 설명할 때 사용됩니다.

Vue 템플릿 컴파일러는 정적인 VNode를 식별하여, 초기 렌더링 시 캐시에 저장하고 이후의 모든 리렌더링에서 동일한 VNode를 재사용합니다.

자세한 내용은 다음을 참고하세요:
- [가이드 - 렌더링 메커니즘 - 정적 캐시](06_reactivity_and_rendering_in_depth.md#guide-extras-rendering-mechanism-cache-static)

<a id="glossary-index-in-dom-template"></a>

### in-DOM 템플릿
컴포넌트의 템플릿을 지정하는 방법에는 여러 가지가 있습니다. 대부분의 경우 템플릿은 문자열로 제공됩니다.

*in-DOM 템플릿*이라는 용어는 템플릿이 문자열이 아닌 DOM 노드 형태로 제공되는 시나리오를 의미합니다. 그러면 Vue는 `innerHTML`을 사용하여 DOM 노드를 템플릿 문자열로 변환합니다.

일반적으로 in-DOM 템플릿은 페이지의 HTML에 직접 작성된 HTML 마크업으로 시작합니다. 브라우저가 이를 DOM 노드로 파싱한 후, Vue가 `innerHTML`을 사용하여 읽어옵니다.

자세한 내용은 다음을 참고하세요:
- [가이드 - 애플리케이션 생성 - in-DOM 루트 컴포넌트 템플릿](02_essentials.md#guide-essentials-application-in-dom-root-component-template)
- [가이드 - 컴포넌트 기본 - in-DOM 템플릿 파싱 주의사항](02_essentials.md#guide-essentials-component-basics-in-dom-template-parsing-caveats)
- [옵션: 렌더링 - template](08_component_and_advanced_apis.md#api-options-rendering-template)

<a id="glossary-index-inject"></a>

### inject
[provide / inject](09_style_guide_examples_and_reference.md#glossary-index-provide-inject)를 참고하세요.

<a id="glossary-index-lifecycle-hooks"></a>

### 라이프사이클 훅
Vue 컴포넌트 인스턴스는 라이프사이클을 거칩니다. 예를 들어, 생성되고, 마운트되고, 업데이트되고, 언마운트됩니다.

*라이프사이클 훅*은 이러한 라이프사이클 이벤트를 수신하는 방법입니다.

옵션 API에서는 각 훅이 별도의 옵션(예: `mounted`)으로 제공됩니다. 컴포지션 API에서는 대신 `onMounted()`와 같은 함수를 사용합니다.

자세한 내용은 다음을 참고하세요:
- [가이드 - 라이프사이클 훅](02_essentials.md#guide-essentials-lifecycle)

<a id="glossary-index-macro"></a>

### 매크로
[컴파일러 매크로](09_style_guide_examples_and_reference.md#glossary-index-compiler-macro)를 참고하세요.

<a id="glossary-index-named-slot"></a>

### 명명된 슬롯
컴포넌트는 이름으로 구분되는 여러 슬롯을 가질 수 있습니다. 기본 슬롯이 아닌 슬롯을 *명명된 슬롯*이라고 합니다.

자세한 내용은 다음을 참고하세요:
- [가이드 - 슬롯 - 명명된 슬롯](03_components_and_reusability.md#guide-components-slots-named-slots)

<a id="glossary-index-options-api"></a>

### 옵션 API
Vue 컴포넌트는 객체를 사용하여 정의됩니다. 이러한 컴포넌트 객체의 속성을 *옵션*이라고 합니다.

컴포넌트는 두 가지 스타일로 작성할 수 있습니다. 한 가지 스타일은 [컴포지션 API](09_style_guide_examples_and_reference.md#glossary-index-composition-api)와 `setup`(명시적 `setup()` 옵션 또는 `<script setup>`을 통해)을 함께 사용합니다. 다른 스타일은 컴포지션 API를 직접적으로는 거의 사용하지 않고, 다양한 컴포넌트 옵션을 사용하여 유사한 결과를 얻습니다. 이와 같이 사용되는 컴포넌트 옵션을 *옵션 API*라고 합니다.

옵션 API에는 `data()`, `computed`, `methods`, `created()`와 같은 옵션이 포함됩니다.

`props`, `emits`, `inheritAttrs`와 같은 일부 옵션은 두 API 모두에서 컴포넌트 작성 시 사용할 수 있습니다. 이들은 컴포넌트 옵션이므로 옵션 API의 일부로 간주될 수 있습니다. 하지만 이러한 옵션은 `setup()`과 함께 사용되기도 하므로, 두 컴포넌트 스타일 모두에서 공유된다고 생각하는 것이 더 유용할 때가 많습니다.

`setup()` 함수 자체도 컴포넌트 옵션이므로, 옵션 API의 일부로 *설명될 수* 있습니다. 하지만 일반적으로 '옵션 API'라는 용어는 이렇게 사용되지 않습니다. 대신, `setup()` 함수는 컴포지션 API의 일부로 간주됩니다.

<a id="glossary-index-plugin"></a>

### 플러그인
*플러그인*이라는 용어는 다양한 맥락에서 사용될 수 있지만, Vue에는 애플리케이션에 기능을 추가하는 방법으로서의 플러그인이라는 구체적인 개념이 있습니다.

플러그인은 `app.use(plugin)`을 호출하여 애플리케이션에 추가됩니다. 플러그인 자체는 함수이거나 `install` 함수를 가진 객체입니다. 이 함수는 애플리케이션 인스턴스를 전달받아 필요한 작업을 수행할 수 있습니다.

자세한 내용은 다음을 참고하세요:
- [가이드 - 플러그인](03_components_and_reusability.md#guide-reusability-plugins)

<a id="glossary-index-prop"></a>

### prop
Vue에서 *prop*이라는 용어는 세 가지 일반적인 용도로 사용됩니다:

* 컴포넌트 prop
* VNode prop
* 슬롯 prop

*컴포넌트 prop*은 대부분의 사람들이 prop이라고 생각하는 것입니다. 이는 컴포넌트가 `defineProps()` 또는 `props` 옵션을 사용하여 명시적으로 정의한 것입니다.

*VNode prop*이라는 용어는 `h()`의 두 번째 인자로 전달되는 객체의 속성을 의미합니다. 여기에는 컴포넌트 prop도 포함될 수 있지만, 컴포넌트 이벤트, DOM 이벤트, DOM 속성, DOM 프로퍼티도 포함될 수 있습니다. VNode prop은 일반적으로 VNode를 직접 조작하기 위해 렌더 함수를 사용할 때만 접하게 됩니다.

*슬롯 prop*은 스코프 슬롯에 전달되는 속성입니다.

모든 경우에 prop은 외부에서 전달되는 속성입니다.

props라는 단어는 *properties*에서 유래했지만, Vue 맥락에서 prop은 훨씬 더 구체적인 의미를 가집니다. properties의 약어로 props를 사용하는 것은 피해야 합니다.

자세한 내용은 다음을 참고하세요:
- [가이드 - Props](03_components_and_reusability.md#guide-components-props)
- [가이드 - 렌더 함수 & JSX](06_reactivity_and_rendering_in_depth.md#guide-extras-render-function)
- [가이드 - 슬롯 - 스코프 슬롯](03_components_and_reusability.md#guide-components-slots-scoped-slots)

<a id="glossary-index-provide-inject"></a>

### provide / inject
`provide`와 `inject`는 컴포넌트 간 통신의 한 형태입니다.

컴포넌트가 값을 *제공*하면, 해당 컴포넌트의 모든 하위 컴포넌트는 `inject`를 사용하여 그 값을 가져올 수 있습니다. prop과 달리, 값을 제공하는 컴포넌트는 어떤 컴포넌트가 그 값을 받는지 정확히 알지 못합니다.

`provide`와 `inject`는 *prop 드릴링*을 피하기 위해 사용되기도 합니다. 또한 컴포넌트가 슬롯 콘텐츠와 암묵적으로 통신하는 방법으로도 사용할 수 있습니다.

`provide`는 애플리케이션 레벨에서도 사용할 수 있어, 해당 애플리케이션 내의 모든 컴포넌트에서 값을 사용할 수 있게 합니다.

자세한 내용은 다음을 참고하세요:
- [가이드 - provide / inject](03_components_and_reusability.md#guide-components-provide-inject)

<a id="glossary-index-reactive-effect"></a>

### 반응형 이펙트
*반응형 이펙트*는 Vue의 반응성 시스템의 일부입니다. 이는 함수의 의존성을 추적하고, 해당 의존성의 값이 변경될 때 그 함수를 다시 실행하는 과정을 의미합니다.

`watchEffect()`는 이펙트를 생성하는 가장 직접적인 방법입니다. 컴포넌트 렌더링 업데이트, `computed()`, `watch()` 등 Vue의 다양한 다른 부분도 내부적으로 이펙트를 사용합니다.

Vue는 반응형 이펙트 내에서만 반응형 의존성을 추적할 수 있습니다. 속성 값이 반응형 이펙트 외부에서 읽히면, 해당 속성은 '반응성'을 잃게 됩니다. 즉, 이후에 그 속성이 변경되어도 Vue가 무엇을 해야 할지 알지 못합니다.

이 용어는 '사이드 이펙트'에서 유래했습니다. 이펙트 함수를 호출하는 것은 속성 값이 변경될 때 발생하는 사이드 이펙트입니다.

자세한 내용은 다음을 참고하세요:
- [가이드 - 반응성 심층 분석](06_reactivity_and_rendering_in_depth.md#guide-extras-reactivity-in-depth)

<a id="glossary-index-reactivity"></a>

### 반응성
일반적으로 *반응성*은 데이터 변경에 자동으로 반응하여 동작을 수행하는 능력을 의미합니다. 예를 들어, 데이터 값이 변경될 때 DOM을 업데이트하거나 네트워크 요청을 보내는 것 등이 있습니다.

Vue 맥락에서 반응성은 여러 기능의 집합을 설명하는 데 사용됩니다. 이러한 기능들은 *반응성 시스템*을 구성하며, [반응성 API](09_style_guide_examples_and_reference.md#glossary-index-reactivity-api)를 통해 노출됩니다.

반응성 시스템을 구현하는 방법에는 여러 가지가 있습니다. 예를 들어, 코드의 의존성을 정적으로 분석하여 구현할 수도 있습니다. 하지만 Vue는 그런 형태의 반응성 시스템을 사용하지 않습니다.

대신, Vue의 반응성 시스템은 런타임에 속성 접근을 추적합니다. 이는 Proxy 래퍼와 속성의 [getter](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Functions/get#description)/[setter](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Functions/set#description) 함수를 모두 사용하여 이루어집니다.

자세한 내용은 다음을 참고하세요:
- [가이드 - 반응성 기초](02_essentials.md#guide-essentials-reactivity-fundamentals)
- [가이드 - 반응성 심층 분석](06_reactivity_and_rendering_in_depth.md#guide-extras-reactivity-in-depth)

<a id="glossary-index-reactivity-api"></a>

### 반응성 API
*반응성 API*는 [반응성](09_style_guide_examples_and_reference.md#glossary-index-reactivity)과 관련된 핵심 Vue 함수들의 모음입니다. 이 함수들은 컴포넌트와 독립적으로 사용할 수 있습니다. `ref()`, `reactive()`, `computed()`, `watch()`, `watchEffect()`와 같은 함수가 포함됩니다.

반응성 API는 컴포지션 API의 하위 집합입니다.

자세한 내용은 다음을 참고하세요:
- [반응성 API: Core](07_composition_and_reactivity_apis.md#api-reactivity-core)
- [반응성 API: Utilities](07_composition_and_reactivity_apis.md#api-reactivity-utilities)
- [반응성 API: Advanced](07_composition_and_reactivity_apis.md#api-reactivity-advanced)

<a id="glossary-index-ref"></a>

### ref
> 이 항목은 반응성을 위한 `ref` 사용에 관한 것입니다. 템플릿에서 사용하는 `ref` 속성에 대해서는 [템플릿 ref](09_style_guide_examples_and_reference.md#glossary-index-template-ref)를 참고하세요.

`ref`는 Vue의 반응성 시스템의 일부입니다. 이는 `value`라는 단일 반응형 속성을 가진 객체입니다.

ref에는 다양한 유형이 있습니다. 예를 들어, `ref()`, `shallowRef()`, `computed()`, `customRef()`를 사용하여 ref를 생성할 수 있습니다. `isRef()` 함수로는 객체가 ref인지, `isReadonly()`로는 ref가 값의 직접 재할당을 허용하는지 확인할 수 있습니다.

자세한 내용은 다음을 참고하세요:
- [가이드 - 반응성 기초](02_essentials.md#guide-essentials-reactivity-fundamentals)
- [반응성 API: Core](07_composition_and_reactivity_apis.md#api-reactivity-core)
- [반응성 API: Utilities](07_composition_and_reactivity_apis.md#api-reactivity-utilities)
- [반응성 API: Advanced](07_composition_and_reactivity_apis.md#api-reactivity-advanced)

<a id="glossary-index-render-function"></a>

### 렌더 함수
*렌더 함수*는 렌더링 중에 사용되는 VNode를 생성하는 컴포넌트의 일부입니다. 템플릿은 렌더 함수로 컴파일됩니다.

자세한 내용은 다음을 참고하세요:
- [가이드 - 렌더 함수 & JSX](06_reactivity_and_rendering_in_depth.md#guide-extras-render-function)

<a id="glossary-index-scheduler"></a>

### 스케줄러
*스케줄러*는 [반응형 이펙트](09_style_guide_examples_and_reference.md#glossary-index-reactive-effect)가 실행되는 타이밍을 제어하는 Vue 내부의 일부입니다.

반응형 상태가 변경되면, Vue는 즉시 렌더링 업데이트를 트리거하지 않습니다. 대신, 큐를 사용하여 이를 함께 배치(batch)합니다. 이를 통해 기본 데이터가 여러 번 변경되어도 컴포넌트가 한 번만 다시 렌더링되도록 보장합니다.

[감시자](02_essentials.md#guide-essentials-watchers)도 스케줄러 큐를 사용하여 배치됩니다. `flush: 'pre'`(기본값)인 감시자는 컴포넌트 렌더링 전에 실행되고, `flush: 'post'`인 감시자는 컴포넌트 렌더링 후에 실행됩니다.

스케줄러는 [라이프사이클 훅](09_style_guide_examples_and_reference.md#glossary-index-lifecycle-hooks) 트리거, [템플릿 ref](09_style_guide_examples_and_reference.md#glossary-index-template-ref) 업데이트 등 다양한 내부 작업을 수행하는 데에도 사용됩니다.

<a id="glossary-index-scoped-slot"></a>

### 스코프 슬롯
*스코프 슬롯*이라는 용어는 [slot](09_style_guide_examples_and_reference.md#glossary-index-slot)이 [prop](09_style_guide_examples_and_reference.md#glossary-index-prop)을 받는 경우를 의미합니다.

과거에는 Vue가 스코프 슬롯과 비스코프 슬롯을 훨씬 더 엄격하게 구분했습니다. 어느 정도는 두 가지 별도의 기능으로 간주될 수 있었으며, 공통 템플릿 문법 아래 통합되어 있었습니다.

Vue 3에서는 슬롯 API가 단순화되어 모든 슬롯이 스코프 슬롯처럼 동작하게 되었습니다. 하지만 스코프 슬롯과 비스코프 슬롯의 사용 사례가 종종 다르기 때문에, prop이 있는 슬롯을 지칭하는 용어로 여전히 유용하게 사용됩니다.

슬롯에 전달된 prop은 슬롯 콘텐츠를 정의하는 부모 템플릿의 특정 영역에서만 사용할 수 있습니다. 이 템플릿 영역은 prop에 대한 변수 스코프처럼 동작하므로 '스코프 슬롯'이라는 이름이 붙었습니다.

자세한 내용은 다음을 참고하세요:
- [가이드 - 슬롯 - 스코프 슬롯](03_components_and_reusability.md#guide-components-slots-scoped-slots)

<a id="glossary-index-sfc"></a>

### SFC
[싱글 파일 컴포넌트](09_style_guide_examples_and_reference.md#glossary-index-single-file-component)를 참고하세요.

<a id="glossary-index-side-effect"></a>

### 사이드 이펙트
*사이드 이펙트*라는 용어는 Vue에만 국한된 것이 아닙니다. 이는 로컬 스코프를 넘어 무언가를 수행하는 연산이나 함수를 설명할 때 사용됩니다.

예를 들어, `user.name = null`과 같이 속성을 설정할 때, 이로 인해 `user.name`의 값이 변경되는 것이 기대됩니다. 만약 이와 함께 Vue의 반응성 시스템이 트리거된다면, 이는 사이드 이펙트로 설명될 수 있습니다. 이것이 Vue 내에서 [반응형 이펙트](09_style_guide_examples_and_reference.md#glossary-index-reactive-effect)라는 용어의 기원입니다.

함수가 사이드 이펙트를 가진다고 설명될 때, 이는 함수가 단순히 값을 반환하는 것 외에 함수 외부에서 관찰 가능한 어떤 동작을 수행한다는 의미입니다. 예를 들어, 상태의 값을 업데이트하거나 네트워크 요청을 트리거할 수 있습니다.

이 용어는 렌더링이나 계산된 속성을 설명할 때 자주 사용됩니다. 렌더링에는 사이드 이펙트가 없어야 한다는 것이 모범 사례로 간주됩니다. 마찬가지로, 계산된 속성의 getter 함수에도 사이드 이펙트가 없어야 합니다.

<a id="glossary-index-single-file-component"></a>

### 싱글 파일 컴포넌트
*싱글 파일 컴포넌트* 또는 SFC라는 용어는 Vue 컴포넌트에 일반적으로 사용되는 `.vue` 파일 형식을 의미합니다.

또한 참고:
- [가이드 - 싱글 파일 컴포넌트](01_getting_started_and_tutorial.md#guide-scaling-up-sfc)
- [SFC 문법 명세](08_component_and_advanced_apis.md#api-sfc-spec)

<a id="glossary-index-slot"></a>

### 슬롯
슬롯은 자식 컴포넌트에 콘텐츠를 전달하는 데 사용됩니다. prop이 데이터 값을 전달하는 데 사용된다면, 슬롯은 HTML 엘리먼트와 다른 Vue 컴포넌트로 구성된 더 풍부한 콘텐츠를 전달하는 데 사용됩니다.

자세한 내용은 다음을 참고하세요:
- [가이드 - 슬롯](03_components_and_reusability.md#guide-components-slots)

<a id="glossary-index-template-ref"></a>

### 템플릿 ref
*템플릿 ref*라는 용어는 템플릿 내 태그에 `ref` 속성을 사용하는 것을 의미합니다. 컴포넌트가 렌더링된 후, 이 속성은 그 태그에 해당하는 HTML 엘리먼트나 컴포넌트 인스턴스를 동일한 이름의 속성에 할당하는 데 사용됩니다.

옵션 API를 사용하는 경우, ref는 `$refs` 객체의 속성으로 노출됩니다.

컴포지션 API를 사용하는 경우, 템플릿 ref는 동일한 이름의 반응형 [ref](09_style_guide_examples_and_reference.md#glossary-index-ref)에 할당됩니다.

템플릿 ref를 Vue의 반응성 시스템에서 사용하는 반응형 ref와 혼동해서는 안 됩니다.

자세한 내용은 다음을 참고하세요:
- [가이드 - 템플릿 ref](02_essentials.md#guide-essentials-template-refs)

<a id="glossary-index-vdom"></a>

### VDOM
[가상 DOM](09_style_guide_examples_and_reference.md#glossary-index-virtual-dom)을 참고하세요.

<a id="glossary-index-virtual-dom"></a>

### 가상 DOM
*가상 DOM* (VDOM)이라는 용어는 Vue에만 국한된 것이 아닙니다. 이는 여러 웹 프레임워크에서 UI 업데이트를 관리하는 데 사용되는 일반적인 접근 방식입니다.

브라우저는 페이지의 현재 상태를 나타내는 노드 트리를 사용합니다. 이 트리와 이를 조작하는 JavaScript API를 *문서 객체 모델* 또는 *DOM*이라고 합니다.

DOM을 조작하는 것은 주요 성능 병목입니다. 가상 DOM은 이를 관리하는 전략 중 하나입니다.

DOM 노드를 직접 생성하는 대신, Vue 컴포넌트는 자신이 원하는 DOM 노드에 대한 설명을 생성합니다. 이러한 설명자는 일반 JavaScript 객체로, VNode(가상 DOM 노드)라고 불립니다. VNode를 만드는 비용은 비교적 저렴합니다.

컴포넌트가 다시 렌더링될 때마다, 새로운 VNode 트리는 이전 VNode 트리와 비교되고, 차이점만 실제 DOM에 적용됩니다. 변경된 것이 없다면 DOM을 건드릴 필요가 없습니다.

Vue는 [컴파일러 기반 가상 DOM](06_reactivity_and_rendering_in_depth.md#guide-extras-rendering-mechanism-compiler-informed-virtual-dom)이라는 하이브리드 방식을 사용합니다. Vue의 템플릿 컴파일러는 템플릿의 정적 분석을 기반으로 성능 최적화를 적용할 수 있습니다. 컴포넌트의 이전/새 VNode 트리를 런타임에 전체 비교하는 대신, 컴파일러가 추출한 정보를 사용하여 실제로 변경될 수 있는 트리의 일부만 비교하도록 줄일 수 있습니다.

자세한 내용은 다음을 참고하세요:
- [가이드 - 렌더링 메커니즘](06_reactivity_and_rendering_in_depth.md#guide-extras-rendering-mechanism)
- [가이드 - 렌더 함수 & JSX](06_reactivity_and_rendering_in_depth.md#guide-extras-render-function)

<a id="glossary-index-vnode"></a>

### VNode
*VNode*는 *가상 DOM 노드*입니다. [`h()`](08_component_and_advanced_apis.md#api-render-function-h) 함수를 사용하여 생성할 수 있습니다.

자세한 내용은 [가상 DOM](09_style_guide_examples_and_reference.md#glossary-index-virtual-dom)을 참고하세요.

<a id="glossary-index-web-component"></a>

### 웹 컴포넌트
*웹 컴포넌트* 표준은 최신 웹 브라우저에 구현된 기능들의 모음입니다.

Vue 컴포넌트는 웹 컴포넌트가 아니지만, `defineCustomElement()`를 사용하여 Vue 컴포넌트로부터 [커스텀 엘리먼트](09_style_guide_examples_and_reference.md#glossary-index-custom-element)를 생성할 수 있습니다. Vue는 또한 Vue 컴포넌트 내에서 커스텀 엘리먼트 사용도 지원합니다.

자세한 내용은 다음을 참고하세요:
- [가이드 - Vue와 웹 컴포넌트](06_reactivity_and_rendering_in_depth.md#guide-extras-web-components)

---

<a id="error-reference-index"></a>

<a id="error-reference-index-error-reference"></a>

## 프로덕션 에러 코드 참조

> 원문: https://github.com/vuejs/docs/blob/40aa88af0094f7bab4aaf786e55c748a6a251d88/src/error-reference/index.md
> 한국어 번역 기반: https://github.com/vuejs-translations/docs-ko/blob/6e646acb5687510feda533281faa546b26ea243c/src/error-reference/index.md

<a id="error-reference-index-runtime-errors"></a>

### 런타임 에러
프로덕션 빌드에서는, 아래의 에러 핸들러 API에 전달되는 세 번째 인자가 전체 정보 문자열 대신 짧은 코드가 됩니다:

- [`app.config.errorHandler`](07_composition_and_reactivity_apis.md#api-application-app-config-errorhandler)
- [`onErrorCaptured`](07_composition_and_reactivity_apis.md#api-composition-api-lifecycle-onerrorcaptured) (컴포지션 API)
- [`errorCaptured`](08_component_and_advanced_apis.md#api-options-lifecycle-errorcaptured) (옵션 API)

아래 표는 코드와 원래의 전체 정보 문자열을 매핑한 것입니다.

| 코드 | 원문 메시지 | 한국어 설명 |
| --- | --- | --- |
| 0 | setup function | setup 함수 |
| 1 | render function | 렌더 함수 |
| 2 | watcher getter | 감시자의 getter |
| 3 | watcher callback | 감시자의 콜백 |
| 4 | watcher cleanup function | 감시자의 정리 함수 |
| 5 | native event handler | 네이티브 이벤트 핸들러 |
| 6 | component event handler | 컴포넌트 이벤트 핸들러 |
| 7 | vnode hook | VNode 훅 |
| 8 | directive hook | 디렉티브 훅 |
| 9 | transition hook | 트랜지션 훅 |
| 10 | app errorHandler | 앱의 errorHandler |
| 11 | app warnHandler | 앱의 warnHandler |
| 12 | ref function | ref 함수 |
| 13 | async component loader | 비동기 컴포넌트 로더 |
| 14 | scheduler flush | 스케줄러의 작업 일괄 처리 |
| 15 | component update | 컴포넌트 업데이트 |
| 16 | app unmount cleanup function | 앱 마운트 해제 시 정리 함수 |
| sp | serverPrefetch hook | serverPrefetch 훅 |
| bc | beforeCreate hook | beforeCreate 훅 |
| c | created hook | created 훅 |
| bm | beforeMount hook | beforeMount 훅 |
| m | mounted hook | mounted 훅 |
| bu | beforeUpdate hook | beforeUpdate 훅 |
| u | updated | updated 훅 |
| bum | beforeUnmount hook | beforeUnmount 훅 |
| um | unmounted hook | unmounted 훅 |
| a | activated hook | activated 훅 |
| da | deactivated hook | deactivated 훅 |
| ec | errorCaptured hook | errorCaptured 훅 |
| rtc | renderTracked hook | renderTracked 훅 |
| rtg | renderTriggered hook | renderTriggered 훅 |

<a id="error-reference-index-compiler-errors"></a>

### 컴파일러 에러
아래 표는 프로덕션 컴파일러 에러 코드와 원래 메시지의 매핑을 제공합니다.

| 코드 | 원문 메시지 | 한국어 설명 |
| --- | --- | --- |
| 0 | Illegal comment. | 올바르지 않은 주석입니다. |
| 1 | CDATA section is allowed only in XML context. | CDATA 구간은 XML 문맥에서만 허용됩니다. |
| 2 | Duplicate attribute. | 속성이 중복되었습니다. |
| 3 | End tag cannot have attributes. | 닫는 태그에는 속성을 지정할 수 없습니다. |
| 4 | Illegal &#x27;/&#x27; in tags. | 태그에 허용되지 않는 &#x27;/&#x27;가 있습니다. |
| 5 | Unexpected EOF in tag. | 태그 안에서 예기치 않게 파일이 끝났습니다. |
| 6 | Unexpected EOF in CDATA section. | CDATA 구간 안에서 예기치 않게 파일이 끝났습니다. |
| 7 | Unexpected EOF in comment. | 주석 안에서 예기치 않게 파일이 끝났습니다. |
| 8 | Unexpected EOF in script. | 스크립트 안에서 예기치 않게 파일이 끝났습니다. |
| 9 | Unexpected EOF in tag. | 태그 안에서 예기치 않게 파일이 끝났습니다. |
| 10 | Incorrectly closed comment. | 주석을 잘못 닫았습니다. |
| 11 | Incorrectly opened comment. | 주석을 잘못 시작했습니다. |
| 12 | Illegal tag name. Use &#x27;&amp;lt;&#x27; to print &#x27;&lt;&#x27;. | 잘못된 태그 이름입니다. &#x27;&lt;&#x27;를 출력하려면 &#x27;&amp;lt;&#x27;를 사용하세요. |
| 13 | Attribute value was expected. | 속성값이 필요합니다. |
| 14 | End tag name was expected. | 닫는 태그 이름이 필요합니다. |
| 15 | Whitespace was expected. | 공백이 필요합니다. |
| 16 | Unexpected &#x27;&lt;!--&#x27; in comment. | 주석 안에 예기치 않은 &#x27;&lt;!--&#x27;가 있습니다. |
| 17 | Attribute name cannot contain U+0022 (&quot;), U+0027 (&#x27;), and U+003C (&lt;). | 속성 이름에는 U+0022(큰따옴표), U+0027(작은따옴표), U+003C(&lt;)를 사용할 수 없습니다. |
| 18 | Unquoted attribute value cannot contain U+0022 (&quot;), U+0027 (&#x27;), U+003C (&lt;), U+003D (=), and U+0060 (`). | 따옴표로 감싸지 않은 속성값에는 U+0022(큰따옴표), U+0027(작은따옴표), U+003C(&lt;), U+003D(=), U+0060(백틱)을 사용할 수 없습니다. |
| 19 | Attribute name cannot start with &#x27;=&#x27;. | 속성 이름은 &#x27;=&#x27;로 시작할 수 없습니다. |
| 20 | Unexpected null character. | 예기치 않은 널 문자가 있습니다. |
| 21 | &#x27;&lt;?&#x27; is allowed only in XML context. | &#x27;&lt;?&#x27;는 XML 문맥에서만 허용됩니다. |
| 22 | Illegal &#x27;/&#x27; in tags. | 태그에 허용되지 않는 &#x27;/&#x27;가 있습니다. |
| 23 | Invalid end tag. | 유효하지 않은 닫는 태그입니다. |
| 24 | Element is missing end tag. | 요소에 닫는 태그가 없습니다. |
| 25 | Interpolation end sign was not found. | 텍스트 보간의 종료 기호를 찾지 못했습니다. |
| 26 | Legal directive name was expected. | 올바른 디렉티브 이름이 필요합니다. |
| 27 | End bracket for dynamic directive argument was not found. Note that dynamic directive argument cannot contain spaces. | 동적 디렉티브 인자의 닫는 대괄호를 찾지 못했습니다. 동적 인자에는 공백을 사용할 수 없습니다. |
| 28 | v-if/v-else-if is missing expression. | v-if/v-else-if에 표현식이 없습니다. |
| 29 | v-if/else branches must use unique keys. | v-if/else 분기는 서로 다른 고유한 key를 사용해야 합니다. |
| 30 | v-else/v-else-if has no adjacent v-if or v-else-if. | v-else/v-else-if 앞에 인접한 v-if 또는 v-else-if가 없습니다. |
| 31 | v-for is missing expression. | v-for에 표현식이 없습니다. |
| 32 | v-for has invalid expression. | v-for의 표현식이 유효하지 않습니다. |
| 33 | &lt;template v-for&gt; key should be placed on the &lt;template&gt; tag. | &lt;template v-for&gt;의 key는 &lt;template&gt; 태그에 지정해야 합니다. |
| 34 | v-bind is missing expression. | v-bind에 표현식이 없습니다. |
| 35 | v-on is missing expression. | v-on에 표현식이 없습니다. |
| 36 | Unexpected custom directive on &lt;slot&gt; outlet. | &lt;slot&gt; 아웃렛에 예기치 않은 커스텀 디렉티브가 있습니다. |
| 37 | Mixed v-slot usage on both the component and nested &lt;template&gt;. When there are multiple named slots, all slots should use &lt;template&gt; syntax to avoid scope ambiguity. | 컴포넌트와 중첩된 &lt;template&gt;에 v-slot을 혼용했습니다. 여러 명명된 슬롯이 있을 때는 스코프가 모호해지지 않도록 모든 슬롯에 &lt;template&gt; 문법을 사용해야 합니다. |
| 38 | Duplicate slot names found.  | 슬롯 이름이 중복되었습니다. |
| 39 | Extraneous children found when component already has explicitly named default slot. These children will be ignored. | 컴포넌트에 명시적으로 이름을 지정한 default 슬롯이 있는데 추가 자식이 있습니다. 이 자식들은 무시됩니다. |
| 40 | v-slot can only be used on components or &lt;template&gt; tags. | v-slot은 컴포넌트 또는 &lt;template&gt; 태그에서만 사용할 수 있습니다. |
| 41 | v-model is missing expression. | v-model에 표현식이 없습니다. |
| 42 | v-model value must be a valid JavaScript member expression. | v-model 값은 유효한 JavaScript 멤버 표현식이어야 합니다. |
| 43 | v-model cannot be used on v-for or v-slot scope variables because they are not writable. | v-for 또는 v-slot의 스코프 변수에는 쓸 수 없으므로 v-model을 사용할 수 없습니다. |
| 44 | v-model cannot be used on a prop, because local prop bindings are not writable.<br>Use a v-bind binding combined with a v-on listener that emits update:x event instead. | 로컬 prop 바인딩에는 쓸 수 없으므로 prop에 v-model을 사용할 수 없습니다. 대신 v-bind 바인딩과 update:x 이벤트를 내보내는 v-on 리스너를 함께 사용하세요. |
| 45 | v-model cannot be used on a const binding because it is not writable. | const 바인딩에는 쓸 수 없으므로 v-model을 사용할 수 없습니다. |
| 46 | Error parsing JavaScript expression:  | JavaScript 표현식을 파싱하는 중 오류가 발생했습니다. |
| 47 | &lt;KeepAlive&gt; expects exactly one child component. | &lt;KeepAlive&gt;에는 자식 컴포넌트가 정확히 하나 있어야 합니다. |
| 48 | &quot;prefixIdentifiers&quot; option is not supported in this build of compiler. | 이 컴파일러 빌드는 prefixIdentifiers 옵션을 지원하지 않습니다. |
| 49 | ES module mode is not supported in this build of compiler. | 이 컴파일러 빌드는 ES 모듈 모드를 지원하지 않습니다. |
| 50 | &quot;cacheHandlers&quot; option is only supported when the &quot;prefixIdentifiers&quot; option is enabled. | cacheHandlers 옵션은 prefixIdentifiers 옵션을 활성화한 경우에만 지원됩니다. |
| 51 | &quot;scopeId&quot; option is only supported in module mode. | scopeId 옵션은 모듈 모드에서만 지원됩니다. |
| 52 | @vnode-* hooks in templates are no longer supported. Use the vue: prefix instead. For example, @vnode-mounted should be changed to @vue:mounted. @vnode-* hooks support has been removed in 3.4. | 템플릿의 @vnode-* 훅은 더 이상 지원되지 않습니다. 대신 vue: 접두사를 사용하세요. 예를 들어 @vnode-mounted는 @vue:mounted로 바꿔야 합니다. @vnode-* 지원은 3.4에서 제거되었습니다. |
| 53 | v-bind with same-name shorthand only allows static argument. | v-bind의 동일 이름 축약형은 정적 인자만 허용합니다. |
| 54 | v-html is missing expression. | v-html에 표현식이 없습니다. |
| 55 | v-html will override element children. | v-html은 요소의 자식 콘텐츠를 덮어씁니다. |
| 56 | v-text is missing expression. | v-text에 표현식이 없습니다. |
| 57 | v-text will override element children. | v-text는 요소의 자식 콘텐츠를 덮어씁니다. |
| 58 | v-model can only be used on &lt;input&gt;, &lt;textarea&gt; and &lt;select&gt; elements. | v-model은 &lt;input&gt;, &lt;textarea&gt;, &lt;select&gt; 요소에서만 사용할 수 있습니다. |
| 59 | v-model argument is not supported on plain elements. | 일반 요소에서는 v-model 인자를 지원하지 않습니다. |
| 60 | v-model cannot be used on file inputs since they are read-only. Use a v-on:change listener instead. | 파일 입력은 읽기 전용이므로 v-model을 사용할 수 없습니다. 대신 v-on:change 리스너를 사용하세요. |
| 61 | Unnecessary value binding used alongside v-model. It will interfere with v-model&#x27;s behavior. | v-model과 함께 불필요한 value 바인딩을 사용했습니다. 이 바인딩은 v-model의 동작을 방해합니다. |
| 62 | v-show is missing expression. | v-show에 표현식이 없습니다. |
| 63 | &lt;Transition&gt; expects exactly one child element or component. | &lt;Transition&gt;에는 자식 요소 또는 컴포넌트가 정확히 하나 있어야 합니다. |
| 64 | Tags with side effect (&lt;script&gt; and &lt;style&gt;) are ignored in client component templates. | 부수 효과가 있는 태그(&lt;script&gt;, &lt;style&gt;)는 클라이언트 컴포넌트 템플릿에서 무시됩니다. |

표 출처: https://ko.vuejs.org/error-reference/ (2026-10-05 열람). 원문 메시지는 로그 대조를 위해 보존하고 한국어 설명을 병기했습니다. 에러 코드는 사용 중인 Vue 버전에 따라 달라질 수 있습니다.

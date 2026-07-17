# TypeScript 시작하기

# TypeScript 핸드북

> **원문:** https://www.typescriptlang.org/docs/handbook/intro.html

## 이 핸드북에 대하여

프로그래밍 커뮤니티에 소개된 지 20년이 넘은 지금, JavaScript는 역대 가장 널리 사용되는 크로스 플랫폼 언어 중 하나가 되었습니다. 웹페이지에 간단한 상호작용을 추가하기 위한 작은 스크립팅 언어로 시작한 JavaScript는 모든 규모의 프론트엔드 및 백엔드 애플리케이션에서 선택받는 언어로 성장했습니다. JavaScript로 작성된 프로그램의 크기, 범위, 복잡성이 기하급수적으로 증가한 반면, JavaScript 언어가 서로 다른 코드 단위 간의 관계를 표현하는 능력은 그에 미치지 못했습니다. JavaScript의 다소 독특한 런타임 시맨틱과 결합하여, 이러한 언어와 프로그램 복잡성 간의 불일치는 JavaScript 개발을 대규모로 관리하기 어려운 작업으로 만들었습니다.

프로그래머가 작성하는 가장 흔한 종류의 오류는 타입 오류로 설명할 수 있습니다: 다른 종류의 값이 예상되는 곳에 특정 종류의 값이 사용된 경우입니다. 이는 단순한 오타, 라이브러리의 API 표면에 대한 이해 실패, 런타임 동작에 대한 잘못된 가정, 또는 기타 오류로 인한 것일 수 있습니다. TypeScript의 목표는 JavaScript 프로그램을 위한 정적 타입 검사기가 되는 것입니다 - 다시 말해, 코드가 실행되기 전에 실행되어(정적) 프로그램의 타입이 올바른지 확인하는(타입 검사) 도구입니다.

JavaScript 배경 없이 TypeScript를 처음 접하며 TypeScript를 첫 번째 언어로 배우려는 경우, 먼저 [Microsoft Learn JavaScript 튜토리얼](https://developer.microsoft.com/javascript/)이나 [Mozilla Web Docs의 JavaScript](https://developer.mozilla.org/docs/Web/JavaScript/Guide)에서 문서를 읽어보시기 바랍니다.
다른 언어 경험이 있다면, 핸드북을 읽으면서 JavaScript 문법을 꽤 빠르게 익힐 수 있을 것입니다.

## 이 핸드북의 구성

핸드북은 두 섹션으로 나뉩니다:

- **핸드북**

  TypeScript 핸드북은 일상적인 프로그래머에게 TypeScript를 설명하는 포괄적인 문서가 되도록 의도되었습니다. 왼쪽 네비게이션에서 위에서 아래로 핸드북을 읽을 수 있습니다.

  각 챕터나 페이지가 주어진 개념에 대한 강력한 이해를 제공할 것으로 기대해도 됩니다. TypeScript 핸드북은 완전한 언어 명세가 아니지만, 언어의 모든 기능과 동작에 대한 포괄적인 가이드가 되도록 의도되었습니다.

  연습을 완료한 독자는 다음을 할 수 있어야 합니다:

  - 일반적으로 사용되는 TypeScript 문법과 패턴 읽고 이해하기
  - 중요한 컴파일러 옵션의 효과 설명하기
  - 대부분의 경우에서 타입 시스템 동작을 올바르게 예측하기

  명확성과 간결성을 위해, 핸드북의 주요 내용은 다루는 기능의 모든 엣지 케이스나 세부 사항을 탐구하지 않습니다. 특정 개념에 대한 자세한 내용은 참조 문서에서 찾을 수 있습니다.

- **참조 파일**

  네비게이션에서 핸드북 아래의 참조 섹션은 TypeScript의 특정 부분이 어떻게 작동하는지에 대한 더 풍부한 이해를 제공하도록 구축되었습니다. 위에서 아래로 읽을 수 있지만, 각 섹션은 단일 개념에 대한 더 깊은 설명을 제공하는 것을 목표로 합니다 - 연속성을 목표로 하지 않습니다.

### 다루지 않는 내용

핸드북은 또한 몇 시간 안에 편안하게 읽을 수 있는 간결한 문서가 되도록 의도되었습니다. 간결하게 유지하기 위해 특정 주제는 다루지 않습니다.

구체적으로, 핸드북은 함수, 클래스, 클로저와 같은 핵심 JavaScript 기본 사항을 완전히 소개하지 않습니다. 적절한 경우, 해당 개념을 읽어볼 수 있는 배경 읽기 링크를 포함합니다.

핸드북은 또한 언어 명세를 대체하도록 의도되지 않았습니다. 일부 경우, 엣지 케이스나 동작의 공식적인 설명은 높은 수준의 이해하기 쉬운 설명을 위해 생략됩니다. 대신, TypeScript의 동작의 많은 측면을 더 정확하고 공식적으로 설명하는 별도의 참조 페이지가 있습니다. 참조 페이지는 TypeScript에 익숙하지 않은 독자를 위한 것이 아니므로, 고급 용어를 사용하거나 아직 읽지 않은 주제를 참조할 수 있습니다.

마지막으로, 핸드북은 필요한 경우를 제외하고 TypeScript가 다른 도구와 어떻게 상호작용하는지 다루지 않습니다. webpack, rollup, parcel, react, babel, closure, lerna, rush, bazel, preact, vue, angular, svelte, jquery, yarn, npm으로 TypeScript를 구성하는 방법과 같은 주제는 범위를 벗어납니다 - 이러한 리소스는 웹의 다른 곳에서 찾을 수 있습니다.

## 시작하기

[기본 사항](/docs/handbook/2/basic-types.html)을 시작하기 전에, 다음 소개 페이지 중 하나를 읽어보시기를 권장합니다. 이 소개는 TypeScript와 선호하는 프로그래밍 언어 간의 주요 유사점과 차이점을 강조하고, 해당 언어에 특정한 일반적인 오해를 해소하기 위한 것입니다.

- [새 프로그래머를 위한 TypeScript](/docs/handbook/typescript-from-scratch.html)
- [JavaScript 프로그래머를 위한 TypeScript](/docs/handbook/typescript-in-5-minutes.html)
- [Java/C# 프로그래머를 위한 TypeScript](/docs/handbook/typescript-in-5-minutes-oop.html)
- [함수형 프로그래머를 위한 TypeScript](/docs/handbook/typescript-in-5-minutes-func.html)

그렇지 않으면 [기본 사항](/docs/handbook/2/basic-types.html)으로 바로 이동하세요.

---

# 처음 프로그래밍을 배우는 사람을 위한 TypeScript

> **원문:** https://www.typescriptlang.org/docs/handbook/typescript-from-scratch.html

TypeScript를 첫 번째 언어 중 하나로 선택하신 것을 축하드립니다. 이미 좋은 결정을 내리고 계시네요!

TypeScript가 JavaScript의 "변형" 또는 "파생형"이라는 말을 이미 들어보셨을 것입니다.
TypeScript(TS)와 JavaScript(JS) 사이의 관계는 현대 프로그래밍 언어들 사이에서 매우 독특하므로, 이 관계에 대해 더 알아보면 TypeScript가 JavaScript에 어떤 것을 추가하는지 이해하는 데 도움이 될 것입니다.

## JavaScript란 무엇인가? 간략한 역사

JavaScript(ECMAScript라고도 알려진)는 브라우저를 위한 간단한 스크립팅 언어로 시작했습니다.
발명될 당시에는 웹 페이지에 포함된 짧은 코드 조각에 사용될 것으로 예상되었으며, 수십 줄 이상의 코드를 작성하는 것은 다소 드문 일이었을 것입니다.
이 때문에 초기 웹 브라우저들은 이러한 코드를 상당히 느리게 실행했습니다.
그러나 시간이 지남에 따라 JS는 점점 더 인기를 얻게 되었고, 웹 개발자들은 이를 사용하여 인터랙티브한 경험을 만들기 시작했습니다.

웹 브라우저 개발자들은 이렇게 증가하는 JS 사용에 대응하여 실행 엔진을 최적화하고(동적 컴파일) JS로 할 수 있는 것을 확장(API 추가)했으며, 이는 다시 웹 개발자들이 JS를 더 많이 사용하게 만들었습니다.
현대의 웹사이트에서 브라우저는 수십만 줄에 달하는 코드로 이루어진 애플리케이션을 자주 실행합니다.
이것이 "웹"의 길고 점진적인 성장입니다. 단순한 정적 페이지들의 네트워크로 시작하여 모든 종류의 풍부한 *애플리케이션*을 위한 플랫폼으로 진화한 것입니다.

그 이상으로, JS는 브라우저의 맥락 바깥에서도 사용될 만큼 충분히 인기를 얻었습니다. 예를 들어 node.js를 사용하여 JS 서버를 구현하는 것처럼요.
JS의 "어디서나 실행 가능한" 특성은 크로스 플랫폼 개발에 매력적인 선택이 되게 합니다.
요즘에는 *오직* JavaScript만을 사용하여 전체 스택을 프로그래밍하는 개발자들이 많습니다!

요약하자면, 우리에게는 빠른 사용을 위해 설계되었다가 수백만 줄의 애플리케이션을 작성하는 완전한 도구로 성장한 언어가 있습니다.
모든 언어에는 고유한 *특이점*이 있습니다. 이상하고 놀라운 점들 말이죠. JavaScript의 소박한 시작은 이런 특이점을 많이 만들었습니다. 몇 가지 예를 들면:

- JavaScript의 동등 연산자(`==`)는 피연산자를 *강제 변환*하여 예상치 못한 동작을 유발합니다:

```ts
if ("" == 0) {
  // 참입니다! 하지만 왜??
}
if (1 < x < 3) {
  // *어떤* x 값에 대해서도 참입니다!
}
```

- JavaScript는 존재하지 않는 속성에 접근하는 것도 허용합니다:

```ts
const obj = { width: 10, height: 15 };
// 왜 이것이 NaN일까요? 철자를 쓰기가 어렵네요!
const area = obj.width * obj.heigth;
```

대부분의 프로그래밍 언어는 이런 종류의 오류가 발생할 때 에러를 발생시킵니다. 일부는 컴파일 중에, 즉 코드가 실행되기 전에 그렇게 합니다.
작은 프로그램을 작성할 때 이런 특이점들은 귀찮지만 관리할 수 있습니다. 하지만 수백 또는 수천 줄의 코드로 애플리케이션을 작성할 때 이런 끊임없는 놀라움은 심각한 문제가 됩니다.

## TypeScript: 정적 타입 검사기

앞서 일부 언어는 그런 버그가 있는 프로그램을 아예 실행하지 않을 것이라고 말했습니다.
코드를 실행하지 않고 오류를 감지하는 것을 *정적 검사*라고 합니다.
연산되는 값의 종류를 기반으로 무엇이 오류이고 무엇이 아닌지 결정하는 것을 정적 *타입* 검사라고 합니다.

TypeScript는 실행 전에 프로그램의 오류를 검사하고, *값의 종류*를 기반으로 검사하므로, *정적 타입 검사기*입니다.
예를 들어, 위의 마지막 예제는 `obj`의 *타입* 때문에 오류가 있습니다.
TypeScript가 발견한 오류는 다음과 같습니다:

```ts
const obj = { width: 10, height: 15 };
const area = obj.width * obj.heigth;
// Property 'heigth' does not exist on type '{ width: number; height: number; }'. Did you mean 'height'?
// '{ width: number; height: number; }' 타입에 'heigth' 속성이 존재하지 않습니다. 'height'를 의미했나요?
```

### JavaScript의 타입이 있는 상위 집합

그렇다면 TypeScript는 JavaScript와 어떤 관계가 있을까요?

#### 문법

TypeScript는 JavaScript의 *상위 집합*인 언어입니다. 따라서 JS 문법은 유효한 TS입니다.
문법은 프로그램을 형성하기 위해 텍스트를 작성하는 방식을 말합니다.
예를 들어, 이 코드는 `)`가 빠져 있기 때문에 *문법* 오류가 있습니다:

```ts
let a = (4
// ')' expected.
// ')'가 필요합니다.
```

TypeScript는 문법 때문에 어떤 JavaScript 코드도 오류로 간주하지 않습니다.
이는 작동하는 JavaScript 코드를 가져와서 정확히 어떻게 작성되었는지 걱정하지 않고 TypeScript 파일에 넣을 수 있다는 것을 의미합니다.

#### 타입

그러나 TypeScript는 *타입이 있는* 상위 집합입니다. 이는 다양한 종류의 값이 어떻게 사용될 수 있는지에 대한 규칙을 추가한다는 의미입니다.
앞서 `obj.heigth`에 대한 오류는 *문법* 오류가 아니었습니다. 이것은 어떤 종류의 값(*타입*)을 잘못된 방식으로 사용하는 오류입니다.

또 다른 예로, 이것은 브라우저에서 실행할 수 있는 JavaScript 코드이며, 값을 로그로 *출력할 것입니다*:

```ts
console.log(4 / []);
```

이 문법적으로 유효한 프로그램은 `Infinity`를 로그로 출력합니다.
하지만 TypeScript는 숫자를 배열로 나누는 것을 무의미한 연산으로 간주하고 오류를 발생시킵니다:

```ts
console.log(4 / []);
// The right-hand side of an arithmetic operation must be of type 'any', 'number', 'bigint' or an enum type.
// 산술 연산의 오른쪽은 'any', 'number', 'bigint' 또는 열거형 타입이어야 합니다.
```

숫자를 배열로 나누려는 의도가 정말로 있었을 수도 있습니다. 아마도 무슨 일이 일어나는지 보기 위해서요. 하지만 대부분의 경우 이것은 프로그래밍 실수입니다.
TypeScript의 타입 검사기는 올바른 프로그램은 통과시키면서도 가능한 한 많은 일반적인 오류를 잡도록 설계되었습니다.
(나중에 TypeScript가 코드를 얼마나 엄격하게 검사하는지 구성하는 데 사용할 수 있는 설정에 대해 배울 것입니다.)

JavaScript 파일에서 TypeScript 파일로 일부 코드를 옮기면, 코드가 어떻게 작성되었는지에 따라 *타입 오류*를 볼 수 있습니다.
이것들은 코드의 정당한 문제일 수도 있고, TypeScript가 지나치게 보수적인 것일 수도 있습니다.
이 가이드 전체에서 우리는 그러한 오류를 제거하기 위해 다양한 TypeScript 문법을 추가하는 방법을 보여드릴 것입니다.

#### 런타임 동작

TypeScript는 또한 JavaScript의 *런타임 동작*을 보존하는 프로그래밍 언어입니다.
예를 들어, JavaScript에서 0으로 나누면 런타임 예외를 발생시키는 대신 `Infinity`를 생성합니다.
원칙적으로 TypeScript는 **절대로** JavaScript 코드의 런타임 동작을 변경하지 않습니다.

이것은 JavaScript에서 TypeScript로 코드를 옮기면, TypeScript가 코드에 타입 오류가 있다고 생각하더라도 동일한 방식으로 실행되는 것이 **보장된다**는 것을 의미합니다.

JavaScript와 동일한 런타임 동작을 유지하는 것은 TypeScript의 기본적인 약속입니다. 이것은 프로그램이 작동을 멈추게 할 수 있는 미묘한 차이점에 대해 걱정하지 않고 두 언어 사이를 쉽게 전환할 수 있다는 것을 의미하기 때문입니다.

#### 지워지는 타입

대략적으로 말해서, TypeScript의 컴파일러가 코드 검사를 완료하면, 결과적인 "컴파일된" 코드를 생성하기 위해 타입을 *지웁니다*.
이것은 코드가 컴파일되면 결과적인 일반 JS 코드에는 타입 정보가 없다는 것을 의미합니다.

이것은 또한 TypeScript가 추론한 타입을 기반으로 프로그램의 *동작*을 절대 변경하지 않는다는 것을 의미합니다.
결론은 컴파일 중에 타입 오류를 볼 수 있지만, 타입 시스템 자체는 프로그램이 실행될 때 작동하는 방식에 영향을 미치지 않는다는 것입니다.

마지막으로, TypeScript는 추가적인 런타임 라이브러리를 제공하지 않습니다.
프로그램은 JavaScript 프로그램과 동일한 표준 라이브러리(또는 외부 라이브러리)를 사용하므로, 배울 추가적인 TypeScript 전용 프레임워크가 없습니다.

## JavaScript와 TypeScript 배우기

우리는 "JavaScript를 배워야 할까요, TypeScript를 배워야 할까요?"라는 질문을 자주 봅니다.

대답은 JavaScript를 배우지 않고는 TypeScript를 배울 수 없다는 것입니다!
TypeScript는 JavaScript와 문법과 런타임 동작을 공유하므로, JavaScript에 대해 배우는 모든 것은 동시에 TypeScript를 배우는 데 도움이 됩니다.

프로그래머들이 JavaScript를 배울 수 있는 많은 리소스가 있습니다. TypeScript를 작성한다면 이러한 리소스를 무시하지 *마세요*.
예를 들어, `typescript`보다 약 20배 더 많은 `javascript` 태그가 달린 StackOverflow 질문이 있지만, *모든* `javascript` 질문은 TypeScript에도 적용됩니다.

"TypeScript에서 리스트를 정렬하는 방법"과 같은 것을 검색하고 있다면, 기억하세요: **TypeScript는 컴파일 타임 타입 검사기가 있는 JavaScript의 런타임입니다**.
TypeScript에서 리스트를 정렬하는 방법은 JavaScript에서 하는 방법과 같습니다.
TypeScript를 직접 사용하는 리소스를 찾으면 그것도 좋지만, 런타임 작업을 수행하는 방법에 대한 일상적인 질문에 TypeScript 전용 답변이 필요하다고 생각하여 자신을 제한하지 마세요.

## 다음 단계

이것은 일상적인 TypeScript에서 사용되는 문법과 도구에 대한 간략한 개요였습니다. 여기서부터 다음을 할 수 있습니다:

- JavaScript 기본 사항을 배우세요. 다음을 권장합니다:
  - [Microsoft의 JavaScript 리소스](https://developer.microsoft.com/javascript/) 또는
  - [Mozilla Web Docs의 JavaScript 가이드](https://developer.mozilla.org/docs/Web/JavaScript/Guide)

- [JavaScript 프로그래머를 위한 TypeScript](/docs/handbook/typescript-in-5-minutes.html)로 계속하세요

- 전체 핸드북을 [처음부터 끝까지](/docs/handbook/intro.html) 읽으세요

- [플레이그라운드 예제](/play#show-examples)를 탐색하세요

---

# JavaScript 프로그래머를 위한 TypeScript

> **원문:** https://www.typescriptlang.org/docs/handbook/typescript-in-5-minutes.html

TypeScript는 JavaScript와 특별한 관계에 있습니다. TypeScript는 JavaScript의 모든 기능을 제공하며, 그 위에 추가적인 계층인 TypeScript의 타입 시스템을 더합니다.

예를 들어, JavaScript는 `string`과 `number` 같은 언어 원시 타입을 제공하지만, 이들을 일관되게 할당했는지 검사하지 않습니다. TypeScript는 검사합니다.

이것은 기존에 작동하는 JavaScript 코드가 TypeScript 코드이기도 하다는 것을 의미합니다. TypeScript의 주요 이점은 코드에서 예상치 못한 동작을 강조하여 버그 가능성을 낮출 수 있다는 것입니다.

이 튜토리얼은 TypeScript의 타입 시스템에 초점을 맞춘 TypeScript의 간략한 개요를 제공합니다.

## 추론에 의한 타입

TypeScript는 JavaScript 언어를 알고 있으며 많은 경우에 타입을 자동으로 생성해줍니다.
예를 들어, 변수를 생성하고 특정 값을 할당할 때, TypeScript는 그 값을 타입으로 사용합니다.

```ts
let helloWorld = "Hello World";
//  let helloWorld: string
```

JavaScript가 어떻게 작동하는지 이해함으로써, TypeScript는 JavaScript 코드를 받아들이면서도 타입을 가지는 타입 시스템을 구축할 수 있습니다. 이것은 코드에서 타입을 명시적으로 만들기 위해 추가적인 문자를 추가할 필요 없이 타입 시스템을 제공합니다. 이것이 TypeScript가 위의 예제에서 `helloWorld`가 `string`이라는 것을 아는 방법입니다.

Visual Studio Code에서 JavaScript를 작성하면서 편집기 자동 완성을 경험해보셨을 수 있습니다. Visual Studio Code는 JavaScript 작업을 더 쉽게 만들기 위해 내부적으로 TypeScript를 사용합니다.

## 타입 정의하기

JavaScript에서는 다양한 디자인 패턴을 사용할 수 있습니다. 그러나 일부 디자인 패턴은 타입이 자동으로 추론되기 어렵게 만듭니다(예: 동적 프로그래밍을 사용하는 패턴). 이러한 경우를 다루기 위해 TypeScript는 JavaScript 언어의 확장을 지원하여 TypeScript에게 타입이 무엇이어야 하는지 알려줄 수 있는 장소를 제공합니다.

예를 들어, `name: string`과 `id: number`를 포함하는 추론된 타입을 가진 객체를 생성하려면 다음과 같이 작성할 수 있습니다:

```ts
const user = {
  name: "Hayes",
  id: 0,
};
```

`interface` 선언을 사용하여 이 객체의 모양을 명시적으로 설명할 수 있습니다:

```ts
interface User {
  name: string;
  id: number;
}
```

그런 다음 변수 선언 후에 `: TypeName` 같은 문법을 사용하여 JavaScript 객체가 새로운 `interface`의 모양을 따른다고 선언할 수 있습니다:

```ts
const user: User = {
  name: "Hayes",
  id: 0,
};
```

제공한 인터페이스와 일치하지 않는 객체를 제공하면 TypeScript가 경고합니다:

```ts
interface User {
  name: string;
  id: number;
}

const user: User = {
  username: "Hayes",
  // Object literal may only specify known properties, and 'username' does not exist in type 'User'.
  // 객체 리터럴은 알려진 속성만 지정할 수 있으며, 'username'은 'User' 타입에 존재하지 않습니다.
  id: 0,
};
```

JavaScript가 클래스와 객체 지향 프로그래밍을 지원하므로 TypeScript도 지원합니다. 클래스와 함께 인터페이스 선언을 사용할 수 있습니다:

```ts
interface User {
  name: string;
  id: number;
}

class UserAccount {
  name: string;
  id: number;

  constructor(name: string, id: number) {
    this.name = name;
    this.id = id;
  }
}

const user: User = new UserAccount("Murphy", 1);
```

인터페이스를 사용하여 함수의 매개변수와 반환 값에 어노테이션을 달 수 있습니다:

```ts
function deleteUser(user: User) {
  // ...
}

function getAdminUser(): User {
  //...
}
```

JavaScript에서 이미 사용할 수 있는 작은 원시 타입 세트가 있습니다: `boolean`, `bigint`, `null`, `number`, `string`, `symbol`, `undefined`. 이것들은 인터페이스에서 사용할 수 있습니다. TypeScript는 이 목록을 몇 가지로 확장합니다. 예를 들어 `any`(무엇이든 허용), `unknown`(이 타입을 사용하는 사람이 타입이 무엇인지 선언하도록 보장), `never`(이 타입이 발생할 수 없음), `void`(`undefined`를 반환하거나 반환 값이 없는 함수) 등이 있습니다.

타입을 구축하는 두 가지 문법이 있다는 것을 알게 될 것입니다: [인터페이스와 타입](/play/?e=83#example/types-vs-interfaces). `interface`를 선호해야 합니다. 특정 기능이 필요할 때 `type`을 사용하세요.

## 타입 조합하기

TypeScript를 사용하면 간단한 타입을 결합하여 복잡한 타입을 만들 수 있습니다. 이를 수행하는 두 가지 인기 있는 방법이 있습니다: 유니온과 제네릭.

### 유니온

유니온을 사용하면 타입이 여러 타입 중 하나일 수 있다고 선언할 수 있습니다. 예를 들어, `boolean` 타입을 `true` 또는 `false`로 설명할 수 있습니다:

```ts
type MyBool = true | false;
```

*참고:* 위의 `MyBool` 위에 마우스를 올리면 `boolean`으로 분류된다는 것을 알 수 있습니다. 이것은 구조적 타입 시스템의 속성입니다. 아래에서 더 자세히 설명합니다.

유니온 타입의 일반적인 사용 사례는 값이 될 수 있는 `string` 또는 `number` [리터럴](/docs/handbook/2/everyday-types.html#literal-types) 세트를 설명하는 것입니다:

```ts
type WindowStates = "open" | "closed" | "minimized";
type LockStates = "locked" | "unlocked";
type PositiveOddNumbersUnderTen = 1 | 3 | 5 | 7 | 9;
```

유니온은 다양한 타입을 처리하는 방법도 제공합니다. 예를 들어, `array` 또는 `string`을 받는 함수가 있을 수 있습니다:

```ts
function getLength(obj: string | string[]) {
  return obj.length;
}
```

변수의 타입을 알아내려면 `typeof`를 사용하세요:

| 타입 | 조건식 |
|------|--------|
| string | `typeof s === "string"` |
| number | `typeof n === "number"` |
| boolean | `typeof b === "boolean"` |
| undefined | `typeof undefined === "undefined"` |
| function | `typeof f === "function"` |
| array | `Array.isArray(a)` |

예를 들어, 문자열 또는 배열이 전달되었는지에 따라 다른 값을 반환하는 함수를 만들 수 있습니다:

```ts
function wrapInArray(obj: string | string[]) {
  if (typeof obj === "string") {
    return [obj];
    //      ^? (parameter) obj: string
  }
  return obj;
}
```

### 제네릭

제네릭은 타입에 변수를 제공합니다. 일반적인 예는 배열입니다. 제네릭이 없는 배열은 무엇이든 포함할 수 있습니다. 제네릭이 있는 배열은 배열이 포함하는 값을 설명할 수 있습니다.

```ts
type StringArray = Array<string>;
type NumberArray = Array<number>;
type ObjectWithNameArray = Array<{ name: string }>;
```

제네릭을 사용하는 자신만의 타입을 선언할 수 있습니다:

```ts
interface Backpack<Type> {
  add: (obj: Type) => void;
  get: () => Type;
}

// 이 줄은 TypeScript에게 `backpack`이라는 상수가 있고,
// 어디서 왔는지 걱정하지 말라고 알려주는 단축키입니다.
declare const backpack: Backpack<string>;

// 위에서 Backpack의 변수 부분으로 선언했기 때문에 object는 string입니다.
const object = backpack.get();

// backpack 변수가 string이므로 add 함수에 number를 전달할 수 없습니다.
backpack.add(23);
// Argument of type 'number' is not assignable to parameter of type 'string'.
// 'number' 타입의 인수는 'string' 타입의 매개변수에 할당할 수 없습니다.
```

## 구조적 타입 시스템

TypeScript의 핵심 원칙 중 하나는 타입 검사가 값이 가진 *모양*에 초점을 맞춘다는 것입니다. 이것은 때때로 "덕 타이핑" 또는 "구조적 타이핑"이라고 불립니다.

구조적 타입 시스템에서 두 객체가 같은 모양을 가지면 같은 타입으로 간주됩니다.

```ts
interface Point {
  x: number;
  y: number;
}

function logPoint(p: Point) {
  console.log(`${p.x}, ${p.y}`);
}

// "12, 26"을 로그로 출력
const point = { x: 12, y: 26 };
logPoint(point);
```

`point` 변수는 `Point` 타입으로 선언된 적이 없습니다. 그러나 TypeScript는 타입 검사에서 `point`의 모양을 `Point`의 모양과 비교합니다. 같은 모양을 가지므로 코드가 통과합니다.

모양 일치는 객체 필드의 하위 집합만 일치하면 됩니다.

```ts
const point3 = { x: 12, y: 26, z: 89 };
logPoint(point3); // "12, 26" 로그 출력

const rect = { x: 33, y: 3, width: 30, height: 80 };
logPoint(rect); // "33, 3" 로그 출력

const color = { hex: "#187ABF" };
logPoint(color);
// Argument of type '{ hex: string; }' is not assignable to parameter of type 'Point'.
//   Type '{ hex: string; }' is missing the following properties from type 'Point': x, y
// '{ hex: string; }' 타입의 인수는 'Point' 타입의 매개변수에 할당할 수 없습니다.
//   '{ hex: string; }' 타입에는 'Point' 타입의 다음 속성이 없습니다: x, y
```

클래스와 객체가 모양을 따르는 방식에는 차이가 없습니다:

```ts
class VirtualPoint {
  x: number;
  y: number;

  constructor(x: number, y: number) {
    this.x = x;
    this.y = y;
  }
}

const newVPoint = new VirtualPoint(13, 56);
logPoint(newVPoint); // "13, 56" 로그 출력
```

객체 또는 클래스가 필요한 모든 속성이 있으면, TypeScript는 구현 세부 사항에 관계없이 일치한다고 말합니다.

## 다음 단계

이것은 일상적인 TypeScript에서 사용되는 문법과 도구에 대한 간략한 개요였습니다. 여기서부터 다음을 할 수 있습니다:

- 전체 핸드북을 [처음부터 끝까지](/docs/handbook/intro.html) 읽으세요

- [플레이그라운드 예제](/play#show-examples)를 탐색하세요

---

# Java/C# 프로그래머를 위한 TypeScript

> **원문:** https://www.typescriptlang.org/docs/handbook/typescript-in-5-minutes-oop.html

TypeScript는 C#이나 Java 같은 정적 타이핑을 가진 다른 언어에 익숙한 프로그래머들에게 인기 있는 선택입니다.

TypeScript의 타입 시스템은 더 나은 코드 완성, 더 빠른 오류 감지, 프로그램 부분들 간의 더 명확한 통신 같은 많은 동일한 이점을 제공합니다.
이러한 개발자들에게 TypeScript가 많은 익숙한 기능을 제공하지만, JavaScript(그리고 따라서 TypeScript)가 전통적인 OOP 언어와 어떻게 다른지 살펴볼 가치가 있습니다.
이러한 차이점을 이해하면 더 나은 JavaScript 코드를 작성하는 데 도움이 되고, C#/Java에서 바로 TypeScript로 넘어온 프로그래머들이 빠지기 쉬운 일반적인 함정을 피할 수 있습니다.

## JavaScript 함께 배우기

이미 JavaScript에 익숙하지만 주로 Java나 C# 프로그래머라면, 이 소개 페이지가 여러분이 빠지기 쉬운 일반적인 오해와 함정을 설명하는 데 도움이 될 수 있습니다.
TypeScript가 타입을 모델링하는 방식 중 일부는 Java나 C#과 상당히 다르며, TypeScript를 배울 때 이것을 염두에 두는 것이 중요합니다.

Java나 C# 프로그래머이면서 일반적으로 JavaScript를 처음 접한다면, JavaScript의 런타임 동작을 이해하기 위해 먼저 타입 *없이* 약간의 JavaScript를 배우는 것을 권장합니다.
TypeScript는 코드가 *실행되는* 방식을 변경하지 않으므로, 실제로 무언가를 하는 코드를 작성하려면 여전히 JavaScript가 어떻게 작동하는지 배워야 합니다!

TypeScript가 JavaScript와 동일한 *런타임*을 사용한다는 것을 기억하는 것이 중요합니다. 따라서 특정 런타임 동작(문자열을 숫자로 변환, 알림 표시, 디스크에 파일 쓰기 등)을 수행하는 방법에 대한 모든 리소스는 TypeScript 프로그램에도 동일하게 적용됩니다.
TypeScript 전용 리소스로 자신을 제한하지 마세요!

## 클래스 다시 생각하기

C#과 Java는 우리가 *필수 OOP* 언어라고 부를 수 있는 것입니다.
이러한 언어에서 *클래스*는 코드 구성의 기본 단위이자 런타임에 모든 데이터 *와* 동작의 기본 컨테이너입니다.
모든 기능과 데이터를 클래스에 강제로 담는 것은 일부 문제에 대해 좋은 도메인 모델이 될 수 있지만, 모든 도메인이 이 방식으로 표현될 *필요*는 없습니다.

### 자유 함수와 데이터

JavaScript에서 함수는 어디에나 존재할 수 있고, 데이터는 미리 정의된 `class`나 `struct` 안에 있지 않고도 자유롭게 전달될 수 있습니다.
이러한 유연성은 매우 강력합니다.
암시적인 OOP 계층 구조 없이 데이터를 다루는 "자유" 함수(클래스와 연관되지 않은 함수)는 JavaScript에서 프로그램을 작성하는 선호되는 모델인 경향이 있습니다.

### 정적 클래스

또한, C#과 Java의 싱글톤이나 정적 클래스 같은 특정 구조는 TypeScript에서 불필요합니다.

## TypeScript에서의 OOP

그렇다고 해서, 원한다면 여전히 클래스를 사용할 수 있습니다!
일부 문제는 전통적인 OOP 계층 구조로 해결하기에 적합하며, TypeScript의 JavaScript 클래스 지원은 이러한 모델을 더욱 강력하게 만들어줍니다.
TypeScript는 인터페이스 구현, 상속, 정적 메서드 같은 많은 일반적인 패턴을 지원합니다.

클래스에 대해서는 이 가이드의 뒷부분에서 다룰 것입니다.

## 타입 다시 생각하기

TypeScript의 *타입* 이해는 실제로 C#이나 Java의 것과 상당히 다릅니다.
몇 가지 차이점을 살펴보겠습니다.

### 명목적 구체화 타입 시스템

C#이나 Java에서 주어진 값이나 객체는 하나의 정확한 타입을 가집니다 - `null`, 원시 타입, 또는 알려진 클래스 타입 중 하나입니다.
런타임에 정확한 타입을 조회하기 위해 `value.GetType()` 또는 `value.getClass()` 같은 메서드를 호출할 수 있습니다.
이 타입의 정의는 어딘가의 클래스에 어떤 이름으로 존재할 것이며, 명시적인 상속 관계나 공통으로 구현된 인터페이스가 없으면 유사한 모양의 두 클래스를 서로 대신 사용할 수 없습니다.

이러한 측면은 *구체화된, 명목적* 타입 시스템을 설명합니다.
코드에 작성한 타입이 런타임에 존재하며, 타입은 구조가 아니라 선언을 통해 관련됩니다.

### 집합으로서의 타입

C#이나 Java에서는 런타임 타입과 컴파일 타임 선언 사이의 일대일 대응을 생각하는 것이 의미 있습니다.

TypeScript에서는 타입을 공통점을 공유하는 *값들의 집합*으로 생각하는 것이 좋습니다.
타입은 단지 집합이기 때문에, 특정 값은 동시에 *많은* 집합에 속할 수 있습니다.

타입을 집합으로 생각하기 시작하면 특정 연산이 매우 자연스러워집니다.
예를 들어, C#에서는 `string` *또는* `int`인 값을 전달하는 것이 어색합니다. 이런 종류의 값을 나타내는 단일 타입이 없기 때문입니다.

TypeScript에서는 모든 타입이 단지 집합이라는 것을 깨닫는 순간 이것이 매우 자연스러워집니다.
`string` 집합 또는 `number` 집합에 속하는 값을 어떻게 설명할까요?
그것은 단순히 해당 집합들의 *유니온*에 속합니다: `string | number`.

TypeScript는 집합론적 방식으로 타입을 다루는 여러 메커니즘을 제공하며, 타입을 집합으로 생각하면 더 직관적으로 느껴질 것입니다.

### 지워지는 구조적 타입

TypeScript에서 객체는 하나의 정확한 타입이 *아닙니다*.
예를 들어, 인터페이스를 만족하는 객체를 구성하면, 둘 사이에 선언적 관계가 없었더라도 해당 인터페이스가 예상되는 곳에서 그 객체를 사용할 수 있습니다.

```ts
interface Pointlike {
  x: number;
  y: number;
}
interface Named {
  name: string;
}

function logPoint(point: Pointlike) {
  console.log("x = " + point.x + ", y = " + point.y);
}

function logName(x: Named) {
  console.log("Hello, " + x.name);
}

const obj = {
  x: 0,
  y: 0,
  name: "Origin",
};

logPoint(obj);
logName(obj);
```

TypeScript의 타입 시스템은 명목적이 아니라 *구조적*입니다: `obj`가 둘 다 숫자인 `x`와 `y` 속성이 있기 때문에 `Pointlike`로 사용할 수 있습니다.
타입 간의 관계는 특정 관계로 선언되었는지가 아니라 포함하는 속성에 의해 결정됩니다.

TypeScript의 타입 시스템은 또한 *구체화되지 않습니다*: 런타임에 `obj`가 `Pointlike`라고 알려주는 것은 아무것도 없습니다.
사실, `Pointlike` 타입은 런타임에 *어떤 형태로도* 존재하지 않습니다.

*집합으로서의 타입* 아이디어로 돌아가면, `obj`를 `Pointlike` 값 집합과 `Named` 값 집합 둘 다의 구성원으로 생각할 수 있습니다.

### 구조적 타이핑의 결과

OOP 프로그래머들은 종종 구조적 타이핑의 두 가지 특정 측면에 놀라곤 합니다.

#### 빈 타입

첫 번째는 *빈 타입*이 기대를 거스르는 것처럼 보인다는 것입니다:

```ts
class Empty {}

function fn(arg: Empty) {
  // 무언가를 하나?
}

// 오류 없음, 하지만 이것은 'Empty'가 아니지 않나?
fn({ k: 10 });
```

TypeScript는 제공된 인수가 유효한 `Empty`인지 확인하여 여기서 `fn` 호출이 유효한지 결정합니다.
이것은 `{ k: 10 }`과 `class Empty { }`의 *구조*를 검사하여 수행합니다.
`Empty`는 속성이 없기 때문에 `{ k: 10 }`이 `Empty`가 가진 *모든* 속성이 있음을 알 수 있습니다.
따라서 이것은 유효한 호출입니다!

이것은 놀랍게 보일 수 있지만, 궁극적으로 명목적 OOP 언어에서 강제하는 것과 매우 유사한 관계입니다.
하위 클래스는 기본 클래스의 속성을 *제거*할 수 없습니다. 그렇게 하면 파생 클래스와 기본 클래스 사이의 자연스러운 하위 타입 관계가 깨지기 때문입니다.
구조적 타입 시스템은 호환 가능한 타입의 속성을 가지는 것으로 하위 타입을 설명하여 이 관계를 암묵적으로 식별합니다.

#### 동일한 타입

또 다른 빈번한 놀라움의 원인은 동일한 타입에서 옵니다:

```ts
class Car {
  drive() {
    // 액셀을 밟는다
  }
}
class Golfer {
  drive() {
    // 공을 멀리 친다
  }
}
// 오류 없음?
let w: Car = new Golfer();
```

다시 말하지만, 이러한 클래스들의 *구조*가 같기 때문에 이것은 오류가 아닙니다.
이것이 잠재적인 혼란의 원인처럼 보일 수 있지만, 실제로 관련되어서는 안 되는 동일한 클래스는 일반적이지 않습니다.

클래스가 서로 어떻게 관련되는지에 대해서는 클래스 챕터에서 더 배울 것입니다.

### 리플렉션

OOP 프로그래머들은 제네릭 타입을 포함하여 모든 값의 타입을 조회할 수 있는 것에 익숙합니다:

```csharp
// C#
static void LogType<T>() {
    Console.WriteLine(typeof(T).Name);
}
```

TypeScript의 타입 시스템이 완전히 지워지기 때문에, 예를 들어 제네릭 타입 매개변수의 인스턴스화에 대한 정보는 런타임에 사용할 수 없습니다.

JavaScript에는 `typeof`와 `instanceof` 같은 일부 제한된 원시 기능이 있지만, 이러한 연산자는 여전히 타입이 지워진 출력 코드에 존재하는 값에 대해 작동한다는 것을 기억하세요.
예를 들어, `typeof (new Car())`는 `Car`나 `"Car"`가 아니라 `"object"`가 됩니다.

## 다음 단계

이것은 일상적인 TypeScript에서 사용되는 문법과 도구에 대한 간략한 개요였습니다. 여기서부터 다음을 할 수 있습니다:

- 전체 핸드북을 [처음부터 끝까지](/docs/handbook/intro.html) 읽으세요

- [플레이그라운드 예제](/play#show-examples)를 탐색하세요

---

# 함수형 프로그래머를 위한 TypeScript

> **원문:** https://www.typescriptlang.org/docs/handbook/typescript-in-5-minutes-func.html

TypeScript는 전통적인 객체 지향 타입을 JavaScript에 가져와서 Microsoft의 프로그래머들이 전통적인 객체 지향 프로그램을 웹에 가져올 수 있도록 하려는 시도로 시작했습니다. TypeScript가 발전하면서, 타입 시스템은 네이티브 JavaScripter들이 작성하는 코드를 모델링하도록 진화했습니다. 결과적인 시스템은 강력하고, 흥미롭고, 복잡합니다.

이 소개는 TypeScript를 배우고자 하는 Haskell이나 ML 프로그래머를 위해 설계되었습니다. TypeScript의 타입 시스템이 Haskell의 타입 시스템과 어떻게 다른지 설명합니다. 또한 JavaScript 코드 모델링에서 발생하는 TypeScript 타입 시스템의 고유한 특징도 설명합니다.

이 소개는 객체 지향 프로그래밍을 다루지 않습니다. 실제로 TypeScript의 객체 지향 프로그램은 OO 기능을 가진 다른 인기 있는 언어들과 유사합니다.

## 전제 조건

이 소개에서는 다음을 알고 있다고 가정합니다:

- JavaScript의 좋은 부분으로 프로그래밍하는 방법.

- C 계열 언어의 타입 문법.

JavaScript의 좋은 부분을 배워야 한다면, [JavaScript: The Good Parts](https://shop.oreilly.com/product/9780596517748.do)를 읽으세요.
많은 가변성과 그 외에는 별로 없는 값에 의한 호출, 어휘적 스코핑 언어로 프로그램을 작성하는 방법을 알고 있다면 책을 건너뛸 수 있을 것입니다.
[R4RS Scheme](https://people.csail.mit.edu/jaffer/r4rs.pdf)이 좋은 예입니다.

[The C++ Programming Language](http://www.stroustrup.com/4th.html)는 C 스타일 타입 문법을 배우기 좋은 곳입니다. C++과 달리, TypeScript는 접미사 타입을 사용합니다. 예: `string x` 대신 `x: string`.

## Haskell에 없는 개념

### 내장 타입

JavaScript는 8가지 내장 타입을 정의합니다:

| 타입 | 설명 |
|------|------|
| `Number` | 배정밀도 IEEE 754 부동소수점. |
| `String` | 불변 UTF-16 문자열. |
| `BigInt` | 임의 정밀도 형식의 정수. |
| `Boolean` | `true`와 `false`. |
| `Symbol` | 주로 키로 사용되는 고유한 값. |
| `Null` | 유닛 타입과 동등. |
| `Undefined` | 역시 유닛 타입과 동등. |
| `Object` | 레코드와 유사. |

[더 자세한 내용은 MDN 페이지를 참조하세요](https://developer.mozilla.org/docs/Web/JavaScript/Data_structures).

TypeScript는 내장 타입에 대응하는 원시 타입이 있습니다:

- `number`
- `string`
- `bigint`
- `boolean`
- `symbol`
- `null`
- `undefined`
- `object`

#### 다른 중요한 TypeScript 타입

| 타입 | 설명 |
|------|------|
| `unknown` | 최상위 타입. |
| `never` | 최하위 타입. |
| 객체 리터럴 | 예: `{ property: Type }` |
| `void` | 문서화된 반환 값이 없는 함수용 |
| `T[]` | 가변 배열, `Array<T>`로도 작성 |
| `[T, T]` | 튜플, 고정 길이지만 가변 |
| `(t: T) => U` | 함수 |

참고:

- 함수 문법은 매개변수 이름을 포함합니다. 이것은 익숙해지기 꽤 어렵습니다!

```ts
let fst: (a: any, b: any) => any = (a, b) => a;

// 또는 더 정확하게:
let fst: <T, U>(a: T, b: U) => T = (a, b) => a;
```

- 객체 리터럴 타입 문법은 객체 리터럴 값 문법을 거의 그대로 반영합니다:

```ts
let o: { n: number; xs: object[] } = { n: 1, xs: [] };
```

- `[T, T]`는 `T[]`의 하위 타입입니다. 이것은 튜플이 리스트와 관련이 없는 Haskell과 다릅니다.

#### 박싱된 타입

JavaScript에는 프로그래머들이 해당 타입과 연관시키는 메서드를 포함하는 원시 타입의 박싱된 등가물이 있습니다. TypeScript는 이것을 반영하여, 예를 들어 원시 타입 `number`와 박싱된 타입 `Number` 사이의 차이를 둡니다. 박싱된 타입은 메서드가 원시 타입을 반환하기 때문에 거의 필요하지 않습니다.

```ts
(1).toExponential();
// 다음과 동등
Number.prototype.toExponential.call(1);
```

숫자 리터럴에서 메서드를 호출하려면 파서를 돕기 위해 괄호 안에 넣어야 한다는 점에 주의하세요.

### 점진적 타이핑

TypeScript는 표현식의 타입이 무엇이어야 하는지 알 수 없을 때마다 `any` 타입을 사용합니다. `Dynamic`과 비교하여 `any`를 타입이라고 부르는 것은 과장입니다. 그것은 나타나는 곳마다 타입 검사기를 끄는 것입니다. 예를 들어, 값을 어떤 방식으로도 표시하지 않고 `any[]`에 어떤 값이든 푸시할 수 있습니다:

```ts
// tsconfig.json에서 "noImplicitAny": false인 경우, anys: any[]
const anys = [];
anys.push(1);
anys.push("oh no");
anys.push({ anything: "goes" });
```

그리고 `any` 타입의 표현식을 어디서든 사용할 수 있습니다:

```ts
anys.map(anys[1]); // oh no, "oh no"는 함수가 아닙니다
```

`any`는 전염성이 있습니다 - `any` 타입의 표현식으로 변수를 초기화하면, 변수도 `any` 타입을 가집니다.

```ts
let sepsis = anys[0] + anys[1]; // 이것은 무엇이든 될 수 있습니다
```

TypeScript가 `any`를 생성할 때 오류를 얻으려면, `tsconfig.json`에서 `"noImplicitAny": true` 또는 `"strict": true`를 사용하세요.

### 구조적 타이핑

구조적 타이핑은 대부분의 함수형 프로그래머에게 익숙한 개념이지만, Haskell과 대부분의 ML은 구조적으로 타이핑되지 않습니다. 기본 형태는 꽤 간단합니다:

```ts
// @strict: false
let o = { x: "hi", extra: 1 }; // ok
let o2: { x: string } = o; // ok
```

여기서 객체 리터럴 `{ x: "hi", extra: 1 }`은 일치하는 리터럴 타입 `{ x: string, extra: number }`를 가집니다. 그 타입은 필요한 모든 속성이 있고 해당 속성들이 할당 가능한 타입이 있기 때문에 `{ x: string }`에 할당 가능합니다. 추가 속성은 할당을 방해하지 않고, 단지 `{ x: string }`의 하위 타입으로 만듭니다.

명명된 타입은 단지 타입에 이름을 부여할 뿐입니다; 할당 가능성 측면에서 아래의 타입 별칭 `One`과 인터페이스 타입 `Two` 사이에는 차이가 없습니다. 둘 다 `p: string` 속성을 가집니다. (타입 별칭은 재귀적 정의와 타입 매개변수와 관련하여 인터페이스와 다르게 동작하지만.)

```ts
type One = { p: string };
interface Two {
  p: string;
}
class Three {
  p = "Hello";
}

let x: One = { p: "hi" };
let two: Two = x;
two = new Three();
```

### 유니온

TypeScript에서 유니온 타입은 태그가 없습니다. 다시 말해, Haskell의 `data`처럼 판별 유니온이 아닙니다. 그러나 종종 내장 태그나 다른 속성을 사용하여 유니온의 타입을 구별할 수 있습니다.

```ts
function start(
  arg: string | string[] | (() => string) | { s: string }
): string {
  // 이것은 JavaScript에서 매우 일반적입니다
  if (typeof arg === "string") {
    return commonCase(arg);
  } else if (Array.isArray(arg)) {
    return arg.map(commonCase).join(",");
  } else if (typeof arg === "function") {
    return commonCase(arg());
  } else {
    return commonCase(arg.s);
  }

  function commonCase(s: string): string {
    // 마지막으로, 문자열을 다른 문자열로 변환
    return s;
  }
}
```

`string`, `Array`, `Function`은 내장 타입 조건자가 있어, `else` 분기에는 객체 타입이 편리하게 남습니다. 그러나 런타임에 구별하기 어려운 유니온을 생성하는 것이 가능합니다. 새 코드의 경우, 판별 유니온만 만드는 것이 가장 좋습니다.

다음 타입들은 내장 조건자가 있습니다:

| 타입 | 조건자 |
|------|--------|
| string | `typeof s === "string"` |
| number | `typeof n === "number"` |
| bigint | `typeof m === "bigint"` |
| boolean | `typeof b === "boolean"` |
| symbol | `typeof g === "symbol"` |
| undefined | `typeof undefined === "undefined"` |
| function | `typeof f === "function"` |
| array | `Array.isArray(a)` |
| object | `typeof o === "object"` |

함수와 배열은 런타임에 객체이지만, 자체 조건자가 있다는 점에 주의하세요.

#### 교차

유니온 외에도, TypeScript는 교차도 있습니다:

```ts
type Combined = { a: number } & { b: string };
type Conflicting = { a: number } & { a: string };
```

`Combined`는 마치 하나의 객체 리터럴 타입으로 작성된 것처럼 `a`와 `b` 두 속성을 가집니다. 교차와 유니온은 충돌의 경우 재귀적이므로, `Conflicting.a: number & string`입니다.

### 유닛 타입

유닛 타입은 정확히 하나의 원시 값을 포함하는 원시 타입의 하위 타입입니다. 예를 들어, 문자열 `"foo"`는 `"foo"` 타입을 가집니다. JavaScript에는 내장 열거형이 없기 때문에, 잘 알려진 문자열 세트를 대신 사용하는 것이 일반적입니다. 문자열 리터럴 타입의 유니온으로 TypeScript는 이 패턴을 타입화할 수 있습니다:

```ts
declare function pad(s: string, n: number, direction: "left" | "right"): string;
pad("hi", 10, "left");
```

필요할 때, 컴파일러는 유닛 타입을 원시 타입으로 *확장*합니다. 예를 들어 `"foo"`를 `string`으로. 이것은 가변성을 사용할 때 발생하며, 가변 변수의 일부 사용을 방해할 수 있습니다:

```ts
let s = "right";
pad("hi", 10, s); // 오류: 'string'은 '"left" | "right"'에 할당할 수 없습니다
// Argument of type 'string' is not assignable to parameter of type '"left" | "right"'.
```

오류가 발생하는 방식은 다음과 같습니다:

- `"right": "right"`
- `s: string` 왜냐하면 `"right"`가 가변 변수에 할당될 때 `string`으로 확장되기 때문입니다.
- `string`은 `"left" | "right"`에 할당할 수 없습니다

`s`에 대한 타입 어노테이션으로 이 문제를 해결할 수 있지만, 그러면 `"left" | "right"` 타입이 아닌 변수를 `s`에 할당하는 것을 막습니다.

```ts
let s: "left" | "right" = "right";
pad("hi", 10, s);
```

## Haskell과 유사한 개념

### 문맥적 타이핑

TypeScript에는 변수 선언과 같이 타입을 추론할 수 있는 명백한 장소가 있습니다:

```ts
let s = "I'm a string!";
```

하지만 다른 C 문법 언어에서 작업했다면 예상하지 못할 수 있는 몇 가지 다른 장소에서도 타입을 추론합니다:

```ts
declare function map<T, U>(f: (t: T) => U, ts: T[]): U[];
let sns = map((n) => n.toString(), [1, 2, 3]);
```

여기서 `n: number`이기도 합니다. `T`와 `U`가 호출 전에 추론되지 않았음에도 불구하고요. 사실, `[1,2,3]`이 `T=number`를 추론하는 데 사용된 후, `n => n.toString()`의 반환 타입이 `U=string`을 추론하는 데 사용되어 `sns`가 `string[]` 타입을 가지게 됩니다.

추론은 어떤 순서로든 작동하지만, intellisense는 왼쪽에서 오른쪽으로만 작동하므로, TypeScript는 배열을 먼저 둔 `map`을 선언하는 것을 선호합니다:

```ts
declare function map<T, U>(ts: T[], f: (t: T) => U): U[];
```

문맥적 타이핑은 또한 객체 리터럴을 통해 재귀적으로 작동하고, `string`이나 `number`로 추론될 유닛 타입에도 작동합니다. 그리고 문맥에서 반환 타입을 추론할 수 있습니다:

```ts
declare function run<T>(thunk: (t: T) => void): T;
let i: { inference: string } = run((o) => {
  o.inference = "INSERT STATE HERE";
});
```

`o`의 타입은 `{ inference: string }`으로 결정됩니다. 왜냐하면:

- 선언 초기화자는 선언의 타입에 의해 문맥적으로 타이핑됩니다: `{ inference: string }`.
- 호출의 반환 타입은 추론을 위해 문맥적 타입을 사용하므로, 컴파일러는 `T={ inference: string }`을 추론합니다.
- 화살표 함수는 매개변수를 타이핑하기 위해 문맥적 타입을 사용하므로, 컴파일러는 `o: { inference: string }`을 제공합니다.

그리고 타이핑하는 동안 이것을 수행하므로, `o.`를 입력한 후 실제 프로그램에서 가질 수 있는 다른 속성과 함께 `inference` 속성에 대한 완성을 얻습니다.
전체적으로, 이 기능은 TypeScript의 추론을 통합 타입 추론 엔진처럼 보이게 할 수 있지만, 그렇지 않습니다.

### 타입 별칭

타입 별칭은 Haskell의 `type`처럼 단순한 별칭입니다. 컴파일러는 소스 코드에서 사용된 곳마다 별칭 이름을 사용하려고 시도하지만, 항상 성공하지는 않습니다.

```ts
type Size = [number, number];
let x: Size = [101.1, 999.9];
```

`newtype`에 가장 가까운 것은 *태그된 교차*입니다:

```ts
type FString = string & { __compileTimeOnly: any };
```

`FString`은 일반 문자열과 같지만, 컴파일러가 `__compileTimeOnly`라는 속성이 있다고 생각하고 실제로는 존재하지 않습니다. 이것은 `FString`이 여전히 `string`에 할당될 수 있지만, 그 반대는 안 된다는 것을 의미합니다.

### 판별 유니온

`data`에 가장 가까운 것은 판별 속성을 가진 타입들의 유니온이며, TypeScript에서는 보통 판별 유니온이라고 불립니다:

```ts
type Shape =
  | { kind: "circle"; radius: number }
  | { kind: "square"; x: number }
  | { kind: "triangle"; x: number; y: number };
```

Haskell과 달리, 태그 또는 구별자는 각 객체 타입의 단순한 속성입니다. 각 변형은 다른 유닛 타입을 가진 동일한 속성을 가집니다. 이것은 여전히 일반적인 유니온 타입입니다; 맨 앞의 `|`는 유니온 타입 문법의 선택적 부분입니다. 일반적인 JavaScript 코드를 사용하여 유니온의 구성원을 구별할 수 있습니다:

```ts
type Shape =
  | { kind: "circle"; radius: number }
  | { kind: "square"; x: number }
  | { kind: "triangle"; x: number; y: number };

function area(s: Shape) {
  if (s.kind === "circle") {
    return Math.PI * s.radius * s.radius;
  } else if (s.kind === "square") {
    return s.x * s.x;
  } else {
    return (s.x * s.y) / 2;
  }
}
```

`area`의 반환 타입이 `number`로 추론된다는 점에 주목하세요. TypeScript가 함수가 전체 함수임을 알기 때문입니다. 일부 변형이 다루어지지 않으면, `area`의 반환 타입은 대신 `number | undefined`가 됩니다.

또한, Haskell과 달리, 공통 속성은 모든 유니온에 나타나므로, 유니온의 여러 구성원을 유용하게 구별할 수 있습니다:

```ts
function height(s: Shape) {
  if (s.kind === "circle") {
    return 2 * s.radius;
  } else {
    // s.kind: "square" | "triangle"
    return s.x;
  }
}
```

### 타입 매개변수

대부분의 C 계열 언어처럼, TypeScript는 타입 매개변수의 선언을 요구합니다:

```ts
function liftArray<T>(t: T): Array<T> {
  return [t];
}
```

대소문자 요구 사항은 없지만, 타입 매개변수는 관례적으로 단일 대문자입니다. 타입 매개변수는 타입 클래스 제약과 비슷하게 동작하는 타입으로 제한될 수도 있습니다:

```ts
function firstish<T extends { length: number }>(t1: T, t2: T): T {
  return t1.length > t2.length ? t1 : t2;
}
```

TypeScript는 보통 인수의 타입을 기반으로 호출에서 타입 인수를 추론할 수 있으므로, 타입 인수는 보통 필요하지 않습니다.

TypeScript가 구조적이기 때문에, 명목적 시스템만큼 타입 매개변수가 필요하지 않습니다. 구체적으로, 함수를 다형적으로 만드는 데 필요하지 않습니다. 타입 매개변수는 매개변수를 같은 타입으로 제한하는 것과 같이 타입 정보를 *전파*하는 데만 사용해야 합니다:

```ts
function length<T extends ArrayLike<unknown>>(t: T): number {}

function length(t: ArrayLike<unknown>): number {}
```

첫 번째 `length`에서 `T`는 필요하지 않습니다; 한 번만 참조되므로, 반환 값이나 다른 매개변수의 타입을 제한하는 데 사용되지 않습니다.

#### 상위 종류 타입

TypeScript에는 상위 종류 타입이 없으므로, 다음은 유효하지 않습니다:

```ts
function length<T extends ArrayLike<unknown>, U>(m: T<U>) {}
```

#### 포인트-프리 프로그래밍

포인트-프리 프로그래밍 - 커링과 함수 합성의 많은 사용 - 은 JavaScript에서 가능하지만, 장황할 수 있습니다.
TypeScript에서 포인트-프리 프로그램에 대한 타입 추론은 종종 실패하므로, 값 매개변수 대신 타입 매개변수를 지정하게 됩니다. 결과가 너무 장황해서 보통 포인트-프리 프로그래밍을 피하는 것이 좋습니다.

### 모듈 시스템

JavaScript의 현대 모듈 문법은 Haskell과 약간 비슷하지만, `import`나 `export`가 있는 모든 파일은 암묵적으로 모듈입니다:

```ts
import { value, Type } from "npm-package";
import { other, Types } from "./local-package";
import * as prefix from "../lib/third-package";
```

commonjs 모듈 - node.js의 모듈 시스템을 사용하여 작성된 모듈 - 도 가져올 수 있습니다:

```ts
import f = require("single-function-package");
```

export 목록으로 내보낼 수 있습니다:

```ts
export { f };

function f() {
  return g();
}

function g() {} // g는 내보내지지 않음
```

또는 각 export를 개별적으로 표시하여:

```ts
export function f() { return g() }

function g() { }
```

후자의 스타일이 더 일반적이지만 둘 다 허용되며, 같은 파일에서도 가능합니다.

### `readonly`와 `const`

JavaScript에서 가변성이 기본이지만, *참조*가 불변임을 선언하기 위해 `const`로 변수 선언을 허용합니다. 참조 대상은 여전히 가변입니다:

```ts
const a = [1, 2, 3];
a.push(102); // ):
a[0] = 101; // D:
```

TypeScript는 추가로 속성에 대한 `readonly` 수정자가 있습니다.

```ts
interface Rx {
  readonly x: number;
}
let rx: Rx = { x: 1 };
rx.x = 12; // 오류
```

또한 모든 속성을 `readonly`로 만드는 매핑된 타입 `Readonly<T>`가 함께 제공됩니다:

```ts
interface X {
  x: number;
}
let rx: Readonly<X> = { x: 1 };
rx.x = 12; // 오류
```

그리고 부작용이 있는 메서드를 제거하고 배열의 인덱스에 쓰기를 방지하는 특정 `ReadonlyArray<T>` 타입과 이 타입에 대한 특별한 문법이 있습니다:

```ts
let a: ReadonlyArray<number> = [1, 2, 3];
let b: readonly number[] = [1, 2, 3];
a.push(102); // 오류
b[0] = 101; // 오류
```

배열과 객체 리터럴에서 작동하는 const 단언도 사용할 수 있습니다:

```ts
let a = [1, 2, 3] as const;
a.push(102); // 오류
a[0] = 101; // 오류
```

그러나 이러한 옵션 중 어느 것도 기본값이 아니므로, TypeScript 코드에서 일관되게 사용되지 않습니다.

### 다음 단계

이 문서는 일상적인 코드에서 사용할 문법과 타입에 대한 높은 수준의 개요입니다. 여기서부터 다음을 할 수 있습니다:

- 전체 핸드북을 [처음부터 끝까지](/docs/handbook/intro.html) 읽으세요

- [플레이그라운드 예제](/play#show-examples)를 탐색하세요

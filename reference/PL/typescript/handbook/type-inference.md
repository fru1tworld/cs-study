# 타입 추론 (Type Inference)

TypeScript에서 명시적인 타입 주석이 없을 때 타입 정보를 제공하기 위해 타입 추론이 사용되는 여러 곳이 있습니다. 예를 들어, 이 코드에서

```ts
let x = 3;
//  ^? let x: number
```

`x` 변수의 타입은 `number`로 추론됩니다.
이러한 종류의 추론은 변수와 멤버를 초기화하고, 매개변수 기본값을 설정하고, 함수 반환 타입을 결정할 때 발생합니다.

대부분의 경우, 타입 추론은 간단합니다.
다음 섹션에서는 타입이 추론되는 방식의 일부 뉘앙스를 살펴보겠습니다.

## 최적 공통 타입

여러 표현식에서 타입 추론이 이루어질 때, 해당 표현식의 타입을 사용하여 "최적 공통 타입"을 계산합니다. 예를 들어,

```ts
let x = [0, 1, null];
//  ^? let x: (number | null)[]
```

위 예제에서 `x`의 타입을 추론하려면, 각 배열 요소의 타입을 고려해야 합니다.
여기서 배열 타입에 대해 두 가지 선택이 주어집니다: `number`와 `null`.
최적 공통 타입 알고리즘은 각 후보 타입을 고려하고, 다른 모든 후보와 호환되는 타입을 선택합니다.

최적 공통 타입은 제공된 후보 타입에서 선택해야 하므로, 타입이 공통 구조를 공유하지만 모든 후보 타입의 슈퍼 타입인 타입이 없는 경우가 있습니다. 예를 들어:

```ts
// @strict: false
class Animal {}
class Rhino extends Animal {
  hasHorn: true;
}
class Elephant extends Animal {
  hasTrunk: true;
}
class Snake extends Animal {
  hasLegs: false;
}
// ---cut---
let zoo = [new Rhino(), new Elephant(), new Snake()];
//    ^? let zoo: (Rhino | Elephant | Snake)[]
```

이상적으로, `zoo`가 `Animal[]`로 추론되기를 원할 수 있지만, 배열에 `Animal` 타입의 객체가 엄격하게 없기 때문에, 배열 요소 타입에 대한 추론을 하지 않습니다.
이것을 수정하려면, 모든 다른 후보의 슈퍼 타입인 타입이 없을 때 명시적으로 타입을 제공하세요:

```ts
// @strict: false
class Animal {}
class Rhino extends Animal {
  hasHorn: true;
}
class Elephant extends Animal {
  hasTrunk: true;
}
class Snake extends Animal {
  hasLegs: false;
}
// ---cut---
let zoo: Animal[] = [new Rhino(), new Elephant(), new Snake()];
//    ^? let zoo: Animal[]
```

최적 공통 타입을 찾을 수 없으면, 결과 추론은 유니온 배열 타입 `(Rhino | Elephant | Snake)[]`입니다.

## 컨텍스트적 타이핑

타입 추론은 TypeScript의 일부 경우에 "반대 방향"으로도 작동합니다.
이것을 "컨텍스트적 타이핑"이라고 합니다. 컨텍스트적 타이핑은 표현식의 타입이 해당 위치에 의해 암시될 때 발생합니다. 예를 들어:

```ts
// @errors: 2339
window.onmousedown = function (mouseEvent) {
  console.log(mouseEvent.button);
  console.log(mouseEvent.kangaroo);
};
```

여기서 TypeScript 타입 체커는 `Window.onmousedown` 함수의 타입을 사용하여 할당의 오른쪽에 있는 함수 표현식의 타입을 추론했습니다.
그렇게 하면, `button` 프로퍼티를 포함하지만 `kangaroo` 프로퍼티는 포함하지 않는 `mouseEvent` 매개변수의 [타입](https://developer.mozilla.org/docs/Web/API/MouseEvent)을 추론할 수 있었습니다.

이것은 window에 이미 타입에 `onmousedown`이 선언되어 있기 때문에 작동합니다:

```ts
// 'window'라는 전역 변수가 있음을 선언합니다
declare var window: Window & typeof globalThis;

// 이것은 (단순화되어) 다음과 같이 선언됩니다:
interface Window extends GlobalEventHandlers {
  // ...
}

// 많은 알려진 핸들러 이벤트를 정의합니다
interface GlobalEventHandlers {
  onmousedown: ((this: GlobalEventHandlers, ev: MouseEvent) => any) | null;
  // ...
}
```

TypeScript는 다른 컨텍스트에서도 타입을 추론할 만큼 똑똑합니다:

```ts
// @errors: 2339
window.onscroll = function (uiEvent) {
  console.log(uiEvent.button);
};
```

위의 함수가 `Window.onscroll`에 할당되고 있다는 사실을 기반으로, TypeScript는 `uiEvent`가 이전 예제의 [MouseEvent](https://developer.mozilla.org/docs/Web/API/MouseEvent)가 아닌 [UIEvent](https://developer.mozilla.org/docs/Web/API/UIEvent)임을 알고 있습니다. `UIEvent` 객체에는 `button` 프로퍼티가 없으므로, TypeScript가 오류를 발생시킵니다.

이 함수가 컨텍스트적으로 타입이 지정된 위치에 없었다면, 함수의 인수는 암시적으로 `any` 타입을 가지며, 오류가 발생하지 않습니다([`noImplicitAny`](/tsconfig#noImplicitAny) 옵션을 사용하지 않는 한):

```ts
// @noImplicitAny: false
const handler = function (uiEvent) {
  console.log(uiEvent.button); // <- OK
};
```

함수의 인수에 명시적으로 타입 정보를 제공하여 컨텍스트적 타입을 재정의할 수도 있습니다:

```ts
window.onscroll = function (uiEvent: any) {
  console.log(uiEvent.button); // <- 이제 오류가 없습니다
};
```

그러나 `uiEvent`에 `button`이라는 프로퍼티가 없으므로 이 코드는 `undefined`를 로깅합니다.

컨텍스트적 타이핑은 많은 경우에 적용됩니다.
일반적인 경우에는 함수 호출에 대한 인수, 할당의 오른쪽, 타입 단언, 객체 및 배열 리터럴의 멤버, 반환 문이 포함됩니다.
컨텍스트적 타입은 최적 공통 타입에서 후보 타입으로도 작동합니다. 예를 들어:

```ts
// @strict: false
class Animal {}
class Rhino extends Animal {
  hasHorn: true;
}
class Elephant extends Animal {
  hasTrunk: true;
}
class Snake extends Animal {
  hasLegs: false;
}
// ---cut---
function createZoo(): Animal[] {
  return [new Rhino(), new Elephant(), new Snake()];
}
```

이 예제에서 최적 공통 타입은 네 가지 후보 집합을 가집니다: `Animal`, `Rhino`, `Elephant`, `Snake`.
이 중에서 `Animal`이 최적 공통 타입 알고리즘에 의해 선택될 수 있습니다.

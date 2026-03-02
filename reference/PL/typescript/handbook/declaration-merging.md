# 선언 병합 (Declaration Merging)

## 소개

TypeScript의 독특한 개념 중 일부는 타입 수준에서 JavaScript 객체의 형태를 설명합니다.
TypeScript에 특히 고유한 예는 '선언 병합' 개념입니다.
이 개념을 이해하면 기존 JavaScript로 작업할 때 이점을 얻을 수 있습니다.
또한 더 고급 추상화 개념으로의 문을 열어줍니다.

이 글의 목적상, "선언 병합"은 컴파일러가 동일한 이름으로 선언된 두 개의 별도 선언을 단일 정의로 병합한다는 것을 의미합니다.
이 병합된 정의는 원래 두 선언의 기능을 모두 가지고 있습니다.
병합할 수 있는 선언의 수에는 제한이 없습니다; 두 선언에만 국한되지 않습니다.

## 기본 개념

TypeScript에서 선언은 네임스페이스, 타입 또는 값의 세 그룹 중 하나 이상에 엔티티를 생성합니다.
네임스페이스 생성 선언은 점 표기법을 사용하여 접근하는 이름을 포함하는 네임스페이스를 생성합니다.
타입 생성 선언은 바로 그것을 수행합니다: 선언된 형태로 표시되고 주어진 이름에 바인딩된 타입을 생성합니다.
마지막으로, 값 생성 선언은 출력 JavaScript에서 볼 수 있는 값을 생성합니다.

| 선언 타입      | 네임스페이스 | 타입 |  값  |
| -------------- | :----------: | :--: | :--: |
| Namespace      |      X       |      |  X   |
| Class          |              |  X   |  X   |
| Enum           |              |  X   |  X   |
| Interface      |              |  X   |      |
| Type Alias     |              |  X   |      |
| Function       |              |      |  X   |
| Variable       |              |      |  X   |

각 선언으로 무엇이 생성되는지 이해하면 선언 병합을 수행할 때 무엇이 병합되는지 이해하는 데 도움이 됩니다.

## 인터페이스 병합

가장 단순하고 아마도 가장 일반적인 선언 병합 유형은 인터페이스 병합입니다.
가장 기본적인 수준에서 병합은 기계적으로 두 선언의 멤버를 동일한 이름의 단일 인터페이스로 결합합니다.

```ts
interface Box {
  height: number;
  width: number;
}

interface Box {
  scale: number;
}

let box: Box = { height: 5, width: 6, scale: 10 };
```

인터페이스의 비함수 멤버는 고유해야 합니다.
고유하지 않은 경우 동일한 타입이어야 합니다.
인터페이스가 동일한 이름의 비함수 멤버를 선언하지만 다른 타입인 경우 컴파일러가 오류를 발생시킵니다.

함수 멤버의 경우 동일한 이름의 각 함수 멤버는 동일한 함수의 오버로드를 설명하는 것으로 처리됩니다.
또한 인터페이스 `A`가 나중의 인터페이스 `A`와 병합되는 경우, 두 번째 인터페이스가 첫 번째 인터페이스보다 더 높은 우선순위를 갖는다는 점도 주목할 만합니다.

즉, 예제에서:

```ts
interface Cloner {
  clone(animal: Animal): Animal;
}

interface Cloner {
  clone(animal: Sheep): Sheep;
}

interface Cloner {
  clone(animal: Dog): Dog;
  clone(animal: Cat): Cat;
}
```

세 인터페이스가 병합되어 다음과 같은 단일 선언이 생성됩니다:

```ts
interface Cloner {
  clone(animal: Dog): Dog;
  clone(animal: Cat): Cat;
  clone(animal: Sheep): Sheep;
  clone(animal: Animal): Animal;
}
```

각 그룹의 요소가 동일한 순서를 유지하지만, 그룹 자체는 나중의 오버로드 세트가 먼저 정렬되어 병합됩니다.

이 규칙의 한 가지 예외는 특수화된 시그니처입니다.
시그니처에 _단일_ 문자열 리터럴 타입(예: 문자열 리터럴의 유니온이 아닌)인 매개변수가 있는 경우, 병합된 오버로드 목록의 맨 위로 버블링됩니다.

예를 들어, 다음 인터페이스들이 함께 병합됩니다:

```ts
interface Document {
  createElement(tagName: any): Element;
}
interface Document {
  createElement(tagName: "div"): HTMLDivElement;
  createElement(tagName: "span"): HTMLSpanElement;
}
interface Document {
  createElement(tagName: string): HTMLElement;
  createElement(tagName: "canvas"): HTMLCanvasElement;
}
```

결과적으로 `Document`의 병합된 선언은 다음과 같습니다:

```ts
interface Document {
  createElement(tagName: "canvas"): HTMLCanvasElement;
  createElement(tagName: "div"): HTMLDivElement;
  createElement(tagName: "span"): HTMLSpanElement;
  createElement(tagName: string): HTMLElement;
  createElement(tagName: any): Element;
}
```

## 네임스페이스 병합

인터페이스와 마찬가지로 동일한 이름의 네임스페이스도 멤버를 병합합니다.
네임스페이스는 네임스페이스와 값을 모두 생성하므로, 둘 다 어떻게 병합되는지 이해해야 합니다.

네임스페이스를 병합하려면, 각 네임스페이스에서 선언된 내보낸 인터페이스의 타입 정의가 자체적으로 병합되어, 병합된 인터페이스 정의가 내부에 있는 단일 네임스페이스를 형성합니다.

네임스페이스 값을 병합하려면, 각 선언 사이트에서 주어진 이름의 네임스페이스가 이미 존재하는 경우, 기존 네임스페이스를 가져와서 두 번째 네임스페이스의 내보낸 멤버를 첫 번째에 추가하여 더 확장됩니다.

이 예제에서 `Animals`의 선언 병합:

```ts
namespace Animals {
  export class Zebra {}
}

namespace Animals {
  export interface Legged {
    numberOfLegs: number;
  }
  export class Dog {}
}
```

은 다음과 같습니다:

```ts
namespace Animals {
  export interface Legged {
    numberOfLegs: number;
  }

  export class Zebra {}
  export class Dog {}
}
```

이 네임스페이스 병합 모델은 유용한 출발점이지만, 내보내지 않은 멤버에 어떤 일이 발생하는지도 이해해야 합니다.
내보내지 않은 멤버는 원래의 (병합되지 않은) 네임스페이스에서만 볼 수 있습니다. 이것은 병합 후, 다른 선언에서 온 병합된 멤버가 내보내지 않은 멤버를 볼 수 없음을 의미합니다.

이 예제에서 이것을 더 명확하게 볼 수 있습니다:

```ts
namespace Animal {
  let haveMuscles = true;

  export function animalsHaveMuscles() {
    return haveMuscles;
  }
}

namespace Animal {
  export function doAnimalsHaveMuscles() {
    return haveMuscles; // 오류, haveMuscles는 여기서 접근할 수 없습니다
  }
}
```

`haveMuscles`가 내보내지지 않았기 때문에, 동일한 병합되지 않은 네임스페이스를 공유하는 `animalsHaveMuscles` 함수만 이 심볼을 볼 수 있습니다.
`doAnimalsHaveMuscles` 함수는 병합된 `Animal` 네임스페이스의 일부이지만, 이 내보내지 않은 멤버를 볼 수 없습니다.

## 네임스페이스와 클래스, 함수, 열거형 병합

네임스페이스는 다른 유형의 선언과도 병합할 수 있을 만큼 유연합니다.
그렇게 하려면, 네임스페이스 선언이 병합할 선언 뒤에 와야 합니다. 결과 선언은 두 선언 타입의 프로퍼티를 모두 가집니다.
TypeScript는 이 기능을 사용하여 JavaScript와 다른 프로그래밍 언어의 일부 패턴을 모델링합니다.

### 네임스페이스와 클래스 병합

이것은 사용자에게 내부 클래스를 설명하는 방법을 제공합니다.

```ts
class Album {
  label: Album.AlbumLabel;
}
namespace Album {
  export class AlbumLabel {}
}
```

병합된 멤버의 가시성 규칙은 [네임스페이스 병합](./declaration-merging.html#네임스페이스-병합) 섹션에 설명된 것과 동일하므로, 병합된 클래스가 볼 수 있도록 `AlbumLabel` 클래스를 내보내야 합니다.
최종 결과는 다른 클래스 내부에서 관리되는 클래스입니다.
네임스페이스를 사용하여 기존 클래스에 더 많은 정적 멤버를 추가할 수도 있습니다.

내부 클래스 패턴 외에도, 함수를 만든 다음 함수에 프로퍼티를 추가하여 함수를 더 확장하는 JavaScript 관행에 익숙할 수도 있습니다.
TypeScript는 선언 병합을 사용하여 이와 같은 정의를 타입 안전한 방식으로 구축합니다.

```ts
function buildLabel(name: string): string {
  return buildLabel.prefix + name + buildLabel.suffix;
}

namespace buildLabel {
  export let suffix = "";
  export let prefix = "Hello, ";
}

console.log(buildLabel("Sam Smith"));
```

마찬가지로, 네임스페이스는 열거형을 정적 멤버로 확장하는 데 사용할 수 있습니다:

```ts
enum Color {
  red = 1,
  green = 2,
  blue = 4,
}

namespace Color {
  export function mixColor(colorName: string) {
    if (colorName == "yellow") {
      return Color.red + Color.green;
    } else if (colorName == "white") {
      return Color.red + Color.green + Color.blue;
    } else if (colorName == "magenta") {
      return Color.red + Color.blue;
    } else if (colorName == "cyan") {
      return Color.green + Color.blue;
    }
  }
}
```

## 허용되지 않는 병합

TypeScript에서 모든 병합이 허용되는 것은 아닙니다.
현재 클래스는 다른 클래스나 변수와 병합할 수 없습니다.
클래스 병합을 모방하는 방법에 대한 정보는 [TypeScript의 믹스인](/docs/handbook/mixins.html) 섹션을 참조하세요.

## 모듈 확장

JavaScript 모듈은 병합을 지원하지 않지만, 기존 객체를 가져온 다음 업데이트하여 패치할 수 있습니다.
장난감 Observable 예제를 살펴봅시다:

```ts
// observable.ts
export class Observable<T> {
  // ... 구현은 독자를 위한 연습으로 남깁니다 ...
}

// map.ts
import { Observable } from "./observable";
Observable.prototype.map = function (f) {
  // ... 독자를 위한 또 다른 연습
};
```

이것은 TypeScript에서도 잘 작동하지만, 컴파일러는 `Observable.prototype.map`에 대해 알지 못합니다.
모듈 확장을 사용하여 컴파일러에게 이에 대해 알릴 수 있습니다:

```ts
// observable.ts
export class Observable<T> {
  // ... 구현은 독자를 위한 연습으로 남깁니다 ...
}

// map.ts
import { Observable } from "./observable";
declare module "./observable" {
  interface Observable<T> {
    map<U>(f: (x: T) => U): Observable<U>;
  }
}
Observable.prototype.map = function (f) {
  // ... 독자를 위한 또 다른 연습
};

// consumer.ts
import { Observable } from "./observable";
import "./map";
let o: Observable<number>;
o.map((x) => x.toFixed());
```

모듈 이름은 `import`/`export`의 모듈 지정자와 동일한 방식으로 해석됩니다.
자세한 내용은 [모듈](/docs/handbook/modules.html)을 참조하세요.
그런 다음 확장의 선언은 원본 파일에서 선언된 것처럼 병합됩니다.

그러나 두 가지 제한 사항을 명심해야 합니다:

1. 확장에서 새 최상위 선언을 선언할 수 없습니다 -- 기존 선언에 대한 패치만 가능합니다.
2. 기본 내보내기도 확장할 수 없습니다. 명명된 내보내기만 가능합니다(내보낸 이름으로 내보내기를 확장해야 하며, `default`는 예약어이기 때문입니다 - 자세한 내용은 [#14080](https://github.com/Microsoft/TypeScript/issues/14080)를 참조하세요)

### 전역 확장

모듈 내부에서 전역 범위에 선언을 추가할 수도 있습니다:

```ts
// observable.ts
export class Observable<T> {
  // ... 아직 구현이 없습니다 ...
}

declare global {
  interface Array<T> {
    toObservable(): Observable<T>;
  }
}

Array.prototype.toObservable = function () {
  // ...
};
```

전역 확장은 모듈 확장과 동일한 동작 및 제한을 가집니다.

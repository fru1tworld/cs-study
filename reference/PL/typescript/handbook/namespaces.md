# 네임스페이스 (Namespaces)

> **용어에 대한 참고:**
> TypeScript 1.5에서 명명법이 변경되었다는 것을 주목하는 것이 중요합니다.
> "내부 모듈"은 이제 "네임스페이스"입니다.
> "외부 모듈"은 이제 단순히 "모듈"로, [ECMAScript 2015](https://www.ecma-international.org/ecma-262/6.0/)의 용어에 맞춰졌습니다 (즉, `module X {`는 이제 선호되는 `namespace X {`와 동등합니다).

이 글에서는 TypeScript에서 네임스페이스(이전에는 "내부 모듈")를 사용하여 코드를 구성하는 다양한 방법을 설명합니다.
용어에 대한 참고에서 언급했듯이, "내부 모듈"은 이제 "네임스페이스"라고 합니다.
또한, 내부 모듈을 선언할 때 `module` 키워드가 사용되었던 모든 곳에서 `namespace` 키워드를 대신 사용할 수 있고 사용해야 합니다.
이렇게 하면 유사하게 명명된 용어로 새 사용자를 과부하시켜 혼동을 주는 것을 방지합니다.

## 첫 번째 단계

이 페이지 전체에서 예제로 사용할 프로그램부터 시작하겠습니다.
웹페이지의 폼에서 사용자의 입력을 확인하거나 외부에서 제공된 데이터 파일의 형식을 확인하기 위해 작성할 수 있는 것처럼 간단한 문자열 유효성 검사기의 작은 집합을 작성했습니다.

## 단일 파일의 유효성 검사기

```ts
interface StringValidator {
  isAcceptable(s: string): boolean;
}

let lettersRegexp = /^[A-Za-z]+$/;
let numberRegexp = /^[0-9]+$/;

class LettersOnlyValidator implements StringValidator {
  isAcceptable(s: string) {
    return lettersRegexp.test(s);
  }
}

class ZipCodeValidator implements StringValidator {
  isAcceptable(s: string) {
    return s.length === 5 && numberRegexp.test(s);
  }
}

// 시도할 몇 가지 샘플
let strings = ["Hello", "98052", "101"];

// 사용할 유효성 검사기
let validators: { [s: string]: StringValidator } = {};
validators["ZIP code"] = new ZipCodeValidator();
validators["Letters only"] = new LettersOnlyValidator();

// 각 문자열이 각 유효성 검사기를 통과했는지 표시
for (let s of strings) {
  for (let name in validators) {
    let isMatch = validators[name].isAcceptable(s);
    console.log(`'${s}' ${isMatch ? "matches" : "does not match"} '${name}'.`);
  }
}
```

## 네임스페이싱

더 많은 유효성 검사기를 추가하면, 타입을 추적하고 다른 객체와의 이름 충돌을 걱정하지 않도록 어떤 종류의 조직 체계가 필요합니다.
많은 다른 이름을 전역 네임스페이스에 넣는 대신, 객체를 네임스페이스로 래핑합시다.

이 예제에서는 모든 유효성 검사기 관련 엔티티를 `Validation`이라는 네임스페이스로 이동합니다.
여기서 인터페이스와 클래스가 네임스페이스 외부에서 보이도록 하려면 `export`로 시작합니다.
반대로, 변수 `lettersRegexp`와 `numberRegexp`는 구현 세부 사항이므로, 내보내지 않아 네임스페이스 외부의 코드에 보이지 않습니다.
파일 하단의 테스트 코드에서, 네임스페이스 외부에서 사용될 때 타입의 이름을 한정해야 합니다. 예: `Validation.LettersOnlyValidator`.

## 네임스페이스 유효성 검사기

```ts
namespace Validation {
  export interface StringValidator {
    isAcceptable(s: string): boolean;
  }

  const lettersRegexp = /^[A-Za-z]+$/;
  const numberRegexp = /^[0-9]+$/;

  export class LettersOnlyValidator implements StringValidator {
    isAcceptable(s: string) {
      return lettersRegexp.test(s);
    }
  }

  export class ZipCodeValidator implements StringValidator {
    isAcceptable(s: string) {
      return s.length === 5 && numberRegexp.test(s);
    }
  }
}

// 시도할 몇 가지 샘플
let strings = ["Hello", "98052", "101"];

// 사용할 유효성 검사기
let validators: { [s: string]: Validation.StringValidator } = {};
validators["ZIP code"] = new Validation.ZipCodeValidator();
validators["Letters only"] = new Validation.LettersOnlyValidator();

// 각 문자열이 각 유효성 검사기를 통과했는지 표시
for (let s of strings) {
  for (let name in validators) {
    console.log(
      `"${s}" - ${
        validators[name].isAcceptable(s) ? "matches" : "does not match"
      } ${name}`
    );
  }
}
```

## 여러 파일로 분할

애플리케이션이 커지면, 코드를 여러 파일로 분할하여 유지보수하기 쉽게 하고 싶을 것입니다.

## 다중 파일 네임스페이스

여기서, `Validation` 네임스페이스를 여러 파일로 분할합니다.
파일이 분리되어 있어도, 각각 동일한 네임스페이스에 기여할 수 있으며 모두 한 곳에서 정의된 것처럼 소비할 수 있습니다.
파일 간에 종속성이 있으므로, 파일 간의 관계를 컴파일러에 알려주기 위해 참조 태그를 추가합니다.
테스트 코드는 그 외에는 변경되지 않습니다.

##### Validation.ts

```ts
namespace Validation {
  export interface StringValidator {
    isAcceptable(s: string): boolean;
  }
}
```

##### LettersOnlyValidator.ts

```ts
/// <reference path="Validation.ts" />
namespace Validation {
  const lettersRegexp = /^[A-Za-z]+$/;
  export class LettersOnlyValidator implements StringValidator {
    isAcceptable(s: string) {
      return lettersRegexp.test(s);
    }
  }
}
```

##### ZipCodeValidator.ts

```ts
/// <reference path="Validation.ts" />
namespace Validation {
  const numberRegexp = /^[0-9]+$/;
  export class ZipCodeValidator implements StringValidator {
    isAcceptable(s: string) {
      return s.length === 5 && numberRegexp.test(s);
    }
  }
}
```

##### Test.ts

```ts
/// <reference path="Validation.ts" />
/// <reference path="LettersOnlyValidator.ts" />
/// <reference path="ZipCodeValidator.ts" />

// 시도할 몇 가지 샘플
let strings = ["Hello", "98052", "101"];

// 사용할 유효성 검사기
let validators: { [s: string]: Validation.StringValidator } = {};
validators["ZIP code"] = new Validation.ZipCodeValidator();
validators["Letters only"] = new Validation.LettersOnlyValidator();

// 각 문자열이 각 유효성 검사기를 통과했는지 표시
for (let s of strings) {
  for (let name in validators) {
    console.log(
      `"${s}" - ${
        validators[name].isAcceptable(s) ? "matches" : "does not match"
      } ${name}`
    );
  }
}
```

여러 파일이 관련되면, 모든 컴파일된 코드가 로드되도록 해야 합니다.
이를 수행하는 두 가지 방법이 있습니다.

첫째, [`outFile`](/tsconfig#outFile) 옵션을 사용하여 연결된 출력으로 모든 입력 파일을 단일 JavaScript 출력 파일로 컴파일할 수 있습니다:

```Shell
tsc --outFile sample.js Test.ts
```

컴파일러는 파일에 있는 참조 태그를 기반으로 출력 파일을 자동으로 정렬합니다. 각 파일을 개별적으로 지정할 수도 있습니다:

```Shell
tsc --outFile sample.js Validation.ts LettersOnlyValidator.ts ZipCodeValidator.ts Test.ts
```

또는, 파일별 컴파일(기본값)을 사용하여 각 입력 파일에 대해 하나의 JavaScript 파일을 방출할 수 있습니다.
여러 JS 파일이 생성되면, 웹페이지에서 `<script>` 태그를 사용하여 적절한 순서로 각 방출된 파일을 로드해야 합니다. 예를 들어:

##### MyTestPage.html (발췌)

```html
<script src="Validation.js" type="text/javascript" />
<script src="LettersOnlyValidator.js" type="text/javascript" />
<script src="ZipCodeValidator.js" type="text/javascript" />
<script src="Test.js" type="text/javascript" />
```

## 별칭

네임스페이스 작업을 단순화할 수 있는 또 다른 방법은 `import q = x.y.z`를 사용하여 일반적으로 사용되는 객체에 대해 더 짧은 이름을 만드는 것입니다.
모듈을 로드하는 데 사용되는 `import x = require("name")` 구문과 혼동하지 마세요, 이 구문은 단순히 지정된 심볼에 대한 별칭을 만듭니다.
모듈 임포트에서 생성된 객체를 포함하여 모든 종류의 식별자에 대해 이러한 종류의 임포트(일반적으로 별칭이라고 함)를 사용할 수 있습니다.

```ts
namespace Shapes {
  export namespace Polygons {
    export class Triangle {}
    export class Square {}
  }
}

import polygons = Shapes.Polygons;
let sq = new polygons.Square(); // 'new Shapes.Polygons.Square()'와 동일
```

`require` 키워드를 사용하지 않는다는 것에 주목하세요; 대신 임포트하는 심볼의 정규화된 이름에서 직접 할당합니다.
이것은 `var`를 사용하는 것과 유사하지만, 임포트된 심볼의 타입 및 네임스페이스 의미에서도 작동합니다.
중요하게도, 값의 경우, `import`는 원본 심볼과 구별되는 참조이므로, 별칭이 지정된 `var`에 대한 변경 사항은 원본 변수에 반영되지 않습니다.

## 다른 JavaScript 라이브러리와 작업하기

TypeScript로 작성되지 않은 라이브러리의 형태를 설명하려면, 라이브러리가 노출하는 API를 선언해야 합니다.
대부분의 JavaScript 라이브러리는 몇 가지 최상위 객체만 노출하기 때문에, 네임스페이스는 이를 나타내는 좋은 방법입니다.

구현을 정의하지 않는 선언을 "앰비언트"라고 합니다.
일반적으로 이들은 `.d.ts` 파일에 정의됩니다.
C/C++에 익숙하다면, 이들을 `.h` 파일로 생각할 수 있습니다.
몇 가지 예를 살펴봅시다.

## 앰비언트 네임스페이스

인기 있는 라이브러리 D3는 `d3`라는 전역 객체에 기능을 정의합니다.
이 라이브러리는 (모듈 로더 대신) `<script>` 태그를 통해 로드되기 때문에, 선언에서 네임스페이스를 사용하여 형태를 정의합니다.
TypeScript 컴파일러가 이 형태를 보려면, 앰비언트 네임스페이스 선언을 사용합니다.
예를 들어, 다음과 같이 작성을 시작할 수 있습니다:

##### D3.d.ts (단순화된 발췌)

```ts
declare namespace D3 {
  export interface Selectors {
    select: {
      (selector: string): Selection;
      (element: EventTarget): Selection;
    };
  }

  export interface Event {
    x: number;
    y: number;
  }

  export interface Base extends Selectors {
    event: Event;
  }
}

declare var d3: D3.Base;
```

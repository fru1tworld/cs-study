# TypeScript 시작하기

## TypeScript 시작하기

## TypeScript 핸드북

> **원문:** https://www.typescriptlang.org/docs/handbook/intro.html

### 이 핸드북에 대하여

- JavaScript: 프로그래밍 커뮤니티에 소개된 지 20년 이상 → 역대 가장 널리 사용되는 크로스 플랫폼 언어 중 하나로 성장
- 시작은 웹페이지에 간단한 상호작용을 추가하는 작은 스크립팅 언어 → 모든 규모의 프론트엔드·백엔드 애플리케이션에서 선택받는 언어로 성장
- 문제: 프로그램의 크기·범위·복잡성은 기하급수적으로 증가했으나, JavaScript 언어 자체가 서로 다른 코드 단위 간의 관계를 표현하는 능력은 이를 따라가지 못함
  - JavaScript의 독특한 런타임 시맨틱과 결합 → 언어와 프로그램 복잡성 간 불일치로 대규모 개발 관리가 어려워짐

- 프로그래머가 작성하는 가장 흔한 오류 유형: 타입 오류 → 다른 종류의 값이 예상되는 곳에 특정 종류의 값을 사용한 경우
  - 원인: 단순 오타 · 라이브러리 API 표면에 대한 이해 부족 · 런타임 동작에 대한 잘못된 가정 · 기타 오류
- TypeScript의 목표: JavaScript 프로그램을 위한 정적 타입 검사기
  - 정적: 코드 실행 전에 검사 수행
  - 타입 검사: 프로그램의 타입이 올바른지 확인

- JavaScript 배경 없이 TypeScript를 첫 언어로 배우려는 경우 → 먼저 [Microsoft Learn JavaScript 튜토리얼](https://developer.microsoft.com/javascript/)이나 [Mozilla Web Docs의 JavaScript](https://developer.mozilla.org/docs/Web/JavaScript/Guide) 문서 선행 학습 권장
- 다른 언어 경험이 있는 경우 → 핸드북을 읽으며 JavaScript 문법을 빠르게 습득 가능

### 이 핸드북의 구성

- 핸드북 구성: 두 섹션
  - 핸드북
    - 일상적인 프로그래머에게 TypeScript를 설명하는 포괄적 문서
    - 왼쪽 네비게이션에서 위→아래 순서로 읽음
    - 각 챕터·페이지 → 주어진 개념에 대한 강력한 이해 제공
    - 완전한 언어 명세는 아니지만, 언어의 모든 기능·동작에 대한 포괄적 가이드로 의도됨
    - 연습 완료 후 가능한 것
      - 일반적으로 사용되는 TypeScript 문법·패턴 읽고 이해
      - 중요한 컴파일러 옵션의 효과 설명
      - 대부분의 경우 타입 시스템 동작을 올바르게 예측
    - 명확성·간결성을 위해 주요 내용은 모든 엣지 케이스·세부 사항을 다루지 않음 → 세부 내용은 참조 문서에서 확인
  - 참조 파일
    - 네비게이션에서 핸드북 아래 위치 → TypeScript의 특정 부분이 작동하는 방식에 대한 더 풍부한 이해 제공
    - 위→아래로 읽을 수 있으나, 각 섹션은 단일 개념에 대한 깊은 설명이 목표(연속성 목표 아님)

#### 다루지 않는 내용

- 핸드북은 몇 시간 안에 읽을 수 있는 간결한 문서로 의도 → 특정 주제는 다루지 않음
- 함수·클래스·클로저 같은 핵심 JavaScript 기본 사항은 완전히 소개하지 않음(적절한 경우 배경 읽기 링크 포함)
- 언어 명세 대체 목적 아님
  - 일부 엣지 케이스·동작의 공식 설명은 이해하기 쉬운 설명을 위해 생략
  - 대신 TypeScript 동작을 더 정확·공식적으로 설명하는 별도의 참조 페이지 존재
  - 참조 페이지는 TypeScript 초심자 대상이 아님 → 고급 용어 사용 또는 아직 다루지 않은 주제 참조 가능
- 필요한 경우를 제외하고 TypeScript와 다른 도구의 상호작용은 다루지 않음
  - 범위 밖 예: webpack·rollup·parcel·react·babel·closure·lerna·rush·bazel·preact·vue·angular·svelte·jquery·yarn·npm으로 TypeScript 구성하는 방법
  - 해당 리소스는 웹의 다른 곳에서 확인 가능

### 시작하기

- [기본 사항](/docs/handbook/2/basic-types.html) 진입 전 다음 소개 페이지 중 하나를 읽는 것을 권장
  - 목적: TypeScript와 선호 언어 간 주요 유사점·차이점 강조, 해당 언어에 특정한 일반적 오해 해소
- [새 프로그래머를 위한 TypeScript](/docs/handbook/typescript-from-scratch.html)
- [JavaScript 프로그래머를 위한 TypeScript](/docs/handbook/typescript-in-5-minutes.html)
- [Java/C# 프로그래머를 위한 TypeScript](/docs/handbook/typescript-in-5-minutes-oop.html)
- [함수형 프로그래머를 위한 TypeScript](/docs/handbook/typescript-in-5-minutes-func.html)
- 위 항목이 필요 없으면 [기본 사항](/docs/handbook/2/basic-types.html)으로 바로 이동

---

## 처음 프로그래밍을 배우는 사람을 위한 TypeScript

> **원문:** https://www.typescriptlang.org/docs/handbook/typescript-from-scratch.html

- TypeScript를 첫 번째 언어로 선택 → 좋은 결정
- TypeScript는 흔히 JavaScript의 "변형" 또는 "파생형"으로 소개됨
  - TS와 JS의 관계는 현대 프로그래밍 언어 중 매우 독특함 → 이 관계를 이해하면 TypeScript가 JavaScript에 추가하는 것을 이해하는 데 도움

### JavaScript란 무엇인가? 간략한 역사

- JavaScript(ECMAScript)는 브라우저용 간단한 스크립팅 언어로 시작
  - 발명 당시: 웹 페이지에 포함된 짧은 코드 조각용으로 예상 → 수십 줄 이상 작성은 드문 경우
  - 이 때문에 초기 웹 브라우저는 이러한 코드를 상당히 느리게 실행
  - 시간이 지나며 JS 인기 상승 → 웹 개발자들이 인터랙티브 경험 제작에 사용
- 웹 브라우저 개발자들의 대응: 실행 엔진 최적화(동적 컴파일) · JS로 할 수 있는 것 확장(API 추가) → JS 사용 증가로 이어지는 순환
  - 현대 웹사이트: 브라우저가 수십만 줄 규모 애플리케이션을 자주 실행
  - "웹"의 성장: 단순한 정적 페이지 네트워크 → 다양한 애플리케이션을 위한 플랫폼으로 진화
- JS는 브라우저 맥락 밖에서도 인기 획득(예: node.js로 서버 구현)
  - "어디서나 실행 가능" 특성 → 크로스 플랫폼 개발에 매력적
  - 최근에는 JavaScript만으로 전체 스택을 프로그래밍하는 개발자도 다수
- 요약: 빠른 사용을 위해 설계된 언어 → 수백만 줄 애플리케이션을 작성하는 완전한 도구로 성장
  - 모든 언어에는 고유한 특이점 존재 → JavaScript의 소박한 시작이 특이점을 다수 만듦
  - 예1: 동등 연산자(`==`)는 피연산자를 강제 변환 → 예상치 못한 동작 유발

```ts
if ("" == 0) {
  // 참입니다! 하지만 왜??
}
if (1 < x < 3) {
  // *어떤* x 값에 대해서도 참입니다!
}
```

  - 예2: JavaScript는 존재하지 않는 속성에 접근하는 것도 허용

```ts
const obj = { width: 10, height: 15 };
// 왜 이것이 NaN일까요? 철자를 쓰기가 어렵네요!
const area = obj.width * obj.heigth;
```

- 대부분의 프로그래밍 언어는 이런 오류 발생 시 에러 발생(일부는 컴파일 중, 코드 실행 전)
- 작은 프로그램에서는 이런 특이점이 귀찮은 수준이지만 관리 가능 → 수백~수천 줄 규모에서는 심각한 문제로 확대

### TypeScript: 정적 타입 검사기

- 일부 언어는 버그 있는 프로그램을 실행 자체를 하지 않음
  - 코드를 실행하지 않고 오류를 감지 = 정적 검사
  - 연산되는 값의 종류를 기반으로 오류 여부를 결정 = 정적 타입 검사
- TypeScript: 실행 전 오류를 검사, 값의 종류를 기반으로 검사 → 정적 타입 검사기
  - 위 마지막 예제 오류 원인: `obj`의 타입 문제
- TypeScript가 발견한 오류

```ts
const obj = { width: 10, height: 15 };
const area = obj.width * obj.heigth;
// Property 'heigth' does not exist on type '{ width: number; height: number; }'. Did you mean 'height'?
// '{ width: number; height: number; }' 타입에 'heigth' 속성이 존재하지 않습니다. 'height'를 의미했나요?
```

#### JavaScript의 타입이 있는 상위 집합

- TypeScript와 JavaScript의 관계

##### 문법

- TypeScript는 JavaScript의 상위 집합 → JS 문법은 모두 유효한 TS
  - 문법: 프로그램을 형성하기 위해 텍스트를 작성하는 방식
  - 예: `)`가 빠진 코드는 문법 오류

```ts
let a = (4
// ')' expected.
// ')'가 필요합니다.
```

- TypeScript는 문법 문제로 JavaScript 코드를 오류로 간주하지 않음
  - → 작동하는 JavaScript 코드를 작성 방식 걱정 없이 TypeScript 파일에 그대로 사용 가능

##### 타입

- TypeScript는 "타입이 있는" 상위 집합 → 다양한 종류의 값이 어떻게 사용될 수 있는지에 대한 규칙 추가
  - 앞서의 `obj.heigth` 오류: 문법 오류가 아니라 값(타입)을 잘못된 방식으로 사용한 오류
- 예: 브라우저에서 실행 가능한 JavaScript 코드, 값을 로그로 출력

```ts
console.log(4 / []);
```

  - 문법적으로 유효한 이 프로그램은 `Infinity`를 로그로 출력
  - TypeScript는 숫자를 배열로 나누는 것을 무의미한 연산으로 간주 → 오류 발생

```ts
console.log(4 / []);
// The right-hand side of an arithmetic operation must be of type 'any', 'number', 'bigint' or an enum type.
// 산술 연산의 오른쪽은 'any', 'number', 'bigint' 또는 열거형 타입이어야 합니다.
```

- 숫자를 배열로 나누는 의도가 실제로 있을 수도 있으나(동작 확인 목적) 대부분은 프로그래밍 실수
  - TypeScript 타입 검사기 설계 목표: 올바른 프로그램은 통과, 일반적인 오류는 최대한 포착
  - (검사 엄격도를 구성하는 설정은 이후 학습)
- JavaScript 파일 → TypeScript 파일로 이전 시 코드 작성 방식에 따라 타입 오류가 나타날 수 있음
  - 코드의 정당한 문제일 수도 있고, TypeScript가 지나치게 보수적인 판단일 수도 있음
  - 이 가이드에서 이러한 오류를 제거하기 위한 다양한 TypeScript 문법을 다룰 예정

##### 런타임 동작

- TypeScript는 JavaScript의 런타임 동작을 보존하는 언어
  - 예: JavaScript에서 0으로 나누면 런타임 예외 대신 `Infinity` 생성
  - 원칙: TypeScript는 JavaScript 코드의 런타임 동작을 변경하지 않음
- 의미: JavaScript → TypeScript로 코드 이전 시, TypeScript가 타입 오류를 지적해도 동일한 방식으로 실행됨이 보장
- JavaScript와 동일한 런타임 동작 유지 = TypeScript의 기본 약속
  - 이유: 두 언어 사이를 전환할 때 프로그램 동작을 멈추게 할 미묘한 차이를 걱정할 필요 없음

##### 지워지는 타입

- TypeScript 컴파일러는 코드 검사 완료 후 결과 "컴파일된" 코드 생성을 위해 타입을 지움
  - → 컴파일된 일반 JS 코드에는 타입 정보가 남지 않음
- TypeScript는 추론한 타입을 기반으로 프로그램의 동작을 절대 변경하지 않음
  - 결론: 컴파일 중 타입 오류는 볼 수 있으나, 타입 시스템 자체는 실행 시 동작에 영향을 주지 않음
- TypeScript는 추가 런타임 라이브러리를 제공하지 않음
  - 프로그램은 JavaScript 프로그램과 동일한 표준 라이브러리(또는 외부 라이브러리) 사용 → 별도 학습이 필요한 TypeScript 전용 프레임워크 없음

### JavaScript와 TypeScript 배우기

- 자주 나오는 질문: "JavaScript를 배워야 할까요, TypeScript를 배워야 할까요?"
- 답: JavaScript를 배우지 않고는 TypeScript를 배울 수 없음
  - TypeScript는 JavaScript와 문법·런타임 동작을 공유 → JavaScript 학습이 곧 TypeScript 학습에 도움
- 프로그래머가 JavaScript를 배울 수 있는 리소스는 다수 존재 → TypeScript 작성 시에도 무시하지 말 것
  - 예: StackOverflow에서 `javascript` 태그 질문 수가 `typescript` 태그의 약 20배 → 모든 `javascript` 질문은 TypeScript에도 적용됨
- "TypeScript에서 리스트를 정렬하는 방법" 같은 검색 시 기억할 점: TypeScript는 컴파일 타임 타입 검사기가 있는 JavaScript의 런타임
  - TypeScript에서 리스트를 정렬하는 방법 = JavaScript에서 하는 방법과 동일
  - TypeScript 전용 리소스를 찾으면 좋지만, 런타임 작업 질문에 TypeScript 전용 답변이 필요하다고 스스로 제한하지 말 것

### 다음 단계

- 위 내용은 일상적인 TypeScript에서 사용되는 문법·도구에 대한 간략한 개요
- 이후 진행 가능한 것
  - JavaScript 기본 사항 학습
    - [Microsoft의 JavaScript 리소스](https://developer.microsoft.com/javascript/)
    - [Mozilla Web Docs의 JavaScript 가이드](https://developer.mozilla.org/docs/Web/JavaScript/Guide)
  - [JavaScript 프로그래머를 위한 TypeScript](/docs/handbook/typescript-in-5-minutes.html)로 계속 학습
  - 전체 핸드북을 [처음부터 끝까지](/docs/handbook/intro.html) 학습
  - [플레이그라운드 예제](/play#show-examples) 탐색

---

## JavaScript 프로그래머를 위한 TypeScript

> **원문:** https://www.typescriptlang.org/docs/handbook/typescript-in-5-minutes.html

- TypeScript는 JavaScript와 특별한 관계
  - JavaScript의 모든 기능 제공 + 추가 계층인 타입 시스템 도입
  - 예: JavaScript는 `string`·`number` 같은 언어 원시 타입 제공하지만 일관된 할당 여부는 검사하지 않음 → TypeScript는 검사함
- 의미: 기존에 작동하는 JavaScript 코드는 TypeScript 코드이기도 함
  - TypeScript의 주요 이점: 코드의 예상치 못한 동작을 강조 → 버그 가능성 감소
- 이 튜토리얼: TypeScript 타입 시스템에 초점을 맞춘 개요

### 추론에 의한 타입

- TypeScript는 JavaScript 언어를 알고 있음 → 많은 경우 타입을 자동 생성
  - 예: 변수 생성 시 특정 값 할당 → TypeScript가 그 값을 타입으로 사용

```ts
let helloWorld = "Hello World";
//  let helloWorld: string
```

- JavaScript 동작 원리에 대한 이해를 기반으로, TypeScript는 JavaScript 코드를 그대로 받아들이면서도 타입을 갖는 타입 시스템 구축 가능
  - → 타입을 명시하기 위한 추가 문자 없이 타입 시스템 제공
  - 위 예제에서 TypeScript는 `helloWorld`가 `string`임을 인지
- Visual Studio Code에서 JavaScript 작성 시 경험하는 편집기 자동 완성 → 내부적으로 TypeScript 사용

### 타입 정의하기

- JavaScript에서는 다양한 디자인 패턴 사용 가능
  - 일부 패턴(예: 동적 프로그래밍 사용 패턴)은 타입 자동 추론이 어려움
  - TypeScript는 JavaScript 언어의 확장을 지원 → 타입이 무엇이어야 하는지 알려줄 장소 제공
- 예: `name: string`, `id: number`를 포함하는 추론된 타입의 객체 생성

```ts
const user = {
  name: "Hayes",
  id: 0,
};
```

- `interface` 선언으로 객체 모양을 명시적으로 설명 가능

```ts
interface User {
  name: string;
  id: number;
}
```

- 변수 선언 후 `: TypeName` 문법으로 JavaScript 객체가 새로운 `interface`의 모양을 따른다고 선언 가능

```ts
const user: User = {
  name: "Hayes",
  id: 0,
};
```

- 제공한 인터페이스와 불일치하는 객체 제공 시 TypeScript가 경고

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

- JavaScript가 클래스·객체 지향 프로그래밍을 지원 → TypeScript도 지원
  - 클래스와 함께 인터페이스 선언 사용 가능

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

- 인터페이스로 함수의 매개변수·반환 값에 어노테이션 가능

```ts
function deleteUser(user: User) {
  // ...
}

function getAdminUser(): User {
  //...
}
```

- JavaScript에서 이미 사용 가능한 원시 타입: `boolean`·`bigint`·`null`·`number`·`string`·`symbol`·`undefined` → 인터페이스에서 사용 가능
  - TypeScript가 확장한 타입: `any`(무엇이든 허용) · `unknown`(사용하는 쪽에서 타입을 명시하도록 강제) · `never`(발생 불가능한 타입) · `void`(반환 값이 `undefined`이거나 없는 함수)
- 타입 구축 문법 두 가지: [인터페이스와 타입](/play/?e=83#example/types-vs-interfaces)
  - `interface`를 우선 사용 권장
  - 특정 기능이 필요할 때만 `type` 사용

### 타입 조합하기

- 간단한 타입을 결합해 복잡한 타입 생성 가능
  - 방법: 유니온 · 제네릭

#### 유니온

- 유니온: 타입이 여러 타입 중 하나일 수 있다고 선언
  - 예: `boolean` 타입을 `true` 또는 `false`로 설명

```ts
type MyBool = true | false;
```

- 참고: 위 `MyBool`에 마우스를 올리면 `boolean`으로 분류됨 → 구조적 타입 시스템의 속성(아래에서 상세 설명)
- 유니온 타입의 일반적 사용 사례: 값이 될 수 있는 `string` 또는 `number` [리터럴](/docs/handbook/2/everyday-types.html#literal-types) 세트 설명

```ts
type WindowStates = "open" | "closed" | "minimized";
type LockStates = "locked" | "unlocked";
type PositiveOddNumbersUnderTen = 1 | 3 | 5 | 7 | 9;
```

- 유니온은 다양한 타입 처리 방법도 제공
  - 예: `array` 또는 `string`을 받는 함수

```ts
function getLength(obj: string | string[]) {
  return obj.length;
}
```

- 변수의 타입을 알아내려면 `typeof` 사용
  - string: `typeof s === "string"`
  - number: `typeof n === "number"`
  - boolean: `typeof b === "boolean"`
  - undefined: `typeof undefined === "undefined"`
  - function: `typeof f === "function"`
  - array: `Array.isArray(a)`

- 예: 문자열 또는 배열 전달 여부에 따라 다른 값을 반환하는 함수

```ts
function wrapInArray(obj: string | string[]) {
  if (typeof obj === "string") {
    return [obj];
    //      ^? (parameter) obj: string
  }
  return obj;
}
```

#### 제네릭

- 제네릭: 타입에 변수를 제공
  - 일반적 예: 배열
  - 제네릭이 없는 배열은 무엇이든 포함 가능 · 제네릭이 있는 배열은 포함 값을 명시 가능

```ts
type StringArray = Array<string>;
type NumberArray = Array<number>;
type ObjectWithNameArray = Array<{ name: string }>;
```

- 제네릭을 사용하는 자체 타입 선언 가능

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

### 구조적 타입 시스템

- TypeScript 핵심 원칙: 타입 검사는 값이 가진 모양에 초점(일명 "덕 타이핑" · "구조적 타이핑")
- 구조적 타입 시스템: 두 객체가 같은 모양이면 같은 타입으로 간주

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

- `point` 변수는 `Point` 타입으로 선언된 적 없음 → TypeScript는 타입 검사 시 `point`의 모양을 `Point`의 모양과 비교 → 같은 모양이므로 통과
- 모양 일치는 객체 필드의 하위 집합만 일치해도 충족

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

- 클래스와 객체가 모양을 따르는 방식에는 차이 없음

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

- 객체·클래스가 필요한 모든 속성을 가지면, TypeScript는 구현 세부 사항과 무관하게 일치로 판단

### 다음 단계

- 위 내용은 일상적인 TypeScript에서 사용되는 문법·도구에 대한 간략한 개요
- 이후 진행 가능한 것
  - 전체 핸드북을 [처음부터 끝까지](/docs/handbook/intro.html) 학습
  - [플레이그라운드 예제](/play#show-examples) 탐색

---

## Java/C# 프로그래머를 위한 TypeScript

> **원문:** https://www.typescriptlang.org/docs/handbook/typescript-in-5-minutes-oop.html

- TypeScript는 C#·Java 등 정적 타이핑 언어에 익숙한 프로그래머에게 인기 있는 선택
- TypeScript 타입 시스템의 이점: 더 나은 코드 완성 · 더 빠른 오류 감지 · 프로그램 부분 간 더 명확한 통신
  - 이런 개발자들에게 많은 익숙한 기능을 제공하지만, JavaScript(및 TypeScript)가 전통적 OOP 언어와 다른 점을 살펴볼 필요 있음
  - 차이점 이해 → 더 나은 JavaScript 코드 작성에 도움, C#/Java에서 바로 넘어온 프로그래머가 빠지기 쉬운 함정 회피

### JavaScript 함께 배우기

- 이미 JavaScript에 익숙하나 주로 Java/C# 프로그래머인 경우 → 이 소개 페이지가 흔한 오해·함정 설명에 도움
  - TypeScript의 타입 모델링 방식은 Java/C#과 상당히 다름 → 학습 시 유의 필요
- Java/C# 프로그래머이면서 JavaScript를 처음 접하는 경우 → 먼저 타입 없이 JavaScript를 배워 런타임 동작을 이해하는 것을 권장
  - TypeScript는 코드 실행 방식을 변경하지 않음 → 실제 동작하는 코드 작성을 위해 JavaScript 동작 원리 학습 필요
- TypeScript는 JavaScript와 동일한 런타임 사용
  - → 특정 런타임 동작(문자열→숫자 변환, 알림 표시, 디스크 파일 쓰기 등) 관련 리소스는 TypeScript 프로그램에도 동일하게 적용
  - TypeScript 전용 리소스로 스스로 제한하지 말 것

### 클래스 다시 생각하기

- C#·Java: "필수 OOP" 언어
  - 클래스 = 코드 구성의 기본 단위 + 런타임의 모든 데이터·동작의 기본 컨테이너
  - 모든 기능·데이터를 클래스에 담는 것이 일부 문제에는 적합하나, 모든 도메인에 필요한 방식은 아님

#### 자유 함수와 데이터

- JavaScript: 함수는 어디에나 존재 가능, 데이터는 미리 정의된 `class`·`struct` 없이 자유롭게 전달 가능
  - 이 유연성이 강력함
  - 암시적 OOP 계층 구조 없이 데이터를 다루는 "자유" 함수(클래스와 무관한 함수)가 JavaScript에서 선호되는 프로그램 작성 모델

#### 정적 클래스

- C#·Java의 싱글톤·정적 클래스 같은 특정 구조는 TypeScript에서 불필요

### TypeScript에서의 OOP

- 원한다면 여전히 클래스 사용 가능
  - 일부 문제는 전통적 OOP 계층 구조로 해결하기에 적합 → TypeScript의 JavaScript 클래스 지원이 이 모델을 더 강력하게 함
  - TypeScript가 지원하는 일반적 패턴: 인터페이스 구현 · 상속 · 정적 메서드
- 클래스에 대한 상세 내용은 이 가이드 뒷부분에서 다룸

### 타입 다시 생각하기

- TypeScript의 "타입" 개념은 C#·Java와 상당히 다름

#### 명목적 구체화 타입 시스템

- C#·Java: 값·객체는 정확히 하나의 타입을 가짐(`null` · 원시 타입 · 알려진 클래스 타입 중 하나)
  - 런타임에 정확한 타입 조회: `value.GetType()` · `value.getClass()` 등 호출
  - 타입 정의는 어딘가의 클래스에 이름으로 존재 → 명시적 상속 관계나 공통 구현 인터페이스가 없으면 모양이 유사한 두 클래스도 서로 대체 사용 불가
- 이는 구체화된·명목적 타입 시스템의 특징
  - 코드에 작성한 타입이 런타임에 존재 · 타입은 구조가 아닌 선언을 통해 관련됨

#### 집합으로서의 타입

- C#·Java: 런타임 타입과 컴파일 타임 선언 간 일대일 대응으로 사고
- TypeScript: 타입을 공통점을 공유하는 값들의 집합으로 사고하는 것이 유용
  - 타입은 집합 → 특정 값이 동시에 여러 집합에 속할 수 있음
- 타입을 집합으로 사고하면 특정 연산이 자연스러워짐
  - 예: C#에서 `string` 또는 `int`인 값을 전달하는 것은 어색함(이런 값을 나타내는 단일 타입이 없기 때문)
  - TypeScript: 모든 타입이 집합이라는 점을 인식하면 자연스러움 → `string` 집합 또는 `number` 집합에 속하는 값 = 두 집합의 유니온 `string | number`
- TypeScript는 집합론적 방식으로 타입을 다루는 여러 메커니즘 제공 → 타입을 집합으로 사고하면 더 직관적

#### 지워지는 구조적 타입

- TypeScript에서 객체는 하나의 정확한 타입이 아님
  - 예: 인터페이스를 만족하는 객체를 구성하면, 둘 사이 선언적 관계가 없어도 해당 인터페이스가 요구되는 곳에서 사용 가능

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

- TypeScript 타입 시스템은 명목적이 아닌 구조적 → `obj`가 숫자인 `x`·`y` 속성을 가지므로 `Pointlike`로 사용 가능
  - 타입 간 관계는 특정 관계로 선언되었는지가 아니라 포함하는 속성으로 결정
- TypeScript 타입 시스템은 구체화되지 않음 → 런타임에 `obj`가 `Pointlike`라고 알려주는 정보 없음
  - `Pointlike` 타입은 런타임에 어떤 형태로도 존재하지 않음
- "집합으로서의 타입" 관점: `obj`는 `Pointlike` 값 집합과 `Named` 값 집합 둘 다의 구성원

#### 구조적 타이핑의 결과

- OOP 프로그래머가 구조적 타이핑에서 흔히 놀라는 두 가지 측면

##### 빈 타입

- 빈 타입이 기대를 거스르는 것처럼 보임

```ts
class Empty {}

function fn(arg: Empty) {
  // 무언가를 하나?
}

// 오류 없음, 하지만 이것은 'Empty'가 아니지 않나?
fn({ k: 10 });
```

- TypeScript는 인수의 유효성을 `{ k: 10 }`과 `class Empty {}`의 구조 검사로 결정
  - `Empty`는 속성이 없으므로 `{ k: 10 }`이 `Empty`의 모든 속성을 가짐 → 유효한 호출
- 놀라울 수 있으나, 명목적 OOP 언어에서 강제하는 것과 매우 유사한 관계
  - 하위 클래스는 기본 클래스의 속성을 제거할 수 없음(제거 시 파생 클래스-기본 클래스 간 자연스러운 하위 타입 관계 붕괴)
  - 구조적 타입 시스템은 호환 가능한 속성을 가지는 것으로 하위 타입을 설명해 이 관계를 암묵적으로 식별

##### 동일한 타입

- 또 다른 놀라움의 원인: 동일한 타입

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

- 이 클래스들의 구조가 같으므로 오류 아님
  - 잠재적 혼란처럼 보일 수 있으나, 실제로 관련되어서는 안 되는 동일한 클래스 구조는 일반적이지 않음
- 클래스 간 관계는 클래스 챕터에서 상세히 다룸

#### 리플렉션

- OOP 프로그래머는 제네릭 타입 포함, 모든 값의 타입을 조회하는 데 익숙

```csharp
// C#
static void LogType<T>() {
    Console.WriteLine(typeof(T).Name);
}
```

- TypeScript 타입 시스템은 완전히 지워짐 → 예를 들어 제네릭 타입 매개변수의 인스턴스화 정보는 런타임에 사용 불가
- JavaScript에는 `typeof`·`instanceof` 같은 제한된 원시 기능 존재
  - 단, 이 연산자들은 타입이 지워진 출력 코드에 존재하는 값에 대해서만 작동
  - 예: `typeof (new Car())`는 `Car`나 `"Car"`가 아니라 `"object"`

### 다음 단계

- 위 내용은 일상적인 TypeScript에서 사용되는 문법·도구에 대한 간략한 개요
- 이후 진행 가능한 것
  - 전체 핸드북을 [처음부터 끝까지](/docs/handbook/intro.html) 학습
  - [플레이그라운드 예제](/play#show-examples) 탐색

---

## 함수형 프로그래머를 위한 TypeScript

> **원문:** https://www.typescriptlang.org/docs/handbook/typescript-in-5-minutes-func.html

- TypeScript의 시작: 전통적인 객체 지향 타입을 JavaScript에 도입 → Microsoft 프로그래머들이 전통적 OOP 프로그램을 웹에 가져올 수 있도록 하려는 시도
  - 발전 과정에서 타입 시스템은 네이티브 JavaScripter들이 작성하는 코드를 모델링하도록 진화
  - 결과: 강력하고 흥미롭고 복잡한 시스템
- 이 소개의 대상: TypeScript를 배우려는 Haskell·ML 프로그래머
  - 내용: TypeScript 타입 시스템과 Haskell 타입 시스템의 차이, JavaScript 코드 모델링에서 발생하는 TypeScript 타입 시스템의 고유 특징
- 이 소개는 객체 지향 프로그래밍을 다루지 않음
  - TypeScript의 OOP 프로그램은 OO 기능을 가진 다른 인기 언어와 유사

### 전제 조건

- 이 소개에서 가정하는 사전 지식
  - JavaScript의 좋은 부분으로 프로그래밍하는 방법
  - C 계열 언어의 타입 문법
- JavaScript의 좋은 부분을 배워야 한다면 [JavaScript: The Good Parts](https://shop.oreilly.com/product/9780596517748.do) 참조
  - 가변성이 많고 그 외에는 값에 의한 호출, 어휘적 스코핑 언어로 프로그램을 작성하는 방법을 이미 안다면 생략 가능
  - 예: [R4RS Scheme](https://people.csail.mit.edu/jaffer/r4rs.pdf)
- C 스타일 타입 문법 학습: [The C++ Programming Language](http://www.stroustrup.com/4th.html) 참조
  - C++과 차이점: TypeScript는 접미사 타입 사용(예: `string x` 대신 `x: string`)

### Haskell에 없는 개념

#### 내장 타입

- JavaScript가 정의하는 8가지 내장 타입
  - `Number`: 배정밀도 IEEE 754 부동소수점
  - `String`: 불변 UTF-16 문자열
  - `BigInt`: 임의 정밀도 형식의 정수
  - `Boolean`: `true`와 `false`
  - `Symbol`: 주로 키로 사용되는 고유한 값
  - `Null`: 유닛 타입과 동등
  - `Undefined`: 역시 유닛 타입과 동등
  - `Object`: 레코드와 유사
  - 상세: [MDN 페이지 참조](https://developer.mozilla.org/docs/Web/JavaScript/Data_structures)
- TypeScript의 대응 원시 타입: `number` · `string` · `bigint` · `boolean` · `symbol` · `null` · `undefined` · `object`

##### 다른 중요한 TypeScript 타입

- `unknown`: 최상위 타입
- `never`: 최하위 타입
- 객체 리터럴: 예 `{ property: Type }`
- `void`: 문서화된 반환 값이 없는 함수용
- `T[]`: 가변 배열(`Array<T>`로도 작성)
- `[T, T]`: 튜플, 고정 길이지만 가변
- `(t: T) => U`: 함수
- 참고
  - 함수 문법은 매개변수 이름을 포함 → 익숙해지기 다소 어려움

```ts
let fst: (a: any, b: any) => any = (a, b) => a;

// 또는 더 정확하게:
let fst: <T, U>(a: T, b: U) => T = (a, b) => a;
```

  - 객체 리터럴 타입 문법은 객체 리터럴 값 문법을 거의 그대로 반영

```ts
let o: { n: number; xs: object[] } = { n: 1, xs: [] };
```

  - `[T, T]`는 `T[]`의 하위 타입 → 튜플이 리스트와 무관한 Haskell과 다른 점

##### 박싱된 타입

- JavaScript: 프로그래머가 원시 타입과 연관시키는 메서드를 포함하는 박싱된 등가물 존재
  - TypeScript도 이를 반영 → 원시 타입 `number`와 박싱된 타입 `Number`를 구분
  - 박싱된 타입은 메서드가 원시 타입을 반환하므로 거의 필요 없음

```ts
(1).toExponential();
// 다음과 동등
Number.prototype.toExponential.call(1);
```

  - 숫자 리터럴에서 메서드 호출 시 파서를 돕기 위해 괄호로 감싸야 함

#### 점진적 타이핑

- TypeScript는 표현식의 타입을 알 수 없을 때 `any` 타입 사용
  - `Dynamic`과 비교해 `any`를 타입이라 부르는 것은 과장 → 나타나는 곳마다 타입 검사기를 끄는 것과 동일
  - 예: 값을 표시하지 않고 `any[]`에 어떤 값이든 푸시 가능

```ts
// tsconfig.json에서 "noImplicitAny": false인 경우, anys: any[]
const anys = [];
anys.push(1);
anys.push("oh no");
anys.push({ anything: "goes" });
```

- `any` 타입 표현식은 어디서든 사용 가능

```ts
anys.map(anys[1]); // oh no, "oh no"는 함수가 아닙니다
```

- `any`는 전염성 있음 → `any` 타입 표현식으로 변수를 초기화하면 그 변수도 `any` 타입을 가짐

```ts
let sepsis = anys[0] + anys[1]; // 이것은 무엇이든 될 수 있습니다
```

- TypeScript가 `any`를 생성할 때 오류를 원하면 `tsconfig.json`에서 `"noImplicitAny": true` 또는 `"strict": true` 설정

#### 구조적 타이핑

- 구조적 타이핑은 대부분의 함수형 프로그래머에게 익숙한 개념(단, Haskell·대부분의 ML은 구조적으로 타이핑되지 않음)
  - 기본 형태는 단순

```ts
// @strict: false
let o = { x: "hi", extra: 1 }; // ok
let o2: { x: string } = o; // ok
```

- 객체 리터럴 `{ x: "hi", extra: 1 }`은 일치하는 리터럴 타입 `{ x: string, extra: number }`를 가짐
  - 필요한 모든 속성이 있고 해당 속성의 타입이 할당 가능 → `{ x: string }`에 할당 가능
  - 추가 속성은 할당을 방해하지 않고 `{ x: string }`의 하위 타입으로 만들 뿐
- 명명된 타입은 타입에 이름을 부여할 뿐 → 할당 가능성 측면에서 타입 별칭 `One`과 인터페이스 타입 `Two` 사이에 차이 없음(둘 다 `p: string` 속성)
  - 예외: 타입 별칭은 재귀적 정의·타입 매개변수 관련해 인터페이스와 다르게 동작

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

#### 유니온

- TypeScript의 유니온 타입은 태그가 없음(Haskell의 `data`처럼 판별 유니온이 아님)
  - 단, 종종 내장 태그나 다른 속성으로 유니온의 타입 구별 가능

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

- `string`·`Array`·`Function`은 내장 타입 조건자 존재 → `else` 분기에는 객체 타입만 남음
  - 단, 런타임에 구별하기 어려운 유니온 생성도 가능 → 새 코드에서는 판별 유니온만 만드는 것이 최선
- 내장 조건자가 있는 타입
  - string: `typeof s === "string"`
  - number: `typeof n === "number"`
  - bigint: `typeof m === "bigint"`
  - boolean: `typeof b === "boolean"`
  - symbol: `typeof g === "symbol"`
  - undefined: `typeof undefined === "undefined"`
  - function: `typeof f === "function"`
  - array: `Array.isArray(a)`
  - object: `typeof o === "object"`
  - 참고: 함수·배열은 런타임에 객체이지만 자체 조건자를 가짐

##### 교차

- 유니온 외에 TypeScript는 교차도 지원

```ts
type Combined = { a: number } & { b: string };
type Conflicting = { a: number } & { a: string };
```

- `Combined`는 하나의 객체 리터럴 타입처럼 `a`·`b` 두 속성을 가짐
  - 교차·유니온은 충돌 시 재귀적으로 처리 → `Conflicting.a: number & string`

#### 유닛 타입

- 유닛 타입: 정확히 하나의 원시 값을 포함하는 원시 타입의 하위 타입
  - 예: 문자열 `"foo"`는 `"foo"` 타입을 가짐
  - JavaScript에는 내장 열거형이 없음 → 잘 알려진 문자열 세트를 대신 사용하는 것이 일반적
  - TypeScript는 문자열 리터럴 타입의 유니온으로 이 패턴을 타입화

```ts
declare function pad(s: string, n: number, direction: "left" | "right"): string;
pad("hi", 10, "left");
```

- 필요 시 컴파일러는 유닛 타입을 원시 타입으로 확장(예: `"foo"` → `string`)
  - 가변성 사용 시 발생 → 가변 변수의 일부 사용을 방해할 수 있음

```ts
let s = "right";
pad("hi", 10, s); // 오류: 'string'은 '"left" | "right"'에 할당할 수 없습니다
// Argument of type 'string' is not assignable to parameter of type '"left" | "right"'.
```

- 오류 발생 과정
  - `"right": "right"`
  - `s: string`(가변 변수에 할당될 때 `"right"`가 `string`으로 확장되기 때문)
  - `string`은 `"left" | "right"`에 할당 불가
- `s`에 타입 어노테이션을 추가하면 해결 가능(단, `"left" | "right"`가 아닌 값의 할당은 막힘)

```ts
let s: "left" | "right" = "right";
pad("hi", 10, s);
```

### Haskell과 유사한 개념

#### 문맥적 타이핑

- TypeScript에서 타입을 추론할 수 있는 명백한 장소: 변수 선언 등

```ts
let s = "I'm a string!";
```

- 다른 C 문법 언어 경험자가 예상하지 못할 몇몇 장소에서도 타입 추론 발생

```ts
declare function map<T, U>(f: (t: T) => U, ts: T[]): U[];
let sns = map((n) => n.toString(), [1, 2, 3]);
```

- 위 코드에서 `n: number`
  - `T`·`U`가 호출 전에 추론되지 않았음에도, `[1,2,3]`이 `T=number`를 추론하는 데 사용된 후 `n => n.toString()`의 반환 타입이 `U=string`을 추론하는 데 사용 → `sns`는 `string[]` 타입
- 추론은 순서 무관하게 작동하나 intellisense는 왼쪽→오른쪽으로만 작동 → TypeScript는 배열을 먼저 두는 `map` 선언을 선호

```ts
declare function map<T, U>(ts: T[], f: (t: T) => U): U[];
```

- 문맥적 타이핑은 객체 리터럴을 통해 재귀적으로 작동, `string`·`number`로 추론될 유닛 타입에도 작동, 문맥에서 반환 타입도 추론 가능

```ts
declare function run<T>(thunk: (t: T) => void): T;
let i: { inference: string } = run((o) => {
  o.inference = "INSERT STATE HERE";
});
```

- `o`의 타입이 `{ inference: string }`으로 결정되는 과정
  - 선언 초기화자는 선언의 타입으로 문맥적 타이핑: `{ inference: string }`
  - 호출의 반환 타입은 추론을 위해 문맥적 타입 사용 → 컴파일러가 `T={ inference: string }` 추론
  - 화살표 함수는 매개변수 타이핑에 문맥적 타입 사용 → 컴파일러가 `o: { inference: string }` 제공
- 타이핑 도중 이 과정이 수행되므로 `o.` 입력 후 `inference` 속성 등에 대한 완성 제공
  - 전체적으로 이 기능은 TypeScript의 추론을 통합 타입 추론 엔진처럼 보이게 하지만, 실제로는 그렇지 않음

#### 타입 별칭

- 타입 별칭: Haskell의 `type`처럼 단순한 별칭
  - 컴파일러는 소스 코드 사용 위치마다 별칭 이름을 사용하려 시도하나 항상 성공하지는 않음

```ts
type Size = [number, number];
let x: Size = [101.1, 999.9];
```

- `newtype`에 가장 가까운 것: 태그된 교차

```ts
type FString = string & { __compileTimeOnly: any };
```

- `FString`은 일반 문자열과 같으나, 컴파일러는 `__compileTimeOnly` 속성이 있다고 간주(실제로는 존재하지 않음)
  - 의미: `FString`은 여전히 `string`에 할당 가능하나 반대는 불가능

#### 판별 유니온

- `data`에 가장 가까운 것: 판별 속성을 가진 타입들의 유니온(TypeScript에서는 보통 판별 유니온)

```ts
type Shape =
  | { kind: "circle"; radius: number }
  | { kind: "square"; x: number }
  | { kind: "triangle"; x: number; y: number };
```

- Haskell과 차이: 태그(구별자)는 각 객체 타입의 단순한 속성
  - 각 변형은 다른 유닛 타입을 가진 동일한 속성을 가짐 → 일반적인 유니온 타입의 하나
  - 맨 앞의 `|`는 유니온 타입 문법의 선택적 부분
  - 일반적인 JavaScript 코드로 유니온 구성원 구별 가능

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

- `area`의 반환 타입이 `number`로 추론됨(TypeScript가 함수가 전체 함수임을 인지하기 때문)
  - 일부 변형이 다뤄지지 않으면 반환 타입은 `number | undefined`가 됨
- Haskell과 또 다른 차이: 공통 속성이 모든 유니온에 나타남 → 유니온의 여러 구성원을 유용하게 구별 가능

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

#### 타입 매개변수

- 대부분의 C 계열 언어처럼 TypeScript는 타입 매개변수의 선언을 요구

```ts
function liftArray<T>(t: T): Array<T> {
  return [t];
}
```

- 대소문자 요구 사항은 없으나 타입 매개변수는 관례적으로 단일 대문자
  - 타입 클래스 제약과 비슷하게 동작하는 타입으로 제한 가능

```ts
function firstish<T extends { length: number }>(t1: T, t2: T): T {
  return t1.length > t2.length ? t1 : t2;
}
```

- TypeScript는 보통 인수의 타입을 기반으로 호출에서 타입 인수를 추론 → 타입 인수 명시는 보통 불필요
- TypeScript는 구조적이므로 명목적 시스템만큼 타입 매개변수가 필요하지 않음
  - 함수를 다형적으로 만드는 데도 불필요
  - 타입 매개변수는 매개변수를 같은 타입으로 제한하는 등 타입 정보를 전파하는 용도로만 사용해야 함

```ts
function length<T extends ArrayLike<unknown>>(t: T): number {}

function length(t: ArrayLike<unknown>): number {}
```

- 첫 번째 `length`의 `T`는 불필요(한 번만 참조 → 반환 값·다른 매개변수 타입 제한에 사용되지 않음)

##### 상위 종류 타입

- TypeScript에는 상위 종류 타입이 없음 → 다음은 유효하지 않음

```ts
function length<T extends ArrayLike<unknown>, U>(m: T<U>) {}
```

##### 포인트-프리 프로그래밍

- 포인트-프리 프로그래밍(커링·함수 합성 다용) 자체는 JavaScript에서 가능하나 장황해질 수 있음
  - TypeScript에서 포인트-프리 프로그램에 대한 타입 추론은 종종 실패 → 값 매개변수 대신 타입 매개변수를 지정하게 됨
  - 결과가 너무 장황해지므로 보통 포인트-프리 프로그래밍은 피하는 것을 권장

#### 모듈 시스템

- JavaScript 현대 모듈 문법은 Haskell과 약간 유사하나, `import`·`export`가 있는 모든 파일은 암묵적으로 모듈

```ts
import { value, Type } from "npm-package";
import { other, Types } from "./local-package";
import * as prefix from "../lib/third-package";
```

- commonjs 모듈(node.js 모듈 시스템)도 가져오기 가능

```ts
import f = require("single-function-package");
```

- export 목록으로 내보내기 가능

```ts
export { f };

function f() {
  return g();
}

function g() {} // g는 내보내지지 않음
```

- 또는 각 export를 개별 표시

```ts
export function f() { return g() }

function g() { }
```

- 후자 스타일이 더 일반적이나 둘 다 허용(같은 파일 내 혼용도 가능)

#### `readonly`와 `const`

- JavaScript는 가변성이 기본이나, `const`로 참조 불변 선언 가능(참조 대상은 여전히 가변)

```ts
const a = [1, 2, 3];
a.push(102); // ):
a[0] = 101; // D:
```

- TypeScript는 추가로 속성용 `readonly` 수정자 지원

```ts
interface Rx {
  readonly x: number;
}
let rx: Rx = { x: 1 };
rx.x = 12; // 오류
```

- 모든 속성을 `readonly`로 만드는 매핑된 타입 `Readonly<T>`도 제공

```ts
interface X {
  x: number;
}
let rx: Readonly<X> = { x: 1 };
rx.x = 12; // 오류
```

- 부작용이 있는 메서드를 제거하고 배열 인덱스 쓰기를 방지하는 `ReadonlyArray<T>` 타입과 전용 문법도 존재

```ts
let a: ReadonlyArray<number> = [1, 2, 3];
let b: readonly number[] = [1, 2, 3];
a.push(102); // 오류
b[0] = 101; // 오류
```

- 배열·객체 리터럴에서 작동하는 const 단언도 사용 가능

```ts
let a = [1, 2, 3] as const;
a.push(102); // 오류
a[0] = 101; // 오류
```

- 단, 이러한 옵션 중 어느 것도 기본값이 아님 → TypeScript 코드에서 일관되게 사용되지는 않음

#### 다음 단계

- 위 내용은 일상적인 코드에서 사용할 문법·타입에 대한 높은 수준의 개요
- 이후 진행 가능한 것
  - 전체 핸드북을 [처음부터 끝까지](/docs/handbook/intro.html) 학습
  - [플레이그라운드 예제](/play#show-examples) 탐색

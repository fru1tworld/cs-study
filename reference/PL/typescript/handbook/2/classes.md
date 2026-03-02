# 클래스

TypeScript는 ES2015에서 도입된 `class` 키워드에 대한 완전한 지원을 제공합니다.

다른 JavaScript 언어 기능과 마찬가지로 TypeScript는 클래스와 다른 타입 간의 관계를 표현할 수 있도록 타입 어노테이션 및 기타 구문을 추가합니다.

## 클래스 멤버

다음은 가장 기본적인 클래스입니다 - 빈 클래스:

```ts twoslash
class Point {}
```

이 클래스는 아직 유용하지 않으므로, 몇 가지 멤버를 추가해 봅시다.

### 필드

필드 선언은 클래스에 공개 쓰기 가능 속성을 생성합니다:

```ts twoslash
// @strictPropertyInitialization: false
class Point {
  x: number;
  y: number;
}

const pt = new Point();
pt.x = 0;
pt.y = 0;
```

다른 위치와 마찬가지로, 타입 어노테이션은 선택 사항이지만, 지정되지 않으면 암시적 `any`가 됩니다.

필드는 _초기화자_도 가질 수 있습니다; 이것들은 클래스가 인스턴스화될 때 자동으로 실행됩니다:

```ts twoslash
class Point {
  x = 0;
  y = 0;
}

const pt = new Point();
// 0, 0을 출력
console.log(`${pt.x}, ${pt.y}`);
```

`const`, `let`, `var`와 마찬가지로, 클래스 속성의 초기화자는 타입을 추론하는 데 사용됩니다:

```ts twoslash
// @errors: 2322
class Point {
  x = 0;
  y = 0;
}
// ---cut---
const pt = new Point();
pt.x = "0";
```

#### `--strictPropertyInitialization`

[`strictPropertyInitialization`](/tsconfig#strictPropertyInitialization) 설정은 클래스 필드가 생성자에서 초기화되어야 하는지를 제어합니다.

```ts twoslash
// @errors: 2564
class BadGreeter {
  name: string;
}
```

```ts twoslash
class GoodGreeter {
  name: string;

  constructor() {
    this.name = "hello";
  }
}
```

필드가 _생성자 자체에서_ 초기화되어야 한다는 점에 주의하세요.
TypeScript는 파생 클래스가 해당 메서드를 재정의하고 멤버를 초기화하지 못할 수 있으므로, 생성자에서 호출하는 메서드를 분석하여 초기화를 감지하지 않습니다.

생성자 외의 수단을 통해 필드를 확실히 초기화하려는 경우(예: 외부 라이브러리가 클래스의 일부를 채워주는 경우), _확정 할당 단언 연산자_ `!`를 사용할 수 있습니다:

```ts twoslash
class OKGreeter {
  // 초기화되지 않았지만, 오류 없음
  name!: string;
}
```

### `readonly`

필드에 `readonly` 수정자를 접두사로 붙일 수 있습니다.
이것은 생성자 외부에서 필드에 대한 할당을 방지합니다.

```ts twoslash
// @errors: 2540 2540
class Greeter {
  readonly name: string = "world";

  constructor(otherName?: string) {
    if (otherName !== undefined) {
      this.name = otherName;
    }
  }

  err() {
    this.name = "not ok";
  }
}
const g = new Greeter();
g.name = "also not ok";
```

### 생성자

클래스 생성자는 함수와 매우 유사합니다.
타입 어노테이션, 기본값 및 오버로드와 함께 매개변수를 추가할 수 있습니다:

```ts twoslash
class Point {
  x: number;
  y: number;

  // 기본값이 있는 일반 시그니처
  constructor(x = 0, y = 0) {
    this.x = x;
    this.y = y;
  }
}
```

```ts twoslash
class Point {
  x: number = 0;
  y: number = 0;

  // 생성자 오버로드
  constructor(x: number, y: number);
  constructor(xy: string);
  constructor(x: string | number, y: number = 0) {
    // 여기에 코드 로직
  }
}
```

클래스 생성자 시그니처와 함수 시그니처 사이에는 몇 가지 차이점이 있습니다:

- 생성자는 타입 매개변수를 가질 수 없습니다 - 이것들은 외부 클래스 선언에 속하며, 나중에 배울 것입니다
- 생성자는 반환 타입 어노테이션을 가질 수 없습니다 - 클래스 인스턴스 타입이 항상 반환되는 것입니다

#### Super 호출

JavaScript에서와 마찬가지로, 기본 클래스가 있으면 `this.` 멤버를 사용하기 전에 생성자 본문에서 `super();`를 호출해야 합니다:

```ts twoslash
// @errors: 17009
class Base {
  k = 4;
}

class Derived extends Base {
  constructor() {
    // ES5에서 잘못된 값을 출력; ES6에서 예외 발생
    console.log(this.k);
    super();
  }
}
```

`super`를 호출하는 것을 잊는 것은 JavaScript에서 쉬운 실수이지만, TypeScript는 필요할 때 알려줍니다.

### 메서드

클래스의 함수 속성을 _메서드_라고 합니다.
메서드는 함수 및 생성자와 동일한 모든 타입 어노테이션을 사용할 수 있습니다:

```ts twoslash
class Point {
  x = 10;
  y = 10;

  scale(n: number): void {
    this.x *= n;
    this.y *= n;
  }
}
```

표준 타입 어노테이션 외에 TypeScript는 메서드에 새로운 것을 추가하지 않습니다.

메서드 본문 내에서 `this.`를 통해 필드 및 기타 메서드에 접근하는 것이 여전히 필수라는 점에 주의하세요.
메서드 본문의 비한정 이름은 항상 둘러싼 범위의 무언가를 참조합니다:

```ts twoslash
// @errors: 2322
let x: number = 0;

class C {
  x: string = "hello";

  m() {
    // 이것은 클래스 속성이 아닌 1행의 'x'를 수정하려고 함
    x = "world";
  }
}
```

### 게터 / 세터

클래스도 _접근자_를 가질 수 있습니다:

```ts twoslash
class C {
  _length = 0;
  get length() {
    return this._length;
  }
  set length(value) {
    this._length = value;
  }
}
```

> 추가 로직이 없는 필드 기반 get/set 쌍은 JavaScript에서 거의 유용하지 않습니다.
> get/set 작업 중에 추가 로직을 추가할 필요가 없다면 공개 필드를 노출하는 것이 좋습니다.

TypeScript에는 접근자에 대한 몇 가지 특별한 추론 규칙이 있습니다:

- `get`이 있지만 `set`이 없으면, 속성이 자동으로 `readonly`입니다
- 세터 매개변수의 타입이 지정되지 않으면, 게터의 반환 타입에서 추론됩니다

[TypeScript 4.3](https://devblogs.microsoft.com/typescript/announcing-typescript-4-3/)부터 가져오기와 설정에 대해 다른 타입을 가진 접근자를 가질 수 있습니다.

```ts twoslash
class Thing {
  _size = 0;

  get size(): number {
    return this._size;
  }

  set size(value: string | number | boolean) {
    let num = Number(value);

    // NaN, Infinity 등을 허용하지 않음

    if (!Number.isFinite(num)) {
      this._size = 0;
      return;
    }

    this._size = num;
  }
}
```

### 인덱스 시그니처

클래스는 인덱스 시그니처를 선언할 수 있습니다; 이것들은 [다른 객체 타입의 인덱스 시그니처](/docs/handbook/2/objects.html#index-signatures)와 같은 방식으로 작동합니다:

```ts twoslash
class MyClass {
  [s: string]: boolean | ((s: string) => boolean);

  check(s: string) {
    return this[s] as boolean;
  }
}
```

인덱스 시그니처 타입이 메서드의 타입도 캡처해야 하므로, 이러한 타입을 유용하게 사용하기는 쉽지 않습니다.
일반적으로 인덱싱된 데이터를 클래스 인스턴스 자체가 아닌 다른 곳에 저장하는 것이 좋습니다.

## 클래스 상속

객체 지향 기능을 가진 다른 언어와 마찬가지로, JavaScript의 클래스는 기본 클래스에서 상속할 수 있습니다.

### `implements` 절

`implements` 절을 사용하여 클래스가 특정 `interface`를 만족하는지 확인할 수 있습니다.
클래스가 올바르게 구현하지 못하면 오류가 발생합니다:

```ts twoslash
// @errors: 2420
interface Pingable {
  ping(): void;
}

class Sonar implements Pingable {
  ping() {
    console.log("ping!");
  }
}

class Ball implements Pingable {
  pong() {
    console.log("pong!");
  }
}
```

클래스는 여러 인터페이스를 구현할 수도 있습니다. 예: `class C implements A, B {`.

#### 주의 사항

`implements` 절은 클래스가 인터페이스 타입으로 처리될 수 있는지에 대한 검사일 뿐이라는 것을 이해하는 것이 중요합니다.
클래스의 타입이나 메서드를 _전혀_ 변경하지 않습니다.
일반적인 오류 원인은 `implements` 절이 클래스 타입을 변경할 것이라고 가정하는 것입니다 - 변경하지 않습니다!

```ts twoslash
// @errors: 7006
interface Checkable {
  check(name: string): boolean;
}

class NameChecker implements Checkable {
  check(s) {
    // 여기서 오류 없음에 주목
    return s.toLowerCase() === "ok";
    //         ^?
  }
}
```

이 예제에서, 아마도 `s`의 타입이 `check`의 `name: string` 매개변수에 의해 영향을 받을 것으로 예상했을 것입니다.
그렇지 않습니다 - `implements` 절은 클래스 본문이 검사되거나 타입이 추론되는 방식을 변경하지 않습니다.

마찬가지로, 선택적 속성이 있는 인터페이스를 구현해도 해당 속성이 생성되지 않습니다:

```ts twoslash
// @errors: 2339
interface A {
  x: number;
  y?: number;
}
class C implements A {
  x = 0;
}
const c = new C();
c.y = 10;
```

### `extends` 절

클래스는 기본 클래스에서 `extend`할 수 있습니다.
파생 클래스는 기본 클래스의 모든 속성과 메서드를 가지며, 추가 멤버도 정의할 수 있습니다.

```ts twoslash
class Animal {
  move() {
    console.log("Moving along!");
  }
}

class Dog extends Animal {
  woof(times: number) {
    for (let i = 0; i < times; i++) {
      console.log("woof!");
    }
  }
}

const d = new Dog();
// 기본 클래스 메서드
d.move();
// 파생 클래스 메서드
d.woof(3);
```

#### 메서드 재정의

파생 클래스는 기본 클래스의 필드나 속성을 재정의할 수도 있습니다.
`super.` 구문을 사용하여 기본 클래스 메서드에 접근할 수 있습니다.
JavaScript 클래스는 간단한 조회 객체이므로, "슈퍼 필드"라는 개념이 없습니다.

TypeScript는 파생 클래스가 항상 기본 클래스의 하위 타입이 되도록 강제합니다.

예를 들어, 다음은 메서드를 재정의하는 합법적인 방법입니다:

```ts twoslash
class Base {
  greet() {
    console.log("Hello, world!");
  }
}

class Derived extends Base {
  greet(name?: string) {
    if (name === undefined) {
      super.greet();
    } else {
      console.log(`Hello, ${name.toUpperCase()}`);
    }
  }
}

const d = new Derived();
d.greet();
d.greet("reader");
```

파생 클래스가 기본 클래스 계약을 따르는 것이 중요합니다.
기본 클래스 참조를 통해 파생 클래스 인스턴스를 참조하는 것은 매우 일반적(그리고 항상 합법적!)입니다:

```ts twoslash
class Base {
  greet() {
    console.log("Hello, world!");
  }
}
class Derived extends Base {}
const d = new Derived();
// ---cut---
// 기본 클래스 참조를 통해 파생 인스턴스를 별칭으로 지정
const b: Base = d;
// 문제 없음
b.greet();
```

`Derived`가 `Base`의 계약을 따르지 않으면 어떻게 될까요?

```ts twoslash
// @errors: 2416
class Base {
  greet() {
    console.log("Hello, world!");
  }
}

class Derived extends Base {
  // 이 매개변수를 필수로 만듦
  greet(name: string) {
    console.log(`Hello, ${name.toUpperCase()}`);
  }
}
```

오류에도 불구하고 이 코드를 컴파일하면, 이 샘플은 충돌합니다:

```ts twoslash
declare class Base {
  greet(): void;
}
declare class Derived extends Base {}
// ---cut---
const b: Base = new Derived();
// "name"이 undefined가 되어 충돌
b.greet();
```

#### 타입 전용 필드 선언

`target >= ES2022` 또는 [`useDefineForClassFields`](/tsconfig#useDefineForClassFields)가 `true`이면, 클래스 필드는 부모 클래스 생성자가 완료된 후 초기화되어 부모 클래스가 설정한 값을 덮어씁니다. 이것은 상속된 필드에 대해 더 정확한 타입만 다시 선언하고 싶을 때 문제가 될 수 있습니다. 이러한 경우를 처리하려면, 이 필드 선언에 대한 런타임 효과가 없어야 함을 TypeScript에 나타내기 위해 `declare`를 작성할 수 있습니다.

```ts twoslash
interface Animal {
  dateOfBirth: any;
}

interface Dog extends Animal {
  breed: any;
}

class AnimalHouse {
  resident: Animal;
  constructor(animal: Animal) {
    this.resident = animal;
  }
}

class DogHouse extends AnimalHouse {
  // JavaScript 코드를 내보내지 않음,
  // 타입이 올바른지만 확인
  declare resident: Dog;
  constructor(dog: Dog) {
    super(dog);
  }
}
```

#### 초기화 순서

JavaScript 클래스가 초기화되는 순서는 일부 경우에 놀라울 수 있습니다.
다음 코드를 생각해 봅시다:

```ts twoslash
class Base {
  name = "base";
  constructor() {
    console.log("My name is " + this.name);
  }
}

class Derived extends Base {
  name = "derived";
}

// "derived"가 아닌 "base"를 출력
const d = new Derived();
```

여기서 무슨 일이 일어났나요?

JavaScript에 의해 정의된 클래스 초기화 순서는:

- 기본 클래스 필드가 초기화됨
- 기본 클래스 생성자가 실행됨
- 파생 클래스 필드가 초기화됨
- 파생 클래스 생성자가 실행됨

이것은 파생 클래스 필드 초기화가 아직 실행되지 않았기 때문에 기본 클래스 생성자가 자체 생성자 중에 `name`에 대해 자체 값을 보았다는 것을 의미합니다.

#### 내장 타입 상속

> 참고: `Array`, `Error`, `Map` 등과 같은 내장 타입에서 상속할 계획이 없거나 컴파일 대상이 명시적으로 `ES6`/`ES2015` 이상으로 설정된 경우, 이 섹션을 건너뛸 수 있습니다.

ES2015에서 객체를 반환하는 생성자는 `super(...)`의 모든 호출자에 대해 `this` 값을 암시적으로 대체합니다.
생성된 생성자 코드가 `super(...)`의 잠재적 반환 값을 캡처하고 `this`로 대체해야 합니다.

결과적으로, `Error`, `Array` 등의 서브클래싱이 더 이상 예상대로 작동하지 않을 수 있습니다.
이것은 `Error`, `Array` 등의 생성자 함수가 ECMAScript 6의 `new.target`을 사용하여 프로토타입 체인을 조정하기 때문입니다;
그러나 ECMAScript 5에서 생성자를 호출할 때 `new.target`에 대한 값을 보장할 방법이 없습니다.
다른 다운레벨 컴파일러도 일반적으로 기본적으로 같은 제한이 있습니다.

다음과 같은 서브클래스의 경우:

```ts twoslash
class MsgError extends Error {
  constructor(m: string) {
    super(m);
  }
  sayHello() {
    return "hello " + this.message;
  }
}
```

다음을 발견할 수 있습니다:

- 이러한 서브클래스를 생성하여 반환된 객체에서 메서드가 `undefined`일 수 있으므로, `sayHello`를 호출하면 오류가 발생합니다.
- 서브클래스의 인스턴스와 해당 인스턴스 사이에서 `instanceof`가 깨지므로, `(new MsgError()) instanceof MsgError`가 `false`를 반환합니다.

권장 사항으로, 모든 `super(...)` 호출 직후에 프로토타입을 수동으로 조정할 수 있습니다.

```ts twoslash
class MsgError extends Error {
  constructor(m: string) {
    super(m);

    // 명시적으로 프로토타입 설정.
    Object.setPrototypeOf(this, MsgError.prototype);
  }

  sayHello() {
    return "hello " + this.message;
  }
}
```

그러나 `MsgError`의 모든 서브클래스도 프로토타입을 수동으로 설정해야 합니다.
[`Object.setPrototypeOf`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/setPrototypeOf)를 지원하지 않는 런타임의 경우, 대신 [`__proto__`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/proto)를 사용할 수 있습니다.

안타깝게도, [이러한 해결 방법은 Internet Explorer 10 및 이전 버전에서 작동하지 않습니다](https://msdn.microsoft.com/en-us/library/s4esdbwz(v=vs.94).aspx).
프로토타입에서 인스턴스 자체로 메서드를 수동으로 복사할 수 있지만(예: `MsgError.prototype`을 `this`로), 프로토타입 체인 자체는 수정할 수 없습니다.

## 멤버 가시성

TypeScript를 사용하여 특정 메서드나 속성이 클래스 외부의 코드에 표시되는지 여부를 제어할 수 있습니다.

### `public`

클래스 멤버의 기본 가시성은 `public`입니다.
`public` 멤버는 어디서든 접근할 수 있습니다:

```ts twoslash
class Greeter {
  public greet() {
    console.log("hi!");
  }
}
const g = new Greeter();
g.greet();
```

`public`이 이미 기본 가시성 수정자이므로, 클래스 멤버에 작성할 _필요_가 없지만, 스타일/가독성을 위해 선택할 수 있습니다.

### `protected`

`protected` 멤버는 선언된 클래스의 서브클래스에서만 볼 수 있습니다.

```ts twoslash
// @errors: 2445
class Greeter {
  public greet() {
    console.log("Hello, " + this.getName());
  }
  protected getName() {
    return "hi";
  }
}

class SpecialGreeter extends Greeter {
  public howdy() {
    // 여기서 protected 멤버에 접근 OK
    console.log("Howdy, " + this.getName());
    //                          ^^^^^^^^^^^^^^
  }
}
const g = new SpecialGreeter();
g.greet(); // OK
g.getName();
```

#### `protected` 멤버 노출

파생 클래스는 기본 클래스 계약을 따라야 하지만, 더 많은 기능을 가진 기본 클래스의 하위 타입을 노출하도록 선택할 수 있습니다.
여기에는 `protected` 멤버를 `public`으로 만드는 것이 포함됩니다:

```ts twoslash
class Base {
  protected m = 10;
}
class Derived extends Base {
  // 수정자 없음, 기본값은 'public'
  m = 15;
}
const d = new Derived();
console.log(d.m); // OK
```

`Derived`가 이미 `m`을 자유롭게 읽고 쓸 수 있었으므로, 이것은 이 상황의 "보안"을 의미 있게 변경하지 않습니다.
여기서 주목할 주요 사항은 파생 클래스에서 이 노출이 의도적이지 않은 경우 `protected` 수정자를 반복하도록 주의해야 한다는 것입니다.

#### 계층 간 `protected` 접근

TypeScript는 클래스 계층에서 형제 클래스의 `protected` 멤버에 접근하는 것을 허용하지 않습니다:

```ts twoslash
// @errors: 2446
class Base {
  protected x: number = 1;
}
class Derived1 extends Base {
  protected x: number = 5;
}
class Derived2 extends Base {
  f1(other: Derived2) {
    other.x = 10;
  }
  f2(other: Derived1) {
    other.x = 10;
  }
}
```

이것은 `Derived2`에서 `x`에 접근하는 것이 `Derived2`의 서브클래스에서만 합법적이어야 하고, `Derived1`은 그중 하나가 아니기 때문입니다.
또한, `Derived1` 참조를 통해 `x`에 접근하는 것이 불법이라면(확실히 그래야 합니다!), 기본 클래스 참조를 통해 접근하는 것도 상황을 개선하지 않아야 합니다.

### `private`

`private`은 `protected`와 같지만, 서브클래스에서도 멤버에 대한 접근을 허용하지 않습니다:

```ts twoslash
// @errors: 2341
class Base {
  private x = 0;
}
const b = new Base();
// 클래스 외부에서 접근 불가
console.log(b.x);
```

```ts twoslash
// @errors: 2341
class Base {
  private x = 0;
}
// ---cut---
class Derived extends Base {
  showX() {
    // 서브클래스에서 접근 불가
    console.log(this.x);
  }
}
```

`private` 멤버는 파생 클래스에 표시되지 않으므로, 파생 클래스는 가시성을 높일 수 없습니다:

```ts twoslash
// @errors: 2415
class Base {
  private x = 0;
}
class Derived extends Base {
  x = 1;
}
```

#### 인스턴스 간 `private` 접근

다른 OOP 언어는 같은 클래스의 다른 인스턴스가 서로의 `private` 멤버에 접근할 수 있는지에 대해 의견이 다릅니다.
Java, C#, C++, Swift, PHP와 같은 언어는 이를 허용하지만, Ruby는 허용하지 않습니다.

TypeScript는 인스턴스 간 `private` 접근을 허용합니다:

```ts twoslash
class A {
  private x = 10;

  public sameAs(other: A) {
    // 오류 없음
    return other.x === this.x;
  }
}
```

#### 주의 사항

TypeScript 타입 시스템의 다른 측면과 마찬가지로, `private`과 `protected`는 [타입 검사 중에만 적용됩니다](https://www.typescriptlang.org/play?removeComments=true&target=99&ts=4.3.4#code/PTAEGMBsEMGddAEQPYHNQBMCmVoCcsEAHPASwDdoAXLUAM1K0gwQFdZSA7dAKWkoDK4MkSoByBAGJQJLAwAeAWABQIUH0HDSoiTLKUaoUggAW+DHorUsAOlABJcQlhUy4KpACeoLJzrI8cCwMGxU1ABVPIiwhESpMZEJQTmR4lxFQaQxWMm4IZABbIlIYKlJkTlDlXHgkNFAAbxVQTIAjfABrAEEC5FZOeIBeUAAGAG5mmSw8WAroSFIqb2GAIjMiIk8VieVJ8Ar01ncAgAoASkaAXxVr3dUwGoQAYWpMHBgCYn1rekZmNg4eUi0Vi2icoBWJCsNBWoA6WE8AHcAiEwmBgTEtDovtDaMZQLM6PEoQZbA5wSk0q5SO4vD4-AEghZoJwLGYEIRwNBoqAzFRwCZCFUIlFMXECdSiAhId8YZgclx0PsiiVqOVOAAaUAFLAsxWgKiC35MFigfC0FKgSAVVDTSyk+W5dB4fplHVVR6gF7xJrKFotEk-HXIRE9PoDUDDcaTAPTWaceaLZYQlmoPBbHYx-KcQ7HPDnK43FQqfY5+IMDDISPJLCIuqoc47UsuUCofAME3Vzi1r3URvF5QV5A2STtPDdXqunZDgDaYlHnTDrrEAF0dm28B3mDZg6HJwN1+2-hg57ulwNV2NQGoZbjYfNrYiENBwEFaojFiZQK08C-4fFKTVCozWfTgfFgLkeT5AUqiAA).

이것은 `in`이나 간단한 속성 조회와 같은 JavaScript 런타임 구문이 여전히 `private` 또는 `protected` 멤버에 접근할 수 있다는 것을 의미합니다:

```ts twoslash
class MySafe {
  private secretKey = 12345;
}
```

```js
// JavaScript 파일에서...
const s = new MySafe();
// 12345를 출력
console.log(s.secretKey);
```

`private`은 또한 타입 검사 중에 대괄호 표기법을 사용한 접근을 허용합니다. 이것은 `private`으로 선언된 필드를 유닛 테스트와 같은 것에 대해 잠재적으로 더 쉽게 접근할 수 있게 하지만, 이러한 필드가 _소프트 private_이며 프라이버시를 엄격하게 적용하지 않는다는 단점이 있습니다.

```ts twoslash
// @errors: 2341
class MySafe {
  private secretKey = 12345;
}

const s = new MySafe();

// 타입 검사 중 허용되지 않음
console.log(s.secretKey);

// OK
console.log(s["secretKey"]);
```

TypeScript의 `private`과 달리, JavaScript의 [private 필드](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Classes/Private_class_fields)(`#`)는 컴파일 후에도 private으로 유지되며 대괄호 표기법 접근과 같은 앞서 언급한 탈출구를 제공하지 않아 _하드 private_입니다.

```ts twoslash
class Dog {
  #barkAmount = 0;
  personality = "happy";

  constructor() {}
}
```

ES2021 이하로 컴파일할 때, TypeScript는 `#` 대신 WeakMap을 사용합니다.

악의적인 행위자로부터 클래스의 값을 보호해야 하는 경우, 클로저, WeakMap 또는 private 필드와 같은 하드 런타임 프라이버시를 제공하는 메커니즘을 사용해야 합니다. 이러한 추가된 프라이버시 검사는 런타임 중 성능에 영향을 미칠 수 있습니다.

## 정적 멤버

클래스는 `static` 멤버를 가질 수 있습니다.
이러한 멤버는 클래스의 특정 인스턴스와 연관되지 않습니다.
클래스 생성자 객체 자체를 통해 접근할 수 있습니다:

```ts twoslash
class MyClass {
  static x = 0;
  static printX() {
    console.log(MyClass.x);
  }
}
console.log(MyClass.x);
MyClass.printX();
```

정적 멤버도 같은 `public`, `protected`, `private` 가시성 수정자를 사용할 수 있습니다:

```ts twoslash
// @errors: 2341
class MyClass {
  private static x = 0;
}
console.log(MyClass.x);
```

정적 멤버도 상속됩니다:

```ts twoslash
class Base {
  static getGreeting() {
    return "Hello world";
  }
}
class Derived extends Base {
  myGreeting = Derived.getGreeting();
}
```

### 특수 정적 이름

`Function` 프로토타입의 속성을 덮어쓰는 것은 일반적으로 안전하지 않거나 불가능합니다.
클래스 자체가 `new`로 호출할 수 있는 함수이므로, 특정 `static` 이름은 사용할 수 없습니다.
`name`, `length`, `call`과 같은 함수 속성은 `static` 멤버로 정의하기에 유효하지 않습니다:

```ts twoslash
// @errors: 2699
class S {
  static name = "S!";
}
```

### 왜 정적 클래스가 없나요?

TypeScript(및 JavaScript)에는 예를 들어 C#처럼 `static class`라는 구문이 없습니다.

이러한 구문은 해당 언어가 모든 데이터와 함수를 클래스 내부에 강제하기 때문에 _존재_합니다; TypeScript에는 그러한 제한이 없으므로, 필요하지 않습니다.
단일 인스턴스만 있는 클래스는 일반적으로 JavaScript/TypeScript에서 일반 _객체_로 표현됩니다.

예를 들어, 일반 객체(또는 최상위 함수)가 동일하게 작업을 수행하므로 TypeScript에서 "정적 클래스" 구문이 필요하지 않습니다:

```ts twoslash
// 불필요한 "정적" 클래스
class MyStaticClass {
  static doSomething() {}
}

// 선호됨 (대안 1)
function doSomething() {}

// 선호됨 (대안 2)
const MyHelperObject = {
  dosomething() {},
};
```

## 클래스의 `static` 블록

정적 블록을 사용하면 포함하는 클래스 내의 private 필드에 접근할 수 있는 자체 범위를 가진 일련의 명령문을 작성할 수 있습니다. 이것은 명령문 작성의 모든 기능, 변수 누출 없음, 클래스 내부에 대한 완전한 접근으로 초기화 코드를 작성할 수 있다는 것을 의미합니다.

```ts twoslash
declare function loadLastInstances(): any[]
// ---cut---
class Foo {
    static #count = 0;

    get count() {
        return Foo.#count;
    }

    static {
        try {
            const lastInstances = loadLastInstances();
            Foo.#count += lastInstances.length;
        }
        catch {}
    }
}
```

## 제네릭 클래스

인터페이스와 마찬가지로 클래스도 제네릭일 수 있습니다.
제네릭 클래스가 `new`로 인스턴스화될 때, 타입 매개변수는 함수 호출에서와 같은 방식으로 추론됩니다:

```ts twoslash
class Box<Type> {
  contents: Type;
  constructor(value: Type) {
    this.contents = value;
  }
}

const b = new Box("hello!");
//    ^?
```

클래스는 인터페이스와 같은 방식으로 제네릭 제약 조건과 기본값을 사용할 수 있습니다.

### 정적 멤버의 타입 매개변수

이 코드는 합법적이지 않으며, 왜 그런지 명확하지 않을 수 있습니다:

```ts twoslash
// @errors: 2302
class Box<Type> {
  static defaultValue: Type;
}
```

타입은 항상 완전히 지워진다는 것을 기억하세요!
런타임에 `Box.defaultValue` 속성 슬롯은 _하나_뿐입니다.
이것은 `Box<string>.defaultValue`를 설정하면(가능하다면) `Box<number>.defaultValue`도 _변경_한다는 것을 의미합니다 - 좋지 않습니다.
제네릭 클래스의 `static` 멤버는 클래스의 타입 매개변수를 참조할 수 없습니다.

## 클래스에서의 런타임 `this`

TypeScript가 JavaScript의 런타임 동작을 변경하지 않으며, JavaScript는 다소 독특한 런타임 동작을 가지고 있다는 것을 기억하는 것이 중요합니다.

JavaScript의 `this` 처리는 실제로 특이합니다:

```ts twoslash
class MyClass {
  name = "MyClass";
  getName() {
    return this.name;
  }
}
const c = new MyClass();
const obj = {
  name: "obj",
  getName: c.getName,
};

// "MyClass"가 아닌 "obj"를 출력
console.log(obj.getName());
```

간단히 말해서, 기본적으로 함수 내부의 `this` 값은 _함수가 어떻게 호출되었는지_에 따라 달라집니다.
이 예제에서 함수가 `obj` 참조를 통해 호출되었으므로, `this` 값은 클래스 인스턴스가 아닌 `obj`였습니다.

이것은 거의 원하지 않는 일입니다!
TypeScript는 이런 종류의 오류를 완화하거나 방지하는 몇 가지 방법을 제공합니다.

### 화살표 함수

`this` 컨텍스트를 잃는 방식으로 자주 호출되는 함수가 있다면, 메서드 정의 대신 화살표 함수 속성을 사용하는 것이 의미가 있을 수 있습니다:

```ts twoslash
class MyClass {
  name = "MyClass";
  getName = () => {
    return this.name;
  };
}
const c = new MyClass();
const g = c.getName;
// 충돌 대신 "MyClass"를 출력
console.log(g());
```

이것에는 몇 가지 트레이드오프가 있습니다:

- `this` 값은 TypeScript로 검사되지 않은 코드에서도 런타임에 올바름이 보장됩니다
- 각 클래스 인스턴스가 이런 방식으로 정의된 각 함수의 자체 복사본을 가지므로 더 많은 메모리를 사용합니다
- 파생 클래스에서 `super.getName`을 사용할 수 없습니다. 프로토타입 체인에 기본 클래스 메서드를 가져올 항목이 없기 때문입니다

### `this` 매개변수

메서드나 함수 정의에서 `this`라는 이름의 초기 매개변수는 TypeScript에서 특별한 의미를 가집니다.
이러한 매개변수는 컴파일 중에 지워집니다:

```ts twoslash
type SomeType = any;
// ---cut---
// 'this' 매개변수가 있는 TypeScript 입력
function fn(this: SomeType, x: number) {
  /* ... */
}
```

```js
// JavaScript 출력
function fn(x) {
  /* ... */
}
```

TypeScript는 `this` 매개변수가 있는 함수가 올바른 컨텍스트로 호출되는지 검사합니다.
화살표 함수를 사용하는 대신, 메서드 정의에 `this` 매개변수를 추가하여 메서드가 올바르게 호출되도록 정적으로 적용할 수 있습니다:

```ts twoslash
// @errors: 2684
class MyClass {
  name = "MyClass";
  getName(this: MyClass) {
    return this.name;
  }
}
const c = new MyClass();
// OK
c.getName();

// 오류, 충돌할 것
const g = c.getName;
console.log(g());
```

이 메서드는 화살표 함수 접근 방식의 반대 트레이드오프를 만듭니다:

- JavaScript 호출자는 여전히 인식하지 못하고 클래스 메서드를 잘못 사용할 수 있습니다
- 클래스 인스턴스당 하나가 아닌 클래스 정의당 하나의 함수만 할당됩니다
- 기본 메서드 정의는 여전히 `super`를 통해 호출할 수 있습니다

## `this` 타입

클래스에서 `this`라는 특별한 타입은 현재 클래스의 타입을 _동적으로_ 참조합니다.
이것이 어떻게 유용한지 봅시다:

```ts twoslash
class Box {
  contents: string = "";
  set(value: string) {
//  ^?
    this.contents = value;
    return this;
  }
}
```

여기서 TypeScript는 `set`의 반환 타입을 `Box`가 아닌 `this`로 추론했습니다.
이제 `Box`의 서브클래스를 만들어 봅시다:

```ts twoslash
class Box {
  contents: string = "";
  set(value: string) {
    this.contents = value;
    return this;
  }
}
// ---cut---
class ClearableBox extends Box {
  clear() {
    this.contents = "";
  }
}

const a = new ClearableBox();
const b = a.set("hello");
//    ^?
```

매개변수 타입 어노테이션에서도 `this`를 사용할 수 있습니다:

```ts twoslash
class Box {
  content: string = "";
  sameAs(other: this) {
    return other.content === this.content;
  }
}
```

이것은 `other: Box`를 작성하는 것과 다릅니다 -- 파생 클래스가 있는 경우, `sameAs` 메서드는 이제 같은 파생 클래스의 다른 인스턴스만 받아들입니다:

```ts twoslash
// @errors: 2345
class Box {
  content: string = "";
  sameAs(other: this) {
    return other.content === this.content;
  }
}

class DerivedBox extends Box {
  otherContent: string = "?";
}

const base = new Box();
const derived = new DerivedBox();
derived.sameAs(base);
```

### `this` 기반 타입 가드

클래스와 인터페이스의 메서드에서 반환 위치에 `this is Type`을 사용할 수 있습니다.
타입 좁히기(예: `if` 문)와 혼합하면 대상 객체의 타입이 지정된 `Type`으로 좁혀집니다.

```ts twoslash
// @strictPropertyInitialization: false
class FileSystemObject {
  isFile(): this is FileRep {
    return this instanceof FileRep;
  }
  isDirectory(): this is Directory {
    return this instanceof Directory;
  }
  isNetworked(): this is Networked & this {
    return this.networked;
  }
  constructor(public path: string, private networked: boolean) {}
}

class FileRep extends FileSystemObject {
  constructor(path: string, public content: string) {
    super(path, false);
  }
}

class Directory extends FileSystemObject {
  children: FileSystemObject[];
}

interface Networked {
  host: string;
}

const fso: FileSystemObject = new FileRep("foo/bar.txt", "foo");

if (fso.isFile()) {
  fso.content;
// ^?
} else if (fso.isDirectory()) {
  fso.children;
// ^?
} else if (fso.isNetworked()) {
  fso.host;
// ^?
}
```

this 기반 타입 가드의 일반적인 사용 사례는 특정 필드의 지연 유효성 검사를 허용하는 것입니다. 예를 들어, 이 경우 `hasValue`가 true로 확인되면 box 내부에 있는 값에서 `undefined`를 제거합니다:

```ts twoslash
class Box<T> {
  value?: T;

  hasValue(): this is { value: T } {
    return this.value !== undefined;
  }
}

const box = new Box<string>();
box.value = "Gameboy";

box.value;
//  ^?

if (box.hasValue()) {
  box.value;
  //  ^?
}
```

## 매개변수 속성

TypeScript는 생성자 매개변수를 같은 이름과 값을 가진 클래스 속성으로 바꾸는 특별한 구문을 제공합니다.
이것들을 _매개변수 속성_이라고 하며, 생성자 인수에 가시성 수정자 `public`, `private`, `protected`, 또는 `readonly` 중 하나를 접두사로 붙여 생성합니다.
결과 필드는 해당 수정자를 얻습니다:

```ts twoslash
// @errors: 2341
class Params {
  constructor(
    public readonly x: number,
    protected y: number,
    private z: number
  ) {
    // 본문 필요 없음
  }
}
const a = new Params(1, 2, 3);
console.log(a.x);
//            ^?
console.log(a.z);
```

## 클래스 표현식

클래스 표현식은 클래스 선언과 매우 유사합니다.
유일한 실제 차이점은 클래스 표현식은 이름이 필요하지 않지만, 결국 바인딩된 식별자를 통해 참조할 수 있다는 것입니다:

```ts twoslash
const someClass = class<Type> {
  content: Type;
  constructor(value: Type) {
    this.content = value;
  }
};

const m = new someClass("Hello, world");
//    ^?
```

## 생성자 시그니처

JavaScript 클래스는 `new` 연산자로 인스턴스화됩니다. 클래스 자체의 타입이 주어지면, [InstanceType](/docs/handbook/utility-types.html#instancetypetype) 유틸리티 타입이 이 작업을 모델링합니다.

```ts twoslash
class Point {
  createdAt: number;
  x: number;
  y: number
  constructor(x: number, y: number) {
    this.createdAt = Date.now()
    this.x = x;
    this.y = y;
  }
}
type PointInstance = InstanceType<typeof Point>

function moveRight(point: PointInstance) {
  point.x += 5;
}

const point = new Point(3, 4);
moveRight(point);
point.x; // => 8
```

## `abstract` 클래스와 멤버

TypeScript의 클래스, 메서드 및 필드는 _abstract_일 수 있습니다.

_추상 메서드_ 또는 _추상 필드_는 구현이 제공되지 않은 것입니다.
이러한 멤버는 직접 인스턴스화할 수 없는 _추상 클래스_ 내에 존재해야 합니다.

추상 클래스의 역할은 모든 추상 멤버를 구현하는 서브클래스의 기본 클래스 역할을 하는 것입니다.
클래스에 추상 멤버가 없으면 _구체적_이라고 합니다.

예제를 살펴봅시다:

```ts twoslash
// @errors: 2511
abstract class Base {
  abstract getName(): string;

  printName() {
    console.log("Hello, " + this.getName());
  }
}

const b = new Base();
```

`Base`가 추상이므로 `new`로 인스턴스화할 수 없습니다.
대신, 파생 클래스를 만들고 추상 멤버를 구현해야 합니다:

```ts twoslash
abstract class Base {
  abstract getName(): string;
  printName() {}
}
// ---cut---
class Derived extends Base {
  getName() {
    return "world";
  }
}

const d = new Derived();
d.printName();
```

기본 클래스의 추상 멤버를 구현하지 않으면 오류가 발생합니다:

```ts twoslash
// @errors: 2515
abstract class Base {
  abstract getName(): string;
  printName() {}
}
// ---cut---
class Derived extends Base {
  // 아무것도 하는 것을 잊음
}
```

### 추상 생성 시그니처

때때로 어떤 추상 클래스에서 파생된 클래스의 인스턴스를 생성하는 일부 클래스 생성자 함수를 받아들이고 싶습니다.

예를 들어, 다음 코드를 작성하고 싶을 수 있습니다:

```ts twoslash
// @errors: 2511
abstract class Base {
  abstract getName(): string;
  printName() {}
}
class Derived extends Base {
  getName() {
    return "";
  }
}
// ---cut---
function greet(ctor: typeof Base) {
  const instance = new ctor();
  instance.printName();
}
```

TypeScript는 추상 클래스를 인스턴스화하려고 한다고 올바르게 알려줍니다.
결국, `greet`의 정의가 주어지면, 추상 클래스를 생성하게 될 이 코드를 작성하는 것이 완전히 합법적입니다:

```ts twoslash
declare const greet: any, Base: any;
// ---cut---
// 나쁨!
greet(Base);
```

대신, 생성 시그니처가 있는 것을 받아들이는 함수를 작성하고 싶습니다:

```ts twoslash
// @errors: 2345
abstract class Base {
  abstract getName(): string;
  printName() {}
}
class Derived extends Base {
  getName() {
    return "";
  }
}
// ---cut---
function greet(ctor: new () => Base) {
  const instance = new ctor();
  instance.printName();
}
greet(Derived);
greet(Base);
```

이제 TypeScript는 어떤 클래스 생성자 함수를 호출할 수 있는지 올바르게 알려줍니다 - `Derived`는 구체적이므로 가능하지만, `Base`는 불가능합니다.

## 클래스 간의 관계

대부분의 경우, TypeScript의 클래스는 다른 타입과 같이 구조적으로 비교됩니다.

예를 들어, 이 두 클래스는 동일하기 때문에 서로 대신 사용할 수 있습니다:

```ts twoslash
class Point1 {
  x = 0;
  y = 0;
}

class Point2 {
  x = 0;
  y = 0;
}

// OK
const p: Point1 = new Point2();
```

마찬가지로, 명시적 상속이 없어도 클래스 간의 하위 타입 관계가 존재합니다:

```ts twoslash
// @strict: false
class Person {
  name: string;
  age: number;
}

class Employee {
  name: string;
  age: number;
  salary: number;
}

// OK
const p: Person = new Employee();
```

이것은 간단하게 들리지만, 다른 것보다 더 이상하게 보이는 몇 가지 경우가 있습니다.

빈 클래스에는 멤버가 없습니다.
구조적 타입 시스템에서 멤버가 없는 타입은 일반적으로 다른 모든 것의 슈퍼타입입니다.
따라서 빈 클래스를 작성하면(하지 마세요!), 어떤 것이든 그 자리에 사용할 수 있습니다:

```ts twoslash
class Empty {}

function fn(x: Empty) {
  // 'x'로 아무것도 할 수 없으므로, 하지 않겠습니다
}

// 모두 OK!
fn(window);
fn({});
fn(fn);
```

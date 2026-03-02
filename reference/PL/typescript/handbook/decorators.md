# 데코레이터 (Decorators)

> **참고**&nbsp; 이 문서는 실험적인 stage 2 데코레이터 구현을 다룹니다. Stage 3 데코레이터 지원은 TypeScript 5.0부터 사용할 수 있습니다.
> 참조: [TypeScript 5.0의 데코레이터](https://devblogs.microsoft.com/typescript/announcing-typescript-5-0/#decorators)

## 소개

TypeScript와 ES6에서 클래스가 도입되면서, 이제 클래스 및 클래스 멤버에 주석을 달거나 수정하는 것을 지원하기 위한 추가 기능이 필요한 특정 시나리오가 존재합니다.
데코레이터는 클래스 선언과 멤버에 대한 주석과 메타프로그래밍 구문을 추가하는 방법을 제공합니다.

> 추가 읽기 (stage 2): [TypeScript 데코레이터 완벽 가이드](https://saul-mirone.github.io/a-complete-guide-to-typescript-decorator/)

데코레이터에 대한 실험적 지원을 활성화하려면, 커맨드 라인이나 `tsconfig.json`에서 [`experimentalDecorators`](/tsconfig#experimentalDecorators) 컴파일러 옵션을 활성화해야 합니다:

**커맨드 라인**:

```shell
tsc --target ES5 --experimentalDecorators
```

**tsconfig.json**:

```json
{
  "compilerOptions": {
    "target": "ES5",
    "experimentalDecorators": true
  }
}
```

## 데코레이터

_데코레이터_는 [클래스 선언](#클래스-데코레이터), [메서드](#메서드-데코레이터), [접근자](#접근자-데코레이터), [프로퍼티](#프로퍼티-데코레이터), 또는 [매개변수](#매개변수-데코레이터)에 첨부할 수 있는 특별한 종류의 선언입니다.
데코레이터는 `@expression` 형식을 사용하며, 여기서 `expression`은 데코레이트된 선언에 대한 정보와 함께 런타임에 호출될 함수로 평가되어야 합니다.

예를 들어, 데코레이터 `@sealed`가 주어지면 다음과 같이 `sealed` 함수를 작성할 수 있습니다:

```ts
function sealed(target) {
  // 'target'으로 무언가를 합니다...
}
```

## 데코레이터 팩토리

데코레이터가 선언에 적용되는 방식을 커스터마이즈하려면, 데코레이터 팩토리를 작성할 수 있습니다.
_데코레이터 팩토리_는 런타임에 데코레이터에 의해 호출될 표현식을 반환하는 간단한 함수입니다.

다음과 같은 방식으로 데코레이터 팩토리를 작성할 수 있습니다:

```ts
function color(value: string) {
  // 이것은 데코레이터 팩토리이며,
  // 반환된 데코레이터 함수를 설정합니다
  return function (target) {
    // 이것이 데코레이터입니다
    // 'target'과 'value'로 무언가를 합니다...
  };
}
```

## 데코레이터 합성

여러 데코레이터를 선언에 적용할 수 있습니다. 예를 들어 한 줄에:

```ts
@f @g x
```

여러 줄에:

```ts
@f
@g
x
```

여러 데코레이터가 단일 선언에 적용될 때, 그 평가는 [수학에서의 함수 합성](https://wikipedia.org/wiki/Function_composition)과 유사합니다. 이 모델에서 함수 _f_와 _g_를 합성할 때, 결과 합성 (_f_ ∘ _g_)(_x_)는 _f_(_g_(_x_))와 동등합니다.

따라서, TypeScript에서 단일 선언에 여러 데코레이터를 평가할 때 다음 단계가 수행됩니다:

1. 각 데코레이터의 표현식이 위에서 아래로 평가됩니다.
2. 그런 다음 결과가 아래에서 위로 함수로 호출됩니다.

[데코레이터 팩토리](#데코레이터-팩토리)를 사용하면, 다음 예제로 이 평가 순서를 관찰할 수 있습니다:

```ts
function first() {
  console.log("first(): factory evaluated");
  return function (target: any, propertyKey: string, descriptor: PropertyDescriptor) {
    console.log("first(): called");
  };
}

function second() {
  console.log("second(): factory evaluated");
  return function (target: any, propertyKey: string, descriptor: PropertyDescriptor) {
    console.log("second(): called");
  };
}

class ExampleClass {
  @first()
  @second()
  method() {}
}
```

이것은 콘솔에 다음 출력을 출력합니다:

```shell
first(): factory evaluated
second(): factory evaluated
second(): called
first(): called
```

## 데코레이터 평가

클래스 내의 다양한 선언에 적용된 데코레이터가 어떻게 적용되는지에 대한 잘 정의된 순서가 있습니다:

1. _매개변수 데코레이터_, _메서드_, _접근자_, 또는 _프로퍼티 데코레이터_가 각 인스턴스 멤버에 대해 적용됩니다.
2. _매개변수 데코레이터_, _메서드_, _접근자_, 또는 _프로퍼티 데코레이터_가 각 정적 멤버에 대해 적용됩니다.
3. _매개변수 데코레이터_가 생성자에 대해 적용됩니다.
4. _클래스 데코레이터_가 클래스에 대해 적용됩니다.

## 클래스 데코레이터

_클래스 데코레이터_는 클래스 선언 바로 앞에 선언됩니다.
클래스 데코레이터는 클래스의 생성자에 적용되며 클래스 정의를 관찰, 수정 또는 대체하는 데 사용할 수 있습니다.
클래스 데코레이터는 선언 파일이나 다른 앰비언트 컨텍스트(예: `declare` 클래스)에서 사용할 수 없습니다.

클래스 데코레이터의 표현식은 런타임에 함수로 호출되며, 데코레이트된 클래스의 생성자가 유일한 인수로 전달됩니다.

클래스 데코레이터가 값을 반환하면, 제공된 생성자 함수로 클래스 선언을 대체합니다.

> **참고**&nbsp; 새로운 생성자 함수를 반환하기로 선택한 경우, 원래 프로토타입을 유지하도록 주의해야 합니다.
> 런타임에 데코레이터를 적용하는 로직은 이것을 대신 해주지 **않습니다**.

다음은 `BugReport` 클래스에 적용된 클래스 데코레이터(`@sealed`)의 예입니다:

```ts
@sealed
class BugReport {
  type = "report";
  title: string;

  constructor(t: string) {
    this.title = t;
  }
}
```

다음 함수 선언을 사용하여 `@sealed` 데코레이터를 정의할 수 있습니다:

```ts
function sealed(constructor: Function) {
  Object.seal(constructor);
  Object.seal(constructor.prototype);
}
```

`@sealed`가 실행되면, 생성자와 프로토타입을 모두 봉인하여, 런타임에 `BugReport.prototype`에 접근하거나 `BugReport` 자체에 프로퍼티를 정의하여 이 클래스에 추가 기능을 추가하거나 제거하는 것을 방지합니다(ES2015 클래스는 실제로 프로토타입 기반 생성자 함수에 대한 문법적 설탕일 뿐입니다). 이 데코레이터는 클래스가 `BugReport`를 서브클래싱하는 것을 방지하지 **않습니다**.

다음으로 새 기본값을 설정하기 위해 생성자를 재정의하는 방법의 예가 있습니다.

```ts
// @errors: 2339
function reportableClassDecorator<T extends { new (...args: any[]): {} }>(constructor: T) {
  return class extends constructor {
    reportingURL = "http://www...";
  };
}

@reportableClassDecorator
class BugReport {
  type = "report";
  title: string;

  constructor(t: string) {
    this.title = t;
  }
}

const bug = new BugReport("Needs dark mode");
console.log(bug.title); // "Needs dark mode" 출력
console.log(bug.type); // "report" 출력

// 데코레이터가 TypeScript 타입을 변경하지 _않으므로_
// 새 프로퍼티 `reportingURL`은 타입 시스템에서
// 알려지지 않습니다:
bug.reportingURL;
```

## 메서드 데코레이터

_메서드 데코레이터_는 메서드 선언 바로 앞에 선언됩니다.
데코레이터는 메서드의 _프로퍼티 설명자_에 적용되며, 메서드 정의를 관찰, 수정 또는 대체하는 데 사용할 수 있습니다.
메서드 데코레이터는 선언 파일, 오버로드, 또는 다른 앰비언트 컨텍스트(예: `declare` 클래스)에서 사용할 수 없습니다.

메서드 데코레이터의 표현식은 런타임에 다음 세 가지 인수와 함께 함수로 호출됩니다:

1. 정적 멤버의 경우 클래스의 생성자 함수, 또는 인스턴스 멤버의 경우 클래스의 프로토타입.
2. 멤버의 이름.
3. 멤버의 _프로퍼티 설명자_.

> **참고**&emsp; 스크립트 대상이 `ES5` 미만인 경우 _프로퍼티 설명자_는 `undefined`가 됩니다.

메서드 데코레이터가 값을 반환하면, 해당 메서드의 _프로퍼티 설명자_로 사용됩니다.

> **참고**&emsp; 스크립트 대상이 `ES5` 미만인 경우 반환 값은 무시됩니다.

다음은 `Greeter` 클래스의 메서드에 적용된 메서드 데코레이터(`@enumerable`)의 예입니다:

```ts
class Greeter {
  greeting: string;
  constructor(message: string) {
    this.greeting = message;
  }

  @enumerable(false)
  greet() {
    return "Hello, " + this.greeting;
  }
}
```

다음 함수 선언을 사용하여 `@enumerable` 데코레이터를 정의할 수 있습니다:

```ts
function enumerable(value: boolean) {
  return function (target: any, propertyKey: string, descriptor: PropertyDescriptor) {
    descriptor.enumerable = value;
  };
}
```

여기서 `@enumerable(false)` 데코레이터는 [데코레이터 팩토리](#데코레이터-팩토리)입니다.
`@enumerable(false)` 데코레이터가 호출되면, 프로퍼티 설명자의 `enumerable` 프로퍼티를 수정합니다.

## 접근자 데코레이터

_접근자 데코레이터_는 접근자 선언 바로 앞에 선언됩니다.
접근자 데코레이터는 접근자의 _프로퍼티 설명자_에 적용되며 접근자의 정의를 관찰, 수정 또는 대체하는 데 사용할 수 있습니다.
접근자 데코레이터는 선언 파일이나 다른 앰비언트 컨텍스트(예: `declare` 클래스)에서 사용할 수 없습니다.

> **참고**&emsp; TypeScript는 단일 멤버에 대해 `get`과 `set` 접근자를 모두 데코레이트하는 것을 허용하지 않습니다.
> 대신, 멤버에 대한 모든 데코레이터는 문서 순서로 지정된 첫 번째 접근자에 적용되어야 합니다.
> 이는 데코레이터가 `get`과 `set` 접근자를 별도로 결합하는 것이 아니라, _프로퍼티 설명자_에 적용되기 때문입니다.

접근자 데코레이터의 표현식은 런타임에 다음 세 가지 인수와 함께 함수로 호출됩니다:

1. 정적 멤버의 경우 클래스의 생성자 함수, 또는 인스턴스 멤버의 경우 클래스의 프로토타입.
2. 멤버의 이름.
3. 멤버의 _프로퍼티 설명자_.

> **참고**&emsp; 스크립트 대상이 `ES5` 미만인 경우 _프로퍼티 설명자_는 `undefined`가 됩니다.

접근자 데코레이터가 값을 반환하면, 해당 멤버의 _프로퍼티 설명자_로 사용됩니다.

> **참고**&emsp; 스크립트 대상이 `ES5` 미만인 경우 반환 값은 무시됩니다.

다음은 `Point` 클래스의 멤버에 적용된 접근자 데코레이터(`@configurable`)의 예입니다:

```ts
class Point {
  private _x: number;
  private _y: number;
  constructor(x: number, y: number) {
    this._x = x;
    this._y = y;
  }

  @configurable(false)
  get x() {
    return this._x;
  }

  @configurable(false)
  get y() {
    return this._y;
  }
}
```

다음 함수 선언을 사용하여 `@configurable` 데코레이터를 정의할 수 있습니다:

```ts
function configurable(value: boolean) {
  return function (target: any, propertyKey: string, descriptor: PropertyDescriptor) {
    descriptor.configurable = value;
  };
}
```

## 프로퍼티 데코레이터

_프로퍼티 데코레이터_는 프로퍼티 선언 바로 앞에 선언됩니다.
프로퍼티 데코레이터는 선언 파일이나 다른 앰비언트 컨텍스트(예: `declare` 클래스)에서 사용할 수 없습니다.

프로퍼티 데코레이터의 표현식은 런타임에 다음 두 가지 인수와 함께 함수로 호출됩니다:

1. 정적 멤버의 경우 클래스의 생성자 함수, 또는 인스턴스 멤버의 경우 클래스의 프로토타입.
2. 멤버의 이름.

> **참고**&emsp; TypeScript에서 프로퍼티 데코레이터가 초기화되는 방식 때문에 _프로퍼티 설명자_가 프로퍼티 데코레이터의 인수로 제공되지 않습니다.
> 이는 현재 프로토타입의 멤버를 정의할 때 인스턴스 프로퍼티를 설명하는 메커니즘이 없고, 프로퍼티의 이니셜라이저를 관찰하거나 수정할 방법이 없기 때문입니다. 반환 값도 무시됩니다.
> 따라서 프로퍼티 데코레이터는 특정 이름의 프로퍼티가 클래스에 대해 선언되었음을 관찰하는 데에만 사용할 수 있습니다.

이 정보를 사용하여 다음 예제와 같이 프로퍼티에 대한 메타데이터를 기록할 수 있습니다:

```ts
class Greeter {
  @format("Hello, %s")
  greeting: string;

  constructor(message: string) {
    this.greeting = message;
  }

  greet() {
    let formatString = getFormat(this, "greeting");
    return formatString.replace("%s", this.greeting);
  }
}
```

그런 다음 다음 함수 선언을 사용하여 `@format` 데코레이터와 `getFormat` 함수를 정의할 수 있습니다:

```ts
import "reflect-metadata";

const formatMetadataKey = Symbol("format");

function format(formatString: string) {
  return Reflect.metadata(formatMetadataKey, formatString);
}

function getFormat(target: any, propertyKey: string) {
  return Reflect.getMetadata(formatMetadataKey, target, propertyKey);
}
```

여기서 `@format("Hello, %s")` 데코레이터는 [데코레이터 팩토리](#데코레이터-팩토리)입니다.
`@format("Hello, %s")`이 호출되면, `reflect-metadata` 라이브러리의 `Reflect.metadata` 함수를 사용하여 프로퍼티에 대한 메타데이터 항목을 추가합니다.
`getFormat`이 호출되면, 형식에 대한 메타데이터 값을 읽습니다.

> **참고**&emsp; 이 예제는 `reflect-metadata` 라이브러리가 필요합니다.
> `reflect-metadata` 라이브러리에 대한 자세한 내용은 [메타데이터](#메타데이터)를 참조하세요.

## 매개변수 데코레이터

_매개변수 데코레이터_는 매개변수 선언 바로 앞에 선언됩니다.
매개변수 데코레이터는 클래스 생성자 또는 메서드 선언의 함수에 적용됩니다.
매개변수 데코레이터는 선언 파일, 오버로드, 또는 다른 앰비언트 컨텍스트(예: `declare` 클래스)에서 사용할 수 없습니다.

매개변수 데코레이터의 표현식은 런타임에 다음 세 가지 인수와 함께 함수로 호출됩니다:

1. 정적 멤버의 경우 클래스의 생성자 함수, 또는 인스턴스 멤버의 경우 클래스의 프로토타입.
2. 멤버의 이름.
3. 함수의 매개변수 목록에서 매개변수의 서수 인덱스.

> **참고**&emsp; 매개변수 데코레이터는 매개변수가 메서드에 선언되었음을 관찰하는 데에만 사용할 수 있습니다.

매개변수 데코레이터의 반환 값은 무시됩니다.

다음은 `BugReport` 클래스 멤버의 매개변수에 적용된 매개변수 데코레이터(`@required`)의 예입니다:

```ts
class BugReport {
  type = "report";
  title: string;

  constructor(t: string) {
    this.title = t;
  }

  @validate
  print(@required verbose: boolean) {
    if (verbose) {
      return `type: ${this.type}\ntitle: ${this.title}`;
    } else {
     return this.title;
    }
  }
}
```

그런 다음 다음 함수 선언을 사용하여 `@required`와 `@validate` 데코레이터를 정의할 수 있습니다:

```ts
import "reflect-metadata";
const requiredMetadataKey = Symbol("required");

function required(target: Object, propertyKey: string | symbol, parameterIndex: number) {
  let existingRequiredParameters: number[] = Reflect.getOwnMetadata(requiredMetadataKey, target, propertyKey) || [];
  existingRequiredParameters.push(parameterIndex);
  Reflect.defineMetadata( requiredMetadataKey, existingRequiredParameters, target, propertyKey);
}

function validate(target: any, propertyName: string, descriptor: TypedPropertyDescriptor<Function>) {
  let method = descriptor.value!;

  descriptor.value = function () {
    let requiredParameters: number[] = Reflect.getOwnMetadata(requiredMetadataKey, target, propertyName);
    if (requiredParameters) {
      for (let parameterIndex of requiredParameters) {
        if (parameterIndex >= arguments.length || arguments[parameterIndex] === undefined) {
          throw new Error("Missing required argument.");
        }
      }
    }
    return method.apply(this, arguments);
  };
}
```

`@required` 데코레이터는 매개변수를 필수로 표시하는 메타데이터 항목을 추가합니다.
`@validate` 데코레이터는 기존 `print` 메서드를 원래 메서드를 호출하기 전에 인수의 유효성을 검사하는 함수로 래핑합니다.

> **참고**&emsp; 이 예제는 `reflect-metadata` 라이브러리가 필요합니다.
> `reflect-metadata` 라이브러리에 대한 자세한 내용은 [메타데이터](#메타데이터)를 참조하세요.

## 메타데이터

일부 예제는 [실험적 메타데이터 API](https://github.com/rbuckton/ReflectDecorators)에 대한 폴리필을 추가하는 `reflect-metadata` 라이브러리를 사용합니다.
이 라이브러리는 아직 ECMAScript (JavaScript) 표준의 일부가 아닙니다.
그러나 데코레이터가 공식적으로 ECMAScript 표준의 일부로 채택되면, 이러한 확장이 채택을 위해 제안될 것입니다.

이 라이브러리는 npm을 통해 설치할 수 있습니다:

```shell
npm i reflect-metadata --save
```

TypeScript는 데코레이터가 있는 선언에 대해 특정 유형의 메타데이터를 방출하는 실험적 지원을 포함합니다.
이 실험적 지원을 활성화하려면, 커맨드 라인이나 `tsconfig.json`에서 [`emitDecoratorMetadata`](/tsconfig#emitDecoratorMetadata) 컴파일러 옵션을 설정해야 합니다:

**커맨드 라인**:

```shell
tsc --target ES5 --experimentalDecorators --emitDecoratorMetadata
```

**tsconfig.json**:

```json
{
  "compilerOptions": {
    "target": "ES5",
    "experimentalDecorators": true,
    "emitDecoratorMetadata": true
  }
}
```

활성화되면, `reflect-metadata` 라이브러리가 임포트된 한, 추가 설계 시간 타입 정보가 런타임에 노출됩니다.

다음 예제에서 이것이 어떻게 작동하는지 볼 수 있습니다:

```ts
import "reflect-metadata";

class Point {
  constructor(public x: number, public y: number) {}
}

class Line {
  private _start: Point;
  private _end: Point;

  @validate
  set start(value: Point) {
    this._start = value;
  }

  get start() {
    return this._start;
  }

  @validate
  set end(value: Point) {
    this._end = value;
  }

  get end() {
    return this._end;
  }
}

function validate<T>(target: any, propertyKey: string, descriptor: TypedPropertyDescriptor<T>) {
  let set = descriptor.set!;

  descriptor.set = function (value: T) {
    let type = Reflect.getMetadata("design:type", target, propertyKey);

    if (!(value instanceof type)) {
      throw new TypeError(`Invalid type, got ${typeof value} not ${type.name}.`);
    }

    set.call(this, value);
  };
}

const line = new Line()
line.start = new Point(0, 0)

// @ts-ignore
// line.end = {}

// 런타임에 다음과 함께 실패합니다:
// > Invalid type, got object not Point
```

TypeScript 컴파일러는 `@Reflect.metadata` 데코레이터를 사용하여 설계 시간 타입 정보를 주입합니다.
다음 TypeScript와 동등하다고 생각할 수 있습니다:

```ts
class Line {
  private _start: Point;
  private _end: Point;

  @validate
  @Reflect.metadata("design:type", Point)
  set start(value: Point) {
    this._start = value;
  }
  get start() {
    return this._start;
  }

  @validate
  @Reflect.metadata("design:type", Point)
  set end(value: Point) {
    this._end = value;
  }
  get end() {
    return this._end;
  }
}
```

> **참고**&emsp; 데코레이터 메타데이터는 실험적 기능이며 향후 릴리스에서 호환성이 깨지는 변경 사항이 도입될 수 있습니다.

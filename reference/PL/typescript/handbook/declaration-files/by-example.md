# 선언 참조

이 가이드의 목적은 고품질의 정의 파일을 작성하는 방법을 알려주는 것입니다. 이 가이드는 일부 API에 대한 문서와 해당 API의 샘플 사용법을 보여주고, 해당하는 선언을 작성하는 방법을 설명하는 방식으로 구성되어 있습니다.

이 예제들은 대략적으로 복잡성이 증가하는 순서로 정렬되어 있습니다.

## 프로퍼티를 가진 객체

_문서_

> 전역 변수 `myLib`는 인사말을 생성하는 `makeGreeting` 함수와 지금까지 만들어진 인사말의 수를 나타내는 `numberOfGreetings` 프로퍼티를 가지고 있습니다.

_코드_

```ts
let result = myLib.makeGreeting("hello, world");
console.log("The computed greeting is:" + result);

let count = myLib.numberOfGreetings;
```

_선언_

`declare namespace`를 사용하여 점 표기법으로 접근하는 타입이나 값을 설명합니다.

```ts
declare namespace myLib {
  function makeGreeting(s: string): string;
  let numberOfGreetings: number;
}
```

## 오버로드된 함수

_문서_

`getWidget` 함수는 숫자를 받아 Widget을 반환하거나, 문자열을 받아 Widget 배열을 반환합니다.

_코드_

```ts
let x: Widget = getWidget(43);

let arr: Widget[] = getWidget("all of them");
```

_선언_

```ts
declare function getWidget(n: number): Widget;
declare function getWidget(s: string): Widget[];
```

## 재사용 가능한 타입 (인터페이스)

_문서_

> 인사말을 지정할 때, `GreetingSettings` 객체를 전달해야 합니다.
> 이 객체는 다음 프로퍼티를 가집니다:
>
> 1 - greeting: 필수 문자열
>
> 2 - duration: 선택적 시간 길이 (밀리초)
>
> 3 - color: 선택적 문자열, 예: '#ff00ff'

_코드_

```ts
greet({
  greeting: "hello world",
  duration: 4000
});
```

_선언_

`interface`를 사용하여 프로퍼티를 가진 타입을 정의합니다.

```ts
interface GreetingSettings {
  greeting: string;
  duration?: number;
  color?: string;
}

declare function greet(setting: GreetingSettings): void;
```

## 재사용 가능한 타입 (타입 별칭)

_문서_

> 인사말이 예상되는 곳 어디서나, `string`, `string`을 반환하는 함수, 또는 `Greeter` 인스턴스를 제공할 수 있습니다.

_코드_

```ts
function getGreeting() {
  return "howdy";
}
class MyGreeter extends Greeter {}

greet("hello");
greet(getGreeting);
greet(new MyGreeter());
```

_선언_

타입 별칭을 사용하여 타입에 대한 약어를 만들 수 있습니다:

```ts
type GreetingLike = string | (() => string) | MyGreeter;

declare function greet(g: GreetingLike): void;
```

## 타입 구성하기

_문서_

> `greeter` 객체는 파일에 로그를 남기거나 알림을 표시할 수 있습니다.
> `.log(...)`에 LogOptions을 제공하고 `.alert(...)`에 alert 옵션을 제공할 수 있습니다.

_코드_

```ts
const g = new Greeter("Hello");
g.log({ verbose: true });
g.alert({ modal: false, title: "Current Greeting" });
```

_선언_

네임스페이스를 사용하여 타입을 구성합니다.

```ts
declare namespace GreetingLib {
  interface LogOptions {
    verbose?: boolean;
  }
  interface AlertOptions {
    modal: boolean;
    title?: string;
    color?: string;
  }
}
```

하나의 선언에서 중첩된 네임스페이스를 만들 수도 있습니다:

```ts
declare namespace GreetingLib.Options {
  // GreetingLib.Options.Log를 통해 참조
  interface Log {
    verbose?: boolean;
  }
  interface Alert {
    modal: boolean;
    title?: string;
    color?: string;
  }
}
```

## 클래스

_문서_

> `Greeter` 객체를 인스턴스화하여 greeter를 만들거나, 이를 확장하여 커스텀 greeter를 만들 수 있습니다.

_코드_

```ts
const myGreeter = new Greeter("hello, world");
myGreeter.greeting = "howdy";
myGreeter.showGreeting();

class SpecialGreeter extends Greeter {
  constructor() {
    super("Very special greetings");
  }
}
```

_선언_

`declare class`를 사용하여 클래스 또는 클래스와 유사한 객체를 설명합니다.
클래스는 생성자뿐만 아니라 프로퍼티와 메서드를 가질 수 있습니다.

```ts
declare class Greeter {
  constructor(greeting: string);

  greeting: string;
  showGreeting(): void;
}
```

## 전역 변수

_문서_

> 전역 변수 `foo`는 현재 존재하는 위젯의 수를 포함합니다.

_코드_

```ts
console.log("Half the number of widgets is " + foo / 2);
```

_선언_

`declare var`를 사용하여 변수를 선언합니다.
변수가 읽기 전용인 경우 `declare const`를 사용할 수 있습니다.
변수가 블록 스코프인 경우 `declare let`을 사용할 수도 있습니다.

```ts
/** 현재 존재하는 위젯의 수 */
declare var foo: number;
```

## 전역 함수

_문서_

> 사용자에게 인사말을 표시하기 위해 문자열과 함께 `greet` 함수를 호출할 수 있습니다.

_코드_

```ts
greet("hello, world");
```

_선언_

`declare function`을 사용하여 함수를 선언합니다.

```ts
declare function greet(greeting: string): void;
```

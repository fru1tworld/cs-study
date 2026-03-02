# 믹스인 (Mixins)

전통적인 OO 계층 구조와 함께, 재사용 가능한 컴포넌트로부터 클래스를 빌드하는 또 다른 인기 있는 방법은 더 간단한 부분 클래스를 결합하여 빌드하는 것입니다.
Scala와 같은 언어에서 믹스인이나 트레이트 아이디어에 익숙할 수 있으며, 이 패턴은 JavaScript 커뮤니티에서도 인기를 얻고 있습니다.

## 믹스인은 어떻게 작동하나요?

이 패턴은 클래스 상속과 함께 제네릭을 사용하여 베이스 클래스를 확장하는 것에 의존합니다.
TypeScript의 가장 좋은 믹스인 지원은 클래스 표현식 패턴을 통해 수행됩니다.
이 패턴이 JavaScript에서 어떻게 작동하는지에 대해 [여기](https://justinfagnani.com/2015/12/21/real-mixins-with-javascript-classes/)에서 더 읽을 수 있습니다.

시작하려면, 믹스인이 위에 적용될 클래스가 필요합니다:

```ts
class Sprite {
  name = "";
  x = 0;
  y = 0;

  constructor(name: string) {
    this.name = name;
  }
}
```

그런 다음 베이스 클래스를 확장하는 클래스 표현식을 반환하는 타입과 팩토리 함수가 필요합니다.

```ts
// 시작하려면, 다른 클래스에서 확장하는 데 사용할 타입이 필요합니다.
// 주요 책임은 전달되는 타입이 클래스임을 선언하는 것입니다.

type Constructor = new (...args: any[]) => {};

// 이 믹스인은 scale 프로퍼티와
// 캡슐화된 private 프로퍼티로 변경하기 위한 getter와 setter를 추가합니다:

function Scale<TBase extends Constructor>(Base: TBase) {
  return class Scaling extends Base {
    // 믹스인은 private/protected 프로퍼티를 선언할 수 없습니다
    // 그러나 ES2020 private 필드를 사용할 수 있습니다
    _scale = 1;

    setScale(scale: number) {
      this._scale = scale;
    }

    get scale(): number {
      return this._scale;
    }
  };
}
```

이것들이 모두 설정되면, 믹스인이 적용된 베이스 클래스를 나타내는 클래스를 만들 수 있습니다:

```ts
// 믹스인 Scale 적용자와 함께 Sprite 클래스에서
// 새 클래스를 구성합니다:
const EightBitSprite = Scale(Sprite);

const flappySprite = new EightBitSprite("Bird");
flappySprite.setScale(0.8);
console.log(flappySprite.scale);
```

## 제한된 믹스인

위 형식에서 믹스인은 클래스에 대한 기본 지식이 없어 원하는 디자인을 만들기 어려울 수 있습니다.

이를 모델링하기 위해, 원래 생성자 타입을 제네릭 인수를 받도록 수정합니다.

```ts
// 이것은 이전 생성자였습니다:
type Constructor = new (...args: any[]) => {};
// 이제 이 믹스인이 적용되는 클래스에 제약을 적용할 수 있는
// 제네릭 버전을 사용합니다
type GConstructor<T = {}> = new (...args: any[]) => T;
```

이를 통해 제한된 베이스 클래스에서만 작동하는 클래스를 만들 수 있습니다:

```ts
type Positionable = GConstructor<{ setPos: (x: number, y: number) => void }>;
type Spritable = GConstructor<Sprite>;
type Loggable = GConstructor<{ print: () => void }>;
```

그런 다음 빌드할 특정 베이스가 있을 때만 작동하는 믹스인을 만들 수 있습니다:

```ts
function Jumpable<TBase extends Positionable>(Base: TBase) {
  return class Jumpable extends Base {
    jump() {
      // 이 믹스인은 Positionable 제약으로 인해
      // setPos가 정의된 베이스 클래스가 전달된 경우에만 작동합니다.
      this.setPos(0, 20);
    }
  };
}
```

## 대안적 패턴

이 문서의 이전 버전에서는 런타임과 타입 계층 구조를 별도로 생성한 다음 마지막에 병합하는 방식으로 믹스인을 작성하는 방법을 권장했습니다:

```ts
// 각 믹스인은 전통적인 ES 클래스입니다
class Jumpable {
  jump() {}
}

class Duckable {
  duck() {}
}

// 베이스 포함
class Sprite {
  x = 0;
  y = 0;
}

// 그런 다음 예상되는 믹스인을
// 베이스와 같은 이름으로 병합하는 인터페이스를 만듭니다
interface Sprite extends Jumpable, Duckable {}
// 런타임에 JS를 통해 베이스 클래스에 믹스인을 적용합니다
applyMixins(Sprite, [Jumpable, Duckable]);

let player = new Sprite();
player.jump();
console.log(player.x, player.y);

// 이것은 코드베이스의 어디에나 있을 수 있습니다:
function applyMixins(derivedCtor: any, constructors: any[]) {
  constructors.forEach((baseCtor) => {
    Object.getOwnPropertyNames(baseCtor.prototype).forEach((name) => {
      Object.defineProperty(
        derivedCtor.prototype,
        name,
        Object.getOwnPropertyDescriptor(baseCtor.prototype, name) ||
          Object.create(null)
      );
    });
  });
}
```

이 패턴은 컴파일러에 덜 의존하고, 런타임과 타입 시스템이 올바르게 동기화되도록 코드베이스에 더 의존합니다.

## 제약 사항

믹스인 패턴은 TypeScript 컴파일러 내에서 코드 흐름 분석을 통해 네이티브로 지원됩니다.
네이티브 지원의 가장자리에 도달할 수 있는 몇 가지 경우가 있습니다.

#### 데코레이터와 믹스인 [`#4881`](https://github.com/microsoft/TypeScript/issues/4881)

데코레이터를 사용하여 코드 흐름 분석을 통해 믹스인을 제공할 수 없습니다:

```ts
// @experimentalDecorators
// @errors: 2339
// 믹스인 패턴을 복제하는 데코레이터 함수:
const Pausable = (target: typeof Player) => {
  return class Pausable extends target {
    shouldFreeze = false;
  };
};

@Pausable
class Player {
  x = 0;
  y = 0;
}

// Player 클래스에는 데코레이터의 타입이 병합되지 않습니다:
const player = new Player();
player.shouldFreeze;

// 런타임 측면은 타입 구성이나 인터페이스 병합을 통해
// 수동으로 복제할 수 있습니다.
type FreezablePlayer = Player & { shouldFreeze: boolean };

const playerTwo = (new Player() as unknown) as FreezablePlayer;
playerTwo.shouldFreeze;
```

#### 정적 프로퍼티 믹스인 [`#17829`](https://github.com/microsoft/TypeScript/issues/17829)

제약보다는 함정입니다.
클래스 표현식 패턴은 싱글톤을 생성하므로, 다른 변수 타입을 지원하기 위해 타입 시스템에서 매핑할 수 없습니다.

제네릭에 따라 다른 클래스를 반환하는 함수를 사용하여 이 문제를 해결할 수 있습니다:

```ts
function base<T>() {
  class Base {
    static prop: T;
  }
  return Base;
}

function derived<T>() {
  class Derived extends base<T>() {
    static anotherProp: T;
  }
  return Derived;
}

class Spec extends derived<string>() {}

Spec.prop; // string
Spec.anotherProp; // string
```

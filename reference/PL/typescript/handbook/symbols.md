# 심볼 (Symbols)

ECMAScript 2015부터 `symbol`은 `number`와 `string`처럼 원시 데이터 타입입니다.

`symbol` 값은 `Symbol` 생성자를 호출하여 생성됩니다.

```ts
let sym1 = Symbol();

let sym2 = Symbol("key"); // 선택적 문자열 키
```

심볼은 불변이며 고유합니다.

```ts
let sym2 = Symbol("key");
let sym3 = Symbol("key");

sym2 === sym3; // false, 심볼은 고유합니다
```

문자열처럼, 심볼은 객체 프로퍼티의 키로 사용할 수 있습니다.

```ts
const sym = Symbol();

let obj = {
  [sym]: "value",
};

console.log(obj[sym]); // "value"
```

심볼은 계산된 프로퍼티 선언과 결합하여 객체 프로퍼티와 클래스 멤버를 선언하는 데에도 사용할 수 있습니다.

```ts
const getClassNameSymbol = Symbol();

class C {
  [getClassNameSymbol]() {
    return "C";
  }
}

let c = new C();
let className = c[getClassNameSymbol](); // "C"
```

## `unique symbol`

심볼을 고유한 리터럴로 처리하기 위해 `unique symbol`이라는 특별한 타입을 사용할 수 있습니다. `unique symbol`은 `symbol`의 하위 타입이며, `Symbol()` 또는 `Symbol.for()` 호출이나 명시적 타입 주석에서만 생성됩니다. 이 타입은 `const` 선언과 `readonly static` 프로퍼티에만 허용되며, 특정 고유 심볼을 참조하려면 `typeof` 연산자를 사용해야 합니다. 고유 심볼에 대한 각 참조는 주어진 선언에 연결된 완전히 고유한 정체성을 의미합니다.

```ts
// @errors: 1332
declare const sym1: unique symbol;

// sym2는 상수 참조만 될 수 있습니다.
let sym2: unique symbol = Symbol();

// 작동함 - 고유 심볼을 참조하지만, 정체성은 'sym1'에 연결됨.
let sym3: typeof sym1 = sym1;

// 이것도 작동합니다.
class C {
  static readonly StaticSymbol: unique symbol = Symbol();
}
```

각 `unique symbol`은 완전히 별개의 정체성을 가지므로, 두 `unique symbol` 타입은 서로 할당하거나 비교할 수 없습니다.

```ts
// @errors: 2367
const sym2 = Symbol();
const sym3 = Symbol();

if (sym2 === sym3) {
  // ...
}
```

## 잘 알려진 심볼

사용자 정의 심볼 외에도 잘 알려진 내장 심볼이 있습니다.
내장 심볼은 내부 언어 동작을 나타내는 데 사용됩니다.

다음은 잘 알려진 심볼 목록입니다:

### `Symbol.asyncIterator`

for await..of 루프와 함께 사용할 수 있는 객체에 대한 비동기 이터레이터를 반환하는 메서드입니다.

### `Symbol.hasInstance`

생성자 객체가 객체를 생성자의 인스턴스 중 하나로 인식하는지 여부를 결정하는 메서드입니다. instanceof 연산자의 의미론에 의해 호출됩니다.

### `Symbol.isConcatSpreadable`

Array.prototype.concat에 의해 객체가 배열 요소로 평탄화되어야 함을 나타내는 불리언 값입니다.

### `Symbol.iterator`

객체의 기본 이터레이터를 반환하는 메서드입니다. for-of 문의 의미론에 의해 호출됩니다.

### `Symbol.match`

정규 표현식을 문자열과 매치하는 정규 표현식 메서드입니다. `String.prototype.match` 메서드에 의해 호출됩니다.

### `Symbol.replace`

문자열의 매치된 부분 문자열을 대체하는 정규 표현식 메서드입니다. `String.prototype.replace` 메서드에 의해 호출됩니다.

### `Symbol.search`

정규 표현식과 일치하는 문자열 내의 인덱스를 반환하는 정규 표현식 메서드입니다. `String.prototype.search` 메서드에 의해 호출됩니다.

### `Symbol.species`

파생 객체를 생성하는 데 사용되는 생성자 함수인 함수 값 프로퍼티입니다.

### `Symbol.split`

정규 표현식과 일치하는 인덱스에서 문자열을 분할하는 정규 표현식 메서드입니다.
`String.prototype.split` 메서드에 의해 호출됩니다.

### `Symbol.toPrimitive`

객체를 해당하는 원시 값으로 변환하는 메서드입니다.
`ToPrimitive` 추상 연산에 의해 호출됩니다.

### `Symbol.toStringTag`

객체의 기본 문자열 설명 생성에 사용되는 문자열 값입니다.
내장 메서드 `Object.prototype.toString`에 의해 호출됩니다.

### `Symbol.unscopables`

자체 프로퍼티 이름이 연관된 객체의 'with' 환경 바인딩에서 제외되는 프로퍼티 이름인 객체입니다.

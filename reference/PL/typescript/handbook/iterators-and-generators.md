# 이터레이터와 제너레이터 (Iterators and Generators)

## 이터러블

객체가 [`Symbol.iterator`](symbols.html#symboliterator) 프로퍼티에 대한 구현을 가지고 있으면 이터러블로 간주됩니다.
`Array`, `Map`, `Set`, `String`, `Int32Array`, `Uint32Array` 등과 같은 일부 내장 타입에는 이미 `Symbol.iterator` 프로퍼티가 구현되어 있습니다.
객체의 `Symbol.iterator` 함수는 반복할 값 목록을 반환하는 역할을 합니다.

### `Iterable` 인터페이스

`Iterable`은 이터러블인 위에 나열된 타입을 받고 싶을 때 사용할 수 있는 타입입니다. 다음은 예제입니다:

```ts
function toArray<X>(xs: Iterable<X>): X[] {
  return [...xs]
}
```

### `for..of` 문

`for..of`는 이터러블 객체를 반복하며, 객체의 `Symbol.iterator` 프로퍼티를 호출합니다.
배열에 대한 간단한 `for..of` 루프는 다음과 같습니다:

```ts
let someArray = [1, "string", false];

for (let entry of someArray) {
  console.log(entry); // 1, "string", false
}
```

### `for..of` vs. `for..in` 문

`for..of`와 `for..in` 문 모두 리스트를 반복합니다; 반복되는 값은 다르지만, `for..in`은 반복되는 객체의 _키_ 목록을 반환하는 반면, `for..of`는 반복되는 객체의 숫자 프로퍼티의 _값_ 목록을 반환합니다.

다음은 이 구별을 보여주는 예제입니다:

```ts
let list = [4, 5, 6];

for (let i in list) {
  console.log(i); // "0", "1", "2",
}

for (let i of list) {
  console.log(i); // 4, 5, 6
}
```

또 다른 구별은 `for..in`은 어떤 객체에서도 작동하며, 이 객체의 프로퍼티를 검사하는 방법 역할을 한다는 것입니다.
반면 `for..of`는 주로 이터러블 객체의 값에 관심이 있습니다. `Map`과 `Set`과 같은 내장 객체는 저장된 값에 접근할 수 있도록 `Symbol.iterator` 프로퍼티를 구현합니다.

```ts
let pets = new Set(["Cat", "Dog", "Hamster"]);
pets["species"] = "mammals";

for (let pet in pets) {
  console.log(pet); // "species"
}

for (let pet of pets) {
  console.log(pet); // "Cat", "Dog", "Hamster"
}
```

### 코드 생성

#### ES5 대상

ES5 호환 엔진을 대상으로 할 때, 이터레이터는 `Array` 타입의 값에만 허용됩니다.
비Array 값이 `Symbol.iterator` 프로퍼티를 구현하더라도 비Array 값에 `for..of` 루프를 사용하는 것은 오류입니다.

컴파일러는 `for..of` 루프에 대해 간단한 `for` 루프를 생성합니다. 예를 들어:

```ts
let numbers = [1, 2, 3];
for (let num of numbers) {
  console.log(num);
}
```

는 다음과 같이 생성됩니다:

```js
var numbers = [1, 2, 3];
for (var _i = 0; _i < numbers.length; _i++) {
  var num = numbers[_i];
  console.log(num);
}
```

#### ECMAScript 2015 이상 대상

ECMAScript 2015 호환 엔진을 대상으로 할 때, 컴파일러는 엔진의 내장 이터레이터 구현을 대상으로 `for..of` 루프를 생성합니다.

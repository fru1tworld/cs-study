# 제네릭 타입, 트레이트, 그리고 라이프타임

## 개요

모든 프로그래밍 언어에는 개념의 중복을 효과적으로 처리하기 위한 도구가 있습니다. Rust에서 그러한 도구 중 하나가 **제네릭(Generics)**입니다: 구체적인 타입이나 다른 속성의 추상적인 대역입니다. 컴파일하고 코드를 실행할 때 그 자리에 무엇이 들어갈지 모르는 상태에서 제네릭의 동작이나 다른 제네릭과의 관계를 표현할 수 있습니다.

함수는 `i32`나 `String` 같은 구체적인 타입 대신 제네릭 타입의 매개변수를 받을 수 있습니다. 이는 알 수 없는 값을 가진 매개변수를 받아 여러 구체적인 값에 대해 동일한 코드를 실행하는 것과 같습니다.

**이미 사용해본 제네릭:**
- 6장의 `Option<T>`
- 8장의 `Vec<T>`와 `HashMap<K, V>`
- 9장의 `Result<T, E>`

이 장에서는 자신만의 타입, 함수, 메서드를 제네릭으로 정의하는 방법을 탐구합니다.

## 다루는 주제

1. **함수 추출로 중복 제거** - 코드 중복을 줄이기 위해 함수를 추출하는 방법 복습
2. **제네릭 함수** - 매개변수 타입만 다른 두 함수에서 제네릭 함수를 만드는 동일한 기법 사용
3. **구조체와 열거형의 제네릭 타입** - 구조체와 열거형 정의에서 제네릭 타입을 사용하는 방법
4. **트레이트(Traits)** - 동작을 제네릭 방식으로 정의하고, 트레이트와 제네릭 타입을 결합하여 제네릭 타입을 제약하는 방법
5. **라이프타임(Lifetimes)** - 참조들이 서로 어떻게 관련되는지 컴파일러에게 정보를 제공하는 제네릭의 일종

## 함수 추출로 중복 제거하기

### 초기 코드 - 가장 큰 숫자 찾기

**Listing 10-1**: 숫자 목록에서 가장 큰 숫자 찾기

```rust
fn main() {
    let number_list = vec![34, 50, 25, 100, 65];

    let mut largest = &number_list[0];

    for number in &number_list {
        if number > largest {
            largest = number;
        }
    }

    println!("The largest number is {largest}");
    assert_eq!(*largest, 100);
}
```

이 코드는 정수 목록을 `number_list` 변수에 저장하고, 목록의 첫 번째 숫자에 대한 참조를 `largest`라는 변수에 넣습니다. 그런 다음 목록의 모든 숫자를 순회합니다. 현재 숫자가 `largest`에 저장된 숫자보다 크면 해당 변수의 참조를 교체합니다. 목록의 모든 숫자를 확인한 후, `largest`는 가장 큰 숫자(이 경우 100)를 참조해야 합니다.

### 중복 코드 문제

**Listing 10-2**: *두 개의* 숫자 목록에서 가장 큰 숫자를 찾는 코드

```rust
fn main() {
    let number_list = vec![34, 50, 25, 100, 65];

    let mut largest = &number_list[0];

    for number in &number_list {
        if number > largest {
            largest = number;
        }
    }

    println!("The largest number is {largest}");

    let number_list = vec![102, 34, 6000, 89, 54, 2, 43, 8];

    let mut largest = &number_list[0];

    for number in &number_list {
        if number > largest {
            largest = number;
        }
    }

    println!("The largest number is {largest}");
}
```

이 코드는 작동하지만, 코드를 중복하는 것은 지루하고 오류가 발생하기 쉽습니다. 또한 변경하고 싶을 때 여러 곳에서 코드를 업데이트해야 한다는 것을 기억해야 합니다.

### 함수 추출하기

**Listing 10-3**: 두 목록에서 가장 큰 숫자를 찾는 추상화된 코드

```rust
fn largest(list: &[i32]) -> &i32 {
    let mut largest = &list[0];

    for item in list {
        if item > largest {
            largest = item;
        }
    }

    largest
}

fn main() {
    let number_list = vec![34, 50, 25, 100, 65];

    let result = largest(&number_list);
    println!("The largest number is {result}");
    assert_eq!(*result, 100);

    let number_list = vec![102, 34, 6000, 89, 54, 2, 43, 8];

    let result = largest(&number_list);
    println!("The largest number is {result}");
    assert_eq!(*result, 6000);
}
```

`largest` 함수는 `list`라는 매개변수를 가지며, 이는 함수에 전달할 수 있는 모든 구체적인 `i32` 값의 슬라이스를 나타냅니다. 결과적으로, 함수를 호출할 때 코드는 전달한 특정 값에 대해 실행됩니다.

### 리팩토링 단계

Listing 10-2에서 Listing 10-3으로 코드를 변경하기 위해 취한 단계:

1. **중복 코드 식별** - 반복되는 로직 인식
2. **중복 코드를 함수 본문으로 추출** - 함수 시그니처에서 해당 코드의 입력과 반환 값 지정
3. **중복된 코드 인스턴스 업데이트** - 코드를 반복하는 대신 함수 호출

### 제네릭으로의 다음 단계

이와 동일한 단계를 제네릭과 함께 사용하여 코드 중복을 더욱 줄일 것입니다. 함수 본문이 특정 값 대신 추상적인 `list`에서 작동할 수 있는 것처럼, 제네릭은 코드가 추상적인 타입에서 작동할 수 있게 합니다.

예를 들어, `i32` 값의 슬라이스에서 가장 큰 항목을 찾는 함수와 `char` 값의 슬라이스에서 가장 큰 항목을 찾는 함수, 두 개의 함수가 있다면 제네릭이 그 중복을 제거할 것입니다.

## 핵심 포인트

**제네릭의 장점:**
- 코드 중복 제거
- 다양한 타입에 대해 동일한 로직 적용
- 타입 안전성 유지하면서 유연성 제공

**함수 추출 과정:**
- 중복 코드 식별
- 공통 로직을 함수로 분리
- 매개변수와 반환 타입 정의
- 원래 코드를 함수 호출로 대체

**이 장에서 배울 내용:**
- 제네릭 함수 정의 방법
- 구조체와 열거형에서 제네릭 사용
- 트레이트로 동작 정의
- 라이프타임으로 참조 관계 표현

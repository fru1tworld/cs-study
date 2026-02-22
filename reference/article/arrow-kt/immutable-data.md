# Arrow-kt 불변 데이터 (Optics)

Arrow Optics는 불변 데이터를 다루기 위한 간결한 방법을 제공합니다. `arrow-optics` 라이브러리와 관련 플러그인을 통해 이 기능에 접근할 수 있습니다.

## 목차

1. [소개 (Introduction)](#소개-introduction)
2. [렌즈 (Lenses)](#렌즈-lenses)
3. [옵셔널 (Optionals)](#옵셔널-optionals)
4. [트래버설 (Traversals)](#트래버설-traversals)
5. [프리즘과 아이소 (Prisms & Isos)](#프리즘과-아이소-prisms--isos)
6. [리플렉션 (Reflection)](#리플렉션-reflection)
7. [정규 표현식 (Regular Expressions)](#정규-표현식-regular-expressions)

---

## 소개 (Introduction)

### 개요

Arrow는 Kotlin에서 깊게 중첩된 불변 데이터 구조를 간단하게 변환할 수 있도록 옵틱스(optics) 를 제공합니다. 이 프레임워크는 Kotlin의 `copy` 메서드가 여러 중첩 레벨을 통해 필드 이름을 반복해야 하는 핵심적인 제한사항을 해결합니다.

### 핵심 문제

중첩된 데이터 클래스로 작업할 때의 어려움을 보여드리겠습니다:

```kotlin
data class Person(val name: String, val age: Int, val address: Address)
data class Address(val street: Street, val city: City)
data class Street(val name: String, val number: Int?)
data class City(val name: String, val country: String)
```

옵틱스 없이 단일 중첩 필드를 수정하려면 연쇄적인 copy가 필요합니다:

```kotlin
fun Person.capitalizeCountry(): Person =
  this.copy(
    address = address.copy(
      city = address.city.copy(
        country = address.city.country.capitalize()
      )
    )
  )
```

### 해결책: 옵틱스

Arrow Optics를 사용하려면 클래스에 `@optics` 어노테이션을 추가하고 빈 companion object를 포함시킵니다:

```kotlin
import arrow.optics.*

@optics data class Person(val name: String, val age: Int, val address: Address) {
  companion object
}
```

두 가지 접근 방식으로 변환을 단순화할 수 있습니다:

Modify 접근 방식:
```kotlin
fun Person.capitalizeCountryModify(): Person =
  Person.address.city.country.modify(this) { it.capitalize() }
```

Copy 빌더 접근 방식:
```kotlin
fun Person.capitalizeCountryCopy(): Person =
  this.copy {
    Person.address.city.country transform { it.capitalize() }
  }
```

### 옵틱 타입

Arrow는 요소 수에 초점을 맞춘 계층 구조를 구현합니다:

- 렌즈(Lens): 정확히 하나의 요소에 초점
- 옵셔널(Optional): 0개 또는 1개의 요소에 초점
- 트래버설(Traversal): 임의의 수의 요소에 초점
- 프리즘(Prism) & 아이소(Iso): 값 생성과 패턴 매칭 가능

### 기술 요구사항

- 라이브러리: `arrow-optics`
- 컴파일러 플러그인: `arrow-optics-ksp-plugin`
- KSP 프레임워크 제한으로 인해 어노테이션이 달린 각 클래스는 빈 `companion object` 선언이 필요합니다.

---

## 렌즈 (Lenses)

### 개요

"렌즈는 가장 일반적으로 사용하는 옵틱 타입입니다." 렌즈는 데이터 필드에 대한 타입이 지정된 참조로 기능하며, 중첩된 구조에 대한 안전한 접근과 수정을 가능하게 합니다.

### 렌즈 타입

렌즈는 데이터 클래스의 `@optics` 어노테이션을 통해 생성됩니다. 제공된 예제를 사용하면:

```kotlin
import arrow.optics.*

@optics data class Person(val name: String, val age: Int, val address: Address) {
  companion object
}

@optics data class Address(val street: Street, val city: City) {
  companion object
}

@optics data class Street(val name: String, val number: Int?) {
  companion object
}

@optics data class City(val name: String, val country: String) {
  companion object
}
```

companion object에 생성된 렌즈는 다음 패턴을 따릅니다:

```kotlin
data class Person(val name: String, val age: Int, val address: Address) {
  companion object {
    val name: Lens<Person, String> = TODO()
    val age: Lens<Person, Int> = TODO()
    val address: Lens<Person, Address> = TODO()
  }
}
```

렌즈는 "타입이 지정됩니다" - 컨테이너 타입과 초점이 맞춰진 요소 타입을 모두 추적합니다.

### 핵심 연산

#### Get, Set, Modify

세 가지 핵심 연산이 있습니다:
1. `get` - 초점이 맞춰진 값을 가져옴
2. `set` - 초점이 맞춰진 값을 새 값으로 교체
3. `modify` - 함수를 사용하여 초점이 맞춰진 값을 변환

```kotlin
fun example() {
  val me = Person(
    "Alejandro", 35,
    Address(Street("Kotlinstraat", 1), City("Hilversum", "Netherlands"))
  )

  Person.name.get(me) shouldBe "Alejandro"

  val meAfterBirthdayParty = Person.age.modify(me) { it + 1 }
  Person.age.get(meAfterBirthdayParty) shouldBe 36

  val newAddress = Address(Street("Kotlinplein", null), City("Amsterdam", "Netherlands"))
  val meAfterMoving = Person.address.set(me, newAddress)
  Person.address.get(meAfterMoving) shouldBe newAddress
}
```

### 합성 (Composition)

렌즈의 뛰어난 기능은 중첩된 접근을 위한 렌즈 합성입니다. 중첩된 `copy` 호출을 연쇄하는 대신, 점 표기법을 사용하여 렌즈를 합성할 수 있습니다.

명시적 구문은 `compose`를 사용합니다:

```kotlin
val personCity: Lens<Person, String> =
  Person.address compose Address.city compose City.name

fun example() {
  val me = Person(
    "Alejandro", 35,
    Address(Street("Kotlinstraat", 1), City("Hilversum", "Netherlands"))
  )
  personCity.get(me) shouldBe "Hilversum"
  val meAtTheCapital = personCity.set(me, "Amsterdam")
}
```

컴파일러 플러그인은 더 깔끔한 구문을 위한 점 표기법을 활성화합니다:

```kotlin
fun example() {
  val me = Person(
    "Alejandro", 35,
    Address(Street("Kotlinstraat", 1), City("Hilversum", "Netherlands"))
  )
  Person.address.city.name.get(me) shouldBe "Hilversum"
  val meAtTheCapital = Person.address.city.name.set(me, "Amsterdam")
}
```

### 고급 Copy 연산

여러 필드 수정은 DSL 구문과 함께 `copy` 함수를 사용합니다:

```kotlin
fun Person.moveToAmsterdamCopy(): Person = copy {
  Person.address.city.name set "Amsterdam"
  Person.address.city.country set "Netherlands"
}
```

값 의존적 업데이트에는 `transform` 사용:

```kotlin
fun Person.capitalizeNameAndCountry(): Person = copy {
  Person.address.city.name transform { it.capitalize() }
  Person.address.city.country transform { it.capitalize() }
}
```

### inside 연산자

`inside` 함수는 공통 경로를 공유하는 연산을 압축하여, 여러 중첩 필드를 수정할 때 중복을 줄입니다:

```kotlin
fun Person.moveToAmsterdamInside(): Person = copy {
  inside(Person.address.city) {
    City.name set "Amsterdam"
    City.country set "Netherlands"
  }
}
```

### 봉인 클래스 계층 구조

렌즈는 봉인된 부모 인터페이스의 속성에 대해 자동으로 생성됩니다:

```kotlin
@optics sealed interface SUser {
  val name: String
  companion object
}

@optics data class SPerson(
  override val name: String,
  val age: Int): SUser {
  companion object
}

@optics data class SCompany(
  override val name: String,
  val vat: VATNumber): SUser {
  companion object
}
```

### Compose 통합

`arrow-optics-compose` 패키지는 `MutableState`를 위한 `updateCopy`를 제공합니다:

```kotlin
class AppViewModel: ViewModel() {
  private val _personData = mutableStateOf<Person>(...)

  fun updatePersonalData(
    newName: String, newAge: Int
  ) {
    _personData.updateCopy {
      Person.name set newName
      Person.age set newAge
    }
  }
}
```

참고: "`updateCopy`는 Compose의 스냅샷 시스템을 사용하여 블록 내의 모든 수정이 원자적으로 발생하도록 보장합니다."

---

## 옵셔널 (Optionals)

### 개요

Arrow Optics는 nullable 타입과 인덱싱된 컬렉션 요소를 포함하여 잠재적으로 누락된 값에 초점을 맞추기 위한 옵셔널을 제공합니다.

빠른 참조:
- 옵셔널은 잠재적으로 누락된 값을 나타냅니다
- 프리즘은 옵셔널을 확장하여 클래스 계층 구조를 나타냅니다
- `getOrNull`을 통해 값에 접근
- `set`과 `modify`를 사용하여 값 수정 (존재하는 경우에만)

### 인덱싱된 컬렉션

문서는 데이터베이스 매핑 시스템을 통해 옵셔널을 보여줍니다:

```kotlin
import arrow.optics.*
import arrow.optics.dsl.*
import arrow.optics.typeclasses.*

@optics data class Db(val cities: Map<String, City>) {
  companion object
}

@optics data class City(val name: String, val country: String) {
  companion object
}
```

map 값에 접근하기 위해 `index` 옵셔널 사용:

```kotlin
val db = Db(mapOf(
  "Alejandro" to City("Hilversum", "Netherlands"),
  "Ambrosio"  to City("Ciudad Real", "Spain")
))

Db.cities.index("Alejandro").country.getOrNull(db) // "Netherlands" 반환
Db.cities.index("Jack").country.getOrNull(db)      // null 반환
```

중요한 동작: 옵셔널을 통한 설정이나 수정은 기존 값에만 영향을 미칩니다. `index`를 사용하여 새 요소를 추가할 수 없습니다 - 존재하는 요소만 변환합니다.

### Nullable 타입

Arrow 1.x에서 2.x로의 주요 변경 사항:

- Arrow 2.x는 nullable 타입 필드에 대해 옵셔널 대신 렌즈를 생성합니다
- nullable 타입에 대한 렌즈를 옵셔널로 변환하려면 `notNull` 확장을 사용하세요
- `Lens<Person, String?>`를 `Optional<Person, String>`으로 변환
- "값이 null인지 여부를 변경할 수 없습니다; null이 아닌 경우에만 수정"

---

## 트래버설 (Traversals)

### 개요

트래버설은 구조 내에서 무한한 수의 값에 초점을 맞추도록 설계된 옵틱으로, 특히 컬렉션에 유용합니다. 문서에 따르면, "트래버설은 무한한 수의 값에 초점을 맞춥니다" 그리고 여러 요소로 작업하기 위한 컬렉션과 유사한 API를 제공합니다.

핵심 연산:
- `getAll`: 트래버설이 초점을 맞춘 모든 값에 접근
- `modify`: 트래버설이 초점을 맞춘 모든 값을 업데이트
- `isEmpty`, `size`: 중간 리스트 없이 트래버설 속성 확인

### 컬렉션 작업: `Every`

`Every` 객체는 컬렉션을 위한 기본 트래버설을 제공합니다. 사용된 예제 데이터 구조는 다음과 같습니다:

```kotlin
@optics data class Person(val name: String, val age: Int, val friends: List<Person>) {
  companion object
}
```

#### 기본 예제: Map vs. Optics

`map`을 사용한 전통적인 접근 방식:
```kotlin
fun List<Person>.happyBirthdayMap(): List<Person> =
  map { Person.age.modify(it) { age -> age + 1 } }
```

트래버설 사용:
```kotlin
fun List<Person>.happyBirthdayOptics(): List<Person> =
  Every.list<Person>().age.modify(this) { age -> age + 1 }
```

#### 합성된 트래버설

중첩된 컬렉션 업데이트:
```kotlin
fun Person.happyBirthdayFriendsOptics(): Person =
  Person.friends.every.age.modify(this) { it + 1 }
```

전통적인 접근 방식을 사용하면:
```kotlin
// 전통적인 접근 방식
copy(friends = friends.map { friend ->
  friend.copy(age = friend.age + 1)
})

// 옵틱스 접근 방식
Person.friends.every.age.modify(this) { it + 1 }
```

옵틱스 버전은 "값에 접근하는 경로"에 초점을 맞춤으로써 상용구 코드를 추상화합니다.

### 추가 연산

`getAll` 외에도 트래버설은 컬렉션과 유사한 연산을 지원합니다:
- `isEmpty`: 트래버설이 어떤 요소와 일치하는지 확인
- `size`: 일치하는 요소 수 계산

모든 연산은 "옵틱 우선" 패턴을 따르며, 값을 인수로 요구합니다.

참고: Arrow 2.0은 구문을 단순화했습니다 - `.every`는 개선된 타입 인코딩으로 인해 더 이상 명시적 타입 인수가 필요하지 않습니다.

---

## 프리즘과 아이소 (Prisms & Isos)

### 개요

프리즘은 검사 및 수정과 함께 값 생성을 가능하게 하기 위해 옵틱을 확장합니다. 봉인된 계층 구조 및 값 클래스와 함께 특히 유용합니다.

아이소(동형사상)는 정보가 양방향으로 손실 없이 흐르는 타입 간의 무손실 변환을 나타냅니다. 프리즘과 렌즈 기능을 결합합니다.

### 봉인 클래스 계층 구조

문서는 두 가지 구현이 있는 `User` 봉인 인터페이스를 보여줍니다:

```kotlin
@optics sealed interface User {
  companion object
}

@optics data class Person(val name: String, val age: Int): User {
  companion object
}

@optics data class Company(val name: String, val country: String): User {
  companion object
}
```

Arrow Optics 플러그인은 특정 타입에 초점을 맞추기 위해 `User.person`과 `User.company` 옵틱을 생성합니다. 이를 통해 타입 안전한 수정이 가능합니다:

```kotlin
fun List<User>.happyBirthday() =
  map { User.person.age.modify(it) { age -> age + 1 } }
```

사용 사례 예시: "사람은 나이가 증가하지만 회사는 변경되지 않음" - 사용자 목록에 생일 로직을 적용할 때.

### 값 생성

프리즘은 새 값을 생성하기 위한 `reverseGet` 연산을 도입합니다:

```kotlin
fun example() {
  val x = Prism.left<Int, String>().reverseGet(5)
  x shouldBe Either.Left(5)
}
```

"해당 프리즘을 생성자 대신 사용하여 Left 값을 만들 수 있습니다."

### 값 클래스

아이소는 값/인라인 클래스와의 원활한 변환을 가능하게 합니다:

```kotlin
@optics data class Person(val name: String, val age: Age) {
  companion object
}

@JvmInline @optics value class Age(val age: Int) {
  companion object
}

fun Person.happyBirthday(): Person =
  Person.age.age.modify(this) { it + 1 }
```

이를 통해 무손실 변환을 통해 타입 안전성을 유지하면서 도메인에 정확한 모델링이 가능합니다.

---

## 리플렉션 (Reflection)

### 개요

Arrow 라이브러리는 DSL과 `@optics` 속성을 사용할 수 없는 시나리오에서 Arrow Optics를 Kotlin의 리플렉션 기능과 연결하는 유틸리티 패키지 `arrow-optics-reflect`를 제공합니다.

### 핵심 참조와 렌즈

Kotlin은 `ClassName::memberName` 구문을 사용한 멤버 참조를 허용합니다. 리플렉션 패키지는 이를 확장하여 옵틱을 생성합니다:

```kotlin
data class Person(val name: String, val friends: List<String>)

Person::name.lens
```

### 실용적 적용

얻은 렌즈는 다른 렌즈처럼 작동합니다:

```kotlin
fun example() {
  val p = Person("me", listOf("pat", "mat"))
  val m = Person::name.lens.modify(p) { it.capitalize() }
  m.name shouldBe "Me"
}
```

중요한 제한: "이것은 public `copy` 메서드를 가진 `data` 클래스에서만 작동합니다 (기본값)." 옵틱은 기존 객체를 변경하는 대신 _새 복사본_을 생성합니다.

### 고급 패턴

Nullable과 컬렉션:
- nullable 필드에는 `optional` 사용
- `List` 타입에는 `every`, `Map` 타입에는 `values` 사용

```kotlin
fun example() {
  val p = Person("me", listOf("pat", "mat"))
  val m = Person::friends.every.modify(p) { it.capitalize() }
  m.friends shouldBe listOf("Pat", "Mat")
}
```

봉인 클래스를 위한 프리즘:
가이드는 `instance<ParentType, SubType>()`를 사용하여 봉인 클래스 계층 구조를 위한 프리즘 생성을 다룹니다:

```kotlin
sealed interface Cutlery
object Fork: Cutlery
object Spoon: Cutlery

val forks = Every.list<Cutlery>() compose instance<Cutlery, Fork>()
```

이를 통해 유니온 타입 내에서 초점이 맞춰진 탐색이 가능합니다.

---

## 정규 표현식 (Regular Expressions)

### 개요

이 페이지는 트리와 JSON 문서와 같은 계층적 데이터 구조를 쿼리하기 위한 Arrow의 옵틱 정규 표현식 패키지를 문서화합니다.

기본 옵틱의 제한: "렌즈, 프리즘, 기본 트래버설은 중요한 제한이 있습니다: 데이터 구조에서 _한 레벨만_ 깊이 들어갑니다."

`arrow.optics.regex` 패키지는 재귀적 순회 패턴을 가능하게 하여 이를 극복합니다.

### 반복 함수

Arrow는 옵틱의 재귀적 적용을 위한 `zeroOrMore`와 `onceOrMore`를 제공합니다:

```kotlin
@optics sealed interface BinaryTree1<out A> { companion object }

@optics data class Node1<out A>(
    val children: List<BinaryTree1<A>>) : BinaryTree1<A> {
    constructor(vararg children: BinaryTree1<A>) : this(children.toList())
    companion object
}

@optics data class Leaf1<out A>(
    val value: A) : BinaryTree1<A> {
    companion object
}

val exampleTree1 = Node1(Node1(Leaf1(1), Leaf1(2)), Leaf1(3))

fun example() {
    val path = zeroOrMore(BinaryTree1.node1<Int>().children().every).leaf1().value()
    val modifiedTree1 = path.modify(exampleTree1) { it + 1 }
    modifiedTree1 shouldBe Node1(Node1(Leaf1(2), Leaf1(3)), Leaf1(4))
}
```

### `and`를 사용한 조합

여러 레벨에 값이 있는 트리의 경우:

```kotlin
@optics sealed interface BinaryTree2<out A> { companion object }

@optics data class Node2<out A>(
    val innerValue: A,
    val children: List<BinaryTree2<A>>) : BinaryTree2<A> {
    constructor(value: A, vararg children: BinaryTree2<A>) : this(value, children.toList())
    companion object
}

@optics data class Leaf2<out A>(
    val value: A) : BinaryTree2<A> {
    companion object
}

val exampleTree2 = Node2(1, Node2(2, Leaf2(3), Leaf2(4)), Leaf2(5))

fun example() {
    val nodeValues = zeroOrMore(BinaryTree2.node2<Int>().children().every).node2().innerValue()
    val leafValues = zeroOrMore(BinaryTree2.node2<Int>().children().every).leaf2().value()
    val path = nodeValues and leafValues
    val modifiedTree2 = path.modify(exampleTree2) { it + 1 }
    modifiedTree2 shouldBe Node2(2, Node2(3, Leaf2(4), Leaf2(5)), Leaf2(6))
}
```

문서는 다음을 보여줍니다: "이진 트리에서 파고드는 것을 노드의 값을 보거나 리프의 값을 보는 것으로부터 분리할 수 있습니다."

### 왜 "정규 표현식"인가?

이 함수들은 정규 표현식 구문을 미러링합니다: 경로는 옵틱이 "문자"를 나타내는 "문자열"로 기능합니다.

| 함수 | 정규 표현식 패턴 |
|------|-----------------|
| `zeroOrMore` | `*` |
| `onceOrMore` | `+` |
| `and` | `\|` (대안) |

패턴 `(nce)*lv`는 "0개 이상의 node-children-every 시퀀스 다음에 leaf-value"를 나타내며, 이는 `zeroOrMore` 의미론에 직접 대응합니다.

---

## 요약

Arrow Optics는 Kotlin에서 불변 데이터를 다루기 위한 강력하고 타입 안전한 방법을 제공합니다:

| 옵틱 타입 | 초점 | 주요 용도 |
|----------|------|----------|
| 렌즈 (Lens) | 정확히 1개 | 필드 접근 및 수정 |
| 옵셔널 (Optional) | 0개 또는 1개 | nullable 값, 인덱싱된 접근 |
| 트래버설 (Traversal) | 0개 이상 | 컬렉션 작업 |
| 프리즘 (Prism) | 0개 또는 1개 + 생성 | 봉인 클래스 계층 구조 |
| 아이소 (Iso) | 정확히 1개 + 양방향 | 값 클래스, 타입 변환 |

### 핵심 이점

1. 상용구 코드 감소: 중첩된 `copy` 호출 제거
2. 타입 안전성: 컴파일 타임에 오류 감지
3. 합성 가능: 옵틱을 결합하여 복잡한 경로 생성
4. 선언적: "무엇"에 집중하고 "어떻게"는 추상화

### 시작하기

```gradle
// build.gradle.kts
plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("io.arrow-kt:arrow-optics:2.0.0")
    ksp("io.arrow-kt:arrow-optics-ksp-plugin:2.0.0")
}
```

자세한 내용은 [Arrow 공식 문서](https://arrow-kt.io/learn/immutable-data/)를 참조하세요.

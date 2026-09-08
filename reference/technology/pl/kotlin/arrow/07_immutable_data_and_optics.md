# 07. 불변 데이터와 Optics

- 확인일: 2026-09-08.
- 코드: 개념 설명용 발췌 예제 포함. 생략한 타입, import, 프로젝트 설정이 있는 예제는 독립 실행 프로그램이 아님.
- 원본 문서와 코드의 라이선스: [Apache-2.0](LICENSE.arrow-website).

[전체 목차](01_introduction_and_setup.md)


## 수록한 공식 문서

- [immutable-data](https://arrow-kt.io/learn/immutable-data/)
- [immutable-data/intro](https://arrow-kt.io/learn/immutable-data/intro/)
- [immutable-data/lens](https://arrow-kt.io/learn/immutable-data/lens/)
- [immutable-data/optional](https://arrow-kt.io/learn/immutable-data/optional/)
- [immutable-data/traversal](https://arrow-kt.io/learn/immutable-data/traversal/)
- [immutable-data/prism-iso](https://arrow-kt.io/learn/immutable-data/prism-iso/)
- [immutable-data/reflection](https://arrow-kt.io/learn/immutable-data/reflection/)
- [immutable-data/regex](https://arrow-kt.io/learn/immutable-data/regex/)

## 소개 (Introduction)

> 원문: [immutable-data/intro](https://arrow-kt.io/learn/immutable-data/intro/)

### 개요

Kotlin의 불변 데이터에서 깊게 중첩된 필드를 바꾸려면 각 단계의 `copy`를 호출하면서 필드 이름을 반복해야 합니다. Arrow의 옵틱스(optics)는 필드에 접근하는 경로를 표현해 이런 변환을 간단하게 만듭니다.

### 핵심 문제

다음은 주소와 도시 정보가 중첩된 `Person` 데이터 클래스입니다:

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

Arrow의 옵틱은 초점으로 삼는 요소의 수에 따라 계층을 이룹니다.

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

> 원문: [immutable-data/lens](https://arrow-kt.io/learn/immutable-data/lens/)

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

`Lens<Person, String>`은 전체 데이터가 `Person`이고 초점이 맞춰진 값은 `String`임을 나타냅니다. 렌즈는 이렇게 두 타입을 함께 추적합니다.

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

렌즈를 합성하면 여러 필드를 거쳐 중첩된 값에 접근할 수 있습니다. 아래에서는 `compose`로 `Person`의 주소, 도시, 이름을 연결해 도시 이름을 가리키는 렌즈를 만듭니다:

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

컴파일러 플러그인을 사용하면 점 표기법으로 옵틱을 간결하게 연결할 수 있습니다.

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

여러 필드를 수정할 때는 `copy` 함수의 DSL을 사용합니다.

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

같은 경로 아래의 여러 필드를 수정한다면 `inside`로 공통 경로를 한 번만 지정할 수 있습니다. 아래에서는 `Person.address.city` 안에서 도시 이름과 국가를 바꿉니다:

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

> 원문: [immutable-data/optional](https://arrow-kt.io/learn/immutable-data/optional/)

### 개요

Arrow Optics의 옵셔널은 nullable 값이나 인덱스로 찾는 컬렉션 요소처럼 존재하지 않을 수도 있는 값에 접근할 때 사용합니다.

빠른 참조:

- 옵셔널은 잠재적으로 누락된 값을 나타냅니다
- 프리즘은 옵셔널을 확장하여 클래스 계층 구조를 나타냅니다
- `getOrNull`을 통해 값에 접근
- `set`과 `modify`를 사용하여 값 수정 (존재하는 경우에만)

### 인덱싱된 컬렉션

아래 `Db`는 이름을 키로 도시를 저장합니다. 키에 해당하는 도시가 없을 수도 있으므로 옵셔널로 접근합니다:

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

옵셔널의 `set`과 `modify`는 값이 존재할 때만 적용됩니다. 따라서 `index`로 새 요소를 추가할 수는 없으며, 위 예시에서도 키에 해당하는 기존 요소만 변환할 수 있습니다.

### Nullable 타입

Arrow 1.x에서 2.x로의 주요 변경 사항:

- Arrow 2.x는 nullable 타입 필드에 대해 옵셔널 대신 렌즈를 생성합니다
- nullable 타입에 대한 렌즈를 옵셔널로 변환하려면 `notNull` 확장을 사용하세요
- `Lens<Person, String?>`를 `Optional<Person, String>`으로 변환
- "값이 null인지 여부를 변경할 수 없습니다; null이 아닌 경우에만 수정"

---

## 트래버설 (Traversals)

> 원문: [immutable-data/traversal](https://arrow-kt.io/learn/immutable-data/traversal/)

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

> 원문: [immutable-data/prism-iso](https://arrow-kt.io/learn/immutable-data/prism-iso/)

### 개요

프리즘은 검사 및 수정과 함께 값 생성을 가능하게 하기 위해 옵틱을 확장합니다. 봉인된 계층 구조 및 값 클래스와 함께 특히 유용합니다.

아이소(동형사상)는 두 타입 사이에서 정보를 잃지 않고 양방향으로 변환하는 방법을 나타냅니다. 프리즘과 렌즈의 기능을 결합합니다.

### 봉인 클래스 계층 구조

다음 `User` 봉인 인터페이스에는 `Person`과 `Company`라는 두 구현이 있습니다:

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

이 함수는 사용자 목록에서 `Person`의 나이만 늘리고 `Company`는 그대로 둡니다. 프리즘으로 수정할 타입을 먼저 선택했기 때문입니다.

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

아이소를 사용하면 값/인라인 클래스와 다른 타입 사이를 양방향으로 변환할 수 있습니다.

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

`Age`라는 도메인 타입을 유지하면서 그 안의 정수에 접근해 나이를 갱신할 수 있습니다. 아이소가 두 표현 사이를 손실 없이 변환합니다.

---

## 리플렉션 (Reflection)

> 원문: [immutable-data/reflection](https://arrow-kt.io/learn/immutable-data/reflection/)

### 개요

DSL과 `@optics` 애너테이션을 사용할 수 없다면 `arrow-optics-reflect`로 Kotlin의 리플렉션 기능을 연결할 수 있습니다. 이 패키지는 멤버 참조에서 옵틱을 만드는 유틸리티를 제공합니다.

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

> 원문: [immutable-data/regex](https://arrow-kt.io/learn/immutable-data/regex/)

### 개요

트리나 JSON 문서에서 같은 경로를 여러 깊이에 걸쳐 반복해서 따라가야 할 때는 옵틱 정규 표현식을 사용할 수 있습니다.

기본 옵틱의 제한: "렌즈, 프리즘, 기본 트래버설은 중요한 제한이 있습니다: 데이터 구조에서 _한 레벨만_ 깊이 들어갑니다."

`arrow.optics.regex` 패키지는 같은 경로를 재귀적으로 순회하는 패턴으로 이 제한을 해결합니다.

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

`nodeValues`와 `leafValues`는 같은 경로를 따라 트리를 내려가되 서로 다른 값을 선택합니다. 두 경로를 `and`로 묶으면 노드와 리프의 값을 함께 수정할 수 있습니다.

### 왜 "정규 표현식"인가?

이 함수들의 구문은 정규 표현식과 대응합니다. 각 옵틱을 문자 하나로 보면, 옵틱을 이어 붙인 경로는 문자열에 해당합니다.

- `zeroOrMore`: 정규 표현식 패턴: `*`
- `onceOrMore`: 정규 표현식 패턴: `+`
- `and`: 정규 표현식 패턴: `|` (대안)

패턴 `(nce)*lv`는 "0개 이상의 node-children-every 시퀀스 다음에 leaf-value"를 나타내며, 이는 `zeroOrMore` 의미론에 직접 대응합니다.

---

## 시작하기

```gradle
// build.gradle.kts
plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("io.arrow-kt:arrow-optics:2.2.3")
    ksp("io.arrow-kt:arrow-optics-ksp-plugin:2.2.3")
}
```

자세한 내용은 [Arrow 공식 문서](https://arrow-kt.io/learn/immutable-data/)를 참조하세요.

# Scala 3의 enum

> 원문: https://rockthejvm.com/articles/enums-in-scala-3

## 대상 독자

이 글은 모든 수준의 Scala 프로그래머를 위한 것이며, 특히 Scala 2에서 오랫동안 enum을 흉내 내며 써왔던 분들에게 유용하다.

## 도입

드디어 그날이 왔다. Scala 3부터 enum을 기본 지원한다. 이 글에서는 바로 이 기능을 다룬다.

이 기능은 (그 외 수십 가지 변경사항과 함께) Scala 3 New Features 과정에서 깊이 있게 설명한다.

## 배경

Scala 2에는 enum 지원이 없었다. 억지로 구현하려면 이런 식으로 해야 했다:

```scala
object Permissions extends Enumeration {
  val READ, WRITE, EXEC, NONE = Value
}
```

대체 이게 뭔가? `Value`라니?!

여기서 자세히 파고들 생각은 없다. 애초에 말이 안 되는 구조다. 아마 Scala 2에서 가장 기이한 부분 중 하나였을 것이다.

## enum 등장

마침내 Scala에도 다른 표준 프로그래밍 언어처럼 일급(first-class) enum이 생겼다:

```scala
enum Permissions {
    case READ, WRITE, EXEC
}
```

끝이다. `case`가 꼭 필요했는지 아닌지는 논란의 여지가 있지만, 너무 까다롭게 굴 필요는 없다. 이제 enum이 일급 시민이고, Java나 다른 언어처럼 사용할 수 있다:

```scala
val read: Permissions = Permissions.READ
```

내부적으로 컴파일러는 sealed class 하나와, 그 companion object 안에 잘 정의된 3개의 값을 생성한다.

## 인자가 있는 enum

예상대로 enum에 인자를 줄 수도 있다. 이 경우 각 상수는 주어진 표현식과 함께 선언해야 한다:

```scala
enum PermissionsWithBits(bits: Int) {
    case READ extends PermissionsWithBits(4) // 이진수 100
    case WRITE extends PermissionsWithBits(2) // 이진수 010
    case EXEC extends PermissionsWithBits(1) // 이진수 001
    case NONE extends PermissionsWithBits(0)
}
```

구문이 약간 장황하긴 하다. 감싸고 있는 enum 외에 다른 것을 확장할 수 없다는 제약도 있지만, 불평할 수준은 아니다. 위 예제에서는 4개의 값을 가진 enum이 있고, 물론 일반적인 접근자 문법으로 필드에 접근할 수 있다.

## 필드와 메서드

enum은 일반 클래스처럼 필드와 메서드를 가질 수 있다. 어차피 컴파일하면 sealed class가 되니까. enum 본문 안에 정의하면 되고, 보통의 dot 접근자 문법으로 접근한다.

```scala
enum PermissionsWithBits(bits: Int) {
    // case 정의는 여기에

    def toHex: String = Integer.toHexString(bits) // Java 스타일 구현
    // val 등 다른 멤버도 정의할 수 있다
}
```

재미있는 점은 enum 안에 변수(`var`)도 정의할 수 있다는 것이다. 하지만 이는 enum의 불변성과 충돌할 수 있다. enum 안에 변수를 만드는 것은 추천하지 않는다. 코드 어디에서든 누구나 바꿀 수 있는 전역 변수를 만드는 것과 마찬가지이기 때문이다.

enum에 companion object를 만들 수 있다는 점도 좋다. 여기에 "정적" 필드와 메서드, 혹은 "스마트" 생성자를 정의할 수 있다:

```scala
object PermissionsWithBits {
    def fromBits(bits: Int): PermissionsWithBits = // 비트 검사 로직
      PermissionsWithBits.NONE
}
```

## 표준 API

enum에는 미리 정의된 유틸리티 메서드가 몇 가지 있다.

첫째, 주어진 enum 값이 case 정의 순서에서 몇 번째인지 확인하는 기능이다:

```scala
// 정수로 변환하고 싶다면 (enum 인스턴스의 정의 순서)
val indexOfRead = Permissions.READ.ordinal
```

둘째, enum 타입의 모든 가능한 값을 가져오는 기능이다. 순회하거나 한꺼번에 다룰 때 유용하다:

```scala
val allPermissions = Permissions.values
```

셋째, `String`을 enum 값으로 변환하는 기능이다:

```scala
val readPermission = Permissions.valueOf("READ")
```

## 마무리

여기까지다. Scala 3은 이제 다른 많은 언어와 마찬가지로 enum을 정의할 수 있게 되었다. enum을 안전하게 정의하고 사용하는 법, 매개변수와 메서드 및 필드를 추가하는 법, 그리고 미리 정의된 API를 활용하는 법을 알게 되었을 것이다.

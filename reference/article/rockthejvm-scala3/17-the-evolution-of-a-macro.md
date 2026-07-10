> 원본: https://rockthejvm.com/articles/scala-3-the-evolution-of-a-macro

# Scala 3: 컴파일 타임 데이터 계약

## Introduction

파이프라인이 고장 나기 전에, 컴파일러가 먼저 막아준다면?

> "컴파일되면 데이터 계약도 맞는다." Scala 3에서 스키마 불일치를 컴파일 타임에 잡아보자.

이 문제에 한동안 매달려왔다. 다들 겪는 상황인데, 데이터 파이프라인을 운영하다가 누군가 상류에서 필드명을 바꾸면 갑자기 새벽 2시에 잡이 터진다. 온콜 엔지니어(아마 당신)가 호출당하고, 한 시간 동안 원인을 찾아 헤맨다.

한번은 상류 팀이 조용히 `amount` 컬럼을 `amt`로 바꿔버렸다. 파이프라인이 바로 터지지 않아서 몇 주간 null을 계속 기록했다. 발견했을 때는 수백만 건의 레코드를 백필해야 했다. 그날 깨달았다: 런타임 검증은 문제를 늦게 잡고, 컴파일 타임 계약은 일찍 잡는다.

이 글에서는 컴파일 타임에 이런 문제를 잡는 방법을 보여준다. 런타임도 아니고, 테스트 타임도 아니고, 컴파일 타임이다. 스키마가 어긋나면 코드 자체가 빌드되지 않는다.

처음부터 만들어 볼 건데, 먼저 Scala 3 매크로의 동작 방식을 이해한 다음 스키마 호환성을 코드 실행 전에 증명하는 최소한의 계약 시스템을 작성한다. Scala 2 버전도 함께 보여줄 테니 아직 Scala 2를 쓰는 분들도 참고하면 된다.

모든 코드는 일반적인 sbt 프로젝트에서 돌아간다. [GitHub](https://github.com/vim89/compile-time-data-contracts)에서 동작하는 코드를 받아 직접 실행해볼 수 있다.

## 왜 중요한가

테스트 작성은 중요하다. 하지만 테스트는 샘플일 뿐이다. 깨질 것 같은 부분만 검사한다. 반면 컴파일러는 *구조에 대해 빠짐없이 검사*한다. 모든 필드, 모든 타입이 매번 일치하는지 증명할 수 있다.

실제 프로젝트에서 스키마는 수시로 바뀐다. 그때 이런 원칙이 필요하다:

- 프로덕션 잡이 아니라 빌드가 먼저 실패해야 한다
- 변환은 순수하게 유지하고 부수 효과는 경계에 둔다(재시도 시 중복 쓰기 방지)
- 컴파일러가 무거운 일을 맡게 한다

> **적용 범위**
>
> 컴파일 타임 계약은 직접 관리하는 코드베이스 내의 drift를 잡는다. 작성하는 변환, 정의하는 case class, 함께 버전 관리하는 스키마가 대상이다. 상류 API 변경, 뒤늦게 업데이트되는 스키마 레지스트리, 독립적으로 변경되는 외부 데이터 소스는 잡지 못한다. 직접 관리하는 것은 컴파일 타임으로, 나머지는 런타임 검증으로 방어한다. 둘 다 하는 것이지, 양자택일이 아니다.

## 무엇을 만들 것인가

간단한 컴파일 타임 검증 시스템을 만든다:

- case class 구조를 컴파일 타임에 기술하는 방법 ("shape"이라 부르겠다)
- 정책 시스템 - Exact, Backward, Forward 호환성 모드
- "이 두 shape가 정책 P 하에서 적합한지" 증명하거나 컴파일을 거부하는 매크로
- 올바른 조립 순서를 강제하는 phantom type 기반 파이프라인 빌더

먼저 Scala 3(inline + quotes), 다음으로 Scala 2(blackbox macros) 순서로 진행한다. 차이점은 그때그때 짚어주겠다.

### GitHub에서 코드 실행하기

```bash
git clone https://github.com/vim89/compile-time-data-contracts.git
cd compile-time-data-contracts

# Scala 3
sbt "runMain ctdc.CtdcPoc"
```

> **참고:** GitHub의 코드에는 바로 실행 가능한 예제가 포함되어 있다. 컴파일 실패 케이스는 인라인으로 문서화되어 있으므로, 주석을 해제하면 컴파일러 오류를 확인할 수 있다.

## Part 1 - Scala 3: 기본 개념 (inline + quotes)

Scala 3 매크로는 Scala 2와 다르다. 솔직히 더 낫다. 더 안전하고 추론하기도 쉽다.

핵심 아이디어는 두 가지다:

- `inline def`: 컴파일러에게 "컴파일 타임에 전개하라"고 알려준다
- `quotes` + `Expr[T]`: 검사할 수 있는 타입 기반 AST를 제공한다

동작 방식을 보기 위한 최소 매크로 예시:

IntroMacro.scala
```scala
// scalaVersion := "3.3.3" in build.sbt
package com.example
import scala.quoted.*

object IntroMacro {
  inline def show(inline x: Any): String = ${ showImpl('x) }
  private def showImpl(x: Expr[Any])(using Quotes): Expr[String] = {
    Expr(s"You passed: ${x.show}")
  }
}
```

여기서 무슨 일이 일어나는가?

- `inline` 메서드가 사용자가 호출하는 부분이다
- `${ ... }` splice가 구현부를 호출한다
- 구현부는 `'x`(quoted code)를 `Expr[Any]`로 받는다
- `Expr[String]`을 반환한다. 이것이 컴파일러가 내장할 AST다

사용해보자:

Demo.scala
```scala
package com.example
@main def run(): Unit =
  println(IntroMacro.show(1 + 2)) // prints: You passed: 1.+(2)
```

`1 + 2`의 AST가 출력된 것을 보라. 이것이 핵심이다. 컴파일 타임에 코드 구조를 들여다볼 수 있다.

자세한 내용은 [Scala 3 macros overview](https://docs.scala-lang.org/scala3/guides/macros/macros.html), [best practices](https://docs.scala-lang.org/scala3/guides/macros/best-practices.html), 그리고 [Rock the JVM Macros & Metaprogramming course](https://rockthejvm.com/courses/scala-macros-and-metaprogramming)를 참고하라.

> **빠른 팁:** Scala 3 매크로를 처음 접한다면, REPL에서 매크로가 생성한 코드를 읽는 것부터 시작하자. `sbt> console`에서 `scala> import scala.quoted.*`를 입력한 뒤, 매크로가 실제로 어떻게 전개되는지 살펴보면 된다. 생성된 코드를 이해하는 것이 매크로 디버깅의 80%다. 컴파일러가 일하고 있으니, 실제로 뭘 만들어내는지만 확인하면 된다.

## Part 2 - 컴파일 타임에 shape 기술하기 (Scala 3)

이제 case class 구조를 기술해야 한다. 실제 프로젝트에서는 Shapeless, Magnolia, Scala 3의 Mirror를 쓸 수 있다. 여기서는 자체 완결적인 Mirror 기반 간결한 버전을 보여주겠다.

아이디어: `A`의 필드를 아는 typeclass `Shape[A]`.

Shape.scala
```scala
package com.example

import scala.deriving.Mirror

final case class Field(name: String, tpe: String, hasDefault: Boolean = false, isOptional: Boolean = false)
trait Shape[A] {
  def fields: List[Field]
}

object Shape {
  given Shape[String] with { def fields = Nil }
  given Shape[Int]     with { def fields = Nil }
  given Shape[Long]    with { def fields = Nil }
  given [A]: Shape[Option[A]] with { def fields = Nil } // element optionality는 별도 처리

  inline given derived[A](using m: Mirror.Of[A]): Shape[A] = {
    inline m match {
      case p: Mirror.ProductOf[A] =>
        val labels = constValueTuple[p.MirroredElemLabels]
        val types  = typeNames[p.MirroredElemTypes]
        val zipped = zip(labels, types)
        new Shape[A] {
          def fields: List[Field] =
            zipped.map { (name, tpe) => Field(name, tpe, hasDefault = false, isOptional = tpe.startsWith("Option[")) }.toList
        }
      case _ => new Shape[A] { def fields = Nil }
    }
  }

  import scala.compiletime.{erasedValue, constValueTuple}
  private inline def typeNames[T <: Tuple]: List[String] = inline erasedValue[T] match {
    case _: EmptyTuple => Nil
    case _: (h *: t)   => summonTypeName[h] :: typeNames[t]
  }

  private inline def summonTypeName[T]: String = constValue["" + T]

  private inline def zip[L <: Tuple, R <: List[String]](labels: L, types: List[String]): List[(String, String)] =
    inline labels match {
      case _: EmptyTuple => Nil
      case _: (h *: t)   => constValue[h].asInstanceOf[String] -> types.head :: zip[t, List[String]](erasedValue, types.tail)
    }
```

꽤 빽빽하지만 하는 일은 이렇다:

- Scala 3의 Mirror를 사용해 컴파일 타임에 case class를 리플렉션한다
- 필드명과 타입을 추출한다
- optional 필드(`Option`으로 감싸진 것)를 표시한다
- 구조를 기술하는 `List[Field]`를 반환한다

사용법:

ShapeSpec.scala
```scala
package com.example

final case class User(id: Long, email: String, note: Option[String])

@main def checkShape(): Unit =
  val s = summon[Shape[User]]
  println(s.fields) // List(Field("id","Long"), Field("email","String"), Field("note","Option[String]",false,true))
```

이제 컴파일러에게 "이 case class에 어떤 필드가 있는가?"라고 컴파일 타임에 물어볼 수 있다. 이것이 기반이다.

> **대안:** 라이브러리를 선호한다면 [Magnolia](https://github.com/softwaremill/magnolia)를 확인해보라. Scala 2와 3 모두 잘 동작한다.

## Part 3 - 정책: Exact / Backward / Forward

호환성 규칙을 정의하는 부분이다. 마이그레이션 전략이라 생각하면 된다:

Policy.scala
```scala
package com.example

sealed trait Policy
object Policy {
  sealed trait Exact     extends Policy
  sealed trait Backward  extends Policy // producer가 optional/기본값 필드를 추가 가능
  sealed trait Forward   extends Policy // consumer가 추가 필드를 허용
  case object Exact    extends Exact
  case object Backward extends Backward
  case object Forward  extends Forward
}
```

왜 세 가지 정책인가?

- **Exact**: 스키마가 정확히 일치해야 한다. 예외 없다. 핵심 경로에 사용한다.
- **Backward**: Producer가 optional 필드를 추가할 수 있다. consumer를 깨뜨리지 않고 새 기능을 배포할 때 유용하다.
- **Forward**: Consumer가 추가 필드를 무시한다. producer가 필요 이상으로 보낼 수 있을 때 유용하다.

> **프로덕션 마이그레이션 전략**
>
> 실제로 잘 동작하는 정책 순서: 안정적인 파이프라인에는 Exact, producer 롤아웃 중에는 Backward(새 optional 필드 허용), consumer 업데이트 중에는 Forward(추가 필드 허용), 마이그레이션 완료 후 다시 Exact. 예상 일정을 문서화하라. "Q2 마이그레이션 동안 Backward 정책, 이후 Exact로 강화." 이렇게 해야 정책 drift가 영구화되는 것을 방지할 수 있다.

## Part 4 - 기존 도구와의 관계

더 들어가기 전에 이런 생각이 들 수 있다: "이미 [Avro](https://avro.apache.org/), [Protobuf](https://protobuf.dev/), 스키마 레지스트리, [Great Expectations](https://greatexpectations.io/)가 있는데 왜 컴파일 타임 계약을 추가하나?"

합리적인 질문이다. 비교해보자:

**Schema Registry ([Confluent Schema Registry](https://docs.confluent.io/platform/current/schema-registry/index.html), [AWS Glue Schema Registry](https://docs.aws.amazon.com/glue/latest/dg/schema-registry.html))**

- **하는 일**: 버전 관리가 되는 중앙 집중식 스키마 저장소
- **도움이 되는 경우**: 서비스 간 런타임 검증, 스키마 진화 추적
- **빈틈**: 불일치를 잡이 실행될 때 발견한다. 컴파일할 때가 아니다
- **함께 사용**: 서비스 간 계약에는 스키마 레지스트리, 리포지토리 내부 검사에는 컴파일 타임

**[Avro](https://avro.apache.org/) / [Protobuf](https://protobuf.dev/)**

- **하는 일**: 내장 스키마가 있는 바이너리 직렬화
- **도움이 되는 경우**: 네트워크 효율성, 서비스 간 강력한 계약
- **빈틈**: 스키마 호환성 검사가 런타임이나 외부 도구로 이뤄진다
- **함께 사용**: 와이어 포맷에는 Avro/Protobuf, case class가 해당 스키마와 맞는지 확인하는 데는 컴파일 타임

**[Great Expectations](https://greatexpectations.io/) / [Deequ](https://github.com/awslabs/deequ)**

- **하는 일**: 런타임 데이터 품질 검증 (null, 범위, 분포)
- **도움이 되는 경우**: 잘못된 데이터 값, 통계적 이상 탐지
- **빈틈**: 데이터 내용은 검증하지만 스키마 구조를 컴파일 타임에 검증하지 않는다
- **함께 사용**: 구조 검증에는 컴파일 타임, 데이터 품질에는 Great Expectations

**컴파일 타임의 장점**: 배포 전에 관리 대상 코드베이스의 drift를 잡는다. 변환이 필드 `X`를 기대하는데 소스가 필드 `Y`를 생성하면 빌드가 실패한다. 프로덕션 잡이 아니라. 함께 빌드하고 버전 관리하는 시스템을 위한 추가 안전 계층이라 생각하면 된다.

실제 시나리오: 레지스트리에 Avro 스키마가 있고, Scala 파이프라인이 이를 case class로 읽는다. 컴파일 타임 계약은 배포 전에 해당 case class가 기대 계약과 맞는지 검증한다. 상류에서 Avro 스키마를 바꾸면 빌드가 깨진다. 잡이 크래시할 때까지 기다리지 않는다.

## Part 5 - TypeRepr 이해하기: 타입에 대한 컴파일러의 관점

매크로에 들어가기 전에 `TypeRepr`를 이해해야 한다. 컴파일 중에 타입을 표현하는 Scala 3의 방식이다.

이렇게 생각하면 된다: `List[String]`을 쓰면 컴파일러는 단순한 텍스트가 아니라 구조화된 트리를 본다:

```
AppliedType(List, [String])
 │ │
 │ └─ TypeRepr for String
 └─ Type constructor
```

**주요 TypeRepr 연산:**

```scala
val t: TypeRepr = TypeRepr.of[List[String]]

// 1. 서브타입 검사: List[String]이 Seq[?]의 서브타입인가?
t <:< TypeRepr.of[Seq[?]] // true

// 2. 타입 동등성: 이것이 정확히 String인가?
t =:= TypeRepr.of[String] // false

// 3. 패턴 매칭: 분해하기
t match {
  case AppliedType(tycon, args) =>
    // tycon = List
    // args = [String]
}
```

**왜 필요한가:** 스키마를 비교하려면 이런 질문을 던져야 한다:

- "이 필드가 Option인가?"
- "이 List 안에 뭐가 들었는가?"
- "이 case class에 primary constructor가 있는가?"

`TypeRepr`로 이런 질문에 컴파일 타임에 답할 수 있다.

## Part 6 - TypeInspector 패턴: 질문 하나씩

이제 유틸리티를 만들어보자. 각 함수는 타입에 대해 딱 하나의 질문에 답한다. 단순한 것부터 시작한다:

### Inspector 1: case class인가?

```scala
def isCaseClass(t: TypeRepr): Boolean =
  val s = t.typeSymbol // 타입의 심볼(타입의 "이름") 가져오기
  s.isClassDef && s.flags.is(Flags.Case)
```

**무슨 일이 일어나는가:**

- `typeSymbol` - 모든 타입에는 심볼(고유 ID 같은 것)이 있다
- `isClassDef` - 클래스인가? (trait, object 등이 아니라)
- `flags.is(Flags.Case)` - `case` 수정자가 있는가?

**시도해보자:**

```scala
case class User(id: Long) // YES - isClassDef=true, Case 플래그 있음
trait UserTrait           // NO - isClassDef=false
object UserObject         // NO - 클래스가 아님
```

### Inspector 2: 타입 인자는 무엇인가?

```scala
def appliedArgs(t: TypeRepr): List[TypeRepr] = t match {
  case AppliedType(_, args) => args // 인자 추출
  case _ => Nil                     // 인자 없음
}
```

**무슨 일이 일어나는가:**

- `AppliedType` - `List[String]`이나 `Map[String, Int]` 같은 제네릭 타입 패턴
- `args` - 타입 파라미터: `[String]` 또는 `[String, Int]`

**예시:**

```scala
List[String]    // AppliedType(List, [String])
                // appliedArgs returns [String]

Map[String, Int] // AppliedType(Map, [String, Int])
                 // appliedArgs returns [String, Int]

String           // AppliedType이 아님
                 // appliedArgs returns Nil
```

### Inspector 3: Option인가?

```scala
def optionArg(t: TypeRepr): Option[TypeRepr] =
  if t <:< TypeRepr.of[Option[?]] then // Option[Something]의 서브타입인가?
    appliedArgs(t).headOption           // 그렇다! Something 추출
  else None                             // 아니다
```

**무슨 일이 일어나는가:**

- `<:<` - 서브타입 검사: "이 타입이 Option[?]과 호환되는가?"
- `Option[?]` - `?`는 "아무거나 담은 Option"을 의미한다
- `appliedArgs(t).headOption` - 첫 번째(유일한) 타입 인자를 가져온다

**예시:**

```scala
Option[String]  // YES - Option[?]의 서브타입
                // appliedArgs returns [String]
                // headOption returns Some(String)

Some[Int]       // YES - Some은 Option의 서브타입
                // Returns Some(Int)

String          // NO - Option이 아님
                // Returns None
```

**왜 필요한가:** 필드 수준의 optionality 때문이다. `age: Option[Int]`를 보면 다음을 알아야 한다:

- 필드 자체가 optional이다
- 기저 타입은 `Int`이다

### Inspector 4: 시퀀스인가?

```scala
def seqArg(t: TypeRepr): Option[TypeRepr] =
  val isSeqLike =
    t <:< TypeRepr.of[List[?]]   ||
    t <:< TypeRepr.of[Seq[?]]    ||
    t <:< TypeRepr.of[Vector[?]] ||
    t <:< TypeRepr.of[Array[?]]  ||
    t <:< TypeRepr.of[Set[?]]
  if isSeqLike then appliedArgs(t).headOption else None
```

**무슨 일이 일어나는가:**

- 어떤 종류든 시퀀스 타입인지 확인한다
- 맞으면 내부의 것(요소 타입)을 추출한다

**왜 여러 타입을 검사하나?** Scala에는 컬렉션 타입이 많기 때문이다:

```scala
List[String]   // Is List[?]   YES
Seq[Int]       // Is Seq[?]    YES
Vector[Long]   // Is Vector[?] YES
Array[Byte]    // Is Array[?]  YES
Set[String]    // Is Set[?]    YES
```

이들 모두 `SequenceShape(element)`가 되어야 한다.

### Inspector 5: Map인가?

```scala
def mapArgs(t: TypeRepr): Option[(TypeRepr, TypeRepr)] =
  if t <:< TypeRepr.of[Map[?, ?]] then {
    appliedArgs(t) match {
      case k :: v :: Nil => Some((k, v)) // key와 value를 얻었다
      case _ => report.errorAndAbort(s"Map requires two type args: ${t.show}")
    }
  } else None
```

**무슨 일이 일어나는가:**

- `Map[Something, Something]`인지 확인한다
- 타입 인자 두 개를 모두 추출한다: key와 value
- Map의 인자 수가 틀리면 중단한다 (컴파일러 오류)

**예시:**

```scala
Map[String, Int]  // true - Returns Some((String, Int))
Map[Long, User]   // true - Returns Some((Long, User))
String            // false - Returns None
```

### Inspector 6: Map key로 쓸 수 있는가?

```scala
def isAtomicKey(t: TypeRepr): Boolean =
  t =:= TypeRepr.of[String]  ||
  t =:= TypeRepr.of[Int]     ||
  t =:= TypeRepr.of[Long]    ||
  t =:= TypeRepr.of[Short]   ||
  t =:= TypeRepr.of[Byte]    ||
  t =:= TypeRepr.of[Boolean]
```

**왜 중요한가:** Spark StructType의 Map은 원시 타입 key만 지원한다. `Map[User, String]`은 안 된다. User는 원시 타입이 아니다.

**예시:**

```scala
Map[String, User]  // YES - String은 atomic
Map[Int, Data]     // YES - Int는 atomic
Map[User, String]  // NO - User는 atomic이 아님 - 컴파일 오류!
```

### 패턴 정리:

각 inspector는 딱 하나의 역할을 한다:

- **isCaseClass** - "case class인가?"
- **appliedArgs** - "제네릭 타입 인자가 무엇인가?"
- **optionArg** - "Option인가, 그리고 안에 뭐가 있는가?"
- **seqArg** - "시퀀스인가, 그리고 요소 타입이 무엇인가?"
- **mapArgs** - "Map인가, key/value 타입이 무엇인가?"
- **isAtomicKey** - "Map key로 쓸 수 있는가?"

이들을 조합해서 복잡한 질문에 답한다. 이것이 TypeInspector 패턴이다.

## Part 7 - 재귀적으로 shape 만들기: 인터프리터 패턴

이제 앞의 inspector들을 사용해 `TypeShape`을 만든다. 타입 구조의 내부 표현이다.

이건 재귀적이다. `List[Option[User]]`의 경우 이렇게 만든다:

```
SequenceShape(
  OptionalShape(
    StructShape([field1, field2, ...])
  )
)
```

알고리즘을 단계별로 보여주겠다.

### shape 빌드 알고리즘:

```scala
def typeShapeOf(t: TypeRepr): TypeShape =
  // Step 1: Option인가?
  optionArg(t).map(typeShapeOf).getOrElse {

    // Step 2: 시퀀스인가?
    seqArg(t).map(a => SequenceShape(typeShapeOf(a))).getOrElse {

      // Step 3: Map인가?
      mapArgs(t).map { case (k, v) =>
        if !isAtomicKey(k) then {
          report.errorAndAbort(s"Unsupported Map key: ${k.show}")
        }
        MapShape(PrimitiveShape(k.show), typeShapeOf(v))
      }.getOrElse {

        // Step 4: case class인가?
        if isCaseClass(t) then {
          // ... case class 처리
        } else {
          // Step 5: 폴백 - 원시 타입
          PrimitiveShape(t.show)
        }
      }
    }
  }
```

**순서가 중요하다!** Option을 먼저 검사하고, 그다음 시퀀스, Map, case class, 마지막으로 원시 타입이다.

### 예제 따라가기: List[Option[String]]

- **Step 1:** `List[Option[String]]`이 Option인가?

  아니다. `optionArg`이 `None`을 반환한다.
  → Step 2로 계속

- **Step 2:** 시퀀스인가?

  그렇다! `seqArg`이 `Some(Option[String])`을 반환한다.
  → `SequenceShape(typeShapeOf(Option[String]))`을 만든다.
  → `Option[String]`에 대해 **재귀**.

- **재귀 Step 1:** `Option[String]`이 Option인가?

  그렇다! `optionArg`이 `Some(String)`을 반환한다.
  → `String`에 대해 **재귀**.

- **재귀 Step 2:** `String`이 Option? → NO
- **재귀 Step 3:** `String`이 시퀀스? → NO
- **재귀 Step 4:** `String`이 Map? → NO
- **재귀 Step 5:** `String`이 case class? → NO
- **재귀 Step 6:** 원시 타입으로 폴백. → `PrimitiveShape("String")` 반환.

**재귀 풀기:**

```
PrimitiveShape("String")
  → (Option 재귀에서)
  → SequenceShape(PrimitiveShape("String"))
```

잠깐, `Option`은 어디 갔나? 소비되었다! `typeShapeOf`가 `Option`을 만나면 내부 타입에 대해 재귀할 뿐이다. 이것이 앞서 언급한 엣지 케이스다. 요소 수준의 optionality가 사라진다.

### case class 처리:

```scala
if isCaseClass(t) then {
  val sym = t.typeSymbol                               // 클래스 심볼 가져오기
  val params = sym.primaryConstructor.paramSymss.flatten // 생성자 파라미터 가져오기

  val fields = params.map { p =>
    val name = p.name                                   // 필드명
    val ptpe = t.memberType(p)                          // 필드 타입
    val hasDefault = p.flags.is(Flags.HasDefault)       // 기본값 있는가?
    val (uT, isOpt) = optionArg(ptpe).fold(ptpe -> false)(a => a -> true)

    FieldShape(name, typeShapeOf(uT), hasDefault, isOpt)
  }

  StructShape(fields)
}
```

**한 줄씩 분석:**

- **`t.typeSymbol`** - case class의 심볼을 가져온다
- **`sym.primaryConstructor`** - case class에는 primary constructor가 있다
- **`.paramSymss.flatten`** - 모든 파라미터를 가져온다(flatten은 여러 파라미터 목록을 처리한다)
- **각 파라미터 `p`에 대해:**
  - **`p.name`** - 필드명: `"id"`, `"email"` 등
  - **`t.memberType(p)`** - 필드의 타입을 TypeRepr로
  - **`p.flags.is(Flags.HasDefault)`** - `age: Int = 0` 같은 기본값이 있는지 확인
  - **`optionArg(ptpe).fold(...)`** - 필드 타입이 Option[T]인지 확인:
    - 맞으면: T를 추출, `isOpt = true`
    - 아니면: 타입 그대로, `isOpt = false`
  - **`typeShapeOf(uT)`** - **재귀!** 필드의 기저 타입에 대해 shape를 만든다

### 예제:

`case class User(id: Long, email: String, age: Option[Int] = None)`

**처리 과정:**

- **Field 1: id**
  - `name` = "id"
  - `ptpe` = `Long`의 TypeRepr
  - `hasDefault` = false
  - `optionArg(Long)` = None → `uT = Long`, `isOpt = false`
  - `typeShapeOf(Long)` = `PrimitiveShape("Long")`
  - 결과: `FieldShape("id", PrimitiveShape("Long"), hasDefault=false, isOptional=false)`

- **Field 2: email**
  - `name` = "email"
  - `ptpe` = `String`의 TypeRepr
  - `hasDefault` = false
  - `optionArg(String)` = None → `uT = String`, `isOpt = false`
  - `typeShapeOf(String)` = `PrimitiveShape("String")`
  - 결과: `FieldShape("email", PrimitiveShape("String"), hasDefault=false, isOptional=false)`

- **Field 3: age**
  - `name` = "age"
  - `ptpe` = `Option[Int]`의 TypeRepr
  - `hasDefault` = **true** (`= None`이 있음)
  - `optionArg(Option[Int])` = Some(`Int`) → `uT = Int`, `isOpt = true`
  - `typeShapeOf(Int)` = `PrimitiveShape("Int")`
  - 결과: `FieldShape("age", PrimitiveShape("Int"), hasDefault=true, isOptional=true)`

- **최종 shape:**

```
StructShape([
  FieldShape("id", PrimitiveShape("Long"), false, false),
  FieldShape("email", PrimitiveShape("String"), false, false),
  FieldShape("age", PrimitiveShape("Int"), true, true)
])
```

### 왜 중요한가:

이제 case class의 정규화된 표현이 생겼다. 원래 Scala 문법과 관계없이 두 `StructShape`를 비교해서 일치하는지 확인할 수 있다.

재귀 구조 덕분에 임의로 중첩된 타입을 처리할 수 있다:

```scala
case class Order(
  items: List[LineItem],          // List로 재귀, 그다음 LineItem으로 재귀
  metadata: Map[String, String],  // Map으로 재귀, 그다음 String으로 (두 번)
  address: Option[Address]        // Option으로 재귀, 그다음 Address로 재귀
)
```

각 재귀가 트리의 한 조각을 만든다. 트리가 곧 스키마다.

## Part 8 - 필드 vs 요소 optionality: 중요한 구분

대부분의 가이드가 놓치는 미묘한 지점이다. GitHub의 코드는 두 종류의 optionality를 다룬다:

### 필드 수준 optionality (필드 자체의 Option)

```scala
case class User(name: String, age: Option[Int])
//                                 ^^^^^^^^^^^ 필드 수준 Option
```

매크로가 이를 감지하고 FieldShape에 `isOptional = true`를 설정한다:

```scala
val (uT, isOpt) = optionArg(ptpe).fold(ptpe -> false)(a => a -> true)
FieldShape(name, typeShapeOf(uT), hasDefault, isOpt)
```

### 요소 수준 optionality (컬렉션 내부의 Option)

```scala
case class Data(items: List[Option[String]])
//                          ^^^^^^^^^^^^^^ 요소 수준 Option
```

이는 중첩된 shape를 만든다: `SequenceShape(OptionShape(PrimitiveShape("String")))`

잠깐, 그건 GitHub 코드에 없다! 확인해보면, GitHub의 `typeShapeOf`는 재귀로 이를 처리한다:

```scala
optionArg(t).map(typeShapeOf).getOrElse {
  seqArg(t).map(a => SequenceShape(typeShapeOf(a))).getOrElse { ... }
}
```

따라서 `List[Option[String]]`은:

- `seqArg`이 List를 감지 → `Option[String]` 추출
- `Option[String]`에 대해 재귀 → `typeShapeOf(Option[String])`
- `optionArg`이 Option을 감지 → `String` 추출
- 기저 케이스: `PrimitiveShape("String")`

하지만 최종 shape는 `SequenceShape(PrimitiveShape("String"))`이다. 중간 Option이 소비되었다!

**계약 측면에서 왜 중요한가:** 이렇게 쓰면:

```scala
case class Contract(items: List[String])
case class Producer(items: List[Option[String]])
```

Exact 하에서는 아마 적합하지 않아야 한다. producer는 null 요소를 허용하지만 contract는 아니다. 하지만 Option이 벗겨지기 때문에 현재 GitHub 코드에서는 허용될 수 있다.

이것이 프로덕션 배포 시 발견하게 되는 엣지 케이스다. 수정하려면 모델에 `OptionalShape` 래퍼를 추가하면 된다.

> **Optionality 엣지 케이스**
>
> 요소 수준 optionality 문제(List[Option[String]]이 평탄화되는 것)는 실제로 존재하며 미묘하다. 스키마에 nullable 컬렉션이 있다면, 컴파일 타임 검사가 List[String]과 List[Option[String]]의 차이를 실제로 잡는지 테스트하라. 현재 구현은 여기서 불일치를 허용할 수 있다. 프로덕션에서 사용하기 전에 nullable 컬렉션 패턴에 대한 명시적 테스트 케이스를 추가하라.

## Part 9 - 컴파일 타임 "conforms" 증거 (전체 그림)

이제 전부 합쳐보자. GitHub 코드의 전체 매크로다:

Conforms.scala
```scala
package com.example

import scala.quoted.*

final class Conforms[Out, Contract, P <: Policy]

object Conforms {
  inline given materialize[Out, Contract, P <: Policy](using Shape[Out], Shape[Contract]): Conforms[Out, Contract, P] = {
    ${ conformsImpl[Out, Contract, P] }
  }

  private def conformsImpl[Out: Type, Contract: Type, P: Type](using Quotes): Expr[Conforms[Out, Contract, P]] = {
    import quotes.reflect.*

    def shapeOf[A: Type]: List[(String, String, Boolean, Boolean)] = {
      val sh = Expr.summon[Shape[A]].getOrElse(report.errorAndAbort("No Shape available"))
      '{
        val fs = $sh.fields
        fs.map(f => (f.name, f.tpe, f.hasDefault, f.isOptional))
      }.valueOrAbort
    }

    val out = shapeOf[Out]
    val contract = shapeOf[Contract]

    // 필드명 기준 비교 (순서 무관, 대소문자 구분)
    val outMap = out.map { case (n,t,_,opt) => n -> (t,opt) }.toMap
    val contractMap = contract.map { case (n,t,_,opt) => n -> (t,opt) }.toMap

    val missing = contract.collect { case (n,t,_,opt) if !outMap.contains(n) => s"$n:$t${if opt then " (optional)" else ""}" }
    val extra = out.collect { case (n,_,_,_) if !contractMap.contains(n) => n }
    val mism = contract.collect {
      case (n,t,_,opt) if outMap.get(n).exists(_._1 != t) => s"$n expected $t, found ${outMap(n)._1}"
      case (n,_,_,opt) if outMap.get(n).exists(_._2 != opt) => s"$n optionality mismatch"
    }

    val (okMissing, okExtra, okMism) = Type.of[P] match {
      case '[Policy.Exact]    => (missing, extra, mism)
      case '[Policy.Backward] => (missing.filterNot(_.contains("(optional)")), Nil, mism)
      case '[Policy.Forward]  => (Nil, extra, mism)
      case _                  => (missing, extra, mism)
    }

    if okMissing.nonEmpty || okExtra.nonEmpty || okMism.nonEmpty then {
      val msg = s"""
        |Compile‑time contract drift (policy: ${Type.show[P]}).
        |Out: ${Type.show[Out]} vs Contract: ${Type.show[Contract]}
        |Missing: ${okMissing.mkString(", ")}
        |Extra: ${okExtra.mkString(", ")}
        |Mismatched: ${okMism.mkString("; ")}
        |""".stripMargin
      quotes.reflect.report.errorAndAbort(msg)
    } else '{ new Conforms[Out, Contract, P] }
  }
}
```

무슨 일이 일어나는지 분석하자:

- **shape 추출**: `Shape[Out]`과 `Shape[Contract]`를 소환해서 필드 목록을 가져온다. `.valueOrAbort`에 주목하라. 이것이 컴파일 타임 평가를 강제한다.
- **비교**: 필드명 → (타입, optionality) 맵을 만들고 차이를 찾는다.
- **정책 적용**:
  - Exact: 모든 차이가 오류
  - Backward: 누락된 optional 필드는 OK, 추가 필드도 OK
  - Forward: 추가 필드는 OK (하지만 누락된 필수 필드는 안 됨)
- **중단 또는 성공**: 위반이 있으면 정확히 뭐가 잘못됐는지 보여주는 상세 메시지와 함께 `report.errorAndAbort`를 호출한다. 아니면 증거를 반환한다.

핵심: **이것은 컴파일 중에 실행된다**. 스키마가 맞지 않으면 코드가 컴파일되지 않는다. 끝.

### 실제 drift 시나리오

Apache Spark 파이프라인 시나리오에서 실제로 동작하는 것을 보자. 데이터 웨어하우스 팀이 `CustomerContract`를 관리하고 하류 분석 대시보드가 이에 의존한다. 상류 CRM 팀이 고객 세분화 데이터를 추가하기 시작하고, 이를 통과시킬지 필터할지 결정해야 한다.

**초기 상태 - 모든 것이 정렬:**

CustomerPipeline.scala
```scala
package com.example

// 분석 팀이 의존하는 웨어하우스 계약
final case class CustomerContract(
  id: Long,
  email: String,
  age: Option[Int] = None
)

// 상류 CRM 내보내기가 계약과 정확히 일치
final case class CRMExport(
  id: Long,
  email: String,
  age: Option[Int]
)

// 컴파일 타임 검증이 포함된 Spark 파이프라인
val pipeline = PipelineBuilder[Nothing]("customer-sync")
  .addSource(TypedSource[CRMExport]("parquet", "s3://data-lake/crm/exports"))
  .noTransform // 변환 불필요 - 스키마가 일치
  .addSink[CustomerContract, Policy.Exact](
    TypedSink("s3://warehouse/customers")
  )
  .build

// 컴파일 성공 - CRMExport가 Exact 정책 하에서 CustomerContract와 일치
summon[Conforms[CRMExport, CustomerContract, Policy.Exact]]
```

**상류 팀이 세분화를 추가:**

CRM 팀이 새 세분화 기능을 배포하고 내보내기에 포함시키기 시작한다. 컴파일 타임 계약 없이는 파이프라인이 새 필드를 조용히 무시하거나(읽기 모드에 따라) 크래시한다. 컴파일 타임 계약이 있으면 빌드가 즉시 깨진다:

```scala
// CRM 팀의 새 내보내기 스키마에 고객 세그먼트가 포함
final case class CRMExportWithSegment(
  id: Long,
  email: String,
  age: Option[Int],
  segment: String // NEW: "premium", "standard", "trial"
)

// 새 소스를 읽도록 파이프라인 업데이트
val pipeline = PipelineBuilder[Nothing]("customer-sync")
  .addSource(TypedSource[CRMExportWithSegment]("parquet", "s3://data-lake/crm/exports"))
  .noTransform
  .addSink[CustomerContract, Policy.Exact](
    TypedSink("s3://warehouse/customers")
  )
  .build

// 컴파일 오류:
// Compile-time contract drift (policy: Policy.Exact).
// Out: CRMExportWithSegment vs Contract: CustomerContract
// Extra: segment
```

**선택지:**

빌드가 명시적 결정을 강제한다. 세 가지 일반적인 접근법이 있다:

```scala
// Option 1: 전환 기간에 Backward 정책 사용 (추가 필드 허용)
.addSink[CustomerContract, Policy.Backward](
  TypedSink("s3://warehouse/customers")
)
// 컴파일 성공 - Backward 정책이 producer의 추가 필드를 허용

// Option 2: 변환에서 명시적으로 필드를 필터
.transformAs[CustomerContract]("drop-segment") { df =>
  df.select("id", "email", "age")
}
.addSink[CustomerContract, Policy.Exact](
  TypedSink("s3://warehouse/customers")
)
// 컴파일 성공 - 변환이 계약 스키마로 명시적 프로젝션

// Option 3: 계약을 업데이트해서 segment 포함 (분석 팀과 조율)
final case class CustomerContractV2(
  id: Long,
  email: String,
  age: Option[Int],
  segment: String
)

.addSink[CustomerContractV2, Policy.Exact](
  TypedSink("s3://warehouse/customers_v2")
)
// 컴파일 성공 - 스키마가 정확히 일치
```

**왜 중요한가:**

컴파일 타임 계약 없이는 drift가 런타임까지 보이지 않는다. Apache Spark 잡이 이럴 수 있다:

- `segment` 필드를 조용히 드롭 (스키마 진화 모드)
- 새벽 2시에 스키마 불일치 오류로 크래시
- 몇 주간 `segment`에 null을 기록하고 아무도 모름

컴파일 타임 계약이 있으면 개발 단계에서 대화가 이뤄진다:

- "하류 대시보드가 고객 세그먼트를 필요로 하나?"
- "계약을 버전업하고 consumer를 마이그레이션해야 하나?"
- "이건 필터해야 할 임시 필드인가?"

drift를 명시적으로 처리할 때까지 빌드가 통과하지 않는다. 그게 의도다.

> **빌드 중단 알림**
>
> 컴파일 타임 계약 검사는 스키마가 어긋나면 빌드를 깨뜨린다. 그것이 요점이지만, 팀과 조율하라. 상류에서 필드를 추가하면 모든 하류 파이프라인 빌드가 계약이나 변환을 업데이트할 때까지 실패한다. 이는 정렬을 강제하지만, 관리하지 않으면 배포를 막을 수 있다. CI 브랜치 빌드를 사용해 스키마 변경 머지 전에 영향을 미리 확인하라.

전체 핵심은 이것이다. 프로덕션에서는 중첩 타입과 더 복잡한 시나리오를 다뤄야 하겠지만(GitHub 코드에서 보여준다), 구조는 동일하다.

## Part 10 - 개발자 경험: 실제로 사용하면 어떤 느낌인가

실무적 측면을 이야기해보자. 일상 업무에서 실제로 사용하면 어떻게 되는가?

### 컴파일 시간

**질문: 컴파일이 느려지나?**

실제로는 눈에 띄지 않는다. 매크로는 컴파일 중에 `summon[Conforms[...]]` 호출마다 한 번 실행된다. 계약 검사가 5-10개인 일반적인 파이프라인이라면 빌드에 100-200ms 정도 추가된다. 프로덕션 스키마 불일치를 디버깅하는 데 걸리는 시간과 비교해보라.

### 오류 메시지

스키마가 어긋나면 이런 메시지를 받는다:

```
[error] Compile-time contract drift (policy: Policy.Exact).
[error] Out: CustomerProducer vs Contract: CustomerContract
[error] Extra: segment
[error] Missing:
[error] Mismatched:
```

명확하고, 조치 가능하며, 빌드를 멈춘다. 난해한 매크로 오류도 없고 스택 트레이스도 없다. 그냥: "`segment`라는 추가 필드가 있다."

### 통합 패턴

**Apache Spark 연동:**

```scala
// Spark 잡
val df = spark.read.parquet(path)
val typed = df.as[CustomerProducer] // Spark 인코더

// 변환 전 컴파일 타임 검사
summon[Conforms[CustomerProducer, CustomerContract, Policy.Exact]]

val output = typed.transform(dropSegment).as[CustomerContract]
```

**스키마 레지스트리 연동:**

```scala
// 레지스트리의 Avro 스키마 → case class → 계약 검사
case class CustomerAvro(id: Long, email: String, amt: Double)
summon[Conforms[CustomerAvro, CustomerContract, Policy.Exact]]
// Avro 스키마 drift가 있으면 빌드 실패!
```

**CI/CD 통합:** `sbt compile`만 실행하면 된다. 계약이 어긋나면 빌드가 실패한다. 특별한 도구가 필요 없다. Jenkins, GitHub Actions, GitLab CI 등 sbt를 실행하는 모든 환경에서 동작한다.

### 온보딩

**새 팀원이 묻는다**: "왜 컴파일이 안 되나요?"

**답변**: "오류를 확인해보세요. `CustomerProducer`를 쓰려고 하는데 계약은 `CustomerContract`를 기대합니다. 변환을 조정하거나 계약을 업데이트하세요."

끝이다. 컴파일러가 안내한다. 한두 번 겪으면 감을 잡는다.

### 사용 vs 생략 기준

**컴파일 타임 계약을 사용할 때**:

- 여러 팀이 같은 파이프라인 코드베이스를 작업할 때
- 스키마 변경이 프로덕션을 정기적으로 깨뜨릴 때
- 빠른 피드백이 필요할 때 (컴파일 vs 배포 후 테스트)

**생략할 때**:

- 일회성 스크립트나 탐색적 작업
- 제어할 수 없는 외부 API (런타임 검증 사용)
- 스키마가 진정으로 동적일 때 (알 수 없는 키를 가진 JSON)

## Part 11 - 중첩 타입: Map, List, 깊은 구조

GitHub의 코드는 중첩 타입을 다루는 방법을 보여준다. `CtdcPoc.scala`의 이 예제를 보자:

```scala
// 깊은 / 중첩 구조
final case class LineItem(sku: String, qty: Int, attrs: Map[String, String])
final case class Address(street: String, zip: String)

final case class OrderOut(
  id: Long,
  items: List[LineItem],
  shipTo: Option[Address],
  tags: Set[String]
)

final case class OrderContract(
  id: Long,
  items: Seq[LineItem],       // List vs Seq - 둘 다 시퀀스
  shipTo: Option[Address],    // 중첩된 case class
  tags: Seq[String] = Nil     // Set vs Seq, 기본값 있음
)

val evDeepOk: SchemaConforms[OrderOut, OrderContract, SchemaPolicy.Backward.type] = summon
```

**여기서 무슨 일이 일어나는가:**

- **중첩된 case class** - `OrderOut` 안의 `Address`. 매크로가 재귀한다: `Option[Address]`를 보면 `Address`로 언랩하고, `Address`가 case class인지 확인한 뒤 다시 재귀해서 필드를 가져온다.
- **컬렉션** - `List[LineItem]` vs `Seq[LineItem]`. `seqArg`이 둘 다 동등하게 취급하므로 일치한다. 매크로는 "LineItem의 시퀀스"를 보고 `LineItem`에 대해 재귀한다.
- **Map** - `attrs`의 `Map[String, String]`. 매크로가 key 타입(atomic이어야 함)을 검사한 뒤 value 타입에 대해 재귀한다.
- **기본값** - `tags: Seq[String] = Nil`은 기본값이 있다. Backward 정책 하에서 `OrderOut`에 `tags`가 없으면 계약에 기본값이 있으므로 OK다.

이것이 TypeInspector 패턴이 중요한 이유다. 특수한 경우 처리 없이 임의의 중첩을 다룬다.

## Part 12 - 정책 수정자 (Ordered, CI, By-Position)

때로는 더 엄격한 비교가 필요하다. 또는 다른 규칙이 필요하다. 세 가지 정책 확장을 빠르게 살펴보자:

PolicyMods.scala
```scala
package com.example

sealed trait PolicyMod
object PolicyMod {
  sealed trait ExactOrdered    extends PolicyMod // 이름 + 순서 + 타입이 일치해야 함
  sealed trait ExactCI         extends PolicyMod // 대소문자 구분 없는 필드명
  sealed trait ExactByPosition extends PolicyMod // 이름 무시; 인덱스로 타입 비교
  case object ExactOrdered    extends ExactOrdered
  case object ExactCI         extends ExactCI
  case object ExactByPosition extends ExactByPosition
}

object ConformsMod {
  import scala.quoted.*
  inline def apply[Out, Contract, M <: PolicyMod](using Shape[Out], Shape[Contract]): Unit = ${ impl[Out, Contract, M] }

  private def impl[Out: Type, Contract: Type, M: Type](using Quotes): Expr[Unit] = {
    import quotes.reflect.*
    def fields[A: Type]: List[(String,String)] = {
      val sh = Expr.summon[Shape[A]].getOrElse(report.errorAndAbort("No Shape available"))
      '{ $sh.fields.map(f => (f.name, f.tpe)) }.valueOrAbort
    }

    val out = fields[Out]
    val contract = fields[Contract]

    val mismatches: List[String] = Type.of[M] match {
      case '[PolicyMod.ExactCI] =>
        val n = (s: String) => s.toLowerCase
        val om = out.map{ case (n0,t) => n(n0) -> t }.toMap
        val cm = contract.map{ case (n0,t) => n(n0) -> t }.toMap
        (cm.keySet union om.keySet).toList.flatMap { k =>
          (cm.get(k), om.get(k)) match {
            case (Some(t1), Some(t2)) if t1 != t2 => List(s"$k expected $t1, found $t2")
            case (Some(_), None)                   => List(s"missing ${k}")
            case (None, Some(_))                   => List(s"extra ${k}")
            case _                                 => Nil
          }
        }
      case '[PolicyMod.ExactByPosition] =>
        if (out.length != contract.length) {
          List(s"length mismatch: ${out.length} vs ${contract.length}")
        } else {
          out.zip(contract).zipWithIndex.collect {
            case (((_,tO),(_,tC)), i) if tO != tC => s"@$i expected $tC, found $tO"
          }
        }
      case '[PolicyMod.ExactOrdered] =>
        if (out.length != contract.length) {
          List(s"length mismatch: ${out.length} vs ${contract.length}")
        } else {
          out.zip(contract).zipWithIndex.flatMap {
            case (((nO,tO),(nC,tC)), i) =>
              val nameOk = if (nO == nC) Nil else List(s"@$i(name) expected $nC, found $nO")
              val typeOk = if (tO == tC) Nil else List(s"@$i(type) expected $tC, found $tO")
              nameOk ++ typeOk
          }
        }
      case _ => report.errorAndAbort("Unknown policy mod")
    }

    if (mismatches.nonEmpty) {
      report.errorAndAbort(s"Modifier drift: ${mismatches.mkString("; ")}")
    } else {
      '{ () }
    }
  }
}
```

시도해보자:

```scala
import com.example.*

final case class A(id: Long, name: String)
final case class B(name: String, id: Long)

ConformsMod[A, B, PolicyMod.ExactByPosition] // 성공, 인덱스 기준 같은 타입
// ConformsMod[A, B, PolicyMod.ExactOrdered] // 실패, 순서가 중요 -> 이름 불일치

final case class CIa(ID: Long)
final case class CIb(id: Long)
ConformsMod[CIa, CIb, PolicyMod.ExactCI] // 성공, 대소문자 무시 이름 일치
```

왜 이 세 가지인가? 실제 시스템에서 필요하기 때문이다:

- **ExactOrdered**: 위치가 중요한 포맷 (일부 CSV 파서, 필드 번호가 있는 protobuf)
- **ExactCI**: 계약은 `userId`인데 누군가 항상 `UserId`를 쓰기 때문
- **ExactByPosition**: 필드명을 완전히 무시하는 레거시 시스템

## Part 13 - Phantom type과 type-state 빌더: 잘못 사용할 수 없는 API 만들기

여기서부터 흥미로워진다. 코드는 phantom type, type-indexing / typestate 빌더 패턴을 사용해 **잘못 사용하는 것이 불가능한** 파이프라인 빌더를 만든다. Phantom type이란? 클래스 본문에는 나타나지 않지만 인스턴스로 무엇을 할 수 있는지 제어하는 타입 파라미터다. 컴파일 타임 상태 기계라 생각하면 된다.

코드베이스 `SparkCore.scala`에서:

```scala
// Phantom type 상태
sealed trait BuilderState
sealed trait Empty         extends BuilderState
sealed trait WithSource    extends BuilderState
sealed trait WithTransform extends BuilderState
sealed trait Complete      extends BuilderState

final case class PipelineBuilder[S <: BuilderState, CurContract] private (
  name: String,
  steps: List[PipelineStep]
) {
  // Empty일 때만 소스 추가 가능
  def addSource[C](src: TypedSource[C])(using SparkSchema[C]): PipelineBuilder[WithSource, C] =
    PipelineBuilder[WithSource, C](name, steps :+ ...)

  // 소스 추가 후에만 변환 가능
  def transformAs[Next](f: DataFrame => DataFrame)(using
    ev: S <:< WithSource,    // 이 제약이 순서를 강제한다!
    sch: SparkSchema[Next]
  ): PipelineBuilder[WithTransform, Next] =
    PipelineBuilder[WithTransform, Next](name, steps :+ ...)

  // 변환 후에만 싱크 추가 가능
  def addSink[R, P <: SchemaPolicy](sink: TypedSink[R])(using
    ev0: S <:< WithTransform,                  // 변환이 있어야 함
    ev1: SchemaConforms[CurContract, R, P],     // 컴파일 타임 계약 검사!
    sch: SparkSchema[R]
  ): PipelineBuilder[Complete, CurContract] =
    PipelineBuilder[Complete, CurContract](name, steps :+ ...)

  // Complete일 때만 빌드 가능
  def build(using ev: S =:= Complete): SparkSession => DataFrame =
    (spark: SparkSession) => ...
}

object PipelineBuilder {
  def apply[CurContract](name: String): PipelineBuilder[Empty, CurContract] = {
    PipelineBuilder[Empty, CurContract](name, Nil)
  }
}
```

### 동작 방식:

- **타입으로서의 상태 전환** - 각 메서드가 새 타입 파라미터 `S`를 반환한다. 컴파일러가 현재 상태를 추적한다.
- **증거 제약** - `ev: S <:< WithSource`는 "S가 WithSource의 서브타입이어야 한다"는 뜻이다. 아니면 이 메서드가 존재하지 않는다.
- **컴파일 타임 상태 기계** - 말 그대로 잘못된 순서로 메서드를 호출할 수 없다:

```scala
// 성공, 컴파일 됨
PipelineBuilder[Contract]("good")
  .addSource(src)
  .transformAs[Next](transform)
  .addSink[Contract, SchemaPolicy.Exact](sink)
  .build

// 실패, 컴파일 안 됨 - 소스 추가 전에 변환할 수 없음
PipelineBuilder[Contract]("bad")
  .transformAs[Next](transform) // ERROR: No implicit evidence S <:< WithSource
```

- **내장된 계약 검사** - `addSink`의 `ev1: SchemaConforms[CurContract, R, P]`를 보라. 여기서 컴파일 타임 계약 검사가 일어난다. 스키마가 어긋나면 파이프라인이 빌드되지 않는다.

이 패턴은 **Phantom Type Builder Pattern**이라 불린다. 자세한 내용은 다음을 참고하라:

- [Compile-safe builder pattern using phantom types (Xebia)](https://xebia.com/blog/compile-safe-builder-pattern-using-phantom-types-in-scala/)
- [Phantom Types in Scala (Rhetorical Musings)](https://blog.rhetoricalmusings.com/posts/builder1/)

**왜 중요한가**: 프로덕션 데이터 파이프라인에서 작업 순서가 중요하다. Read → Transform → Validate → Write. Phantom type으로 컴파일러가 이 순서를 강제한다. 변환 전에 쓰는 것이 불가능하다. 소스 없이 변환하는 것도 불가능하다.

이것이 "매크로 동작 방식 설명"과 "프로덕션 계약 시스템 구축 방법"의 차이다.

## Part 14 - 스키마 진화와 버전 관리: 시간에 따른 변경 처리

현실은 이렇다: 스키마는 진화한다. 필드가 추가되고, 폐기되고, 이름이 바뀐다. 팀은 변경을 점진적으로 배포한다. 문제는 스키마가 바뀔 것이냐가 아니라, 프로덕션을 깨뜨리지 않고 어떻게 관리하느냐다.

### 스키마 진화 문제

**시나리오**: `CustomerV1`이 프로덕션에서 돌아가고 있다. 마케팅이 타겟팅을 위한 `segment` 필드 추가를 원한다. 하지만 `CustomerV1`을 읽는 파이프라인이 10개 있다. 모든 것을 깨뜨리지 않고 어떻게 진화할 것인가?

**잘못된 접근**: `CustomerV1`에 `segment`를 추가하고 배포하고 잘 되기를 기도한다. 결과: 새 필드를 처리하지 않는 파이프라인 일부가 깨진다.

**올바른 접근**: 계약을 버전 관리하고 정책으로 전환을 관리한다.

### 버전 관리 전략

CustomerV1.scala
```scala
package contracts
final case class CustomerV1(id: Long, email: String)
```

CustomerV2.scala
```scala
package contracts
final case class CustomerV2(id: Long, email: String, segment: Option[String] = None)
```

`segment`가 다음과 같다는 것에 주목하라:

- **Optional** (`Option[String]`)
- **기본값 있음** (`= None`)

이로써 하위 호환성이 보장된다. 기존 코드가 새 데이터와 동작할 수 있다.

### 마이그레이션 단계

**Phase 1: 필드 추가 (Backward 정책)**

`CustomerV2`를 쓰는 새 producer를 배포한다:

```scala
val producer = PipelineBuilder[CustomerV2]("write-v2")
  .addSource(rawData)
  .transformAs[CustomerV2](addSegment)
  .addSink[CustomerV2, Policy.Backward](sink) // Backward가 optional 필드 허용
  .build
```

기존 consumer는 여전히 `CustomerV1`을 읽는다. `segment` 필드를 무시한다. 깨지지 않는다.

**Phase 2: Consumer 마이그레이션**

각 consumer를 하나씩 업데이트한다:

```scala
val consumer = PipelineBuilder[CustomerV2]("read-v2")
  .addSource[CustomerV2](source)
  .transformAs[EnrichedCustomer](useSegment) // 이제 segment 필드를 사용
  .addSink[EnrichedCustomer, Policy.Exact](sink)
  .build
```

컴파일 타임 계약이 보장한다: "이 consumer가 새 스키마를 처리하는가?" case class 업데이트를 잊으면 컴파일되지 않는다.

**Phase 3: V1 폐기**

모든 파이프라인이 `CustomerV2`를 사용하면:

```scala
// V1을 deprecated로 표시
@deprecated("Use CustomerV2", "2025-10-01")
final case class CustomerV1(id: Long, email: String)
```

컴파일러가 남은 V1 사용에 대해 경고한다. 유예 기간 후 V1을 완전히 삭제한다.

### 호환성을 깨는 변경 처리

필드명을 바꿔야 한다면? 예를 들어 `email` → `emailAddress`?

**호환성을 유지하는 접근:**

```scala
// Step 1: 새 필드 추가, 기존 유지
final case class CustomerV3(
  id: Long,
  email: String,        // 기존 필드
  emailAddress: String, // 새 필드
  segment: Option[String] = None
)
```

잠깐, 중복이다. 더 나은 방법:

```scala
// Step 1: 별칭 생성자 추가
final case class CustomerV3(
  id: Long,
  emailAddress: String, // 새 정식 이름
  segment: Option[String] = None
)
object CustomerV3 {
  def fromV2(v2: CustomerV2): CustomerV3 = {
    CustomerV3(v2.id, v2.email, v2.segment)
  }
}
```

**Step 2**: Producer가 `emailAddress`를 쓰도록 마이그레이션. **Step 3**: Consumer가 `emailAddress`를 읽도록 마이그레이션. **Step 4**: `fromV2` 생성자 제거.

각 단계에서 컴파일 타임 계약이 검증한다: "이 변환이 기대 스키마를 생성하는가?"

### 마이그레이션 중 공존

마이그레이션 중에 V2와 V3가 모두 돌아간다. 어떻게 처리하나?

```scala
sealed trait CustomerSchema
case class V2(id: Long, email: String, segment: Option[String] = None) extends CustomerSchema
case class V3(id: Long, emailAddress: String, segment: Option[String] = None) extends CustomerSchema

// 변환이 양쪽을 처리
def normalize(schema: CustomerSchema): V3 = schema match {
  case V2(id, email, segment) => V3(id, email, segment)
  case v3: V3                 => v3
}
```

계약이 양쪽 경로를 검사한다:

```scala
summon[Conforms[V2, V3Contract, Policy.Backward]] // V2 → V3 허용
summon[Conforms[V3, V3Contract, Policy.Exact]]     // V3 → V3 정확히 일치
```

### 실제 마이그레이션: Backward → Exact

GitHub 코드의 전체 마이그레이션 시나리오를 보자. 프로덕션에서 스키마 변경을 롤아웃하는 정확한 방법이다.

`CtdcPoc.scala`에서:

```scala
/** 싱크 계약: 쓰기를 약속하는 대상 스키마. */
final case class CustomerContract(id: Long, email: String, age: Option[Int] = None)

/** Producer: 상류 소스가 추가 필드 `segment`를 추가했다고 가정. */
final case class CustomerProducer(id: Long, email: String, age: Option[Int], segment: String)

/** 변환 후 선언된 "Next" 스키마 (`segment` 제거). */
final case class CustomerNext(id: Long, email: String, age: Option[Int])

// Step 1: 마이그레이션 단계 - Backward 정책 사용
// Producer에 추가 필드가 있지만 마이그레이션 중에 계약이 허용
val srcB = TypedSource[CustomerProducer]("csv", inPath, Map("header" -> "true"))
val sinkB = TypedSink[CustomerContract](tmpOutB)

val dropExtras: DataFrame => DataFrame = _.select($"id", $"email", $"age")

val planB =
  PipelineBuilder[CustomerContract]("CSV -> Parquet B: transformAs[CustomerNext], Exact")
    .addSource(srcB)
    .transformAs[CustomerNext]("drop segment")(dropExtras)
    .addSink[CustomerContract, SchemaPolicy.Exact.type](sinkB)
    .build
```

### 무슨 일이 일어나는가:

**Phase 1: Producer가 필드 추가**

- 상류가 `CustomerProducer`에 `segment` 필드를 추가
- 계약은 여전히 `CustomerContract` (`segment` 없음)
- `transformAs[CustomerNext]`로 추가 필드를 명시적으로 제거
- `CustomerNext`가 `CustomerContract`에 **Exact** 하에서 적합한지 검사

**왜 동작하는가:**

- 변환이 출력 스키마(`CustomerNext`)를 명시적으로 선언
- 컴파일러가 검사: `CustomerNext`가 `CustomerContract`와 맞는가? 맞다 (둘 다 id, email, age)
- 나중에 `CustomerNext`를 실수로 바꾸면 컴파일 실패

**Phase 2: 안정화** 마이그레이션이 끝나면:

```scala
// 이후: 모두가 CustomerContract를 사용, 변환 불필요
val planStable =
  PipelineBuilder[CustomerContract]("stable")
    .addSource(contractSource)
    .noTransform // 직접 통과
    .addSink[CustomerContract, SchemaPolicy.Exact.type](sink)
    .build
```

실제 현장 이야기다. 코드가 스키마를 안전하게 마이그레이션하는 방법을 정확히 보여준다.

## Part 15 - 컴파일 타임 테스트하기 (복사해서 쓰는 테스트)

코드가 컴파일에 실패하는 것을 어떻게 테스트하나? `assertDoesNotCompile`로:

CompileTimeSpec.scala
```scala
package com.example
import org.scalatest.funsuite.AnyFunSuite

class CompileTimeSpec extends AnyFunSuite {
  test("Exact should fail when a required field is missing") {
    assertDoesNotCompile("summon[Conforms[OutMissing, Contract, Policy.Exact]]")
  }
  test("Backward should pass for the same mismatch") {
    assertCompiles("summon[Conforms[OutMissing, Contract, Policy.Backward]]")
  }
  test("By-Position should accept re-ordered names with same types") {
    assertCompiles("ConformsMod[A, B, PolicyMod.ExactByPosition]")
  }
  test("Ordered should reject the same re-ordering") {
    assertDoesNotCompile("ConformsMod[A, B, PolicyMod.ExactOrdered]")
  }
  test("Pipeline builder enforces correct order") {
    assertDoesNotCompile("""
      PipelineBuilder[Contract]("bad")
        .transformAs[Next](identity) // 소스 없이 변환할 수 없음
    """)
  }
  test("Nested types conform correctly") {
    assertCompiles("summon[Conforms[OrderOut, OrderContract, Policy.Backward]]")
  }
}
```

CI에 연결할 수 있다:

```yaml
# .github/workflows/compile-fail.yml (개념)
name: compile-fail
on: [pull_request]
jobs:
  cf:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '17' }
      - name: Compile-fail suite
        run: sbt -v "testOnly *CompileTimeSpec"
```

이것은 아주 귀하다. CI가 깨진 스키마는 배포될 수 없음을 글자 그대로 증명한다.

## Part 16 - Scala 2: 같은 작업을 하는 방법 (그리고 차이점)

Scala 2는 `Context`(blackbox/whitebox)가 있는 def macro를 사용한다. 아이디어는 동일하지만, 구현이 `Expr`/quotes 대신 `c.universe` 트리를 사용한다.

ConformsS2.scala
```scala
// build.sbt
// scalaVersion := "2.13.14"
// libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value

package com.example
import scala.reflect.macros.blackbox

final class ConformsS2[Out, Contract, P]

object ConformsS2 {
  implicit def materialize[Out, Contract, P <: Policy](implicit sOut: Shape[Out], sCon: Shape[Contract]): ConformsS2[Out, Contract, P] =
    macro conformsImpl[Out, Contract, P]

  def conformsImpl[Out: c.WeakTypeTag, Contract: c.WeakTypeTag, P: c.WeakTypeTag](c: blackbox.Context)(sOut: c.Expr[Shape[Out]], sCon: c.Expr[Shape[Contract]]): c.Expr[ConformsS2[Out, Contract, P]] = {
    import c.universe._

    def fieldsOf[T: c.WeakTypeTag](sh: c.Expr[Shape[T]]): List[(String,String,Boolean,Boolean)] = {
      c.eval(c.Expr[List[(String,String,Boolean,Boolean)]](q"$sh.fields.map(f => (f.name, f.tpe, f.hasDefault, f.isOptional))"))
    }

    val out = fieldsOf[Out](sOut)
    val contract = fieldsOf[Contract](sCon)

    val outMap = out.map{ case (n,t,_,opt) => n -> (t,opt)}.toMap
    val contractMap = contract.map{ case (n,t,_,opt) => n -> (t,opt)}.toMap

    val missing = contract.collect { case (n,t,_,opt) if !outMap.contains(n) => s"$n:$t${if (opt) " (optional)" else ""}" }
    val extra = out.collect { case (n,_,_,_) if !contractMap.contains(n) => n }
    val mism = contract.collect {
      case (n,t,_,_) if outMap.get(n).exists(_._1 != t) => s"$n expected $t, found ${outMap(n)._1}"
      case (n,_,_,o) if outMap.get(n).exists(_._2 != o) => s"$n optionality mismatch"
    }

    if (missing.nonEmpty || extra.nonEmpty || mism.nonEmpty)
      c.abort(c.enclosingPosition, s"Drift: missing=${missing.mkString(",")} extra=${extra.mkString(",")} mism=${mism.mkString(";")}")
    else
      c.Expr[ConformsS2[Out, Contract, P]](q"new _root_.com.example.ConformsS2[${weakTypeOf[Out]}, ${weakTypeOf[Contract]}, ${weakTypeOf[P]}]")
  }
}
```

주요 차이점:

- Scala 3: `inline` + `${ ... }` splice; Scala 2: `macro def`와 `Context`
- 오류 보고: `report.errorAndAbort` (Scala 3) vs `c.abort` (Scala 2)
- 트리: `Expr[T]` (Scala 3) vs raw `Tree` (Scala 2)
- 타입 검사: `TypeRepr` (Scala 3) vs `Type` (Scala 2)

Scala 3 매크로가 더 안전하다. quoted API가 많은 일반적인 매크로 버그를 방지한다. 하지만 Scala 2 매크로도 주의를 기울이면 잘 동작한다.

derivation에 대해서는 [Magnolia](https://github.com/softwaremill/magnolia)를 확인하라. 양쪽 버전 모두 지원한다.

## Part 17 - Spark 통합: 런타임 심층 방어

GitHub의 코드는 Spark의 내장 구조 비교기를 사용해 컴파일 타임 정책을 런타임 검증에 대응시키는 방법도 보여준다.

`SparkCore.scala`에서:

```scala
// 컴파일 타임에 case class에서 Spark StructType 도출
trait SparkSchema[C] {
  def struct: StructType
}

object SparkSchema {
  inline given derived[C]: SparkSchema[C] = ${ sparkSchemaImpl[C] }

  // case class → StructType으로 변환하는 매크로
  private def sparkSchemaImpl[C: Type](using Quotes): Expr[SparkSchema[C]] = ...
}

// Spark 비교기에 대한 런타임 정책 매핑
trait PolicyRuntime[P <: SchemaPolicy] {
  def ok(found: StructType, expected: StructType): Boolean
}

object PolicyRuntime {
  given PolicyRuntime[SchemaPolicy.Exact.type] with {
    def ok(found: StructType, expected: StructType) = {
      DataType.equalsIgnoreCaseAndNullability(found, expected)
    }
  }

  given PolicyRuntime[SchemaPolicy.ExactByPosition.type] with {
    def ok(found: StructType, expected: StructType) = {
      DataType.equalsStructurally(found, expected, ignoreNullability = true)
    }
  }

  given PolicyRuntime[SchemaPolicy.ExactOrdered.type] with {
    def ok(found: StructType, expected: StructType) = {
      DataType.equalsStructurallyByName(found, expected, _ == _)
    }
  }
}
```

**2계층 검증 전략**:

- **컴파일 타임** (매크로) - 배포 전에 스키마 drift를 잡는다
- **런타임** (Spark 비교기) - 외부 데이터 소스에 대한 방어적 검사

Spark의 구조 비교기에 대한 자세한 내용은 [DataType API 문서](https://spark.apache.org/docs/3.5.1/api/scala/org/apache/spark/sql/types/DataType%24.html)를 참고하라.

## Part 18 - 내일 바로 쓸 수 있는 마이그레이션 플레이북

실무에서 이 정책들을 사용하는 방식:

- **핵심 경로 → Exact**. 예외 없다. 스키마가 정확히 맞지 않으면 빌드 실패.
- **Producer가 optional 필드 추가 → 롤아웃 중 Backward**. Producer 측의 새 optional 필드를 허용.
- **Consumer가 추가 필드 무시 가능 → 롤아웃 중 Forward**. Consumer가 추가 필드를 허용.
- **안정화 후 → Exact로 다시 강화**.

그리고 기억할 것:

- 변환은 순수하게 유지 (map/filter 안에 부수 효과 없이)
- I/O는 경계에 배치 (한 번 읽고, 한 번 쓰기)
- 부수 효과를 멱등하게 만들기 (재시도 시 중복 쓰기 방지)

컴파일 타임은 drift를 방지하고, 런타임은 현실을 관리한다. 테스트는 동작을 잡고, 구조는 잡지 않는다.

## Part 19 - 프로덕션 패턴: 배포하며 배운 것

### Pattern 1: 계약을 버전별로 정리

CustomerV1.scala
```scala
package contracts
final case class CustomerV1(id: Long, email: String)
```

CustomerV2.scala
```scala
package contracts
final case class CustomerV2(id: Long, email: String, name: Option[String] = None)
```

CustomerMigration.scala
```scala
import contracts._

val migration = PipelineBuilder[CustomerV2]("v1-to-v2")
  .addSource[CustomerV1](sourceV1)
  .transformAs[CustomerV2](addNameField)
  .addSink[CustomerV2, SchemaPolicy.Exact](sinkV2)
  .build
```

### Pattern 2: companion object로 스키마 캐싱

```scala
case class User(id: Long, email: String)
object User {
  given SparkSchema[User] = summon[SparkSchema[User]] // 한 번 계산, 재사용
}
```

### Pattern 3: 정책 선택 이유를 인라인으로 문서화

```scala
// Good: 명시적 근거
.addSink[Contract, SchemaPolicy.Backward](sink) // Q2 마이그레이션 중 optional 필드 허용

// Bad: 맥락 없음
.addSink[Contract, SchemaPolicy.Full](sink) // 왜 Full? 언제 강화할 건가?
```

### Pattern 4: 컴파일 실패 케이스 테스트

```scala
// 문서화로서 버전 관리에 보관
test("CustomerV1 should not conform to CustomerV2 under Exact") {
  assertDoesNotCompile("summon[Conforms[CustomerV1, CustomerV2, Policy.Exact]]")
}
```

## Appendix - 전체 데모 프로젝트 구조 (복사해서 실행)

```
.
├── build.sbt
└── src
    ├── main
    │   └── scala
    │       └── com/example/
    │           ├── IntroMacro.scala
    │           ├── Shape.scala
    │           ├── Policy.scala
    │           ├── Conforms.scala
    │           └── SparkCore.scala
    └── test
        └── scala
            └── com/example/
                ├── ShapeSpec.scala
                ├── ConformsSpec.scala
                └── CompileTimeSpec.scala
```

**build.sbt (Scala 3)**

```scala
ThisBuild / scalaVersion := "3.3.3"
ThisBuild / organization := "com.example"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-sql" % "3.5.1" % "provided",
  "org.scalatest"    %% "scalatest" % "3.2.17" % Test
)
```

실행:

```bash
sbt "runMain ctdc.CtdcPoc"
```

> **Scala 2 설정:** Scala 2의 경우 scala-reflect가 포함된 별도 모듈을 추가하고 위의 S2 파일을 복사하면 된다.

## References

### Scala 3 Macros & Metaprogramming

- [Scala 3 Macros - Official overview](https://docs.scala-lang.org/scala3/guides/macros/macros.html)
- [Scala 3 Reflection guide](https://docs.scala-lang.org/scala3/guides/macros/reflection.html)
- [Scala 3 Macro best practices](https://docs.scala-lang.org/scala3/guides/macros/best-practices.html)
- [Rock the JVM - Scala Macros and Metaprogramming (course)](https://rockthejvm.com/courses/scala-macros-and-metaprogramming)
- [Rock the JVM - Scala 3 Macros comprehensive guide](https://rockthejvm.com/articles/scala-3-macros-comprehensive-guide)

### Apache Spark integration

- [Spark DataType structural comparators](https://spark.apache.org/docs/3.5.1/api/scala/org/apache/spark/sql/types/DataType%24.html)
- [Spark Dataset implicits (toDF/toDS)](https://spark.apache.org/docs/3.5.1/api/scala/org/apache/spark/sql/DatasetHolder.html)
- [Spark CSV data source](https://spark.apache.org/docs/3.5.1/sql-data-sources-csv.html)
- [Scala 3 encoders for Spark (community)](https://index.scala-lang.org/vincenzobaz/spark-scala3-encoders)
- [Rock the JVM - Apache Spark Essentials](https://rockthejvm.com/courses/apache-spark-essentials-with-scala)

### Type-Level programming

- [Rock the JVM - Type level](https://rockthejvm.com/courses/typelevel-rite-of-passage)
- [Rock the JVM - Advanced Scala](https://rockthejvm.com/courses/advanced-scala)
- [Rock the JVM - Cats](https://rockthejvm.com/courses/cats)

### Phantom types & type-state / type-level builders

- [Compile-safe builder pattern using phantom types (Xebia)](https://xebia.com/blog/compile-safe-builder-pattern-using-phantom-types-in-scala/)
- [Phantom Types in Scala - Builder Pattern (Rhetorical Musings)](https://blog.rhetoricalmusings.com/posts/builder1/)

### Other Resources

- [Magnolia - Generic derivation library](https://github.com/softwaremill/magnolia)

## 컴파일 타임 계약이 잡지 못하는 것

솔직해지자. 컴파일 타임 계약이 많은 것을 다루지만 마법은 아니다. 잡지 **못하는** 것:

| 범주 | 잡지 못하는 것 | 이유 | 해결책 |
|------|---------------|------|--------|
| **외부 데이터 drift** | 알리지 않고 바뀌는 서드파티 API, 조율하지 않는 팀의 Kafka 토픽, 외부 벤더가 쓰는 S3 버킷 | 상대의 빌드 프로세스를 제어할 수 없다 | 런타임 검증 (스키마 레지스트리, 데이터 계약) |
| **지연 도착 스키마 변경** | 배포 후 업데이트되는 스키마 레지스트리, 잡 실행 중 추가되는 DB 컬럼 | 컴파일 타임은 배포 전에 일어난다 | 버전 모니터링이 포함된 런타임 검사 |
| **데이터 품질 문제** | 값이 있을 것으로 기대하는 곳의 null (non-optional 필드라도), 범위 밖 숫자 (age = -5), 잘못된 문자열 (@ 없는 이메일) | 컴파일 타임은 구조를 검사하지 내용은 아니다 | Great Expectations, Deequ, 커스텀 검증 |
| **호환성을 깨지 않는 추가** | 아직 사용하지 않는 상류의 optional 필드 추가 | 보통 괜찮다! `Forward` 정책이 처리 | 정책 기반 인지 또는 더 엄격한 모니터링 |
| **부분 배치 실패** | 1000건은 스키마 일치, 5건은 불일치 | 컴파일 타임은 이진적 (컴파일되거나 안 되거나) | 에러 테이블/격리가 포함된 런타임 검증 |

**결론**: 컴파일 타임 계약은 개발과 배포 중에 **직접 관리하는 코드베이스 내의** drift를 잡는다. 나머지 모든 것, 즉 외부 소스, 런타임 변경, 데이터 품질에 대해서는 런타임 검증을 겹겹이 적용하라. 심층 방어라 생각하면 된다: 컴파일 타임이 첫 번째 관문, 런타임이 안전망.

## Conclusions

- 컴파일 타임 증거 + 정책 타입으로 스키마 의도를 명시적이고 강제 가능하게 만든다
- TypeInspector 패턴이 타입 검사를 조합 가능한 유틸리티로 정리한다
- Phantom type과 type-indexing/type-state 빌더가 컴파일 타임 상태 기계를 통해 잘못 사용할 수 없는 API를 만든다
- 필드 vs 요소 optionality가 중첩 타입에서 중요하다
- 중첩 구조(Map, List, case class)는 재귀적 shape 빌드로 동작한다
- 실제 마이그레이션: 롤아웃 중 Backward, 안정화 후 Exact로 강화
- Spark의 비교기가 런타임 심층 방어를 제공한다
- 스키마 진화에는 버전 관리, 공존 전략, 정책 기반 전환이 필요하다
- 개발자 경험이 중요하다: 명확한 오류, 빠른 컴파일 시간, 쉬운 온보딩
- 컴파일 타임은 런타임 검증을 대체하지 않는다. 관리 대상 시스템을 보완한다
- 많은 drift 유형을 일찍 잡지만, 외부 데이터에 대해서는 여전히 런타임 검사가 필요하다
- 컴파일 타임 계약이 파이프라인을 깨지지 않게 만들지는 않는다. 깨짐을 예측 가능하게 만든다

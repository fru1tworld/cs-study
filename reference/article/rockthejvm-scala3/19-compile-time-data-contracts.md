> 원본: https://rockthejvm.com/articles/compile-time-data-contracts-scala-3

# Scala 3의 컴파일 타임 데이터 계약

## 목차

1. [서론](#서론)
2. [왜 중요한가](#왜-중요한가)
3. [무엇을 만들 것인가](#무엇을-만들-것인가)
4. [Part 1 - Scala 3: 핵심 개념 (inline + quotes)](#part-1---scala-3-핵심-개념-inline--quotes)
5. [Part 2 - 컴파일 타임에 Shape 정의하기 (Scala 3)](#part-2---컴파일-타임에-shape-정의하기-scala-3)
6. [Part 3 - 정책: Exact / Backward / Forward](#part-3---정책-exact--backward--forward)
7. [Part 4 - 기존 도구와의 관계](#part-4---기존-도구와의-관계)
8. [Part 5 - TypeRepr 이해하기: 컴파일러의 타입 관점](#part-5---typerepr-이해하기-컴파일러의-타입-관점)
9. [Part 6 - TypeInspector 패턴: 한 번에 하나의 질문](#part-6---typeinspector-패턴-한-번에-하나의-질문)
10. [Part 7 - Shape를 재귀적으로 구성하기: 인터프리터 패턴](#part-7---shape를-재귀적으로-구성하기-인터프리터-패턴)
11. [Part 8 - 필드 선택성 vs 요소 선택성: 핵심 구분](#part-8---필드-선택성-vs-요소-선택성-핵심-구분)
12. [Part 9 - 컴파일 타임 "conforms" 증거 (전체 그림)](#part-9---컴파일-타임-conforms-증거-전체-그림)
13. [Part 10 - 개발자 경험: 실제 사용감](#part-10---개발자-경험-실제-사용감)
14. [Part 11 - 중첩 타입: Map, List, 깊은 구조](#part-11---중첩-타입-map-list-깊은-구조)
15. [Part 12 - 정책 변형자 2분 요약 (Ordered, CI, By-Position)](#part-12---정책-변형자-2분-요약-ordered-ci-by-position)
16. [Part 13 - Phantom 타입과 type-state 빌더: 오용 불가능한 API 만들기](#part-13---phantom-타입과-type-state-빌더-오용-불가능한-api-만들기)
17. [Part 14 - 스키마 진화와 버전 관리: 변화 다루기](#part-14---스키마-진화와-버전-관리-변화-다루기)
18. [Part 15 - 컴파일 타임 테스트, 제대로 하기 (복사해서 바로 쓰는 테스트)](#part-15---컴파일-타임-테스트-제대로-하기)
19. [Part 16 - Scala 2: 같은 일을 하는 방법 (그리고 차이점)](#part-16---scala-2-같은-일을-하는-방법)
20. [Part 17 - Spark 통합: 런타임 심층 방어](#part-17---spark-통합-런타임-심층-방어)
21. [Part 18 - 내일 당장 적용할 수 있는 마이그레이션 플레이북](#part-18---마이그레이션-플레이북)
22. [Part 19 - 프로덕션 패턴: 실제 배포하며 배운 것들](#part-19---프로덕션-패턴)
23. [부록 - 전체 데모 프로젝트 뼈대 (복사 & 실행)](#부록---전체-데모-프로젝트-뼈대)
24. [참고 자료](#참고-자료)
25. [컴파일 타임 계약이 잡지 못하는 것](#컴파일-타임-계약이-잡지-못하는-것)
26. [결론](#결론)

---

## 들어가며

깨진 파이프라인이 아예 실행되지 않는다면 어떨까 - 컴파일러가 미리 막아줬으니까?

> "컴파일이 되면 데이터 계약도 맞다." 스키마 불일치를 Scala 3에서 컴파일 타임에 잡아보자.

이 문제를 꽤 오래 고민해왔다. 다들 겪어봤을 것이다 - 데이터 파이프라인을 잘 돌리고 있는데, 누군가 업스트림에서 필드 이름을 바꿔버리면 새벽 2시에 잡이 터진다. 온콜 엔지니어(아마 본인)가 호출당하고, 뭐가 잘못됐는지 파악하느라 한 시간을 쓰게 된다.

한번은 업스트림 팀이 아무 공지 없이 컬럼명을 `amount` → `amt`로 바꾼 적이 있었다. 파이프라인은 바로 죽지 않았다 - 몇 주 동안 아무 문제 없다는 듯이 null을 써댔다. 발견했을 때는 이미 수백만 건을 백필해야 했다. 그날 깨달았다: 런타임 검증은 문제를 늦게 잡고, 컴파일 타임 계약은 훨씬 일찍 잡는다.

이 글에서는 이런 문제를 컴파일 타임에 잡는 방법을 보여준다. 런타임이 아니다. 테스트 타임도 아니다. 컴파일 타임이다. 스키마가 어긋나면 코드가 아예 빌드되지 않는다.

처음부터 차근차근 만들어볼 것이다 - 먼저 Scala 3 매크로가 어떻게 동작하는지 이해하고, 코드가 실행되기도 전에 스키마 호환성을 증명하는 최소한의 계약 시스템을 작성한다. Scala 2 버전도 함께 보여줄 테니 아직 Scala 2를 쓰는 분들도 참고하면 된다.

여기 나오는 코드는 전부 일반 sbt 프로젝트에서 돌아간다. [GitHub](https://github.com/vim89/compile-time-data-contracts)에서 동작하는 코드를 받아서 직접 실행해볼 수 있다.

## 왜 중요한가

테스트를 작성하는 건 중요하다. 하지만 테스트는 샘플일 뿐이다 - 깨질 것 같다고 생각하는 부분만 확인한다. 반면 컴파일러는 _구조를 빠짐없이 검증한다_. 스키마가 일치하는지 증명할 수 있다 - 모든 필드, 모든 타입, 매번.

실제 프로젝트에서 스키마는 수시로 바뀐다. 그리고 바뀔 때:

  * 프로덕션 잡이 아니라 빌드가 먼저 실패해야 한다
  * 변환은 순수하게, 사이드 이펙트는 경계에만 두자 (재시도 시 중복 쓰기를 방지하려면)
  * 무거운 작업은 컴파일러에게 맡기자

범위 한정

컴파일 타임 계약은 직접 관리하는 코드베이스 내의 드리프트를 잡아준다 - 작성하는 변환, 정의하는 case class, 함께 버전 관리하는 스키마. 업스트림 API 변경, 늦게 도착하는 스키마 레지스트리 업데이트, 독립적으로 변하는 외부 데이터 소스는 잡지 못한다. 직접 제어하는 것에는 컴파일 타임을, 나머지에는 런타임 검증을 쓰자. 둘 중 하나가 아니라 다층 방어다.

## 무엇을 만들 것인가

작은 컴파일 타임 검증 시스템을 만들어볼 것이다:

  1. case class 구조를 컴파일 타임에 기술하는 방법 ("shape"이라 부를 것이다)
  2. 정책 시스템 - Exact, Backward, Forward 호환 모드
  3. "이 두 shape이 정책 P 하에서 호환되는지" 증명하는 매크로 - 호환되지 않으면 컴파일을 거부한다
  4. 올바른 구성 순서를 강제하는 팬텀 타입 파이프라인 빌더

먼저 Scala 3 (inline + quotes 사용), 그 다음 Scala 2 (blackbox 매크로 사용) 순으로 진행한다. 차이점은 그때그때 짚어주겠다.

### GitHub에서 코드 실행하기

**Terminal window**
```bash
git clone https://github.com/vim89/compile-time-data-contracts.git
cd compile-time-data-contracts
# Scala 3
sbt "runMain ctdc.CtdcPoc"
```
참고

GitHub의 코드에는 바로 실행해볼 수 있는 예제가 포함되어 있다. 컴파일 실패 케이스는 모두 인라인으로 문서화되어 있으니, 주석을 해제하면 컴파일러 에러를 직접 확인할 수 있다.

* * *

## Part 1 - Scala 3: 멘탈 모델 (inline + quotes)

Scala 3 매크로는 Scala 2와 다르다. 사실 더 낫다. 더 안전하고 이해하기 쉽다.

핵심 아이디어 두 가지:

  * `inline def`: 컴파일러에게 "이걸 컴파일 타임에 전개하라"고 지시한다
  * `quotes` + `Expr[T]`: 타입이 붙은 AST를 받아서 검사할 수 있다

간단한 매크로로 동작 방식을 살펴보자:

**IntroMacro.scala**
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

  * `inline` 메서드가 사용자가 호출하는 부분이다
  * `${ ... }` splice가 구현부를 호출한다
  * 구현부는 `'x` (quoted 코드)를 `Expr[Any]`로 받는다
  * `Expr[String]`을 반환한다 - 컴파일러가 삽입할 AST다

실행해보자:

**Demo.scala**
```scala
package com.example
@main def run(): Unit =
  println(IntroMacro.show(1 + 2)) // prints: You passed: 1.+(2)
```
`1 + 2`의 AST가 출력된 것을 보라. 이것이 핵심이다 - 컴파일 타임에 코드 구조를 들여다볼 수 있다.

더 자세한 내용은 [Scala 3 매크로 개요](https://docs.scala-lang.org/scala3/guides/macros/macros.html), [모범 사례](https://docs.scala-lang.org/scala3/guides/macros/best-practices.html), 그리고 [Rock the JVM Macros & Metaprogramming 강좌](https://rockthejvm.com/courses/scala-macros-and-metaprogramming)를 참고하자.

빠른 팁

Scala 3 매크로가 처음이라면, REPL에서 매크로가 생성하는 코드를 읽어보는 것부터 시작하자: sbt> console, 그다음 scala> import scala.quoted.*, 그러면 매크로가 실제로 무엇으로 전개되는지 확인할 수 있다. 생성된 코드를 이해하면 매크로 디버깅의 80%는 해결된다. 컴파일러가 일을 하는 것이니 - 실제로 뭘 만들어내는지만 보면 된다.

* * *

## Part 2 - 컴파일 타임에 shape 기술하기 (Scala 3)

이제 case class 구조를 기술해야 한다. 실제 프로젝트에서는 Shapeless, Magnolia, 또는 Scala 3의 Mirror를 쓸 수 있다. 여기서는 외부 의존성 없이 완결되는 Mirror 기반 버전을 보여주겠다.

아이디어: `A`의 필드를 알고 있는 타입클래스 `Shape[A]`.

**Shape.scala**
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
  given [A]: Shape[Option[A]] with { def fields = Nil } // 요소의 옵셔널 여부는 다른 곳에서 처리
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
코드가 좀 빽빽하다. 하지만 하는 일은 이렇다:

  * Scala 3의 Mirror를 사용해 컴파일 타임에 case class를 리플렉션한다
  * 필드 이름과 타입을 추출한다
  * 옵셔널 필드(`Option`으로 감싸진 필드)를 표시한다
  * 구조를 기술하는 `List[Field]`를 반환한다

사용법:

**ShapeSpec.scala**
```scala
package com.example
final case class User(id: Long, email: String, note: Option[String])
@main def checkShape(): Unit =
  val s = summon[Shape[User]]
  println(s.fields) // List(Field("id","Long"), Field("email","String"), Field("note","Option[String]",false,true))
```
이제 컴파일러에게 "이 case class에 어떤 필드가 있나?"라고 컴파일 타임에 물어볼 수 있다. 이것이 기반이다.

대안적 접근

라이브러리를 선호한다면 [Magnolia](https://github.com/softwaremill/magnolia)를 확인해보자 - Scala 2와 3 모두에서 잘 동작한다.

* * *

## Part 3 - 정책: Exact / Backward / Forward

여기서 호환성 규칙을 정의한다. 마이그레이션 전략이라고 생각하면 된다:

**Policy.scala**
```scala
package com.example
sealed trait Policy
object Policy {
  sealed trait Exact     extends Policy
  sealed trait Backward  extends Policy // 프로듀서가 옵셔널/기본값 필드를 추가할 수 있음
  sealed trait Forward   extends Policy // 컨슈머가 추가 필드를 허용함
  case object Exact    extends Exact
  case object Backward extends Backward
  case object Forward  extends Forward
}
```
왜 세 가지 정책인가?

  * **Exact**: 스키마가 정확히 일치해야 한다. 예외 없다. 중요한 경로에 쓴다.
  * **Backward**: 프로듀서가 옵셔널 필드를 추가할 수 있다. 컨슈머를 깨뜨리지 않으면서 새 기능을 배포할 때 유용하다.
  * **Forward**: 컨슈머가 추가 필드를 무시한다. 프로듀서가 필요 이상으로 많은 데이터를 보낼 수 있을 때 유용하다.

프로덕션 마이그레이션 전략

실전에서 통하는 정책 진행 방식: 안정적인 파이프라인에는 Exact, 프로듀서 롤아웃 중에는 Backward (새 옵셔널 필드 허용), 컨슈머 업데이트 중에는 Forward (추가 필드 허용), 마이그레이션 완료 후 다시 Exact. 예상 타임라인을 문서화하자 - "Q2 마이그레이션 동안 Backward 정책 적용 후 Exact로 전환." 정책 드리프트가 영구적으로 고착되는 것을 방지한다.

* * *

## Part 4 - 기존 도구들과의 관계

본격적으로 들어가기 전에, 이런 생각이 들 수 있다: "이미 [Avro](https://avro.apache.org/), [Protobuf](https://protobuf.dev/), 스키마 레지스트리, [Great Expectations](https://greatexpectations.io/)가 있는데, 왜 컴파일 타임 계약을 추가하나?"

당연한 질문이다. 비교해보자:

**스키마 레지스트리 ([Confluent Schema Registry](https://docs.confluent.io/platform/current/schema-registry/index.html), [AWS Glue Schema Registry](https://docs.aws.amazon.com/glue/latest/dg/schema-registry.html))**

  * **하는 일**: 버전 관리를 갖춘 중앙 집중식 스키마 저장소
  * **도움이 될 때**: 서비스 간 런타임 검증, 스키마 진화 추적
  * **빈틈**: 불일치를 잡이 실행될 때 발견한다, 컴파일할 때가 아니라
  * **함께 쓰기**: 서비스 간 계약에는 스키마 레지스트리, 리포지토리 내부 검사에는 컴파일 타임

**[Avro](https://avro.apache.org/) / [Protobuf](https://protobuf.dev/)**

  * **하는 일**: 스키마가 내장된 바이너리 직렬화
  * **도움이 될 때**: 네트워크 효율성, 서비스 간 강한 계약
  * **빈틈**: 스키마 호환성을 런타임에 또는 외부 도구로 검사한다
  * **함께 쓰기**: 와이어 포맷에는 Avro/Protobuf, case class가 해당 스키마와 일치하는지는 컴파일 타임 검사

**[Great Expectations](https://greatexpectations.io/) / [Deequ](https://github.com/awslabs/deequ)**

  * **하는 일**: 런타임 데이터 품질 검증 (null, 범위, 분포)
  * **도움이 될 때**: 잘못된 데이터 값, 통계적 이상 탐지
  * **빈틈**: 데이터 내용을 검증하지, 컴파일 타임에 스키마 구조를 검증하지는 않는다
  * **함께 쓰기**: 구조에는 컴파일 타임, 데이터 품질에는 Great Expectations

**컴파일 타임의 장점**: 배포 전에, 직접 관리하는 코드베이스 내의 드리프트를 잡아낸다. 변환이 필드 `X`를 기대하는데 소스가 필드 `Y`를 생산하면 빌드가 실패한다 - 프로덕션 잡이 아니라. 함께 빌드하고 버전 관리하는 시스템을 위한 추가 안전 계층이라고 생각하면 된다.

실제 시나리오: 레지스트리에 Avro 스키마가 있고, Scala 파이프라인이 이를 case class로 읽어들인다. 컴파일 타임 계약은 배포 전에 해당 case class가 기대하는 계약과 일치하는지 검증한다. 업스트림이 Avro 스키마를 바꾸면 빌드가 깨진다 - 잡이 터지기를 기다리지 않는다.

* * *
## Part 5 - TypeRepr 이해하기: 컴파일러가 타입을 바라보는 방식

매크로 구현에 들어가기 전에, `TypeRepr`을 먼저 이해해야 한다. Scala 3에서 컴파일 타임에 타입을 표현하는 방식이다.

이렇게 생각하면 된다. `List[String]`이라고 작성하면, 컴파일러는 단순한 텍스트로 보지 않는다. 구조화된 트리로 인식한다:
```
AppliedType(List, [String])
   │               │
   │               └─ String에 대한 TypeRepr
   └─ 타입 생성자
```

**주요 TypeRepr 연산:**
```scala
val t: TypeRepr = TypeRepr.of[List[String]]
// 1. 서브타입 검사: List[String]이 Seq[?]의 서브타입인가?
t <:< TypeRepr.of[Seq[?]]  // true
// 2. 타입 동일성 검사: 이것이 정확히 String인가?
t =:= TypeRepr.of[String]  // false
// 3. 패턴 매칭: 분해하기
t match {
  case AppliedType(tycon, args) =>
    // tycon = List
    // args = [String]
}
```
**왜 필요한가:** 스키마를 비교하려면 다음과 같은 질문에 답할 수 있어야 한다:

  * "이 필드가 Option인가?"
  * "이 List 안에 뭐가 들어 있는가?"
  * "이 case class에 주 생성자가 있는가?"

`TypeRepr`을 사용하면 이런 질문에 컴파일 타임에 답할 수 있다.

* * *

## Part 6 - TypeInspector 패턴: 한 번에 하나의 질문만

이제 유틸리티 함수들을 만들어 보자. 각 함수는 타입에 대한 질문 하나에만 답한다. 간단한 것부터 시작한다:

### Inspector 1: case class인가?
```scala
def isCaseClass(t: TypeRepr): Boolean =
  val s = t.typeSymbol  // 심볼 가져오기 (타입의 "이름" 같은 것)
  s.isClassDef && s.flags.is(Flags.Case)
```
**무슨 일이 일어나는가:**

  * `typeSymbol` - 모든 타입에는 심볼이 있다 (고유 ID 같은 것)
  * `isClassDef` - 클래스인가? (trait, object 등이 아니라)
  * `flags.is(Flags.Case)` - `case` 수정자가 붙어 있는가?

**직접 확인해 보자:**
```scala
case class User(id: Long)  // YES - isClassDef=true, Case 플래그 있음
trait UserTrait            // NO - isClassDef=false
object UserObject          // NO - 클래스가 아님
```
### Inspector 2: 타입 인자는 무엇인가?
```scala
def appliedArgs(t: TypeRepr): List[TypeRepr] = t match {
  case AppliedType(_, args) => args  // 인자 추출
  case _                    => Nil   // 인자 없음
}
```
**무슨 일이 일어나는가:**

  * `AppliedType` - `List[String]`이나 `Map[String, Int]` 같은 제네릭 타입의 패턴
  * `args` - 타입 파라미터: `[String]` 또는 `[String, Int]`

**예시:**
```
List[String]        // AppliedType(List, [String])
                    // appliedArgs는 [String]을 반환
Map[String, Int]    // AppliedType(Map, [String, Int])
                    // appliedArgs는 [String, Int]을 반환
String              // AppliedType이 아님
                    // appliedArgs는 Nil을 반환
```
### Inspector 3: Option인가?
```scala
def optionArg(t: TypeRepr): Option[TypeRepr] =
  if t <:< TypeRepr.of[Option[?]] then  // Option[Something]의 서브타입인가?
    appliedArgs(t).headOption           // 맞다면 Something을 추출
  else None                             // 아니라면 None
```
**무슨 일이 일어나는가:**

  * `<:<` - 서브타입 검사: "이 타입이 Option[?]과 호환되는가?"
  * `Option[?]` - `?`는 "어떤 것이든 담은 Option"을 의미
  * `appliedArgs(t).headOption` - 첫 번째(이자 유일한) 타입 인자를 가져옴

**예시:**
```
Option[String]  // YES - Option[?]의 서브타입
                // appliedArgs는 [String]을 반환
                // headOption은 Some(String)을 반환
Some[Int]       // YES - Some은 Option의 서브타입
                // Some(Int)를 반환
String          // NO - Option이 아님
                // None을 반환
```
**왜 필요한가:** 필드 수준의 선택성(optionality) 때문이다. `age: Option[Int]`를 만나면 다음을 알아야 한다:

  1. 필드 자체가 선택적(optional)이다
  2. 내부 타입은 `Int`이다

### Inspector 4: 시퀀스인가?
```scala
def seqArg(t: TypeRepr): Option[TypeRepr] =
  val isSeqLike =
    t <:< TypeRepr.of[List[?]] ||
    t <:< TypeRepr.of[Seq[?]] ||
    t <:< TypeRepr.of[Vector[?]] ||
    t <:< TypeRepr.of[Array[?]] ||
    t <:< TypeRepr.of[Set[?]]
  if isSeqLike then appliedArgs(t).headOption else None
```
**무슨 일이 일어나는가:**

  * 시퀀스 계열 타입인지 검사
  * 맞다면 내부에 들어 있는 것(원소 타입)을 추출

**왜 여러 타입을 검사하는가?** Scala에는 컬렉션 타입이 다양하기 때문이다:
```
List[String]    // List[?]인가? YES
Seq[Int]        // Seq[?]인가? YES
Vector[Long]    // Vector[?]인가? YES
Array[Byte]     // Array[?]인가? YES
Set[String]     // Set[?]인가? YES
```
이 모든 것이 `SequenceShape(element)`로 변환되어야 한다.

### Inspector 5: Map인가?
```scala
def mapArgs(t: TypeRepr): Option[(TypeRepr, TypeRepr)] =
  if t <:< TypeRepr.of[Map[?, ?]] then {
    appliedArgs(t) match {
      case k :: v :: Nil => Some((k, v))  // key와 value를 가져옴
      case _ => report.errorAndAbort(s"Map requires two type args: ${t.show}")
    }
  } else None
```
**무슨 일이 일어나는가:**

  * `Map[Something, Something]`인지 검사
  * 타입 인자 두 개(key와 value)를 모두 추출
  * Map의 인자 개수가 맞지 않으면 중단 (컴파일 에러)

**예시:**
```
Map[String, Int]  // true - Some((String, Int))를 반환
Map[Long, User]   // true - Some((Long, User))를 반환
String            // false - None을 반환
```
### Inspector 6: Map의 key가 될 수 있는가?
```scala
def isAtomicKey(t: TypeRepr): Boolean =
  t =:= TypeRepr.of[String] ||
  t =:= TypeRepr.of[Int] ||
  t =:= TypeRepr.of[Long] ||
  t =:= TypeRepr.of[Short] ||
  t =:= TypeRepr.of[Byte] ||
  t =:= TypeRepr.of[Boolean]
```
**왜 중요한가:** Spark StructType의 Map은 원시 타입 key만 지원한다. `Map[User, String]`은 불가능하다 - User는 원시 타입이 아니기 때문이다.

**예시:**
```
Map[String, User]  // YES - String은 원시 타입
Map[Int, Data]     // YES - Int는 원시 타입
Map[User, String]  // NO - User는 원시 타입이 아님 - 컴파일 에러!
```
### 패턴 정리:

각 Inspector는 하나의 역할만 수행한다:

  1. **isCaseClass** - "case class인가?"
  2. **appliedArgs** - "제네릭 타입 인자가 무엇인가?"
  3. **optionArg** - "Option인가, 그렇다면 안에 뭐가 있는가?"
  4. **seqArg** - "시퀀스인가, 그렇다면 원소 타입이 무엇인가?"
  5. **mapArgs** - "Map인가, 그렇다면 key/value 타입이 무엇인가?"
  6. **isAtomicKey** - "Map의 key로 쓸 수 있는가?"

이것들을 조합하면 복잡한 질문에도 답할 수 있다. 이것이 TypeInspector 패턴이다.

* * *

## Part 7 - 재귀적으로 Shape 만들기: 인터프리터 패턴

이제 앞서 만든 Inspector들을 활용해 `TypeShape`를 만들어 보자. 타입 구조의 내부 표현이다.

이 과정은 재귀적이다. `List[Option[User]]`에 대해 다음과 같이 만들어진다:
```
SequenceShape(
  OptionalShape(
    StructShape([field1, field2, ...])
  )
)
```
알고리즘을 단계별로 살펴보자.

### Shape 생성 알고리즘:

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
**순서가 중요하다!** Option을 먼저 검사하고, 그다음 시퀀스, Map, case class 순으로, 마지막에 원시 타입으로 폴백한다.

### 예제를 따라가 보자: `List[Option[String]]`

  * **Step 1:** `List[Option[String]]`이 Option인가?
    * 아니다. `optionArg`이 `None`을 반환한다.
    * Step 2로 진행.
  * **Step 2:** 시퀀스인가?
    * 맞다! `seqArg`이 `Some(Option[String])`을 반환한다.
    * `SequenceShape(typeShapeOf(Option[String]))`를 생성한다.
    * `Option[String]`에 대해 **재귀 호출**.
  * **재귀 Step 1:** `Option[String]`이 Option인가?
    * 맞다! `optionArg`이 `Some(String)`을 반환한다.
    * `String`에 대해 **재귀 호출**.
  * **재귀 Step 2:** `String`이 Option인가? -> 아니다
  * **재귀 Step 3:** `String`이 시퀀스인가? -> 아니다
  * **재귀 Step 4:** `String`이 Map인가? -> 아니다
  * **재귀 Step 5:** `String`이 case class인가? -> 아니다
  * **재귀 Step 6:** 원시 타입으로 폴백 —> `PrimitiveShape("String")`을 반환.

**재귀를 풀어보면:**
```
PrimitiveShape("String")
  → (Option 재귀에서)
  → SequenceShape(PrimitiveShape("String"))
```
잠깐, `Option`은 어디로 갔는가? 소비되어 사라졌다! `Option`에 대한 `typeShapeOf`는 내부 타입에 대해 그냥 재귀할 뿐이다. 이것이 앞서 언급한 엣지 케이스다 - 원소 수준의 선택성(optionality) 정보가 유실된다.

### case class 처리:
```scala
if isCaseClass(t) then {
  val sym    = t.typeSymbol                      // 클래스 심볼 가져오기
  val params = sym.primaryConstructor.paramSymss.flatten  // 생성자 파라미터 가져오기
  val fields = params.map { p =>
    val name       = p.name                      // 필드 이름
    val ptpe       = t.memberType(p)             // 필드 타입
    val hasDefault = p.flags.is(Flags.HasDefault) // 기본값이 있는가?
    val (uT, isOpt) = optionArg(ptpe).fold(ptpe -> false)(a => a -> true)
    FieldShape(name, typeShapeOf(uT), hasDefault, isOpt)
  }
  StructShape(fields)
}
```
**한 줄씩 뜯어보자:**

  1. **`t.typeSymbol`** - case class의 심볼을 가져온다
  2. **`sym.primaryConstructor`** - case class는 주 생성자를 갖는다
  3. **`.paramSymss.flatten`** - 모든 파라미터를 가져온다 (flatten은 다중 파라미터 목록을 처리)
  4. **각 파라미터 `p`에 대해:**
     * **`p.name`** - 필드 이름: `"id"`, `"email"` 등
     * **`t.memberType(p)`** - 필드의 타입을 TypeRepr로 가져옴
     * **`p.flags.is(Flags.HasDefault)`** - `age: Int = 0`처럼 기본값이 있는지 확인
     * **`optionArg(ptpe).fold(...)`** - 필드 타입이 Option[T]인지 확인:
       * 맞으면: T를 추출하고, `isOpt = true`
       * 아니면: 타입을 그대로 사용하고, `isOpt = false`
     * **`typeShapeOf(uT)`** - **재귀 호출!** 필드의 내부 타입에 대한 Shape를 생성

### 예제:

`case class User(id: Long, email: String, age: Option[Int] = None)`

**처리 과정:**

  1. **필드 1: id**

     * `name` = "id"
     * `ptpe` = `Long`에 대한 TypeRepr
     * `hasDefault` = false
     * `optionArg(Long)` = None → `uT = Long`, `isOpt = false`
     * `typeShapeOf(Long)` = `PrimitiveShape("Long")`
     * 결과: `FieldShape("id", PrimitiveShape("Long"), hasDefault=false, isOptional=false)`
  2. **필드 2: email**

  * `name` = "email"
  * `ptpe` = `String`에 대한 TypeRepr
  * `hasDefault` = false
  * `optionArg(String)` = None → `uT = String`, `isOpt = false`
  * `typeShapeOf(String)` = `PrimitiveShape("String")`
  * 결과: `FieldShape("email", PrimitiveShape("String"), hasDefault=false, isOptional=false)`

  3. **필드 3: age**

  * `name` = "age"
  * `ptpe` = `Option[Int]`에 대한 TypeRepr
  * `hasDefault` = **true** (`= None`이 있으므로)
  * `optionArg(Option[Int])` = Some(`Int`) → `uT = Int`, `isOpt = true`
  * `typeShapeOf(Int)` = `PrimitiveShape("Int")`
  * 결과: `FieldShape("age", PrimitiveShape("Int"), hasDefault=true, isOptional=true)`

  4. **최종 Shape:**
```
     StructShape([
       FieldShape("id", PrimitiveShape("Long"), false, false),
       FieldShape("email", PrimitiveShape("String"), false, false),
       FieldShape("age", PrimitiveShape("Int"), true, true)
     ])
```
### 왜 이것이 중요한가:

이제 case class의 정규화된 표현을 갖게 되었다. 원래의 Scala 문법과 무관하게, 두 `StructShape`를 비교해서 일치하는지 확인할 수 있다.

재귀적 구조 덕분에 임의로 중첩된 타입도 처리할 수 있다:
```scala
case class Order(
  items: List[LineItem],           // List로 재귀, 그다음 LineItem으로 재귀
  metadata: Map[String, String],   // Map으로 재귀, 그다음 String으로 (두 번) 재귀
  address: Option[Address]         // Option으로 재귀, 그다음 Address로 재귀
)
```
각 재귀 호출이 트리의 한 조각을 만든다. 이 트리가 곧 스키마다.

* * *
## Part 8 - Field vs Element optionality: 놓치기 쉬운 핵심 차이

대부분의 가이드에서 다루지 않는 미묘한 부분이 있다. GitHub 코드는 두 가지 종류의 optionality를 처리한다:

### 필드 수준 optionality (필드 자체에 Option이 붙는 경우)
```scala
case class User(name: String, age: Option[Int])
//                                   ^^^^^^^^^^^ field-level Option
```
매크로가 이를 감지하면 `FieldShape`에 `isOptional = true`를 설정한다:
```scala
val (uT, isOpt) = optionArg(ptpe).fold(ptpe -> false)(a => a -> true)
FieldShape(name, typeShapeOf(uT), hasDefault, isOpt)
```
### 요소 수준 optionality (컬렉션 내부에 Option이 있는 경우)
```scala
case class Data(items: List[Option[String]])
//                          ^^^^^^^^^^^^^^ element-level Option
```
이 경우 중첩된 shape가 만들어진다: `SequenceShape(OptionShape(PrimitiveShape("String")))`

잠깐, 이건 GitHub 코드에 없다! 확인해보면... 실제로 GitHub 코드의 `typeShapeOf`는 재귀적으로 처리한다:
```
optionArg(t).map(typeShapeOf).getOrElse {
  seqArg(t).map(a => SequenceShape(typeShapeOf(a))).getOrElse { ... }
}
```
따라서 `List[Option[String]]`은 다음과 같이 처리된다:

  1. `seqArg`가 List를 감지 -> `Option[String]` 추출
  2. `Option[String]`으로 재귀 -> `typeShapeOf(Option[String])`
  3. `optionArg`가 Option을 감지 -> `String` 추출
  4. 기본 케이스: `PrimitiveShape("String")`

그런데 최종 shape는 `SequenceShape(PrimitiveShape("String"))`이다 - 중간의 Option이 소비되어 사라진다!

**계약 검사에서 이것이 중요한 이유**: 다음과 같이 작성하면:
```scala
case class Contract(items: List[String])
case class Producer(items: List[Option[String]])
```
Exact 정책에서는 이 둘이 일치하지 않아야 한다. producer는 null 요소를 허용하지만 contract는 그렇지 않기 때문이다. 그러나 현재 GitHub 코드에서는 Option이 벗겨지면서 이를 허용할 수 있다.

이런 종류의 엣지 케이스는 프로덕션에 배포해봐야 발견된다. 수정하려면 모델에 `OptionalShape` 래퍼를 추가해야 한다.

Optionality 엣지 케이스

요소 수준의 optionality 문제(List[Option[String]]이 평탄화되는 현상)는 실제로 존재하며 미묘하다. 스키마에서 nullable 컬렉션을 사용한다면, 컴파일 타임 검사가 List[String]과 List[Option[String]]의 차이를 실제로 잡아내는지 테스트해야 한다. 현재 구현에서는 이 불일치를 놓칠 수 있다. 프로덕션에 의존하기 전에 nullable 컬렉션 패턴에 대한 명시적 테스트 케이스를 추가하자.

* * *

## Part 9 - 컴파일 타임 **"conforms"** 증거 (전체 그림)

이제 모든 것을 하나로 합쳐보자. GitHub 코드의 전체 매크로는 다음과 같다:

**Conforms.scala**
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
    val out      = shapeOf[Out]
    val contract = shapeOf[Contract]
    // Compare fields by name (unordered, case-sensitive for now)
    val outMap      = out.map { case (n,t,_,opt) => n -> (t,opt) }.toMap
    val contractMap = contract.map { case (n,t,_,opt) => n -> (t,opt) }.toMap
    val missing = contract.collect { case (n,t,_,opt) if !outMap.contains(n) => s"$n:$t${if opt then " (optional)" else ""}" }
    val extra   = out.collect      { case (n,_,_,_) if !contractMap.contains(n) => n }
    val mism    = contract.collect {
      case (n,t,_,opt) if outMap.get(n).exists(_._1 != t)  => s"$n expected $t, found ${outMap(n)._1}"
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
각 단계를 분석해보자:

  1. **Shape 추출**: `Shape[Out]`과 `Shape[Contract]`를 소환(summon)하여 필드 목록을 얻는다. `.valueOrAbort`에 주목하자 - 이것이 컴파일 타임 평가를 강제한다.

  2. **비교**: 필드 이름 -> (타입, optionality)의 맵을 만들고 차이점을 찾는다.

  3. **정책 적용**:

     * Exact: 모든 차이가 에러
     * Backward: 누락된 optional 필드는 허용, 추가 필드도 허용
     * Forward: 추가 필드는 허용 (단, 누락된 필수 필드는 불허)
  4. **중단 또는 성공**: 위반 사항이 있으면 `report.errorAndAbort`를 호출하여 정확히 무엇이 잘못되었는지 상세 메시지를 보여준다. 그렇지 않으면 증거를 반환한다.

핵심은 이것이다: **이 모든 것이 컴파일 시점에 실행된다**. 스키마가 일치하지 않으면 코드가 컴파일되지 않는다. 그게 전부다.

### 실제 드리프트 시나리오

Apache Spark 파이프라인 시나리오에서 이것이 어떻게 동작하는지 살펴보자. 데이터 웨어하우스 팀이 다운스트림 분석 대시보드가 의존하는 `CustomerContract`를 관리하고 있다. 업스트림 CRM 팀이 고객 세분화 데이터를 추가하기 시작했고, 이를 통과시킬지 필터링할지 결정해야 한다.

**초기 상태 - 모든 것이 맞아 있는 상태:**

**CustomerPipeline.scala**
```scala
package com.example
// 분석 팀이 의존하는 웨어하우스 계약
final case class CustomerContract(
  id: Long,
  email: String,
  age: Option[Int] = None
)
// 업스트림 CRM 내보내기가 계약과 정확히 일치
final case class CRMExport(
  id: Long,
  email: String,
  age: Option[Int]
)
// 컴파일 타임 검증이 포함된 Spark 파이프라인
val pipeline = PipelineBuilder[Nothing]("customer-sync")
  .addSource(TypedSource[CRMExport]("parquet", "s3://data-lake/crm/exports"))
  .noTransform  // 변환 불필요 - 스키마가 일치하므로
  .addSink[CustomerContract, Policy.Exact](
    TypedSink("s3://warehouse/customers")
  )
  .build
// 컴파일 성공 - CRMExport가 Exact 정책 하에서 CustomerContract와 일치
summon[Conforms[CRMExport, CustomerContract, Policy.Exact]]
```
**업스트림 팀이 세분화 필드를 추가:**

CRM 팀이 새 세분화 기능을 배포하고 내보내기에 포함하기 시작했다. 컴파일 타임 계약이 없었다면 파이프라인은 새 필드를 조용히 무시하거나 (읽기 모드에 따라) 장애가 발생했을 것이다. 컴파일 타임 계약이 있으면 빌드가 즉시 깨진다:
```scala
// CRM 팀의 새 내보내기 스키마에 고객 세그먼트가 포함됨
final case class CRMExportWithSegment(
  id: Long,
  email: String,
  age: Option[Int],
  segment: String  // 신규: "premium", "standard", "trial"
)
// 파이프라인을 업데이트하여 새 소스를 읽도록 변경
val pipeline = PipelineBuilder[Nothing]("customer-sync")
  .addSource(TypedSource[CRMExportWithSegment]("parquet", "s3://data-lake/crm/exports"))
  .noTransform
  .addSink[CustomerContract, Policy.Exact](
    TypedSink("s3://warehouse/customers")
  )
  .build
// 컴파일 에러:
// Compile-time contract drift (policy: Policy.Exact).
// Out: CRMExportWithSegment vs Contract: CustomerContract
// Extra: segment
```
**선택지:**

빌드가 명시적인 결정을 강제한다. 세 가지 일반적인 접근 방식이 있다:
```
// 방법 1: 전환 기간 동안 Backward 정책 사용 (추가 필드 허용)
.addSink[CustomerContract, Policy.Backward](
  TypedSink("s3://warehouse/customers")
)
// 컴파일 성공 - Backward 정책은 producer의 추가 필드를 허용

// 방법 2: transform에서 필드를 명시적으로 제거
.transformAs[CustomerContract]("drop-segment") { df =>
  df.select("id", "email", "age")
}
.addSink[CustomerContract, Policy.Exact](
  TypedSink("s3://warehouse/customers")
)
// 컴파일 성공 - transform이 계약 스키마로 명시적으로 프로젝션

// 방법 3: 계약을 업데이트하여 segment 포함 (분석 팀과 조율 필요)
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
**이것이 중요한 이유:**

컴파일 타임 계약이 없으면 드리프트는 런타임까지 보이지 않는다. Apache Spark 잡이 다음과 같은 상황에 빠질 수 있다:

  * `segment` 필드를 조용히 누락시킴 (스키마 진화 모드)
  * 새벽 2시에 스키마 불일치 에러로 장애 발생
  * 누군가 알아차리기까지 몇 주간 `segment`에 null을 기록

컴파일 타임 계약이 있으면 대화가 개발 시점에 일어난다:

  * "다운스트림 대시보드에 고객 세그먼트가 필요한가?"
  * "계약 버전을 올리고 소비자를 마이그레이션해야 하나?"
  * "이 필드는 임시적인 것이니 필터링해야 하나?"

드리프트를 명시적으로 처리하기 전까지 빌드가 통과하지 않는다. 의도된 설계다.

빌드 실패 알림

컴파일 타임 계약 검사는 스키마가 어긋나면 빌드를 깨뜨린다. 그게 목적이지만, 팀과 조율이 필요하다. 업스트림에서 필드를 추가하면 모든 다운스트림 파이프라인 빌드가 계약이나 transform을 업데이트할 때까지 실패한다. 정렬을 강제하는 효과가 있지만, 관리하지 않으면 배포를 막을 수 있다. CI 브랜치 빌드를 활용하여 스키마 변경을 머지하기 전에 영향 범위를 미리 확인하자.

여기까지가 전체 핵심이다. 프로덕션에서는 중첩 타입과 더 복잡한 시나리오를 처리해야 하겠지만 (GitHub 코드에서 보여주듯이), 구조는 동일하다.

* * *

## Part 10 - 개발자 경험: 실제로 사용하면 어떤 느낌인가

실용적인 측면을 이야기해보자 - 일상적인 작업에서 이것을 실제로 사용하면 어떤 일이 벌어지는가?

### 컴파일 시간

**질문: 컴파일이 느려지나?**

실질적으로 체감되지 않는다. 매크로는 컴파일 중에 `summon[Conforms[...]]` 호출마다 한 번씩 실행된다. 5-10개의 계약 검사가 있는 일반적인 파이프라인이라면 빌드에 100-200ms 정도 추가된다. 프로덕션 스키마 불일치를 디버깅하는 데 수 시간을 쓰는 것과 비교해보자.

### 에러 메시지

스키마가 어긋나면 다음과 같은 메시지가 나온다:
```
[error] Compile-time contract drift (policy: Policy.Exact).
[error] Out: CustomerProducer vs Contract: CustomerContract
[error] Extra: segment
[error] Missing:
[error] Mismatched:
```
명확하고 실행 가능하며 빌드를 멈춘다. 난해한 매크로 에러도 없고 스택 트레이스도 없다. 그냥: "`segment`라는 추가 필드가 있습니다."

### 통합 패턴

**Apache Spark와 함께:**
```scala
// Spark 잡
val df = spark.read.parquet(path)
val typed = df.as[CustomerProducer]  // Spark encoder
// 변환 전 컴파일 타임 검사
summon[Conforms[CustomerProducer, CustomerContract, Policy.Exact]]
val output = typed.transform(dropSegment).as[CustomerContract]
```
**스키마 레지스트리와 함께:**
```scala
// Avro 스키마 레지스트리 -> case class -> 계약 검사
case class CustomerAvro(id: Long, email: String, amt: Double)
summon[Conforms[CustomerAvro, CustomerContract, Policy.Exact]]
// Avro 스키마가 어긋나면 빌드 실패!
```
**CI/CD 통합:** `sbt compile`만 실행하면 된다. 계약이 어긋나면 빌드가 실패한다. 특별한 도구가 필요 없다. Jenkins, GitHub Actions, GitLab CI 등 sbt를 실행할 수 있는 환경이면 어디서든 동작한다.

### 온보딩

**새 팀원이 묻는다**: "왜 컴파일이 안 되나요?"

**답변**: "에러 메시지를 확인해보세요. `CustomerProducer`를 쓰려고 하는데 계약은 `CustomerContract`를 기대합니다. transform을 수정하거나 계약을 업데이트하세요."

그게 전부다. 컴파일러가 안내해준다. 한두 번 경험하면 바로 이해한다.

### 사용할 때 vs 건너뛸 때

**컴파일 타임 계약을 사용해야 할 때**:

  * 여러 팀이 같은 파이프라인 코드베이스에서 작업할 때
  * 스키마 변경이 정기적으로 프로덕션을 깨뜨릴 때
  * 빠른 피드백이 필요할 때 (컴파일 vs 배포 후 테스트)

**건너뛰어도 될 때**:

  * 일회성 스크립트나 탐색적 작업
  * 제어할 수 없는 외부 API (런타임 검증을 사용)
  * 스키마가 진정으로 동적인 경우 (키를 알 수 없는 JSON)

* * *

## Part 11 - 중첩 타입: Map, List, 그리고 깊은 구조

GitHub 코드에서 중첩 타입을 처리하는 방법을 보여준다. `CtdcPoc.scala`의 예제를 보자:
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
  items: Seq[LineItem],        // List vs Seq - 둘 다 시퀀스
  shipTo: Option[Address],      // 중첩된 case class
  tags: Seq[String] = Nil       // Set vs Seq, 기본값 있음
)
val evDeepOk: SchemaConforms[OrderOut, OrderContract, SchemaPolicy.Backward.type] = summon
```
**여기서 일어나는 일:**

  1. **중첩된 case class** - `OrderOut` 안의 `Address`. 매크로가 재귀한다: `Option[Address]`를 만나면 언래핑하여 `Address`를 꺼내고, `Address`가 case class인지 확인한 다음 다시 재귀하여 필드를 가져온다.

  2. **컬렉션** - `List[LineItem]` vs `Seq[LineItem]`. `seqArg`가 이들을 동등하게 취급하므로 둘 다 일치한다. 매크로는 "LineItem의 시퀀스"로 보고 `LineItem`으로 재귀한다.

  3. **Map** - `attrs`의 `Map[String, String]`. 매크로가 키 타입(원자 타입이어야 함)을 확인한 후 값 타입으로 재귀한다.

  4. **기본값** - `tags: Seq[String] = Nil`은 기본값이 있다. Backward 정책에서 `OrderOut`에 `tags`가 없으면, 계약에 기본값이 있으므로 허용된다.

TypeInspector 패턴이 중요한 이유가 바로 이것이다 - 특별한 케이스 없이 임의의 중첩을 처리할 수 있다.

* * *

## Part 12 - Policy modifier 2분 정리 (Ordered, CI, By-Position)

때로는 더 엄격한 비교가 필요하다. 혹은 다른 규칙이 필요하다. 세 가지 정책 확장을 빠르게 살펴보자:

**PolicyMods.scala**
```scala
package com.example
sealed trait PolicyMod
object PolicyMod {
  sealed trait ExactOrdered   extends PolicyMod // 이름 + 순서 + 타입 모두 일치해야 함
  sealed trait ExactCI        extends PolicyMod // 대소문자 구분 없는 필드 이름
  sealed trait ExactByPosition extends PolicyMod // 이름 무시; 인덱스 기준 타입 비교
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
    val out      = fields[Out]
    val contract = fields[Contract]
    val mismatches: List[String] = Type.of[M] match {
      case '[PolicyMod.ExactCI] =>
        val n = (s: String) => s.toLowerCase
        val om = out.map{ case (n0,t) => n(n0) -> t }.toMap
        val cm = contract.map{ case (n0,t) => n(n0) -> t }.toMap
        (cm.keySet union om.keySet).toList.flatMap { k =>
          (cm.get(k), om.get(k)) match {
            case (Some(t1), Some(t2)) if t1 != t2 => List(s"$k expected $t1, found $t2")
            case (Some(_), None) => List(s"missing ${k}")
            case (None, Some(_)) => List(s"extra ${k}")
            case _ => Nil
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
사용해보자:
```scala
import com.example.*
final case class A(id: Long, name: String)
final case class B(name: String, id: Long)
ConformsMod[A, B, PolicyMod.ExactByPosition] // 성공, 인덱스 기준으로 타입이 동일
// ConformsMod[A, B, PolicyMod.ExactOrdered] // 실패, 순서가 중요 -> 이름 불일치
final case class CIa(ID: Long)
final case class CIb(id: Long)
ConformsMod[CIa, CIb, PolicyMod.ExactCI] // 성공, 대소문자 무시하면 이름이 일치
```
왜 이 세 가지인가? 실제 시스템에서 필요하기 때문이다:

  * **ExactOrdered**: 위치가 중요한 포맷용 (일부 CSV 파서, 필드 번호가 있는 protobuf)
  * **ExactCI**: 누군가는 반드시 계약이 `userId`라고 해놓은 곳에 `UserId`를 쓰기 때문
  * **ExactByPosition**: 필드 이름을 완전히 무시하는 레거시 시스템용

* * *
## Part 13 - Phantom types와 type-state 빌더: 잘못 사용할 수 없는 API 만들기

여기서부터 흥미로워진다. 이 코드는 phantom types, type-indexing / typestate builder 패턴을 활용해 **잘못 사용하는 것 자체가 불가능한** 파이프라인 빌더를 만든다. Phantom types란 무엇인가? 클래스 본문에는 등장하지 않지만, 인스턴스로 할 수 있는 작업을 제어하는 타입 파라미터다. 컴파일 타임 상태 머신이라고 생각하면 된다.

코드베이스 `SparkCore.scala`에서 가져온 코드:
```scala
// Phantom type states
sealed trait BuilderState
sealed trait Empty         extends BuilderState
sealed trait WithSource    extends BuilderState
sealed trait WithTransform extends BuilderState
sealed trait Complete      extends BuilderState
final case class PipelineBuilder[S <: BuilderState, CurContract] private (
  name: String,
  steps: List[PipelineStep]
) {
  // Can only add source when Empty
  def addSource[C](src: TypedSource[C])(using SparkSchema[C]): PipelineBuilder[WithSource, C] =
    PipelineBuilder[WithSource, C](name, steps :+ ...)
  // Can only transform after adding source
  def transformAs[Next](f: DataFrame => DataFrame)(using
    ev: S <:< WithSource,  // This constraint enforces order!
    sch: SparkSchema[Next]
  ): PipelineBuilder[WithTransform, Next] =
    PipelineBuilder[WithTransform, Next](name, steps :+ ...)
  // Can only add sink after transform
  def addSink[R, P <: SchemaPolicy](sink: TypedSink[R])(using
    ev0: S <:< WithTransform,  // Must have transform
    ev1: SchemaConforms[CurContract, R, P],  // Compile-time contract check!
    sch: SparkSchema[R]
  ): PipelineBuilder[Complete, CurContract] =
    PipelineBuilder[Complete, CurContract](name, steps :+ ...)
  // Can only build when Complete
  def build(using ev: S =:= Complete): SparkSession => DataFrame =
    (spark: SparkSession) => ...
}
object PipelineBuilder {
  def apply[CurContract](name: String): PipelineBuilder[Empty, CurContract] = {
    PipelineBuilder[Empty, CurContract](name, Nil)
  }
}
```
### 동작 원리:

  1. **타입으로 표현된 상태 전이** - 각 메서드가 새로운 타입 파라미터 `S`를 반환한다. 컴파일러가 현재 어떤 상태인지 추적한다.

  2. **증거 제약** - `ev: S <:< WithSource`는 "S가 WithSource의 하위 타입이어야 한다"는 뜻이다. 아니라면 이 메서드 자체가 존재하지 않는다.

  3. **컴파일 타임 상태 머신** - 메서드를 잘못된 순서로 호출하는 것 자체가 불가능하다:

```
// YES, This compiles
PipelineBuilder[Contract]("good")
  .addSource(src)
  .transformAs[Next](transform)
  .addSink[Contract, SchemaPolicy.Exact](sink)
  .build
// NO, This doesn't compile - can't transform before adding source
PipelineBuilder[Contract]("bad")
  .transformAs[Next](transform)  // ERROR: No implicit evidence S <:< WithSource
```
  4. **계약 검사가 내장되어 있다** - `addSink`의 `ev1: SchemaConforms[CurContract, R, P]`를 보라. 바로 여기서 컴파일 타임 계약 검사가 일어난다. 스키마가 어긋나면 파이프라인 자체가 빌드되지 않는다.

이 패턴을 **Phantom Type Builder Pattern**이라고 부른다. 더 자세한 내용은 다음을 참고:

  * [Compile-safe builder pattern using phantom types (Xebia)](https://xebia.com/blog/compile-safe-builder-pattern-using-phantom-types-in-scala/)
  * [Phantom Types in Scala (Rhetorical Musings)](https://blog.rhetoricalmusings.com/posts/builder1/)

**왜 중요한가**: 운영 데이터 파이프라인에서는 연산 순서가 중요하다. Read -> Transform -> Validate -> Write. Phantom types를 쓰면 컴파일러가 이 순서를 강제한다. 변환 없이 쓰기를 실행할 수 없고, 소스 없이 변환을 수행할 수 없다.

"매크로가 어떻게 동작하는가"와 "운영 수준의 계약 시스템을 어떻게 만드는가"의 차이가 바로 여기에 있다.

* * *

## Part 14 - 스키마 진화와 버전 관리: 시간에 따른 변화 다루기

현실은 이렇다: 스키마는 진화한다. 필드가 추가되고, 폐기되고, 이름이 바뀐다. 팀들은 변경 사항을 점진적으로 배포한다. 스키마가 변할지 여부가 문제가 아니라, 운영 환경을 깨뜨리지 않으면서 어떻게 변화를 관리하느냐가 문제다.

### 스키마 진화 문제

**시나리오**: `CustomerV1`이 운영 환경에서 돌아가고 있다. 마케팅팀이 타겟팅용 `segment` 필드를 추가하고 싶어 한다. 그런데 `CustomerV1`을 읽는 파이프라인이 10개나 있다. 모든 것을 깨뜨리지 않으면서 어떻게 진화시킬 수 있을까?

**잘못된 접근**: `CustomerV1`에 `segment`를 추가하고, 배포하고, 잘 되기를 바란다. 결과: 새 필드를 처리하지 못하는 파이프라인 일부가 깨진다.

**올바른 접근**: 계약에 버전을 매기고, 정책을 사용해 전환을 관리한다.

### 버전 관리 전략

**CustomerV1.scala**
```scala
package contracts
final case class CustomerV1(id: Long, email: String)
```
**CustomerV2.scala**
```scala
package contracts
final case class CustomerV2(id: Long, email: String, segment: Option[String] = None)
```
`segment`가 다음과 같은 점에 주목하라:

  1. **선택적**(`Option[String]`)
  2. **기본값이 있음**(`= None`)

이렇게 하면 하위 호환성이 확보된다 - 기존 코드가 새 데이터와 함께 작동할 수 있다.

### 마이그레이션 단계

**1단계: 필드 추가 (Backward 정책)**

`CustomerV2`를 쓰는 새 프로듀서를 배포한다:
```scala
val producer = PipelineBuilder[CustomerV2]("write-v2")
  .addSource(rawData)
  .transformAs[CustomerV2](addSegment)
  .addSink[CustomerV2, Policy.Backward](sink)  // Backward allows optional fields
  .build
```
기존 컨슈머는 여전히 `CustomerV1`을 읽는다. `segment` 필드를 무시한다. 깨지는 것 없음.

**2단계: 컨슈머 마이그레이션**

각 컨슈머를 하나씩 업데이트한다:
```scala
val consumer = PipelineBuilder[CustomerV2]("read-v2")
  .addSource[CustomerV2](source)
  .transformAs[EnrichedCustomer](useSegment)  // Now uses segment field
  .addSink[EnrichedCustomer, Policy.Exact](sink)
  .build
```
컴파일 타임 계약이 보장한다: "이 컨슈머가 새 스키마를 제대로 처리하는가?" case class 업데이트를 빠뜨리면 컴파일되지 않는다.

**3단계: V1 폐기**

모든 파이프라인이 `CustomerV2`를 사용하게 되면:
```scala
// Mark V1 as deprecated
@deprecated("Use CustomerV2", "2025-10-01")
final case class CustomerV1(id: Long, email: String)
```
컴파일러가 남아 있는 V1 사용에 경고를 준다. 유예 기간이 지나면 V1을 완전히 삭제한다.

### 호환성을 깨는 변경 처리

필드 이름을 바꿔야 한다면? 예를 들어 `email` -> `emailAddress`라면?

**호환성을 유지하는 접근**:
```scala
// Step 1: Add the new field, keep the old
final case class CustomerV3(
  id: Long,
  email: String,              // Old field
  emailAddress: String,       // New field
  segment: Option[String] = None
)
```
잠깐, 중복이다. 더 나은 방법:
```scala
// Step 1: Add alias constructor
final case class CustomerV3(
  id: Long,
  emailAddress: String,       // New canonical name
  segment: Option[String] = None
)
object CustomerV3 {
  def fromV2(v2: CustomerV2): CustomerV3 = {
    CustomerV3(v2.id, v2.email, v2.segment)
  }
}
```
**2단계**: 프로듀서가 `emailAddress`를 쓰도록 마이그레이션 **3단계**: 컨슈머가 `emailAddress`를 읽도록 마이그레이션 **4단계**: `fromV2` 생성자 제거

각 단계마다 컴파일 타임 계약이 검증한다: "이 변환이 기대하는 스키마를 생성하는가?"

### 마이그레이션 중 공존

마이그레이션 중에는 V2와 V3가 동시에 돌아간다. 어떻게 처리할까?
```scala
sealed trait CustomerSchema
case class V2(id: Long, email: String, segment: Option[String] = None) extends CustomerSchema
case class V3(id: Long, emailAddress: String, segment: Option[String] = None) extends CustomerSchema
// Transformation handles both
def normalize(schema: CustomerSchema): V3 = schema match {
  case V2(id, email, segment) => V3(id, email, segment)
  case v3: V3 => v3
}
```
계약이 양쪽 경로를 모두 검사한다:
```
summon[Conforms[V2, V3Contract, Policy.Backward]]  // V2 → V3 allowed
summon[Conforms[V3, V3Contract, Policy.Exact]]     // V3 → V3 exact match
```
### 실제 마이그레이션: Backward에서 Exact로

GitHub 코드에서 가져온 전체 마이그레이션 시나리오를 살펴보자. 운영 환경에서 스키마 변경을 배포하는 정확한 방법이다.

`CtdcPoc.scala`에서:
```scala
/** Sink contract: the target schema we promise to write. */
final case class CustomerContract(id: Long, email: String, age: Option[Int] = None)
/** Producer: imagine an upstream source adds an extra field `segment`. */
final case class CustomerProducer(id: Long, email: String, age: Option[Int], segment: String)
/** Declared "Next" schema after a transform (dropping `segment`). */
final case class CustomerNext(id: Long, email: String, age: Option[Int])
// Step 1: Migration phase - use Backward policy
// Producer has extra field, but contract allows it during migration
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
### 여기서 일어나는 일:

**1단계: 프로듀서가 필드를 추가**

  * 업스트림이 `CustomerProducer`에 `segment` 필드를 추가
  * 우리 계약은 여전히 `CustomerContract` (`segment` 없음)
  * `transformAs[CustomerNext]`로 추가 필드를 명시적으로 제거
  * 그런 다음 `CustomerNext`가 **Exact** 정책 하에서 `CustomerContract`에 적합한지 검사

**왜 이것이 작동하는가:**

  1. 변환이 출력 스키마(`CustomerNext`)를 명시적으로 선언한다
  2. 컴파일러가 검사한다: `CustomerNext`가 `CustomerContract`와 일치하는가? 그렇다 (둘 다 id, email, age를 가진다)
  3. 나중에 실수로 `CustomerNext`를 변경하면 컴파일이 실패한다

**2단계: 안정화** 마이그레이션이 끝나면:
```scala
// Later: Everyone uses CustomerContract, no transform needed
val planStable =
  PipelineBuilder[CustomerContract]("stable")
    .addSource(contractSource)
    .noTransform  // Direct pass-through
    .addSink[CustomerContract, SchemaPolicy.Exact.type](sink)
    .build
```
실무에서 쓰는 방식 그대로다. 이 코드가 스키마를 안전하게 마이그레이션하는 정확한 방법을 보여준다.

* * *

## Part 15 - 컴파일 타임 테스트, 제대로 하기 (복사해서 바로 쓰는 테스트)

코드가 컴파일에 실패하는지 어떻게 테스트할까? `assertDoesNotCompile`을 쓰면 된다:

**CompileTimeSpec.scala**
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
  test("By‑Position should accept re‑ordered names with same types") {
    assertCompiles("ConformsMod[A, B, PolicyMod.ExactByPosition]")
  }
  test("Ordered should reject the same re‑ordering") {
    assertDoesNotCompile("ConformsMod[A, B, PolicyMod.ExactOrdered]")
  }
  test("Pipeline builder enforces correct order") {
    assertDoesNotCompile("""
      PipelineBuilder[Contract]("bad")
        .transformAs[Next](identity)  // Can't transform without source
    """)
  }
  test("Nested types conform correctly") {
    assertCompiles("summon[Conforms[OrderOut, OrderContract, Policy.Backward]]")
  }
}
```
CI에 연결할 수 있다:
```
# .github/workflows/compile-fail.yml (concept)
name: compile-fail
on: [pull_request]
jobs:
  cf:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '17' }
      - name: Compile‑fail suite
        run: sbt -v "testOnly *CompileTimeSpec"
```
이건 정말 강력하다. CI가 깨진 스키마는 배포될 수 없다는 것을 직접 증명해 준다.

* * *

## Part 16 - Scala 2: 같은 것을 구현하는 방법 (그리고 차이점)

Scala 2에서는 `Context`(blackbox/whitebox)를 사용하는 def 매크로를 쓴다. 아이디어는 동일하지만, 구현은 `Expr`/quotes 대신 `c.universe` 트리를 사용한다.

**ConformsS2.scala**
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
    val out      = fieldsOf[Out](sOut)
    val contract = fieldsOf[Contract](sCon)
    val outMap      = out.map{ case (n,t,_,opt) => n -> (t,opt)}.toMap
    val contractMap = contract.map{ case (n,t,_,opt) => n -> (t,opt)}.toMap
    val missing = contract.collect { case (n,t,_,opt) if !outMap.contains(n) => s"$n:$t${if (opt) " (optional)" else ""}" }
    val extra   = out.collect      { case (n,_,_,_) if !contractMap.contains(n) => n }
    val mism    = contract.collect {
      case (n,t,_,_) if outMap.get(n).exists(_._1 != t)  => s"$n expected $t, found ${outMap(n)._1}"
      case (n,_,_,o) if outMap.get(n).exists(_._2 != o)  => s"$n optionality mismatch"
    }
    if (missing.nonEmpty || extra.nonEmpty || mism.nonEmpty)
      c.abort(c.enclosingPosition, s"Drift: missing=${missing.mkString(",")} extra=${extra.mkString(",")} mism=${mism.mkString(";")}")
    else
      c.Expr[ConformsS2[Out, Contract, P]](q"new _root_.com.example.ConformsS2[${weakTypeOf[Out]}, ${weakTypeOf[Contract]}, ${weakTypeOf[P]}]")
  }
}
```
주요 차이점:

  * Scala 3: `inline` + `${ ... }` splices; Scala 2: `macro def`와 `Context`
  * 오류 보고: `report.errorAndAbort` (Scala 3) vs `c.abort` (Scala 2)
  * 트리: `Expr[T]` (Scala 3) vs 원시 `Tree` (Scala 2)
  * 타입 검사: `TypeRepr` (Scala 3) vs `Type` (Scala 2)

Scala 3 매크로가 더 안전하다. quoted API가 흔한 매크로 버그를 상당수 방지해 준다. 다만 Scala 2 매크로도 주의해서 쓰면 잘 작동한다.

제네릭 파생을 위해서는 [Magnolia](https://github.com/softwaremill/magnolia)를 확인해 보라 - 두 버전 모두에서 작동한다.

* * *

## Part 17 - Spark 통합: 런타임 심층 방어

GitHub의 코드는 컴파일 타임 정책을 Spark의 내장 구조적 비교기로 미러링하여 런타임 검증을 수행하는 방법도 보여준다.

`SparkCore.scala`에서:
```scala
// Derive Spark StructType from case class at compile time
trait SparkSchema[C] {
  def struct: StructType
}
object SparkSchema {
  inline given derived[C]: SparkSchema[C] = ${ sparkSchemaImpl[C] }
  // Macro that converts case class → StructType
  private def sparkSchemaImpl[C: Type](using Quotes): Expr[SparkSchema[C]] = ...
}
// Runtime policy mapping to Spark comparators
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

  1. **컴파일 타임** (매크로) - 배포 전에 스키마 드리프트를 잡는다
  2. **런타임** (Spark 비교기) - 외부 데이터 소스에 대한 방어적 검사

Spark의 구조적 비교기에 대한 자세한 내용은 [DataType API 문서](https://spark.apache.org/docs/3.5.1/api/scala/org/apache/spark/sql/types/DataType%24.html)를 참고하라.

* * *

## Part 18 - 당장 내일부터 적용할 수 있는 마이그레이션 플레이북

실무에서 이 정책들을 사용하는 방식은 다음과 같다:

  * **핵심 경로 -> Exact**. 예상치 못한 상황이 없어야 한다. 스키마가 정확히 일치하지 않으면 빌드가 실패한다.
  * **프로듀서가 선택적 필드를 추가할 때 -> 배포 중에는 Backward**. 프로듀서 쪽에서 새로운 선택적 필드를 허용한다.
  * **추가 필드를 무시할 수 있는 컨슈머 -> 배포 중에는 Forward**. 컨슈머가 추가 필드를 허용하게 한다.
  * **안정화 이후 -> Exact로 다시 강화**.

그리고 다음 사항을 기억하라:

  * 변환은 순수하게 유지하라 (map/filter 안에서 사이드 이펙트 금지)
  * I/O는 경계에 배치하라 (한 번 읽고, 한 번 쓰기)
  * 사이드 이펙트는 멱등으로 만들어라 (재시도 시 중복 쓰기가 발생하지 않도록)

> 컴파일 타임은 드리프트를 방지하고, 런타임은 현실을 관리한다. 테스트는 구조가 아닌 동작을 검증한다.

* * *

## Part 19 - 운영 패턴: 이것을 실제 배포하면서 배운 것들

### 패턴 1: 계약을 버전별로 구성

**CustomerV1.scala**
```scala
package contracts
final case class CustomerV1(id: Long, email: String)
```
**CustomerV2.scala**
```scala
package contracts
final case class CustomerV2(id: Long, email: String, name: Option[String] = None)
```
**CustomerMigration.scala**
```scala
import contracts._
val migration = PipelineBuilder[CustomerV2]("v1-to-v2")
  .addSource[CustomerV1](sourceV1)
  .transformAs[CustomerV2](addNameField)
  .addSink[CustomerV2, SchemaPolicy.Exact](sinkV2)
  .build
```
### 패턴 2: 컴패니언 객체로 스키마 캐싱
```scala
case class User(id: Long, email: String)
object User {
  given SparkSchema[User] = summon[SparkSchema[User]]  // Compute once, reuse
}
```
### 패턴 3: 정책 선택 이유를 인라인으로 문서화
```
// Good: Explicit reasoning
.addSink[Contract, SchemaPolicy.Backward](sink)  // Allow optional fields during Q2 migration
// Bad: No context
.addSink[Contract, SchemaPolicy.Full](sink)  // Why Full? When will we tighten?
```
### 패턴 4: 컴파일 실패 케이스 테스트
```
// Keep these in version control as documentation
test("CustomerV1 should not conform to CustomerV2 under Exact") {
  assertDoesNotCompile("summon[Conforms[CustomerV1, CustomerV2, Policy.Exact]]")
}
```
* * *

## 부록 - 전체 데모 프로젝트 구조 (복사해서 바로 실행)
```
.
```
**├── build.sbt**
```scala
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
```
ThisBuild / scalaVersion := "3.3.3"
ThisBuild / organization := "com.example"
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-sql" % "3.5.1" % "provided",
  "org.scalatest" %% "scalatest" % "3.2.17" % Test
)
```
실행 방법:

**터미널**
```bash
sbt "runMain ctdc.CtdcPoc"
```
Scala 2 설정

Scala 2의 경우, scala-reflect를 포함하는 별도 모듈을 추가하고 위의 S2 파일을 복사한다.

* * *

## 참고 자료

### Scala 3 매크로 & 메타프로그래밍

  * [Scala 3 매크로 - 공식 개요](https://docs.scala-lang.org/scala3/guides/macros/macros.html)
  * [Scala 3 리플렉션 가이드](https://docs.scala-lang.org/scala3/guides/macros/reflection.html)
  * [Scala 3 매크로 모범 사례](https://docs.scala-lang.org/scala3/guides/macros/best-practices.html)
  * [Rock the JVM - Scala Macros and Metaprogramming (강의)](https://rockthejvm.com/courses/scala-macros-and-metaprogramming)
  * [Rock the JVM - Scala 3 매크로 종합 가이드](https://rockthejvm.com/articles/scala-3-macros-comprehensive-guide)

### Apache Spark 통합

  * [Spark DataType 구조적 비교기](https://spark.apache.org/docs/3.5.1/api/scala/org/apache/spark/sql/types/DataType%24.html)
  * [Spark Dataset implicits (toDF/toDS)](https://spark.apache.org/docs/3.5.1/api/scala/org/apache/spark/sql/DatasetHolder.html)
  * [Spark CSV 데이터 소스](https://spark.apache.org/docs/3.5.1/sql-data-sources-csv.html)
  * [Spark용 Scala 3 encoders (커뮤니티)](https://index.scala-lang.org/vincenzobaz/spark-scala3-encoders)
  * [Rock the JVM - Apache Spark Essentials](https://rockthejvm.com/courses/apache-spark-essentials-with-scala)

### 타입 레벨 프로그래밍

  * [Rock the JVM - Type level](https://rockthejvm.com/courses/typelevel-rite-of-passage)
  * [Rock the JVM - Advanced Scala](https://rockthejvm.com/courses/advanced-scala)
  * [Rock the JVM - Cats](https://rockthejvm.com/courses/cats)

### Phantom types & type-state / 타입 레벨 빌더

  * [Compile-safe builder pattern using phantom types (Xebia)](https://xebia.com/blog/compile-safe-builder-pattern-using-phantom-types-in-scala/)
  * [Phantom Types in Scala - Builder Pattern (Rhetorical Musings)](https://blog.rhetoricalmusings.com/posts/builder1/)

### 기타 자료

  * [Magnolia - 제네릭 파생 라이브러리](https://github.com/softwaremill/magnolia)

* * *

## 컴파일 타임 계약이 잡지 못하는 것

한계를 솔직히 짚어 보자. 컴파일 타임 계약이 많은 것을 처리하지만, 만능은 아니다. 다음은 **할 수 없는** 것들이다:

범주| 잡지 못하는 것| 이유| 해결책  
---|---|---|---  
**외부 데이터 드리프트**|  - 사전 고지 없이 변경되는 서드파티 API  
- 조율하지 않는 팀의 Kafka 토픽  
- 외부 벤더가 쓰는 S3 버킷| 그들의 빌드 프로세스를 제어할 수 없음| 런타임 검증 사용 (schema registry, data contract)  
**늦게 도착하는 스키마 변경**|  - 배포 후에 업데이트된 schema registry  
- 작업 실행 중에 추가된 데이터베이스 컬럼| 컴파일 타임은 배포 전에 일어남| 버전 모니터링과 런타임 검사  
**데이터 품질 문제**|  - 값이 있어야 하는 곳의 null (필드가 non-optional이어도)  
- 범위 밖의 숫자 (age = -5)  
- 잘못된 형식의 문자열 (@가 없는 이메일)| 컴파일 타임은 구조를 검사하지, 내용을 검사하지 않음| Great Expectations, Deequ, 또는 커스텀 검증기  
**호환성을 깨지 않는 추가**|  - 업스트림이 아직 사용하지 않는 선택적 필드를 추가| 보통은 괜찮다! `Forward` 정책이 처리함| 정책 기반 인지 또는 더 엄격한 모니터링  
**부분 배치 실패**|  - 1000개 레코드는 스키마와 일치하지만 5개는 불일치| 컴파일 타임은 이분법적 (컴파일되거나 안 되거나)| 오류 테이블/격리 영역을 활용한 런타임 검증  
  
**핵심 정리**: 컴파일 타임 계약은 개발과 배포 과정에서 **통제 가능한 코드베이스 내의** 드리프트를 잡는다. 그 외의 것 - 외부 소스, 런타임 변경, 데이터 품질 - 에는 런타임 검증을 겹겹이 쌓아야 한다. 심층 방어로 생각하라: 컴파일 타임이 첫 번째 관문이고, 런타임이 안전망이다.

* * *

## 결론

  * 컴파일 타임 증거 + 정책 타입은 스키마 의도를 명시적이고 강제 가능하게 만든다
  * TypeInspector 패턴은 타입 검사를 조합 가능한 유틸리티로 구성한다
  * Type-indexing/type-state를 활용한 phantom types 빌더는 컴파일 타임 상태 머신으로 잘못 사용할 수 없는 API를 만든다
  * 중첩 타입에서는 필드 vs 요소 수준의 선택성이 중요하다
  * 중첩 구조(Map, List, case class)는 재귀적 shape 빌딩으로 처리된다
  * 실제 마이그레이션: 배포 중에는 Backward를 사용하고, 안정화 후 Exact로 강화한다
  * Spark의 비교기는 런타임 심층 방어를 제공한다
  * 스키마 진화에는 버전 관리, 공존 전략, 정책 기반 전환이 필요하다
  * 개발자 경험이 중요하다: 명확한 오류 메시지, 빠른 컴파일 시간, 쉬운 온보딩
  * 컴파일 타임은 런타임 검증을 대체하지 않는다 - 통제 가능한 시스템에서 이를 보완한다
  * 많은 종류의 드리프트를 일찍 잡아내지만, 외부 데이터에 대해서는 여전히 런타임 검사가 필요하다
  * 컴파일 타임 계약은 파이프라인을 깨지지 않게 만드는 것이 아니라, 깨짐을 예측 가능하게 만든다

[글 저장소 바로 가기](https://github.com/vim89/compile-time-data-contracts)

> 원본: https://rockthejvm.com/articles/scala-3-mastering-path-dependent-types-and-type-projections

# Scala 3: 경로 의존 타입과 타입 프로젝션 완벽 정리

## 대상 독자

Scala 타입 시스템의 기능이 궁금한 Scala 개발자를 위한 짧은 글이다.

## 소개

지금부터 설명할 내용은 자주 쓰이지는 않지만, 필요한 순간에는 상당히 강력한 도구가 된다.

추상 타입 프로젝션이 왜 건전하지 않아서(unsound) Scala 3에서 제거되었는지 알고 싶다면 [이 글](/articles/scala-3-general-type-projections)을 참고하라.

## 타입 중첩

클래스, 오브젝트, 트레이트 안에 다른 클래스, 오브젝트, 트레이트를 정의할 수 있고, 타입 멤버(추상 타입이든 타입 별칭 형태의 구체 타입이든)도 선언할 수 있다는 사실은 이미 알고 있을 것이다.

```scala
class Outer {
    class Inner
    object InnerObj
    type InnerType
}
```

`Outer` 클래스 **내부**에서 이 타입들을 사용하는 것은 간단하다. 중첩 클래스, 오브젝트, 타입 별칭을 이름 그대로 쓰면 된다.

`Outer` 클래스 **외부**에서 사용하는 것은 조금 까다롭다. 예를 들어 `Inner` 클래스를 인스턴스화하려면 `Outer`의 인스턴스가 필요하다.

```scala
val outer = new Outer
val inner = new outer.Inner
```

`inner`의 타입은 `outer.Inner`이다. 다시 말해, `Outer`의 각 인스턴스마다 고유한 중첩 타입이 존재한다. 따라서 다음 코드는 타입 불일치 오류가 발생한다.

```scala
val outerA = new Outer
val outerB = new Outer
val inner: outerA.Inner = new outerB.Inner
```

중첩 싱글턴 오브젝트에도 같은 원리가 적용된다. 예를 들어 다음 표현식은

```scala
outerA.InnerObj == outerB.InnerObj
```

`false`를 반환한다. 마찬가지로 추상 타입 멤버 `InnerType`도 `Outer` 인스턴스마다 서로 다른 타입이다.

## 경로 의존 타입과 타입 프로젝션

`Outer` 클래스에 다음과 같은 메서드가 있다고 가정하자.

```scala
class Outer {
  // 타입 정의들
  def process(inner: Inner): Unit = ??? // 인자를 출력하는 등의 더미 구현
}
```

이 메서드에는 해당 `Outer` 인스턴스가 생성한 `Inner` 인스턴스만 전달할 수 있다.

```scala
val outerA = new Outer
val innerA = new outerA.Inner
val outerB = new Outer
val innerB = new outerB.Inner

outerA.process(innerA) // OK
outerA.process(innerB) // 오류: 타입 불일치
```

`innerA`와 `innerB`의 타입이 서로 다르니 예상대로다. 그런데 모든 `Inner` 타입의 상위 타입이 존재한다. 바로 `Outer#Inner`이다.

```scala
class Outer {
  // 타입 정의들
  def processGeneral(inner: Outer#Inner): Unit = ??? // 인자를 출력하는 등의 더미 구현
}
```

`Outer#Inner` 타입을 **타입 프로젝션**이라 부른다. 이렇게 정의하면 어떤 `Outer` 인스턴스가 만든 `Inner`든 전달할 수 있다.

```scala
outerA.processGeneral(innerA) // OK
outerA.processGeneral(innerB) // OK
```

`instance.MyType`이나 `Outer#Inner` 형태의 타입을 **경로 의존 타입(path-dependent types)** 이라 부르는데, 인스턴스나 외부 타입("경로")에 의존하기 때문이다. 이 용어가 다소 혼란스럽다는 피드백이 이 글과 [영상](https://www.youtube.com/watch?v=63syJfNoDPI)에 있었다. 앞으로는 `Outer#Inner` 형태의 타입을 나머지와 구분하기 위해 **타입 프로젝션**이라는 용어를 쓰겠다.

## 경로 의존 타입과 타입 프로젝션이 유용한 이유

경로 의존 타입과 타입 프로젝션이 유용한 사례 몇 가지를 살펴보자.

**예시 1:** 여러 라이브러리가 타입 검사와 타입 추론에 타입 프로젝션을 활용한다. [Akka Streams](https://doc.akka.io/docs/akka/current/stream/index.html)가 대표적인데, 컴포넌트를 서로 연결할 때 적절한 스트림 타입을 자동으로 결정하는 데 경로 의존 타입을 사용한다. 예를 들어 타입 추론기에서 `Flow[Int, Int, NotUsed]#Repr` 같은 타입을 볼 수 있다.

**예시 2:** [타입 람다](/articles/scala-3-type-lambdas)는 Scala 2에서 타입 프로젝션에 전적으로 의존했고, 그 모양은 상당히 끔찍했다(예: `{ type T[A] = List[A] }#T`). 사실상 유일한 방법이었기 때문이다. 다행히 Scala 3에서는 타입 람다를 위한 문법이 제대로 마련되었다.

**예시 3:** 추상 타입, 인스턴스 의존 타입, implicit(또는 Scala 3의 given)을 조합하면 본격적인 [타입 레벨 정렬기](/articles/type-level-programming-in-scala-part-1-numbers-and-comparisons)까지 만들 수 있다.

## 의존 타입을 사용하는 메서드

이 배경지식을 갖추었으니, 인자의 타입에 따라 적절한 중첩 타입을 반환하는 메서드를 살펴보자. 예를 들어 데이터 접근 API용 데이터 구조/레코드 기술(description)이 있다면,

```scala
class AbstractRow {
    type Key
}
```

다음 메서드는 문제없이 컴파일된다.

```scala
def getIdentifier(row: AbstractRow): row.Key = ???
```

제네릭 외에, 인자의 값에 따라 다른 타입을 반환할 수 있게 해주는 유일한 기법이다. 라이브러리 설계에서 매우 강력하게 활용할 수 있다.

## 의존 타입을 사용하는 함수

이 기능은 Scala 3에서 새로 도입되었다. Scala 3 이전에는 `getIdentifier` 같은 메서드를 함수 값으로 변환해서 고차 함수에 활용(인자로 넘기거나 결과로 반환하는 등)할 수 없었다. Scala 3에서 **의존 함수 타입(dependent function types)** 이 도입되면서 가능해졌다. 이제 메서드를 함수 값에 대입할 수 있다.

```scala
val getIdentifierFunc = getIdentifier
```

이 함수 값의 타입은 `(r: AbstractRow) => r.Key`이다.

주제를 마무리하자면, `(r: AbstractRow) => r.Key`라는 새 타입은 다음의 문법 설탕이다.

```scala
Function1[AbstractRow, AbstractRow#Key] {
  def apply(arg: AbstractRow): arg.Key
}
```

이 타입은 `Function1[AbstractRow, AbstractRow#Key]`의 하위 타입이다. `apply` 메서드가 `arg.Key` 타입을 반환하는데, [앞에서 살펴본 것처럼](#경로-의존-타입과-타입-프로젝션) `arg.Key`는 `AbstractRow#Key`의 하위 타입이므로 오버라이드가 성립한다.

## 결론

정리하면 다음과 같다.

- 중첩 타입을 살펴보았고, 외부 클래스의 인스턴스마다 서로 다른 타입이 만들어진다는 점을 확인했다
- 인스턴스 의존 타입의 상위 타입인 타입 프로젝션(`Outer#Inner`)을 알아보았다
- 경로 의존 타입과 타입 프로젝션이 유용한 사례를 살펴보았다
- 의존 메서드와 의존 *함수*를 다루었는데, 후자는 Scala 3에서만 지원된다

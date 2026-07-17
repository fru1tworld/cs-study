> 원본: https://rockthejvm.com/articles/scala-3-inlines

# Scala 3 Inline 해설

## 목차

1. [소개](#소개)
2. [Scala Inline](#scala-inline)
3. [Inline 인자](#inline-인자)
4. [Transparent Inline](#transparent-inline)
5. [성능](#성능)
6. [결론](#결론)

## 소개

Scala 3은 강력한 메타프로그래밍 기능을 제공한다. 컴파일 과정에서 올바른 타입이 붙은 코드를 확장하거나, 합성하거나, 조작할 수 있다. 다른 언어들이 텍스트 전처리기, quasi-quote, 컴파일러 플러그인 등을 제공하는 반면, Scala는 "컴파일 시점에 모든 타입 정보를 유지한 채 본격적인 코드 처리"를 할 수 있다는 점에서 독보적이다.

이 글에서는 메타프로그래밍의 기본 형태인 inline을 다룬다. 이어지는 글에서는 Scala의 가장 고급 메타프로그래밍 기능인 매크로를 다룬다.

> **참고:** 이 글은 Scala Macros and Metaprogramming 강좌의 미리보기 성격이다. 해당 강좌에서는 다른 언어로는 제공하기 어려운 라이브러리·도구 개발 역량을 가르친다.

## Scala Inline

inline 메서드는 함수 호출을 생성하는 대신, 호출 지점에 메서드 본문을 직접 삽입한다. 컴파일 결과를 확인하려면 `build.sbt`에 다음 옵션을 추가한다:

```
ThisBuild / scalacOptions ++= Seq(
  "-Xprint:postInlining",
  "-Xmax-inlines:100000"
)
```

예시:

```scala
def increment(x: Int): Int = x + 1
inline def inc(x: Int): Int = x + 1

val aNumber = 3
val four = increment(aNumber)
val four_v2 = inc(aNumber)
```

SBT 출력을 보면, 일반 메서드는 함수 호출이 생성되고 inline 메서드는 본문이 인자를 치환한 형태로 전개된다:

```
val four: Int = 
  com.rockthejvm.inlines.SimpleInlines.increment(
    com.rockthejvm.inlines.SimpleInlines.aNumber)
    
val four_v2: Int = 
  com.rockthejvm.inlines.SimpleInlines.aNumber.+(1):Int
```

inline 메서드에 복잡한 표현식을 넘기면 컴파일러가 중간 결과를 생성한다:

```scala
val eight = inc(2 * aNumber + 1)
```

결과:

```
val eight: Int = {
  val x$proxy1: Int = 2.*(aNumber).+(1)
  x$proxy1.+(1):Int
}
```

## Inline 인자

매개변수에 `inline`을 붙이면 인자가 평가되지 않고 표현식 자체가 메서드 본문에 직접 삽입된다:

```scala
inline def incia(inline x: Int) = x + 1
```

호출:

```scala
val eight_v2 = incia(2 * aNumber + 1)
```

결과:

```
val eight_v2: Int = 2.*(aNumber).+(1).+(1):Int
```

inline 인자에 대한 모든 참조가 실제 표현식으로 대체된다. 이는 by-name 매개변수와 비슷해 보이지만 컴파일 시점에 결정적으로 동작한다는 차이가 있다. inline 인자는 inline 메서드에서만 사용할 수 있으며, 어떤 매개변수에 inline을 붙일지는 자유롭게 선택할 수 있다.

## Transparent Inline

`transparent` 수식자를 사용하면 더 정밀한 타입 추론이 가능해진다. 일반 메서드는 선언된 반환 타입을 더 구체적으로 좁힐 수 없다:

```scala
def wrap(x: Int): Option[Int] = Some(x)

val myWrapper: Some[Int] = wrap(2) // 오류: 타입 불일치
```

transparent inline 메서드를 사용하면 컴파일러가 실제 반환 타입을 추론할 수 있다:

```scala
transparent inline def wrap(x: Int): Option[Int] = Some(x)

val myWrapper: Option[Int] = wrap(2) // 가능
val myWrapper: Some[Int] = wrap(2)   // 이것도 가능
```

이를 통해 일반 타입에서는 접근할 수 없는 구체 타입의 멤버와 메서드를 사용할 수 있다.

**트레이드오프:** `transparent inline`은 유용하지만, higher-kind, 공변/반공변, f-bound, 재귀 타입, 타입 람다 등이 얽힌 복잡한 타입에서는 컴파일 시간이 길어질 수 있다. transparent inline이 중첩되거나 체이닝되면 컴파일 속도가 눈에 띄게 느려진다.

## 성능

인라이닝은 메서드 호출 오버헤드를 제거한다. 함수 인자를 받는 메서드가 자주 호출되는 상황에서 특히 효과적이다.

예시로 함수 매개변수를 받는 범용 루프 함수를 살펴보자:

```scala
def loop[A](start: A, condition: A => Boolean, advance: A => A)(action: A => Any) = {
    var a = start
    while (condition(a)) {
        action(a)
        a = advance(a)
    }
}
```

**inline 없이 (10억 회 반복):**

```scala
def testNoInline() = {
  def loop[A](start: A, condition: A => Boolean, advance: A => A)(action: A => Any) = {
    var a = start
    while (condition(a)) {
      action(a)
      a = advance(a)
    }
  }
  val start = System.currentTimeMillis()
  val r = Random().nextInt(10000)
  val u = Random().nextInt(10000)
  val arr = Array.ofDim[Int](10000)
  loop(0, _ < 10000, _ + 1) { i =>
    loop(0, _ < 100000, _ + 1) { j =>
      arr(i) = arr(i) + u
    }
    arr(i) = arr(i) + r
  }
  println(s"Basic version: ${(System.currentTimeMillis() - start) / 1000.0} s")
}
```

**결과: 1.197초 (MacBook M3 기준)**

**inline 매개변수 적용:**

```scala
inline def loop[A](inline start: A, inline condition: A => Boolean, inline advance: A => A)(inline action: A => Any) = {
  var a = start
  while (condition(a)) {
    action(a)
    a = advance(a)
  }
}
```

컴파일러가 이를 중첩 while 루프로 전개한다:

```
{
  var a: Int = 0
  (while {
    val _$1: Int = a
    _$1.<(10000)
  } do {
    {
      val i: Int = a
      {
        {
          var a: Int = 0
          (while {
            val _$3: Int = a
            _$3.<(100000)
          } do {
            {
              val j: Int = a
              arr.update(i, arr.apply(i).+(u))
            }
            a = {
              val _$4: Int = a
              _$4.+(1)
            }
          })
        }:Unit
        arr.update(i, arr.apply(i).+(r))
      }
    }
    a = {
      val _$2: Int = a
      _$2.+(1)
    }
  })
}:Unit
```

**결과: 0.115초**

**성능 개선: 약 10배 빨라졌다.**

## 결론

Scala의 inline은 다음을 제공한다:

- 메서드 호출을 구현 본문으로 대체
- `inline` 매개변수를 통한 인자 표현식 직접 전개
- `transparent` 수식자를 통한 정밀한 타입 추론 (컴파일 시간과의 트레이드오프 있음)
- 메서드 호출 오버헤드 제거로 인한 성능 향상

전체 코드는 [GitHub 저장소](https://github.com/rockthejvm/scala-3-inlines-demo)에서 확인할 수 있다.

inline은 컴파일 시점 if문과 패턴 매칭도 지원하며, `compiletime` 패키지를 통해 컴파일 시점 에러 보고와 타입 소거 우회도 가능하다. 더 깊이 알고 싶다면 Scala 메타프로그래밍 강좌를 참고하라.

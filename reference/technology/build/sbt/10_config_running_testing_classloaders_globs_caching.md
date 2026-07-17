# sbt: 프로젝트 코드 실행, 테스트, 인프로세스 클래스로더, Glob, 원격 캐시

> 원본: https://www.scala-sbt.org/1.x/docs/Running-Project-Code.html, https://www.scala-sbt.org/1.x/docs/Testing.html, https://www.scala-sbt.org/1.x/docs/In-Process-Classloaders.html, https://www.scala-sbt.org/1.x/docs/Globs.html, https://www.scala-sbt.org/1.x/docs/Remote-Caching.html

## 목차

1. [run/runMain으로 프로젝트 코드 실행](#1-runrunmain으로-프로젝트-코드-실행)
2. [같은 JVM에서 실행할 때 생기는 문제](#2-같은-jvm에서-실행할-때-생기는-문제)
3. [테스트 기본 구성](#3-테스트-기본-구성)
4. [test / testOnly / testQuick](#4-test--testonly--testquick)
5. [테스트 출력과 리포트](#5-테스트-출력과-리포트)
6. [테스트 옵션 (testOptions)](#6-테스트-옵션-testoptions)
7. [테스트 포킹 (fork, testGrouping)](#7-테스트-포킹-fork-testgrouping)
8. [추가 테스트 구성 (IntegrationTest, 커스텀 설정)](#8-추가-테스트-구성-integrationtest-커스텀-설정)
9. [JUnit 지원](#9-junit-지원)
10. [테스트 프레임워크/리포터 확장](#10-테스트-프레임워크리포터-확장)
11. [인프로세스 클래스로더 개념](#11-인프로세스-클래스로더-개념)
12. [classLoaderLayeringStrategy 세 가지 전략](#12-classloaderlayeringstrategy-세-가지-전략)
13. [클래스로더 계층화로 인한 리플렉션 문제](#13-클래스로더-계층화로-인한-리플렉션-문제)
14. [Glob 개념과 생성 방법](#14-glob-개념과-생성-방법)
15. [Glob 필터, 깊이, 정규식](#15-glob-필터-깊이-정규식)
16. [FileTreeView로 파일시스템 조회](#16-filetreeview로-파일시스템-조회)
17. [Glob vs PathFinder](#17-glob-vs-pathfinder)
18. [원격 캐시(Remote Caching) 개념과 사용법](#18-원격-캐시remote-caching-개념과-사용법)
19. [rootPaths와 remoteCacheId](#19-rootpaths와-remotecacheid)

---

## 1. run/runMain으로 프로젝트 코드 실행

`run`과 `console` 태스크는 sbt와 동일한 JVM 안에서 사용자 코드를 실행한다. 이 방식은 별도 JVM을 기동하는 오버헤드가 없어 빠르지만, sbt 프로세스 자체와 자원을 공유한다는 제약이 따른다.

`run`에는 `runMain`이라는 변형이 있는데, 실행할 메인 클래스의 완전한 클래스 이름(fully qualified name)을 첫 번째 인자로 추가로 받는다. 즉 프로젝트 안에 메인 클래스가 여러 개 있을 때 `runMain`으로 특정 클래스를 지정해서 실행할 수 있다.

```
> run
> runMain org.example.MyMainClass arg1 arg2
```

`run`과 `runMain`은 같은 설정을 공유하며 서로 별도로 구성할 수 없다.

## 2. 같은 JVM에서 실행할 때 생기는 문제

sbt와 같은 JVM에서 사용자 코드를 돌릴 때 세 가지 문제가 발생할 수 있다.

| 문제 | 내용 |
|------|------|
| `System.exit` | 사용자 코드가 `System.exit`을 호출하면 JVM 전체가 종료되어 sbt 빌드도 함께 죽는다. sbt를 재시작해야 한다. |
| 스레드(Threads) | 사용자 코드가 새 스레드를 띄울 수 있고, main 메서드가 반환된 후에도 스레드가 살아 있을 수 있다. 특히 GUI를 생성하면 non-daemon 스레드가 여러 개 만들어져 JVM이 종료될 때까지 남아 있는 경우가 있다. `System.exit`이 호출되거나 모든 non-daemon 스레드가 끝나야 프로그램이 완료된 것으로 본다. |
| 역직렬화와 클래스로딩 | 역직렬화 과정에서 여러 복합적인 이유로 잘못된 클래스로더가 쓰일 수 있다. 이는 sbt에만 국한된 문제가 아니라 JVM 클래스로더 위임 모델 자체의 특성에서 비롯된다. |

이런 제약 때문에 GUI 애플리케이션이나 `System.exit`을 호출하는 코드, 또는 클래스로더에 민감한 코드는 포크(fork)된 별도 JVM에서 실행하는 편이 안전하다.

---

## 3. 테스트 기본 구성

테스트 소스의 표준 위치는 다음과 같다.

| 언어/종류 | 경로 |
|-----------|------|
| Scala 소스 | `src/test/scala/` |
| Java 소스 | `src/test/java/` |
| 리소스 | `src/test/resources/` |

리소스는 테스트 코드에서 `java.lang.Class`나 `java.lang.ClassLoader`의 `getResource` 메서드로 접근한다.

주요 Scala 테스트 프레임워크(ScalaCheck, ScalaTest, specs2)는 공통 테스트 인터페이스를 구현하고 있어 클래스패스에 추가하기만 하면 sbt와 바로 연동된다.

```scala
lazy val scalacheck = "org.scalacheck" %% "scalacheck" % "1.17.0"
libraryDependencies += scalacheck % Test
```

`Test`는 구성(configuration)을 뜻하며, ScalaCheck이 테스트 클래스패스에만 들어가고 메인 소스에는 필요 없다는 것을 의미한다. 라이브러리를 만들 때는 이렇게 테스트 의존성을 분리해두는 편이 좋다. 사용자가 라이브러리를 쓸 때 테스트 의존성까지 끌고 올 필요가 없기 때문이다.

## 4. test / testOnly / testQuick

| 태스크 | 동작 |
|--------|------|
| `test` | 커맨드라인 인자 없이 모든 테스트를 실행 |
| `testOnly` | 공백으로 구분된 테스트 이름 목록을 받아 지정한 테스트만 실행. 와일드카드 지원 |
| `testQuick` | `testOnly`와 같은 필터 문법을 쓰되, 추가로 이전 실행에서 실패했거나, 아직 실행된 적이 없거나, 전이적 의존성이 재컴파일된 테스트만 골라 실행 |

```
> test
> testOnly org.example.MyTest1 org.example.MyTest2
> testOnly org.example.*Slow org.example.MyTest1
```

`testOnly`/`testQuick`의 탭 완성은 가장 최근 `Test / compile` 결과를 기준으로 제공된다. 즉 새로 작성한 소스는 컴파일하기 전까지 탭 완성 목록에 나타나지 않고, 삭제한 소스도 재컴파일하기 전까지는 목록에 남아 있다. 다만 컴파일이 안 된 새 테스트도 `testOnly`에 직접 이름을 입력하면 실행할 수 있다.

메인 소스에 적용되는 태스크는 대부분 테스트 소스에도 `Test /` 접두어를 붙여 그대로 쓸 수 있다.

```
Test / compile
Test / console
Test / consoleQuick
Test / run
Test / runMain
```

## 5. 테스트 출력과 리포트

기본적으로 각 테스트 소스 파일의 로그는 해당 파일의 모든 테스트가 끝날 때까지 버퍼링된다. `logBuffered`를 끄면 실시간으로 출력된다.

```scala
Test / logBuffered := false
```

sbt는 기본적으로 프로젝트별 `target/test-reports` 디렉터리에 JUnit XML 형식의 테스트 리포트를 생성한다. 이 기능은 `JUnitXmlReportPlugin`을 비활성화해서 끌 수 있다.

```scala
val myProject = (project in file(".")).disablePlugins(plugins.JUnitXmlReportPlugin)
```

## 6. 테스트 옵션 (testOptions)

### 테스트 프레임워크 인자

`testOnly` 실행 시 `--` 뒤에 프레임워크 전용 인자를 붙일 수 있다.

```
> testOnly org.example.MyTest -- -verbosity 1
```

빌드 설정에 고정으로 넣으려면 `Tests.Argument`를 쓴다.

```scala
Test / testOptions += Tests.Argument("-verbosity", "1")
```

특정 프레임워크에만 인자를 전달하려면 프레임워크를 명시한다.

```scala
Test / testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-verbosity", "1")
```

### 설정(Setup)과 정리(Cleanup)

`Tests.Setup`, `Tests.Cleanup`은 `() => Unit` 또는 `ClassLoader => Unit` 타입의 함수를 받는다. `ClassLoader`를 받는 변형은 테스트 실행에 사용된(혹은 사용됐던) 클래스로더를 전달받아 테스트 클래스와 테스트 프레임워크 클래스에 접근할 수 있게 해준다.

포킹(forking)을 사용하는 경우 테스트 클래스가 다른 JVM에 있으므로 `ClassLoader`를 넘겨줄 수 없다. 이때는 `() => Unit` 변형만 써야 한다.

```scala
Test / testOptions += Tests.Setup( () => println("Setup") )
Test / testOptions += Tests.Cleanup( () => println("Cleanup") )
Test / testOptions += Tests.Setup( loader => ... )
Test / testOptions += Tests.Cleanup( loader => ... )
```

### 병렬 실행 끄기

sbt는 기본적으로 모든 태스크를 sbt 자신과 같은 JVM에서 병렬 실행한다. 각 테스트가 태스크로 매핑되므로 테스트도 기본적으로 병렬 실행된다. 한 프로젝트 안의 테스트를 순차 실행하려면 다음과 같이 설정한다.

```scala
Test / parallelExecution := false
```

`Test` 대신 `IntegrationTest`를 지정하면 통합 테스트만 순차 실행하게 할 수 있다. 단, 서로 다른 프로젝트에 속한 테스트는 여전히 동시에 실행될 수 있다.

### 클래스 필터링

이름이 "Test"로 끝나는 클래스만 실행하려면 `Tests.Filter`를 쓴다.

```scala
Test / testOptions := Seq(Tests.Filter(s => s.endsWith("Test")))
```

## 7. 테스트 포킹 (fork, testGrouping)

```scala
Test / fork := true
```

이 설정은 모든 테스트를 하나의 외부 JVM에서 실행하도록 지정한다. 포크된 JVM의 표준 옵션 구성은 Forking 문서를 참고한다. 포킹된 테스트는 기본적으로 순차 실행된다.

테스트를 어떤 JVM에 어떻게 배분할지, 어떤 옵션을 넘길지 더 세밀하게 제어하려면 `testGrouping` 키를 쓴다.

```scala
import Tests._

{
  def groupByFirst(tests: Seq[TestDefinition]) =
    tests groupBy (_.name(0)) map {
      case (letter, tests) =>
        val options = ForkOptions()
          .withRunJVMOptions(Vector("-Dfirst.letter"+letter))
        new Group(letter.toString, tests, SubProcess(options))
    } toSeq

    Test / testGrouping := groupByFirst( (Test / definedTests).value )
}
```

같은 그룹에 속한 테스트는 순차 실행된다. 동시에 실행할 수 있는 포크된 JVM 개수는 `Tags.ForkedTestGroup` 태그의 제한값(기본 1)으로 제어한다. 그룹이 포크될 경우 `Setup`/`Cleanup`에 실제 테스트 클래스로더를 넘길 수 없다.

포크된 JVM 안에서도 테스트를 병렬 실행하고 싶다면 다음 설정을 추가한다.

```scala
Test / testForkedParallel := true
```

## 8. 추가 테스트 구성 (IntegrationTest, 커스텀 구성)

프로젝트에 별도의 테스트 소스 집합과 그에 딸린 컴파일/패키징/테스트 태스크를 추가하고 싶을 때는 새 구성(configuration)을 정의한다. 절차는 다음과 같다.

1. 구성 정의
2. 태스크/설정값 추가
3. 라이브러리 의존성 선언
4. 소스 작성
5. 태스크 실행

### 통합 테스트(Integration Tests)

```scala
lazy val scalatest = "org.scalatest" %% "scalatest" % "3.2.17"

ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.12.18"
ThisBuild / version      := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    libraryDependencies += scalatest % "it,test"
    // other settings here
  )
```

- `configs(IntegrationTest)`: 미리 정의된 통합 테스트 설정을 추가한다. 이 설정은 `it`이라는 이름으로 참조된다.
- `settings(Defaults.itSettings)`: `IntegrationTest` 설정에 컴파일/패키징/테스트 관련 태스크와 설정값을 추가한다.
- `libraryDependencies += scalatest % "it,test"`: ScalaTest를 일반 `test` 설정과 `it` 설정 양쪽에 추가한다. 통합 테스트에만 넣고 싶으면 `"it"`만 지정한다.

표준 소스 계층은 다음과 같다.

- `src/it/scala`: Scala 소스
- `src/it/java`: Java 소스
- `src/it/resources`: 통합 테스트 클래스패스에 들어갈 리소스

표준 테스트 태스크는 `IntegrationTest/` 접두어를 붙여서 실행한다.

```
> IntegrationTest/test
> IntegrationTest/testOnly org.example.AnIntegrationTest
```

`IntegrationTest` 설정에서 별도로 값을 지정하지 않으면 대부분 `Test` 설정값에 위임된다. 예를 들어 `Test / testOptions += ...`로 지정한 옵션은 `Test`뿐 아니라 `IntegrationTest`에도 그대로 적용된다. `IntegrationTest`에만 옵션을 추가하려면 `IntegrationTest / testOptions += ...`처럼 해당 설정에 직접 넣거나, `:=`로 덮어써서 통합 테스트 전용 값으로 확정할 수 있다.

### 커스텀 테스트 설정

내장 설정 대신 새 설정을 직접 정의할 수도 있다.

```scala
lazy val scalatest = "org.scalatest" %% "scalatest" % "3.2.17"
lazy val FunTest = config("fun") extend(Test)

ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.12.18"
ThisBuild / version      := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .configs(FunTest)
  .settings(
    inConfig(FunTest)(Defaults.testSettings),
    libraryDependencies += scalatest % FunTest
    // other settings here
  )
```

`config("fun") extend(Test)`는 `FunTest`에 정의되지 않은 설정값을 `Test`에 위임하겠다는 뜻이다. `inConfig(FunTest)(Defaults.testSettings)`가 `FunTest` 설정에 테스트 관련 태스크/설정값을 추가하는 부분이다. 사실 `Defaults.itSettings`도 `inConfig(IntegrationTest)(Defaults.testSettings)`를 축약한 정의일 뿐이다.

`FunTest` 전용 옵션은 `FunTest / testOptions += ...`로 지정하고, 실행은 다음과 같이 한다.

```
> FunTest / test
```

### 소스를 공유하는 추가 테스트 구성

별도 컴파일 없이 소스는 공유하되, 설정별로 실행할 테스트만 다르게 필터링하는 방식도 가능하다.

```scala
lazy val scalatest = "org.scalatest" %% "scalatest" % "3.2.17"
lazy val FunTest = config("fun") extend(Test)

ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.12.18"
ThisBuild / version      := "0.1.0-SNAPSHOT"

def itFilter(name: String): Boolean = name endsWith "ITest"
def unitFilter(name: String): Boolean =
  (name endsWith "Test") && !itFilter(name)

lazy val root = (project in file("."))
  .configs(FunTest)
  .settings(
    inConfig(FunTest)(Defaults.testTasks),
    libraryDependencies += scalatest % FunTest,
    Test / testOptions := Seq(Tests.Filter(unitFilter)),
    FunTest / testOptions := Seq(Tests.Filter(itFilter))
    // other settings here
  )
```

핵심 차이는 두 가지다.

- `Defaults.testTasks`만 추가하고 컴파일/패키징 태스크는 추가하지 않는다(소스가 이미 공유되므로).
- 설정별로 `Tests.Filter`를 다르게 지정해 실행할 테스트를 나눈다.

```
> test               # 일반 유닛 테스트
> FunTest / test      # FunTest로 필터링된 테스트
> FunTest / testOnly org.example.AFunTest
```

이 소스 공유 방식은 병렬 실행 가능한 테스트와 순차 실행이 필요한 테스트를 나누는 데도 응용할 수 있다. `serial`이라는 설정을 만들고 그 설정에서만 병렬 실행을 끄면, `test`는 병렬로, `Serial / test`는 순차로 실행된다.

```scala
lazy val Serial = config("serial") extend(Test)

Serial / parallelExecution := false
```

## 9. JUnit 지원

JUnit5(Jupiter) 지원은 `sbt-jupiter-interface`가 제공한다.

```scala
// build.sbt
libraryDependencies += "net.aichler" % "jupiter-interface" % "0.9.0" % Test
```

```scala
// project/plugins.sbt
addSbtPlugin("net.aichler" % "sbt-jupiter-interface" % "0.9.0")
```

JUnit4 지원은 `junit-interface`가 제공한다.

```scala
libraryDependencies += "com.github.sbt" % "junit-interface" % "0.13.3" % Test
```

## 10. 테스트 프레임워크/리포터 확장

주요 Scala 테스트 라이브러리는 sbt 지원이 내장돼 있다. 다른 프레임워크를 붙이려면 공통 테스트 인터페이스(uniform test interface)를 구현하면 된다. 프레임워크 저자라면 이 인터페이스를 `provided` 의존성으로 가져다 쓸 수 있고, 제3자도 별도 프로젝트로 구현해 sbt 플러그인 형태로 배포할 수 있다.

테스트 프레임워크는 결과와 상태를 테스트 리포터에 보고한다. `TestReportListener`나 `TestsListener`를 구현해 새 리포터를 만들 수 있다.

확장 기능을 프로젝트에 적용하려면 두 설정값을 수정한다.

```scala
testFrameworks += new TestFramework("custom.framework.ClassName")
testListeners += customTestListener  // sbt.TestReportListener 타입
```

---

## 11. 인프로세스 클래스로더 개념

sbt는 기본적으로 `run`과 `test` 태스크를 자기 자신의 JVM 인스턴스 안에서 실행한다. 격리된 `ClassLoader`로 태스크를 호출하는 방식으로 외부 java 명령 실행을 흉내 낸다. 포킹과 비교하면 시작 지연 시간과 전체 실행 시간이 줄어드는데, 단순히 JVM을 재사용하는 것만으로 얻는 성능 이득은 크지 않다. 대부분 애플리케이션의 시작 시간을 좌우하는 건 의존성 클래스의 로딩과 링킹이다.

sbt는 실행 간에 이미 로드된 클래스 일부를 재사용해 이 시작 지연을 줄인다. 방식은 표준 자바 `ClassLoader` 위임 모델을 따르는 계층화된(layered) `ClassLoader`를 구성하는 것이다.

- 최외곽 계층(outermost layer): 항상 프로젝트 고유의 클래스 파일과 jar를 담으며, 매 실행마다 버려진다.
- 내부 계층(inner layers): 재사용이 가능하다.

## 12. classLoaderLayeringStrategy 세 가지 전략

sbt 1.3.0부터 `classLoaderLayeringStrategy` 설정으로 계층화 `ClassLoader`를 생성하는 방식을 고를 수 있다.

| 전략 | 설명 |
|------|------|
| `ScalaLibrary`(기본값) | 최외곽 계층의 부모가 Scala 표준 라이브러리와(애플리케이션 클래스패스에 있다면) Scala reflect 라이브러리를 로드할 수 있다. sbt 1.3.0 이전 버전의 계층화 방식과 가장 비슷하다. |
| `AllLibraryJars` | Scala 라이브러리 계층과 최외곽 계층 사이에 모든 의존성 jar를 위한 계층을 추가한다. turbo 모드에서는 이 전략이 기본값이며 `ScalaLibrary`보다 시작/전체 실행 성능을 크게 높일 수 있다. 단, 라이브러리가 가변 전역 상태를 갖고 있으면 실행 간에 그 상태가 유지돼 결과가 일관되지 않을 수 있다. 라이브러리가 자바 직렬화를 쓴다면 이 전략은 피해야 한다. |
| `Flat` | 계층화를 아예 쓰지 않는다. `fullClasspath` 키가 지정하는 전체 classpath가 최외곽 계층 하나에 로드된다. `ScalaLibrary`에서 문제가 생기거나, 애플리케이션이 모든 클래스를 같은 `ClassLoader`에 로드해야 하는 경우(자바 직렬화의 일부 사용 사례 등) 포크 대신 고려할 만하다. |

`classLoaderLayeringStrategy`는 구성(configuration)별로 다르게 지정할 수 있다. 예를 들어 `Test` 구성에서만 `AllLibraryJars`를 쓰려면 `build.sbt`에 다음을 추가한다.

```scala
Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.AllLibraryJars
```

이렇게 해도 다른 설정을 바꾸지 않았다면 `run` 태스크는 여전히 `ScalaLibrary` 전략을 쓴다.

## 13. 클래스로더 계층화로 인한 리플렉션 문제

계층화된 클래스로더를 쓰면 자바 리플렉션이 문제를 일으킬 수 있다. 리플렉션으로 다른 클래스를 로드하는 메서드가 속한 클래스로더가 대상 클래스에 접근하지 못하는 경우가 생기기 때문이다. 특히 `Class.forName`이 아니라 `Thread.currentThread.getContextClassLoader.loadClass`를 쓸 때 자주 발생한다.

```scala
package example

import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object ReflectionExample {
  def main(args: Array[String]): Unit = Await.result(Future {
      val cl = Thread.currentThread.getContextClassLoader
      println(cl.loadClass("example.Foo"))
  }, Duration.Inf)
}
class Foo
```

`sbt run`으로 이 코드를 기본 `ScalaLibrary` 전략에서 실행하면 `ClassNotFoundException`이 발생한다. `Future`를 뒷받침하는 스레드의 컨텍스트 클래스로더가 Scala 라이브러리 클래스로더인데, 이 클래스로더는 프로젝트 클래스를 로드할 수 없기 때문이다.

계층화 전략을 `Flat`으로 바꾸지 않고도 이 문제를 우회하는 방법이 두 가지 있다.

1. `ClassLoader.loadClass` 대신 `Class.forName`을 쓴다. JVM은 `Class.forName` 호출 시 호출한 클래스의 클래스로더를 암묵적으로 사용한다. 위 예제에서 `ReflectionExample`과 `Foo`는 같은 프로젝트 클래스패스에 속해 같은 클래스로더에서 로드되므로 문제가 없다.
2. 클래스로더를 명시적으로 넘겨준다. `val cl = Thread.currentThread.getContextClassLoader`를 `val cl = getClass.getClassLoader`로 바꾸면 된다.

두 번째 방법을 라이브러리 안에서 적용하려면, 이름 조회를 수행하는 라이브러리 메서드에 `ClassLoader` 파라미터를 추가하면 된다.

```scala
// 변경 전
object Library {
  def lookup(name: String): Class[_] =
    Thread.currentThread.getContextClassLoader.loadClass(name)
}
```

```scala
// 변경 후
object Library {
  def lookup(name: String): Class[_] =
    lookup(name, Thread.currentThread.getContextClassLoader)
  def lookup(name: String, loader: ClassLoader): Class[_] =
    loader.loadClass(name)
}
```

---

## 14. Glob 개념과 생성 방법

sbt 1.3.0은 파일시스템 쿼리를 셸 글롭(shell glob)과 비슷한 방식으로 지정하는 `Glob` 타입을 도입했다. `Glob`은 공개 메서드가 `matches(java.nio.file.Path)` 하나뿐이며, 어떤 경로가 글롭 패턴에 맞는지 확인하는 데 쓰인다.

가장 단순한 글롭은 경로 하나를 그대로 나타낸다.

```scala
val glob = Glob(Paths.get("foo/bar"))
println(glob.matches(Paths.get("foo"))) // false
println(glob.matches(Paths.get("foo/bar"))) // true
println(glob.matches(Paths.get("foo/bar/baz"))) // false
```

DSL로도 만들 수 있다.

```scala
val glob = Paths.get("foo/bar").toGlob
```

특수 글롭 객체가 두 가지 있다.

- `AnyPath`(별칭 `*`): 이름 구성요소가 하나뿐인 경로에 매치
- `RecursiveGlob`(별칭 `**`): 모든 경로에 매치

```scala
val path = Paths.get("/foo/bar")
val children = Glob(path, AnyPath)
println(children.matches(path)) // false
println(children.matches(path.resolve("baz"))) // true
println(children.matches(path.resolve("baz").resolve("buzz"))) // false
```

DSL로는 다음과 같이 쓴다(둘은 동일하다).

```scala
val children    = Paths.get("/foo/bar").toGlob / AnyPath
val dslChildren = Paths.get("/foo/bar").toGlob / *
```

재귀 글롭도 마찬가지다.

```scala
val allDescendants = Paths.get("/foo/bar").toGlob / **
```

경로 이름 문자열로도 만들 수 있다. 아래 세 식은 동등하다.

```scala
val pathGlob = Paths.get("foo").resolve("bar")
val glob = Glob("foo/bar")
val altGlob = Glob("foo") / "bar"
```

경로 문자열을 파싱할 때 윈도우에서는 `/`가 자동으로 `\`로 변환된다.

## 15. Glob 필터, 깊이, 정규식

글롭은 경로의 각 단계마다 이름 필터를 적용할 수 있다.

```scala
val scalaSources = Paths.get("/foo/bar").toGlob / ** / "src" / "*.scala"
```

이 글롭은 `/foo/bar`의 모든 하위 경로 중 부모 디렉터리가 `src`이고 확장자가 `scala`인 것에 매치한다. 확장자를 여러 개 지정할 수도 있다.

```scala
val scalaAndJavaSources =
  Paths.get("/foo/bar").toGlob / ** / "src" / "*.{scala,java}"
```

`AnyPath`(`*`)를 여러 번 연결해 쿼리의 깊이를 정확히 제한할 수도 있다.

```scala
val twoDeep = Glob("/foo/bar") / * / * / *
```

이 글롭은 `/foo/bar`의 자손 중 부모가 정확히 둘인 경로에만 매치한다. `/foo/bar/a/b/c.txt`는 매치하지만 `/foo/bar/a/b`나 `/foo/bar/a/b/c/d.txt`는 매치하지 않는다.

정규식도 쓸 수 있다.

```scala
val digitGlob = Glob("/foo/bar") / ".*-\d{2,3}[.]txt".r
digitGlob.matches(Paths.get("/foo/bar").resolve("foo-1.txt")) // false
digitGlob.matches(Paths.get("/foo/bar").resolve("foo-23.txt")) // true
digitGlob.matches(Paths.get("/foo/bar").resolve("foo-123.txt")) // true
```

여러 경로 구성요소를 아우르는 정규식도 지정할 수 있다.

```scala
val multiRegex = Glob("/foo/bar") / "baz-\d/.*/foo.txt"
multiRegex.matches(Paths.get("/foo/bar/baz-1/buzz/foo.txt")) // true
multiRegex.matches(Paths.get("/foo/bar/baz-12/buzz/foo.txt")) // false
```

단, `**`는 정규식에서 유효하지 않은 문법이므로 재귀 글롭은 정규식으로 표현할 수 없다. 경로는 구성요소 단위로 매치되므로(`"foo/.*/foo.txt"`는 실제로 `{"foo", ".*", "foo.txt"}` 세 개의 정규식으로 나뉘어 처리된다), 위 `multiRegex`를 재귀적으로 만들려면 다음처럼 써야 한다.

```scala
val multiRegex = Glob("/foo/bar") / "baz-\d/".r / ** / "foo.txt"
multiRegex.matches(Paths.get("/foo/bar/baz-1/buzz/foo.txt")) // true
multiRegex.matches(Paths.get("/foo/bar/baz-1/fizz/buzz/foo.txt")) // true
```

정규식 문법에서 `\`는 이스케이프 문자이므로 경로 구분자로 쓸 수 없다. 여러 경로 구성요소에 걸치는 정규식에서는 윈도우에서도 `/`를 경로 구분자로 써야 한다.

```scala
val multiRegex = Glob("/foo/bar") / "baz-\d/foo\.txt".r
val validRegex = Glob("/foo/bar") / "baz/Foo[.].txt".r
// \F는 유효한 정규식 구성 요소가 아니므로 PatternSyntaxException 발생
val invalidRegex = Glob("/foo/bar") / "baz\Foo[.].txt".r
```

## 16. FileTreeView로 파일시스템 조회

하나 이상의 `Glob` 패턴에 매치하는 파일을 파일시스템에서 조회하는 작업은 `sbt.nio.file.FileTreeView` 트레이트가 담당한다. `list` 메서드는 오버로드가 두 개 있다.

```scala
def list(glob: Glob): Seq[(Path, FileAttributes)]
def list(globs: Seq[Glob]): Seq[(Path, FileAttributes)]
```

```scala
val scalaSources: Glob = ** / "*.scala"
val regularSources: Glob = "/foo/src/main/scala" / scalaSources
val scala212Sources: Glob = "/foo/src/main/scala-2.12"
val sources: Seq[Path] = FileTreeView.default.list(regularSources).map(_._1)
val allSources: Seq[Path] =
  FileTreeView.default.list(Seq(regularSources, scala212Sources)).map(_._1)
```

`Seq[Glob]`을 받는 버전은 같은 디렉터리를 중복해서 순회하지 않도록 내부적으로 글롭들을 병합해서 처리한다.

### 파일 속성(FileAttributes)

`FileTreeView`는 `(Path, FileAttributes)` 쌍을 반환한다. `FileAttributes`는 다음 속성을 제공한다.

- `isDirectory`: 디렉터리인지
- `isRegularFile`: 정규 파일인지(대개 `isDirectory`의 반대)
- `isSymbolicLink`: 심볼릭 링크인지. 기본 `FileTreeView` 구현은 항상 심볼릭 링크를 따라가며, 링크가 깨진 경우 `isSymbolicLink`만 true고 `isDirectory`/`isRegularFile`은 모두 false다.

파일 타입 검사는 시스템 콜을 필요로 해 느릴 수 있는데, 대부분의 데스크톱 OS는 디렉터리를 나열할 때 파일 이름과 노드 타입을 함께 반환하는 API를 제공한다. 그래서 sbt는 추가 시스템 콜 없이 이 정보를 제공할 수 있다.

```scala
// attributes.isRegularFile 호출에는 추가 IO가 발생하지 않음
val scalaSourcePaths =
  FileTreeView.default.list(Glob("/foo/src/main/scala/**/*.scala")).collect {
    case (path, attributes) if attributes.isRegularFile => path
  }
```

### PathFilter로 필터링

`list`에는 `sbt.nio.file.PathFilter`를 받는 오버로드도 있다. `PathFilter`는 추상 메서드 `accept(path: Path, attributes: FileAttributes): Boolean` 하나만 갖는다.

```scala
val regularFileFilter: PathFilter = (_, a) => a.isRegularFile
val scalaSourceFiles =
  FileTreeView.list(Glob("/foo/bar/src/main/scala/**/*.scala"), regularFileFilter)
```

`Glob` 자체도 `PathFilter`로 쓸 수 있고, `!`(부정), `&&`(결합), `||`(합집합) 연산자도 지원한다.

```scala
val hiddenFileFilter: PathFilter = (p, _) => Try(Files.isHidden(p)).getOrElse(false)
val notHiddenFileFilter: PathFilter = !hiddenFileFilter

val regularFileFilter: PathFilter = (_, a) => a.isRegularFile
val andFilter = regularFileFilter && notHiddenFileFilter

val scalaSources: PathFilter = ** / "*.scala"
val javaSources: PathFilter = ** / "*.java"
val jvmSourceFilter = scalaSources || javaSources
```

`String`에서 `PathFilter`로의 암묵적 변환도 제공된다.

```scala
val regularFileFilter: PathFilter = (p, a) => a.isRegularFile
val regularScalaFiles: PathFilter = regularFileFilter && "**/*.scala"
```

자주 쓰는 내장 필터로는 `sbt.io.HiddenFileFilter`, `sbt.io.RegularFileFilter`, `sbt.io.DirectoryFilter`가 있다. `sbt.io.FileFilter`를 `sbt.nio.file.PathFilter`로 변환하려면 `toNio`를 호출한다.

```scala
val excludeFilter: sbt.io.FileFilter = HiddenFileFilter || DirectoryFilter
val excludePathFilter: sbt.nio.file.PathFilter = excludeFilter.toNio
```

`Glob`과 기존 `sbt.io.NameFilter`는 의미가 다르다. `NameFilter`로 `.scala` 확장자를 걸러낼 때는 `"*.scala"`라고 쓰지만, 같은 의도의 `PathFilter`는 `"**/*.scala"`처럼 `**/` 접두어를 붙여야 한다. `"*.scala"`는 이름 구성요소가 하나뿐인 경로에만 매치하는 글롭이기 때문이다.

### 스트리밍

메모리 부담을 줄이려면 `FileTreeView.iterator`를 쓴다.

```scala
FileTreeView.iterator(Glob("/**")).foreach { case (p, _) => println(p) }
```

sbt 안에서 제공되는 구현은 `FileTreeView.native`(64비트 FreeBSD/Linux/macOS/Windows용 JNI 기반 네이티브 구현, 없으면 `java.nio.file` 기반으로 폴백)와 `FileTreeView.nio`(순수 `java.nio.file` 기반) 두 가지가 있다. `FileTreeView.default`는 `FileTreeView.native`를 반환한다.

`fileTreeView` 키로 sbt 태스크 안에서 바로 쓸 수 있다.

```scala
fileTreeView.value.list(baseDirectory.value / ** / "*.txt")
```

## 17. Glob vs PathFinder

sbt에는 원래 파일을 수집하는 DSL인 `PathFinder` API가 있었다. `Glob`과 겹치는 부분이 있지만 `Glob`이 표현력은 더 약한 대신 최적화하기 쉽다는 차이가 있다. `Glob`은 쿼리의 "무엇을"만 기술하고 "어떻게"는 기술하지 않는 반면, `PathFinder`는 이 둘을 함께 조합하기 때문에 최적화가 어렵다.

```scala
// Glob: 파일시스템을 한 번만 순회
val paths = fileTreeView.value.list(
    baseDirectory.value / ** / "*.scala",
    baseDirectory.value / ** / "*.java").map(_._1)
```

```scala
// PathFinder: 두 번 순회하므로 대략 두 배 느림
val paths =
    (baseDirectory.value ** "*.scala" +++
     baseDirectory.value ** "*.java").allPaths
```

---

## 18. 원격 캐시(Remote Caching) 개념과 사용법

sbt 1.4.0 / Zinc 1.4.0은 증분 컴파일 과정에서 추적하는 파일 경로를 가상화(virtualize)하고, 변경 감지에 콘텐츠 해시를 사용한다. 이 두 가지 조합으로 재현 가능한 빌드(build as function)를 실현할 수 있다.

이를 바탕으로 실험적인(experimental) 원격 캐싱(캐시된 컴파일) 기능이 제공된다. 아이디어는 개발팀이나 CI 시스템이 빌드 산출물을 서로 공유하는 것이다. 빌드가 재현 가능하다면 한 머신에서 만든 산출물을 다른 머신이 그대로 재사용할 수 있어 빌드가 훨씬 빨라진다.

### 사용법

```scala
ThisBuild / pushRemoteCacheTo := Some(MavenCache("local-cache", file("/tmp/remote-cache")))
```

머신 1에서 `pushRemoteCache`를 호출하면 `*.class` 파일과 Zinc 분석(Analysis) 아티팩트를 지정한 위치에 퍼블리시한다. 이어서 머신 2에서 `pullRemoteCache`를 호출하면 그 캐시를 내려받아 재사용한다.

### Maven 저장소를 통한 원격 캐싱

sbt 1.4.0 기준으로 캐시된 빌드 산출물을 주고받는 데 기존 Maven 퍼블리시/리졸브 메커니즘을 그대로 재사용한다. Bintray 같은 기존 인프라를 활용하기 쉽다는 장점이 있다. 향후에는 `PUT`/`GET`만으로 동작하는 단순한 HTTP 캐시 서버 방식도 고려될 수 있는데, 이 경우 어딘가에 HTTP 서버를 호스팅해야 하지만 프로비저닝 자체는 더 단순해질 수 있다.

## 19. rootPaths와 remoteCacheId

작업 디렉터리나 Coursier 캐시 디렉터리처럼 머신마다 달라지는 경로를 추상화하기 위해, sbt는 `ThisBuild / rootPaths`에 루트 경로들의 맵을 유지한다. 빌드에서 소스나 출력 디렉터리로 특수한 경로를 추가로 쓴다면 이 경로들을 `ThisBuild / rootPaths`에 등록해야 한다.

`ThisBuild / rootPaths`가 필요한 경로를 모두 포함하고 있는지 보장하고 싶다면 `ThisBuild / allowMachinePath`를 `false`로 설정한다.

sbt 1.4.2부터 `remoteCacheId`는 입력 소스들의 콘텐츠 해시를 모아 만든 해시 값을 사용한다.

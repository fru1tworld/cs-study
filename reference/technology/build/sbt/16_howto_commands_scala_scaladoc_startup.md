# sbt How-to 모음: 명령어 실행 / Scala 설정 / Scaladoc / 시작 시 작업

> 원본: https://www.scala-sbt.org/1.x/docs/Howto-Running-Commands.html, https://www.scala-sbt.org/1.x/docs/Howto-Scala.html, https://www.scala-sbt.org/1.x/docs/Howto-Scaladoc.html, https://www.scala-sbt.org/1.x/docs/Howto-Startup.html

---

## 목차

1. [명령어 실행: 배치 모드에서 인자 전달](#1-명령어-실행-배치-모드에서-인자-전달)
2. [명령어 실행: 여러 명령어 연속 실행](#2-명령어-실행-여러-명령어-연속-실행)
3. [명령어 실행: 파일에서 명령어 읽기](#3-명령어-실행-파일에서-명령어-읽기)
4. [명령어 실행: 별칭(alias) 정의](#4-명령어-실행-별칭alias-정의)
5. [명령어 실행: Scala 표현식 즉석 평가](#5-명령어-실행-scala-표현식-즉석-평가)
6. [Scala 설정: 빌드에 쓸 Scala 버전 지정](#6-scala-설정-빌드에-쓸-scala-버전-지정)
7. [Scala 설정: 표준 라이브러리 자동 의존성 끄기](#7-scala-설정-표준-라이브러리-자동-의존성-끄기)
8. [Scala 설정: 일시적으로 다른 버전 전환](#8-scala-설정-일시적으로-다른-버전-전환)
9. [Scala 설정: 로컬 설치본 사용](#9-scala-설정-로컬-설치본-사용)
10. [Scala 설정: 여러 버전으로 크로스 빌드](#10-scala-설정-여러-버전으로-크로스-빌드)
11. [Scala REPL: 의존성만 올린 콘솔](#11-scala-repl-의존성만-올린-콘솔)
12. [Scala REPL: 의존성 + 컴파일 코드 포함 콘솔](#12-scala-repl-의존성--컴파일-코드-포함-콘솔)
13. [Scala REPL: 빌드 정의와 플러그인 포함 콘솔](#13-scala-repl-빌드-정의와-플러그인-포함-콘솔)
14. [Scala REPL: 진입/종료 시 실행 명령 지정](#14-scala-repl-진입종료-시-실행-명령-지정)
15. [Scala REPL: 프로젝트 코드 안에서 REPL 내장](#15-scala-repl-프로젝트-코드-안에서-repl-내장)
16. [Scaladoc: javadoc과 scaladoc 자동 선택](#16-scaladoc-javadoc과-scaladoc-자동-선택)
17. [Scaladoc: 문서 생성 옵션 컴파일과 분리 설정](#17-scaladoc-문서-생성-옵션-컴파일과-분리-설정)
18. [Scaladoc: javadoc 옵션 설정](#18-scaladoc-javadoc-옵션-설정)
19. [Scaladoc: 외부 의존성 문서 자동/수동 링크](#19-scaladoc-외부-의존성-문서-자동수동-링크)
20. [Scaladoc: 라이브러리 자신의 API 문서 위치 선언](#20-scaladoc-라이브러리-자신의-api-문서-위치-선언)
21. [시작 시 작업 실행: onLoad 훅](#21-시작-시-작업-실행-onload-훅)

---

## 1. 명령어 실행: 배치 모드에서 인자 전달

셸을 띄우지 않고 커맨드라인에서 바로 sbt 명령어를 실행할 때, 명령어 자체에 공백이나 인자가 포함되어 있으면 셸이 이를 별개의 인자로 쪼개버린다. 이때는 명령어 전체를 큰따옴표로 감싸서 하나의 토큰으로 넘긴다.

```
$ sbt "project X" clean "~ compile"
```

`project X`(프로젝트 전환), `clean`, `~ compile`(파일 변경 감시 컴파일) 세 개의 명령어가 순서대로 실행된다.

## 2. 명령어 실행: 여러 명령어 연속 실행

sbt 셸 안에서 여러 태스크를 한 줄에 이어서 실행하고 싶을 때는 세미콜론(`;`)으로 구분한다.

```
> ~ ;clean;compile
```

`~`(watch) 뒤에 세미콜론으로 묶인 명령어 그룹을 붙이면, 소스 파일이 바뀔 때마다 `clean`과 `compile`이 순서대로 다시 실행된다.

## 3. 명령어 실행: 파일에서 명령어 읽기

반복해서 입력하는 명령어 시퀀스를 파일에 저장해두고 불러와 실행하는 기능이 sbt 셸에 내장되어 있다. 이 문서 자체에는 별도 코드 예제가 없고, 관련 기능은 sbt 셸의 명령어 입력 소스 확장 방식을 참고한다.

## 4. 명령어 실행: 별칭(alias) 정의

자주 쓰는 명령어나 태스크를 짧은 이름으로 부르고 싶을 때 `alias` 명령어를 사용한다.

```
> alias a=about
> alias
    a = about
> a
[info] This is sbt ...
```

별칭을 없앨 때는 우변을 비워서 다시 `alias`를 실행한다.

```
> alias a=
> alias
> a
[error] Not a valid command: a ...
```

인자 없이 `alias`만 입력하면 현재 등록된 별칭 목록이 출력된다.

## 5. 명령어 실행: Scala 표현식 즉석 평가

빌드 파일을 만들지 않고 간단한 Scala 코드를 바로 컴파일해서 실행 결과를 확인하고 싶을 때는 `eval` 명령어를 쓴다.

```
> eval 2+2
4: Int
```

## 6. Scala 설정: 빌드에 쓸 Scala 버전 지정

프로젝트를 컴파일할 때 사용할 Scala 버전은 `scalaVersion` 키로 지정한다. 지정하지 않으면 sbt 자체가 내장한 Scala 버전이 기본값으로 쓰인다.

```
scalaVersion := "2.11.1"
```

## 7. Scala 설정: 표준 라이브러리 자동 의존성 끄기

sbt는 기본적으로 `scala-library`를 프로젝트 의존성에 자동으로 추가한다. 순수 Java 프로젝트를 sbt로 빌드하거나 표준 라이브러리를 직접 관리하고 싶을 때는 이 자동 추가를 끈다.

```
autoScalaLibrary := false
```

## 8. Scala 설정: 일시적으로 다른 버전 전환

빌드 정의를 고치지 않고 셸에서 즉시 다른 Scala 버전으로 전환해 명령어를 실행하고 싶을 때는 `++` 명령어를 쓴다.

```
> ++ 2.10.4
```

## 9. Scala 설정: 로컬 설치본 사용

퍼블리시되지 않은 로컬 빌드의 Scala 컴파일러/라이브러리로 프로젝트를 빌드해야 할 때는 `scalaHome`으로 로컬 설치 경로를 지정한다.

```
scalaVersion := "2.10.0-local"

scalaHome := Some(file("/path/to/scala/home/"))
```

`scalaVersion`은 아티팩트 이름 등에 쓰이는 식별용 버전 문자열이고, 실제 컴파일러/라이브러리는 `scalaHome`이 가리키는 경로에서 가져온다.

## 10. Scala 설정: 여러 버전으로 크로스 빌드

하나의 소스를 2.12, 2.13, 3.x 등 여러 Scala 버전으로 동시에 빌드해야 하는 경우는 크로스 빌딩(cross-building) 기능을 사용한다. `crossScalaVersions`에 버전 목록을 지정하고 `+compile`, `+publish`처럼 `+` 접두사 명령어로 전체 버전에 대해 태스크를 실행한다. 상세 절차는 크로스 빌딩 전용 문서를 참고한다.

## 11. Scala REPL: 의존성만 올린 콘솔

프로젝트 소스 코드는 컴파일하지 않고 라이브러리 의존성만 클래스패스에 올린 상태로 REPL에 진입하고 싶을 때는 `consoleQuick`을 쓴다.

```
> consoleQuick
```

테스트 의존성까지 포함하려면 `Test` 스코프를 붙인다.

```
> Test/consoleQuick
```

프로젝트 소스가 커서 컴파일이 오래 걸릴 때, 의존 라이브러리 API만 빠르게 확인하고 싶은 상황에 적합하다.

## 12. Scala REPL: 의존성 + 컴파일 코드 포함 콘솔

프로젝트 소스를 컴파일한 뒤 그 결과와 모든 의존성을 함께 클래스패스에 올려 REPL을 시작하려면 `console`을 쓴다.

```
> console
```

테스트 코드까지 포함하려면 마찬가지로 `Test` 스코프를 붙인다.

```
> Test/console
```

## 13. Scala REPL: 빌드 정의와 플러그인 포함 콘솔

프로젝트 소스가 아니라 `build.sbt`/`project/` 아래의 빌드 정의 코드와 여기에 적용된 플러그인들을 클래스패스에 올려 REPL을 여는 명령어는 `consoleProject`다.

```
> consoleProject
```

커스텀 태스크나 플러그인 API를 셸에서 직접 실험할 때 유용하다.

## 14. Scala REPL: 진입/종료 시 실행 명령 지정

REPL이 시작될 때 자동으로 평가할 코드는 `initialCommands`로, 종료될 때 실행할 코드는 `cleanupCommands`로 지정한다. 세 종류의 콘솔(`console`, `consoleQuick`, `consoleProject`) 각각에 독립적으로 스코핑할 수 있다.

```
console / initialCommands := """println("Hello from console")"""

consoleQuick / initialCommands := """println("Hello from consoleQuick")"""

consoleProject / initialCommands := """println("Hello from consoleProject")"""
```

```
console / cleanupCommands := """println("Bye from console")"""

consoleQuick / cleanupCommands := """println("Bye from consoleQuick")"""

consoleProject / cleanupCommands := """println("Bye from consoleProject")"""
```

## 15. Scala REPL: 프로젝트 코드 안에서 REPL 내장

셸이 아니라 애플리케이션 코드 내부에서 Scala REPL을 직접 띄워야 하는 경우(예: 디버깅용 대화형 콘솔 삽입) `scala.tools.nsc.interpreter`의 `Settings`/`Interpreter`를 사용한다. 이때 애플리케이션 클래스로더와 REPL 클래스로더가 어긋나면 `class scala.runtime.VolatileBooleanRef not found`류의 클래스로더 불일치 오류가 발생할 수 있다. 이를 막으려면 `Settings.embeddedDefaults`에 인터프리터 클래스패스에 반드시 포함되어야 할 대표 클래스를 타입 파라미터로 넘겨준다.

```
val settings = new Settings
settings.embeddedDefaults[MyType]
val interpreter = new Interpreter(settings, ...)

def x(a: Int, b: Int) = {
  import scala.tools.nsc.interpreter.ILoop
  ILoop.breakIf[MyType](a != b, "a" -> a, "b" -> b )
}
```

`MyType`은 REPL이 참조해야 할 클래스로더를 결정짓는 기준점 역할을 하는 임의의 클래스이며, 보통 REPL을 호출하는 컨텍스트의 클래스를 넘기면 된다. `ILoop.breakIf`는 조건이 참일 때 그 지점에서 REPL을 열어 중단점처럼 사용할 수 있게 해준다.

## 16. Scaladoc: javadoc과 scaladoc 자동 선택

`doc` 태스크를 실행하면 sbt가 소스 구성을 보고 자동으로 도구를 고른다. Java 소스만 있으면 `javadoc`을, Scala 소스가 하나라도 있으면 `scaladoc`을 실행한다. 별도 설정 없이 프로젝트 언어 구성에 따라 알아서 적절한 문서 생성기가 선택된다.

## 17. Scaladoc: 문서 생성 옵션 컴파일과 분리 설정

컴파일러 옵션과 scaladoc 생성 옵션은 서로 다른 목적을 가지므로, `Compile / doc / scalacOptions`처럼 `doc` 태스크에 스코핑하면 컴파일 시 쓰이는 `Compile / scalacOptions`와 독립적으로 관리할 수 있다.

전체를 새로 지정(덮어쓰기)할 때는 `:=`를 쓴다.

```
Compile / doc / scalacOptions := Seq("-groups", "-implicits")
```

기존 컴파일 옵션에 scaladoc 전용 옵션만 추가하고 싶을 때는 `++=`로 이어 붙인다.

```
Compile / doc / scalacOptions ++= Seq("-groups", "-implicits")
```

`-groups`는 멤버를 그룹으로 묶어 표시하고, `-implicits`는 암시적 변환으로 추가된 멤버까지 문서에 드러내는 옵션이다.

## 18. Scaladoc: javadoc 옵션 설정

Java 소스에 대해 `javadoc`을 실행할 때 넘길 옵션은 `javacOptions`를 `doc` 태스크에 스코핑해서 지정한다. scaladoc과 마찬가지로 `:=`(덮어쓰기)와 `++=`(추가) 둘 다 쓸 수 있다.

```
Compile / doc / javacOptions ++= Seq("-notimestamp", "-linksource")
```

`-notimestamp`는 생성 문서에 타임스탬프를 남기지 않아 재현 가능한 빌드에 유리하고, `-linksource`는 문서에서 원본 소스 코드로 링크를 건다.

## 19. Scaladoc: 외부 의존성 문서 자동/수동 링크

프로젝트가 참조하는 클래스의 API 문서를 자동으로 찾아 링크해주는 기능이다. 관리되는(managed) 의존성, 즉 `libraryDependencies`로 선언된 라이브러리는 `autoAPIMappings`를 켜면 자동으로 처리된다.

```
autoAPIMappings := true
```

이 기능은 의존 라이브러리가 POM에 `apiURL`(또는 이에 준하는 메타데이터)을 게시해뒀을 때만 동작한다. 로컬 jar처럼 관리되지 않는(unmanaged) 의존성은 자동 인식이 불가능하므로 `apiMappings`에 파일 경로와 문서 URL을 직접 매핑해줘야 한다.

```
apiMappings += (
  (unmanagedBase.value / "a-library.jar") ->
    url("https://example.org/api/")
)
```

## 20. Scaladoc: 라이브러리 자신의 API 문서 위치 선언

라이브러리를 배포하는 입장에서, 자신의 문서를 사용하는 다른 프로젝트가 19번 항목의 `autoAPIMappings` 기능으로 자동 링크할 수 있도록 하려면 `apiURL`에 문서가 호스팅되는 위치를 지정해서 함께 퍼블리시한다.

```
apiURL := Some(url("https://example.org/api/"))
```

## 21. 시작 시 작업 실행: onLoad 훅

sbt는 프로젝트 로드가 끝난 직후 자동으로 특정 명령어를 실행하는 훅을 제공한다. 전역 설정 `onLoad`는 `State => State` 타입의 함수이며, 셸에 입력하는 명령어를 상태(state) 앞에 이어 붙이는 방식으로 동작한다. 짝을 이루는 `onUnload`는 `reload`나 `set` 등으로 프로젝트가 언로드될 때 실행된다.

```scala
lazy val dependencyUpdates = taskKey[Unit]("foo")

// This prepends the String you would type into the shell
lazy val startupTransition: State => State = { s: State =>
  "dependencyUpdates" :: s
}

lazy val root = (project in file("."))
  .settings(
    ThisBuild / scalaVersion := "2.12.6",
    ThisBuild / organization := "com.example",
    name := "helloworld",
    dependencyUpdates := { println("hi") },

    // onLoad is scoped to Global because there's only one.
    Global / onLoad := {
      val old = (Global / onLoad).value
      // compose the new transition on top of the existing one
      // in case your plugins are using this hook.
      startupTransition compose old
    }
  )
```

핵심은 세 가지다.

- `onLoad`는 프로젝트 전체에 하나만 존재하므로 반드시 `Global` 스코프에 설정한다.
- 플러그인이 이미 `onLoad`를 사용 중일 수 있으므로, 기존 값을 `val old = (Global / onLoad).value`로 먼저 받아둔다.
- 새 동작을 기존 동작 앞에 쌓기 위해 `startupTransition compose old`처럼 함수 합성을 사용한다. 이렇게 하면 기존 훅이 덮어써지지 않고 함께 실행된다.

같은 방식으로 시작 시 실행할 명령어 문자열만 바꾸면 특정 서브프로젝트로 자동 전환하는 등의 커스텀 시작 동작도 구성할 수 있다.

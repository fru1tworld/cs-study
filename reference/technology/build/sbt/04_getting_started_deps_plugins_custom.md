# sbt 라이브러리 의존성, 플러그인, 커스텀 설정, 빌드 구성

> 원본: https://www.scala-sbt.org/1.x/docs/Library-Dependencies.html, https://www.scala-sbt.org/1.x/docs/Using-Plugins.html, https://www.scala-sbt.org/1.x/docs/Custom-Settings.html, https://www.scala-sbt.org/1.x/docs/Organizing-Build.html, https://www.scala-sbt.org/1.x/docs/Summary.html

---

## 목차

1. [라이브러리 의존성 선언](#1-라이브러리-의존성-선언)
2. [플러그인 사용법](#2-플러그인-사용법)
3. [커스텀 설정과 태스크 키 정의](#3-커스텀-설정과-태스크-키-정의)
4. [멀티 빌드 파일 구성](#4-멀티-빌드-파일-구성)
5. [Getting Started 요약](#5-getting-started-요약)

---

## 1. 라이브러리 의존성 선언

sbt는 의존성을 두 가지 방식으로 관리한다.

| 방식 | 설명 |
|---|---|
| 비관리형(unmanaged) | `lib` 디렉터리에 JAR 파일을 직접 두는 방식 |
| 관리형(managed) | `libraryDependencies` 키로 좌표를 선언하면 sbt가 자동으로 다운로드하는 방식 |

### 비관리형 의존성

`lib` 디렉터리에 있는 JAR은 별도 설정 없이 클래스패스에 포함된다. 디렉터리 위치를 바꾸려면 `unmanagedBase`를 재정의한다.

```scala
unmanagedBase := baseDirectory.value / "custom_lib"
```

`Compile` 구성에서 비관리형 JAR 검색 자체를 끄려면 다음처럼 빈 시퀀스로 재정의한다.

```scala
Compile / unmanagedJars := Seq.empty[sbt.Attributed[java.io.File]]
```

### 관리형 의존성과 libraryDependencies

관리형 의존성은 내부적으로 **Coursier**가 해석하고 다운로드한다. 좌표는 `groupID % artifactID % revision` 형태로 표현한다.

```scala
libraryDependencies += groupID % artifactID % revision
```

구성(configuration)까지 지정할 때는 `%`를 하나 더 붙인다.

```scala
libraryDependencies += groupID % artifactID % revision % configuration
```

여러 의존성을 한꺼번에 추가할 때는 `++=`와 `Seq`를 쓴다.

```scala
libraryDependencies ++= Seq(
  groupID % artifactID % revision,
  groupID % otherID % otherRevision
)
```

Apache Derby를 추가하는 예:

```scala
libraryDependencies += "org.apache.derby" % "derby" % "10.4.1.3"
```

### %% 연산자: Scala 버전 자동 매칭

Scala 라이브러리 아티팩트 이름에는 보통 빌드에 사용된 Scala 버전이 접미사로 붙는다(`scala-stm_2.13`처럼). `%%`를 쓰면 프로젝트의 `scalaVersion`에 맞는 접미사를 sbt가 자동으로 붙여준다. 아래 두 선언은 `scalaVersion`이 `2.13.12`일 때 동일하다.

```scala
libraryDependencies += "org.scala-stm" % "scala-stm_2.13" % "0.9.1"

libraryDependencies += "org.scala-stm" %% "scala-stm" % "0.9.1"
```

### 버전 리비전 지정 방식

고정 버전 문자열 외에 Ivy 스타일의 동적 리비전도 쓸 수 있다.

```
"latest.integration"
"2.9.+"
"[1.0,)"
```

Maven 스타일 버전 범위(`[1.3.0,)` 등)는 기본적으로 하한값으로 대체되어 해석된다. 이 동작은 `-Dsbt.modversionrange=false` 옵션으로 끌 수 있다.

### 저장소(resolvers) 설정

sbt는 기본으로 Maven2 중앙 저장소를 사용한다. 추가 저장소가 필요하면 `resolvers`에 항목을 더한다.

```scala
resolvers += name at location
```

Sonatype 스냅샷 저장소 예:

```scala
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
```

로컬 Maven 저장소(`~/.m2/repository`)를 참조하는 방법은 두 가지다.

```scala
resolvers += Resolver.mavenLocal
```

```scala
resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
```

`resolvers` 키는 어디까지나 기존 저장소 목록에 항목을 **추가**할 뿐이다. 기본 저장소 자체를 교체하려면 `externalResolvers`를 직접 재정의해야 한다.

### 구성별(per-configuration) 의존성

테스트 전용 라이브러리처럼 특정 구성에만 필요한 의존성은 `% "test"` 또는 타입 안전한 `% Test`로 지정한다.

```scala
libraryDependencies += "org.apache.derby" % "derby" % "10.4.1.3" % "test"
```

```scala
libraryDependencies += "org.apache.derby" % "derby" % "10.4.1.3" % Test
```

`Test` 구성으로 선언한 의존성은 `Compile / dependencyClasspath`에는 나타나지 않고 테스트 실행 시에만 클래스패스에 포함된다. ScalaCheck, Specs2, ScalaTest 등 테스트 프레임워크가 보통 이 방식으로 선언된다.

### 확인용 명령

```
update
show Compile/dependencyClasspath
show Test/dependencyClasspath
```

`update` 태스크는 선언된 의존성을 Coursier 캐시로 내려받는다. 두 `show` 명령은 각각 컴파일/테스트 시점의 실제 클래스패스 구성을 보여준다.

---

## 2. 플러그인 사용법

플러그인은 빌드 정의에 새로운 설정, 특히 새로운 태스크를 추가하는 확장 메커니즘이다. 예를 들어 테스트 커버리지 리포트를 생성하는 태스크가 플러그인으로 제공될 수 있다.

### project/plugins.sbt에 플러그인 추가

플러그인은 빌드 루트의 `project/` 디렉터리 아래 `.sbt` 파일에 선언한다. 문법은 다음과 같다.

```scala
addSbtPlugin("조직" % "플러그인이름" % "버전")
```

sbt-site 플러그인 예:

```scala
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "0.7.0")
```

sbt-assembly 플러그인 예:

```scala
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.11.2")
```

일부 플러그인은 기본 저장소에 등록되어 있지 않으므로 `project/plugins.sbt`에 저장소를 추가해야 한다.

```scala
resolvers ++= Resolver.sonatypeOssRepos("public")
```

### Auto Plugin과 활성화

sbt 0.13.5부터 **auto plugin** 기능이 제공되어, 플러그인이 조건에 맞으면 설정을 자동으로 프로젝트에 추가한다. 대부분의 auto plugin은 명시적 활성화 없이도 적용되지만, 일부는 프로젝트에서 명시적으로 활성화해야 동작한다.

```scala
lazy val util = (project in file("util"))
  .enablePlugins(FooPlugin, BarPlugin)
  .settings(
    name := "hello-util"
  )
```

특정 플러그인을 제외하고 싶을 때는 `disablePlugins`를 쓴다.

```scala
lazy val util = (project in file("util"))
  .enablePlugins(FooPlugin, BarPlugin)
  .disablePlugins(plugins.IvyPlugin)
  .settings(
    name := "hello-util"
  )
```

### 활성화된 플러그인 확인

sbt 콘솔에서 `plugins` 명령을 실행하면 현재 프로젝트에 활성화된 플러그인 목록을 볼 수 있다.

```
> plugins
In file:/home/jsuereth/projects/sbt/test-ivy-issues/
        sbt.plugins.IvyPlugin: enabled in scala-sbt-org
        sbt.plugins.JvmPlugin: enabled in scala-sbt-org
        sbt.plugins.CorePlugin: enabled in scala-sbt-org
```

sbt가 기본으로 제공하는 플러그인은 다음과 같다.

| 플러그인 | 역할 |
|---|---|
| CorePlugin | 태스크 병렬 실행 제어 |
| IvyPlugin | 모듈 발행/해결 메커니즘 |
| JvmPlugin | Java/Scala 프로젝트의 컴파일·테스트·실행·패키징 |

### 구 플러그인(non-auto plugin) 설정

auto plugin 이전 방식의 플러그인은 설정을 명시적으로 프로젝트에 섞어 넣어야 한다.

```scala
site.settings
```

멀티 프로젝트 빌드에서 특정 서브프로젝트에만 적용하려면 `.settings(...)` 안에 넣는다.

```scala
lazy val util = (project in file("util"))

lazy val core = (project in file("core"))
  .settings(site.settings)
```

### 전역 플러그인

`$HOME/.sbt/1.0/plugins/` 디렉터리에 플러그인을 설치하면 해당 머신의 모든 sbt 프로젝트에 적용된다. 이 디렉터리의 `build.sbt` 파일에 `addSbtPlugin(...)` 선언을 추가하면 된다. 다만 빌드가 특정 머신 환경에 종속되어 재현성이 떨어지므로 신중하게 사용해야 한다.

---

## 3. 커스텀 설정과 태스크 키 정의

sbt가 제공하는 키 타입은 세 가지다.

| 키 타입 | 특성 |
|---|---|
| `SettingKey[T]` | 프로젝트가 로드될 때 한 번 계산되고 재로드 전까지 고정값 유지 |
| `TaskKey[T]` | 실행할 때마다 새로 계산되는 태스크 |
| `InputKey[T]` | 커맨드라인 인자를 받아 실행하는 태스크 |

### 키 선언

키를 만들 때는 이름과 설명 문자열, 두 인자를 넘긴다.

```scala
val scalaVersion = settingKey[String](
  "The version of Scala used for building."
)
val clean = taskKey[Unit](
  "Deletes files produced by the build, such as generated sources, compiled classes, and task caches."
)
```

### 태스크 구현

`:=` 연산자로 키에 실제 동작을 연결한다.

```scala
val sampleStringTask = taskKey[String]("A sample string task.")
val sampleIntTask = taskKey[Int]("A sample int task.")

ThisBuild / organization := "com.example"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.12.18"

lazy val library = (project in file("library"))
  .settings(
    sampleStringTask := System.getProperty("user.home"),
    sampleIntTask := {
      val sum = 1 + 2
      println("sum: " + sum)
      sum
    }
  )
```

### 태스크 간 의존성

다른 태스크의 결과가 필요하면 `.value`로 참조한다. `.value`는 값을 즉시 평가하는 문법이 아니라 "이 태스크가 저 태스크에 의존한다"는 선언이며, sbt가 이를 바탕으로 실행 순서와 병렬화를 결정한다.

```scala
val startServer = taskKey[Unit]("start server")
val stopServer = taskKey[Unit]("stop server")
val sampleIntTask = taskKey[Int]("A sample int task.")
val sampleStringTask = taskKey[String]("A sample string task.")

lazy val library = (project in file("library"))
  .settings(
    startServer := {
      println("starting...")
      Thread.sleep(500)
    },
    stopServer := {
      println("stopping...")
      Thread.sleep(500)
    },
    sampleIntTask := {
      startServer.value
      val sum = 1 + 2
      println("sum: " + sum)
      sum
    },
    sampleStringTask := {
      startServer.value
      val s = sampleIntTask.value.toString
      println("s: " + s)
      s
    }
  )
```

하나의 태스크 정의 내부에서는 위에서 아래로 순차 실행되지만, 같은 의존 태스크가 여러 경로에서 참조되더라도 실제로는 한 번만 실행된다. 위 예에서 `sampleStringTask`가 `startServer`와 `sampleIntTask`에 의존하고 `sampleIntTask` 역시 `startServer`에 의존하지만, `startServer`는 전체 실행 중 단 한 번만 실행된다.

### 정리(cleanup) 로직 작성 패턴

정리 작업은 반드시 마지막에 실행되어야 하는 태스크의 의존성 사슬 끝에 연결해야 한다.

```scala
lazy val library = (project in file("library"))
  .settings(
    startServer := {
      println("starting...")
      Thread.sleep(500)
    },
    sampleIntTask := {
      startServer.value
      val sum = 1 + 2
      println("sum: " + sum)
      sum
    },
    sampleStringTask := {
      startServer.value
      val s = sampleIntTask.value.toString
      println("s: " + s)
      s
    },
    sampleStringTask := {
      val old = sampleStringTask.value
      println("stopping...")
      Thread.sleep(500)
      old
    }
  )
```

의존성 중복 제거가 부담스러운 상황(예: 서버를 시작하고 반드시 종료해야 하는 로직)에는 순수 Scala의 `try/finally`를 태스크 본문 안에서 직접 쓰는 대안도 있다. 이 경우 sbt의 의존성 그래프가 아니라 일반 메서드 호출이므로 중복 제거 없이 매번 실행된다.

```scala
sampleIntTask := {
  ServerUtil.startServer
  try {
    val sum = 1 + 2
    println("sum: " + sum)
  } finally {
    ServerUtil.stopServer
  }
  sum
}
```

### 재사용을 플러그인으로 승격

동일한 커스텀 태스크/설정 코드가 여러 빌드에서 반복된다면, 해당 코드를 플러그인으로 추출해 재사용하는 편이 낫다. auto plugin의 `autoImport` 객체 아래 정의한 `val`들은 빌드 파일에서 별도 import 없이 자동으로 노출된다.

---

## 4. 멀티 빌드 파일 구성

### 재귀적 메타빌드 구조

sbt의 빌드 정의 자체도 하나의 프로젝트이며, sbt는 이를 **메타빌드(meta-build)** 로 취급한다. 즉 `project/` 디렉터리는 "당신의 빌드를 빌드하는 빌드"다. 이 구조는 이론적으로 무한히 중첩될 수 있지만(메타-메타빌드, ...), 실무에서는 보통 1~2단계 이상 들어가지 않는다.

```
hello/                          # 루트 프로젝트 기본 디렉터리
  Hello.scala                   # 소스 파일
  build.sbt                     # 메타빌드의 정의

  project/                      # 메타빌드 루트 프로젝트
    Dependencies.scala          # 빌드 정의 소스
    assembly.sbt                # 메타-메타빌드 정의

    project/                    # 메타-메타빌드
      MetaDeps.scala           # 최상위 레벨 정의
```

`.sbt` 파일이든 `.scala` 파일이든 이름 자체는 관례일 뿐이며, 같은 디렉터리에 여러 파일을 두고 자유롭게 나눠 관리할 수 있다.

### 의존성 정의를 별도 파일로 분리

의존성 좌표와 버전을 한곳에 모아두면 멀티 프로젝트 빌드에서 일관성을 유지하기 쉽다. `project/Dependencies.scala`에 객체로 정의하는 방식이 흔히 쓰인다.

```scala
import sbt._

object Dependencies {
  // 버전 정의
  lazy val akkaVersion = "2.6.21"

  // 라이브러리 선언
  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaCluster = "com.typesafe.akka" %% "akka-cluster" % akkaVersion
  val specs2core = "org.specs2" %% "specs2-core" % "4.20.0"

  // 프로젝트별 의존성 묶음
  val backendDeps =
    Seq(akkaActor, specs2core % Test)
}
```

루트 `build.sbt`에서는 `import Dependencies._`로 불러와 사용한다.

```scala
import Dependencies._

ThisBuild / organization := "com.example"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.12.18"

lazy val backend = (project in file("backend"))
  .settings(
    name := "backend",
    libraryDependencies ++= backendDeps
  )
```

### .sbt와 .scala 중 어떤 것을 쓸지

- 대부분의 설정은 `build.sbt`에 두는 편이 간단하고 읽기 쉽다.
- 복잡한 로직, 태스크 구현, 여러 서브프로젝트가 공유하는 값은 `project/*.scala`로 분리하는 편이 관리에 유리하다.
- 팀의 Scala 숙련도와 빌드 복잡도에 따라 경계를 조정하면 된다.

### 고급: project/*.scala에 auto plugin 정의

숙련된 사용자는 `project/` 아래 `.scala` 파일에 직접 auto plugin을 정의해, 모든 서브프로젝트에 커스텀 태스크나 커맨드를 일괄로 주입할 수 있다. 이는 여러 서브프로젝트에 공통 동작을 강제해야 하는 대규모 빌드에서 유용하다.

---

## 5. Getting Started 요약

sbt Getting Started 시리즈 전체가 강조하는 핵심 개념은 다음 네 가지로 정리된다.

| 개념 | 요지 |
|---|---|
| 빌드 정의 | `.sbt` 파일과 태스크 간 의존성 그래프로 구성된다 |
| 설정 생성 | `:=`, `+=`, `++=` 연산자로 키에 값을 연결한다 |
| 스코프 | 구성(configuration), 프로젝트, 태스크 세 축으로 같은 키에 다른 값을 지정할 수 있다 |
| 플러그인 | `project/plugins.sbt`에 선언해 빌드 기능을 확장한다 |

이 개념들이 아직 낯설다면 다시 앞 단계 문서로 돌아가 복습하거나, sbt의 대화형 콘솔에서 직접 명령을 실행해 보며 감을 잡는 편이 좋다. sbt는 오픈소스 프로젝트이므로 궁금한 동작은 소스 코드를 직접 확인해 검증할 수도 있다. Getting Started를 마쳤다면 다음 단계로는 FAQ 문서를 참고한다.

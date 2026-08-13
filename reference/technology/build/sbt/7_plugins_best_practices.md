# sbt 플러그인과 베스트 프랙티스

## sbt 플러그인과 베스트 프랙티스

> 원본: https://www.scala-sbt.org/1.x/docs/Best-Practices.html , https://www.scala-sbt.org/1.x/docs/Plugins.html , https://www.scala-sbt.org/1.x/docs/Plugins-Best-Practices.html , https://www.scala-sbt.org/1.x/docs/GitHub-Actions-with-sbt.html , https://www.scala-sbt.org/1.x/docs/Travis-CI-with-sbt.html , https://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html , https://www.scala-sbt.org/1.x/docs/sbt-new-and-Templates.html , https://www.scala-sbt.org/1.x/docs/Cross-Build-Plugins.html

---

### 목차

1. [빌드 작성 베스트 프랙티스](#1-빌드-작성-베스트-프랙티스)
2. [플러그인 작성법(AutoPlugin)](#2-플러그인-작성법autoplugin)
3. [플러그인 작성 베스트 프랙티스](#3-플러그인-작성-베스트-프랙티스)
4. [GitHub Actions 연동](#4-github-actions-연동)
5. [Travis CI 연동](#5-travis-ci-연동)
6. [sbt 플러그인 테스트(scripted)](#6-sbt-플러그인-테스트scripted)
7. [sbt new와 giter8 템플릿](#7-sbt-new와-giter8-템플릿)
8. [플러그인의 크로스 빌드](#8-플러그인의-크로스-빌드)

---

### 1. 빌드 작성 베스트 프랙티스

sbt 공식 문서는 빌드 정의 작성 시 지켜야 할 원칙 몇 가지를 제시 → 대부분 재사용성·이식성·예측 가능성을 높이는 방향

#### 1.1 `project/` 디렉터리와 `~/.sbt/` 디렉터리의 구분

- `<project>/project/`: 프로젝트 빌드에 필수적인 요소(빌드를 컴파일하는 데 반드시 필요한 플러그인 등)
- `~/.sbt/1.0/`: 개인 취향에 따른 선택적 확장(IDE 연동 플러그인, 개인 alias 등)

- 프로젝트가 다른 팀원의 머신이나 CI 환경으로 옮겨져도 동일하게 빌드되려면 빌드에 실제로 필요한 것과 개발자 개인의 편의 설정을 명확히 구분 필요
- `project/plugins.sbt`에는 빌드 수행에 꼭 필요한 플러그인만 배치, 개인 취향의 플러그인(코드 포매터 알림, 통계 수집 등)은 전역 디렉터리에 배치

#### 1.2 로컬 설정 관리 방법 두 가지

- 전역 설정 파일: `$HOME/.sbt/1.0/global.sbt`에 배치 → 머신에 있는 모든 프로젝트에 공통 적용
- 프로젝트별 로컬 설정: 프로젝트 루트에 `local.sbt` 같은 파일을 추가하고 버전 관리에서 제외(`.gitignore`) → `build.sbt`와 별개 파일로 두면 여러 설정을 조합해서 로드 가능

```scala
// local.sbt (버전 관리 제외 대상)
resolvers := {
  val localMaven = "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository"
  localMaven +: resolvers.value
}
```

#### 1.3 `.sbtrc` 파일

sbt 시작 시 자동으로 읽어 실행하는 커맨드 파일 → 다음 순서로 탐색

1. `$HOME/.sbtrc`
2. `<project>/.sbtrc`

alias 정의나 초기화 커맨드를 담아두는 용도

#### 1.4 생성 파일은 반드시 `target` 하위에 쓴다

- 빌드 과정에서 생성하는 파일은 항상 `target` 설정이 가리키는 출력 디렉터리 아래에 작성 필요
- Scala 버전에 따라 결과물이 달라지는 파일은 버전별로 분리된 `crossTarget` 사용
- 이렇게 하면 `clean` 한 번으로 생성물을 안전하게 정리 가능 → 크로스 빌드 시 버전 간 산출물이 서로 덮어쓰지 않음

#### 1.5 상수를 하드코딩하지 않는다

```scala
// 좋지 않은 예
myDirectory := "target/sub-directory"

// 권장하는 예
myDirectory := target.value / "sub-directory"
```

경로 문자열을 직접 적으면 사용자가 `target` 설정을 다른 값으로 바꿨을 때 플러그인이 엉뚱한 위치를 가리키게 됨 → 항상 관련 설정 키를 참조해 값을 조합해야 재사용 가능한 플러그인이 됨

#### 1.6 파일을 여러 태스크에서 변경(mutate)하지 않는다

```scala
lazy val makeFile = taskKey[File]("Creates a file with some content.")

makeFile := {
  val f: File = file("/tmp/data.txt")
  IO.write(f, "Some content")
  f
}

useFile := doSomething(makeFile.value)
```

- 각 파일은 정확히 한 태스크에서만 작성 필요
- 여러 태스크가 같은 파일을 서로 다른 시점에 고쳐 쓰면 실행 순서에 따라 결과가 달라지는 비결정적 빌드가 되기 쉬움
- 파일 하나에 하나의 책임을 부여하는 편이 캐싱과 증분 빌드에도 유리

#### 1.7 절대 경로를 사용한다

```scala
file("/home/user/A.scala")           // 절대 경로 직접 지정

myPath := baseDirectory.value / "licenses"   // 기준 디렉터리 기반 조합
```

- JVM에서 상대 경로는 프로세스의 현재 작업 디렉터리를 기준으로 해석 → 이 작업 디렉터리가 항상 빌드 루트와 같으리라는 보장 없음
- 따라서 파일 경로는 절대 경로로 만들거나 `baseDirectory`, `target` 같은 기준 설정에 상대 경로를 조합해서 사용 필요
- 예외: 프로젝트의 기본 디렉터리 지정 시에만 상대 경로 사용 허용, sbt가 알아서 해석

#### 1.8 파서 조합자(Parser Combinators) 작성 지침

커맨드나 입력 태스크의 인자를 파싱할 때 지켜야 할 규칙 세 가지

1. 토큰 경계를 명확히 하기 위해 어디서든 `token`으로 감쌈
2. 토큰을 중첩하거나 중복시키지 않음
3. 재귀적인 파서는 `flatMap`으로 구현

```scala
lazy val parser: Parser[Int] =
  token(IntBasic) flatMap { i =>
    if (i <= 0)
      success(i)
    else
      token(Space ~> parser)
  }
```

위 예시: 공백으로 구분된 정수 목록을 파싱하다가 음수를 만나면 종료하는 파서

---

### 2. 플러그인 작성법(AutoPlugin)

#### 2.1 플러그인이란

- sbt 플러그인: 외부 코드를 빌드 정의 안으로 끌어들이는 수단
- 태스크 구현에 쓰이는 단순한 라이브러리일 수도, 모든 프로젝트에 설정을 자동으로 주입하는 확장일 수도 있음

#### 2.2 플러그인 추가하기

`project/plugins.sbt`에 선언

```scala
addSbtPlugin("org.example" % "plugin" % "1.0")
addSbtPlugin("org.example" % "another-plugin" % "2.0")

libraryDependencies += "org.example" % "utilities" % "1.3"
resolvers += "Example Plugin Repository" at "https://example.org/repo/"
```

#### 2.3 프로젝트에서 플러그인 활성화/비활성화

```scala
lazy val util = (project in file("util"))
  .enablePlugins(FooPlugin, BarPlugin)
  .disablePlugins(plugins.IvyPlugin)
  .settings(
    name := "hello-util"
  )
```

#### 2.4 AutoPlugin 기본 구조

```scala
package sbthello

import sbt._
import Keys._

object HelloPlugin extends AutoPlugin {
  override def trigger = allRequirements

  object autoImport {
    val helloGreeting = settingKey[String]("greeting")
    val hello = taskKey[Unit]("say hello")
  }

  import autoImport._
  override lazy val globalSettings: Seq[Setting[_]] = Seq(
    helloGreeting := "hi"
  )

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    hello := {
      val s = streams.value
      val g = helloGreeting.value
      s.log.info(g)
    }
  )
}
```

#### 2.5 `trigger`: 자동 활성화 조건

- `allRequirements`: `requires`로 지정한 의존 플러그인 조건이 충족되면 자동으로 활성화됨
- 지정하지 않으면 기본값은 루트 플러그인 취급 → 사용자가 `enablePlugins`로 명시적으로 활성화 필요

#### 2.6 `requires`: 플러그인 간 의존 관계

```scala
override def requires = SbtJsTaskPlugin
// 여러 플러그인에 동시 의존
override def requires = SbtJsTaskPlugin && SbtWebPlugin
// 의존성 없음
override def requires = empty
```

#### 2.7 설정 범위: projectSettings / buildSettings / globalSettings

- `projectSettings`: 플러그인이 활성화된 각 프로젝트마다 추가
- `buildSettings`: 빌드 전체에 한 번만 추가
- `globalSettings`: 전역(Global 스코프) 기본값이나 커맨드 등록에 사용

```scala
override lazy val globalSettings: Seq[Setting[_]] = Seq(
  greeting := "Hi!",
  commands += helloCommand
)

override def buildSettings: Seq[Setting[_]] = Nil

override lazy val projectSettings: Seq[Setting[_]] = Seq(
  obfuscate := { /* ... */ }
)
```

#### 2.8 `autoImport` 객체

- `autoImport`라는 이름의 안정적인 필드(`val` 또는 `object`)를 플러그인에 정의해두면 그 안의 내용이 `set`, `eval`, `.sbt` 파일에서 와일드카드 임포트 형태로 자동 노출됨
- 사용자는 별도의 import 문 없이 플러그인이 정의한 키를 바로 사용 가능

```scala
object autoImport {
  val obfuscate = taskKey[Seq[File]]("Obfuscates files.")
  val obfuscateLiterals = settingKey[Boolean]("Obfuscate literals.")
}
```

#### 2.9 플러그인 개발 프로젝트 설정

```scala
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"

lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-hello",
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.2.8" // 지원할 최소 sbt 버전
      }
    }
  )
```

`SbtPlugin`을 활성화하면 해당 프로젝트가 sbt 플러그인 프로젝트로 취급됨 → `scripted` 태스크 등 플러그인 개발 전용 설정이 함께 포함됨

#### 2.10 완전한 예제: 난독화 플러그인

```scala
package sbtobfuscate

import sbt._
import sbt.Keys._

object ObfuscatePlugin extends AutoPlugin {
  object autoImport {
    val obfuscate = taskKey[Seq[File]]("Obfuscates files.")
    val obfuscateLiterals = settingKey[Boolean]("Obfuscate literals.")

    lazy val baseObfuscateSettings: Seq[Def.Setting[_]] = Seq(
      obfuscate := {
        Obfuscate(sources.value, (obfuscate / obfuscateLiterals).value)
      },
      obfuscate / obfuscateLiterals := false
    )
  }

  import autoImport._
  override def requires = sbt.plugins.JvmPlugin
  override def trigger = allRequirements

  override val projectSettings =
    inConfig(Compile)(baseObfuscateSettings) ++
      inConfig(Test)(baseObfuscateSettings)
}

object Obfuscate {
  def apply(sources: Seq[File], obfuscateLiterals: Boolean): Seq[File] = {
    // 난독화 로직
    sources
  }
}
```

사용자 빌드에서는 다음처럼 설정 덮어쓰기 가능

```scala
obfuscate / obfuscateLiterals := true
```

#### 2.11 전역 플러그인과 퍼블리싱

- `$HOME/.sbt/1.0/plugins/build.sbt`에 플러그인을 추가하면 해당 머신의 모든 프로젝트에서 사용 가능
- 플러그인 퍼블리싱은 일반 라이브러리와 같은 절차를 따름
  - Maven 레이아웃 저장소에 퍼블리시 시 sbt 1.9.x 이상에서는 다음 설정으로 레이아웃 방식 제어

```scala
sbtPluginPublishLegacyMavenStyle := false
```

---

### 3. 플러그인 작성 베스트 프랙티스

#### 3.1 키 네이밍: 플러그인 접두사

키 이름 충돌을 피하기 위해 태스크·설정 키를 플러그인 이름으로 시작하도록 명명

```scala
package sbtassembly

object AssemblyPlugin extends AutoPlugin {
  object autoImport {
    val assembly = taskKey[File]("Builds a deployable fat jar.")
    val assemblyJarName = taskKey[String]("name of the fat jar")
    val assemblyMergeStrategy = settingKey[String => MergeStrategy]("merge strategy")
  }
}
```

사용 시 메인 태스크 아래로 스코핑

```scala
assembly / assemblyJarName := "something.jar"
```

피해야 할 네이밍 예시:

```scala
// 좋지 않은 예
val jarName = SettingKey[String]("assembly-jar-name")
val jarName = SettingKey[String]("jar-name")
```

#### 3.2 아티팩트 이름과 클래스 이름 규칙

- 아티팩트 이름
  - 규칙: `sbt-{projectname}`
  - 좋은 예: `sbt-foobar`
  - 나쁜 예: `foobar`, `foobar-sbt`, `sbt-foobar-plugin`
- 플러그인 클래스
  - 규칙: `FooBarPlugin`
  - 좋은 예: `AssemblyPlugin`

#### 3.3 기본 패키지 사용 금지

플러그인 소스에는 반드시 패키지 선언 필요

```scala
package sbtassembly
```

패키지가 없으면 사용자의 `build.sbt`가 패키지 안에 있을 때 플러그인 심볼을 import할 방법이 없어짐

#### 3.4 명령(Command) 대신 설정(Setting)과 태스크(Task)를 쓴다

```scala
// 피해야 할 방식: Command는 조합할 수 없다
lazy val myCommand = Command.command("mycommand") { state =>
  state
}

// 권장 방식: Task는 다른 태스크·설정과 조합되고 병렬 실행된다
lazy val myTask = taskKey[Unit]("My task")
override lazy val projectSettings = Seq(
  myTask := { /* ... */ }
)
```

- 태스크는 다른 태스크나 설정으로부터 값을 끌어와 조합 가능, 병렬 실행·중복 제거·`ScopeFilter` 같은 고급 기능도 그대로 활용 가능
- 명령은 `State`를 직접 다루는 저수준 API → 조합성 떨어짐

#### 3.5 핵심 로직은 순수 Scala 객체로 분리한다

```scala
object AssemblyPlugin extends AutoPlugin {
  override lazy val projectSettings = Seq(
    assembly := Assembly((assembly / sources).value)
  )
}

object Assembly {
  def apply(sources: Seq[File]): File = {
    new File("result.jar")
  }
}
```

sbt API에 의존하지 않는 순수 객체로 로직을 분리해두면 다른 플러그인이나 도구에서도 재사용 가능

#### 3.6 새 키보다 기존 키를 재사용한다

```scala
// 피해야 할 방식
val sourceFiles = settingKey[Seq[File]]("Some source files")

// 권장 방식
sources // sbt 기본 제공 키 재사용
```

새로운 개념이 아니라면 sbt가 미리 정의해둔 `Keys` 객체의 키를 그대로 쓰는 편이 사용자 학습 비용을 줄임

#### 3.7 Configuration은 필요할 때만 새로 만든다

새로운 소스 코드 집합이나 별도의 라이브러리 의존성이 필요한 경우에만 새 Configuration 생성

```scala
package sbtfuzz

object FuzzPlugin extends AutoPlugin {
  object autoImport {
    lazy val Fuzz = config("fuzz") extend Compile
  }
  import autoImport._

  lazy val baseFuzzSettings: Seq[Def.Setting[_]] = Seq(
    test := { println("fuzz test") }
  )

  override lazy val projectSettings = inConfig(Fuzz)(baseFuzzSettings)
}
```

사용자 측 설정:

```scala
Fuzz / scalaSource := baseDirectory.value / "source" / "fuzz" / "scala"
Compile / scalaSource := baseDirectory.value / "source" / "main" / "scala"
```

반대로 단순한 태스크·설정이라면 새 Configuration을 만들지 말고 기존 Configuration(`Compile` 등) 재사용

```scala
// 피해야 할 방식: 불필요한 Configuration 신설
lazy val Whatever = config("whatever") extend Compile
lazy val specificKey = settingKey[String]("...")
override lazy val projectSettings = Seq(
  Whatever / specificKey := "value"
)

// 권장 방식: 기존 Configuration 활용
override lazy val projectSettings = inConfig(Compile)(baseFuzzSettings)
```

#### 3.8 Scoping 전략

프로젝트 레벨 설정에 의존하지 않는 기본값은 `globalSettings`에 배치

```scala
object ObfuscatePlugin extends AutoPlugin {
  object autoImport {
    lazy val obfuscate = taskKey[Seq[File]]("obfuscate sources")
    lazy val obfuscateOption = settingKey[ObfuscateOption]("options")
  }
  import autoImport._

  override lazy val globalSettings = Seq(
    obfuscateOption := ObfuscateOption()
  )

  override lazy val projectSettings = inConfig(Compile)(
    Seq(
      obfuscate := Obfuscate(
        (obfuscate / sources).value,
        (obfuscate / obfuscateOption).value
      ),
      obfuscate / sources := sources.value
    )
  )
}
```

사용자는 빌드 전체 또는 특정 프로젝트 단위로 값 덮어쓰기 가능

```scala
// 빌드 전체에 적용
ThisBuild / obfuscate / obfuscateOption := ObfuscateOption().withX(true)

// 특정 프로젝트에만 적용
obfuscate / obfuscateOption := ObfuscateOption().withY(false)
```

메인 태스크를 스코프로 삼아 다른 설정을 매다는 패턴도 자주 사용

```scala
lazy val baseObfuscateSettings: Seq[Def.Setting[_]] = Seq(
  obfuscate := Obfuscate((obfuscate / sources).value),
  obfuscate / sources := sources.value // 메인 태스크로 스코핑
)
```

이렇게 만들어두면 다른 Configuration에서도 재사용 가능

```scala
lazy val app = project.settings(
  inConfig(Test)(ObfuscatePlugin.baseObfuscateSettings)
)
```

기존 `globalSettings`(예: `Global / onLoad`) 수정 시 이전 핸들러를 지우지 말고 체이닝

```scala
object MyPlugin extends AutoPlugin {
  override val globalSettings: Seq[Def.Setting[_]] = Seq(
    Global / onLoad := (Global / onLoad).value andThen { state =>
      // 새 로직 추가
      state
    }
  )
}
```

#### 3.9 raw 설정과 configured 설정을 함께 제공한다

```scala
package sbtobfuscate

object ObfuscatePlugin extends AutoPlugin {
  object autoImport {
    lazy val obfuscate = taskKey[Seq[File]]("obfuscate sources")
    lazy val obfuscateStylesheet = settingKey[File]("stylesheet")
  }
  import autoImport._

  // raw 설정: 다른 Configuration에서도 재사용 가능
  lazy val baseObfuscateSettings: Seq[Def.Setting[_]] = Seq(
    obfuscate := Obfuscate((obfuscate / sources).value),
    obfuscate / sources := sources.value
  )

  // Compile 스코프에 적용된 기본 설정
  override lazy val projectSettings = inConfig(Compile)(baseObfuscateSettings)
}
```

이렇게 raw 설정 시퀀스를 공개해두면 사용자가 원하는 Configuration에 직접 적용 가능 → 유연성 확대

#### 3.10 플러그인 공지

플러그인을 만들었다면 다른 사용자가 발견할 수 있도록 공지 필요

- Twitter에서 `@scala_sbt`를 태그해 공지
- GitHub의 `sbt/website` 저장소에 있는 Community Plugins 목록에 추가

#### 3.11 체크리스트

- 키 네이밍: 플러그인 접두사 사용, 메인 태스크로 스코핑
- 아티팩트 이름: `sbt-{projectname}`
- 플러그인 클래스명: `FooBarPlugin`
- 패키지: 기본 패키지 사용 금지
- Command vs Task: Task를 우선 사용, Command는 지양
- 키 재사용: 새 키보다 sbt 기본 키 우선
- Configuration: 새 소스·의존성이 필요할 때만 신설
- Scoping: 넓은 범위에서 정의하고 좁은 범위에서 사용
- Global 설정: 프로젝트에 의존하지 않는 기본값만, 기존 핸들러는 체이닝
- 문서화·공지: 발견하기 쉽게 커뮤니티에 알림

---

### 4. GitHub Actions 연동

#### 4.1 sbt 버전 고정

`project/build.properties`에 사용할 sbt 버전을 명시 → CI 환경에서도 로컬과 동일한 버전 사용

```
sbt.version=1.10.10
```

#### 4.2 최소 워크플로우

Java와 sbt 런처를 구성한 뒤 테스트를 돌리는 최소 구성

```yaml
name: CI
on:
  pull_request:
  push:
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 11
      - uses: sbt/setup-sbt@v1
      - run: sbt +test
```

- `sbt/setup-sbt@v1`: sbt 런처 자체를 설치
- `sbt +test`의 `+`: `crossScalaVersions`에 설정된 모든 Scala 버전에 대해 테스트 반복 실행

#### 4.3 JVM 옵션 커스터마이징

메모리 힙이나 코드 캐시 크기를 환경변수로 조정 가능

```yaml
env:
  JAVA_OPTS: -Xms2048M -Xmx2048M -Xss6M -XX:ReservedCodeCacheSize=256M
  JVM_OPTS: -Xms2048M -Xmx2048M -Xss6M -XX:ReservedCodeCacheSize=256M
```

#### 4.4 의존성 캐싱

`setup-java`의 `cache: sbt` 옵션 하나로 ivy/coursier 캐시를 재사용해 빌드 속도 향상 가능

```yaml
- uses: actions/setup-java@v4
  with:
    distribution: temurin
    java-version: 8
    cache: sbt
```

#### 4.5 빌드 매트릭스

여러 JDK 버전과 OS 조합에서 동시에 테스트

```yaml
strategy:
  fail-fast: false
  matrix:
    include:
      - os: ubuntu-latest
        java: 8
      - os: ubuntu-latest
        java: 17
      - os: windows-latest
        java: 17
runs-on: ${{ matrix.os }}
```

`fail-fast: false` 지정 시 한 조합이 실패해도 나머지 조합을 계속 실행 → 전체 매트릭스의 결과 확인 가능

---

### 5. Travis CI 연동

Travis CI는 과거 sbt 프로젝트에서 널리 쓰인 CI 서비스 → GitHub Actions로 이전 권장되나 레거시 설정 이해를 위해 함께 정리

#### 5.1 sbt 버전 고정

GitHub Actions와 마찬가지로 `project/build.properties`에 버전 명시

```
sbt.version=1.10.10
```

#### 5.2 기본 `.travis.yml`

```yaml
language: scala
jdk: openjdk8

scala:
   - 2.10.4
   - 2.12.18

script:
   - sbt ++$TRAVIS_SCALA_VERSION test
```

- `$TRAVIS_SCALA_VERSION`: Travis가 `scala:` 목록의 각 버전을 순회하며 채워주는 환경변수
- `++`: sbt 셸에서 특정 Scala 버전으로 전환하는 커맨드

#### 5.3 플러그인 빌드 설정

플러그인은 Scala 버전 크로스 빌드가 굳이 필요 없는 경우가 많음

```yaml
language: scala
jdk: openjdk8

script:
   - sbt scripted
```

#### 5.4 JVM 옵션 커스터마이징

파일로 지정하는 방식:

```yaml
script:
   - sbt ++$TRAVIS_SCALA_VERSION -jvm-opts travis/jvmopts test
```

인라인으로 지정하는 방식:

```yaml
script:
   - sbt -Dfile.encoding=UTF8 -J-XX:ReservedCodeCacheSize=256M test
```

#### 5.5 캐싱

의존성 캐시 디렉터리를 지정 → 캐시 저장 전에 잠금 파일과 타임스탬프 메타데이터를 정리해 캐시 오염 방지

```yaml
cache:
  directories:
    - $HOME/.cache/coursier
    - $HOME/.ivy2/cache
    - $HOME/.sbt

before_cache:
  - rm -fv $HOME/.ivy2/.sbt.ivy.lock
  - find $HOME/.ivy2/cache -name "ivydata-*.properties" -print -delete
  - find $HOME/.sbt -name "*.lock" -print -delete
```

#### 5.6 빌드 매트릭스로 테스트 분할

`scripted` 테스트가 많을 때 여러 잡으로 나눠 병렬 실행

```yaml
env:
  matrix:
    - TEST_COMMAND="scripted tests/*1of3"
    - TEST_COMMAND="scripted tests/*2of3"
    - TEST_COMMAND="scripted tests/*3of3"

script:
   - sbt "$TEST_COMMAND"
```

#### 5.7 알림 설정

```yaml
notifications:
  email:
    recipients:
      - dev@example.com
    on_success: always
```

#### 5.8 종합 예시

```yaml
language: scala
jdk: openjdk8

env:
  matrix:
    - TEST_COMMAND="scripted sbt-assembly/*"
    - TEST_COMMAND="scripted merging/* caching/*"

script:
  - sbt -Dfile.encoding=UTF8 -J-XX:ReservedCodeCacheSize=256M "$TEST_COMMAND"

before_cache:
  - rm -fv $HOME/.ivy2/.sbt.ivy.lock
  - find $HOME/.ivy2/cache -name "ivydata-*.properties" -print -delete
  - find $HOME/.sbt -name "*.lock" -print -delete

cache:
  directories:
    - $HOME/.cache/coursier
    - $HOME/.ivy2/cache
    - $HOME/.sbt
```

---

### 6. sbt 플러그인 테스트(scripted)

#### 6.1 scripted 프레임워크 개요

- scripted: 빌드 시나리오를 스크립트로 기술해 플러그인을 테스트하는 프레임워크
- 파일 변경 감지, 증분 컴파일처럼 실제 프로젝트에서만 재현되는 복잡한 상황 검증에 적합

#### 6.2 설정 준비

- 프로젝트 버전을 SNAPSHOT으로 지정 필요 → scripted-plugin이 테스트 대상 플러그인을 로컬 저장소에 퍼블리시한 뒤 그 버전을 참조해 테스트 프로젝트를 실행하는 구조이기 때문, SNAPSHOT이 아니면 버전 불일치 문제 발생
- `SbtPlugin` 활성화 필요

```scala
lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-something",
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false
  )
```

scripted 사용에는 sbt 1.2.1 이상 필요

#### 6.3 디렉터리 구조

```
src/sbt-test/
  └── <플러그인-이름>/
      └── simple/
          ├── build.sbt
          ├── project/
          │   └── plugins.sbt
          ├── src/main/scala/
          └── test
```

각 테스트 케이스는 `src/sbt-test/{그룹}/{케이스이름}/` 아래에 독립된 미니 sbt 프로젝트로 존재

`project/plugins.sbt`에서는 시스템 프로퍼티로 전달받은 플러그인 버전을 사용해 테스트 대상 플러그인 추가

```scala
sys.props.get("plugin.version") match {
  case Some(x) => addSbtPlugin("com.eed3si9n" % "sbt-assembly" % x)
  case _ => sys.error("plugin.version이 정의되지 않았습니다")
}
```

#### 6.4 test 스크립트 문법

파일 이름은 관례상 `test`, 아래 문법으로 작성

- `# 텍스트`: 한 줄 주석
- `> taskname`: sbt 태스크 실행, 성공해야 통과
- `-> taskname`: sbt 태스크 실행, 실패해야 통과
- `$ command arg*`: 파일 커맨드 실행, 성공해야 통과
- `-$ command arg*`: 파일 커맨드 실행, 실패해야 통과

파일 커맨드 목록:

- `touch path+`: 파일 생성 또는 타임스탬프 갱신
- `delete path+`: 파일 삭제
- `exists path+`: 파일 존재 확인
- `absent path+`: 파일 부재 확인
- `mkdir path+`: 디렉터리 생성
- `newer source target`: source가 target보다 최신인지 확인
- `copy-file fromPath toPath`: 파일 복사
- `copy fromPath+ toDir`: 상대 디렉터리 구조를 유지한 채 복사
- `copy-flat fromPath+ toDir`: 디렉터리 구조 없이 평면적으로 복사
- `exec command args*`: 별도 프로세스에서 커맨드 실행
- `sleep time`: 밀리초 단위로 대기
- `pause`: Enter 입력을 기다림(디버깅용)

#### 6.5 스크립트 예시

```
# assembly 태스크 실행
> assembly
# 생성된 jar 파일 확인
$ exists target/scala-2.10/foo.jar

# 소스 변경 후 재컴파일 실패를 검증
$ copy-file changes/A.scala A.scala
-> compile
```

#### 6.6 커스텀 검증 로직 추가

테스트 프로젝트의 `build.sbt`에 검증용 태스크 직접 정의 가능

```scala
import scala.sys.process.Process

TaskKey[Unit]("check") := {
  val process = Process("java", Seq("-jar", "foo.jar"))
  val output = process.!!
  if (output.trim != "expected") sys.error("예상 출력과 다릅니다: " + output)
  ()
}
```

이렇게 정의한 `check` 태스크는 스크립트에서 `> check`로 호출

#### 6.7 실행과 디버깅

```
# 전체 테스트 실행
> scripted

# 특정 테스트만 실행
> scripted sbt-assembly/simple

# 로그 버퍼링 비활성화(실패 원인 파악에 유용)
> set scriptedBufferLog := false
```

- 테스트 스크립트 안에 `pause`를 넣어 중간에 실행을 멈추고 상태를 직접 들여다보는 것도 가능
- 테스트는 원본 디렉터리가 아니라 임시로 복사된 위치에서 실행 → 실패 시 로그에 나오는 임시 경로를 확인해 디버깅

---

### 7. sbt new와 giter8 템플릿

#### 7.1 `sbt new` 개요

- `sbt new`: sbt 0.13.13부터 제공되는 명령, 템플릿으로부터 새 빌드 정의(프로젝트 뼈대) 생성
- sbt 런처 0.13.13 이상 필요
- 신규 프로젝트를 만드는 명령이므로 `project/build.properties` 파일이 미리 없어도 동작

#### 7.2 기본 사용법

```bash
$ sbt new scala/scala-seed.g8
```

동작 순서

1. GitHub 저장소(또는 임의의 git 저장소)에서 템플릿을 내려받음
2. 템플릿에 정의된 파라미터 값을 사용자에게 입력받음
3. 입력받은 값으로 치환한 프로젝트를 예를 들어 `./hello` 같은 디렉터리에 생성

특정 브랜치 지정도 가능

```bash
$ sbt new scala/scala-seed.g8 --branch myBranch
```

#### 7.3 공식 giter8 템플릿 예시

- 일반 Scala 프로젝트: `scala/scala-seed.g8`
- Scala 3 프로젝트: `scala/scala3.g8`
- Akka: `akka/akka-quickstart-scala.g8`
- Play Framework: `playframework/play-scala-seed.g8`
- Lagom: `lagom/lagom-scala.g8`
- Scala Native: `scala-native/scala-native.g8`

#### 7.4 giter8이란

- giter8: GitHub을 호스팅으로 사용하는 템플릿 프로젝트, 누구나 템플릿을 만들어 공유 가능
- Nathan Hamblen이 2010년에 시작 → 현재는 foundweekends 조직이 관리

#### 7.5 커스텀 템플릿 만들기

giter8 템플릿 자체를 생성하는 템플릿도 giter8 형태로 제공됨

```bash
$ sbt new foundweekends/giter8.g8
```

상세한 문법(디렉터리 구조, `default.properties`, 플레이스홀더 치환 규칙 등)은 foundweekends의 giter8 공식 문서 참고

- 라이선스 권장: 템플릿에는 CC0 1.0 라이선스 권장
- CC0 1.0은 저작권과 관련 권리를 모두 포기해 퍼블릭 도메인과 유사한 효과
- MIT·Apache 라이선스와 달리 템플릿을 사용해 만든 결과물에 출처 표기 의무를 지우지 않음 → 프로젝트 뼈대로 쓰기에 적합

#### 7.6 `sbt new` 확장하기: 템플릿 리졸버

- 개념: 템플릿 리졸버(template resolver)는 `sbt new` 뒤에 오는 인자를 검사해 어떤 템플릿을 가리키는지 해석하는 부분 함수
- 라이브러리 의존성의 `ModuleID`를 인터넷에서 찾아주는 리졸버와 비슷한 역할

구현 단계

1. `org.scala-sbt % template-resolver % 0.1`에 의존하는 라이브러리 생성
2. `TemplateResolver` 인터페이스 구현

```java
public interface TemplateResolver {
  public boolean isDefined(String[] arguments);
  public void run(String[] arguments);
}
```

- `isDefined()`: 주어진 인자를 이 리졸버가 처리할 수 있는지 판단
- `run()`: 실제로 템플릿을 내려받고 프로젝트를 생성

3. sbt 플러그인으로 리졸버 등록

```scala
templateResolverInfos +=
  TemplateResolverInfo(
    ModuleID("org.scala-sbt.sbt-giter8-resolver",
      "sbt-giter8-resolver", "0.1.0"),
    "sbtgiter8resolver.Giter8TemplateResolver"
  )
```

#### 7.7 내장 `Giter8TemplateResolver`의 동작 원리

sbt에 기본 내장된 리졸버는 다음 순서로 인자를 판별

1. 하이픈(`-`)으로 시작하지 않는 첫 번째 인자를 확인
2. 그 인자가 GitHub 저장소 패턴(`owner/repo`) 또는 `.g8`로 끝나는 git 저장소 패턴과 일치하는지 검사
3. 일치하면 해당 인자를 giter8 엔진에 넘겨 템플릿을 처리

이 구조 덕분에 `sbt new`는 여러 종류의 템플릿 소스를 리졸버 체인으로 확장해서 지원 가능

---

### 8. 플러그인의 크로스 빌드

#### 8.1 왜 sbt 버전 크로스 빌드가 필요한가

- sbt 플러그인은 특정 sbt 버전의 API에 의존
- 사용자층이 sbt 0.13과 1.x에 걸쳐 있던 시기처럼 서로 다른 메이저 버전을 동시에 지원해야 하는 플러그인은 Scala 버전 크로스 빌드와 별개로 sbt 버전 자체도 크로스 빌드 필요

#### 8.2 `crossSbtVersions` 설정

```scala
crossSbtVersions := Vector("1.2.8", "0.13.18")
```

이 설정을 추가하면 플러그인을 각 sbt 버전에 맞춰 순차적으로 빌드 가능

#### 8.3 sbt 버전 전환과 크로스 컴파일 커맨드

```
^^ 0.13.18
```

이 커맨드를 실행하면 다음과 같은 로그가 출력되며 `pluginCrossBuild / sbtVersion` 전환됨

```
[info] Setting `sbtVersion in pluginCrossBuild` to 0.13.18
```

`crossSbtVersions`에 등록된 모든 버전에 대해 한 번에 컴파일하려면 다음 사용

```
^compile
```

#### 8.4 버전별 소스 코드 분리

sbt 0.13과 1.x 사이에 API 차이가 있는 부분은 버전별 소스 디렉터리로 분리해서 구현

```
src/main/scala-sbt-0.13/   # sbt 0.13 전용 코드
src/main/scala-sbt-1.0/    # sbt 1.x 전용 코드
```

sbt는 현재 크로스 빌드 중인 sbt 버전에 맞춰 해당 디렉터리의 소스만 컴파일에 포함

#### 8.5 라이브러리와 플러그인을 한 빌드에서 함께 관리하기

- 같은 코드베이스 안에 일반 라이브러리(Scala 버전 크로스 빌드 대상)와 sbt 플러그인(sbt 버전 크로스 빌드 대상)을 함께 두는 경우가 있음
- 이때는 Scala 바이너리 버전에 따라 사용할 sbt 버전을 매핑

```scala
ThisBuild / crossScalaVersions := Seq("2.10.7", "2.12.10")

lazy val plugin = (project in file("sbt-something"))
  .enablePlugins(SbtPlugin)
  .dependsOn(core)
  .settings(
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.10" => "0.13.18"
        case "2.12" => "1.2.8"
      }
    }
  )
```

이렇게 매핑해두면 `+compile`, `+publish`처럼 이미 익숙한 Scala 크로스 빌드 커맨드를 그대로 실행하는 것만으로 내부적으로 각 Scala 버전에 맞는 sbt 버전이 자동 선택되어 빌드·퍼블리시가 이뤄짐

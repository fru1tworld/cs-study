# sbt 빌드 예제 모음

> 원본: https://www.scala-sbt.org/1.x/docs/Examples.html, https://www.scala-sbt.org/1.x/docs/Basic-Def-Examples.html, https://www.scala-sbt.org/1.x/docs/Scala-Files-Example.html, https://www.scala-sbt.org/1.x/docs/Advanced-Configurations-Example.html, https://www.scala-sbt.org/1.x/docs/Advanced-Command-Example.html

---

## 목차

1. [.sbt 빌드 예제](#1-sbt-빌드-예제)
2. [.sbt 빌드 + .scala 파일 예제](#2-sbt-빌드--scala-파일-예제)
3. [고급 설정(configuration) 예제](#3-고급-설정configuration-예제)
4. [고급 커맨드 예제](#4-고급-커맨드-예제)

---

## 1. .sbt 빌드 예제

sbt 공식 문서의 Examples 섹션은 각 항목이 독립적인 `build.sbt` 설정 스니펫들을 모아 놓은 참고 자료다. sbt 0.13.7 이후로는 `build.sbt` 안에서 설정을 구분하는 데 더 이상 빈 줄이 필요하지 않으며, 아래 예제는 이 방식(세미콜론 없는 콤마 구분 설정 목록)을 기준으로 작성됐다.

### 1.1 공통 설정 팩터링

여러 프로젝트에서 공유해야 하는 값은 `ThisBuild` 스코프에 지정해 한 곳에서 관리한다.

```scala
import scala.concurrent.duration._

// factor out common settings
ThisBuild / organization := "org.myproject"
ThisBuild / scalaVersion := "2.12.18"
// set the Scala version used for the project
ThisBuild / version      := "0.1.0-SNAPSHOT"

// set the prompt (for this build) to include the project id.
ThisBuild / shellPrompt := { state => Project.extract(state).currentRef.project + "> " }
```

### 1.2 라이브러리 의존성 정의

일반적인 의존성은 `%%`(Scala 버전 자동 부착) 연산자로 `ModuleID`를 만들어 `lazy val`로 미리 뽑아둔다. 저장소가 아니라 특정 URL에서 직접 받아야 하는 아티팩트는 `from` 절과 문자열 인터폴레이션을 함께 쓴다.

```scala
// define ModuleID for library dependencies
lazy val scalacheck = "org.scalacheck" %% "scalacheck" % "1.17.0"

// define ModuleID using string interpolator
lazy val osmlibVersion = "2.5.2-RC1"
lazy val osmlib = ("net.sf.travelingsales" % "osmlib" % osmlibVersion from
  s"""http://downloads.sourceforge.net/project/travelingsales/libosm/$osmlibVersion/libosm-$osmlibVersion.jar""")
```

### 1.3 프로젝트별 설정 모음

아래는 `root` 프로젝트에 적용 가능한 개별 설정 키를 주제별로 묶어 정리한 것이다. 실제 문서에서는 하나의 `.settings(...)` 블록 안에 전부 나열되어 있다.

**소스 디렉터리·의존성**

```scala
// set the name of the project
name := "My Project",

// set the main Scala source directory to be <base>/src
Compile / scalaSource := baseDirectory.value / "src",

// set the Scala test source directory to be <base>/test
Test / scalaSource := baseDirectory.value / "test",

// add a test dependency on ScalaCheck
libraryDependencies += scalacheck % Test,

// add compile dependency on osmlib
libraryDependencies += osmlib,
```

**컴파일러·태스크 옵션**

```scala
// reduce the maximum number of errors shown by the Scala compiler
maxErrors := 20,

// increase the time between polling for file changes when using continuous execution
pollInterval := 1.second,

// append several options to the list of options passed to the Java compiler
javacOptions ++= Seq("-source", "1.5", "-target", "1.5"),

// append -deprecation to the options passed to the Scala compiler
scalacOptions += "-deprecation",

// define the statements initially evaluated when entering 'console', 'consoleQuick', or 'consoleProject'
initialCommands := """
  |import System.{currentTimeMillis => now}
  |def time[T](f: => T): T = {
  |  val start = now
  |  try { f } finally { println("Elapsed: " + (now - start)/1000.0 + " s") }
  |}""".stripMargin,

// set the initial commands when entering 'console' or 'consoleQuick', but not 'consoleProject'
console / initialCommands := "import myproject._",
```

**패키징·실행**

```scala
// set the main class for packaging the main jar
// 'run' will still auto-detect and prompt
// change Compile to Test to set it for the test jar
Compile / packageBin / mainClass := Some("myproject.MyMain"),

// set the main class for the main 'run' task
// change Compile to Test to set it for 'Test/run'
Compile / run / mainClass := Some("myproject.MyMain"),

// add <base>/input to the files that '~' triggers on
watchSources += baseDirectory.value / "input",
```

**저장소·퍼블리시**

```scala
// add a maven-style repository
resolvers += "name" at "url",

// add a sequence of maven-style repositories
resolvers ++= Seq("name" at "url"),

// define the repository to publish to
publishTo := Some("name" at "url"),

// set Ivy logging to be at the highest level
ivyLoggingLevel := UpdateLogging.Full,

// disable updating dynamic revisions (including -SNAPSHOT versions)
offline := true,
```

**셸 프롬프트·출력 제어**

```scala
// set the prompt (for the current project) to include the username
shellPrompt := { state => System.getProperty("user.name") + "> " },

// disable printing timing information, but still print [success]
showTiming := false,

// disable printing a message indicating the success or failure of running a task
showSuccess := false,

// change the format used for printing task completion time
timingFormat := {
    import java.text.DateFormat
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
},

// disable using the Scala version in output paths and artifacts
crossPaths := false,
```

**포킹·병렬 실행**

```scala
// fork a new JVM for 'run' and 'Test/run'
fork := true,

// fork a new JVM for 'Test/run', but not 'run'
Test / fork := true,

// add a JVM option to use when forking a JVM for 'run'
javaOptions += "-Xmx2G",

// only use a single thread for building
parallelExecution := false,

// Execute tests in the current project serially
//   Tests from other projects may still run concurrently.
Test / parallelExecution := false,

// set the location of the JDK to use for compiling Java code.
// if 'fork' is true, this is used for 'run' as well
javaHome := Some(file("/usr/lib/jvm/sun-jdk-1.6")),

// Use Scala from a directory on the filesystem instead of retrieving from a repository
scalaHome := Some(file("/home/user/scala/trunk/")),
```

**로깅·트레이스**

```scala
// don't aggregate clean (See FullConfiguration for aggregation details)
clean / aggregate := false,

// only show warnings and errors on the screen for compilations.
//  this applies to both Test/compile and compile and is Info by default
compile / logLevel := Level.Warn,

// only show warnings and errors on the screen for all tasks (the default is Info)
//  individual tasks can then be more verbose using the previous setting
logLevel := Level.Warn,

// only store messages at info and above (the default is Debug)
//   this is the logging level for replaying logging with 'last'
persistLogLevel := Level.Debug,

// only show 10 lines of stack traces
traceLevel := 10,

// only show stack traces up to the first sbt stack frame
traceLevel := 0,
```

**의존성·아티팩트 관리**

```scala
// add SWT to the unmanaged classpath
Compile / unmanagedJars += Attributed.blank(file("/usr/share/java/swt.jar")),

// publish test jar, sources, and docs
Test / publishArtifact := true,

// disable publishing of main docs
Compile / packageDoc / publishArtifact := false,

// change the classifier for the docs artifact
packageDoc / artifactClassifier := Some("doc"),

// Copy all managed dependencies to <build-root>/lib_managed/
//   This is essentially a project-local cache.  There is only one
//   lib_managed/ in the build root (not per-project).
retrieveManaged := true,

/* Specify a file containing credentials for publishing. The format is:
realm=Sonatype Nexus Repository Manager
host=nexus.scala-tools.org
user=admin
password=admin123
*/
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),

// Directly specify credentials for publishing.
credentials += Credentials("Sonatype Nexus Repository Manager", "nexus.scala-tools.org", "admin", "admin123"),

// Exclude transitive dependencies, e.g., include log4j without including logging via jdmk, jmx, or jms.
libraryDependencies +=
  "log4j" % "log4j" % "1.2.15" excludeAll(
    ExclusionRule(organization = "com.sun.jdmk"),
    ExclusionRule(organization = "com.sun.jmx"),
    ExclusionRule(organization = "javax.jms")
  )
```

이 설정들을 하나의 `root` 프로젝트에 모으면 다음과 같은 구조가 된다.

```scala
lazy val root = (project in file("."))
  .settings(
    // 위 목록의 설정을 모두 콤마로 나열
  )
```

### 1.4 핵심 패턴 요약

| 패턴 | 설명 |
|---|---|
| `ThisBuild / key := value` | 빌드 전체에 적용되는 전역 설정 |
| `Config / task / key := value` | 특정 설정(Compile/Test)과 태스크(run/packageBin) 조합에 국한된 설정 |
| `key += value` / `key ++= values` | 기존 시퀀스 값에 항목 추가 |
| `lazy val x = ...` | 의존성이나 설정 값을 빌드 파일 상단에서 재사용 가능하게 추출 |

---

## 2. .sbt 빌드 + .scala 파일 예제

`.sbt` 파일만으로 빌드가 비대해지면, 가장 먼저 저장소(resolver)와 의존성 선언을 `project/*.scala` 파일로 분리하는 것이 일반적인 다음 단계다. `project/` 디렉터리 아래의 `.scala` 파일은 sbt가 빌드 자체를 컴파일할 때 함께 컴파일되어, `build.sbt`에서 `import`로 바로 참조할 수 있다.

### 2.1 저장소 분리 — project/Resolvers.scala

Maven 저장소 목록을 한 객체에 모아두고 `Seq[Resolver]` 형태로 내보낸다.

```scala
import sbt._
import Keys._

object Resolvers {
  val sunrepo    = "Sun Maven2 Repo" at "http://download.java.net/maven/2"
  val sunrepoGF  = "Sun GF Maven2 Repo" at "http://download.java.net/maven/glassfish"
  val oraclerepo = "Oracle Maven2 Repo" at "http://download.oracle.com/maven"

  val oracleResolvers = Seq(sunrepo, sunrepoGF, oraclerepo)
}
```

### 2.2 의존성 분리 — project/Dependencies.scala

라이브러리 버전과 `ModuleID`를 한곳에서 관리하면, 여러 서브프로젝트가 필요한 의존성만 선택적으로 참조할 수 있다.

```scala
import sbt._
import Keys._

object Dependencies {
  val logbackVersion = "0.9.16"
  val grizzlyVersion = "1.9.19"

  val logbackcore    = "ch.qos.logback" % "logback-core"     % logbackVersion
  val logbackclassic = "ch.qos.logback" % "logback-classic"  % logbackVersion

  val jacksonjson = "org.codehaus.jackson" % "jackson-core-lgpl" % "1.7.2"

  val grizzlyframwork = "com.sun.grizzly" % "grizzly-framework" % grizzlyVersion
  val grizzlyhttp     = "com.sun.grizzly" % "grizzly-http"      % grizzlyVersion
  val grizzlyrcm      = "com.sun.grizzly" % "grizzly-rcm"       % grizzlyVersion
  val grizzlyutils    = "com.sun.grizzly" % "grizzly-utils"     % grizzlyVersion
  val grizzlyportunif = "com.sun.grizzly" % "grizzly-portunif"  % grizzlyVersion

  val sleepycat = "com.sleepycat" % "je" % "4.0.92"

  val apachenet   = "commons-net"   % "commons-net"   % "2.0"
  val apachecodec = "commons-codec" % "commons-codec" % "1.4"

  val scalatest = "org.scalatest" %% "scalatest" % "3.2.17"
}
```

### 2.3 자동 플러그인 — project/ShellPromptPlugin.scala

커스텀 태스크나 커맨드를 구현하고 싶을 때는 원오프(one-off) `AutoPlugin`으로 빌드를 정리할 수 있다. 아래 예제는 현재 프로젝트 이름과 git 브랜치를 셸 프롬프트에 표시한다.

```scala
import sbt._
import Keys._
import scala.sys.process._

// Shell prompt which show the current project and git branch
object ShellPromptPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override lazy val projectSettings = Seq(
    shellPrompt := buildShellPrompt
  )
  val devnull: ProcessLogger = new ProcessLogger {
    def out(s: => String): Unit = {}
    def err(s: => String): Unit = {}
    def buffer[T] (f: => T): T = f
  }
  def currBranch =
    ("git status -sb" lineStream_! devnull headOption)
      .getOrElse("-").stripPrefix("## ")
  val buildShellPrompt: State => String = {
    case (state: State) =>
      val currProject = Project.extract (state).currentProject.id
      s"""$currProject:$currBranch> """
  }
}
```

`trigger = allRequirements`로 지정했기 때문에 이 플러그인은 별도 `enablePlugins` 없이 빌드의 모든 프로젝트에 자동으로 적용된다.

### 2.4 build.sbt — 분리한 파일 조합

`project/*.scala`에서 정의한 객체를 `import`해서 여러 서브프로젝트에 공통 설정과 의존성을 나눠 적용한다.

```scala
import Resolvers._
import Dependencies._

// factor out common settings into a sequence
lazy val buildSettings = Seq(
  organization := "com.example",
  version := "0.1.0",
  scalaVersion := "2.12.18"
)

// Sub-project specific dependencies
lazy val commonDeps = Seq(
  logbackcore,
  logbackclassic,
  jacksonjson,
  scalatest % Test
)

lazy val serverDeps = Seq(
  grizzlyframwork,
  grizzlyhttp,
  grizzlyrcm,
  grizzlyutils,
  grizzlyportunif,
  sleepycat,
  scalatest % Test
)

lazy val pricingDeps = Seq(
  apachenet,
  apachecodec,
  scalatest % Test
)

lazy val cdap2 = (project in file("."))
  .aggregate(common, server, compact, pricing, pricing_service)
  .settings(buildSettings)

lazy val common = (project in file("cdap2-common"))
  .settings(
    buildSettings,
    libraryDependencies ++= commonDeps
  )

lazy val server = (project in file("cdap2-server"))
  .dependsOn(common)
  .settings(
    buildSettings,
    resolvers := oracleResolvers,
    libraryDependencies ++= serverDeps
  )

lazy val pricing = (project in file("cdap2-pricing"))
  .dependsOn(common, compact, server)
  .settings(
    buildSettings,
    libraryDependencies ++= pricingDeps
  )

lazy val pricing_service = (project in file("cdap2-pricing-service"))
  .dependsOn(pricing, server)
  .settings(buildSettings)

lazy val compatct = (project in file("compact-hashmap"))
  .settings(buildSettings)
```

### 2.5 구조 정리

| 파일 | 역할 |
|---|---|
| `project/Resolvers.scala` | 저장소(Resolver) 목록 중앙화 |
| `project/Dependencies.scala` | 라이브러리 버전과 ModuleID 중앙화 |
| `project/ShellPromptPlugin.scala` | 원오프 AutoPlugin으로 커스텀 태스크/설정 구현 |
| `build.sbt` | 위 정의들을 import해 서브프로젝트별로 조합 |

이 구조는 `buildSettings`처럼 공통 설정을 시퀀스로 뽑아 여러 프로젝트의 `.settings(...)`에 재사용하는 것이 핵심이며, 서브프로젝트 수가 늘어날수록 의존성/저장소 선언 중복을 줄이는 효과가 커진다.

---

## 3. 고급 구성(configuration) 예제

sbt의 "configuration"은 Ivy의 구성 개념을 그대로 가져온 것으로, 하나의 모듈이 제공하는 의존성을 여러 그룹으로 나눠 선택적으로 노출하는 데 쓰인다. 이 예제는 별도 유틸리티 모듈을 여러 개 만들지 않고도, 하나의 `utils` 모듈에서 필요한 기능만 골라 의존하게 만드는 방법을 보여준다.

### 3.1 문제 상황

`utils` 모듈이 Scalate 관련 유틸리티와 Saxon 관련 유틸리티를 모두 제공한다고 하면, 컴파일 클래스패스에는 두 라이브러리가 전부 올라가야 한다. 하지만 `utils`를 사용하는 프로젝트 `a`가 Scalate 기능만 필요하다면, Saxon 의존성까지 전이적으로 끌려오는 것은 불필요하다.

### 3.2 커스텀 구성 정의

`config(...)`로 새 구성을 만들고 `.extend(...)`로 상속 관계를 지정한다.

```scala
// Custom configurations
lazy val Common = config("common").describedAs("Dependencies required in all configurations.")
lazy val Scalate = config("scalate").extend(Common).describedAs("Dependencies for using Scalate utilities.")
lazy val Saxon = config("saxon").extend(Common).describedAs("Dependencies for using Saxon utilities.")

// Define a customized compile configuration that includes
// dependencies defined in our other custom configurations
lazy val CustomCompile = config("compile").extend(Saxon, Common, Scalate)

// factor out common settings
ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.12.18"
ThisBuild / version      := "0.1.0-SNAPSHOT"
```

### 3.3 프로젝트별 부분 의존

프로젝트는 `%` 연산자에 구성 매핑 문자열(`"compile->scalate"` 등)을 넘겨, `utils`가 제공하는 여러 구성 중 필요한 것만 선택해 의존할 수 있다.

```scala
// An example project that only uses the Scalate utilities.
lazy val a = (project in file("a"))
  .dependsOn(utils % "compile->scalate")

// An example project that uses the Scalate and Saxon utilities.
// For the configurations defined here, this is equivalent to doing dependsOn(utils),
//  but if there were more configurations, it would select only the Scalate and Saxon
//  dependencies.
lazy val b = (project in file("b"))
  .dependsOn(utils % "compile->scalate,saxon")
```

### 3.4 utils 모듈 설정

`utils` 프로젝트 자체는 `inConfig`로 `common` 구성 전용 컴파일 설정(예: `src/common/scala`)을 추가하고, 각 라이브러리를 알맞은 구성에 배정한다.

```scala
// Defines the utilities project
lazy val utils = (project in file("utils"))
  .settings(
    inConfig(Common)(Defaults.configSettings),  // Add the src/common/scala/ compilation configuration.
    addArtifact(Common / packageBin / artifact, Common / packageBin), // Publish the common artifact

    // We want our Common sources to have access to all of the dependencies on the classpaths
    //   for compile and test, but when depended on, it should only require dependencies in 'common'
    Common / classpathConfiguration := CustomCompile,

    // Modify the default Ivy configurations.
    // 'overrideConfigs' ensures that Compile is replaced by CustomCompile
    ivyConfigurations := overrideConfigs(Scalate, Saxon, Common, CustomCompile)(ivyConfigurations.value),

    // Put all dependencies without an explicit configuration into Common (optional)
    defaultConfiguration := Some(Common),

    // Declare dependencies in the appropriate configurations
    libraryDependencies ++= Seq(
       "org.fusesource.scalate" % "scalate-core" % "1.5.0" % Scalate,
       "org.squeryl" %% "squeryl" % "0.9.5-6" % Scalate,
       "net.sf.saxon" % "saxon" % "8.7" % Saxon
    )
  )
```

### 3.5 핵심 개념 정리

| 요소 | 역할 |
|---|---|
| `config("이름")` | 새 Ivy 구성 생성 |
| `.extend(other)` | 다른 구성의 의존성을 상속 |
| `ivyConfigurations := overrideConfigs(...)` | 기본 구성 집합을 커스텀 구성으로 교체 |
| `defaultConfiguration := Some(Common)` | 구성을 명시하지 않은 의존성의 기본 귀속처 지정 |
| `dependsOn(module % "compile->scalate")` | 의존 대상 모듈의 특정 구성만 선택적으로 참조 |
| `Common / classpathConfiguration` | 특정 구성이 참조할 클래스패스 구성 지정 |

이 방식은 다수의 유틸리티 jar를 따로 만들지 않고도, 단일 모듈에서 여러 기능 조합을 세분화해 제공하고 싶을 때 쓸 수 있는 대안이다.

---

## 4. 고급 커맨드 예제

이 예제는 sbt 설정 시스템의 확장성을 보여주는 고급 사례로, 빌드에 선언된 모든 의존성을 어디서 정의됐는지와 무관하게 일괄 수정하는 커맨드를 구현한다. `Project`가 아니라 빌드 전체에서 만들어진 최종 `Seq[Setting[_]]`을 직접 조작한다는 점이 특징이다.

### 4.1 동작 방식

- `canonicalize` 커맨드를 실행하면 변경 사항이 적용된다.
- `reload`를 하거나 `set`으로 설정을 다시 지정하면 변경 사항이 되돌아가며, 다시 적용하려면 `canonicalize`를 재실행해야 한다.
- 이 예제는 선언된 모든 ScalaCheck 의존성을 버전 1.8로 강제 변환하는 것을 보여준다. 같은 방식으로 다른 의존성, 저장소, `scalacOptions` 등을 변환하거나 설정을 추가/삭제하는 것도 가능하다.
- `Project`의 설정에 대해서만 직접 적용할 수도 있지만, 그렇게 하면 플러그인이나 `build.sbt`에서 자동으로 추가된 설정은 빠지게 된다. 이 예제는 외부 빌드를 포함한 모든 빌드, 모든 프로젝트의 모든 설정에 무조건 적용하는 방법을 보여준다.

### 4.2 구현

```scala
import sbt._
import Keys._

object Canon extends AutoPlugin {
  // Registers the canonicalize command in every project
  override def trigger = allRequirements
  override def projectSettings = Seq(commands += canonicalize)

  // Define the command.  This takes the existing settings (including any session settings)
  // and applies 'f' to each Setting[_]
  def canonicalize = Command.command("canonicalize") { (state: State) =>
    val extracted = Project.extract(state)
    import extracted._
    val transformed = session.mergeSettings map ( s => f(s) )
    appendWithSession(transformed, state)
  }

  // Transforms a Setting[_].
  def f(s: Setting[_]): Setting[_] = s.key.key match {
    // transform all settings that modify libraryDependencies
    case Keys.libraryDependencies.key =>
      // hey scalac.  T == Seq[ModuleID]
      s.asInstanceOf[Setting[Seq[ModuleID]]].mapInit(mapLibraryDependencies)
      // preserve other settings
    case _ => s
  }
  // This must be idempotent because it gets applied after every transformation.
  // That is, if the user does:
  //  libraryDependencies += a
  //  libraryDependencies += b
  // then this method will be called for Seq(a) and Seq(a,b)
  def mapLibraryDependencies(key: ScopedKey[Seq[ModuleID]], value: Seq[ModuleID]): Seq[ModuleID] =
    value map mapSingle

  // This is the fundamental transformation.
  // Here we map all declared ScalaCheck dependencies to be version 1.8
  def mapSingle(module: ModuleID): ModuleID =
    if(module.name == "scalacheck") module.withRevision(revision = "1.8")
    else module
}
```

### 4.3 핵심 개념 정리

| 요소 | 역할 |
|---|---|
| `override def trigger = allRequirements` | 이 AutoPlugin을 모든 프로젝트에 자동 적용 |
| `Command.command("이름") { state => ... }` | `State`를 받아 새 `State`를 반환하는 커맨드 정의 |
| `Project.extract(state).session.mergeSettings` | 세션에서 병합된 전체 `Setting[_]` 목록을 얻음 |
| `Setting#mapInit(f)` | 설정의 초기값 계산 함수를 감싸 변환 로직 삽입 |
| `appendWithSession(transformed, state)` | 변환된 설정을 현재 세션에 반영해 새 `State` 생성 |
| `mapLibraryDependencies`의 멱등성 | 설정이 누적 적용(`+=`)될 때마다 매번 호출되므로 반드시 멱등이어야 함 |

이 패턴은 특정 프로젝트나 특정 설정 파일에 국한되지 않고, 빌드 그래프 전체의 모든 설정에 일괄적으로 개입해야 하는 상황(의존성 버전 강제 통일, 전역 컴파일 옵션 주입 등)에 응용할 수 있다.

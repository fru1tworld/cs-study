# sbt 파일 추적, 메모리 트러블슈팅, 커스텀 의존성 구성, 커스텀 태스크

> 원본: https://www.scala-sbt.org/1.x/docs/Howto-Track-File-Inputs-and-Outputs.html, https://www.scala-sbt.org/1.x/docs/Troubleshoot-Memory-Issues.html, https://www.scala-sbt.org/1.x/docs/Custom-Dependency-Configuration.html, https://www.scala-sbt.org/1.x/docs/Define+Custom+Tasks.html

---

## 목차

1. [파일 입출력 추적 개요](#1-파일-입출력-추적-개요)
2. [fileInputs와 자동 생성되는 allInputFiles](#2-fileinputs와-자동-생성되는-allinputfiles)
3. [inputFileChanges로 증분 빌드 구현하기](#3-inputfilechanges로-증분-빌드-구현하기)
4. [fileOutputs와 allOutputFiles](#4-fileoutputs와-alloutputfiles)
5. [필터: fileInput/OutputInclude/ExcludeFilter](#5-필터-fileinputoutputincludeexcludefilter)
6. [출력 정리(clean)와 FileStamp](#6-출력-정리clean와-filestamp)
7. [연속 빌드와의 연동, 부분 파이프라인 오류 처리](#7-연속-빌드와의-연동-부분-파이프라인-오류-처리)
8. [메모리 부족 문제 해결](#8-메모리-부족-문제-해결)
9. [메모리 누수 진단](#9-메모리-누수-진단)
10. [커스텀 의존성 구성이란](#10-커스텀-의존성-구성이란)
11. [커스텀 구성 정의 시 주의사항](#11-커스텀-구성-정의-시-주의사항)
12. [기본 커스텀 구성 예제](#12-기본-커스텀-구성-예제)
13. [샌드박스 구성 패턴](#13-샌드박스-구성-패턴)
14. [커스텀 태스크 키 정의하기](#14-커스텀-태스크-키-정의하기)

---

## 1. 파일 입출력 추적 개요

sbt의 많은 태스크는 파일 집합에 의존한다. 예를 들어 `package` 태스크는 `compile` 태스크가 생성한 클래스 파일과 리소스를 모아 jar로 묶는다. sbt 1.3.0부터는 태스크의 파일 입력과 출력을 추적하는 파일 관리 시스템이 제공되어, 태스크가 마지막 실행 이후 어떤 파일이 바뀌었는지 스스로 조회하고 변경된 파일만 다시 빌드할 수 있다. 이 시스템은 [Triggered Execution](https://www.scala-sbt.org/1.x/docs/Triggered-Execution.html)(`~` 연속 빌드)과도 통합되어, 태스크의 파일 의존성이 자동으로 모니터링 대상이 된다.

이 개념을 보여주는 대표 예제는 gcc로 C 소스를 컴파일해 공유 라이브러리를 만드는 빌드다. 두 태스크로 구성된다.

- `buildObjects`: `*.c` 소스 파일을 객체 파일(`*.o`)로 컴파일
- `linkLibrary`: 객체 파일들을 링크해 공유 라이브러리 생성

```scala
import java.nio.file.Path
val buildObjects = taskKey[Seq[Path]]("Compiles c files into object files.")
val linkLibrary = taskKey[Path]("Links objects into a shared library.")
```

`buildObjects`는 `*.c` 소스 입력에 의존하고, `linkLibrary`는 `buildObjects`가 생성한 `*.o` 출력에 의존한다. 소스가 바뀌지 않으면 컴파일도 링크도 다시 일어나지 않아야 하고, 소스가 바뀌면 변경된 파일만 다시 컴파일한 뒤 링크까지 다시 수행해야 한다.

## 2. fileInputs와 자동 생성되는 allInputFiles

태스크가 의존하는 입력 파일은 `fileInputs` 키로 지정한다. 타입은 `Seq[Glob]`이며, 여러 디렉터리나 여러 파일 종류를 함께 지정할 수 있도록 시퀀스로 받는다.

`fileInputs`를 특정 스코프에 설정하면 sbt는 그 스코프에 `allInputFiles`라는 태스크를 자동으로 만들어 준다. 이 태스크는 `fileInputs`의 Glob 패턴에 매칭되는 모든 파일을 `Seq[Path]`로 반환한다. 편의상 `foo.inputFiles`라고 쓰면 `(foo / allInputFiles).value`로 해석되는 확장 메서드도 제공된다.

```scala
import scala.sys.process._
import java.nio.file.{ Files, Path }
import sbt.nio._
import sbt.nio.Keys._

val buildObjects = taskKey[Seq[Path]]("Compiles c files into object files.")
buildObjects / fileInputs += baseDirectory.value.toGlob / "src" / "*.c"
buildObjects := {
  val outputDir = Files.createDirectories(streams.value.cacheDirectory.toPath)
  def outputPath(path: Path): Path =
    outputDir / path.getFileName.toString.replaceAll(".c$", ".o")
  val logger = streams.value.log
  buildObjects.inputFiles.map { path =>
    val output = outputPath(path)
    logger.info(s"Compiling $path to $output")
    Seq("gcc", "-c", path.toString, "-o", output.toString).!!
    output
  }
}
```

이 구현은 `src` 디렉터리 아래 `*.c` 파일을 모두 모아 gcc로 컴파일한다. 다만 이 상태에서는 `buildObjects`를 실행할 때마다 모든 소스 파일을 매번 다시 컴파일하므로, 소스 파일 수가 늘어나면 비효율이 커진다. sbt는 `fileInputs`로 지정된 Glob에 매칭되는 모든 파일을 자동으로 감시하기 때문에, 연속 빌드 모드에서는 `src` 아래 `*.c` 파일 중 하나만 바뀌어도 다시 빌드가 트리거된다.

## 3. inputFileChanges로 증분 빌드 구현하기

`fileInputs`만으로는 매 실행마다 전체 재컴파일이 일어난다. 여기에 `inputFileChanges` API를 추가하면 마지막으로 성공한 실행 이후 어떤 소스가 바뀌었는지 알 수 있어 진짜 증분 빌드를 만들 수 있다.

```scala
import scala.sys.process._
import java.nio.file.{ Files, Path }
import sbt.nio._
import sbt.nio.Keys._

val buildObjects = taskKey[Seq[Path]]("Generate object files from c sources")
buildObjects / fileInputs += baseDirectory.value.toGlob / "src" / "*.c"
buildObjects := {
  val outputDir = Files.createDirectories(streams.value.cacheDirectory.toPath)
  val logger = streams.value.log
  def outputPath(path: Path): Path =
    outputDir / path.getFileName.toString.replaceAll(".c$", ".o")
  def compile(path: Path): Path = {
    val output = outputPath(path)
    logger.info(s"Compiling $path to $output")
    Seq("gcc", "-fPIC", "-std=gnu99", "-c", s"$path", "-o", s"$output").!!
    output
  }
  val sourceMap = buildObjects.inputFiles.view.map(p => outputPath(p) -> p).toMap
  val existingTargets = fileTreeView.value.list(outputDir.toGlob / **).flatMap { case (p, _) =>
    if (!sourceMap.contains(p)) {
      Files.deleteIfExists(p)
      None
    } else {
      Some(p)
    }
  }.toSet
  val changes = buildObjects.inputFileChanges
  val updatedPaths = (changes.created ++ changes.modified).toSet
  val needCompile = updatedPaths ++ sourceMap.filterKeys(!existingTargets(_)).values
  needCompile.foreach(compile)
  sourceMap.keys.toVector
}
```

`inputFileChanges`가 반환하는 `FileChangeReport`는 봉인된(sealed) 트레이트이며 다음 세 가지 케이스 클래스로 구현된다.

| 케이스 | 의미 |
|---|---|
| `Changes` | 하나 이상의 소스 파일이 수정됨 |
| `Unmodified` | 마지막 실행 이후 변경 없음 |
| `Fresh` | 이전 실행의 캐시 항목 자체가 없음 |

`FileChanges`를 패턴 매칭으로 다루면 다음처럼 생성/삭제/수정/미변경 파일을 각각 구분해서 처리할 수 있다.

```scala
foo.inputFileChanges match {
  case FileChanges(created, deleted, modified, unmodified)
    if created.nonEmpty || modified.nonEmpty =>
      build(created ++ modified)
      delete(deleted)
  case _ => // no changes
}
```

입력 변경 리포트는 출력에 대해서는 아무것도 말해 주지 않는다. 그래서 위 `buildObjects` 구현은 출력 디렉터리를 직접 조회해서 어떤 객체 파일이 이미 존재하는지 확인하는 방식으로 처리한다. 이 예제는 입력과 출력이 1:1로 매핑되지만, 항상 그런 것은 아니다. 예를 들어 헤더 파일을 `fileInputs`에 포함시키면, 헤더 파일 자체는 컴파일 대상이 아니지만 변경 시 하나 이상의 `*.c` 소스를 재컴파일하도록 트리거하는 역할만 할 수도 있다.

`buildObjects.inputFileChanges`를 호출하면 `buildObjects / fileInputs`가 연속 빌드에서 자동으로 감시 대상이 된다는 점도 기억해 둘 만하다.

## 4. fileOutputs와 allOutputFiles

파일 출력을 지정하는 가장 쉬운 방법은 태스크의 반환값을 그대로 활용하는 것이다. sbt는 태스크의 반환 타입이 `Path`, `Seq[Path]`, `File`, `Seq[File]` 중 하나면 그 결과를 자동으로 출력 파일로 추적한다.

```scala
val linkLibrary = taskKey[Path]("Links objects into a shared library.")
linkLibrary := {
  val outputDir = Files.createDirectories(streams.value.cacheDirectory.toPath)
  val logger = streams.value.log
  val isMac = scala.util.Properties.isMac
  val library = outputDir / s"mylib.${if (isMac) "dylib" else "so"}"
  val linkOpts = if (isMac) Seq("-dynamiclib") else Seq("-shared", "-fPIC")
  if (buildObjects.outputFileChanges.hasChanges || !Files.exists(library)) {
    logger.info(s"Linking $library")
    (Seq("gcc") ++ linkOpts ++ Seq("-o", s"$library") ++
      buildObjects.outputFiles.map(_.toString)).!!
  } else {
    logger.debug(s"Skipping linking of $library")
  }
  library
}
```

링크 작업은 증분으로 나눌 수 없는 작업이라 로직이 더 단순하다. `buildObjects`의 출력 중 하나라도 바뀌었거나 라이브러리 파일 자체가 없으면 다시 링크하고, 그렇지 않으면 건너뛴다.

출력 패턴이 미리 알려진 경우에는 `fileOutputs` 키로 직접 지정할 수도 있다. 외부 도구가 입력과 출력의 매핑 관계를 알려주지 않는 블랙박스일 때 특히 유용하다.

```scala
val buildObjects = taskKey[Unit]("Compiles c files into object files.")
buildObjects / fileOutputs := target.value / "objects" / ** / "*.o"
```

`fileInputs`와 마찬가지로, 태스크 `foo`의 반환 타입이 파일 관련 타입이거나 `foo / fileOutputs`가 지정되어 있으면 `allOutputFiles` 태스크가 자동 생성된다. 둘 다 지정된 경우 `allOutputFiles`의 결과는 태스크가 반환한 파일과 `fileOutputs`가 기술한 파일의 합집합(중복 제거)이다. `foo.outputFiles`는 `(foo / allOutputFiles).value`의 축약형이다.

## 5. 필터: fileInput/OutputInclude/ExcludeFilter

`fileInputs`/`fileOutputs`의 Glob 패턴 이외에 추가로 걸러내고 싶을 때는 [sbt.nio.file.PathFilter](https://www.scala-sbt.org/1.x/docs/Globs.html#path-filters) 타입의 네 가지 설정을 쓴다.

| 설정 | 역할 | 기본값 |
|---|---|---|
| `fileInputIncludeFilter` | 이 필터에도 매칭되는 입력 파일만 포함 | `AllPassFilter.toNio` |
| `fileInputExcludeFilter` | 이 필터에 매칭되는 입력 파일 제외 | `HiddenFileFilter.toNio \|\| DirectoryFilter` |
| `fileOutputIncludeFilter` | 이 필터에도 매칭되는 출력 파일만 포함 | `AllPassFilter.toNio` |
| `fileOutputExcludeFilter` | 이 필터에 매칭되는 출력 파일 제외 | `NothingFilter.toNio` |

이름에 test가 들어간 파일을 `buildObjects`에서 제외하고 싶다면 다음처럼 쓴다.

```scala
buildObjects / fileInputExcludeFilter := "*test*"
```

기본으로 걸려 있는 숨김 파일/디렉터리 제외 규칙을 유지한 채로 조건을 추가하려면 아래 두 방식 중 하나를 쓴다.

```scala
buildObjects / fileInputExcludeFilter :=
  (buildObjects / fileInputExcludeFilter).value || "*test*"
```

```scala
buildObjects / fileInputExcludeFilter ~= { ef => ef || "*test*" }
```

대부분의 경우 `fileInputIncludeFilter`까지 손댈 필요는 없다. 경로 이름 필터링은 `fileInputs` 자체에서 이미 처리되기 때문이다. 출력 필터도 일반적으로는 건드릴 필요가 없다.

## 6. 출력 정리(clean)와 FileStamp

sbt는 `allOutputFiles`가 생성되는 태스크 `foo`에 대해 `foo / clean`도 함께 자동 생성한다. `foo / clean`을 실행하면 `foo`가 이전에 생성한 모든 파일이 삭제되며, `foo` 자체를 재평가하지는 않는다. 예를 들어 `buildObjects / clean`은 이전 `buildObjects` 실행이 만든 객체 파일들을 지운다. 이 clean 태스크는 전이적이지 않다. `linkLibrary / clean`을 호출하면 공유 라이브러리는 지워지지만 `buildObjects`가 만든 객체 파일은 지워지지 않는다.

sbt가 추적하는 각 입력/출력 파일에는 `FileStamp`가 연결된다. FileStamp는 파일의 마지막 수정 시각이거나 해시값일 수 있다. 기본값은 입력은 해시, 출력은 마지막 수정 시각이다. 이 기본 동작은 `inputFileStamper`/`outputFileStamper` 설정으로 바꿀 수 있다.

```scala
val generateSources = taskKey[Seq[Path]]("Generates source files from json schema.")
generateSources / fileInputs := baseDirectory.value.toGlob / "schema" / ** / "*.json"
generateSources / outputFileStamper := FileStamper.Hash
```

## 7. 연속 빌드와의 연동, 부분 파이프라인 오류 처리

`~bar` 형태의 연속 빌드에서, `bar` 안에서 다른 태스크 `foo`의 `foo.inputFiles`나 `foo.inputFileChanges`를 호출하면 `foo / fileInputs`에 지정된 모든 Glob이 자동으로 감시 대상이 된다. 이 감시는 전이적으로 적용되어, `~linkLibrary`를 실행하면 `linkLibrary`가 의존하는 `buildObjects`의 `*.c` 소스 파일까지 함께 감시된다.

입력 파일은 해시가 바뀌어야만 재빌드를 트리거한다. 이 동작은 다음 설정으로 무조건 트리거하도록 바꿀 수 있다.

```scala
Global / watchForceTriggerOnAnyChange := true
```

출력 파일의 변경(`foo.outputFiles`나 `foo.outputFileChanges`로 조회하는 값)은 재빌드를 트리거하지 않는다.

파일 스탬프는 태스크 단위로 추적되며, 해당 증분 태스크 자신이 성공했을 때만 갱신된다. 위 예제에서는 `linkLibrary`가 성공했을 때만 `buildObjects` 출력의 마지막 수정 시각이 갱신되어 저장된다. 즉 `linkLibrary`를 호출하지 않고 `buildObjects`만 여러 번 실행해도 되고, 다음에 `linkLibrary`가 실행되면 그동안 누적된 `buildObjects` 출력 변경 사항을 한 번에 인식한다. 반대로 `linkLibrary`가 실패하면 `buildObjects` 출력에 대한 스탬프 갱신도 건너뛴다. 어떤 파일까지 성공적으로 처리됐는지 일반적으로 알 수 없기 때문이다.

## 8. 메모리 부족 문제 해결

sbt는 서브프로젝트 개수와 활성화된 플러그인 수에 따라 메모리를 많이 요구할 수 있고, 이 때문에 메모리 부족으로 크래시가 나거나 성능이 크게 떨어지기도 한다. 기본 JVM 힙 크기는 1GB다. 메모리 발자국이 큰 프로젝트라면 힙 크기를 늘려서 sbt를 시작해야 할 수 있다.

힙을 2GB로 늘리려면 다음처럼 실행한다.

```bash
sbt -J-Xmx2G
```

`-J`로 시작하는 커맨드 인자는 모두 JVM 인자로 해석된다. 매번 옵션을 붙이지 않고 프로젝트 차원에서 항상 2GB로 늘리고 싶다면 `.sbtopts` 파일을 만들거나 편집해서 다음 줄을 추가하면 된다.

```
-J-Xmx2G
```

sbt를 대화형 모드나 서버 모드(`sbt --client` 또는 `sbtn`으로 시작한 경우)로 실행할 때는, 빌드 안의 각 태스크가 자신이 사용한 자원을 반드시 정리해야 한다. 그러지 않으면 sbt 프로세스의 메모리 사용량이 시간이 지날수록 계속 늘어날 수 있다. 예를 들어 `run` 태스크가 Akka `ActorSystem`을 시작한다면, `run`이 끝나기 전에 그 `ActorSystem`을 shutdown해야 한다. 그렇지 않으면 `run`을 호출할 때마다 sbt 프로세스의 메모리 사용량이 누적된다.

## 9. 메모리 누수 진단

메모리 누수를 고치려면 예상보다 오래 메모리에 남아 있는 객체가 어떤 클래스인지 먼저 찾아야 한다. 가장 쉬운 방법은 JDK가 제공하는 `jmap` 명령과 VisualVM 같은 JVM 메모리 분석 도구를 함께 쓰는 것이다.

1. `ps` 명령으로 디버깅하려는 sbt 프로세스의 PID를 찾는다.
2. 다음 명령으로 힙 덤프를 뜬다.
   ```bash
   jmap -dump:format=b,file=leak.hprof $SBT_PID
   ```
3. 생성된 `leak.hprof` 파일을 VisualVM에서 연다.

어떤 클래스가 메모리를 가장 많이 차지하는지 한눈에 보일 때도 있지만, 그렇지 않으면 "Compute Retained Sizes" 버튼을 눌러야 한다. 힙이 크면 이 계산에 시간이 걸릴 수 있지만, 메모리를 가장 많이 차지하는 클래스를 정확히 짚어낼 수 있다. 이 과정을 통해 누수가 발생한 스레드나 정리되지 않은 캐시의 위치를 찾아내는 데 도움이 된다.

## 10. 커스텀 의존성 구성이란

의존성 구성(dependency configuration, 줄여서 구성)은 라이브러리 의존성 그래프를 정의하며, 자체 클래스패스·소스·생성 패키지 등을 가질 수 있다. 이 개념은 sbt가 관리 의존성에 예전에 사용했던 Ivy와 [Maven의 스코프](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Dependency_Scope)에서 유래했다.

sbt에서 흔히 볼 수 있는 구성은 다음과 같다.

| 구성 | 역할 |
|---|---|
| `Compile` | 메인 빌드 정의(`src/main/scala`) |
| `Test` | 테스트 빌드 정의(`src/test/scala`) |
| `Runtime` | `run` 태스크의 클래스패스 |

## 11. 커스텀 구성 정의 시 주의사항

커스텀 구성은 `Test`처럼 새로운 소스 코드 집합이나 독자적인 라이브러리 의존성을 도입할 때만 고려해야 한다. 단순히 키를 네임스페이스로 구분하려는 목적으로 구성을 도입하는 것은 좋지 않다.

커스텀 구성에는 다음과 같은 단점이 있다.

- 사용자가 스코프 개념의 복잡도에 혼란을 겪는다. 서브프로젝트와 태스크 개념에는 익숙해도, 여기에 구성 스코프까지 얽히면 이해하기 어려워진다.
- sbt 자체의 지원이 제한적이다. 어떤 구성이 다른 구성을 `extend`한다고 선언할 수는 있지만, 설정(setting)의 상속은 이루어지지 않는다. 필요한 설정과 태스크를 전부 직접 채워 넣어야 한다.
- 이 때문에 sbt에 새 기능이 추가되어도 커스텀 구성까지 그 기능을 지원하지 못할 가능성이 크다. 서드파티 플러그인도 마찬가지로 커스텀 구성을 잘 지원하지 않는 경우가 많다.

## 12. 기본 커스텀 구성 예제

플러그인 안에서 `config()` 함수로 새 구성을 만들고, `inConfig()`로 그 구성 스코프에 표준 설정을 적용한다.

```scala
// project/FuzzPlugin.scala
package com.example.sbtfuzz

import sbt._

object FuzzPlugin extends AutoPlugin {
  object autoImport {
    lazy val Fuzz = config("fuzz")
  }
  import autoImport._
  override lazy val projectSettings =
    inConfig(Fuzz)(Defaults.configSettings)
}
```

`Defaults.configSettings`는 `Compile`/`Test` 같은 표준 구성이 갖는 소스 디렉터리, 컴파일, 패키징 관련 설정 일체를 그대로 제공한다. 정의한 플러그인은 build.sbt에서 `.configs(...)`로 구성을 등록하고 `.enablePlugins(...)`으로 활성화한다.

```scala
// build.sbt
ThisBuild / scalaVersion     := "2.13.4"
ThisBuild / version          := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .configs(Fuzz)
  .enablePlugins(FuzzPlugin, ScalafmtCliPlugin)
  .settings(
    name := "use",
  )
```

## 13. 샌드박스 구성 패턴

구성을 활용하는 또 다른 유용한 기법은, 사용자 프로젝트에 부가적인 의존성 그래프를 하나 더 추가해서 Coursier가 별도의 jar를 내려받게 하고 그 jar를 태스크가 실행하도록 만드는 것이다. 이를 샌드박스 구성이라고 부른다. Scala 2.13 CLI 버전의 scalafmt를 프로젝트 안에서 실행하는 용도로 활용할 수 있다. sbt 1.4.x 기준으로는 샌드박스 구성이 사용자 서브프로젝트와 동일한 Scala 버전을 써야 한다는 제약이 있다.

```scala
// project/ScalafmtPlugin.scala
package com.example

import sbt._
import Keys._

object ScalafmtCliPlugin extends AutoPlugin {
  object autoImport {
    lazy val ScalafmtSandbox = config("scalafmt").hide
    lazy val scalafmt = inputKey[Unit]("")
  }
  import autoImport._
  override lazy val projectSettings = Seq(
    ivyConfigurations += ScalafmtSandbox,
    libraryDependencies += "org.scalameta" %% "scalafmt-cli" % "2.7.5" % ScalafmtSandbox,
    scalafmt := (ScalafmtSandbox / run).evaluated
  ) ++ inConfig(ScalafmtSandbox)(
    Seq(
      run := Defaults.runTask(managedClasspath, run / mainClass, run / runner)
        .evaluated,
      managedClasspath := Classpaths.managedJars(
        ScalafmtSandbox,
        classpathTypes.value,
        update.value,
      )
    ) ++
      inTask(run)(
        Seq(
          mainClass := Some("org.scalafmt.cli.Cli"),
          fork := true, // to avoid exit
        ) ++ Defaults.runnerSettings
      )
  )
}
```

핵심 포인트는 다음과 같다.

- `config("scalafmt").hide`: `hide`를 붙이면 이 구성이 퍼블리시 대상 POM/ivy.xml에는 나타나지 않는다. 사용자 프로젝트가 실제로 의존하는 것처럼 노출할 필요가 없는 내부용 구성이기 때문이다.
- `ivyConfigurations += ScalafmtSandbox`: 새 구성을 Ivy(의존성 해석기)에 등록한다.
- `libraryDependencies += ... % ScalafmtSandbox`: `%` 뒤에 구성을 명시하면 해당 의존성이 이 구성 전용 클래스패스로만 들어간다.
- `scalafmt := (ScalafmtSandbox / run).evaluated`: 사용자에게 노출되는 `scalafmt` 인풋 태스크는, 내부적으로 샌드박스 구성 스코프의 `run`을 그대로 호출하는 방식으로 구현된다.

이렇게 만든 `scalafmt` 태스크를 sbt 셸에서 실행하면 다음처럼 동작한다.

```
sbt:custom-configs> scalafmt --version
[info] running (fork) org.scalafmt.cli.Cli --version
[info] scalafmt 2.7.5
[success] Total time: 3 s, completed Feb 8, 2021 12:01:34 AM
sbt:custom-configs> scalafmt
[info] running (fork) org.scalafmt.cli.Cli
[info] Reformatting...
           Reformatting...
[success] Total time: 6 s, completed Feb 8, 2021 12:01:40 AM
```

테스트 전용 구성을 추가하는 방법은 별도로 [Testing 문서의 Additional test configurations](https://www.scala-sbt.org/1.x/docs/Testing.html#additional-test-configurations) 절에서 다룬다.

## 14. 커스텀 태스크 키 정의하기

새 태스크를 만드는 첫 단계는 `taskKey[T]("설명")`로 태스크 키를 선언하는 것이다. 타입 파라미터 `T`는 이 태스크가 반환할 값의 타입이고, 문자열 인자는 `inspect`, 도움말 등에 표시되는 설명이다. 이렇게 선언한 키에 `:=`로 실제 구현을 대입하면 태스크가 완성된다.

예를 들어 3개의 서브프로젝트(`core`, `tools`, `client`)로 이루어진 멀티 프로젝트 빌드에서, `core`와 `tools`의 테스트만 실행하고 `client`는 건너뛰는 커스텀 태스크 `myTestTask`는 다음처럼 정의한다.

```scala
lazy val core = project.in(file("./core"))
lazy val tools = project.in(file("./tools"))
lazy val client = project.in(file("./client"))

lazy val myTestTask = taskKey[Unit]("my test task")

myTestTask := {
  (core / Test / test).value
  (tools / Test / test).value
}
```

여기서 눈여겨볼 점은 다음과 같다.

- `taskKey[Unit](...)`: 반환값이 없는 태스크는 `Unit`으로 선언한다. 값을 반환하는 태스크라면 `taskKey[String]`, `taskKey[Seq[Path]]`처럼 실제 타입을 지정한다.
- `(core / Test / test).value`: 다른 서브프로젝트의 특정 구성 스코프에 있는 태스크를 `프로젝트 / 구성 / 태스크` 형태로 참조하고, `.value`로 그 결과값을 꺼내 쓴다. 이 문법 덕분에 `myTestTask`는 `core`와 `client`는 그대로 둔 채 원하는 서브프로젝트만 골라 테스트를 실행할 수 있다.
- `:=`로 대입되는 블록 자체가 `Def.task`에 해당하는 태스크 본문이며, 그 안에서 다른 태스크의 `.value`를 호출하면 sbt가 자동으로 태스크 그래프의 의존 관계를 구성한다.

이렇게 정의한 `myTestTask`는 이후 sbt 셸에서 `myTestTask`라고 입력하는 것만으로 실행할 수 있고, 필요하다면 build.sbt의 다른 설정에서도 하나의 태스크 키로 참조할 수 있다. 값이 아니라 빌드 시점에 고정되는 설정을 정의하고 싶을 때는 같은 방식으로 `settingKey[T]`를, 커맨드라인 인자를 받는 태스크가 필요할 때는 `inputKey[T]`를 사용한다.

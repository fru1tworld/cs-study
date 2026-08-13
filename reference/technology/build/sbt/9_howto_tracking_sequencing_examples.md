# sbt How-to: 파일 추적, 실행 순서 제어, 빌드 예제

## sbt 파일 추적, 메모리 트러블슈팅, 커스텀 의존성 구성, 커스텀 태스크

> 원본: https://www.scala-sbt.org/1.x/docs/Howto-Track-File-Inputs-and-Outputs.html, https://www.scala-sbt.org/1.x/docs/Troubleshoot-Memory-Issues.html, https://www.scala-sbt.org/1.x/docs/Custom-Dependency-Configuration.html, https://www.scala-sbt.org/1.x/docs/Define+Custom+Tasks.html

---

### 목차

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

### 1. 파일 입출력 추적 개요

- sbt의 많은 태스크는 파일 집합에 의존
  - 예: `package` 태스크는 `compile` 태스크가 생성한 클래스 파일과 리소스를 모아 jar로 묶음
- sbt 1.3.0부터 태스크의 파일 입력과 출력을 추적하는 파일 관리 시스템 제공
  - 태스크가 마지막 실행 이후 어떤 파일이 바뀌었는지 스스로 조회 → 변경된 파일만 다시 빌드 가능
  - [Triggered Execution](https://www.scala-sbt.org/1.x/docs/Triggered-Execution.html)(`~` 연속 빌드)과도 통합 → 태스크의 파일 의존성이 자동으로 모니터링 대상이 됨
- 이 개념을 보여주는 대표 예제: gcc로 C 소스를 컴파일해 공유 라이브러리를 만드는 빌드, 두 태스크로 구성
  - `buildObjects`: `*.c` 소스 파일을 객체 파일(`*.o`)로 컴파일
  - `linkLibrary`: 객체 파일들을 링크해 공유 라이브러리 생성

```scala
import java.nio.file.Path
val buildObjects = taskKey[Seq[Path]]("Compiles c files into object files.")
val linkLibrary = taskKey[Path]("Links objects into a shared library.")
```

- `buildObjects`는 `*.c` 소스 입력에 의존, `linkLibrary`는 `buildObjects`가 생성한 `*.o` 출력에 의존
- 소스가 바뀌지 않으면 컴파일도 링크도 다시 일어나지 않아야 함
- 소스가 바뀌면 변경된 파일만 다시 컴파일한 뒤 링크까지 다시 수행 필요

### 2. fileInputs와 자동 생성되는 allInputFiles

- 태스크가 의존하는 입력 파일은 `fileInputs` 키로 지정
  - 타입은 `Seq[Glob]` → 여러 디렉터리·여러 파일 종류를 함께 지정할 수 있도록 시퀀스로 받음
- `fileInputs`를 특정 스코프에 설정하면 sbt가 그 스코프에 `allInputFiles`라는 태스크를 자동 생성
  - 이 태스크는 `fileInputs`의 Glob 패턴에 매칭되는 모든 파일을 `Seq[Path]`로 반환
  - 편의상 `foo.inputFiles`라고 쓰면 `(foo / allInputFiles).value`로 해석되는 확장 메서드도 제공

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

- 이 구현은 `src` 디렉터리 아래 `*.c` 파일을 모두 모아 gcc로 컴파일
- 다만 이 상태에서는 `buildObjects`를 실행할 때마다 모든 소스 파일을 매번 다시 컴파일 → 소스 파일 수가 늘어나면 비효율 커짐
- sbt는 `fileInputs`로 지정된 Glob에 매칭되는 모든 파일을 자동으로 감시 → 연속 빌드 모드에서는 `src` 아래 `*.c` 파일 중 하나만 바뀌어도 다시 빌드 트리거됨

### 3. inputFileChanges로 증분 빌드 구현하기

- `fileInputs`만으로는 매 실행마다 전체 재컴파일 발생
- 여기에 `inputFileChanges` API를 추가하면 마지막으로 성공한 실행 이후 어떤 소스가 바뀌었는지 알 수 있어 진짜 증분 빌드 구현 가능

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

- `inputFileChanges`가 반환하는 `FileChangeReport`는 봉인된(sealed) 트레이트이며 다음 세 가지 케이스 클래스로 구현됨
  - `Changes`: 하나 이상의 소스 파일이 수정됨
  - `Unmodified`: 마지막 실행 이후 변경 없음
  - `Fresh`: 이전 실행의 캐시 항목 자체가 없음

`FileChanges`를 패턴 매칭으로 다루면 다음처럼 생성·삭제·수정·미변경 파일을 각각 구분해서 처리 가능.

```scala
foo.inputFileChanges match {
  case FileChanges(created, deleted, modified, unmodified)
    if created.nonEmpty || modified.nonEmpty =>
      build(created ++ modified)
      delete(deleted)
  case _ => // no changes
}
```

- 입력 변경 리포트는 출력에 대해서는 아무것도 알려주지 않음
  - 그래서 위 `buildObjects` 구현은 출력 디렉터리를 직접 조회해서 어떤 객체 파일이 이미 존재하는지 확인
  - 이 예제는 입력과 출력이 1:1로 매핑되지만 항상 그런 것은 아님 → 예를 들어 헤더 파일을 `fileInputs`에 포함시키면, 헤더 파일 자체는 컴파일 대상이 아니지만 변경 시 하나 이상의 `*.c` 소스를 재컴파일하도록 트리거하는 역할만 할 수도 있음
- `buildObjects.inputFileChanges`를 호출하면 `buildObjects / fileInputs`가 연속 빌드에서 자동으로 감시 대상이 된다는 점도 기억해 둘 만함

### 4. fileOutputs와 allOutputFiles

- 파일 출력을 지정하는 가장 쉬운 방법은 태스크의 반환값을 그대로 활용하는 것
  - sbt는 태스크의 반환 타입이 `Path`, `Seq[Path]`, `File`, `Seq[File]` 중 하나면 그 결과를 자동으로 출력 파일로 추적

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

- 링크 작업은 증분으로 나눌 수 없는 작업이라 로직이 더 단순
  - `buildObjects`의 출력 중 하나라도 바뀌었거나 라이브러리 파일 자체가 없으면 다시 링크, 그렇지 않으면 건너뜀
- 출력 패턴이 미리 알려진 경우에는 `fileOutputs` 키로 직접 지정 가능
  - 외부 도구가 입력과 출력의 매핑 관계를 알려주지 않는 블랙박스일 때 특히 유용

```scala
val buildObjects = taskKey[Unit]("Compiles c files into object files.")
buildObjects / fileOutputs := target.value / "objects" / ** / "*.o"
```

- `fileInputs`와 마찬가지로, 태스크 `foo`의 반환 타입이 파일 관련 타입이거나 `foo / fileOutputs`가 지정되어 있으면 `allOutputFiles` 태스크 자동 생성
  - 둘 다 지정된 경우 `allOutputFiles`의 결과는 태스크가 반환한 파일과 `fileOutputs`가 기술한 파일의 합집합(중복 제거)
  - `foo.outputFiles`는 `(foo / allOutputFiles).value`의 축약형

### 5. 필터: fileInput/OutputInclude/ExcludeFilter

- `fileInputs`/`fileOutputs`의 Glob 패턴 이외에 추가로 걸러내고 싶을 때는 [sbt.nio.file.PathFilter](https://www.scala-sbt.org/1.x/docs/Globs.html#path-filters) 타입의 네 가지 설정 사용
  - `fileInputIncludeFilter`: 이 필터에도 매칭되는 입력 파일만 포함, 기본값 `AllPassFilter.toNio`
  - `fileInputExcludeFilter`: 이 필터에 매칭되는 입력 파일 제외, 기본값 `HiddenFileFilter.toNio || DirectoryFilter`
  - `fileOutputIncludeFilter`: 이 필터에도 매칭되는 출력 파일만 포함, 기본값 `AllPassFilter.toNio`
  - `fileOutputExcludeFilter`: 이 필터에 매칭되는 출력 파일 제외, 기본값 `NothingFilter.toNio`

이름에 test가 들어간 파일을 `buildObjects`에서 제외하려면 다음처럼 작성.

```scala
buildObjects / fileInputExcludeFilter := "*test*"
```

기본으로 걸려 있는 숨김 파일/디렉터리 제외 규칙을 유지한 채로 조건을 추가하려면 아래 두 방식 중 하나 사용.

```scala
buildObjects / fileInputExcludeFilter :=
  (buildObjects / fileInputExcludeFilter).value || "*test*"
```

```scala
buildObjects / fileInputExcludeFilter ~= { ef => ef || "*test*" }
```

- 대부분의 경우 `fileInputIncludeFilter`까지 손댈 필요는 없음 → 경로 이름 필터링은 `fileInputs` 자체에서 이미 처리됨
- 출력 필터도 일반적으로는 건드릴 필요 없음

### 6. 출력 정리(clean)와 FileStamp

- sbt는 `allOutputFiles`가 생성되는 태스크 `foo`에 대해 `foo / clean`도 함께 자동 생성
  - `foo / clean`을 실행하면 `foo`가 이전에 생성한 모든 파일이 삭제됨, `foo` 자체를 재평가하지는 않음
  - 예: `buildObjects / clean`은 이전 `buildObjects` 실행이 만든 객체 파일들을 지움
  - 이 clean 태스크는 전이적이지 않음 → `linkLibrary / clean`을 호출하면 공유 라이브러리는 지워지지만 `buildObjects`가 만든 객체 파일은 지워지지 않음
- sbt가 추적하는 각 입력/출력 파일에는 `FileStamp`가 연결됨
  - FileStamp는 파일의 마지막 수정 시각이거나 해시값일 수 있음
  - 기본값은 입력은 해시, 출력은 마지막 수정 시각
  - 이 기본 동작은 `inputFileStamper`/`outputFileStamper` 설정으로 변경 가능

```scala
val generateSources = taskKey[Seq[Path]]("Generates source files from json schema.")
generateSources / fileInputs := baseDirectory.value.toGlob / "schema" / ** / "*.json"
generateSources / outputFileStamper := FileStamper.Hash
```

### 7. 연속 빌드와의 연동, 부분 파이프라인 오류 처리

- `~bar` 형태의 연속 빌드에서, `bar` 안에서 다른 태스크 `foo`의 `foo.inputFiles`나 `foo.inputFileChanges`를 호출하면 `foo / fileInputs`에 지정된 모든 Glob이 자동으로 감시 대상이 됨
  - 이 감시는 전이적으로 적용 → `~linkLibrary`를 실행하면 `linkLibrary`가 의존하는 `buildObjects`의 `*.c` 소스 파일까지 함께 감시됨
- 입력 파일은 해시가 바뀌어야만 재빌드 트리거

이 동작은 다음 설정으로 무조건 트리거하도록 변경 가능.

```scala
Global / watchForceTriggerOnAnyChange := true
```

- 출력 파일의 변경(`foo.outputFiles`나 `foo.outputFileChanges`로 조회하는 값)은 재빌드를 트리거하지 않음
- 파일 스탬프는 태스크 단위로 추적되며, 해당 증분 태스크 자신이 성공했을 때만 갱신됨
  - 위 예제에서는 `linkLibrary`가 성공했을 때만 `buildObjects` 출력의 마지막 수정 시각이 갱신되어 저장됨
  - 즉 `linkLibrary`를 호출하지 않고 `buildObjects`만 여러 번 실행해도 되고, 다음에 `linkLibrary`가 실행되면 그동안 누적된 `buildObjects` 출력 변경 사항을 한 번에 인식
  - 반대로 `linkLibrary`가 실패하면 `buildObjects` 출력에 대한 스탬프 갱신도 건너뜀 → 어떤 파일까지 성공적으로 처리됐는지 일반적으로 알 수 없기 때문

### 8. 메모리 부족 문제 해결

- sbt는 서브프로젝트 개수와 활성화된 플러그인 수에 따라 메모리를 많이 요구할 수 있음 → 메모리 부족으로 크래시가 나거나 성능이 크게 떨어지기도 함
- 기본 JVM 힙 크기는 1GB
- 메모리 발자국이 큰 프로젝트라면 힙 크기를 늘려서 sbt를 시작해야 할 수 있음

힙을 2GB로 늘리려면 다음처럼 실행.

```bash
sbt -J-Xmx2G
```

- `-J`로 시작하는 커맨드 인자는 모두 JVM 인자로 해석됨
- 매번 옵션을 붙이지 않고 프로젝트 차원에서 항상 2GB로 늘리려면 `.sbtopts` 파일을 만들거나 편집해서 다음 줄 추가

```
-J-Xmx2G
```

- sbt를 대화형 모드나 서버 모드(`sbt --client` 또는 `sbtn`으로 시작한 경우)로 실행할 때는, 빌드 안의 각 태스크가 자신이 사용한 자원을 반드시 정리 필요
  - 그러지 않으면 sbt 프로세스의 메모리 사용량이 시간이 지날수록 계속 늘어날 수 있음
  - 예: `run` 태스크가 Akka `ActorSystem`을 시작한다면, `run`이 끝나기 전에 그 `ActorSystem`을 shutdown 필요 → 그러지 않으면 `run`을 호출할 때마다 sbt 프로세스의 메모리 사용량 누적

### 9. 메모리 누수 진단

- 메모리 누수를 고치려면 예상보다 오래 메모리에 남아 있는 객체가 어떤 클래스인지 먼저 찾아야 함
- 가장 쉬운 방법은 JDK가 제공하는 `jmap` 명령과 VisualVM 같은 JVM 메모리 분석 도구를 함께 쓰는 것

1. `ps` 명령으로 디버깅하려는 sbt 프로세스의 PID 확인
2. 다음 명령으로 힙 덤프 생성
   ```bash
   jmap -dump:format=b,file=leak.hprof $SBT_PID
   ```
3. 생성된 `leak.hprof` 파일을 VisualVM에서 열기

- 어떤 클래스가 메모리를 가장 많이 차지하는지 한눈에 보일 때도 있지만, 그렇지 않으면 "Compute Retained Sizes" 버튼 클릭 필요
- 힙이 크면 이 계산에 시간이 걸릴 수 있지만, 메모리를 가장 많이 차지하는 클래스를 정확히 짚어낼 수 있음
- 이 과정을 통해 누수가 발생한 스레드나 정리되지 않은 캐시의 위치를 찾아내는 데 도움됨

### 10. 커스텀 의존성 구성이란

- 의존성 구성(dependency configuration, 줄여서 구성)은 라이브러리 의존성 그래프를 정의하며, 자체 클래스패스·소스·생성 패키지 등을 가질 수 있음
  - 이 개념은 sbt가 관리 의존성에 예전에 사용했던 Ivy와 [Maven의 스코프](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Dependency_Scope)에서 유래
- sbt에서 흔히 볼 수 있는 구성
  - `Compile`: 메인 빌드 정의(`src/main/scala`)
  - `Test`: 테스트 빌드 정의(`src/test/scala`)
  - `Runtime`: `run` 태스크의 클래스패스

### 11. 커스텀 구성 정의 시 주의사항

- 커스텀 구성은 `Test`처럼 새로운 소스 코드 집합이나 독자적인 라이브러리 의존성을 도입할 때만 고려 필요
- 단순히 키를 네임스페이스로 구분하려는 목적으로 구성을 도입하는 것은 지양

커스텀 구성의 단점:

- 사용자가 스코프 개념의 복잡도에 혼란을 겪음 → 서브프로젝트와 태스크 개념에는 익숙해도, 여기에 구성 스코프까지 얽히면 이해하기 어려워짐
- sbt 자체의 지원이 제한적 → 어떤 구성이 다른 구성을 `extend`한다고 선언할 수는 있지만, 설정(setting)의 상속은 이루어지지 않음 → 필요한 설정과 태스크를 전부 직접 채워 넣어야 함
- 이 때문에 sbt에 새 기능이 추가되어도 커스텀 구성까지 그 기능을 지원하지 못할 가능성이 큼, 서드파티 플러그인도 마찬가지로 커스텀 구성을 잘 지원하지 않는 경우가 많음

### 12. 기본 커스텀 구성 예제

- 플러그인 안에서 `config()` 함수로 새 구성을 만들고, `inConfig()`로 그 구성 스코프에 표준 설정을 적용

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

- `Defaults.configSettings`는 `Compile`/`Test` 같은 표준 구성이 갖는 소스 디렉터리, 컴파일, 패키징 관련 설정 일체를 그대로 제공
- 정의한 플러그인은 build.sbt에서 `.configs(...)`로 구성을 등록하고 `.enablePlugins(...)`으로 활성화

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

### 13. 샌드박스 구성 패턴

- 구성을 활용하는 또 다른 유용한 기법: 사용자 프로젝트에 부가적인 의존성 그래프를 하나 더 추가해서 Coursier가 별도의 jar를 내려받게 하고 그 jar를 태스크가 실행하도록 만드는 것, 이를 샌드박스 구성이라 부름
  - Scala 2.13 CLI 버전의 scalafmt를 프로젝트 안에서 실행하는 용도로 활용 가능
  - sbt 1.4.x 기준으로는 샌드박스 구성이 사용자 서브프로젝트와 동일한 Scala 버전을 써야 한다는 제약 존재

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

핵심 포인트:

- `config("scalafmt").hide`: `hide`를 붙이면 이 구성이 퍼블리시 대상 POM/ivy.xml에는 나타나지 않음 → 사용자 프로젝트가 실제로 의존하는 것처럼 노출할 필요가 없는 내부용 구성이기 때문
- `ivyConfigurations += ScalafmtSandbox`: 새 구성을 Ivy(의존성 해석기)에 등록
- `libraryDependencies += ... % ScalafmtSandbox`: `%` 뒤에 구성을 명시하면 해당 의존성이 이 구성 전용 클래스패스로만 들어감
- `scalafmt := (ScalafmtSandbox / run).evaluated`: 사용자에게 노출되는 `scalafmt` 인풋 태스크는, 내부적으로 샌드박스 구성 스코프의 `run`을 그대로 호출하는 방식으로 구현됨

이렇게 만든 `scalafmt` 태스크를 sbt 셸에서 실행하면 다음처럼 동작.

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

테스트 전용 구성을 추가하는 방법은 별도로 [Testing 문서의 Additional test configurations](https://www.scala-sbt.org/1.x/docs/Testing.html#additional-test-configurations) 절에서 다룸.

### 14. 커스텀 태스크 키 정의하기

- 새 태스크를 만드는 첫 단계는 `taskKey[T]("설명")`로 태스크 키를 선언하는 것
  - 타입 파라미터 `T`는 이 태스크가 반환할 값의 타입, 문자열 인자는 `inspect`·도움말 등에 표시되는 설명
  - 이렇게 선언한 키에 `:=`로 실제 구현을 대입하면 태스크 완성

예를 들어 3개의 서브프로젝트(`core`, `tools`, `client`)로 이루어진 멀티 프로젝트 빌드에서, `core`와 `tools`의 테스트만 실행하고 `client`는 건너뛰는 커스텀 태스크 `myTestTask`는 다음처럼 정의.

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

여기서 눈여겨볼 점:

- `taskKey[Unit](...)`: 반환값이 없는 태스크는 `Unit`으로 선언, 값을 반환하는 태스크라면 `taskKey[String]`, `taskKey[Seq[Path]]`처럼 실제 타입 지정
- `(core / Test / test).value`: 다른 서브프로젝트의 특정 구성 스코프에 있는 태스크를 `프로젝트 / 구성 / 태스크` 형태로 참조하고, `.value`로 그 결과값을 꺼내 씀 → 이 문법 덕분에 `myTestTask`는 `core`와 `client`는 그대로 둔 채 원하는 서브프로젝트만 골라 테스트 실행 가능
- `:=`로 대입되는 블록 자체가 `Def.task`에 해당하는 태스크 본문 → 그 안에서 다른 태스크의 `.value`를 호출하면 sbt가 자동으로 태스크 그래프의 의존 관계를 구성

- 이렇게 정의한 `myTestTask`는 이후 sbt 셸에서 `myTestTask`라고 입력하는 것만으로 실행 가능, 필요하다면 build.sbt의 다른 설정에서도 하나의 태스크 키로 참조 가능
- 값이 아니라 빌드 시점에 고정되는 설정을 정의하고 싶을 때는 같은 방식으로 `settingKey[T]`를, 커맨드라인 인자를 받는 태스크가 필요할 때는 `inputKey[T]` 사용

---

## 태스크 실행 순서 제어하기

> 원본: https://www.scala-sbt.org/1.x/docs/Howto-Sequencing.html, https://www.scala-sbt.org/1.x/docs/Howto-Sequential-Task.html, https://www.scala-sbt.org/1.x/docs/Howto-Dynamic-Task.html, https://www.scala-sbt.org/1.x/docs/Howto-After-Input-Task.html, https://www.scala-sbt.org/1.x/docs/Howto-Dynamic-Input-Task.html, https://www.scala-sbt.org/1.x/docs/Howto-Sequence-using-Commands.html

---

### 목차

1. [개요: 왜 순서 제어가 어려운가](#1-개요-왜-순서-제어가-어려운가)
2. [Def.sequential로 순차 실행하기](#2-defsequential로-순차-실행하기)
3. [Def.taskDyn으로 동적 태스크 만들기](#3-deftaskdyn으로-동적-태스크-만들기)
4. [input task 실행 이후 작업 이어붙이기](#4-input-task-실행-이후-작업-이어붙이기)
5. [Def.inputTaskDyn으로 dynamic input task 만들기](#5-definputtaskdyn으로-dynamic-input-task-만들기)
6. [커맨드로 시퀀싱하기](#6-커맨드로-시퀀싱하기)

---

### 1. 개요: 왜 순서 제어가 어려운가

- sbt의 `build.sbt`는 명령어를 순서대로 나열하는 스크립트가 아니라 태스크 사이의 의존성 그래프를 선언하는 DSL
  - 예를 들어 다음 코드는 "Y가 X 다음에 실행된다"가 아니라 "Y는 X에 의존한다"는 선언

```scala
taskY := {
  val x = taskX.value
  x + 1
}
```

이는 일반적인 Scala의 명령형 코드와 다름.

```scala
def foo(): Unit = {
  doX()
  doY()
}
```

의존성 그래프 모델을 쓰는 이유는 크게 두 가지.

- 병렬화: 서로 의존 관계가 없는 태스크는 가능한 경우 병렬로 실행
- 중복 제거: `Compile / compile` 같은 태스크는 한 커맨드 실행당 한 번만 평가되고 재사용

- 문제는 이 모델이 "A를 실행한 다음 B를 실행하라" 같은 명령형 순서 제어와는 결이 다르다는 점
- 순서를 강제로 지정해야 하는 경우 sbt는 다음과 같은 도구를 제공하며, 이는 사실상 병렬화·중복 제거라는 기본 이점을 일부 포기하고 명시적 순서를 얻는 트레이드오프
  - `Def.sequential`: 여러 태스크를 순서대로, 앞선 태스크가 실패하면 중단하며 실행
  - `Def.taskDyn`: 태스크 실행 도중 값을 보고 다음에 실행할 태스크를 동적으로 결정
  - input task 뒤에 코드 잇기: `.evaluated`를 이용해 input task 실행 후 부수효과 코드 실행
  - `Def.inputTaskDyn`: input task의 파싱 결과에 따라 동적으로 다음 태스크를 결정
  - 커맨드(`Command`): 태스크 그래프 밖에서 상태(`State`)를 직접 조작하며 순차 실행

이어지는 절에서 각각을 다룸.

---

### 2. Def.sequential로 순차 실행하기

- `Def.sequential`은 sbt 0.13.8에서 도입, 여러 태스크를 "준-순차적(semi-sequential)" 의미로 실행
  - 앞의 태스크가 실패하면 뒤의 태스크는 실행되지 않는다는 점이 일반적인 태스크 의존성 선언과 다름

```scala
// build.sbt
lazy val compilecheck = taskKey[Unit]("compile and then scalastyle")

lazy val root = (project in file("."))
  .settings(
    Compile / compilecheck := Def.sequential(
      Compile / compile,
      (Compile / scalastyle).toTask("")
    ).value
  )
```

- `compilecheck`를 실행하면 `Compile / compile`이 먼저 실행되고, 컴파일이 성공한 경우에만 `Compile / scalastyle`이 이어서 실행됨

특징:

- 순서 보장: 나열한 순서 그대로 실행됨
- 실패 시 중단: 앞 태스크가 실패하면 뒤 태스크는 실행되지 않음 → 일반적인 태스크 그래프에서는 여러 업스트림이 병렬로 평가되고 실패 처리 방식도 다르지만, `Def.sequential`은 명시적으로 "차례로, 실패하면 멈춘다"는 의미를 부여
- `.value` 필요: `Def.sequential(...)`은 그 자체로 `Initialize[Task[A]]`이므로 최종적으로 `.value`를 호출해 결과를 꺼내야 함
- `Compile / scalastyle` 같은 input task는 `.toTask("")`로 인자 없이 태스크화한 뒤에 시퀀스에 넣음

- `Def.sequential`은 "순서만" 강제할 뿐, 태스크 실행 도중 얻은 값을 보고 다음에 실행할 태스크를 바꾸지는 못함 → 그런 동적 분기가 필요하면 3장의 `Def.taskDyn` 사용

---

### 3. Def.taskDyn으로 동적 태스크 만들기

- `Def.taskDyn`은 `Def.sequential`에서 한 단계 더 나아간 도구
  - `Def.task`는 순수한 값 `A`를 반환하는 반면, `Def.taskDyn`은 `sbt.Def.Initialize[sbt.Task[A]]`, 즉 "또 다른 태스크"를 반환
  - 이 덕분에 태스크 엔진이 그 반환된 태스크를 이어서 평가 가능 → 실행 중간에 얻은 값을 바탕으로 다음에 무엇을 실행할지 동적으로 결정 가능

```scala
lazy val compilecheck = taskKey[sbt.inc.Analysis]("compile and then scalastyle")

lazy val root = (project in file("."))
  .settings(
    compilecheck := (Def.taskDyn {
      val c = (Compile / compile).value
      Def.task {
        val x = (Compile / scalastyle).toTask("").value
        c
      }
    }).value
  )
```

- 바깥쪽 `Def.taskDyn` 블록에서 `(Compile / compile).value`로 컴파일을 먼저 실행하고, 그 결과 `c`를 안쪽 `Def.task` 클로저 안에서 활용
- 안쪽 블록에서 `scalastyle`을 실행한 뒤 컴파일 결과 `c`를 최종값으로 반환

- 기존 키를 그대로 재정의하는 것도 가능 → `Compile / compile`과 같은 반환 타입(`Analysis`)을 갖는 동적 태스크라면 아예 `Compile / compile` 자체를 다시 바인딩 가능

```scala
Compile / compile := (Def.taskDyn {
  val c = (Compile / compile).value
  Def.task {
    val x = (Compile / scalastyle).toTask("").value
    c
  }
}).value
```

- 이렇게 하면 쉘에서 그냥 `compile`만 입력해도 내부적으로 `scalastyle` 검사까지 자동으로 딸려 실행됨

`Def.sequential`과 `Def.taskDyn`의 차이:

- 반환값: `Def.sequential`은 마지막 태스크의 결과, `Def.taskDyn`은 클로저 안에서 자유롭게 조합한 값
- 동적 분기: `Def.sequential`은 불가(고정된 태스크 목록), `Def.taskDyn`은 가능(이전 값에 따라 다음 태스크 선택)
- 실패 처리: `Def.sequential`은 앞 태스크 실패 시 중단, `Def.taskDyn`은 일반 태스크와 동일한 실패 전파
- 대표 사용처: `Def.sequential`은 단순 순차 파이프라인, `Def.taskDyn`은 조건부·값 기반 순서 제어

---

### 4. input task 실행 이후 작업 이어붙이기

- `Compile / run`처럼 인자를 받는 input task를 실행한 다음, 예를 들어 브라우저를 여는 것과 같은 후속 작업을 붙이고 싶은 경우 존재
  - 이때 핵심은 input task 참조에 `.evaluated`를 붙여 그 결과를 먼저 평가시키고, 그 뒤에 이어지는 코드를 부수효과로 실행하는 것

예제 애플리케이션(`src/main/scala/Greeting.scala`):

```scala
object Greeting {
  def main(args: Array[String]): Unit = {
    println("hello " + args.toList)
  }
}
```

방법 1: 새 input task 정의

```scala
lazy val runopen = inputKey[Unit]("run and then open the browser")

lazy val root = (project in file("."))
  .settings(
    runopen := {
      (Compile / run).evaluated
      println("open browser!")
    }
  )
```

방법 2: 기존 태스크 재정의

```scala
lazy val root = (project in file("."))
  .settings(
    Compile / run := {
      (Compile / run).evaluated
      println("open browser!")
    }
  )
```

`> runopen foo`를 실행하면 다음 순서로 출력됨.

1. 컴파일 로그
2. 애플리케이션 실행 결과: `hello List(foo)`
3. 후속 부수효과: `open browser!`

- 여기서 `.evaluated`는 "이 input task를 지금 실행하고 그 결과값을 받아온다"는 의미 → 이 구문이 있어야 뒤따르는 코드가 실행 이후 시점에 동작한다는 것이 보장됨
- 다만 이 방식은 인자를 그대로 전달만 할 뿐, 파싱한 인자값 자체를 바탕으로 실행할 태스크를 바꾸지는 못함 → 인자에 따라 동적으로 분기해야 한다면 5장의 `Def.inputTaskDyn` 필요

---

### 5. Def.inputTaskDyn으로 dynamic input task 만들기

- `Def.inputTaskDyn`은 `Def.taskDyn`의 input task 버전
  - 인자를 파싱한 뒤 그 값을 바탕으로 동적으로 다음 태스크를 결정하고, 실행이 끝나면 후속 태스크를 이어 붙일 수 있음

v1: 기본 접근

```scala
lazy val runopen = inputKey[Unit]("run and then open the browser")
lazy val openbrowser = taskKey[Unit]("open the browser")

lazy val root = (project in file("."))
  .settings(
    runopen := (Def.inputTaskDyn {
      import sbt.complete.Parsers.spaceDelimited
      val args = spaceDelimited("<args>").parsed
      Def.taskDyn {
        (Compile / run).toTask(" " + args.mkString(" ")).value
        openbrowser
      }
    }).evaluated,
    openbrowser := {
      println("open browser!")
    }
  )
```

- `spaceDelimited("<args>").parsed`로 커맨드라인 인자를 받은 뒤, `(Compile / run).toTask(...)`에 그 인자를 문자열로 붙여 넘기고, 실행이 끝나면 `openbrowser` 태스크를 이어서 실행

v2: 순환 참조를 피하는 권장 방식

- v1처럼 `Compile / run`을 그대로 재정의하면서 동시에 그 안에서 `Compile / run`을 호출하면 순환 참조 문제 발생 가능
- 이를 피하려면 실제 실행 로직을 별도 키(`actualRun`)로 분리

```scala
lazy val actualRun = inputKey[Unit]("The actual run task")
lazy val openbrowser = taskKey[Unit]("open the browser")

lazy val root = (project in file("."))
  .settings(
    Compile / run := (Def.inputTaskDyn {
      import sbt.complete.Parsers.spaceDelimited
      val args = spaceDelimited("<args>").parsed
      Def.taskDyn {
        (Compile / actualRun).toTask(" " + args.mkString(" ")).value
        openbrowser
      }
    }).evaluated,
    Compile / actualRun := Defaults.runTask(
      Runtime / fullClasspath,
      Compile / run / mainClass,
      Compile / run / runner
    ).evaluated,
    openbrowser := {
      println("open browser!")
    }
  )
```

- `Compile / actualRun`의 구현은 sbt 내부 `Defaults.scala`에서 그대로 가져온 것으로, `Compile / run`을 재정의하면서도 실제 실행 로직과의 순환을 끊는 역할

주의할 점:

- `testOnly`처럼 인자 끝에 공백이 있으면 실패하는 태스크가 있으므로, `toTask`에 넘길 문자열은 `.replaceAll("\\s+$", "")`로 뒤쪽 공백을 제거해두는 편이 안전
- `run foo`를 실행하면 인자를 반영한 `actualRun`이 먼저 실행되고, 그다음 `openbrowser`가 순서대로 실행됨

---

### 6. 커맨드로 시퀀싱하기

- 태스크 그래프의 캐싱·병렬화보다 부수효과와 순서 자체가 더 중요한 경우, 예를 들어 릴리스 절차처럼 사람이 콘솔에 명령어를 하나씩 입력하는 것을 그대로 흉내 내고 싶은 경우에는 태스크 대신 커맨드(`Command`)를 쓰는 편이 적합

```scala
commands += Command.command("releaseNightly") { state =>
  "stampVersion" ::
    "clean" ::
    "compile" ::
    "publish" ::
    "bintrayRelease" ::
    state
}
```

- 이 예제는 `releaseNightly`라는 커맨드를 새로 정의하고, 버전 스탬핑 → 클린 → 컴파일 → 퍼블리시 → bintray 릴리스 순서로 다른 커맨드들을 체이닝

커맨드 시퀀싱의 특징:

- 동작 방식: 태스크처럼 의존성 그래프와 캐싱을 관리하는 게 아니라, 사용자가 콘솔에 직접 입력하는 순서를 그대로 재현
- 파라미터: `state: State`를 받아 다음에 실행할 커맨드 목록이 반영된 새 `State`를 반환
- 체이닝: `::` 연산자로 커맨드 문자열들을 이어 붙임
- 반환값: 마지막에 갱신된 `state`를 반환해 세션이 계속 이어지도록 함

- 이 패턴은 sbt 자신의 릴리스 절차에서도 쓰이는 방식이며, 태스크 단위의 세밀한 값 조합보다는 "이 순서대로 여러 명령을 그대로 실행하고 싶다"는 상황에 적합

---

### 정리

- 그냥 순서대로, 실패하면 멈추면 됨 → `Def.sequential`
- 이전 태스크의 결과값을 보고 다음 태스크를 정해야 함 → `Def.taskDyn`
- input task 실행 후 단순 부수효과만 덧붙이면 됨 → `.evaluated` 뒤에 코드 추가
- input task의 인자값에 따라 동적으로 다음 태스크를 정해야 함 → `Def.inputTaskDyn`
- 캐싱/병렬화보다 콘솔 입력 순서 재현이 중요함(릴리스 절차 등) → 커맨드(`Command.command`)

---

## sbt 빌드 예제 모음

> 원본: https://www.scala-sbt.org/1.x/docs/Examples.html, https://www.scala-sbt.org/1.x/docs/Basic-Def-Examples.html, https://www.scala-sbt.org/1.x/docs/Scala-Files-Example.html, https://www.scala-sbt.org/1.x/docs/Advanced-Configurations-Example.html, https://www.scala-sbt.org/1.x/docs/Advanced-Command-Example.html

---

### 목차

1. [.sbt 빌드 예제](#1-sbt-빌드-예제)
2. [.sbt 빌드 + .scala 파일 예제](#2-sbt-빌드--scala-파일-예제)
3. [고급 설정(configuration) 예제](#3-고급-설정configuration-예제)
4. [고급 커맨드 예제](#4-고급-커맨드-예제)

---

### 1. .sbt 빌드 예제

- sbt 공식 문서의 Examples 섹션은 각 항목이 독립적인 `build.sbt` 설정 스니펫들을 모아 놓은 참고 자료
- sbt 0.13.7 이후로는 `build.sbt` 안에서 설정을 구분하는 데 더 이상 빈 줄이 필요 없음, 아래 예제는 이 방식(세미콜론 없는 콤마 구분 설정 목록)을 기준으로 작성

#### 1.1 공통 설정 팩터링

- 여러 프로젝트에서 공유해야 하는 값은 `ThisBuild` 스코프에 지정해 한 곳에서 관리

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

#### 1.2 라이브러리 의존성 정의

- 일반적인 의존성은 `%%`(Scala 버전 자동 부착) 연산자로 `ModuleID`를 만들어 `lazy val`로 미리 뽑아둠
- 저장소가 아니라 특정 URL에서 직접 받아야 하는 아티팩트는 `from` 절과 문자열 인터폴레이션을 함께 사용

```scala
// define ModuleID for library dependencies
lazy val scalacheck = "org.scalacheck" %% "scalacheck" % "1.17.0"

// define ModuleID using string interpolator
lazy val osmlibVersion = "2.5.2-RC1"
lazy val osmlib = ("net.sf.travelingsales" % "osmlib" % osmlibVersion from
  s"""http://downloads.sourceforge.net/project/travelingsales/libosm/$osmlibVersion/libosm-$osmlibVersion.jar""")
```

#### 1.3 프로젝트별 설정 모음

- 아래는 `root` 프로젝트에 적용 가능한 개별 설정 키를 주제별로 묶어 정리한 것
- 실제 문서에서는 하나의 `.settings(...)` 블록 안에 전부 나열되어 있음

소스 디렉터리·의존성

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

컴파일러·태스크 옵션

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

패키징·실행

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

저장소·퍼블리시

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

셸 프롬프트·출력 제어

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

포킹·병렬 실행

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

로깅·트레이스

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

의존성·아티팩트 관리

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

이 설정들을 하나의 `root` 프로젝트에 모으면 다음과 같은 구조.

```scala
lazy val root = (project in file("."))
  .settings(
    // 위 목록의 설정을 모두 콤마로 나열
  )
```

#### 1.4 핵심 패턴 요약

- `ThisBuild / key := value`: 빌드 전체에 적용되는 전역 설정
- `Config / task / key := value`: 특정 설정(Compile/Test)과 태스크(run/packageBin) 조합에 국한된 설정
- `key += value` / `key ++= values`: 기존 시퀀스 값에 항목 추가
- `lazy val x = ...`: 의존성이나 설정 값을 빌드 파일 상단에서 재사용 가능하게 추출

---

### 2. .sbt 빌드 + .scala 파일 예제

- `.sbt` 파일만으로 빌드가 비대해지면, 가장 먼저 저장소(resolver)와 의존성 선언을 `project/*.scala` 파일로 분리하는 것이 일반적인 다음 단계
- `project/` 디렉터리 아래의 `.scala` 파일은 sbt가 빌드 자체를 컴파일할 때 함께 컴파일됨 → `build.sbt`에서 `import`로 바로 참조 가능

#### 2.1 저장소 분리 — project/Resolvers.scala

- Maven 저장소 목록을 한 객체에 모아두고 `Seq[Resolver]` 형태로 내보냄

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

#### 2.2 의존성 분리 — project/Dependencies.scala

- 라이브러리 버전과 `ModuleID`를 한곳에서 관리하면, 여러 서브프로젝트가 필요한 의존성만 선택적으로 참조 가능

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

#### 2.3 자동 플러그인 — project/ShellPromptPlugin.scala

- 커스텀 태스크나 커맨드를 구현하고 싶을 때는 원오프(one-off) `AutoPlugin`으로 빌드를 정리 가능
- 아래 예제는 현재 프로젝트 이름과 git 브랜치를 셸 프롬프트에 표시

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

- `trigger = allRequirements`로 지정했기 때문에 이 플러그인은 별도 `enablePlugins` 없이 빌드의 모든 프로젝트에 자동으로 적용됨

#### 2.4 build.sbt — 분리한 파일 조합

- `project/*.scala`에서 정의한 객체를 `import`해서 여러 서브프로젝트에 공통 설정과 의존성을 나눠 적용

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

#### 2.5 구조 정리

- `project/Resolvers.scala`: 저장소(Resolver) 목록 중앙화
- `project/Dependencies.scala`: 라이브러리 버전과 ModuleID 중앙화
- `project/ShellPromptPlugin.scala`: 원오프 AutoPlugin으로 커스텀 태스크/설정 구현
- `build.sbt`: 위 정의들을 import해 서브프로젝트별로 조합

- 이 구조는 `buildSettings`처럼 공통 설정을 시퀀스로 뽑아 여러 프로젝트의 `.settings(...)`에 재사용하는 것이 핵심이며, 서브프로젝트 수가 늘어날수록 의존성/저장소 선언 중복을 줄이는 효과가 커짐

---

### 3. 고급 구성(configuration) 예제

- sbt의 "configuration"은 Ivy의 구성 개념을 그대로 가져온 것으로, 하나의 모듈이 제공하는 의존성을 여러 그룹으로 나눠 선택적으로 노출하는 데 쓰임
- 이 예제는 별도 유틸리티 모듈을 여러 개 만들지 않고도, 하나의 `utils` 모듈에서 필요한 기능만 골라 의존하게 만드는 방법을 보여줌

#### 3.1 문제 상황

- `utils` 모듈이 Scalate 관련 유틸리티와 Saxon 관련 유틸리티를 모두 제공한다고 하면, 컴파일 클래스패스에는 두 라이브러리가 전부 올라가야 함
- 하지만 `utils`를 사용하는 프로젝트 `a`가 Scalate 기능만 필요하다면, Saxon 의존성까지 전이적으로 끌려오는 것은 불필요

#### 3.2 커스텀 구성 정의

- `config(...)`로 새 구성을 만들고 `.extend(...)`로 상속 관계 지정

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

#### 3.3 프로젝트별 부분 의존

- 프로젝트는 `%` 연산자에 구성 매핑 문자열(`"compile->scalate"` 등)을 넘겨, `utils`가 제공하는 여러 구성 중 필요한 것만 선택해 의존 가능

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

#### 3.4 utils 모듈 설정

- `utils` 프로젝트 자체는 `inConfig`로 `common` 구성 전용 컴파일 설정(예: `src/common/scala`)을 추가하고, 각 라이브러리를 알맞은 구성에 배정

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

#### 3.5 핵심 개념 정리

- `config("이름")`: 새 Ivy 구성 생성
- `.extend(other)`: 다른 구성의 의존성을 상속
- `ivyConfigurations := overrideConfigs(...)`: 기본 구성 집합을 커스텀 구성으로 교체
- `defaultConfiguration := Some(Common)`: 구성을 명시하지 않은 의존성의 기본 귀속처 지정
- `dependsOn(module % "compile->scalate")`: 의존 대상 모듈의 특정 구성만 선택적으로 참조
- `Common / classpathConfiguration`: 특정 구성이 참조할 클래스패스 구성 지정

- 이 방식은 다수의 유틸리티 jar를 따로 만들지 않고도, 단일 모듈에서 여러 기능 조합을 세분화해 제공하고 싶을 때 쓸 수 있는 대안

---

### 4. 고급 커맨드 예제

- 이 예제는 sbt 설정 시스템의 확장성을 보여주는 고급 사례로, 빌드에 선언된 모든 의존성을 어디서 정의됐는지와 무관하게 일괄 수정하는 커맨드를 구현
- `Project`가 아니라 빌드 전체에서 만들어진 최종 `Seq[Setting[_]]`을 직접 조작한다는 점이 특징

#### 4.1 동작 방식

- `canonicalize` 커맨드를 실행하면 변경 사항 적용
- `reload`를 하거나 `set`으로 설정을 다시 지정하면 변경 사항이 되돌아감, 다시 적용하려면 `canonicalize` 재실행 필요
- 이 예제는 선언된 모든 ScalaCheck 의존성을 버전 1.8로 강제 변환하는 것을 보여줌, 같은 방식으로 다른 의존성·저장소·`scalacOptions` 등을 변환하거나 설정을 추가/삭제하는 것도 가능
- `Project`의 설정에 대해서만 직접 적용할 수도 있지만, 그렇게 하면 플러그인이나 `build.sbt`에서 자동으로 추가된 설정은 빠지게 됨 → 이 예제는 외부 빌드를 포함한 모든 빌드, 모든 프로젝트의 모든 설정에 무조건 적용하는 방법을 보여줌

#### 4.2 구현

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

#### 4.3 핵심 개념 정리

- `override def trigger = allRequirements`: 이 AutoPlugin을 모든 프로젝트에 자동 적용
- `Command.command("이름") { state => ... }`: `State`를 받아 새 `State`를 반환하는 커맨드 정의
- `Project.extract(state).session.mergeSettings`: 세션에서 병합된 전체 `Setting[_]` 목록을 얻음
- `Setting#mapInit(f)`: 설정의 초기값 계산 함수를 감싸 변환 로직 삽입
- `appendWithSession(transformed, state)`: 변환된 설정을 현재 세션에 반영해 새 `State` 생성
- `mapLibraryDependencies`의 멱등성: 설정이 누적 적용(`+=`)될 때마다 매번 호출되므로 반드시 멱등이어야 함

- 이 패턴은 특정 프로젝트나 특정 설정 파일에 국한되지 않고, 빌드 그래프 전체의 모든 설정에 일괄적으로 개입해야 하는 상황(의존성 버전 강제 통일, 전역 컴파일 옵션 주입 등)에 응용 가능

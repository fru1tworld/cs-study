# sbt 매크로 프로젝트, 경로 API, 병렬 실행, 외부 프로세스

> 원본: https://www.scala-sbt.org/1.x/docs/Macro-Projects.html, https://www.scala-sbt.org/1.x/docs/Paths.html, https://www.scala-sbt.org/1.x/docs/Parallel-Execution.html, https://www.scala-sbt.org/1.x/docs/Process.html

---

## 목차

1. [매크로 프로젝트 분리](#1-매크로-프로젝트-분리)
2. [경로(PathFinder) API](#2-경로pathfinder-api)
3. [병렬 태스크 실행 제어](#3-병렬-태스크-실행-제어)
4. [외부 프로세스 실행](#4-외부-프로세스-실행)

---

## 1. 매크로 프로젝트 분리

### 1.1 왜 분리해야 하는가

Scala 컴파일러는 매크로 구현체를 사용하는 시점보다 먼저 컴파일해 두어야 한다는 제약을 가진다. 같은 컴파일 단위 안에 매크로 정의와 매크로 호출을 함께 두면 컴파일 순서 문제로 빌드가 실패하므로, 매크로 구현을 별도 서브프로젝트로 분리하고 사용하는 쪽 프로젝트가 이를 의존하는 구조를 취해야 한다.

일반적으로 다음과 같이 역할을 나눈다.

| 서브프로젝트 | 역할 |
|--------------|------|
| `macro` | 매크로 구현체를 담는 프로젝트 |
| `core` | 매크로를 실제로 사용하는 메인 프로젝트, `macro`를 의존 |
| `util` (선택) | `macro`와 `core`가 함께 참조하는 공용 코드 |

### 1.2 기본 빌드 구성

`macro` 프로젝트는 blackbox 매크로 컨텍스트를 다루기 위해 `scala-reflect`를 의존성으로 추가해야 한다.

```scala
lazy val commonSettings = Seq(
  scalaVersion := "2.12.18",
  organization := "com.example"
)

lazy val scalaReflect = Def.setting {
  "org.scala-lang" % "scala-reflect" % scalaVersion.value
}

lazy val core = (project in file("core"))
  .dependsOn(macroSub)
  .settings(
    commonSettings,
    // other settings here
  )

lazy val macroSub = (project in file("macro"))
  .settings(
    commonSettings,
    libraryDependencies += scalaReflect.value
    // other settings here
  )
```

`core`는 `.dependsOn(macroSub)`로 `macro`를 참조하므로, sbt는 `macro`를 먼저 컴파일한 뒤 `core`를 컴파일한다. 이 순서 보장이 매크로 분리 구조의 핵심이다.

### 1.3 공용 인터페이스 프로젝트 추가하기

매크로와 그 사용처가 공통 타입이나 헬퍼를 공유해야 한다면 `util` 프로젝트를 추가로 둔다.

```scala
lazy val util = (project in file("util"))
  .settings(commonSettings)

lazy val core = (project in file("core"))
  .dependsOn(macroSub, util)

lazy val macroSub = (project in file("macro"))
  .dependsOn(util)
```

### 1.4 매크로 프로젝트를 별도로 퍼블리시하지 않기

매크로 구현은 라이브러리 사용자 입장에서 별도 아티팩트로 노출할 필요가 없는 경우가 많다. 이때는 `macro`의 컴파일 결과물을 `core` JAR에 병합하면서, `macro` 자체는 저장소에 퍼블리시되지 않도록 막을 수 있다.

```scala
lazy val core = (project in file("core"))
  .dependsOn(macroSub % "compile-internal, test-internal")
  .settings(
    Compile / packageBin / mappings ++=
      (macroSub / Compile / packageBin / mappings).value,
    Compile / packageSrc / mappings ++=
      (macroSub / Compile / packageSrc / mappings).value
  )

lazy val macroSub = (project in file("macro"))
  .settings(
    libraryDependencies += scalaReflect.value,
    publish := {},
    publishLocal := {}
  )
```

핵심은 세 가지다.

- `dependsOn(macroSub % "compile-internal, test-internal")`: `macro`를 컴파일/테스트 시점에만 참조하고, `core`를 사용하는 외부 프로젝트의 클래스패스에는 `macro`가 별도로 노출되지 않는다.
- `packageBin`/`packageSrc`의 `mappings`에 `macro`의 결과물을 추가: 컴파일된 클래스와 소스가 `core` 아티팩트 안에 함께 패키징된다.
- `publish := {}`, `publishLocal := {}`: `macro` 프로젝트 자체는 퍼블리시 대상에서 제외한다.

---

## 2. 경로(PathFinder) API

### 2.1 기본 타입

sbt의 경로 처리는 `java.io.File`을 중심에 두고 다음 세 가지 보조 개념으로 확장된다.

| 개념 | 역할 |
|------|------|
| `RichFile` | `File`에 경로 조합 연산자(`/` 등)를 추가 |
| `PathFinder` | `File`과 `Seq[File]`에 검색·필터링 연산자를 추가 |
| `Path`, `IO` | 파일 경로 조작, 읽기/쓰기 등 일반적인 파일 I/O 유틸리티 |

파일 경로는 다음처럼 직접 만들 수도 있고,

```scala
val source: File = file("/home/user/code/A.scala")
```

`/` 연산자로 하위 경로를 이어 붙일 수도 있다.

```scala
def readme(base: File): File = base / "README"
```

설정값에서 기준 경로가 필요하면 상대 경로 대신 `baseDirectory.value`를 사용해 절대 경로를 확보하는 것이 안전하다.

```scala
unmanagedBase := baseDirectory.value / "custom_lib"
```

### 2.2 PathFinder로 파일 집합 찾기

`PathFinder`는 즉시 계산되지 않고, `get`을 호출하는 시점에 실제 파일시스템을 조회해 `Seq[File]`을 만들어낸다. 따라서 같은 `PathFinder`를 재사용해도 매번 최신 파일 목록을 얻을 수 있다.

**하위 디렉터리 전체 탐색(`**`)**: 재귀적으로 조건에 맞는 파일을 모두 찾는다.

```scala
def scalaSources(base: File): Seq[File] = {
  val finder: PathFinder = (base / "src") ** "*.scala"
  finder.get
}
```

**직계 자식만 탐색(`*`)**: 한 단계 아래 항목만 대상으로 한다.

```scala
def scalaSources(base: File): PathFinder = (base / "src") * "*.scala"
```

**합치기(`+++`)**: 여러 `PathFinder`를 하나로 합친다.

```scala
def multiPath(base: File): PathFinder =
   (base / "src" / "main") +++
   (base / "lib") +++
   (base / "target" / "classes")
```

합친 결과에 다시 선택 연산자를 적용할 수도 있다.

```scala
def jars(base: File): PathFinder =
   (base / "lib" +++ base / "target") * "*.jar"
```

**제외(`---`)**: 특정 파일 집합을 결과에서 빼낸다.

```scala
def sources(base: File) =
   ( (base / "src") ** "*.scala") --- ( (base / "src") ** ".svn" ** "*.scala")
```

**임의 조건 필터링(`filter`)**: 파일 단위 술어를 그대로 적용한다.

```scala
def srcDirs(base: File) = ( (base / "src") ** "*") filter { _.isDirectory }
def archivesOnly(base: PathFinder) = base filter ClasspathUtilities.isArchive
```

### 2.3 파일 필터 조합

문자열 패턴은 `*`를 와일드카드로 쓰는 `FileFilter`로 암묵 변환된다.

```scala
def testSrcs(base: File): PathFinder = (base / "src") * "*Test*.scala"
```

필터끼리는 다음 연산자로 조합한다.

- `||`: 여러 패턴 중 하나라도 일치하면 선택

```scala
def sources(base: File): PathFinder = (base / "src") ** ("*.scala" || "*.java")
```

- `--`: 패턴에 일치하되 특정 이름은 제외

```scala
def imageResources(base: File): PathFinder =
   (base/"src"/"main"/"resources") * ("*.png" -- "logo.png")
```

### 2.4 결과를 문자열로 변환하기

| 메서드 | 반환 형태 |
|--------|-----------|
| `toString` | 절대 경로를 한 줄에 하나씩 나열 (디버깅용) |
| `absString` | 절대 경로를 플랫폼 구분자로 이어 붙인 문자열 |
| `getPaths` | 절대 경로 문자열의 `Seq[String]` |

### 2.5 참고 사항

- `get`은 호출할 때마다 파일 목록을 새로 계산하므로 파일시스템 변경 사항이 즉시 반영된다.
- 존재하지 않는 파일은 결과에서 제외되며, 빈 `PathFinder`의 `get`은 빈 `Seq[File]()`이다.
- 기준 경로 자체가 존재하지 않으면 그 위에 적용한 선택 연산도 빈 결과를 낸다.

---

## 3. 병렬 태스크 실행 제어

### 3.1 태스크 순서와 병렬 실행의 기본 원칙

sbt는 태스크 간 선언된 의존 관계에 따라 실행 순서를 정한다. 두 태스크 사이에 명시적인 입력 의존이 없으면 sbt는 실행 순서를 보장하지 않으며, 필요하다면 동시에 실행할 수도 있다. 다음처럼 파일을 통해서만 암묵적으로 연결된 코드는 문제를 일으킨다.

```scala
write := IO.write(file("/tmp/sample.txt"), "Some content.")
read := IO.read(file("/tmp/sample.txt"))
```

`write`와 `read` 사이에는 태스크 그래프상 의존 관계가 없으므로, `read`가 `write`보다 먼저 실행되거나 동시에 실행될 위험이 있다. 올바른 방법은 한 태스크의 반환값을 다른 태스크의 입력으로 명시적으로 사용하는 것이다.

```scala
write := {
  val f = file("/tmp/sample.txt")
  IO.write(f, "Some content.")
  f
}

read := IO.read(write.value)
```

이렇게 하면 `read`는 `write.value`를 참조하므로 sbt가 `write`를 먼저 완료한 뒤 `read`를 실행하도록 순서를 보장한다.

### 3.2 태스크 태깅과 동시성 제한

sbt는 태스크에 태그를 붙이고, 태그별로 동시 실행 개수를 제한하는 방식으로 병렬성을 제어한다.

**태그 붙이기**: `tag`는 가중치 1을 가진 태그를, `tagw`는 임의 가중치를 가진 태그를 부여한다.

```scala
def myCompileTask = Def.task { ... } tag(Tags.CPU, Tags.Compile)
compile := myCompileTask.value

def downloadImpl = Def.task { ... } tagw(Tags.Network -> 3)
download := downloadImpl.value
```

**제한 규칙 설정**: `Global / concurrentRestrictions`에 규칙 목록을 지정한다.

```scala
Global / concurrentRestrictions := Seq(
  Tags.limit(Tags.CPU, 2),
  Tags.limit(Tags.Network, 10),
  Tags.limit(Tags.Test, 1),
  Tags.limitAll(15)
)
```

이 설정은 CPU 태그가 붙은 태스크는 동시 2개까지, Network 태그는 동시 10개까지, Test 태그는 동시 1개까지, 전체 태스크는 프로젝트를 통틀어 동시 15개까지만 실행되도록 제한한다.

### 3.3 기본 제공 태그

| 분류 | 태그 |
|------|------|
| 의미 기반 | `Compile`, `Test`, `Publish`, `Update`, `Untagged`, `All` |
| 자원 기반 | `Network`, `Disk`, `CPU` |

`compile` 태스크는 기본적으로 `Compile`, `CPU` 태그를, `test`는 `Test` 태그를, `update`는 `Update`, `Network` 태그를, 퍼블리시 관련 태스크는 `Publish`, `Network` 태그를 갖는다.

### 3.4 고급 제한 방법

짧게 끝나는 태그 없는 태스크까지 CPU 제한에 함께 포함시키고 싶다면 `limitSum`을 쓴다.

```scala
Tags.limitSum(2, Tags.CPU, Tags.Untagged)
```

특정 태스크를 다른 태스크와 절대 동시에 실행하지 않도록 하려면 `exclusive`를 쓴다.

```scala
Tags.exclusive(Benchmark)
```

더 복잡한 조건이 필요하면 커스텀 함수로 직접 정의할 수 있다.

```scala
Tags.customLimit { (tags: Map[Tag,Int]) =>
  val exclusive = tags.getOrElse(Benchmark, 0)
  val all = tags.getOrElse(Tags.All, 0)
  exclusive == 0 || all == 1
}
```

### 3.5 기본 동작과 하위 호환성

`concurrentRestrictions`를 별도로 지정하지 않으면 sbt는 다음과 동등한 기본값을 사용한다.

```scala
Global / concurrentRestrictions := {
  val max = Runtime.getRuntime.availableProcessors
  Tags.limitAll(if(parallelExecution.value) max else 1) :: Nil
}
```

즉 `parallelExecution` 설정값이 `true`면 사용 가능한 코어 수만큼, `false`면 1개로 전체 동시 실행 개수를 제한한다. 기존에 널리 쓰이던 `Test / parallelExecution := false` 같은 설정도 이 체계 위에서 그대로 동작한다.

### 3.6 커스텀 태그 정의하기

새로운 태그가 필요하면 `Tags.Tag`에 이름을 넘겨 직접 만든다.

```scala
val Custom = Tags.Tag("custom")

def aImpl = Def.task { ... } tag(Custom)
aCustomTask := aImpl.value

Global / concurrentRestrictions +=
  Tags.limit(Custom, 1)
```

---

## 4. 외부 프로세스 실행

### 4.1 개요

sbt는 외부 명령을 실행할 때 Scala 표준 라이브러리의 프로세스 API(`scala.sys.process`)를 그대로 활용한다. 빌드 스크립트에서 사용하려면 다음을 임포트한다.

```scala
import scala.sys.process._
```

### 4.2 명령 실행과 종료 코드

`!` 연산자는 명령을 실행하고 완료를 기다린 뒤 종료 코드를 반환한다. 문자열은 암묵적으로 `ProcessBuilder`로 변환된다.

```scala
"find project -name *.jar" !
```

출력을 sbt 로거로 보내려면 로거를 함께 넘긴다.

```scala
val log = streams.value.log
"find project -name *.jar" ! log
```

`run` 메서드는 `scala.sys.process.Process` 인스턴스를 반환하며, 이를 이용해 완료 전에 프로세스를 `destroy`로 강제 종료할 수 있다.

### 4.3 작업 디렉터리와 환경변수 지정

작업 디렉터리나 환경변수를 지정하려면 `Process`를 명시적으로 생성한다.

```scala
Process("ls" :: "-l" :: Nil, Path.userHome, "key1" -> value1, "key2" -> value2) ! log
```

### 4.4 흐름 제어 연산자

`#` 접두 연산자는 셸의 제어 흐름과 유사하게 동작한다.

| 연산자 | 의미 |
|--------|------|
| `a #&& b` | `a`를 실행하고, 종료 코드가 0이면 `b`도 실행 |
| `a #\|\| b` | `a`를 실행하고, 종료 코드가 0이 아니면 `b`도 실행 |
| `a #\| b` | `a`의 출력을 `b`의 입력으로 파이프 |

### 4.5 입출력 리디렉션

```scala
a #< url               // URL을 입력으로 사용
a #< file              // 파일을 입력으로 사용
a #> file              // 출력을 파일에 기록
a #>> file             // 출력을 파일에 이어 붙임
url #> a               // 입력 지정의 대안 표기
file #> a              // 위와 동일한 대안 표기
file #< a              // 출력 리디렉션의 대안 표기
file #<< a             // 이어 붙이기의 대안 표기
```

### 4.6 출력을 문자열로 받기

```scala
val listed: String = "ls" !!
val lines2: Stream[String] = "ls" lines_!
```

`!!`는 실행 결과 전체를 하나의 `String`으로 반환하고, `lines_!`는 줄 단위 `Stream[String]`으로 반환한다.

### 4.7 실전 예제

URL 내용을 파일로 내려받는다.

```scala
url("http://databinder.net/dispatch/About") #> file("About.html") !
```

파일을 복사한다.

```scala
file("About.html") #> file("About_copy.html") !
```

내려받은 내용을 필터링해 파일에 이어 붙인다.

```scala
url("http://databinder.net/dispatch/About") #> "grep JSON" #>> file("About_JSON") !
```

여러 연산자를 조합한 복합 파이프라인도 구성할 수 있다.

```scala
"find src -name *.scala -exec grep null {} ;" #| "xargs test -z" #&& "echo null-free" #|| "echo null detected" !
```

여러 소스를 이어 붙인 뒤 필터링한다.

```scala
cat(spde, dispatch, build) #| "grep -i scala" !
```

# sbt Classpath 구성, 컴파일러 플러그인, Scala 설정, Forking

> 원본: https://www.scala-sbt.org/1.x/docs/Classpaths.html, https://www.scala-sbt.org/1.x/docs/Compiler-Plugins.html, https://www.scala-sbt.org/1.x/docs/Configuring-Scala.html, https://www.scala-sbt.org/1.x/docs/Forking.html

---

## 목차

1. [Classpath 기본 구조](#1-classpath-기본-구조)
2. [Unmanaged vs Managed, Internal vs External](#2-unmanaged-vs-managed-internal-vs-external)
3. [소스와 리소스 구성](#3-소스와-리소스-구성)
4. [컴파일러 플러그인 추가](#4-컴파일러-플러그인-추가)
5. [Scala 버전 자동 관리](#5-scala-버전-자동-관리)
6. [로컬 Scala 설치 사용](#6-로컬-scala-설치-사용)
7. [sbt 자체의 Scala 버전](#7-sbt-자체의-scala-버전)
8. [Forking으로 별도 JVM에서 실행](#8-forking으로-별도-jvm에서-실행)
9. [Fork API 직접 사용](#9-fork-api-직접-사용)

---

## 1. Classpath 기본 구조

sbt는 `compile`, `run`, `test` 등 태스크마다 필요한 클래스패스를 자동으로 구성한다. 클래스패스는 컴파일된 클래스 파일, Scala 라이브러리, 외부 의존성 jar 등을 모두 포함하는 파일 목록이다.

sbt에서 클래스패스를 다루는 키는 대부분 `Classpath` 타입, 즉 `Seq[Attributed[File]]`을 값으로 가진다. `Attributed[File]`은 파일 하나에 임의의 메타데이터(예를 들어 어떤 모듈에서 왔는지)를 함께 담을 수 있는 래퍼다.

```scala
// Classpath에서 File 시퀀스 추출
val cp: Classpath = ...
val raw: Seq[File] = cp.files

// File 시퀀스에서 Classpath 생성
val raw: Seq[File] = ...
val cp: Classpath = raw.classpath

// 파일 하나를 Attributed[File]로 감싸기
val rawFile: File = ...
val af: Attributed[File] = Attributed.blank(rawFile)
```

`Attributed` 래핑 덕분에 클래스패스 값을 그대로 파일 목록처럼 다루면서도, 필요하면 각 항목에 붙은 부가 정보(analysis 결과, 모듈 ID 등)를 꺼내 쓸 수 있다.

---

## 2. Unmanaged vs Managed, Internal vs External

sbt는 클래스패스를 구성하는 요소를 두 축으로 분류한다.

| 분류 축 | 구분 | 의미 |
|---|---|---|
| 생성 주체 | **Unmanaged** | 개발자가 직접 놓아둔, 빌드가 추적하지 않는 파일 |
| 생성 주체 | **Managed** | 빌드 도구가 의존성 해석이나 소스 생성을 통해 만들어낸 파일 |
| 출처 | **Internal** | 같은 빌드 안의 다른 프로젝트에서 온 산출물 |
| 출처 | **External** | 빌드 바깥에서 온 것으로, Unmanaged와 Managed 클래스패스를 합친 것 |

이 네 가지 축을 조합해 다음과 같은 클래스패스 키가 정의된다.

| 키 | 설명 |
|---|---|
| `unmanagedClasspath` | `lib/` 디렉터리 등에 수동으로 넣어둔 jar |
| `managedClasspath` | Coursier/Ivy로 해석한 라이브러리 의존성 |
| `internalDependencyClasspath` | 프로젝트 간 의존성(다른 서브프로젝트의 산출물) |
| `externalDependencyClasspath` | unmanagedClasspath와 managedClasspath를 합친 것 |
| `dependencyClasspath` | internal + external 의존성 클래스패스 전체 |
| `fullClasspath` | 실제 컴파일/실행에 사용되는 최종 클래스패스(exportedProducts 포함) |
| `exportedProducts` | 해당 프로젝트가 컴파일 결과로 내보내는 클래스 디렉터리/jar |

실행 시점에 설정 디렉터리를 클래스패스에 얹고 싶다면 `unmanagedClasspath`에 직접 추가하면 된다.

```scala
Runtime / unmanagedClasspath += baseDirectory.value / "config"
```

이렇게 하면 `config` 디렉터리에 있는 `.properties` 파일 등을 런타임에 클래스패스 리소스로 읽어들일 수 있다.

각 키가 실제로 어떤 값을 갖는지 궁금하면 `inspect` 명령으로 세부 구성과 의존 관계를 확인한다.

```bash
sbt> inspect Compile / fullClasspath
```

---

## 3. 소스와 리소스 구성

소스와 리소스도 클래스패스와 같은 축으로 나뉜다.

**소스 관련 키**

| 키 | 설명 |
|---|---|
| `unmanagedSources` | `scalaSource`, `javaSource` 디렉터리 아래에서 직접 찾은 소스 파일 |
| `managedSources` | `sourceGenerators`가 생성한 소스 파일 |
| `sources` | `managedSources` + `unmanagedSources` |
| `sourceGenerators` | 소스 파일을 생성하는 태스크 목록 |

**리소스 관련 키**

| 키 | 설명 |
|---|---|
| `unmanagedResources` | `resourceDirectory` 아래에서 직접 찾은 리소스 파일 |
| `managedResources` | `resourceGenerators`가 생성한 리소스 파일 |
| `resourceGenerators` | 리소스 파일을 생성하는 태스크 목록 |

특정 파일을 소스 목록에서 제외하려면 `excludeFilter`를 스코프에 맞춰 설정한다.

```scala
unmanagedSources / excludeFilter := "butler.scala"
```

### 소스 생성 태스크 등록

`sourceGenerators`에 태스크를 더하면 컴파일 전에 해당 태스크가 실행되어 그 결과가 `managedSources`에 편입된다.

```scala
Compile / sourceGenerators +=
  generate((Compile / sourceManaged).value / "some_directory")
```

플러그인처럼 재사용 가능한 형태로 만들려면 별도의 명명된 태스크로 감싸는 편이 낫다.

```scala
val mySourceGenerator = taskKey[Seq[File]]("생성된 소스 파일 목록을 반환한다")

Compile / mySourceGenerator :=
  generate((Compile / sourceManaged).value / "some_directory")

Compile / sourceGenerators += (Compile / mySourceGenerator)
```

이렇게 분리해두면 사용자가 `mySourceGenerator` 태스크만 따로 실행하거나 재정의하기도 쉬워진다.

---

## 4. 컴파일러 플러그인 추가

Scala 컴파일러 플러그인은 컴파일 과정에 개입해 추가 검사나 코드 변환을 수행하는 확장 기능이다. sbt에서 플러그인을 쓰려면 우선 자동 컴파일러 플러그인 기능을 켠다.

```scala
autoCompilerPlugins := true
```

### addCompilerPlugin으로 추가

가장 간단한 방법은 `addCompilerPlugin` 헬퍼를 쓰는 것이다.

```scala
addCompilerPlugin("org.scala-tools.sxr" %% "sxr" % "0.3.0")
```

플러그인 jar은 `libraryDependencies`에 `compilerPlugin(...)`을 감싸 추가하는 방식으로도 지정할 수 있고, `lib/` 디렉터리에 직접 놓아둘 수도 있다.

### 플러그인에 옵션 전달

일부 플러그인은 추가 컴파일러 옵션이 필요하다. 예를 들어 Scala X-Ray는 소스 디렉터리 경로를 옵션으로 요구한다.

```scala
scalacOptions :=
  scalacOptions.value :+ ("-Psxr:base-directory:" +
    (Compile / scalaSource).value.getAbsolutePath)
```

플러그인 jar 경로를 수동으로 지정하려면 `-Xplugin` 옵션을 직접 사용한다.

```scala
scalacOptions += "-Xplugin:<path-to-sxr>/sxr-0.3.0.jar"
```

### Continuations 플러그인 예제

Scala 버전에 종속적인 플러그인을 추가할 때 흔히 쓰는 패턴이다.

```scala
val continuationsVersion = "1.0.3"
autoCompilerPlugins := true
addCompilerPlugin(
  "org.scala-lang.plugins" % "scala-continuations-plugin_2.12.2" % continuationsVersion
)
libraryDependencies +=
  "org.scala-lang.plugins" %% "scala-continuations-library" % continuationsVersion
scalacOptions += "-P:continuations:enable"
```

플러그인 artifact 이름 자체에 Scala 버전이 섞여 들어가는 경우, `scalaVersion.value`를 이용해 동적으로 조립한다.

```scala
libraryDependencies +=
  compilerPlugin(
    "org.scala-lang.plugins" % ("scala-continuations-plugin_" + scalaVersion.value) % continuationsVersion
  )
```

이렇게 등록한 플러그인은 `compile`과 `testCompile` 태스크 실행 시 자동으로 컴파일러에 적용된다.

---

## 5. Scala 버전 자동 관리

sbt는 기본적으로 `scalaVersion`에 지정한 버전의 Scala를 저장소에서 찾아 자동으로 내려받아 쓴다.

```scala
scalaVersion := "2.10.0"
```

이 한 줄만으로 컴파일러, 표준 라이브러리, REPL이 모두 해당 버전으로 동작한다.

### 표준 라이브러리 자동 추가 제어

기본적으로 `scala-library`가 `libraryDependencies`에 자동으로 더해진다. 이 동작을 끄고 싶으면 다음처럼 설정한다.

```scala
autoScalaLibrary := false
```

표준 라이브러리를 테스트 스코프에서만 쓰고 싶은 경우처럼 세밀한 제어가 필요하면, 자동 추가를 끈 뒤 직접 스코프를 지정해 추가한다.

```scala
autoScalaLibrary := false
libraryDependencies +=
  "org.scala-lang" % "scala-library" % scalaVersion.value % "test"
```

### 컴파일러/REPL용 도구 의존성 제어

컴파일과 REPL 실행에는 `scala-compiler` jar이 필요하다. sbt는 기본적으로 이를 자동 관리하지만, `managedScalaInstance`를 꺼서 수동으로 제어할 수도 있다.

```scala
managedScalaInstance := false
```

수동 제어 시 `scala-tool` configuration에 필요한 jar을 직접 나열한다.

```scala
managedScalaInstance := false
ivyConfigurations += Configurations.ScalaTool
libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-library" % scalaVersion.value,
  "org.scala-lang" % "scala-compiler" % scalaVersion.value % "scala-tool"
)
```

`scala-reflect`처럼 추가 모듈이 필요하면 `libraryDependencies`에 같은 방식으로 더한다.

```scala
libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value
```

---

## 6. 로컬 Scala 설치 사용

원격 저장소 대신 로컬 디스크에 있는 Scala 배포판을 그대로 쓰고 싶다면 `scalaHome`을 지정한다.

```scala
scalaHome := Some(file("/home/user/scala-2.10/"))
```

`scalaHome`을 지정하면 해당 경로의 `lib/scala-library.jar`이 언매니지드 클래스패스에 추가되고, `lib/scala-compiler.jar`이 컴파일과 REPL 실행에 쓰인다.

### 관리 의존성과 혼합

`scalaHome`을 쓰면서도 일부 모듈은 저장소에서 관리 의존성으로 받아올 수 있다.

```scala
scalaHome := Some(file("/home/user/scala-2.10/"))
libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
```

sbt는 먼저 `scalaHome` 디렉터리 안에서 일치하는 jar을 찾고, 있으면 그 파일을 우선 사용한다.

### 언매니지드 의존성만으로 구성

관리 의존성 해석을 아예 거치지 않고, 로컬 Scala 설치의 jar들을 그대로 언매니지드 jar로 등록할 수도 있다.

```scala
scalaHome := Some(file("/home/user/scala-2.10/"))
Compile / unmanagedJars ++= scalaInstance.value.jars
```

---

## 7. sbt 자체의 Scala 버전

`scalaVersion`이 조정하는 것은 어디까지나 **프로젝트가 빌드에 사용할** Scala 버전이다. sbt 런처 자체가 내부적으로 실행되는 Scala 버전은 별개이며 사용자가 변경할 수 없다. 예를 들어 sbt 1.10.10은 내부적으로 Scala 2.12.18을 사용해 동작한다. 이 버전과 관련한 리소스 관리는 sbt 런처가 전담하므로, 빌드 정의에서 신경 쓸 필요는 없다.

---

## 8. Forking으로 별도 JVM에서 실행

sbt는 기본적으로 `run`이나 `test` 태스크를 sbt 자신과 같은 JVM 프로세스 안에서 실행한다. 그러나 애플리케이션이 `System.exit`을 호출하거나, 별도의 JVM 옵션·환경 변수·작업 디렉터리가 필요하거나, sbt 프로세스 자체에 영향을 주지 않고 격리해서 실행하고 싶을 때는 forking(별도 JVM 프로세스로 실행)을 켠다.

### fork 활성화

`fork` 키는 스코프에 따라 적용 범위를 세밀하게 조절할 수 있다.

```scala
fork := true                  // 모든 run/test 태스크에 forking 적용
Compile / run / fork := true  // Compile 스코프의 run만 forking
Test / run / fork := true     // Test 스코프의 run만 forking
Test / fork := true           // 모든 test 태스크에 forking 적용
```

### 작업 디렉터리 지정

포크된 프로세스가 실행될 작업 디렉터리를 바꾸고 싶으면 `baseDirectory`를 스코프별로 재정의한다.

```scala
run / baseDirectory := file("/path/to/working/directory/")
Compile / run / baseDirectory := file("/path/to/working/directory/")
Test / run / baseDirectory := file("/path/to/working/directory/")
Test / baseDirectory := file("/path/to/working/directory/")
```

### JVM 옵션 전달

`javaOptions`로 힙 크기 등 JVM 옵션을 지정한다.

```scala
run / javaOptions += "-Xmx8G"
Test / run / javaOptions += "-Xmx8G"
Test / javaOptions += "-Xmx8G"
```

### 사용할 JRE/JDK 지정

`javaHome`으로 sbt가 실행 중인 JVM과 다른 JRE/JDK를 지정할 수 있다. 이 키를 설정하면 자동으로 forking이 강제된다.

```scala
javaHome := Some(file("/path/to/jre/"))
run / javaHome := Some(file("/path/to/jre/"))
```

### 출력 스트림 처리 전략

포크된 프로세스의 표준 출력을 어떻게 다룰지 `outputStrategy`로 정한다.

```scala
outputStrategy := Some(StdoutOutput)
outputStrategy := Some(CustomOutput(someStream: OutputStream))
outputStrategy := Some(LoggedOutput(log: Logger))
outputStrategy := Some(BufferedOutput(log: Logger))
```

- `StdoutOutput`: 포크된 프로세스의 출력을 sbt의 표준 출력으로 그대로 전달한다.
- `CustomOutput`: 지정한 `OutputStream`으로 보낸다.
- `LoggedOutput`: sbt 로거를 거쳐 출력한다.
- `BufferedOutput`: 로거를 거치되 다른 로그와 섞이지 않도록 버퍼링한다.

### 표준 입력 연결

포크된 프로세스가 sbt 콘솔의 표준 입력을 받아야 한다면(예: 콘솔에서 값을 입력받는 프로그램) 다음을 설정한다.

```scala
run / connectInput := true
```

기본적으로 포크된 프로세스는 sbt와 같은 Java/Scala 버전, 작업 디렉터리, JVM 옵션을 그대로 물려받는다.

---

## 9. Fork API 직접 사용

`run`/`test` 태스크가 아니라 임의의 커스텀 태스크에서 프로세스를 직접 포크하고 싶다면 `Fork` API를 사용한다.

```scala
val options = ForkOptions(...)
val arguments: Seq[String] = ...
val mainClass: String = ...
val exitCode: Int = Fork.java(options, mainClass +: arguments)
```

`ForkOptions`에는 환경 변수, 작업 디렉터리, JVM 경로 등을 세밀하게 지정할 수 있다.

```scala
val cwd: File = ...
val javaDir: File = ...
val options = ForkOptions(
  envVars = Map("KEY" -> "value"),
  workingDirectory = Some(cwd),
  javaHome = Some(javaDir)
)
```

이 API는 태스크 안에서 임의의 메인 클래스를 실행하거나, 별도 프로세스로 스크립트를 돌리는 등 `run`/`test`가 다루지 않는 상황에서 직접 프로세스 격리 실행을 제어할 때 쓴다.

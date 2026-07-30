# sbt 설정: Classpath, 컴파일러, 전역 설정, 병렬 실행

## sbt Classpath 구성, 컴파일러 플러그인, Scala 설정, Forking

> 원본: https://www.scala-sbt.org/1.x/docs/Classpaths.html, https://www.scala-sbt.org/1.x/docs/Compiler-Plugins.html, https://www.scala-sbt.org/1.x/docs/Configuring-Scala.html, https://www.scala-sbt.org/1.x/docs/Forking.html

---

### 목차

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

### 1. Classpath 기본 구조

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

### 2. Unmanaged vs Managed, Internal vs External

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

### 3. 소스와 리소스 구성

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

#### 소스 생성 태스크 등록

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

### 4. 컴파일러 플러그인 추가

Scala 컴파일러 플러그인은 컴파일 과정에 개입해 추가 검사나 코드 변환을 수행하는 확장 기능이다. sbt에서 플러그인을 쓰려면 우선 자동 컴파일러 플러그인 기능을 켠다.

```scala
autoCompilerPlugins := true
```

#### addCompilerPlugin으로 추가

가장 간단한 방법은 `addCompilerPlugin` 헬퍼를 쓰는 것이다.

```scala
addCompilerPlugin("org.scala-tools.sxr" %% "sxr" % "0.3.0")
```

플러그인 jar은 `libraryDependencies`에 `compilerPlugin(...)`을 감싸 추가하는 방식으로도 지정할 수 있고, `lib/` 디렉터리에 직접 놓아둘 수도 있다.

#### 플러그인에 옵션 전달

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

#### Continuations 플러그인 예제

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

### 5. Scala 버전 자동 관리

sbt는 기본적으로 `scalaVersion`에 지정한 버전의 Scala를 저장소에서 찾아 자동으로 내려받아 쓴다.

```scala
scalaVersion := "2.10.0"
```

이 한 줄만으로 컴파일러, 표준 라이브러리, REPL이 모두 해당 버전으로 동작한다.

#### 표준 라이브러리 자동 추가 제어

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

#### 컴파일러/REPL용 도구 의존성 제어

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

### 6. 로컬 Scala 설치 사용

원격 저장소 대신 로컬 디스크에 있는 Scala 배포판을 그대로 쓰고 싶다면 `scalaHome`을 지정한다.

```scala
scalaHome := Some(file("/home/user/scala-2.10/"))
```

`scalaHome`을 지정하면 해당 경로의 `lib/scala-library.jar`이 언매니지드 클래스패스에 추가되고, `lib/scala-compiler.jar`이 컴파일과 REPL 실행에 쓰인다.

#### 관리 의존성과 혼합

`scalaHome`을 쓰면서도 일부 모듈은 저장소에서 관리 의존성으로 받아올 수 있다.

```scala
scalaHome := Some(file("/home/user/scala-2.10/"))
libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
```

sbt는 먼저 `scalaHome` 디렉터리 안에서 일치하는 jar을 찾고, 있으면 그 파일을 우선 사용한다.

#### 언매니지드 의존성만으로 구성

관리 의존성 해석을 아예 거치지 않고, 로컬 Scala 설치의 jar들을 그대로 언매니지드 jar로 등록할 수도 있다.

```scala
scalaHome := Some(file("/home/user/scala-2.10/"))
Compile / unmanagedJars ++= scalaInstance.value.jars
```

---

### 7. sbt 자체의 Scala 버전

`scalaVersion`이 조정하는 것은 어디까지나 **프로젝트가 빌드에 사용할** Scala 버전이다. sbt 런처 자체가 내부적으로 실행되는 Scala 버전은 별개이며 사용자가 변경할 수 없다. 예를 들어 sbt 1.10.10은 내부적으로 Scala 2.12.18을 사용해 동작한다. 이 버전과 관련한 리소스 관리는 sbt 런처가 전담하므로, 빌드 정의에서 신경 쓸 필요는 없다.

---

### 8. Forking으로 별도 JVM에서 실행

sbt는 기본적으로 `run`이나 `test` 태스크를 sbt 자신과 같은 JVM 프로세스 안에서 실행한다. 그러나 애플리케이션이 `System.exit`을 호출하거나, 별도의 JVM 옵션·환경 변수·작업 디렉터리가 필요하거나, sbt 프로세스 자체에 영향을 주지 않고 격리해서 실행하고 싶을 때는 forking(별도 JVM 프로세스로 실행)을 켠다.

#### fork 활성화

`fork` 키는 스코프에 따라 적용 범위를 세밀하게 조절할 수 있다.

```scala
fork := true                  // 모든 run/test 태스크에 forking 적용
Compile / run / fork := true  // Compile 스코프의 run만 forking
Test / run / fork := true     // Test 스코프의 run만 forking
Test / fork := true           // 모든 test 태스크에 forking 적용
```

#### 작업 디렉터리 지정

포크된 프로세스가 실행될 작업 디렉터리를 바꾸고 싶으면 `baseDirectory`를 스코프별로 재정의한다.

```scala
run / baseDirectory := file("/path/to/working/directory/")
Compile / run / baseDirectory := file("/path/to/working/directory/")
Test / run / baseDirectory := file("/path/to/working/directory/")
Test / baseDirectory := file("/path/to/working/directory/")
```

#### JVM 옵션 전달

`javaOptions`로 힙 크기 등 JVM 옵션을 지정한다.

```scala
run / javaOptions += "-Xmx8G"
Test / run / javaOptions += "-Xmx8G"
Test / javaOptions += "-Xmx8G"
```

#### 사용할 JRE/JDK 지정

`javaHome`으로 sbt가 실행 중인 JVM과 다른 JRE/JDK를 지정할 수 있다. 이 키를 설정하면 자동으로 forking이 강제된다.

```scala
javaHome := Some(file("/path/to/jre/"))
run / javaHome := Some(file("/path/to/jre/"))
```

#### 출력 스트림 처리 전략

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

#### 표준 입력 연결

포크된 프로세스가 sbt 콘솔의 표준 입력을 받아야 한다면(예: 콘솔에서 값을 입력받는 프로그램) 다음을 설정한다.

```scala
run / connectInput := true
```

기본적으로 포크된 프로세스는 sbt와 같은 Java/Scala 버전, 작업 디렉터리, JVM 옵션을 그대로 물려받는다.

---

### 9. Fork API 직접 사용

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

---

## 전역 설정, Java 혼합 빌드, 파일 매핑, 로컬 Scala

> 원본: https://www.scala-sbt.org/1.x/docs/Global-Settings.html , https://www.scala-sbt.org/1.x/docs/Java-Sources.html , https://www.scala-sbt.org/1.x/docs/Mapping-Files.html , https://www.scala-sbt.org/1.x/docs/Local-Scala.html

---

### 목차

1. [전역 설정 (~/.sbt)](#1-전역-설정-home사sbt)
2. [Java 소스와 혼합 빌드](#2-java-소스와-혼합-빌드)
3. [파일 매핑 (Mapping Files)](#3-파일-매핑-mapping-files)
4. [로컬 커스텀 Scala 배포판 사용](#4-로컬-커스텀-scala-배포판-사용)

---

### 1. 전역 설정 (~/.sbt)

sbt는 특정 프로젝트에 국한되지 않고 사용자의 모든 프로젝트에 공통으로 적용할 설정을 `$HOME/.sbt` 아래에 둘 수 있게 지원한다. 방법은 두 가지다.

| 방법 | 위치 | 특징 |
|---|---|---|
| 전역 설정 파일 | `$HOME/.sbt/1.0/global.sbt` | 단순 키-값 설정 위주 |
| 전역 플러그인 | `$HOME/.sbt/1.0/plugins/` | 코드(AutoPlugin)까지 포함해 모든 프로젝트에 주입 |

#### 1.1 기본 전역 설정 파일

`$HOME/.sbt/1.0/global.sbt`에 작성한 설정은 sbt로 빌드하는 모든 프로젝트에 적용된다. 셸 프롬프트를 커스터마이징하는 예제는 다음과 같다.

```scala
shellPrompt := { state =>
  "sbt (%s)> ".format(Project.extract(state).currentProject.id)
}
```

`$HOME/.sbt/1.0/plugins/`에 추가한 플러그인이 제공하는 설정 키도 이 파일에서 그대로 사용할 수 있다. 다만 전역 파일에서는 프로젝트별 `build.sbt`와 달리 이름이 자동으로 스코프에 잡히지 않으므로, 플러그인이 제공하는 키는 정규화된(fully-qualified) 이름으로 참조해야 한다.

```scala
com.typesafe.sbteclipse.core.EclipsePlugin.EclipseKeys.withSource := true
```

#### 1.2 전역 플러그인을 이용한 전역 설정

`$HOME/.sbt/1.0/plugins/` 디렉터리는 그 자체로 하나의 플러그인 프로젝트처럼 동작한다. 이 디렉터리에 `build.sbt`를 두고 플러그인을 추가하면,

```scala
addSbtPlugin("org.example" % "plugin" % "1.0")
```

여기서 정의한 설정과 코드는 사용자가 빌드하는 **모든** 프로젝트에 적용된다. 단순 설정 파일과 달리, 이 디렉터리에는 `AutoPlugin`을 직접 작성해 넣을 수도 있다.

```scala
// ShellPrompt.scala
object ShellPrompt extends AutoPlugin {
  override def trigger = allRequirements
  override def projectSettings = Seq(
    shellPrompt := { state =>
      "sbt (%s)> ".format(Project.extract(state).currentProject.id) }
  )
}
```

`trigger = allRequirements`로 지정했으므로 이 플러그인은 별도 설정 없이 모든 프로젝트에 자동 적용된다.

`$HOME/.sbt/1.0/plugins/` 디렉터리는 개별 프로젝트의 `project/` 디렉터리와 사실상 동일한 위치를 가진다. 즉 여기서 정의한 설정과 코드는 마치 모든 프로젝트의 `project/` 디렉터리 안에 있는 것처럼 취급된다. 이 성질을 이용하면 새 플러그인을 정식으로 퍼블리시하기 전에 전역 플러그인 디렉터리에서 먼저 실험해볼 수 있다.

#### 1.3 활용 정리

- 개인 편의 설정(프롬프트, 로깅 레벨 등)은 `global.sbt`에 둔다.
- 모든 프로젝트에서 공통으로 쓰고 싶은 플러그인(코드 포매터, 이클립스/인텔리제이 연동 등)은 `plugins/` 디렉터리에 추가한다.
- 전역 설정은 팀 전체가 공유하는 저장소 빌드 설정과는 별개로, 개인 로컬 환경에만 적용된다는 점에 유의한다.

---

### 2. Java 소스와 혼합 빌드

sbt는 Scala 전용 빌드 도구가 아니라 Java 소스도 함께 컴파일할 수 있는 혼합 빌드 도구다. 별도 설정 없이도 관례적인 디렉터리 구조를 따르는 Java 소스가 자동으로 인식된다.

| 디렉터리 | 대상 태스크 |
|---|---|
| `src/main/java` | `compile` |
| `src/test/java` | `test:compile` |

#### 2.1 javac 옵션 지정

Java 컴파일러(`javac`)에 전달할 옵션은 `javacOptions` 키로 설정한다.

```scala
javacOptions += "-g:none"
```

여러 개의 옵션(특히 옵션과 그 인자가 쌍을 이루는 경우)은 시퀀스로 한 번에 추가한다.

```scala
javacOptions ++= Seq("-source", "1.5")
```

#### 2.2 컴파일 순서 제어: compileOrder

Scala와 Java 소스가 섞여 있을 때 어느 쪽을 먼저 컴파일할지는 `compileOrder` 키로 제어한다. `CompileOrder`가 가질 수 있는 값은 다음 세 가지다.

| 값 | 의미 |
|---|---|
| `CompileOrder.Mixed` (기본값) | Scala와 Java를 함께 컴파일러에 넘겨 순환 의존까지 지원 |
| `CompileOrder.JavaThenScala` | Java 소스를 먼저 컴파일한 뒤 Scala 소스를 컴파일 |
| `CompileOrder.ScalaThenJava` | Scala 소스를 먼저 컴파일한 뒤 Java 소스를 컴파일 |

```scala
compileOrder := CompileOrder.JavaThenScala
```

컴파일 순서는 설정 범위(configuration scope)별로 다르게 지정할 수도 있다. 예를 들어 메인 소스는 `JavaThenScala`로, 테스트 소스는 기본값인 `Mixed`로 유지하려면 다음과 같이 작성한다.

```scala
Compile / compileOrder := CompileOrder.JavaThenScala
Test / compileOrder := CompileOrder.Mixed
```

#### 2.3 혼합 컴파일에서 알려진 문제

Scala 컴파일러와 `javac`를 한 번에 돌리는 `Mixed` 모드에서는 다음과 같은 상호운용 제약이 알려져 있다.

- Scala 컴파일러가 Java 쪽에서 정의한, 리터럴이 아닌 상수(non-literal constant) 값을 인식하지 못하는 경우가 있다.
- 그런 값을 어노테이션의 인자로 사용하면 컴파일이 거부될 수 있다.
- Scala 2.11.4 이후 버전에서 Java 어노테이션에 붙은 `@Retention` 메타 어노테이션을 인식하지 못하는 문제가 보고된 바 있다.

이런 문제가 발생하면 `compileOrder`를 `JavaThenScala`나 `ScalaThenJava`로 바꿔 두 컴파일러를 분리 실행하는 방식으로 우회할 수 있다.

#### 2.4 Java 전용 프로젝트로 좁히기

프로젝트에 Scala 소스가 전혀 없는 순수 Java 프로젝트라면, 굳이 존재하지도 않는 `src/main/scala` 디렉터리를 소스 경로 목록에 포함시킬 이유가 없다. `unmanagedSourceDirectories`를 Java 소스 디렉터리만으로 재설정하면 된다.

```scala
Compile / unmanagedSourceDirectories := (Compile / javaSource).value :: Nil
Test / unmanagedSourceDirectories := (Test / javaSource).value :: Nil
```

이렇게 하면 소스 디렉터리 탐색 범위가 명확해지고, 불필요한 Scala 컴파일러 초기화 비용도 줄어든다.

---

### 3. 파일 매핑 (Mapping Files)

`package`, `packageSrc`, `packageDoc` 같은 태스크는 산출물을 만들 때 입력 파일과, 그 파일이 결과 아티팩트(jar 등) 안에서 가질 상대 경로를 짝지은 `Seq[(File, String)]` 타입의 값을 필요로 한다. 이런 짝을 **매핑(mapping)** 이라 부른다. sbt는 `PathFinder`와 `Path` 객체가 제공하는 몇 가지 메서드로 이 매핑 시퀀스를 손쉽게 구성할 수 있게 해준다.

핵심은 `pair` 메서드다. `Seq[File]`에 `pair`를 호출하면서 매핑 함수(`File => Option[String]` 또는 `File => Option[File]`)를 인자로 넘기면 `Seq[(File, String)]`(또는 `Seq[(File, File)]`)이 만들어진다.

#### 3.1 relativeTo: 기준 디렉터리 상대 경로로 변환

`relativeTo(baseDirectories)`는 각 파일을 주어진 기준 디렉터리들 중 하나를 기준으로 한 상대 경로로 바꾼다. 기준 디렉터리가 여러 개이면 파일을 포함하는 첫 번째 디렉터리를 사용한다.

```scala
import Path.relativeTo

val files: Seq[File] = file("/a/b/C.scala") :: Nil
val baseDirectories: Seq[File] = file("/a") :: Nil

val mappings: Seq[(File, String)] = files pair relativeTo(baseDirectories)

val expected = (file("/a/b/C.scala") -> "b/C.scala") :: Nil
```

#### 3.2 rebase: 상대 경로로 바꾼 뒤 새 접두사 붙이기

`rebase(baseDirectories, prefix)`는 `relativeTo`처럼 상대 경로를 구한 다음, 그 앞에 새로운 접두사를 덧붙인다. 접두사는 문자열일 수도 있고 `File`일 수도 있다.

문자열 접두사(결과가 `String` 매핑):

```scala
import Path.rebase

val files: Seq[File] = file("/a/b/C.scala") :: Nil
val baseDirectories: Seq[File] = file("/a") :: Nil

val mappings: Seq[(File, String)] = files pair rebase(baseDirectories, "pre/")

val expected = (file("/a/b/C.scala") -> "pre/b/C.scala") :: Nil
```

`File` 접두사(결과가 `File` 매핑, 즉 파일을 다른 디렉터리 트리로 복사할 때 유용):

```scala
val newBase: File = file("/new/base")
val mappings: Seq[(File, File)] = files pair rebase(baseDirectories, newBase)

val expected = (file("/a/b/C.scala") -> file("/new/base/b/C.scala")) :: Nil
```

#### 3.3 flat: 디렉터리 구조를 무시하고 파일명만 사용

`flat`은 원래 경로 정보를 버리고 파일명만 남긴다. 여러 디렉터리에 흩어진 파일을 한 디렉터리에 모아 담을 때 사용한다.

```scala
import Path.flat

val files: Seq[File] = file("/a/b/C.scala") :: Nil
val mappings: Seq[(File, String)] = files pair flat

val expected = (file("/a/b/C.scala") -> "C.scala") :: Nil
```

`File` 대상 디렉터리를 지정하는 변형(`flat(newBase)`)도 있다.

```scala
val newBase: File = file("/new/base")
val mappings: Seq[(File, File)] = files pair flat(newBase)

val expected = (file("/a/b/C.scala") -> file("/new/base/C.scala")) :: Nil
```

#### 3.4 대안 조합: `|` 연산자

여러 매핑 전략 중 앞의 전략이 실패(해당 파일에 대해 `None` 반환)했을 때 다음 전략으로 넘어가도록 `|` 연산자로 이어붙일 수 있다.

```scala
import Path.relativeTo

val files: Seq[File] = file("/a/b/C.scala") :: file("/zzz/D.scala") :: Nil
val baseDirectories: Seq[File] = file("/a") :: Nil

val mappings: Seq[(File, String)] = files pair (relativeTo(baseDirectories) | flat)
```

위 예에서 `C.scala`는 `/a` 아래에 있으므로 `relativeTo`가 성공해 `"b/C.scala"`로 매핑되지만, `D.scala`는 `/zzz` 아래에 있어 `relativeTo`가 실패하고 `flat`으로 넘어가 `"D.scala"`로 매핑된다.

#### 3.5 정리

| 함수 | 입력 | 동작 |
|---|---|---|
| `relativeTo(bases)` | 기준 디렉터리 목록 | 파일을 포함하는 첫 기준 디렉터리에 대한 상대 경로 생성 |
| `rebase(bases, prefix)` | 기준 디렉터리 목록 + 접두사 | 상대 경로를 구한 뒤 접두사를 붙임 |
| `flat` / `flat(newBase)` | (없음) / 대상 디렉터리 | 경로를 버리고 파일명만 사용 |
| `a \| b` | 매핑 함수 두 개 | `a`가 실패한 파일에 한해 `b` 적용 |

이 함수들을 조합하면 `package` 계열 태스크의 `mappings` 키를 원하는 아티팩트 레이아웃에 맞춰 세밀하게 구성할 수 있다.

---

### 4. 로컬 커스텀 Scala 배포판 사용

빌드 도구가 저장소에서 내려받은 표준 Scala 배포판이 아니라, 로컬에 직접 빌드해둔 커스텀 Scala 배포판으로 프로젝트를 컴파일하고 싶은 경우가 있다(예: Scala 컴파일러나 표준 라이브러리 자체를 수정하며 개발할 때). sbt는 이를 위해 `scalaHome` 설정을 제공한다.

#### 4.1 scalaHome 설정

`scalaHome`은 `Option[File]` 타입의 설정 키이며, 로컬 Scala 배포판이 설치된 디렉터리를 가리키면 된다.

```scala
scalaHome := Some(file("/path/to/scala"))
```

이 설정을 켜면 sbt는 저장소에서 Scala 아티팩트를 내려받는 대신, 지정한 경로에 있는 컴파일러와 라이브러리 jar들을 사용해 빌드를 수행한다.

#### 4.2 제약 사항

- `scalaHome`을 지정하면 이 값이 `scalaVersion` 설정보다 우선하며, `scalaVersion`은 사실상 무시된다.
- 로컬 Scala 배포판을 사용하는 프로젝트는 여러 Scala 버전을 대상으로 하는 크로스 빌드(`+` 명령, `crossScalaVersions`)와 함께 쓸 수 없다. 크로스 빌드는 저장소에서 여러 버전의 표준 아티팩트를 받아오는 방식으로 동작하는데, `scalaHome`은 항상 고정된 하나의 로컬 배포판만 가리키기 때문이다.

#### 4.3 대화형 세션에서 변경 사항 반영하기

로컬 Scala 배포판의 소스를 수정하고 다시 빌드한 뒤, 이미 실행 중인 sbt 대화형 세션에 그 변경을 반영하려면 클래스 로더를 새로 고쳐야 한다. `reload` 명령을 사용한다.

```
> reload
```

`reload`를 실행하면 sbt가 클래스패스를 다시 읽어들이므로, 방금 재컴파일한 로컬 Scala 배포판의 최신 결과물이 이후 컴파일 작업에 곧바로 반영된다.

---

## sbt 매크로 프로젝트, 경로 API, 병렬 실행, 외부 프로세스

> 원본: https://www.scala-sbt.org/1.x/docs/Macro-Projects.html, https://www.scala-sbt.org/1.x/docs/Paths.html, https://www.scala-sbt.org/1.x/docs/Parallel-Execution.html, https://www.scala-sbt.org/1.x/docs/Process.html

---

### 목차

1. [매크로 프로젝트 분리](#1-매크로-프로젝트-분리)
2. [경로(PathFinder) API](#2-경로pathfinder-api)
3. [병렬 태스크 실행 제어](#3-병렬-태스크-실행-제어)
4. [외부 프로세스 실행](#4-외부-프로세스-실행)

---

### 1. 매크로 프로젝트 분리

#### 1.1 왜 분리해야 하는가

Scala 컴파일러는 매크로 구현체를 사용하는 시점보다 먼저 컴파일해 두어야 한다는 제약을 가진다. 같은 컴파일 단위 안에 매크로 정의와 매크로 호출을 함께 두면 컴파일 순서 문제로 빌드가 실패하므로, 매크로 구현을 별도 서브프로젝트로 분리하고 사용하는 쪽 프로젝트가 이를 의존하는 구조를 취해야 한다.

일반적으로 다음과 같이 역할을 나눈다.

| 서브프로젝트 | 역할 |
|--------------|------|
| `macro` | 매크로 구현체를 담는 프로젝트 |
| `core` | 매크로를 실제로 사용하는 메인 프로젝트, `macro`를 의존 |
| `util` (선택) | `macro`와 `core`가 함께 참조하는 공용 코드 |

#### 1.2 기본 빌드 구성

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

#### 1.3 공용 인터페이스 프로젝트 추가하기

매크로와 그 사용처가 공통 타입이나 헬퍼를 공유해야 한다면 `util` 프로젝트를 추가로 둔다.

```scala
lazy val util = (project in file("util"))
  .settings(commonSettings)

lazy val core = (project in file("core"))
  .dependsOn(macroSub, util)

lazy val macroSub = (project in file("macro"))
  .dependsOn(util)
```

#### 1.4 매크로 프로젝트를 별도로 퍼블리시하지 않기

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

### 2. 경로(PathFinder) API

#### 2.1 기본 타입

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

#### 2.2 PathFinder로 파일 집합 찾기

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

#### 2.3 파일 필터 조합

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

#### 2.4 결과를 문자열로 변환하기

| 메서드 | 반환 형태 |
|--------|-----------|
| `toString` | 절대 경로를 한 줄에 하나씩 나열 (디버깅용) |
| `absString` | 절대 경로를 플랫폼 구분자로 이어 붙인 문자열 |
| `getPaths` | 절대 경로 문자열의 `Seq[String]` |

#### 2.5 참고 사항

- `get`은 호출할 때마다 파일 목록을 새로 계산하므로 파일시스템 변경 사항이 즉시 반영된다.
- 존재하지 않는 파일은 결과에서 제외되며, 빈 `PathFinder`의 `get`은 빈 `Seq[File]()`이다.
- 기준 경로 자체가 존재하지 않으면 그 위에 적용한 선택 연산도 빈 결과를 낸다.

---

### 3. 병렬 태스크 실행 제어

#### 3.1 태스크 순서와 병렬 실행의 기본 원칙

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

#### 3.2 태스크 태깅과 동시성 제한

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

#### 3.3 기본 제공 태그

| 분류 | 태그 |
|------|------|
| 의미 기반 | `Compile`, `Test`, `Publish`, `Update`, `Untagged`, `All` |
| 자원 기반 | `Network`, `Disk`, `CPU` |

`compile` 태스크는 기본적으로 `Compile`, `CPU` 태그를, `test`는 `Test` 태그를, `update`는 `Update`, `Network` 태그를, 퍼블리시 관련 태스크는 `Publish`, `Network` 태그를 갖는다.

#### 3.4 고급 제한 방법

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

#### 3.5 기본 동작과 하위 호환성

`concurrentRestrictions`를 별도로 지정하지 않으면 sbt는 다음과 동등한 기본값을 사용한다.

```scala
Global / concurrentRestrictions := {
  val max = Runtime.getRuntime.availableProcessors
  Tags.limitAll(if(parallelExecution.value) max else 1) :: Nil
}
```

즉 `parallelExecution` 설정값이 `true`면 사용 가능한 코어 수만큼, `false`면 1개로 전체 동시 실행 개수를 제한한다. 기존에 널리 쓰이던 `Test / parallelExecution := false` 같은 설정도 이 체계 위에서 그대로 동작한다.

#### 3.6 커스텀 태그 정의하기

새로운 태그가 필요하면 `Tags.Tag`에 이름을 넘겨 직접 만든다.

```scala
val Custom = Tags.Tag("custom")

def aImpl = Def.task { ... } tag(Custom)
aCustomTask := aImpl.value

Global / concurrentRestrictions +=
  Tags.limit(Custom, 1)
```

---

### 4. 외부 프로세스 실행

#### 4.1 개요

sbt는 외부 명령을 실행할 때 Scala 표준 라이브러리의 프로세스 API(`scala.sys.process`)를 그대로 활용한다. 빌드 스크립트에서 사용하려면 다음을 임포트한다.

```scala
import scala.sys.process._
```

#### 4.2 명령 실행과 종료 코드

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

#### 4.3 작업 디렉터리와 환경변수 지정

작업 디렉터리나 환경변수를 지정하려면 `Process`를 명시적으로 생성한다.

```scala
Process("ls" :: "-l" :: Nil, Path.userHome, "key1" -> value1, "key2" -> value2) ! log
```

#### 4.4 흐름 제어 연산자

`#` 접두 연산자는 셸의 제어 흐름과 유사하게 동작한다.

| 연산자 | 의미 |
|--------|------|
| `a #&& b` | `a`를 실행하고, 종료 코드가 0이면 `b`도 실행 |
| `a #\|\| b` | `a`를 실행하고, 종료 코드가 0이 아니면 `b`도 실행 |
| `a #\| b` | `a`의 출력을 `b`의 입력으로 파이프 |

#### 4.5 입출력 리디렉션

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

#### 4.6 출력을 문자열로 받기

```scala
val listed: String = "ls" !!
val lines2: Stream[String] = "ls" lines_!
```

`!!`는 실행 결과 전체를 하나의 `String`으로 반환하고, `lines_!`는 줄 단위 `Stream[String]`으로 반환한다.

#### 4.7 실전 예제

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

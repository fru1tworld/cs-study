# sbt CLI, console-project, 크로스 빌드, 설정 조회

> 원본: https://www.scala-sbt.org/1.x/docs/Command-Line-Reference.html, https://www.scala-sbt.org/1.x/docs/Console-Project.html, https://www.scala-sbt.org/1.x/docs/Cross-Build.html, https://www.scala-sbt.org/1.x/docs/Inspecting-Settings.html

---

## 목차

1. [CLI 실행 방식](#1-cli-실행-방식)
2. [커맨드 · 태스크 · 세팅](#2-커맨드-태스크-세팅)
3. [커맨드 결합 문법](#3-커맨드-결합-문법)
4. [자주 쓰는 태스크/커맨드](#4-자주-쓰는-태스크커맨드)
5. [JVM 옵션과 시스템 프로퍼티](#5-jvm-옵션과-시스템-프로퍼티)
6. [console-project](#6-console-project)
7. [크로스 빌드](#7-크로스-빌드)
8. [설정 조회: inspect / show](#8-설정-조회-inspect--show)

---

## 1. CLI 실행 방식

sbt는 세 가지 방식으로 실행할 수 있다.

| 방식 | 명령 | 설명 |
|---|---|---|
| 대화형 셸 | `sbt` | 셸에 진입해 커맨드를 반복 입력 |
| 배치 모드(인자로 전달) | `sbt clean compile test` | 커맨드를 인자로 나열, 순서대로 실행 후 종료 |
| 배치 모드(파일) | `sbt < commands.txt` | 파일에 한 줄씩 적힌 커맨드를 순차 실행 |

```bash
sbt
> compile
> test
> run
> exit
```

```bash
sbt clean compile test publish
```

```bash
cat > commands.txt << EOF
clean
compile
test
EOF
sbt < commands.txt
```

`<` 로 읽어들이는 파일은 공백 줄과 `#`으로 시작하는 주석 줄을 무시한다.

---

## 2. 커맨드 · 태스크 · 세팅

sbt를 이루는 세 요소는 성격이 다르다.

- **커맨드(command)**: 빌드 정의 자체를 조작하는 최상위 동작. `reload`, `project`, `set` 등.
- **태스크(task)**: 빌드 정의 안에서 실행되는 작업. `compile`, `test`, `run` 등.
- **세팅(setting)**: 빌드 시점에 한 번 계산되는 값. `name`, `version`, `libraryDependencies` 등.

태스크는 설정 축(`Compile`, `Test` 등)과 프로젝트 축을 기준으로 범위가 정해진다. `Test/compile`은 테스트 소스 컴파일, 그냥 `compile`은 메인 소스 컴파일을 가리킨다.

### 2.1 프로젝트 레벨 태스크

| 태스크 | 설명 |
|---|---|
| `clean` | `target` 디렉터리 등 생성된 파일을 모두 삭제 |
| `update` | 외부 의존성을 해결하고 다운로드 |
| `publishLocal` | 로컬 Ivy 저장소에 아티팩트를 퍼블리시 |
| `publish` | `publishTo`로 지정한 저장소에 퍼블리시 |

### 2.2 설정 레벨 태스크

| 태스크 | 설명 |
|---|---|
| `compile` | `src/main/scala` 소스를 컴파일 |
| `Test/compile` | `src/test/scala` 테스트 소스를 컴파일 |
| `console` | 컴파일된 소스, `lib` 디렉터리 jar, 의존성 라이브러리를 클래스패스에 올린 Scala 인터프리터 시작 |
| `consoleQuick` | 컴파일을 강제하지 않고 의존성만으로 인터프리터 시작 |
| `consoleProject` | 빌드 정의와 sbt 자체가 클래스패스에 있는 대화형 세션 진입 (6절 참고) |
| `doc` | scaladoc으로 API 문서 생성 |
| `package` | 자원과 컴파일된 클래스를 담은 jar 생성 |
| `packageDoc` | API 문서를 담은 jar 생성 |
| `packageSrc` | 소스 파일과 자원을 담은 jar 생성 |
| `run <argument>*` | 프로젝트의 메인 클래스를 sbt와 같은 JVM에서 실행 |
| `runMain <main-class> <argument>*` | 지정한 메인 클래스 실행 |
| `test` | 감지된 모든 테스트 실행 |
| `testOnly <test>*` | 지정한 테스트만 실행 (`*` 와일드카드 지원) |
| `testQuick <test>*` | 아직 실행하지 않았거나 실패했거나 의존성이 재컴파일된 테스트만 실행 |

### 2.3 일반 커맨드

| 커맨드 | 설명 |
|---|---|
| `exit` / `quit` | 현재 세션 종료 (Ctrl+D, Ctrl+Z도 동일) |
| `help <command>` | 커맨드 상세 도움말 표시 |
| `projects` | 사용 가능한 모든 프로젝트 나열 |
| `project <project-id>` | 현재 프로젝트를 전환 |
| `eval <expression>` | Scala 식을 평가하고 결과를 반환 |

```
> eval System.setProperty("demo", "true")
> eval 1+1
> eval "ls -l" !
```

### 2.4 빌드 정의 관리 커맨드

| 커맨드 | 설명 |
|---|---|
| `reload` | 빌드를 재로드하고 플러그인 정의를 다시 컴파일 |
| `reload plugins` | 빌드 정의 프로젝트(`project/`)로 전환 |
| `reload return` | 원래 프로젝트로 복귀 |
| `set <expression>` | 세팅 정의를 평가하고 즉시 적용 (재시작·재로드 전까지 유지) |
| `session <command>` | `set`으로 정의한 세션 세팅을 관리(저장, 삭제 등) |
| `inspect <key>` | 값, 정의 위치, 의존성 등 세팅/태스크 정보 표시 (8절 참고) |

---

## 3. 커맨드 결합 문법

| 문법 | 의미 | 예제 |
|---|---|---|
| `; A ; B` | A가 성공하면 이어서 B 실행 (앞에 세미콜론 필수) | `; clean ; compile ; test` |
| `~ <command>` | 소스 파일이 변경될 때마다 커맨드를 반복 실행 | `~ compile`, `~ test` |
| `< filename` | 파일에 적힌 커맨드를 순차 실행 | `< commands.txt` |
| `+ <command>` | `crossScalaVersions`에 등록된 모든 Scala 버전으로 실행 | `+ test` |
| `++ <version|path> <command>` | Scala 버전을 임시로 전환한 뒤 실행 | `++ 2.13.0 compile` |

`~`는 파일 변경을 감지해 반복 실행하며 Ctrl+C로 감시를 중단한다. `+`와 `++`의 자세한 차이는 7절에서 다룬다.

셸에서 커맨드에 공백이나 특수문자가 있으면 따옴표로 감싸는 것이 안전하다.

```bash
sbt "testOnly com.example.*"
sbt "++ 2.13.0 compile"
```

---

## 4. 자주 쓰는 태스크/커맨드 실전 예제

```bash
# 정리 후 컴파일, 테스트, 퍼블리시까지 한 번에
sbt clean compile test publish

# 모든 Scala 버전으로 컴파일
sbt + compile

# 특정 Scala 버전으로 일회성 실행
sbt "++ 2.13.0 compile"

# 패턴에 맞는 테스트만 실행
sbt "testOnly com.example.*"

# 소스 변경 감시 후 자동 재컴파일
sbt ~compile

# 커맨드 파일 기반 배치 실행
sbt < commands.txt

# CI 환경: 컬러 출력 비활성화
sbt -Dsbt.ci=true clean test

# 메모리 옵션 조정
sbt -J-Xmx2048M -J-XX:MaxMetaspaceSize=512M clean compile
```

---

## 5. JVM 옵션과 시스템 프로퍼티

### 5.1 환경 변수와 파일 기반 옵션

`JAVA_OPTS`, `SBT_OPTS` 환경 변수가 정의되어 있으면 sbt를 실행하는 JVM에 전달된다. 매번 환경 변수를 설정하기 번거로우면 파일로도 지정할 수 있다.

| 파일 | 대응 환경 변수 |
|---|---|
| `.jvmopts` (프로젝트 루트) | `JAVA_OPTS`에 추가 |
| `.sbtopts`, `/etc/sbt/sbtopts` | `SBT_OPTS`에 추가 |

기본값은 `JAVA_OPTS=-Dfile.encoding=UTF8`이다.

```bash
export SBT_OPTS="-Xmx2048M -Xss2M"
sbt
```

한 번만 옵션을 주려면 `-J` 접두사를 사용한다.

```bash
sbt -J-Xmx2048M -J-Xss2M
```

### 5.2 주요 시스템 프로퍼티

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `sbt.banner` | true | 시작 시 배너 표시 여부 |
| `sbt.boot.directory` | `~/.sbt/boot` | 공유 부트 디렉터리 경로 |
| `sbt.ci` | false | CI 환경 감지 시 supershell/색상 비활성화 |
| `sbt.color` | auto | 색상 출력 여부 (`always`/`never`/`auto`) |
| `sbt.coursier` | true | 의존성 해결에 coursier 사용 여부 |
| `sbt.coursier.home` | 기본 경로 | coursier 아티팩트 캐시 위치 |
| `sbt.global.base` | `~/.sbt/1.0` | 전역 세팅/플러그인 디렉터리 |
| `sbt.ivy.home` | `~/.ivy2` | 로컬 Ivy 저장소 경로 |
| `sbt.log.noformat` | false | ANSI 컬러 코드 비활성화 |
| `sbt.offline` | false | 저장소에서 클래스 다운로드를 회피 |
| `sbt.progress` | auto | 진행 상황(supershell) 표시 여부 |
| `sbt.task.timings` | false | 태스크 실행 시간 측정 여부 |
| `sbt.version` | (버전 문자열) | 사용할 sbt 버전 지정 |

```bash
# 부트 디렉터리 변경
sbt -Dsbt.boot.directory=project/boot/

# 프록시 설정 (유닉스 환경 변수 방식)
export http_proxy=http://proxy.example.com:8080

# 프록시 설정 (명시적 지정, Windows 등)
sbt -Dhttp.proxyHost=myproxy -Dhttp.proxyPort=8080 \
    -Dhttp.proxyUser=username -Dhttp.proxyPassword=password

# 터미널 인코딩 지정
export JAVA_OPTS="-Dfile.encoding=Cp1252"
sbt
```

HTTPS, FTP 프록시가 필요하면 `http` 부분을 각각 `https`, `ftp`로 바꿔 지정한다.

---

## 6. console-project

`consoleProject` 태스크는 일반 `console`과 달리 빌드 정의와 sbt 자체가 클래스패스에 올라간 Scala 인터프리터를 실행한다. 즉 프로젝트 코드가 아니라 **빌드 시스템 자체를 대화식으로 조작**하기 위한 콘솔이다.

```bash
sbt consoleProject
```

### 6.1 자동 임포트되는 항목

`consoleProject`에 진입하면 다음이 자동으로 임포트된다.

```scala
import sbt._
import Keys._
import <프로젝트-정의>._
import currentState._
import extracted._
import cpHelpers._
```

### 6.2 설정값과 태스크 평가

세팅과 태스크 모두 `.eval`을 붙여 평가한다.

```scala
> val value = (<scope> / <key>).eval
```

```scala
// 컴파일 클래스 디렉터리 삭제
IO.delete( (Compile / classesDirectory).eval )

// 컴파일 옵션 나열
(Compile / scalacOptions).eval foreach println

// 등록된 저장소 확인
resolvers.eval foreach println

// 전체 저장소(플러그인 저장소 포함) 확인
fullResolvers.eval foreach println

// 컴파일 클래스패스의 파일 목록
(Compile / fullClasspath).eval.files foreach println

// 테스트 클래스패스의 파일 목록
(Test / fullClasspath).eval.files foreach println
```

### 6.3 빌드 상태(State) 접근

현재 빌드 상태는 `currentState`로 접근하며 기본으로 임포트되어 있다.

```scala
// 실행 대기 중인 커맨드 목록
remainingCommands

// 등록된 커맨드 개수
definedCommands.size
```

외부 프로세스도 그대로 실행할 수 있다.

```scala
"tar -zcvf project-src.tar.gz src" !
```

`consoleProject`는 빌드 정의를 탐색하거나 디버깅할 때, 혹은 세팅/태스크 값을 즉석에서 확인할 때 유용하다.

---

## 7. 크로스 빌드

### 7.1 개념

Scala는 버전 간 소스 호환성은 유지하지만 바이너리 호환성은 보장하지 않는다. 그래서 라이브러리를 여러 Scala 버전으로 각각 컴파일해 배포해야 하는 경우가 생기는데, 이를 **크로스 빌드**라 한다.

퍼블리시 규약상 아티팩트 이름 뒤에 `_<scala-binary-version>` 접미사가 붙는다. 예를 들어 `dispatch-core_2.12`는 Scala 2.12.x용으로 컴파일된 아티팩트다.

### 7.2 %% 연산자

`%%`를 쓰면 sbt가 현재 `scalaVersion`을 자동으로 의존성 이름에 붙여준다.

```scala
libraryDependencies += "net.databinder.dispatch" %% "dispatch-core" % "0.13.3"
```

위는 아래와 동일한 의미다.

```scala
libraryDependencies += "net.databinder.dispatch" % "dispatch-core_2.12" % "0.13.3"
```

### 7.3 crossScalaVersions 설정

```scala
lazy val scala212 = "2.12.18"
lazy val scala211 = "2.11.12"
lazy val supportedScalaVersions = List(scala212, scala211)

ThisBuild / organization := "com.example"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := scala212

lazy val root = (project in file("."))
  .aggregate(util, core)
  .settings(
    crossScalaVersions := Nil,
    publish / skip := true
  )

lazy val core = (project in file("core"))
  .settings(
    crossScalaVersions := supportedScalaVersions
  )

lazy val util = (project in file("util"))
  .settings(
    crossScalaVersions := supportedScalaVersions
  )
```

루트 프로젝트에서는 `crossScalaVersions := Nil`로 두어 같은 아티팩트가 중복 퍼블리시되지 않도록 해야 한다. 실제 크로스 빌드 대상 서브 프로젝트에만 `supportedScalaVersions`를 지정한다.

### 7.4 + 커맨드

`+`는 `crossScalaVersions`에 등록된 **모든** Scala 버전으로 태스크를 실행한다.

```
> + test
> + compile
> + publishSigned
```

개발 중에는 `+` 없이 단일 버전으로 빠르게 작업하고, 릴리스 시점에만 `+`를 붙여 전 버전 테스트/퍼블리시를 수행하는 방식이 일반적이다.

### 7.5 ++ 커맨드

`++`는 Scala 버전을 **일시적으로 전환**한 뒤 커맨드를 실행한다. `crossScalaVersions`와 무관하게 원하는 버전으로 바꿀 수 있다.

```
> ++ 2.12.18
> ++ 2.11.12
> compile
```

`-v` 플래그를 붙이면 전환 과정의 상세 정보를 볼 수 있다.

```
> ++ 2.11.12 -v test
```

프로젝트가 해당 버전을 지원하는지 확인하지 않고 강제로 전환하려면 느낌표를 붙인다.

```
> ++ 2.13.0-M5!
```

| 커맨드 | 용도 | 적용 범위 |
|---|---|---|
| `+ test` | 등록된 모든 버전에서 실행 | `crossScalaVersions` 목록 전체 |
| `++ 2.12.18 compile` | 특정 버전으로 전환 후 실행 | 해당 버전을 지원하는 프로젝트만 |

### 7.6 버전별 설정 분기

`CrossVersion.partialVersion`으로 Scala 버전에 따라 의존성이나 컴파일 옵션을 다르게 줄 수 있다.

```scala
lazy val core = (project in file("core"))
  .settings(
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n <= 12 =>
          List(compilerPlugin("org.scalamacros" % "paradise" % "2.1.1"
            cross CrossVersion.full))
        case _ => Nil
      }
    },
    Compile / scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n <= 12 => Nil
        case _ => List("-Ymacro-annotations")
      }
    }
  )
```

### 7.7 버전별 소스 디렉터리

`src/main/scala-<scala-binary-version>/` 디렉터리는 해당 버전으로 빌드할 때 자동으로 소스 경로에 포함된다. 예를 들어 `scalaVersion`이 `2.12.10`이면 `src/main/scala-2.12`가 함께 컴파일된다.

### 7.8 자바 프로젝트를 포함한 멀티 프로젝트 크로스 빌드

여러 서브 프로젝트 중 일부가 순수 자바 프로젝트라면 다음 원칙을 따른다.

1. 루트 프로젝트의 `crossScalaVersions`는 `Nil`로 둔다.
2. 자바 프로젝트는 `crossPaths := false`, `autoScalaLibrary := false`로 설정한다.
3. 자바 프로젝트는 `crossScalaVersions`에 정확히 하나의 버전만 넣는다.
4. Scala 프로젝트는 여러 버전을 등록할 수 있지만, 자바 프로젝트를 크로스 대상에 포함시키지 않는다.

```scala
lazy val scala212 = "2.12.18"
lazy val scala211 = "2.11.12"
lazy val supportedScalaVersions = List(scala212, scala211)

ThisBuild / organization := "com.example"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := scala212

lazy val root = (project in file("."))
  .aggregate(network, core)
  .settings(
    crossScalaVersions := Nil,
    publish / skip := false
  )

// 자바 프로젝트
lazy val network = (project in file("network"))
  .settings(
    crossScalaVersions := List(scala212),
    crossPaths := false,
    autoScalaLibrary := false
  )

lazy val core = (project in file("core"))
  .dependsOn(network)
  .settings(
    crossScalaVersions := supportedScalaVersions
  )
```

### 7.9 CrossVersion 옵션

```scala
"a" % "b" % "1.0"                             // 접미사 없음
("a" % "b" % "1.0").cross(CrossVersion.disabled)

"a" %% "b" % "1.0"                            // 바이너리 버전 접미사 (2.12, 2.13 등)
("a" % "b" % "1.0").cross(CrossVersion.binary)

("a" % "b" % "1.0").cross(CrossVersion.full)     // 전체 버전 접미사 (2.12.18 등)
("a" % "b" % "1.0").cross(CrossVersion.patch)    // 패치 버전 제외
("a" % "b" % "1.0") cross CrossVersion.constant("2.9.1")  // 고정 버전 접미사
```

### 7.10 크로스 퍼블리시 워크플로

```
> + test              # 등록된 모든 버전에서 테스트
> + publishSigned     # 등록된 모든 버전으로 서명 퍼블리시
```

버전별 산출물은 출력 디렉터리도 분리된다. 예를 들어 `./target/`은 실제로 `./target/scala-2.12/`, `./target/scala-2.11/`처럼 버전마다 나뉘어 저장되며, 각 버전의 의존성도 독립적으로 해결된다.

---

## 8. 설정 조회: inspect / show

### 8.1 완전한 키 참조 형식

sbt의 모든 세팅/태스크는 아래 형식으로 완전하게(fully-qualified) 참조할 수 있다.

```
{<build-uri>}<project-id>/<config>:<key>
```

다음은 모두 같은 태스크를 가리킨다.

```
> compile
> Compile/compile
> root/compile
> root/Compile/compile
> {file:/home/user/sample/}root/Compile/compile
```

기호 규칙은 다음과 같다.

- 단일 콜론(`:`): 설정 축(configuration) 뒤에 온다.
- 이중 콜론(`::`): 태스크 축 뒤에 온다.
- 별표(`*`): 전역(Global) 컨텍스트를 의미한다.

### 8.2 show 명령어

`show <key>`는 태스크나 세팅을 평가해 **값**만 보여준다.

```
> show libraryDependencies
```

값을 확인하는 용도로 가장 간단하지만, 그 값이 어디서 정의되었는지, 어떤 의존 관계를 갖는지는 알려주지 않는다. 그 정보는 `inspect`가 담당한다.

### 8.3 inspect 명령어

`inspect <key>`는 세팅/태스크의 값, 정의 위치, 의존 관계까지 종합적으로 보여준다.

**Value and Provided By** — 타입과 실제 정의 위치.

```
> inspect libraryDependencies
[info] Setting: scala.collection.Seq[sbt.ModuleID] = List(...)
[info] Provided by:
[info]  {file:/home/user/sample/}root/*:libraryDependencies
```

**Related** — 같은 키의 다른 설정 축에서의 정의 목록.

```
> inspect compile
[info] Related:
[info]  test:compile
```

**Dependencies** — 해당 세팅/태스크가 직접 참조하는 입력값들.

```
> inspect console
[info] Dependencies:
[info]  Compile / console / initialCommands
[info]  Compile / console / streams
[info]  Compile / console / compilers
```

### 8.4 inspect actual

`inspect actual <key>`는 위임(delegation)까지 반영해 **실제로** 참조되는 스코프를 보여준다. 원래 요청한 스코프와 다를 수 있다.

```
> inspect actual console
[info] Dependencies:
[info]  Global / taskTemporaryDirectory
[info]  Global / initialCommands
[info]  Compile / fullClasspath
```

`Reverse dependencies`는 반대로 해당 세팅/태스크를 사용하는 쪽을 보여준다.

```
> inspect actual initialCommands
[info] Reverse dependencies:
[info]  Compile / console
[info]  Test / console
[info]  Compile / consoleQuick
```

**Delegates** — 값이 현재 스코프에 정의되어 있지 않을 때 sbt가 검색하는 위임 체인 순서.

```
> inspect console/initialCommands
[info] Delegates:
[info]  console / initialCommands
[info]  initialCommands
[info]  ThisBuild / console / initialCommands
[info]  ThisBuild / initialCommands
[info]  Global / initialCommands
```

### 8.5 실무 활용: 세팅 변경과 확인

콘솔 시작 시 특정 패키지를 자동 임포트하도록 바꾸고 싶다면 `Compile / console / initialCommands`를 `set`으로 지정한다.

```
> set Compile / console / initialCommands := "import mypackage._"
> console
```

특정 설정 축에 국한하지 않고 전역으로 지정하려면 다음과 같이 한다.

```
> set initialCommands := "import mypackage._"
```

바뀐 값이 실제로 어느 스코프에 적용되었는지 확인하려면 `inspect actual`로 위임 체인과 실제 의존 관계를 다시 조회하면 된다.

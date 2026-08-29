# sbt 활용: CLI, 크로스 빌드, Triggered Execution, Server

## sbt CLI, console-project, 크로스 빌드, 설정 조회

> 원본: https://www.scala-sbt.org/1.x/docs/Command-Line-Reference.html, https://www.scala-sbt.org/1.x/docs/Console-Project.html, https://www.scala-sbt.org/1.x/docs/Cross-Build.html, https://www.scala-sbt.org/1.x/docs/Inspecting-Settings.html

---

### 목차

1. [CLI 실행 방식](#1-cli-실행-방식)
2. [커맨드 · 태스크 · 세팅](#2-커맨드-태스크-세팅)
3. [커맨드 결합 문법](#3-커맨드-결합-문법)
4. [자주 쓰는 태스크/커맨드](#4-자주-쓰는-태스크커맨드)
5. [JVM 옵션과 시스템 프로퍼티](#5-jvm-옵션과-시스템-프로퍼티)
6. [console-project](#6-console-project)
7. [크로스 빌드](#7-크로스-빌드)
8. [설정 조회: inspect / show](#8-설정-조회-inspect--show)

---

### 1. CLI 실행 방식

sbt는 세 가지 방식으로 실행 가능.

- 대화형 셸: `sbt` — 셸에 진입해 커맨드를 반복 입력
- 배치 모드(인자로 전달): `sbt clean compile test` — 커맨드를 인자로 나열, 순서대로 실행 후 종료
- 배치 모드(파일): `sbt < commands.txt` — 파일에 한 줄씩 적힌 커맨드를 순차 실행

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

`<` 로 읽어들이는 파일은 공백 줄과 `#`으로 시작하는 주석 줄을 무시.

---

### 2. 커맨드 · 태스크 · 세팅

sbt를 이루는 세 요소는 성격이 다름.

- 커맨드(command): 빌드 정의 자체를 조작하는 최상위 동작. `reload`, `project`, `set` 등
- 태스크(task): 빌드 정의 안에서 실행되는 작업. `compile`, `test`, `run` 등
- 세팅(setting): 빌드 시점에 한 번 계산되는 값. `name`, `version`, `libraryDependencies` 등

태스크는 설정 축(`Compile`, `Test` 등)과 프로젝트 축을 기준으로 범위가 정해짐. `Test/compile`은 테스트 소스 컴파일, 그냥 `compile`은 메인 소스 컴파일을 가리킴.

#### 2.1 프로젝트 레벨 태스크

- `clean`: `target` 디렉터리 등 생성된 파일을 모두 삭제
- `update`: 외부 의존성을 해결하고 다운로드
- `publishLocal`: 로컬 Ivy 저장소에 아티팩트를 퍼블리시
- `publish`: `publishTo`로 지정한 저장소에 퍼블리시

#### 2.2 설정 레벨 태스크

- `compile`: `src/main/scala` 소스를 컴파일
- `Test/compile`: `src/test/scala` 테스트 소스를 컴파일
- `console`: 컴파일된 소스, `lib` 디렉터리 jar, 의존성 라이브러리를 클래스패스에 올린 Scala 인터프리터 시작
- `consoleQuick`: 컴파일을 강제하지 않고 의존성만으로 인터프리터 시작
- `consoleProject`: 빌드 정의와 sbt 자체가 클래스패스에 있는 대화형 세션 진입 (6절 참고)
- `doc`: scaladoc으로 API 문서 생성
- `package`: 자원과 컴파일된 클래스를 담은 jar 생성
- `packageDoc`: API 문서를 담은 jar 생성
- `packageSrc`: 소스 파일과 자원을 담은 jar 생성
- `run <argument>*`: 프로젝트의 메인 클래스를 sbt와 같은 JVM에서 실행
- `runMain <main-class> <argument>*`: 지정한 메인 클래스 실행
- `test`: 감지된 모든 테스트 실행
- `testOnly <test>*`: 지정한 테스트만 실행 (`*` 와일드카드 지원)
- `testQuick <test>*`: 아직 실행하지 않았거나 실패했거나 의존성이 재컴파일된 테스트만 실행

#### 2.3 일반 커맨드

- `exit` / `quit`: 현재 세션 종료 (Ctrl+D, Ctrl+Z도 동일)
- `help <command>`: 커맨드 상세 도움말 표시
- `projects`: 사용 가능한 모든 프로젝트 나열
- `project <project-id>`: 현재 프로젝트를 전환
- `eval <expression>`: Scala 식을 평가하고 결과를 반환

```
> eval System.setProperty("demo", "true")
> eval 1+1
> eval "ls -l" !
```

#### 2.4 빌드 정의 관리 커맨드

- `reload`: 빌드를 재로드하고 플러그인 정의를 다시 컴파일
- `reload plugins`: 빌드 정의 프로젝트(`project/`)로 전환
- `reload return`: 원래 프로젝트로 복귀
- `set <expression>`: 세팅 정의를 평가하고 즉시 적용 (재시작·재로드 전까지 유지)
- `session <command>`: `set`으로 정의한 세션 세팅을 관리(저장, 삭제 등)
- `inspect <key>`: 값, 정의 위치, 의존성 등 세팅/태스크 정보 표시 (8절 참고)

---

### 3. 커맨드 결합 문법

- `; A ; B`: A가 성공하면 이어서 B 실행 (앞에 세미콜론 필수). 예: `; clean ; compile ; test`
- `~ <command>`: 소스 파일이 변경될 때마다 커맨드를 반복 실행. 예: `~ compile`, `~ test`
- `< filename`: 파일에 적힌 커맨드를 순차 실행. 예: `< commands.txt`
- `+ <command>`: `crossScalaVersions`에 등록된 모든 Scala 버전으로 실행. 예: `+ test`
- `++ <version|path> <command>`: Scala 버전을 임시로 전환한 뒤 실행. 예: `++ 2.13.0 compile`

`~`는 파일 변경을 감지해 반복 실행하며 Ctrl+C로 감시를 중단. `+`와 `++`의 자세한 차이는 7절에서 다룸.

셸에서 커맨드에 공백이나 특수문자가 있으면 따옴표로 감싸는 것이 안전.

```bash
sbt "testOnly com.example.*"
sbt "++ 2.13.0 compile"
```

---

### 4. 자주 쓰는 태스크/커맨드 실전 예제

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

### 5. JVM 옵션과 시스템 프로퍼티

#### 5.1 환경 변수와 파일 기반 옵션

`JAVA_OPTS`, `SBT_OPTS` 환경 변수가 정의되어 있으면 sbt를 실행하는 JVM에 전달됨. 매번 환경 변수를 설정하기 번거로우면 파일로도 지정 가능.

- `.jvmopts` (프로젝트 루트): `JAVA_OPTS`에 추가
- `.sbtopts`, `/etc/sbt/sbtopts`: `SBT_OPTS`에 추가

기본값은 `JAVA_OPTS=-Dfile.encoding=UTF8`.

```bash
export SBT_OPTS="-Xmx2048M -Xss2M"
sbt
```

한 번만 옵션을 주려면 `-J` 접두사를 사용.

```bash
sbt -J-Xmx2048M -J-Xss2M
```

#### 5.2 주요 시스템 프로퍼티

- `sbt.banner` (기본값 true): 시작 시 배너 표시 여부
- `sbt.boot.directory` (기본값 `~/.sbt/boot`): 공유 부트 디렉터리 경로
- `sbt.ci` (기본값 false): CI 환경 감지 시 supershell/색상 비활성화
- `sbt.color` (기본값 auto): 색상 출력 여부 (`always`/`never`/`auto`)
- `sbt.coursier` (기본값 true): 의존성 해결에 coursier 사용 여부
- `sbt.coursier.home` (기본값 기본 경로): coursier 아티팩트 캐시 위치
- `sbt.global.base` (기본값 `~/.sbt/1.0`): 전역 세팅/플러그인 디렉터리
- `sbt.ivy.home` (기본값 `~/.ivy2`): 로컬 Ivy 저장소 경로
- `sbt.log.noformat` (기본값 false): ANSI 컬러 코드 비활성화
- `sbt.offline` (기본값 false): 저장소에서 클래스 다운로드를 회피
- `sbt.progress` (기본값 auto): 진행 상황(supershell) 표시 여부
- `sbt.task.timings` (기본값 false): 태스크 실행 시간 측정 여부
- `sbt.version` (기본값 버전 문자열): 사용할 sbt 버전 지정

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

HTTPS, FTP 프록시가 필요하면 `http` 부분을 각각 `https`, `ftp`로 바꿔 지정.

---

### 6. console-project

`consoleProject` 태스크는 일반 `console`과 달리 빌드 정의와 sbt 자체가 클래스패스에 올라간 Scala 인터프리터를 실행. 즉 프로젝트 코드가 아니라 빌드 시스템 자체를 대화식으로 조작하기 위한 콘솔.

```bash
sbt consoleProject
```

#### 6.1 자동 임포트되는 항목

`consoleProject`에 진입하면 다음이 자동으로 임포트됨.

```scala
import sbt._
import Keys._
import <프로젝트-정의>._
import currentState._
import extracted._
import cpHelpers._
```

#### 6.2 설정값과 태스크 평가

세팅과 태스크 모두 `.eval`을 붙여 평가.

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

#### 6.3 빌드 상태(State) 접근

현재 빌드 상태는 `currentState`로 접근하며 기본으로 임포트되어 있음.

```scala
// 실행 대기 중인 커맨드 목록
remainingCommands

// 등록된 커맨드 개수
definedCommands.size
```

외부 프로세스도 그대로 실행 가능.

```scala
"tar -zcvf project-src.tar.gz src" !
```

`consoleProject`는 빌드 정의를 탐색하거나 디버깅할 때, 혹은 세팅/태스크 값을 즉석에서 확인할 때 유용.

---

### 7. 크로스 빌드

#### 7.1 개념

Scala는 버전 간 소스 호환성은 유지하지만 바이너리 호환성은 보장하지 않음. 그래서 라이브러리를 여러 Scala 버전으로 각각 컴파일해 배포해야 하는 경우가 생기는데, 이를 크로스 빌드라 함.

퍼블리시 규약상 아티팩트 이름 뒤에 `_<scala-binary-version>` 접미사가 붙음. 예를 들어 `dispatch-core_2.12`는 Scala 2.12.x용으로 컴파일된 아티팩트.

#### 7.2 %% 연산자

`%%`를 쓰면 sbt가 현재 `scalaVersion`을 자동으로 의존성 이름에 붙여줌.

```scala
libraryDependencies += "net.databinder.dispatch" %% "dispatch-core" % "0.13.3"
```

위는 아래와 동일한 의미.

```scala
libraryDependencies += "net.databinder.dispatch" % "dispatch-core_2.12" % "0.13.3"
```

#### 7.3 crossScalaVersions 설정

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

루트 프로젝트에서는 `crossScalaVersions := Nil`로 두어 같은 아티팩트가 중복 퍼블리시되지 않도록 해야 함. 실제 크로스 빌드 대상 서브 프로젝트에만 `supportedScalaVersions`를 지정.

#### 7.4 + 커맨드

`+`는 `crossScalaVersions`에 등록된 모든 Scala 버전으로 태스크를 실행.

```
> + test
> + compile
> + publishSigned
```

개발 중에는 `+` 없이 단일 버전으로 빠르게 작업하고, 릴리스 시점에만 `+`를 붙여 전 버전 테스트/퍼블리시를 수행하는 방식이 일반적.

#### 7.5 ++ 커맨드

`++`는 Scala 버전을 일시적으로 전환한 뒤 커맨드를 실행. `crossScalaVersions`와 무관하게 원하는 버전으로 바꿀 수 있음.

```
> ++ 2.12.18
> ++ 2.11.12
> compile
```

`-v` 플래그를 붙이면 전환 과정의 상세 정보를 볼 수 있음.

```
> ++ 2.11.12 -v test
```

프로젝트가 해당 버전을 지원하는지 확인하지 않고 강제로 전환하려면 느낌표를 붙임.

```
> ++ 2.13.0-M5!
```

- `+ test`: 등록된 모든 버전에서 실행, `crossScalaVersions` 목록 전체에 적용
- `++ 2.12.18 compile`: 특정 버전으로 전환 후 실행, 해당 버전을 지원하는 프로젝트에만 적용

#### 7.6 버전별 설정 분기

`CrossVersion.partialVersion`으로 Scala 버전에 따라 의존성이나 컴파일 옵션을 다르게 줄 수 있음.

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

#### 7.7 버전별 소스 디렉터리

`src/main/scala-<scala-binary-version>/` 디렉터리는 해당 버전으로 빌드할 때 자동으로 소스 경로에 포함됨. 예를 들어 `scalaVersion`이 `2.12.10`이면 `src/main/scala-2.12`가 함께 컴파일됨.

#### 7.8 자바 프로젝트를 포함한 멀티 프로젝트 크로스 빌드

여러 서브 프로젝트 중 일부가 순수 자바 프로젝트라면 다음 원칙을 따름.

1. 루트 프로젝트의 `crossScalaVersions`는 `Nil`로 둠
2. 자바 프로젝트는 `crossPaths := false`, `autoScalaLibrary := false`로 설정
3. 자바 프로젝트는 `crossScalaVersions`에 정확히 하나의 버전만 등록
4. Scala 프로젝트는 여러 버전을 등록할 수 있지만, 자바 프로젝트를 크로스 대상에 포함시키지 않음

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

#### 7.9 CrossVersion 옵션

```scala
"a" % "b" % "1.0"                             // 접미사 없음
("a" % "b" % "1.0").cross(CrossVersion.disabled)

"a" %% "b" % "1.0"                            // 바이너리 버전 접미사 (2.12, 2.13 등)
("a" % "b" % "1.0").cross(CrossVersion.binary)

("a" % "b" % "1.0").cross(CrossVersion.full)     // 전체 버전 접미사 (2.12.18 등)
("a" % "b" % "1.0").cross(CrossVersion.patch)    // 패치 버전 제외
("a" % "b" % "1.0") cross CrossVersion.constant("2.9.1")  // 고정 버전 접미사
```

#### 7.10 크로스 퍼블리시 워크플로

```
> + test              # 등록된 모든 버전에서 테스트
> + publishSigned     # 등록된 모든 버전으로 서명 퍼블리시
```

버전별 산출물은 출력 디렉터리도 분리됨. 예를 들어 `./target/`은 실제로 `./target/scala-2.12/`, `./target/scala-2.11/`처럼 버전마다 나뉘어 저장되며, 각 버전의 의존성도 독립적으로 해결됨.

---

### 8. 설정 조회: inspect / show

#### 8.1 완전한 키 참조 형식

sbt의 모든 세팅/태스크는 아래 형식으로 완전하게(fully-qualified) 참조 가능.

```
{<build-uri>}<project-id>/<config>:<key>
```

다음은 모두 같은 태스크를 가리킴.

```
> compile
> Compile/compile
> root/compile
> root/Compile/compile
> {file:/home/user/sample/}root/Compile/compile
```

기호 규칙은 다음과 같음.

- 단일 콜론(`:`): 설정 축(configuration) 뒤에 옴
- 이중 콜론(`::`): 태스크 축 뒤에 옴
- 별표(`*`): 전역(Global) 컨텍스트를 의미

#### 8.2 show 명령어

`show <key>`는 태스크나 세팅을 평가해 값만 보여줌.

```
> show libraryDependencies
```

값을 확인하는 용도로 가장 간단하지만, 그 값이 어디서 정의되었는지, 어떤 의존 관계를 갖는지는 알려주지 않음. 그 정보는 `inspect`가 담당.

#### 8.3 inspect 명령어

`inspect <key>`는 세팅/태스크의 값, 정의 위치, 의존 관계까지 종합적으로 보여줌.

Value and Provided By — 타입과 실제 정의 위치.

```
> inspect libraryDependencies
[info] Setting: scala.collection.Seq[sbt.ModuleID] = List(...)
[info] Provided by:
[info]  {file:/home/user/sample/}root/*:libraryDependencies
```

Related — 같은 키의 다른 설정 축에서의 정의 목록.

```
> inspect compile
[info] Related:
[info]  test:compile
```

Dependencies — 해당 세팅/태스크가 직접 참조하는 입력값들.

```
> inspect console
[info] Dependencies:
[info]  Compile / console / initialCommands
[info]  Compile / console / streams
[info]  Compile / console / compilers
```

#### 8.4 inspect actual

`inspect actual <key>`는 위임(delegation)까지 반영해 실제로 참조되는 스코프를 보여줌. 원래 요청한 스코프와 다를 수 있음.

```
> inspect actual console
[info] Dependencies:
[info]  Global / taskTemporaryDirectory
[info]  Global / initialCommands
[info]  Compile / fullClasspath
```

`Reverse dependencies`는 반대로 해당 세팅/태스크를 사용하는 쪽을 보여줌.

```
> inspect actual initialCommands
[info] Reverse dependencies:
[info]  Compile / console
[info]  Test / console
[info]  Compile / consoleQuick
```

Delegates — 값이 현재 스코프에 정의되어 있지 않을 때 sbt가 검색하는 위임 체인 순서.

```
> inspect console/initialCommands
[info] Delegates:
[info]  console / initialCommands
[info]  initialCommands
[info]  ThisBuild / console / initialCommands
[info]  ThisBuild / initialCommands
[info]  Global / initialCommands
```

#### 8.5 실무 활용: 세팅 변경과 확인

콘솔 시작 시 특정 패키지를 자동 임포트하도록 바꾸고 싶다면 `Compile / console / initialCommands`를 `set`으로 지정.

```
> set Compile / console / initialCommands := "import mypackage._"
> console
```

특정 설정 축에 국한하지 않고 전역으로 지정하려면 다음과 같이 함.

```
> set initialCommands := "import mypackage._"
```

바뀐 값이 실제로 어느 스코프에 적용되었는지 확인하려면 `inspect actual`로 위임 체인과 실제 의존 관계를 다시 조회.

```
> inspect actual initialCommands
```

---

## sbt 활용: Triggered Execution, 스크립트, Server, 증분 컴파일

> 원본: https://www.scala-sbt.org/1.x/docs/Triggered-Execution.html, https://www.scala-sbt.org/1.x/docs/Scripts.html, https://www.scala-sbt.org/1.x/docs/sbt-server.html, https://www.scala-sbt.org/1.x/docs/Understanding-Recompilation.html

---

### 목차

1. [Triggered Execution (~명령)](#1-triggered-execution-명령)
2. [스크립트 모드](#2-스크립트-모드)
3. [sbt server와 BSP](#3-sbt-server와-bsp)
4. [증분 컴파일(Zinc) 원리](#4-증분-컴파일zinc-원리)

---

### 1. Triggered Execution (~명령)

#### 1.1 개념

sbt 콘솔에서 명령 앞에 물결표(`~`)를 붙이면, 관련 소스 파일이 바뀔 때마다 그 명령을 자동으로 재실행. 파일을 저장할 때마다 수동으로 `compile`, `test`를 다시 입력하는 대신 sbt가 파일 시스템을 감시(watch)하다가 변경을 감지하면 즉시 태스크를 재실행하는 방식.

```
> ~ compile
```

위 명령을 실행하면 sbt는 `compile` 태스크가 참조하는 소스 파일들을 감시하다가, 파일이 바뀌는 즉시 다시 `compile`을 수행. 종료하려면 Enter를 누르면 됨.

#### 1.2 자주 쓰는 조합

```
> ~ Test / compile
> ~ testQuick
> ~ testQuick foo.BarTest
> ~ testOnly foo.BarTest
> ~ test
```

- `~ Test / compile`: 테스트 스코프 컴파일만 감시
- `~ testQuick`: 이전 실행에서 실패했거나, 재컴파일된 클래스와 관련된 테스트만 실행
- `~ testOnly foo.BarTest`: 특정 테스트 클래스만 반복 실행

세미콜론으로 여러 명령을 묶어 한 번에 감시 가능.

```
> ~ clean; test
```

#### 1.3 watch 대상 결정 방식

기본적으로 sbt는 실행하는 태스크가 직접 의존하는 입력 파일만 감시 대상으로 삼음. 예를 들어 `~ compile`은 `Compile / sources`가 가리키는 파일들을 감시하며, 그 파일들과 무관한 리소스나 문서 파일은 감시하지 않음.

태스크가 참조하지 않는 파일까지 감시하고 싶다면 `watchTriggers`에 글롭 패턴을 추가.

```
foo / watchTriggers += baseDirectory.value.toGlob / "*.txt"
```

빌드 정의 파일(`build.sbt`, `project/*.scala`) 자체의 변경까지 감시해 자동으로 빌드를 재로드하고 싶다면 다음을 설정.

```
Global / onChangedBuildSource := ReloadOnSourceChanges
```

이 설정을 켜면 빌드 소스가 바뀔 때 세션을 재시작할 필요 없이 sbt가 자동으로 빌드를 재로드한 뒤 triggered execution 모드로 복귀.

#### 1.4 동작 커스터마이징 키

- `watchTriggers`: 태스크가 직접 참조하지 않는 파일까지 감시 대상에 추가
- `watchTriggeredMessage`: 트리거가 발동했을 때 출력할 메시지
- `watchStartMessage`: 감시 대기 상태에서 보여줄 배너
- `watchInputOptions`: 감시 중 사용할 수 있는 키 입력 옵션 추가
- `watchInputParser`: 키 입력 이벤트를 어떻게 해석할지 정의
- `watchBeforeCommand`: 태스크 실행 직전에 실행할 콜백 (화면 클리어 등)
- `watchOnIteration`: 매 반복 전에 실행할 함수
- `watchLogLevel`: 파일 모니터링 관련 로그 레벨
- `watchForceTriggerOnAnyChange` (기본값 false): 파일 내용이 바뀌지 않아도(타임스탬프만 바뀌어도) 트리거할지 여부
- `watchPersistFileStamps` (기본값 false): 파일 해시를 디스크에 캐싱해 재시작 후에도 유지할지 여부
- `watchAntiEntropy` (기본값 500ms): 같은 파일에 대한 재트리거를 억제하는 대기 시간

화면을 매번 새로 지우고 싶다면 다음처럼 설정.

```
ThisBuild / watchTriggeredMessage := Watch.clearScreenOnTrigger
ThisBuild / watchBeforeCommand := Watch.clearScreen
```

감시 중 커스텀 키 입력을 추가하려면 다음처럼 옵션을 등록.

```
ThisBuild / watchInputOptions += Watch.InputOption('l', "reload", Watch.Reload)
```

#### 1.5 종료

감시 도중 Enter 키를 누르면 트리거 루프가 끝나고 콘솔로 돌아감. `?`를 입력하면 그 시점에 사용 가능한 입력 옵션 목록을 확인 가능.

---

### 2. 스크립트 모드

#### 2.1 개념

sbt는 일반 프로젝트 빌드 외에, 단일 Scala 파일을 스크립트처럼 즉시 컴파일·실행하는 대체 진입점을 제공. `ScriptMain`을 메인 클래스로 지정해 실행하면 셔뱅(`#!`) 라인으로 실행 가능한 스크립트 파일을 만들 수 있음. 다만 매 실행마다 sbt 자체의 부팅 비용이 들어가므로 시작 시간이 느리다는 단점이 있고, 이 기능은 실험적(experimental) 상태로 제공됨.

#### 2.2 기본 예제

```scala
#!/usr/bin/env sbt -Dsbt.version=1.6.1 -Dsbt.main.class=sbt.ScriptMain -error

/***
ThisBuild / scalaVersion := "2.13.12"
libraryDependencies += "org.scala-sbt" %% "io" % "1.6.0"
*/

println("hello")
```

파일에 실행 권한을 부여하면 바로 실행 가능.

```bash
chmod u+x script.scala
./script.scala
```

`/*** ... */` 블록 안에는 일반 `build.sbt`에 쓰는 것과 동일한 설정 문법(`scalaVersion`, `libraryDependencies` 등)을 그대로 적을 수 있음. sbt는 이 블록을 파싱해 필요한 Scala 버전과 라이브러리를 내려받은 뒤 나머지 코드를 컴파일하고 실행.

#### 2.3 인자를 받는 스크립트 예제

```scala
#!/usr/bin/env sbt -Dsbt.version=1.6.1 -Dsbt.main.class=sbt.ScriptMain -error

/***
ThisBuild / scalaVersion := "2.13.12"
libraryDependencies += "org.scala-sbt" %% "io" % "1.6.0"
*/

import sbt.io.IO
import sbt.io.Path._
import sbt.io.syntax._
import java.io.File
import java.net.URI
import sys.process._

def file(s: String): File = new File(s)
def uri(s: String): URI = new URI(s)

def processFile(f: File): Unit = {
  val lines = IO.readLines(f)
  lines foreach { line =>
    println(line.toUpperCase)
  }
}

args.toList match {
  case Nil => sys.error("usage: ./script.scala <file>...")
  case xs  => xs foreach { x => processFile(file(x)) }
}
```

```bash
./script.scala script.scala
```

이 스크립트는 인자로 받은 파일의 각 줄을 대문자로 바꿔 출력. `args`는 커맨드라인 인자 리스트를 그대로 받음.

#### 2.4 제약

- 스크립트 모드는 실험적 기능이라 향후 문법이 바뀔 수 있음
- 매번 sbt 부트스트랩 과정을 거치므로 일반 스크립트 언어(예: 셸 스크립트, Python)에 비해 시작 지연이 큼
- 저장소 접근이 필요한 의존성을 선언하면 첫 실행 시 다운로드 시간이 추가됨

---

### 3. sbt server와 BSP

#### 3.1 개념

sbt server는 sbt 1.x부터 기본 내장된 기능으로, 하나의 sbt 세션을 백그라운드에 띄워두고 여러 클라이언트가 네트워크(로컬 소켓 또는 TCP)를 통해 명령을 주고받을 수 있게 함. 콘솔에서 직접 명령을 입력하는 대신, 에디터나 IDE가 클라이언트가 되어 컴파일 결과·진단(diagnostics)·자동완성 같은 정보를 받아볼 수 있도록 설계됨.

와이어 프로토콜은 Language Server Protocol(LSP) 3.0 스펙을 기반으로 하며, 메시지는 JSON-RPC 2.0 형식으로 주고받음.

```
Content-Type: application/vscode-jsonrpc; charset=utf-8\r\n
Content-Length: ...\r\n
\r\n
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "textDocument/didSave",
  "params": { ... }
}
```

#### 3.2 연결 방식

- 도메인 소켓 / 명명된 파이프 (기본값, sbt 1.1.x+): Unix domain socket / Windows named pipe 대상. 설정: `serverConnectionType := ConnectionType.Local`
- TCP: TCP 소켓 대상. 설정: `serverConnectionType := ConnectionType.Tcp`

서버를 처음 발견하려면 `project/target/active.json` 파일을 읽음. 도메인 소켓 모드(Unix)에서는 다음과 같은 형태.

```json
{"uri":"local:///Users/someone/.sbt/1.0/server/0845deda85cb41abdb9f/sock"}
```

Windows 명명된 파이프 모드는 다음과 같음.

```json
{"uri":"local:sbt-server-0845deda85cb41abdb9f"}
```

TCP 모드에서는 포트 정보와 함께 인증 토큰 파일 경로가 포함됨.

```json
{
  "uri":"tcp://127.0.0.1:5010",
  "tokenfilePath":"/Users/xxx/.sbt/1.0/server/0845deda85cb41abdb9f/token.json",
  "tokenfileUri":"file:/Users/xxx/.sbt/1.0/server/0845deda85cb41abdb9f/token.json"
}
```

토큰 파일 내용은 다음과 같은 구조.

```json
{
  "uri":"tcp://127.0.0.1:5010",
  "token":"12345678901234567890123456789012345678"
}
```

클라이언트는 접속 후 가장 먼저 `initialize` 메서드로 핸드셰이크를 수행해야 함.

```
$ telnet 127.0.0.1 5010
Content-Type: application/vscode-jsonrpc; charset=utf-8
Content-Length: 149

{ "jsonrpc": "2.0", "id": 1, "method": "initialize",
  "params": { "initializationOptions":
    { "token": "84046191245433876643612047032303751629" } } }
```

#### 3.3 주요 요청/이벤트

- `textDocument/didSave` (클라이언트 → 서버): 파일 저장 시 자동으로 `compile` 태스크 트리거 (sbt 1.1.0+)
- `textDocument/publishDiagnostics` (서버 → 클라이언트): 컴파일 경고/오류 알림
- `sbt/exec` (클라이언트 → 서버): 임의의 셸 명령 실행 요청
- `sbt/setting` (클라이언트 → 서버): 특정 설정 키의 값 조회
- `sbt/completion` (클라이언트 → 서버): 탭 완성 후보 조회 (sbt 1.3.0+)
- `sbt/cancelRequest` (클라이언트 → 서버): 실행 중인 태스크 취소 요청 (sbt 1.3.0+)

`sbt/exec`로 셸 명령을 실행하는 예시는 다음과 같음.

```
Content-Length: 91

{ "jsonrpc": "2.0", "id": 2, "method": "sbt/exec",
  "params": { "commandLine": "clean" } }
```

`sbt/setting`으로 설정값을 조회하면 다음과 같은 응답을 받음.

```
{ "jsonrpc": "2.0", "id": 3, "method": "sbt/setting",
  "params": { "setting": "root/scalaVersion" } }
```

```json
{"jsonrpc":"2.0","id":"3","result":{"value":"2.12.2","contentType":"java.lang.String"}}
```

`publishDiagnostics` 알림은 컴파일 오류 위치와 메시지를 담아 IDE에 실시간으로 전달.

```json
{
  "jsonrpc": "2.0",
  "method": "textDocument/publishDiagnostics",
  "params": {
    "uri": "file:/Users/xxx/work/hellotest/Hello.scala",
    "diagnostics": [
      {
        "range": {
          "start": { "line": 2, "character": 0 },
          "end": { "line": 2, "character": 1 }
        },
        "severity": 1,
        "source": "sbt",
        "message": "')' expected but '}' found."
      }
    ]
  }
}
```

#### 3.4 유휴 타임아웃

sbt server가 무한정 떠 있지 않도록 유휴 타임아웃을 설정 가능.

```
Global / serverIdleTimeout := Some(new FiniteDuration(5, TimeUnit.MINUTES))
```

지정한 시간 동안 어떤 클라이언트 요청도 없으면 서버가 자동으로 종료됨.

#### 3.5 BSP와의 관계

sbt server가 사용하는 LSP 기반 프로토콜은 이후 여러 빌드 도구가 공통으로 채택하는 Build Server Protocol(BSP)의 기반이 됨. BSP는 IDE가 특정 빌드 도구(sbt, Mill, Bazel 등)의 세부 구현을 몰라도 컴파일·테스트·의존성 조회 같은 요청을 동일한 프로토콜로 보낼 수 있게 표준화한 것. sbt server의 exec/setting/completion/diagnostics 개념은 BSP가 정의하는 빌드 타겟 컴파일, 진단 발행, 의존성 조회 요청들과 맥락을 같이함.

---

### 4. 증분 컴파일(Zinc) 원리

#### 4.1 목표

증분 컴파일의 목표는 한 파일을 고쳤을 때 프로젝트 전체가 아니라 실제로 영향을 받는 파일만 다시 컴파일하는 것. 두 축으로 접근.

- Scalac을 매번 새 JVM에서 띄우지 않고, 컴파일러 자체를 계속 살려 둬 JIT 워밍업 이점을 활용
- 소스 파일 사이의 인터페이스(공개 API) 변경 여부만 추적해, 구현부만 바뀐 경우 하위 의존 파일은 건드리지 않음

#### 4.2 의존성 두 종류

sbt는 소스 파일 단위로 의존성 그래프를 만들며, 의존성을 두 가지로 구분.

상속 의존성(Inheritance dependency):

```scala
// A.scala
abstract class A

// B.scala
class B extends A
```

부모 클래스/트레이트가 바뀌면 자식은 무조건 재컴파일 대상이 됨. 상속 관계는 name hashing 최적화를 적용하지 않음 — 부모의 시그니처 변경이 자식의 컴파일 성립 여부 자체에 영향을 주기 때문.

멤버 참조 의존성(Member reference dependency):

```scala
// A.scala
class A {
  def foo(): Int = 12
}

// B.scala
class B {
  def bar(x: A): Int = x.foo()
}
```

한 클래스의 메서드/필드를 호출만 하는 관계는 name hashing 최적화 대상이 됨.

#### 4.3 Name Hashing

sbt 0.13.6부터 도입된 알고리즘으로, "이 소스 파일이 실제로 사용하는 이름(usedNames)"과 "다른 파일에서 변경된 멤버의 이름"을 비교해 재컴파일 여부를 좁힘.

```scala
// A.scala (변경 전)
class A {
  def inc(x: Int): Int = x + 1
}

// A.scala (변경 후 — 새 메서드 추가)
class A {
  def inc(x: Int): Int = x + 1
  def dec(x: Int): Int = x - 1
}

// B.scala
class B {
  def foo(a: A, x: Int): Int = a.inc(x)
}
```

A에 `dec`이 새로 생겼지만, B.scala의 usedNames에는 `dec`이 없음. 따라서 A의 API 해시는 바뀌었어도 B가 실제로 참조하는 이름의 해시는 그대로이므로 B는 재컴파일하지 않음.

Name hashing이 통하지 않는 대표적인 경우 두 가지.

1. 상속 관계: 부모에 추상 메서드가 추가되면 자식이 그 구현을 갖추지 못해 컴파일 자체가 깨질 수 있으므로 이름 사용 여부와 무관하게 무조건 재컴파일
2. 암시적 변환(enrich-my-library 패턴): `implicit` 변환으로 확장 메서드를 제공하는 구조에서는 호출부 코드에 원래 이름이 등장하지 않을 수 있어, 변환 대상 클래스의 변경이 은근히 넓게 영향을 미침 → sbt는 이런 경우도 usedNames 추적 범위에 포함시켜 처리

#### 4.4 컴파일러 확장 페이즈

sbt는 Scala 컴파일러 뒤에 세 가지 페이즈를 추가로 끼워 넣어 증분 컴파일에 필요한 정보를 뽑아냄.

- API 추출(API phase): 클래스의 공개 인터페이스(시그니처)를 컴파일러 버전에 무관한 형태로 직렬화
- 의존성 추출(Dependency phase): 소스 파일이 참조하는 다른 심볼을 찾아 파일 간 의존 관계로 매핑
- 분석기(Analyzer phase): 소스 파일이 생성한 클래스 파일 목록을 수집

API 추출 결과는 파일별 해시(`nameHashes`)로 저장되고, 다음 컴파일 시점에 이전 해시와 비교해 실제로 바뀐 멤버만 골라냄.

#### 4.5 인터페이스로 취급되는 요소들

다음과 같은 변경은 "구현 세부사항"이 아니라 "인터페이스 변경"으로 취급되어 하위 파일 재컴파일을 유발 가능.

- 메서드 파라미터 이름 변경 (named argument 호출이 가능하므로 이름 자체가 API의 일부)
- 트레이트에 정의된 메서드의 시그니처 변경
- `sealed` 클래스 계층에 새 케이스 추가 — 패턴 매칭에서 `MatchError` 가능성이 달라지므로 해당 계층을 매칭하는 모든 파일이 영향받음
- 반대로 `private` 메서드는 애초에 공개 API에 포함되지 않으므로, 구현을 바꿔도 호출 측 재컴파일을 유발하지 않음

#### 4.6 타입 추론이 만드는 함정

반환 타입을 명시하지 않은 메서드는 구현을 바꾸는 것만으로 추론된 반환 타입이 달라질 수 있고, 이는 곧 인터페이스 변경으로 이어져 예상치 못한 넓은 범위의 재컴파일을 유발.

```scala
// 변경 전 — 추론 타입: List[FileWriter]
def openFiles(list: List[File]) =
  list.map(name => new FileWriter(name))

// 변경 후 — 추론 타입이 Vector[BufferedWriter]로 바뀜
def openFiles(list: List[File]) =
  Vector(list.map(name => new BufferedWriter(new FileWriter(name))): _*)
```

호출부에서 `List[FileWriter]`를 기대하고 있었다면 타입 체크가 깨지고 재컴파일이 강제됨. 공개 API에 해당하는 메서드는 반환 타입을 명시적으로 선언해두면 구현이 바뀌어도 인터페이스가 안정적으로 유지됨.

```scala
def openFiles(list: List[File]): Seq[Writer] =
  Vector(list.map(name => new BufferedWriter(new FileWriter(name))): _*)
```

#### 4.7 추적의 한계

- sbt는 파일 단위로 의존성을 추적하므로, 한 파일 안에 여러 클래스를 넣어두면 그중 하나만 바뀌어도 같은 파일을 참조하는 다른 파일까지 넓게 재컴파일될 수 있음. 클래스를 파일별로 분리하면 이 범위를 줄일 수 있음
- `sealed` 계층에 케이스를 추가하면 해당 타입을 패턴 매칭하는 모든 소스가 재컴파일 대상이 됨
- 복잡한 제네릭 타입 매개변수나 와일드카드가 얽힌 의존성은 정밀하게 추적되지 않을 수 있음

#### 4.8 디버깅

API 변경 추적 로그를 보고 싶다면 `apiDebug` 옵션을 켬.

```
sbt> set incOptions := incOptions.value.withApiDebug(true)
```

이 옵션을 켜면 어떤 API 차이 때문에 재컴파일이 유발됐는지 diff 형태로 로그에 출력됨.

```
[debug] Detected a change in a public API:
[debug] --- /path/Test.scala
[debug] +++ /path/Test.scala
[debug]  def b: scala.this#Int
[debug] -def b: scala.this#Int
[debug] +def b: java.lang.this#String
```

#### 4.9 정리

- 상속 의존성: 부모-자식 관계, name hashing 미적용, 변경 시 무조건 재컴파일
- 멤버 참조 의존성: 호출 관계, name hashing 적용 대상
- usedNames: 한 소스 파일이 실제로 참조하는 이름 집합
- nameHashes: 멤버 이름별 API 해시
- API 추출 페이즈: 공개 인터페이스를 버전 독립적 형태로 뽑아내는 컴파일러 확장
- 타입 추론 위험: 반환 타입 미명시 시 구현 변경만으로 인터페이스가 바뀔 수 있음

이 원리 덕분에 대규모 프로젝트에서도 한두 파일을 고쳤을 때 전체 재컴파일이 아니라 실제로 영향받는 부분집합만 다시 컴파일해 반복 개발 주기를 크게 단축 가능.

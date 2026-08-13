# sbt 시작하기: 설치, 실행, 빌드 정의

## sbt 시작하기: 개요, 설치, 프로젝트 설정

> 원본: https://www.scala-sbt.org/1.x/docs/Getting-Started.html, https://www.scala-sbt.org/1.x/docs/Setup.html, https://www.scala-sbt.org/1.x/docs/Installing-sbt-on-Mac.html, https://www.scala-sbt.org/1.x/docs/Installing-sbt-on-Windows.html, https://www.scala-sbt.org/1.x/docs/Installing-sbt-on-Linux.html, https://www.scala-sbt.org/1.x/docs/sbt-by-example.html, https://www.scala-sbt.org/1.x/docs/Directories.html

---

### 목차

1. [sbt란 무엇인가](#1-sbt란-무엇인가)
2. [설치 준비: JDK, 설치 방법(macOS/Windows/Linux)](#2-설치-준비-jdk-설치-방법macoswindowslinux)
3. [프로젝트 설정 첫걸음](#3-프로젝트-설정-첫걸음)
4. [예제로 배우는 sbt (sbt by example)](#4-예제로-배우는-sbt-sbt-by-example)
5. [표준 디렉터리 구조](#5-표준-디렉터리-구조)

---

### 1. sbt란 무엇인가

sbt는 Scala·Java 프로젝트용 빌드 도구. Maven·Gradle 등 다른 빌드 도구와 개념 모델이 다름 → 공식 문서도 이 점을 강조 → 아래 세 가지 개념을 먼저 이해할 필요.

- 빌드 정의(build definition): `build.sbt`와 `project/` 아래의 `.scala` 파일들로 구성되는, 프로젝트를 기술하는 코드
- 스코프(scope): 같은 키(`scalaVersion` 등)라도 프로젝트·설정(Compile/Test)·태스크에 따라 다른 값을 가질 수 있게 하는 범위 개념
- 태스크 그래프(task graph): `compile`, `test`, `run` 같은 태스크들이 서로 의존하며 이루는 실행 그래프

Getting Started 가이드는 다음 순서로 구성됨.

- 설치: OS별 sbt 설치, 예제로 배우는 sbt, 디렉터리 구조, 실행 방법
- 핵심 개념: IDE 연동, 빌드 정의, 멀티 프로젝트 빌드, 태스크 그래프
- 심화: 스코프, 값 추가(append), 스코프 위임(`.value` 조회), 라이브러리 의존성, 플러그인 사용, 커스텀 설정/태스크, 빌드 구조화

sbt 레퍼런스 매뉴얼 전체는 Getting Started 외에도 FAQ·커뮤니티/마이그레이션 안내를 담은 General Information, 클래스패스·컴파일러·병렬 실행·테스트·원격 캐싱 등을 다루는 Detailed Topics, 실무 문제별 해법을 모은 How-To, 색인, Developer's Guide로 구성됨. 이 노트는 그중 설치와 프로젝트 초기 설정, `sbt-by-example` 튜토리얼, 디렉터리 구조 부분을 정리.

---

### 2. 설치 준비: JDK, 설치 방법(macOS/Windows/Linux)

#### 2.1 공통 사전 조건

sbt를 쓰려면 먼저 JDK 설치 필요. 공식 문서는 Eclipse Adoptium(Temurin) JDK 8, 11, 17 계열을 권장. sbt 자체는 JVM 위에서 동작 → 빌드 대상 프로젝트가 사용할 Scala/Java 버전과 sbt 실행에 쓰이는 JDK는 별개로 취급 가능.

#### 2.2 OS별 설치 방법 비교

세 플랫폼 모두 공통적으로 Coursier(`cs setup`)를 통한 설치를 가장 권장. Coursier로 Scala 툴체인을 설치하면 최신 안정 버전 sbt가 함께 설치됨. 패키지 매니저(brew, choco, scoop, apt, yum 등)로 설치하는 방법도 안내되어 있으나, 서드파티 패키지는 최신 버전을 즉시 반영하지 못할 수 있음(공식 문서 명시).

- macOS
  - 권장 방법: `cs setup`(Coursier)
  - 패키지 매니저: Homebrew(`brew install sbt`)
  - 수동 설치: zip/tgz 다운로드 후 압축 해제
  - 비고: SDKMAN도 지원
- Windows
  - 권장 방법: `cs setup`(Coursier)
  - 패키지 매니저: Scoop(`scoop install sbt`), Chocolatey(`choco install sbt`)
  - 수동 설치: MSI 설치 프로그램(`sbt-1.10.10.msi`), zip/tgz
  - 비고: GitHub Release에서 MSI 배포
- Linux
  - 권장 방법: `cs setup`(Coursier)
  - 패키지 매니저: Ubuntu/Debian은 apt 저장소 등록 후 `apt-get install sbt`, RedHat/Fedora/CentOS는 yum/dnf 저장소 등록 후 설치, Gentoo는 `emerge dev-java/sbt`
  - 수동 설치: zip/tgz 다운로드 후 압축 해제
  - 비고: SDKMAN 지원

macOS에서 SDKMAN을 쓰는 경우 예시:

```bash
$ sdk install java $(sdk list java | grep -o "\b8\.[0-9]*\.[0-9]*\-tem" | head -1)
$ sdk install sbt
```

```bash
$ brew install sbt
```

Windows에서 Scoop/Chocolatey를 쓰는 경우:

```
$ scoop install sbt
```

```
$ choco install sbt
```

Linux(Ubuntu/Debian 계열)에서 apt 저장소를 직접 등록해 설치하는 경우, sbt 공식 저장소를 추가하고 GPG 키를 등록한 뒤 설치.

```bash
sudo apt-get update
sudo apt-get install apt-transport-https curl gnupg -yqq
echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | sudo tee /etc/apt/sources.list.d/sbt.list
echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | sudo tee /etc/apt/sources.list.d/sbt_old.list
curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo -H gpg --no-default-keyring --keyring gnupg-ring:/etc/apt/trusted.gpg.d/scalasbt-release.gpg --import
sudo chmod 644 /etc/apt/trusted.gpg.d/scalasbt-release.gpg
sudo apt-get update
sudo apt-get install sbt
```

RedHat/Fedora/CentOS 계열은 yum 저장소 파일을 받아 등록한 뒤 설치.

```bash
sudo rm -f /etc/yum.repos.d/bintray-rpm.repo
curl -L https://www.scala-sbt.org/sbt-rpm.repo > sbt-rpm.repo
sudo mv sbt-rpm.repo /etc/yum.repos.d/
sudo yum install sbt
```

Fedora 31 이상에서는 `dnf`를 바로 쓸 수 있음.

```bash
sudo dnf install sbt
```

세 플랫폼 모두 zip/tgz 형태의 수동 설치 패키지(예: `sbt-1.10.10.zip`, `sbt-1.10.10.tgz`)를 내려받아 원하는 위치에 압축을 풀어 쓸 수도 있음. 어떤 방법을 택하든 sbt 실행 파일이 `PATH`에 잡히도록 해두면 이후 과정은 동일.

---

### 3. 프로젝트 설정 첫걸음

sbt 프로젝트를 만드는 데 필요한 절차는 크게 세 단계로 요약됨.

1. JDK 설치 (2장 참고)
2. sbt 설치 (2장 참고)
3. 간단한 hello world 프로젝트 구성

설치를 마쳤다면 sbt 실행 방법과 `.sbt` 확장자로 작성하는 빌드 정의 문법 학습이 다음 순서. 빌드 정의는 프로젝트 루트의 `build.sbt`, 그리고 필요에 따라 `project/` 디렉터리 아래 두는 `.scala` 헬퍼 파일들로 구성됨. 이어지는 4장의 `sbt by example` 튜토리얼이 빈 프로젝트에서 시작해 빌드 정의를 채워가는 과정을 예제로 보여줌.

---

### 4. 예제로 배우는 sbt (sbt by example)

`sbt-by-example` 튜토리얼은 빈 디렉터리에서 시작해 셸 사용, 컴파일, 라이브러리 의존성, 테스트, 멀티 프로젝트, 패키징까지 이어지는 흐름을 단계별로 보여줌.

#### 4.1 최소 빌드 만들기와 셸 진입

`build.sbt` 파일 하나만 있어도(내용이 비어 있어도) sbt는 그 디렉터리를 프로젝트로 인식.

```
$ mkdir foo-build
$ cd foo-build
$ touch build.sbt
```

sbt 셸은 대화형으로 여러 명령을 이어서 실행할 수 있는 환경.

```
$ sbt
```

셸을 나올 때는 `exit`를 입력하거나 Ctrl+D(Unix/macOS) 혹은 Ctrl+Z(Windows)를 누름.

#### 4.2 컴파일과 파일 감시(watch)

```
sbt:foo-build> compile
```

소스가 없으면 컴파일할 것이 없다는 메시지가 뜨지만, 이 명령 자체는 정상 동작. 명령 앞에 `~`를 붙이면 소스 파일 변경을 감지할 때마다 자동 재실행.

```
sbt:foo-build> ~compile
```

#### 4.3 소스 작성과 실행

Scala 소스는 관례상 `src/main/scala/` 아래에 둠.

```scala
// src/main/scala/example/Hello.scala
package example

object Hello {
  def main(args: Array[String]): Unit = {
    println("Hello")
  }
}
```

```
sbt:foo-build> run
[info] running example.Hello
Hello
```

셸에서는 위쪽 화살표 키로 이전 명령 히스토리를 다시 불러올 수 있고, `help`나 `help run`처럼 태스크 이름을 붙이면 해당 태스크 설명 확인 가능.

#### 4.4 셸에서 설정값 바꾸고 영구 저장하기

`set` 명령으로 셸 세션 안에서 임시로 설정값을 바꿀 수 있음.

```
sbt:foo-build> set ThisBuild / scalaVersion := "2.13.12"
sbt:foo-build> scalaVersion
[info] 2.13.12
```

`ThisBuild` 스코프는 이 빌드에 속한 모든 프로젝트에 적용되는 범위를 가리킴. 셸에서 `set`으로 바꾼 값은 세션이 끝나면 사라짐 → `session save`를 실행하면 그 변경 내용을 `build.sbt`에 써넣어 영구화 가능.

```
sbt:foo-build> session save
```

#### 4.5 프로젝트 이름 붙이기와 reload

`build.sbt`에 프로젝트를 명시적으로 선언하면 이름, 조직 등 메타데이터 지정 가능.

```scala
ThisBuild / scalaVersion := "2.13.12"
ThisBuild / organization := "com.example"

lazy val hello = (project in file("."))
  .settings(
    name := "Hello"
  )
```

`project`는 sbt가 제공하는 프로젝트 생성 헬퍼 → `lazy val`로 선언하는 이유는 프로젝트 간 순환 참조를 다루기 위한 초기화 순서 문제 때문. `build.sbt`를 직접 고친 뒤에는 셸에서 `reload`를 실행해야 변경 사항 반영.

```
sbt:Hello> reload
```

#### 4.6 라이브러리 의존성 추가와 테스트

`libraryDependencies` 키에 `+=`로 의존성을 덧붙임. `%%`는 프로젝트에서 쓰는 Scala 버전을 아티팩트 이름에 자동으로 끼워 넣으라는 뜻 · `% Test`는 이 의존성이 테스트 범위(Configuration)에서만 유효함을 뜻함.

```scala
lazy val hello = project
  .in(file("."))
  .settings(
    name := "Hello",
    libraryDependencies +=
      "org.scala-lang" %% "toolkit-test" % "0.1.7" % Test
  )
```

테스트 소스는 `src/test/scala/` 아래에 둠.

```scala
// src/test/scala/example/HelloSuite.scala
class HelloSuite extends munit.FunSuite {
  test("Hello should start with H") {
    assert("Hello".startsWith("H"))
  }
}
```

```
sbt:Hello> test
sbt:Hello> ~testQuick
```

`testQuick`은 이전 실행 이후 변경된 부분과 관련된 테스트만 골라 실행하는 태스크 → `~`와 함께 쓰면 저장할 때마다 관련 테스트만 반복 실행됨.

Scala REPL이 필요하면 `console` 태스크로 진입.

```
sbt:Hello> console
scala> :paste
```

REPL 종료는 `:q`.

#### 4.7 멀티 프로젝트 빌드

서브프로젝트를 하나 더 선언.

```scala
lazy val helloCore = project
  .in(file("core"))
  .settings(
    name := "Hello Core"
  )
```

```
sbt:Hello> projects
[info]   * hello
[info]     helloCore
```

`projects` 태스크로 현재 빌드에 속한 프로젝트 목록 확인 가능 · `프로젝트명/태스크` 형태로 특정 서브프로젝트만 지정해 태스크 실행 가능.

```
sbt:Hello> helloCore/compile
```

서브프로젝트 사이의 관계는 두 가지 메서드로 표현.

- `aggregate`: 상위 프로젝트에 내린 명령이 하위 프로젝트에도 함께 전파됨(태스크 브로드캐스트), 코드 의존은 아님
- `dependsOn`: 다른 프로젝트의 컴파일된 코드를 클래스패스에 가져와 실제로 참조·사용함

```scala
lazy val hello = project
  .aggregate(helloCore)
```

```scala
lazy val hello = project
  .dependsOn(helloCore)
```

여러 서브프로젝트가 같은 라이브러리를 나눠 쓸 때는 의존성 값 자체를 `val`로 뽑아 재사용.

```scala
val toolkitTest = "org.scala-lang" %% "toolkit-test" % "0.1.7"

lazy val hello = project
  .settings(
    libraryDependencies += toolkitTest % Test
  )

lazy val helloCore = project
  .settings(
    libraryDependencies += "org.scala-lang" %% "toolkit" % "0.1.7"
  )
```

#### 4.8 플러그인, 패키징, 버전, Scala 버전 전환

플러그인은 `project/plugins.sbt`에 선언. 아래는 sbt-native-packager를 추가하는 예.

```
// project/plugins.sbt
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.4")
```

`build.sbt`에서는 `enablePlugins`로 해당 플러그인이 제공하는 기능을 프로젝트에 켬.

```scala
lazy val hello = project
  .enablePlugins(JavaAppPackaging)
  .settings(
    maintainer := "A Scala Dev!"
  )
```

```
sbt:Hello> dist
[info] Your package is ready in
  /tmp/foo-build/target/universal/hello-0.1.0-SNAPSHOT.zip
```

`dist` 태스크는 실행 가능한 배포용 압축 파일을 만듦. 같은 플러그인은 Docker 이미지 빌드도 지원.

```
sbt:Hello> Docker/publishLocal
[info] Built image hello with tags [0.1.0-SNAPSHOT]
```

버전은 `ThisBuild / version`으로 지정.

```scala
ThisBuild / version := "0.1.0"
```

`++` 명령은 현재 셸 세션에서만 임시로 다른 Scala 버전으로 전환(끝에 `!`를 붙이면 강제 전환) → `reload`하면 원래 설정으로 복귀.

```
sbt:Hello> ++3.3.1!
```

태스크의 정의 위치나 의존 관계가 궁금하면 `help`, `inspect`, `inspect tree`로 조사 가능.

```
sbt:Hello> help dist
sbt:Hello> inspect dist
sbt:Hello> inspect tree dist
```

#### 4.9 배치 모드와 프로젝트 템플릿

셸에 들어가지 않고 커맨드라인에서 바로 태스크를 실행하는 배치 모드도 지원 → 다만 매번 JVM을 새로 띄우므로 셸 모드보다 느림.

```
$ sbt clean "testOnly HelloSuite"
```

`sbt new`로 g8 템플릿에서 새 프로젝트를 빠르게 생성 가능.

```
$ sbt new scala/scala-seed.g8
```

#### 4.10 핵심 개념 요약

- `build.sbt`: 빌드 정의가 담기는 파일
- `ThisBuild`: 빌드에 속한 모든 프로젝트에 적용되는 스코프
- `%%`: 의존성 아티팩트 이름에 Scala 버전을 자동으로 붙여줌
- `% Test`: 해당 Configuration(테스트 범위)에서만 유효한 의존성 지정
- `lazy val`: 프로젝트를 선언할 때 쓰는 지연 초기화 값
- `aggregate`: 태스크를 하위 프로젝트로 전파(코드 의존 아님)
- `dependsOn`: 다른 프로젝트의 컴파일 산출물을 실제로 참조
- `~`: 파일 변경을 감지해 태스크를 자동 재실행

---

### 5. 표준 디렉터리 구조

sbt는 베이스 디렉터리(`build.sbt`가 있는 루트 폴더)를 기준으로 Maven과 동일한 디렉터리 관례를 따름. 기본 구조는 다음과 같음.

- `src/main/scala/`: 메인 Scala 소스
- `src/main/scala-2.12/`: Scala 2.12 전용 소스(버전별 소스 분리 시)
- `src/main/java/`: 메인 Java 소스
- `src/main/resources/`: 메인 jar에 포함될 리소스 파일
- `src/test/scala/`: 테스트 Scala 소스
- `src/test/scala-2.12/`: 테스트용 Scala 2.12 전용 소스
- `src/test/java/`: 테스트 Java 소스
- `src/test/resources/`: 테스트 jar에 포함될 리소스 파일
- `build.sbt`: 빌드 정의 파일
- `project/`: 빌드 정의를 돕는 `.scala` 헬퍼와 플러그인 선언(`plugins.sbt`)이 위치하는 곳
- `target/`: 컴파일된 클래스, jar, 기타 생성 산출물이 쌓이는 곳

몇 가지 참고할 특징.

- 위 목록에 없는 숨김 디렉터리는 소스 탐색 시 무시됨.
- 소규모 프로젝트라면 위 관례를 따르지 않고 베이스 디렉터리 바로 아래에 `*.scala` 파일을 두어도 동작.
- 버전 관리 시스템에서는 `target/`를 반드시 무시 대상에 넣을 필요. `.gitignore`에는 다음과 같이 후행 슬래시를 붙여 디렉터리임을 명시하고, 선행 슬래시는 붙이지 않음.

```
target/
```

---

## sbt 실행, IDE 연동, 빌드 정의 기본 문법, 멀티 프로젝트

> 원본: https://www.scala-sbt.org/1.x/docs/Running.html, https://www.scala-sbt.org/1.x/docs/IDE.html, https://www.scala-sbt.org/1.x/docs/Basic-Def.html, https://www.scala-sbt.org/1.x/docs/Multi-Project.html

---

### 목차

1. [sbt shell 실행 방식](#1-sbt-shell-실행-방식)
2. [배치 모드와 연속 실행(watch)](#2-배치-모드와-연속-실행watch)
3. [주요 명령과 셸 편의 기능](#3-주요-명령과-셸-편의-기능)
4. [IDE 연동: Build Server Protocol](#4-ide-연동-build-server-protocol)
5. [Metals(VS Code) 연동](#5-metalsvs-code-연동)
6. [IntelliJ IDEA 연동](#6-intellij-idea-연동)
7. [Neovim + Metals 연동](#7-neovim--metals-연동)
8. [build.sbt와 세팅 표현식](#8-buildsbt와-세팅-표현식)
9. [키 종류: SettingKey / TaskKey / InputKey](#9-키-종류-settingkey--taskkey--inputkey)
10. [세팅과 태스크 정의하기](#10-세팅과-태스크-정의하기)
11. [라이브러리 의존성 추가](#11-라이브러리-의존성-추가)
12. [ThisBuild와 빌드 전역 설정](#12-thisbuild와-빌드-전역-설정)
13. [멀티 프로젝트 기본 선언](#13-멀티-프로젝트-기본-선언)
14. [공통 세팅 재사용](#14-공통-세팅-재사용)
15. [Aggregation(집계)](#15-aggregation집계)
16. [dependsOn(클래스패스 의존성)](#16-dependson클래스패스-의존성)
17. [설정별 의존성과 의존성 추적 제어](#17-설정별-의존성과-의존성-추적-제어)

---

### 1. sbt shell 실행 방식

sbt는 인자 없이 실행하면 대화형 셸(interactive shell)로 진입.

```
$ sbt
```

셸 안에서는 탭 완성과 명령 히스토리 사용 가능. 예를 들어 `compile`을 입력해 컴파일, `run`을 입력해 프로그램 실행. 셸 종료 시 `exit`를 입력하거나 Unix에서는 Ctrl+D, Windows에서는 Ctrl+Z를 누름.

### 2. 배치 모드와 연속 실행(watch)

셸에 진입하지 않고 명령을 순차적으로 실행하고 싶으면 배치 모드를 씀.

```
$ sbt clean compile "testOnly TestA TestB"
```

인자가 붙는 명령은 따옴표로 감쌈. 배치 모드는 매번 JVM을 새로 띄우고 JIT 워밍업을 다시 거침 → 셸 모드보다 실행 속도가 느림.

명령 앞에 `~`를 붙이면 소스 파일이 바뀔 때마다 해당 명령을 자동으로 재실행하는 연속 실행 모드가 됨.

```
> ~testQuick
```

Enter를 누르면 감시 중단. 이 접두사는 셸 모드와 배치 모드 어느 쪽에서도 동작.

### 3. 주요 명령과 셸 편의 기능

- `clean`: target 디렉터리에 생성된 산출물 삭제
- `compile`: `src/main/scala`, `src/main/java` 아래 메인 소스 컴파일
- `test`: 테스트 컴파일 및 전체 실행
- `console`: 프로젝트 클래스패스를 포함한 Scala REPL 시작
- `run <인자>*`: 메인 클래스를 같은 JVM 안에서 실행
- `package`: 리소스와 컴파일된 클래스를 묶어 jar 생성
- `help <명령>`: 해당 명령의 설명 출력
- `reload`: 빌드 정의 파일 다시 로드

셸에서는 다음과 같은 편의 기능도 제공.

- 탭 완성: 어느 프롬프트에서든 사용 가능 · 여러 번 누르면 확장된 후보를 보여줌
- 히스토리 탐색: 위쪽 화살표로 이전 명령을 순회하고, Ctrl+R로 히스토리를 역순 검색. `!`는 히스토리 도움말, `!!`는 마지막 명령 재실행, `!n`은 인덱스로 명령 실행, `!문자열`은 해당 문자열로 시작하는 가장 최근 명령을 실행

### 4. IDE 연동: Build Server Protocol

sbt는 Build Server Protocol(BSP)을 통해 여러 빌드 서버와 IDE가 공통 인터페이스로 통신하도록 지원. BSP를 쓰면 IDE가 sbt를 직접 조작하지 않고도 컴파일 상태·진단 정보·클래스패스 등을 표준화된 방식으로 주고받을 수 있음.

특정 서브프로젝트를 BSP 노출 대상에서 제외하고 싶으면 다음 세팅을 끔.

```scala
bspEnabled := false
```

### 5. Metals(VS Code) 연동

Metals는 VS Code에서 Scala 프로젝트를 다루기 위한 언어 서버. 연동 절차는 다음과 같음.

1. Metals 확장 설치
2. `build.sbt`가 있는 디렉터리 열기
3. Command Palette에서 "Metals: Switch build server" 선택 후 sbt 지정
4. 파일 저장 시 Metals가 내부적으로 sbt를 호출해 실제 빌드 작업 수행

Metals는 코드 완성과 중단점·변수 검사를 포함한 대화형 디버깅 지원. `sbt --client` 명령으로 실행 중인 sbt 서버에 씬 클라이언트로 접속도 가능.

### 6. IntelliJ IDEA 연동

IntelliJ에서 sbt 프로젝트를 여는 방법은 두 가지.

- 전통적인 임포트 방식: Scala 플러그인을 설치한 뒤 `build.sbt`가 있는 디렉터리 열기. 기본적으로 IntelliJ 자체의 경량 컴파일러로 오류 검출 → 필요 시 scalac 기반 하이라이팅으로 전환 가능
- BSP 방식(권장, 고급)
  1. Scala 플러그인 설치
  2. 일반적인 "Open"이 아니라 "Project From Existing Sources" 선택
  3. 프롬프트에서 BSP 선택
  4. 임포트 도구로 "sbt (recommended)" 선택
  5. 설정에서 "저장 시 자동 빌드"를 켜고, "임포트 전 sbt 프로젝트를 Bloop로 내보내기"는 끔

BSP 노출 여부는 앞서 언급한 `bspEnabled := false`로 동일하게 제어. 디버깅은 테스트를 우클릭해 "Debug"를 선택하거나 인라인 실행 아이콘을 클릭하면 됨.

### 7. Neovim + Metals 연동

Neovim에서는 nvim-metals 플러그인을 통해 LSP 기반으로 Metals 연동. `$XDG_CONFIG_HOME/nvim/lua/` 아래 lsp 설정 파일 필요 · `:MetalsInstall`과 `:MetalsStartServer` 명령으로 설치와 서버 실행 진행. 주요 키바인딩은 다음과 같음.

- `gD`: 정의로 이동
- `K`: 호버 정보 표시
- `<leader>aa`: 코드 액션/진단 표시
- `<leader>dt`: 디버그 토글

### 8. build.sbt와 세팅 표현식

`build.sbt`는 프로젝트(서브프로젝트) 구성을 담는 파일로, 키-값 형태의 세팅 표현식으로 구성됨. 사용할 sbt 버전은 `project/build.properties`에 명시해 여러 환경에서 동일한 런처 버전을 쓰도록 고정.

```
sbt.version=1.10.10
```

세팅 표현식은 세 요소로 구성됨.

1. 키(좌변): `SettingKey[T]`, `TaskKey[T]`, `InputKey[T]`의 인스턴스
2. 연산자: 보통 `:=`
3. 본문(우변): 대입할 값 또는 계산식

```scala
lazy val root = (project in file("."))
  .settings(
    name := "Hello",
    scalaVersion := "2.12.7"
  )
```

`build.sbt`에는 다음 임포트가 암묵적으로 적용됨.

```scala
import sbt._
import Keys._
```

### 9. 키 종류: SettingKey / TaskKey / InputKey

- `SettingKey[T]`: 프로젝트 로드 시 한 번 평가되고 값이 고정됨
- `TaskKey[T]`: 참조될 때마다 다시 평가됨
- `InputKey[T]`: 커맨드라인 인자를 받는 태스크

세팅은 태스크에 의존 불가 → 세팅은 로드 시점에 한 번만 평가되므로 매번 다시 계산되는 태스크의 결과를 담을 수 없기 때문. 반대로 태스크는 세팅과 다른 태스크 양쪽 모두에 의존 가능.

커스텀 키는 다음처럼 직접 선언.

```scala
lazy val hello = taskKey[Unit]("An example task")
```

### 10. 세팅과 태스크 정의하기

세팅은 계산된 값을 그대로 저장.

```scala
lazy val root = (project in file("."))
  .settings(
    name := "hello"
  )
```

태스크는 호출될 때마다 코드를 실행.

```scala
lazy val hello = taskKey[Unit]("An example task")

lazy val root = (project in file("."))
  .settings(
    hello := { println("Hello!") }
  )
```

`TaskKey[T]`는 실제로는 `Setting[Task[T]]`를 생성. 즉 "태스크"라는 성질 자체가 키에 담긴 세팅이고, 그 세팅이 매번 재실행되는 계산(Task)을 값으로 가진다고 이해하면 됨. 값의 타입이 키의 타입과 맞지 않으면 컴파일 단계에서 오류 발생 → 세팅/태스크 정의는 타입 안전하게 검사됨.

### 11. 라이브러리 의존성 추가

의존성 목록처럼 이미 값이 있는 키에 항목을 덧붙일 때는 `:=` 대신 `+=`(단일 추가)나 `++=`(복수 추가)를 씀.

```scala
val derby = "org.apache.derby" % "derby" % "10.4.1.3"

lazy val root = (project in file("."))
  .settings(
    libraryDependencies += derby
  )
```

`%` 메서드는 문자열로부터 Ivy 모듈 식별자를 구성.

### 12. ThisBuild와 빌드 전역 설정

여러 서브프로젝트에 공통으로 적용할 값은 `ThisBuild` 스코프에 지정.

```scala
ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.12.18"
ThisBuild / version := "0.1.0-SNAPSHOT"
```

`ThisBuild` 스코프에 지정한 값은 각 서브프로젝트가 별도로 재정의하지 않는 한 기본값으로 그대로 상속됨.

### 13. 멀티 프로젝트 기본 선언

프로젝트는 `Project` 타입의 `lazy val`로 선언 → 변수명이 곧 서브프로젝트 ID가 됨.

```scala
lazy val util = (project in file("util"))

lazy val core = (project in file("core"))
```

베이스 디렉터리가 변수명과 같으면 `in file(...)` 부분을 생략 가능.

```scala
lazy val util = project

lazy val core = project
```

명시적으로 루트 프로젝트를 정의하지 않으면 sbt가 나머지 모든 프로젝트를 집계하는 기본 루트 프로젝트를 자동 생성.

### 14. 공통 세팅 재사용

여러 프로젝트에서 반복되는 세팅은 `Seq[Setting[_]]`로 뽑아내 재사용 가능.

```scala
lazy val commonSettings = Seq(
  target := { baseDirectory.value / "target2" }
)

lazy val core = (project in file("core"))
  .settings(
    commonSettings,
    // 다른 세팅들
  )

lazy val util = (project in file("util"))
  .settings(
    commonSettings,
    // 다른 세팅들
  )
```

### 15. Aggregation(집계)

집계 프로젝트에서 태스크를 실행하면 집계된 프로젝트들에서도 같은 태스크가 실행됨.

```scala
lazy val root = (project in file("."))
  .aggregate(util, core)

lazy val util = (project in file("util"))

lazy val core = (project in file("core"))
```

특정 태스크만 집계에서 제외하고 싶으면 해당 태스크의 `aggregate` 세팅을 끔.

```scala
lazy val root = (project in file("."))
  .aggregate(util, core)
  .settings(
    update / aggregate := false
  )
```

### 16. dependsOn(클래스패스 의존성)

`dependsOn`은 컴파일 순서를 정하고 대상 프로젝트의 클래스패스를 포함시키는 클래스패스 의존성을 만듦.

```scala
lazy val core = project.dependsOn(util)
```

의존 대상이 여러 개면 나열.

```scala
lazy val core = project.dependsOn(bar, baz)
```

#### 설정별 의존성

`%`와 `->` 표기로 어떤 설정(configuration)이 어떤 설정에 의존하는지 세밀하게 지정 가능.

```scala
dependsOn(util % "compile->compile")
dependsOn(util % "test->compile")
dependsOn(util % "test->test;compile->compile")
```

대상 설정을 생략하면 기본값은 `compile`.

### 17. 설정별 의존성과 의존성 추적 제어

대규모 멀티 프로젝트 빌드에서 프로젝트 간 컴파일 트리거를 세밀하게 제어하고 싶을 때는 다음 세팅을 씀.

```scala
ThisBuild / trackInternalDependencies := TrackLevel.TrackIfMissing
ThisBuild / exportJars := true

lazy val root = (project in file("."))
  .aggregate(/* ... */)
```

특정 프로젝트만 이 추적에서 제외하고 싶으면 그 프로젝트에 개별로 지정.

```scala
lazy val dontTrackMe = (project in file("dontTrackMe"))
  .settings(
    exportToInternal := TrackLevel.NoTracking
  )
```

### 셸 명령: 프로젝트 탐색

멀티 프로젝트 빌드에서는 셸 안에서 다음 명령으로 프로젝트를 오감.

- `projects`: 빌드에 정의된 전체 프로젝트 목록 표시
- `project <이름>`: 현재 프로젝트를 지정한 프로젝트로 전환
- `<서브프로젝트ID>/compile`: 현재 프로젝트를 바꾸지 않고 특정 프로젝트의 태스크만 실행

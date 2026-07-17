# sbt 시작하기: 개요, 설치, 프로젝트 설정

> 원본: https://www.scala-sbt.org/1.x/docs/Getting-Started.html, https://www.scala-sbt.org/1.x/docs/Setup.html, https://www.scala-sbt.org/1.x/docs/Installing-sbt-on-Mac.html, https://www.scala-sbt.org/1.x/docs/Installing-sbt-on-Windows.html, https://www.scala-sbt.org/1.x/docs/Installing-sbt-on-Linux.html, https://www.scala-sbt.org/1.x/docs/sbt-by-example.html, https://www.scala-sbt.org/1.x/docs/Directories.html

---

## 목차

1. [sbt란 무엇인가](#1-sbt란-무엇인가)
2. [설치 준비: JDK, 설치 방법(macOS/Windows/Linux)](#2-설치-준비-jdk-설치-방법macoswindowslinux)
3. [프로젝트 설정 첫걸음](#3-프로젝트-설정-첫걸음)
4. [예제로 배우는 sbt (sbt by example)](#4-예제로-배우는-sbt-sbt-by-example)
5. [표준 디렉터리 구조](#5-표준-디렉터리-구조)

---

## 1. sbt란 무엇인가

sbt는 Scala와 Java 프로젝트를 위한 빌드 도구다. Maven, Gradle 같은 다른 빌드 도구와 개념 모델이 다르다는 점을 공식 문서에서도 강조하는데, 특히 아래 세 가지 개념을 이해하지 않고 넘어가면 나중에 헤매기 쉽다.

- **빌드 정의(build definition)**: `build.sbt`와 `project/` 아래의 `.scala` 파일들로 구성되는, 프로젝트를 기술하는 코드
- **스코프(scope)**: 같은 키(`scalaVersion` 등)라도 프로젝트·설정(Compile/Test)·태스크에 따라 다른 값을 가질 수 있게 하는 범위 개념
- **태스크 그래프(task graph)**: `compile`, `test`, `run` 같은 태스크들이 서로 의존하며 이루는 실행 그래프

Getting Started 가이드는 다음 순서로 학습을 진행하도록 구성되어 있다.

| 단계 | 다루는 내용 |
|------|------|
| 설치 | OS별 sbt 설치, 예제로 배우는 sbt, 디렉터리 구조, 실행 방법 |
| 핵심 개념 | IDE 연동, 빌드 정의, 멀티 프로젝트 빌드, 태스크 그래프 |
| 심화 | 스코프, 값 추가(append), 스코프 위임(`.value` 조회), 라이브러리 의존성, 플러그인 사용, 커스텀 설정/태스크, 빌드 구조화 |

sbt 레퍼런스 매뉴얼 전체는 Getting Started 외에도 FAQ, 커뮤니티/마이그레이션 안내를 담은 General Information, 클래스패스·컴파일러·병렬 실행·테스트·원격 캐싱 등을 다루는 Detailed Topics, 실무 문제별 해법을 모은 How-To, 색인, Developer's Guide로 구성되어 있다. 이 노트는 그중 설치와 프로젝트 초기 설정, `sbt-by-example` 튜토리얼, 디렉터리 구조 부분을 정리한다.

---

## 2. 설치 준비: JDK, 설치 방법(macOS/Windows/Linux)

### 2.1 공통 사전 조건

sbt를 쓰려면 먼저 JDK가 설치되어 있어야 한다. 공식 문서는 Eclipse Adoptium(Temurin) JDK 8, 11, 17 계열을 권장한다. sbt 자체는 JVM 위에서 동작하며, 빌드 대상 프로젝트가 사용할 Scala/Java 버전과 sbt 실행에 쓰이는 JDK는 별개로 생각해도 된다.

### 2.2 OS별 설치 방법 비교

세 플랫폼 모두 공통적으로 **Coursier(`cs setup`)를 통한 설치**를 가장 권장하는 방법으로 안내한다. Coursier로 Scala 툴체인을 설치하면 최신 안정 버전 sbt가 함께 딸려온다. 패키지 매니저(brew, choco, scoop, apt, yum 등)로 설치하는 방법도 안내되어 있지만, 서드파티 패키지는 최신 버전을 즉시 반영하지 못할 수 있다는 점을 공식 문서가 명시하고 있다.

| 플랫폼 | 권장 방법 | 패키지 매니저 | 수동 설치 | 비고 |
|--------|-----------|---------------|-----------|------|
| macOS | `cs setup` (Coursier) | Homebrew: `brew install sbt` | zip/tgz 다운로드 후 압축 해제 | SDKMAN도 지원 |
| Windows | `cs setup` (Coursier) | Scoop: `scoop install sbt`, Chocolatey: `choco install sbt` | MSI 설치 프로그램(`sbt-1.10.10.msi`), zip/tgz | GitHub Release에서 MSI 배포 |
| Linux | `cs setup` (Coursier) | Ubuntu/Debian: apt 저장소 등록 후 `apt-get install sbt`, RedHat/Fedora/CentOS: yum/dnf 저장소 등록 후 설치, Gentoo: `emerge dev-java/sbt` | zip/tgz 다운로드 후 압축 해제 | SDKMAN 지원 |

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

Linux(Ubuntu/Debian 계열)에서 apt 저장소를 직접 등록해 설치하는 경우, sbt 공식 저장소를 추가하고 GPG 키를 등록한 뒤 설치한다.

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

RedHat/Fedora/CentOS 계열은 yum 저장소 파일을 받아 등록한 뒤 설치한다.

```bash
sudo rm -f /etc/yum.repos.d/bintray-rpm.repo
curl -L https://www.scala-sbt.org/sbt-rpm.repo > sbt-rpm.repo
sudo mv sbt-rpm.repo /etc/yum.repos.d/
sudo yum install sbt
```

Fedora 31 이상에서는 `dnf`를 바로 쓸 수 있다.

```bash
sudo dnf install sbt
```

세 플랫폼 모두 zip/tgz 형태의 수동 설치 패키지(예: `sbt-1.10.10.zip`, `sbt-1.10.10.tgz`)를 내려받아 원하는 위치에 압축을 풀어 쓸 수도 있다. 어떤 방법을 택하든 sbt 실행 파일이 `PATH`에 잡히도록 해두면 이후 과정은 동일하다.

---

## 3. 프로젝트 설정 첫걸음

sbt 프로젝트를 만드는 데 필요한 절차는 크게 세 단계로 요약된다.

1. JDK 설치 (2장 참고)
2. sbt 설치 (2장 참고)
3. 간단한 hello world 프로젝트 구성

설치를 마쳤다면 sbt를 실행하는 방법과 `.sbt` 확장자로 작성하는 빌드 정의 문법을 익히는 것이 다음 순서다. 빌드 정의는 프로젝트 루트의 `build.sbt`, 그리고 필요에 따라 `project/` 디렉터리 아래 두는 `.scala` 헬퍼 파일들로 구성된다. 이어지는 4장의 `sbt by example` 튜토리얼이 실제로 빈 프로젝트에서 시작해 빌드 정의를 채워가는 과정을 예제로 보여준다.

---

## 4. 예제로 배우는 sbt (sbt by example)

`sbt-by-example` 튜토리얼은 빈 디렉터리에서 시작해 셸 사용, 컴파일, 라이브러리 의존성, 테스트, 멀티 프로젝트, 패키징까지 이어지는 흐름을 단계별로 보여준다.

### 4.1 최소 빌드 만들기와 셸 진입

`build.sbt` 파일 하나만 있어도(내용이 비어 있어도) sbt는 그 디렉터리를 프로젝트로 인식한다.

```
$ mkdir foo-build
$ cd foo-build
$ touch build.sbt
```

sbt 셸은 대화형으로 여러 명령을 이어서 실행할 수 있는 환경이다.

```
$ sbt
```

셸을 나올 때는 `exit`를 입력하거나 Ctrl+D(Unix/macOS) 혹은 Ctrl+Z(Windows)를 누른다.

### 4.2 컴파일과 파일 감시(watch)

```
sbt:foo-build> compile
```

소스가 없으면 컴파일할 것이 없다는 메시지가 뜨지만, 이 명령 자체는 정상 동작한다. 명령 앞에 `~`를 붙이면 소스 파일 변경을 감지할 때마다 자동으로 재실행한다.

```
sbt:foo-build> ~compile
```

### 4.3 소스 작성과 실행

Scala 소스는 관례상 `src/main/scala/` 아래에 둔다.

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

셸에서는 위 화살표(↑)로 이전 명령 히스토리를 다시 불러올 수 있고, `help`나 `help run`처럼 태스크 이름을 붙이면 해당 태스크에 대한 설명을 볼 수 있다.

### 4.4 셸에서 설정값 바꾸고 영구 저장하기

`set` 명령으로 셸 세션 안에서 임시로 설정값을 바꿀 수 있다.

```
sbt:foo-build> set ThisBuild / scalaVersion := "2.13.12"
sbt:foo-build> scalaVersion
[info] 2.13.12
```

`ThisBuild` 스코프는 이 빌드에 속한 모든 프로젝트에 적용되는 범위를 가리킨다. 셸에서 `set`으로 바꾼 값은 세션이 끝나면 사라지는데, `session save`를 실행하면 그 변경 내용을 `build.sbt`에 실제로 써넣어 영구화할 수 있다.

```
sbt:foo-build> session save
```

### 4.5 프로젝트 이름 붙이기와 reload

`build.sbt`에 프로젝트를 명시적으로 선언하면 이름, 조직 등 메타데이터를 지정할 수 있다.

```scala
ThisBuild / scalaVersion := "2.13.12"
ThisBuild / organization := "com.example"

lazy val hello = (project in file("."))
  .settings(
    name := "Hello"
  )
```

`project`는 sbt가 제공하는 프로젝트 생성 헬퍼이고, `lazy val`로 선언하는 이유는 프로젝트 간 순환 참조를 다루기 위한 초기화 순서 문제 때문이다. `build.sbt`를 직접 고친 뒤에는 셸에서 `reload`를 실행해야 변경 사항이 반영된다.

```
sbt:Hello> reload
```

### 4.6 라이브러리 의존성 추가와 테스트

`libraryDependencies` 키에 `+=`로 의존성을 덧붙인다. `%%`는 프로젝트에서 쓰는 Scala 버전을 아티팩트 이름에 자동으로 끼워 넣으라는 뜻이고, `% Test`는 이 의존성이 테스트 범위(Configuration)에서만 유효함을 뜻한다.

```scala
lazy val hello = project
  .in(file("."))
  .settings(
    name := "Hello",
    libraryDependencies +=
      "org.scala-lang" %% "toolkit-test" % "0.1.7" % Test
  )
```

테스트 소스는 `src/test/scala/` 아래에 둔다.

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

`testQuick`은 이전 실행 이후 변경된 부분과 관련된 테스트만 골라 실행하는 태스크로, `~`와 함께 쓰면 저장할 때마다 관련 테스트만 반복 실행된다.

Scala REPL이 필요하면 `console` 태스크로 진입한다.

```
sbt:Hello> console
scala> :paste
```

REPL 종료는 `:q`.

### 4.7 멀티 프로젝트 빌드

서브프로젝트를 하나 더 선언한다.

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

`projects` 태스크로 현재 빌드에 속한 프로젝트 목록을 확인할 수 있고, `프로젝트명/태스크` 형태로 특정 서브프로젝트만 지정해 태스크를 실행할 수 있다.

```
sbt:Hello> helloCore/compile
```

서브프로젝트 사이의 관계는 두 가지 메서드로 표현한다.

| 메서드 | 의미 |
|--------|------|
| `aggregate` | 상위 프로젝트에 내린 명령이 하위 프로젝트에도 함께 전파됨(태스크 브로드캐스트). 코드 의존은 아님 |
| `dependsOn` | 다른 프로젝트의 컴파일된 코드를 클래스패스에 가져와 실제로 참조·사용함 |

```scala
lazy val hello = project
  .aggregate(helloCore)
```

```scala
lazy val hello = project
  .dependsOn(helloCore)
```

여러 서브프로젝트가 같은 라이브러리를 나눠 쓸 때는 의존성 값 자체를 `val`로 뽑아 재사용한다.

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

### 4.8 플러그인, 패키징, 버전, Scala 버전 전환

플러그인은 `project/plugins.sbt`에 선언한다. 아래는 sbt-native-packager를 추가하는 예다.

```
// project/plugins.sbt
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.4")
```

`build.sbt`에서는 `enablePlugins`로 해당 플러그인이 제공하는 기능을 프로젝트에 켠다.

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

`dist` 태스크는 실행 가능한 배포용 압축 파일을 만든다. 같은 플러그인은 Docker 이미지 빌드도 지원한다.

```
sbt:Hello> Docker/publishLocal
[info] Built image hello with tags [0.1.0-SNAPSHOT]
```

버전은 `ThisBuild / version`으로 지정한다.

```scala
ThisBuild / version := "0.1.0"
```

`++` 명령은 현재 셸 세션에서만 임시로 다른 Scala 버전으로 전환한다(끝에 `!`를 붙이면 강제 전환). `reload`하면 원래 설정으로 돌아간다.

```
sbt:Hello> ++3.3.1!
```

태스크의 정의 위치나 의존 관계가 궁금하면 `help`, `inspect`, `inspect tree`로 조사할 수 있다.

```
sbt:Hello> help dist
sbt:Hello> inspect dist
sbt:Hello> inspect tree dist
```

### 4.9 배치 모드와 프로젝트 템플릿

셸에 들어가지 않고 커맨드라인에서 바로 태스크를 실행하는 배치 모드도 지원한다. 다만 매번 JVM을 새로 띄우므로 셸 모드보다 느리다.

```
$ sbt clean "testOnly HelloSuite"
```

`sbt new`로 g8 템플릿에서 새 프로젝트를 빠르게 생성할 수도 있다.

```
$ sbt new scala/scala-seed.g8
```

### 4.10 핵심 개념 요약

| 개념 | 설명 |
|------|------|
| `build.sbt` | 빌드 정의가 담기는 파일 |
| `ThisBuild` | 빌드에 속한 모든 프로젝트에 적용되는 스코프 |
| `%%` | 의존성 아티팩트 이름에 Scala 버전을 자동으로 붙여줌 |
| `% Test` | 해당 Configuration(테스트 범위)에서만 유효한 의존성 지정 |
| `lazy val` | 프로젝트를 선언할 때 쓰는 지연 초기화 값 |
| `aggregate` | 태스크를 하위 프로젝트로 전파(코드 의존 아님) |
| `dependsOn` | 다른 프로젝트의 컴파일 산출물을 실제로 참조 |
| `~` | 파일 변경을 감지해 태스크를 자동 재실행 |

---

## 5. 표준 디렉터리 구조

sbt는 베이스 디렉터리(`build.sbt`가 있는 루트 폴더)를 기준으로 Maven과 동일한 디렉터리 관례를 따른다. 기본 구조는 다음과 같다.

| 경로 | 용도 |
|------|------|
| `src/main/scala/` | 메인 Scala 소스 |
| `src/main/scala-2.12/` | Scala 2.12 전용 소스(버전별 소스 분리 시) |
| `src/main/java/` | 메인 Java 소스 |
| `src/main/resources/` | 메인 jar에 포함될 리소스 파일 |
| `src/test/scala/` | 테스트 Scala 소스 |
| `src/test/scala-2.12/` | 테스트용 Scala 2.12 전용 소스 |
| `src/test/java/` | 테스트 Java 소스 |
| `src/test/resources/` | 테스트 jar에 포함될 리소스 파일 |
| `build.sbt` | 빌드 정의 파일 |
| `project/` | 빌드 정의를 돕는 `.scala` 헬퍼와 플러그인 선언(`plugins.sbt`)이 위치하는 곳 |
| `target/` | 컴파일된 클래스, jar, 기타 생성 산출물이 쌓이는 곳 |

몇 가지 참고할 특징이 있다.

- 위 목록에 없는 숨김 디렉터리는 소스 탐색 시 무시된다.
- 소규모 프로젝트라면 위 관례를 따르지 않고 베이스 디렉터리 바로 아래에 `*.scala` 파일을 두어도 동작한다.
- 버전 관리 시스템에서는 `target/`를 반드시 무시 대상에 넣어야 한다. `.gitignore`에는 다음과 같이 후행 슬래시를 붙여 디렉터리임을 명시하고, 선행 슬래시는 붙이지 않는다.

```
target/
```

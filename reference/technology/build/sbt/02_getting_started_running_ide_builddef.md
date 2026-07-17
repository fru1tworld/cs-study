# sbt 실행, IDE 연동, 빌드 정의 기본 문법, 멀티 프로젝트

> 원본: https://www.scala-sbt.org/1.x/docs/Running.html, https://www.scala-sbt.org/1.x/docs/IDE.html, https://www.scala-sbt.org/1.x/docs/Basic-Def.html, https://www.scala-sbt.org/1.x/docs/Multi-Project.html

---

## 목차

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

## 1. sbt shell 실행 방식

sbt는 인자 없이 실행하면 대화형 셸(interactive shell)로 진입한다.

```
$ sbt
```

셸 안에서는 탭 완성과 명령 히스토리를 사용할 수 있다. 예를 들어 `compile`을 입력해 컴파일하고, `run`을 입력해 프로그램을 실행한다. 셸을 종료할 때는 `exit`를 입력하거나 Unix에서는 Ctrl+D, Windows에서는 Ctrl+Z를 누른다.

## 2. 배치 모드와 연속 실행(watch)

셸에 진입하지 않고 명령을 순차적으로 실행하고 싶으면 배치 모드를 쓴다.

```
$ sbt clean compile "testOnly TestA TestB"
```

인자가 붙는 명령은 따옴표로 감싼다. 배치 모드는 매번 JVM을 새로 띄우고 JIT 워밍업을 다시 거치므로, 셸 모드보다 실행 속도가 느리다는 점을 감안해야 한다.

명령 앞에 `~`를 붙이면 소스 파일이 바뀔 때마다 해당 명령을 자동으로 재실행하는 연속 실행 모드가 된다.

```
> ~testQuick
```

Enter를 누르면 감시를 멈춘다. 이 접두사는 셸 모드와 배치 모드 어느 쪽에서도 동작한다.

## 3. 주요 명령과 셸 편의 기능

| 명령 | 설명 |
|------|------|
| `clean` | target 디렉터리에 생성된 산출물 삭제 |
| `compile` | `src/main/scala`, `src/main/java` 아래 메인 소스 컴파일 |
| `test` | 테스트 컴파일 및 전체 실행 |
| `console` | 프로젝트 클래스패스를 포함한 Scala REPL 시작 |
| `run <인자>*` | 메인 클래스를 같은 JVM 안에서 실행 |
| `package` | 리소스와 컴파일된 클래스를 묶어 jar 생성 |
| `help <명령>` | 해당 명령의 설명 출력 |
| `reload` | 빌드 정의 파일 다시 로드 |

셸에서는 다음과 같은 편의 기능도 제공한다.

- **탭 완성**: 어느 프롬프트에서든 사용 가능하며, 여러 번 누르면 확장된 후보를 보여준다.
- **히스토리 탐색**: 위쪽 화살표로 이전 명령을 순회하고, Ctrl+R로 히스토리를 역순 검색한다. `!`는 히스토리 도움말, `!!`는 마지막 명령 재실행, `!n`은 인덱스로 명령 실행, `!문자열`은 해당 문자열로 시작하는 가장 최근 명령을 실행한다.

## 4. IDE 연동: Build Server Protocol

sbt는 Build Server Protocol(BSP)을 통해 여러 빌드 서버와 IDE가 공통 인터페이스로 통신하도록 지원한다. BSP를 쓰면 IDE가 sbt를 직접 조작하지 않고도 컴파일 상태, 진단 정보, 클래스패스 등을 표준화된 방식으로 주고받을 수 있다.

특정 서브프로젝트를 BSP 노출 대상에서 제외하고 싶으면 다음 세팅을 끈다.

```scala
bspEnabled := false
```

## 5. Metals(VS Code) 연동

Metals는 VS Code에서 Scala 프로젝트를 다루기 위한 언어 서버다. 연동 절차는 다음과 같다.

1. Metals 확장을 설치한다.
2. `build.sbt`가 있는 디렉터리를 연다.
3. Command Palette에서 "Metals: Switch build server"를 선택하고 sbt를 고른다.
4. 파일을 저장하면 Metals가 내부적으로 sbt를 호출해 실제 빌드 작업을 수행한다.

Metals는 코드 완성과 중단점·변수 검사를 포함한 대화형 디버깅을 지원한다. `sbt --client` 명령으로 실행 중인 sbt 서버에 씬 클라이언트로 접속할 수도 있다.

## 6. IntelliJ IDEA 연동

IntelliJ에서 sbt 프로젝트를 여는 방법은 두 가지다.

**전통적인 임포트 방식**: Scala 플러그인을 설치한 뒤 `build.sbt`가 있는 디렉터리를 연다. 기본적으로 IntelliJ 자체의 경량 컴파일러로 오류를 검출하며, 필요하면 scalac 기반 하이라이팅으로 바꿀 수 있다.

**BSP 방식(권장, 고급)**:

1. Scala 플러그인을 설치한다.
2. 일반적인 "Open"이 아니라 "Project From Existing Sources"를 선택한다.
3. 프롬프트에서 BSP를 선택한다.
4. 임포트 도구로 "sbt (recommended)"를 선택한다.
5. 설정에서 "저장 시 자동 빌드"를 켜고, "임포트 전 sbt 프로젝트를 Bloop로 내보내기"는 끈다.

BSP 노출 여부는 앞서 언급한 `bspEnabled := false`로 동일하게 제어한다. 디버깅은 테스트를 우클릭해 "Debug"를 선택하거나, 인라인 실행 아이콘을 클릭하면 된다.

## 7. Neovim + Metals 연동

Neovim에서는 nvim-metals 플러그인을 통해 LSP 기반으로 Metals를 연동한다. `$XDG_CONFIG_HOME/nvim/lua/` 아래 lsp 설정 파일이 필요하며, `:MetalsInstall`과 `:MetalsStartServer` 명령으로 설치와 서버 실행을 진행한다. 주요 키바인딩은 다음과 같다.

| 키 | 동작 |
|----|------|
| `gD` | 정의로 이동 |
| `K` | 호버 정보 표시 |
| `<leader>aa` | 코드 액션/진단 표시 |
| `<leader>dt` | 디버그 토글 |

## 8. build.sbt와 세팅 표현식

`build.sbt`는 프로젝트(서브프로젝트) 구성을 담는 파일로, 키-값 형태의 **세팅 표현식**으로 이루어진다. 사용할 sbt 버전은 `project/build.properties`에 명시해 여러 환경에서 동일한 런처 버전을 쓰도록 고정한다.

```
sbt.version=1.10.10
```

세팅 표현식은 세 요소로 구성된다.

1. **키**(좌변): `SettingKey[T]`, `TaskKey[T]`, `InputKey[T]`의 인스턴스
2. **연산자**: 보통 `:=`
3. **본문**(우변): 대입할 값 또는 계산식

```scala
lazy val root = (project in file("."))
  .settings(
    name := "Hello",
    scalaVersion := "2.12.7"
  )
```

`build.sbt`에는 다음 임포트가 암묵적으로 적용된다.

```scala
import sbt._
import Keys._
```

## 9. 키 종류: SettingKey / TaskKey / InputKey

| 타입 | 평가 시점 |
|------|-----------|
| `SettingKey[T]` | 프로젝트 로드 시 한 번 평가되고 값이 고정됨 |
| `TaskKey[T]` | 참조될 때마다 다시 평가됨 |
| `InputKey[T]` | 커맨드라인 인자를 받는 태스크 |

세팅은 태스크에 의존할 수 없다. 세팅은 로드 시점에 한 번만 평가되므로, 매번 다시 계산되는 태스크의 결과를 담을 수 없기 때문이다. 반대로 태스크는 세팅과 다른 태스크 양쪽 모두에 의존할 수 있다.

커스텀 키는 다음처럼 직접 선언한다.

```scala
lazy val hello = taskKey[Unit]("An example task")
```

## 10. 세팅과 태스크 정의하기

**세팅**은 계산된 값을 그대로 저장한다.

```scala
lazy val root = (project in file("."))
  .settings(
    name := "hello"
  )
```

**태스크**는 호출될 때마다 코드를 실행한다.

```scala
lazy val hello = taskKey[Unit]("An example task")

lazy val root = (project in file("."))
  .settings(
    hello := { println("Hello!") }
  )
```

`TaskKey[T]`는 실제로는 `Setting[Task[T]]`를 생성한다. 즉 "태스크"라는 성질 자체가 키에 담긴 세팅이고, 그 세팅이 매번 재실행되는 계산(Task)을 값으로 가진다고 이해하면 된다. 값의 타입이 키의 타입과 맞지 않으면 컴파일 단계에서 오류가 나므로, 세팅/태스크 정의는 타입 안전하게 검사된다.

## 11. 라이브러리 의존성 추가

의존성 목록처럼 이미 값이 있는 키에 항목을 덧붙일 때는 `:=` 대신 `+=`(단일 추가)나 `++=`(복수 추가)를 쓴다.

```scala
val derby = "org.apache.derby" % "derby" % "10.4.1.3"

lazy val root = (project in file("."))
  .settings(
    libraryDependencies += derby
  )
```

`%` 메서드는 문자열로부터 Ivy 모듈 식별자를 구성한다.

## 12. ThisBuild와 빌드 전역 설정

여러 서브프로젝트에 공통으로 적용할 값은 `ThisBuild` 스코프에 지정한다.

```scala
ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.12.18"
ThisBuild / version := "0.1.0-SNAPSHOT"
```

`ThisBuild` 스코프에 지정한 값은 각 서브프로젝트가 별도로 재정의하지 않는 한 기본값으로 그대로 상속된다.

## 13. 멀티 프로젝트 기본 선언

프로젝트는 `Project` 타입의 `lazy val`로 선언하며, 변수명이 곧 서브프로젝트 ID가 된다.

```scala
lazy val util = (project in file("util"))

lazy val core = (project in file("core"))
```

베이스 디렉터리가 변수명과 같으면 `in file(...)` 부분을 생략할 수 있다.

```scala
lazy val util = project

lazy val core = project
```

명시적으로 루트 프로젝트를 정의하지 않으면, sbt가 나머지 모든 프로젝트를 집계하는 기본 루트 프로젝트를 자동으로 만들어 준다.

## 14. 공통 세팅 재사용

여러 프로젝트에서 반복되는 세팅은 `Seq[Setting[_]]`로 뽑아내 재사용할 수 있다.

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

## 15. Aggregation(집계)

집계 프로젝트에서 태스크를 실행하면 집계된 프로젝트들에서도 같은 태스크가 실행된다.

```scala
lazy val root = (project in file("."))
  .aggregate(util, core)

lazy val util = (project in file("util"))

lazy val core = (project in file("core"))
```

특정 태스크만 집계에서 제외하고 싶으면 해당 태스크의 `aggregate` 세팅을 끈다.

```scala
lazy val root = (project in file("."))
  .aggregate(util, core)
  .settings(
    update / aggregate := false
  )
```

## 16. dependsOn(클래스패스 의존성)

`dependsOn`은 컴파일 순서를 정하고 대상 프로젝트의 클래스패스를 포함시키는 클래스패스 의존성을 만든다.

```scala
lazy val core = project.dependsOn(util)
```

의존 대상이 여러 개면 나열한다.

```scala
lazy val core = project.dependsOn(bar, baz)
```

### 설정별 의존성

`%`와 `->` 표기로 어떤 설정(configuration)이 어떤 설정에 의존하는지 세밀하게 지정할 수 있다.

```scala
dependsOn(util % "compile->compile")
dependsOn(util % "test->compile")
dependsOn(util % "test->test;compile->compile")
```

대상 설정을 생략하면 기본값은 `compile`이다.

## 17. 설정별 의존성과 의존성 추적 제어

대규모 멀티 프로젝트 빌드에서 프로젝트 간 컴파일 트리거를 세밀하게 제어하고 싶을 때는 다음 세팅을 쓴다.

```scala
ThisBuild / trackInternalDependencies := TrackLevel.TrackIfMissing
ThisBuild / exportJars := true

lazy val root = (project in file("."))
  .aggregate(/* ... */)
```

특정 프로젝트만 이 추적에서 제외하고 싶으면 그 프로젝트에 개별로 지정한다.

```scala
lazy val dontTrackMe = (project in file("dontTrackMe"))
  .settings(
    exportToInternal := TrackLevel.NoTracking
  )
```

## 셸 명령: 프로젝트 탐색

멀티 프로젝트 빌드에서는 셸 안에서 다음 명령으로 프로젝트를 오간다.

| 명령 | 설명 |
|------|------|
| `projects` | 빌드에 정의된 전체 프로젝트 목록 표시 |
| `project <이름>` | 현재 프로젝트를 지정한 프로젝트로 전환 |
| `<서브프로젝트ID>/compile` | 현재 프로젝트를 바꾸지 않고 특정 프로젝트의 태스크만 실행 |

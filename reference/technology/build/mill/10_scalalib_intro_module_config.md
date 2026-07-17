# Mill scalalib: Scala 빌드 소개와 모듈 설정

> 원본: https://mill-build.org/mill/scalalib/intro.html, https://mill-build.org/mill/scalalib/module-config.html

---

## 목차

1. [Mill로 Scala 프로젝트 구성하기](#1-mill로-scala-프로젝트-구성하기)
2. [기본 단일 모듈 구조](#2-기본-단일-모듈-구조)
3. [다중 모듈 프로젝트와 moduleDeps](#3-다중-모듈-프로젝트와-moduledeps)
4. [테스트 모듈: ScalaTests](#4-테스트-모듈-scalatests)
5. [Cross 모듈: 다중 Scala 버전 빌드](#5-cross-모듈-다중-scala-버전-빌드)
6. [SBT 호환 모듈](#6-sbt-호환-모듈)
7. [자주 쓰는 명령어](#7-자주-쓰는-명령어)
8. [ScalaModule 핵심 설정 옵션](#8-scalamodule-핵심-설정-옵션)
9. [컴파일러 플러그인 설정](#9-컴파일러-플러그인-설정)
10. [소스·리소스 커스터마이징과 generatedSources](#10-소스리소스-커스터마이징과-generatedsources)
11. [ScalaDoc과 Unidoc](#11-scaladoc과-unidoc)
12. [REPL·콘솔·Ammonite](#12-repl콘솔ammonite)
13. [기타 고급 옵션](#13-기타-고급-옵션)

---

## 1. Mill로 Scala 프로젝트 구성하기

Mill은 Scala 프로젝트를 구성하는 방식을 세 가지 제공하며, 프로젝트 복잡도에 따라 선택하거나 섞어 쓸 수 있다.

| 방식 | 파일 | 용도 |
|------|------|------|
| 선언적(YAML) | `build.mill.yaml`, `package.mill.yaml` | 의존성·버전 설정만 필요한 단순한 모듈 |
| 단일 파일 스크립트 | `Foo.scala` (헤더에 `//\| ...`) | 스크립트 하나로 빌드 설정과 코드를 함께 관리 |
| 프로그래매틱(build.mill) | `build.mill` (Scala 코드) | trait 상속, 커스텀 task 등 복잡한 로직이 필요한 빌드 |

세 방식은 서로 완전히 배타적이지 않다. 선언적 모듈이 `build.mill`에 정의된 프로그래매틱 모듈에 `moduleDeps`로 의존할 수 있고, YAML의 `extends:` 항목에 `mill-build/src/`에 정의한 커스텀 Scala trait을 지정할 수도 있다.

---

## 2. 기본 단일 모듈 구조

### 2.1 YAML 방식

```yaml
# build.mill.yaml
extends: ScalaModule
scalaVersion: 3.8.2
mvnDeps:
- com.lihaoyi::scalatags:0.13.1
- com.lihaoyi::mainargs:0.7.8
```

### 2.2 build.mill (프로그래매틱) 방식

```scala
package build
import mill.*, scalalib.*

object foo extends ScalaModule {
  def scalaVersion = "3.8.2"
  def mvnDeps = Seq(
    mvn"com.lihaoyi::scalatags:0.13.1",
    mvn"com.lihaoyi::mainargs:0.7.8"
  )
}
```

### 2.3 디렉터리 구조 (Mill 기본 레이아웃)

```
build.mill.yaml   # 또는 build.mill
src/
  Foo.scala
resources/
  ...
test/
  package.mill.yaml
  src/
    FooTests.scala
out/
  compile.json
  compile.dest/
  test/
    compile.json
    compile.dest/
```

SBT의 `src/main/scala`, `src/test/scala`와 달리 Mill 기본 레이아웃은 소스를 `src/`, 테스트를 별도 하위 모듈인 `test/`에 둔다. `out/`은 모든 task의 캐시된 산출물이 저장되는 디렉터리로, 소스 변경이 없으면 재실행되지 않는다.

---

## 3. 다중 모듈 프로젝트와 moduleDeps

각 하위 디렉터리가 하나의 모듈이 되며, `moduleDeps`로 모듈 간 의존 관계를 선언한다.

```yaml
# foo/package.mill.yaml
extends: ScalaModule
moduleDeps: [bar]
scalaVersion: 3.8.2
mvnDeps:
- com.lihaoyi::mainargs:0.7.8
```

```yaml
# bar/package.mill.yaml
extends: ScalaModule
scalaVersion: 3.8.2
mvnDeps:
- com.lihaoyi::scalatags:0.13.1
```

build.mill 방식에서는 공통 설정을 trait으로 뽑아 재사용하는 패턴이 일반적이다.

```scala
package build
import mill.*, scalalib.*

trait MyModule extends ScalaModule {
  def scalaVersion = "3.8.2"
  object test extends ScalaTests {
    def mvnDeps = Seq(mvn"com.lihaoyi::utest:0.8.9")
    def testFramework = "utest.runner.Framework"
  }
}

object foo extends MyModule {
  def moduleDeps = Seq(bar)
  def mvnDeps = Seq(mvn"com.lihaoyi::mainargs:0.7.8")
}

object bar extends MyModule {
  def mvnDeps = Seq(mvn"com.lihaoyi::scalatags:0.13.1")
}
```

### 여러 task를 한 번에 지정하는 쿼리 문법

| 표기 | 의미 |
|------|------|
| `_` | 경로 한 세그먼트에 매칭되는 와일드카드 |
| `__` | 임의 깊이의 경로 세그먼트에 매칭 |
| `{a,b}` | 여러 task를 동시에 지정 |
| `+` | 여러 쿼리 결과를 합침 |

```bash
./mill bar._.compile        # bar의 직속 하위 모듈만 compile
./mill bar.__.test          # bar 하위 모든 모듈의 test
./mill {foo,bar}.compile    # foo와 bar의 compile
./mill __.compile + bar.__.test
```

---

## 4. 테스트 모듈: ScalaTests

테스트는 별도 하위 모듈(`ScalaTests` 상속)로 정의하고, `testFramework`로 사용할 테스트 프레임워크를 지정한다.

```yaml
# test/package.mill.yaml
extends: [build.ScalaTests, TestModule.Utest]
mvnDeps:
- com.lihaoyi::utest:0.9.1
```

```scala
object test extends ScalaTests {
  def mvnDeps = Seq(mvn"com.lihaoyi::utest:0.9.1")
  def testFramework = "utest.runner.Framework"
}
```

지원되는 테스트 프레임워크: JUnit4, JUnit5, TestNG, MUnit, ScalaTest, Specs2, Utest, Weaver, ZioTest.

---

## 5. Cross 모듈: 다중 Scala 버전 빌드

여러 Scala 버전을 동시에 빌드해야 할 때 `Cross`와 `CrossScalaModule`을 사용한다.

```scala
package build
import mill.*, scalalib.*

object foo extends Cross[FooModule]("2.13.16", "3.3.6")
trait FooModule extends CrossScalaModule {
  object test extends CrossScalaTests {
    def mvnDeps = Seq(mvn"com.lihaoyi::utest:0.9.1")
    def testFramework = "utest.runner.Framework"
  }
}
```

```bash
./mill foo.2_13_16.run
./mill foo.3_3_6.run
./mill __.test
```

### 버전별 소스 오버라이드 규칙

| 디렉터리 | 적용 범위 |
|----------|-----------|
| `src/` | 모든 크로스 버전 공통 |
| `src-2/` | Scala 2.x 전용 |
| `src-2-12/` | Scala 2.12.x 전용 |
| `src-2-12-20/` | Scala 2.12.20 정확히 일치할 때만 |

Cross 모듈 간 의존은 현재 크로스 값을 넘겨 참조한다.

```scala
object foo2 extends Cross[Foo2Module](scalaVersions)
trait Foo2Module extends CrossScalaModule {
  def moduleDeps = Seq(bar(crossScalaVersion))
}
```

선언적(YAML) 모듈에서 Cross 모듈을 `moduleDeps`로 참조할 때는 Mill의 task 쿼리 문법을 그대로 쓰되, YAML 배열 문법과 겹치지 않도록 따옴표로 감싼다.

```yaml
moduleDeps: [bar, "qux[1]"]
```

---

## 6. SBT 호환 모듈

기존 SBT 프로젝트의 표준 디렉터리 구조(`src/main/scala`, `src/test/scala`)를 그대로 유지하려면 `ScalaModule` 대신 `SbtModule`을 상속한다.

```yaml
extends: SbtModule
scalaVersion: 3.8.2
mvnDeps:
- com.lihaoyi::scalatags:0.13.1

object test:
  extends: [SbtTests, TestModule.Utest]
  mvnDeps:
  - com.lihaoyi::utest:0.9.1
```

Cross 버전 조합에는 `CrossSbtModule` / `CrossSbtTests`를 사용하며, 이 경우 `src/main/scala-2.12/`, `src/main/scala-2.13/`처럼 버전별 소스 디렉터리를 SBT 규칙대로 배치한다.

---

## 7. 자주 쓰는 명령어

| 명령 | 설명 |
|------|------|
| `./mill resolve _` | 사용 가능한 task 목록 조회 |
| `./mill inspect <task>` | task 정보(입력/설명) 조회 |
| `./mill compile` | 컴파일 |
| `./mill test` | 테스트 실행 |
| `./mill run -- <args>` | 프로그램 실행 |
| `./mill runBackground` | 백그라운드 실행 |
| `./mill assembly` | fat JAR 생성 |
| `./mill show <task>` | task 결과를 JSON으로 출력 |
| `./mill repl` / `./mill test.repl` | Scala REPL 시작 |
| `./mill clean <task>` | 캐시 삭제 |
| `./mill launcher` | 실행 가능한 launcher 스크립트 생성 |
| `./mill -w <task>` | 파일 변경 감시 후 자동 재실행 |
| `./mill visualizePlan <task>` | task 의존 그래프 시각화 |

---

## 8. ScalaModule 핵심 설정 옵션

### 8.1 YAML 선언적 설정

```yaml
scalaVersion: 3.7.4

mvnDeps:
  - com.lihaoyi::scalatags:0.13.1
  - com.lihaoyi::os-lib:0.11.4

repositories: [https://oss.sonatype.org/content/repositories/releases]

mainClass: foo.Foo2   # 메인 클래스가 여럿이거나 의존 라이브러리에서 올 때 명시

jvmVersion: 11
# 또는 배포판까지 지정
jvmVersion: temurin:11

sources: !append [custom-src/]
resources: !append [custom-resources/]

scalacOptions: [-deprecation, -Xfatal-warnings]

forkArgs: [-Dmy.custom.property=my-prop-value]
forkEnv: { MY_CUSTOM_ENV: my-env-value }
```

### 8.2 build.mill 프로그래매틱 설정

```scala
object `package` extends ScalaModule {
  def scalaVersion = "3.8.2"

  def mvnDeps = Seq(
    mvn"com.lihaoyi::scalatags:0.13.1",
    mvn"com.lihaoyi::os-lib:0.11.4"
  )

  def mainClass: T[Option[String]] = Some("foo.Foo2")

  def scalacOptions: T[Seq[String]] = Seq("-deprecation", "-Werror")
  def forkArgs: T[Seq[String]] = Seq("-Dmy.custom.property=my-prop-value")
  def forkEnv: T[Map[String, String]] = Map("MY_CUSTOM_ENV" -> "my-env-value")
}
```

### 8.3 주요 옵션 정리

| 옵션 | 역할 |
|------|------|
| `scalaVersion` | 사용할 Scala 버전 |
| `mvnDeps` | Maven 좌표 기반 라이브러리 의존성 (`groupId::artifactId:version` 형식은 Scala 버전이 자동 붙는 표기) |
| `repositories` | 추가 아티팩트 저장소 |
| `mainClass` | `run`/`assembly` 시 사용할 진입점 클래스 |
| `jvmVersion` | 컴파일·실행에 쓸 JDK 버전(배포판 지정 가능) |
| `sources` / `resources` | 소스·리소스 디렉터리 목록 커스터마이즈 (`!append`로 기본값에 추가) |
| `scalacOptions` | scalac에 전달할 컴파일 옵션 |
| `forkArgs` / `forkEnv` | `run`/`test`를 별도 JVM 프로세스로 fork할 때 전달할 JVM 인자·환경변수 |
| `moduleDeps` | 다른 모듈에 대한 의존 |
| `zincIncrementalCompilation` | Zinc 증분 컴파일 사용 여부 |

`sources`/`resources`의 경로는 상대 경로면 해당 모듈의 `moduleDir` 기준, `//src/`처럼 슬래시 두 개로 시작하면 워크스페이스 루트 기준으로 해석된다.

---

## 9. 컴파일러 플러그인 설정

Scala 컴파일러 플러그인은 특정 Scala **전체 버전**(예: `2.13.16`)에 맞춰 발행되므로 일반 `::` 대신 `:::` 구분자를 사용해 아티팩트를 지정한다.

```yaml
extends: ScalaModule
scalaVersion: 2.13.16

compileMvnDeps: [com.lihaoyi:::acyclic:0.3.18]
scalacOptions: [-P:acyclic:force]
scalacPluginMvnDeps: [com.lihaoyi:::acyclic:0.3.18]
```

- `compileMvnDeps`: 컴파일 시점에만 필요한 의존성(런타임 클래스패스에는 포함되지 않음)
- `scalacPluginMvnDeps`: 컴파일러 플러그인 자체의 의존성

---

## 10. 소스·리소스 커스터마이징과 generatedSources

기본 소스/리소스 목록에 디렉터리를 추가하려면 `super`를 호출해 이어붙인다.

```scala
def customSources = Task.Sources("custom-src")
def sources = Task { super.sources() ++ customSources() }

def customResources = Task.Sources("custom-resources")
def resources = Task { super.resources() ++ customResources() }
```

빌드 시점에 소스 코드를 생성해야 하면 `generatedSources`를 오버라이드한다. 결과는 `Task.dest`(해당 task 전용 출력 디렉터리)에 파일을 쓰고 `PathRef`로 감싸 반환한다.

```scala
object foo extends ScalaModule {
  def scalaVersion = "3.8.2"

  def generatedSources: T[Seq[PathRef]] = Task {
    os.write(
      Task.dest / "Foo.scala",
      """package foo
        |object Foo {
        |  def main(args: Array[String]): Unit = println("Hello World")
        |}
      """.stripMargin
    )
    Seq(PathRef(Task.dest))
  }
}
```

리소스에 값을 주입하는 것도 같은 패턴이다.

```scala
def lineCount = Task {
  allSourceFiles().map(f => os.read.lines(f.path).size).sum
}

override def resources = Task {
  os.write(Task.dest / "line-count.txt", "" + lineCount())
  super.resources() ++ Seq(PathRef(Task.dest))
}
```

### 클래스패스 리소스 vs 리소스 폴더

| 구분 | 특징 |
|------|------|
| 클래스패스 리소스 (`os.resource`) | 개별 파일만 조회 가능, `assembly` JAR에 함께 번들 |
| 리소스 폴더 (`MILL_TEST_RESOURCE_DIR`) | 폴더 나열·파일시스템 조작 가능, Mill 내부 실행 시에만 유효(배포 아티팩트에는 미포함) |

테스트에서 클래스패스에 없는 별도 파일에 접근해야 하면 `forkEnv`로 경로를 명시적으로 넘긴다.

```scala
object test extends ScalaTests {
  def otherFiles = Task.Source("other-files")
  def forkEnv = super.forkEnv() ++ Map(
    "OTHER_FILES_DIR" -> otherFiles().path.toString
  )
}
```

---

## 11. ScalaDoc과 Unidoc

### 11.1 ScalaDoc 옵션

```yaml
extends: ScalaModule
scalaVersion: 3.3.6

scalaDocOptions: [-siteroot, mydocs, -no-link-warnings]
```

관련 task: `docJar`(문서를 JAR로 패키징), `scalaDocGenerated`(패키징 없이 디렉터리 형태). Scala 3에서는 `docResources()`를 정적 사이트의 루트로 취급하며 그 안의 `_docs/`, `_blog/` 디렉터리를 탐색한다. 소스 코드 링크를 문서에 붙이려면 다음처럼 지정한다.

```scala
def scalaDocOptions = Seq(
  s"-source-links:${mill.api.BuildCtx.workspaceRoot}=github://owner/repo/main"
)
```

### 11.2 Unidoc (다중 모듈 통합 문서)

여러 모듈의 문서를 하나로 합치려면 `UnidocModule`을 추가로 상속한다.

```scala
object foo extends ScalaModule, UnidocModule {
  def scalaVersion = "3.8.2"
  def moduleDeps = Seq(bar, qux)

  object bar extends ScalaModule { def scalaVersion = "3.8.2" }
  object qux extends ScalaModule {
    def scalaVersion = "3.8.2"
    def moduleDeps = Seq(bar)
  }

  def unidocDocumentTitle = Task { "foo docs" }
  def unidocVersion = Some("0.1.0")
  def unidocSourceUrl = Some("https://github.com/lihaoyi/test/blob/master")
}
```

- `unidocLocal`: 로컬 브라우징용 산출물
- `unidocSite`: 외부 링크가 포함된 배포용 사이트

---

## 12. REPL·콘솔·Ammonite

`console` task에서만 별도로 적용할 scalac 옵션을 지정할 수 있다(예: `-Xfatal-warnings` 제외).

```scala
object foo extends ScalaModule {
  def consoleScalacOptions = scalacOptions().filterNot(_ == "-Xfatal-warnings")
}
```

Ammonite REPL을 쓰려면 버전을 명시한다. Ammonite는 Scala 컴파일러 내부 API에 의존하고 컴파일러가 이진 호환성을 보장하지 않으므로 반드시 정확한 버전을 맞춰야 한다.

```scala
object foo extends ScalaModule {
  def scalaVersion = "2.12.6"
  def ammoniteVersion = "2.4.0"
}
```

---

## 13. 기타 고급 옵션

### 13.1 Zinc 증분 컴파일 비활성화

기본적으로 Mill은 변경된 소스만 재컴파일하는 Zinc 증분 컴파일을 사용한다. 필요하면 끌 수 있다.

```scala
object foo extends ScalaModule {
  def zincIncrementalCompilation = false
}
```

### 13.2 Scala 나이틀리 빌드 사용

나이틀리 저장소를 `JvmWorkerModule`과 모듈 양쪽에 추가해야 한다.

```scala
object JvmWorker extends JvmWorkerModule {
  override def repositories =
    super.repositories() ++ Seq(CoursierModule.KnownRepositories.ScalaLangNightlies)
}

object foo extends ScalaModule {
  override def jvmWorker: ModuleRef[JvmWorkerModule] = ModuleRef(JvmWorker)
  override def repositories =
    super.repositories() ++ Seq(CoursierModule.KnownRepositories.ScalaLangNightlies)
  override def scalaVersion = "3.8.0-RC1-bin-20250825-ee2f641-NIGHTLY"
  override def mvnDeps = Seq(mvn"org.scala-lang.modules::scala-xml:2.4.0")
}
```

### 13.3 선언적 모듈에서 커스텀 trait 재사용

`mill-build/src/`에 정의한 Scala trait을 YAML의 `extends:`에서 참조할 수 있다.

```scala
// mill-build/src/LineCountScalaModule.scala
package millbuild
import mill.*, scalalib.*

trait LineCountScalaModule extends mill.scalalib.ScalaModule {
  def lineCount = Task {
    allSourceFiles().map(f => os.read.lines(f.path).size).sum
  }
  override def resources = Task {
    os.write(Task.dest / "line-count.txt", "" + lineCount())
    super.resources() ++ Seq(PathRef(Task.dest))
  }
}
```

```yaml
# build.mill.yaml
extends: millbuild.LineCountScalaModule
scalaVersion: 3.8.2
```

### 13.4 핵심 개념 요약

| 개념 | 의미 |
|------|------|
| `moduleDir` | 모듈의 기준 경로. 루트 모듈은 저장소 루트, 하위 모듈은 해당 디렉터리 |
| `Task.dest` | 각 task 전용 출력 디렉터리. 임시 계산·생성 파일 저장에 사용 |
| `PathRef` | 파일/폴더의 내용을 참조값으로 감싸, 내용이 바뀌면 하위 task 캐시를 무효화 |

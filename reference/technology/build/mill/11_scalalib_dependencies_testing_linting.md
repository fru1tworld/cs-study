# Mill scalalib: 의존성, 테스트, 린팅

> 원본: https://mill-build.org/mill/scalalib/dependencies.html, https://mill-build.org/mill/scalalib/testing.html, https://mill-build.org/mill/scalalib/linting.html

---

## 목차

1. [의존성 선언 기본](#1-의존성-선언-기본)
2. [컴파일 타임 vs 런타임 의존성](#2-컴파일-타임-vs-런타임-의존성)
3. [저장소(repositories) 설정](#3-저장소repositories-설정)
4. [관리되지 않는 JAR (unmanagedClasspath)](#4-관리되지-않는-jar-unmanagedclasspath)
5. [Task 기반 프로그래머블 의존성](#5-task-기반-프로그래머블-의존성)
6. [Scala 의존성 표기법](#6-scala-의존성-표기법)
7. [Scala 3 상호운용성](#7-scala-3-상호운용성)
8. [ScalaJS / Scala Native 의존성](#8-scalajs--scala-native-의존성)
9. [의존성 최신화 (Scala Steward)](#9-의존성-최신화-scala-steward)
10. [테스트 모듈 기본 구성](#10-테스트-모듈-기본-구성)
11. [테스트 슈트/클래스/케이스 선택 실행](#11-테스트-슈트클래스케이스-선택-실행)
12. [지원 테스트 프레임워크](#12-지원-테스트-프레임워크)
13. [테스트 실행 모드: testForked / testCached / testLocal](#13-테스트-실행-모드-testforked--testcached--testlocal)
14. [테스트 간 의존성](#14-테스트-간-의존성)
15. [테스트 병렬화 (testParallelism)](#15-테스트-병렬화-testparallelism)
16. [테스트 그룹화 (testForkGrouping)](#16-테스트-그룹화-testforkgrouping)
17. [scalafmt 자동 포매팅](#17-scalafmt-자동-포매팅)
18. [scalafix 린팅/자동 수정](#18-scalafix-린팅자동-수정)
19. [acyclic: 순환 의존성 검출](#19-acyclic-순환-의존성-검출)
20. [Spotless 통합 포매팅 & Ratchet](#20-spotless-통합-포매팅--ratchet)
21. [Scoverage 코드 커버리지](#21-scoverage-코드-커버리지)
22. [MiMa 바이너리 호환성](#22-mima-바이너리-호환성)

---

## 1. 의존성 선언 기본

`ScalaModule`은 의존성을 `mvnDeps`(일반 의존성), `compileMvnDeps`(컴파일 전용), `runMvnDeps`(런타임 전용)로 구분해 선언한다. Mill 설정은 `.mill.yaml` 형태로도, `build.mill` Scala DSL로도 작성 가능하다.

```yaml
extends: ScalaModule
scalaVersion: 3.8.2
compileMvnDeps:
- javax.servlet:servlet-api:2.5
runMvnDeps:
- org.eclipse.jetty:jetty-server:9.4.42.v20210604
```

- `compileMvnDeps`: 컴파일 타임에만 필요하고 런타임에는 제외되는 의존성(예: 서블릿 API처럼 컨테이너가 제공하는 API).
- `runMvnDeps`: 런타임에만 필요한 의존성(리플렉션이나 classpath scanning으로 로드되는 구현체 등). `compile` 태스크의 classpath에는 포함되지 않고 `run`/`assembly` 시점에 추가된다.

## 2. 컴파일 타임 vs 런타임 의존성

| 구분 | 전이성(transitive) | 특징 |
|------|---------------------|------|
| `runMvnDeps` | 전이적 | `run`/`assembly` 생성 시 업스트림 모듈의 런타임 의존성까지 함께 포함 |
| `compileMvnDeps` | 비전이적 | 업스트림 의존성에서 자동으로 집계되지 않으므로, 필요한 각 모듈에서 직접 선언하거나 공통 trait을 상속해 전달해야 함 |

`compileMvnDeps`로 선언한 의존성은 Maven/Ivy로 퍼블리시할 때 `provided` scope로 변환된다.

## 3. 저장소(repositories) 설정

기본적으로 Mill은 Coursier를 통해 Maven Central에서 의존성을 해석한다. 추가 저장소가 필요하면 `repositories`를 선언한다.

```scala
def repositories = Seq(
  "https://oss.sonatype.org/content/repositories/releases"
)
```

Scala 컴파일러 나이틀리 빌드 등 특수 저장소도 동일하게 추가한다.

```yaml
repositories:
- https://repo.scala-lang.org/artifactory/maven-nightlies/
```

Coursier 미러(mirror) 설정 파일 위치는 OS별로 다르다.

| OS | 경로 |
|----|------|
| Linux | `~/.config/coursier/mirror.properties` |
| macOS | `~/Library/Preferences/Coursier/mirror.properties` |
| Windows | `C:\Users\<user_name>\AppData\Roaming\Coursier\config\mirror.properties` |

## 4. 관리되지 않는 JAR (unmanagedClasspath)

파일시스템에 이미 존재하는 JAR을 직접 classpath에 넣고 싶을 때 `unmanagedClasspath`를 사용한다. 다만 대부분의 경우 `mvnDeps`/`moduleDeps`로 해결 가능하므로 `unmanagedClasspath`는 좌표가 없는 사설 JAR 등 극히 드문 상황에만 쓴다.

```yaml
unmanagedClasspath:
- lib/nanojson-1.8.jar
```

## 5. Task 기반 프로그래머블 의존성

`unmanagedClasspath`를 Task로 정의하면 빌드 시점에 JAR을 동적으로 다운로드해 사용할 수 있다.

```scala
def unmanagedClasspath = Task {
  if (Task.offline) Task.fail("Cannot download...")
  else {
    os.write(
      Task.dest / "fastjavaio.jar",
      requests.get.stream("https://github.com/.../fastjavaio.jar")
    )
    Seq(PathRef(Task.dest / "fastjavaio.jar"))
  }
}
```

Task는 캐시되므로 JAR은 최초 1회만 다운로드되고 이후 빌드에서는 캐시된 결과가 재사용된다. `Task.offline`으로 오프라인 모드를 감지해 명시적으로 실패시킬 수 있다.

## 6. Scala 의존성 표기법

Scala는 2.13까지 major 버전 간 이진 호환을 보장하지 않기 때문에, 배포되는 아티팩트 이름에 Scala major 버전이 접미사로 붙는다(예: `foo_2.13`). Mill은 이를 콜론 개수로 구분해 선언한다.

```scala
// 단일 콜론: Scala 버전을 직접 명시 (비권장)
mvn"com.typesafe.akka:akka-actor_2.12:2.5.25"

// 더블 콜론: Mill이 현재 scalaVersion의 major 버전을 자동으로 붙임 (권장)
mvn"com.typesafe.akka::akka-actor:2.5.25"

// 트리플 콜론: Scala full 버전(예: 컴파일러 플러그인)까지 붙임
mvn"org.scalamacros:::paradise:2.1.1"
```

## 7. Scala 3 상호운용성

Scala 3는 Scala 2.13과 같은 표준 라이브러리를 공유하므로 Scala 2.13용 라이브러리를 Scala 3 프로젝트에서 그대로 쓸 수 있다. 다만 아티팩트 이름 매핑을 위해 `withDottyCompat`을 호출해야 한다.

```scala
def scalaVersion = "3.2.1"
def mvnDeps = Seq(
  mvn"com.lihaoyi::upickle:2.0.0".withDottyCompat(scalaVersion())
)
```

## 8. ScalaJS / Scala Native 의존성

`::` 표기법은 ScalaJS 의존성 선언에도 그대로 쓰인다. 대상 모듈이 각각 `ScalaJSModule` 또는 `ScalaNativeModule`을 상속해야 플랫폼별 아티팩트 접미사(`_sjs1_2.13` 등)가 올바르게 해석된다.

## 9. 의존성 최신화 (Scala Steward)

GitHub, GitLab, Bitbucket에 호스팅된 프로젝트는 Scala Steward를 연동해 `mvnDeps`에 선언된 라이브러리 버전을 자동으로 최신화하는 PR을 받을 수 있다.

---

## 10. 테스트 모듈 기본 구성

Mill에는 별도의 "test scope" 개념이 없다. 테스트 모듈은 그냥 하나의 일반 모듈이며, `ScalaTests` trait과 원하는 `TestModule.*` 프레임워크 trait을 함께 상속해서 만든다.

```yaml
# build.mill.yaml
extends: ScalaModule
scalaVersion: 3.8.2
```

```yaml
# test/package.mill.yaml
extends: [build.ScalaTests, TestModule.Utest]
mvnDeps: [com.lihaoyi::utest:0.9.1]
```

멀티 테스트 슈트도 동일한 패턴으로 만들 수 있다. 예를 들어 `integration` 모듈이 `test` 모듈의 유틸리티를 재사용하려면 `moduleDeps`로 연결한다.

```yaml
# integration/package.mill.yaml
extends: [build.ScalaTests, TestModule.Utest]
moduleDeps: !append [test]
mvnDeps: [com.lihaoyi::utest:0.9.1]
```

이 경우 의존 관계는 `root → test → integration` 형태가 되어 `integration`에서 `test/src`의 코드를 그대로 가져다 쓸 수 있다.

## 11. 테스트 슈트/클래스/케이스 선택 실행

```bash
./mill '{test,integration}'     # test, integration 두 슈트만 실행
./mill __.integration           # 모든 모듈의 integration 테스트
./mill __.test                  # 모든 모듈의 일반 테스트
./mill __.testForked            # 테스트 종류와 무관하게 모두 실행
```

특정 클래스나 케이스만 고를 때는 프레임워크별 선택자 문법을 그대로 인자로 넘긴다(uTest 예시).

```bash
./mill test qux.QuxTests          # 클래스 단위
./mill test qux.QuxTests.hello    # 케이스 단위
```

## 12. 지원 테스트 프레임워크

| 프레임워크 | TestModule trait | 비고 |
|-----------|-------------------|------|
| uTest | `TestModule.Utest` | `com.lihaoyi::utest` |
| JUnit 4 | `TestModule.Junit4` | sbt/junit-interface |
| JUnit 5 | `TestModule.Junit5` | sbt/sbt-jupiter-interface |
| TestNG | `TestModule.TestNg` | |
| MUnit | `TestModule.Munit` | |
| ScalaTest | `TestModule.ScalaTest` | |
| Specs2 | `TestModule.Specs2` | |
| Weaver | `TestModule.Weaver` | weaver-test |
| ZIO Test | `TestModule.ZioTest` | zio.dev |

`testFramework`를 직접 문자열로 지정할 수도 있고(`TestModule.*` trait 없이), trait을 상속하면 기본값이 채워진다.

```yaml
# testFramework를 직접 지정하는 방식
extends: [build.foo.ScalaTests]
testFramework: utest.runner.Framework
mvnDeps: [com.lihaoyi::utest:0.9.1]
```

```yaml
# TestModule.Utest trait을 상속하는 방식(더 간결)
extends: [build.bar.ScalaTests, TestModule.Utest]
mvnDeps: [com.lihaoyi::utest:0.8.9]
```

테스트 코드 예시(uTest):

```scala
// foo/src/Foo.scala
package foo
object Foo {
  def hello(): String = "Hello World"
}
```

```scala
// foo/test/src/FooTests.scala
package foo
import utest.*
object FooTests extends TestSuite {
  def tests = Tests {
    test("hello") {
      val result = Foo.hello()
      assert(result.startsWith("Hello"))
      result
    }
    test("world") {
      val result = Foo.hello()
      assert(result.endsWith("World"))
      result
    }
  }
}
```

```bash
./mill foo.compile                     # 소스만 컴파일
./mill foo.test.compile                # 테스트 소스 컴파일
./mill foo.test.testForked             # 포크된 JVM에서 실행
./mill foo.test                        # testForked의 기본 별칭
./mill foo.test.testOnly foo.FooTests  # 특정 클래스만
./mill __.test                         # 모든 테스트 슈트
```

## 13. 테스트 실행 모드: testForked / testCached / testLocal

| 모드 | 실행 방식 | 특징 |
|------|-----------|------|
| `testForked`(기본) | 비어있는 `sandbox/` 디렉터리에서 subprocess로 실행 | 격리·재현성이 높지만 JVM 기동 오버헤드 존재 |
| `testCached` | `testForked`와 동일하게 실행하되 성공 결과를 캐시 | 업스트림 변경이 없으면 재실행을 건너뜀. 다운스트림 테스트에 유용 |
| `testLocal` | 격리된 classloader로 Mill 메인 프로세스 안에서 실행 | 더 빠르지만 유연성이 낮고 프로세스 간섭 위험 있음 |

```bash
./mill foo.test               # testForked
./mill foo.test.testCached
./mill foo.test.testLocal
./mill --watch foo.test       # 파일 변경 시 자동 재실행
```

테스트는 기본적으로 비어있는 `sandbox/` 폴더를 작업 디렉터리로 실행되며, 리소스 파일에 접근하려면 `MILL_TEST_RESOURCE_DIR` 환경 변수를 사용한다. 특정 클래스/케이스만 지정하려면 인자를 그대로 이어서 전달한다.

```bash
./mill foo.test foo.FooMoreTests
./mill bar.test bar.BarTests.hello
```

## 14. 테스트 간 의존성

Mill에는 test-scoped dependency가 없다. 테스트 모듈도 일반 모듈이므로 `mvnDeps`, `runMvnDeps`, `moduleDeps`를 그대로 사용해 다른 테스트 모듈에 의존할 수 있다.

```yaml
# baz/package.mill.yaml
extends: ScalaModule
scalaVersion: 3.8.2
```

```yaml
# baz/test/package.mill.yaml
extends: [build.baz.ScalaTests]
testFramework: utest.runner.Framework
mvnDeps: [com.lihaoyi::utest:0.8.9]
```

```yaml
# qux/package.mill.yaml
extends: ScalaModule
scalaVersion: 3.8.2
moduleDeps: [baz]
```

```yaml
# qux/test/package.mill.yaml
extends: [build.qux.ScalaTests]
testFramework: utest.runner.Framework
mvnDeps: [com.lihaoyi::utest:0.8.9]
moduleDeps: [qux, baz.test]   # baz.test의 테스트 유틸을 재사용
```

의존 그래프는 `baz → baz.test`, `qux → baz`, `qux.test → baz.test`가 되어 `qux.test`가 `baz.test`에 있는 공용 테스트 유틸을 그대로 가져다 쓸 수 있다.

## 15. 테스트 병렬화 (testParallelism)

```scala
package build
import mill.*, scalalib.*

object foo extends ScalaModule {
  def scalaVersion = "3.8.2"
  object test extends ScalaTests, TestModule.Utest {
    def utestVersion = "0.8.9"
    def testParallelism = true  // 기본값
  }
}
```

```bash
./mill --jobs 2 foo.test
```

`--jobs`로 지정한 개수만큼 테스트 클래스를 여러 JVM subprocess에 자동으로 분산 실행한다. 실행 결과는 워커별로 분리되어 기록된다.

```
out/foo/test/testForked.dest/
├── worker-0.log
├── worker-0/
├── worker-1.log
├── worker-1/
└── test-report.xml
```

`def testParallelism = false`로 끄면 단일 프로세스에서 순차 실행된다.

## 16. 테스트 그룹화 (testForkGrouping)

`testForkGrouping`으로 테스트 클래스를 그룹으로 나누어 각 그룹을 별도 JVM에서 실행할 수 있다. 느리거나 부작용이 있는 테스트를 격리하거나, 그룹별 로그를 따로 관리할 때 유용하다.

```scala
object foo extends ScalaModule {
  def scalaVersion = "3.8.2"
  object test extends ScalaTests, TestModule.Utest {
    def utestVersion = "0.8.9"
    def testForkGrouping = discoveredTestClasses()
      .grouped(1).toSeq   // 클래스 하나당 그룹 하나
    def testParallelism = false
  }
}
```

```
out/foo/test/testForked.dest/
├── foo.HelloTests/
│   └── sandbox/
├── foo.WorldTests/
│   └── sandbox/
└── test-report.xml
```

이름 패턴으로 그룹을 나누고 병렬화도 함께 켤 수 있다.

```scala
object test extends ScalaTests, TestModule.Utest {
  def utestVersion = "0.8.9"

  // 클래스 이름에 "GroupX"가 포함되는지로 두 그룹으로 분리
  def testForkGrouping =
    discoveredTestClasses()
      .groupMapReduce(_.contains("GroupX"))(Seq(_))(_ ++ _)
      .toSeq
      .sortBy(data => !data._1)
      .map(_._2)

  def testParallelism = true
}
```

```bash
./mill --jobs 2 foo.test
```

```
out/foo/test/testForked.dest/
├── group-0-foo.GroupX1/
│   ├── worker-.../
│   └── test-classes/
├── group-1-foo.GroupY1/
│   ├── worker-.../
│   └── test-classes/
└── test-report.xml
```

Mill은 각 subprocess가 GroupX 또는 GroupY 중 한 그룹의 테스트만 독점적으로 실행하도록 보장하여 그룹 간 테스트가 섞이지 않게 한다.

---

## 17. scalafmt 자동 포매팅

프로젝트 루트에 `.scalafmt.conf`를 두면 별도 플러그인 없이 scalafmt를 바로 쓸 수 있다.

```
version = "3.7.15"
runner.dialect = scala213
```

```bash
./mill mill.scalalib.scalafmt/                        # 전체 재포매팅
./mill mill.scalalib.scalafmt/ '{foo,bar}.sources'     # 특정 모듈만
./mill mill.scalalib.scalafmt/checkFormatAll           # 포맷 위반만 확인(수정 안 함)
```

`.scalafmt.conf`에 `maxColumn` 같은 옵션을 추가하면 그 기준으로 재포매팅된다.

```
maxColumn: 50
```

## 18. scalafix 린팅/자동 수정

scalafix는 서드파티 `mill-scalafix` 플러그인을 통해 연동한다.

```yaml
mill-build:
  mvnDeps: [com.goyeau::mill-scalafix_mill1:0.6.0]
extends: [ScalaModule, com.goyeau.mill.scalafix.ScalafixModule]
scalaVersion: 3.8.2
scalacOptions: [-Wunused:all]
```

`.scalafix.conf`에 적용할 규칙을 선언한다.

```
rules = [
    RemoveUnused,
]
OrganizeImports.targetDialect = Scala3
```

```bash
./mill __.fix    # ScalafixModule을 상속한 모든 모듈에 대해 실행
```

## 19. acyclic: 순환 의존성 검출

acyclic은 모듈 내부 파일 간 순환 의존성을 감지하는 Scala 컴파일러 플러그인이다.

```yaml
extends: ScalaModule
scalaVersion: 2.13.16
compileMvnDeps: [com.lihaoyi:::acyclic:0.3.18]
scalacPluginMvnDeps: [com.lihaoyi:::acyclic:0.3.18]
scalacOptions: [-P:acyclic:force]
```

```scala
// Foo.scala
package foo
object Foo {
  val value = 123
  def main(args: Array[String]): Unit = {
    println("hello " + Bar)
  }
}
```

```scala
// Bar.scala (Foo를 다시 참조 -> 순환)
package foo
object Bar {
  val value = Foo + " world"
}
```

컴파일 시 다음과 같이 실패한다.

```
error: Unwanted cyclic dependency
```

해결 방법은 한쪽 방향으로만 의존하도록 코드를 리팩터링하는 것이다.

## 20. Spotless 통합 포매팅 & Ratchet

여러 언어/파일 그룹마다 서로 다른 포매팅 규칙이 필요할 때는 Spotless로 하나의 명령에 통합할 수 있다.

```scala
import mill.Cross
import mill.scalalib.CrossScalaModule
import mill.javalib.spotless.SpotlessModule

object `package` extends SpotlessModule {
  object lib extends Cross[LibModule]("2.13.16", "3.7.0")
  trait LibModule extends CrossScalaModule
}
```

`.spotless-formats.json`에서 파일 패턴별 포매팅 스텝을 지정한다.

```json
[
  {
    "includes": ["glob:**.java"],
    "steps": [{"$type": "PalantirJavaFormat"}]
  },
  {
    "includes": ["glob:**.scala"],
    "excludes": ["glob:**/src-3**"],
    "steps": [
      { "$type": "ScalaFmt", "version": "3.8.5" }
    ]
  },
  {
    "includes": ["glob:**/src-3**", "glob:build.mill"],
    "steps": [
      { "$type": "ScalaFmt", "version": "3.8.5", "configFile": ".scalafmt3.conf" }
    ]
  }
]
```

Scala 버전별로 별도의 `.scalafmt.conf` / `.scalafmt3.conf`를 둔다.

```
# .scalafmt.conf (Scala 2.13)
version = "3.8.5"
runner.dialect = scala213
```

```
# .scalafmt3.conf (Scala 3)
version = "3.8.5"
runner.dialect = scala3
```

```bash
./mill spotless --check    # 포맷 위반 확인
./mill spotless            # 자동 포매팅(증분 적용)
./mill mill.javalib.spotless.SpotlessModule/ --check   # 전역 실행
```

**Ratchet**은 Git 트리 간 차이가 있는 파일에 대해서만 점진적으로 포매팅을 적용하는 기능이다. 레거시 코드베이스에 새 포맷 규칙을 한 번에 적용하기 어려울 때, 변경된 파일부터 순차적으로 규칙을 도입할 수 있다.

```bash
git init . -b main
git add .gitignore build.mill.yaml src/A.java
git commit -a -m "1"

echo " module hello {}" > src/module-info.java   # 포맷 오류가 있는 새 파일

./mill spotless --check
# format errors in src/A.java
# format errors in src/module-info.java

./mill ratchet --check     # HEAD 이후 변경된 파일만 검사
# ratchet found changes in 1 files
# format errors in src/module-info.java

./mill ratchet             # 변경분만 자동 수정
# ratchet found changes in 1 files
# formatting src/module-info.java
# formatted 1 files

git add src/module-info.java
git commit -a -m "2"

./mill ratchet --check HEAD^ HEAD   # 특정 커밋 구간 비교
```

전역 실행: `./mill mill.javalib.spotless.SpotlessModule/ratchet --check HEAD^ HEAD`

## 21. Scoverage 코드 커버리지

`mill-contrib-scoverage`를 통해 statement/branch 커버리지를 수집하고 최소 기준을 강제할 수 있다.

```yaml
mill-build:
  mvnDeps:
  - com.lihaoyi::mill-contrib-scoverage:$MILL_VERSION

extends: mill.contrib.scoverage.ScoverageModule
scoverageVersion: 2.1.0
scalaVersion: 3.8.2
mvnDeps:
- com.lihaoyi::scalatags:0.13.1
- com.lihaoyi::mainargs:0.7.8

statementCoverageMin: 10.00
branchCoverageMin: 90.00
```

```yaml
# test/package.mill.yaml
extends: build.ScoverageTests
mvnDeps:
- com.lihaoyi::utest:0.9.1

testFramework: utest.runner.Framework
```

```bash
./mill test                              # 테스트 실행과 함께 커버리지 수집
./mill resolve scoverage._                # 사용 가능한 커버리지 태스크 조회
./mill scoverage.validateCoverageMinimums # 최소 기준 검증
./mill scoverage.consoleReport            # 콘솔 리포트 출력
```

최소 기준을 만족하지 못하면 다음과 같이 실패한다.

```
error: This project's statement coverage (40.00) did not meet minimum (50.0)
```

## 22. MiMa 바이너리 호환성

라이브러리를 배포하는 프로젝트에서 이전 버전과의 바이너리 호환성을 검사하려면 Lightbend Migration Manager(MiMa)를 `mill-mima` 플러그인으로 연동한다. 자세한 설정은 https://github.com/lolgab/mill-mima 참고.

---

## 요약: 명령어 모음

| 도구 | 명령어 | 기능 |
|------|--------|------|
| 테스트 | `./mill foo.test` | testForked로 테스트 실행(기본) |
| 테스트 | `./mill foo.test.testCached` | 캐시된 성공 결과 재사용 |
| 테스트 | `./mill foo.test.testLocal` | 메인 프로세스 내 실행(빠름, 격리 낮음) |
| 테스트 | `./mill foo.test.testOnly <class>` | 특정 클래스만 실행 |
| scalafmt | `./mill mill.scalalib.scalafmt/` | 전체 포매팅 |
| scalafmt | `./mill mill.scalalib.scalafmt/checkFormatAll` | 포맷 확인만 |
| scalafix | `./mill __.fix` | 전체 모듈 자동 수정 |
| Spotless | `./mill spotless` / `--check` | 다국어 통합 포매팅/확인 |
| Ratchet | `./mill ratchet` / `--check` | 변경분만 점진적 포매팅 |
| Scoverage | `./mill scoverage.validateCoverageMinimums` | 커버리지 기준 검증 |

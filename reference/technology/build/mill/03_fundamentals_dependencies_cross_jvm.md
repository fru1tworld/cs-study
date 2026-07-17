# Mill 라이브러리 의존성, Cross Build, 번들 라이브러리, JVM 버전 설정

> 원본: https://mill-build.org/mill/fundamentals/library-deps.html, https://mill-build.org/mill/fundamentals/cross-builds.html, https://mill-build.org/mill/fundamentals/bundled-libraries.html, https://mill-build.org/mill/fundamentals/configuring-jvm-versions.html

---

## 목차

1. [라이브러리 의존성 기본](#1-라이브러리-의존성-기본)
2. [의존성 종류: mvnDeps / compileMvnDeps / runMvnDeps](#2-의존성-종류-mvndeps--compilemvndeps--runmvndeps)
3. [전이 의존성 확인](#3-전이-의존성-확인)
4. [전이 의존성 제외와 버전 강제](#4-전이-의존성-제외와-버전-강제)
5. [의존성 관리(BOM)](#5-의존성-관리bom)
6. [의존성 업데이트 검색](#6-의존성-업데이트-검색)
7. [Cross Build 기본](#7-cross-build-기본)
8. [Cross 모듈 참조와 다중 축](#8-cross-모듈-참조와-다중-축)
9. [동적 Cross 모듈](#9-동적-cross-모듈)
10. [Mill에 번들된 라이브러리](#10-mill에-번들된-라이브러리)
11. [JVM 버전 설정](#11-jvm-버전-설정)

---

## 1. 라이브러리 의존성 기본

Mill은 **Coursier**로 의존성을 해석하고 다운로드한다. 의존성 좌표는 다음 형식을 따른다.

```scala
mvn"{organization}:{name}:{version}"
mvn"{organization}:{name}:{version}[;{attribute}={value}]*"
```

Maven 생태계 용어로는 `organization`이 group, `name`이 artifact에 해당하고, 이 둘과 `version`을 합쳐 GAV(GroupArtifactVersion)라 부른다.

```scala
def mvnDeps = Seq(
  mvn"org.slf4j:slf4j-api:1.7.25"
)
```

---

## 2. 의존성 종류: mvnDeps / compileMvnDeps / runMvnDeps

Mill에는 다른 빌드 도구가 흔히 쓰는 "test scope" 개념이 없다. 테스트는 scope가 아니라 독립된 모듈로 표현한다.

```scala
object main extends JavaModule {
  object test extends JavaTests {
    def mvnDeps = Seq(
      mvn"ch.qos.logback:logback-classic:1.2.3"
    )
  }
}
```

컴파일 시에만 필요한 의존성은 `compileMvnDeps`로 선언하며, 생성되는 `pom.xml`에서는 `provided` scope로 매핑된다. provided scope 의존성은 전이적으로 해결되지 않는다는 점에 주의한다.

```scala
def compileMvnDeps = Seq(
  mvn"org.slf4j:slf4j-api:1.7.25"
)
```

런타임에만 필요한 의존성은 `runMvnDeps`로 선언한다. 컴파일은 최소 API 버전으로 하고 런타임에는 더 새 버전을 쓰는 조합에 유용하다.

```scala
def runMvnDeps = Seq(
  mvn"ch.qos.logback:logback-classic:1.2.0"
)
```

---

## 3. 전이 의존성 확인

### 의존성 트리

```bash
mill myModule.showMvnDepsTree
mill __.showMvnDepsTree
```

출력에서 화살표(`->`)는 버전이 변경되었음을, `(possible incompatibility)` 주석은 잠재적 충돌 가능성을 나타낸다.

### 해결된 의존성 조회

```bash
mill show myModule.resolvedMvnDeps
mill myModule.resolvedMvnDeps
```

결과는 `out/myModule/resolvedMvnDeps.json`에 저장된다.

### 의존성 출처 추적

`--whatDependsOn` 옵션으로 특정 artifact가 어느 경로로 들어왔는지 확인한다.

```bash
./mill main.showMvnDepsTree --whatDependsOn com.github.plokhotnyuk.jsoniter-scala:jsoniter-scala-core_2.13
```

여러 artifact를 동시에 지정할 수도 있다.

```bash
./mill main.showMvnDepsTree --whatDependsOn artifact1 --whatDependsOn artifact2
```

이때 패턴은 `org:artifact` 형식이며 version은 포함하지 않는다.

---

## 4. 전이 의존성 제외와 버전 강제

### exclude

조직/이름 튜플을 지정해 특정 전이 의존성을 제외한다. `*`는 모든 조직 또는 이름에 매칭된다.

```scala
def deps = Seq(
  mvn"com.lihaoyi::pprint:0.5.3".exclude("com.lihaoyi" -> "fansi_2.12")
)
```

단축 표기법도 지원한다.

```scala
def deps = Seq(
  mvn"com.lihaoyi::pprint:0.5.3;exclude=com.lihaoyi:fansi_2.12"
)
```

조직 전체 또는 이름 단위로 제외할 수도 있고, 체이닝도 가능하다.

```scala
val deps1 = Seq(mvn"com.lihaoyi::pprint:0.5.3".excludeOrg("com.lihaoyi"))

val deps2 = Seq(
  mvn"com.lihaoyi::pprint:0.5.3"
    .excludeName("fansi_2.12")
    .excludeName("sourcecode")
)
```

### forceVersion

특정 의존성의 버전을 강제로 고정한다.

```scala
val deps = Seq(mvn"com.lihaoyi::fansi:0.2.14".forceVersion())
```

예를 들어 pprint 0.8.1이 전이적으로 fansi 0.4.0을 요구해도, `forceVersion()`을 적용한 fansi 0.2.14가 최종적으로 사용된다.

---

## 5. 의존성 관리(BOM)

의존성 관리는 버전 강제와 exclude를 한꺼번에 적용하는 방식이다. 관리 대상 의존성을 직접 선언하지 않아도, 전이적으로 나타나기만 하면 지정한 버전이 강제된다.

### 외부 BOM 사용

`bomMvnDeps`에 외부 Maven BOM을 지정한다.

```yaml
# foo/package.mill.yaml
extends: JavaModule
bomMvnDeps:
- com.google.cloud:libraries-bom:26.50.0

mvnDeps:
- io.grpc:grpc-protobuf
```

`grpc-protobuf`는 버전을 명시하지 않았으므로 BOM에 정의된 `1.67.1`이 사용된다. BOM이 `protobuf-java`를 `4.28.3`으로 지정한다면, 원래 `grpc-protobuf`가 요구하는 `3.25.3` 대신 `4.28.3`이 채택된다.

여러 BOM을 동시에 쓸 때 같은 의존성 버전이 겹치면 먼저 선언한 BOM이 우선하고, exclude 항목은 모두 누적된다.

### depManagement로 직접 관리

```yaml
# foo/package.mill.yaml
extends: JavaModule
depManagement:
- com.google.protobuf:protobuf-java:4.28.3
- io.grpc:grpc-protobuf:1.67.1

mvnDeps:
- io.grpc:grpc-protobuf
```

`depManagement` 항목에도 exclude를 붙일 수 있다.

```scala
object bar extends JavaModule {
  def depManagement = Seq(
    mvn"io.grpc:grpc-protobuf:1.67.1"
      .exclude(("com.google.protobuf", "protobuf-java"))
  )
  def mvnDeps = Seq(
    mvn"io.grpc:grpc-protobuf"
  )
}
```

버전을 생략한 exclude 전용 항목도 허용된다.

```scala
object baz extends JavaModule {
  def depManagement = Seq(
    mvn"io.grpc:grpc-protobuf"
      .exclude(("com.google.protobuf", "protobuf-java"))
  )
  def mvnDeps = Seq(
    mvn"io.grpc:grpc-protobuf:1.67.1"
  )
}
```

### BOM 모듈 생성

`BomModule`을 섞어 넣으면 모듈 자체를 BOM으로 사용할 수 있다.

```yaml
# myBom/package.mill.yaml
extends: [JavaModule, BomModule]
bomMvnDeps:
- com.google.protobuf:protobuf-bom:4.28.1

depManagement:
- io.grpc:grpc-protobuf:1.67.1
```

정의한 BOM 모듈은 `bomModuleDeps`로 참조한다.

```yaml
extends: JavaModule
bomModuleDeps:
- myBom

mvnDeps:
- io.grpc:grpc-protobuf
```

### BOM 모듈 발행

`BomModule`과 `PublishModule`을 함께 섞으면 BOM을 다른 프로젝트가 소비할 수 있도록 발행할 수 있다.

```scala
object myPublishedBom extends BomModule, MyPublishModule {
  def bomMvnDeps = Seq(
    mvn"com.google.protobuf:protobuf-bom:4.28.1"
  )
  def depManagement = Seq(
    mvn"io.grpc:grpc-protobuf:1.67.1"
  )
}

trait MyPublishModule extends PublishModule {
  def pomSettings = PomSettings(
    description = "My Project",
    organization = "com.lihaoyi.mill-examples",
    url = "https://github.com/com-lihaoyi/mill",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("com-lihaoyi", "mill"),
    developers = Seq(Developer("me", "Me", "https://github.com/me"))
  )
  def publishVersion = "0.1.0"
}
```

발행된 BOM은 다른 모듈에서 그대로 `bomModuleDeps`로 가져다 쓴다.

```scala
object publishedBomUser extends JavaModule, MyPublishModule {
  def bomModuleDeps = Seq(
    myPublishedBom
  )
  def mvnDeps = Seq(
    mvn"io.grpc:grpc-protobuf"
  )
}
```

---

## 6. 의존성 업데이트 검색

`mill.javalib.Dependency/showUpdates` 태스크로 빌드 전체의 의존성 최신 버전을 조회한다. JavaModule 계열(ScalaModule, KotlinModule 포함)과 Maven 리포지토리 대상으로만 동작하며, 빌드에 존재하는 모든 모듈에 항상 적용된다.

```bash
./mill mill.javalib.Dependency/showUpdates
```

```
Found 2 dependency update for bar
  com.lihaoyi:mainargs_2.13 : 0.4.0 -> 0.5.0 -> 0.5.1 -> ...
  com.lihaoyi:scalatags_2.13 : 0.8.2 -> 0.8.3 -> 0.8.4 -> ...
```

출력 형식을 모듈별이 아니라 의존성별로 묶어서 보려면 `--format PerDependency`를 쓴다.

```bash
./mill mill.scalalib.Dependency/showUpdates --format PerDependency
```

```
com.lihaoyi:mainargs_2.13 : 0.4.0 -> 0.5.0 -> ... in bar, foo
com.lihaoyi:scalatags_2.13 : 0.8.2 -> 0.8.3 -> ... in bar
```

프리릴리스 버전까지 포함하려면 `--allowPreRelease true`, 메타빌드(`build.mill`을 빌드하는 빌드) 자체의 의존성을 확인하려면 `--meta-level 1`을 붙인다.

```bash
./mill mill.javalib.Dependency/showUpdates --allowPreRelease true
./mill --meta-level 1 mill.javalib.Dependency/showUpdates
```

---

## 7. Cross Build 기본

Cross build는 동일한 소스와 설정을 여러 버전이나 플랫폼에 대해 반복 빌드하는 기법이다. 같은 Scala 코드를 여러 Scala 버전으로 빌드하거나, 개발용/릴리스용 산출물을 나란히 만드는 상황에 쓴다.

```scala
object foo extends Cross[FooModule]("2.10", "2.11", "2.12")
trait FooModule extends Cross.Module[String] {
  def suffix = Task { "_" + crossValue }
  def bigSuffix = Task { "[[[" + suffix() + "]]]" }
  def sources = Task.Sources(moduleDir)
}
```

`Cross[T]`는 지정한 값 목록만큼 모듈 인스턴스를 생성한다. 각 인스턴스는 `Cross.Module[String]`을 상속하며, `crossValue`로 자신에게 배정된 값에 접근한다. 기본 상태에서는 모든 인스턴스가 같은 `foo` 소스 폴더를 공유한다.

```bash
./mill show foo.2_10.suffix     # "_2.10"
./mill show foo.2_12.bigSuffix  # "[[[_2.12]]]"
```

버전 문자열의 점(`.`)은 명령줄에서 언더스코어로 치환해 참조한다(`2.10` -> `2_10`).

기본으로 선택되는 값은 목록의 첫 번째지만, `defaultCrossSegments`로 재정의할 수 있다.

```scala
object bar extends Cross[FooModule]("2.10", "2.11", "2.12") {
  def defaultCrossSegments = Seq("2.12")
}
```

```bash
./mill show foo[].suffix   # 기본값(2.10)으로 실행
./mill show bar[].suffix   # 2.12로 실행
```

각 cross 값이 독립된 소스 폴더를 쓰게 하려면 `moduleDir`을 값별로 분기한다.

```scala
object foo extends Cross[FooModule]("2.10", "2.11", "2.12")
trait FooModule extends Cross.Module[String] {
  def moduleDir = super.moduleDir / crossValue
  def sources = Task.Sources(moduleDir)
}
```

이 설정에서는 `foo/2.10`, `foo/2.11`, `foo/2.12` 폴더가 각각 독립적인 소스 루트가 된다. 출력 경로 역시 cross 값별로 분리된다.

```
out/
├── foo/
│   ├── 2.10/
│   │   ├── suffix.json
│   │   └── bigSuffix.json
│   ├── 2.11/
│   └── 2.12/
```

---

## 8. Cross 모듈 참조와 다중 축

### 외부 태스크에서 참조

`foo("2.10")` 형태로 특정 cross 값의 인스턴스에 접근한다.

```scala
object foo extends Cross[FooModule]("2.10", "2.11", "2.12")
trait FooModule extends Cross.Module[String] {
  def suffix = Task { "_" + crossValue }
}

def bar = Task { s"hello ${foo("2.10").suffix()}" }
def qux = Task { s"hello ${foo("2.10").suffix()} world ${foo("2.12").suffix()}" }
```

### Cross 모듈 간 참조

`crossValue`를 그대로 다른 Cross 모듈의 키로 전달하면, 같은 값을 가진 인스턴스끼리 자동으로 매칭된다.

```scala
object foo extends mill.Cross[FooModule]("2.10", "2.11", "2.12")
trait FooModule extends Cross.Module[String] {
  def suffix = Task { "_" + crossValue }
}

object bar extends mill.Cross[BarModule]("2.10", "2.11", "2.12")
trait BarModule extends Cross.Module[String] {
  def bigSuffix = Task { "[[[" + foo(crossValue).suffix() + "]]]" }
}
```

```bash
./mill showNamed bar._.bigSuffix
# {
#   "bar.2_10.bigSuffix": "[[[_2.10]]]",
#   "bar.2_11.bigSuffix": "[[[_2.11]]]",
#   "bar.2_12.bigSuffix": "[[[_2.12]]]"
# }
```

### 내부 Cross 모듈 (CrossValue)

`CrossValue` 트레이트를 섞으면 Cross 모듈 내부의 중첩 모듈이 상위 `crossValue`를 자동으로 물려받는다.

```scala
trait MyModule extends Module {
  def crossValue: String
  def name: T[String]
  def param = Task { name() + " Param Value: " + crossValue }
}

object foo extends Cross[FooModule]("a", "b")
trait FooModule extends Cross.Module[String] {
  object bar extends MyModule, CrossValue {
    def name = "Bar"
  }
  object qux extends MyModule, CrossValue {
    def name = "Qux"
  }
}
```

```bash
./mill show foo.a.bar.param   # "Bar Param Value: a"
./mill show foo.b.qux.param   # "Qux Param Value: b"
```

### Cross Resolver로 단축 문법 사용

암묵적 `Cross.Resolver`를 정의하면 `foo(crossValue)` 대신 `foo()`처럼 짧게 쓸 수 있다.

```scala
trait MyModule extends Cross.Module[String] {
  implicit object resolver extends mill.api.Cross.Resolver[MyModule] {
    def resolve[V <: MyModule](c: Cross[V]): V =
      c.valuesToModules(List(crossValue))
  }
}

object bar extends mill.Cross[BarModule]("2.10", "2.11", "2.12")
trait BarModule extends MyModule {
  def bigSuffix = Task { "[[[" + foo().suffix() + "]]]" }
}
```

### 다중 축(Multiple Cross Axes)

축이 두 개 이상 필요하면 튜플 시퀀스와 `Cross.Module2[T1, T2]`(세 축이면 `Module3`, 이런 식으로 확장)를 사용한다.

```scala
val crossMatrix = for {
  crossVersion <- Seq("2.10", "2.11", "2.12")
  platform <- Seq("jvm", "js", "native")
  if !(platform == "native" && crossVersion != "2.12")
} yield (crossVersion, platform)

object foo extends mill.Cross[FooModule](crossMatrix)
trait FooModule extends Cross.Module2[String, String] {
  val (crossVersion, platform) = (crossValue, crossValue2)
  def suffix = Task { "_" + crossVersion + "_" + platform }
}
```

```bash
./mill show foo.2_10.jvm.suffix    # "_2.10_jvm"
./mill showNamed foo.__.suffix     # 모든 조합 표시
```

기존 단일 축 Cross 모듈을 다중 축으로 확장할 수도 있다. 이때 값 목록의 튜플 크기가 `Module2`, `Module3` 등의 타입 인자 개수와 맞지 않으면 컴파일 에러가 난다.

```scala
object foo extends Cross[FooModule]("a", "b")
trait FooModule extends Cross.Module[String] {
  def param1 = Task { "Param Value: " + crossValue }
}

object foo2 extends Cross[FooModule2](("a", 1), ("b", 2))
trait FooModule2 extends Cross.Module2[String, Int] {
  def param1 = Task { "Param Value: " + crossValue }
  def param2 = Task { "Param Value: " + crossValue2 }
}
```

---

## 9. 동적 Cross 모듈

Cross 값 목록을 정적으로 나열하지 않고, 파일시스템 등 런타임 정보로부터 계산할 수도 있다. `BuildCtx.watchValue`로 감싸면 Mill이 해당 값의 변화를 감지해 모듈 구조를 자동으로 재계산한다.

```scala
import mill.api.BuildCtx

val moduleNames = BuildCtx.watchValue(
  os.list(moduleDir / "modules").map(_.last)
)

object modules extends Cross[FolderModule](moduleNames)
trait FolderModule extends ScalaModule, Cross.Module[String] {
  def moduleDir = super.moduleDir / crossValue
  def scalaVersion = "2.13.16"
}
```

```bash
./mill resolve modules._    # 현재 존재하는 모듈 목록
./mill modules.foo.run
```

`modules` 아래에 폴더를 추가하면 다음 빌드 시 자동으로 새 cross 모듈로 인식된다.

### 실무 예제: 정적 블로그 빌드

마크다운 포스트 목록을 동적 Cross 모듈로 만들어 HTML로 렌더링하는 예시다.

```scala
val posts = BuildCtx.watchValue {
  os.list(moduleDir / "post").map(_.last).sorted
}

object post extends Cross[PostModule](posts)
trait PostModule extends Cross.Module[String] {
  def source = Task.Source(moduleDir / crossValue)
  def render = Task {
    val doc = Parser.builder().build().parse(os.read(source().path))
    val title = mdNameToTitle(crossValue)
    val rendered = doctype("html")(
      html(body(
        h1(a("Blog", href := "../index.html"), " / ", title),
        raw(HtmlRenderer.builder().build().render(doc))
      ))
    )
    os.write(Task.dest / mdNameToHtml(crossValue), rendered)
    PathRef(Task.dest / mdNameToHtml(crossValue))
  }
}

def index = Task {
  val rendered = doctype("html")(
    html(body(h1("Blog"), postsInput().map(renderIndexEntry)))
  )
  os.write(Task.dest / "index.html", rendered)
  PathRef(Task.dest / "index.html")
}

def dist = Task {
  for (post <- Task.traverse(post.crossModules)(_.render)()) {
    os.copy(post.path, Task.dest / "post" / post.path.last,
      createFolders = true)
  }
  os.copy(index().path, Task.dest / "index.html")
  PathRef(Task.dest)
}
```

포스트 파일을 추가하면 별도 설정 변경 없이 자동으로 인식되고, 변경된 파일만 재처리되며 `-j` 플래그로 병렬 렌더링도 가능하다.

---

## 10. Mill에 번들된 라이브러리

Mill은 빌드 도구 내부적으로 여러 오픈소스 라이브러리를 사용하며, 이들은 빌드 스크립트(`build.mill`)에서 직접 import 없이 바로 쓸 수 있다.

| 라이브러리 | 용도 |
|---|---|
| **OS-Lib** | 파일시스템·서브프로세스 연산 |
| **uPickle** | 태스크 출력의 JSON 직렬화(캐싱용) |
| **Requests-Scala** | HTTP 요청을 통한 파일 다운로드 |
| **MainArgs** | 커맨드라인 인자 파싱 |
| **Coursier** | JVM 아티팩트(의존성) 해결 |

### OS-Lib

Mill이 파일 및 프로세스 연산에 전반적으로 사용하는 라이브러리다. 각 태스크는 격리된 작업 디렉터리(`Task.dest`)에서 실행되어 태스크 간 파일 간섭이 발생하지 않는다.

```scala
def task1 = Task {
  os.write(os.pwd / "file.txt", "hello")
  PathRef(os.pwd / "file.txt")
}
```

### uPickle

모든 Mill 태스크의 결과값은 uPickle을 거쳐 JSON으로 저장되고, 이 값을 기준으로 캐시 무효화 여부를 판단한다. 기본 타입, 컬렉션, `Path`, `PathRef` 등을 직렬화 대상으로 지원한다.

```scala
def taskMap = Task { Map("int" -> "123", "boolean" -> "true") }
// 출력: {"int": "123", "boolean": "true"}
```

### Requests-Scala

컴파일러, 소스코드, 데이터 파일 등을 원격에서 내려받을 때 쓴다.

```scala
def remoteFile = Task {
  os.write(Task.dest / "file.zip",
    requests.get("https://example.com/file.zip"))
}
```

### MainArgs

`Task.Command`의 매개변수로 문자열, 정수, 불린 등 기본 타입을 그대로 받을 수 있게 해준다.

```scala
def command(str: String, i: Int, bool: Boolean = true) = Task.Command {
  println(s"$str $i $bool")
}
```

### Coursier

Scala, Java 등 JVM 생태계의 서드파티 의존성 해결을 담당하는 엔진으로, [1. 라이브러리 의존성 기본](#1-라이브러리-의존성-기본)에서 다룬 `mvnDeps` 해석의 실질적인 구현체다.

각 라이브러리의 상세 API는 해당 프로젝트의 ScalaDoc과 저장소 페이지에서 확인한다.

---

## 11. JVM 버전 설정

Mill은 기본적으로 자체 실행 중인 JVM(예: `zulu:25`)을 Java/Scala/Kotlin 모듈 컴파일에 사용한다. 필요한 JVM은 자동으로 다운로드하고 캐싱하므로, 별도의 전역 JDK 설치 없이도 원하는 버전으로 빌드할 수 있다.

### Mill 자체가 쓰는 JVM 버전 지정

선언적 설정(`build.mill.yaml`):

```yaml
mill-jvm-version: 19
```

프로그래머블 설정(`build.mill` 헤더 주석):

```
//| mill-jvm-version: 19
```

버전 문자열은 `"{version}"` 또는 `"{name}:{version}"` 형식을 쓴다. 예: `temurin:11.0.21`, `zulu:25`.

### 모듈별 JVM 버전 지정

선언적 설정(`package.mill.yaml`):

```yaml
extends: JavaModule
jvmVersion: temurin:11.0.21
```

프로그래머블 설정(`package.mill`):

```scala
object `package` extends JavaModule {
  def jvmVersion = "temurin:11.0.21"
}
```

### JVM 지정 방법 정리

| 방법 | 예시 |
|---|---|
| 표준 배포판(distribution) 이름 | `temurin:11.0.21`, `zulu:25` |
| JVM Index 버전 지정 | `jvmIndexVersion: latest.release` |
| 명시적 다운로드 URL | `https://github.com/adoptium/.../OpenJDK22U-jdk_x64_linux_hotspot_22.0.2_9.tar.gz` |
| 로컬 JVM 경로 | `javaHome: /my/java/home` |

사용 가능한 배포판 목록은 [coursier/jvm-index](https://github.com/coursier/jvm-index)에서 확인한다. 다운로드 URL을 직접 지정하는 경우 OS/아키텍처별 분기 처리가 필요하다.

### JVM 옵션 전달

`javacOptions`에 `-J` 접두사를 붙이면 JVM 자체에 옵션을 전달할 수 있다.

```yaml
javacOptions:
  - "-J-Xss8m"
```

### 모듈에 연결된 JVM 명령 직접 실행

```bash
./mill foo.java -version
./mill foo.javap
./mill foo.java -jar out/foo/assembly.dest/out.jar
```

### 버전 선택 가이드

- **라이브러리**: 최대한 낮은(하지만 지원 대상인) 버전, 예를 들어 17을 타깃으로 컴파일해 호환 범위를 넓힌다.
- **애플리케이션**: 최신 버전을 사용해 성능·보안 개선을 그대로 누린다.

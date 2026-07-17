# Mill Scala 빌드 예제 (실전 프로젝트 / 웹 / Native)

> 원본: https://mill-build.org/mill/scalalib/build-examples.html, https://mill-build.org/mill/scalalib/web-examples.html, https://mill-build.org/mill/scalalib/native-examples.html

---

## 목차

1. [실전 프로젝트 빌드: Acyclic - 크로스 버전 단일 모듈](#1-실전-프로젝트-빌드-acyclic---크로스-버전-단일-모듈)
2. [실전 프로젝트 빌드: Fansi - 버전×플랫폼 크로스 매트릭스](#2-실전-프로젝트-빌드-fansi---버전플랫폼-크로스-매트릭스)
3. [알려진 실제 Mill 빌드 프로젝트](#3-알려진-실제-mill-빌드-프로젝트)
4. [웹 서버 빌드: 기본 구성과 테스트](#4-웹-서버-빌드-기본-구성과-테스트)
5. [웹 서버 빌드: 정적 리소스 캐시 버스팅](#5-웹-서버-빌드-정적-리소스-캐시-버스팅)
6. [웹 서버 빌드: Http4s 프레임워크 연동](#6-웹-서버-빌드-http4s-프레임워크-연동)
7. [Scala.js 단독 모듈](#7-scalajs-단독-모듈)
8. [Scala.js + 백엔드 서버 통합](#8-scalajs--백엔드-서버-통합)
9. [Scala.js/JVM 코드 공유 모듈](#9-scalajsjvm-코드-공유-모듈)
10. [크로스 버전 × 크로스 플랫폼 모듈 발행](#10-크로스-버전--크로스-플랫폼-모듈-발행)
11. [Scala.js WebAssembly 빌드](#11-scalajs-webassembly-빌드)
12. [Scala Native: 기본 구성](#12-scala-native-기본-구성)
13. [Scala Native: C/C++ 코드 연동(interop)](#13-scala-native-cc-코드-연동interop)
14. [Scala Native: 멀티모듈 구성](#14-scala-native-멀티모듈-구성)
15. [Scala Native: 공통 설정 오버라이드](#15-scala-native-공통-설정-오버라이드)

---

## 1. 실전 프로젝트 빌드: Acyclic - 크로스 버전 단일 모듈

Mill 배포판에는 실제 오픈소스 프로젝트를 Mill로 빌드하는 예제가 함께 들어 있다. `acyclic`은 Scala 컴파일러 플러그인이며, Scala 2.12.8부터 2.13.16까지 모든 point 버전에 대해 크로스 빌드된다.

핵심 패턴은 다음과 같다.

- `Cross[AcyclicModule](crosses)`로 버전 목록을 순회하며 모듈을 생성
- `CrossScalaModule`을 상속해 `crossScalaVersion`을 자동으로 주입받음
- `PublishModule`을 함께 상속해 `pomSettings`/`publishVersion` 정의
- 컴파일 시에만 필요한 `scala-compiler`를 `compileMvnDeps`로 선언
- 테스트는 `TestModule.Utest`를 사용하고, 테스트 리소스가 `os.pwd` 상대 경로를 가정하므로 `BuildCtx.withFilesystemCheckerDisabled` 블록에서 파일을 미리 복사해둠

```scala
package build

import mill.*, scalalib.*, publish.*
import mill.api.BuildCtx

// acyclic 테스트 스위트는 os.pwd 기준 특정 경로에 파일이 있다고 가정한다.
// 테스트 코드를 바꾸지 않기 위해, 테스트 실행 전 리소스를 os.pwd로 복사한다.
BuildCtx.withFilesystemCheckerDisabled {
  os.copy.over(
    BuildCtx.watch(mill.api.BuildCtx.workspaceRoot / "acyclic"),
    os.pwd / "acyclic",
    createFolders = true
  )
}

object Deps {
  def acyclic = mvn"com.lihaoyi:::acyclic:0.3.6"
  def scalaCompiler(scalaVersion: String) = mvn"org.scala-lang:scala-compiler:$scalaVersion"
  val utest = mvn"com.lihaoyi::utest:0.8.5"
}

val crosses =
  Range.inclusive(8, 20).map("2.12." + _) ++
    Range.inclusive(0, 16).map("2.13." + _)

object acyclic extends Cross[AcyclicModule](crosses)
trait AcyclicModule extends CrossScalaModule, PublishModule {
  def crossFullScalaVersion = true
  def artifactName = "acyclic"
  def publishVersion = "1.3.3.7"

  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "com.lihaoyi",
    url = "https://github.com/com-lihaoyi/acyclic",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github(owner = "com-lihaoyi", repo = "acyclic"),
    developers = Seq(Developer("lihaoyi", "Li Haoyi", "https://github.com/lihaoyi"))
  )

  def compileMvnDeps = Seq(Deps.scalaCompiler(crossScalaVersion))

  object test extends ScalaTests, TestModule.Utest {
    def sources = Task.Sources("src", "resources")
    def mvnDeps = Seq(Deps.utest, Deps.scalaCompiler(crossScalaVersion))
  }
}
```

`crossFullScalaVersion = true`는 크로스 버전 문자열을 `2_13_16`처럼 point 버전까지 그대로 태스크 경로에 반영하도록 만든다. 실행은 버전별로 아래처럼 접두사를 붙인다.

```
> ./mill acyclic.2_12_20.compile
compiling 7 Scala sources...
...

> ./mill acyclic.2_13_16.test.testLocal # acyclic tests need testLocal due to classloader assumptions
...
```

---

## 2. 실전 프로젝트 빌드: Fansi - 버전×플랫폼 크로스 매트릭스

`fansi`는 모든 Scala 마이너 버전(2.12, 2.13, 3.x)과 모든 플랫폼(JVM, JS, Native)에 대해 동시에 크로스 빌드되는 작은 라이브러리 예제다. 라이브러리 본체와 테스트 스위트가 버전×플랫폼 조합(matrix) 전체에 걸쳐 중복 정의되며, 원하는 조합만 골라 compile/test/publish할 수 있다.

공통 설정은 트레이트로 뽑아내고, 각 플랫폼별 모듈은 해당 플랫폼 전용 트레이트(`ScalaModule`/`ScalaJSModule`/`ScalaNativeModule`)를 추가로 mixin한다.

```scala
package build

import mill._, scalalib._, scalajslib._, scalanativelib._, publish.*
import mill.util.VcsVersion
import mill.javalib.api.JvmWorkerUtil.isScala3

val dottyCommunityBuildVersion = sys.props.get("dottyVersion")

val scalaVersions = Seq("2.12.20", "2.13.16", "3.3.6") ++ dottyCommunityBuildVersion

trait FansiModule extends PublishModule with CrossScalaModule with PlatformScalaModule {
  def artifactName = "fansi"

  def publishVersion = "1.3.3.7"

  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "com.lihaoyi",
    url = "https://github.com/com-lihaoyi/Fansi",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github(owner = "com-lihaoyi", repo = "fansi"),
    developers = Seq(
      Developer("lihaoyi", "Li Haoyi", "https://github.com/lihaoyi")
    )
  )

  def mvnDeps = Seq(mvn"com.lihaoyi::sourcecode::0.4.0")
}

trait FansiTestModule extends ScalaModule with TestModule.Utest {
  def mvnDeps = Seq(mvn"com.lihaoyi::utest::0.8.3")
}

object fansi extends Module {
  object jvm extends Cross[JvmFansiModule](scalaVersions)
  trait JvmFansiModule extends FansiModule with ScalaModule {
    object test extends ScalaTests with FansiTestModule
  }

  object js extends Cross[JsFansiModule](scalaVersions)
  trait JsFansiModule extends FansiModule with ScalaJSModule {
    def scalaJSVersion = "1.20.2"
    object test extends ScalaJSTests with FansiTestModule
  }

  object native extends Cross[NativeFansiModule](scalaVersions)
  trait NativeFansiModule extends FansiModule with ScalaNativeModule {
    def scalaNativeVersion = "0.5.8"
    object test extends ScalaNativeTests with FansiTestModule
  }
}
```

`PlatformScalaModule`이 `FansiModule`에 섞여 들어가면서 `jvm`/`js`/`native` 각 서브모듈이 소스 폴더 우선순위(`src`, `src-jvm`, `src-2.13.16` 등)를 플랫폼/버전에 맞춰 자동으로 잡아준다.

버전(3) × 플랫폼(3) × 라이브러리/테스트(2) 조합으로 총 18개의 `compile` 태스크가 생성된다.

```
> ./mill resolve __.compile
fansi.js.2_12_20.compile
fansi.js.2_12_20.test.compile
...
fansi.native.3_3_6.test.compile

> ./mill fansi.jvm.2_12_20.compile
compiling 1 Scala source...

> ./mill fansi.js.2_13_16.test
Starting process: node
-------------------------------- Running Tests --------------------------------
...

> ./mill fansi.native.3_3_6.publishLocal
Publishing com.lihaoyi:fansi_native0.5_3:1.3.3.7 to ivy repo...
```

---

## 3. 알려진 실제 Mill 빌드 프로젝트

문서에서는 Mill로 빌드되는 대표적인 실전 오픈소스 프로젝트 세 가지를 언급한다.

| 프로젝트 | 설명 |
|---|---|
| [Ammonite](https://github.com/com-lihaoyi/Ammonite) | 사용하기 편한(ergonomic) Scala REPL |
| [Scala-CLI](https://github.com/VirtusLab/scala-cli) | 터미널에서 `scala` 명령으로 실행되는 기본 CLI 도구. 컴파일/테스트/실행/패키징을 다양한 방식으로 지원 |
| [Coursier](https://github.com/coursier/coursier) | 여러 빌드 도구에서 서드파티 의존성 해석·다운로드에 쓰이는 빠른 JVM 의존성 리졸버 |

---

## 4. 웹 서버 빌드: 기본 구성과 테스트

가장 단순한 웹 서버 예제는 [Cask](https://github.com/com-lihaoyi/cask) 프레임워크를 사용하며, `build.mill.yaml`(YAML 빌드 헤더) 형식으로도 표현 가능할 만큼 설정이 짧다.

```yaml
extends: ScalaModule
scalaVersion: 3.8.2
mvnDeps: [com.lihaoyi::cask:0.9.1]
```

```scala
package example
object WebServer extends cask.MainRoutes {
  override def port = sys.env.getOrElse("PORT", "8080").toInt

  @cask.post("/reverse-string")
  def doThing(request: cask.Request) = {
    request.text().reverse
  }

  initialize()
}
```

```
> ./mill runBackground

> curl -d 'helloworld' localhost:${PORT:-8080}/reverse-string
dlrowolleh

> ./mill clean runBackground # shut down webserver
```

테스트는 `test/package.mill.yaml`로 별도 정의하며, 실제 서버를 띄운 뒤 HTTP 요청을 보내 동작을 검증하는 방식이다.

```yaml
extends: [build.ScalaTests, TestModule.Utest]
mvnDeps:
- com.lihaoyi::requests:0.9.0
- com.lihaoyi::utest:0.9.1
```

TodoMVC 예제는 여기에 uPickle(JSON)과 Scalatags(HTML 생성)를 추가해 실전 웹 앱 구조를 보여준다.

```yaml
extends: ScalaModule
scalaVersion: 3.8.2
mvnDeps:
- com.lihaoyi::cask:0.9.1
- com.lihaoyi::upickle:4.3.0
- com.lihaoyi::scalatags:0.13.1
```

---

## 5. 웹 서버 빌드: 정적 리소스 캐시 버스팅

정적 파일에 콘텐츠 해시를 붙여 파일명을 바꾸고, 원본 경로 → 해시 경로 매핑을 JSON으로 저장해두는 "cache busting" 패턴이다. CDN 등에서 정적 파일을 긴 만료 시간으로 캐싱해도, 배포 시 파일명이 바뀌므로 클라이언트가 즉시 새 파일을 받아가게 된다.

핵심은 `resources` 태스크를 오버라이드해서 `super.resources()`의 결과를 순회하며 해시된 사본을 만들고, 매핑 정보를 `hashed-resource-mapping.json`으로 함께 내보내는 것이다.

```scala
package build

import mill.*, scalalib.*
import java.util.Arrays

object `package` extends ScalaModule {
  def scalaVersion = "3.8.2"
  def mvnDeps = Seq(
    mvn"com.lihaoyi::cask:0.9.1",
    mvn"com.lihaoyi::upickle:4.3.0",
    mvn"com.lihaoyi::scalatags:0.13.1",
    mvn"com.lihaoyi::os-lib:0.11.4"
  )

  def resources = Task {
    val hashMapping = for {
      resourceRoot <- super.resources()
      path <- os.walk(resourceRoot.path)
      if os.isFile(path)
    } yield hashFile(path, resourceRoot.path, Task.dest)

    os.write(
      Task.dest / "hashed-resource-mapping.json",
      upickle.write(hashMapping.toMap, indent = 4)
    )

    Seq(PathRef(Task.dest))
  }

  object test extends ScalaTests, TestModule.Utest {
    def mvnDeps = Seq(
      mvn"com.lihaoyi::utest::0.8.9",
      mvn"com.lihaoyi::requests::0.6.9"
    )
  }

  def hashFile(path: os.Path, src: os.Path, dest: os.Path) = {
    val hash = Integer.toHexString(Arrays.hashCode(os.read.bytes(path)))
    val relPath = path.relativeTo(src)
    val ext = if (relPath.ext == "") "" else s".${relPath.ext}"
    val hashedPath = relPath / os.up / s"${relPath.baseName}-$hash$ext"
    os.copy(path, dest / hashedPath, createFolders = true)
    (relPath.toString(), hashedPath.toString())
  }
}
```

실행 시 실제로 해시가 붙은 파일 경로로 정적 리소스가 서빙되는 것을 확인할 수 있다.

```
> curl http://localhost:${PORT:-8080}/static/main-6da98e99.js # mac/linux
initListeners()
```

이 패턴은 `resources` 태스크를 오버라이드해 빌드 산출물을 가공하는 일반적인 Mill 확장 방식의 좋은 예시이기도 하다.

---

## 6. 웹 서버 빌드: Http4s 프레임워크 연동

Cask 대신 [Http4s](https://http4s.org/)(Ember 서버 + DSL) 기반으로도 동일한 TodoMVC 앱을 구성할 수 있다. Circe(JSON), Scalatags-http4s 연동 라이브러리가 함께 쓰인다.

```yaml
extends: ScalaModule
scalaVersion: 3.8.2
mvnDeps:
- org.http4s::http4s-ember-server::0.23.30
- org.http4s::http4s-dsl::0.23.30
- org.http4s::http4s-scalatags::0.25.2
- io.circe::circe-generic::0.14.10
```

테스트에서는 Cats Effect 기반 테스트 유틸리티(`cats-effect-testing-utest`)와 http4s 클라이언트를 사용한다.

```yaml
extends: [build.ScalaTests, TestModule.Utest]
mvnDeps:
- com.lihaoyi::utest::0.8.9
- org.typelevel::cats-effect-testing-utest::1.6.0
- org.http4s::http4s-client::0.23.30
```

Cask 예제와 마찬가지로 `test`, `runBackground` 태스크로 로컬 구동/검증이 가능하며, 웹 프레임워크가 바뀌어도 Mill 빌드 구조(모듈 정의 + mvnDeps + test 서브모듈) 자체는 동일하게 유지된다는 점이 핵심이다.

---

## 7. Scala.js 단독 모듈

`ScalaJSModule`은 `ScalaModule`과 거의 동일하지만 `scalaJSVersion`을 반드시 지정해야 한다. Scala.js 전용 의존성은 organization과 artifact 사이에 콜론을 **두 개**(`::`) 붙여 표기한다.

```yaml
# foo/package.mill.yaml
extends: mill.scalajslib.ScalaJSModule
scalaVersion: 3.8.2
scalaJSVersion: 1.20.2
mvnDeps:
- com.lihaoyi::scalatags::0.13.1
```

```yaml
# foo/test/package.mill.yaml
extends: [build.foo.ScalaJSTests, TestModule.Utest]
utestVersion: 0.8.9
```

`compile`/`run`/`test`는 그대로 쓸 수 있지만, `run`과 `test`는 JVM이 아니라 `node`를 호출해 실행된다. 여기에 더해 모듈 전체를 하나의 JS 파일로 링크하는 `fastLinkJS`(개발용, 빠른 빌드) / `fullLinkJS`(배포용, 최적화)가 추가로 제공된다.

```
> ./mill foo.run
<h1>Hello World</h1>
stringifiedJsObject: ["hello","world","!"]

> ./mill foo.test
+ foo.FooTests.hello...

> ./mill show foo.fullLinkJS # mac/linux
{
  ..."jsFileName": "main.js",
  "dest": ".../out/foo/fullLinkJS.dest"
}

> node out/foo/fullLinkJS.dest/main.js # mac/linux
<h1>Hello World</h1>
stringifiedJsObject: ["hello","world","!"]
```

Scala.js 모듈을 로컬에서 실행하려면 `node` 런타임이 설치되어 있어야 한다.

---

## 8. Scala.js + 백엔드 서버 통합

JVM 백엔드(Cask)와 Scala.js 프론트엔드를 한 빌드 안에서 묶는 패턴. 핵심은 백엔드의 `resources` 태스크에서 `client` 서브모듈의 `fastLinkJS()` 결과물(`main.js`, `main.js.map`)을 리소스 폴더로 복사해 넣는 것이다.

```scala
package build

import mill.*, scalalib.*, scalajslib.*

object `package` extends ScalaModule {

  def scalaVersion = "3.8.2"
  def mvnDeps = Seq(
    mvn"com.lihaoyi::cask:0.9.1",
    mvn"com.lihaoyi::upickle:4.3.0",
    mvn"com.lihaoyi::scalatags:0.13.1"
  )

  def resources = Task {
    os.makeDir(Task.dest / "webapp")
    val jsPath = client.fastLinkJS().dest.path
    // Move main.js[.map]into the proper filesystem position
    // in the resource folder for the web server code to pick up
    os.copy(jsPath / "main.js", Task.dest / "webapp/main.js")
    os.copy(jsPath / "main.js.map", Task.dest / "webapp/main.js.map")
    super.resources() ++ Seq(PathRef(Task.dest))
  }

  object test extends ScalaTests, TestModule.Utest {
    def utestVersion = "0.8.9"
    def mvnDeps = Seq(
      mvn"com.lihaoyi::requests::0.6.9"
    )
  }

  object client extends ScalaJSModule {
    def scalaVersion = "3.8.2"
    def scalaJSVersion = "1.20.2"
    def mvnDeps = Seq(mvn"org.scala-js::scalajs-dom::2.2.0")
  }
}
```

`client` 서브모듈이 부모 모듈 안에 중첩되어 있으므로, 부모의 `resources` 태스크가 `client.fastLinkJS()`를 태스크 의존성으로 참조하는 것만으로 "JS 빌드 → 리소스 복사 → 서버 패키징"까지 하나의 Mill 그래프로 자동 연결된다. 즉 별도의 빌드 파이프라인 스크립트 없이 `def resources` 오버라이드만으로 프론트엔드-백엔드 빌드를 묶을 수 있다.

---

## 9. Scala.js/JVM 코드 공유 모듈

클라이언트-서버 간 로직(JSON 직렬화, HTML 렌더링 등)을 재사용하려면 `shared` 모듈을 JVM/JS 양쪽으로 빌드한다. 공통 설정은 상위 트레이트로 추출한다.

```scala
package build

import mill.*, scalalib.*, scalajslib.*

trait AppScalaModule extends ScalaModule {
  def scalaVersion = "3.3.6"
}

trait AppScalaJSModule extends AppScalaModule, ScalaJSModule {
  def scalaJSVersion = "1.20.2"
}

object `package` extends AppScalaModule {
  def moduleDeps = Seq(shared.jvm)
  def mvnDeps = Seq(mvn"com.lihaoyi::cask:0.9.1")

  def resources = Task {
    os.makeDir(Task.dest / "webapp")
    val jsPath = client.fastLinkJS().dest.path
    os.copy(jsPath / "main.js", Task.dest / "webapp/main.js")
    os.copy(jsPath / "main.js.map", Task.dest / "webapp/main.js.map")
    super.resources() ++ Seq(PathRef(Task.dest))
  }

  object test extends ScalaTests, TestModule.Utest {
    def utestVersion = "0.8.9"
    def mvnDeps = Seq(
      mvn"com.lihaoyi::requests::0.6.9"
    )
  }

  object shared extends Module {
    trait SharedModule extends AppScalaModule, PlatformScalaModule {
      def mvnDeps = Seq(
        mvn"com.lihaoyi::scalatags::0.13.1",
        mvn"com.lihaoyi::upickle::4.3.0"
      )
    }

    object jvm extends SharedModule
    object js extends SharedModule, AppScalaJSModule
  }

  object client extends AppScalaJSModule {
    def moduleDeps = Seq(shared.js)
    def mvnDeps = Seq(mvn"org.scala-js::scalajs-dom::2.2.0")
  }
}
```

구조를 정리하면 다음과 같다.

| 모듈 | 플랫폼 | 역할 |
|---|---|---|
| `package` (루트) | JVM | Cask 백엔드 서버, `shared.jvm`에 의존 |
| `shared.jvm` | JVM | uPickle/Scalatags 기반 공유 로직 |
| `shared.js` | JS | 동일 소스를 Scala.js로 컴파일 |
| `client` | JS | `shared.js`에 의존하는 프론트엔드 |

서버는 초기 페이지 로드에는 HTML을, 이후 갱신에는 JSON을 내려주고 클라이언트가 이를 HTML로 렌더링하는 구조이며, 직렬화/렌더링 로직 자체는 `shared` 모듈 하나로 양쪽에서 공유된다.

---

## 10. 크로스 버전 × 크로스 플랫폼 모듈 발행

Scala 버전(2.13.16 / 3.3.6)과 플랫폼(JVM / JS)을 동시에 크로스 빌드하며 여러 개의 상호 의존 모듈(`bar`, `qux`)을 발행하는 예제. 두 가지 구성 방식이 있다.

### 10.1 상위 Cross로 감싸는 방식

`foo`라는 최상위 `Cross[FooModule]`이 버전을 순회하고, 그 안에 `bar`/`qux` 각각의 `jvm`/`js` 서브모듈을 둔다.

```scala
package build

import mill.*, scalalib.*, scalajslib.*, publish.*

object foo extends Cross[FooModule]("2.13.16", "3.3.6")
trait FooModule extends Cross.Module[String] {
  trait Shared extends CrossScalaModule, CrossValue, PlatformScalaModule, PublishModule {
    def publishVersion = "0.0.1"

    def pomSettings = PomSettings(
      description = "Hello",
      organization = "com.lihaoyi",
      url = "https://github.com/lihaoyi/example",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("lihaoyi", "example"),
      developers = Seq(Developer("lihaoyi", "Li Haoyi", "https://github.com/lihaoyi"))
    )

    def mvnDeps = Seq(mvn"com.lihaoyi::scalatags::0.13.1")
  }

  trait FooTestModule extends TestModule.Utest {
    def utestVersion = "0.8.9"
  }

  trait SharedJS extends Shared, ScalaJSModule {
    def scalaJSVersion = "1.20.2"
  }

  object bar extends Module {
    object jvm extends Shared {
      object test extends ScalaTests, FooTestModule
    }
    object js extends SharedJS {
      object test extends ScalaJSTests, FooTestModule
    }
  }

  object qux extends Module {
    object jvm extends Shared {
      def moduleDeps = Seq(bar.jvm)
      def mvnDeps = super.mvnDeps() ++ Seq(mvn"com.lihaoyi::upickle::4.3.0")

      object test extends ScalaTests, FooTestModule
    }

    object js extends SharedJS {
      def moduleDeps = Seq(bar.js)

      object test extends ScalaJSTests, FooTestModule
    }
  }
}
```

이 경우 태스크 경로는 `foo.<version>.<module>.<platform>` 형태가 되고, 발행되는 아티팩트는 버전×플랫폼×모듈 조합 전체(2×2×2=8개)다.

```
> ./mill foo.2_13_16.qux.jvm.run
Bar.value: <p>world Specific code for Scala 2.x</p>
Parsing JSON with ujson.read
Qux.main: Set(<p>i</p>, <p>cow</p>, <p>me</p>)

> ./mill __.publishLocal
Publishing com.lihaoyi:foo-bar_sjs1_2.13:0.0.1 to ivy repo...
Publishing com.lihaoyi:foo-bar_2.13:0.0.1 to ivy repo...
...
```

### 10.2 모듈별 개별 Cross 방식

`bar`, `qux` 각각이 자기만의 `Cross`를 갖는 대안 구조. 모듈마다 지원 Scala 버전 목록을 다르게 줄 수 있다는 점이 장점이다.

```scala
package build

import mill.*, scalalib.*, scalajslib.*, publish.*

trait Shared extends CrossScalaModule, PlatformScalaModule, PublishModule {
  def publishVersion = "0.0.1"
  def pomSettings = PomSettings(
    description = "Hello",
    organization = "com.lihaoyi",
    url = "https://github.com/lihaoyi/example",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("lihaoyi", "example"),
    developers = Seq(Developer("lihaoyi", "Li Haoyi", "https://github.com/lihaoyi"))
  )
  def mvnDeps = Seq(mvn"com.lihaoyi::scalatags::0.13.1")
}

trait SharedTestModule extends TestModule.Utest {
  def utestVersion = "0.8.9"
}

trait SharedJS extends Shared, ScalaJSModule {
  def scalaJSVersion = "1.20.2"
}

val scalaVersions = Seq("2.13.16", "3.3.6")

object bar extends Module {
  object jvm extends Cross[JvmModule](scalaVersions)
  trait JvmModule extends Shared {
    object test extends ScalaTests, SharedTestModule
  }

  object js extends Cross[JsModule](scalaVersions)
  trait JsModule extends SharedJS {
    object test extends ScalaJSTests, SharedTestModule
  }
}

object qux extends Module {
  object jvm extends Cross[JvmModule](scalaVersions)
  trait JvmModule extends Shared {
    def moduleDeps = Seq(bar.jvm())
    def mvnDeps = super.mvnDeps() ++ Seq(mvn"com.lihaoyi::upickle::4.3.0")

    object test extends ScalaTests, SharedTestModule
  }

  object js extends Cross[JsModule](scalaVersions)
  trait JsModule extends SharedJS {
    def moduleDeps = Seq(bar.js())

    object test extends ScalaJSTests, SharedTestModule
  }
}
```

이때 태스크 경로는 `<module>.<platform>.<version>` 순서가 되어 10.1과 순서가 바뀐다(`qux.js.3_3_6.run` 형태).

두 방식의 차이를 정리하면 다음과 같다.

| 항목 | 상위 Cross로 감싸기 | 모듈별 개별 Cross |
|---|---|---|
| 태스크 경로 | `foo.<version>.<module>.<platform>` | `<module>.<platform>.<version>` |
| 버전 목록 | 전체 모듈이 공유 | 모듈마다 다르게 지정 가능 |
| 적합한 상황 | 모든 모듈이 동일 버전 매트릭스 지원 | 모듈별 지원 버전이 다를 때 |

---

## 11. Scala.js WebAssembly 빌드

Scala.js 1.17+ 부터 지원하는 WASM 백엔드를 사용하는 예제. `scalaJSExperimentalUseWebAssembly`, `moduleKind`(ESModule), `moduleSplitStyle`(FewestModules)를 함께 설정해야 한다.

```scala
package build

import mill.*, scalalib.*, scalajslib.*
import mill.scalajslib.api.*

object wasm extends ScalaJSModule {
  override def scalaVersion = "3.8.2"

  override def scalaJSVersion = "1.20.2"

  override def moduleKind = ModuleKind.ESModule

  override def moduleSplitStyle = ModuleSplitStyle.FewestModules

  override def scalaJSExperimentalUseWebAssembly = true
}
```

빌드 결과물은 단일 WASM 모듈 + 로더 + `main.js`(엔트리포인트) 형태이며, 실행에는 Node.js 22 이상과 `--experimental-wasm-exnref` 플래그가 필요하다.

```
> node --experimental-wasm-exnref out/wasm/fastLinkJS.dest/main.js # mac/linux
hello  wasm!
```

---

## 12. Scala Native: 기본 구성

`ScalaNativeModule`은 `scalaVersion`에 더해 `scalaNativeVersion`을 지정해야 한다. `nativeLink` 태스크로 네이티브 바이너리를 빌드/링크할 수 있다.

```scala
package build

import mill.*, scalalib.*, scalanativelib.*

object `package` extends ScalaNativeModule {
  def scalaVersion = "3.3.6"
  def scalaNativeVersion = "0.5.8"

  // You can have arbitrary numbers of third-party dependencies
  def mvnDeps = Seq(
    mvn"com.lihaoyi::mainargs::0.7.8"
  )

  object test extends ScalaNativeTests, TestModule.Utest {
    def mvnDeps = Seq(mvn"com.lihaoyi::utest::0.8.9")
    def testFramework = "utest.runner.Framework"
  }
}
```

```
> ./mill run --text hello
<h1>hello</h1>

> ./mill show nativeLink  # Build and link native binary
".../out/nativeLink.dest/out"

> ./out/nativeLink.dest/out --text hello  # Run the executable
<h1>hello</h1>
```

---

## 13. Scala Native: C/C++ 코드 연동(interop)

Scala Native는 C/C++ 소스와 직접 링크할 수 있다. 빌드 설정 자체는 기본 구성과 거의 같지만, **C/C++ 소스 파일은 반드시 `resources/scala-native` 디렉터리 아래 위치**해야 정상적으로 링크·컴파일된다(Scala Native 공식 문서의 native code 연동 규칙).

```scala
package build

import mill.*, scalalib.*, scalanativelib.*

object `package` extends ScalaNativeModule {
  def scalaVersion = "3.3.6"
  def scalaNativeVersion = "0.5.8"

  object test extends ScalaNativeTests {
    def mvnDeps = Seq(mvn"com.lihaoyi::utest::0.8.9")
    def testFramework = "utest.runner.Framework"
  }

}
```

예상되는 프로젝트 레이아웃:

```
build.mill
src/
	foo/
	    HelloWorld.scala

resources/
    scala-native/
        HelloWorld.c

test/
    src/
        foo/
            HelloWorldTests.scala
```

```
> ./mill run
Running HelloWorld function
Done...
Reversed: !dlroW ,olleH

> ./mill test
Tests: 1, Passed: 1, Failed: 0
```

---

## 14. Scala Native: 멀티모듈 구성

두 개의 Scala Native 모듈(`foo`, `bar`)을 `moduleDeps`로 연결하는 예제. 공통 설정(scalaVersion, scalaNativeVersion, test 서브모듈)은 `trait MyModule`로 추출해 재사용한다. 최상위에서 `mill.Module`을 별도로 상속하지 않았으므로, 태스크 실행 시 모듈명을 접두사로 붙여야 한다(`foo.run`, `bar.run`).

```scala
package build

import mill.*, scalalib.*, scalanativelib.*

trait MyModule extends ScalaNativeModule {
  def scalaVersion = "3.3.6"
  def scalaNativeVersion = "0.5.8"

  object test extends ScalaNativeTests {
    def mvnDeps = Seq(mvn"com.lihaoyi::utest::0.8.9")
    def testFramework = "utest.runner.Framework"
  }
}

object foo extends MyModule {
  def moduleDeps = Seq(bar)

  def mvnDeps = Seq(mvn"com.lihaoyi::mainargs::0.7.8")
}

object bar extends MyModule
```

각 모듈은 자신만의 C/C++ 소스를 `resources/scala-native` 아래에 독립적으로 둘 수 있다.

```
build.mill
bar/
    resources/
        scala-native/
            bar.h
            HelloWorldBar.c
    src/
        Bar.scala
    test/
        src/
            BarTests.scala
foo/
    resources/
        scala-native/
            bar.h
            HelloWorldFoo.c
    src/
        Foo.scala
```

```
> ./mill bar.run hello
Running HelloWorld function
Done...
Bar value: Argument length is 5

> ./mill foo.run --bar-text hello --foo-text world
Foo.value: The vowel density of 'world' is 20
Bar.value: The string length of 'hello' is 5
```

---

## 15. Scala Native: 공통 설정 오버라이드

`ScalaNativeModule`에서 자주 오버라이드하는 설정 값들을 모아둔 예제.

| 태스크 | 기본 목적 | 예제 값 |
|---|---|---|
| `releaseMode` | 링크 최적화 수준 | `ReleaseMode.ReleaseFast` |
| `nativeIncrementalCompilation` | 증분 컴파일 활성화 여부 | `true` |
| `nativeLinkingOptions` | 링커에 전달할 추가 옵션(`-L` 등) | `Seq("-L" + moduleDir.toString + "/target")` |
| `nativeWorkdir` | Native 컴파일 작업 디렉터리 | `Task.dest / "newDir"` |

```scala
package build

import mill.*, scalalib.*, scalanativelib.*, scalanativelib.api.*

object `package` extends ScalaNativeModule {
  def scalaVersion = "3.3.6"
  def scalaNativeVersion = "0.5.8"

  // You can have arbitrary numbers of third-party dependencies
  // Scala Native uses double colon `::` between organization and the dependency names
  def mvnDeps = Seq(
    mvn"com.lihaoyi::fansi::0.5.0"
  )

  // Set the releaseMode to ReleaseFast.
  def releaseMode: T[ReleaseMode] = ReleaseMode.ReleaseFast

  // Set incremental compilation to true
  def nativeIncrementalCompilation: T[Boolean] = true

  // Set nativeLinkingOptions path to a directory named `target`.
  def nativeLinkingOptions = Seq("-L" + moduleDir.toString + "/target")

  // Set nativeWorkdir directory to `newDir`
  def nativeWorkdir = Task.dest / "newDir"
}
```

```
> ./mill show releaseMode
"ReleaseFast"

> ./mill show nativeIncrementalCompilation
true
```

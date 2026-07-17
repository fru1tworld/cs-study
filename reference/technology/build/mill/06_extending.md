# Mill 확장하기 (플러그인과 커스텀 로직)

> 원본: https://mill-build.org/mill/extending/import-mvn-plugins.html , https://mill-build.org/mill/extending/contrib-plugins.html , https://mill-build.org/mill/extending/thirdparty-plugins.html , https://mill-build.org/mill/extending/running-jvm-code.html , https://mill-build.org/mill/extending/writing-plugins.html , https://mill-build.org/mill/extending/meta-build.html

---

## 목차

1. [Maven 좌표로 라이브러리·플러그인 임포트](#1-maven-좌표로-라이브러리플러그인-임포트)
2. [Contrib 플러그인](#2-contrib-플러그인)
3. [서드파티 플러그인](#3-서드파티-플러그인)
4. [빌드 중 JVM 코드 실행하기](#4-빌드-중-jvm-코드-실행하기)
5. [커스텀 플러그인 작성법](#5-커스텀-플러그인-작성법)
6. [meta-build](#6-meta-build)

---

## 1. Maven 좌표로 라이브러리·플러그인 임포트

Mill은 `//| mvnDeps` 헤더 주석 하나로 `build.mill`에 임의의 JVM 라이브러리를 끌어와 빌드 타임에 사용할 수 있습니다. 플러그인 전용 메커니즘을 따로 배우지 않아도, 서드파티 라이브러리를 빌드 로직 안에서 바로 호출할 수 있다는 것이 핵심입니다.

### Java 라이브러리 임포트

아래는 Thymeleaf로 HTML 조각을 빌드 타임에 생성해 리소스로 포함시키는 예시입니다. 런타임에 템플릿 엔진을 돌리는 대신 빌드 타임에 미리 렌더링해 애플리케이션 시작 지연을 줄이는 용도로 쓸 수 있습니다.

```scala
//| mill-jvm-version: 17
//| mvnDeps:
//| - org.thymeleaf:thymeleaf:3.1.1.RELEASE
//| - org.slf4j:slf4j-nop:2.0.7

package build
import mill.*, javalib.*
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context

object foo extends JavaModule {
  def htmlSnippet = Task {
    val context = new Context
    context.setVariable("heading", "hello")
    context.setVariable("paragraph", "world")
    new TemplateEngine().process(
      """<div><h1 th:text="${heading}"></h1><p th:text="${paragraph}"></p></div>""",
      context
    )
  }

  def resources = Task {
    os.write(Task.dest / "snippet.txt", htmlSnippet())
    super.resources() ++ Seq(PathRef(Task.dest))
  }
}
```

저장소를 커스터마이징하려면 `//| repositories` 헤더로 추가 저장소를 지정합니다.

```
//| repositories:
//| - https://oss.sonatype.org/content/repositories/snapshots
//| mvnDeps:
//| - com.goyeau::mill-scalafix::0.5.1-14-4d3f5ea-SNAPSHOT
```

### Scala 라이브러리 임포트

Scala 라이브러리는 organization과 artifact 사이에 `::`(더블 콜론)를 사용해 Scala 바이너리 버전이 자동으로 붙도록 합니다.

```scala
//| mvnDeps: ["com.lihaoyi::scalatags:0.13.1"]

import mill.*, scalalib.*
import scalatags.Text.all.*

object bar extends ScalaModule {
  def scalaVersion = "2.13.16"

  def mvnDeps = Seq(mvn"com.lihaoyi::os-lib:0.10.7")
  def htmlSnippet = Task { div(h1("hello"), p("world")).toString }
  def resources = Task {
    os.write(Task.dest / "snippet.txt", htmlSnippet())
    super.resources() ++ Seq(PathRef(Task.dest))
  }
}
```

### 플러그인 임포트

Mill 플러그인 자체도 결국 일반 JVM 라이브러리이며, 동일하게 `//| mvnDeps`로 로드합니다. 다만 Mill 바이너리 버전과의 호환성을 위해 콜론 표기법이 조금 다릅니다.

| 표기 | 의미 |
|------|------|
| `<group>::<plugin>::<version>` | Scala 라이브러리 표기에 `_mill$MILL_BIN_PLATFORM` 접미사가 자동으로 붙음 |
| `<group>:::<plugin>::<version>` | 마찬가지로 플랫폼 접미사가 자동 확장됨(Scala 플랫폼 독립적 플러그인용) |

플레이스홀더:

- `$MILL_VERSION` — 현재 사용 중인 Mill 버전으로 치환됨(주로 contrib 모듈에서 사용)
- `$MILL_BIN_PLATFORM` — Mill 바이너리 플랫폼 식별자로 치환됨

```
//| mvnDeps: ["com.lihaoyi::mill-contrib-buildinfo:$MILL_VERSION"]
//| mvnDeps: ["de.tototec::de.tobiasroeser.mill.vcs.version_mill$MILL_BIN_PLATFORM:0.1.2"]
```

플러그인은 builtin 모듈, contrib 플러그인, 서드파티 플러그인 세 계층으로 나뉘며 각각 2, 3절에서 다룹니다.

---

## 2. Contrib 플러그인

Contrib 플러그인은 builtin 모듈과 서드파티 플러그인 사이의 중간 지점에 위치합니다. Mill 저장소 안에서 함께 관리되며 `mill-contrib-*` 네이밍으로 배포됩니다.

- Mill 자체 CI 프로세스 안에서 함께 테스트·릴리스되므로, 어떤 Mill 버전을 쓰든 호환되는 버전이 항상 존재합니다.
- 서드파티가 기여한 코드지만 builtin 모듈만큼의 바이너리 호환성 보장은 하지 않습니다.
- 버전 문자열에 `$MILL_VERSION` 리터럴을 쓰면 현재 Mill 버전으로 자동 치환됩니다.

### 임포트 방법

```
//| mvnDeps: ["com.lihaoyi::mill-contrib-[plugin-name]:$MILL_VERSION"]
```

그다음 필요한 클래스를 import합니다.

```scala
import mill.contrib.buildinfo.BuildInfo
```

### 공식 contrib 플러그인 목록

| 플러그인 | 용도 |
|----------|------|
| Artifactory | JFrog Artifactory에 아티팩트 배포 |
| BuildInfo | 빌드 시점 메타데이터(버전 등)를 담은 소스 코드 생성 |
| Codeartifact | AWS CodeArtifact 저장소 연동 |
| Dependency Check | OWASP Dependency-Check 기반 취약점 스캔 |
| Docker | Docker 이미지 빌드 |
| Flyway | Flyway 데이터베이스 마이그레이션 실행 |
| Gitlab | GitLab 패키지 레지스트리 배포 |
| JMH | Java Microbenchmark Harness 벤치마크 실행 |
| Play Framework | Play Framework 프로젝트 지원 |
| Proguard | ProGuard를 이용한 코드 축소·난독화 |
| ScalaPB | Protocol Buffers용 Scala 코드 생성 |
| Scoverage | Scoverage 기반 코드 커버리지 측정 |
| Software Bill of Materials (SBOM) | SBOM 생성 |
| Sonatype Central | Maven Central(Sonatype) 배포 |
| TestNG | TestNG 테스트 프레임워크 지원 |
| Twirl | Play의 Twirl 템플릿 컴파일 |
| Version file | 버전 정보 파일 생성 |

각 플러그인의 세부 옵션과 사용법은 Mill 공식 문서의 해당 플러그인 페이지를 참고합니다(이 노트의 범위 밖).

---

## 3. 서드파티 플러그인

Mill 저장소 밖에서 커뮤니티가 독립적으로 개발·유지하는 플러그인들입니다. 코드 생성, 배포, 테스트, 언어 지원 등 다양한 영역을 커버합니다. 아래는 대표적인 플러그인을 요약한 표입니다. 세부 사용법은 각 플러그인 저장소 문서를 참고합니다.

| 플러그인 | 용도 | 저장소 |
|----------|------|--------|
| Aliases | 빌드 전역 커맨드 별칭 정의(`__.test` 등) | carlosedp/mill-aliases |
| Antlr | ANTLR 문법 파일로부터 파서 자동 생성 | ml86/mill-antlr |
| AspectJ | AspectJ 컴파일러 통합, AOP 지원 | lefou/mill-aspectj |
| Bash Completion | Bash 셸 자동완성(제한적) | lefou/mill-bash-completion |
| Bundler | Scala.js용 NPM 의존성 관리·번들링(Webpack/Rollup) | nafg/mill-bundler |
| Daemon | systemd 데몬 런처, 빠른 재컴파일 지원 | swaldman/mill-daemon |
| DGraph | 브라우저에서 의존성 그래프 시각화 | ajrnz/mill-dgraph |
| Docker Jib Packager | Google Jib 기반, 데몬 없이 Docker 이미지 생성 | GeorgOfenbeck/mill-docker |
| Docker Native-Image Packager | GraalVM Native-Image 기반 컨테이너 빌드 | carlosedp/mill-docker-nativeimage |
| Docusaurus 2 | Docusaurus 2 기반 프로젝트 사이트 생성 | atooni/mill-docusaurus2 |
| Ensime | IDE용 `.ensime` 설정 파일 생성 | davoclavo/mill-ensime |
| Explicit Dependencies | 미사용/미선언 의존성 검출 | MarmaladeSky/mill-explicit-dependencies |
| Explicit Deps | `mvnDeps`/`mvnCompileDeps`와 실제 사용 코드 일치 검증 | kierendavies/mill-explicit-deps |
| Fish Completion | Fish 셸 자동완성(제한적) | ckipp01/mill-fish-completions |
| Giter8 | Giter8 템플릿 생성 결과 검증 테스트 | ckipp01/mill-giter8 |
| Git | Git 저장소 상태 기반 자동 버전 부여 | joan38/mill-git |
| GitHub Dependency Graph Submission | GitHub Dependency Submission API로 의존성 그래프 제출 | ckipp01/mill-github-dependency-graph |
| Header | 소스 파일 헤더 자동 추가·검증 | lewisjkl/header |
| Hepek | Scala 객체를 파일로 출력하는 정적 사이트 생성기 | sake92/mill-hepek |
| Integration Testing Mill Plugins | 여러 Mill 버전(0.6.x~0.11.x)에 대한 플러그인 호환성 테스트 | lefou/mill-integrationtest |
| JaCoCo | 코드 커버리지 수집·리포트 | lefou/mill-jacoco |
| JBake | JBake 기반 정적 사이트/블로그 생성 | lefou/mill-jbake |
| JBuildInfo | 빌드 메타데이터를 담은 Java 클래스 생성 | carueda/mill-jbuildinfo |
| Kotlin | Kotlin 컴파일러 통합(Mill 0.12부터 본체에 편입됨) | lefou/mill-kotlin |
| MDoc | 마크다운 안의 Scala 코드 스니펫 컴파일·실행 | atooni/mill-mdoc |
| millw / millw.bat | Mill 버전 자동 부트스트랩 래퍼(Mill 0.13/1.0부터 본체에 편입됨) | lefou/millw |
| MiMa | 이전 릴리스와의 바이너리 호환성 검사 | lolgab/mill-mima |
| Missinglink | 바이트코드 분석 기반 런타임 의존성 누락 검출 | hoangmaihuy/mill-missinglink |
| Native-Image | GraalVM Native-Image AOT 컴파일 실행 파일 생성 | alexarchambault/mill-native-image |
| OpenApi | OpenAPI/Swagger 스펙으로부터 소스 코드 생성(공식 도구) | OpenAPITools/openapi-generator |
| OpenApi4s | OpenAPI 스펙으로부터 Scala 코드 생성 | sake92/mill-openapi4s |
| OSGi | OSGi 규격 번들 생성 | lefou/mill-osgi |
| PowerShell Completion | PowerShell 셸 자동완성(기본적) | sake92/mill-powershell-completion |
| PublishM2 | 로컬 Maven 저장소 배포(Mill 0.6.1-27부터 `publishM2Local` 내장) | lefou/mill-publishM2 |
| Rust JNI | Rust 기반 JNI 라이브러리 빌드 | otavia-projects/mill-rust-jni |
| ScalablyTyped | Scala.js용 TypeScript 타입 정의 변환 | lolgab/mill-scalablytyped |
| Scala TSI | Scala case class로부터 TypeScript 인터페이스 생성 | hoangmaihuy/mill-scala-tsi |
| Scalafix | Scalafix 규칙 기반 린팅·자동 수정 | joan38/mill-scalafix |
| SCIP | Sourcegraph용 SCIP 코드 인덱스 생성 | ckipp01/mill-scip |
| Spring Boot | Spring Boot 실행 가능 jar 패키징 | lefou/mill-spring-boot |
| Squery | Squery SQL 라이브러리용 보일러플레이트 코드 생성 | sake92/mill-squery |
| Strict Deps | Bazel Strict Java Deps 방식의 의존성 엄격성 검사 | nguyenyou/mill-strict-deps |
| Universal Packager | sbt-native-packager 스타일의 범용 배포 아카이브 생성 | hoangmaihuy/mill-universal-packager |
| Zsh Completion | Zsh 셸 자동완성 | carlosedp/mill-zsh-completions |

더 많은 커뮤니티 플러그인은 GitHub의 [`mill-plugin` 토픽](https://github.com/topics/mill-plugin)에서 검색할 수 있습니다. 임포트 방법 자체는 1절(Maven 좌표로 라이브러리·플러그인 임포트)과 동일합니다.

예시로 몇 가지만 살펴보면 다음과 같습니다.

```scala
// Aliases
//| mvnDeps: ["com.carlosedp::mill-aliases::0.2.1"]
import mill.*, scalalib.*
import com.carlosedp.aliases.*

object MyAliases extends Aliases {
  def testall = alias("__.test")
}
```

```scala
// Scalafix
//| mvnDeps: ["com.goyeau::mill-scalafix:<latest version>"]
import com.goyeau.mill.scalafix.ScalafixModule

object project extends ScalaModule, ScalafixModule {
  def scalaVersion = "2.12.11"
}
// 실행: mill project.fix
```

```scala
// Native-Image
//| mvnDeps: ["io.github.alexarchambault.mill::mill-native-image::0.1.25"]
import io.github.alexarchambault.millnativeimage.NativeImage

object hello extends ScalaModule, NativeImage {
  def scalaVersion = "3.3.0"
  def nativeImageName = "hello"
  def nativeImageMainClass = "Main"
}
```

---

## 4. 빌드 중 JVM 코드 실행하기

`//| mvnDeps`는 편리하지만 한계가 있습니다.

1. 라이브러리가 빌드 시작 전에 전역적으로 resolve되어야 해서, 일부 모듈만 필요한 의존성인데도 불필요하게 다운로드될 수 있습니다.
2. 빌드 전체에서 해당 라이브러리의 버전이 하나로 고정됩니다. 모듈마다 다른 버전을 쓸 수 없습니다.
3. 라이브러리를 meta-build 없이는 "메인 빌드의 일부로" 직접 빌드할 수 없습니다.

이런 제약을 우회해야 할 때는 아래 네 가지 방식 중 하나로 JVM 코드를 직접 실행합니다.

### 4.1 서브프로세스: Jvm.callProcess

`defaultResolver().classpath()`로 Maven Central 의존성을 `PathRef` 목록으로 resolve한 뒤, 별도 프로세스에서 실행합니다.

```scala
def groovyClasspath: Task[Seq[PathRef]] = Task {
  defaultResolver().classpath(Seq(mvn"org.codehaus.groovy:groovy:3.0.9"))
}

def groovyGeneratedResources = Task {
  Jvm.callProcess(
    mainClass = "groovy.ui.GroovyMain",
    classPath = groovyClasspath().map(_.path).toSeq,
    mainArgs = Seq(groovyScript().path.toString, "Groovy!",
      (Task.dest / "groovy-generated.html").toString)
  )
  PathRef(Task.dest)
}
```

강력한 격리(별도 작업 디렉터리, 환경변수 설정 가능)를 제공하지만 프로세스 기동 오버헤드가 있습니다.

### 4.2 인프로세스 격리 클래스로더: Jvm.withClassLoader

의존성을 메모리 안의 클래스로더로 로드하고 리플렉션으로 메서드를 호출합니다.

```scala
def groovyGeneratedResources = Task {
  Jvm.withClassLoader(classPath = groovyClasspath().map(_.path).toSeq) { classLoader =>
    classLoader
      .loadClass("groovy.ui.GroovyMain")
      .getMethod("main", classOf[Array[String]])
      .invoke(null, Array[String](...))
  }
  PathRef(Task.dest)
}
```

`Jvm.callProcess`보다 시간·메모리 오버헤드가 훨씬 적지만, 격리 수준이 낮아 별도 작업 디렉터리나 환경변수를 지정할 수 없습니다.

### 4.3 클래스로더 워커 태스크: Task.Worker

장기 생존 클래스로더를 유지해 JVM 런타임 최적화(JIT 워밍업 등)의 이점을 누립니다.

```scala
def groovyWorker: Worker[java.net.URLClassLoader] = Task.Worker {
  Jvm.createClassLoader(groovyClasspath().map(_.path).toSeq)
}

def groovyGeneratedResources = Task {
  mill.api.ClassLoader.withContextClassLoader(groovyWorker()) {
    groovyWorker()
      .loadClass("groovy.ui.GroovyMain")
      .getMethod("main", classOf[Array[String]])
      .invoke(null, Array[String](...))
  }
  PathRef(Task.dest)
}
```

이 클래스로더는 싱글 스레드에서 초기화되지만 멀티 스레드 환경에서 동시에 사용될 수 있으므로, 동기화되지 않은 전역 가변 변수 없이 스레드 안전하게 작성해야 합니다.

### 4.4 프로젝트 내 모듈의 classpath를 클래스로더로 실행

프로젝트 안에서 빌드된 모듈의 코드를 클래스로더로 실행할 수도 있습니다.

```scala
def sources = Task {
  Jvm.withClassLoader(classPath = bar.runClasspath().map(_.path)) { classLoader =>
    classLoader
      .loadClass("bar.Bar")
      .getMethod("main", classOf[Array[String]])
      .invoke(null, Array(Task.dest.toString) ++ super.sources().map(_.path.toString))
  }
  Seq(PathRef(Task.dest))
}
```

### ScalaModule/JavaModule을 빌드 태스크로 실행하기

`runner().run()`을 사용하면 빌드된 모듈을 서브프로세스로 바로 실행할 수 있습니다.

```scala
def sources = Task {
  bar.runner().run(args = super.sources())
  Seq(PathRef(Task.dest))
}
```

`Runner` 트레이트의 시그니처는 다음과 같습니다.

```scala
trait Runner{
  def run(args: os.Shellable,
          mainClass: String = null,
          forkArgs: Seq[String] = null,
          forkEnv: Map[String, String] = null,
          workingDir: os.Path = null,
          useCpPassingJar: java.lang.Boolean = null)
         (using ctx: Ctx): Unit
}
```

서브프로세스의 작업 디렉터리는 기본적으로 호출한 태스크의 `Task.dest`로 설정됩니다.

### JVM 재사용과 캐싱

비용이 큰 상태 유지형(stateful) JVM 작업에는 `CachedFactory`를 사용해 클래스로더를 필요할 때 생성하고, 가능하면 캐시하며, 더 이상 필요 없을 때 정리합니다.

```scala
class BarWorker(runClasspath: Seq[os.Path])
    extends CachedFactory[Unit, URLClassLoader] {
  def setup(key: Unit) = {
    Jvm.createClassLoader(runClasspath)
  }
  def teardown(key: Unit, value: URLClassLoader) = {
    value.close()
  }
  def maxCacheSize = 2
}

def barWorker: Worker[BarWorker] = Task.Worker {
  new BarWorker(bar.runClasspath().map(_.path).toSeq)
}

def sources = Task {
  barWorker().withValue(()) { classLoader =>
    classLoader.loadClass("bar.Bar").getMethod("main", classOf[Array[String]])
      .invoke(null, Array(...))
  }
  Seq(PathRef(Task.dest))
}
```

이 패턴은 동시성 환경에서 가변 상태를 다룰 때 발생하는 스레드 안전성 문제를 클래스로더 생명주기 관리로 해결합니다.

---

## 5. 커스텀 플러그인 작성법

Mill 플러그인은 결국 Mill의 기능을 확장하는 일반 JVM 라이브러리입니다. 모듈이 상속할 수 있는 트레이트를 제공해 재사용 가능한 빌드 로직을 공유하는 것이 핵심 패턴입니다.

### 프로젝트 구성

플러그인 자체도 표준 JVM 프로젝트로 구성하며, `mill-libs`(Mill API 접근용)와 `mill-testkit`(테스트용)에 의존합니다.

```scala
object myplugin extends ScalaModule, PublishModule {
  def millVersion = "1.0.6"
  def scalaVersion = "3.7.4"
  def jvmVersion = "11"
  def platformSuffix = s"_mill1"
  def mvnDeps = Seq(mvn"com.lihaoyi::mill-libs:$millVersion")

  object test extends ScalaTests, TestModule.Utest {
    def mvnDeps = Seq(mvn"com.lihaoyi::mill-testkit:$millVersion")
  }
}
```

- `platformSuffix`는 플러그인이 타깃으로 하는 Mill 버전을 구분하는 데 사용됩니다.
- Mill 1.x는 하위 호환은 유지하지만 상위 호환은 보장하지 않으므로, 이전 버전 대상으로 만든 플러그인이 최신 Mill에서 그대로 동작하는 것과 달리 최신 버전 전용 플러그인이 과거 Mill에서 동작하지는 않습니다.

### 트레이트로 기능 구현하기

플러그인은 보통 모듈이 상속하는 트레이트를 제공합니다. 트레이트는 새 태스크를 추가하거나, 기존 태스크를 오버라이드(`super` 호출 포함)하거나, 구현해야 할 추상 태스크를 정의할 수 있습니다.

```scala
trait LineCountJavaModule extends mill.javalib.JavaModule {
  def lineCountResourceFileName: T[String]

  def lineCount = Task {
    allSourceFiles().map(f => os.read.lines(f.path).size).sum
  }

  override def resources = Task {
    os.write(Task.dest / lineCountResourceFileName(), "" + lineCount())
    super.resources() ++ Seq(PathRef(Task.dest))
  }
}
```

### 테스트 방법

Mill 플러그인 테스트는 세 단계로 나뉩니다.

#### 유닛 테스트

`TestRootModule`과 `UnitTester`로 인프로세스에서 실행합니다. 서브프로세스 기동을 검증하지는 않지만 대부분의 비즈니스 로직 검증에는 충분합니다.

```scala
object UnitTests extends TestSuite {
  def tests: Tests = Tests {
    test("unit") {
      object build extends TestRootModule with LineCountJavaModule {
        def lineCountResourceFileName = "line-count.txt"
        lazy val millDiscover = Discover[this.type]
      }

      val resourceFolder = os.Path(sys.env("MILL_TEST_RESOURCE_DIR"))
      UnitTester(build, resourceFolder / "unit-test-project").scoped { eval =>
        val Right(result) = eval(build.resources)
        assert(result.value.exists(pathref =>
          os.exists(pathref.path / "line-count.txt")
        ))
      }
    }
  }
}
```

태스크는 직접 참조하거나 문자열 셀렉터로 평가할 수 있습니다.

#### 통합 테스트

Mill을 서브프로세스로 실행해 전체 생명주기를 검증합니다.

```scala
object IntegrationTests extends TestSuite {
  def tests: Tests = Tests {
    test("integration") {
      val resourceFolder = os.Path(sys.env("MILL_TEST_RESOURCE_DIR"))
      val tester = new IntegrationTester(
        daemonMode = true,
        workspaceSourcePath = resourceFolder / "integration-test-project",
        millExecutable = os.Path(sys.env("MILL_EXECUTABLE_PATH"))
      )

      val res1 = tester.eval("run")
      assert(res1.isSuccess)
      assert(res1.out.contains("Line Count: 18"))
      assert(tester.out("lineCount").value[Int] == 18)
    }
  }
}
```

검증 가능한 항목:

- `.isSuccess` — 종료 코드 성공 여부
- `.out` — 표준 출력
- `.err` — 표준 에러
- `tester.out("…").text` / `.json` / `.value[T]` — 생성된 결과 값

빌드 설정에는 실행 가능한 Mill 런처를 함께 준비합니다.

```scala
object integration extends ScalaTests, TestModule.Utest {
  def mvnDeps = Seq(mvn"com.lihaoyi::mill-testkit:$millVersion")
  def forkEnv = Task {
    val repos = Seq(publishLocalTestRepo().path.toNIO.toUri.toASCIIString) ++
      Seq(Task.env.getOrElse("COURSIER_REPOSITORIES", "ivy2Local|central"))
    Map(
      "MILL_EXECUTABLE_PATH" -> millExecutable.assembly().path.toString,
      "COURSIER_REPOSITORIES" -> repos.mkString("|")
    )
  }

  object millExecutable extends JavaModule {
    def mvnDeps = Seq(mvn"com.lihaoyi:mill-runner-launcher_3:$millVersion")
    def mainClass = Some("mill.launcher.MillLauncherMain")
  }
}
```

#### 예제 테스트

`build.mill` 주석에 `/** Usage */` 블록으로 사용 예시와 테스트를 동시에 작성합니다. `>`로 시작하는 줄은 실행 명령, 그다음 줄들은 기대 출력이며 `…`는 불안정하거나 불필요한 출력 구간을 매칭하는 와일드카드입니다.

```scala
/** Usage

> ./mill run
Line Count: 18
...

> printf "\n" >> src/foo/Foo.java

> ./mill run
Line Count: 19
...

*/
```

```scala
object ExampleTests extends TestSuite {
  def tests: Tests = Tests {
    test("example") {
      val resourceFolder = os.Path(sys.env("MILL_TEST_RESOURCE_DIR"))
      ExampleTester.run(
        daemonMode = true,
        workspaceSourcePath = resourceFolder / "example-test-project",
        millExecutable = os.Path(sys.env("MILL_EXECUTABLE_PATH"))
      )
    }
  }
}
```

이 방식은 문서화와 테스트를 하나로 묶어, 사용자에게 실제 사용 패턴을 그대로 보여주면서 동시에 회귀를 검증합니다.

### 배포

플러그인은 `PublishModule`을 통해 일반 JVM 라이브러리와 동일하게 배포합니다.

```scala
def publishVersion = "0.0.1"

def pomSettings = PomSettings(
  description = "Line Count Mill Plugin",
  organization = "com.lihaoyi",
  url = "https://github.com/lihaoyi/myplugin",
  licenses = Seq(License.MIT),
  versionControl = VersionControl.github("lihaoyi", "myplugin"),
  developers = Seq(Developer("lihaoyi", "Li Haoyi", "https://github.com/lihaoyi"))
)
```

로컬에는 `publishLocal`로, Maven Central 등 외부 배포에는 일반 배포 절차를 그대로 사용합니다.

### 배포된 플러그인 사용하기

배포가 끝나면 다른 프로젝트의 `build.mill`에서 1절과 동일한 방식으로 임포트합니다.

```scala
//| mvnDeps:
//| - com.lihaoyi::myplugin::0.0.1

import mill.*, myplugin.*

object `package` extends LineCountJavaModule {
  def lineCountResourceFileName = "line-count.txt"
}
```

---

## 6. meta-build

### 개념

meta-build는 `build.mill` 파일 자체를 컴파일하는 빌드입니다. `mill-build/` 디렉터리에 위치하며, `MillBuildRootModule` 타입의 최상위 모듈을 포함해야 합니다. 별도 설정이 없으면 Mill이 자동으로 합성(synthetic) meta-build를 생성해 사용합니다.

meta-build는 재귀적입니다. 즉 meta-build 자신도 또 다른 nested meta-build를 가질 수 있고, 그 meta-build도 마찬가지입니다.

### `--meta-level`로 레벨 선택하기

| 값 | 의미 |
|----|------|
| `--meta-level 1` | 첫 번째 커스터마이즈된 meta-build 선택 |
| `--meta-level -1` | 가장 높은(bootstrap) meta-build 선택 |

`build.mill` 관련 문제를 디버깅할 때는 `--meta-level 1`로 태스크를 실행해보는 것이 좋은 접근법입니다.

```
./mill --meta-level 1 inspect millBuildRootModuleResult
./mill --meta-level 1 visualizePlan millBuildRootModuleResult
./mill --meta-level 1 show runClasspath
```

이 명령들은 `build.mill` 파일의 태스크 의존관계와 컴파일 파이프라인을 보여줍니다.

### 플러그인 업데이트 확인

meta-build의 `mvnDeps`로 선언된 Mill 플러그인들의 업데이트 여부를 다음 명령으로 확인할 수 있습니다.

```
$ mill --meta-level 1 mill.scalalib.Dependency/showUpdates
```

### 코드 공유 패턴 1: 계층 간 라이브러리 버전 공유

`build.mill`과 애플리케이션 코드 사이에서 의존성 버전을 공유할 수 있습니다. meta-build 파일(`mill-build/build.mill`)에서 `DepVersions` 객체를 생성해 소스로 노출합니다.

```scala
package build

import mill.*, scalalib.*
import mill.meta.MillBuildRootModule

object `package` extends MillBuildRootModule {
  val scalatagsVersion = "0.13.1"
  def mvnDeps = Seq(mvn"com.lihaoyi::scalatags:$scalatagsVersion")

  def generatedSources = Task {
    os.write(
      Task.dest / "DepVersions.scala",
      s"""
         |package millbuild
         |object DepVersions{
         |  def scalatagsVersion = "$scalatagsVersion"
         |}
      """.stripMargin
    )
    super.generatedSources() ++ Seq(PathRef(Task.dest))
  }
}
```

메인 `build.mill`에서는 이렇게 참조합니다.

```scala
def mvnDeps = Seq(
  mvn"com.lihaoyi::scalatags:${millbuild.DepVersions.scalatagsVersion}",
  mvn"com.lihaoyi::os-lib:0.10.7"
)
```

이 패턴을 쓰면 build.mill과 애플리케이션 코드 양쪽에서 버전 문자열을 중복 선언할 필요가 없습니다.

### 코드 공유 패턴 2: 계층 간 소스 코드 공유

애플리케이션 코드에서 `mill-build/src/` 안의 소스 파일을 직접 참조할 수도 있습니다. 메인 `build.mill`에서 다음처럼 설정합니다.

```scala
def customSources = Task.Sources("mill-build/src")
def sources = Task { super.sources() ++ customSources() }
```

공유 소스 파일 `mill-build/src/ScalaVersion.scala`:

```scala
package millbuild
object ScalaVersion {
  def myScalaVersion = "2.13.16"
}
```

애플리케이션 코드에서는 `millbuild.ScalaVersion.myScalaVersion`으로 사용합니다.

### 설계 원칙

Mill의 meta-build와 `mill-build` 디렉터리는 빌드 타임 로직과 런타임 로직 사이의 경계를 의도적으로 흐리게 만듭니다. 덕분에 로직을 한 번만 작성해 두고, 빌드 타임이든 런타임이든 더 효율적인 시점에 실행하도록 선택할 수 있습니다. 일반 `ScalaModule`이 제공하는 `scalacOptions`, `generatedSources` 등의 태스크도 meta-build 모듈에서 동일하게 커스터마이징할 수 있어, 복잡한 빌드 시나리오에도 충분한 유연성을 제공합니다.

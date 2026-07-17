# 대규모 빌드와 모노레포

> 원본: https://mill-build.org/mill/large/large.html , https://mill-build.org/mill/large/selective-execution.html , https://mill-build.org/mill/large/multi-file-builds.html , https://mill-build.org/mill/large/multi-language-builds.html , https://mill-build.org/mill/large/pre-compiled-modules.html

---

## 목차

1. [대규모 빌드 개요](#1-대규모-빌드-개요)
2. [선택적 실행 (Selective Execution)](#2-선택적-실행-selective-execution)
3. [멀티 파일 빌드 (Multi-File Builds)](#3-멀티-파일-빌드-multi-file-builds)
4. [멀티 언어 빌드 (Multi-Language Builds)](#4-멀티-언어-빌드-multi-language-builds)
5. [사전 컴파일 모듈 (Pre-Compiled Modules)](#5-사전-컴파일-모듈-pre-compiled-modules)

---

## 1. 대규모 빌드 개요

Mill은 단일 모듈짜리 소규모 프로젝트뿐 아니라 수백 개 모듈 규모의 대형 프로젝트에서도 효과적으로 동작하도록 설계되었다. Mill 자체 프로젝트(com-lihaoyi/mill)도 약 400개 모듈로 구성되어 있다.

모듈 수가 늘어나도 다음 특성 때문에 성능·리소스 사용량에 큰 영향을 주지 않는다.

- 모듈 자체는 "가벼운" 단위다. 모듈 개수가 늘어도 오버헤드가 선형적으로 커지지 않는다.
- 빌드 파일(`build.mill`, `package.mill` 등)은 변경된 파일만 증분(incremental) 재컴파일된다.
- 모듈은 지연 로딩(lazy loading)되어, 실제로 필요할 때만 로드·초기화된다.

이런 기본적인 확장성 위에, 대규모 코드베이스 관리를 돕는 다음 네 가지 기능을 추가로 제공한다.

| 기능 | 목적 |
|---|---|
| Selective Test Execution | 변경 영향을 받은 작업만 골라 실행해 CI 시간 단축 |
| Multi-File Builds | `build.mill` 하나에 몰아넣지 않고 여러 파일로 분할 |
| Multi-Language Builds | JVM 언어에 국한하지 않고 Python·TypeScript 등을 한 빌드에서 조합 |
| Pre-Compiled Modules | 반복되는 모듈 정의를 매번 컴파일하지 않고 재사용 |

---

## 2. 선택적 실행 (Selective Execution)

### 2.1 목적

모노레포에서 코드 변경 하나 때문에 전체 모듈의 테스트를 다 돌리면 CI 시간이 길어진다. Selective Execution은 **변경된 코드에 실제로 영향을 받는 태스크만** 골라 실행함으로써 이 문제를 완화한다.

### 2.2 핵심 명령어 3종

| 명령어 | 시점 | 동작 |
|---|---|---|
| `mill selective.prepare <selector>` | 코드 변경 **전** | 현재 코드베이스의 태스크 입력값·구현을 스냅샷으로 저장(`out/mill-selective-execution.json`) |
| `mill selective.run <selector>` | 코드 변경 **후** | prepare 시점 스냅샷과 비교해 영향받은 태스크만 `<selector>` 범위 내에서 실행 |
| `mill selective.resolve <selector>` | 코드 변경 후 | `selective.run`의 dry-run 버전. 실행하지 않고 영향받은 태스크 목록만 출력 |

전형적인 CI 사용 흐름은 다음과 같다.

```bash
git checkout main
./mill selective.prepare __.test
git checkout pull-request-branch
./mill selective.run __.test
```

### 2.3 진단용 보조 명령어

| 명령어 | 용도 |
|---|---|
| `selective.resolveChanged` | 어떤 입력(input) 태스크가 변경되어서 재실행 대상이 되었는지 표시 |
| `selective.resolveTree` | 선택된 태스크들을 JSON 트리 구조로 출력 |

### 2.4 selectiveInputs로 세밀하게 제어하기

기본적으로 태스크는 자신이 사용하는 모든 입력의 변경에 반응한다. `selectiveInputs` 파라미터를 주면 특정 입력이 바뀔 때만 재실행되도록 좁힐 수 있다.

```scala
def qux() = Task.Command(selectiveInputs = Seq(foo)) {
  println("Running qux")
  os.read(foo().path) + os.read(bar().path)
}
```

위 예에서 `qux`는 `foo()`와 `bar()`를 모두 읽어 사용하지만, `selectiveInputs = Seq(foo)`로 지정했기 때문에 `foo`가 변경될 때만 재실행 대상으로 잡히고 `bar`만 바뀌었을 때는 잡히지 않는다.

### 2.5 재현성(reproducibility) 요구사항

`prepare`와 `run` 사이의 비교가 정확하려면 다음 조건이 지켜져야 한다.

- Git 메타데이터처럼 매번 값이 바뀌는 동적 `Task.Input`을 사용하지 않아야 한다.
- 변경 전후 체크아웃이 동일한 파일시스템 경로(작업 디렉터리)를 사용해야 한다.
- 동일한 OS·파일시스템 환경이어야 한다.
- 파일 권한이 보존되어야 한다.

### 2.6 제한사항

- **세분성(granularity)이 Mill 태스크 단위**다. 즉 하나의 모듈 안에서 개별 테스트 케이스 단위까지 구분하지는 못한다.
- 통합 테스트처럼 애플리케이션 전체에 의존하는 태스크는 어떤 변경이든 매번 실행 대상이 된다.
- 런타임 캐싱(예: 태스크 결과의 실제 불변성 검사)보다는 보수적으로 동작한다. 즉 실제로는 영향이 없어도 "영향받았다"고 판단해 더 많이 실행하는 쪽으로 안전 마진을 둔다.

---

## 3. 멀티 파일 빌드 (Multi-File Builds)

### 3.1 배경

프로젝트가 커지면 하나의 `build.mill` 파일에 모든 모듈 정의를 몰아넣기 어려워진다. Mill은 각 하위 폴더에 `package.mill` 파일을 두어 빌드 정의를 여러 파일로 쪼갤 수 있게 한다. 빌드 로직이 관련 소스 코드와 같은 위치에 있으면 유지보수가 쉬워지고, 수정된 파일만 재컴파일되므로 컴파일 속도도 개선된다.

디렉터리 구조 비교:

```text
# 단일 파일 방식
build.mill
foo/src/...
bar/src/...
bar/qux/mymodule/src/...
```

```text
# Multi-File 방식
build.mill
foo/
    src/...
    package.mill
bar/
    package.mill
    qux/
        mymodule/src/...
        package.mill
```

### 3.2 기본 예제

```scala
// build.mill
package build

import mill.*, scalalib.*

trait MyModule extends ScalaModule {
  def scalaVersion = "3.7.4"
}
```

```scala
// foo/package.mill
package build.foo
import mill.*, scalalib.*

object `package` extends build.MyModule {
  def moduleDeps = Seq(build.bar.qux.mymodule)
  def mvnDeps = Seq(mvn"com.lihaoyi::mainargs:0.7.8")
}
```

```scala
// bar/package.mill
package build.bar
```

```scala
// bar/qux/package.mill
package build.bar.qux
import mill.*, scalalib.*

object mymodule extends build.MyModule {
  def mvnDeps = Seq(mvn"com.lihaoyi::scalatags:0.13.1")
}
```

실행 예:

```bash
> ./mill resolve __
bar
bar.qux.mymodule
bar.qux.mymodule.compile
foo
foo.compile

> ./mill bar.qux.mymodule.compile
> ./mill foo.compile
> ./mill foo.run --foo-text hello --bar-qux-text world
```

핵심 규칙:

- `package.mill` 안에서 `object` `package`로 선언한 모듈은 참조 시 `.package` 접미사 없이 경로만으로(`build.foo` 등) 참조한다. 다른 이름(예: `mymodule`)으로 선언한 모듈은 그 이름을 그대로 명시해서 참조해야 한다(`build.bar.qux.mymodule`).
- `package.mill` 파일은 루트 `build.mill`의 직속 하위 폴더, 혹은 이미 `package.mill`이 있는 폴더의 하위 폴더에서만 인식된다. 즉 폴더 트리를 따라 연속적으로 존재해야 한다.

### 3.3 헬퍼 파일 (Helper Files)

`package.mill`이나 `build.mill`이 아닌 임의의 `*.mill` 파일에 공통 로직을 정의해두고 여러 빌드 파일에서 가져다 쓸 수 있다.

```scala
// build.mill
package build
import mill.*, scalalib.*

object `package` extends MyModule {
  def forkEnv = Map(
    "MY_SCALA_VERSION" -> build.scalaVersion(),
    "MY_PROJECT_VERSION" -> foo.myProjectVersion
  )
}
```

```scala
// util.mill
package build
import mill.*, scalalib.*

def myScalaVersion = "2.13.16"

trait MyModule extends ScalaModule {
  def scalaVersion = myScalaVersion
}
```

```scala
// foo/package.mill
package build.foo
import mill.*, scalalib.*

object `package` extends build.MyModule {
  def forkEnv = Map(
    "MY_SCALA_VERSION" -> build.myScalaVersion,
    "MY_PROJECT_VERSION" -> myProjectVersion
  )
}
```

```scala
// foo/versions.mill
package build.foo

def myProjectVersion = "0.0.1"
```

참조 규칙: `build.mill`과 같은 폴더의 헬퍼 파일(`util.mill`)은 `build`로 참조하고, `foo/package.mill`과 같은 폴더의 헬퍼 파일(`foo/versions.mill`)은 `build.foo`로 참조한다.

```bash
> ./mill run
Main Env build.util.myScalaVersion: 2.13.16
Main Env build.foo.versions.myProjectVersion: 0.0.1
```

### 3.4 중첩 빌드 파일 (Nested Build Files)

기본적으로 `build.mill`은 프로젝트 루트에 하나만 있어야 한다. 하지만 파일 최상단에 `//| mill-allow-nested-build-mill: true` 헤더를 붙이면 하위 디렉터리에도 `build.mill`을 중첩해서 둘 수 있다. 이때는 파일이 실제로 위치한 경로와 무관하게 항상 `package build`를 선언한다(패키지 경로가 디렉터리 경로를 그대로 반영하지 않는다).

```scala
// build.mill (루트)
//| mill-allow-nested-build-mill: true

package build

import mill.*, scalalib.*

trait BarModule extends build.deps.foo.FooModule
```

```scala
// deps/package.mill
package build.deps
```

```scala
// deps/foo/build.mill  (중첩 build.mill)
package build

import mill.*, scalalib.*

object `package` extends FooModule
```

```scala
// deps/foo/module.mill
package build

import mill.*, scalalib.*
import build.myScalaVersion

trait FooModule extends ScalaModule {
  def scalaVersion = myScalaVersion
}
```

```scala
// deps/foo/versions.mill
package build

def myScalaVersion = "2.13.16"
```

상위 빌드에서는 중첩 build.mill이 정의한 모듈을 경로 기준으로 참조한다(`build.deps.foo.myScalaVersion` 등).

```bash
> ./mill show deps.foo.scalaVersion
"2.13.16"
```

제한사항: 중첩된 `build.mill` 파일 자체의 정의는 다른 곳에서 `import`할 수 없다. import 가능한 것은 같은 폴더의 헬퍼 파일(`module.mill`, `versions.mill` 등)이 선언한 정의뿐이다.

### 3.5 정리

Multi-File Builds가 주는 이점:

- 모듈별 빌드 설정을 해당 모듈의 소스 코드 옆에 위치시켜 유지보수성을 높인다.
- 수정된 파일만 독립적으로 재컴파일되어 컴파일 성능이 개선된다.
- 헬퍼 파일로 공통 로직을 여러 곳에서 재사용할 수 있다.
- 필요하면 nested `build.mill`로 하위 트리를 사실상 독립된 빌드처럼 구성할 수 있다.

---

## 4. 멀티 언어 빌드 (Multi-Language Builds)

Mill은 JVM 언어(Java, Scala, Kotlin)에 국한되지 않고 Python, TypeScript 등 다른 언어의 툴체인도 같은 빌드 안에서 구성할 수 있다. 예제는 다음 세 모듈을 하나의 애플리케이션으로 묶는다.

| 모듈 | 언어/모듈 타입 | 역할 |
|---|---|---|
| `client` | TypeScript / `ReactScriptsModule` | React 기반 사용자 인터페이스 |
| `sentiment-analysis` | Python / `PythonModule` | 텍스트 감정 분석 수행 |
| `server` | Java / `JavaModule` | Spring Boot 웹서버로 client 번들과 Python 바이너리를 서빙 |

```scala
object client extends ReactScriptsModule

object `sentiment-analysis` extends PythonModule {
  def mainScript = Task.Source("src/foo.py")
  def pythonDeps = Seq("textblob==0.19.0")
}

object server extends JavaModule {
  def resources = Task {
    os.copy(client.bundle().path, Task.dest / "static")
    os.copy(`sentiment-analysis`.bundle().path, Task.dest / "analysis")
  }
}
```

`server.resources`는 `client.bundle()`(빌드된 프론트엔드 산출물)과 `sentiment-analysis.bundle()`(패키징된 Python 산출물)을 가져와 자신의 리소스 디렉터리에 복사한다. 이 의존 관계 덕분에 언어가 다른 모듈끼리도 Mill의 태스크 그래프 안에서 하나로 엮인다.

멀티 언어 빌드에서 얻는 이점은 단일 언어 빌드와 동일하게 적용된다.

- **자동 캐싱·무효화**: TypeScript `client` 코드를 변경하면 그 결과를 리소스로 사용하는 Java `server`가 자동으로 재빌드된다.
- **병렬 빌드**: 서로 다른 언어의 툴체인이라도 독립적인 태스크라면 여러 코어에서 동시에 실행된다.

각 언어 모듈은 각자의 방식으로 테스트·실행한다.

```bash
./mill client.test
./mill sentiment-analysis.test
./mill server.test

curl -X POST http://localhost:8080/api/analysis
```

---

## 5. 사전 컴파일 모듈 (Pre-Compiled Modules)

### 5.1 문제 의식

대규모 코드베이스에서는 구조가 거의 동일한 모듈이 수십·수백 개 반복되는 경우가 흔하다. 이런 모듈들을 매번 `.mill` 스크립트로 정의하면 그때그때 컴파일해야 해서 비용이 커진다. Pre-Compiled Modules는 `mill.api.PrecompiledModule`을 상속하는 클래스를 **한 번만 컴파일**해두고, YAML 설정 파일에서 그 클래스를 참조해 여러 모듈 인스턴스를 만들어내는 기능이다.

이 클래스는 `mill-build/src/` 아래에 직접 작성하거나 Maven Central에 배포된 아티팩트에서 가져올 수 있다.

### 5.2 사전 컴파일 클래스 예시

```scala
// mill-build/src/LineCountJavaModule.scala
package millbuild
import mill.*, javalib.*

class LineCountJavaModule(val scriptConfig: mill.api.PrecompiledModule.Config)
    extends mill.javalib.JavaModule with mill.api.PrecompiledModule {

  override lazy val millDiscover = mill.api.Discover[this.type]

  object test extends JavaTests with mill.javalib.TestModule.Junit5

  /** Total number of lines in module source files */
  def lineCount: T[Int] = Task {
    allSourceFiles().map(f => os.read.lines(f.path).size).sum
  }

  /** Generate resources using lineCount of sources */
  override def resources: T[Seq[PathRef]] = Task {
    os.write(Task.dest / "line-count.txt", "" + lineCount())
    super.resources() ++ Seq(PathRef(Task.dest))
  }
}
```

이 `LineCountJavaModule`은 일반 `JavaModule`에 소스 라인 수를 세는 `lineCount` 태스크와, 그 값을 리소스 파일로 만들어 내는 `resources` 오버라이드를 추가한 것이다.

### 5.3 YAML에서 사용하기

```yaml
# foo/package.mill.yaml
extends: millbuild.LineCountJavaModule
mill-experimental-precompiled-module: true

object test:
  mvnDeps:
  - org.hamcrest:hamcrest:2.2
```

```yaml
# bar/package.mill.yaml
extends: millbuild.LineCountJavaModule
mill-experimental-precompiled-module: true
moduleDeps: !append [foo]

object test:
  mvnDeps:
  - org.hamcrest:hamcrest:2.2
  moduleDeps: !append [foo.test]
```

`bar`는 `moduleDeps: !append [foo]`로 `foo`에 의존하고, `bar.test`는 `!append [foo.test]`로 `foo.test`에 의존한다. `!append`는 상위(사전 컴파일 클래스가 기본 제공하는) 값에 항목을 이어 붙이는 YAML 태그다.

활성화 키는 `mill-experimental-precompiled-module: true`이며, 이 실험적 플래그를 켜야 YAML의 `extends`가 사전 컴파일 클래스를 가리키도록 동작한다.

### 5.4 동작 방식과 제약

- Mill은 YAML로부터 Scala 코드를 생성해 컴파일하는 것이 아니라, 이미 컴파일되어 있는 `PrecompiledModule` 서브클래스를 **런타임에 리플렉션으로 인스턴스화**한다. 이 때문에 모듈이 늘어나도 추가 컴파일 비용이 거의 들지 않는다.
- Pre-compiled 모듈은 resolve(모듈 그래프 해석) 이후에야 인스턴스화되므로, 일반 `.mill` 빌드 스크립트 코드에서는 이 모듈들을 직접 참조할 수 없다.

### 5.5 실행 예시

```bash
> ./mill foo.run
Hello, Foo!

> ./mill bar.run
Hello, Bar!, Hello, Qux!

> ./mill show foo.lineCount
11

> ./mill show bar.lineCount
18
```

Pre-Compiled Modules는 구조가 반복되는 모노레포에서, 매 모듈마다 빌드 스크립트를 새로 컴파일하지 않고도 일관된 모듈 정의를 빠르게 늘려나갈 수 있게 해주는 기능이다.

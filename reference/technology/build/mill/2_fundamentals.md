# Mill 기본: Task, Module, 의존성, Cross Build

## Mill의 Task, Module, out 디렉터리

> 원본: https://mill-build.org/mill/fundamentals/tasks.html, https://mill-build.org/mill/fundamentals/modules.html, https://mill-build.org/mill/fundamentals/out-dir.html

---

### 목차

1. [Task Graph 기본 개념](#1-task-graph-기본-개념)
2. [캐시되는 Task](#2-캐시되는-task)
3. [Task.Source / Task.Sources](#3-tasksource--tasksources)
4. [Task.Input](#4-taskinput)
5. [Task.Command](#5-taskcommand)
6. [Task.Persistent](#6-taskpersistent)
7. [Task.Worker와 CachedFactory](#7-taskworker와-cachedfactory)
8. [Task.Uncached와 Task.Anon](#8-taskuncached와-taskanon)
9. [Module 기본 구조](#9-module-기본-구조)
10. [trait으로 Module 재사용하기](#10-trait으로-module-재사용하기)
11. [moduleDir로 입력 경로 관리하기](#11-moduledir로-입력-경로-관리하기)
12. [override와 super](#12-override와-super)
13. [Root Module](#13-root-module)
14. [DefaultTaskModule, ExternalModule, ModuleRef](#14-defaulttaskmodule-externalmodule-moduleref)
15. [out 디렉터리 구조](#15-out-디렉터리-구조)
16. [빌드 진단용 JSON 파일들](#16-빌드-진단용-json-파일들)
17. [out 디렉터리 관리](#17-out-디렉터리-관리)

---

### 1. Task Graph 기본 개념

Mill 빌드는 결국 Task들의 그래프입니다. 각 Task는 다른 Task를 입력으로 받아 임의의 코드를 실행하고 결과를 반환하는 단위이며, 이 결과를 다른 Task가 다시 입력으로 사용합니다. Mill은 이 그래프를 위상 정렬해서 실행 순서를 정하고, 각 Task의 입력이 바뀌었는지 감지해서 필요한 부분만 다시 계산합니다.

Task 간 의존성은 괄호를 붙여 호출하는 문법(`foo()`)으로 선언합니다. 어떤 Task 정의 안에서 `otherTask()`를 호출하면, Mill은 이를 보고 자동으로 의존성 그래프의 엣지를 구성합니다. 의존성 중 하나라도 실패하면 그 Task에 의존하는 하위(다운스트림) Task들은 아예 실행되지 않습니다.

Task는 크게 다음 종류로 나뉩니다.

| 종류 | 캐싱 | 용도 |
|------|------|------|
| `Task { ... }` | 결과 캐시됨 | 소스 컴파일처럼 입력이 같으면 결과도 같은 순수 계산 |
| `Task.Source` / `Task.Sources` | 파일 해시로 추적 | 소스 파일, 리소스 폴더 등 외부 입력 |
| `Task.Input` | 매번 재평가 | git 커밋 해시, 환경변수 등 매번 확인이 필요한 외부 상태 |
| `Task.Command` | 캐시 안 됨 | `run`, `test`처럼 실행할 때마다 동작해야 하는 커맨드 |
| `Task.Persistent` | dest 폴더가 유지됨 | 증분 컴파일처럼 이전 실행 결과를 디스크에 남겨야 하는 작업 |
| `Task.Worker` | 메모리에 객체 유지 | 컴파일러 인스턴스 등 프로세스 생존 기간 동안 재사용할 무거운 객체 |
| `Task.Uncached` | 캐시 안 되고 watch도 안 됨 | 외부 서비스 호출처럼 부수효과 자체가 목적인 작업 |
| `Task.Anon` | 캐싱 대상 아님 | CLI에서 직접 실행할 수 없는, 여러 Task가 공유하는 헬퍼 로직 |

### 2. 캐시되는 Task

가장 기본적인 형태는 `Task { ... }`로 정의하는 캐시된 Task입니다.

```scala
def lineCount: T[Int] = Task {
  println("Computing line count")
  allSources()
    .map(p => os.read.lines(p.path).size)
    .sum
}
```

이 Task는 반환값이 uPickle로 JSON 직렬화 가능해야 합니다. Mill은 이 값을 `out/` 아래에 저장해 두고, 입력(위 예제라면 `allSources()`)이 바뀌지 않으면 다시 실행하지 않고 캐시된 값을 그대로 씁니다. 반대로 입력이 바뀌어 재실행했더라도 결과값의 해시가 이전과 같다면, 그 Task에 의존하는 다운스트림 Task는 다시 실행할 필요가 없다고 판단합니다. 즉 캐시 무효화는 값 기준이지 실행 여부 기준이 아닙니다.

### 3. Task.Source / Task.Sources

파일이나 폴더를 입력으로 삼는 Task는 `Task.Source`(단일 경로)와 `Task.Sources`(여러 경로)로 정의합니다.

```scala
def sources = Task.Source("src")
def resourceRoots = Task.Sources("resources", "resources2")
```

Mill은 지정된 경로 아래 파일들의 내용을 해시로 추적하다가, 파일이 추가/삭제/수정되면 그 해시가 달라졌다고 보고 이 Task에 의존하는 다운스트림 Task들을 다시 계산합니다.

### 4. Task.Input

`Task.Input`은 매번 무조건 재평가되는 Task입니다. git 커밋 해시, 시스템 프로퍼티, 환경변수처럼 "Mill이 파일 감시만으로는 변경을 알 수 없는" 외부 상태를 감지할 때 씁니다.

```scala
def myPropertyInput = Task.Input {
  sys.props("my-property")
}
```

Task.Input 자체는 매번 실행되지만, 이 결과를 입력으로 받는 캐시된 다운스트림 Task는 값이 실제로 바뀌었을 때만 재실행됩니다. 즉 재평가 비용은 Task.Input 하나에서 끝나고, 그 값이 그대로라면 이후 그래프는 캐시를 그대로 사용합니다.

### 5. Task.Command

`Task.Command`는 `run`, `test`처럼 호출할 때마다 실행되어야 하는 작업에 사용하며 결과를 캐싱하지 않습니다. MainArgs 라이브러리를 이용해 커맨드라인 인자를 위치 인자/명명 인자 형태로 그대로 받을 수 있습니다.

```scala
def run(mainClass: String, args: String*) = Task.Command {
  os.call(("java", "-cp", s"${classFiles().path}:${resources().path}", mainClass, args))
}
```

### 6. Task.Persistent

`Task.Persistent`는 `Task.dest` 폴더를 매 실행마다 초기화하지 않고 그대로 유지합니다. 증분 컴파일처럼 이전 실행 결과물을 디스크에 남겨 놓고 다음 실행에서 재사용해야 할 때 유용합니다. 다만 폴더 정리를 Mill이 대신 해 주지 않으므로, 파일 상태가 항상 일관되도록 만드는 책임은 해당 Task를 작성한 사람에게 있습니다.

### 7. Task.Worker와 CachedFactory

`Task.Worker`는 파일이 아니라 메모리에 객체를 유지하는 Task입니다. 컴파일러 인스턴스나 데몬 프로세스처럼 초기화 비용이 크고 여러 번의 실행에 걸쳐 재사용하고 싶은 객체에 적합합니다.

```scala
def counterWorker = Task.Worker {
  new java.util.concurrent.atomic.AtomicInteger(0)
}
```

Worker 객체가 여러 스레드에서 동시에 쓰일 수 있으므로 스레드 안전성을 직접 챙겨야 하며, `AutoCloseable`을 구현해 두면 Mill이 자원 정리 시점에 `close()`를 호출해 줍니다.

여러 개의 장기 상태를 setup/teardown 패턴으로 관리해야 할 때는 `CachedFactory`를 헬퍼로 사용할 수 있습니다. 최대 캐시 크기를 지정해 두면 그 크기를 넘길 때 오래된 항목부터 정리됩니다.

### 8. Task.Uncached와 Task.Anon

`Task.Uncached`는 매번 실행되고 캐시에서 값을 읽지 않는 Task로, 컨테이너 레지스트리 접근처럼 외부 시스템과의 상호작용 자체가 목적일 때 씁니다. `--watch` 모드에서 파일 변경 트리거로 실행되지도 않습니다.

`Task.Anon`은 CLI에서 직접 실행할 수 없는 익명 Task로, 여러 Task가 공통으로 쓰는 로직을 재사용하기 위한 헬퍼입니다. 결과를 직렬화하거나 캐싱할 필요가 없으므로 반환 타입 제약도 없습니다.

### 9. Module 기본 구조

Module은 두 가지 역할을 합니다. 하나는 Task들을 논리적으로 묶는 네임스페이스이고, 다른 하나는 trait을 통해 Task 집합을 복제·커스터마이징할 수 있는 재사용 템플릿입니다.

```scala
object foo extends Module {
  def bar = Task { "hello" }
  object qux extends Module {
    def baz = Task { "world" }
  }
}
```

이렇게 정의하면 `mill foo.bar`, `mill foo.qux.baz`로 각 Task를 실행할 수 있고, 결과 메타데이터도 모듈 계층을 그대로 반영해 `out/foo/bar.json`, `out/foo/qux/baz.json` 같은 경로에 저장됩니다. 즉 Scala 코드상의 중첩 구조, CLI 호출 경로, out 디렉터리 경로 세 가지가 항상 일치합니다.

### 10. trait으로 Module 재사용하기

여러 모듈에 공통 Task 집합을 적용하고 싶으면 class가 아니라 trait으로 정의해야 합니다.

```scala
trait FooModule extends Module {
  def bar: T[String]           // 하위에서 반드시 구현해야 하는 추상 Task
  def qux = Task { bar() + " world" }
}

object foo1 extends FooModule {
  def bar = "hello"
  def qux = super.qux().toUpperCase   // 상위 구현을 override
}

object foo2 extends FooModule {
  def bar = "hi"
  def baz = Task { qux() + " I am Cow" }   // 새 Task 추가
}
```

`foo1`은 `qux`를 override해서 대문자로 바꾸고, `foo2`는 `qux`는 그대로 쓰면서 `baz`라는 새 Task를 얹었습니다. 실제 JavaModule, ScalaModule 같은 Mill 내장 모듈들도 이런 방식의 trait 조합으로 구성되어 있습니다.

### 11. moduleDir로 입력 경로 관리하기

각 Module은 입력 파일이 위치할 기준 경로를 `moduleDir` 필드로 갖습니다. `Task.Source`, `Task.Sources`에 상대 경로를 넘기면 이 `moduleDir` 기준으로 해석됩니다.

```scala
trait MyModule extends Module {
  def sources = Task.Source("sources")
}

object outer extends MyModule {
  object inner extends MyModule
}
```

- `outer`의 `moduleDir`은 `outer/`
- `outer.inner`의 `moduleDir`은 `outer/inner/`

기본적으로 모듈 계층 경로를 그대로 따라가지만, 필요하면 override로 바꿀 수 있습니다.

```scala
object outer2 extends MyModule {
  def moduleDir = super.moduleDir / "nested"   // outer2/nested/ 로 변경
  object inner extends MyModule
}
```

주의할 점은 `moduleDir`은 어디까지나 입력 파일의 기준 경로일 뿐이고, 출력은 항상 `out/` 아래 모듈 계층 경로에 고정되며 바꿀 수 없다는 것입니다. 모듈을 다른 위치로 옮기거나 감쌀 때 소스 경로가 깨지지 않도록 `moduleDir`을 신경 써서 다뤄야 합니다.

### 12. override와 super

Task를 override할 때는 일반 Scala 메서드처럼 `super`로 상위 구현의 결과에 접근할 수 있습니다.

```scala
trait Foo extends Module {
  def sourceRoots = Task.Sources("src")
}

trait Bar extends Foo {
  def additionalSources = Task.Sources("src2")
  def sourceRoots = Task { super.sourceRoots() ++ additionalSources() }
}
```

`Bar`는 `Foo`가 정의한 `src` 경로에 `src2`를 덧붙이는 식으로 확장했습니다. `moduleDeps`처럼 Task가 아니라 일반 메서드로 선언되는 값들도 이 방식으로 override해서 모듈 그래프의 형태(의존 모듈 목록 등)를 바꿀 수 있습니다.

### 13. Root Module

빌드 파일의 최상위 스코프에 직접 Task나 서브 모듈을 두고 싶다면, `` `package` `` 라는 이름으로 Root Module을 선언합니다.

```scala
object `package` extends JavaModule {
  def mvnDeps = Seq(
    mvn"net.sourceforge.argparse4j:argparse4j:0.9.0"
  )

  object test extends JavaTests, TestModule.Junit4 {
    def mvnDeps = Seq(mvn"com.google.guava:guava:33.3.0-jre")
  }
}
```

이 경우 프로젝트 레이아웃은 다음과 같습니다.

```
build.mill
src/              # 루트 모듈의 소스
resources/
test/
    src/
out/
```

Root Module의 이름은 반드시 `package`여야 하고, 파일 안의 다른 모듈이나 Task는 모두 이 Root Module 내부에 정의해야 합니다.

### 14. DefaultTaskModule, ExternalModule, ModuleRef

**DefaultTaskModule**: 모듈이 `DefaultTaskModule`을 상속하면 Task 이름을 생략하고 모듈 이름만으로 기본 Task를 실행할 수 있습니다.

```scala
object foo extends DefaultTaskModule {
  override def defaultTask() = "bar"
  def bar() = Task.Command { println("Hello Bar") }
}
```

`mill foo`라고만 호출해도 `mill foo.bar`와 동일하게 동작합니다.

**하이픈이 포함된 이름**: 모듈이나 Task 이름에 하이픈을 쓰고 싶으면 백틱으로 감싸 선언합니다.

```scala
object `hyphenated-module` extends Module {
  def `hyphenated-task` = Task { println("task") }
}
```

CLI에서 호출할 때는 `mill hyphenated-module.hyphenated-task`처럼 백틱 없이 그대로 씁니다.

**ExternalModule**: 라이브러리가 여러 프로젝트에 공유 제공하는 모듈은 `ExternalModule`로 정의합니다.

```scala
object Bar extends mill.api.ExternalModule {
  def baz = Task { 1 }
  lazy val millDiscover = Discover[this.type]
}
```

`mill foo.Bar/baz`처럼 슬래시 표기로 호출하며, 자주 쓰는 것이면 `def myAutoformat = mill.javalib.palantirformat.PalantirFormatModule`처럼 별칭을 만들어 짧게 쓸 수도 있습니다.

**ModuleRef**: 추상 모듈이 다른 구체 모듈을 참조해야 할 때는 일반 참조 대신 `ModuleRef`를 씁니다.

```scala
trait MyTestModule extends JavaModule {
  def upstreamModule: ModuleRef[JavaModule]
  def moduleDeps = Seq(upstreamModule())
}

object footest extends MyTestModule {
  def upstreamModule = ModuleRef(foo)
}
```

`ModuleRef`로 감싼 참조는 Mill의 Task 쿼리 해석(예: `mill resolve __.compile` 같은 와일드카드 조회) 대상에서 제외되어, 같은 모듈이 여러 경로에서 중복으로 잡히는 문제를 막아줍니다.

### 15. out 디렉터리 구조

Mill은 모든 빌드 산출물과 캐시 메타데이터를 프로젝트 루트의 `out/` 폴더 아래에 모듈 계층과 동일한 구조로 저장합니다. 예를 들어 `foo.compile`이라는 Task를 실행하면 다음 파일들이 생깁니다.

| 경로 | 내용 |
|------|------|
| `out/foo/compile.json` | 캐시 키와 JSON 직렬화된 반환값. 반환값에 `PathRef`가 포함되어 있으면 `.dest/` 안의 실제 바이너리 파일을 가리키는 참조가 들어있음 |
| `out/foo/compile.dest/` | 해당 Task 전용 스크래치 폴더. Task는 이 폴더 안에만 파일을 생성해야 함 |
| `out/foo/compile.log` | 해당 Task 실행 중 발생한 stdout/stderr. 여러 Task가 병렬 실행되어 콘솔 출력이 뒤섞일 때 개별 로그를 확인하는 용도 |
| `out/foo/compile.super/` | `super.foo()` 같은 방식으로 override된 원래 구현을 호출했을 때 그 원래 구현의 메타데이터가 저장되는 곳 |

`mill show foo.compile` 명령으로 `compile.json`에 저장된 값을 바로 확인할 수 있습니다.

### 16. 빌드 진단용 JSON 파일들

`out/` 바로 아래에는 Task별 파일 외에 빌드 전체를 진단하기 위한 시스템 파일들도 함께 생성됩니다.

| 파일 | 용도 |
|------|------|
| `mill-profile.json` | 마지막 실행에서 수행된 각 Task의 레이블, 소요 시간(ms), 캐시 적중 여부, 값 해시 변경 여부, 의존 Task 목록을 기록 |
| `mill-chrome-profile.json` | `chrome://tracing`에서 열어 Task 실행의 순차/병렬 타임라인을 시각적으로 확인할 수 있는 프로파일 |
| `mill-dependency-tree.json` | 지정한 Task를 루트로 삼아 그 상위 의존 Task들을 트리로 표현. 특정 Task가 왜 실행 대상에 포함됐는지 추적할 때 사용 |
| `mill-invalidation-tree.json` | 마지막 실행에서 무효화된 입력과, 그로 인해 연쇄적으로 무효화된 하위 Task들의 스패닝 트리 |
| `mill-runner-state.json` | Mill 자체의 부트스트랩 메타데이터(워커, 감시 대상 파일, 클래스패스 등) |
| `mill-console-tail` | 실시간 로그 스트림. 외부 스크립트에서 Mill 실행 로그를 따라가며 확인할 때 활용 |
| `mill-daemon/` | 장기 실행 서버 인스턴스 관리용 임시 파일 |

`mill-dependency-tree.json`은 예를 들어 다음과 같은 관계를 보여줍니다.

```
foo.compile → foo.compileClasspath → foo.resolvedMvnDeps
           → foo.allSourceFiles → foo.sources
```

`mill-invalidation-tree.json`은 소스 파일이 바뀌었을 때 이런 식으로 무효화 경로를 나타냅니다.

```
foo.sources → foo.allSources → foo.allSourceFiles → foo.compile
```

`build.mill` 자체를 수정한 경우에는 변경된 메서드 단위로 `def`, `call` 노드가 표시됩니다. 이 무효화 분석은 보수적으로 동작하므로, 실제 실행 결과에는 영향이 없는 코드를 고쳐도 그 메서드를 호출하는 Task들이 전부 무효화 대상으로 잡힐 수 있습니다.

### 17. out 디렉터리 관리

`out/` 아래 파일을 손으로 정리할 때는 반드시 `.dest/` 폴더와 대응하는 `.json` 파일을 함께 지워야 합니다. `foo.dest/`만 지우고 `foo.json`을 남겨두면 캐시 메타데이터와 실제 파일 상태가 어긋나 진단하기 어려운 문제가 생길 수 있습니다. 개별 폴더를 직접 건드리기보다는 Mill이 제공하는 `clean` 명령을 쓰는 편이 안전합니다.

`out/` 디렉터리 위치는 `MILL_OUTPUT_DIR` 환경변수로 바꿀 수 있습니다.

```
MILL_OUTPUT_DIR=build-stuff/working-dir ./mill foo.printDest
```

빌드 산출물을 더 빠른 디스크나 별도의 쓰기 가능한 파일시스템에 두고 싶을 때 이 옵션을 활용할 수 있습니다.

---

## Mill 라이브러리 의존성, Cross Build, 번들 라이브러리, JVM 버전 설정

> 원본: https://mill-build.org/mill/fundamentals/library-deps.html, https://mill-build.org/mill/fundamentals/cross-builds.html, https://mill-build.org/mill/fundamentals/bundled-libraries.html, https://mill-build.org/mill/fundamentals/configuring-jvm-versions.html

---

### 목차

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

### 1. 라이브러리 의존성 기본

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

### 2. 의존성 종류: mvnDeps / compileMvnDeps / runMvnDeps

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

### 3. 전이 의존성 확인

#### 의존성 트리

```bash
mill myModule.showMvnDepsTree
mill __.showMvnDepsTree
```

출력에서 화살표(`->`)는 버전이 변경되었음을, `(possible incompatibility)` 주석은 잠재적 충돌 가능성을 나타낸다.

#### 해결된 의존성 조회

```bash
mill show myModule.resolvedMvnDeps
mill myModule.resolvedMvnDeps
```

결과는 `out/myModule/resolvedMvnDeps.json`에 저장된다.

#### 의존성 출처 추적

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

### 4. 전이 의존성 제외와 버전 강제

#### exclude

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

#### forceVersion

특정 의존성의 버전을 강제로 고정한다.

```scala
val deps = Seq(mvn"com.lihaoyi::fansi:0.2.14".forceVersion())
```

예를 들어 pprint 0.8.1이 전이적으로 fansi 0.4.0을 요구해도, `forceVersion()`을 적용한 fansi 0.2.14가 최종적으로 사용된다.

---

### 5. 의존성 관리(BOM)

의존성 관리는 버전 강제와 exclude를 한꺼번에 적용하는 방식이다. 관리 대상 의존성을 직접 선언하지 않아도, 전이적으로 나타나기만 하면 지정한 버전이 강제된다.

#### 외부 BOM 사용

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

#### depManagement로 직접 관리

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

#### BOM 모듈 생성

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

#### BOM 모듈 발행

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

### 6. 의존성 업데이트 검색

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

### 7. Cross Build 기본

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

### 8. Cross 모듈 참조와 다중 축

#### 외부 태스크에서 참조

`foo("2.10")` 형태로 특정 cross 값의 인스턴스에 접근한다.

```scala
object foo extends Cross[FooModule]("2.10", "2.11", "2.12")
trait FooModule extends Cross.Module[String] {
  def suffix = Task { "_" + crossValue }
}

def bar = Task { s"hello ${foo("2.10").suffix()}" }
def qux = Task { s"hello ${foo("2.10").suffix()} world ${foo("2.12").suffix()}" }
```

#### Cross 모듈 간 참조

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

#### 내부 Cross 모듈 (CrossValue)

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

#### Cross Resolver로 단축 문법 사용

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

#### 다중 축(Multiple Cross Axes)

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

### 9. 동적 Cross 모듈

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

#### 실무 예제: 정적 블로그 빌드

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

### 10. Mill에 번들된 라이브러리

Mill은 빌드 도구 내부적으로 여러 오픈소스 라이브러리를 사용하며, 이들은 빌드 스크립트(`build.mill`)에서 직접 import 없이 바로 쓸 수 있다.

| 라이브러리 | 용도 |
|---|---|
| **OS-Lib** | 파일시스템·서브프로세스 연산 |
| **uPickle** | 태스크 출력의 JSON 직렬화(캐싱용) |
| **Requests-Scala** | HTTP 요청을 통한 파일 다운로드 |
| **MainArgs** | 커맨드라인 인자 파싱 |
| **Coursier** | JVM 아티팩트(의존성) 해결 |

#### OS-Lib

Mill이 파일 및 프로세스 연산에 전반적으로 사용하는 라이브러리다. 각 태스크는 격리된 작업 디렉터리(`Task.dest`)에서 실행되어 태스크 간 파일 간섭이 발생하지 않는다.

```scala
def task1 = Task {
  os.write(os.pwd / "file.txt", "hello")
  PathRef(os.pwd / "file.txt")
}
```

#### uPickle

모든 Mill 태스크의 결과값은 uPickle을 거쳐 JSON으로 저장되고, 이 값을 기준으로 캐시 무효화 여부를 판단한다. 기본 타입, 컬렉션, `Path`, `PathRef` 등을 직렬화 대상으로 지원한다.

```scala
def taskMap = Task { Map("int" -> "123", "boolean" -> "true") }
// 출력: {"int": "123", "boolean": "true"}
```

#### Requests-Scala

컴파일러, 소스코드, 데이터 파일 등을 원격에서 내려받을 때 쓴다.

```scala
def remoteFile = Task {
  os.write(Task.dest / "file.zip",
    requests.get("https://example.com/file.zip"))
}
```

#### MainArgs

`Task.Command`의 매개변수로 문자열, 정수, 불린 등 기본 타입을 그대로 받을 수 있게 해준다.

```scala
def command(str: String, i: Int, bool: Boolean = true) = Task.Command {
  println(s"$str $i $bool")
}
```

#### Coursier

Scala, Java 등 JVM 생태계의 서드파티 의존성 해결을 담당하는 엔진으로, [1. 라이브러리 의존성 기본](#1-라이브러리-의존성-기본)에서 다룬 `mvnDeps` 해석의 실질적인 구현체다.

각 라이브러리의 상세 API는 해당 프로젝트의 ScalaDoc과 저장소 페이지에서 확인한다.

---

### 11. JVM 버전 설정

Mill은 기본적으로 자체 실행 중인 JVM(예: `zulu:25`)을 Java/Scala/Kotlin 모듈 컴파일에 사용한다. 필요한 JVM은 자동으로 다운로드하고 캐싱하므로, 별도의 전역 JDK 설치 없이도 원하는 버전으로 빌드할 수 있다.

#### Mill 자체가 쓰는 JVM 버전 지정

선언적 설정(`build.mill.yaml`):

```yaml
mill-jvm-version: 19
```

프로그래머블 설정(`build.mill` 헤더 주석):

```
//| mill-jvm-version: 19
```

버전 문자열은 `"{version}"` 또는 `"{name}:{version}"` 형식을 쓴다. 예: `temurin:11.0.21`, `zulu:25`.

#### 모듈별 JVM 버전 지정

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

#### JVM 지정 방법 정리

| 방법 | 예시 |
|---|---|
| 표준 배포판(distribution) 이름 | `temurin:11.0.21`, `zulu:25` |
| JVM Index 버전 지정 | `jvmIndexVersion: latest.release` |
| 명시적 다운로드 URL | `https://github.com/adoptium/.../OpenJDK22U-jdk_x64_linux_hotspot_22.0.2_9.tar.gz` |
| 로컬 JVM 경로 | `javaHome: /my/java/home` |

사용 가능한 배포판 목록은 [coursier/jvm-index](https://github.com/coursier/jvm-index)에서 확인한다. 다운로드 URL을 직접 지정하는 경우 OS/아키텍처별 분기 처리가 필요하다.

#### JVM 옵션 전달

`javacOptions`에 `-J` 접두사를 붙이면 JVM 자체에 옵션을 전달할 수 있다.

```yaml
javacOptions:
  - "-J-Xss8m"
```

#### 모듈에 연결된 JVM 명령 직접 실행

```bash
./mill foo.java -version
./mill foo.javap
./mill foo.java -jar out/foo/assembly.dest/out.jar
```

#### 버전 선택 가이드

- **라이브러리**: 최대한 낮은(하지만 지원 대상인) 버전, 예를 들어 17을 타깃으로 컴파일해 호환 범위를 넓힌다.
- **애플리케이션**: 최신 버전을 사용해 성능·보안 개선을 그대로 누린다.

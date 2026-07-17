# Mill의 Task, Module, out 디렉터리

> 원본: https://mill-build.org/mill/fundamentals/tasks.html, https://mill-build.org/mill/fundamentals/modules.html, https://mill-build.org/mill/fundamentals/out-dir.html

---

## 목차

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

## 1. Task Graph 기본 개념

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

## 2. 캐시되는 Task

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

## 3. Task.Source / Task.Sources

파일이나 폴더를 입력으로 삼는 Task는 `Task.Source`(단일 경로)와 `Task.Sources`(여러 경로)로 정의합니다.

```scala
def sources = Task.Source("src")
def resourceRoots = Task.Sources("resources", "resources2")
```

Mill은 지정된 경로 아래 파일들의 내용을 해시로 추적하다가, 파일이 추가/삭제/수정되면 그 해시가 달라졌다고 보고 이 Task에 의존하는 다운스트림 Task들을 다시 계산합니다.

## 4. Task.Input

`Task.Input`은 매번 무조건 재평가되는 Task입니다. git 커밋 해시, 시스템 프로퍼티, 환경변수처럼 "Mill이 파일 감시만으로는 변경을 알 수 없는" 외부 상태를 감지할 때 씁니다.

```scala
def myPropertyInput = Task.Input {
  sys.props("my-property")
}
```

Task.Input 자체는 매번 실행되지만, 이 결과를 입력으로 받는 캐시된 다운스트림 Task는 값이 실제로 바뀌었을 때만 재실행됩니다. 즉 재평가 비용은 Task.Input 하나에서 끝나고, 그 값이 그대로라면 이후 그래프는 캐시를 그대로 사용합니다.

## 5. Task.Command

`Task.Command`는 `run`, `test`처럼 호출할 때마다 실행되어야 하는 작업에 사용하며 결과를 캐싱하지 않습니다. MainArgs 라이브러리를 이용해 커맨드라인 인자를 위치 인자/명명 인자 형태로 그대로 받을 수 있습니다.

```scala
def run(mainClass: String, args: String*) = Task.Command {
  os.call(("java", "-cp", s"${classFiles().path}:${resources().path}", mainClass, args))
}
```

## 6. Task.Persistent

`Task.Persistent`는 `Task.dest` 폴더를 매 실행마다 초기화하지 않고 그대로 유지합니다. 증분 컴파일처럼 이전 실행 결과물을 디스크에 남겨 놓고 다음 실행에서 재사용해야 할 때 유용합니다. 다만 폴더 정리를 Mill이 대신 해 주지 않으므로, 파일 상태가 항상 일관되도록 만드는 책임은 해당 Task를 작성한 사람에게 있습니다.

## 7. Task.Worker와 CachedFactory

`Task.Worker`는 파일이 아니라 메모리에 객체를 유지하는 Task입니다. 컴파일러 인스턴스나 데몬 프로세스처럼 초기화 비용이 크고 여러 번의 실행에 걸쳐 재사용하고 싶은 객체에 적합합니다.

```scala
def counterWorker = Task.Worker {
  new java.util.concurrent.atomic.AtomicInteger(0)
}
```

Worker 객체가 여러 스레드에서 동시에 쓰일 수 있으므로 스레드 안전성을 직접 챙겨야 하며, `AutoCloseable`을 구현해 두면 Mill이 자원 정리 시점에 `close()`를 호출해 줍니다.

여러 개의 장기 상태를 setup/teardown 패턴으로 관리해야 할 때는 `CachedFactory`를 헬퍼로 사용할 수 있습니다. 최대 캐시 크기를 지정해 두면 그 크기를 넘길 때 오래된 항목부터 정리됩니다.

## 8. Task.Uncached와 Task.Anon

`Task.Uncached`는 매번 실행되고 캐시에서 값을 읽지 않는 Task로, 컨테이너 레지스트리 접근처럼 외부 시스템과의 상호작용 자체가 목적일 때 씁니다. `--watch` 모드에서 파일 변경 트리거로 실행되지도 않습니다.

`Task.Anon`은 CLI에서 직접 실행할 수 없는 익명 Task로, 여러 Task가 공통으로 쓰는 로직을 재사용하기 위한 헬퍼입니다. 결과를 직렬화하거나 캐싱할 필요가 없으므로 반환 타입 제약도 없습니다.

## 9. Module 기본 구조

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

## 10. trait으로 Module 재사용하기

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

## 11. moduleDir로 입력 경로 관리하기

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

## 12. override와 super

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

## 13. Root Module

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

## 14. DefaultTaskModule, ExternalModule, ModuleRef

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

## 15. out 디렉터리 구조

Mill은 모든 빌드 산출물과 캐시 메타데이터를 프로젝트 루트의 `out/` 폴더 아래에 모듈 계층과 동일한 구조로 저장합니다. 예를 들어 `foo.compile`이라는 Task를 실행하면 다음 파일들이 생깁니다.

| 경로 | 내용 |
|------|------|
| `out/foo/compile.json` | 캐시 키와 JSON 직렬화된 반환값. 반환값에 `PathRef`가 포함되어 있으면 `.dest/` 안의 실제 바이너리 파일을 가리키는 참조가 들어있음 |
| `out/foo/compile.dest/` | 해당 Task 전용 스크래치 폴더. Task는 이 폴더 안에만 파일을 생성해야 함 |
| `out/foo/compile.log` | 해당 Task 실행 중 발생한 stdout/stderr. 여러 Task가 병렬 실행되어 콘솔 출력이 뒤섞일 때 개별 로그를 확인하는 용도 |
| `out/foo/compile.super/` | `super.foo()` 같은 방식으로 override된 원래 구현을 호출했을 때 그 원래 구현의 메타데이터가 저장되는 곳 |

`mill show foo.compile` 명령으로 `compile.json`에 저장된 값을 바로 확인할 수 있습니다.

## 16. 빌드 진단용 JSON 파일들

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

## 17. out 디렉터리 관리

`out/` 아래 파일을 손으로 정리할 때는 반드시 `.dest/` 폴더와 대응하는 `.json` 파일을 함께 지워야 합니다. `foo.dest/`만 지우고 `foo.json`을 남겨두면 캐시 메타데이터와 실제 파일 상태가 어긋나 진단하기 어려운 문제가 생길 수 있습니다. 개별 폴더를 직접 건드리기보다는 Mill이 제공하는 `clean` 명령을 쓰는 편이 안전합니다.

`out/` 디렉터리 위치는 `MILL_OUTPUT_DIR` 환경변수로 바꿀 수 있습니다.

```
MILL_OUTPUT_DIR=build-stuff/working-dir ./mill foo.printDest
```

빌드 산출물을 더 빠른 디스크나 별도의 쓰기 가능한 파일시스템에 두고 싶을 때 이 옵션을 활용할 수 있습니다.

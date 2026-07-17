# Mill 내부 동작 원리

> 원본: https://mill-build.org/mill/depth/evaluation-model.html, https://mill-build.org/mill/depth/caching.html, https://mill-build.org/mill/depth/parallelism.html, https://mill-build.org/mill/depth/process-architecture.html, https://mill-build.org/mill/depth/sandboxing.html, https://mill-build.org/mill/depth/design-principles.html, https://mill-build.org/mill/depth/why-scala.html

---

## 목차

1. [Evaluation Model](#1-evaluation-model)
2. [캐싱 (Caching)](#2-캐싱-caching)
3. [병렬성 (Parallelism)](#3-병렬성-parallelism)
4. [프로세스 아키텍처](#4-프로세스-아키텍처)
5. [샌드박싱 (Sandboxing)](#5-샌드박싱-sandboxing)
6. [설계 원칙 (Design Principles)](#6-설계-원칙-design-principles)
7. [왜 Scala인가](#7-왜-scala인가)

---

## 1. Evaluation Model

`./mill foo.assembly` 같은 명령 하나가 실제로 실행되기까지 Mill은 내부적으로 4단계를 거칩니다.

| 단계 | 하는 일 |
|------|---------|
| Compilation | `build.mill`/`package.mill`을 Scala 코드로 변환 후 JVM 클래스파일로 컴파일 |
| Resolution | 커맨드라인 태스크 셀렉터를 실제 태스크 객체 목록으로 변환 |
| Planning | 선택된 태스크의 upstream 의존성을 모두 포함한 전체 태스크 그래프 구성 |
| Execution | 그래프를 따라 태스크를 실제로 실행 (캐싱·병렬화 적용) |

### 1.1 Compilation

`build.mill`, `package.mill` 파일은 매번 처음부터 다시 컴파일되지 않습니다. Zinc 기반 증분 컴파일을 사용하므로 첫 실행만 느리고 이후 수정은 변경분만 반영합니다. 컴파일된 클래스파일은 동적으로 로드되어 구체적인 `RootModule` 객체로 인스턴스화됩니다.

### 1.2 Resolution

Java reflection으로 module tree를 순회하며 커맨드라인 셀렉터와 매칭되는 태스크를 찾습니다. 이때 필요한 모듈만 지연 인스턴스화합니다. 예를 들어 `./mill _.assembly`는 wildcard `_`를 순회하며 `foo.assembly`, `bar.assembly`를 찾아냅니다.

### 1.3 Planning

Resolution에서 찾은 태스크뿐 아니라, 그 태스크가 의존하는 모든 상위 태스크까지 포함해 완전한 그래프를 만듭니다. `bar.assembly` 하나만 선택해도 실제로는 다음과 같은 그래프 전체가 계획에 포함됩니다.

```
bar.sources    → bar.compile → bar.classPath → bar.assembly
bar.sources    → bar.lineCount → bar.resources → bar.assembly
foo.sources    → foo.compile  → foo.classPath  → bar.classPath
```

즉 사용자는 `assembly`만 지정했지만 컴파일, 소스 수집, 커스텀 태스크(`lineCount`) 등 필요한 모든 의존 작업이 자동으로 딸려 온다는 뜻입니다. 그래프를 눈으로 확인하려면 다음 명령을 씁니다.

```bash
./mill plan foo.assembly
./mill visualizePlan foo.assembly   # SVG로 시각화
```

`mill-dependency-tree.json`을 열어보면 이 구조를 데이터로도 확인할 수 있습니다.

### 1.4 Execution

계획된 그래프를 실제로 순회하며 태스크를 실행합니다. 이 단계에서 캐싱과 병렬화가 함께 적용됩니다(각각 2장, 3장 참고).

예제 모듈을 보겠습니다.

```scala
object bar extends JavaModule {
  def moduleDeps = Seq(foo)

  def lineCount = Task {
    allSourceFiles().map(f => os.read.lines(f.path).size).sum
  }

  override def resources = Task {
    os.write(Task.dest / "line-count.txt", "" + lineCount())
    Seq(PathRef(Task.dest))
  }
}
```

`foo/src/*.java`만 변경했다면 무효화되는 경로는 다음과 같습니다.

```
foo.sources (변경) → foo.compile → foo.classPath → foo.assembly
                                  → bar.classPath → bar.assembly
```

이때 `bar.sources`, `bar.lineCount`는 foo 변경과 무관하므로 캐시된 값을 그대로 재사용합니다. 반대로 `bar/src/*.java`만 변경하면 `bar.sources → bar.compile / bar.lineCount → bar.resources → bar.assembly`만 재실행되고, `foo` 쪽 태스크는 전부 캐시를 씁니다.

Mill의 중요한 설계상 제약 하나는, **태스크 바깥의 코드는 그래프 구조(어떤 태스크가 있고 서로 어떻게 연결되는지)를 자유롭게 정의할 수 있지만, 태스크 안의 코드는 그래프 형태를 바꿀 수 없다**는 점입니다. Resolution/Planning 단계에서 그래프가 이미 고정되기 때문에, `inspect`, `plan`, `visualize` 같은 명령이 실제로 태스크를 실행하지 않고도 빌드 구조를 정확히 들여다볼 수 있습니다.

### 1.5 out/ 디렉터리

태스크 실행 결과는 module/task 이름을 그대로 반영한 경로에 저장됩니다.

```
out/
  foo/
    compile.json     # 캐시 메타데이터 (hashcode 등)
    compile.dest/    # 컴파일 결과물
  bar/
    lineCount.json
    assembly.dest/   # 최종 산출물
```

태스크마다 전용 경로가 분리되어 있어 서로 다른 태스크가 같은 파일을 두고 경쟁하지 않습니다.

### 1.6 부트스트래핑

`./mill` 실행 시 내부적으로 벌어지는 일입니다.

1. 부트스트랩 스크립트가 필요한 Mill 버전을 확인
2. 필요한 의존성을 다운로드하고 daemon을 기동
3. `MillBuildRootModule.BootstrapModule` 인스턴스화
4. 메타빌드(`mill-build/build.mill`)가 있다면 먼저 처리
5. 최종적으로 `build.mill`을 파싱·의존성 해석·컴파일

사용자는 JVM만 있으면 되고, 나머지 의존성은 Mill이 Maven Central에서 자동으로 받아옵니다.

---

## 2. 캐싱 (Caching)

Mill은 4단계 evaluation 각각에서 서로 다른 방식으로 캐싱을 적용합니다.

| 단계 | 캐싱 방식 |
|------|-----------|
| Compilation | `build.mill`/`package.mill` 변경분만 Zinc 증분 컴파일. 미변경 시 단계 자체를 스킵 |
| Resolution | 파일 미변경 시 이전에 만든 Module 객체를 그대로 재사용. 코드 변경 시 Module과 classloader를 전부 폐기 후 재생성 |
| Planning | 상대적으로 가벼운 단계라 별도 캐싱 없음 |
| Execution | 아래에서 상세히 설명 |

### 2.1 Execution 단계의 캐싱

- **일반 태스크**: 입력값이 바뀌지 않으면 재평가하지 않고 캐시된 반환값을 사용합니다.
- **Persistent Task**: `Task.dest` 폴더 내용을 디스크에 그대로 보존해, 파일 단위로 세밀한 증분 재사용이 가능합니다(예: Zinc의 analysis 파일).
- **Worker**: 메모리에 상태를 유지하는 장기 객체로, 입력이 바뀌었을 때만 재검증/재생성합니다.
- Mill은 메서드 호출 그래프를 코드 수준에서 추적하여 어떤 태스크가 어떤 태스크에 의존하는지 파악하고, 이를 캐시 키(반환값의 hashCode 포함)로 사용합니다.

### 2.2 캐싱 디버깅용 산출물

| 파일 | 용도 |
|------|------|
| `out/mill-profile.json` | 각 태스크가 캐시를 썼는지, 얼마나 걸렸는지 기록 |
| `out/mill-invalidation-tree.json` | 캐시가 무효화된 태스크의 트리와 원인(코드 변경 등) 추적 |

### 2.3 프로그래밍 제약

캐싱이 올바르게 동작하려면 빌드 코드가 **실행 순서에 관계없이 항상 같은 결과**를 내야 합니다. 특히 Resolution 단계(module 초기화 코드)에서 외부 상태(파일, 환경변수 등)를 읽는다면 반드시 다음처럼 감싸야 캐시 무효화 추적에 반영됩니다.

```scala
mill.api.BuildCtx.watchValue { os.read(someConfigFile) }
```

### 2.4 언어별 증분 컴파일 백엔드

| 언어 | 증분 컴파일 도구 |
|------|------------------|
| Java / Scala | Zinc |
| Kotlin | Kotlin Build Tool API |

이들은 모두 Persistent Task로 구현되어 이전 실행의 캐시·analysis 파일을 재사용합니다.
 캐시가 깨졌다고 의심되면 `./mill clean`으로 초기화할 수 있습니다.

---

## 3. 병렬성 (Parallelism)

### 3.1 기본 동작

Mill은 별도 설정 없이도 기본적으로 코어 수만큼 태스크를 병렬 평가합니다. `--jobs`(`-j`) 플래그로 동시성 수준을 직접 제어할 수 있습니다.

```bash
./mill -j 1 __.compile   # 완전 직렬 실행
./mill -j 8 __.compile   # 최대 8개 동시 실행
```

### 3.2 병렬화 가능 여부

무엇이 병렬화될 수 있는지는 전적으로 태스크 그래프 구조에 달려 있습니다.

- 서로 의존하는 태스크/모듈은 반드시 순차 실행됩니다.
- 서로 독립적인 태스크/모듈은 병렬 실행됩니다.

따라서 "넓고 얕은(wide & shallow)" 구조 — 즉 서로 독립적인 모듈이 많은 빌드 — 일수록 병렬화 효과가 큽니다. 앞서 1장 예제에서 `bar.compile`과 `bar.lineCount`(그리고 그 하위 `bar.classPath`/`bar.resources`)는 서로 무관하므로 별도 스레드에서 동시에 실행되고, `bar.assembly`는 둘 다 끝날 때까지 대기합니다.

### 3.3 그래프 시각화와 프로파일링

```bash
./mill visualize __.compile   # 의존 관계를 SVG로
```

매 실행마다 `out/mill-chrome-profile.json`이 생성되며, 이를 `chrome://tracing`에 로드하면 실제 병렬 실행 타임라인을 볼 수 있습니다. 여기서 downstream 태스크를 오래 기다리게 만드는 "long pole" 태스크를 찾아 최적화 대상으로 삼을 수 있습니다.

### 3.4 스레드 안전성

병렬 실행이 기본값이므로 빌드 코드 작성 시 다음을 신경 써야 합니다.

- **불변 함수형 스타일**: 값과 순수 함수 위주로 작성하면 기본적으로 스레드 안전합니다.
- **파일시스템 쓰기**: 각 태스크는 자신의 `Task.dest`에만 쓰므로 서로 다른 태스크 간 경쟁 조건이 원천적으로 방지됩니다.
- **Worker의 장기 상태**: Worker는 초기화는 단일 스레드에서 이뤄지지만 사용은 여러 스레드에서 동시에 일어날 수 있으므로, `synchronized`, `ConcurrentHashMap`, Mill이 제공하는 `CachedFactory` 등으로 직접 동시성을 관리해야 합니다.

### 3.5 Task.fork.async/await (실험적)

태스크 내부에서 추가 비동기 작업을 띄우고 싶을 때 쓰는 API입니다.

```scala
val fut = Task.fork.async(dest, key, message) { logger => /* 작업 */ }
val result = Task.fork.await(fut)
```

| 파라미터 | 의미 |
|----------|------|
| `dest` | 비동기 작업이 사용할 전용 디렉토리 |
| `key` | 로그에 표시할 식별자 |
| `message` | 사람이 읽을 설명 문구 |
| `priority` | 음수일수록 우선순위 높음, 양수일수록 낮음 |

`Task.fork`는 내부적으로 Java `ForkJoinPool`과 `ManagedBlocker`를 사용해 `-j`로 지정한 동시성 상한 안에서 동작하므로, 직접 스레드 풀을 새로 만드는 것보다 이 API를 쓰는 편이 Mill의 병렬성 제어와 충돌하지 않습니다.

---

## 4. 프로세스 아키텍처

Mill은 client-server 구조로 동작합니다. `./mill` 명령을 실행할 때마다 매번 새 launcher 프로세스가 뜨지만, 실제 빌드 작업은 하나의 장기 실행 daemon이 처리합니다.

| 컴포넌트 | 역할 |
|----------|------|
| Mill Launcher (client) | 사용자 입력(stdin, 인자)을 daemon에 전달하고, daemon의 stdout/stderr/exit code를 그대로 사용자에게 돌려주는 얇은 래퍼. 일반 CLI 애플리케이션처럼 보이고 동작함 |
| Mill Daemon (server) | 장기 실행 프로세스. 한 번에 하나만 떠 있도록 파일락으로 상호 배제. `build.mill`/`package.mill`을 컴파일하고 `URLClassLoader`로 모듈·태스크를 메모리에 인스턴스화. `Evaluator`가 resolve/plan/execute 수행 |

### 4.1 통신

Launcher와 daemon은 소켓으로 통신합니다. daemon에서 발생하는 표준출력/표준에러는 `PromptLogger`가 캡처해 ANSI 이스케이프로 포맷팅한 뒤 launcher 쪽으로 전달합니다.

### 4.2 생명주기

```
1. ./mill 실행 → launcher 기동
2. daemon이 없으면 새로 생성, 있으면 재사용
3. launcher가 소켓으로 명령/인자 전달
4. daemon이 evaluate(resolve→plan→execute) 수행
5. 결과(stdout/stderr/exit code)를 launcher로 반환
6. daemon은 종료하지 않고 다음 명령을 위해 대기
```

daemon을 계속 띄워두는 이유는 JVM 시작 오버헤드를 줄이고, JIT 워밍업된 상태를 다음 실행에도 활용하기 위해서입니다. Mill 버전이 바뀌거나 daemon이 비정상 종료되면 다음 실행 시 자동으로 재시작됩니다. 한 daemon은 한 번에 하나의 빌드 작업만 진행하도록 보장됩니다.

### 4.3 out/ 디렉터리와의 관계

```
out/
  foo/compile.json       # 태스크 캐시 메타데이터
  foo/compile.dest/       # 생성 파일/바이너리
  mill-daemon/            # launcher-daemon 간 데이터 교환용
```

각 태스크는 자신에게 할당된 경로에서만 읽고 쓰므로, daemon이 여러 빌드 요청을 연달아 처리해도 태스크 간 파일 충돌이 발생하지 않습니다.

---

## 5. 샌드박싱 (Sandboxing)

Mill의 샌드박싱은 보안 격리가 아니라, 태스크/모듈이 서로 실수로 간섭하지 않도록 막는 **best-practice 가드레일**입니다.

### 5.1 Task.dest와 파일시스템 제약

각 태스크는 `out/<module>/<task>.dest/` 경로에 고유한 dest 폴더를 가지며, 실제로 그 경로를 참조하는 시점에 지연 생성됩니다. 실행 중인 태스크 코드는 다음 규칙을 지켜야 합니다.

- **쓰기**: 자신의 `Task.dest` 폴더 안에만 가능
- **읽기**: 업스트림 태스크가 반환한 `PathRef`를 통해서만 가능
- **모듈 초기화 코드에서 디스크 읽기**: `BuildCtx.watchValue { ... }`로 감싸야 캐시 무효화 추적 대상에 포함됨

규칙을 어기면 다음과 같은 에러가 발생합니다.

```
Writing to banned-path not allowed during execution of `bannedWriteTask`.
Reading from build.mill not allowed during execution of `bannedReadTask`.
```

### 5.2 제약 우회

정말 필요하다면 다음 방법으로 체크를 끌 수 있지만, "Mill이 강제하려는 best practice를 우회하는 것"이라는 점을 감안해야 합니다.

```scala
BuildCtx.withFilesystemCheckerDisabled { /* ... */ }
```

```bash
./mill --no-filesystem-checker ...
```

### 5.3 os.pwd 리디렉션

Mill은 태스크 실행 도중 `os.pwd`를 자동으로 바꿔치기합니다.

| 상황 | os.pwd가 가리키는 곳 |
|------|----------------------|
| 태스크 내부 실행 중 | 해당 태스크의 `.dest/` 폴더 |
| 태스크 외부(설정 코드 등) | `out/mill-daemon/sandbox/` |

`os.proc`, `os.call`, `os.spawn`으로 띄우는 하위 프로세스에도 동일하게 적용됩니다.

### 5.4 테스트 샌드박싱

각 테스트 모듈은 독립된 작업 디렉토리에서 실행되므로 병렬로 테스트를 돌려도 파일이 충돌하지 않습니다. 예를 들어 `foo.test`와 `bar.test`가 둘 다 `generated.html`이라는 동일한 파일명을 쓰더라도, 실제로는 각각 `out/foo/test/testForked.dest/`, `out/bar/test/testForked.dest/`에 격리되어 기록됩니다.

테스트 코드에서 프로젝트 루트가 필요하면 환경변수 `MILL_WORKSPACE_ROOT`로 접근할 수 있습니다. Maven/Gradle에서 마이그레이션하는 과정처럼 테스트가 반드시 프로젝트 루트에서 실행돼야 하는 특수한 경우에는 다음처럼 끌 수 있지만 권장하지는 않습니다.

```scala
def testSandboxWorkingDir = false
```

기본값(`true`)을 유지해야 watch 모드와 selective test execution이 정확하게 동작합니다.

### 5.5 한계

Mill의 샌드박싱은 우발적인 간섭만 방지할 뿐, 의도적인 우회까지 막지는 못합니다. `java.io`/`java.nio` API 호출은 리디렉션 대상이 아니며, `BuildCtx.workspaceRoot`나 `withFilesystemCheckerDisabled`를 쓰면 watch/캐시 기능이 일부 의존성을 놓칠 수 있습니다.

---

## 6. 설계 원칙 (Design Principles)

### 6.1 핵심 5가지 원칙

| 원칙 | 내용 |
|------|------|
| Dependency graph first | `Task {...}` 문법으로 만든 의존성 그래프가 가장 중요한 추상화. `ScalaModule` 같은 헬퍼는 이 그래프 위에 얹힌 편의 계층일 뿐 |
| Builds are hierarchical | 커맨드라인에서 `mill Foo.bar.baz`로 실행하는 것과 Scala 코드에서 `Foo.bar.baz`로 참조하는 것이 동일한 계층 구조를 가리킴 |
| Caching by default | "느린 태스크만" 캐싱하는 게 아니라 모든 태스크를 기본으로 캐싱. 반환값의 hashCode를 키로 삼고, 파일시스템 내용을 함께 검증 |
| Functional purity | 태스크는 입력 태스크에만 의존하고 반환값만 출력하는 순수 함수여야 함. 전역 파일시스템을 여기저기 읽고 쓰지 않음 |
| Short-lived build processes | 빌드는 daemon처럼 계속 떠 있는 게 아니라 반복적으로 짧게 실행됨. 시작 속도가 중요하며, 재시작해도 `out/`의 JSON에서 이전 상태를 복원 |

추가로, 태스크는 Monadic이 아니라 **Applicative**입니다. `.map`, `.zip`은 있지만 `.flatMap`은 없습니다. 이 덕분에 실행 전에 전체 그래프 구조를 정적으로 알 수 있고, 이를 기반으로 병렬화, dry-run, 그래프 시각화, 쿼리 같은 기능을 구현할 수 있습니다.

### 6.2 단순함을 만드는 3가지 개념

Mill은 빌드 도구가 흔히 따로따로 만들어내는 개념들을 익숙한 프로그래밍 개념 3가지로 통합합니다.

| 개념 | 역할 |
|------|------|
| 객체 계층(object hierarchy) | 모듈 구조, 태스크 위치, 캐시/출력 폴더 경로를 결정 |
| 호출 그래프(call graph) | 태스크 간 의존 관계, 병렬화 가능 여부, 데이터 흐름을 결정 |
| Trait/Class 인스턴스화 | 공통 구조 재사용, 커스터마이징, cross-build 구현 |

즉 변수 선언, 함수 호출, 상속, 오버라이딩 같은 평범한 Scala 패턴이 그대로 빌드 설정 문법이 됩니다.

### 6.3 다른 빌드 도구와의 비교

| 도구 | 특징 | Mill이 다르게 가져간 부분 |
|------|------|---------------------------|
| Maven | 선언적 XML | Scala로 프로그래밍 가능하게 함 |
| Gradle | 프로그래밍 가능(Groovy/Kotlin) | 더 단순하고 명시적인 의존성 그래프 모델 |
| sbt | Scala 기반 | 전역 태스크 설정과 복잡한 스코프 규칙을 정리 |
| Bazel | 계층화된 캐시(Starlark + 실행 계층) | Starlark(Python 유사) 계층을 없애고 Scala 한 계층으로 단순화 |
| CBT | 상속 기반 구성 | 상속 모델은 채택하되 실행 모델을 개선 |

정리하면 Mill은 Bazel의 강점(계층화된 강력한 캐싱)과 sbt의 강점(Scala 기반 설정)을 결합하면서 각각이 가진 복잡성(Starlark 이중 계층, sbt의 전역 스코프 규칙)은 덜어내는 방향을 택했습니다.

---

## 7. 왜 Scala인가

### 7.1 범용 프로그래밍 언어를 선택한 이유

YAML/XML/TOML 같은 제한된 설정 언어 대신 범용 언어를 택한 이유는 빌드 작업 자체가 본질적으로 복잡하기 때문입니다. Protocol Buffer 코드 생성, 컨테이너 이미지 빌드, 정적 리소스 생성처럼 특수한 요구사항이 계속 튀어나오는데, 제한된 언어로는 이를 표현할 수 없어 결국 외부 플러그인, bash 스크립트, 동적으로 설정을 생성하는 우회책에 의존하게 됩니다.

한편 CMake나 Make처럼 자체 DSL을 만든 도구들은 IDE 지원, 디버거/프로파일러 같은 개발 도구, 언어 자체의 완성도와 표준 라이브러리 면에서 범용 언어에 뒤처지는 경향이 있습니다.

### 7.2 그중에서도 Scala를 선택한 이유

| 이유 | 설명 |
|------|------|
| 간결성 | Python(Bazel), Groovy(Gradle), Ruby(Rake) 수준의 간결함을 제공하면서 Java만큼 장황해지지 않음 |
| 정적 타입 | 빌드 설정을 고치는 대부분의 사람은 빌드 전문가가 아니라 온라인 예제를 복사해 붙여넣는 사람들. 정적 타입은 IDE 자동완성/네비게이션을 강화하고, 초보자가 저지르는 타입 오류를 사전에 잡아줌 |
| FP + OOP 균형 | 빌드 그래프는 메서드 호출로 이뤄진 함수형 그래프로 모델링하고, 모듈 계층은 `extends`/`override`/`super` 같은 클래스 상속으로 표현. 대학 프로그래밍 개론 수준의 개념만으로 빌드 설정을 다룰 수 있음 |

### 7.3 JVM을 플랫폼으로 선택한 이유

- **동적 클래스 로딩**: 서버 애플리케이션에서는 크게 필요 없지만 빌드 도구에서는 필수에 가깝습니다. `mvnDeps`로 외부 의존성을 동적으로 불러오거나, 메타빌드처럼 빌드 자체를 동적으로 재구성하는 기능은 Go/Rust/C++ 같은 정적 링크 플랫폼에서는 구현하기 어렵습니다.
- **거대한 JVM 생태계**: IntelliJ, VS Code 같은 IDE가 클래스파일·클래스패스·Scala 언어를 이미 깊이 이해하고 있어 추가 설정 없이 강력한 IDE 지원을 받을 수 있습니다. `jstack`, JProfiler, YourKit 같은 디버깅/프로파일링 도구도 그대로 활용 가능합니다.
- **출판 인프라**: Maven Central은 네임스페이싱, 검색 용이성, 불변성, 코드 서명 같은 특성을 갖춘 탄탄한 패키지 저장소입니다. Mill 플러그인 배포에 표준화된 경로를 제공하며, 이미 모든 JVM 프로젝트가 신뢰하고 사용하는 인프라를 그대로 활용합니다.

결과적으로 Mill은 빌드에서 자주 필요한 복잡한 요구사항들을 외부 스크립트나 플러그인으로 흩어놓지 않고 하나의 범용 언어 안에 담아내면서, Scala의 정적 타입과 간결함, JVM의 동적 로딩과 생태계를 함께 활용해 개발자 경험을 끌어올리는 방향을 택했습니다.

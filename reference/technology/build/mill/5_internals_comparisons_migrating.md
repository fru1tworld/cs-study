# Mill 내부 동작 원리, 다른 빌드 도구 비교, 마이그레이션

## Mill 내부 동작 원리

> 원본: https://mill-build.org/mill/depth/evaluation-model.html, https://mill-build.org/mill/depth/caching.html, https://mill-build.org/mill/depth/parallelism.html, https://mill-build.org/mill/depth/process-architecture.html, https://mill-build.org/mill/depth/sandboxing.html, https://mill-build.org/mill/depth/design-principles.html, https://mill-build.org/mill/depth/why-scala.html

---

### 목차

1. [Evaluation Model](#1-evaluation-model)
2. [캐싱 (Caching)](#2-캐싱-caching)
3. [병렬성 (Parallelism)](#3-병렬성-parallelism)
4. [프로세스 아키텍처](#4-프로세스-아키텍처)
5. [샌드박싱 (Sandboxing)](#5-샌드박싱-sandboxing)
6. [설계 원칙 (Design Principles)](#6-설계-원칙-design-principles)
7. [왜 Scala인가](#7-왜-scala인가)

---

### 1. Evaluation Model

Mill은 `./mill foo.assembly` 같은 명령 하나가 실제로 실행되기까지 내부적으로 4단계를 거침.

- Compilation: `build.mill`/`package.mill`을 Scala 코드로 변환 후 JVM 클래스파일로 컴파일
- Resolution: 커맨드라인 태스크 셀렉터를 실제 태스크 객체 목록으로 변환
- Planning: 선택된 태스크의 upstream 의존성을 모두 포함한 전체 태스크 그래프 구성
- Execution: 그래프를 따라 태스크를 실제로 실행(캐싱·병렬화 적용)

#### 1.1 Compilation

`build.mill`, `package.mill` 파일은 매번 처음부터 다시 컴파일되지 않음 → Zinc 기반 증분 컴파일을 사용 → 첫 실행만 느리고 이후 수정은 변경분만 반영함. 컴파일된 클래스파일은 동적으로 로드되어 구체적인 `RootModule` 객체로 인스턴스화됨.

#### 1.2 Resolution

Java reflection으로 module tree를 순회하며 커맨드라인 셀렉터와 매칭되는 태스크를 찾음 → 이때 필요한 모듈만 지연 인스턴스화함. 예를 들어 `./mill _.assembly`는 wildcard `_`를 순회하며 `foo.assembly`, `bar.assembly`를 찾아냄.

#### 1.3 Planning

Resolution에서 찾은 태스크뿐 아니라, 그 태스크가 의존하는 모든 상위 태스크까지 포함해 완전한 그래프를 구성함. `bar.assembly` 하나만 선택해도 실제로는 다음과 같은 그래프 전체가 계획에 포함됨.

```
bar.sources    → bar.compile → bar.classPath → bar.assembly
bar.sources    → bar.lineCount → bar.resources → bar.assembly
foo.sources    → foo.compile  → foo.classPath  → bar.classPath
```

즉 사용자는 `assembly`만 지정했지만 컴파일·소스 수집·커스텀 태스크(`lineCount`) 등 필요한 모든 의존 작업이 자동으로 딸려옴. 그래프를 눈으로 확인하려면 다음 명령을 사용함.

```bash
./mill plan foo.assembly
./mill visualizePlan foo.assembly   # SVG로 시각화
```

`mill-dependency-tree.json`을 열어보면 이 구조를 데이터로도 확인 가능함.

#### 1.4 Execution

계획된 그래프를 실제로 순회하며 태스크를 실행함 → 이 단계에서 캐싱과 병렬화가 함께 적용됨(각각 2장, 3장 참고).

예제 모듈.

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

`foo/src/*.java`만 변경했다면 무효화되는 경로는 다음과 같음.

```
foo.sources (변경) → foo.compile → foo.classPath → foo.assembly
                                  → bar.classPath → bar.assembly
```

이때 `bar.sources`, `bar.lineCount`는 foo 변경과 무관 → 캐시된 값을 그대로 재사용함. 반대로 `bar/src/*.java`만 변경하면 `bar.sources → bar.compile / bar.lineCount → bar.resources → bar.assembly`만 재실행되고, `foo` 쪽 태스크는 전부 캐시를 사용함.

Mill의 중요한 설계상 제약 하나: 태스크 바깥의 코드는 그래프 구조(어떤 태스크가 있고 서로 어떻게 연결되는지)를 자유롭게 정의할 수 있지만, 태스크 안의 코드는 그래프 형태를 바꿀 수 없음. Resolution/Planning 단계에서 그래프가 이미 고정되기 때문에, `inspect`, `plan`, `visualize` 같은 명령이 실제로 태스크를 실행하지 않고도 빌드 구조를 정확히 확인 가능함.

#### 1.5 out/ 디렉터리

태스크 실행 결과는 module/task 이름을 그대로 반영한 경로에 저장됨.

```
out/
  foo/
    compile.json     # 캐시 메타데이터 (hashcode 등)
    compile.dest/    # 컴파일 결과물
  bar/
    lineCount.json
    assembly.dest/   # 최종 산출물
```

태스크마다 전용 경로가 분리되어 있어 서로 다른 태스크가 같은 파일을 두고 경쟁하지 않음.

#### 1.6 부트스트래핑

`./mill` 실행 시 내부적으로 벌어지는 일.

1. 부트스트랩 스크립트가 필요한 Mill 버전을 확인
2. 필요한 의존성을 다운로드하고 daemon을 기동
3. `MillBuildRootModule.BootstrapModule` 인스턴스화
4. 메타빌드(`mill-build/build.mill`)가 있다면 먼저 처리
5. 최종적으로 `build.mill`을 파싱·의존성 해석·컴파일

사용자는 JVM만 있으면 되고, 나머지 의존성은 Mill이 Maven Central에서 자동으로 받아옴.

---

### 2. 캐싱 (Caching)

Mill은 4단계 evaluation 각각에서 서로 다른 방식으로 캐싱을 적용함.

- Compilation: `build.mill`/`package.mill` 변경분만 Zinc 증분 컴파일. 미변경 시 단계 자체를 스킵
- Resolution: 파일 미변경 시 이전에 만든 Module 객체를 그대로 재사용. 코드 변경 시 Module과 classloader를 전부 폐기 후 재생성
- Planning: 상대적으로 가벼운 단계라 별도 캐싱 없음
- Execution: 아래에서 상세히 설명

#### 2.1 Execution 단계의 캐싱

- 일반 태스크: 입력값이 바뀌지 않으면 재평가하지 않고 캐시된 반환값을 사용함
- Persistent Task: `Task.dest` 폴더 내용을 디스크에 그대로 보존 → 파일 단위로 세밀한 증분 재사용 가능(예: Zinc의 analysis 파일)
- Worker: 메모리에 상태를 유지하는 장기 객체로, 입력이 바뀌었을 때만 재검증/재생성함
- Mill은 메서드 호출 그래프를 코드 수준에서 추적하여 어떤 태스크가 어떤 태스크에 의존하는지 파악하고, 이를 캐시 키(반환값의 hashCode 포함)로 사용함

#### 2.2 캐싱 디버깅용 산출물

- `out/mill-profile.json`: 각 태스크가 캐시를 썼는지, 얼마나 걸렸는지 기록
- `out/mill-invalidation-tree.json`: 캐시가 무효화된 태스크의 트리와 원인(코드 변경 등) 추적

#### 2.3 프로그래밍 제약

캐싱이 올바르게 동작하려면 빌드 코드가 실행 순서에 관계없이 항상 같은 결과를 내야 함. 특히 Resolution 단계(module 초기화 코드)에서 외부 상태(파일, 환경변수 등)를 읽는다면 반드시 다음처럼 감싸야 캐시 무효화 추적에 반영됨.

```scala
mill.api.BuildCtx.watchValue { os.read(someConfigFile) }
```

#### 2.4 언어별 증분 컴파일 백엔드

- Java / Scala: Zinc
- Kotlin: Kotlin Build Tool API

이들은 모두 Persistent Task로 구현되어 이전 실행의 캐시·analysis 파일을 재사용함. 캐시가 깨졌다고 의심되면 `./mill clean`으로 초기화 가능함.

---

### 3. 병렬성 (Parallelism)

#### 3.1 기본 동작

Mill은 별도 설정 없이도 기본적으로 코어 수만큼 태스크를 병렬 평가함. `--jobs`(`-j`) 플래그로 동시성 수준을 직접 제어 가능함.

```bash
./mill -j 1 __.compile   # 완전 직렬 실행
./mill -j 8 __.compile   # 최대 8개 동시 실행
```

#### 3.2 병렬화 가능 여부

무엇이 병렬화될 수 있는지는 전적으로 태스크 그래프 구조에 달려 있음.

- 서로 의존하는 태스크/모듈은 반드시 순차 실행됨
- 서로 독립적인 태스크/모듈은 병렬 실행됨

따라서 "넓고 얕은(wide & shallow)" 구조 — 즉 서로 독립적인 모듈이 많은 빌드 — 일수록 병렬화 효과가 큼. 앞서 1장 예제에서 `bar.compile`과 `bar.lineCount`(그리고 그 하위 `bar.classPath`/`bar.resources`)는 서로 무관하므로 별도 스레드에서 동시에 실행되고, `bar.assembly`는 둘 다 끝날 때까지 대기함.

#### 3.3 그래프 시각화와 프로파일링

```bash
./mill visualize __.compile   # 의존 관계를 SVG로
```

매 실행마다 `out/mill-chrome-profile.json`이 생성됨 → 이를 `chrome://tracing`에 로드하면 실제 병렬 실행 타임라인을 확인 가능함. 여기서 downstream 태스크를 오래 기다리게 만드는 "long pole" 태스크를 찾아 최적화 대상으로 삼을 수 있음.

#### 3.4 스레드 안전성

병렬 실행이 기본값이므로 빌드 코드 작성 시 다음을 신경 써야 함.

- 불변 함수형 스타일: 값과 순수 함수 위주로 작성하면 기본적으로 스레드 안전함
- 파일시스템 쓰기: 각 태스크는 자신의 `Task.dest`에만 쓰므로 서로 다른 태스크 간 경쟁 조건이 원천적으로 방지됨
- Worker의 장기 상태: Worker는 초기화는 단일 스레드에서 이뤄지지만 사용은 여러 스레드에서 동시에 일어날 수 있으므로, `synchronized`, `ConcurrentHashMap`, Mill이 제공하는 `CachedFactory` 등으로 직접 동시성을 관리해야 함

#### 3.5 Task.fork.async/await (실험적)

태스크 내부에서 추가 비동기 작업을 띄우고 싶을 때 쓰는 API.

```scala
val fut = Task.fork.async(dest, key, message) { logger => /* 작업 */ }
val result = Task.fork.await(fut)
```

- `dest`: 비동기 작업이 사용할 전용 디렉토리
- `key`: 로그에 표시할 식별자
- `message`: 사람이 읽을 설명 문구
- `priority`: 음수일수록 우선순위 높음, 양수일수록 낮음

`Task.fork`는 내부적으로 Java `ForkJoinPool`과 `ManagedBlocker`를 사용해 `-j`로 지정한 동시성 상한 안에서 동작함 → 직접 스레드 풀을 새로 만드는 것보다 이 API를 쓰는 편이 Mill의 병렬성 제어와 충돌하지 않음.

---

### 4. 프로세스 아키텍처

Mill은 client-server 구조로 동작함. `./mill` 명령을 실행할 때마다 매번 새 launcher 프로세스가 뜨지만, 실제 빌드 작업은 하나의 장기 실행 daemon이 처리함.

- Mill Launcher (client): 사용자 입력(stdin, 인자)을 daemon에 전달하고, daemon의 stdout/stderr/exit code를 그대로 사용자에게 돌려주는 얇은 래퍼. 일반 CLI 애플리케이션처럼 보이고 동작함
- Mill Daemon (server): 장기 실행 프로세스. 한 번에 하나만 떠 있도록 파일락으로 상호 배제. `build.mill`/`package.mill`을 컴파일하고 `URLClassLoader`로 모듈·태스크를 메모리에 인스턴스화함. `Evaluator`가 resolve/plan/execute 수행

#### 4.1 통신

Launcher와 daemon은 소켓으로 통신함. daemon에서 발생하는 표준출력/표준에러는 `PromptLogger`가 캡처해 ANSI 이스케이프로 포맷팅한 뒤 launcher 쪽으로 전달함.

#### 4.2 생명주기

```
1. ./mill 실행 → launcher 기동
2. daemon이 없으면 새로 생성, 있으면 재사용
3. launcher가 소켓으로 명령/인자 전달
4. daemon이 evaluate(resolve→plan→execute) 수행
5. 결과(stdout/stderr/exit code)를 launcher로 반환
6. daemon은 종료하지 않고 다음 명령을 위해 대기
```

daemon을 계속 띄워두는 이유: JVM 시작 오버헤드를 줄이고 JIT 워밍업된 상태를 다음 실행에도 활용하기 위함. Mill 버전이 바뀌거나 daemon이 비정상 종료되면 다음 실행 시 자동으로 재시작됨. 한 daemon은 한 번에 하나의 빌드 작업만 진행하도록 보장됨.

#### 4.3 out/ 디렉터리와의 관계

```
out/
  foo/compile.json       # 태스크 캐시 메타데이터
  foo/compile.dest/       # 생성 파일/바이너리
  mill-daemon/            # launcher-daemon 간 데이터 교환용
```

각 태스크는 자신에게 할당된 경로에서만 읽고 쓰므로, daemon이 여러 빌드 요청을 연달아 처리해도 태스크 간 파일 충돌이 발생하지 않음.

---

### 5. 샌드박싱 (Sandboxing)

Mill의 샌드박싱은 보안 격리가 아니라, 태스크/모듈이 서로 실수로 간섭하지 않도록 막는 best-practice 가드레일임.

#### 5.1 Task.dest와 파일시스템 제약

각 태스크는 `out/<module>/<task>.dest/` 경로에 고유한 dest 폴더를 가지며, 실제로 그 경로를 참조하는 시점에 지연 생성됨. 실행 중인 태스크 코드는 다음 규칙을 지켜야 함.

- 쓰기: 자신의 `Task.dest` 폴더 안에만 가능
- 읽기: 업스트림 태스크가 반환한 `PathRef`를 통해서만 가능
- 모듈 초기화 코드에서 디스크 읽기: `BuildCtx.watchValue { ... }`로 감싸야 캐시 무효화 추적 대상에 포함됨

규칙을 어기면 다음과 같은 에러가 발생함.

```
Writing to banned-path not allowed during execution of `bannedWriteTask`.
Reading from build.mill not allowed during execution of `bannedReadTask`.
```

#### 5.2 제약 우회

정말 필요하다면 다음 방법으로 체크를 끌 수 있지만, "Mill이 강제하려는 best practice를 우회하는 것"이라는 점을 감안해야 함.

```scala
BuildCtx.withFilesystemCheckerDisabled { /* ... */ }
```

```bash
./mill --no-filesystem-checker ...
```

#### 5.3 os.pwd 리디렉션

Mill은 태스크 실행 도중 `os.pwd`를 자동으로 바꿔치기함.

- 태스크 내부 실행 중: `os.pwd`가 해당 태스크의 `.dest/` 폴더를 가리킴
- 태스크 외부(설정 코드 등): `os.pwd`가 `out/mill-daemon/sandbox/`를 가리킴

`os.proc`, `os.call`, `os.spawn`으로 띄우는 하위 프로세스에도 동일하게 적용됨.

#### 5.4 테스트 샌드박싱

각 테스트 모듈은 독립된 작업 디렉토리에서 실행되므로 병렬로 테스트를 돌려도 파일이 충돌하지 않음. 예를 들어 `foo.test`와 `bar.test`가 둘 다 `generated.html`이라는 동일한 파일명을 쓰더라도, 실제로는 각각 `out/foo/test/testForked.dest/`, `out/bar/test/testForked.dest/`에 격리되어 기록됨.

테스트 코드에서 프로젝트 루트가 필요하면 환경변수 `MILL_WORKSPACE_ROOT`로 접근 가능함. Maven/Gradle에서 마이그레이션하는 과정처럼 테스트가 반드시 프로젝트 루트에서 실행돼야 하는 특수한 경우에는 다음처럼 끌 수 있지만 권장하지 않음.

```scala
def testSandboxWorkingDir = false
```

기본값(`true`)을 유지해야 watch 모드와 selective test execution이 정확하게 동작함.

#### 5.5 한계

Mill의 샌드박싱은 우발적인 간섭만 방지할 뿐, 의도적인 우회까지 막지는 못함. `java.io`/`java.nio` API 호출은 리디렉션 대상이 아니며, `BuildCtx.workspaceRoot`나 `withFilesystemCheckerDisabled`를 쓰면 watch/캐시 기능이 일부 의존성을 놓칠 수 있음.

---

### 6. 설계 원칙 (Design Principles)

#### 6.1 핵심 5가지 원칙

- Dependency graph first: `Task {...}` 문법으로 만든 의존성 그래프가 가장 중요한 추상화. `ScalaModule` 같은 헬퍼는 이 그래프 위에 얹힌 편의 계층일 뿐
- Builds are hierarchical: 커맨드라인에서 `mill Foo.bar.baz`로 실행하는 것과 Scala 코드에서 `Foo.bar.baz`로 참조하는 것이 동일한 계층 구조를 가리킴
- Caching by default: "느린 태스크만" 캐싱하는 게 아니라 모든 태스크를 기본으로 캐싱함. 반환값의 hashCode를 키로 삼고, 파일시스템 내용을 함께 검증
- Functional purity: 태스크는 입력 태스크에만 의존하고 반환값만 출력하는 순수 함수여야 함. 전역 파일시스템을 여기저기 읽고 쓰지 않음
- Short-lived build processes: 빌드는 daemon처럼 계속 떠 있는 게 아니라 반복적으로 짧게 실행됨. 시작 속도가 중요하며, 재시작해도 `out/`의 JSON에서 이전 상태를 복원함

추가로 태스크는 Monadic이 아니라 Applicative임. `.map`, `.zip`은 있지만 `.flatMap`은 없음 → 실행 전에 전체 그래프 구조를 정적으로 파악 가능 → 이를 기반으로 병렬화·dry-run·그래프 시각화·쿼리 같은 기능을 구현할 수 있음.

#### 6.2 단순함을 만드는 3가지 개념

Mill은 빌드 도구가 흔히 따로따로 만들어내는 개념들을 익숙한 프로그래밍 개념 3가지로 통합함.

- 객체 계층(object hierarchy): 모듈 구조, 태스크 위치, 캐시/출력 폴더 경로를 결정
- 호출 그래프(call graph): 태스크 간 의존 관계, 병렬화 가능 여부, 데이터 흐름을 결정
- Trait/Class 인스턴스화: 공통 구조 재사용, 커스터마이징, cross-build 구현

즉 변수 선언·함수 호출·상속·오버라이딩 같은 평범한 Scala 패턴이 그대로 빌드 설정 문법이 됨.

#### 6.3 다른 빌드 도구와의 비교

- Maven
  - 특징: 선언적 XML
  - Mill이 다르게 가져간 부분: Scala로 프로그래밍 가능하게 함
- Gradle
  - 특징: 프로그래밍 가능(Groovy/Kotlin)
  - Mill이 다르게 가져간 부분: 더 단순하고 명시적인 의존성 그래프 모델
- sbt
  - 특징: Scala 기반
  - Mill이 다르게 가져간 부분: 전역 태스크 설정과 복잡한 스코프 규칙을 정리
- Bazel
  - 특징: 계층화된 캐시(Starlark + 실행 계층)
  - Mill이 다르게 가져간 부분: Starlark(Python 유사) 계층을 없애고 Scala 한 계층으로 단순화
- CBT
  - 특징: 상속 기반 구성
  - Mill이 다르게 가져간 부분: 상속 모델은 채택하되 실행 모델을 개선

정리하면 Mill은 Bazel의 강점(계층화된 강력한 캐싱)과 sbt의 강점(Scala 기반 설정)을 결합하면서 각각이 가진 복잡성(Starlark 이중 계층, sbt의 전역 스코프 규칙)은 덜어내는 방향을 택함.

---

### 7. 왜 Scala인가

#### 7.1 범용 프로그래밍 언어를 선택한 이유

빌드 작업 자체가 본질적으로 복잡함 → YAML/XML/TOML 같은 제한된 설정 언어 대신 범용 언어를 택함. Protocol Buffer 코드 생성·컨테이너 이미지 빌드·정적 리소스 생성처럼 특수한 요구사항이 계속 튀어나옴 → 제한된 언어로는 이를 표현할 수 없어 결국 외부 플러그인, bash 스크립트, 동적으로 설정을 생성하는 우회책에 의존하게 됨.

한편 CMake나 Make처럼 자체 DSL을 만든 도구들은 IDE 지원·디버거/프로파일러 같은 개발 도구·언어 자체의 완성도와 표준 라이브러리 면에서 범용 언어에 뒤처지는 경향이 있음.

#### 7.2 그중에서도 Scala를 선택한 이유

- 간결성: Python(Bazel), Groovy(Gradle), Ruby(Rake) 수준의 간결함을 제공하면서 Java만큼 장황해지지 않음
- 정적 타입: 빌드 설정을 고치는 대부분의 사람은 빌드 전문가가 아니라 온라인 예제를 복사해 붙여넣는 사람들. 정적 타입은 IDE 자동완성/네비게이션을 강화하고, 초보자가 저지르는 타입 오류를 사전에 잡아줌
- FP + OOP 균형: 빌드 그래프는 메서드 호출로 이뤄진 함수형 그래프로 모델링하고, 모듈 계층은 `extends`/`override`/`super` 같은 클래스 상속으로 표현함. 대학 프로그래밍 개론 수준의 개념만으로 빌드 설정을 다룰 수 있음

#### 7.3 JVM을 플랫폼으로 선택한 이유

- 동적 클래스 로딩: 서버 애플리케이션에서는 크게 필요 없지만 빌드 도구에서는 필수에 가까움. `mvnDeps`로 외부 의존성을 동적으로 불러오거나, 메타빌드처럼 빌드 자체를 동적으로 재구성하는 기능은 Go/Rust/C++ 같은 정적 링크 플랫폼에서는 구현하기 어려움
- 거대한 JVM 생태계: IntelliJ, VS Code 같은 IDE가 클래스파일·클래스패스·Scala 언어를 이미 깊이 이해하고 있어 추가 설정 없이 강력한 IDE 지원을 받을 수 있음. `jstack`, JProfiler, YourKit 같은 디버깅/프로파일링 도구도 그대로 활용 가능
- 출판 인프라: Maven Central은 네임스페이싱, 검색 용이성, 불변성, 코드 서명 같은 특성을 갖춘 탄탄한 패키지 저장소. Mill 플러그인 배포에 표준화된 경로를 제공하며, 이미 모든 JVM 프로젝트가 신뢰하고 사용하는 인프라를 그대로 활용함

결과적으로 Mill은 빌드에서 자주 필요한 복잡한 요구사항들을 외부 스크립트나 플러그인으로 흩어놓지 않고 하나의 범용 언어 안에 담아내면서, Scala의 정적 타입과 간결함·JVM의 동적 로딩과 생태계를 함께 활용해 개발자 경험을 끌어올리는 방향을 택함.

---

## Mill과 다른 빌드 도구 비교 (Maven, Gradle, 성능)

> 원본: https://mill-build.org/mill/comparisons/maven.html, https://mill-build.org/mill/comparisons/gradle.html, https://mill-build.org/mill/comparisons/performance.html

---

### 목차

1. [Maven과 비교: 선언적 빌드](#1-maven과-비교-선언적-빌드)
2. [Maven 대비 Mill이 개선한 지점](#2-maven-대비-mill이-개선한-지점)
3. [Gradle과 비교: 프로그래머블 빌드](#3-gradle과-비교-프로그래머블-빌드)
4. [Gradle 대비 Mill이 개선한 지점](#4-gradle-대비-mill이-개선한-지점)
5. [성능 벤치마크: Mill vs Maven](#5-성능-벤치마크-mill-vs-maven)
6. [성능 벤치마크: Mill vs Gradle](#6-성능-벤치마크-mill-vs-gradle)
7. [성능 차이가 발생하는 이유](#7-성능-차이가-발생하는-이유)
8. [종합 정리](#8-종합-정리)

---

### 1. Maven과 비교: 선언적 빌드

Maven은 `pom.xml`을 통해 빌드를 "선언적으로" 기술하는 방식을 채택한 원조 격 도구임. Mill은 이 선언적 접근 자체는 계승하되, Maven을 실제로 사용할 때 반복적으로 겪는 불편(마찰)을 없애는 데 초점을 맞춤.

Mill 문서는 `HtmlScraper.java`라는 예제(Jsoup으로 웹 페이지를 스크래핑하는 단일 파일 Java 프로그램)를 기준으로 Maven 사용 과정에서 겪는 문제를 하나씩 짚음.

#### Maven에서 마찰이 생기는 지점

- Java 설치: 배포판(JDK vendor), OS, 개인 취향에 따라 설치 방법이 제각각이라 "숙련된 개발자들 사이에서도 Java를 어떻게 설치해야 하는지 합의가 없다"
- `pom.xml` 작성: 복붙 없이 처음부터 작성 가능한 개발자가 드물 정도로 XML 네임스페이스 선언, `modelVersion`, `groupId`/`artifactId`, 플러그인 설정 등 보일러플레이트가 과도함
- Maven 설치: 플랫폼별 설치 명령이 다름(`brew install maven`, `apt install maven` 등), 버전 고정과 wrapper 스크립트 사용 여부도 별도 고민거리
- Java 프로그램 실행: 직관적이지 않은 명령: `mvn exec:java -Dexec.args="Java 1"` — 자주 안 쓰면 매번 검색해야 하는 수준

즉 Maven이 표방하는 "선언적 설정" 자체는 좋은 방향이지만, 그 주변을 둘러싼 설치/실행 경험이 발목을 잡는다는 것이 Mill 문서의 핵심 주장임.

---

### 2. Maven 대비 Mill이 개선한 지점

#### 빌드 설정 파일

Mill은 XML 대신 YAML 기반 `build.mill.yaml`을 사용해 동일한 의존성 선언을 훨씬 짧게 표현함.

```yaml
extends: JavaModule
mvnDeps:
- org.jsoup:jsoup:1.7.2
```

"Convention over Configuration" 원칙에 따라 프로젝트마다 달라지는 값만 적으면 되고, 나머지는 `JavaModule` 같은 기본 모듈이 채워줌.

#### 부트스트랩 스크립트

`./mill` 스크립트 하나로 다음을 전부 해결함.

- Mill 실행 파일 자체를 내려받고 캐싱
- 프로젝트에 필요한 JVM 런타임까지 관리(사전에 Java/Maven이 설치돼 있을 필요 없음)
- `jvmVersion` 설정으로 원하는 JDK 버전을 선언적으로 지정 가능(기본값 `zulu:25`)

#### 프로그램 실행 명령

Python(`python foo.py <args>`), Bash(`bash foo.sh <args>`)처럼 익숙한 형태를 그대로 따름.

```bash
./mill run Java 1
```

#### 단일 파일 스크립트

Java 파일 안에 `//|` 주석으로 빌드 헤더를 넣어 별도 프로젝트 구조 없이 바로 실행 가능함.

```java
//| mvnDeps:
//| - org.jsoup:jsoup:1.7.2
import org.jsoup.Jsoup;
```

```bash
./mill HtmlScraper.java Java 1
```

#### 요약 비교

- 설정 파일
  - Maven: XML(`pom.xml`), 보일러플레이트 많음
  - Mill: YAML(`build.mill.yaml`), 최소 선언
- JDK 관리
  - Maven: 별도 설치 필요, 버전 관리는 사용자 몫
  - Mill: `./mill` 스크립트가 JVM까지 자동 관리
- 도구 설치
  - Maven: OS별 패키지 매니저로 별도 설치
  - Mill: 부트스트랩 스크립트로 zero-setup
- 실행 명령
  - Maven: `mvn exec:java -Dexec.args="..."`
  - Mill: `./mill run ...` (Python/Bash와 동일한 어감)
- 단일 파일 스크립트
  - Maven: 미지원
  - Mill: `//|` 헤더로 지원

이 페이지에는 Maven과의 성능 수치 비교는 포함되어 있지 않고, 사용성/개발자 경험 위주로 서술됨. 정량적 성능 비교는 5절 참고.

---

### 3. Gradle과 비교: 프로그래머블 빌드

Gradle은 빌드를 "프로그래밍 가능하게" 만든 대표 도구임. Mill 문서는 Gradle의 프로그래머빌리티 자체는 인정하면서도, 그 구현 방식(커스텀 DSL + 전역 가변 변수)이 여러 실질적 문제를 낳는다고 지적함.

#### Gradle의 문제로 지적되는 지점

- 전용 플러그인 API를 새로 배워야 함
  - 간단한 "라인 수 세기" 태스크 하나를 만드는 데도 `task.named`, `task.register`, `dependsOn`, `inputs.files`, `outputs.file`, `doLast` 같은 Gradle 전용 개념을 익혀야 함
- 문자열 기반 설정
  - `"generateLineCount"`, `"generated-resources"`, `"processResources"` 같은 문자열 리터럴로 태스크와 설정을 참조함. 타입 안전성이 없어 오탈자나 리팩터링 누락에 취약함
- 발견하기 어려운 버그
  - 문서에 제시된 예제 코드에는 `inputs.files`가 `src/main/java`가 아니라 `src/main` 전체에 의존하도록 잘못 설정된 버그가 있음 → Java가 아닌 리소스 파일만 바뀌어도 불필요하게 태스크가 재실행됨. 문서는 이를 "가장 간단한 hello-world 커스터마이징조차 찾기 힘든 하이젠버그를 만들어낸다"고 표현함
- IDE 지원 미흡
  - Gradle 설정은 전역 가변 변수와 커스텀 DSL에 의존하기 때문에 IDE가 이를 제대로 분석하지 못함 → IntelliJ가 `compilerArgs` 같은 프로퍼티 존재는 인식해도 그 값이 어디서 왔는지, 무엇에 의존하는지, 어디서 쓰이는지는 추적하지 못해 결국 문서나 외부 자료에 의존하게 됨

---

### 4. Gradle 대비 Mill이 개선한 지점

#### 객체지향으로 회귀

Mill은 별도 플러그인 API 대신 표준 OOP 개념만으로 빌드를 표현함.

- 메서드(method): 빌드의 개별 단계(task) 정의
- 메서드 간 호출 그래프: 빌드 의존성 그래프 형성
- 클래스(class): 재사용 가능한 빌드 파이프라인 템플릿
- 오버라이드/서브클래스: 커스터마이징 수단

일반 메서드로 정의되는 태스크 예:

```scala
def lineCount = Task {
  allSourceFiles().map(f => os.read.lines(f.path).size).sum
}
```

다운스트림 태스크 커스터마이징은 메서드 오버라이드로 처리함:

```scala
override def resources = Task {
  os.write(Task.dest / "line-count.txt", "" + lineCount())
  super.resources() ++ Seq(PathRef(Task.dest))
}
```

#### IDE 경험

전역 변수가 아니라 클래스/메서드이기 때문에 IDE가 자연스럽게 다음을 지원함.

- 오버라이드 가능한 메서드에 대한 자동완성과 문서 표시
- 상속 계층을 따라가는 정의로 이동(jump to definition)
- 사용처 찾기(find usages)
- 외부 문서 없이도 빌드 로직을 인터랙티브하게 탐색

문서는 이를 "다른 대부분의 빌드 도구와 달리, Mill 빌드 파이프라인은 IDE에서 인터랙티브하게 탐색할 수 있다"고 강조함.

#### 자동으로 따라오는 이점

Mill의 모든 태스크는 별도 설정 없이 캐싱·무효화(invalidation)·병렬화를 자동으로 지원함. Gradle에서는 `inputs`/`outputs`/`dependsOn`을 수동으로 정확히 지정해야 하고, 이 과정에서 앞서 언급한 버그가 생기기 쉬움.

#### 재사용과 배포

커스텀 빌드 로직은 다음과 같이 재사용 가능함.

- 프로젝트 내부에서 공유 trait/class로 정의
- Maven Central에 플러그인 형태로 배포
- 여러 모듈에서 import하여 상속

#### 요약 비교

- 빌드 로직 표현
  - Gradle: 전용 DSL + 전역 가변 변수
  - Mill: 표준 OOP(메서드, 클래스, 오버라이드)
- 설정 참조 방식
  - Gradle: 문자열 기반(task 이름 등)
  - Mill: 타입 세이프한 메서드 참조
- 캐싱/병렬화
  - Gradle: `inputs`/`outputs`/`dependsOn` 수동 설정, 실수 시 버그 발생
  - Mill: 모든 태스크에 자동 적용
- IDE 지원
  - Gradle: 정의 추적/사용처 찾기 어려움
  - Mill: 자동완성, 정의로 이동, 사용처 찾기 모두 지원
- 커스텀 로직 재사용
  - Gradle: 플러그인 작성 필요
  - Mill: 일반 클래스 상속/오버라이드로 재사용, Maven Central 배포 가능

이 페이지 역시 Gradle과의 정량적 성능 비교 수치는 포함하지 않음. 실제 벤치마크는 아래 5, 6절에서 다룸.

---

### 5. 성능 벤치마크: Mill vs Maven

테스트 대상은 Netty 프로젝트(약 50만 줄, 서브모듈 50개)임.

- 순차 클린 컴파일(전체): Maven 98.78s, Mill 24.43s, 배속 4.0x
- 병렬 클린 컴파일(전체): Maven 50.67s, Mill 10.35s, 배속 4.9x
- 클린 컴파일(단일 모듈): Maven 6.33s, Mill 0.90s, 배속 7.0x
- 증분 컴파일(단일 모듈): Maven 6.76s, Mill 0.22s, 배속 30.7x
- No-op 컴파일(단일 모듈, 변경 없음): Maven 5.28s, Mill 0.12s, 배속 44.0x

- 증분 컴파일 격차는 Mill이 Zinc 증분 컴파일러를 사용해 파일 간 의존 관계를 분석하고 재컴파일이 필요한 범위를 정확히 좁히는 데서 옴. 파일 하나를 내부 구현만 바꿔 수정하면 Mill은 그 파일만(약 0.2초) 재컴파일하는 반면, Maven은 174개 소스 파일 전체를 다시 컴파일함.
- No-op 컴파일에서는 "아무것도 컴파일할 필요가 없다"는 사실을 확인하는 데 걸리는 시간 자체가 차이 남. Maven은 이를 판단하는 데만 약 5초가 걸리지만 Mill은 0.12초면 충분함.

---

### 6. 성능 벤치마크: Mill vs Gradle

테스트 대상은 Mockito 프로젝트(약 10만 줄, 서브프로젝트 22개)임.

- 순차 클린 컴파일(전체): Gradle 18.3s, Mill 5.0s, 배속 3.7x
- 병렬 클린 컴파일(전체): Gradle 14.8s, Mill 3.87s, 배속 3.6x
- 클린 컴파일(단일 모듈): Gradle 4.65s, Mill 0.89s, 배속 5.2x
- 증분 컴파일(단일 모듈): Gradle 1.61s, Mill 0.28s, 배속 5.9x
- No-op 컴파일(단일 모듈, 변경 없음): Gradle 1.02s, Mill 0.10s, 배속 10.2x

---

### 7. 성능 차이가 발생하는 이유

#### 컴파일 이론치와의 비교

Java는 통상 초당 1만~5만 줄 수준으로 컴파일됨. Mill의 측정치는 이 이론적 성능 범위에 부합하는 반면, Maven/Gradle은 그보다 훨씬 오래 걸림. 문서는 이를 다음과 같이 요약함.

> "Mill과 Maven은 동일한 작업을 수행하고 있다. Mill은 50만 줄의 Java 소스 코드를 컴파일하는 이 작업에 걸려야 할 만큼의 시간만 쓰는 반면, Maven은 그보다 훨씬 오래 걸린다."

즉 차이의 본질은 컴파일러 자체의 속도가 아니라, 빌드 도구가 컴파일 작업 주변에 얹는 오버헤드(오케스트레이션, 변경 감지, 태스크 그래프 평가 등)에 있음.

#### 증분 컴파일 (Zinc 통합)

Mill은 Zinc 증분 컴파일러와 통합되어 있어, public 시그니처 변경처럼 다운스트림 재컴파일이 필요한 경우에도 영향받는 파일만 보수적으로 좁혀 재컴파일함 → Maven의 전체 재컴파일 방식과 대비됨.

#### 추가로 제공되는 성능 관련 기능

- 선택적 테스트 실행(Selective Test Execution): PR 검증 시 변경과 무관한 테스트를 건너뜀
- 테스트 병렬화(Test Parallelism): 가용 코어 전체에 걸쳐 자동으로 스레드 분배
- 증분 Assembly Jar 생성: Assembly jar 생성 워크플로 가속
- 빌드 성능 프로파일: Chrome 프로파일을 자동 생성해 빌드 병목을 시각적으로 확인 가능

#### 측정 환경

M1 MacBook Pro(10 CPU / 32GB RAM), Mill 1.1.0 기준. 각 명령을 5회 수동 실행한 뒤 마지막 3회의 결과를 기록함. 재현용 테스트 프로젝트는 별도로 다운로드해 로컬에서 직접 검증 가능함.

---

### 8. 종합 정리

- 설정 철학
  - Maven: 선언적(XML), 보일러플레이트 과다
  - Gradle: 프로그래머블(커스텀 DSL), 전역 가변 상태
  - Mill: 선언적 + 프로그래머블(표준 OOP)
- 설치/실행 경험
  - Maven: Java/Maven 별도 설치, 실행 명령이 직관적이지 않음
  - Gradle: Gradle 별도 설치, wrapper 관리 필요
  - Mill: `./mill` 하나로 JVM까지 자동 관리, `run` 명령이 Python/Bash와 유사
- IDE 지원
  - Maven: 상대적으로 단순(플러그인 XML 기반)
  - Gradle: 전역 변수/DSL로 인해 추적 어려움
  - Mill: 클래스/메서드 기반이라 자동완성, 정의로 이동, 사용처 찾기 모두 지원
- 캐싱/병렬화
  - Maven: 기본 지원 미흡
  - Gradle: 수동 설정(`inputs`/`outputs`), 실수 시 버그
  - Mill: 모든 태스크에 자동 적용
- 클린 컴파일 성능(대형 프로젝트 기준)
  - Maven: Mill 대비 4.0x~7.0x 느림
  - Gradle: Mill 대비 3.6x~5.2x 느림
  - Mill: 기준
- 증분/No-op 컴파일 성능
  - Maven: Mill 대비 30.7x~44.0x 느림
  - Gradle: Mill 대비 5.9x~10.2x 느림
  - Mill: 기준

Mill 문서가 일관되게 강조하는 논지: Maven의 선언성과 Gradle의 프로그래머빌리티라는 두 축의 장점을 표준 OOP라는 이미 검증된 패러다임 위에서 동시에 확보하면서, 그 구현 방식(부트스트랩 스크립트, Zinc 증분 컴파일, 자동 캐싱) 덕분에 실측 빌드 시간에서도 우위를 보인다는 것임.

---

## Mill 마이그레이션

> 원본: https://mill-build.org/mill/migrating/migrating.html, https://mill-build.org/mill/migrating/auto-migrating.html

---

### 목차

1. [마이그레이션 개요](#1-마이그레이션-개요)
2. [예상 소요 시간](#2-예상-소요-시간)
3. [마이그레이션 4단계 전략](#3-마이그레이션-4단계-전략)
4. [하위 프로젝트를 Module로 변환](#4-하위-프로젝트를-module로-변환)
5. [서드파티 의존성 변환](#5-서드파티-의존성-변환)
6. [서드파티 플러그인·커스텀 로직 변환](#6-서드파티-플러그인커스텀-로직-변환)
7. [자주 겪는 문제와 해결](#7-자주-겪는-문제와-해결)
8. [마이그레이션 이후 정리](#8-마이그레이션-이후-정리)
9. [자동 마이그레이션 도구 (`mill init`)](#9-자동-마이그레이션-도구-mill-init)
10. [`mill init` 실전 사례](#10-mill-init-실전-사례)
11. [`mill init`의 한계](#11-mill-init의-한계)
12. [`mill init` 이후 트러블슈팅](#12-mill-init-이후-트러블슈팅)

---

### 1. 마이그레이션 개요

Maven/Gradle/sbt로 작성된 기존 빌드를 Mill로 옮기는 작업은 단순 변환이 아니라 프로젝트 구조·의존성·플러그인·커스텀 로직을 하나씩 대응시키는 과정임. Mill은 이 과정을 돕기 위해 `./mill init` 자동 임포터를 제공하지만, 이 도구는 뼈대만 만들어줄 뿐이며 나머지는 수작업 변환이 필요함.

핵심 메시지는 두 가지.

- 대부분의 빌드 시스템은 실제로는 소수의 서드파티 플러그인과 적은 양의 커스텀 로직으로 이루어져 있음 → 기술적 난이도 자체는 높지 않음
- 프로젝트가 오래되고 활발하게 유지보수될수록 Mill로 옮겼을 때 얻는 빌드 속도·사용성 이득이 누적되어 투자 가치가 커짐

### 2. 예상 소요 시간

프로젝트 규모와 모듈 수에 따라 마이그레이션 시간은 크게 달라짐.

- JimFS: 규모 약 26k LOC, 모듈 수 1, 예상 소요 약 2시간
- Apache Commons-IO: 규모 약 100k LOC, 모듈 수 1, 예상 소요 약 2시간
- Gatling: 규모 약 70k LOC, 모듈 수 21, 예상 소요 약 1일
- Arrow: 규모 약 60k LOC, 모듈 수 22, 예상 소요 약 5일
- Mockito: 규모 약 100k LOC, 모듈 수 22, 예상 소요 약 5일
- Netty: 규모 약 500k LOC, 모듈 수 47, 예상 소요 약 5일

단일 모듈 프로젝트는 코드량이 많아도 몇 시간 내로 끝나는 반면, 모듈 수가 많은 다중모듈 프로젝트는 모듈 간 의존성 그래프를 다시 구성해야 함 → 규모(LOC)보다 모듈 개수가 소요 시간에 더 큰 영향을 줌.

### 3. 마이그레이션 4단계 전략

기존 빌드를 곧바로 지우지 않고 두 빌드 시스템을 병행하면서 단계적으로 전환하는 방식을 권장함.

1. 기존 빌드 보존: Maven/Gradle/sbt 설정을 그대로 두어 언제든 롤백 가능한 상태를 유지함
2. Mill 병렬 구성: 별도로 Mill 빌드를 설정하고, 하위 프로젝트를 Mill Module로 옮기고, 서드파티 의존성을 이전함
3. Mill을 기본값으로 전환: 일정 기간 두 시스템을 동시에 운용하며 CI/로컬에서 Mill 결과를 검증함
4. 기존 빌드 제거: 충분히 검증된 뒤 Maven/Gradle/sbt 설정 파일을 삭제함

이 단계별 접근 덕분에 마이그레이션 도중 문제가 생겨도 원래 빌드로 즉시 되돌아갈 수 있음.

### 4. 하위 프로젝트를 Module로 변환

언어별로 대응되는 Mill Module 트레이트가 다름.

- Java: 사용할 Module `MavenModule`, 테스트 Module `MavenTests`
- Kotlin: 사용할 Module `KotlinMavenModule`, 테스트 Module `KotlinMavenTests`
- Scala: 사용할 Module `SbtModule`, 테스트 Module `SbtTests`

```scala
// Java 프로젝트
object foo extends MavenModule{
  object test extends MavenTests{
  }
}
```

```scala
// Kotlin 프로젝트
object foo extends KotlinMavenModule{
  object test extends KotlinMavenTests{
  }
}
```

```scala
// Scala 프로젝트
object foo extends SbtModule{
  object test extends SbtTests{
  }
}
```

디렉터리 중첩 구조(예: `bar/qux/`)는 Module을 중첩 `object`로 그대로 반영함.

```scala
object bar extends MavenModule {
  object qux extends MavenModule
}
```

모듈 간 의존성은 `moduleDeps`로 선언함. 테스트 모듈에서는 상위 Module이 이미 가진 `moduleDeps`를 잃지 않도록 `super.moduleDeps`에 이어붙임.

```scala
object foo extends MavenModule{
  object test extends MavenTests{
  }
}

object bar extends MavenModule{
  def moduleDeps = Seq(foo)
  object test extends MavenTests{
    def moduleDeps = super.moduleDeps ++ Seq(foo)
  }
}
```

변환한 구조가 원본과 맞는지는 아래 명령으로 확인함.

```bash
./mill visualize _.compile_     # 모듈 의존 그래프를 SVG로 시각화
./mill show _.sources           # 각 모듈이 인식하는 소스 폴더 확인
```

### 5. 서드파티 의존성 변환

빌드 도구마다 의존성 선언 문법이 다르지만, groupId/artifactId/version 세 값을 옮기는 것은 동일함.

```xml
<!-- Maven -->
<dependency>
  <groupId>com.google.guava</groupId>
  <artifactId>guava</artifactId>
  <version>3.3.1-jre</version>
</dependency>
```

```groovy
// Gradle
implementation "com.google.guava:guava:3.3.1-jre"
```

```scala
// sbt
libraryDependencies += "com.google.guava" % "guava" % "3.3.1-jre"
```

```scala
// Mill
def mvnDeps = Seq(mvn"com.google.guava:guava:3.3.1-jre")
```

Scala 크로스 빌드 의존성(sbt의 `%%`)은 Mill에서 더블 콜론(`::`)으로 바뀜.

```scala
// sbt
libraryDependencies += "com.lihaoyi" %% "scalatags" % "0.12.0"

// Mill
def mvnDeps = Seq(mvn"com.lihaoyi::scalatags:0.12.0")
```

의존성 범위(scope)는 오버라이드하는 def로 구분함.

```scala
def mvnDeps = Seq(...)          // 일반(compile+runtime) 의존성
def compileMvnDeps = Seq(...)   // 컴파일 전용 (Maven의 provided에 대응)
def runMvnDeps = Seq(...)       // 런타임 전용
```

테스트 전용 의존성은 `object test` 안에 동일한 방식으로 선언함.

### 6. 서드파티 플러그인·커스텀 로직 변환

Maven/Gradle/sbt 플러그인은 대부분 Mill의 내장 기능으로 대체 가능함.

- 린팅: 각 언어별(Java/Scala/Kotlin) 린팅 문서 참고
- 테스트: 언어별 테스트 설정 문서 참고
- 발행(publish): 패키징·발행 가이드 참고
- 그 외: Mill Contrib 모듈 또는 서드파티 플러그인 활용

커스텀 빌드 로직(스크립트 태스크 등)은 Mill의 커스텀 빌드 로직 메커니즘으로 옮김.

- 파일 시스템 조작: OS-Lib
- JSON/바이너리 직렬화: uPickle
- HTTP 요청: Requests-Scala
- Maven Central의 임의 라이브러리를 직접 import해서 빌드 로직에 사용 가능(Import Libraries and Plugins 문서 참고)
- 더 복잡한 통합이 필요하면 Running Dynamic JVM Code 문서 참고

### 7. 자주 겪는 문제와 해결

- 테스트가 리소스 파일에 접근 못함(샌드박스 실행 때문): `resources/` 대신 `$MILL_TEST_RESOURCE_DIR` 환경변수 사용
- 프로젝트 루트 경로가 필요함: `mill.api.BuildCtx.workspaceRoot` 또는 `WorkspaceRoot.workspaceRoot` 사용
- JVM 버전을 프로젝트별로 다르게 지정해야 함: Managing JVM Versions 문서 참고
- 컴파일 플래그를 세밀하게 제어해야 함: 선언적 설정(Compilation & Execution Flags) 문서 참고
- 어노테이션 프로세서 설정: Annotation Processors 문서 참고
- JNI 네이티브 코드 빌드: Native C Code with JNI 문서 참고
- Spring Boot/Micronaut 같은 프레임워크 통합: 해당 프레임워크 전용 가이드 참고

### 8. 마이그레이션 이후 정리

빌드가 정상 동작하는 것을 확인한 뒤에는 유지보수성을 높이는 방향으로 다듬음.

- 공통 설정 추출: 여러 모듈에서 반복되는 설정을 trait으로 뽑아 상속시킴

```scala
trait CommonConfig extends MavenModule {
  def scalaVersion = "3.3.0"
}
```

- Multi-File Builds: 빌드 로직을 소스 코드와 같은 위치에 파일 단위로 분리해 관리함
- 커스텀 플러그인 작성: 조직 내에서 반복되는 빌드 로직을 재사용 가능한 플러그인으로 만듦

---

### 9. 자동 마이그레이션 도구 (`mill init`)

`./mill init`은 Maven/Gradle/sbt 빌드를 감지해 Mill 빌드로 자동 변환하는 명령어. 완전한 마이그레이션을 보장하지는 않고, 초기 뼈대(스캐폴딩)를 생성해주는 역할.

빌드 도구별 인식 방식:

- Maven: 감지 대상 `pom.xml`, 변환 결과 `MavenModule`(하위에 `src/test`가 있으면 중첩 `test` Module도 함께 생성)
- Gradle: 감지 대상 `.gradle` / `.gradle.kts` 파일, 변환 결과 해당 Module + 테스트 프레임워크 자동 구성
- sbt: 감지 대상 sbt 프로젝트 구조, 변환 결과 `SbtModule`, Scala 소스 자동 처리

기본 사용법:

```bash
./mill init
```

실행하면 다음이 생성됨.

- 루트 모듈용 `build.mill`
- 하위(중첩) 모듈용 `package.mill`
- 공유 설정을 담는 `mill-build` 디렉터리

JVM 버전을 지정하고 싶으면 옵션을 추가함.

```bash
./mill init --mill-jvm-id 25
```

이 값은 생성되는 `build.mill`의 `mill-jvm-version` 설정에 반영됨. 전체 옵션은 다음으로 확인함.

```bash
./mill init --help
```

`init`은 여러 번 실행해도 되며, 옵션을 바꿔가며 재생성 가능함.

### 10. `mill init` 실전 사례

#### Maven 프로젝트 (davidmoten/geo)

```bash
> git init .
> git remote add -f origin https://github.com/davidmoten/geo.git
> git checkout 0.8.1

> ./mill init
converting Maven build
...
init completed, run "mill resolve _" to list available tasks

> ./mill __.compile
compiling 9 Java sources to ...geo...
compiling 2 Java sources to ...geo-mem...

> ./mill __.test
Test run com.github.davidmoten.geo.GeoHashTest finished: 0 failed, ...
```

#### Gradle 프로젝트 (komamitsu/fluency)

```bash
> git init .
> git remote add -f origin https://github.com/komamitsu/fluency.git
> git checkout 2.7.3

> ./mill init
converting Gradle build
...
init completed, run "mill resolve _" to list available tasks

> ./mill __.compile
compiling 9 Java sources to ...fluency-aws-s3...
compiling 6 Java sources to ...fluency-aws-s3...test...

> ./mill fluency-core.test
Test org.komamitsu.fluency.flusher.FlusherTest finished, ...
```

#### sbt 프로젝트 (scala/scala3-example-project)

```bash
> git init .
> git remote add -f origin https://github.com/scala/scala3-example-project.git
> git checkout 853808c50601e88edaa7272bcfb887b96be0e22a

> ./mill init
converting sbt build
...
init completed, run "mill resolve _" to list available tasks

> ./mill compile
compiling 13 Scala sources to ...

> ./mill test
MySuite:
  + example test that succeeds ...
```

#### 대규모 다중모듈 프로젝트 (iluwatar/java-design-patterns, 363개 서브프로젝트)

```bash
> git init .
> git remote add -f origin https://github.com/iluwatar/java-design-patterns
> git checkout ede37bd05568b1b8b814d8e9a1d2bbd71d9d615d

> ./mill init --mill-jvm-id 21
# Java 21 이상 필요

> rm twin/src/test/java/com/iluwatar/twin/BallThreadTest.java
> rm actor-model/src/test/java/com/iluwatar/actor/ActorModelTest.java

> ./mill __.compile
compiling 6 Java sources to ...service-to-worker...
compiling 4 Java sources to ...update-method...

> ./mill acyclic-visitor.test
Test com.iluwatar.acyclicvisitor.ZoomTest#testAcceptForUnix() started
Test com.iluwatar.acyclicvisitor.ZoomTest#testAcceptForUnix() finished, took ...
```

363개 서브프로젝트 규모에서도 `init` 한 번과 소수의 수동 정리(호환 안 되는 테스트 삭제 등)만으로 컴파일·테스트가 통과함 → 자동 임포터의 실용성을 보여줌.

### 11. `mill init`의 한계

자동 변환이 다루지 못하는 영역이 명확히 존재함.

- 빌드 확장(Build Extensions): 커스텀 플러그인, 커스텀 태스크는 옮겨주지 않음
- 빌드 프로필(Build Profiles): Maven profile 같은 조건부 빌드 설정은 변환 대상이 아님
- 비Java 소스(Non-Java Sources): Java 계열 이외 언어의 소스는 자동 처리되지 않음

이런 부분은 아래 방법으로 직접 보완해야 함.

- Mill Contrib 또는 서드파티 플러그인 구성
- 커스텀 플러그인 작성
- 커스텀 태스크 정의
- 커스텀 크로스 모듈 활용

### 12. `mill init` 이후 트러블슈팅

#### 모듈/태스크 이름 충돌

모듈 이름과 태스크 이름이 겹치면 컴파일 오류가 남. 모듈명을 바꾸거나 백틱으로 감싸서(예: `` `moduleName` ``) 충돌을 피함.

#### JPMS(Java Platform Module System) 오류

```
error: module not found: org.apache.commons.compress
```

이런 오류가 나면 `javacOptions`에 필요한 Java 모듈 옵션(`--add-modules` 등)을 추가해야 함.

#### 테스트 컴파일 오류

원인은 대개 둘 중 하나.

- 지원되지 않는 테스트 프레임워크 버전을 쓰고 있음 → 최신 버전으로 업그레이드
- `compileMvnDeps`로 선언한 의존성이 중첩 테스트 모듈의 전이 의존성에 포함되지 않음 → 필요한 의존성을 `mvnDeps` 또는 `runMvnDeps`에 명시적으로 추가

#### sbt 프로젝트의 런타임 오류

```
java.io.IOError: java.lang.RuntimeException: /packages cannot be represented as URI
```

이 오류는 오래된 sbt/Java 조합에서 발생하는 경우가 많음. 프로젝트의 sbt 버전을 최신(권장: v1.10.10 이상)으로 올리고 적절한 Java 버전을 확인한 뒤 재시도함.

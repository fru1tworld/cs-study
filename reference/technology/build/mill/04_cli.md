# Mill CLI

> 원본: https://mill-build.org/mill/cli/installation-ide.html, https://mill-build.org/mill/cli/flags.html, https://mill-build.org/mill/cli/builtin-commands.html, https://mill-build.org/mill/cli/query-syntax.html, https://mill-build.org/mill/cli/build-header.html

---

## 목차

1. [설치 & IDE 연동](#1-설치--ide-연동)
2. [CLI 플래그](#2-cli-플래그)
3. [내장 명령어](#3-내장-명령어)
4. [태스크 쿼리 문법](#4-태스크-쿼리-문법)
5. [build 파일 헤더 설정](#5-build-파일-헤더-설정)

---

## 1. 설치 & IDE 연동

### 1.1 부트스트랩 스크립트

Mill의 표준 설치 방식은 `./gradlew`, `./mvnw`와 같은 방식의 `./mill` 부트스트랩 스크립트를 프로젝트 루트에 두는 것이다. 이 스크립트는 build 헤더의 `mill-version` 등을 읽어 프로젝트에 맞는 Mill 버전을 자동으로 판단하고, 로컬에 없으면 자동 다운로드한다.

```bash
# Mac/Linux
curl -L https://repo1.maven.org/maven2/com/lihaoyi/mill-dist/1.1.7/mill-dist-1.1.7-mill.sh -o mill
chmod +x mill
./mill version

# Windows
curl.exe -L https://repo1.maven.org/maven2/com/lihaoyi/mill-dist/1.1.7/mill-dist-1.1.7-mill.bat -o mill.bat
./mill version
```

주의: PowerShell 5.1은 `.mill-version` 파일을 UTF-16LE로 생성해 Mill이 읽지 못한다. PowerShell 7 이상 사용 권장.

프로젝트 루트에 부트스트랩 스크립트를 커밋해두면 신규 기여자가 별도 설치 없이 바로 `./mill`을 실행할 수 있고, CI(Jenkins, GitHub Actions 등)에서도 버전 불일치 문제를 줄일 수 있다.

```bash
./mill --version
./mill __.compile   # 이중 언더스코어: 모든 모듈 컴파일
```

신규 프로젝트를 시작할 때는 `mill init`으로 예제 템플릿을 받아 시작하는 것을 권장한다.

### 1.2 글로벌 설치

```bash
sudo curl -L https://repo1.maven.org/maven2/com/lihaoyi/mill-dist/1.1.7/mill-dist-1.1.7-mill.sh -o /usr/local/bin/mill
sudo chmod +x /usr/local/bin/mill
```

특정 프로젝트에 속하지 않은 Java/Scala/Kotlin 스크립트 파일을 실행할 때 유용하다. 다만 프로젝트 내에서는 개별 부트스트랩 스크립트 방식을 권장한다.

### 1.3 JVM 버전

Mill은 시스템에 설치된 `java`와 무관하게 기본으로 `zulu:25`를 사용한다. build 헤더에서 명시적으로 지정할 수 있다.

```scala
//| mill-jvm-version: temurin:24.0.1
```

- 이 값은 `build.mill` 평가와 `JavaModule`/`ScalaModule`/`KotlinModule` 실행 모두에 적용된다.
- 모듈별로 다른 JVM 버전을 쓰려면 모듈 단위 설정 사용.
- Mill 자체 실행에는 최소 Java 17 필요, 사용자 정의 모듈은 Java 11부터 지원.
- 시스템 Java를 쓰려면 `mill-jvm-version: system` (비권장 — 개발/CI/운영 환경 간 JVM 불일치로 디버깅이 어려워질 수 있음).

### 1.4 Native 런처 vs JVM 런처

기본 Mill 런처는 GraalVM Native Image로, Java가 없는 환경에서도 빠르게 실행된다.

| OS | Intel | ARM |
|---|---|---|
| Windows | Y | N |
| Mac | Y | Y |
| Linux | Y | Y |

Windows-ARM은 업스트림 Graal Native Image 제약으로 미지원. 네이티브 미지원 플랫폼에서는 JVM 기반 런처를 사용하며, 버전에 `-jvm` 접미사를 붙이면 다른 플랫폼에서도 JVM 런처를 선택할 수 있다.

```
1.1.7-jvm
```

JVM 런처는 네이티브 대비 시작 오버헤드가 약 100ms 있고, Java 17 이상이 전역에 설치되어 있어야 한다.

### 1.5 탭 완성

```bash
./mill mill.tabcomplete/install
```

`~/.local/share/mill/completion/mill-completion.sh`에 스크립트를 생성하고 `~/.bash_profile`, `~/.zshrc`에 자동 등록한다. 이후 `./mill <TAB>`으로 모듈/태스크를, `./mill -<TAB>` 또는 `./mill --<TAB>`으로 플래그를 자동완성할 수 있다. `mainargs` 기반 커스텀 커맨드의 파라미터도 자동완성 대상이다.

```bash
./mill mill.tabcomplete/install --dest <path>   # 경로를 직접 지정, 등록은 하지 않음
```

### 1.6 GitHub 문법 강조

GitHub는 아직 `.mill` 파일 문법 강조를 기본 지원하지 않으므로 `.gitattributes`에 다음을 추가한다.

```
*.mill linguist-language=Scala
```

### 1.7 IDE 연동

Mill은 IntelliJ, VSCode를 포함해 표준 Build Server Protocol(BSP)을 지원하는 모든 클라이언트와 연동된다. Eclipse는 BSP를 지원하지 않으므로 별도의 프로젝트 파일 생성 명령을 제공한다.

**IntelliJ IDEA**

- 무료 IntelliJ Scala Plugin 필요 (Java/Kotlin 프로젝트라도 `build.mill` 자체가 Scala라서 필요).
- `build.mill`이 있는 폴더를 열면 자동 인식되며, 다중 빌드 시스템이 감지되면 `BSP`를 선택.
- build 변경 후에는 "BSP" 탭 → Refresh.
- 명시적 BSP 설정 파일 생성: `./mill --bsp-install`
- BSP 모드의 기본 출력 디렉터리는 `.bsp/mill-bsp-out`이며, 일반 빌드와 분리되어 컴파일이 두 번 일어난다. `out/`을 공유하려면 `MILL_NO_SEPARATE_BSP_OUTPUT_DIR=1` 환경변수 설정.
- BSP 대신 XML 프로젝트 파일을 직접 생성하려면:

```bash
./mill mill.idea/
```

`.idea/` 하위에 `scala_settings.xml`, `mill_modules/*.iml`, `libraries/*.xml`, `workspace.xml` 등이 생성된다. `build.mill` 변경 후에는 동일 명령을 재실행.

**VSCode**

- 무료 Metals Scala language server 필요.
- Java, Scala만 지원 (Kotlin 미지원 — Kotlin은 IntelliJ Community Edition 권장).
- `build.mill`이 있는 폴더를 열면 임포트 여부를 물어보며, 승인 시 자동 로드.
- 변경 후 반영: BSP 탭 → Refresh.

**Eclipse IDE**

- BSP 미지원. Java Development Tools(JDT) 필요.
- Scala, Kotlin 미지원 (Scala plugin 부재로 `build.mill` 자체도 지원 안 됨).

```bash
./mill mill.eclipse/
```

`.settings/org.eclipse.core.resources.prefs`, `.settings/org.eclipse.jdt.core.prefs`, `.classpath`, `.project` 생성. IntelliJ와 달리 프로젝트 구조에 따라 여러 개의 Eclipse 프로젝트가 생성될 수 있고, 이들 간 의존관계가 있을 수 있다. 이후 변경 시 동일 명령 재실행 후 Eclipse에서 F5로 새로고침.

---

## 2. CLI 플래그

Mill 실행 시 플래그를 받을 수 있는 대상은 네 가지로 구분된다.

| 대상 | 예시 | 설정 위치 |
|---|---|---|
| 실행할 태스크 | `foo.run --text hello` | 커맨드라인에서 직접 전달 |
| 태스크를 실행하는 JVM | `java -Xss10m -Xmx10G` | Compilation and Execution Flags |
| Mill 빌드 도구 프로세스 | `./mill --jobs 10` | 커맨드라인, build 헤더의 `mill-opts`, `.mill-opts` 파일 |
| Mill 빌드 도구를 실행하는 JVM | `java -Xss10m -Xmx10G` | `JAVA_OPTS`, build 헤더의 `mill-jvm-opts`, `.mill-jvm-opts` 파일 |

플래그를 넘길 때 태스크/Mill 자체/JVM 중 어디로 향하는지 명확히 구분해야 한다.

### 2.1 기본 플래그 (`mill --help`)

| 플래그 | 형식 | 설명 |
|---|---|---|
| `-D, --define <k=v>` | 문자열 | 시스템 속성 정의 또는 덮어쓰기 |
| `-L, --meta-level <int>` | 정수 | 실행할 메타레벨 선택. 0은 `build.mill`, 1은 `mill-build/build.mill`, 음수(-1, -2, ...)는 더 깊은 메타빌드 |
| `--color <bool>` | bool | 색상 출력 토글. 기본은 대화형 콘솔이거나 `FORCE_COLOR` 설정 시 활성, `NO_COLOR` 설정 시 비활성 |
| `--help` | bool | 도움말 출력 |
| `--help-advanced` | bool | 내부/고급 플래그 출력 |
| `--import <str>` | 문자열 | Mill에 로드할 추가 ivy 의존성(플러그인 등) |
| `-j, --jobs <str>` | 문자열 | 병렬 스레드 수. 정수(`5`), 코어 비율식(`0.5C`), 코어 대비 뺄셈식(`C-2`) 지원. `1`은 병렬성 비활성, 기본값 `0`은 코어당 스레드 1개 |
| `-k, --keep-going` | bool | 빌드 실패 후에도 계속 진행 |
| `--no-daemon` | bool | 장기 실행 백그라운드 데몬 없이 실행 |
| `--replay-logs` | bool | 캐시된 태스크의 로그 재생 |
| `--ticker <bool>` | bool | 실행 중 태스크 정보를 보여주는 ticker 로그 on/off |
| `-v, --version` | bool | Mill 버전 정보 출력 후 종료 |
| `-w, --watch` | bool | 입력이 변경되면 태스크 자동 재실행 |
| `task <str>...` | - | 실행할 태스크 이름 또는 쿼리 |

### 2.2 고급 플래그 (`mill --help-advanced`)

일반적인 사용에는 필요하지 않은 내부/고급 플래그이며 "자기 책임 하에" 사용해야 한다.

| 플래그 | 설명 |
|---|---|
| `<bool>` (파일 워칭 방식) | 폴링 대신 파일시스템 기반 워칭 사용 여부 (기본 true) |
| `--allow-positional` | `--arg` 없이 위치 기반으로 커맨드 인자 전달 허용 |
| `-b` | 실행 성공 시 벨 1회, 실패 시 2회 |
| `--bsp` | BSP 서버 모드 활성화 (BSP 클라이언트가 Mill BSP 서버를 시작할 때 사용) |
| `--bsp-install` | `.bsp/` 아래 `mill-bsp.json` 생성 |
| `--bsp-watch <bool>` | BSP 서버 실행 중 소스 변경 시 자동으로 build 리로드 (기본 true) |
| `-d` | STDOUT에 디버그 출력 표시 |
| `--disable-ticker` | 사용 중단됨. `--ticker false` 사용 권장 |
| `-i, --interactive` | `--no-daemon`의 별칭 (Mill 1.1.0부터는 대화형 명령에 더 이상 필수 아님) |
| `--no-build-lock` | Mill 출력 디렉터리에 배타적 락 없이 태스크 평가 |
| `--no-filesystem-checker` | 평가 중 허용되지 않은 파일/폴더 접근 검사를 전역 비활성화 (위험을 감수해야 하는 예외 상황용) |
| `--no-server` | 사용 중단됨. `--no-daemon` 사용 권장 |
| `--no-wait-for-build-lock` | 출력 디렉터리 락을 기다리지 않고 평가 |
| `--offline` | 오프라인 작업 시도 (best-effort, 완전 보장은 아님) |
| `--tab-complete` | 탭 완성 모드로 Mill 실행 |
| `--use-file-locks` | PID 기반 대신 전통적인 파일 기반 락 사용 (크래시 후 락 확보 시 레이스 컨디션 제거, 단 docker mount 등 락 미지원 파일시스템에서 문제 가능) |

### 2.3 주요 플래그 상세

**`--interactive` / `-i` / `--no-server`**: 현재는 모두 `--no-daemon`의 별칭. Mill 1.1.0부터 `ScalaModule#console`, `ScalaModule#repl` 같은 대화형 태스크에 별도 플래그가 필요 없으며, stdin/stdout 직결이 필요할 때 `--no-daemon`만으로 충분하다.

**`--watch` / `-w`**: 태스크의 입력이 바뀔 때마다 자동 재평가.

```bash
mill --watch foo.compile
mill -w foo.run
```

`build.mill`과 그 안의 import까지 감시 대상이다. `runBackground`와 조합하면 코드 변경 시 이전 프로세스를 강제 종료하고 재시작한다.

```bash
mill -w foo.runBackground
```

단, CTRL+C로 watch를 중단해도 `runBackground`가 띄운 서버는 백그라운드에 남는다. 완전히 종료하려면:

```bash
mill clean foo.runBackground
```

**`--jobs` / `-j`**: 병렬성 설정. 기본값은 시스템 코어 수.

```bash
mill -j4 __.compile      # 4개 스레드로 컴파일
mill -j0.5C __.compile   # 코어 수의 절반
mill -j2C __.compile     # 코어 수의 2배
```

빌드 헤더의 `mill-opts`에 설정해두면 개발 머신과 CI 머신처럼 코어 수가 다른 환경에서도 일관되게 적용할 수 있다.

---

## 3. 내장 명령어

Mill은 애플리케이션 코드 빌드와 직접 관련 없는 유틸리티 명령을 다수 내장하고 있다(`mill.main.MainModule` API 문서 참고). 아래 예시는 다음 `build.mill`을 기준으로 한다.

```scala
package build
import mill.*, javalib.*

trait MyModule extends JavaModule {
  object test extends JavaTests, TestModule.Junit4
}

object foo extends MyModule {
  def moduleDeps = Seq(bar)
  def mvnDeps = Seq(
    mvn"net.sourceforge.argparse4j:argparse4j:0.9.0"
  )
}

object bar extends MyModule {
  def mvnDeps = Seq(
    mvn"net.sourceforge.argparse4j:argparse4j:0.9.0",
    mvn"org.thymeleaf:thymeleaf:3.1.1.RELEASE",
    mvn"org.slf4j:slf4j-nop:2.0.7"
  )
}
```

| 명령 | 설명 |
|---|---|
| `resolve` | 쿼리에 매칭되는 태스크 목록을 실행 없이 출력 |
| `inspect` | `resolve`보다 상세하게 소스 위치와 입력 태스크 목록까지 출력 |
| `show` | 태스크 평가 결과(메타데이터)를 JSON으로 출력 |
| `showNamed` | `show`와 동일하되 결과가 항상 `{태스크명: 값}` 형태의 JSON 딕셔너리로 통일됨 |
| `path` | 두 태스크 사이의 의존성 체인 출력 |
| `plan` | 특정 태스크 실행 시 평가될 태스크들과 순서를 실행 없이 미리보기 |
| `clean` | 이전에 실행한 태스크의 캐시된 출력 삭제 |
| `visualize` | 태스크 집합의 의존관계를 `.svg`/`.png`/`.dot`/`.json`/`.txt`로 시각화 |
| `visualizePlan` | `visualize`와 유사하되 직접 resolve된 태스크 외에 전체 빌드 플랜까지 표시 |
| `init` | 예제 프로젝트나 Giter8 템플릿으로 프로젝트 생성 |
| `selective.*` | Selective Test Execution 관련 명령 |
| `shutdown` | 백그라운드 데몬 종료 |
| `version` | 현재 Mill 버전 출력 |

### 3.1 resolve

쿼리에 매칭되는 태스크를 나열만 하고 실행하지 않는다. 어떤 태스크가 실행될지 미리 확인하거나(dry run), 어떤 모듈/태스크가 있는지 탐색할 때 사용한다.

```bash
./mill resolve _
# foo
# bar
# clean
# inspect
# path
# plan
# resolve
# show
# shutdown
# version
# visualize
# visualizePlan

./mill resolve _.compile
# foo.compile
# bar.compile

./mill resolve foo._
# foo.allSourceFiles
# foo.allSources
# foo.artifactId
# foo.artifactName
# ...
```

와일드카드 `_`(단일 세그먼트), `__`(다중 세그먼트), 중괄호 열거 `{}`를 지원한다.

```bash
./mill resolve foo.{compile,run}
./mill resolve "foo.{compile,run}"
./mill resolve foo.compile foo.run
./mill resolve _.compile   # 모든 최상위 모듈의 compile 태스크
./mill resolve __.compile  # 모든 모듈의 compile 태스크(재귀)
./mill resolve _           # 최상위 모듈/태스크 전체
./mill resolve foo._       # foo 바로 아래 태스크 전체
./mill resolve __          # 모든 모듈/태스크(재귀)
```

### 3.2 inspect

`resolve`보다 상세하게 태스크의 소스 위치와 입력(inputs) 목록까지 보여준다. 빌드 구조를 디버깅하거나 대화식으로 탐색할 때 유용하다.

```bash
./mill inspect foo.run
# foo.run(RunModule...)
# Runs this module's code in a subprocess and waits for it to finish
# args <str>...
# Inputs:
# foo.finalMainClass
# foo.finalMainClassOpt
# foo.runClasspath
# foo.forkArgs
# foo.allForkEnv
# foo.forkWorkingDir
# foo.runUseArgsFile
```

`resolve`와 동일한 와일드카드 쿼리 문법을 지원하지만, 보통은 한 번에 한 태스크씩 inspect하는 경우가 많다.

### 3.3 show / showNamed

Mill은 기본적으로 태스크 평가 메타데이터를 출력하지 않는다. `show`로 특정 태스크의 결과값을 확인할 수 있다.

```bash
./mill show foo.sources
# [".../foo/src"]

./mill show foo.compile
# {
#   "analysisFile": ".../out/foo/compile.dest/...",
#   "classes": ".../out/foo/compile.dest/classes"
# }
```

JSON 출력이므로 `jq`나 외부 스크립트와 연동하기 쉽다. 여러 태스크를 조회하면 `{태스크명: 값}` 형태의 딕셔너리로 바뀐다.

```bash
./mill show 'foo.{sources,compileClasspath}'
# {
#   "foo.sources": [".../foo/src"],
#   "foo.compileClasspath": [..., ".../foo/compile-resources"]
# }
```

`showNamed`는 태스크가 하나든 여럿이든 항상 `{"태스크명": 결과}` 형태로 통일해 출력한다. 프로그램에서 결과를 다룰 때 일관된 구조가 필요하면 `showNamed`를 쓴다.

```bash
./mill showNamed 'foo.sources'
# { "foo.sources": [".../foo/src"] }
```

### 3.4 path

첫 번째 태스크와 두 번째 태스크 사이의 의존성 체인을 출력한다. 데이터가 어떻게 한 태스크에서 다른 태스크로 흘러가는지, 혹은 `mill foo`가 왜 예상 못한 `bar`까지 실행하는지 파악할 때 유용하다.

```bash
./mill path foo.assembly foo.sources
# foo.sources
# foo.allSources
# foo.allSourceFiles
# foo.compile
# foo.localRunClasspath
# foo.localClasspath
# foo.assembly
```

가능한 경로가 여럿이면 그중 하나가 임의로 선택된다.

### 3.5 plan

`mill foo`를 실행하면 평가될 태스크들과 순서를 실제 실행 없이 미리 보여준다.

```bash
./mill plan foo.compileClasspath
# foo.compileResources
# foo.unmanagedClasspath
# ...
# foo.compileMvnDeps
# ...
# foo.mvnDeps
# foo.compileClasspath
```

병렬성 때문에 출력 순서는 가능한 위상 정렬 중 하나일 뿐이며, 실제 실행 순서는 스케줄링이나 소요 시간에 따라 달라질 수 있다.

### 3.6 clean

이전에 실행한 태스크의 캐시된 출력을 삭제한다.

```bash
./mill clean             # 전체 출력 삭제
./mill clean foo         # foo 모듈(중첩 모듈 포함)의 모든 출력 삭제
./mill clean foo.compile # foo.compile 태스크의 출력만 삭제
./mill clean foo.{compile,run}
./mill clean "foo.{compile,run}"
./mill clean foo.compile foo.run
./mill clean _.compile
./mill clean __.compile
```

### 3.7 visualize / visualizePlan

`visualize`는 지정한 태스크 집합의 의존관계를 그래프로 그려준다.

```bash
./mill visualize foo._
# [
#   ".../out/visualize.dest/out.dot",
#   ".../out/visualize.dest/out.json",
#   ".../out/visualize.dest/out.png",
#   ".../out/visualize.dest/out.svg",
#   ".../out/visualize.dest/out.txt"
# ]
```

`.svg`/`.png`로 눈으로 확인할 수 있고, `.txt`/`.dot`/`.json`은 별도 도구로 후처리하기 쉽다.

```bash
./mill visualize __.compile
```

내부적으로 Transitive Reduction(전이 축약)을 적용해 중복 간선을 제거하고 그래프를 단순화한다. 따라서 실제로는 존재하지만 표시되지 않는 중복 간선이 있을 수 있다.

`visualizePlan`은 `visualize`와 비슷하지만 직접 resolve된 태스크뿐 아니라 전체 빌드 플랜(간접 의존성 포함)까지 보여준다. 직접 resolve된 태스크는 실선, 의존성은 점선으로 표시된다.

```bash
./mill visualizePlan foo.run
```

### 3.8 init

Mill 예제 프로젝트나 Giter8 템플릿을 기반으로 프로젝트를 생성한다.

```bash
./mill init
# Run `mill init <example-id>` with one of these examples as an argument to download and extract example.
# Run `mill init --show-all` to see full list of examples.
# Run `mill init <Giter8 template>` to generate project from Giter8 template.
# scalalib/basic/1-simple
# scalalib/web/1-web-example
# ...
# javalib/basic/1-simple
# ...
# kotlinlib/basic/1-simple
# ...
```

빈 폴더에 부트스트랩 스크립트를 받은 뒤 `./mill init`으로 원하는 것에 가장 가까운 예제를 풀어 시작하는 방식이 일반적이다. 기존 Maven/Gradle 빌드를 Mill 설정으로 초기화하는 용도로도 쓸 수 있다.

### 3.9 selective.*

Selective Test Execution(선택적 테스트 실행)을 위한 내장 명령들.

### 3.10 shutdown / version

```bash
./mill shutdown   # 백그라운드 데몬 종료 (미사용 30분 후 자동 종료되므로 필수는 아님)
./mill version    # 현재 Mill 버전 출력
```

---

## 4. 태스크 쿼리 문법

Mill CLI에서 태스크를 지정할 때, 실제로는 단순한 태스크 이름뿐 아니라 와일드카드·타입 필터 등을 포함한 **태스크 셀렉터 쿼리**를 받는다.

### 4.1 단일 태스크 선택

완전한 이름으로 태스크를 지정한다.

```bash
mill foo.compile
mill foo.run hello world
mill foo.testCached
```

### 4.2 경로 세그먼트

각 모듈/태스크는 고유한 경로를 가지며, 경로는 `.`으로 구분된 세그먼트로 구성된다.

- **label segment**: Scala 식별자 규칙과 동일 — 문자로 시작하고 문자, 숫자, `-`, `_`를 포함 가능. 모듈, 태스크, 외부 모듈의 Scala 패키지명 등을 표현.
- **cross segment**: label segment에 대괄호(`[`, `]`)가 붙어 크로스 모듈과 그 파라미터를 표현.
- 세그먼트는 괄호 `(`, `)`로 감쌀 수 있다. 타입 필터처럼 `.`을 포함하는 세그먼트를 쓸 때는 `.`이 경로 구분자로 오인되지 않도록 괄호로 감싸야 한다.

### 4.3 여러 태스크 선택

| 방법 | 문법 | 설명 |
|---|---|---|
| 열거(enumeration) | `{a,b}` | 중괄호로 여러 셀렉터를 나열 |
| 와일드카드 | `_`, `__` | 단일/다중 세그먼트 자리표시자 |
| 타입 필터 | `_:Type`, `__:Type` | 와일드카드에 타입 제약 추가 |
| 새 셀렉터 시작 | `+` | 서로 다른 파라미터로 여러 태스크 선택자를 연결 |

**열거(enumeration)**

```bash
{foo,bar}          # foo와 bar
foo.{compile,run}  # foo.compile과 foo.run
{_,foo.bar}.baz    # _.baz와 foo.bar.baz
```

일부 셸(bash 등)은 중괄호를 자체적으로 확장하므로, 쿼리를 따옴표로 감싸는 것이 안전하다.

```bash
mill "foo.{compile,run}"
```

**와일드카드**

- `_`: 단일 세그먼트를 대체하는 자리표시자.
- `__`: 여러 세그먼트(빈 세그먼트 포함)를 대체하는 자리표시자.

```bash
_._._.jar   # 빌드 트리 3단계에 있는 모든 jar 태스크
```

**타입 필터**

와일드카드는 콜론(`:`) 뒤에 타입 한정자를 붙여 특정 타입의 모듈만 매칭하도록 제한할 수 있다. 타입은 이름, 또는 그 상위 타입/패키지를 `.`으로 이어붙여 지정한다. `.`이 포함된 타입 한정자는 괄호로 감싸야 하며, `_root_` 패키지로 완전한 정규 타입명도 표현 가능하다.

```bash
mill resolve __:TestModule.jar
mill resolve "(__:scalalib.TestModule).jar"
mill resolve "(__:mill.scalalib.TestModule).jar"
mill resolve "(__:_root_.mill.scalalib.TestModule).jar"
```

타입 한정자 앞에 `^` 또는 `!`를 붙이면 해당 타입의 인스턴스가 "아닌" 것만 매칭한다.

```bash
mill resolve __:^TestModule.jar
```

타입 필터는 여러 개를 이어붙일 수 있다.

```bash
mill resolve "__:JavaModule:^ScalaModule:^TestModule.jar"
```

타입 필터는 현재 모듈 선택에만 지원되며, 태스크의 결과 타입 기준 필터링은 지원하지 않는다.

**`+`로 새 셀렉터 시작**

열거/와일드카드/타입 필터는 Mill이 하나의 복합 셀렉터 경로로 파싱하며, 이후에 오는 파라미터는 resolve된 모든 태스크에 동일하게 적용된다. 반면 `+`는 완전히 새로운 셀렉터 경로를 시작하며, 서로 다른 파라미터 목록을 지정할 수 있다. 이는 `JavaModule.run`처럼 자체 파라미터를 받는 command 태스크에서 중요하다.

```bash
mill foo.run hello                                   # foo.run에 hello 전달
mill {foo,bar}.run hello                             # foo.run, bar.run 각각에 hello 전달
mill __:JavaModule:^TestModule.run hello             # 테스트 모듈 제외 모든 Java 모듈의 run에 hello 전달
mill foo.run hello + bar.run world                   # foo.run에는 hello, bar.run에는 world
```

### 4.4 `.super`로 오버라이드된 태스크 선택

`.super`를 사용하면 오버라이드되기 전의 태스크를 호출할 수 있다. 이름이 겹치지 않으면 단독으로, 겹치면 클래스명을 붙여 명시적으로 선택한다.

```scala
package build
import mill.*, scalalib.*

object foo extends ScalaModule {
  def scalaVersion = "3.8.2"
}
```

```bash
./mill inspect foo.compile                              # 현재 compile 태스크
# foo.compile(ScalaModule.scala:...)

./mill inspect foo.compile.super                         # 오버라이드된 compile (모호하지 않으면 단축형)
# foo.compile.super.javalib.JavaModule(JavaModule.scala:...)

./mill inspect foo.compile.super.mill.javalib.JavaModule # mill.javalib.JavaModule의 compile
# foo.compile.super.javalib.JavaModule(JavaModule.scala:...)

./mill inspect foo.compile.super.javalib.JavaModule      # 클래스명 접미사로도 선택 가능(모호하지 않은 범위에서)
./mill inspect foo.compile.super.JavaModule              # 더 짧은 접미사도 가능
```

---

## 5. build 파일 헤더 설정

Mill은 `build.mill` / `build.mill.yaml` 본문이 컴파일되기 전에 적용되어야 하는 설정을 파일 최상단 헤더에 저장한다.

### 5.1 문법

**`build.mill.yaml`**: `mill-` 접두사가 붙은 키로 작성.

```yaml
mill-version: {mill-version}
mill-opts: ["--jobs=0.5C"]
mill-jvm-version: temurin:11
mill-jvm-opts: ["-XX:NonProfiledCodeHeapSize=250m", "-XX:ReservedCodeCacheSize=500m"]
mill-build:  # 메타빌드 설정
  repositories:
    - https://oss.sonatype.org/content/repositories/snapshots
  mvnDeps:
    - com.goyeau::mill-scalafix::0.5.1-14-4d3f5ea-SNAPSHOT
```

**`build.mill`**: 각 줄 앞에 `//|`를 붙인 YAML 주석 블록. YAML 주석과 빈 줄 허용.

```scala
//| mill-version: {mill-version}
//| # yaml 주석 허용
//| mill-opts: ["--jobs=0.5C"]
//| mill-jvm-version: temurin:11
//| mill-jvm-opts: ["-XX:NonProfiledCodeHeapSize=250m", "-XX:ReservedCodeCacheSize=500m"]
//|
//| # 메타빌드 설정
//| repositories:
//| - https://oss.sonatype.org/content/repositories/snapshots
//| mvnDeps:
//| - com.goyeau::mill-scalafix::0.5.1-14-4d3f5ea-SNAPSHOT
package build
...
```

### 5.2 헤더 키 목록

| 키 | 용도 |
|---|---|
| `mill-version` | 프로젝트가 사용할 Mill 버전. 없으면 `./mill`/`./mill.bat` 부트스트랩 스크립트의 버전 사용 |
| `mill-opts` | Mill 자체에 기본으로 전달할 플래그. 모든 사용자가 매번 입력하지 않아도 되도록 공유 |
| `mill-jvm-version` | 빌드 재현성을 위한 명시적 JVM 버전 지정 |
| `mill-jvm-index-version` | Coursier JVM Index 버전 오버라이드 |
| `mill-jvm-opts` | Mill 백그라운드 데몬 JVM 프로세스에 전달할 플래그(메모리 등) |
| `mvnDeps`, `repositories` | `build.mill`에서 사용할 의존성/저장소 정의 |
| 그 외 키 | The Mill Meta-Build의 태스크에 대한 오버라이드로 균일하게 취급 |

`mill-version`, `mill-opts`, `mill-jvm-version`, `mill-jvm-opts`는 구버전 Mill과의 호환을 위해 `.mill-version` 같은 개별 파일이나, XDG Base Directory 표준을 따르는 `.config` 폴더(`.config/mill-version` 등)에도 둘 수 있다.

### 5.3 mill-opts

프로젝트 태스크가 CPU를 많이 쓴다면, 모든 사용자가 코어당 0.5개 태스크만 동시 실행하도록 강제할 수 있다.

```scala
//| mill-opts: ["--jobs=0.5C"]
```

커맨드라인에서 `--jobs=10`처럼 명시적으로 전달하면 `.mill-opts` 값을 오버라이드한다. `mill-opts`는 Mill 자체에 전달하는 옵션이고, `mill-jvm-opts`는 Mill을 실행하는 JVM에 전달하는 옵션이다. Mill이 빌드하는 대상 프로젝트에 JVM 옵션을 전달하려면 Compilation and Execution Flags 쪽을 참고해야 한다.

### 5.4 mill-jvm-version

```scala
//| mill-jvm-version: temurin:17.0.6
```

`build.mill.yaml`에서는 최상위 키로 지정한다.

```yaml
mill-jvm-version: temurin:17.0.6
extends: JavaModule
```

```bash
> ./mill run
Java version: 17.0.6
```

지정하지 않으면 기본값 `zulu:25`가 사용된다. Mill 빌드 도구 자체는 최소 Java 17이 필요하지만, 사용자 정의 모듈은 Java 11까지 지원한다. 시스템 Java를 쓰려면 `mill-jvm-version: system`으로 지정할 수 있으나, 개발/CI/운영 환경 간 JVM 불일치로 디버깅이 어려운 문제가 생길 수 있어 권장하지 않는다.

### 5.5 mill-jvm-index-version

Mill은 Coursier JVM Index를 사용해 `temurin:11` 같은 부분 버전을 `temurin:11.0.28` 같은 구체적인 다운로드 버전으로 해석한다. JVM 인덱스 버전은 사용 중인 Mill 버전에 따라 정해지지만, 새로 릴리스된 JVM을 쓰고 싶거나 특정 과거 버전을 고정하고 싶을 때 오버라이드할 수 있다.

```scala
//| mill-jvm-index-version: 0.0.4-88-f9ba3d
//| mill-jvm-version: temurin:17
package build
import mill.*

def printJavaVersion() = Task.Command {
  println("Java version: " + System.getProperty("java.version"))
}
```

```bash
> ./mill printJavaVersion
Java version: 17.0.14
```

위 예시는 `mill-jvm-index-version`을 과거 버전으로 고정해 `temurin:17`이 최신 `17.0.17`이 아닌, 해당 인덱스 시점에 존재하던 `17.0.14`로 해석되도록 한 경우다.

### 5.6 mill-repositories

의존성 해석에 사용할 추가 Maven 저장소를 설정한다.

```scala
//| mill-repositories: ["https://oss.sonatype.org/content/repositories/releases"]
```

```yaml
mill-repositories:
  - https://oss.sonatype.org/content/repositories/releases
```

이 저장소는 다음 용도로 쓰인다.

- Mill 자체 데몬 JAR 해석
- `mill-jvm-version`을 위한 JVM 인덱스 해석
- `CoursierModule`을 상속하는 모든 모듈의 기본 저장소

설정한 저장소는 기본 Maven Central보다 우선순위가 높게(앞에) 추가된다. 개별 모듈 단위 저장소 설정은 Repository Configuration 참고.

### 5.7 mill-jvm-opts

Mill 런처(빌드 도구 자체를 실행하는 JVM)에 JVM 레벨 플래그를 전달한다. `JAVA_OPTS` 환경변수, build 헤더, 또는 프로젝트 루트의 `.mill-jvm-opts` 파일(한 줄에 하나씩) 중 하나로 설정 가능하다.

```bash
JAVA_OPTS='-Xss10m -Xmx10G' ./mill __.compile
```

```scala
//| mill-jvm-opts: ["-Xss10m", "-Xmx10G"]
```

환경변수 보간을 지원한다. 예: `-Dmy.jvm.property=${PWD}`. 존재하지 않는 환경변수는 빈 문자열로 치환된다.

### 5.8 메타빌드 태스크 오버라이드 (mvnDeps / repositories / 기타)

build 헤더로 The Mill Meta-Build(즉 `build.mill`을 컴파일하는 빌드)의 임의 태스크를 오버라이드할 수 있다. `mvnDeps`, `repositories`로 `build.mill`에서 쓸 라이브러리를 추가하는 것 외에도, `scalacOptions` 같은 다른 태스크를 오버라이드할 수 있다.

```scala
//| scalacOptions: ["-Wunused:privates", "-Xfatal-warnings"]
package build
import mill.*, javalib.*

private def foo = 1
```

```bash
> ./mill resolve _
error: ...unused private member
...No warnings can be incurred under -Werror
```

`build.mill`은 Scala로 작성되므로, `ScalaModule`에서 오버라이드 가능한 대부분의 태스크를 build 헤더에서도 지정할 수 있다.

`build.mill.yaml` 파일에서는 메타빌드 설정을 `mill-build:` 키 아래에 둔다.

```yaml
mill-build:
  scalacOptions: ["-verbose"]
```

```bash
> ./mill resolve _
[typer...]
[constructors...]
[erasure...]
[genBCode...]
```

### 5.9 단일 파일 프로젝트에서의 build 헤더

build 헤더 설정은 Single-File Scripts(단일 Java/Scala/Kotlin 파일 스크립트) 설정에도 동일하게 쓰이며, 파일 내부에 설정값을 가볍게 내장하는 문법으로 활용된다.

# sbt 활용: Triggered Execution, 스크립트, Server, 증분 컴파일

> 원본: https://www.scala-sbt.org/1.x/docs/Triggered-Execution.html, https://www.scala-sbt.org/1.x/docs/Scripts.html, https://www.scala-sbt.org/1.x/docs/sbt-server.html, https://www.scala-sbt.org/1.x/docs/Understanding-Recompilation.html

---

## 목차

1. [Triggered Execution (~명령)](#1-triggered-execution-명령)
2. [스크립트 모드](#2-스크립트-모드)
3. [sbt server와 BSP](#3-sbt-server와-bsp)
4. [증분 컴파일(Zinc) 원리](#4-증분-컴파일zinc-원리)

---

## 1. Triggered Execution (~명령)

### 1.1 개념

sbt 콘솔에서 명령 앞에 물결표(`~`)를 붙이면, 관련 소스 파일이 바뀔 때마다 그 명령을 자동으로 재실행한다. 파일을 저장할 때마다 수동으로 `compile`, `test`를 다시 입력하는 대신 sbt가 파일 시스템을 감시(watch)하다가 변경을 감지하면 즉시 태스크를 재실행하는 방식이다.

```
> ~ compile
```

위 명령을 실행하면 sbt는 `compile` 태스크가 참조하는 소스 파일들을 감시하다가, 파일이 바뀌는 즉시 다시 `compile`을 수행한다. 종료하려면 Enter를 누르면 된다.

### 1.2 자주 쓰는 조합

```
> ~ Test / compile
> ~ testQuick
> ~ testQuick foo.BarTest
> ~ testOnly foo.BarTest
> ~ test
```

- `~ Test / compile`: 테스트 스코프 컴파일만 감시
- `~ testQuick`: 이전 실행에서 실패했거나, 재컴파일된 클래스와 관련된 테스트만 실행
- `~ testOnly foo.BarTest`: 특정 테스트 클래스만 반복 실행

세미콜론으로 여러 명령을 묶어 한 번에 감시할 수도 있다.

```
> ~ clean; test
```

### 1.3 watch 대상 결정 방식

기본적으로 sbt는 실행하는 태스크가 **직접 의존하는 입력 파일**만 감시 대상으로 삼는다. 예를 들어 `~ compile`은 `Compile / sources`가 가리키는 파일들을 감시하며, 그 파일들과 무관한 리소스나 문서 파일은 감시하지 않는다.

태스크가 참조하지 않는 파일까지 감시하고 싶다면 `watchTriggers`에 글롭 패턴을 추가한다.

```
foo / watchTriggers += baseDirectory.value.toGlob / "*.txt"
```

빌드 정의 파일(`build.sbt`, `project/*.scala`) 자체의 변경까지 감시해 자동으로 빌드를 재로드하고 싶다면 다음을 설정한다.

```
Global / onChangedBuildSource := ReloadOnSourceChanges
```

이 설정을 켜면 빌드 소스가 바뀔 때 세션을 재시작할 필요 없이 sbt가 자동으로 빌드를 재로드한 뒤 triggered execution 모드로 복귀한다.

### 1.4 동작 커스터마이징 키

| 설정 키 | 역할 |
|---------|------|
| `watchTriggers` | 태스크가 직접 참조하지 않는 파일까지 감시 대상에 추가 |
| `watchTriggeredMessage` | 트리거가 발동했을 때 출력할 메시지 |
| `watchStartMessage` | 감시 대기 상태에서 보여줄 배너 |
| `watchInputOptions` | 감시 중 사용할 수 있는 키 입력 옵션 추가 |
| `watchInputParser` | 키 입력 이벤트를 어떻게 해석할지 정의 |
| `watchBeforeCommand` | 태스크 실행 직전에 실행할 콜백 (화면 클리어 등) |
| `watchOnIteration` | 매 반복 전에 실행할 함수 |
| `watchLogLevel` | 파일 모니터링 관련 로그 레벨 |
| `watchForceTriggerOnAnyChange` | 파일 내용이 바뀌지 않아도(타임스탬프만 바뀌어도) 트리거할지 여부 (기본값 false) |
| `watchPersistFileStamps` | 파일 해시를 디스크에 캐싱해 재시작 후에도 유지할지 여부 (기본값 false) |
| `watchAntiEntropy` | 같은 파일에 대한 재트리거를 억제하는 대기 시간 (기본값 500ms) |

화면을 매번 새로 지우고 싶다면 다음처럼 설정한다.

```
ThisBuild / watchTriggeredMessage := Watch.clearScreenOnTrigger
ThisBuild / watchBeforeCommand := Watch.clearScreen
```

감시 중 커스텀 키 입력을 추가하려면 다음처럼 옵션을 등록한다.

```
ThisBuild / watchInputOptions += Watch.InputOption('l', "reload", Watch.Reload)
```

### 1.5 종료

감시 도중 Enter 키를 누르면 트리거 루프가 끝나고 콘솔로 돌아간다. `?`를 입력하면 그 시점에 사용 가능한 입력 옵션 목록을 확인할 수 있다.

---

## 2. 스크립트 모드

### 2.1 개념

sbt는 일반 프로젝트 빌드 외에, 단일 Scala 파일을 스크립트처럼 즉시 컴파일·실행하는 대체 진입점을 제공한다. `ScriptMain`을 메인 클래스로 지정해 실행하면 셔뱅(`#!`) 라인으로 실행 가능한 스크립트 파일을 만들 수 있다. 다만 매 실행마다 sbt 자체의 부팅 비용이 들어가므로 시작 시간이 느리다는 단점이 있고, 이 기능은 실험적(experimental) 상태로 제공된다.

### 2.2 기본 예제

```scala
#!/usr/bin/env sbt -Dsbt.version=1.6.1 -Dsbt.main.class=sbt.ScriptMain -error

/***
ThisBuild / scalaVersion := "2.13.12"
libraryDependencies += "org.scala-sbt" %% "io" % "1.6.0"
*/

println("hello")
```

파일에 실행 권한을 부여하면 바로 실행할 수 있다.

```bash
chmod u+x script.scala
./script.scala
```

`/*** ... */` 블록 안에는 일반 `build.sbt`에 쓰는 것과 동일한 설정 문법(`scalaVersion`, `libraryDependencies` 등)을 그대로 적을 수 있다. sbt는 이 블록을 파싱해 필요한 Scala 버전과 라이브러리를 내려받은 뒤 나머지 코드를 컴파일하고 실행한다.

### 2.3 인자를 받는 스크립트 예제

```scala
#!/usr/bin/env sbt -Dsbt.version=1.6.1 -Dsbt.main.class=sbt.ScriptMain -error

/***
ThisBuild / scalaVersion := "2.13.12"
libraryDependencies += "org.scala-sbt" %% "io" % "1.6.0"
*/

import sbt.io.IO
import sbt.io.Path._
import sbt.io.syntax._
import java.io.File
import java.net.URI
import sys.process._

def file(s: String): File = new File(s)
def uri(s: String): URI = new URI(s)

def processFile(f: File): Unit = {
  val lines = IO.readLines(f)
  lines foreach { line =>
    println(line.toUpperCase)
  }
}

args.toList match {
  case Nil => sys.error("usage: ./script.scala <file>...")
  case xs  => xs foreach { x => processFile(file(x)) }
}
```

```bash
./script.scala script.scala
```

이 스크립트는 인자로 받은 파일의 각 줄을 대문자로 바꿔 출력한다. `args`는 커맨드라인 인자 리스트를 그대로 받는다.

### 2.4 제약

- 스크립트 모드는 실험적 기능이라 향후 문법이 바뀔 수 있다.
- 매번 sbt 부트스트랩 과정을 거치므로 일반 스크립트 언어(예: 셸 스크립트, Python)에 비해 시작 지연이 크다.
- 저장소 접근이 필요한 의존성을 선언하면 첫 실행 시 다운로드 시간이 추가된다.

---

## 3. sbt server와 BSP

### 3.1 개념

sbt server는 sbt 1.x부터 기본 내장된 기능으로, 하나의 sbt 세션을 백그라운드에 띄워두고 여러 클라이언트가 네트워크(로컬 소켓 또는 TCP)를 통해 명령을 주고받을 수 있게 한다. 콘솔에서 직접 명령을 입력하는 대신, 에디터나 IDE가 클라이언트가 되어 컴파일 결과·진단(diagnostics)·자동완성 같은 정보를 받아볼 수 있도록 설계되었다.

와이어 프로토콜은 **Language Server Protocol(LSP) 3.0** 스펙을 기반으로 하며, 메시지는 JSON-RPC 2.0 형식으로 주고받는다.

```
Content-Type: application/vscode-jsonrpc; charset=utf-8\r\n
Content-Length: ...\r\n
\r\n
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "textDocument/didSave",
  "params": { ... }
}
```

### 3.2 연결 방식

| 모드 | 대상 | 설정 |
|------|------|------|
| 도메인 소켓 / 명명된 파이프 (기본값, sbt 1.1.x+) | Unix domain socket / Windows named pipe | `serverConnectionType := ConnectionType.Local` |
| TCP | TCP 소켓 | `serverConnectionType := ConnectionType.Tcp` |

서버를 처음 발견하려면 `project/target/active.json` 파일을 읽는다. 도메인 소켓 모드(Unix)에서는 다음과 같은 형태다.

```json
{"uri":"local:///Users/someone/.sbt/1.0/server/0845deda85cb41abdb9f/sock"}
```

Windows 명명된 파이프 모드는 다음과 같다.

```json
{"uri":"local:sbt-server-0845deda85cb41abdb9f"}
```

TCP 모드에서는 포트 정보와 함께 인증 토큰 파일 경로가 포함된다.

```json
{
  "uri":"tcp://127.0.0.1:5010",
  "tokenfilePath":"/Users/xxx/.sbt/1.0/server/0845deda85cb41abdb9f/token.json",
  "tokenfileUri":"file:/Users/xxx/.sbt/1.0/server/0845deda85cb41abdb9f/token.json"
}
```

토큰 파일 내용은 다음과 같은 구조다.

```json
{
  "uri":"tcp://127.0.0.1:5010",
  "token":"12345678901234567890123456789012345678"
}
```

클라이언트는 접속 후 가장 먼저 `initialize` 메서드로 핸드셰이크를 수행해야 한다.

```
$ telnet 127.0.0.1 5010
Content-Type: application/vscode-jsonrpc; charset=utf-8
Content-Length: 149

{ "jsonrpc": "2.0", "id": 1, "method": "initialize",
  "params": { "initializationOptions":
    { "token": "84046191245433876643612047032303751629" } } }
```

### 3.3 주요 요청/이벤트

| 메서드 | 방향 | 용도 |
|--------|------|------|
| `textDocument/didSave` | 클라이언트 → 서버 | 파일 저장 시 자동으로 `compile` 태스크 트리거 (sbt 1.1.0+) |
| `textDocument/publishDiagnostics` | 서버 → 클라이언트 | 컴파일 경고/오류 알림 |
| `sbt/exec` | 클라이언트 → 서버 | 임의의 셸 명령 실행 요청 |
| `sbt/setting` | 클라이언트 → 서버 | 특정 설정 키의 값 조회 |
| `sbt/completion` | 클라이언트 → 서버 | 탭 완성 후보 조회 (sbt 1.3.0+) |
| `sbt/cancelRequest` | 클라이언트 → 서버 | 실행 중인 태스크 취소 요청 (sbt 1.3.0+) |

`sbt/exec`로 셸 명령을 실행하는 예시는 다음과 같다.

```
Content-Length: 91

{ "jsonrpc": "2.0", "id": 2, "method": "sbt/exec",
  "params": { "commandLine": "clean" } }
```

`sbt/setting`으로 설정값을 조회하면 다음과 같은 응답을 받는다.

```
{ "jsonrpc": "2.0", "id": 3, "method": "sbt/setting",
  "params": { "setting": "root/scalaVersion" } }
```

```json
{"jsonrpc":"2.0","id":"3","result":{"value":"2.12.2","contentType":"java.lang.String"}}
```

`publishDiagnostics` 알림은 컴파일 오류 위치와 메시지를 담아 IDE에 실시간으로 전달한다.

```json
{
  "jsonrpc": "2.0",
  "method": "textDocument/publishDiagnostics",
  "params": {
    "uri": "file:/Users/xxx/work/hellotest/Hello.scala",
    "diagnostics": [
      {
        "range": {
          "start": { "line": 2, "character": 0 },
          "end": { "line": 2, "character": 1 }
        },
        "severity": 1,
        "source": "sbt",
        "message": "')' expected but '}' found."
      }
    ]
  }
}
```

### 3.4 유휴 타임아웃

sbt server가 무한정 떠 있지 않도록 유휴 타임아웃을 설정할 수 있다.

```
Global / serverIdleTimeout := Some(new FiniteDuration(5, TimeUnit.MINUTES))
```

지정한 시간 동안 어떤 클라이언트 요청도 없으면 서버가 자동으로 종료된다.

### 3.5 BSP와의 관계

sbt server가 사용하는 LSP 기반 프로토콜은 이후 여러 빌드 도구가 공통으로 채택하는 **Build Server Protocol**(BSP)의 기반이 되었다. BSP는 IDE가 특정 빌드 도구(sbt, Mill, Bazel 등)의 세부 구현을 몰라도 컴파일·테스트·의존성 조회 같은 요청을 동일한 프로토콜로 보낼 수 있게 표준화한 것이다. sbt server의 exec/setting/completion/diagnostics 개념은 BSP가 정의하는 빌드 타겟 컴파일, 진단 발행, 의존성 조회 요청들과 맥락을 같이한다.

---

## 4. 증분 컴파일(Zinc) 원리

### 4.1 목표

증분 컴파일의 목표는 한 파일을 고쳤을 때 프로젝트 전체가 아니라 **실제로 영향을 받는 파일만** 다시 컴파일하는 것이다. 두 축으로 접근한다.

- Scalac을 매번 새 JVM에서 띄우지 않고, 컴파일러 자체를 계속 살려 둬 JIT 워밍업 이점을 활용
- 소스 파일 사이의 **인터페이스(공개 API)** 변경 여부만 추적해, 구현부만 바뀐 경우 하위 의존 파일은 건드리지 않음

### 4.2 의존성 두 종류

sbt는 소스 파일 단위로 의존성 그래프를 만들며, 의존성을 두 가지로 구분한다.

**상속 의존성(Inheritance dependency)**

```scala
// A.scala
abstract class A

// B.scala
class B extends A
```

부모 클래스/트레이트가 바뀌면 자식은 무조건 재컴파일 대상이 된다. 상속 관계는 name hashing 최적화를 적용하지 않는다 — 부모의 시그니처 변경이 자식의 컴파일 성립 여부 자체에 영향을 주기 때문이다.

**멤버 참조 의존성(Member reference dependency)**

```scala
// A.scala
class A {
  def foo(): Int = 12
}

// B.scala
class B {
  def bar(x: A): Int = x.foo()
}
```

한 클래스의 메서드/필드를 호출만 하는 관계는 name hashing 최적화 대상이 된다.

### 4.3 Name Hashing

sbt 0.13.6부터 도입된 알고리즘으로, "이 소스 파일이 실제로 사용하는 이름(usedNames)"과 "다른 파일에서 변경된 멤버의 이름"을 비교해 재컴파일 여부를 좁힌다.

```scala
// A.scala (변경 전)
class A {
  def inc(x: Int): Int = x + 1
}

// A.scala (변경 후 — 새 메서드 추가)
class A {
  def inc(x: Int): Int = x + 1
  def dec(x: Int): Int = x - 1
}

// B.scala
class B {
  def foo(a: A, x: Int): Int = a.inc(x)
}
```

A에 `dec`이 새로 생겼지만, B.scala의 usedNames에는 `dec`이 없다. 따라서 A의 API 해시는 바뀌었어도 B가 실제로 참조하는 이름의 해시는 그대로이므로 B는 재컴파일하지 않는다.

Name hashing이 통하지 않는 대표적인 경우 두 가지가 있다.

1. **상속 관계**: 부모에 추상 메서드가 추가되면 자식이 그 구현을 갖추지 못해 컴파일 자체가 깨질 수 있으므로 이름 사용 여부와 무관하게 무조건 재컴파일한다.
2. **암시적 변환(enrich-my-library 패턴)**: `implicit` 변환으로 확장 메서드를 제공하는 구조에서는 호출부 코드에 원래 이름이 등장하지 않을 수 있어, 변환 대상 클래스의 변경이 은근히 넓게 영향을 미친다. sbt는 이런 경우도 usedNames 추적 범위에 포함시켜 처리한다.

### 4.4 컴파일러 확장 페이즈

sbt는 Scala 컴파일러 뒤에 세 가지 페이즈를 추가로 끼워 넣어 증분 컴파일에 필요한 정보를 뽑아낸다.

| 페이즈 | 역할 |
|--------|------|
| API 추출(API phase) | 클래스의 공개 인터페이스(시그니처)를 컴파일러 버전에 무관한 형태로 직렬화 |
| 의존성 추출(Dependency phase) | 소스 파일이 참조하는 다른 심볼을 찾아 파일 간 의존 관계로 매핑 |
| 분석기(Analyzer phase) | 소스 파일이 생성한 클래스 파일 목록을 수집 |

API 추출 결과는 파일별 해시(`nameHashes`)로 저장되고, 다음 컴파일 시점에 이전 해시와 비교해 실제로 바뀐 멤버만 골라낸다.

### 4.5 인터페이스로 취급되는 요소들

다음과 같은 변경은 "구현 세부사항"이 아니라 "인터페이스 변경"으로 취급되어 하위 파일 재컴파일을 유발할 수 있다.

- 메서드 파라미터 이름 변경 (named argument 호출이 가능하므로 이름 자체가 API의 일부)
- 트레이트에 정의된 메서드의 시그니처 변경
- `sealed` 클래스 계층에 새 케이스 추가 — 패턴 매칭에서 `MatchError` 가능성이 달라지므로 해당 계층을 매칭하는 모든 파일이 영향받는다
- 반대로 `private` 메서드는 애초에 공개 API에 포함되지 않으므로, 구현을 바꿔도 호출 측 재컴파일을 유발하지 않는다

### 4.6 타입 추론이 만드는 함정

반환 타입을 명시하지 않은 메서드는 구현을 바꾸는 것만으로 추론된 반환 타입이 달라질 수 있고, 이는 곧 인터페이스 변경으로 이어져 예상치 못한 넓은 범위의 재컴파일을 유발한다.

```scala
// 변경 전 — 추론 타입: List[FileWriter]
def openFiles(list: List[File]) =
  list.map(name => new FileWriter(name))

// 변경 후 — 추론 타입이 Vector[BufferedWriter]로 바뀜
def openFiles(list: List[File]) =
  Vector(list.map(name => new BufferedWriter(new FileWriter(name))): _*)
```

호출부에서 `List[FileWriter]`를 기대하고 있었다면 타입 체크가 깨지고 재컴파일이 강제된다. 공개 API에 해당하는 메서드는 반환 타입을 명시적으로 선언해두면 구현이 바뀌어도 인터페이스가 안정적으로 유지된다.

```scala
def openFiles(list: List[File]): Seq[Writer] =
  Vector(list.map(name => new BufferedWriter(new FileWriter(name))): _*)
```

### 4.7 추적의 한계

- sbt는 **파일 단위**로 의존성을 추적하므로, 한 파일 안에 여러 클래스를 넣어두면 그중 하나만 바뀌어도 같은 파일을 참조하는 다른 파일까지 넓게 재컴파일될 수 있다. 클래스를 파일별로 분리하면 이 범위를 줄일 수 있다.
- `sealed` 계층에 케이스를 추가하면 해당 타입을 패턴 매칭하는 모든 소스가 재컴파일 대상이 된다.
- 복잡한 제네릭 타입 매개변수나 와일드카드가 얽힌 의존성은 정밀하게 추적되지 않을 수 있다.

### 4.8 디버깅

API 변경 추적 로그를 보고 싶다면 `apiDebug` 옵션을 켠다.

```
sbt> set incOptions := incOptions.value.withApiDebug(true)
```

이 옵션을 켜면 어떤 API 차이 때문에 재컴파일이 유발됐는지 diff 형태로 로그에 출력된다.

```
[debug] Detected a change in a public API:
[debug] --- /path/Test.scala
[debug] +++ /path/Test.scala
[debug]  def b: scala.this#Int
[debug] -def b: scala.this#Int
[debug] +def b: java.lang.this#String
```

### 4.9 정리

| 개념 | 의미 |
|------|------|
| 상속 의존성 | 부모-자식 관계, name hashing 미적용, 변경 시 무조건 재컴파일 |
| 멤버 참조 의존성 | 호출 관계, name hashing 적용 대상 |
| usedNames | 한 소스 파일이 실제로 참조하는 이름 집합 |
| nameHashes | 멤버 이름별 API 해시 |
| API 추출 페이즈 | 공개 인터페이스를 버전 독립적 형태로 뽑아내는 컴파일러 확장 |
| 타입 추론 위험 | 반환 타입 미명시 시 구현 변경만으로 인터페이스가 바뀔 수 있음 |

이 원리 덕분에 대규모 프로젝트에서도 한두 파일을 고쳤을 때 전체 재컴파일이 아니라 실제로 영향받는 부분집합만 다시 컴파일해 반복 개발 주기를 크게 단축할 수 있다.

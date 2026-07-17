# sbt How-To: 대화형 모드, 로깅, 프로젝트 메타데이터, 패키징

> 원본: https://www.scala-sbt.org/1.x/docs/Howto-Interactive-Mode.html, https://www.scala-sbt.org/1.x/docs/Howto-Logging.html, https://www.scala-sbt.org/1.x/docs/Howto-Project-Metadata.html, https://www.scala-sbt.org/1.x/docs/Howto-Package.html

---

## 목차

1. [대화형 모드: 탭 완성과 프롬프트](#1-대화형-모드-탭-완성과-프롬프트)
2. [대화형 모드: 히스토리와 시작 명령](#2-대화형-모드-히스토리와-시작-명령)
3. [로깅: 이전 실행 결과 다시 보기](#3-로깅-이전-실행-결과-다시-보기)
4. [로깅: 로그 레벨과 스택 트레이스 제어](#4-로깅-로그-레벨과-스택-트레이스-제어)
5. [로깅: 태스크 안에서 로거 사용하기](#5-로깅-태스크-안에서-로거-사용하기)
6. [프로젝트 메타데이터: 기본 정보](#6-프로젝트-메타데이터-기본-정보)
7. [프로젝트 메타데이터: 퍼블리시용 부가 정보](#7-프로젝트-메타데이터-퍼블리시용-부가-정보)
8. [패키징: 클래스패스에 jar 포함시키기](#8-패키징-클래스패스에-jar-포함시키기)
9. [패키징: 매니페스트 속성 추가](#9-패키징-매니페스트-속성-추가)
10. [패키징: 패키지 내용물 커스터마이징](#10-패키징-패키지-내용물-커스터마이징)

---

## 1. 대화형 모드: 탭 완성과 프롬프트

인자 없이 `sbt`를 실행하거나 `shell` 태스크를 호출하면 대화형 모드로 진입한다. 대화형 모드는 명령어 자동 완성과 커스텀 프롬프트를 제공한다.

**탭 완성**: 커서 위치에서 Tab 키를 누르면 입력을 완성해준다. 후보가 하나뿐이면 자동으로 이어붙고, 여러 개면 목록을 보여준다.

```
> tes<TAB>
test
> test<TAB>
testFrameworks  testListeners  testLoader  testOnly  testOptions
> testOnly <TAB>
```

일부 명령은 Tab을 반복해서 누를수록 더 상세한 후보를 보여준다. 현재는 `set` 명령이 이 방식을 지원한다.

**프롬프트 커스터마이징**: `shellPrompt` 설정(`State => String` 타입)으로 프롬프트 문자열을 원하는 대로 바꿀 수 있다.

```scala
// 현재 프로젝트 이름을 표시
ThisBuild / shellPrompt := { state =>
  Project.extract(state).currentRef.project + "> "
}

// 로그인 사용자명을 표시
shellPrompt := { state => System.getProperty("user.name") + "> " }
```

키바인딩을 바꾸고 싶다면 `jline.keybindings` 시스템 프로퍼티에 커스텀 키바인딩 파일 경로를 지정하면 된다. 기본 키바인딩 파일을 참고해서 작성하면 된다.

---

## 2. 대화형 모드: 히스토리와 시작 명령

대화형 모드에서 입력한 명령 히스토리는 기본적으로 각 프로젝트의 `target/` 디렉터리 아래 저장된다.

저장 위치는 `historyPath` 설정으로 바꿀 수 있다.

```scala
historyPath := Some(baseDirectory.value / ".history")
```

멀티 프로젝트 빌드에서 서브프로젝트끼리 히스토리를 공유하고 싶다면 루트 프로젝트의 `target`을 가리키게 한다.

```scala
historyPath := Some((target in LocalRootProject).value / ".history")
```

히스토리 저장 자체를 끄려면 `None`으로 설정한다.

```scala
historyPath := None
```

sbt를 실행하면서 대화형 프롬프트에 들어가기 전에 특정 명령을 먼저 실행하게 할 수도 있다. 명령줄 인자로 순서대로 나열하면 되고, 마지막 인자를 `shell`로 주면 이후 대화형 모드로 전환된다.

```bash
$ sbt clean compile shell
```

`clean compile`이 실패하면 sbt는 즉시 종료하므로 프롬프트를 볼 수 없다. 실패해도 프롬프트에 진입하게 하려면 `onFailure shell`을 앞에 끼워 넣는다.

```bash
$ sbt "onFailure shell" clean compile shell
```

---

## 3. 로깅: 이전 실행 결과 다시 보기

sbt는 화면에 보여주는 것보다 더 상세한 로그를 파일에 함께 남긴다. `last` 명령으로 방금 실행한 명령의 상세 로그를 다시 확인할 수 있다.

```
> last
> last compile   # 컴파일 관련 상세 로그
> last update    # 의존성 해석(resolve) 관련 상세 로그
```

Scala 컴파일러는 기본적으로 경고(warning)의 전체 내용을 출력하지 않는다. 재컴파일 없이 직전 컴파일에서 발생한 경고를 모두 보고 싶다면 `printWarnings` 태스크를 쓴다. 테스트 소스의 경고를 보려면 `Test/printWarnings`를 쓴다.

```
> printWarnings
> Test / printWarnings
```

---

## 4. 로깅: 로그 레벨과 스택 트레이스 제어

**전역 로그 레벨**: `error`, `warn`, `info`, `debug` 명령으로 기본 로그 레벨을 바꾼다. sbt 시작 시 옵션으로도 지정 가능하다.

```bash
$ sbt --warn
```

**태스크/설정 단위 로그 레벨**: `logLevel` 설정에 `Level.Error`, `Level.Warn`, `Level.Info`, `Level.Debug` 값을 지정한다.

```scala
// compile 태스크에만 적용
set Compile / compile / logLevel := Level.Warn

// 프로젝트 전체에 적용
set logLevel := Level.Warn
```

**스택 트레이스 상세도**: `traceLevel` 설정(정수 값)으로 예외 발생 시 보여줄 스택 트레이스 범위를 조절한다.

| 값 | 동작 |
|---|---|
| 음수 | 스택 트레이스를 표시하지 않음 |
| 0 | 첫 sbt 프레임까지만 표시 |
| 양수 | 지정한 프레임 수만큼 표시 |

```scala
set every traceLevel := 0
```

**테스트 출력 버퍼링**: 기본적으로 sbt는 테스트 클래스 하나가 끝날 때까지 출력을 모아뒀다가 한꺼번에 보여준다. 즉시 출력하려면 `logBuffered`를 끈다.

```scala
logBuffered := false
```

**커스텀 로거**: `extraLoggers` 설정에 `ScopedKey[_] => Seq[Appender]` 함수를 넣으면 log4j2 커스텀 appender를 추가할 수 있다. appender는 `AbstractAppender`를 상속해서 구현한다.

---

## 5. 로깅: 태스크 안에서 로거 사용하기

태스크 본문에서는 `streams.value.log`로 `Logger` 인스턴스를 얻어 로그를 남긴다.

```scala
myTask := {
  val log = streams.value.log
  log.warn("A warning.")
}
```

설정(setting) 초기화 시점에는 태스크 실행 컨텍스트가 없으므로 `streams`를 쓸 수 없다. 대신 `sLog.value`로 로거를 얻는다.

```scala
mySetting := {
  val log = sLog.value
  log.warn("A warning.")
}
```

---

## 6. 프로젝트 메타데이터: 기본 정보

sbt 프로젝트는 이름, 버전, 조직(organization) 세 가지를 기본 메타데이터로 갖는다. 이 값들은 생성되는 아티팩트 이름 등 빌드 여러 곳에서 사용된다.

```scala
name := "Your project name"
version := "1.0"
organization := "org.example"
```

- `name`은 아티팩트 식별자로 쓰기 위해 정규화(normalize)되며, 정규화된 값은 `normalizedName`에 저장된다.
- `organization`은 관례상 본인이 소유한 도메인을 역순으로 적어 프로젝트 네임스페이스로 삼는다. 퍼블리시하는 프로젝트라면 반드시 지정해야 한다.

---

## 7. 프로젝트 메타데이터: 퍼블리시용 부가 정보

조직명, 홈페이지, 설명, 라이선스 등은 생성되는 `pom.xml`이나 프로젝트 웹 페이지에 그대로 반영된다.

```scala
organizationName := "Example, Inc."
organizationHomepage := Some(url("http://example.org"))

homepage := Some(url("https://www.scala-sbt.org"))
startYear := Some(2008)
description := "A build tool for Scala."
licenses += "GPLv2" -> url("https://www.gnu.org/licenses/gpl-2.0.html")
```

| 설정 | 의미 |
|---|---|
| `organizationName` | 조직의 정식 명칭(사람이 읽는 이름) |
| `organizationHomepage` | 조직 홈페이지 URL |
| `homepage` | 프로젝트 홈페이지 URL |
| `startYear` | 프로젝트 시작 연도 |
| `description` | 프로젝트 설명 |
| `licenses` | (라이선스명, URL) 쌍의 목록 |

---

## 8. 패키징: 클래스패스에 jar 포함시키기

sbt는 기본적으로 `run`, `test`, `console` 등에서 클래스패스를 구성할 때 컴파일된 클래스 디렉터리를 그대로 사용한다. `exportJars`를 켜면 클래스 디렉터리 대신 패키징된 jar 파일을 클래스패스에 올린다.

```scala
exportJars := true
```

패키징 단계를 거치는 만큼 매 실행마다 jar를 새로 빌드하는 오버헤드가 생기지만, 실제 배포 산출물과 동일한 형태로 클래스패스가 구성된다는 장점이 있다.

---

## 9. 패키징: 매니페스트 속성 추가

바이너리 패키지(jar)의 매니페스트에 속성을 추가하는 방법은 두 가지다.

**`Package.ManifestAttributes`**: `java.util.jar.Attributes.Name` 또는 문자열 키와 문자열 값의 쌍을 전달한다.

```scala
Compile / packageBin / packageOptions +=
  Package.ManifestAttributes(java.util.jar.Attributes.Name.SEALED -> "true")
```

**`Package.JarManifest`**: `java.util.jar.Manifest` 객체를 직접 조작하거나 파일에서 읽어와 지정할 때 쓴다.

---

## 10. 패키징: 패키지 내용물 커스터마이징

패키지에 포함되는 파일과 패키지 내부 경로의 매핑은 `mappings` 태스크로 정의한다. 각 매핑은 (포함할 파일 → 패키지 안에서의 경로) 쌍이며, 구성(configuration)과 패키지 태스크별로 스코프가 나뉜다.

기본 바이너리 jar(`packageBin`)에 파일 하나를 추가하는 예시.

```scala
Compile / packageBin / mappings += {
  (baseDirectory.value / "in" / "example.txt") -> "out/example.txt"
}
```

패키지 종류마다 별도의 `mappings` 태스크가 있다. 예를 들어 테스트 소스 jar에 파일을 추가하려면 다음처럼 스코프를 바꾼다.

```scala
Test / packageSrc / mappings += {
  (baseDirectory.value / "test-in" / "note.txt") -> "note.txt"
}
```

생성되는 패키지 파일명 규칙은 `artifactName` 설정으로 제어한다.

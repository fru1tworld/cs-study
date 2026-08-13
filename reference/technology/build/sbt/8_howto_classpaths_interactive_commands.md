# sbt How-to: 클래스패스, 대화형 모드, 명령어, 패키징

## sbt How-to 모음: 클래스패스 / 경로 / 파일 생성 / 빌드 검사

> 원본: https://www.scala-sbt.org/1.x/docs/Howto-Classpaths.html, https://www.scala-sbt.org/1.x/docs/Howto-Customizing-Paths.html, https://www.scala-sbt.org/1.x/docs/Howto-Generating-Files.html, https://www.scala-sbt.org/1.x/docs/Howto-Inspect-the-Build.html

---

### 목차

1. [클래스패스 다루기](#1-클래스패스-다루기)
2. [경로 커스터마이징](#2-경로-커스터마이징)
3. [소스/리소스 파일 생성](#3-소스리소스-파일-생성)
4. [빌드 검사](#4-빌드-검사)

---

### 1. 클래스패스 다루기

#### 1.1 새로운 관리형 아티팩트 타입을 클래스패스에 포함

문제: `mar` 같은 비표준 아티팩트 타입을 클래스패스 계산에 포함 필요

해법: `classpathTypes`에 타입 문자열을 추가

```scala
classpathTypes += "mar"
```

#### 1.2 컴파일 클래스패스 획득

문제: 컴파일 시 사용하는 클래스패스(관리형 의존성만, 클래스 디렉터리 미포함일 수 있음)를 태스크 안에서 획득 필요

해법: `dependencyClasspath`를 `Compile` 스코프로 조회

```scala
example := {
  val cp: Seq[File] = (Compile / dependencyClasspath).value.files
  // cp를 활용하는 로직
}
```

#### 1.3 런타임 클래스패스 획득

문제: 컴파일된 클래스와 런타임 의존성을 모두 포함한 클래스패스 필요

해법: `fullClasspath`를 `Runtime` 스코프로 조회

```scala
example := {
  val cp: Seq[File] = (Runtime / fullClasspath).value.files
}
```

#### 1.4 테스트 클래스패스 획득

문제: 테스트 컴파일 클래스·테스트 전용 의존성까지 포함된 클래스패스 필요

해법: `fullClasspath`를 `Test` 스코프로 조회

```scala
example := {
  val cp: Seq[File] = (Test / fullClasspath).value.files
}
```

#### 1.5 클래스 디렉터리 대신 패키징된 JAR 사용

문제: `fullClasspath`가 컴파일된 클래스 디렉터리를 그대로 노출하는 대신, 패키징된 JAR 파일을 가리키도록 하고 싶음

해법: `exportJars`를 켬 → 이 설정이 `true`가 되면 `exportedProducts`가 `packageBin`의 결과물(JAR)을 반환

```scala
exportJars := true
```

#### 1.6 특정 설정의 관리형 JAR 전체 획득

문제: `Compile` 설정에서 jar/zip 타입인 관리형 의존성 파일 목록만 추출 필요

해법: `Classpaths.managedJars`에 원하는 아티팩트 타입 집합과 `update` 결과를 전달

```scala
example := {
  val artifactTypes = Set("jar", "zip")
  val files = Classpaths.managedJars(Compile, artifactTypes, update.value)
}
```

#### 1.7 클래스패스에서 순수 파일 목록만 추출

문제: 클래스패스는 `Seq[Attributed[File]]` 타입이라 메타데이터가 붙어 있는데, 파일 경로만 필요

해법: `.files` 메서드로 `Seq[File]` 획득

```scala
val cp: Seq[Attributed[File]] = ???
val files: Seq[File] = cp.files
```

#### 1.8 클래스패스 항목의 모듈/아티팩트 메타데이터 조회

문제: 각 클래스패스 항목이 어떤 `ModuleID`/`Artifact`에서 왔는지, 혹은 어떤 컴파일 분석(Analysis) 결과와 연결되는지 확인 필요

해법: `Attributed`의 `get` 메서드로 속성 키에 대응하는 값을 조회 → 소스에서 온 항목만 `analysis`를, 관리형 의존성 항목만 `artifact`/`moduleID`를 보유

```scala
val classpath: Seq[Attributed[File]] = ???
for (entry <- classpath) yield {
  val art: Option[Artifact] = entry.get(artifact.key)
  val mod: Option[ModuleID] = entry.get(moduleID.key)
  val an: Option[inc.Analysis] = entry.get(analysis)
}
```

태스크/메서드별 용도는 다음과 같음.

- `classpathTypes`: 클래스패스에 포함할 아티팩트 타입 집합
- `Compile / dependencyClasspath`: 컴파일용 클래스패스(관리형 의존성 중심)
- `Runtime / fullClasspath`: 런타임 클래스패스(컴파일 결과 + 의존성)
- `Test / fullClasspath`: 테스트 클래스패스
- `exportJars`: true면 클래스 디렉터리 대신 JAR을 클래스패스에 노출
- `Classpaths.managedJars(config, types, updateReport)`: 설정별 관리형 JAR 목록 추출

---

### 2. 경로 커스터마이징

기본 디렉터리 구조(`src/main/scala`, `src/main/resources`, `lib` 등)를 바꾸고 싶을 때 사용하는 키 모음 → 대부분 `Compile`/`Test` 스코프를 붙여 설정별로 다르게 지정

#### 2.1 Scala 소스 디렉터리 변경

```scala
Compile / scalaSource := baseDirectory.value / "src"
Test / scalaSource := baseDirectory.value / "test-src"
```

#### 2.2 Java 소스 디렉터리 변경

```scala
Compile / javaSource := baseDirectory.value / "src"
Test / javaSource := baseDirectory.value / "test-src"
```

#### 2.3 리소스 디렉터리 변경

```scala
Compile / resourceDirectory := baseDirectory.value / "resources"
Test / resourceDirectory := baseDirectory.value / "test-resources"
```

#### 2.4 비관리 라이브러리 디렉터리(`lib`) 변경

프로젝트 전체에 적용하거나, 설정별로 다르게 지정 가능

```scala
// 프로젝트 전체
unmanagedBase := baseDirectory.value / "jars"

// Compile 설정만
Compile / unmanagedBase := baseDirectory.value / "lib" / "main"
```

#### 2.5 프로젝트 루트 디렉터리의 소스 포함 비활성화

sbt는 기본적으로 프로젝트 루트에 있는 `.scala` 파일도 소스로 인식 → 원치 않으면 비활성화

```scala
sourcesInBase := false
```

#### 2.6 추가 소스 디렉터리 추가

기본 디렉터리(`scalaSource`, `javaSource`)를 대체하지 않고, 추가로 더할 때는 `unmanagedSourceDirectories`에 추가

```scala
Compile / unmanagedSourceDirectories += baseDirectory.value / "extra-src"
```

#### 2.7 추가 리소스 디렉터리 추가

```scala
Compile / unmanagedResourceDirectories += baseDirectory.value / "extra-resources"
```

#### 2.8 소스 파일 포함/제외 필터링

`includeFilter`/`excludeFilter`는 `unmanagedSources` 등 특정 태스크의 하위 키로 스코프를 좁혀 지정

```scala
// 전체에 적용되는 제외 필터
unmanagedSources / excludeFilter := HiddenFileFilter || "*impl*"

// 설정별 포함 필터
Compile / unmanagedSources / includeFilter := "*.scala" || "*.java"
Test / unmanagedSources / includeFilter := HiddenFileFilter || "*impl*"
```

#### 2.9 리소스 파일 포함/제외 필터링

```scala
unmanagedResources / excludeFilter := HiddenFileFilter || "*impl*"

Compile / unmanagedResources / includeFilter := "*.txt"
Test / unmanagedResources / includeFilter := "*.html"
```

#### 2.10 비관리 라이브러리(JAR) 포함/제외 필터링

```scala
unmanagedJars / excludeFilter := HiddenFileFilter || "*.zip"

Compile / unmanagedJars / includeFilter := "*.jar"
Test / unmanagedJars / includeFilter := "*.jar" || "*.zip"
```

경로 관련 키 정리:

- `scalaSource` (기본값: `src/main/scala`, `src/test/scala`): Scala 소스 루트
- `javaSource` (기본값: `src/main/java`, `src/test/java`): Java 소스 루트
- `resourceDirectory` (기본값: `src/main/resources`, `src/test/resources`): 리소스 루트
- `unmanagedBase` (기본값: `lib/`): 비관리 JAR 디렉터리
- `sourcesInBase` (기본값: `true`): 프로젝트 루트의 `.scala` 파일을 소스로 취급할지 여부
- `unmanagedSourceDirectories` (기본값: 위 소스 루트들): 추가 소스 디렉터리 목록
- `unmanagedResourceDirectories` (기본값: 위 리소스 루트): 추가 리소스 디렉터리 목록
- `includeFilter` / `excludeFilter` (기본값: 태스크마다 다름): 파일 포함/제외 조건

`includeFilter`/`excludeFilter`는 `HiddenFileFilter`처럼 sbt가 제공하는 필터 조합자와 `||`(OR), `&&`(AND), `--`(차집합) 등으로 조합해 사용

---

### 3. 소스/리소스 파일 생성

빌드 도중 코드·설정 파일을 자동 생성해 컴파일/패키징 대상에 포함시키는 방법 → 생성된 파일은 `sourceManaged`/`resourceManaged` 아래(`target/scala-*/src_managed`, `target/scala-*/resource_managed`)에 위치하며, 기본적으로 패키지된 소스 아티팩트에는 미포함.

#### 3.1 소스 파일 생성

문제: 빌드 시점에 Scala/Java 소스를 동적으로 만들어 컴파일 대상에 포함 필요

해법: `sourceGenerators`에 `Seq[File]`을 반환하는 태스크를 추가 → 반드시 `.value`가 아니라 `.taskValue`로 등록 필요

```scala
Compile / sourceGenerators += Def.task {
  makeSomeSources((Compile / sourceManaged).value / "demo")
}.taskValue
```

간단한 예: 앱 진입점 객체 하나를 직접 생성하는 경우.

```scala
Compile / sourceGenerators += Def.task {
  val file = (Compile / sourceManaged).value / "demo" / "Test.scala"
  IO.write(file, """object Test extends App { println("Hi") }""")
  Seq(file)
}.taskValue
```

`run`을 실행하면 생성된 `Test` 객체가 컴파일되어 "Hi" 출력.

`makeSomeSources`의 시그니처는 다음과 같은 형태를 기대.

```scala
def makeSomeSources(base: File): Seq[File]
```

#### 3.2 리소스 파일 생성

문제: 빌드 시점에 값이 채워지는 프로퍼티 파일 등의 리소스 생성 필요

해법: `resourceGenerators`에 `Seq[File]`을 반환하는 태스크를 추가

```scala
Compile / resourceGenerators += Def.task {
  val file = (Compile / resourceManaged).value / "demo" / "myapp.properties"
  val contents = "name=%s\nversion=%s".format(name.value, version.value)
  IO.write(file, contents)
  Seq(file)
}.taskValue
```

생성된 파일은 `target/scala-*/resource_managed` 아래에 위치 → `run`이나 `package` 실행 시 함께 처리.

#### 3.3 공통 주의사항

- 태스크를 `sourceGenerators`/`resourceGenerators`에 추가할 때는 `.value`가 아니라 `.taskValue`를 사용 → `.value`를 쓰면 즉시 평가된 결과(빈 목록일 수 있는 값)가 고정되므로, 매 빌드마다 다시 실행되는 태스크 자체를 등록 필요
- 생성 로직이 매번 무조건 다시 쓰지 않도록, 입력이 바뀌었을 때만 재생성하는 캐싱을 넣는 편이 좋음 → `sbt.Tracked.inputChanged`/`outputChanged` 같은 파일 추적 유틸리티 활용 가능
- 생성된 소스/리소스는 기본적으로 `packageSrc` 결과물에는 미포함 → 패키지에 포함하려면 `mappings` 관련 설정을 별도로 조정 필요
- 스코프를 `Test`로 바꾸면 테스트 전용 소스/리소스 생성에도 동일한 패턴 적용 가능

소스/리소스 생성 관련 키 정리:

- `sourceGenerators` (반환 타입: `Seq[Task[Seq[File]]]`): 소스 파일을 생성하는 태스크 목록
- `resourceGenerators` (반환 타입: `Seq[Task[Seq[File]]]`): 리소스 파일을 생성하는 태스크 목록
- `sourceManaged` (반환 타입: `File`): 생성된 소스가 위치할 디렉터리
- `resourceManaged` (반환 타입: `File`): 생성된 리소스가 위치할 디렉터리

---

### 4. 빌드 검사

sbt 셸에서 현재 빌드 정의의 설정값·태스크·의존관계를 살펴볼 때 쓰는 명령어 모음.

#### 4.1 명령어/태스크/설정 도움말 검색: `help`

```
> help
> help compile
```

인자 없이 실행하면 사용 가능한 명령어 전체 목록을 표시 → 정규식을 인자로 주면 이름이 일치하는 항목만 검색.

#### 4.2 사용 가능한 태스크 목록: `tasks`

```
> tasks
```

현재 프로젝트에서 실행 가능한 태스크 목록을 표시 → 정규식으로 필터링 가능.

#### 4.3 사용 가능한 설정 목록: `settings`

```
> settings
```

현재 프로젝트에 정의된 설정 키 목록을 표시 → 마찬가지로 정규식 검색 가능.

#### 4.4 설정/태스크의 상세 정보와 의존관계: `inspect`

```
> inspect Test/compile
> inspect tree clean
> inspect scalacOptions
```

`inspect`는 대상 키에 대해 다음 정보를 출력.

- 정방향 의존성(Dependencies): 이 키가 값을 계산하기 위해 참조하는 다른 키
- 역방향 의존성(Reverse dependencies): 이 키를 참조하는 다른 키
- 값의 데이터 타입
- 현재 스코프에서 적용된 설정값의 출처(어느 빌드 파일에서 정의됐는지)

`inspect tree`를 쓰면 의존관계를 트리 형태로 확인 가능.

#### 4.5 로드된 프로젝트 목록: `projects`

```
> projects
[info] In file:/home/user/demo/
[info]   * parent
[info]     sub
```

현재 빌드에 로드된 모든 서브프로젝트를 파일 단위로 묶어 표시 → `*`는 현재 활성 프로젝트를 표시.

#### 4.6 세션 임시 설정 조회: `session`

```
> session list
> session list-all
```

`set` 명령으로 sbt 셸에서 즉석으로 덮어쓴(세션) 설정을 확인할 때 사용.

#### 4.7 빌드/sbt 기본 정보: `about`

```
> about
```

sbt 버전·Scala 버전·현재 로드된 플러그인 등 빌드 환경 정보를 요약해서 표시.

#### 4.8 설정값/태스크 실행 결과 출력: `show`

```
> show organization
> show update
> show Test/dependencyClasspath
> show compile:discoveredMainClasses
> show Test/definedTestNames
```

`show <키>`는 설정이면 그 값을, 태스크면 태스크를 실행한 결과를 콘솔에 출력 → `run` 등과 달리 부수효과 없이 값만 확인하고 싶을 때 우선적으로 쓰는 명령.

빌드 검사에 자주 쓰이는 명령어 정리:

- `help [regex]`: 명령어/태스크/설정 도움말 검색
- `tasks [regex]`: 사용 가능한 태스크 목록
- `settings [regex]`: 사용 가능한 설정 목록
- `inspect <키>`: 키의 의존관계, 타입, 정의 위치 확인
- `inspect tree <키>`: 의존관계를 트리로 확인
- `projects`: 로드된 서브프로젝트 목록
- `session list[-all]`: 세션 임시 설정 확인
- `about`: sbt/빌드 기본 정보
- `show <키>`: 설정값 또는 태스크 실행 결과 출력

---

## sbt How-To: 대화형 모드, 로깅, 프로젝트 메타데이터, 패키징

> 원본: https://www.scala-sbt.org/1.x/docs/Howto-Interactive-Mode.html, https://www.scala-sbt.org/1.x/docs/Howto-Logging.html, https://www.scala-sbt.org/1.x/docs/Howto-Project-Metadata.html, https://www.scala-sbt.org/1.x/docs/Howto-Package.html

---

### 목차

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

### 1. 대화형 모드: 탭 완성과 프롬프트

인자 없이 `sbt`를 실행하거나 `shell` 태스크를 호출하면 대화형 모드로 진입 → 대화형 모드는 명령어 자동 완성과 커스텀 프롬프트를 제공.

탭 완성: 커서 위치에서 Tab 키를 누르면 입력을 완성 → 후보가 하나뿐이면 자동으로 이어붙고, 여러 개면 목록을 표시.

```
> tes<TAB>
test
> test<TAB>
testFrameworks  testListeners  testLoader  testOnly  testOptions
> testOnly <TAB>
```

일부 명령은 Tab을 반복해서 누를수록 더 상세한 후보를 표시 → 현재는 `set` 명령이 이 방식을 지원.

프롬프트 커스터마이징: `shellPrompt` 설정(`State => String` 타입)으로 프롬프트 문자열을 원하는 대로 변경 가능.

```scala
// 현재 프로젝트 이름을 표시
ThisBuild / shellPrompt := { state =>
  Project.extract(state).currentRef.project + "> "
}

// 로그인 사용자명을 표시
shellPrompt := { state => System.getProperty("user.name") + "> " }
```

키바인딩을 바꾸고 싶다면 `jline.keybindings` 시스템 프로퍼티에 커스텀 키바인딩 파일 경로를 지정 → 기본 키바인딩 파일을 참고해서 작성.

---

### 2. 대화형 모드: 히스토리와 시작 명령

대화형 모드에서 입력한 명령 히스토리는 기본적으로 각 프로젝트의 `target/` 디렉터리 아래 저장.

저장 위치는 `historyPath` 설정으로 변경 가능.

```scala
historyPath := Some(baseDirectory.value / ".history")
```

멀티 프로젝트 빌드에서 서브프로젝트끼리 히스토리를 공유하고 싶다면 루트 프로젝트의 `target`을 가리키도록 설정.

```scala
historyPath := Some((target in LocalRootProject).value / ".history")
```

히스토리 저장 자체를 끄려면 `None`으로 설정.

```scala
historyPath := None
```

sbt를 실행하면서 대화형 프롬프트에 들어가기 전에 특정 명령을 먼저 실행 가능 → 명령줄 인자로 순서대로 나열하면 되고, 마지막 인자를 `shell`로 주면 이후 대화형 모드로 전환.

```bash
$ sbt clean compile shell
```

`clean compile`이 실패하면 sbt는 즉시 종료하므로 프롬프트 확인 불가 → 실패해도 프롬프트에 진입하게 하려면 `onFailure shell`을 앞에 삽입.

```bash
$ sbt "onFailure shell" clean compile shell
```

---

### 3. 로깅: 이전 실행 결과 다시 보기

sbt는 화면에 표시하는 것보다 더 상세한 로그를 파일에 함께 기록 → `last` 명령으로 방금 실행한 명령의 상세 로그를 다시 확인 가능.

```
> last
> last compile   # 컴파일 관련 상세 로그
> last update    # 의존성 해석(resolve) 관련 상세 로그
```

Scala 컴파일러는 기본적으로 경고(warning)의 전체 내용을 출력하지 않음 → 재컴파일 없이 직전 컴파일에서 발생한 경고를 모두 보고 싶다면 `printWarnings` 태스크 사용 → 테스트 소스의 경고를 보려면 `Test/printWarnings` 사용.

```
> printWarnings
> Test / printWarnings
```

---

### 4. 로깅: 로그 레벨과 스택 트레이스 제어

전역 로그 레벨: `error`, `warn`, `info`, `debug` 명령으로 기본 로그 레벨 변경 → sbt 시작 시 옵션으로도 지정 가능.

```bash
$ sbt --warn
```

태스크/설정 단위 로그 레벨: `logLevel` 설정에 `Level.Error`, `Level.Warn`, `Level.Info`, `Level.Debug` 값을 지정.

```scala
// compile 태스크에만 적용
set Compile / compile / logLevel := Level.Warn

// 프로젝트 전체에 적용
set logLevel := Level.Warn
```

스택 트레이스 상세도: `traceLevel` 설정(정수 값)으로 예외 발생 시 보여줄 스택 트레이스 범위 조절 가능

- 값별 동작
  - 음수: 스택 트레이스를 표시하지 않음
  - 0: 첫 sbt 프레임까지만 표시
  - 양수: 지정한 프레임 수만큼 표시

```scala
set every traceLevel := 0
```

테스트 출력 버퍼링: 기본적으로 sbt는 테스트 클래스 하나가 끝날 때까지 출력을 모아뒀다가 한꺼번에 표시 → 즉시 출력하려면 `logBuffered`를 끔

```scala
logBuffered := false
```

커스텀 로거: `extraLoggers` 설정에 `ScopedKey[_] => Seq[Appender]` 함수를 넣으면 log4j2 커스텀 appender 추가 가능 → appender는 `AbstractAppender`를 상속해서 구현

---

### 5. 로깅: 태스크 안에서 로거 사용하기

태스크 본문에서는 `streams.value.log`로 `Logger` 인스턴스를 얻어 로그 기록

```scala
myTask := {
  val log = streams.value.log
  log.warn("A warning.")
}
```

설정(setting) 초기화 시점에는 태스크 실행 컨텍스트가 없으므로 `streams` 사용 불가 → 대신 `sLog.value`로 로거 획득

```scala
mySetting := {
  val log = sLog.value
  log.warn("A warning.")
}
```

---

### 6. 프로젝트 메타데이터: 기본 정보

sbt 프로젝트는 이름·버전·조직(organization) 세 가지를 기본 메타데이터로 가짐 → 이 값들은 생성되는 아티팩트 이름 등 빌드 여러 곳에서 사용됨

```scala
name := "Your project name"
version := "1.0"
organization := "org.example"
```

- `name`: 아티팩트 식별자로 쓰기 위해 정규화(normalize)됨 → 정규화된 값은 `normalizedName`에 저장
- `organization`: 관례상 본인이 소유한 도메인을 역순으로 적어 프로젝트 네임스페이스로 삼음 → 퍼블리시하는 프로젝트라면 반드시 지정 필요

---

### 7. 프로젝트 메타데이터: 퍼블리시용 부가 정보

조직명·홈페이지·설명·라이선스 등은 생성되는 `pom.xml`이나 프로젝트 웹 페이지에 그대로 반영됨

```scala
organizationName := "Example, Inc."
organizationHomepage := Some(url("http://example.org"))

homepage := Some(url("https://www.scala-sbt.org"))
startYear := Some(2008)
description := "A build tool for Scala."
licenses += "GPLv2" -> url("https://www.gnu.org/licenses/gpl-2.0.html")
```

- `organizationName`: 조직의 정식 명칭(사람이 읽는 이름)
- `organizationHomepage`: 조직 홈페이지 URL
- `homepage`: 프로젝트 홈페이지 URL
- `startYear`: 프로젝트 시작 연도
- `description`: 프로젝트 설명
- `licenses`: (라이선스명, URL) 쌍의 목록

---

### 8. 패키징: 클래스패스에 jar 포함시키기

sbt는 기본적으로 `run`, `test`, `console` 등에서 클래스패스를 구성할 때 컴파일된 클래스 디렉터리를 그대로 사용 → `exportJars`를 켜면 클래스 디렉터리 대신 패키징된 jar 파일을 클래스패스에 올림

```scala
exportJars := true
```

패키징 단계를 거치는 만큼 매 실행마다 jar를 새로 빌드하는 오버헤드가 생기지만 → 실제 배포 산출물과 동일한 형태로 클래스패스가 구성되는 장점 있음

---

### 9. 패키징: 매니페스트 속성 추가

바이너리 패키지(jar)의 매니페스트에 속성을 추가하는 방법은 두 가지.

- `Package.ManifestAttributes`: `java.util.jar.Attributes.Name` 또는 문자열 키와 문자열 값의 쌍을 전달

```scala
Compile / packageBin / packageOptions +=
  Package.ManifestAttributes(java.util.jar.Attributes.Name.SEALED -> "true")
```

- `Package.JarManifest`: `java.util.jar.Manifest` 객체를 직접 조작하거나 파일에서 읽어와 지정할 때 사용

---

### 10. 패키징: 패키지 내용물 커스터마이징

패키지에 포함되는 파일과 패키지 내부 경로의 매핑은 `mappings` 태스크로 정의 → 각 매핑은 (포함할 파일 → 패키지 안에서의 경로) 쌍이며, 구성(configuration)과 패키지 태스크별로 스코프가 나뉨

기본 바이너리 jar(`packageBin`)에 파일 하나를 추가하는 예시.

```scala
Compile / packageBin / mappings += {
  (baseDirectory.value / "in" / "example.txt") -> "out/example.txt"
}
```

패키지 종류마다 별도의 `mappings` 태스크 존재 → 예를 들어 테스트 소스 jar에 파일을 추가하려면 다음처럼 스코프 변경

```scala
Test / packageSrc / mappings += {
  (baseDirectory.value / "test-in" / "note.txt") -> "note.txt"
}
```

생성되는 패키지 파일명 규칙은 `artifactName` 설정으로 제어

---

## sbt How-to 모음: 명령어 실행 / Scala 설정 / Scaladoc / 시작 시 작업

> 원본: https://www.scala-sbt.org/1.x/docs/Howto-Running-Commands.html, https://www.scala-sbt.org/1.x/docs/Howto-Scala.html, https://www.scala-sbt.org/1.x/docs/Howto-Scaladoc.html, https://www.scala-sbt.org/1.x/docs/Howto-Startup.html

---

### 목차

1. [명령어 실행: 배치 모드에서 인자 전달](#1-명령어-실행-배치-모드에서-인자-전달)
2. [명령어 실행: 여러 명령어 연속 실행](#2-명령어-실행-여러-명령어-연속-실행)
3. [명령어 실행: 파일에서 명령어 읽기](#3-명령어-실행-파일에서-명령어-읽기)
4. [명령어 실행: 별칭(alias) 정의](#4-명령어-실행-별칭alias-정의)
5. [명령어 실행: Scala 표현식 즉석 평가](#5-명령어-실행-scala-표현식-즉석-평가)
6. [Scala 설정: 빌드에 쓸 Scala 버전 지정](#6-scala-설정-빌드에-쓸-scala-버전-지정)
7. [Scala 설정: 표준 라이브러리 자동 의존성 끄기](#7-scala-설정-표준-라이브러리-자동-의존성-끄기)
8. [Scala 설정: 일시적으로 다른 버전 전환](#8-scala-설정-일시적으로-다른-버전-전환)
9. [Scala 설정: 로컬 설치본 사용](#9-scala-설정-로컬-설치본-사용)
10. [Scala 설정: 여러 버전으로 크로스 빌드](#10-scala-설정-여러-버전으로-크로스-빌드)
11. [Scala REPL: 의존성만 올린 콘솔](#11-scala-repl-의존성만-올린-콘솔)
12. [Scala REPL: 의존성 + 컴파일 코드 포함 콘솔](#12-scala-repl-의존성--컴파일-코드-포함-콘솔)
13. [Scala REPL: 빌드 정의와 플러그인 포함 콘솔](#13-scala-repl-빌드-정의와-플러그인-포함-콘솔)
14. [Scala REPL: 진입/종료 시 실행 명령 지정](#14-scala-repl-진입종료-시-실행-명령-지정)
15. [Scala REPL: 프로젝트 코드 안에서 REPL 내장](#15-scala-repl-프로젝트-코드-안에서-repl-내장)
16. [Scaladoc: javadoc과 scaladoc 자동 선택](#16-scaladoc-javadoc과-scaladoc-자동-선택)
17. [Scaladoc: 문서 생성 옵션 컴파일과 분리 설정](#17-scaladoc-문서-생성-옵션-컴파일과-분리-설정)
18. [Scaladoc: javadoc 옵션 설정](#18-scaladoc-javadoc-옵션-설정)
19. [Scaladoc: 외부 의존성 문서 자동/수동 링크](#19-scaladoc-외부-의존성-문서-자동수동-링크)
20. [Scaladoc: 라이브러리 자신의 API 문서 위치 선언](#20-scaladoc-라이브러리-자신의-api-문서-위치-선언)
21. [시작 시 작업 실행: onLoad 훅](#21-시작-시-작업-실행-onload-훅)

---

### 1. 명령어 실행: 배치 모드에서 인자 전달

셸을 띄우지 않고 커맨드라인에서 바로 sbt 명령어를 실행할 때, 명령어 자체에 공백이나 인자가 포함되어 있으면 셸이 이를 별개의 인자로 쪼개버림 → 이때는 명령어 전체를 큰따옴표로 감싸서 하나의 토큰으로 전달

```
$ sbt "project X" clean "~ compile"
```

`project X`(프로젝트 전환), `clean`, `~ compile`(파일 변경 감시 컴파일) 세 개의 명령어가 순서대로 실행됨

### 2. 명령어 실행: 여러 명령어 연속 실행

sbt 셸 안에서 여러 태스크를 한 줄에 이어서 실행하고 싶을 때는 세미콜론(`;`)으로 구분

```
> ~ ;clean;compile
```

`~`(watch) 뒤에 세미콜론으로 묶인 명령어 그룹을 붙이면 → 소스 파일이 바뀔 때마다 `clean`과 `compile`이 순서대로 다시 실행됨

### 3. 명령어 실행: 파일에서 명령어 읽기

반복해서 입력하는 명령어 시퀀스를 파일에 저장해두고 불러와 실행하는 기능이 sbt 셸에 내장되어 있음 → 이 문서 자체에는 별도 코드 예제가 없고, 관련 기능은 sbt 셸의 명령어 입력 소스 확장 방식 참고

### 4. 명령어 실행: 별칭(alias) 정의

자주 쓰는 명령어나 태스크를 짧은 이름으로 부르고 싶을 때 `alias` 명령어 사용

```
> alias a=about
> alias
    a = about
> a
[info] This is sbt ...
```

별칭을 없앨 때는 우변을 비워서 다시 `alias` 실행

```
> alias a=
> alias
> a
[error] Not a valid command: a ...
```

인자 없이 `alias`만 입력하면 현재 등록된 별칭 목록 출력

### 5. 명령어 실행: Scala 표현식 즉석 평가

빌드 파일을 만들지 않고 간단한 Scala 코드를 바로 컴파일해서 실행 결과를 확인하고 싶을 때는 `eval` 명령어 사용

```
> eval 2+2
4: Int
```

### 6. Scala 설정: 빌드에 쓸 Scala 버전 지정

프로젝트를 컴파일할 때 사용할 Scala 버전은 `scalaVersion` 키로 지정 → 지정하지 않으면 sbt 자체가 내장한 Scala 버전이 기본값으로 쓰임

```
scalaVersion := "2.11.1"
```

### 7. Scala 설정: 표준 라이브러리 자동 의존성 끄기

sbt는 기본적으로 `scala-library`를 프로젝트 의존성에 자동으로 추가 → 순수 Java 프로젝트를 sbt로 빌드하거나 표준 라이브러리를 직접 관리하고 싶을 때는 이 자동 추가를 끔

```
autoScalaLibrary := false
```

### 8. Scala 설정: 일시적으로 다른 버전 전환

빌드 정의를 고치지 않고 셸에서 즉시 다른 Scala 버전으로 전환해 명령어를 실행하고 싶을 때는 `++` 명령어 사용

```
> ++ 2.10.4
```

### 9. Scala 설정: 로컬 설치본 사용

퍼블리시되지 않은 로컬 빌드의 Scala 컴파일러/라이브러리로 프로젝트를 빌드해야 할 때는 `scalaHome`으로 로컬 설치 경로 지정

```
scalaVersion := "2.10.0-local"

scalaHome := Some(file("/path/to/scala/home/"))
```

`scalaVersion`은 아티팩트 이름 등에 쓰이는 식별용 버전 문자열이고, 실제 컴파일러/라이브러리는 `scalaHome`이 가리키는 경로에서 가져옴

### 10. Scala 설정: 여러 버전으로 크로스 빌드

하나의 소스를 2.12, 2.13, 3.x 등 여러 Scala 버전으로 동시에 빌드해야 하는 경우는 크로스 빌딩(cross-building) 기능 사용 → `crossScalaVersions`에 버전 목록을 지정하고 `+compile`, `+publish`처럼 `+` 접두사 명령어로 전체 버전에 대해 태스크 실행 → 상세 절차는 크로스 빌딩 전용 문서 참고

### 11. Scala REPL: 의존성만 올린 콘솔

프로젝트 소스 코드는 컴파일하지 않고 라이브러리 의존성만 클래스패스에 올린 상태로 REPL에 진입하고 싶을 때는 `consoleQuick` 사용

```
> consoleQuick
```

테스트 의존성까지 포함하려면 `Test` 스코프 추가

```
> Test/consoleQuick
```

프로젝트 소스가 커서 컴파일이 오래 걸릴 때, 의존 라이브러리 API만 빠르게 확인하고 싶은 상황에 적합

### 12. Scala REPL: 의존성 + 컴파일 코드 포함 콘솔

프로젝트 소스를 컴파일한 뒤 그 결과와 모든 의존성을 함께 클래스패스에 올려 REPL을 시작하려면 `console` 사용

```
> console
```

테스트 코드까지 포함하려면 마찬가지로 `Test` 스코프 추가

```
> Test/console
```

### 13. Scala REPL: 빌드 정의와 플러그인 포함 콘솔

프로젝트 소스가 아니라 `build.sbt`/`project/` 아래의 빌드 정의 코드와 여기에 적용된 플러그인들을 클래스패스에 올려 REPL을 여는 명령어는 `consoleProject`

```
> consoleProject
```

커스텀 태스크나 플러그인 API를 셸에서 직접 실험할 때 유용

### 14. Scala REPL: 진입/종료 시 실행 명령 지정

REPL이 시작될 때 자동으로 평가할 코드는 `initialCommands`로, 종료될 때 실행할 코드는 `cleanupCommands`로 지정 → 세 종류의 콘솔(`console`, `consoleQuick`, `consoleProject`) 각각에 독립적으로 스코핑 가능

```
console / initialCommands := """println("Hello from console")"""

consoleQuick / initialCommands := """println("Hello from consoleQuick")"""

consoleProject / initialCommands := """println("Hello from consoleProject")"""
```

```
console / cleanupCommands := """println("Bye from console")"""

consoleQuick / cleanupCommands := """println("Bye from consoleQuick")"""

consoleProject / cleanupCommands := """println("Bye from consoleProject")"""
```

### 15. Scala REPL: 프로젝트 코드 안에서 REPL 내장

셸이 아니라 애플리케이션 코드 내부에서 Scala REPL을 직접 띄워야 하는 경우(예: 디버깅용 대화형 콘솔 삽입) `scala.tools.nsc.interpreter`의 `Settings`/`Interpreter` 사용 → 이때 애플리케이션 클래스로더와 REPL 클래스로더가 어긋나면 `class scala.runtime.VolatileBooleanRef not found`류의 클래스로더 불일치 오류 발생 가능 → 이를 막으려면 `Settings.embeddedDefaults`에 인터프리터 클래스패스에 반드시 포함되어야 할 대표 클래스를 타입 파라미터로 전달

```
val settings = new Settings
settings.embeddedDefaults[MyType]
val interpreter = new Interpreter(settings, ...)

def x(a: Int, b: Int) = {
  import scala.tools.nsc.interpreter.ILoop
  ILoop.breakIf[MyType](a != b, "a" -> a, "b" -> b )
}
```

`MyType`은 REPL이 참조해야 할 클래스로더를 결정짓는 기준점 역할을 하는 임의의 클래스 → 보통 REPL을 호출하는 컨텍스트의 클래스를 전달하면 됨 → `ILoop.breakIf`는 조건이 참일 때 그 지점에서 REPL을 열어 중단점처럼 사용 가능

### 16. Scaladoc: javadoc과 scaladoc 자동 선택

`doc` 태스크를 실행하면 sbt가 소스 구성을 보고 자동으로 도구 선택 → Java 소스만 있으면 `javadoc`을, Scala 소스가 하나라도 있으면 `scaladoc`을 실행 → 별도 설정 없이 프로젝트 언어 구성에 따라 알아서 적절한 문서 생성기 선택됨

### 17. Scaladoc: 문서 생성 옵션 컴파일과 분리 설정

컴파일러 옵션과 scaladoc 생성 옵션은 서로 다른 목적을 가지므로, `Compile / doc / scalacOptions`처럼 `doc` 태스크에 스코핑하면 컴파일 시 쓰이는 `Compile / scalacOptions`와 독립적으로 관리 가능

전체를 새로 지정(덮어쓰기)할 때는 `:=` 사용

```
Compile / doc / scalacOptions := Seq("-groups", "-implicits")
```

기존 컴파일 옵션에 scaladoc 전용 옵션만 추가하고 싶을 때는 `++=`로 이어 붙임

```
Compile / doc / scalacOptions ++= Seq("-groups", "-implicits")
```

`-groups`는 멤버를 그룹으로 묶어 표시하고, `-implicits`는 암시적 변환으로 추가된 멤버까지 문서에 드러내는 옵션

### 18. Scaladoc: javadoc 옵션 설정

Java 소스에 대해 `javadoc`을 실행할 때 넘길 옵션은 `javacOptions`를 `doc` 태스크에 스코핑해서 지정 → scaladoc과 마찬가지로 `:=`(덮어쓰기)와 `++=`(추가) 둘 다 사용 가능

```
Compile / doc / javacOptions ++= Seq("-notimestamp", "-linksource")
```

`-notimestamp`는 생성 문서에 타임스탬프를 남기지 않아 재현 가능한 빌드에 유리하고, `-linksource`는 문서에서 원본 소스 코드로 링크를 거는 옵션

### 19. Scaladoc: 외부 의존성 문서 자동/수동 링크

프로젝트가 참조하는 클래스의 API 문서를 자동으로 찾아 링크해주는 기능 → 관리되는(managed) 의존성, 즉 `libraryDependencies`로 선언된 라이브러리는 `autoAPIMappings`를 켜면 자동으로 처리됨

```
autoAPIMappings := true
```

이 기능은 의존 라이브러리가 POM에 `apiURL`(또는 이에 준하는 메타데이터)을 게시해뒀을 때만 동작 → 로컬 jar처럼 관리되지 않는(unmanaged) 의존성은 자동 인식 불가 → `apiMappings`에 파일 경로와 문서 URL을 직접 매핑 필요

```
apiMappings += (
  (unmanagedBase.value / "a-library.jar") ->
    url("https://example.org/api/")
)
```

### 20. Scaladoc: 라이브러리 자신의 API 문서 위치 선언

라이브러리를 배포하는 입장에서, 자신의 문서를 사용하는 다른 프로젝트가 19번 항목의 `autoAPIMappings` 기능으로 자동 링크할 수 있도록 하려면 `apiURL`에 문서가 호스팅되는 위치를 지정해서 함께 퍼블리시

```
apiURL := Some(url("https://example.org/api/"))
```

### 21. 시작 시 작업 실행: onLoad 훅

sbt는 프로젝트 로드가 끝난 직후 자동으로 특정 명령어를 실행하는 훅 제공 → 전역 설정 `onLoad`는 `State => State` 타입의 함수이며, 셸에 입력하는 명령어를 상태(state) 앞에 이어 붙이는 방식으로 동작 → 짝을 이루는 `onUnload`는 `reload`나 `set` 등으로 프로젝트가 언로드될 때 실행됨

```scala
lazy val dependencyUpdates = taskKey[Unit]("foo")

// This prepends the String you would type into the shell
lazy val startupTransition: State => State = { s: State =>
  "dependencyUpdates" :: s
}

lazy val root = (project in file("."))
  .settings(
    ThisBuild / scalaVersion := "2.12.6",
    ThisBuild / organization := "com.example",
    name := "helloworld",
    dependencyUpdates := { println("hi") },

    // onLoad is scoped to Global because there's only one.
    Global / onLoad := {
      val old = (Global / onLoad).value
      // compose the new transition on top of the existing one
      // in case your plugins are using this hook.
      startupTransition compose old
    }
  )
```

핵심은 세 가지.

- `onLoad`는 프로젝트 전체에 하나만 존재하므로 반드시 `Global` 스코프에 설정 필요
- 플러그인이 이미 `onLoad`를 사용 중일 수 있으므로, 기존 값을 `val old = (Global / onLoad).value`로 먼저 받아둠
- 새 동작을 기존 동작 앞에 쌓기 위해 `startupTransition compose old`처럼 함수 합성 사용 → 이렇게 하면 기존 훅이 덮어써지지 않고 함께 실행됨

같은 방식으로 시작 시 실행할 명령어 문자열만 바꾸면 특정 서브프로젝트로 자동 전환하는 등의 커스텀 시작 동작도 구성 가능

# sbt How-to 모음: 클래스패스 / 경로 / 파일 생성 / 빌드 검사

> 원본: https://www.scala-sbt.org/1.x/docs/Howto-Classpaths.html, https://www.scala-sbt.org/1.x/docs/Howto-Customizing-Paths.html, https://www.scala-sbt.org/1.x/docs/Howto-Generating-Files.html, https://www.scala-sbt.org/1.x/docs/Howto-Inspect-the-Build.html

---

## 목차

1. [클래스패스 다루기](#1-클래스패스-다루기)
2. [경로 커스터마이징](#2-경로-커스터마이징)
3. [소스/리소스 파일 생성](#3-소스리소스-파일-생성)
4. [빌드 검사](#4-빌드-검사)

---

## 1. 클래스패스 다루기

### 1.1 새로운 관리형 아티팩트 타입을 클래스패스에 포함

**문제**: `mar` 같은 비표준 아티팩트 타입을 클래스패스 계산에 포함시켜야 한다.

**해법**: `classpathTypes`에 타입 문자열을 추가한다.

```scala
classpathTypes += "mar"
```

### 1.2 컴파일 클래스패스 획득

**문제**: 컴파일 시 사용하는 클래스패스(관리형 의존성만, 클래스 디렉터리 미포함일 수 있음)를 태스크 안에서 얻어야 한다.

**해법**: `dependencyClasspath`를 `Compile` 스코프로 조회한다.

```scala
example := {
  val cp: Seq[File] = (Compile / dependencyClasspath).value.files
  // cp를 활용하는 로직
}
```

### 1.3 런타임 클래스패스 획득

**문제**: 컴파일된 클래스와 런타임 의존성을 모두 포함한 클래스패스가 필요하다.

**해법**: `fullClasspath`를 `Runtime` 스코프로 조회한다.

```scala
example := {
  val cp: Seq[File] = (Runtime / fullClasspath).value.files
}
```

### 1.4 테스트 클래스패스 획득

**문제**: 테스트 컴파일 클래스와 테스트 전용 의존성까지 포함된 클래스패스가 필요하다.

**해법**: `fullClasspath`를 `Test` 스코프로 조회한다.

```scala
example := {
  val cp: Seq[File] = (Test / fullClasspath).value.files
}
```

### 1.5 클래스 디렉터리 대신 패키징된 JAR 사용

**문제**: `fullClasspath`가 컴파일된 클래스 디렉터리를 그대로 노출하는 대신, 패키징된 JAR 파일을 가리키도록 하고 싶다.

**해법**: `exportJars`를 켠다. 이 설정이 `true`가 되면 `exportedProducts`가 `packageBin`의 결과물(JAR)을 반환한다.

```scala
exportJars := true
```

### 1.6 특정 설정의 관리형 JAR 전체 획득

**문제**: `Compile` 설정에서 jar/zip 타입인 관리형 의존성 파일 목록만 뽑아야 한다.

**해법**: `Classpaths.managedJars`에 원하는 아티팩트 타입 집합과 `update` 결과를 넘긴다.

```scala
example := {
  val artifactTypes = Set("jar", "zip")
  val files = Classpaths.managedJars(Compile, artifactTypes, update.value)
}
```

### 1.7 클래스패스에서 순수 파일 목록만 추출

**문제**: 클래스패스는 `Seq[Attributed[File]]` 타입이라 메타데이터가 붙어 있는데, 파일 경로만 필요하다.

**해법**: `.files` 메서드로 `Seq[File]`을 얻는다.

```scala
val cp: Seq[Attributed[File]] = ???
val files: Seq[File] = cp.files
```

### 1.8 클래스패스 항목의 모듈/아티팩트 메타데이터 조회

**문제**: 각 클래스패스 항목이 어떤 `ModuleID`/`Artifact`에서 왔는지, 혹은 어떤 컴파일 분석(Analysis) 결과와 연결되는지 알아야 한다.

**해법**: `Attributed`의 `get` 메서드로 속성 키에 대응하는 값을 조회한다. 소스에서 온 항목만 `analysis`를, 관리형 의존성 항목만 `artifact`/`moduleID`를 가진다.

```scala
val classpath: Seq[Attributed[File]] = ???
for (entry <- classpath) yield {
  val art: Option[Artifact] = entry.get(artifact.key)
  val mod: Option[ModuleID] = entry.get(moduleID.key)
  val an: Option[inc.Analysis] = entry.get(analysis)
}
```

| 태스크/메서드 | 용도 |
|---|---|
| `classpathTypes` | 클래스패스에 포함할 아티팩트 타입 집합 |
| `Compile / dependencyClasspath` | 컴파일용 클래스패스(관리형 의존성 중심) |
| `Runtime / fullClasspath` | 런타임 클래스패스(컴파일 결과 + 의존성) |
| `Test / fullClasspath` | 테스트 클래스패스 |
| `exportJars` | true면 클래스 디렉터리 대신 JAR을 클래스패스에 노출 |
| `Classpaths.managedJars(config, types, updateReport)` | 설정별 관리형 JAR 목록 추출 |

---

## 2. 경로 커스터마이징

기본 디렉터리 구조(`src/main/scala`, `src/main/resources`, `lib` 등)를 바꾸고 싶을 때 사용하는 키 모음이다. 대부분 `Compile`/`Test` 스코프를 붙여 설정별로 다르게 지정한다.

### 2.1 Scala 소스 디렉터리 변경

```scala
Compile / scalaSource := baseDirectory.value / "src"
Test / scalaSource := baseDirectory.value / "test-src"
```

### 2.2 Java 소스 디렉터리 변경

```scala
Compile / javaSource := baseDirectory.value / "src"
Test / javaSource := baseDirectory.value / "test-src"
```

### 2.3 리소스 디렉터리 변경

```scala
Compile / resourceDirectory := baseDirectory.value / "resources"
Test / resourceDirectory := baseDirectory.value / "test-resources"
```

### 2.4 비관리 라이브러리 디렉터리(`lib`) 변경

프로젝트 전체에 적용하거나, 설정별로 다르게 지정할 수 있다.

```scala
// 프로젝트 전체
unmanagedBase := baseDirectory.value / "jars"

// Compile 설정만
Compile / unmanagedBase := baseDirectory.value / "lib" / "main"
```

### 2.5 프로젝트 루트 디렉터리의 소스 포함 비활성화

sbt는 기본적으로 프로젝트 루트에 있는 `.scala` 파일도 소스로 인식한다. 이 동작이 원치 않으면 끈다.

```scala
sourcesInBase := false
```

### 2.6 추가 소스 디렉터리 추가

기본 디렉터리(`scalaSource`, `javaSource`)를 대체하지 않고, 추가로 더할 때는 `unmanagedSourceDirectories`에 더한다.

```scala
Compile / unmanagedSourceDirectories += baseDirectory.value / "extra-src"
```

### 2.7 추가 리소스 디렉터리 추가

```scala
Compile / unmanagedResourceDirectories += baseDirectory.value / "extra-resources"
```

### 2.8 소스 파일 포함/제외 필터링

`includeFilter`/`excludeFilter`는 `unmanagedSources` 등 특정 태스크의 하위 키로 스코프를 좁혀 지정한다.

```scala
// 전체에 적용되는 제외 필터
unmanagedSources / excludeFilter := HiddenFileFilter || "*impl*"

// 설정별 포함 필터
Compile / unmanagedSources / includeFilter := "*.scala" || "*.java"
Test / unmanagedSources / includeFilter := HiddenFileFilter || "*impl*"
```

### 2.9 리소스 파일 포함/제외 필터링

```scala
unmanagedResources / excludeFilter := HiddenFileFilter || "*impl*"

Compile / unmanagedResources / includeFilter := "*.txt"
Test / unmanagedResources / includeFilter := "*.html"
```

### 2.10 비관리 라이브러리(JAR) 포함/제외 필터링

```scala
unmanagedJars / excludeFilter := HiddenFileFilter || "*.zip"

Compile / unmanagedJars / includeFilter := "*.jar"
Test / unmanagedJars / includeFilter := "*.jar" || "*.zip"
```

경로 관련 키를 정리하면 다음과 같다.

| 키 | 기본값 | 역할 |
|---|---|---|
| `scalaSource` | `src/main/scala`, `src/test/scala` | Scala 소스 루트 |
| `javaSource` | `src/main/java`, `src/test/java` | Java 소스 루트 |
| `resourceDirectory` | `src/main/resources`, `src/test/resources` | 리소스 루트 |
| `unmanagedBase` | `lib/` | 비관리 JAR 디렉터리 |
| `sourcesInBase` | `true` | 프로젝트 루트의 `.scala` 파일을 소스로 취급할지 여부 |
| `unmanagedSourceDirectories` | 위 소스 루트들 | 추가 소스 디렉터리 목록 |
| `unmanagedResourceDirectories` | 위 리소스 루트 | 추가 리소스 디렉터리 목록 |
| `includeFilter` / `excludeFilter` | 태스크마다 다름 | 파일 포함/제외 조건 |

`includeFilter`/`excludeFilter`는 `HiddenFileFilter`처럼 sbt가 제공하는 필터 조합자와 `||`(OR), `&&`(AND), `--`(차집합) 등으로 조합해 사용한다.

---

## 3. 소스/리소스 파일 생성

빌드 도중 코드나 설정 파일을 자동 생성해 컴파일/패키징 대상에 포함시키는 방법이다. 생성된 파일은 `sourceManaged`/`resourceManaged` 아래(`target/scala-*/src_managed`, `target/scala-*/resource_managed`)에 위치하며, 기본적으로 패키지된 소스 아티팩트에는 포함되지 않는다.

### 3.1 소스 파일 생성

**문제**: 빌드 시점에 Scala/Java 소스를 동적으로 만들어 컴파일 대상에 넣어야 한다.

**해법**: `sourceGenerators`에 `Seq[File]`을 반환하는 태스크를 추가한다. 반드시 `.value`가 아니라 `.taskValue`로 등록해야 한다.

```scala
Compile / sourceGenerators += Def.task {
  makeSomeSources((Compile / sourceManaged).value / "demo")
}.taskValue
```

간단한 예로, 앱 진입점 객체 하나를 직접 생성하는 경우는 다음과 같다.

```scala
Compile / sourceGenerators += Def.task {
  val file = (Compile / sourceManaged).value / "demo" / "Test.scala"
  IO.write(file, """object Test extends App { println("Hi") }""")
  Seq(file)
}.taskValue
```

`run`을 실행하면 생성된 `Test` 객체가 컴파일되어 "Hi"가 출력된다.

`makeSomeSources`의 시그니처는 다음과 같은 형태를 기대한다.

```scala
def makeSomeSources(base: File): Seq[File]
```

### 3.2 리소스 파일 생성

**문제**: 빌드 시점에 값이 채워지는 프로퍼티 파일 등의 리소스를 만들어야 한다.

**해법**: `resourceGenerators`에 `Seq[File]`을 반환하는 태스크를 추가한다.

```scala
Compile / resourceGenerators += Def.task {
  val file = (Compile / resourceManaged).value / "demo" / "myapp.properties"
  val contents = "name=%s\nversion=%s".format(name.value, version.value)
  IO.write(file, contents)
  Seq(file)
}.taskValue
```

생성된 파일은 `target/scala-*/resource_managed` 아래에 놓이며, `run`이나 `package` 실행 시 함께 처리된다.

### 3.3 공통 주의사항

- 태스크를 `sourceGenerators`/`resourceGenerators`에 추가할 때는 `.value`가 아니라 `.taskValue`를 사용한다. `.value`를 쓰면 즉시 평가된 결과(빈 목록일 수 있는 값)가 고정되어 버리므로, 매 빌드마다 다시 실행되는 태스크 자체를 등록해야 한다.
- 생성 로직이 매번 무조건 다시 쓰지 않도록, 입력이 바뀌었을 때만 재생성하는 캐싱을 넣는 편이 좋다. `sbt.Tracked.inputChanged`/`outputChanged` 같은 파일 추적 유틸리티를 활용할 수 있다.
- 생성된 소스/리소스는 기본적으로 `packageSrc` 결과물에는 들어가지 않는다. 패키지에 포함하려면 `mappings` 관련 설정을 별도로 조정해야 한다.
- 스코프를 `Test`로 바꾸면 테스트 전용 소스/리소스 생성에도 동일한 패턴을 쓸 수 있다.

| 키 | 반환 타입 | 역할 |
|---|---|---|
| `sourceGenerators` | `Seq[Task[Seq[File]]]` | 소스 파일을 생성하는 태스크 목록 |
| `resourceGenerators` | `Seq[Task[Seq[File]]]` | 리소스 파일을 생성하는 태스크 목록 |
| `sourceManaged` | `File` | 생성된 소스가 위치할 디렉터리 |
| `resourceManaged` | `File` | 생성된 리소스가 위치할 디렉터리 |

---

## 4. 빌드 검사

sbt 셸에서 현재 빌드 정의의 설정값, 태스크, 의존관계를 살펴볼 때 쓰는 명령어들이다.

### 4.1 명령어/태스크/설정 도움말 검색: `help`

```
> help
> help compile
```

인자 없이 실행하면 사용 가능한 명령어 전체 목록을 보여주고, 정규식을 인자로 주면 이름이 일치하는 항목만 검색한다.

### 4.2 사용 가능한 태스크 목록: `tasks`

```
> tasks
```

현재 프로젝트에서 실행 가능한 태스크 목록을 보여주며, 정규식으로 필터링할 수 있다.

### 4.3 사용 가능한 설정 목록: `settings`

```
> settings
```

현재 프로젝트에 정의된 설정 키 목록을 보여주며, 마찬가지로 정규식 검색이 가능하다.

### 4.4 설정/태스크의 상세 정보와 의존관계: `inspect`

```
> inspect Test/compile
> inspect tree clean
> inspect scalacOptions
```

`inspect`는 대상 키에 대해 다음 정보를 출력한다.

- 정방향 의존성(Dependencies): 이 키가 값을 계산하기 위해 참조하는 다른 키
- 역방향 의존성(Reverse dependencies): 이 키를 참조하는 다른 키
- 값의 데이터 타입
- 현재 스코프에서 적용된 설정값의 출처(어느 빌드 파일에서 정의됐는지)

`inspect tree`를 쓰면 의존관계를 트리 형태로 펼쳐서 볼 수 있다.

### 4.5 로드된 프로젝트 목록: `projects`

```
> projects
[info] In file:/home/user/demo/
[info]   * parent
[info]     sub
```

현재 빌드에 로드된 모든 서브프로젝트를 파일 단위로 묶어 보여주며, `*`는 현재 활성 프로젝트를 나타낸다.

### 4.6 세션 임시 설정 조회: `session`

```
> session list
> session list-all
```

`set` 명령으로 sbt 셸에서 즉석으로 덮어쓴(세션) 설정들을 확인할 때 사용한다.

### 4.7 빌드/sbt 기본 정보: `about`

```
> about
```

sbt 버전, Scala 버전, 현재 로드된 플러그인 등 빌드 환경 정보를 요약해서 보여준다.

### 4.8 설정값/태스크 실행 결과 출력: `show`

```
> show organization
> show update
> show Test/dependencyClasspath
> show compile:discoveredMainClasses
> show Test/definedTestNames
```

`show <키>`는 설정이면 그 값을, 태스크면 태스크를 실행한 결과를 콘솔에 출력한다. `run` 등과 달리 부수효과 없이 값만 확인하고 싶을 때 우선적으로 쓰는 명령이다.

빌드 검사에 자주 쓰이는 명령어를 정리하면 다음과 같다.

| 명령어 | 용도 |
|---|---|
| `help [regex]` | 명령어/태스크/설정 도움말 검색 |
| `tasks [regex]` | 사용 가능한 태스크 목록 |
| `settings [regex]` | 사용 가능한 설정 목록 |
| `inspect <키>` | 키의 의존관계, 타입, 정의 위치 확인 |
| `inspect tree <키>` | 의존관계를 트리로 확인 |
| `projects` | 로드된 서브프로젝트 목록 |
| `session list[-all]` | 세션 임시 설정 확인 |
| `about` | sbt/빌드 기본 정보 |
| `show <키>` | 설정값 또는 태스크 실행 결과 출력 |


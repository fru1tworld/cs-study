# Mill 마이그레이션

> 원본: https://mill-build.org/mill/migrating/migrating.html, https://mill-build.org/mill/migrating/auto-migrating.html

---

## 목차

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

## 1. 마이그레이션 개요

Maven/Gradle/sbt로 작성된 기존 빌드를 Mill로 옮기는 작업은 단순 변환이 아니라 프로젝트 구조·의존성·플러그인·커스텀 로직을 하나씩 대응시키는 과정입니다. Mill은 이 과정을 돕기 위해 `./mill init` 자동 임포터를 제공하지만, 이 도구는 뼈대만 만들어줄 뿐이며 나머지는 수작업 변환이 필요합니다.

핵심 메시지는 두 가지입니다.

- 대부분의 빌드 시스템은 실제로는 소수의 서드파티 플러그인과 적은 양의 커스텀 로직으로 이루어져 있어서, 기술적 난이도 자체는 높지 않습니다.
- 프로젝트가 오래되고 활발하게 유지보수될수록 Mill로 옮겼을 때 얻는 빌드 속도·사용성 이득이 누적되어 투자 가치가 커집니다.

## 2. 예상 소요 시간

프로젝트 규모와 모듈 수에 따라 마이그레이션 시간은 크게 달라집니다.

| 프로젝트 | 규모 | 모듈 수 | 예상 소요 |
|---|---|---|---|
| JimFS | ~26k LOC | 1 | 약 2시간 |
| Apache Commons-IO | ~100k LOC | 1 | 약 2시간 |
| Gatling | ~70k LOC | 21 | 약 1일 |
| Arrow | ~60k LOC | 22 | 약 5일 |
| Mockito | ~100k LOC | 22 | 약 5일 |
| Netty | ~500k LOC | 47 | 약 5일 |

단일 모듈 프로젝트는 코드량이 많아도 몇 시간 내로 끝나는 반면, 모듈 수가 많은 다중모듈 프로젝트는 모듈 간 의존성 그래프를 다시 구성해야 해서 규모(LOC)보다 모듈 개수가 소요 시간에 더 큰 영향을 줍니다.

## 3. 마이그레이션 4단계 전략

기존 빌드를 곧바로 지우지 않고, 두 빌드 시스템을 병행하면서 단계적으로 전환하는 방식을 권장합니다.

1. **기존 빌드 보존**: Maven/Gradle/sbt 설정을 그대로 두어 언제든 롤백 가능한 상태를 유지합니다.
2. **Mill 병렬 구성**: 별도로 Mill 빌드를 설정하고, 하위 프로젝트를 Mill Module로 옮기고, 서드파티 의존성을 이전합니다.
3. **Mill을 기본값으로 전환**: 일정 기간 두 시스템을 동시에 운용하며 CI/로컬에서 Mill 결과를 검증합니다.
4. **기존 빌드 제거**: 충분히 검증된 뒤 Maven/Gradle/sbt 설정 파일을 삭제합니다.

이 단계별 접근 덕분에 마이그레이션 도중 문제가 생겨도 원래 빌드로 즉시 되돌아갈 수 있습니다.

## 4. 하위 프로젝트를 Module로 변환

언어별로 대응되는 Mill Module 트레이트가 다릅니다.

| 원본 언어 | 사용할 Module | 테스트 Module |
|---|---|---|
| Java | `MavenModule` | `MavenTests` |
| Kotlin | `KotlinMavenModule` | `KotlinMavenTests` |
| Scala | `SbtModule` | `SbtTests` |

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

디렉터리 중첩 구조(예: `bar/qux/`)는 Module을 중첩 `object`로 그대로 반영합니다.

```scala
object bar extends MavenModule {
  object qux extends MavenModule
}
```

모듈 간 의존성은 `moduleDeps`로 선언합니다. 테스트 모듈에서는 상위 Module이 이미 가진 `moduleDeps`를 잃지 않도록 `super.moduleDeps`에 이어붙입니다.

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

변환한 구조가 원본과 맞는지는 아래 명령으로 확인합니다.

```bash
./mill visualize _.compile_     # 모듈 의존 그래프를 SVG로 시각화
./mill show _.sources           # 각 모듈이 인식하는 소스 폴더 확인
```

## 5. 서드파티 의존성 변환

빌드 도구마다 의존성 선언 문법이 다르지만, groupId/artifactId/version 세 값을 옮기는 것은 동일합니다.

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

Scala 크로스 빌드 의존성(sbt의 `%%`)은 Mill에서 더블 콜론(`::`)으로 바뀝니다.

```scala
// sbt
libraryDependencies += "com.lihaoyi" %% "scalatags" % "0.12.0"

// Mill
def mvnDeps = Seq(mvn"com.lihaoyi::scalatags:0.12.0")
```

의존성 범위(scope)는 오버라이드하는 def로 구분합니다.

```scala
def mvnDeps = Seq(...)          // 일반(compile+runtime) 의존성
def compileMvnDeps = Seq(...)   // 컴파일 전용 (Maven의 provided에 대응)
def runMvnDeps = Seq(...)       // 런타임 전용
```

테스트 전용 의존성은 `object test` 안에 동일한 방식으로 선언합니다.

## 6. 서드파티 플러그인·커스텀 로직 변환

Maven/Gradle/sbt 플러그인은 대부분 Mill의 내장 기능으로 대체할 수 있습니다.

- 린팅: 각 언어별(Java/Scala/Kotlin) 린팅 문서 참고
- 테스트: 언어별 테스트 설정 문서 참고
- 발행(publish): 패키징·발행 가이드 참고
- 그 외: Mill Contrib 모듈 또는 서드파티 플러그인 활용

커스텀 빌드 로직(스크립트 태스크 등)은 Mill의 커스텀 빌드 로직 메커니즘으로 옮깁니다.

- 파일 시스템 조작: **OS-Lib**
- JSON/바이너리 직렬화: **uPickle**
- HTTP 요청: **Requests-Scala**
- Maven Central의 임의 라이브러리를 직접 import해서 빌드 로직에 사용 가능 (Import Libraries and Plugins 문서 참고)
- 더 복잡한 통합이 필요하면 Running Dynamic JVM Code 문서 참고

## 7. 자주 겪는 문제와 해결

| 문제 | 해결 |
|---|---|
| 테스트가 리소스 파일에 접근 못함 (샌드박스 실행 때문) | `resources/` 대신 `$MILL_TEST_RESOURCE_DIR` 환경변수 사용 |
| 프로젝트 루트 경로가 필요함 | `mill.api.BuildCtx.workspaceRoot` 또는 `WorkspaceRoot.workspaceRoot` 사용 |
| JVM 버전을 프로젝트별로 다르게 지정해야 함 | Managing JVM Versions 문서 참고 |
| 컴파일 플래그를 세밀하게 제어해야 함 | 선언적 설정(Compilation & Execution Flags) 문서 참고 |
| 어노테이션 프로세서 설정 | Annotation Processors 문서 참고 |
| JNI 네이티브 코드 빌드 | Native C Code with JNI 문서 참고 |
| Spring Boot/Micronaut 같은 프레임워크 통합 | 해당 프레임워크 전용 가이드 참고 |

## 8. 마이그레이션 이후 정리

빌드가 정상 동작하는 것을 확인한 뒤에는 유지보수성을 높이는 방향으로 다듬습니다.

- **공통 설정 추출**: 여러 모듈에서 반복되는 설정을 trait으로 뽑아 상속시킵니다.

```scala
trait CommonConfig extends MavenModule {
  def scalaVersion = "3.3.0"
}
```

- **Multi-File Builds**: 빌드 로직을 소스 코드와 같은 위치에 파일 단위로 분리해 관리합니다.
- **커스텀 플러그인 작성**: 조직 내에서 반복되는 빌드 로직을 재사용 가능한 플러그인으로 만듭니다.

---

## 9. 자동 마이그레이션 도구 (`mill init`)

`./mill init`은 Maven/Gradle/sbt 빌드를 감지해 Mill 빌드로 자동 변환하는 명령어입니다. 완전한 마이그레이션을 보장하지는 않고, 초기 뼈대(스캐폴딩)를 생성해주는 역할입니다.

빌드 도구별 인식 방식:

| 원본 | 감지 대상 | 변환 결과 |
|---|---|---|
| Maven | `pom.xml` | `MavenModule` (하위에 `src/test`가 있으면 중첩 `test` Module도 함께 생성) |
| Gradle | `.gradle` / `.gradle.kts` 파일 | 해당 Module + 테스트 프레임워크 자동 구성 |
| sbt | sbt 프로젝트 구조 | `SbtModule`, Scala 소스 자동 처리 |

기본 사용법:

```bash
./mill init
```

실행하면 다음이 생성됩니다.

- 루트 모듈용 `build.mill`
- 하위(중첩) 모듈용 `package.mill`
- 공유 설정을 담는 `mill-build` 디렉터리

JVM 버전을 지정하고 싶으면 옵션을 추가합니다.

```bash
./mill init --mill-jvm-id 25
```

이 값은 생성되는 `build.mill`의 `mill-jvm-version` 설정에 반영됩니다. 전체 옵션은 다음으로 확인합니다.

```bash
./mill init --help
```

`init`은 여러 번 실행해도 되며, 옵션을 바꿔가며 재생성할 수 있습니다.

## 10. `mill init` 실전 사례

### Maven 프로젝트 (davidmoten/geo)

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

### Gradle 프로젝트 (komamitsu/fluency)

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

### sbt 프로젝트 (scala/scala3-example-project)

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

### 대규모 다중모듈 프로젝트 (iluwatar/java-design-patterns, 363개 서브프로젝트)

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

363개 서브프로젝트 규모에서도 `init` 한 번과 소수의 수동 정리(호환 안 되는 테스트 삭제 등)만으로 컴파일·테스트가 통과했다는 점이 자동 임포터의 실용성을 보여줍니다.

## 11. `mill init`의 한계

자동 변환이 다루지 못하는 영역이 명확히 존재합니다.

- **빌드 확장(Build Extensions)**: 커스텀 플러그인, 커스텀 태스크는 옮겨주지 않습니다.
- **빌드 프로필(Build Profiles)**: Maven profile 같은 조건부 빌드 설정은 변환 대상이 아닙니다.
- **비Java 소스(Non-Java Sources)**: Java 계열 이외 언어의 소스는 자동 처리되지 않습니다.

이런 부분은 아래 방법으로 직접 보완해야 합니다.

- Mill Contrib 또는 서드파티 플러그인 구성
- 커스텀 플러그인 작성
- 커스텀 태스크 정의
- 커스텀 크로스 모듈 활용

## 12. `mill init` 이후 트러블슈팅

### 모듈/태스크 이름 충돌

모듈 이름과 태스크 이름이 겹치면 컴파일 오류가 납니다. 모듈명을 바꾸거나 백틱으로 감싸서 (예: `` `moduleName` ``) 충돌을 피합니다.

### JPMS(Java Platform Module System) 오류

```
error: module not found: org.apache.commons.compress
```

이런 오류가 나면 `javacOptions`에 필요한 Java 모듈 옵션(`--add-modules` 등)을 추가해야 합니다.

### 테스트 컴파일 오류

원인은 대개 둘 중 하나입니다.

- 지원되지 않는 테스트 프레임워크 버전을 쓰고 있음 → 최신 버전으로 업그레이드
- `compileMvnDeps`로 선언한 의존성이 중첩 테스트 모듈의 전이 의존성에 포함되지 않음 → 필요한 의존성을 `mvnDeps` 또는 `runMvnDeps`에 명시적으로 추가

### sbt 프로젝트의 런타임 오류

```
java.io.IOError: java.lang.RuntimeException: /packages cannot be represented as URI
```

이 오류는 오래된 sbt/Java 조합에서 발생하는 경우가 많습니다. 프로젝트의 sbt 버전을 최신(권장: v1.10.10 이상)으로 올리고 적절한 Java 버전을 확인한 뒤 재시도합니다.

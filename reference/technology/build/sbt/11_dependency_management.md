# sbt 의존성 관리

> 원본: https://www.scala-sbt.org/1.x/docs/Artifacts.html, https://www.scala-sbt.org/1.x/docs/Dependency-Management-Flow.html, https://www.scala-sbt.org/1.x/docs/Library-Management.html, https://www.scala-sbt.org/1.x/docs/Proxy-Repositories.html, https://www.scala-sbt.org/1.x/docs/Publishing.html, https://www.scala-sbt.org/1.x/docs/Resolvers.html, https://www.scala-sbt.org/1.x/docs/Update-Report.html, https://www.scala-sbt.org/1.x/docs/Cached-Resolution.html

---

## 목차

1. [아티팩트란 무엇인가](#1-아티팩트란-무엇인가)
2. [의존성 해석 흐름 (update 태스크)](#2-의존성-해석-흐름-update-태스크)
3. [라이브러리 관리: 수동 vs 자동](#3-라이브러리-관리-수동-vs-자동)
4. [libraryDependencies 문법](#4-librarydependencies-문법)
5. [Ivy와 Coursier, 충돌 관리](#5-ivy와-coursier-충돌-관리)
6. [resolver 설정](#6-resolver-설정)
7. [사내 프록시 저장소](#7-사내-프록시-저장소)
8. [퍼블리싱](#8-퍼블리싱)
9. [UpdateReport 조회](#9-updatereport-조회)
10. [Cached Resolution](#10-cached-resolution)

---

## 1. 아티팩트란 무엇인가

빌드 과정에서 만들어져 저장소에 올라가는 산출물을 아티팩트라고 부른다. sbt 프로젝트는 기본적으로 세 종류의 아티팩트를 퍼블리시한다.

| 아티팩트 | 태스크 | 내용 |
|---|---|---|
| 메인 바이너리 jar | `packageBin` | 컴파일된 클래스와 리소스 |
| 소스 jar | `packageSrc` | 소스 코드 |
| API 문서 jar | `packageDoc` | scaladoc/javadoc |

`Test` 설정에서도 동일하게 세 종류를 만들 수 있으나 기본은 꺼져 있다.

```scala
// 테스트 바이너리 jar를 퍼블리시 대상에 포함
Test / publishArtifact := true

// 메인 아티팩트 중 문서/소스만 끄고 싶을 때
Compile / packageDoc / publishArtifact := false
Compile / packageSrc / publishArtifact := false
```

### Artifact 값과 artifactName

아티팩트 하나하나는 `Artifact` 값(이름, 타입, 확장자, classifier, extra 속성)으로 표현되며, 실제 파일 이름은 `artifactName` 설정이 결정한다. 시그니처는 `(ScalaVersion, ModuleID, Artifact) => String`이다.

```scala
Artifact("myproject", "zip", "zip")
Artifact("myproject", "image", "jpg")
Artifact("myproject", "jdk15")   // classifier만 지정하는 축약형

artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
  artifact.name + "-" + module.revision + "." + artifact.extension
}
```

기본 아티팩트의 타입/확장자를 바꾸고 싶으면 해당 태스크의 `artifact` 설정을 오버라이드한다.

```scala
Compile / packageBin / artifact := {
  val prev: Artifact = (Compile / packageBin / artifact).value
  prev.withType("bundle")
}
```

### 커스텀 아티팩트 추가

`packageBin`/`packageSrc`/`packageDoc` 외에 별도 산출물(예: WAR 파일, 압축 이미지)을 퍼블리시 목록에 넣으려면 `addArtifact`로 태스크와 `Artifact` 값을 연결한다.

```scala
// WAR 파일을 퍼블리시 아티팩트로 추가
lazy val app = (project in file("app"))
  .settings(
    Compile / packageBin / publishArtifact := false,
    Compile / packageWar / artifact := {
      val prev: Artifact = (Compile / packageWar / artifact).value
      prev.withType("war").withExtension("war")
    },
    addArtifact(Compile / packageWar / artifact, packageWar)
  )
```

`(Artifact, File)` 쌍을 직접 다루고 싶다면 `packagedArtifact` 태스크를 참조한다.

```scala
myTask := {
  val (art, file) = (Compile / packageBin / packagedArtifact).value
  println(s"artifact: $art, file: ${file.getAbsolutePath}")
}
```

### 의존성 쪽에서 아티팩트 지정

의존하는 라이브러리가 classifier가 붙은 아티팩트를 가질 때는 `.artifacts(...)` 또는 축약형 `.classifier(...)`를 쓴다.

```scala
libraryDependencies += ("org" % "name" % "rev")
  .artifacts(Artifact("name", "type", "ext"))

// 아래와 동일
("org.testng" % "testng" % "5.7").classifier("jdk15")
```

크로스빌딩 시에는 `artifactName`에 전달되는 `ScalaVersion` 인자(전체 버전과 바이너리 호환 버전을 모두 담고 있음)를 이용해 버전별로 이름이 다른 아티팩트를 만들 수 있다.

---

## 2. 의존성 해석 흐름 (update 태스크)

sbt의 의존성 관리는 `update` 태스크를 중심으로 돌아간다. `libraryDependencies`와 `resolvers` 설정을 읽어 실제로 저장소에서 아티팩트를 내려받고, 그 결과를 `UpdateReport`로 만든다. `compile`, `run`, `test` 같은 태스크는 이 리포트를 참조해 클래스패스를 구성한다.

`compile` 실행 전에는 항상 `update`가 선행되지만, 매번 원격 해석을 다시 하면 느려지므로 sbt는 캐시된 해석 결과를 최대한 재사용한다. 판단 규칙은 다음과 같다.

| 상황 | 동작 |
|---|---|
| 의존성 설정이 그대로고 캐시 파일이 존재 | 재해석 생략 |
| `libraryDependencies` 등 설정이 바뀜 | 자동으로 재해석 |
| `update`를 직접 실행 | 설정 변경 여부와 무관하게 강제 재해석 |
| `clean` 실행 | 캐시가 지워져 다음 빌드에서 재해석 |
| `update / skip := true` | 해석 자체를 건너뜀(의존 태스크가 실패할 수 있음) |

### 엔진: Ivy vs Coursier

sbt 1.3.0부터 라이브러리 관리(LM) API는 Apache Ivy 구현과 Coursier 구현을 모두 지원하며, 기본값은 Coursier다. Ivy로 되돌리려면 다음처럼 설정한다.

```scala
ThisBuild / useCoursier := false
```

### SNAPSHOT과 해석 성능

SNAPSHOT 버전은 빌드에 가변성을 끌어들이고, 매번 네트워크로 최신 여부를 확인해야 하므로 해석 속도를 떨어뜨린다. Coursier는 기본적으로 SNAPSHOT 아티팩트에 24시간 TTL을 부여해 불필요한 네트워크 IO를 줄인다. 이 캐시를 무시하고 즉시 재확인하고 싶다면 환경 변수로 TTL을 0으로 만든다.

```bash
COURSIER_TTL=0s sbt update
```

---

## 3. 라이브러리 관리: 수동 vs 자동

sbt는 의존성을 관리하는 두 가지 방식을 함께 지원한다.

### 수동 관리 (unmanaged)

jar 파일을 프로젝트의 `lib` 디렉터리에 직접 복사해두면 sbt가 컴파일/테스트/실행 시 클래스패스에 자동으로 얹는다. 별도의 빌드 설정 변경 없이 동작하는 대신, 버전 추적과 전이 의존성 해결은 전부 개발자 몫이다.

```scala
// unmanaged jar 디렉터리 위치 변경
unmanagedBase := baseDirectory.value / "custom_lib"

// 여러 디렉터리를 조합해 지정
Compile / unmanagedJars ++= {
  val base = baseDirectory.value
  val baseDirectories = (base / "libA") +++ (base / "b" / "lib") +++ (base / "libC")
  val customJars = (baseDirectories ** "*.jar") +++ (base / "d" / "my.jar")
  customJars.classpath
}
```

### 자동 관리 (managed)

`libraryDependencies`에 좌표를 선언하면 sbt가 저장소에서 내려받아 관리한다. 규모가 커질수록 실질적으로 표준이 되는 방식이며, 이후 절에서 다루는 문법이 모두 이 방식에 해당한다.

---

## 4. libraryDependencies 문법

### 기본 선언

```scala
libraryDependencies += groupID % artifactID % revision
libraryDependencies += groupID % artifactID % revision % configuration

libraryDependencies ++= Seq(
  groupID %% artifactID % revision,
  groupID %% otherID % otherRevision
)
```

`%`와 `%%`의 차이는 대상 라이브러리가 Scala로 작성됐는지 여부다.

| 연산자 | 대상 | 예시 |
|---|---|---|
| `%` | Java 라이브러리(버전 suffix 없음) | `"log4j" % "log4j" % "1.2.15"` |
| `%%` | Scala 라이브러리(빌드 중인 Scala 버전 suffix 자동 부여) | `"org.scalatest" %% "scalatest" % "3.2.17"` |

`%%`는 사용 중인 Scala 바이너리 버전에 맞춰 `scalatest_2.13`처럼 아티팩트 이름 뒤에 suffix를 붙여준다.

### 버전 범위

```scala
libraryDependencies += "org.example" % "lib" % "latest.integration"
libraryDependencies += "org.example" % "lib" % "2.9.+"
libraryDependencies += "org.example" % "lib" % "[1.0,)"
```

### classifier와 소스/문서 동시 다운로드

```scala
libraryDependencies += "org.testng" % "testng" % "5.7" classifier "jdk15"

libraryDependencies += "org.lwjgl.lwjgl" % "lwjgl-platform" % lwjglVersion classifier "natives-windows" classifier "natives-linux" classifier "natives-osx"

libraryDependencies += "org.apache.felix" % "org.apache.felix.framework" % "1.8.0" withSources() withJavadoc()
```

### 전이 의존성 제어

```scala
// 전이 의존성 전체 차단
libraryDependencies += "org.apache.felix" % "org.apache.felix.framework" % "1.8.0" intransitive()

// 개별 제외 (pom에 반영됨)
libraryDependencies += "log4j" % "log4j" % "1.2.15" exclude("javax.jms", "jms")

// 다건 제외 (pom 미반영)
libraryDependencies += "log4j" % "log4j" % "1.2.15" excludeAll(
  ExclusionRule(organization = "com.sun.jdmk"),
  ExclusionRule(organization = "com.sun.jmx")
)

// 모든 의존성 대상 공통 제외 규칙
excludeDependencies ++= Seq(
  ExclusionRule("commons-logging", "commons-logging")
)
```

### 구성 매핑 (Configuration mapping)

Ivy 구성(configuration)을 프로젝트 classpath와 연결하는 문법이다.

```scala
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.17" % "test->compile"
```

커스텀 구성(configuration)도 선언 가능하다.

```scala
val JS = config("js") hide

ivyConfigurations += JS

libraryDependencies += "jquery" % "jquery" % "3.2.1" % "js->default" from "https://code.jquery.com/jquery-3.2.1.min.js"

Compile / resources ++= update.value.select(configurationFilter("js"))
```

### 직접 URL 지정, extra 속성

```scala
libraryDependencies += "slinky" % "slinky" % "2.1" from
  "https://slinky2.googlecode.com/svn/artifacts/2.1/slinky.jar"

libraryDependencies += "org" % "name" % "rev" extra("color" -> "blue")
```

### Ivy XML을 직접 쓰는 경우

```scala
ivyXML :=
  <dependencies>
    <dependency org="javax.mail" name="mail" rev="1.4.2">
      <exclude module="activation"/>
    </dependency>
  </dependencies>
```

---

## 5. Ivy와 Coursier, 충돌 관리

### 체크섬

```scala
update / checksums := Nil        // 검증 비활성화
publishLocal / checksums := Nil  // 퍼블리시 시 체크섬 미포함
publish / checksums := Nil

checksums := Seq("sha1", "md5")  // 기본값
```

### 충돌 관리자와 버전 오버라이드

```scala
conflictManager := ConflictManager.strict

// 전이 의존성 어딘가에서 끌려온 버전을 강제로 고정 (직접 의존성으로 추가하지 않음, pom 미반영)
dependencyOverrides += "log4j" % "log4j" % "1.2.16"

// Ivy 전용 강제 지정 (권장하지 않음, pom 미반영)
libraryDependencies += "log4j" % "log4j" % "1.2.14" force()
```

### 진단용 명령어

```bash
show update             # 해석된 의존성 트리 확인
evicted                 # 버전 충돌로 밀려난(evict) 라이브러리 목록
updateClassifiers       # 소스/문서 등 classifier 아티팩트 전체 다운로드
updateSbtClassifiers    # sbt 자체 classifier 다운로드
```

### 알려진 제약

- POM의 `relativePath`를 지정하면 오류가 난다.
- Ivy는 POM의 `<repositories>`를 무시한다. 필요하면 `ivysettings.xml`로 우회해야 한다.
- `override`/`force`는 Ivy 전용 개념이라 생성되는 pom.xml에는 반영되지 않는다.

---

## 6. resolver 설정

`resolvers`는 의존성을 내려받을 저장소 위치를 지정하는 설정이다. 기본값은 Maven Central과 로컬 Ivy 저장소다.

```scala
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
```

### 자주 쓰는 사전 정의 resolver

| 대상 | 설정 |
|---|---|
| 로컬 Maven 저장소 | `Resolver.mavenLocal` |
| Maven 스타일 캐시 | `MavenCache("local-maven", file("path/to/repo"))` |
| Sonatype OSS | `Resolver.sonatypeOssRepos("public")` |
| Typesafe | `Resolver.typesafeRepo("releases")` |
| sbt 플러그인 저장소 | `Resolver.sbtPluginRepo("releases")` |
| JCenter | `Resolver.jcenterRepo` |

### 커스텀 resolver

```scala
// 파일시스템
resolvers += Resolver.file("my-test-repo", file("test")) transactional()

// Maven 스타일 URL
resolvers += Resolver.url("my-test-repo", url("https://example.org/repo-releases/"))

// Ivy 스타일 URL
resolvers += Resolver.url("my-test-repo", url)(Resolver.ivyStylePatterns)

// SFTP / SSH
resolvers += Resolver.sftp("my-sftp-repo", "example.org")
resolvers += Resolver.ssh("my-ssh-repo", "example.org") as("user", "password")
```

레이아웃이 표준과 다른 저장소는 패턴을 직접 지정한다.

```scala
resolvers += Resolver.url("my-test-repo", url)(
  Patterns("[organisation]/[module]/[revision]/[artifact].[ext]")
)
```

기본 저장소를 배제하고 사내 저장소만 쓰고 싶다면 다음처럼 조합한다.

```scala
externalResolvers := Resolver.combineDefaultResolvers(resolvers.value.toVector, mavenCentral = false)
```

resolver는 선언한 순서대로 조회되지만, 아래 절에서 다루는 전역 `~/.sbt/repositories` 설정이 있으면 그쪽이 우선 적용될 수 있다.

---

## 7. 사내 프록시 저장소

여러 개발자가 같은 아티팩트를 반복해서 원격 저장소에서 내려받는 비효율을 줄이려면, 조직 내부에 단일 진입점 역할을 하는 프록시 저장소(Artifactory, Nexus Repository Manager, Apache Archiva, CloudRepo 등)를 둔다. 다운로드 속도와 보안 통제 두 측면에서 이점이 있다.

설정은 두 곳에서 이뤄진다.

1. `~/.sbt/repositories` 파일
2. sbt 런처(launcher) 실행 옵션

### repositories 파일

```ini
[repositories]
  local
  my-ivy-proxy-releases: http://repo.company.com/ivy-releases/,
    [organization]/[module]/(scala_[scalaVersion]/)(sbt_[sbtVersion]/)[revision]/[type]s/[artifact](-[classifier]).[ext]
  my-maven-proxy-releases: http://repo.company.com/maven-releases/
```

| 항목 | 역할 |
|---|---|
| `local` | `publishLocal`로 만든 로컬 아티팩트 공유 |
| `my-ivy-proxy-releases` | sbt 자신과 플러그인 해석용(Ivy 패턴 필수) |
| `my-maven-proxy-releases` | Maven Central을 프록시하는 사내 저장소 |

Maven용과 Ivy용 프록시는 반드시 분리해서 구성해야 한다. sbt 플러그인은 Ivy 특유의 레이아웃을 쓰므로 두 종류를 한 저장소에 병합하면 충돌이 난다. Nexus를 쓸 경우 `https://repo.scala-sbt.org/scalasbt/sbt-plugin-releases` 매핑의 레이아웃 정책을 permissive로 두지 않으면 404로 해석이 실패할 수 있다.

### 자격 증명

```bash
export SBT_CREDENTIALS="$HOME/.ivy2/.credentials"
```

```
realm=My Nexus Repository Manager
host=my.artifact.repo.net
user=admin
password=admin123
```

### 런처 옵션

```bash
# 프로젝트별 resolvers 설정을 무시하고 repositories 파일을 우선 적용 (기본값 false)
-Dsbt.override.build.repos=true

# repositories 파일 경로를 직접 지정
-Dsbt.repository.config=<path-to-your-repo-file>
```

---

## 8. 퍼블리싱

`publish`는 원격 저장소에, `publishLocal`은 로컬 Ivy 저장소(`$HOME/.ivy2/local/`)에 아티팩트와 메타데이터(ivy.xml 또는 pom.xml)를 올리는 태스크다.

특정 서브프로젝트(대개 루트 프로젝트)를 퍼블리시 대상에서 빼려면 다음처럼 설정한다.

```scala
publish / skip := true
```

### publishTo

```scala
publishTo := Some("Sonatype Snapshots Nexus" at
  "https://oss.sonatype.org/content/repositories/snapshots")

publishTo := Some(MavenCache("local-maven", file("path/to/maven-repo/releases")))

publishTo := Some(Resolver.file("local-ivy", file("path/to/ivy-repo/releases")))
```

릴리스/스냅샷을 버전에 따라 분기하는 패턴이 흔하다.

```scala
publishTo := {
  val nexus = "https://my.artifact.repo.net/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
```

### 로컬 퍼블리싱

```scala
ThisBuild / organization := "org.me"
ThisBuild / version      := "0.1-SNAPSHOT"
name := "My Project"
```

`publishLocal`은 `$HOME/.ivy2/local/`에, `publishM2`는 `$HOME/.m2/repository/`에 결과를 남긴다. 로컬 머신의 다른 프로젝트에서는 그냥 좌표를 의존성으로 선언하면 소비할 수 있다.

```scala
libraryDependencies += "org.me" %% "my-project" % "0.1-SNAPSHOT"
```

### 자격 증명

파일 기반 방식을 권장한다.

```scala
credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
```

```
realm=Sonatype Nexus Repository Manager
host=my.artifact.repo.net
user=admin
password=admin123
```

인라인으로 직접 넣을 수도 있다.

```scala
credentials += Credentials("Sonatype Nexus Repository Manager",
  "my.artifact.repo.net", "admin", "admin123")
```

자격 증명 매칭은 `realm`과 `host` 두 값을 모두 사용한다. `realm`은 서버가 돌려주는 HTTP Basic Authentication 응답 헤더에서 확인할 수 있다.

### 크로스 퍼블리싱

여러 Scala 버전으로 동시에 퍼블리시하려면 `+` 접두사를 붙인다.

```bash
+ publish
```

기본적으로 sbt는 사용 중인 Scala 바이너리 버전을 아티팩트 이름에 붙여 퍼블리시한다(예: `example_2.13`). 순수 Java 라이브러리나 컴파일러 플러그인처럼 이 규칙이 맞지 않는 경우 `CrossVersion` 설정을 바꿔야 한다.

### POM 커스터마이징

```scala
pomExtra := <something></something>

pomPostProcess := { (node: Node) => /* XML 가공 */ node }

pomIncludeRepository := { (repo: MavenRepository) =>
  repo.root.startsWith("file:")
}
```

### versionScheme (sbt 1.4.0+)

```scala
ThisBuild / versionScheme := Some("early-semver")
```

| 값 | 의미 |
|---|---|
| `early-semver` | 0.y.z 단계에서도 패치 버전 사이 바이너리 호환을 기대 |
| `semver-spec` | 모든 0.y.z를 초기 개발 단계로 취급 |
| `pvp` | Haskell 패키지 버전 정책 |
| `strict` | 정확히 일치하는 버전만 호환으로 인정 |

지정한 값은 pom.xml과 ivy.xml에 프로퍼티로 함께 기록된다.

---

## 9. UpdateReport 조회

`update` 태스크의 결과 타입은 `sbt.UpdateReport`이며, 계층 구조로 해석 결과를 담는다.

```
UpdateReport
 └─ ConfigurationReport (compile, test, runtime ...)
     └─ ModuleReport (모듈 단위)
         └─ 성공적으로 받은 Artifact와 File, 실패한 Artifact 목록
```

`update` 자체는 아티팩트를 하나라도 못 받으면 태스크가 실패하므로 실패 목록은 항상 비어 있다. 반면 `updateClassifiers`/`updateSbtClassifiers`는 classifier 아티팩트가 없을 수 있어 실패 목록이 채워질 수 있다.

### 필터로 원하는 파일만 골라내기

암묵적 변환으로 제공되는 메서드다.

```scala
def matching(f: DependencyFilter): Seq[File]
def select(
  configuration: ConfigurationFilter = ...,
  module: ModuleFilter = ...,
  artifact: ArtifactFilter = ...
): Seq[File]
```

필터 종류:

| 필터 | 기준 | 예시 |
|---|---|---|
| `ConfigurationFilter` | configuration 이름 | `configurationFilter(name = "compile" \| "test")` |
| `ModuleFilter` | organization, 모듈명, 버전 | `moduleFilter(organization = "*sbt*", name = "main" \| "actions", revision = "1.*" - "1.0")` |
| `ArtifactFilter` | 이름, 타입, 확장자, classifier | `artifactFilter(name = "*", \`type\` = "source", extension = "jar", classifier = "sources")` |

여러 필터를 합성해 `DependencyFilter`를 만들 수 있다.

```scala
val df: DependencyFilter =
  configurationFilter(name = "compile" | "test") &&
  artifactFilter(`type` = "jar") ||
  moduleFilter(name = "dispatch-*")
```

개별 필터끼리는 단일 연산자(`&`, `|`, `-`)로, `DependencyFilter` 사이는 이중 연산자(`&&`, `||`, `--`)로 결합한다는 점이 문법상 구분 포인트다. 이렇게 얻은 `Seq[File]`은 커스텀 태스크에서 특정 파일만 골라 리소스로 넣거나 classpath에 추가하는 용도로 흔히 쓰인다(앞서 config mapping 예제의 `update.value.select(configurationFilter("js"))`가 그 예다).

---

## 10. Cached Resolution

Cached Resolution은 sbt 0.13.7부터 제공되는 기능으로, 다중 서브프로젝트 빌드에서 의존성 해석 속도를 개선한다. 활성화는 설정 하나로 끝난다.

```scala
updateOptions := updateOptions.value.withCachedResolution(true)
```

### 동작 방식

프로젝트의 의존성은 방향성 비순환 그래프(DAG)를 이룬다(예: `dispatch-core → async-http-client → netty`). Cached Resolution은 직접 의존성마다 작은 그래프(mini graph) 단위로 해석 결과를 `$HOME/.sbt/1.0/dependency/`에 캐시해두고, 새 의존성이 추가돼도 기존 캐시를 재사용하면서 변경분만 다시 해석한다. 서브프로젝트가 많고 공통 의존성이 겹치는 빌드에서 효과가 크며, 알려진 사례로 해석 시간이 260초에서 25초로 줄어든 보고가 있다.

### 제약사항

| 항목 | 내용 |
|---|---|
| 첫 실행 | 모든 미니 그래프를 새로 해석해야 하므로 오히려 느릴 수 있음 |
| Ivy 충실도 | Maven 동작을 완전히 동일하게 재현하지 못하는 경우가 있음 |
| SNAPSHOT 의존성 | 매번 무효화되어 캐시 이점이 없음 |
| classifier | 다양한 Maven classifier 조합에서 문제가 생길 수 있음 |

SNAPSHOT의 최신 버전 확인 자체를 끄면 해석 시간을 더 줄일 수 있지만, 다중 저장소 환경에서는 오히려 조회 시간이 늘어날 수 있다.

```scala
updateOptions := updateOptions.value.withLatestSnapshots(false)
```

실험적 기능이므로 대규모 멀티 프로젝트에서 반복 빌드가 잦고 해석 시간이 병목인 경우에 한해 적용을 검토하는 것이 적절하다.

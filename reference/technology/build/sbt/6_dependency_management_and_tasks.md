# sbt 의존성 관리와 Task·Command·State

## sbt 의존성 관리

> 원본: https://www.scala-sbt.org/1.x/docs/Artifacts.html, https://www.scala-sbt.org/1.x/docs/Dependency-Management-Flow.html, https://www.scala-sbt.org/1.x/docs/Library-Management.html, https://www.scala-sbt.org/1.x/docs/Proxy-Repositories.html, https://www.scala-sbt.org/1.x/docs/Publishing.html, https://www.scala-sbt.org/1.x/docs/Resolvers.html, https://www.scala-sbt.org/1.x/docs/Update-Report.html, https://www.scala-sbt.org/1.x/docs/Cached-Resolution.html

---

### 목차

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

### 1. 아티팩트란 무엇인가

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

#### Artifact 값과 artifactName

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

#### 커스텀 아티팩트 추가

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

#### 의존성 쪽에서 아티팩트 지정

의존하는 라이브러리가 classifier가 붙은 아티팩트를 가질 때는 `.artifacts(...)` 또는 축약형 `.classifier(...)`를 쓴다.

```scala
libraryDependencies += ("org" % "name" % "rev")
  .artifacts(Artifact("name", "type", "ext"))

// 아래와 동일
("org.testng" % "testng" % "5.7").classifier("jdk15")
```

크로스빌딩 시에는 `artifactName`에 전달되는 `ScalaVersion` 인자(전체 버전과 바이너리 호환 버전을 모두 담고 있음)를 이용해 버전별로 이름이 다른 아티팩트를 만들 수 있다.

---

### 2. 의존성 해석 흐름 (update 태스크)

sbt의 의존성 관리는 `update` 태스크를 중심으로 돌아간다. `libraryDependencies`와 `resolvers` 설정을 읽어 실제로 저장소에서 아티팩트를 내려받고, 그 결과를 `UpdateReport`로 만든다. `compile`, `run`, `test` 같은 태스크는 이 리포트를 참조해 클래스패스를 구성한다.

`compile` 실행 전에는 항상 `update`가 선행되지만, 매번 원격 해석을 다시 하면 느려지므로 sbt는 캐시된 해석 결과를 최대한 재사용한다. 판단 규칙은 다음과 같다.

| 상황 | 동작 |
|---|---|
| 의존성 설정이 그대로고 캐시 파일이 존재 | 재해석 생략 |
| `libraryDependencies` 등 설정이 바뀜 | 자동으로 재해석 |
| `update`를 직접 실행 | 설정 변경 여부와 무관하게 강제 재해석 |
| `clean` 실행 | 캐시가 지워져 다음 빌드에서 재해석 |
| `update / skip := true` | 해석 자체를 건너뜀(의존 태스크가 실패할 수 있음) |

#### 엔진: Ivy vs Coursier

sbt 1.3.0부터 라이브러리 관리(LM) API는 Apache Ivy 구현과 Coursier 구현을 모두 지원하며, 기본값은 Coursier다. Ivy로 되돌리려면 다음처럼 설정한다.

```scala
ThisBuild / useCoursier := false
```

#### SNAPSHOT과 해석 성능

SNAPSHOT 버전은 빌드에 가변성을 끌어들이고, 매번 네트워크로 최신 여부를 확인해야 하므로 해석 속도를 떨어뜨린다. Coursier는 기본적으로 SNAPSHOT 아티팩트에 24시간 TTL을 부여해 불필요한 네트워크 IO를 줄인다. 이 캐시를 무시하고 즉시 재확인하고 싶다면 환경 변수로 TTL을 0으로 만든다.

```bash
COURSIER_TTL=0s sbt update
```

---

### 3. 라이브러리 관리: 수동 vs 자동

sbt는 의존성을 관리하는 두 가지 방식을 함께 지원한다.

#### 수동 관리 (unmanaged)

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

#### 자동 관리 (managed)

`libraryDependencies`에 좌표를 선언하면 sbt가 저장소에서 내려받아 관리한다. 규모가 커질수록 실질적으로 표준이 되는 방식이며, 이후 절에서 다루는 문법이 모두 이 방식에 해당한다.

---

### 4. libraryDependencies 문법

#### 기본 선언

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

#### 버전 범위

```scala
libraryDependencies += "org.example" % "lib" % "latest.integration"
libraryDependencies += "org.example" % "lib" % "2.9.+"
libraryDependencies += "org.example" % "lib" % "[1.0,)"
```

#### classifier와 소스/문서 동시 다운로드

```scala
libraryDependencies += "org.testng" % "testng" % "5.7" classifier "jdk15"

libraryDependencies += "org.lwjgl.lwjgl" % "lwjgl-platform" % lwjglVersion classifier "natives-windows" classifier "natives-linux" classifier "natives-osx"

libraryDependencies += "org.apache.felix" % "org.apache.felix.framework" % "1.8.0" withSources() withJavadoc()
```

#### 전이 의존성 제어

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

#### 구성 매핑 (Configuration mapping)

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

#### 직접 URL 지정, extra 속성

```scala
libraryDependencies += "slinky" % "slinky" % "2.1" from
  "https://slinky2.googlecode.com/svn/artifacts/2.1/slinky.jar"

libraryDependencies += "org" % "name" % "rev" extra("color" -> "blue")
```

#### Ivy XML을 직접 쓰는 경우

```scala
ivyXML :=
  <dependencies>
    <dependency org="javax.mail" name="mail" rev="1.4.2">
      <exclude module="activation"/>
    </dependency>
  </dependencies>
```

---

### 5. Ivy와 Coursier, 충돌 관리

#### 체크섬

```scala
update / checksums := Nil        // 검증 비활성화
publishLocal / checksums := Nil  // 퍼블리시 시 체크섬 미포함
publish / checksums := Nil

checksums := Seq("sha1", "md5")  // 기본값
```

#### 충돌 관리자와 버전 오버라이드

```scala
conflictManager := ConflictManager.strict

// 전이 의존성 어딘가에서 끌려온 버전을 강제로 고정 (직접 의존성으로 추가하지 않음, pom 미반영)
dependencyOverrides += "log4j" % "log4j" % "1.2.16"

// Ivy 전용 강제 지정 (권장하지 않음, pom 미반영)
libraryDependencies += "log4j" % "log4j" % "1.2.14" force()
```

#### 진단용 명령어

```bash
show update             # 해석된 의존성 트리 확인
evicted                 # 버전 충돌로 밀려난(evict) 라이브러리 목록
updateClassifiers       # 소스/문서 등 classifier 아티팩트 전체 다운로드
updateSbtClassifiers    # sbt 자체 classifier 다운로드
```

#### 알려진 제약

- POM의 `relativePath`를 지정하면 오류가 난다.
- Ivy는 POM의 `<repositories>`를 무시한다. 필요하면 `ivysettings.xml`로 우회해야 한다.
- `override`/`force`는 Ivy 전용 개념이라 생성되는 pom.xml에는 반영되지 않는다.

---

### 6. resolver 설정

`resolvers`는 의존성을 내려받을 저장소 위치를 지정하는 설정이다. 기본값은 Maven Central과 로컬 Ivy 저장소다.

```scala
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
```

#### 자주 쓰는 사전 정의 resolver

| 대상 | 설정 |
|---|---|
| 로컬 Maven 저장소 | `Resolver.mavenLocal` |
| Maven 스타일 캐시 | `MavenCache("local-maven", file("path/to/repo"))` |
| Sonatype OSS | `Resolver.sonatypeOssRepos("public")` |
| Typesafe | `Resolver.typesafeRepo("releases")` |
| sbt 플러그인 저장소 | `Resolver.sbtPluginRepo("releases")` |
| JCenter | `Resolver.jcenterRepo` |

#### 커스텀 resolver

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

### 7. 사내 프록시 저장소

여러 개발자가 같은 아티팩트를 반복해서 원격 저장소에서 내려받는 비효율을 줄이려면, 조직 내부에 단일 진입점 역할을 하는 프록시 저장소(Artifactory, Nexus Repository Manager, Apache Archiva, CloudRepo 등)를 둔다. 다운로드 속도와 보안 통제 두 측면에서 이점이 있다.

설정은 두 곳에서 이뤄진다.

1. `~/.sbt/repositories` 파일
2. sbt 런처(launcher) 실행 옵션

#### repositories 파일

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

#### 자격 증명

```bash
export SBT_CREDENTIALS="$HOME/.ivy2/.credentials"
```

```
realm=My Nexus Repository Manager
host=my.artifact.repo.net
user=admin
password=admin123
```

#### 런처 옵션

```bash
# 프로젝트별 resolvers 설정을 무시하고 repositories 파일을 우선 적용 (기본값 false)
-Dsbt.override.build.repos=true

# repositories 파일 경로를 직접 지정
-Dsbt.repository.config=<path-to-your-repo-file>
```

---

### 8. 퍼블리싱

`publish`는 원격 저장소에, `publishLocal`은 로컬 Ivy 저장소(`$HOME/.ivy2/local/`)에 아티팩트와 메타데이터(ivy.xml 또는 pom.xml)를 올리는 태스크다.

특정 서브프로젝트(대개 루트 프로젝트)를 퍼블리시 대상에서 빼려면 다음처럼 설정한다.

```scala
publish / skip := true
```

#### publishTo

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

#### 로컬 퍼블리싱

```scala
ThisBuild / organization := "org.me"
ThisBuild / version      := "0.1-SNAPSHOT"
name := "My Project"
```

`publishLocal`은 `$HOME/.ivy2/local/`에, `publishM2`는 `$HOME/.m2/repository/`에 결과를 남긴다. 로컬 머신의 다른 프로젝트에서는 그냥 좌표를 의존성으로 선언하면 소비할 수 있다.

```scala
libraryDependencies += "org.me" %% "my-project" % "0.1-SNAPSHOT"
```

#### 자격 증명

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

#### 크로스 퍼블리싱

여러 Scala 버전으로 동시에 퍼블리시하려면 `+` 접두사를 붙인다.

```bash
+ publish
```

기본적으로 sbt는 사용 중인 Scala 바이너리 버전을 아티팩트 이름에 붙여 퍼블리시한다(예: `example_2.13`). 순수 Java 라이브러리나 컴파일러 플러그인처럼 이 규칙이 맞지 않는 경우 `CrossVersion` 설정을 바꿔야 한다.

#### POM 커스터마이징

```scala
pomExtra := <something></something>

pomPostProcess := { (node: Node) => /* XML 가공 */ node }

pomIncludeRepository := { (repo: MavenRepository) =>
  repo.root.startsWith("file:")
}
```

#### versionScheme (sbt 1.4.0+)

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

### 9. UpdateReport 조회

`update` 태스크의 결과 타입은 `sbt.UpdateReport`이며, 계층 구조로 해석 결과를 담는다.

```
UpdateReport
 └─ ConfigurationReport (compile, test, runtime ...)
     └─ ModuleReport (모듈 단위)
         └─ 성공적으로 받은 Artifact와 File, 실패한 Artifact 목록
```

`update` 자체는 아티팩트를 하나라도 못 받으면 태스크가 실패하므로 실패 목록은 항상 비어 있다. 반면 `updateClassifiers`/`updateSbtClassifiers`는 classifier 아티팩트가 없을 수 있어 실패 목록이 채워질 수 있다.

#### 필터로 원하는 파일만 골라내기

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

### 10. Cached Resolution

Cached Resolution은 sbt 0.13.7부터 제공되는 기능으로, 다중 서브프로젝트 빌드에서 의존성 해석 속도를 개선한다. 활성화는 설정 하나로 끝난다.

```scala
updateOptions := updateOptions.value.withCachedResolution(true)
```

#### 동작 방식

프로젝트의 의존성은 방향성 비순환 그래프(DAG)를 이룬다(예: `dispatch-core → async-http-client → netty`). Cached Resolution은 직접 의존성마다 작은 그래프(mini graph) 단위로 해석 결과를 `$HOME/.sbt/1.0/dependency/`에 캐시해두고, 새 의존성이 추가돼도 기존 캐시를 재사용하면서 변경분만 다시 해석한다. 서브프로젝트가 많고 공통 의존성이 겹치는 빌드에서 효과가 크며, 알려진 사례로 해석 시간이 260초에서 25초로 줄어든 보고가 있다.

#### 제약사항

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

---

## sbt의 Task, Command, State

> 원본: https://www.scala-sbt.org/1.x/docs/Tasks.html, https://www.scala-sbt.org/1.x/docs/Caching.html, https://www.scala-sbt.org/1.x/docs/Input-Tasks.html, https://www.scala-sbt.org/1.x/docs/Commands.html, https://www.scala-sbt.org/1.x/docs/Parsing-Input.html, https://www.scala-sbt.org/1.x/docs/Build-State.html, https://www.scala-sbt.org/1.x/docs/Task-Inputs.html

---

### 목차

1. [Setting과 Task, InputTask의 구분](#1-setting과-task-inputtask의-구분)
2. [Task 정의 문법](#2-task-정의-문법)
3. [Task 그래프는 프로젝트 로드 시점에 고정된다](#3-task-그래프는-프로젝트-로드-시점에-고정된다)
4. [Def.taskDyn과 Def.sequential](#4-deftaskdyn과-defsequential)
5. [실패 처리: failure, result, andFinally](#5-실패-처리-failure-result-andfinally)
6. [Task 캐싱: Cache/Tracked API](#6-task-캐싱-cachetracked-api)
7. [InputTask와 Def.inputTask](#7-inputtask와-definputtask)
8. [Parser 콤비네이터](#8-parser-콤비네이터)
9. [Command API](#9-command-api)
10. [Build State와 State 모나드](#10-build-state와-state-모나드)
11. [Mill과의 개념 대응](#11-mill과의-개념-대응)

---

### 1. Setting과 Task, InputTask의 구분

sbt의 빌드 정의는 세 종류의 키(key)로 구성된다. 이름은 비슷해 보이지만 평가 시점과 의존성 결정 시점이 서로 다르다.

| 구분 | 타입 | 평가 시점 | 의존성 그래프 | 예 |
|------|------|-----------|----------------|-----|
| Setting | `SettingKey[T]` | 프로젝트 로드 시 한 번 평가되고 고정 | 로드 시점에 확정 | `scalaVersion`, `name` |
| Task | `TaskKey[T]` | 명령을 내릴 때마다(요청 시) 실행 | 실행마다 재구성될 수 있음(동적 태스크 가능) | `compile`, `test` |
| InputTask | `InputKey[T]` | 명령줄 문자열을 파싱한 뒤 그 결과로 결정되는 Task를 실행 | Task와 동일하되 파서가 선행됨 | `run`, `testOnly` |

Setting은 Scala의 `val`에 가깝고, Task는 매번 다시 계산될 수 있는 `def`에 가깝다. 다만 Task 자체는 결과를 캐시하지 않는 것이 기본 동작이며(실행할 때마다 본문이 돈다), 어떤 값을 캐시하고 싶으면 6장에서 다루는 Cache/Tracked API를 별도로 써야 한다. 이 점이 Mill의 `Task { }`(자동으로 값 캐싱)와 가장 크게 갈리는 지점이다.

Key는 `build.sbt`에서 다음처럼 선언한다.

```scala
lazy val sampleTask = taskKey[Int]("설명")
lazy val demo = inputKey[Unit]("A demo input task.")
```

`taskKey[T]`의 `T`는 해당 Task가 만들어내는 값의 타입이고, 코드상의 변수 이름이 곧 sbt 콘솔/CLI에서 그 Task를 호출하는 이름이 된다.

### 2. Task 정의 문법

가장 단순한 형태는 상수식이나 부수효과를 그대로 대입하는 것이다.

```scala
intTask := 1 + 2
stringTask := System.getProperty("user.name")
```

다른 Task나 Setting의 값을 참조할 때는 `.value`를 붙인다. `.value`는 Task 본문 안에서만 쓸 수 있는 특수한 메서드로, sbt 매크로가 이 호출을 분석해 의존성 그래프의 엣지를 만든다.

```scala
sampleTask := intTask.value + 1
```

특정 구성의 값을 참조하려면 스코프를 슬래시로 붙인다.

```scala
Test / sampleTask := (Compile / intTask).value * 3
```

`Initialize[Task[T]]` 형태로 로직을 분리해 두고 나중에 키에 대입할 수도 있다.

```scala
lazy val intTaskImpl: Initialize[Task[Int]] =
  Def.task { sampleTask.value - 3 }

intTask := intTaskImpl.value
```

기존 Task 값을 참조하면서 확장(`intTask := intTask.value + 1`)할 수도 있고, 본문을 통째로 새로 써서 재정의할 수도 있다.

여러 프로젝트/구성에 걸친 값을 한 번에 모으고 싶을 때는 `ScopeFilter`를 쓴다.

```scala
val filter = ScopeFilter(
  inProjects(core, util),
  inConfigurations(Compile)
)

sources := {
  val allSources: Seq[Seq[File]] = sources.all(filter).value
  allSources.flatten
}
```

`:=` 연산자는 우선순위가 매우 낮으므로, 다른 연산자와 섞어 쓸 때는 괄호를 명시해야 한다.

```scala
// helloTask.:=( "echo Hello".! ) 로 해석되도록 괄호가 필요
helloTask := { "echo Hello" ! }
```

### 3. Task 그래프는 프로젝트 로드 시점에 고정된다

sbt의 Task 그래프는 기본적으로 프로젝트를 로드할 때 정적으로 결정된다. Task 본문 안에서 `otherTask.value`를 호출하면, sbt는 이 매크로 호출 자체를 정적으로 분석해 "이 Task는 otherTask에 의존한다"는 그래프 엣지를 만든다. 즉 `if` 조건 안에서 `.value` 호출을 서로 다르게 배치하더라도, 실행 흐름과 무관하게 두 브랜치에 등장하는 모든 `.value` 의존성이 그래프에 미리 잡힌다.

이 정적 분석 방식 덕분에 sbt는 Task를 실행하기 전에 전체 의존 그래프를 미리 계산해 병렬 실행 계획을 세울 수 있다. 반대로 "런타임에 확인해 봐야 어떤 Task를 실행할지 알 수 있는" 경우에는 이 정적 분석 방식을 그대로 쓸 수 없고, 4장에서 다루는 `Def.taskDyn`이 필요하다.

sbt 1.4.0부터는 아래처럼 최상위(top-level)에 있는 `if` 표현식은 특별히 인식해서, 분기마다 실제로 선택된 쪽의 의존성만 실행하도록 최적화한다.

```scala
bar := {
  if (number.value < 0) negAction.value
  else if (number.value == 0) zeroAction.value
  else posAction.value
}
```

이 문법은 `Def.taskDyn`과 달리 `inspect` 같은 정적 조회 명령과도 잘 호환된다는 점이 문서에서 강조된다.

### 4. Def.taskDyn과 Def.sequential

`Def.taskDyn`은 실행 결과에 따라 어떤 Task를 다음에 실행할지 런타임에 결정해야 할 때 쓴다. 정적 분석 대상이 되는 것은 `taskDyn` 블록 자체에서 조건 판단에 쓰인 값(`stringTask`)뿐이고, 분기 안에서 선택된 Task(`intTask`)는 실제로 그 분기가 선택될 때만 로드된다.

```scala
val dynamic = Def.taskDyn {
  if (stringTask.value == "dev")
    Def.task { 3 }
  else
    Def.task { intTask.value + 5 }
}

myTask := {
  val num = dynamic.value
  println(s"Number: $num")
}
```

`Def.taskDyn`으로 만들어지는 그래프는 순환 참조를 만들 수 없다는 제약이 있다.

여러 Task를 순서대로, 그리고 앞선 Task가 실패하면 즉시 중단하는 방식으로 실행하고 싶을 때는 `Def.sequential`을 쓴다.

```scala
lazy val compilecheck = taskKey[Unit]("compile then scalastyle")

Compile / compilecheck := Def.sequential(
  Compile / compile,
  (Compile / scalastyle).toTask("")
).value
```

### 5. 실패 처리: failure, result, andFinally

Task 실행 중 예외가 나면 sbt는 이를 `Incomplete`라는 실패 값으로 감싼다. 이 실패를 다루는 세 가지 방법이 있다.

| 메서드 | 동작 |
|--------|------|
| `.failure` | 원래 Task가 실패하면 `Incomplete` 값을 정상값처럼 돌려받고, 원래 Task가 성공하면 오히려 이쪽이 실패로 뒤집힌다 |
| `.result` | `Result[T]`(성공/실패를 모두 포함하는 값)로 감싸서 반환하므로 항상 성공한다 |
| `andFinally { ... }` | 성공/실패 여부와 무관하게 정리 코드를 실행한다 |

```scala
intTask.result.value match {
  case Inc(inc: Incomplete) => println("Failed: " + inc); 3
  case Value(v)             => println("Success: " + v); v
}
```

```scala
lazy val intTaskImpl = intTask andFinally {
  println("andFinally")
}
```

Task 안에서 로그를 남기려면 `streams.value`로 얻는 `TaskStreams`를 사용한다. `myTask / logLevel := Level.Debug`로 로그 레벨을 조정할 수 있고, `last myTask` 명령으로 마지막 실행의 로그를 다시 볼 수 있다.

### 6. Task 캐싱: Cache/Tracked API

sbt의 Task는 기본적으로 결과를 캐시하지 않는다. Mill처럼 "입력이 같으면 이전 결과를 그대로 쓴다"는 동작을 원한다면, `sbt.util` 패키지가 제공하는 별도의 캐싱 유틸리티를 Task 본문 안에서 직접 조합해서 써야 한다. sbt에는 이 목적의 전용 키워드(`Def.cachedTask` 같은 것)가 따로 있는 것이 아니라, 아래 API들을 태스크 작성자가 조합하는 방식이다.

| API | 역할 |
|-----|------|
| `Cache.cached(store)(f)` | 키 타입 `I`, 값 타입 `O`인 단순 캐시. 같은 입력이면 저장된 값을 재사용 |
| `TaskKey#previous` | 이전 실행에서 만든 값을 `Option[A]`로 돌려줌. 첫 실행은 `None` |
| `Tracked.lastOutput` | 마지막 출력값만 저장. 입력이 바뀌어도 무효화하지 않으므로 그 자체만으로는 부족 |
| `Tracked.inputChanged` | 입력이 이전과 달라졌는지를 `Boolean` 플래그로 알려줌 |
| `Tracked.outputChanged` | 작업 완료 후 결과 파일에 스탬프를 남겨, 다음 실행에서 불필요한 재작업을 막음 |
| `Tracked.diffInputs` / `Tracked.diffOutputs` | 단순 불린이 아니라 `ChangeReport[T]`(added/removed/modified/checked 집합)를 반환해 증분 처리를 가능하게 함 |
| `FileInfo.exists` / `lastModified` / `hash` / `full` | 파일을 어떤 속성으로 추적할지 선택 (존재 여부, 수정 시각, SHA-1 해시, 또는 둘 다) |
| `FileFunction.cached(cacheDir)(...)` | 파일 집합을 입출력으로 삼는 함수를 up-to-date 검사로 감싸는 헬퍼. 기본은 입력을 `lastModified`, 출력을 `exists`로 추적 |

입력 변경과 출력 변경을 동시에 챙기고 싶으면 `inputChanged` 콜백 안에 `lastOutput`을 중첩해서 쓰는 패턴이 일반적이다. 캐시 무효화는 결국 "입력값 해시/타임스탬프가 달라졌는가"와 "출력 파일 상태가 기대와 다른가"라는 두 조건의 조합으로 판단한다.

### 7. InputTask와 Def.inputTask

InputTask는 사용자가 명령줄에 입력한 문자열을 파싱한 뒤, 그 파싱 결과로부터 실행할 Task를 만들어내는 메커니즘이다. Task와 달리 반드시 `Parser`를 거친다는 점이 핵심 차이다.

```scala
class InputTask[T](val parser: State => Parser[Task[T]])
```

즉 InputTask는 "State로부터 Parser를 만들고, 그 Parser가 최종적으로 Task를 만들어낸다"는 두 단계 구조를 갖는다. Setting만으로 파서를 구성할 수도 있고(`Initialize[Parser[I]]`), State까지 참조해야 하는 더 복잡한 파서도 만들 수 있다(`Initialize[State => Parser[I]]`).

가장 단순한 InputTask는 `spaceDelimited`로 공백 구분 인자를 그대로 받는 것이다.

```scala
import complete.DefaultParsers._

val demo = inputKey[Unit]("A demo input task.")

demo := {
  val args: Seq[String] = spaceDelimited("<arg>").parsed
  println("Scala version: " + scalaVersion.value)
  args foreach println
}
```

`spaceDelimited`는 탭 완성 기능이 제한적이므로, 정교한 자동완성이 필요하면 8장의 Parser 콤비네이터를 직접 조합해야 한다.

| 메서드 | 역할 |
|--------|------|
| `.parsed` | Parser가 만든 값을 InputTask 본문에서 꺼냄 |
| `.evaluated` | 다른 Task를 InputTask 안에서 실행하고 그 결과값을 얻음 |
| `.toTask(" 인자 문자열")` | InputTask를 고정 입력이 미리 채워진 일반 Task처럼 다룸 |
| `.fullInput(문자열)` / `.partialInput(문자열)` | 입력을 코드로 미리 채워 넣되, 추가 입력을 막을지(`full`) 허용할지(`partial`) 선택 |

```scala
val run2 = inputKey[Unit]("Run main class twice with -- separator")

run2 := {
  val one = (Compile / run).evaluated
  val sep = separator.parsed
  val two = (Compile / run).evaluated
}
```

`run2 a b -- c d`처럼 호출하면 첫 번째 `run` 실행에는 "a b"가, 두 번째 실행에는 "c d"가 전달된다.

### 8. Parser 콤비네이터

sbt는 커맨드와 InputTask의 사용자 입력을 처리하기 위해 자체 Parser 콤비네이터 라이브러리(`sbt.internal.util.complete`, 흔히 `Parser[T]`)를 제공한다. 가장 기본적인 형태로 보면 `Parser[T]`는 문자열을 받아 성공 시 값을, 실패 시 실패 정보를 돌려주는 함수와 같다.

기본 콤비네이터는 다음과 같다.

```scala
// 리터럴
'x'          // 문자 하나
"blue"       // 문자열 완전 일치
charClass(_.isDigit, "digit")   // 조건 기반 매칭

// 결합
"fg" ~ " " ~ "color"     // 순차 결합 (튜플로 결과 모음)
"fg" ~> color            // 좌측 버리고 우측만
color <~ Space           // 우측 버리고 좌측만
"blue" | "green"         // 선택

// 반복
p.+   // 1회 이상
p.*   // 0회 이상
p.?   // 0 또는 1회

// 결과 변환
digits map { chars => chars.mkString.toInt }

// 항상 성공/항상 실패
success(3)
failure("메시지")
```

탭 완성 후보는 `.examples("0", "1", "2")`처럼 명시적으로 지정할 수 있고, `token(...)`으로 감싸면 자동완성 제안의 경계가 어디까지인지를 sbt에게 알려줄 수 있다(중첩해서 쓰면 안 된다).

앞선 Parser의 결과에 따라 다음 Parser 자체를 다르게 구성해야 할 때는 `flatMap`을 쓴다. 예를 들어 이미 선택한 항목을 제외한 나머지 항목만 파싱하도록 재귀적으로 구성하는 패턴이 대표적이다.

Command와 InputTask 양쪽 모두 이 Parser 값을 받아서 사용자 입력 문자열 전체를 해석한다는 점에서 두 메커니즘의 파싱 계층은 동일한 라이브러리를 공유한다.

### 9. Command API

Command는 Task와 겉모습은 비슷하지만("sbt 콘솔에서 실행할 수 있는 이름 붙은 작업") 받는 것과 돌려주는 것이 완전히 다르다. Task가 특정 설정값들을 조합해 결과값 하나를 만든다면, Command는 빌드 전체의 `State`를 받아 새로운 `State`를 돌려주는 함수다.

```
파서: State => Parser[T]
액션: (State, T) => State
```

Command는 이 두 함수의 조합으로 만들어진다. State 전체에 접근할 수 있으므로, 등록된 커맨드 목록을 바꾸거나 다른 프로젝트의 설정을 조회/수정하는 등 Task로는 할 수 없는 일을 할 수 있다.

| 생성 방법 | 인자 형태 | 액션 시그니처 |
|-----------|-----------|----------------|
| `Command.command("name")(action)` | 인자 없음 | `State => State` |
| `Command.single("name")(action)` | 단일 인자 | `(State, String) => State` |
| `Command.args("name", "<arg>")(action)` | 공백 구분 다중 인자 | `(State, Seq[String]) => State` |
| `Command("name")(parser)(action)` | 커스텀 Parser | `(State, T) => State` |

```scala
val hello = Command.command("hello") { state =>
  println("Hi!")
  state
}
```

정의한 Command는 `build.sbt`에서 `commands` 세팅에 등록해야 인식된다.

```scala
lazy val root = (project in file("."))
  .settings(
    commands ++= Seq(hello, helloAll, changeColor)
  )
```

Command와 Task의 차이를 정리하면 다음과 같다.

| 구분 | Task | Command |
|------|------|---------|
| 매개변수 | 관련 설정값들 | 빌드 전체의 `State` |
| 반환값 | 계산된 결과값 | 변형된 새 `State` |
| 상태 접근 범위 | 제한적(자신이 의존하는 값만) | 전체 접근 가능 |
| 용도 | 컴파일, 테스트 등 일반 빌드 작업 | 등록된 커맨드 목록 변경, 세션 조작 등 메타 작업 |

간단한 별칭이 필요할 뿐이라면 Command를 새로 만들지 않고 `addCommandAlias("c", "compile")`처럼 기존 커맨드 시퀀스에 이름을 붙이는 방법도 있다.

### 10. Build State와 State 모나드

sbt 콘솔의 실행 모델은 `State => State` 함수의 체인으로 볼 수 있다. 초기 State에서 시작해, 대기 중인 커맨드를 하나씩 꺼내 실행하면서 매번 새로운 State로 치환하고, `remainingCommands`가 빌 때까지 이 과정을 반복한다. 각 단계의 State가 다음 단계로 그대로 전달된다는 점에서 커맨드 실행 흐름은 State 모나드와 유사한 구조를 갖는다.

`State`는 불변 값이며 주요 필드/메서드는 다음과 같다.

| 필드/메서드 | 내용 |
|-------------|------|
| `definedCommands` | 현재 등록된 모든 Command 정의 |
| `remainingCommands` | 앞으로 실행할 커맨드 목록 |
| `attributes` | 임의의 데이터를 담는 `AttributeMap` |
| `history` | 실행한 커맨드 이력 |

State를 직접 바꾸는 것이 아니라 `copy()`로 새 인스턴스를 만드는 방식으로 변형한다.

```scala
// 실행할 커맨드를 뒤에 추가
state.copy(remainingCommands = state.remainingCommands :+ "cleanup")

// 다음에 바로 실행되도록 앞에 끼워 넣기
state.copy(remainingCommands = "next" +: state.remainingCommands)

// 실패 상태로 표시
if (success) state else state.fail
```

임의의 값을 State에 얹어 다른 커맨드/Task와 공유하고 싶을 때는 `AttributeKey`와 `AttributeMap`을 쓴다.

```scala
val counter = AttributeKey[Int]("counter")
state.put(counter, value)   // 값 설정 (새 State 반환)
state.get(counter)          // 값 조회
```

프로젝트에 설정된 값을 State로부터 읽어오려면 `Project.extract`로 `Extracted`를 얻는다.

```scala
val extracted: Extracted = Project.extract(state)
```

`Extracted`는 현재 빌드/프로젝트 참조, 초기화된 설정 데이터(`structure.data`), 세션 설정, 빌드 파일을 평가하는 `Eval` 인스턴스 등에 접근하는 통로가 된다. 프로젝트 데이터 자체는 `sbt.Settings[Scope]` 타입인 `structure.data`에 저장되며, 특정 키의 값은 `(key in scope).get(structure.data)` 형태로 조회한다.

Task 안에서도 `state.value`로 현재 State를 읽을 수 있고, `Project.runTask()`를 이용하면 커맨드나 다른 Task 안에서 특정 Task를 직접 실행시킬 수도 있다.

### 11. Mill과의 개념 대응

sbt와 Mill은 둘 다 "빌드는 태스크 그래프"라는 철학을 공유하지만, 세부 구현은 서로 다른 축으로 나뉜다.

| 개념 | sbt | Mill |
|------|-----|------|
| 값이 자동으로 캐시되는 단위 | 없음(Task는 기본적으로 매번 실행) | `Task { ... }` |
| 캐싱을 직접 구성해야 하는 경우 | Cache/Tracked API를 태스크 안에서 조합 | 대부분 자동, `Task.Persistent`로 폴더 유지 |
| CLI에서 호출할 때마다 실행 | Task 전반 + Command | `Task.Command` |
| 커맨드라인 인자를 받는 단위 | `InputTask`(Parser 기반) | `Task.Command`(MainArgs 기반) |
| 빌드 전체 상태를 다루는 단위 | `Command`(`State => State`) | 별도 대응 없음(모듈/Task 그래프로 대체) |
| 그래프 결정 시점 | 프로젝트 로드 시 정적 분석(`.value`), 예외적으로 `Def.taskDyn` | 매 실행 시 `foo()` 호출로 그래프 구성 |

가장 크게 다른 지점은 두 가지다. 첫째, sbt의 Task는 기본적으로 비캐시이고 캐싱은 라이브러리 차원에서 선택적으로 붙이는 반면, Mill의 `Task { }`는 캐싱이 기본값이다. 둘째, sbt에는 빌드 전체 State를 조작하는 Command라는 별도의 계층이 있어서 Parser 콤비네이터로 임의의 커맨드라인 문법을 정의할 수 있지만, Mill에는 이에 대응하는 개념이 없고 대신 Task/Module 그래프 자체로 대부분의 요구를 처리한다.

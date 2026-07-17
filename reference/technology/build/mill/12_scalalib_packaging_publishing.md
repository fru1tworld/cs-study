# Mill scalalib: 패키징과 퍼블리싱

> 원본: https://mill-build.org/mill/scalalib/packaging.html, https://mill-build.org/mill/scalalib/publishing.html

---

## 목차

1. [assembly로 실행 가능 JAR 만들기](#1-assembly로-실행-가능-jar-만들기)
2. [assemblyRules로 병합 규칙 제어](#2-assemblyrules로-병합-규칙-제어)
3. [GraalVM Native Image](#3-graalvm-native-image)
4. [RepackageModule (Spring Boot 방식)](#4-repackagemodule-spring-boot-방식)
5. [jlink / jpackage](#5-jlink--jpackage)
6. [PublishModule 기본 설정](#6-publishmodule-기본-설정)
7. [Git 태그 기반 버전 관리](#7-git-태그-기반-버전-관리)
8. [publishLocal](#8-publishlocal)
9. [Maven Central 퍼블리싱 준비](#9-maven-central-퍼블리싱-준비)
10. [GPG 서명 설정](#10-gpg-서명-설정)
11. [SonatypeCentralPublishModule 실행](#11-sonatypecentralpublishmodule-실행)
12. [GitHub Actions 연동](#12-github-actions-연동)
13. [SNAPSHOT 버전](#13-snapshot-버전)
14. [일반 Maven 저장소 / 기타 저장소](#14-일반-maven-저장소--기타-저장소)

---

## 1. assembly로 실행 가능 JAR 만들기

Mill의 모든 `JavaModule`(따라서 `ScalaModule`도 포함)은 `assembly` 태스크를 기본 제공한다. transitive classpath 전체를 하나의 JAR로 평탄화(flatten)하고, 앞에 Mac/Linux용 셸 스크립트와 Windows용 배치 스크립트를 붙여 `java -jar` 없이도 바로 실행 가능한 파일로 만든다.

```scala
// build.mill
package build
import mill.*, scalalib.*

object foo extends ScalaModule {
  def moduleDeps = Seq(bar)
  def scalaVersion = "3.8.2"
  def mvnDeps = Seq(mvn"com.lihaoyi::os-lib:0.11.4")
}

object bar extends ScalaModule {
  def scalaVersion = "3.8.2"
}
```

```bash
./mill foo.assembly
java -jar ./out/foo/assembly.dest/out.jar
```

셸 스크립트가 앞에 붙어 있으므로 실행 권한만 있으면 다음처럼도 실행할 수 있다.

```bash
./out/foo/assembly.dest/out.jar
JAVA_OPTS=-Dtest.property=1337 ./out/foo/assembly.dest/out.jar
```

주요 관련 태스크/필드:

| 이름 | 설명 |
|---|---|
| `assembly` | 실행 가능한 fat JAR을 만드는 태스크. 결과는 `assembly.dest/out.jar` |
| `manifest: T[JarManifest]` | JAR 매니페스트 내용을 커스터마이즈 |
| `prependShellScript: T[String]` | JAR 앞에 붙는 셸 스크립트 내용을 재정의 |
| `upstreamAssemblyClasspath` | 의존 모듈들의 classpath를 모아 assembly에 포함 |

---

## 2. assemblyRules로 병합 규칙 제어

여러 JAR을 하나로 합치다 보면 같은 경로의 파일(`reference.conf`, 시그니처 파일 등)이 충돌한다. Mill은 기본적으로 서명 파일(`.SF`, `.DSA`, `.RSA` 등)을 자동 제외하고, `reference.conf` 같은 설정 파일은 자동으로 이어붙인다(concatenate). 이 동작을 `assemblyRules`로 세밀하게 조정할 수 있다.

```scala
package build
import mill.*, scalalib.*
import mill.scalalib.Assembly.*

object foo extends ScalaModule {
  def moduleDeps = Seq(bar)
  def scalaVersion = "3.8.2"
  def mvnDeps = Seq(mvn"com.lihaoyi::os-lib:0.11.4")
  def assemblyRules = Seq(
    Rule.Append("application.conf"),
    Rule.AppendPattern(".*\\.conf"),
    Rule.ExcludePattern(".*\\.temp"),
    Rule.Relocate("shapeless.**", "shade.shapeless.@1")
  )
}
```

| 규칙 | 동작 |
|---|---|
| `Rule.Append(path)` | 정확히 일치하는 경로의 파일들을 모두 이어붙임(concat) |
| `Rule.AppendPattern(regex)` | 정규식에 매치되는 경로들을 이어붙임 |
| `Rule.ExcludePattern(regex)` | 정규식에 매치되는 파일을 assembly에서 제외 |
| `Rule.Relocate(from, to)` | 패키지 경로를 다른 이름으로 재배치(shading). `shapeless.**` 같은 와일드카드와 `@1` 캡처 그룹 문법 사용 |

`assemblyRules`의 타입은 `T[Seq[Rule]]`이며, 여러 모듈이 합쳐지는 멀티모듈 프로젝트에서 라이브러리 충돌(예: shapeless 버전 충돌)을 shading으로 회피할 때 특히 유용하다.

---

## 3. GraalVM Native Image

`NativeImageModule` 트레이트를 섞어 쓰면 GraalVM `native-image` 컴파일러로 AOT(ahead-of-time) 네이티브 실행 파일을 생성할 수 있다.

```yaml
# foo/package.mill.yaml
extends: [ScalaModule, NativeImageModule]
scalaVersion: 3.8.2
jvmVersion: graalvm-community:17.0.7
nativeImageOptions: ["--no-fallback"]
```

```bash
./mill show foo.nativeImage
./out/foo/nativeImage.dest/native-executable
```

의존성이 있는 경우도 동일하게 동작한다.

```yaml
extends: [ScalaModule, NativeImageModule]
scalaVersion: 3.8.2
mvnDeps:
- com.lihaoyi::scalatags:0.13.1
- com.lihaoyi::mainargs:0.7.8

nativeImageOptions: ["--no-fallback", "-Os"]
jvmVersion: graalvm-community:23.0.1
```

주의할 점:

- 모든 JVM이 native-image 생성을 지원하지 않으므로, `jvmVersion`을 GraalVM 배포판(`graalvm-community:...`)으로 명시적으로 지정해야 한다. 이 값은 `JvmWorkerModule`을 통해 실제 사용할 JDK를 결정한다.
- `nativeImageOptions`로 GraalVM `native-image` CLI 옵션(`--no-fallback`, `-Os` 등)을 그대로 전달할 수 있다.
- `native-image` 도구 자체를 직접 다루고 싶다면 `./mill show foo.nativeImageTool`로 도구 경로를 조회해 실험할 수 있다.

| 필드/태스크 | 설명 |
|---|---|
| `nativeImage` | 네이티브 실행 파일 생성 태스크 |
| `nativeImageTool` | 사용 중인 `native-image` 실행 파일 경로 조회 |
| `nativeImageOptions: [String]` | GraalVM native-image CLI 옵션 목록 |
| `jvmVersion: String` | 사용할 GraalVM/JDK 버전 문자열 |

---

## 4. RepackageModule (Spring Boot 방식)

`assembly`는 의존성 JAR 내부 파일을 모두 풀어 하나로 평탄화하지만, `RepackageModule`(Spring Boot Tools 기반)은 의존성 JAR를 압축 해제하지 않고 원본 그대로 내부에 담는(embed) 방식을 쓴다. `BOOT-INF/lib/` 아래 각 JAR가 그대로 보존되므로, 나중에 개별 라이브러리의 체크섬이나 라이선스 정보를 그대로 조회할 수 있다는 장점이 있다. 결과물은 여전히 자체 실행 가능한 JAR다(내장된 launcher를 통해).

```scala
package build
import mill.*, api.*, scalalib.*, publish.*
import mill.javalib.repackage.RepackageModule
import mill.javalib.spring.boot.SpringBootToolsModule

trait MyModule extends ScalaModule, PublishModule {
  def scalaVersion = "3.8.2"
  def publishVersion = "0.0.1"
  def pomSettings = PomSettings(
    description = "Hello",
    organization = "com.lihaoyi",
    url = "https://github.com/lihaoyi/example",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("lihaoyi", "example"),
    developers = Seq(Developer("lihaoyi", "Li Haoyi", "https://github.com/lihaoyi"))
  )
  def mvnDeps = Seq(mvn"org.thymeleaf:thymeleaf:3.1.1.RELEASE")
  object test extends ScalaTests, TestModule.Junit4
}

object SpringBootTools2 extends SpringBootToolsModule {
  override def springBootToolsVersion = "2.7.18"
  lazy val millDiscover = Discover[this.type]
}

object foo extends MyModule, RepackageModule {
  def moduleDeps = Seq(bar, qux)
  override def springBootToolsModule = ModuleRef(SpringBootTools2)
}

object bar extends MyModule {
  def moduleDeps = Seq(qux)
}

object qux extends MyModule
```

```bash
./mill foo.run
./mill __.test
./mill show foo.repackagedJar
unzip -l ./out/foo/repackagedJar.dest/out.jar "BOOT-INF/lib*"
```

| 이름 | 설명 |
|---|---|
| `RepackageModule` | 의존성을 압축 해제하지 않고 담는 패키징 트레이트 |
| `repackagedJar` | 결과 JAR을 생성하는 태스크 |
| `springBootToolsModule: ModuleRef` | 사용할 `SpringBootToolsModule` 인스턴스 지정 |
| `springBootToolsVersion: String` | Spring Boot Tools 라이브러리 버전 |

---

## 5. jlink / jpackage

Mill은 JDK 표준 도구인 `jlink`(커스텀 런타임 이미지 생성)와 `jpackage`(OS 네이티브 설치 패키지 생성)도 지원한다. 이 문서 자체에는 별도 예제 코드가 없고, Java 쪽 구현(javalib) 문서를 참고하도록 안내되어 있다. 필요 시 `mill.javalib` 패키지의 jlink/jpackage 관련 모듈 문서를 확인한다.

---

## 6. PublishModule 기본 설정

퍼블리싱하려는 모듈은 `ScalaModule`과 `PublishModule`을 함께 상속(mix-in)한다. 최소 설정은 버전, 아티팩트 이름, POM 메타데이터(`pomSettings`)로 구성된다.

```scala
package build
import mill.*, scalalib.*, publish.*

object foo extends ScalaModule, PublishModule {
  def scalaVersion = "3.8.2"
  def publishVersion = "0.0.1"

  def pomSettings = PomSettings(
    description = "Hello",
    organization = "com.lihaoyi",
    url = "https://github.com/lihaoyi/example",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("lihaoyi", "example"),
    developers = Seq(Developer("lihaoyi", "Li Haoyi", "https://github.com/lihaoyi"))
  )
}
```

`build.mill.yaml` 형식(YAML DSL)으로도 동일하게 작성 가능하다.

```yaml
extends: [ScalaModule, PublishModule]
scalaVersion: 3.8.2
publishVersion: 0.0.1
artifactName: example
pomSettings:
  description: Example
  organization: com.lihaoyi
  url: https://github.com/com.lihaoyi/example
  licenses: [MIT]
  versionControl: https://github.com/com.lihaoyi/example
  developers: [{name: Li Haoyi, email: example@example.com}]
```

| 필드 | 타입/설명 |
|---|---|
| `publishVersion` | 퍼블리시할 아티팩트 버전 문자열 |
| `artifactName` | 모듈 이름 대신 사용할 아티팩트 이름(생략 시 모듈 경로 기반 기본값) |
| `pomSettings` | `PomSettings(description, organization, url, licenses, versionControl, developers)` |

---

## 7. Git 태그 기반 버전 관리

`mill.util.VcsVersionModule`을 함께 mix-in하면 커밋되지 않은 `publishVersion`을 Git 태그로부터 자동 유도할 수 있다.

```yaml
extends: [ScalaModule, PublishModule, mill.util.VcsVersionModule]
scalaVersion: 3.8.2
artifactName: example
pomSettings:
  description: Example
  organization: com.lihaoyi
  url: https://github.com/com.lihaoyi/example
  licenses: [MIT]
  versionControl: https://github.com/com.lihaoyi/example
  developers: [{name: Li Haoyi, email: example@example.com}]
```

```bash
git init .
git add -A
git commit -m "initial commit"
git tag v0.1.0
./mill publishLocal
```

`v0.1.0` 태그가 있는 커밋 시점에서는 버전이 `0.1.0`으로, 이후 커밋이 추가되면 dirty/dev 형태의 버전 문자열이 자동 계산된다.

---

## 8. publishLocal

로컬 Ivy 저장소(`~/.ivy2`)에 아티팩트를 퍼블리시할 때는 `publishLocal`을 사용한다. 멀티모듈일 때 특정 모듈만, 또는 `__`로 전체 모듈을 대상으로 지정할 수 있다.

```bash
./mill publishLocal
./mill foo.publishLocal
./mill __.publishLocal
```

| 옵션 | 설명 |
|---|---|
| `--doc=false` | scaladoc JAR 생성 생략 |
| `--sources=false` | 소스 JAR 생성 생략 |
| `--transitive=true` | 의존 모듈들도 함께 로컬 퍼블리시 |

---

## 9. Maven Central 퍼블리싱 준비

Maven Central(Sonatype Central)에 퍼블리시하려면 사전에 Sonatype Central에 네임스페이스(그룹 ID)를 등록하고 소유권을 검증해야 한다(도메인 DNS 검증 또는 GitHub 계정 검증 등). 이 절차는 Mill과 무관한 Sonatype 측 계정/네임스페이스 설정이며, 완료 후에야 아래 GPG 서명 및 `SonatypeCentralPublishModule` 사용 단계로 넘어간다.

---

## 10. GPG 서명 설정

Maven Central은 업로드되는 아티팩트에 GPG 서명을 요구한다. Mill은 GPG 키 생성을 돕는 내장 명령을 제공한다.

```bash
./mill mill.javalib.SonatypeCentralPublishModule/initGpgKeys
```

이 명령이 대화형으로 키 생성과 Base64 인코딩까지 처리해준다. 수동으로 설정하려면 다음 절차를 따른다.

```bash
# 키 생성 후 키서버에 업로드/조회
gpg --keyserver keyserver.ubuntu.com --send-keys $LONG_ID
gpg --keyserver keyserver.ubuntu.com --recv-keys $LONG_ID
```

플랫폼별 비밀키 Base64 인코딩:

```bash
# MacOS or FreeBSD
gpg --export-secret-key -a $LONG_ID | base64

# Ubuntu (GNU base64)
gpg --export-secret-key -a $LONG_ID | base64 -w0

# Arch
gpg --export-secret-key -a $LONG_ID | base64 | sed -z 's;\n;;g'

# Windows
gpg --export-secret-key -a %LONG_ID% | openssl base64
```

인코딩된 값과 자격 증명을 환경 변수로 노출한다.

```bash
export MILL_SONATYPE_USERNAME=...
export MILL_SONATYPE_PASSWORD=...
export MILL_PGP_SECRET_BASE64=...
export MILL_PGP_PASSPHRASE=...
```

| 환경 변수 | 용도 |
|---|---|
| `MILL_SONATYPE_USERNAME` | Sonatype Central API 사용자명 |
| `MILL_SONATYPE_PASSWORD` | Sonatype Central API 비밀번호 |
| `MILL_PGP_SECRET_BASE64` | Base64 인코딩된 GPG 비밀키 |
| `MILL_PGP_PASSPHRASE` | GPG 키 패스프레이즈 |

---

## 11. SonatypeCentralPublishModule 실행

환경 변수가 준비되면 `SonatypeCentralPublishModule`을 직접 호출해 퍼블리시한다.

```bash
mill mill.javalib.SonatypeCentralPublishModule/
```

퍼블리시 대상 모듈을 좁히려면 `--publishArtifacts`로 특정 태스크를 지정한다.

```bash
mill mill.javalib.SonatypeCentralPublishModule/ --publishArtifacts foo.publishArtifacts
```

전체 인자를 명시한 예:

```bash
mill mill.javalib.SonatypeCentralPublishModule/publishAll \
--username myusername \
--password mypassword \
--gpgArgs --passphrase=$MILL_PGP_PASSPHRASE,--no-tty,--pinentry-mode,loopback,--batch,--yes,--armor,--detach-sign \
--publishArtifacts __.publishArtifacts \
--readTimeout 36000 \
--awaitTimeout 36000 \
--connectTimeout 36000 \
--shouldRelease false \
--bundleName com.lihaoyi-requests:1.0.0
```

| 인자 | 설명 | 기본값 |
|---|---|---|
| `username` | Sonatype Central API 사용자명 (env: `MILL_SONATYPE_USERNAME`) | - |
| `password` | Sonatype Central API 비밀번호 (env: `MILL_SONATYPE_PASSWORD`) | - |
| `gpgArgs` | GPG 서명에 넘길 인자 목록 | `--passphrase=$MILL_PGP_PASSPHRASE, --no-tty, --pinentry-mode, loopback, --batch, --yes, --armor, --detach-sign` |
| `publishArtifacts` | 퍼블리시 대상을 생성하는 태스크 | 필수 |
| `readTimeout` | 응답 대기 타임아웃(ms) | `60000` |
| `awaitTimeout` | 백오프 포함 전체 재시도 타임아웃(ms) | `120000` |
| `connectTimeout` | 초기 연결 타임아웃(ms) | `5000` |
| `shouldRelease` | 업로드 후 번들 자동 릴리스 여부 | `true` |
| `bundleName` | 여러 패키지를 하나의 번들 이름으로 묶기 | 미지정 |

기본적으로 업로드 완료 시 자동으로 release되므로(`shouldRelease=true`), 검토 후 수동 릴리스하려면 `--shouldRelease false`로 지정한다.

---

## 12. GitHub Actions 연동

태그 push나 수동 트리거 시 CI에서 퍼블리시하는 워크플로 예시.

```yaml
# .github/workflows/publish-artifacts.yml
name: Publish Artifacts
on:
  push:
    tags:
      - '**'
  workflow_dispatch:
jobs:
  publish-artifacts:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - run: ./mill mill.javalib.SonatypeCentralPublishModule/
        env:
          MILL_PGP_PASSPHRASE: ${{ secrets.MILL_PGP_PASSPHRASE }}
          MILL_PGP_SECRET_BASE64: ${{ secrets.MILL_PGP_SECRET_BASE64 }}
          MILL_SONATYPE_PASSWORD: ${{ secrets.MILL_SONATYPE_PASSWORD }}
          MILL_SONATYPE_USERNAME: ${{ secrets.MILL_SONATYPE_USERNAME }}
```

시크릿 4개(`MILL_PGP_PASSPHRASE`, `MILL_PGP_SECRET_BASE64`, `MILL_SONATYPE_PASSWORD`, `MILL_SONATYPE_USERNAME`)를 GitHub repo secrets에 등록해두면, 앞서 로컬에서 GPG 설정한 것과 동일한 방식으로 CI에서 자동 퍼블리시된다.

---

## 13. SNAPSHOT 버전

Maven Central 네임스페이스에서 snapshot 지원을 활성화한 뒤, 버전 문자열 끝에 `-SNAPSHOT`을 붙이면 스냅샷으로 퍼블리시된다.

```yaml
extends: JavaModule
publishVersion: 0.0.1-SNAPSHOT
```

스냅샷 아티팩트를 소비하려면 별도의 snapshot 저장소를 `repositories`에 추가해야 한다.

```yaml
extends: JavaModule
repositories: ["https://central.sonatype.com/repository/maven-snapshots"]
mvnDeps:
- com.example:mymodule:1.0.0-SNAPSHOT
```

일반 릴리스 저장소에는 스냅샷 저장소가 포함되어 있지 않으므로, 스냅샷 버전 의존성을 쓰는 프로젝트는 반드시 위처럼 저장소를 명시해야 한다.

---

## 14. 일반 Maven 저장소 / 기타 저장소

Sonatype Central이 아닌 사내 Maven 저장소(Nexus, Artifactory 등)에 퍼블리시할 때는 `MavenPublishModule`을 사용한다.

```bash
export MILL_MAVEN_USERNAME=...
export MILL_MAVEN_PASSWORD=...
```

```bash
mill mill.javalib.MavenPublishModule/ \
--username myusername \
--password mypassword \
--releaseUri https://example.company.com/release \
--snapshotUri https://example.company.com/snapshot
```

`releaseUri`/`snapshotUri`로 릴리스용과 스냅샷용 업로드 대상 URL을 각각 지정한다. 자격 증명은 `--username`/`--password` 인자 또는 `MILL_MAVEN_USERNAME`/`MILL_MAVEN_PASSWORD` 환경 변수로 전달한다.

기존 `PublishModule`의 Sonatype 스타일 API로도 non-staging 방식 업로드를 흉내낼 수 있다.

```bash
mill mill.scalalib.PublishModule/ \
--publishArtifacts foo.publishArtifacts \
--sonatypeCreds lihaoyi:$SONATYPE_PASSWORD \
--sonatypeUri http://example.company.com/release \
--stagingRelease false
```

`--stagingRelease false`를 지정하면 Sonatype의 staging repository 흐름 없이 지정한 URI로 바로 업로드한다. 이 밖에 Codeartifact, Artifactory 같은 저장소는 별도 contrib 플러그인(`mill-contrib-codeartifact` 등)으로 지원된다.

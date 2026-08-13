# Mill scalalib: 패키징·퍼블리싱과 빌드 예제

## Mill scalalib: 패키징과 퍼블리싱

> 원본: https://mill-build.org/mill/scalalib/packaging.html, https://mill-build.org/mill/scalalib/publishing.html

---

### 목차

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

### 1. assembly로 실행 가능 JAR 만들기

Mill의 모든 `JavaModule`(따라서 `ScalaModule`도 포함)은 `assembly` 태스크를 기본 제공함. transitive classpath 전체를 하나의 JAR로 평탄화(flatten)하고, 앞에 Mac/Linux용 셸 스크립트와 Windows용 배치 스크립트를 붙여 `java -jar` 없이도 바로 실행 가능한 파일로 만듦.

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

셸 스크립트가 앞에 붙어 있으므로 실행 권한만 있으면 다음처럼도 실행 가능함.

```bash
./out/foo/assembly.dest/out.jar
JAVA_OPTS=-Dtest.property=1337 ./out/foo/assembly.dest/out.jar
```

주요 관련 태스크/필드:

- `assembly`: 실행 가능한 fat JAR을 만드는 태스크 → 결과는 `assembly.dest/out.jar`
- `manifest: T[JarManifest]`: JAR 매니페스트 내용 커스터마이즈용
- `prependShellScript: T[String]`: JAR 앞에 붙는 셸 스크립트 내용 재정의용
- `upstreamAssemblyClasspath`: 의존 모듈들의 classpath를 모아 assembly에 포함

---

### 2. assemblyRules로 병합 규칙 제어

여러 JAR을 하나로 합치다 보면 같은 경로의 파일(`reference.conf`, 시그니처 파일 등)이 충돌함. Mill은 기본적으로 서명 파일(`.SF`, `.DSA`, `.RSA` 등)을 자동 제외하고, `reference.conf` 같은 설정 파일은 자동으로 이어붙임(concatenate). 이 동작은 `assemblyRules`로 세밀하게 조정 가능함.

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

- `Rule.Append(path)`: 정확히 일치하는 경로의 파일들을 모두 이어붙임(concat)
- `Rule.AppendPattern(regex)`: 정규식에 매치되는 경로들을 이어붙임
- `Rule.ExcludePattern(regex)`: 정규식에 매치되는 파일을 assembly에서 제외
- `Rule.Relocate(from, to)`: 패키지 경로를 다른 이름으로 재배치(shading) → `shapeless.**` 같은 와일드카드와 `@1` 캡처 그룹 문법 사용

`assemblyRules`의 타입은 `T[Seq[Rule]]`이며, 여러 모듈이 합쳐지는 멀티모듈 프로젝트에서 라이브러리 충돌(예: shapeless 버전 충돌)을 shading으로 회피할 때 특히 유용함.

---

### 3. GraalVM Native Image

`NativeImageModule` 트레이트를 섞어 쓰면 GraalVM `native-image` 컴파일러로 AOT(ahead-of-time) 네이티브 실행 파일을 생성 가능함.

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

의존성이 있는 경우도 동일하게 동작함.

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

- 모든 JVM이 native-image 생성을 지원하지 않으므로, `jvmVersion`을 GraalVM 배포판(`graalvm-community:...`)으로 명시적으로 지정 필요 → 이 값이 `JvmWorkerModule`을 통해 실제 사용할 JDK를 결정함.
- `nativeImageOptions`로 GraalVM `native-image` CLI 옵션(`--no-fallback`, `-Os` 등)을 그대로 전달 가능함.
- `native-image` 도구 자체를 직접 다루고 싶다면 `./mill show foo.nativeImageTool`로 도구 경로를 조회해 실험 가능함.

- `nativeImage`: 네이티브 실행 파일 생성 태스크
- `nativeImageTool`: 사용 중인 `native-image` 실행 파일 경로 조회
- `nativeImageOptions: [String]`: GraalVM native-image CLI 옵션 목록
- `jvmVersion: String`: 사용할 GraalVM/JDK 버전 문자열

---

### 4. RepackageModule (Spring Boot 방식)

`assembly`는 의존성 JAR 내부 파일을 모두 풀어 하나로 평탄화하지만, `RepackageModule`(Spring Boot Tools 기반)은 의존성 JAR를 압축 해제하지 않고 원본 그대로 내부에 담는(embed) 방식을 씀. `BOOT-INF/lib/` 아래 각 JAR가 그대로 보존되므로, 나중에 개별 라이브러리의 체크섬이나 라이선스 정보를 그대로 조회 가능하다는 장점 있음. 결과물은 여전히 자체 실행 가능한 JAR임(내장된 launcher를 통해).

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

- `RepackageModule`: 의존성을 압축 해제하지 않고 담는 패키징 트레이트
- `repackagedJar`: 결과 JAR을 생성하는 태스크
- `springBootToolsModule: ModuleRef`: 사용할 `SpringBootToolsModule` 인스턴스 지정
- `springBootToolsVersion: String`: Spring Boot Tools 라이브러리 버전

---

### 5. jlink / jpackage

Mill은 JDK 표준 도구인 `jlink`(커스텀 런타임 이미지 생성)와 `jpackage`(OS 네이티브 설치 패키지 생성)도 지원함. 이 문서 자체에는 별도 예제 코드가 없고, Java 쪽 구현(javalib) 문서를 참고하도록 안내됨. 필요 시 `mill.javalib` 패키지의 jlink/jpackage 관련 모듈 문서 확인 필요.

---

### 6. PublishModule 기본 설정

퍼블리싱하려는 모듈은 `ScalaModule`과 `PublishModule`을 함께 상속(mix-in)함. 최소 설정은 버전, 아티팩트 이름, POM 메타데이터(`pomSettings`)로 구성됨.

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

`build.mill.yaml` 형식(YAML DSL)으로도 동일하게 작성 가능함.

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

- `publishVersion`: 퍼블리시할 아티팩트 버전 문자열
- `artifactName`: 모듈 이름 대신 사용할 아티팩트 이름 → 생략 시 모듈 경로 기반 기본값
- `pomSettings`: `PomSettings(description, organization, url, licenses, versionControl, developers)`

---

### 7. Git 태그 기반 버전 관리

`mill.util.VcsVersionModule`을 함께 mix-in하면 커밋되지 않은 `publishVersion`을 Git 태그로부터 자동 유도 가능함.

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

`v0.1.0` 태그가 있는 커밋 시점에서는 버전이 `0.1.0`으로, 이후 커밋이 추가되면 dirty/dev 형태의 버전 문자열이 자동 계산됨.

---

### 8. publishLocal

로컬 Ivy 저장소(`~/.ivy2`)에 아티팩트를 퍼블리시할 때는 `publishLocal`을 사용함. 멀티모듈일 때 특정 모듈만, 또는 `__`로 전체 모듈을 대상으로 지정 가능함.

```bash
./mill publishLocal
./mill foo.publishLocal
./mill __.publishLocal
```

- `--doc=false`: scaladoc JAR 생성 생략
- `--sources=false`: 소스 JAR 생성 생략
- `--transitive=true`: 의존 모듈들도 함께 로컬 퍼블리시

---

### 9. Maven Central 퍼블리싱 준비

Maven Central(Sonatype Central)에 퍼블리시하려면 사전에 Sonatype Central에 네임스페이스(그룹 ID)를 등록하고 소유권 검증 필요(도메인 DNS 검증 또는 GitHub 계정 검증 등). 이 절차는 Mill과 무관한 Sonatype 측 계정/네임스페이스 설정이며, 완료 후에야 아래 GPG 서명 및 `SonatypeCentralPublishModule` 사용 단계로 넘어감.

---

### 10. GPG 서명 설정

Maven Central은 업로드되는 아티팩트에 GPG 서명을 요구함. Mill은 GPG 키 생성을 돕는 내장 명령을 제공함.

```bash
./mill mill.javalib.SonatypeCentralPublishModule/initGpgKeys
```

이 명령이 대화형으로 키 생성과 Base64 인코딩까지 처리함. 수동으로 설정하려면 다음 절차를 따름.

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

인코딩된 값과 자격 증명을 환경 변수로 노출함.

```bash
export MILL_SONATYPE_USERNAME=...
export MILL_SONATYPE_PASSWORD=...
export MILL_PGP_SECRET_BASE64=...
export MILL_PGP_PASSPHRASE=...
```

- `MILL_SONATYPE_USERNAME`: Sonatype Central API 사용자명
- `MILL_SONATYPE_PASSWORD`: Sonatype Central API 비밀번호
- `MILL_PGP_SECRET_BASE64`: Base64 인코딩된 GPG 비밀키
- `MILL_PGP_PASSPHRASE`: GPG 키 패스프레이즈

---

### 11. SonatypeCentralPublishModule 실행

환경 변수가 준비되면 `SonatypeCentralPublishModule`을 직접 호출해 퍼블리시함.

```bash
mill mill.javalib.SonatypeCentralPublishModule/
```

퍼블리시 대상 모듈을 좁히려면 `--publishArtifacts`로 특정 태스크를 지정함.

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

- `username`: Sonatype Central API 사용자명(env: `MILL_SONATYPE_USERNAME`) → 기본값 없음
- `password`: Sonatype Central API 비밀번호(env: `MILL_SONATYPE_PASSWORD`) → 기본값 없음
- `gpgArgs`: GPG 서명에 넘길 인자 목록 → 기본값 `--passphrase=$MILL_PGP_PASSPHRASE, --no-tty, --pinentry-mode, loopback, --batch, --yes, --armor, --detach-sign`
- `publishArtifacts`: 퍼블리시 대상을 생성하는 태스크 → 필수
- `readTimeout`: 응답 대기 타임아웃(ms) → 기본값 `60000`
- `awaitTimeout`: 백오프 포함 전체 재시도 타임아웃(ms) → 기본값 `120000`
- `connectTimeout`: 초기 연결 타임아웃(ms) → 기본값 `5000`
- `shouldRelease`: 업로드 후 번들 자동 릴리스 여부 → 기본값 `true`
- `bundleName`: 여러 패키지를 하나의 번들 이름으로 묶기 → 기본값 미지정

기본적으로 업로드 완료 시 자동으로 release됨(`shouldRelease=true`) → 검토 후 수동 릴리스하려면 `--shouldRelease false`로 지정.

---

### 12. GitHub Actions 연동

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

시크릿 4개(`MILL_PGP_PASSPHRASE`, `MILL_PGP_SECRET_BASE64`, `MILL_SONATYPE_PASSWORD`, `MILL_SONATYPE_USERNAME`)를 GitHub repo secrets에 등록해두면, 앞서 로컬에서 GPG 설정한 것과 동일한 방식으로 CI에서 자동 퍼블리시됨.

---

### 13. SNAPSHOT 버전

Maven Central 네임스페이스에서 snapshot 지원 활성화 후, 버전 문자열 끝에 `-SNAPSHOT`을 붙이면 스냅샷으로 퍼블리시됨.

```yaml
extends: JavaModule
publishVersion: 0.0.1-SNAPSHOT
```

스냅샷 아티팩트를 소비하려면 별도의 snapshot 저장소를 `repositories`에 추가 필요.

```yaml
extends: JavaModule
repositories: ["https://central.sonatype.com/repository/maven-snapshots"]
mvnDeps:
- com.example:mymodule:1.0.0-SNAPSHOT
```

일반 릴리스 저장소에는 스냅샷 저장소가 포함되어 있지 않음 → 스냅샷 버전 의존성을 쓰는 프로젝트는 반드시 위처럼 저장소 명시 필요.

---

### 14. 일반 Maven 저장소 / 기타 저장소

Sonatype Central이 아닌 사내 Maven 저장소(Nexus, Artifactory 등)에 퍼블리시할 때는 `MavenPublishModule`을 사용함.

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

`releaseUri`/`snapshotUri`로 릴리스용과 스냅샷용 업로드 대상 URL을 각각 지정함. 자격 증명은 `--username`/`--password` 인자 또는 `MILL_MAVEN_USERNAME`/`MILL_MAVEN_PASSWORD` 환경 변수로 전달함.

기존 `PublishModule`의 Sonatype 스타일 API로도 non-staging 방식 업로드를 흉내 가능함.

```bash
mill mill.scalalib.PublishModule/ \
--publishArtifacts foo.publishArtifacts \
--sonatypeCreds lihaoyi:$SONATYPE_PASSWORD \
--sonatypeUri http://example.company.com/release \
--stagingRelease false
```

`--stagingRelease false` 지정 시 Sonatype의 staging repository 흐름 없이 지정한 URI로 바로 업로드함. 이 밖에 Codeartifact, Artifactory 같은 저장소는 별도 contrib 플러그인(`mill-contrib-codeartifact` 등)으로 지원됨.

---

## Mill Scala 빌드 예제 (실전 프로젝트 / 웹 / Native)

> 원본: https://mill-build.org/mill/scalalib/build-examples.html, https://mill-build.org/mill/scalalib/web-examples.html, https://mill-build.org/mill/scalalib/native-examples.html

---

### 목차

1. [실전 프로젝트 빌드: Acyclic - 크로스 버전 단일 모듈](#1-실전-프로젝트-빌드-acyclic---크로스-버전-단일-모듈)
2. [실전 프로젝트 빌드: Fansi - 버전×플랫폼 크로스 매트릭스](#2-실전-프로젝트-빌드-fansi---버전플랫폼-크로스-매트릭스)
3. [알려진 실제 Mill 빌드 프로젝트](#3-알려진-실제-mill-빌드-프로젝트)
4. [웹 서버 빌드: 기본 구성과 테스트](#4-웹-서버-빌드-기본-구성과-테스트)
5. [웹 서버 빌드: 정적 리소스 캐시 버스팅](#5-웹-서버-빌드-정적-리소스-캐시-버스팅)
6. [웹 서버 빌드: Http4s 프레임워크 연동](#6-웹-서버-빌드-http4s-프레임워크-연동)
7. [Scala.js 단독 모듈](#7-scalajs-단독-모듈)
8. [Scala.js + 백엔드 서버 통합](#8-scalajs--백엔드-서버-통합)
9. [Scala.js/JVM 코드 공유 모듈](#9-scalajsjvm-코드-공유-모듈)
10. [크로스 버전 × 크로스 플랫폼 모듈 발행](#10-크로스-버전--크로스-플랫폼-모듈-발행)
11. [Scala.js WebAssembly 빌드](#11-scalajs-webassembly-빌드)
12. [Scala Native: 기본 구성](#12-scala-native-기본-구성)
13. [Scala Native: C/C++ 코드 연동(interop)](#13-scala-native-cc-코드-연동interop)
14. [Scala Native: 멀티모듈 구성](#14-scala-native-멀티모듈-구성)
15. [Scala Native: 공통 설정 오버라이드](#15-scala-native-공통-설정-오버라이드)

---

### 1. 실전 프로젝트 빌드: Acyclic - 크로스 버전 단일 모듈

Mill 배포판에는 실제 오픈소스 프로젝트를 Mill로 빌드하는 예제가 함께 들어 있음. `acyclic`은 Scala 컴파일러 플러그인이며, Scala 2.12.8부터 2.13.16까지 모든 point 버전에 대해 크로스 빌드됨.

핵심 패턴은 다음과 같음.

- `Cross[AcyclicModule](crosses)`로 버전 목록을 순회하며 모듈을 생성
- `CrossScalaModule`을 상속해 `crossScalaVersion`을 자동으로 주입받음
- `PublishModule`을 함께 상속해 `pomSettings`/`publishVersion` 정의
- 컴파일 시에만 필요한 `scala-compiler`를 `compileMvnDeps`로 선언
- 테스트는 `TestModule.Utest`를 사용하고, 테스트 리소스가 `os.pwd` 상대 경로를 가정하므로 `BuildCtx.withFilesystemCheckerDisabled` 블록에서 파일을 미리 복사해둠

```scala
package build

import mill.*, scalalib.*, publish.*
import mill.api.BuildCtx

// acyclic 테스트 스위트는 os.pwd 기준 특정 경로에 파일이 있다고 가정한다.
// 테스트 코드를 바꾸지 않기 위해, 테스트 실행 전 리소스를 os.pwd로 복사한다.
BuildCtx.withFilesystemCheckerDisabled {
  os.copy.over(
    BuildCtx.watch(mill.api.BuildCtx.workspaceRoot / "acyclic"),
    os.pwd / "acyclic",
    createFolders = true
  )
}

object Deps {
  def acyclic = mvn"com.lihaoyi:::acyclic:0.3.6"
  def scalaCompiler(scalaVersion: String) = mvn"org.scala-lang:scala-compiler:$scalaVersion"
  val utest = mvn"com.lihaoyi::utest:0.8.5"
}

val crosses =
  Range.inclusive(8, 20).map("2.12." + _) ++
    Range.inclusive(0, 16).map("2.13." + _)

object acyclic extends Cross[AcyclicModule](crosses)
trait AcyclicModule extends CrossScalaModule, PublishModule {
  def crossFullScalaVersion = true
  def artifactName = "acyclic"
  def publishVersion = "1.3.3.7"

  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "com.lihaoyi",
    url = "https://github.com/com-lihaoyi/acyclic",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github(owner = "com-lihaoyi", repo = "acyclic"),
    developers = Seq(Developer("lihaoyi", "Li Haoyi", "https://github.com/lihaoyi"))
  )

  def compileMvnDeps = Seq(Deps.scalaCompiler(crossScalaVersion))

  object test extends ScalaTests, TestModule.Utest {
    def sources = Task.Sources("src", "resources")
    def mvnDeps = Seq(Deps.utest, Deps.scalaCompiler(crossScalaVersion))
  }
}
```

`crossFullScalaVersion = true`는 크로스 버전 문자열을 `2_13_16`처럼 point 버전까지 그대로 태스크 경로에 반영하도록 만듦. 실행은 버전별로 아래처럼 접두사를 붙임.

```
> ./mill acyclic.2_12_20.compile
compiling 7 Scala sources...
...

> ./mill acyclic.2_13_16.test.testLocal # acyclic tests need testLocal due to classloader assumptions
...
```

---

### 2. 실전 프로젝트 빌드: Fansi - 버전×플랫폼 크로스 매트릭스

`fansi`는 모든 Scala 마이너 버전(2.12, 2.13, 3.x)과 모든 플랫폼(JVM, JS, Native)에 대해 동시에 크로스 빌드되는 작은 라이브러리 예제임. 라이브러리 본체와 테스트 스위트가 버전×플랫폼 조합(matrix) 전체에 걸쳐 중복 정의되며, 원하는 조합만 골라 compile/test/publish 가능함.

공통 설정은 트레이트로 뽑아내고, 각 플랫폼별 모듈은 해당 플랫폼 전용 트레이트(`ScalaModule`/`ScalaJSModule`/`ScalaNativeModule`)를 추가로 mixin함.

```scala
package build

import mill._, scalalib._, scalajslib._, scalanativelib._, publish.*
import mill.util.VcsVersion
import mill.javalib.api.JvmWorkerUtil.isScala3

val dottyCommunityBuildVersion = sys.props.get("dottyVersion")

val scalaVersions = Seq("2.12.20", "2.13.16", "3.3.6") ++ dottyCommunityBuildVersion

trait FansiModule extends PublishModule with CrossScalaModule with PlatformScalaModule {
  def artifactName = "fansi"

  def publishVersion = "1.3.3.7"

  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "com.lihaoyi",
    url = "https://github.com/com-lihaoyi/Fansi",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github(owner = "com-lihaoyi", repo = "fansi"),
    developers = Seq(
      Developer("lihaoyi", "Li Haoyi", "https://github.com/lihaoyi")
    )
  )

  def mvnDeps = Seq(mvn"com.lihaoyi::sourcecode::0.4.0")
}

trait FansiTestModule extends ScalaModule with TestModule.Utest {
  def mvnDeps = Seq(mvn"com.lihaoyi::utest::0.8.3")
}

object fansi extends Module {
  object jvm extends Cross[JvmFansiModule](scalaVersions)
  trait JvmFansiModule extends FansiModule with ScalaModule {
    object test extends ScalaTests with FansiTestModule
  }

  object js extends Cross[JsFansiModule](scalaVersions)
  trait JsFansiModule extends FansiModule with ScalaJSModule {
    def scalaJSVersion = "1.20.2"
    object test extends ScalaJSTests with FansiTestModule
  }

  object native extends Cross[NativeFansiModule](scalaVersions)
  trait NativeFansiModule extends FansiModule with ScalaNativeModule {
    def scalaNativeVersion = "0.5.8"
    object test extends ScalaNativeTests with FansiTestModule
  }
}
```

`PlatformScalaModule`이 `FansiModule`에 섞여 들어가면서 `jvm`/`js`/`native` 각 서브모듈이 소스 폴더 우선순위(`src`, `src-jvm`, `src-2.13.16` 등)를 플랫폼/버전에 맞춰 자동으로 잡아줌.

버전(3) × 플랫폼(3) × 라이브러리/테스트(2) 조합으로 총 18개의 `compile` 태스크가 생성됨.

```
> ./mill resolve __.compile
fansi.js.2_12_20.compile
fansi.js.2_12_20.test.compile
...
fansi.native.3_3_6.test.compile

> ./mill fansi.jvm.2_12_20.compile
compiling 1 Scala source...

> ./mill fansi.js.2_13_16.test
Starting process: node
-------------------------------- Running Tests --------------------------------
...

> ./mill fansi.native.3_3_6.publishLocal
Publishing com.lihaoyi:fansi_native0.5_3:1.3.3.7 to ivy repo...
```

---

### 3. 알려진 실제 Mill 빌드 프로젝트

문서에서는 Mill로 빌드되는 대표적인 실전 오픈소스 프로젝트 세 가지를 언급함.

- [Ammonite](https://github.com/com-lihaoyi/Ammonite): 사용하기 편한(ergonomic) Scala REPL
- [Scala-CLI](https://github.com/VirtusLab/scala-cli): 터미널에서 `scala` 명령으로 실행되는 기본 CLI 도구 → 컴파일·테스트·실행·패키징을 다양한 방식으로 지원
- [Coursier](https://github.com/coursier/coursier): 여러 빌드 도구에서 서드파티 의존성 해석·다운로드에 쓰이는 빠른 JVM 의존성 리졸버

---

### 4. 웹 서버 빌드: 기본 구성과 테스트

가장 단순한 웹 서버 예제는 [Cask](https://github.com/com-lihaoyi/cask) 프레임워크를 사용하며, `build.mill.yaml`(YAML 빌드 헤더) 형식으로도 표현 가능할 만큼 설정이 짧음.

```yaml
extends: ScalaModule
scalaVersion: 3.8.2
mvnDeps: [com.lihaoyi::cask:0.9.1]
```

```scala
package example
object WebServer extends cask.MainRoutes {
  override def port = sys.env.getOrElse("PORT", "8080").toInt

  @cask.post("/reverse-string")
  def doThing(request: cask.Request) = {
    request.text().reverse
  }

  initialize()
}
```

```
> ./mill runBackground

> curl -d 'helloworld' localhost:${PORT:-8080}/reverse-string
dlrowolleh

> ./mill clean runBackground # shut down webserver
```

테스트는 `test/package.mill.yaml`로 별도 정의하며, 실제 서버를 띄운 뒤 HTTP 요청을 보내 동작을 검증하는 방식임.

```yaml
extends: [build.ScalaTests, TestModule.Utest]
mvnDeps:
- com.lihaoyi::requests:0.9.0
- com.lihaoyi::utest:0.9.1
```

TodoMVC 예제는 여기에 uPickle(JSON)과 Scalatags(HTML 생성)를 추가해 실전 웹 앱 구조를 보여줌.

```yaml
extends: ScalaModule
scalaVersion: 3.8.2
mvnDeps:
- com.lihaoyi::cask:0.9.1
- com.lihaoyi::upickle:4.3.0
- com.lihaoyi::scalatags:0.13.1
```

---

### 5. 웹 서버 빌드: 정적 리소스 캐시 버스팅

정적 파일에 콘텐츠 해시를 붙여 파일명을 바꾸고, 원본 경로 → 해시 경로 매핑을 JSON으로 저장해두는 "cache busting" 패턴임. CDN 등에서 정적 파일을 긴 만료 시간으로 캐싱해도, 배포 시 파일명이 바뀌므로 클라이언트가 즉시 새 파일을 받아가게 됨.

핵심은 `resources` 태스크를 오버라이드해서 `super.resources()`의 결과를 순회하며 해시된 사본을 만들고, 매핑 정보를 `hashed-resource-mapping.json`으로 함께 내보내는 것임.

```scala
package build

import mill.*, scalalib.*
import java.util.Arrays

object `package` extends ScalaModule {
  def scalaVersion = "3.8.2"
  def mvnDeps = Seq(
    mvn"com.lihaoyi::cask:0.9.1",
    mvn"com.lihaoyi::upickle:4.3.0",
    mvn"com.lihaoyi::scalatags:0.13.1",
    mvn"com.lihaoyi::os-lib:0.11.4"
  )

  def resources = Task {
    val hashMapping = for {
      resourceRoot <- super.resources()
      path <- os.walk(resourceRoot.path)
      if os.isFile(path)
    } yield hashFile(path, resourceRoot.path, Task.dest)

    os.write(
      Task.dest / "hashed-resource-mapping.json",
      upickle.write(hashMapping.toMap, indent = 4)
    )

    Seq(PathRef(Task.dest))
  }

  object test extends ScalaTests, TestModule.Utest {
    def mvnDeps = Seq(
      mvn"com.lihaoyi::utest::0.8.9",
      mvn"com.lihaoyi::requests::0.6.9"
    )
  }

  def hashFile(path: os.Path, src: os.Path, dest: os.Path) = {
    val hash = Integer.toHexString(Arrays.hashCode(os.read.bytes(path)))
    val relPath = path.relativeTo(src)
    val ext = if (relPath.ext == "") "" else s".${relPath.ext}"
    val hashedPath = relPath / os.up / s"${relPath.baseName}-$hash$ext"
    os.copy(path, dest / hashedPath, createFolders = true)
    (relPath.toString(), hashedPath.toString())
  }
}
```

실행 시 실제로 해시가 붙은 파일 경로로 정적 리소스가 서빙되는 것을 확인 가능함.

```
> curl http://localhost:${PORT:-8080}/static/main-6da98e99.js # mac/linux
initListeners()
```

이 패턴은 `resources` 태스크를 오버라이드해 빌드 산출물을 가공하는 일반적인 Mill 확장 방식의 좋은 예시이기도 함.

---

### 6. 웹 서버 빌드: Http4s 프레임워크 연동

Cask 대신 [Http4s](https://http4s.org/)(Ember 서버 + DSL) 기반으로도 동일한 TodoMVC 앱 구성 가능함. Circe(JSON), Scalatags-http4s 연동 라이브러리가 함께 쓰임.

```yaml
extends: ScalaModule
scalaVersion: 3.8.2
mvnDeps:
- org.http4s::http4s-ember-server::0.23.30
- org.http4s::http4s-dsl::0.23.30
- org.http4s::http4s-scalatags::0.25.2
- io.circe::circe-generic::0.14.10
```

테스트에서는 Cats Effect 기반 테스트 유틸리티(`cats-effect-testing-utest`)와 http4s 클라이언트를 사용함.

```yaml
extends: [build.ScalaTests, TestModule.Utest]
mvnDeps:
- com.lihaoyi::utest::0.8.9
- org.typelevel::cats-effect-testing-utest::1.6.0
- org.http4s::http4s-client::0.23.30
```

Cask 예제와 마찬가지로 `test`, `runBackground` 태스크로 로컬 구동/검증이 가능하며, 웹 프레임워크가 바뀌어도 Mill 빌드 구조(모듈 정의 + mvnDeps + test 서브모듈) 자체는 동일하게 유지된다는 점이 핵심임.

---

### 7. Scala.js 단독 모듈

`ScalaJSModule`은 `ScalaModule`과 거의 동일하지만 `scalaJSVersion` 반드시 지정 필요. Scala.js 전용 의존성은 organization과 artifact 사이에 콜론을 두 개(`::`) 붙여 표기함.

```yaml
# foo/package.mill.yaml
extends: mill.scalajslib.ScalaJSModule
scalaVersion: 3.8.2
scalaJSVersion: 1.20.2
mvnDeps:
- com.lihaoyi::scalatags::0.13.1
```

```yaml
# foo/test/package.mill.yaml
extends: [build.foo.ScalaJSTests, TestModule.Utest]
utestVersion: 0.8.9
```

`compile`/`run`/`test`는 그대로 사용 가능하지만, `run`과 `test`는 JVM이 아니라 `node`를 호출해 실행됨. 여기에 더해 모듈 전체를 하나의 JS 파일로 링크하는 `fastLinkJS`(개발용, 빠른 빌드) / `fullLinkJS`(배포용, 최적화)가 추가로 제공됨.

```
> ./mill foo.run
<h1>Hello World</h1>
stringifiedJsObject: ["hello","world","!"]

> ./mill foo.test
+ foo.FooTests.hello...

> ./mill show foo.fullLinkJS # mac/linux
{
  ..."jsFileName": "main.js",
  "dest": ".../out/foo/fullLinkJS.dest"
}

> node out/foo/fullLinkJS.dest/main.js # mac/linux
<h1>Hello World</h1>
stringifiedJsObject: ["hello","world","!"]
```

Scala.js 모듈을 로컬에서 실행하려면 `node` 런타임 설치 필요.

---

### 8. Scala.js + 백엔드 서버 통합

JVM 백엔드(Cask)와 Scala.js 프론트엔드를 한 빌드 안에서 묶는 패턴. 핵심은 백엔드의 `resources` 태스크에서 `client` 서브모듈의 `fastLinkJS()` 결과물(`main.js`, `main.js.map`)을 리소스 폴더로 복사해 넣는 것임.

```scala
package build

import mill.*, scalalib.*, scalajslib.*

object `package` extends ScalaModule {

  def scalaVersion = "3.8.2"
  def mvnDeps = Seq(
    mvn"com.lihaoyi::cask:0.9.1",
    mvn"com.lihaoyi::upickle:4.3.0",
    mvn"com.lihaoyi::scalatags:0.13.1"
  )

  def resources = Task {
    os.makeDir(Task.dest / "webapp")
    val jsPath = client.fastLinkJS().dest.path
    // Move main.js[.map]into the proper filesystem position
    // in the resource folder for the web server code to pick up
    os.copy(jsPath / "main.js", Task.dest / "webapp/main.js")
    os.copy(jsPath / "main.js.map", Task.dest / "webapp/main.js.map")
    super.resources() ++ Seq(PathRef(Task.dest))
  }

  object test extends ScalaTests, TestModule.Utest {
    def utestVersion = "0.8.9"
    def mvnDeps = Seq(
      mvn"com.lihaoyi::requests::0.6.9"
    )
  }

  object client extends ScalaJSModule {
    def scalaVersion = "3.8.2"
    def scalaJSVersion = "1.20.2"
    def mvnDeps = Seq(mvn"org.scala-js::scalajs-dom::2.2.0")
  }
}
```

`client` 서브모듈이 부모 모듈 안에 중첩되어 있으므로, 부모의 `resources` 태스크가 `client.fastLinkJS()`를 태스크 의존성으로 참조하는 것만으로 "JS 빌드 → 리소스 복사 → 서버 패키징"까지 하나의 Mill 그래프로 자동 연결됨. 즉 별도의 빌드 파이프라인 스크립트 없이 `def resources` 오버라이드만으로 프론트엔드-백엔드 빌드를 묶을 수 있음.

---

### 9. Scala.js/JVM 코드 공유 모듈

클라이언트-서버 간 로직(JSON 직렬화, HTML 렌더링 등)을 재사용하려면 `shared` 모듈을 JVM/JS 양쪽으로 빌드함. 공통 설정은 상위 트레이트로 추출함.

```scala
package build

import mill.*, scalalib.*, scalajslib.*

trait AppScalaModule extends ScalaModule {
  def scalaVersion = "3.3.6"
}

trait AppScalaJSModule extends AppScalaModule, ScalaJSModule {
  def scalaJSVersion = "1.20.2"
}

object `package` extends AppScalaModule {
  def moduleDeps = Seq(shared.jvm)
  def mvnDeps = Seq(mvn"com.lihaoyi::cask:0.9.1")

  def resources = Task {
    os.makeDir(Task.dest / "webapp")
    val jsPath = client.fastLinkJS().dest.path
    os.copy(jsPath / "main.js", Task.dest / "webapp/main.js")
    os.copy(jsPath / "main.js.map", Task.dest / "webapp/main.js.map")
    super.resources() ++ Seq(PathRef(Task.dest))
  }

  object test extends ScalaTests, TestModule.Utest {
    def utestVersion = "0.8.9"
    def mvnDeps = Seq(
      mvn"com.lihaoyi::requests::0.6.9"
    )
  }

  object shared extends Module {
    trait SharedModule extends AppScalaModule, PlatformScalaModule {
      def mvnDeps = Seq(
        mvn"com.lihaoyi::scalatags::0.13.1",
        mvn"com.lihaoyi::upickle::4.3.0"
      )
    }

    object jvm extends SharedModule
    object js extends SharedModule, AppScalaJSModule
  }

  object client extends AppScalaJSModule {
    def moduleDeps = Seq(shared.js)
    def mvnDeps = Seq(mvn"org.scala-js::scalajs-dom::2.2.0")
  }
}
```

구조를 정리하면 다음과 같음.

- `package`(루트): JVM → Cask 백엔드 서버, `shared.jvm`에 의존
- `shared.jvm`: JVM → uPickle/Scalatags 기반 공유 로직
- `shared.js`: JS → 동일 소스를 Scala.js로 컴파일
- `client`: JS → `shared.js`에 의존하는 프론트엔드

서버는 초기 페이지 로드에는 HTML을, 이후 갱신에는 JSON을 내려주고 클라이언트가 이를 HTML로 렌더링하는 구조이며, 직렬화/렌더링 로직 자체는 `shared` 모듈 하나로 양쪽에서 공유됨.

---

### 10. 크로스 버전 × 크로스 플랫폼 모듈 발행

Scala 버전(2.13.16 / 3.3.6)과 플랫폼(JVM / JS)을 동시에 크로스 빌드하며 여러 개의 상호 의존 모듈(`bar`, `qux`)을 발행하는 예제. 두 가지 구성 방식 있음.

#### 10.1 상위 Cross로 감싸는 방식

`foo`라는 최상위 `Cross[FooModule]`이 버전을 순회하고, 그 안에 `bar`/`qux` 각각의 `jvm`/`js` 서브모듈을 둠.

```scala
package build

import mill.*, scalalib.*, scalajslib.*, publish.*

object foo extends Cross[FooModule]("2.13.16", "3.3.6")
trait FooModule extends Cross.Module[String] {
  trait Shared extends CrossScalaModule, CrossValue, PlatformScalaModule, PublishModule {
    def publishVersion = "0.0.1"

    def pomSettings = PomSettings(
      description = "Hello",
      organization = "com.lihaoyi",
      url = "https://github.com/lihaoyi/example",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("lihaoyi", "example"),
      developers = Seq(Developer("lihaoyi", "Li Haoyi", "https://github.com/lihaoyi"))
    )

    def mvnDeps = Seq(mvn"com.lihaoyi::scalatags::0.13.1")
  }

  trait FooTestModule extends TestModule.Utest {
    def utestVersion = "0.8.9"
  }

  trait SharedJS extends Shared, ScalaJSModule {
    def scalaJSVersion = "1.20.2"
  }

  object bar extends Module {
    object jvm extends Shared {
      object test extends ScalaTests, FooTestModule
    }
    object js extends SharedJS {
      object test extends ScalaJSTests, FooTestModule
    }
  }

  object qux extends Module {
    object jvm extends Shared {
      def moduleDeps = Seq(bar.jvm)
      def mvnDeps = super.mvnDeps() ++ Seq(mvn"com.lihaoyi::upickle::4.3.0")

      object test extends ScalaTests, FooTestModule
    }

    object js extends SharedJS {
      def moduleDeps = Seq(bar.js)

      object test extends ScalaJSTests, FooTestModule
    }
  }
}
```

이 경우 태스크 경로는 `foo.<version>.<module>.<platform>` 형태가 되고, 발행되는 아티팩트는 버전×플랫폼×모듈 조합 전체(2×2×2=8개)임.

```
> ./mill foo.2_13_16.qux.jvm.run
Bar.value: <p>world Specific code for Scala 2.x</p>
Parsing JSON with ujson.read
Qux.main: Set(<p>i</p>, <p>cow</p>, <p>me</p>)

> ./mill __.publishLocal
Publishing com.lihaoyi:foo-bar_sjs1_2.13:0.0.1 to ivy repo...
Publishing com.lihaoyi:foo-bar_2.13:0.0.1 to ivy repo...
...
```

#### 10.2 모듈별 개별 Cross 방식

`bar`, `qux` 각각이 자기만의 `Cross`를 갖는 대안 구조. 모듈마다 지원 Scala 버전 목록을 다르게 줄 수 있다는 점이 장점임.

```scala
package build

import mill.*, scalalib.*, scalajslib.*, publish.*

trait Shared extends CrossScalaModule, PlatformScalaModule, PublishModule {
  def publishVersion = "0.0.1"
  def pomSettings = PomSettings(
    description = "Hello",
    organization = "com.lihaoyi",
    url = "https://github.com/lihaoyi/example",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("lihaoyi", "example"),
    developers = Seq(Developer("lihaoyi", "Li Haoyi", "https://github.com/lihaoyi"))
  )
  def mvnDeps = Seq(mvn"com.lihaoyi::scalatags::0.13.1")
}

trait SharedTestModule extends TestModule.Utest {
  def utestVersion = "0.8.9"
}

trait SharedJS extends Shared, ScalaJSModule {
  def scalaJSVersion = "1.20.2"
}

val scalaVersions = Seq("2.13.16", "3.3.6")

object bar extends Module {
  object jvm extends Cross[JvmModule](scalaVersions)
  trait JvmModule extends Shared {
    object test extends ScalaTests, SharedTestModule
  }

  object js extends Cross[JsModule](scalaVersions)
  trait JsModule extends SharedJS {
    object test extends ScalaJSTests, SharedTestModule
  }
}

object qux extends Module {
  object jvm extends Cross[JvmModule](scalaVersions)
  trait JvmModule extends Shared {
    def moduleDeps = Seq(bar.jvm())
    def mvnDeps = super.mvnDeps() ++ Seq(mvn"com.lihaoyi::upickle::4.3.0")

    object test extends ScalaTests, SharedTestModule
  }

  object js extends Cross[JsModule](scalaVersions)
  trait JsModule extends SharedJS {
    def moduleDeps = Seq(bar.js())

    object test extends ScalaJSTests, SharedTestModule
  }
}
```

이때 태스크 경로는 `<module>.<platform>.<version>` 순서가 되어 10.1과 순서가 바뀜(`qux.js.3_3_6.run` 형태).

두 방식의 차이를 정리하면 다음과 같음.

- 상위 Cross로 감싸기
  - 태스크 경로: `foo.<version>.<module>.<platform>`
  - 버전 목록: 전체 모듈이 공유
  - 적합한 상황: 모든 모듈이 동일 버전 매트릭스 지원
- 모듈별 개별 Cross
  - 태스크 경로: `<module>.<platform>.<version>`
  - 버전 목록: 모듈마다 다르게 지정 가능
  - 적합한 상황: 모듈별 지원 버전이 다를 때

---

### 11. Scala.js WebAssembly 빌드

Scala.js 1.17+ 부터 지원하는 WASM 백엔드를 사용하는 예제. `scalaJSExperimentalUseWebAssembly`, `moduleKind`(ESModule), `moduleSplitStyle`(FewestModules) 함께 설정 필요.

```scala
package build

import mill.*, scalalib.*, scalajslib.*
import mill.scalajslib.api.*

object wasm extends ScalaJSModule {
  override def scalaVersion = "3.8.2"

  override def scalaJSVersion = "1.20.2"

  override def moduleKind = ModuleKind.ESModule

  override def moduleSplitStyle = ModuleSplitStyle.FewestModules

  override def scalaJSExperimentalUseWebAssembly = true
}
```

빌드 결과물은 단일 WASM 모듈 + 로더 + `main.js`(엔트리포인트) 형태이며, 실행에는 Node.js 22 이상과 `--experimental-wasm-exnref` 플래그 필요.

```
> node --experimental-wasm-exnref out/wasm/fastLinkJS.dest/main.js # mac/linux
hello  wasm!
```

---

### 12. Scala Native: 기본 구성

`ScalaNativeModule`은 `scalaVersion`에 더해 `scalaNativeVersion` 지정 필요. `nativeLink` 태스크로 네이티브 바이너리 빌드/링크 가능함.

```scala
package build

import mill.*, scalalib.*, scalanativelib.*

object `package` extends ScalaNativeModule {
  def scalaVersion = "3.3.6"
  def scalaNativeVersion = "0.5.8"

  // You can have arbitrary numbers of third-party dependencies
  def mvnDeps = Seq(
    mvn"com.lihaoyi::mainargs::0.7.8"
  )

  object test extends ScalaNativeTests, TestModule.Utest {
    def mvnDeps = Seq(mvn"com.lihaoyi::utest::0.8.9")
    def testFramework = "utest.runner.Framework"
  }
}
```

```
> ./mill run --text hello
<h1>hello</h1>

> ./mill show nativeLink  # Build and link native binary
".../out/nativeLink.dest/out"

> ./out/nativeLink.dest/out --text hello  # Run the executable
<h1>hello</h1>
```

---

### 13. Scala Native: C/C++ 코드 연동(interop)

Scala Native는 C/C++ 소스와 직접 링크 가능함. 빌드 설정 자체는 기본 구성과 거의 같지만, C/C++ 소스 파일은 반드시 `resources/scala-native` 디렉터리 아래 위치해야 정상적으로 링크·컴파일됨(Scala Native 공식 문서의 native code 연동 규칙).

```scala
package build

import mill.*, scalalib.*, scalanativelib.*

object `package` extends ScalaNativeModule {
  def scalaVersion = "3.3.6"
  def scalaNativeVersion = "0.5.8"

  object test extends ScalaNativeTests {
    def mvnDeps = Seq(mvn"com.lihaoyi::utest::0.8.9")
    def testFramework = "utest.runner.Framework"
  }

}
```

예상되는 프로젝트 레이아웃:

```
build.mill
src/
	foo/
	    HelloWorld.scala

resources/
    scala-native/
        HelloWorld.c

test/
    src/
        foo/
            HelloWorldTests.scala
```

```
> ./mill run
Running HelloWorld function
Done...
Reversed: !dlroW ,olleH

> ./mill test
Tests: 1, Passed: 1, Failed: 0
```

---

### 14. Scala Native: 멀티모듈 구성

두 개의 Scala Native 모듈(`foo`, `bar`)을 `moduleDeps`로 연결하는 예제. 공통 설정(scalaVersion, scalaNativeVersion, test 서브모듈)은 `trait MyModule`로 추출해 재사용함. 최상위에서 `mill.Module`을 별도로 상속하지 않았으므로, 태스크 실행 시 모듈명을 접두사로 붙여야 함(`foo.run`, `bar.run`).

```scala
package build

import mill.*, scalalib.*, scalanativelib.*

trait MyModule extends ScalaNativeModule {
  def scalaVersion = "3.3.6"
  def scalaNativeVersion = "0.5.8"

  object test extends ScalaNativeTests {
    def mvnDeps = Seq(mvn"com.lihaoyi::utest::0.8.9")
    def testFramework = "utest.runner.Framework"
  }
}

object foo extends MyModule {
  def moduleDeps = Seq(bar)

  def mvnDeps = Seq(mvn"com.lihaoyi::mainargs::0.7.8")
}

object bar extends MyModule
```

각 모듈은 자신만의 C/C++ 소스를 `resources/scala-native` 아래에 독립적으로 둘 수 있음.

```
build.mill
bar/
    resources/
        scala-native/
            bar.h
            HelloWorldBar.c
    src/
        Bar.scala
    test/
        src/
            BarTests.scala
foo/
    resources/
        scala-native/
            bar.h
            HelloWorldFoo.c
    src/
        Foo.scala
```

```
> ./mill bar.run hello
Running HelloWorld function
Done...
Bar value: Argument length is 5

> ./mill foo.run --bar-text hello --foo-text world
Foo.value: The vowel density of 'world' is 20
Bar.value: The string length of 'hello' is 5
```

---

### 15. Scala Native: 공통 설정 오버라이드

`ScalaNativeModule`에서 자주 오버라이드하는 설정 값들을 모아둔 예제.

- `releaseMode`: 링크 최적화 수준 → 예제 값 `ReleaseMode.ReleaseFast`
- `nativeIncrementalCompilation`: 증분 컴파일 활성화 여부 → 예제 값 `true`
- `nativeLinkingOptions`: 링커에 전달할 추가 옵션(`-L` 등) → 예제 값 `Seq("-L" + moduleDir.toString + "/target")`
- `nativeWorkdir`: Native 컴파일 작업 디렉터리 → 예제 값 `Task.dest / "newDir"`

```scala
package build

import mill.*, scalalib.*, scalanativelib.*, scalanativelib.api.*

object `package` extends ScalaNativeModule {
  def scalaVersion = "3.3.6"
  def scalaNativeVersion = "0.5.8"

  // You can have arbitrary numbers of third-party dependencies
  // Scala Native uses double colon `::` between organization and the dependency names
  def mvnDeps = Seq(
    mvn"com.lihaoyi::fansi::0.5.0"
  )

  // Set the releaseMode to ReleaseFast.
  def releaseMode: T[ReleaseMode] = ReleaseMode.ReleaseFast

  // Set incremental compilation to true
  def nativeIncrementalCompilation: T[Boolean] = true

  // Set nativeLinkingOptions path to a directory named `target`.
  def nativeLinkingOptions = Seq("-L" + moduleDir.toString + "/target")

  // Set nativeWorkdir directory to `newDir`
  def nativeWorkdir = Task.dest / "newDir"
}
```

```
> ./mill show releaseMode
"ReleaseFast"

> ./mill show nativeIncrementalCompilation
true
```

---

## Mill scalalib: Spark 예제와 Scala 단일 파일 스크립트

> 원본: https://mill-build.org/mill/scalalib/spark.html, https://mill-build.org/mill/scalalib/script.html

---

### 목차

1. [Hello Spark (Scala)](#1-hello-spark-scala)
2. [Hello PySpark (Python)](#2-hello-pyspark-python)
3. [spark-submit을 사용하는 실전에 가까운 Spark 프로젝트](#3-spark-submit을-사용하는-실전에-가까운-spark-프로젝트)
4. [Scala 단일 파일 스크립트 개요](#4-scala-단일-파일-스크립트-개요)
5. [스크립트 활용 사례](#5-스크립트-활용-사례)
6. [어셈블리와 네이티브 바이너리 패키징](#6-어셈블리와-네이티브-바이너리-패키징)
7. [스크립트 REPL 열기](#7-스크립트-repl-열기)
8. [스크립트 moduleDeps](#8-스크립트-moduledeps)
9. [상대/절대 경로 스크립트 moduleDeps](#9-상대절대-경로-스크립트-moduledeps)
10. [프로젝트 모듈에 대한 moduleDeps](#10-프로젝트-모듈에-대한-moduledeps)
11. [커스텀 스크립트 모듈 클래스](#11-커스텀-스크립트-모듈-클래스)
12. [스크립트 리소스](#12-스크립트-리소스)
13. [커스텀 JVM 버전](#13-커스텀-jvm-버전)
14. [번들 라이브러리와 Raw 모드](#14-번들-라이브러리와-raw-모드)
15. [스크립트 IDE 연동(BSP)](#15-스크립트-ide-연동bsp)

---

### 1. Hello Spark (Scala)

`ScalaModule`에 `spark-core`, `spark-sql` 의존성을 추가하면 그대로 Spark 애플리케이션 빌드/실행 가능함. JDK 9 이상에서 Spark가 내부적으로 사용하는 `sun.nio.ch` 패키지에 접근하려면 `forkArgs`에 `--add-opens` 옵션을 반드시 넣어야 함.

```scala
// build.mill
package build

import mill.*, scalalib.*

object foo extends ScalaModule {
  def scalaVersion = "2.12.20"
  def mvnDeps = Seq(
    mvn"org.apache.spark::spark-core:3.5.4",
    mvn"org.apache.spark::spark-sql:3.5.4"
  )

  def forkArgs = Seq("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")

  object test extends ScalaTests {
    def mvnDeps = Seq(mvn"com.lihaoyi::utest:0.9.1")
    def testFramework = "utest.runner.Framework"

    def forkArgs = Seq("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")
  }

}
```

- `foo.test`(uTest)도 별도의 `object test extends ScalaTests`로 정의하며, 테스트 실행 시에도 Spark를 fork하므로 동일한 `forkArgs` 필요함.
- 실행/테스트:

```console
> ./mill foo.run
...
+-------------+
|      message|
+-------------+
|Hello, World!|
+-------------+
...

> ./mill foo.test
...
+ foo.FooTests.helloWorld should create a DataFrame with one row containing 'Hello, World!'...
...
```

---

### 2. Hello PySpark (Python)

동일한 Spark 예제를 Python(PySpark)으로도 구성 가능함. `pythonlib`의 `PythonModule`을 사용하고 `mainScript`로 진입점 파일을 지정함.

```scala
// build.mill
package build

import mill.*, pythonlib.*

object foo extends PythonModule {

  def mainScript = Task.Source("src/foo.py")
  def pythonDeps = Seq("pyspark==4.0.1")

  object test extends PythonTests, TestModule.Unittest

}
```

- 테스트 프레임워크는 Python 표준 `unittest`(`TestModule.Unittest`)를 사용함.

```console
> ./mill foo.run
...
+-------------+
|      message|
+-------------+
|Hello, World!|
+-------------+
...

> ./mill foo.test
...
test_hello_world...
...
Ran 1 test...
...
OK
...
```

---

### 3. spark-submit을 사용하는 실전에 가까운 Spark 프로젝트

인자로 받은 `transactions.csv`(없으면 리소스의 기본 파일 사용)에서 카테고리별 합계·평균·건수를 계산하는 예제임. 모듈 이름을 ``` `package` ```로 지정해 루트 모듈로 만들고, `assembly`로 fat jar를 만든 뒤 `spark-submit.sh`로 실행하는 흐름을 보여줌.

```scala
// build.mill
package build

import mill.*, scalalib.*

object `package` extends ScalaModule {
  def scalaVersion = "2.12.20"
  def mvnDeps = Seq(
    mvn"org.apache.spark::spark-core:3.5.6",
    mvn"org.apache.spark::spark-sql:3.5.6"
  )

  def forkArgs = Seq("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")

  def prependShellScript = ""

  object test extends ScalaTests {
    def mvnDeps = Seq(mvn"com.lihaoyi::utest:0.9.1")
    def testFramework = "utest.runner.Framework"

    def forkArgs = Seq("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")
  }

}
```

- `def prependShellScript = ""`: Mill의 기본 assembly는 실행 가능한 셸 스크립트를 jar 앞에 붙이는데, `spark-submit`은 순수 jar 포맷을 기대하므로 이를 빈 문자열로 비워 일반 jar 형태로 만듦.

```console
> ./mill run
...
Summary Statistics by Category:
+-----------+------------+--------------+-----------------+
|   category|total_amount|average_amount|transaction_count|
+-----------+------------+--------------+-----------------+
|       Food|        70.5|          23.5|                3|
|Electronics|       375.0|         187.5|                2|
|   Clothing|       120.5|         60.25|                2|
+-----------+------------+--------------+-----------------+
...

> ./mill test
...
+ foo.FooTests.computeSummary should compute correct summary statistics...
...

> chmod +x spark-submit.sh

> ./mill show assembly # spark-submit 준비
".../out/assembly.dest/out.jar"

> ./spark-submit.sh out/assembly.dest/out.jar foo.Foo resources/transactions.csv
...
Summary Statistics by Category:
+-----------+------------+--------------+-----------------+
|   category|total_amount|average_amount|transaction_count|
+-----------+------------+--------------+-----------------+
|       Food|        70.5|          23.5|                3|
|Electronics|       375.0|         187.5|                2|
|   Clothing|       120.5|         60.25|                2|
+-----------+------------+--------------+-----------------+
...
```

#### 요약: Spark 예제 3종 비교

- 1. Hello Spark
  - 언어: Scala
  - 모듈 타입: `ScalaModule`
  - 테스트: uTest
  - 배포 방식: `./mill foo.run`
  - 특이 설정: `forkArgs`에 `--add-opens`
- 2. Hello PySpark
  - 언어: Python
  - 모듈 타입: `PythonModule`
  - 테스트: unittest
  - 배포 방식: `./mill foo.run`
  - 특이 설정: `mainScript`, `pythonDeps`
- 3. 실전에 가까운 프로젝트
  - 언어: Scala
  - 모듈 타입: 루트 ``` `package` ``` 모듈
  - 테스트: uTest
  - 배포 방식: `assembly` + `spark-submit.sh`
  - 특이 설정: `prependShellScript = ""` 추가

---

### 4. Scala 단일 파일 스크립트 개요

Mill은 소스 파일이 하나뿐인 Scala 모듈, 즉 "스크립트(script)"를 지원함. 빌드 설정은 파일 최상단의 헤더 주석 블록(`//|`로 시작하는 YAML)에 기술함.

- 독립 실행 파일로 쓸 수도 있고, 더 큰 프로젝트의 일부(moduleDeps 대상)로 쓸 수도 있음.
- 실행/조작 방식:

```bash
./mill Foo.scala              # 스크립트 실행 (run)
./mill Foo.scala:compile      # 컴파일만 수행
./mill Foo.scala:assembly     # 어셈블리(fat jar) 생성
```

- 단일 소스 파일이라는 제약 외에는 일반 `ScalaModule`이 지원하는 태스크를 동일하게 지원함.
- 파일 시스템/서브프로세스/HTTP 엔드포인트를 다루는 간단한 커맨드라인 워크플로를 짤 때, Bash 스크립트 대신 타입체크와 IDE 지원을 받으며 작성할 수 있다는 것이 핵심 이점임.
- 이런 스크립트는 보통 [번들 라이브러리](#14-번들-라이브러리와-raw-모드)를 활용해 빠르게 시작함.

---

### 5. 스크립트 활용 사례

#### 5.1 HTML 웹 스크래퍼

JSoup으로 Wikipedia HTML 페이지를 크롤링함. JSoup은 Mill에 번들되어 있지 않으므로 헤더의 `mvnDeps`로 추가함.

```scala
// HtmlScraper.scala
//| mvnDeps: [org.jsoup:jsoup:1.7.2]
import org.jsoup.*
import scala.collection.JavaConverters.*

def fetchLinks(title: String): Seq[String] = {
  Jsoup.connect(s"https://en.wikipedia.org/wiki/$title")
    .header("User-Agent", "My Scraper")
    .get().select("main p a").asScala.toSeq.map(_.attr("href"))
    .collect { case s"/wiki/$rest" => rest }
}

def main(startArticle: String, depth: Int) = {
  var seen = Set(startArticle)
  var current = Set(startArticle)
  for (i <- Range(0, depth)) {
    current = current.flatMap(fetchLinks(_)).filter(!seen.contains(_))
    seen = seen ++ current
  }

  pprint.log(seen, height = Int.MaxValue)
}
```

```console
> ./mill HtmlScraper.scala --start-article singapore --depth 1
...
  "Hokkien",
  "Conscription_in_Singapore",
  "Malaysia_Agreement",
...
```

`def main(startArticle: String, depth: Int)`처럼 정의하면 `mainargs` 기반으로 `--start-article`, `--depth` 같은 named 인자를 자동으로 받을 수 있음.

#### 5.2 JSON API 클라이언트

이번엔 JSoup 대신 Mill에 번들된 라이브러리(`requests`, `ujson`, `upickle`)만으로 Wikipedia JSON API를 조회하고 결과를 파일에 씀.

```scala
// JsonApiClient.scala
def fetchLinks(title: String): Seq[String] = {
  val resp = requests.get.stream(
    "https://en.wikipedia.org/w/api.php",
    params = Seq(
      "action" -> "query",
      "titles" -> title,
      "prop" -> "links",
      "format" -> "json"
    )
  )
  for {
    page <- ujson.read(resp)("query")("pages").obj.values.toSeq
    links <- page.obj.get("links").toSeq
    link <- links.arr
  } yield link("title").str
}

def main(startArticle: String, depth: Int) = {
  var seen = Set(startArticle)
  var current = Set(startArticle)
  for (i <- Range(0, depth)) {
    current = current.flatMap(fetchLinks(_)).filter(!seen.contains(_))
    seen = seen ++ current
  }

  pprint.log(seen)
  os.write(os.pwd / "fetched.json", upickle.stream(seen, indent = 4))
}
```

```console
> ./mill JsonApiClient.scala --start-article singapore --depth 2
...
  "Calling code",
  "+65",
  "British Empire",
  "1st Parliament of Singapore",
...
)

> cat fetched.json
[
    "Calling code",
    "+65",
    "British Empire",
    "1st Parliament of Singapore",
    ...
]
```

이 예제는 헤더에 별도 `mvnDeps` 선언 없이 번들 라이브러리만으로 동작함.

#### 5.3 웹 서버

[Cask](https://github.com/com-lihaoyi/cask) 프레임워크로 간단한 웹 서버를 띄움. 로컬 테스트/실험용으로 적합하며, 필요하면 나중에 `build.mill` 기반의 본격적인 프로젝트로 확장 가능함.

```scala
// WebServer.scala
//| mvnDeps: [com.lihaoyi::cask:0.9.1]

object WebServer extends cask.MainRoutes {
  override def port = sys.env.getOrElse("PORT", "8080").toInt

  @cask.post("/reverse-string")
  def doThing(request: cask.Request) = {
    request.text().reverse
  }

  initialize()
}
```

```console
> ./mill WebServer.scala:runBackground

> curl -d 'helloworld' localhost:${PORT:-8080}/reverse-string
dlrowolleh
```

`:runBackground` 태스크로 서버를 백그라운드 프로세스로 띄운 뒤 다른 터미널에서 요청을 보낼 수 있음.

#### 5.4 데이터베이스 쿼리

[ScalaSql](https://github.com/com-lihaoyi/scalasql)로 로컬 SQLite 데이터베이스를 채우고 쿼리함. `sqlite-customers.sql` 파일 내용으로 DB를 초기화한 뒤, join/filter/map으로 오늘 이후 배송 예정인 구매자 이름을 조회함.

```scala
// Database.scala
//| mvnDeps:
//| - com.lihaoyi::scalasql:0.2.3
//| - com.lihaoyi::scalasql-namedtuples:0.2.3
//| - org.xerial:sqlite-jdbc:3.43.0.0

import scalasql.simple._, SqliteDialect.*
import java.time.LocalDate

case class Buyer(id: Int, name: String, dateOfBirth: LocalDate)
object Buyer extends SimpleTable[Buyer]

case class ShippingInfo(id: Int, buyerId: Int, shippingDate: LocalDate)
object ShippingInfo extends SimpleTable[ShippingInfo]

def main(args: Array[String]): Unit = {
  // 데이터베이스 초기화
  val dataSource = new org.sqlite.SQLiteDataSource()
  dataSource.setUrl(s"jdbc:sqlite:./file.db")
  val sqliteClient = new scalasql.DbClient.DataSource(dataSource, config = new scalasql.Config {})

  sqliteClient.transaction { db =>
    db.updateRaw(os.read(os.pwd / "sqlite-customers.sql")) // SQL 파일로 데이터베이스 채우기

    val names = db.run( // 배송일이 오늘 이후인 구매자 이름 조회
      Buyer.select.join(ShippingInfo)(_.id === _.buyerId)
        .filter { case (b, s) => s.shippingDate >= LocalDate.parse(args(0)) }
        .map { case (b, s) => b.name })

    for (name <- names) println(name)
  }
}
```

```console
> ./mill Database.scala 2011-01-01
James Bond
John Doe
```

`mvnDeps` 헤더는 리스트 인라인 표기(`[...]`)뿐 아니라 YAML 시퀀스(`- 항목`) 형태로도 여러 줄에 걸쳐 쓸 수 있음.

#### 5.5 정적 사이트 생성기

`post/` 폴더의 마크다운 파일을 [Commonmark-Java](https://github.com/commonmark/commonmark-java)로 HTML 변환해 `site-out/` 폴더에 씀.

```scala
// StaticSite.scala
//| mvnDeps:
//| - com.lihaoyi::scalatags:0.13.1
//| - com.atlassian.commonmark:commonmark:0.13.1
import scalatags.Text.all.*

def main() = {
  val postInfo = os
    .list(os.pwd / "post")
    .map { p =>
      val s"$prefix - $suffix.md" = p.last
      (prefix, suffix, p)
    }
    .sortBy(_._1.toInt)

  os.remove.all(os.pwd / "site-out")
  os.makeDir.all(os.pwd / "site-out/post")

  for ((_, suffix, path) <- postInfo) {
    val parser = org.commonmark.parser.Parser.builder().build()
    val document = parser.parse(os.read(path))
    val renderer = org.commonmark.renderer.html.HtmlRenderer.builder().build()
    val output = renderer.render(document)
    os.write(
      os.pwd / "site-out/post" / (suffix.replace(" ", "-").toLowerCase + ".html"),
      doctype("html")(
        html(
          body(
            h1(a("Blog"), " / ", suffix),
            raw(output)
          )
        )
      )
    )
  }
}
```

```console
> ./mill StaticSite.scala

> cat site-out/post/my-first-post.html
<!DOCTYPE html><html><body><h1><a>Blog</a> / My First Post</h1><p>Sometimes you want numbered lists:</p>
<ol>
<li>One</li>
<li>Two</li>
<li>Three</li>
</ol>
</body></html>
```

`scalatags`로 HTML을 타입 안전하게 생성하는 패턴을 보여줌.

---

### 6. 어셈블리와 네이티브 바이너리 패키징

단일 파일 스크립트도 실행 가능한 어셈블리(fat jar)나 GraalVM 네이티브 이미지로 패키징해 배포 가능함. 네이티브 이미지를 만들려면 `jvmVersion`을 `graalvm` 계열 버전으로 지정 필요.

```scala
// Bar.scala
//| scalaVersion: 3.8.2
//| jvmVersion: "graalvm-community:17"
//| nativeImageOptions: ["--no-fallback"]
def main() = {
  println("Hello World")
}
```

```console
> ./mill Bar.scala:assembly

> ./out/Bar.scala/assembly.dest/out.jar # mac/linux
Hello World

> ./mill Bar.scala:nativeImage

> ./out/Bar.scala/nativeImage.dest/native-executable
Hello World
```

---

### 7. 스크립트 REPL 열기

스크립트에 정의된 코드를 로드한 채로 Scala REPL을 열어 대화형으로 실험 가능함.

```scala
./mill Foo.scala:repl
```

---

### 8. 스크립트 moduleDeps

단일 파일 스크립트끼리도 `moduleDeps`로 서로 의존 가능함. 여러 스크립트가 공유하는 로직을 재사용할 때 유용함.

```scala
// Foo.scala
val fooValue = 1337
```

```scala
// Bar.scala
//| moduleDeps: [Foo.scala]

def main() =
  println(fooValue)
```

```console
> ./mill Bar.scala
1337
```

---

### 9. 상대/절대 경로 스크립트 moduleDeps

스크립트 간 `moduleDeps`는 상대 경로 또는 워크스페이스 절대 경로(`//` 접두사)로 지정 가능함.

```scala
// bar/Bar.scala
//| scalaVersion: 3.8.2
package bar

def generateHtml(text: String) = "<h1>" + text + "</h1>"
```

같은 폴더(`bar/`) 안에서는 `Bar.scala`처럼 폴더 상대 경로로 참조함.

```scala
// bar/BarTests.scala
//| extends: mill.script.ScalaModule.Utest
//| moduleDeps: [Bar.scala]
//| mvnDeps:
//| - com.lihaoyi::utest:0.9.1
package bar
import utest.*
object BarTests extends TestSuite {
  def tests = Tests {
    assert(generateHtml("hello") == "<h1>hello</h1>")
  }
}
```

다른 폴더(`foo/`)에서는 `//bar/Bar.scala`처럼 워크스페이스 루트 기준 절대 경로(`//` 접두사)로 참조함.

```scala
// foo/Foo.scala
//| moduleDeps: [//bar/Bar.scala]
//| scalaVersion: 3.8.2

package foo

def main(args: Array[String]): Unit = println(bar.generateHtml(args(0)))
```

```console
> ./mill bar/Bar.scala:compile
> ./mill bar/BarTests.scala
> ./mill foo/Foo.scala --text hello
```

`BarTests.scala`는 `//| extends: mill.script.ScalaModule.Utest`로 uTest 기반 테스트 모듈로 확장된 예임.

---

### 10. 프로젝트 모듈에 대한 moduleDeps

단일 파일 스크립트는 `build.mill`에 선언된 프로그래밍 가능한(programmable) 모듈에도 의존 가능함. 라이브러리/애플리케이션 코드에 정의된 클래스·메서드를 스크립트에서 재사용하고 싶을 때 유용함.

```scala
// build.mill
package build
import mill.*, scalalib.*

object bar extends ScalaModule {
  def scalaVersion = "3.8.2"
  def mvnDeps = Seq(mvn"com.lihaoyi::scalatags:0.13.1")
}
```

```scala
// Foo.scala
//| moduleDeps: [bar]

def main(args: Array[String]) = {
  println(bar.Bar.generateHtml(args(0)))
}
```

```console
> ./mill Foo.scala hello
<h1>hello</h1>
```

---

### 11. 커스텀 스크립트 모듈 클래스

기본적으로 단일 파일 Scala 모듈은 내장 `mill.script.ScalaModule`을 상속함. 메타 빌드(`mill-build/src/`)에 직접 정의한 커스텀 `Module` 클래스를 상속하도록 바꿀 수도 있음. 예를 들어 소스 파일을 처리해 생성한 리소스 파일을 추가하려면 다음처럼 `LineCountScalaModule`을 정의함.

```scala
// Qux.scala
//| extends: millbuild.LineCountScalaModule
//| scalaVersion: 3.8.2
package qux

def getLineCount() = {
  scala.io.Source
    .fromResource("line-count.txt")
    .mkString
}

def main() = {
  println(s"Line Count: ${getLineCount()}")
}
```

```scala
// mill-build/src/LineCountScalaModule.scala
package millbuild
import mill.*, scalalib.*

class LineCountScalaModule(scriptConfig: mill.api.ScriptModule.Config)
    extends mill.script.ScalaModule(scriptConfig) {

  /** 모듈 소스 파일의 총 라인 수 */
  def lineCount = Task {
    allSourceFiles().map(f => os.read.lines(f.path).size).sum
  }

  /** 소스의 lineCount를 이용해 리소스 생성 */
  override def resources = Task {
    os.write(Task.dest / "line-count.txt", "" + lineCount())
    super.resources() ++ Seq(PathRef(Task.dest))
  }
}
```

```console
> ./mill Qux.scala
...
Line Count: 17

> ./mill show Qux.scala:lineCount
17
```

주의할 점:

- 커스텀 클래스는 `class`여야 하고, 생성자 인자로 `mill.script.ScriptModule.Config`를 받아 `mill.script.ScalaModule`에 넘겨야 함.
- 많은 스크립트가 비슷한 설정을 공유하거나, YAML 빌드 헤더만으로는 표현할 수 없는 커스터마이징이 필요할 때 이 방식으로 동작을 중앙에서 정의하고 `extends`로 표준화할 수 있음.
- 일반 선언적(declarative)/프로그래밍 가능한(programmable) 모듈은 여러 abstract `trait`을 상속할 수 있지만, 단일 파일 스크립트는 실행 시점에 동적으로 인스턴스화되기 때문에 오직 하나의 concrete `class`만 상속할 수 있음.

---

### 12. 스크립트 리소스

단일 파일 Scala 모듈은 기본적으로 classpath 리소스가 활성화되어 있지 않으며, 빌드 헤더의 `resources` 키로 추가함.

```scala
// Foo.scala
//| resources: [resources/]

package foo

def main() = {
  println(os.read(os.resource / "file.txt"))
}
```

```console
> ./mill Foo.scala
Hello World Resource File
```

classpath 리소스는 스크립트를 assembly나 실행 파일로 패키징할 때 함께 포함되므로, 배포 시 별도 파일을 관리할 필요 없음.

```console
> ./mill show Foo.scala:assembly
".../out/Foo.scala/assembly.dest/out.jar"

> out/Foo.scala/assembly.dest/out.jar
Hello World Resource File
```

---

### 13. 커스텀 JVM 버전

단일 파일 Scala 모듈은 기본적으로 JVM `zulu:25`를 사용함. 헤더의 `jvmVersion`으로 변경 가능함.

```scala
// Foo.scala
//| jvmVersion: 11.0.21
//| scalaVersion: 3.7.4

def main() = {
  println("java.version " + System.getProperty("java.version"))
}
```

```console
> ./mill Foo.scala
java.version 11.0.21
```

`jvmVersion`을 `11.x`로 낮추면 기본 `scalaVersion`인 `3.8.x`와 호환되지 않으므로, 위 예제처럼 `scalaVersion: 3.7.4`를 명시적으로 함께 지정 필요.

---

### 14. 번들 라이브러리와 Raw 모드

Mill Scala 스크립트는 다음 라이브러리를 기본 번들로 제공하며, 이는 Mill 빌드 파일 자체에서 사용 가능한 라이브러리 목록과 대체로 동일함.

- [OS-Lib](https://github.com/com-lihaoyi/os-lib): 파일시스템/프로세스 작업
- [uPickle](https://github.com/com-lihaoyi/upickle): JSON 직렬화
- [Requests-Scala](https://github.com/com-lihaoyi/requests-scala): HTTP 요청
- [MainArgs](https://github.com/com-lihaoyi/mainargs): CLI 인자 파싱(`@main`이 `@mainargs.main`으로 별칭됨)
- [PPrint](https://github.com/com-lihaoyi/PPrint): 값 pretty-print

이 라이브러리들은 단일 파일 스크립트에서만 편의상 기본 제공되며, 선언적/프로그래밍 가능 모듈에서는 `mvnDeps`에 명시적으로 추가 필요. 이 외의 라이브러리는 `//| mvnDeps`로 자유롭게 추가 가능함.

번들 라이브러리를 원하지 않으면 `mill.script.ScalaModule.Raw`를 상속하도록 만듦.

```scala
// Foo.scala
//| extends: mill.script.ScalaModule.Raw

def main(args: Array[String]): Unit = {
  println(os.read(os.pwd / "file.txt"))
}
```

```console
> ./mill Foo.scala
error: ...Not found: os
```

`Raw` 모드에서는 번들 라이브러리에 접근할 수 없으므로, Java 표준 라이브러리를 쓰거나

```scala
// Bar.scala
//| extends: mill.script.ScalaModule.Raw

def main(args: Array[String]): Unit = {
  println(java.nio.file.Files.readString(java.nio.file.Path.of("file.txt")))
}
```

```console
> ./mill Bar.scala
hello
```

필요한 라이브러리는 `mvnDeps`로 직접 추가 필요.

```scala
// Qux.scala
//| extends: mill.script.ScalaModule.Raw
//| mvnDeps: [com.lihaoyi::os-lib:0.11.4]

def main(args: Array[String]): Unit = {
  println(os.read(os.pwd / "file.txt"))
}
```

```console
> ./mill Qux.scala
hello
```

---

### 15. 스크립트 IDE 연동(BSP)

IntelliJ/VSCode용 BSP(Build Server Protocol) 지원은 스크립트를 단일 파일 모듈로 취급하되, 탐색(discovery) 방식이 일반 모듈과 다름. IDE로 프로젝트를 임포트하면 Mill은 워크스페이스 폴더를 재귀적으로 탐색하며 `.java`, `.scala`, `.kt` 파일 중 스크립트일 수 있는 파일을 찾되, 다음 경로는 건너뜀.

- `sources` 디렉터리
- `out` 디렉터리
- `**/src/`
- `**/src-*/`
- `**/resources/`
- `**/out/`
- `**/target/`

기본 무시 경로로 충분하지 않다면, `build.mill`에 `//| bspScriptIgnore: [...]`를 추가하거나 `build.mill.yaml`에 `mill-build: bspScriptIgnore: [...]`를 추가해 폴더를 더 지정 가능함. `bspScriptIgnore`는 문자열 시퀀스를 받으며 `.gitignore` 문법을 따름.

- `*`: 단일 경로 세그먼트 내 와일드카드
- `**`: 여러 경로 세그먼트에 걸친 와일드카드
- `!`: 무시 규칙 부정(negate)

프로젝트가 비전통적인 폴더 레이아웃으로 Java/Scala/Kotlin 소스 파일을 배치하고 있어 IDE가 이를 스크립트로 잘못 인식할 때 유용한 옵션임.

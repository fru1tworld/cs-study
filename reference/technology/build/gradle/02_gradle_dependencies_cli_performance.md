# Gradle 기초: 의존성, CLI, 래퍼, 성능

## Gradle 의존성 관리 기초

> **원문:** https://docs.gradle.org/current/userguide/dependency_management_basics.html

### 개요

Gradle은 프로젝트에 필요한 라이브러리, JAR, 소스 코드 같은 외부 자원을 선언하고 해석(resolve)하는 의존성 관리(Dependency Management) 기능을 제공한다. `build.gradle(.kts)`에 의존성을 선언하면 다운로드와 캐싱, 버전 해석, 충돌 처리를 자동으로 수행해 개발자가 수동으로 관리해야 할 일을 줄인다.

### 의존성 선언하기

의존성은 `dependencies {}` 블록에 선언한다. 아래 예제에서는 각 라이브러리를 `implementation`과 `api`에 넣어 사용 범위를 구분한다.

```kotlin
plugins {
    id("java-library")
}

dependencies {
    implementation("com.google.guava:guava:32.1.2-jre")
    api("org.apache.juneau:juneau-marshall:8.2.0")
}
```

Gradle은 의존성을 컨피규레이션(Configuration)이라는 버킷 단위로 묶어 관리한다. 어떤 컨피규레이션에 넣느냐에 따라 의존성이 언제, 어디서 사용되는지 범위(scope)가 달라진다.

- `implementation`: 프로덕션 코드를 컴파일, 실행할 때 필요한 의존성
  - 소비자(consumer)에게는 노출되지 않음
- `api`: 라이브러리를 사용하는 외부 모듈에도 노출되어야 하는 의존성
- 플러그인을 적용하면 그에 맞는 컨피규레이션이 자동으로 생성됨
  - `java` / `java-library` 플러그인 → `implementation`, `api`, `compileOnly`, `runtimeOnly`, `testImplementation` 등
  - Android Gradle Plugin(AGP) → `debugImplementation`, `releaseImplementation`, `androidTestImplementation`, `freeDebugImplementation` 같은 빌드 타입, 플레이버별 컨피규레이션
  - Kotlin Multiplatform(KMP) → `commonMainImplementation`, `commonTestImplementation`, `iosArm64MainImplementation` 등 소스 세트 단위 컨피규레이션

플러그인이 프로젝트 구조에 맞는 컨피규레이션을 미리 만들기 때문에, 개발자는 필요한 범위에 의존성을 선언하면 된다.

### 의존성 트리 확인하기

- `dependencies` 태스크로 프로젝트의 의존성 트리를 컨피규레이션별로 확인 가능

```bash
./gradlew :app:dependencies
```

- 출력 결과는 컨피규레이션(예: `runtimeClasspath`)별로 그룹화되어 트리 형태로 표시됨
- `->` 표시는 버전이 다른 값으로 변경(치환)되었음을 의미
- `(c)`는 해당 좌표가 제약(constraint)으로만 참여했음을 나타내는 표시
- 전이 의존성(transitive dependency, 의존성이 끌고 들어오는 또 다른 의존성)까지 한눈에 파악 가능 → 버전 충돌 여부 진단에 유용

### 버전 카탈로그(Version Catalog)

#### 왜 필요한가

여러 서브프로젝트에서 같은 라이브러리를 반복 선언하면 버전 정보가 흩어지기 쉽다. 버전 카탈로그는 이 좌표(coordinate)와 버전을 한곳에 모아 관리하는 기능이다.

- 서브프로젝트 간 공통 의존성 선언 공유
- 중복 선언, 버전 불일치 방지
- 대규모 프로젝트에서 의존성/플러그인 버전 강제(enforce)

#### 구조

- `gradle/libs.versions.toml` 파일에 다음 네 섹션을 정의

- `[versions]`: 라이브러리, 플러그인이 참조할 버전 번호 선언
- `[libraries]`: 실제 빌드에서 사용할 라이브러리 정의
- `[bundles]`: 여러 의존성을 하나로 묶은 집합 정의
- `[plugins]`: 플러그인 정의

```toml
[versions]
guava = "32.1.2-jre"
juneau = "8.2.0"

[libraries]
guava = { group = "com.google.guava", name = "guava", version.ref = "guava" }
juneau-marshall = { group = "org.apache.juneau", name = "juneau-marshall", version.ref = "juneau" }
```

- 이 파일을 프로젝트의 `gradle/` 디렉터리에 두면 Gradle이 자동으로 인식 → 빌드 스크립트에서는 `libs` 접근자로 참조 가능

```kotlin
dependencies {
    implementation(libs.guava)
    api(libs.juneau.marshall)
}
```

- IntelliJ, Android Studio 등 IDE도 이 메타데이터를 인식해 코드 자동완성 지원
- 버전 문자열을 직접 하드코딩하지 않으므로, 버전을 올릴 때 `[versions]` 섹션 한 곳만 수정하면 됨

## Gradle 커맨드라인 인터페이스 기초

> **원문:** https://docs.gradle.org/current/userguide/command_line_interface_basics.html

### 개요

IDE를 사용하지 않을 때는 커맨드라인에서 Gradle을 실행한다. 태스크와 옵션을 조합해 빌드하거나, 의존성을 확인하고 로그 레벨을 조정할 수 있다. 여기서는 기본 사용법을 다루며, 전체 옵션 목록은 별도의 Command-Line Interface 레퍼런스 문서에서 확인할 수 있다.

### 기본 명령 구조

- Gradle 명령은 다음과 같은 형태를 따름

```
gradle [태스크...] [--옵션...]
```

- 태스크는 공백으로 구분해 여러 개를 한 번에 지정 가능
- 옵션은 태스크 이름의 앞이나 뒤 어디에 와도 무방

```bash
gradle build
gradle clean build          # 여러 태스크를 순서대로 실행
gradle --build-cache build  # 옵션을 태스크보다 먼저 써도 동일하게 동작
```

### 옵션 작성 규칙

#### 값을 받는 옵션

- 값이 필요한 옵션은 `=`로 값을 지정하는 것이 명확함

```bash
gradle build --console=plain
```

#### on/off 토글 옵션

- 일부 옵션은 `--no-` 접두사를 붙여 반대 동작을 지정하는 짝을 가짐

```bash
gradle build --build-cache      # 빌드 캐시 사용
gradle build --no-build-cache   # 빌드 캐시 미사용
```

#### 짧은 옵션 표기

- 자주 쓰는 옵션은 짧은 별칭 존재
  - 예를 들어 `-h`는 `--help`와 동일

### 태스크 실행과 프로젝트 지정

- 멀티 프로젝트 빌드에서는 콜론(`:`)으로 프로젝트 경로를 표현 → 특정 하위 프로젝트의 태스크만 실행 가능

```bash
gradle :test              # 루트 프로젝트의 test 태스크
gradle :app:test          # app 하위 프로젝트의 test 태스크
gradle test               # 현재 디렉터리를 기준으로 실행
```

### 태스크별 옵션

- 태스크 자체가 고유한 옵션을 가질 수 있으며, 태스크 이름 뒤에 붙여 전달

```bash
gradle taskName --exampleOption=exampleValue
```

- 즉 커맨드라인 옵션은 두 종류로 구분됨
- Gradle 실행 자체를 제어하는 전역 옵션 (`--build-cache`, `--console` 등)
- 특정 태스크의 동작만 바꾸는 태스크 전용 옵션

### Gradle Wrapper 사용 권장

- 문서는 `gradle` 명령을 직접 쓰기보다 Gradle Wrapper(`./gradlew`, Windows는 `gradlew.bat`) 사용을 강력히 권장
  - Wrapper를 쓰면 프로젝트에 지정된 Gradle 버전이 로컬 설치 여부와 무관하게 그대로 사용됨 → 팀원, CI 환경 간 빌드 결과가 달라지는 문제 방지

```bash
./gradlew build       # macOS / Linux
gradlew.bat build     # Windows
```

## Gradle Wrapper 기본 개념

> **원문:** https://docs.gradle.org/current/userguide/gradle_wrapper_basics.html

### 개요

Gradle Wrapper는 프로젝트에 지정된 버전의 Gradle을 내려받아 실행하는 얇은 래퍼다. Gradle 실행 파일 자체를 포함하는 것은 아니므로, 로컬에 Gradle이 없어도 프로젝트의 스크립트와 설정 파일로 빌드할 수 있다. 시스템에 설치된 `gradle` 명령을 직접 실행하기보다 Wrapper를 사용하는 것이 권장된다.

### Wrapper를 쓰는 이유

- 버전 자동 관리: 프로젝트에서 정한 Gradle 버전을 알아서 다운로드해 사용
- 팀 표준화: 팀원 전체가 항상 동일한 Gradle 버전으로 빌드 → "내 컴퓨터에서는 되는데" 같은 문제 감소
- 환경 일관성: 로컬 개발 환경, IDE, CI 서버 등 어디서 실행하든 같은 버전의 Gradle 사용
- 설치 부담 감소: 사용자가 Gradle을 별도로 설치할 필요 없음

### Wrapper 구성 파일

- Gradle 프로젝트에는 다음 네 가지 Wrapper 관련 파일이 존재함

- `gradlew`: Unix 계열(Linux/macOS)에서 실행하는 셸 스크립트
- `gradlew.bat`: Windows에서 실행하는 배치 스크립트
- `gradle/wrapper/gradle-wrapper.jar`: 지정된 Gradle 버전을 다운로드, 설치하는 로직이 담긴 작은 JAR
- `gradle/wrapper/gradle-wrapper.properties`: 다운로드할 Gradle 배포판의 URL, 배포 형식(zip/tarball) 등을 담은 설정 파일

디렉터리 구조는 대략 다음과 같다.

```
project-root/
├── gradlew
├── gradlew.bat
└── gradle/
    └── wrapper/
        ├── gradle-wrapper.jar
        └── gradle-wrapper.properties
```

Wrapper 파일은 버전 관리 시스템에 커밋해 팀과 CI가 같은 설정을 공유하도록 한다. 파일을 직접 수정하기보다는 아래의 Wrapper 갱신 명령으로 버전을 바꾼다.

### 사용법

#### 빌드 실행

- Linux/macOS: `./gradlew build`
- Windows(PowerShell/cmd): `gradlew.bat build`

- 다른 디렉터리에서 실행해야 한다면 `gradlew` 스크립트까지의 상대 경로를 지정해서 호출하면 됨

#### 버전 확인

```
./gradlew --version
```

#### 버전 업그레이드

```
./gradlew wrapper --gradle-version 7.2
```
- 이 명령을 실행하면 `gradle-wrapper.properties`의 배포판 URL이 갱신됨 → 이후 빌드부터는 새 버전이 다운로드되어 사용됨

### Wrapper가 없는 경우

- 만약 저장소에 Wrapper 파일이 없다면 두 가지 경우 중 하나임
- 1\. 애초에 Gradle 프로젝트가 아님
- 2\. Gradle 프로젝트이지만 Wrapper가 아직 생성되지 않음

- 이때는 Gradle이 이미 설치되어 있는 머신에서 `gradle wrapper` 명령을 실행해 Wrapper 파일들을 새로 생성하면 됨

## Build Scan 기본 개념

> **원문:** https://docs.gradle.org/current/userguide/build_scans.html

### 개요

Build Scan은 빌드 실행 중 수집한 메타데이터를 시각화한 결과물이며, 실행할 때마다 새로 생성된다. 빌드가 끝나면 Gradle이 실행 정보를 Build Scan Service로 보내고, 서비스는 이 데이터를 분석하기 쉬운 웹 리포트로 가공한다. 기본 업로드 대상은 공개 서비스인 `scans.gradle.com`이며, 링크를 아는 사람은 리포트에 접근할 수 있다. 조직에서 운영하는 Develocity 서버로 대상을 바꿀 수도 있다.

### 왜 필요한가

- 트러블슈팅: 빌드 실패나 성능 저하의 원인을 파악할 때, 수집된 데이터가 근거 자료가 됨
- 협업/질문: 커뮤니티나 동료에게 도움을 요청할 때 에러 메시지와 환경 정보를 일일이 복사해서 붙여넣을 필요 없이, Build Scan 링크 하나만 공유하면 됨
- 성능 분석: 태스크별 소요 시간, 캐시 적중 여부 등을 확인해 빌드 성능 개선에 활용 가능

### 활성화 방법

#### 커맨드라인에서 즉시 생성

- Gradle 명령 뒤에 `--scan` 플래그만 붙이면 됨

```
./gradlew build --scan
```

- 처음 사용할 때는 Build Scan 이용 약관에 동의하라는 프롬프트가 표시될 수 있음
- 빌드가 끝나면 콘솔에 리포트로 이동할 수 있는 URL이 출력됨

#### 자체 서버(Develocity)로 전송

- 공개 서비스가 아니라 조직에서 운영하는 Develocity 서버로 리포트를 보내고 싶다면, `--develocity-url` 옵션으로 대상 서버를 지정

```
./gradlew build --develocity-url=https://develocity.example.com
```

- 지속적으로 특정 서버를 사용하려면 매번 옵션을 주는 대신 Develocity Gradle 플러그인을 통해 프로젝트 설정에 고정해 두는 방식 권장

### 어떤 정보가 담기는가

- Build Scan에는 실행 환경(OS, JVM 버전), 태스크 실행 결과, 소요 시간, 의존성 해석 과정, 콘솔 출력 등 빌드 관련 메타데이터가 포함됨
- 구체적으로 어떤 항목이 수집되는지, 그리고 데이터가 어떻게 보호되는지에 대한 세부 내용은 Gradle Develocity 플러그인 문서에서 별도로 다룸
- 민감한 정보가 포함될 수 있으므로, 공개 서비스에 업로드하기 전에는 어떤 데이터가 전송되는지 확인 필요

## Gradle 캐싱 기초 (증분 빌드와 빌드 캐시)

> **원문:** https://docs.gradle.org/current/userguide/gradle_optimizations.html

### 개요

Gradle은 이전 실행 결과를 기억해 두고 바뀐 부분만 다시 계산해 빌드 시간을 줄인다. 이를 가능하게 하는 두 가지 메커니즘이 증분 빌드(incremental build)와 빌드 캐시(build cache)다.

- 증분 빌드: 로컬에서 "이전 실행과 입력이 같으면 건너뜀"이라는 원리
- 빌드 캐시: "누군가(나 자신 또는 팀원, CI)가 이미 만들어둔 결과물을 재사용함"이라는 원리

- 두 기능 모두 태스크의 입력(input)과 출력(output)을 비교해서 동작 여부를 판단하기 때문에, 태스크가 입력/출력을 제대로 선언하고 있어야 효과를 볼 수 있음

### 태스크 실행 결과 레이블

- `--console=verbose` 옵션으로 빌드를 실행하면 각 태스크가 어떤 이유로 실행되었거나 건너뛰어졌는지 라벨로 확인 가능

- (라벨 없음, 실행됨): 입력이 바뀌어 태스크가 실제로 수행됨
- `UP-TO-DATE`: 이전 실행과 입력, 출력이 동일해서 실행을 건너뜀
- `FROM-CACHE`: 로컬 또는 원격 빌드 캐시에서 출력을 그대로 복원함
- `NO-SOURCE`: 처리할 소스 파일 자체가 없어서 건너뜀 (예: 컴파일할 `.java` 파일 없음)
- `SKIPPED`: `onlyIf` 조건이나 커맨드라인 옵션 등으로 인해 실행되지 않음
- `FAILED`: 태스크 실행 중 오류 발생

`UP-TO-DATE`와 `FROM-CACHE`는 모두 태스크를 다시 실행하지 않았다는 뜻이다. 전자는 증분 빌드 판단으로 실행을 생략한 것이고, 후자는 빌드 캐시에서 출력을 복원한 것이다. 한번 `FROM-CACHE`로 복원한 뒤 다음 빌드에서도 입력이 그대로라면, 이제 로컬 결과를 사용할 수 있어 `UP-TO-DATE`로 표시된다.

### 증분 빌드 (Incremental Build)

- 증분 빌드는 직전 빌드와 비교해 입력이 바뀌지 않은 태스크의 실행을 생략하는 기능
  - Gradle에서는 별도 설정 없이 기본적으로 항상 켜져 있음

- 동작 원리는 단순함

- 태스크마다 선언된 입력(소스 파일, 설정 값 등)과 출력(생성 파일 등)의 상태(주로 해시 값)를 기록
- 다음 빌드 때 현재 상태와 기록된 상태를 비교
- 값이 같으면 실행을 건너뛰고 `UP-TO-DATE`로 표시 → 다르면 태스크를 실제로 실행

- 실행 과정을 자세히 보려면 verbose 콘솔 모드를 사용

```bash
$ ./gradlew compileJava --console=verbose
> Task :app:compileJava UP-TO-DATE
BUILD SUCCESSFUL in 374ms
```

- 매번 옵션을 입력하기 번거롭다면 `gradle.properties`에 다음처럼 고정 가능

```properties
org.gradle.console=verbose
```

### 빌드 캐시 (Build Cache)

증분 빌드는 같은 워크스페이스의 로컬 이력을 참고한다. 빌드 캐시는 여기에 없는 결과도 재사용할 수 있도록, 이전에 같은 입력으로 실행한 태스크의 결과물을 저장한다. 브랜치가 다르거나 팀원과 CI 서버처럼 실행 주체가 달라도 입력이 같으면 저장된 결과를 가져올 수 있다.

전형적인 활용 시나리오는 다음과 같다.

- 같은 코드를 여러 브랜치에서 반복 빌드하는 경우
- 여러 팀원이 동일한 모듈을 각자 빌드하는 경우
- CI 파이프라인에서 동일한 커밋을 여러 번 빌드하는 경우

- 이런 상황에서는 이미 누군가 만들어 놓은 컴파일/테스트 결과를 그대로 재사용해 중복 작업을 제거

- 빌드 캐시는 기본적으로 꺼져 있으며, `--build-cache` 옵션으로 활성화

```bash
$ ./gradlew compileJava --build-cache
> Task :app:compileJava FROM-CACHE
BUILD SUCCESSFUL in 364ms
```

### 두 기능 비교

- 기본 활성화 여부
  - 증분 빌드: 항상 켜짐
  - 빌드 캐시: `--build-cache`로 켜야 함
- 비교 대상
  - 증분 빌드: 같은 워크스페이스의 직전 실행 결과
  - 빌드 캐시: 로컬/원격 캐시에 저장된 임의의 과거 결과
- 결과 표시
  - 증분 빌드: `UP-TO-DATE`
  - 빌드 캐시: `FROM-CACHE`
- 공유 범위
  - 증분 빌드: 나 혼자, 이 프로젝트 디렉터리
  - 빌드 캐시: 팀원, CI 등 여러 실행 주체 간 공유 가능

두 메커니즘은 함께 작동한다. 로컬에서는 증분 빌드 판단으로 실행을 건너뛰고, 로컬 이력에 없는 경우에는 빌드 캐시에서 재사용할 결과를 찾는다.

# Gradle 기초: 의존성, CLI, 래퍼, 성능

## Gradle 의존성 관리 기초

> **원문:** https://docs.gradle.org/current/userguide/dependency_management_basics.html

### 개요

Gradle은 프로젝트에 필요한 외부 자원(라이브러리, JAR, 소스 코드 등)을 선언하고 해석(resolve)하는 과정을 자동화하는 기능을 기본으로 제공함. 이를 **의존성 관리**(Dependency Management)라고 부름.

- 의존성은 `build.gradle(.kts)` 스크립트 안에서 선언
- Gradle이 다운로드·캐싱·버전 해석·버전 충돌 처리를 자동으로 처리
- 개발자가 라이브러리 버전을 수동으로 관리할 필요 감소

### 의존성 선언하기

의존성은 `dependencies {}` 블록 안에 선언함.

```kotlin
plugins {
    id("java-library")
}

dependencies {
    implementation("com.google.guava:guava:32.1.2-jre")
    api("org.apache.juneau:juneau-marshall:8.2.0")
}
```

#### 핵심 포인트: 컨피규레이션(Configuration, "버킷")

Gradle은 의존성을 **컨피규레이션**이라는 버킷 단위로 묶어서 관리함. 컨피규레이션은 해당 의존성이 *언제·어디서·어떻게* 사용되는지 범위(scope)를 정의함.

- `implementation` — 프로덕션 코드를 컴파일·실행할 때 필요한 의존성. 소비자(consumer)에게는 노출되지 않음
- `api` — 라이브러리를 사용하는 외부 모듈에도 노출되어야 하는 의존성
- 플러그인을 적용하면 그에 맞는 컨피규레이션이 자동으로 생성됨
  - `java` / `java-library` 플러그인 → `implementation`, `api`, `compileOnly`, `runtimeOnly`, `testImplementation` 등
  - Android Gradle Plugin(AGP) → `debugImplementation`, `releaseImplementation`, `androidTestImplementation`, `freeDebugImplementation` 같은 빌드 타입·플레이버별 컨피규레이션
  - Kotlin Multiplatform(KMP) → `commonMainImplementation`, `commonTestImplementation`, `iosArm64MainImplementation` 등 소스 세트 단위 컨피규레이션

즉, 별도 설정 없이도 플러그인이 프로젝트 구조에 맞는 "올바른 자리"를 미리 만들어 두는 방식임.

### 의존성 트리 확인하기

`dependencies` 태스크로 프로젝트의 의존성 트리를 컨피규레이션별로 확인 가능.

```bash
./gradlew :app:dependencies
```

- 출력 결과는 컨피규레이션(예: `runtimeClasspath`)별로 그룹화되어 트리 형태로 표시됨
- `->` 표시는 버전이 다른 값으로 변경(치환)되었음을 의미
- `(c)`는 해당 좌표가 제약(constraint)으로만 참여했음을 나타내는 표시
- 전이 의존성(transitive dependency, 의존성이 끌고 들어오는 또 다른 의존성)까지 한눈에 파악 가능 → 버전 충돌 여부 진단에 유용

### 버전 카탈로그(Version Catalog)

#### 왜 필요한가

여러 서브프로젝트에서 같은 라이브러리를 반복 선언하면 버전이 제각각 흩어지기 쉬움. 버전 카탈로그는 의존성의 좌표(coordinate)와 버전을 한 곳에서 중앙 관리하기 위한 기능.

- 서브프로젝트 간 공통 의존성 선언 공유
- 중복 선언·버전 불일치 방지
- 대규모 프로젝트에서 의존성/플러그인 버전 강제(enforce)

#### 구조

`gradle/libs.versions.toml` 파일에 다음 네 섹션을 정의.

- `[versions]`: 라이브러리·플러그인이 참조할 버전 번호 선언
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

이 파일을 프로젝트의 `gradle/` 디렉터리에 두면 Gradle이 자동으로 인식 → 빌드 스크립트에서는 `libs` 접근자로 참조 가능.

```kotlin
dependencies {
    implementation(libs.guava)
    api(libs.juneau.marshall)
}
```

- IntelliJ, Android Studio 등 IDE도 이 메타데이터를 인식해 코드 자동완성 지원
- 버전 문자열을 직접 하드코딩하지 않으므로, 버전을 올릴 때 `[versions]` 섹션 한 곳만 수정하면 됨

### 정리

- Gradle의 의존성 관리는 선언 → 해석(다운로드/캐싱) → 충돌 해결의 자동화된 파이프라인
- 의존성은 `dependencies {}` 블록에, 컨피규레이션(예: `implementation`, `api`)이라는 버킷을 통해 범위 결정
- `./gradlew :app:dependencies`로 실제 해석된 의존성 트리와 충돌·치환 여부 검사 가능
- 버전 카탈로그(`libs.versions.toml`)를 쓰면 버전과 좌표를 한 곳에서 관리 → 멀티 모듈 프로젝트의 일관성 향상

---

## Gradle 커맨드라인 인터페이스 기초

> **원문:** https://docs.gradle.org/current/userguide/command_line_interface_basics.html

### 개요
IDE를 사용하지 않을 때 Gradle과 상호작용하는 기본 수단은 커맨드라인. 태스크 실행, 빌드 정보 확인, 의존성 관리, 로그 레벨 조정 등을 모두 명령줄 옵션 조합으로 처리 가능.

### 기본 명령 구조
Gradle 명령은 다음과 같은 형태를 따름.

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
값이 필요한 옵션은 `=`로 값을 지정하는 것이 명확함.

```bash
gradle build --console=plain
```

#### on/off 토글 옵션
일부 옵션은 `--no-` 접두사를 붙여 반대 동작을 지정하는 짝을 가짐.

```bash
gradle build --build-cache      # 빌드 캐시 사용
gradle build --no-build-cache   # 빌드 캐시 미사용
```

#### 짧은 옵션 표기
자주 쓰는 옵션은 짧은 별칭 존재. 예를 들어 `-h`는 `--help`와 동일.

### 태스크 실행과 프로젝트 지정
멀티 프로젝트 빌드에서는 콜론(`:`)으로 프로젝트 경로를 표현 → 특정 하위 프로젝트의 태스크만 실행 가능.

```bash
gradle :test              # 루트 프로젝트의 test 태스크
gradle :app:test          # app 하위 프로젝트의 test 태스크
gradle test               # 현재 디렉터리를 기준으로 실행
```

### 태스크별 옵션
태스크 자체가 고유한 옵션을 가질 수 있으며, 태스크 이름 뒤에 붙여 전달.

```bash
gradle taskName --exampleOption=exampleValue
```

즉 커맨드라인 옵션은 두 종류로 구분됨.
- Gradle 실행 자체를 제어하는 전역 옵션 (`--build-cache`, `--console` 등)
- 특정 태스크의 동작만 바꾸는 태스크 전용 옵션

### Gradle Wrapper 사용 권장
문서는 `gradle` 명령을 직접 쓰기보다 Gradle Wrapper(`./gradlew`, Windows는 `gradlew.bat`) 사용을 강력히 권장. Wrapper를 쓰면 프로젝트에 지정된 Gradle 버전이 로컬 설치 여부와 무관하게 그대로 사용됨 → 팀원·CI 환경 간 빌드 결과가 달라지는 문제 방지.

```bash
./gradlew build       # macOS / Linux
gradlew.bat build     # Windows
```

### 핵심 포인트
- 명령 형식은 `gradle [태스크...] [--옵션...]`이며 옵션·태스크 순서는 자유
- 값이 있는 옵션은 `=`로, on/off 옵션은 `--no-` 짝으로 표현
- 멀티 프로젝트에서는 `:하위프로젝트:태스크` 형태로 실행 대상 축소 가능
- 태스크 전용 옵션과 Gradle 전역 옵션 구분 필요
- 실무에서는 `gradle` 대신 `./gradlew`(Wrapper) 사용이 표준
- 이 문서는 개요(basics) 수준이며, 전체 옵션 목록은 별도의 Command-Line Interface 레퍼런스 문서에서 다룸

---

## Gradle Wrapper 기본 개념

> **원문:** https://docs.gradle.org/current/userguide/gradle_wrapper_basics.html

### 개요
Gradle Wrapper는 Gradle 빌드를 실행할 때 권장되는 표준 방법. 로컬 환경에 Gradle이 미리 설치되어 있지 않아도, 프로젝트에 포함된 스크립트와 설정 파일만으로 지정된 버전의 Gradle을 자동으로 내려받아 실행 가능.

### Wrapper를 쓰는 이유
- 버전 자동 관리: 프로젝트에서 정한 Gradle 버전을 알아서 다운로드해 사용
- 팀 표준화: 팀원 전체가 항상 동일한 Gradle 버전으로 빌드 → "내 컴퓨터에서는 되는데" 같은 문제 감소
- 환경 일관성: 로컬 개발 환경, IDE, CI 서버 등 어디서 실행하든 같은 버전의 Gradle 사용
- 설치 부담 감소: 사용자가 Gradle을 별도로 설치할 필요 없음

### 핵심 포인트
- Wrapper는 Gradle 실행 파일 자체가 아니라, "지정된 버전의 Gradle을 필요하면 다운로드해서 실행해 주는" 얇은 래퍼
- 시스템에 설치된 `gradle` 명령을 직접 쓰는 것보다 `gradlew`/`gradlew.bat`를 쓰는 것이 항상 권장되는 방식

### Wrapper 구성 파일
Gradle 프로젝트에는 다음 네 가지 Wrapper 관련 파일이 존재함.

- `gradlew`: Unix 계열(Linux/macOS)에서 실행하는 셸 스크립트
- `gradlew.bat`: Windows에서 실행하는 배치 스크립트
- `gradle/wrapper/gradle-wrapper.jar`: 지정된 Gradle 버전을 다운로드·설치하는 로직이 담긴 작은 JAR
- `gradle/wrapper/gradle-wrapper.properties`: 다운로드할 Gradle 배포판의 URL, 배포 형식(zip/tarball) 등을 담은 설정 파일

디렉터리 구조는 대략 다음과 같음.

```
project-root/
├── gradlew
├── gradlew.bat
└── gradle/
    └── wrapper/
        ├── gradle-wrapper.jar
        └── gradle-wrapper.properties
```

이 파일들은 직접 수정하지 않는 것이 원칙. 버전을 바꾸고 싶다면 아래에 나오는 Wrapper 갱신 명령 사용 필요.

### 사용법

#### 빌드 실행
- Linux/macOS: `./gradlew build`
- Windows(PowerShell/cmd): `gradlew.bat build`

다른 디렉터리에서 실행해야 한다면 `gradlew` 스크립트까지의 상대 경로를 지정해서 호출하면 됨.

#### 버전 확인
```
./gradlew --version
```

#### 버전 업그레이드
```
./gradlew wrapper --gradle-version 7.2
```
이 명령을 실행하면 `gradle-wrapper.properties`의 배포판 URL이 갱신됨 → 이후 빌드부터는 새 버전이 다운로드되어 사용됨.

### Wrapper가 없는 경우
만약 저장소에 Wrapper 파일이 없다면 두 가지 경우 중 하나임.
1. 애초에 Gradle 프로젝트가 아님
2. Gradle 프로젝트이지만 Wrapper가 아직 생성되지 않음

이때는 Gradle이 이미 설치되어 있는 머신에서 `gradle wrapper` 명령을 실행해 Wrapper 파일들을 새로 생성하면 됨.

### 정리
- 로컬 `gradle` 명령 직접 실행과 `gradlew`(Wrapper) 실행은 다른 방식이며, 실무에서는 항상 Wrapper 사용 권장
- Wrapper 파일은 버전 관리 시스템에 커밋해서 팀 전체와 CI가 동일한 설정을 공유하도록 함
- 관련 문서로 CLI 기본 사용법, Build Scan, Gradle Daemon 등을 함께 참고

---

## Build Scan 기본 개념

> **원문:** https://docs.gradle.org/current/userguide/build_scans.html

### 개요
Build Scan은 Gradle 빌드를 실행하는 동안 수집되는 메타데이터를 시각화한 결과물. 빌드가 끝나면 Gradle이 실행 정보를 Build Scan Service로 전송 → 이 서비스가 원시 메타데이터를 사람이 분석하기 쉬운 웹 리포트 형태로 가공.

### 왜 필요한가
- 트러블슈팅: 빌드 실패나 성능 저하의 원인을 파악할 때, 수집된 데이터가 근거 자료가 됨
- 협업/질문: 커뮤니티나 동료에게 도움을 요청할 때 에러 메시지와 환경 정보를 일일이 복사해서 붙여넣을 필요 없이, Build Scan 링크 하나만 공유하면 됨
- 성능 분석: 태스크별 소요 시간, 캐시 적중 여부 등을 확인해 빌드 성능 개선에 활용 가능

### 핵심 포인트
- Build Scan은 "빌드가 실행되면서 어떤 일이 있었는지"를 기록한 스냅샷이며, 실행할 때마다 새로 생성됨
- 기본적으로 리포트는 공개 서비스인 `scans.gradle.com`에 업로드되며, 링크를 아는 사람은 누구나 접근 가능
- 조직 자체 Develocity 서버로 업로드 대상 변경 가능

### 활성화 방법

#### 커맨드라인에서 즉시 생성
Gradle 명령 뒤에 `--scan` 플래그만 붙이면 됨.

```
./gradlew build --scan
```

- 처음 사용할 때는 Build Scan 이용 약관에 동의하라는 프롬프트가 표시될 수 있음
- 빌드가 끝나면 콘솔에 리포트로 이동할 수 있는 URL이 출력됨

#### 자체 서버(Develocity)로 전송
공개 서비스가 아니라 조직에서 운영하는 Develocity 서버로 리포트를 보내고 싶다면, `--develocity-url` 옵션으로 대상 서버를 지정.

```
./gradlew build --develocity-url=https://develocity.example.com
```

- 지속적으로 특정 서버를 사용하려면 매번 옵션을 주는 대신 Develocity Gradle 플러그인을 통해 프로젝트 설정에 고정해 두는 방식 권장

### 어떤 정보가 담기는가
- Build Scan에는 실행 환경(OS, JVM 버전), 태스크 실행 결과, 소요 시간, 의존성 해석 과정, 콘솔 출력 등 빌드 관련 메타데이터가 포함됨
- 구체적으로 어떤 항목이 수집되는지, 그리고 데이터가 어떻게 보호되는지에 대한 세부 내용은 Gradle Develocity 플러그인 문서에서 별도로 다룸
- 민감한 정보가 포함될 수 있으므로, 공개 서비스에 업로드하기 전에는 어떤 데이터가 전송되는지 확인 필요

### 정리
- 빌드에 문제가 생기거나 성능을 점검하고 싶을 때는 `--scan` 플래그를 붙여 실행하는 것만으로 진단용 리포트 획득 가능
- 리포트는 기본적으로 공개 서비스(`scans.gradle.com`)에 저장되지만, `--develocity-url`로 자체 서버 지정 가능
- Build Scan 링크는 텍스트로 상황을 설명하는 것보다 훨씬 정확하고 빠르게 문제를 공유하는 방법 → 이슈 리포트나 팀 내 질문에 적극 활용 권장

---

## Gradle 캐싱 기초 (증분 빌드와 빌드 캐시)

> **원문:** https://docs.gradle.org/current/userguide/gradle_optimizations.html

### 개요

Gradle은 매번 모든 태스크를 처음부터 다시 실행하지 않음. 이전 실행 결과를 기억해두고, 바뀐 부분만 다시 계산하는 방식으로 빌드 속도를 크게 향상. 이를 가능하게 하는 두 가지 핵심 메커니즘이 증분 빌드(incremental build)와 빌드 캐시(build cache).

- 증분 빌드: 로컬에서 "이전 실행과 입력이 같으면 건너뜀"이라는 원리
- 빌드 캐시: "누군가(나 자신 또는 팀원, CI)가 이미 만들어둔 결과물을 재사용함"이라는 원리

두 기능 모두 태스크의 입력(input)과 출력(output)을 비교해서 동작 여부를 판단하기 때문에, 태스크가 입력/출력을 제대로 선언하고 있어야 효과를 볼 수 있음.

### 태스크 실행 결과 레이블

`--console=verbose` 옵션으로 빌드를 실행하면 각 태스크가 어떤 이유로 실행되었거나 건너뛰어졌는지 라벨로 확인 가능.

- (라벨 없음, 실행됨): 입력이 바뀌어 태스크가 실제로 수행됨
- `UP-TO-DATE`: 이전 실행과 입력·출력이 동일해서 실행을 건너뜀
- `FROM-CACHE`: 로컬 또는 원격 빌드 캐시에서 출력을 그대로 복원함
- `NO-SOURCE`: 처리할 소스 파일 자체가 없어서 건너뜀 (예: 컴파일할 `.java` 파일 없음)
- `SKIPPED`: `onlyIf` 조건이나 커맨드라인 옵션 등으로 인해 실행되지 않음
- `FAILED`: 태스크 실행 중 오류 발생

**핵심 포인트**
- `UP-TO-DATE`와 `FROM-CACHE`는 둘 다 "태스크를 다시 실행하지 않았음"이라는 뜻이지만, 전자는 증분 빌드, 후자는 빌드 캐시에 의한 결과라는 점이 다름
- 한 번 `FROM-CACHE`로 복원된 태스크는, 다음 빌드부터는 입력이 그대로라면 `UP-TO-DATE`로 표시됨. 즉 캐시 복원 이후에는 증분 빌드 판단이 다시 그 자리를 대체함

### 증분 빌드 (Incremental Build)

증분 빌드는 직전 빌드와 비교해 입력이 바뀌지 않은 태스크의 실행을 생략하는 기능. Gradle에서는 별도 설정 없이 기본적으로 항상 켜져 있음.

동작 원리는 단순함.

- 태스크마다 선언된 입력(소스 파일, 설정 값 등)과 출력(생성 파일 등)의 상태(주로 해시 값)를 기록
- 다음 빌드 때 현재 상태와 기록된 상태를 비교
- 값이 같으면 실행을 건너뛰고 `UP-TO-DATE`로 표시 → 다르면 태스크를 실제로 실행

실행 과정을 자세히 보려면 verbose 콘솔 모드를 사용.

```bash
$ ./gradlew compileJava --console=verbose
> Task :app:compileJava UP-TO-DATE
BUILD SUCCESSFUL in 374ms
```

매번 옵션을 입력하기 번거롭다면 `gradle.properties`에 다음처럼 고정 가능.

```properties
org.gradle.console=verbose
```

**핵심 포인트**
- 증분 빌드는 "로컬 이력"만 참고하는 최적화. 다른 브랜치나 다른 머신에서 만든 결과는 알지 못함
- 효과를 보려면 태스크 작성 시 입력/출력을 정확히 선언하는 것이 전제 조건

### 빌드 캐시 (Build Cache)

빌드 캐시는 한 단계 더 나아가, 이전에 실행했던(같은 입력을 가진) 태스크의 결과물을 저장해두고 필요할 때 재사용하는 기능. 증분 빌드가 "같은 워크스페이스, 같은 이력"을 가정하는 반면, 빌드 캐시는 브랜치가 다르거나 실행 주체(팀원, CI 서버)가 달라도 입력이 같으면 결과를 그대로 가져다 쓸 수 있다는 점이 다름.

전형적인 활용 시나리오는 다음과 같음.

- 같은 코드를 여러 브랜치에서 반복 빌드하는 경우
- 여러 팀원이 동일한 모듈을 각자 빌드하는 경우
- CI 파이프라인에서 동일한 커밋을 여러 번 빌드하는 경우

이런 상황에서는 이미 누군가 만들어 놓은 컴파일/테스트 결과를 그대로 재사용해 중복 작업을 제거.

빌드 캐시는 기본적으로 꺼져 있으며, `--build-cache` 옵션으로 활성화.

```bash
$ ./gradlew compileJava --build-cache
> Task :app:compileJava FROM-CACHE
BUILD SUCCESSFUL in 364ms
```

**핵심 포인트**
- 빌드 캐시가 적용된 태스크는 `FROM-CACHE`로 표시되며, 이는 "실행하지 않고 캐시에서 출력을 복원했음"이라는 의미
- 팀 전체나 CI 환경처럼 여러 실행 주체가 결과를 공유할수록 빌드 캐시의 효과 증가

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
  - 빌드 캐시: 팀원·CI 등 여러 실행 주체 간 공유 가능

결국 두 메커니즘은 서로 배타적이지 않음. 로컬에서는 증분 빌드로 빠르게 건너뛰고, 로컬 이력에 없는 경우에는 빌드 캐시로 다시 한번 재사용 기회를 찾는 식으로 함께 작동함.

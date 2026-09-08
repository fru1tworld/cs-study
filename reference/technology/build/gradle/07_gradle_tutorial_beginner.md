# 튜토리얼: 입문 (Beginner)

## Gradle 튜토리얼 1편: 프로젝트 초기화

> 원문: https://docs.gradle.org/current/userguide/part1_gradle_init.html

### 개요

간단한 Java 애플리케이션을 만들면서 태스크, 플러그인, 프로젝트 구조를 익힌다. 첫 단계에서는 프로젝트를 초기화하고 빌드한 뒤, 자동으로 생성된 파일이 어떤 역할을 하는지 살펴본다.

- `gradle init`으로 새 프로젝트 뼈대를 생성
- Wrapper(`gradlew`)로 프로젝트를 빌드
- Gradle의 표준 디렉터리 구조를 파악
- 자동 생성된 설정 파일들의 역할을 이해

### 사전 준비물

- Gradle이 로컬에 설치되어 있어야 함 (`gradle` 명령 실행 가능 여부로 확인)
- (선택) IntelliJ IDEA Community Edition: 프로젝트 구조를 시각적으로 확인할 때 사용

- 실습은 macOS 셸 기준이지만 Windows/Linux에서도 동일한 절차가 적용됨
  - IDE는 무엇을 쓰든 무관

### 프로젝트 생성 절차

#### 1) 작업 디렉터리 준비

```bash
$ mkdir tutorial
$ cd tutorial
```

#### 2) `gradle init`으로 스캐폴딩

```bash
$ gradle init --type java-application --dsl kotlin
```

- `--type java-application`: Java 애플리케이션 템플릿 지정
- `--dsl kotlin`: 빌드 스크립트를 Kotlin DSL로 작성 (Groovy를 쓰려면 `--dsl groovy`)
- 이후 대화형 질문이 이어지는데, 튜토리얼에서는 기본값을 그대로 선택해도 무방

### 생성된 프로젝트 구조

- `gradle init` 실행 후 아래와 비슷한 구조가 만들어짐

```text
tutorial/
├── .gradle/                # 프로젝트별 캐시 (자동 생성)
├── gradle/
│   ├── libs.versions.toml  # 버전 카탈로그
│   └── wrapper/            # Wrapper 설정과 JAR
├── gradlew                 # Wrapper 실행 스크립트 (Unix)
├── gradlew.bat             # Wrapper 실행 스크립트 (Windows)
├── settings.gradle.kts     # 루트 프로젝트 및 서브프로젝트 선언
└── app/                    # 애플리케이션 서브프로젝트
    ├── build.gradle.kts
    └── src/
```

- 각 요소의 역할

- `.gradle/`: Gradle이 빌드 성능을 위해 자동으로 만드는 프로젝트별 캐시 디렉터리, 직접 건드릴 일 없음
- `gradle/`: Wrapper가 사용할 특정 Gradle 버전의 설정과 JAR을 담음
- `libs.versions.toml`: 프로젝트 전체에서 쓰는 의존성 버전을 한곳에 모아 관리하는 버전 카탈로그
- `gradlew` / `gradlew.bat`: 시스템에 Gradle이 없어도 지정된 버전으로 빌드하는 실행 스크립트 → 버전 관리에 반드시 포함 필요
- `app/`: 실제 소스 코드와 해당 서브프로젝트만의 빌드 스크립트를 담는 서브프로젝트 디렉터리

### Wrapper로 빌드하기

Wrapper는 최초 실행 시 프로젝트에 지정된 Gradle 배포판을 내려받아 로컬 캐시에 저장한다. 팀원과 CI가 모두 이 버전을 사용하므로 Gradle 버전 불일치를 막을 수 있고, 각 시스템에 Gradle을 따로 설치할 필요도 없다. 실무에서 `gradle`보다 `./gradlew`로 빌드하는 이유다.

```bash
$ ./gradlew build      # macOS/Linux
$ gradlew.bat build    # Windows
```

- 처음 실행하면 지정된 버전의 Gradle 배포판(예: `gradle-8.13-bin.zip`)을 다운로드한 뒤 빌드 진행 → 성공 시 `BUILD SUCCESSFUL` 메시지와 실행된 태스크 수 출력
  - 빌드가 끝나면 `app/build/` 디렉터리에 컴파일 결과물 등 생성물이 쌓임

### 프로젝트 구조 이해: 빌드와 프로젝트

- 빌드(Build): 여러 컴포넌트를 함께 개발, 배포하기 위해 묶어놓은 소프트웨어 단위 전체
  - `tutorial`이 하나의 빌드
- 프로젝트(Project): 라이브러리, 애플리케이션, 플러그인 등 개별 구성 요소
  - 하나의 빌드 안에 여러 프로젝트(루트 + 서브프로젝트)가 존재 가능
- 이 예제에서는 `tutorial`이 루트 프로젝트, `app`이 그 안에 포함된 서브프로젝트

> 루트 디렉터리에는 별도의 `build.gradle(.kts)`를 두지 않는 것이 권장. 각 서브프로젝트가 자기 설정을 독립적으로 갖도록 하는 것이 Gradle의 일반적인 관례.

### 설정 파일 뜯어보기

`settings.gradle(.kts)`는 빌드에 포함할 프로젝트를 정의하고, `build.gradle(.kts)`는 각 프로젝트를 어떻게 빌드할지 정의한다. 서브프로젝트가 늘어나면 각자의 빌드 스크립트가 추가되지만, 설정 파일은 루트에 하나만 둔다.

#### `settings.gradle.kts` (루트)

```kotlin
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "tutorial"
include("app")
```

- `rootProject.name`으로 빌드 전체의 이름을 지정
- `include("app")`으로 `app` 디렉터리를 서브프로젝트로 등록 → 이 선언이 있어야 Gradle이 해당 폴더를 프로젝트로 인식

#### `app/build.gradle.kts` (서브프로젝트)

```kotlin
plugins {
    application
}

dependencies {
    implementation(libs.guava)
    testImplementation(libs.junit.jupiter)
}

application {
    mainClass = "org.example.App"
}
```

- `plugins { application }`: 실행 가능한 CLI 애플리케이션을 만들기 위한 플러그인 적용
- `repositories`: 의존성을 어디서 내려받을지 지정 (예: Maven Central)
- `dependencies`: 컴파일/테스트에 필요한 라이브러리 선언
- `java.toolchain`: 빌드에 사용할 JDK 버전 고정
- `application.mainClass`: 실행 시 진입점이 될 메인 클래스 지정
- `tasks.named<Test>("test")`: 테스트 실행 방식(예: JUnit Platform 사용) 커스터마이징

### (선택) IDE에서 열어보기

- IntelliJ IDEA를 쓴다면 `settings.gradle.kts` 파일을 더블클릭해서 열면 프로젝트 구조를 트리 형태로 확인 가능하고, 설정 파일에 문법 강조와 자동완성이 적용됨

## Gradle 초급 튜토리얼 2부 - 태스크 실행하기

> 원문: https://docs.gradle.org/current/userguide/part2_gradle_tasks.html

### 개요

1부에서 만든 Java 애플리케이션을 대상으로 사용 가능한 태스크를 조회한다. 이어서 태스크가 어디서 생겨나고 의존 관계가 실행 순서를 어떻게 결정하는지 살펴본 뒤, 터미널과 IDE에서 직접 실행해 본다.

### 사용 가능한 태스크 조회하기

- `tasks` 태스크를 실행하면 현재 프로젝트에서 쓸 수 있는 태스크 목록을 확인 가능

```bash
./gradlew tasks
```

- 출력은 Application, Build, Documentation, Other 등 카테고리별로 정리되어 나옴
- 멀티 프로젝트 구성에서는 특정 서브프로젝트만 지정해 조회 가능 (예: `./gradlew :app:tasks`)
- 기본 출력에는 사용자에게 노출할 만한 태스크만 표시
  - 내부용/라이프사이클 태스크까지 모두 보려면 `--all` 옵션 사용

```bash
./gradlew tasks --all
```

### 태스크는 어디서 오는가

- 태스크는 세 가지 경로로 프로젝트에 추가됨

- Gradle 기본 제공: 별도 설정 없이도 존재하는 태스크
- 적용된 플러그인: 예를 들어 `application` 플러그인을 적용하면 `run`, `build`, `compileJava`, `test`, `jar` 등의 태스크가 자동으로 생김
- 사용자 정의: 빌드 스크립트에서 직접 등록한 태스크

- 사용자 정의 태스크는 `tasks.register<타입>("이름") { ... }` 형태로 등록
  - 예를 들어 특정 디렉터리의 `.war` 파일을 다른 곳으로 복사하는 태스크는 다음처럼 작성 가능

```kotlin
tasks.register<Copy>("copyTask") {
    from("source")
    into("target")
    include("*.war")
}
```

- Gradle이 기본으로 제공하는 대표적인 태스크 타입

- `Copy`: 파일을 한 위치에서 다른 위치로 복사
- `Delete`: 파일/디렉터리 삭제
- `Exec`: OS 명령 실행
- `Zip`: 파일을 압축 아카이브로 묶음

### 태스크 간 의존성

- 대부분의 태스크는 독립적으로 실행되지 않고, 다른 태스크가 먼저 끝나야 의미가 있음
  - Gradle은 이 순서를 두 가지 방식으로 파악

- 명시적 의존성: `dependsOn()`으로 "이 태스크는 저 태스크 다음에 실행되어야 한다"고 직접 선언
- 암묵적 의존성: 한 태스크의 출력이 다른 태스크의 입력으로 쓰이는 관계를 Gradle이 자동으로 감지해 순서를 정함

- 명시적 의존성 예시

```kotlin
tasks.register("hello") {
    doLast { println("Hello!") }
}
tasks.register("greet") {
    doLast { println("How are you?") }
    dependsOn("hello")
}
```

- `greet`를 실행하면 `dependsOn("hello")` 때문에 `hello`가 먼저 실행되어 `Hello! How are you?` 순서로 출력됨
  - 개발자가 실행 순서를 일일이 지정하지 않아도 Gradle이 의존성 그래프를 계산해 올바른 순서로 처리

### IDE에서 태스크 확인, 실행하기 (선택)

- IntelliJ IDEA 같은 IDE를 쓴다면 화면 우측의 Gradle 패널에서 프로젝트의 태스크를 카테고리별로 시각적으로 확인 가능
  - 목록에서 원하는 태스크를 더블클릭하면 바로 실행되고, 콘솔 창에서 진행 상황과 성공 여부 확인 가능
  - 예를 들어 `build` 태스크를 더블클릭하면 컴파일부터 테스트, 패키징까지 이어지는 전체 과정이 순서대로 실행됨

### 터미널에서 태스크 실행하기

- Gradle Wrapper로 원하는 태스크를 직접 실행 가능

```bash
./gradlew build
./gradlew jar
./gradlew run
```

- `build`는 컴파일, 리소스 처리, 테스트, 검증 등 여러 하위 태스크를 순서대로 실행하는 라이프사이클 태스크
- `jar`는 실행 가능한 JAR 파일을 `app/build/libs/` 아래에 만듦
- `run`은 애플리케이션을 실제로 구동 → 이 예제에서는 `Hello World!` 출력 확인 가능

- 이 과정에서 `compileJava → jar → build` 같은 순서로 태스크가 자동 실행되는 것을 로그에서 직접 확인 가능 → 앞서 설명한 태스크 의존성이 실제로 동작하는 모습

## Gradle 튜토리얼 Part 3: 의존성 이해하기

> 원문: https://docs.gradle.org/current/userguide/part3_gradle_dep_man.html

### 개요

프로젝트 초기화와 태스크 실행을 마쳤으니, 이제 외부 라이브러리를 가져오고 관리하는 과정을 살펴본다. 버전 카탈로그에 의존성을 선언한 뒤 좌표와 전이 의존성을 이해하고, 의존성 트리와 Build Scan으로 실제 구성을 확인한다. 버전을 바꾼 뒤에도 같은 도구로 결과를 비교할 수 있다.

### 버전 카탈로그부터 살펴보기

프로젝트를 초기화할 때 생성된 `gradle/libs.versions.toml`에 라이브러리 버전을 모아 둔다. 여러 서브프로젝트가 같은 정의를 참조하므로 빌드 스크립트마다 버전이 흩어지는 문제를 막을 수 있다. IntelliJ 같은 IDE는 카탈로그 항목을 인식해 `libs.xxx` 형태로 타입 안전한 자동완성을 제공한다.

```toml
[versions]
guava = "33.3.1-jre"

[libraries]
guava = { module = "com.google.guava:guava", version.ref = "guava" }
```

- 빌드 스크립트에서는 문자열 좌표 대신 `libs` 접근자로 참조

```kotlin
dependencies {
    implementation(libs.guava)
}
```

`dependencies {}`에는 외부 저장소에서 내려받는 라이브러리뿐 아니라 같은 빌드의 다른 서브프로젝트나 로컬 JAR 파일도 선언할 수 있다. 의존성이 필요한 범위는 컨피규레이션으로 구분한다. 예를 들어 `implementation`은 컴파일과 런타임에 필요한 의존성에, `testImplementation`은 테스트 코드에만 필요한 의존성에 사용한다.

### 좌표(GAV)로 라이브러리를 식별하기

- 리포지토리에 있는 라이브러리 하나를 정확히 가리키려면 세 가지 정보가 필요
  - 이를 줄여서 GAV라 부름

- Group: 배포 주체(조직). 예: `com.google.guava`
- Artifact: 라이브러리 이름. 예: `guava`
- Version: 배포 버전. 예: `33.3.1-jre`

- 리포지토리는 이 좌표를 실제 아티팩트(jar 등)로 바꿔 주는 저장소 역할
  - Java 생태계에서는 Maven Central이 사실상 표준 리포지토리로 쓰임

### 전이 의존성: 딸려 오는 라이브러리들

선언한 라이브러리가 다른 라이브러리를 필요로 하면 Gradle은 그 의존성까지 함께 가져온다. 이렇게 간접적으로 포함되는 라이브러리를 전이 의존성(transitive dependency)이라 한다. 예를 들어 Guava를 선언하면 내부적으로 필요한 `failureaccess`도 따로 선언하지 않고 내려받을 수 있다.

개발자는 직접 필요한 라이브러리를 선언하고, 나머지 의존 관계 해석은 Gradle에 맡긴다. 다만 프로젝트가 커지면 전이 의존성이 서로 다른 버전을 요구하며 충돌할 수 있으므로, 실제로 어떤 라이브러리가 포함됐는지 확인해야 한다.

### 의존성 트리로 실제 구성 확인하기

- `dependencies` 태스크를 실행하면 컴파일/런타임 클래스패스별로 어떤 라이브러리가 어떤 경로로 들어왔는지 트리 형태로 확인 가능

```bash
./gradlew :app:dependencies
```

- 직접 선언한 의존성과 그로부터 파생된 전이 의존성이 계층 구조(트리)로 함께 출력됨
- 컨피규레이션(예: `compileClasspath`, `runtimeClasspath`)별로 결과가 나뉘어, "이 라이브러리가 왜 여기 들어와 있는지" 추적 가능

### Build Scan으로 한눈에 시각화하기

- 터미널 출력만으로 구조를 파악하기 어렵다면 Build Scan을 활용 가능

```bash
./gradlew build --scan
```

- 최초 실행 시 Gradle 이용약관 동의와 이메일 인증 절차를 거쳐야 상세 리포트에 접근 가능
- 결과는 웹 페이지 형태로 제공되며, 의존성 그래프를 컨피규레이션별로 펼쳐 보거나 특정 라이브러리가 어느 경로를 통해 들어왔는지 시각적으로 추적 가능
- 팀원과 링크를 공유해 같은 화면을 보며 의존성 문제를 논의하기에도 유용

### 버전을 바꾸고 다시 확인하기

- 의존성 버전을 올리거나 내릴 때는 빌드 스크립트가 아니라 버전 카탈로그(`libs.versions.toml`)의 버전 값을 수정

- 카탈로그 한 곳만 고치면 그 버전을 참조하는 모든 서브프로젝트/모듈에 동일하게 반영됨
- IntelliJ 같은 IDE에는 변경 후 프로젝트 인덱스를 다시 동기화하는 버튼이 있어, 자동완성 정보가 최신 버전 기준으로 갱신됨
- 버전을 바꾼 뒤 다시 `dependencies` 태스크나 Build Scan을 돌려 보면, 트리 구조가 새 버전을 기준으로 어떻게 달라졌는지 비교 가능

- 마지막으로 `./gradlew run`을 실행해 애플리케이션이 정상 동작하면, 의존성 선언, 해석, 컴파일까지 문제없이 끝났다는 의미

## Gradle 튜토리얼 Part 4 - 플러그인 적용하기

> 원문: https://docs.gradle.org/current/userguide/part4_gradle_plugins.html

### 개요

- 이번 파트에서는 앞서(Part 1~3) 초기화, 태스크 실행, 의존성 관리를 다룬 자바 애플리케이션에 Maven Publish 플러그인을 새로 적용해 보면서, 플러그인이 실제로 빌드에 무엇을 추가해 주는지 손으로 확인함

### 플러그인이란

- Gradle 공식 문서는 플러그인을 "빌드 로직을 조직화하고 프로젝트 내에서 재사용하는 가장 기본적인 방법"으로 설명함
  - 플러그인을 적용하면 아래와 같은 것들이 한꺼번에 빌드에 편입됨

- 태스크 추가: 컴파일, 테스트 등 실행 가능한 작업 단위
- 모델 확장: 새로운 DSL 요소(`publishing { }` 같은 설정 블록) 도입
- 컨벤션 적용: 프로젝트에 합리적인 기본값과 표준을 부여
- 타입 확장: 기존 클래스에 새 속성/메서드 부여
- 설정 관리: 레포지토리, 조직 표준 등을 일괄 적용

### 플러그인 적용해 보기: Maven Publish

- 기존 프로젝트는 `application` 플러그인만 적용되어 있었음
  - 여기에 로컬/원격 Maven 저장소로 아티팩트를 배포하는 `maven-publish` 플러그인을 추가함

```kotlin
plugins {
    application
    id("maven-publish")
}
```

- 플러그인을 추가하는 것만으로 `publish`, `publishToMavenLocal` 같은 새 태스크가 프로젝트에 생겨남
  - 이는 앞서 학습한 "플러그인은 코어(내장)/커뮤니티(Plugin Portal)/커스텀(자체 제작) 세 부류로 나뉜다"는 분류에서 `maven-publish`가 코어 플러그인에 해당하는 예

### 플러그인 설정하기

- 플러그인을 적용했다고 끝이 아니라, 대부분은 별도의 설정 블록을 통해 동작을 구체화해야 함
  - Maven Publish의 경우 `publishing { }` 블록에서 배포될 아티팩트의 좌표(groupId/artifactId/version)와 어떤 컴포넌트를 배포할지 지정

```kotlin
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.gradle.tutorial"
            artifactId = "tutorial"
            version = "1.0"
            from(components["java"])
        }
    }
}
```

이 설정을 추가하면 `generatePomFileForMavenPublication`처럼 지정한 배포 대상을 처리하는 태스크가 더 생성된다. 플러그인을 적용해 기본 태스크를 만든 뒤, 설정 블록에서 작업 대상을 지정하며 태스크를 구체화하는 흐름이다.

### 플러그인 사용해 보기

- 설정을 마친 뒤 `./gradlew :app:publishToMavenLocal`을 실행하면, 컴파일, 리소스 처리, jar 생성을 거쳐 POM/메타데이터 파일을 만들고 로컬 Maven 저장소(`~/.m2`)에 게시하는 태스크 체인이 순서대로 실행됨
  - 생성된 POM 파일에는 앞서 지정한 groupId, artifactId, version이 그대로 반영됨

### 플러그인 생태계 정리

- 코어 플러그인: Gradle에 기본 내장
  - `id("java")`처럼 짧은 이름만으로 적용, 버전 지정 불필요
- 커뮤니티 플러그인: Gradle Plugin Portal에 배포된 서드파티 플러그인
  - `id("com.diffplug.spotless").version("6.25.0")`처럼 ID와 버전을 함께 지정
- 커스텀 플러그인: 조직/팀이 직접 작성한 플러그인
  - 여러 서브프로젝트가 빌드 로직을 공유해야 할 때는 "컨벤션 플러그인" 형태로 만들어 중복을 없애는 것이 정석

## Gradle 튜토리얼 5편: 증분 빌드 살펴보기

> 원문: https://docs.gradle.org/current/userguide/part5_gradle_inc_builds.html

### 개요

같은 프로젝트를 다시 빌드할 때 Gradle이 어떤 작업을 건너뛰는지 살펴본다. `gradle.properties`로 콘솔 출력을 설정하고, 태스크 옆에 표시되는 라벨을 읽으며 증분 빌드의 동작을 확인한다.

### 증분 빌드란

- 증분 빌드는 "직전 빌드 이후 입력이 바뀌지 않은 태스크는 다시 실행할 필요가 없으니 건너뛴다"는 최적화
  - 별도로 켜는 옵션이 아니라 Gradle의 기본 동작이며, 대신 전제 조건이 있음

- 태스크가 자신의 입력(input)과 출력(output)을 명확히 선언하고 있어야 함
- Gradle은 매 실행마다 입력/출력 상태를 기록해두고, 다음 실행 때 이전 기록과 비교
- 비교 결과가 같으면 실행을 생략하고, 다르면 실제로 태스크를 수행

- 즉 "코드를 안 건드렸으면 컴파일도, 테스트도 다시 할 필요 없다"는 상식적인 판단을 Gradle이 태스크 단위로 자동화해주는 것

### 실행 과정 자세히 보기: `--console=verbose`

- 기본 콘솔 출력은 태스크 실행 여부를 요약해서 보여주지 않음
  - 어떤 태스크가 왜 건너뛰어졌는지 확인하려면 verbose 모드 필요
  - 매번 옵션을 붙이는 대신 프로젝트 루트의 `gradle.properties`에 아래처럼 고정해두면 편함

```properties
org.gradle.console=verbose
```

- 이렇게 설정한 뒤 빌드를 돌리면, 각 태스크 옆에 실행 결과 라벨이 함께 출력됨

### 실습으로 확인하기

- 1) 클린 빌드 (처음 실행)

```bash
$ ./gradlew :app:clean :app:build
```

- 캐시나 이전 산출물이 전혀 없는 상태이므로, 모든 태스크(예: 8개)가 라벨 없이 실제로 실행됨

- 2) 같은 명령을 한 번 더 실행

```bash
$ ./gradlew :app:clean :app:build
```

- `clean`이 들어있긴 하지만, 소스 코드나 설정에는 변화가 없으므로 대부분의 태스크가 `UP-TO-DATE` 라벨을 달고 실행을 건너뜀
  - 결과적으로 빌드 소요 시간이 수 초에서 수십~수백 밀리초 수준으로 확 줄어듦
  - 이 차이가 바로 증분 빌드의 효과

### 태스크 실행 결과 라벨

- (라벨 없음): 입력이 바뀌어 태스크가 실제로 새로 실행됨
- `UP-TO-DATE`: 직전 실행과 입력, 출력이 동일해서 실행을 건너뜀 (증분 빌드)
- `SKIPPED`: 조건이나 옵션에 의해 의도적으로 실행이 제외됨
- `FROM-CACHE`: 로컬 재실행이 아니라 빌드 캐시에서 결과물을 그대로 가져옴
- `NO-SOURCE`: 처리할 입력 자체가 없어서 실행되지 않음

verbose 콘솔은 실행 정보를 자세히 보여주는 설정이므로 빌드 결과 자체를 바꾸지는 않는다.

## Gradle 튜토리얼 6장 - 빌드 캐시 활성화하기

> 원문: https://docs.gradle.org/current/userguide/part6_gradle_caching.html

### 증분 빌드와 빌드 캐시

- 증분 빌드는 "같은 워크스페이스에서 방금 전 실행"과 비교해 변경이 없으면 태스크를 건너뛰는 최적화였음
  - 하지만 브랜치를 전환하거나 다른 사람이 같은 코드를 빌드하는 상황에서는 그 이력이 무의미해져 매번 처음부터 다시 빌드해야 함



### 왜 캐시가 필요한가

- 증분 빌드는 "직전 실행"만 기억함
  - 그래서 다음과 같은 상황에서는 무력함

- 브랜치를 A에서 B로 옮긴 뒤 다시 A로 돌아왔을 때
- 동료가 이미 컴파일해본 소스를 내 로컬에서 다시 컴파일할 때
- CI가 같은 커밋을 여러 파이프라인에서 반복 빌드할 때

- 이런 경우에도 태스크의 입력이 동일하다면 출력도 동일하다는 사실은 변하지 않음
  - 빌드 캐시는 이 점을 이용해, 한 번 만든 결과물을 어딘가에 저장해두고 입력이 같은 태스크를 만나면 실행 대신 결과물을 그대로 복사해옴

### 로컬 빌드 캐시 켜기

- 빌드 캐시는 기본적으로 꺼져 있음
  - `gradle.properties`에 한 줄만 추가하면 프로젝트 전역에서 활성화 가능

```properties
org.gradle.caching=true
```

- 옵션 없이 매번 켜고 끄고 싶다면 커맨드라인 플래그로도 지정 가능

```bash
./gradlew :app:build --build-cache
```

### 캐시 재사용 확인하기 (브랜치 전환 흉내)

- 캐시 효과를 눈으로 보려면 먼저 정상적으로 한 번 빌드하고, 그다음 `clean`으로 빌드 결과물만 지운 뒤 다시 빌드해봄
  - 이는 "다른 브랜치로 갔다가 돌아온" 상황과 유사

```bash
./gradlew :app:build          # 1차 빌드 - 태스크 실행, 캐시에 결과 저장
./gradlew :app:clean :app:build   # 출력물만 삭제 후 재빌드
```

- 두 번째 빌드의 결과 라벨을 보면 캐시가 실제로 무엇을 했는지 구분 가능

- `UP-TO-DATE`: 로컬 실행 이력이 남아있어 증분 빌드가 건너뜀
- `FROM-CACHE`: `clean`으로 이력은 지워졌지만, 빌드 캐시에 저장된 출력물을 그대로 복사해옴

- 즉 `clean`은 로컬의 "실행 이력과 출력 파일"만 지울 뿐, 별도로 보관된 캐시 저장소는 건드리지 않음
  - 그래서 `FROM-CACHE`가 나타나는 것은 "태스크를 다시 안 돌리고 캐시에서 결과만 복사했다"는 신호

### 로컬 캐시는 어디에 저장되나

- 로컬 빌드 캐시는 사용자 홈 디렉터리 아래 Gradle 캐시 폴더에 저장됨

- Windows: `%USERPROFILE%\.gradle\caches`
- macOS/Linux: `~/.gradle/caches/`

- 이 저장소는 무한정 커지지 않음
  - Gradle이 오래 쓰이지 않은 항목을 자동으로 정리해 크기를 관리함

### 원격 빌드 캐시 - 팀/CI 간 공유

로컬 캐시의 결과물은 같은 컴퓨터에서만 재사용할 수 있다. 여러 개발자와 CI 서버가 이 결과물을 공유하려면 원격 빌드 캐시를 연결해야 한다. 두 캐시를 함께 사용하면 다음 순서로 결과물을 찾는다.

- 캐시를 찾는 순서

- 1\. 로컬 캐시를 먼저 확인
- 2\. 로컬에 없으면 원격 캐시에서 다운로드를 시도
- 3\. 어디에도 없으면 태스크를 실제로 실행

- Gradle은 테스트/학습용으로 무료 Docker 이미지를 제공하며, 실제 팀/프로덕션 환경에서는 Gradle에서 만든 상용 솔루션인 Develocity를 통한 원격 캐시 구축을 권장

원격 캐시는 누군가 이미 빌드한 입력을 다른 개발자나 CI가 다시 만날 때 효과를 낸다. 팀 규모가 크고 빌드가 잦을수록 결과물을 공유할 기회도 많아진다.

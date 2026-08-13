# 플러그인 사용과 빌드 구조화

## 플러그인 다루기 (Working with Plugins)

> 원문: https://docs.gradle.org/current/userguide/plugins_intermediate.html

### 개요

- 이 문서는 `plugin_basics.md`에서 다룬 "플러그인이란 무엇인가"를 넘어, 플러그인을 어떻게 배포하고 · 어떤 방법으로 적용하고 · 버전과 저장소를 어떻게 중앙에서 관리하는지를 다룸
- 핵심은 `plugins { }` 블록이 표준이라는 점 → 그 외의 적용 방식(서브프로젝트 일괄 적용, `buildscript`, 레거시 `apply()`)은 대부분 레거시이거나 특수한 상황에서만 쓰임

### 플러그인 적용 방법 6가지

- Gradle은 플러그인을 적용하는 여러 방법을 제공하나, 권장도는 방법마다 크게 다름

1. `plugins { }` 블록 (권장, 표준)
   - 각 프로젝트의 빌드 스크립트에서 플러그인 ID(와 필요 시 버전)를 선언
   - 문법이 간결 · 타입 세이프 접근자 제공 · Gradle이 적용 전에 미리 분석해 클래스패스를 최적화 가능

   ```kotlin
   plugins {
       id("org.springframework.boot") version "3.3.1"
   }
   ```

2. 루트 프로젝트에서 서브프로젝트에 일괄 적용
   - 루트의 `plugins { }`에 `apply false`로 선언해두고, 각 서브프로젝트 스크립트에서 버전 없이 `id(...)`로 다시 적용하는 방식
   - 과거 멀티 프로젝트 빌드에서 흔했으나 더는 권장되지 않음

3. 루트에서 선언만 하고 적용은 서브프로젝트가 선택
   - 2번과 메커니즘은 같으나 목적이 다름
   - 모든 서브프로젝트가 강제로 같은 플러그인을 쓰게 하는 것이 아니라, 버전만 한 곳에서 통일해두고 필요한 서브프로젝트만 골라서 적용하게 하는 용도

4. `buildSrc`의 컨벤션 플러그인 (멀티 프로젝트 권장안)
   - 공유할 빌드 로직을 `buildSrc`에 플러그인 형태로 만들어두고, 각 서브프로젝트에서는 버전 없이 ID만으로 적용
   - 2·3번 방식보다 유지보수성이 좋아 멀티 프로젝트 빌드의 사실상 표준으로 자리잡음

5. `buildscript { }` 블록 (레거시)
   - 빌드 스크립트 자체가 실행되기 위해 필요한 의존성이나, Plugin Portal에 없는 커스텀/사설 저장소의 플러그인을 가져올 때 쓰던 방식
   - `plugins { }` 블록이 등장한 이후로는 사용 빈도가 크게 줄음

   ```kotlin
   buildscript {
       repositories { /* ... */ }
       dependencies { classpath("...") }
   }
   apply(plugin = "...")
   ```

6. 레거시 `apply()` 함수
   - 플러그인 ID 대신 클래스를 직접 참조해 적용하는 방식(`apply<MyPlugin>()`)
   - 스크립트 플러그인을 임시로 붙일 때나 쓰이며 사실상 사용을 지양해야 함

### `plugins { }` DSL의 제약

- `plugins { }` 블록이 최적화와 도구 지원(에디터 자동완성 등)을 제공할 수 있는 이유는 문법이 엄격하게 제한되어 있기 때문
  - 플러그인 ID와 버전은 리터럴 상수 문자열이어야 하며, 변수 조합이나 조건식으로 동적으로 만들 수 없음
  - `if`문이나 반복문 안에 넣을 수 없음
  - 블록 실행은 부작용이 없고(idempotent) 반복 실행해도 같은 결과를 내야 함
  - `build.gradle(.kts)`와 `settings.gradle(.kts)`에서만 쓸 수 있고, 스크립트 플러그인이나 init 스크립트에서는 사용 불가
- 즉 "동적으로 플러그인을 고르고 싶다"는 요구는 `plugins { }` 블록이 아니라 `pluginManagement`의 리졸루션 규칙이나 `buildscript` 같은 다른 메커니즘으로 풀어야 함

### 플러그인 관리(`pluginManagement`)

- `settings.gradle(.kts)`의 가장 첫 블록으로 위치 → 빌드 전체에서 플러그인을 어떻게 찾고 어떤 버전을 쓸지 한 곳에서 통제
- 내부에 세 가지 하위 블록을 둘 수 있음

- `repositories { }`
  - 플러그인을 어디서 내려받을지 지정
  - 기본값은 Gradle Plugin Portal 하나뿐이며, 사설 Maven/Ivy 저장소를 추가하면 선언한 순서대로 검색

  ```kotlin
  pluginManagement {
      repositories {
          maven(url = file("./maven-repo"))
          gradlePluginPortal()
          ivy(url = file("./ivy-repo"))
      }
  }
  ```

- `plugins { }`
  - 여기서는 빌드 스크립트의 `plugins { }`와 달리 문법 제약이 없어서, `gradle.properties` 값을 읽어오거나 Provider를 활용해 버전을 동적으로 정할 수 있음
  - 버전을 이 블록에서 한 번 정의해두면, 각 서브프로젝트의 `plugins { }`에서는 버전 없이 ID만 쓰면 됨
  - 즉 버전을 한 곳(single source of truth)에서 관리하는 것이 핵심 목적

- `resolutionStrategy { eachPlugin { ... } }`
  - 요청된 플러그인 ID를 가로채 실제 적용할 아티팩트 좌표를 바꿔치기 가능
  - 예: 특정 네임스페이스로 시작하는 ID를 사내 저장소의 특정 모듈로 강제 매핑

  ```kotlin
  resolutionStrategy {
      eachPlugin {
          if (requested.id.namespace == "com.example") {
              useModule("com.example:sample-plugins:1.0.0")
          }
      }
  }
  ```

### 플러그인 마커 아티팩트

- Gradle이 `plugins { id("com.example.foo") }`처럼 ID만 보고 실제 구현 jar를 찾아낼 수 있는 이유는 마커(marker) 아티팩트 덕분
- 저장소에는 `plugin.id:plugin.id.gradle.plugin:plugin.version`라는 이름의 자그마한 마커 모듈이 별도로 존재 → 이 마커가 실제 플러그인 구현체를 가리킴
- `java-gradle-plugin`으로 플러그인을 게시하면 이 마커도 함께 자동 생성 → 플러그인 작성자가 직접 신경 쓸 일은 거의 없음

### 버전 카탈로그와 함께 쓰기

- `libs.versions.toml` 같은 버전 카탈로그에 플러그인 좌표를 등록해두면, 빌드 스크립트에서는 다음처럼 별칭(alias)으로 적용 가능

```kotlin
plugins {
    alias(libs.plugins.pluginName)
}
```

- 의존성 카탈로그와 동일한 인프라를 재사용하기 때문에 타입 세이프 접근자가 생성되고, 여러 모듈에 걸쳐 플러그인 버전을 일관되게 유지하기 좋음

### 핵심 포인트

- 플러그인은 코어/커뮤니티/커스텀 세 출처가 있고, 적용 방법은 그와 별개로 6가지가 존재하나 실무에서 쓸 만한 것은 사실상 `plugins { }` 블록과 `buildSrc` 컨벤션 플러그인 두 가지
- `plugins { }` DSL은 리터럴 문자열·비조건문·부작용 없음이라는 제약을 지키는 대가로 Gradle의 최적화와 IDE 지원을 얻음
- 멀티 프로젝트에서 플러그인을 서브프로젝트마다 반복 적용하던 옛 방식(루트 `apply false` + 서브프로젝트 재적용)은 `buildSrc` 컨벤션 플러그인으로 대체하는 것이 권장됨
- 플러그인 버전과 저장소는 빌드 스크립트가 아니라 `settings.gradle(.kts)`의 `pluginManagement`에서 중앙 관리하는 것이 최신 관례
- `buildscript { }`와 레거시 `apply()`는 사설 저장소 대응 등 특수한 경우를 위해 남아있는 하위 호환 수단일 뿐, 새 코드에서는 지양

---

## Gradle 의존성 선언 심화

> 원문: https://docs.gradle.org/current/userguide/dependencies_intermediate.html

### 개요

- 기초 단계에서는 `implementation`, `api` 같은 컨피규레이션에 라이브러리를 선언하는 방법을 다룸
- 이 문서는 한 단계 더 들어가서 다음 네 가지를 정리
  - 의존성 표기법 중 어떤 방식을 써야 하는지
  - 버전 카탈로그로 여러 모듈의 버전을 한곳에서 관리하는 방법
  - 버전을 강제(enforce)하거나 제약(constrain)하는 방법
  - 동일 기능을 제공하는 라이브러리가 충돌할 때 캐퍼빌리티(capability)로 해결하는 방법

### 의존성 표기법: 문자열 vs 맵

- `dependencies {}` 블록 안에서 라이브러리를 지정하는 방식은 두 가지가 있으나, 권장되는 것은 하나뿐

```kotlin
dependencies {
    implementation("com.google.guava:guava:30.0-jre")
    runtimeOnly("org.apache.commons:commons-lang3:3.14.0")
}
```

- 위처럼 `"group:name:version"` 형태의 단일 문자열 표기법이 표준
- `group`, `name`, `version` 키를 나열하는 맵 표기법은 Gradle 9.1.0부터 지원 중단(deprecated) → Gradle 10부터는 빌드가 실패
- 어떤 컨피규레이션(`implementation`, `runtimeOnly`, `testImplementation` 등)을 쓸 수 있는지는 적용된 플러그인(java-library, Android, Kotlin Multiplatform 등)에 따라 달라짐

#### 핵심 포인트

- 맵 표기법으로 작성된 레거시 빌드 스크립트가 있다면 Gradle 10 이전에 단일 문자열 표기법으로 옮겨야 마이그레이션 리스크가 없음

### 버전 카탈로그로 버전 중앙화하기

- 여러 서브프로젝트가 같은 라이브러리를 각자 다른 버전으로 선언하는 문제를 막기 위해, `gradle/libs.versions.toml` 파일 하나에 버전과 좌표를 모아둠

```toml
[versions]
guava = "33.3.1-jre"
junit-jupiter = "5.11.3"

[libraries]
guava = { module = "com.google.guava:guava", version.ref = "guava" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit-jupiter" }
```

- 빌드 스크립트에서는 `libs` 접근자로 참조

```kotlin
dependencies {
    implementation(libs.guava)
    testImplementation(libs.junit.jupiter)
}
```

- 버전을 올릴 때 TOML 파일 한 곳만 고치면 모든 서브프로젝트에 반영됨
- IDE가 카탈로그 메타데이터를 인식해 자동완성을 지원함

### 버전 강제와 제약

- 전이 의존성이 예상치 못한 버전을 끌고 오는 경우, 두 가지 방법으로 버전을 통제 가능

#### 1) 의존성 선언에 직접 제약을 붙이는 방법

```kotlin
dependencies {
    implementation("org.apache.httpcomponents:httpclient:4.5.4")
    implementation("commons-codec:commons-codec") {
        version { strictly("1.9") }
    }
}
```

- `strictly()`는 해당 버전 범위를 벗어나는 어떤 치환도 허용하지 않도록 강제

#### 2) `constraints {}` 블록으로 별도 선언하는 방법

```kotlin
dependencies {
    implementation("org.apache.httpcomponents:httpclient")
    constraints {
        implementation("org.apache.httpcomponents:httpclient:4.5.3") {
            because("이전 버전에 이 애플리케이션에 영향을 주는 버그가 있음")
        }
        implementation("commons-codec:commons-codec:1.11") {
            because("httpclient가 끌어오는 1.9 버전에 버그가 있음")
        }
    }
}
```

#### 핵심 포인트

- `constraints {}` 블록은 의존성 자체를 추가하지 않고 버전 범위만 좁히는 용도 → 실제 의존성 선언과 제약 이유를 분리해 관리 가능
- `because()`로 왜 그 버전을 강제했는지 남겨두면, 나중에 다른 개발자가 제약을 지우거나 바꿔도 되는지 판단하기 쉬워짐

### 캐퍼빌리티로 중복 기능 라이브러리 충돌 해결

- 서로 다른 좌표를 가진 두 라이브러리가 같은 기능(예: XPath 처리)을 제공하면, Gradle은 이를 별개의 의존성으로 인식해 둘 다 클래스패스에 올림 → 런타임 충돌 가능

```kotlin
dependencies {
    implementation("jaxen:jaxen:1.1.6")     // XPath 기능 제공
    implementation("org.jdom:jdom2:2.0.6")  // 동일한 XPath 기능 제공
}
```

- 해결 방법은 두 라이브러리에 같은 캐퍼빌리티를 부여해 "둘 중 하나만 선택되어야 하는 대체 관계"임을 Gradle에 알려주는 것

```kotlin
dependencies {
    components {
        withModule("jaxen:jaxen") {
            allVariants { withCapabilities { addCapability("xml", "xpath-support", "1.0") } }
        }
        withModule("org.jdom:jdom2") {
            allVariants { withCapabilities { addCapability("xml", "xpath-support", "1.0") } }
        }
    }
    implementation("jaxen:jaxen:1.1.6") {
        capabilities { requireCapability("xml:xpath-support") }
    }
}
```

- 메타데이터 규칙으로 두 모듈에 동일한 캐퍼빌리티(`xml:xpath-support`)를 선언
- 의존성 선언 쪽에서 `requireCapability()`로 원하는 쪽을 명시하면, Gradle이 나머지를 충돌 후보로 인식해 하나만 선택

#### 핵심 포인트

- 캐퍼빌리티는 "같은 좌표"가 아니라 "같은 역할"을 하는 라이브러리 간의 충돌을 표현하는 수단
- 버전 충돌(같은 모듈, 다른 버전)과는 다른 문제이므로 혼동하지 않아야 함

### 정리

- 의존성은 단일 문자열 표기법(`"group:name:version"`)으로 선언하며, 맵 표기법은 Gradle 10에서 사라짐
- 버전 카탈로그(`libs.versions.toml`)로 멀티 프로젝트 전체의 버전을 한 곳에서 관리
- `strictly()`나 `constraints {}` + `because()`로 전이 의존성의 버전을 강제하고, 그 이유를 문서화
- 동일 기능을 제공하는 서로 다른 라이브러리는 캐퍼빌리티를 부여해 대체 관계로 만들고 충돌을 해소

---

## 멀티 프로젝트 빌드 구조화하기 (Structuring Multi-Project Builds)

> 원문: https://docs.gradle.org/current/userguide/multi_project_builds_intermediate.html

### 개요

- 애플리케이션이 커지면 하나의 프로젝트에 모든 소스를 몰아넣기보다 기능·계층별로 모듈을 쪼개는 편이 유지보수에 유리
- Gradle은 이런 요구를 멀티 프로젝트 빌드(멀티 모듈 프로젝트라고도 부름)로 지원 → 하나의 빌드는 정확히 하나의 루트 프로젝트와 그 아래 여러 서브프로젝트로 구성됨

### 디렉터리 구조

- 가장 단순한 형태는 루트 아래에 서브프로젝트 디렉터리를 나란히 두고, 각 서브프로젝트가 자신만의 빌드 스크립트를 갖는 방식

```text
.
├── gradlew
├── gradlew.bat
├── settings.gradle(.kts)
├── sub-project-1/build.gradle(.kts)
├── sub-project-2/build.gradle(.kts)
└── sub-project-3/build.gradle(.kts)
```

- 루트의 `settings.gradle.kts`는 이 빌드에 어떤 서브프로젝트가 속하는지를 선언하는 유일한 진입점

```kotlin
include("sub-project-1", "sub-project-2", "sub-project-3")
```

- 각 서브프로젝트 디렉터리에는 자체 `build.gradle(.kts)`가 있어야 하며, 여기에 해당 모듈만의 플러그인·의존성·태스크를 정의
- 루트 프로젝트 자체에도 `build.gradle(.kts)`를 둘 수 있으나, 보통은 공통 설정을 모으는 용도로만 가볍게 사용

### 프로젝트 경로(Project Path)

- Gradle은 각 프로젝트를 콜론(`:`)으로 구분되는 경로로 식별
- 루트 프로젝트의 경로는 `:` 하나뿐이고 이름을 붙이지 않아도 되며, 서브프로젝트는 `:서브프로젝트이름` 형태를 가짐
- 전체 프로젝트 목록과 경로는 `gradle projects` 명령으로 확인 가능

#### 이름으로 태스크 실행

```bash
./gradlew test
```

- 이렇게 태스크 이름만 주면, 현재 디렉터리를 기준으로 그 이름의 태스크를 가진 모든 하위 프로젝트에서 해당 태스크가 실행됨
- 계층 아래로 내려가며 이름이 일치하는 태스크를 전부 찾아 실행 → 어디에도 없으면 오류로 처리

#### 정규화된 이름으로 태스크 실행

- 특정 서브프로젝트 하나만 대상으로 하려면 프로젝트 경로를 태스크 이름 앞에 붙인 정규화된 이름을 사용

```bash
./gradlew :sub-project-1:build
./gradlew :sub-project-3:tasks
```

### 핵심 포인트: 태스크 실행 범위 구분

- 이름만 지정하면 "범위 실행"(하위 트리 전체에서 이름이 일치하는 태스크를 모두 실행), 경로를 붙이면 "정밀 실행"(단 하나의 프로젝트, 단 하나의 태스크만 실행)이라는 차이를 기억해두면 CI 스크립트나 부분 빌드를 짤 때 헷갈리지 않음

### buildSrc로 빌드 로직 공유하기

- 여러 서브프로젝트가 같은 플러그인 설정이나 커스텀 태스크를 반복해서 써야 한다면, 그 로직을 `buildSrc`라는 특별한 디렉터리에 모아 재사용 가능

```text
.
├── settings.gradle(.kts)
├── buildSrc
│   ├── build.gradle.kts
│   └── src/main/kotlin/shared-build-conventions.gradle.kts
├── sub-project-1/build.gradle(.kts)
├── sub-project-2/build.gradle(.kts)
└── sub-project-3/build.gradle(.kts)
```

- `buildSrc`는 별도로 `include()` 하지 않아도 Gradle이 자동으로 인식해서 특수한 서브프로젝트처럼 취급 → 다른 모든 프로젝트가 빌드되기 전에 먼저 컴파일함
- 여기서 만든 컨벤션 플러그인을 각 서브프로젝트의 `build.gradle(.kts)`에서 `plugins { id("shared-build-conventions") }` 형태로 적용하는 식

### 핵심 포인트: buildSrc의 장단점

- 장점: 설정 파일에 별도로 등록할 필요 없이 자동으로 인식되어 시작하기 쉬움 → 작은~중간 규모 프로젝트에서 빠르게 공통 로직을 모으는 데 적합
- 단점: `buildSrc` 코드가 바뀌면 전체 빌드가 무효화되어 다시 컴파일됨 → 프로젝트가 커질수록 이 비용이 부담스러워짐. 또한 여러 독립된 빌드 사이에서는 재사용 불가하고, 어디까지나 "하나의 빌드 내부"에서만 유효

### build-logic을 이용한 컴포짓 빌드

- 더 큰 규모로 확장하려면 `buildSrc` 대신 별도의 Gradle 빌드를 만들어 메인 빌드에 포함시키는 컴포짓 빌드(Composite Build) 방식을 씀
- 관례적으로 이 디렉터리를 `build-logic`이라 부름

```text
.
├── settings.gradle(.kts)
├── build-logic
│   ├── settings.gradle.kts
│   └── conventions
│       ├── build.gradle.kts
│       └── src/main/kotlin/shared-build-conventions.gradle.kts
├── sub-project-1/build.gradle(.kts)
├── sub-project-2/build.gradle(.kts)
└── sub-project-3/build.gradle(.kts)
```

- `build-logic`은 자기 자신만의 `settings.gradle.kts`를 가진, 독립적으로 빌드·테스트가 가능한 하나의 완전한 Gradle 프로젝트
- 루트 빌드는 `includeBuild()`로 이를 끌어와 사용

```kotlin
// 루트 settings.gradle.kts
include("sub-project-1", "sub-project-2", "sub-project-3")
includeBuild("build-logic")
```

### 핵심 포인트: buildSrc vs build-logic 선택 기준

- 원문은 컴포짓 빌드가 "서브프로젝트 간이 아니라 빌드와 빌드 사이에 로직을 공유하거나, 공유 빌드 로직에 대한 접근을 격리하는 데 가장 적합하다"고 설명
- `build-logic`은 독립된 빌드이므로 별도 팀이 따로 개발·테스트한 뒤 필요한 시점에 메인 빌드로 통합 가능하고, 여러 저장소·여러 메인 빌드에서 동일한 `build-logic`을 재사용하기도 쉬움
- 반대로 당장 프로토타입 수준이거나 단일 빌드 안에서만 로직을 쓴다면 `buildSrc`가 더 간단함 → 즉 규모와 재사용 범위가 커질수록 `buildSrc`에서 `build-logic` 기반 컴포짓 빌드로 옮겨가는 것이 Gradle이 권장하는 성장 경로
- 서브프로젝트 자체도 내부적으로 또 다른 컴포짓 빌드를 포함할 수 있어, 필요하다면 다단계로 중첩된 빌드 구조도 구성 가능

### 더 알아보기

- 빌드 로직 공유의 세부 내용은 "Sharing Build Logic between Subprojects" 문서에서, 컴포짓 빌드 자체의 동작 원리는 "Composite Builds" 문서에서 더 깊이 다룸

---

## Gradle 빌드의 디렉터리 구조 (Anatomy of a Gradle Build)

> 원문: https://docs.gradle.org/current/userguide/gradle_directories_intermediate.html

### 개요

- Gradle 빌드는 프로젝트(Project), 태스크(Task), 빌드 스크립트(Build Script), 플러그인(Plugin)이라는 네 요소로 구성됨
- 이 문서는 이 요소들이 실제 파일 시스템에서 어떤 디렉터리와 파일로 나타나는지, 그리고 프로젝트 루트와 사용자 홈 두 곳에 각각 무엇이 저장되는지를 정리

### 프로젝트 루트 디렉터리

- 프로젝트를 체크아웃했을 때 최상단에 위치하는 디렉터리로, 소스 코드와 Gradle이 자동으로 생성하는 산출물이 함께 존재

```text
gradle-project/
├── .gradle/            # 프로젝트 전용 캐시 (버전별 하위 디렉터리)
├── build/              # 빌드 결과물 출력 디렉터리
├── gradle/wrapper/      # Wrapper 실행에 필요한 jar와 설정
├── gradlew, gradlew.bat # Wrapper 실행 스크립트
├── gradle.properties    # 이 프로젝트에만 적용되는 설정값
├── settings.gradle(.kts) # 프로젝트 구성과 서브프로젝트 목록
├── subproject-one/build.gradle(.kts)
└── subproject-two/build.gradle(.kts)
```

- `.gradle/`: Gradle이 자동 생성하는 캐시 디렉터리
  - 사용한 Gradle 버전마다(`4.8/`, `4.9/` 등) 하위 폴더가 생겨 증분 빌드(incremental build)에 필요한 정보를 담음
  - 사람이 직접 손댈 필요가 없고, 소스 컨트롤에도 포함하지 않음
- `build/`: `build`, `assemble`, `test` 같은 태스크를 실행할 때 생성되는 기본 출력 디렉터리
  - 컴파일 결과, 테스트 리포트, 패키징 산출물이 여기 쌓이며 `./gradlew clean`으로 정리
- `gradle/wrapper/`: Wrapper가 사용할 Gradle 배포판 정보와 관련 jar를 담음 → 로컬에 Gradle을 설치하지 않고도 지정된 버전으로 빌드 가능
- `gradle.properties`: 이 프로젝트에만 적용되는 설정(JVM 옵션, 커스텀 속성 등)
- `settings.gradle(.kts)`: 이 빌드에 포함되는 서브프로젝트 목록과 전체 구조를 정의하는 최상위 파일
- 서브프로젝트 디렉터리: 각 서브프로젝트는 독립적인 `build.gradle(.kts)`를 가지며, 그 안에서 자신만의 플러그인·의존성·태스크를 설정

### 핵심 포인트: `.gradle`과 `gradle`의 차이

- `gradle/` (점 없음): Wrapper 설정처럼 사람이 관리하고 소스 컨트롤에 커밋해야 하는 디렉터리
- `.gradle/` (점 있음): Gradle이 스스로 만들고 관리하는 임시 캐시 디렉터리 → 지워도 다시 생성되며 커밋 대상이 아님
- `build/`: 빌드 산출물 전용 디렉터리이므로 반드시 쓰기 가능해야 하고, 출력 위치를 커스텀 경로로 바꾸는 경우에도 해당 경로가 미리 존재해야 정상 동작

### Gradle 사용자 홈 디렉터리 (`GRADLE_USER_HOME`)

- 프로젝트 루트와는 별개로, 로컬 머신 전체에서 공유되는 전역 디렉터리가 존재
- 기본 위치는 다음과 같고, `GRADLE_USER_HOME` 환경 변수로 위치 변경 가능
  - Unix/Linux/Mac: `~/.gradle`
  - Windows: `C:\Users\<사용자명>\.gradle`

```text
~/.gradle/
├── caches/           # 버전별 캐시 + 공유 캐시(jars-3, modules-2 등)
├── daemon/           # Gradle 데몬 레지스트리와 로그
├── init.d/           # 모든 빌드에 앞서 실행되는 전역 초기화 스크립트
├── jdks/             # 툴체인 기능으로 내려받은 JDK 배포판
├── wrapper/dists/     # Wrapper가 내려받은 Gradle 배포판
└── gradle.properties # 모든 빌드에 공통 적용되는 전역 설정
```

- `caches/`: 여러 프로젝트가 공유하는 의존성 jar(`jars-3`), 모듈 메타데이터(`modules-2`) 등을 버전별로 저장 → 반복 다운로드를 줄여줌
- `daemon/`: 빌드를 빠르게 재사용하기 위해 백그라운드에서 대기하는 Gradle 데몬 프로세스의 레지스트리와 로그가 Gradle 버전별로 쌓임
- `init.d/`: 머신에 존재하는 모든 Gradle 빌드 시작 전에 공통으로 적용할 설정(`my-setup.gradle` 등)을 넣는 곳
- `jdks/`: Gradle 툴체인 기능이 필요에 따라 자동으로 내려받은 JDK들이 보관됨
- `wrapper/dists/`: 각 프로젝트의 Wrapper가 요청한 Gradle 배포판(`gradle-4.8-bin`, `gradle-4.9-all` 등)이 버전별로 저장되어, 동일 버전을 여러 프로젝트가 재사용 가능
- `gradle.properties`: 특정 프로젝트가 아니라 이 머신에서 실행되는 모든 Gradle 빌드에 공통으로 적용되는 전역 설정

### 핵심 포인트: 프로젝트 설정 vs 전역 설정

- 같은 이름(`gradle.properties`)의 파일이 프로젝트 루트와 사용자 홈 두 곳에 존재할 수 있으며, 적용 범위가 다름 → 전자는 해당 프로젝트에만, 후자는 그 머신의 모든 빌드에 영향을 줌
- 값이 충돌하면 프로젝트 루트의 설정이 사용자 홈의 전역 설정보다 우선 적용됨

### 핵심 포인트: `GRADLE_USER_HOME`과 `GRADLE_HOME` 구분

- `GRADLE_USER_HOME`: 캐시, 데몬 로그, 다운로드한 배포판 등 사용자별 상태를 보관하는 디렉터리로, 대부분의 로컬 환경에서 실질적으로 사용됨
- `GRADLE_HOME`: 시스템에 직접 설치한 Gradle 배포판 자체의 설치 경로를 가리키는 선택적 값으로, Wrapper를 쓰는 경우에는 사실상 필요하지 않음
- 두 이름이 비슷해 혼동하기 쉬우니 "사용자 상태"와 "설치 경로"라는 역할 차이로 기억해두는 것이 좋음

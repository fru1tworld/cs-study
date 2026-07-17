# Part 5. Build 스크립트 작성하기 (Writing a Build Script)

> **원문:** https://docs.gradle.org/current/userguide/part5_build_scripts.html

## 개요
Intermediate Tutorial의 5번째 파트로, `build.gradle(.kts)` 파일을 실제로 뜯어보면서 각 블록이 어떤 API에서 왔는지, 그리고 커스텀 플러그인을 만들 때 build 스크립트를 어떻게 고쳐 나가는지를 다룹니다. Part 1(프로젝트 초기화), Part 2(빌드 라이프사이클), Part 3(서브프로젝트 구성), Part 4(settings 파일)를 이해했다는 전제 위에서 진행되며, 예제는 Gradle plugin 프로젝트(`java-gradle-plugin` 적용)를 기준으로 합니다.

## Project 객체와 build 스크립트의 관계
- 구성(configuration) 단계에서 Gradle은 루트와 서브프로젝트 디렉터리에서 build 스크립트를 찾고, 이를 발견하면 `Project` 객체를 하나 만들어 붙입니다.
- `Project` 객체의 역할은 크게 세 가지입니다.
  - 태스크(Task) 모음 생성
  - 플러그인 적용
  - 의존성 조회/해석
- `Project` 인터페이스가 제공하는 메서드·속성은 build 스크립트 안에서 한정자 없이 바로 호출할 수 있습니다.

```kotlin
defaultTasks("some-task")      // Project.defaultTasks()로 위임
reportsDir = file("reports")   // Project.file() + Java 플러그인의 확장 속성
```

## 핵심 포인트: build 스크립트를 구성하는 블록의 출처
공식 튜토리얼이 예로 드는 Gradle 플러그인 프로젝트의 build 스크립트는 아래 요소들로 구성됩니다.

```kotlin
plugins {
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm") version "2.2.0-RC2"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

gradlePlugin {
    plugins.create("greeting") {
        id = "license.greeting"
        implementationClass = "license.LicensePlugin"
    }
}
```

각 블록이 어디서 온 것인지 정리하면 다음과 같습니다.

| 블록 | 역할 | 출처 |
|---|---|---|
| `plugins { }` | 코어 플러그인(`java-gradle-plugin`)과 언어 플러그인(Kotlin/Groovy) 적용 | `PluginDependenciesSpec` |
| `repositories { }` | 의존성을 받아올 저장소 지정(`mavenCentral()`) | `Project.repositories()` |
| `dependencies { }` | 컴파일/런타임에 필요한 라이브러리 선언 | `Project.dependencies()` |
| `gradlePlugin { }` | 개발 중인 플러그인의 id·구현 클래스 정의 | Java Gradle Plugin 플러그인이 추가한 확장 |
| `tasks.register(...)` / `tasks.named(...)` | 새 태스크 등록 / 기존 태스크 설정 | `TaskContainer` |

- `plugins { }`는 코어 플러그인(버전 불필요)과 커뮤니티 플러그인(버전 필요)을 함께 선언할 수 있습니다.
- `dependencies { }`에서 `implementation(...)`으로 선언한 의존성은 **컨피규레이션(configuration)** 이라는 스코프에 귀속되며, 컴파일 타임/런타임/둘 다 중 어디에 필요한지를 결정합니다. `implementation`은 보통 런타임 클래스패스에만 필요한 경우에 씁니다.
- `gradlePlugin { }` 같은 블록은 "dependency configuration"과는 별개의 개념으로, 적용된 플러그인을 설정(configure)하는 블록입니다. `java-gradle-plugin`을 적용하면 개발 중인 플러그인의 정보를 반드시 `gradlePlugin { }`으로 채워야 합니다.
- 태스크는 `tasks.register()`로 새로 만들거나 `tasks.named()`로 이미 존재하는 태스크(예: 플러그인이 만들어 둔 `test`)를 찾아 설정합니다.

## 실습: greeting 플러그인을 license 플러그인으로 바꾸기
튜토리얼은 `gradle init`이 생성한 예제 플러그인(`greeting`)을 개조해서, 소스 파일에 라이선스 헤더를 자동으로 붙이는 `license` 플러그인으로 만들어 가는 과정을 보여줍니다.

1. **build 스크립트 수정** — `gradlePlugin { }` 블록의 플러그인 이름·id를 바꿉니다.

```kotlin
gradlePlugin {
    plugins.create("license") {         // 이름을 license로 변경
        id = "com.tutorial.license"     // id를 com.tutorial.license로 변경
        implementationClass = "license.LicensePlugin"
    }
}
```

2. **서브프로젝트에 플러그인 적용** — `app` 서브프로젝트의 build 스크립트에서 `id(...)`로 방금 정의한 플러그인을 적용합니다.

```kotlin
plugins {
    application
    id("com.tutorial.license")
}
```

3. **플러그인이 노출하는 태스크 확인** — `LicensePlugin`은 아직 실제 라이선스 로직 없이, `apply()` 안에서 `greeting`이라는 태스크 하나만 등록합니다.

```kotlin
class LicensePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("greeting") {
            doLast { println("Hello from plugin 'com.tutorial.greeting'") }
        }
    }
}
```

4. **태스크 목록/실행으로 검증** — `./gradlew tasks --all`을 실행하면 `app` 프로젝트 아래 `app:greeting` 태스크가 새로 보이고, `./gradlew :app:greeting`으로 실행하면 `Hello from plugin 'com.tutorial.greeting'`이 출력됩니다.

## 핵심 포인트: 플러그인 적용 = build 스크립트에 새 어휘가 늘어나는 것
- 플러그인을 적용하기 전에는 `application`, `gradlePlugin { }` 같은 이름을 build 스크립트에서 쓸 수 없습니다. 플러그인이 적용되는 순간 `Project`의 확장(extension)으로 새 블록/속성이 추가되고, 그때부터 한정자 없이 쓸 수 있게 됩니다.
- 따라서 낯선 블록을 만났을 때는 "이게 어느 플러그인이 추가한 확장인가?"를 먼저 확인하는 것이 build 스크립트를 읽는 기본 태도입니다.
- 서브프로젝트별로 다른 플러그인을 적용할 수 있으므로(`app`에만 `license` 적용), 같은 저장소(멀티 프로젝트)라도 프로젝트마다 사용 가능한 블록/태스크 목록이 다를 수 있습니다.

## 정리
- build 스크립트는 구성 단계에서 생성되는 `Project` 객체를 대상으로 실행되는 코드이며, 한정자 없는 메서드/속성 호출은 모두 이 `Project`(또는 그 확장)로 위임된다.
- `plugins { }` → 플러그인 적용, `repositories { }` → 의존성 출처, `dependencies { }` → 의존성 선언, `gradlePlugin { }` 같은 확장 블록 → 플러그인별 세부 설정, `tasks.register()`/`tasks.named()` → 태스크 등록/설정이라는 역할 분담을 갖는다.
- 새 플러그인을 개발할 때는 `gradlePlugin { }`에서 id·구현 클래스를 정의하고, 이를 적용할 서브프로젝트의 `plugins { }`에 `id(...)`로 등록하는 흐름을 따른다.
- 플러그인이 적용되면 해당 프로젝트에서 쓸 수 있는 태스크·블록이 늘어나며, `./gradlew tasks --all`로 이를 직접 확인할 수 있다.

다음 단계는 태스크 작성하기(Writing Tasks)로 이어집니다.

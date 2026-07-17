# Gradle 의존성 관리 모범 사례

> **원문:** https://docs.gradle.org/current/userguide/best_practices_dependencies.html

## 개요

Gradle 공식 문서가 정리한 의존성 관리 모범 사례 목록이다. 버전을 어디서 관리할지, 의존성을 어떻게 선언할지, 저장소를 어디에 설정할지 등 실무에서 자주 실수하는 지점들을 Do/Don't 형태로 정리한다.

## 1. 버전은 버전 카탈로그 한곳에 모은다

- `project.ext`, 커스텀 상수, 지역 변수로 버전을 흩어 관리하지 말고 `gradle/libs.versions.toml` 파일에 중앙화한다.
- 버전을 올릴 때 여러 `build.gradle(.kts)` 파일을 돌아다니며 고칠 필요 없이 TOML 한 곳만 수정하면 된다.
- `bundles`를 이용하면 자주 같이 쓰는 라이브러리 묶음을 하나의 별칭으로 선언할 수 있다.

```toml
[versions]
groovy = "3.0.5"

[libraries]
groovy-core = { module = "org.codehaus.groovy:groovy", version.ref = "groovy" }

[bundles]
groovy = ["groovy-core", "groovy-json", "groovy-nio"]
```

```kotlin
dependencies {
    api(libs.bundles.groovy)
}
```

### 핵심 포인트

멀티 모듈 빌드에서 버전 드리프트(모듈마다 버전이 조금씩 달라지는 문제)를 막는 가장 확실한 방법이 버전 카탈로그 중앙화다.

## 2. 카탈로그 항목 이름을 일관되게 짓는다

- 언더스코어 대신 대시(`-`)로 세그먼트를 구분한다.
- 최상위 도메인은 빼고 group ID에서 첫 세그먼트를 뽑아낸다.
- 대시로 구분된 이름은 접근 시 카멜케이스로 변환된다. 예: `spring-boot-starter-web` → `libs.spring.boot.starter.web`
- `foo`, `bar` 같은 모호한 이름은 피하고, 플러그인 라이브러리에는 `-plugin` 접미사를 붙인다.

### 핵심 포인트

이름이 일관돼야 자동완성과 검색이 편해지고, 다른 개발자가 카탈로그만 보고도 어떤 라이브러리인지 짐작할 수 있다.

## 3. 저장소는 build 파일이 아니라 settings 파일에 설정한다

- `pluginManagement {}`와 `dependencyResolutionManagement {}` 블록을 `settings.gradle.kts`에 두고, 개별 `build.gradle.kts`에는 저장소를 선언하지 않는다.
- 저장소는 프로젝트 정의의 일부가 아니라 빌드 전역 로직에 속하는 설정이기 때문이다.

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}
```

### 핵심 포인트

settings 파일에 모아두면 반복 선언을 피하고, `FAIL_ON_PROJECT_REPOS`로 서브프로젝트가 몰래 다른 저장소를 추가하는 것도 막아 디버깅이 쉬워진다.

## 4. Kotlin 표준 라이브러리는 명시적으로 선언하지 않는다

- `kotlin("stdlib")`을 직접 추가하지 않는다. Kotlin Gradle Plugin이 알아서, 그리고 항상 일치하는 버전으로 추가해준다.
- stdlib 버전을 수동으로 관리해야 하는 특수한 경우에만 `kotlin.stdlib.default.dependency = false`를 설정한다.

```kotlin
plugins {
    kotlin("jvm").version("2.3.21")
}
// api(kotlin("stdlib")) 같은 선언은 불필요하다
```

## 5. 같은 의존성을 여러 컨피규레이션에 중복 선언하지 않는다

- 같은 좌표를 `api`와 `implementation`, 혹은 `compileOnly`와 `implementation`에 동시에 선언하지 않는다.
- 중복 선언은 관리를 어렵게 만들고, 원인을 찾기 힘든 클래스패스 문제로 이어질 수 있다.

```kotlin
// Don't
dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.0") // 중복
}
```

## 6. 의존성은 단일 GAV 문자열로 선언한다

- `implementation("org.example:library:1.0")`처럼 `"group:name:version"` 한 줄로 쓴다.
- `group = ...`, `name = ...`, `version = ...` 형태의 명명된 인자(맵) 표기법은 지원 중단되었고 Gradle 10부터는 사용할 수 없다.

```kotlin
// Do
implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")

// Don't
implementation(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = "2.17.0")
```

### 핵심 포인트

단일 문자열 표기법이 더 간결하고, JVM 생태계 전반에서 표준으로 쓰인다.

## 7. 저장소가 여러 개면 콘텐츠 필터링을 적용한다

- `includeGroupByRegex()` 등으로 각 저장소가 어떤 그룹의 의존성만 제공할 수 있는지 명시한다.
- 얻는 이점은 세 가지다.
  - **성능**: 실제로 존재할 가능성이 있는 저장소에만 질의한다.
  - **보안**: 모든 저장소에 무분별하게 요청을 보내지 않는다.
  - **신뢰성**: 잘못된 메타데이터를 가진 저장소를 건너뛴다.

```kotlin
dependencyResolutionManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroup("com.google.gms")
            }
        }
        mavenCentral()
    }
}
```

## 8. 제외(exclude)는 최대한 좁은 범위로 적용한다

- 제외는 컨피규레이션 전체가 아니라 특정 의존성에 붙인다.
- 그룹 전체가 아니라 개별 모듈 단위로 제외한다.
- `configurations.all { }`이나 `configurations.configureEach { }`를 통한 전역 제외는 피한다.

```kotlin
dependencies {
    implementation("org.apache.commons:commons-pool2:2.12.1") {
        exclude(group = "cglib", module = "cglib")
        exclude(group = "org.ow2.asm", module = "asm-util")
    }
    implementation("org.hibernate:hibernate-core:3.6.10.Final") {
        exclude(group = "javassist", module = "javassist")
    }
}
```

### 핵심 포인트

전역 제외는 성능에 영향을 주고, 빌드의 다른 곳에서 필요한 의존성을 모르게 걷어내거나 런타임 충돌을 유발할 위험이 있다.

## 정리

- 버전은 카탈로그(`libs.versions.toml`) 한곳에, 저장소는 `settings.gradle.kts` 한곳에 모은다.
- 카탈로그 항목 이름은 대시 기반으로 일관되게 짓는다.
- Kotlin stdlib처럼 플러그인이 자동으로 넣어주는 의존성은 직접 선언하지 않는다.
- 같은 의존성을 여러 컨피규레이션에 중복 선언하지 않고, 표기법은 단일 GAV 문자열로 통일한다.
- 저장소가 여러 개면 콘텐츠 필터링으로 범위를 좁히고, 제외(exclude)는 가능한 한 좁게 적용한다.

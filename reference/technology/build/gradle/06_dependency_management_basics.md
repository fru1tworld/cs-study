# Gradle 의존성 관리 기초

> **원문:** https://docs.gradle.org/current/userguide/dependency_management_basics.html

## 개요

Gradle은 프로젝트에 필요한 외부 자원(라이브러리, JAR, 소스 코드 등)을 선언하고 해석(resolve)하는 과정을 자동화하는 기능을 기본으로 제공합니다. 이를 **의존성 관리(Dependency Management)**라고 부릅니다.

- 의존성은 `build.gradle(.kts)` 스크립트 안에서 선언한다.
- Gradle이 다운로드, 캐싱, 버전 해석, 버전 충돌 처리를 자동으로 처리해 준다.
- 개발자가 라이브러리 버전을 수동으로 관리할 필요가 줄어든다.

## 의존성 선언하기

의존성은 `dependencies {}` 블록 안에 선언한다.

```kotlin
plugins {
    id("java-library")
}

dependencies {
    implementation("com.google.guava:guava:32.1.2-jre")
    api("org.apache.juneau:juneau-marshall:8.2.0")
}
```

### 핵심 포인트: 컨피규레이션(Configuration, "버킷")

Gradle은 의존성을 **컨피규레이션**이라는 버킷 단위로 묶어서 관리한다. 컨피규레이션은 해당 의존성이 *언제, 어디서, 어떻게* 사용되는지 범위(scope)를 정의한다.

- `implementation` — 프로덕션 코드를 컴파일·실행할 때 필요한 의존성. 소비자(consumer)에게는 노출되지 않는다.
- `api` — 라이브러리를 사용하는 외부 모듈에도 노출되어야 하는 의존성.
- 플러그인을 적용하면 그에 맞는 컨피규레이션이 자동으로 생성된다.
  - `java` / `java-library` 플러그인 → `implementation`, `api`, `compileOnly`, `runtimeOnly`, `testImplementation` 등.
  - Android Gradle Plugin(AGP) → `debugImplementation`, `releaseImplementation`, `androidTestImplementation`, `freeDebugImplementation` 같은 빌드 타입·플레이버별 컨피규레이션.
  - Kotlin Multiplatform(KMP) → `commonMainImplementation`, `commonTestImplementation`, `iosArm64MainImplementation` 등 소스 세트 단위 컨피규레이션.

즉, 별도 설정 없이도 플러그인이 프로젝트 구조에 맞는 "올바른 자리"를 미리 만들어 두는 방식이다.

## 의존성 트리 확인하기

`dependencies` 태스크로 프로젝트의 의존성 트리를 컨피규레이션별로 확인할 수 있다.

```bash
./gradlew :app:dependencies
```

- 출력 결과는 컨피규레이션(예: `runtimeClasspath`)별로 그룹화되어 트리 형태로 나타난다.
- `->` 표시는 버전이 다른 값으로 변경(치환)되었음을 의미한다.
- `(c)`는 해당 좌표가 제약(constraint)으로만 참여했음을 나타내는 표시다.
- 전이 의존성(transitive dependency, 의존성이 끌고 들어오는 또 다른 의존성)까지 한눈에 파악할 수 있어 버전 충돌 여부를 진단하는 데 유용하다.

## 버전 카탈로그(Version Catalog)

### 왜 필요한가

여러 서브프로젝트에서 같은 라이브러리를 반복 선언하면 버전이 제각각 흩어지기 쉽다. 버전 카탈로그는 의존성의 좌표(coordinate)와 버전을 **한 곳에서 중앙 관리**하기 위한 기능이다.

- 서브프로젝트 간 공통 의존성 선언 공유
- 중복 선언·버전 불일치 방지
- 대규모 프로젝트에서 의존성/플러그인 버전 강제(enforce)

### 구조

`gradle/libs.versions.toml` 파일에 다음 네 섹션을 정의한다.

| 섹션 | 역할 |
|---|---|
| `[versions]` | 라이브러리·플러그인이 참조할 버전 번호 선언 |
| `[libraries]` | 실제 빌드에서 사용할 라이브러리 정의 |
| `[bundles]` | 여러 의존성을 하나로 묶은 집합 정의 |
| `[plugins]` | 플러그인 정의 |

```toml
[versions]
guava = "32.1.2-jre"
juneau = "8.2.0"

[libraries]
guava = { group = "com.google.guava", name = "guava", version.ref = "guava" }
juneau-marshall = { group = "org.apache.juneau", name = "juneau-marshall", version.ref = "juneau" }
```

이 파일을 프로젝트의 `gradle/` 디렉터리에 두면 Gradle이 자동으로 인식하고, 빌드 스크립트에서는 `libs` 접근자로 참조할 수 있다.

```kotlin
dependencies {
    implementation(libs.guava)
    api(libs.juneau.marshall)
}
```

- IntelliJ, Android Studio 등 IDE도 이 메타데이터를 인식해 코드 자동완성을 지원한다.
- 버전 문자열을 직접 하드코딩하지 않으므로, 버전을 올릴 때 `[versions]` 섹션 한 곳만 수정하면 된다.

## 정리

- Gradle의 의존성 관리는 **선언 → 해석(다운로드/캐싱) → 충돌 해결**의 자동화된 파이프라인이다.
- 의존성은 `dependencies {}` 블록에, 컨피규레이션(예: `implementation`, `api`)이라는 버킷을 통해 범위가 결정된다.
- `./gradlew :app:dependencies`로 실제 해석된 의존성 트리와 충돌·치환 여부를 검사할 수 있다.
- 버전 카탈로그(`libs.versions.toml`)를 쓰면 버전과 좌표를 한 곳에서 관리해 멀티 모듈 프로젝트의 일관성을 높일 수 있다.

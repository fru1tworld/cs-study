# 빌드 구조화 모범 사례 (Best Practices for Structuring Builds)

> **원문:** https://docs.gradle.org/current/userguide/best_practices_structuring_builds.html

## 개요

Gradle 빌드를 어떻게 쪼개고 배치하느냐는 컴파일 회피, 병렬 실행, 캐시 적중률에 직접 영향을 줍니다. 이 문서는 "프로젝트를 어디에 두고, 빌드 로직을 어디에 모을지"에 대한 다섯 가지 원칙을 Do/Don't 형태로 정리합니다.

## 원칙 1. 빌드는 최대한 여러 프로젝트로 쪼개라

하나의 거대한 프로젝트에 소스를 전부 몰아넣으면 파일 하나만 바뀌어도 전체를 다시 컴파일해야 하고, 태스크를 병렬로 돌릴 방법이 없습니다. 반대로 API/구현, 프런트엔드/백엔드, 기능 단위(수직 슬라이스), 코드 생성기/사용처처럼 경계를 나눠 별도 프로젝트로 만들면 변경분만 재컴파일되고 프로젝트별 컴파일 클래스패스도 최소화됩니다.

- **Don't**: `app` 프로젝트 하나에 `Main.java`, `Util.java`, `GuavaUtil.java`, `CommonsUtil.java`를 다 넣고 `application` 플러그인 하나로 전체를 컴파일한다. Guava·commons-lang 의존성도 필요 없는 코드까지 다 보게 된다.
- **Do**: `util`, `util-guava`, `util-commons`를 별도 프로젝트로 분리하고 `app`은 필요한 것만 `implementation(project(":util-guava"))` 식으로 참조한다.

```kotlin
// util-commons/build.gradle.kts
plugins { `java-library` }
dependencies {
    api(project(":util"))
    implementation("commons-lang:commons-lang:2.6")
}
```

> **핵심 포인트**: 원문은 "프로젝트 개수를 줄이기보다는 늘리는 쪽으로 판단하라"고 권장합니다. 프로젝트를 잘게 쪼갤수록 Gradle이 변경 영향 범위를 정확히 계산해 불필요한 재컴파일과 순차 실행을 줄일 수 있기 때문입니다.

## 원칙 2. 루트 프로젝트에 소스 파일을 두지 마라

루트 프로젝트는 빌드의 진입점이자 전역 설정·컨벤션을 모아두는 자리입니다. 여기에 `src/main/java`를 직접 두고 `java-library` 같은 플러그인을 적용하면, 전역 설정과 특정 모듈의 컴파일 설정이 뒤섞여 버립니다.

- **Don't**: 루트 `build.gradle.kts`에 `java-library`를 적용하고 루트 아래 `src/main/java/...`에 소스를 직접 둔다.
- **Do**: 소스는 `core` 같은 별도 서브프로젝트로 옮기고, 루트는 `settings.gradle.kts`에서 `include("core")`만 선언한 채 비워 둔다.

> **핵심 포인트**: 루트는 "빌드 전체를 조율하는 자리"로만 남겨 둬야 프로젝트가 커질 때 새 모듈을 추가하기 쉽고, 소스 전용 플러그인이 루트에 잘못 적용되는 사고를 막을 수 있습니다.

## 원칙 3. buildSrc보다 build-logic 컴포짓 빌드를 우선하라

`buildSrc`는 별도 등록 없이 자동으로 인식되어 편리하지만, 클래스로더가 메인 빌드와 다르게 동작해 예상치 못한 문제가 생기고, 파일이 바뀌면 무효화 범위가 넓으며, 독립적으로 개발·배포하기도 어렵습니다. Settings 플러그인을 `buildSrc`에 두면 `pluginManagement` 블록에 포함시켜야 해서 빌드 캐시 활용도까지 떨어집니다.

- **Don't**: 커스텀 플러그인·태스크를 `buildSrc/src/main/java`에 넣고 자동 인식에만 의존한다.
- **Do**: `build-logic`이라는 별도의 완전한 Gradle 빌드(자체 `settings.gradle.kts` 포함)를 만들고, 루트에서 `includeBuild("build-logic")`으로 명시적으로 끌어온다.

```kotlin
// settings.gradle.kts (root)
includeBuild("build-logic")
```

> **핵심 포인트**: 컴포짓 빌드는 외부 의존성과 동일한 방식으로 명시적 의존 관계를 맺기 때문에, 변경 시 무효화 범위가 더 좁고 독립적으로 개발·게시할 수 있습니다. 단, Settings 플러그인은 별도의 얇은 포함 빌드(예: `build-logic-settings`)로 분리하는 편이 낫고, `build-logic` 내부에 서브프로젝트가 지나치게 많고 플러그인 조합이 제각각이면 성능 저하나 Build Services 이슈가 생길 수 있다는 점도 원문이 짚고 있습니다.

## 원칙 4. 의도치 않은 빈 프로젝트 생성을 피하라

`include(":subs:web:my-module")`처럼 경로에 콜론을 여러 개 넣으면, Gradle은 `:subs`, `:subs:web`, `:subs:web:my-module` 세 프로젝트를 전부 만듭니다. 앞의 두 개는 실제 `build.gradle.kts`가 없는 빈 프로젝트인데도 생성되어 `allprojects {}`/`subprojects {}` 설정에 영향을 받고, 리포트를 어지럽히고, 태스크 경로도 길어집니다.

- **Don't**: `include(":subs:web:my-web-module")`만 쓰고 끝낸다. → `:subs`, `:subs:web`이 빈 프로젝트로 딸려 생성된다.
- **Do**: 짧은 이름으로 `include()`하고 `projectDir`을 실제 디렉터리로 직접 지정한다.

```kotlin
include(":my-web-module")
project(":my-web-module").projectDir = file("subs/web/my-web-module")
```

> **핵심 포인트**: 이렇게 하면 `gradle :subs:web:my-web-module:build` 대신 `gradle :my-web-module:build`처럼 짧은 경로로 태스크를 실행할 수 있고, 프로젝트 리포트에도 실제 존재하는 프로젝트만 나타나며 전역 설정이 불필요한 빈 프로젝트에 적용되는 것도 막을 수 있습니다.

## 원칙 5. 공통 빌드 로직은 컨벤션 플러그인으로 뽑아내라

여러 프로젝트의 `build.gradle.kts`에 자바 버전, 컴파일러 옵션, 테스트 설정 같은 코드가 그대로 복사·붙여넣기 되어 있으면, 하나를 고칠 때마다 전체 파일을 다 손봐야 합니다. 이런 반복 설정은 컨벤션 플러그인 하나로 뽑아내고, 각 프로젝트는 그 플러그인 id만 적용하면 됩니다.

- **Don't**: `project-a`, `project-b`의 `build.gradle.kts`에 `sourceCompatibility`, `JavaCompile` 옵션, `useJUnitPlatform()` 설정을 각각 똑같이 반복해서 적어 둔다.
- **Do**: `build-logic`에 `my.java-library.gradle.kts` 같은 컨벤션 플러그인을 만들어 공통 설정을 담고, 각 프로젝트는 `plugins { id("my.java-library") }`만 선언한다.

```kotlin
// build-logic/src/main/kotlin/my.java-library.gradle.kts
plugins {
    id("my.base-java-library")
    id("my.java-use-junit5")
}
```

> **핵심 포인트**: 컨벤션 플러그인은 원칙 3의 `build-logic` 컴포짓 빌드 안에 두는 것이 자연스러운 조합입니다. 공통 동작을 한 곳에서 바꾸면 전체 프로젝트에 반영되고, 각 프로젝트 빌드 파일은 그 프로젝트만의 관심사(추가 의존성 등)로 간결하게 남습니다.

## 요약

| 원칙 | 하지 말 것 | 할 것 |
| --- | --- | --- |
| 모듈화 | 소스를 한 프로젝트에 몰아넣기 | 경계별로 프로젝트를 잘게 분리 |
| 루트 구성 | 루트에 소스·컴파일 플러그인 배치 | 루트는 조율 전용, 소스는 서브프로젝트로 |
| 빌드 로직 위치 | `buildSrc` 자동 인식에 의존 | `build-logic` 컴포짓 빌드로 명시적 포함 |
| include 경로 | 중첩 경로로 `include()`해 빈 프로젝트 생성 | 짧은 이름 + `projectDir` 지정 |
| 공통 설정 | 프로젝트마다 설정 코드 복붙 | 컨벤션 플러그인으로 추출·재사용 |

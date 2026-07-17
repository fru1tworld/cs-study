# Gradle 사전 컴파일 스크립트 플러그인

> **원문:** https://docs.gradle.org/current/userguide/pre_compiled_script_plugin_advanced.html

## 개요
**사전 컴파일 스크립트 플러그인(precompiled script plugin)**은 개발하기 가장 간단한 플러그인 형태입니다. 코틀린(`.kts`)이나 그루비(`.gradle`) 스크립트를 일반 빌드 스크립트와 동일한 문법으로 작성하면, Gradle이 이를 컴파일해 진짜 플러그인처럼 동작시켜줍니다. `Plugin` 인터페이스를 구현하는 바이너리 플러그인과 달리 클래스 작성이나 수동 등록이 필요 없습니다.

- **캡슐화** — 반복되는 설정을 이름 있는 플러그인으로 빼내 `build.gradle(.kts)`를 깔끔하게 유지
- **툴링 지원** — IDE 자동완성·네비게이션을 그대로 활용
- **보일러플레이트 제거** — 플러그인 클래스 작성이나 등록 절차 불필요
- **낮은 진입 장벽** — 익숙한 빌드 스크립트 문법을 그대로 사용하면서 플러그인의 구조적 이점을 얻음

## 컨벤션 플러그인으로서의 역할
사전 컴파일 스크립트 플러그인은 대부분 **컨벤션 플러그인(convention plugin)** 용도로 쓰입니다. 여러 프로젝트에 공통 설정·기본값·동작을 일관되게 적용하는 재사용 가능한 빌드 로직을 뜻하며, 특히 멀티 프로젝트 빌드에서 일관성을 지키는 데 유용합니다.

예를 들어 `java-library-convention.gradle.kts` 하나에 자바 라이브러리 플러그인 적용, 체크스타일 설정, 소스 호환성 지정, 공통 의존성 등을 몰아넣으면, 각 서브프로젝트는 다음처럼 한 줄로 그 컨벤션을 가져다 쓸 수 있습니다.

```kotlin
// buildSrc/src/main/kotlin/java-library-convention.gradle.kts
plugins {
    `java-library`
    checkstyle
}
java { sourceCompatibility = JavaVersion.VERSION_11 }
```

```kotlin
// 소비하는 프로젝트의 build.gradle.kts
plugins {
    `java-library-convention`
}
```

## 어디에 두는가: buildSrc vs 컴포지트 빌드
사전 컴파일 스크립트 플러그인은 보통 다음 두 위치 중 하나에 둡니다.

1. **`buildSrc` 디렉터리** — 루트 프로젝트 바로 아래에 두는 특별한 디렉터리로, Gradle이 메인 빌드보다 먼저 빌드해 자동으로 클래스패스에 올려줍니다.
   ```
   buildSrc/
   ├── build.gradle.kts
   └── src/main/kotlin/myproject.java-conventions.gradle.kts
   ```
2. **컴포지트 빌드(`build-logic` 등)** — `includeBuild`로 포함시키는 독립된 빌드로, 여러 리포지토리·프로젝트에 걸쳐 빌드 로직을 공유하고 싶을 때 `buildSrc`보다 유연합니다.
   ```
   build-logic/
   ├── settings.gradle.kts
   ├── build.gradle.kts
   └── src/main/kotlin/myproject.java-conventions.gradle.kts
   ```

## 플러그인 ID와 적용 방법
플러그인 ID는 확장자(`.gradle.kts`/`.gradle`)를 뗀 **스크립트 파일명**에서 그대로 나옵니다. 예를 들어 `myproject.java-conventions.gradle.kts`는 곧 `myproject.java-conventions`라는 ID를 갖는 플러그인이 됩니다. 별도의 `Plugin` 인터페이스 구현이나 등록 없이, 스크립트 자체가 플러그인 역할을 합니다.

```kotlin
plugins {
    id("myproject.java-conventions")
}
```

## 배포는 권장되지 않음
Gradle Plugin Portal 같은 공개 저장소나 Maven/Artifactory/Nexus 같은 사내 저장소에 사전 컴파일 스크립트 플러그인을 게시하는 것 자체는 기술적으로 가능합니다. 하지만 이 방식은 **내부 사용을 전제로 설계**되어 있어 공식적으로 배포를 권장하지 않습니다. 외부에 배포해야 한다면 먼저 바이너리 플러그인으로 전환하는 것이 정석입니다.

## 실전 적용 흐름 (buildSrc 기준)
1. **`buildSrc` 디렉터리 생성** — 루트 프로젝트 아래에 `buildSrc/src/main/kotlin`(또는 `groovy`) 구조를 만든다.
2. **빌드 파일 작성** — `buildSrc/build.gradle.kts`에 `kotlin-dsl`(그루비는 `groovy-gradle-plugin`) 플러그인과 `gradlePluginPortal()` 저장소를 선언해, 컨벤션 스크립트가 컴파일될 수 있게 한다.
3. **플러그인 스크립트 작성** — `myproject.java-conventions.gradle.kts` 같은 파일에 `java` 플러그인 적용, 태스크 설정, 저장소 지정 등 공유하고 싶은 로직을 담는다. 이 파일명이 곧 플러그인 ID(`myproject.java-conventions`)가 된다.
4. **소비 프로젝트에서 적용** — 각 서브프로젝트의 `build.gradle.kts`에서 `id("myproject.java-conventions")`로 적용하고, 그 프로젝트만의 추가 의존성 등을 덧붙인다.

## 핵심 포인트
- 사전 컴파일 스크립트 플러그인 = **빌드 스크립트 문법 그대로 작성하는 플러그인**. 클래스 구현이나 수동 등록이 필요 없어 진입 장벽이 가장 낮다.
- 실무에서는 거의 항상 **컨벤션 플러그인**(여러 프로젝트에 공통 설정을 강제하는 용도)으로 쓰인다.
- 배치 위치는 **`buildSrc`**(단순, 자동 인식) 또는 **컴포지트 빌드인 `build-logic`**(더 유연, 여러 빌드 간 공유 용이) 둘 중 하나.
- 플러그인 ID는 스크립트 **파일명**에서 그대로 파생되므로, 명명 규칙(`<namespace>.<name>`)만 지키면 별도 메타데이터 등록이 필요 없다.
- 외부 배포용이 아니라 **내부 전용**으로 설계된 기능이며, 공개 배포가 필요하면 바이너리 플러그인으로 전환해야 한다.

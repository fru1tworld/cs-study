# Gradle 모범 사례

## Gradle 모범 사례 소개 (Best Practices Introduction)

> 원문: https://docs.gradle.org/current/userguide/best_practices.html

### 개요

Gradle은 유연하고 강력한 빌드 도구이나, 그 유연함 때문에 같은 문제를 여러 방식으로 풀 수 있고 그중 일부는 유지보수하기 어렵거나 성능이 나쁜 선택일 수 있음. 공식 문서의 "Best Practices" 챕터는 이런 상황에서 "지금 하는 방식이 널리 권장되는 방식인지"를 빠르게 점검하도록, Gradle 팀이 대규모 실제 프로젝트를 지원하며 얻은 경험을 정리한 모음임. 이 페이지는 그 챕터 전체의 입구 역할.

### 모범 사례 항목이 지켜야 할 세 가지 기준

문서는 각 권장 사항을 작성할 때 다음 세 가지 기준을 지키려 한다고 밝힘.

- 간결함(Brief): 빌드 스크립트를 작성하는 사람이 자신이 이 권장 사항을 따르고 있는지 아닌지 한눈에 알아볼 수 있어야 함
- 이해 가능함(Understandable): 단순히 "이렇게 하라"가 아니라 왜 그렇게 해야 하는지 근거를 함께 제시해 이해를 도움
- 실행 가능함(Actionable): 일반적인 빌드 상황에 폭넓게 적용될 수 있는, 구체적이고 실무적인 조언이어야 함

#### 핵심 포인트
- 세 기준은 결국 "빨리 알아채고, 왜인지 이해하고, 바로 적용할 수 있는" 조언을 목표로 함 → 원칙 나열이 아니라 실무 체크리스트에 가까움
- 권장 사항마다 도입된 Gradle 버전이 함께 표기되어, 자신이 쓰는 Gradle 버전에서 실제로 적용 가능한지 바로 확인 가능

### 7개 카테고리로 나눈 이유

모범 사례는 한 페이지에 나열하기엔 범위가 넓어, 관심 영역별로 아래 7개 카테고리 문서로 분리됨.

1. General Best Practices — 특정 영역에 묶이지 않는, 모든 빌드에 공통으로 적용되는 기본 원칙
2. Best Practices for Structuring Builds — 프로젝트·모듈 구조를 어떻게 나눌지에 대한 권장 사항
3. Best Practices for Dependencies — 의존성 선언, 버전 관리, 저장소 설정 관련 권장 사항
4. Best Practices for Tasks — 커스텀 태스크 작성 및 태스크 그래프 구성 관련 권장 사항
5. Best Practices for Performance — 빌드 속도를 높이기 위한 캐싱, 병렬화 등의 권장 사항
6. Best Practices for Security — 빌드 공급망(supply chain) 보안 관련 권장 사항
7. Best Practices for Testing — 테스트 작성 및 구성 전략 관련 권장 사항

#### 핵심 포인트
- 카테고리 구분은 "어떤 문제를 풀 때 어디를 봐야 하는가"에 대한 안내판 역할 → 빌드가 느리면 Performance 문서로, 멀티 모듈 구조를 잡아야 하면 Structuring Builds 문서로 이동
- 각 카테고리 문서는 이 소개 페이지보다 훨씬 구체적인 예시와 안티패턴을 다룸 (이 페이지 자체에는 코드 예시 없음)

### 문서를 활용하는 방법

- 처음 Gradle 모범 사례를 훑어볼 때는 이 소개 페이지 → 관심 있는 카테고리 문서 순서로 읽는 것이 자연스러움
- 전체 권장 사항을 표 형태로 한눈에 보고 싶으면, 별도로 제공되는 Best Practices Index 페이지에서 모든 항목과 해당 항목이 도입된 Gradle 버전을 한 번에 확인 가능
- 이 챕터는 초급자부터 대규모 빌드를 운영하는 숙련자까지 모두를 대상으로 하며, 특정 기능의 사용법(How-To)이 아니라 "여러 선택지 중 어떤 것이 더 나은 선택인가"에 대한 판단 기준 제공이 목적

---

## Gradle 일반 모범 사례 (General Best Practices)

> 원문: https://docs.gradle.org/current/userguide/best_practices_general.html

### 개요

공식 문서는 빌드 스크립트 문법 선택부터 플러그인 적용 방식, `afterEvaluate` 사용 여부까지 총 9가지 규칙을 제시함. 대부분 "Don't Do This / Do This Instead" 형태의 대조 예제로 설명되며, 공통 목표는 빌드를 예측 가능하고, 설정 캐시(Configuration Cache)와 호환되게, 순서에 의존하지 않게 만드는 것.

### 1. Kotlin DSL을 사용하라

- 신규 빌드나 서브프로젝트는 `build.gradle` 대신 `build.gradle.kts`로 작성하는 것이 권장됨
- Groovy DSL은 동적 타이핑이라 IDE 자동완성·탐색이 약하고, 오탈자가 실행 시점에야 드러남 → Kotlin DSL은 정적 타입이라 IDE 지원이 훨씬 강력
- Gradle 8.0부터 Kotlin DSL이 기본값이 됨, Android Studio도 동일한 방향

### 2. Gradle은 항상 최신 마이너 버전을 유지하라

- 활성 지원 대상은 현재 메이저 버전과 그 직전 메이저 버전뿐 → 오래된 버전에 머물면 보안 패치나 성능 개선을 받지 못함
- Wrapper로 버전을 올리는 것이 표준 절차

```bash
./gradlew wrapper --gradle-version <version>
```

- 핵심 포인트: 플러그인보다 먼저 Gradle 버전을 올리고, CI에 "새 버전 미리 테스트용" 섀도우 잡을 두어 호환성을 검증한 뒤, 플러그인을 순차적으로 업그레이드하는 순서를 권장. Deprecation 경고는 다음 메이저 업그레이드를 대비하라는 신호로 취급 필요.

### 3. 플러그인은 `plugins {}` 블록으로 적용하라

레거시 방식인 `buildscript {}` + `apply(plugin = ...)` 조합은 지양하고, `plugins {}` 블록을 사용.

하지 말아야 할 방식
```kotlin
buildscript {
    repositories { gradlePluginPortal() }
    dependencies { classpath("com.google.protobuf:protobuf-gradle-plugin:0.9.4") }
}
apply(plugin = "java")
apply(plugin = "com.google.protobuf")
```

권장 방식
```kotlin
plugins {
    id("java")
    id("com.google.protobuf").version("0.9.4")
}
```

- `plugins {}` 블록은 플러그인 클래스 로딩을 최적화하고 재사용하며, 부작용 없이(idempotent) 실행되고, 문법도 더 간결함

### 4. 플러그인 적용 순서를 가정하지 마라

- Gradle의 플러그인 적용은 결정론적(deterministic)이지만, 여러 파일·프로젝트·Included Build에 걸쳐 순서를 추론하기는 어려움
- `allprojects {}`, `subprojects {}`, `afterEvaluate {}`로 순서를 강제하는 로직은 프로젝트를 하나 추가하거나 include 순서만 바꿔도 깨지는 취약한 빌드가 됨

- 빌드 작성자 입장: 서로 다른 프로젝트·Included Build·스크립트 간 순서에 의존하는 구성을 피할 것
- 플러그인 개발자 입장: 사용자가 어떤 순서로 플러그인을 적용해도 실패하지 않아야 함 → 의존하는 플러그인이 있으면 명시적으로 적용하고, 선택적 연동은 존재 여부에 반응하도록 작성

```kotlin
// 필수 의존 플러그인은 직접 적용
project.pluginManager.apply("com.example.required-plugin")

// 선택적 연동은 존재할 때만 반응
project.pluginManager.withPlugin("com.example.other-plugin") { /* ... */ }
```

### 5. 내부(Internal) API를 사용하지 마라

- 패키지 경로에 `internal`이 포함되거나 타입 이름이 `Internal`/`Impl`로 끝나는 것은 모두 비공개 구현
- 이런 API는 마이너 버전에서도 예고 없이 변경될 수 있고, Android Gradle Plugin·Kotlin Gradle Plugin 같은 주요 플러그인도 이를 불안정하다고 간주
- 필요한 기능이 공개 API에 없으면 기능 요청을 제출하거나, 임시로 필요한 로직만 복사해 자체 구현으로 대체하는 것이 안전

하지 말아야 할 방식
```kotlin
import org.gradle.api.internal.attributes.AttributeContainerInternal
val badMap = (attributes as AttributeContainerInternal).asMap()
```

권장 방식
```kotlin
val goodMap = attributes.keySet().associate {
    Attribute.of(it.name, it.type) to attributes.getAttribute(it)
}
```

### 6. 빌드 플래그는 `gradle.properties`에 설정하라

- `org.gradle`로 시작하는 빌드 설정값은 커맨드라인 옵션이나 환경 변수 대신 루트 프로젝트의 `gradle.properties`에 적을 것
- 커맨드라인 지정은 일시적인 디버깅용일 뿐이고, 파일에 적어 두면 모든 개발자·CI 환경에서 동일하게 적용되며 형상 관리 대상이 됨. 단, 이 설정은 Composite Build 경계를 넘어 자동으로 상속되지 않음

```properties
# gradle.properties
org.gradle.continue=true
```

이렇게 하면 매번 `-D` 옵션을 기억해서 붙일 필요 없이 `gradle run`만으로 동일한 동작을 보장.

### 7. 루트 프로젝트 이름을 명시하라

- `settings.gradle(.kts)`를 비워 두면 루트 프로젝트 이름이 디렉터리 이름에서 자동으로 결정되는데, 디렉터리에 공백이나 특수문자가 있으면 문제가 생기고, 사람마다 클론 위치가 달라 이름이 흔들릴 수 있음
- 프로젝트 이름은 에러 메시지·로그·리포트에 계속 노출되므로 명시적으로 고정해 두는 편이 이해하기 쉬움

```kotlin
// settings.gradle.kts
rootProject.name = "my-example-project"
```

### 8. 서브프로젝트에 `gradle.properties`를 두지 마라

- 서브프로젝트 디렉터리 안에 개별 `gradle.properties`를 두는 방식은 Gradle과 주요 플러그인(Android Gradle Plugin, Kotlin Gradle Plugin)이 일관되게 지원하지 않음
- 설정 값이 여러 위치에 흩어지면 어디서 값이 덮어써지는지 추적하기 어려워짐

- 서브프로젝트 하나만 설정이 필요하면 그 프로젝트의 `build.gradle(.kts)`에 직접 정의
- 여러 서브프로젝트가 같은 설정을 공유해야 하면 Convention Plugin으로 뽑아내고, 각 서브프로젝트가 필요할 때 값을 오버라이드하게 만들 것

### 9. `afterEvaluate` 사용을 피하라

`project.afterEvaluate {}`로 태스크를 구성하거나 프로퍼티를 연결하거나 플러그인 적용에 반응하는 패턴은 세 가지 문제를 낳음.

1. 순서에 취약함: 여러 플러그인이 각자 `afterEvaluate`를 등록하면 실행 순서는 등록 순서를 따름 → 어떤 콜백은 아직 완전히 구성되지 않은 상태를 보게 될 수 있음
2. 태스크 구성 회피(Task Configuration Avoidance)를 무력화함: `afterEvaluate` 안에서 등록·구성된 태스크는 실행 여부와 무관하게 즉시(eager) 처리되어 불필요한 작업 발생
3. 설정 캐시와 호환되지 않음: 콜백이 캡처하는 가변 상태는 안정적으로 직렬화되지 않음

대안: 값 해석을 실행 시점으로 미루는 Lazy Property/Provider와, 플러그인 적용 여부에 안전하게 반응하는 `pluginManager.withPlugin()`을 사용. `afterEvaluate`는 필수 설정 여부를 조기에 검증하는 fail-fast 검사나 최종 구성 상태를 로깅하는 용도로만 좁게 허용.

하지 말아야 할 방식
```kotlin
class AppInfoPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("appInfo", AppInfoExtension::class.java)
        project.afterEvaluate {
            val name = extension.appName.getOrElse("unnamed")
            tasks.register("printAppInfo") { doLast { println("App: $name") } }
        }
    }
}
```
콜백 등록 순서가 실행 순서를 결정하고, 빌드 스크립트가 값을 설정하기 전에 프로퍼티를 읽어 버리며, 태스크가 즉시 등록됨.

권장 방식
```kotlin
class AppInfoPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("appInfo", AppInfoExtension::class.java)
        extension.appName.convention("unnamed")

        project.tasks.register("printAppInfo") {
            val name = extension.appName
            doLast { println("App: ${name.get()}") }
        }
    }
}
```
값은 `get()`이 호출되는 실행 시점에 해석되므로 구성 순서가 더 이상 문제되지 않고, 설정 캐시와도 호환됨.

### 핵심 포인트: 규칙들을 관통하는 원칙

- 선언적으로, 지연 평가로: `plugins {}` 블록, Lazy Property, `pluginManager.withPlugin()` 모두 "지금 당장 실행하지 말고 필요할 때 해석하라"는 같은 철학을 공유
- 순서 의존성을 걷어내라: 플러그인 적용 순서, `afterEvaluate` 등록 순서처럼 암묵적 순서에 기대는 로직은 프로젝트 구조가 조금만 바뀌어도 깨지기 쉬운 코드
- 공개 계약만 신뢰하라: 내부 API, 서브프로젝트별 `gradle.properties`처럼 문서화되지 않았거나 지원이 불안정한 경로는 피하고, `gradle.properties`·Convention Plugin·공개 API처럼 안정적인 경로로 설정을 모아야 유지보수가 쉬워짐

---

## 빌드 구조화 모범 사례 (Best Practices for Structuring Builds)

> 원문: https://docs.gradle.org/current/userguide/best_practices_structuring_builds.html

### 개요

Gradle 빌드를 어떻게 쪼개고 배치하느냐는 컴파일 회피, 병렬 실행, 캐시 적중률에 직접 영향을 줌. 이 문서는 "프로젝트를 어디에 두고, 빌드 로직을 어디에 모을지"에 대한 다섯 가지 원칙을 Do/Don't 형태로 정리.

### 원칙 1. 빌드는 최대한 여러 프로젝트로 쪼개라

하나의 거대한 프로젝트에 소스를 전부 몰아넣으면 파일 하나만 바뀌어도 전체를 다시 컴파일해야 하고, 태스크를 병렬로 돌릴 방법이 없음. 반대로 API/구현, 프런트엔드/백엔드, 기능 단위(수직 슬라이스), 코드 생성기/사용처처럼 경계를 나눠 별도 프로젝트로 만들면 변경분만 재컴파일되고 프로젝트별 컴파일 클래스패스도 최소화됨.

- Don't: `app` 프로젝트 하나에 `Main.java`, `Util.java`, `GuavaUtil.java`, `CommonsUtil.java`를 다 넣고 `application` 플러그인 하나로 전체를 컴파일 → Guava·commons-lang 의존성도 필요 없는 코드까지 다 보게 됨
- Do: `util`, `util-guava`, `util-commons`를 별도 프로젝트로 분리하고 `app`은 필요한 것만 `implementation(project(":util-guava"))` 식으로 참조

```kotlin
// util-commons/build.gradle.kts
plugins { `java-library` }
dependencies {
    api(project(":util"))
    implementation("commons-lang:commons-lang:2.6")
}
```

핵심 포인트: 원문은 "프로젝트 개수를 줄이기보다는 늘리는 쪽으로 판단하라"고 권장. 프로젝트를 잘게 쪼갤수록 Gradle이 변경 영향 범위를 정확히 계산해 불필요한 재컴파일과 순차 실행을 줄일 수 있기 때문.

### 원칙 2. 루트 프로젝트에 소스 파일을 두지 마라

루트 프로젝트는 빌드의 진입점이자 전역 설정·컨벤션을 모아두는 자리. 여기에 `src/main/java`를 직접 두고 `java-library` 같은 플러그인을 적용하면, 전역 설정과 특정 모듈의 컴파일 설정이 뒤섞여 버림.

- Don't: 루트 `build.gradle.kts`에 `java-library`를 적용하고 루트 아래 `src/main/java/...`에 소스를 직접 둠
- Do: 소스는 `core` 같은 별도 서브프로젝트로 옮기고, 루트는 `settings.gradle.kts`에서 `include("core")`만 선언한 채 비워 둠

핵심 포인트: 루트는 "빌드 전체를 조율하는 자리"로만 남겨 둬야 프로젝트가 커질 때 새 모듈을 추가하기 쉽고, 소스 전용 플러그인이 루트에 잘못 적용되는 사고를 막을 수 있음.

### 원칙 3. buildSrc보다 build-logic 컴포짓 빌드를 우선하라

`buildSrc`는 별도 등록 없이 자동으로 인식되어 편리하지만, 클래스로더가 메인 빌드와 다르게 동작해 예상치 못한 문제가 생기고, 파일이 바뀌면 무효화 범위가 넓으며, 독립적으로 개발·배포하기도 어려움. Settings 플러그인을 `buildSrc`에 두면 `pluginManagement` 블록에 포함시켜야 해서 빌드 캐시 활용도까지 떨어짐.

- Don't: 커스텀 플러그인·태스크를 `buildSrc/src/main/java`에 넣고 자동 인식에만 의존
- Do: `build-logic`이라는 별도의 완전한 Gradle 빌드(자체 `settings.gradle.kts` 포함)를 만들고, 루트에서 `includeBuild("build-logic")`으로 명시적으로 끌어옴

```kotlin
// settings.gradle.kts (root)
includeBuild("build-logic")
```

핵심 포인트: 컴포짓 빌드는 외부 의존성과 동일한 방식으로 명시적 의존 관계를 맺기 때문에, 변경 시 무효화 범위가 더 좁고 독립적으로 개발·게시 가능. 단, Settings 플러그인은 별도의 얇은 포함 빌드(예: `build-logic-settings`)로 분리하는 편이 낫고, `build-logic` 내부에 서브프로젝트가 지나치게 많고 플러그인 조합이 제각각이면 성능 저하나 Build Services 이슈가 생길 수 있다는 점도 원문이 짚음.

### 원칙 4. 의도치 않은 빈 프로젝트 생성을 피하라

`include(":subs:web:my-module")`처럼 경로에 콜론을 여러 개 넣으면, Gradle은 `:subs`, `:subs:web`, `:subs:web:my-module` 세 프로젝트를 전부 만듦. 앞의 두 개는 실제 `build.gradle.kts`가 없는 빈 프로젝트인데도 생성되어 `allprojects {}`/`subprojects {}` 설정에 영향을 받고, 리포트를 어지럽히고, 태스크 경로도 길어짐.

- Don't: `include(":subs:web:my-web-module")`만 쓰고 끝냄 → `:subs`, `:subs:web`이 빈 프로젝트로 딸려 생성됨
- Do: 짧은 이름으로 `include()`하고 `projectDir`을 실제 디렉터리로 직접 지정

```kotlin
include(":my-web-module")
project(":my-web-module").projectDir = file("subs/web/my-web-module")
```

핵심 포인트: 이렇게 하면 `gradle :subs:web:my-web-module:build` 대신 `gradle :my-web-module:build`처럼 짧은 경로로 태스크 실행 가능, 프로젝트 리포트에도 실제 존재하는 프로젝트만 나타나며 전역 설정이 불필요한 빈 프로젝트에 적용되는 것도 방지 가능.

### 원칙 5. 공통 빌드 로직은 컨벤션 플러그인으로 뽑아내라

여러 프로젝트의 `build.gradle.kts`에 자바 버전, 컴파일러 옵션, 테스트 설정 같은 코드가 그대로 복사·붙여넣기 되어 있으면, 하나를 고칠 때마다 전체 파일을 다 손봐야 함. 이런 반복 설정은 컨벤션 플러그인 하나로 뽑아내고, 각 프로젝트는 그 플러그인 id만 적용하면 됨.

- Don't: `project-a`, `project-b`의 `build.gradle.kts`에 `sourceCompatibility`, `JavaCompile` 옵션, `useJUnitPlatform()` 설정을 각각 똑같이 반복해서 적어 둠
- Do: `build-logic`에 `my.java-library.gradle.kts` 같은 컨벤션 플러그인을 만들어 공통 설정을 담고, 각 프로젝트는 `plugins { id("my.java-library") }`만 선언

```kotlin
// build-logic/src/main/kotlin/my.java-library.gradle.kts
plugins {
    id("my.base-java-library")
    id("my.java-use-junit5")
}
```

핵심 포인트: 컨벤션 플러그인은 원칙 3의 `build-logic` 컴포짓 빌드 안에 두는 것이 자연스러운 조합. 공통 동작을 한 곳에서 바꾸면 전체 프로젝트에 반영되고, 각 프로젝트 빌드 파일은 그 프로젝트만의 관심사(추가 의존성 등)로 간결하게 남음.

### 요약

- 모듈화: 소스를 한 프로젝트에 몰아넣기(하지 말 것) → 경계별로 프로젝트를 잘게 분리(할 것)
- 루트 구성: 루트에 소스·컴파일 플러그인 배치(하지 말 것) → 루트는 조율 전용, 소스는 서브프로젝트로(할 것)
- 빌드 로직 위치: `buildSrc` 자동 인식에 의존(하지 말 것) → `build-logic` 컴포짓 빌드로 명시적 포함(할 것)
- include 경로: 중첩 경로로 `include()`해 빈 프로젝트 생성(하지 말 것) → 짧은 이름 + `projectDir` 지정(할 것)
- 공통 설정: 프로젝트마다 설정 코드 복붙(하지 말 것) → 컨벤션 플러그인으로 추출·재사용(할 것)

---

## 태스크 작성 모범 사례 (Best Practices for Tasks)

> 원문: https://docs.gradle.org/current/userguide/best_practices_tasks.html

### 개요

Gradle 태스크는 편하게 쓰려고 하면 성능과 캐시 정확성을 갉아먹기 쉬운 함정이 많은 영역. 대부분의 문제는 "구성 단계(configuration phase)에서 값을 너무 일찍 확정해버린다"는 한 가지 패턴에서 비롯됨. 아래 항목들은 그 함정을 피하고 최신 상태(up-to-date) 검사·빌드 캐시·구성 캐시가 제대로 동작하도록 태스크를 설계하는 규칙들.

### 1. dependsOn은 라이프사이클 태스크에만 쓴다

- `dependsOn`은 "무조건 먼저 실행되어야 한다"는 거친(coarse-grained) 관계만 표현할 뿐, Gradle에게 "왜" 그 태스크가 필요한지는 알려주지 못함
- 액션이 있는 태스크 사이의 관계는 `dependsOn`이 아니라 입력·출력 와이어링으로 표현 필요 → 그래야 Gradle이 실제로 필요한 파일만 추적해 불필요한 재실행을 줄일 수 있음

```kotlin
// 지양: dependsOn만으로 관계를 표현
tasks.register<SimpleTranslationTask>("translateBad") {
    dependsOn(tasks.named("helloWorld"))
}

// 권장: 출력을 입력으로 직접 연결 (암묵적 의존성 생성)
tasks.register<SimpleTranslationTask>("translateGood") {
    inputs.file(tasks.named<SimplePrintingTask>("helloWorld").map { messageFile })
}
```

- `dependsOn`은 자신은 액션이 없고 하위 태스크들을 묶어주기만 하는 라이프사이클 태스크(`build`, `check` 등)에만 사용

### 2. 캐시 여부는 애너테이션으로, cacheIf는 지양한다

- 태스크 타입에 `@CacheableTask` 또는 `@DisableCachingByDefault`를 붙여 캐시 가능 여부를 타입 수준에서 고정
- 인스턴스마다 `outputs.cacheIf { ... }`를 호출하는 방식은 실수하기 쉽고(설정을 빼먹으면 캐시가 적용되지 않음), 런타임에 조건을 매번 평가해야 해서 비효율적

```kotlin
// 지양: 인스턴스별로 캐시 조건 설정 (누락 위험)
tasks.register<BadCalculatorTask>("addBad1") {
    outputs.cacheIf { true }
}
tasks.register<BadCalculatorTask>("addBad2") {
    // 캐시 설정을 깜빡함
}

// 권장: 타입에 애너테이션 부여
@CacheableTask
abstract class GoodCalculatorTask : DefaultTask() { /* ... */ }
```

### 3. 구성 단계에서 Provider.get()을 호출하지 않는다

- `get()`은 값을 즉시 확정시키는 호출 → 구성 단계에서 호출하면 아직 최종 값이 아닌 상태를 읽어버리거나, 구성 캐시 미스를 유발하거나, Gradle이 자동으로 추적해줄 태스크 간 의존성 연결이 끊어질 수 있음
- 값을 가공해야 하면 `map()`/`flatMap()`으로 지연 변환을 걸어두고, 실제 실행 시점에 읽히도록 함

```kotlin
// 지양: 구성 시점에 즉시 값 확정
tasks.register<MyTask>("avoidThis") {
    myInput = "currentEnvironment=${currentEnvironment.get()}"
}

// 권장: map()으로 지연 평가
tasks.register<MyTask>("doThis") {
    myInput = currentEnvironment.map { "currentEnvironment=$it" }
}
```

### 4. 커스텀 태스크에는 group과 description을 채운다

- `group`은 짧고 소문자로, 태스크의 목적(문서화, 검증, 릴리스, 배포 등)을 나타낼 것
- 이 정보는 `./gradlew tasks` 결과에 그대로 노출됨. `group`이 없는 태스크는 `--all` 옵션을 주지 않으면 목록에서 숨겨지고, 있어도 "Other tasks"처럼 애매한 분류에 묻혀 찾기 어려움

```kotlin
// 지양: 분류 정보 없음 → "Other tasks"에 묻힘
tasks.register("generateDocs") { /* ... */ }

// 권장: 그룹과 설명 명시 → "Documentation tasks"에 정리됨
tasks.register("generateDocs") {
    group = "documentation"
    description = "소스 파일로부터 프로젝트 문서를 생성합니다."
}
```

### 5. FileCollection/Configuration에 즉시(eager) API를 쓰지 않는다

- `Configuration`이나 `FileCollection`에서 `.size()`, `.isEmpty()`, `.getFiles()`, `.asPath()`, `.toList()` 같은 메서드를 구성 단계에서 호출하면, 그 순간 의존성 해석(resolution)이 강제로 일어남
- 이는 구성 캐시 미스, 너무 이른 시점의 잘못된 평가, 태스크 간 자동 의존성 연결 붕괴로 이어짐

```kotlin
// 지양: isEmpty() 호출이 구성 단계에 해석을 강제함
tasks.register<FileCounterTask>("badCountingTask") {
    if (!configurations.runtimeClasspath.get().isEmpty()) {
        countMe.from(configurations.runtimeClasspath)
    }
}

// 권장: 지연 프로퍼티에 그대로 할당, 해석은 실행 시점으로 미룸
tasks.register<FileCounterTask>("goodCountingTask") {
    countMe.from(configurations.runtimeClasspath)
}
```

### 6. 태스크 실행 전에 Configuration을 미리 resolve()하지 않는다

- `resolve()`를 호출해 얻은 파일 집합은 "어떤 태스크가 이 파일을 만들었는지"에 대한 참조 정보를 잃어버림
- 그 결과 소비자(consumer) 태스크와 생산자(producer) 태스크 사이의 암묵적 의존성이 끊어져, 실행 순서가 보장되지 않고 파일이 아직 생성되지 않은 채로 참조될 수 있음

```kotlin
// 지양: resolve()로 즉시 해석 → 암묵적 의존성 소실
tasks.register("badClasspathPrinter", BadClasspathPrinter::class) {
    classpath = configurations.named("runtimeClasspath").get().resolve()
}

// 권장: ConfigurableFileCollection에 지연 연결
tasks.register("goodClasspathPrinter", GoodClasspathPrinter::class.java) {
    classpath.from(configurations.named("runtimeClasspath"))
}
```

### 7. 경로 민감도(PathSensitivity)를 상황에 맞게 낮춘다

- 태스크는 보통 입력 파일의 내용에만 관심이 있을 뿐, 그 파일이 디스크의 어느 경로에 있는지는 중요하지 않음
- 기본값인 `ABSOLUTE`는 경로 전체를 캐시 키에 포함시켜, 다른 머신이나 다른 체크아웃 위치에서는 빌드 캐시·구성 캐시가 전혀 재사용되지 않게 만듦
- 단일 파일 입력에는 `PathSensitivity.NONE`을, 디렉터리 입력에는 상대 구조가 의미 있는 경우 `PathSensitivity.RELATIVE`를 지정

```kotlin
// 지양: 절대 경로까지 캐시 키에 포함됨
@InputFile
@PathSensitive(PathSensitivity.ABSOLUTE)
abstract val candidatesFile: RegularFileProperty

// 권장: 내용만 비교, 경로가 달라도 UP-TO-DATE 유지
@InputFile
@PathSensitive(PathSensitivity.NONE)
abstract val candidatesFile: RegularFileProperty
```

### 8. 출력 파일/디렉터리는 태스크마다 겹치지 않게 한다

- 여러 태스크가 같은 출력 디렉터리를 공유하면, 한 태스크가 그 디렉터리에 쓴 결과가 다른 태스크의 출력과 뒤섞여 "겹치는 출력(overlapping output)" 발생
- 이 경우 실제로 바뀐 파일이 없어도, 같은 디렉터리를 쓰는 다른 태스크가 실행되었다는 사실만으로 소비자 태스크가 무효화되어 불필요하게 재실행됨

```kotlin
// 지양: greeterA, greeterB가 같은 디렉터리를 출력으로 공유
val greeterA = tasks.register<GreetingTask>("greeterA") {
    outputDirectory = layout.buildDirectory.dir("greetings")
}
tasks.register<GreetingTask>("greeterB") {
    outputDirectory = layout.buildDirectory.dir("greetings")
}
// consumer는 greeterA의 출력만 쓰지만, greeterB 실행만으로도 무효화됨

// 권장: 각 태스크가 자신만의 출력 파일을 갖도록 분리
val greeterA = tasks.register<GreetingTask>("greeterA") {
    outputFile = layout.buildDirectory.dir("greetings").map { it.file("a.txt") }
}
tasks.register<GreetingTask>("greeterB") {
    outputFile = layout.buildDirectory.dir("greetings").map { it.file("b.txt") }
}
```

### 핵심 포인트

- 태스크 간 관계는 `dependsOn`이 아니라 입력·출력 와이어링으로 표현해야 Gradle이 정확한 최신 상태 검사 가능
- 캐시 가능 여부(`@CacheableTask`)는 인스턴스가 아니라 타입에 선언하고, `Provider.get()`이나 `Configuration.resolve()` 같은 즉시 평가 API는 구성 단계에서 호출 금지
- `group`/`description`으로 태스크를 문서화하고, `@PathSensitivity`를 적절히 낮춰 캐시 재사용성(relocatability) 확보
- 태스크마다 고유한 출력 파일/디렉터리를 갖도록 설계해야 불필요한 재실행과 캐시 무효화 방지 가능

---

## Gradle 의존성 관리 모범 사례

> 원문: https://docs.gradle.org/current/userguide/best_practices_dependencies.html

### 개요

Gradle 공식 문서가 정리한 의존성 관리 모범 사례 목록임. 버전을 어디서 관리할지, 의존성을 어떻게 선언할지, 저장소를 어디에 설정할지 등 실무에서 자주 실수하는 지점들을 Do/Don't 형태로 정리.

### 1. 버전은 버전 카탈로그 한곳에 모은다

- `project.ext`, 커스텀 상수, 지역 변수로 버전을 흩어 관리하지 말고 `gradle/libs.versions.toml` 파일에 중앙화
- 버전을 올릴 때 여러 `build.gradle(.kts)` 파일을 돌아다니며 고칠 필요 없이 TOML 한 곳만 수정하면 됨
- `bundles`를 이용하면 자주 같이 쓰는 라이브러리 묶음을 하나의 별칭으로 선언 가능

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

#### 핵심 포인트

멀티 모듈 빌드에서 버전 드리프트(모듈마다 버전이 조금씩 달라지는 문제)를 막는 가장 확실한 방법이 버전 카탈로그 중앙화.

### 2. 카탈로그 항목 이름을 일관되게 짓는다

- 언더스코어 대신 대시(`-`)로 세그먼트를 구분
- 최상위 도메인은 빼고 group ID에서 첫 세그먼트를 뽑아냄
- 대시로 구분된 이름은 접근 시 카멜케이스로 변환됨. 예: `spring-boot-starter-web` → `libs.spring.boot.starter.web`
- `foo`, `bar` 같은 모호한 이름은 피하고, 플러그인 라이브러리에는 `-plugin` 접미사를 붙일 것

#### 핵심 포인트

이름이 일관돼야 자동완성과 검색이 편해지고, 다른 개발자가 카탈로그만 보고도 어떤 라이브러리인지 짐작 가능.

### 3. 저장소는 build 파일이 아니라 settings 파일에 설정한다

- `pluginManagement {}`와 `dependencyResolutionManagement {}` 블록을 `settings.gradle.kts`에 두고, 개별 `build.gradle.kts`에는 저장소를 선언하지 않음
- 저장소는 프로젝트 정의의 일부가 아니라 빌드 전역 로직에 속하는 설정이기 때문

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

#### 핵심 포인트

settings 파일에 모아두면 반복 선언을 피하고, `FAIL_ON_PROJECT_REPOS`로 서브프로젝트가 몰래 다른 저장소를 추가하는 것도 막아 디버깅이 쉬워짐.

### 4. Kotlin 표준 라이브러리는 명시적으로 선언하지 않는다

- `kotlin("stdlib")`을 직접 추가하지 않음. Kotlin Gradle Plugin이 알아서, 그리고 항상 일치하는 버전으로 추가해줌
- stdlib 버전을 수동으로 관리해야 하는 특수한 경우에만 `kotlin.stdlib.default.dependency = false`를 설정

```kotlin
plugins {
    kotlin("jvm").version("2.3.21")
}
// api(kotlin("stdlib")) 같은 선언은 불필요하다
```

### 5. 같은 의존성을 여러 컨피규레이션에 중복 선언하지 않는다

- 같은 좌표를 `api`와 `implementation`, 혹은 `compileOnly`와 `implementation`에 동시에 선언하지 않음
- 중복 선언은 관리를 어렵게 만들고, 원인을 찾기 힘든 클래스패스 문제로 이어질 수 있음

```kotlin
// Don't
dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.0") // 중복
}
```

### 6. 의존성은 단일 GAV 문자열로 선언한다

- `implementation("org.example:library:1.0")`처럼 `"group:name:version"` 한 줄로 작성
- `group = ...`, `name = ...`, `version = ...` 형태의 명명된 인자(맵) 표기법은 지원 중단되었고 Gradle 10부터는 사용 불가

```kotlin
// Do
implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")

// Don't
implementation(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = "2.17.0")
```

#### 핵심 포인트

단일 문자열 표기법이 더 간결하고, JVM 생태계 전반에서 표준으로 쓰임.

### 7. 저장소가 여러 개면 콘텐츠 필터링을 적용한다

- `includeGroupByRegex()` 등으로 각 저장소가 어떤 그룹의 의존성만 제공할 수 있는지 명시
- 얻는 이점은 세 가지
  - 성능: 실제로 존재할 가능성이 있는 저장소에만 질의
  - 보안: 모든 저장소에 무분별하게 요청을 보내지 않음
  - 신뢰성: 잘못된 메타데이터를 가진 저장소를 건너뜀

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

### 8. 제외(exclude)는 최대한 좁은 범위로 적용한다

- 제외는 컨피규레이션 전체가 아니라 특정 의존성에 붙임
- 그룹 전체가 아니라 개별 모듈 단위로 제외
- `configurations.all { }`이나 `configurations.configureEach { }`를 통한 전역 제외는 지양

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

#### 핵심 포인트

전역 제외는 성능에 영향을 주고, 빌드의 다른 곳에서 필요한 의존성을 모르게 걷어내거나 런타임 충돌을 유발할 위험이 있음.

### 정리

- 버전은 카탈로그(`libs.versions.toml`) 한곳에, 저장소는 `settings.gradle.kts` 한곳에 모음
- 카탈로그 항목 이름은 대시 기반으로 일관되게 지음
- Kotlin stdlib처럼 플러그인이 자동으로 넣어주는 의존성은 직접 선언하지 않음
- 같은 의존성을 여러 컨피규레이션에 중복 선언하지 않고, 표기법은 단일 GAV 문자열로 통일
- 저장소가 여러 개면 콘텐츠 필터링으로 범위를 좁히고, 제외(exclude)는 가능한 한 좁게 적용

---

## Gradle 테스트 모범 사례 (Best Practices for Testing)

> 원문: https://docs.gradle.org/current/userguide/best_practices_testing.html

### 개요

커스텀 태스크와 플러그인은 대개 `build.gradle(.kts)` 안에 인라인으로 작성하며 프로토타이핑을 시작함. 기능이 안정되면 `buildSrc`나 별도의 플러그인 프로젝트로 뽑아내야 하는데, 그 순간부터 TestKit을 이용한 기능 테스트(functional test)가 가능해짐. 이 문서는 단 하나의 규칙, "커스텀 태스크·플러그인은 TestKit으로 테스트하라"를 하나의 예시로 깊게 다룸.

### 규칙: 커스텀 태스크·플러그인은 TestKit으로 검증하라

- Do: 코드가 `build.gradle` 밖으로(=`buildSrc` 또는 독립 플러그인 프로젝트로) 나가는 즉시, TestKit 기반 기능 테스트를 세트로 갖출 것
- Do: 성숙한 빌드는 커스텀 타입이 "선언대로" 동작함을 보장하는 기능 테스트를 반드시 포함
- Don't: 인라인 상태로 남겨둔 채 "동작하는 것처럼 보이니 괜찮다"고 넘기지 않음 → 캐시 가능성·입력 선언·와이어링 실수는 눈으로 봐서는 잘 드러나지 않음

### Don't: 문제가 있는 예시

아래처럼 `build.gradle.kts`에 인라인으로 정의한 태스크·플러그인에는 테스트 없이는 잡기 힘든 버그가 네 가지 섞여 있음.

```kotlin
@CacheableTask                       // (1) 매번 다른 결과인데 캐시 가능 표시
abstract class MyTask : DefaultTask() {
    private final val today = Instant.now()   // (2) 선언되지 않은 입력값
    // ...
}

project.tasks.withType<MyTask>().configureEach {
    lastName.convention(extension.firstName)   // (3) firstName을 잘못 연결
    // ...
}

tasks.named<MyTask>("task2") {
    greeter = "Bonjour"               // (4) 태스크 프로퍼티가 아닌 빌드스크립트 변수에 대입
}
```

1. 비결정적 출력에 `@CacheableTask`: 실행 시각에 따라 결과가 달라지는데도 캐시 대상으로 표시
2. 현재 시각을 선언되지 않은 입력으로 사용: `@Input`으로 선언하지 않으면 Gradle이 변경 여부를 추적 불가
3. 프로퍼티 와이어링 실수: `lastName`이 확장의 `firstName`에 연결됨
4. 엉뚱한 변수에 대입: 태스크의 `greeting` 프로퍼티가 아니라 빌드스크립트 지역 변수 `greeter`에 값을 넣음

이런 실수는 코드 리뷰만으로 발견하기 어렵고, 기능 테스트를 작성하는 과정에서 자연스럽게 드러남.

### Do: build-logic + functionalTest 구조로 분리

- Do: 커스텀 타입을 `build-logic`이라는 포함 빌드(composite build)로 뽑아내고, `main`과 별도로 `functionalTest` 소스셋을 둘 것
- Do: 시간처럼 비결정적인 값은 `@Input`으로 선언하고, 플러그인 설정(`configureEach`) 안에서 `Instant.now()`를 단 한 번만 호출해 모든 태스크에 동일한 값을 줄 것
- Do: 프로퍼티 와이어링(`lastName.convention(extension.getLastName())`)이 실제 의도한 필드를 가리키는지 테스트로 고정

```
build-logic/
├── src/main/java/...        (MyTask, MyPlugin 본체)
└── src/functionalTest/java/... (MyPluginFunctionalTest)
```

TestKit 테스트는 느리고 무거운 편이라 단위 테스트와 뒤섞지 않고 `functionalTest`라는 전용 소스셋으로 분리하는 것이 관례.

### functionalTest 소스셋 구성

`JvmTestSuite`로 소스셋을 만들고 `gradlePlugin`에 등록해야 `java-gradle-plugin`이 플러그인 코드를 테스트 클래스패스에 자동으로 얹어줌.

```kotlin
val functionalTest = testing.suites.register("functionalTest", JvmTestSuite::class) {
    useJUnitJupiter()
    dependencies {
        implementation(project())
        implementation(gradleTestKit())
    }
}

tasks.check { dependsOn(functionalTest) }

gradlePlugin {
    testSourceSets(functionalTest.get().sources)
}
```

- Don't: `build-logic`(포함 빌드)의 테스트가 루트 프로젝트의 `check`를 실행하면 같이 돌 것이라고 가정하지 않음 → 포함 빌드는 산출물만 제공할 뿐 내부 테스트는 자동 실행되지 않으므로, `./gradlew :build-logic:check`처럼 명시적으로 호출 필요

### 기능 테스트 시나리오 네 가지

`GradleRunner`로 임시 프로젝트를 만들어 실제 빌드를 돌리며, 아래 네 관점을 각각 검증.

- `testTaskRegistration`: `tasks --all` 결과에 태스크가 의도한 그룹·이름으로 나타나는지 확인
- `testTaskExecution`: 태스크를 실행하면 출력 파일에 기대한 내용이 쓰이는지 확인
- `testTaskDeterminism`: 같은 입력으로 두 번 실행(`--rerun-tasks`)해도 출력이 동일한지 확인
- `testTaskCacheability`: 빌드 캐시를 켠 채 두 번째 실행이 `FROM_CACHE`로 뜨는지 확인

```java
BuildResult result = GradleRunner.create()
    .withProjectDir(testProjectDir)
    .withPluginClasspath()
    .withArguments("--build-cache", "task1")
    .build();

assertEquals(TaskOutcome.FROM_CACHE, result.task(":task1").getOutcome());
```

캐시 검증은 여기서 더 나아가, 입력값을 바꿨을 때 실제로 재실행되는지(relocatability 포함)까지 확인하면 더 견고해짐.

### 핵심 포인트

- 커스텀 타입은 "인라인 프로토타입 → `buildSrc`/독립 플러그인으로 추출 → TestKit 기능 테스트 추가"라는 자연스러운 성숙 경로를 따름
- 캐시 가능(`@CacheableTask`) 선언, 입력값의 `@Input` 명시, 프로퍼티 와이어링, 변수 대입 실수는 코드만 봐서는 놓치기 쉽고 기능 테스트가 가장 효과적으로 잡아냄
- 기능 테스트는 무겁기 때문에 `functionalTest` 같은 전용 소스셋으로 분리하고, `java-gradle-plugin`의 `testSourceSets`에 등록해 클래스패스를 자동으로 연결
- 포함 빌드(`includeBuild`)의 테스트는 루트 프로젝트 `check`에 묶이지 않으므로 `:build-logic:check`처럼 명시적으로 실행 필요
- 등록 여부, 실행 결과, 결정성(determinism), 캐시 가능성(cacheability)까지 네 겹으로 검증하는 것이 TestKit 기반 테스트의 기본 틀

---

## Gradle 성능 모범 사례

> 원문: https://docs.gradle.org/current/userguide/best_practices_performance.html

### 개요

Gradle 공식 문서는 빌드 속도를 갉아먹는 흔한 실수 다섯 가지를 모범 사례로 정리해둠. 배포판 선택, 인코딩 설정처럼 한 번 고쳐두면 끝나는 설정값도 있고, 빌드 캐시·구성 캐시처럼 켜기만 하면 되는 기능도 있음. 마지막 하나는 빌드 스크립트를 작성하는 습관 자체에 관한 것.

- 배포판(`-bin` vs `-all`) 선택
- 파일 인코딩(UTF-8) 고정
- 빌드 캐시 활성화
- 구성 캐시 활성화
- 구성 단계에서 무거운 연산 피하기

### 1. Gradle 배포판은 `-bin`을 쓴다

`gradle-wrapper.properties`의 `distributionUrl`은 흔히 `-all` 배포판을 가리키도록 설정되는데, 대부분의 경우 이는 낭비.

- `-bin`: 실행에 필요한 바이너리만 포함, 다운로드 용량 작음 → 검증·CI 시간 단축
- `-all`: 바이너리 + 소스 + 문서 포함, 용량 큼 → IDE에서 소스를 미리 받아두고 싶을 때만 의미 있음

```properties
# Bad: 매번 큰 zip을 내려받는다
distributionUrl=https\://services.gradle.org/distributions/gradle-9.0-all.zip

# Good
distributionUrl=https\://services.gradle.org/distributions/gradle-9.0-bin.zip
```

핵심 포인트
- 최신 IDE는 소스/문서를 필요할 때 별도로 내려받을 수 있으므로, `-all`을 미리 준비해둘 이유가 거의 없음
- CI처럼 매번 새 환경에서 배포판을 내려받는 상황일수록 `-bin`의 이득이 큼

### 2. 파일 인코딩을 UTF-8로 고정한다

JVM의 기본 파일 인코딩은 OS·로케일에 따라 달라짐. 이 값이 빌드마다 다르면, 같은 소스인데도 컴파일 태스크의 입력 해시가 달라져 캐시가 어긋날 수 있음.

```properties
# gradle.properties
org.gradle.jvmargs=-Dfile.encoding=UTF-8
```

핵심 포인트
- 인코딩을 명시하지 않으면 "내 로컬에서는 캐시가 잘 맞는데 CI에서는 매번 다시 컴파일된다" 같은 플랫폼 종속적인 캐시 미스가 생길 수 있음
- 팀 전체, CI 서버까지 포함해 동일한 값으로 고정하는 것이 중요

### 3. 빌드 캐시를 켠다

빌드 캐시는 기본적으로 꺼져 있음. 입력이 같은 태스크의 출력을 재사용해 재실행을 건너뛰게 하는 기능이므로, 켜두지 않으면 그 이득을 아예 얻지 못함.

```properties
# gradle.properties
org.gradle.caching=false   # 기본값 (비활성)
org.gradle.caching=true    # 권장
```

활성화하면 조건이 맞는 태스크가 실행 대신 `FROM-CACHE`로 표시됨.

핵심 포인트
- 로컬 개발, CI 파이프라인 모두에서 켜두는 것이 기본값처럼 취급되어야 함
- 캐시 효과를 제대로 보려면 태스크의 입력/출력 선언이 정확해야 한다는 전제는 그대로 유지됨

### 4. 구성 캐시를 켠다

빌드 캐시가 "태스크 실행 결과"를 저장하는 반면, 구성 캐시는 "구성 단계 결과(태스크 그래프)" 자체를 저장. 두 캐시는 서로 독립적으로 동작하며, 어느 하나만 켜도 되고 둘 다 켜도 됨.

```properties
# gradle.properties
org.gradle.configuration-cache=false   # 기본값 (비활성)
org.gradle.configuration-cache=true    # 권장
```

- 첫 실행: `Configuration cache entry stored`
- 이후 입력이 같으면: `Configuration cache entry reused` → 구성 단계 자체를 건너뜀

핵심 포인트
- 구성 캐시가 재사용되면 스크립트 평가, 플러그인 적용 등 구성 단계 전체를 스킵하므로 특히 태스크 수가 많은 대형 프로젝트에서 효과가 큼
- 빌드 캐시 = 태스크 출력 재사용, 구성 캐시 = 태스크 그래프(구성 결과) 재사용. 이름이 비슷해 혼동하기 쉬우니 구분해서 기억 필요

### 5. 구성 단계에서 무거운 연산을 하지 않는다

Gradle 빌드는 두 단계로 나뉨. 구성 단계는 태스크 그래프를 결정하기 위해 어떤 태스크를 실행하든 상관없이 항상 도는 부분이고, 실행 단계는 실제로 선택된 태스크만 도는 부분. 파일 I/O, 네트워크 호출, CPU 연산처럼 비용이 큰 작업을 구성 단계에 두면, 그 태스크를 실행할 필요가 없는 빌드에서도 매번 비용을 치르게 됨.

```kotlin
// Bad: heavyWork()가 구성 단계에서 즉시 실행됨
tasks.register<MyTask>("myTask") {
    computationResult = heavyWork()
}

// Good: heavyWork()는 태스크가 실제로 실행될 때만 호출됨
abstract class MyTask : DefaultTask() {
    @TaskAction
    fun run() {
        logger.lifecycle(heavyWork())
    }
}
tasks.register<MyTask>("myTask")
```

핵심 포인트
- `tasks.register { ... }` 블록 안에 직접 쓴 코드는 구성 시점에 실행된다는 점을 항상 의식할 것
- 무거운 로직은 `@TaskAction`이 붙은 실행 메서드 안으로 옮겨서, 해당 태스크가 실제로 선택됐을 때만 비용을 치르게 만들 것
- 이 습관은 구성 캐시(4번)의 효과와도 맞물림 → 구성 단계가 가벼울수록 구성 캐시 저장/복원도 빨라짐

### 정리

- 배포판: 기본값이 `-all`인 경우 있음 → 권장값 `-bin` (다운로드/검증 시간 단축)
- 파일 인코딩: 기본값 플랫폼 종속 → 권장값 UTF-8 고정 (캐시 미스 방지)
- 빌드 캐시: 기본값 비활성 → 권장값 활성 (태스크 재실행 생략)
- 구성 캐시: 기본값 비활성 → 권장값 활성 (구성 단계 자체 생략)
- 구성 단계 로직: 기본값 즉시 실행 코드 허용 → 권장값 `@TaskAction`으로 지연 (불필요한 연산 방지)

다섯 항목 모두 "한 번 설정해두면 계속 이득을 보는" 성격이 강하므로, 신규 프로젝트를 세팅할 때 체크리스트로 삼아둘 것.

---

## Gradle 보안 모범 사례 (Best Practices for Security)

> 원문: https://docs.gradle.org/current/userguide/best_practices_security.html

### 개요

Gradle Wrapper는 빌드 로직이 실행되기 전에 먼저 동작하는 컴포넌트임. 즉, 프로젝트가 정의한 어떤 보안 장치보다도 먼저 신뢰할 수 없는 코드가 실행될 여지가 있는 지점. 이 문서는 그 위험을 줄이기 위한 두 가지 핵심 규칙, "Gradle 배포판 체크섬 검증"과 "Wrapper 업그레이드 시 무결성 검증"을 다룸.

### 규칙 1: Gradle 배포판의 SHA-256 체크섬을 검증하라

- Do: `gradle-wrapper.properties`에 `distributionSha256Sum` 속성을 함께 선언해, 다운로드한 Gradle 배포판이 실제로 공식 배포본과 동일한지 확인
- Don't: `distributionUrl`만 지정하고 체크섬 검증 없이 배포판을 그대로 받아 실행하지 않음 → 이 경우 손상되었거나 변조된 Gradle 실행 파일을 그대로 받아들이게 됨

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.6-bin.zip
distributionSha256Sum=<gradle.org에 공시된 SHA-256 값>
```

#### 핵심 포인트

- 체크섬 값은 Gradle 공식 릴리스 페이지(release-checksums)에 게시된 값을 그대로 가져와야 하며, 임의로 생성하거나 추정하는 것은 금지
- 이 설정은 "다운로드한 zip 파일 == 공식 배포판"이라는 사실만 보장할 뿐, Wrapper JAR 자체의 무결성까지 보장하지는 않음. 그래서 규칙 2가 별도로 필요

### 규칙 2: Wrapper를 업그레이드할 때마다 무결성을 검증하라

Wrapper 업그레이드는 단순히 버전 문자열을 바꾸는 작업이 아니라, 빌드보다 먼저 실행되는 실행 파일을 새로 신뢰하는 행위. 따라서 다음 두 파일을 매번 점검 대상으로 삼을 것.

- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties` (`distributionUrl`, `distributionSha256Sum`)

#### 검증 체크리스트

- Do: JAR 파일이 공식 Gradle 바이너리와 변경 없이 동일한지, 그 체크섬이 `gradle.org/release-checksums`에 공시된 값과 일치하는지 확인
- Do: `distributionUrl`이 실제로 의도한 Gradle 릴리스를 가리키는지, `distributionSha256Sum`이 그 릴리스 버전에 맞는 값인지 함께 확인
- Don't: PR이나 외부 기여로 들어온 Wrapper 관련 파일 변경을 체크섬 대조 없이 그대로 머지하지 않음 → 검증되지 않은 Wrapper를 실행하면 빌드의 다른 안전장치가 작동하기도 전에 신뢰할 수 없는 코드가 실행될 수 있음

#### 핵심 포인트: 자동화

- 매번 수동으로 대조하는 대신 CI에서 자동 검증하는 것이 권장됨
- GitHub Actions를 사용한다면 `setup-gradle` 액션(v4 이상)이 모든 `gradle-wrapper.jar`를 검사해, 알려지지 않은 JAR가 발견되면 빌드를 실패시켜 줌
- 별도의 Gradle Wrapper 검증 액션(Wrapper Validation Action)을 붙이는 방법도 있음

### 두 규칙의 관계

- 규칙 1은 "배포판 zip 파일"의 무결성, 규칙 2는 "Wrapper JAR + 설정 파일"의 무결성을 다룬다는 점에서 검증 대상이 다름
- 두 검증 모두 통과해야 비로소 "내가 실행하는 Gradle이 공식적이고 변조되지 않았다"는 공급망(Supply Chain) 신뢰 사슬이 완성됨
- 신규 프로젝트를 셋업할 때뿐 아니라, Wrapper 버전을 올릴 때마다(`./gradlew wrapper --gradle-version ...`) 반복적으로 점검해야 하는 항목

---

## Gradle 모범 사례 체크리스트 (Best Practices Index)

> 원문: https://docs.gradle.org/current/userguide/best_practices_index.html

### 개요

Gradle 공식 문서가 "일반, 빌드 구조화, 의존성, 태스크, 성능, 보안, 테스트" 7개 범주로 정리해 둔 모범 사례 목록. 개별 규칙 하나하나가 어렵지는 않지만, 실제 프로젝트에서 놓치기 쉬운 항목들이라 체크리스트 형태로 정리.

### 일반 (General)

- Kotlin DSL을 사용하라: Groovy DSL보다 타입 안정성과 IDE 자동완성이 뛰어나므로 신규 빌드는 Kotlin DSL(`build.gradle.kts`)로 작성

  ```kotlin
  plugins {
      id("java")
      id("org.springframework.boot") version "3.3.0"
  }
  ```

- Gradle의 최신 마이너 버전을 유지하라: 메이저 업그레이드가 아니어도 마이너 버전 업데이트만으로 버그 수정과 성능 개선 수령 가능
- 플러그인은 `plugins {}` 블록으로 적용하라: `apply plugin:` 같은 레거시 방식 대신 `plugins {}` 블록을 사용해야 버전 관리와 클래스패스 해석이 안전해짐
- Gradle 내부 API를 사용하지 마라: `internal` 패키지나 문서화되지 않은 API는 버전 간 예고 없이 바뀔 수 있음
- 빌드 플래그는 `gradle.properties`에 설정하라: JVM 힙, 병렬 실행 등 환경 설정은 스크립트가 아닌 `gradle.properties`에 두어 환경별 일관성 유지
- 루트 프로젝트에 이름을 명시하라: `settings.gradle(.kts)`에서 `rootProject.name`을 지정해 멀티 프로젝트 빌드에서 프로젝트를 명확히 구분
- 서브프로젝트에는 `gradle.properties`를 두지 마라: 설정이 여러 곳에 흩어지면 우선순위 혼란이 생기므로 루트에만 둘 것
- `afterEvaluate`를 남용하지 마라: 평가 순서에 의존하는 코드는 디버깅이 어렵고 성능도 떨어지므로 최소화

### 빌드 구조화 (Structuring Builds)

- 빌드를 모듈화하라: 하나의 거대한 프로젝트 대신 기능 단위로 서브프로젝트를 나누면 병렬 빌드와 캐시 재사용성이 좋아짐
- 루트 프로젝트에 소스 파일을 두지 마라: 루트는 조율(coordination) 역할만 하고, 실제 소스 코드는 서브프로젝트에 둘 것
- 공용 빌드 로직은 `build-logic` 컴포지트 빌드로 분리하라: `buildSrc`나 스크립트 복붙 대신 별도의 컴포지트 빌드로 관리하면 재사용과 테스트가 쉬워짐
- 의도치 않은 빈 프로젝트 생성을 피하라: `include`로 등록했지만 실제 코드가 없는 프로젝트는 빌드 구조를 불필요하게 복잡하게 만듦
- 컨벤션 플러그인을 사용하라: 여러 서브프로젝트에 반복되는 설정은 컨벤션 플러그인으로 뽑아내 한 곳에서 관리

### 의존성 관리 (Dependencies)

- 단일 GAV 문자열을 사용하라: `group`, `name`, `version`을 따로 쓰지 않고 `"group:artifact:version"` 한 줄로 표기해 가독성을 높임
- 버전 카탈로그로 의존성 버전을 중앙화하라: `libs.versions.toml`에 버전을 모아두면 여러 모듈 간 버전 불일치 방지 가능

  ```toml
  [versions]
  junit = "5.10.2"

  [libraries]
  junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
  ```

- 버전 카탈로그 항목 이름은 명확하게 지어라: 알파벳 순 정렬이 아니라 용도가 드러나는 이름을 붙여 검색성을 높임
- 의존성 저장소는 Settings 파일에 설정하라: `dependencyResolutionManagement`를 `settings.gradle(.kts)`에 두어야 초기화 시점부터 저장소가 확정됨
- `kotlin-stdlib`를 명시적으로 의존성에 추가하지 마라: Kotlin 플러그인이 자동으로 포함시켜 주므로 중복 선언
- 중복 의존성 선언을 피하라: 같은 라이브러리를 여러 곳에서 다른 버전으로 선언하면 카탈로그나 플랫폼으로 통합
- 여러 저장소 사용 시 콘텐츠 필터링을 적용하라: 어떤 저장소가 어떤 그룹의 의존성을 제공하는지 명시하면 불필요한 네트워크 조회 감소
- `exclude`는 좁은 범위에만 적용하라: 전역 제외 대신 특정 의존성에만 국한된 제외 규칙을 써서 의도치 않은 부작용 방지

### 태스크 (Task)

- `dependsOn` 사용을 최소화하라: 태스크 간 순서만 강제하는 `dependsOn` 대신 입력/출력을 연결하면 캐시와 병렬화가 제대로 동작
- `cacheIf`/`doNotCacheIf` 대신 `@CacheableTask`, `@DisableCachingByDefault` 애너테이션을 써라: 조건부 메서드보다 애너테이션이 캐싱 의도를 더 명확히 드러냄
- 커스텀 태스크에는 `group`과 `description`을 지정하라: `gradle tasks` 출력에서 잘 분류되고 검색되도록 함
- 태스크 액션 밖에서 `Provider.get()`을 호출하지 마라: 설정 단계에서 값을 즉시 꺼내면 지연 평가(lazy evaluation)의 이점이 사라짐
- 태스크 실행 전에 `Configuration`을 resolve하지 마라: 조기 resolve는 구성 캐시 효율과 빌드 유연성을 해침
- `FileCollection`에 즉시 평가 API를 쓰지 마라: 지연 API를 사용해야 증분 빌드와 캐싱이 정상 동작
- 파일 입력은 `PathSensitivity.NONE`, 디렉터리는 `PathSensitivity.RELATIVE`를 우선하라: 경로 민감도를 적절히 낮춰야 캐시 적중률이 올라감
- 출력 파일·디렉터리 경로는 고유하게 지정하라: 여러 변형(variant) 빌드에서 경로가 겹치면 캐싱 충돌과 잘못된 결과 유발

### 성능 (Performance)

- UTF-8 인코딩을 명시하라: 플랫폼마다 기본 인코딩이 달라 컴파일 결과가 갈릴 수 있으므로 명시적으로 고정
- 빌드 캐시를 사용하라: 입력이 바뀌지 않은 태스크는 재실행 없이 캐시 결과를 재사용
- 구성 캐시(Configuration Cache)를 사용하라: 빌드 모델 계산 자체를 캐싱해 설정 단계 시간을 절약
- 구성 단계에서 비용이 큰 연산을 피하라: 무거운 계산은 태스크 액션으로 미뤄야 구성 캐시의 효과를 온전히 얻음
- Gradle 배포판은 `-bin`을 우선하라: 소스가 포함된 `-all` 배포판 대신 바이너리 전용 배포판을 써서 다운로드/시작 시간을 줄임

### 보안 (Security)

- Gradle 배포판의 SHA-256 체크섬을 검증하라: 다운로드한 배포판이 변조되지 않았는지 확인
- Wrapper 업그레이드마다 무결성을 검증하라: `gradle-wrapper.properties`의 체크섬을 매 업그레이드 시 확인해 공급망 공격을 예방

### 테스트 (Testing)

- 커스텀 태스크·플러그인은 TestKit으로 테스트하라: `gradle-testkit`을 이용해 격리된 환경에서 반복 가능한 통합 테스트를 작성

### 핵심 포인트: 왜 이런 규칙들이 존재하는가

- 대부분의 규칙은 결국 "지연 평가(laziness)를 지키고, 설정 단계와 실행 단계를 명확히 분리하라"는 하나의 원칙으로 수렴
- 의존성·태스크 관련 규칙은 캐시 적중률과 병렬 빌드 효율을 높이는 방향으로 설계됨

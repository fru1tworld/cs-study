# Gradle 일반 모범 사례 (General Best Practices)

> **원문:** https://docs.gradle.org/current/userguide/best_practices_general.html

## 개요

공식 문서는 빌드 스크립트 문법 선택부터 플러그인 적용 방식, `afterEvaluate` 사용 여부까지 총 9가지 규칙을 제시합니다. 대부분 "Don't Do This / Do This Instead" 형태의 대조 예제로 설명되며, 공통 목표는 **빌드를 예측 가능하고, 설정 캐시(Configuration Cache)와 호환되게, 순서에 의존하지 않게 만드는 것**입니다.

## 1. Kotlin DSL을 사용하라

- 신규 빌드나 서브프로젝트는 `build.gradle` 대신 `build.gradle.kts`로 작성하는 것이 권장됩니다.
- Groovy DSL은 동적 타이핑이라 IDE 자동완성·탐색이 약하고, 오탈자가 실행 시점에야 드러납니다. Kotlin DSL은 정적 타입이라 IDE 지원이 훨씬 강력합니다.
- Gradle 8.0부터 Kotlin DSL이 기본값이 되었고, Android Studio도 동일한 방향입니다.

## 2. Gradle은 항상 최신 마이너 버전을 유지하라

- 활성 지원 대상은 현재 메이저 버전과 그 직전 메이저 버전뿐입니다. 오래된 버전에 머물면 보안 패치나 성능 개선을 받지 못합니다.
- Wrapper로 버전을 올리는 것이 표준 절차입니다.

```bash
./gradlew wrapper --gradle-version <version>
```

- **핵심 포인트**: 플러그인보다 먼저 Gradle 버전을 올리고, CI에 "새 버전 미리 테스트용" 섀도우 잡을 두어 호환성을 검증한 뒤, 플러그인을 순차적으로 업그레이드하는 순서를 권장합니다. Deprecation 경고는 다음 메이저 업그레이드를 대비하라는 신호로 취급해야 합니다.

## 3. 플러그인은 `plugins {}` 블록으로 적용하라

레거시 방식인 `buildscript {}` + `apply(plugin = ...)` 조합은 지양하고, `plugins {}` 블록을 사용합니다.

**하지 말아야 할 방식**
```kotlin
buildscript {
    repositories { gradlePluginPortal() }
    dependencies { classpath("com.google.protobuf:protobuf-gradle-plugin:0.9.4") }
}
apply(plugin = "java")
apply(plugin = "com.google.protobuf")
```

**권장 방식**
```kotlin
plugins {
    id("java")
    id("com.google.protobuf").version("0.9.4")
}
```

- `plugins {}` 블록은 플러그인 클래스 로딩을 최적화하고 재사용하며, 부작용 없이(idempotent) 실행되고, 문법도 더 간결합니다.

## 4. 플러그인 적용 순서를 가정하지 마라

- Gradle의 플러그인 적용은 결정론적(deterministic)이지만, 여러 파일·프로젝트·Included Build에 걸쳐 순서를 추론하기는 어렵습니다.
- `allprojects {}`, `subprojects {}`, `afterEvaluate {}`로 순서를 강제하는 로직은 프로젝트를 하나 추가하거나 include 순서만 바꿔도 깨지는 취약한 빌드가 됩니다.

- **빌드 작성자 입장**: 서로 다른 프로젝트·Included Build·스크립트 간 순서에 의존하는 구성을 피해야 합니다.
- **플러그인 개발자 입장**: 사용자가 어떤 순서로 플러그인을 적용해도 실패하지 않아야 합니다. 의존하는 플러그인이 있다면 명시적으로 적용하고, 선택적 연동은 존재 여부에 반응하도록 작성합니다.

```kotlin
// 필수 의존 플러그인은 직접 적용
project.pluginManager.apply("com.example.required-plugin")

// 선택적 연동은 존재할 때만 반응
project.pluginManager.withPlugin("com.example.other-plugin") { /* ... */ }
```

## 5. 내부(Internal) API를 사용하지 마라

- 패키지 경로에 `internal`이 포함되거나 타입 이름이 `Internal`/`Impl`로 끝나는 것은 모두 비공개 구현입니다.
- 이런 API는 마이너 버전에서도 예고 없이 변경될 수 있고, Android Gradle Plugin·Kotlin Gradle Plugin 같은 주요 플러그인도 이를 불안정하다고 간주합니다.
- 필요한 기능이 공개 API에 없다면 기능 요청을 제출하거나, 임시로 필요한 로직만 복사해 자체 구현으로 대체하는 것이 안전합니다.

**하지 말아야 할 방식**
```kotlin
import org.gradle.api.internal.attributes.AttributeContainerInternal
val badMap = (attributes as AttributeContainerInternal).asMap()
```

**권장 방식**
```kotlin
val goodMap = attributes.keySet().associate {
    Attribute.of(it.name, it.type) to attributes.getAttribute(it)
}
```

## 6. 빌드 플래그는 `gradle.properties`에 설정하라

- `org.gradle`로 시작하는 빌드 설정값은 커맨드라인 옵션이나 환경 변수 대신 루트 프로젝트의 `gradle.properties`에 적어야 합니다.
- 커맨드라인 지정은 일시적인 디버깅용일 뿐이고, 파일에 적어 두면 모든 개발자·CI 환경에서 동일하게 적용되며 형상 관리 대상이 됩니다. 단, 이 설정은 Composite Build 경계를 넘어 자동으로 상속되지 않습니다.

```properties
# gradle.properties
org.gradle.continue=true
```

이렇게 하면 매번 `-D` 옵션을 기억해서 붙일 필요 없이 `gradle run`만으로 동일한 동작을 보장합니다.

## 7. 루트 프로젝트 이름을 명시하라

- `settings.gradle(.kts)`를 비워 두면 루트 프로젝트 이름이 디렉터리 이름에서 자동으로 결정되는데, 디렉터리에 공백이나 특수문자가 있으면 문제가 생기고, 사람마다 클론 위치가 달라 이름이 흔들릴 수 있습니다.
- 프로젝트 이름은 에러 메시지·로그·리포트에 계속 노출되므로 명시적으로 고정해 두는 편이 이해하기 쉽습니다.

```kotlin
// settings.gradle.kts
rootProject.name = "my-example-project"
```

## 8. 서브프로젝트에 `gradle.properties`를 두지 마라

- 서브프로젝트 디렉터리 안에 개별 `gradle.properties`를 두는 방식은 Gradle과 주요 플러그인(Android Gradle Plugin, Kotlin Gradle Plugin)이 일관되게 지원하지 않습니다.
- 설정 값이 여러 위치에 흩어지면 어디서 값이 덮어써지는지 추적하기 어려워집니다.

- 서브프로젝트 하나만 설정이 필요하면 그 프로젝트의 `build.gradle(.kts)`에 직접 정의합니다.
- 여러 서브프로젝트가 같은 설정을 공유해야 한다면 Convention Plugin으로 뽑아내고, 각 서브프로젝트가 필요할 때 값을 오버라이드하게 만듭니다.

## 9. `afterEvaluate` 사용을 피하라

`project.afterEvaluate {}`로 태스크를 구성하거나 프로퍼티를 연결하거나 플러그인 적용에 반응하는 패턴은 세 가지 문제를 낳습니다.

1. **순서에 취약함**: 여러 플러그인이 각자 `afterEvaluate`를 등록하면 실행 순서는 등록 순서를 따르므로, 어떤 콜백은 아직 완전히 구성되지 않은 상태를 보게 될 수 있습니다.
2. **태스크 구성 회피(Task Configuration Avoidance)를 무력화함**: `afterEvaluate` 안에서 등록·구성된 태스크는 실행 여부와 무관하게 즉시(eager) 처리되어 불필요한 작업이 발생합니다.
3. **설정 캐시와 호환되지 않음**: 콜백이 캡처하는 가변 상태는 안정적으로 직렬화되지 않습니다.

**대안**: 값 해석을 실행 시점으로 미루는 Lazy Property/Provider와, 플러그인 적용 여부에 안전하게 반응하는 `pluginManager.withPlugin()`을 사용합니다. `afterEvaluate`는 필수 설정 여부를 조기에 검증하는 fail-fast 검사나 최종 구성 상태를 로깅하는 용도로만 좁게 허용됩니다.

**하지 말아야 할 방식**
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
콜백 등록 순서가 실행 순서를 결정하고, 빌드 스크립트가 값을 설정하기 전에 프로퍼티를 읽어 버리며, 태스크가 즉시 등록됩니다.

**권장 방식**
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
값은 `get()`이 호출되는 실행 시점에 해석되므로 구성 순서가 더 이상 문제되지 않고, 설정 캐시와도 호환됩니다.

## 핵심 포인트: 규칙들을 관통하는 원칙

- **선언적으로, 지연 평가로**: `plugins {}` 블록, Lazy Property, `pluginManager.withPlugin()` 모두 "지금 당장 실행하지 말고 필요할 때 해석하라"는 같은 철학을 공유합니다.
- **순서 의존성을 걷어내라**: 플러그인 적용 순서, `afterEvaluate` 등록 순서처럼 암묵적 순서에 기대는 로직은 프로젝트 구조가 조금만 바뀌어도 깨지기 쉬운 코드입니다.
- **공개 계약만 신뢰하라**: 내부 API, 서브프로젝트별 `gradle.properties`처럼 문서화되지 않았거나 지원이 불안정한 경로는 피하고, `gradle.properties`·Convention Plugin·공개 API처럼 안정적인 경로로 설정을 모아야 유지보수가 쉬워집니다.

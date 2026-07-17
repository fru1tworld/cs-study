# Gradle 바이너리 플러그인

> **원문:** https://docs.gradle.org/current/userguide/binary_plugin_advanced.html

## 개요
빌드 스크립트에 직접 로직을 넣는 스크립트 플러그인만으로는 감당하기 어려운 요구사항(재사용, 테스트, 배포 등)이 생기면 **바이너리 플러그인**을 작성하는 것이 정답이 됩니다. 바이너리 플러그인은 코틀린·자바·그루비 같은 컴파일 언어로 작성되어 **JAR로 패키징**되는 플러그인으로, Gradle이 규정한 `Plugin` 인터페이스를 반드시 구현해야 합니다.

## Plugin 인터페이스 구현하기
바이너리 플러그인을 만드는 절차는 크게 두 단계로 요약됩니다.

### 1단계 — `org.gradle.api.Plugin` 인터페이스 확장
플러그인 클래스를 선언하고 `Plugin<Project>`를 구현(확장)합니다. 제네릭 타입 파라미터(`Project`)는 이 플러그인이 어떤 대상에 적용되는지를 나타내며, 프로젝트가 아닌 `Settings`나 `Gradle` 객체에 적용되는 플러그인을 만들 수도 있습니다.

```kotlin
abstract class SamplePlugin : Plugin<Project> {
}
```

### 2단계 — `apply` 메서드 오버라이드
실제 플러그인 로직(태스크 등록, 확장 추가, 설정 등)은 `apply()` 메서드 안에 작성합니다. Gradle은 플러그인이 적용되는 시점에 이 메서드를 호출하며, 인자로 전달된 `Project` 객체를 통해 빌드에 필요한 요소를 추가합니다.

```kotlin
abstract class SamplePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("ScriptPlugin") {
            doLast {
                println("Hello world from the build file!")
            }
        }
    }
}
```

## 플러그인 적용 방법
클래스로 정의한 플러그인은 빌드 스크립트에서 클래스 참조로 직접 적용할 수 있습니다. (일반적으로는 `plugin-id`를 통한 적용을 사용하지만, 개발·테스트 단계에서는 클래스를 직접 지정하는 방식도 가능합니다.)

```kotlin
apply<SamplePlugin>()
```

```groovy
apply plugin: SamplePlugin
```

## 핵심 포인트
- 바이너리 플러그인 = **컴파일 언어로 작성 + JAR로 패키징**된 플러그인. 스크립트 플러그인보다 재사용·테스트·배포에 유리하다.
- 구현은 단 두 단계다. ① `Plugin<T>` 인터페이스를 확장하고 ② `apply(T target)` 메서드에 실제 로직을 작성한다.
- 제네릭 타입 `T`는 플러그인이 적용될 대상(`Project`, `Settings`, `Gradle` 등)을 의미하며, 대부분은 `Project`를 대상으로 작성한다.
- `apply()` 안에서 태스크 등록, 확장 추가 등 원하는 빌드 커스터마이징을 자유롭게 구현할 수 있다.
- 이 문서는 바이너리 플러그인의 최소 골격만 다루며, 실전 개발(확장 객체 설계, 컨벤션 적용 등)은 이어지는 "Developing Binary Plugins" 문서에서 다룬다.

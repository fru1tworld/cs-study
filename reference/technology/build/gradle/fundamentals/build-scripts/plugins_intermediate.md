# 플러그인 다루기 (Working with Plugins)

> **원문:** https://docs.gradle.org/current/userguide/plugins_intermediate.html

## 개요

이 문서는 `plugin_basics.md`에서 다룬 "플러그인이란 무엇인가"를 넘어서, 플러그인을 **어떻게 배포하고, 어떤 방법으로 적용하고, 버전과 저장소를 어떻게 중앙에서 관리하는지**를 다룹니다. 핵심은 `plugins { }` 블록이 표준이라는 점과, 그 외의 적용 방식(서브프로젝트 일괄 적용, `buildscript`, 레거시 `apply()`)은 대부분 레거시이거나 특수한 상황에서만 쓰인다는 점입니다.

## 플러그인 적용 방법 6가지

Gradle은 플러그인을 적용하는 여러 방법을 제공하지만, 권장도는 방법마다 크게 다릅니다.

1. **`plugins { }` 블록 (권장, 표준)** — 각 프로젝트의 빌드 스크립트에서 플러그인 ID(와 필요 시 버전)를 선언합니다. 문법이 간결하고, 타입 세이프 접근자를 제공하며, Gradle이 적용 전에 미리 분석해 클래스패스를 최적화할 수 있습니다.

   ```kotlin
   plugins {
       id("org.springframework.boot") version "3.3.1"
   }
   ```

2. **루트 프로젝트에서 서브프로젝트에 일괄 적용** — 루트의 `plugins { }`에 `apply false`로 선언해두고, 각 서브프로젝트 스크립트에서 버전 없이 `id(...)`로 다시 적용하는 방식. 과거 멀티 프로젝트 빌드에서 흔했지만 **더는 권장되지 않습니다.**

3. **루트에서 선언만 하고 적용은 서브프로젝트가 선택** — 2번과 메커니즘은 같지만 목적이 다릅니다. 모든 서브프로젝트가 강제로 같은 플러그인을 쓰게 하는 게 아니라, 버전만 한 곳에서 통일해두고 필요한 서브프로젝트만 골라서 적용하게 하는 용도입니다.

4. **`buildSrc`의 컨벤션 플러그인 (멀티 프로젝트 권장안)** — 공유할 빌드 로직을 `buildSrc`에 플러그인 형태로 만들어두고, 각 서브프로젝트에서는 버전 없이 ID만으로 적용합니다. 2·3번 방식보다 유지보수성이 좋아 **멀티 프로젝트 빌드의 사실상 표준**으로 자리잡았습니다.

5. **`buildscript { }` 블록 (레거시)** — 빌드 스크립트 자체가 실행되기 위해 필요한 의존성이나, Plugin Portal에 없는 커스텀/사설 저장소의 플러그인을 가져올 때 쓰던 방식입니다. `plugins { }` 블록이 등장한 이후로는 사용 빈도가 크게 줄었습니다.

   ```kotlin
   buildscript {
       repositories { /* ... */ }
       dependencies { classpath("...") }
   }
   apply(plugin = "...")
   ```

6. **레거시 `apply()` 함수** — 플러그인 ID 대신 클래스를 직접 참조해 적용하는 방식(`apply<MyPlugin>()`)으로, 스크립트 플러그인을 임시로 붙일 때나 쓰이며 사실상 사용을 지양해야 합니다.

## `plugins { }` DSL의 제약

`plugins { }` 블록이 최적화와 도구 지원(에디터 자동완성 등)을 제공할 수 있는 이유는 문법이 **엄격하게 제한**되어 있기 때문입니다.

- 플러그인 ID와 버전은 **리터럴 상수 문자열**이어야 하며, 변수 조합이나 조건식으로 동적으로 만들 수 없습니다.
- `if`문이나 반복문 안에 넣을 수 없습니다.
- 블록 실행은 **부작용이 없고(idempotent)** 반복 실행해도 같은 결과를 내야 합니다.
- `build.gradle(.kts)`와 `settings.gradle(.kts)`에서만 쓸 수 있고, 스크립트 플러그인이나 init 스크립트에서는 사용할 수 없습니다.

즉 "동적으로 플러그인을 고르고 싶다"는 요구는 `plugins { }` 블록이 아니라 `pluginManagement`의 리졸루션 규칙이나 `buildscript` 같은 다른 메커니즘으로 풀어야 합니다.

## 플러그인 관리(`pluginManagement`)

`settings.gradle(.kts)`의 **가장 첫 블록**으로 위치하며, 빌드 전체에서 플러그인을 어떻게 찾고 어떤 버전을 쓸지 한 곳에서 통제합니다. 내부에 세 가지 하위 블록을 둘 수 있습니다.

- **`repositories { }`** — 플러그인을 어디서 내려받을지 지정합니다. 기본값은 Gradle Plugin Portal 하나뿐이며, 사설 Maven/Ivy 저장소를 추가하면 선언한 순서대로 검색합니다.

  ```kotlin
  pluginManagement {
      repositories {
          maven(url = file("./maven-repo"))
          gradlePluginPortal()
          ivy(url = file("./ivy-repo"))
      }
  }
  ```

- **`plugins { }`** — 여기서는 빌드 스크립트의 `plugins { }`와 달리 문법 제약이 없어서, `gradle.properties` 값을 읽어오거나 Provider를 활용해 버전을 동적으로 정할 수 있습니다. 버전을 이 블록에서 한 번 정의해두면, 각 서브프로젝트의 `plugins { }`에서는 버전 없이 ID만 쓰면 됩니다. 즉 **버전을 한 곳(single source of truth)에서 관리**하는 것이 핵심 목적입니다.

- **`resolutionStrategy { eachPlugin { ... } }`** — 요청된 플러그인 ID를 가로채 실제 적용할 아티팩트 좌표를 바꿔치기할 수 있습니다. 예를 들어 특정 네임스페이스로 시작하는 ID를 사내 저장소의 특정 모듈로 강제 매핑하는 식입니다.

  ```kotlin
  resolutionStrategy {
      eachPlugin {
          if (requested.id.namespace == "com.example") {
              useModule("com.example:sample-plugins:1.0.0")
          }
      }
  }
  ```

## 플러그인 마커 아티팩트

Gradle이 `plugins { id("com.example.foo") }`처럼 ID만 보고 실제 구현 jar를 찾아낼 수 있는 이유는 **마커(marker) 아티팩트** 덕분입니다. 저장소에는 `plugin.id:plugin.id.gradle.plugin:plugin.version`라는 이름의 자그마한 마커 모듈이 별도로 존재하고, 이 마커가 실제 플러그인 구현체를 가리킵니다. `java-gradle-plugin`으로 플러그인을 게시하면 이 마커도 함께 자동 생성되므로, 플러그인 작성자가 직접 신경 쓸 일은 거의 없습니다.

## 버전 카탈로그와 함께 쓰기

`libs.versions.toml` 같은 버전 카탈로그에 플러그인 좌표를 등록해두면, 빌드 스크립트에서는 다음처럼 별칭(alias)으로 적용할 수 있습니다.

```kotlin
plugins {
    alias(libs.plugins.pluginName)
}
```

의존성 카탈로그와 동일한 인프라를 재사용하기 때문에 타입 세이프 접근자가 생성되고, 여러 모듈에 걸쳐 플러그인 버전을 일관되게 유지하기 좋습니다.

## 핵심 포인트

- 플러그인은 코어/커뮤니티/커스텀 세 출처가 있고, 적용 방법은 그와 별개로 6가지가 존재하지만 실무에서 쓸 만한 것은 사실상 `plugins { }` 블록과 `buildSrc` 컨벤션 플러그인 두 가지다.
- `plugins { }` DSL은 리터럴 문자열·비조건문·부작용 없음이라는 제약을 지키는 대가로 Gradle의 최적화와 IDE 지원을 얻는다.
- 멀티 프로젝트에서 플러그인을 서브프로젝트마다 반복 적용하던 옛 방식(루트 `apply false` + 서브프로젝트 재적용)은 `buildSrc` 컨벤션 플러그인으로 대체하는 것이 권장된다.
- 플러그인 버전과 저장소는 빌드 스크립트가 아니라 `settings.gradle(.kts)`의 `pluginManagement`에서 중앙 관리하는 것이 최신 관례다.
- `buildscript { }`와 레거시 `apply()`는 사설 저장소 대응 등 특수한 경우를 위해 남아있는 하위 호환 수단일 뿐, 새 코드에서는 지양한다.

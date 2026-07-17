# Gradle 심화 튜토리얼 8장 - 플러그인 로컬 배포

> **원문:** https://docs.gradle.org/current/userguide/part8_publish_locally.html

## 개요
지금까지 초기화, 확장(extension) 설계, 커스텀 태스크 작성, 단위·기능 테스트를 거쳐 플러그인을 완성했습니다. 마지막 단계는 이 플러그인이 실제로 **배포 가능한 형태**인지 검증하는 것입니다. 이 장에서는 플러그인을 로컬 Maven 저장소에 배포한 뒤, 별도의 컨슈머(consumer) 프로젝트에서 마치 외부 저장소에서 받아온 것처럼 소비해 보면서 배포 파이프라인 전체를 눈으로 확인합니다.

## 1. 배포에 필요한 메타데이터 추가
플러그인 프로젝트의 빌드 스크립트에 두 가지를 추가해야 합니다.

- **`maven-publish` 플러그인 적용** — 기존에 쓰던 `java-gradle-plugin`, 그리고 Kotlin/Groovy 언어 플러그인과 함께 선언합니다.
- **`group`과 `version` 명시** — 이 두 값이 없으면 `publish` 태스크 자체가 실패합니다.

```kotlin
plugins {
    `java-gradle-plugin`
    `maven-publish`
}

group = "org.example"
version = "1.0.0"
```

이 값들은 배포되는 아티팩트를 식별하는 좌표(coordinate) 역할을 하므로, 임의의 값이 아니라 실제로 관리할 버전 체계를 반영해야 합니다.

## 2. 로컬 폴더로 배포하기
원격 저장소 대신 프로젝트 빌드 디렉터리 하위의 폴더를 배포 대상으로 지정합니다.

```kotlin
publishing {
    repositories {
        maven {
            name = "localRepo"
            url = layout.buildDirectory.dir("local-repo").get().asFile.toURI()
        }
    }
}
```

배포는 다음 명령으로 실행합니다.

```bash
./gradlew :plugin:publishAllPublicationsToLocalRepoRepository
```

- 태스크 이름은 `publishAllPublicationsTo<저장소 이름>Repository` 패턴을 따르며, `localRepo`라는 이름을 그대로 반영합니다.
- 실행 결과 `plugin/build/local-repo/` 아래에 플러그인 마커, JAR, `pom.xml` 등 표준 Maven 배포물이 생성됩니다.

## 3. 컨슈머 프로젝트에서 로컬 저장소 바라보기
플러그인을 사용하는 별도 프로젝트의 `settings.gradle(.kts)`에서 `pluginManagement.repositories`에 방금 만든 로컬 경로를 추가합니다.

```kotlin
pluginManagement {
    repositories {
        maven {
            url = file("../plugin/build/local-repo").toURI()
        }
        gradlePluginPortal()
    }
}
```

- 로컬 경로를 먼저 등록하면 Gradle이 플러그인 해석 시 이곳을 우선 탐색합니다.
- `gradlePluginPortal()`은 그다음 순위로, 로컬에 없는 나머지 플러그인은 여전히 정상적으로 받아옵니다.

## 4. 컨슈머 빌드에 플러그인 적용
컨슈머 프로젝트의 `build.gradle(.kts)`에서 방금 배포한 버전을 명시적으로 지정해 플러그인을 적용합니다.

```kotlin
plugins {
    id("org.example.slack") version "1.0.0"
}
```

이 상태로 컨슈머 프로젝트의 태스크를 실행해 보면, 사내/오픈소스 저장소에서 내려받은 플러그인을 쓸 때와 동일한 방식으로 동작하는지 확인할 수 있습니다.

## 핵심 포인트
- 플러그인을 로컬 Maven 저장소에 배포해 보는 것은, 실제 배포 전에 "이 플러그인이 외부에서도 정상적으로 설치·적용되는가"를 검증하는 가장 간단한 리허설이다.
- `group`/`version`이 없으면 `publish` 태스크가 실패하므로 배포용 빌드 스크립트에서는 반드시 명시해야 한다.
- 배포 대상 저장소에 붙인 이름(`localRepo`)이 곧 실행할 태스크 이름(`publishAllPublicationsToLocalRepoRepository`)에 그대로 반영된다.
- 컨슈머 프로젝트는 `pluginManagement { repositories { ... } }`에 로컬 경로를 추가하는 것만으로, 별도 설정 없이 로컬에서 만든 플러그인을 원격 플러그인처럼 가져다 쓸 수 있다.
- 이 과정을 통과하면 해당 플러그인은 "배포 가능(publishable)"할 뿐 아니라 "소비 가능(consumable)"하다는 것, 즉 실제 배포 파이프라인에 올릴 준비가 되었다는 것이 증명된다.

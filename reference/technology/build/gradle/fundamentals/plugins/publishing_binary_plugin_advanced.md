# Gradle 바이너리 플러그인 배포

> **원문:** https://docs.gradle.org/current/userguide/publishing_binary_plugin_advanced.html

## 개요
바이너리 플러그인을 만들고 테스트까지 마쳤다면, 다음 단계는 **배포(publish)**입니다. 여러 빌드에서 재사용하거나 다른 사람과 공유하려면 플러그인을 어딘가의 저장소에 올려야 하는데, Gradle은 이를 위한 여러 방법을 제공합니다. 가장 기본이 되는 것이 코어 플러그인인 `maven-publish`이며, 이 문서는 로컬 Maven 저장소에 플러그인을 배포하는 가장 단순한 경로를 다룹니다.

## 1. maven-publish 플러그인 적용
플러그인을 배포하려면 `java-gradle-plugin`과 함께 `maven-publish`를 적용해야 합니다.

```kotlin
plugins {
    `java-gradle-plugin`
    `maven-publish`
}
```

- `java-gradle-plugin`은 플러그인 개발에 필요한 기본 골격(플러그인 디스크립터 생성 등)을 제공하고,
- `maven-publish`는 그 결과물을 Maven 형식의 저장소에 올리는 역할을 담당합니다.

흥미로운 점은 배포에 필요한 핵심 정보(`group`, `version`, 플러그인 `id`)가 이미 개발 단계에서 작성해 둔 `gradlePlugin { }` 블록과 프로젝트 설정에 들어 있다는 것입니다.

```kotlin
group = "org.example"
version = "1.0.0"

gradlePlugin {
    plugins {
        create("filesizediff") {
            id = "org.example.filesizediff"
            implementationClass = "org.example.FileSizeDiffPlugin"
        }
    }
}
```

- `group` — 배포될 아티팩트의 그룹 ID
- `version` — 배포될 아티팩트의 버전
- `gradlePlugin { }` — `java-gradle-plugin`이 제공하는 블록으로, 플러그인의 전체 이름(`org.example.filesizediff`)과 이를 구현하는 클래스(`FileSizeDiffPlugin`)를 등록

즉 별도로 배포 메타데이터를 새로 작성할 필요 없이, 개발 시점에 선언한 정보가 배포 시점에 그대로 재사용됩니다.

## 2. 배포 대상 저장소 설정
`publishing { }` 블록 안에 `repositories { }`를 두어 아티팩트를 어디에 올릴지 지정합니다.

```kotlin
publishing {
    repositories {
        maven {
            url = uri("${layout.projectDirectory}/publish")
        }
    }
}
```

- 예시는 원격 저장소가 아니라 프로젝트 디렉터리 안의 로컬 경로(`publish` 폴더)를 대상으로 지정합니다.
- 실무에서는 이 자리에 사내 아티팩트 저장소나 Gradle Plugin Portal 등 실제 배포처의 URL이 들어가게 됩니다.

## 3. 배포 실행
저장소까지 설정했다면 배포는 태스크 한 번 실행으로 끝납니다.

```bash
./gradlew publish
```

이 명령을 실행하면 Gradle이 플러그인 JAR과 함께 `pom.xml` 등 관련 메타데이터 파일을 생성해 지정한 Maven 저장소에 설치합니다.

## 핵심 포인트
- 플러그인 배포의 최소 구성은 `java-gradle-plugin` + `maven-publish` 두 플러그인의 조합이다.
- 배포에 필요한 `group`/`version`/`id` 정보는 새로 작성하는 것이 아니라, 개발 단계의 `gradlePlugin { }` 선언을 그대로 재사용한다.
- `publishing { repositories { maven { url = ... } } }`로 배포 목적지(로컬 경로, 원격 저장소, Plugin Portal 등)만 바꿔주면 배포 대상을 자유롭게 전환할 수 있다.
- 실제 배포는 `./gradlew publish` 한 줄로 수행되며, 이 과정에서 JAR과 `pom.xml` 같은 메타데이터가 함께 생성·업로드된다.
- 이 문서는 로컬 Maven 저장소 배포라는 가장 단순한 사례만 다루며, Gradle Plugin Portal 공개 배포 등은 별도 플러그인·절차가 필요하다.

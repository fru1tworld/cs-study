# Gradle 튜토리얼 Part 4 - 플러그인 적용하기

> **원문:** https://docs.gradle.org/current/userguide/part4_gradle_plugins.html

## 개요
이번 파트에서는 앞서(Part 1~3) 초기화·태스크 실행·의존성 관리를 다룬 자바 애플리케이션에 **Maven Publish 플러그인**을 새로 적용해 보면서, 플러그인이 실제로 빌드에 무엇을 추가해 주는지 손으로 확인합니다.

## 플러그인이란
Gradle 공식 문서는 플러그인을 "빌드 로직을 조직화하고 프로젝트 내에서 재사용하는 가장 기본적인 방법"으로 설명합니다. 플러그인을 적용하면 아래와 같은 것들이 한꺼번에 빌드에 편입됩니다.

- **태스크 추가** — 컴파일, 테스트 등 실행 가능한 작업 단위
- **모델 확장** — 새로운 DSL 요소(`publishing { }` 같은 설정 블록) 도입
- **컨벤션 적용** — 프로젝트에 합리적인 기본값과 표준을 부여
- **타입 확장** — 기존 클래스에 새 속성/메서드 부여
- **설정 관리** — 레포지토리, 조직 표준 등을 일괄 적용

## 플러그인 적용해 보기: Maven Publish
기존 프로젝트는 `application` 플러그인만 갖고 있었습니다. 여기에 로컬/원격 Maven 저장소로 아티팩트를 배포할 수 있게 해 주는 `maven-publish` 플러그인을 추가합니다.

```kotlin
plugins {
    application
    id("maven-publish")
}
```

플러그인을 추가하는 것만으로 `publish`, `publishToMavenLocal` 같은 새 태스크가 프로젝트에 생겨납니다. 이는 앞서 학습한 "플러그인은 코어(내장)/커뮤니티(Plugin Portal)/커스텀(자체 제작) 세 부류로 나뉜다"는 분류에서 `maven-publish`가 **코어 플러그인**에 해당하는 예입니다.

## 플러그인 설정하기
플러그인을 적용했다고 끝이 아니라, 대부분은 별도의 설정 블록을 통해 동작을 구체화해야 합니다. Maven Publish의 경우 `publishing { }` 블록에서 배포될 아티팩트의 좌표(groupId/artifactId/version)와 어떤 컴포넌트를 배포할지 지정합니다.

```kotlin
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.gradle.tutorial"
            artifactId = "tutorial"
            version = "1.0"
            from(components["java"])
        }
    }
}
```

이 설정을 추가하면 `generatePomFileForMavenPublication` 등 좀 더 세분화된 태스크들이 추가로 생성됩니다. 즉 "플러그인 적용 → 태스크 생성"과 "설정 블록 작성 → 태스크 세분화·구체화"는 서로 다른 단계입니다.

## 플러그인 사용해 보기
설정을 마친 뒤 `./gradlew :app:publishToMavenLocal`을 실행하면, 컴파일·리소스 처리·jar 생성을 거쳐 POM/메타데이터 파일을 만들고 로컬 Maven 저장소(`~/.m2`)에 게시하는 태스크 체인이 순서대로 실행됩니다. 생성된 POM 파일에는 앞서 지정한 groupId·artifactId·version이 그대로 반영됩니다.

## 플러그인 생태계 정리
- **코어 플러그인**: Gradle에 기본 내장. `id("java")`처럼 짧은 이름만으로 적용, 버전 지정 불필요
- **커뮤니티 플러그인**: Gradle Plugin Portal에 배포된 서드파티 플러그인. `id("com.diffplug.spotless").version("6.25.0")`처럼 ID와 버전을 함께 지정
- **커스텀 플러그인**: 조직/팀이 직접 작성한 플러그인. 여러 서브프로젝트가 빌드 로직을 공유해야 할 때는 "컨벤션 플러그인" 형태로 만들어 중복을 없애는 것이 정석

## 핵심 포인트
- 플러그인 적용은 `plugins { }` 블록 한 줄로 끝나지만, 그 뒤에는 태스크·DSL·컨벤션이 한꺼번에 따라온다.
- "적용"과 "설정"은 별개 단계다. `maven-publish`를 적용만 해서는 부족하고, `publishing { }` 블록으로 좌표와 배포 대상을 지정해야 실제로 쓸 수 있는 태스크가 완성된다.
- 플러그인은 코어(내장)/커뮤니티(Plugin Portal)/커스텀(자체 제작) 세 종류로 나뉘며, 코어만 버전 없이 짧은 이름으로 적용 가능하다.
- 여러 서브프로젝트에서 빌드 로직을 재사용해야 한다면 컨벤션 플러그인으로 공통 설정을 뽑아내는 것이 Gradle이 권장하는 방식이다.

# Gradle 어드밴스드 튜토리얼 7편: 소비자 프로젝트로 플러그인 검증하기

> **원문:** https://docs.gradle.org/current/userguide/part7_use_consumer_project.html

## 개요

1~6편을 거치며 `org.example.slack` 플러그인은 확장(extension), 커스텀 태스크, 단위 테스트, 데이터플로우 액션, 기능 테스트까지 갖춘 상태가 됐다. 하지만 지금까지의 검증은 전부 플러그인 프로젝트 "안에서" 이뤄진 것이다. 7편은 플러그인을 실제로 "가져다 쓰는" 별도의 **소비자(consumer) 프로젝트**를 만들어, 진짜 사용자가 겪을 환경과 최대한 비슷한 조건에서 플러그인을 검증해보는 단계다. Gradle Plugin Portal 같은 저장소에 게시하지 않고도 이런 검증이 가능하도록, Gradle의 **복합 빌드(Composite Build)** 기능을 사용한다.

## 선행 조건

1~6편에서 만든 결과물이 이번 편의 출발점이다.

- 1편: 플러그인 프로젝트 초기화
- 2편: 확장(extension) 추가
- 3편: 커스텀 태스크 작성
- 4편: 단위 테스트 작성
- 5편: 데이터플로우 액션(`FlowAction`) 추가 — 빌드 종료 시 슬랙 알림 전송
- 6편: 기능 테스트 작성

## 1단계: 소비자 프로젝트 생성과 복합 빌드 구성

먼저 기존 `plugin/` 디렉터리 옆에 `consumer/` 디렉터리를 새로 만든다. `consumer`는 `settings.gradle(.kts)`와 `build.gradle(.kts)`를 자체적으로 가지는, `plugin`과는 독립된 별도 프로젝트다.

```
.
├── gradlew
├── settings.gradle.kts
├── plugin/
└── consumer/
    ├── settings.gradle.kts
    └── build.gradle.kts
```

이렇게 두 프로젝트를 나란히 두고, 루트 `settings.gradle.kts`에서 `plugin`을 **포함된 빌드(included build)**로 등록해 하나의 복합 빌드로 묶는다.

```kotlin
// settings.gradle.kts (root)
rootProject.name = "plugin-tutorial"
includeBuild("consumer")
```

아울러 복합 빌드로 묶이면 `plugin` 프로젝트가 루트의 버전 카탈로그(`libs.versions.toml`)에 더 이상 접근할 수 없으므로, `plugin/build.gradle.kts`의 플러그인 ID·의존성 선언을 카탈로그 참조가 아닌 문자열/좌표 직접 명시 방식으로 바꿔줘야 한다.

## 2단계: 소비자 프로젝트를 플러그인에 연결하기

`consumer/settings.gradle.kts`에 `pluginManagement { }` 블록을 추가해, 소비자 프로젝트가 `plugins { }` 블록에서 `org.example.slack`을 찾을 때 `plugin` 프로젝트를 참조하도록 지정한다.

```kotlin
// consumer/settings.gradle.kts
pluginManagement {
    includeBuild("../plugin")
    repositories {
        gradlePluginPortal()
    }
}
rootProject.name = "consumer"
```

핵심은 `pluginManagement` 안의 `includeBuild("../plugin")`이다. 이 한 줄로 `plugin` 프로젝트가 제공하는 모든 플러그인이 ID만으로 다른 프로젝트에서 바로 적용 가능해진다.

## 3단계: 소비자 프로젝트에서 플러그인 적용·설정하기

`consumer/build.gradle.kts`에서 평소처럼 `plugins { }` 블록으로 플러그인을 적용하고, 확장 블록(`slack { }`)에 값을 채운다.

```kotlin
plugins {
    id("java")
    id("org.example.slack")
}

repositories { mavenCentral() }

slack {
    token.set(System.getenv("SLACK_TOKEN"))
    channel.set("#social")
    message.set("Hello from consumer via composite build!")
}
```

일반적으로 `plugins { }`에서 외부 플러그인을 쓰려면 `id("...") version "..."`처럼 버전을 명시해야 하지만, 여기서는 **버전을 적지 않는다.** 복합 빌드로 연결된 포함 빌드의 플러그인은 Gradle이 자동으로 해당 소스를 찾아 해석해주기 때문이다. 즉 게시된 아티팩트의 버전 번호를 신경 쓸 필요 없이, `plugin` 디렉터리의 현재 소스 상태가 곧바로 적용된다.

## 4단계: 소비자 프로젝트 실행으로 동작 확인하기

플러그인이 등록한 커스텀 태스크는 `consumer` 디렉터리에서 직접, 또는 루트에서 전체 경로로 실행할 수 있다.

```bash
$ ./gradlew sendTestSlackMessage            # consumer 디렉터리에서
$ ./gradlew :consumer:sendTestSlackMessage  # 루트 디렉터리에서
$ ./gradlew :consumer:build                 # build 실행 시 FlowAction이 슬랙 알림도 함께 전송
```

`:consumer:build`처럼 표준 라이프사이클 태스크를 돌리면, 5편에서 등록해둔 `FlowAction`이 빌드 종료 시점에 자동으로 실행되어 슬랙 메시지를 보내는 것까지 한 번에 확인할 수 있다.

## 핵심 포인트

- 플러그인을 게시하지 않고도 "실제 사용자처럼" 검증하려면, 별도의 `consumer` 프로젝트를 만들고 **복합 빌드(`includeBuild`)**로 `plugin` 프로젝트와 엮는 방식이 표준적인 접근이다.
- 연결은 두 군데에서 이뤄진다 — 루트 `settings.gradle.kts`의 `includeBuild("consumer")`(consumer를 복합 빌드에 포함)와, `consumer/settings.gradle.kts`의 `pluginManagement { includeBuild("../plugin") }`(플러그인 ID 해석 경로 지정).
- 복합 빌드로 포함된 플러그인은 `plugins { id("...") }`에서 **버전 없이** 바로 적용할 수 있다. Gradle이 포함 빌드의 소스를 그 자리에서 바로 참조하기 때문이다.
- `consumer`에서 태스크를 실행하는 방법은 일반 서브프로젝트와 동일하게 `./gradlew <task>`(해당 디렉터리) 또는 `./gradlew :consumer:<task>`(루트) 두 가지이며, `build`처럼 라이프사이클 태스크를 돌리면 앞서 만든 `FlowAction` 같은 부가 로직도 함께 트리거된다.
- 이 방식의 가장 큰 이점은 **게시 과정 없이 즉시 반복(iterate)** 할 수 있다는 점이다 — `plugin` 코드를 수정하면 바로 `consumer`에서 재실행해 결과를 확인할 수 있다. 다만 TestKit 기반 자동화 테스트(4·6편)를 대체하는 것은 아니고, 눈으로 직접 확인하는 보조 수단에 가깝다.

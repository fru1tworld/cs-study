# Gradle 고급 튜토리얼 1편: 플러그인 프로젝트 초기화

> **원문:** https://docs.gradle.org/current/userguide/part1_gradle_init_plugin.html

## 개요

고급 튜토리얼 시리즈는 앞선 초급·중급 편에서 다룬 "빌드를 사용하는 법"에서 한 걸음 나아가, "빌드 로직을 재사용 가능한 플러그인으로 직접 작성하는 법"을 다룹니다. 1편에서는 실습 예제로 빌드 결과를 Slack 채널에 알려주는 커스텀 플러그인을 목표로 잡고, 그 첫 단계인 프로젝트 초기화와 "Hello World" 수준의 플러그인 뼈대를 만드는 과정을 살펴봅니다.

## 사전 준비물

- Gradle이 로컬에 설치되어 있어야 함 (`gradle -v`로 버전 확인)
- IntelliJ IDEA Community Edition — 플러그인 코드 탐색 및 작성용

## 1) 플러그인 프로젝트 초기화

일반 애플리케이션이 아닌 **플러그인**을 만들 것이므로 `gradle init`의 타입 옵션이 달라집니다.

```bash
$ mkdir plugin-tutorial
$ cd plugin-tutorial
$ gradle init --type kotlin-gradle-plugin --dsl kotlin
```

- Groovy로 작성하려면 `--type groovy-gradle-plugin --dsl groovy`를 사용
- `--type` 값이 `java-application`이 아니라 `*-gradle-plugin`인 점이 이전 튜토리얼과의 핵심 차이

## 2) 생성된 디렉터리 구조

```text
plugin-tutorial/
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── gradlew, gradlew.bat
├── settings.gradle.kts
└── plugin/
    ├── build.gradle.kts
    └── src/
        ├── main/kotlin(또는 groovy)/
        ├── test/kotlin(또는 groovy)/
        └── functionalTest/kotlin(또는 groovy)/
```

- 서브프로젝트 이름이 `app`이 아니라 `plugin`으로 생성되는 점이 눈에 띔
- `test` 외에 `functionalTest` 소스 세트가 별도로 생성되어, 단위 테스트와 "플러그인을 실제 빌드에 적용해보는" 통합 테스트를 구분해서 작성하도록 스캐폴딩됨
- 나머지 wrapper·버전 카탈로그 구조는 이전 애플리케이션 프로젝트와 동일

## 3) `plugin/build.gradle.kts` 살펴보기

```kotlin
plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
}

gradlePlugin {
    plugins.create("greeting") {
        id = "org.example.greeting"
        implementationClass = "org.example.PluginTutorialPlugin"
    }
}
```

- `java-gradle-plugin` 플러그인이 핵심: 플러그인 메타데이터(`id` → 구현 클래스 매핑)를 자동 생성하고, 플러그인 개발에 특화된 컨벤션·검증 태스크, 게시(publish) 편의 기능까지 함께 제공
- `gradlePlugin { plugins.create(...) }` 블록에서 플러그인 ID와 진입점 클래스를 연결
  - `id`: 다른 빌드 스크립트에서 `plugins { id("org.example.greeting") }`로 적용할 때 쓰는 식별자
  - `implementationClass`: 실제 로직이 담긴 클래스의 정규화된 이름

## 4) "Hello World" 플러그인 코드

```kotlin
class PluginTutorialPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("greeting") { task ->
            task.doLast {
                println("Hello from plugin 'org.example.greeting'")
            }
        }
    }
}
```

- 모든 Gradle 플러그인은 `Plugin<Project>` 인터페이스를 구현해야 함
- `apply(project: Project)`가 플러그인의 진입점 — 이 빌드에 플러그인이 적용되는 순간 Gradle이 호출
- 내부에서 `project.tasks.register("greeting") { ... }`로 새 태스크를 등록하고, `doLast { }` 블록에 실행할 로직(여기서는 콘솔 출력)을 넣음
- 즉 플러그인의 본질은 "프로젝트 객체를 받아 태스크·확장·설정을 추가하는 코드"라는 점을 이 최소 예제로 확인할 수 있음

## 핵심 포인트: 플러그인은 어떻게 동작을 검증하는가

- 다른 빌드 스크립트에서 `plugins { id("org.example.greeting") }`로 플러그인을 적용하면, `apply()`가 실행되며 `greeting` 태스크가 등록됨
- `./gradlew greeting`을 실행하면 `Hello from plugin 'org.example.greeting'`이 출력됨
- 이 흐름은 이후 만들 모든 커스텀 태스크·확장(extension)에도 그대로 적용되는 기본 검증 패턴: "플러그인 적용 → 태스크 등록 확인 → 실행해서 동작 확인"

## 5) 이번 튜토리얼의 목표: Slack 알림 플러그인

이 시리즈에서 최종적으로 완성할 플러그인은 다음 기능을 갖춥니다.

- `sendTestSlackMessage` 같은 태스크로 임의의 Slack 메시지 전송
- 빌드가 끝났을 때 성공/실패 여부를 자동으로 Slack에 보고
- `FlowAction` 등 최신 Gradle API를 활용해 빌드 생명주기(lifecycle) 종료 시점에 훅을 걸어 동작

## 6) Slack API 준비 작업

플러그인이 실제로 메시지를 보내려면 Slack 쪽 설정이 먼저 필요합니다.

1. Slack 워크스페이스 관리자 권한 확보
2. Slack 앱 생성 페이지에서 앱 매니페스트로 앱 생성
3. `chat:write` OAuth 스코프 권한 추가
4. `xoxb-`로 시작하는 봇 토큰 발급 및 안전하게 보관

- 이 토큰이 이후 플러그인 코드에서 Slack API를 호출할 때 인증 수단으로 사용됨

## 다음 단계

이 편에서는 플러그인 프로젝트를 초기화하고 "Hello World" 수준의 최소 플러그인으로 태스크 등록·적용·실행 흐름을 확인했습니다. 다음 편 "확장(Extension) 추가하기"에서는 빌드 스크립트에서 플러그인 동작을 설정할 수 있도록 확장 객체를 도입하는 과정을 다룹니다.

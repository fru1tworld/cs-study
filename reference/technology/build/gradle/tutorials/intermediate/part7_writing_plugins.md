# Gradle 인터미디에이트 튜토리얼 7편: 플러그인 작성하기

> **원문:** https://docs.gradle.org/current/userguide/part7_writing_plugins.html

## 개요

6편에서는 `DefaultTask`를 상속한 `LicenseTask`를 직접 만들어봤다. 하지만 클래스 하나만 정의해뒀다고 곧바로 빌드에서 쓸 수 있는 건 아니다. 태스크를 실제로 "적용 가능한 기능 단위"로 묶어주는 것이 바로 플러그인이다. 7편은 인터미디에이트 튜토리얼의 마지막 편으로, 앞서 만든 `LicenseTask`를 `LicensePlugin`에 연결하고, 서브프로젝트에 적용해서 실제로 동작시켜보는 것으로 마무리된다.

## 선행 조건

1~6편에서 다룬 내용이 이번 편의 전제가 된다.

- 1편: Java 앱 초기화
- 2편: 빌드 생명주기
- 3편: 서브프로젝트와 별도 빌드 추가
- 4편: settings 파일
- 5편: build 스크립트
- 6편: `LicenseTask` 같은 커스텀 태스크 작성

## 태스크와 플러그인의 연결: `apply()` 안에서 태스크 등록

플러그인 클래스는 `Plugin<Project>` 인터페이스를 구현하고, `apply(project: Project)` 메서드 안에서 `project.tasks.register<T>(...)`를 호출해 태스크를 등록한다. 이 시점에 `description`과 `group`을 지정해두면, 나중에 `./gradlew tasks`로 태스크 목록을 훑어볼 때 이 태스크가 어떤 플러그인에서 왔는지, 무슨 일을 하는지 바로 드러난다.

```kotlin
class LicensePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register<LicenseTask>("license") {
            description = "add a license header to source code"
            group = "from license plugin"
        }
    }
}
```

즉 플러그인의 본체는 새로운 로직을 만드는 게 아니라, 이미 만들어둔 태스크 클래스를 "이 프로젝트에 태스크로 등록해라"라고 지시하는 얇은 접착제(glue) 역할이다.

## 플러그인이 참조하는 리소스: license.txt

`LicenseTask`는 6편에서 `project.layout.settingsDirectory.file("license.txt")` 경로를 `@Input`으로 참조하도록 작성돼 있었다. 이 경로가 실제로 존재해야 태스크가 동작하므로, 프로젝트 루트에 라이선스 헤더 텍스트가 담긴 `license.txt` 파일(Apache License 2.0 문구)을 준비해둔다. 태스크 클래스와 그 클래스가 읽어들이는 데이터 파일은 별개로 관리된다는 점이 드러나는 부분이다.

## 서브프로젝트에 플러그인 적용하기

플러그인은 `plugins { }` 블록에서 플러그인 ID로 적용한다. 3편에서 별도 빌드로 분리해둔 `license-plugin`이 `com.tutorial.license`라는 ID로 게시돼 있다고 가정하고, 이를 `app` 서브프로젝트의 build 스크립트에 추가한다.

```kotlin
// app/build.gradle.kts
plugins {
    application
    id("com.tutorial.license")
}
```

적용이 제대로 됐는지는 `./gradlew :app:tasks`로 확인한다. 출력에 "From license plugin tasks"라는 그룹이 새로 생기고, 그 아래 `license` 태스크와 방금 지정한 설명이 함께 나타난다면 플러그인이 정상적으로 프로젝트에 연결된 것이다.

## 태스크 실행과 결과 확인

`./gradlew :app:license`를 실행하면 `LicenseTask`의 `@TaskAction` 메서드가 동작해 `app` 서브프로젝트의 `.java` 소스 파일 맨 앞에 라이선스 헤더가 삽입된다. 실행 전후로 같은 소스 파일(`App.java`)을 비교해보면, 헤더 텍스트가 파일 맨 위에 그대로 붙어 있는 것을 확인할 수 있다. 빌드 로그에는 플러그인이 속한 `license-plugin` 프로젝트의 컴파일·태스크들이 대부분 `UP-TO-DATE`로 표시되고, 실제로 새로 실행된 것은 `app:license` 하나뿐이라는 점도 눈에 띈다 — 이는 이전 편들에서 다룬 최신 상태(up-to-date) 검사와 캐싱이 여기서도 그대로 작동하고 있다는 뜻이다.

## 핵심 포인트

- 플러그인은 새로운 실행 로직을 담는 그릇이 아니라, 이미 정의된 태스크 클래스를 `apply()` 안에서 `tasks.register()`로 프로젝트에 연결해주는 조립 코드에 가깝다.
- 태스크에 `description`/`group`을 붙이는 습관은 플러그인 단계에서도 그대로 유효하며, `./gradlew tasks` 출력에서 플러그인 출처를 파악하는 데 직접적으로 도움이 된다.
- 플러그인을 서브프로젝트에서 쓰려면 결국 `plugins { id("...") }`로 적용해야 하며, 이는 이전 편(플러그인 다루기)에서 다룬 표준 적용 방식과 동일하다.
- 태스크·태스크가 참조하는 리소스(license.txt)·플러그인 등록 코드는 각각 다른 관심사이며, 6~7편을 거치며 이 세 가지가 어떻게 맞물려 하나의 기능으로 완성되는지 확인할 수 있었다.

## 마무리

여기까지 인터미디에이트 튜토리얼 전 과정이다. Java 앱 초기화부터 빌드 생명주기, 멀티 프로젝트 구성, settings/build 스크립트 구조, 커스텀 태스크, 그리고 이번 편의 커스텀 플러그인까지 이어지는 흐름을 통해 하나의 빌드가 어떻게 조립되는지 큰 그림을 그릴 수 있다. 다음 단계는 어드밴스드 튜토리얼로, 여기서는 플러그인 개발을 더 깊이 파고든다.

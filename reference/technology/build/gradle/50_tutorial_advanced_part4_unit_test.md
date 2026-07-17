# Gradle 어드밴스드 튜토리얼 4편: 단위 테스트 작성하기

> **원문:** https://docs.gradle.org/current/userguide/part4_unit_test.html

## 개요

플러그인도 결국 코드이기 때문에, 배포하기 전에 "제대로 동작하는가"를 검증할 방법이 필요하다. 4편은 1~3편에서 만든 확장(extension)과 커스텀 태스크가 실제로 의도대로 등록되는지를 단위 테스트로 확인하는 단계다. `gradle init`이 생성해주는 기본 테스트 파일을 출발점으로 삼아, 플러그인 이름에 맞게 다듬고 내용을 보강하는 흐름으로 진행한다.

## 선행 조건

1~3편에서 다룬 내용이 전제가 된다.

- 1편: 플러그인 프로젝트 초기화
- 2편: 확장(extension) 추가
- 3편: 커스텀 태스크 작성

## 기본 테스트 파일부터 시작하기

`gradle init`으로 플러그인 프로젝트를 만들면, 플러그인을 적용하고 태스크 존재 여부를 확인하는 샘플 테스트가 함께 생성된다. 이 샘플은 그대로도 동작하지만, 이름이 프로젝트 초기 템플릿(`PluginTutorialPluginTest`) 그대로라 실제 플러그인과 무관해 보인다. 가장 먼저 할 일은 파일명과 클래스명을 지금 만들고 있는 플러그인에 맞게 바꾸는 것이다.

- Kotlin: `PluginTutorialPluginTest.kt` → `SlackPluginTest.kt`
- Groovy: `PluginTutorialPluginTest.groovy` → `SlackPluginTest.groovy`

## 핵심 도구: `ProjectBuilder`

단위 테스트는 실제 빌드를 실행하지 않고도 플러그인 로직만 떼어서 검증할 수 있게 해주는 `ProjectBuilder`를 중심으로 짜여진다. `ProjectBuilder.builder().build()`는 파일시스템에 실제 빌드 산출물을 만들지 않는, 메모리상의 "가짜" `Project` 객체를 하나 만들어준다. 여기에 플러그인을 적용해보고, 기대한 태스크가 등록됐는지만 확인하면 되므로 실행 속도가 빠르고 테스트가 가볍다.

```kotlin
// Kotlin + kotlin.test
val project = ProjectBuilder.builder().build()
project.plugins.apply("org.example.slack")
assertNotNull(project.tasks.findByName("sendTestSlackMessage"))
```

```groovy
// Groovy + Spock
given:
def project = ProjectBuilder.builder().build()

when:
project.plugins.apply("org.example.slack")

then:
project.tasks.findByName("sendTestSlackMessage") != null
```

두 버전 모두 구조는 동일하다.

1. `ProjectBuilder`로 가상 프로젝트를 만든다.
2. `project.plugins.apply(...)`로 플러그인을 적용한다.
3. `project.tasks.findByName(...)`로 태스크가 등록됐는지 확인한다.

## 이 테스트가 검증하는 것 / 검증하지 못하는 것

- 검증하는 것: 플러그인이 에러 없이 적용되는가, 커스텀 태스크가 기대한 이름으로 등록되는가.
- 검증하지 못하는 것: 태스크를 실제로 실행했을 때의 동작이나 결과물. `ProjectBuilder`가 만드는 프로젝트는 태스크를 등록해볼 수 있는 대상일 뿐, 실제 빌드 실행 파이프라인을 태우지는 않기 때문이다.

즉 이 단계의 테스트는 "플러그인이 프로젝트에 올바르게 연결됐는가"라는 가장 기초적인 전제를 빠르게 확인하는 용도이고, 태스크가 실행됐을 때의 실제 동작까지 확인하려면 다음 편에서 다루는 기능 테스트(functional test, TestKit)가 필요하다.

## 테스트 실행

```bash
./gradlew test
```

테스트가 통과하면, 플러그인이 커스텀 태스크를 의도한 대로 등록하고 있다는 뜻이다.

## 핵심 포인트

- `ProjectBuilder`는 실제 빌드 없이 메모리상의 가상 프로젝트를 만들어, 플러그인 적용과 태스크 등록만 빠르게 검증하는 단위 테스트용 도구다.
- 테스트 흐름은 "가상 프로젝트 생성 → 플러그인 적용 → `findByName`으로 태스크 존재 확인"의 세 단계로 단순하다.
- 이 수준의 테스트는 태스크가 *등록됐는지*만 보장하며, 태스크 실행 결과 같은 실제 동작 검증은 다루지 못한다는 한계를 명확히 알아둘 필요가 있다.
- `gradle init`이 만들어주는 기본 테스트 파일의 이름과 내용을 플러그인에 맞게 고쳐 쓰는 것이 실질적인 첫 작업이며, 이후 편에서 다룰 기능 테스트와 짝을 이뤄 플러그인 검증 체계를 완성한다.

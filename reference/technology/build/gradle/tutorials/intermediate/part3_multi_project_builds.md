# Gradle 튜토리얼 Part 3: 멀티 프로젝트 빌드

> **원문:** https://docs.gradle.org/current/userguide/part3_multi_project_builds.html

## 개요

Part 1(프로젝트 초기화)과 Part 2(빌드 생명주기)를 마쳤다면, 이제 프로젝트가 하나의 모듈로 끝나지 않고 여러 서브프로젝트로 쪼개지는 경우를 다룰 차례다. 이 파트에서는 기존 `authoring-tutorial` 예제(루트 프로젝트 + `app` 서브프로젝트)에 새 서브프로젝트를 추가하고, 더 나아가 별도의 Gradle 빌드를 통째로 끌어와 결합하는 컴포짓 빌드까지 다룬다.

## 멀티 프로젝트 빌드의 기본 골격

멀티 프로젝트 빌드는 세 가지 요소로 이루어진다.

- 루트 `settings.gradle(.kts)` — 어떤 서브프로젝트들이 이 빌드에 속하는지 선언하는 진입점.
- 서브프로젝트별 `build.gradle(.kts)` — 각 모듈이 자신만의 플러그인·의존성·태스크를 독립적으로 정의.
- 서브프로젝트별 소스 디렉터리 — 모듈 간 코드가 물리적으로도 분리된다.

예제에서는 이미 `app`이라는 서브프로젝트가 존재하는 상태에서 시작한다.

## 서브프로젝트 추가하기: `lib`

새 서브프로젝트를 추가하는 과정은 다음 순서로 진행된다.

1. 루트에 `lib` 디렉터리를 만들고 `build.gradle.kts`를 작성해 Java 플러그인, 테스트 의존성(JUnit Jupiter), 외부 의존성(Guava) 등을 선언한다.
2. `lib/src/main/java/...` 아래에 실제 소스 코드(`CustomLib` 클래스 등)를 작성한다.
3. 루트 `settings.gradle(.kts)`에 `lib`을 추가한다.

   ```kotlin
   include("app", "lib")
   ```

4. `app/build.gradle.kts`에서 `lib`을 프로젝트 의존성으로 선언한다.

   ```kotlin
   dependencies {
       implementation(project(":lib"))
   }
   ```

5. `App.java`에서 `CustomLib`을 import해 사용하도록 코드를 수정한 뒤 `./gradlew run`으로 확인한다.

이 과정을 마치면 Gradle이 `app`을 빌드하기 전에 의존 관계에 따라 `lib`을 먼저 빌드하고, 두 모듈이 각각 독립적으로도 빌드될 수 있음을 확인할 수 있다.

## 핵심 포인트: 프로젝트 의존성과 외부 의존성은 선언 방식만 다르다

- `implementation(project(":lib"))`처럼 콜론 경로를 쓰면 "같은 빌드 안의 다른 서브프로젝트"를 가리키는 프로젝트 의존성이 되고, `implementation("com.google.guava:guava:33.3.1-jre")`처럼 GAV 좌표를 쓰면 외부 리포지토리에서 내려받는 의존성이 된다.
- 두 방식 모두 같은 `dependencies {}` 블록 안에서 나란히 선언할 수 있어, 서브프로젝트를 늘려 가는 것과 외부 라이브러리를 추가하는 것이 개발자 입장에서는 동일한 절차로 느껴진다.

## 컴포짓 빌드란 무엇인가

컴포짓 빌드(Composite Build)는 "빌드가 다른 빌드를 포함하는" 구조다. 서브프로젝트가 하나의 빌드 안에서 모듈을 나누는 것과 달리, 컴포짓 빌드는 완전히 독립된 별개의 Gradle 빌드를 통째로 끌어와 결합한다. 원문이 제시하는 활용 목적은 다음과 같다.

- 프로젝트 빌드 로직 자체를 별도로 분리해 재사용하기.
- 독립적으로 개발된 여러 빌드(예: 플러그인과 그 플러그인을 쓰는 애플리케이션)를 하나로 묶기.
- 지나치게 커진 하나의 빌드를 여러 개의 격리된 작은 빌드로 쪼개기.

## 빌드에 빌드 추가하기: `license-plugin`

컴포짓 빌드를 실습하기 위해 별도의 Gradle 플러그인 프로젝트를 만들어 메인 빌드에 포함시킨다.

1. `gradle/license-plugin` 디렉터리에서 `gradle init --type kotlin-gradle-plugin`(또는 Groovy 버전)을 실행해 플러그인 전용 빌드를 새로 생성한다. 이 빌드는 자신만의 `settings.gradle(.kts)`, 소스, 테스트, Gradle Wrapper를 갖춘 완전히 독립된 프로젝트다.
2. 루트 `settings.gradle(.kts)`에 다음을 추가해 이 빌드를 포함시킨다.

   ```kotlin
   includeBuild("gradle/license-plugin")
   ```

3. `./gradlew projects`를 실행하면 루트 프로젝트 아래 서브프로젝트(`app`, `lib`) 목록과 별도로, 포함된 빌드(included builds) 섹션에 `license-plugin`이 표시된다.

이후 태스크를 실행할 때 대상 범위가 뚜렷하게 구분된다.

```bash
./gradlew build                          # app, lib 등 서브프로젝트 전체 빌드
./gradlew :app:build                     # app과 그 의존 대상만 빌드
./gradlew :license-plugin:plugin:build   # 포함된 빌드 내부의 플러그인만 빌드
```

## 핵심 포인트: 서브프로젝트와 포함된 빌드는 격리 수준이 다르다

- 서브프로젝트(`include()`)는 같은 빌드에 속하므로 설정·버전·의존성 해석 규칙을 공유하고, 하나의 `settings.gradle(.kts)`가 전체를 조율한다.
- 포함된 빌드(`includeBuild()`)는 자기만의 `settings.gradle(.kts)`를 갖는 별개의 빌드이며, 메인 빌드는 그 결과물(플러그인, 라이브러리 등)만 가져다 쓴다. 즉 "설정을 공유하는 확장"이 아니라 "완성된 산출물을 결합하는 조립"에 가깝다.
- 이 격리 덕분에 `license-plugin` 같은 빌드 로직은 별도 팀이 독립적으로 개발·테스트하거나, 여러 저장소에서 재사용하기 쉬워진다.

## 언제 무엇을 쓸까

- 모듈 수가 늘어나고 서로 의존 관계가 있는 애플리케이션(모바일 앱, 웹 앱, API, 라이브러리, 문서 모듈 등)을 한 저장소에서 관리한다면 멀티 프로젝트 빌드(서브프로젝트)가 기본 선택지다.
- 빌드 로직 자체(컨벤션 플러그인)를 분리해 재사용하거나, 라이브러리를 패치해서 테스트하는 등 "빌드와 빌드 사이"의 결합이 필요하다면 컴포짓 빌드(`includeBuild`)가 더 적합하다.

## 정리

- 멀티 프로젝트 빌드는 루트 `settings.gradle(.kts)`가 `include()`로 서브프로젝트를 선언하고, 각 서브프로젝트는 자체 `build.gradle(.kts)`와 소스를 갖는 구조다.
- 서브프로젝트 간 참조는 `implementation(project(":모듈이름"))`으로 선언하며, Gradle이 의존 순서에 따라 빌드 순서를 자동으로 정리한다.
- 컴포짓 빌드는 `includeBuild()`로 완전히 독립된 별개의 Gradle 빌드를 메인 빌드에 결합하는 방식으로, 서브프로젝트보다 더 강한 격리를 제공한다.
- `./gradlew projects`로 서브프로젝트와 포함된 빌드 목록을 함께 확인할 수 있고, `:app:build`처럼 정규화된 경로로 특정 모듈만 골라 빌드할 수 있다.
- 모듈이 늘어나는 상황에는 서브프로젝트, 빌드 로직·플러그인을 독립적으로 재사용해야 하는 상황에는 컴포짓 빌드를 쓰는 것이 원문이 제시하는 기준이다.

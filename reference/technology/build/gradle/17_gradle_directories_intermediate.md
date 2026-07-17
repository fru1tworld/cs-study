# Gradle 빌드의 디렉터리 구조 (Anatomy of a Gradle Build)

> **원문:** https://docs.gradle.org/current/userguide/gradle_directories_intermediate.html

## 개요

Gradle 빌드는 프로젝트(Project), 태스크(Task), 빌드 스크립트(Build Script), 플러그인(Plugin)이라는 네 요소로 구성됩니다. 이 문서는 이 요소들이 실제 파일 시스템에서 어떤 디렉터리와 파일로 나타나는지, 그리고 프로젝트 루트와 사용자 홈 두 곳에 각각 무엇이 저장되는지를 정리합니다.

## 프로젝트 루트 디렉터리

프로젝트를 체크아웃했을 때 최상단에 위치하는 디렉터리로, 소스 코드와 Gradle이 자동으로 생성하는 산출물이 함께 존재합니다.

```text
gradle-project/
├── .gradle/            # 프로젝트 전용 캐시 (버전별 하위 디렉터리)
├── build/              # 빌드 결과물 출력 디렉터리
├── gradle/wrapper/      # Wrapper 실행에 필요한 jar와 설정
├── gradlew, gradlew.bat # Wrapper 실행 스크립트
├── gradle.properties    # 이 프로젝트에만 적용되는 설정값
├── settings.gradle(.kts) # 프로젝트 구성과 서브프로젝트 목록
├── subproject-one/build.gradle(.kts)
└── subproject-two/build.gradle(.kts)
```

- **`.gradle/`**: Gradle이 자동 생성하는 캐시 디렉터리로, 사용한 Gradle 버전마다(`4.8/`, `4.9/` 등) 하위 폴더가 생겨 증분 빌드(incremental build)에 필요한 정보를 담습니다. 사람이 직접 손댈 필요가 없고, 소스 컨트롤에도 포함하지 않습니다.
- **`build/`**: `build`, `assemble`, `test` 같은 태스크를 실행할 때 생성되는 기본 출력 디렉터리입니다. 컴파일 결과, 테스트 리포트, 패키징 산출물이 여기 쌓이며 `./gradlew clean`으로 정리됩니다.
- **`gradle/wrapper/`**: Wrapper가 사용할 Gradle 배포판 정보와 관련 jar를 담고 있어, 로컬에 Gradle을 설치하지 않고도 지정된 버전으로 빌드할 수 있게 해줍니다.
- **`gradle.properties`**: 이 프로젝트에만 적용되는 설정(JVM 옵션, 커스텀 속성 등)을 담습니다.
- **`settings.gradle(.kts)`**: 이 빌드에 포함되는 서브프로젝트 목록과 전체 구조를 정의하는 최상위 파일입니다.
- **서브프로젝트 디렉터리**: 각 서브프로젝트는 독립적인 `build.gradle(.kts)`를 가지며, 그 안에서 자신만의 플러그인·의존성·태스크를 설정합니다.

## 핵심 포인트: `.gradle`과 `gradle`의 차이

- **`gradle/`** (점 없음): Wrapper 설정처럼 사람이 관리하고 소스 컨트롤에 커밋해야 하는 디렉터리입니다.
- **`.gradle/`** (점 있음): Gradle이 스스로 만들고 관리하는 임시 캐시 디렉터리로, 지워도 다시 생성되며 커밋 대상이 아닙니다.
- **`build/`**: 빌드 산출물 전용 디렉터리이므로 반드시 쓰기 가능해야 하고, 출력 위치를 커스텀 경로로 바꾸는 경우에도 해당 경로가 미리 존재해야 정상 동작합니다.

## Gradle 사용자 홈 디렉터리 (`GRADLE_USER_HOME`)

프로젝트 루트와는 별개로, 로컬 머신 전체에서 공유되는 전역 디렉터리가 존재합니다. 기본 위치는 다음과 같고, `GRADLE_USER_HOME` 환경 변수로 위치를 바꿀 수 있습니다.

- Unix/Linux/Mac: `~/.gradle`
- Windows: `C:\Users\<사용자명>\.gradle`

```text
~/.gradle/
├── caches/           # 버전별 캐시 + 공유 캐시(jars-3, modules-2 등)
├── daemon/           # Gradle 데몬 레지스트리와 로그
├── init.d/           # 모든 빌드에 앞서 실행되는 전역 초기화 스크립트
├── jdks/             # 툴체인 기능으로 내려받은 JDK 배포판
├── wrapper/dists/     # Wrapper가 내려받은 Gradle 배포판
└── gradle.properties # 모든 빌드에 공통 적용되는 전역 설정
```

- **`caches/`**: 여러 프로젝트가 공유하는 의존성 jar(`jars-3`), 모듈 메타데이터(`modules-2`) 등을 버전별로 저장해 반복 다운로드를 줄여줍니다.
- **`daemon/`**: 빌드를 빠르게 재사용하기 위해 백그라운드에서 대기하는 Gradle 데몬 프로세스의 레지스트리와 로그가 Gradle 버전별로 쌓입니다.
- **`init.d/`**: 머신에 존재하는 모든 Gradle 빌드 시작 전에 공통으로 적용할 설정(`my-setup.gradle` 등)을 넣는 곳입니다.
- **`jdks/`**: Gradle 툴체인 기능이 필요에 따라 자동으로 내려받은 JDK들이 보관됩니다.
- **`wrapper/dists/`**: 각 프로젝트의 Wrapper가 요청한 Gradle 배포판(`gradle-4.8-bin`, `gradle-4.9-all` 등)이 버전별로 저장되어, 동일 버전을 여러 프로젝트가 재사용할 수 있습니다.
- **`gradle.properties`**: 특정 프로젝트가 아니라 이 머신에서 실행되는 모든 Gradle 빌드에 공통으로 적용되는 전역 설정입니다.

## 핵심 포인트: 프로젝트 설정 vs 전역 설정

- 같은 이름(`gradle.properties`)의 파일이 프로젝트 루트와 사용자 홈 두 곳에 존재할 수 있으며, 적용 범위가 다릅니다. 전자는 해당 프로젝트에만, 후자는 그 머신의 모든 빌드에 영향을 줍니다.
- 값이 충돌하면 프로젝트 루트의 설정이 사용자 홈의 전역 설정보다 우선 적용됩니다.

## 핵심 포인트: `GRADLE_USER_HOME`과 `GRADLE_HOME` 구분

- **`GRADLE_USER_HOME`**: 캐시, 데몬 로그, 다운로드한 배포판 등 사용자별 상태를 보관하는 디렉터리로, 대부분의 로컬 환경에서 실질적으로 사용됩니다.
- **`GRADLE_HOME`**: 시스템에 직접 설치한 Gradle 배포판 자체의 설치 경로를 가리키는 선택적 값으로, Wrapper를 쓰는 경우에는 사실상 필요하지 않습니다.
- 두 이름이 비슷해 혼동하기 쉬우니 "사용자 상태"와 "설치 경로"라는 역할 차이로 기억해두는 것이 좋습니다.

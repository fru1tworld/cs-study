# Gradle Wrapper 기본 개념

> **원문:** https://docs.gradle.org/current/userguide/gradle_wrapper_basics.html

## 개요
Gradle Wrapper는 Gradle 빌드를 실행할 때 권장되는 표준 방법입니다. 로컬 환경에 Gradle이 미리 설치되어 있지 않아도, 프로젝트에 포함된 스크립트와 설정 파일만으로 지정된 버전의 Gradle을 자동으로 내려받아 실행할 수 있게 해줍니다.

## Wrapper를 쓰는 이유
- **버전 자동 관리**: 프로젝트에서 정한 Gradle 버전을 알아서 다운로드하고 사용합니다.
- **팀 표준화**: 팀원 전체가 항상 동일한 Gradle 버전으로 빌드하게 되어 "내 컴퓨터에서는 되는데" 같은 문제를 줄입니다.
- **환경 일관성**: 로컬 개발 환경, IDE, CI 서버 등 어디서 실행하든 같은 버전의 Gradle이 사용됩니다.
- **설치 부담 감소**: 사용자가 Gradle을 별도로 설치할 필요가 없습니다.

## 핵심 포인트
- Wrapper는 Gradle 실행 파일 자체가 아니라, "지정된 버전의 Gradle을 필요하면 다운로드해서 실행해 주는" 얇은 래퍼입니다.
- 시스템에 설치된 `gradle` 명령을 직접 쓰는 것보다 `gradlew`/`gradlew.bat`를 쓰는 것이 항상 권장되는 방식입니다.

## Wrapper 구성 파일
Gradle 프로젝트에는 다음 네 가지 Wrapper 관련 파일이 존재합니다.

| 파일 | 역할 |
|---|---|
| `gradlew` | Unix 계열(Linux/macOS)에서 실행하는 셸 스크립트 |
| `gradlew.bat` | Windows에서 실행하는 배치 스크립트 |
| `gradle/wrapper/gradle-wrapper.jar` | 지정된 Gradle 버전을 다운로드·설치하는 로직이 담긴 작은 JAR |
| `gradle/wrapper/gradle-wrapper.properties` | 다운로드할 Gradle 배포판의 URL, 배포 형식(zip/tarball) 등을 담은 설정 파일 |

디렉터리 구조는 대략 다음과 같습니다.

```
project-root/
├── gradlew
├── gradlew.bat
└── gradle/
    └── wrapper/
        ├── gradle-wrapper.jar
        └── gradle-wrapper.properties
```

이 파일들은 **직접 수정하지 않는 것이 원칙**입니다. 버전을 바꾸고 싶다면 아래에 나오는 Wrapper 갱신 명령을 사용해야 합니다.

## 사용법

### 빌드 실행
- Linux/macOS: `./gradlew build`
- Windows(PowerShell/cmd): `gradlew.bat build`

다른 디렉터리에서 실행해야 한다면 `gradlew` 스크립트까지의 상대 경로를 지정해서 호출하면 됩니다.

### 버전 확인
```
./gradlew --version
```

### 버전 업그레이드
```
./gradlew wrapper --gradle-version 7.2
```
이 명령을 실행하면 `gradle-wrapper.properties`의 배포판 URL이 갱신되고, 이후 빌드부터는 새 버전이 다운로드되어 사용됩니다.

## Wrapper가 없는 경우
만약 저장소에 Wrapper 파일이 없다면 두 가지 경우 중 하나입니다.
1. 애초에 Gradle 프로젝트가 아니다.
2. Gradle 프로젝트이지만 Wrapper가 아직 생성되지 않았다.

이때는 Gradle이 이미 설치되어 있는 머신에서 `gradle wrapper` 명령을 실행해 Wrapper 파일들을 새로 생성해 주면 됩니다.

## 정리
- 로컬 `gradle` 명령 직접 실행과 `gradlew`(Wrapper) 실행은 다른 방식이며, 실무에서는 항상 Wrapper 사용이 권장됩니다.
- Wrapper 파일은 버전 관리 시스템에 커밋해서 팀 전체와 CI가 동일한 설정을 공유하도록 합니다.
- 관련 문서로 CLI 기본 사용법, Build Scan, Gradle Daemon 등을 함께 참고하면 좋습니다.

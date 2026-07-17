# Gradle 보안 모범 사례 (Best Practices for Security)

> **원문:** https://docs.gradle.org/current/userguide/best_practices_security.html

## 개요

Gradle Wrapper는 빌드 로직이 실행되기 전에 먼저 동작하는 컴포넌트입니다. 즉, 프로젝트가 정의한 어떤 보안 장치보다도 먼저 신뢰할 수 없는 코드가 실행될 여지가 있는 지점입니다. 이 문서는 그 위험을 줄이기 위한 두 가지 핵심 규칙, "Gradle 배포판 체크섬 검증"과 "Wrapper 업그레이드 시 무결성 검증"을 다룹니다.

## 규칙 1: Gradle 배포판의 SHA-256 체크섬을 검증하라

- **Do**: `gradle-wrapper.properties`에 `distributionSha256Sum` 속성을 함께 선언해, 다운로드한 Gradle 배포판이 실제로 공식 배포본과 동일한지 확인합니다.
- **Don't**: `distributionUrl`만 지정하고 체크섬 검증 없이 배포판을 그대로 받아 실행하지 않습니다. 이 경우 손상되었거나 변조된 Gradle 실행 파일을 그대로 받아들이게 됩니다.

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.6-bin.zip
distributionSha256Sum=<gradle.org에 공시된 SHA-256 값>
```

### 핵심 포인트

- 체크섬 값은 Gradle 공식 릴리스 페이지(release-checksums)에 게시된 값을 그대로 가져와야 하며, 임의로 생성하거나 추정해서는 안 됩니다.
- 이 설정은 "다운로드한 zip 파일 == 공식 배포판"이라는 사실만 보장할 뿐, Wrapper JAR 자체의 무결성까지 보장하지는 않습니다. 그래서 규칙 2가 별도로 필요합니다.

## 규칙 2: Wrapper를 업그레이드할 때마다 무결성을 검증하라

Wrapper 업그레이드는 단순히 버전 문자열을 바꾸는 작업이 아니라, 빌드보다 먼저 실행되는 실행 파일을 새로 신뢰하는 행위입니다. 따라서 다음 두 파일을 매번 점검 대상으로 삼아야 합니다.

- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties` (`distributionUrl`, `distributionSha256Sum`)

### 검증 체크리스트

- **Do**: JAR 파일이 공식 Gradle 바이너리와 변경 없이 동일한지, 그 체크섬이 `gradle.org/release-checksums`에 공시된 값과 일치하는지 확인합니다.
- **Do**: `distributionUrl`이 실제로 의도한 Gradle 릴리스를 가리키는지, `distributionSha256Sum`이 그 릴리스 버전에 맞는 값인지 함께 확인합니다.
- **Don't**: PR이나 외부 기여로 들어온 Wrapper 관련 파일 변경을 체크섬 대조 없이 그대로 머지하지 않습니다. 검증되지 않은 Wrapper를 실행하면 빌드의 다른 안전장치가 작동하기도 전에 신뢰할 수 없는 코드가 실행될 수 있습니다.

### 핵심 포인트: 자동화

- 매번 수동으로 대조하는 대신 CI에서 자동 검증하는 것이 권장됩니다.
- GitHub Actions를 사용한다면 `setup-gradle` 액션(v4 이상)이 모든 `gradle-wrapper.jar`를 검사해, 알려지지 않은 JAR가 발견되면 빌드를 실패시켜 줍니다.
- 별도의 Gradle Wrapper 검증 액션(Wrapper Validation Action)을 붙이는 방법도 있습니다.

## 두 규칙의 관계

- 규칙 1은 "배포판 zip 파일"의 무결성, 규칙 2는 "Wrapper JAR + 설정 파일"의 무결성을 다룬다는 점에서 검증 대상이 다릅니다.
- 두 검증 모두 통과해야 비로소 "내가 실행하는 Gradle이 공식적이고 변조되지 않았다"는 공급망(Supply Chain) 신뢰 사슬이 완성됩니다.
- 신규 프로젝트를 셋업할 때뿐 아니라, Wrapper 버전을 올릴 때마다(`./gradlew wrapper --gradle-version ...`) 반복적으로 점검해야 하는 항목입니다.

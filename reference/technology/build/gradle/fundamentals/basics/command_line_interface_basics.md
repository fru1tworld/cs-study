# Gradle 커맨드라인 인터페이스 기초

> **원문:** https://docs.gradle.org/current/userguide/command_line_interface_basics.html

## 개요
IDE를 사용하지 않을 때 Gradle과 상호작용하는 기본 수단은 커맨드라인이다. 태스크 실행, 빌드 정보 확인, 의존성 관리, 로그 레벨 조정 등을 모두 명령줄 옵션 조합으로 처리할 수 있다.

## 기본 명령 구조
Gradle 명령은 다음과 같은 형태를 따른다.

```
gradle [태스크...] [--옵션...]
```

- 태스크는 공백으로 구분해 여러 개를 한 번에 지정할 수 있다.
- 옵션은 태스크 이름의 앞이나 뒤 어디에 와도 상관없다.

```bash
gradle build
gradle clean build          # 여러 태스크를 순서대로 실행
gradle --build-cache build  # 옵션을 태스크보다 먼저 써도 동일하게 동작
```

## 옵션 작성 규칙

### 값을 받는 옵션
값이 필요한 옵션은 `=`로 값을 지정하는 것이 명확하다.

```bash
gradle build --console=plain
```

### on/off 토글 옵션
일부 옵션은 `--no-` 접두사를 붙여 반대 동작을 지정하는 짝을 가진다.

```bash
gradle build --build-cache      # 빌드 캐시 사용
gradle build --no-build-cache   # 빌드 캐시 미사용
```

### 짧은 옵션 표기
자주 쓰는 옵션은 짧은 별칭이 있다. 예를 들어 `-h`는 `--help`와 동일하다.

## 태스크 실행과 프로젝트 지정
멀티 프로젝트 빌드에서는 콜론(`:`)으로 프로젝트 경로를 표현해 특정 하위 프로젝트의 태스크만 실행할 수 있다.

```bash
gradle :test              # 루트 프로젝트의 test 태스크
gradle :app:test          # app 하위 프로젝트의 test 태스크
gradle test               # 현재 디렉터리를 기준으로 실행
```

## 태스크별 옵션
태스크 자체가 고유한 옵션을 가질 수 있으며, 태스크 이름 뒤에 붙여 전달한다.

```bash
gradle taskName --exampleOption=exampleValue
```

즉 커맨드라인 옵션은 두 종류로 나뉜다.
- Gradle 실행 자체를 제어하는 전역 옵션 (`--build-cache`, `--console` 등)
- 특정 태스크의 동작만 바꾸는 태스크 전용 옵션

## Gradle Wrapper 사용 권장
문서는 `gradle` 명령을 직접 쓰기보다 **Gradle Wrapper**(`./gradlew`, Windows는 `gradlew.bat`) 사용을 강력히 권장한다. Wrapper를 쓰면 프로젝트에 지정된 Gradle 버전이 로컬 설치 여부와 무관하게 그대로 사용되어, 팀원·CI 환경 간 빌드 결과가 달라지는 문제를 막을 수 있다.

```bash
./gradlew build       # macOS / Linux
gradlew.bat build     # Windows
```

## 핵심 포인트
- 명령 형식은 `gradle [태스크...] [--옵션...]`이며 옵션·태스크 순서는 자유롭다.
- 값이 있는 옵션은 `=`로, on/off 옵션은 `--no-` 짝으로 표현한다.
- 멀티 프로젝트에서는 `:하위프로젝트:태스크` 형태로 실행 대상을 좁힐 수 있다.
- 태스크 전용 옵션과 Gradle 전역 옵션을 구분해서 이해해야 한다.
- 실무에서는 `gradle` 대신 `./gradlew`(Wrapper)를 쓰는 것이 표준이다.
- 이 문서는 개요(basics) 수준이며, 전체 옵션 목록은 별도의 Command-Line Interface 레퍼런스 문서에서 다룬다.

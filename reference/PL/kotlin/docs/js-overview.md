# Kotlin/JavaScript 개요

## Kotlin/JS란?

**Kotlin/JavaScript (Kotlin/JS)**는 Kotlin 코드, Kotlin 표준 라이브러리 및 호환 가능한 의존성을 JavaScript로 트랜스파일할 수 있게 해줍니다. 이를 통해 JavaScript를 지원하는 모든 환경에서 Kotlin 애플리케이션을 실행할 수 있습니다.

주요 기능:
- **Kotlin Multiplatform Gradle 플러그인** (`kotlin.multiplatform`)을 통해 구성 및 관리
- **ES5** 및 **ES2015** JavaScript 표준 지원
- npm 의존성 관리 및 애플리케이션 번들링 지원
- 일반적인 모듈 시스템과 호환: ESM, CommonJS, UMD 및 AMD

---

## Kotlin/JS 사용 사례

### 1. 프론트엔드와 JVM 백엔드 간 공통 로직 공유

Kotlin/JVM 백엔드와 웹 애플리케이션 간에 DTO, 유효성 검사 규칙, 인증 로직 및 REST API 추상화를 공유합니다.

### 2. Android, iOS, 웹 간 로직 공유

Kotlin으로 비즈니스 로직을 한 번 작성하고 각 플랫폼에서 네이티브 UI를 사용하면서 코드 중복을 피합니다.

### 3. 프론트엔드 웹 애플리케이션 구축

여러 접근 방식 사용 가능:

- **Compose 기반 프레임워크**: Android 개발에 익숙하다면 [Kobweb](https://kobweb.varabyte.com/) 또는 [Kilua](https://kilua.dev/) 같은 프레임워크 사용
- **React와 Kotlin**: React Redux, React Router, styled-components를 지원하는 [JavaScript 라이브러리용 Kotlin 래퍼](https://github.com/JetBrains/kotlin-wrappers)를 사용하여 타입 안전한 React 애플리케이션 구축
- **Kotlin/JS 프레임워크**: Kotlin 생태계와 통합되는 전용 프레임워크 사용

### 4. Compose Multiplatform으로 구형 브라우저 지원

모바일/데스크톱 UI를 재사용하면서 레거시 브라우저 지원을 확장합니다.

### 5. 서버 사이드 및 서버리스 애플리케이션

빠른 시작과 낮은 메모리 사용량으로 타입 안전한 서버 사이드 개발을 위해 [`kotlinx-nodejs`](https://github.com/Kotlin/kotlinx-nodejs) 라이브러리와 함께 Node.js 타겟을 사용합니다.

---

## 시작하기

### 사전 준비
- [Kotlin 기본 문법](basic-syntax.html)에 익숙해지기
- [Kotlin 투어](kotlin-tour-welcome.html) 탐색

### 다음 단계
1. **설정**: [Kotlin/JS 프로젝트 설정 가이드](js-project-setup.html) 검토
2. **샘플 프로젝트**: 패턴과 모범 사례를 위해 제공된 예제 학습
3. **탐색**: 디버깅 및 테스트 같은 고급 주제로 이동

---

## 샘플 프로젝트

| 프로젝트 | 설명 |
|---------|-------------|
| [Spring & Angular를 사용한 Petclinic](https://github.com/Kotlin/kmp-spring-petclinic/#readme) | DTO와 유효성 검사를 공유하는 Spring Boot 백엔드 + Angular 프론트엔드 |
| [풀스택 컨퍼런스 CMS](https://github.com/Kotlin/kmp-fullstack-conference-cms/#readme) | Ktor, Jetpack Compose, Vue.js 코드 공유 접근 방식 |
| [Kobweb의 Todo 앱](https://github.com/varabyte/kobweb-templates/tree/main/examples/todo/#readme) | Kobweb를 사용한 Android 익숙한 웹 UI 접근 방식 |
| [로직 공유: Android/iOS/Web](https://github.com/Kotlin/kmp-logic-sharing-simple-example/#readme) | 각 플랫폼의 네이티브 UI와 공통 로직 |
| [풀스택 Todo 리스트](https://github.com/kotlin-hands-on/jvm-js-fullstack/#readme) | Ktor 백엔드 + React 프론트엔드 협업 |

---

## Kotlin/JS 프레임워크

내장 컴포넌트, 라우팅, 상태 관리로 웹 개발을 단순화하는 다양한 저자들이 작성한 [사용 가능한 프레임워크](js-frameworks.html)를 참조하세요.

---

## 커뮤니티 및 지원

커뮤니티 및 Kotlin/JS 팀과 연결하려면 공식 [Kotlin Slack](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up)의 **[#javascript 채널](https://kotlinlang.slack.com/archives/C0B8L3U69)**에 참여하세요.

---

## 다음 단계

- [Kotlin/JS 프로젝트 설정](js-project-setup.html)
- [Kotlin/JS 프로젝트 실행](running-kotlin-js.html)
- [Kotlin/JS 코드 디버깅](js-debugging.html)
- [Kotlin/JS에서 테스트 실행](js-running-tests.html)

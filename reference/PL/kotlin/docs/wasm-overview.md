# Kotlin/Wasm 개요

## Kotlin/Wasm이란?

Kotlin/Wasm은 Kotlin 코드를 **WebAssembly (Wasm)** 형식으로 컴파일할 수 있게 해주어 Wasm을 지원하는 다양한 환경과 장치에서 애플리케이션을 실행할 수 있게 합니다.

WebAssembly는 스택 기반 가상 머신을 위한 바이너리 명령어 형식으로 다음과 같은 특징이 있습니다:
- **플랫폼 독립적**: 자체 가상 머신에서 실행
- **언어 무관**: Kotlin 및 기타 언어를 위한 컴파일 타겟 제공

## 주요 사용 사례

### 1. Compose Multiplatform을 사용한 웹 애플리케이션

- **Compose Multiplatform**을 사용하여 UI를 한 번 빌드하고 플랫폼 간 공유
- `wasm-js` 타겟을 사용하며 브라우저에서 실행
- 웹 프로젝트에서 모바일 및 데스크톱 UI 재사용
- [온라인 데모 체험](https://zal.im/wasm/jetsnack/)

### 2. WASI를 사용한 서버 사이드 애플리케이션

- 서버 사이드 애플리케이션을 위한 **WebAssembly System Interface (WASI)** 사용
- 브라우저 환경 외부에서 Kotlin 애플리케이션 실행 가능
- `wasm-wasi` 타겟 사용
- 다양한 환경에서 안전하고 표준화된 인터페이스 제공

## 브라우저 요구 사항

Kotlin/Wasm 애플리케이션을 브라우저에서 실행하려면 사용자에게 다음이 필요합니다:
- WebAssembly의 **가비지 컬렉션** 제안을 지원하는 브라우저 버전
- **레거시 예외 처리** 제안 지원
- 현재 브라우저 지원 현황은 [WebAssembly 로드맵](https://webassembly.org/roadmap/) 확인

## 성능

Kotlin/Wasm (베타 상태)은 유망한 성능을 보여줍니다:
- **실행 속도가 JavaScript를 능가**
- **JVM 성능 수준에 근접**
- 최신 Chrome 버전에서 정기적인 벤치마크 수행

## 브라우저 API 지원

Kotlin/Wasm 표준 라이브러리는 다음에 대한 선언을 제공합니다:
- **DOM API** 접근 및 조작
- 사용자 정의 선언 없이 **Fetch API**
- **JavaScript 상호운용성** 기능

Kotlin/Wasm-JavaScript 상호운용성을 사용하여 사용자 정의 선언을 정의할 수도 있습니다.

## 시작하기

- **[Kotlin/Wasm 및 Compose Multiplatform 시작하기](wasm-get-started.html)**
- **[Kotlin/Wasm 및 WASI 시작 튜토리얼](wasm-wasi.html)**
- [GitHub 예제](https://github.com/Kotlin/kotlin-wasm-examples) 탐색
- [YouTube 재생목록](https://kotl.in/wasm-pl) 시청

## 주요 장점

### 1. 성능

- JavaScript보다 빠른 실행
- 거의 네이티브에 가까운 성능
- 작은 바이너리 크기

### 2. 보안

- 메모리 안전한 샌드박스 환경
- 플랫폼 간 일관된 동작

### 3. 이식성

- 브라우저, 서버, 엣지 컴퓨팅 등 다양한 환경 지원
- WASI를 통한 시스템 접근

### 4. 코드 공유

- Kotlin Multiplatform과 통합
- 기존 Kotlin 코드 재사용

## 지원되는 타겟

| 타겟 | 환경 | 사용 사례 |
|------|------|----------|
| `wasm-js` | 브라우저 | 웹 애플리케이션, Compose Multiplatform 웹 |
| `wasm-wasi` | 서버/런타임 | 서버 사이드, CLI 도구 |

## 커뮤니티 피드백

**Kotlin/Wasm:**
- Slack: [#webassembly 채널](https://kotlinlang.slack.com/archives/CDFP59223)
- 이슈: [YouTrack](https://youtrack.jetbrains.com/issue/KT-56492)

**Compose Multiplatform:**
- Slack: [#compose-web 채널](https://slack-chats.kotlinlang.org/c/compose-web)
- 이슈: [GitHub](https://github.com/JetBrains/compose-multiplatform/issues)

## 다음 단계

- Kotlin/Wasm 프로젝트 설정 방법 알아보기
- Compose Multiplatform과 함께 사용하기
- JavaScript와의 상호운용성 탐색

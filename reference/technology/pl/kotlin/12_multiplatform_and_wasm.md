# 멀티플랫폼과 Wasm

# Kotlin Multiplatform

> **원문:** https://kotlinlang.org/docs/multiplatform.html

## Kotlin Multiplatform이란?

**Kotlin Multiplatform**(KMP)은 JetBrains의 오픈소스 기술로, **Android, iOS, 데스크톱, 웹, 서버** 간에 코드를 공유하면서도 네이티브 개발의 장점을 유지할 수 있게 해줍니다.

**Compose Multiplatform**을 사용하면 여러 플랫폼에서 UI 코드도 공유하여 코드 재사용을 극대화할 수 있습니다.

---

## 주요 이점

### 1. 비용 효율성과 빠른 출시

- 로직과 UI 코드를 공유하여 중복과 유지보수 비용 절감
- 여러 플랫폼에 동시에 기능 출시 가능
- KMP 도입 후 55%의 사용자가 협업 개선을 보고함
- 65%의 팀이 성능과 품질 향상을 보고함 (KMP Survey Q2 2024)

### 2. 코드 공유의 유연성

- 격리된 모듈(네트워킹, 스토리지 등) 공유
- 시간이 지남에 따라 점진적으로 공유 코드 확장
- 다양한 옵션 제공:
  - UI는 네이티브로 유지하면서 모든 비즈니스 로직 공유
  - Compose Multiplatform을 사용하여 점진적으로 UI 마이그레이션
  - 네이티브와 공유 UI 코드 혼합

### 3. iOS에서의 네이티브 느낌

- SwiftUI, UIKit 또는 Compose Multiplatform을 사용하여 UI 구축
- 각 플랫폼에서 네이티브처럼 느껴지는 앱 생성

### 4. 네이티브 성능

- 네이티브 바이너리를 위한 **Kotlin/Native** 활용
- 플랫폼 API에 직접 접근
- iOS에서 SwiftUI와 비슷한 성능

### 5. 원활한 툴링

- Kotlin Multiplatform IDE 플러그인과 함께 IntelliJ IDEA 및 Android Studio 지원
- 공통 UI 미리보기
- Compose Multiplatform용 핫 리로드
- 크로스 언어 네비게이션, 리팩토링, 디버깅

### 6. AI 기반 개발

- KMP 작업을 위한 **Junie** (JetBrains의 AI 코딩 에이전트) 통합

---

## 지원 플랫폼

Kotlin Multiplatform의 주요 장점 중 하나는 다양한 플랫폼에 대한 광범위한 지원입니다:

- **Android**
- **iOS**
- **데스크톱** (Windows, macOS, Linux)
- **웹** (JavaScript 및 WebAssembly)
- **서버** (Java Virtual Machine)

---

## KMP를 사용하는 기업

Google, Duolingo, Forbes, Philips, McDonald's, Bolt, H&M, Baidu, Kuaishou, Bilibili 등이 프로덕션 애플리케이션에 KMP를 도입했습니다.

---

## 사용 사례

### 스타트업 및 MVP

스타트업은 종종 제한된 리소스와 빠듯한 일정으로 운영됩니다. 개발 효율성과 비용 효과를 극대화하기 위해 공유 코드베이스를 사용하여 여러 플랫폼을 타겟팅하는 것이 유리합니다 - 특히 시장 출시 시간이 중요한 초기 단계 제품이나 MVP에서 더욱 그렇습니다.

### 중소기업

중소기업은 종종 작은 팀을 유지하면서도 성숙하고 기능이 풍부한 제품을 관리합니다. Kotlin Multiplatform을 사용하면 사용자가 기대하는 네이티브 룩앤필을 유지하면서 핵심 로직을 공유할 수 있습니다. 기존 코드베이스에 의존하여 이러한 팀은 사용자 경험을 손상시키지 않으면서 개발을 가속화할 수 있습니다.

### 엔터프라이즈 애플리케이션

대규모 애플리케이션은 일반적으로 광범위한 코드베이스가 있으며, 새로운 기능이 지속적으로 추가되고, 모든 플랫폼에서 동일하게 작동해야 하는 복잡한 비즈니스 로직이 있습니다. Kotlin Multiplatform은 점진적 통합을 제공하여 팀이 단계적으로 도입할 수 있게 합니다.

### 모바일 코드 공유

Kotlin Multiplatform의 주요 사용 사례 중 하나는 모바일 플랫폼 간 코드 공유입니다. iOS와 Android 앱 간에 애플리케이션 로직을 공유하고, 네이티브 UI를 구현하거나 플랫폼 API로 작업해야 할 때만 플랫폼별 코드를 작성할 수 있습니다.

---

## 안정성 및 도입

2023년 11월, JetBrains는 Kotlin Multiplatform이 이제 Stable임을 발표했습니다. Google I/O 2024에서 Google은 Android와 iOS 간 비즈니스 로직 공유를 위한 Kotlin Multiplatform 사용에 대한 공식 지원을 발표했습니다.

Kotlin Multiplatform 사용량은 단 1년 만에 두 배 이상 증가했습니다 - 2024년 7%에서 2025년 18%로 증가하여 이 기술의 증가하는 모멘텀을 강조합니다.

---

## 시작하기

- **[퀵스타트 가이드](quickstart.html)**: 환경 설정 및 샘플 애플리케이션 실행
- **[케이스 스터디](https://kotlinlang.org/case-studies/?type=multiplatform)**: 실제 도입 사례 학습
- **[샘플 앱](multiplatform-samples.html)**: 엄선된 예제 탐색
- **[라이브러리 검색](https://klibs.io/)**: 멀티플랫폼 라이브러리 찾기

---

## 다음 단계

- Kotlin/JVM, Kotlin/JS, Kotlin/Native 및 Kotlin/Wasm에 대해 자세히 알아보세요
- Compose Multiplatform을 사용하여 첫 번째 앱 만들기
- Kotlin Multiplatform 프로젝트 구조 이해하기

---

# Kotlin/Wasm 및 Compose Multiplatform 시작하기

> **원문:** https://kotlinlang.org/docs/wasm-get-started.html

이 튜토리얼은 IntelliJ IDEA에서 **Kotlin/Wasm**과 함께 **Compose Multiplatform** 앱을 실행하고 웹 게시용 아티팩트를 생성하는 방법을 보여줍니다.

## 프로젝트 생성

1. Kotlin Multiplatform 개발을 위한 **환경 설정**
2. IntelliJ IDEA에서 **File | New | Project**로 이동
3. 왼쪽 패널에서 **Kotlin Multiplatform** 선택
   - *대안:* [KMP 웹 마법사](https://kmp.jetbrains.com/?web=true&webui=compose&includeTests=true) 사용
4. 새 프로젝트 창 구성:
   - **Name:** WasmDemo
   - **Group:** wasm.project.demo
   - **Artifact:** wasmdemo
5. **Web target** 및 **Share UI 탭** 선택 (다른 옵션 없음)
6. **Create** 클릭

## 애플리케이션 실행

1. 프로젝트가 로드된 후 실행 구성에서 **composeApp [wasmJs]** 선택
2. **Run** 클릭
3. 앱이 자동으로 `http://localhost:8080/`에서 열림
   - *참고:* 포트가 다를 수 있음; 실제 포트는 Gradle 출력 확인
4. "Click me!" 버튼을 클릭하여 Compose Multiplatform 로고 표시

## 아티팩트 생성

게시용 아티팩트를 생성하려면:

1. **Gradle 도구 창** 열기 (View | Tool Windows | Gradle)
2. **wasmdemo | Tasks | kotlin browser**로 이동
3. **wasmJsBrowserDistribution** 태스크 실행
   - *요구 사항:* Java 11+ (Java 17+ 권장)

**대체 명령:**

```bash
./gradlew wasmJsBrowserDistribution
```

**출력 위치:** `composeApp/build/dist/wasmJs/productionExecutable`

## 애플리케이션 게시

선호하는 플랫폼을 사용하여 배포:
- **GitHub Pages** - [시작 가이드](https://docs.github.com/en/pages/getting-started-with-github-pages/creating-a-github-pages-site#creating-your-site)
- **Cloudflare** - [Workers 문서](https://developers.cloudflare.com/workers/)
- **Apache HTTP Server** - [시작 가이드](https://httpd.apache.org/docs/2.4/getting-started.html)

## 프로젝트 구조

### 디렉토리 레이아웃

```
WasmDemo/
├── composeApp/
│   ├── src/
│   │   ├── commonMain/     # 공유 코드
│   │   └── wasmJsMain/     # Wasm 특정 코드
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml  # 버전 카탈로그
├── build.gradle.kts
└── settings.gradle.kts
```

### build.gradle.kts 예시

```kotlin
plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.ui)
            }
        }
        val wasmJsMain by getting
    }
}

compose {
    // Compose 구성
}
```

## 간단한 UI 예제

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun App() {
    var count by remember { mutableStateOf(0) }

    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Count: $count")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { count++ }) {
                Text("Click me!")
            }
        }
    }
}
```

## 개발 서버 실행

개발 중 핫 리로드 사용:

```bash
./gradlew wasmJsBrowserDevelopmentRun --continuous
```

또는 Gradle 도구 창에서 `wasmJsBrowserDevelopmentRun` 태스크 실행.

## 브라우저 호환성

Kotlin/Wasm 애플리케이션을 실행하려면 WebAssembly GC를 지원하는 브라우저가 필요합니다:

| 브라우저 | 최소 버전 |
|---------|----------|
| Chrome | 119+ |
| Firefox | 120+ |
| Safari | 18.2+ |
| Edge | 119+ |

## 문제 해결

### 일반적인 문제

1. **"브라우저에서 열리지 않음"**
   - 브라우저가 WebAssembly GC를 지원하는지 확인
   - 최신 Chrome 또는 Firefox 사용

2. **"빌드 실패"**
   - Java 버전 확인 (11+ 필요)
   - Gradle 버전 호환성 확인

3. **"느린 초기 로드"**
   - 프로덕션 빌드 사용 (개발 빌드는 더 큼)
   - 적절한 캐싱 구성

## 다음 단계

- [iOS/Android용 Compose Multiplatform](/docs/multiplatform/compose-multiplatform-create-first-app.html) 알아보기
- Kotlin/Wasm 예제 탐색:
  - [KotlinConf 애플리케이션](https://github.com/JetBrains/kotlinconf-app)
  - [Compose 이미지 뷰어](https://github.com/JetBrains/compose-multiplatform/tree/master/examples/imageviewer)
  - [Node.js 예제](https://github.com/Kotlin/kotlin-wasm-nodejs-template)
  - [WASI 예제](https://github.com/Kotlin/kotlin-wasm-wasi-template)
- [Kotlin/Wasm Slack 커뮤니티](https://slack-chats.kotlinlang.org/c/webassembly) 참여

---

# Kotlin/Wasm 개요

> **원문:** https://kotlinlang.org/docs/wasm-overview.html

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

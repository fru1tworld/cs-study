# Kotlin/Wasm 및 Compose Multiplatform 시작하기

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

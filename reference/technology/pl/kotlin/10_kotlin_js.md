# 코틀린/JS

# Kotlin/JS 시작하기 - 완전 가이드

> **원문:** https://kotlinlang.org/docs/js-get-started.html

## 개요

이 튜토리얼은 Kotlin/JavaScript (Kotlin/JS)를 사용하여 브라우저용 웹 애플리케이션을 만드는 방법을 설명합니다. 두 가지 접근 방식을 사용할 수 있습니다:

1. **IntelliJ IDEA** - 버전 관리에서 프로젝트 템플릿 복제
2. **Gradle** - 더 나은 이해를 위해 빌드 파일을 수동으로 생성

## 방법 1: IntelliJ IDEA에서 애플리케이션 생성

### 환경 설정

1. 최신 버전의 [IntelliJ IDEA](https://www.jetbrains.com/idea/) 다운로드 및 설치
2. Kotlin Multiplatform 개발을 위한 환경 설정

### 프로젝트 생성

1. **File | New | Project from Version Control** 선택
2. URL 입력: `https://github.com/Kotlin/kmp-js-wizard`
3. **Clone** 클릭

### 프로젝트 구성

1. `kmp-js-wizard/gradle/libs.versions.toml` 열기
2. Kotlin 버전이 Kotlin Multiplatform Gradle 플러그인 버전과 일치하는지 확인:

```
[versions]
kotlin = "2.3.10"

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
```

3. **Load Gradle Changes** 버튼을 사용하여 Gradle 파일 동기화

### 애플리케이션 빌드 및 실행

1. `src/jsMain/kotlin/Main.kt` 열기
2. `main()` 함수에서 **Run 아이콘** 클릭
3. 웹 애플리케이션이 자동으로 `http://localhost:8080/`에서 열림

애플리케이션은 [`kotlinx.browser`](https://github.com/Kotlin/kotlinx-browser) API를 사용하여 페이지에 "Hello, Kotlin/JS!"를 표시합니다.

### 연속 빌드 활성화

1. 실행 구성에서 `jsMain [js]` 선택
2. **More Actions | Edit** 클릭
3. **Run 필드**에 입력: `jsBrowserDevelopmentRun --continuous`
4. **OK** 클릭

이제 변경 사항을 저장하면 Gradle이 자동으로 다시 빌드하고 브라우저를 핫 리로드합니다.

## 애플리케이션 수정 - 글자 수 세기 예제

### 입력 요소 추가

```kotlin
fun Element.appendInput() {
    val input = document.createElement("input")
    appendChild(input)
}

fun main() {
    document.body?.appendInput()
}
```

### 입력 이벤트 처리 추가

```kotlin
fun Element.appendInput(onChange: (String) -> Unit = {}) {
    val input = document.createElement("input").apply {
        addEventListener("change") { event ->
            onChange(event.target.unsafeCast<HTMLInputElement>().value)
        }
    }
    appendChild(input)
}

fun main() {
    document.body?.appendInput(onChange = { println(it) })
}
```

### 출력 요소 추가

```kotlin
fun Element.appendTextContainer(): Element {
    return document.createElement("p").also(::appendChild)
}

fun main() {
    val output = document.body?.appendTextContainer()
    document.body?.appendInput(onChange = { println(it) })
}
```

### 입력 처리하여 글자 수 세기

```kotlin
fun main() {
    val output = document.body?.appendTextContainer()
    document.body?.appendInput(onChange = { name ->
        name.replace(" ", "").let {
            output?.textContent = "Your name contains ${it.length} letters"
        }
    })
}
```

### 고유 글자 수 세기 (고급)

```kotlin
fun String.countDistinctCharacters() =
    lowercase().toList().distinct().count()

fun main() {
    val output = document.body?.appendTextContainer()
    document.body?.appendInput(onChange = { name ->
        name.replace(" ", "").let {
            output?.textContent = "Your name contains ${it.countDistinctCharacters()} unique letters"
        }
    })
}
```

**사용된 주요 함수:**
- `lowercase()` - 소문자로 변환
- `toList()` - 문자열을 문자 리스트로 변환
- `distinct()` - 고유 문자 필터링
- `count()` - 요소 개수 세기
- `replace()` - 공백 제거
- `let{}` - 스코프 함수
- `${}`를 사용한 문자열 템플릿

## 방법 2: Gradle을 사용하여 애플리케이션 생성

### 프로젝트 파일 생성

1. **Gradle 호환성 확인** - [호환성 표](gradle-configure-project.html#apply-the-plugin) 확인

2. **`build.gradle.kts` 생성:**

```kotlin
plugins {
    kotlin("multiplatform") version "2.3.10"
}

repositories {
    mavenCentral()
}

kotlin {
    js {
        browser()  // browser() 또는 nodejs() 사용
        binaries.executable()
    }
}
```

또는 **Groovy (`build.gradle`):**

```groovy
plugins {
    id 'org.jetbrains.kotlin.multiplatform' version '2.3.10'
}

repositories {
    mavenCentral()
}

kotlin {
    js {
        browser()
        binaries.executable()
    }
}
```

3. **빈 `settings.gradle.kts` 생성**

4. **디렉토리 구조 생성:**

```
src/jsMain/kotlin/
```

5. **`src/jsMain/kotlin/hello.kt` 생성:**

```kotlin
fun main() {
    println("Hello, Kotlin/JS!")
}
```

### 브라우저 환경 전용

1. **`src/jsMain/resources` 디렉토리 생성**

2. **`src/jsMain/resources/index.html` 생성:**

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Application title</title>
</head>
<body>
    <script src="$NAME_OF_YOUR_PROJECT_DIRECTORY.js"></script>
</body>
</html>
```

`$NAME_OF_YOUR_PROJECT_DIRECTORY`를 실제 프로젝트 디렉토리 이름으로 교체하세요.

### 프로젝트 빌드 및 실행

```bash
# 브라우저용
gradle jsBrowserDevelopmentRun

# Node.js용
gradle jsNodeDevelopmentRun
```

**브라우저 출력:** `index.html`을 열고 브라우저 콘솔에 출력 (Ctrl+Shift+J / Cmd+Option+J)

**Node.js 출력:** 터미널에 출력

### IntelliJ IDEA에서 프로젝트 열기

1. **File | Open** 선택
2. 프로젝트 디렉토리 찾기
3. **Open** 클릭

IDEA가 Kotlin/JS 프로젝트를 자동으로 감지하고 Build 창에 오류를 표시합니다.

## 다음 단계

- [Kotlin/JS 프로젝트 설정](js-project-setup.html)
- [Kotlin/JS 애플리케이션 디버깅](js-debugging.html)
- [테스트 작성 및 실행](js-running-tests.html)
- [실제 프로젝트를 위한 Gradle 빌드 스크립트](/docs/multiplatform/multiplatform-dsl-reference.html)
- [Gradle 빌드 시스템에 대해 알아보기](gradle.html)

---

# Kotlin/JavaScript 개요

> **원문:** https://kotlinlang.org/docs/js-overview.html

## Kotlin/JS란?

**Kotlin/JavaScript**(Kotlin/JS)는 Kotlin 코드, Kotlin 표준 라이브러리 및 호환 가능한 의존성을 JavaScript로 트랜스파일할 수 있게 해줍니다. 이를 통해 JavaScript를 지원하는 모든 환경에서 Kotlin 애플리케이션을 실행할 수 있습니다.

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

커뮤니티 및 Kotlin/JS 팀과 연결하려면 공식 [Kotlin Slack](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up)의 **[#javascript 채널]**(https://kotlinlang.slack.com/archives/C0B8L3U69)에 참여하세요.

---

## 다음 단계

- [Kotlin/JS 프로젝트 설정](js-project-setup.html)
- [Kotlin/JS 프로젝트 실행](running-kotlin-js.html)
- [Kotlin/JS 코드 디버깅](js-debugging.html)
- [Kotlin/JS에서 테스트 실행](js-running-tests.html)

---

# Kotlin/JS 프로젝트 설정

> **원문:** https://kotlinlang.org/docs/js-project-setup.html

이 문서는 Gradle과 Kotlin Multiplatform 플러그인을 사용하여 Kotlin/JS 프로젝트를 구성하고 관리하는 방법을 설명합니다.

## 개요

Kotlin/JS 프로젝트는 `kotlin.multiplatform` 플러그인과 함께 Gradle을 빌드 시스템으로 사용하며, 다음 기능을 제공합니다:
- npm 또는 Yarn을 사용하여 npm 의존성 다운로드
- webpack을 사용하여 JavaScript 번들 빌드
- JavaScript 개발을 위한 구성 도구 및 헬퍼 태스크 제공

### 플러그인 적용

```kotlin
plugins { kotlin("multiplatform") version "2.3.10" }
```

```gradle
plugins { id 'org.jetbrains.kotlin.multiplatform' version '2.3.10' }
```

빌드 스크립트의 `kotlin {}` 블록에서 프로젝트를 구성합니다.

## 실행 환경

Kotlin/JS는 두 가지 환경을 타겟팅할 수 있습니다:

### 브라우저

```kotlin
kotlin {
    js {
        browser { }
        binaries.executable()
    }
}
```

### Node.js

```kotlin
kotlin {
    js {
        nodejs { }
        binaries.executable()
    }
}
```

`binaries.executable()` 명령은 실행 가능한 `.js` 파일을 생성합니다. 이를 생략하면 라이브러리 파일만 생성됩니다.

## ES2015 기능 지원

Kotlin은 다음에 대한 실험적 지원을 제공합니다:
- **모듈** - 유지보수성 향상
- **클래스** - 더 깔끔한 OOP 코드
- **제너레이터** - suspend 함수에 대해 더 나은 번들 크기와 디버깅

ES2015 기능 활성화:

```kotlin
tasks.withType<KotlinJsCompile>().configureEach {
    compilerOptions { target = "es2015" }
}
```

## 출력 세분화

`gradle.properties`에서 컴파일러가 `.js` 파일을 출력하는 방식을 구성합니다:

- **모듈별** (기본값): 각 모듈마다 별도의 `.js`
- **전체 프로그램**: 전체 프로젝트에 단일 `.js` 파일
  ```properties
  kotlin.js.ir.output.granularity=whole-program
  ```
- **파일별**: Kotlin 파일마다 하나의 `.js` (ES2015 타겟 필요)
  ```properties
  kotlin.js.ir.output.granularity=per-file
  ```

## TypeScript 선언 파일

자동 완성 및 정적 분석을 위해 Kotlin 코드에서 TypeScript 정의 (`.d.ts`)를 생성합니다:

```kotlin
kotlin {
    js {
        browser { }
        binaries.executable()
        generateTypeScriptDefinitions()
    }
}
```

포함할 선언은 `@JsExport`로 표시합니다. 파일은 `build/js/packages/<package_name>/kotlin/`에 생성됩니다.

## 의존성

### Kotlin 표준 라이브러리

표준 라이브러리에 대한 의존성은 플러그인과 동일한 버전으로 자동 추가됩니다.

멀티플랫폼 테스트의 경우 `kotlin.test`를 사용합니다:

```kotlin
kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
```

### NPM 의존성

`npm()` 함수를 사용하여 npm 의존성을 선언합니다:

```kotlin
dependencies {
    implementation(npm("react", "> 14.0.0 <=16.9.0"))
}
```

의존성 유형:
- `npm(...)` - 일반 의존성
- `devNpm(...)` - 개발 의존성
- `optionalNpm(...)` - 선택적 의존성
- `peerNpm(...)` - 피어 의존성

기본적으로 Yarn이 의존성을 관리합니다. npm을 대신 사용하려면:

```properties
kotlin.js.yarn=false
```

## 실행 및 테스트

### 실행 태스크

브라우저 프로젝트의 경우:

```bash
./gradlew jsBrowserDevelopmentRun
```

연속 빌드 사용:

```bash
./gradlew jsBrowserDevelopmentRun --continuous
# 또는
./gradlew jsBrowserDevelopmentRun -t
```

Node.js 프로젝트의 경우:

```bash
./gradlew jsNodeDevelopmentRun
```

### 테스트 태스크

테스트는 자동으로 구성됩니다:
- **브라우저**: 기본적으로 Headless Chrome과 함께 Karma 테스트 러너
- **Node.js**: Mocha 테스트 프레임워크

테스트 브라우저 구성:

```kotlin
kotlin {
    js {
        browser {
            testTask {
                useKarma {
                    useChrome()
                    useFirefox()
                    useSafari()
                }
            }
        }
    }
}
```

또는 `gradle.properties`에서:

```properties
kotlin.js.browser.karma.browsers=firefox,safari
```

테스트 실행:

```bash
./gradlew check
```

테스트 비활성화:

```kotlin
kotlin {
    js {
        browser {
            testTask { enabled = false }
        }
    }
}
```

### Karma 구성

사용자 정의 구성 파일을 `karma.config.d/` 디렉토리에 배치합니다. 모든 `.js` 파일은 생성된 `karma.conf.js`에 자동으로 병합됩니다.

## Webpack 번들링

### 버전

Kotlin Multiplatform 플러그인은 기본적으로 **webpack 5**를 사용합니다. 이전 프로젝트의 경우 webpack 4로 되돌립니다:

```properties
kotlin.js.webpack.major.version=4
```

### Webpack 태스크 구성

```kotlin
webpackTask {
    outputFileName = "mycustomfilename.js"
    output.libraryTarget = "commonjs2"
}
```

번들링, 실행, 테스트를 위한 공통 설정:

```kotlin
browser {
    commonWebpackConfig {
        // 여기에 구성
    }
}
```

### Webpack 구성 파일

사용자 정의 webpack 구성은 `webpack.config.d/` 디렉토리에 저장합니다. 모든 `.js` 파일은 `build/js/packages/projectName/webpack.config.js`에 자동으로 병합됩니다.

예시 - webpack 로더 추가:

```javascript
config.module.rules.push({
    test: /\.extension$/,
    loader: 'loader-name'
});
```

### 실행 파일 빌드

```bash
# 개발 (더 빠르고, 더 큼)
./gradlew browserDevelopmentWebpack

# 프로덕션 (더 느리고, 더 작고, 최소화됨)
./gradlew browserProductionWebpack
```

출력은 `build/dist` (또는 사용자 정의 [배포 대상 디렉토리](#배포-대상-디렉토리))에 있습니다.

## CSS 지원

CSS 지원 활성화:

```kotlin
browser {
    commonWebpackConfig {
        cssSupport { enabled.set(true) }
    }
}
```

또는 태스크별로:

```kotlin
browser {
    webpackTask { cssSupport { enabled.set(true) } }
    runTask { cssSupport { enabled.set(true) } }
    testTask {
        useKarma {
            webpackConfig.cssSupport { enabled.set(true) }
        }
    }
}
```

`cssSupport.mode`를 통한 CSS 처리 모드:
- **`"inline"`** (기본값) - 전역 `<style>` 태그에 추가
- **`"extract"`** - 별도 파일로 추출
- **`"import"`** - 코드 접근을 위해 문자열로 처리

## Node.js 구성

### Node.js 버전 구성

특정 하위 프로젝트용:

```kotlin
project.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin> {
    project.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().version = "18.0.0"
}
```

전체 프로젝트용:

```kotlin
allprojects {
    project.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin> {
        project.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().version = "18.0.0"
    }
}
```

### 사전 설치된 Node.js 사용

```kotlin
project.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin> {
    project.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().download = false
}
```

## Yarn 구성

플러그인은 기본적으로 자체 Yarn 인스턴스를 관리하지만 사전 설치된 Yarn을 사용할 수 있습니다.

### 사용자 정의 Yarn 설정 (.yarnrc)

프로젝트 루트에 `.yarnrc` 생성:

```
registry "http://my.registry/api/npm/"
```

### 사전 설치된 Yarn 사용

```kotlin
rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().download = false
}
```

### 버전 잠금 (kotlin-js-store)

`kotlin-js-store/` 디렉토리에는 버전 잠금을 위한 `yarn.lock`이 포함됩니다. 이를 버전 관리에 커밋하세요.

디렉토리 및 잠금 파일 사용자 정의:

```kotlin
rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().lockFileDirectory =
        project.rootDir.resolve("my-kotlin-js-store")
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().lockFileName = "my-yarn.lock"
}
```

### yarn.lock 업데이트 보고

`yarn.lock` 변경 사항 보고 방식 제어:

```kotlin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    rootProject.the<YarnRootExtension>().yarnLockMismatchReport =
        YarnLockMismatchReport.WARNING  // NONE | FAIL
    rootProject.the<YarnRootExtension>().reportNewYarnLock = false  // true
    rootProject.the<YarnRootExtension>().yarnLockAutoReplace = false  // true
}
```

### NPM 라이프사이클 스크립트 보안

기본적으로 npm 라이프사이클 스크립트는 보안을 위해 비활성화됩니다. 필요한 경우 활성화:

```kotlin
rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().ignoreScripts = false
}
```

## 배포 대상 디렉토리

기본값: `/build/dist/<targetName>/<binaryName>`

출력 위치 변경:

```kotlin
kotlin {
    js {
        browser {
            distribution { outputDirectory.set(projectDir.resolve("output")) }
            binaries.executable()
        }
    }
}
```

## 모듈 이름

JavaScript 모듈 이름 구성 (`build/js/packages/myModuleName`에 영향):

```kotlin
js { outputModuleName = "myModuleName" }
```

이는 `build/dist`의 webpack 출력에는 영향을 주지 않습니다.

## package.json 사용자 정의

Kotlin Multiplatform 플러그인은 자동으로 `package.json`을 생성합니다. `customField()` 함수를 사용하여 사용자 정의 필드를 추가합니다:

```kotlin
kotlin {
    js {
        compilations["main"].packageJson {
            customField("hello", mapOf("one" to 1, "two" to 2))
        }
    }
}
```

다음을 생성합니다:

```json
{
    "hello": { "one": 1, "two": 2 }
}
```

---

# Kotlin/JS 실행하기

> **원문:** https://kotlinlang.org/docs/running-kotlin-js.html

이 문서는 Kotlin Multiplatform Gradle 플러그인을 사용하여 Kotlin/JS 프로젝트를 실행하는 방법을 다룹니다.

## 초기 설정

`src/jsMain/kotlin/App.kt`에 샘플 파일을 생성합니다:

```kotlin
fun main() {
    console.log("Hello, Kotlin/JS!")
}
```

## Node.js 타겟 실행

Node.js를 타겟팅하는 Kotlin/JS 프로젝트를 실행하려면 `jsNodeDevelopmentRun` Gradle 태스크를 실행합니다:

```bash
./gradlew jsNodeDevelopmentRun
```

**IntelliJ IDEA에서:** Gradle 도구 창에서 `jsNodeDevelopmentRun` 액션을 찾습니다.

**프로세스:**
1. `kotlin.multiplatform` 플러그인이 첫 실행 시 필요한 의존성을 다운로드합니다
2. 빌드가 완료되고 프로그램이 실행됩니다
3. 로깅 출력이 터미널에 나타납니다

## 브라우저 타겟 실행

### HTML 설정

`/src/jsMain/resources/index.html`을 생성합니다:

```html
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8">
    <title>JS Client</title>
  </head>
  <body>
    <script src="js-tutorial.js"></script>
  </body>
</html>
```

**참고:** 스크립트 파일명은 기본적으로 프로젝트 이름입니다. 그에 맞게 조정하세요 (예: 프로젝트 이름이 `followAlong`이면 `followAlong.js`).

### 개발 서버 실행

```bash
./gradlew jsBrowserDevelopmentRun
```

**IntelliJ IDEA에서:** Gradle 도구 창에서 `jsBrowserDevelopmentRun` 액션을 찾습니다.

**진행 과정:**
1. 프로젝트가 빌드됩니다
2. Webpack 개발 서버가 시작됩니다
3. 브라우저 창이 HTML 파일로 열립니다
4. 브라우저 개발자 도구에서 콘솔 출력을 확인합니다 (우클릭 → 검사 → Console 탭)

### 연속 개발

문서에서는 개발 중 연속 컴파일/핫 리로드 설정을 위한 [별도 튜토리얼](dev-server-continuous-compilation.html)을 참조합니다.

## 요약

| 타겟 | Gradle 태스크 | 출력 위치 |
|------|--------------|----------|
| Node.js | `jsNodeDevelopmentRun` | 터미널 |
| 브라우저 | `jsBrowserDevelopmentRun` | 브라우저 콘솔 |

## 다음 단계

- [Kotlin/JS 프로젝트 설정](js-project-setup.html) - 프로젝트 구성 옵션 자세히 알아보기
- [Kotlin/JS 디버깅](js-debugging.html) - 애플리케이션 디버깅 방법
- [Kotlin/JS 테스트 실행](js-running-tests.html) - 테스트 작성 및 실행

---

# Kotlin의 Dynamic 타입

> **원문:** https://kotlinlang.org/docs/dynamic-type.html

## 개요

**dynamic 타입**은 정적 타입 검사를 비활성화하여 타입이 없거나 느슨하게 타입이 지정된 환경, 특히 JavaScript와의 상호운용성을 용이하게 하는 Kotlin 기능입니다. **참고:** dynamic 타입은 JVM을 타겟팅하는 코드에서는 지원되지 않습니다.

## 선언

```kotlin
val dyn: dynamic = ...
```

## 타입 검사 동작

`dynamic` 타입은 본질적으로 Kotlin의 타입 검사기를 끕니다:

- `dynamic` 값은 모든 변수에 할당하거나 어디서든 매개변수로 전달할 수 있습니다
- 모든 값을 `dynamic` 변수에 할당하거나 `dynamic`을 받는 함수에 전달할 수 있습니다
- `dynamic` 값의 null 검사가 비활성화됩니다

## Dynamic 함수 및 프로퍼티 호출

`dynamic` 변수에서 모든 매개변수로 모든 프로퍼티나 함수를 호출할 수 있습니다:

```kotlin
dyn.whatever(1, "foo", dyn)           // 'whatever'는 어디에도 정의되어 있지 않음
dyn.whatever(*arrayOf(1, 2, 3))       // 스프레드 연산자와 함께 작동
```

이 코드는 그대로 JavaScript로 컴파일됩니다.

## 중요 고려사항

`dynamic` 값에서 Kotlin 함수를 호출할 때 Kotlin-to-JavaScript 컴파일러의 **이름 맹글링**에 주의하세요. 잘 정의된 이름을 할당하려면 `@JsName` 어노테이션을 사용합니다:

```kotlin
@JsName("myFunction")
fun myFunction() { ... }
```

## Dynamic 호출 체이닝

dynamic 호출은 `dynamic`을 반환하여 자유로운 체이닝을 허용합니다:

```kotlin
dyn.foo().bar.baz()
```

## 람다 매개변수

dynamic 호출의 람다 매개변수는 기본적으로 `dynamic` 타입입니다:

```kotlin
dyn.foo { x -> x.bar() }  // x는 dynamic
```

## 지원되는 연산자

- **이항:** `+`, `-`, `*`, `/`, `%`, `>`, `<`, `>=`, `<=`, `==`, `!=`, `===`, `!==`, `&&`, `||`
- **단항 전위:** `-`, `+`, `!`
- **전위/후위:** `++`, `--`
- **할당:** `+=`, `-=`, `*=`, `/=`, `%=`
- **인덱스 접근:** `d[a]` (읽기), `d[a1] = a2` (쓰기)

## 금지된 연산

`dynamic` 타입과 함께 `in`, `!in`, `..` 연산은 **허용되지 않습니다**.

## 사용 예시

### JavaScript 라이브러리와 상호작용

```kotlin
// JavaScript 라이브러리의 객체를 dynamic으로 처리
val jsObject: dynamic = js("{}")
jsObject.name = "Kotlin"
jsObject.version = 2.0
console.log(jsObject.name)  // "Kotlin" 출력
```

### JSON 데이터 처리

```kotlin
val jsonData: dynamic = JSON.parse("""{"key": "value", "count": 42}""")
println(jsonData.key)    // "value"
println(jsonData.count)  // 42
```

### DOM 조작

```kotlin
val element: dynamic = document.getElementById("myElement")
element.style.color = "red"
element.innerHTML = "Hello, World!"
element.addEventListener("click") {
    console.log("Element clicked!")
}
```

## 주의사항

- dynamic 타입은 컴파일 타임 타입 안전성을 포기합니다
- 런타임 오류가 발생할 수 있으므로 주의해서 사용해야 합니다
- 가능하면 타입이 지정된 Kotlin 래퍼나 외부 선언을 사용하는 것이 좋습니다

## 다음 단계

- [Kotlin/JS 개요](js-overview.html)
- [JavaScript 모듈과 상호작용](js-modules.html)
- [JavaScript에서 Kotlin 호출](js-to-kotlin-interop.html)

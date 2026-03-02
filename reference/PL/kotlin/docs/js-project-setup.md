# Kotlin/JS 프로젝트 설정

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

- **모듈별** (기본값): 각 모듈에 대해 별도의 `.js`
- **전체 프로그램**: 전체 프로젝트에 대해 단일 `.js` 파일
  ```properties
  kotlin.js.ir.output.granularity=whole-program
  ```
- **파일별**: 각 Kotlin 파일에 대해 하나의 `.js` (ES2015 타겟 필요)
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

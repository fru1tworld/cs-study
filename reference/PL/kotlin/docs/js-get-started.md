# Kotlin/JS 시작하기 - 완전 가이드

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

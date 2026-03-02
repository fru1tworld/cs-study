# Kotlin/JS 실행하기

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

# 콘솔 앱 만들기 - Kotlin 튜토리얼

이 튜토리얼은 IntelliJ IDEA와 Kotlin을 사용하여 콘솔 애플리케이션을 만드는 방법을 안내합니다.

## 사전 준비

- 최신 버전의 [IntelliJ IDEA](https://www.jetbrains.com/idea/download/index.html)를 다운로드하고 설치하세요

## 프로젝트 생성

1. **새 프로젝트 대화상자 열기**: File | New | Project
2. 왼쪽 목록에서 **Kotlin** 선택
3. **프로젝트 설정 구성**:
   - 프로젝트 이름 지정 및 위치 설정
   - 버전 관리를 위해 선택적으로 "Create Git repository" 선택
4. **빌드 시스템 선택**:
   - **IntelliJ** (네이티브, 추가 다운로드 불필요)
   - **Maven 또는 Gradle** (복잡한 프로젝트용)
   - Gradle의 경우: 빌드 스크립트 언어로 Kotlin 또는 Groovy 선택
5. **JDK 선택**:
   - 설치된 JDK 목록에서 선택
   - 또는 "Add JDK"를 선택하여 경로 지정
   - 또는 설치되어 있지 않은 경우 "Download JDK" 선택
6. **샘플 코드 활성화** (선택 사항):
   - "Add sample code"는 "Hello World!" 예제를 생성
   - "Generate code with onboarding tips"는 유용한 주석을 추가
7. **Create 클릭**

### Gradle 구성 (선택한 경우)

```kotlin
plugins {
    kotlin("jvm") version "2.3.10"
    application
}
```

## 애플리케이션 생성

`src/main/kotlin`에서 `Main.kt`를 열고 사용자 입력을 요청하도록 수정합니다:

```kotlin
fun main() {
    println("What's your name?")
    val name = readln()
    println("Hello, $name!")
}
```

**핵심 개념**:
- `readln()` 함수는 사용자 입력을 읽습니다
- 문자열 템플릿은 보간을 위해 `$variableName` 구문을 사용합니다

## 애플리케이션 실행

1. 거터에서 녹색 **Run** 아이콘을 클릭합니다
2. **Run 'MainKt'**를 선택합니다
3. Run 도구 창에서 출력을 확인합니다
4. 프롬프트가 표시되면 이름을 입력합니다

## 다음 단계

Kotlin 학습을 계속하세요:
- [Kotlin 투어](kotlin-tour-welcome.html) 참여하기
- [JetBrains Academy 플러그인](https://plugins.jetbrains.com/plugin/10081-jetbrains-academy)을 설치하고 [Kotlin Koans 과정](https://plugins.jetbrains.com/plugin/10081-jetbrains-academy/docs/learner-start-guide.html?section=Kotlin%20Koans) 완료하기

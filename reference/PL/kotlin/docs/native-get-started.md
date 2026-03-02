# Kotlin/Native 시작하기 - 완전 가이드

## 개요

이 튜토리얼은 Kotlin/Native 애플리케이션을 만드는 세 가지 방법을 다룹니다:
1. **IntelliJ IDEA** (IDE 접근 방식)
2. **Gradle** (빌드 시스템)
3. **명령줄 컴파일러** (직접 컴파일)

Kotlin/Native는 다양한 타겟(Linux, macOS, Windows)으로 컴파일합니다. 크로스 플랫폼 컴파일이 가능하지만, 이 튜토리얼은 동일 플랫폼 컴파일에 중점을 둡니다.

**Mac 사용자**: 먼저 Xcode 명령줄 도구를 설치하세요.

---

## 방법 1: IntelliJ IDEA 사용

### 프로젝트 생성

1. 최신 [IntelliJ IDEA](https://www.jetbrains.com/idea/) 다운로드 (Community 또는 Ultimate Edition)
2. 프로젝트 템플릿 복제:
   - **File | New | Project from Version Control** 선택
   - URL: `https://github.com/Kotlin/kmp-native-wizard`
3. `gradle/libs.versions.toml`을 열고 최신 Kotlin 버전 확인:
   ```
   [versions]
   kotlin = "2.3.10"
   ```
4. Gradle 파일 다시 로드 제안을 따름

### 빌드 및 실행

1. `src/nativeMain/kotlin/Main.kt` 열기
2. 거터에서 녹색 실행 아이콘 누르기
3. IntelliJ IDEA가 Gradle을 통해 코드를 실행하고 Run 탭에 출력 표시

### 애플리케이션 업데이트

**기본 이름 입력:**

```kotlin
fun main() {
    println("Hello, enter your name:")
    val name = readln()
}
```

**이름의 글자 수 세기:**

```kotlin
fun main() {
    println("Hello, enter your name:")
    val name = readln()

    name.replace(" ", "").let {
        println("Your name contains ${it.length} letters")
    }
}
```

Gradle 입력 지원을 위해 `build.gradle.kts` 업데이트:

```kotlin
kotlin {
    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "main"
                runTaskProvider?.configure {
                    standardInput = System.`in`
                }
            }
        }
    }
}
```

**고유 글자 수 세기:**

```kotlin
fun String.countDistinctCharacters() = lowercase().toList().distinct().count()

fun main() {
    println("Hello, enter your name:")
    val name = readln()

    name.replace(" ", "").let {
        println("Your name contains ${it.length} letters")
        println("Your name contains ${it.countDistinctCharacters()} unique letters")
    }
}
```

---

## 방법 2: Gradle 사용

### 프로젝트 파일 생성

1. 호환되는 [Gradle 버전](https://gradle.org/install/) 설치

2. `build.gradle.kts` 생성:

```kotlin
plugins {
    kotlin("multiplatform") version "2.3.10"
}

repositories {
    mavenCentral()
}

kotlin {
    macosArm64("native") {      // macOS에서
    // linuxArm64("native")      // Linux에서
    // mingwX64("native")        // Windows에서
        binaries {
            executable()
        }
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "9.0.0"
    distributionType = Wrapper.DistributionType.BIN
}
```

3. 빈 `settings.gradle(.kts)` 파일 생성

4. `src/nativeMain/kotlin/hello.kt` 생성:

```kotlin
fun main() {
    println("Hello, Kotlin/Native!")
}
```

### 빌드 및 실행

```bash
# 프로젝트 빌드
./gradlew nativeBinaries

# 실행 파일 실행
build/bin/native/debugExecutable/<project_name>.kexe
```

이렇게 하면 `build/bin/native/debugExecutable` 및 `releaseExecutable` 디렉토리가 생성됩니다.

### IDE에서 열기

- **File | Open** 선택
- 프로젝트 디렉토리 선택
- IntelliJ IDEA가 Kotlin/Native 프로젝트를 자동으로 감지

---

## 방법 3: 명령줄 컴파일러 사용

### 다운로드 및 설치

1. [Kotlin GitHub 릴리스](https://github.com/JetBrains/kotlin/releases/tag/v2.3.10)로 이동
2. `kotlin-native-prebuilt-<os>-<arch>-2.3.10.tar.gz` 다운로드
3. 아카이브 압축 해제
4. PATH에 추가:

```bash
export PATH="/<path to compiler>/kotlin-native/bin:$PATH"
```

**요구 사항**: Java 1.8 이상

### 프로그램 생성

`hello.kt` 생성:

```kotlin
fun main() {
    println("Hello, Kotlin/Native!")
}
```

### 컴파일 및 실행

```bash
# 컴파일
kotlinc-native hello.kt -o hello

# 실행
./hello.kexe    # macOS/Linux
./hello.exe     # Windows
```

`-o` 옵션은 출력 파일명을 지정합니다. 컴파일러는 `.kexe` (macOS/Linux) 또는 `.exe` (Windows)를 생성합니다.

---

## 타겟 플랫폼

| 플랫폼 | 타겟 |
|--------|------|
| macOS (ARM) | `macosArm64` |
| macOS (x64) | `macosX64` |
| Linux (ARM) | `linuxArm64` |
| Linux (x64) | `linuxX64` |
| Windows | `mingwX64` |
| iOS (ARM) | `iosArm64` |
| iOS 시뮬레이터 | `iosSimulatorArm64` |

---

## 다음 단계

- [C 상호운용 및 libcurl을 사용한 앱 만들기](native-app-with-c-and-libcurl.html) 튜토리얼 완료
- 고급 [Gradle 빌드 스크립트](/docs/multiplatform/multiplatform-dsl-reference.html) 학습
- [Gradle 문서](gradle.html) 탐색

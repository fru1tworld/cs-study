# Kotlin/Native 개요

## Kotlin/Native란?

**Kotlin/Native**는 Kotlin 코드를 가상 머신 없이 실행되는 네이티브 바이너리로 컴파일하는 기술입니다. 다음을 특징으로 합니다:
- Kotlin 컴파일러를 위한 LLVM 기반 백엔드
- Kotlin 표준 라이브러리의 네이티브 구현

## Kotlin/Native를 사용하는 이유?

Kotlin/Native는 다음을 위해 설계되었습니다:
- **VM이 없는 플랫폼**: 임베디드 장치, iOS 및 기타 제한된 환경
- **자체 포함 프로그램**: 추가 런타임이나 VM 불필요
- **쉬운 통합**: C, C++, Swift, Objective-C 및 기타 언어의 기존 프로젝트와 원활하게 작동

## 지원되는 타겟 플랫폼

- **Linux**
- **Windows** (MinGW를 통해)
- **Android NDK**
- **Apple 플랫폼**: macOS, iOS, tvOS, watchOS
  - *Xcode 및 명령줄 도구 필요*

## 상호운용성 기능

### C와의 상호운용성

- Kotlin 코드에서 직접 기존 C 라이브러리 사용
- 제공되는 튜토리얼:
  - C 헤더가 있는 동적 라이브러리 생성
  - C 타입을 Kotlin으로 매핑
  - libcurl로 네이티브 HTTP 클라이언트 빌드

### Swift/Objective-C와의 상호운용성

- Objective-C를 통한 양방향 상호운용성
- macOS 및 iOS의 Swift/Objective-C 애플리케이션에서 직접 Kotlin 코드 사용
- Kotlin에서 Apple 프레임워크 생성

## 코드 공유

- **플랫폼 라이브러리**: POSIX, gzip, OpenGL, Metal, Foundation 및 Apple 프레임워크용 사전 빌드된 라이브러리
- **Kotlin Multiplatform**: Android, iOS, JVM, 웹 및 네이티브 플랫폼 간 코드 공유

## 메모리 관리

- JVM 및 Go와 유사한 자동 메모리 관리자
- 내장 추적 가비지 컬렉터
- Swift/Objective-C의 ARC (Automatic Reference Counting)와 통합
- 사용량을 최적화하고 할당 급증을 방지하는 커스텀 메모리 할당자

## 주요 장점

1. **네이티브 성능**: JIT 컴파일 오버헤드 없이 네이티브 속도로 실행
2. **작은 바이너리**: VM 없이 자체 포함된 실행 파일
3. **직접 메모리 접근**: 시스템 수준 프로그래밍 가능
4. **플랫폼 간 공유**: 단일 코드베이스로 여러 플랫폼 지원

## 사용 사례

- **iOS 앱 개발**: Kotlin Multiplatform으로 Android와 iOS 간 코드 공유
- **임베디드 시스템**: 리소스가 제한된 환경에서 실행
- **명령줄 도구**: 빠른 시작 시간과 작은 바이너리
- **게임 개발**: 네이티브 성능이 필요한 경우
- **시스템 프로그래밍**: C 라이브러리와의 통합

## 시작하기

- [Kotlin/Native 시작하기](native-get-started.html)
- [C 상호운용 및 libcurl을 사용한 앱 만들기](native-app-with-c-and-libcurl.html)
- [Gradle 빌드 스크립트 참조](/docs/multiplatform/multiplatform-dsl-reference.html)

## 다음 단계

- C 라이브러리 사용 방법 알아보기
- Swift/Objective-C와의 상호운용성 탐색
- 메모리 관리 이해하기

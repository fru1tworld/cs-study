# Kotlin Multiplatform

## Kotlin Multiplatform이란?

**Kotlin Multiplatform (KMP)**은 JetBrains의 오픈소스 기술로, **Android, iOS, 데스크톱, 웹, 서버** 간에 코드를 공유하면서도 네이티브 개발의 장점을 유지할 수 있게 해줍니다.

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

대규모 애플리케이션은 일반적으로 광범위한 코드베이스를 가지고 있으며, 새로운 기능이 지속적으로 추가되고, 모든 플랫폼에서 동일하게 작동해야 하는 복잡한 비즈니스 로직이 있습니다. Kotlin Multiplatform은 점진적 통합을 제공하여 팀이 단계적으로 도입할 수 있게 합니다.

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

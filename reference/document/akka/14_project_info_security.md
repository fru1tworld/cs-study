# Akka 프로젝트 정보와 보안

> 원본: https://doc.akka.io/libraries/akka-core/current/project/index.html

---

## 목차

1. [바이너리 호환성 규칙 (Binary Compatibility Rules)](#1-바이너리-호환성-규칙-binary-compatibility-rules)
2. ["변경될 수 있음"으로 표시된 모듈 (Modules marked "May Change")](#2-변경될-수-있음으로-표시된-모듈-modules-marked-may-change)
3. [자주 묻는 질문 (FAQ)](#3-자주-묻는-질문-faq)
4. [보안 공지 (Security Announcements)](#4-보안-공지-security-announcements)
5. [마이그레이션 가이드 (Migration Guides)](#5-마이그레이션-가이드-migration-guides)
6. [라이선스 (Licenses)](#6-라이선스-licenses)
7. [참고 자료](#7-참고-자료)

---

## 1. 바이너리 호환성 규칙 (Binary Compatibility Rules)

Akka는 여러 버전에 걸쳐 **하위 바이너리 호환성(backwards binary compatibility)**을 유지합니다. 이는 새로운 JAR가 이전 JAR를 그대로 대체할 수 있는 **드롭인 교체(drop-in replacement)**가 됨을 의미합니다(단, Scala 인라이너(inliner)를 사용하는 경우는 제외합니다).

애플리케이션을 다시 컴파일하지 않고도 의존하는 Akka 라이브러리의 JAR를 교체하는 것만으로 업그레이드를 수행할 수 있다는 것이 핵심 원칙입니다.

### 1.1 호환성 프레임워크

**바이너리 호환성이 유지되는 경우:**

- 마이너 버전(minor version)과 패치 버전(patch version) 사이에서 유지됩니다. 단, 이러한 호환성 정의는 2.4.0부터 새롭게 정비되었습니다(revised definitions since 2.4.0).

**바이너리 호환성이 유지되지 않는(보장되지 않는) 경우:**

- 메이저 버전(major version) 사이
- "변경될 수 있음(may change)"으로 표시된 모듈
- 아래에 명시된 특정 예외 항목들

### 1.2 버전 체계의 변화 (Version Scheme Evolution)

Akka는 버전 명명 체계를 다음과 같이 전환했습니다.

- **2.3.x까지:** `epoch.major.minor` 체계
- **2.4.0 이후:** `major.minor.patch` 체계

이러한 변경을 통해 더 강력한 호환성 보장(stronger compatibility guarantees)이 가능해졌습니다. 그 결과 2.4.x 계열은 2.3.x 릴리스에 대해 하위 호환성(backwards compatibility)을 달성했습니다.

### 1.3 실제 예시 (Practical Examples)

공식 문서는 유효한 업그레이드 경로를 다음과 같은 예시로 설명합니다.

- `2.2.0 → 2.2.1 → ... → 2.2.x` (패치 업그레이드 허용)
- `2.3.x → 2.4.x` (특별한 마이그레이션 사례)
- `2.4.0 → 2.5.x` (2.4 이후의 마이너 업그레이드 허용)

### 1.4 주요 제약 사항 (Key Restrictions)

**혼합 버전 사용 금지(Mixed versioning prohibited):** 함께 릴리스된 Akka 모듈들은 반드시 함께 업그레이드되어야 합니다. 예를 들어, Actor 2.6.2와 Cluster 2.5.3을 함께 사용하는 것은 각 모듈이 개별적으로는 바이너리 호환성을 주장한다 하더라도 이 규칙을 위반합니다.

따라서 한 프로젝트 내에서 사용하는 모든 Akka 모듈은 동일한 버전 번호를 유지해야 합니다.

### 1.5 주목할 만한 예외 (Notable Exceptions)

보안 취약점(security vulnerabilities)으로 인해 마이너 릴리스에서 호환성을 깨는 변경(breaking changes)이 불가피하게 발생할 수 있습니다. 또한 다음 모듈들은 바이너리 호환성 보장에서 제외됩니다.

- `-testkit` 모듈
- `-tck` 모듈
- "변경될 수 있음(may change)"으로 표시된 모든 모듈

### 1.6 폐기 정책 (Deprecation Policy)

폐기(deprecated)된 메서드는 최소한 하나의 완전한 마이너 버전 주기(at least one complete minor version cycle) 동안 유지됩니다. 다만 드물게 예외가 존재할 수 있습니다.

### 1.7 API 안정성 표시자 (API Stability Markers)

세 가지 핵심 애너테이션(annotation)이 API의 안정성 상태를 식별합니다.

1. **`@InternalApi`** — 호환성을 전혀 보장하지 않으며, 사전 통지 없이 변경될 수 있는 내부 API입니다.
2. **`@ApiMayChange`** — 불안정한(unstable) API입니다. 자세한 내용은 ["변경될 수 있음"으로 표시된 모듈](#2-변경될-수-있음으로-표시된-모듈-modules-marked-may-change) 섹션을 참고하십시오.
3. **`@DoNotInherit`** — 폐쇄형 세계 가정(closed-world assumption)을 따르며, 사용자가 상속(extends)할 경우 호환성이 깨질 위험이 있는 타입을 표시합니다.

### 1.8 검증 절차 (Verification Process)

Akka는 모든 풀 리퀘스트(pull request)에 대해 바이너리 호환성을 자동으로 검증하기 위해 **MiMa**(Migration Manager for Scala, Lightbend가 유지 관리)를 사용합니다.

### 1.9 Scala 직렬화 관련 주의 사항

Java 직렬화(Java serialization)는 서로 다른 Scala 메이저 버전(Scala major versions) 사이에서 호환성을 보장하지 않습니다.

---

## 2. "변경될 수 있음"으로 표시된 모듈 (Modules marked "May Change")

### 2.1 개요 (Overview)

Akka는 새로운 모듈과 API를 즉시 바이너리 호환성 보장 아래로 동결(freeze)하지 않으면서도 조기에 릴리스할 수 있도록 **"변경될 수 있음(may change)"** 지정 방식을 도입했습니다.

### 2.2 정의와 범위 (Definition and Scope)

공식 문서에 따르면, **"변경될 수 있음(may change)"**은 어떤 API나 모듈이 **얼리 액세스(early access)** 모드에 있음을 의미하며, 다음과 같은 특성을 가집니다.

- Lightbend가 명시적으로 달리 언급하지 않는 한, **상업적으로 지원되지 않음(not commercially supported)**
- 마이너 릴리스 간에 **바이너리 호환성이 없음(not binary compatible)**
- 마이너 릴리스에서 사전 경고 없이 **API가 깨질 수 있음(API may break)**
- 마이너 릴리스에서 **완전히 중단(discontinued)될 수 있음**

### 2.3 API 수준의 지정 (API-Level Designation)

개별 공개(public) API에는 [`ApiMayChange`](https://doc.akka.io/japi/akka-core/2.10/akka/annotation/ApiMayChange.html) 애너테이션을 부여하여, 해당 API가 안정적인 모듈 API보다 적은 보장(fewer guarantees)을 가짐을 알릴 수 있습니다.

이 방식을 통해 안정적으로 자리잡은 기존 모듈에 실험적인 Java 8 API를 도입하면서도, 해당 API가 아직 동결되지 않았음(unfrozen)을 신호로 전달할 수 있습니다.

### 2.4 근거 (Rationale)

이러한 지정 방식의 목적은 다음과 같이 명시되어 있습니다.

> "기능을 조기에 릴리스하여 쉽게 사용할 수 있도록 만들고, 피드백을 바탕으로 개선하거나, 심지어 해당 모듈이나 API가 유용하지 않았다는 사실을 발견하기 위함이다."
>
> ("release features early and make them easily available and improve based on feedback, or even discover that the module or API wasn't useful.")

### 2.5 현재 "변경될 수 있음"으로 표시된 모듈

현재 두 개의 완전한 모듈이 이 지정을 가지고 있습니다.

1. **다중 노드 테스트(Multi Node Testing)**
2. **신뢰성 있는 전달(Reliable Delivery)**

### 2.6 마이그레이션 지원 (Migration Support)

"변경될 수 있음" 모듈에 대해서도 마이그레이션 가이드(migration guidance)가 제공될 수 있으나, 이는 사례별(case-by-case basis)로 개별적으로 결정됩니다.

---

## 3. 자주 묻는 질문 (FAQ)

### 3.1 Akka 프로젝트 관련 (Akka Project Section)

**Q. Akka라는 이름은 어디에서 유래했나요? (Where does the name Akka come from?)**

이 이름은 "스웨덴 북부 라포니아(Laponia) 지역에 있는 아름다운 스웨덴 산"에서 유래했으며, 이 산은 '라포니아의 여왕(The Queen of Laponia)'으로도 알려져 있습니다. 사미(Sámi) 신화에서 Akka는 "세상의 모든 아름다움과 선함을 상징하는 여신"을 의미합니다.

또한 AKKA라는 약어는 회문(palindrome) 구조를 통해 "Actor Kernel(액터 커널)"을 반영하기도 합니다. 그 밖의 추가적인 의미로는 다음이 있습니다.

- 셀마 라겔뢰프(Selma Lagerlöf)의 작품 "닐스의 신기한 여행(The Wonderful Adventures of Nils)"에 등장하는 거위 캐릭터
- 핀란드어 및 인도 언어에서의 용어
- 한 서체(typeface) 디자인의 이름
- 모로코의 한 마을
- 지구 근접 소행성(near-earth asteroid)

**Q. 명시적인 생명주기(lifecycle)를 가진 리소스 (Resources with Explicit Lifecycle)**

액터(Actor), 액터 시스템(ActorSystem), 머티리얼라이저(Materializer)는 명시적으로 해제해야 하는 리소스를 점유합니다. 생성할 때마다 대응하는 `stop`, `terminate`, 또는 `shutdown` 호출이 필요합니다. 이는 액터가 독립적으로 존재하며 고유한 생명주기를 갖도록 설계되었기 때문입니다.

**Q. JVM 애플리케이션 또는 Scala REPL이 "멈춘(hanging)" 것처럼 보이는 이유 (JVM application or Scala REPL "hanging")**

시스템이 자동으로 종료되지 않는 이유는, JVM이 명시적으로 중단되기 전까지는 종료되지 않기 때문입니다. 정상적인 종료를 위해서는 실행 중인 애플리케이션 또는 Scala REPL 세션 내의 모든 ActorSystem을 명시적으로 종료(shutdown)해야 합니다.

### 3.2 액터 관련 (Actors Section)

**Q. 왜 `OutOfMemoryError`가 발생하나요? (Why OutOfMemoryError?)**

흔한 원인은 메시지 흐름 제어(message flow control)의 실패입니다. 순수 푸시(push) 기반 시스템에서 메시지 소비자(consumer)가 생산자(producer)보다 처리 속도가 느릴 경우, 반드시 어떤 형태의 메시지 흐름 제어(flow control)를 추가해야 합니다.

### 3.3 클러스터 관련 (Cluster Section)

**Q. 메시지 전달은 얼마나 신뢰할 수 있나요? (How reliable is the message delivery?)**

프레임워크는 **최대 한 번 전달(at-most-once delivery)**, 즉 보장된 전달이 아닌(no guaranteed delivery) 방식으로 동작합니다. 다만, 이 기반 위에 더 강력한 신뢰성을 구축(constructed above this foundation)할 수 있습니다.

### 3.4 디버깅 관련 (Debugging Section)

**Q. 디버그 로깅(debug logging)을 어떻게 켜나요? (How do I turn on debug logging?)**

다음 설정을 추가합니다.

```hocon
akka.loglevel = DEBUG
```

---

## 4. 보안 공지 (Security Announcements)

### 4.1 주요 정보 (Key Information)

- **현재 버전(Current Version):** 2.10.19
- **보안 공지 위치(Security Announcements Location):** 모든 Akka 프로젝트에 대한 보안 공지 내용은 [akka.io/security](https://akka.io/security)로 통합되었습니다.

### 4.2 보안 권고(Advisory) 수신 방법

사용자는 Google Groups를 통해 "Akka 보안 메일링 리스트(Akka security list)"를 구독해야 합니다. 이 메일링 리스트는 매우 적은 양의 트래픽으로 운영되며, 핵심 팀(core team)이 보안 보고를 처리하고 수정 사항을 공개적으로 제공한 이후에만 알림을 배포합니다.

### 4.3 취약점 보고 절차 (Vulnerability Reporting Process)

잠재적인 취약점은 **공개 공시(public disclosure) 이전에** 전용 비공개 보안 이메일 주소로 먼저 보고할 것을 강력히 권장합니다. 보고된 내용은 보안 팀이 검토하며, 신속한 수정을 위해 보고자와 협력합니다.

- **연락처(Contact):** security@akka.io

### 4.4 수정된 보안 이슈 (Fixed Security Issues)

문서에는 다음 세 가지 과거 취약점이 기록되어 있습니다.

1. **Java 직렬화(Java Serialization)** — Akka 2.4.17에서 해결됨 (2017년 2월)
2. **Camel 의존성(Camel Dependency)** — Akka 2.5.4에서 해결됨 (2017년 8월)
3. **난수 생성기(Random Number Generators)** — `AES128CounterSecureRNG` / `AES256CounterSecureRNG` 관련, Akka 2.5.16에서 해결됨 (2018년 8월)

### 4.5 관련 문서 (Related Documentation)

보안 관련 가이드는 다음 주제들을 다룹니다.

- Java 직렬화(Java serialization) 사용 시의 주의 사항 및 모범 사례
- 원격 배포(remote deployment) 보호 장치
- 원격 시스템 보안(remote system security) 프로토콜

---

## 5. 마이그레이션 가이드 (Migration Guides)

### 5.1 제공되는 마이그레이션 가이드 (Available Migration Guides)

다음 마이그레이션 경로(migration path)가 제공됩니다.

1. **마이그레이션 가이드 2.9.x → 2.10.x** (가장 최신)
2. **마이그레이션 가이드 2.8.x → 2.9.x**
3. **마이그레이션 가이드 2.7.x → 2.8.x**
4. **마이그레이션 가이드 2.6.x → 2.7.x**
5. **마이그레이션 가이드 2.5.x → 2.6.x**
6. **이전 마이그레이션 가이드(Older Migration Guides)** (그 이전 버전용 아카이브)

---

## 6. 라이선스 (Licenses)

### 6.1 주요 라이선스: Business Source License 1.1

Akka 2.10.19는 **Business Source License 1.1**(BSL 1.1) 하에서 라이선스가 부여되며, Lightbend, Inc.가 이를 관리합니다.

#### BSL 1.1 주요 조건:

- **라이선스 부여(License Grant):** 사용자는 라이선스 대상 저작물(Licensed Work)을 "복사(copy)하고, 수정(modify)하고, 2차 저작물(derivative works)을 생성하고, 재배포(redistribute)하며, 비프로덕션 용도로 사용(make non-production use)"할 권리를 부여받습니다. 프로덕션 용도의 사용은 추가 사용 허가(Additional Use Grant)를 통해 제한적으로 허용됩니다.

- **변경 날짜(Change Date):** 2029년 6월 4일

- **전환 라이선스(Conversion License):** Apache License 2.0 (변경 날짜(Change Date) 또는 최초 공개 배포 후 4주년 중 먼저 도래하는 시점에 자동으로 적용됩니다.)

- **Play Framework를 위한 추가 사용 허가(Additional Use Grant for Play Framework):** akka-streams를 포함하는 Play Framework 바이너리를 사용하는 개발자는, 해당 바이너리를 애플리케이션 개발에 사용할 수 있으나 그 범위는 "Play Framework 웹소켓(websocket)에 연결하거나 Play Framework의 요청/응답 본문(request/response bodies)에 연결하는 것"으로 제한됩니다.

- **주요 제한 사항(Key Restrictions):** 사용자는 승인 없이 소프트웨어를 역공학(reverse engineer)하거나, 수정(modify)하거나, 배포(distribute)하거나, 양도(transfer)할 수 없으며, 독점 표시(proprietary notices)를 제거할 수 없습니다.

### 6.2 문서 및 테스트 소스 라이선스 (Documentation and Test Sources License)

별도의 **Lightbend Commercial Software License Agreement**(Lightbend 상용 소프트웨어 라이선스 계약)가 문서와 테스트 소스 코드를 규율하며, 다음과 같은 특징을 가집니다.

- 제한적이고(limited), 비독점적이며(non-exclusive), 양도 불가능한(non-transferable) 바이너리 실행 권한
- 역공학(reverse engineering), 수정(modification), 재배포(redistribution)의 금지
- 오픈 소스 소프트웨어(Open Source Software) 구성 요소는 각자의 원래 라이선스를 따름
- 상품성(merchantability) 및 특정 목적 적합성(fitness for purpose)을 포함한 보증의 부인(Disclaimer of warranties)
- 미화 500달러(USD)의 책임 한도(liability cap)
- 캘리포니아 주법(California law)의 적용 및 중재(arbitration) 요구

### 6.3 커미터 라이선스 계약 (Committer License Agreement)

모든 Akka 커미터(committer)는 **Akka 커미터 라이선스 계약(Akka Committer License Agreement, CLA)**을 체결했으며, 이 계약은 Lightbend 웹사이트에서 온라인으로 서명할 수 있습니다.

### 6.4 의존성 라이선스 (Dependency Licenses)

개별 의존성(dependency)의 라이선스는 프로젝트 빌드 파일인 **`AkkaBuild.scala`**(버전 2.10.19)에 문서화되어 있으며, 각 의존성 선언 옆에 라이선스 정보가 주석(comment) 형태로 표시됩니다.

---

## 7. 참고 자료

- [Akka 공식 문서](https://doc.akka.io/libraries/akka-core/current/)
- [바이너리 호환성 규칙 (Binary Compatibility Rules)](https://doc.akka.io/libraries/akka-core/current/common/binary-compatibility-rules.html)
- ["변경될 수 있음"으로 표시된 모듈 (May Change)](https://doc.akka.io/libraries/akka-core/current/common/may-change.html)
- [자주 묻는 질문 (FAQ)](https://doc.akka.io/libraries/akka-core/current/additional/faq.html)
- [보안 공지 (Security Announcements)](https://doc.akka.io/libraries/akka-core/current/security/index.html)
- [마이그레이션 가이드 (Migration Guides)](https://doc.akka.io/libraries/akka-core/current/project/migration-guides.html)
- [라이선스 (Licenses)](https://doc.akka.io/libraries/akka-core/current/project/licenses.html)
- [Akka 보안 페이지](https://akka.io/security)

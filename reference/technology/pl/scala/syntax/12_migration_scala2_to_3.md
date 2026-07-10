# Scala 2에서 Scala 3로 마이그레이션

---

## 목차

1. [개요: Scala 3가 바꾸는 호환성의 패러다임](#1-개요-scala-3가-바꾸는-호환성의-패러다임)
2. [호환성의 네 가지 차원](#2-호환성의-네-가지-차원)
3. [소스 수준 호환성(Source Level Compatibility)](#3-소스-수준-호환성source-level-compatibility)
4. [클래스패스 수준 호환성(Classpath Level Compatibility)](#4-클래스패스-수준-호환성classpath-level-compatibility)
   - 4.1 [Scala 3 언피클러(Unpickler)](#41-scala-3-언피클러unpickler)
   - 4.2 [표준 라이브러리(Standard Library)](#42-표준-라이브러리standard-library)
   - 4.3 [Scala 2.13용 TASTy 리더(TASTy Reader)](#43-scala-213용-tasty-리더tasty-reader)
   - 4.4 [점진적 마이그레이션 전략과 샌드위치 패턴](#44-점진적-마이그레이션-전략과-샌드위치-패턴)
   - 4.5 [라이브러리 메인테이너를 위한 주의사항](#45-라이브러리-메인테이너를-위한-주의사항)
5. [런타임(Runtime) 고려사항](#5-런타임runtime-고려사항)
6. [메타프로그래밍/매크로(Macros) 변경](#6-메타프로그래밍매크로macros-변경)
7. [비호환성 표(Incompatibility Table)](#7-비호환성-표incompatibility-table)
   - 7.1 [구문 변경(Syntactic Changes)](#71-구문-변경syntactic-changes)
   - 7.2 [제거된 기능(Dropped Features)](#72-제거된-기능dropped-features)
   - 7.3 [문맥 추상화(Contextual Abstractions)](#73-문맥-추상화contextual-abstractions)
   - 7.4 [그 밖의 변경된 기능(Other Changed Features)](#74-그-밖의-변경된-기능other-changed-features)
   - 7.5 [타입 검사기(Type Checker)](#75-타입-검사기type-checker)
   - 7.6 [타입 추론(Type Inference)](#76-타입-추론type-inference)
   - 7.7 [매크로(Macros)](#77-매크로macros)
8. [Scala 3 마이그레이션 모드(Migration Mode)와 도구](#8-scala-3-마이그레이션-모드migration-mode와-도구)
   - 8.1 [`-source` 컴파일러 옵션](#81--source-컴파일러-옵션)
   - 8.2 [마이그레이션 모드 동작 방식](#82-마이그레이션-모드-동작-방식)
   - 8.3 [자동 재작성(`-rewrite`)과 2단계 워크플로](#83-자동-재작성-rewrite과-2단계-워크플로)
   - 8.4 [Scalafix를 통한 마이그레이션](#84-scalafix를-통한-마이그레이션)
   - 8.5 [오류 진단 옵션](#85-오류-진단-옵션)
9. [Scala 3의 주요 신기능 요약](#9-scala-3의-주요-신기능-요약)
10. [권장 마이그레이션 절차](#10-권장-마이그레이션-절차)

---

## 1. 개요: Scala 3가 바꾸는 호환성의 패러다임

공식 마이그레이션 가이드는 Scala 3를 "Scala 생태계에서 호환성(compatibility) 측면의 game changer"로 소개하며, 모든 Scala 프로그래머의 일상적인 개발 경험을 크게 향상시킬 것이라고 설명합니다.

Scala 3는 **언어의 핵심 기반(core foundations)을 완전히 재설계(complete redesign)** 한 결과물입니다. 그럼에도 실제 마이그레이션의 난이도는 **Scala 2 버전 간 이동과 비슷한 수준**이며, 상호운용성(interoperability) 기능 덕분에 일부 측면에서는 오히려 더 수월합니다.

핵심은 다음과 같습니다.

- Scala 3는 새로운 컴파일러(Dotty 기반)와 새로운 중간 표현(intermediate representation)인 **TASTy**를 도입했습니다.
- 그럼에도 Scala 2.13과 Scala 3는 **서로의 산출물을 읽고 함께 컴파일/링크할 수 있는 상호운용성**을 갖추고 있습니다.
- 따라서 거대한 애플리케이션도 **한 번에 전부가 아니라 모듈 단위로 점진적으로(one module at a time)** 마이그레이션할 수 있습니다.

> 💡 **왜 필요한가 — "한 번에 다 안 바꿔도 된다"가 핵심**
>
> 코드가 수십만 줄인 회사 프로젝트를 하루아침에 통째로 새 버전으로 바꾸는 건 위험하고 비현실적입니다. Scala 3는 "옛 코드(2.13)와 새 코드(3)가 한 프로젝트 안에서 공존"하도록 설계되어, **일부 모듈만 먼저 옮기고 나머지는 천천히** 옮길 수 있습니다. 이 문서의 거의 모든 내용이 결국 "어떻게 하면 부분적·안전하게 옮길 수 있는가"를 설명하는 것입니다.

호환성의 차원, 알려진 비호환성 목록, 컴파일러 마이그레이션 모드와 도구를 순서대로 설명합니다.

---

## 2. 호환성의 네 가지 차원

> 📘 **처음 배우는 분께 — "호환성"이 한 가지가 아니다**
>
> "호환된다"는 말은 막연하지만, 여기서는 서로 다른 세 가지 질문으로 갈라집니다. 비유로 정리하면:
>
> - **소스(source) 호환** — *같은 글(소스 코드)을 두 사람(컴파일러 버전)이 똑같이 읽을 수 있는가?* → 옛 코드를 새 컴파일러에 그대로 넣어도 컴파일되는가.
> - **클래스패스/바이너리 호환** — *이미 번역되어 출판된 책(컴파일된 라이브러리 `.jar`)을 다른 버전이 가져다 쓸 수 있는가?* → 소스를 다시 컴파일하지 않고 결과물끼리 맞물리는가.
> - **런타임(runtime) 호환** — *실제로 돌렸을 때 같은 동작을 하는가?* → 컴파일은 됐는데 실행 결과가 달라지지 않는가.
>
> 뒤에 나오는 **TASTy**는 Scala 3가 만들어 내는 "라이브러리 결과물 안에 담기는 상세 설계도"라고 보면 됩니다. 이게 있어야 다른 버전이 그 라이브러리의 타입을 정확히 읽을 수 있습니다.

공식 마이그레이션 가이드는 호환성을 다음 네 가지 주요 차원으로 나누어 다룹니다.

| 차원 | 핵심 질문 | 비고 |
|---|---|---|
| **소스 수준(Source Level)** | Scala 3는 근본적으로 다른 언어인가? 기존 Scala 2.13 코드베이스를 포팅(port)하는 데 얼마나 노력이 드는가? | 대부분의 코드는 그대로 컴파일되거나 자동 재작성으로 해결됨 |
| **클래스패스 수준(Classpath Level)** | Scala 2.13 라이브러리를 Scala 3에서 쓸 수 있는가? 그 반대는? | "Scala 3 언피클러"와 "TASTy 리더"로 양방향 상호운용 |
| **런타임(Runtime)** | 프로덕션 배포가 안전한가? 두 버전 사이의 성능 특성은? | 동일 JVM 바이트코드 모델, 프로덕션 안전 |
| **메타프로그래밍(Metaprogramming)** | 매크로 시스템이 교체되었는데 어떻게 대응하는가? | Scala 2 매크로는 재구현 필요, 새로운 매크로 API 제공 |

이어지는 섹션에서 각 차원을 상세히 다룹니다.

---

## 3. 소스 수준 호환성(Source Level Compatibility)

소스 수준 호환성(source compatibility)이란, **동일한 소스 코드가 두 컴파일러 버전 모두에서 동일하게 컴파일되는지**를 의미합니다.

- Scala 3는 핵심 기반을 재설계했지만, **대부분의 Scala 2.13 소스 코드는 Scala 3에서도 그대로 컴파일**됩니다.
- 컴파일되지 않는 부분(비호환성)은 잘 정의되어 있으며, 다음 두 가지 방식으로 대부분 자동 해결됩니다.
  - **Scala 3 마이그레이션 모드(`-source:3.0-migration`)** + **자동 재작성(`-rewrite`)**
  - **Scalafix** 규칙(개별 규칙을 하나씩 적용)
- 남은 일부 비호환성만 수동으로 수정하면 됩니다.

> 즉, Scala 3는 "완전히 새로운 언어"라기보다는, **Scala 2.13에서 매끄럽게 진화한 언어**이며, 자동화 도구로 포팅 비용을 크게 낮출 수 있습니다.

모든 알려진 소스 비호환성과 그 대응(경고/자동 재작성/Scalafix 규칙)은 [7. 비호환성 표](#7-비호환성-표incompatibility-table)에 정리되어 있습니다.

---

## 4. 클래스패스 수준 호환성(Classpath Level Compatibility)

클래스패스 수준 호환성(classpath compatibility)이란, **서로 다른 컴파일러로 빌드된 모듈/라이브러리를 같은 클래스패스(classpath)에 놓고 함께 사용할 수 있는지**를 가리킵니다. 이는 시그니처(signature)를 읽어 타입 검사(type check)를 수행하는 능력에 달려 있습니다.

Scala 3와 Scala 2.13 모듈은 **양방향으로 상호운용(interoperate)** 할 수 있습니다.

---

### 4.1 Scala 3 언피클러(Unpickler)

Scala 3 컴파일러는 **Scala 2.13의 Pickle 포맷(Pickle format)을 읽을 수 있습니다.**

> "The Scala 3 compiler is able to read the Scala 2.13 Pickle format and thus it can type check code that depends on modules or libraries compiled with Scala 2.13."
> (Scala 3 컴파일러는 Scala 2.13 Pickle 포맷을 읽을 수 있으며, 따라서 Scala 2.13으로 컴파일된 모듈이나 라이브러리에 의존하는 코드를 타입 검사할 수 있습니다.)

- 이 기능을 담당하는 컴포넌트를 **Scala 3 언피클러(unpickler)** 라고 부릅니다.
- 언피클러는 커뮤니티 차원에서 광범위하게 검증되었으며 **프로덕션(production) 사용에 안전**한 것으로 인정됩니다.

> 📘 **처음 배우는 분께 — Pickle / 언피클러가 뭔가요**
>
> 컴파일된 Scala 2 라이브러리 안에는 "이 라이브러리에 어떤 타입과 메서드가 들었는지" 적어둔 메타데이터가 들어 있는데, 이 포맷을 **Pickle**이라고 부릅니다(데이터를 차곡차곡 절여 담아 둔 병 같은 것). 거꾸로 그 병을 열어 읽어 들이는 것이 **언피클러(unpickler)** 입니다.
>
> 즉 "Scala 3 언피클러"는 *Scala 2.13으로 만든 라이브러리를 Scala 3 컴파일러가 열어 읽는 기능*입니다. 덕분에 아직 Scala 3로 다시 안 만든 옛 라이브러리도 새 프로젝트에서 바로 가져다 쓸 수 있습니다.

> 💡 **왜 필요한가 — `CrossVersion.for3Use2_13`는 무엇을 하나**
>
> Scala 라이브러리는 보통 버전별로 따로 출판됩니다. 그래서 이름 뒤에 `_2.13`, `_3` 같은 꼬리표가 붙습니다(`some-library_2.13`처럼). Scala 3 프로젝트는 기본적으로 `_3` 꼬리표가 붙은 것을 찾는데, 아직 `_3` 버전이 없는 라이브러리라면 못 찾습니다.
>
> `CrossVersion.for3Use2_13`은 빌드 도구에게 **"이 라이브러리는 `_3` 말고 `_2.13` 것을 가져와라"** 라고 알려주는 설정입니다. 위 언피클러 덕분에 그렇게 가져온 옛 라이브러리도 문제없이 동작합니다.

**실전 적용 예시 (sbt):**

Scala 3 모듈이 Scala 2.13 모듈에 의존할 수 있습니다.

```scala
lazy val foo = project.in(file("foo"))
  .settings(scalaVersion := "3.3.1")
  .dependsOn(bar)

lazy val bar = project.in(file("bar"))
  .settings(scalaVersion := "2.13.11")
```

위 설정에서 `foo`(Scala 3)는 `bar`(Scala 2.13)에 의존합니다.

**퍼블리시된(published) 라이브러리의 경우:**

Scala 3 모듈이 Maven/Ivy로 퍼블리시된 Scala 2.13 라이브러리에 의존할 때는, 올바른 바이너리 버전(binary version) 접미사를 해석하기 위해 `CrossVersion.for3Use2_13`을 사용합니다.

```scala
libraryDependencies +=
  ("org.example" %% "some-library" % "1.0.0").cross(CrossVersion.for3Use2_13)
```

이는 `some-library_2.13` 아티팩트를 Scala 3 프로젝트에서 사용하도록 지시합니다.

---

### 4.2 표준 라이브러리(Standard Library)

- **Scala 2.13 표준 라이브러리(standard library)가 곧 Scala 3의 공식 표준 라이브러리입니다.**
- 빌드 도구(sbt 등)가 이를 자동으로 제공하므로, 별도 설정 없이 동일한 컬렉션/타입을 사용합니다.
- 이는 두 언어 간 상호운용의 토대가 됩니다. 두 버전이 같은 표준 라이브러리를 공유하므로 컬렉션, 타입 등을 자연스럽게 주고받을 수 있습니다.

---

### 4.3 Scala 2.13용 TASTy 리더(TASTy Reader)

> 📘 **처음 배우는 분께 — TASTy와 TASTy 리더**
>
> **TASTy**는 Scala 3 컴파일러가 코드를 컴파일할 때 함께 만들어 내는 *"타입 정보까지 다 담긴 상세 설계도 파일"* 입니다(Typed Abstract Syntax Trees의 약자). 앞의 Pickle보다 훨씬 풍부한 정보를 담습니다.
>
> **TASTy 리더**는 그 설계도를 읽어 들이는 기능입니다. 바로 앞 절(언피클러)이 "Scala 3가 Scala 2 라이브러리를 읽는" 방향이었다면, 여기 TASTy 리더는 그 **반대 방향** — "Scala 2.13이 Scala 3 라이브러리를 읽는" 기능입니다. 다만 Scala 3에만 있는 새 기능(유니온 타입, 매치 타입 등)까지는 Scala 2가 이해하지 못하므로 아래 표처럼 일부만 지원합니다.

반대 방향, 즉 **Scala 2.13이 Scala 3 라이브러리를 소비(consume)** 하는 것도 가능합니다. 이를 위해 Scala 2.13 컴파일러에 **`-Ytasty-reader`** 컴파일러 옵션을 사용합니다. 이는 전방 호환성(forward compatibility)을 가능하게 합니다.

다만 TASTy 리더는 Scala 3의 모든 기능을 지원하지는 않습니다. 지원 수준은 다음과 같이 분류됩니다.

| 지원 수준 | 기능 |
|---|---|
| **지원(Supported)** | 열거형(enumerations / `enum`), 교차 타입(intersection types), 불투명 타입 별칭(opaque type aliases), 타입 람다(type lambdas), 문맥 추상화(contextual abstractions, `given`/`using`), 오픈 클래스(open classes), `export` 절(export clauses) |
| **부분 지원(Partial Support)** | 최상위 정의(top-level definitions), 확장 메서드(extension methods) |
| **미지원(Unsupported)** | 문맥 함수(context functions), 다형성 함수 타입(polymorphic function types), 트레이트 파라미터(trait parameters), 매치 타입(match types), 유니온 타입(union types), `inline` 및 Scala 3 매크로(inline and Scala 3 macros), 종 다형성(kind polymorphism) |

> 즉, 미지원 기능(유니온 타입, 매치 타입, `inline` 등)을 노출하는 Scala 3 라이브러리는 Scala 2.13에서 사용할 수 없습니다.

---

### 4.4 점진적 마이그레이션 전략과 샌드위치 패턴

공식 문서는 **점진적 마이그레이션(gradual migration)** 을 강조합니다.

> "You can port a big Scala application one module at a time, even if its library dependencies have not yet been ported."
> (라이브러리 의존성이 아직 포팅되지 않았더라도, 큰 Scala 애플리케이션을 한 번에 한 모듈씩 포팅할 수 있습니다.)

이로 인해 **샌드위치 패턴(sandwich pattern)** 이 가능합니다. 즉, **Scala 2.13 모듈 사이에 Scala 3 모듈을 끼워 넣는** 구조를 만들 수 있습니다.

```
Scala 2.13 모듈  →  Scala 3 모듈  →  Scala 2.13 모듈
   (위쪽)            (가운데)          (아래쪽)
```

**단, 전제 조건이 있습니다.**

- 모든 라이브러리가 **단일 바이너리 버전(single binary version)으로 해석**되어야 합니다.
- 즉, 클래스패스에 `lib-foo_3`와 `lib-foo_2.13`가 **동시에 존재하는 충돌(conflict)** 이 발생해서는 안 됩니다. 같은 라이브러리가 두 바이너리 버전으로 동시에 들어오면 충돌이 발생합니다.

> 📘 **처음 배우는 분께 — "샌드위치는 OK, 같은 라이브러리 두 버전은 NG"**
>
> 두 규칙이 헷갈릴 수 있어 정리합니다.
>
> - **허용**: Scala 2 모듈과 Scala 3 모듈을 한 프로젝트에 섞는 것(샌드위치) — 이건 괜찮습니다.
> - **금지**: *똑같은* 라이브러리 하나가 `_2.13` 버전과 `_3` 버전으로 **둘 다** 끌려 들어오는 것 — 같은 이름의 도구가 두 개라 어느 걸 쓸지 충돌이 납니다.
>
> 비유하면, 부품 공급처가 두 곳인 건 괜찮지만, *완전히 똑같은 부품*이 규격만 다른 두 종류로 동시에 배송 오면 조립이 안 되는 것과 같습니다.

---

### 4.5 라이브러리 메인테이너를 위한 주의사항

공식 문서는 **라이브러리 메인테이너(library maintainers)에게 명시적 경고**를 합니다.

- **Scala 2.13 라이브러리에 의존하는 Scala 3 라이브러리를 퍼블리시하지 말 것.**
- 이렇게 하면 최종 사용자(end users)에게 클래스패스 충돌(classpath conflicts)을 유발할 수 있습니다.

다시 말해, 애플리케이션 내부에서 점진적으로 샌드위치를 만드는 것은 자신이 통제할 수 있으므로 안전하지만, **공개 라이브러리로 퍼블리시할 때**는 다른 바이너리 버전 의존성이 사용자 측에서 충돌을 일으킬 수 있으므로 피해야 합니다.

---

## 5. 런타임(Runtime) 고려사항

- Scala 3는 Scala 2와 동일한 JVM 바이트코드(bytecode) 모델로 컴파일되므로, **프로덕션 배포(production deployment)에 안전**합니다.
- 런타임 동작과 성능 특성은 Scala 2.13과 사실상 동등하며, 동일한 표준 라이브러리를 공유하므로 런타임 의미(runtime semantics)도 일관됩니다.
- 일부 비호환성(예: 암시적 변환(implicit views)의 적용 여부 변화)은 컴파일 결과뿐 아니라 **런타임 동작에 영향(Runtime: Possible)** 을 줄 수 있으므로, 해당 항목은 [7.3 문맥 추상화](#73-문맥-추상화contextual-abstractions) 표에서 별도로 표시됩니다.

---

## 6. 메타프로그래밍/매크로(Macros) 변경

> ⚠️ **짚고 넘어가기 — 매크로는 도구가 자동으로 못 옮긴다**
>
> 이 문서 대부분은 "도구가 알아서 고쳐준다"는 안심되는 이야기지만, **매크로(macro)** 만은 예외입니다. 매크로는 *"컴파일 시점에 코드를 만들어 내는 코드"* 인데, Scala 3는 매크로를 만드는 방식 자체를 완전히 새로 갈아엎었습니다. 그래서 옛 매크로는 자동 변환이 **불가능하고, 사람이 새 API로 다시 작성(재구현)** 해야 합니다.
>
> 새로 배우는 분 입장에서 실무적 교훈은 하나입니다: 어떤 라이브러리가 Scala 3로 옮겨지는 게 유독 늦다면, 그 라이브러리가 옛 매크로를 많이 써서 손이 많이 가기 때문인 경우가 흔합니다.

- Scala 2의 매크로 시스템(`scala.reflect` / `def macro` 기반)은 **Scala 3에서 동작하지 않으며 완전히 교체**되었습니다.
- Scala 3는 새로운 메타프로그래밍 API를 제공합니다.
  - **인라인(`inline`)**: 컴파일 타임에 코드를 펼치는 메커니즘
  - **인용/접합(quotes & splices, `'{ ... }` / `${ ... }`)**: 타입 안전한 코드 생성
  - **TASTy 리플렉션(reflection)**: AST 수준의 검사/조작
- Scala 2 매크로를 사용하는 라이브러리는 **Scala 3용으로 매크로를 재구현(reimplement)** 해야 합니다.
- 매크로는 Scala 3 TASTy 리더에서도 **미지원**이므로, 매크로를 노출하는 Scala 3 라이브러리는 Scala 2.13에서 사용할 수 없습니다([4.3 참조](#43-scala-213용-tasty-리더tasty-reader)).

---

## 7. 비호환성 표(Incompatibility Table)

공식 가이드는 알려진 비호환성(incompatibility)을 아래 범주로 분류합니다.

1. **구문 변경(Syntactic Changes)** — 더 이상 지원되지 않는 옛 구문
2. **제거된 기능(Dropped Features)** — 언어 단순화를 위해 제거된 기능
3. **문맥 추상화(Contextual Abstractions)** — 재설계된 암시(implicit) 시스템으로 인한 변경
4. **그 밖의 변경된 기능(Other Changed Features)** — 단순화되거나 제한된 기능
5. **타입 검사기(Type Checker)** — Scala 2.13의 불건전성(unsoundness) 버그 수정
6. **타입 추론(Type Inference)** — 변경된 추론 규칙
7. **매크로(Macros)** — 재구현이 필요한 Scala 2.13 매크로

각 표의 열(column) 의미는 다음과 같습니다.

- **Scala 2.13 경고(Warning)**: Scala 2.13에서 미리 받을 수 있는 경고. `Deprecation`(지원 중단 경고) 또는 `Feature warning`(기능 경고) 등.
- **마이그레이션 재작성(Migration Rewrite)**: Scala 3 마이그레이션 모드(`-source:3.0-migration -rewrite`)가 자동으로 코드를 고쳐줄 수 있는지 여부. (`✅` = 가능)
- **Scalafix 규칙(Rule)**: 별도 도구인 Scalafix가 해당 항목을 처리하는 규칙을 제공하는지 여부.
- **런타임(Runtime)**: 런타임 동작에 영향을 줄 가능성이 있는지.

> 표기: `✅` = 지원/제공됨, `—` = 해당 없음/제공되지 않음.

---

### 7.1 구문 변경(Syntactic Changes)

| 비호환성 | Scala 2.13 경고 | 마이그레이션 재작성 | Scalafix 규칙 |
|---|---|---|---|
| 제한된 키워드(Restricted keywords) | — | ✅ | — |
| 프로시저 구문(Procedure syntax) | Deprecation | ✅ | ✅ |
| 람다 파라미터 주위의 괄호(Parentheses around lambda parameter) | — | ✅ | — |
| 인자 전달 시 여는 중괄호 들여쓰기(Open brace indentation for passing argument) | — | ✅ | — |
| 잘못된 들여쓰기(Wrong indentation) | — | — | — |
| 타입 파라미터로서의 `_` (`_` as type parameter) | — | — | — |
| 타입 파라미터로서의 `+`와 `-` (`+` and `-` as type parameters) | — | — | — |

**설명:**

- **제한된 키워드(Restricted keywords)**: Scala 3에서 새로 예약된 키워드(예: `enum`, `given`, `export` 등)와 충돌하는 식별자는 더 이상 일반 식별자로 쓸 수 없습니다. 자동 재작성으로 백틱(backtick) 처리됩니다.
- **프로시저 구문(Procedure syntax)**: `def foo() { ... }`처럼 반환 타입과 `=`를 생략하던 옛 구문은 제거되었습니다. `def foo(): Unit = { ... }` 형태로 자동 재작성됩니다.
- **람다 파라미터 주위의 괄호**: `(x) => x + 1`처럼 단일 파라미터에 불필요한 괄호를 두던 구문이 제한됩니다.
- **여는 중괄호 들여쓰기 / 잘못된 들여쓰기**: Scala 3의 들여쓰기 기반 구문(indentation-based syntax) 규칙에 맞지 않는 코드는 재작성/수정이 필요합니다.
- **타입 파라미터로서의 `_`, `+`, `-`**: 새로운 타입 시스템 구문과의 충돌로 해당 사용이 제한됩니다.

---

### 7.2 제거된 기능(Dropped Features)

| 비호환성 | Scala 2.13 경고 | 마이그레이션 재작성 | Scalafix 규칙 |
|---|---|---|---|
| 심볼 리터럴(Symbol literals) | Deprecation | ✅ | — |
| `do`-`while` 구문(`do`-`while` construct) | — | ✅ | — |
| 자동 적용(Auto-application) | Deprecation | ✅ | ✅ |
| 값 에타 확장(Value eta-expansion) | Deprecation | ✅ | ✅ |
| `any2stringadd` 변환(`any2stringadd` conversion) | Deprecation | — | ✅ |
| 초기화 선행 블록(Early initializer) | Deprecation | — | — |
| 실존 타입(Existential type) | Feature warning | — | — |

**설명:**

- **심볼 리터럴(Symbol literals)**: `'foo`와 같은 심볼 리터럴 구문이 제거되었습니다. `Symbol("foo")`로 대체합니다.
- **`do`-`while` 구문**: `do { ... } while (cond)` 구문이 제거되었습니다. `while` 루프로 변환하는 자동 재작성이 제공됩니다.
- **자동 적용(Auto-application)**: 빈 파라미터 목록(empty argument list) 메서드를 인자 없이 호출하던 것(`foo` ↔ `foo()`)이 제한됩니다. 명시적으로 `foo()`를 쓰도록 자동 재작성됩니다.
- **값 에타 확장(Value eta-expansion)**: 값을 함수로 자동 변환하던 동작이 변경되어 명시적 표현이 필요합니다.
- **`any2stringadd` 변환**: 임의의 값에 `+ "문자열"`을 적용하던 암시적 문자열 결합 변환이 제거되었습니다. `String.valueOf(x) + ...` 등으로 대체합니다(Scalafix 규칙 제공).
- **초기화 선행 블록(Early initializer)**: `new { val x = ... } with Trait` 형태의 옛 초기화 구문이 제거되었습니다. **트레이트 파라미터(trait parameters)** 로 대체합니다(자동 재작성 없음, 수동 수정 필요).
- **실존 타입(Existential type)**: `T forSome { ... }` 형태의 실존 타입 구문이 제거되었습니다(수동 수정 필요).

---

### 7.3 문맥 추상화(Contextual Abstractions)

재설계된 암시(implicit) 시스템으로 인한 변경입니다.

| 비호환성 | Scala 2.13 경고 | 마이그레이션 재작성 | Scalafix 규칙 | 런타임 |
|---|---|---|---|---|
| 암시적 def의 타입(Type of implicit def) | — | — | ✅ | — |
| 암시적 뷰(Implicit views) | — | — | — | 가능(Possible) |
| 뷰 바운드(View bounds) | Deprecation | — | — | — |
| `A`와 `=> A`에 대한 모호한 변환(Ambiguous conversion on `A` and `=> A`) | — | — | — | — |

**설명:**

- **암시적 def의 타입(Type of implicit def)**: 암시적 정의의 결과 타입(result type)을 명시하지 않으면 추론 결과가 달라질 수 있어 명시가 권장됩니다(Scalafix 규칙 제공).
- **암시적 뷰(Implicit views)**: 암시적 변환(implicit conversion)이 적용되는 규칙이 변경되어, **런타임 동작이 달라질 가능성(Possible)** 이 있습니다. 변환이 더 이상 자동 적용되지 않으면 동작이 변할 수 있으므로 주의가 필요합니다.

> ⚠️ **짚고 넘어가기 — "컴파일은 됐는데 결과가 달라지는" 가장 까다로운 경우**
>
> 대부분의 비호환성은 *컴파일이 안 되는* 형태라 컴파일러가 바로 잡아냅니다. 하지만 이 "암시적 뷰" 항목은 다릅니다 — **컴파일은 통과하는데 실행 결과만 조용히 달라질 수 있는** 경우라, 가장 발견하기 어렵습니다. 그래서 표에 "런타임: 가능(Possible)"이라고 따로 표시한 것입니다. 마이그레이션 마지막에 **테스트를 꼭 돌려보라**(절차 10번)고 강조하는 이유가 바로 이런 항목 때문입니다.
>
> (암시적 변환이 무엇인지는 `00_prerequisites_scala_basics.md` 9번 항목을 참고하세요.)
- **뷰 바운드(View bounds)**: `[A <% B]` 형태의 뷰 바운드 구문이 제거되었습니다. 문맥 바운드(`[A : C]`)와 `given`/`using`으로 대체합니다.
- **`A`와 `=> A`에 대한 모호한 변환**: 즉시값 타입(`A`)과 이름에 의한 호출 타입(`=> A`) 사이에서 발생하던 변환 모호성에 대한 규칙이 변경되었습니다.

---

### 7.4 그 밖의 변경된 기능(Other Changed Features)

단순화되거나 제한된 기능입니다.

| 비호환성 | 마이그레이션 재작성 |
|---|---|
| 상속 그림자(Inheritance shadowing) | ✅ |
| private 클래스의 non-private 생성자(Non-private constructor in private class) | 마이그레이션 경고(Migration Warning) |
| 추상 오버라이드(Abstract override) | — |
| 케이스 클래스 동반 객체(Case class companion) | — |
| `unapply`에 대한 명시적 호출(Explicit call to unapply) | — |
| 보이지 않는 빈 프로퍼티(Invisible bean property) | — |
| 타입 인자로서의 `=>T` (`=>T` as type argument) | — |
| 와일드카드 타입 인자(Wildcard type argument) | — |

**설명:**

- **상속 그림자(Inheritance shadowing)**: 상속받은 멤버가 동일 이름의 다른 멤버를 가리는(shadowing) 모호한 상황에 대한 규칙이 변경되어 자동 재작성으로 명시화됩니다.
- **private 클래스의 non-private 생성자**: private 클래스 안에서 non-private 생성자를 두는 경우 마이그레이션 경고가 발생합니다.
- **추상 오버라이드(Abstract override), 케이스 클래스 동반 객체, `unapply` 명시적 호출, 보이지 않는 빈 프로퍼티, `=>T`를 타입 인자로, 와일드카드 타입 인자**: 각각 규칙이 변경되어 일부는 수동 수정이 필요합니다.

---

### 7.5 타입 검사기(Type Checker)

- Scala 3는 Scala 2.13에 존재하던 **불건전성(unsoundness) 버그를 수정**했습니다.
- 특히 **변성 검사(variance checks)** 와 **패턴 매칭(pattern matching)** 에서의 불건전성이 수정되었습니다.
- 그 결과, Scala 2.13에서는 (잘못) 컴파일되던 일부 코드가 Scala 3에서는 거부될 수 있습니다. 이는 의도된 정확성(correctness) 개선입니다.

> 💡 **왜 필요한가 — "멀쩡하던 코드가 Scala 3에서 거부되는" 게 사실은 좋은 일**
>
> 여기서는 "원래 통과하던 코드가 Scala 3에서 막힌다"는데, 버그처럼 들리지만 반대입니다. Scala 2에는 **타입 검사가 놓치던 구멍(불건전성, unsoundness)** 이 있어서, 사실은 위험한 코드인데도 통과시켜 주던 경우가 있었습니다. Scala 3는 그 구멍을 막았기 때문에, 옛날엔 "운 좋게" 통과하던 코드가 이제 정직하게 거부되는 것입니다. 즉 컴파일러가 더 깐깐해진 게 아니라 더 **정확해진** 것이고, 막힌 코드는 원래 고쳐야 할 코드였던 셈입니다.

---

### 7.6 타입 추론(Type Inference)

- **오버라이드된 메서드(overridden methods)의 반환 타입 추론(return type inference)** 규칙이 변경되었습니다.
- **리플렉티브 타입(reflective type) 처리** 방식이 변경되었습니다.
- 따라서 일부 경우 타입을 명시적으로 작성해야(annotate) 할 수 있습니다.

---

### 7.7 매크로(Macros)

- Scala 2.13 매크로는 Scala 3에서 동작하지 않으며 **반드시 재구현(reimplement)** 해야 합니다. 자세한 내용은 [6. 메타프로그래밍/매크로 변경](#6-메타프로그래밍매크로macros-변경)을 참고하세요.

---

## 8. Scala 3 마이그레이션 모드(Migration Mode)와 도구

마이그레이션을 위한 두 가지 주요 접근법이 있습니다.

> 📘 **처음 배우는 분께 — 마이그레이션 "도구" 한눈에 정리**
>
> 여기서부터 도구 이름이 쏟아집니다. 무엇을 하는 도구인지 한 줄씩 먼저 잡고 가세요.
>
> - **`scalac`** — Scala 컴파일러 자체(소스를 실행 가능한 형태로 번역하는 프로그램). 아래 옵션들은 모두 이 `scalac`에 붙이는 것입니다.
> - **마이그레이션 모드(`-source:3.0-migration`)** — 컴파일러에게 "옛 문법을 만나도 에러로 멈추지 말고 경고만 띄워라"라고 시키는 *옵션*. 별도 설치가 필요 없습니다.
> - **자동 재작성(`-rewrite`)** — 위 모드와 함께 쓰면, 컴파일러가 옛 문법을 새 문법으로 **소스 파일을 직접 고쳐주는** 옵션.
> - **Scalafix** — 컴파일러와 별개인 *외부 도구*로, 마이그레이션 변환 규칙을 **하나씩 골라** 적용하는 코드 정리·변환 도구.
> - **`-explain` / `-explain-types`** — 에러가 왜 났는지 자세히 풀어 설명해 주는 옵션(마이그레이션이 아니어도 평소 학습에 유용).

- **Scala 3 마이그레이션 모드(Scala 3 Migration Mode)**: 컴파일러에 내장(built-in)되어 있으며 자동(automatic)으로 동작합니다.
- **Scalafix**: 별도 도구로, 개별 규칙(individual rules)을 한 번에 하나씩(one at a time) 적용합니다.

`scalac`는 Scala 컴파일러 실행 파일로, GitHub에서 다운로드하거나 Coursier로 설치할 수 있습니다.

```
$ scalac
Usage: scalac <options> <source files>
```

> "scalac is the executable of the Scala compiler, it can be downloaded from Github. It can also be installed using Coursier with `cs install scala3-compiler`"

```
cs install scala3-compiler
```

---

### 8.1 `-source` 컴파일러 옵션

컴파일러는 마이그레이션을 위해 여러 소스 수준(source-level) 옵션을 제공합니다.

| 옵션 | 동작 |
|---|---|
| **`-source:3.0`** | 기본값(default). 표준 Scala 3 컴파일 동작. |
| **`-source:3.0-migration`** | 마이그레이션 모드. 제거된 대부분의 기능에 대해 **관대하게(forgiving)** 동작하며, 오류(error) 대신 **경고(warning)** 를 출력합니다. 각 경고는 해당 코드를 컴파일러가 안전하게 자동 재작성할 수 있음을 의미합니다. |
| **`-source:future`** | 미래(future) 언어 버전에 대한 표준 컴파일 모드. |
| **`-source:future-migration`** | 미래 언어 버전에 대한 마이그레이션(관대한) 모드. |

---

### 8.2 마이그레이션 모드 동작 방식

`-source:3.0-migration`으로 컴파일하면, 공식 문서의 표현대로:

> "makes the compiler forgiving on most of the dropped features, printing warnings in place of errors."
> (컴파일러가 제거된 대부분의 기능에 대해 관대해지며, 오류 대신 경고를 출력합니다.)

이를 통해 컴파일을 중단시키지 않고도 더 이상 지원되지 않는(deprecated) 코드를 식별할 수 있습니다.

---

### 8.3 자동 재작성(`-rewrite`)과 2단계 워크플로

`-rewrite` 플래그를 `-source:3.0-migration`과 함께 사용하면, 컴파일러가 **소스 코드를 자동으로 변환(automatic code transformation)** 합니다.

> "almost all warnings can be resolved automatically by the compiler itself"
> (거의 모든 경고는 컴파일러 자체에 의해 자동으로 해결될 수 있습니다.)

**주요 제약 사항:**

- 코드에 (재작성 대상이 아닌) **오류가 있으면 재작성이 적용되지 않습니다.**
- 적용 가능한 모든 규칙이 **자동으로 실행되며, 특정 재작성 규칙을 선택적으로 비활성화할 수 없습니다.**
- 컴파일러가 **소스 파일을 직접 수정**하므로, 사전에 코드를 백업(version control commit)하는 것이 권장됩니다.

> ⚠️ **짚고 넘어가기 — `-rewrite`는 내 파일을 진짜로 덮어쓴다**
>
> 보통 컴파일은 "원본은 그대로 두고 결과물만 따로 만드는" 작업이라 안전합니다. 그런데 `-rewrite`는 다릅니다 — **원본 소스 파일을 그 자리에서 직접 고쳐 씁니다.** 그래서 실행 전에 반드시 **Git 등으로 먼저 커밋(저장)** 해 두세요. 그래야 도구가 무엇을 바꿨는지 diff로 확인하고, 마음에 안 들면 되돌릴 수 있습니다. "안전하게 설계됐다"고는 하지만, 안전망(커밋)을 깔아두는 건 별개의 일입니다.

**권장 2단계 워크플로(Two-Step Workflow):**

**1단계 - 문제 식별(Identify issues):**

```
scalac -source:3.0-migration <files>
```

**2단계 - 자동 재작성 적용(Apply automatic rewrites):**

```
scalac -source:3.0-migration -rewrite <files>
```

> "Beware that the compiler will modify the code! It is intended to be safe. However you may want to commit the initial state so that you can print the diff applied by the compiler and revert if necessary."
> (주의: 컴파일러가 코드를 수정합니다! 이는 안전하도록 설계되었지만, 컴파일러가 적용한 diff를 확인하고 필요하면 되돌릴 수 있도록 초기 상태를 커밋해 두는 것이 좋습니다.)

**sbt에서의 설정 예시:**

```scala
scalacOptions ++= Seq("-source:3.0-migration", "-rewrite")
```

전형적인 마이그레이션 경로는 다음과 같습니다.

1. `-source:3.0-migration`으로 컴파일하여 문제를 식별한다.
2. `-rewrite`를 추가하여 자동 수정을 적용한다.
3. 남은 오류는 [비호환성 표](#7-비호환성-표incompatibility-table)를 참고하여 수동으로 해결한다.

---

### 8.4 Scalafix를 통한 마이그레이션

- **Scalafix**는 컴파일러와는 별개의 도구로, 마이그레이션을 위한 개별 규칙들을 제공합니다.
- 마이그레이션 모드의 자동 재작성이 "모든 규칙을 한꺼번에" 적용하는 것과 달리, Scalafix는 **규칙을 하나씩(one at a time) 선택해 적용**할 수 있습니다.
- 비호환성 표에서 "Scalafix 규칙(`✅`)"이 표시된 항목(예: 프로시저 구문, 자동 적용, 값 에타 확장, `any2stringadd` 변환, 암시적 def의 타입)은 Scalafix로 처리할 수 있습니다.

---

### 8.5 오류 진단 옵션

마이그레이션뿐 아니라 일반적인 Scala 3 학습/개발에도 유용한 진단 옵션입니다.

| 옵션 | 설명 |
|---|---|
| **`-explain`** | 상세한 오류 설명(detailed error explanations)을 제공합니다. |
| **`-explain-types`** | 타입 관련 문제(type-related issues)를 명확히 설명합니다. |

이 옵션들은 마이그레이션에 국한되지 않고 Scala 3 학습 전반에 유용합니다.

---

## 9. Scala 3의 주요 신기능 요약

마이그레이션 시 Scala 3의 신기능을 파악해 두면 옛 구문을 어떤 새 구문으로 대체할지 판단하기 쉽습니다. (출처: Scala 3 Book - Scala Features)

### 고수준(High-Level) 특성

- 간결하고 읽기 쉬운 구문(concise, readable syntax)을 가진 고수준 언어.
- **정적 타입(statically-typed)이지만 동적인 느낌(feels dynamic)**: 강력한 타입 추론(type inference) 덕분.
- **함수형 프로그래밍(FP)과 객체지향 프로그래밍(OOP)을 모두 지원**.
- JVM 위에서 실행되며 Java와 매끄러운 상호운용(seamless interop) 가능.
- 서버 애플리케이션, 마이크로서비스, 빅데이터 처리, 브라우저(Scala.js) 개발에 활용.

### 표현력 있는 타입 시스템(Expressive Type System)

- 추론 타입(inferred types), 제네릭 클래스(generic classes), 변성 주석(variance annotations)
- 상한/하한 타입 바운드(upper/lower type bounds)
- 다형성 메서드(polymorphic methods), 교차/유니온 타입(intersection/union types)
- 타입 람다(type lambdas), 의존 함수 타입(dependent function types)
- `given` 인스턴스와 `using` 절을 통한 문맥 파라미터(context parameters)
- 확장 메서드(extension methods)와 타입 클래스(type classes)
- 다중 보편 동등성(multiversal equality), 불투명 타입 별칭(opaque type aliases)
- 오픈 클래스(open classes), 매치 타입(match types)

### Scala 2 대비 저수준(Lower-Level) 개선 — 마이그레이션 매핑 관점

| 옛 방식 (Scala 2) | 새 방식 (Scala 3) |
|---|---|
| 여러 `implicit` 용도 | `given`, `using`, `extension` 키워드로 분리/명시화 |
| 암시적 클래스(implicit class)로 확장 | 명확한 확장 메서드(`extension`) |
| 초기화 선행 블록(early initializer) | 트레이트 파라미터(trait parameters) |
| 봉인 계층 + `sealed trait` + case object | 간결한 `enum`으로 ADT(대수적 데이터 타입) 정의 |
| 다수의 중괄호/`new` 키워드 | "조용한(quiet)" 제어 구문, 선택적 중괄호, `new` 키워드 축소 |
| (없음) | `open` 수정자로 의도적 상속 표시 |
| (없음) | 유니온/교차 타입으로 유연한 모델링 |
| (없음) | `@targetName`으로 Java 상호운용 개선, `@infix`로 메서드 적용 명확화 |
| (없음) | `export` 절로 멤버 재노출(aggregation) |

### 배포 옵션

- JVM 실행(보안/성능/메모리 관리), Scala.js(브라우저), Scala Native·GraalVM(네이티브 실행 파일).

---

## 10. 권장 마이그레이션 절차

위 내용을 종합한 실전 마이그레이션 절차는 다음과 같습니다.

> 💡 **왜 필요한가 — 왜 "2.13으로 먼저 올린 뒤" 3으로 가나**
>
> 곧장 Scala 3로 점프하지 않고 최신 Scala 2.13을 한 번 거치는 데는 이유가 있습니다. **Scala 3에서 문제가 될 옛 코드 상당수는 이미 Scala 2.13이 "곧 없어질 기능(deprecation) 경고"로 미리 알려줍니다.** 즉 2.13에서 경고를 먼저 다 잡아두면, 3으로 넘어갈 때 고칠 거리가 크게 줄어듭니다. 익숙한 버전에서 미리 청소를 끝내고 이사하는 셈입니다.
>
> 아래 절차를 한 문장으로 요약하면: **(2.13에서 청소) → (커밋) → (마이그레이션 모드로 문제 찾기) → (`-rewrite`·Scalafix로 자동 수정) → (남은 건 수동·매크로 재작성) → (테스트로 검증)** 입니다.

1. **준비**: 프로젝트를 최신 **Scala 2.13**으로 먼저 업그레이드하고, `-deprecation`/`-feature` 경고를 해소해 둔다(많은 Scala 3 비호환성이 Scala 2.13에서 Deprecation 경고로 미리 드러난다).
2. **의존성 확인**: 의존 라이브러리가 Scala 3(또는 TASTy 리더로 사용 가능한 형태)로 제공되는지 확인한다. 아직 포팅되지 않은 라이브러리는 [`CrossVersion.for3Use2_13`](#41-scala-3-언피클러unpickler)로 Scala 2.13 아티팩트를 사용할 수 있다.
3. **점진적 포팅**: 큰 애플리케이션은 [샌드위치 패턴](#44-점진적-마이그레이션-전략과-샌드위치-패턴)을 활용해 **모듈 단위로** 옮긴다. 단, 한 라이브러리가 두 바이너리 버전으로 충돌하지 않도록 한다.
4. **초기 상태 커밋**: 자동 재작성이 소스를 직접 수정하므로 먼저 버전 관리에 커밋한다.
5. **마이그레이션 모드 컴파일**: `scalac -source:3.0-migration <files>`로 비호환성을 식별한다.
6. **자동 재작성**: `scalac -source:3.0-migration -rewrite <files>`(또는 sbt `scalacOptions`)로 가능한 부분을 자동 수정하고, diff를 검토한다.
7. **Scalafix 적용**: 컴파일러 재작성이 처리하지 못하는 항목은 [Scalafix](#84-scalafix를-통한-마이그레이션) 규칙으로 처리한다.
8. **수동 수정**: [비호환성 표](#7-비호환성-표incompatibility-table)를 참고하여 자동화로 해결되지 않는 항목(실존 타입, 초기화 선행 블록, 타입 검사기/타입 추론 변경, 매크로 등)을 수동으로 고친다.
9. **매크로 재구현**: Scala 2 매크로 의존성은 Scala 3의 `inline` + 인용/접합 API로 [재구현](#6-메타프로그래밍매크로macros-변경)한다.
10. **검증**: 테스트를 실행하여 런타임 동작(특히 암시적 변환 관련) 변화가 없는지 확인한다.

---

## 참고 자료

- [Scala 3 공식 문서](https://docs.scala-lang.org/scala3/)
- [Scala 3 Migration Guide](https://docs.scala-lang.org/scala3/guides/migration/compatibility-intro.html)
- [Classpath Level Compatibility](https://docs.scala-lang.org/scala3/guides/migration/compatibility-classpath.html)
- [Incompatibility Table](https://docs.scala-lang.org/scala3/guides/migration/incompatibility-table.html)
- [Scala 3 Migration Mode (Tooling)](https://docs.scala-lang.org/scala3/guides/migration/tooling-migration-mode.html)
- [Scala 3 Book — Scala Features](https://docs.scala-lang.org/scala3/book/scala-features.html)

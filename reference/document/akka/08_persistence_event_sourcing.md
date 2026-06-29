# Akka 영속성: 이벤트 소싱

> 원본: https://doc.akka.io/libraries/akka-core/current/typed/persistence.html

---

## 목차

1. [개요: 이벤트 소싱이란](#개요-이벤트-소싱이란)
2. [EventSourcedBehavior의 구성 요소](#eventsourcedbehavior의-구성-요소)
3. [커맨드와 이벤트의 흐름](#커맨드와-이벤트의-흐름)
4. [이펙트(Effect)와 사이드 이펙트(Side Effect)](#이펙트effect와-사이드-이펙트side-effect)
5. [상태(State) 관리](#상태state-관리)
6. [복구(Recovery) 과정](#복구recovery-과정)
7. [클러스터 샤딩(Cluster Sharding)과의 통합](#클러스터-샤딩cluster-sharding과의-통합)
8. [태깅(Tagging)](#태깅tagging)
9. [이벤트 어댑터(Event Adapter)](#이벤트-어댑터event-adapter)
10. [응답(Reply) 패턴](#응답reply-패턴)
11. [직렬화(Serialization) 요구사항](#직렬화serialization-요구사항)
12. [저널 실패와 거부(Journal Failures and Rejections)](#저널-실패와-거부journal-failures-and-rejections)
13. [원자적 쓰기(Atomic Writes)](#원자적-쓰기atomic-writes)
14. [고급 기능](#고급-기능)
15. [의존성과 설정](#의존성과-설정)
16. [스타일 가이드(Style Guide)](#스타일-가이드style-guide)
17. [스냅샷(Snapshot)](#스냅샷snapshot)
18. [이벤트 소싱 동작 테스트하기](#이벤트-소싱-동작-테스트하기)
19. [복제 이벤트 소싱(Replicated Event Sourcing)](#복제-이벤트-소싱replicated-event-sourcing)
20. [CQRS](#cqrs)
21. [참고 자료](#참고-자료)

---

## 개요: 이벤트 소싱이란

Akka의 이벤트 소싱(event sourcing)은 상태를 가진 액터(stateful actor)가 상태 자체를 저장하는 대신, **상태 변화를 나타내는 이벤트(event)만을 저장**하여 자신의 상태를 영속화(persist)할 수 있게 합니다. 공식 문서의 표현으로는, "액터에 의해 영속화되는 것은 이벤트뿐이며, 액터의 실제 상태는 저장되지 않는다(only the events that are persisted by the actor are stored, not the actual state of the actor)".

이벤트는 저장소(storage)에 **추가(append)될 뿐 절대 변경(mutate)되지 않습니다.** 이러한 추가 전용(append-only) 특성 덕분에 높은 트랜잭션 처리율(high transaction rate)과 효율적인 복제(replication)가 가능합니다. 액터는 저장된 이벤트를 다시 재생(replay)함으로써 상태를 재구성(rebuild)하여 복구(recover)됩니다.

이벤트 소싱의 핵심 아이디어는 다음과 같습니다.

- 상태(state)는 현재 시점의 스냅샷에 불과하며, 진실의 원천(source of truth)은 시간 순서대로 누적된 이벤트의 로그(event log)입니다.
- 어떤 시점의 상태든 처음부터 이벤트를 순서대로 재생함으로써 재현할 수 있습니다.
- 이벤트는 이미 일어난 사실(fact)을 표현하므로 과거형으로 명명되며(예: `AccountCreated`, `MoneyDeposited`), 이미 발생한 사실이기 때문에 재생 도중에 실패할 수 없습니다.

---

## EventSourcedBehavior의 구성 요소

`EventSourcedBehavior`를 정의하려면 다음 네 가지 필수 구성 요소가 필요합니다.

1. **PersistenceId**: 백엔드 저널(journal) 및 스냅샷 저장소(snapshot store)에서 영속 액터(persistent actor)를 식별하는, 안정적이고 고유한 식별자(stable unique identifier)입니다. 이 식별자는 액터의 생애 동안 변하지 않아야 합니다.
2. **EmptyState**: 엔티티(entity)가 처음 생성되었을 때의 초기 상태(initial state)입니다.
3. **CommandHandler**: 들어오는 커맨드(command)를 처리하고 이펙트(Effect)를 생성합니다(예: 이벤트를 영속화하기).
4. **EventHandler**: 이벤트가 영속화된 후 상태를 갱신합니다.

이 동작(behavior)은 Command 타입에 대해 엄격하게 타입이 지정(strictly typed)되며, Akka의 타입 시스템(type system)이 이를 강제합니다.

`EventSourcedBehavior`를 구성하는 의사 코드(pseudo-code)의 형태는 다음과 같습니다.

```
EventSourcedBehavior[Command, Event, State](
  persistenceId = PersistenceId.ofUniqueId("abc"),
  emptyState = State(...),
  commandHandler = (state, command) => { ... },   // Effect 반환
  eventHandler = (state, event) => { ... }          // 새로운 State 반환
)
```

- **커맨드 핸들러(command handler)** 는 `(State, Command)`를 입력으로 받아 `Effect`를 반환합니다. 즉, "이 커맨드에 반응하여 무엇을 할 것인가"를 결정합니다.
- **이벤트 핸들러(event handler)** 는 `(State, Event)`를 입력으로 받아 새로운 `State`를 반환합니다. 즉, "이 이벤트가 영속화되었을 때 상태를 어떻게 바꿀 것인가"를 결정합니다.

---

## 커맨드와 이벤트의 흐름

커맨드(command)는 **영속화되지 않는(non-persistent) 메시지**로서, 현재 상태(current state)를 기준으로 검증(validate)됩니다. 검증이 성공하면, 커맨드의 효과(effect)를 나타내는 이벤트(event)가 생성됩니다. 이 이벤트들은 영속화된 뒤 액터의 상태를 변경하기 위해 적용(apply)됩니다.

여기서 가장 중요한 원칙은 다음과 같습니다. **"이벤트는 영속 액터에 재생될 때 실패할 수 없으며, 이는 커맨드와는 대조적이다(events cannot fail when being replayed to a persistent actor, in contrast to commands)."**

즉,

- **커맨드**: 거부될 수 있고(reject), 검증에 실패할 수 있습니다. 외부에서 들어온 "요청"이며, 비즈니스 규칙에 의해 받아들여지거나 거절됩니다.
- **이벤트**: 이미 검증을 통과하여 발생이 확정된 "사실"입니다. 재생 시 검증 로직이 다시 실행되지 않으며 무조건 적용됩니다. 따라서 이벤트 핸들러에는 절대로 실패할 수 있는 검증 로직을 두어서는 안 됩니다.

전형적인 처리 순서는 다음과 같습니다.

1. 커맨드 도착
2. 커맨드 핸들러가 현재 상태를 바탕으로 커맨드를 검증
3. 검증 통과 시, 하나 이상의 이벤트를 영속화(persist)하는 이펙트 반환
4. 이벤트가 저널에 성공적으로 기록됨
5. 이벤트 핸들러가 이벤트를 상태에 적용하여 새로운 상태 생성
6. (선택적으로) 영속화 성공 후 사이드 이펙트(예: 응답 전송) 실행

---

## 이펙트(Effect)와 사이드 이펙트(Side Effect)

이펙트(Effect)는 커맨드 처리 후 **무엇이 일어나야 하는지**를 정의합니다. 주요 이펙트는 다음과 같습니다.

- **persist**: 하나 또는 여러 개의 이벤트를 원자적으로(atomically) 저장합니다.
- **none**: 어떤 이벤트도 영속화하지 않습니다(읽기 전용(read-only) 커맨드에 사용).
- **unhandled**: 현재 상태에서 지원되지 않는 커맨드임을 나타냅니다.
- **stop**: 액터를 종료합니다.
- **reply**: 요청을 보낸 `ActorRef`에 응답(reply)을 전송합니다.

### 사이드 이펙트 체이닝

사이드 이펙트(side effect)는 다음 메서드를 사용해 이펙트에 연결(chain)할 수 있습니다.

- `thenRun`: 영속화 성공 후 임의의 코드를 실행
- `thenStop`: 영속화 후 액터를 종료
- `thenReply`: 영속화 후 응답 전송
- `thenUnstashAll`: 보관(stash)해 둔 커맨드를 모두 다시 처리

이 사이드 이펙트들은 영속화가 성공한 뒤 **순차적으로(sequentially)** 실행되며, **"최대 한 번(at-most-once)"** 기준으로 동작합니다. persist가 실패하면 사이드 이펙트들은 실행되지 않습니다.

> **중요**: 사이드 이펙트는 영속화가 완료된 이후에만 수행되어야 합니다. 복구(recovery) 시에는 이벤트가 다시 재생되지만, 이때 사이드 이펙트가 다시 실행되어서는 안 됩니다. 따라서 복구가 완료된 뒤에 한 번만 수행되어야 하는 사이드 이펙트는 `RecoveryCompleted` 시그널(signal)에 대한 반응으로 수행해야 합니다.

---

## 상태(State) 관리

상태(state)는 가능한 한 **불변(immutable)** 이어야 합니다. 상태가 가변(mutable)이어야 한다면, 상태 팩토리 함수(state factory function)를 받는 `withMutableState` 팩토리 메서드를 사용하여, 복구 도중 항상 새로운 인스턴스(new instance)가 생성되도록 보장해야 합니다.

모든 상태 갱신(state update)은 **오직 이벤트 핸들러에서**, 영속화된 이벤트에 기반하여 이루어집니다. **커맨드 핸들러에서는 절대 상태를 변경하지 않습니다.** 커맨드 핸들러는 오직 검증과 이펙트 생성만을 담당합니다. 이 규율을 지키면, 동일한 이벤트 로그를 재생할 때 항상 동일한 상태가 재현됨이 보장됩니다.

---

## 복구(Recovery) 과정

복구(recovery) 중에는 액터가 저널에 기록된 이벤트(journaled events)를 이벤트 핸들러를 통해 자동으로 재생(replay)하여 상태를 재구성합니다.

- 시스템은 동시(concurrent) 복구의 수를 제한합니다(기본값: 50). 이는 시스템 과부하를 막기 위함입니다.
- 복구 도중에 수신된 새로운 메시지(커맨드)는 보관(stash)되었다가, 복구가 완료된 후 처리됩니다.

문서의 표현에 따르면, "사이드 이펙트는 복구가 완료되었을 때 `RecoveryCompleted` 시그널에 대한 반응으로 수행되어야 한다(side effects should be performed once recovery has completed as a reaction to the `RecoveryCompleted` signal)."

복구는 다음과 같이 설정을 통해 조정할 수 있습니다.

- 복구를 완전히 비활성화(disable)할 수 있습니다.
- 특정 사용 사례를 위한 최적화로, 마지막 이벤트(last event)만 재생하도록 설정할 수도 있습니다.

---

## 클러스터 샤딩(Cluster Sharding)과의 통합

이벤트 소싱은 클러스터 샤딩(Cluster Sharding)과 매끄럽게 통합되어, **"각 id에 대해 오직 하나의 활성 엔티티(only one active entity for each id)"** 만 존재하도록 보장합니다.

이는 동일한 영속 액터의 여러 인스턴스에서 발생하는 이벤트가 서로 뒤섞이는(interleaving) 것을 막아주며, 재생(replay) 시 일관성(consistency)을 유지합니다. 단일 작성자 원칙(single-writer principle)을 보장하는 것이 이벤트 소싱의 핵심 전제이며, 클러스터 샤딩은 클러스터 전체에 걸쳐 이를 자동으로 강제해 줍니다.

---

## 태깅(Tagging)

이벤트는 프로젝션(projection)에서 사용하기 위해 태깅(tagging)될 수 있으며, 이때 이벤트 어댑터(event adapter)는 필요하지 않습니다.

태그(tag)는 `withTagger` 메서드를 통해 적용되며, 각 이벤트에 대해 문자열 태그의 집합(set of string tags)을 반환하는 함수를 받습니다. 태깅된 이벤트는 이후 읽기 측(read side, 예: CQRS의 쿼리 모델)에서 특정 태그 단위로 소비될 수 있습니다.

---

## 이벤트 어댑터(Event Adapter)

이벤트 어댑터(event adapter)는 저널 저장(journal storage)을 위해 이벤트를 다른 타입으로 변환합니다. `EventAdapter`를 확장하여 다음 세 가지 메서드를 구현합니다.

- **toJournal**: 이벤트를 저장 형식(storage format)으로 변환
- **fromJournal**: 저장 형식을 다시 이벤트로 변환
- **manifest**: 버전 정보(version information)를 제공

어댑터는 복구 도중 형식 변환을 처리하여 **스키마 진화(schema evolution)** 를 지원합니다. 애플리케이션의 이벤트 구조가 변경되더라도, 과거에 저장된 이벤트를 새로운 형식으로 변환하여 읽을 수 있습니다.

---

## 응답(Reply) 패턴

요청-응답(request-response) 상호작용을 위해, 커맨드는 응답을 위한 `ActorRef` 필드를 포함합니다.

- `StatusReply` 타입은 성공적인 응답(successful response) 또는 검증 오류(validation error) 중 하나를 지원합니다.
- `EventSourcedBehaviorWithEnforcedReplies`를 사용하면, 타입 시스템이 **모든 커맨드 핸들러가 `ReplyEffect`를 반환하도록 강제**합니다. 이는 응답을 깜빡 잊고 보내지 않는 실수(forgotten replies)를 방지합니다.

이 패턴을 사용하면 클라이언트는 커맨드의 처리 결과(성공/실패)를 명확하게 알 수 있으며, 검증 실패 같은 비즈니스 오류를 자연스럽게 전달할 수 있습니다.

---

## 직렬화(Serialization) 요구사항

모든 커맨드(command), 이벤트(event), 상태(state)는 Akka의 직렬화(serialization) 메커니즘을 사용해 직렬화 가능(serializable)해야 합니다.

유연성(flexibility)과 스키마 진화(schema evolution) 지원을 위해 **Jackson 직렬화(Jackson serialization)** 를 권장합니다. 이벤트는 애플리케이션이 발전하는 동안에도 계속 읽힐 수 있어야 하기 때문입니다. 한 번 저장된 이벤트는 영구적으로 남으므로, 미래의 코드 버전에서도 역직렬화(deserialize)할 수 있어야 합니다.

---

## 저널 실패와 거부(Journal Failures and Rejections)

- 기본적으로 저널(journal)이 예외(exception)를 던지면 액터는 **정지(stop)** 합니다. 이 동작은 특정 실패 시나리오에 대해 사용자 정의 `BackoffSupervisorStrategy`로 재정의(override)할 수 있습니다(예: 백오프 후 재시도).
- **저널 거부(journal rejection)**: 저널이 이벤트를 영속화하기 전에 거부하기로 결정한 경우, `PersistRejected` 시그널을 통해 알려집니다.
- **복구 실패(recovery failure)**: `RecoveryFailed` 시그널을 트리거합니다.
- **영속화 실패(persist failure)**: `PersistFailed` 시그널을 트리거합니다.

각 시그널을 `receiveSignal` 핸들러로 처리하여 적절한 대응(예: 로깅, 알림, 재시도 전략)을 구현할 수 있습니다.

---

## 원자적 쓰기(Atomic Writes)

여러 개의 이벤트를 단일 `persist` 호출로 **원자적으로(atomically)** 영속화할 수 있습니다.

시스템은 단일 persist 호출의 모든 이벤트가 저장되거나(all), 아니면 하나도 저장되지 않음(none)을 보장합니다. **결코 일부만(partial subset) 저장되는 일은 없습니다.** 이는 복구 시 불완전한 상태(incomplete recovery)가 만들어지는 것을 막아 줍니다. 따라서 논리적으로 하나로 묶여야 하는 여러 이벤트는 하나의 persist 호출로 함께 저장하는 것이 안전합니다.

---

## 고급 기능

### 상태 기반 동작 변경 (유한 상태 기계)

상태 기반 커맨드 처리(state-based command handling)는 상태 타입에 대한 패턴 매칭(pattern matching)을 사용하여, 어떤 핸들러가 적용될지 결정합니다. 이를 통해 유한 상태 기계(finite-state machine, FSM) 패턴을 구현할 수 있습니다. 현재 상태에 따라 서로 다른 커맨드 핸들러가 적용됩니다. 예를 들어 "열린 계좌(OpenedAccount)"와 "닫힌 계좌(ClosedAccount)"는 동일한 커맨드에 대해 다르게 반응할 수 있습니다.

### Behaviors.setup으로 감싸기

`EventSourcedBehavior`의 생성을 감싸면 `ActorContext`에 접근할 수 있습니다. 이를 통해 자식 액터(child actor)를 스폰(spawn)하거나 로깅(logging)에 접근하는 등의 작업이 가능합니다.

### 보관(Stashing)

커맨드는 영속화 및 복구 도중에 자동으로 보관(stash)됩니다. 추가로, `Effect.stash`와 `Effect.unstashAll`을 통해 수동 보관(manual stashing)도 가능하며, 이를 사용해 커맨드 처리를 의도적으로 지연(defer)시킬 수 있습니다.

### 스냅샷(Snapshot)

최적화를 위해, 스냅샷(snapshot)은 일정한 간격으로 상태를 포착(capture)합니다. 이렇게 하면 모든 과거 이벤트를 재생할 필요가 없어지므로 복구 시간(recovery time)이 극적으로 줄어듭니다. (자세한 내용은 아래 [스냅샷](#스냅샷snapshot) 섹션 참고)

---

## 의존성과 설정

Akka Persistence 라이브러리를 사용하려면 저널(journal)과 스냅샷 저장소(snapshot store) 플러그인을 선택해야 합니다.

- 모듈 `akka-persistence-typed`는 타입이 지정된(typed) API를 제공합니다.
- 설정(configuration)은 다음과 같은 항목을 제어합니다.
  - 동시 복구(concurrent recoveries)의 수
  - 손상 탐지(corruption detection)를 위한 재생 필터(replay filter)
  - 저널 구현체별 플러그인 매개변수(plugin-specific parameters)

---

## 스타일 가이드(Style Guide)

> 원본: https://doc.akka.io/libraries/akka-core/current/typed/persistence-style.html

이 절은 Akka에서 이벤트 소싱 엔티티를 구조화하는 모범 사례(best practice)를 다루며, 네 가지 주요 아키텍처 패턴을 중심으로 설명합니다.

### 상태 안의 이벤트 핸들러 (Event handlers in the state)

이 패턴은 이벤트 처리 로직(event handling logic)을 상태 클래스(state class) 자체 안에 배치합니다. 은행 계좌(bank account) 도메인 예제를 사용하며, 생애 주기(lifecycle)의 각 단계를 별도의 상태 클래스로 표현합니다.

- `EmptyAccount` (빈 계좌)
- `OpenedAccount` (개설된 계좌)
- `ClosedAccount` (닫힌 계좌)

각 상태 클래스는 상태 전이(transition)를 처리하는 `applyEvent` 메서드를 구현합니다. 동작 수준(behavior level)의 이벤트 핸들러는 단순히 이 메서드에 위임(delegate)합니다.

```
state.applyEvent(event)
```

이 접근법은 상태를 핵심 비즈니스 로직(core business logic)을 담은 도메인 객체(domain object)로 취급하고, 이벤트 적용 로직을 관련 상태 타입에 국한시킵니다. 또한 예상치 못한 이벤트가 적절하지 않은 상태에 도착하면 예외(exception)를 던져 잘못된 상태 전이(invalid state transition)를 방지합니다.

### 상태 안의 커맨드 핸들러 (Command handlers in the state)

앞선 패턴을 확장하여 커맨드 처리(command handling)도 상태 클래스 안의 `applyCommand` 메서드로 내장할 수 있습니다. 이는 비즈니스 로직을 더욱 중앙집중화하여, 각 상태가 들어오는 커맨드에 어떻게 반응할지를 스스로 정의하게 합니다.

동작 수준의 커맨드 핸들러는 다음처럼 됩니다.

```
state.applyCommand(cmd)
```

커맨드 처리가 비즈니스 상태 생애 주기와 밀접하게 대응될 때 이 방식이 잘 맞습니다. 서로 다른 상태가 동일한 커맨드에 자연스럽게 다르게 반응하기 때문입니다.

### 선택적 초기 상태 (Optional initial state)

별도의 빈 상태 클래스(empty state class)를 만드는 대신, 초기 상태로 `None` 또는 `null`을 사용할 수 있습니다.

- `null`을 `emptyState`로 사용할 경우, 핸들러는 상태 메서드에 위임하기 전에 `null` 여부를 확인해야 합니다.
- Scala 애플리케이션에서는 `Option[State]`를 사용하고 핸들러 계층에서 패턴 매칭(pattern matching)을 수행할 수도 있습니다.

이 접근법은 빈 상태에 의미 있는 연산이나 데이터가 없을 때 보일러플레이트(boilerplate)를 줄여 줍니다. 보통 첫 번째 커맨드가 정상 운영 전에 초기 상태를 생성합니다.

### 가변 상태 (Mutable state)

불변 상태(immutable state, 변경 시 새 인스턴스를 생성)가 일반적으로 선호되지만, 가변 상태(mutable state)도 사용 가능합니다.

가장 중요한 제약은 다음과 같습니다. **가변 상태 인스턴스를 절대 액터 간 메시지로 보내지 마십시오.** 응답(reply)과 통신(communication)에는 반드시 불변 타입(immutable type)을 사용해야 합니다.

가변 상태를 사용할 때 `emptyState` 메서드는 호출할 때마다 새로운 인스턴스(fresh instance)를 반환해야 합니다. 이는 실패 복구(failure recovery) 도중 올바른 동작을 보장하기 위함입니다.

### Java 21 기능 활용하기 (Leveraging Java 21 features)

Java 21 이상을 대상으로 하는 프로젝트는 `EventSourcedOnCommandBehavior`를 확장하여 switch 패턴 매칭(switch pattern matching)을 활용할 수 있습니다.

`sealed` 커맨드 및 이벤트 인터페이스와 결합하면, **모든 커맨드 및 이벤트 타입이 처리되었는지 컴파일 타임에 검증(compile-time verification)** 할 수 있습니다.

`onCommand`와 `onEvent` 메서드는 중첩된(nested) switch 식(expression)을 사용하여 빌더 기반(builder-based) 등록을 대체합니다. 이 접근법은 보일러플레이트를 제거하면서 컴파일 타임 완전성 검사(exhaustiveness checking)를 보장합니다. 레코드(record)와도 자연스럽게 짝을 이루어 간결한 타입 정의가 가능합니다.

---

## 스냅샷(Snapshot)

> 원본: https://doc.akka.io/libraries/akka-core/current/typed/persistence-snapshot.html

### 개요

긴 이벤트 로그(long event log)와 늘어난 복구 시간(extended recovery time)을 겪는 영속 액터는 스냅샷(snapshot)의 혜택을 받을 수 있습니다. 문서의 표현으로는, "영속 액터는 N개의 이벤트마다 또는 주어진 상태의 술어(predicate)가 충족될 때 내부 상태의 스냅샷을 저장할 수 있다(Persistent actors can save snapshots of internal state every N events or when a given predicate of the state is fulfilled)."

### 기본 스냅샷 동작

스냅샷은 주기적으로 액터 상태를 저장함으로써 복구 시간을 줄입니다. 복구 시, "영속 액터는 가장 최근에 저장된 스냅샷(latest saved snapshot)을 사용하여 상태를 초기화한다. 그 이후, 스냅샷 이후의 이벤트들이 이벤트 핸들러를 통해 재생되어 영속 액터를 현재(즉 최신) 상태로 복구한다."

즉, 복구는 (1) 최신 스냅샷으로 상태를 초기화하고, (2) 그 스냅샷 이후의 이벤트들만 재생하는 두 단계로 이루어지므로, 전체 이벤트를 처음부터 재생할 때보다 훨씬 빠릅니다.

### 스냅샷 선택 기준 (Snapshot selection criteria)

기본적으로 복구는 가장 최신 스냅샷을 사용합니다. 개발자는 이 동작을 재정의(override)할 수 있습니다.

- 스냅샷 기반 복구를 완전히 비활성화하려면 `SnapshotSelectionCriteria.none()`을 사용합니다.
- 이 접근법은 "스냅샷 직렬화 형식(snapshot serialization format)이 호환되지 않는 방식으로 변경된 경우(if snapshot serialization format has changed in an incompatible way)" 유용합니다.

### 스냅샷 저장소 설정 (Snapshot store configuration)

"스냅샷을 사용하려면 기본 스냅샷 저장소(`akka.persistence.snapshot-store.plugin`)가 설정되어 있어야 하거나, 특정 `EventSourcedBehavior`에 대해 스냅샷 저장소를 선택할 수 있다."

스냅샷 저장소를 설정하지 않고 운영할 수도 있지만, 액터가 스냅샷 작업을 시도하기 전까지 Akka는 경고(warning)를 로깅합니다.

### 스냅샷 중 커맨드 보관 (Command stashing during snapshots)

"스냅샷이 트리거되면, 들어오는 커맨드는 스냅샷이 저장될 때까지 보관(stash)된다. 이는 상태가 가변(mutable)이어도 안전하다는 것을 의미한다. 왜냐하면 상태의 직렬화와 저장이 비동기적으로(asynchronously) 수행되기 때문이다."

### snapshotWhen 과 RetentionCriteria

스냅샷을 트리거하는 방법은 크게 두 가지입니다.

1. **술어 기반(predicate-based)**: `snapshotWhen` / `shouldSnapshot`을 사용하여, 특정 상태/이벤트 조건이 충족될 때 스냅샷을 저장합니다.
2. **자동/주기 기반(automatic)**: `RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2)`처럼 일정 이벤트 수마다 스냅샷을 저장합니다.

### 스냅샷 실패 (Snapshot failures)

스냅샷 작업은 시그널(signal)을 통해 결과를 보고합니다. "스냅샷 저장은 성공하거나 실패할 수 있다 – 이 정보는 `SnapshotCompleted` 또는 `SnapshotFailed` 시그널을 통해 영속 액터에 보고된다. 스냅샷 실패(snapshot failure)는 기본적으로 로깅되지만, 액터를 정지하거나 재시작시키지는 않는다."

복구 실패는 이와 크게 다릅니다. "액터가 시작될 때 저널로부터 액터의 상태를 복구하는 데 문제가 있으면, `RecoveryFailed` 시그널이 방출되고(기본적으로 오류를 로깅), 액터는 정지된다."

### 선택적 스냅샷 (Optional snapshots)

선택적 스냅샷(optional snapshots)은 스냅샷 로드(load) 실패 시 무조건적인 액터 종료를 방지합니다. 스냅샷 저장소 설정에서 `snapshot-is-optional = true`로 활성화합니다.

이 메커니즘은 "예를 들어 역직렬화 오류(deserialization error)가 발생했을 때 스냅샷을 무시해도 괜찮은 경우에 유용하다. 스냅샷 로딩이 실패하면, 대신 모든 이벤트를 재생함으로써 복구한다."

> **경고**: 이벤트가 삭제(delete)된 경우에는 이 기능을 활성화하지 마십시오. 실패한 스냅샷 복구가 잘못된 상태(incorrect state)를 만들어 낼 수 있기 때문입니다.

### 스냅샷 삭제 (Snapshot deletion)

자동 스냅샷 삭제는 `RetentionCriteria`를 통해 저장 공간을 회수(reclaim)합니다. 예제 설정인 `RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2)`가 이 패턴을 보여 줍니다.

"스냅샷 삭제는 새로운 스냅샷을 성공적으로 저장한 후에 트리거된다." 위 예제에서는, "저장된 스냅샷의 시퀀스 번호(sequence number)에서 `keepNSnapshots * numberOfEvents`(`100 * 2`)를 뺀 값보다 작은 시퀀스 번호를 가진 스냅샷이 자동으로 삭제된다."

#### 술어 기반 vs. 자동 스냅샷

- 술어 기반 스냅샷(`snapshotWhen` / `shouldSnapshot`)은 오래된 스냅샷의 자동 삭제를 **트리거하지 않습니다.**
- 오직 `numberOfEvents`에 기반한 자동 스냅샷만이 삭제를 트리거합니다.

#### 삭제에 대한 시그널 처리

비동기 삭제는 `DeleteSnapshotsCompleted` 또는 `DeleteSnapshotsFailed` 시그널을 방출합니다. "기본적으로, 성공적인 완료는 시스템에 의해 `debug` 로그 레벨로, 실패는 `warning` 로그 레벨로 로깅된다." `receiveSignal` 핸들러를 사용해 결과에 대응할 수 있습니다.

### 이벤트 삭제 (Event deletion)

#### 이벤트 삭제의 함의

이벤트를 삭제하면 시스템의 이력(history)이 제거되며, 이는 이벤트 소싱의 원칙에 어긋납니다. "이벤트 소싱 기반 애플리케이션에서 이벤트 삭제(deleting events)는 일반적으로 전혀 사용되지 않거나, 스냅샷과 함께 사용된다."

#### 프로젝션 및 복제 이벤트 소싱과의 제약

"즉각적인 이벤트 삭제(immediate event deletion)는 프로젝션(Projection)과 함께 사용해서는 안 되며, 복제 이벤트 소싱(Replicated Event Sourcing)의 경우 스냅샷 술어 또는 보존 기준(retention criteria)을 통한 이벤트 삭제는 허용되지 않는다."

권장되는 접근법은 다음과 같습니다. "최종 삭제 이벤트(final deleted event)를 방출하고, 엔티티가 삭제되었다는 사실을 프로젝션을 통해 별도로 저장한다. 그런 다음 백그라운드 작업(background task)이 삭제된 엔티티들의 이벤트와 스냅샷을 정리(clean up)할 수 있다."

#### 술어 기반 스냅샷에서 이벤트 삭제 활성화

술어 기반 스냅샷에서는 다음으로 삭제를 활성화합니다.

- `.snapshotWhen(predicate, deleteEventsOnSnapshot = true)` (Scala)
- Java에서는 `deleteEventsOnSnapshot()` 재정의

#### 보존 기준에서 이벤트 삭제 활성화

스냅샷 기반 보존(snapshot-based retention)의 경우, `RetentionCriteria`에서 `withDeleteEventsOnSnapshot`을 호출합니다(기본적으로 비활성화). "스냅샷 기반 보존이 활성화되면, 스냅샷이 성공적으로 저장된 후, 해당 스냅샷이 보유한 데이터의 시퀀스 번호까지의 이벤트 삭제(단일 이벤트 소싱 액터가 저널링한)를 발행할 수 있다."

이벤트 삭제는 스냅샷 삭제보다 먼저 진행됩니다. 비동기 삭제는 `DeleteEventsCompleted` 또는 `DeleteEventsFailed` 시그널을 방출합니다.

#### 기술적 참고 사항

- "메시지 삭제(message deletion)는 저널의 최고 시퀀스 번호(highest sequence number)에 영향을 주지 않는다. 삭제가 일어난 후 모든 메시지가 삭제되었더라도 마찬가지다."
- "이벤트가 저장소에서 실제로 제거되는지 여부는 저널 구현체에 달려 있다."

---

## 이벤트 소싱 동작 테스트하기

> 원본: https://doc.akka.io/libraries/akka-core/current/typed/persistence-testing.html

### 개요

Akka는 `EventSourcedBehavior`에 대한 세 가지 상호 보완적인 접근법으로 종합적인 테스트 도구를 제공합니다.

1. `BehaviorTestKit`과 함께 사용하는 `UnpersistentBehavior`
2. `EventSourcedBehaviorTestKit`
3. `PersistenceTestKit`

### UnpersistentBehavior를 사용한 단위 테스트

`UnpersistentBehavior` 클래스는 `EventSourcedBehavior`를 영속화하지 않는(non-persisting) 변형으로 변환하여 단위 테스트(unit test)를 가능하게 합니다. 이벤트와 스냅샷을 저장소에 기록하는 대신, 영속화 작업에 대한 단언(assertion)을 할 수 있도록 `PersistenceProbe` 객체를 노출합니다.

#### 초기화

이벤트 소싱 액터로부터 unpersistent 동작을 생성합니다.

```
UnpersistentBehavior.fromEventSourced(
  AccountEntity("1", PersistenceId("Account", "1"))
)
```

#### 임의의 상태로 초기화

이 동작은 미리 정해진 상태(predetermined state)로 시작할 수 있으며, 이벤트 핸들러를 재사용하여 이전 커맨드 실행을 시뮬레이션합니다.

```
UnpersistentBehavior.fromEventSourced(
  AccountEntity("1", PersistenceId("Account", "1")),
  Some(AccountEntity.EmptyAccount.applyEvent(...) -> 1L)
)
```

#### BehaviorTestKit과 함께 테스트

`UnpersistentBehavior`는 동기적(synchronous) 테스트를 위해 `BehaviorTestKit`과 직접 통합됩니다. 커맨드는 호출 스레드(calling thread)에서 실행되어, 단언이 실행되기 전에 완전한 처리가 끝나도록 보장합니다. 내부 상태는 동작의 응답 및 영속화된 이벤트/스냅샷을 통해서만 드러납니다.

#### 주요 장점

- 설정(configuration)이 필요 없음
- 테스트 스레드에서 동기 실행
- 이벤트/스냅샷 검증을 위한 영속화 프로브(persistence probe) 접근
- 참고: 직렬화(serialization)는 검증하지 않으므로, 직렬화 테스트는 독립적으로 수행하는 것을 권장

### EventSourcedBehaviorTestKit을 사용한 단위 테스트

`EventSourcedBehaviorTestKit`은 종합적인 단위 테스트 기능을 제공하며, 한 번에 하나의 커맨드(one command at a time)를 처리하고, 방출된 이벤트와 결과 상태를 담아 동기적으로 반환되는 결과(result)에 대해 단언합니다.

#### 설정

`ActorSystem`을 `EventSourcedBehaviorTestKit.config`로 설정하여 인메모리(in-memory) 저널 및 스냅샷 저장소를 활성화합니다.

```
class AccountExampleDocSpec extends 
  ScalaTestWithActorTestKit(EventSourcedBehaviorTestKit.config)
```

#### 테스트킷 초기화

```
EventSourcedBehaviorTestKit.create(
  testKit.system(), 
  AccountEntity.create("1", PersistenceId.of("Account", "1"))
)
```

#### 테스트 구조

`runCommand()`로 커맨드를 실행하고, 응답(reply), 영속화된 이벤트, 새로운 상태를 담은 결과를 받습니다.

```
val result = eventSourcedTestKit.runCommand[StatusReply[Done]](
  AccountEntity.CreateAccount(_)
)
result.reply shouldBe StatusReply.Ack
result.event shouldBe AccountEntity.AccountCreated
result.stateOfType[AccountEntity.OpenedAccount].balance shouldBe 0
```

#### 직렬화 검증

직렬화는 왕복(roundtrip) 검사를 통해 자동으로 검증됩니다. 테스트킷 생성 시 `SerializationSettings`로 커스터마이즈할 수 있습니다. 기본적으로 동등성(equality) 검증은 비활성화되어 있으며, 커맨드/이벤트/상태가 `equals`를 구현하거나 케이스 클래스(case class)를 사용하는 경우 `verifyEquality`로 활성화할 수 있습니다.

#### 복구 테스트

`restart()` 메서드를 사용해 복구 동작을 테스트합니다. 재시작된 동작은 이전 커맨드로 저장된 스냅샷과 이벤트로부터 복구됩니다. 내부의 `PersistenceTestKit`에 접근하여 저장소를 사전에 채우거나(prepopulate) 실패를 시뮬레이션할 수 있습니다.

#### 상태 초기화

테스트 사이에 `clear()`를 호출하여 동작의 저장소를 초기화합니다.

```
override protected def beforeEach(): Unit = {
  super.beforeEach()
  eventSourcedTestKit.clear()
}
```

### Persistence TestKit

`PersistenceTestKit`은 영속화된 이벤트를 검증하고, 저장 작업을 에뮬레이션(emulation)하며, 예외(exception)를 시뮬레이션할 수 있게 해 줍니다. 이벤트와 스냅샷을 다루는 두 개의 병렬 클래스가 있습니다.

- 이벤트용 `PersistenceTestKit`
- 스냅샷용 `SnapshotTestKit`

#### 플러그인 설정

해당 플러그인이 `ActorSystem`에 설정되어야 합니다.

```
val system = ActorSystem(
  behavior,
  "test-system",
  PersistenceTestKitPlugin.config.withFallback(yourConfiguration)
)
val testKit = PersistenceTestKit(system)
```

#### 핵심 API 메서드

이 테스트킷은 다음을 수행하는 메서드를 제공합니다.

- 다음에 영속화된 이벤트/스냅샷이 기대값과 일치하는지 단언
- 영속화된 항목들의 시퀀스를 읽기
- 어떤 항목도 영속화되지 않았는지 검증
- 이후 작업에서 기본 저장소 예외(default storage exception)를 트리거
- 모든 영속화된 데이터 초기화
- 이벤트 거부(reject) (스냅샷은 거부를 지원하지 않음)
- 작업 동작을 제어하는 사용자 정의 저장소 정책(custom storage policy) 정의
- 모든 영속화된 항목 조회
- 복구 테스트를 위한 저장소 사전 채우기(prepopulate)

#### 사용자 정의 저장소 정책

`ProcessingPolicy`를 구현하여 저장소 동작을 제어합니다. `tryProcess()` 메서드는 영속화 ID와 저장 작업(storage operation)을 받아 다음 세 가지 결과 중 하나를 반환합니다.

- `ProcessingSuccess`: 작업이 정상적으로 완료됨
- `StorageFailure`: 저장소 예외를 에뮬레이션
- `Reject`: 거부를 에뮬레이션 (이벤트에만 해당)

##### 이벤트 저장 작업

- `ReadEvents`: 저장소에서 이벤트 조회
- `WriteEvents`: 저장소에 이벤트 쓰기
- `DeleteEvents`: 저장소에서 이벤트 제거
- `ReadSeqNum`: 특정 영속화 ID의 최고 시퀀스 번호 가져오기

##### 스냅샷 저장 작업

- `ReadSnapshot`: 저장소에서 스냅샷 조회
- `WriteSnapshot`: 저장소에 스냅샷 쓰기
- `DeleteSnapshotsByCriteria`: 기준에 맞는 스냅샷 제거
- `DeleteSnapshotByMeta`: 메타데이터로 특정 스냅샷 제거

##### 정책 구현 예제

사용자 정의 정책은 내부 상태를 유지하고 작업 타입에 대해 패턴 매칭합니다.

```
class SampleEventStoragePolicy extends EventStorage.JournalPolicies.PolicyType {
  var count = 1
  override def tryProcess(persistenceId: String, processingUnit: JournalOperation): 
    ProcessingResult =
    if (count < 10) {
      count += 1
      processingUnit match {
        case ReadEvents(batch) if batch.nonEmpty => ProcessingSuccess
        case WriteEvents(batch) if batch.size > 1 => ProcessingSuccess
        case ReadSeqNum => StorageFailure()
        case DeleteEvents(_) => Reject()
        case _ => StorageFailure()
      }
    } else {
      ProcessingSuccess
    }
}
```

#### 설정 참조

Persistence 테스트킷은 `akka.persistence.testkit` 하위의 참조 설정(reference configuration)에 문서화된 여러 설정 속성을 지원합니다.

### 통합 테스트 (Integration testing)

`EventSourcedBehavior` 액터는 다른 액터와 함께 테스트하기 위해 `ActorTestKit`과 통합됩니다. `PersistenceTestKit`의 인메모리 저널 및 스냅샷 저장소는 단일 `ActorSystem` 내에서의 통합 테스트(예: 클러스터 샤딩 시나리오)를 지원합니다.

다중 노드(multi-node) 클러스터 테스트의 경우, 실제 데이터베이스 백엔드가 필요합니다. Persistence Plugin Proxy를 사용할 수도 있지만, 운영 환경에 가까운(production-realistic) 테스트는 일반적으로 실제 데이터베이스를 사용합니다.

### 플러그인 초기화

일부 영속성 플러그인은 테이블 생성(table creation)을 필요로 하는데, 이는 여러 `ActorSystem` 인스턴스에서 동시에 일어날 수 없습니다. `PersistenceInit` 유틸리티를 사용하여 초기화를 조율합니다.

```
val done: Future[Done] = 
  PersistenceInit.initializeDefaultPlugins(system, timeout)
Await.result(done, timeout)
```

이는 테스트 중 클러스터 노드 전반에 걸쳐 플러그인 설정을 동기화합니다.

---

## 복제 이벤트 소싱(Replicated Event Sourcing)

> 원본: https://doc.akka.io/libraries/akka-core/current/typed/replicated-eventsourcing.html

### 핵심 개념

복제 이벤트 소싱(Replicated Event Sourcing)은 표준 `EventSourcedBehavior`를 확장하여, 단일 작성자 원칙(single-writer principle) 대신 **각 엔티티의 여러 활성 복제본(multiple active replicas)** 을 허용합니다. 이를 통해 여러 위치에 걸쳐 "액티브-액티브(active-active)" 및 "핫 스탠바이(hot standby)" 배포 패턴을 유지하면서 최종 일관성(eventual consistency)을 달성할 수 있습니다.

### 단일 작성자 원칙의 완화

전통적인 `EventSourcedBehavior`는 동시 작성자(concurrent writer)로부터의 이벤트가 뒤섞여 상태 재구성을 손상시키는 것을 막기 위해, `persistenceId`당 하나의 활성 인스턴스로 제한합니다. 복제 이벤트 소싱은 이 제약을 포기합니다. "이벤트는 모든 복제본(all replicas)으로 자동으로 복제된다." 이를 통해 클라우드 리전(region), 데이터 센터, 가용 영역(availability zone) 전반에 걸친 중복성(redundancy)을 가능하게 합니다.

이는 다음과 같은 배포상의 이점을 제공합니다.

- 한 위치가 실패했을 때의 장애 내성(fault tolerance)
- 요청을 로컬에서 처리함으로써 지연 시간(latency) 감소
- 여러 위치에서의 업데이트
- 여러 서버에 걸친 부하 분산(load distribution)

**핵심적인 트레이드오프**: "단일 작성자 보장(single-writer guarantee)이 유지되지 않기" 때문에, 이벤트 핸들러는 동시 이벤트(concurrent event)를 처리할 수 있어야 합니다. 네트워크 분할(partition)이나 장애 상황에서 복제가 지연될 수 있으므로, 상태는 "최종 일관성(eventually consistent)"을 갖게 됩니다.

### API 구조

두 가지 주요 추상화(abstraction)가 존재합니다.

- `ReplicatedEventSourcedBehavior`
- `ReplicatedEventSourcedOnCommandBehavior`

이들은 표준 `EventSourcedBehavior` API를 그대로 반영합니다. 설정에는 `ReplicatedEventSourcing.commonJournalConfig()` 팩토리 메서드를 사용하며, 다음을 받습니다.

- `ReplicationId`: 엔티티 비즈니스 키(business key), 복제본 식별자(replica identifier), 그리고 선택적인 도메인 구분 정보를 포함
- 모든 `ReplicaId` 인스턴스의 집합
- 저널 식별자(journal identifier)

설정 예제:

```
ReplicatedEventSourcing.commonJournalConfig(
  new ReplicationId("movies", entityId, replicaId),
  allReplicas,
  journalId,
  behaviorFactory
)
```

### 이벤트 복제 전송 (Event replication transports)

**gRPC 전송 (권장)**: Akka 2.8.0부터 gRPC가 표준 복제 메커니즘을 제공합니다. 이는 Akka Projection gRPC 모듈을 통해 구현되며, Akka Distributed Cluster Guide에 종합적인 문서와 예제가 있습니다.

**직접 데이터베이스 접근 (Direct database access)**: 복제본들이 서로의 데이터베이스에 직접 연결하여 이벤트를 소비하는 대안적 접근법으로, 별도의 문서에 자세히 설명되어 있습니다.

### 충돌 해결 전략 (Conflict resolution strategies)

#### 충돌 없는 복제 데이터 타입 (CRDTs)

분산 일관성(distributed consistency)을 위한 기반 접근법입니다. 연산 기반(operation-based) CRDT가 복제 이벤트 소싱에 적합한데, "이벤트가 곧 연산(operation)을 표현"하기 때문입니다. 교환 법칙(commutativity) 요구사항은 "동일한 이벤트를 어떤 순서로 적용하더라도 항상 동일한 최종 상태를 만들어 내야 함"을 의미합니다.

내장 CRDT 구현체:

- **`LwwTime`**: 타임스탬프와 복제본 식별자를 사용하는 최종 작성자 우선(last-writer-wins)
- **`Counter`**: 동시 증가(concurrent increment)를 지원하는 분산 카운터
- **`ORSet`**: 관찰-제거 집합(Observed-Remove Set), 동시 추가 우선(concurrent-add-wins) 의미론으로 추가/제거 허용

영화 시청 목록(watchlist)에 `ORSet`을 사용하는 예제:

```
ReplicatedEventSourcing.commonJournalConfig(...) { ctx =>
  EventSourcedBehavior[Command, ORSet.DeltaOp, ORSet[String]](
    ctx.persistenceId,
    ORSet.empty(ctx.replicaId),
    commandHandler,
    eventHandler)
}
```

이벤트 핸들러는 연산을 적용합니다: `state.applyOperation(event)`.

#### 최종 작성자 우선 (Last-Writer-Wins, LWW)

타임스탬프만으로 충돌 해결이 충분할 때, LWW는 가장 높은 타임스탬프를 가진 이벤트를 사용합니다. 이 접근법은 "동기화된 시계(synchronized clock)에 의존"하며, "시계 오차(clock skew) 범위 내의 동시 업데이트에서 값의 선택이 중요하지 않은 경우"에만 동작합니다.

`LwwTime` 사용:

- 영속화 시점의 타임스탬프와 원본 복제본 식별자를 포함
- 비교 시 가장 높은 타임스탬프를 우선하며, 동률(tie)일 때는 복제본 ID를 알파벳/숫자 순으로 비교하여 결정
- 단조 증가하는(monotonically advancing) 타임스탬프를 위해 `increase()` 메서드 사용

**핵심적인 한계**: 부분 상태 업데이트(partial state update)는 복제본을 발산(diverge)시킬 수 있습니다. 만약 별개의 필드가 독립적인 타임스탬프를 갖는다면(예: `AuthorChanged`와 `TitleChanged` 이벤트), 동시 업데이트가 복제본 전반에 걸쳐 일관되지 않게 적용될 수 있습니다. 해결책은 다음과 같습니다.

1. 각 이벤트에 전체 상태(full state)를 포함시키기
2. 여러 개의 타임스탬프 사용하기 — 독립적으로 업데이트 가능한 필드 그룹마다 하나씩

예제:

```
PostAdded 이벤트는 다음을 통해 타임스탬프를 포함:
state.contentTimestamp.increase(
  replicationContext.currentTimeMillis(),
  replicationContext.replicaId())
```

이벤트 핸들러는 조건부로 적용합니다.

```
if (timestamp.isAfter(state.contentTimestamp)) {
  // 이벤트를 적용하고 타임스탬프 갱신
} else {
  // 오래된 이벤트는 폐기
}
```

### 인과성 및 동시성 탐지 (Causality and concurrent detection)

#### 버전 벡터 (Version vectors)

복제 이벤트 소싱은 이벤트 간의 인과 관계(causal relationship)를 추적하기 위해 버전 벡터(version vector)를 사용합니다. 각 복제본은 논리적 시계(logical clock, 카운터)를 유지하며, 이벤트를 영속화할 때 이를 증가시킵니다. 벡터는 이벤트와 함께 전파되며, 복제된 이벤트를 소비할 때 버전 벡터가 로컬에서 병합(merge)됩니다.

버전 벡터 비교는 다음을 판별합니다.

- **SAME**: 모든 위치가 동일
- **BEFORE**: 모든 위치가 ≤이고 일부가 엄격하게 <
- **AFTER**: 모든 위치가 ≥이고 일부가 엄격하게 >
- **CONCURRENT**: before/after 관계가 성립하지 않음 (동시 발생)

#### 인과적 전달 순서 (Causal delivery order)

"한 복제본에서 영속화된 이벤트는 다른 복제본에서 동일한 순서로 읽힌다." 그러나 "동시 이벤트(concurrent event)의 순서는 정의되지 않으며", 이는 CRDT를 사용할 때 허용 가능해야 합니다. 인과성 체인(causality chain) 예제:

```
DC-1: e1 쓰기
DC-2: e1 읽기, e2 쓰기
DC-1: e2 읽기, e3 쓰기
```

이는 모든 복제본에 걸쳐 보편적인 순서 e1 → e2 → e3를 만들어 냅니다.

동시 이벤트가 있는 경우(예: e3와 e2가 서로를 알지 못한 채 생성된 경우), 서로 다른 복제본이 서로 다른 순서를 관찰할 수 있지만, 교환 법칙(commutativity) 덕분에 둘 다 연산을 동일하게 적용합니다.

### ReplicationContext

커맨드 핸들러와 이벤트 핸들러에서 사용할 수 있으며, 다음을 제공합니다.

- 현재 복제본 식별자
- 엔티티 비즈니스 식별자
- 이벤트를 처리하는 원본 복제본(origin replica)
- 복구 상태 플래그(recovery status flag)
- 밀리초 단위의 현재 시간

이를 통해 선택적인 사이드 이펙트를 구현할 수 있습니다. 특정 복제본에서만, 또는 완전한 복제가 이루어진 후에만 동작을 트리거하는 식입니다.

### 사이드 이펙트 (Side effects)

이벤트 핸들러에서의 직접적인 사이드 이펙트는 "일반적으로 권장되지 않습니다." 이벤트 핸들러는 재생(replay) 시와 복제된 이벤트를 소비할 때도 호출되므로, 의도치 않은 재실행(re-execution)을 유발할 수 있기 때문입니다.

적절한 사이드 이펙트 사용 사례:

- 단 하나의 복제본에서만 동작을 트리거
- 모든 복제본이 이벤트를 본 후 한 번만 실행
- 복제된 이벤트의 도착 처리
- 탐지된 충돌(conflict)에 대한 응답

`ReplicationContext` 필드가 이러한 패턴을 가능하게 합니다. 예를 들어, 최종 동작(예: 경매(auction) 정산)을 위해 하나의 복제본을 지정하면 중복 실행(duplicate execution)을 방지할 수 있습니다. Auction 예제가 이러한 기법을 보여 줍니다.

### 프로젝션 통합 (Projection integration)

Akka Projections는 복제된 엔티티와 함께 동작하지만, "프로젝션은 모든 복제본으로부터 모든 이벤트(all events from all replicas)를 받게 되며", 복제본마다 도착 순서가 다를 수 있다는 점을 고려해야 합니다.

두 가지 패턴:

1. **모든 이벤트 처리**: 복제본 내의 재정렬(reordering)을 수용
2. **로컬 복제본으로 필터링**: `ReplicationContext`의 원본 정보를 사용하여, 태그나 이벤트 메타데이터로 저장한 뒤, 프로젝션이 로컬 복제본 이벤트만 처리하도록 함

### 저널 및 스냅샷 지원

저널은 `PersistentRepr`의 `metadata` 필드를 통해 `withMetadata()`를 사용하여 이벤트 메타데이터를 저장하고 읽을 수 있어야 합니다. 스냅샷 저장소도 마찬가지로 저장 및 조회 시 메타데이터를 처리해야 합니다.

Persistence TCK는 검증을 위한 `supportsMetadata` 기능 플래그를 제공합니다. 현재 Akka Persistence R2DBC(버전 1.1.0 이상)가 gRPC를 통한 복제 이벤트 소싱을 지원합니다.

### 비복제에서의 마이그레이션 (Migration from non-replicated)

기존 `EventSourcedBehavior` 엔티티를 `ReplicatedEventSourcedBehavior`로 변환하면 이벤트가 보존되고 동일하게 저장되며, 메타데이터가 없으면 자동으로 채워집니다(auto-populated). 원본 복제본은 동일한 `PersistenceId`와 이벤트 시퀀스를 유지하기 위해 빈 `ReplicaId`(empty `ReplicaId`)를 사용해야 합니다.

핵심 고려사항: 이제 동시 업데이트(concurrent update)가 가능해지므로, 이벤트 핸들러 로직은 "충돌하는 업데이트 해결(resolving conflicting updates)" 의미론에 맞게 조정되어야 하며, 이를 우아하게(gracefully) 처리해야 합니다.

---

## CQRS

> 원본: https://doc.akka.io/libraries/akka-core/current/typed/cqrs.html

### 개요

CQRS(Command Query Responsibility Segregation, 커맨드 쿼리 책임 분리)는 데이터를 변경하는 작업(커맨드, command)과 데이터를 읽는 작업(쿼리, query)을 서로 다른 모델로 분리하는 아키텍처 패턴입니다. 공식 문서의 핵심 메시지는 다음과 같습니다.

> "`EventSourcedBehavior`는 Akka Projections와 함께 사용되어 CQRS(Command Query Responsibility Segregation)를 구현할 수 있다(EventSourcedBehavior s along with Akka Projections can be used to implement Command Query Responsibility Segregation (CQRS))."

문서는 이벤트 소싱과 프로젝션을 결합하는 방법을 설명하는 종합적인 튜토리얼이 **"Microservices with Akka tutorial"** 에 존재한다고 안내합니다.

### Akka에서의 CQRS 구성

Akka에서 CQRS는 일반적으로 다음과 같이 구성됩니다.

- **쓰기 측 (Write side)**: `EventSourcedBehavior`로 구현됩니다. 커맨드를 받아 검증하고, 이벤트를 영속화합니다. 이 측이 진실의 원천(source of truth)인 이벤트 로그를 만들어 냅니다. 쓰기 모델은 트랜잭션 일관성(transactional consistency)과 비즈니스 규칙 검증에 최적화됩니다.

- **읽기 측 (Read side)**: **Akka Projections**로 구현됩니다. 쓰기 측이 만들어 낸 이벤트 스트림을 소비하여, 쿼리에 최적화된 별도의 읽기 모델(read model, 예: 데이터베이스 뷰, 검색 인덱스 등)을 구축합니다. 읽기 모델은 다양한 쿼리 패턴에 맞게 자유롭게 비정규화(denormalize)될 수 있습니다.

### 이벤트 태깅 (Tagging events)

읽기 측 프로젝션이 이벤트를 효율적으로 소비하려면, 쓰기 측에서 이벤트에 태그(tag)를 붙입니다. `EventSourcedBehavior`의 `withTagger`를 사용해 각 이벤트에 태그를 부여하면, 프로젝션은 해당 태그 단위로 이벤트를 읽어 처리할 수 있습니다. 일반적으로 이벤트를 여러 개의 태그(슬라이스, slice)로 나누어, 여러 프로젝션 인스턴스가 병렬로(parallel) 이벤트를 처리하도록 하여 처리량(throughput)을 높입니다.

### 샤딩 (Sharding)

쓰기 측은 클러스터 샤딩(Cluster Sharding)과 결합되어, 각 엔티티 id에 대해 단일 활성 인스턴스만 존재하도록 보장합니다. 이는 이벤트 로그의 일관성을 유지하고, 부하를 클러스터 전반에 분산시키는 데 핵심적입니다.

### 정리

CQRS의 본질은 "쓰기에 최적화된 모델"과 "읽기에 최적화된 모델"을 분리하고, 이벤트 소싱을 통해 이 둘을 비동기적으로 연결하는 것입니다. 결과적으로 쓰기 측은 일관성과 무결성에, 읽기 측은 다양한 조회 성능에 각각 집중할 수 있게 됩니다. 보다 상세한 구현 예제는 Akka의 "Microservices with Akka" 튜토리얼과 Akka Projections 라이브러리 문서를 참고하는 것이 좋습니다.

---

## 참고 자료

- [Akka 공식 문서](https://doc.akka.io/libraries/akka-core/current/)
- [Event Sourcing (원본)](https://doc.akka.io/libraries/akka-core/current/typed/persistence.html)
- [Style guide for EventSourcedBehaviors](https://doc.akka.io/libraries/akka-core/current/typed/persistence-style.html)
- [Snapshotting](https://doc.akka.io/libraries/akka-core/current/typed/persistence-snapshot.html)
- [Testing](https://doc.akka.io/libraries/akka-core/current/typed/persistence-testing.html)
- [Replicated Event Sourcing](https://doc.akka.io/libraries/akka-core/current/typed/replicated-eventsourcing.html)
- [CQRS](https://doc.akka.io/libraries/akka-core/current/typed/cqrs.html)

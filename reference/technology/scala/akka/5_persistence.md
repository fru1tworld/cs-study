# Akka 영속성

## Akka 영속성: 이벤트 소싱

> 원본: https://doc.akka.io/libraries/akka-core/current/typed/persistence.html

---

### 목차

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

### 개요: 이벤트 소싱이란

- Akka의 이벤트 소싱(event sourcing): 상태를 가진 액터(stateful actor)가 상태 자체를 저장하는 대신, 상태 변화를 나타내는 이벤트(event)만 저장하여 자신의 상태를 영속화(persist)
  - 공식 문서 표현: "액터에 의해 영속화되는 것은 이벤트뿐이며, 액터의 실제 상태는 저장되지 않는다(only the events that are persisted by the actor are stored, not the actual state of the actor)"
- 이벤트는 저장소(storage)에 추가(append)될 뿐 절대 변경(mutate)되지 않음
  - 추가 전용(append-only) 특성 → 높은 트랜잭션 처리율(high transaction rate)·효율적인 복제(replication) 가능
  - 액터는 저장된 이벤트를 재생(replay)함으로써 상태를 재구성(rebuild)하여 복구(recover)

이벤트 소싱의 핵심 아이디어:

- 상태(state)는 현재 시점의 스냅샷에 불과, 진실의 원천(source of truth)은 시간 순서대로 누적된 이벤트의 로그(event log)
- 어떤 시점의 상태든 처음부터 이벤트를 순서대로 재생함으로써 재현 가능
- 이벤트는 이미 일어난 사실(fact)을 표현 → 과거형으로 명명(예: `AccountCreated`, `MoneyDeposited`) → 이미 발생한 사실이므로 재생 도중 실패 불가

---

### EventSourcedBehavior의 구성 요소

`EventSourcedBehavior` 정의에 필요한 네 가지 필수 구성 요소:

1. PersistenceId: 백엔드 저널(journal) 및 스냅샷 저장소(snapshot store)에서 영속 액터(persistent actor)를 식별하는 안정적이고 고유한 식별자(stable unique identifier)
   - 액터의 생애 동안 변하지 않아야 함
2. EmptyState: 엔티티(entity)가 처음 생성되었을 때의 초기 상태(initial state)
3. CommandHandler: 들어오는 커맨드(command)를 처리하고 이펙트(Effect)를 생성(예: 이벤트를 영속화하기)
4. EventHandler: 이벤트가 영속화된 후 상태를 갱신

- 이 동작(behavior)은 Command 타입에 대해 엄격하게 타입이 지정(strictly typed)됨 → Akka의 타입 시스템(type system)이 강제

`EventSourcedBehavior`를 구성하는 의사 코드(pseudo-code)의 형태:

```
EventSourcedBehavior[Command, Event, State](
  persistenceId = PersistenceId.ofUniqueId("abc"),
  emptyState = State(...),
  commandHandler = (state, command) => { ... },   // Effect 반환
  eventHandler = (state, event) => { ... }          // 새로운 State 반환
)
```

- 커맨드 핸들러(command handler): `(State, Command)`를 입력으로 받아 `Effect`를 반환 → "이 커맨드에 반응하여 무엇을 할 것인가"를 결정
- 이벤트 핸들러(event handler): `(State, Event)`를 입력으로 받아 새로운 `State`를 반환 → "이 이벤트가 영속화되었을 때 상태를 어떻게 바꿀 것인가"를 결정

---

### 커맨드와 이벤트의 흐름

- 커맨드(command): 영속화되지 않는(non-persistent) 메시지 → 현재 상태(current state)를 기준으로 검증(validate)
- 검증 성공 → 커맨드의 효과(effect)를 나타내는 이벤트(event) 생성
- 이벤트는 영속화된 뒤 액터의 상태를 변경하기 위해 적용(apply)

가장 중요한 원칙: "이벤트는 영속 액터에 재생될 때 실패할 수 없으며, 이는 커맨드와는 대조적이다(events cannot fail when being replayed to a persistent actor, in contrast to commands)."

- 커맨드: 거부될 수 있고(reject), 검증에 실패할 수 있음 → 외부에서 들어온 "요청"이며, 비즈니스 규칙에 의해 받아들여지거나 거절됨
- 이벤트: 이미 검증을 통과하여 발생이 확정된 "사실" → 재생 시 검증 로직 재실행 없이 무조건 적용 → 이벤트 핸들러에는 실패 가능한 검증 로직을 두면 안 됨

전형적인 처리 순서:

1. 커맨드 도착
2. 커맨드 핸들러가 현재 상태를 바탕으로 커맨드를 검증
3. 검증 통과 시, 하나 이상의 이벤트를 영속화(persist)하는 이펙트 반환
4. 이벤트가 저널에 성공적으로 기록됨
5. 이벤트 핸들러가 이벤트를 상태에 적용하여 새로운 상태 생성
6. (선택적으로) 영속화 성공 후 사이드 이펙트(예: 응답 전송) 실행

---

### 이펙트(Effect)와 사이드 이펙트(Side Effect)

이펙트(Effect): 커맨드 처리 후 무엇이 일어나야 하는지를 정의. 주요 이펙트:

- persist: 하나 또는 여러 개의 이벤트를 원자적으로(atomically) 저장
- none: 어떤 이벤트도 영속화하지 않음(읽기 전용(read-only) 커맨드에 사용)
- unhandled: 현재 상태에서 지원되지 않는 커맨드임을 나타냄
- stop: 액터를 종료
- reply: 요청을 보낸 `ActorRef`에 응답(reply) 전송

#### 사이드 이펙트 체이닝

사이드 이펙트(side effect)는 다음 메서드로 이펙트에 연결(chain) 가능:

- `thenRun`: 영속화 성공 후 임의의 코드를 실행
- `thenStop`: 영속화 후 액터를 종료
- `thenReply`: 영속화 후 응답 전송
- `thenUnstashAll`: 보관(stash)해 둔 커맨드를 모두 다시 처리

- 이 사이드 이펙트들은 영속화가 성공한 뒤 순차적으로(sequentially) 실행 → "최대 한 번(at-most-once)" 기준으로 동작
- persist가 실패하면 사이드 이펙트들은 실행되지 않음

> 중요: 사이드 이펙트는 영속화가 완료된 이후에만 수행되어야 함. 복구(recovery) 시에는 이벤트가 다시 재생되지만, 이때 사이드 이펙트가 다시 실행되어서는 안 됨 → 복구가 완료된 뒤에 한 번만 수행되어야 하는 사이드 이펙트는 `RecoveryCompleted` 시그널(signal)에 대한 반응으로 수행

---

### 상태(State) 관리

- 상태(state)는 가능한 한 불변(immutable)이어야 함
  - 상태가 가변(mutable)이어야 한다면, 상태 팩토리 함수(state factory function)를 받는 `withMutableState` 팩토리 메서드를 사용 → 복구 도중 항상 새로운 인스턴스(new instance) 생성 보장
- 모든 상태 갱신(state update)은 오직 이벤트 핸들러에서, 영속화된 이벤트에 기반하여 이루어짐
  - 커맨드 핸들러에서는 절대 상태를 변경하지 않음 → 커맨드 핸들러는 오직 검증과 이펙트 생성만 담당
  - 이 규율 준수 → 동일한 이벤트 로그를 재생할 때 항상 동일한 상태가 재현됨 보장

---

### 복구(Recovery) 과정

- 복구(recovery) 중에는 액터가 저널에 기록된 이벤트(journaled events)를 이벤트 핸들러를 통해 자동으로 재생(replay)하여 상태를 재구성
- 시스템은 동시(concurrent) 복구의 수를 제한(기본값: 50) → 시스템 과부하 방지
- 복구 도중에 수신된 새로운 메시지(커맨드)는 보관(stash)되었다가, 복구가 완료된 후 처리

문서 표현: "사이드 이펙트는 복구가 완료되었을 때 `RecoveryCompleted` 시그널에 대한 반응으로 수행되어야 한다(side effects should be performed once recovery has completed as a reaction to the `RecoveryCompleted` signal)."

복구 조정 설정:

- 복구를 완전히 비활성화(disable) 가능
- 특정 사용 사례를 위한 최적화로, 마지막 이벤트(last event)만 재생하도록 설정 가능

---

### 클러스터 샤딩(Cluster Sharding)과의 통합

- 이벤트 소싱은 클러스터 샤딩(Cluster Sharding)과 매끄럽게 통합 → "각 id에 대해 오직 하나의 활성 엔티티(only one active entity for each id)"만 존재하도록 보장
- 동일한 영속 액터의 여러 인스턴스에서 발생하는 이벤트가 서로 뒤섞이는(interleaving) 것을 방지 → 재생(replay) 시 일관성(consistency) 유지
- 단일 작성자 원칙(single-writer principle) 보장이 이벤트 소싱의 핵심 전제 → 클러스터 샤딩이 클러스터 전체에 걸쳐 이를 자동으로 강제

---

### 태깅(Tagging)

- 이벤트는 프로젝션(projection)에서 사용하기 위해 태깅(tagging) 가능, 이때 이벤트 어댑터(event adapter)는 불필요
- 태그(tag)는 `withTagger` 메서드를 통해 적용 → 각 이벤트에 대해 문자열 태그의 집합(set of string tags)을 반환하는 함수를 받음
- 태깅된 이벤트는 이후 읽기 측(read side, 예: CQRS의 쿼리 모델)에서 특정 태그 단위로 소비 가능

---

### 이벤트 어댑터(Event Adapter)

- 이벤트 어댑터(event adapter): 저널 저장(journal storage)을 위해 이벤트를 다른 타입으로 변환. `EventAdapter`를 확장하여 다음 세 가지 메서드를 구현
  - toJournal: 이벤트를 저장 형식(storage format)으로 변환
  - fromJournal: 저장 형식을 다시 이벤트로 변환
  - manifest: 버전 정보(version information) 제공
- 어댑터는 복구 도중 형식 변환을 처리하여 스키마 진화(schema evolution) 지원
  - 애플리케이션의 이벤트 구조가 변경되더라도, 과거에 저장된 이벤트를 새로운 형식으로 변환하여 읽을 수 있음

---

### 응답(Reply) 패턴

- 요청-응답(request-response) 상호작용을 위해, 커맨드는 응답을 위한 `ActorRef` 필드를 포함
- `StatusReply` 타입: 성공적인 응답(successful response) 또는 검증 오류(validation error) 중 하나를 지원
- `EventSourcedBehaviorWithEnforcedReplies` 사용 시, 타입 시스템이 모든 커맨드 핸들러가 `ReplyEffect`를 반환하도록 강제 → 응답을 깜빡 잊고 보내지 않는 실수(forgotten replies) 방지

이 패턴 사용 → 클라이언트는 커맨드의 처리 결과(성공/실패)를 명확하게 파악, 검증 실패 같은 비즈니스 오류를 자연스럽게 전달 가능

---

### 직렬화(Serialization) 요구사항

- 모든 커맨드(command), 이벤트(event), 상태(state)는 Akka의 직렬화(serialization) 메커니즘을 사용해 직렬화 가능(serializable)해야 함
- 유연성(flexibility)과 스키마 진화(schema evolution) 지원을 위해 Jackson 직렬화(Jackson serialization) 권장
  - 이벤트는 애플리케이션이 발전하는 동안에도 계속 읽힐 수 있어야 함 → 한 번 저장된 이벤트는 영구적으로 남으므로, 미래의 코드 버전에서도 역직렬화(deserialize) 가능해야 함

---

### 저널 실패와 거부(Journal Failures and Rejections)

- 기본적으로 저널(journal)이 예외(exception)를 던지면 액터는 정지(stop)
  - 특정 실패 시나리오에 대해 사용자 정의 `BackoffSupervisorStrategy`로 재정의(override) 가능(예: 백오프 후 재시도)
- 저널 거부(journal rejection): 저널이 이벤트를 영속화하기 전에 거부하기로 결정한 경우 → `PersistRejected` 시그널로 알려짐
- 복구 실패(recovery failure): `RecoveryFailed` 시그널을 트리거
- 영속화 실패(persist failure): `PersistFailed` 시그널을 트리거

각 시그널을 `receiveSignal` 핸들러로 처리하여 적절한 대응(예: 로깅, 알림, 재시도 전략) 구현 가능

---

### 원자적 쓰기(Atomic Writes)

- 여러 개의 이벤트를 단일 `persist` 호출로 원자적으로(atomically) 영속화 가능
- 시스템은 단일 persist 호출의 모든 이벤트가 저장되거나(all), 아니면 하나도 저장되지 않음(none)을 보장 → 결코 일부만(partial subset) 저장되는 일은 없음
  - 복구 시 불완전한 상태(incomplete recovery)가 만들어지는 것을 방지
  - 논리적으로 하나로 묶여야 하는 여러 이벤트는 하나의 persist 호출로 함께 저장하는 것이 안전

---

### 고급 기능

#### 상태 기반 동작 변경 (유한 상태 기계)

- 상태 기반 커맨드 처리(state-based command handling): 상태 타입에 대한 패턴 매칭(pattern matching)을 사용하여 어떤 핸들러가 적용될지 결정
  - 유한 상태 기계(finite-state machine, FSM) 패턴 구현 가능
  - 현재 상태에 따라 서로 다른 커맨드 핸들러가 적용됨(예: "열린 계좌(OpenedAccount)"와 "닫힌 계좌(ClosedAccount)"는 동일한 커맨드에 대해 다르게 반응 가능)

#### Behaviors.setup으로 감싸기

- `EventSourcedBehavior`의 생성을 감싸면 `ActorContext`에 접근 가능
- 자식 액터(child actor)를 스폰(spawn)하거나 로깅(logging)에 접근하는 등의 작업 가능

#### 보관(Stashing)

- 커맨드는 영속화 및 복구 도중에 자동으로 보관(stash)됨
- 추가로 `Effect.stash`와 `Effect.unstashAll`을 통해 수동 보관(manual stashing) 가능 → 커맨드 처리를 의도적으로 지연(defer) 가능

#### 스냅샷(Snapshot)

- 최적화를 위해, 스냅샷(snapshot)은 일정한 간격으로 상태를 포착(capture)
  - 모든 과거 이벤트를 재생할 필요가 없어짐 → 복구 시간(recovery time)이 극적으로 감소
  - (자세한 내용은 아래 [스냅샷](#스냅샷snapshot) 섹션 참고)

---

### 의존성과 설정

- Akka Persistence 라이브러리 사용 시 저널(journal)과 스냅샷 저장소(snapshot store) 플러그인을 선택해야 함
- 모듈 `akka-persistence-typed`는 타입이 지정된(typed) API를 제공
- 설정(configuration)이 제어하는 항목:
  - 동시 복구(concurrent recoveries)의 수
  - 손상 탐지(corruption detection)를 위한 재생 필터(replay filter)
  - 저널 구현체별 플러그인 매개변수(plugin-specific parameters)

---

### 스타일 가이드(Style Guide)

> 원본: https://doc.akka.io/libraries/akka-core/current/typed/persistence-style.html

이 절은 Akka에서 이벤트 소싱 엔티티를 구조화하는 모범 사례(best practice)를 다루며, 네 가지 주요 아키텍처 패턴을 중심으로 설명.

#### 상태 안의 이벤트 핸들러 (Event handlers in the state)

- 이 패턴은 이벤트 처리 로직(event handling logic)을 상태 클래스(state class) 자체 안에 배치
- 은행 계좌(bank account) 도메인 예제 사용 → 생애 주기(lifecycle)의 각 단계를 별도의 상태 클래스로 표현
  - `EmptyAccount` (빈 계좌)
  - `OpenedAccount` (개설된 계좌)
  - `ClosedAccount` (닫힌 계좌)
- 각 상태 클래스는 상태 전이(transition)를 처리하는 `applyEvent` 메서드를 구현
- 동작 수준(behavior level)의 이벤트 핸들러는 단순히 이 메서드에 위임(delegate)

```
state.applyEvent(event)
```

- 이 접근법은 상태를 핵심 비즈니스 로직(core business logic)을 담은 도메인 객체(domain object)로 취급, 이벤트 적용 로직을 관련 상태 타입에 국한
- 예상치 못한 이벤트가 적절하지 않은 상태에 도착하면 예외(exception)를 던져 잘못된 상태 전이(invalid state transition) 방지

#### 상태 안의 커맨드 핸들러 (Command handlers in the state)

- 앞선 패턴을 확장하여 커맨드 처리(command handling)도 상태 클래스 안의 `applyCommand` 메서드로 내장 가능
- 비즈니스 로직을 더욱 중앙집중화 → 각 상태가 들어오는 커맨드에 어떻게 반응할지를 스스로 정의

동작 수준의 커맨드 핸들러:

```
state.applyCommand(cmd)
```

- 커맨드 처리가 비즈니스 상태 생애 주기와 밀접하게 대응될 때 이 방식이 적합 → 서로 다른 상태가 동일한 커맨드에 자연스럽게 다르게 반응

#### 선택적 초기 상태 (Optional initial state)

- 별도의 빈 상태 클래스(empty state class)를 만드는 대신, 초기 상태로 `None` 또는 `null` 사용 가능
  - `null`을 `emptyState`로 사용할 경우, 핸들러는 상태 메서드에 위임하기 전에 `null` 여부를 확인해야 함
  - Scala 애플리케이션에서는 `Option[State]`를 사용하고 핸들러 계층에서 패턴 매칭(pattern matching) 수행 가능
- 이 접근법은 빈 상태에 의미 있는 연산이나 데이터가 없을 때 보일러플레이트(boilerplate) 감소
  - 보통 첫 번째 커맨드가 정상 운영 전에 초기 상태를 생성

#### 가변 상태 (Mutable state)

- 불변 상태(immutable state, 변경 시 새 인스턴스를 생성)가 일반적으로 선호되지만, 가변 상태(mutable state)도 사용 가능
- 가장 중요한 제약: 가변 상태 인스턴스를 절대 액터 간 메시지로 보내지 말 것 → 응답(reply)과 통신(communication)에는 반드시 불변 타입(immutable type) 사용
- 가변 상태를 사용할 때 `emptyState` 메서드는 호출할 때마다 새로운 인스턴스(fresh instance)를 반환해야 함 → 실패 복구(failure recovery) 도중 올바른 동작 보장

#### Java 21 기능 활용하기 (Leveraging Java 21 features)

- Java 21 이상을 대상으로 하는 프로젝트는 `EventSourcedOnCommandBehavior`를 확장하여 switch 패턴 매칭(switch pattern matching) 활용 가능
- `sealed` 커맨드 및 이벤트 인터페이스와 결합 → 모든 커맨드 및 이벤트 타입이 처리되었는지 컴파일 타임에 검증(compile-time verification) 가능
- `onCommand`와 `onEvent` 메서드는 중첩된(nested) switch 식(expression)을 사용하여 빌더 기반(builder-based) 등록을 대체
  - 이 접근법은 보일러플레이트를 제거하면서 컴파일 타임 완전성 검사(exhaustiveness checking) 보장
  - 레코드(record)와도 자연스럽게 짝을 이루어 간결한 타입 정의 가능

---

### 스냅샷(Snapshot)

> 원본: https://doc.akka.io/libraries/akka-core/current/typed/persistence-snapshot.html

#### 개요

- 긴 이벤트 로그(long event log)와 늘어난 복구 시간(extended recovery time)을 겪는 영속 액터는 스냅샷(snapshot)의 혜택을 받을 수 있음
- 문서 표현: "영속 액터는 N개의 이벤트마다 또는 주어진 상태의 술어(predicate)가 충족될 때 내부 상태의 스냅샷을 저장할 수 있다(Persistent actors can save snapshots of internal state every N events or when a given predicate of the state is fulfilled)."

#### 기본 스냅샷 동작

- 스냅샷은 주기적으로 액터 상태를 저장함으로써 복구 시간을 감소
- 복구 시: "영속 액터는 가장 최근에 저장된 스냅샷(latest saved snapshot)을 사용하여 상태를 초기화한다. 그 이후, 스냅샷 이후의 이벤트들이 이벤트 핸들러를 통해 재생되어 영속 액터를 현재(즉 최신) 상태로 복구한다."
- 즉, 복구는 (1) 최신 스냅샷으로 상태를 초기화, (2) 그 스냅샷 이후의 이벤트들만 재생하는 두 단계로 이루어짐 → 전체 이벤트를 처음부터 재생할 때보다 훨씬 빠름

#### 스냅샷 선택 기준 (Snapshot selection criteria)

- 기본적으로 복구는 가장 최신 스냅샷을 사용 → 개발자는 이 동작을 재정의(override) 가능
  - 스냅샷 기반 복구를 완전히 비활성화하려면 `SnapshotSelectionCriteria.none()` 사용
  - 이 접근법은 "스냅샷 직렬화 형식(snapshot serialization format)이 호환되지 않는 방식으로 변경된 경우(if snapshot serialization format has changed in an incompatible way)" 유용

#### 스냅샷 저장소 설정 (Snapshot store configuration)

- "스냅샷을 사용하려면 기본 스냅샷 저장소(`akka.persistence.snapshot-store.plugin`)가 설정되어 있어야 하거나, 특정 `EventSourcedBehavior`에 대해 스냅샷 저장소를 선택할 수 있다."
- 스냅샷 저장소를 설정하지 않고 운영할 수도 있음 → 액터가 스냅샷 작업을 시도하기 전까지 Akka는 경고(warning)를 로깅

#### 스냅샷 중 커맨드 보관 (Command stashing during snapshots)

- "스냅샷이 트리거되면, 들어오는 커맨드는 스냅샷이 저장될 때까지 보관(stash)된다. 이는 상태가 가변(mutable)이어도 안전하다는 것을 의미한다. 왜냐하면 상태의 직렬화와 저장이 비동기적으로(asynchronously) 수행되기 때문이다."

#### snapshotWhen 과 RetentionCriteria

스냅샷을 트리거하는 방법 크게 두 가지:

1. 술어 기반(predicate-based): `snapshotWhen` / `shouldSnapshot`을 사용하여, 특정 상태/이벤트 조건이 충족될 때 스냅샷을 저장
2. 자동/주기 기반(automatic): `RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2)`처럼 일정 이벤트 수마다 스냅샷을 저장

#### 스냅샷 실패 (Snapshot failures)

- 스냅샷 작업은 시그널(signal)을 통해 결과를 보고
- "스냅샷 저장은 성공하거나 실패할 수 있다 – 이 정보는 `SnapshotCompleted` 또는 `SnapshotFailed` 시그널을 통해 영속 액터에 보고된다. 스냅샷 실패(snapshot failure)는 기본적으로 로깅되지만, 액터를 정지하거나 재시작시키지는 않는다."
- 복구 실패는 이와 크게 다름: "액터가 시작될 때 저널로부터 액터의 상태를 복구하는 데 문제가 있으면, `RecoveryFailed` 시그널이 방출되고(기본적으로 오류를 로깅), 액터는 정지된다."

#### 선택적 스냅샷 (Optional snapshots)

- 선택적 스냅샷(optional snapshots)은 스냅샷 로드(load) 실패 시 무조건적인 액터 종료를 방지
  - 스냅샷 저장소 설정에서 `snapshot-is-optional = true`로 활성화
- 이 메커니즘은 "예를 들어 역직렬화 오류(deserialization error)가 발생했을 때 스냅샷을 무시해도 괜찮은 경우에 유용하다. 스냅샷 로딩이 실패하면, 대신 모든 이벤트를 재생함으로써 복구한다."

> 경고: 이벤트가 삭제(delete)된 경우에는 이 기능을 활성화하지 말 것. 실패한 스냅샷 복구가 잘못된 상태(incorrect state)를 만들어 낼 수 있음.

#### 스냅샷 삭제 (Snapshot deletion)

- 자동 스냅샷 삭제는 `RetentionCriteria`를 통해 저장 공간을 회수(reclaim)
  - 예제 설정: `RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2)`
- "스냅샷 삭제는 새로운 스냅샷을 성공적으로 저장한 후에 트리거된다." 위 예제에서는, "저장된 스냅샷의 시퀀스 번호(sequence number)에서 `keepNSnapshots * numberOfEvents`(`100 * 2`)를 뺀 값보다 작은 시퀀스 번호를 가진 스냅샷이 자동으로 삭제된다."

##### 술어 기반 vs. 자동 스냅샷

- 술어 기반 스냅샷(`snapshotWhen` / `shouldSnapshot`)은 오래된 스냅샷의 자동 삭제를 트리거하지 않음
- 오직 `numberOfEvents`에 기반한 자동 스냅샷만이 삭제를 트리거

##### 삭제에 대한 시그널 처리

- 비동기 삭제는 `DeleteSnapshotsCompleted` 또는 `DeleteSnapshotsFailed` 시그널을 방출
- "기본적으로, 성공적인 완료는 시스템에 의해 `debug` 로그 레벨로, 실패는 `warning` 로그 레벨로 로깅된다." `receiveSignal` 핸들러를 사용해 결과에 대응 가능

#### 이벤트 삭제 (Event deletion)

##### 이벤트 삭제의 함의

- 이벤트를 삭제하면 시스템의 이력(history)이 제거됨 → 이벤트 소싱의 원칙에 어긋남
- "이벤트 소싱 기반 애플리케이션에서 이벤트 삭제(deleting events)는 일반적으로 전혀 사용되지 않거나, 스냅샷과 함께 사용된다."

##### 프로젝션 및 복제 이벤트 소싱과의 제약

- "즉각적인 이벤트 삭제(immediate event deletion)는 프로젝션(Projection)과 함께 사용해서는 안 되며, 복제 이벤트 소싱(Replicated Event Sourcing)의 경우 스냅샷 술어 또는 보존 기준(retention criteria)을 통한 이벤트 삭제는 허용되지 않는다."
- 권장되는 접근법: "최종 삭제 이벤트(final deleted event)를 방출하고, 엔티티가 삭제되었다는 사실을 프로젝션을 통해 별도로 저장한다. 그런 다음 백그라운드 작업(background task)이 삭제된 엔티티들의 이벤트와 스냅샷을 정리(clean up)할 수 있다."

##### 술어 기반 스냅샷에서 이벤트 삭제 활성화

술어 기반 스냅샷에서는 다음으로 삭제를 활성화:

- `.snapshotWhen(predicate, deleteEventsOnSnapshot = true)` (Scala)
- Java에서는 `deleteEventsOnSnapshot()` 재정의

##### 보존 기준에서 이벤트 삭제 활성화

- 스냅샷 기반 보존(snapshot-based retention)의 경우, `RetentionCriteria`에서 `withDeleteEventsOnSnapshot`을 호출(기본적으로 비활성화)
- "스냅샷 기반 보존이 활성화되면, 스냅샷이 성공적으로 저장된 후, 해당 스냅샷이 보유한 데이터의 시퀀스 번호까지의 이벤트 삭제(단일 이벤트 소싱 액터가 저널링한)를 발행할 수 있다."
- 이벤트 삭제는 스냅샷 삭제보다 먼저 진행. 비동기 삭제는 `DeleteEventsCompleted` 또는 `DeleteEventsFailed` 시그널을 방출

##### 기술적 참고 사항

- "메시지 삭제(message deletion)는 저널의 최고 시퀀스 번호(highest sequence number)에 영향을 주지 않는다. 삭제가 일어난 후 모든 메시지가 삭제되었더라도 마찬가지다."
- "이벤트가 저장소에서 실제로 제거되는지 여부는 저널 구현체에 달려 있다."

---
### 이벤트 소싱 동작 테스트하기

> 원본: https://doc.akka.io/libraries/akka-core/current/typed/persistence-testing.html

#### 개요

- Akka는 `EventSourcedBehavior`에 대해 세 가지 상호 보완적 접근법으로 테스트 도구 제공
  1. `BehaviorTestKit`과 함께 사용하는 `UnpersistentBehavior`
  2. `EventSourcedBehaviorTestKit`
  3. `PersistenceTestKit`

#### UnpersistentBehavior를 사용한 단위 테스트

- `UnpersistentBehavior` 클래스 → `EventSourcedBehavior`를 영속화하지 않는(non-persisting) 변형으로 변환 → 단위 테스트(unit test) 가능
- 이벤트·스냅샷을 저장소에 기록하는 대신 → 영속화 작업에 대한 단언(assertion)이 가능하도록 `PersistenceProbe` 객체 노출

##### 초기화

- 이벤트 소싱 액터로부터 unpersistent 동작 생성

```
UnpersistentBehavior.fromEventSourced(
  AccountEntity("1", PersistenceId("Account", "1"))
)
```

##### 임의의 상태로 초기화

- 이 동작은 미리 정해진 상태(predetermined state)로 시작 가능
- 이벤트 핸들러를 재사용해 이전 커맨드 실행을 시뮬레이션

```
UnpersistentBehavior.fromEventSourced(
  AccountEntity("1", PersistenceId("Account", "1")),
  Some(AccountEntity.EmptyAccount.applyEvent(...) -> 1L)
)
```

##### BehaviorTestKit과 함께 테스트

- `UnpersistentBehavior`는 동기적(synchronous) 테스트를 위해 `BehaviorTestKit`과 직접 통합
- 커맨드는 호출 스레드(calling thread)에서 실행 → 단언 실행 전에 완전한 처리가 끝남을 보장
- 내부 상태는 동작의 응답 및 영속화된 이벤트/스냅샷을 통해서만 노출

##### 주요 장점

- 설정(configuration) 불필요
- 테스트 스레드에서 동기 실행
- 이벤트/스냅샷 검증을 위한 영속화 프로브(persistence probe) 접근
- 참고: 직렬화(serialization)는 검증 대상 아님 → 직렬화 테스트는 독립적으로 수행 권장

#### EventSourcedBehaviorTestKit을 사용한 단위 테스트

- `EventSourcedBehaviorTestKit`은 종합적인 단위 테스트 기능 제공
- 한 번에 하나의 커맨드(one command at a time)를 처리
- 방출된 이벤트와 결과 상태를 담아 동기적으로 반환되는 결과(result)에 대해 단언

##### 설정

- `ActorSystem`을 `EventSourcedBehaviorTestKit.config`로 설정 → 인메모리(in-memory) 저널 및 스냅샷 저장소 활성화

```
class AccountExampleDocSpec extends 
  ScalaTestWithActorTestKit(EventSourcedBehaviorTestKit.config)
```

##### 테스트킷 초기화

```
EventSourcedBehaviorTestKit.create(
  testKit.system(), 
  AccountEntity.create("1", PersistenceId.of("Account", "1"))
)
```

##### 테스트 구조

- `runCommand()`로 커맨드 실행 → 응답(reply)·영속화된 이벤트·새로운 상태를 담은 결과 수신

```
val result = eventSourcedTestKit.runCommand[StatusReply[Done]](
  AccountEntity.CreateAccount(_)
)
result.reply shouldBe StatusReply.Ack
result.event shouldBe AccountEntity.AccountCreated
result.stateOfType[AccountEntity.OpenedAccount].balance shouldBe 0
```

##### 직렬화 검증

- 직렬화는 왕복(roundtrip) 검사를 통해 자동 검증
- 테스트킷 생성 시 `SerializationSettings`로 커스터마이즈 가능
- 기본값: 동등성(equality) 검증 비활성화
- 커맨드/이벤트/상태가 `equals`를 구현하거나 케이스 클래스(case class)를 사용하는 경우 → `verifyEquality`로 활성화 가능

##### 복구 테스트

- `restart()` 메서드로 복구 동작 테스트
- 재시작된 동작은 이전 커맨드로 저장된 스냅샷과 이벤트로부터 복구
- 내부의 `PersistenceTestKit`에 접근 → 저장소 사전 채우기(prepopulate) 또는 실패 시뮬레이션 가능

##### 상태 초기화

- 테스트 사이에 `clear()` 호출 → 동작의 저장소 초기화

```
override protected def beforeEach(): Unit = {
  super.beforeEach()
  eventSourcedTestKit.clear()
}
```

#### Persistence TestKit

- `PersistenceTestKit`: 영속화된 이벤트 검증·저장 작업 에뮬레이션(emulation)·예외(exception) 시뮬레이션 지원
- 이벤트와 스냅샷을 다루는 두 개의 병렬 클래스 존재
  - 이벤트용 `PersistenceTestKit`
  - 스냅샷용 `SnapshotTestKit`

##### 플러그인 설정

- 해당 플러그인을 `ActorSystem`에 설정 필요

```
val system = ActorSystem(
  behavior,
  "test-system",
  PersistenceTestKitPlugin.config.withFallback(yourConfiguration)
)
val testKit = PersistenceTestKit(system)
```

##### 핵심 API 메서드

- 이 테스트킷이 제공하는 메서드
  - 다음에 영속화된 이벤트/스냅샷이 기대값과 일치하는지 단언
  - 영속화된 항목들의 시퀀스 읽기
  - 어떤 항목도 영속화되지 않았는지 검증
  - 이후 작업에서 기본 저장소 예외(default storage exception) 트리거
  - 모든 영속화된 데이터 초기화
  - 이벤트 거부(reject) — 스냅샷은 거부 미지원
  - 작업 동작을 제어하는 사용자 정의 저장소 정책(custom storage policy) 정의
  - 모든 영속화된 항목 조회
  - 복구 테스트를 위한 저장소 사전 채우기(prepopulate)

##### 사용자 정의 저장소 정책

- `ProcessingPolicy`를 구현 → 저장소 동작 제어
- `tryProcess()` 메서드는 영속화 ID와 저장 작업(storage operation)을 받아 다음 세 가지 결과 중 하나를 반환
  - `ProcessingSuccess`: 작업 정상 완료
  - `StorageFailure`: 저장소 예외 에뮬레이션
  - `Reject`: 거부 에뮬레이션 (이벤트에만 해당)

###### 이벤트 저장 작업

- `ReadEvents`: 저장소에서 이벤트 조회
- `WriteEvents`: 저장소에 이벤트 쓰기
- `DeleteEvents`: 저장소에서 이벤트 제거
- `ReadSeqNum`: 특정 영속화 ID의 최고 시퀀스 번호 가져오기

###### 스냅샷 저장 작업

- `ReadSnapshot`: 저장소에서 스냅샷 조회
- `WriteSnapshot`: 저장소에 스냅샷 쓰기
- `DeleteSnapshotsByCriteria`: 기준에 맞는 스냅샷 제거
- `DeleteSnapshotByMeta`: 메타데이터로 특정 스냅샷 제거

###### 정책 구현 예제

- 사용자 정의 정책은 내부 상태를 유지 → 작업 타입에 대해 패턴 매칭

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

##### 설정 참조

- Persistence 테스트킷은 `akka.persistence.testkit` 하위의 참조 설정(reference configuration)에 문서화된 여러 설정 속성 지원

#### 통합 테스트 (Integration testing)

- `EventSourcedBehavior` 액터는 다른 액터와 함께 테스트하기 위해 `ActorTestKit`과 통합
- `PersistenceTestKit`의 인메모리 저널 및 스냅샷 저장소 → 단일 `ActorSystem` 내에서의 통합 테스트(예: 클러스터 샤딩 시나리오) 지원
- 다중 노드(multi-node) 클러스터 테스트의 경우 → 실제 데이터베이스 백엔드 필요
- Persistence Plugin Proxy 사용 가능하나 → 운영 환경에 가까운(production-realistic) 테스트는 일반적으로 실제 데이터베이스 사용

#### 플러그인 초기화

- 일부 영속성 플러그인은 테이블 생성(table creation) 필요 → 여러 `ActorSystem` 인스턴스에서 동시에 일어날 수 없음
- `PersistenceInit` 유틸리티로 초기화 조율

```
val done: Future[Done] = 
  PersistenceInit.initializeDefaultPlugins(system, timeout)
Await.result(done, timeout)
```

- 이는 테스트 중 클러스터 노드 전반에 걸쳐 플러그인 설정을 동기화

---

### 복제 이벤트 소싱(Replicated Event Sourcing)

> 원본: https://doc.akka.io/libraries/akka-core/current/typed/replicated-eventsourcing.html

#### 핵심 개념

- 복제 이벤트 소싱(Replicated Event Sourcing) → 표준 `EventSourcedBehavior`를 확장
- 단일 작성자 원칙(single-writer principle) 대신 각 엔티티의 여러 활성 복제본(multiple active replicas) 허용
- 이를 통해 여러 위치에 걸쳐 "액티브-액티브(active-active)" 및 "핫 스탠바이(hot standby)" 배포 패턴 유지 → 최종 일관성(eventual consistency) 달성 가능

#### 단일 작성자 원칙의 완화

- 전통적인 `EventSourcedBehavior`는 `persistenceId`당 하나의 활성 인스턴스로 제한
  - 사유: 동시 작성자(concurrent writer)로부터의 이벤트가 뒤섞여 상태 재구성을 손상시키는 것을 방지
- 복제 이벤트 소싱은 이 제약을 포기

> "이벤트는 모든 복제본(all replicas)으로 자동으로 복제된다."

- 이를 통해 클라우드 리전(region)·데이터 센터·가용 영역(availability zone) 전반에 걸친 중복성(redundancy) 가능

배포상의 이점

- 한 위치가 실패했을 때의 장애 내성(fault tolerance)
- 요청을 로컬에서 처리 → 지연 시간(latency) 감소
- 여러 위치에서의 업데이트
- 여러 서버에 걸친 부하 분산(load distribution)

핵심 트레이드오프

- 단일 작성자 보장(single-writer guarantee)이 유지되지 않음 → 이벤트 핸들러는 동시 이벤트(concurrent event) 처리 가능해야 함
- 네트워크 분할(partition)이나 장애 상황에서 복제가 지연될 수 있음 → 상태는 최종 일관성(eventually consistent)을 가짐

#### API 구조

- 두 가지 주요 추상화(abstraction) 존재
  - `ReplicatedEventSourcedBehavior`
  - `ReplicatedEventSourcedOnCommandBehavior`
- 이들은 표준 `EventSourcedBehavior` API를 그대로 반영
- 설정에는 `ReplicatedEventSourcing.commonJournalConfig()` 팩토리 메서드 사용 → 다음을 받음
  - `ReplicationId`: 엔티티 비즈니스 키(business key)·복제본 식별자(replica identifier)·선택적인 도메인 구분 정보 포함
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

#### 이벤트 복제 전송 (Event replication transports)

- gRPC 전송 (권장)
  - Akka 2.8.0부터 gRPC가 표준 복제 메커니즘 제공
  - Akka Projection gRPC 모듈을 통해 구현
  - Akka Distributed Cluster Guide에 종합적인 문서와 예제 존재
- 직접 데이터베이스 접근 (Direct database access)
  - 복제본들이 서로의 데이터베이스에 직접 연결 → 이벤트 소비하는 대안적 접근법
  - gRPC 전송에 비해 설정과 보안이 까다로움 → 사설 네트워크(private network)로 묶여 있지 않은 한 실용적이지 않음
  - 자세한 내용은 아래 "직접 데이터베이스 전송을 통한 복제"에서 다룸

#### 직접 데이터베이스 전송을 통한 복제 (Replicated Event Sourcing over Direct Database Transport)

> 원본: https://doc.akka.io/libraries/akka-core/current/typed/replicated-eventsourcing-db-transport.html

- gRPC 전송이 권장되는 기본 방식이지만, 모든 복제본이 같은 분산 데이터베이스(예: Cassandra)를 공유하는 사설 네트워크 환경이나 테스트 목적이라면 각 복제본이 다른 복제본의 저널을 직접 폴링(polling)하는 방식도 사용할 수 있음
- 클러스터 샤딩(Cluster Sharding)과 함께 쓰는 경우, 복제본들은 저널 폴링에 더해 이벤트를 액터 클러스터 네트워크로도 발행(publish)함 → 대부분의 이벤트는 클러스터를 통해 먼저 도착하므로, 저널 폴링 주기를 더 느슨하게 설정할 수 있음. 다만 클러스터를 통한 전달은 보장되지 않으므로 저널 조회 자체는 계속 필요함

##### 샤딩된 복제 이벤트 소싱 엔티티(Sharded Replicated Event Sourced entities)

- 클러스터 샤딩 위에서 복제 이벤트 소싱 엔티티를 실행할 때 적용되는 설정 방식으로, 엔티티 ID·복제본 식별자·알려진 전체 복제본 목록을 미리 지정해야 함

##### 이벤트 직접 복제(Direct Replication of Events)

- 저널 폴링과 별개로, 액터 간 직접 메시지 전달을 통해 이벤트를 다른 복제본에 즉시 전파하는 경로를 함께 둘 수 있음 → 저널 폴링 주기가 길어도 지연(latency)을 낮게 유지할 수 있음

##### 핫 스탠바이(Hot Standby)

- 평상시에는 조회 트래픽을 받지 않는 복제본이라도, 이벤트를 지속적으로 복제받아 로컬 상태를 최신으로 유지하도록 구성할 수 있음 → 장애 조치(failover) 시 즉시 활성화될 수 있는 준비된 복제본을 유지하는 용도

##### 설정 팩토리 메서드

두 가지 설정 방식이 제공됨.

- `perReplicaJournalConfig`: 복제본마다 별도의 데이터베이스·고유한 저널 식별자를 사용함
- `commonJournalConfig`: 모든 복제본이 동일한 저널을 공유함(Cassandra처럼 분산 데이터베이스를 여러 복제본이 함께 바라보는 경우에 적합)

두 방식 모두 엔티티 ID, 복제본 식별자, 알려진 모든 복제본 목록을 사전에 지정해야 함.

##### 저널 지원(Journal Support)

- 직접 데이터베이스 전송을 통한 복제를 지원하는 저널 플러그인
  - Akka Persistence Cassandra 1.0.3 이상
  - Akka Persistence R2DBC 1.0.0 이상
  - Akka Persistence JDBC 5.0.0 이상
- 각 플러그인은 복제에 필요한 메타데이터를 저널 구현 내부에서 처리함

#### 충돌 해결 전략 (Conflict resolution strategies)

##### 충돌 없는 복제 데이터 타입 (CRDTs)

- 분산 일관성(distributed consistency)을 위한 기반 접근법
- 연산 기반(operation-based) CRDT가 복제 이벤트 소싱에 적합 → "이벤트가 곧 연산(operation)을 표현"
- 교환 법칙(commutativity) 요구사항: "동일한 이벤트를 어떤 순서로 적용하더라도 항상 동일한 최종 상태를 만들어 내야 함"

내장 CRDT 구현체

- `LwwTime`: 타임스탬프와 복제본 식별자를 사용하는 최종 작성자 우선(last-writer-wins)
- `Counter`: 동시 증가(concurrent increment)를 지원하는 분산 카운터
- `ORSet`: 관찰-제거 집합(Observed-Remove Set), 동시 추가 우선(concurrent-add-wins) 의미론으로 추가/제거 허용

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

- 이벤트 핸들러는 연산을 적용: `state.applyOperation(event)`

##### 최종 작성자 우선 (Last-Writer-Wins, LWW)

- 타임스탬프만으로 충돌 해결이 충분할 때 사용
- LWW는 가장 높은 타임스탬프를 가진 이벤트를 사용
- 이 접근법은 동기화된 시계(synchronized clock)에 의존
- 시계 오차(clock skew) 범위 내의 동시 업데이트에서 값의 선택이 중요하지 않은 경우에만 동작

`LwwTime` 사용

- 영속화 시점의 타임스탬프와 원본 복제본 식별자 포함
- 비교 시 가장 높은 타임스탬프 우선 → 동률(tie)일 때는 복제본 ID를 알파벳/숫자 순으로 비교하여 결정
- 단조 증가하는(monotonically advancing) 타임스탬프를 위해 `increase()` 메서드 사용

핵심 한계

- 부분 상태 업데이트(partial state update)는 복제본을 발산(diverge)시킬 수 있음
- 별개의 필드가 독립적인 타임스탬프를 갖는 경우(예: `AuthorChanged`와 `TitleChanged` 이벤트) → 동시 업데이트가 복제본 전반에 걸쳐 일관되지 않게 적용될 수 있음
- 해결책
  1. 각 이벤트에 전체 상태(full state) 포함
  2. 여러 개의 타임스탬프 사용 — 독립적으로 업데이트 가능한 필드 그룹마다 하나씩

예제:

```
PostAdded 이벤트는 다음을 통해 타임스탬프를 포함:
state.contentTimestamp.increase(
  replicationContext.currentTimeMillis(),
  replicationContext.replicaId())
```

- 이벤트 핸들러는 조건부로 적용

```
if (timestamp.isAfter(state.contentTimestamp)) {
  // 이벤트를 적용하고 타임스탬프 갱신
} else {
  // 오래된 이벤트는 폐기
}
```

#### 인과성 및 동시성 탐지 (Causality and concurrent detection)

##### 버전 벡터 (Version vectors)

- 복제 이벤트 소싱은 이벤트 간의 인과 관계(causal relationship)를 추적하기 위해 버전 벡터(version vector) 사용
- 각 복제본은 논리적 시계(logical clock, 카운터)를 유지 → 이벤트를 영속화할 때 증가
- 벡터는 이벤트와 함께 전파 → 복제된 이벤트를 소비할 때 버전 벡터가 로컬에서 병합(merge)

버전 벡터 비교가 판별하는 것

- SAME: 모든 위치가 동일
- BEFORE: 모든 위치가 ≤이고 일부가 엄격하게 <
- AFTER: 모든 위치가 ≥이고 일부가 엄격하게 >
- CONCURRENT: before/after 관계가 성립하지 않음 (동시 발생)

##### 인과적 전달 순서 (Causal delivery order)

> "한 복제본에서 영속화된 이벤트는 다른 복제본에서 동일한 순서로 읽힌다."

- 단 "동시 이벤트(concurrent event)의 순서는 정의되지 않음" → CRDT 사용 시 허용 가능해야 함

인과성 체인(causality chain) 예제:

```
DC-1: e1 쓰기
DC-2: e1 읽기, e2 쓰기
DC-1: e2 읽기, e3 쓰기
```

- 이는 모든 복제본에 걸쳐 보편적인 순서 e1 → e2 → e3를 만들어 냄
- 동시 이벤트가 있는 경우(예: e3와 e2가 서로를 알지 못한 채 생성된 경우) → 서로 다른 복제본이 서로 다른 순서를 관찰할 수 있으나, 교환 법칙(commutativity) 덕분에 둘 다 연산을 동일하게 적용

#### ReplicationContext

- 커맨드 핸들러와 이벤트 핸들러에서 사용 가능
- 제공 항목
  - 현재 복제본 식별자
  - 엔티티 비즈니스 식별자
  - 이벤트를 처리하는 원본 복제본(origin replica)
  - 복구 상태 플래그(recovery status flag)
  - 밀리초 단위의 현재 시간
- 이를 통해 선택적인 사이드 이펙트 구현 가능 → 특정 복제본에서만, 또는 완전한 복제가 이루어진 후에만 동작을 트리거하는 식

#### 사이드 이펙트 (Side effects)

- 이벤트 핸들러에서의 직접적인 사이드 이펙트는 일반적으로 권장되지 않음
  - 사유: 이벤트 핸들러는 재생(replay) 시와 복제된 이벤트를 소비할 때도 호출됨 → 의도치 않은 재실행(re-execution) 유발 가능

적절한 사이드 이펙트 사용 사례

- 단 하나의 복제본에서만 동작을 트리거
- 모든 복제본이 이벤트를 본 후 한 번만 실행
- 복제된 이벤트의 도착 처리
- 탐지된 충돌(conflict)에 대한 응답

- `ReplicationContext` 필드가 이러한 패턴을 가능하게 함
- 예: 최종 동작(예: 경매(auction) 정산)을 위해 하나의 복제본을 지정 → 중복 실행(duplicate execution) 방지
- Auction 예제가 이러한 기법을 보여줌

#### 프로젝션 통합 (Projection integration)

- Akka Projections는 복제된 엔티티와 함께 동작

> "프로젝션은 모든 복제본으로부터 모든 이벤트(all events from all replicas)를 받게 된다"

- 복제본마다 도착 순서가 다를 수 있다는 점 고려 필요

두 가지 패턴

1. 모든 이벤트 처리: 복제본 내의 재정렬(reordering)을 수용
2. 로컬 복제본으로 필터링: `ReplicationContext`의 원본 정보를 사용 → 태그나 이벤트 메타데이터로 저장 → 프로젝션이 로컬 복제본 이벤트만 처리하도록 함

#### 저널 및 스냅샷 지원

- 저널은 `PersistentRepr`의 `metadata` 필드를 통해 `withMetadata()`를 사용 → 이벤트 메타데이터를 저장하고 읽을 수 있어야 함
- 스냅샷 저장소도 마찬가지로 저장 및 조회 시 메타데이터 처리 필요
- Persistence TCK는 검증을 위한 `supportsMetadata` 기능 플래그 제공
- 현재 Akka Persistence R2DBC(버전 1.1.0 이상)가 gRPC를 통한 복제 이벤트 소싱 지원

#### 비복제에서의 마이그레이션 (Migration from non-replicated)

- 기존 `EventSourcedBehavior` 엔티티를 `ReplicatedEventSourcedBehavior`로 변환 시
  - 이벤트가 보존되고 동일하게 저장됨
  - 메타데이터가 없으면 자동으로 채워짐(auto-populated)
- 원본 복제본은 동일한 `PersistenceId`와 이벤트 시퀀스를 유지하기 위해 빈 `ReplicaId`(empty `ReplicaId`)를 사용해야 함
- 핵심 고려사항: 이제 동시 업데이트(concurrent update)가 가능해짐 → 이벤트 핸들러 로직은 "충돌하는 업데이트 해결(resolving conflicting updates)" 의미론에 맞게 조정 필요, 이를 우아하게(gracefully) 처리해야 함

---

### CQRS

> 원본: https://doc.akka.io/libraries/akka-core/current/typed/cqrs.html

#### 개요

- CQRS(Command Query Responsibility Segregation, 커맨드 쿼리 책임 분리): 데이터를 변경하는 작업(커맨드, command)과 데이터를 읽는 작업(쿼리, query)을 서로 다른 모델로 분리하는 아키텍처 패턴
- 공식 문서의 핵심 메시지

> "`EventSourcedBehavior`는 Akka Projections와 함께 사용되어 CQRS(Command Query Responsibility Segregation)를 구현할 수 있다(EventSourcedBehavior s along with Akka Projections can be used to implement Command Query Responsibility Segregation (CQRS))."

- 문서는 이벤트 소싱과 프로젝션을 결합하는 방법을 설명하는 종합적인 튜토리얼이 "Microservices with Akka tutorial"에 존재한다고 안내

#### Akka에서의 CQRS 구성

- 쓰기 측 (Write side)
  - `EventSourcedBehavior`로 구현
  - 커맨드를 받아 검증 → 이벤트를 영속화
  - 이 측이 진실의 원천(source of truth)인 이벤트 로그를 생성
  - 쓰기 모델은 트랜잭션 일관성(transactional consistency)과 비즈니스 규칙 검증에 최적화
- 읽기 측 (Read side)
  - Akka Projections로 구현
  - 쓰기 측이 만들어 낸 이벤트 스트림을 소비 → 쿼리에 최적화된 별도의 읽기 모델(read model, 예: 데이터베이스 뷰·검색 인덱스 등) 구축
  - 읽기 모델은 다양한 쿼리 패턴에 맞게 자유롭게 비정규화(denormalize) 가능

#### 이벤트 태깅 (Tagging events)

- 읽기 측 프로젝션이 이벤트를 효율적으로 소비하려면 → 쓰기 측에서 이벤트에 태그(tag) 부여
- `EventSourcedBehavior`의 `withTagger`를 사용해 각 이벤트에 태그 부여 → 프로젝션은 해당 태그 단위로 이벤트를 읽어 처리 가능
- 일반적으로 이벤트를 여러 개의 태그(슬라이스, slice)로 나눔 → 여러 프로젝션 인스턴스가 병렬로(parallel) 이벤트를 처리 → 처리량(throughput) 향상

#### 샤딩 (Sharding)

- 쓰기 측은 클러스터 샤딩(Cluster Sharding)과 결합 → 각 엔티티 id에 대해 단일 활성 인스턴스만 존재하도록 보장
- 이는 이벤트 로그의 일관성 유지와 부하의 클러스터 전반 분산에 핵심

#### 정리

- CQRS의 본질: "쓰기에 최적화된 모델"과 "읽기에 최적화된 모델"을 분리 → 이벤트 소싱을 통해 이 둘을 비동기적으로 연결
- 결과적으로 쓰기 측은 일관성과 무결성에, 읽기 측은 다양한 조회 성능에 각각 집중 가능
- 보다 상세한 구현 예제는 Akka의 "Microservices with Akka" 튜토리얼과 Akka Projections 라이브러리 문서 참고 권장

---

### 참고 자료

- [Akka 공식 문서](https://doc.akka.io/libraries/akka-core/current/)
- [Event Sourcing (원본)](https://doc.akka.io/libraries/akka-core/current/typed/persistence.html)
- [Style guide for EventSourcedBehaviors](https://doc.akka.io/libraries/akka-core/current/typed/persistence-style.html)
- [Snapshotting](https://doc.akka.io/libraries/akka-core/current/typed/persistence-snapshot.html)
- [Testing](https://doc.akka.io/libraries/akka-core/current/typed/persistence-testing.html)
- [Replicated Event Sourcing](https://doc.akka.io/libraries/akka-core/current/typed/replicated-eventsourcing.html)
- [CQRS](https://doc.akka.io/libraries/akka-core/current/typed/cqrs.html)

---

## Akka 영속성: 쿼리, 플러그인, Durable State

> 원본: https://doc.akka.io/libraries/akka-core/current/

---

### 목차

1. [영속성 쿼리(Persistence Query)](#1-영속성-쿼리persistence-query)
   - [1.1 의존성 설정](#11-의존성-설정)
   - [1.2 개요](#12-개요)
   - [1.3 설계 철학](#13-설계-철학)
   - [1.4 ReadJournal 접근](#14-readjournal-접근)
   - [1.5 미리 정의된 쿼리 타입](#15-미리-정의된-쿼리-타입)
   - [1.6 구체화된 값(Materialized Values)](#16-구체화된-값materialized-values)
   - [1.7 성능과 비정규화(Denormalization)](#17-성능과-비정규화denormalization)
   - [1.8 쿼리 플러그인](#18-쿼리-플러그인)
   - [1.9 스케일 아웃(Scaling Out)](#19-스케일-아웃scaling-out)
   - [1.10 예제 프로젝트](#110-예제-프로젝트)
2. [영속성 플러그인(Persistence Plugins)](#2-영속성-플러그인persistence-plugins)
   - [2.1 개요](#21-개요)
   - [2.2 유지보수되는 플러그인](#22-유지보수되는-플러그인)
   - [2.3 기능 제한 사항](#23-기능-제한-사항)
   - [2.4 플러그인 설정](#24-플러그인-설정)
   - [2.5 사전 패키징된 플러그인](#25-사전-패키징된-플러그인)
   - [2.6 영속성 플러그인 프록시(Persistence Plugin Proxy)](#26-영속성-플러그인-프록시persistence-plugin-proxy)
   - [2.7 커스텀 저장소 백엔드 구현(Building a Storage Backend)](#27-커스텀-저장소-백엔드-구현building-a-storage-backend)
3. [스키마 진화(Schema Evolution)](#3-스키마-진화schema-evolution)
   - [3.1 의존성 설정](#31-의존성-설정)
   - [3.2 소개](#32-소개)
   - [3.3 이벤트 소스 시스템에서의 스키마 진화](#33-이벤트-소스-시스템에서의-스키마-진화)
   - [3.4 올바른 직렬화 형식 선택](#34-올바른-직렬화-형식-선택)
   - [3.5 스키마 진화 실전](#35-스키마-진화-실전)
4. [영속 상태(Durable State) 영속성](#4-영속-상태durable-state-영속성)
   - [4.1 모듈 정보](#41-모듈-정보)
   - [4.2 소개](#42-소개)
   - [4.3 핵심 구성 요소](#43-핵심-구성-요소)
   - [4.4 이펙트(Effect)와 부수 효과(Side Effect)](#44-이펙트effect와-부수-효과side-effect)
   - [4.5 완전한 예제: Counter](#45-완전한-예제-counter)
   - [4.6 강제된 응답(Enforced Replies)](#46-강제된-응답enforced-replies)
   - [4.7 상태 기반 동작 변경](#47-상태-기반-동작-변경)
   - [4.8 응답(Replies)](#48-응답replies)
   - [4.9 직렬화(Serialization)](#49-직렬화serialization)
   - [4.10 태깅(Tagging)](#410-태깅tagging)
   - [4.11 ActorContext 접근](#411-actorcontext-접근)
   - [4.12 클러스터 샤딩(Cluster Sharding)](#412-클러스터-샤딩cluster-sharding)
   - [4.13 동작 변경 및 가변 상태 고려 사항](#413-동작-변경-및-가변-상태-고려-사항)
5. [영속 상태 스타일 가이드(Durable State Style)](#5-영속-상태-스타일-가이드durable-state-style)
6. [영속 상태 영속성 쿼리(Durable State Persistence Query)](#6-영속-상태-영속성-쿼리durable-state-persistence-query)
7. [참고 자료](#7-참고-자료)

---
### 1. 영속성 쿼리(Persistence Query)

#### 1.1 의존성 설정

영속성 쿼리(Persistence Query) 사용 → `akka-persistence-query` 의존성 추가. sbt의 경우:

```
val AkkaVersion = "2.10.19"
libraryDependencies += "com.typesafe.akka" %% "akka-persistence-query" % AkkaVersion
```

- Maven 및 Gradle 설정도 공식 문서에서 제공

---

#### 1.2 개요

> "Akka 영속성 쿼리(persistence query)는 다양한 저널(journal) 플러그인이 자신의 쿼리 능력을 노출하기 위해 구현할 수 있는, 범용적이고 비동기적인 스트림 기반 쿼리 인터페이스를 제공함으로써 이벤트 소싱(Event Sourcing)을 보완한다."

- 주요 사용 사례: CQRS 아키텍처 패턴의 쿼리 측(query-side) 기능 구현
  - 쓰기 작업(write operation)과 읽기 작업(read operation)을 서로 다른 데이터 저장소(datastore)로 분리

---

#### 1.3 설계 철학

- API는 저널 구현체가 각자의 강점을 최대한 노출할 수 있도록 의도적으로 느슨하게(loosely) 명세
- 각 읽기 저널(read journal)은 자신이 지원하는 쿼리 타입을 명시적으로 문서화해야 함

---

#### 1.4 ReadJournal 접근

- 쿼리 실행 → 플러그인 식별자(plugin identifier)를 통해 `ReadJournal` 인스턴스 획득

Scala:
```scala
val readJournal = 
  PersistenceQuery(system).readJournalFor[MyScaladslReadJournal](
    "akka.persistence.query.my-read-journal")

val source: Source[EventEnvelope, NotUsed] =
  readJournal.eventsByPersistenceId("user-1337", 0, Long.MaxValue)

source.runForeach { event =>
  println("Event: " + event)
}
```

Java:
```java
final MyJavadslReadJournal readJournal =
  PersistenceQuery.get(system)
    .getReadJournalFor(
      MyJavadslReadJournal.class, 
      "akka.persistence.query.my-read-journal");

Source<EventEnvelope, NotUsed> source =
  readJournal.eventsByPersistenceId("user-1337", 0L, Long.MAX_VALUE);

source.runForeach(event -> System.out.println("Event: " + event), system);
```

---

#### 1.5 미리 정의된 쿼리 타입

##### PersistenceIdsQuery 와 CurrentPersistenceIdsQuery

- `persistenceIds()`: "시스템 내의 모든 영속 id(persistent id)의 스트림" 제공
  - 기본 동작: 라이브 스트림(live stream) → 새로운 ID가 나타날 때마다 방출(emit)
- `currentPersistenceIds()`: 라이브 업데이트 없이 현재 존재하는 ID만 반환

Scala:
```scala
readJournal.persistenceIds()
readJournal.currentPersistenceIds()
```

Java:
```java
readJournal.persistenceIds();
readJournal.currentPersistenceIds();
```

##### EventsByPersistenceIdQuery 와 CurrentEventsByPersistenceIdQuery

- `eventsByPersistenceId()`: "이벤트 소스 액터(event sourced actor)를 재생(replay)하는 것과 동등하게" 동작
  - 새로 들어오는 이벤트 관찰을 위해 스트림 유지

Scala:
```scala
readJournal.eventsByPersistenceId(
  "user-us-1337", fromSequenceNr = 0L, toSequenceNr = Long.MaxValue)
```

Java:
```java
readJournal.eventsByPersistenceId("user-us-1337", 0L, Long.MAX_VALUE);
```

- 대부분의 저널은 폴링(polling) 사용 → `refresh-interval` 속성으로 설정 가능
- `currentEventsByPersistenceId()`: 라이브 스트리밍 없이 스냅샷(snapshot)과 유사한 동작 제공

##### EventsByTag 와 CurrentEventsByTag

- "어떤 `persistenceId` 에 연관되어 있는지와 무관하게 이벤트를 쿼리"
  - → 애그리거트 루트(aggregate root)에 걸친 도메인 주도(domain-driven) 이벤트 쿼리 가능

이벤트 태깅(tagging) 구현:

Scala:
```scala
val NumberOfEntityGroups = 10

def tagEvent(entityId: String, event: Event): Set[String] = {
  val entityGroup = s"group-${math.abs(entityId.hashCode % NumberOfEntityGroups)}"
  event match {
    case _: OrderCompleted => Set(entityGroup, "order-completed")
    case _                 => Set(entityGroup)
  }
}

def apply(entityId: String): Behavior[Command] = {
  EventSourcedBehavior[Command, Event, State](
    persistenceId = PersistenceId("ShoppingCart", entityId),
    emptyState = State(),
    commandHandler = (state, cmd) => ???,
    eventHandler = (state, evt) => ???)
    .withTagger(event => tagEvent(entityId, event))
}
```

Java:
```java
private final String entityId;
public static final int NUMBER_OF_ENTITY_GROUPS = 10;

@Override
public Set<String> tagsFor(Event event) {
  String entityGroup = "group-" + 
    Math.abs(entityId.hashCode() % NUMBER_OF_ENTITY_GROUPS);
  Set<String> tags = new HashSet<>();
  tags.add(entityGroup);
  if (event instanceof OrderCompleted) tags.add("order-completed");
  return tags;
}
```

태깅된 이벤트 쿼리:

Scala:
```scala
val completedOrders: Source[EventEnvelope, NotUsed] =
  readJournal.eventsByTag("order-completed", Offset.noOffset)

val firstCompleted: Future[Vector[OrderCompleted]] =
  completedOrders
    .map(_.event)
    .collectType[OrderCompleted]
    .take(10)
    .runFold(Vector.empty[OrderCompleted])(_ :+ _)

val furtherOrders = 
  readJournal.eventsByTag("order-completed", offset = Sequence(10))
```

Java:
```java
final Source<EventEnvelope, NotUsed> completedOrders =
  readJournal.eventsByTag("order-completed", new Sequence(0L));

final CompletionStage<List<OrderCompleted>> firstCompleted =
  completedOrders
    .map(EventEnvelope::event)
    .collectType(OrderCompleted.class)
    .take(10)
    .runFold(
      new ArrayList<>(10),
      (acc, e) -> {
        acc.add(e);
        return acc;
      },
      system);

Source<EventEnvelope, NotUsed> furtherOrders =
  readJournal.eventsByTag("order-completed", new Sequence(10));
```

중요한 고려 사항: "EventsByTag 처럼 여러 persistenceId 에 걸친 쿼리를 사용할 때 매우 중요하게 염두에 두어야 할 점은, 이벤트가 스트림에 나타나는 순서가 보장되는 경우가 드물다는 것이다(그리고 여러 번의 구체화(materialization) 사이에서도 안정적이지 않다)."

- 저널은 특정 순서 보장(ordering guarantee)을 문서화할 수도 있음

##### EventsBySlice 와 CurrentEventsBySlice

- 지정된 엔티티 타입(entity type)과 슬라이스 범위(slice range)에 대한 이벤트 쿼리
- "슬라이스(slice)는 영속 id를 기반으로 결정론적으로(deterministically) 정의된다. 그 목적은 모든 영속 id를 슬라이스 전반에 고르게 분산시키는 것이다."
- 변형(variation):
  - `EventsBySliceStartingFromSnapshotsQuery` / `CurrentEventsBySliceStartingFromSnapshotsQuery`
  - `EventsBySliceFirehoseQuery` — 여러 소비자(consumer)에 대한 확장성 개선용

---

#### 1.6 구체화된 값(Materialized Values)

- 저널은 스트림 구체화(stream materialization)를 통해 추가적인 메타데이터 제공
- 구체화된 값의 타입: 반환된 `Source` 의 두 번째 타입 매개변수

Scala:
```scala
final case class RichEvent(tags: Set[String], payload: Any)

case class QueryMetadata(deterministicOrder: Boolean, infinite: Boolean)

def byTagsWithMeta(tags: Set[String]): Source[RichEvent, QueryMetadata] = {
  // implementation
}

val query: Source[RichEvent, QueryMetadata] =
  readJournal.byTagsWithMeta(Set("red", "blue"))

query
  .mapMaterializedValue { meta =>
    println(
      s"The query is: " +
      s"ordered deterministically: ${meta.deterministicOrder}, " +
      s"infinite: ${meta.infinite}")
  }
  .map { event =>
    println(s"Event payload: ${event.payload}")
  }
  .runWith(Sink.ignore)
```

Java:
```java
static class RichEvent {
  public final Set<String> tags;
  public final Object payload;

  public RichEvent(Set<String> tags, Object payload) {
    this.tags = tags;
    this.payload = payload;
  }
}

static final class QueryMetadata {
  public final boolean deterministicOrder;
  public final boolean infinite;

  public QueryMetadata(boolean deterministicOrder, boolean infinite) {
    this.deterministicOrder = deterministicOrder;
    this.infinite = infinite;
  }
}

public Source<RichEvent, QueryMetadata> byTagsWithMeta(Set<String> tags) {
  // implementation
}

Set<String> tags = new HashSet<String>();
tags.add("red");
tags.add("blue");
final Source<RichEvent, QueryMetadata> events =
  readJournal
    .byTagsWithMeta(tags)
    .mapMaterializedValue(
      meta -> {
        System.out.println(
          "The query is: "
          + "ordered deterministically: "
          + meta.deterministicOrder
          + " "
          + "infinite: "
          + meta.infinite);
        return meta;
      });

events
  .map(
    event -> {
      System.out.println("Event payload: " + event.payload);
      return event.payload;
    })
  .runWith(Sink.ignore(), system);
```

---

#### 1.7 성능과 비정규화(Denormalization)

- 쓰기 측(write-side)과 읽기 측(read-side)은 근본적으로 서로 다른 최적화 요구 사항을 가짐
  - → 관심사(concern)를 전용 데이터 저장소로 분리하면 각각에 대해 최적의 성능 달성 가능
- 쓰기 측(Write-side): 처리량(throughput)과 빠른 확인(acknowledgment)을 우선시
- 읽기 측(Read-side): 표현력 있는 쿼리 능력(query capability) 요구

- "구체화된 뷰(Materialized View)": "쿼리 결과의 영속적인 저장(persistent storage)" 의미
  - 소스 이벤트로부터 매번 재계산하는 대신 효율적인 반복 접근 가능

##### Reactive Streams 호환 데이터 저장소

- 대상 데이터 저장소가 Reactive Streams 를 구현하는 경우:

Scala:
```scala
implicit val system: ActorSystem = ActorSystem()

val readJournal =
  PersistenceQuery(system).readJournalFor[MyScaladslReadJournal](JournalId)
val dbBatchWriter: Subscriber[immutable.Seq[Any]] =
  ReactiveStreamsCompatibleDBDriver.batchWriter

readJournal
  .eventsByPersistenceId(
    "user-1337", fromSequenceNr = 0L, toSequenceNr = Long.MaxValue)
  .map(envelope => envelope.event)
  .map(convertToReadSideTypes)
  .grouped(20)
  .runWith(Sink.fromSubscriber(dbBatchWriter))
```

Java:
```java
final ReactiveStreamsCompatibleDBDriver driver = 
  new ReactiveStreamsCompatibleDBDriver();
final Subscriber<List<Object>> dbBatchWriter = driver.batchWriter();

readJournal
  .eventsByPersistenceId("user-1337", 0L, Long.MAX_VALUE)
  .map(envelope -> envelope.event())
  .grouped(20)
  .runWith(Sink.fromSubscriber(dbBatchWriter), system);
```

##### mapAsync 통합

- Reactive Streams 지원이 없는 데이터베이스의 경우:

Scala:
```scala
trait ExampleStore {
  def save(event: Any): Future[Unit]
}

val store: ExampleStore = ???

readJournal
  .eventsByTag("bid", NoOffset)
  .mapAsync(1) { e =>
    store.save(e)
  }
  .runWith(Sink.ignore)
```

Java:
```java
static class ExampleStore {
  CompletionStage<Void> save(Object any) {
    // ...
  }
}

final ExampleStore store = new ExampleStore();

readJournal
  .eventsByTag("bid", new Sequence(0L))
  .mapAsync(1, store::save)
  .runWith(Sink.ignore(), system);
```

##### 재개 가능한 프로젝션(Resumable Projections)

- 오프셋(offset)을 영속화하는 상태 기반 프로젝션의 경우: "처리된 이벤트의 시퀀스 번호(sequence number, 또는 `offset`)가 저장되고, 다음번에 이 프로젝션이 시작될 때 사용된다."
- [Akka Projections](https://doc.akka.io/libraries/akka-projection/current/) 모듈이 이 패턴 구현

---

#### 1.8 쿼리 플러그인

- 읽기 저널 구현체는 특정 데이터 저장소를 대상으로 하는 커뮤니티 플러그인(community plugin)으로 제공
- 전체 목록: [Community Plugins](https://akka.io/community/#plugins-to-akka-persistence-query) 페이지

##### ReadJournal 플러그인 API

- 플러그인은 `akka.persistence.query.ReadJournalProvider` 를 구현해야 함
  - `scaladsl.ReadJournal` 과 `javadsl.ReadJournal` 인스턴스를 모두 생성해야 함 → 두 인스턴스의 `Source` 타입이 다름

Scala 예제:
```scala
class MyReadJournalProvider(system: ExtendedActorSystem, config: Config) 
    extends ReadJournalProvider {

  private val readJournal: MyScaladslReadJournal =
    new MyScaladslReadJournal(system, config)

  override def scaladslReadJournal(): MyScaladslReadJournal =
    readJournal

  override def javadslReadJournal(): MyJavadslReadJournal =
    new MyJavadslReadJournal(readJournal)
}

class MyScaladslReadJournal(system: ExtendedActorSystem, config: Config)
    extends akka.persistence.query.scaladsl.ReadJournal
    with akka.persistence.query.scaladsl.EventsByTagQuery
    with akka.persistence.query.scaladsl.EventsByPersistenceIdQuery
    with akka.persistence.query.scaladsl.PersistenceIdsQuery
    with akka.persistence.query.scaladsl.CurrentPersistenceIdsQuery {

  private val refreshInterval: FiniteDuration =
    config.getDuration("refresh-interval", MILLISECONDS).millis

  override def eventsByTag(tag: String, offset: Offset): 
      Source[EventEnvelope, NotUsed] = offset match {
    case Sequence(offsetValue) =>
      Source.fromGraph(new MyEventsByTagSource(tag, offsetValue, refreshInterval))
    case NoOffset => eventsByTag(tag, Sequence(0L))
    case _ =>
      throw new IllegalArgumentException(
        "MyJournal does not support " + offset.getClass.getName + " offsets")
  }

  override def eventsByPersistenceId(
      persistenceId: String,
      fromSequenceNr: Long,
      toSequenceNr: Long): Source[EventEnvelope, NotUsed] = {
    ???
  }

  override def persistenceIds(): Source[String, NotUsed] = {
    ???
  }

  override def currentPersistenceIds(): Source[String, NotUsed] = {
    ???
  }

  def byTagsWithMeta(tags: Set[String]): Source[RichEvent, QueryMetadata] = {
    ???
  }
}

class MyJavadslReadJournal(scaladslReadJournal: MyScaladslReadJournal)
    extends akka.persistence.query.javadsl.ReadJournal
    with akka.persistence.query.javadsl.EventsByTagQuery
    with akka.persistence.query.javadsl.EventsByPersistenceIdQuery
    with akka.persistence.query.javadsl.PersistenceIdsQuery
    with akka.persistence.query.javadsl.CurrentPersistenceIdsQuery {

  override def eventsByTag(tag: String, offset: Offset = Sequence(0L)): 
      javadsl.Source[EventEnvelope, NotUsed] =
    scaladslReadJournal.eventsByTag(tag, offset).asJava

  override def eventsByPersistenceId(
      persistenceId: String,
      fromSequenceNr: Long = 0L,
      toSequenceNr: Long = Long.MaxValue): 
      javadsl.Source[EventEnvelope, NotUsed] =
    scaladslReadJournal.eventsByPersistenceId(
      persistenceId, fromSequenceNr, toSequenceNr).asJava

  override def persistenceIds(): javadsl.Source[String, NotUsed] =
    scaladslReadJournal.persistenceIds().asJava

  override def currentPersistenceIds(): javadsl.Source[String, NotUsed] =
    scaladslReadJournal.currentPersistenceIds().asJava

  def byTagsWithMeta(tags: java.util.Set[String]): 
      javadsl.Source[RichEvent, QueryMetadata] = {
    import scala.jdk.CollectionConverters._
    scaladslReadJournal.byTagsWithMeta(tags.asScala.toSet).asJava
  }
}
```

Java 예제:
```java
static class MyReadJournalProvider implements ReadJournalProvider {
  private final MyJavadslReadJournal javadslReadJournal;

  public MyReadJournalProvider(ExtendedActorSystem system, Config config) {
    this.javadslReadJournal = new MyJavadslReadJournal(system, config);
  }

  @Override
  public MyScaladslReadJournal scaladslReadJournal() {
    return new MyScaladslReadJournal(javadslReadJournal);
  }

  @Override
  public MyJavadslReadJournal javadslReadJournal() {
    return this.javadslReadJournal;
  }
}

static class MyJavadslReadJournal
    implements akka.persistence.query.javadsl.ReadJournal,
        akka.persistence.query.javadsl.EventsByTagQuery,
        akka.persistence.query.javadsl.EventsByPersistenceIdQuery,
        akka.persistence.query.javadsl.PersistenceIdsQuery,
        akka.persistence.query.javadsl.CurrentPersistenceIdsQuery {

  private final Duration refreshInterval;
  private Connection conn;

  public MyJavadslReadJournal(ExtendedActorSystem system, Config config) {
    refreshInterval = config.getDuration("refresh-interval");
  }

  @Override
  public Source<EventEnvelope, NotUsed> eventsByTag(
      String tag, Offset offset) {
    if (offset instanceof Sequence) {
      Sequence sequenceOffset = (Sequence) offset;
      return Source.fromGraph(
        new MyEventsByTagSource(
          conn, tag, sequenceOffset.value(), refreshInterval));
    } else if (offset == NoOffset.getInstance())
      return eventsByTag(tag, Offset.sequence(0L));
    else
      throw new IllegalArgumentException(
        "MyJavadslReadJournal does not support " + 
        offset.getClass().getName() + " offsets");
  }

  @Override
  public Source<EventEnvelope, NotUsed> eventsByPersistenceId(
      String persistenceId, long fromSequenceNr, long toSequenceNr) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public Source<String, NotUsed> persistenceIds() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public Source<String, NotUsed> currentPersistenceIds() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  public Source<RichEvent, QueryMetadata> byTagsWithMeta(Set<String> tags) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}

static class MyScaladslReadJournal
    implements akka.persistence.query.scaladsl.ReadJournal,
        akka.persistence.query.scaladsl.EventsByTagQuery,
        akka.persistence.query.scaladsl.EventsByPersistenceIdQuery,
        akka.persistence.query.scaladsl.PersistenceIdsQuery,
        akka.persistence.query.scaladsl.CurrentPersistenceIdsQuery {

  private final MyJavadslReadJournal javadslReadJournal;

  public MyScaladslReadJournal(MyJavadslReadJournal javadslReadJournal) {
    this.javadslReadJournal = javadslReadJournal;
  }

  @Override
  public akka.stream.scaladsl.Source<EventEnvelope, NotUsed> eventsByTag(
      String tag, akka.persistence.query.Offset offset) {
    return javadslReadJournal.eventsByTag(tag, offset).asScala();
  }

  @Override
  public akka.stream.scaladsl.Source<EventEnvelope, NotUsed> 
      eventsByPersistenceId(
      String persistenceId, long fromSequenceNr, long toSequenceNr) {
    return javadslReadJournal
      .eventsByPersistenceId(persistenceId, fromSequenceNr, toSequenceNr)
      .asScala();
  }

  @Override
  public akka.stream.scaladsl.Source<String, NotUsed> persistenceIds() {
    return javadslReadJournal.persistenceIds().asScala();
  }

  @Override
  public akka.stream.scaladsl.Source<String, NotUsed> 
      currentPersistenceIds() {
    return javadslReadJournal.currentPersistenceIds().asScala();
  }

  public akka.stream.scaladsl.Source<RichEvent, QueryMetadata> 
      byTagsWithMeta(scala.collection.Set<String> tags) {
    Set<String> jTags = 
      scala.jdk.javaapi.CollectionConverters.asJava(tags);
    return javadslReadJournal.byTagsWithMeta(jTags).asScala();
  }
}
```

`eventsByTag` 를 위한 GraphStage 구현:

Scala:
```scala
class MyEventsByTagSource(tag: String, offset: Long, refreshInterval: FiniteDuration)
    extends GraphStage[SourceShape[EventEnvelope]] {

  private case object Continue
  val out: Outlet[EventEnvelope] = Outlet("MyEventByTagSource.out")
  override def shape: SourceShape[EventEnvelope] = SourceShape(out)

  override protected def initialAttributes: Attributes = 
    Attributes(ActorAttributes.IODispatcher)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new TimerGraphStageLogic(shape) with OutHandler {
      lazy val system = materializer.system
      private val Limit = 1000
      private val connection: java.sql.Connection = ???
      private var currentOffset = offset
      private var buf = Vector.empty[EventEnvelope]
      private val serialization = SerializationExtension(system)

      override def preStart(): Unit = {
        scheduleWithFixedDelay(Continue, refreshInterval, refreshInterval)
      }

      override def onPull(): Unit = {
        query()
        tryPush()
      }

      override def onDownstreamFinish(cause: Throwable): Unit = {
        // close connection if responsible for doing so
      }

      private def query(): Unit = {
        if (buf.isEmpty) {
          try {
            buf = Select.run(tag, currentOffset, Limit)
          } catch {
            case NonFatal(e) =>
              failStage(e)
          }
        }
      }

      private def tryPush(): Unit = {
        if (buf.nonEmpty && isAvailable(out)) {
          push(out, buf.head)
          buf = buf.tail
        }
      }

      override protected def onTimer(timerKey: Any): Unit = timerKey match {
        case Continue =>
          query()
          tryPush()
      }

      object Select {
        private def statement() =
          connection.prepareStatement("""
            SELECT id, persistence_id, seq_nr, serializer_id, 
                   serializer_manifest, payload
            FROM journal WHERE tag = ? AND id > ?
            ORDER BY id LIMIT ?
          """)

        def run(tag: String, from: Long, limit: Int): Vector[EventEnvelope] = {
          val s = statement()
          try {
            s.setString(1, tag)
            s.setLong(2, from)
            s.setLong(3, limit)
            val rs = s.executeQuery()

            val b = Vector.newBuilder[EventEnvelope]
            while (rs.next()) {
              val deserialized = serialization
                .deserialize(
                  rs.getBytes("payload"), 
                  rs.getInt("serializer_id"), 
                  rs.getString("serializer_manifest"))
                .get
              currentOffset = rs.getLong("id")
              b += EventEnvelope(
                Offset.sequence(currentOffset),
                rs.getString("persistence_id"),
                rs.getLong("seq_nr"),
                deserialized,
                System.currentTimeMillis())
            }
            b.result()
          } finally s.close()
        }
      }
    }
}
```

Java:
```java
public class MyEventsByTagSource extends GraphStage<SourceShape<EventEnvelope>> {
  public Outlet<EventEnvelope> out = Outlet.create("MyEventByTagSource.out");
  private static final String QUERY =
    "SELECT id, persistence_id, seq_nr, serializer_id, " +
    "serializer_manifest, payload " +
    "FROM journal WHERE tag = ? AND id > ? " +
    "ORDER BY id LIMIT ?";

  enum Continue {
    INSTANCE;
  }

  private static final int LIMIT = 1000;
  private final Connection connection;
  private final String tag;
  private final long initialOffset;
  private final Duration refreshInterval;

  public MyEventsByTagSource(
      Connection connection, String tag, long initialOffset, 
      Duration refreshInterval) {
    this.connection = connection;
    this.tag = tag;
    this.initialOffset = initialOffset;
    this.refreshInterval = refreshInterval;
  }

  @Override
  public Attributes initialAttributes() {
    return Attributes.apply(ActorAttributes.IODispatcher());
  }

  @Override
  public SourceShape<EventEnvelope> shape() {
    return SourceShape.of(out);
  }

  @Override
  public GraphStageLogic createLogic(Attributes inheritedAttributes) {
    return new TimerGraphStageLogic(shape()) {
      private ActorSystem system = materializer().system();
      private long currentOffset = initialOffset;
      private List<EventEnvelope> buf = new LinkedList<>();
      private final Serialization serialization = SerializationExtension.get(system);

      @Override
      public void preStart() {
        scheduleWithFixedDelay(Continue.INSTANCE, refreshInterval, refreshInterval);
      }

      @Override
      public void onTimer(Object timerKey) {
        query();
        deliver();
      }

      private void deliver() {
        if (isAvailable(out) && !buf.isEmpty()) {
          push(out, buf.remove(0));
        }
      }

      private void query() {
        if (buf.isEmpty()) {
          try (PreparedStatement s = connection.prepareStatement(QUERY)) {
            s.setString(1, tag);
            s.setLong(2, currentOffset);
            s.setLong(3, LIMIT);
            try (ResultSet rs = s.executeQuery()) {
              final List<EventEnvelope> res = new ArrayList<>(LIMIT);
              while (rs.next()) {
                Object deserialized =
                  serialization
                    .deserialize(
                      rs.getBytes("payload"),
                      rs.getInt("serializer_id"),
                      rs.getString("serializer_manifest"))
                    .get();
                currentOffset = rs.getLong("id");
                res.add(
                  new EventEnvelope(
                    Offset.sequence(currentOffset),
                    rs.getString("persistence_id"),
                    rs.getLong("seq_nr"),
                    deserialized,
                    System.currentTimeMillis()));
              }
              buf = res;
            }
          } catch (Exception e) {
            failStage(e);
          }
        }
      }

      {
        setHandler(
          out,
          new AbstractOutHandler() {
            @Override
            public void onPull() {
              query();
              deliver();
            }
          });
      }
    };
  }
}
```

##### 플러그인 생성자 요구 사항

- `ReadJournalProvider` 클래스는 다음 시그니처 중 하나에 부합하는 생성자를 가져야 함
  - `ExtendedActorSystem`, `Config`, `String` 매개변수를 받는 생성자
  - `ExtendedActorSystem`, `Config` 매개변수를 받는 생성자
  - `ExtendedActorSystem` 매개변수 하나만 받는 생성자
  - 매개변수가 없는 생성자

- 유한한 결과 집합(finite result set)만 지원하는 데이터 저장소의 경우
  - 저널은 무한 스트림(infinite stream)을 모사하기 위해 주기적인 쿼리를 발행해야 함
  - 설정 가능한 `refresh-interval` 속성 사용

---

#### 1.9 스케일 아웃(Scaling Out)

- 높은 이벤트 볼륨이나 복원력(resilience) 요구 사항 → "클러스터 샤딩(Cluster Sharding)을 이벤트 태깅과 함께 사용하면 이벤트를 클러스터 전반에 걸쳐 샤딩(shard)하는 데 매우 적합하다."
  - → 수평 확장과 노드 장애로부터의 복구 가능

---

#### 1.10 예제 프로젝트

- [Akka로 만드는 마이크로서비스 튜토리얼(Microservices with Akka tutorial)](https://doc.akka.io/libraries/guide/microservices-tutorial/)
  - CQRS 패턴을 사용한 이벤트 소싱 시연
  - 이벤트 스트림으로부터 대안적인 데이터 표현(alternative data representation)을 구축하기 위한 Projections 통합 시연

---
### 2. 영속성 플러그인(Persistence Plugins)

#### 2.1 개요

- Akka 영속성 → 저널(journal)·스냅샷 스토어(snapshot store)·영속 상태 스토어(durable state store)·영속성 쿼리(persistence query)용 플러그형(pluggable) 스토리지 백엔드 지원
- Akka 팀 → 여러 프로덕션 준비된(production-ready) 플러그인 유지보수

---

#### 2.2 유지보수되는 플러그인

##### R2DBC 플러그인

- 반응형 데이터베이스 드라이버(Reactive database drivers, R2DBC) → PostgreSQL·H2(최소한의 인-프로세스 메모리 또는 파일 기반 데이터베이스로서)·Yugabyte 등 관계형 데이터베이스 지원
- 이 구현체 → Akka Persistence의 최신 기능 지원
- 일반적으로 JDBC 기반 플러그인보다 권장

##### Cassandra 플러그인

- Akka → 전용 Cassandra 영속성 구현체를 통해 지원 제공
- Durable State 포함 일부 최신 기능 → 이 플러그인에서 미지원

##### AWS DynamoDB 플러그인

- DynamoDB → 백엔드로 사용 가능
- 미지원 기능: Durable State · 마지막 이벤트만으로부터의 복구(recovery from only the last event)

##### JDBC 플러그인

- JDBC 드라이버를 사용하는 관계형 데이터베이스 → 이 구현체를 통해 지원
- 신규 프로젝트 → 일부 후속 기능 추가 사항 미지원 → R2DBC 옵션 권장

---

#### 2.3 기능 제한 사항

- Cassandra 및 JDBC 플러그인 → 다음 기능 미지원
  - `eventsBySlices` 쿼리
  - gRPC를 통한 Projections
  - gRPC를 통한 복제된 이벤트 소싱(Replicated Event Sourcing)
  - 동적 Projection 스케일링
  - 저지연(low latency) Projections
  - 스냅샷으로부터의 Projections
  - Durable State 엔티티

---

#### 2.4 플러그인 설정

##### 기본값 vs 개별 선택

- 영속 액터가 `journalPluginId` 와 `snapshotPluginId` 메서드를 오버라이드하지 않는 경우 → 시스템은 다음으로 설정된 기본 플러그인 사용

```
akka.persistence.journal.plugin = ""
akka.persistence.snapshot-store.plugin = ""
akka.persistence.state.plugin = ""
```

- 이들 → 사용자의 `application.conf` 에서 명시적으로 설정 필요

##### 즉시 초기화(Eager Initialization)

- 기본 → 플러그인은 필요할 때(on-demand) 시작
- 즉시 초기화(eager initialization) 활성화 방법
  - `akka.extensions` 아래에 `akka.persistence.Persistence` 추가
  - `akka.persistence.journal.auto-start-journals` 와 `akka.persistence.snapshot-store.auto-start-snapshot-stores` 아래에 플러그인 ID 지정

설정 예시:

```
akka {
  extensions = [akka.persistence.Persistence]
  persistence {
    journal {
      plugin = "akka.persistence.journal.leveldb"
      auto-start-journals = ["akka.persistence.journal.leveldb"]
    }
    snapshot-store {
      plugin = "akka.persistence.snapshot-store.local"
      auto-start-snapshot-stores = ["akka.persistence.snapshot-store.local"]
    }
  }
}
```

---

#### 2.5 사전 패키징된 플러그인

##### 로컬 LevelDB 저널(Local LevelDB Journal)

- 경고: 사용 중단(Deprecation) — "LevelDB 저널은 사용 중단(deprecated)되었으며 이를 사용하여 새 애플리케이션을 구축하는 것은 권장되지 않는다. 대체재로 Akka Persistence JDBC 사용을 권장한다."
- LevelDB → 로컬 파일시스템 스토리지 사용 → Akka 클러스터에서 사용 불가
- 활성화 방법:

```
akka.persistence.journal.plugin = "akka.persistence.journal.leveldb"
```

필요한 의존성:

sbt:
```
libraryDependencies += "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"
```

Maven:
```xml
<dependency>
  <groupId>org.fusesource.leveldbjni</groupId>
  <artifactId>leveldbjni-all</artifactId>
  <version>1.8</version>
</dependency>
```

Gradle:
```
implementation "org.fusesource.leveldbjni:leveldbjni-all:1.8"
```

스토리지 위치 설정:

```
akka.persistence.journal.leveldb.dir = "target/journal"
```

- LevelDB → 메시지를 제거하는 대신 "툼스톤(tombstone)"을 추가하는 방식으로 삭제 처리
- 잦은 삭제가 동반되는 과중한 사용의 경우 → 저널 압축(journal compaction) 사용 가능:

```
akka.persistence.journal.leveldb.compaction-intervals {
  persistence-id-1 = 100
  persistence-id-2 = 200
  persistence-id-N = 1000
  "*" = 250
}
```

##### 공유 LevelDB 저널(Shared LevelDB Journal)

- 경고: 사용 중단(Deprecated) — "영속성 플러그인 프록시(Persistence Plugin Proxy)로 대체되었다."
- 공유 스토어 인스턴스 생성

Scala:
```scala
import akka.persistence.journal.leveldb.SharedLeveldbStore
val store = system.actorOf(Props[SharedLeveldbStore](), "store")
```

Java:
```java
final ActorRef store = system.actorOf(Props.create(SharedLeveldbStore.class), "store");
```

스토리지 위치 설정:

```
akka.persistence.journal.leveldb-shared.store.dir = "target/shared"
```

플러그인 활성화:

```
akka.persistence.journal.plugin = "akka.persistence.journal.leveldb-shared"
```

- `SharedLeveldbJournal.setStore()` 를 통해 스토어 참조를 주입(inject)

Scala:
```scala
trait SharedStoreUsage extends Actor {
  override def preStart(): Unit = {
    context.actorSelection("akka://example@127.0.0.1:2552/user/store") ! Identify(1)
  }
  def receive = {
    case ActorIdentity(1, Some(store)) =>
      SharedLeveldbJournal.setStore(store, context.system)
  }
}
```

Java:
```java
class SharedStorageUsage extends AbstractActor {
  @Override
  public void preStart() throws Exception {
    String path = "akka://example@127.0.0.1:2552/user/store";
    ActorSelection selection = getContext().actorSelection(path);
    selection.tell(new Identify(1), getSelf());
  }
  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(
            ActorIdentity.class,
            ai -> {
              if (ai.correlationId().equals(1)) {
                Optional<ActorRef> store = ai.getActorRef();
                if (store.isPresent()) {
                  SharedLeveldbJournal.setStore(store.get(), getContext().getSystem());
                } else {
                  throw new RuntimeException("Couldn't identify store");
                }
              }
            })
        .build();
  }
}
```

- 주입 → 멱등(idempotent) · 완료 전까지 내부 명령 버퍼링

##### 로컬 스냅샷 스토어(Local Snapshot Store)

- 경고: 로컬 파일시스템 제약 → Akka 클러스터에서 사용 불가
- 활성화 방법:

```
akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
```

위치 설정:

```
akka.persistence.snapshot-store.local.dir = "target/snapshots"
```

- 스냅샷 미사용 시 → 스냅샷 스토어 지정은 선택 사항

---

#### 2.6 영속성 플러그인 프록시(Persistence Plugin Proxy)

- 테스트용 → 이 프록시는 "단일 노드의 저널과 스냅샷 스토어를 여러 액터 시스템(같은 노드 또는 다른 노드 상의)에 걸쳐 공유할 수 있게 한다."
- 경고: "공유 저널/스냅샷 스토어는 단일 장애 지점(single point of failure)이며 테스트 목적으로만 사용해야 한다."
- 설정 방법
  - `akka.persistence.journal.proxy` 와 `akka.persistence.snapshot-store.proxy` 항목을 통해 설정
  - `target-journal-plugin` 또는 `target-snapshot-store-plugin` 을 기반 플러그인(예: `akka.persistence.journal.inmem`)으로 설정
  - 하나의 시스템에서 `start-target-journal` 또는 `start-target-snapshot-store` 를 `on` 으로 설정 → 대상 인스턴스 생성 활성화
- 공유 플러그인 → `target-journal-address` 및 `target-snapshot-store-address` 설정 키를 통해, 또는 프로그래밍 방식으로 `PersistencePluginProxy.setTargetLocation()` 을 사용하여 대상 위치 탐색
- Akka → 확장(extension)을 지연 로딩(lazily) → 대상 플러그인 로드 보장이 필요하면 `PersistencePluginProxyExtension` 을 인스턴스화하거나 `PersistencePluginProxy.start()` 호출

---

#### 2.7 커스텀 저장소 백엔드 구현(Building a Storage Backend)

> 원본: https://doc.akka.io/libraries/akka-core/current/persistence-journals.html

- 미리 제공되는 플러그인이 요구사항에 맞지 않는 경우 → 저널(journal)·스냅샷 스토어를 직접 구현 가능
- 플러그인 API는 공개(public) API임 → 서드파티(third-party) 구현체를 자유롭게 만들 수 있음

##### 저널 플러그인 API

- `AsyncWriteJournal` 을 확장하여 다음 메서드 구현
  - `asyncWriteMessages()`: 영속 메시지의 배치(batch) 쓰기를 처리함. 원자적(atomic) 쓰기가 보장되어야 함
  - `asyncDeleteMessagesTo()`: 지정한 시퀀스 번호까지의 메시지를 삭제함
  - `asyncReplayMessages()`: 액터 복구(recovery)를 위해 저장된 메시지를 재생(replay)함
  - `asyncReadHighestSequenceNr()`: 저장된 가장 높은 시퀀스 번호를 조회함
- 핵심 요구사항
  - 동일한 `persistenceId` 에 대한 쓰기는 직렬화(serialize)되어야 함(순서를 지켜 하나씩 처리)
  - 시퀀스 번호의 정합성을 유지해야 함
  - 완전 비동기(async)로 동작하는 스토리지든, 블로킹(blocking) API만 제공하는 스토리지든 모두 감쌀 수 있어야 함

##### 스냅샷 스토어 플러그인 API

- `SnapshotStore` 액터를 확장하여 다음 메서드 구현
  - `loadAsync()`: 선택 조건(selection criteria)에 맞는 스냅샷을 조회함
  - `saveAsync()`: 스냅샷을 비동기로 저장함
  - `deleteAsync(메타데이터)`: 스냅샷 하나를 삭제함
  - `deleteAsync(조건)`: 조건에 맞는 여러 스냅샷을 한꺼번에 삭제함

##### 플러그인 설정

- 플러그인은 클래스 이름과 디스패처(dispatcher)만 지정하면 최소한의 설정으로 활성화됨
- 생성자 시그니처는 다음 세 가지 형태 중 하나를 지원
  - `(Config, String)`: 설정 객체와 설정 경로(path)를 함께 받음
  - `(Config)`: 설정 객체만 받음
  - 인자 없음(parameterless)

##### 기술 호환성 키트(Technology Compatibility Kit, TCK)

- `akka-persistence-tck` 의존성이 제공하는 표준 테스트 스위트로, 직접 만든 플러그인이 계약(contract)을 만족하는지 검증함
  - `JournalSpec`/`JavaJournalSpec`: 저널 구현체를 포괄적으로 검증함
  - `SnapshotStoreSpec`: 스냅샷 스토어 구현체를 검증함
  - `JournalPerfSpec`: 저널 구현체의 성능을 벤치마킹함
  - 선택적(optional) 기능은 캐퍼빌리티 플래그(capability flag)로 테스트 포함 여부를 제어함
- 비동기 퓨처(future) 기반 구현, 서킷 브레이커(circuit breaker)를 통한 보호, 명확한 오류 신호 전달, 테스트 인프라 준비를 위한 `beforeAll`/`afterAll` 생명주기 훅 사용을 권장함

##### 손상된 이벤트 로그(Corrupt Event Logs)

- 동일한 `persistenceId` 를 사용하는 여러 액터가 동시에 존재하는 등의 상황에서 저널이 손상될 수 있음
- 권장 사항: 저널 구현체는 이런 손상된 이벤트를 조용히 걸러내기(filter)보다는, 중복된 시퀀스 번호를 포함해 있는 그대로 전달하여 상위 계층(애플리케이션)이 어떻게 처리할지 판단할 수 있게 함

---

### 3. 스키마 진화(Schema Evolution)

#### 3.1 의존성 설정

##### sbt
```
val AkkaVersion = "2.10.19"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-persistence" % AkkaVersion,
  "com.typesafe.akka" %% "akka-persistence-testkit" % AkkaVersion % Test
)
```

##### Maven
```xml
<properties>
  <scala.binary.version>2.13</scala.binary.version>
</properties>
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.typesafe.akka</groupId>
      <artifactId>akka-bom_${scala.binary.version}</artifactId>
      <version>2.10.19</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
<dependencies>
  <dependency>
    <groupId>com.typesafe.akka</groupId>
    <artifactId>akka-persistence_${scala.binary.version}</artifactId>
  </dependency>
  <dependency>
    <groupId>com.typesafe.akka</groupId>
    <artifactId>akka-persistence-testkit_${scala.binary.version}</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

##### Gradle
```
def versions = [
  ScalaBinary: "2.13"
]
dependencies {
  implementation platform("com.typesafe.akka:akka-bom_${versions.ScalaBinary}:2.10.19")
  implementation "com.typesafe.akka:akka-persistence_${versions.ScalaBinary}"
  testImplementation "com.typesafe.akka:akka-persistence-testkit_${versions.ScalaBinary}"
}
```

---

#### 3.2 소개

- 스키마 진화(schema evolution) → 이벤트 소싱을 사용하는 장기 운영 프로젝트에서 핵심 요소
- 비즈니스 요구 사항·도메인에 대한 이해 발전 → 영속화된 이벤트의 데이터 구조도 그에 맞게 변화 필요
- 스키마 변경이 필요한 성공적인 프로젝트 → 활발한 사용과 지속적인 비즈니스 성장을 의미

---

#### 3.3 이벤트 소스 시스템에서의 스키마 진화

##### 근본적 특성

- 이벤트 소싱 → 스키마 진화와 관련하여 다음과 같은 특유의 도전 과제 수반
  - 시스템은 대규모 마이그레이션을 요구하지 않고 계속 운영되어야 함
  - 시스템은 스토리지로부터 오래된 이벤트를 읽어와 현재 형식으로 애플리케이션 로직에 제시해야 함
  - 이벤트는 복구(recovery)나 쿼리 시 최신 버전으로 투명하게(transparently) 변환(promote)되어야 함
  - 비즈니스 로직은 여러 이벤트 버전의 존재를 인식하지 않아도 되어야 함

##### 스키마 진화의 유형

- 가장 흔한 스키마 변경
  1. 이벤트 타입에 필드 추가
  2. 이벤트 타입의 필드 제거 또는 이름 변경
  3. 이벤트 타입 전체 제거
  4. 큰 이벤트를 여러 개의 작은 이벤트로 분할

---

#### 3.4 올바른 직렬화 형식 선택

##### 선택 시 고려 사항

- 직렬화 형식(serialization format) 선택 → 어떤 스키마 진화가 쉬운지·어려운지를 크게 좌우
- 중요한 고려 요소
  - 스키마 진화 능력
  - 새로운 데이터타입에 대한 개발 및 유지보수 노력
  - 직렬화 성능 특성
- 권장 접근: Jackson 직렬화 → 많은 경우에 좋은 스키마 진화 지원 제공

##### 직렬화 형식 옵션

- IDL 기반 바이너리 형식(IDL-based binary format) → 장기간 운영되는 애플리케이션에 적합
  - Google Protocol Buffers — 스키마 진화에 대해 더 많은 제어 제공
  - Apache Thrift — 유연한 IDL 기반 접근
  - Apache Avro — 스키마 레지스트리(schema registry)를 사용한 전체 스키마 기반 진화

##### 제공되는 기본 직렬화기(Provided Default Serializers)

- Akka Persistence → `PersistentRepr` 와 `AtomicWrite` 같은 내부 타입을 위해 Google Protocol Buffers 기반 직렬화기 제공
- 저널 플러그인 구현체 → 이 제공된 직렬화기를 사용하거나, 자신의 기반 데이터베이스에 더 적합한 형식 선택 가능
- 중요한 노트: "직렬화(Serialization)는 Akka Persistence 자체가 자동으로 처리하지 않는다." → 프레임워크는 직렬화기를 제공할 뿐 → 사용 여부는 저널 플러그인이 결정
- 일부 저널 → Akka Serialization 대신 해당 데이터 저장소에 적합한 형식(JSON 등) 사용 가능

##### 직렬화 엔벨로프(Envelope) 구조

- 프레임워크 → 사용자 페이로드(payload)를 영속성 메타데이터가 포함된 엔벨로프(envelope)로 감쌈
- 저널이 래퍼(wrapper) 타입에 제공된 Protobuf 직렬화기를 사용하는 경우 → 사용자 이벤트 페이로드는 설정된 직렬화기로 직렬화됨(별도로 지정하지 않으면 기본적으로 Java 직렬화 사용)
- 경고: 치명적 — 스키마 진화 지원이 빈약하고 성능 특성이 낮음 → 진지한 애플리케이션 개발에서 Java 직렬화에 의존하지 말 것

##### 페이로드 직렬화기 설정

- 커스텀 직렬화기 → Akka Serialization 설정을 통해 등록 가능
- 다음은 최소한의 예시

도메인 모델:

Scala:
```scala
final case class Person(name: String, surname: String)
```

Java:
```java
static class Person {
  public final String name;
  public final String surname;
  
  public Person(String name, String surname) {
    this.name = name;
    this.surname = surname;
  }
}
```

커스텀 직렬화기 구현:

Scala:
```scala
class SimplestPossiblePersonSerializer extends SerializerWithStringManifest {
  val Utf8 = Charset.forName("UTF-8")
  
  val PersonManifest = classOf[Person].getName
  
  def identifier = 1234567
  
  override def manifest(o: AnyRef): String = o.getClass.getName
  
  override def toBinary(obj: AnyRef): Array[Byte] = obj match {
    case p: Person => s"""${p.name}|${p.surname}""".getBytes(Utf8)
    case _         => throw new IllegalArgumentException(s"Unable to serialize to bytes, clazz was: ${obj.getClass}!")
  }
  
  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    manifest match {
      case PersonManifest =>
        val nameAndSurname = new String(bytes, Utf8)
        val Array(name, surname) = nameAndSurname.split("[|]")
        Person(name, surname)
      case _ =>
        throw new NotSerializableException(
          s"Unable to deserialize from bytes, manifest was: $manifest! Bytes length: " +
          bytes.length)
    }
}
```

Java:
```java
static class SimplestPossiblePersonSerializer extends SerializerWithStringManifest {
  private final Charset utf8 = StandardCharsets.UTF_8;
  
  private final String personManifest = Person.class.getName();
  
  @Override
  public int identifier() {
    return 1234567;
  }
  
  @Override
  public String manifest(Object o) {
    return o.getClass().getName();
  }
  
  @Override
  public byte[] toBinary(Object obj) {
    if (obj instanceof Person) {
      Person p = (Person) obj;
      return (p.name + "|" + p.surname).getBytes(utf8);
    } else {
      throw new IllegalArgumentException(
          "Unable to serialize to bytes, clazz was: " + obj.getClass().getName());
    }
  }
  
  @Override
  public Object fromBinary(byte[] bytes, String manifest) throws NotSerializableException {
    if (personManifest.equals(manifest)) {
      String nameAndSurname = new String(bytes, utf8);
      String[] parts = nameAndSurname.split("[|]");
      return new Person(parts[0], parts[1]);
    } else {
      throw new NotSerializableException(
          "Unable to deserialize from bytes, manifest was: "
              + manifest
              + "! Bytes length: "
              + bytes.length);
    }
  }
}
```

설정 (application.conf):
```
akka {
  actor {
    serializers {
      person = "docs.persistence.SimplestPossiblePersonSerializer"
    }
    
    serialization-bindings {
      "docs.persistence.Person" = person
    }
  }
}
```

---
#### 3.5 스키마 진화 실전

##### 필드 추가(Adding Fields)

상황: 기존 메시지 타입에 새 필드 추가. 예: `SeatReserved(letter: String, row: Int)` 에 창가 또는 통로 좌석을 나타내는 추가 `seatType` 필드가 필요한 경우.

해결책: 직렬화 형식이 누락된 필드에 대한 바이너리 호환성(binary compatibility)을 처리하도록 보장 → 필드는 선택적(optional)이어야 하며 적절한 기본값(default value)을 가져야 함.

도메인 모델:

Scala:
```scala
sealed abstract class SeatType { def code: String }
object SeatType {
  def fromString(s: String) = s match {
    case Window.code => Window
    case Aisle.code  => Aisle
    case Other.code  => Other
    case _           => Unknown
  }
  case object Window extends SeatType { override val code = "W" }
  case object Aisle extends SeatType { override val code = "A" }
  case object Other extends SeatType { override val code = "O" }
  case object Unknown extends SeatType { override val code = "" }
}

case class SeatReserved(letter: String, row: Int, seatType: SeatType)
```

Java:
```java
static enum SeatType {
  Window("W"),
  Aisle("A"),
  Other("O"),
  Unknown("");
  
  private final String code;
  
  private SeatType(String code) {
    this.code = code;
  }
  
  public static SeatType fromCode(String c) {
    if (Window.code.equals(c)) return Window;
    else if (Aisle.code.equals(c)) return Aisle;
    else if (Other.code.equals(c)) return Other;
    else return Unknown;
  }
}

static class SeatReserved {
  public final String letter;
  public final int row;
  public final SeatType seatType;
  
  public SeatReserved(String letter, int row, SeatType seatType) {
    this.letter = letter;
    this.row = row;
    this.seatType = seatType;
  }
}
```

Protocol Buffer 정의:
```
option java_package = "docs.persistence.proto";
option optimize_for = SPEED;

message SeatReserved {
  required string letter   = 1;
  required uint32 row      = 2;
  optional string seatType = 3; // the new field
}
```

선택적 필드 처리를 포함한 직렬화기:

Scala:
```scala
class AddedFieldsSerializerWithProtobuf extends SerializerWithStringManifest {
  override def identifier = 67876
  
  final val SeatReservedManifest = classOf[SeatReserved].getName
  
  override def manifest(o: AnyRef): String = o.getClass.getName
  
  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    manifest match {
      case SeatReservedManifest =>
        seatReserved(FlightAppModels.SeatReserved.parseFrom(bytes))
      case _ =>
        throw new NotSerializableException("Unable to handle manifest: " + manifest)
    }
  
  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case s: SeatReserved =>
      FlightAppModels.SeatReserved.newBuilder
        .setRow(s.row)
        .setLetter(s.letter)
        .setSeatType(s.seatType.code)
        .build()
        .toByteArray
  }
  
  private def seatReserved(p: FlightAppModels.SeatReserved): SeatReserved =
    SeatReserved(p.getLetter, p.getRow, seatType(p))
  
  private def seatType(p: FlightAppModels.SeatReserved): SeatType =
    if (p.hasSeatType) SeatType.fromString(p.getSeatType) else SeatType.Unknown
}
```

Java:
```java
static class AddedFieldsSerializerWithProtobuf extends SerializerWithStringManifest {
  @Override
  public int identifier() {
    return 67876;
  }
  
  private final String seatReservedManifest = SeatReserved.class.getName();
  
  @Override
  public String manifest(Object o) {
    return o.getClass().getName();
  }
  
  @Override
  public Object fromBinary(byte[] bytes, String manifest) throws NotSerializableException {
    if (seatReservedManifest.equals(manifest)) {
      try {
        return seatReserved(FlightAppModels.SeatReserved.parseFrom(bytes));
      } catch (InvalidProtocolBufferException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    } else {
      throw new NotSerializableException("Unable to handle manifest: " + manifest);
    }
  }
  
  @Override
  public byte[] toBinary(Object o) {
    if (o instanceof SeatReserved) {
      SeatReserved s = (SeatReserved) o;
      return FlightAppModels.SeatReserved.newBuilder()
          .setRow(s.row)
          .setLetter(s.letter)
          .setSeatType(s.seatType.code)
          .build()
          .toByteArray();
    } else {
      throw new IllegalArgumentException("Unable to handle: " + o);
    }
  }
  
  private SeatReserved seatReserved(FlightAppModels.SeatReserved p) {
    return new SeatReserved(p.getLetter(), p.getRow(), seatType(p));
  }
  
  private SeatType seatType(FlightAppModels.SeatReserved p) {
    if (p.hasSeatType()) return SeatType.fromCode(p.getSeatType());
    else return SeatType.Unknown;
  }
}
```

##### 필드 이름 변경(Renaming Fields)

상황: 도메인 개념을 더 잘 반영하기 위해 필드 이름 변경. 예: `code` 필드를 `seatNr` 로 변경.

해결책 1 - IDL 기반 직렬화기:
- IDL 기반 형식(Protocol Buffers 같은)에서는 필드 이름이 바이너리 표현에 절대 저장되지 않음 → 필드 이름 변경이 "공짜(free)"
- 오직 숫자 형태의 필드 식별자(numeric field identifier)만 중요

Protocol Buffer 예제:
```
// BEFORE:
message SeatReserved {
  required string code = 1;
}

// AFTER:
message SeatReserved {
  required string seatNr = 1; // field renamed, id remains the same
}
```

해결책 2 - JSON과 함께 수동 버전 관리:
- 자동 이름 변경을 지원하지 않는 직렬화 형식의 경우, EventAdapter로 스키마 버전 관리(versioning) 구현

JSON 이름 변경을 포함한 EventAdapter:

Scala:
```scala
class JsonRenamedFieldAdapter extends EventAdapter {
  val marshaller = new ExampleJsonMarshaller
  
  val V1 = "v1"
  val V2 = "v2"
  
  override def manifest(event: Any): String = V2
  
  override def toJournal(event: Any): JsObject =
    marshaller.toJson(event)
  
  override def fromJournal(event: Any, manifest: String): EventSeq = event match {
    case json: JsObject =>
      EventSeq(marshaller.fromJson(manifest match {
        case V1      => rename(json, "code", "seatNr")
        case V2      => json // pass-through
        case unknown => throw new IllegalArgumentException(s"Unknown manifest: $unknown")
      }))
    case _ =>
      val c = event.getClass
      throw new IllegalArgumentException("Can only work with JSON, was: %s".format(c))
  }
  
  def rename(json: JsObject, from: String, to: String): JsObject = {
    val value = json.fields(from)
    val withoutOld = json.fields - from
    JsObject(withoutOld + (to -> value))
  }
}
```

Java:
```java
static class JsonRenamedFieldAdapter implements EventAdapter {
  private final ExampleJsonMarshaller marshaller = new ExampleJsonMarshaller();
  
  private final String V1 = "v1";
  private final String V2 = "v2";
  
  @Override
  public String manifest(Object event) {
    return V2;
  }
  
  @Override
  public JsObject toJournal(Object event) {
    return marshaller.toJson(event);
  }
  
  @Override
  public EventSeq fromJournal(Object event, String manifest) {
    if (event instanceof JsObject) {
      JsObject json = (JsObject) event;
      if (V1.equals(manifest)) json = rename(json, "code", "seatNr");
      return EventSeq.single(json);
    } else {
      throw new IllegalArgumentException(
          "Can only work with JSON, was: " + event.getClass().getName());
    }
  }
  
  private JsObject rename(JsObject json, String from, String to) {
    // use your favorite json library to rename the field
    JsObject renamed = json;
    return renamed;
  }
}
```

##### 이벤트 클래스 제거 및 이벤트 무시(Remove Event Class and Ignore Events)

상황: 더 이상 가치를 제공하지 않는 이벤트 타입 제거. 예: `CustomerBlinked` 이벤트는 상태(state)에 기여하지 않으면서 스토리지만 소비함.

단순한 해결책 - EventAdapter에서 이벤트 폐기(drop):
- 이벤트는 빈 EventSeq를 반환하는 EventAdapter에 의해 필터링 가능 → 단, 여전히 역직렬화(deserialization)를 요구

개선된 해결책 - 툼스톤(tombstone)으로 역직렬화:
- 제거된 이벤트 타입을 인지하는 직렬화기 사용 → 역직렬화 오버헤드 방지

툼스톤(Tombstone) 정의:

Scala:
```scala
case object EventDeserializationSkipped
```

Java:
```java
static class EventDeserializationSkipped {
  public static EventDeserializationSkipped instance = new EventDeserializationSkipped();
  
  private EventDeserializationSkipped() {}
}
```

툼스톤을 지원하는 직렬화기:

Scala:
```scala
class RemovedEventsAwareSerializer extends SerializerWithStringManifest {
  val utf8 = Charset.forName("UTF-8")
  override def identifier: Int = 8337
  
  val SkipEventManifestsEvents = Set("docs.persistence.CustomerBlinked" // ...
  )
  
  override def manifest(o: AnyRef): String = o.getClass.getName
  
  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case _ => o.toString.getBytes(utf8) // example serialization
  }
  
  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    manifest match {
      case m if SkipEventManifestsEvents.contains(m) =>
        EventDeserializationSkipped
      case _ => new String(bytes, utf8)
    }
}
```

Java:
```java
static class RemovedEventsAwareSerializer extends SerializerWithStringManifest {
  private final Charset utf8 = StandardCharsets.UTF_8;
  private final String customerBlinkedManifest = "blinked";
  
  @Override
  public int identifier() {
    return 8337;
  }
  
  @Override
  public String manifest(Object o) {
    if (o instanceof CustomerBlinked) return customerBlinkedManifest;
    else return o.getClass().getName();
  }
  
  @Override
  public byte[] toBinary(Object o) {
    return o.toString().getBytes(utf8); // example serialization
  }
  
  @Override
  public Object fromBinary(byte[] bytes, String manifest) {
    if (customerBlinkedManifest.equals(manifest)) return EventDeserializationSkipped.instance;
    else return new String(bytes, utf8);
  }
}
```

툼스톤 필터링을 포함한 EventAdapter:

Scala:
```scala
class SkippedEventsAwareAdapter extends EventAdapter {
  override def manifest(event: Any) = ""
  override def toJournal(event: Any) = event
  
  override def fromJournal(event: Any, manifest: String) = event match {
    case EventDeserializationSkipped => EventSeq.empty
    case _                           => EventSeq(event)
  }
}
```

Java:
```java
static class SkippedEventsAwareAdapter implements EventAdapter {
  @Override
  public String manifest(Object event) {
    return "";
  }
  
  @Override
  public Object toJournal(Object event) {
    return event;
  }
  
  @Override
  public EventSeq fromJournal(Object event, String manifest) {
    if (event == EventDeserializationSkipped.instance) return EventSeq.empty();
    else return EventSeq.single(event);
  }
}
```

##### 도메인 모델과 데이터 모델 분리(Detach Domain Model from Data Model)

상황: 애플리케이션 도메인 모델(domain model)과 영속성 데이터 모델(data model)을 분리 → 독립적인 진화 허용. 직렬화 도구가 생성된(generated) 클래스를 요구할 때 유용함.

도메인 모델과 데이터 모델:

Scala:
```scala
object DomainModel {
  final case class Customer(name: String)
  final case class Seat(code: String) {
    def bookFor(customer: Customer): SeatBooked = SeatBooked(code, customer)
  }
  
  final case class SeatBooked(code: String, customer: Customer)
}

object DataModel {
  final case class SeatBooked(code: String, customerName: String)
}
```

Java:
```java
static class Customer {
  public final String name;
  
  public Customer(String name) {
    this.name = name;
  }
}

static class Seat {
  public final String code;
  
  public Seat(String code) {
    this.code = code;
  }
  
  public SeatBooked bookFor(Customer customer) {
    return new SeatBooked(code, customer);
  }
}

static class SeatBooked {
  public final String code;
  public final Customer customer;
  
  public SeatBooked(String code, Customer customer) {
    this.code = code;
    this.customer = customer;
  }
}

static class SeatBookedData {
  public final String code;
  public final String customerName;
  
  public SeatBookedData(String code, String customerName) {
    this.code = code;
    this.customerName = customerName;
  }
}
```

모델 변환 EventAdapter:

Scala:
```scala
class DetachedModelsAdapter extends EventAdapter {
  override def manifest(event: Any): String = ""
  
  override def toJournal(event: Any): Any = event match {
    case DomainModel.SeatBooked(code, customer) =>
      DataModel.SeatBooked(code, customer.name)
  }
  override def fromJournal(event: Any, manifest: String): EventSeq = event match {
    case DataModel.SeatBooked(code, customerName) =>
      EventSeq(DomainModel.SeatBooked(code, DomainModel.Customer(customerName)))
  }
}
```

Java:
```java
class DetachedModelsAdapter implements EventAdapter {
  @Override
  public String manifest(Object event) {
    return "";
  }
  
  @Override
  public Object toJournal(Object event) {
    if (event instanceof SeatBooked) {
      SeatBooked s = (SeatBooked) event;
      return new SeatBookedData(s.code, s.customer.name);
    } else {
      throw new IllegalArgumentException("Unsupported: " + event.getClass());
    }
  }
  
  @Override
  public EventSeq fromJournal(Object event, String manifest) {
    if (event instanceof SeatBookedData) {
      SeatBookedData d = (SeatBookedData) event;
      return EventSeq.single(new SeatBooked(d.code, new Customer(d.customerName)));
    } else {
      throw new IllegalArgumentException("Unsupported: " + event.getClass());
    }
  }
}
```

##### 사람이 읽을 수 있는 데이터 모델로 이벤트 저장(Store Events as Human-Readable Data Model)

상황: 사람이 읽을 수 있는(human-readable) JSON 형식으로 이벤트 영속화.

해결책: 도메인/데이터 모델 분리의 특수한 경우 → 저널 구현체의 지원 필요. EventAdapter가 JSON으로의 직렬화/역직렬화 처리.

JSON 데이터 모델 어댑터:

Scala:
```scala
class JsonDataModelAdapter extends EventAdapter {
  override def manifest(event: Any): String = ""
  
  val marshaller = new ExampleJsonMarshaller
  
  override def toJournal(event: Any): JsObject =
    marshaller.toJson(event)
  
  override def fromJournal(event: Any, manifest: String): EventSeq = event match {
    case json: JsObject =>
      EventSeq(marshaller.fromJson(json))
    case _ =>
      throw new IllegalArgumentException("Unable to fromJournal a non-JSON object! Was: " + event.getClass)
  }
}
```

Java:
```java
static class JsonDataModelAdapter implements EventAdapter {
  
  private final ExampleJsonMarshaller marshaller = new ExampleJsonMarshaller();
  
  @Override
  public String manifest(Object event) {
    return "";
  }
  
  @Override
  public JsObject toJournal(Object event) {
    return marshaller.toJson(event);
  }
  
  @Override
  public EventSeq fromJournal(Object event, String manifest) {
    if (event instanceof JsObject) {
      JsObject json = (JsObject) event;
      return EventSeq.single(marshaller.fromJson(json));
    } else {
      throw new IllegalArgumentException(
          "Unable to fromJournal a non-JSON object! Was: " + event.getClass());
    }
  }
}
```

노트: 이 기법은 네이티브 JSON 저장에 대한 저널 플러그인의 지원을 요구함.

##### 큰 이벤트를 세분화된 이벤트로 분할(Split Large Event into Fine-Grained Events)

상황: 굵은 입자(coarse-grained) 이벤트를 여러 개의 세분화된(fine-grained) 이벤트로 분해. 예: `UserDetailsChanged` 를 `UserNameChanged` 와 `UserAddressChanged` 로 분할.

이벤트 정의:

Scala:
```scala
trait Version1
trait Version2

final case class UserDetailsChanged(name: String, address: String) extends Version1

final case class UserNameChanged(name: String) extends Version2
final case class UserAddressChanged(address: String) extends Version2

class UserEventsAdapter extends EventAdapter {
  override def manifest(event: Any): String = ""
  
  override def fromJournal(event: Any, manifest: String): EventSeq = event match {
    case UserDetailsChanged(null, address) => EventSeq(UserAddressChanged(address))
    case UserDetailsChanged(name, null)    => EventSeq(UserNameChanged(name))
    case UserDetailsChanged(name, address) =>
      EventSeq(UserNameChanged(name), UserAddressChanged(address))
    case event: Version2 => EventSeq(event)
  }
  
  override def toJournal(event: Any): Any = event
}
```

Java:
```java
interface Version1 {};

interface Version2 {}

static class UserDetailsChanged implements Version1 {
  public final String name;
  public final String address;
  
  public UserDetailsChanged(String name, String address) {
    this.name = name;
    this.address = address;
  }
}

static class UserNameChanged implements Version2 {
  public final String name;
  
  public UserNameChanged(String name) {
    this.name = name;
  }
}

static class UserAddressChanged implements Version2 {
  public final String address;
  
  public UserAddressChanged(String address) {
    this.address = address;
  }
}

static class UserEventsAdapter implements EventAdapter {
  @Override
  public String manifest(Object event) {
    return "";
  }
  
  @Override
  public EventSeq fromJournal(Object event, String manifest) {
    if (event instanceof UserDetailsChanged) {
      UserDetailsChanged c = (UserDetailsChanged) event;
      if (c.name == null) return EventSeq.single(new UserAddressChanged(c.address));
      else if (c.address == null) return EventSeq.single(new UserNameChanged(c.name));
      else return EventSeq.create(new UserNameChanged(c.name), new UserAddressChanged(c.address));
    } else {
      return EventSeq.single(event);
    }
  }
  
  @Override
  public Object toJournal(Object event) {
    return event;
  }
}
```

어댑터는 `EventSeq` 를 통해 여러 이벤트를 반환 → 복구 도중 단일 굵은 입자 이벤트를 여러 세분화된 이벤트로 변환.

##### 핵심 요약(Key Takeaways)

- 스키마 진화를 지원하는 직렬화 형식(Jackson·Protocol Buffers·Avro·Thrift) 선택
- 직렬화기의 능력을 이해 → 어떤 진화 전략이 실현 가능한지 결정됨
- EventAdapter는 형식 및 구조 변환을 위한 유연한 메커니즘 제공
- 문자열 매니페스트(string manifest)를 갖는 직렬화기는 고급 진화 패턴을 가능하게 함
- 도메인 모델과 데이터 모델을 분리 → 독립적인 진화 가능
- 툼스톤(tombstone) 패턴 → 역직렬화 코드를 유지하지 않고도 이벤트 클래스 제거 가능

---
### 4. 영속 상태(Durable State) 영속성

#### 4.1 모듈 정보

Akka Persistence 모듈 → 영속 상태 스토어(durable state store) 플러그인 설정 요구.

의존성:

SBT:
```
"com.typesafe.akka" %% "akka-persistence-typed" % "2.10.19"
"com.typesafe.akka" %% "akka-persistence-testkit" % "2.10.19" % Test
```

- Maven/Gradle: 적절한 Scala 바이너리 버전(2.13.17 또는 3.3.7)과 함께 `akka-bom` 아티팩트를 통해 의존성 관리
- 프로젝트 세부 정보
  - Lightbend 지원을 받는 Supported 상태
  - 2.6.0 (2019-11-06)부터 사용 가능

---

#### 4.2 소개

영속 상태(Durable State) → 이벤트 소싱에 대한 대안.

- 각 명령(command) 이후 전체 엔티티 상태 저장 → 다음 모델 따름: `(State, Command) => State`
- 단순한 사용 사례에 대한 개념적 복잡성 감소 → CRUD 작업과 유사
- 데이터베이스에는 오직 최신 상태(latest state)만 영속화 → 이벤트 소싱과 달리 과거의 변경 이력 보존 안 됨

> "Akka Persistence는 그 상태를 읽어와 메모리에 저장한다. 명령의 처리가 완료된 후, 새 상태가 데이터베이스에 저장된다."

---

#### 4.3 핵심 구성 요소

##### DurableStateBehavior 구조

영속 액터(persistent actor) 필수 요소 3가지:

- persistenceId: 백엔드 스토어에서 영속 액터를 식별하는 안정적인 고유 식별자
- emptyState: 엔티티가 처음 생성될 때의 초기 상태(예: Counter는 0에서 시작)
- commandHandler: 현재 상태와 들어오는 명령을 이펙트(effect)로 매핑

##### PersistenceId

`PersistenceId` → 영속 액터를 고유하게 식별.

- 일반적으로 클러스터 샤딩(Cluster Sharding)과 결합 → 클러스터 전반에서 `PersistenceId` 당 오직 하나의 활성 엔티티만 존재하도록 보장
- 생성 방식
  - `entityTypeHint` 와 `entityId` 를 기본 구분자 `|` 로 결합하는 `PersistenceId.apply` 또는 `PersistenceId.of` 팩토리 메서드 사용
  - 커스텀 구분자나 고유 식별자는 `PersistenceId.ofUniqueId` 사용

##### 명령 핸들러(Command Handler)

- 현재 `State` 와 들어오는 `Command` 를 받아 `Effect` 지시(directive) 반환
- 이펙트 → 어떤 상태를(있다면) 영속화할지 나타냄
- 핸들러 → `thenRun` 을 사용하여 부수 효과(side effect) 연쇄(chain) 가능

---

#### 4.4 이펙트(Effect)와 부수 효과(Side Effect)

사용 가능한 이펙트:

- persist: 최신 상태 영속화
  - 리비전(revision)이 1씩 증가하면 새 레코드 삽입 또는 기존 레코드 갱신
- delete: 상태를 빈 상태(empty state)로 설정하여 삭제, 리비전 증가
- none: 상태를 영속화하지 않음(읽기 전용 명령)
- unhandled: 현재 상태에서 지원되지 않는 명령
- stop: 액터 정지
- stash: 현재 명령을 스태시(stash)
- unstashAll: 스태시된 명령들 처리
- reply: 지정된 `ActorRef` 로 응답 전송

- 명령당 오직 하나의 기본 이펙트(primary effect)만 허용
- 부수 효과 → `thenRun` 을 통해 연쇄 → 성공적인 persist 이후 최대 1회(at-most-once) 기준으로 순차 실행

추가적인 persist 이후 동작:
- `thenStop`: 액터 정지
- `thenUnstashAll`: 스태시된 명령들 처리
- `thenReply`: 응답 메시지 전송

중요한 보장

> "모든 부수 효과는 최대 1회(at-most-once) 기준으로 실행되며, persist가 실패하면 실행되지 않는다."

---

#### 4.5 완전한 예제: Counter

State:
```scala
final case class State(value: Int) extends CborSerializable
```

Commands:
```scala
sealed trait Command[ReplyMessage] extends CborSerializable
final case object Increment extends Command[Nothing]
final case class IncrementBy(value: Int) extends Command[Nothing]
final case class GetValue(replyTo: ActorRef[State]) extends Command[State]
final case object Delete extends Command[Nothing]
```

명령 핸들러 (Scala):
```scala
val commandHandler: (State, Command[_]) => Effect[State] = (state, command) =>
  command match {
    case Increment         => Effect.persist(state.copy(value = state.value + 1))
    case IncrementBy(by)   => Effect.persist(state.copy(value = state.value + by))
    case GetValue(replyTo) => Effect.reply(replyTo)(state)
    case Delete            => Effect.delete[State]()
  }
```

명령 핸들러 (Java):
```java
@Override
public CommandHandler<Command<?>, State> commandHandler() {
  return newCommandHandlerBuilder()
      .forAnyState()
      .onCommand(Increment.class, (state, command) -> 
          Effect().persist(new State(state.get() + 1)))
      .onCommand(IncrementBy.class, (state, command) -> 
          Effect().persist(new State(state.get() + command.value)))
      .onCommand(GetValue.class, (state, command) -> 
          Effect().reply(command.replyTo, state.get()))
      .onCommand(Delete.class, (state, command) -> Effect().delete())
      .build();
}
```

Behavior 생성 (Scala):
```scala
def counter(id: String): DurableStateBehavior[Command[_], State] = {
  DurableStateBehavior.apply[Command[_], State](
    persistenceId = PersistenceId.ofUniqueId(id),
    emptyState = State(0),
    commandHandler = commandHandler)
}
```

---

#### 4.6 강제된 응답(Enforced Replies)

확인 응답(confirmation response)을 요구하는 명령의 경우 → `DurableStateBehavior.withEnforcedReplies` 사용 → 컴파일 수준(compilation-level)에서 응답 요구 사항 강제.

부수 효과를 포함한 예제:
```scala
sealed trait Command[ReplyMessage] extends CborSerializable
final case class IncrementWithConfirmation(replyTo: ActorRef[Done]) 
    extends Command[Done]
final case class GetValue(replyTo: ActorRef[State]) extends Command[State]

def counter(persistenceId: PersistenceId): DurableStateBehavior[Command[_], State] = {
  DurableStateBehavior.withEnforcedReplies[Command[_], State](
    persistenceId,
    emptyState = State(0),
    commandHandler = (state, command) =>
      command match {
        case IncrementWithConfirmation(replyTo) =>
          Effect.persist(state.copy(value = state.value + 1))
              .thenReply(replyTo)(_ => Done)
        case GetValue(replyTo) =>
          Effect.reply(replyTo)(state)
      })
}
```

---

#### 4.7 상태 기반 동작 변경

`forStateType` 을 사용하는 빌더 패턴(builder pattern) → 현재 상태에 따라 서로 다른 명령 핸들러 적용.

State 클래스 (Scala):
```scala
sealed trait State
case object BlankState extends State

final case class DraftState(content: PostContent) extends State {
  def withBody(newBody: String): DraftState = 
    copy(content = content.copy(body = newBody))
  def postId: String = content.postId
}

final case class PublishedState(content: PostContent) extends State {
  def postId: String = content.postId
}
```

Commands:
```scala
sealed trait Command
final case class AddPost(content: PostContent, 
    replyTo: ActorRef[StatusReply[AddPostDone]]) extends Command
final case class AddPostDone(postId: String)
final case class GetPost(replyTo: ActorRef[PostContent]) extends Command
final case class ChangeBody(newBody: String, replyTo: ActorRef[Done]) extends Command
final case class Publish(replyTo: ActorRef[Done]) extends Command
final case class PostContent(postId: String, title: String, body: String)
```

상태 디스패치를 포함한 명령 핸들러 (Scala):
```scala
private val commandHandler: (State, Command) => Effect[State] = { (state, command) =>
  state match {
    case BlankState =>
      command match {
        case cmd: AddPost => addPost(cmd)
        case _            => Effect.unhandled
      }
    case draftState: DraftState =>
      command match {
        case cmd: ChangeBody  => changeBody(draftState, cmd)
        case Publish(replyTo) => publish(draftState, replyTo)
        case GetPost(replyTo) => getPost(draftState, replyTo)
        case AddPost(_, replyTo) =>
          Effect.unhandled[State]
              .thenRun(_ => replyTo ! 
                  StatusReply.Error("Cannot add post while in draft"))
      }
    case publishedState: PublishedState =>
      command match {
        case GetPost(replyTo) => getPost(publishedState, replyTo)
        case AddPost(_, replyTo) =>
          Effect.unhandled[State]
              .thenRun(_ => replyTo ! 
                  StatusReply.Error("Cannot add post, already published"))
        case _ => Effect.unhandled
      }
  }
}

private def addPost(cmd: AddPost): Effect[State] = {
  Effect.persist(DraftState(cmd.content)).thenRun { _ =>
    cmd.replyTo ! StatusReply.Success(AddPostDone(cmd.content.postId))
  }
}

private def changeBody(state: DraftState, cmd: ChangeBody): Effect[State] = {
  Effect.persist(state.withBody(cmd.newBody)).thenRun { _ =>
    cmd.replyTo ! Done
  }
}

private def publish(state: DraftState, replyTo: ActorRef[Done]): Effect[State] = {
  Effect.persist(PublishedState(state.content)).thenRun { _ =>
    println(s"Blog post ${state.postId} was published")
    replyTo ! Done
  }
}

private def getPost(state: DraftState, replyTo: ActorRef[PostContent]): Effect[State] = {
  replyTo ! state.content
  Effect.none
}

private def getPost(state: PublishedState, replyTo: ActorRef[PostContent]): Effect[State] = {
  replyTo ! state.content
  Effect.none
}
```

Java 구현:
```java
@Override
public CommandHandler<Command, State> commandHandler() {
  CommandHandlerBuilder<Command, State> builder = newCommandHandlerBuilder();
  builder.forStateType(BlankState.class).onCommand(AddPost.class, this::onAddPost);
  builder.forStateType(DraftState.class)
      .onCommand(ChangeBody.class, this::onChangeBody)
      .onCommand(Publish.class, this::onPublish)
      .onCommand(GetPost.class, this::onGetPost);
  builder.forStateType(PublishedState.class)
      .onCommand(ChangeBody.class, this::onChangeBody)
      .onCommand(GetPost.class, this::onGetPost);
  builder.forAnyState().onCommand(AddPost.class, (state, cmd) -> Effect().unhandled());
  return builder.build();
}

private Effect<State> onAddPost(AddPost cmd) {
  return Effect()
      .persist(new DraftState(cmd.content))
      .thenRun(() -> cmd.replyTo.tell(new AddPostDone(cmd.content.postId)));
}

private Effect<State> onChangeBody(DraftState state, ChangeBody cmd) {
  return Effect()
      .persist(state.withBody(cmd.newBody))
      .thenRun(() -> cmd.replyTo.tell(Done.getInstance()));
}

private Effect<State> onPublish(DraftState state, Publish cmd) {
  return Effect()
      .persist(new PublishedState(state.content))
      .thenRun(() -> {
        System.out.println("Blog post published: " + state.postId());
        cmd.replyTo.tell(Done.getInstance());
      });
}

private Effect<State> onGetPost(DraftState state, GetPost cmd) {
  cmd.replyTo.tell(state.content);
  return Effect().none();
}

private Effect<State> onGetPost(PublishedState state, GetPost cmd) {
  cmd.replyTo.tell(state.content);
  return Effect().none();
}
```

---

#### 4.8 응답(Replies)

요청-응답(request-response) 상호작용 패턴 → 영속 액터의 표준.

- 명령에 `ActorRef[ReplyMessageType]` 포함
- 성공하거나 검증(validation)에 실패할 수 있는 응답에는 `StatusReply[ReplyType]` 사용
- 미리 정의된 `StatusReply.Ack` → 값을 반환하지 않고 확인(acknowledge)

검증 이후 또는 persist 이후에 `thenRun` 부수 효과를 사용하여 응답 전송:

```scala
Effect.persist(DraftState(cmd.content)).thenRun { _ =>
  cmd.replyTo ! StatusReply.Success(AddPostDone(cmd.content.postId))
}
```

`withEnforcedReplies` 사용 시 → 모든 명령 핸들러가 `Effect.reply`, `Effect.noReply`, `Effect.thenReply`, 또는 `Effect.thenNoReply` 를 통해 생성된 `ReplyEffect` 를 반환하도록 컴파일 단계에서 요구됨.

---

#### 4.9 직렬화(Serialization)

- Durable State → 액터 메시지와 동일한 직렬화 메커니즘 사용
- 명령과 상태에 대해 직렬화 활성화 필요
- Jackson 기반 직렬화가 범용적인 해결책으로 권장

---

#### 4.10 태깅(Tagging)

태그(tag) 사용 → `DurableStateStoreQuery` 인터페이스를 통해 영속 상태 스토어 내의 상태 부분집합(subset)을 별도의 스트림 소비를 위해 식별 가능.

Scala:
```scala
DurableStateBehavior[Command[_], State](
   persistenceId = PersistenceId.ofUniqueId("abc"),
   emptyState = State(0),
   commandHandler = (state, cmd) => throw new NotImplementedError("TODO"))
   .withTag("tag1")
```

Java:
```java
public class MyPersistentBehavior 
    extends DurableStateBehavior<Command, State> {
  @Override
  public String tag() {
    return "tag1";
  }
}
```

---

#### 4.11 ActorContext 접근

자식 액터(child actor)를 스폰(spawn)하거나 로깅(logging)을 위해 `ActorContext` 에 접근 → `Behaviors.setup` 으로 생성을 감쌈.

Scala:
```scala
def apply(): Behavior[String] =
  Behaviors.setup { context =>
    DurableStateBehavior[String, State](
      persistenceId = PersistenceId.ofUniqueId("myPersistenceId"),
      emptyState = State(0),
      commandHandler = CommandHandler.command { cmd =>
        context.log.info("Got command {}", cmd)
        Effect.none
      })
  }
```

Java:
```java
public class MyPersistentBehavior 
    extends DurableStateBehavior<Command, State> {
  public static Behavior<Command> create(PersistenceId persistenceId) {
    return Behaviors.setup(ctx -> new MyPersistentBehavior(persistenceId, ctx));
  }
  
  private final ActorContext<Command> context;
  private final ActorRef<Command> self;
  
  public MyPersistentBehavior(PersistenceId persistenceId, 
      ActorContext<Command> ctx) {
    super(persistenceId);
    this.context = ctx;
    this.self = ctx.getSelf();
  }
}
```

DurableStateBehavior 감싸기(Wrapping)

`ActorContext` 기능에 접근하기 위해 `DurableStateBehavior` 를 `Behaviors.setup` 같은 다른 동작(behavior)으로 감쌀 수 있음.

```scala
Behaviors.setup[Command[_]] { context =>
  DurableStateBehavior[Command[_], State](
    persistenceId = PersistenceId.ofUniqueId("abc"),
    emptyState = State(0),
    commandHandler = CommandHandler.command { cmd =>
      context.log.info("Got command {}", cmd)
      Effect.none
    })
}
```

---

#### 4.12 클러스터 샤딩(Cluster Sharding)

> "클러스터 샤딩(Cluster Sharding)은 영속 액터를 클러스터 전반에 분산시키고 id로 주소를 지정(address)하는 데 매우 적합하다."

- `persistenceId` 당 오직 하나의 활성 엔티티 인스턴스만 존재하도록 보장
- 사용 가능한 노드에서 액터를 신속하게 재시작 → 노드 장애 전반에 걸친 복원력 개선

---

#### 4.13 동작 변경 및 가변 상태 고려 사항

##### 동작 변경(Behavior Changes)

- 영속 액터는 명령 핸들러에서 새로운 동작(behavior)을 반환할 수 없음
  - 이유: 동작은 복구 과정에서 신중하게 재구성되어야 하는 액터 상태를 이룸 → 함부로 변경 불가
- 대신 현재 상태에 따라 서로 다른 함수가 명령을 처리하는 상태 기반 명령 처리(state-based command handling) 구현 필요
- 이 방식 → 유한 상태 기계(finite state machine) 구현에 유용

##### 가변 상태 고려 사항(Mutable State Considerations)

가변 상태(mutable state) 사용 시 → 실패 후 재시작 시 상태 인스턴스가 새로 생성되도록 `emptyStateFactory: () => State` 매개변수를 받는 `DurableStateBehavior.withMutableState` 또는 관련 팩토리 사용.

---

### 5. 영속 상태 스타일 가이드(Durable State Style)

이 가이드 → 타입드 Akka 애플리케이션에서 명령 처리와 상태 관리를 구조화하는 방법, Durable State 엔티티를 위한 스타일 접근법 다룸.

#### 상태 안의 명령 핸들러(Command Handlers in the State)

이 가이드 → 명령 처리 로직을 상태 객체(state object) 자체에 위임하는 아키텍처 패턴 제시.

> "명령 핸들러는 `Account`(상태) 안의 `applyCommand` 에 위임하며, 이는 구체적인 `EmptyAccount`, `OpenedAccount`, `ClosedAccount` 에 구현되어 있다."

##### Scala 구현

Scala 예제 → 세 가지 상태 구현을 갖는 봉인된 트레이트 계층(sealed trait hierarchy) 제시.

- EmptyAccount: 초기 생성을 처리 → 다른 작업 이전에 `CreateAccount` 요구
- OpenedAccount: 입금(deposit)·출금(withdrawal)·잔액 조회·계좌 폐쇄 관리
- ClosedAccount: 적절한 오류 응답과 함께 모든 작업 거부

각 상태 타입 → `applyCommand(cmd: Command): ReplyEffect` 구현 → 현재 상태에 기반한 다형적(polymorphic) 명령 처리 가능.

##### Java 구현

Java 접근법 → `null` 이 빈 초기 상태를 나타내는 널 가능 상태(nullable state) 패턴 사용.

- `CommandHandlerWithReplyBuilder` → `forNullState()` 와 `forStateType()` 메서드를 통해 상태별 명령 처리 허용
- 컴파일 타임 타입 안정성(compile-time type safety) 제공

#### 선택적 초기 상태(Optional Initial State)

전용 빈 상태 클래스를 유지하는 대신 → 개발자는 `null` 또는 `Option`/`Optional` 타입 사용 가능.

가이드 언급:
> "빈 초기 상태를 위해 별도의 상태 클래스를 사용하는 것이 바람직하지 않고, 마치 아직 상태가 없는 것처럼 행동하는 것이 바람직하다"

##### Option을 사용하는 Scala

`None` 을 초기 상태로 하는 `Option[Account]` 사용 시 → `state.applyCommand(cmd)` 를 통해 상태별 메서드에 위임하기 전에 외부 핸들러 계층에서 패턴 매칭(pattern matching) 필요.

##### Null을 사용하는 Java

Java 구현 → `null` 을 반환하는 `emptyState()` 를 오버라이드한 다음, 명령 핸들러 빌더에서 `forNullState()` 를 사용하여 생성을 다른 작업과 분리하여 처리 가능.

#### Java 21 기능 활용

Java 21+ 프로젝트 → 봉인된 인터페이스(sealed interface) 및 레코드(record)와 결합된 `DurableStateOnCommandBehavior` 기반 클래스 활용 가능.

- 컴파일 타임 완전성 검사(exhaustiveness checking)와 함께 switch 패턴 매칭 가능

##### Java 21 예제

`BlogPostEntityDurableState` 예제가 보여주는 것:

- 봉인된 Command 인터페이스: 모든 명령 타입이 알려져 있음을 보장
- 레코드 클래스: 데이터를 담는 명령 및 상태 정의를 단순화
- 패턴 매칭: 중첩된 switch 가 명확한 상태-명령 라우팅 제공

이 패턴 → 컴파일러 검증을 통해 핸들러 메서드가 모든 명령과 이벤트 유형을 빠짐없이 처리했음을 보장.

#### 핵심 아키텍처 노트

모든 접근법 → 상태 영속화 및 응답 의미론(reply semantics)을 기술하기 위해 `Effect` 사용:

- `Effect.persist()`: 영속적인 갱신에 사용
- `.thenReply()`: 응답을 보내기 위해
- `.thenNoReply()`: 발사 후 망각(fire-and-forget) 작업을 위해
- `Effect.unhandled()`: 유효하지 않은 명령-상태 조합을 위해

함수형(functional) 위임 패턴과 객체지향(object-oriented) 위임 패턴 중 무엇을 선택할지 → 애플리케이션 복잡성과 팀 선호도에 따라 달라짐 → 각각 코드 구조화와 테스트 용이성(testability) 측면에서 고유한 장점 지님.

---

### 6. 영속 상태 영속성 쿼리(Durable State Persistence Query)

#### 6.1 개요

이 섹션 → 영속 상태 동작(Durable State Behaviors)에 대한 쿼리 인터페이스를 비동기 스트림(asynchronous stream)으로 제공하는 Durable State용 Akka Persistence Query 다룸.

- CQRS 아키텍처 패턴에서 읽기 측(read side)을 구현하는 데 활용

---

#### 6.2 의존성 설정

sbt:
```
val AkkaVersion = "2.10.19"
libraryDependencies += "com.typesafe.akka" %% "akka-persistence-query" % AkkaVersion
```

- Maven: BOM 임포트로 `akka-bom_${scala.binary.version}` 버전 2.10.19 와 `akka-persistence-query_${scala.binary.version}` 의존성 요구

Gradle:
```
implementation platform("com.typesafe.akka:akka-bom_${versions.ScalaBinary}:2.10.19")
implementation "com.typesafe.akka:akka-persistence-query_${versions.ScalaBinary}"
```

노트: 의존성은 https://account.akka.io/token 에 명시된 보안 토큰화 URL을 통해 접근해야 함.

---

#### 6.3 소개

이 쿼리 모듈 → 비동기 스트림을 통해 영속 상태 동작(Durable State Behaviors)에 대한 인터페이스 제공.

- CQRS 패턴에서 읽기 측 구현 지원
- Akka Persistence를 통한 쓰기와 쿼리 분리

대안: R2DBC 플러그인 사용자는 쿼리 표현(query representation)을 읽기 측 대신 쓰기 측에서 직접 저장할 수도 있음.

---

#### 6.4 Akka Projections와 함께 쿼리 사용하기

`DurableStateStoreQuery` 인터페이스 → Akka Projections에서 태그 기반(tag-based) 검색 가능.

- 동작하려면 태깅된 객체(tagged object) 필요

##### Scala 예제
```scala
import akka.persistence.state.DurableStateStoreRegistry
import akka.persistence.query.scaladsl.DurableStateStoreQuery
import akka.persistence.query.DurableStateChange
import akka.persistence.query.UpdatedDurableState

val durableStateStoreQuery = 
  DurableStateStoreRegistry(system).durableStateStoreFor[DurableStateStoreQuery[Record]](pluginId)
val source: Source[DurableStateChange[Record], NotUsed] = 
  durableStateStoreQuery.changes("tag", offset)
source.map {
  case UpdatedDurableState(persistenceId, revision, value, offset, timestamp) => Some(value)
  case _: DeletedDurableState[_] => None
}
```

##### Java 예제
```java
import akka.persistence.state.DurableStateStoreRegistry;
import akka.persistence.query.javadsl.DurableStateStoreQuery;
import akka.persistence.query.DurableStateChange;
import akka.persistence.query.UpdatedDurableState;

DurableStateStoreQuery<Record> durableStateStoreQuery =
    DurableStateStoreRegistry.get(system)
        .getDurableStateStoreFor(DurableStateStoreQuery.class, pluginId);
Source<DurableStateChange<Record>, NotUsed> source =
    durableStateStoreQuery.changes("tag", offset);
source.map(chg -> {
  if (chg instanceof UpdatedDurableState) {
    UpdatedDurableState<Record> upd = (UpdatedDurableState<Record>) chg;
    return upd.value();
  } else {
    throw new IllegalArgumentException("Unexpected DurableStateChange " + chg.getClass());
  }
});
```

---

#### 6.5 DurableStateChange 타입

쿼리로부터 반환되는 요소 → `UpdatedDurableState` 또는 `DeletedDurableState` 인스턴스 중 하나.

- UpdatedDurableState
  - `persistenceId`·`revision`·`value`·`offset`·`timestamp` 필드 포함
- DeletedDurableState
  - 상태가 삭제되었음을 나타냄

- `changes("tag", offset)` 쿼리 → 라이브 스트림(live stream) → 변경이 발생할 때마다 새 요소 방출
- `currentChanges` 변형 → 현재 존재하는 변경만 반환하고 스트림을 완료
- 처리된 마지막 변경의 `offset` 을 저장하고 다음 시작 시 재사용 → 재개 가능한 프로젝션(resumable projection) 구현 가능

---

### 7. 참고 자료

- [Akka 공식 문서](https://doc.akka.io/libraries/akka-core/current/)
- [Persistence Query](https://doc.akka.io/libraries/akka-core/current/persistence-query.html)
- [Persistence Plugins](https://doc.akka.io/libraries/akka-core/current/persistence-plugins.html)
- [Persistence - Schema Evolution](https://doc.akka.io/libraries/akka-core/current/persistence-schema-evolution.html)
- [Durable State](https://doc.akka.io/libraries/akka-core/current/typed/durable-state/persistence.html)
- [Durable State - Style](https://doc.akka.io/libraries/akka-core/current/typed/durable-state/persistence-style.html)
- [Durable State - Persistence Query](https://doc.akka.io/libraries/akka-core/current/durable-state/persistence-query.html)
- [Akka Projections](https://doc.akka.io/libraries/akka-projection/current/)
- [Community Plugins](https://akka.io/community/#plugins-to-akka-persistence-query)

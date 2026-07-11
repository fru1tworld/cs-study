# Jepsen: 일관성 모델

> 원문: [Consistency](https://jepsen.io/consistency), [Consistency Models](https://jepsen.io/consistency/models)
> 저자: Jepsen, LLC (Kyle Kingsbury) | 번역일: 2026-07-10

Jepsen은 분산 시스템의 안전성 속성을 분석한다. 가장 대표적으로는 일관성 모델 위반을 찾아낸다. 그런데 일관성 모델이란 무엇인가? 각 모델은 어떤 현상을 허용하는가? 주어진 프로그램에 정말로 필요한 일관성은 어떤 종류인가?

이 레퍼런스 가이드는 다양한 일관성 모델의 기본 정의, 직관적인 설명, 이론적 토대를 문헌 링크와 함께 제공한다. 업계 실무자, 학계 연구자, 애호가 모두가 일관성 속성에 쉽게 접근할 수 있게 하는 것이 목표다.

## [모델(Models)](https://jepsen.io/consistency/models)

[일관성 모델](https://jepsen.io/consistency/models)은 시스템이 무엇을 할 수 있는지 선언하는 안전성 속성이다. 형식적으로 말하면, 일관성 모델은 시스템이 합법적으로 실행할 수 있는 히스토리들의 집합을 정의한다. 예를 들어 [직렬화 가능성(Serializability)](https://jepsen.io/consistency/models/serializable)이라는 모델은 모든 합법적 히스토리가 전순서(totally ordered) 실행과 동등해야 함을 보장한다.

## [현상(Phenomena)](https://jepsen.io/consistency/phenomena)

일관성 모델은 흔히 금지되는 [현상](https://jepsen.io/consistency/phenomena), 즉 특정한 연산 패턴으로 정의된다. 예를 들어 [G1a(중단된 읽기, Aborted Read)](https://jepsen.io/consistency/phenomena/g1a)는 어떤 트랜잭션이 중단된(aborted) 다른 트랜잭션이 수행한 쓰기를 관찰할 때 발생한다.

## [의존성(Dependencies)](https://jepsen.io/consistency/dependencies)

일관성 모델과 현상은 흔히 서로 다른 연산 사이의 [의존성](https://jepsen.io/consistency/dependencies)으로 정의된다. 예를 들어 트랜잭션 T2가 T1이 쓴 값을 읽으면 T2가 T1에 쓰기-읽기(write-read) 의존한다고 말한다. 이 의존성들은 그래프를 이룬다. 그 그래프에서 특정 사이클을 금지하면 서로 다른 일관성 모델이 생겨난다.

---

# 일관성 모델 (Consistency Models)

Jepsen은 분산 시스템의 안전성 속성을 분석한다. 가장 대표적으로는 일관성 모델 위반을 찾아낸다. 그런데 일관성 모델이란 무엇인가? 각 모델은 어떤 현상을 허용하는가? 주어진 프로그램에 정말로 필요한 일관성은 어떤 종류인가?

이 레퍼런스 가이드는 엔지니어와 학계 모두를 위해 다양한 일관성 모델의 기본 정의, 직관적인 설명, 이론적 토대를 제공한다.

![일관성 모델 지도](https://jepsen.io/consistency/models/map.svg)

이 클릭 가능한 지도([Bailis, Davidson, Fekete 외](http://www.vldb.org/pvldb/vol7/p181-bailis.pdf)와 [Viotti와 Vukolic](https://arxiv.org/pdf/1512.00168.pdf)에서 변형)는 동시성 시스템에서 흔히 쓰이는 일관성 모델들 사이의 관계를 보여준다. 화살표는 일관성 모델 간의 관계를 나타낸다. 예를 들어 strict serializable은 serializability와 linearizability를 모두 함의하고, linearizability는 순차 일관성(sequential consistency)을 함의하는 식이다. 색깔은 비동기 네트워크 위의 분산 시스템에서 각 모델이 얼마나 가용(available)할 수 있는지를 보여준다.

이 지도는 일관성 모델들을 계층 구조로 배열한다. 지도에 표시된 가장 강한 모델은 [strict serializable](https://jepsen.io/consistency/models/strict-serializable)이다. Strict serializable은 서로 겹치지 않는 두 일관성 모델 계열, 즉 다중 객체 트랜잭션에 대한 모델들과 단일 객체 연산에 대한 모델들을 하나로 통합한다. 모델 x가 y를 함의한다(implies)고 말할 때는, x가 성립하는 모든 히스토리에서 y도 성립한다는 뜻이다. 이때 x는 y보다 "강하다".

다중 객체 쪽에서는 strict serializable이 [serializable](https://jepsen.io/consistency/models/serializable)을 함의하고, serializable은 다시 [repeatable read](https://jepsen.io/consistency/models/repeatable-read)와 [snapshot isolation](https://jepsen.io/consistency/models/snapshot-isolation)을 함의한다. (일반화된 정의를 가정하면) snapshot isolation과 repeatable read는 둘 다 [monotonic atomic view](https://jepsen.io/consistency/models/monotonic-atomic-view)를 함의한다. Repeatable read는 [cursor stability](https://jepsen.io/consistency/models/cursor-stability)를 함의한다. Cursor stability와 monotonic atomic view는 둘 다 [read committed](https://jepsen.io/consistency/models/read-committed)를 함의하고, read committed는 [read uncommitted](https://jepsen.io/consistency/models/read-uncommitted)를 함의한다.

단일 객체 모델 쪽에서는 strict serializable이 [linearizable](https://jepsen.io/consistency/models/linearizable)을 함의하고, linearizable은 [sequential](https://jepsen.io/consistency/models/sequential)을, sequential은 [causal](https://jepsen.io/consistency/models/causal)을 함의한다. Causal은 [writes follow reads](https://jepsen.io/consistency/models/writes-follow-reads)와 [PRAM](https://jepsen.io/consistency/models/pram)을 함의한다. PRAM은 [monotonic reads](https://jepsen.io/consistency/models/monotonic-reads), [monotonic writes](https://jepsen.io/consistency/models/monotonic-writes), [read your writes](https://jepsen.io/consistency/models/read-your-writes)를 함의한다.

Cursor stability, snapshot isolation, sequential과 같거나 그보다 강한 모든 모델은 비동기 네트워크에서 완전 가용(totally available)할 수 없다. Read your writes와 같거나 그보다 강한 모든 모델은 기껏해야 스티키 가용(sticky available)할 수 있다. (여기 나열된 것 중) 더 약한 모델들은 완전 가용할 수 있다.

## 기본 개념 (Fundamental Concepts)

### 시스템 (Systems)

분산 시스템은 동시성 시스템의 한 종류이며, 동시성 제어에 관한 문헌 상당수가 분산 시스템에 그대로 적용된다. 실제로 우리가 다룰 개념 대부분은 원래 단일 노드 동시성 시스템을 위해 정식화된 것이다. 다만 가용성과 성능 면에서는 몇 가지 중요한 차이가 있다.

시스템은 시간에 따라 변하는 논리적 상태를 가진다. 예를 들어 단순한 시스템은 정수 변수 하나일 수 있고, 그 상태는 `0`, `3`, `42` 같은 것이다. 뮤텍스는 잠김과 풀림, 두 가지 상태만 가진다. 키-값 저장소의 상태는 키에서 값으로 가는 맵일 수 있다. 예: `{cat: 1, dog: 1}` 또는 `{cat: 4}`.

### 프로세스 (Processes)

프로세스[^1]는 계산을 수행하고 연산을 실행하는, 논리적으로 싱글 스레드인 프로그램이다. 프로세스는 절대 비동기적이지 않다. 비동기 계산은 독립적인 프로세스들로 모델링한다. "논리적으로 싱글 스레드"라고 말하는 이유는, 프로세스가 한 번에 한 가지 일만 할 수 있긴 하지만 그 구현은 여러 스레드, 운영체제 프로세스, 심지어 물리 노드에 걸쳐 있을 수 있음을 강조하기 위해서다. 그 구성 요소들이 일관된 싱글 스레드 프로그램이라는 환상을 제공하기만 하면 된다.

### 연산 (Operations)

연산은 상태에서 상태로의 전이다. 예를 들어 단일 변수 시스템에는 그 변수의 값을 읽고 쓰는 `read`와 `write` 같은 연산이 있을 수 있다. 카운터에는 증가, 감소, 읽기 같은 연산이 있을 수 있다. SQL 저장소에서 연산은 여러 개의 select, update, delete 등으로 구성된 트랜잭션일 수 있다.

#### 함수, 인자, 반환값 (Functions, Arguments & Return Values)

이론상으로는 모든 상태 전이에 고유한 이름을 붙일 수도 있다. 락은 정확히 두 가지 전이, `lock`과 `unlock`을 가진다. 정수 레지스터는 무한히 많은 읽기와 쓰기를 가진다: `read-the-value-1`, `read-the-value-2`, …, 그리고 `write-1`, `write-2`, ….

이를 다루기 쉽게 만들기 위해, 전이들을 `read`, `write`, `cas`, `increment` 같은 함수와 그 함수를 매개변수화하는 값으로 쪼갠다. 단일 레지스터 시스템에서 1을 쓰는 연산은 이렇게 쓸 수 있다.

```clojure
{:f :write, :value 1}
```

키-값 저장소가 주어졌을 때, 키 "a"의 값을 3만큼 증가시키는 것은 이렇게 표현할 수 있다.

```clojure
{:f :increment, :value ["a" 3]}
```

트랜잭션 저장소에서는 값이 복잡한 트랜잭션일 수 있다. 여기서는 하나의 상태 전이 안에서 `a`의 현재 값을 읽어 2를 얻고, `b`를 `3`으로 설정한다.

```clojure
{:f :txn, :value [[:read "a" 2] [:write "b" 3]]}
```

#### 호출 시각과 완료 시각 (Invocation & Completion Times)

연산은 일반적으로 시간이 걸린다. 멀티스레드 프로그램에서 연산은 함수 호출일 수 있다. 분산 시스템에서 연산은 서버에 요청을 보내고 응답을 받는 것을 의미할 수 있다.

이를 모델링하기 위해, 각 연산에는 호출 시각(invocation time)이 있고, 완료된다면 그보다 엄격히 큰 완료 시각(completion time)이 있다고 본다. 둘 다 상상 속의[^2], 완벽히 동기화된, 전역적으로 접근 가능한 시계가 부여한다.[^3] 이런 시계는 실시간(real-time) 순서를 제공한다고 말하는데, 인과적 순서만 추적하는 시계와 구분하기 위해서다.[^4]

#### 동시성 (Concurrency)

연산은 시간이 걸리므로, 두 연산이 시간상 겹칠 수 있다. 예를 들어 두 연산 A와 B가 있을 때, A가 시작하고, B가 시작하고, A가 완료된 다음, B가 완료될 수 있다. A와 B가 둘 다 실행 중인 시간이 존재하면 두 연산 A와 B가 동시적(concurrent)이라고 말한다.

프로세스는 싱글 스레드이므로, 같은 프로세스가 실행한 두 연산은 절대 동시적일 수 없다.

#### 크래시 (Crashes)

어떤 연산이 어떤 이유로든 완료되지 않으면(타임아웃이 났거나 핵심 구성 요소가 크래시했기 때문일 수 있다), 그 연산에는 완료 시각이 없으며, 일반적으로 그 호출 시각 이후의 모든 연산과 동시적이라고 간주해야 한다. 그 연산은 실행될 수도, 되지 않을 수도 있다.

이런 상태의 연산을 가진 프로세스는 사실상 멈춘 것이며, 다시는 다른 연산을 호출할 수 없다. 만약 다른 연산을 호출한다면 싱글 스레드 제약을 위반하게 된다. 프로세스는 한 번에 한 가지 일만 한다.

### 히스토리 (Histories)

히스토리는 연산들의 모음이며, 그 동시적 구조까지 포함한다.

일부 논문은 이를 연산들의 집합으로 표현한다. 각 연산은 호출 시각과 완료 시각을 나타내는 두 숫자를 포함하고, 동시적 구조는 프로세스 간 시간 구간을 비교해서 추론한다.

Jepsen은 [히스토리를](https://github.com/jepsen-io/history) 호출 연산과 완료 연산의 순서 있는 리스트로 표현하며, 사실상 각 연산을 둘로 쪼갠다. 이 표현은 동시 연산과 가능한 상태의 표현을 유지하면서 히스토리를 순회하는 알고리즘에 더 편리하다.

### 일관성 모델 (Consistency Models)

일관성 모델은 히스토리들의 집합이다. 우리는 일관성 모델을 사용해 시스템에서 어떤 히스토리가 "좋은" 또는 "합법적인" 것인지 정의한다. 어떤 히스토리가 "serializability를 위반한다"거나 "serializable하지 않다"고 말할 때는, 그 히스토리가 serializable한 히스토리들의 집합에 속하지 않는다는 뜻이다.

일관성 모델 A가 B의 부분집합이면 A가 모델 B를 함의한다고 말한다. 예를 들어 linearizability는 순차 일관성을 함의하는데, linearizable한 모든 히스토리는 순차적으로도 일관되기 때문이다. 이 덕분에 일관성 모델들을 계층 구조로 연관 지을 수 있다.

격식 없이 말할 때, 더 작고 더 제약이 강한 일관성 모델을 "더 강하다"고, 더 크고 더 관대한 일관성 모델을 "더 약하다"고 부른다.

모든 일관성 모델이 직접 비교 가능한 것은 아니다. 두 모델이 서로 다른 동작을 허용하되 어느 쪽도 다른 쪽을 포함하지 않는 경우가 흔하다. 예를 들어 [Snapshot Isolation](https://jepsen.io/consistency/models/snapshot-isolation)은 [A5B(쓰기 스큐, Write Skew)](https://jepsen.io/consistency/phenomena/a5b)라는 현상을 허용하는데, 이는 [Repeatable Read](https://jepsen.io/consistency/models/repeatable-read)에서는 금지된다. 반면 Repeatable Read는 프레디킷(predicate)에 대한 [G-single](https://jepsen.io/consistency/phenomena/g-single)을 허용하는데, 이는 Snapshot Isolation에서는 금지된다. 어느 모델도 다른 쪽보다 엄격히 강하지 않다. 이럴 때 Snapshot Isolation과 Repeatable Read는 비교 불가능(incomparable)하다고 말한다.

[^1]: 문헌에 따라 프로세스를 노드, 호스트, 액터, 에이전트, 사이트, 스레드라고 부르기도 하며, 맥락에 따라 미묘한 차이가 있는 경우가 많다.

[^2]: 이 마법 같은 동기화 시계가 실제로 존재할 필요는 없다. 다만 일부 일관성 모델은 그런 시계가 존재한다면 어떤 연산이 다른 연산보다 먼저 일어난다는 것을 함의한다.

[^3]: "상대성 이론 때문에 동시성(simultaneity)이라는 것은 존재하지 않는다"는 말을 동기화된 시계가 존재할 수 없다는 논거로 들어봤을지 모른다. 이는 오해다. 특수 상대성과 일반 상대성의 방정식은 시간 변환에 대한 정확한 식을 제공하며, 합리적이고 전역적으로 동기화된 시계를 얼마든지 정의할 수 있다. 이런 시계를 참조하는 일관성 제약은 시계의 선택에 의존한다. 예컨대 기준틀(reference frame)에 따라 어떤 시스템은 linearizability를 제공할 수도, 제공하지 않을 수도 있다. 다행인 점은 a) 사실상 모든 실용적 목적에서, 지구상의 시계들은 서로의 속도와 가속도가 워낙 가까워서 오차가 사이드 채널 지연보다 훨씬 작다는 것, b) 비동기 네트워크에서 실시간 경계를 보장하는 알고리즘 다수가 인과적 메시지를 사용해 실시간 순서를 강제하며, 그 결과 순서는 모든 기준틀에서 불변이라는 것이다.

[^4]: "실시간(real-time)"은 일정 시간 안에 실행을 보장하는 프로그램을 가리키기도 하지만, 여기서는 동기화되지 않았을 수 있는 로컬 시계나 어떤 논리적 시간 관계가 아니라, 현실 세계의 시간을 가리키는 뜻으로 쓴다.

# 불가능한 상태를 표현 불가능하게 만들어라 (Make Illegal States Unrepresentable)

- 원문: [Make Illegal States Unrepresentable](https://functional-architecture.org/make_illegal_states_unrepresentable/)
- 출처: Functional Software Architecture (Active Group GmbH)
- 분류: 함수형 소프트웨어 아키텍처 원칙(Principle)

> 이 문서는 Functional Software Architecture의 "Make Illegal States Unrepresentable" 원칙 페이지 전문을 한국어로 번역한 것입니다.

---

함수형 소프트웨어 아키텍처에서 데이터 모델은 프로그래머의 의도(intent)를 표현합니다. 데이터 모델이 의미 있는 모든 값은 표현할 수 있게 하고 의미 없는 값은 하나도 표현할 수 없게 한다면, 의도가 충분히 포착된 것입니다. "불가능한 상태를 표현 불가능하게 만들어라(Make illegal states unrepresentable)"라는 격언은 후자, 즉 의미 없는 값을 겨냥합니다. 이 격언은 Yaron Minsky가 [2010년 하버드 초청 강연](https://blog.janestreet.com/effective-ml-revisited/)에서 만들어 낸 것입니다. 그 아이디어는, 말이 안 되는("불가능한", illegal) 값("상태", state)을 표현할 수 없도록("표현 불가능", unrepresentable) 데이터를 모델링하자는 것입니다. "불가능한 상태를 표현 불가능하게 만들어라"를 준수하면 결과로 나오는 모델은 더 단순한 경우가 많고, 원하는 의도를 더 직접적으로 표현하게 됩니다. 이는 (흔히 구성에 의해 올바른, correct by construction) 더 견고하고 더 느슨하게 결합된 시스템으로 이어집니다.

목표: [단순성(Simplicity)](https://functional-architecture.org/#values), [유지보수성(Maintainability)](https://functional-architecture.org/#values), [정확성(Correctness)](https://functional-architecture.org/#values)

관련 패턴: [정적 타입(Static types)](https://functional-architecture.org/static_types), [검증하지 말고 파싱하라(Parse, don't validate)](https://functional-architecture.org/parse_dont_validate), [스마트 생성자(Smart Constructor)](https://functional-architecture.org/smart_constructor)

## 기법(Techniques)

"불가능한 상태를 표현 불가능하게 만들어라"는 대수적 데이터 타입(algebraic data types)을 명시적으로 지원하는 정적 타입 언어인 OCaml의 맥락에서 Ron Minsky가 처음 소개했습니다. 원래 예제는 nullable 필드가 많은 평평한(flat) 곱 타입(product type)을 곱들의 합(sum-of-products)으로 바꾸어 이 격언을 설명합니다. 격언 자체는 정적 타입 언어에 국한되지 않으며, 이 격언을 구현하는 다른 기법들도 있습니다.

### 합 타입(sum types) 활용하기

Minsky의 원래 예제는 다음 OCaml 코드에서 출발하는데, 이 코드는 몇 가지 미묘한 데이터 모델링 문제를 보여 줍니다.

OCaml

    type connection_state =
    | Connecting
    | Connected
    | Disconnected

    type connection_info = {
      state: connection_state;
      server: Inet_addr.t;
      last_ping_time: Time.t option;
      last_ping_id: int option;
      session_id: string option;
      when_initiated: Time.t option;
      when_disconnected: Time.t option;
    }

이 표현 방식에서는 일부 불가능한 상태를 표현할 수 있습니다. 예를 들어, `when_disconnected` 값을 가진 `Connecting` 상태를 만들어 낼 수 있습니다.

OCaml

    let illegal_1 = {
      state = Connecting;
      when_disconnected = Some some_time;
      ...
    }

또는 `last_ping_time`은 있지만 `last_ping_id`는 없는 `Connected` 상태를 만들어 낼 수도 있습니다.

OCaml

    let illegal_2 = {
      state = Connected;
      last_ping_time = Some some_time;
      last_ping_id = None;
      ...
    }

이 표현 방식에는 모델이 포착하지 못하는 불변식(invariant)들이 있으므로, 모델 사용자가 이를 지켜야만 하고, 이는 암묵적 결합(implicit coupling)을 초래합니다. 불변식은 프로그래밍 언어의 기능 밖에서, 예를 들면 주석으로 기술되어야 합니다. 이러한 기술(description)은 코드와 동기화가 어긋날 수 있습니다. 불변식이 기계에 의해 검사되지 않으므로 흔한 버그의 원천이 됩니다.

관련 불변식을 본질적으로 표현하는 개선된 모델은 다음과 같습니다.

OCaml

    type connecting = { when_initiated: Time.t; }
    type connected = { last_ping : (Time.t * int) option;
              session_id: string; }
    type disconnected = { when_disconnected: Time.t; }

    type connection_state =
    | Connecting of connecting
    | Connected of connected
    | Disconnected of disconnected

    type connection_info = {
      state : connection_state;
      server: Inet_addr.t;
    }

이제 `connection_state`는 제대로 된 곱들의 합(sum-of-products)입니다. 세 상태 중 하나에만 의미가 있는 필드들은 이제 해당하는 타입의 일부로만 존재합니다. `server` 필드는 모든 상태에 의미가 있으므로 더 큰 `connection_info` 타입의 일부입니다. 추가로, `last_ping_time`과 `last_ping_id`가 둘 다 `Some ...`이거나 둘 다 `None`이어야 한다는 요구사항은 이제 `last_ping`이 튜플의 단일 옵션(option)이라는 점으로 표현됩니다.

#### 동적 타입 언어에서 합 타입 활용하기

Clojure 같은 동적 타입 언어는 대수적 데이터 타입을 언어 기능으로 명시적으로 언급하지 않습니다. 그럼에도 평평한 곱을 곱들의 합으로 바꾸는 위 기법은 동적 타입 언어에서도 사용할 수 있습니다. 이 점을 설명하기 위해 Clojure(+ [active-clojure](https://github.com/active-group/active-clojure) 레코드)를 사용합니다. 결함 있는 표현은 위 OCaml 코드에서처럼 평평한 레코드를 사용합니다.

Clojure

    (define-record-type ConnectionInfo
      make-connection-info
      connection-info?
      [state connection-info-state ;; :connecting, :connected, :disconnected 중 하나
       server connection-info-server ;; non-nullable
       last-ping-time connection-info-last-ping-time ;; nullable
       last-ping-id connection-info-last-ping-id ;; nullable
       session-id connection-info-session-id ;; nullable
       when-initiated connection-info-when-initiated ;; nullable
       when-disconnected connection-info-when-disconnected ;; nullable
       ])

이 역시 말이 안 되는 값을 표현할 수 있게 합니다.

Clojure

    (def illegal-1
      (make-connection-info
       ;; connection-state
       :connecting
       ...
       ;; when-disconnected
       some-date))

    (def illegal-2
      (make-connection-info
       ;; connection-state
       :connected
       ...
       ;; last-ping-time
       some-date
       ;; last-ping-id
       nil
       ...
       ))

더 나은 표현은 위에서 개선한 OCaml 코드와 상당히 비슷해 보입니다.

Clojure

    (define-record-type Connecting
      ^:private mk-connecting
      connecting?
      [when connecting-when])

    (defn make-connecting [when]
      (assert (date? when))
      (mk-connecting when))

    (define-record-type Connected
      ^:private mk-connected
      connected?
      [last-ping connected-last-ping
       session-id connected-session-id])

    (define-record-type Ping
      make-ping
      ping?
      [when ping-when
       id ping-id])

    (defn make-connected [last-ping session-id]
      (assert (ping? last-ping))
      (assert (session-id? session-id))
      (mk-connected last-ping session-id))

    (define-record-type Disconnected
      ^:private mk-disconnected
      disconnected?
      [when disconnected-when])

    (defn make-disconnected [when]
      (assert (date? when))
      (mk-disconnected when))

    (defn connection-state? [x]
      (or (connecting? x)
          (connected? x)
          (disconnected? x))

    (define-record-type ConnectionInfo
      ^:private mk-connection-info
      connection-info?
      [state connection-info-state
       server connection-info-server])

    (defn make-connection-info [state server]
      (assert (connection-state? state))
      (assert (inet-addr? server))
      (mk-connection-info state server))

Clojure는 제대로 된 정적 타입 검사를 하지 않으므로, 위의 스마트 생성자 `make-connection-info`는 `assert`를 통해 런타임에만 매개변수를 검사할 수 있습니다. 그럼에도 이 후자의 표현은 원래 OCaml 예제와 동일한 이유로 이전 표현보다 선호됩니다.

### 연관 맵(associative maps)과 함수 활용하기

어떤 도메인은 연관 키-값 맵이나 순수 함수의 도움으로 가장 잘 모델링됩니다. 예로, 시계열(time series) 개념을 모델링하고 싶다고 상상해 봅시다. 시계열을 시간-실수(double) 튜플의 리스트로 표현하는 Scala 표현에서 시작합니다.

Scala

    object TimeSeriesService {
      type TimeSeries = List[(Time, Double)]
    }

이는 불가능하고 말이 안 되는 값을 표현할 수 있게 합니다.

Scala

    // t1, t2, t3을 t1 < t2 < t3을 만족하는 타임스탬프라고 하자
    val ts1 = List((t2, 6.5), (t1, 5.0), (t3, 7.3))
    val ts2 = List((t1, 6.5), (t1, 6.5))
    val ts3 = List((t1, 6.5), (t1, 13.4))

`ts3`은 확실히 불가능하고, `ts1`과 `ts2`도 분명 흔한 경우는 아닙니다. 이제 `TimeSeriesService`의 사용자는 이 각각의 경우가 무엇을 나타내는지 골똘히 생각해야 합니다. 더 직관적인 모델은 시계열을 시간에서 실수로 가는 연관 맵으로 표현하는 것입니다.

Scala

    object TimeSeriesService {
      type TimeSeries = Map[Time, Double]
    }

이제 모순되거나 중복된 값은 표현할 수 없습니다. 더욱 나은 모델은 시계열을 시간에서 선택적(optional) 실수로 가는 함수로 표현하는 것입니다.

Scala

    sealed trait TS extends Function1[Time, Option[Double]]

    object TimeSeriesService {
      type TimeSeries = TS
    }

이렇게 하면, 우리가 어떤 타입을 함수처럼 동작하게 만들 수만 있다면, 시계열은 리스트, 맵, 제대로 된 함수, 정적 값 등으로 표현될 수 있습니다.

Scala

    class TSList(list: List[(Time, Option[Double])])
        extends TS {

      def apply(t: Time): Option[Double] =
        list.find( (tx, _) => tx == t ).flatMap(_._2)
    }

    class TSConst(val: Double) extends TS {
      def apply(_: Time): Option[Double] =
        Some(val)
    }

시계열을 함수로 보는 이 모델로, 우리는 불가능한 상태를 표현 불가능하게 만들었을 뿐만 아니라 가능한 모든 합법적 상태를 표현 가능하게 만들었습니다. 후자도 전자만큼 중요할 수 있습니다.

### 스마트 생성자(Smart constructors)

TODO

## 아키텍처적 영향(Architectural impact)

"불가능한 상태를 표현 불가능하게 만들어라"는 소프트웨어의 도메인을 이해하는 데에도, 소프트웨어 아키텍처 자체에도 큰 영향을 줄 수 있습니다. 이 원칙을 염두에 두고 설계된 모델은 더 단순하므로 도메인에 대한 통찰을 얻는 데 도움이 됩니다. 이러한 모델을 사용하는 시스템은 견고하고 느슨하게 결합되어 있습니다.

### 단순성(Simplicity)

"불가능한 상태를 표현 불가능하게 만들어라"는 더 단순한 모델로 이어집니다. 이는 설계자의 정신을 모델의 자의적이고 기술적인 복잡성을 다루어야 하는 부담에서 해방시킵니다. 위에서 설명한, 시계열을 시간의 함수로 보는 모델을 봅시다.

Scala

    sealed trait TS extends Function1[Time, Option[Double]]

함수로 할 수 있는 일은 별로 없으므로, 함수로 *잘못할* 수 있는 일도 별로 없습니다.

### 견고성(Robustness)

시계열을 튜플 리스트로 보는 결함 있는 표현은 많은 불가능한 값을 표현할 수 있게 합니다.

Scala

    object TimeSeriesServiceLegacy {
      def getTimeSeriesData(from: Time, to: Time): List[(Time, Double)] = ???
    }

    // t1, t2, t3을 t1 < t2 < t3을 만족하는 타임스탬프라고 하자
    val ts1 = List((t2, 6.5), (t1, 5.0), (t3, 7.3))
    val ts2 = List((t1, 6.5), (t1, 6.5))
    val ts3 = List((t1, 6.5), (t1, 13.4))

이러한 값들을 표현할 *수 있으므로*, 우리 소프트웨어 시스템은 이들을 일관되게 처리해*야만* 합니다. 즉, 해당 인터페이스의 각 사용자는 이러한 값 중 하나를 마주쳤을 때의 행동 방침에 합의해야 합니다. 시계열을 튜플 리스트로 보는 경우, 우리는 다음 질문들에 만장일치로 답해야 합니다.

  * 시계열 리스트의 순서가 뒤바뀐 항목은 어떻게 처리하는가?

  * 시계열 리스트의 중복 항목은 어떻게 처리하는가?

  * 시계열 리스트의 모순된 항목은 어떻게 처리하는가?

또는, 일부 사용자는 사전에 별도의 검증(validation) 단계에 의존하면서 이러한 경우들을 무시하기로 선택할 수도 있습니다.

이 모든 것은 깨지기 쉬운(brittle) 시스템으로 이어집니다. "불가능한 상태를 표현 불가능하게 만들어라"로 우리는 그 문제를 간단히 정의에서 없애 버립니다. 세부적인 결정들은 단일 파싱(parse) 단계([검증하지 말고 파싱하라](https://functional-architecture.org/parse_dont_validate))에 캡슐화될 수 있으므로, 이후의 모든 계산은 이러한 세세한 사항을 다룰 필요에서 해방됩니다. 도메인 주도 설계(Domain-Driven-Design) 문헌에서는 이러한 파싱 단계를 흔히 부패 방지 계층(Anti-Corruption Layer)이라고 부릅니다.

Scala

    object TimeSeriesServiceAntiCorruption {
      private def parseList(lst: List[(Time, Double)], acc: Map[Time, Double]): Map[Time, Double] =
        lst match {
          case Nil => acc
          case (t, x) :: rest => parseList(rest, acc + (t -> x))
        }

      def getTimeSeriesData(from: Time, to: Time): Map[Time, Double] =
        parseList(TimeSeriesServiceLegacy.getTimeSeriesData(from, to), Map.empty)
    }

### 결합도 낮추기(Decoupling)

TODO

## 역사적 맥락과 논의(Historical context and discussion)

"불가능한 상태를 표현 불가능하게 만들어라"는 명시적으로 "상태(states)"를 언급합니다. 소프트웨어 공학에서 "상태"는 보통 가변 상태(mutable state)를 가리키지만, Ron Minsky의 원래 예제는 이 맥락에서 "상태"가 다른 개념을 가진다는 점을 보여 줍니다. 그의 `connection_state`는 불변(immutable) 값을 기술합니다. "상태"라는 용어는 이 값들이 상태 기계(state machine)의 일부라는 발상을 암시합니다. `connection_state`는 십중팔구 `Connecting ...`에서 `Connected ...`를 거쳐 `Disconnected ...`로 이동합니다.

"불가능한 상태를 표현 불가능하게 만들어라"는 상태 기계 외의 사용 사례에도 적용됩니다. 더 나은 용어는 "불가능한 값을 표현 불가능하게 만들어라(Make illegal values unrepresentable)"일 것입니다. 합과 곱으로 만든 값에 더해, 후자는 함수도 포함하게 되는데, 함수형 프로그래밍에서 함수는 값이기 때문입니다. "불가능한 값을 표현 불가능하게 만들어라"라는 발상은 그렇다면 기계가 검사하는 명세(machine-checked specifications)로 프로그래밍하는 것과 동등해집니다. 후자의 예로는 Conor McBride의 [How to Keep Your Neighbours in Order](https://personal.cis.strath.ac.uk/conor.mcbride/Pivotal.pdf)와 Conal Elliott의 [Symbolic and Automatic Differentiation of Languages](http://conal.net/papers/language-derivatives/)가 있습니다.

# ZIO 메트릭과 관찰 가능성

> 원본: https://zio.dev/reference/observability/metrics/

---

## 목차

1. [메트릭 개요(Metrics Overview)](#1-메트릭-개요metrics-overview)
2. [카운터(Counter)](#2-카운터counter)
3. [게이지(Gauge)](#3-게이지gauge)
4. [히스토그램(Histogram)](#4-히스토그램histogram)
5. [서머리(Summary)](#5-서머리summary)
6. [프리퀀시(Frequency)](#6-프리퀀시frequency)
7. [메트릭 레이블(MetricLabel)](#7-메트릭-레이블metriclabel)
8. [메트릭 적용과 타이머(Applying Metrics & Timer)](#8-메트릭-적용과-타이머applying-metrics--timer)
9. [Prometheus 연동 예제(Prometheus Integration)](#9-prometheus-연동-예제prometheus-integration)
10. [로깅(Logging)](#10-로깅logging)
11. [슈퍼바이저(Supervisor)](#11-슈퍼바이저supervisor)
12. [Chunk(Misc)](#12-chunkmisc)
13. [참고 자료](#13-참고-자료)

---

## 1. 메트릭 개요(Metrics Overview)

분산 시스템(distributed system)에서 다운타임(downtime) 없이 환경을 원활하게 유지하는 것은 매우 어려운 일입니다. ZIO 메트릭(ZIO Metrics)은 복잡하게 복제된(replicated) 서비스 전반에서 메트릭을 수집(capture)하고, 이를 메트릭 서비스(metric service)로 전송하여 분석·조사할 수 있게 해 줍니다.

### 다섯 가지 메트릭 종류(Five Metric Types)

ZIO는 다섯 가지의 서로 다른 메트릭 범주를 지원합니다.

1. **카운터(Counter)** — 요청 수(request counts)처럼 시간이 지남에 따라 증가하기만(increases over time) 하는 값에 사용합니다.
2. **게이지(Gauge)** — 메모리 사용량(memory usage)처럼 시간이 지나면서 올라가거나 내려갈 수 있는(go up or down) 단일 수치값(single numerical value)입니다.
3. **히스토그램(Histogram)** — 요청 지연 시간(request latencies)처럼 관측된 값들의 집합을 버킷(bucket)의 집합에 걸쳐 분포(distribution)로 추적합니다.
4. **서머리(Summary)** — 특정 백분위수(percentile)에 대한 메트릭과 함께, 시계열(time series)의 슬라이딩 윈도우(sliding window)를 나타냅니다.
5. **프리퀀시(Frequency)** — 서로 구별되는 문자열 값(distinct string values)의 발생 횟수(number of occurrences)를 셉니다.

### 애스펙트(Aspect)로서의 메트릭

ZIO에서 메트릭은 ZIO 애스펙트(Aspect)로 정의됩니다. 따라서 이펙트(effect)의 시그니처(signature)를 변경하지 않고도 메트릭을 적용할 수 있습니다. 메트릭은 `@@` 연산자를 통해 이펙트에 부착됩니다.

```scala
import zio._
import zio.metrics._

def memoryUsage: ZIO[Any, Nothing, Double] = {
  import java.lang.Runtime._
  ZIO
    .succeed(getRuntime.totalMemory() - getRuntime.freeMemory())
    .map(_ / (1024.0 * 1024.0)) @@ Metric.gauge("memory_usage")
}
```

### 지원되는 백엔드(Supported Backends)

ZIO 메트릭 커넥터(ZIO Metrics Connectors)는 다음 메트릭 백엔드(backend)와 통합됩니다.

- Prometheus
- Datadog
- New Relic
- StatsD

---

## 2. 카운터(Counter)

### 정의(Definition)

`Counter`는 "시간이 지남에 따라 증가될 수 있는 단일 수치값을 나타내는 메트릭(a metric representing a single numerical value that may be incremented over time)"입니다. 시점별 스냅숏(point-in-time snapshot)이 아니라 누적값(cumulative value)을 추적하므로, 오직 증가하기만(monotonically increasing) 하는 수량을 모니터링하는 데 적합합니다.

### API 메서드

ZIO 라이브러리는 세 가지 생성자(constructor) 메서드를 제공합니다.

```scala
object Metric {
  def counter(name: String): Counter[Long] = ???
  def counterDouble(name: String): Counter[Double] = ???
  def counterInt(name: String): Counter[Int] = ???
}
```

### `@@`로 적용하기

카운터는 `@@` 연산자를 사용하여 이펙트에 적용합니다.

```scala
val countAll = Metric.counter("countAll").fromConst(1)
val myApp = for {
  _ <- ZIO.unit @@ countAll
  _ <- ZIO.unit @@ countAll
} yield ()
```

### 사용 예시(Usage Examples)

**반복(repeating) 시나리오:**

```scala
(zio.Random.nextLongBounded(10) @@ 
  Metric.counter("request_counts")).repeatUntil(_ == 7)
```

**값과 함께 사용:**

```scala
val countBytes = Metric.counter("countBytes")
val myApp = Random.nextLongBetween(0, 100) @@ countBytes
```

### 주요 사용 사례(Key Use Cases)

카운터 메트릭은 다음과 같은 경우에 적합합니다.

- 요청 수(Request counts)
- 완료된 작업(Completed tasks)
- 에러 수(Error counts)
- 단조 증가하는(monotonically increasing) 모든 값

---

## 3. 게이지(Gauge)

### 정의(Definition)

`Gauge`는 "설정(set)되거나 조정(adjusted)될 수 있는 단일 수치값(a single numerical value that may be set or adjusted)"을 나타냅니다. 누적값을 추적하는 카운터와 달리, 게이지는 특정 시점의 현재 값(current value)을 측정합니다.

### 주요 특징(Key Characteristics)

- 시간이 지나면서 변하는 `Double` 타입의 이름이 붙은 변수(named variable)입니다.
- 절대값(absolute value)으로 설정하거나, 현재 값을 기준으로 상대적으로(relative) 조정할 수 있습니다.
- 위아래로 변동(fluctuate)하는 메트릭에 이상적입니다.

### API

```scala
object Metric {
  def gauge(name: String): Gauge[Double] = ???
}
```

### 일반적인 사용 사례(Typical Use Cases)

- 메모리 사용량(Memory Usage)
- 큐 크기(Queue Size)
- 진행 중인 요청 수(In-Progress Request Counts)
- 온도(Temperature)

### 코드 예제(Code Example)

```scala
import zio._
import zio.metrics._
val absoluteGauge = Metric.gauge("setGauge")

for {
  _ <- Random.nextDoubleBetween(0.0d, 100.0d) @@ absoluteGauge @@ countAll
} yield ()
```

위 예제는 `Double` 값을 산출하는 이펙트에 게이지를 적용하면서, `@@` 연산자를 사용해 추가적인 메트릭 애스펙트를 함께 결합하는 모습을 보여 줍니다.

---

## 4. 히스토그램(Histogram)

### 정의(Definition)

히스토그램(Histogram)은 "시간에 걸친 누적값의 분포를 갖는 수치값들의 모음(a collection of numerical with the distribution of the cumulative values over time)"을 나타내는 메트릭입니다. 측정값들을 서로 구별되는 구간(interval), 즉 버킷(bucket)으로 조직화하고, 각 버킷에 속하는 값의 빈도(frequency)를 기록합니다.

### 동작 방식(How It Works)

히스토그램은 `Double` 값을 관측(observe)하여 미리 정의된 버킷에서 관측값의 개수를 셉니다. 각 버킷은 상한 경계(upper boundary) `b`를 가지며, 관측된 값 `v`가 `b` 이하일 때(`v <= b`) 해당 버킷의 카운트가 1 증가합니다. 마지막 버킷은 항상 `Double.MaxValue`로 설정되어, 모든 관측값이 빠짐없이 포착되도록 보장합니다.

### API

```scala
object Metric {
  def histogram(
      name: String,
      boundaries: Histogram.Boundaries
  ): Histogram[Double] = ???
  
  def timer(
      name: String,
      description: String,
      chronoUnit: ChronoUnit
  ): Metric[MetricKeyType.Histogram, Duration, MetricState.Histogram] = ???
  
  def timer(
      name: String,
      chronoUnit: ChronoUnit,
      boundaries: Chunk[Double]
  ): Metric[MetricKeyType.Histogram, Duration, MetricState.Histogram] = ???
}
```

### 경계(Boundaries)

경계는 `MetricKeyType.Histogram.Boundaries.linear(0, 10, 11)`과 같이 생성합니다. 위 호출은 0부터 100까지 10 단위(step)로 12개의 버킷(0~100 구간의 11개 버킷 + `Double.MaxValue` 버킷)을 만듭니다.

### 코드 예제: 선형 버킷(Linear Buckets)

```scala
import zio._
import zio.metrics._

val histogram = Metric.histogram(
  "histogram", 
  MetricKeyType.Histogram.Boundaries.linear(0, 10, 11)
)

Random.nextDoubleBetween(0.0d, 120.0d) @@ histogram
```

### 사용 사례(Use Cases)

히스토그램은 다음과 같은 시나리오에 적합합니다.

- 백분위수(percentile) 계산이 필요한 경우
- 값의 범위(value range)를 미리 추정할 수 있는 경우
- 정확한 값보다 허용 가능한 근사치(acceptable approximation)로 충분한 경우
- 여러 인스턴스(multi-instance)에 걸친 집계(aggregation)가 필요한 경우 (예: HTTP 응답 시간, 지연 시간, 처리량)

---

## 5. 서머리(Summary)

### 정의(Definition)

`Summary`는 지정된 백분위수(즉, 분위수(quantile))에 대한 메트릭과 함께, 시계열(time series) 데이터의 슬라이딩 윈도우(sliding window)를 나타냅니다. `Double` 값을 관측하며, 히스토그램처럼 버킷 카운터를 직접 변경하는 대신 내부 샘플(internal samples)을 유지합니다.

### 분위수와 백분위수(Quantiles and Percentiles)

"분위수(quantile)는 `0 <= q <= 1`을 만족하는 `Double` 값 `q`로 정의되며, 그 결과 또한 `Double`로 해석(resolve)됩니다." 일반적으로 추적되는 분위수는 0.5(중앙값(median))와 0.95입니다.

오차 한계(error margin) `e`를 설정하여 유연성을 둘 수 있습니다. 분위수 `q`는, `n`이 `v` 이하인 값의 개수이고 `s`가 샘플 집합 크기(sample set size)일 때, `(1-e)q * s <= n <= (1+e)q * s`를 만족하면 값 `v`로 해석됩니다.

### API 파라미터

```scala
object Metric {
  def summary(
    name: String,
    maxAge: Duration,
    maxSize: Int,
    error: Double,
    quantiles: Chunk[Double]
  ): Summary[Double]
  
  def summaryInstant(
    name: String,
    maxAge: Duration,
    maxSize: Int,
    error: Double,
    quantiles: Chunk[Double]
  ): Summary[(Double, java.time.Instant)]
}
```

- **maxAge**: 슬라이딩 윈도우 안에서 샘플의 최대 수명(Maximum age of samples)
- **maxSize**: 유지되는 샘플의 최대 개수(Maximum number of samples retained)
- **error**: 분위수 계산에 적용되는 오차 한계(Error margin)
- **quantiles**: 원하는 백분위수를 나타내는 `Double` 값들의 Chunk

### 코드 예제(Code Example)

```scala
import zio._
import zio.metrics._
import zio.metrics.Metric.Summary

val summary: Summary[Double] =
  Metric.summary(
    name = "mySummary",
    maxAge = 1.day,
    maxSize = 100,
    error = 0.03d,
    quantiles = Chunk(0.1, 0.5, 0.9)
  )

Random.nextDoubleBetween(100, 500) @@ summary
```

### 사용 사례(Use Cases)

서머리는 히스토그램의 정확도가 부족한 경우의 지연 시간(latency) 모니터링, 값의 범위를 미리 추정할 수 없는 경우, 또는 인스턴스 간 집계가 필요하지 않은 경우에 적합한 선택입니다.

---

## 6. 프리퀀시(Frequency)

### 정의(Definition)

`Frequency` 메트릭은 "지정된 값들의 발생 횟수(the number of occurrences of specified values)"를 나타냅니다. 자동으로 확장되는(auto-expanding) 카운터 집합처럼 동작하며, 새로운 값이 관측되면 그 값에 대한 카운터가 자동으로 생성됩니다.

기술적으로 프리퀀시는 "같은 이름과 태그(tags)를 공유하는 관련 카운터들의 집합(a set of related counters sharing the same name and tags)"으로 구성됩니다. 이 카운터들은 추가로 설정 가능한 태그(configurable tag)에 의해 서로 구별되며, 그 태그의 값들이 관측된 서로 다른 값(distinct values)을 나타냅니다.

### 주요 사용 사례(Primary Use Cases)

프리퀀시 메트릭은 서로 구별되는 문자열 값의 발생을 추적하며, 주로 두 가지 시나리오에서 사용됩니다.

1. **서비스 호출 횟수 집계(Service invocation counting)** — 이름이 붙은 각 서비스가 몇 번 호출되는지 모니터링
2. **실패 유형 빈도(Failure type frequency)** — 서로 다른 실패 유형이 얼마나 자주 발생하는지 측정

### API 레퍼런스

핵심 생성자는 다음 형태를 따릅니다.

```scala
object Metric {
  def frequency(name: String): Frequency[String] = ???
}
```

### 코드 예제(Code Example)

프리퀀시 메트릭 생성:

```scala
import zio.metrics._
val freq = Metric.frequency("MySet")
```

문자열을 산출하는 이펙트에 적용:

```scala
import zio._
(Random.nextIntBounded(10).map(v => s"MyKey-$v") @@ freq).repeatN(100)
```

위 코드는 랜덤 키를 생성하고, 100번 반복하는 동안 서로 구별되는 각 값의 발생 횟수를 셉니다.

---

## 7. 메트릭 레이블(MetricLabel)

### 정의(Definition)

`MetricLabel`은 더 세분화된(granular) 메트릭 분석을 가능하게 하는, 키-값 쌍(key-value pair) 형태의 메타데이터(metadata)를 나타냅니다. ZIO 메트릭은 "레이블 기반 차원 데이터 모델(label-based dimensional data model)"을 사용합니다. 즉, 메트릭은 이름과 함께 부착된 키-값 쌍을 가지며, 이 레이블들은 모니터링 대시보드에서 필터링(filtering)과 쿼리(querying)를 위한 일급 시민(first-class citizen)이 됩니다.

### 목적(Purpose)

레이블을 사용하면 추가적인 차원(dimension)별로 메트릭을 분리하여 추적할 수 있습니다. 예를 들어 서비스 응답 시간 메트릭을 클라이언트(client)별로 레이블을 통해 구분할 수 있습니다.

### 흔한 레이블 예시(Common Label Examples)

- 엔드포인트(Endpoint): `/api/users`, `/api/documents`
- HTTP 메서드(HTTP Method): `POST`, `GET`
- 배포 환경(Deployment Environment): `Staging`, `Production`
- HTTP 응답 코드(HTTP Response Code)
- 에러 코드(Error code): `404`, `503`
- 데이터센터 존(Datacenter Zone): `us-east`, `eu-west`

### 코드 예제(Code Example)

```scala
import zio._
import zio.metrics._

val counter = Metric.counter("http_requests")
  .tagged(
    MetricLabel("env", "production"),
    MetricLabel("method", "GET"),
    MetricLabel("endpoint", "/api/users"),
    MetricLabel("zone", "ap-northeast"),
  )
```

`.tagged()` 메서드는 메트릭에 레이블을 부착합니다.

### 쿼리 측면의 이점(Querying Benefits)

레이블은 다음과 같은 세분화된 모니터링 쿼리를 가능하게 합니다.

- 엔드포인트별 요청 수(Request counts per endpoint)
- 존(zone)별 SLA 위반 위험도(SLA violation risk by zone)
- 엔드포인트별 지연 시간 비교(Endpoint latency comparisons)

---

## 8. 메트릭 적용과 타이머(Applying Metrics & Timer)

### `@@` 연산자로 메트릭 적용하기

ZIO 메트릭의 핵심 설계는, 메트릭이 ZIO 애스펙트(Aspect)로 정의되어 `@@` 연산자를 통해 이펙트에 부착된다는 점입니다. 이렇게 하면 이펙트의 타입 시그니처(type signature)를 바꾸지 않은 채로 관측 가능성(observability)을 추가할 수 있습니다.

여러 메트릭 애스펙트를 하나의 이펙트에 연쇄적으로 결합할 수도 있습니다.

```scala
for {
  _ <- Random.nextDoubleBetween(0.0d, 100.0d) @@ absoluteGauge @@ countAll
} yield ()
```

### `Metric.timer`

`Metric.timer`는 이펙트의 실행 소요 시간(duration)을 측정하기 위한 메트릭으로, 내부적으로 히스토그램(Histogram) 기반으로 구현됩니다. `ChronoUnit`(시간 단위)을 받아 측정 결과를 해당 단위로 기록합니다.

```scala
object Metric {
  def timer(
      name: String,
      description: String,
      chronoUnit: ChronoUnit
  ): Metric[MetricKeyType.Histogram, Duration, MetricState.Histogram] = ???
  
  def timer(
      name: String,
      chronoUnit: ChronoUnit,
      boundaries: Chunk[Double]
  ): Metric[MetricKeyType.Histogram, Duration, MetricState.Histogram] = ???
}
```

타이머는 `Duration` 값을 입력으로 받아, 그 분포를 히스토그램(`MetricState.Histogram`)으로 집계합니다. 두 번째 오버로드(overload)는 직접 버킷 경계(`boundaries: Chunk[Double]`)를 지정할 수 있게 해 줍니다.

---

## 9. Prometheus 연동 예제(Prometheus Integration)

다음은 Prometheus로 메트릭을 수집·노출하는 완전한 예제 애플리케이션입니다. `/metrics` 엔드포인트는 Prometheus가 스크레이프(scrape)할 수 있는 메트릭 텍스트를 반환하고, `/foo` 엔드포인트는 호출될 때마다 메모리 사용량 게이지를 갱신합니다.

```scala
package zio.examples.metrics

import zio._
import zio.http._
import zio.metrics.Metric
import zio.metrics.connectors.prometheus.PrometheusPublisher
import zio.metrics.connectors.{MetricsConfig, prometheus}

object MetricAppExample extends ZIOAppDefault {
  def memoryUsage: ZIO[Any, Nothing, Double] = {
    import java.lang.Runtime._
    ZIO
      .succeed(getRuntime.totalMemory() - getRuntime.freeMemory())
      .map(_ / (1024.0 * 1024.0)) @@ Metric.gauge("memory_usage")
  }

  private val httpApp =
    Routes(
      Method.GET / "metrics" ->
        handler(ZIO.serviceWithZIO[PrometheusPublisher](_.get.map(Response.text))),
      Method.GET / "foo" -> handler {
        for {
          _    <- memoryUsage
          time <- Clock.currentDateTime
        } yield Response.text(s"$time\t/foo API called")
      }
    )

  override def run = Server
    .serve(httpApp)
    .provide(
      Server.default,
      prometheus.prometheusLayer,
      prometheus.publisherLayer,
      ZLayer.succeed(MetricsConfig(5.seconds))
    )
}
```

이 예제에서 `MetricsConfig(5.seconds)`는 메트릭 폴링(polling) 주기를 5초로 설정합니다. `prometheus.prometheusLayer`와 `prometheus.publisherLayer`는 각각 Prometheus 메트릭 백엔드와 퍼블리셔(publisher) 레이어(layer)를 제공합니다.

---

## 10. 로깅(Logging)

ZIO는 경량(lightweight)의 내장 로깅 파사드(logging facade)를 제공합니다.

### 기본 로깅(Basic Logging)

핵심 함수는 `ZIO.log`입니다.

```scala
import zio._
val app = for {
  _ <- ZIO.log("Application started!")
  name <- Console.readLine("Please enter your name: ")
  _ <- ZIO.log(s"User entered its name: $name")
  _ <- Console.printLine(s"Hello, $name")
} yield ()
```

### 로그 레벨(Logging Levels)

`ZIO.logLevel`을 사용하여 로그 레벨을 지정할 수 있습니다.

```scala
ZIO.logLevel(LogLevel.Warning) {
  ZIO.log("The response time exceeded its threshold!")
}
```

직접 사용할 수 있는 레벨별 함수들:

- `ZIO.logDebug`
- `ZIO.logError`
- `ZIO.logFatal`
- `ZIO.logInfo`
- `ZIO.logWarning`

에러 레벨(error level) 예시:

```scala
ZIO.logError("File does not exist: ~/var/www/favicon.ico")
```

### 로그 스팬(Log Spans)

스팬(span)을 사용하면 연산의 소요 시간(duration)을 측정할 수 있습니다.

```scala
ZIO.logSpan("myspan") {
  ZIO.sleep(1.second) *> ZIO.log("The job is finished!")
}
```

"ZIO 로깅은 해당 스팬의 실행 소요 시간(running duration)을 계산하여, 그 스팬 레이블(span label)에 해당하는 로깅 데이터에 포함시킵니다."

### 로그 애너테이션(Log Annotations)

#### 내장 애너테이션(Built-in Annotations)

`ZIO.logAnnotate`를 사용하여 컨텍스트 정보(contextual information)를 추가합니다.

```scala
ZIO.logAnnotate("correlation_id", user) {
  for {
    _ <- ZIO.log("fetching user from database")
  } yield ()
}
```

#### 타입이 지정된 애너테이션(Typed Annotations)

구조화된 데이터(structured data)를 위해 `LogAnnotation`으로 커스텀 애너테이션을 정의할 수 있습니다.

```scala
private val userLogAnnotation = 
  LogAnnotation[User]("user", (_, u) => u, _.toJson)
```

`@@` 연산자로 적용합니다.

```scala
ZIO.logInfo("Starting operation") @@ userLogAnnotation(user)
```

---

## 11. 슈퍼바이저(Supervisor)

### 정의(Definition)

`Supervisor[A]`는 파이버(fiber)의 시작(launching)과 종료(termination)를 모니터링하면서, 그 슈퍼비전(supervision) 활동으로부터 타입 `A`의 관측 가능한 값(visible value)을 산출합니다.

### 생성 메서드(Creation Methods)

#### `Supervisor.track`

자식 파이버들을 하나의 집합(set)에 유지하는 슈퍼바이저를 생성합니다. `weak` 불리언(boolean) 파라미터는 `WeakSet`을 사용할지 표준 집합(standard set)을 사용할지를 결정합니다.

```scala
val supervisor = Supervisor.track(true)
```

이는 `UIO[Supervisor[Chunk[Fiber.Runtime[Any, Any]]]]`을 반환하며, 프로그램 파이버들의 상태를 주기적으로 보고(periodic status reporting)할 수 있게 합니다.

#### `Supervisor.fibersIn`

기존의 정렬된 파이버 집합(sorted set of fibers)으로 초기화된 슈퍼바이저를 생성합니다.

```scala
def fiberListSupervisor = for {
  ref <- ZIO.succeed(new AtomicReference(SortedSet.from(fibers)))
  s <- Supervisor.fibersIn(ref)
} yield (s)
```

### 파이버 슈퍼비전(Supervising Fibers)

`ZIO#supervised` 함수는 이펙트에 슈퍼비전을 적용합니다. 슈퍼바이저를 인자로 받아, 자식 파이버의 동작이 그 슈퍼바이저에게 보고되는 이펙트를 반환합니다.

```scala
val supervised = supervisor.flatMap(s => fib(20).supervised(s))
```

### 전체 예제(Complete Example)

```scala
import zio._
import zio.Fiber.Status

object SupervisorExample extends ZIOAppDefault {
  def run = for {
    supervisor <- Supervisor.track(true)
    fiber <- fib(20).supervised(supervisor).fork
    policy = Schedule
      .spaced(500.milliseconds)
      .whileInputZIO[Any, Unit](_ => fiber.status.map(_ != Status.Done))
    logger <- monitorFibers(supervisor)
      .repeat(policy).fork
    _ <- logger.join
    result <- fiber.join
    _ <- Console.printLine(s"fibonacci result: $result")
  } yield ()

  def monitorFibers(supervisor: Supervisor[Chunk[Fiber.Runtime[Any, Any]]]) = for {
    length <- supervisor.value.map(_.length)
    _ <- Console.printLine(s"number of fibers: $length")
  } yield ()

  def fib(n: Int): ZIO[Any, Nothing, Int] =
    if (n <= 1) {
      ZIO.succeed(1)
    } else {
      for {
        _ <- ZIO.sleep(500.milliseconds)
        fiber1 <- fib(n - 2).fork
        fiber2 <- fib(n - 1).fork
        v2 <- fiber2.join
        v1 <- fiber1.join
      } yield v1 + v2
    }
}
```

이 예제는 피보나치(fibonacci) 계산을 위해 생성되는 파이버 수를, 500밀리초마다 모니터링하며 콘솔에 출력합니다. `Schedule.spaced(500.milliseconds)` 정책과 `whileInputZIO`를 결합하여, 메인 파이버가 `Done` 상태가 될 때까지 모니터링 루프를 반복합니다.

---

## 12. Chunk(Misc)

### 정의(Definition)

`Chunk[A]`는 타입 `A`의 값들로 이루어진 순서 있는 컬렉션(ordered collection)을 나타냅니다. "배열(array)로 뒷받침되지만, 순수 함수형의 안전한 인터페이스(purely functional, safe interface)를 노출"하면서도 높은 성능 특성을 유지합니다. 처음에는 ZIO 스트림(stream)을 위해 작성되었으나, 이후 다른 용도로도 유용한 매력적인 범용 컬렉션 타입(general collection type)으로 발전했습니다.

### 왜 Chunk를 사용하는가(Why Use Chunk)

- **불변성(Immutability)**: 스칼라(Scala)의 가변(mutable) 배열과 달리, Chunk는 성능을 희생하지 않으면서도 불변(immutable) 인터페이스를 제공합니다.
- **원시 타입에 대한 제로 박싱(Zero Boxing for Primitives)**: "Chunk는 배열로 뒷받침되므로, 불변 인터페이스를 제공하면서도 원시 타입(primitive)에 대해 박싱(boxing)이 전혀 없습니다."
- **ClassTag 불필요(No ClassTag Requirement)**: "Chunk는 `ClassTag`가 필요 없습니다." 따라서 제네릭 배열 생성(generic array creation)의 번거로움을 없애 줍니다.
- **높은 성능(High Performance)**: "Chunk는 단일 원소를 덧붙이거나(appending) 두 Chunk를 연결(concatenating)하는 것 같은 작업에 대해 특화된 연산(specialized operations)을 가지며", 표준 라이브러리 구현보다 뛰어난 성능을 제공합니다.

> 참고: 스트림을 다룰 때 우리는 항상 청크(chunk)를 다룹니다. 개별 원소(individual element)로 이루어진 스트림은 없으며, 스트림의 기반 구현(underlying implementation)에는 항상 청크가 존재합니다. 따라서 스트림을 평가(evaluate)하여 원소를 꺼낼 때, 실제로는 원소들의 청크(chunk of elements)를 꺼내는 것입니다.

### 생성 메서드(Creation Methods)

```scala
Chunk.empty                          // 빈 청크
Chunk(1, 2, 3)                       // 직접 값으로 생성
Chunk.fromIterable(List(1, 2, 3))    // 컬렉션으로부터 생성
Chunk.fromArray(Array(1, 2, 3))      // 배열로부터 생성
Chunk.fill(3)(0)                     // 반복된 값으로 채우기
Chunk.unfold(0)(n => if (n < 8) Some((n*2, n+2)) else None)
```

### 연산(Operations)

- **연결(Concatenation)**: `Chunk(1,2,3) ++ Chunk(4,5,6)`
- **수집(Collecting)**: `chunk.collect { case string: String => string }`
- **버리기(Dropping)**: `Chunk(9, 2, 5, 1, 6).drop(1)` 또는 `.dropWhile(_ >= 2)`
- **변환(Conversion)**: `.toArray` 및 `.toSeq` 메서드를 사용할 수 있습니다.

---

## 13. 참고 자료

- [ZIO Metrics (Overview)](https://zio.dev/reference/observability/metrics/)
- [Counter - ZIO Metrics](https://zio.dev/reference/observability/metrics/counter)
- [Gauge - ZIO Metrics](https://zio.dev/reference/observability/metrics/gauge)
- [Histogram - ZIO Metrics](https://zio.dev/reference/observability/metrics/histogram)
- [Summary - ZIO Metrics](https://zio.dev/reference/observability/metrics/summary)
- [Frequency - ZIO Metrics](https://zio.dev/reference/observability/metrics/frequency)
- [MetricLabel - ZIO Metrics](https://zio.dev/reference/observability/metrics/metriclabel/)
- [JVM Metrics - ZIO Metrics](https://zio.dev/reference/observability/metrics/jvm/)
- [Logging - ZIO Observability](https://zio.dev/reference/observability/logging)
- [Supervisor - ZIO Observability](https://zio.dev/reference/observability/supervisor)
- [Chunk - ZIO Reference](https://zio.dev/reference/stream/chunk/)
- [ZIO Metrics Connectors](https://zio.dev/zio-metrics-connectors/metrics/metric-reference/)

# 프로파일 타입과 pprof 형식

> 이 문서는 Pyroscope가 다루는 프로파일의 종류와 표준 형식인 pprof, 각 프로파일을 어떤 상황에서 사용하는지 설명합니다.
> 원본: https://grafana.com/docs/pyroscope/latest/view-and-analyze-profile-data/profile-types/

---

## 목차

1. [프로파일이란](#프로파일이란)
2. [pprof 표준 형식](#pprof-표준-형식)
3. [프로파일 타입별 상세](#프로파일-타입별-상세)
4. [샘플링 프로파일러 vs 계측 프로파일러](#샘플링-프로파일러-vs-계측-프로파일러)
5. [언어별 지원 매트릭스](#언어별-지원-매트릭스)
6. [언제 어떤 프로파일을 보아야 하나](#언제-어떤-프로파일을-보아야-하나)

---

## 프로파일이란

**프로파일(Profile)**은 짧은 시간 동안 애플리케이션이 어디서 어떻게 자원을 사용했는지를 **콜 스택(call stack)** 의 집합으로 표현한 데이터입니다.

기본 단위는 다음과 같습니다.

- **샘플(Sample)**: 어떤 시점에 캡처된 콜 스택 + 그 시점의 측정 값(예: CPU 시간, 할당된 바이트)
- **스택 트레이스(Stack Trace)**: `func1 -> func2 -> func3` 형태의 호출 체인
- **위치(Location)**: 함수 ID, 라인 번호, 인라인 정보
- **함수(Function)**: 함수 이름, 파일명, 시작 라인

샘플들의 집합을 시각화한 것이 **플레임 그래프(Flame Graph)** 입니다.

---

## pprof 표준 형식

Pyroscope는 [Google pprof](https://github.com/google/pprof) 의 protobuf 기반 형식을 표준으로 채택했습니다.

### 핵심 메시지 구조

```
Profile {
  sample_type[]   // 측정 항목 목록 (예: cpu/nanoseconds)
  sample[]        // 콜 스택 + 값
  mapping[]       // 바이너리/공유 라이브러리 매핑
  location[]      // 명령어 위치 (PC 단위)
  function[]      // 함수 메타데이터
  string_table[]  // 문자열 dedup 테이블
  duration_nanos  // 프로파일 수집 기간
  period          // 샘플링 주기
  default_sample_type
}
```

### 장점

- **언어 중립적**: protobuf로 정의되어 모든 언어에서 다룰 수 있음
- **압축 효율**: 문자열/위치/함수 dedup으로 작은 크기
- **다양한 도구 호환**: `go tool pprof`, Speedscope, Pyroscope 등
- **다중 측정**: 한 프로파일에 여러 sample_type을 담을 수 있음 (예: CPU와 inuse_space 동시)

### 라벨 (Labels)

Pyroscope는 pprof의 표준 필드에 추가로 **외부 라벨(external labels)**을 사용해 시리즈를 구분합니다.

```
service_name="checkout"
env="prod"
cluster="us-east-1"
host="checkout-7d8f-abc"
```

이 라벨은 LabelSelector로 쿼리에 사용됩니다.

---

## 프로파일 타입별 상세

### CPU 프로파일

**측정**: CPU 시간 — 어떤 함수가 CPU를 가장 많이 차지했는가.

- 단위: `cpu/nanoseconds` 또는 `samples/count`
- 수집 방식: 통상 **샘플링** (예: 100Hz로 스택 캡처)
- 오버헤드: 매우 낮음 (수 % 미만)
- 핵심 사용처: 핫스팟 분석, 컴퓨팅 비용 절감, 알고리즘 비효율 발견

```
function: 27.3% CPU
└─ function: heavyComputation
   └─ function: regexCompile
      └─ function: parseRegex
```

### Memory 프로파일 (Heap)

Go의 분류로 4가지가 있고, 다른 언어도 유사합니다.

#### inuse_space

- **측정**: 현재 힙에 살아있는 객체의 바이트
- **사용처**: 메모리 사용량 분석, OOM 직전 상태 진단

#### inuse_objects

- **측정**: 현재 힙에 살아있는 객체의 개수
- **사용처**: 객체 누수, GC 압박 분석

#### alloc_space

- **측정**: 프로파일 기간 동안 누적 할당된 바이트
- **사용처**: 할당 핫스팟 (단명 객체가 많이 만들어지는 함수)

#### alloc_objects

- **측정**: 누적 할당된 객체 개수
- **사용처**: 작은 객체의 빈번한 할당으로 인한 GC 부담

### Goroutines (Go)

- **측정**: 현재 실행 중인 고루틴 수와 각자의 스택
- **사용처**: 고루틴 누수 (`time.After` 의 잘못된 사용 등)
- **단위**: count

### Mutex 프로파일

- **측정**: 락(mutex) 대기로 소모된 시간
- **사용처**: 컨텐션이 큰 락 발견, 멀티 코어 활용 저하 분석
- Go의 경우 `runtime.SetMutexProfileFraction` 으로 활성화 필요

### Block 프로파일

- **측정**: 동기화 객체에서 블록(블로킹)된 시간 (channel, mutex 대기 등)
- **사용처**: 동기화 병목, I/O 대기 분석
- Go의 경우 `runtime.SetBlockProfileRate` 로 활성화 필요

### Wall-clock 프로파일

- **측정**: 실시간(wall clock) 기준 시간 — CPU + I/O 대기 + sleep 모두 포함
- **사용처**: 응답 지연(latency) 분석. Java async-profiler의 `wall` 모드 등

### 기타

| 타입 | 설명 |
|------|------|
| `process_cpu` | OS 관점 프로세스 CPU 사용 (eBPF 등) |
| `goroutine` | Go 활성 고루틴 |
| `thread_create` | 스레드 생성 위치 (Go) |
| `exception_samples` | 예외 발생 위치 (.NET 등) |
| `lock` | Java JVM 락 컨텐션 |
| `live` | 살아있는 객체 (Java JFR 등) |

---

## 샘플링 프로파일러 vs 계측 프로파일러

### 샘플링 (Sampling)

- 일정 주기(예: 100Hz)마다 현재 스택을 캡처
- **오버헤드 매우 낮음** → 프로덕션 적합
- 통계적 정확도: 짧은 함수는 누락될 수 있음
- 예: Go runtime, async-profiler, perf, eBPF 프로파일러

### 계측 (Instrumentation)

- 컴파일 타임 또는 런타임에 모든 함수 진입/종료에 hook 추가
- 정확한 호출 횟수와 시간
- **오버헤드 큼** → 프로덕션 비권장
- 예: 일부 APM 도구, valgrind callgrind

### Pyroscope의 선택

Pyroscope는 **샘플링 기반 프로파일러를 권장** 합니다. 모든 공식 SDK와 eBPF 통합은 샘플링 방식입니다. 이는 "항상 켜둘 수 있는" 연속 프로파일링 철학과 일치합니다.

---

## 언어별 지원 매트릭스

| 언어 | CPU | Heap | Goroutines | Mutex/Block | Wall | 비고 |
|------|-----|------|------------|-------------|------|------|
| Go | ✅ | ✅ | ✅ | ✅ | - | 표준 `runtime/pprof` 사용 |
| Java | ✅ | ✅ (alloc/inuse) | - | ✅ (lock) | ✅ | async-profiler 기반 |
| Python | ✅ | - | - | - | ✅ | py-spy / pyroscope SDK |
| Ruby | ✅ | - | - | - | ✅ | rbspy 기반 |
| Node.js | ✅ | ✅ | - | - | ✅ | V8 inspector |
| .NET | ✅ | ✅ | - | ✅ | ✅ | dotnet diagnostics |
| Rust | ✅ | - | - | - | - | pprof-rs |
| eBPF | ✅ (process_cpu) | - | - | - | - | 무계측, 커널 레벨 |

---

## 언제 어떤 프로파일을 보아야 하나

### "응답 시간이 느려요"

1. **Wall-clock** (지원 언어) → 함수가 어디서 시간을 보내고 있는지
2. **Block** → I/O/락 대기 비중 확인
3. **CPU** → 실제 계산 비용 확인

### "메모리가 계속 늘어나요 (누수 의심)"

1. **inuse_space** 시간 비교(diff) → 어느 함수의 객체가 줄지 않는가
2. **alloc_space** → 그 객체를 누가 할당하는가

### "GC 시간이 길어요"

1. **alloc_objects** → 많은 작은 객체를 만드는 핫스팟
2. **alloc_space** → 큰 객체 핫스팟

### "CPU 비용이 너무 비싸요 (클라우드 비용 절감)"

1. **CPU** → 가장 비싼 함수 식별
2. 다른 시간/배포와 **diff** 비교 → 회귀 발견

### "데드락/지연 의심"

1. **Goroutines/Threads** → 비정상적으로 많은 stack인지
2. **Mutex** → 어떤 락이 컨텐션이 큰지

### "응답에 가끔 큰 지연이 있어요 (tail latency)"

- 트레이스에서 느린 스팬 발견 → **Span Profiles** 로 해당 시간대 프로파일 점프

---

## 다음 단계

- [05_flamegraphs.md](./05_flamegraphs.md) - Flame Graph 읽고 분석하기
- [06_instrumentation.md](./06_instrumentation.md) - 언어별 계측 방법
- [08_use_cases.md](./08_use_cases.md) - 실전 트러블슈팅 사례

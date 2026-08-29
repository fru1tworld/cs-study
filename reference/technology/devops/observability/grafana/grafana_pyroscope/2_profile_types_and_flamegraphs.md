# 프로파일 타입과 Flame Graph 분석

## 프로파일 타입과 pprof 형식

> 원본: https://grafana.com/docs/pyroscope/latest/view-and-analyze-profile-data/profile-types/

---

### 목차

1. [프로파일이란](#프로파일이란)
2. [pprof 표준 형식](#pprof-표준-형식)
3. [프로파일 타입별 상세](#프로파일-타입별-상세)
4. [샘플링 프로파일러 vs 계측 프로파일러](#샘플링-프로파일러-vs-계측-프로파일러)
5. [언어별 지원 매트릭스](#언어별-지원-매트릭스)
6. [언제 어떤 프로파일을 보아야 하나](#언제-어떤-프로파일을-보아야-하나)

---

### 프로파일이란

프로파일(Profile)은 짧은 시간 동안 애플리케이션이 어디서 어떻게 자원을 사용했는지를 콜 스택(call stack)의 집합으로 표현한 데이터임.

기본 단위는 다음과 같음.

- **샘플(Sample)**: 어떤 시점에 캡처된 콜 스택 + 그 시점의 측정 값(예: CPU 시간, 할당된 바이트)
- **스택 트레이스(Stack Trace)**: `func1 -> func2 -> func3` 형태의 호출 체인
- **위치(Location)**: 함수 ID, 라인 번호, 인라인 정보
- **함수(Function)**: 함수 이름, 파일명, 시작 라인

샘플들의 집합을 시각화한 것이 플레임 그래프(Flame Graph)임.

---

### pprof 표준 형식

Pyroscope는 [Google pprof](https://github.com/google/pprof)의 protobuf 기반 형식을 표준으로 채택함.

#### 핵심 메시지 구조

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

#### 장점

- **언어 중립적**: protobuf로 정의되어 모든 언어에서 다룰 수 있음
- **압축 효율**: 문자열/위치/함수 dedup으로 작은 크기
- **다양한 도구 호환**: `go tool pprof`, Speedscope, Pyroscope 등
- **다중 측정**: 한 프로파일에 여러 sample_type을 담을 수 있음 (예: CPU와 inuse_space 동시)

#### 라벨 (Labels)

Pyroscope는 pprof의 표준 필드에 추가로 외부 라벨(external labels)을 사용해 시리즈를 구분함.

```
service_name="checkout"
env="prod"
cluster="us-east-1"
host="checkout-7d8f-abc"
```

이 라벨은 LabelSelector로 쿼리에 사용됨.

---

### 프로파일 타입별 상세

#### CPU 프로파일

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

#### Memory 프로파일 (Heap)

Go의 분류로 4가지가 있고, 다른 언어도 유사함.

##### inuse_space

- **측정**: 현재 힙에 살아있는 객체의 바이트
- **사용처**: 메모리 사용량 분석, OOM 직전 상태 진단

##### inuse_objects

- **측정**: 현재 힙에 살아있는 객체의 개수
- **사용처**: 객체 누수, GC 압박 분석

##### alloc_space

- **측정**: 프로파일 기간 동안 누적 할당된 바이트
- **사용처**: 할당 핫스팟 (단명 객체가 많이 만들어지는 함수)

##### alloc_objects

- **측정**: 누적 할당된 객체 개수
- **사용처**: 작은 객체의 빈번한 할당으로 인한 GC 부담

#### Goroutines (Go)

- **측정**: 현재 실행 중인 고루틴 수와 각자의 스택
- **사용처**: 고루틴 누수 (`time.After` 의 잘못된 사용 등)
- **단위**: count

#### Mutex 프로파일

- **측정**: 락(mutex) 대기로 소모된 시간
- **사용처**: 컨텐션이 큰 락 발견, 멀티 코어 활용 저하 분석
- Go의 경우 `runtime.SetMutexProfileFraction` 으로 활성화 필요

#### Block 프로파일

- **측정**: 동기화 객체에서 블록(블로킹)된 시간 (channel, mutex 대기 등)
- **사용처**: 동기화 병목, I/O 대기 분석
- Go의 경우 `runtime.SetBlockProfileRate` 로 활성화 필요

#### Wall-clock 프로파일

- **측정**: 실시간(wall clock) 기준 시간 — CPU + I/O 대기 + sleep 모두 포함
- **사용처**: 응답 지연(latency) 분석. Java async-profiler의 `wall` 모드 등

#### 기타

- `process_cpu`: OS 관점 프로세스 CPU 사용(eBPF 등)
- `goroutine`: Go 활성 고루틴
- `thread_create`: 스레드 생성 위치(Go)
- `exception_samples`: 예외 발생 위치(.NET 등)
- `lock`: Java JVM 락 컨텐션
- `live`: 살아있는 객체(Java JFR 등)

---

### 샘플링 프로파일러 vs 계측 프로파일러

#### 샘플링 (Sampling)

- 일정 주기(예: 100Hz)마다 현재 스택을 캡처
- **오버헤드 매우 낮음** → 프로덕션 적합
- 통계적 정확도: 짧은 함수는 누락될 수 있음
- 예: Go runtime, async-profiler, perf, eBPF 프로파일러

#### 계측 (Instrumentation)

- 컴파일 타임 또는 런타임에 모든 함수 진입/종료에 hook 추가
- 정확한 호출 횟수와 시간
- **오버헤드 큼** → 프로덕션 비권장
- 예: 일부 APM 도구, valgrind callgrind

#### Pyroscope의 선택

Pyroscope는 샘플링 기반 프로파일러를 권장함. 모든 공식 SDK와 eBPF 통합은 샘플링 방식임. 이는 "항상 켜둘 수 있는" 연속 프로파일링 철학과 일치함.

---

### 언어별 지원 매트릭스

- Go: CPU 지원 · Heap 지원 · Goroutines 지원 · Mutex/Block 지원 · Wall 미지원 · 비고 표준 `runtime/pprof` 사용
- Java: CPU 지원 · Heap 지원(alloc/inuse) · Goroutines 미지원 · Mutex/Block 지원(lock) · Wall 지원 · 비고 async-profiler 기반
- Python: CPU 지원 · Heap 미지원 · Goroutines 미지원 · Mutex/Block 미지원 · Wall 지원 · 비고 py-spy / pyroscope SDK
- Ruby: CPU 지원 · Heap 미지원 · Goroutines 미지원 · Mutex/Block 미지원 · Wall 미지원 · 비고 rbspy 기반
- Node.js: CPU 지원 · Heap 지원 · Goroutines 미지원 · Mutex/Block 미지원 · Wall 지원 · 비고 V8 inspector
- .NET: CPU 지원 · Heap 지원 · Goroutines 미지원 · Mutex/Block 지원 · Wall 지원 · 비고 dotnet diagnostics
- Rust: CPU 지원 · Heap 미지원 · Goroutines 미지원 · Mutex/Block 미지원 · Wall 미지원 · 비고 pprof-rs
- eBPF: CPU 지원(process_cpu) · Heap 미지원 · Goroutines 미지원 · Mutex/Block 미지원 · Wall 미지원 · 비고 무계측, 커널 레벨

---

### 언제 어떤 프로파일을 보아야 하나

#### "응답 시간이 느려요"

1. **Wall-clock** (지원 언어) → 함수가 어디서 시간을 보내고 있는지
2. **Block** → I/O/락 대기 비중 확인
3. **CPU** → 실제 계산 비용 확인

#### "메모리가 계속 늘어나요 (누수 의심)"

1. **inuse_space** 시간 비교(diff) → 어느 함수의 객체가 줄지 않는가
2. **alloc_space** → 그 객체를 누가 할당하는가

#### "GC 시간이 길어요"

1. **alloc_objects** → 많은 작은 객체를 만드는 핫스팟
2. **alloc_space** → 큰 객체 핫스팟

#### "CPU 비용이 너무 비싸요 (클라우드 비용 절감)"

1. **CPU** → 가장 비싼 함수 식별
2. 다른 시간/배포와 **diff** 비교 → 회귀 발견

#### "데드락/지연 의심"

1. **Goroutines/Threads** → 비정상적으로 많은 스택인지
2. **Mutex** → 어떤 락이 컨텐션이 큰지

#### "응답에 가끔 큰 지연이 있어요 (tail latency)"

- 트레이스에서 느린 스팬 발견 → **Span Profiles** 로 해당 시간대 프로파일로 이동

---

### 다음 단계

- [05_flamegraphs.md](./2_profile_types_and_flamegraphs.md) - Flame Graph 읽고 분석하기
- [06_instrumentation.md](./3_instrumentation.md) - 언어별 계측 방법
- [08_use_cases.md](./4_manage_use_cases_configuration.md) - 실전 트러블슈팅 사례

---

## Flame Graph 분석

> 원본: https://grafana.com/docs/pyroscope/latest/view-and-analyze-profile-data/

---

### 목차

1. [Flame Graph란](#flame-graph란)
2. [Flame Graph 읽는 법](#flame-graph-읽는-법)
3. [표현 방향: Flame vs Icicle](#표현-방향-flame-vs-icicle)
4. [Diff 뷰 (비교)](#diff-뷰-비교)
5. [Sandwich 뷰](#sandwich-뷰)
6. [Table / Tree / Top 뷰](#table--tree--top-뷰)
7. [실전 분석 워크플로](#실전-분석-워크플로)

---

### Flame Graph란

Flame Graph는 Brendan Gregg이 2011년에 제안한 시각화 기법으로, 프로파일에 포함된 수많은 콜 스택의 집합을 한눈에 보여줌.

```
[━━━━━━━━━━━━━━━━━━ root ━━━━━━━━━━━━━━━━━━]
[━━━━━━ funcA ━━━━━━][━━━━━━ funcB ━━━━━━]
[━━ a1 ━━][━━ a2 ━━][━━ b1 ━━][━━ b2 ━━]
[a1.x][a1.y]      [b1.x]    [b2.x]
```

- **너비(가로)**: 해당 함수가 차지한 비율 (CPU 시간, 메모리 등 sample value의 합)
- **높이(세로)**: 콜 스택 깊이
- **색깔**: 정보 없음 (대비를 위한 시각적 구분만)

너비가 넓은 함수가 비싼 함수임.

---

### Flame Graph 읽는 법

#### 가장 먼저 보아야 할 곳

1. **루트 바로 아래 가장 넓은 박스**
   - 프로그램 전체 시간/메모리에서 가장 큰 비중을 차지하는 함수
2. **꼭대기의 평평한 영역**
   - 실제로 일이 일어나는 곳 (leaf function). 자식이 없는 박스
3. **수직으로 깊은 스택**
   - 깊다고 무조건 나쁜 것은 아니지만, 프레임워크 오버헤드가 큰 신호일 수 있음

#### Self vs Total

- **Total**: 자식까지 포함한 총 비용 (박스 너비)
- **Self**: 자신이 직접 사용한 비용 (자식이 차지하지 않은 부분)
- "Total은 크지만 Self는 작은" 함수 = 분배자(dispatcher), 진짜 비용은 자식
- "Self가 큰 함수" = 실제로 일하고 있는 함수

#### 줌(Zoom)

박스를 클릭하면 그 박스 기준으로 100%로 확대됨. 깊은 스택을 탐색할 때 유용함.

#### 검색(Search)

함수명/파일명으로 검색하면 매칭되는 박스가 강조됨. 특정 모듈의 비용 비중을 빠르게 파악 가능.

---

### 표현 방향: Flame vs Icicle

- Flame: 방향 위로 자라는 형태(root 위) · 특징 전통적인 표현, 잎(leaf)이 위
- Icicle: 방향 아래로 자라는 형태(root 아래) · 특징 가독성 향상, 깊은 스택에 유리

Pyroscope UI는 두 모드 모두 지원하며, Icicle이 기본인 경우가 많음.

---

### Diff 뷰 (비교)

두 프로파일을 색상으로 비교해 어디서 비용이 늘었고 줄었는지 보여줌.

#### 사용 시나리오

- **배포 전후 비교**: 어제(Baseline) vs 오늘(Comparison)
- **A/B 테스트**: 실험군 vs 대조군 라벨 비교
- **인스턴스 비교**: 빠른 노드 vs 느린 노드

#### 색깔 의미

- **빨강(Red)**: Comparison에서 **증가**한 부분 (회귀 후보)
- **초록(Green)**: Comparison에서 **감소** 한 부분 (개선)
- **회색**: 변화 없음

#### 권장 사용법

1. 베이스라인 시간/라벨 선택
2. 비교 대상 시간/라벨 선택
3. **빨강 박스**부터 살펴봄 → 회귀 위치 식별
4. 자세히 보고 싶으면 해당 박스로 줌

#### Pyroscope 쿼리 예

```
# 베이스라인
service_name="checkout", deployment="v1.0"

# 비교
service_name="checkout", deployment="v1.1"
```

---

### Sandwich 뷰

특정 함수에 집중해 호출자(callers)와 피호출자(callees)를 한 화면에 보여줌.

```
┌──────── 호출자(Callers) - Reverse ────────┐
│  who calls this function                   │
└────────────────────────────────────────────┘
              ▼
       [SELECTED FUNCTION]
              ▼
┌─────── 피호출자(Callees) - Forward ────────┐
│  what this function calls                  │
└────────────────────────────────────────────┘
```

#### 언제 유용한가

- "이 함수는 누가 그렇게 자주 호출하지?" — 호출자 추적
- "이 함수 안에서 뭐가 비싼 거지?" — 내부 비용 분석
- 라이브러리 함수의 진짜 비용 추적 (예: `json.Unmarshal` 의 호출 컨텍스트별 비중)

#### 사용법

1. Flame Graph 또는 Table에서 함수 우클릭 → "Sandwich view"
2. 위쪽: 그 함수에 도달하는 모든 경로
3. 아래쪽: 그 함수가 부르는 모든 경로

---

### Table / Tree / Top 뷰

#### Table 뷰

행은 함수, 열은 Self/Total로 구성된 정렬 가능한 목록.

- `regexp.compile`: Self 4.2s · Total 4.2s · Self % 22% · Total % 22%
- `json.Unmarshal`: Self 1.1s · Total 3.8s · Self % 6% · Total % 19%
- `runtime.mallocgc`: Self 2.3s · Total 2.3s · Self % 12% · Total % 12%

- **Self 정렬**: 진짜 비싼 leaf 함수 발견
- **Total 정렬**: 큰 그림(전체 비용 분배) 파악

#### Tree 뷰

콜 스택을 트리(외곽선) 형태로 표현. 각 노드 옆에 비중이 표시되어 깊이별 분포를 따라가기 좋음.

#### Top 뷰

가장 비싼 함수 상위 N개 리스트. 빠른 핫스팟 식별에 유용함.

---

### 실전 분석 워크플로

#### 1단계: 큰 그림

- Flame Graph 전체에서 가장 넓은 1~3개 박스를 식별
- "이게 정상인가? 충분히 최적화된 라이브러리인가?" 자문

#### 2단계: 의심 후보 검증

- 의심 함수에 줌 인
- Self/Total 비교 → 진짜 비용 위치 파악
- Sandwich 뷰로 호출 컨텍스트 확인

#### 3단계: 회귀라면 Diff

- 이전 시간/배포와 Diff
- 빨강 박스 → 회귀 책임 함수
- 코드 차이와 매칭

#### 4단계: 가설 검증

- 코드 수정 → 배포 → 동일 라벨로 다시 프로파일
- 다시 Diff: 빨강이 사라졌는지 확인

#### 흔한 함정

- **인라인된 함수**: 컴파일 최적화로 호출 스택에서 사라질 수 있음 → 부모 함수에 비용이 합쳐 보임
- **GC 비용**: 직접 할당이 적은 코드에서도 `runtime.mallocgc` 가 크게 나타날 수 있음
- **시스템 콜**: `syscall.read` 등 OS 호출은 wall-clock에서만 큰 비중을 차지하는 경우가 많음

---

### 다음 단계

- [04_profile_types.md](./2_profile_types_and_flamegraphs.md) - 프로파일 종류
- [08_use_cases.md](./4_manage_use_cases_configuration.md) - 실제 분석 사례
- [12_visualization.md](./5_cli_api_visualization.md) - Grafana Explore Profiles 통합

# 사용 사례 및 트러블슈팅

> 이 문서는 Pyroscope 의 실전 활용 사례와 분석 워크플로를 다룹니다.
> 원본: https://grafana.com/docs/pyroscope/latest/

---

## 목차

1. [성능 회귀 탐지](#1-성능-회귀-탐지)
2. [CPU 핫스팟 분석](#2-cpu-핫스팟-분석)
3. [메모리 누수 진단](#3-메모리-누수-진단)
4. [고루틴 누수 (Go)](#4-고루틴-누수-go)
5. [클라우드 비용 절감](#5-클라우드-비용-절감)
6. [Tail Latency 디버깅](#6-tail-latency-디버깅)
7. [락 컨텐션 분석](#7-락-컨텐션-분석)
8. [Continuous Profiling 도입 단계](#continuous-profiling-도입-단계)

---

## 1. 성능 회귀 탐지

### 시나리오

새로운 버전을 배포한 후 응답 시간이 미묘하게 늘었습니다. 메트릭 그래프로는 명확히 보이지만, 어느 코드 변경이 원인인지 모릅니다.

### 절차

1. **Diff 뷰 사용**
   - 베이스라인: `service_name="checkout", version="v1.2.0"`
   - 비교: `service_name="checkout", version="v1.2.1"`
2. **빨간 박스 분석**
   - 가장 큰 빨간 박스 = 가장 비싸진 함수
3. **코드 변경과 매칭**
   - Git diff로 v1.2.0 → v1.2.1 변경 사항 중 그 함수 영역 확인
4. **수정 후 재검증**
   - v1.2.2 배포 후 v1.2.0 vs v1.2.2 Diff → 회귀 사라졌는지 확인

### 자동화 아이디어

- CI에서 PR마다 staging 환경의 Pyroscope에 부하 테스트 프로파일을 보냄
- 메인 브랜치와 자동 Diff
- 일정 % 이상 회귀가 있으면 PR 코멘트로 알림

---

## 2. CPU 핫스팟 분석

### 시나리오

CPU 사용률이 항상 80%를 넘고 스케일 아웃 비용이 큽니다. 어디를 최적화해야 할지 모릅니다.

### 절차

1. **CPU 프로파일 수집**
   - `process_cpu` 또는 `cpu` 타입
2. **Table 뷰에서 Self 정렬**
   - 진짜 CPU를 직접 사용하는 leaf 함수 식별
3. **Top 5 후보 검토**
   - 정상인지(이미 최적화된 라이브러리), 개선 여지가 있는지 판단
4. **Sandwich 뷰로 호출 컨텍스트 확인**
   - 그 함수를 누가, 얼마나 자주 호출하는지

### 흔히 발견되는 패턴

- **반복적인 정규식 컴파일**: `regexp.MustCompile` 을 매 요청마다 호출
- **JSON 마샬링/언마샬링 비용**: 캐시 가능한 결과를 매번 직렬화
- **로깅 비용**: 디버그 로그가 production 에서 활성화
- **암호화 핫스팟**: TLS handshake가 connection pooling 부재로 매번 발생

---

## 3. 메모리 누수 진단

### 시나리오

서비스 메모리가 시간에 따라 계속 증가하고 결국 OOM Kill 됩니다.

### 절차

1. **inuse_space 시계열 보기**
   - 시간이 지나며 어떤 함수의 살아있는 객체가 늘어나는가
2. **Diff: 시작 시점 vs 현재**
   - 빨간 박스 = 메모리를 보유 중인 함수
3. **alloc_space 와 비교**
   - alloc만 크고 inuse가 평탄하면 → GC가 잘 동작 (누수 아님)
   - alloc과 inuse가 동시에 증가 → 누수
4. **코드 점검**
   - 캐시가 만료되지 않거나
   - 글로벌 슬라이스/맵에 항목이 무한 추가되거나
   - 고루틴/타이머가 살아있어 클로저 변수가 잡혀 있거나

### 사례

- `time.AfterFunc` 에 해제하지 않은 타이머
- HTTP 응답 본문을 close 하지 않아 connection이 살아있음
- `sync.Pool` 의 잘못된 사용으로 객체가 풀에 누적

---

## 4. 고루틴 누수 (Go)

### 시나리오

서비스의 고루틴 수가 시간이 지날수록 증가합니다. 결국 메모리/스케줄링 비용으로 OOM 또는 latency 폭증이 발생합니다.

### 절차

1. **`goroutines` 프로파일 수집**
2. **시계열 차트**: 시간에 따른 고루틴 수 추세 확인
3. **flame graph**에서 가장 많은 비중을 차지하는 콜 스택 식별
4. **흔한 원인** 점검:
   - `chan` 송신/수신이 영원히 차단됨 (상대방이 close 안 함)
   - HTTP 클라이언트의 timeout 미설정 → 응답 대기 무한
   - `select` 의 default 케이스 부재
   - `time.After` 가 GC되지 않는 패턴

---

## 5. 클라우드 비용 절감

### 시나리오

EC2/GCE 비용이 급증했고 일부 마이크로서비스의 CPU 사용량이 크게 늘었습니다. 가능한 코드 최적화로 노드 수를 줄이고 싶습니다.

### 절차

1. **CPU 비용이 큰 서비스 Top N 식별**
   - 메트릭(`container_cpu_usage_seconds_total`)으로 후보 선정
2. **각 서비스의 CPU 프로파일 분석**
   - leaf 함수 self 비중 → 최적화 후보
3. **개선 → 측정 → 반복**
   - 개선 후 같은 서비스의 CPU 프로파일을 Diff
   - "코드 최적화로 CPU 평균 -23%" 처럼 정량 결과 산출

### 사례 (실제 사례에서 자주 인용)

- 정규식 미리 컴파일로 30% 절감
- JSON 라이브러리 교체로 15% 절감
- HTTP keep-alive 활성화로 TLS handshake 비용 90% 감소

---

## 6. Tail Latency 디버깅

### 시나리오

P50은 정상인데 P99가 가끔 10배 튑니다. 트레이스에서는 `process` 스팬이 길게 잡힙니다.

### 절차

1. **트레이스에서 느린 요청 찾기**
   - Tempo에서 `duration > 1s` 검색
2. **해당 스팬 Span Profile 클릭**
   - 그 트레이스의 시간 윈도우 + 인스턴스 라벨로 Pyroscope 쿼리 자동 작성
3. **flame graph에서 비싼 박스 확인**
   - 일반적인 평균과 다른 패턴 찾기

### 흔한 원인

- GC 멈춤 → `runtime.gcBgMarkWorker` 비중 ↑
- 락 컨텐션 → `mutex` 프로파일에서 박스 큰 락
- 외부 API timeout 후 retry → I/O 대기 (wall clock)
- 콜드 캐시 → 첫 요청에서만 큰 비용

---

## 7. 락 컨텐션 분석

### 시나리오

CPU는 충분히 남는데 처리량이 늘지 않습니다. 멀티 코어가 활용되지 않는 의심.

### 절차

1. **Mutex 프로파일 활성화** (Go: `runtime.SetMutexProfileFraction(5)`)
2. **flame graph**에서 큰 박스 = 컨텐션 큰 락
3. **Sandwich** 로 누가 그 락을 가지고/기다리고 있는지 분석

### 개선 패턴

- **shard 락**: 하나의 큰 mutex → N개로 분할 (`sync.Map` 등)
- **read-write 분리**: `sync.RWMutex` 로 읽기 동시성 ↑
- **lock-free 자료구조**: atomic, channel
- **임계 영역 축소**: 락 내부에서 I/O를 하지 않기

---

## Continuous Profiling 도입 단계

조직에 Pyroscope를 도입할 때 권장 순서입니다.

### 단계 1: 핵심 서비스부터 (1~2주)

- 비용이 가장 큰 서비스 1~2개에 SDK 또는 Alloy 도입
- 단일 인스턴스 Pyroscope (Monolithic) 운영
- 팀에 flame graph 읽는 법 교육

### 단계 2: 표준화 (2~4주)

- 모든 서비스에 동일한 라벨 컨벤션 적용 (`service_name`, `env`, `cluster`)
- 자동 계측 → Alloy로 중앙 관리
- 첫 회귀 탐지 사례 만들기

### 단계 3: 통합 (1~2개월)

- Tempo, Loki, Mimir 와 라벨 통일 → 신호 간 점프
- Span Profiles 활성화
- 대시보드/Explore Profiles 정착

### 단계 4: 자동화 (지속)

- CI에서 PR별 자동 Diff
- 알람 규칙 (특정 함수가 임계 비중 넘으면)
- 정기적인 비용/성능 리뷰 미팅

---

## 다음 단계

- [05_flamegraphs.md](./05_flamegraphs.md) - 분석 도구 사용법
- [12_visualization.md](./12_visualization.md) - Grafana 통합
- [04_profile_types.md](./04_profile_types.md) - 어떤 프로파일을 쓸지

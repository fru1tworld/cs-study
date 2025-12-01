# perf c2c

> 캐시 라인 False Sharing 탐지 도구

## 개요

`perf c2c` (Cache-to-Cache)는 NUMA 시스템에서 캐시 라인 경합과 False Sharing을 탐지하는 도구입니다. Linux 4.10 이상에서 사용 가능하며, 멀티스레드 애플리케이션의 성능 문제를 진단하는 데 매우 유용합니다.

---

## False Sharing이란?

**False Sharing** 은 서로 다른 스레드가 동일한 캐시 라인에 있는 다른 변수를 수정할 때 발생합니다. 논리적으로는 공유하지 않지만, 캐시 라인 단위로 인해 불필요한 캐시 무효화가 발생합니다.

```
Cache Line (64 bytes)
┌────────────────────────────────────────────────────────────────┐
│ var_a (Thread 1 writes) │ var_b (Thread 2 writes)              │
└────────────────────────────────────────────────────────────────┘
       ↓                            ↓
   Thread 1이 var_a 수정      Thread 2가 var_b 수정
       ↓                            ↓
   전체 캐시 라인 무효화!    Thread 1의 캐시에서 reload 필요
```

---

## 기본 사용법

```bash
# 데이터 수집
perf c2c record -a sleep 5

# 또는 특정 프로그램
perf c2c record ./my_program

# 결과 분석
perf c2c report
```

---

## 주요 옵션

### record 옵션

| 옵션 | 설명 |
|------|------|
| `-a` | 시스템 전체 수집 |
| `-p <pid>` | 특정 프로세스 |
| `-F <freq>` | 샘플링 주파수 |
| `--ldlat <n>` | 로드 레이턴시 임계값 (사이클) |
| `-u, --all-user` | 유저 스페이스만 |
| `-k, --all-kernel` | 커널 스페이스만 |
| `--call-graph <mode>` | 콜 그래프 수집 |

### report 옵션

| 옵션 | 설명 |
|------|------|
| `-i <file>` | 입력 파일 |
| `-c <keys>` | 출력 컬럼 |
| `-g` | 콜 그래프 표시 |
| `--call-graph` | 콜 그래프 표시 |
| `-d <dir>` | 정렬 방향 (lcl, rmt) |
| `--stdio` | stdio 출력 |
| `-NN` | NUMA 노드 표시 |
| `--full-symbols` | 전체 심볼 표시 |

---

## 핵심 개념: HITM

**HITM** (Hit In Modified)은 False Sharing의 핵심 지표입니다.

| 유형 | 설명 | 심각도 |
|------|------|--------|
| **Remote HITM** | 다른 NUMA 노드의 수정된 캐시 라인 접근 | 매우 높음 |
| **Local HITM** | 같은 NUMA 노드의 수정된 캐시 라인 접근 | 높음 |

```
Remote HITM: CPU가 다른 NUMA 노드의 수정된 캐시 라인 요청
→ 가장 비용이 큰 메모리 접근 패턴
→ False Sharing의 강력한 지표
```

---

## 출력 해석

### 수집 및 리포트

```bash
# 수집
perf c2c record -a -u sleep 5

# 리포트 (인터랙티브)
perf c2c report -NN -c pid,iaddr

# stdio 출력
perf c2c report -NN -c pid,iaddr --stdio
```

### Trace Event Information 테이블

```
=================================================
            Trace Event Information
=================================================
  Total records                     :     123456
  Locked Load/Store Operations      :      12345
  Load Operations                   :      80000
  Loads - uncacheable               :          0
  Loads - IO                        :          0
  Loads - Miss                      :       5678
  Loads - no mapping                :          0
  Load Fill Buffer Hit              :      20000
  Load L1D hit                      :      40000
  Load L2D hit                      :      10000
  Load LLC hit                      :       3000
  Load Local HITM                   :        500   ← 주목!
  Load Remote HITM                  :        100   ← 핵심 지표!
  Load Remote HIT                   :         50
  Load Local DRAM                   :        300
  Load Remote DRAM                  :        200
```

### 핵심 지표

- **LLC Misses to Remote Cache (HITM)**: 이 값이 0에 가까우면 좋음
- 높은 값 = False Sharing 문제 가능성

### Shared Data Cache Line 테이블

```
=================================================
   Shared Data Cache Line Table
=================================================
#
# Total Shared Cache Lines          :         25
#       Remote HITM                 :        100
#       Local HITM                  :        500
#
# ----- HITM -----  -- Store Coverage --
#  RmtHITM  LclHITM   Load    Store    L1    L2
#      45       23    234      567   45.2%  5.1%  0x7f1234560000
#      30       15    156      345   30.1%  3.2%  0x7f1234560040
```

### Pareto 테이블 (상세)

```
=================================================
  Shared Cache Line Distribution Pareto
=================================================
#
#  ----- HITM -----  - Sampl -  -- Snoop --
#    Rmt    Lcl  Off    Pid    Tid   Cnt   Hit   CPU  Data Object   Symbol
#  ..... ...... ....  ......  ....  ....  ....  ....  ...........   ......
     45     23   0x0    1234  5678    89   HIT     0  my_struct     hot_func+0x20
     30     15   0x8    1234  5679    56   HITM    1  my_struct     other_func+0x15
```

---

## 실용적인 예시

### 기본 False Sharing 탐지

```bash
# 시스템 전체 수집 (5초)
sudo perf c2c record -a sleep 5

# 리포트
perf c2c report
```

### 유저 스페이스만 분석

```bash
# 유저 스페이스만 수집
perf c2c record -a --all-user sleep 5

# 또는 특정 프로그램
perf c2c record --all-user ./my_multithreaded_app
```

### 높은 레이턴시 로드만 캡처

```bash
# 50 사이클 이상 레이턴시
perf c2c record -a --ldlat 50 sleep 5
```

### 콜 그래프 포함

```bash
# DWARF 기반 콜 그래프
perf c2c record --call-graph dwarf,8192 -a --all-user sleep 5

# 리포트에서 콜 그래프 표시
perf c2c report -g --call-graph -c pid,iaddr --stdio
```

### 로컬 HITM 기준 정렬

```bash
# 로컬 HITM 기준
perf c2c report -d lcl
```

### NUMA 노드 표시

```bash
perf c2c report -NN
```

### 특정 캐시 라인 상세 분석

리포트에서 문제 캐시 라인 주소 확인 후:

```bash
# 특정 락/주소 필터
perf c2c report -L 0x7f1234560000
```

---

## 문제 식별 및 해결

### 문제 식별 체크리스트

1. **Remote HITM 확인**: Trace Event 테이블에서 "LLC Misses to Remote Cache (HITM)" 확인
2. ** 상위 캐시 라인 분석**: Shared Cache Line 테이블의 상위 1-3개 캐시 라인 집중
3. ** 오프셋 확인**: 같은 캐시 라인 내 다른 오프셋에서 접근하는지 확인
4. ** 평균 레이턴시**: 높은 평균 로드 레이턴시 = 심각한 경합

### 일반적인 해결 방법

#### 1. 패딩으로 캐시 라인 분리

```c
// Before: False Sharing 발생
struct data {
    int counter1;  // Thread 1
    int counter2;  // Thread 2
};

// After: 캐시 라인 분리
struct data {
    int counter1;  // Thread 1
    char padding[60];  // 캐시 라인 크기(64) - sizeof(int)
    int counter2;  // Thread 2
};
```

#### 2. `__attribute__((aligned))` 사용

```c
struct data {
    int counter1 __attribute__((aligned(64)));
    int counter2 __attribute__((aligned(64)));
};
```

#### 3. 자주 읽는 데이터와 자주 쓰는 데이터 분리

```c
// Read-mostly 데이터 그룹화
struct read_data {
    int config;
    int flags;
} __attribute__((aligned(64)));

// Write-frequently 데이터 그룹화
struct write_data {
    int counter;
    int timestamp;
} __attribute__((aligned(64)));
```

#### 4. Thread-Local Storage 사용

```c
// 스레드별 카운터
__thread int local_counter;

// 주기적으로 전역 카운터에 합산
```

---

## pahole로 구조체 분석

`perf c2c` 결과와 함께 `pahole` 도구를 사용하여 구조체 레이아웃을 분석할 수 있습니다.

```bash
# 구조체 레이아웃 확인
pahole -C my_struct ./my_program

# 캐시 라인 경계 표시
pahole --show_padding ./my_program
```

---

## 고급 사용

### 커널 샘플 레이트 조정

많은 CPU 시스템에서는 샘플 레이트 조정이 필요할 수 있습니다:

```bash
# 샘플 레이트 증가
echo 500 > /proc/sys/kernel/perf_cpu_time_max_percent
echo 100000 > /proc/sys/kernel/perf_event_max_sample_rate
```

### 상세 분석을 위한 perf script

```bash
# Raw 샘플 확인
perf script -i perf.data
```

---

## 해석 팁

### False Sharing이 심각한 경우

- Remote HITM 비율이 높음 (전체 로드의 1% 이상)
- 특정 캐시 라인에 HITM이 집중
- 평균 로드 레이턴시가 매우 높음 (수백 사이클)

### False Sharing이 경미한 경우

- Remote HITM이 거의 없음
- HITM이 여러 캐시 라인에 분산
- 레이턴시가 정상 범위

### 최적화 불필요 케이스

- 콜드 패스 (자주 실행되지 않는 코드)
- 이미 I/O 바운드인 워크로드
- 복잡도 대비 성능 향상이 미미한 경우

---

## 요구사항

### 하드웨어

- Intel (Nehalem 이상) 또는 AMD (Zen 이상) CPU
- PEBS/IBS 지원

### 커널

- Linux 4.10 이상
- CONFIG_PERF_EVENTS=y

### 권한

```bash
# root 또는 CAP_PERFMON 필요
sudo perf c2c record -a sleep 5

# 또는 paranoid 설정
sudo sysctl kernel.perf_event_paranoid=-1
```

---

## 참고 자료

- [perf c2c Blog (Joe Mario)](https://joemario.github.io/blog/2016/09/01/c2c-blog/)
- [Linux Kernel Documentation - False Sharing](https://docs.kernel.org/kernel-hacking/false-sharing.html)
- [Red Hat - Detecting false sharing](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/9/html/monitoring_and_managing_system_status_and_performance/detecting-false-sharing_monitoring-and-managing-system-status-and-performance)
- [LWN - perf c2c](https://lwn.net/Articles/688262/)

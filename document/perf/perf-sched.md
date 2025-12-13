# perf sched

> CPU 스케줄러 분석 도구

## 개요

`perf sched`는 Linux CPU 스케줄러의 동작을 추적하고 분석합니다. 컨텍스트 스위치, 스케줄링 레이턴시, 런타임 분포 등을 상세히 분석할 수 있습니다.

---

## 기본 사용법

```bash
# 스케줄러 이벤트 기록
sudo perf sched record ./my_program

# 또는 시스템 전체 (일정 시간)
sudo perf sched record sleep 5

# 레이턴시 요약
perf sched latency

# 컨텍스트 스위치 맵
perf sched map

# 상세 트레이스
perf sched script
```

---

## 서브커맨드

### record

스케줄러 이벤트를 기록합니다.

```bash
sudo perf sched record ./my_program
sudo perf sched record -a sleep 10
```

### latency

태스크별 스케줄링 레이턴시 통계를 표시합니다.

```bash
perf sched latency
```

### map

CPU별 컨텍스트 스위치를 시각적으로 표시합니다.

```bash
perf sched map
```

### script

Raw 스케줄러 이벤트를 출력합니다.

```bash
perf sched script
```

### replay

기록된 워크로드를 재현합니다.

```bash
perf sched replay
```

### timehist

이벤트별 스케줄러 레이턴시를 표시합니다 (Linux 4.10+).

```bash
perf sched timehist
```

---

## 주요 옵션

### 공통 옵션

| 옵션 | 설명 |
|------|------|
| `-i <file>` | 입력 파일 (기본: perf.data) |
| `-v` | 상세 출력 |
| `-D` | Raw 트레이스 덤프 |
| `-f` | 강제 실행 |

### latency 옵션

| 옵션 | 설명 |
|------|------|
| `-C <cpu>` | 특정 CPU |
| `-p` | PID별 통계 |
| `-s <key>` | 정렬 키 (runtime, switch, avg, max) |

### map 옵션

| 옵션 | 설명 |
|------|------|
| `--compact` | 활성 CPU만 표시 |
| `--cpus <list>` | 특정 CPU만 표시 |
| `--color-cpus <list>` | CPU 하이라이트 |
| `--color-pids <list>` | PID 하이라이트 |
| `--task-name <name>` | 특정 태스크 필터 |

### timehist 옵션

| 옵션 | 설명 |
|------|------|
| `-k <vmlinux>` | 커널 심볼 파일 |
| `-g` | 콜 그래프 표시 |
| `--max-stack <n>` | 최대 스택 깊이 |
| `-C <cpu>` | 특정 CPU 필터 |
| `-p <pid>` | 특정 PID 필터 |
| `-s` | 요약만 표시 |
| `-V` | CPU 시각화 |
| `-w` | 웨이크업 이벤트 표시 |
| `-M` | 마이그레이션 이벤트 표시 |
| `-I` | 유휴 이벤트만 표시 |
| `--state` | 태스크 상태 표시 |
| `--show-prio` | 태스크 우선순위 표시 |

---

## perf sched latency

### 기본 출력

```bash
perf sched latency
```

출력:
```
 Task                  |   Runtime ms  | Switches | Avg delay ms | Max delay ms |
 ----------------------|---------------|----------|--------------|--------------|
 my_program:1234       |     234.567   |     456  |   0.123      |    15.678    |
 worker:5678           |     123.456   |     234  |   0.089      |     8.901    |
 [kernel]:0            |      56.789   |     123  |   0.045      |     3.456    |
```

### 컬럼 설명

| 컬럼 | 설명 |
|------|------|
| **Task** | 태스크 이름:PID |
| **Runtime ms** | 총 CPU 실행 시간 |
| **Switches** | 컨텍스트 스위치 횟수 |
| **Avg delay ms** | 평균 스케줄링 지연 |
| **Max delay ms** | 최대 스케줄링 지연 |

### 정렬 옵션

```bash
# 런타임 기준 정렬
perf sched latency -s runtime

# 스위치 횟수 기준
perf sched latency -s switch

# 평균 지연 기준
perf sched latency -s avg

# 최대 지연 기준
perf sched latency -s max
```

---

## perf sched map

### 기본 출력

```bash
perf sched map
```

출력:
```
  *A0           995962.884726 secs A0 => migration/0:12
   A0  *B0      995962.884730 secs B0 => worker:1234
   A0   B0  *C0 995962.884735 secs C0 => my_program:5678
   A0   B0   C0  *.            995962.884740 secs
  *D0   B0   C0   .            995962.884745 secs D0 => kworker:15
```

### 표시 설명

| 표시 | 의미 |
|------|------|
| `*` | 이벤트 발생 CPU |
| `.` | 유휴 상태 |
| `A0`, `B0` 등 | 태스크 식별자 |

### 옵션 사용

```bash
# 컴팩트 모드
perf sched map --compact

# 특정 CPU만
perf sched map --cpus 0,1,2,3

# 특정 태스크 하이라이트
perf sched map --color-pids 1234
```

---

## perf sched timehist

Linux 4.10 이상에서 사용 가능한 상세 분석 도구입니다.

### 기본 출력

```bash
perf sched timehist
```

출력:
```
     time    cpu  task name           wait time   sch delay   run time
                                       (msec)      (msec)      (msec)
  --------   ---  ----------------   ---------   ---------   ---------
  1000.123     0  my_program:1234      0.123       0.015       5.678
  1000.129     1  worker:5678          0.089       0.012       3.456
  1000.135     0  [idle]               0.000       0.000      15.234
```

### 컬럼 설명

| 컬럼 | 설명 |
|------|------|
| **time** | 타임스탬프 |
| **cpu** | CPU 번호 |
| **task name** | 태스크 이름:PID |
| **wait time** | 웨이크업 대기 시간 |
| **sch delay** | 스케줄러 지연 (런큐 대기) |
| **run time** | CPU 실행 시간 |

### 주요 옵션 사용

```bash
# CPU 시각화
perf sched timehist -V

# 웨이크업 이벤트 포함
perf sched timehist -w

# 마이그레이션 이벤트 포함
perf sched timehist -M

# 요약만 표시
perf sched timehist -s

# 태스크 상태 표시
perf sched timehist --state

# 우선순위 표시
perf sched timehist --show-prio
```

### CPU 시각화 출력 (-V)

```
     time    cpu  012345678901234567890123  task name  ...
  --------   ---  ------------------------  ---------
  1000.123     0  s]                        my_program:1234
  1000.129     1   [s                       worker:5678
```

- `s`: 스케줄러 이벤트
- `i`: 유휴
- `[`: 런 시작
- `]`: 런 종료

---

## perf sched script

Raw 스케줄러 이벤트를 출력합니다.

```bash
perf sched script
```

출력:
```
  migration/0    12 [000]   995.123456: sched:sched_switch: prev_comm=migration/0 prev_pid=12 prev_prio=0 prev_state=S ==> next_comm=my_program next_pid=1234 next_prio=120
  my_program  1234 [000]   995.128456: sched:sched_stat_runtime: comm=my_program pid=1234 runtime=5000000 [ns] vruntime=12345678 [ns]
```

---

## perf sched replay

기록된 워크로드를 시뮬레이션합니다.

```bash
# 기록
sudo perf sched record ./my_program

# 재현 (기본 10회)
perf sched replay

# 반복 횟수 지정
perf sched replay -r 5

# 무한 반복
perf sched replay -r 0
```

---

## 실용적인 예시

### 스케줄링 레이턴시 분석

```bash
# 데이터 수집
sudo perf sched record -a sleep 10

# 레이턴시 확인
perf sched latency

# 최대 레이턴시 순으로 정렬
perf sched latency -s max
```

### 특정 프로세스 분석

```bash
# 특정 프로세스 기록
sudo perf sched record -p $(pgrep my_app) sleep 30

# 분석
perf sched latency
perf sched timehist -p $(pgrep my_app)
```

### 컨텍스트 스위치 패턴 분석

```bash
# 기록
sudo perf sched record -a sleep 5

# 시각적 맵
perf sched map --compact
```

### CPU 마이그레이션 분석

```bash
# timehist로 마이그레이션 확인
perf sched timehist -M
```

### 실시간 태스크 분석

```bash
# 우선순위 포함 분석
perf sched timehist --show-prio

# 상태 포함
perf sched timehist --state
```

### 유휴 시간 분석

```bash
# 유휴 관련 이벤트만
perf sched timehist -I
```

---

## 추적되는 이벤트

`perf sched record`는 다음 트레이스포인트를 캡처합니다:

- `sched:sched_switch` - 컨텍스트 스위치
- `sched:sched_stat_wait` - 대기 통계
- `sched:sched_stat_sleep` - 슬립 통계
- `sched:sched_stat_iowait` - I/O 대기 통계
- `sched:sched_stat_runtime` - 런타임 통계
- `sched:sched_process_fork` - 포크 이벤트
- `sched:sched_wakeup` - 웨이크업 이벤트
- `sched:sched_wakeup_new` - 새 태스크 웨이크업
- `sched:sched_migrate_task` - 태스크 마이그레이션

---

## 오버헤드 주의

`perf sched`는 스케줄러 이벤트를 모두 캡처하므로 오버헤드가 클 수 있습니다:

- 이벤트가 초당 수백만 개 발생 가능
- CPU, 메모리, 디스크 오버헤드 발생
- 짧은 시간으로 시작하여 점진적 확장 권장

```bash
# 짧은 시간으로 시작
sudo perf sched record sleep 1

# 데이터 크기 확인
ls -lh perf.data
```

---

## 트러블슈팅

### 권한 오류

```bash
# root 권한 필요
sudo perf sched record ./my_program
```

### 데이터 파일이 너무 큼

```bash
# 짧은 시간 수집
sudo perf sched record sleep 1

# 또는 특정 프로세스만
sudo perf sched record -p <pid> sleep 5
```

### "No samples" 오류

```bash
# 트레이스포인트 확인
perf list | grep sched

# debugfs 마운트 확인
mount | grep debugfs
```

---

## 참고 자료

- [perf-sched(1) man page](https://man7.org/linux/man-pages/man1/perf-sched.1.html)
- [Brendan Gregg's perf sched](https://www.brendangregg.com/blog/2017-03-16/perf-sched.html)
- [Linux kernel source: tools/perf/Documentation/perf-sched.txt](https://github.com/torvalds/linux/blob/master/tools/perf/Documentation/perf-sched.txt)

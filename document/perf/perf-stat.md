# perf stat

> 성능 카운터 통계 수집 도구

## 개요

`perf stat`은 프로그램 실행 중 성능 카운터 통계를 수집하는 명령어입니다. CPU 사이클, 명령어 수, 캐시 미스 등 하드웨어 및 소프트웨어 이벤트를 측정합니다.

---

## 기본 사용법

```bash
# 프로그램 실행 통계
perf stat ./my_program

# 시스템 전체 통계 (5초)
perf stat -a sleep 5

# 실행 중인 프로세스 측정
perf stat -p <pid> sleep 10

# 특정 이벤트만 측정
perf stat -e cycles,instructions ./my_program
```

---

## 주요 옵션

### 대상 지정

| 옵션 | 설명 |
|------|------|
| `-p <pid>` | 특정 프로세스 ID 측정 |
| `-t <tid>` | 특정 스레드 ID 측정 |
| `-a` | 시스템 전체 (모든 CPU) 측정 |
| `-C <cpu>` | 특정 CPU만 측정 (예: `-C 0,1,2`) |
| `-G <cgroup>` | 특정 cgroup 측정 |

### 이벤트 지정

| 옵션 | 설명 |
|------|------|
| `-e <events>` | 측정할 이벤트 지정 (쉼표로 구분) |
| `-d` | 상세 모드 (더 많은 캐시 이벤트 포함) |
| `-dd` | 매우 상세 모드 |
| `-ddd` | 최대 상세 모드 |

### 출력 제어

| 옵션 | 설명 |
|------|------|
| `-o <file>` | 결과를 파일로 저장 |
| `-x <sep>` | 필드 구분자 지정 (CSV 출력용) |
| `-n` | null run (카운터만 활성화, 측정 안 함) |
| `-v` | 상세 출력 |
| `-B` | 큰 숫자에 천단위 구분자 표시 |

### 반복 및 시간

| 옵션 | 설명 |
|------|------|
| `-r <n>` | n회 반복 실행 후 평균 계산 |
| `-I <ms>` | 주기적으로 통계 출력 (밀리초) |
| `--sync` | 실행 전 sync() 호출 |
| `--timeout <ms>` | 타임아웃 설정 |

---

## 이벤트 수정자 (Modifiers)

이벤트 뒤에 콜론과 수정자를 붙여 필터링할 수 있습니다.

| 수정자 | 설명 |
|--------|------|
| `u` | 유저 스페이스만 측정 |
| `k` | 커널 스페이스만 측정 |
| `h` | 하이퍼바이저 이벤트 |
| `H` | 호스트 머신 (가상화) |
| `G` | 게스트 머신 (가상화) |
| `p` | 정밀 이벤트 (PEBS) |

```bash
# 유저 스페이스 사이클만 측정
perf stat -e cycles:u ./my_program

# 커널 스페이스만 측정
perf stat -e cycles:k ./my_program

# 유저와 커널 분리 측정
perf stat -e cycles:u,cycles:k ./my_program
```

---

## 출력 해석

```
 Performance counter stats for './my_program':

          1,234.56 msec task-clock                #    0.985 CPUs utilized
               123      context-switches          #   99.612 /sec
                 5      cpu-migrations            #    4.050 /sec
            12,345      page-faults               #   10.000 K/sec
     3,456,789,012      cycles                    #    2.800 GHz
     2,345,678,901      instructions              #    0.68  insn per cycle
       234,567,890      cache-references          #  190.000 M/sec
        23,456,789      cache-misses              #   10.00% of all cache refs

       1.253456789 seconds time elapsed
       1.200000000 seconds user
       0.050000000 seconds sys
```

### 주요 지표

| 지표 | 설명 | 이상적인 값 |
|------|------|-------------|
| **IPC** (Instructions Per Cycle) | 사이클당 명령어 수 | > 1.0 |
| **Cache Miss Rate** | 캐시 미스 비율 | < 5% |
| **Branch Miss Rate** | 분기 예측 실패율 | < 2% |
| **CPUs utilized** | CPU 활용도 | 병렬화에 따라 다름 |

---

## 멀티플렉싱과 스케일링

PMU(Performance Monitoring Unit)의 카운터 수는 제한되어 있습니다 (일반적으로 4-8개). 측정할 이벤트가 카운터 수보다 많으면 ** 시분할 멀티플렉싱** 이 발생합니다.

```
final_count = raw_count × (time_enabled / time_running)
```

- `time_enabled`: 이벤트가 활성화된 시간
- `time_running`: 실제로 카운터가 실행된 시간

**100% 미만 표시**: 멀티플렉싱이 발생했음을 의미

---

## 실용적인 예시

### CPU 성능 분석

```bash
# 기본 CPU 통계
perf stat -e cycles,instructions,cache-misses,branch-misses ./my_program

# IPC와 캐시 효율 분석
perf stat -e cycles,instructions,\
L1-dcache-loads,L1-dcache-load-misses,\
LLC-loads,LLC-load-misses ./my_program
```

### 메모리 성능 분석

```bash
# 캐시 계층별 분석
perf stat -e L1-dcache-loads,L1-dcache-load-misses,\
L1-icache-load-misses,\
LLC-loads,LLC-load-misses ./my_program

# 메모리 대역폭 분석
perf stat -e cache-references,cache-misses,\
mem-loads,mem-stores ./my_program
```

### 분기 예측 분석

```bash
perf stat -e branch-instructions,branch-misses,\
branch-loads,branch-load-misses ./my_program
```

### 컨텍스트 스위치 분석

```bash
perf stat -e context-switches,cpu-migrations,page-faults ./my_program
```

### 시스템 콜 분석

```bash
# 시스템 콜 카운트
perf stat -e 'syscalls:sys_enter_*' -a sleep 5

# 초당 시스템 콜 수
perf stat -e raw_syscalls:sys_enter -I 1000 -a
```

### CPU별 통계

```bash
# CPU별 분리 측정
perf stat -a -A sleep 5

# 특정 CPU만 측정
perf stat -C 0,1 -a sleep 5
```

### 반복 측정으로 신뢰도 확보

```bash
# 10회 반복 후 평균과 표준편차 출력
perf stat -r 10 ./my_program
```

### 주기적 출력

```bash
# 1초마다 통계 출력
perf stat -I 1000 -a

# 500ms마다 출력
perf stat -I 500 -e cycles,instructions -a
```

---

## Raw PMC 이벤트

프로세서별 특정 이벤트는 raw 형식으로 지정할 수 있습니다.

```bash
# Intel raw event 형식: rXXYY (XX=umask, YY=event)
perf stat -e r003c -a sleep 5

# 상세 지정 형식
perf stat -e cpu/event=0x0e,umask=0x01,inv,cmask=0x01/ -a sleep 5
```

Intel SDM 또는 AMD PPR 문서에서 이벤트 코드를 확인할 수 있습니다.

---

## 트러블슈팅

### 권한 오류

```bash
# 커널 이벤트 접근 권한 설정
sudo sysctl kernel.perf_event_paranoid=-1

# 또는 root로 실행
sudo perf stat -a sleep 5
```

### 이벤트 사용 불가

```bash
# 사용 가능한 이벤트 확인
perf list

# 가상화 환경에서는 일부 하드웨어 이벤트 제한됨
# 소프트웨어 이벤트 사용
perf stat -e cpu-clock,task-clock ./my_program
```

### 멀티플렉싱 최소화

```bash
# 한 번에 측정하는 이벤트 수 줄이기
perf stat -e cycles ./my_program
perf stat -e instructions ./my_program
perf stat -e cache-misses ./my_program
```

---

## 참고 자료

- [perf-stat(1) man page](https://man7.org/linux/man-pages/man1/perf-stat.1.html)
- [perf Wiki - Tutorial](https://perfwiki.github.io/main/tutorial/)
- [Brendan Gregg's perf Examples](https://www.brendangregg.com/perf.html)

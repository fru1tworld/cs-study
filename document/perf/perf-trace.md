# perf trace

> 시스템 콜 추적 도구 (strace 대안)

## 개요

`perf trace`는 `strace`와 유사하게 시스템 콜을 추적하지만, perf_events 인프라를 사용하여 훨씬 낮은 오버헤드로 동작합니다. 시스템 콜뿐만 아니라 페이지 폴트, 스케줄러 이벤트 등도 추적할 수 있습니다.

---

## 기본 사용법

```bash
# 명령어의 시스템 콜 추적
perf trace ls

# 실행 중인 프로세스 추적
sudo perf trace -p <pid>

# 시스템 전체 추적
sudo perf trace -a

# 요약 통계만 출력
perf trace -s ls
```

---

## 주요 옵션

### 대상 지정

| 옵션 | 설명 |
|------|------|
| `-p <pid>` | 특정 프로세스 추적 |
| `-t <tid>` | 특정 스레드 추적 |
| `-u <user>` | 특정 사용자 프로세스 추적 |
| `-a` | 시스템 전체 추적 |
| `-C <cpu>` | 특정 CPU만 추적 |

### 이벤트 필터링

| 옵션 | 설명 |
|------|------|
| `-e <events>` | 특정 시스템 콜/이벤트만 추적 |
| `--syscalls` | 시스템 콜 추적 (기본값) |
| `--no-syscalls` | 시스템 콜 추적 비활성화 |
| `-F <faults>` | 페이지 폴트 추적 (all, min, maj) |
| `--filter-pids <pids>` | 특정 PID 제외 |

### 출력 제어

| 옵션 | 설명 |
|------|------|
| `-o <file>` | 출력 파일 지정 |
| `-s, --summary` | 요약 통계만 출력 |
| `-S, --with-summary` | 상세 출력 + 요약 |
| `-T, --time` | 전체 타임스탬프 표시 |
| `-v, --verbose` | 상세 출력 |
| `--duration <ms>` | 지정 시간 이상 소요된 것만 표시 |
| `--failure` | 실패한 시스템 콜만 표시 |

### 콜 그래프

| 옵션 | 설명 |
|------|------|
| `--call-graph <mode>` | 콜 그래프 수집 |
| `--max-stack <n>` | 최대 스택 깊이 |
| `--kernel-syscall-graph` | 커널 콜체인 표시 |

### 고급 옵션

| 옵션 | 설명 |
|------|------|
| `--max-events <n>` | 최대 이벤트 수 |
| `--sched` | 런타임 요약 표시 |
| `--bpf-summary` | BPF로 통계 수집 |

---

## 출력 형식

### 기본 출력

```
     0.000 ( 0.012 ms): ls/1234 open(filename: "/etc/passwd", flags: RDONLY) = 3
     0.015 ( 0.005 ms): ls/1234 fstat(fd: 3, statbuf: 0x7ffd...) = 0
     0.021 ( 0.008 ms): ls/1234 read(fd: 3, buf: 0x55ab..., count: 4096) = 1234
     0.032 ( 0.003 ms): ls/1234 close(fd: 3) = 0
```

### 필드 설명

```
타임스탬프 (소요시간): 명령어/PID 시스템콜(인자들) = 반환값
```

### 요약 출력 (-s)

```
 syscall            calls    total       min       avg       max      stddev
                               (msec)    (msec)    (msec)    (msec)        (%)
 --------------- -------- --------- --------- --------- ---------     ------
 read                 156    45.234     0.001     0.290    12.345     45.67%
 write                 89    23.456     0.002     0.264     5.678     23.45%
 open                  45    12.345     0.010     0.274     1.234     12.34%
```

---

## 실용적인 예시

### 특정 시스템 콜만 추적

```bash
# read, write만 추적
perf trace -e read,write ls

# open 관련 시스템 콜
perf trace -e 'open*' ls

# 네트워크 관련
perf trace -e 'connect,accept*,send*,recv*' ./my_server

# 파일 시스템 관련
perf trace -e 'open*,close,read,write,stat*' ls
```

### 글로브 패턴 사용

```bash
# msg로 시작하는 시스템 콜
perf trace -e 'msg*' ./my_program

# epoll 관련
perf trace -e 'epoll_*' ./my_server
```

### 페이지 폴트 추적

```bash
# 모든 페이지 폴트
perf trace -F all ./my_program

# 메이저 폴트만 (디스크 I/O 발생)
perf trace -F maj ./my_program

# 마이너 폴트만
perf trace -F min ./my_program

# 시스템 콜 없이 페이지 폴트만
perf trace --no-syscalls -F all ./my_program
```

### 느린 시스템 콜 찾기

```bash
# 10ms 이상 소요된 시스템 콜
perf trace --duration 10 ./my_program

# 1ms 이상
perf trace --duration 1 -a
```

### 실패한 시스템 콜 찾기

```bash
# 실패한 것만 표시
perf trace --failure ./my_program

# ENOENT 등 에러 확인
perf trace --failure ls /nonexistent
```

### 프로세스 추적

```bash
# 실행 중인 프로세스
sudo perf trace -p $(pgrep nginx)

# 여러 프로세스
sudo perf trace -p 1234,5678

# 새 프로세스 포함
sudo perf trace -p $(pgrep nginx) --children
```

### 시스템 전체 추적

```bash
# 시스템 전체 (짧은 시간)
sudo perf trace -a sleep 5

# 특정 CPU만
sudo perf trace -C 0,1 -a sleep 5
```

### 통계 수집

```bash
# 요약만
perf trace -s ./my_program

# 상세 + 요약
perf trace -S ./my_program

# BPF 기반 요약 (더 효율적)
sudo perf trace --bpf-summary -a sleep 10
```

### 콜 그래프 포함

```bash
# 콜 그래프와 함께
perf trace --call-graph dwarf ls

# 특정 페이지 폴트의 콜 스택
perf trace -F min --max-stack 7 --max-events 1 ./my_program
```

---

## 파일 기반 분석

### 이벤트 기록

```bash
# 기록
perf trace record ./my_program

# 또는 perf record 사용
perf record -e raw_syscalls:* ./my_program
```

### 기록된 데이터 분석

```bash
# 분석
perf trace -i perf.data

# 요약
perf trace -i perf.data -s
```

---

## strace와 비교

| 특성 | perf trace | strace |
|------|-----------|--------|
| 오버헤드 | 낮음 (~1.4x) | 높음 (~10x+) |
| 커널 통합 | O | X |
| 페이지 폴트 추적 | O | 제한적 |
| 스케줄러 이벤트 | O | X |
| 통계 요약 | O | O |
| 출력 상세도 | 보통 | 매우 상세 |
| 루트 권한 | 일부 필요 | 불필요 |

### 사용 선택

- **perf trace**: 프로덕션 환경, 낮은 오버헤드 필요
- **strace**: 개발 환경, 상세한 인자 분석 필요

---

## 고급 사용

### 특정 시간 범위 분석

```bash
# 처음 4개 이벤트만
perf trace --max-events 4 ./my_program
```

### 조건부 추적

```bash
# 특정 이벤트 발생 시 시작
perf trace --switch-on <event> ./my_program

# 특정 이벤트 발생 시 중지
perf trace --switch-off <event> ./my_program
```

### 이벤트 조합

```bash
# 시스템 콜 + 스케줄러 이벤트
perf trace -e 'sched:*,syscalls:*' -a sleep 5
```

### 출력 리다이렉션

```bash
# 파일로 저장
perf trace -o trace.log ./my_program

# 파이프 처리
perf trace ls 2>&1 | grep open
```

---

## 페이지 폴트 출력 형식

```
 majfault [do_page_fault+0x1a5] => 0x7f1234567890 (d.)
```

### 필드 설명

| 필드 | 설명 |
|------|------|
| `majfault/minfault` | 폴트 유형 |
| `[symbol+offset]` | 폴트 발생 위치 |
| `address` | 폴트 주소 |
| `(d.)` | 맵 타입 (`d`=데이터, `x`=실행), 레벨 (`.`=유저, `k`=커널) |

---

## 트러블슈팅

### 권한 오류

```bash
# perf_event_paranoid 설정
sudo sysctl kernel.perf_event_paranoid=-1

# 또는 sudo 사용
sudo perf trace ./my_program
```

### 이벤트 접근 불가

```bash
# 사용 가능한 이벤트 확인
perf list | grep syscall

# debugfs 마운트 확인
mount | grep debugfs
```

### 출력이 너무 많음

```bash
# 필터링
perf trace -e 'read,write' ./my_program

# 시간 제한
perf trace --duration 10 ./my_program

# 이벤트 수 제한
perf trace --max-events 1000 ./my_program
```

---

## 참고 자료

- [perf-trace(1) man page](https://man7.org/linux/man-pages/man1/perf-trace.1.html)
- [Brendan Gregg's perf Examples](https://www.brendangregg.com/perf.html)
- [Linux kernel source: tools/perf/Documentation/perf-trace.txt](https://github.com/torvalds/linux/blob/master/tools/perf/Documentation/perf-trace.txt)

# perf record

> 성능 프로파일 데이터 수집 도구

## 개요

`perf record`는 프로그램 실행 중 성능 샘플을 수집하여 `perf.data` 파일에 저장합니다. 이후 `perf report`, `perf script` 등으로 분석할 수 있습니다.

---

## 기본 사용법

```bash
# 기본 프로파일링 (cycles 이벤트, 1000Hz 샘플링)
perf record ./my_program

# 콜 그래프 포함
perf record -g ./my_program

# 시스템 전체 프로파일링
sudo perf record -a -g sleep 10

# 특정 프로세스 프로파일링
perf record -p <pid> -g sleep 10

# 출력 파일 지정
perf record -o profile.data ./my_program
```

---

## 주요 옵션

### 대상 지정

| 옵션 | 설명 |
|------|------|
| `-p <pid>` | 특정 프로세스 프로파일링 |
| `-t <tid>` | 특정 스레드 프로파일링 |
| `-a` | 시스템 전체 (all CPUs) |
| `-C <cpu>` | 특정 CPU만 프로파일링 |
| `-u <user>` | 특정 사용자 프로세스만 |
| `--cgroup <name>` | 특정 cgroup (컨테이너) |

### 이벤트 및 샘플링

| 옵션 | 설명 |
|------|------|
| `-e <event>` | 프로파일링할 이벤트 지정 |
| `-F <freq>` | 샘플링 주파수 (Hz) |
| `-c <count>` | n번의 이벤트마다 샘플링 |
| `--freq` | `-F`와 동일 |

### 콜 그래프 (스택 트레이스)

| 옵션 | 설명 |
|------|------|
| `-g` | 콜 그래프 수집 (frame pointer 방식) |
| `--call-graph <mode>` | 콜 그래프 수집 방식 지정 |

### 출력 제어

| 옵션 | 설명 |
|------|------|
| `-o <file>` | 출력 파일 지정 (기본: perf.data) |
| `-q` | 조용한 모드 |
| `-v` | 상세 출력 |

---

## 샘플링 모드

### 주파수 기반 샘플링 (-F)

```bash
# 99Hz로 샘플링 (1초에 99개 샘플)
perf record -F 99 ./my_program

# 높은 정밀도 프로파일링
perf record -F 999 ./my_program

# 낮은 오버헤드 프로파일링
perf record -F 49 ./my_program
```

** 권장 주파수**:
- 일반 프로파일링: 99Hz
- 상세 분석: 999Hz
- 낮은 오버헤드: 49Hz

### 카운트 기반 샘플링 (-c)

```bash
# 10,000 사이클마다 샘플링
perf record -c 10000 -e cycles ./my_program

# 캐시 미스 100번마다 샘플링
perf record -c 100 -e cache-misses ./my_program
```

---

## 콜 그래프 수집 방식

### Frame Pointer (기본)

```bash
perf record -g ./my_program
# 또는
perf record --call-graph fp ./my_program
```

- ** 장점**: 가장 빠름, 낮은 오버헤드
- ** 단점**: `-fno-omit-frame-pointer`로 컴파일 필요

### DWARF

```bash
perf record --call-graph dwarf ./my_program

# 스택 크기 지정 (기본 8192)
perf record --call-graph dwarf,16384 ./my_program
```

- ** 장점**: 최적화된 바이너리에서도 동작
- ** 단점**: 더 큰 데이터 파일, 약간의 오버헤드

### LBR (Last Branch Record)

```bash
perf record --call-graph lbr ./my_program
```

- ** 장점**: 하드웨어 기반, 매우 낮은 오버헤드
- ** 단점**: Intel CPU 전용, 제한된 스택 깊이 (8-32 프레임)

---

## 이벤트 기반 프로파일링

### CPU 프로파일링

```bash
# CPU 사이클 (기본)
perf record -e cycles ./my_program

# 커널만
perf record -e cycles:k ./my_program

# 유저 스페이스만
perf record -e cycles:u ./my_program

# PEBS 정밀 이벤트
perf record -e cycles:up ./my_program
```

### 캐시 프로파일링

```bash
# L1 데이터 캐시 미스
perf record -e L1-dcache-load-misses -c 10000 -ag sleep 5

# LLC (Last Level Cache) 미스
perf record -e LLC-load-misses -c 100 -ag sleep 5
```

### 브랜치 프로파일링

```bash
# 분기 예측 실패
perf record -e branch-misses -c 1000 ./my_program

# 분기 추적
perf record -b -a sleep 1
```

---

## 트레이스포인트 프로파일링

### 블록 I/O

```bash
# 디스크 요청 추적
perf record -e block:block_rq_issue -ag sleep 10

# 디스크 I/O 완료
perf record -e block:block_rq_complete -ag sleep 10
```

### 스케줄러

```bash
# 컨텍스트 스위치
perf record -e context-switches -ag sleep 10

# 스케줄러 상세
perf record -e 'sched:*' -a sleep 5
```

### 시스템 콜

```bash
# 모든 시스템 콜
perf record -e 'syscalls:sys_enter_*' -a sleep 5

# 특정 시스템 콜
perf record -e syscalls:sys_enter_read -ag sleep 5

# 네트워크 연결
perf record -e syscalls:sys_enter_connect -ag sleep 5
```

### 네트워크

```bash
# TCP 전송
perf record -e 'tcp:*' -a sleep 5

# 소켓 버퍼
perf record -e 'skb:*' -a sleep 5
```

---

## 필터링

```bash
# 블록 크기 필터
perf record -e block:block_rq_complete --filter 'nr_sector > 200'

# 쓰기만 필터
perf record -e block:block_rq_complete --filter 'rwbs ~ "*W*"'

# 읽기 크기 필터
perf record -e syscalls:sys_enter_read --filter 'count < 10'
```

---

## 고급 옵션

### 여러 이벤트 동시 수집

```bash
# CPU와 컨텍스트 스위치
perf record -F 99 -e cpu-clock -e cs -a -g sleep 5

# 스택 깊이 개별 설정
perf record -F 99 -e cpu-clock/max-stack=2/ -e cs/max-stack=5/ -a -g sleep 5
```

### 컨테이너 프로파일링

```bash
# Docker 컨테이너 프로파일링
perf record -F 99 -e cpu-clock --cgroup=docker/<container_id> -a sleep 10
```

### 설정 확인

```bash
# raw 설정 표시
perf record -vv -e context-switches -a sleep 1
```

---

## Flame Graph용 프로파일링

```bash
# DWARF 콜 그래프로 수집
perf record -F 99 -g --call-graph dwarf -a sleep 30

# Frame Pointer 방식
perf record -F 99 -g -a sleep 30

# 특정 프로세스
perf record -F 99 -g --call-graph dwarf -p <pid> sleep 30
```

---

## 실용적인 예시

### 웹 서버 프로파일링

```bash
# HTTP 요청 처리 분석
sudo perf record -F 99 -g -p $(pgrep nginx) sleep 60
```

### 데이터베이스 프로파일링

```bash
# PostgreSQL 프로파일링
sudo perf record -F 99 -g --call-graph dwarf -p $(pgrep postgres) sleep 30
```

### Java 애플리케이션

```bash
# JVM 프로파일링 (perf-map-agent 필요)
perf record -F 99 -g -p <java_pid> sleep 30
```

### Node.js 애플리케이션

```bash
# --perf_basic_prof 플래그로 시작된 Node.js
node --perf_basic_prof app.js &
perf record -F 99 -g -p $! sleep 30
```

### 메모리 분석

```bash
# 페이지 폴트 추적
perf record -e page-faults -ag sleep 10

# 마이너 폴트 (RSS 증가 추적)
perf record -e minor-faults -ag sleep 10

# 모든 마이너 폴트 추적
perf record -e minor-faults -c 1 -ag sleep 10
```

---

## 데이터 파일 관리

### 파일 크기 제어

```bash
# 크기 제한
perf record --mmap-pages=128 ./my_program

# 압축 활성화
perf record -z ./my_program
```

### 파일 정보 확인

```bash
# 헤더 정보
perf report --header-only

# build-id 확인
perf buildid-list -i perf.data
```

---

## 트러블슈팅

### 파일 디스크립터 제한

멀티 스레드 프로그램에서 "too many open files" 오류 발생 시:

```bash
ulimit -n 2048
```

### 심볼 해석 문제

```bash
# 디버그 심볼 필요
# 컴파일 시 -g 옵션 사용

# 커널 심볼 활성화
sudo sysctl kernel.kptr_restrict=0
```

### 권한 문제

```bash
# perf_event_paranoid 설정
sudo sysctl kernel.perf_event_paranoid=-1

# 또는 sudo 사용
sudo perf record -a ./my_program
```

### 빈 프로파일

```bash
# 샘플 수 확인
perf report --stdio | head

# 이벤트 확인
perf list | grep cycles
```

---

## 참고 자료

- [perf-record(1) man page](https://man7.org/linux/man-pages/man1/perf-record.1.html)
- [perf Wiki - Tutorial](https://perfwiki.github.io/main/tutorial/)
- [Brendan Gregg's perf Examples](https://www.brendangregg.com/perf.html)

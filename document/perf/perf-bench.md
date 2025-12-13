# perf bench

> 시스템 성능 벤치마크 도구

## 개요

`perf bench`는 Linux 커널과 시스템의 다양한 서브시스템 성능을 측정하는 마이크로벤치마크 프레임워크입니다. 스케줄러, 메모리, 시스템 콜 등의 성능을 테스트하고 변경 사항의 영향을 측정할 수 있습니다.

---

## 기본 사용법

```bash
# 모든 벤치마크 실행
perf bench all

# 특정 서브시스템 벤치마크
perf bench sched all
perf bench mem all
perf bench syscall all

# 특정 테스트 실행
perf bench sched messaging
perf bench mem memcpy
```

---

## 공통 옵션

| 옵션 | 설명 |
|------|------|
| `-r, --repeat <n>` | 반복 횟수 (기본: 10) |
| `-f, --format <fmt>` | 출력 형식 (default, simple) |

### 출력 형식

```bash
# 기본 형식 (사람 읽기용)
perf bench sched messaging

# 간단 형식 (스크립트 처리용)
perf bench -f simple sched messaging
```

---

## 서브시스템별 벤치마크

### 스케줄러 (sched)

```bash
# 모든 스케줄러 벤치마크
perf bench sched all
```

#### messaging

프로세스/스레드 간 메시지 전달 성능 테스트입니다. hackbench 기반입니다.

```bash
# 기본 실행
perf bench sched messaging

# 옵션
perf bench sched messaging -p    # 프로세스 사용 (기본: 스레드)
perf bench sched messaging -t    # 스레드 사용
perf bench sched messaging -g 20 # 그룹 수 (기본: 10)
perf bench sched messaging -l 100 # 루프 수 (기본: 100)
```

#### pipe

pipe() 시스템 콜 성능 테스트입니다.

```bash
# 기본 실행
perf bench sched pipe

# 옵션
perf bench sched pipe -l 1000000  # 루프 수
```

### 시스템 콜 (syscall)

```bash
# 모든 시스템 콜 벤치마크
perf bench syscall all
```

#### basic

간단한 시스템 콜(getppid) 성능 테스트입니다.

```bash
# 기본 실행
perf bench syscall basic

# 옵션
perf bench syscall basic -l 10000000  # 루프 수
```

### 메모리 (mem)

```bash
# 모든 메모리 벤치마크
perf bench mem all
```

#### memcpy

메모리 복사 성능 테스트입니다.

```bash
# 기본 실행
perf bench mem memcpy

# 옵션
perf bench mem memcpy -s 1GB      # 복사할 크기
perf bench mem memcpy -l 10       # 반복 횟수
perf bench mem memcpy -f default  # 기본 memcpy 사용
perf bench mem memcpy -c          # 사이클 단위 출력
```

#### memset

메모리 설정 성능 테스트입니다.

```bash
# 기본 실행
perf bench mem memset

# 옵션
perf bench mem memset -s 1GB      # 설정할 크기
perf bench mem memset -l 10       # 반복 횟수
```

### NUMA

NUMA 스케줄링과 메모리 관리 벤치마크입니다.

```bash
# NUMA 벤치마크
perf bench numa mem
```

### Futex

Futex (Fast Userspace muTEX) 성능 테스트입니다.

```bash
# 모든 futex 벤치마크
perf bench futex all
```

#### hash

Futex 해시 테이블 성능 테스트입니다.

```bash
perf bench futex hash
```

#### wake

Futex wake 작업 성능 테스트입니다.

```bash
perf bench futex wake
```

#### wake-parallel

병렬 futex wake 성능 테스트입니다.

```bash
perf bench futex wake-parallel
```

#### requeue

Futex requeue 작업 성능 테스트입니다.

```bash
perf bench futex requeue
```

#### lock-pi

Priority Inheritance 락 성능 테스트입니다.

```bash
perf bench futex lock-pi
```

### Epoll

이벤트 폴링 성능 테스트입니다.

```bash
# Epoll 벤치마크
perf bench epoll wait
perf bench epoll ctl
```

### 내부 (internals)

perf 내부 이벤트 합성 성능 테스트입니다.

```bash
perf bench internals synthesize
```

---

## 실용적인 예시

### 스케줄러 성능 테스트

```bash
# 기본 스케줄러 성능
perf bench sched messaging

# 높은 부하 테스트
perf bench sched messaging -g 40 -l 200

# 프로세스 vs 스레드 비교
perf bench sched messaging -p  # 프로세스
perf bench sched messaging -t  # 스레드
```

### 메모리 대역폭 측정

```bash
# 다양한 크기로 memcpy 테스트
perf bench mem memcpy -s 4KB
perf bench mem memcpy -s 1MB
perf bench mem memcpy -s 1GB

# 반복 측정으로 신뢰도 확보
perf bench -r 20 mem memcpy -s 1MB
```

### 시스템 콜 오버헤드 측정

```bash
# 시스템 콜 레이턴시
perf bench syscall basic -l 10000000
```

### 락 경합 테스트

```bash
# Futex 락 성능
perf bench futex lock-pi

# Wake 성능
perf bench futex wake
perf bench futex wake-parallel
```

### 변경 사항 영향 비교

```bash
# 변경 전 결과 저장
perf bench -f simple sched messaging > before.txt

# (시스템 변경 후)

# 변경 후 결과 저장
perf bench -f simple sched messaging > after.txt

# 비교
diff before.txt after.txt
```

---

## 출력 해석

### 기본 출력

```
# Running 'sched/messaging' benchmark:
# 20 sender and receiver processes per group
# 10 groups == 400 processes run

     Total time: 0.308 [sec]
```

### simple 형식 출력

```
0.308
```

### 메모리 벤치마크 출력

```
# Running 'mem/memcpy' benchmark:
# function 'default' (Default memcpy() provided by glibc)
# Copying 1MB bytes ...

       8.628614 GB/sec
```

---

## 고급 사용

### 자동화 스크립트

```bash
#!/bin/bash
# 벤치마크 자동화 스크립트

echo "=== Scheduler Benchmarks ==="
perf bench -r 5 sched messaging
perf bench -r 5 sched pipe

echo "=== Memory Benchmarks ==="
for size in 4KB 64KB 1MB 16MB 256MB; do
    echo "memcpy $size:"
    perf bench -r 5 -f simple mem memcpy -s $size
done

echo "=== Syscall Benchmark ==="
perf bench -r 5 syscall basic
```

### 결과 CSV 출력

```bash
# CSV 형식으로 결과 수집
echo "size,throughput" > memcpy_results.csv
for size in 4KB 64KB 1MB 16MB 256MB 1GB; do
    result=$(perf bench -f simple mem memcpy -s $size 2>/dev/null)
    echo "$size,$result" >> memcpy_results.csv
done
```

### 시스템 튜닝 비교

```bash
# 기본 설정 테스트
echo "Default settings:"
perf bench sched messaging

# 튜닝 후 테스트
echo "After tuning:"
# (sysctl 또는 기타 튜닝 적용)
perf bench sched messaging
```

---

## 벤치마크 선택 가이드

| 목적 | 벤치마크 |
|------|----------|
| IPC 성능 | `sched messaging` |
| 파이프 성능 | `sched pipe` |
| 시스템 콜 오버헤드 | `syscall basic` |
| 메모리 대역폭 | `mem memcpy`, `mem memset` |
| 락 경합 | `futex lock-pi`, `futex wake` |
| 이벤트 폴링 | `epoll wait`, `epoll ctl` |
| NUMA 성능 | `numa mem` |

---

## 주의사항

### 정확한 측정을 위해

1. ** 백그라운드 프로세스 최소화**: 다른 워크로드 종료
2. **CPU 주파수 고정**:
   ```bash
   sudo cpupower frequency-set -g performance
   ```
3. ** 여러 번 반복**: `-r` 옵션으로 반복 측정
4. **NUMA 인식**: NUMA 시스템에서 노드별 테스트
5. ** 웜업**: 첫 번째 결과는 제외 고려

### 결과 해석 주의

- 마이크로벤치마크 결과는 실제 워크로드와 다를 수 있음
- 절대값보다 상대적 비교에 유용
- 시스템 구성(CPU, 메모리, 커널 버전)에 따라 결과 상이

---

## 참고 자료

- [perf-bench(1) man page](https://man7.org/linux/man-pages/man1/perf-bench.1.html)
- [Linux kernel source: tools/perf/bench](https://github.com/torvalds/linux/tree/master/tools/perf/bench)
- [hackbench](https://people.redhat.com/mingo/cfs-scheduler/tools/hackbench.c)

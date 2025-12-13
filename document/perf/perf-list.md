# perf list

> 사용 가능한 이벤트 목록 조회 도구

## 개요

`perf list`는 현재 시스템에서 사용 가능한 모든 perf 이벤트를 나열합니다. 하드웨어 이벤트, 소프트웨어 이벤트, 트레이스포인트, 동적 프로브 등을 확인할 수 있습니다.

---

## 기본 사용법

```bash
# 모든 이벤트 표시
perf list

# 특정 패턴으로 필터링
perf list 'sched:*'
perf list '*cache*'

# 이벤트 수 확인
perf list | wc -l
```

---

## 이벤트 유형별 조회

### 하드웨어 이벤트

```bash
# 하드웨어 이벤트만 표시
perf list hw
perf list hardware
```

** 주요 하드웨어 이벤트:**

| 이벤트 | 설명 |
|--------|------|
| `cpu-cycles` (또는 `cycles`) | CPU 사이클 수 |
| `instructions` | 실행된 명령어 수 |
| `cache-references` | 캐시 참조 수 |
| `cache-misses` | 캐시 미스 수 |
| `branch-instructions` (또는 `branches`) | 분기 명령어 수 |
| `branch-misses` | 분기 예측 실패 수 |
| `bus-cycles` | 버스 사이클 수 |
| `stalled-cycles-frontend` | 프론트엔드 스톨 사이클 |
| `stalled-cycles-backend` | 백엔드 스톨 사이클 |
| `ref-cycles` | 참조 사이클 (TSC) |

### 소프트웨어 이벤트

```bash
# 소프트웨어 이벤트만 표시
perf list sw
perf list software
```

** 주요 소프트웨어 이벤트:**

| 이벤트 | 설명 |
|--------|------|
| `cpu-clock` | CPU 클럭 |
| `task-clock` | 태스크 클럭 |
| `page-faults` (또는 `faults`) | 페이지 폴트 |
| `context-switches` (또는 `cs`) | 컨텍스트 스위치 |
| `cpu-migrations` (또는 `migrations`) | CPU 마이그레이션 |
| `minor-faults` | 마이너 페이지 폴트 |
| `major-faults` | 메이저 페이지 폴트 |
| `alignment-faults` | 정렬 폴트 |
| `emulation-faults` | 에뮬레이션 폴트 |
| `dummy` | 더미 이벤트 |
| `bpf-output` | BPF 출력 |

### 하드웨어 캐시 이벤트

```bash
# 캐시 이벤트만 표시
perf list cache
perf list hwcache
```

** 주요 캐시 이벤트:**

| 이벤트 | 설명 |
|--------|------|
| `L1-dcache-loads` | L1 데이터 캐시 로드 |
| `L1-dcache-load-misses` | L1 데이터 캐시 로드 미스 |
| `L1-dcache-stores` | L1 데이터 캐시 저장 |
| `L1-dcache-store-misses` | L1 데이터 캐시 저장 미스 |
| `L1-icache-load-misses` | L1 명령 캐시 로드 미스 |
| `LLC-loads` | Last Level Cache 로드 |
| `LLC-load-misses` | LLC 로드 미스 |
| `LLC-stores` | LLC 저장 |
| `LLC-store-misses` | LLC 저장 미스 |
| `dTLB-loads` | 데이터 TLB 로드 |
| `dTLB-load-misses` | 데이터 TLB 로드 미스 |
| `iTLB-loads` | 명령 TLB 로드 |
| `iTLB-load-misses` | 명령 TLB 로드 미스 |
| `branch-loads` | 분기 로드 |
| `branch-load-misses` | 분기 로드 미스 |

### 트레이스포인트

```bash
# 모든 트레이스포인트
perf list tracepoint

# 특정 서브시스템 트레이스포인트
perf list 'sched:*'
perf list 'syscalls:*'
perf list 'block:*'
perf list 'net:*'
perf list 'irq:*'
```

** 주요 트레이스포인트 카테고리:**

| 카테고리 | 설명 | 예시 |
|----------|------|------|
| `sched` | 스케줄러 | `sched:sched_switch` |
| `syscalls` | 시스템 콜 | `syscalls:sys_enter_read` |
| `block` | 블록 I/O | `block:block_rq_issue` |
| `net` | 네트워크 | `net:net_dev_xmit` |
| `irq` | 인터럽트 | `irq:irq_handler_entry` |
| `timer` | 타이머 | `timer:timer_start` |
| `signal` | 시그널 | `signal:signal_generate` |
| `kmem` | 커널 메모리 | `kmem:kmalloc` |
| `writeback` | 페이지 라이트백 | `writeback:writeback_start` |
| `ext4` | ext4 파일시스템 | `ext4:ext4_read_block` |
| `tcp` | TCP | `tcp:tcp_retransmit_skb` |

### Raw 하드웨어 이벤트

```bash
# PMU 이벤트
perf list pmu
```

---

## 패턴 필터링

```bash
# 와일드카드 사용
perf list '*miss*'
perf list 'cache*'
perf list 'L1-*'

# 정규식 패턴
perf list 'sched:sched_*'
perf list 'syscalls:sys_enter_*'

# 특정 키워드 검색
perf list | grep cache
perf list | grep -i branch
```

---

## 이벤트 수정자

이벤트 이름 뒤에 수정자를 붙여 필터링할 수 있습니다.

### 권한 수준 수정자

| 수정자 | 설명 |
|--------|------|
| `u` | 유저 스페이스만 |
| `k` | 커널 스페이스만 |
| `h` | 하이퍼바이저 |
| `H` | 호스트 (가상화) |
| `G` | 게스트 (가상화) |

```bash
# 유저 스페이스 사이클만
perf stat -e cycles:u ./my_program

# 커널 스페이스만
perf stat -e cycles:k ./my_program
```

### 정밀도 수정자

| 수정자 | 설명 |
|--------|------|
| `p` | 정밀 이벤트 (PEBS) |
| `pp` | 더 정밀한 이벤트 |
| `ppp` | 최대 정밀도 |

```bash
# PEBS 정밀 샘플링
perf record -e cycles:pp ./my_program
```

### 기타 수정자

| 수정자 | 설명 |
|--------|------|
| `D` | 핀된 이벤트 (디버그) |
| `I` | 스레드 무시 |
| `S` | 슬랙 기반 샘플링 |

---

## Raw 이벤트 지정

프로세서별 특정 이벤트는 raw 형식으로 지정합니다.

### Intel 형식

```bash
# rXXYY 형식 (XX=umask, YY=event)
perf stat -e r003c -a sleep 1

# cpu/event=0xYY,umask=0xXX/ 형식
perf stat -e cpu/event=0x3c,umask=0x00/ -a sleep 1

# 추가 옵션 포함
perf stat -e cpu/event=0x0e,umask=0x01,inv=1,cmask=1/ -a sleep 1
```

### Raw 이벤트 파라미터

| 파라미터 | 설명 |
|----------|------|
| `event` | 이벤트 코드 |
| `umask` | 유닛 마스크 |
| `edge` | 엣지 감지 |
| `inv` | 인버트 |
| `cmask` | 카운터 마스크 |
| `any` | 아무 스레드 |

---

## USDT (User-level Statically Defined Tracing)

```bash
# USDT 프로브 목록
perf list 'sdt_*'

# Node.js USDT
perf list | grep sdt_node

# Python USDT
perf list | grep sdt_python
```

---

## 실용적인 예시

### CPU 관련 이벤트 찾기

```bash
# CPU 관련 모든 이벤트
perf list | grep -i cpu

# 사이클 관련
perf list | grep -i cycle
```

### 메모리 관련 이벤트 찾기

```bash
# 메모리/캐시 관련
perf list | grep -i mem
perf list | grep -i cache
perf list | grep -i tlb
```

### I/O 관련 이벤트 찾기

```bash
# 블록 I/O
perf list 'block:*'

# 파일시스템
perf list 'ext4:*'
perf list 'xfs:*'
```

### 네트워크 관련 이벤트 찾기

```bash
# 네트워크 이벤트
perf list 'net:*'
perf list 'tcp:*'
perf list 'sock:*'
```

### 시스템 콜 이벤트 찾기

```bash
# 시스템 콜 진입점
perf list 'syscalls:sys_enter_*'

# 시스템 콜 반환점
perf list 'syscalls:sys_exit_*'

# 특정 시스템 콜
perf list | grep sys_enter_read
perf list | grep sys_enter_write
```

---

## 이벤트 가용성 확인

### 시스템별 차이

```bash
# 하드웨어 이벤트는 프로세서에 따라 다름
perf list hw

# 가상화 환경에서는 일부 이벤트 제한
# 클라우드 VM에서 사용 가능한 이벤트 확인
perf list | wc -l
```

### 권한별 차이

```bash
# root 권한으로 더 많은 이벤트 접근 가능
sudo perf list | wc -l
perf list | wc -l  # 일반 사용자

# perf_event_paranoid 확인
cat /proc/sys/kernel/perf_event_paranoid
```

---

## 트러블슈팅

### 트레이스포인트가 보이지 않음

```bash
# debugfs 마운트 확인
mount | grep debugfs

# 마운트되어 있지 않으면
sudo mount -t debugfs none /sys/kernel/debug
```

### 이벤트 접근 거부

```bash
# perf_event_paranoid 확인 및 설정
sudo sysctl kernel.perf_event_paranoid=-1
```

### 이벤트 사용 가능 여부 테스트

```bash
# 이벤트 테스트
perf stat -e <event_name> true

# 오류 메시지 확인으로 지원 여부 판단
```

---

## 참고 자료

- [perf-list(1) man page](https://man7.org/linux/man-pages/man1/perf-list.1.html)
- [Intel® 64 and IA-32 Architectures Software Developer's Manual](https://www.intel.com/content/www/us/en/developer/articles/technical/intel-sdm.html)
- [AMD Processor Programming Reference](https://www.amd.com/en/support/tech-docs)
- [Linux Kernel Tracepoints](https://www.kernel.org/doc/Documentation/trace/tracepoints.txt)

# Tracing과 Observability 활용

---

## 목차

1. [eBPF 관측의 강점](#ebpf-관측의-강점)
2. [Tracing 종류와 선택](#tracing-종류와-선택)
3. [CPU 프로파일링](#cpu-프로파일링)
4. [Off-CPU 분석](#off-cpu-분석)
5. [지연 분석](#지연-분석)
6. [USDT와 애플리케이션 트레이싱](#usdt와-애플리케이션-트레이싱)
7. [생태계 도구](#생태계-도구)
8. [프로덕션 운용 패턴](#프로덕션-운용-패턴)
9. [참고 자료](#참고-자료)

---

## eBPF 관측의 강점

전통 도구(`strace`, `gdb`, `perf trace`) 대비:

- **저비용**: kprobe/uprobe overhead가 매우 낮아 프로덕션에서 상시 가능
- **저지연**: 컨텍스트 스위치 없이 커널 안에서 즉시 처리
- **선택적 캡처**: BPF 코드로 필터링 후 필요한 것만 사용자 공간으로
- **풍부한 컨텍스트**: PID/TID/UID/cgroup/SELinux 등 신뢰 가능한 메타데이터
- **안전성**: verifier가 시스템 안정성 보장

`strace -p`는 ptrace 기반이라 syscall마다 컨텍스트 스위치가 두 번 발생합니다(수십 배 느림). eBPF는 같은 작업을 거의 무비용으로 처리합니다.

---

## Tracing 종류와 선택

| 종류 | 안정성 | 오버헤드 | 추천 용도 |
| --- | --- | --- | --- |
| **Static Tracepoint** | 안정 ABI | 매우 낮음 | 첫 선택지 |
| **kfunc/fentry** | BTF 기반, 안정 | 가장 낮음 (5.5+) | 새 코드 |
| **kprobe** | 함수 시그니처 의존 | 낮음 | tracepoint 없는 함수 |
| **kretprobe** | 함수 시그니처 의존 | 약간 높음 | 함수 종료/지연 측정 |
| **USDT** | 애플리케이션 ABI | 낮음 | 애플리케이션 내부 이벤트 |
| **uprobe** | 함수 시그니처 의존 | 중간 | 자기 코드 트레이싱 |

### 선택 가이드

1. tracepoint가 있으면 그걸 쓴다. ABI 안정.
2. fentry/kfunc 가능하면 그걸 쓴다. 가장 빠름.
3. 안정 ABI가 없으면 kprobe.
4. 사용자 공간 라이브러리는 USDT > uprobe.

### tracepoint 찾기

```bash
sudo bpftrace -l 'tracepoint:*' | head -50
sudo find /sys/kernel/debug/tracing/events -maxdepth 2 -type d | head
```

### tracepoint 인자

```bash
$ sudo bpftrace -lv 'tracepoint:syscalls:sys_enter_openat'
tracepoint:syscalls:sys_enter_openat
    int dfd
    const char * filename
    int flags
    umode_t mode
```

---

## CPU 프로파일링

### 99Hz 스택 샘플링

```bash
sudo bpftrace -e '
profile:hz:99 {
    @[ustack, kstack, comm] = count();
}
interval:s:30 {
    exit();
}'
```

99 Hz를 쓰는 이유는 100 Hz와 자연스럽게 어긋나 lockstep 샘플링 편향을 피할 수 있기 때문입니다.

### Flame Graph

```bash
# bcc 도구
sudo profile -af 30 > out.stacks

# FlameGraph 변환
git clone https://github.com/brendangregg/FlameGraph
./FlameGraph/flamegraph.pl < out.stacks > flame.svg

xdg-open flame.svg
```

각 박스의 너비는 해당 함수가 CPU를 점유한 시간 비율을 나타냅니다. 가장 넓은 박스가 hot path입니다.

### Continuous profiling

프로덕션에서 상시 동작하는 프로파일러:
- **Parca** — Polar Signals
- **Pyroscope** — Grafana
- **Perforator** — Yandex
- **Pixie** — New Relic

위 도구들은 모두 eBPF profile probe를 기반으로 동작합니다.

---

## Off-CPU 분석

CPU를 사용하지 않는데도 시스템이 느리다면 — 프로세스가 어디서 대기하는지 파악하는 것이 핵심입니다.

### offcputime

```bash
sudo offcputime -df 30 > out.stacks
./FlameGraph/flamegraph.pl --bgcolors=blue < out.stacks > offcpu-flame.svg
```

스택의 각 박스 너비는 해당 코드 경로에서 대기한 시간을 나타냅니다.

### bpftrace로

```
kprobe:finish_task_switch {
    @start[arg0] = nsecs;
}

kretprobe:finish_task_switch /@start[arg0]/ {
    $task = (struct task_struct *)arg0;
    @[$task->comm, kstack] = sum(nsecs - @start[arg0]);
    delete(@start[arg0]);
}
```

### 결합

CPU + Off-CPU를 합산하면 전체 시간이 어디에 쓰이는지(CPU 작업 vs 대기)를 한눈에 파악할 수 있습니다.

---

## 지연 분석

### 함수 지연 분포

```bash
sudo funclatency 'vfs_read'
sudo funclatency -p 1234 'tcp_*'
```

### syscall 지연

```bash
sudo bpftrace -e '
tracepoint:syscalls:sys_enter_openat { @start[tid] = nsecs; }
tracepoint:syscalls:sys_exit_openat /@start[tid]/ {
    @ns[comm] = hist(nsecs - @start[tid]);
    delete(@start[tid]);
}'
```

### 디스크 IO

```bash
sudo biolatency -m 10
```

- `-m`: 밀리초 단위
- `10`: 10초

```
     msecs               : count     distribution
       0 -> 1            : 12     |@                          |
       2 -> 3            : 234    |@@@@@@@@@@@@@@@@@@@@@@@@@@ |
       4 -> 7            : 89     |@@@@@@@@@                  |
       8 -> 15           : 5      |                           |
```

### 큐 지연 (스케줄러)

```bash
sudo runqlat
```

CPU를 할당받기 전 런 큐에서 대기한 시간을 측정합니다. CPU 부족이나 우선순위 역전을 진단할 때 유용합니다.

---

## USDT와 애플리케이션 트레이싱

USDT는 애플리케이션이 미리 삽입해 둔 트레이스 포인트입니다. systemtap에서 시작했지만 eBPF에서도 활용합니다.

### 사용 가능한 USDT 보기

```bash
sudo bpftrace -l 'usdt:/usr/local/bin/myapp:*'
sudo readelf -n /usr/local/bin/myapp | grep -i 'NT_STAPSDT' -A 8
```

### 잘 알려진 USDT 제공 프로그램

| 프로그램 | USDT |
| --- | --- |
| MySQL | query__start, query__done, sql__lock 등 |
| Postgres | query__start, transaction__start 등 |
| Python (DTrace 빌드) | function__entry, function__return |
| Node.js | gc__start, http__server__request |
| Java (libstapsdt) | 사용자 정의 가능 |
| OpenJDK (with USDT) | hotspot:thread__start 등 |

### 예: MySQL 느린 쿼리

```bash
sudo bpftrace -e '
usdt:/usr/sbin/mysqld:mysql:query__start {
    @start[tid] = nsecs;
    @sql[tid] = str(arg0);
}

usdt:/usr/sbin/mysqld:mysql:query__done /@start[tid]/ {
    $delta = nsecs - @start[tid];
    if ($delta > 100000000) {   // 100ms
        printf("SLOW: %dms %s\n", $delta / 1000000, @sql[tid]);
    }
    delete(@start[tid]);
    delete(@sql[tid]);
}'
```

### 자기 애플리케이션에 USDT 추가

C/C++:
```c
#include <sys/sdt.h>

void handle_request(int id) {
    DTRACE_PROBE1(myapp, request_start, id);
    // ...
    DTRACE_PROBE1(myapp, request_end, id);
}
```

Go: [github.com/aclements/go-stapdtrace](https://github.com/aclements/go-stapdtrace) 등.

Python: `pystap` 또는 DTrace 활성화 빌드.

---

## 생태계 도구

### Pixie

쿠버네티스 클러스터에 설치하면 자동으로:
- 모든 프로세스의 메트릭 수집
- HTTP/gRPC/Kafka/MySQL/Postgres 자동 디코드
- 코드 변경 없이 분산 트레이싱
- 대시보드 + script 언어 (PxL)

### Pyroscope (Grafana)

continuous profiling. eBPF 기반 자동 프로파일러 + 다양한 언어별 SDK.

### Parca

Polar Signals의 continuous profiling. open source.

### Tetragon (Cilium)

런타임 보안. 의심스러운 행위(파일 접근, 네트워크 연결, syscall) 감지·차단.

### Falco

eBPF 기반 침입 탐지. CNCF 졸업 프로젝트.

### Tracee (Aqua Security)

런타임 보안 + 포렌식. 시스템 이벤트 풍부한 캡처.

### Hubble (Cilium)

서비스 단위 네트워크 가시성.

### Coroot

OpenTelemetry + eBPF 자동 instrumentation.

---

## 프로덕션 운용 패턴

### 오버헤드 측정

운영 환경 적용 전에 측정:

```bash
# CPU benchmark with/without BPF
sysbench --threads=8 cpu run
# (BPF attach)
sysbench --threads=8 cpu run
```

대부분의 단순 BPF는 1~5% 이내. 복잡한 트레이싱은 20%+ 가능.

### Sampling

이벤트가 너무 많을 경우 샘플링을 적용합니다:

```c
SEC("kprobe/tcp_v4_connect")
int trace(struct pt_regs *ctx) {
    if (bpf_get_prandom_u32() > 0x80000000) return 0;   // 50% 샘플
    // ...
}
```

### 백프레셔

ringbuf가 가득 차면 새 이벤트가 손실됩니다. 사용자 공간이 충분히 빠르게 소비하는지, 아니면 BPF 측에서 명시적으로 드롭할지 결정해야 합니다.

```c
// 사용자 공간이 못 따라오면 그냥 버림
struct event *e = bpf_ringbuf_reserve(&events, sizeof(*e), 0);
if (!e) {
    __sync_fetch_and_add(&dropped, 1);
    return 0;
}
```

### 기능 감지

배포 대상 머신마다 커널 버전이 다를 수 있습니다:

```c
// libbpf
if (!bpf_program__attach(skel->progs.fentry_handler)) {
    // fentry 실패하면 kprobe로 폴백
    bpf_program__attach_kprobe(skel->progs.kprobe_handler, false, "vfs_read");
}
```

### 보안 권한

```bash
sudo setcap cap_bpf,cap_perfmon+ep /usr/local/bin/myprobe
```

`CAP_SYS_ADMIN` 대신 `CAP_BPF` + `CAP_PERFMON` (5.8+)으로 권한 최소화.

---

## 참고 자료

- [Brendan Gregg: BPF Performance Tools (책)](https://www.brendangregg.com/bpf-performance-tools-book.html)
- [Brendan Gregg: 60 second checklist](https://www.brendangregg.com/blog/2015-12-03/linux-perf-tools.html)
- [FlameGraph](https://github.com/brendangregg/FlameGraph)
- [Pixie docs](https://docs.px.dev/)
- [Parca](https://www.parca.dev/)
- [Pyroscope](https://grafana.com/oss/pyroscope/)

# bcc (BPF Compiler Collection)

> 이 문서는 IO Visor의 bcc 프로젝트 사용법을 정리한 것입니다.
> 원본: https://github.com/iovisor/bcc

---

## 목차

1. [bcc란?](#bcc란)
2. [설치](#설치)
3. [내장 도구 사용](#내장-도구-사용)
4. [Python으로 직접 작성](#python으로-직접-작성)
5. [주요 도구 카탈로그](#주요-도구-카탈로그)
6. [성능 분석 워크플로우](#성능-분석-워크플로우)
7. [한계와 대안](#한계와-대안)
8. [참고 자료](#참고-자료)

---

## bcc란?

**BPF Compiler Collection**. IO Visor 프로젝트가 만든 BPF 학습·관측 도구 모음. 100+개의 즉시 사용 가능한 트레이싱 도구와 Python/C++ 라이브러리를 제공합니다.

특징:
- **런타임 컴파일**: Python에서 BPF C 코드를 문자열로 작성, Clang으로 즉석 컴파일
- 학습에 최적 — 같은 도구의 .py 소스를 보면 BPF 동작 이해
- 풍부한 ready-made 도구
- 단점: LLVM/커널 헤더 필요, 시작 시간 느림

---

## 설치

### Ubuntu/Debian

```bash
sudo apt install bpfcc-tools libbpfcc python3-bpfcc linux-headers-$(uname -r)
```

도구는 `/usr/sbin/<tool>-bpfcc` 같은 이름으로 설치됩니다.

### RHEL/Fedora

```bash
sudo dnf install bcc bcc-tools python3-bcc kernel-devel
```

도구는 `/usr/share/bcc/tools/<tool>` 에 설치.

### libbpf 변형

bcc 일부 도구는 libbpf-tools(`/usr/share/bcc/libbpf-tools/`)로 마이그레이션 진행 중. 더 빠르고 의존성 적음.

---

## 내장 도구 사용

### 가장 자주 쓰는 도구들

```bash
# 시스템 전반 모니터링
sudo execsnoop          # 새로 fork된 프로세스 추적
sudo opensnoop          # open() 호출 추적
sudo tcpconnect         # TCP 연결 시도
sudo tcpaccept          # TCP accept
sudo biolatency         # 블록 IO 지연 히스토그램
sudo profile            # CPU 프로파일러 (스택 샘플링)
sudo offcputime         # off-CPU 분석 (왜 대기 중인지)
sudo runqlat            # 런 큐 지연 (스케줄러)
sudo memleak            # 메모리 누수 추적
```

### 옵션 패턴

대부분의 도구가 비슷한 옵션 패턴을 따릅니다:

```bash
# 특정 PID
sudo execsnoop -p 1234

# 시간 제한
sudo profile 10            # 10초

# 출력 형식
sudo biolatency -m         # 밀리초 단위
sudo biolatency -P         # 프로세스별
```

### `-h` 와 `man`

```bash
sudo execsnoop -h
man execsnoop
```

각 도구는 자체 매뉴얼이 있습니다.

---

## Python으로 직접 작성

### 가장 간단한 예제

```python
#!/usr/bin/env python3
from bcc import BPF

text = """
int hello(void *ctx) {
    bpf_trace_printk("Hello from BPF\\n");
    return 0;
}
"""

b = BPF(text=text)
b.attach_kprobe(event="sys_clone", fn_name="hello")

print("Tracing... Ctrl-C to exit")
b.trace_print()
```

실행하면 `clone()` 이 호출될 때마다 메시지 출력.

### 데이터 수집 예제

```python
from bcc import BPF
from time import sleep

text = """
BPF_HASH(counts, u32);

int count_clones(void *ctx) {
    u32 pid = bpf_get_current_pid_tgid() >> 32;
    u64 zero = 0, *val;
    val = counts.lookup_or_try_init(&pid, &zero);
    if (val) (*val)++;
    return 0;
}
"""

b = BPF(text=text)
b.attach_kprobe(event="sys_clone", fn_name="count_clones")

while True:
    sleep(2)
    print("\n%-10s %-10s" % ("PID", "COUNT"))
    counts = b["counts"]
    for k, v in sorted(counts.items(), key=lambda kv: -kv[1].value):
        print("%-10d %-10d" % (k.value, v.value))
    counts.clear()
```

### 매크로 단축

bcc는 BPF 코드 안에서 자체 매크로를 제공:

| BCC 매크로 | 의미 |
| --- | --- |
| `BPF_HASH(name, key_type, value_type)` | hash map 선언 |
| `BPF_ARRAY(name, type, size)` | array map |
| `BPF_PERCPU_ARRAY(...)` | percpu |
| `BPF_HISTOGRAM(name)` | 히스토그램 |
| `BPF_PERF_OUTPUT(name)` | perf event array |

이 매크로들은 컴파일 타임에 raw libbpf 선언으로 펼쳐집니다.

### Tracepoint

```python
from bcc import BPF

text = """
TRACEPOINT_PROBE(syscalls, sys_enter_openat) {
    bpf_trace_printk("open: %s\\n", args->filename);
    return 0;
}
"""
b = BPF(text=text)
b.trace_print()
```

`TRACEPOINT_PROBE` 매크로가 자동으로 적절한 SEC와 인자 구조체를 처리.

### Perf event 출력

```python
text = """
struct event_t { u32 pid; u64 ts; };
BPF_PERF_OUTPUT(events);

int trace(void *ctx) {
    struct event_t e = {};
    e.pid = bpf_get_current_pid_tgid() >> 32;
    e.ts = bpf_ktime_get_ns();
    events.perf_submit(ctx, &e, sizeof(e));
    return 0;
}
"""
b = BPF(text=text)
b.attach_kprobe(event="sys_clone", fn_name="trace")

def print_event(cpu, data, size):
    e = b["events"].event(data)
    print(f"pid={e.pid} ts={e.ts}")

b["events"].open_perf_buffer(print_event)
while True:
    b.perf_buffer_poll()
```

---

## 주요 도구 카탈로그

### 프로세스/시스템콜

| 도구 | 용도 |
| --- | --- |
| `execsnoop` | exec() 호출 (새 명령어) |
| `exitsnoop` | 프로세스 종료 |
| `killsnoop` | kill() syscall |
| `opensnoop` | 파일 열기 |
| `statsnoop` | stat() |
| `syscount` | syscall 빈도 |

### CPU/성능

| 도구 | 용도 |
| --- | --- |
| `profile` | CPU 스택 샘플링 (flame graph 데이터) |
| `offcputime` | off-CPU 시간 (대기 분석) |
| `runqlat` | 런 큐 지연 |
| `runqlen` | 런 큐 길이 |
| `cpudist` | CPU 사용 분포 |
| `funccount` | 함수 호출 빈도 |
| `funclatency` | 함수 지연 |
| `argdist` | 인자 분포 |

### IO/디스크

| 도구 | 용도 |
| --- | --- |
| `biolatency` | 블록 IO 지연 히스토그램 |
| `biosnoop` | 블록 IO 트레이싱 (per IO) |
| `biotop` | 블록 IO top |
| `xfsslower` / `ext4slower` | 느린 파일시스템 op |
| `dcsnoop` / `dcstat` | dentry 캐시 |
| `cachestat` | 페이지 캐시 통계 |

### 네트워크

| 도구 | 용도 |
| --- | --- |
| `tcpconnect` | TCP active open |
| `tcpaccept` | TCP passive open |
| `tcpretrans` | TCP 재전송 |
| `tcptop` | TCP throughput per connection |
| `tcplife` | TCP 연결 라이프타임 |
| `tcpdrop` | TCP drop 원인 |
| `sockstat` | 소켓 통계 |
| `udpconnect` | UDP 연결 |

### 메모리

| 도구 | 용도 |
| --- | --- |
| `memleak` | 메모리 누수 |
| `oomkill` | OOM 킬러 |
| `slabratetop` | SLAB 할당률 |
| `mountsnoop` | mount/umount |

### 기타

| 도구 | 용도 |
| --- | --- |
| `trace` | 임의 함수 trace (강력) |
| `argdist` | 함수 인자 분포 |
| `stackcount` | 함수 호출별 스택 카운트 |
| `dbslower` | MySQL/Postgres 느린 쿼리 |
| `gethostlatency` | DNS 해석 지연 |

---

## 성능 분석 워크플로우

### 1. CPU가 많이 쓰일 때

```bash
sudo profile -F 99 -af 30 > out.stacks   # 30초간 스택 샘플
flamegraph.pl < out.stacks > flame.svg    # FlameGraph로 시각화
```

[Brendan Gregg의 FlameGraph](https://github.com/brendangregg/FlameGraph) 와 결합.

### 2. 시스템이 느린데 CPU는 안 씀

```bash
sudo offcputime -df 30
```

프로세스가 어디서 대기하는지 (mutex, IO, 네트워크 등).

### 3. 디스크 IO 지연

```bash
sudo biolatency -m
sudo biosnoop
```

### 4. 어느 함수가 느린지

```bash
sudo funclatency 'vfs_read'
sudo funclatency -p 1234 'do_*'
```

### 5. 임의의 함수 디버깅

```bash
sudo trace 'do_sys_open "%s", arg2'
sudo trace -K 'tcp_v4_connect "%pK", arg1'   # 스택까지
```

### 6. 시스템콜 빈도

```bash
sudo syscount -P -d 10
```

10초간 프로세스별 syscall 빈도.

---

## 한계와 대안

### bcc 한계

- LLVM/Clang 의존 (수 백 MB)
- 시작 시간 느림 (수 초)
- 메모리 사용 큼
- 프로덕션 상시 운영보다 ad-hoc 디버깅에 적합

### libbpf-tools

bcc 도구 일부가 CO-RE 기반 libbpf로 재작성됨 — 더 빠르고 가볍습니다.

```bash
sudo apt install bcc-libbpf-tools
ls /usr/sbin/*-libbpf       # 또는 /usr/share/bcc/libbpf-tools/
```

같은 이름의 도구가 두 버전 있을 수 있음 (`opensnoop-bpfcc` vs libbpf-tools 버전).

### bpftrace

원라이너 빠른 디버깅에는 bpftrace가 더 편합니다 (다음 챕터).

### 마이그레이션 가이드

```
"학습/탐색" → bcc Python
"원라이너" → bpftrace
"프로덕션 도구" → libbpf + CO-RE
"인프라 시스템" → Cilium, Falco, Tetragon
```

---

## 참고 자료

- [bcc GitHub](https://github.com/iovisor/bcc)
- [bcc tutorial](https://github.com/iovisor/bcc/blob/master/docs/tutorial.md)
- [bcc Reference Guide](https://github.com/iovisor/bcc/blob/master/docs/reference_guide.md)
- [Brendan Gregg: BPF Performance Tools](https://www.brendangregg.com/bpf-performance-tools-book.html)

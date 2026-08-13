# bcc와 bpftrace

## bcc (BPF Compiler Collection)

> 원본: https://github.com/iovisor/bcc

---

### 목차

1. [bcc란?](#bcc란)
2. [설치](#설치)
3. [내장 도구 사용](#내장-도구-사용)
4. [Python으로 직접 작성](#python으로-직접-작성)
5. [주요 도구 카탈로그](#주요-도구-카탈로그)
6. [성능 분석 워크플로우](#성능-분석-워크플로우)
7. [한계와 대안](#한계와-대안)
8. [참고 자료](#참고-자료)

---

### bcc란?

**BPF Compiler Collection**. IO Visor 프로젝트가 만든 BPF 학습·관측 도구 모음. 100+개의 즉시 사용 가능한 트레이싱 도구와 Python/C++ 라이브러리를 제공함.

특징:
- **런타임 컴파일**: Python에서 BPF C 코드를 문자열로 작성, Clang으로 즉석 컴파일
- 학습에 최적 → 같은 도구의 .py 소스를 보면 BPF 동작 이해 가능
- 풍부한 ready-made 도구
- 단점: LLVM/커널 헤더 필요, 시작 시간 느림

---

### 설치

#### Ubuntu/Debian

```bash
sudo apt install bpfcc-tools libbpfcc python3-bpfcc linux-headers-$(uname -r)
```

도구는 `/usr/sbin/<tool>-bpfcc` 같은 이름으로 설치됨.

#### RHEL/Fedora

```bash
sudo dnf install bcc bcc-tools python3-bcc kernel-devel
```

도구는 `/usr/share/bcc/tools/<tool>` 에 설치.

#### libbpf 변형

bcc 일부 도구는 libbpf-tools(`/usr/share/bcc/libbpf-tools/`)로 마이그레이션 진행 중. 더 빠르고 의존성 적음.

---

### 내장 도구 사용

#### 가장 자주 쓰는 도구들

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

#### 옵션 패턴

대부분의 도구가 비슷한 옵션 패턴을 따름:

```bash
# 특정 PID
sudo execsnoop -p 1234

# 시간 제한
sudo profile 10            # 10초

# 출력 형식
sudo biolatency -m         # 밀리초 단위
sudo biolatency -P         # 프로세스별
```

#### `-h` 와 `man`

```bash
sudo execsnoop -h
man execsnoop
```

각 도구는 자체 매뉴얼 보유.

---

### Python으로 직접 작성

#### 가장 간단한 예제

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

#### 데이터 수집 예제

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

#### 매크로 단축

bcc는 BPF 코드 안에서 자체 매크로 제공:

- `BPF_HASH(name, key_type, value_type)`: hash map 선언
- `BPF_ARRAY(name, type, size)`: array map
- `BPF_PERCPU_ARRAY(...)`: percpu
- `BPF_HISTOGRAM(name)`: 히스토그램
- `BPF_PERF_OUTPUT(name)`: perf event array

이 매크로들은 컴파일 타임에 raw libbpf 선언으로 확장됨.

#### Tracepoint

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

`TRACEPOINT_PROBE` 매크로가 SEC 지정과 인자 구조체를 자동으로 처리.

#### Perf event 출력

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

### 주요 도구 카탈로그

#### 프로세스/시스템콜

- `execsnoop`: exec() 호출 (새 명령어)
- `exitsnoop`: 프로세스 종료
- `killsnoop`: kill() syscall
- `opensnoop`: 파일 열기
- `statsnoop`: stat()
- `syscount`: syscall 빈도

#### CPU/성능

- `profile`: CPU 스택 샘플링 (flame graph 데이터)
- `offcputime`: off-CPU 시간 (대기 분석)
- `runqlat`: 런 큐 지연
- `runqlen`: 런 큐 길이
- `cpudist`: CPU 사용 분포
- `funccount`: 함수 호출 빈도
- `funclatency`: 함수 지연
- `argdist`: 인자 분포

#### IO/디스크

- `biolatency`: 블록 IO 지연 히스토그램
- `biosnoop`: 블록 IO 트레이싱 (per IO)
- `biotop`: 블록 IO top
- `xfsslower` / `ext4slower`: 느린 파일시스템 op
- `dcsnoop` / `dcstat`: dentry 캐시
- `cachestat`: 페이지 캐시 통계

#### 네트워크

- `tcpconnect`: TCP active open
- `tcpaccept`: TCP passive open
- `tcpretrans`: TCP 재전송
- `tcptop`: TCP throughput per connection
- `tcplife`: TCP 연결 라이프타임
- `tcpdrop`: TCP drop 원인
- `sockstat`: 소켓 통계
- `udpconnect`: UDP 연결

#### 메모리

- `memleak`: 메모리 누수
- `oomkill`: OOM 킬러
- `slabratetop`: SLAB 할당률
- `mountsnoop`: mount/umount

#### 기타

- `trace`: 임의 함수 trace (강력)
- `argdist`: 함수 인자 분포
- `stackcount`: 함수 호출별 스택 카운트
- `dbslower`: MySQL/Postgres 느린 쿼리
- `gethostlatency`: DNS 해석 지연

---

### 성능 분석 워크플로우

#### 1. CPU가 많이 쓰일 때

```bash
sudo profile -F 99 -af 30 > out.stacks   # 30초간 스택 샘플
flamegraph.pl < out.stacks > flame.svg    # FlameGraph로 시각화
```

[Brendan Gregg의 FlameGraph](https://github.com/brendangregg/FlameGraph) 와 결합.

#### 2. 시스템이 느린데 CPU는 안 씀

```bash
sudo offcputime -df 30
```

프로세스가 어디서 대기하는지 확인 (mutex, IO, 네트워크 등).

#### 3. 디스크 IO 지연

```bash
sudo biolatency -m
sudo biosnoop
```

#### 4. 어느 함수가 느린지

```bash
sudo funclatency 'vfs_read'
sudo funclatency -p 1234 'do_*'
```

#### 5. 임의의 함수 디버깅

```bash
sudo trace 'do_sys_open "%s", arg2'
sudo trace -K 'tcp_v4_connect "%pK", arg1'   # 스택까지
```

#### 6. 시스템콜 빈도

```bash
sudo syscount -P -d 10
```

10초간 프로세스별 syscall 빈도 확인.

---

### 한계와 대안

#### bcc 한계

- LLVM/Clang 의존 (수 백 MB)
- 시작 시간 느림 (수 초)
- 메모리 사용 큼
- 프로덕션 상시 운영보다 ad-hoc 디버깅에 적합

#### libbpf-tools

bcc 도구 일부가 CO-RE 기반 libbpf로 재작성됨 → 더 빠르고 가벼움.

```bash
sudo apt install bcc-libbpf-tools
ls /usr/sbin/*-libbpf       # 또는 /usr/share/bcc/libbpf-tools/
```

같은 이름의 도구가 두 버전 있을 수 있음 (`opensnoop-bpfcc` vs libbpf-tools 버전).

#### bpftrace

원라이너 빠른 디버깅에는 bpftrace가 더 편함 (다음 챕터 참고).

#### 마이그레이션 가이드

- "학습/탐색" 용도 → bcc Python
- "원라이너" 용도 → bpftrace
- "프로덕션 도구" 용도 → libbpf + CO-RE
- "인프라 시스템" 용도 → Cilium, Falco, Tetragon

---

### 참고 자료

- [bcc GitHub](https://github.com/iovisor/bcc)
- [bcc tutorial](https://github.com/iovisor/bcc/blob/master/docs/tutorial.md)
- [bcc Reference Guide](https://github.com/iovisor/bcc/blob/master/docs/reference_guide.md)
- [Brendan Gregg: BPF Performance Tools](https://www.brendangregg.com/bpf-performance-tools-book.html)

---

## bpftrace

> 원본: https://github.com/bpftrace/bpftrace , https://bpftrace.org/

---

### 목차

1. [bpftrace란?](#bpftrace란)
2. [설치](#설치)
3. [Probe 종류](#probe-종류)
4. [내장 변수](#내장-변수)
5. [내장 함수](#내장-함수)
6. [맵과 집계](#맵과-집계)
7. [흐름 제어](#흐름-제어)
8. [실전 원라이너 모음](#실전-원라이너-모음)
9. [스크립트 파일](#스크립트-파일)
10. [참고 자료](#참고-자료)

---

### bpftrace란?

DTrace에서 영감을 받은 **고수준 트레이싱 언어**로, BPF 위에서 동작하며 짧은 스크립트로 복잡한 관측 표현 가능.

특징:
- 원라이너 친화 (CLI에서 즉석 트레이싱)
- AWK 스타일 문법 (probe / filter / action)
- 자동 메모리 관리
- 풍부한 내장 변수와 helper
- 100개 이상의 라이브러리 예제

---

### 설치

```bash
# Ubuntu/Debian
sudo apt install bpftrace

# RHEL/Fedora
sudo dnf install bpftrace

# macOS는 지원 안 함 (Linux 전용)
```

#### 권한

CAP_BPF, CAP_PERFMON 또는 root 권한 필요.

```bash
sudo bpftrace -e 'BEGIN { printf("hello\n"); }'
```

#### 가능한 probe 검색

```bash
sudo bpftrace -l 'tracepoint:syscalls:sys_enter*'
sudo bpftrace -l 'kprobe:tcp_*'
sudo bpftrace -l 'usdt:/usr/lib/libpython3.so:*'
```

#### 검증

```bash
sudo bpftrace -e 'BEGIN { exit(); }'
```

---

### Probe 종류

```
provider:target:[fn|tp]
```

#### kprobe / kretprobe

```
kprobe:vfs_read
kretprobe:vfs_read
```

#### uprobe / uretprobe

```
uprobe:/usr/lib/libc.so.6:malloc
uretprobe:/usr/local/bin/myapp:handle_request
```

#### tracepoint

```
tracepoint:syscalls:sys_enter_openat
tracepoint:sched:sched_switch
```

`args->필드` 형식으로 인자에 접근.

#### usdt

```
usdt:/usr/local/bin/myapp:provider:probe_name
```

#### profile / interval

```
profile:hz:99      # 모든 CPU에서 99Hz로 샘플링
interval:s:1       # 1초마다
```

#### software / hardware

```
software:cpu-clock:1000000   # CPU clock event
hardware:cache-misses:100000
```

#### BEGIN / END

```
BEGIN  { printf("starting\n"); }
END    { printf("done\n"); }
```

#### kfunc / kretfunc (BTF 기반, Linux 5.5+)

```
kfunc:vfs_read       # kprobe보다 빠르며 인자에 직접 접근 가능
```

```
kfunc:vfs_read {
    printf("file=%s count=%zu\n", str(args->file->f_path.dentry->d_name.name), args->count);
}
```

---

### 내장 변수

- `pid`: 현재 PID (TGID)
- `tid`: 스레드 ID
- `uid`, `gid`: UID, GID
- `comm`: 명령어 이름
- `cpu`: CPU 번호
- `ncpus`: CPU 수
- `nsecs`: nanosecond 타임스탬프
- `elapsed`: 프로그램 시작 후 ns
- `cgroup`: cgroup ID
- `args`: tracepoint/kfunc 인자 (`args->name`)
- `arg0..arg9`: kprobe/uprobe 인자 (정수)
- `retval`: kretprobe 반환값
- `func`: 현재 함수 이름
- `kstack`: 커널 스택
- `ustack`: 사용자 스택
- `probe`: 현재 probe 이름

#### 인자 접근

##### tracepoint

```
tracepoint:syscalls:sys_enter_openat {
    printf("filename=%s\n", str(args->filename));
}
```

(필드 목록은 `bpftrace -lv` 로 확인.)

##### kprobe

```
kprobe:vfs_read {
    printf("file=%p count=%lu\n", arg1, arg2);
}
```

##### kfunc (구조체로)

```
kfunc:vfs_read {
    printf("count=%zu\n", args->count);
}
```

---

### 내장 함수

#### 출력

- `printf("...", ...)`: C printf
- `print(@map)`: map 출력
- `time("...")` / `strftime("...", nsec)`: 시간 포맷
- `cat("/path")`: 파일 내용 출력
- `system("cmd")`: 외부 명령 실행 (느림 → 디버깅 용도 외에는 사용 비권장)

#### 메모리 접근

- `str(ptr)`: 문자열
- `str(ptr, len)`: 길이 제한 문자열
- `buf(ptr, size)`: hex 덤프용
- `kaddr("symbol")`: 커널 심볼 주소
- `uaddr("symbol")`: 유저 심볼 주소
- `usym(addr)`: 유저 심볼 이름
- `ksym(addr)`: 커널 심볼 이름

#### 시간

- `nsecs`: 현재 ns
- `elapsed`: 시작 후 ns

#### 스택

- `kstack`: 커널 스택 (변수처럼)
- `kstack(N)`: 깊이 N
- `ustack`: 유저 스택
- `ustack(N, mode)`: mode는 `perf` 또는 `bpftrace`

#### 데이터 변환

- `count()`: 1 증가 (집계)
- `sum(x)`: 누적 합
- `avg(x)`: 평균
- `min(x)`, `max(x)`: 최소/최대
- `hist(x)`: 로그 스케일 히스토그램
- `lhist(x, min, max, step)`: 선형 히스토그램
- `stats(x)`: count + avg + total

---

### 맵과 집계

맵은 `@` 으로 시작:

#### 단순 카운터

```
tracepoint:syscalls:sys_enter_openat {
    @opens[comm] = count();
}
```

프로그램 종료 시 자동으로 출력됨. 수동으로 제어하려면:

```
END {
    print(@opens);
    clear(@opens);
}
```

#### 다차원 키

```
@latency[comm, pid] = hist(nsecs - @start);
```

#### 히스토그램

```
kprobe:vfs_read {
    @start[tid] = nsecs;
}

kretprobe:vfs_read /@start[tid]/ {
    @ns = hist(nsecs - @start[tid]);
    delete(@start[tid]);
}
```

출력:
```
@ns:
[256, 512)             3 |@                                     |
[512, 1K)              7 |@@@                                   |
[1K, 2K)             21 |@@@@@@@@                              |
[2K, 4K)            134 |@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@|
[4K, 8K)             86 |@@@@@@@@@@@@@@@@@@@@@@@@              |
```

#### Top N

```
END {
    print(@opens, 10);     // top 10
}
```

---

### 흐름 제어

#### Filter (predicate)

```
kprobe:vfs_read /pid == 1234/ {
    printf("read by target\n");
}

kprobe:vfs_open /comm == "myapp"/ {
    @[args->path] = count();
}
```

#### if/else

```
kprobe:vfs_read {
    if (arg2 > 1024) {
        @big_reads = count();
    } else {
        @small_reads = count();
    }
}
```

#### 변수

스칼라 변수는 `$` 접두사 사용:

```
{
    $size = arg2;
    if ($size > 0) @hist = hist($size);
}
```

전역 맵은 `@`을 사용.

#### exit

```
profile:hz:99 {
    @[ustack] = count();
}

interval:s:30 {
    exit();
}
```

30초 후 자동 종료.

---

### 실전 원라이너 모음

#### 프로세스 추적

```bash
# 새 프로세스 (exec)
sudo bpftrace -e 'tracepoint:sched:sched_process_exec { printf("%s pid=%d\n", str(args->filename), pid); }'

# 프로세스 종료
sudo bpftrace -e 'tracepoint:sched:sched_process_exit { printf("%s pid=%d exit\n", comm, pid); }'
```

#### 파일 시스템

```bash
# open() 추적
sudo bpftrace -e 'tracepoint:syscalls:sys_enter_openat { printf("%s %s\n", comm, str(args->filename)); }'

# 큰 read만
sudo bpftrace -e 'kprobe:vfs_read /arg2 > 1048576/ { printf("%s big read %lu\n", comm, arg2); }'

# 파일별 read 통계
sudo bpftrace -e 'kprobe:vfs_read { @[comm] = count(); }'
```

#### 시스템 콜

```bash
# 가장 많이 호출된 syscall
sudo bpftrace -e 'tracepoint:raw_syscalls:sys_enter { @[probe] = count(); }'

# 특정 프로세스의 syscall
sudo bpftrace -e 'tracepoint:raw_syscalls:sys_enter /pid == 1234/ { @[args->id] = count(); }'

# 실패한 syscall (errno)
sudo bpftrace -e 'tracepoint:raw_syscalls:sys_exit /args->ret < 0/ { @[args->id, -args->ret] = count(); }'
```

#### 네트워크

```bash
# 새 TCP 연결
sudo bpftrace -e 'kprobe:tcp_v4_connect { printf("%s connect\n", comm); }'

# TCP 재전송
sudo bpftrace -e 'kprobe:tcp_retransmit_skb { printf("%s retx\n", comm); }'
```

#### 성능

```bash
# 99Hz CPU 프로파일링
sudo bpftrace -e 'profile:hz:99 { @[ustack, comm] = count(); }'

# Off-CPU 시간
sudo bpftrace -e '
kprobe:finish_task_switch { @start[arg0] = nsecs; }
kretprobe:finish_task_switch /@start[arg0]/ { @[comm] = sum(nsecs - @start[arg0]); delete(@start[arg0]); }'

# vfs_read 지연 분포
sudo bpftrace -e '
kprobe:vfs_read { @start[tid] = nsecs; }
kretprobe:vfs_read /@start[tid]/ { @ns = hist(nsecs - @start[tid]); delete(@start[tid]); }'
```

#### 메모리

```bash
# malloc 호출 빈도
sudo bpftrace -e 'uprobe:/usr/lib/libc.so.6:malloc { @[comm] = count(); }'

# 큰 malloc만
sudo bpftrace -e 'uprobe:/usr/lib/libc.so.6:malloc /arg0 > 1048576/ { printf("%s malloc(%lu)\n", comm, arg0); }'
```

---

### 스크립트 파일

긴 트레이싱 스크립트는 `.bt` 파일로 작성:

```bpftrace
#!/usr/bin/env bpftrace

BEGIN {
    printf("Tracing TCP connections... Hit Ctrl-C to end.\n");
    printf("%-10s %-12s %-16s %-16s\n", "PID", "COMM", "SADDR", "DADDR");
}

kprobe:tcp_v4_connect {
    @sk[tid] = arg0;
}

kretprobe:tcp_v4_connect /@sk[tid]/ {
    if (retval == 0) {
        $sk = (struct sock *)@sk[tid];
        $sport = $sk->__sk_common.skc_num;
        $dport = bswap_16($sk->__sk_common.skc_dport);
        $saddr = $sk->__sk_common.skc_rcv_saddr;
        $daddr = $sk->__sk_common.skc_daddr;
        printf("%-10d %-12s %-16s:%-5d %-16s:%-5d\n",
               pid, comm, ntop($saddr), $sport, ntop($daddr), $dport);
    }
    delete(@sk[tid]);
}

END {
    clear(@sk);
}
```

실행:

```bash
chmod +x tcpconn.bt
sudo ./tcpconn.bt
```

#### 라이브러리

`/usr/share/bpftrace/tools/` 에 100개 이상의 예제 스크립트 존재.

```bash
ls /usr/share/bpftrace/tools/
sudo /usr/share/bpftrace/tools/tcpconnect.bt
sudo /usr/share/bpftrace/tools/biolatency.bt
sudo /usr/share/bpftrace/tools/runqlat.bt
```

---

### 참고 자료

- [bpftrace GitHub](https://github.com/bpftrace/bpftrace)
- [bpftrace 공식 사이트](https://bpftrace.org/)
- [bpftrace one-liners (Brendan Gregg)](https://github.com/bpftrace/bpftrace/blob/master/docs/tutorial_one_liners.md)
- [bpftrace reference](https://bpftrace.org/learn-ebpf/)

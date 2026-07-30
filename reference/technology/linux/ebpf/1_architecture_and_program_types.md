# BPF 아키텍처, Verifier, Program Types

## BPF 아키텍처와 Verifier

> 원본: https://docs.kernel.org/bpf/instruction-set.html , https://docs.kernel.org/bpf/verifier.html

---

### 목차

1. [BPF 가상머신](#bpf-가상머신)
2. [명령어 셋](#명령어-셋)
3. [레지스터와 ABI](#레지스터와-abi)
4. [Verifier](#verifier)
5. [JIT 컴파일러](#jit-컴파일러)
6. [Bounded Loop](#bounded-loop)
7. [Tail Call](#tail-call)
8. [BPF-to-BPF Function Call](#bpf-to-bpf-function-call)
9. [참고 자료](#참고-자료)

---

### BPF 가상머신

eBPF는 64비트 RISC 풍 가상머신 명령어 셋을 정의합니다.

#### 특징

- 11개의 64비트 레지스터 (R0~R10)
- 512바이트 스택
- 무한 루프 불가 (또는 bounded)
- 무제한 코드 크기 (이전 4096 → 현재 100만 명령어)
- 안전한 메모리 접근만

#### 실행 흐름

```
사용자 공간
   │
   ├─ 1. C로 작성 → Clang -target bpf → ELF 오브젝트
   │
   └─ 2. bpf(BPF_PROG_LOAD, ...) syscall
        │
커널 공간 ↓
        ├─ 3. Verifier 검사
        ├─ 4. JIT 컴파일 (x86_64, arm64, ...)
        └─ 5. attach (kprobe, XDP, ...)
        │
이벤트 발생 시 ↓
        └─ 6. JIT된 네이티브 코드 실행
```

---

### 명령어 셋

64비트 명령어. 각 명령어는 8바이트 (opcode 1B + dst/src 1B + offset 2B + immediate 4B).

#### 주요 명령어 클래스

| 클래스 | 의미 | 예 |
| --- | --- | --- |
| `BPF_LD` / `BPF_LDX` | 메모리 로드 | `r1 = *(u32 *)(r10 - 4)` |
| `BPF_ST` / `BPF_STX` | 메모리 저장 | `*(u64 *)(r10 - 8) = r2` |
| `BPF_ALU` / `BPF_ALU64` | 산술/논리 (32/64비트) | `r1 += r2` |
| `BPF_JMP` / `BPF_JMP32` | 조건/무조건 분기 | `if r1 == 0 goto +5` |
| `BPF_CALL` | helper/BPF 함수 호출 | `call bpf_get_current_pid_tgid` |
| `BPF_EXIT` | 프로그램 종료, R0가 반환값 | |

#### 메모리 접근 크기

`B`(8b), `H`(16b), `W`(32b), `DW`(64b).

#### 예: ALU64 더하기

```
0x07 R1 += imm    (ALU64 ADD imm)
0x0f R1 += R2     (ALU64 ADD reg)
```

직접 작성할 일은 거의 없으며, Clang이 자동으로 생성합니다.

---

### 레지스터와 ABI

#### 11개 64비트 레지스터

| 레지스터 | 용도 |
| --- | --- |
| R0 | 반환값 (BPF_EXIT의 결과, helper 반환) |
| R1~R5 | 함수 인자 (caller-saved) |
| R6~R9 | callee-saved |
| R10 | 스택 포인터 (read-only) |

#### Calling Convention

helper나 BPF 함수 호출 시:
- 인자: R1~R5
- 반환: R0
- R6~R9는 callee가 보존

#### 스택

- 크기: 512바이트
- R10이 가리킴 (감소 방향 사용)
- 스택 메모리 접근은 verifier가 정확히 추적

---

### Verifier

eBPF의 안전성을 보장하는 핵심 컴포넌트. 모든 BPF 프로그램은 verifier를 통과해야 로드됩니다.

#### 검증 항목

1. **DAG 분석**: 프로그램이 종료되는지 확인 (모든 경로가 BPF_EXIT에 도달하도록 보장)
2. **레지스터 상태 추적**: 각 명령어 실행 후 모든 레지스터의 가능한 값 범위
3. **메모리 안전성**: 포인터 역참조 시 그 영역이 valid한지
4. **헬퍼 호출**: 인자 타입과 컨텍스트가 올바른지
5. **권한**: 프로그램 타입이 호출하려는 helper에 접근 가능한지

#### 레지스터 타입 추적

verifier는 각 레지스터를 다음 타입 중 하나로 추적:

- `SCALAR_VALUE`: 일반 정수
- `PTR_TO_STACK`: 스택 영역 포인터
- `PTR_TO_PACKET`: 패킷 데이터 포인터
- `PTR_TO_MAP_VALUE`: map 값 포인터
- `PTR_TO_CTX`: 컨텍스트 (각 프로그램 타입마다 다른 구조체)
- `NOT_INIT`: 초기화 안 됨
- ...

각 타입은 특정 연산만 허용되며, 잘못된 사용은 거부됩니다.

#### 값 범위 추적

```c
if (r1 < 100) {
    // 이 분기 안에서 verifier는 r1의 범위를 [0, 99]로 압니다
    r2 = arr[r1];   // 배열 크기 100이면 안전. verifier 통과.
}
```

이 추적 덕분에 사용자가 명시적으로 범위 검사(bound check)만 해두면 verifier가 메모리 안전성을 자동으로 검증합니다.

#### 검증 한도

- 명령어 수: 최대 100만 (5.x+)
- 검증 복잡도: 분기 폭발 방지를 위해 전체 분석 횟수에 상한을 둠

복잡한 프로그램이 거부되면 흔한 메시지:
```
math between map_value pointer and register with unbounded ...
verifier reaches one million instructions limit
```

#### Verifier 로그 보기

```bash
bpftool prog load my.o /sys/fs/bpf/myprog \
  type kprobe \
  log_level 2
```

`log_level 2` 면 모든 명령어와 레지스터 상태를 출력. 디버깅 필수.

---

### JIT 컴파일러

검증을 통과한 BPF 바이트코드는 **JIT 컴파일러**가 호스트 아키텍처의 네이티브 코드로 변환합니다.

지원 아키텍처: x86_64, arm64, riscv64, ppc64, s390x, mips, sparc64.

#### 활성화 확인

```bash
sysctl net.core.bpf_jit_enable
# 1: 활성, 2: 디버그 정보 포함
```

#### JIT vs 인터프리터

- JIT 활성: 거의 네이티브 속도 (네이티브 코드의 90~100%)
- 인터프리터: 5~10배 느림. 임베디드/구식 시스템에서만

#### 검사

```bash
sudo bpftool prog dump xlated id <prog-id>      # 검증 후 BPF
sudo bpftool prog dump jited id <prog-id>       # JIT된 native asm
```

---

### Bounded Loop

원래 BPF는 루프를 전혀 허용하지 않았지만, Linux 5.3에서 **bounded loop** 가 추가되었습니다.

```c
int sum = 0;
for (int i = 0; i < 10; i++) {     // i가 10보다 작음을 verifier가 추적
    sum += array[i];
}
```

verifier가 루프 종료를 증명할 수 있으면 통과합니다. 증명할 수 없는 패턴(예: 사용자 입력 값에 의존하는 루프 종료 조건)은 거부됩니다.

#### bpf_loop helper (5.17+)

```c
bpf_loop(100, callback, ctx, 0);
```

verifier 부담을 줄여주는 명시적 루프 helper. callback을 N번 호출하며, 더 큰 루프를 처리할 수 있습니다.

---

### Tail Call

한 BPF 프로그램이 **다른 BPF 프로그램으로 점프**하는 메커니즘. JS의 `tail call`과 비슷.

```c
SEC("xdp/dispatcher")
int dispatcher(struct xdp_md *ctx) {
    int key = classify(ctx);
    bpf_tail_call(ctx, &progs, key);
    return XDP_PASS;
}
```

`progs` 는 `BPF_MAP_TYPE_PROG_ARRAY` 타입의 map. 인덱스마다 다른 BPF 프로그램이 등록됨.

#### 특징

- 점프 후 원래 함수로 돌아오지 않음
- 스택은 그대로 유지
- 최대 33회 chain (4.2+)
- 큰 프로그램을 작은 모듈로 나누는 용도

#### 활용

- XDP 패킷 분류기 (프로토콜별 다른 BPF)
- iptables-like 룰 체인
- BPF 프로그램의 동적 디스패치

---

### BPF-to-BPF Function Call

5.x 이후 BPF 프로그램 안에서 **함수를 정의하고 호출**할 수 있습니다. tail call과 달리 정상적인 호출-리턴 방식입니다.

```c
static __always_inline int helper(int x) {
    return x * 2;
}

SEC("kprobe/sys_open")
int trace_open(void *ctx) {
    int v = helper(42);
    bpf_printk("value=%d\n", v);
    return 0;
}
```

이전에는 `__always_inline`으로 인라인만 가능했지만, 이제 일반 함수 호출도 지원합니다. JIT가 실제 함수로 컴파일합니다.

#### 한계

- 재귀 불가
- 콜 스택 깊이 제한 (8 호출 단계)
- 현재 일부 프로그램 타입에서만 지원

---

### 참고 자료

- [BPF instruction set](https://docs.kernel.org/bpf/instruction-set.html)
- [BPF verifier](https://docs.kernel.org/bpf/verifier.html)
- [Cilium: Linux Kernel BPF guide](https://docs.cilium.io/en/stable/bpf/)
- [LWN: BPF in-kernel verifier](https://lwn.net/Articles/794934/)

---

## BPF Program Types

> 원본: https://docs.kernel.org/bpf/libbpf/program_types.html

---

### 목차

1. [Program Type이란?](#program-type이란)
2. [Tracing 계열](#tracing-계열)
3. [Networking 계열](#networking-계열)
4. [보안 계열](#보안-계열)
5. [Cgroup 계열](#cgroup-계열)
6. [기타](#기타)
7. [Section 이름 컨벤션](#section-이름-컨벤션)
8. [참고 자료](#참고-자료)

---

### Program Type이란?

각 BPF 프로그램은 **하나의 program type** 을 가지며, 이는:
- 어디에 attach할 수 있는지 (hook 종류)
- 컨텍스트(`ctx`) 인자의 구조체 타입
- 사용 가능한 helper 함수 셋
- 반환값의 의미

를 결정합니다.

소스 코드에서는 SEC() 매크로로 type을 표현:

```c
SEC("kprobe/sys_open")            // BPF_PROG_TYPE_KPROBE
SEC("xdp")                         // BPF_PROG_TYPE_XDP
SEC("tracepoint/syscalls/sys_enter_open")
SEC("cgroup/skb")
SEC("lsm/file_open")
```

---

### Tracing 계열

#### KPROBE (`BPF_PROG_TYPE_KPROBE`)

**커널 함수의 진입/종료 지점**에 부착. 가장 자유로운 트레이싱 방법이지만 함수 시그니처에 의존하므로 커널 버전 변경에 취약.

```c
SEC("kprobe/sys_clone")
int trace_clone(struct pt_regs *ctx) {
    pid_t pid = bpf_get_current_pid_tgid() >> 32;
    bpf_printk("clone called by pid %d", pid);
    return 0;
}
```

`kretprobe` 는 함수 종료 시점.

#### TRACEPOINT (`BPF_PROG_TYPE_TRACEPOINT`)

**커널 정적 트레이스 포인트** 에 부착. 안정적인 ABI.

```c
SEC("tracepoint/syscalls/sys_enter_openat")
int trace_open(struct trace_event_raw_sys_enter *ctx) {
    char *filename = (char *)ctx->args[1];
    bpf_printk("open: %s", filename);
    return 0;
}
```

가능한 tracepoint 목록:
```bash
sudo find /sys/kernel/debug/tracing/events -name format
```

#### RAW_TRACEPOINT (`BPF_PROG_TYPE_RAW_TRACEPOINT`)

tracepoint와 비슷하지만 커널이 인자를 가공하지 않은 raw 형태. 더 빠르지만 약간 더 복잡.

#### FENTRY/FEXIT (`BPF_PROG_TYPE_TRACING`)

5.5+ 추가. kprobe보다 **5~10배 빠름**. BTF 기반으로 함수 시그니처를 정확히 알아 효율적.

```c
SEC("fentry/vfs_read")
int BPF_PROG(read_entry, struct file *file, char *buf, size_t count) {
    bpf_printk("vfs_read: count=%zu", count);
    return 0;
}

SEC("fexit/vfs_read")
int BPF_PROG(read_exit, struct file *file, char *buf, size_t count, ssize_t ret) {
    bpf_printk("vfs_read returned %zd", ret);
    return 0;
}
```

#### UPROBE (`BPF_PROG_TYPE_KPROBE` 의 사용자 공간 변형)

**사용자 공간 함수** 에 부착. 자신의 애플리케이션이나 라이브러리(libc 등) 트레이싱.

```c
SEC("uprobe//usr/lib/libc.so.6:malloc")
int trace_malloc(struct pt_regs *ctx) {
    size_t size = PT_REGS_PARM1(ctx);
    bpf_printk("malloc(%zu)", size);
    return 0;
}
```

#### USDT (User Statically-Defined Tracing)

애플리케이션이 명시적으로 정의한 트레이스 포인트(MySQL, Postgres, Python, Java 등 많은 프로젝트가 USDT probe를 가짐). bpftrace에서 `usdt:...` 로 사용.

#### PERF_EVENT (`BPF_PROG_TYPE_PERF_EVENT`)

perf event(하드웨어 카운터, 소프트웨어 이벤트)에 부착. 프로파일링.

```c
SEC("perf_event")
int profile(struct bpf_perf_event_data *ctx) {
    u64 ip = PT_REGS_IP(&ctx->regs);
    // 스택 트레이스 등
    return 0;
}
```

---

### Networking 계열

#### XDP (`BPF_PROG_TYPE_XDP`)

**eXpress Data Path**. NIC 드라이버 단계(또는 generic XDP, hardware offload). 패킷이 OS 스택에 들어오기 전에 처리.

```c
SEC("xdp")
int xdp_drop_icmp(struct xdp_md *ctx) {
    void *data = (void *)(long)ctx->data;
    void *data_end = (void *)(long)ctx->data_end;
    struct ethhdr *eth = data;

    if ((void *)(eth + 1) > data_end) return XDP_PASS;
    if (eth->h_proto != bpf_htons(ETH_P_IP)) return XDP_PASS;

    struct iphdr *ip = (void *)(eth + 1);
    if ((void *)(ip + 1) > data_end) return XDP_PASS;

    if (ip->protocol == IPPROTO_ICMP) return XDP_DROP;
    return XDP_PASS;
}
```

반환값:
- `XDP_DROP`: 패킷 폐기
- `XDP_PASS`: 다음 단계로
- `XDP_TX`: 같은 NIC로 재전송
- `XDP_REDIRECT`: 다른 NIC/AF_XDP 소켓으로
- `XDP_ABORTED`: 에러

용도: DDoS 방어, 로드밸런싱, 패킷 미러링.

#### TC (Traffic Control) (`BPF_PROG_TYPE_SCHED_CLS`)

`tc` qdisc/filter에 부착. ingress와 egress 모두 처리. XDP보다 늦은 단계지만 더 풍부한 컨텍스트.

```c
SEC("tc")
int tc_filter(struct __sk_buff *skb) {
    if (skb->protocol == bpf_htons(ETH_P_IPV6))
        return TC_ACT_SHOT;     // drop
    return TC_ACT_OK;
}
```

용도: 컨테이너 네트워킹 (Cilium), QoS, 로드밸런싱.

#### Socket Filter (`BPF_PROG_TYPE_SOCKET_FILTER`)

cBPF(classic BPF)에서 이어진 원형 소켓 패킷 필터. tcpdump/Wireshark가 사용.

#### Sock Ops (`BPF_PROG_TYPE_SOCK_OPS`)

소켓 라이프사이클 이벤트 (`TCP_CONNECT`, `STATE_CHANGE` 등)에 hook. TCP congestion 제어 변경, 라우팅 결정.

#### Socket Map / Sockmap (`BPF_PROG_TYPE_SK_MSG`, `SK_SKB`)

소켓 redirect. msg/skb를 다른 소켓으로 전달. 사이드카 프록시 우회 패턴.

#### LWT (Lightweight Tunnel)

라우팅 결정 후 인캡슐레이션 등에 hook.

#### Cgroup Skb (`BPF_PROG_TYPE_CGROUP_SKB`)

cgroup 단위 패킷 필터링. 컨테이너 정책에 활용.

---

### 보안 계열

#### LSM (`BPF_PROG_TYPE_LSM`)

5.7+ 도입. **Linux Security Module** hook을 BPF로 구현. SELinux/AppArmor 같은 정책을 BPF로 작성 가능.

```c
SEC("lsm/file_open")
int BPF_PROG(check_file_open, struct file *file) {
    if (some_condition(file))
        return -EPERM;     // 거부
    return 0;              // 허용
}
```

#### Cgroup Sock (`BPF_PROG_TYPE_CGROUP_SOCK_*`)

cgroup 단위 소켓 정책. `connect()`, `bind()`, `sendmsg()`, `getsockopt()` 등에 hook.

#### Seccomp (BPF_PROG_TYPE_SOCKET_FILTER 변형)

syscall 필터링. classic BPF 호환 형식이지만 커널 안의 실행 메커니즘은 eBPF.

```c
// seccomp BPF 프로그램 (cBPF 형식)
struct sock_filter filter[] = {
    BPF_STMT(BPF_LD|BPF_W|BPF_ABS, offsetof(struct seccomp_data, nr)),
    BPF_JUMP(BPF_JMP|BPF_JEQ|BPF_K, __NR_open, 0, 1),
    BPF_STMT(BPF_RET|BPF_K, SECCOMP_RET_KILL),
    BPF_STMT(BPF_RET|BPF_K, SECCOMP_RET_ALLOW),
};
```

systemd의 `SystemCallFilter=` 가 내부적으로 이를 사용합니다.

---

### Cgroup 계열

| 타입 | 의미 |
| --- | --- |
| `CGROUP_SKB` | cgroup 패킷 ingress/egress |
| `CGROUP_SOCK` | 소켓 생성 시 |
| `CGROUP_DEVICE` | device cgroup 정책 |
| `CGROUP_SOCK_ADDR` | bind/connect/sendmsg 주소 |
| `CGROUP_SOCKOPT` | setsockopt/getsockopt |
| `CGROUP_SYSCTL` | sysctl 접근 정책 |

cgroup에 attach하면 해당 cgroup 내 모든 프로세스에 정책이 적용됩니다. Kubernetes pod 단위 정책 등에 활용.

---

### 기타

#### KSYSCALL (`BPF_PROG_TYPE_KPROBE` 변형)

5.16+. syscall에 보다 깔끔하게 hook. ABI 차이를 자동으로 처리.

```c
SEC("ksyscall/openat")
int trace_openat(struct pt_regs *ctx, int dirfd, const char *pathname) {
    // ...
}
```

#### Sched (Scheduler)

스케줄러 결정에 hook. 5.x 후반 기준 실험적 기능.

#### NETFILTER

netfilter hook (5.13+). iptables의 BPF 대안.

#### Iter (Map Iterator)

map의 모든 엔트리를 순회하며 BPF 프로그램으로 처리. 효율적인 통계 추출에 활용.

---

### Section 이름 컨벤션

`SEC("...")` 의 형식이 program type을 결정합니다. libbpf가 인식하는 주요 패턴:

| 패턴 | Type |
| --- | --- |
| `kprobe/<func>` | KPROBE |
| `kretprobe/<func>` | KPROBE (retprobe) |
| `uprobe/<binary>:<func>` | KPROBE (user) |
| `tracepoint/<category>/<name>` | TRACEPOINT |
| `tp/<category>/<name>` | TRACEPOINT (단축) |
| `raw_tp/<name>` | RAW_TRACEPOINT |
| `fentry/<func>` | TRACING fentry |
| `fexit/<func>` | TRACING fexit |
| `lsm/<hook>` | LSM |
| `xdp` | XDP |
| `tc` | SCHED_CLS |
| `cgroup/skb` | CGROUP_SKB |
| `cgroup/connect4` | CGROUP_SOCK_ADDR |
| `perf_event` | PERF_EVENT |
| `socket` | SOCKET_FILTER |
| `sk_msg` | SK_MSG |
| `sockops` | SOCK_OPS |

---

### 참고 자료

- [libbpf program types](https://docs.kernel.org/bpf/libbpf/program_types.html)
- [Cilium BPF program types](https://docs.cilium.io/en/stable/bpf/progtypes/)
- [Brendan Gregg: Linux extended BPF tracing tools](https://www.brendangregg.com/ebpf.html)

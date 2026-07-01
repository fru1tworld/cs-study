# BPF Program Types

> 원본: https://docs.kernel.org/bpf/libbpf/program_types.html

---

## 목차

1. [Program Type이란?](#program-type이란)
2. [Tracing 계열](#tracing-계열)
3. [Networking 계열](#networking-계열)
4. [보안 계열](#보안-계열)
5. [Cgroup 계열](#cgroup-계열)
6. [기타](#기타)
7. [Section 이름 컨벤션](#section-이름-컨벤션)
8. [참고 자료](#참고-자료)

---

## Program Type이란?

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

## Tracing 계열

### KPROBE (`BPF_PROG_TYPE_KPROBE`)

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

### TRACEPOINT (`BPF_PROG_TYPE_TRACEPOINT`)

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

### RAW_TRACEPOINT (`BPF_PROG_TYPE_RAW_TRACEPOINT`)

tracepoint와 비슷하지만 커널이 인자를 가공하지 않은 raw 형태. 더 빠르지만 약간 더 복잡.

### FENTRY/FEXIT (`BPF_PROG_TYPE_TRACING`)

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

### UPROBE (`BPF_PROG_TYPE_KPROBE` 의 사용자 공간 변형)

**사용자 공간 함수** 에 부착. 자신의 애플리케이션이나 라이브러리(libc 등) 트레이싱.

```c
SEC("uprobe//usr/lib/libc.so.6:malloc")
int trace_malloc(struct pt_regs *ctx) {
    size_t size = PT_REGS_PARM1(ctx);
    bpf_printk("malloc(%zu)", size);
    return 0;
}
```

### USDT (User Statically-Defined Tracing)

애플리케이션이 명시적으로 정의한 트레이스 포인트(MySQL, Postgres, Python, Java 등 많은 프로젝트가 USDT probe를 가짐). bpftrace에서 `usdt:...` 로 사용.

### PERF_EVENT (`BPF_PROG_TYPE_PERF_EVENT`)

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

## Networking 계열

### XDP (`BPF_PROG_TYPE_XDP`)

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

### TC (Traffic Control) (`BPF_PROG_TYPE_SCHED_CLS`)

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

### Socket Filter (`BPF_PROG_TYPE_SOCKET_FILTER`)

cBPF(classic BPF)에서 이어진 원형 소켓 패킷 필터. tcpdump/Wireshark가 사용.

### Sock Ops (`BPF_PROG_TYPE_SOCK_OPS`)

소켓 라이프사이클 이벤트 (`TCP_CONNECT`, `STATE_CHANGE` 등)에 hook. TCP congestion 제어 변경, 라우팅 결정.

### Socket Map / Sockmap (`BPF_PROG_TYPE_SK_MSG`, `SK_SKB`)

소켓 redirect. msg/skb를 다른 소켓으로 전달. 사이드카 프록시 우회 패턴.

### LWT (Lightweight Tunnel)

라우팅 결정 후 인캡슐레이션 등에 hook.

### Cgroup Skb (`BPF_PROG_TYPE_CGROUP_SKB`)

cgroup 단위 패킷 필터링. 컨테이너 정책에 활용.

---

## 보안 계열

### LSM (`BPF_PROG_TYPE_LSM`)

5.7+ 도입. **Linux Security Module** hook을 BPF로 구현. SELinux/AppArmor 같은 정책을 BPF로 작성 가능.

```c
SEC("lsm/file_open")
int BPF_PROG(check_file_open, struct file *file) {
    if (some_condition(file))
        return -EPERM;     // 거부
    return 0;              // 허용
}
```

### Cgroup Sock (`BPF_PROG_TYPE_CGROUP_SOCK_*`)

cgroup 단위 소켓 정책. `connect()`, `bind()`, `sendmsg()`, `getsockopt()` 등에 hook.

### Seccomp (BPF_PROG_TYPE_SOCKET_FILTER 변형)

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

## Cgroup 계열

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

## 기타

### KSYSCALL (`BPF_PROG_TYPE_KPROBE` 변형)

5.16+. syscall에 보다 깔끔하게 hook. ABI 차이를 자동으로 처리.

```c
SEC("ksyscall/openat")
int trace_openat(struct pt_regs *ctx, int dirfd, const char *pathname) {
    // ...
}
```

### Sched (Scheduler)

스케줄러 결정에 hook. 5.x 후반 기준 실험적 기능.

### NETFILTER

netfilter hook (5.13+). iptables의 BPF 대안.

### Iter (Map Iterator)

map의 모든 엔트리를 순회하며 BPF 프로그램으로 처리. 효율적인 통계 추출에 활용.

---

## Section 이름 컨벤션

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

## 참고 자료

- [libbpf program types](https://docs.kernel.org/bpf/libbpf/program_types.html)
- [Cilium BPF program types](https://docs.cilium.io/en/stable/bpf/progtypes/)
- [Brendan Gregg: Linux extended BPF tracing tools](https://www.brendangregg.com/ebpf.html)

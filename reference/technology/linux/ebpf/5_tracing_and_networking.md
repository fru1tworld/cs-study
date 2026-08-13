# Tracing·Observability와 Networking (XDP, TC)

## Tracing과 Observability 활용

---

### 목차

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

### eBPF 관측의 강점

전통 도구(`strace`, `gdb`, `perf trace`) 대비:

- **저비용**: kprobe/uprobe overhead가 매우 낮아 프로덕션에서 상시 가능
- **저지연**: 컨텍스트 스위치 없이 커널 안에서 즉시 처리
- **선택적 캡처**: BPF 코드로 필터링 후 필요한 것만 사용자 공간으로
- **풍부한 컨텍스트**: PID/TID/UID/cgroup/SELinux 등 신뢰 가능한 메타데이터
- **안전성**: verifier가 시스템 안정성 보장

`strace -p`는 ptrace 기반이라 syscall마다 컨텍스트 스위치가 두 번 발생함(수십 배 느림) → eBPF는 같은 작업을 거의 무비용으로 처리함.

---

### Tracing 종류와 선택

- **Static Tracepoint**
  - 안정성: 안정 ABI
  - 오버헤드: 매우 낮음
  - 추천 용도: 첫 선택지
- **kfunc/fentry**
  - 안정성: BTF 기반, 안정
  - 오버헤드: 가장 낮음(5.5+)
  - 추천 용도: 새 코드
- **kprobe**
  - 안정성: 함수 시그니처 의존
  - 오버헤드: 낮음
  - 추천 용도: tracepoint 없는 함수
- **kretprobe**
  - 안정성: 함수 시그니처 의존
  - 오버헤드: 약간 높음
  - 추천 용도: 함수 종료/지연 측정
- **USDT**
  - 안정성: 애플리케이션 ABI
  - 오버헤드: 낮음
  - 추천 용도: 애플리케이션 내부 이벤트
- **uprobe**
  - 안정성: 함수 시그니처 의존
  - 오버헤드: 중간
  - 추천 용도: 자기 코드 트레이싱

#### 선택 가이드

1. tracepoint가 있으면 그걸 사용 → ABI 안정
2. fentry/kfunc 가능하면 그걸 사용 → 가장 빠름
3. 안정 ABI가 없으면 kprobe 사용
4. 사용자 공간 라이브러리는 USDT > uprobe 순으로 우선 적용

#### tracepoint 찾기

```bash
sudo bpftrace -l 'tracepoint:*' | head -50
sudo find /sys/kernel/debug/tracing/events -maxdepth 2 -type d | head
```

#### tracepoint 인자

```bash
$ sudo bpftrace -lv 'tracepoint:syscalls:sys_enter_openat'
tracepoint:syscalls:sys_enter_openat
    int dfd
    const char * filename
    int flags
    umode_t mode
```

---

### CPU 프로파일링

#### 99Hz 스택 샘플링

```bash
sudo bpftrace -e '
profile:hz:99 {
    @[ustack, kstack, comm] = count();
}
interval:s:30 {
    exit();
}'
```

99 Hz 사용 이유: 100 Hz와 자연스럽게 어긋나 lockstep 샘플링 편향 회피 가능.

#### Flame Graph

```bash
# bcc 도구
sudo profile -af 30 > out.stacks

# FlameGraph 변환
git clone https://github.com/brendangregg/FlameGraph
./FlameGraph/flamegraph.pl < out.stacks > flame.svg

xdg-open flame.svg
```

각 박스의 너비는 해당 함수가 CPU를 점유한 시간 비율을 나타냄 → 가장 넓은 박스가 hot path.

#### Continuous profiling

프로덕션에서 상시 동작하는 프로파일러:
- **Parca** — Polar Signals
- **Pyroscope** — Grafana
- **Perforator** — Yandex
- **Pixie** — New Relic

위 도구들은 모두 eBPF profile probe를 기반으로 동작함.

---

### Off-CPU 분석

CPU를 사용하지 않는데도 시스템이 느리다면 → 프로세스가 어디서 대기하는지 파악이 핵심.

#### offcputime

```bash
sudo offcputime -df 30 > out.stacks
./FlameGraph/flamegraph.pl --bgcolors=blue < out.stacks > offcpu-flame.svg
```

스택의 각 박스 너비는 해당 코드 경로에서 대기한 시간을 나타냄.

#### bpftrace로

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

#### 결합

CPU + Off-CPU를 합산하면 전체 시간이 어디에 쓰이는지(CPU 작업 vs 대기)를 한눈에 파악 가능.

---

### 지연 분석

#### 함수 지연 분포

```bash
sudo funclatency 'vfs_read'
sudo funclatency -p 1234 'tcp_*'
```

#### syscall 지연

```bash
sudo bpftrace -e '
tracepoint:syscalls:sys_enter_openat { @start[tid] = nsecs; }
tracepoint:syscalls:sys_exit_openat /@start[tid]/ {
    @ns[comm] = hist(nsecs - @start[tid]);
    delete(@start[tid]);
}'
```

#### 디스크 IO

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

#### 큐 지연 (스케줄러)

```bash
sudo runqlat
```

CPU를 할당받기 전 런 큐에서 대기한 시간을 측정함 → CPU 부족이나 우선순위 역전 진단에 유용.

---

### USDT와 애플리케이션 트레이싱

USDT는 애플리케이션이 미리 삽입해 둔 트레이스 포인트임 → systemtap에서 시작했지만 eBPF에서도 활용함.

#### 사용 가능한 USDT 보기

```bash
sudo bpftrace -l 'usdt:/usr/local/bin/myapp:*'
sudo readelf -n /usr/local/bin/myapp | grep -i 'NT_STAPSDT' -A 8
```

#### 잘 알려진 USDT 제공 프로그램

- MySQL: query__start, query__done, sql__lock 등
- Postgres: query__start, transaction__start 등
- Python (DTrace 빌드): function__entry, function__return
- Node.js: gc__start, http__server__request
- Java (libstapsdt): 사용자 정의 가능
- OpenJDK (with USDT): hotspot:thread__start 등

#### 예: MySQL 느린 쿼리

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

#### 자기 애플리케이션에 USDT 추가

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

### 생태계 도구

#### Pixie

쿠버네티스 클러스터에 설치하면 자동으로:
- 모든 프로세스의 메트릭 수집
- HTTP/gRPC/Kafka/MySQL/Postgres 자동 디코드
- 코드 변경 없이 분산 트레이싱
- 대시보드 + script 언어 (PxL)

#### Pyroscope (Grafana)

continuous profiling. eBPF 기반 자동 프로파일러 + 다양한 언어별 SDK.

#### Parca

Polar Signals의 continuous profiling. open source.

#### Tetragon (Cilium)

런타임 보안. 의심스러운 행위(파일 접근, 네트워크 연결, syscall) 감지·차단.

#### Falco

eBPF 기반 침입 탐지. CNCF 졸업 프로젝트.

#### Tracee (Aqua Security)

런타임 보안 + 포렌식. 시스템 이벤트 풍부한 캡처.

#### Hubble (Cilium)

서비스 단위 네트워크 가시성.

#### Coroot

OpenTelemetry + eBPF 자동 instrumentation.

---

### 프로덕션 운용 패턴

#### 오버헤드 측정

운영 환경 적용 전에 측정:

```bash
# CPU benchmark with/without BPF
sysbench --threads=8 cpu run
# (BPF attach)
sysbench --threads=8 cpu run
```

대부분의 단순 BPF는 1~5% 이내. 복잡한 트레이싱은 20%+ 가능.

#### Sampling

이벤트가 너무 많을 경우 샘플링을 적용함:

```c
SEC("kprobe/tcp_v4_connect")
int trace(struct pt_regs *ctx) {
    if (bpf_get_prandom_u32() > 0x80000000) return 0;   // 50% 샘플
    // ...
}
```

#### 백프레셔

ringbuf가 가득 차면 새 이벤트가 손실됨 → 사용자 공간이 충분히 빠르게 소비하는지, 아니면 BPF 측에서 명시적으로 드롭할지 결정 필요.

```c
// 사용자 공간이 못 따라오면 그냥 버림
struct event *e = bpf_ringbuf_reserve(&events, sizeof(*e), 0);
if (!e) {
    __sync_fetch_and_add(&dropped, 1);
    return 0;
}
```

#### 기능 감지

배포 대상 머신마다 커널 버전이 다를 수 있음:

```c
// libbpf
if (!bpf_program__attach(skel->progs.fentry_handler)) {
    // fentry 실패하면 kprobe로 폴백
    bpf_program__attach_kprobe(skel->progs.kprobe_handler, false, "vfs_read");
}
```

#### 보안 권한

```bash
sudo setcap cap_bpf,cap_perfmon+ep /usr/local/bin/myprobe
```

`CAP_SYS_ADMIN` 대신 `CAP_BPF` + `CAP_PERFMON` (5.8+)으로 권한 최소화.

---

### 참고 자료

- [Brendan Gregg: BPF Performance Tools (책)](https://www.brendangregg.com/bpf-performance-tools-book.html)
- [Brendan Gregg: 60 second checklist](https://www.brendangregg.com/blog/2015-12-03/linux-perf-tools.html)
- [FlameGraph](https://github.com/brendangregg/FlameGraph)
- [Pixie docs](https://docs.px.dev/)
- [Parca](https://www.parca.dev/)
- [Pyroscope](https://grafana.com/oss/pyroscope/)

---

## Networking — XDP와 TC

> 원본: https://docs.kernel.org/networking/filter.html , https://docs.cilium.io/en/stable/bpf/

---

### 목차

1. [네트워크 데이터 경로의 hook 지점](#네트워크-데이터-경로의-hook-지점)
2. [XDP](#xdp)
3. [TC (Traffic Control)](#tc-traffic-control)
4. [Socket 필터](#socket-필터)
5. [Sockmap / Sockhash](#sockmap--sockhash)
6. [Cgroup BPF](#cgroup-bpf)
7. [실전 시나리오](#실전-시나리오)
8. [Cilium 사례](#cilium-사례)
9. [참고 자료](#참고-자료)

---

### 네트워크 데이터 경로의 hook 지점

```
[NIC]
  ↓
  ├─ XDP (driver level — 가장 빠름)
  ↓
[skb 할당]
  ↓
  ├─ TC ingress (clsact qdisc)
  ↓
[Bridge / Routing / netfilter]
  ↓
  ├─ Socket Filter
  ↓
[Local Socket 또는 forwarding]
  ↓
  ├─ Cgroup egress
  ↓
  ├─ TC egress
  ↓
[NIC] (egress)
```

각 hook은 서로 다른 정보에 접근하며 서로 다른 동작을 수행 가능.

---

### XDP

**eXpress Data Path**. 디바이스 드라이버가 패킷을 수신하자마자(skb 할당 전) BPF를 호출함 → 가장 빠른 hook 지점.

#### 모드

- **Native**
  - 동작: NIC 드라이버가 직접 호출
  - 호환성: 일부 드라이버만 지원(ixgbe, mlx5, virtio_net 등)
- **Generic**
  - 동작: skb 할당 후 hook(느림)
  - 호환성: 모든 NIC
- **Offload**
  - 동작: NIC 하드웨어가 실행
  - 호환성: Netronome 등 일부 SmartNIC

#### 컨텍스트

```c
struct xdp_md {
    __u32 data;          // 패킷 시작
    __u32 data_end;      // 패킷 끝
    __u32 data_meta;     // 메타데이터 영역
    __u32 ingress_ifindex;
    __u32 rx_queue_index;
    __u32 egress_ifindex;
};
```

#### 반환값

- `XDP_DROP`: 패킷 폐기(버림)
- `XDP_PASS`: 일반 스택으로 전달
- `XDP_TX`: 같은 NIC로 재송신
- `XDP_REDIRECT`: 다른 NIC 또는 AF_XDP 소켓으로
- `XDP_ABORTED`: 에러(trace point 발생)

#### 예: ICMP drop

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

char LICENSE[] SEC("license") = "GPL";
```

#### Attach

```bash
sudo bpftool prog load drop.bpf.o /sys/fs/bpf/drop type xdp
sudo bpftool net attach xdp pinned /sys/fs/bpf/drop dev eth0

# 또는 ip 명령
sudo ip link set dev eth0 xdp obj drop.bpf.o sec xdp

# 제거
sudo ip link set dev eth0 xdp off
```

#### 한계

- 패킷 크기 임의 변경은 불가 (단, `bpf_xdp_adjust_head/tail/meta` 로 일부 조정 가능)
- VLAN/터널 처리는 직접 작성 필요
- 일부 NIC만 native 지원

#### 활용

- **DDoS 방어** (Cloudflare, Facebook Katran)
- **L4 로드 밸런서**
- **방화벽 firstline**
- **네트워크 모니터링/샘플링**

---

### TC (Traffic Control)

`tc` 의 `clsact` qdisc에 attach함 → XDP보다 늦은 단계지만 **ingress와 egress 모두** 지원하며 **skb를 자유롭게 변형** 가능.

#### 컨텍스트

`struct __sk_buff` — skb를 BPF 프로그램에서 다루기 위한 추상 뷰. 패킷 헤더, 길이, 인터페이스, 마크 등 다양한 필드를 제공함.

#### 반환값

- `TC_ACT_OK`: 정상 진행
- `TC_ACT_SHOT`: drop
- `TC_ACT_REDIRECT`: redirect(`bpf_redirect()` 후)
- `TC_ACT_PIPE`: 다음 action으로
- `TC_ACT_RECLASSIFY`: 재분류

#### 예: VLAN 추가

```c
SEC("tc")
int tc_vlan_egress(struct __sk_buff *skb) {
    if (bpf_skb_vlan_push(skb, bpf_htons(ETH_P_8021Q), 100) < 0)
        return TC_ACT_SHOT;
    return TC_ACT_OK;
}
```

#### Attach

```bash
sudo tc qdisc add dev eth0 clsact
sudo tc filter add dev eth0 ingress bpf da obj filter.bpf.o sec tc
sudo tc filter add dev eth0 egress bpf da obj filter.bpf.o sec tc
```

또는 tcx (tc express) 방식:

```bash
sudo bpftool net attach tc pinned /sys/fs/bpf/myprog dev eth0
```

#### XDP vs TC

- 위치: XDP - 드라이버(skb 전) · TC - clsact qdisc(skb 후)
- 속도: XDP - 가장 빠름 · TC - XDP 다음
- 패킷 변형: XDP - 제한적 · TC - 자유
- Egress: XDP - 불가 · TC - 가능
- 컨텍스트: XDP - xdp_md · TC - __sk_buff(풍부)

DDoS drop은 XDP, 라우팅·NAT·QoS는 TC가 일반적.

---

### Socket 필터

원조 BPF — 소켓 단위 패킷 필터. tcpdump가 내부적으로 사용함.

```c
SEC("socket")
int filter(struct __sk_buff *skb) {
    // 0이 아닌 값 = 패킷 전달, 0 = drop
    return skb->len > 100 ? -1 : 0;
}
```

```c
setsockopt(fd, SOL_SOCKET, SO_ATTACH_BPF, &prog_fd, sizeof(prog_fd));
```

최근에는 raw_tracepoint나 XDP/TC를 주로 사용함.

---

### Sockmap / Sockhash

소켓 fd를 저장하는 BPF 맵. 소켓 redirect에 사용하며, 사용자 공간을 거치지 않고 커널 내부에서 데이터를 다른 소켓으로 전달함.

#### 활용

- **사이드카 우회**: Envoy/Istio 사이드카가 수신한 데이터를 커널 공간에서 백엔드 소켓으로 직접 redirect
- **L7 스위칭**

#### 예 (간략)

```c
struct {
    __uint(type, BPF_MAP_TYPE_SOCKHASH);
    __uint(max_entries, 1024);
    __type(key, struct sock_key);
    __type(value, int);
} sock_map SEC(".maps");

SEC("sk_msg")
int sk_msg(struct sk_msg_md *msg) {
    struct sock_key key = {.sip = msg->remote_ip4, ...};
    bpf_msg_redirect_hash(msg, &sock_map, &key, BPF_F_INGRESS);
    return SK_PASS;
}
```

---

### Cgroup BPF

cgroup 단위로 attach함 → 컨테이너/Pod 네트워크 정책 적용에 활용됨.

- `cgroup_skb_ingress`: cgroup으로 들어오는 패킷
- `cgroup_skb_egress`: cgroup에서 나가는 패킷
- `cgroup_sock`: socket() 생성 시
- `cgroup_sock_addr/connect4`: connect/bind/sendmsg
- `cgroup_sysctl`: sysctl 쓰기
- `cgroup_setsockopt/getsockopt`: sockopt 정책

#### 예: cgroup 내 outbound 차단

```c
SEC("cgroup/connect4")
int restrict_connect(struct bpf_sock_addr *ctx) {
    __u32 dst = ctx->user_ip4;
    if ((dst & 0xff) == 192 && ((dst >> 8) & 0xff) == 168) {
        return 0;     // 거부
    }
    return 1;         // 허용
}
```

```bash
int cg_fd = open("/sys/fs/cgroup/restricted", O_RDONLY);
bpf_prog_attach(prog_fd, cg_fd, BPF_CGROUP_INET4_CONNECT, 0);
```

이 cgroup에 속한 모든 프로세스는 192.168.x.x 대역으로 connect 불가.

---

### 실전 시나리오

#### 1. DDoS 방어

```c
SEC("xdp")
int ddos_filter(struct xdp_md *ctx) {
    // SYN flood: 같은 src IP에서 초당 N개 이상 SYN 차단
    // ratelimit map 사용
    // ...
}
```

Cloudflare, Facebook이 실제로 적용한 방식임.

#### 2. L4 로드 밸런서

`XDP_REDIRECT`와 redirect map을 사용해 백엔드 NIC 큐에 분산함.

```c
SEC("xdp")
int xdp_lb(struct xdp_md *ctx) {
    // 5-tuple 해시 → 백엔드 인덱스
    // bpf_redirect_map(&backends, idx, 0)
}
```

Facebook의 Katran이 대표적인 구현체임.

#### 3. 컨테이너 네트워킹

Cilium은 iptables를 대체함 → cgroup BPF + TC + XDP 조합으로 다음을 구현함:
- L3 라우팅
- L7 정책 (HTTP path 검사)
- 서비스 로드밸런싱
- 암호화 (WireGuard)
- 가시성

#### 4. 프로토콜 디코드

XDP/TC에서 HTTP/gRPC/Kafka 메시지를 파싱해 메트릭을 수집함 → Pixie의 자동 instrumentation이 이 방식을 사용함.

#### 5. 네트워크 디버깅

```bash
# 어느 프로세스가 어디로 connect 하는지
sudo tcpconnect -t

# 패킷 손실 원인
sudo tcpdrop

# 재전송 추적
sudo tcpretrans
```

---

### Cilium 사례

쿠버네티스 CNI의 사실상 표준 중 하나로, 거의 모든 네트워크 기능을 eBPF로 구현함.

```
[Pod A] → cgroup_sock_addr (connect 정책)
        → TC egress (encap, 정책)
        → XDP redirect (다른 노드 NIC)
        → 네트워크
        → XDP ingress (decap)
        → TC ingress (정책 + L7 검사)
        → veth → [Pod B]
```

iptables 의존도가 거의 없어 노드당 룰 수에 관계없이 일정한 성능을 보임.

---

### 참고 자료

- [Cilium BPF & XDP Reference Guide](https://docs.cilium.io/en/stable/bpf/)
- [Kernel networking BPF](https://docs.kernel.org/networking/filter.html)
- [Suchakra: Linux observability with BPF (책)](https://www.oreilly.com/library/view/linux-observability-with/9781492050193/)
- [Cloudflare: How Cloudflare uses XDP](https://blog.cloudflare.com/how-to-drop-10-million-packets/)
- [Facebook Katran](https://github.com/facebookincubator/katran)

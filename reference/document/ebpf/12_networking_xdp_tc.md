# Networking — XDP와 TC

> 이 문서는 eBPF의 네트워킹 활용(XDP, TC)을 정리한 것입니다.
> 원본: https://docs.kernel.org/networking/filter.html , https://docs.cilium.io/en/stable/bpf/

---

## 목차

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

## 네트워크 데이터 경로의 hook 지점

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

각 hook이 다른 정보를 보고 다른 일을 할 수 있습니다.

---

## XDP

**eXpress Data Path**. 디바이스 드라이버가 패킷을 받자마자 (skb 할당 전) BPF를 호출. 가장 빠름.

### 모드

| 모드 | 동작 | 호환성 |
| --- | --- | --- |
| **Native** | NIC 드라이버가 직접 호출 | 일부 드라이버만 지원 (ixgbe, mlx5, virtio_net 등) |
| **Generic** | skb 할당 후 hook (느림) | 모든 NIC |
| **Offload** | NIC 하드웨어가 실행 | Netronome 등 일부 SmartNIC |

### 컨텍스트

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

### 반환값

| 값 | 동작 |
| --- | --- |
| `XDP_DROP` | 패킷 폐기 (버림) |
| `XDP_PASS` | 일반 스택으로 전달 |
| `XDP_TX` | 같은 NIC로 재송신 |
| `XDP_REDIRECT` | 다른 NIC 또는 AF_XDP 소켓으로 |
| `XDP_ABORTED` | 에러 (trace point 발생) |

### 예: ICMP drop

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

### Attach

```bash
sudo bpftool prog load drop.bpf.o /sys/fs/bpf/drop type xdp
sudo bpftool net attach xdp pinned /sys/fs/bpf/drop dev eth0

# 또는 ip 명령
sudo ip link set dev eth0 xdp obj drop.bpf.o sec xdp

# 제거
sudo ip link set dev eth0 xdp off
```

### 한계

- 패킷 크기 변경 불가 (단, `bpf_xdp_adjust_head/tail/meta` 로 일부 가능)
- VLAN/터널 처리는 직접 작성
- 일부 NIC만 native 지원

### 활용

- **DDoS 방어** (Cloudflare, Facebook Katran)
- **L4 로드 밸런서**
- **방화벽 firstline**
- **네트워크 모니터링/샘플링**

---

## TC (Traffic Control)

`tc` 의 `clsact` qdisc에 attach. XDP보다 늦은 단계지만 **ingress와 egress 모두** 가능, **skb 변형 가능**.

### 컨텍스트

`struct __sk_buff` — skb의 BPF 친화적 뷰. 패킷 헤더, 길이, 인터페이스, 마크 등 풍부.

### 반환값

| 값 | 의미 |
| --- | --- |
| `TC_ACT_OK` | 정상 진행 |
| `TC_ACT_SHOT` | drop |
| `TC_ACT_REDIRECT` | redirect (`bpf_redirect()` 후) |
| `TC_ACT_PIPE` | 다음 action으로 |
| `TC_ACT_RECLASSIFY` | 재분류 |

### 예: VLAN 추가

```c
SEC("tc")
int tc_vlan_egress(struct __sk_buff *skb) {
    if (bpf_skb_vlan_push(skb, bpf_htons(ETH_P_8021Q), 100) < 0)
        return TC_ACT_SHOT;
    return TC_ACT_OK;
}
```

### Attach

```bash
sudo tc qdisc add dev eth0 clsact
sudo tc filter add dev eth0 ingress bpf da obj filter.bpf.o sec tc
sudo tc filter add dev eth0 egress bpf da obj filter.bpf.o sec tc
```

또는 5.x+ tcx (tc express):

```bash
sudo bpftool net attach tc pinned /sys/fs/bpf/myprog dev eth0
```

### XDP vs TC

| 항목 | XDP | TC |
| --- | --- | --- |
| 위치 | 드라이버 (skb 전) | clsact qdisc (skb 후) |
| 속도 | 가장 빠름 | XDP 다음 |
| 패킷 변형 | 제한적 | 자유 |
| Egress | 불가 | 가능 |
| 컨텍스트 | xdp_md | __sk_buff (풍부) |

DDoS drop은 XDP, 라우팅·NAT·QoS는 TC가 일반적.

---

## Socket 필터

원조 BPF — 소켓 단위 패킷 필터. tcpdump가 사용.

```c
SEC("socket")
int filter(struct __sk_buff *skb) {
    // 0이 아닌 값 = 패킷 전달, 0 = drop
    return skb->len > 100 ? -1 : 0;
}
```

```bash
sudo setsockopt(fd, SOL_SOCKET, SO_ATTACH_BPF, &prog_fd, sizeof(prog_fd));
```

요즘은 거의 쓰지 않고 raw_tracepoint이나 XDP/TC 사용.

---

## Sockmap / Sockhash

소켓 fd를 저장하는 BPF map. 소켓 redirect — 사용자 공간을 거치지 않고 커널 안에서 데이터를 다른 소켓으로 점프.

### 활용

- **사이드카 우회**: Envoy/Istio 사이드카가 받은 데이터를 백엔드 소켓으로 직접 redirect (kernel-space)
- **L7 스위칭**

### 예 (간략)

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

## Cgroup BPF

cgroup 단위로 attach. 컨테이너/Pod 정책에 활용.

| Hook | 용도 |
| --- | --- |
| `cgroup_skb_ingress` | cgroup으로 들어오는 패킷 |
| `cgroup_skb_egress` | cgroup에서 나가는 패킷 |
| `cgroup_sock` | socket() 생성 시 |
| `cgroup_sock_addr/connect4` | connect/bind/sendmsg |
| `cgroup_sysctl` | sysctl 쓰기 |
| `cgroup_setsockopt/getsockopt` | sockopt 정책 |

### 예: cgroup 내 outbound 차단

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

이 cgroup의 모든 프로세스는 192.168.x.x 로 connect 불가.

---

## 실전 시나리오

### 1. DDoS 방어

```c
SEC("xdp")
int ddos_filter(struct xdp_md *ctx) {
    // SYN flood: 같은 src IP에서 초당 N개 이상 SYN 차단
    // ratelimit map 사용
    // ...
}
```

Cloudflare, Facebook이 실제로 사용.

### 2. L4 로드 밸런서

XDP_REDIRECT_MAP으로 백엔드 NIC 큐에 분산.

```c
SEC("xdp")
int xdp_lb(struct xdp_md *ctx) {
    // 5-tuple 해시 → 백엔드 인덱스
    // bpf_redirect_map(&backends, idx, 0)
}
```

Facebook의 Katran이 대표.

### 3. 컨테이너 네트워킹

Cilium이 iptables를 대체. cgroup BPF + TC + XDP 조합으로:
- L3 라우팅
- L7 정책 (HTTP path 검사)
- 서비스 로드밸런싱
- 암호화 (WireGuard)
- 가시성

### 4. 프로토콜 디코드

XDP/TC에서 HTTP/gRPC/Kafka 메시지를 파싱해 메트릭 수집. Pixie의 자동 instrumentation.

### 5. 네트워크 디버깅

```bash
# 어느 프로세스가 어디로 connect 하는지
sudo tcpconnect -t

# 패킷 손실 원인
sudo tcpdrop

# 재전송 추적
sudo tcpretrans
```

---

## Cilium 사례

쿠버네티스 CNI의 사실상 표준 중 하나. 거의 모든 네트워크 기능이 eBPF.

```
[Pod A] → cgroup_sock_addr (connect 정책)
        → TC egress (encap, 정책)
        → XDP redirect (다른 노드 NIC)
        → 네트워크
        → XDP ingress (decap)
        → TC ingress (정책 + L7 검사)
        → veth → [Pod B]
```

iptables 의존이 거의 없어 노드당 룰 수와 상관없이 일정한 성능.

---

## 참고 자료

- [Cilium BPF & XDP Reference Guide](https://docs.cilium.io/en/stable/bpf/)
- [Kernel networking BPF](https://docs.kernel.org/networking/filter.html)
- [Suchakra: Linux observability with BPF (책)](https://www.oreilly.com/library/view/linux-observability-with/9781492050193/)
- [Cloudflare: How Cloudflare uses XDP](https://blog.cloudflare.com/how-to-drop-10-million-packets/)
- [Facebook Katran](https://github.com/facebookincubator/katran)

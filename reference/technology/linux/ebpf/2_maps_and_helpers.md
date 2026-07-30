# BPF Maps와 Helper 함수

## BPF Maps

> 원본: https://docs.kernel.org/bpf/maps.html

---

### 목차

1. [BPF Map이란?](#bpf-map이란)
2. [기본 사용 패턴](#기본-사용-패턴)
3. [Map 종류](#map-종류)
4. [PERCPU 맵](#percpu-맵)
5. [Ring Buffer와 Perf Event Array](#ring-buffer와-perf-event-array)
6. [BPF 객체 핀닝](#bpf-객체-핀닝)
7. [Map-in-Map](#map-in-map)
8. [성능 고려사항](#성능-고려사항)
9. [참고 자료](#참고-자료)

---

### BPF Map이란?

BPF 프로그램과 사용자 공간이 데이터를 교환하는 **공유 자료구조**. 직접 메모리 공유가 불가능한 BPF 환경에서 핵심 통신 채널 역할을 한다.

특징:
- 커널 내부에 존재
- BPF 프로그램과 사용자 공간(`bpf()` 시스템 콜) 양쪽에서 읽기/쓰기 가능
- 여러 BPF 프로그램 간에 공유 가능
- key-value 또는 큐 형태
- 대부분의 맵 타입에서 커널이 동시성(락) 처리

---

### 기본 사용 패턴

#### BPF 측 (선언)

```c
struct {
    __uint(type, BPF_MAP_TYPE_HASH);
    __type(key, u32);
    __type(value, u64);
    __uint(max_entries, 1024);
} my_map SEC(".maps");

SEC("kprobe/sys_open")
int count_opens(struct pt_regs *ctx) {
    u32 pid = bpf_get_current_pid_tgid() >> 32;
    u64 *count = bpf_map_lookup_elem(&my_map, &pid);
    if (count) {
        __sync_fetch_and_add(count, 1);
    } else {
        u64 one = 1;
        bpf_map_update_elem(&my_map, &pid, &one, BPF_ANY);
    }
    return 0;
}
```

#### 사용자 공간 (libbpf)

```c
struct my_skel *skel = my_skel__open_and_load();
my_skel__attach(skel);

u32 cur_key, next_key;
u64 value;

while (true) {
    void *start = NULL;
    while (bpf_map__get_next_key(skel->maps.my_map, start, &next_key, sizeof(next_key)) == 0) {
        bpf_map__lookup_elem(skel->maps.my_map, &next_key, sizeof(next_key), &value, sizeof(value), 0);
        printf("pid %u: %llu opens\n", next_key, value);
        start = &next_key;
        cur_key = next_key;
    }
    sleep(1);
}
```

---

### Map 종류

#### Hash 계열

##### `BPF_MAP_TYPE_HASH`
범용 해시 테이블. key는 임의 크기, value도 임의 크기.

##### `BPF_MAP_TYPE_LRU_HASH`
LRU eviction. 맵이 가득 차면 가장 오래 사용되지 않은 엔트리를 자동 제거. 캐시 워크로드에 적합.

##### `BPF_MAP_TYPE_HASH_OF_MAPS`
map-in-map — 값으로 다른 맵의 fd를 저장. 동적 맵 디스패치에 활용.

#### Array 계열

##### `BPF_MAP_TYPE_ARRAY`
고정 크기 배열. key는 0..max_entries-1 정수. 매우 빠름.

##### `BPF_MAP_TYPE_PERCPU_ARRAY`
CPU별 별도 인스턴스. 동기화 비용 없음. (아래 PERCPU 절 참조)

##### `BPF_MAP_TYPE_PROG_ARRAY`
값으로 BPF 프로그램 fd를 저장. tail call에 사용.

#### 큐 계열

##### `BPF_MAP_TYPE_QUEUE` / `BPF_MAP_TYPE_STACK`
FIFO 큐 / LIFO 스택. push, pop, peek.

```c
struct {
    __uint(type, BPF_MAP_TYPE_QUEUE);
    __uint(max_entries, 1000);
    __type(value, struct event);
} events SEC(".maps");

bpf_map_push_elem(&events, &event, BPF_EXIST);
```

#### Ring Buffer / Perf Event

##### `BPF_MAP_TYPE_RINGBUF`
**5.8+ 권장**. multi-producer/single-consumer ring buffer. 사용자 공간이 폴링. perf_event_array보다 효율적.

##### `BPF_MAP_TYPE_PERF_EVENT_ARRAY`
이전 표준. CPU별 perf ring buffer. 더 복잡하지만 호환성 좋음.

(자세한 내용은 별도 절)

#### Socket/Networking

##### `BPF_MAP_TYPE_SOCKHASH` / `BPF_MAP_TYPE_SOCKMAP`
소켓 fd를 저장. 소켓 리다이렉트에 사용.

##### `BPF_MAP_TYPE_DEVMAP` / `BPF_MAP_TYPE_DEVMAP_HASH`
네트워크 디바이스 인덱스. XDP_REDIRECT의 대상.

##### `BPF_MAP_TYPE_CPUMAP`
RPS와 유사하게 CPU별로 패킷을 분배.

##### `BPF_MAP_TYPE_XSKMAP`
AF_XDP 소켓 맵.

#### LPM (Longest Prefix Match)

##### `BPF_MAP_TYPE_LPM_TRIE`
IP 프리픽스 매칭. 라우팅 테이블, 방화벽 룰.

```c
struct {
    __uint(type, BPF_MAP_TYPE_LPM_TRIE);
    __type(key, struct bpf_lpm_trie_key);
    __type(value, u32);
    __uint(max_entries, 1024);
    __uint(map_flags, BPF_F_NO_PREALLOC);
} routes SEC(".maps");
```

#### 기타

##### `BPF_MAP_TYPE_TASK_STORAGE` / `INODE_STORAGE` / `SK_STORAGE` / `CGROUP_STORAGE`
task/inode/socket/cgroup 객체에 직접 연결된 storage. 객체 수명에 따라 자동으로 관리.

##### `BPF_MAP_TYPE_BLOOM_FILTER` (5.16+)
블룸 필터 — 빠른 멤버십 검사.

---

### PERCPU 맵

`HASH`, `ARRAY` 등에 PERCPU 변형이 있다. CPU 코어마다 별도 인스턴스를 유지하므로 동기화 없이 빠르게 접근할 수 있다.

```c
struct {
    __uint(type, BPF_MAP_TYPE_PERCPU_ARRAY);
    __type(key, u32);
    __type(value, u64);
    __uint(max_entries, 256);
} counter SEC(".maps");

// BPF 측: 자기 CPU 인스턴스에만 접근
u32 key = 0;
u64 *val = bpf_map_lookup_elem(&counter, &key);
if (val) (*val)++;
```

사용자 공간에서 읽을 때:
```c
u64 values[NR_CPUS];
bpf_map__lookup_elem(map, &key, sizeof(key), values, sizeof(values), 0);

u64 total = 0;
for (int i = 0; i < num_cpus; i++) total += values[i];
```

장점: 락 없음, 매우 빠름
단점: 합계 계산은 사용자 공간에서

PERCPU 맵은 카운터, 히스토그램에 거의 항상 권장.

---

### Ring Buffer와 Perf Event Array

BPF 프로그램이 사용자 공간으로 **이벤트 스트림**을 전달할 때 사용.

#### Perf Event Array

```c
struct {
    __uint(type, BPF_MAP_TYPE_PERF_EVENT_ARRAY);
    __uint(key_size, sizeof(int));
    __uint(value_size, sizeof(int));
} events SEC(".maps");

// BPF에서 이벤트 송신
struct event_t event = {.pid = pid, .ts = ts};
bpf_perf_event_output(ctx, &events, BPF_F_CURRENT_CPU, &event, sizeof(event));
```

특징:
- CPU당 별도 링
- 호환성 좋음 (4.x부터)
- 사용자 공간이 CPU마다 개별 폴링

#### Ring Buffer (5.8+, 권장)

```c
struct {
    __uint(type, BPF_MAP_TYPE_RINGBUF);
    __uint(max_entries, 256 * 1024);
} rb SEC(".maps");

// 송신
struct event_t *e = bpf_ringbuf_reserve(&rb, sizeof(*e), 0);
if (!e) return 0;
e->pid = pid;
e->ts = ts;
bpf_ringbuf_submit(e, 0);
```

특징:
- multi-producer / single-consumer
- CPU 공유 링 (단일 링으로 모든 CPU의 이벤트 수신)
- perf_event_array보다 메모리 효율적
- BPF API가 간결 (reserve/submit)
- 커널 5.8 이상에서만 지원

```c
// 사용자 공간 (libbpf)
struct ring_buffer *rb = ring_buffer__new(map_fd, handle_event, NULL, NULL);
while (1) {
    int err = ring_buffer__poll(rb, 100);   // 100ms timeout
    if (err < 0) break;
}
```

---

### BPF 객체 핀닝

맵과 프로그램은 참조하는 사용자 프로세스가 종료되면 사라진다. 영구 보존이 필요하면 BPF FS에 핀한다.

```bash
sudo mount -t bpf bpf /sys/fs/bpf      # 보통 자동 마운트됨
sudo bpftool map pin id <map-id> /sys/fs/bpf/my_map
sudo bpftool prog pin id <prog-id> /sys/fs/bpf/my_prog
```

이후 다른 프로세스에서:
```c
int fd = bpf_obj_get("/sys/fs/bpf/my_map");
```

핀된 객체는 마지막 참조가 사라질 때까지 유지된다. `rm /sys/fs/bpf/my_map`으로 unpin할 수 있다.

---

### Map-in-Map

값으로 다른 맵을 저장하는 맵. 동적 디스패치에 활용.

```c
struct inner_map {
    __uint(type, BPF_MAP_TYPE_HASH);
    __type(key, u32);
    __type(value, u64);
    __uint(max_entries, 100);
};

struct {
    __uint(type, BPF_MAP_TYPE_HASH_OF_MAPS);
    __type(key, u32);
    __array(values, struct inner_map);
    __uint(max_entries, 10);
} outer SEC(".maps");
```

`PROG_ARRAY`와 함께 동적 BPF 라우팅 구현에 자주 쓰인다.

---

### 성능 고려사항

#### Hash vs LRU_HASH vs Array

- 작은 고정 키: **PERCPU_ARRAY** (가장 빠름)
- 동적 키, 풀 컨트롤: **HASH**
- 캐시: **LRU_HASH**
- 매우 큰 키 공간 (예: IP 5-tuple): **HASH** + 적절한 prealloc

#### Prealloc vs No-Prealloc

기본값은 prealloc — max_entries 수만큼 미리 할당. 빠르지만 메모리를 많이 사용.

`BPF_F_NO_PREALLOC`: 동적 할당. 메모리를 절약하지만 약간 느림. LPM_TRIE는 이 플래그가 필수.

#### Update Flag

```c
bpf_map_update_elem(&map, &key, &val, BPF_ANY);    // 생성/업데이트
bpf_map_update_elem(&map, &key, &val, BPF_NOEXIST); // 없을 때만
bpf_map_update_elem(&map, &key, &val, BPF_EXIST);  // 있을 때만
```

#### Atomic 연산

PERCPU 맵이 아닌 환경에서 카운터 증가:
```c
__sync_fetch_and_add(val_ptr, 1);
```

또는 5.13+:
```c
bpf_atomic_xchg(...);
__atomic_fetch_add(val_ptr, 1, __ATOMIC_RELAXED);
```

#### bpf_map_lookup_and_delete

5.14+. 조회와 삭제를 원자적으로 수행. 큐 패턴에서 race condition 방지.

---

### 참고 자료

- [Kernel BPF maps](https://docs.kernel.org/bpf/maps.html)
- [Cilium BPF map types](https://docs.cilium.io/en/stable/bpf/maps/)
- [LWN: BPF ring buffer](https://lwn.net/Articles/820002/)

---

## BPF Helper 함수

> 원본: https://docs.kernel.org/bpf/helpers.html , `man 7 bpf-helpers`

---

### 목차

1. [Helper란?](#helper란)
2. [프로세스/태스크 관련](#프로세스태스크-관련)
3. [Map 관련](#map-관련)
4. [메모리 접근](#메모리-접근)
5. [시간](#시간)
6. [난수](#난수)
7. [출력/디버깅](#출력디버깅)
8. [Tail call과 함수 호출](#tail-call과-함수-호출)
9. [네트워킹](#네트워킹)
10. [트레이싱 전용](#트레이싱-전용)
11. [Kfunc (Kernel Function)](#kfunc-kernel-function)
12. [참고 자료](#참고-자료)

---

### Helper란?

BPF 프로그램은 일반 함수를 호출할 수 없습니다 (자체 BPF 함수 + tail call 제외). 대신 커널이 제공하는 **helper 함수** 만 호출 가능합니다.

각 program type마다 사용 가능한 helper 셋이 다르며, 호출 시 verifier가 인자 타입과 권한을 검증합니다.

호출 형태:
```c
long bpf_some_helper(arg1, arg2, ...);
```

helper는 항상 `long` 반환 (또는 음수 errno).

전체 목록 보기:
```bash
bpftool feature probe
man 7 bpf-helpers
```

---

### 프로세스/태스크 관련

#### bpf_get_current_pid_tgid

```c
u64 id = bpf_get_current_pid_tgid();
u32 tgid = id >> 32;        // 사용자가 보는 PID (=Linux의 TGID)
u32 pid = id & 0xFFFFFFFF;  // 커널 PID (=실제 thread ID)
```

#### bpf_get_current_uid_gid

```c
u64 ug = bpf_get_current_uid_gid();
u32 uid = ug & 0xFFFFFFFF;
u32 gid = ug >> 32;
```

#### bpf_get_current_comm

```c
char comm[16];
bpf_get_current_comm(&comm, sizeof(comm));
```

현재 프로세스의 명령어 이름 (`/proc/<pid>/comm`).

#### bpf_get_current_task

5.10+. `struct task_struct *` 반환 — 현재 태스크의 모든 정보 접근 가능.

```c
struct task_struct *task = (struct task_struct *)bpf_get_current_task();
u32 ppid = BPF_CORE_READ(task, real_parent, tgid);    // 부모 PID
```

`BPF_CORE_READ` 는 CO-RE 매크로로 안전한 필드 접근.

#### bpf_get_smp_processor_id

현재 실행 중인 CPU 번호.

---

### Map 관련

```c
void *bpf_map_lookup_elem(map, key);
long bpf_map_update_elem(map, key, value, flags);
long bpf_map_delete_elem(map, key);

// 큐/스택
long bpf_map_push_elem(map, value, flags);
long bpf_map_pop_elem(map, value);
long bpf_map_peek_elem(map, value);
```

#### bpf_for_each_map_elem (5.13+)

```c
bpf_for_each_map_elem(&map, callback, ctx, 0);
```

map의 모든 엔트리를 순회. callback에서 `BPF_LOOP_RET_BREAK` 반환으로 조기 종료.

#### Ring buffer

```c
void *bpf_ringbuf_reserve(rb, size, flags);
void bpf_ringbuf_submit(data, flags);
void bpf_ringbuf_discard(data, flags);
long bpf_ringbuf_output(rb, data, size, flags);
```

#### Perf event output

```c
long bpf_perf_event_output(ctx, map, flags, data, size);
```

---

### 메모리 접근

BPF는 임의의 메모리를 직접 역참조할 수 없습니다. helper로 안전하게 읽기:

#### bpf_probe_read

```c
char buf[64];
bpf_probe_read(&buf, sizeof(buf), src_ptr);    // 커널 메모리
```

#### bpf_probe_read_user / bpf_probe_read_kernel

5.5+. 커널과 유저 메모리를 명확히 구분.

```c
bpf_probe_read_kernel(&dst, sizeof(dst), kernel_ptr);
bpf_probe_read_user(&dst, sizeof(dst), user_ptr);
```

#### bpf_probe_read_str / _user_str / _kernel_str

널 종료 문자열 읽기.

```c
char filename[256];
bpf_probe_read_user_str(&filename, sizeof(filename), pathname);
```

#### bpf_probe_write_user

위험. 사용자 메모리에 쓰기. 보안 도구 외에는 거의 안 씀.

---

### 시간

#### bpf_ktime_get_ns

부팅 후 경과 나노초 (`CLOCK_MONOTONIC`).

```c
u64 ts = bpf_ktime_get_ns();
```

#### bpf_ktime_get_boot_ns

5.7+. 절전 시간 포함 (`CLOCK_BOOTTIME`).

#### bpf_ktime_get_coarse_ns

5.10+. 빠르지만 jiffy 정밀도. 호출 빈도가 높을 때 유리.

#### bpf_jiffies64

5.5+. 현재 jiffies.

---

### 난수

#### bpf_get_prandom_u32

```c
u32 r = bpf_get_prandom_u32();
```

암호학적이지 않은 빠른 난수.

---

### 출력/디버깅

#### bpf_printk / bpf_trace_printk

```c
bpf_printk("pid=%d count=%llu\n", pid, count);
```

`/sys/kernel/debug/tracing/trace_pipe` 로 출력.

```bash
sudo cat /sys/kernel/debug/tracing/trace_pipe
```

> 디버깅 전용. 프로덕션에서는 ringbuf/perf event로 대체.

---

### Tail call과 함수 호출

#### bpf_tail_call

```c
bpf_tail_call(ctx, &prog_array, key);
// 여기로 돌아오지 않음. 다음 BPF로 점프.
```

#### bpf_tail_call_static

5.10+. 컴파일 타임에 prog_array 인덱스 지정.

---

### 네트워킹

#### Skb 헬퍼

```c
long bpf_skb_load_bytes(skb, offset, to, len);
long bpf_skb_store_bytes(skb, offset, from, len, flags);
long bpf_skb_pull_data(skb, len);
long bpf_l3_csum_replace(skb, offset, from, to, size);
long bpf_l4_csum_replace(skb, offset, from, to, size);
long bpf_redirect(ifindex, flags);
long bpf_redirect_map(map, key, flags);
long bpf_clone_redirect(skb, ifindex, flags);
```

#### XDP 헬퍼

```c
long bpf_xdp_adjust_head(xdp, delta);     // 패킷 시작 조정 (encap/decap)
long bpf_xdp_adjust_tail(xdp, delta);
long bpf_xdp_adjust_meta(xdp, delta);
long bpf_xdp_redirect(ifindex, flags);
long bpf_xdp_redirect_map(map, key, flags);
```

#### 라우팅

```c
long bpf_fib_lookup(ctx, params, plen, flags);
```

커널 FIB 테이블에서 next-hop 조회.

#### Socket 정보

```c
long bpf_sk_lookup_tcp(skb, tuple, len, netns, flags);
long bpf_sk_lookup_udp(...);
void bpf_sk_release(sk);
```

#### Tunnel

```c
long bpf_skb_get_tunnel_key(skb, key, size, flags);
long bpf_skb_set_tunnel_key(skb, key, size, flags);
```

---

### 트레이싱 전용

#### Stack trace

```c
struct {
    __uint(type, BPF_MAP_TYPE_STACK_TRACE);
    __uint(max_entries, 10000);
    __uint(key_size, sizeof(u32));
    __uint(value_size, PERF_MAX_STACK_DEPTH * sizeof(u64));
} stacks SEC(".maps");

int kid = bpf_get_stackid(ctx, &stacks, BPF_F_FAST_STACK_CMP);
int uid = bpf_get_stackid(ctx, &stacks, BPF_F_USER_STACK);
```

flame graph 만들 때 핵심.

또는 5.5+:
```c
u64 ips[32];
bpf_get_stack(ctx, ips, sizeof(ips), BPF_F_USER_STACK);
```

#### bpf_perf_prog_read_value

PERF_EVENT 프로그램에서 perf 카운터 값 읽기.

#### Override return / send signal

5.x+:
```c
bpf_override_return(ctx, errno);   // kprobe에서 함수 강제 실패시키기
bpf_send_signal(SIGKILL);          // 현재 태스크에 시그널
```

위험한 helper. CAP_SYS_ADMIN 필요. 보안 정책에 활용.

---

### Kfunc (Kernel Function)

helper와 별개로 5.x 후반에 도입된 메커니즘. **커널 함수를 직접 BPF에서 호출**할 수 있게 노출.

```c
extern int bpf_dynptr_from_skb(struct sk_buff *skb, ...) __ksym;
```

helper는 커널 ABI가 안정적이지만 새로 추가하기가 까다롭고, kfunc는 더 유연하지만 ABI 보장이 약하다.

지원 kfunc 목록:
```bash
bpftool kfunc list      # 새 버전에서 지원
```

---

### 참고 자료

- [Kernel BPF helpers](https://docs.kernel.org/bpf/helpers.html)
- [man 7 bpf-helpers](https://man7.org/linux/man-pages/man7/bpf-helpers.7.html)
- [bpftool feature probe](https://docs.kernel.org/bpf/bpftool.html)

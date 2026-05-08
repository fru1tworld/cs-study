# BPF Maps

> 이 문서는 BPF map 종류와 사용법을 정리한 것입니다.
> 원본: https://docs.kernel.org/bpf/maps.html

---

## 목차

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

## BPF Map이란?

BPF 프로그램과 사용자 공간이 데이터를 교환하는 **공유 자료구조**. 직접 메모리 공유가 안 되는 BPF 환경에서 유일한 통신 채널.

특징:
- 커널 안에 존재
- BPF 프로그램과 사용자 공간(`bpf()` syscall) 양쪽에서 read/write
- 여러 BPF 프로그램 간에도 공유 가능
- key-value 또는 큐 형태
- 커널이 동시성(락) 처리 (대부분의 map 타입)

---

## 기본 사용 패턴

### BPF 측 (선언)

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

### 사용자 공간 (libbpf)

```c
struct my_skel *skel = my_skel__open_and_load();
my_skel__attach(skel);

u32 key = 0;
u64 value;

while (true) {
    while (bpf_map__get_next_key(skel->maps.my_map, &key, &key, sizeof(key)) == 0) {
        bpf_map__lookup_elem(skel->maps.my_map, &key, sizeof(key), &value, sizeof(value), 0);
        printf("pid %u: %llu opens\n", key, value);
    }
    sleep(1);
}
```

---

## Map 종류

### Hash 계열

#### `BPF_MAP_TYPE_HASH`
범용 해시 테이블. key는 임의 크기, value도 임의 크기.

#### `BPF_MAP_TYPE_LRU_HASH`
LRU eviction. 가득 차면 가장 적게 사용된 엔트리 자동 제거. 캐시 워크로드에 적합.

#### `BPF_MAP_TYPE_HASH_OF_MAPS`
map of maps — 값이 다른 map의 fd. 동적 맵 디스패치.

### Array 계열

#### `BPF_MAP_TYPE_ARRAY`
고정 크기 배열. key는 0..max_entries-1 정수. 매우 빠름.

#### `BPF_MAP_TYPE_PERCPU_ARRAY`
CPU별 별도 인스턴스. 동기화 비용 없음. (아래 PERCPU 절 참조)

#### `BPF_MAP_TYPE_PROG_ARRAY`
값이 BPF 프로그램 fd. tail call에 사용.

### 큐 계열

#### `BPF_MAP_TYPE_QUEUE` / `BPF_MAP_TYPE_STACK`
FIFO 큐 / LIFO 스택. push, pop, peek.

```c
struct {
    __uint(type, BPF_MAP_TYPE_QUEUE);
    __uint(max_entries, 1000);
    __type(value, struct event);
} events SEC(".maps");

bpf_map_push_elem(&events, &event, BPF_EXIST);
```

### Ring Buffer / Perf Event

#### `BPF_MAP_TYPE_RINGBUF`
**5.8+ 권장**. mp/sc ring buffer. 사용자 공간이 polling. perf_event_array보다 효율적.

#### `BPF_MAP_TYPE_PERF_EVENT_ARRAY`
이전 표준. CPU별 perf ring buffer. 더 복잡하지만 호환성 좋음.

(자세한 내용은 별도 절)

### Socket/Networking

#### `BPF_MAP_TYPE_SOCKHASH` / `BPF_MAP_TYPE_SOCKMAP`
소켓 fd를 저장. socket redirect에 사용.

#### `BPF_MAP_TYPE_DEVMAP` / `BPF_MAP_TYPE_DEVMAP_HASH`
네트워크 디바이스 인덱스. XDP_REDIRECT의 대상.

#### `BPF_MAP_TYPE_CPUMAP`
CPU에 RPS 비슷하게 패킷 dispatch.

#### `BPF_MAP_TYPE_XSKMAP`
AF_XDP 소켓.

### LPM (Longest Prefix Match)

#### `BPF_MAP_TYPE_LPM_TRIE`
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

### 기타

#### `BPF_MAP_TYPE_TASK_STORAGE` / `INODE_STORAGE` / `SK_STORAGE` / `CGROUP_STORAGE`
task/inode/socket/cgroup 객체에 직접 매달린 storage. 자동 라이프사이클.

#### `BPF_MAP_TYPE_BLOOM_FILTER` (5.16+)
블룸 필터 — 빠른 멤버십 검사.

---

## PERCPU 맵

`HASH`, `ARRAY` 등에 PERCPU 변형이 있음. CPU 코어마다 별도 인스턴스를 가져 동기화 없이 빠르게 접근 가능.

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

## Ring Buffer와 Perf Event Array

BPF가 사용자 공간으로 **이벤트 스트림** 을 보낼 때 사용.

### Perf Event Array

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
- CPU당 별도 ring
- 호환성 좋음 (4.x부터)
- 사용자 공간이 CPU마다 separate poll

### Ring Buffer (5.8+, 권장)

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
- CPU 통합 ring (단일 ring으로 모든 CPU의 이벤트)
- 더 효율적
- BPF 측 API가 깔끔 (reserve/submit)
- 5.8+ 만 지원

```c
// 사용자 공간 (libbpf)
struct ring_buffer *rb = ring_buffer__new(map_fd, handle_event, NULL, NULL);
while (1) {
    int err = ring_buffer__poll(rb, 100);   // 100ms timeout
    if (err < 0) break;
}
```

---

## BPF 객체 핀닝

map과 프로그램은 사용자 프로세스가 죽으면 사라집니다. 영구화하려면 BPF FS에 핀.

```bash
sudo mount -t bpf bpf /sys/fs/bpf      # 보통 자동
sudo bpftool map pin id <map-id> /sys/fs/bpf/my_map
sudo bpftool prog pin id <prog-id> /sys/fs/bpf/my_prog
```

이후 다른 프로세스에서:
```c
int fd = bpf_obj_get("/sys/fs/bpf/my_map");
```

핀된 객체는 마지막 참조가 사라질 때까지 유지. `rm /sys/fs/bpf/my_map` 으로 unpin.

---

## Map-in-Map

값이 다른 map인 map. 동적 디스패치.

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

`PROG_ARRAY` 와 함께 동적 BPF 라우팅을 구현하는 데 자주 쓰임.

---

## 성능 고려사항

### Hash vs LRU_HASH vs Array

- 작은 고정 키: **PERCPU_ARRAY** (가장 빠름)
- 동적 키, 풀 컨트롤: **HASH**
- 캐시: **LRU_HASH**
- 매우 큰 키 공간 (예: IP 5-tuple): **HASH** + 적절한 prealloc

### Prealloc vs No-Prealloc

기본은 prealloc — 모든 max_entries만큼 미리 할당. 빠르지만 메모리 사용 큼.

`BPF_F_NO_PREALLOC`: 동적 할당. 메모리 절약, 약간 느림. LPM_TRIE는 이게 강제.

### Update Flag

```c
bpf_map_update_elem(&map, &key, &val, BPF_ANY);    // 생성/업데이트
bpf_map_update_elem(&map, &key, &val, BPF_NOEXIST); // 없을 때만
bpf_map_update_elem(&map, &key, &val, BPF_EXIST);  // 있을 때만
```

### Atomic 연산

PERCPU가 아닌 곳에서 카운터 증가:
```c
__sync_fetch_and_add(val_ptr, 1);
```

또는 5.13+:
```c
bpf_atomic_xchg(...);
__atomic_fetch_add(val_ptr, 1, __ATOMIC_RELAXED);
```

### bpf_map_lookup_and_delete

5.14+. 조회+삭제를 하나로. 큐 패턴에서 race 방지.

---

## 참고 자료

- [Kernel BPF maps](https://docs.kernel.org/bpf/maps.html)
- [Cilium BPF map types](https://docs.cilium.io/en/stable/bpf/maps/)
- [LWN: BPF ring buffer](https://lwn.net/Articles/820002/)

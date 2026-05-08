# BPF Helper 함수

> 이 문서는 BPF helper 함수의 카테고리와 주요 helper들을 정리한 것입니다.
> 원본: https://docs.kernel.org/bpf/helpers.html , `man 7 bpf-helpers`

---

## 목차

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

## Helper란?

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

## 프로세스/태스크 관련

### bpf_get_current_pid_tgid

```c
u64 id = bpf_get_current_pid_tgid();
u32 tgid = id >> 32;        // 사용자가 보는 PID (=Linux의 TGID)
u32 pid = id & 0xFFFFFFFF;  // 커널 PID (=실제 thread ID)
```

### bpf_get_current_uid_gid

```c
u64 ug = bpf_get_current_uid_gid();
u32 uid = ug & 0xFFFFFFFF;
u32 gid = ug >> 32;
```

### bpf_get_current_comm

```c
char comm[16];
bpf_get_current_comm(&comm, sizeof(comm));
```

현재 프로세스의 명령어 이름 (`/proc/<pid>/comm`).

### bpf_get_current_task

5.10+. `struct task_struct *` 반환 — 현재 태스크의 모든 정보 접근 가능.

```c
struct task_struct *task = (struct task_struct *)bpf_get_current_task();
u32 ppid = BPF_CORE_READ(task, real_parent, tgid);    // 부모 PID
```

`BPF_CORE_READ` 는 CO-RE 매크로로 안전한 필드 접근.

### bpf_get_smp_processor_id

현재 실행 중인 CPU 번호.

---

## Map 관련

```c
void *bpf_map_lookup_elem(map, key);
long bpf_map_update_elem(map, key, value, flags);
long bpf_map_delete_elem(map, key);

// 큐/스택
long bpf_map_push_elem(map, value, flags);
long bpf_map_pop_elem(map, value);
long bpf_map_peek_elem(map, value);
```

### bpf_for_each_map_elem (5.13+)

```c
bpf_for_each_map_elem(&map, callback, ctx, 0);
```

map의 모든 엔트리를 순회. callback에서 `BPF_LOOP_RET_BREAK` 반환으로 조기 종료.

### Ring buffer

```c
void *bpf_ringbuf_reserve(rb, size, flags);
void bpf_ringbuf_submit(data, flags);
void bpf_ringbuf_discard(data, flags);
long bpf_ringbuf_output(rb, data, size, flags);
```

### Perf event output

```c
long bpf_perf_event_output(ctx, map, flags, data, size);
```

---

## 메모리 접근

BPF는 임의의 메모리를 직접 역참조할 수 없습니다. helper로 안전하게 읽기:

### bpf_probe_read

```c
char buf[64];
bpf_probe_read(&buf, sizeof(buf), src_ptr);    // 커널 메모리
```

### bpf_probe_read_user / bpf_probe_read_kernel

5.5+. 커널과 유저 메모리를 명확히 구분.

```c
bpf_probe_read_kernel(&dst, sizeof(dst), kernel_ptr);
bpf_probe_read_user(&dst, sizeof(dst), user_ptr);
```

### bpf_probe_read_str / _user_str / _kernel_str

널 종료 문자열 읽기.

```c
char filename[256];
bpf_probe_read_user_str(&filename, sizeof(filename), pathname);
```

### bpf_probe_write_user

위험. 사용자 메모리에 쓰기. 보안 도구 외에는 거의 안 씀.

---

## 시간

### bpf_ktime_get_ns

부팅 후 경과 나노초 (`CLOCK_MONOTONIC`).

```c
u64 ts = bpf_ktime_get_ns();
```

### bpf_ktime_get_boot_ns

5.7+. 절전 시간 포함 (`CLOCK_BOOTTIME`).

### bpf_ktime_get_coarse_ns

5.10+. 빠르지만 jiffy 정밀도. 많이 호출하는 경우 유리.

### bpf_jiffies64

5.5+. 현재 jiffies.

---

## 난수

### bpf_get_prandom_u32

```c
u32 r = bpf_get_prandom_u32();
```

암호학적이지 않은 빠른 난수.

---

## 출력/디버깅

### bpf_printk / bpf_trace_printk

```c
bpf_printk("pid=%d count=%llu\n", pid, count);
```

`/sys/kernel/debug/tracing/trace_pipe` 로 출력.

```bash
sudo cat /sys/kernel/debug/tracing/trace_pipe
```

> 디버깅 전용. 프로덕션에서는 ringbuf/perf event로 대체.

---

## Tail call과 함수 호출

### bpf_tail_call

```c
bpf_tail_call(ctx, &prog_array, key);
// 여기로 돌아오지 않음. 다음 BPF로 점프.
```

### bpf_tail_call_static

5.10+. 컴파일 타임에 prog_array 인덱스 지정.

---

## 네트워킹

### Skb 헬퍼

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

### XDP 헬퍼

```c
long bpf_xdp_adjust_head(xdp, delta);     // 패킷 시작 조정 (encap/decap)
long bpf_xdp_adjust_tail(xdp, delta);
long bpf_xdp_adjust_meta(xdp, delta);
long bpf_xdp_redirect(ifindex, flags);
long bpf_xdp_redirect_map(map, key, flags);
```

### 라우팅

```c
long bpf_fib_lookup(ctx, params, plen, flags);
```

커널 FIB 테이블에서 next-hop 조회.

### Socket 정보

```c
long bpf_sk_lookup_tcp(skb, tuple, len, netns, flags);
long bpf_sk_lookup_udp(...);
void bpf_sk_release(sk);
```

### Tunnel

```c
long bpf_skb_get_tunnel_key(skb, key, size, flags);
long bpf_skb_set_tunnel_key(skb, key, size, flags);
```

---

## 트레이싱 전용

### Stack trace

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

### bpf_perf_prog_read_value

PERF_EVENT 프로그램에서 perf 카운터 값 읽기.

### Override return / send signal

5.x+:
```c
bpf_override_return(ctx, errno);   // kprobe에서 함수 강제 실패시키기
bpf_send_signal(SIGKILL);          // 현재 태스크에 시그널
```

위험한 helper. CAP_SYS_ADMIN 필요. 보안 정책에 활용.

---

## Kfunc (Kernel Function)

helper와 별개로 5.x 후반에 도입된 메커니즘. **커널 함수를 직접 BPF에서 호출** 할 수 있게 노출.

```c
extern int bpf_dynptr_from_skb(struct sk_buff *skb, ...) __ksym;
```

helper는 커널 ABI가 안정적이지만 새로 추가하기 까다로움. kfunc는 더 유연하지만 ABI 보장 약함.

지원 kfunc 목록:
```bash
bpftool kfunc list      # 새 버전에서 지원
```

---

## 참고 자료

- [Kernel BPF helpers](https://docs.kernel.org/bpf/helpers.html)
- [man 7 bpf-helpers](https://man7.org/linux/man-pages/man7/bpf-helpers.7.html)
- [bpftool feature probe](https://docs.kernel.org/bpf/bpftool.html)

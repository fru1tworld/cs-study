# BTF와 CO-RE

> 이 문서는 BPF Type Format(BTF)과 Compile Once Run Everywhere(CO-RE)의 개념을 정리한 것입니다.
> 원본: https://docs.kernel.org/bpf/btf.html , https://nakryiko.com/posts/bpf-portability-and-co-re/

---

## 목차

1. [문제: BPF 휴대성](#문제-bpf-휴대성)
2. [BTF란?](#btf란)
3. [CO-RE란?](#co-re란)
4. [vmlinux.h](#vmlinuxh)
5. [BPF_CORE_READ](#bpf_core_read)
6. [Field Existence와 Relocation](#field-existence와-relocation)
7. [실전 작성 패턴](#실전-작성-패턴)
8. [BTF 활용 helper](#btf-활용-helper)
9. [참고 자료](#참고-자료)

---

## 문제: BPF 휴대성

옛날 BPF 개발의 큰 고통:

```c
struct task_struct *task = ...;
char *comm = task->comm;           // 컴파일 시 task_struct 정의가 필요
```

`task_struct` 의 필드 오프셋은 커널 버전에 따라 달라집니다. `task->comm` 의 메모리 주소는 5.10에서는 `+0x648`, 5.15에서는 `+0x650` 같은 식으로 바뀔 수 있어요.

옛 해결책 — **BCC** : 사용자 머신에서 매번 컴파일.

```python
# BCC 사용 예
text = """
int trace_clone(struct pt_regs *ctx) {
    struct task_struct *t = (struct task_struct *)bpf_get_current_task();
    bpf_trace_printk("comm=%s\\n", t->comm);
    return 0;
}
"""
b = BPF(text=text)    # 실행 시 컴파일!
```

문제:
- LLVM/Clang을 모든 머신에 설치
- 컴파일 시간이 소요 (수 초)
- 커널 헤더 필요 (몇 백 MB)
- 메모리 사용 큼

**해결책 — CO-RE** : 한 번 컴파일하고 다양한 커널에서 실행. BTF가 핵심 빌딩 블록.

---

## BTF란?

**BPF Type Format**. 커널 소스의 디버깅 정보(DWARF의 부분집합)를 압축해서 커널 이미지나 모듈에 박아 넣은 것.

특징:
- 모든 커널 타입 정보 포함 (struct, enum, typedef, ...)
- 컴팩트 (수 MB)
- 5.x 이상 커널에 보통 내장 (`/sys/kernel/btf/vmlinux`)

확인:
```bash
$ ls -la /sys/kernel/btf/vmlinux
-r--r--r-- 1 root root 4521234 May  8 09:00 /sys/kernel/btf/vmlinux

$ bpftool btf dump file /sys/kernel/btf/vmlinux | head
```

### BTF 활성화 빌드

커널 컴파일 시 `CONFIG_DEBUG_INFO_BTF=y` 필요. 모든 메이저 배포판이 켜둠 (RHEL 8.2+, Ubuntu 20.10+ 등).

```bash
zcat /proc/config.gz | grep BTF
```

### 모듈 BTF

`CONFIG_DEBUG_INFO_BTF_MODULES=y` 면 커널 모듈도 자체 BTF를 가짐. `/sys/kernel/btf/<module>` 에 있음.

---

## CO-RE란?

**Compile Once - Run Everywhere**. 한 번 컴파일한 BPF 오브젝트가 여러 커널 버전에서 동작하도록 하는 기법.

작동 원리:
1. **컴파일 타임**: Clang이 BPF 코드의 필드 접근 부분에 "이건 task_struct.comm 이라는 필드의 오프셋이 들어갈 자리야" 라는 **재배치(relocation) 메타데이터** 를 남김
2. **로드 타임**: libbpf가 실행 머신의 BTF를 읽어 실제 오프셋을 계산하고 BPF 바이트코드를 패치
3. 실행: 패치된 코드가 정확한 오프셋으로 메모리 접근

이 덕분에 5.4에서 컴파일한 BPF가 5.15에서도 동작합니다.

---

## vmlinux.h

CO-RE BPF 작성에 필요한 거대한 헤더. 커널의 모든 타입 정의가 들어있음.

생성:
```bash
sudo bpftool btf dump file /sys/kernel/btf/vmlinux format c > vmlinux.h
```

이 파일은 수 MB 크기 (수십만 줄). BPF 소스에서:

```c
#include "vmlinux.h"
#include <bpf/bpf_helpers.h>
#include <bpf/bpf_core_read.h>
```

`vmlinux.h` 가 있으면 일반 커널 헤더(`linux/sched.h` 등) 없이도 모든 커널 구조체를 사용할 수 있습니다.

---

## BPF_CORE_READ

CO-RE 방식의 안전한 필드 접근 매크로.

### 단일 필드

```c
struct task_struct *task = (struct task_struct *)bpf_get_current_task();
u32 pid = BPF_CORE_READ(task, pid);
```

내부적으로 `bpf_probe_read_kernel(&pid, sizeof(pid), &task->pid)` 같은 코드를 생성하면서 오프셋을 CO-RE 재배치로 처리.

### 체이닝

```c
u32 ppid = BPF_CORE_READ(task, real_parent, tgid);
char *parent_comm = BPF_CORE_READ(task, real_parent, comm);
```

직접 쓰면 매번 probe_read를 해야 하지만 매크로가 자동 처리.

### 문자열

```c
char comm[16];
BPF_CORE_READ_STR_INTO(&comm, task, comm);
```

### 변형

| 매크로 | 용도 |
| --- | --- |
| `BPF_CORE_READ` | 값 반환 |
| `BPF_CORE_READ_INTO` | 버퍼에 쓰기 |
| `BPF_CORE_READ_STR_INTO` | 문자열 |
| `BPF_CORE_READ_USER` | 유저 메모리 |
| `BPF_CORE_READ_USER_STR_INTO` | 유저 문자열 |

---

## Field Existence와 Relocation

### 필드 존재 확인

커널 버전에 따라 필드가 있을 수도 없을 수도 있는 경우:

```c
if (bpf_core_field_exists(task->bpf_specific_field)) {
    // 5.x 이상에서만 존재
    val = BPF_CORE_READ(task, bpf_specific_field);
}
```

### 필드 크기

```c
size_t sz = bpf_core_field_size(task->comm);
```

### Enum 값

enum 값이 커널 버전마다 다를 때:

```c
if (BPF_CORE_READ(...) == bpf_core_enum_value(enum some_enum, SOME_VAL))
```

### Type Existence

```c
if (bpf_core_type_exists(struct new_struct_v55)) {
    // 5.5+ 에서만 동작
}
```

### Relocation의 종류

CO-RE는 다음 종류의 재배치를 지원:
- field offset
- field size
- field existence
- enum value
- type id, exists, size
- kernel version

이 모두 컴파일 타임 메타로 기록되고 로드 시 패치됩니다.

---

## 실전 작성 패턴

### 디렉터리 구조 (libbpf-bootstrap 스타일)

```
project/
├── src/
│   ├── myapp.bpf.c           # BPF 프로그램
│   ├── myapp.c               # 사용자 공간
│   └── myapp.h               # 공유 구조체
├── vmlinux.h
├── Makefile
└── ...
```

### 공유 구조체

```c
// myapp.h — BPF와 user-space 공통
struct event {
    u32 pid;
    u32 ppid;
    u64 timestamp;
    char comm[16];
};
```

### BPF 측

```c
// myapp.bpf.c
#include "vmlinux.h"
#include <bpf/bpf_helpers.h>
#include <bpf/bpf_core_read.h>
#include <bpf/bpf_tracing.h>

#include "myapp.h"

char LICENSE[] SEC("license") = "Dual BSD/GPL";

struct {
    __uint(type, BPF_MAP_TYPE_RINGBUF);
    __uint(max_entries, 256 * 1024);
} events SEC(".maps");

SEC("tp_btf/sched_process_exec")
int handle_exec(u64 *ctx) {
    struct task_struct *task = (struct task_struct *)bpf_get_current_task();

    struct event *e = bpf_ringbuf_reserve(&events, sizeof(*e), 0);
    if (!e) return 0;

    e->pid = BPF_CORE_READ(task, tgid);
    e->ppid = BPF_CORE_READ(task, real_parent, tgid);
    e->timestamp = bpf_ktime_get_ns();
    BPF_CORE_READ_STR_INTO(&e->comm, task, comm);

    bpf_ringbuf_submit(e, 0);
    return 0;
}
```

### 빌드

```makefile
myapp.bpf.o: src/myapp.bpf.c vmlinux.h
	clang -O2 -target bpf -D__TARGET_ARCH_x86 \
	      -I. -c src/myapp.bpf.c -o $@

myapp.skel.h: myapp.bpf.o
	bpftool gen skeleton $< > $@

myapp: src/myapp.c myapp.skel.h
	cc -O2 -o $@ src/myapp.c -lbpf -lelf -lz
```

### 사용자 공간

```c
// myapp.c
#include "myapp.skel.h"
#include "myapp.h"

static int handle_event(void *ctx, void *data, size_t sz) {
    const struct event *e = data;
    printf("%llu pid=%u ppid=%u comm=%s\n",
           e->timestamp, e->pid, e->ppid, e->comm);
    return 0;
}

int main() {
    struct myapp_bpf *skel = myapp_bpf__open_and_load();
    myapp_bpf__attach(skel);

    struct ring_buffer *rb = ring_buffer__new(
        bpf_map__fd(skel->maps.events), handle_event, NULL, NULL);

    while (1) ring_buffer__poll(rb, 100);
}
```

---

## BTF 활용 helper

### bpf_snprintf_btf

5.10+. BTF 정보로 구조체를 자동 포매팅.

```c
char buf[256];
struct task_struct *t = (void *)bpf_get_current_task();
bpf_snprintf_btf(buf, sizeof(buf), &t->comm, sizeof(t->comm), 0);
```

### bpf_seq_btf_write

iter 프로그램에서 BTF 형식으로 출력.

### Type Cast (5.10+)

```c
struct task_struct *task = bpf_get_current_task_btf();   // BTF 타입 캐스트
```

---

## 참고 자료

- [Kernel BTF docs](https://docs.kernel.org/bpf/btf.html)
- [BPF CO-RE: BPF Portability and CO-RE](https://nakryiko.com/posts/bpf-portability-and-co-re/)
- [libbpf-bootstrap](https://github.com/libbpf/libbpf-bootstrap)
- [BTF deduplication](https://nakryiko.com/posts/btf-dedup/)

# bpftrace

> 이 문서는 bpftrace의 문법과 사용법을 정리한 것입니다.
> 원본: https://github.com/bpftrace/bpftrace , https://bpftrace.org/

---

## 목차

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

## bpftrace란?

DTrace에서 영감을 받은 **고수준 트레이싱 언어**. BPF 위에서 동작하지만 사용자는 짧은 스크립트로 복잡한 관측을 표현할 수 있습니다.

특징:
- 원라이너 친화 (CLI에서 즉석 트레이싱)
- AWK 스타일 문법 (probe / filter / action)
- 자동 메모리 관리
- 풍부한 내장 변수와 helper
- 100+ 라이브러리 예제

---

## 설치

```bash
# Ubuntu/Debian
sudo apt install bpftrace

# RHEL/Fedora
sudo dnf install bpftrace

# macOS는 지원 안 함 (Linux 전용)
```

### 권한

CAP_BPF, CAP_PERFMON 또는 root.

```bash
sudo bpftrace -e 'BEGIN { printf("hello\n"); }'
```

### 가능한 probe 검색

```bash
sudo bpftrace -l 'tracepoint:syscalls:sys_enter*'
sudo bpftrace -l 'kprobe:tcp_*'
sudo bpftrace -l 'usdt:/usr/lib/libpython3.so:*'
```

### 검증

```bash
sudo bpftrace -e 'BEGIN { exit(); }'
```

---

## Probe 종류

```
provider:target:[fn|tp]
```

### kprobe / kretprobe

```
kprobe:vfs_read
kretprobe:vfs_read
```

### uprobe / uretprobe

```
uprobe:/usr/lib/libc.so.6:malloc
uretprobe:/usr/local/bin/myapp:handle_request
```

### tracepoint

```
tracepoint:syscalls:sys_enter_openat
tracepoint:sched:sched_switch
```

`args->필드` 로 인자 접근.

### usdt

```
usdt:/usr/local/bin/myapp:provider:probe_name
```

### profile / interval

```
profile:hz:99      # 모든 CPU에서 99Hz로 샘플링
interval:s:1       # 1초마다
```

### software / hardware

```
software:cpu-clock:1000000   # CPU clock event
hardware:cache-misses:100000
```

### BEGIN / END

```
BEGIN  { printf("starting\n"); }
END    { printf("done\n"); }
```

### kfunc / kretfunc (BTF 기반, 5.5+)

```
kfunc:vfs_read       # kprobe보다 빠르고 인자 직접 접근
```

```
kfunc:vfs_read {
    printf("file=%s count=%zu\n", str(args->file->f_path.dentry->d_name.name), args->count);
}
```

---

## 내장 변수

| 변수 | 의미 |
| --- | --- |
| `pid` | 현재 PID (TGID) |
| `tid` | 스레드 ID |
| `uid`, `gid` | UID, GID |
| `comm` | 명령어 이름 |
| `cpu` | CPU 번호 |
| `ncpus` | CPU 수 |
| `nsecs` | nanosecond 타임스탬프 |
| `elapsed` | 프로그램 시작 후 ns |
| `cgroup` | cgroup ID |
| `args` | tracepoint/kfunc 인자 (`args->name`) |
| `arg0..arg9` | kprobe/uprobe 인자 (정수) |
| `retval` | kretprobe 반환값 |
| `func` | 현재 함수 이름 |
| `kstack` | 커널 스택 |
| `ustack` | 사용자 스택 |
| `probe` | 현재 probe 이름 |

### 인자 접근

#### tracepoint

```
tracepoint:syscalls:sys_enter_openat {
    printf("filename=%s\n", str(args->filename));
}
```

(필드 자동완성은 `bpftrace -lv` 로 확인.)

#### kprobe

```
kprobe:vfs_read {
    printf("file=%p count=%lu\n", arg1, arg2);
}
```

#### kfunc (구조체로)

```
kfunc:vfs_read {
    printf("count=%zu\n", args->count);
}
```

---

## 내장 함수

### 출력

| 함수 | 설명 |
| --- | --- |
| `printf("...", ...)` | C printf |
| `print(@map)` | map 출력 |
| `time("...")` / `strftime("...", nsec)` | 시간 포맷 |
| `cat("/path")` | 파일 내용 출력 |
| `system("cmd")` | 외부 명령 실행 (느림 — 디버깅 외 비추) |

### 메모리 접근

| 함수 | 설명 |
| --- | --- |
| `str(ptr)` | 문자열 |
| `str(ptr, len)` | 길이 제한 문자열 |
| `buf(ptr, size)` | hex 덤프용 |
| `kaddr("symbol")` | 커널 심볼 주소 |
| `uaddr("symbol")` | 유저 심볼 주소 |
| `usym(addr)` | 유저 심볼 이름 |
| `ksym(addr)` | 커널 심볼 이름 |

### 시간

| 함수 | 설명 |
| --- | --- |
| `nsecs` | 현재 ns |
| `elapsed` | 시작 후 ns |

### 스택

| 함수 | 설명 |
| --- | --- |
| `kstack` | 커널 스택 (변수처럼) |
| `kstack(N)` | 깊이 N |
| `ustack` | 유저 스택 |
| `ustack(N, mode)` | mode: `perf` 또는 `bpftrace` |

### 데이터 변환

| 함수 | 설명 |
| --- | --- |
| `count()` | 1 증가 (집계) |
| `sum(x)` | 누적 합 |
| `avg(x)` | 평균 |
| `min(x)`, `max(x)` | 최소/최대 |
| `hist(x)` | 로그 스케일 히스토그램 |
| `lhist(x, min, max, step)` | 선형 히스토그램 |
| `stats(x)` | count + avg + total |

---

## 맵과 집계

map은 `@` 으로 시작:

### 단순 카운터

```
tracepoint:syscalls:sys_enter_openat {
    @opens[comm] = count();
}
```

종료 시 자동 출력. 또는:

```
END {
    print(@opens);
    clear(@opens);
}
```

### 다차원 키

```
@latency[comm, pid] = hist(nsecs - @start);
```

### 히스토그램

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

### Top N

```
END {
    print(@opens, 10);     // top 10
}
```

---

## 흐름 제어

### Filter (predicate)

```
kprobe:vfs_read /pid == 1234/ {
    printf("read by target\n");
}

kprobe:vfs_open /comm == "myapp"/ {
    @[args->path] = count();
}
```

### if/else

```
kprobe:vfs_read {
    if (arg2 > 1024) {
        @big_reads = count();
    } else {
        @small_reads = count();
    }
}
```

### 변수

스칼라 변수는 `$` 접두사:

```
{
    $size = arg2;
    if ($size > 0) @hist = hist($size);
}
```

전역 map은 `@`.

### exit

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

## 실전 원라이너 모음

### 프로세스 추적

```bash
# 새 프로세스 (exec)
sudo bpftrace -e 'tracepoint:sched:sched_process_exec { printf("%s pid=%d\n", str(args->filename), pid); }'

# 프로세스 종료
sudo bpftrace -e 'tracepoint:sched:sched_process_exit { printf("%s pid=%d exit\n", comm, pid); }'
```

### 파일 시스템

```bash
# open() 추적
sudo bpftrace -e 'tracepoint:syscalls:sys_enter_openat { printf("%s %s\n", comm, str(args->filename)); }'

# 큰 read만
sudo bpftrace -e 'kprobe:vfs_read /arg2 > 1048576/ { printf("%s big read %lu\n", comm, arg2); }'

# 파일별 read 통계
sudo bpftrace -e 'kprobe:vfs_read { @[comm] = count(); }'
```

### 시스템 콜

```bash
# 가장 많이 호출된 syscall
sudo bpftrace -e 'tracepoint:raw_syscalls:sys_enter { @[probe] = count(); }'

# 특정 프로세스의 syscall
sudo bpftrace -e 'tracepoint:raw_syscalls:sys_enter /pid == 1234/ { @[args->id] = count(); }'

# 실패한 syscall (errno)
sudo bpftrace -e 'tracepoint:raw_syscalls:sys_exit /args->ret < 0/ { @[args->id, -args->ret] = count(); }'
```

### 네트워크

```bash
# 새 TCP 연결
sudo bpftrace -e 'kprobe:tcp_v4_connect { printf("%s connect\n", comm); }'

# TCP 재전송
sudo bpftrace -e 'kprobe:tcp_retransmit_skb { printf("%s retx\n", comm); }'
```

### 성능

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

### 메모리

```bash
# malloc 호출 빈도
sudo bpftrace -e 'uprobe:/usr/lib/libc.so.6:malloc { @[comm] = count(); }'

# 큰 malloc만
sudo bpftrace -e 'uprobe:/usr/lib/libc.so.6:malloc /arg0 > 1048576/ { printf("%s malloc(%lu)\n", comm, arg0); }'
```

---

## 스크립트 파일

긴 트레이싱은 `.bt` 파일로:

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

### 라이브러리

`/usr/share/bpftrace/tools/` 에 100+개 예제 스크립트가 있습니다.

```bash
ls /usr/share/bpftrace/tools/
sudo /usr/share/bpftrace/tools/tcpconnect.bt
sudo /usr/share/bpftrace/tools/biolatency.bt
sudo /usr/share/bpftrace/tools/runqlat.bt
```

---

## 참고 자료

- [bpftrace GitHub](https://github.com/bpftrace/bpftrace)
- [bpftrace 공식 사이트](https://bpftrace.org/)
- [bpftrace one-liners (Brendan Gregg)](https://github.com/bpftrace/bpftrace/blob/master/docs/tutorial_one_liners.md)
- [bpftrace reference](https://bpftrace.org/learn-ebpf/)

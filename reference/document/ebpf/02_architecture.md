# BPF 아키텍처와 Verifier

> 이 문서는 Linux 커널의 BPF 아키텍처 문서를 한국어로 정리한 것입니다.
> 원본: https://docs.kernel.org/bpf/instruction-set.html , https://docs.kernel.org/bpf/verifier.html

---

## 목차

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

## BPF 가상머신

eBPF는 64비트 RISC 풍 가상머신 명령어 셋을 정의합니다.

### 특징

- 11개의 64비트 레지스터 (R0~R10)
- 512바이트 스택
- 무한 루프 불가 (또는 bounded)
- 무제한 코드 크기 (이전 4096 → 현재 100만 명령어)
- 안전한 메모리 접근만

### 실행 흐름

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

## 명령어 셋

64비트 명령어. 각 명령어는 8바이트 (opcode 1B + dst/src 1B + offset 2B + immediate 4B).

### 주요 명령어 클래스

| 클래스 | 의미 | 예 |
| --- | --- | --- |
| `BPF_LD` / `BPF_LDX` | 메모리 로드 | `r1 = *(u32 *)(r10 - 4)` |
| `BPF_ST` / `BPF_STX` | 메모리 저장 | `*(u64 *)(r10 - 8) = r2` |
| `BPF_ALU` / `BPF_ALU64` | 산술/논리 (32/64비트) | `r1 += r2` |
| `BPF_JMP` / `BPF_JMP32` | 조건/무조건 분기 | `if r1 == 0 goto +5` |
| `BPF_CALL` | helper/BPF 함수 호출 | `call bpf_get_current_pid_tgid` |
| `BPF_EXIT` | 프로그램 종료, R0가 반환값 | |

### 메모리 접근 크기

`B`(8b), `H`(16b), `W`(32b), `DW`(64b).

### 예: ALU64 더하기

```
0x07 R1 += imm    (ALU64 ADD imm)
0x0f R1 += R2     (ALU64 ADD reg)
```

직접 작성하는 일은 거의 없고, Clang이 자동 생성합니다.

---

## 레지스터와 ABI

### 11개 64비트 레지스터

| 레지스터 | 용도 |
| --- | --- |
| R0 | 반환값 (BPF_EXIT의 결과, helper 반환) |
| R1~R5 | 함수 인자 (caller-saved) |
| R6~R9 | callee-saved |
| R10 | 스택 포인터 (read-only) |

### Calling Convention

helper나 BPF 함수 호출 시:
- 인자: R1~R5
- 반환: R0
- R6~R9는 callee가 보존

### 스택

- 크기: 512바이트
- R10이 가리킴 (감소 방향 사용)
- 스택 위 메모리 접근은 verifier가 정확히 추적

---

## Verifier

eBPF의 안전성을 보장하는 핵심 컴포넌트. 모든 BPF 프로그램은 verifier를 통과해야 로드됩니다.

### 검증 항목

1. **DAG 분석**: 프로그램이 종료되는지 (Halting Problem 회피 위해 모든 경로가 BPF_EXIT 도달 보장)
2. **레지스터 상태 추적**: 각 명령어 실행 후 모든 레지스터의 가능한 값 범위
3. **메모리 안전성**: 포인터 역참조 시 그 영역이 valid한지
4. **헬퍼 호출**: 인자 타입과 컨텍스트가 올바른지
5. **권한**: 프로그램 타입이 호출하려는 helper에 접근 가능한지

### 레지스터 타입 추적

verifier는 각 레지스터를 다음 타입 중 하나로 추적:

- `SCALAR_VALUE`: 일반 정수
- `PTR_TO_STACK`: 스택 영역 포인터
- `PTR_TO_PACKET`: 패킷 데이터 포인터
- `PTR_TO_MAP_VALUE`: map 값 포인터
- `PTR_TO_CTX`: 컨텍스트 (각 프로그램 타입마다 다른 구조체)
- `NOT_INIT`: 초기화 안 됨
- ...

각 타입은 특정 연산만 허용되며, 잘못된 사용은 거부됩니다.

### 값 범위 추적

```c
if (r1 < 100) {
    // 이 분기 안에서 verifier는 r1의 범위를 [0, 99]로 압니다
    r2 = arr[r1];   // 배열 크기 100이면 안전. verifier 통과.
}
```

이 추적 덕분에 사용자가 명시적으로 bound check만 하면 verifier가 메모리 안전성을 자동 검증합니다.

### 검증 한도

- 명령어 수: 최대 100만 (5.x+)
- 검증 복잡도: 각 명령어를 평균적으로 1번 이상 분석 (분기 폭발 방지 위한 한도)

복잡한 프로그램이 거부되면 흔한 메시지:
```
math between map_value pointer and register with unbounded ...
verifier reaches one million instructions limit
```

### Verifier 로그 보기

```bash
bpftool prog load my.o /sys/fs/bpf/myprog \
  type kprobe \
  log_level 2
```

`log_level 2` 면 모든 명령어와 레지스터 상태를 출력. 디버깅 필수.

---

## JIT 컴파일러

검증을 통과한 BPF 바이트코드는 **JIT 컴파일러** 가 호스트 아키텍처의 네이티브 코드로 변환합니다.

지원 아키텍처: x86_64, arm64, riscv64, ppc64, s390x, mips, sparc64.

### 활성화 확인

```bash
sysctl net.core.bpf_jit_enable
# 1: 활성, 2: 디버그 정보 포함
```

### JIT vs 인터프리터

- JIT 활성: 거의 네이티브 속도 (네이티브 코드의 90~100%)
- 인터프리터: 5~10배 느림. 임베디드/구식 시스템에서만

### 검사

```bash
sudo bpftool prog dump xlated id <prog-id>      # 검증 후 BPF
sudo bpftool prog dump jited id <prog-id>       # JIT된 native asm
```

---

## Bounded Loop

원래 BPF는 루프를 전혀 허용하지 않았지만, Linux 5.3에서 **bounded loop** 가 추가되었습니다.

```c
int sum = 0;
for (int i = 0; i < 10; i++) {     // i가 10보다 작음을 verifier가 추적
    sum += array[i];
}
```

verifier가 루프 종료를 증명할 수 있으면 통과합니다. 증명 불가능한 패턴(예: 사용자 입력 값으로 끝나는 루프)은 거부.

### bpf_loop helper (5.17+)

```c
bpf_loop(100, callback, ctx, 0);
```

verifier 부담을 줄이는 명시적 루프 helper. callback을 N번 호출. 더 큰 루프 가능.

---

## Tail Call

한 BPF 프로그램이 **다른 BPF 프로그램으로 점프** 하는 메커니즘. JS의 `tail call` 과 비슷.

```c
SEC("xdp/dispatcher")
int dispatcher(struct xdp_md *ctx) {
    int key = classify(ctx);
    bpf_tail_call(ctx, &progs, key);
    return XDP_PASS;
}
```

`progs` 는 `BPF_MAP_TYPE_PROG_ARRAY` 타입의 map. 인덱스마다 다른 BPF 프로그램이 등록됨.

### 특징

- 점프 후 원래 함수로 돌아오지 않음
- 스택은 그대로 유지
- 최대 33회 chain (4.2+)
- 큰 프로그램을 작은 모듈로 나누는 용도

### 활용

- XDP 패킷 분류기 (프로토콜별 다른 BPF)
- iptables-like 룰 체인
- BPF 프로그램의 동적 디스패치

---

## BPF-to-BPF Function Call

5.x 이후 BPF 프로그램 안에서 **함수를 정의하고 호출** 가능. tail call과 달리 정상 호출-리턴.

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

옛날에는 `__always_inline` 으로 인라인만 가능했지만, 이제 호출 가능. JIT가 진짜 함수로 컴파일.

### 한계

- 재귀 불가
- 콜 스택 깊이 제한 (8 호출 단계)
- 현재 일부 프로그램 타입에서만 지원

---

## 참고 자료

- [BPF instruction set](https://docs.kernel.org/bpf/instruction-set.html)
- [BPF verifier](https://docs.kernel.org/bpf/verifier.html)
- [Cilium: Linux Kernel BPF guide](https://docs.cilium.io/en/stable/bpf/)
- [LWN: BPF in-kernel verifier](https://lwn.net/Articles/794934/)

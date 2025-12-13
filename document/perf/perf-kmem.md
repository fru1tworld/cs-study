# perf kmem

> 커널 메모리 할당 분석 도구

## 개요

`perf kmem`은 커널 메모리 할당자(SLAB, page allocator)의 동작을 추적하고 분석합니다. 커널 내 메모리 할당 패턴, 조각화, 콜사이트 등을 파악할 수 있습니다.

---

## 기본 사용법

```bash
# 커널 메모리 이벤트 기록
sudo perf kmem record ./my_program

# 또는 시스템 전체
sudo perf kmem record -a sleep 10

# 통계 리포트
perf kmem stat
```

---

## 서브커맨드

### record

커널 메모리 할당 이벤트를 기록합니다.

```bash
sudo perf kmem record ./my_program
```

### stat

기록된 데이터의 통계를 출력합니다.

```bash
perf kmem stat
```

---

## 주요 옵션

### 공통 옵션

| 옵션 | 설명 |
|------|------|
| `-i <file>` | 입력 파일 (기본: perf.data) |
| `-f` | 강제 실행 |
| `-v` | 상세 출력 |

### stat 옵션

| 옵션 | 설명 |
|------|------|
| `--caller` | 콜사이트별 통계 |
| `--alloc` | 할당별 통계 |
| `-s <keys>` | 정렬 키 |
| `-l <n>` | 출력 라인 수 제한 |
| `--raw-ip` | Raw IP 주소 표시 |
| `--slab` | SLAB 할당자 분석 |
| `--page` | 페이지 할당자 분석 |
| `--live` | 현재 할당된 페이지만 표시 |
| `--time <range>` | 시간 범위 필터 |

---

## 분석 모드

### SLAB 분석

SLAB 할당자(kmalloc, kmem_cache 등)를 분석합니다.

```bash
# SLAB 이벤트 기록
sudo perf kmem record --slab ./my_program

# 분석
perf kmem stat --slab
```

### 페이지 분석

페이지 할당자(alloc_pages 등)를 분석합니다.

```bash
# 페이지 이벤트 기록
sudo perf kmem record --page -a sleep 10

# 분석
perf kmem stat --page
```

### 둘 다 분석

```bash
sudo perf kmem record --slab --page -a sleep 10
perf kmem stat --slab
perf kmem stat --page
```

---

## perf kmem stat 출력

### SLAB 통계

```bash
perf kmem stat --slab
```

출력:
```
 SUMMARY (SLAB allocator)
 ========================
 Total bytes requested: 12345678
 Total bytes allocated: 15678901
 Total bytes wasted on internal fragmentation: 3333223
 Internal fragmentation: 21.25%
 Cross CPU allocations: 234/5678

 Callsite                         alloc count  Total bytes   Avg bytes   Frag
 -------------------------------- -----------  -----------   ---------   ----
 __alloc_skb                           12345     1234567       100.0    15.2%
 kmem_cache_alloc                       5678      567890       100.0    12.3%
 __kmalloc                              3456      345678       100.0    10.5%
```

### 페이지 통계

```bash
perf kmem stat --page
```

출력:
```
 SUMMARY (page allocator)
 ========================
 Total allocation requests: 12345
 Total free requests: 12340
 Total alloc+freed pages: 23456

 Callsite                         alloc count  Order   GFP flags   Migtype
 -------------------------------- -----------  -----   ---------   -------
 __page_cache_alloc                    5678       0    GFP_USER    MOVABLE
 alloc_pages_vma                       3456       0    GFP_USER    MOVABLE
 __get_free_pages                      2345       1    GFP_KERNEL  UNMOVABLE
```

### 정렬 키 (SLAB)

```bash
# 사용 가능한 정렬 키
perf kmem stat --slab -s ptr        # 포인터
perf kmem stat --slab -s callsite   # 콜사이트
perf kmem stat --slab -s bytes      # 바이트 수
perf kmem stat --slab -s hit        # 히트 수
perf kmem stat --slab -s pingpong   # Cross-CPU 할당
perf kmem stat --slab -s frag       # 조각화 (기본)
```

### 정렬 키 (페이지)

```bash
perf kmem stat --page -s page       # 페이지
perf kmem stat --page -s callsite   # 콜사이트
perf kmem stat --page -s bytes      # 바이트 수
perf kmem stat --page -s hit        # 히트 수
perf kmem stat --page -s order      # 페이지 오더
perf kmem stat --page -s migtype    # 마이그레이션 타입
perf kmem stat --page -s gfp        # GFP 플래그
```

---

## 콜사이트 vs 할당 분석

### 콜사이트별 분석 (--caller)

어떤 코드가 메모리를 할당하는지 분석합니다.

```bash
perf kmem stat --slab --caller
```

출력:
```
 Callsite                         alloc count  Total bytes   Avg   Frag
 -------------------------------- -----------  -----------   ---   ----
 __alloc_skb+0x45                      12345     1234567     100   15.2%
 copy_process+0x123                     5678      567890     100   12.3%
```

### 할당별 분석 (--alloc)

실제 메모리 할당을 분석합니다.

```bash
perf kmem stat --slab --alloc
```

---

## live 분석

현재 해제되지 않은(live) 페이지만 표시합니다.

```bash
# 기록
sudo perf kmem record --page -a sleep 10

# live 페이지만 분석
perf kmem stat --page --live
```

---

## 실용적인 예시

### SLAB 메모리 누수 추적

```bash
# 기록
sudo perf kmem record --slab -a sleep 30

# 콜사이트별 분석
perf kmem stat --slab --caller

# 조각화 확인
perf kmem stat --slab -s frag
```

### 페이지 할당 패턴 분석

```bash
# 기록
sudo perf kmem record --page -a sleep 10

# 오더별 분석
perf kmem stat --page -s order

# GFP 플래그별 분석
perf kmem stat --page -s gfp
```

### 특정 프로세스의 커널 메모리 사용

```bash
# 특정 프로그램 분석
sudo perf kmem record --slab ./my_program

# 결과 확인
perf kmem stat --slab --caller
```

### Cross-CPU 할당 분석

Cross-CPU 할당(pingpong)은 성능에 영향을 줄 수 있습니다.

```bash
# 기록
sudo perf kmem record --slab -a sleep 10

# pingpong 정렬
perf kmem stat --slab -s pingpong
```

### 내부 조각화 분석

```bash
# 조각화 순으로 정렬
perf kmem stat --slab -s frag

# 상위 10개만
perf kmem stat --slab -s frag -l 10
```

### 시간 범위 분석

```bash
# 특정 시간 범위만 분석
perf kmem stat --slab --time 10,20

# 처음 5초만
perf kmem stat --slab --time ,5
```

---

## 추적되는 이벤트

### SLAB 이벤트

- `kmem:kmalloc` - kmalloc 호출
- `kmem:kfree` - kfree 호출
- `kmem:kmem_cache_alloc` - 캐시 할당
- `kmem:kmem_cache_free` - 캐시 해제

### 페이지 이벤트

- `kmem:mm_page_alloc` - 페이지 할당
- `kmem:mm_page_free` - 페이지 해제
- `kmem:mm_page_free_batched` - 배치 해제

---

## 출력 해석

### 내부 조각화

```
Internal fragmentation: 21.25%
```

- 요청된 크기와 실제 할당 크기의 차이
- 높은 값 = 메모리 낭비

### Cross CPU 할당

```
Cross CPU allocations: 234/5678
```

- 할당한 CPU와 해제한 CPU가 다른 경우
- 캐시 효율성 저하 가능

### GFP 플래그

| 플래그 | 설명 |
|--------|------|
| `GFP_KERNEL` | 일반 커널 할당 |
| `GFP_ATOMIC` | 인터럽트 컨텍스트 |
| `GFP_USER` | 유저 페이지 |
| `GFP_HIGHUSER` | 하이 메모리 |
| `GFP_DMA` | DMA 가능 메모리 |

### 마이그레이션 타입

| 타입 | 설명 |
|------|------|
| `UNMOVABLE` | 이동 불가 |
| `MOVABLE` | 이동 가능 |
| `RECLAIMABLE` | 회수 가능 |

---

## 관련 도구 비교

| 도구 | 용도 |
|------|------|
| `perf kmem` | 커널 메모리 할당 추적 |
| `perf mem` | 메모리 접근 패턴 |
| `perf c2c` | 캐시 라인 공유 |
| `slabinfo` | SLAB 캐시 상태 |
| `/proc/meminfo` | 시스템 메모리 상태 |

---

## 트러블슈팅

### "No events found" 오류

```bash
# 트레이스포인트 확인
perf list | grep kmem

# debugfs 마운트 확인
mount | grep debugfs
```

### 권한 오류

```bash
# root 권한 필요
sudo perf kmem record -a sleep 5
```

### 심볼 해석 실패

```bash
# 커널 심볼 확인
cat /proc/kallsyms | head

# vmlinux 지정
sudo perf kmem record -k /boot/vmlinux-$(uname -r) -a sleep 5
```

---

## 커널 요구사항

### 필요한 커널 설정

```
CONFIG_TRACING=y
CONFIG_PERF_EVENTS=y
CONFIG_FTRACE=y
CONFIG_ENABLE_DEFAULT_TRACERS=y
```

### 트레이스포인트 활성화

```bash
# kmem 트레이스포인트 확인
ls /sys/kernel/debug/tracing/events/kmem/
```

---

## 참고 자료

- [perf-kmem(1) man page](https://man7.org/linux/man-pages/man1/perf-kmem.1.html)
- [Linux kernel source: tools/perf/Documentation/perf-kmem.txt](https://github.com/torvalds/linux/blob/master/tools/perf/Documentation/perf-kmem.txt)
- [Brendan Gregg's perf Examples](https://www.brendangregg.com/perf.html)

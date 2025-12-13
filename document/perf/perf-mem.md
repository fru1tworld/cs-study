# perf mem

> 메모리 접근 프로파일링 도구

## 개요

`perf mem`은 메모리 로드/스토어 작업을 샘플링하여 메모리 접근 패턴, 레이턴시, 캐시 히트/미스 등을 분석합니다. 메모리 성능 병목을 찾는 데 유용합니다.

---

## 기본 사용법

```bash
# 메모리 접근 프로파일 수집
perf mem record ./my_program

# 결과 분석
perf mem report

# 로드만 수집
perf mem record -t load ./my_program

# 스토어만 수집
perf mem record -t store ./my_program
```

---

## 주요 옵션

### record 옵션

| 옵션 | 설명 |
|------|------|
| `-t <type>` | 메모리 작업 유형 (load, store) |
| `-e <event>` | 사용할 이벤트 지정 |
| `-K` | 커널 스페이스만 |
| `-U` | 유저 스페이스만 |
| `--ldlat <n>` | 로드 레이턴시 임계값 (사이클) |

### report 옵션

| 옵션 | 설명 |
|------|------|
| `-i <file>` | 입력 파일 |
| `-C <cpu>` | 특정 CPU만 표시 |
| `-D` | Raw 샘플 덤프 |
| `-s <keys>` | 정렬 키 |
| `-F <fields>` | 출력 필드 |
| `-T` | 데이터 타입 프로파일 |
| `-U` | 심볼 미해석 항목 숨김 |
| `-x <sep>` | 필드 구분자 |

---

## 지원 플랫폼

### Intel

- **PEBS** (Precise Event-Based Sampling) 사용
- **use-latency** 측정 (파이프라인 대기 시간 포함)
- `--ldlat` 옵션으로 레이턴시 임계값 설정

### AMD

- **IBS** (Instruction-Based Sampling) Op PMU 사용
- 로드/스토어 작업 분석

### ARM64

- **SPE** (Statistical Profiling Extension) 사용
- 하드웨어 및 커널 지원 필요

---

## 출력 해석

### report 출력 예시

```
# Overhead  Samples  Local Weight  Memory access        Symbol
# ........  .......  ............  ...................  ......
    35.45%      123       45 cycles  L1 hit               hot_function
    25.23%       89      120 cycles  L3 hit               process_data
    15.67%       56      350 cycles  Local RAM            read_buffer
     8.12%       34      500 cycles  Remote RAM (1 hop)   fetch_remote
```

### 컬럼 설명

| 컬럼 | 설명 |
|------|------|
| **Overhead** | 전체 샘플 중 비율 |
| **Samples** | 샘플 수 |
| **Local Weight** | 접근 레이턴시 (사이클) |
| **Memory access** | 메모리 계층 위치 |
| **Symbol** | 함수 이름 |

### 메모리 계층 설명

| 레벨 | 설명 | 일반적 레이턴시 |
|------|------|----------------|
| **L1 hit** | L1 캐시 히트 | ~4 사이클 |
| **L2 hit** | L2 캐시 히트 | ~12 사이클 |
| **L3 hit** | L3/LLC 캐시 히트 | ~40 사이클 |
| **Local RAM** | 로컬 DRAM | ~200 사이클 |
| **Remote RAM** | 원격 NUMA 노드 DRAM | ~300+ 사이클 |

---

## 정렬 키 (-s)

```bash
# 사용 가능한 정렬 키
perf mem report -s symbol_daddr   # 데이터 주소 심볼
perf mem report -s mem            # 메모리 계층
perf mem report -s snoop          # 스눕 결과
perf mem report -s tlb            # TLB 상태
perf mem report -s locked         # 락 상태
perf mem report -s blocked        # 블록 상태
```

---

## 실용적인 예시

### 기본 메모리 프로파일링

```bash
# 메모리 접근 수집
perf mem record ./my_program

# 결과 확인
perf mem report
```

### 로드 레이턴시 분석

```bash
# 높은 레이턴시 로드만 캡처 (50 사이클 이상)
perf mem record --ldlat 50 ./my_program

# 분석
perf mem report
```

### 캐시 미스 분석

```bash
# 메모리 계층별 정렬
perf mem report -s mem

# 높은 레이턴시만 필터
perf mem report --sort=mem | grep -E "RAM|miss"
```

### NUMA 분석

```bash
# 시스템 전체 메모리 접근
sudo perf mem record -a sleep 10

# NUMA 노드별 분석
perf mem report -s mem
```

### 특정 함수의 메모리 접근

```bash
# 데이터 심볼별 분석
perf mem report -s symbol_daddr

# 특정 심볼 필터
perf mem report | grep hot_function
```

### 유저 스페이스만 분석

```bash
# 유저 스페이스 메모리 접근만
perf mem record -U ./my_program
perf mem report
```

### 커널 메모리 접근 분석

```bash
# 커널 스페이스만
sudo perf mem record -K -a sleep 10
perf mem report
```

---

## TLB 분석

```bash
# TLB 상태별 정렬
perf mem report -s tlb
```

### TLB 상태

| 상태 | 설명 |
|------|------|
| **L1 hit** | L1 TLB 히트 |
| **L2 hit** | L2 TLB 히트 |
| **Walk** | 페이지 테이블 워크 필요 |
| **Fault** | 페이지 폴트 |

---

## 데이터 타입 프로파일

Linux 커널 6.x 이상에서 데이터 타입 정보를 표시합니다.

```bash
# 데이터 타입 프로파일
perf mem report -T
```

---

## 고급 사용

### Raw 이벤트 지정

```bash
# 특정 이벤트 사용
perf mem record -e cpu/mem-loads,ldlat=30/ ./my_program
```

### Raw 샘플 덤프

```bash
# 디버깅용 raw 덤프
perf mem report -D
```

### 물리 주소 기록

```bash
# 물리 주소 기록
perf mem record -p ./my_program

# 물리 주소 포함 리포트
perf mem report --phys-data
```

### 페이지 크기 분석

```bash
# 데이터 페이지 크기 기록
perf mem record --data-page-size ./my_program
```

---

## 스눕 분석 (캐시 일관성)

```bash
# 스눕 결과별 분석
perf mem report -s snoop
```

### 스눕 상태

| 상태 | 설명 |
|------|------|
| **None** | 스눕 없음 |
| **Hit** | 다른 캐시에서 히트 |
| **Miss** | 스눕 미스 |
| **HitM** | 수정된 라인 히트 (False Sharing 가능성) |

---

## perf c2c와 연계

`perf mem`으로 메모리 문제를 식별한 후, `perf c2c`로 캐시 라인 공유 문제를 상세 분석할 수 있습니다.

```bash
# perf mem으로 문제 영역 식별
perf mem report -s mem,snoop

# HitM이 많으면 perf c2c로 상세 분석
perf c2c record -a sleep 5
perf c2c report
```

---

## 트러블슈팅

### "No samples found" 오류

```bash
# 하드웨어 지원 확인
perf list | grep mem-loads

# 대안 이벤트 사용
perf mem record -e <available_event> ./my_program
```

### 가상화 환경에서 제한

```bash
# 클라우드 VM에서는 PEBS/IBS 제한될 수 있음
# 소프트웨어 이벤트 사용
perf record -e cache-misses ./my_program
```

### 권한 오류

```bash
# root 권한 또는 paranoid 설정
sudo perf mem record ./my_program
# 또는
sudo sysctl kernel.perf_event_paranoid=-1
```

---

## 참고 자료

- [perf-mem(1) man page](https://man7.org/linux/man-pages/man1/perf-mem.1.html)
- [Red Hat - Profiling memory accesses with perf mem](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/9/html/monitoring_and_managing_system_status_and_performance/profiling-memory-accesses-with-perf-mem_monitoring-and-managing-system-status-and-performance)
- [Linux kernel source: tools/perf/Documentation/perf-mem.txt](https://github.com/torvalds/linux/blob/master/tools/perf/Documentation/perf-mem.txt)

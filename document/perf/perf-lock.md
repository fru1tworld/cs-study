# perf lock

> 커널 락 경합 분석 도구

## 개요

`perf lock`은 커널 락의 동작과 경합 상황을 분석합니다. 스핀락, 뮤텍스, rwlock 등 다양한 커널 락 유형의 대기 시간과 경합 패턴을 추적할 수 있습니다.

> **참고**: `perf lock`은 **커널 락만** 추적합니다. 유저 스페이스 락(pthread_mutex 등)은 추적하지 않습니다.

---

## 기본 사용법

```bash
# 락 이벤트 기록
sudo perf lock record ./my_program

# 또는 시스템 전체
sudo perf lock record -a sleep 5

# 통계 리포트
perf lock report

# 경합 분석 (BPF 기반, 실시간)
sudo perf lock contention -ab sleep 5
```

---

## 서브커맨드

### record

락 관련 트레이스포인트를 기록합니다.

```bash
sudo perf lock record ./my_program
```

### report

기록된 데이터의 통계를 표시합니다.

```bash
perf lock report
```

### script

Raw 락 이벤트를 출력합니다.

```bash
perf lock script
```

### info

락 인스턴스의 메타데이터를 표시합니다.

```bash
perf lock info
```

### contention

락 경합 통계를 표시합니다 (BPF 지원 시 실시간 분석 가능).

```bash
sudo perf lock contention -ab sleep 5
```

---

## 주요 옵션

### 공통 옵션

| 옵션 | 설명 |
|------|------|
| `-i <file>` | 입력 파일 |
| `-o <file>` | 출력 파일 |
| `-v` | 상세 출력 |
| `-q` | 조용한 모드 |
| `-D` | Raw 트레이스 덤프 |
| `-f` | 강제 실행 |

### report 옵션

| 옵션 | 설명 |
|------|------|
| `-k <key>` | 정렬 키 (acquired, contended, avg_wait, wait_total, wait_max, wait_min) |
| `-F <fields>` | 출력 필드 |
| `-c` | 같은 클래스 내 인스턴스 병합 |
| `-t` | 스레드별 통계 |
| `-E <n>` | 출력 항목 수 제한 |

### contention 옵션

| 옵션 | 설명 |
|------|------|
| `-a` | 시스템 전체 |
| `-b` | BPF 사용 |
| `-C <cpu>` | 특정 CPU |
| `-p <pid>` | 특정 프로세스 |
| `--tid <tid>` | 특정 스레드 |
| `-t` | 스레드별 통계 |
| `-l` | 락 주소별 통계 |
| `-o` | 락 소유자 추적 (v6.3+) |
| `-k <key>` | 정렬 키 |
| `-Y <type>` | 락 유형 필터 |
| `-L <addr>` | 특정 락 필터 |
| `-E <n>` | 출력 항목 수 제한 |
| `--max-stack <n>` | 최대 스택 깊이 |
| `--stack-skip <n>` | 스킵할 스택 엔트리 |

---

## perf lock report

### 기본 출력

```bash
perf lock report
```

출력:
```
                   Name    acquired  contended total wait (ns)  avg wait (ns)  max wait (ns)
-----------------------    --------  --------- ---------------  -------------  -------------
              &rq->lock       12345        567       123456789          2178         45678
         &sb->s_umount        5678        234        45678901          1952         23456
           tasklist_lock      3456        123        23456789          1907         12345
```

### 컬럼 설명

| 컬럼 | 설명 |
|------|------|
| **Name** | 락 이름/주소 |
| **acquired** | 락 획득 횟수 |
| **contended** | 경합 발생 횟수 |
| **total wait** | 총 대기 시간 |
| **avg wait** | 평균 대기 시간 |
| **max wait** | 최대 대기 시간 |

### 정렬 옵션

```bash
# 경합 횟수 기준
perf lock report -k contended

# 총 대기 시간 기준 (기본)
perf lock report -k wait_total

# 최대 대기 시간 기준
perf lock report -k wait_max

# 평균 대기 시간 기준
perf lock report -k avg_wait
```

---

## perf lock contention

BPF를 사용한 실시간 경합 분석입니다. 더 효율적이고 상세한 정보를 제공합니다.

### 기본 사용

```bash
# BPF 기반 실시간 분석
sudo perf lock contention -ab sleep 5
```

### 분석 모드

#### 스택 트레이스별 (기본)

```bash
sudo perf lock contention -ab sleep 5
```

출력:
```
 contended  total wait  max wait  avg wait         type  caller
 ---------  ----------  --------  --------  -----------  ------
       456    12345678     45678      2709     spinlock  __schedule+0x123
       234     5678901     23456      2427       rwlock  copy_process+0x456
```

#### 스레드별

```bash
sudo perf lock contention -abt sleep 5
```

출력:
```
 contended  total wait  max wait  avg wait   pid  comm
 ---------  ----------  --------  --------  ----  ----
       456    12345678     45678      2709  1234  my_program
       234     5678901     23456      2427  5678  worker
```

#### 락 주소별

```bash
sudo perf lock contention -abl sleep 5
```

출력:
```
 contended  total wait  max wait  avg wait  address           symbol
 ---------  ----------  --------  --------  -------           ------
       456    12345678     45678      2709  0xffff888012345678  rq_lock
       234     5678901     23456      2427  0xffff888087654321  sb_lock
```

### 락 유형 필터

```bash
# 뮤텍스만
sudo perf lock contention -abt -Y mutex sleep 5

# 스핀락만
sudo perf lock contention -abt -Y spinlock sleep 5

# 여러 유형
sudo perf lock contention -abt -Y mutex,spinlock sleep 5
```

### 지원되는 락 유형

| 유형 | 설명 |
|------|------|
| `spinlock` | 스핀락 |
| `rwlock` | 읽기-쓰기 락 |
| `rwlock:R` | rwlock 읽기 |
| `rwlock:W` | rwlock 쓰기 |
| `mutex` | 뮤텍스 |
| `rwsem` | 읽기-쓰기 세마포어 |
| `rwsem:R` | rwsem 읽기 |
| `rwsem:W` | rwsem 쓰기 |
| `semaphore` | 세마포어 |
| `rtmutex` | RT 뮤텍스 |
| `percpu-rwsem` | Per-CPU rwsem |

### 특정 락 분석

```bash
# 락 주소로 필터
sudo perf lock contention -ab -L 0xffff888012345678 sleep 5

# 락 심볼로 필터
sudo perf lock contention -ab -L rq_lock sleep 5
```

### 소유자 추적 (v6.3+)

```bash
# 락 소유자 추적 (대기자 대신)
sudo perf lock contention -abto -Y mutex sleep 5
```

---

## BPF vs Non-BPF 모드

### BPF 모드 (권장)

```bash
sudo perf lock contention -ab sleep 5
```

**장점**:
- 실시간 분석
- 낮은 오버헤드
- 심볼 해석
- 더 상세한 정보

### Non-BPF 모드

```bash
# 1단계: 기록
sudo perf lock record -a sleep 5

# 2단계: 분석
perf lock report
```

**장점**:
- BPF 미지원 시스템에서 사용 가능
- 일관된 결과 (저장된 데이터 기반)

---

## 실용적인 예시

### 락 경합 핫스팟 찾기

```bash
# 시스템 전체 경합 분석
sudo perf lock contention -ab sleep 10

# 상위 경합 확인
sudo perf lock contention -ab -E 10 sleep 10
```

### 특정 프로세스 분석

```bash
# PID로 분석
sudo perf lock contention -b -p $(pgrep my_app) sleep 10
```

### 스레드별 경합 분석

```bash
sudo perf lock contention -abt sleep 10
```

### 뮤텍스 경합만 분석

```bash
sudo perf lock contention -abt -Y mutex sleep 10
```

### 콜 스택 포함 분석

```bash
sudo perf lock contention -ab --max-stack 10 sleep 10
```

### 경합이 심한 락 상세 분석

```bash
# 먼저 전체 확인
sudo perf lock contention -abl sleep 5

# 특정 락 집중 분석
sudo perf lock contention -ab -L <lock_address> sleep 10
```

---

## CONFIG_LOCK_STAT

커널 빌트인 락 통계 기능입니다.

### 활성화

```bash
# 커널 설정 확인
grep LOCK_STAT /boot/config-$(uname -r)

# 활성화
echo 1 > /proc/sys/kernel/lock_stat

# 통계 확인
cat /proc/lock_stat
```

### 출력 항목

| 항목 | 설명 |
|------|------|
| `con-bounces` | 경합 발생 횟수 |
| `contentions` | 실제 경합 |
| `waittime-*` | 대기 시간 통계 |
| `acq-bounces` | 획득 바운스 |
| `acquisitions` | 락 획득 횟수 |
| `holdtime-*` | 보유 시간 통계 |

---

## 트러블슈팅

### "No lock events found" 오류

```bash
# 트레이스포인트 확인
perf list | grep lock

# 커널 설정 확인
grep LOCKDEP /boot/config-$(uname -r)
grep LOCK_STAT /boot/config-$(uname -r)
```

### BPF 관련 오류

```bash
# BPF 지원 확인
ls /sys/kernel/debug/tracing/events/lock

# BPF 없이 사용
sudo perf lock record -a sleep 5
perf lock report
```

### 권한 오류

```bash
# root 권한 필요
sudo perf lock contention -ab sleep 5
```

---

## 유저 스페이스 락 분석 대안

`perf lock`은 커널 락만 추적합니다. 유저 스페이스 락을 분석하려면:

### LTTng

```bash
# LTTng으로 pthread_mutex 추적
lttng create mysession
lttng enable-event -u pthread_mutex_lock
lttng start
./my_program
lttng stop
lttng view
```

### Valgrind DRD/Helgrind

```bash
# DRD로 락 분석
valgrind --tool=drd ./my_program

# Helgrind로 데드락 탐지
valgrind --tool=helgrind ./my_program
```

### perf + uprobe

```bash
# pthread_mutex_lock에 uprobe 설치
sudo perf probe -x /lib/x86_64-linux-gnu/libpthread.so.0 --add pthread_mutex_lock
sudo perf record -e probe_libpthread:pthread_mutex_lock -ag sleep 10
```

---

## 참고 자료

- [perf-lock(1) man page](https://man7.org/linux/man-pages/man1/perf-lock.1.html)
- [perf Wiki - Lock Contention](https://perfwiki.github.io/main/lock-contention/)
- [Linux Kernel Lock Statistics](https://docs.kernel.org/locking/lockstat.html)

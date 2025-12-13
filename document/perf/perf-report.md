# perf report

> 프로파일 데이터 분석 및 시각화 도구

## 개요

`perf report`는 `perf record`로 수집한 `perf.data` 파일을 분석하여 성능 프로파일을 표시합니다. 인터랙티브 TUI(ncurses) 또는 텍스트 출력을 지원합니다.

---

## 기본 사용법

```bash
# 인터랙티브 모드 (기본)
perf report

# 텍스트 출력
perf report --stdio

# 특정 파일 분석
perf report -i profile.data

# 샘플 수 포함
perf report -n
```

---

## 주요 옵션

### 입출력

| 옵션 | 설명 |
|------|------|
| `-i <file>` | 입력 파일 지정 (기본: perf.data) |
| `--stdio` | 텍스트 출력 (ncurses 대신) |
| `--tui` | TUI 모드 강제 |
| `--gtk` | GTK GUI 모드 |
| `-n` | 샘플 수 표시 |

### 정렬 및 필터링

| 옵션 | 설명 |
|------|------|
| `--sort <keys>` | 정렬 키 지정 |
| `-g` | 콜 그래프 표시 |
| `--call-graph <mode>` | 콜 그래프 표시 방식 |
| `--no-children` | 자식 오버헤드 제외 |
| `-c <comm>` | 특정 커맨드만 표시 |
| `-S <symbol>` | 특정 심볼만 표시 |
| `--dsos <dso>` | 특정 DSO만 표시 |

### 심볼 해석

| 옵션 | 설명 |
|------|------|
| `-k <vmlinux>` | 커널 심볼 파일 지정 |
| `--kallsyms <file>` | kallsyms 파일 지정 |
| `--symfs <dir>` | 심볼 파일 디렉토리 |

---

## 출력 해석

### 기본 출력 형식

```
# Overhead  Command       Shared Object        Symbol
# ........  ............  ...................  .....................
    45.23%  my_program    my_program           [.] hot_function
    20.15%  my_program    libc.so.6            [.] malloc
    15.67%  my_program    [kernel.kallsyms]    [k] do_syscall_64
```

### 컬럼 설명

| 컬럼 | 설명 |
|------|------|
| **Overhead** | 해당 함수에서 샘플된 비율 (%) |
| **Command** | 프로세스/명령어 이름 |
| **Shared Object** | 공유 라이브러리 또는 바이너리 |
| **Symbol** | 함수 이름 |

### 심볼 타입 표시

| 표시 | 설명 |
|------|------|
| `[.]` | 유저 스페이스 |
| `[k]` | 커널 |
| `[g]` | 게스트 커널 |
| `[u]` | 게스트 유저 |
| `[H]` | 하이퍼바이저 |

---

## 정렬 옵션

### 사용 가능한 정렬 키

```bash
# 기본 정렬 키
--sort comm             # 명령어
--sort dso              # 공유 객체 (라이브러리)
--sort symbol           # 함수 심볼
--sort cpu              # CPU 번호
--sort pid              # 프로세스 ID
--sort tid              # 스레드 ID
--sort parent           # 부모 심볼
--sort srcline          # 소스 라인

# 복합 정렬
--sort comm,dso,symbol
--sort cpu,comm
```

### 예시

```bash
# 공유 객체별 정렬
perf report --sort dso --stdio

# CPU별 통계
perf report --sort cpu --stdio

# 프로세스별, 라이브러리별 정렬
perf report --sort comm,dso --stdio
```

---

## 콜 그래프

### 콜 그래프 모드

```bash
# 기본 콜 그래프
perf report -g

# 그래프 형식 지정
perf report -g graph      # 그래프 형식 (기본)
perf report -g fractal    # 프랙탈 형식
perf report -g flat       # 평면 형식
perf report -g folded     # Flame Graph용 폴드 형식

# 역방향 콜 그래프 (caller 관점)
perf report -g caller

# 정방향 콜 그래프 (callee 관점)
perf report -g callee
```

### 콜 그래프 임계값

```bash
# 0.5% 이상만 표시
perf report -g graph,0.5

# caller 관점, 1% 이상
perf report -g caller,1
```

### 상세 콜 그래프 옵션

```bash
# 전체 옵션 형식
perf report --call-graph graph,0.5,caller

# 자식 오버헤드 제외
perf report -g --no-children
```

---

## 인터랙티브 모드 키보드 단축키

### 기본 탐색

| 키 | 설명 |
|----|------|
| `↑/↓` | 항목 이동 |
| `Enter` | 확장/축소 |
| `+/-` | 콜 그래프 확장/축소 |
| `/` | 필터/검색 |
| `q` | 종료 |

### 상세 분석

| 키 | 설명 |
|----|------|
| `a` | annotate (어셈블리 보기) |
| `s` | 스크립트 보기 |
| `h` | 도움말 |
| `t` | 스레드 보기 토글 |
| `d` | DSO 필터 |

### 정렬

| 키 | 설명 |
|----|------|
| `P` | 부모 기준 정렬 |
| `E` | 오버헤드 기준 정렬 |

---

## 필터링

### 특정 프로세스/스레드

```bash
# 특정 명령어만
perf report -c my_program

# 특정 PID
perf report --pid 1234

# 특정 TID
perf report --tid 5678
```

### 특정 심볼/DSO

```bash
# 특정 심볼만
perf report -S malloc

# 특정 DSO만
perf report --dsos libc.so.6

# 커널 제외
perf report --dsos '[^\\[].*'
```

### 시간 범위 필터

```bash
# 특정 시간 범위
perf report --time 10%/3

# 시작~끝 시간
perf report --time 0,10
```

---

## 출력 형식

### stdio 출력 옵션

```bash
# 기본 stdio
perf report --stdio

# 샘플 수 포함
perf report --stdio -n

# 퍼센트 형식
perf report --stdio --percent-limit 1

# 헤더만 표시
perf report --header-only
```

### CSV/파싱용 출력

```bash
# 필드 구분자 지정
perf report --stdio -x ','

# 특정 필드만
perf report --stdio --fields overhead,symbol
```

### Flame Graph용 출력

```bash
# folded 형식
perf report --stdio -n -g folded

# perf script 사용 (권장)
perf script | stackcollapse-perf.pl | flamegraph.pl > flame.svg
```

---

## 고급 분석

### 브랜치 분석

```bash
# 브랜치 프로파일 분석
perf report --branch-history
```

### 메모리 분석

```bash
# 메모리 접근 프로파일
perf report --mem-mode
```

### 소스 라인 분석

```bash
# 소스 라인별 분석
perf report --sort srcline

# 소스 파일별
perf report --sort srcfile
```

---

## 실용적인 예시

### 핫 함수 찾기

```bash
# 상위 10개 함수
perf report --stdio | head -20

# 유저 스페이스만
perf report --stdio --dsos-filter '[^\\[].*' | head -20
```

### 라이브러리별 분석

```bash
# 어떤 라이브러리가 CPU를 많이 사용하는지
perf report --sort dso --stdio
```

### CPU별 분석

```bash
# CPU별 부하 분포
perf report --sort cpu --stdio
```

### 콜 체인 분석

```bash
# 누가 malloc을 호출하는지
perf report -g caller --stdio | grep -A 20 malloc
```

### 분석 자동화

```bash
# 스크립트용 출력
perf report --stdio -n --percent-limit 0.1 \
  --sort comm,dso,symbol \
  | awk 'NR>3 {print}' > report.txt
```

---

## 심볼 해석 문제 해결

### 커널 심볼

```bash
# vmlinux 파일 지정
perf report -k /boot/vmlinux-$(uname -r)

# kallsyms 활성화
sudo sysctl kernel.kptr_restrict=0
```

### 유저 스페이스 심볼

```bash
# 심볼 파일 디렉토리 지정
perf report --symfs /path/to/symbols

# 디버그 패키지 설치 (예: Ubuntu)
sudo apt install libc6-dbg
```

### 주소만 표시되는 경우

```bash
# build-id 확인
perf buildid-list -i perf.data

# 캐시된 심볼 확인
ls ~/.debug/
```

---

## diff 분석

```bash
# 두 프로파일 비교
perf diff perf.data.old perf.data.new

# 자세한 diff
perf diff --stdio perf.data.old perf.data.new
```

---

## 참고 자료

- [perf-report(1) man page](https://man7.org/linux/man-pages/man1/perf-report.1.html)
- [perf Wiki - Tutorial](https://perfwiki.github.io/main/tutorial/)
- [Brendan Gregg's perf Examples](https://www.brendangregg.com/perf.html)

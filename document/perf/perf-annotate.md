# perf annotate

> 소스 코드/어셈블리 레벨 성능 분석 도구

## 개요

`perf annotate`는 `perf record`로 수집한 데이터를 기반으로 소스 코드나 어셈블리 명령어 수준에서 성능 병목을 분석합니다. 각 명령어에 샘플이 몇 번 수집되었는지 표시합니다.

---

## 기본 사용법

```bash
# 먼저 프로파일 데이터 수집
perf record -g ./my_program

# 인터랙티브 모드로 분석
perf annotate

# 특정 함수만 분석
perf annotate <function_name>

# stdio 출력
perf annotate --stdio

# 특정 심볼 분석
perf annotate --symbol=hot_function
```

---

## 주요 옵션

### 입력/출력

| 옵션 | 설명 |
|------|------|
| `-i <file>` | 입력 파일 지정 (기본: perf.data) |
| `--stdio` | 텍스트 출력 모드 |
| `--tui` | TUI 모드 (기본) |
| `--gtk` | GTK GUI 모드 |

### 심볼 지정

| 옵션 | 설명 |
|------|------|
| `-s <symbol>` | 특정 심볼(함수)만 분석 |
| `--symbol <name>` | `-s`와 동일 |
| `-d <dso>` | 특정 DSO(라이브러리)만 분석 |

### 소스 코드

| 옵션 | 설명 |
|------|------|
| `-l` | 소스 라인 번호 표시 |
| `--source` | 소스 코드 표시 |
| `--no-source` | 소스 코드 숨기기 |
| `--symfs <dir>` | 심볼/소스 파일 디렉토리 |
| `--objdump <path>` | objdump 경로 지정 |

### 어셈블리 옵션

| 옵션 | 설명 |
|------|------|
| `--asm-raw` | raw 명령어 바이트 표시 |
| `--show-nr-samples` | 샘플 수 표시 |
| `--show-total-period` | 총 기간 표시 |

### 커널 심볼

| 옵션 | 설명 |
|------|------|
| `-k <vmlinux>` | vmlinux 파일 지정 |
| `--kallsyms <file>` | kallsyms 파일 지정 |

---

## 출력 해석

### 어셈블리 출력 예시

```
 Percent |      Source code & Disassembly of my_program
---------|----------------------------------------------
         :
         :      void hot_function(int* arr, int n) {
   15.23 :  400510:   push   %rbp
    0.00 :  400511:   mov    %rsp,%rbp
         :          for (int i = 0; i < n; i++) {
    5.67 :  400514:   mov    -0x14(%rbp),%eax
   45.89 :  400517:   cmp    -0x18(%rbp),%eax
    0.12 :  40051a:   jge    400530
         :              sum += arr[i];
   20.34 :  40051c:   mov    -0x14(%rbp),%eax
   12.75 :  400520:   cltq
    0.00 :  400522:   lea    0x0(,%rax,4),%rdx
```

### 컬럼 설명

| 컬럼 | 설명 |
|------|------|
| **Percent** | 해당 명령어에서 샘플된 비율 (%) |
| **Address** | 메모리 주소 |
| **Instruction** | 어셈블리 명령어 |
| **Source** | 소스 코드 (가능한 경우) |

### 높은 퍼센트의 의미

- 높은 퍼센트 = 해당 명령어에서 CPU 시간을 많이 소비
- 메모리 접근 명령어가 높으면 = 캐시 미스 가능성
- 비교/점프 명령어가 높으면 = 분기 예측 실패 가능성

---

## 인터랙티브 모드 단축키

### 탐색

| 키 | 설명 |
|----|------|
| `↑/↓` | 라인 이동 |
| `Enter` | 선택 |
| `/` | 검색 |
| `q` | 종료 |
| `h` | 도움말 |

### 분석

| 키 | 설명 |
|----|------|
| `s` | 심볼 선택 |
| `k` | 라인 토글 (소스/어셈블리) |
| `o` | 출력 옵션 |
| `t` | 총 기간 표시 토글 |

---

## 실용적인 예시

### 기본 분석 워크플로우

```bash
# 1. 디버그 정보 포함 컴파일
gcc -g -O2 my_program.c -o my_program

# 2. 프로파일 수집
perf record -g ./my_program

# 3. 핫 함수 확인
perf report --stdio | head -20

# 4. 특정 함수 상세 분석
perf annotate hot_function --stdio
```

### 소스 코드와 함께 분석

```bash
# 소스 코드 포함 분석
perf annotate --source -s hot_function

# 소스 파일 디렉토리 지정
perf annotate --symfs /path/to/source --source
```

### stdio 출력으로 저장

```bash
# 파일로 저장
perf annotate --stdio > annotation.txt

# 특정 함수만
perf annotate --stdio -s hot_function > hot_function.txt
```

### 라이브러리 함수 분석

```bash
# libc 함수 분석
perf annotate -d libc.so.6 --stdio

# 특정 라이브러리 함수
perf annotate -d libc.so.6 -s malloc --stdio
```

### 커널 함수 분석

```bash
# 커널 함수 분석
sudo perf annotate -k /boot/vmlinux-$(uname -r) --stdio

# 특정 커널 함수
sudo perf annotate -k /boot/vmlinux-$(uname -r) -s do_syscall_64 --stdio
```

---

## 디버그 심볼 요구사항

### 소스 코드 주석을 위해 필요한 것

1. **컴파일 시 `-g` 옵션**
   ```bash
   gcc -g -O2 program.c -o program
   ```

2. **디버그 패키지 설치**
   ```bash
   # Ubuntu/Debian
   sudo apt install libc6-dbg
   sudo apt install linux-image-$(uname -r)-dbgsym

   # CentOS/RHEL
   sudo debuginfo-install glibc
   ```

3. **Frame Pointer 보존** (권장)
   ```bash
   gcc -g -fno-omit-frame-pointer program.c -o program
   ```

---

## 고급 사용

### 여러 이벤트 분석

```bash
# 캐시 미스 프로파일 후 분석
perf record -e cache-misses ./my_program
perf annotate --stdio
```

### 퍼센트 임계값 설정

```bash
# 0.1% 이상만 표시
perf annotate --stdio --min-percent=0.1
```

### 특정 DSO만 분석

```bash
# 내 프로그램만
perf annotate -d my_program --stdio

# 커널 제외
perf annotate -d '[^\\[].*' --stdio
```

---

## perf report에서 annotate 접근

`perf report` 인터랙티브 모드에서 `a` 키를 누르면 선택한 함수에 대해 annotate를 실행합니다.

```bash
perf report
# 함수 선택 후 'a' 키 입력
```

---

## 트러블슈팅

### "no symbol found" 오류

```bash
# 디버그 심볼 확인
file my_program
# 출력에 "not stripped" 확인

# objdump로 심볼 확인
objdump -t my_program | head
```

### 소스 코드가 표시되지 않음

```bash
# -g 옵션으로 재컴파일
gcc -g program.c -o program

# 소스 경로 지정
perf annotate --symfs /path/to/source
```

### 커널 심볼 해석 실패

```bash
# kptr_restrict 해제
sudo sysctl kernel.kptr_restrict=0

# vmlinux 파일 지정
sudo perf annotate -k /boot/vmlinux-$(uname -r)
```

### 주소만 표시되는 경우

```bash
# objdump 경로 확인
which objdump

# objdump 명시적 지정
perf annotate --objdump=/usr/bin/objdump
```

---

## 최적화 힌트 찾기

### 캐시 미스 찾기

```bash
# L1 캐시 미스 프로파일
perf record -e L1-dcache-load-misses ./my_program
perf annotate --stdio

# 높은 퍼센트의 메모리 접근 명령어 확인
```

### 분기 예측 실패 찾기

```bash
# 분기 미스 프로파일
perf record -e branch-misses ./my_program
perf annotate --stdio

# 높은 퍼센트의 조건 점프 명령어 확인
```

### 명령어 캐시 미스

```bash
perf record -e L1-icache-load-misses ./my_program
perf annotate --stdio
```

---

## 참고 자료

- [perf-annotate(1) man page](https://man7.org/linux/man-pages/man1/perf-annotate.1.html)
- [perf Wiki - Tutorial](https://perfwiki.github.io/main/tutorial/)
- [Brendan Gregg's perf Examples](https://www.brendangregg.com/perf.html)

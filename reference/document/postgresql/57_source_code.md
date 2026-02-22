# PostgreSQL 소스 코드 (Source Code)

## 개요

PostgreSQL은 오픈 소스 관계형 데이터베이스 관리 시스템으로, 잘 구조화된 소스 코드 기반을 가지고 있습니다. 이 문서는 PostgreSQL 소스 코드의 구조, 주요 디렉토리, 빌드 시스템, 그리고 코딩 규칙에 대해 설명합니다.

---

## 1. 소스 코드 디렉토리 구조 (Directory Structure)

### 1.1 최상위 `src` 디렉토리

PostgreSQL 소스 코드의 핵심은 `src` 디렉토리에 있으며, 다음과 같은 주요 하위 디렉토리로 구성됩니다:

| 디렉토리 | 설명 |
|---------|------|
| `backend` | 핵심 데이터베이스 엔진 및 서버 기능 |
| `bin` | 명령줄 유틸리티 및 실행 파일 |
| `common` | 프론트엔드와 백엔드에서 공유하는 공통 코드 |
| `fe_utils` | 클라이언트 애플리케이션용 프론트엔드 유틸리티 |
| `include` | 헤더 파일 및 선언 |
| `interfaces` | 데이터베이스 인터페이스 구현 (예: libpq) |
| `pl` | 절차적 언어 지원 (PL/pgSQL, PL/Perl 등) |
| `port` | 플랫폼 특화 이식성 코드 |
| `test` | 테스트 유틸리티 및 테스트 코드 |
| `timezone` | 시간대 처리 및 데이터 |
| `tools` | 개발 및 유지보수 도구 |
| `tutorial` | 교육 자료 및 예제 |

### 1.2 `src/backend` 디렉토리 상세

백엔드 디렉토리는 PostgreSQL 서버의 핵심을 구성하며, 25개 이상의 하위 디렉토리를 포함합니다:

| 디렉토리 | 설명 |
|---------|------|
| `access` | 데이터베이스 액세스 방법 (힙, 인덱스 등) |
| `archive` | 아카이브 기능 |
| `backup` | 백업 작업 |
| `bootstrap` | 데이터베이스 초기화 절차 |
| `catalog` | 시스템 카탈로그 관리 |
| `commands` | SQL 명령어 처리 |
| `executor` | 쿼리 실행 엔진 |
| `foreign` | 외부 데이터 래퍼(FDW) 지원 |
| `jit` | JIT(Just-In-Time) 컴파일 |
| `lib` | 라이브러리 유틸리티 |
| `libpq` | PostgreSQL 클라이언트 라이브러리 |
| `main` | 주 서버 기능 |
| `nodes` | 파스 트리 노드 구조 |
| `optimizer` | 쿼리 최적화 |
| `parser` | SQL 파싱 |
| `partitioning` | 테이블 파티셔닝 |
| `port` | 플랫폼 특화 코드 |
| `postmaster` | 서버 프로세스 관리 |
| `regex` | 정규 표현식 지원 |
| `replication` | 복제 기능 |
| `rewrite` | 쿼리 재작성 규칙 |
| `snowball` | Snowball 스테머 지원 |
| `statistics` | 쿼리 통계 |
| `storage` | 데이터 저장소 관리 |
| `tcop` | 최상위 명령 처리 |
| `tsearch` | 전문 검색(Full-Text Search) |
| `utils` | 유틸리티 함수 |

### 1.3 프론트엔드와 백엔드 코드 분리

PostgreSQL에서 프론트엔드와 백엔드 코드는 명확히 분리됩니다. 프론트엔드 변경사항은 주로 `src/bin` 또는 `src/fe_utils` 디렉토리에 위치합니다.

공유 코드의 경우, `#ifdef FRONTEND` 전처리기 지시문을 사용하여 프론트엔드와 백엔드 로직을 분리합니다:

```c
#ifndef FRONTEND
static inline MemoryContext
MemoryContextSwitchTo(MemoryContext context)
{
    MemoryContext old = CurrentMemoryContext;
    CurrentMemoryContext = context;
    return old;
}
#endif   /* FRONTEND */
```

---

## 2. 코딩 규칙 (Coding Conventions)

### 2.1 포맷팅 (Formatting)

PostgreSQL은 BSD 스타일의 코드 포맷팅을 따릅니다:

- 탭 간격: 4열 탭 간격 사용 (탭을 공백으로 확장하지 않음)
- 들여쓰기: 각 논리적 들여쓰기 레벨마다 하나의 탭 스탑 추가
- 중괄호: 제어 블록(`if`, `while`, `switch` 등)의 중괄호는 별도의 줄에 배치
- 줄 길이: 80열 창에서의 가독성을 위해 줄 길이 제한

```c
/* 올바른 포맷팅 예시 */
if (condition)
{
    /* 코드 블록 */
    do_something();
}
else
{
    /* 다른 코드 블록 */
    do_something_else();
}
```

### 2.2 주석 스타일 (Comment Style)

C++ 스타일 주석(`//`)은 사용하지 않습니다. `pgindent`가 이를 `/* ... */` 형태로 대체합니다.

단일 줄 주석:
```c
/* 이것은 단일 줄 주석입니다 */
```

여러 줄 주석:
```c
/*
 * 주석 텍스트가 여기서 시작하고
 * 여기서 계속됩니다
 */
```

줄 바꿈 보존이 필요한 들여쓰기된 주석:
```c
/*----------
 * 주석 텍스트가 여기서 시작하고
 * 여기서 계속됩니다
 *----------
 */
```

### 2.3 C 표준 (C Standard)

PostgreSQL 코드는 C99 표준 기능만을 사용해야 합니다.

금지된 C99 기능:
- 가변 길이 배열 (Variable Length Arrays)
- 선언과 코드의 혼합
- `//` 주석
- 유니버설 문자 이름

폴백(fallback)과 함께 허용되는 비-C99 기능:
- `_Static_assert()` - C99 호환 대체로 폴백
- `__builtin_constant_p` - GCC 확장으로 폴백 있음

### 2.4 함수형 매크로와 인라인 함수

`static inline` 함수가 선호되는 경우:
- 매크로 형태에서 다중 평가 위험이 있을 때
- 매크로가 매우 길 때

```c
/* 인라인 함수 예시 */
static inline int
Max(int a, int b)
{
    return (a > b) ? a : b;
}
```

매크로가 필요하거나 더 쉬운 경우:
- 다양한 타입을 매크로에 전달해야 할 때
- 표현식 다형성이 필요할 때

### 2.5 시그널 핸들러 (Signal Handlers)

시그널 핸들러는 인터럽트 위험 때문에 매우 조심스럽게 작성되어야 합니다:

- async-signal-safe 함수만 호출 가능 (POSIX 정의)
- `volatile sig_atomic_t` 변수에만 접근 가능
- `SetLatch()`는 PostgreSQL에서 signal-safe로 간주됨

최선의 관행: 최소한의 작업만 수행 - 시그널 도착만 기록하고 외부 코드를 깨움:

```c
static void
handle_sighup(SIGNAL_ARGS)
{
    got_SIGHUP = true;
    SetLatch(MyLatch);
}
```

### 2.6 함수 포인터 호출

단순 변수의 경우: `(*pointer)()` 표기법으로 명시적 역참조:
```c
(*emit_log_hook)(edata);
```

구조체 멤버의 경우: 추가 구두점 생략:
```c
paramInfo->paramFetch(paramInfo, paramId);
```

---

## 3. 서버 내 오류 보고 (Reporting Errors Within the Server)

### 3.1 ereport 함수

서버 코드 내에서 생성되는 오류, 경고, 로그 메시지는 `ereport` 또는 이전 버전인 `elog`를 사용하여 생성해야 합니다.

필수 요소:
1. 심각도 레벨 (Severity Level): `DEBUG`부터 `PANIC`까지 (`src/include/utils/elog.h`에 정의)
2. 기본 메시지 텍스트 (Primary Message Text): 주요 오류 설명
3. 선택적 요소: 오류 식별자 코드 (SQLSTATE), 힌트, 세부 정보 등

### 3.2 기본 사용법

간단한 예시:
```c
ereport(ERROR,
        errcode(ERRCODE_DIVISION_BY_ZERO),
        errmsg("division by zero"));
```

복잡한 예시:
```c
ereport(ERROR,
        errcode(ERRCODE_AMBIGUOUS_FUNCTION),
        errmsg("function %s is not unique",
               func_signature_string(funcname, nargs,
                                     NIL, actual_arg_types)),
        errhint("Unable to choose a best candidate function. "
                "You might need to add explicit typecasts."));
```

### 3.3 심각도에 따른 동작

- ERROR 이상의 심각도: 현재 쿼리 실행을 중단하고 반환하지 않음
- ERROR 미만: 정상적으로 반환

### 3.4 보조 함수 (Auxiliary Functions)

| 함수 | 목적 |
|-----|------|
| `errcode(sqlerrcode)` | SQLSTATE 오류 식별자 코드 지정 |
| `errmsg(msg, ...)` | sprintf 스타일 형식 코드를 사용한 기본 오류 메시지; `%m`은 `strerror()` 지원 |
| `errmsg_internal(msg, ...)` | 내부 오류용 비번역 메시지 |
| `errmsg_plural(fmt_singular, fmt_plural, n, ...)` | 복수형 지원 메시지 |
| `errdetail(msg, ...)` | 선택적 추가 세부 정보 |
| `errdetail_internal(msg, ...)` | 비번역 세부 메시지 |
| `errdetail_log(msg, ...)` | 서버 로그에만 전송되는 세부 정보 (클라이언트 제외) |
| `errhint(msg, ...)` | 문제 해결을 위한 제안 |
| `errcontext(msg, ...)` | 컨텍스트 정보 (콜백 함수에서 사용) |
| `errposition(int)` | 쿼리 문자열에서 오류의 텍스트 위치 |
| `errtable(Relation)` | 오류와 릴레이션 이름/스키마 연결 |
| `errtablecol(Relation, attnum)` | 오류와 컬럼 연결 |
| `errtableconstraint(Relation, conname)` | 오류와 제약조건 연결 |
| `errdatatype(Oid)` | 오류와 데이터 타입 연결 |
| `errdomainconstraint(Oid, conname)` | 오류와 도메인 제약조건 연결 |
| `errcode_for_file_access()` | 파일 시스템 오류에 적합한 SQLSTATE 선택 |
| `errcode_for_socket_access()` | 소켓 오류에 적합한 SQLSTATE 선택 |
| `errhidestmt(bool)` | 로그에서 STATEMENT 부분 숨김 |
| `errhidecontext(bool)` | 로그에서 CONTEXT 부분 숨김 |

### 3.5 기본 SQLSTATE 코드

`errcode()`가 생략된 경우:
- ERROR 이상: `ERRCODE_INTERNAL_ERROR`
- WARNING: `ERRCODE_WARNING`
- NOTICE 이하: `ERRCODE_SUCCESSFUL_COMPLETION`

### 3.6 elog() - 레거시 함수

```c
elog(level, "format string", ...);
```

다음과 동일:
```c
ereport(level, errmsg_internal("format string", ...));
```

특성:
- SQLSTATE 코드가 항상 기본값
- 메시지가 번역 대상이 아님
- 내부 오류 및 저수준 디버그 로깅에만 사용
- 표기법의 단순함으로 인해 "발생할 수 없는" 오류 체크에 선호됨

---

## 4. 오류 메시지 스타일 가이드 (Error Message Style Guide)

### 4.1 메시지 구조

| 메시지 유형 | 설명 |
|------------|------|
| Primary (기본) | 짧고, 사실적이며, 한 줄; 구현 세부사항 회피 |
| Detail (세부) | 구현 세부사항, 시스템 호출, 기술 정보 |
| Hint (힌트) | 문제 해결을 위한 제안 |

예시:
```
잘못된 방식:
IpcMemoryCreate: shmget(key=%d, size=%u, 0%o) failed: %m
(기본적으로 힌트인 긴 부록 포함)

올바른 방식:
Primary:    could not create shared memory segment: %m
Detail:     Failed syscall was shmget(key=%d, size=%u, 0%o).
Hint:       부록을 완전한 문장으로 작성.
```

### 4.2 문법 및 구두점

| 메시지 유형 | 규칙 |
|------------|------|
| Primary | 대문자화 없음, 마침표 없음, 느낌표 없음 |
| Detail/Hint | 완전한 문장, 대문자화, 끝에 마침표, 문장 사이 공백 두 개 |
| Context | 대문자화 없음, 마침표 없음 |

### 4.3 시제 사용

- 과거 시제 ("could not"): 나중에 성공할 수 있는 일시적 실패
- 현재 시제 ("cannot"): 영구적, 지속적인 조건

```c
could not open file "%s": %m     /* 다음에는 작동할 수 있음 */
cannot open file "%s"            /* 불가능한 작업 */
```

### 4.4 피해야 할 단어

| 단어 | 문제 | 대안 |
|-----|------|------|
| Unable | 수동태에 가까움 | "cannot" 또는 "could not" |
| Bad | 모호함 | 이유 명시 (예: "invalid format") |
| Illegal | 잘못된 법률 용어 | "invalid" + 설명 |
| Unknown | 모호함 | "unrecognized" + 값 표시 |
| Contractions | 비격식적 | "can't" 대신 "cannot" |
| Non-negative | 모호함 | "greater than zero" 또는 "≥ zero" |

---

## 5. 빌드 시스템 (Build System)

### 5.1 Meson 빌드 시스템

PostgreSQL 16 이상에서는 Meson 빌드 시스템을 지원합니다. Meson은 Ninja를 기본 백엔드로 사용하는 현대적인 빌드 시스템입니다.

빠른 시작:
```bash
# 빌드 디렉토리 설정
meson setup build --prefix=/usr/local/pgsql

# 빌드
cd build
ninja

# 설치
su
ninja install

# 사용자 추가 및 데이터 디렉토리 생성
adduser postgres
mkdir -p /usr/local/pgsql/data
chown postgres /usr/local/pgsql/data

# 데이터베이스 초기화 및 시작
su - postgres
/usr/local/pgsql/bin/initdb -D /usr/local/pgsql/data
/usr/local/pgsql/bin/pg_ctl -D /usr/local/pgsql/data -l logfile start
```

### 5.2 설치 절차

1. 구성:
```bash
# 기본 설정
meson setup build

# 사용자 정의 접두사
meson setup build --prefix=/home/user/pg-install

# 디버그 빌드
meson setup build --buildtype=debug

# OpenSSL 지원
meson setup build -Dssl=openssl

# 초기 설정 후 재구성
meson configure -Dcassert=true
```

2. 빌드:
```bash
ninja
# 자동으로 병렬 빌드 지원; -j 플래그로 오버라이드 가능
```

3. 회귀 테스트:
```bash
meson test
# 실행 중인 인스턴스에 대해:
meson test --setup running
```

4. 설치:
```bash
ninja install
# 또는 더 많은 옵션과 함께:
meson install
```

유지보수 명령:
```bash
ninja uninstall    # 설치 제거
ninja clean        # 빌드 산출물 제거
```

### 5.3 구성 옵션

#### 설치 위치
- `--prefix=PREFIX` - 기본 설치 디렉토리 (기본값: `/usr/local/pgsql`)
- `--bindir=DIRECTORY` - 실행 파일 (기본값: `PREFIX/bin`)
- `--libdir=DIRECTORY` - 라이브러리 (기본값: `PREFIX/lib`)
- `--includedir=DIRECTORY` - 헤더 (기본값: `PREFIX/include`)
- `--datadir=DIRECTORY` - 데이터 파일 (기본값: `PREFIX/share`)
- `--sysconfdir=DIRECTORY` - 구성 파일 (기본값: `PREFIX/etc`)
- `--mandir=DIRECTORY` - Man 페이지 (기본값: `DATADIR/man`)
- `--localedir=DIRECTORY` - 로케일 데이터 (기본값: `DATADIR/locale`)

#### PostgreSQL 기능
| 옵션 | 설명 |
|-----|------|
| `-Dnls={auto\|enabled\|disabled}` | 네이티브 언어 지원 |
| `-Dplperl={auto\|enabled\|disabled}` | PL/Perl 언어 |
| `-Dplpython={auto\|enabled\|disabled}` | PL/Python 언어 |
| `-Dpltcl={auto\|enabled\|disabled}` | PL/Tcl 언어 |
| `-Dicu={auto\|enabled\|disabled}` | ICU 콜레이션 지원 |
| `-Dllvm={auto\|enabled\|disabled}` | LLVM JIT 컴파일 |
| `-Dlz4={auto\|enabled\|disabled}` | LZ4 압축 |
| `-Dzstd={auto\|enabled\|disabled}` | Zstandard 압축 |
| `-Dssl={auto\|openssl}` | SSL/TLS 지원 |
| `-Dgssapi={auto\|enabled\|disabled}` | GSSAPI 인증 |
| `-Dldap={auto\|enabled\|disabled}` | LDAP 지원 |
| `-Dpam={auto\|enabled\|disabled}` | PAM 인증 |
| `-Dsystemd={auto\|enabled\|disabled}` | systemd 통합 |
| `-Dbonjour={auto\|enabled\|disabled}` | Bonjour 검색 |
| `-Duuid=LIBRARY` | UUID 지원 (`none\|bsd\|e2fs\|ossp`) |
| `-Dlibxml={auto\|enabled\|disabled}` | XML 지원 |
| `-Dlibxslt={auto\|enabled\|disabled}` | XSLT 변환 |

#### 서버 튜닝
| 옵션 | 설명 |
|-----|------|
| `-Dpgport=NUMBER` | 기본 포트 (기본값: 5432) |
| `-Dblocksize=BLOCKSIZE` | 블록 크기 KB (기본값: 8) |
| `-Dwal_blocksize=BLOCKSIZE` | WAL 블록 크기 KB (기본값: 8) |
| `-Dsegsize=SEGSIZE` | 세그먼트 크기 GB (기본값: 1) |

#### 개발자 옵션
| 옵션 | 설명 |
|-----|------|
| `--buildtype=BUILDTYPE` | 빌드 타입 (`plain\|debug\|debugoptimized\|release`) |
| `--debug` | 디버깅 심볼 포함 |
| `--optimization=LEVEL` | 최적화 레벨 (0,g,1,2,3,s) |
| `-Dcassert={true\|false}` | 어서션 체크 |
| `-Ddtrace={auto\|enabled\|disabled}` | DTrace 지원 |
| `-Dtap_tests={auto\|enabled\|disabled}` | TAP 테스트 도구 |

### 5.4 Meson 빌드 타겟

코드 타겟:
```bash
ninja all        # 문서를 제외한 모든 것
ninja backend    # 백엔드와 모듈
ninja bin        # 프론트엔드 바이너리
ninja contrib    # Contrib 모듈
ninja pl         # 절차적 언어
```

문서 타겟:
```bash
ninja html       # 멀티페이지 HTML
ninja man        # Man 페이지
ninja docs       # HTML과 man 페이지
ninja alldocs    # 모든 형식
```

설치 타겟:
```bash
ninja install           # 문서 제외
ninja install-docs      # 문서만
ninja install-world     # 모든 것
ninja install-quiet     # 조용한 설치
ninja uninstall         # 파일 제거
```

기타 타겟:
```bash
ninja clean    # 빌드 산출물 제거
ninja test     # 모든 테스트 실행
ninja world    # 문서 포함 모든 것
```

### 5.5 Autoconf 빌드 시스템 (레거시)

PostgreSQL은 여전히 전통적인 GNU Autoconf 기반 빌드 시스템도 지원합니다:

```bash
# 구성
./configure --prefix=/usr/local/pgsql

# 빌드
make

# 테스트
make check

# 설치
make install
```

주요 사항:
- `configure.in`을 편집 (생성된 `configure`가 아님)
- 변경 후 `autoconf` 실행
- `make distclean`으로 모든 파생 파일 제거
- 자동 헤더 의존성 추적을 위해 `--enable-depend` 플래그 사용

---

## 6. 개발 도구 (Development Tools)

### 6.1 pgindent

`pgindent`는 PostgreSQL 코딩 표준에 맞게 코드를 자동으로 재포맷하는 도구입니다.

위치: `src/tools/pgindent`

사용 목적:
- 코드 스타일 일관성 유지
- 패치 제출 전 코드 정리
- 릴리스 전 자동 포맷팅

### 6.2 에디터 설정

`src/tools/editors` 디렉토리에는 PostgreSQL 코딩 표준을 준수하는 데 도움이 되는 샘플 에디터 설정이 있습니다:
- Emacs
- XEmacs
- Vim

### 6.3 기타 개발 도구

| 도구 | 설명 |
|-----|------|
| `make_ctags` / `make_etags` | 태그 파일 생성 |
| `pginclude` | 인클루드 파일 관리 스크립트 |
| `find_static` | 정적 분석 도구 |
| `find_typedef` | typedef 찾기 도구 |
| `find_badmacros` | 잘못된 매크로 검출 |

### 6.4 OID 관리 도구

`src/include/catalog` 디렉토리에는 OID 관리를 위한 도구가 있습니다:
- `unused_oids` - 사용 가능한 OID 할당 식별
- `duplicate_oids` - OID 충돌 감지

---

## 7. 파서 컴포넌트 (Parser Components)

PostgreSQL 파서는 `src/backend/parser` 디렉토리에 위치합니다:

| 파일 | 설명 |
|-----|------|
| `scan.l` | SQL을 토큰화하는 렉서 |
| `gram.y` | BNF 표기법의 문법 정의 |

이들은 flex와 bison 도구에 의해 생성됩니다.

---

## 8. 회귀 테스트 (Regression Testing)

PostgreSQL은 SQL 구현을 검증하고 시스템 발전에 따른 호환성을 보장하기 위해 설계된 내장 회귀 테스트 프레임워크를 가지고 있습니다.

```bash
# Meson으로 테스트 실행
meson test

# make로 테스트 실행
make check

# 병렬 테스트
make check PROVE_FLAGS=-j4
```

테스트 디렉토리: `src/test`

---

## 9. 설치된 디렉토리 구조

PostgreSQL을 빌드하고 설치한 후의 디렉토리 구조:

| 디렉토리 | 설명 |
|---------|------|
| `bin` | PostgreSQL 실행 파일 (psql, initdb, pg_ctl, 서버 바이너리) |
| `include` | 확장이나 애플리케이션 컴파일에 필요한 C 헤더 파일 |
| `lib` | PostgreSQL과 클라이언트 애플리케이션이 사용하는 공유 라이브러리 |
| `share` | 시간대 데이터, 로케일 데이터, SQL 스크립트 등 |

---

## 10. 예제 코드

### 10.1 간단한 오류 보고

```c
#include "postgres.h"
#include "utils/elog.h"

void
example_function(int value)
{
    if (value < 0)
    {
        ereport(ERROR,
                errcode(ERRCODE_INVALID_PARAMETER_VALUE),
                errmsg("value must be non-negative"),
                errdetail("Received value: %d", value),
                errhint("Provide a value greater than or equal to zero."));
    }

    /* 정상 처리 */
    elog(DEBUG1, "processing value: %d", value);
}
```

### 10.2 파일 접근 오류 처리

```c
#include "postgres.h"
#include <fcntl.h>

void
open_data_file(const char *filename)
{
    int fd;

    fd = open(filename, O_RDONLY);
    if (fd < 0)
    {
        ereport(ERROR,
                errcode_for_file_access(),
                errmsg("could not open file \"%s\": %m", filename));
    }

    /* 파일 처리... */
    close(fd);
}
```

### 10.3 시그널 핸들러 예시

```c
#include "postgres.h"
#include "miscadmin.h"
#include "storage/latch.h"

static volatile sig_atomic_t got_SIGHUP = false;
static volatile sig_atomic_t got_SIGTERM = false;

static void
handle_sighup(SIGNAL_ARGS)
{
    int save_errno = errno;
    got_SIGHUP = true;
    SetLatch(MyLatch);
    errno = save_errno;
}

static void
handle_sigterm(SIGNAL_ARGS)
{
    int save_errno = errno;
    got_SIGTERM = true;
    SetLatch(MyLatch);
    errno = save_errno;
}

void
setup_signal_handlers(void)
{
    pqsignal(SIGHUP, handle_sighup);
    pqsignal(SIGTERM, handle_sigterm);
}
```

### 10.4 메모리 컨텍스트 사용

```c
#include "postgres.h"
#include "utils/memutils.h"

void
example_memory_usage(void)
{
    MemoryContext oldcontext;
    MemoryContext mycontext;
    char *data;

    /* 새 메모리 컨텍스트 생성 */
    mycontext = AllocSetContextCreate(CurrentMemoryContext,
                                      "MyContext",
                                      ALLOCSET_DEFAULT_SIZES);

    /* 새 컨텍스트로 전환 */
    oldcontext = MemoryContextSwitchTo(mycontext);

    /* 메모리 할당 */
    data = palloc(1024);
    /* 데이터 처리... */

    /* 원래 컨텍스트로 복원 */
    MemoryContextSwitchTo(oldcontext);

    /* 컨텍스트와 모든 할당된 메모리 해제 */
    MemoryContextDelete(mycontext);
}
```

---

## 11. 참고 자료

- [PostgreSQL 공식 문서 - Internals](https://www.postgresql.org/docs/current/internals.html)
- [PostgreSQL Developer FAQ](https://wiki.postgresql.org/wiki/Developer_FAQ)
- [PostgreSQL Source Code Doxygen](https://doxygen.postgresql.org/)
- [PostgreSQL Meson 빌드 문서](https://www.postgresql.org/docs/current/install-meson.html)
- [PostgreSQL 오류 보고 문서](https://www.postgresql.org/docs/current/error-message-reporting.html)

---

## 요약

PostgreSQL 소스 코드는 체계적으로 구성된 디렉토리 구조를 가지며, 명확한 코딩 규칙을 따릅니다. 주요 포인트:

1. 디렉토리 구조: `src` 디렉토리 아래에 backend, bin, include 등의 핵심 디렉토리가 있음
2. 코딩 규칙: BSD 스타일 포맷팅, C99 표준 준수, 특정 주석 스타일 사용
3. 오류 보고: `ereport`/`elog` 함수를 통한 일관된 오류 메시지 생성
4. 빌드 시스템: Meson (현대적) 및 Autoconf (레거시) 지원
5. 개발 도구: pgindent, 에디터 설정, 다양한 분석 도구 제공

이러한 구조와 규칙을 이해하면 PostgreSQL 소스 코드를 효과적으로 탐색하고 기여할 수 있습니다.

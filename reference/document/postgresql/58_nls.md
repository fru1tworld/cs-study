# Chapter 57. 네이티브 언어 지원 (Native Language Support)

## 목차
1. [개요](#개요)
2. [번역자 가이드](#번역자-가이드-for-the-translator)
   - [요구사항](#요구사항-requirements)
   - [개념](#개념-concepts)
   - [메시지 카탈로그 생성 및 유지](#메시지-카탈로그-생성-및-유지-creating-and-maintaining-message-catalogs)
   - [PO 파일 편집](#po-파일-편집-editing-the-po-files)
3. [프로그래머 가이드](#프로그래머-가이드-for-the-programmer)
   - [메커니즘](#메커니즘-mechanics)
   - [메시지 작성 가이드라인](#메시지-작성-가이드라인-message-writing-guidelines)

---

## 개요

PostgreSQL은 네이티브 언어 지원(Native Language Support, NLS) 을 통해 사용자에게 친숙한 언어로 메시지를 제공합니다. 이를 통해 오류 메시지, 경고, 정보 메시지 등이 사용자의 모국어로 표시될 수 있습니다.

PostgreSQL의 NLS는 GNU gettext 라이브러리를 기반으로 구현되어 있으며, 이 표준화된 접근 방식을 통해 다양한 언어로의 번역이 가능합니다.

### NLS의 주요 특징

- gettext 기반: 널리 사용되는 GNU gettext 국제화 프레임워크 사용
- 메시지 카탈로그: PO(Portable Object) 및 MO(Machine Object) 파일 형식 지원
- 복수형 처리: 언어별 복수형 규칙 지원
- 동적 번역: 런타임에 적절한 번역 선택

---

## 번역자 가이드 (For the Translator)

이 섹션은 PostgreSQL 메시지를 다른 언어로 번역하고자 하는 번역자를 위한 가이드입니다.

### 요구사항 (Requirements)

번역 작업을 수행하기 위해 필요한 도구들입니다.

#### 기본 요구사항

| 도구 | 설명 | 필수 여부 |
|------|------|-----------|
| 텍스트 편집기 | PO 파일 편집용 | 필수 |
| `msgfmt` | MO 파일 생성 | 필수 |
| `libintl` | gettext 라이브러리 | 필수 |

#### 새로운 번역 시작 또는 병합 시 추가 요구사항

| 도구 | 설명 | 버전 요구사항 |
|------|------|---------------|
| `xgettext` | 소스에서 메시지 추출 | GNU Gettext 0.10.36+ |
| `msgmerge` | PO 파일 병합 | GNU Gettext 0.10.36+ |

#### PostgreSQL 소스 빌드 요구사항

```bash
# NLS 지원을 활성화하여 PostgreSQL 빌드
./configure --enable-nls
make
```

### 개념 (Concepts)

#### 메시지 카탈로그 파일 형식

NLS는 두 가지 파일 형식을 사용합니다:

| 형식 | 확장자 | 용도 | 특징 |
|------|--------|------|------|
| PO (Portable Object) | `.po` | 원본-번역 쌍 저장 | 텍스트 형식, 번역자가 직접 편집 |
| MO (Machine Object) | `.mo` | 런타임에 사용 | 바이너리 형식, 자동 생성 |

#### 파일 명명 규칙

```
progname.pot     # 템플릿 파일 (번역되지 않은 원본)
fr.po            # 프랑스어 PO 파일
de.po            # 독일어 PO 파일
ja.po            # 일본어 PO 파일
pt_BR.po         # 브라질 포르투갈어 PO 파일
ko.po            # 한국어 PO 파일
```

#### 언어 코드 규칙

- ISO 639-1 두 글자 코드 (소문자): `fr`, `de`, `ja`, `ko`
- 지역 변형 포함 시: `pt_BR` (브라질 포르투갈어), `zh_CN` (중국어 간체)

### 메시지 카탈로그 생성 및 유지 (Creating and Maintaining Message Catalogs)

#### 새로운 번역 시작하기

1단계: 프로그램 디렉토리 확인

번역 가능한 프로그램인지 확인합니다. `nls.mk` 파일이 있으면 번역 준비가 된 프로그램입니다.

```bash
# 예: psql 디렉토리 확인
ls src/bin/psql/nls.mk
```

2단계: 기본 카탈로그(POT) 생성

```bash
cd src/bin/psql
make init-po
```

이 명령은 `progname.pot` 템플릿 파일을 생성합니다.

3단계: 언어별 PO 파일 생성

```bash
# 한국어 번역 파일 생성
cp psql.pot ko.po
```

4단계: 언어 등록

`po/LINGUAS` 파일에 언어 코드를 추가합니다:

```
# po/LINGUAS
de fr ja ko pt_BR zh_CN
```

#### 기존 번역 업데이트

프로그램 소스가 변경되면 번역을 업데이트해야 합니다:

```bash
# 메시지 병합
make update-po
```

이 명령은:
1. 새로운 POT 파일을 생성합니다
2. 기존 PO 파일과 병합합니다
3. 불확실한 메시지를 "fuzzy"로 표시합니다
4. 결과를 `.po.new` 확장자로 저장합니다

### PO 파일 편집 (Editing the PO Files)

#### PO 파일 기본 구조

```po
# 번역자 주석
#. 자동 추출된 주석 (소스 코드에서)
#: filename.c:1023
#, c-format
msgid "Cannot open file \"%s\": %m"
msgstr "파일 \"%s\"을(를) 열 수 없습니다: %m"

msgid "Connection refused"
msgstr "연결이 거부되었습니다"

# 복수형 메시지
msgid "copied %d file"
msgid_plural "copied %d files"
msgstr[0] "%d개 파일을 복사했습니다"
```

#### 주석 유형

| 기호 | 의미 | 설명 |
|------|------|------|
| `#` | 번역자 주석 | 번역자가 작성하는 메모 |
| `#.` | 자동 주석 | 소스 코드에서 자동 추출된 주석 |
| `#:` | 위치 정보 | 메시지가 사용되는 소스 파일과 라인 번호 |
| `#,` | 플래그 | 메시지 특성 (예: c-format, fuzzy) |

#### 플래그 설명

- `fuzzy`: 소스 변경으로 인해 번역이 오래되었을 수 있음. 번역자가 확인 후 제거해야 함
  - 중요: Fuzzy 메시지는 최종 사용자에게 제공되지 않음!
- `c-format`: printf 형식 문자열임을 나타냄. 번역도 동일한 플레이스홀더를 포함해야 함

#### 번역 시 주의사항

1. 형식 지정자 보존

원본의 형식 지정자(`%s`, `%d`, `%m` 등)를 반드시 보존해야 합니다:

```po
# 올바른 예
msgid "File %s has %d lines"
msgstr "파일 %s에 %d개의 줄이 있습니다"
```

2. 순서 변경이 필요한 경우

언어에 따라 인자 순서를 변경해야 할 때는 위치 지정자를 사용합니다:

```po
msgid "File %s has %d characters."
msgstr "%2$d개의 문자가 파일 %1$s에 있습니다."
```

- `%1$s`: 첫 번째 인자 (문자열)
- `%2$d`: 두 번째 인자 (정수)

3. 줄바꿈 및 특수 문자 유지

```po
# 원본이 \n으로 끝나면 번역도 동일하게
msgid "Operation completed.\n"
msgstr "작업이 완료되었습니다.\n"
```

4. 스타일 일관성

- 완전한 문장이 아닌 메시지는 대문자로 시작하지 않음
- 마침표로 끝내지 않음 (문장이 아닌 경우)
- 예: `cannot open file %s` → `파일 %s을(를) 열 수 없음`

#### 편집 도구 추천

- Emacs PO 모드: PO 파일 편집에 최적화
- Poedit: GUI 기반 PO 편집기
- Lokalize: KDE 번역 도구
- 일반 텍스트 편집기: 기본적인 편집 가능

---

## 프로그래머 가이드 (For the Programmer)

이 섹션은 PostgreSQL 코드에 NLS 지원을 추가하려는 프로그래머를 위한 가이드입니다.

### 메커니즘 (Mechanics)

#### 1단계: 프로그램 시작 시퀀스에 초기화 코드 추가

```c
#ifdef ENABLE_NLS
#include <locale.h>
#endif

...

#ifdef ENABLE_NLS
setlocale(LC_ALL, "");
bindtextdomain("progname", LOCALEDIR);
textdomain("progname");
#endif
```

함수 설명:

| 함수 | 설명 |
|------|------|
| `setlocale(LC_ALL, "")` | 환경 변수에서 로케일 설정 로드 |
| `bindtextdomain()` | 메시지 카탈로그 디렉토리 지정 |
| `textdomain()` | 현재 텍스트 도메인(프로그램 이름) 설정 |

#### 2단계: 번역 대상 메시지에 gettext() 호출 추가

변경 전:

```c
fprintf(stderr, "panic level %d\n", lvl);
```

변경 후:

```c
fprintf(stderr, gettext("panic level %d\n"), lvl);
```

단축 매크로 사용 (권장):

```c
#define _(x) gettext(x)

// 사용 예
fprintf(stderr, _("panic level %d\n"), lvl);
```

#### 3단계: nls.mk 파일 작성

프로그램 소스 디렉토리에 `nls.mk` 파일을 생성합니다:

```makefile
CATALOG_NAME = psql
GETTEXT_FILES = command.c common.c copy.c help.c input.c \
                large_obj.c mainloop.c print.c startup.c \
                describe.c sql_help.h tab-complete.c \
                variables.c
GETTEXT_TRIGGERS = psql_error simple_prompt write_msg:2
```

변수 설명:

| 변수 | 설명 |
|------|------|
| `CATALOG_NAME` | `textdomain()` 호출에서 사용된 프로그램 이름 |
| `GETTEXT_FILES` | 번역 가능한 문자열을 포함하는 소스 파일 목록 |
| `GETTEXT_TRIGGERS` | 번역 문자열을 포함하는 함수/매크로 이름 |

GETTEXT_TRIGGERS 형식:

- `gettext` - 첫 번째 인자가 번역 대상
- `_` - 첫 번째 인자가 번역 대상 (매크로)
- `func:2` - 두 번째 인자가 번역 대상
- `ereport:2` - 두 번째 인자가 번역 대상

#### 4단계: po/LINGUAS 파일 생성

제공할 번역 목록을 작성합니다. 초기에는 비어 있을 수 있습니다:

```
# po/LINGUAS
de fr ja ko pt_BR zh_CN
```

### 메시지 작성 가이드라인 (Message-Writing Guidelines)

국제화를 위해 메시지를 작성할 때 따라야 할 중요한 가이드라인입니다.

#### 1. 런타임에 문장을 구성하지 말 것

잘못된 예:

```c
printf("Files were %s.\n", flag ? "copied" : "removed");
```

문제점: 다른 언어에서는 어순이 다를 수 있어 번역이 불가능합니다.

올바른 예:

```c
if (flag)
    printf(_("Files were copied.\n"));
else
    printf(_("Files were removed.\n"));
```

#### 2. 복수형 처리

잘못된 예 1:

```c
printf("copied %d file%s", n, n != 1 ? "s" : "");
```

잘못된 예 2:

```c
if (n == 1)
    printf("copied 1 file");
else
    printf("copied %d files", n);
```

문제점: 많은 언어에서 복수형 규칙이 영어와 다릅니다 (예: 러시아어는 3가지 복수형).

권장 방법 1 - 복수형 회피:

```c
printf(_("number of copied files: %d"), n);
```

권장 방법 2 - errmsg_plural() 사용:

```c
ereport(INFO,
    errmsg_plural("copied %d file",
                  "copied %d files",
                  n,
                  n));
```

errmsg_plural() 인자 설명:

| 인자 | 설명 |
|------|------|
| 첫 번째 | 단수형 형식 문자열 (영어) |
| 두 번째 | 복수형 형식 문자열 (영어) |
| 세 번째 | 복수형 결정 제어값 (n) |
| 네 번째 이후 | 형식 문자열에 따른 인자들 |

ngettext() 직접 사용:

`ereport`/`errmsg` 외부에서는 `ngettext()`를 직접 사용합니다:

```c
printf(ngettext("Processed %d row", "Processed %d rows", count), count);
```

#### 3. 번역자를 위한 주석 추가

복잡하거나 모호한 메시지에는 번역자를 위한 주석을 추가합니다:

```c
/* translator: %s is the name of a data type */
_("cannot cast to %s")

/* translator: This message appears when connection is lost */
_("lost connection")
```

이 주석은 메시지 카탈로그 파일에 복사되어 번역자가 컨텍스트를 이해하는 데 도움이 됩니다.

#### 4. 플레이스홀더 설명

여러 플레이스홀더가 있는 경우 각각이 무엇을 나타내는지 설명합니다:

```c
/*
 * translator: first %s is the column name,
 * second %s is the table name
 */
errmsg("column %s of table %s does not exist",
       colname, tablename)
```

#### 5. 일관된 용어 사용

동일한 개념에 대해 일관된 용어를 사용합니다:

```c
// 좋은 예 - 일관된 용어
_("cannot open file")
_("cannot read file")
_("cannot write file")

// 나쁜 예 - 불일관된 용어
_("cannot open file")
_("unable to read file")
_("failed to write file")
```

---

## 예제: 완전한 NLS 지원 프로그램

다음은 NLS를 지원하는 간단한 PostgreSQL 확장 또는 유틸리티 프로그램의 예제입니다.

### 소스 코드 (myprogram.c)

```c
#include "postgres.h"

#ifdef ENABLE_NLS
#include <locale.h>
#include <libintl.h>
#define _(x) gettext(x)
#else
#define _(x) (x)
#endif

#include "utils/elog.h"

void
initialize_nls(void)
{
#ifdef ENABLE_NLS
    setlocale(LC_ALL, "");
    bindtextdomain("myprogram", LOCALEDIR);
    textdomain("myprogram");
#endif
}

void
process_files(int count)
{
    if (count < 0)
    {
        ereport(ERROR,
            errmsg(_("invalid file count: %d"), count));
        return;
    }

    /* 복수형 처리 예제 */
    ereport(INFO,
        errmsg_plural("processed %d file successfully",
                      "processed %d files successfully",
                      count,
                      count));
}

void
connect_database(const char *dbname, const char *host)
{
    if (dbname == NULL)
    {
        /* translator: 데이터베이스 이름이 제공되지 않았을 때 표시됨 */
        ereport(ERROR,
            errmsg(_("database name is required")));
        return;
    }

    /*
     * translator: first %s is database name,
     * second %s is host name
     */
    ereport(INFO,
        errmsg(_("connecting to database \"%s\" on host \"%s\""),
               dbname, host));
}
```

### nls.mk 파일

```makefile
CATALOG_NAME = myprogram
GETTEXT_FILES = myprogram.c utils.c commands.c
GETTEXT_TRIGGERS = ereport:2 errmsg errmsg_plural:1,2
```

### 한국어 번역 파일 (ko.po)

```po
# Korean translation for myprogram
# Copyright (C) 2024 PostgreSQL Global Development Group
# This file is distributed under the same license as PostgreSQL.
#
msgid ""
msgstr ""
"Project-Id-Version: myprogram 1.0\n"
"Report-Msgid-Bugs-To: \n"
"POT-Creation-Date: 2024-01-15 10:00+0900\n"
"PO-Revision-Date: 2024-01-15 11:00+0900\n"
"Last-Translator: 번역자 이름 <translator@example.com>\n"
"Language-Team: Korean\n"
"Language: ko\n"
"MIME-Version: 1.0\n"
"Content-Type: text/plain; charset=UTF-8\n"
"Content-Transfer-Encoding: 8bit\n"
"Plural-Forms: nplurals=1; plural=0;\n"

#: myprogram.c:25
#, c-format
msgid "invalid file count: %d"
msgstr "잘못된 파일 개수: %d"

#: myprogram.c:32
#, c-format
msgid "processed %d file successfully"
msgid_plural "processed %d files successfully"
msgstr[0] "%d개 파일을 성공적으로 처리했습니다"

#. translator: 데이터베이스 이름이 제공되지 않았을 때 표시됨
#: myprogram.c:42
msgid "database name is required"
msgstr "데이터베이스 이름이 필요합니다"

#. translator: first %s is database name, second %s is host name
#: myprogram.c:50
#, c-format
msgid "connecting to database \"%s\" on host \"%s\""
msgstr "호스트 \"%2$s\"의 데이터베이스 \"%1$s\"에 연결 중"
```

### po/LINGUAS 파일

```
de fr ja ko pt_BR zh_CN
```

---

## 번역 워크플로우 요약

```
┌─────────────────────────────────────────────────────────────┐
│                    새로운 번역 시작                           │
├─────────────────────────────────────────────────────────────┤
│  1. make init-po         → progname.pot 생성                 │
│  2. cp progname.pot ko.po → 언어별 PO 파일 생성              │
│  3. po/LINGUAS에 ko 추가  → 언어 등록                        │
│  4. ko.po 편집            → msgstr 작성                      │
│  5. make                  → MO 파일 생성 및 설치              │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    기존 번역 업데이트                          │
├─────────────────────────────────────────────────────────────┤
│  1. make update-po        → 새 POT와 기존 PO 병합            │
│  2. fuzzy 항목 확인        → 변경된 메시지 검토               │
│  3. 새 메시지 번역         → 빈 msgstr 채우기                 │
│  4. fuzzy 플래그 제거      → 검토 완료 표시                   │
│  5. make                  → MO 파일 재생성                   │
└─────────────────────────────────────────────────────────────┘
```

---

## 자주 발생하는 문제와 해결책

### 1. Fuzzy 메시지가 표시되지 않음

문제: 번역했는데도 원본 영어 메시지가 표시됨

원인: `#, fuzzy` 플래그가 남아 있음

해결: PO 파일에서 fuzzy 플래그를 제거

```po
# 변경 전
#, fuzzy, c-format
msgid "cannot open file"
msgstr "파일을 열 수 없음"

# 변경 후
#, c-format
msgid "cannot open file"
msgstr "파일을 열 수 없음"
```

### 2. 형식 지정자 불일치 오류

문제: `msgfmt` 실행 시 오류 발생

원인: 원본과 번역의 형식 지정자가 다름

해결: 형식 지정자를 정확히 일치시킴

```po
# 잘못된 예
msgid "Error: %s (code %d)"
msgstr "오류: %s"  # %d 누락!

# 올바른 예
msgid "Error: %s (code %d)"
msgstr "오류: %s (코드 %d)"
```

### 3. 인코딩 문제

문제: 한글이 깨져서 표시됨

원인: PO 파일 인코딩이 올바르지 않음

해결: UTF-8 인코딩 사용 및 헤더 확인

```po
msgid ""
msgstr ""
"Content-Type: text/plain; charset=UTF-8\n"
```

---

## 참고 자료

- [GNU gettext 매뉴얼](https://www.gnu.org/software/gettext/manual/)
- [PostgreSQL 공식 문서 - Native Language Support](https://www.postgresql.org/docs/current/nls.html)
- [ISO 639-1 언어 코드](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes)
- [PostgreSQL Error Message Style Guide](https://www.postgresql.org/docs/current/error-style-guide.html)

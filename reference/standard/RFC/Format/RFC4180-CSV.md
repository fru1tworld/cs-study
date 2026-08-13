# RFC 4180 - 쉼표로 구분된 값(CSV) 파일의 공통 형식 및 MIME 타입

```
Network Working Group                                    Y. Shafranovich
Request for Comments: 4180                SolidMatrix Technologies, Inc.
Category: Informational                                     October 2005
```

### Abstract

- 쉼표로 구분된 값(Comma-Separated Values, CSV) 파일에 사용되는 형식 문서화
- 관련 MIME 타입 "text/csv" 등록

## 이 메모의 상태

- 인터넷 커뮤니티를 위한 정보 제공 목적, 인터넷 표준을 명시하지 않음
- 배포 제한 없음

## 저작권 고지

- Copyright (C) The Internet Society (2005)

## 목차

```
1.  소개
2.  CSV 형식의 정의
3.  text/csv의 MIME 타입 등록
4.  IANA 고려 사항
5.  보안 고려 사항
6.  감사의 말
7.  참고 문헌
    7.1.  규범적 참고 문헌
    7.2.  정보성 참고 문헌
저자 주소
```

## 1. 소개

- CSV(쉼표로 구분된 값 형식) → 다양한 스프레드시트 프로그램 간 데이터 교환·변환에 오랫동안 사용
- 흔하게 사용됨에도 공식 문서화된 적 없음
- IANA MIME 등록 트리에 "text/tab-separated-values" 등록은 있으나 CSV용 MIME 타입은 등록된 적 없음
- 다양한 프로그램·운영체제가 서로 다른 MIME 타입을 사용하기 시작함
- 이 RFC의 목적
  - CSV 파일 형식 문서화
  - RFC 2048 [1]에 따라 "text/csv" MIME 타입 공식 등록

## 2. CSV 형식의 정의

- CSV 형식에 대한 다양한 사양·구현 존재(예: [4], [5], [6], [7]) → 공식 사양 부재 → 구현마다 해석 차이 큼
- 이 절은 대부분의 구현이 따르는 형식을 문서화

1. 각 레코드는 별도의 줄에 위치, 줄 바꿈(CRLF)으로 구분
   - 예:
     ```
     aaa,bbb,ccc CRLF
     zzz,yyy,xxx CRLF
     ```
2. 파일의 마지막 레코드는 끝맺는 줄 바꿈 유무 모두 허용
   - 예:
     ```
     aaa,bbb,ccc CRLF
     zzz,yyy,xxx
     ```
3. 파일 첫 줄에 선택적 헤더 줄 가능, 일반 레코드 줄과 동일한 형식
   - 파일의 필드에 대응하는 이름 포함
   - 나머지 레코드와 동일한 필드 개수 권장
   - 헤더 존재 여부는 MIME 타입의 선택적 "header" 매개변수로 표시 권장
   - 예:
     ```
     field_name,field_name,field_name CRLF
     aaa,bbb,ccc CRLF
     zzz,yyy,xxx CRLF
     ```
4. 헤더와 각 레코드 안에는 쉼표로 구분된 하나 이상의 필드 가능
   - 각 줄은 파일 전체에서 동일한 필드 개수 권장
   - 공백은 필드의 일부로 간주 → 무시하지 않는 것을 권장
   - 레코드의 마지막 필드 뒤에는 쉼표 금지
   - 예: `aaa,bbb,ccc`
5. 각 필드는 큰따옴표로 둘러쌀 수도 있고 안 할 수도 있음(단 Microsoft Excel 등 일부 프로그램은 큰따옴표 미사용)
   - 필드가 큰따옴표로 둘러싸이지 않은 경우 → 필드 안에 큰따옴표 등장 금지
   - 예:
     ```
     "aaa","bbb","ccc" CRLF
     zzz,yyy,xxx
     ```
6. 줄 바꿈(CRLF)·큰따옴표·쉼표를 포함하는 필드는 큰따옴표로 둘러싸는 것을 권장
   - 예:
     ```
     "aaa","b CRLF
     bb","ccc" CRLF
     zzz,yyy,xxx
     ```
7. 필드를 둘러싸기 위해 큰따옴표를 사용하는 경우 → 필드 안의 큰따옴표는 앞에 큰따옴표를 하나 더 붙여 이스케이프 필수
   - 예: `"aaa","b""bb","ccc"`

ABNF 문법 [2]:

```
file = [header CRLF] record *(CRLF record) [CRLF]

header = name *(COMMA name)

record = field *(COMMA field)

name = field

field = (escaped / non-escaped)

escaped = DQUOTE *(TEXTDATA / COMMA / CR / LF / 2DQUOTE) DQUOTE

non-escaped = *TEXTDATA

COMMA = %x2C

CR = %x0D ;as per section 6.1 of RFC 2234 [2]

DQUOTE =  %x22 ;as per section 6.1 of RFC 2234 [2]

LF = %x0A ;as per section 6.1 of RFC 2234 [2]

CRLF = CR LF ;as per section 6.1 of RFC 2234 [2]

TEXTDATA =  %x20-21 / %x23-2B / %x2D-7E
```

## 3. text/csv의 MIME 타입 등록

이 절은 (RFC 2048 [1]에 따른) 미디어 타입 등록 신청서 제공.

- To: ietf-types@iana.org
- Subject: Registration of MIME media type text/csv
- MIME 미디어 타입 이름: text
- MIME 서브타입 이름: csv
- 필수 매개변수: 없음
- 선택적 매개변수: charset, header
  - charset: CSV의 일반적 용도는 US-ASCII이나 "text" 트리에 대해 IANA가 정의한 다른 문자 집합도 사용 가능
  - header: 헤더 줄의 존재 여부 표시
    - 유효 값: "present" · "absent"
    - 이 매개변수를 사용하지 않는 구현자는 헤더 존재 여부를 스스로 결정 필요
- 인코딩 고려 사항
  - RFC 2046 [3] 4.1.1절에 따라 줄 바꿈 표시에 CRLF 사용
  - 일부 구현이 다른 값을 사용할 수 있음에 유의 필요
- 보안 고려 사항
  - CSV 파일은 수동적 텍스트 데이터 포함 → 위험 없어야 함
  - 이론상 CSV 처리 프로그램의 버퍼 오버런을 노린 악의적 바이너리 데이터 포함 가능
  - 이 형식을 통해 비공개 데이터 노출 가능(모든 텍스트 데이터에 공통되는 문제)
- 상호운용성 고려 사항
  - 단일 사양 부재 → 구현 간 상당한 차이 존재
  - 구현자는 "자신이 하는 일에는 보수적으로, 다른 곳으로부터 받아들이는 것에는 관대하게"(RFC 793 [8]) 원칙 준수 필요 → 공통 정의는 2절 참고
  - 선택적 "header" 매개변수를 사용하지 않는 구현은 헤더 존재 여부를 스스로 결정 필요
- 발행된 사양
  - 다양한 프로그램·시스템에 대한 비공개 사양은 다수 존재하나 단일 "마스터" 사양 없음 → 공통 정의는 2절 참고
- 이 미디어 타입을 사용하는 애플리케이션: 스프레드시트 프로그램 · 다양한 데이터 변환 유틸리티
- 추가 정보
  - 매직 넘버: 없음
  - 파일 확장자: CSV
  - 매킨토시 파일 타입 코드: TEXT
- 추가 정보를 위해 연락할 사람 및 이메일 주소: Yakov Shafranovich <ietf@shaftek.org>
- 의도된 용도: COMMON
- 저자/변경 관리자: IESG

## 4. IANA 고려 사항

- IANA는 3절에 제공된 신청서를 사용해 MIME 타입 "text/csv" 등록 완료

## 5. 보안 고려 사항

- 3절의 논의 참고

## 6. 감사의 말

- 유익한 제안을 해 준 다음 분들께 감사: Dave Crocker, Martin Duerst, Joel M. Halpern, Clyde Ingram, Graham Klyne, Bruce Lilly, Chris Lilley, IESG 구성원
- ABNF 문법을 도와준 Dave에게 특별히 감사
- RFC와 인터넷 드래프트 준비에 사용된 많은 도구를 제공해 준 Henrik Lefkowetz, Marshall Rose, xml.resource.org 관계자들에게도 감사
- L.T.S.에게 특별히 감사

## 7. 참고 문헌

### 7.1. 규범적 참고 문헌

- [1] Freed, N., Klensin, J., and J. Postel, "Multipurpose Internet Mail Extensions (MIME) Part Four: Registration Procedures", BCP 13, RFC 2048, November 1996.
- [2] Crocker, D. and P. Overell, "Augmented BNF for Syntax Specifications: ABNF", RFC 2234, November 1997.
- [3] Freed, N. and N. Borenstein, "Multipurpose Internet Mail Extensions (MIME) Part Two: Media Types", RFC 2046, November 1996.

### 7.2. 정보성 참고 문헌

- [4] Repici, J., "HOW-TO: The Comma Separated Value (CSV) File Format", 2004, <http://www.creativyst.com/Doc/Articles/CSV/CSV01.htm>.
- [5] Edoceo, Inc., "CSV Standard File Format", 2004, <http://www.edoceo.com/utilis/csv-file-format.php>.
- [6] Rodger, R. and O. Shanaghy, "Documentation for Ricebridge CSV Manager", February 2005, <http://www.ricebridge.com/products/csvman/reference.htm>.
- [7] Raymond, E., "The Art of Unix Programming, Chapter 5", September 2003, <http://www.catb.org/~esr/writings/taoup/html/ch05s02.html>.
- [8] Postel, J., "Transmission Control Protocol", STD 7, RFC 793, September 1981.

## 저자 주소

```
Yakov Shafranovich
SolidMatrix Technologies, Inc.

Email: ietf@shaftek.org
URI:   http://www.shaftek.org
```

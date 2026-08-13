# RFC 4122 - 범용 고유 식별자(UUID) URN 네임스페이스

```
Network Working Group                                           P. Leach
Request for Comments: 4122                                     Microsoft
Category: Standards Track                                    M. Mealling
                                                Refactored Networks, LLC
                                                                 R. Salz
                                              DataPower Technology, Inc.
                                                               July 2005
```

> 참고: RFC 4122는 2024년 5월 RFC 9562로 대체(obsolete)됨. RFC 9562는 버전 1-5를 그대로 유지하면서 버전 6(시간 기반 필드를 재정렬해 정렬 순서를 개선한 UUID)·버전 7(유닉스 타임스탬프 기반 UUID로 시간순 정렬이 자연스러워 데이터베이스 기본 키로 널리 쓰임)·버전 8(사용자 정의 목적용)을 신규 정의. 아래 본문은 RFC 4122 원문 번역이며, 최신 규격 필요 시 RFC 9562 참고.

## 이 메모의 상태

- 인터넷 커뮤니티를 위한 인터넷 표준 추적 프로토콜 규정, 개선을 위한 논의와 제안 요청
- 본 프로토콜의 표준화 상태 및 현황: "Internet Official Protocol Standards" (STD 1) 최신판 참조
- 본 메모의 배포는 무제한

## 저작권 고지

- Copyright (C) The Internet Society (2005)

## Abstract

- UUID(Universally Unique IDentifier)를 위한 URN(Uniform Resource Name) 네임스페이스 정의
- UUID = GUID(Globally Unique IDentifier)
- UUID: 128비트 길이 → 공간과 시간에 걸쳐 고유성 보장 가능
- 유래: Apollo Network Computing System → Open Software Foundation(OSF)의 Distributed Computing Environment(DCE) → Microsoft Windows 플랫폼
- 본 명세는 OSF의 허가 하에 DCE 명세에서 파생

## 목차

```
1.  소개
2.  동기
3.  네임스페이스 등록 템플릿
4.  명세
    4.1.  형식
          4.1.1.  Variant
          4.1.2.  레이아웃과 바이트 순서
          4.1.3.  버전
          4.1.4.  타임스탬프
          4.1.5.  클럭 시퀀스
          4.1.6.  노드
          4.1.7.  Nil UUID
    4.2.  시간 기반 UUID 생성 알고리즘
          4.2.1.  기본 알고리즘
          4.2.2.  생성 세부사항
    4.3.  이름 기반 UUID 생성 알고리즘
    4.4.  진정한 난수 또는 의사 난수로부터 UUID를 생성하는 알고리즘
    4.5.  호스트를 식별하지 않는 노드 ID
5.  커뮤니티 고려사항
6.  보안 고려사항
7.  감사의 글
8.  참고 문헌
    8.1.  규범적 참고 문헌
    8.2.  정보성 참고 문헌
부록 A - 구현 예시
부록 B - 출력 예시
부록 C - 미리 정의된 몇 가지 네임스페이스 ID
```

## 1. 소개

- UUID(Universally Unique IDentifier)를 위한 URN 네임스페이스 정의, UUID = GUID
- UUID: 128비트 길이, 중앙 등록 절차 불요
- 본 명세: 간결한 구현 가이드 목적 → 원래 UUID를 정의한 DCE 표준 [2]를 대체·무시하지 않음
- 요구사항 핵심 용어(MUST/MUST NOT/REQUIRED/SHALL/SHALL NOT/SHOULD/SHOULD NOT/RECOMMENDED/MAY/OPTIONAL): [RFC 2119]에 따라 해석
- UUID = 128비트 숫자에 대한 특정 표현과 시맨틱의 조합 → 본 문서에서 정의한 variant의 바이트 레벨 정확한 표현 외에는 프로그래밍 언어에서 UUID 사용 방식에 대한 요구사항 부과 없음
- UUID는 ITU-T Rec. X.667 | ISO/IEC 9834-8:2004 [3]에 의해서도 규정

## 2. 동기

- 분산 컴퓨팅 환경 내외부에서 고유 식별자 필요 사례 다수
- UUID 사용 이유
  - 중앙 관리 기관 불요 (한 형식은 IEEE 802 노드 식별자 사용, 다른 형식은 미사용)
  - 고정 크기(128비트) → 정렬·순서 지정·해싱·배열 저장·데이터베이스·일반 프로그래밍에서 간편 사용에 적합한 크기
- UUID는 고유하고 영속적 → 훌륭한 URN(Uniform Resource Name)
- 등록 과정 연락 없이 새 UUID 생성 가능 → 여러 URN 중 가장 낮은 발행 비용

## 3. 네임스페이스 등록 템플릿

- Namespace ID: UUID
- Registration Information: 등록일 2003-10-01
- Declared registrant of the namespace: JTC 1/SC6 (ASN.1 Rapporteur Group)
- Declaration of syntactic structure:
  - UUID: 공간과 시간에 걸쳐, 모든 UUID의 공간에 대해 고유한 식별자
  - 고정 크기 + 시간 필드 포함 → 값 순환 가능(사용 알고리즘에 따라 서기 3400년경)
  - 용도: 매우 짧은 수명 객체 태깅 ~ 네트워크를 통한 매우 영속적 객체의 신뢰성 있는 식별까지 다양
  - UUID의 내부 표현: 섹션 4에 기술된 메모리 내 특정 비트 시퀀스 → URN으로 정확히 표현하려면 비트 시퀀스를 문자열 표현으로 변환 필요
  - 각 필드: 정수로 취급, 가장 유효한 숫자를 먼저 하여 0으로 채워진 16진수 문자열로 출력. 16진수 값 "a"~"f"는 소문자로 출력, 입력 시 대소문자 구분 없음
  - UUID 문자열 표현의 형식적 정의는 다음 ABNF [7]로 제공

```abnf
UUID                   = time-low "-" time-mid "-"
                         time-high-and-version "-"
                         clock-seq-and-reserved
                         clock-seq-low "-" node
time-low               = 4hexOctet
time-mid               = 2hexOctet
time-high-and-version  = 2hexOctet
clock-seq-and-reserved = hexOctet
clock-seq-low          = hexOctet
node                   = 6hexOctet
hexOctet               = hexDigit hexDigit
hexDigit =
      "0" / "1" / "2" / "3" / "4" / "5" / "6" / "7" / "8" / "9" /
      "a" / "b" / "c" / "d" / "e" / "f" /
      "A" / "B" / "C" / "D" / "E" / "F"
```

  - 예시(UUID를 URN으로 표현): `urn:uuid:f81d4fae-7dec-11d0-a765-00a0c91e6bf6`

- Relevant ancillary documentation: [1] [2]

- Identifier uniqueness considerations:
  - 세 가지 알고리즘으로 UUID 생성
    - 시간 + 고유 IEEE 802 MAC 주소 활용
    - 의사 난수 생성기 사용
    - 애플리케이션 제공 텍스트 문자열 + 암호화 해싱 사용
  - 각 메커니즘 특성은 다르나 세 가지 모두 고유성 보장

- Identifier persistence considerations:
  - UUID는 전역 해석에 의존하지 않음 → 공간·시간에 걸친 고유성에 의해, UUID를 만들어 배포한 조직이 소멸한 후에도 지속 가능

- Process of identifier assignment:
  - UUID 생성에 등록 기관 연락 불요
  - 한 알고리즘은 IEEE 802 MAC 주소(대개 이미 사용 가능) 요구
  - 하나는 의사 난수 시드 사용
  - 세 번째는 애플리케이션 제공 텍스트 문자열 + 암호화 해싱 사용
  - IEEE 802 주소: 해당 IEEE 등록 기관에 문의하여 획득 가능

- Process for identifier resolution:
  - UUID는 전역적으로 해석 가능하지 않음 → 해당 없음

- Rules for Lexical Equivalence:
  - 두 UUID의 동일성 판단: 각 필드를 부호 없는 정수로 비교, 모든 해당 필드가 같을 때만 동일
  - 문자열 표현에서 대소문자 무시 비교 = 동등성 판단을 위한 비교와 동등
  - 사전식 순서 설정: 각 필드를 부호 없는 정수로 비교, 유효한 필드를 먼저 비교

- Conformance with URN Syntax:
  - UUID 문자열 표현은 URN 구문과 완전히 호환
  - URN → UUID 변환: 선행 `urn:uuid:` 제거 필요
  - UUID → URN 변환(역방향): `urn:uuid:` 추가 필요

- Validation mechanism:
  - UUID의 타임스탬프 부분이 미래인지(→ 아직 할당 불가) 판단하는 것 외에, UUID '유효성' 판단 메커니즘 없음

- Scope:
  - UUID는 범위가 전역적

## 4. 명세

### 4.1. 형식

- UUID 형식: 16옥텟
- 8옥텟 variant 필드 일부 비트가 더 세밀한 구조를 결정

### 4.1.1. Variant

- variant 필드: UUID의 레이아웃 결정 → 다른 모든 비트의 해석 의미가 variant 필드 비트 설정에 따라 달라짐
- 정확히는 variant 필드 자체의 크기도 variant에 의해 결정
- variant 필드: UUID의 옥텟 8에서 최상위 비트로 구성

- variant 필드 내용 (Msb0/Msb1/Msb2, x = "상관없음"):
  - `0 x x` → NCS 역호환용으로 예약
  - `1 0 x` → 본 문서에 규정된 variant
  - `1 1 0` → Microsoft Corporation 역호환용으로 예약
  - `1 1 1` → 향후 정의를 위해 예약

- UUID의 "variant" 참조 = 옥텟 8의 비트 참조

### 4.1.2. 레이아웃과 바이트 순서

- UUID 레이아웃: 최상위 비트가 먼저 표시되도록(왼쪽에서 오른쪽으로 읽음) 제시, 아래 비트 번호 매기기 체계는 별도 언급 없는 한 적용
- UUID 필드: 16옥텟으로 인코딩, 각 필드는 가장 유효한 바이트(Most Significant Byte)를 먼저 하여 인코딩(네트워크 바이트 순서)

- 필드 구성:
  - `time_low` — unsigned 32bit integer, 옥텟 0-3, 타임스탬프의 하위 필드
  - `time_mid` — unsigned 16bit integer, 옥텟 4-5, 타임스탬프의 중간 필드
  - `time_hi_and_version` — unsigned 16bit integer, 옥텟 6-7, 타임스탬프의 상위 필드에 4비트 "version"이 곱해진 것
  - `clock_seq_hi_and_reserved` — unsigned 8bit integer, 옥텟 8, 클럭 시퀀스의 상위 필드에 2비트 variant가 곱해진 것
  - `clock_seq_low` — unsigned 8bit integer, 옥텟 9, 클럭 시퀀스의 하위 필드
  - `node` — unsigned 48bit integer, 옥텟 10-15, 공간적으로 고유한 노드 식별자

- 해석상 UUID는 바이트 0-3의 time_low 필드가 가장 유효한 것으로 표시(다이어그램):

```
    0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                          time_low                             |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |       time_mid                |         time_hi_and_version   |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |clk_seq_hi_res |  clk_seq_low  |         node (0-1)            |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                         node (2-5)                            |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

### 4.1.3. 버전

- 버전 번호: time_hi_and_version 필드의 최상위 4비트에 위치

- 정의된 버전 (Msb0/Msb1/Msb2/Msb3 → 버전):
  - `0 0 0 1` → 1: 본 문서에 규정된 시간 기반 버전
  - `0 0 1 0` → 2: 임베디드 POSIX UID를 가진 DCE Security 버전
  - `0 0 1 1` → 3: MD5 해싱을 사용하는, 본 문서에 규정된 이름 기반 버전
  - `0 1 0 0` → 4: 본 문서에 규정된 랜덤 또는 의사 랜덤 생성 버전
  - `0 1 0 1` → 5: SHA-1 해싱을 사용하는, 본 문서에 규정된 이름 기반 버전

- 버전: UUID가 어떻게 생성되었는지를 더 정확히 결정하는 데 사용
- 버전의 문자열 표현: 위 표의 버전 번호에 해당하는 16진수 + variant 비트를 갖는 옥텟의 16진수 값

### 4.1.4. 타임스탬프

- 타임스탬프: 60비트 값
- 버전 1 UUID: UUID가 생성된 시스템의 시간 값에 해당, 그레고리력 개혁 채택일(1582년 10월 15일 00:00:00.00) 이후의 100나노초 간격 수로 표현
- 버전 3/5 UUID: 이름에서 구성된 60비트 값
- 버전 4 UUID: 랜덤/의사 랜덤하게 생성된 60비트 값 → 알고리즘 설명은 섹션 4.4 참조

### 4.1.5. 클럭 시퀀스

- 버전 1 UUID: 클럭 시퀀스는 UUID가 시간 순서로 생성된 것처럼 보이지만 실제로는 시스템 클럭이 뒤로 설정된 경우 감지 용도

- UUID 클럭 시퀀스 값 규칙:
  - 클럭 시퀀스는 원래(시스템 수명 중 한 번) 랜덤 숫자로 초기화되어야 함(MUST)
  - 시스템에서 마지막 UUID 생성 시 클럭 값이 알려져 있고 현재 클럭 값보다 나중이면, 클럭 시퀀스가 변경되어야 함(MUST)
  - 시스템에서 마지막 UUID 생성 시 클럭 값이 알려져 있지 않으면, 클럭 시퀀스를 랜덤 또는 고품질 의사 랜덤 값으로 설정해야 함(SHOULD)

- 버전 3/5 UUID: 클럭 시퀀스는 이름에서 구성된 14비트 값
- 버전 4 UUID: 클럭 시퀀스는 랜덤/의사 랜덤하게 생성된 14비트 값

### 4.1.6. 노드

- 버전 1 UUID: 노드 필드 = UUID를 생성하는 시스템의 IEEE 802 MAC 주소로 구성
  - 시스템에 여러 IEEE 802 주소가 있는 경우 → 사용 가능한 어떤 것이든 사용 가능
  - 시스템에 IEEE 802 주소가 없는 경우 → 가능한 알고리즘은 섹션 4.5 참조
- 버전 3/5 UUID: 노드 필드 = 이름에서 구성된 48비트 값
- 버전 4 UUID: 노드 필드 = 랜덤/의사 랜덤하게 생성된 48비트 값

### 4.1.7. Nil UUID

- nil UUID: 128비트 모두 0으로 설정되도록 규정된 UUID의 특수 형태

## 4.2. 시간 기반 UUID 생성 알고리즘

- 핵심: 시간 기반 UUID 생성에 사용되는 알고리즘
- UUID 생성기가 보장해야 하는 것(MUST):
  - UUID를 이전에 저장된 어떤 것의 복제본으로 생성하지 않음
  - UUID를 다른 UUID 생성기가 과거에 생성했거나 미래에 생성할 어떤 것의 복제본으로 생성하지 않음
  - 가능하면 UUID를 초당 1000만 개 이상 생성 가능

### 4.2.1. 기본 알고리즘

- 위 조건 충족을 위한 가장 단순하고 합리적인 접근법: UUID 하나 생성 시마다 다음 단계 수행
  - 시스템 전체의 전역 잠금 획득
  - 시스템 전체의 공유 안정 저장소(예: 파일)에서 UUID 생성기 상태 읽기: 마지막 UUID 생성에 사용된 타임스탬프·클럭 시퀀스·노드 ID 값
  - 현재 시간을 1582년 10월 15일 00:00:00.00 이후의 100나노초 간격의 60비트 수로 가져오기
  - 현재 노드 ID 가져오기
  - 상태를 사용할 수 없는 경우(예: 존재하지 않거나 손상됨) 또는 저장된 노드 ID가 현재 노드 ID와 다른 경우 → 랜덤 클럭 시퀀스 값 생성
  - 상태를 사용할 수 있으나 저장된 타임스탬프가 현재 타임스탬프보다 나중인 경우 → 클럭 시퀀스 값 증가
  - 상태(현재 타임스탬프, 클럭 시퀀스, 노드 ID)를 안정 저장소에 다시 저장
  - 전역 잠금 해제
  - 현재 타임스탬프·클럭 시퀀스·노드 ID 값으로부터 섹션 4.2.2의 단계에 따라 UUID 형식화

- 위 단순 알고리즘의 한계: 안정 저장소 읽기/쓰기 I/O가 UUID 하나당 필요 → 성능에 민감한 시스템에서 허용 어려움
- 시스템 클럭 해상도보다 빠르게 UUID를 발행해야 하는 상황 미처리 → 부록 A의 예시 구현이 가능한 해결책 제시

- 아래 알고리즘 변형은 위 측면에서 더 효율적:
  - 더 효율적인 I/O를 위한 절약 조치 채택
  - 더 나은 세분성을 위해 프로세서/잠금을 타임스탬프 틱에 맞춤 → 시스템 전체 잠금 불필요

- 이러한 변형은 공유 상태를 기본 알고리즘과 다른 방식으로 사용 가능 → 세부사항은 완전한 UUID 생성기 구현에 필요하나 본 문서 범위 밖
- 다중 프로세서 다중 처리 환경: 프로세스 간 상태 공유가 어려울 수 있음 → 아래 변형들은 이를 해결하는 알고리즘에 의존

- 전역 잠금에 대한 대안:
  - 안정 저장소 I/O 비용 절약: 각 UUID 생성 시 대신, 이전 타임스탬프 값을 휘발성 저장소(예: 시스템 변수)에 저장
  - 효과: UUID 생성기 재실행 시 타임스탬프·클럭 시퀀스의 이전 값이 사용 불가 → 클럭 시퀀스를 랜덤 값으로 설정해야 함
  - 각 UUID 간 상태 저장 간격을 넓혀 I/O를 더 줄이는 세밀한 접근 가능(부록 A의 구현 예시 참조)

- 클럭 시퀀스 유지를 원하지 않는 시스템의 경우:
  - 클럭 시퀀스는 UUID 생성기 초기화 시마다 랜덤으로 설정 가능
  - 단, 클럭 시퀀스 값이 14비트뿐 → 초기화 시마다 중복 UUID 생성 위험 존재
  - 위험 최소화 방법 세 가지:
    - a) 클럭 시퀀스 초기화 간격을 합리적으로 늘림
    - b) 생성기 초기화 후 시스템 클럭의 다음 틱까지 대기
    - c) 좋은 랜덤 소스로 클럭 시퀀스 초기화(난수 생성 가이드: [4] 참조)

### 4.2.2. 생성 세부사항

- 버전 1 UUID 생성 절차:
  - 섹션 4.2.1에 따라 UTC 기반 타임스탬프와 클럭 시퀀스 결정
  - 타임스탬프의 비트 번호 매기기: 최하위 비트부터 0으로 시작하여 순차 번호(가장 유효한 비트에 0을 매기는 방식과 반대)
  - 타임스탬프의 비트 0~31 → time_low 필드에 설정
  - 타임스탬프의 비트 32~47 → time_mid 필드에 설정
  - 타임스탬프의 비트 48~59 → time_hi_and_version의 12 최하위 비트에 설정
  - time_hi_and_version 필드의 4 최상위 비트 → 섹션 4.1.3 표의 4비트 버전 번호로 설정
  - 클럭 시퀀스의 비트 0~7 → clock_seq_low 필드에 설정
  - 클럭 시퀀스의 비트 8~13 → clock_seq_hi_and_reserved 필드의 6 최하위 비트에 설정
  - clock_seq_hi_and_reserved의 2 최상위 비트 → 각각 0과 1로 설정
  - 노드 필드 → 48비트 IEEE 주소로 설정(버전 1 UUID의 경우 섹션 4.1.6 참조)

## 4.3. 이름 기반 UUID 생성 알고리즘

- 버전 3/5 UUID: 특정 "네임스페이스"에서 "이름"으로부터 생성되도록 의도
- 개념: 같은 네임스페이스의 같은 이름 → 항상 같은 UUID 생성
- 같은 네임스페이스의 다른 이름 → (높은 확률로) 다른 UUID
- 두 다른 네임스페이스의 같은 이름 → (높은 확률로) 다른 UUID

- 이름 기반 UUID의 요구사항:
  - 같은 이름·같은 네임스페이스 → 같은 UUID 생성해야 함
  - 같은 네임스페이스의 두 다른 이름 → 다른 UUID 생성해야 함
  - 두 다른 네임스페이스의 같은 이름 → 다른 UUID 생성해야 함
  - 이름에서 생성된 UUID가 주어졌을 때 → 이름을 역으로 결정하는 것이 (일반적으로) 가능해서는 안 됨

- 이름에서 UUID를 생성하는 알고리즘:
  - 네임스페이스의 UUID 할당(예: 부록 C 참조)
  - 네임스페이스 내 모든 이름에 사용할 해싱 알고리즘 선택: MD5 [4] 또는 SHA-1 [8]
  - 네임스페이스 ID를 네트워크 바이트 순서로 변환
  - 네임스페이스의 이름에서 이름의 표준 옥텟 시퀀스 계산 → 이름 시스템의 표준을 사용해 정규 형태로 변환해야 함(예: DNS에서는 소문자화, X.500에서는 공백 압축 등)
  - 네임스페이스 ID와 이름을 연결하여 해시 계산
  - 해시의 옥텟 0~3 → 네트워크 바이트 순서로 time_low에 설정
  - 해시의 옥텟 4와 5 → 네트워크 바이트 순서로 time_mid에 설정
  - 해시의 옥텟 6과 7 → 네트워크 바이트 순서로 time_hi_and_version에 설정, 4 최상위 비트(비트 12~15)를 적절한 4비트 버전 번호(섹션 4.1.3 참조)로 덮어씀
  - 해시의 옥텟 8 → clock_seq_hi_and_reserved에 설정, 2 최상위 비트(비트 6과 7)를 각각 0과 1로 덮어씀
  - 해시의 옥텟 9 → clock_seq_low에 설정
  - 해시의 옥텟 10~15 → node에 설정
  - UUID를 로컬 바이트 순서로 변환

## 4.4. 진정한 난수 또는 의사 난수로부터 UUID를 생성하는 알고리즘

- 버전 4 UUID: 진정한 난수 또는 의사 난수로부터 생성되도록 의도

- 알고리즘:
  - clock_seq_hi_and_reserved의 2 최상위 비트(비트 6과 7) → 각각 0과 1로 설정
  - time_hi_and_version 필드의 4 최상위 비트(비트 12~15) → 섹션 4.1.3 표의 4비트 버전 번호로 설정
  - 나머지 모든 비트 → 랜덤(또는 의사 랜덤)하게 선택된 값으로 설정

- "Randomness Requirements for Security" [5]에 기술된 것처럼 가능한 최선의 품질을 가진 랜덤 소스 사용 권장(RECOMMENDED)

## 4.5. 호스트를 식별하지 않는 노드 ID

- 프라이버시 문제나 IEEE 802 주소 사용 불가 등의 이유로 → 호스트를 식별하지 않는 노드 ID 포함이 바람직할 수 있음
- 이는 시스템 전체 공유 안정 저장소가 필요한 위 알고리즘에 대한 대안이 아님 → 해당 알고리즘은 IEEE 802 주소 사용 여부와 무관하게 필요

- 방법 1: 47비트 암호화 품질 랜덤/의사 랜덤 숫자 획득 → 최하위 옥텟의 최상위 비트(유니캐스트/멀티캐스트 비트)를 1로 설정
  - 이 값은 이 시스템의 어떤 카드의 IEEE 802 주소와도 충돌하지 않음 → 실제 IEEE 802 주소의 해당 비트는 항상 0이기 때문
  - (참고: 일부 소프트웨어에서 멀티캐스트 주소를 이미 사용 중이므로 RFC 895 가이드라인을 엄격히 따르지는 않음)

- 대안: 다양한 시스템 정보(예: 컴퓨터 이름·운영 체제·프로세서 유형 등) 수집 → 해시 계산 → 노드 ID로 사용
  - 여러 소스 정보를 결합해 가능한 최선의 노드 ID 생성 → MD5 또는 SHA-1 사용 권장

## 5. 커뮤니티 고려사항

- UUID의 사용은 컴퓨팅에서 매우 광범위
- 많은 운영 체제(Microsoft Windows)·응용 프로그램(Mozilla 브라우저)의 핵심 식별자 인프라 구성
- 많은 경우 비표준적인 방식으로 웹에 노출
- 본 명세: 가능한 한 개방적으로, 인터넷 전체에 이익이 되는 방식으로 그러한 관행을 표준화 시도

## 6. 보안 고려사항

- UUID가 추측하기 어렵다고 가정 금지 → UUID를 보안 능력(단순 소유가 접근 권한을 부여하는 식별자)으로 사용 금지
- 예측 가능한 난수 소스는 상황을 악화
- UUID가 다른 객체에 대한 참조를 리디렉션하기 위해 약간 변경되었는지 여부를 판단하기 쉽다고 가정 금지 → 인간은 UUID의 무결성을 한눈에 확인하는 능력 없음
- 다양한 호스트에서 UUID를 생성하는 분산 응용 프로그램 → 모든 호스트의 난수 소스에 의존할 준비 필요
- 이것이 실현 불가능하면 → 네임스페이스 variant 사용 필요

## 7. 감사의 글

- 본 문서는 UUID에 대한 OSF DCE 명세에 크게 의존
- Ted Ts'o: 바이트 순서 섹션에 유용한 의견 제공, 제안된 표현 대부분 차용(해당 섹션의 오류는 저자 책임)
- Ralf S. Engelschall·John Larmouth·Paul Thorpe의 세심한 검토와 비트 조작에 감사
- Larmouth 교수: ISO/IEC와의 조율 달성에도 크게 기여

## 8. 참고 문헌

### 8.1. 규범적 참고 문헌

- [1] Zahn, L., Dineen, T., and P. Leach, "Network Computing Architecture", ISBN 0-13-611674-4, January 1990.
- [2] "DCE: Remote Procedure Call", Open Group CAE Specification C309 ISBN 1-85912-041-5, August 1994.
- [3] ISO/IEC 9834-8:2004 Information Technology, "Procedures for the operation of OSI Registration Authorities: Generation and registration of Universally Unique Identifiers (UUIDs) and their use as ASN.1 Object Identifier components" ITU-T Rec. X.667, 2004.
- [4] Rivest, R., "The MD5 Message-Digest Algorithm", RFC 1321, April 1992.
- [5] Eastlake, D., 3rd, Schiller, J., and S. Crocker, "Randomness Requirements for Security", BCP 106, RFC 4086, June 2005.

### 8.2. 정보성 참고 문헌

- [6] Moats, R., "URN Syntax", RFC 2141, May 1997.
- [7] Crocker, D. and P. Overell, "Augmented BNF for Syntax Specifications: ABNF", RFC 2234, November 1997.
- [8] National Institute of Standards and Technology, "Secure Hash Standard", FIPS PUB 180-1, April 1995, <http://www.itl.nist.gov/fipspubs/fip180-1.htm>.

## 부록 A - 구현 예시

이 구현은 다음 파일로 구성:

- `copyrt.h` - 저작권 고지
- `uuid.h` - UUID 구조체와 함수에 대한 헤더 파일
- `uuid.c` - UUID 생성 함수의 구현
- `sysdep.h` - 시스템 종속 타입과 상수에 대한 헤더 파일
- `sysdep.c` - 시스템 종속 함수의 구현
- `utest.c` - UUID 생성과 비교 연산을 시연하는 테스트 드라이버

### copyrt.h

```c
/*
 Copyright (c) 1990- 1993, 1996 Open Software Foundation, Inc.
 Copyright (c) 1989 by Hewlett-Packard Company, Palo Alto, Ca. &
 Digital Equipment Corporation, Maynard, Mass.
 Copyright (c) 1998 Microsoft.
 To anyone who acknowledges that this file is provided "AS IS"
 without any express or implied warranty: permission to use, copy,
 modify, and distribute this file for any purpose is hereby
 granted without fee, provided that the above copyright notices and
 this notice appears in all source code copies, and that none of
 the names of Open Software Foundation, Inc., Hewlett-Packard
 Company, Microsoft, or Digital Equipment Corporation be used in
 advertising or publicity pertaining to distribution of the software
 without specific, written prior permission. Neither Open Software
 Foundation, Inc., Hewlett-Packard Company, Microsoft, nor Digital
 Equipment Corporation makes any representations about the
 suitability of this software for any purpose.
*/
```

### uuid.h

```c
#include "copyrt.h"
#undef uuid_t
typedef struct {
    unsigned32  time_low;
    unsigned16  time_mid;
    unsigned16  time_hi_and_version;
    unsigned8   clock_seq_hi_and_reserved;
    unsigned8   clock_seq_low;
    byte        node[6];
} uuid_t;

/* uuid_create -- generate a UUID */
int uuid_create(uuid_t * uuid);

/* uuid_create_md5_from_name -- create a version 3 (MD5) UUID using a
   "name" from a "name space" */
void uuid_create_md5_from_name(
    uuid_t *uuid,         /* resulting UUID */
    uuid_t nsid,          /* UUID of the namespace */
    void *name,           /* the name from which to generate a UUID */
    int namelen           /* the length of the name */
);

/* uuid_create_sha1_from_name -- create a version 5 (SHA-1) UUID
   using a "name" from a "name space" */
void uuid_create_sha1_from_name(
    uuid_t *uuid,         /* resulting UUID */
    uuid_t nsid,          /* UUID of the namespace */
    void *name,           /* the name from which to generate a UUID */
    int namelen           /* the length of the name */
);

/* uuid_compare --  Compare two UUID's "lexically" and return
        -1   u1 is lexically before u2
         0   u1 is equal to u2
         1   u1 is lexically after u2
   Note that lexical ordering is not temporal ordering!
*/
int uuid_compare(uuid_t *u1, uuid_t *u2);
```

### uuid.c

```c
#include "copyrt.h"
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include "sysdep.h"
#include "uuid.h"

/* various forward declarations */
static int read_state(unsigned16 *clockseq, uuid_time_t *timestamp,
    uuid_node_t *node);
static void write_state(unsigned16 clockseq, uuid_time_t timestamp,
    uuid_node_t node);
static void format_uuid_v1(uuid_t *uuid, unsigned16 clockseq,
    uuid_time_t timestamp, uuid_node_t node);
static void format_uuid_v3or5(uuid_t *uuid, unsigned char hash[16],
    int v);
static void get_current_time(uuid_time_t *timestamp);
static unsigned16 true_random(void);

/* uuid_create -- generator a UUID */
int uuid_create(uuid_t *uuid)
{
     uuid_time_t timestamp, last_time;
     unsigned16 clockseq;
     uuid_node_t node;
     uuid_node_t last_node;
     int f;

     /* acquire system-wide lock so we're alone */
     LOCK;
     /* get time, node ID, saved state from non-volatile storage */
     get_current_time(&timestamp);
     get_ieee_node_identifier(&node);
     f = read_state(&clockseq, &last_time, &last_node);

     /* if no NV state, or if clock went backwards, or node ID
        changed (e.g., new network card) change clockseq */
     if (!f || memcmp(&node, &last_node, sizeof node))
         clockseq = true_random();
     else if (timestamp < last_time)
         clockseq++;

     /* save the state for next time */
     write_state(clockseq, timestamp, node);

     UNLOCK;

     /* stuff fields into the UUID */
     format_uuid_v1(uuid, clockseq, timestamp, node);
     return 1;
}

/* format_uuid_v1 -- make a UUID from the timestamp, clockseq,
                     and node ID */
void format_uuid_v1(uuid_t* uuid, unsigned16 clock_seq,
                    uuid_time_t timestamp, uuid_node_t node)
{
    /* Construct a version 1 uuid with the information we've gathered
       plus a few constants. */
    uuid->time_low = (unsigned long)(timestamp & 0xFFFFFFFF);
    uuid->time_mid = (unsigned short)((timestamp >> 32) & 0xFFFF);
    uuid->time_hi_and_version =
        (unsigned short)((timestamp >> 48) & 0x0FFF);
    uuid->time_hi_and_version |= (1 << 12);
    uuid->clock_seq_low = clock_seq & 0xFF;
    uuid->clock_seq_hi_and_reserved = (clock_seq & 0x3F00) >> 8;
    uuid->clock_seq_hi_and_reserved |= 0x80;
    memcpy(&uuid->node, &node, sizeof uuid->node);
}

/* data type for UUID generator persistent state */
typedef struct {
    uuid_time_t  ts;       /* saved timestamp */
    uuid_node_t  node;     /* saved node ID */
    unsigned16   cs;       /* saved clock sequence */
} uuid_state;

static uuid_state st;

/* read_state -- read UUID generator state from non-volatile store */
int read_state(unsigned16 *clockseq, uuid_time_t *timestamp,
               uuid_node_t *node)
{
    static int inited = 0;
    FILE *fp;

    /* only need to read state once per boot */
    if (!inited) {
        fp = fopen("state", "rb");
        if (fp == NULL)
            return 0;
        fread(&st, sizeof st, 1, fp);
        fclose(fp);
        inited = 1;
    }
    *clockseq = st.cs;
    *timestamp = st.ts;
    *node = st.node;
    return 1;
}

/* write_state -- save UUID generator state back to non-volatile
   storage */
void write_state(unsigned16 clockseq, uuid_time_t timestamp,
                 uuid_node_t node)
{
    static int inited = 0;
    static uuid_time_t next_save;
    FILE* fp;

    if (!inited) {
        next_save = timestamp;
        inited = 1;
    }

    /* always save state to volatile shared state */
    st.cs = clockseq;
    st.ts = timestamp;
    st.node = node;
    if (timestamp >= next_save) {
        fp = fopen("state", "wb");
        fwrite(&st, sizeof st, 1, fp);
        fclose(fp);
        /* schedule next save for 10 seconds from now */
        next_save = timestamp + (10 * 10 * 1000 * 1000);
    }
}

/* get-current_time -- get time as 60-bit 100ns ticks since UUID epoch.
   Compensate for the fact that real clock resolution is
   less than 100ns. */
void get_current_time(uuid_time_t *timestamp)
{
    static int inited = 0;
    static uuid_time_t time_last;
    static unsigned16 uuids_this_tick;
    uuid_time_t time_now;

    if (!inited) {
        get_system_time(&time_now);
        uuids_this_tick = UUIDS_PER_TICK;
        inited = 1;
    }

    for ( ; ; ) {
        get_system_time(&time_now);

        /* if clock reading changed since last UUID generated, */
        if (time_last != time_now) {
            /* reset count of uuids gen'd with this clock reading */
            uuids_this_tick = 0;
            time_last = time_now;
            break;
        }
        if (uuids_this_tick < UUIDS_PER_TICK) {
            uuids_this_tick++;
            break;
        }

        /* going too fast for our clock; spin */
    }
    /* add the count of uuids to low order bits of the clock reading */
    *timestamp = time_now + uuids_this_tick;
}

/* true_random -- generate a crypto-quality random number.
   This sample doesn't do that. */
static unsigned16 true_random(void)
{
    static int inited = 0;
    uuid_time_t time_now;

    if (!inited) {
        get_system_time(&time_now);
        time_now = time_now / UUIDS_PER_TICK;
        srand((unsigned int)
               (((time_now >> 32) ^ time_now) & 0xffffffff));
        inited = 1;
    }

    return rand();
}

/* uuid_create_md5_from_name -- create a version 3 (MD5) UUID using a
   "name" from a "name space" */
void uuid_create_md5_from_name(uuid_t *uuid, uuid_t nsid, void *name,
                               int namelen)
{
    MD5_CTX c;
    unsigned char hash[16];
    uuid_t net_nsid;

    /* put name space ID in network byte order so it hashes the same
       no matter what endian machine we're on */
    net_nsid = nsid;
    net_nsid.time_low = htonl(net_nsid.time_low);
    net_nsid.time_mid = htons(net_nsid.time_mid);
    net_nsid.time_hi_and_version = htons(net_nsid.time_hi_and_version);

    MD5Init(&c);
    MD5Update(&c, &net_nsid, sizeof net_nsid);
    MD5Update(&c, name, namelen);
    MD5Final(hash, &c);

    /* the hash is in network byte order at this point */
    format_uuid_v3or5(uuid, hash, 3);
}

void uuid_create_sha1_from_name(uuid_t *uuid, uuid_t nsid, void *name,
                                int namelen)
{
    SHA_CTX c;
    unsigned char hash[20];
    uuid_t net_nsid;

    net_nsid = nsid;
    net_nsid.time_low = htonl(net_nsid.time_low);
    net_nsid.time_mid = htons(net_nsid.time_mid);
    net_nsid.time_hi_and_version = htons(net_nsid.time_hi_and_version);

    SHA1_Init(&c);
    SHA1_Update(&c, &net_nsid, sizeof net_nsid);
    SHA1_Update(&c, name, namelen);
    SHA1_Final(hash, &c);

    /* the hash is in network byte order at this point */
    format_uuid_v3or5(uuid, hash, 5);
}

/* format_uuid_v3or5 -- make a UUID from a (pseudo)random 128-bit
   number */
void format_uuid_v3or5(uuid_t *uuid, unsigned char hash[16], int v)
{
    /* convert UUID to local byte order */
    memcpy(uuid, hash, sizeof *uuid);
    uuid->time_low = ntohl(uuid->time_low);
    uuid->time_mid = ntohs(uuid->time_mid);
    uuid->time_hi_and_version = ntohs(uuid->time_hi_and_version);

    /* put in the variant and version bits */
    uuid->time_hi_and_version &= 0x0FFF;
    uuid->time_hi_and_version |= (v << 12);
    uuid->clock_seq_hi_and_reserved &= 0x3F;
    uuid->clock_seq_hi_and_reserved |= 0x80;
}

/* uuid_compare --  Compare two UUID's "lexically" and return */
#define CHECK(f1, f2) if (f1 != f2) return f1 < f2 ? -1 : 1;
int uuid_compare(uuid_t *u1, uuid_t *u2)
{
    int i;

    CHECK(u1->time_low, u2->time_low);
    CHECK(u1->time_mid, u2->time_mid);
    CHECK(u1->time_hi_and_version, u2->time_hi_and_version);
    CHECK(u1->clock_seq_hi_and_reserved, u2->clock_seq_hi_and_reserved);
    CHECK(u1->clock_seq_low, u2->clock_seq_low)
    for (i = 0; i < 6; i++) {
        if (u1->node[i] < u2->node[i])
            return -1;
        if (u1->node[i] > u2->node[i])
            return 1;
    }
    return 0;
}
#undef CHECK
```

### sysdep.h

```c
#include "copyrt.h"
/* remove the following define if you aren't running WIN32 */
#define WININC 0

#ifdef WININC
#include <windows.h>
#else
#include <sys/types.h>
#include <sys/time.h>
#include <sys/sysinfo.h>
#endif

#include "global.h"
/* change to point to where MD5 .h's live; RFC 1321 has sample
   implementation */
#include "md5.h"

/* set the following to the number of 100ns ticks of the actual
   resolution of your system's clock */
#define UUIDS_PER_TICK 1024

/* Set the following to a calls to get and release a global lock */
#define LOCK
#define UNLOCK

typedef unsigned long   unsigned32;
typedef unsigned short  unsigned16;
typedef unsigned char   unsigned8;
typedef unsigned char   byte;

/* Set this to what your compiler uses for 64-bit data type */
#ifdef WININC
#define unsigned64_t unsigned __int64
#define I64(C) C
#else
#define unsigned64_t unsigned long long
#define I64(C) C##LL
#endif

typedef unsigned64_t uuid_time_t;
typedef struct {
    char nodeID[6];
} uuid_node_t;

void get_ieee_node_identifier(uuid_node_t *node);
void get_system_time(uuid_time_t *uuid_time);
void get_random_info(char seed[16]);
```

### sysdep.c

```c
#include "copyrt.h"
#include <stdio.h>
#include "sysdep.h"

/* system dependent call to get IEEE node ID.
   This sample implementation generates a random node ID. */
void get_ieee_node_identifier(uuid_node_t *node)
{
    static inited = 0;
    static uuid_node_t saved_node;
    char seed[16];
    FILE *fp;

    if (!inited) {
        fp = fopen("nodeid", "rb");
        if (fp) {
            fread(&saved_node, sizeof saved_node, 1, fp);
            fclose(fp);
        }
        else {
            get_random_info(seed);
            seed[0] |= 0x01;
            memcpy(&saved_node, seed, sizeof saved_node);
            fp = fopen("nodeid", "wb");
            if (fp) {
                fwrite(&saved_node, sizeof saved_node, 1, fp);
                fclose(fp);
            }
        }

        inited = 1;
    }

    *node = saved_node;
}

/* system dependent call to get the current system time. Returned as
   100ns ticks since UUID epoch, but resolution may be less than
   100ns. */
#ifdef _WINDOWS_

void get_system_time(uuid_time_t *uuid_time)
{
    ULARGE_INTEGER time;

    /* NT keeps time in FILETIME format which is 100ns ticks since
       Jan 1, 1601. UUIDs use time in 100ns ticks since Oct 15, 1582.
       The difference is 17 Days in Oct + 30 (Nov) + 31 (Dec)
       + 18 years and 5 leap days. */
    GetSystemTimeAsFileTime((FILETIME *)&time);
    time.QuadPart +=
          (unsigned __int64) (1000*1000*10)       // seconds
        * (unsigned __int64) (60 * 60 * 24)       // days
        * (unsigned __int64) (17+30+31+365*18+5); // # of days
    *uuid_time = time.QuadPart;
}

/* Sample code, not for use in production; see RFC 1750 */
void get_random_info(char seed[16])
{
    MD5_CTX c;
    struct {
        MEMORYSTATUS m;
        SYSTEM_INFO s;
        FILETIME t;
        LARGE_INTEGER pc;
        DWORD tc;
        DWORD l;
        char hostname[MAX_COMPUTERNAME_LENGTH + 1];
    } r;

    MD5Init(&c);
    GlobalMemoryStatus(&r.m);
    GetSystemInfo(&r.s);
    GetSystemTimeAsFileTime(&r.t);
    QueryPerformanceCounter(&r.pc);
    r.tc = GetTickCount();
    r.l = MAX_COMPUTERNAME_LENGTH + 1;
    GetComputerName(r.hostname, &r.l);
    MD5Update(&c, &r, sizeof r);
    MD5Final(seed, &c);
}

#else

void get_system_time(uuid_time_t *uuid_time)
{
    struct timeval tp;

    gettimeofday(&tp, (struct timezone *)0);

    /* Offset between UUID formatted times and Unix formatted times.
       UUID UTC base time is October 15, 1582.
       Unix base time is January 1, 1970.*/
    *uuid_time = ((unsigned64)tp.tv_sec * 10000000)
        + ((unsigned64)tp.tv_usec * 10)
        + I64(0x01B21DD213814000);
}

/* Sample code, not for use in production; see RFC 1750 */
void get_random_info(char seed[16])
{
    MD5_CTX c;
    struct {
        struct sysinfo s;
        struct timeval t;
        char hostname[257];
    } r;

    MD5Init(&c);
    sysinfo(&r.s);
    gettimeofday(&r.t, (struct timezone *)0);
    gethostname(r.hostname, 256);
    MD5Update(&c, &r, sizeof r);
    MD5Final(seed, &c);
}

#endif
```

### utest.c

```c
#include "copyrt.h"
#include "sysdep.h"
#include <stdio.h>
#include "uuid.h"

uuid_t NameSpace_DNS = { /* 6ba7b810-9dad-11d1-80b4-00c04fd430c8 */
    0x6ba7b810,
    0x9dad,
    0x11d1,
    0x80, 0xb4, {0x00, 0xc0, 0x4f, 0xd4, 0x30, 0xc8}
};

/* puid -- print a UUID */
void puid(uuid_t u)
{
    int i;

    printf("%8.8x-%4.4x-%4.4x-%2.2x%2.2x-", u.time_low, u.time_mid,
    u.time_hi_and_version, u.clock_seq_hi_and_reserved,
    u.clock_seq_low);
    for (i = 0; i < 6; i++)
        printf("%2.2x", u.node[i]);
    printf("\n");
}

/* Simple driver for UUID generator */
void main(int argc, char argv)
{
    uuid_t u;
    int f;

    uuid_create(&u);
    printf("uuid_create(): "); puid(u);

    f = uuid_compare(&u, &u);
    printf("uuid_compare(u,u): %d\n", f);     /* should be 0 */
    f = uuid_compare(&u, &NameSpace_DNS);
    printf("uuid_compare(u, NameSpace_DNS): %d\n", f); /* s.b. 1 */
    f = uuid_compare(&NameSpace_DNS, &u);
    printf("uuid_compare(NameSpace_DNS, u): %d\n", f); /* s.b. -1 */
    uuid_create_md5_from_name(&u, NameSpace_DNS, "www.widgets.com", 15);
    printf("uuid_create_md5_from_name(): "); puid(u);
}
```

## 부록 B - 출력 예시

- 위 코드 실행 시 출력 예시 (시간 기반 UUID와 비교 결과 중 하나는 실행마다 다름)

```
uuid_create(): 7d444840-9dc0-11d1-b245-5ffdce74fad2
uuid_compare(u,u): 0
uuid_compare(u, NameSpace_DNS): 1
uuid_compare(NameSpace_DNS, u): -1
uuid_create_md5_from_name(): e902893a-9d22-3c7e-a7b8-d6e313b71d9f
```

## 부록 C - 미리 정의된 몇 가지 네임스페이스 ID

미리 정의된 네임스페이스로 사용 가능한 UUID 값:

```c
/* 이름 문자열이 정규화된 도메인 이름인 경우 */
uuid_t NameSpace_DNS = { /* 6ba7b810-9dad-11d1-80b4-00c04fd430c8 */
    0x6ba7b810,
    0x9dad,
    0x11d1,
    0x80, 0xb4, {0x00, 0xc0, 0x4f, 0xd4, 0x30, 0xc8}
};

/* 이름 문자열이 URL인 경우 */
uuid_t NameSpace_URL = { /* 6ba7b811-9dad-11d1-80b4-00c04fd430c8 */
    0x6ba7b811,
    0x9dad,
    0x11d1,
    0x80, 0xb4, {0x00, 0xc0, 0x4f, 0xd4, 0x30, 0xc8}
};

/* 이름 문자열이 ISO OID인 경우 */
uuid_t NameSpace_OID = { /* 6ba7b812-9dad-11d1-80b4-00c04fd430c8 */
    0x6ba7b812,
    0x9dad,
    0x11d1,
    0x80, 0xb4, {0x00, 0xc0, 0x4f, 0xd4, 0x30, 0xc8}
};

/* 이름 문자열이 X.500 DN (DER 또는 텍스트 출력 형식)인 경우 */
uuid_t NameSpace_X500 = { /* 6ba7b814-9dad-11d1-80b4-00c04fd430c8 */
    0x6ba7b814,
    0x9dad,
    0x11d1,
    0x80, 0xb4, {0x00, 0xc0, 0x4f, 0xd4, 0x30, 0xc8}
};
```

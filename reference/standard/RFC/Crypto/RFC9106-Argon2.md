# RFC 9106 - 패스워드 해싱 및 작업 증명 애플리케이션을 위한 Argon2 메모리 하드 함수

```
Internet Research Task Force (IRTF)                          A. Biryukov
Request for Comments: 9106                                       D. Dinu
Category: Informational                         University of Luxembourg
ISSN: 2070-1721                                          D. Khovratovich
                                                         ABDK Consulting
                                                            S. Josefsson
                                                                  SJD AB
                                                          September 2021
```

### Abstract

- 패스워드 해싱 및 작업 증명 애플리케이션을 위한 Argon2 메모리 하드 함수 설명
- 테스트 벡터와 함께 구현자 지향 설명 제공
- 목적: 인터넷 프로토콜에서 Argon2 채택 간소화
- 이 문서는 IRTF 암호 포럼 연구 그룹(CFRG)의 산출물

## 이 메모의 상태

- 인터넷 표준 트랙 규격 아님, 정보 제공 목적으로 발행
- 인터넷 연구 태스크 포스(IRTF)의 산출물
  - IRTF는 인터넷 관련 연구 및 개발 활동의 결과를 발행
  - 이러한 결과는 배포에 적합하지 않을 수 있음
- 이 RFC는 IRTF 암호 포럼 연구 그룹의 합의를 나타냄
- IRSG에 의해 발행 승인된 문서는 어떤 수준의 인터넷 표준 후보도 아님 → RFC 7841 섹션 2 참고

이 문서의 현재 상태, 정오표 및 피드백 제공 방법: https://www.rfc-editor.org/info/rfc9106

## 저작권 고지

- Copyright (c) 2021 IETF Trust 및 문서 저자로 식별된 사람들. 모든 권리 보유.
- 이 문서는 발행일 기준 유효한 BCP 78 및 IETF 문서에 관한 IETF Trust의 법적 조항(https://trustee.ietf.org/license-info) 적용 대상 → 권리와 제한 사항 확인을 위해 검토 필요

## 목차

```
1.  소개
  1.1.  요구사항 언어
2.  표기법 및 규칙
3.  Argon2 알고리즘
  3.1.  Argon2 입력 및 출력
  3.2.  Argon2 연산
  3.3.  가변 길이 해시 함수 H'
  3.4.  인덱싱
    3.4.1.  32비트 값 J_1 및 J_2 계산
    3.4.2.  J_1 및 J_2를 참조 블록 인덱스 [l][z]로 매핑
  3.5.  압축 함수 G
  3.6.  순열 P
4.  매개변수 선택
5.  테스트 벡터
  5.1.  Argon2d 테스트 벡터
  5.2.  Argon2i 테스트 벡터
  5.3.  Argon2id 테스트 벡터
6.  IANA 고려사항
7.  보안 고려사항
  7.1.  해시 함수 및 KDF로서의 보안
  7.2.  시간-공간 트레이드오프 공격에 대한 보안
  7.3.  시간 제한 방어자를 위한 보안
  7.4.  권장사항
8.  참고문헌
  8.1.  규범적 참고문헌
  8.2.  정보적 참고문헌
감사의 글
저자 주소
```

## 1. 소개

- 패스워드 해싱 및 작업 증명 애플리케이션을 위한 Argon2 [ARGON2ESP] 메모리 하드 함수 설명
- 테스트 벡터와 함께 구현자 지향 설명 제공
- 목적: 인터넷 프로토콜에서 Argon2 채택 간소화
- 이 문서는 Argon2 해시 함수 버전 1.3에 해당

Argon2 특징:

- 메모리 하드 함수 [HARD], 간소화된 설계
- 트레이드오프 공격 방어 + 최고 메모리 채우기 속도 + 다중 계산 유닛 효과적 사용 목표
- x86 아키텍처에 최적화 → 최신 Intel/AMD 프로세서의 캐시·메모리 구성 활용
- 변형 3종
  - Argon2id: 주요 변형
  - Argon2d: 보조 변형, 데이터 의존적 메모리 접근 → 사이드 채널 타이밍 공격 위협 없는 암호화폐·작업 증명 애플리케이션에 적합
  - Argon2i: 보조 변형, 데이터 독립적 메모리 접근 → 패스워드 해싱 및 패스워드 기반 키 유도에 선호
  - Argon2id: 첫 번째 패스 전반부는 Argon2i, 나머지는 Argon2d로 동작 → 사이드 채널 공격 보호 + 시간-메모리 트레이드오프로 인한 무차별 대입 비용 절감 모두 제공
- Argon2i는 트레이드오프 공격 방어를 위해 메모리에 대해 더 많은 패스 수행 [AB15]

지원 요구사항:

- Argon2id: 이 문서의 모든 구현에서 반드시(MUST) 지원
- Argon2d, Argon2i: 선택적으로(MAY) 지원 가능

- Argon2는 고정 입력 길이 압축 함수 G와 가변 입력 길이 해시 함수 H에 대한 연산 모드
- 임의의 함수 H와 함께 사용 가능하나, 최대 64바이트까지 출력 제공 조건 하에 이 문서에서는 BLAKE2b 함수 [BLAKE2] 사용
- 배경 및 추가 논의: Argon2 논문 [ARGON2] 참고
- 이 문서는 암호 포럼 연구 그룹(CFRG)의 합의를 나타냄

### 1.1. 요구사항 언어

이 문서의 키워드 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "NOT RECOMMENDED", "MAY", "OPTIONAL"은 여기에 표시된 것처럼 모두 대문자로 나타날 때, 그리고 오직 그때에만 BCP 14 [RFC2119] [RFC8174]에 설명된 대로 해석되어야 한다.

## 2. 표기법 및 규칙

```
x^y            정수 x를 자기 자신으로 정수 y번 곱함

a*b            정수 a와 정수 b의 곱셈

c-d            정수 c에서 정수 d를 뺌

E_f            아래첨자 인덱스 f를 가진 변수 E

g / h          정수 g를 정수 h로 나눔. 결과는 유리수이다.

I(j)           j에서 평가된 함수 I

K || L         문자열 K와 문자열 L의 연결

a XOR b        비트문자열 a와 b 사이의 비트 단위 배타적 논리합

a mod b        정수 a를 정수 b로 나눈 나머지, 항상 [0, b-1] 범위

a >>> n        64비트 문자열 a를 n비트 오른쪽으로 회전

trunc(a)       64비트 값을 최하위 32비트로 잘라냄

floor(a)       a보다 크지 않은 가장 큰 정수

ceil(a)        a보다 작지 않은 가장 작은 정수

extract(a, i)  비트문자열 a에서 0번째부터 시작하여 i번째 32비트 집합

|A|            집합 A의 원소 수

LE32(a)        32비트 정수 a를 리틀 엔디안 바이트 문자열로 변환
               (예: 123456(10진수)는 40 E2 01 00)

LE64(a)        64비트 정수 a를 리틀 엔디안 바이트 문자열로 변환
               (예: 123456(10진수)는 40 E2 01 00 00 00 00 00)

int32(s)       32비트 문자열 s를 리틀 엔디안으로 음이 아닌 정수로 변환

int64(s)       64비트 문자열 s를 리틀 엔디안으로 음이 아닌 정수로 변환

length(P)      문자열 P의 바이트 길이를 32비트 정수로 표현

ZERO(P)        P바이트 영 문자열
```

## 3. Argon2 알고리즘

### 3.1. Argon2 입력 및 출력

Argon2 입력 매개변수:

- 메시지 문자열 P
  - 패스워드 해싱 애플리케이션의 경우 패스워드
  - 길이는 2^(32)-1 바이트 초과 금지(MUST)
- 논스 S
  - 패스워드 해싱 애플리케이션의 경우 솔트
  - 길이는 2^(32)-1 바이트 초과 금지(MUST)
  - 패스워드 해싱의 경우 16바이트 권장(RECOMMENDED)
  - 각 패스워드마다 고유해야 함(SHOULD)
- 병렬 처리 정도 p
  - 독립적(그러나 동기화되는) 계산 체인(레인) 실행 개수 결정
  - 1 ~ 2^(24)-1 사이 정수 값(MUST)
- 태그 길이 T
  - 4 ~ 2^(32)-1 사이 정수 바이트 수(MUST)
- 메모리 크기 m
  - 8*p ~ 2^(32)-1 사이 정수 키비바이트 수(MUST)
  - 실제 블록 수는 m' → m을 4*p의 가장 가까운 배수로 내림한 값
- 패스 수 t
  - 메모리 크기와 독립적으로 실행 시간 조정 용도
  - 1 ~ 2^(32)-1 사이 정수 수(MUST)
- 버전 번호 v
  - 1바이트 0x13(MUST)
- 비밀 값 K
  - 선택사항(OPTIONAL)
  - 사용 시 길이는 2^(32)-1 바이트 초과 금지(MUST)
- 연관 데이터 X
  - 선택사항(OPTIONAL)
  - 사용 시 길이는 2^(32)-1 바이트 초과 금지(MUST)
- 타입 y
  - Argon2d: 0, Argon2i: 1, Argon2id: 2 (MUST)

Argon2 출력("태그")은 T바이트 길이의 문자열.

### 3.2. Argon2 연산

- Argon2는 다음 함수 사용
  - 내부 압축 함수 G: 두 개의 1024바이트 입력, 1024바이트 출력
  - 내부 해시 함수 H^x(): x는 바이트 단위 출력 길이
    - 문자열 A에 적용된 H^x()는 BLAKE2b([BLAKE2] 섹션 3.3) 함수
    - 매개변수 (d,ll,kk=0,nn=x) 사용, d는 A를 128바이트 배수로 패딩한 것, ll은 d의 바이트 길이
  - 압축 함수 G는 내부 순열에 기반
  - H 기반 가변 길이 해시 함수 H'도 사용
- G는 3.5절, H'는 3.3절에 설명

Argon2 연산 절차:

1. H_0를 아래 64바이트 값으로 설정
   - K, X, S의 길이가 0인 경우 → 값은 부재하지만 길이 필드는 유지

```
H_0 = H^(64)(LE32(p) || LE32(T) || LE32(m) || LE32(t) ||
        LE32(v) || LE32(y) || LE32(length(P)) || P ||
        LE32(length(S)) || S ||  LE32(length(K)) || K ||
        LE32(length(X)) || X)

                         그림 1: H_0 생성
```

2. 메모리를 m' 1024바이트 블록으로 할당, m'은 다음과 같이 유도:

```
m' = 4 * p * floor (m / 4p)

                       그림 2: 메모리 할당
```

   - p개 레인 기준, 메모리는 p개 행(레인)과 q = m' / p개 열을 가진 블록 행렬 B[i][j]로 구성

3. 0 이상 p 미만 범위의 모든 i에 대해 B[i][0] 계산:

```
B[i][0] = H'^(1024)(H_0 || LE32(0) || LE32(i))

                      그림 3: 레인 시작 블록
```

4. 0 이상 p 미만 범위의 모든 i에 대해 B[i][1] 계산:

```
B[i][1] = H'^(1024)(H_0 || LE32(1) || LE32(i))

                       그림 4: 두 번째 레인 블록
```

5. 0 이상 p 미만 범위의 모든 i와 2 이상 q 미만 범위의 모든 j에 대해 B[i][j] 계산
   - 계산은 반드시(MUST) 슬라이스 단위로(3.4절) 진행 → 먼저 모든 레인에 대해 슬라이스 0의 블록 계산(레인 순서는 임의), 이후 슬라이스 1의 블록 계산 순으로 진행
   - 블록 인덱스 l과 z는 Argon2d, Argon2i, Argon2id에 대해 각 i, j마다 다르게 결정

```
B[i][j] = G(B[i][j-1], B[l][z])

                    그림 5: 추가 블록 생성
```

6. 패스 수 t가 1보다 큰 경우 → 단계 5 반복
   - 0 이상 p 미만 범위의 모든 i와 1 이상 q 미만 범위의 모든 j에 대해 B[i][0]과 B[i][j] 계산
   - 단, 이전 값과 새 값이 XOR되므로 다르게 계산:

```
B[i][0] = G(B[i][q-1], B[l][z]) XOR B[i][0];
B[i][j] = G(B[i][j-1], B[l][z]) XOR B[i][j].

                         그림 6: 추가 패스
```

7. t 단계 반복 후 → 최종 블록 C는 마지막 열의 XOR로 계산:

```
C = B[0][q-1] XOR B[1][q-1] XOR ... XOR B[p-1][q-1]

                          그림 7: 최종 블록
```

8. 출력 태그는 H'^T(C)로 계산

### 3.3. 가변 길이 해시 함수 H'

- V_i를 64바이트 블록, W_i를 그 처음 32바이트로 정의
- 함수 H' 정의:

```
        if T <= 64
            H'^T(A) = H^T(LE32(T)||A)
        else
            r = ceil(T/32)-2
            V_1 = H^(64)(LE32(T)||A)
            V_2 = H^(64)(V_1)
            ...
            V_r = H^(64)(V_{r-1})
            V_{r+1} = H^(T-32*r)(V_{r})
            H'^T(X) = W_1 || W_2 || ... || W_r || V_{r+1}

     그림 8: 태그 및 초기 블록 계산을 위한 함수 H'
```

### 3.4. 인덱싱

- 병렬 블록 계산을 위해 메모리 행렬을 SL = 4개의 수직 슬라이스로 분할
- 슬라이스와 레인의 교차점 = 세그먼트, 길이는 q/SL
- 동일 슬라이스의 세그먼트는 병렬 계산 가능 → 서로 블록 참조 안 함
- 다른 모든 블록은 참조 가능

```
    slice 0    slice 1    slice 2    slice 3
    ___/\___   ___/\___   ___/\___   ___/\___
   /        \ /        \ /        \ /        \
  +----------+----------+----------+----------+
  |          |          |          |          | > lane 0
  +----------+----------+----------+----------+
  |          |          |          |          | > lane 1
  +----------+----------+----------+----------+
  |          |          |          |          | > lane 2
  +----------+----------+----------+----------+
  |         ...        ...        ...         | ...
  +----------+----------+----------+----------+
  |          |          |          |          | > lane p - 1
  +----------+----------+----------+----------+

        그림 9: p개의 레인과 4개의 슬라이스를 가진 단일 패스 Argon2
```

#### 3.4.1. 32비트 값 J_1 및 J_2 계산

##### 3.4.1.1. Argon2d

- J_1: 블록 B[i][j-1]의 처음 32비트
- J_2: 블록 B[i][j-1]의 다음 32비트

```
J_1 = int32(extract(B[i][j-1], 0))
J_2 = int32(extract(B[i][j-1], 1))

                 그림 10: Argon2d에서 J1,J2 유도
```

##### 3.4.1.2. Argon2i

- 각 세그먼트에 대해 값 Z 계산:

```
Z= ( LE64(r) || LE64(l) || LE64(sl) || LE64(m') ||
     LE64(t) || LE64(y) )

             그림 11: Argon2i에서 J1,J2 계산을 위한 입력
```

- 여기서
  - r: 패스 번호
  - l: 레인 번호
  - sl: 슬라이스 번호
  - m': 총 메모리 블록 수
  - t: 총 패스 수
  - y: Argon2 타입(Argon2d=0, Argon2i=1, Argon2id=2)

- 이후 계산:

```
q/(128*SL) 1024바이트 값
G(ZERO(1024),G(ZERO(1024),
Z || LE64(1) || ZERO(968) )),
G(ZERO(1024),G(ZERO(1024),
Z || LE64(2) || ZERO(968) )),... ,
G(ZERO(1024),G(ZERO(1024),
Z || LE64(q/(128*SL)) || ZERO(968) )),
```

- 이는 q/(SL)개의 8바이트 값 X로 분할 → X1||X2로 보고 J_1=int32(X1), J_2=int32(X2)로 변환
- 값 r, l, sl, m', t, y, i는 리틀 엔디안 8바이트로 표현

##### 3.4.1.3. Argon2id

- 패스 번호 0, 슬라이스 번호 0 또는 1 → Argon2i처럼 J_1, J_2 계산
- 그 외 → Argon2d처럼 J_1, J_2 계산

#### 3.4.2. J_1 및 J_2를 참조 블록 인덱스 [l][z]로 매핑

- l = J_2 mod p → 블록을 가져올 레인 인덱스
  - 첫 번째 패스(r=0), 첫 번째 슬라이스(sl=0) → 블록은 현재 레인에서 가져옴

집합 W의 참조 인덱스 규칙:

1. l이 현재 레인인 경우 → W는 계산·완료된 마지막 SL - 1 = 3개 세그먼트의 모든 블록 인덱스와 B[i][j-1]을 제외한 현재 패스의 현재 세그먼트에서 계산된 블록 포함
2. l이 현재 레인이 아닌 경우 → W는 레인 l에서 계산·완료된 마지막 SL - 1 = 3개 세그먼트의 모든 블록 인덱스 포함
   - B[i][j]가 세그먼트의 첫 번째 블록인 경우 → W에서 맨 마지막 인덱스 제외

- 이후 다음 매핑으로 [0, |W|)에 대한 비균일 분포로 W에서 블록 가져오기:

```
J_1 -> |W|(1 - J_1^2 / 2^(64))

                       그림 12: J1 계산
```

- 부동 소수점 계산 회피를 위해 다음 근사 사용:

```
x = J_1^2 / 2^(32)
y = (|W| * x) / 2^(32)
zz = |W| - 1 - y

                   그림 13: J1 계산, 파트 2
```

- W에서 zz번째 인덱스 획득 → 참조 블록 인덱스 [l][z]의 z 값

### 3.5. 압축 함수 G

- 압축 함수 G는 BLAKE2b 기반 변환 P에 기반
- P는 8개의 16바이트 레지스터로 볼 수 있는 128바이트 입력에서 동작:

```
P(A_0, A_1, ... ,A_7) = (B_0, B_1, ... ,B_7)

                  그림 14: Blake 라운드 함수 P
```

- 압축 함수 G(X, Y)는 두 개의 1024바이트 블록 X, Y에서 동작
  - 1단계: R = X XOR Y 계산
  - 2단계: R을 16바이트 레지스터 R_0, R_1, ... , R_63의 8x8 행렬로 취급
  - 3단계: Z를 얻기 위해 P를 각 행 → 각 열 순으로 적용:

```
( Q_0,  Q_1,  Q_2, ... ,  Q_7) <- P( R_0,  R_1,  R_2, ... ,  R_7)
( Q_8,  Q_9, Q_10, ... , Q_15) <- P( R_8,  R_9, R_10, ... , R_15)
                              ...
(Q_56, Q_57, Q_58, ... , Q_63) <- P(R_56, R_57, R_58, ... , R_63)
( Z_0,  Z_8, Z_16, ... , Z_56) <- P( Q_0,  Q_8, Q_16, ... , Q_56)
( Z_1,  Z_9, Z_17, ... , Z_57) <- P( Q_1,  Q_9, Q_17, ... , Q_57)
                              ...
( Z_7, Z_15, Z 23, ... , Z_63) <- P( Q_7, Q_15, Q_23, ... , Q_63)

              그림 15: 압축 함수 G의 핵심
```

- 마지막으로 G는 Z XOR R을 출력:

```
G: (X, Y) -> R -> Q -> Z -> Z XOR R

                         +---+       +---+
                         | X |       | Y |
                         +---+       +---+
                           |           |
                           ---->XOR<----
                         --------|
                         |      \ /
                         |     +---+
                         |     | R |
                         |     +---+
                         |       |
                         |      \ /
                         |   P rowwise
                         |       |
                         |      \ /
                         |     +---+
                         |     | Q |
                         |     +---+
                         |       |
                         |      \ /
                         |  P columnwise
                         |       |
                         |      \ /
                         |     +---+
                         |     | Z |
                         |     +---+
                         |       |
                         |      \ /
                         ------>XOR
                                 |
                                \ /

               그림 16: Argon2 압축 함수 G
```

### 3.6. 순열 P

- 순열 P는 BLAKE2b의 라운드 함수에 기반
- 8개의 16바이트 입력 S_0, S_1, ... , S_7은 64비트 워드의 4x4 행렬로 취급, S_i = (v_{2*i+1} || v_{2*i}):

```
         v_0  v_1  v_2  v_3
         v_4  v_5  v_6  v_7
         v_8  v_9 v_10 v_11
        v_12 v_13 v_14 v_15

                  그림 17: 행렬 요소 레이블링
```

동작 순서:

```
        GB(v_0, v_4,  v_8, v_12)
        GB(v_1, v_5,  v_9, v_13)
        GB(v_2, v_6, v_10, v_14)
        GB(v_3, v_7, v_11, v_15)

        GB(v_0, v_5, v_10, v_15)
        GB(v_1, v_6, v_11, v_12)
        GB(v_2, v_7,  v_8, v_13)
        GB(v_3, v_4,  v_9, v_14)

               그림 18: GB에 행렬 요소 공급
```

GB(a, b, c, d) 정의:

```
        a = (a + b + 2 * trunc(a) * trunc(b)) mod 2^(64)
        d = (d XOR a) >>> 32
        c = (c + d + 2 * trunc(c) * trunc(d)) mod 2^(64)
        b = (b XOR c) >>> 24

        a = (a + b + 2 * trunc(a) * trunc(b)) mod 2^(64)
        d = (d XOR a) >>> 16
        c = (c + d + 2 * trunc(c) * trunc(d)) mod 2^(64)
        b = (b XOR c) >>> 63

                       그림 19: GB의 세부사항
```

- GB의 모듈러 덧셈은 64비트 곱셈과 결합
  - 곱셈은 원래 BLAKE2b 설계와의 유일한 차이점
  - 목적: 회로 깊이 증가 → ASIC 구현의 실행 시간 증가, 반면 병렬 처리 및 파이프라이닝으로 CPU에서는 거의 동일한 실행 시간 유지

## 4. 매개변수 선택

- Argon2d
  - 공격자가 시스템 메모리·CPU에 정기적으로 접근하지 못하는 설정에 최적화
  - 즉, 타이밍 정보 기반 사이드 채널 공격 불가 + 가비지 컬렉션으로 패스워드를 훨씬 빠르게 복구 불가한 설정
  - 백엔드 서버·암호화폐 채굴에 더 일반적
  - 실제 사용 제안: 1코어 2 GHz CPU에서 0.1초 소요 암호화폐 채굴 -- 2개 레인, 250 MB RAM, Argon2d

- Argon2id
  - 공격자가 잠재적으로 같은 머신에 접근·CPU 사용·콜드 부트 공격 가능한 더 현실적인 설정에 최적화
  - 제안 설정
    - 4코어 2 GHz CPU에서 0.5초 소요 백엔드 서버 인증 -- 8개 레인, 4 GiB RAM, Argon2id
    - 2코어 2 GHz CPU에서 3초 소요 하드 드라이브 암호화용 키 유도 -- 4개 레인, 6 GiB RAM, Argon2id
    - 2코어 2 GHz CPU에서 0.5초 소요 프론트엔드 서버 인증 -- 4개 레인, 1 GiB RAM, Argon2id

Argon2 실제 사용을 위한 타입·매개변수 선택 절차:

1. 애플리케이션·하드웨어에 맞춤화되지 않은 균일하게 안전한 옵션이 허용되는 경우 → t=1 반복, p=4 레인, m=2^(21)(2 GiB RAM), 128비트 솔트, 256비트 태그 크기의 Argon2id 선택 (첫 번째 권장 옵션)
2. 훨씬 적은 메모리만 사용 가능한 경우 → t=3 반복, p=4 레인, m=2^(16)(64 MiB RAM), 128비트 솔트, 256비트 태그 크기의 Argon2id 선택 (두 번째 권장 옵션)
3. 그 외의 경우 → 타입 y 선택부터 시작
   - 타입 간 차이를 모르거나 사이드 채널 공격을 실행 가능한 위협으로 고려하는 경우 → Argon2id 선택
4. p=4 레인 선택
5. 각 호출이 감당할 수 있는 최대 메모리 양 파악 → 매개변수 m으로 변환
6. 각 호출이 감당할 수 있는 최대 시간(초) 파악
7. 솔트 길이 선택
   - 128비트: 모든 애플리케이션에 충분
   - 공간 제약 시 64비트로 축소 가능
8. 태그 길이 선택
   - 128비트: 키 유도 포함 대부분 애플리케이션에 충분
   - 더 긴 키가 필요하면 더 긴 태그 선택
9. 사이드 채널 공격이 실행 가능한 위협이거나 불확실한 경우 → 라이브러리 호출에서 메모리 와이핑 옵션 활성화
10. 타입 y, 메모리 m, p 레인의 스킴을 다른 패스 수 t로 실행
    - 실행 시간이 감당 가능한 시간을 초과하지 않는 최대 t 파악
    - t = 1에서도 초과하는 경우 → m을 그에 따라 축소
11. 결정된 값 m, p, t로 Argon2 사용

## 5. 테스트 벡터

이 섹션은 Argon2를 위한 테스트 벡터 포함.

### 5.1. Argon2d 테스트 벡터

- 완전한 출력(태그)과 함께 테스트 벡터 제공
- 개발자 편의를 위해 일부 중간 변수도 제공 (각 패스의 첫 번째와 마지막 메모리 블록)

```
=======================================
Argon2d version number 19
=======================================
Memory: 32 KiB
Passes: 3
Parallelism: 4 lanes
Tag length: 32 bytes
Password[32]: 01 01 01 01 01 01 01 01
              01 01 01 01 01 01 01 01
              01 01 01 01 01 01 01 01
              01 01 01 01 01 01 01 01
Salt[16]: 02 02 02 02 02 02 02 02 02 02 02 02 02 02 02 02
Secret[8]: 03 03 03 03 03 03 03 03
Associated data[12]: 04 04 04 04 04 04 04 04 04 04 04 04
Pre-hashing digest: b8 81 97 91 a0 35 96 60
                    bb 77 09 c8 5f a4 8f 04
                    d5 d8 2c 05 c5 f2 15 cc
                    db 88 54 91 71 7c f7 57
                    08 2c 28 b9 51 be 38 14
                    10 b5 fc 2e b7 27 40 33
                    b9 fd c7 ae 67 2b ca ac
                    5d 17 90 97 a4 af 31 09

 After pass 0:
Block 0000 [  0]: db2fea6b2c6f5c8a
Block 0000 [  1]: 719413be00f82634
Block 0000 [  2]: a1e3f6dd42aa25cc
Block 0000 [  3]: 3ea8efd4d55ac0d1
...
Block 0031 [124]: 28d17914aea9734c
Block 0031 [125]: 6a4622176522e398
Block 0031 [126]: 951aa08aeecb2c05
Block 0031 [127]: 6a6c49d2cb75d5b6

 After pass 1:
Block 0000 [  0]: d3801200410f8c0d
Block 0000 [  1]: 0bf9e8a6e442ba6d
Block 0000 [  2]: e2ca92fe9c541fcc
Block 0000 [  3]: 6269fe6db177a388
...
Block 0031 [124]: 9eacfcfbdb3ce0fc
Block 0031 [125]: 07dedaeb0aee71ac
Block 0031 [126]: 074435fad91548f4
Block 0031 [127]: 2dbfff23f31b5883

 After pass 2:
Block 0000 [  0]: 5f047b575c5ff4d2
Block 0000 [  1]: f06985dbf11c91a8
Block 0000 [  2]: 89efb2759f9a8964
Block 0000 [  3]: 7486a73f62f9b142
...
Block 0031 [124]: 57cfb9d20479da49
Block 0031 [125]: 4099654bc6607f69
Block 0031 [126]: f142a1126075a5c8
Block 0031 [127]: c341b3ca45c10da5
Tag: 51 2b 39 1b 6f 11 62 97
     53 71 d3 09 19 73 42 94
     f8 68 e3 be 39 84 f3 c1
     a1 3a 4d b9 fa be 4a cb
```

### 5.2. Argon2i 테스트 벡터

```
=======================================
Argon2i version number 19
=======================================
Memory: 32 KiB
Passes: 3
Parallelism: 4 lanes
Tag length: 32 bytes
Password[32]: 01 01 01 01 01 01 01 01
              01 01 01 01 01 01 01 01
              01 01 01 01 01 01 01 01
              01 01 01 01 01 01 01 01
Salt[16]: 02 02 02 02 02 02 02 02 02 02 02 02 02 02 02 02
Secret[8]: 03 03 03 03 03 03 03 03
Associated data[12]: 04 04 04 04 04 04 04 04 04 04 04 04
Pre-hashing digest: c4 60 65 81 52 76 a0 b3
                    e7 31 73 1c 90 2f 1f d8
                    0c f7 76 90 7f bb 7b 6a
                    5c a7 2e 7b 56 01 1f ee
                    ca 44 6c 86 dd 75 b9 46
                    9a 5e 68 79 de c4 b7 2d
                    08 63 fb 93 9b 98 2e 5f
                    39 7c c7 d1 64 fd da a9

 After pass 0:
Block 0000 [  0]: f8f9e84545db08f6
Block 0000 [  1]: 9b073a5c87aa2d97
Block 0000 [  2]: d1e868d75ca8d8e4
Block 0000 [  3]: 349634174e1aebcc
...
Block 0031 [124]: 975f596583745e30
Block 0031 [125]: e349bdd7edeb3092
Block 0031 [126]: b751a689b7a83659
Block 0031 [127]: c570f2ab2a86cf00

 After pass 1:
Block 0000 [  0]: b2e4ddfcf76dc85a
Block 0000 [  1]: 4ffd0626c89a2327
Block 0000 [  2]: 4af1440fff212980
Block 0000 [  3]: 1e77299c7408505b
...
Block 0031 [124]: e4274fd675d1e1d6
Block 0031 [125]: 903fffb7c4a14c98
Block 0031 [126]: 7e5db55def471966
Block 0031 [127]: 421b3c6e9555b79d

 After pass 2:
Block 0000 [  0]: af2a8bd8482c2f11
Block 0000 [  1]: 785442294fa55e6d
Block 0000 [  2]: 9256a768529a7f96
Block 0000 [  3]: 25a1c1f5bb953766
...
Block 0031 [124]: 68cf72fccc7112b9
Block 0031 [125]: 91e8c6f8bb0ad70d
Block 0031 [126]: 4f59c8bd65cbb765
Block 0031 [127]: 71e436f035f30ed0
Tag: c8 14 d9 d1 dc 7f 37 aa
     13 f0 d7 7f 24 94 bd a1
     c8 de 6b 01 6d d3 88 d2
     99 52 a4 c4 67 2b 6c e8
```

### 5.3. Argon2id 테스트 벡터

```
=======================================
Argon2id version number 19
=======================================
Memory: 32 KiB, Passes: 3,
Parallelism: 4 lanes, Tag length: 32 bytes
Password[32]: 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01
01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01
Salt[16]: 02 02 02 02 02 02 02 02 02 02 02 02 02 02 02 02
Secret[8]: 03 03 03 03 03 03 03 03
Associated data[12]: 04 04 04 04 04 04 04 04 04 04 04 04
Pre-hashing digest: 28 89 de 48 7e b4 2a e5 00 c0 00 7e d9 25 2f
 10 69 ea de c4 0d 57 65 b4 85 de 6d c2 43 7a 67 b8 54 6a 2f 0a
 cc 1a 08 82 db 8f cf 74 71 4b 47 2e 94 df 42 1a 5d a1 11 2f fa
 11 43 43 70 a1 e9 97

 After pass 0:
Block 0000 [  0]: 6b2e09f10671bd43
Block 0000 [  1]: f69f5c27918a21be
Block 0000 [  2]: dea7810ea41290e1
Block 0000 [  3]: 6787f7171870f893
...
Block 0031 [124]: 377fa81666dc7f2b
Block 0031 [125]: 50e586398a9c39c8
Block 0031 [126]: 6f732732a550924a
Block 0031 [127]: 81f88b28683ea8e5

 After pass 1:
Block 0000 [  0]: 3653ec9d01583df9
Block 0000 [  1]: 69ef53a72d1e1fd3
Block 0000 [  2]: 35635631744ab54f
Block 0000 [  3]: 599512e96a37ab6e
...
Block 0031 [124]: 4d4b435cea35caa6
Block 0031 [125]: c582210d99ad1359
Block 0031 [126]: d087971b36fd6d77
Block 0031 [127]: a55222a93754c692

 After pass 2:
Block 0000 [  0]: 942363968ce597a4
Block 0000 [  1]: a22448c0bdad5760
Block 0000 [  2]: a5f80662b6fa8748
Block 0000 [  3]: a0f9b9ce392f719f
...
Block 0031 [124]: d723359b485f509b
Block 0031 [125]: cb78824f42375111
Block 0031 [126]: 35bc8cc6e83b1875
Block 0031 [127]: 0b012846a40f346a
Tag: 0d 64 0d f5 8d 78 76 6c 08 c0 37 a3 4a 8b 53 c9 d0
 1e f0 45 2d 75 b6 5e b5 25 20 e9 6b 01 e6 59
```

## 6. IANA 고려사항

이 문서는 IANA 조치 없음.

## 7. 보안 고려사항

### 7.1. 해시 함수 및 KDF로서의 보안

- Argon2의 충돌 저항성 및 역상 저항성 수준은 기반이 되는 BLAKE2b 해시 함수와 동등
  - 충돌 생성에 2^(256)개 입력 필요
  - 역상 발견에 2^(512)개 입력 시도 필요

- KDF 보안은 키 길이와 해시 함수 H'의 내부 상태 크기로 결정
  - 키가 있는 Argon2의 출력을 무작위와 구별하려면 최소 (2^(128),2^length(K)) 번의 BLAKE2b 호출 필요

### 7.2. 시간-공간 트레이드오프 공격에 대한 보안

- 시간-공간 트레이드오프: 내부 압축 함수 호출 비용을 더 들이는 대신 메모리 블록을 적게 저장하며 메모리 하드 함수 계산 가능
- 트레이드오프 공격의 이점은 시간-면적 곱에 대한 감소 계수로 측정
  - 메모리와 추가 압축 함수 코어 → 면적에 기여
  - 누락된 블록 재계산 수용 → 시간 증가
  - 높은 감소 계수 → 역상 검색 가속화 가능

- 1패스·2패스 Argon2i에 대한 최선의 공격: [CBS16]에 설명된 저장소 공격, 시간-면적 곱(피크 메모리 값 사용)을 5배 감소
- 3패스 이상 Argon2i에 대한 최선의 공격: [AB16], 감소 계수는 메모리 크기와 패스 수의 함수
  - 예: 1 기비바이트 메모리 기준 → 3패스 감소 계수 3, 4패스 2.5, 6패스 2
  - 감소 계수는 메모리 크기가 두 배 될 때마다 약 0.5씩 증가
- [AB16]의 시간-공간 트레이드오프를 완전히 방지하려면 → 패스 수는 반드시(MUST) 메모리의 이진 로그에서 26을 뺀 값을 초과해야 함
- 점근적으로 1패스 Argon2i에 대한 최선의 공격은 [BZ17]
  - 공격자 최대 이점은 O(m^(0.233))으로 상한 (m은 블록 수)
  - [BZ17]이 어떤 공격에도 상한이 O(m^(0.25))임을 증명 → 이 공격은 점근적으로 최적

- t패스 Argon2d에 대한 최선의 트레이드오프 공격: 랭킹 트레이드오프 공격, 시간-면적 곱을 1.33배 감소

- Argon2id에 대한 최선의 공격: 1패스 Argon2i에 대한 최선의 공격과 다중 패스 Argon2d에 대한 최선의 공격을 보완하여 획득
  - 1패스 Argon2id: 저장소 공격(메모리 전반부) + 랭킹 공격(후반부) 결합, 약 2.1의 계수 생성
  - t패스 Argon2id: 랭킹 트레이드오프 공격, 시간-면적 곱을 1.33배 감소

### 7.3. 시간 제한 방어자를 위한 보안

- 패스워드 해싱 함수 사용 시스템의 병목 현상은 종종 메모리 비용보다 함수 지연 시간
- 합리적인 방어자는 해시·솔트·방어자 머신의 고정된 계산 시간에 대한 타이밍 정보 목록을 갖춘 공격자에 대한 무차별 대입 비용을 최대화
- [AB16]의 공격 비용 추정 결과
  - Argon2i: 3패스가 대부분의 합리적인 메모리 크기에 대해 거의 최적
  - Argon2d, Argon2id: 1패스가 일정한 방어자 시간에 대한 공격 비용을 최대화

### 7.4. 권장사항

- t=1, 2 GiB 메모리 Argon2id 변형 → 첫 번째 권장 옵션, 모든 환경의 기본 설정으로 제안
  - 사이드 채널 공격에 안전 + 전용 무차별 대입 하드웨어에서 공격자 비용 최대화
- t=3, 64 MiB 메모리 Argon2id 변형 → 두 번째 권장 옵션, 메모리 제약 환경의 기본 설정으로 제안

## 8. 참고문헌

### 8.1. 규범적 참고문헌

```
[BLAKE2]   Saarinen, M-J., Ed. and J-P. Aumasson, "The BLAKE2
           Cryptographic Hash and Message Authentication Code (MAC)",
           RFC 7693, DOI 10.17487/RFC7693, November 2015,
           <https://www.rfc-editor.org/info/rfc7693>.

[RFC2119]  Bradner, S., "Key words for use in RFCs to Indicate
           Requirement Levels", BCP 14, RFC 2119,
           DOI 10.17487/RFC2119, March 1997,
           <https://www.rfc-editor.org/info/rfc2119>.

[RFC8174]  Leiba, B., "Ambiguity of Uppercase vs Lowercase in RFC
           2119 Key Words", BCP 14, RFC 8174, DOI 10.17487/RFC8174,
           May 2017, <https://www.rfc-editor.org/info/rfc8174>.
```

### 8.2. 정보적 참고문헌

```
[AB15]     Biryukov, A. and D. Khovratovich, "Tradeoff Cryptanalysis
           of Memory-Hard Functions", ASIACRYPT 2015,
           DOI 10.1007/978-3-662-48800-3_26, December 2015,
           <https://eprint.iacr.org/2015/227.pdf>.

[AB16]     Alwen, J. and J. Blocki, "Efficiently Computing Data-
           Independent Memory-Hard Functions", CRYPTO 2016,
           DOI 10.1007/978-3-662-53008-5_9, March 2016,
           <https://eprint.iacr.org/2016/115.pdf>.

[ARGON2]   Biryukov, A., Dinu, D., and D. Khovratovich, "Argon2: the
           memory-hard function for password hashing and other
           applications", March 2017,
           <https://www.cryptolux.org/images/0/0d/Argon2.pdf>.

[ARGON2ESP]
           Biryukov, A., Dinu, D., and D. Khovratovich, "Argon2: New
           Generation of Memory-Hard Functions for Password Hashing
           and Other Applications", Euro SnP 2016,
           DOI 10.1109/EuroSP.2016.31, March 2016,
           <https://www.cryptolux.org/images/d/d0/Argon2ESP.pdf>.

[BZ17]     Blocki, J. and S. Zhou, "On the Depth-Robustness and
           Cumulative Pebbling Cost of Argon2i", TCC 2017,
           DOI 10.1007/978-3-319-70500-2_15, May 2017,
           <https://eprint.iacr.org/2017/442.pdf>.

[CBS16]    Boneh, D., Corrigan-Gibbs, H., and S. Schechter, "Balloon
           Hashing: A Memory-Hard Function Providing Provable
           Protection Against Sequential Attacks", ASIACRYPT 2016,
           DOI 10.1007/978-3-662-53887-6_8, May 2017,
           <https://eprint.iacr.org/2016/027.pdf>.

[HARD]     Alwen, J. and V. Serbinenko, "High Parallel Complexity
           Graphs and Memory-Hard Functions", STOC '15,
           DOI 10.1145/2746539.2746622, June 2015,
           <https://eprint.iacr.org/2014/238.pdf>.
```

## 감사의 글

- 이 문서를 준비하고 검토하는 데 도움을 준 다음 분들께 감사: Jean-Philippe Aumasson, Samuel Neves, Joel Alwen, Jeremiah Blocki, Bill Cox, Arnold Reinhold, Solar Designer, Russ Housley, Stanislav Smyshlyaev, Kenny Paterson, Alexey Melnikov, Gwynne Raskind
- 이 문서에 설명된 작업은 Daniel Dinu가 Intel 합류 전, 룩셈부르크 대학교 재직 시 수행

## 저자 주소

```
Alex Biryukov
University of Luxembourg

Email: alex.biryukov@uni.lu


Daniel Dinu
University of Luxembourg

Email: daniel.dinu@intel.com


Dmitry Khovratovich
ABDK Consulting

Email: khovratovich@gmail.com


Simon Josefsson
SJD AB

Email: simon@josefsson.org
URI:   http://josefsson.org/
```

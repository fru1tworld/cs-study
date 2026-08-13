# HOTP와 TOTP

## RFC 4226 - HOTP: HMAC 기반 일회용 비밀번호 알고리즘

> 원제: HOTP: An HMAC-Based One-Time Password Algorithm
> 저자: D. M'Raihi(VeriSign), M. Bellare(UCSD), F. Hoornaert(Vasco), D. Naccache(Gemplus), O. Ranen(Aladdin)
> 분류: Informational
> 발행일: 2005년 12월
> 배경: OATH(Open AuTHentication) 회원들의 공동 작업, 상용·오픈소스 구현 간 상호운용성을 목표로 함

### Abstract

- HMAC(Hashed Message Authentication Code)에 기반한 일회용 비밀번호(OTP) 생성 알고리즘 정의
- 보안 분석과 안전한 배포를 위한 주요 매개변수 논의 포함
- 적용 범위: 원격 VPN 접속·Wi-Fi 로그온부터 트랜잭션 지향 웹 애플리케이션까지 광범위한 네트워크 애플리케이션

### 1. 개요

- HMAC[BCK1] 기반 OTP 알고리즘 → HMAC-Based One-Time Password(HOTP) 알고리즘으로 명명
- 문서 구성: 4절 알고리즘 요구사항 → 5절 HOTP 알고리즘 설명 → 6·7절 보안 → 8절 확장·개선 → 10절 결론
- 부록 A: 알고리즘 보안의 상세 분석(이상화된 버전 평가 → HOTP 자체 보안 분석)

### 2. 서론

- 이중 인증(two-factor authentication) 배포는 여전히 규모·범위 면에서 제한적
- 원인
  - 위협 수준 상승에도 대부분 인터넷 애플리케이션이 취약한 정적 비밀번호에 의존
  - 하드웨어·소프트웨어 공급업체 간 상호운용성 부재 → 독점 기술로 결합된 고비용·저채택 솔루션 초래
  - 공개 명세 부재가 근본 원인
- 정적 비밀번호는 주요 인증 수단으로 부적절함이 드러남 → 단일 기능의 고가 인증 기기 휴대는 대안이 아님 → 유연한 기기에 내장 가능한 이중 인증 필요
- 광범위한 상호운용성을 위해서는 기반 기술이 하드웨어·소프트웨어 개발자 커뮤니티에 자유롭게 공개되어야 함 → USB 대용량 저장 기기·IP 전화기·PDA 같은 차세대 소비자 기기 내장 전제조건
- OTP는 이중 인증 중 가장 단순하고 널리 쓰이는 형태 → 대규모 기업 VPN 접속에서 흔히 요구
- OTP가 PKI·생체 인증보다 선호되는 이유: 에어갭 기기 → 클라이언트 데스크톱 소프트웨어 설치 불필요 → 가정용 PC·키오스크·PDA 등 여러 기기를 넘나들며 사용 가능
- 본 문서는 이벤트 기반 알고리즘 제안 → Java 스마트카드·USB 동글·GSM SIM 카드 같은 대량 생산 기기 내장 가능
- 알고리즘은 IETF 지식재산권[RFC3979] 조건에 따라 개발자 커뮤니티에 자유롭게 제공
- 저자들은 OATH(2004년 설립) 회원

### 3. 요구사항 용어

- 핵심 단어 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", "OPTIONAL"은 [RFC2119] 정의를 따름

### 4. 알고리즘 요구사항

- 설계 방향: 사용성뿐 아니라 최소한의 UI만 가능한 저비용 하드웨어 구현 가능성 강조 → 대량 생산 SIM·Java 카드 내장이 기본 전제조건
- R1(MUST): 알고리즘은 시퀀스 기반 또는 카운터 기반 → Java 스마트카드·USB 동글·GSM SIM 카드 같은 대량 생산 기기 내장 목표
- R2(SHOULD): 배터리·버튼 수·연산 능력·LCD 크기 요구사항 최소화 → 하드웨어 경제적 구현 가능
- R3(MUST/MAY): 숫자 입력이 전혀 없는 토큰에서도 동작해야 함(MUST) — 단 안전한 PIN 패드가 있는 정교한 기기에서도 사용 가능(MAY)
- R4(MUST): 토큰에 표시되는 값은 사용자가 쉽게 읽고 입력 가능해야 함 → HOTP 값은 합리적인 길이 필요
  - 최소 6자리 값, 전화기 등 제한된 기기에서 입력 쉽도록 '숫자만'으로 구성 권장
- R5(MUST): 카운터 재동기화를 위한 사용자 친화적 메커니즘 필요 → 7.4절, 부록 E.4에서 상세 설명
- R6(MUST/RECOMMEND): 강력한 공유 비밀 사용 필요
  - 최소 128비트(MUST)
  - 160비트 권장(RECOMMEND)

### 5. HOTP 알고리즘

#### 5.1 표기법과 기호

- 문자열(string): 항상 이진 문자열, 즉 0과 1의 시퀀스
- `|s|`: 문자열 s의 길이
- `|n|`: 숫자 n의 절댓값
- `s[i]`: 문자열 s의 i번째 비트(0부터 시작) → s = s[0]s[1]...s[n-1], n = |s|
- StToNum(String to Number): 입력 문자열 s의 이진 표현에 대응하는 숫자를 반환하는 함수 (예: StToNum(110) = 6)
- 기호 정의
  - C: 8바이트 카운터 값(이동 인자, moving factor). HOTP 생성기(클라이언트)와 검증기(서버) 간 동기화 필요(MUST)
  - K: 클라이언트-서버 간 공유 비밀. 각 HOTP 생성기는 서로 다른 고유 비밀 K를 가짐
  - T: 스로틀링 매개변수. 서버는 T번의 인증 실패 후 연결 거부
  - s: 재동기화 매개변수. 서버는 연속된 s개의 카운터 값에 걸쳐 인증자 검증 시도
  - Digit: HOTP 값의 자릿수(시스템 매개변수)

#### 5.2 설명

- HOTP는 증가하는 카운터 값 + 토큰과 검증 서비스만 아는 정적 대칭키에 기반
- HOTP 값 생성에 RFC 2104[BCK2]의 HMAC-SHA-1 알고리즘 사용
- HMAC-SHA-1 출력은 160비트 → 사용자가 쉽게 입력할 수 있는 형태로 절단 필요

```
HOTP(K,C) = Truncate(HMAC-SHA-1(K,C))
```

- Truncate: 5.3절에 정의된, HMAC-SHA-1 값을 HOTP 값으로 변환하는 함수
- 키(K)·카운터(C)·데이터 값은 상위 바이트(high-order byte)부터 해시
- HOTP 생성기가 만든 HOTP 값은 빅 엔디언으로 취급

#### 5.3 HOTP 값 생성

- 3단계로 구성

```
1단계: HMAC-SHA-1 값 생성
  HS = HMAC-SHA-1(K,C)          // HS는 20바이트 문자열

2단계: 4바이트 문자열 생성(동적 절단, Dynamic Truncation)
  Sbits = DT(HS)                // DT는 31비트 문자열 반환

3단계: HOTP 값 계산
  Snum = StToNum(Sbits)         // 0...2^31-1 범위의 숫자로 변환
  Return D = Snum mod 10^Digit  // D는 0...10^Digit-1 범위
```

- Truncate 함수 = 2단계(동적 절단) + 3단계(10^Digit 모듈로 축약)
- 동적 오프셋 절단 목적: 160비트(20바이트) HMAC-SHA-1 결과 → 4바이트 동적 이진 코드 추출

```
DT(String)                          // String = String[0]...String[19]
  OffsetBits = String[19]의 하위 4비트
  Offset = StToNum(OffsetBits)      // 0 <= Offset <= 15
  P = String[Offset]...String[Offset+3]
  P의 마지막 31비트를 반환
```

- P의 최상위 비트 마스킹 이유: 부호 있는/없는 모듈로 연산 간 혼란 방지 → 프로세서마다 연산 방식이 달라 부호 비트 마스킹으로 모호성 제거
- 구현은 최소 6자리 코드 추출 필요(MUST), 7·8자리 코드도 가능
- 보안 요구사항에 따라 Digit = 7 이상 고려 권장(SHOULD)

#### 5.4 Digit = 6일 때의 HOTP 계산 예시

- hmac_result(HMAC-SHA-1 결과 바이트 배열)로부터 동적 이진 코드 추출:

```
int offset   =  hmac_result[19] & 0xf ;
int bin_code = (hmac_result[offset]  & 0x7f) << 24
   | (hmac_result[offset+1] & 0xff) << 16
   | (hmac_result[offset+2] & 0xff) <<  8
   | (hmac_result[offset+3] & 0xff) ;
```

- SHA-1 HMAC 바이트 예시:

```
바이트 번호: 00|01|02|03|04|05|06|07|08|09|10|11|12|13|14|15|16|17|18|19
바이트 값  : 1f|86|98|69|0e|02|ca|16|61|85|50|ef|7f|19|da|8e|94|5b|55|5a
                                        ^^^^^^^^^^^^^^^^ (오프셋 10부터 4바이트)
```

- 단계별 값
  - 마지막 바이트(바이트 19): 0x5a
  - 하위 4비트 = 0xa → 오프셋 값
  - 오프셋 값이 가리키는 바이트: 바이트 10(0xa)
  - 바이트 10부터 4바이트 값: 0x50ef7f19 = 동적 이진 코드 DBC1
  - DBC1의 MSB는 0x50 → DBC2 = DBC1 = 0x50ef7f19
  - HOTP = DBC2 mod 10^6 = 872921
- 동적 이진 코드는 31비트 부호 없는 빅 엔디언 정수로 취급 → 첫 바이트는 0x7f로 마스킹
- 이 숫자를 1,000,000(10^6)으로 모듈로 연산 → 6자리 HOTP 값 872921

### 6. 보안 고려사항

- 부록 A 분석 결론: 서로 다른 카운터 입력에 대한 DT 출력은 균일·독립적으로 분포된 31비트 문자열로 간주 가능
- 문자열→정수 변환 및 10^Digit으로의 모듈로 축약이 도입하는 편향은 무시 가능 수준 → HOTP에 대한 가능한 최선의 공격은 무차별 대입 공격
- 공격자가 다수의 프로토콜 교환을 관찰해 인증 값 시퀀스를 수집하더라도, 이를 이용해 HOTP 생성 함수 F를 학습해도 무작위 추측 대비 유의미한 이점 없음
- 결론: 가능한 모든 값을 열거·시도하는 무차별 대입 공격이 최선의 전략

```
Sec = sv/10^Digit
```

- Sec: 공격자의 성공 확률
- s: 룩어헤드(look-ahead) 동기화 윈도우 크기
- v: 검증 시도 횟수
- Digit: HOTP 값의 자릿수
- 사용성을 유지하면서 원하는 보안 수준에 도달하도록 s, T(시도 횟수 제한 스로틀링 매개변수), Digit를 조정 가능

### 7. 보안 요구사항

- OTP 알고리즘의 안전성은 이를 구현하는 애플리케이션·인증 프로토콜 수준에 의해 좌우됨
- 매개변수 T, s는 보안에 상당한 영향 → 6절에서 상세 설명
- HOTP는 암호화의 대체물이 아니며 데이터 전송의 프라이버시를 제공하지 않음 → 기밀성·프라이버시 보호는 별도 메커니즘 필요

#### 7.1 인증 프로토콜 요구사항

- 프로토콜 P가 증명자(prover)-검증자(verifier) 간 HOTP 인증을 구현할 때의 요구사항
- RP1(MUST): 이중 인증 지원 필요 — 당신이 아는 것(비밀번호·패스프레이즈·PIN)과 당신이 가진 것(토큰)의 통신·검증 모두 지원. 비밀 코드는 사용자만 알고 보통 OTP 값과 함께 입력(이중 인증)
- RP2(SHOULD NOT): 무차별 대입 공격에 취약하지 않아야 함 → 검증 서버 측 스로틀링/잠금(lockout) 권장(RECOMMENDED)
- RP3(SHOULD): 사용자 프라이버시 보호·재전송(replay) 공격 방지를 위해 안전한 채널을 통해 구현

#### 7.2 HOTP 값의 검증

- HOTP 클라이언트(하드웨어/소프트웨어 토큰)는 카운터를 증가시킨 뒤 다음 HOTP 값 계산
- 서버가 수신한 값이 클라이언트 계산값과 일치 → HOTP 값 검증 성공 → 서버 카운터 값 1 증가
- 일치하지 않으면 → 서버는 재동기화 프로토콜(룩어헤드 윈도우) 개시 후 재시도 요청
- 재동기화 실패 시 → 인가된 최대 시도 횟수까지 재인증 패스 반복 요청
- 최대 시도 횟수 도달 시 → 서버는 계정을 잠그고 사용자에게 알리는 절차 개시 필요(SHOULD)

#### 7.3 서버에서의 스로틀링

- HMAC-SHA-1 값을 짧게 절단 → 무차별 대입 공격 가능 → 인증 서버의 탐지·차단 필요
- 스로틀링 매개변수 T(검증 시도 최대 횟수) 설정 권장(RECOMMEND) — 검증 서버는 HOTP 기기별로 실패 시도 카운터 관리
- 서버의 재동기화 방법이 윈도우 기반이고 윈도우가 큰 경우 T를 너무 크게 하지 않을 것 권장(RECOMMEND)
- T는 사용성에 유의미한 영향이 없는 한도 내에서 가능한 낮게 설정(SHOULD)
- 대안: 지연(delay) 방식 — 각 실패 시도 A 이후 T*A초 대기 (예: T=5일 때 1회 실패 후 5초, 2회 실패 후 10초 대기)
- 지연/잠금 방식은 다중 병렬 추측 공격 방지를 위해 로그인 세션 전반에 적용 필요(MUST)

#### 7.4 카운터의 재동기화

- 서버 카운터는 성공적 HOTP 인증 후에만 증가하지만, 토큰 카운터는 사용자가 새 HOTP를 요청할 때마다 증가 → 양측 카운터가 어긋날 수 있음
- 서버에 룩어헤드 매개변수 s 설정 권장(RECOMMEND) — 룩어헤드 윈도우 크기 정의, 서버는 다음 s개의 HOTP-서버 값을 재계산해 수신값과 대조
- 선택적으로 재동기화 목적의 HOTP 값 시퀀스(예: 2~3개) 입력을 요구 가능(MAY) — 연속 시퀀스 위조는 단일 값 추측보다 훨씬 어려움
- s의 상한 설정 효과
  - 서버가 무한정 HOTP 값을 검사하는 서비스 거부 공격 방지
  - 공격자에게 가능한 해(solution) 공간 제한
- s는 사용성에 영향 없는 한도 내에서 가능한 낮게 설정(SHOULD)

#### 7.5 공유 비밀의 관리

- 공유 비밀을 다루는 연산은 민감 정보 유출 위험 완화를 위해 안전하게 수행 필요
- 검증 시스템에서 공유 비밀을 (안전하게) 생성·저장하는 두 방법:
  - 결정론적 생성(Deterministic Generation): 마스터 시드로부터 프로비저닝·검증 단계 모두에서 필요할 때마다 즉석 도출
  - 무작위 생성(Random Generation): 프로비저닝 단계에서 무작위 생성 → 즉시 저장, 생애 주기 동안 안전 유지 필요

##### 결정론적 생성

- 마스터 비밀(서버에만 저장)로부터 공유 비밀 도출
- 마스터 키 저장 및 공유 비밀 도출에 변조 방지(tamper-resistant) 기기 사용 필요(MUST)
- 이점: 공유 비밀이 노출되는 시점이 없음 + 필요 시 온디맨드 생성 → 별도 저장 요구사항 회피
- 두 가지 방식
  - 단일 마스터 키 MK 사용: K_i = SHA-1(MK, i) — i는 일련번호·토큰 ID 등 기기를 고유 식별하는 공개 정보. 애플리케이션·서비스 제공자별로 다른 비밀·설정 구성
  - 다중 마스터 키 MK_i 사용: 각 기기가 {K_i,j = SHA-1(MK_i,j)} 저장 — j는 기기 식별 공개 정보. HSM에 활성 마스터 키만 저장 + [Shamir] 방식으로 비밀 공유(secret sharing) → 마스터 비밀 MK_i 손상 시 전체 기기 교체 없이 다른 비밀로 전환 가능
- 단점: 마스터 비밀 노출 시 공격자가 공개 정보 기반으로 모든 공유 비밀 재구성 가능 → 전체 비밀 폐기(revocation) 또는(다중 마스터 키의 경우) 새 비밀 집합 전환 필요
- 마스터 키 저장·공유 비밀 생성 기기는 변조 방지여야 함(MUST) — HSM은 검증 시스템 보안 경계 밖으로 노출되지 않아 유출 위험 감소

##### 무작위 생성

- 공유 비밀은 무작위로 생성
- [RFC4086] 권고에 따라 좋고 안전한 무작위 소스 선택 권장(RECOMMEND)
- 실질적 방법 두 가지
  - 하드웨어 기반 생성기: 물리적 현상의 무작위성 이용 (예: 발진기 기반, 능동 공격이 더 어려운 설계 가능)
  - 소프트웨어 기반 생성기: 좋은 설계는 쉽지 않음 → 단순하지만 효율적인 구현은 다양한 소스에 기반 + 샘플링된 시퀀스에 SHA-1 같은 일방향 함수 적용 필요
- 하드웨어든 소프트웨어든 검증된 제품 선택 권장(RECOMMEND)
- 공유 비밀 저장 시 변조 방지 하드웨어 암호화 사용, 필요할 때만 노출 권장(RECOMMEND) — 예: 검증 시 복호화 후 RAM 노출 시간을 줄이기 위해 즉시 재암호화
- 공유 비밀 데이터 저장소는 검증 시스템·비밀 데이터베이스에 대한 직접 공격을 피하기 위해 안전한 영역에 위치 필요(MUST)
- 공유 비밀 접근은 검증 시스템에 필요한 프로그램·프로세스로만 제한 필요 — 공유 비밀 보호는 가장 중요한 사항

### 8. 복합 공유 비밀

- 공유 비밀 K에 추가 인증 인자(authentication factor)를 포함시키는 것이 바람직할 수 있음
- 추가 인자 예시
  - 토큰에서 사용자 입력으로 얻은 PIN 또는 비밀번호
  - 전화번호
  - 토큰에서 프로그래밍적으로 이용 가능한 모든 고유 식별자
- 복합 공유 비밀 K는 프로비저닝 과정에서 무작위 시드 값 + 하나 이상의 추가 인증 인자를 결합해 구성
- 서버는 복합 비밀을 온디맨드로 구축하거나 저장 가능 — 어느 경우든 토큰은 시드 값만 저장(구현 선택에 따름)
- 토큰이 HOTP 계산 시 시드 값 + 로컬에서 도출/입력된 다른 인증 인자로부터 K 계산
- 효과: 추가 인증 인자 포함으로 HOTP 기반 인증 시스템 강화
- 부가 이점: 토큰이 신뢰할 수 있는 기기인 한, 인증 인자(예: 사용자가 입력한 PIN)를 다른 기기에 노출할 필요 없음

### 9. 양방향 인증

- HOTP 클라이언트는 검증 서버가 공유 비밀을 아는 진정한 주체임을 확인하는 데도 사용 가능
- 클라이언트-서버가 동기화되어 동일 비밀(또는 재계산 방법)을 공유 → 간단한 3패스 프로토콜 구성 가능
  1. 최종 사용자가 TokenID와 첫 번째 OTP 값 OTP1 입력
  2. 서버가 OTP1을 검사하고 올바르면 OTP2 회신
  3. 최종 사용자가 자신의 HOTP 기기로 OTP2를 검사하고 올바르면 웹사이트 사용
- 모든 OTP 통신은 SSL/TLS, IPsec 같은 안전한 채널을 통해 이루어져야 함

### 10. 결론

- HMAC 기반 OTP 알고리즘 HOTP 정의, 배포를 위한 선호 구현·운영 모드 권장
- 보안 요소 제시 → HOTP가 실용적·견고하며, 가능한 최선의 공격이 무차별 대입 공격이고 검증 서버 대응책으로 방지 가능함을 입증
- 특정 애플리케이션의 보안 향상을 위한 여러 개선 사항 제안(부록 E)

### 11. 참고문헌

#### 규범적 참고문헌

- [BCK1] Bellare, Canetti, Krawczyk, "Keyed Hash Functions and Message Authentication", Crypto'96
- [BCK2] Krawczyk, Bellare, Canetti, "HMAC: Keyed-Hashing for Message Authentication", RFC 2104, 1997
- [RFC2119] Bradner, "Key words for use in RFCs to Indicate Requirement Levels", BCP 14, 1997
- [RFC3979] Bradner, "Intellectual Property Rights in IETF Technology", BCP 79, 2005
- [RFC4086] Eastlake, Schiller, Crocker, "Randomness Requirements for Security", BCP 106, 2005

#### 정보성 참고문헌

- [OATH] Initiative for Open AuTHentication - http://www.openauthentication.org
- [PrOo] Preneel, van Oorschot, "MD-x MAC and building fast MACs from hash functions", CRYPTO '95
- [Shamir] Shamir, "How to Share a Secret", Communications of the ACM Vol. 22, No. 11, 1979

### 부록 A. HOTP 알고리즘 보안: 상세 분석

- 최선의 공격 전략 상세 분석 → 다양한 가정 하 보안·절단의 영향 분석 → 자릿수 관련 권고
- 분석 초점: Digit = 6(문서가 권장하는 최소 자릿수)

#### A.1 정의와 표기법

- `{0,1}^l`: 길이 l인 모든 문자열의 집합
- `Z_n = {0,..,n-1}`
- IntDiv(a,b): a >= b >= 1인 정수 a,b에 대해 정수 나눗셈 (q,r) 반환 → a = bq + r, 0 <= r < b
- H: {0,1}^k x {0,1}^c → {0,1}^n, k비트 키 K와 c비트 카운터 C를 받아 n비트 출력 H(K,C) 반환하는 기본 함수 (HOTP에서는 H = HMAC-SHA-1, 증명 일반화를 위해 형식적으로 정의)

#### A.2 이상화된 알고리즘: HOTP-IDEAL

- HOTP의 이상화된 대응물 정의 — H의 역할을 키를 형성하는 무작위 함수가 수행
- Maps(c,n): {0,1}^c → {0,1}^n으로 사상하는 모든 함수의 집합. 이상화된 알고리즘의 키 공간은 Maps(c,n) → "키"는 함수 h(무작위로 뽑힘)
- 이 키(함수) 자체가 저장하기에 너무 커서 실제 구현은 불가능 → 그래도 고려하는 이유: H가 널리 받아들여지는 가정을 만족하는 한, 실제 알고리즘과 이상화된 알고리즘의 보안이 실용적으로 동일함을 보이기 위함
- 이상화된 알고리즘 분석 → HMAC-SHA-1과 독립적으로 알고리즘 자체의 설계 품질 평가 가능

#### A.3 보안 모델

- ALG로 HOTP 또는 HOTP-IDEAL을 표기
- 시나리오: 사용자-서버가 ALG용 키 K 공유, 둘 다 초기값 0인 카운터 C 유지 → 사용자는 ALG(K,C)를 서버에 전송해 인증, 서버는 값이 맞으면 수락
- 카운터의 우발적 증가에 대비: 서버가 값 z를 수신하면, z = ALG(K,i) (C <= i <= C+s-1)인 i가 있으면 수락 → 수락 시 카운터를 i+1로 증가, 아니면 변경 없음
- 공격자 모델
  - 도청 가능: 사용자가 전송한 인증자를 볼 수 있음
  - 승리 조건: 사용자가 인증자를 전송한 적 없는 카운터 값에 대해 서버가 인증자를 수락하게 만들면 승리
  - 형식적 공격자 B: 사용 중인 ALG, 시스템 설계, 모든 매개변수를 사전에 알고 시작 — 모르는 것은 오직 공유 키 K
  - B는 이벤트 스케줄링을 완전히 통제 — 인증자 오라클(AuthO) 호출로 사용자 인증자를 획득, 검증 오라클(VerO) 호출로 후보 인증자 제출 가능. 서버가 수락하면 B 승리
- 게임 정의

```
초기화: 키 K를 K로부터 무작위 선택, 카운터 C=0, win=false

Oracle AuthO()
--------------
   A = ALG(K,C)
   C = C + 1
   Return A to B

Oracle VerO(A)
--------------
   i = C
   While (i <= C + s - 1 and Win == FALSE) do
      If A == ALG(K,i) then Win = TRUE; C = i + 1
      Else i = i + 1
   Return Win to B
```

- Adv(B): 위 게임에서 win이 true가 될 확률 = 공격자가 사용자를 사칭하는 데 성공할 확률
- 목표: 검증 질의 횟수 v, 인증자 오라클 질의 횟수 a, 실행 시간 t의 함수로서 Adv(B)의 상한 평가 → 스로틀(v의 상한) 설정 기준으로 활용

#### A.4 이상적 인증 알고리즘의 보안

- HOTP-IDEAL 보안 분석 요약 — 10^Digit 모듈로 변환의 영향에서 시작, 다양한 공격 검토

##### A.4.1 비트에서 자릿수로

- 무작위 n비트 문자열을 동적 오프셋 절단 → 무작위 31비트 문자열 산출
- 이를 m = 10^Digit로 모듈로 연산할 때의 분포 편향 추정

```
Lemma 1
N >= m >= 1을 정수, (q,r) = IntDiv(N,m)이라 하자.
Z_m의 z에 대해 P_{N,m}(z) = Pr[x mod m = z : x는 Z_N에서 무작위 선택]이라 하면

P_{N,m}(z) = (q+1)/N   (0 <= z < r)
           = q/N       (r <= z < m)
```

- 증명 개요: X가 Z_N 위에 균일 분포한다고 하면
  - P_{N,m}(z) = Pr[X mod m = z]
  - = Pr[X < mq]·Pr[X mod m = z | X < mq] + Pr[mq <= X < N]·Pr[X mod m = z | mq <= X < N]
  - = mq/N · 1/m + (N-mq)/N · 1/(N-mq) (0 <= z < N-mq 구간) → 정리하면 q/N + r/N·1/r
  - 정리하면 Lemma 1의 등식이 도출됨
- 적용: N = 2^31, d = 6, m = 10^d일 때 — x mod m을 취해 6자리로 축약하면 완전한 균일 분포가 아님
- 실제 분포
  - 값 0,1,...,483647: 확률 2148/2^31 ≈ 1.00024045/10^6
  - 값 483648,...,999999: 확률 2147/2^31 ≈ 0.99977478/10^6
- 첫 값 집합은 10^-6보다 약간 큰 확률, 나머지는 약간 작은 확률 → 분포가 약간 비균일하지만 편향은 작고 무시 가능(확률이 10^-6에 매우 근접)

##### A.4.2 무차별 대입 공격

- 인증자가 d개의 무작위 자릿수라면, v번 검증 시도의 무차별 대입 공격 성공률은 sv/10^Digit
- Lemma 1의 편향을 이용하면 공격자는 약간 더 나은 공격 가능 — 가장 가능성 높은 값(0,...,r-1 구간, (q,r)=IntDiv(2^31,10^Digit))으로만 시도
- 단순화를 위해 검증 질의 횟수를 최대 r로 가정 (N=2^31, m=10^6일 때 r=483,648 — 실제 스로틀 값은 이보다 훨씬 작으므로 큰 제약 아님)

```
Proposition 1
m = 10^Digit < 2^31, (q,r) = IntDiv(2^31,m), s <= m이라 하자.
무차별 대입 공격자 B-bf는 v <= r번의 검증 오라클 질의만으로(인증자 오라클 질의 없이) HOTP를 공격하며 성공률은

Adv(B-bf) = 1 - (1 - v(q+1)/2^31)^s
          ≈ sv(q+1)/2^31

m = 10^6일 때 q = 2,147이므로
Adv(B-bf) ≈ sv · 2148/2^31 = sv · 1.00024045/10^6
```

- 시사점: 재동기화 매개변수 s가 공격자 성공률에 비례하는 만큼 큰 영향 → 보안을 해치지 않으려면 s를 너무 크게 할 수 없음

##### A.4.3 무차별 대입 공격이 최선의 공격

- 핵심 질문: 무차별 대입보다 나은 공격이 있는가? — 사용자가 보낸 인증자를 수집해 암호 분석적으로 학습하는 시도가 도움이 되는가?
- 답: 아니오 — 공격자가 어떤 전략을 쓰든, 관찰하는 인증 횟수가 극단적으로 크지 않은 한 성공률은 무차별 대입 공격을 넘지 못함

```
Proposition 2
m = 10^Digit < 2^31, (q,r) = IntDiv(2^31,m)이라 하자.
B를 v번의 검증 오라클 질의와 a <= 2^c - s번의 인증자 오라클 질의로 HOTP-IDEAL을 공격하는
임의의 공격자라 하면

Adv(B) <= sv(q+1)/2^31

주의: 공격자가 사용자 인증을 2^c - s회 이상 보지 않는다는 조건에 따름 (c가 충분히 크면 실질적 제약 아님)

m = 10^6일 때 q = 2,147 →

Equation 1
sv · 2148/2^31 ≈ sv · 1.00024045/10^6
```

- 즉 B의 성공률은 무차별 대입 공격 이상이 아님 → 이것이 이 방식의 보안에 관한 핵심 정보

#### A.5 HOTP의 보안 분석

- 앞 절에서 이상화된 HOTP-IDEAL의 보안 분석 → 이제 H에 대한 적절한 가정 하에 실제 HOTP의 보안이 이상화된 대응물과 본질적으로 동일함을 보임
- 가정: H는 안전한 의사난수 함수(PRF) — 입력-출력이 무작위 함수와 실질적으로 구별 불가
- prf-이점(Adv(A)): 공격자 A가 오라클이 H(K,·)인 경우와 무작위 함수인 경우를 구별하는 능력
- 가능한 공격
  - 키 K에 대한 완전 탐색(exhaustive search): A가 t단계 실행, T가 H 1회 계산 시간이면 이점은 (t/T)2^-k
  - 생일 공격(birthday attack)[PrOo]: p번의 오라클 질의와 약 pT의 실행 시간으로 p^2/2^n의 이점

```
Assumption 1
T = H 한 번의 계산 시간.
A가 실행시간 최대 t, 오라클 질의 최대 p번인 임의의 공격자라면

Adv(A) <= (t/T)/2^k + p^2/2^n
```

- 이 가정은 H가 PRF로서 매우 안전함을 의미 — 예: k=n=160일 때 실행시간 2^60, 오라클 질의 2^40번인 공격자의 이점은 최대 약 2^-80

```
Theorem 1
m = 10^Digit < 2^31, (q,r) = IntDiv(2^31,m)이라 하자.
B를 v번의 검증 오라클 질의, a <= 2^c - s번의 인증자 오라클 질의, 실행시간 t로 HOTP를
공격하는 임의의 공격자, T를 H 한 번의 계산 시간이라 하자.
Assumption 1이 참이면

Adv(B) <= sv(q+1)/2^31 + (t/T)/2^k + ((sv+a)^2)/2^n
```

- (t/T)2^-k + ((sv+a)^2)2^-n 항은 sv(q+1)/2^n 항보다 훨씬 작음 → 실용적으로 HOTP 공격 성공률은 HOTP-IDEAL과 마찬가지로 sv(q+1)/2^n → HOTP는 이상화된 대응물만큼 안전
- m = 10^6(6자리 출력)일 때, v번 인증 시도하는 공격자의 성공률은 최대 Equation 1 수준
- 예시: 실행시간 최대 2^60, 사용자 인증 시도를 최대 2^40번 관찰하는(공격자에게 매우 관대한 조건) 공격자도 Equation 1 이상의 성공률을 갖지 못함
- 스로틀링과 s 제한으로 sv <= 2^40 가정 가능

```
(t/T)/2^k + ((sv+a)^2)/2^n <= 2^60/2^160 + (2^41)^2/2^160
                             ≈ 2^-78
```

- 이는 Equation 1의 성공 확률보다 훨씬 작아 무시 가능

### 부록 B. SHA-1 공격

- SHA-1에 대한 공격이 HMAC-SHA-1 기반 HOTP 보안에 미치는 영향 논의

#### B.1 SHA-1 상태

- 해시 함수 h의 충돌(collision): h(x)=h(y)인 서로 다른 입력 쌍 x,y
- SHA-1은 160비트 출력 → 생일 공격은 2^80번 시도로 충돌 발견 가능
- 2005년 2월 15일 Wang, Yin, Yu가 2^69번 시도로 충돌을 찾는 공격 발표 → 기존 최선 추정 뒤집음
- SHA-1이 "깨졌는가": 실용적 관점에서는 아님 — 공격에 필요한 자원이 막대함 (760비트 RSA 모듈러스 인수분해에 필요한 시간과 유사한 수준, 현재 도달 불가능)
- NIST의 Burr는 [Crack]에서 "대규모 국가 정보 기관은 수백만 달러의 컴퓨터 시간으로 합리적인 시간 내에 가능할 것"이라고 언급 — 그러나 이는 자금이 풍부한 기관 외에는 도달 범위 밖
- 서명 애플리케이션에 대한 실제 영향은 불분명
  - 충돌 x,y로 서명을 위조하려면 먼저 x의 서명을 얻어야 함(선택 메시지 공격 필요, 맥락에 따라 가능 여부가 다름)
  - y의 내용이 애플리케이션 맥락에서 무의미할 수 있음
- 언론의 "SHA-1이 깨졌다"[Sha1], "SSL이 깨졌다"[Res] 같은 보도는 과장된 경향
- 암호학자들의 흥분은 이론적 돌파구로서의 의미 때문 — 공격은 시간이 갈수록 개선되므로 향후 진전을 주시하고 마이그레이션 계획을 준비해 둘 필요

#### B.2 HMAC-SHA-1 상태

- SHA-1에 대한 새로운 공격은 HMAC-SHA-1 보안에 영향 없음 — 최선의 공격은 여전히 위조 전에 발신자가 2^80개 메시지를 인증해야 하는 수준
- 이유
  - HMAC은 해시 함수가 아니라 내부적으로 해시 함수를 쓰는 MAC — MAC은 비밀 키에 의존, 해시 함수는 그렇지 않음
  - MAC에서 우려할 것은 위조이지 충돌이 아님
  - HMAC은 해시 함수(SHA-1)의 충돌이 HMAC 위조로 이어지지 않도록 설계됨
- HMAC-SHA-1(K,x) = SHA-1(K_o, SHA-1(K_i,x)), K_o·K_i는 K로부터 도출
  - 공격자가 SHA-1(K_i,x) = SHA-1(K_i,y)인 "숨겨진 키 충돌(hidden-key collision)"을 찾으면 x의 MAC을 알 때 y의 MAC 위조 가능
  - 그러나 숨겨진 키 충돌은 일반 충돌보다 훨씬 어려움 — 공격자가 숨겨진 키 K_i를 모르고, 키 K로 만든 HMAC-SHA-1의 일부 출력만 가짐
  - 현재까지 최근 SHA-1 공격이 숨겨진 키 충돌 발견으로 확장된다는 주장·증거 없음
- 역사적 사례: MD5는 충돌이 쉽게 발견되어 깨진 것으로 간주되지만, HMAC-MD5에 대해서는 자명한 2^64 생일 공격보다 나은 공격이 없음 → SHA-1 맥락에서도 HMAC의 이 강점이 다시 작용

#### B.3 HOTP 상태

- HMAC-SHA-1에 새로운 약점이 드러나지 않았으므로 HOTP에는 영향 없음 — 최선의 공격은 여전히 출력 값 추측 시도
- HOTP 보안 증명은 HMAC-SHA-1이 의사난수 함수처럼 동작할 것을 요구 — 이 품질은 SHA-1에 대한 새 공격의 영향을 받지 않으므로 보장도 그대로 유지

### 부록 C. HOTP 알고리즘: 참조 구현

- OATH 공개 참조 구현(Java) — 라이선스: "OATH HOTP Algorithm"으로 식별하는 조건으로 복사·사용 허용, "as is" 제공, 보증 없음

```java
/*
 * OneTimePasswordAlgorithm.java
 * OATH Initiative,
 * HOTP one-time password algorithm
 *
 */

/* Copyright (C) 2004, OATH.  All rights reserved.
 *
 * License to copy and use this software is granted provided that it
 * is identified as the "OATH HOTP Algorithm" in all material
 * mentioning or referencing this software or this function.
 *
 * License is also granted to make and use derivative works provided
 * that such works are identified as
 *  "derived from OATH HOTP algorithm"
 * in all material mentioning or referencing the derived work.
 *
 * OATH (Open AuTHentication) and its members make no
 * representations concerning either the merchantability of this
 * software or the suitability of this software for any particular
 * purpose.
 *
 * It is provided "as is" without express or implied warranty
 * of any kind and OATH AND ITS MEMBERS EXPRESSaLY DISCLAIMS
 * ANY WARRANTY OR LIABILITY OF ANY KIND relating to this software.
 *
 * These notices must be retained in any copies of any part of this
 * documentation and/or software.
 */

package org.openauthentication.otp;

import java.io.IOException;
import java.io.File;
import java.io.DataInputStream;
import java.io.FileInputStream ;
import java.lang.reflect.UndeclaredThrowableException;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class contains static methods that are used to calculate the
 * One-Time Password (OTP) using
 * JCE to provide the HMAC-SHA-1.
 *
 * @author Loren Hart
 * @version 1.0
 */
public class OneTimePasswordAlgorithm {
    private OneTimePasswordAlgorithm() {}

    // These are used to calculate the check-sum digits.
    //                                0  1  2  3  4  5  6  7  8  9
    private static final int[] doubleDigits =
                    { 0, 2, 4, 6, 8, 1, 3, 5, 7, 9 };

    /**
     * Calculates the checksum using the credit card algorithm.
     * This algorithm has the advantage that it detects any single
     * mistyped digit and any single transposition of
     * adjacent digits.
     *
     * @param num the number to calculate the checksum for
     * @param digits number of significant places in the number
     *
     * @return the checksum of num
     */
    public static int calcChecksum(long num, int digits) {
        boolean doubleDigit = true;
        int     total = 0;
        while (0 < digits--) {
            int digit = (int) (num % 10);
            num /= 10;
            if (doubleDigit) {
                digit = doubleDigits[digit];
            }
            total += digit;
            doubleDigit = !doubleDigit;
        }
        int result = total % 10;
        if (result > 0) {
            result = 10 - result;
        }
        return result;
    }

    /**
     * This method uses the JCE to provide the HMAC-SHA-1
     * algorithm.
     * HMAC computes a Hashed Message Authentication Code and
     * in this case SHA1 is the hash algorithm used.
     *
     * @param keyBytes   the bytes to use for the HMAC-SHA-1 key
     * @param text       the message or text to be authenticated.
     *
     * @throws NoSuchAlgorithmException if no provider makes
     *       either HmacSHA1 or HMAC-SHA-1
     *       digest algorithms available.
     * @throws InvalidKeyException
     *       The secret provided was not a valid HMAC-SHA-1 key.
     *
     */
    public static byte[] hmac_sha1(byte[] keyBytes, byte[] text)
        throws NoSuchAlgorithmException, InvalidKeyException
    {
//        try {
            Mac hmacSha1;
            try {
                hmacSha1 = Mac.getInstance("HmacSHA1");
            } catch (NoSuchAlgorithmException nsae) {
                hmacSha1 = Mac.getInstance("HMAC-SHA-1");
            }
            SecretKeySpec macKey =
        new SecretKeySpec(keyBytes, "RAW");
            hmacSha1.init(macKey);
            return hmacSha1.doFinal(text);
//        } catch (GeneralSecurityException gse) {
//            throw new UndeclaredThrowableException(gse);
//        }
    }

    private static final int[] DIGITS_POWER
  // 0 1  2   3    4     5      6       7        8
  = {1,10,100,1000,10000,100000,1000000,10000000,100000000};

    /**
     * This method generates an OTP value for the given
     * set of parameters.
     *
     * @param secret       the shared secret
     * @param movingFactor the counter, time, or other value that
     *                     changes on a per use basis.
     * @param codeDigits   the number of digits in the OTP, not
     *                     including the checksum, if any.
     * @param addChecksum  a flag that indicates if a checksum digit
     *                     should be appended to the OTP.
     * @param truncationOffset the offset into the MAC result to
     *                     begin truncation.  If this value is out of
     *                     the range of 0 ... 15, then dynamic
     *                     truncation  will be used.
     *                     Dynamic truncation is when the last 4
     *                     bits of the last byte of the MAC are
     *                     used to determine the start offset.
     * @throws NoSuchAlgorithmException if no provider makes
     *                     either HmacSHA1 or HMAC-SHA-1
     *                     digest algorithms available.
     * @throws InvalidKeyException
     *                     The secret provided was not
     *                     a valid HMAC-SHA-1 key.
     *
     * @return A numeric String in base 10 that includes
     * {@link codeDigits} digits plus the optional checksum
     * digit if requested.
     */
    static public String generateOTP(byte[] secret,
               long movingFactor,
          int codeDigits,
               boolean addChecksum,
          int truncationOffset)
        throws NoSuchAlgorithmException, InvalidKeyException
    {
        // put movingFactor value into text byte array
  String result = null;
  int digits = addChecksum ? (codeDigits + 1) : codeDigits;
        byte[] text = new byte[8];
        for (int i = text.length - 1; i >= 0; i--) {
            text[i] = (byte) (movingFactor & 0xff);
            movingFactor >>= 8;
        }

        // compute hmac hash
        byte[] hash = hmac_sha1(secret, text);

        // put selected bytes into result int
        int offset = hash[hash.length - 1] & 0xf;
  if ( (0<=truncationOffset) &&
         (truncationOffset<(hash.length-4)) ) {
      offset = truncationOffset;
  }
        int binary =
            ((hash[offset] & 0x7f) << 24)
            | ((hash[offset + 1] & 0xff) << 16)
            | ((hash[offset + 2] & 0xff) << 8)
            | (hash[offset + 3] & 0xff);

        int otp = binary % DIGITS_POWER[codeDigits];
  if (addChecksum) {
      otp =  (otp * 10) + calcChecksum(otp, codeDigits);
  }
  result = Integer.toString(otp);
  while (result.length() < digits) {
      result = "0" + result;
  }
  return result;
    }
}
```

### 부록 D. HOTP 알고리즘: 테스트 값

- 테스트 데이터의 비밀: ASCII 문자열 "12345678901234567890"
- `Secret = 0x3132333435363738393031323334353637383930`
- 각 count에 대한 중간 HMAC 값(HMAC-SHA-1(secret, count)):

```
Count    Hexadecimal HMAC-SHA-1(secret, count)
0        cc93cf18508d94934c64b65d8ba7667fb7cde4b0
1        75a48a19d4cbe100644e8ac1397eea747a2d33ab
2        0bacb7fa082fef30782211938bc1c5e70416ff44
3        66c28227d03a2d5529262ff016a1e6ef76557ece
4        a904c900a64b35909874b33e61c5938a8e15ed1c
5        a37e783d7b7233c083d4f62926c7a25f238d0316
6        bc9cd28561042c83f219324d3c607256c03272ae
7        a4fb960c0bc06e1eabb804e5b397cdc4b45596fa
8        1b3c89f65e6c9e883012052823443f048b4332db
9        1637409809a679dc698207310c8c7fc07290d9e5
```

- 각 count에 대한 절단 값(16진수·10진수)과 최종 HOTP 값:

```
                  Truncated
Count    Hexadecimal    Decimal        HOTP
0        4c93cf18       1284755224     755224
1        41397eea       1094287082     287082
2         82fef30        137359152     359152
3        66ef7655       1726969429     969429
4        61c5938a       1640338314     338314
5        33c083d4        868254676     254676
6        7256c032       1918287922     287922
7         4e5b397         82162583     162583
8        2823443f        673399871     399871
9        2679dc69        645520489     520489
```

### 부록 E. 확장

- 아래는 표준 알고리즘의 일부가 아닌, 맞춤형 구현에 쓸 수 있는 변형·개선안

#### E.1 자릿수

- 간단한 보안 개선: HMAC-SHA-1 값에서 더 많은 자릿수 추출
- 예: 8자리 HOTP(10^8 모듈로) → 공격 성공률을 sv/10^6에서 sv/10^8로 감소
- T·s를 늘려 사용성을 개선하면서도 전반적 보안은 더 나은 수준 유지 가능 (예: s=10일 때 10v/10^8 = v/10^7 < v/10^6, 즉 s=1·6자리 코드의 이론적 최적값보다 낮음)

#### E.2 영숫자 값

- 대안: A-Z와 0-9 값 사용, 또는 문자 혼동 방지를 위해 32개 기호 부분집합 사용 (0/O/Q, l/1/I 등은 작은 디스플레이에서 유사하게 보임)
- 보안 수준: 6자리 영숫자 HOTP는 sv/32^6, 8자리는 sv/32^8
- 32^6 > 10^9 → 6자리 영숫자 코드는 알고리즘이 지원하는 최대 길이(9자리) HOTP 값보다 약간 더 나음
- 32^8 > 10^12 → 8자리 영숫자 코드는 9자리 HOTP 값보다 상당히 더 나음
- 애플리케이션·토큰 인터페이스에 따라, 영숫자 값 선택은 비용·사용자 영향을 줄이면서 보안을 높이는 효율적인 방법이 될 수 있음

#### E.3 HOTP 값의 시퀀스

- 재동기화를 위한 HOTP 짧은 시퀀스(2~3개) 입력 제안을 프로토콜 수준으로 일반화 — 매개변수 L(입력할 시퀀스 길이) 추가 가능
- 기본값 L=1 권장(SHOULD) — 보안을 높여야 할 경우 사용자에게 L개의 HOTP 값 입력 요청 가능
- HOTP 길이 증가나 영숫자 값 없이 보안을 강화하는 또 다른 방법
- 참고: 시스템이 정기적으로(예: 매일 밤, 주 2회) 동기화를 요청하도록 구성해 L개 시퀀스를 요청할 수 있음(MAY)

#### E.4 카운터 기반 재동기화 방법

- 전제: 클라이언트가 HOTP 값뿐 아니라 카운터 값도 함께 전송 가능
- 이 경우 더 효율적·안전한 재동기화 가능 — 클라이언트는 HOTP-클라이언트 값과 함께 C-클라이언트 카운터 값을 전송, HOTP 값이 카운터의 메시지 인증 코드 역할

```
카운터 기반 재동기화 프로토콜(RCP)

서버는 다음이 모두 참이면 수락(C-server는 서버의 현재 카운터 값):
1) C-client >= C-server
2) C-client - C-server <= s
3) HOTP 클라이언트 값이 유효한 HOTP(K,C-Client)인지 확인
4) 참이면 서버는 C를 C-client + 1로 설정, 클라이언트 인증 완료
```

- 효과: 룩어헤드 윈도우 관리가 더 이상 필요 없음 — 공격자 성공률은 v/10^6 수준(약 백만 분의 v)
- 부수 이점: s를 사실상 무한히 늘려도 보안에 영향 없이 사용성 향상 가능
- 이 재동기화 프로토콜은 클라이언트·서버 애플리케이션에 미치는 영향이 허용 가능하다고 판단될 때 사용 권장(SHOULD)

#### E.5 데이터 필드

- 옵션: OTP 생성에 쓰일 데이터 필드(Data) 도입 → `HOTP(K, C, [Data])`, Data는 다양한 신원 관련 정보의 연결(concatenation)일 수 있는 선택적 필드 (예: `Data = Address | PIN`)
- 타이머(Timer)를 유일한 이동 인자로, 또는 카운터와 조합해 사용 가능 — 예: `Data = Timer`, Timer는 특정 시간 단계(time step)로 나눈 UNIX 시간
  - 예: 시간 단계 64초 + 재동기화 매개변수 7 → 수용 윈도우는 ±3분
- 데이터 필드가 명확히 명세되어 있는 한, 이 방식은 알고리즘 구현에 더 많은 유연성을 제공

---

## RFC 6238 - TOTP: Time-Based One-Time Password Algorithm

> 발행일: 2011년 5월
> 상태: Informational

### 1. 개요

TOTP(Time-based One-Time Password): [RFC 4226 HOTP](./RFC4226-HOTP.md)의 이동 인자(moving factor)를 카운터 대신 현재 시각으로 대체한 알고리즘
서버-클라이언트가 카운터 값을 동기화할 필요 없음 → 둘 다 같은 시계(clock) 기준으로 같은 시간 구간에 있으면 동일한 OTP 값 계산 가능

- 구분별 차이
  - 이동 인자: HOTP는 증가하는 카운터 C, TOTP는 현재 UNIX 시각을 구간으로 나눈 값 T
  - 동기화 대상: HOTP는 카운터, TOTP는 시계
  - 값 변경 시점: HOTP는 사용할 때마다, TOTP는 시간 구간(보통 30초)마다
  - 대표 사용처: HOTP는 하드웨어 토큰·이벤트 기반 인증, TOTP는 Google Authenticator·Authy 등 OTP 앱

---

### 2. 알고리즘

```
TOTP(K) = HOTP(K, T)

T = floor((현재 UNIX 시각 - T0) / X)
```

- 기호
  - K: 공유 비밀 키
  - T0: 카운터 시작 시각(UNIX epoch), 기본값 0
  - X: 시간 구간 길이(초), 기본값 30
  - T: 시간 구간 카운터 값 (HOTP의 C 자리에 대입)

HOTP와 마찬가지로 HMAC 결과를 동적 절단(dynamic truncation)해 지정 자릿수(보통 6자리)로 축약
HMAC 해시 함수는 SHA-1(기본)·SHA-256·SHA-512 사용 가능

#### 2.1 계산 예시 (의사코드)

```python
def totp(key: bytes, digits=6, period=30, t0=0, algo="sha1"):
    t = int((time.time() - t0) // period)
    counter = t.to_bytes(8, "big")
    return hotp(key, counter, digits, algo)
```

30초 구간 사용 시 → 같은 30초 안의 여러 조회는 값이 바뀌지 않음 → 구간이 넘어가는 순간 값 갱신

---

### 3. 시간 동기화와 허용 오차

클라이언트(토큰/앱)와 서버의 시계는 완벽히 일치하기 어려움 → 검증 서버는 보통 앞뒤로 한두 구간의 오차 허용

```
검증 시각 T에 대해 T-1, T, T+1 세 구간의 값을 모두 계산해
클라이언트가 보낸 값과 비교
```

- 항목별 권장 사항
  - 허용 윈도우: 앞뒤 1구간(±30초) 정도로 제한
  - 큰 오차 감지 시: 서버가 클라이언트 시계 오차를 추정해 다음 검증부터 보정
  - 재사용 방지: 이미 성공적으로 검증된 T 값은 재사용 금지(재전송 공격 방지)

---

### 4. 보안 고려사항

- 공유 비밀 길이: HOTP와 동일하게 최소 128비트, 160비트 이상 권장
- 시간 구간 길이: 짧을수록 공격 윈도우가 줄지만 사용성이 나빠짐 → 30초가 일반적
- 재전송 공격: 같은 시간 구간 내 탈취한 OTP 값 재사용 가능 → 재사용 여부를 서버가 기록해 차단 필요
- 시계 신뢰: 서버·클라이언트 모두 신뢰할 수 있는 시각 동기화(NTP 등) 필요
- 브루트포스: HOTP와 동일하게 검증 시도 횟수 제한(스로틀링) 필요

---

### 5. 프로비저닝: `otpauth://` URI

Google Authenticator 등에서 사실상 표준으로 쓰이는 QR 코드 형식(RFC는 아니지만 TOTP 생태계의 관례):

```
otpauth://totp/{issuer}:{account}?secret={base32비밀}&issuer={issuer}&algorithm=SHA1&digits=6&period=30
```

- 파라미터별 의미
  - secret: Base32로 인코딩한 공유 비밀 키
  - issuer: 서비스 제공자 이름
  - algorithm: SHA1(기본)·SHA256·SHA512
  - digits: OTP 자릿수(6 또는 8)
  - period: 시간 구간(초), 기본 30

---

### 6. 요약

- TOTP는 HOTP의 카운터를 시간 구간으로 대체한 알고리즘
- 서버-클라이언트 간 카운터 동기화 문제를 시계 동기화 문제로 전환
- 30초 구간 + ±1구간 허용 오차가 일반적인 구현 관례
- MFA(다중 인증)에서 가장 널리 쓰이는 OTP 방식 → [RFC 4226 HOTP](./RFC4226-HOTP.md), [RFC 6749 OAuth2](./RFC6749-OAuth2.md) 기반 인증 흐름과 함께 2단계 인증에 사용

---

### 참고 자료

- [RFC 6238 원문](https://www.rfc-editor.org/rfc/rfc6238)
- [RFC 4226 HOTP](./RFC4226-HOTP.md)
- [Key Uri Format (Google Authenticator wiki)](https://github.com/google/google-authenticator/wiki/Key-Uri-Format)

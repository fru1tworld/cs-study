# AWS IAM (Identity and Access Management) 문서

AWS Identity and Access Management(IAM)는 AWS 리소스에 대한 접근을 안전하게 제어하는 웹 서비스입니다. 이 문서는 AWS 공식 문서를 기반으로 IAM의 모든 주요 개념과 기능을 정리한 것입니다.

## 문서 구조

```
IAM/
├── index.md                    # 목차 (현재 문서)
├── 01_overview/                # IAM 개요
│   ├── README.md
│   ├── introduction.md
│   └── concepts.md
├── 02_identities/              # IAM 자격 증명
│   ├── README.md
│   ├── root-user.md
│   ├── users.md
│   ├── groups.md
│   ├── roles.md
│   ├── temporary-credentials.md
│   └── identity-providers.md
├── 03_policies/                # IAM 정책
│   ├── README.md
│   ├── policy-overview.md
│   ├── policy-structure.md
│   ├── managed-vs-inline.md
│   ├── condition-keys.md
│   ├── evaluation-logic.md
│   └── permissions-boundary.md
├── 04_security/                # IAM 보안
│   ├── README.md
│   ├── best-practices.md
│   ├── mfa.md
│   └── access-keys.md
└── 05_advanced/                # 고급 기능
    ├── README.md
    ├── access-analyzer.md
    ├── cross-account-access.md
    └── organizations-scp.md
```

## 목차

### 1. IAM 개요 ([01_overview](./01_overview/README.md))

IAM의 기본 개념과 작동 원리를 이해합니다.

- [IAM 소개](./01_overview/introduction.md)
  - IAM이란?
  - 핵심 기능 8가지
  - 인증과 인가
  - 서비스 가용성
  - 요금 정보

- [핵심 개념](./01_overview/concepts.md)
  - 리소스, 자격 증명, 엔터티, 보안 주체
  - ARN (Amazon Resource Name)
  - 요청 컨텍스트
  - 정책 평가 로직
  - 암묵적 거부 vs 명시적 거부

### 2. IAM 자격 증명 ([02_identities](./02_identities/README.md))

AWS 리소스에 접근하는 주체를 정의합니다.

- [루트 사용자](./02_identities/root-user.md)
  - 루트 사용자란?
  - 루트 사용자만 수행 가능한 작업
  - 보안 모범 사례

- [IAM 사용자](./02_identities/users.md)
  - IAM 사용자란?
  - 사용자 식별 방법 (이름, ARN, ID)
  - 자격 증명 유형 (비밀번호, 액세스 키)
  - 사용자 관리 모범 사례

- [IAM 그룹](./02_identities/groups.md)
  - IAM 그룹이란?
  - 그룹 정책 설정
  - 일반적인 그룹 구성 예시
  - 그룹 제한 사항

- [IAM 역할](./02_identities/roles.md)
  - IAM 역할이란?
  - 역할 구성 요소 (신뢰 정책, 권한 정책)
  - 역할 유형별 사용 사례
  - 역할 맡기 (AssumeRole)
  - 역할 체이닝

- [임시 보안 자격 증명](./02_identities/temporary-credentials.md)
  - AWS STS 개요
  - STS API 작업 (AssumeRole, GetSessionToken 등)
  - 사용 사례
  - 세션 기간

- [자격 증명 공급자](./02_identities/identity-providers.md)
  - 연합 인증 이점
  - SAML 2.0 연합
  - OIDC 연합
  - IAM Identity Center

### 3. IAM 정책 ([03_policies](./03_policies/README.md))

접근 권한을 정의하는 JSON 문서를 이해합니다.

- [정책 개요](./03_policies/policy-overview.md)
  - IAM 정책이란?
  - 7가지 정책 유형
  - 정책 평가 순서

- [정책 구조 (JSON)](./03_policies/policy-structure.md)
  - Version, Statement, Effect
  - Principal, Action, Resource
  - Condition
  - 정책 변수
  - 실제 정책 예시

- [관리형 vs 인라인 정책](./03_policies/managed-vs-inline.md)
  - AWS 관리형 정책
  - 고객 관리형 정책
  - 인라인 정책
  - 선택 기준

- [조건 키](./03_policies/condition-keys.md)
  - 조건 연산자
  - 전역 조건 키
  - 서비스별 조건 키
  - 다중 값 조건

- [정책 평가 로직](./03_policies/evaluation-logic.md)
  - 단일 계정 평가 흐름
  - 교차 계정 접근 평가
  - 다중 정책 조합

- [권한 경계](./03_policies/permissions-boundary.md)
  - 권한 경계란?
  - 권한 위임 시나리오
  - 리소스 기반 정책과의 상호작용

### 4. IAM 보안 ([04_security](./04_security/README.md))

IAM을 안전하게 사용하기 위한 모범 사례입니다.

- [보안 모범 사례](./04_security/best-practices.md)
  - 14가지 IAM 보안 권장사항
  - 루트 사용자 보호
  - 최소 권한 원칙
  - 연합 인증 사용
  - 정기적인 검토

- [다중 인증 (MFA)](./04_security/mfa.md)
  - MFA 유형 (패스키, 가상 MFA, 하드웨어 토큰)
  - MFA 설정 방법
  - MFA 강제 정책
  - CLI/API에서 MFA 사용

- [액세스 키 관리](./04_security/access-keys.md)
  - 액세스 키 생성 및 교체
  - 안전한 저장 방법
  - 노출된 키 대응
  - 모니터링

### 5. 고급 기능 ([05_advanced](./05_advanced/README.md))

엔터프라이즈 환경을 위한 고급 기능입니다.

- [IAM Access Analyzer](./05_advanced/access-analyzer.md)
  - 외부 접근 분석
  - 미사용 접근 분석
  - 정책 검증
  - 정책 생성

- [교차 계정 접근](./05_advanced/cross-account-access.md)
  - IAM 역할 기반 접근
  - 외부 ID 사용
  - 혼동된 대리인 문제 방지
  - 역할 체이닝

- [Organizations SCP](./05_advanced/organizations-scp.md)
  - 서비스 제어 정책 개요
  - SCP 전략 (거부 목록 vs 허용 목록)
  - 일반적인 SCP 예시
  - SCP 관리

---

## 빠른 참조

### 핵심 개념 요약

| 개념 | 설명 |
|------|------|
| **IAM 사용자** | AWS 계정 내 개별 자격 증명 (사람 또는 서비스용) |
| **IAM 그룹** | IAM 사용자들의 모음 |
| **IAM 역할** | 임시로 맡을 수 있는 권한 집합 |
| **IAM 정책** | 권한을 정의하는 JSON 문서 |
| **권한 경계** | 자격 증명의 최대 권한 제한 |
| **SCP** | 조직 내 계정의 최대 권한 제한 |

### 정책 평가 우선순위

```
1. 명시적 거부 (Deny) → 항상 최우선
2. SCP (Organizations)
3. 리소스 기반 정책
4. 자격 증명 기반 정책
5. 권한 경계
6. 세션 정책
7. 암묵적 거부 (기본값)
```

### 보안 체크리스트

- [ ] 루트 사용자 MFA 활성화
- [ ] 루트 액세스 키 없음
- [ ] 모든 사용자 MFA 활성화
- [ ] 최소 권한 정책 적용
- [ ] 액세스 키 정기 교체 (90일)
- [ ] 미사용 자격 증명 제거
- [ ] IAM Access Analyzer 활성화
- [ ] CloudTrail 활성화

---

## 참고 자료

- [AWS IAM 공식 문서](https://docs.aws.amazon.com/IAM/latest/UserGuide/)
- [AWS IAM FAQ](https://aws.amazon.com/iam/faqs/)
- [AWS Security Best Practices](https://docs.aws.amazon.com/wellarchitected/latest/security-pillar/)

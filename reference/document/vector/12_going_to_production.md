# 프로덕션 환경으로 전환 (Going to Production)

> 이 문서는 [Vector 공식 문서](https://vector.dev/docs/)의 Going to Production 섹션을 한국어로 번역한 것입니다.
> 원문: https://vector.dev/docs/setup/going-to-prod/

---

## 목차

1. [개요](#개요)
2. [레퍼런스 아키텍처](#레퍼런스-아키텍처)
   - [애그리게이터 아키텍처](#애그리게이터-아키텍처)
   - [에이전트 아키텍처](#에이전트-아키텍처)
   - [통합 아키텍처](#통합-아키텍처)
3. [아키텍처 설계](#아키텍처-설계)
   - [설계 지침](#설계-지침)
   - [배포 역할](#배포-역할)
   - [네트워킹](#네트워킹)
   - [데이터 수집](#데이터-수집)
   - [데이터 처리](#데이터-처리)
   - [버퍼링 전략](#버퍼링-전략)
   - [라우팅 및 분리](#라우팅-및-분리)
4. [보안 강화 (Hardening)](#보안-강화-hardening)
   - [위협 모델](#위협-모델)
   - [심층 방어 전략](#심층-방어-전략)
5. [고가용성 (High Availability)](#고가용성-high-availability)
   - [장애 모드](#장애-모드)
   - [고가용성 전략](#고가용성-전략)
   - [재해 복구](#재해-복구)
6. [롤아웃 (Rollout)](#롤아웃-rollout)
   - [롤아웃 전략](#롤아웃-전략)
   - [롤아웃 계획](#롤아웃-계획)
7. [사이징 및 용량 계획 (Sizing)](#사이징-및-용량-계획-sizing)
   - [인스턴스 권장 사항](#인스턴스-권장-사항)
   - [메모리 및 CPU 전략](#메모리-및-cpu-전략)
   - [디스크 구성](#디스크-구성)
   - [스케일링 접근 방식](#스케일링-접근-방식)
   - [오토스케일링](#오토스케일링)
   - [실제 사례](#실제-사례)

---

## 개요

이 섹션은 Vector를 프로덕션 환경에 배포하는 인프라 아키텍트를 위한 가이드라인과 모범 사례를 제공합니다. 프로덕션 배포를 위한 다섯 가지 주요 영역을 다룹니다:

1. 아키텍처 설계 (Architecting) - Vector 인프라 설계 원칙
2. 보안 강화 (Hardening) - 보안 및 견고성 조치
3. 고가용성 (High Availability) - 서비스 연속성 보장
4. 롤아웃 (Rollout) - 배포 전략 및 단계적 접근 방식
5. 사이징 및 용량 계획 (Sizing) - 리소스 할당 가이드

---

## 레퍼런스 아키텍처

레퍼런스 아키텍처는 파이프라인 인프라의 출발점을 제공합니다. Vector는 세 가지 주요 배포 패턴을 지원합니다.

### 애그리게이터 아키텍처

"원격 처리를 위해 전용 노드에 Vector를 애그리게이터로 배포합니다."

애그리게이터 아키텍처는 중앙 집중식 로그 및 메트릭 수집을 위해 설계되었습니다. 데이터는 하나 이상의 업스트림 에이전트 또는 업스트림 시스템에서 수집됩니다.

#### 사용 시기

이 아키텍처는 다음과 같은 환경에 적합합니다:

- 높은 내구성 또는 고가용성이 필요한 환경 (대부분의 환경)
- 기존 에이전트를 변경하지 않고도 복잡한 환경에 쉽게 통합 할 수 있음
- 기업 및 대규모 조직 에 특히 적합

#### 프로덕션 권장 사항

아키텍처 설계:
- 네트워크 경계(클러스터/VPC)당 여러 애그리게이터 배포
- 에이전트 라우팅을 위해 DNS 또는 서비스 디스커버리 사용
- HTTP 기반 프로토콜 선호
- Vector 간 통신을 위해 Vector 네이티브 source/sink 사용
- 처리 책임을 애그리게이터로 이동
- 에이전트는 단순한 포워더로 구성

고가용성:
- 여러 노드 및 가용 영역에 애그리게이터 배포
- 모든 소스에 대해 엔드투엔드 승인(acknowledgements) 활성화
- 주요 싱크 시스템에 디스크 버퍼 구현
- 분석 시스템에 워터폴 버퍼 사용
- 실패한 데이터를 주요 시스템으로 라우팅

사이징 및 용량:
- 애그리게이터 트래픽 로드 밸런싱
- 인스턴스당 최소 4 vCPU 및 8 GiB 메모리 프로비저닝
- 85% CPU 사용률을 목표로 오토스케일링 활성화

롤아웃:
"한 번에 하나의 네트워크 파티션과 하나의 시스템"으로 점진적 도입 구현

#### 고급 패턴

Pub-Sub 시스템:
Vector는 새로운 pub-sub 서비스를 프로비저닝할 필요가 없습니다. Kafka와 같은 기존 시스템에서 데이터를 소비할 수 있으며, "서비스 또는 호스트와 같은 데이터 출처 라인"을 기반으로 파티셔닝하는 것을 권장합니다.

글로벌 애그리게이션:
단일 장애 지점을 생성하지 않으면서 데이터 축소(히스토그램)를 위한 계층화된 애그리게이션을 지원합니다.

---

### 에이전트 아키텍처

"엣지에서 Vector를 실행하여 처리를 민주화합니다."

에이전트 아키텍처는 로컬 데이터 수집 및 처리를 위해 각 노드에 Vector를 에이전트로 배포합니다. 데이터는 Vector에서 직접 수집하거나, 다른 에이전트를 통해 간접적으로 수집하거나, 두 가지를 동시에 수행할 수 있습니다.

#### 사용 시기

이 아키텍처는 다음과 같은 경우에 권장됩니다:

- 높은 내구성이나 고가용성이 필요하지 않은 단순한 환경
- 빠른 상태 비저장 처리 및 스트리밍 전송과 같이 확장된 데이터 보존이 필요하지 않은 사용 사례
- 최소한의 저항으로 노드 수준 수정을 구현할 수 있는 운영자

#### 프로덕션 권장 사항

아키텍처 설계:
- 일반 데이터 포워딩을 수행하는 에이전트만 교체; 특수 에이전트와는 통합
- 처리를 "빠른 상태 비저장 처리"로 제한
- 전송을 "빠른 스트리밍 전송"으로 제한
- 인메모리 버퍼링만 사용; 디스크 버퍼링 피하기

고가용성:
- 단일 Vector 에이전트 장애가 문제가 되는 경우, 애그리게이터 아키텍처 고려
- 데이터 수신 실패 완화를 위해 엔드투엔드 승인 활성화
- 처리 실패 해결을 위해 드롭된 이벤트 라우팅

사이징 요구 사항:
Vector 에이전트를 2 vCPU 및 4 GiB 메모리 로 제한

롤아웃 전략:
"한 번에 하나의 네트워크 파티션과 하나의 시스템"으로 점진적 배포

#### 고급 섹션

엣지에서의 처리:
에이전트는 데이터를 보존하지 않아야 합니다. 처리와 전송은 "빠르고 스트리밍"이어야 합니다.

---

### 통합 아키텍처

"엣지에서 수집하고 애그리게이션하여 최대 유연성을 제공합니다."

통합 아키텍처는 Vector를 에이전트와 애그리게이터 모두로 배포하여, 에이전트 아키텍처와 애그리게이터 아키텍처를 결합한 통합 옵저버빌리티 파이프라인을 생성합니다.

#### 사용 시기

이 아키텍처는 이미 애그리게이터 접근 방식을 구현하고 개별 노드로 Vector를 확장하려는 사용자에게 권장됩니다. 이 진화는 다음을 통해 옵저버빌리티 인프라를 강화합니다:

- Vector가 데이터 전송 책임을 관리하여 에이전트 관련 위험 감소
- vector source 및 sink 컴포넌트를 통한 Vector의 네이티브 프로토콜을 사용하여 성능 향상
- 상태 비저장 처리 작업을 엣지 위치로 이동하여 확장성 개선
- 고급 장애 조치 및 재해 복구 기능 지원

---

## 아키텍처 설계

### 설계 지침

프로덕션 배포를 위한 핵심 설계 지침:

1. 최적의 도구 사용 - Vector는 모든 도구를 대체하기보다는 기존 도구를 보완합니다.
2. 에이전트 책임 최소화 - 에이전트는 단순한 데이터 포워더로 유지하고, 복잡한 작업은 Vector에 위임합니다.
3. 데이터 가까이에 Vector 배포 - 단일 장애 지점을 피하기 위해 인프라 전체에 Vector를 분산 배포합니다.

### 배포 역할

Vector는 두 가지 주요 역할로 배포할 수 있습니다:

| 역할 | 설명 |
|------|------|
| 에이전트 역할 | 개별 노드에 직접 Vector를 배포하여 로컬 데이터 수집 및 처리. 데이터는 로컬에서 처리하거나 원격 애그리게이터로 전달할 수 있습니다. |
| 애그리게이터 역할 | 업스트림 에이전트 또는 pub-sub 서비스에서 데이터를 수신하는 전용 노드에 Vector를 배포. 성능 최적화된 하드웨어에서 중앙 집중식 처리. |

### 네트워킹

네트워크 경계 내 배포:
- 계정 및 리전 전반에 걸쳐 네트워크 경계(클러스터/VPC)당 애그리게이터 배포
- 이 전략은 단일 장애 지점 방지, 쉽고 안전한 내부 통신 허용, 네트워크 관리자가 유지하는 경계 존중

방화벽 액세스 제한:
- 에이전트 -> 애그리게이터
- 애그리게이터 -> 구성된 소스/싱크

DNS/서비스 디스커버리:
Vector 애그리게이터 등록을 위해 DNS 또는 서비스 디스커버리 사용

프로토콜 선호:
로드 밸런싱 및 전송 승인을 위해 HTTP 및 gRPC 프로토콜 선호

### 데이터 수집

에이전트 선택 가이드:

교체할 에이전트:
- 일반적인 포워딩을 수행하는 에이전트 (로그 테일링, 기본 메트릭 수집)

통합할 에이전트:
- 강화된 데이터를 생성하는 벤더 특정 도구 (Datadog Agent, Metricbeat, Telegraf 등)

### 데이터 처리

로컬 처리:
- 높은 내구성/가용성 요구 사항이 없는 단순한 환경에 적합

원격 처리:
- HA/내구성이 필요한 대부분의 환경에 권장

통합 처리:
- 두 전략을 균형 있게 결합하는 접근 방식

### 버퍼링 전략

- 싱크별로 격리된 버퍼로 목적지 가까이에서 데이터 버퍼링
- 고내구성 기록 시스템에는 디스크 버퍼 사용
- 저지연 분석 시스템에는 메모리 버퍼 사용
- 아카이브용: 배치 크기 5MB 이상, 타임아웃 5분 이상
- 분석용: 타임아웃 5초 이하

### 라우팅 및 분리

기록 시스템 (Systems of Record):
- 원시, 처리되지 않은 데이터 아카이브
- 디스크 버퍼링 사용

분석 시스템 (Systems of Analysis):
- 데이터 샘플링/정리
- 메모리 버퍼링 사용
- 불필요한 속성 제거

---

## 보안 강화 (Hardening)

이 섹션은 데이터, 프로세스, 호스트 및 네트워크 계층 전반에 걸친 심층 방어 전략을 따르는 Vector 배포를 위한 보안 가이드를 제공합니다.

### 위협 모델

다섯 가지 주요 위협에 대한 방어:

| 위협 | 방어 수단 |
|------|----------|
| 도청 공격 | 디스크 암호화, 스왑 비활성화, 디렉토리 제한, 스토리지 암호화, 속성 수정, 코어 덤프 방지, 엔드투엔드 TLS, 현대적 암호화, 방화벽 |
| 공급망 공격 | 암호화된 채널을 통한 다운로드, Vector 다운로드 검증 |
| 자격 증명 도난 | 평문 비밀 저장 피하기, 구성 디렉토리 제한 |
| 권한 상승 | 비특권 사용자로 Vector 실행, 서비스 계정 제한, 단일 테넌시, 원격 액세스 비활성화 |
| 업스트림 공격 | 보안 정책 검토, 빈번한 업그레이드 |

### 심층 방어 전략

심층 방어는 네 가지 계층으로 구성된 "양파 모델"을 따릅니다:

#### 데이터 보안

```yaml
# 데이터 보안 체크리스트
- 운영 체제 수준에서 전체 디스크 암호화 활성화
- 메모리가 디스크로 오버플로되는 것을 방지하기 위해 스왑 비활성화
- Vector 데이터 디렉토리 권한 제한
- 플랫폼별 방법을 사용하여 외부 스토리지 암호화
- VRL 함수를 사용하여 민감한 PII 수정
- 시스템 제한을 통해 코어 덤프 비활성화
```

VRL을 사용한 민감 데이터 수정 예시:

```yaml
transforms:
  redact_pii:
    type: remap
    inputs:
      - source_id
    source: |
      # 이메일 주소 수정
      .message = redact(.message, filters: ["email"])

      # 신용카드 번호 수정
      .message = redact(.message, filters: ["credit_card"])

      # 사용자 정의 패턴 수정
      .message = redact(.message, patterns: [r'\d{3}-\d{2}-\d{4}'])
```

#### 프로세스 보안

- Vector의 오픈 소스 코드를 활용하여 투명성 확보
- TLS를 통해서만 아티팩트 다운로드
- 구성에 평문 비밀 저장 금지
- 구성 디렉토리 액세스 제한
- 절대로 Vector를 root로 실행하지 마세요
- 서비스 계정 권한 제한
- 빈번한 업그레이드 유지

환경 변수를 사용한 비밀 관리:

```yaml
sinks:
  elasticsearch:
    type: elasticsearch
    endpoints:
      - "https://elasticsearch.example.com:9200"
    auth:
      user: "${ELASTICSEARCH_USER}"
      password: "${ELASTICSEARCH_PASSWORD}"
```

#### 호스트 보안

- 애그리게이터 머신에 단일 테넌트로 Vector 배포
- SSH/직접 액세스 비활성화; 대신 중앙 제어 플레인 사용

#### 네트워크 보안

- 모든 소스 및 싱크에 엔드투엔드 TLS 활성화
- 현대적 암호화 구현 (TLS 1.3 권장)
- 트래픽 제한을 위해 방화벽 배포

TLS 구성 예시:

```yaml
sources:
  http_server:
    type: http_server
    address: "0.0.0.0:443"
    tls:
      enabled: true
      crt_file: "/etc/vector/certs/server.crt"
      key_file: "/etc/vector/certs/server.key"
      ca_file: "/etc/vector/certs/ca.crt"
      verify_certificate: true

sinks:
  elasticsearch:
    type: elasticsearch
    endpoints:
      - "https://elasticsearch.example.com:9200"
    tls:
      verify_certificate: true
      verify_hostname: true
```

> 참고: Vector는 Rust의 어파인 타입 시스템을 사용하여 메모리 안전성을 달성하고 자동 메모리 정리를 통해 데이터 노출을 최소화합니다. 시스템은 더 이상 필요하지 않을 때 메모리에서 데이터를 자동으로 제거합니다.

---

## 고가용성 (High Availability)

"인프라 수준 소프트웨어의 엄격한 가동 시간 요구 사항을 충족합니다."

### 장애 모드

#### 하드웨어 장애

디스크 장애:
디스크 장애는 프로세스 장애로 처리됩니다. 런타임 중에 디스크를 사용할 수 없게 되면 Vector가 종료됩니다. 디스크는 디스크 버퍼, 파일 소스/싱크와 같은 컴포넌트를 사용하는 경우에만 필요합니다.

노드 장애:
완화 방법:
- 여러 노드에 Vector 분산
- 장애 조치를 위한 로드 밸런서 사용
- 단일 노드가 데이터의 33% 이상을 처리하지 않도록 보장
- 자동화된 자가 치유 구현

데이터 센터 장애:
보호 전략:
- 여러 가용 영역에 Vector 배포
- 단일 AZ 손실을 처리할 수 있는 용량 유지
- 오버 프로비저닝을 위해 Vector의 고성능 설계 활용

#### 소프트웨어 장애

Vector 프로세스 장애:
노드 장애와 유사한 완화: 플랫폼 수준 감독자(예: Kubernetes 컨트롤러)를 사용한 로드 밸런서 장애 조치

데이터 수신 장애:
해결책: "Vector의 엔드투엔드 승인 기능 활성화." 승인이 전송되기 전에 데이터가 지속됩니다.

```yaml
sources:
  http_server:
    type: http_server
    address: "0.0.0.0:8080"
    acknowledgements:
      enabled: true
```

데이터 처리 장애:
접근 방식: 드롭된 이벤트를 백업 목적지로 라우팅하는 remap transform 사용, 나중에 검사 및 재생 가능

```yaml
transforms:
  parse_logs:
    type: remap
    inputs:
      - source_id
    source: |
      structured, err = parse_json(.message)
      if err != null {
        log("Failed to parse: " + err, level: "error")
      }
      . = structured ?? .
    drop_on_error: true
    reroute_dropped: true

sinks:
  # 성공적으로 처리된 이벤트
  main_destination:
    type: elasticsearch
    inputs:
      - parse_logs
    # ...

  # 실패한 이벤트 백업
  failed_events:
    type: file
    inputs:
      - parse_logs.dropped
    path: "/var/log/vector/failed/%Y-%m-%d.log"
    encoding:
      codec: json
```

데이터 전송 장애:
완화 방법:
- 자동 연결 스케일링을 위한 적응형 요청 동시성 (ARC)
- 백프레셔를 흡수하기 위한 버퍼
- 재생 기능을 갖춘 내구성 있는 데이터 지속성

```yaml
sinks:
  elasticsearch:
    type: elasticsearch
    inputs:
      - transform_id
    endpoints:
      - "https://elasticsearch.example.com:9200"
    buffer:
      type: disk
      max_size: 10737418240  # 10 GiB
      when_full: block
    request:
      adaptive_concurrency:
        decrease_ratio: 0.9
        ewma_alpha: 0.4
        rtt_deviation_scale: 2.5
```

#### 전체 시스템 장애

다음에 대해 서비스 디스커버리 또는 DNS를 사용하여 네트워크 내 스탠바이로 장애 조치:
- 로드 밸런서 시스템 장애
- 애그리게이터 시스템 장애

### 고가용성 전략

#### 폭발 반경 억제

단일 애그리게이터 장애가 연쇄적으로 발생하는 것을 방지하기 위해 네트워크 경계 내에 Vector를 배포합니다.

#### 하드웨어 장애 완화

1. 여러 가용 영역에 노드 배포
2. 단일 AZ 장애를 처리할 수 있는 용량 확보

#### 소프트웨어 장애 완화

구성 요구 사항:
- 가능한 소스에서 엔드투엔드 승인 활성화
- 디스크 오버플로가 있는 디스크/메모리 버퍼 구현
- 드롭된 이벤트를 기록 시스템으로 라우팅

워터폴 버퍼 구성 예시:

```yaml
sinks:
  primary_destination:
    type: elasticsearch
    inputs:
      - transform_id
    endpoints:
      - "https://elasticsearch.example.com:9200"
    buffer:
      type: disk
      max_size: 10737418240  # 10 GiB
      when_full: overflow
```

#### 전체 시스템 장애 완화

엄격한 요구 사항을 위한 고급 전술:
- 에이전트가 스탠바이 로드 밸런서로 장애 조치
- 로드 밸런서가 스탠바이 애그리게이터로 장애 조치
- 비용 절감을 위해 오토스케일링 사용

### 재해 복구

내부 DR:
Vector는 복제된 상태를 공유하지 않습니다. 더 넓은 조직 DR 계획을 따릅니다.

외부 DR:
Vector는 서킷 브레이커를 통해 관리형 서비스 DR 사이트로의 라우팅을 용이하게 합니다.

---

## 롤아웃 (Rollout)

이 섹션은 Vector를 프로덕션 환경에 롤아웃하기 위한 전략을 제공합니다.

### 롤아웃 전략

네 가지 핵심 원칙:

#### 1. 점진적 도입

한 번에 하나의 네트워크 파티션(클러스터 또는 VPC)에 Vector를 배포하여 지속 가능한 진행을 가능하게 합니다.

#### 2. 범위 최소화

롤아웃 계획에 따라 각 단위에 대해 "한 번에 하나의 네트워크 파티션과 하나의 시스템"에 배포를 집중합니다.

#### 3. 안전한 실패

Vector는 현재 프로덕션 워크플로를 방해하지 않고 "중복 데이터 스트림에서 작동"해야 하며, 비즈니스 크리티컬한 사용 전에 신뢰를 구축할 수 있어야 합니다.

#### 4. "빅 스위치" 피하기

전환 시점까지 Vector는 이미 "지속적인 기간 동안 프로덕션 용량으로 운영"되어 신뢰성을 보장해야 합니다.

### 롤아웃 계획

5단계 롤아웃 계획:

#### Phase 1: 블랙홀 배포

- 단일 네트워크 파티션 선택
- 해당 네트워크 파티션 내에 단일 blackhole 싱크(기본값)로 Vector 배포
- 10 MiB/s/vCPU로 인스턴스를 보수적으로 사이징

```yaml
# Phase 1: 블랙홀 구성
sources:
  http_server:
    type: http_server
    address: "0.0.0.0:8080"

sinks:
  blackhole:
    type: blackhole
    inputs:
      - http_server
    print_interval_secs: 60
```

#### Phase 2: 데이터 복사 스트리밍

- 에이전트에서 Vector로 데이터 복사 스트리밍
- `vector top` 및 `vector tap` 명령을 사용하여 수신 확인

```bash
# Vector 상태 모니터링
vector top

# 실시간 이벤트 검사
vector tap transform_id
```

#### Phase 3: Vector 구성

- 사용 사례에 따라 데이터 처리
- 다운스트림 시스템과 통합
- 목적지에서 데이터 확인

```yaml
# Phase 3: 전체 구성 예시
sources:
  http_server:
    type: http_server
    address: "0.0.0.0:8080"
    acknowledgements:
      enabled: true

transforms:
  parse_logs:
    type: remap
    inputs:
      - http_server
    source: |
      . = parse_json!(.message)
      .processed_at = now()

sinks:
  elasticsearch:
    type: elasticsearch
    inputs:
      - parse_logs
    endpoints:
      - "https://elasticsearch.example.com:9200"
    bulk:
      index: "logs-%Y-%m-%d"
    buffer:
      type: disk
      max_size: 10737418240
```

#### Phase 4: 사이징, 스케일링 및 소크

- 오토스케일링 활성화
- 프로덕션 준비 상태를 위해 24시간 이상 모니터링

```yaml
# 오토스케일링 예시 (Kubernetes HPA)
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: vector-aggregator
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: vector-aggregator
  minReplicas: 3
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 85
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
    scaleUp:
      stabilizationWindowSeconds: 300
```

#### Phase 5: 전환

1. 에이전트를 안전하게 종료하여 손실 없이 데이터를 드레인
2. Vector로만 데이터를 전송하도록 에이전트 재구성
3. 에이전트 재시작

```yaml
# 에이전트 구성 예시 - Vector로만 전송
# Fluent Bit 구성 예시
[OUTPUT]
    Name        http
    Match       *
    Host        vector-aggregator.example.com
    Port        8080
    URI         /
    Format      json
    tls         On
    tls.verify  On
```

---

## 사이징 및 용량 계획 (Sizing)

Vector 워크로드는 일반적으로 CPU 집약적 입니다.

### 기준 메트릭

| 데이터 유형 | 처리량 |
|------------|--------|
| 비구조화 로그 | ~10 MiB/s/vCPU |
| 구조화 로그 및 메트릭 | ~25 MiB/s/vCPU |
| 트레이스 스팬 | ~25 MiB/s/vCPU |

### 인스턴스 권장 사항

최소 요구 사항:
- 인스턴스당 최소 8 vCPU 및 16 GiB 메모리

클라우드 제공자별 권장 인스턴스:

| 클라우드 | 권장 인스턴스 |
|----------|---------------|
| AWS | c6i.2xlarge 또는 c6g.2xlarge |
| Azure | f8 또는 D8plds_v6 |
| GCP | c2 (8 vCPU, 16 GiB) |

역할별 최소 요구 사항:

| 역할 | 최소 vCPU |
|------|-----------|
| 에이전트 역할 | 2 vCPU |
| 애그리게이터 역할 | 4 vCPU |

### 메모리 및 CPU 전략

- ARM64 아키텍처 가 일반적으로 더 나은 성능 투자를 제공
- 일반적인 시작점으로 vCPU당 2 GiB 메모리 권장
- 인메모리 배칭으로 인해 싱크 수에 따라 메모리 사용량이 스케일됨

### 디스크 구성

디스크 버퍼의 경우:
- 10분 분량의 데이터 용량 프로비저닝
- 적절한 여유를 위해 처리량을 예상 최대 처리량의 최소 2배 로 구성

비용 예시:
48 GiB 디스크 공간은 AWS EBS io2에서 약 $0.20/일 비용

```yaml
# 디스크 버퍼 구성 예시
sinks:
  elasticsearch:
    type: elasticsearch
    inputs:
      - transform_id
    endpoints:
      - "https://elasticsearch.example.com:9200"
    buffer:
      type: disk
      max_size: 53687091200  # 50 GiB (10분 @ 100 MiB/s)
      when_full: block
```

### 스케일링 접근 방식

#### 수직 스케일링

- Vector는 사용 가능한 vCPU를 자동으로 활용
- 고가용성 복원력을 위해 인스턴스를 총 볼륨의 33% 이하 로 제한

#### 수평 스케일링

- 로드 밸런서 배포
- Keep-alive 활성화
- aggregate/dedupe transform에 스테이트풀 라우팅 사용
- 다른 시나리오에는 라운드 로빈 사용

```yaml
# Kubernetes 서비스 예시 - 로드 밸런싱
apiVersion: v1
kind: Service
metadata:
  name: vector-aggregator
spec:
  type: LoadBalancer
  selector:
    app: vector-aggregator
  ports:
    - port: 8080
      targetPort: 8080
  sessionAffinity: None
```

### 오토스케일링

권장 설정:
- 5분 동안 평균 85% CPU 사용률 을 목표로 설정
- 일치하는 안정화 기간 설정
- CPU 사용률이 가장 강력한 스케일링 신호

```yaml
# Kubernetes HPA 예시
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: vector-aggregator
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: vector-aggregator
  minReplicas: 3
  maxReplicas: 20
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 85
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
        - type: Percent
          value: 10
          periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 300
      policies:
        - type: Percent
          value: 100
          periodSeconds: 60
```

### 실제 사례

#### 시나리오 1: 비구조화 로그 10 TiB/일

요구 사항:
- 10 TiB/일 = ~119 MiB/s
- 비구조화 로그 처리량: ~10 MiB/s/vCPU
- 필요 vCPU: 119 / 10 = 약 12 vCPU

권장 구성:
- 인스턴스: 3 x c6g.xlarge (각 4 vCPU)
- 예상 비용: 약 $6.20/일

#### 시나리오 2: 트레이싱 데이터 25 TiB/일

요구 사항:
- 25 TiB/일 = ~298 MiB/s
- 트레이스 처리량: ~25 MiB/s/vCPU
- 필요 vCPU: 298 / 25 = 약 12 vCPU

권장 구성:
- 인스턴스: 4 x c2 (GCP)
- 예상 비용: 약 $10.49/일

---

## 참고 자료

- [Vector 공식 문서](https://vector.dev/docs/)
- [Going to Production](https://vector.dev/docs/setup/going-to-prod/)
- [Architecting](https://vector.dev/docs/setup/going-to-prod/architecting/)
- [Hardening](https://vector.dev/docs/setup/going-to-prod/hardening/)
- [High Availability](https://vector.dev/docs/setup/going-to-prod/high-availability/)
- [Rollout](https://vector.dev/docs/setup/going-to-prod/rollout/)
- [Sizing](https://vector.dev/docs/setup/going-to-prod/sizing/)
- [Reference Architectures](https://vector.dev/docs/setup/going-to-prod/arch/)
- [Aggregator Architecture](https://vector.dev/docs/setup/going-to-prod/arch/aggregator/)
- [Agent Architecture](https://vector.dev/docs/setup/going-to-prod/arch/agent/)
- [Unified Architecture](https://vector.dev/docs/setup/going-to-prod/arch/unified/)

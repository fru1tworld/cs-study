# Loki 컴포넌트

> 원본: https://grafana.com/docs/loki/latest/get-started/components/

---

## 목차

1. [개요](#개요)
2. [쓰기 경로 컴포넌트](#쓰기-경로-컴포넌트)
3. [읽기 경로 컴포넌트](#읽기-경로-컴포넌트)
4. [운영 컴포넌트](#운영-컴포넌트)
5. [실험적 컴포넌트 (Bloom)](#실험적-컴포넌트-bloom)

---

## 개요

Loki는 모듈식 시스템으로, 컴포넌트들이 함께 또는 개별적으로 실행될 수 있습니다. 다음 세 가지 배포 모드 중 하나에서 동작합니다.

- **Single Binary** (`-target=all`): 모든 컴포넌트가 단일 프로세스에서 실행
- **Simple Scalable** (`-target=read|write|backend`): 읽기/쓰기/백엔드 그룹으로 분리
- **Microservices**: 각 컴포넌트를 개별적으로 실행

---

## 쓰기 경로 컴포넌트

### Distributor

**역할**: 클라이언트로부터 들어오는 푸시 요청을 처리합니다.

**주요 기능**:
- 스트림 유효성 검증 (라벨 형식, 타임스탬프, 라인 길이)
- 테넌트별 Rate Limit 적용
- 일관된 해싱(consistent hashing)을 사용하여 Ingester로 라우팅
- 설정 가능한 복제 계수(replication factor, 기본값 3)
- 멀티 테넌시 강제 적용

**스케일링**: 무상태(stateless)이므로 수평 확장이 자유로움

### Ingester

**역할**: 데이터를 장기 스토리지에 영속화합니다.

**주요 기능**:
- 메모리 내 청크 관리
- 오브젝트 스토리지로 청크 플러시(flush)
- 최근 데이터에 대한 쿼리 응답 (Querier가 직접 호출)
- WAL(Write-Ahead Log) 지원으로 충돌 시 데이터 복구

**상태**: 상태가 있는(stateful) 컴포넌트로, 메모리 내 청크 데이터를 보유

**복제**: 동일 데이터가 N개의 Ingester에 복제되어 가용성 보장

---

## 읽기 경로 컴포넌트

### Query Frontend

**역할**: Querier API 엔드포인트를 제공하는 선택적(optional) 서비스입니다.

**주요 기능**:
- **쿼리 분할(Query Splitting)**: 시간 범위가 긴 쿼리를 여러 작은 쿼리로 분할하여 병렬 실행
- **결과 캐싱(Caching)**: 결과를 캐시하여 반복 쿼리 성능 향상
- **기본 큐잉**: Query Scheduler 없이 동작할 때 자체 FIFO 큐 사용
- **재시도(Retries)**: 실패한 쿼리 자동 재시도

**스케일링**: 무상태이며 수평 확장 가능

### Query Scheduler

**역할**: 테넌트별 공정성을 보장하는 고급 큐잉 컴포넌트입니다.

**주요 기능**:
- 테넌트별 큐 분리 (한 테넌트의 큰 쿼리가 다른 테넌트를 차단하지 않음)
- 쿼리 우선순위 관리
- Querier 가용성에 따른 효율적 분배

**참고**: 매우 큰 클러스터에서 사용 권장

### Querier

**역할**: 실제 LogQL 쿼리를 실행합니다.

**주요 기능**:
- LogQL 쿼리 실행
- Ingester에서 최근 데이터 조회
- 오브젝트 스토리지에서 과거 데이터 조회
- 결과 중복 제거(deduplication, 복제된 청크 처리)
- 결과 병합 후 Query Frontend로 반환

**스케일링**: 수평 확장 가능

---

## 운영 컴포넌트

### Index Gateway

**역할**: Shipper 기반 스토어(TSDB/BoltDB)에 대한 메타데이터 쿼리를 처리합니다.

**주요 기능**:
- Querier가 인덱스 파일을 직접 다운로드하지 않고 인덱스 조회 결과만 받을 수 있도록 지원
- 인덱스 파일 캐싱
- 메모리 사용량 감소

### Compactor

**역할**: 인덱스 파일 병합과 보존 기간(retention) 관리를 담당합니다.

**주요 기능**:
- 여러 작은 인덱스 파일을 하나로 병합 (쿼리 성능 향상)
- 보존 정책에 따른 데이터 삭제
- 사용자가 요청한 데이터 삭제(deletion) 처리
- 단일 인스턴스 권장 (분산 모드 옵션도 있음)

### Ruler

**역할**: 룰(rules)과 알림 표현식을 관리하고 평가합니다.

**주요 기능**:
- Recording Rules 평가 (메트릭 생성)
- Alerting Rules 평가 (알림 조건 모니터링)
- Alertmanager로 알림 전송
- 여러 인스턴스 운영 시 일관된 해싱으로 룰 그룹 분산

### Pattern Ingester

**역할**: Drain 알고리즘을 사용하여 로그 패턴을 감지하고 집계합니다. 기본값은 비활성화 상태이며, 설정 파일에서 명시적으로 활성화해야 합니다.

**주요 기능**:
- 유사한 로그 라인을 패턴으로 그룹화
- 로그 폭증 시 디버깅 보조
- Grafana Logs Drilldown 통합 시 자동 패턴 감지

---

## 실험적 컴포넌트 (Bloom)

> **주의**: Bloom 관련 기능은 실험적(experimental)이며, 프로덕션 사용 전 검토 필요

### Bloom Planner

**역할**: 블룸(bloom) 생성 작업을 계획합니다.

### Bloom Builder

**역할**: 로그 메타데이터로부터 블룸 블록을 생성합니다.

### Bloom Gateway

**역할**: 청크 필터링 요청을 처리합니다.

**Bloom 필터의 목적**: 텍스트 검색 성능 향상. 라벨 인덱스만으로는 빠르게 필터링하기 어려운 본문 텍스트 검색에 효과적입니다.

---

## 컴포넌트별 스케일링 특성

| 컴포넌트 | 상태성 | 스케일링 | 비고 |
|---------|-------|---------|------|
| Distributor | Stateless | 수평 확장 | 트래픽 비례 |
| Ingester | Stateful | 수평 확장 | 복제 계수 고려 |
| Query Frontend | Stateless | 수평 확장 | 보통 2-3개 |
| Query Scheduler | Stateless | 수평 확장 | 큰 클러스터에서 사용 |
| Querier | Stateless | 수평 확장 | 동시 쿼리 수에 비례 |
| Index Gateway | Stateless | 수평 확장 | 인덱스 크기에 비례 |
| Compactor | Stateful | 단일 인스턴스 | 보존 관리 |
| Ruler | Stateful | 수평 확장 | 룰 수에 비례 |

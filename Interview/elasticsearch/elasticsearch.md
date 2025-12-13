# Elasticsearch / 엘라스틱서치

> 카테고리: 검색 엔진
> [← 면접 질문 목록으로 돌아가기](../interview.md)

---

## 📌 Elasticsearch 기본 아키텍처

### ES-001
Elasticsearch의 기본 아키텍처와 주요 컴포넌트(Cluster, Node, Index, Document 등)에 대해 설명해주세요.

### ES-002
Elasticsearch에서 인덱스와 도큐먼트의 개념과 관계는 무엇인가요?

### ES-003
Shard와 Replica의 역할 및 차이점은 무엇인가요?

### ES-004
Elasticsearch에서 클러스터와 노드 간의 관계와 역할에 대해 설명해주세요.

---

## 📌 Elasticsearch Query DSL

### ES-005
Query DSL의 기본 구조와 사용 방법에 대해 설명해주세요.

### ES-006
Match 쿼리와 Term 쿼리의 차이점은 무엇인가요?

### ES-007
Range 쿼리의 활용 사례와 주의사항에 대해 설명해주세요.

### ES-008
Bool 쿼리의 구성 요소(Must, Should, Must Not, Filter)에 대해 설명해주세요.

---

## 📌 Elasticsearch Aggregation

### ES-009
Aggregation의 개념과 Bucket Aggregation, Metric Aggregation의 차이에 대해 설명해주세요.

---

## 📌 Elasticsearch 분석기

### ES-010
Analyzers, Tokenizers, Filters의 역할과 설정 방법에 대해 설명해주세요.

---

## 📌 Elasticsearch Mapping

### ES-011
Mapping의 개념과 동적 매핑(Dynamic Mapping) 및 명시적 매핑(Explicit Mapping)의 차이점은 무엇인가요?

---

## 📌 Elasticsearch 검색 점수

### ES-012
Elasticsearch에서 Relevance Scoring의 원리와 개선 방법에 대해 설명해주세요.

### ES-013
Boosting을 통한 검색 결과 가중치 조정 방법에 대해 설명해주세요.

### ES-014
Multi-match 쿼리와 Cross-field 검색의 차이점은 무엇인가요?

---

## 📌 Elasticsearch 데이터 타입

### ES-015
Nested 타입과 Object 타입의 차이점 및 사용 시 주의사항에 대해 설명해주세요.

---

## 📌 Elasticsearch 인덱스 설정

### ES-016
Elasticsearch의 인덱스 설정(Index Settings)과 매핑 설정(Mapping Settings)의 차이점은 무엇인가요?

---

## 📌 Elasticsearch 성능 튜닝

### ES-017
검색 성능 튜닝을 위한 주요 고려사항은 무엇인가요?

---

## 📌 Elasticsearch 분산 시스템

### ES-018
Elasticsearch의 분산 시스템 특성과 데이터 복제 메커니즘에 대해 설명해주세요.

---

## 📌 Elasticsearch 모니터링

### ES-019
클러스터 상태를 모니터링하기 위한 도구와 주요 지표에는 무엇이 있나요?

### ES-020
Replica가 부족할 때 발생할 수 있는 문제와 해결 방법은 무엇인가요?

---

## 📌 Elasticsearch 노드 유형

### ES-021
Data Node, Master Node, Client Node의 역할과 차이점에 대해 설명해주세요.

---

## 📌 Elasticsearch와 Kibana

### ES-022
Kibana와 Elasticsearch의 관계 및 연동 방법에 대해 설명해주세요.

---

## 📌 Elasticsearch 확장성

### ES-023
Elasticsearch의 스케일링(Scale-out) 전략에는 어떤 것들이 있나요?

---

## 📌 Elasticsearch 인덱스 템플릿

### ES-024
인덱스 템플릿(Index Template)의 역할과 구성 방법에 대해 설명해주세요.

### ES-025
인덱스 롤오버(Rollover) 전략과 사용 사례에 대해 설명해주세요.

---

## 📌 Elasticsearch Suggester

### ES-026
Suggester 기능의 동작 원리와 활용 방법은 무엇인가요?

---

## 📌 Elasticsearch 페이징

### ES-027
Scroll API와 Search After의 차이점 및 각각의 사용 시나리오는 무엇인가요?

---

## 📌 Elasticsearch Time-based Index

### ES-028
Time-based index의 개념과 활용 방안에 대해 설명해주세요.

---

## 📌 Elasticsearch 데이터 삭제

### ES-029
데이터 삭제 시 발생할 수 있는 이슈와 그 해결 방법은 무엇인가요?

---

## 📌 Elasticsearch 백업

### ES-030
Snapshot과 Restore 기능을 통한 백업 전략에 대해 설명해주세요.

---

## 📌 Elasticsearch 트랜잭션

### ES-031
분산 트랜잭션과 관련하여 Elasticsearch는 어떤 접근 방식을 취하나요?

---

## 📌 Elasticsearch 버전 업그레이드

### ES-032
Elasticsearch 버전 업그레이드 시 고려해야 할 사항은 무엇인가요?

---

## 📌 Elasticsearch ILM

### ES-033
Index Lifecycle Management(ILM)의 기능과 필요성에 대해 설명해주세요.

---

## 📌 Elasticsearch 데이터 수집

### ES-034
Logstash, Beats 등과의 연동을 통한 데이터 수집 및 처리 방식에 대해 설명해주세요.

---

## 📌 Elasticsearch 보안

### ES-035
Elasticsearch의 보안 기능(예: X-Pack Security)과 설정 방법에 대해 설명해주세요.

### ES-036
Role-Based Access Control(RBAC)과 Document Level Security의 차이에 대해 설명해주세요.

---

## 📌 Elasticsearch 커스텀 분석기

### ES-037
커스텀 분석기(Custom Analyzer)를 생성하고 적용하는 방법은 무엇인가요?

---

## 📌 Elasticsearch 멀티테넌시

### ES-038
멀티테넌시를 지원하기 위한 Elasticsearch의 접근 방식은 무엇인가요?

---

## 📌 Elasticsearch Bulk API

### ES-039
Bulk API 사용 시 성능 최적화 및 주의사항은 무엇인가요?

---

## 📌 Elasticsearch 성능 지표

### ES-040
Latency와 Throughput 튜닝을 위한 Elasticsearch 설정 방법에 대해 설명해주세요.

---

## 📌 Elasticsearch 복잡한 검색

### ES-041
복잡한 검색 조건을 구현하기 위한 Query DSL 활용 사례를 설명해주세요.

---

## 📌 Elasticsearch Reindex

### ES-042
Reindex API의 사용 목적과 동작 방식에 대해 설명해주세요.

---

## 📌 Elasticsearch Snapshot Repository

### ES-043
Snapshot Repository 구성 및 관리 방법에 대해 설명해주세요.

---

## 📌 Elasticsearch 일관성

### ES-044
데이터 정합성(consistency) 모델과 Elasticsearch의 eventual consistency 특성에 대해 설명해주세요.

---

## 📌 Elasticsearch 최적화

### ES-045
인덱스 및 도큐먼트 크기 최적화를 위한 전략은 무엇인가요?

---

## 📌 Elasticsearch 파이프라인

### ES-046
파이프라인(pipeline) 처리 기능과 Ingest Node의 역할에 대해 설명해주세요.

---

## 📌 Elasticsearch Hot-Warm-Cold

### ES-047
Hot-Warm-Cold 아키텍처의 개념과 구현 방법에 대해 설명해주세요.

---

## 📌 Elasticsearch 스크립팅

### ES-048
커스텀 스크립팅의 활용과 성능에 미치는 영향에 대해 설명해주세요.

---

## 📌 Elasticsearch 장애 복구

### ES-049
Elasticsearch 클러스터에서 발생할 수 있는 장애와 복구 전략은 무엇인가요?

---

## 📌 Elasticsearch 최신 기능

### ES-050
최근 Elasticsearch의 업데이트 및 새로운 기능에 대해 알고 있는 내용을 공유해주세요.

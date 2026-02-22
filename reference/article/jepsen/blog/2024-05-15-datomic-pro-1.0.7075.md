# Datomic Pro 1.0.7075

날짜: 2024-05-14

Jepsen이 Nubank와 협력하여 Datomic Pro 1.0.7075를 분석한 결과를 발표했습니다.

## 주요 발견사항

- Datomic Pro의 트랜잭션 간 안전성이 공식 주장보다 강력함을 확인
- "Strong Session Serializable isolation"과 업데이트 트랜잭션에 제한된 "Strong Serializable" 특성 제공
- 트랜잭션 내 작업이 순차적이 아닌 논리적으로 동시에 적용되는 독특한 의미론 사용

## 주의사항

Datomic의 문서와 일치하지만, 개별 트랜잭션 함수 내에서 유지되는 불변 조건이 단일 트랜잭션 내에서 적용될 때 깨질 수 있다는 점을 지적합니다.

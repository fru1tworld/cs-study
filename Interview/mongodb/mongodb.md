# MongoDB 면접 질문

## 기본 개념

### 1. MongoDB란 무엇이며, RDBMS와 어떤 차이가 있나요?

### 2. NoSQL 데이터베이스의 종류와 MongoDB가 속한 유형은 무엇인가요?

### 3. MongoDB를 사용하는 이유와 장단점은 무엇인가요?

### 4. BSON이란 무엇이며, JSON과의 차이점은 무엇인가요?

### 5. Collection과 Document의 개념을 설명해주세요.

### 6. MongoDB의 스키마리스(Schema-less) 특성은 무엇을 의미하나요?

---

## 데이터 모델링

### 7. MongoDB에서 Schema Design의 중요한 원칙은 무엇인가요?

### 8. Embedding(내장)과 Referencing(참조) 방식의 차이와 각각 언제 사용하나요?

### 9. One-to-Many 관계를 MongoDB에서 어떻게 설계하나요?

### 10. Many-to-Many 관계를 MongoDB에서 어떻게 설계하나요?

### 11. Document의 크기 제한은 얼마이며, 큰 데이터는 어떻게 처리하나요?

### 12. GridFS란 무엇이며 언제 사용하나요?

---

## 쿼리 & 인덱싱

### 13. MongoDB의 기본 CRUD 작업을 설명해주세요.

### 14. find()와 findOne()의 차이는 무엇인가요?

### 15. MongoDB의 쿼리 연산자($eq, $gt, $in, $and, $or 등)를 설명해주세요.

### 16. Projection이란 무엇이며 어떻게 사용하나요?

### 17. MongoDB의 인덱스 종류와 각각의 특징을 설명해주세요.

### 18. Compound Index(복합 인덱스)란 무엇이며 언제 사용하나요?

### 19. 인덱스의 순서가 쿼리 성능에 어떤 영향을 미치나요?

### 20. Text Index와 Geospatial Index는 무엇인가요?

### 21. explain()을 사용하여 쿼리 성능을 분석하는 방법은?

### 22. Covered Query란 무엇이며 어떻게 활용하나요?

---

## Aggregation Framework

### 23. Aggregation Pipeline이란 무엇인가요?

### 24. Aggregation의 주요 Stage($match, $group, $project, $sort 등)를 설명해주세요.

### 25. $lookup을 사용한 Join 작업을 설명해주세요.

### 26. $unwind는 언제 사용하며 어떤 역할을 하나요?

### 27. $facet을 사용하여 여러 Aggregation을 동시에 실행하는 방법은?

### 28. Aggregation Pipeline과 MapReduce의 차이는 무엇인가요?

---

## Replication

### 29. MongoDB의 Replication이란 무엇인가요?

### 30. Replica Set의 구조와 역할을 설명해주세요.

### 31. Primary, Secondary, Arbiter 노드의 역할은 무엇인가요?

### 32. Read Preference란 무엇이며 종류는 무엇인가요?

### 33. Write Concern이란 무엇이며 어떻게 설정하나요?

### 34. Replica Set의 자동 Failover 과정을 설명해주세요.

### 35. Oplog란 무엇이며 어떤 역할을 하나요?

### 36. Secondary 노드에서 읽기 작업을 수행할 때 주의할 점은?

---

## Sharding

### 37. Sharding이란 무엇이며 왜 필요한가요?

### 38. MongoDB의 Sharding 아키텍처(Shard, Config Server, mongos)를 설명해주세요.

### 39. Shard Key란 무엇이며 선택 시 고려사항은?

### 40. Range-based Sharding과 Hash-based Sharding의 차이는?

### 41. Zone Sharding이란 무엇인가요?

### 42. Chunk란 무엇이며 어떻게 분할되나요?

### 43. Balancer의 역할은 무엇인가요?

### 44. Sharding 환경에서 발생할 수 있는 문제점과 해결 방법은?

---

## Transaction & Consistency

### 45. MongoDB의 Transaction 지원에 대해 설명해주세요.

### 46. Multi-Document Transaction은 언제 사용하나요?

### 47. MongoDB의 ACID 특성을 설명해주세요.

### 48. Read Isolation과 Snapshot Isolation에 대해 설명해주세요.

### 49. Atomicity는 Document 레벨과 Multi-Document 레벨에서 어떻게 다르게 동작하나요?

### 50. Eventual Consistency란 무엇이며 MongoDB에서 어떻게 관리되나요?

---

## 성능 최적화

### 51. MongoDB 쿼리 성능을 개선하는 방법은?

### 52. Working Set이란 무엇이며 메모리와의 관계는?

### 53. Connection Pool의 개념과 적절한 크기 설정 방법은?

### 54. WiredTiger Storage Engine의 특징은 무엇인가요?

### 55. Cache 크기 설정과 메모리 관리 전략은?

### 56. Profiler를 사용하여 느린 쿼리를 찾는 방법은?

### 57. Bulk Write Operation의 장점과 사용 방법은?

### 58. Read/Write 성능을 향상시키기 위한 Best Practice는?

---

## 보안 & 관리

### 59. MongoDB의 인증과 권한 관리 방법은?

### 60. Role-Based Access Control(RBAC)이란 무엇인가요?

### 61. MongoDB에서 데이터 암호화 방법은?

### 62. Backup과 Restore 전략을 설명해주세요.

### 63. mongodump와 mongorestore의 차이점과 사용 방법은?

### 64. Point-in-Time Recovery가 가능한가요?

### 65. MongoDB 모니터링 시 중요한 메트릭은 무엇인가요?

---

## 고급 주제

### 66. Change Streams란 무엇이며 어떻게 활용하나요?

### 67. Time Series Collection은 무엇이며 언제 사용하나요?

### 68. Capped Collection의 특징과 사용 사례는?

### 69. TTL Index를 사용한 자동 데이터 만료 처리 방법은?

### 70. MongoDB Atlas의 주요 기능과 장점은?

### 71. MongoDB Compass란 무엇인가요?

### 72. MongoDB와 Elasticsearch를 함께 사용하는 아키텍처는?

### 73. MongoDB와 Redis를 함께 사용하는 캐싱 전략은?

### 74. CDC(Change Data Capture)를 MongoDB에서 구현하는 방법은?

### 75. MongoDB의 버전별 주요 변경사항과 개선점은?

---

## 실전 시나리오

### 76. 대량의 데이터 마이그레이션 시 고려사항은?

### 77. Hot Shard 문제를 어떻게 해결하나요?

### 78. N+1 문제가 MongoDB에서도 발생하나요? 해결 방법은?

### 79. 실시간 분석을 위한 MongoDB 설계 방법은?

### 80. Multi-tenancy 아키텍처를 MongoDB로 구현하는 방법은?

---

⬅️ [면접 질문 목록으로 돌아가기](../interview.md)

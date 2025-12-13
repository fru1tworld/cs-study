# 📚 CS 모의 면접 질문 목록

fru1tworld의 CS 학습 정리를 위한 모의 면접 질문 리스트입니다.

다양한 CS 및 기술 스택에 대해서 학습할 수 있습니다.

다만 제가 학습을 하기 위해 정리한 레포지토리이므로 활용하신다면 참고만 해주세요.

---

## 📂 카테고리별 질문 목록

### 1️⃣ Computer Science 기초

#### 자료구조 (Data Structure)

📄 [자료구조 질문 보기](./CS/DS/ds.md)

- 스택, 큐, 해시, 트리, 힙, 그래프
- 정렬 알고리즘
- MST, Thread Safe
- 이진탐색, 그리디, 동적계획법

#### 컴퓨터 구조 (Computer Architecture)

📄 [컴퓨터 구조 질문 보기](./CS/Architecture/architecture.md)

- CPU 구조, 파이프라이닝
- 메모리 계층, 캐시 메모리
- 가상 메모리, TLB
- 멀티코어, 병렬 처리
- I/O 시스템, 성능 최적화

#### 네트워크 (Network)

📄 [네트워크 질문 보기](./CS/Network/network.md)

- HTTP/HTTPS, 쿠키/세션
- TCP/UDP, OSI 7계층
- DNS, DHCP, IP 주소
- 3-Way/4-Way Handshake
- 로드밸런서, CORS, SOP

#### 데이터베이스 (Database)

📄 [데이터베이스 질문 보기](./CS/DB/db.md)

- Key, RDB vs NoSQL
- 트랜잭션, ACID, 격리 레벨
- 인덱스, B-Tree/B+Tree
- JOIN, 정규화
- 락(Lock), 레플리케이션, 샤딩

#### 운영체제 (Operating System)

📄 [운영체제 질문 보기](./CS/OS/os.md)

- 시스템 콜, 인터럽트
- 프로세스, 스레드, PCB
- CPU 스케줄링, 컨텍스트 스위칭
- 동기화, 뮤텍스, 세마포어, Deadlock
- 가상 메모리, 페이징, TLB
- 캐시 메모리, 파일 시스템

#### 개발 상식 및 기타

📄 [개발 상식 및 기타 질문 보기](./CS/etc/etc.md)

- 가상화, Docker, CI/CD
- 객체지향, SOLID, 디자인 패턴
- 함수형 프로그래밍, 순수함수
- MVC 패턴, GC
- 인증/인가, OAuth, JWT
- Git, 암호화, 인코딩

#### 시스템 설계 (System Design)

📄 [시스템 설계 질문 보기](./system_design/system_design.md)

- 이벤트, 메시지, EDA
- 분산 트랜잭션, SAGA, 이벤트 소싱
- CQRS
- 데이터베이스 샤딩
- CAP 이론, Consensus
- 레플리케이션, 리더십
- MSA, API 게이트웨이, 서비스 메시

---

### 2️⃣ 프로그래밍 언어

📄 [프로그래밍 언어 질문 보기](./PL/pl.md)

#### Java

- JVM, GC, 메모리 구조
- Collection Framework
- 동기화, Thread, Executor
- Stream API, Optional
- 리플렉션, Annotation

#### JavaScript / TypeScript

- 실행 컨텍스트, 클로저, this
- Promise, async/await, Event Loop
- TypeScript 타입 시스템
- 제네릭, 유틸리티 타입

#### Python

- GIL, 메모리 관리
- 데코레이터, 제너레이터
- 동시성 처리 (Threading, Multiprocessing, Asyncio)

#### Go

- 고루틴, 채널
- 인터페이스, 슬라이스
- defer, panic, recover

---

### 3️⃣ 프레임워크 & 라이브러리

#### Spring / Spring Boot

📄 [Spring 질문 보기](./spring/spring.md)

- IoC, DI, Bean 생성 주기
- AOP, Interceptor, Filter
- DispatcherServlet, @Transactional
- JPA, N+1 문제
- Spring Security, Spring Cloud

#### NestJS

📄 [NestJS 질문 보기](./nest/nest.md)

- 모듈 시스템, Dependency Injection
- Controller, Service, Provider
- Middleware, Interceptor, Guard, Pipe
- Exception Filter
- WebSocket, GraphQL, Microservices

---

### 4️⃣ 데이터베이스 & 캐시

#### Redis

📄 [Redis 질문 보기](./redis/redis.md)

- 데이터 타입, Persistence (RDB, AOF)
- Pub/Sub, 트랜잭션
- Redis Cluster, Sentinel
- 캐시 전략, Eviction 정책

#### Elasticsearch

📄 [Elasticsearch 질문 보기](./elasticsearch/elasticsearch.md)

- 아키텍처, Shard, Replica
- Query DSL, Aggregation
- Mapping, Analyzer
- 인덱스 관리, ILM
- 성능 튜닝

#### MongoDB

📄 [MongoDB 질문 보기](./mongodb/mongodb.md)

- NoSQL vs SQL, 문서 지향 데이터베이스
- BSON, Collection, Document
- 인덱싱, Compound Index
- Aggregation Pipeline
- Replication, Replica Set
- Sharding, 분산 처리
- Transaction, ACID
- 성능 최적화, Schema Design

---

### 5️⃣ 메시징 & 이벤트 스트리밍

#### Kafka

📄 [Kafka 질문 보기](./kafka/kafka.md)

- 아키텍처, Producer, Consumer, Broker
- Partition, Offset, Consumer Group
- 리플리케이션, ISR
- Exactly-Once Semantics
- Kafka Streams, Kafka Connect
- 성능 튜닝, 모니터링

---

### 6️⃣ 실시간 통신 & 동기화

#### WebSocket

📄 [WebSocket 질문 보기](./websocket/websocket.md)

- WebSocket vs HTTP
- Handshake, 메시지 프레이밍
- Ping/Pong, 재연결
- 보안, 부하 분산

#### CRDT (Yjs)

📄 [CRDT 질문 보기](./crdt/crdt.md)

- CRDT 개념, Yjs
- 분산 환경 동기화
- CRDT vs OT
- Awareness, Delta 업데이트

---

### 7️⃣ 데이터 동기화

#### CDC (Debezium)

📄 [CDC/Debezium 질문 보기](./cdc/debezium.md)

- CDC 개념, Debezium 작동 원리
- MySQL binlog, 스키마 변경
- Kafka Connect 연동
- 데이터 일관성, 장애 복구

---

## 📊 통계

-  총 카테고리: 18개
-  총 질문 파일: 18개
-  예상 총 질문 수: 약 1,025개 이상

---

## 💡 활용 방법

1. 관심있는 카테고리의 질문 파일을 클릭합니다
2. 각 질문에 대해 스스로 답변을 작성해봅니다
3. 모르는 내용은 학습 후 다시 도전합니다
4. 실제 면접처럼 구두로 설명하는 연습을 합니다

---

⬅️ [README로 돌아가기](../readme.md)

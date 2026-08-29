# Shardcake 아키텍처

> 원본: https://devsisters.github.io/shardcake/docs/architecture.html

---

먼저 [시작하기](01_getting_started.md#1-shardcake란)와 [용어](01_getting_started.md#3-용어terminology)를 읽고 올 것을 권장.

## 목차

1. [핵심 개념(Main Concepts)](#1-핵심-개념main-concepts)
2. [상세 흐름(Detailed Flows)](#2-상세-흐름detailed-flows)
   - [파드 시작(`Sharding#register`)](#파드-시작shardingregister)
   - [파드 중지(`Sharding#unregister`)](#파드-중지shardingunregister)
   - [엔티티에 메시지 보내기(`Messenger#send`)](#엔티티에-메시지-보내기messengersend)
3. [리밸런스 알고리즘(Rebalance Algorithm)](#3-리밸런스-알고리즘rebalance-algorithm)
4. [할당 알고리즘(Assignment Algorithm)](#4-할당-알고리즘assignment-algorithm)
5. [참고 자료](#5-참고-자료)

---

## 1. 핵심 개념(Main Concepts)

![architecture diagram](https://devsisters.github.io/shardcake/arch2.png)

- **Shard Manager**: 파드와 샤드의 할당 관계를 유지하는 일을 담당하는 단일 노드. 어느 시점에나 Shard Manager는 하나만 살아 있어야 함.
- 파드는 시작할 때 Shard Manager에 **등록**(register)하고, 중지할 때 **등록 해제**(unregister).
- 파드는 Storage 계층에서 샤드 할당 정보를 **읽어**(갱신분 포함) 로컬에 캐시. Shard Manager는 파드에 샤드를 **할당**(assign)하거나 **할당 해제**(unassign)할 때 해당 파드에 직접 통지.
- 파드가 특정 엔티티에 **메시지를 보낼** 때는 그 엔티티가 어느 샤드에 속하는지 확인 → 해당 샤드를 담당하는 파드로 메시지 전달. 그 파드는 엔티티가 아직 시작되지 않았다면 로컬에서 엔티티 동작을 시작.
- 엔티티에 할당되는 **샤드 ID**(Shard ID)는 다음과 같이 계산: `shardId = abs(entityId.hashCode % numberOfShards) + 1`. 즉 1부터 샤드 개수 사이의 안정적인(stable) 숫자.
- 파드가 **응답하지 않으면(unresponsive)** 다른 파드들이 Shard Manager에 통지 → Shard Manager는 Health API로 그 파드가 여전히 살아 있는지 확인. 파드가 살아 있지 않을 때만 그 파드의 샤드를 할당 해제
  - 살아 있는 동안에는 샤드 재할당 불가 → 재할당 시 하나의 엔티티가 서로 다른 두 파드에서 동시에 살아 있을 수 있기 때문

> **단일 장애점이 없음**
>
> Shard Manager는 사실 파드가 추가되거나 제거될 때만 관여.
>
> 그 외에는 아무 일도 하지 않음 → 파드가 샤드 할당본을 캐시해 두고 서로 직접 통신하기 때문. 따라서 Shard Manager가 다운되어도 파드는 계속 동작.

---

## 2. 상세 흐름(Detailed Flows)

### 파드 시작(`Sharding#register`)

1. 파드가 Storage 계층에서 샤드 할당 정보를 가져옴
2. 파드가 샤딩 API를 노출(예: gRPC 서버 시작) → Shard Manager와 다른 파드가 이 파드에 연락 가능
3. 파드가 Shard Manager의 `register` 엔드포인트를 호출 → 결국 리밸런스(rebalance) 촉발 → 일부 샤드가 이 파드에 할당될 수 있음. 그 경우 Shard Manager는 이 파드의 `assign` API를 호출

### 파드 중지(`Sharding#unregister`)

1. 파드가 로컬에서 새 엔티티가 시작되지 못하게 차단. 다른 파드에서 받은 메시지에는 `EntityNotManagedByThisPod` 오류를 반환하며, 이 메시지는 재시도됨
2. 파드가 로컬 엔티티 전체에 종료 메시지(termination message)를 보내 중지. 현재 처리 중인 메시지는 끝까지 처리
3. 모든 엔티티가 중지되면 파드가 Shard Manager의 `unregister` 엔드포인트를 호출 → 즉시 리밸런스 촉발 → 이 파드가 담당하던 샤드가 다른 파드로 할당됨
4. 파드가 샤딩 API를 중지하고 종료 준비 완료

### 엔티티에 메시지 보내기(`Messenger#send`)

1. Pod1이 특정 엔티티에 메시지 전송 필요
2. Pod1이 그 엔티티의 샤드 ID를 계산
3. Pod1이 로컬 샤드 할당 캐시에서 이 엔티티를 담당하는 파드가 어디인지 확인(Pod2). Pod1 == Pod2라면 다음 단계는 생략
4. Pod1이 Pod2 샤딩 API의 `sendMessage` 엔드포인트를 호출
5. Pod2가 메시지를 자신의 `EntityManager`로 전달
6. Pod2의 `EntityManager`가 그 엔티티의 샤드 ID를 계산 → 그 샤드 ID를 로컬에서 처리해야 하는지 확인. 아니면 `EntityNotManagedByThisPod` 오류를 반환
7. Pod2가 엔티티를 아직 시작하지 않았다면 시작 후 메시지 전달
8. 응답이 만들어지면 그 응답을 Pod1로 반환

> **응답하지 않는 파드**
>
> Pod2가 응답하지 않으면 Pod1은 Shard Manager의 `notifyUnhealthyPod` 엔드포인트를 호출. Shard Manager는 Health API(예: Kubernetes)로 그 파드가 여전히 살아 있는지 확인 → 아니면 등록 해제. Pod1은 계속 재시도하며 곧 새 파드가 할당되기를 기대.

---

## 3. 리밸런스 알고리즘(Rebalance Algorithm)

리밸런스 과정은 파드에 샤드를 할당하거나 할당 해제하는 작업. 다음 경우에 촉발.

- 첫 번째 파드가 등록될 때(모든 샤드가 그 파드에 할당됨)
- 파드가 등록 해제될 때
- `rebalanceInterval`로 정의된 일정한 주기마다(자세한 내용은 [설정](03_configuration.md#2-shard-manager-설정) 참고)

이 워크플로는 하나의 샤드가 결코 서로 다른 두 파드에 동시에 할당되지 않도록 설계.

1. Shard Manager가 어떤 샤드를 할당하고 할당 해제할지 결정([할당 알고리즘](#4-할당-알고리즘assignment-algorithm) 참고)
2. Shard Manager가 할당과 할당 해제에 관련된 모든 파드에 핑(ping)을 보내 응답 여부 확인(기본 타임아웃 3초, `pingTimeout`으로 정의. [설정](03_configuration.md#2-shard-manager-설정) 참고)
   - 죽은 노드 때문에 리밸런스가 느려지지 않도록 하는 단계
   - 응답하지 않는 파드는 할당과 할당 해제 대상에서 제외
   - 핑은 병렬로 수행
3. Shard Manager가 할당 해제할 샤드가 있는 모든 파드의 `unassign` 엔드포인트를 병렬로 호출. `unassign` 성공 = 해당 샤드의 로컬 엔티티가 모두 중지되어 재할당 준비 완료
4. 할당 해제에 실패한 샤드는 할당 목록에서 제거
5. Shard Manager가 할당할 샤드가 있는 모든 파드의 `assign` 엔드포인트를 병렬로 호출
6. 핑, 할당 해제, 할당에 실패한 파드가 있으면 Health API로 그 파드들이 여전히 살아 있는지 확인. 살아 있지 않으면 등록 해제 → 곧바로 또 다른 리밸런스 촉발
7. Shard Manager가 새 할당 정보를 선택된 Storage에 영속화 → 파드들은 변경 사항을 통지받음
8. 무언가 실패하면 `rebalanceRetryInterval`로 정의된 재시도 간격 후에 또 다른 리밸런스 촉발([설정](03_configuration.md#2-shard-manager-설정) 참고)

---

## 4. 할당 알고리즘(Assignment Algorithm)

할당 알고리즘은 각 파드에 같은 수의 샤드를 배분하는 것이 목표. 또한 파드 버전을 고려 → 롤링 업데이트(rolling update) 중에는 할당 횟수를 줄이기 위해 "새" 파드에 우선적으로 샤드를 할당.

1. Shard Manager가 파드당 평균 샤드 수를 계산(`샤드 수 / 파드 수`)
2. 각 파드에 대해 그 파드에 할당된 샤드 수가 평균보다 많으면 그 차이만큼의 샤드를 무작위로 선택 → 이를 `extraShardsToAllocate`라 함. 일부 파드의 버전이 서로 다르면 `extraShardsToAllocate`는 빈 값으로 설정(롤링 업데이트 도중 → 리밸런스를 원하지 않음)
3. 리밸런스할 샤드 = 할당되지 않은 샤드 + `extraShardsToAllocate`. 할당되지 않은 샤드를 먼저, 그다음 샤드가 가장 많은 파드의 샤드를, 그다음 오래된 파드의 샤드를 처리하도록 정렬
4. 리밸런스할 각 샤드에 대해
   1. 샤드가 가장 적은 파드를 탐색. 최신 버전이 아닌 파드는 이 탐색에서 제외(곧 중지될 오래된 파드에는 샤드를 할당하지 않으려는 목적)
   2. 그 파드가 이미 그 샤드가 할당된 파드라면 할당을 만들지 않음
   3. 그 파드가 현재 할당된 파드보다 샤드가 딱 1개만 적다면 할당을 만들지 않음(차이가 1개뿐이면 리밸런스할 가치 없음)
   4. 그 밖의 경우, 그 샤드를 그 파드에 할당하는 새 할당을 생성하고 이전 파드(있다면)에서 그 샤드를 할당 해제

---

## 5. 참고 자료

- [Shardcake 공식 문서 — Architecture](https://devsisters.github.io/shardcake/docs/architecture.html)
- [설정(Configuration)](03_configuration.md)
- [Shardcake GitHub 저장소](https://github.com/devsisters/shardcake)

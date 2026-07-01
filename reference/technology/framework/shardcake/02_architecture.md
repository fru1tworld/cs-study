# Shardcake 아키텍처

> 원본: https://devsisters.github.io/shardcake/docs/architecture.html

---

먼저 [시작하기](01_getting_started.md#1-shardcake란)와 [용어](01_getting_started.md#3-용어terminology)를 읽고 오는 것이 좋습니다.

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

- **Shard Manager**는 파드와 샤드의 할당 관계를 유지하는 일을 담당하는 단일 노드입니다. 어느 시점에나 Shard Manager는 하나만 살아 있어야 합니다.
- 파드는 시작할 때 Shard Manager에 **등록**(register)하고, 중지할 때 **등록 해제**(unregister)합니다.
- 파드는 Storage 계층에서 샤드 할당 정보를 **읽어**(갱신분 포함) 로컬에 캐시합니다. Shard Manager는 파드에 샤드를 **할당**(assign)하거나 **할당 해제**(unassign)할 때 해당 파드에 직접 알립니다.
- 파드가 특정 엔티티에 **메시지를 보낼** 때는, 그 엔티티가 어느 샤드에 속하는지 확인하고 해당 샤드를 담당하는 파드로 메시지를 넘깁니다. 그 파드는 엔티티가 아직 시작되지 않았다면 로컬에서 엔티티 동작을 시작합니다.
- 엔티티에 할당되는 **샤드 ID**(Shard ID)는 다음과 같이 계산됩니다. `shardId = abs(entityId.hashCode % numberOfShards) + 1`. 즉, 1부터 샤드 개수 사이의 안정적인(stable) 숫자입니다.
- 파드가 **응답하지 않으면(unresponsive)** 다른 파드들이 Shard Manager에 알리고, Shard Manager는 Health API로 그 파드가 여전히 살아 있는지 확인합니다. 파드가 살아 있지 않을 때만 그 파드의 샤드를 할당 해제합니다(살아 있는 동안에는 샤드를 재할당할 수 없습니다. 그렇게 하면 하나의 엔티티가 서로 다른 두 파드에서 동시에 살아 있을 수 있기 때문입니다).

> **💡 단일 장애점이 없음**
>
> Shard Manager는 사실 파드가 추가되거나 제거될 때만 관여합니다.
>
> 그 외에는 아무 일도 하지 않습니다. 파드가 샤드 할당본을 캐시해 두고 서로 직접 통신하기 때문입니다. 따라서 Shard Manager가 다운되어도 파드는 계속 동작합니다.

---

## 2. 상세 흐름(Detailed Flows)

### 파드 시작(`Sharding#register`)

1. 파드가 Storage 계층에서 샤드 할당 정보를 가져옵니다.
2. 파드가 샤딩 API를 노출합니다(예: gRPC 서버 시작). 그러면 Shard Manager와 다른 파드가 이 파드에 연락할 수 있습니다.
3. 파드가 Shard Manager의 `register` 엔드포인트를 호출합니다. 이는 결국 리밸런스(rebalance)를 촉발하며, 일부 샤드가 이 파드에 할당될 수 있습니다. 그 경우 Shard Manager는 이 파드의 `assign` API를 호출합니다.

### 파드 중지(`Sharding#unregister`)

1. 파드가 로컬에서 새 엔티티가 시작되지 못하게 막습니다. 다른 파드에서 받은 메시지에는 `EntityNotManagedByThisPod` 오류를 반환하며, 이 메시지는 재시도됩니다.
2. 파드가 로컬 엔티티 전체에 종료 메시지(termination message)를 보내 중지시킵니다. 현재 처리 중인 메시지는 끝까지 처리됩니다.
3. 모든 엔티티가 중지되면 파드가 Shard Manager의 `unregister` 엔드포인트를 호출합니다. 이는 즉시 리밸런스를 촉발하여, 이 파드가 담당하던 샤드가 다른 파드로 할당됩니다.
4. 파드가 샤딩 API를 중지하고 종료할 준비를 마칩니다.

### 엔티티에 메시지 보내기(`Messenger#send`)

1. Pod1이 특정 엔티티에 메시지를 보내야 합니다.
2. Pod1이 그 엔티티의 샤드 ID를 계산합니다.
3. Pod1이 로컬 샤드 할당 캐시에서 이 엔티티를 담당하는 파드가 어디인지 확인합니다. 그것은 Pod2입니다. 만약 Pod1 == Pod2라면 다음 단계는 건너뜁니다.
4. Pod1이 Pod2 샤딩 API의 `sendMessage` 엔드포인트를 호출합니다.
5. Pod2가 메시지를 자신의 `EntityManager`로 넘깁니다.
6. Pod2의 `EntityManager`가 그 엔티티의 샤드 ID를 계산하고, 그 샤드 ID를 로컬에서 처리해야 하는지 확인합니다. 그렇지 않으면 `EntityNotManagedByThisPod` 오류를 반환합니다.
7. Pod2가 엔티티를 아직 시작하지 않았다면 시작하고 메시지를 전달합니다.
8. 응답이 만들어지면 그 응답을 Pod1로 돌려보냅니다.

> **💡 응답하지 않는 파드**
>
> Pod2가 응답하지 않으면 Pod1은 Shard Manager의 `notifyUnhealthyPod` 엔드포인트를 호출합니다. Shard Manager는 Health API(예: Kubernetes)로 그 파드가 여전히 살아 있는지 확인하고, 그렇지 않으면 등록을 해제합니다. Pod1은 계속 재시도하며, 곧 새 파드가 할당되기를 기대합니다.

---

## 3. 리밸런스 알고리즘(Rebalance Algorithm)

리밸런스 과정은 파드에 샤드를 할당하거나 할당 해제하는 작업입니다. 다음 경우에 촉발됩니다.

- 첫 번째 파드가 등록될 때(모든 샤드가 그 파드에 할당됨)
- 파드가 등록 해제될 때
- `rebalanceInterval`로 정의된 일정한 주기마다(자세한 내용은 [설정](03_configuration.md#2-shard-manager-설정) 참고)

이 워크플로는 하나의 샤드가 결코 서로 다른 두 파드에 동시에 할당되지 않도록 설계되어 있습니다.

1. Shard Manager가 어떤 샤드를 할당하고 할당 해제할지 결정합니다([할당 알고리즘](#4-할당-알고리즘assignment-algorithm) 참고).
2. Shard Manager가 할당과 할당 해제에 관련된 모든 파드에 핑(ping)을 보내 응답 여부를 확인합니다(기본 타임아웃 3초, `pingTimeout`으로 정의됨. [설정](03_configuration.md#2-shard-manager-설정) 참고). 죽은 노드 때문에 리밸런스가 느려지지 않도록 하는 단계입니다. 응답하지 않는 파드는 할당과 할당 해제 대상에서 제외합니다. 핑은 병렬로 수행합니다.
3. Shard Manager가 할당 해제할 샤드가 있는 모든 파드의 `unassign` 엔드포인트를 병렬로 호출합니다. `unassign`이 성공했다는 것은 해당 샤드의 로컬 엔티티가 모두 중지되어 재할당할 준비가 되었다는 뜻입니다.
4. 할당 해제에 실패한 샤드는 할당 목록에서 제거됩니다.
5. Shard Manager가 할당할 샤드가 있는 모든 파드의 `assign` 엔드포인트를 병렬로 호출합니다.
6. 핑, 할당 해제, 할당에 실패한 파드가 있으면, Health API로 그 파드들이 여전히 살아 있는지 확인합니다. 살아 있지 않으면 등록을 해제하고 곧바로 또 다른 리밸런스를 촉발합니다.
7. Shard Manager가 새 할당 정보를 선택된 Storage에 영속화하고, 파드들은 변경 사항을 통지받습니다.
8. 무언가 실패하면 `rebalanceRetryInterval`로 정의된 재시도 간격 후에 또 다른 리밸런스가 촉발됩니다([설정](03_configuration.md#2-shard-manager-설정) 참고).

---

## 4. 할당 알고리즘(Assignment Algorithm)

할당 알고리즘은 각 파드에 같은 수의 샤드를 주는 것을 목표로 합니다. 또한 파드 버전을 고려해, 롤링 업데이트(rolling update) 중에는 할당 횟수를 줄이려고 "새" 파드에 우선적으로 샤드를 할당합니다.

1. Shard Manager가 파드당 평균 샤드 수를 계산합니다(`샤드 수 / 파드 수`).
2. 각 파드에 대해, 그 파드에 할당된 샤드 수가 평균보다 많으면 그 차이만큼의 샤드를 무작위로 고릅니다. 이를 `extraShardsToAllocate`라고 합니다. 일부 파드의 버전이 서로 다르면 `extraShardsToAllocate`는 빈 값으로 설정됩니다(롤링 업데이트 도중이라는 뜻이므로 리밸런스를 원하지 않습니다).
3. 리밸런스할 샤드 = 할당되지 않은 샤드 + `extraShardsToAllocate`. 할당되지 않은 샤드를 먼저, 그다음 샤드가 가장 많은 파드의 샤드를, 그다음 오래된 파드의 샤드를 처리하도록 정렬합니다.
4. 리밸런스할 각 샤드에 대해,
   1. 샤드가 가장 적은 파드를 찾습니다. 최신 버전이 아닌 파드는 이 탐색에서 제외됩니다(곧 중지될 오래된 파드에는 샤드를 할당하지 않으려 하기 때문입니다).
   2. 그 파드가 이미 그 샤드가 할당된 파드라면 할당을 만들지 않습니다.
   3. 그 파드가 현재 할당된 파드보다 샤드가 딱 1개만 적다면 할당을 만들지 않습니다(차이가 1개뿐이면 리밸런스할 가치가 없습니다).
   4. 그 밖의 경우, 그 샤드를 그 파드에 할당하는 새 할당을 만들고, 이전 파드(있다면)에서 그 샤드를 할당 해제합니다.

---

## 5. 참고 자료

- [Shardcake 공식 문서 — Architecture](https://devsisters.github.io/shardcake/docs/architecture.html)
- [설정(Configuration)](03_configuration.md)
- [Shardcake GitHub 저장소](https://github.com/devsisters/shardcake)

# Shardcake 설정

> 원본: https://devsisters.github.io/shardcake/docs/config.html

---

## 목차

1. [샤딩 설정(Sharding Configuration)](#1-샤딩-설정sharding-configuration)
2. [Shard Manager 설정](#2-shard-manager-설정)
3. [참고 자료](#3-참고-자료)

---

## 1. 샤딩 설정(Sharding Configuration)

파드 쪽에서 설정할 수 있는 항목은 다음과 같습니다.

- `numberOfShards`: 샤드 개수

> **💡 샤드 개수는 어떻게 정할까?**
>
> `numberOfShards`는 엔티티 ID에서 샤드 ID를 계산하는 데 씁니다. 그래서 모든 파드에서 같은 값이어야 하며, 앱이 실행되는 도중에는 바꿀 수 없습니다.
>
> 이 값이 파드 수보다 작으면 일부 파드는 샤드가 없어(엔티티를 호스팅하지 못해) 비효율적입니다. 값이 너무 작으면 두 파드 간 샤드 1개 차이가 상당히 커져서 파드마다 호스팅하는 엔티티 수가 불균형해집니다. 반대로 샤드가 너무 많으면 파드마다 많은 샤드를 떠안아 불필요한 오버헤드가 생깁니다.
>
> 좋은 경험칙(rule of thumb)은 샤드 개수를 예상되는 최대 파드 수의 10배로 설정하는 것입니다.

- `selfHost`: 현재 파드의 호스트명 또는 IP 주소
- `shardingPort`: 파드끼리 통신하는 데 쓰는 포트
- `shardManagerUri`: Shard Manager GraphQL API의 URL
- `serverVersion`: 현재 파드의 버전

> **💡 버전이란?**
>
> 롤링 업데이트(다운타임 없이 파드를 하나씩 업그레이드)를 할 때는 곧 중지될 파드에 샤드를 할당하지 않는 편이 좋습니다. 이상적으로는 전체 과정에서 각 샤드를 한 번만 옮기고 싶습니다.
>
> `serverVersion`이 있으면 Shard Manager는 어떤 파드가 오래된 것이고 어떤 파드가 새것인지 구분해 새 파드를 골라 샤드를 할당합니다.

- `entityMaxIdleTime`: 이 시간 동안 메시지를 전혀 받지 못하면 엔티티가 중지되는 비활성 시간

> **💡 종료 메시지(Termination Message)**
>
> `registerEntity`는 `terminationMessage`라는 선택적 매개변수를 받습니다. 여기에 엔티티가 중지되기 전에(리밸런스 때문이든 비활성 때문이든) 엔티티에 보낼 메시지를 정의합니다.
>
> 종료 메시지를 지정하지 않으면 엔티티 큐가 그냥 종료됩니다. 하지만 마지막 메시지를 처리한 뒤 엔티티를 "깔끔하게" 중지하고 싶다면, 종료 메시지를 정의하고 그 메시지를 받은 뒤 (`ZIO.interrupt`를 호출해) 직접 동작을 중지하면 됩니다.
>
> 종료 메시지에는 종료가 완료되었음을 알리기 위해 직접 완료(complete)해야 하는 프로미스(promise)가 들어 있어야 합니다. [예제는 여기](https://github.com/devsisters/shardcake/tree/series/2.x/examples/src/main/scala/example/complex)를 참고하세요.

- `entityTerminationTimeout`: 엔티티가 종료 메시지를 처리하도록 주어지는 시간이며, 이 시간이 지나면 인터럽트(interrupt)합니다
- `sendTimeout`: `sendMessage`를 호출할 때의 타임아웃
- `refreshAssignmentsRetryInterval`: 스토리지에서 샤드 할당 정보를 가져오는 데 실패했을 때의 재시도 간격
- `unhealthyPodReportInterval`: 비정상 파드를 Shard Manager에 보고하는 간격(메시지가 실패할 때마다 Shard Manager를 호출하지 않도록 두는 값)
- `simulateRemotePods`: 로컬 샤드에 호스팅된 엔티티에 메시지를 보낼 때의 최적화를 비활성화(모든 메시지의 직렬화를 강제함)

---

## 2. Shard Manager 설정

Shard Manager 쪽에서 설정할 수 있는 항목은 다음과 같습니다.

- `numberOfShards`: 샤드 개수(위 설명 참고)
- `apiPort`: GraphQL API를 노출할 포트
- `rebalanceInterval`: 샤드를 정기적으로 리밸런스하는 간격
- `rebalanceRetryInterval`: 일부 샤드의 리밸런스가 실패했을 때의 재시도 간격
- `pingTimeout`: 파드가 핑 요청에 응답하기를 기다리는 시간
- `persistRetryInterval`: 파드와 샤드 할당 정보를 영속화하는 데 대한 재시도 간격
- `persistRetryCount`: 파드와 샤드 할당 정보 영속화의 최대 재시도 횟수
- `rebalanceRate`: 한 번의 반복(iteration)에서 리밸런스할 샤드의 최대 비율
- `podHealthCheckInterval`: 파드 상태를 확인하는 간격

> **💡 리밸런스 비율(Rebalance Rate)**
>
> 리밸런스 비율은 새 파드에 너무 많은 샤드가 한꺼번에 할당되는 것을 막습니다. 곧바로 완벽한 분배에 도달하는 대신, 여러 번 반복하며 새 파드가 새 샤드를 감당할 여유를 줍니다. 엔티티를 시작하는 데 성능 비용이 드는 경우(예: 상태 로딩)나 한 번에 너무 많은 엔티티를 시작하고 싶지 않을 때 특히 유용합니다.
>
> 파드가 떠날 때는 그 샤드를 즉시 리밸런스해야 하므로 이 비율이 적용되지 않습니다.

---

## 3. 참고 자료

- [Shardcake 공식 문서 — Configuration](https://devsisters.github.io/shardcake/docs/config.html)
- [아키텍처(Architecture)](02_architecture.md)
- [커스터마이징(Customization)](04_customization.md)

# Shardcake 설정

> 원본: https://devsisters.github.io/shardcake/docs/config.html

<a id="1-샤딩-설정sharding-configuration"></a>
## 1. 샤딩 설정(Sharding Configuration)

파드 쪽에서 설정할 수 있는 항목은 다음과 같다.

- `numberOfShards`: 샤드 개수

> **샤드 개수는 어떻게 정할까?**

>

> `numberOfShards`는 엔티티 ID에서 샤드 ID를 계산하는 데 사용한다. 따라서 모든 파드가 같은 값을 사용해야 하며, 앱 실행 도중에는 변경할 수 없다.

>

> 샤드 수가 파드 수보다 작으면 일부 파드는 샤드를 받지 못해 엔티티를 호스팅할 수 없다. 샤드 수가 너무 적어도 파드 간 샤드 하나의 차이가 커져 엔티티 수가 불균형해진다. 반대로 샤드가 너무 많으면 파드마다 관리할 샤드가 늘어나 불필요한 오버헤드가 생긴다.

>

> 경험칙(rule of thumb)으로는 샤드 수를 예상되는 최대 파드 수의 10배로 설정한다.

- `selfHost`: 현재 파드의 호스트명 또는 IP 주소
- `shardingPort`: 파드끼리 통신하는 데 쓰는 포트
- `shardManagerUri`: Shard Manager GraphQL API의 URL
- `serverVersion`: 현재 파드의 버전

> **버전이란?**

>

> 롤링 업데이트는 다운타임 없이 파드를 하나씩 업그레이드하는 과정이다. 곧 중지될 파드에 샤드를 할당하면 다시 이동해야 하므로, 가능한 한 전체 과정에서 각 샤드를 한 번만 옮기는 것이 좋다.

>

> Shard Manager는 `serverVersion`으로 기존 파드와 새 파드를 구분하고, 새 파드를 골라 샤드를 할당한다.

- `entityMaxIdleTime`: 이 시간 동안 메시지를 전혀 받지 못하면 엔티티가 중지되는 비활성 시간

> **종료 메시지(Termination Message)**

>

> `registerEntity`의 선택적 매개변수인 `terminationMessage`에는 엔티티가 중지되기 전에 보낼 메시지를 정의한다. 리밸런스와 비활성 상태로 인한 중지 모두에 해당한다.

>

> 종료 메시지를 지정하지 않으면 엔티티 큐가 바로 종료된다. 마지막 메시지까지 처리한 뒤 중지하려면 종료 메시지를 정의하고, 엔티티가 그 메시지를 받았을 때 `ZIO.interrupt`를 호출해 직접 동작을 중지한다.

>

> 종료 메시지에는 프로미스(promise)가 들어 있어야 한다. 엔티티는 종료를 마친 뒤 이 프로미스를 직접 완료(complete)해 그 사실을 알린다. 구현은 [예제](https://github.com/devsisters/shardcake/tree/series/2.x/examples/src/main/scala/example/complex)를 참고한다.

- `entityTerminationTimeout`: 엔티티가 종료 메시지를 처리하도록 주어지는 시간, 이 시간 경과 시 인터럽트(interrupt)
- `sendTimeout`: `sendMessage`를 호출할 때의 타임아웃
- `refreshAssignmentsRetryInterval`: 스토리지에서 샤드 할당 정보를 가져오는 데 실패했을 때의 재시도 간격
- `unhealthyPodReportInterval`: 비정상 파드를 Shard Manager에 보고하는 간격(메시지가 실패할 때마다 Shard Manager를 호출하지 않도록 두는 값)
- `simulateRemotePods`: 로컬 샤드에 호스팅된 엔티티에 메시지를 보낼 때의 최적화를 비활성화(모든 메시지의 직렬화를 강제함)

<a id="2-shard-manager-설정"></a>

## 2. Shard Manager 설정

Shard Manager 쪽에서 설정할 수 있는 항목은 다음과 같다.

- `numberOfShards`: 샤드 개수(위 설명 참고)
- `apiPort`: GraphQL API를 노출할 포트
- `rebalanceInterval`: 샤드를 정기적으로 리밸런스하는 간격
- `rebalanceRetryInterval`: 일부 샤드의 리밸런스가 실패했을 때의 재시도 간격
- `pingTimeout`: 파드가 핑 요청에 응답하기를 기다리는 시간
- `persistRetryInterval`: 파드와 샤드 할당 정보를 영속화하는 데 대한 재시도 간격
- `persistRetryCount`: 파드와 샤드 할당 정보 영속화의 최대 재시도 횟수
- `rebalanceRate`: 한 번의 반복(iteration)에서 리밸런스할 샤드의 최대 비율
- `podHealthCheckInterval`: 파드 상태를 확인하는 간격

> **리밸런스 비율(Rebalance Rate)**

>

> 새 파드에 샤드를 한꺼번에 몰아주면 엔티티를 시작하는 부하도 집중된다. 리밸런스 비율은 한 번에 옮기는 양을 제한해 여러 차례에 걸쳐 분배하도록 한다. 상태 로딩처럼 엔티티 시작 비용이 크거나, 한꺼번에 많은 엔티티를 시작하지 않아야 할 때 유용하다.

>

> 파드가 떠날 때는 해당 샤드를 즉시 리밸런스해야 하므로 이 비율을 적용하지 않는다.

<a id="3-참고-자료"></a>

## 3. 참고 자료

- [Shardcake 공식 문서: Configuration](https://devsisters.github.io/shardcake/docs/config.html)
- [아키텍처(Architecture)](02_architecture.md)
- [커스터마이징(Customization)](04_customization.md)

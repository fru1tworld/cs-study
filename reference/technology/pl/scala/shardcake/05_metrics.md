# Shardcake 메트릭

> 원본: https://devsisters.github.io/shardcake/docs/metrics.html

<a id="1-메트릭-개요"></a>
## 1. 메트릭 개요

Shardcake는 등록된 파드 수, 샤드 할당 상태, 리밸런스 횟수 등을 메트릭으로 제공한다. 이 값은 [ZIO Metrics](https://zio.dev/reference/observability/metrics/)로 노출되므로 [원하는 백엔드](https://zio.dev/zio-metrics-connectors/)에 연결해 볼 수 있다. 아래에서는 Shard Manager 전체 상태와 개별 파드 상태를 나눠 살펴본다.

<a id="2-shard-manager-메트릭"></a>

## 2. Shard Manager 메트릭

- `shardcake.pods` (게이지): 현재 등록된 파드 수
- `shardcake.shards_assigned` (게이지): 현재 파드에 할당된 샤드 수
- `shardcake.shards_unassigned` (게이지): 현재 어느 파드에도 할당되지 않은 샤드 수
- `shardcake.rebalances` (카운터): 발생한 리밸런스 횟수
- `shardcake.pod_health_checked` (카운터): 파드 상태를 확인한 횟수

<a id="3-파드-메트릭pod-metrics"></a>

## 3. 파드 메트릭(Pod Metrics)

- `shardcake.shards` (게이지): 현재 파드에 할당된 샤드 수
- `shardcake.entities` (게이지): 현재 파드에서 실행 중인 엔티티 수
- `shardcake.singletons` (게이지): 현재 파드에서 실행 중인 싱글턴 수

<a id="4-참고-자료"></a>

## 4. 참고 자료

- [Shardcake 공식 문서: Metrics](https://devsisters.github.io/shardcake/docs/metrics.html)
- [ZIO Metrics](https://zio.dev/reference/observability/metrics/)
- [ZIO Metrics Connectors](https://zio.dev/zio-metrics-connectors/)

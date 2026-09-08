# Shardcake FAQ

> 원본: https://devsisters.github.io/shardcake/faq/

<a id="1-shard-manager는-단일-장애점인가요"></a>
## 1. Shard Manager는 단일 장애점인가요?

Shard Manager는 단일 조율 지점(single point of coordination)이지만 단일 장애점(single point of failure)은 아니다. 파드끼리는 Shard Manager 없이도 통신하며, 실제로 Shard Manager는 99%의 시간 동안 아무 일도 하지 않는다.

Shard Manager가 필요한 때는 새 파드가 시작되거나 기존 파드가 제거되어 샤드를 재할당해야 할 때다. 따라서 실행 중인 파드에 영향을 주지 않고 Shard Manager 파드를 재시작할 수 있다.

<a id="2-롤링-업데이트-중-타임아웃이-발생하는데-어떻게-해야-하나요"></a>

## 2. 롤링 업데이트 중 타임아웃이 발생하는데 어떻게 해야 하나요?

먼저 Shard Manager의 로그에서 롤링 업데이트 중 어떤 일이 일어났는지 확인한다.

자주 확인하는 원인은 중지되는 파드가 Shard Manager에 등록 해제를 알리지 못하는 경우다. 아래처럼 종료 신호 처리, 네트워크, 종료 로직 중 어디에서 막혔는지 구분해 살펴본다.

- 파드가 KILL 시그널을 처리하지 못한 채 갑자기 중지 → 메인 파이버(fiber)가 인터럽트되지 못함
- 파드가 네트워크 연결을 잃어 `unregister` 엔드포인트를 호출하지 못함(예를 들어 Istio 프록시 사용 시)
- 파드가 종료 과정에서 데드락(deadlock)에 빠짐

등록 해제가 막힌 지점을 찾았다면 해당 종료 처리나 네트워크 문제를 해결해야 한다.

<a id="3-shardcake는-akkapekko의-대체재인가요"></a>

## 3. Shardcake는 Akka/Pekko의 대체재인가요?

Shardcake가 다루는 범위는 Akka/Pekko의 기능 중 Cluster Sharding에 해당하므로 전체 대체재는 아니다. ZIO가 Queue, Hub, Promise 같은 로컬 동시성 도구를 제공하면, Shardcake가 여기에 분산 기능을 더한다.

이벤트 소싱(event sourcing)이나 액터 영속화(actor persistence)는 이 범위에 포함되지 않는다. 이런 기능이 필요하다면 ZIO와 Shardcake 위에 직접 구현하는 방식을 권장한다.

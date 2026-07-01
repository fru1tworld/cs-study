# Shardcake FAQ

> 원본: https://devsisters.github.io/shardcake/faq/

---

## 목차

1. [Shard Manager는 단일 장애점인가요?](#1-shard-manager는-단일-장애점인가요)
2. [롤링 업데이트 중 타임아웃이 발생하는데 어떻게 해야 하나요?](#2-롤링-업데이트-중-타임아웃이-발생하는데-어떻게-해야-하나요)
3. [Shardcake는 Akka/Pekko의 대체재인가요?](#3-shardcake는-akkapekko의-대체재인가요)

---

## 1. Shard Manager는 단일 장애점인가요?

그렇지 않습니다. Shard Manager는 단일 조율 지점(single point of coordination)이지 단일 장애점(single point of failure)이 아닙니다. 실제로 99%의 시간 동안 Shard Manager는 아무 일도 하지 않으며, 파드끼리는 Shard Manager 없이도 서로 통신합니다. Shard Manager가 필요한 순간은 새 파드가 시작되거나 기존 파드가 제거될 때뿐이며, 이때 남은 파드로 샤드를 재할당합니다.

실행 중인 파드에 영향을 주지 않고 Shard Manager 파드를 안전하게 재시작할 수 있습니다.

---

## 2. 롤링 업데이트 중 타임아웃이 발생하는데 어떻게 해야 하나요?

먼저 Shard Manager의 로그를 확인해 롤링 업데이트 중 무슨 일이 일어나는지 살펴보길 권장합니다.

흔한 문제는 중지되는 파드가 Shard Manager에서 스스로 등록을 해제하지 못하는 경우입니다. 원인은 다음과 같이 다양합니다.

- 파드가 KILL 시그널을 처리하지 못한 채 갑자기 중지되어 메인 파이버(fiber)가 인터럽트되지 못함
- 파드가 네트워크 연결을 잃어 `unregister` 엔드포인트를 호출하지 못함(예를 들어 Istio 프록시 사용 시)
- 파드가 종료 과정에서 데드락(deadlock)에 빠짐

이런 경우에는 파드가 왜 등록 해제하지 못하는지 파악하고 근본 원인을 해결하는 것이 중요합니다.

---

## 3. Shardcake는 Akka/Pekko의 대체재인가요?

아닙니다. Shardcake는 Akka/Pekko가 하는 일 중 아주 일부, 구체적으로는 "Cluster Sharding" 기능만 다루는 라이브러리입니다. ZIO는 "로컬" 동시성 도구(예: Queue, Hub, Promise)를 제공하고, Shardcake는 여기에 "분산" 요소를 더합니다. 다만 Shardcake는 이벤트 소싱(event sourcing)이나 액터 영속화(actor persistence) 같은 기능은 다루지 않습니다.

이런 기능이 필요하다면 ZIO와 Shardcake 위에 직접 구현하는 것을 권장합니다.

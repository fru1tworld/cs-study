# Cassandra 운영

> 이 문서는 Apache Cassandra 공식 문서의 "Operating" 섹션을 한국어로 번역한 것입니다.
> 원본: https://cassandra.apache.org/doc/latest/cassandra/managing/operating/

---

## 목차

1. [토폴로지 변경(Topology Changes)](#1-토폴로지-변경topology-changes)
   - [부트스트랩(Bootstrap)](#11-부트스트랩bootstrap)
   - [토큰 할당(Token Allocation)](#12-토큰-할당token-allocation)
   - [부트스트랩 재개(Resuming Bootstrap)](#13-부트스트랩-재개resuming-bootstrap)
   - [노드 제거(Removing Nodes)](#14-노드-제거removing-nodes)
   - [죽은 노드 교체(Replacing a Dead Node)](#15-죽은-노드-교체replacing-a-dead-node)
   - [노드 이동(Moving Nodes)](#16-노드-이동moving-nodes)
   - [진행 상황 모니터링(Monitoring Progress)](#17-진행-상황-모니터링monitoring-progress)
   - [Cleanup](#18-cleanup)
2. [Repair(복구)](#2-repair복구)
   - [Anti-entropy 복구의 원리](#21-anti-entropy-복구의-원리)
   - [Incremental과 Full Repair](#22-incremental과-full-repair)
   - [사용법과 기본값(Usage and Defaults)](#23-사용법과-기본값usage-and-defaults)
   - [nodetool repair 옵션](#24-nodetool-repair-옵션)
   - [복구 주기(Frequency of Repair)](#25-복구-주기frequency-of-repair)
3. [Read Repair(읽기 복구)](#3-read-repair읽기-복구)
   - [단조적 쿼럼 읽기 보장](#31-단조적-쿼럼-읽기-보장)
   - [단조적 읽기의 테이블 수준 설정](#32-단조적-읽기의-테이블-수준-설정)
   - [Read Repair 예시](#33-read-repair-예시)
   - [읽기 일관성 수준과 Read Repair](#34-읽기-일관성-수준과-read-repair)
   - [Cassandra 4.0의 개선된 Read Repair](#35-cassandra-40의-개선된-read-repair)
   - [Read Repair 진단 이벤트](#36-read-repair-진단-이벤트)
   - [백그라운드 Read Repair 제거](#37-백그라운드-read-repair-제거)
4. [Hinted Handoff(힌트 전달)](#4-hinted-handoff힌트-전달)
   - [Hinted Handoff의 동작 방식](#41-hinted-handoff의-동작-방식)
   - [힌트의 적용(Application of Hints)](#42-힌트의-적용application-of-hints)
   - [힌트의 디스크 저장](#43-힌트의-디스크-저장)
   - [타임아웃된 쓰기 요청에 대한 힌트](#44-타임아웃된-쓰기-요청에-대한-힌트)
   - [힌트 설정(Configuring Hints)](#45-힌트-설정configuring-hints)
   - [런타임에서 힌트 설정](#46-런타임에서-힌트-설정)
   - [Hinted Handoff를 견고하게 만들기](#47-hinted-handoff를-견고하게-만들기)
   - [Hinted Handoff의 한계](#48-hinted-handoff의-한계)
5. [하드웨어 선택(Hardware Choices)](#5-하드웨어-선택hardware-choices)
   - [CPU](#51-cpu)
   - [메모리(Memory)](#52-메모리memory)
   - [디스크(Disks)](#53-디스크disks)
   - [클라우드 인스턴스 선택](#54-클라우드-인스턴스-선택)
6. [Bloom Filter(블룸 필터)](#6-bloom-filter블룸-필터)
   - [블룸 필터의 동작 방식](#61-블룸-필터의-동작-방식)
   - [기본 설정값](#62-기본-설정값)
   - [메모리 사용량](#63-메모리-사용량)
   - [튜닝 가이드](#64-튜닝-가이드)
   - [설정 변경](#65-설정-변경)
7. [참고 자료](#7-참고-자료)

---

## 1. 토폴로지 변경(Topology Changes)

분산 데이터베이스인 Cassandra 클러스터를 운영할 때, 운영자는 클러스터의 토폴로지(topology)를 변경해야 하는 경우가 자주 발생합니다. 새로운 노드를 추가하여 용량과 처리량을 늘리거나, 더 이상 필요하지 않거나 고장난 노드를 제거하거나, 죽은 노드를 새 노드로 교체하거나, 토큰 링(token ring) 상에서 노드의 위치를 이동시키는 작업이 여기에 해당합니다. 이 섹션에서는 클러스터 멤버십(membership)을 안전하게 변경하는 방법을 설명합니다.

### 1.1. 부트스트랩(Bootstrap)

새로운 노드를 클러스터에 추가하는 과정을 **부트스트랩(bootstrapping)**이라고 부릅니다. 부트스트랩이란 새로운 노드가 클러스터에 합류하면서 자신이 책임지게 될 토큰 범위(token range)에 해당하는 데이터를 기존의 복제본(replica) 노드들로부터 스트리밍(streaming)받아 채워 넣는 과정을 의미합니다.

새 노드가 클러스터에 합류하면, 해당 노드는 토큰 링 상에서 자신의 위치를 나타내는 가상 노드(virtual node, vnode)들을 할당받습니다. 이때 노드가 할당받는 가상 노드(토큰)의 개수는 `cassandra.yaml`의 `num_tokens` 파라미터에 의해 결정됩니다. `num_tokens`의 기본값은 256개의 토큰입니다. 즉, 별도의 설정이 없으면 하나의 노드는 256개의 가상 노드를 가지게 됩니다.

부트스트랩 과정에서 새 노드는 자신이 책임지게 될 토큰 범위의 데이터를, 그 데이터를 보유하고 있는 기존 복제본 노드들로부터 스트리밍으로 전송받습니다. 이를 통해 새 노드는 클러스터의 일관성(consistency)을 해치지 않으면서 정상적으로 읽기/쓰기 요청을 처리할 수 있는 상태가 됩니다.

부트스트랩이 완료되기 전까지, 새 노드는 클러스터에 의해 `UP` 상태이지만 아직 데이터를 모두 수신하지 못한 상태로 인식됩니다. 부트스트랩이 완료되어야 비로소 정상적인 읽기 요청 라우팅(routing) 대상이 됩니다.

### 1.2. 토큰 할당(Token Allocation)

기본적으로 Cassandra는 새 노드가 부트스트랩할 때 토큰을 무작위(random)로 할당합니다. `num_tokens`가 256처럼 충분히 큰 경우, 무작위 할당만으로도 클러스터 전체에 데이터가 비교적 고르게 분산되는 경향이 있습니다.

그러나 더 정교하게 데이터 부하(load)를 균형 있게 분산시키고 싶다면, 새로운 토큰 할당 알고리즘(token allocation algorithm)을 사용할 수 있습니다. 이 알고리즘은 특정 키스페이스(keyspace)의 복제 전략(replication strategy)과 부하를 분석하여, 데이터가 가능한 한 균등하게 분산되도록 토큰을 최적으로 할당합니다.

이 알고리즘을 사용하려면 새 노드를 시작할 때 다음과 같은 JVM 옵션을 지정해야 합니다.

```
-Dcassandra.allocate_tokens_for_keyspace=<keyspace>
```

여기서 `<keyspace>`는 부하 분석의 기준이 될 키스페이스의 이름입니다. 알고리즘은 지정된 키스페이스의 복제 인수(replication factor)를 분석하여, 그 키스페이스의 데이터가 노드들 사이에 최대한 균등하게 분배되도록 토큰을 선택합니다.

또한 특정 키스페이스에 의존하지 않고, 로컬 복제 인수(local replication factor)를 직접 지정하여 토큰을 할당받을 수도 있습니다. 이 경우 다음 옵션을 사용합니다.

```
-Dcassandra.allocate_tokens_for_local_replication_factor=<rf>
```

`cassandra.yaml`에서는 `allocate_tokens_for_keyspace` 또는 `allocate_tokens_for_local_replication_factor` 설정을 통해 동일한 동작을 구성할 수 있습니다.

수동으로 토큰을 지정하고 싶은 경우에는 `cassandra.yaml`의 `initial_token` 파라미터에 쉼표(comma)로 구분된 토큰 목록을 직접 입력할 수 있습니다. 이 경우 `num_tokens`의 값은 `initial_token`에 지정한 토큰의 개수와 일치해야 합니다. 수동 토큰 할당은 일반적으로 운영자가 토큰 분포를 완전히 제어하고자 하는 고급 사용 사례에서 사용됩니다.

### 1.3. 부트스트랩 재개(Resuming Bootstrap)

부트스트랩 과정은 많은 양의 데이터를 스트리밍하는 작업이기 때문에, 네트워크 문제나 노드 재시작 등의 이유로 도중에 실패할 수 있습니다. 부트스트랩이 실패한 경우, 처음부터 모든 데이터를 다시 스트리밍하는 대신 중단된 지점에서 재개할 수 있습니다.

실패한 부트스트랩을 재개하려면 다음 명령을 사용합니다.

```
nodetool bootstrap resume
```

또는 단순히 노드를 재시작(restart)하는 것으로도 부트스트랩을 재개할 수 있습니다. 노드는 이미 성공적으로 스트리밍받은 데이터 범위를 인식하고, 아직 받지 못한 범위에 대해서만 다시 스트리밍을 시도합니다.

### 1.4. 노드 제거(Removing Nodes)

클러스터에서 노드를 제거하는 방법은 해당 노드가 현재 살아있는(live) 상태인지, 아니면 죽은(dead) 상태인지에 따라 달라집니다. 노드를 제거하면, 그 노드가 책임지던 토큰 범위는 클러스터의 다른 노드들에게 재분배됩니다.

#### nodetool decommission (살아있는 노드 제거)

제거하려는 노드가 아직 정상적으로 동작하고 있는 살아있는 노드라면, 해당 노드 위에서 직접 다음 명령을 실행합니다.

```
nodetool decommission
```

`decommission`을 실행하면, 제거되는 노드가 자신이 보유한 데이터를 다른 복제본 노드들로 스트리밍합니다. 즉, 데이터는 디커미션(decommission)되는 노드로부터 흘러나갑니다(stream from). 이 과정이 완료되면 해당 노드의 토큰 범위는 다른 노드들로 옮겨지고, 그 노드는 클러스터에서 안전하게 제거됩니다.

이 작업은 데이터 손실 없이 노드를 제거하는 가장 깔끔한 방법입니다. 왜냐하면 제거되기 전에 자신의 데이터를 다른 노드에게 모두 넘겨주기 때문입니다.

#### nodetool removenode (죽은 노드 제거)

제거하려는 노드가 이미 다운되어 더 이상 동작하지 않는 죽은 노드라면, 클러스터 내의 다른 (살아있는) 노드 위에서 다음 명령을 실행합니다.

```
nodetool removenode <ID>
```

여기서 `<ID>`는 제거하려는 죽은 노드의 호스트 ID(Host ID)입니다. 호스트 ID는 `nodetool status` 명령으로 확인할 수 있습니다.

`removenode`의 경우, 제거되는 노드는 이미 죽어 있으므로 데이터를 스트리밍할 수 없습니다. 따라서 데이터는 죽은 노드가 책임지던 토큰 범위에 대한 **나머지 복제본 노드들(remaining replicas)로부터** 스트리밍되어 재분배됩니다.

`removenode`가 어떤 이유로 진행되지 않고 멈추는 경우(예: 스트리밍이 정상적으로 완료되지 않는 경우), 다음과 같이 `force` 옵션을 사용하여 제거를 강제로 완료시킬 수 있습니다.

```
nodetool removenode force
```

`removenode force`는 토큰 범위의 재분배를 완료하지 않은 채 노드를 강제로 클러스터에서 제거합니다. 이로 인해 데이터의 복제 인수가 일시적으로 부족해질 수 있으므로, 이후 반드시 복구(repair)를 실행하여 일관성을 회복해야 합니다.

#### nodetool assassinate (마지막 수단)

`decommission`이나 `removenode`로도 노드를 제거할 수 없는 극단적인 상황에서는 마지막 수단으로 다음 명령을 사용할 수 있습니다.

```
nodetool assassinate <IP>
```

`assassinate`는 데이터를 다시 스트리밍하지 않고, 어떠한 일관성 검증도 거치지 않으면서 클러스터에서 노드를 강제로 즉시 제거합니다. 이 명령은 가장 거칠고 위험한 방법이므로, 다른 모든 방법이 실패했을 때에만 사용해야 합니다. `assassinate`를 사용한 후에는 반드시 복구를 실행하여 데이터 일관성을 회복해야 합니다.

### 1.5. 죽은 노드 교체(Replacing a Dead Node)

죽은 노드를 단순히 제거하는 것이 아니라, 같은 토큰 범위를 책임지는 새로운 노드로 **교체(replace)**하고자 하는 경우가 있습니다. 예를 들어, 하드웨어가 고장 난 노드를 동일한 역할을 하는 새 하드웨어로 대체하는 상황입니다.

교체 노드를 시작할 때는 다음과 같은 JVM 옵션을 지정합니다. 이 옵션은 일반적으로 `cassandra-env.sh` 파일이나 `jvm.options` 파일에 추가합니다.

```
-Dcassandra.replace_address_first_boot=<dead_node_ip>
```

여기서 `<dead_node_ip>`는 교체 대상이 되는 죽은 노드의 IP 주소입니다.

교체 노드는 시작되면 먼저 **휴면(hibernate)** 상태로 들어갑니다. 이 상태에서 클러스터의 다른 노드들은 교체 노드를 `DOWN` 상태로 인식합니다. 교체 노드는 휴면 상태에서 죽은 노드가 책임지던 토큰 범위의 데이터를 다른 복제본들로부터 부트스트랩(스트리밍)받습니다. 데이터 수신이 완료되면 교체 노드는 정상적으로 클러스터에 합류하여 죽은 노드의 역할을 이어받습니다.

`replace_address_first_boot` 옵션은 (이전의 `replace_address` 옵션과 달리) 첫 번째 부팅에서만 적용되며, 이후 재시작에서는 무시됩니다. 따라서 교체가 완료된 후 옵션을 제거하는 것을 잊더라도 노드가 매번 교체를 시도하는 문제가 발생하지 않습니다.

> **중요(반드시 복구를 실행해야 하는 경우):**
>
> 다음 두 가지 상황에서는 교체가 완료된 후 **반드시 복구(repair)를 실행해야 합니다.**
>
> 1. 교체 대상이었던 죽은 노드가 `max_hint_window`(기본 3시간)보다 더 오랫동안 다운되어 있었던 경우. 이 경우 다운된 동안의 일부 쓰기에 대해 힌트(hint)가 저장되지 않았을 수 있으므로 데이터가 누락되었을 가능성이 있습니다.
> 2. 죽은 노드와 **동일한 IP 주소**로 교체를 진행하는데, 그 교체 과정(부트스트랩)이 `max_hint_window`를 초과하여 진행되는 경우.
>
> 위 상황에서는 누락된 데이터를 메우기 위해 교체 노드에 대해 복구를 수행해야 완전한 일관성을 회복할 수 있습니다.

### 1.6. 노드 이동(Moving Nodes)

`num_tokens: 1`로 설정되어 단일 토큰(single token)을 사용하는 클러스터(즉, 가상 노드를 사용하지 않는 클러스터)의 경우, 노드를 토큰 링 상의 다른 위치로 이동시킬 수 있습니다. 이동하려는 노드 위에서 다음 명령을 실행합니다.

```
nodetool move <new_token>
```

여기서 `<new_token>`은 노드가 새롭게 책임지게 될 토큰 값입니다. `move` 명령은 노드의 토큰 위치를 변경하고, 그에 따라 필요한 데이터를 스트리밍하여 새로운 토큰 범위를 채웁니다.

> **참고:** `nodetool move`는 단일 토큰 노드에서만 의미가 있습니다. 가상 노드(`num_tokens`가 1보다 큰 경우)를 사용하는 클러스터에서는 토큰이 여러 개이기 때문에 이 방식의 이동은 적용되지 않습니다.

노드를 이동한 후에는, 이동으로 인해 더 이상 해당 노드가 책임지지 않게 된 데이터가 디스크에 남아 있을 수 있습니다. 이러한 불필요한 데이터를 제거하려면 다음 절의 `nodetool cleanup`을 실행해야 합니다.

### 1.7. 진행 상황 모니터링(Monitoring Progress)

부트스트랩, 디커미션, removenode, move 등 모든 토폴로지 변경 작업은 노드 간 데이터 스트리밍을 수반합니다. 이러한 스트리밍 작업의 진행 상황을 모니터링하려면 다음 명령을 사용합니다.

```
nodetool netstats
```

`nodetool netstats`는 현재 진행 중인 스트리밍 작업의 상태를 보여줍니다. 여기에는 어떤 노드와 데이터를 주고받고 있는지, 각 스트림에서 전송된 파일과 바이트의 양, 그리고 완료까지의 진행률 등이 포함됩니다. 이를 통해 운영자는 토폴로지 변경 작업이 정상적으로 진행되고 있는지, 혹은 어딘가에서 멈추어 있는지를 확인할 수 있습니다.

### 1.8. Cleanup

노드를 추가(부트스트랩)하거나 이동(move)하여 토큰 범위가 재분배되면, 기존 노드들은 더 이상 자신이 책임지지 않게 된 데이터를 디스크에 그대로 가지고 있게 됩니다. 이 데이터는 더 이상 필요하지 않지만 자동으로 삭제되지는 않습니다.

이러한 불필요한 데이터를 제거하여 디스크 공간을 회수하려면 다음 명령을 실행합니다.

```
nodetool cleanup
```

`cleanup`은 현재 노드가 더 이상 소유하지 않는 토큰 범위의 데이터를 제거합니다. 이 작업은 디스크 I/O를 많이 사용하므로, 일반적으로 클러스터에 새 노드를 추가하는 작업이 모두 완료된 후 한 번에 수행하는 것이 권장됩니다.

---

## 2. Repair(복구)

분산 데이터베이스에서는 다양한 이유로 노드 간에 데이터가 서로 일치하지 않는(out of sync) 상태가 발생할 수 있습니다. 예를 들어, 노드가 다운된 동안 발생한 쓰기를 놓치거나, 힌트(hint)가 만료되어 적용되지 못하거나, 디스크 손상이나 운영자의 실수가 발생할 수 있습니다. **Repair(복구)**는 이러한 데이터 불일치를 해소하여 모든 복제본이 동일한 데이터를 가지도록 만드는 과정입니다.

힌트(hint)만으로는 데이터 불일치를 완전히 해소할 수 없습니다. 힌트는 최선 노력(best-effort) 기반의 메커니즘이며, 노드가 `max_hint_window`보다 오래 다운되어 있으면 힌트가 저장되지 않을 수 있기 때문입니다. 따라서 진정한 **궁극적 일관성(eventual consistency)**을 보장하려면 정기적으로 복구를 실행해야 합니다.

### 2.1. Anti-entropy 복구의 원리

Cassandra의 복구는 **anti-entropy(엔트로피 방지)** 방식으로 동작합니다. 복구의 기본 아이디어는, 같은 토큰 범위(common token range)에 대한 데이터를 보유한 노드들이 서로의 데이터셋을 비교하여, 일치하지 않는(out of sync) 부분만을 찾아내고 그 차이만을 노드 간에 스트리밍하여 동기화하는 것입니다.

데이터셋 전체를 한 바이트씩 비교하는 것은 비효율적이므로, Cassandra는 **머클 트리(Merkle tree)**라는 자료구조를 사용합니다. 머클 트리는 데이터를 계층적인 해시(hash) 구조로 표현한 트리입니다.

- 트리의 잎(leaf) 노드들은 데이터의 작은 부분 범위(sub-range)에 대한 해시값을 담습니다.
- 상위 노드는 자식 노드들의 해시값을 다시 해싱한 값을 담습니다.
- 결국 트리의 루트(root)는 전체 데이터 범위를 대표하는 단일 해시값이 됩니다.

두 노드가 자신의 머클 트리를 교환하고 비교할 때, 루트 해시값이 같다면 두 노드의 데이터는 완전히 동일한 것이므로 더 이상 비교할 필요가 없습니다. 만약 루트 해시값이 다르다면, 트리를 따라 내려가면서 해시값이 다른 가지(branch)만을 추적해 어느 부분 범위가 불일치하는지를 효율적으로 찾아냅니다. 이렇게 식별된 불일치 구간에 대해서만 실제 데이터를 노드 간에 스트리밍하여 동기화합니다.

### 2.2. Incremental과 Full Repair

Cassandra의 복구에는 크게 두 가지 종류가 있습니다.

#### Incremental Repair(증분 복구) - 기본값

**Incremental repair(증분 복구)**는 Cassandra의 기본 복구 방식입니다. 증분 복구는 **이전 복구 이후에 새로 기록된(written) 데이터만을** 대상으로 복구를 수행합니다.

이미 복구되어 "복구됨(repaired)"으로 표시된 데이터는 다시 복구하지 않습니다. 이렇게 함으로써 증분 복구는 매번 전체 데이터를 다시 비교하지 않아도 되므로, 복구에 소요되는 시간과 I/O 비용을 크게 줄일 수 있습니다. 따라서 증분 복구를 정기적으로(예: 매일) 자주 실행하는 것이 효율적입니다.

다만 증분 복구는 이미 복구됨으로 표시된 데이터를 다시 검사하지 않기 때문에, 그 데이터에 이후 디스크 손상(disk corruption)이나 운영자 실수(operator error) 등이 발생하더라도 이를 잡아내지 못한다는 한계가 있습니다.

#### Full Repair(전체 복구)

**Full repair(전체 복구)**는 토큰 범위 내의 **모든 데이터**를 대상으로 복구를 수행합니다. 데이터가 "복구됨"으로 표시되어 있는지 여부와 관계없이 전체를 다시 비교합니다.

증분 복구가 효율적이긴 하지만, 위에서 언급한 한계(디스크 손상이나 운영자 실수로부터의 보호 부족) 때문에 **full repair도 가끔씩(occasionally) 실행해야 합니다.** Full repair는 전체 데이터를 검증하므로 디스크 손상이나 누락된 데이터를 포괄적으로 잡아낼 수 있습니다.

### 2.3. 사용법과 기본값(Usage and Defaults)

복구는 운영자가 `nodetool` 명령을 통해 수동으로 실행합니다. Cassandra 자체에는 자동으로 복구를 스케줄링하는 내장 기능이 없으므로, 운영자가 cron 등을 통해 주기적으로 실행하거나 외부 도구(예: Reaper)를 사용해야 합니다.

기본적으로 `nodetool repair`는 증분 복구를 실행합니다.

```
# 기본값: 증분 복구 실행
nodetool repair
```

전체 복구를 실행하려면 `--full` 옵션을 사용합니다.

```
# 전체 복구 실행
nodetool repair --full
```

특정 키스페이스만 복구할 수 있습니다.

```
nodetool repair [options] <keyspace_name>
```

특정 키스페이스 내의 특정 테이블만 복구할 수도 있습니다.

```
nodetool repair [options] <keyspace_name> <table1> <table2>
```

키스페이스나 테이블을 지정하지 않으면, 시스템 키스페이스를 제외한 모든 키스페이스가 복구 대상이 됩니다.

### 2.4. nodetool repair 옵션

`nodetool repair`는 복구의 범위와 동작 방식을 제어하는 다양한 옵션을 제공합니다.

#### -pr, --partitioner-range (주 토큰 범위만 복구)

```
nodetool repair -pr
```

이 옵션은 복구를 **복구 대상 노드가 "주(primary)" 토큰 범위로 책임지는 부분에만** 한정합니다. 여기서 "주 토큰 범위"란 해당 노드가 그 토큰 범위의 첫 번째 복제본(first replica)인 범위를 의미합니다.

클러스터 전체를 복구할 때, 모든 노드가 자신이 보유한 전체 토큰 범위를 복구하면 동일한 데이터 범위를 여러 노드가 중복으로 복구하게 됩니다. `-pr` 옵션을 사용하여 각 노드가 자신의 주 범위만 복구하도록 하고, 클러스터의 모든 노드에 대해 복구를 한 번씩 실행하면, 전체 데이터가 정확히 한 번씩 복구되어 중복 작업을 피할 수 있습니다.

#### -prv, --preview (미리보기)

```
nodetool repair --preview
```

이 옵션은 실제로 데이터를 스트리밍하지 않고, **만약 복구를 수행한다면 얼마나 많은 양의 스트리밍이 발생할지를 추정(estimate)**합니다. 복구가 실제로 얼마나 많은 작업을 수반할지, 즉 노드 간에 데이터가 얼마나 어긋나 있는지를 미리 파악하는 데 유용합니다.

#### -vd, --validate (검증)

```
nodetool repair --validate
```

이 옵션은 **복구됨(repaired)으로 표시된 데이터가 모든 노드에서 동일한지를 검증(verify)**합니다. 즉, 이미 복구되었다고 표시된 데이터가 실제로 모든 복제본에서 일치하는지를 확인하여, 데이터 무결성에 문제가 없는지 점검할 수 있습니다.

#### -full (전체 복구)

```
nodetool repair --full
```

증분 복구 대신 전체 복구를 수행하도록 지정합니다.

#### -seq, --sequential (순차 복구)

순차 복구를 수행합니다. 토큰 범위에 대한 복제본들을 동시에(병렬로) 복구하는 대신 한 번에 하나씩(sequentially) 복구합니다. 이렇게 하면 복구가 한 번에 하나의 복제본만 부하를 받게 되어, 복구 중에도 나머지 복제본들이 요청을 처리할 수 있으므로 성능에 미치는 영향을 줄일 수 있습니다. 다만 복구 전체에 걸리는 시간은 더 길어집니다.

#### -dc, --in-dc (특정 데이터센터)

지정한 데이터센터(들) 내의 노드들에 대해서만 복구를 수행합니다.

#### -local, --in-local-dc (로컬 데이터센터만)

복구 대상 노드가 속한 로컬 데이터센터 내의 노드들에 대해서만 복구를 수행합니다. 데이터센터 간 네트워크 트래픽을 발생시키지 않으면서 복구를 수행할 때 유용합니다.

#### -hosts, --in-hosts (특정 호스트)

복구에 참여시킬 특정 호스트(들)를 지정합니다.

#### -j, --job-threads (작업 스레드 수)

하나의 복구 작업 내에서 동시에 복구할 테이블의 개수를 지정합니다. 기본값은 1이며, 값을 늘리면 복구 속도가 빨라질 수 있지만 그만큼 노드의 부하도 증가합니다. 최대값은 4입니다.

#### -st / -et (--start-token / --end-token, 부분 범위 복구)

`-st`(시작 토큰)와 `-et`(끝 토큰) 옵션을 사용하면 특정 토큰 범위(subrange)에 대해서만 복구를 수행할 수 있습니다. 이를 **부분 범위 복구(subrange repair)**라고 합니다. 매우 큰 데이터셋을 작은 범위로 나누어 점진적으로 복구하고자 할 때 유용합니다.

### 2.5. 복구 주기(Frequency of Repair)

복구를 얼마나 자주 실행해야 하는지는 클러스터의 워크로드와 운영 환경에 따라 달라지지만, 공식 문서는 다음과 같은 출발점(starting point)을 제안합니다.

- **증분 복구는 1~3일마다, 전체 복구는 1~3주마다** 실행하는 것을 시작점으로 삼을 수 있습니다.
- 또는 대안으로, **전체 복구를 5일마다** 실행하는 방식도 권장됩니다.

복구 주기에서 가장 중요한 제약은 **gc_grace 기간(garbage collection grace period)**과 관련이 있습니다. Cassandra에서 데이터를 삭제하면 실제로 즉시 삭제되는 것이 아니라, 톰스톤(tombstone)이라는 삭제 표식이 남습니다. 이 톰스톤은 `gc_grace_seconds`(기본값 10일) 동안 유지된 후 가비지 컬렉션됩니다.

만약 톰스톤이 가비지 컬렉션되기 전에 모든 복제본에 삭제가 전파되지 못하면, 이미 삭제된 데이터가 다시 살아나는(좀비 데이터, zombie data) 현상이 발생할 수 있습니다. 이를 방지하려면, **gc_grace 기간이 만료되기 전에 모든 노드에 대해 복구를 완료**해야 합니다.

따라서 최소한의 권장 사항으로, **gc_grace_seconds의 기본값이 10일이라면, 모든 노드를 7일 이내에 한 번씩은 복구**해야 합니다. 이렇게 하면 미복구 데이터에 대해 gc_grace 기간이 만료되어 삭제된 데이터가 부활하는 문제를 방지할 수 있습니다.

> **요약:** 복구는 최소한 7일에 한 번 이상(gc_grace 기간 내에) 실행해야 하며, 디스크 손상에 대비하기 위해 전체 복구도 주기적으로 수행해야 합니다.

---

## 3. Read Repair(읽기 복구)

**Read Repair(읽기 복구)**는 읽기 요청(read request)을 처리하는 과정에서 데이터 복제본들을 복구하는 과정입니다. 즉, 클라이언트가 데이터를 읽을 때, 그 읽기에 관여하는 복제본들의 데이터가 서로 일치하지 않는다는 사실이 발견되면, Cassandra는 그 자리에서 복제본들을 일치시키는 read repair를 수행합니다. 그리고 클라이언트에게는 가장 최신(up-to-date)의 데이터를 반환합니다.

Read repair는 anti-entropy repair(`nodetool repair`)와 함께 Cassandra가 데이터 일관성을 유지하는 핵심 메커니즘 중 하나입니다. 다만 read repair는 실제로 읽힌 데이터에 대해서만 동작하므로, 자주 읽히지 않는 데이터까지 복구하지는 못합니다. 따라서 read repair는 전체 복구나 노드 교체 절차를 대체할 수 없습니다.

### 3.1. 단조적 쿼럼 읽기 보장

Cassandra는 **단조적 쿼럼 읽기(monotonic quorum reads)**를 보장하기 위해 블로킹 read repair(blocking read repair)를 구현합니다.

단조적 쿼럼 읽기란, 연속해서 수행되는 쿼럼 읽기(quorum read)가 이전 읽기보다 더 오래된(older) 데이터를 반환하지 않음을 보장하는 것입니다. 즉, 시간이 지남에 따라 읽기 결과가 과거로 되돌아가지 않습니다.

이 보장은, 실패한 쓰기(failed write)가 복제본의 소수(minority)에만 도달한 경우에도 유지되어야 합니다. 예를 들어, 어떤 쓰기가 쿼럼을 만족하지 못해 클라이언트에게는 실패로 응답되었지만 일부 복제본에는 기록되었다고 가정합시다. 이 경우 후속 읽기에서 어떤 복제본이 응답하느냐에 따라 새 값이 보였다 안 보였다 할 수 있는데, 블로킹 read repair는 이러한 비단조성(non-monotonicity)을 방지합니다.

### 3.2. 단조적 읽기의 테이블 수준 설정

Cassandra 4.0부터는 테이블 수준에서 read repair의 동작을 설정할 수 있는 `read_repair` 테이블 옵션이 도입되었습니다. 이 옵션은 두 가지 값을 가질 수 있습니다.

#### BLOCKING (기본값)

`read_repair`가 `BLOCKING`으로 설정되어 있고 read repair가 시작되면, 그 읽기는 **다른 복제본들에게 전송된 쓰기들이 일관성 수준(consistency level, CL)을 만족할 때까지 블로킹(block)**됩니다.

- **제공하는 것:** 단조적 쿼럼 읽기(monotonic quorum reads)
- **제공하지 않는 것:** 파티션 수준의 쓰기 원자성(partition-level write atomicity)

#### NONE

`read_repair`가 `NONE`으로 설정되면, 코디네이터(coordinator)는 복제본들 사이의 차이를 조정(reconcile)하여 클라이언트에게 올바른 값을 반환하지만, **복제본들을 실제로 복구하려고 시도하지는 않습니다.**

- **제공하는 것:** 쓰기 원자성(write atomicity)
- **제공하지 않는 것:** 단조적 쿼럼 읽기(monotonic quorum reads)

테이블을 생성할 때 다음과 같이 `read_repair` 옵션을 지정할 수 있습니다.

```sql
CREATE TABLE ks.tbl (k INT, c INT, v INT, PRIMARY KEY (k,c)) with read_repair='NONE'
```

### 3.3. Read Repair 예시

5개의 노드로 구성되고 복제 인수(replication factor)가 3인 클러스터에서, 일관성 수준 `TWO`로 읽기를 수행하는 경우를 예로 들어 read repair 과정을 단계별로 살펴봅니다.

#### 1단계: 직접 읽기 요청(Direct Read Request)

코디네이터(클라이언트의 요청을 받은 노드)는 가장 빠르게 응답할 것으로 예상되는 복제본(fastest replica)에게 **전체 데이터(full data)를 요청하는 직접 읽기 요청**을 보냅니다. 이 요청에 대한 응답은 실제 컬럼 데이터를 모두 포함합니다.

#### 2단계: 다이제스트 읽기 요청(Digest Read Requests)

코디네이터는 일관성 수준을 만족하기 위해 추가로 필요한 복제본들에게는 **다이제스트 읽기 요청(digest read request)**을 보냅니다. 다이제스트 요청은 실제 데이터를 모두 보내는 대신 데이터에 대한 **해시값(hash value)만**을 반환합니다. 이렇게 하면 네트워크 트래픽을 줄일 수 있습니다.

#### 3단계: 비교(Comparison)

코디네이터는 직접 읽기로 받은 데이터의 해시값과 다이제스트 요청들로 받은 해시값들을 비교합니다.

- 해시값들이 모두 **일치(match)**하면, 복제본들의 데이터가 동일하다는 의미이므로 복구가 필요하지 않습니다. 코디네이터는 데이터를 클라이언트에게 반환합니다.

#### 4단계: 복구(Repair, 필요한 경우)

- 만약 해시값들이 **불일치(mismatch)**하면, 복제본들의 데이터가 어긋나 있다는 의미입니다. 이 경우 코디네이터는 다른 복제본들로부터 **전체 데이터(full data)**를 다시 요청합니다.
- 코디네이터는 받은 데이터들의 타임스탬프(timestamp)를 비교하여 가장 최신 값을 판별합니다.
- 가장 최신의(reconciled) 데이터를 클라이언트에게 반환합니다.
- 동시에, 오래된 값을 가지고 있던(out-of-date) 복제본들에게 최신 값을 기록하는 쓰기를 보내 그 복제본들을 복구합니다.

### 3.4. 읽기 일관성 수준과 Read Repair

read repair가 수행되는지 여부는 읽기 요청의 일관성 수준(consistency level)에 따라 달라집니다.

- **ONE, LOCAL_ONE:** 이 수준에서는 첫 번째 직접 읽기 요청 하나로 일관성 수준이 이미 충족됩니다. 따라서 **read repair가 수행되지 않습니다.** (오직 하나의 복제본만 읽으면 되므로 비교할 대상이 없습니다.)
- **TWO, THREE, LOCAL_QUORUM, QUORUM:** 이 수준들에서는 여러 복제본의 응답을 비교해야 하므로, **불일치가 감지되면 read repair가 수행됩니다.**

### 3.5. Cassandra 4.0의 개선된 Read Repair

Cassandra 4.0에서는 블로킹 read repair의 동작과 관련하여 두 가지 중요한 개선이 이루어졌습니다.

#### 1) Speculative Retry(추측성 재시도)

응답이 지연되는 것처럼 보이면, Cassandra는 아직 연락하지 않은(uncontacted) 복제본들에게 추가적인 읽기 요청을 **추측성으로(speculatively)** 보냅니다. 이렇게 하면 느린 복제본 하나 때문에 전체 읽기가 지연되는 것을 방지하고, 응답 지연 시간(latency)을 개선할 수 있습니다.

#### 2) Partial Blocking(부분 블로킹)

이전에는 다이제스트 불일치가 발생하면 모든 관련 작업이 완료될 때까지 블로킹했습니다. Cassandra 4.0에서는 **다이제스트 불일치를 해소하는 데 필요한 만큼만 블로킹**하고, 일관성 수준을 만족시키기에 충분한 만큼의 전체 데이터 응답을 받을 때까지만 기다립니다. 즉, 불필요하게 더 많은 응답을 기다리지 않고 꼭 필요한 만큼만 기다리므로 효율이 향상됩니다.

### 3.6. Read Repair 진단 이벤트

Cassandra 4.0은 read repair에 대한 **진단 이벤트(diagnostic events)**를 추가했습니다. 이 진단 이벤트들은 다음과 같은 정보를 노출하여, 운영자가 read repair의 동작을 관찰하고 디버깅할 수 있도록 돕습니다.

- 연락된 엔드포인트(contacted endpoints)
- 다이제스트 응답(digest responses)
- 영향을 받은 파티션 키(affected partition keys)
- 추측성으로 수행된 작업(speculated operations)
- 업데이트의 크기(update sizes)

### 3.7. 백그라운드 Read Repair 제거

Cassandra 4.0에서는 **백그라운드 read repair(background read repair)**가 **제거**되었습니다.

이전 버전에서 백그라운드 read repair는 `cassandra.yaml`의 `read_repair_chance`와 `dclocal_read_repair_chance` 설정을 통해 구성되었습니다. 이 설정들은 읽기가 일어날 때 일정 확률로, 일관성 수준을 만족하기 위해 필요한 복제본보다 더 많은 복제본을 (백그라운드에서) 읽고 비교하여 복구하는 방식이었습니다. Cassandra 4.0부터는 이 두 설정과 백그라운드 read repair 기능이 모두 제거되었습니다.

> **중요:** read repair는 full repair(`nodetool repair`)나 노드 교체 절차를 대체할 수 없습니다. read repair는 실제로 읽힌 데이터만 복구하므로, 데이터 전반의 일관성을 보장하려면 정기적인 anti-entropy repair가 반드시 필요합니다.

---

## 4. Hinted Handoff(힌트 전달)

**Hinted Handoff(힌트 전달)**는 쓰기 작업 도중에 동작하는 복구 메커니즘입니다. 노드 장애나 유지보수로 인해 복제본 노드가 일시적으로 사용 불가능(unavailable) 상태가 되었을 때, 코디네이터(coordinator)는 그 노드로 향했어야 할 쓰기 데이터를 **힌트(hint)**라는 형태로 로컬에 임시 저장해 두었다가, 나중에 해당 노드가 복구되면 그 힌트를 다시 적용(replay)합니다.

힌트는 데이터 불일치(data inconsistency)가 지속되는 기간을 줄이는 중요한 방법입니다. 노드가 잠시 다운되었다 복구되었을 때, 힌트가 없다면 그 노드는 다운된 동안 발생한 모든 쓰기를 놓치게 되어 다른 노드들과 데이터가 어긋나게 됩니다. 힌트는 이러한 격차를 빠르게 메워줍니다.

### 4.1. Hinted Handoff의 동작 방식

힌트가 저장되고 다시 적용되는 과정을 시간 순서대로 살펴보면 다음과 같습니다.

- **(t0)** 클라이언트가 쓰기 요청을 보냅니다. 코디네이터는 이 쓰기를 세 개의 복제본에 전송하지만, 그 중 하나의 복제본이 마침 재시작 중이어서 사용 불가능한 상태입니다.
- **(t1)** 나머지 복제본들이 쓰기를 성공적으로 처리하여 쿼럼(quorum)을 만족하면, 코디네이터는 클라이언트에게 쿼럼 확인 응답(quorum acknowledgement)을 보냅니다. 즉, 한 복제본이 다운되었더라도 쿼럼을 만족하면 쓰기는 클라이언트에게 성공으로 응답됩니다.
- **(t2)** 쓰기 타임아웃(write timeout, 기본값 2초)이 경과한 후에도 그 복제본이 여전히 도달 불가능하다면, 코디네이터는 그 복제본을 위한 힌트를 로컬에 저장합니다.
- **(t3)** 사용 불가능했던 복제본이 재시작되어 다시 살아나고, 가십(gossip) 메시지를 통해 자신이 다시 `UP` 상태가 되었음을 클러스터에 알립니다.
- **(t4)** 코디네이터는 가십을 통해 그 복제본이 복구된 것을 감지하고, 저장해 두었던 힌트 변이(mutation)들을 그 복제본에게 다시 적용(replay)합니다.

### 4.2. 힌트의 적용(Application of Hints)

코디네이터가 저장된 힌트를 복구된 복제본에게 적용할 때, 힌트는 **세그먼트(segment) 단위로 대량(bulk)으로** 대상 복제본 노드에 스트리밍됩니다. 대상 노드는 받은 세그먼트를 로컬에 재생(replay)하여 적용합니다. 하나의 세그먼트를 모두 재생하고 나면, 대상 노드는 그 세그먼트를 삭제하고 다음 세그먼트를 받습니다.

이러한 방식으로 힌트는 한 번에 하나의 세그먼트씩 순차적으로 전송 및 적용되며, 적용이 완료된 세그먼트는 즉시 삭제되어 디스크 공간을 회수합니다.

힌트의 적용은 **멱등(idempotent)**하며, **미래의 변이(future mutation)를 덮어쓸 수 없습니다.** 즉, 힌트에는 원래 쓰기의 타임스탬프가 보존되어 있기 때문에, 힌트가 적용될 때 그 타임스탬프를 기준으로 적용됩니다. 만약 그 사이에 더 최신의 쓰기가 이미 적용되어 있다면, 오래된 타임스탬프를 가진 힌트는 최신 데이터를 덮어쓰지 못합니다. 이로 인해 힌트를 여러 번 적용하거나 늦게 적용하더라도 데이터의 정확성이 깨지지 않습니다.

### 4.3. 힌트의 디스크 저장

힌트는 기본적으로 `$CASSANDRA_HOME/data/hints` 디렉터리에 플랫 파일(flat file) 형태로 저장됩니다.

각 힌트는 다음과 같은 정보를 포함합니다.

- **힌트 ID(hint ID)**: 힌트를 식별하는 고유 ID
- **대상 복제본 노드(target replica node)**: 이 힌트가 적용되어야 할 노드의 식별자
- **직렬화된 변이 블롭(serialized mutation blob)**: 실제로 적용되어야 할 쓰기 변이의 직렬화된 데이터
- **변이의 타임스탬프(mutation timestamp)**: 원래 쓰기가 발생한 시점의 타임스탬프
- **Cassandra 버전(Cassandra version)**: 힌트가 생성된 Cassandra의 버전

기본적으로 힌트는 `LZ4Compressor`를 사용하여 압축됩니다. 이를 통해 디스크 사용량을 줄입니다.

앞서 설명한 대로, 힌트 적용은 멱등하며 미래의 변이를 덮어쓸 수 없습니다. 이는 각 힌트가 원래 변이의 타임스탬프를 그대로 보존하기 때문입니다.

### 4.4. 타임아웃된 쓰기 요청에 대한 힌트

힌트는 노드가 다운된 경우뿐만 아니라, **타임아웃된(timed out) 쓰기 요청**에 대해서도 저장됩니다. 즉, 복제본이 완전히 다운되지 않았더라도 쓰기 요청에 대한 응답을 제때 하지 못하면(타임아웃), 코디네이터는 그 쓰기에 대한 힌트를 저장합니다.

쓰기 요청에 대한 힌트가 언제 생성되는지는 `write_request_timeout` 설정에 의해 결정됩니다. 이 설정의 기본값은 `2000ms`(2초)이며, 설정 가능한 최소값은 `10ms`입니다.

### 4.5. 힌트 설정(Configuring Hints)

힌트의 동작은 `cassandra.yaml` 파일의 여러 설정을 통해 구성할 수 있습니다.

| 설정(Setting) | 설명(Description) | 기본값(Default) |
|---|---|---|
| `hinted_handoff_enabled` | hinted handoff 활성화/비활성화 여부 | `true` |
| `hinted_handoff_disabled_datacenters` | hinted handoff에서 제외할 데이터센터 목록 | 미설정(unset) |
| `max_hint_window` | 힌트를 생성(저장)하는 노드의 최대 다운타임 기간. 이 기간을 초과하여 다운된 노드에 대해서는 더 이상 힌트를 저장하지 않음 | `3h`(3시간) |
| `hinted_handoff_throttle` | 힌트 전달 속도 제한(스로틀). 전달 스레드당 초당 최대 전송량 | `1024KiB` |
| `max_hints_delivery_threads` | 동시 힌트 전달 스레드의 개수 | `2` |
| `hints_directory` | 힌트 파일을 저장하는 디렉터리 위치 | `$CASSANDRA_HOME/data/hints` |
| `hints_flush_period` | 힌트 버퍼를 디스크로 플러시(flush)하는 주기 | `10000ms` |
| `max_hints_file_size` | 단일 힌트 파일의 최대 크기 | `128MiB` |
| `hints_compression` | 힌트 파일에 사용할 압축 알고리즘 | `LZ4Compressor` |

> **`max_hint_window`의 의미:** 노드가 다운된 후 이 기간(`3h`) 이내에 복구되면, 코디네이터가 저장해 둔 힌트를 적용하여 빠르게 일관성을 회복할 수 있습니다. 그러나 노드가 이 기간을 초과하여 다운되어 있으면, 코디네이터는 그 노드를 위한 힌트 저장을 중단합니다. 따라서 노드가 `max_hint_window`보다 오래 다운되었다가 복구되면, 누락된 데이터를 메우기 위해 반드시 `nodetool repair`를 실행해야 합니다.

### 4.6. 런타임에서 힌트 설정

`nodetool` 명령을 사용하면 노드를 재시작하지 않고도 런타임에서 힌트 관련 설정을 변경하거나 제어할 수 있습니다. 이 명령들은 `cassandra.yaml`의 설정을 일시적으로 재정의(override)합니다.

| nodetool 명령 | 설명 |
|---|---|
| `nodetool disablehandoff` | 힌트의 저장과 전달을 중단합니다. |
| `nodetool enablehandoff` | 힌트의 저장과 전달을 다시 활성화합니다. |
| `nodetool disablehintsfordc` | 특정 데이터센터에 대한 힌트를 비활성화합니다. |
| `nodetool enablehintsfordc` | 특정 데이터센터에 대한 힌트를 다시 활성화합니다. |
| `nodetool getmaxhintwindow` | 현재 최대 힌트 윈도우 값을 밀리초(ms) 단위로 표시합니다. |
| `nodetool handoffwindow` | 현재 hinted handoff 윈도우를 표시합니다. |
| `nodetool pausehandoff` | 힌트 전달 과정을 일시 중지합니다. |
| `nodetool resumehandoff` | 일시 중지된 힌트 전달을 재개합니다. |
| `nodetool sethintedhandoffthrottlekb` | 힌트 전달 속도(스로틀)를 런타임에서 동적으로 조정합니다. |
| `nodetool setmaxhintwindow` | 힌트 보존 윈도우를 런타임에서 동적으로 확장합니다. (Cassandra 4.0에서 추가됨) |
| `nodetool statushandoff` | 현재 힌트 저장이 활성화되어 있는지 상태를 확인합니다. |
| `nodetool truncatehints` | 로컬의 모든 힌트, 또는 특정 엔드포인트에 대한 힌트를 삭제합니다. |

### 4.7. Hinted Handoff를 견고하게 만들기

#### 전달 스로틀(Throttle) 조정

기본값인 1024 KiB/s의 힌트 전달 스로틀은 대부분의 최신 네트워크 환경에서는 보수적(conservative)인 값입니다. 즉, 실제 네트워크는 그보다 훨씬 빠른 속도를 낼 수 있습니다.

쓰기 처리량이 매우 높아서 힌트가 대량으로 쌓이는 경우, 기본 스로틀로는 힌트를 모두 전달하여 적용하는 데 너무 오랜 시간이 걸릴 수 있습니다. 공식 문서가 제시하는 예시는 다음과 같습니다.

> 노드당 100 Mbps의 데이터 수집(ingestion)이 일어나는 환경에서, 어떤 노드가 10분 동안 재시작되었다고 가정합니다. 이 경우 그 노드를 위해 약 **7 GiB의 힌트**가 쌓이게 됩니다. 이를 기본 스로틀인 1024 KiB/s로 전달(playback)하면 모두 적용하는 데 **약 2시간**이 걸립니다.

이처럼 힌트 적용이 너무 느린 경우, 운영자는 `nodetool sethintedhandoffthrottlekb` 명령을 사용하여 런타임에서 스로틀 값을 동적으로 높일 수 있습니다. 네트워크 대역폭에 여유가 있다면 이 값을 늘려 힌트를 더 빠르게 적용하고 일관성을 더 빨리 회복할 수 있습니다.

#### 노드가 더 오래 다운될 수 있도록 허용하기

기본적으로 노드가 `max_hint_window`(3시간)보다 오래 다운되면 그 노드에 대한 힌트 저장이 중단됩니다. 그러나 계획된 유지보수 등으로 인해 노드가 더 오랫동안 다운될 것이 예상되고, 그 동안에도 누락 없이 힌트를 저장하고 싶다면, `nodetool setmaxhintwindow`(Cassandra 4.0에서 추가됨) 명령을 사용하여 힌트 윈도우를 런타임에서 동적으로 확장할 수 있습니다.

다만 힌트 윈도우를 늘리면 그만큼 더 많은 힌트가 디스크에 쌓이게 되므로, 디스크 공간이 충분한지 확인해야 합니다.

### 4.8. Hinted Handoff의 한계

힌트는 **최선 노력(best-effort) 기반의 일관성 메커니즘**이며, anti-entropy repair가 제공하는 것과 같은 **궁극적 일관성(eventual consistency)을 보장하지 않습니다.**

힌트가 일관성을 보장하지 못하는 경우는 다음과 같습니다.

- 노드가 `max_hint_window`보다 오래 다운되어 있으면, 그 이후의 쓰기에 대한 힌트는 저장되지 않습니다.
- 힌트를 저장하고 있던 코디네이터 노드 자체가 죽어버리면, 그 코디네이터가 가지고 있던 힌트도 함께 사라집니다.
- 힌트 윈도우 내에 노드가 복구되지 못하면 데이터가 누락됩니다.

따라서 힌트는 **full repair(전체 복구)를 대체할 수 없습니다.** 힌트는 일시적인 노드 다운으로 인한 짧은 기간의 불일치를 빠르게 메우는 보조적인 메커니즘일 뿐이며, 진정한 데이터 일관성을 보장하려면 정기적인 `nodetool repair`가 반드시 필요합니다.

---

## 5. 하드웨어 선택(Hardware Choices)

대부분의 데이터베이스와 마찬가지로, Cassandra의 처리량(throughput)은 더 많은 CPU 코어, 더 많은 RAM, 그리고 더 빠른 디스크를 사용할수록 향상됩니다. 적절한 하드웨어를 선택하면 Cassandra의 성능과 안정성을 크게 높일 수 있습니다.

- **최소 프로덕션 배포(minimal production deployment):** 최소 2개의 코어와 최소 8GB의 RAM이 필요합니다.
- **일반적인 프로덕션 서버(typical production server):** 8개 이상의 코어와 32GB 이상의 RAM을 갖춥니다.

### 5.1. CPU

Cassandra는 여러 CPU 코어에 걸쳐 작업을 동시에(concurrently) 처리하도록 설계된, 높은 동시성(high concurrency)을 가진 시스템입니다. 따라서 코어를 추가하면 읽기와 쓰기 처리량이 모두 증가합니다.

특히 Cassandra의 **쓰기 경로(write path)는 매우 고도로 최적화**되어 있습니다. 쓰기는 먼저 커밋로그(commitlog)에 기록된 후 데이터를 멤테이블(memtable)에 삽입하는 방식으로 처리됩니다. 이 과정은 디스크 탐색이 거의 없는 순차적인 작업이므로, 쓰기는 특히 **CPU에 의해 제한(CPU bound)되는** 경향이 있습니다. 즉, 쓰기 성능을 높이려면 더 빠르고 더 많은 CPU 코어가 효과적입니다.

### 5.2. 메모리(Memory)

Cassandra는 자바 가상 머신(Java VM) 위에서 동작합니다. JVM은 미리 할당된 힙(heap) 메모리를 사용하며, 그 외에도 다음과 같은 용도로 **힙 외부(off-heap)** 메모리를 사용합니다.

- 압축 메타데이터(compression metadata)
- 블룸 필터(bloom filters)
- 캐시(caches)
- 운영체제의 페이지 캐시(OS page caching)

메모리와 관련한 주요 권장 사항은 다음과 같습니다.

- **ECC RAM을 항상 사용해야 합니다.** Cassandra는 비트 수준의 손상(bit level corruption)으로부터 보호하는 내부 안전장치가 거의 없기 때문에, 메모리 오류를 자동으로 감지하고 정정하는 ECC RAM을 사용하는 것이 매우 중요합니다.
- **힙 크기(heap size)는 최소 2GB, 최대 시스템 RAM의 50%**로 설정합니다. 힙을 너무 크게 잡으면 가비지 컬렉션(GC) 일시정지가 길어지고, OS 페이지 캐시에 사용할 메모리가 부족해집니다.
- **힙 크기가 12GB 미만인 경우:** ParNew/ConcurrentMarkSweep(CMS) 가비지 컬렉터를 고려합니다.
- **힙 크기가 12GB를 초과하는 경우:** 16GB 힙에 8~10GB의 new generation을 할당하는 구성을 고려하거나, G1GC(Garbage-First Garbage Collector)를 고려합니다.

> Cassandra는 OS 페이지 캐시를 적극적으로 활용하므로, 시스템 RAM의 상당 부분을 OS가 파일 캐싱에 사용할 수 있도록 남겨두는 것이 성능에 중요합니다.

### 5.3. 디스크(Disks)

Cassandra는 디스크에 두 종류의 데이터를 기록합니다.

1. **커밋로그(commitlog):** 충돌 복구(crash recovery)를 위한 로그입니다. 모든 쓰기는 먼저 커밋로그에 순차적으로 추가(append)됩니다.
2. **데이터 디렉터리(data directory):** SSTable 형태로 데이터를 영구히 저장하는 곳입니다.

Cassandra는 회전식 하드 디스크(spinning hard drive, HDD)와 솔리드 스테이트 디스크(solid state disk, SSD) 모두에서 매우 잘 동작합니다.

#### 커밋로그와 데이터 디렉터리의 분리

**커밋로그와 데이터 디렉터리는 서로 다른 물리 디스크(different physical disks)에 두어야 합니다.**

커밋로그를 데이터 디렉터리와 분리하면, 쓰기는 디스크 플래터(platter) 상에서 이리저리 탐색(seek)할 필요 없이 커밋로그에 순차적으로 추가하는 방식의 이점을 온전히 누릴 수 있습니다. 만약 두 디렉터리가 같은 디스크에 있으면, 커밋로그의 순차 쓰기와 데이터 디렉터리에 대한 무작위 접근이 서로 경합하여 성능이 저하될 수 있습니다.

#### 디스크 사용량 권장 사항

- 디스크 사용량은 컴팩션(compaction)이 동작할 여유 공간을 위해 적정 수준 이하로 유지해야 합니다. 일반적으로 디스크 사용률이 너무 높아지지 않도록 관리해야 컴팩션 시 임시 공간을 확보할 수 있습니다.
- SSD를 사용하는 경우, 성능 유지를 위해 TRIM이 적절히 동작하도록 권장합니다.
- 네트워크 기반 스토리지(NFS, SAN 등)는 일반적으로 권장되지 않습니다. 이는 지연 시간이 길고 단일 장애 지점(single point of failure)이 될 수 있기 때문입니다.

#### RAID 구성

- **RAID0 또는 JBOD**가 RAID1이나 RAID5보다 선호됩니다.
- 그 이유는 Cassandra가 이미 복제(replication)를 통해 데이터 중복성(redundancy)을 제공하기 때문입니다. RAID 수준에서 추가로 중복성을 확보할 필요가 없으며, 오히려 RAID1/RAID5는 쓰기 성능을 떨어뜨리고 가용 용량을 줄입니다.
- RAID0이나 JBOD는 여러 디스크의 처리량을 합쳐 더 높은 성능을 제공합니다. 디스크 하나가 고장 나더라도 Cassandra의 복제가 데이터를 보호합니다.

### 5.4. 클라우드 인스턴스 선택

AWS와 같은 클라우드 환경에서 Cassandra를 운영할 때 자주 사용되는 인스턴스 유형은 다음과 같습니다.

- **i2 인스턴스:** CPU 대비 높은 RAM 비율과 로컬 SSD를 제공합니다.
- **i3 인스턴스:** NVMe 기반의 고성능 로컬 스토리지를 제공합니다.
- **m4.2xlarge / c4.4xlarge + EBS GP2 스토리지:** EBS GP2(General Purpose SSD) 스토리지와 함께 사용하는 범용 인스턴스 조합입니다.

일반적으로 디스크와 네트워크 성능은 인스턴스의 크기(size)와 세대(generation)가 커질수록 향상됩니다. 따라서 더 큰 인스턴스나 최신 세대의 인스턴스를 선택하면 더 나은 I/O 및 네트워크 성능을 기대할 수 있습니다.

---

## 6. Bloom Filter(블룸 필터)

### 6.1. 블룸 필터의 동작 방식

읽기 경로(read path)에서 Cassandra는 디스크에 있는 데이터(SSTable)와 RAM에 있는 데이터(memtable)를 병합(merge)하여 결과를 만들어냅니다. 요청된 파티션(partition)을 찾기 위해 디스크 상의 모든 SSTable 데이터 파일을 일일이 확인하는 것은 매우 비효율적입니다.

이러한 비효율을 피하기 위해 Cassandra는 **블룸 필터(bloom filter)**라는 자료구조를 사용합니다. 블룸 필터는 주어진 SSTable 파일에 특정 파티션 데이터가 존재할 가능성이 있는지를 빠르게 판단해 줍니다. 블룸 필터의 판단 결과는 다음 두 가지 중 하나입니다.

- 해당 데이터가 그 파일에 **확실히 존재하지 않음(definitely does not exist).**
- 해당 데이터가 그 파일에 **아마도 존재함(probably exists).**

즉, 블룸 필터는 **확률적 자료구조(probabilistic data structure)**입니다. 데이터가 존재한다는 것을 보장할 수는 없지만(거짓 양성, false positive가 발생할 수 있음), 데이터가 존재하지 않는다는 것은 확실하게(거짓 음성 없이) 판단할 수 있습니다.

블룸 필터가 "확실히 존재하지 않음"이라고 답하면, Cassandra는 그 SSTable 파일을 읽지 않고 건너뛸 수 있으므로 불필요한 디스크 I/O를 절약할 수 있습니다.

### 6.2. 기본 설정값

블룸 필터의 거짓 양성 확률(false positive chance)은 테이블별로 `bloom_filter_fp_chance` 설정을 통해 조정할 수 있으며, 이 값은 0과 1 사이의 실수(float)입니다.

`bloom_filter_fp_chance`의 기본값은 컴팩션 전략(compaction strategy)에 따라 달라집니다.

- **LeveledCompactionStrategy를 사용하는 테이블:** 기본값 `0.1`
- **그 외의 모든 경우:** 기본값 `0.01`

값이 작을수록(예: 0.01) 거짓 양성이 적어 디스크 I/O가 줄어들지만, 그만큼 더 많은 메모리를 사용합니다. 값이 클수록(예: 0.1) 메모리는 적게 쓰지만 거짓 양성이 늘어나 불필요한 디스크 I/O가 증가할 수 있습니다.

### 6.3. 메모리 사용량

블룸 필터는 RAM에 저장되지만, **힙 외부(off-heap)**에 저장됩니다. 따라서 운영자는 최대 힙 크기(maximum heap size)를 결정할 때 블룸 필터의 메모리 사용량을 고려할 필요가 없습니다.

다만 블룸 필터의 정확도를 높이면 메모리 사용량이 **비선형적(non-linear)**으로 증가한다는 점에 유의해야 합니다. 구체적으로, **`bloom_filter_fp_chance`가 0.01인 블룸 필터는 같은 테이블에서 `bloom_filter_fp_chance`가 0.1인 경우보다 약 3배 더 많은 메모리를 사용**합니다.

즉, 거짓 양성 확률을 1/10로 줄인다고 해서 메모리가 그에 비례해서 증가하는 것이 아니라, 정확도를 높일수록 메모리 비용이 급격히 커집니다.

### 6.4. 튜닝 가이드

블룸 필터 설정은 시스템의 인프라 특성과 워크로드에 맞추어 조정해야 합니다. 공식 문서가 제시하는 튜닝 가이드는 다음과 같습니다.

- **RAM이 풍부하고 스토리지가 느린 시스템:** 디스크 I/O를 최소화하기 위해 더 낮은 값(예: `0.01`)을 사용하는 것이 유리합니다. 메모리를 더 쓰더라도 느린 디스크 접근을 줄이는 것이 이득이기 때문입니다.
- **리소스(특히 RAM)가 제한적이고 스토리지가 빠른 배포 환경:** 메모리를 아끼기 위해 더 높은 값을 사용할 수 있습니다. 디스크가 빠르므로 거짓 양성으로 인한 추가 I/O의 비용이 상대적으로 작기 때문입니다.
- **읽기가 적거나(read-light) 전체 데이터셋 스캔을 감수할 수 있는 분석(analytical) 워크로드:** 훨씬 더 높은 값을 사용해도 무방합니다. 이런 워크로드는 블룸 필터의 정확도가 성능에 큰 영향을 주지 않기 때문입니다.

### 6.5. 설정 변경

블룸 필터의 거짓 양성 확률을 변경하려면 `ALTER TABLE` 명령을 사용합니다.

```sql
ALTER TABLE keyspace.table WITH bloom_filter_fp_chance=0.01
```

그러나 이 변경은 **새로 기록되는 파일에만 적용**됩니다. 기존의 SSTable들은 컴팩션(compaction)이 일어날 때까지 원래의 블룸 필터를 그대로 유지합니다. 즉, 설정을 바꾸어도 기존 데이터 파일의 블룸 필터는 즉시 바뀌지 않습니다.

변경된 설정을 기존 SSTable에 **즉시 적용**하고 싶다면, 다음 두 명령 중 하나를 사용하여 SSTable을 새로 쓰면서 블룸 필터를 재생성할 수 있습니다.

```
nodetool scrub
```

또는

```
nodetool upgradesstables -a
```

`nodetool upgradesstables -a`는 `-a`(all) 옵션을 사용하여 모든 SSTable을 강제로 다시 쓰게 하며, 이 과정에서 변경된 `bloom_filter_fp_chance` 설정이 반영된 새로운 블룸 필터가 생성됩니다.

---

## 7. 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [Operating - Topology Changes](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/topo_changes.html)
- [Operating - Repair](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/repair.html)
- [Operating - Read Repair](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/read_repair.html)
- [Operating - Hints](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/hints.html)
- [Operating - Hardware Choices](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/hardware.html)
- [Operating - Bloom Filters](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/bloom_filters.html)

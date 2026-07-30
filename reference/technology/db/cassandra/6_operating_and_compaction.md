# Cassandra 운영, 컴팩션, 압축

## Cassandra 운영

> 원본: https://cassandra.apache.org/doc/latest/cassandra/managing/operating/

---

### 목차

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

### 1. 토폴로지 변경(Topology Changes)

분산 데이터베이스인 Cassandra 클러스터를 운영할 때, 운영자는 클러스터의 토폴로지(topology)를 변경해야 하는 경우가 자주 발생합니다. 새로운 노드를 추가하여 용량과 처리량을 늘리거나, 더 이상 필요하지 않거나 고장난 노드를 제거하거나, 죽은 노드를 새 노드로 교체하거나, 토큰 링(token ring) 상에서 노드의 위치를 이동시키는 작업이 여기에 해당합니다. 이 섹션에서는 클러스터 멤버십(membership)을 안전하게 변경하는 방법을 설명합니다.

#### 1.1. 부트스트랩(Bootstrap)

새로운 노드를 클러스터에 추가하는 과정을 **부트스트랩**(bootstrapping)이라고 합니다. 부트스트랩은 새로운 노드가 클러스터에 합류하면서 자신이 책임지게 될 토큰 범위(token range)에 해당하는 데이터를 기존의 복제본(replica) 노드들로부터 스트리밍으로 받아 채워 넣는 과정입니다.

새 노드가 클러스터에 합류하면, 해당 노드는 토큰 링 상에서 자신의 위치를 나타내는 가상 노드(virtual node, vnode)들을 할당받습니다. 노드가 할당받는 가상 노드(토큰)의 수는 `cassandra.yaml`의 `num_tokens` 파라미터로 결정됩니다. `num_tokens`의 기본값은 256이므로, 별도로 설정하지 않으면 노드 하나가 256개의 가상 노드를 가집니다.

부트스트랩 과정에서 새 노드는 자신이 책임지게 될 토큰 범위의 데이터를, 그 데이터를 보유하고 있는 기존 복제본 노드들로부터 스트리밍으로 전송받습니다. 이를 통해 새 노드는 클러스터의 일관성(consistency)을 해치지 않으면서 정상적으로 읽기/쓰기 요청을 처리할 수 있는 상태가 됩니다.

부트스트랩이 완료되기 전까지 새 노드는 클러스터에서 `UP` 상태로 인식되지만, 아직 데이터를 모두 수신하지 못한 상태입니다. 부트스트랩이 완료되어야 정상적인 읽기 요청 라우팅 대상이 됩니다.

#### 1.2. 토큰 할당(Token Allocation)

기본적으로 Cassandra는 새 노드가 부트스트랩할 때 토큰을 무작위로 할당합니다. `num_tokens`가 256처럼 충분히 큰 경우, 무작위 할당만으로도 클러스터 전체에 데이터가 비교적 고르게 분산됩니다.

데이터 부하를 더 정교하게 분산시키려면 토큰 할당 알고리즘을 사용할 수 있습니다. 이 알고리즘은 특정 키스페이스의 복제 전략과 부하를 분석하여, 데이터가 가능한 한 균등하게 분산되도록 토큰을 최적으로 할당합니다.

이 알고리즘을 사용하려면 새 노드를 시작할 때 다음 JVM 옵션을 지정합니다.

```
-Dcassandra.allocate_tokens_for_keyspace=<keyspace>
```

`<keyspace>`는 부하 분석의 기준이 될 키스페이스 이름입니다. 알고리즘은 지정된 키스페이스의 복제 인수(replication factor)를 분석하여, 해당 키스페이스의 데이터가 노드들 사이에 최대한 균등하게 분배되도록 토큰을 선택합니다.

특정 키스페이스에 의존하지 않고 로컬 복제 인수를 직접 지정하여 토큰을 할당받으려면 다음 옵션을 사용합니다.

```
-Dcassandra.allocate_tokens_for_local_replication_factor=<rf>
```

`cassandra.yaml`에서도 `allocate_tokens_for_keyspace` 또는 `allocate_tokens_for_local_replication_factor` 설정으로 동일한 동작을 구성할 수 있습니다.

수동으로 토큰을 지정하려면 `cassandra.yaml`의 `initial_token` 파라미터에 쉼표로 구분된 토큰 목록을 직접 입력합니다. 이 경우 `num_tokens` 값은 `initial_token`에 지정한 토큰 수와 일치해야 합니다. 수동 토큰 할당은 운영자가 토큰 분포를 완전히 제어하고자 하는 고급 사용 사례에서 활용됩니다.

#### 1.3. 부트스트랩 재개(Resuming Bootstrap)

부트스트랩은 많은 양의 데이터를 스트리밍하는 작업이기 때문에 네트워크 문제나 노드 재시작 등의 이유로 도중에 실패할 수 있습니다. 실패한 경우 처음부터 모든 데이터를 다시 스트리밍하는 대신 중단된 지점에서 재개할 수 있습니다.

실패한 부트스트랩을 재개하려면 다음 명령을 사용합니다.

```
nodetool bootstrap resume
```

노드를 단순히 재시작하는 것으로도 부트스트랩을 재개할 수 있습니다. 노드는 이미 성공적으로 스트리밍받은 데이터 범위를 인식하고, 아직 받지 못한 범위에 대해서만 다시 스트리밍을 시도합니다.

#### 1.4. 노드 제거(Removing Nodes)

노드를 제거하는 방법은 해당 노드가 현재 활성(live) 상태인지, 죽은(dead) 상태인지에 따라 달라집니다. 노드를 제거하면 그 노드가 책임지던 토큰 범위가 클러스터의 다른 노드들에게 재분배됩니다.

##### nodetool decommission (살아있는 노드 제거)

제거하려는 노드가 정상적으로 동작 중인 활성 노드라면, 해당 노드에서 직접 다음 명령을 실행합니다.

```
nodetool decommission
```

`decommission`을 실행하면 제거되는 노드가 보유한 데이터를 다른 복제본 노드들로 스트리밍합니다. 이 과정이 완료되면 해당 노드의 토큰 범위가 다른 노드들로 옮겨지고, 그 노드는 클러스터에서 안전하게 제거됩니다.

이 방법은 제거되기 전에 자신의 데이터를 모두 다른 노드에게 넘기므로, 데이터 손실 없이 노드를 제거하는 가장 깔끔한 방법입니다.

##### nodetool removenode (죽은 노드 제거)

제거하려는 노드가 이미 다운된 죽은 노드라면, 클러스터 내의 다른 활성 노드에서 다음 명령을 실행합니다.

```
nodetool removenode <ID>
```

`<ID>`는 제거하려는 죽은 노드의 호스트 ID입니다. 호스트 ID는 `nodetool status` 명령으로 확인할 수 있습니다.

`removenode`를 실행하면 죽은 노드 대신 해당 토큰 범위의 **나머지 복제본 노드들**로부터 데이터를 스트리밍하여 재분배합니다.

`removenode`가 어떤 이유로 멈추는 경우(예: 스트리밍이 정상적으로 완료되지 않는 경우), `force` 옵션으로 강제 완료할 수 있습니다.

```
nodetool removenode force
```

`removenode force`는 토큰 범위의 재분배를 완료하지 않은 채 노드를 클러스터에서 강제 제거합니다. 이로 인해 복제 인수가 일시적으로 부족해질 수 있으므로, 이후 반드시 repair를 실행하여 일관성을 회복해야 합니다.

##### nodetool assassinate (마지막 수단)

`decommission`이나 `removenode`로 노드를 제거할 수 없는 극단적인 상황의 마지막 수단으로 다음 명령을 사용할 수 있습니다.

```
nodetool assassinate <IP>
```

`assassinate`는 데이터 스트리밍이나 일관성 검증 없이 노드를 즉시 강제 제거합니다. 가장 위험한 방법이므로 다른 모든 방법이 실패한 경우에만 사용해야 하며, 사용 후에는 반드시 repair를 실행하여 데이터 일관성을 회복해야 합니다.

#### 1.5. 죽은 노드 교체(Replacing a Dead Node)

죽은 노드를 단순히 제거하는 대신, 같은 토큰 범위를 담당하는 새 노드로 **교체**(replace)해야 하는 경우가 있습니다. 예를 들어 하드웨어가 고장 난 노드를 동일한 역할의 새 하드웨어로 대체하는 상황입니다.

교체 노드를 시작할 때는 다음 JVM 옵션을 지정합니다. 일반적으로 `cassandra-env.sh` 파일이나 `jvm.options` 파일에 추가합니다.

```
-Dcassandra.replace_address_first_boot=<dead_node_ip>
```

`<dead_node_ip>`는 교체 대상 죽은 노드의 IP 주소입니다.

교체 노드는 시작되면 먼저 **휴면(hibernate)** 상태로 진입합니다. 이 상태에서 클러스터의 다른 노드들은 교체 노드를 `DOWN`으로 인식하지만, 교체 노드 자신은 다른 복제본들로부터 죽은 노드의 토큰 범위 데이터를 스트리밍으로 받습니다. 수신이 완료되면 교체 노드는 정상적으로 클러스터에 합류하여 죽은 노드의 역할을 이어받습니다.

`replace_address_first_boot` 옵션은 이전의 `replace_address`와 달리 첫 번째 부팅에서만 적용되고 이후 재시작에서는 무시됩니다. 따라서 교체 완료 후 옵션을 제거하지 않아도 노드가 매번 교체를 시도하는 문제가 발생하지 않습니다.

> **중요(반드시 복구를 실행해야 하는 경우):**
>
> 다음 두 가지 상황에서는 교체가 완료된 후 **반드시 복구(repair)를 실행해야 합니다.**
>
> 1. 교체 대상이었던 죽은 노드가 `max_hint_window`(기본 3시간)보다 더 오랫동안 다운되어 있었던 경우. 이 경우 다운된 동안의 일부 쓰기에 대해 힌트(hint)가 저장되지 않았을 수 있으므로 데이터가 누락되었을 가능성이 있습니다.
> 2. 죽은 노드와 **동일한 IP 주소**로 교체를 진행하는데, 그 교체 과정(부트스트랩)이 `max_hint_window`를 초과하여 진행되는 경우.
>
> 위 상황에서는 누락된 데이터를 메우기 위해 교체 노드에 대해 복구를 수행해야 완전한 일관성을 회복할 수 있습니다.

#### 1.6. 노드 이동(Moving Nodes)

`num_tokens: 1`로 설정된 단일 토큰 클러스터(가상 노드를 사용하지 않는 클러스터)에서는 노드를 토큰 링 상의 다른 위치로 이동시킬 수 있습니다. 이동하려는 노드에서 다음 명령을 실행합니다.

```
nodetool move <new_token>
```

`<new_token>`은 노드가 새롭게 담당하게 될 토큰 값입니다. `move` 명령은 노드의 토큰 위치를 변경하고, 새로운 토큰 범위에 필요한 데이터를 스트리밍하여 채웁니다.

> **참고:** `nodetool move`는 단일 토큰 노드에서만 의미가 있습니다. 가상 노드(`num_tokens`가 1보다 큰 경우)를 사용하는 클러스터에서는 토큰이 여러 개이기 때문에 이 방식의 이동은 적용되지 않습니다.

노드를 이동한 후에는 더 이상 해당 노드가 담당하지 않게 된 데이터가 디스크에 남아 있을 수 있습니다. 이를 제거하려면 다음 절의 `nodetool cleanup`을 실행합니다.

#### 1.7. 진행 상황 모니터링(Monitoring Progress)

부트스트랩, 디커미션, removenode, move 등 모든 토폴로지 변경 작업은 노드 간 데이터 스트리밍을 수반합니다. 스트리밍 진행 상황을 모니터링하려면 다음 명령을 사용합니다.

```
nodetool netstats
```

`nodetool netstats`는 현재 진행 중인 스트리밍 작업의 상태를 보여줍니다. 어떤 노드와 데이터를 주고받고 있는지, 각 스트림에서 전송된 파일과 바이트의 양, 완료까지의 진행률 등을 확인할 수 있으며, 토폴로지 변경이 정상적으로 진행 중인지 또는 멈추어 있는지 파악하는 데 유용합니다.

#### 1.8. Cleanup

노드를 추가하거나 이동하여 토큰 범위가 재분배되면, 기존 노드들은 더 이상 담당하지 않게 된 데이터를 디스크에 그대로 보유하게 됩니다. 이 데이터는 자동으로 삭제되지 않습니다.

불필요한 데이터를 제거하여 디스크 공간을 회수하려면 다음 명령을 실행합니다.

```
nodetool cleanup
```

`cleanup`은 현재 노드가 더 이상 소유하지 않는 토큰 범위의 데이터를 제거합니다. 디스크 I/O를 많이 사용하므로, 일반적으로 새 노드 추가 작업이 모두 완료된 후에 한 번에 수행하는 것이 권장됩니다.

---

### 2. Repair(복구)

분산 데이터베이스에서는 다양한 이유로 노드 간 데이터가 불일치(out of sync) 상태가 될 수 있습니다. 노드가 다운된 동안 발생한 쓰기를 놓치거나, 힌트가 만료되어 적용되지 못하거나, 디스크 손상이나 운영자 실수가 발생할 수 있습니다. **Repair**(복구)는 이러한 데이터 불일치를 해소하여 모든 복제본이 동일한 데이터를 가지도록 만드는 과정입니다.

힌트만으로는 데이터 불일치를 완전히 해소할 수 없습니다. 힌트는 최선 노력(best-effort) 기반의 메커니즘이며, 노드가 `max_hint_window`보다 오래 다운되어 있으면 힌트가 저장되지 않을 수 있기 때문입니다. **궁극적 일관성**(eventual consistency)을 보장하려면 정기적으로 repair를 실행해야 합니다.

#### 2.1. Anti-entropy 복구의 원리

Cassandra의 복구는 **anti-entropy** 방식으로 동작합니다. 기본 아이디어는, 같은 토큰 범위의 데이터를 보유한 노드들이 서로 데이터셋을 비교하여 불일치하는 부분만 찾아내고, 그 차이만 스트리밍하여 동기화하는 것입니다.

데이터셋 전체를 바이트 단위로 비교하는 것은 비효율적이므로, Cassandra는 **머클 트리**(Merkle tree)를 사용합니다. 머클 트리는 데이터를 계층적인 해시 구조로 표현한 트리입니다.

- 트리의 잎(leaf) 노드들은 데이터의 작은 부분 범위(sub-range)에 대한 해시값을 담습니다.
- 상위 노드는 자식 노드들의 해시값을 다시 해싱한 값을 담습니다.
- 결국 트리의 루트(root)는 전체 데이터 범위를 대표하는 단일 해시값이 됩니다.

두 노드가 머클 트리를 교환하여 비교할 때, 루트 해시값이 같으면 데이터가 완전히 동일한 것이므로 추가 비교가 불필요합니다. 루트 해시값이 다르면 트리를 따라 내려가면서 해시값이 다른 가지(branch)만 추적하여 불일치 구간을 효율적으로 찾아냅니다. 식별된 불일치 구간에 대해서만 실제 데이터를 스트리밍하여 동기화합니다.

#### 2.2. Incremental과 Full Repair

Cassandra의 복구에는 크게 두 가지 종류가 있습니다.

##### Incremental Repair(증분 복구) - 기본값

**Incremental repair**(증분 복구)는 Cassandra의 기본 복구 방식입니다. 이전 복구 이후 새로 기록된 데이터만을 대상으로 복구를 수행합니다.

이미 "복구됨(repaired)"으로 표시된 데이터는 다시 복구하지 않으므로 복구에 소요되는 시간과 I/O 비용을 크게 줄일 수 있습니다. 따라서 증분 복구를 정기적으로 자주 실행하는 것이 효율적입니다.

다만 이미 복구됨으로 표시된 데이터를 다시 검사하지 않기 때문에, 이후 디스크 손상이나 운영자 실수가 발생해도 이를 잡아내지 못한다는 한계가 있습니다.

##### Full Repair(전체 복구)

**Full repair**(전체 복구)는 "복구됨" 표시 여부와 관계없이 토큰 범위 내의 **모든 데이터**를 대상으로 복구를 수행합니다.

증분 복구가 효율적이지만, 디스크 손상이나 운영자 실수를 잡아내지 못하는 한계 때문에 **full repair도 주기적으로 실행해야 합니다.** Full repair는 전체 데이터를 검증하므로 디스크 손상이나 누락된 데이터를 포괄적으로 발견할 수 있습니다.

#### 2.3. 사용법과 기본값(Usage and Defaults)

repair는 운영자가 `nodetool` 명령으로 수동으로 실행합니다. Cassandra 자체에는 repair를 자동으로 스케줄링하는 내장 기능이 없으므로, cron 등을 통해 주기적으로 실행하거나 외부 도구(예: Reaper)를 사용해야 합니다.

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

#### 2.4. nodetool repair 옵션

`nodetool repair`는 복구의 범위와 동작 방식을 제어하는 다양한 옵션을 제공합니다.

##### -pr, --partitioner-range (주 토큰 범위만 복구)

```
nodetool repair -pr
```

이 옵션은 복구 대상 노드가 첫 번째 복제본(first replica)인 **주(primary) 토큰 범위**에 대해서만 복구를 수행합니다.

모든 노드가 보유한 전체 토큰 범위를 복구하면 동일한 데이터 범위를 여러 노드가 중복으로 복구하게 됩니다. `-pr` 옵션을 사용하여 각 노드가 자신의 주 범위만 복구하고, 클러스터 모든 노드에 대해 한 번씩 실행하면 전체 데이터를 정확히 한 번씩 복구하여 중복 작업을 피할 수 있습니다.

##### -prv, --preview (미리보기)

```
nodetool repair --preview
```

실제로 데이터를 스트리밍하지 않고, **복구 시 발생할 스트리밍 양을 추정**(estimate)합니다. 실제 repair 전에 노드 간 데이터가 얼마나 어긋나 있는지 미리 파악하는 데 유용합니다.

##### -vd, --validate (검증)

```
nodetool repair --validate
```

**복구됨(repaired)으로 표시된 데이터가 모든 노드에서 동일한지 검증**합니다. 이미 복구되었다고 표시된 데이터가 실제로 모든 복제본에서 일치하는지 확인하여 데이터 무결성을 점검할 수 있습니다.

##### -full (전체 복구)

```
nodetool repair --full
```

증분 복구 대신 전체 복구를 수행하도록 지정합니다.

##### -seq, --sequential (순차 복구)

복제본들을 병렬이 아닌 순차적으로 복구합니다. 한 번에 하나의 복제본만 부하를 받으므로 복구 중에도 나머지 복제본이 요청을 처리할 수 있어 성능 영향을 줄일 수 있습니다. 다만 전체 복구에 걸리는 시간은 더 길어집니다.

##### -dc, --in-dc (특정 데이터센터)

지정한 데이터센터(들) 내의 노드들에 대해서만 복구를 수행합니다.

##### -local, --in-local-dc (로컬 데이터센터만)

복구 대상 노드가 속한 로컬 데이터센터 내의 노드들에 대해서만 복구를 수행합니다. 데이터센터 간 네트워크 트래픽을 발생시키지 않으면서 복구를 수행할 때 유용합니다.

##### -hosts, --in-hosts (특정 호스트)

복구에 참여시킬 특정 호스트(들)를 지정합니다.

##### -j, --job-threads (작업 스레드 수)

하나의 복구 작업 내에서 동시에 복구할 테이블 수를 지정합니다. 기본값은 1이며, 값을 늘리면 복구 속도가 빨라질 수 있지만 노드의 부하도 증가합니다. 최대값은 4입니다.

##### -st / -et (--start-token / --end-token, 부분 범위 복구)

`-st`(시작 토큰)와 `-et`(끝 토큰) 옵션을 사용하면 특정 토큰 범위에 대해서만 복구를 수행할 수 있습니다. 이를 **부분 범위 복구**(subrange repair)라고 하며, 매우 큰 데이터셋을 작은 범위로 나누어 점진적으로 복구할 때 유용합니다.

#### 2.5. 복구 주기(Frequency of Repair)

repair 실행 주기는 클러스터의 워크로드와 운영 환경에 따라 달라지지만, 공식 문서는 다음과 같은 출발점을 제안합니다.

- **증분 복구는 1~3일마다, 전체 복구는 1~3주마다** 실행하는 것을 시작점으로 삼을 수 있습니다.
- 또는 대안으로, **전체 복구를 5일마다** 실행하는 방식도 권장됩니다.

repair 주기에서 가장 중요한 제약은 **gc_grace 기간**(garbage collection grace period)입니다. Cassandra에서 데이터를 삭제하면 즉시 제거되지 않고 톰스톤(tombstone)이라는 삭제 표식이 남습니다. 톰스톤은 `gc_grace_seconds`(기본값 10일) 동안 유지된 후 가비지 컬렉션됩니다.

톰스톤이 가비지 컬렉션되기 전에 삭제가 모든 복제본에 전파되지 못하면, 이미 삭제된 데이터가 다시 살아나는(좀비 데이터) 현상이 발생할 수 있습니다. 이를 방지하려면 **gc_grace 기간이 만료되기 전에 모든 노드에 대해 repair를 완료**해야 합니다.

따라서 최소 권장 사항으로, **gc_grace_seconds 기본값이 10일일 때 모든 노드를 7일 이내에 한 번씩 repair**해야 합니다. 이렇게 하면 gc_grace 만료로 인한 삭제 데이터 부활 문제를 방지할 수 있습니다.

> **요약:** repair는 최소 7일에 한 번 이상(gc_grace 기간 내에) 실행해야 하며, 디스크 손상에 대비하여 전체 복구도 주기적으로 수행해야 합니다.

---

### 3. Read Repair(읽기 복구)

**Read Repair**(읽기 복구)는 읽기 요청을 처리하는 과정에서 데이터 복제본들을 복구하는 메커니즘입니다. 클라이언트가 데이터를 읽을 때 관여하는 복제본들 사이에서 불일치가 발견되면, Cassandra는 즉석에서 복제본들을 일치시키고 클라이언트에게는 가장 최신 데이터를 반환합니다.

Read repair는 anti-entropy repair(`nodetool repair`)와 함께 Cassandra가 데이터 일관성을 유지하는 핵심 메커니즘 중 하나입니다. 다만 실제로 읽힌 데이터에 대해서만 동작하므로, 자주 읽히지 않는 데이터는 복구하지 못합니다. 따라서 read repair는 전체 복구나 노드 교체 절차를 대체할 수 없습니다.

#### 3.1. 단조적 쿼럼 읽기 보장

Cassandra는 **단조적 쿼럼 읽기**(monotonic quorum reads)를 보장하기 위해 블로킹 read repair를 구현합니다.

단조적 쿼럼 읽기란 연속된 쿼럼 읽기가 이전 읽기보다 더 오래된 데이터를 반환하지 않음을 보장하는 것입니다. 즉, 읽기 결과가 시간이 지남에 따라 과거로 되돌아가지 않습니다.

이 보장은 실패한 쓰기가 복제본의 소수(minority)에만 도달한 경우에도 유지되어야 합니다. 예를 들어, 어떤 쓰기가 쿼럼을 만족하지 못해 클라이언트에게 실패로 응답되었지만 일부 복제본에는 기록된 경우, 후속 읽기에서 어떤 복제본이 응답하느냐에 따라 새 값이 보였다 안 보였다 할 수 있습니다. 블로킹 read repair는 이러한 비단조성(non-monotonicity)을 방지합니다.

#### 3.2. 단조적 읽기의 테이블 수준 설정

Cassandra 4.0부터는 테이블 수준에서 read repair 동작을 설정할 수 있는 `read_repair` 테이블 옵션이 도입되었습니다. 이 옵션은 두 가지 값을 가질 수 있습니다.

##### BLOCKING (기본값)

`read_repair`가 `BLOCKING`으로 설정되어 있고 read repair가 시작되면, 해당 읽기는 **다른 복제본들에 전송된 쓰기들이 일관성 수준(CL)을 만족할 때까지 블로킹**됩니다.

- **제공하는 것:** 단조적 쿼럼 읽기(monotonic quorum reads)
- **제공하지 않는 것:** 파티션 수준의 쓰기 원자성(partition-level write atomicity)

##### NONE

`read_repair`가 `NONE`으로 설정되면, 코디네이터는 복제본들 사이의 차이를 조정하여 클라이언트에게 올바른 값을 반환하지만, **복제본들을 실제로 복구하지는 않습니다.**

- **제공하는 것:** 쓰기 원자성(write atomicity)
- **제공하지 않는 것:** 단조적 쿼럼 읽기(monotonic quorum reads)

테이블을 생성할 때 다음과 같이 `read_repair` 옵션을 지정할 수 있습니다.

```sql
CREATE TABLE ks.tbl (k INT, c INT, v INT, PRIMARY KEY (k,c)) with read_repair='NONE'
```

#### 3.3. Read Repair 예시

5개의 노드로 구성되고 복제 인수가 3인 클러스터에서 일관성 수준 `TWO`로 읽기를 수행하는 경우를 예로 들어 read repair 과정을 단계별로 살펴봅니다.

##### 1단계: 직접 읽기 요청(Direct Read Request)

코디네이터는 가장 빠르게 응답할 것으로 예상되는 복제본에게 **전체 데이터(full data)를 요청하는 직접 읽기 요청**을 보냅니다. 이 요청에 대한 응답은 실제 컬럼 데이터를 모두 포함합니다.

##### 2단계: 다이제스트 읽기 요청(Digest Read Requests)

코디네이터는 일관성 수준을 만족하기 위해 추가로 필요한 복제본들에게 **다이제스트 읽기 요청**(digest read request)을 보냅니다. 다이제스트 요청은 실제 데이터 대신 **해시값(hash value)만**을 반환하므로 네트워크 트래픽을 줄일 수 있습니다.

##### 3단계: 비교(Comparison)

코디네이터는 직접 읽기로 받은 데이터의 해시값과 다이제스트 요청들로 받은 해시값들을 비교합니다.

- 해시값들이 모두 **일치**(match)하면, 복제본들의 데이터가 동일하다는 의미이므로 복구가 필요하지 않습니다. 코디네이터는 데이터를 클라이언트에게 반환합니다.

##### 4단계: 복구(Repair, 필요한 경우)

- 해시값들이 **불일치**(mismatch)하면 복제본들의 데이터가 어긋나 있다는 의미입니다. 코디네이터는 다른 복제본들로부터 **전체 데이터**(full data)를 다시 요청합니다.
- 코디네이터는 받은 데이터들의 타임스탬프를 비교하여 가장 최신 값을 판별합니다.
- 최신 데이터를 클라이언트에게 반환합니다.
- 동시에, 오래된 값을 가진 복제본들에게 최신 값을 기록하는 쓰기를 보내 해당 복제본들을 복구합니다.

#### 3.4. 읽기 일관성 수준과 Read Repair

read repair가 수행되는지 여부는 읽기 요청의 일관성 수준에 따라 달라집니다.

- **ONE, LOCAL_ONE:** 첫 번째 직접 읽기 요청 하나로 일관성 수준이 충족되므로 **read repair가 수행되지 않습니다.** (하나의 복제본만 읽으므로 비교 대상이 없습니다.)
- **TWO, THREE, LOCAL_QUORUM, QUORUM:** 여러 복제본의 응답을 비교해야 하므로, **불일치가 감지되면 read repair가 수행됩니다.**

#### 3.5. Cassandra 4.0의 개선된 Read Repair

Cassandra 4.0에서는 블로킹 read repair와 관련하여 두 가지 중요한 개선이 이루어졌습니다.

##### 1) Speculative Retry(추측성 재시도)

응답이 지연될 것으로 판단되면, Cassandra는 아직 연락하지 않은 복제본들에게 추가적인 읽기 요청을 **추측성으로(speculatively)** 보냅니다. 느린 복제본 하나 때문에 전체 읽기가 지연되는 것을 방지하여 지연 시간을 개선합니다.

##### 2) Partial Blocking(부분 블로킹)

이전에는 다이제스트 불일치가 발생하면 모든 관련 작업이 완료될 때까지 블로킹했습니다. Cassandra 4.0에서는 **다이제스트 불일치를 해소하는 데 필요한 만큼만 블로킹**하고, 일관성 수준을 만족하기에 충분한 전체 데이터 응답을 받을 때까지만 기다립니다. 꼭 필요한 만큼만 기다리므로 효율이 향상됩니다.

#### 3.6. Read Repair 진단 이벤트

Cassandra 4.0은 read repair에 대한 **진단 이벤트**(diagnostic events)를 추가했습니다. 이 진단 이벤트들은 다음 정보를 노출하여 운영자가 read repair 동작을 관찰하고 디버깅할 수 있도록 합니다.

- 연락된 엔드포인트(contacted endpoints)
- 다이제스트 응답(digest responses)
- 영향을 받은 파티션 키(affected partition keys)
- 추측성으로 수행된 작업(speculated operations)
- 업데이트의 크기(update sizes)

#### 3.7. 백그라운드 Read Repair 제거

Cassandra 4.0에서는 **백그라운드 read repair**(background read repair)가 **제거**되었습니다.

이전 버전에서 백그라운드 read repair는 `cassandra.yaml`의 `read_repair_chance`와 `dclocal_read_repair_chance` 설정으로 구성되었습니다. 이 설정들은 읽기가 발생할 때 일정 확률로, 일관성 수준을 만족하기 위해 필요한 복제본보다 더 많은 복제본을 백그라운드에서 읽고 비교하여 복구하는 방식이었습니다. Cassandra 4.0부터는 이 두 설정과 백그라운드 read repair 기능이 모두 제거되었습니다.

> **중요:** read repair는 full repair(`nodetool repair`)나 노드 교체 절차를 대체할 수 없습니다. read repair는 실제로 읽힌 데이터만 복구하므로, 데이터 전반의 일관성을 보장하려면 정기적인 anti-entropy repair가 반드시 필요합니다.

---

### 4. Hinted Handoff(힌트 전달)

**Hinted Handoff**(힌트 전달)는 쓰기 작업 도중에 동작하는 복구 메커니즘입니다. 노드 장애나 유지보수로 인해 복제본 노드가 일시적으로 사용 불가능한 상태가 되었을 때, 코디네이터는 그 노드로 향했어야 할 쓰기 데이터를 **힌트**(hint)라는 형태로 로컬에 임시 저장해 두었다가, 해당 노드가 복구되면 힌트를 다시 적용(replay)합니다.

힌트는 데이터 불일치가 지속되는 기간을 줄이는 중요한 방법입니다. 노드가 잠시 다운되었다 복구되면 힌트가 없을 때 다운 중에 발생한 모든 쓰기를 놓쳐 다른 노드들과 데이터가 어긋나게 됩니다. 힌트는 이러한 격차를 빠르게 메워줍니다.

#### 4.1. Hinted Handoff의 동작 방식

힌트가 저장되고 다시 적용되는 과정을 시간 순서대로 살펴보면 다음과 같습니다.

- **(t0)** 클라이언트가 쓰기 요청을 보냅니다. 코디네이터는 이 쓰기를 세 개의 복제본에 전송하지만, 그 중 하나가 재시작 중이어서 사용 불가능합니다.
- **(t1)** 나머지 복제본들이 쓰기를 성공적으로 처리하여 쿼럼을 만족하면, 코디네이터는 클라이언트에게 쿼럼 확인 응답을 보냅니다. 한 복제본이 다운되어도 쿼럼을 만족하면 쓰기는 클라이언트에게 성공으로 응답됩니다.
- **(t2)** 쓰기 타임아웃(기본값 2초)이 경과한 후에도 해당 복제본에 도달할 수 없다면, 코디네이터는 그 복제본을 위한 힌트를 로컬에 저장합니다.
- **(t3)** 사용 불가능했던 복제본이 재시작되어 가십(gossip) 메시지를 통해 `UP` 상태가 되었음을 클러스터에 알립니다.
- **(t4)** 코디네이터는 가십을 통해 해당 복제본이 복구된 것을 감지하고, 저장해 두었던 힌트 변이(mutation)들을 그 복제본에 다시 적용(replay)합니다.

#### 4.2. 힌트의 적용(Application of Hints)

코디네이터가 저장된 힌트를 복구된 복제본에 적용할 때, 힌트는 **세그먼트(segment) 단위로 대량** 대상 복제본 노드에 스트리밍됩니다. 대상 노드는 받은 세그먼트를 로컬에 재생(replay)하여 적용합니다. 세그먼트를 모두 재생하고 나면 그 세그먼트를 삭제하고 다음 세그먼트를 받습니다.

힌트는 세그먼트 단위로 순차적으로 전송·적용되며, 적용 완료된 세그먼트는 즉시 삭제되어 디스크 공간을 회수합니다.

힌트의 적용은 **멱등**(idempotent)하며, **이후의 변이(future mutation)를 덮어쓸 수 없습니다.** 힌트에는 원래 쓰기의 타임스탬프가 그대로 보존되어 있어, 힌트 적용 시 그 타임스탬프를 기준으로 처리됩니다. 그 사이에 더 최신의 쓰기가 이미 적용되어 있다면, 오래된 타임스탬프를 가진 힌트는 최신 데이터를 덮어쓰지 못합니다. 따라서 힌트를 여러 번 적용하거나 늦게 적용해도 데이터 정확성이 깨지지 않습니다.

#### 4.3. 힌트의 디스크 저장

힌트는 기본적으로 `$CASSANDRA_HOME/data/hints` 디렉터리에 플랫 파일 형태로 저장됩니다.

각 힌트는 다음과 같은 정보를 포함합니다.

- **힌트 ID**: 힌트를 식별하는 고유 ID
- **대상 복제본 노드**: 이 힌트가 적용되어야 할 노드의 식별자
- **직렬화된 변이 블롭(serialized mutation blob)**: 실제로 적용될 쓰기 변이의 직렬화된 데이터
- **변이 타임스탬프**: 원래 쓰기가 발생한 시점의 타임스탬프
- **Cassandra 버전**: 힌트가 생성된 Cassandra 버전

힌트는 기본적으로 `LZ4Compressor`를 사용하여 압축되므로 디스크 사용량을 줄일 수 있습니다.

#### 4.4. 타임아웃된 쓰기 요청에 대한 힌트

힌트는 노드가 다운된 경우뿐만 아니라, **타임아웃된 쓰기 요청**에 대해서도 저장됩니다. 복제본이 완전히 다운되지 않았더라도 쓰기 요청에 응답을 제때 하지 못하면(타임아웃), 코디네이터는 그 쓰기에 대한 힌트를 저장합니다.

힌트가 생성되는 시점은 `write_request_timeout` 설정에 의해 결정됩니다. 기본값은 `2000ms`(2초)이며, 설정 가능한 최솟값은 `10ms`입니다.

#### 4.5. 힌트 설정(Configuring Hints)

힌트의 동작은 `cassandra.yaml`의 여러 설정을 통해 구성할 수 있습니다.

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

#### 4.6. 런타임에서 힌트 설정

`nodetool` 명령을 사용하면 노드를 재시작하지 않고 런타임에서 힌트 관련 설정을 변경하거나 제어할 수 있습니다. 이 명령들은 `cassandra.yaml`의 설정을 일시적으로 재정의합니다.

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

#### 4.7. Hinted Handoff를 견고하게 만들기

##### 전달 스로틀(Throttle) 조정

기본값인 1024 KiB/s의 힌트 전달 스로틀은 대부분의 최신 네트워크 환경에서 보수적인 값입니다.

쓰기 처리량이 매우 높아 힌트가 대량으로 쌓이는 경우, 기본 스로틀로는 힌트를 모두 전달하는 데 오랜 시간이 걸릴 수 있습니다. 공식 문서의 예시는 다음과 같습니다.

> 노드당 100 Mbps의 데이터 수집(ingestion)이 일어나는 환경에서, 어떤 노드가 10분 동안 재시작되었다고 가정합니다. 이 경우 그 노드를 위해 약 **7 GiB의 힌트**가 쌓이게 됩니다. 이를 기본 스로틀인 1024 KiB/s로 전달(playback)하면 모두 적용하는 데 **약 2시간**이 걸립니다.

힌트 적용이 너무 느린 경우 `nodetool sethintedhandoffthrottlekb` 명령으로 런타임에서 스로틀 값을 동적으로 높일 수 있습니다. 네트워크 대역폭에 여유가 있다면 이 값을 늘려 힌트를 더 빠르게 적용하고 일관성을 빨리 회복할 수 있습니다.

##### 노드가 더 오래 다운될 수 있도록 허용하기

기본적으로 노드가 `max_hint_window`(3시간)보다 오래 다운되면 그 노드에 대한 힌트 저장이 중단됩니다. 계획된 유지보수 등으로 노드가 더 오래 다운될 것이 예상되는 경우, `nodetool setmaxhintwindow`(Cassandra 4.0에서 추가됨) 명령으로 힌트 윈도우를 런타임에서 동적으로 확장할 수 있습니다.

다만 힌트 윈도우를 늘리면 더 많은 힌트가 디스크에 쌓이므로, 디스크 공간이 충분한지 확인해야 합니다.

#### 4.8. Hinted Handoff의 한계

힌트는 **최선 노력(best-effort) 기반의 일관성 메커니즘**이며, anti-entropy repair가 제공하는 수준의 **궁극적 일관성(eventual consistency)을 보장하지 않습니다.**

힌트가 일관성을 보장하지 못하는 경우는 다음과 같습니다.

- 노드가 `max_hint_window`보다 오래 다운되어 있으면, 이후의 쓰기에 대한 힌트는 저장되지 않습니다.
- 힌트를 저장하던 코디네이터 노드 자체가 죽으면, 해당 코디네이터의 힌트도 함께 사라집니다.
- 힌트 윈도우 내에 노드가 복구되지 못하면 데이터가 누락됩니다.

따라서 힌트는 **full repair를 대체할 수 없습니다.** 힌트는 일시적인 노드 다운으로 인한 단기 불일치를 빠르게 메우는 보조 메커니즘일 뿐이며, 진정한 데이터 일관성을 보장하려면 정기적인 `nodetool repair`가 반드시 필요합니다.

---

### 5. 하드웨어 선택(Hardware Choices)

Cassandra의 처리량은 더 많은 CPU 코어, RAM, 빠른 디스크를 사용할수록 향상됩니다.

- **최소 프로덕션 배포:** 최소 2개의 코어와 8GB RAM이 필요합니다.
- **일반적인 프로덕션 서버:** 8개 이상의 코어와 32GB 이상의 RAM을 갖춥니다.

#### 5.1. CPU

Cassandra는 여러 CPU 코어에 걸쳐 작업을 동시에 처리하도록 설계된 고동시성 시스템입니다. 코어를 추가하면 읽기와 쓰기 처리량이 모두 증가합니다.

특히 Cassandra의 **쓰기 경로(write path)는 고도로 최적화**되어 있습니다. 쓰기는 먼저 커밋로그(commitlog)에 기록된 후 멤테이블(memtable)에 삽입되는 방식으로 처리됩니다. 이 과정은 디스크 탐색이 거의 없는 순차적인 작업이므로 쓰기는 특히 **CPU 바운드(CPU bound)** 경향이 있습니다. 쓰기 성능을 높이려면 더 빠르고 많은 CPU 코어가 효과적입니다.

#### 5.2. 메모리(Memory)

Cassandra는 Java VM 위에서 동작합니다. JVM은 미리 할당된 힙(heap) 메모리를 사용하며, 다음과 같은 용도로 **힙 외부(off-heap)** 메모리도 사용합니다.

- 압축 메타데이터(compression metadata)
- 블룸 필터(bloom filters)
- 캐시(caches)
- 운영체제의 페이지 캐시(OS page caching)

주요 권장 사항은 다음과 같습니다.

- **ECC RAM을 항상 사용합니다.** Cassandra는 비트 수준 손상으로부터 보호하는 내부 안전장치가 거의 없으므로, 메모리 오류를 자동으로 감지하고 정정하는 ECC RAM 사용이 중요합니다.
- **힙 크기는 최소 2GB, 최대 시스템 RAM의 50**%로 설정합니다. 힙이 너무 크면 GC 일시정지가 길어지고 OS 페이지 캐시에 사용할 메모리가 부족해집니다.
- **힙 크기가 12GB 미만인 경우:** ParNew/ConcurrentMarkSweep(CMS) 가비지 컬렉터를 고려합니다.
- **힙 크기가 12GB를 초과하는 경우:** 16GB 힙에 8~10GB의 new generation을 할당하거나, G1GC를 고려합니다.

> Cassandra는 OS 페이지 캐시를 적극적으로 활용하므로, 시스템 RAM의 상당 부분을 OS가 파일 캐싱에 사용할 수 있도록 남겨두는 것이 성능에 중요합니다.

#### 5.3. 디스크(Disks)

Cassandra는 디스크에 두 종류의 데이터를 기록합니다.

1. **커밋로그(commitlog):** 충돌 복구(crash recovery)를 위한 로그입니다. 모든 쓰기는 먼저 커밋로그에 순차적으로 추가됩니다.
2. **데이터 디렉터리(data directory):** SSTable 형태로 데이터를 영구 저장하는 곳입니다.

Cassandra는 HDD(회전식 하드 디스크)와 SSD(솔리드 스테이트 디스크) 모두에서 잘 동작합니다.

##### 커밋로그와 데이터 디렉터리의 분리

**커밋로그와 데이터 디렉터리는 서로 다른 물리 디스크에 두어야 합니다.**

분리하면 쓰기가 디스크 플래터에서 탐색(seek) 없이 커밋로그에 순차적으로 추가되는 이점을 온전히 누릴 수 있습니다. 같은 디스크에 두면 커밋로그의 순차 쓰기와 데이터 디렉터리의 무작위 접근이 경합하여 성능이 저하될 수 있습니다.

##### 디스크 사용량 권장 사항

- 컴팩션(compaction)이 동작할 여유 공간 확보를 위해 디스크 사용률을 적정 수준 이하로 유지해야 합니다.
- SSD를 사용하는 경우, 성능 유지를 위해 TRIM이 정상적으로 동작하도록 권장합니다.
- 네트워크 기반 스토리지(NFS, SAN 등)는 지연 시간이 길고 단일 장애 지점(SPOF)이 될 수 있으므로 일반적으로 권장하지 않습니다.

##### RAID 구성

- **RAID0 또는 JBOD**가 RAID1이나 RAID5보다 선호됩니다.
- Cassandra가 이미 복제(replication)를 통해 데이터 중복성을 제공하므로 RAID 수준의 중복성이 불필요하며, RAID1/RAID5는 쓰기 성능을 저하시키고 가용 용량을 줄입니다.
- RAID0이나 JBOD는 여러 디스크의 처리량을 합쳐 높은 성능을 제공하며, 디스크 하나가 고장 나도 Cassandra의 복제가 데이터를 보호합니다.

#### 5.4. 클라우드 인스턴스 선택

AWS와 같은 클라우드 환경에서 Cassandra를 운영할 때 자주 사용되는 인스턴스 유형은 다음과 같습니다.

- **i2 인스턴스:** CPU 대비 높은 RAM 비율과 로컬 SSD를 제공합니다.
- **i3 인스턴스:** NVMe 기반의 고성능 로컬 스토리지를 제공합니다.
- **m4.2xlarge / c4.4xlarge + EBS GP2 스토리지:** EBS GP2 스토리지와 함께 사용하는 범용 인스턴스 조합입니다.

일반적으로 디스크와 네트워크 성능은 인스턴스 크기와 세대가 커질수록 향상되므로, 더 크거나 최신 세대의 인스턴스를 선택하면 더 나은 I/O 및 네트워크 성능을 기대할 수 있습니다.

---

### 6. Bloom Filter(블룸 필터)

#### 6.1. 블룸 필터의 동작 방식

읽기 경로(read path)에서 Cassandra는 디스크의 SSTable과 RAM의 memtable을 병합하여 결과를 만들어냅니다. 요청된 파티션을 찾기 위해 모든 SSTable 데이터 파일을 일일이 확인하는 것은 매우 비효율적입니다.

이를 피하기 위해 Cassandra는 **블룸 필터**(bloom filter)를 사용합니다. 블룸 필터는 주어진 SSTable 파일에 특정 파티션 데이터가 존재할 가능성이 있는지 빠르게 판단합니다. 판단 결과는 다음 두 가지 중 하나입니다.

- 해당 데이터가 그 파일에 **확실히 존재하지 않음.**
- 해당 데이터가 그 파일에 **아마도 존재함.**

블룸 필터는 **확률적 자료구조**(probabilistic data structure)입니다. 데이터가 존재한다는 것을 보장하지는 못하지만(거짓 양성, false positive 발생 가능), 데이터가 존재하지 않는다는 것은 확실하게 판단할 수 있습니다.

블룸 필터가 "확실히 존재하지 않음"이라고 판단하면 Cassandra는 그 SSTable 파일을 건너뛸 수 있어 불필요한 디스크 I/O를 절약합니다.

#### 6.2. 기본 설정값

블룸 필터의 거짓 양성 확률은 테이블별로 `bloom_filter_fp_chance` 설정으로 조정할 수 있으며, 0과 1 사이의 실수(float) 값을 사용합니다.

`bloom_filter_fp_chance`의 기본값은 컴팩션 전략에 따라 달라집니다.

- **LeveledCompactionStrategy를 사용하는 테이블:** 기본값 `0.1`
- **그 외의 모든 경우:** 기본값 `0.01`

값이 작을수록(예: 0.01) 거짓 양성이 줄어 디스크 I/O가 감소하지만 더 많은 메모리를 사용합니다. 값이 클수록(예: 0.1) 메모리는 적게 쓰지만 거짓 양성이 늘어나 불필요한 디스크 I/O가 증가할 수 있습니다.

#### 6.3. 메모리 사용량

블룸 필터는 RAM에 저장되지만 **힙 외부**(off-heap)에 위치합니다. 따라서 최대 힙 크기를 결정할 때 블룸 필터의 메모리 사용량을 별도로 고려할 필요가 없습니다.

다만 정확도를 높이면 메모리 사용량이 **비선형적으로** 증가합니다. 구체적으로 **`bloom_filter_fp_chance`가 0.01인 블룸 필터는 0.1인 경우보다 약 3배 더 많은 메모리를 사용**합니다.

#### 6.4. 튜닝 가이드

블룸 필터 설정은 시스템의 인프라 특성과 워크로드에 맞춰 조정해야 합니다.

- **RAM이 풍부하고 스토리지가 느린 시스템:** 디스크 I/O를 최소화하기 위해 낮은 값(예: `0.01`)이 유리합니다. 메모리를 더 써도 느린 디스크 접근을 줄이는 것이 이득입니다.
- **RAM이 제한적이고 스토리지가 빠른 환경:** 메모리 절약을 위해 높은 값을 사용할 수 있습니다. 디스크가 빠르므로 거짓 양성으로 인한 추가 I/O 비용이 상대적으로 작습니다.
- **읽기가 적거나(read-light) 전체 데이터셋 스캔을 감수할 수 있는 분석 워크로드:** 훨씬 더 높은 값을 사용해도 무방합니다.

#### 6.5. 설정 변경

블룸 필터의 거짓 양성 확률을 변경하려면 `ALTER TABLE` 명령을 사용합니다.

```sql
ALTER TABLE keyspace.table WITH bloom_filter_fp_chance=0.01
```

이 변경은 **새로 기록되는 파일에만 적용**됩니다. 기존 SSTable들은 컴팩션이 발생할 때까지 원래의 블룸 필터를 유지합니다.

변경된 설정을 기존 SSTable에 **즉시 적용**하려면 다음 두 명령 중 하나를 사용하여 SSTable을 새로 쓰고 블룸 필터를 재생성할 수 있습니다.

```
nodetool scrub
```

또는

```
nodetool upgradesstables -a
```

`nodetool upgradesstables -a`는 `-a`(all) 옵션으로 모든 SSTable을 강제로 다시 쓰면서, 변경된 `bloom_filter_fp_chance` 설정이 반영된 새로운 블룸 필터를 생성합니다.

---

### 7. 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [Operating - Topology Changes](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/topo_changes.html)
- [Operating - Repair](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/repair.html)
- [Operating - Read Repair](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/read_repair.html)
- [Operating - Hints](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/hints.html)
- [Operating - Hardware Choices](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/hardware.html)
- [Operating - Bloom Filters](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/bloom_filters.html)

---

## Cassandra 컴팩션과 압축

> 원본: https://cassandra.apache.org/doc/latest/cassandra/managing/operating/

---

### 목차

1. [컴팩션(Compaction) 개요](#1-컴팩션compaction-개요)
   - [컴팩션이란 무엇인가](#11-컴팩션이란-무엇인가)
   - [컴팩션이 필요한 이유](#12-컴팩션이-필요한-이유)
   - [컴팩션이 달성하는 것](#13-컴팩션이-달성하는-것)
   - [컴팩션 동작 원리](#14-컴팩션-동작-원리)
2. [컴팩션 작업의 종류](#2-컴팩션-작업의-종류)
3. [툼스톤(Tombstone)](#3-툼스톤tombstone)
   - [툼스톤이란 무엇인가](#31-툼스톤이란-무엇인가)
   - [툼스톤이 필요한 이유](#32-툼스톤이-필요한-이유)
   - [좀비(Zombie)](#33-좀비zombie)
   - [유예 기간(Grace Period)](#34-유예-기간grace-period)
   - [툼스톤 제거 조건](#35-툼스톤-제거-조건)
   - [완전히 만료된 SSTable](#36-완전히-만료된-sstable)
4. [TTL(Time-To-Live)](#4-ttltime-to-live)
5. [복구된/복구되지 않은 데이터(Repaired/Unrepaired Data)](#5-복구된복구되지-않은-데이터repairedunrepaired-data)
6. [데이터 디렉터리(Data Directories)](#6-데이터-디렉터리data-directories)
7. [단일 SSTable 툼스톤 컴팩션](#7-단일-sstable-툼스톤-컴팩션)
8. [모든 전략에 공통된 컴팩션 옵션](#8-모든-전략에-공통된-컴팩션-옵션)
9. [컴팩션 관련 nodetool 명령어](#9-컴팩션-관련-nodetool-명령어)
10. [JMX를 통한 전략 전환](#10-jmx를-통한-전략-전환)
11. [Size Tiered 컴팩션 전략(STCS)](#11-size-tiered-컴팩션-전략stcs)
12. [Leveled 컴팩션 전략(LCS)](#12-leveled-컴팩션-전략lcs)
13. [Time Window 컴팩션 전략(TWCS)](#13-time-window-컴팩션-전략twcs)
14. [압축(Compression)](#14-압축compression)
    - [압축이란 무엇인가](#141-압축이란-무엇인가)
    - [압축 알고리즘의 트레이드오프](#142-압축-알고리즘의-트레이드오프)
    - [사용 가능한 압축기(Compressor)](#143-사용-가능한-압축기compressor)
    - [공통 압축 옵션](#144-공통-압축-옵션)
    - [압축기별 전용 옵션](#145-압축기별-전용-옵션)
    - [압축 설정 예제(CQL)](#146-압축-설정-예제cql)
    - [압축 변경 적용 방법](#147-압축-변경-적용-방법)
    - [압축의 이점](#148-압축의-이점)
    - [적합한 사용 사례와 부적합한 사용 사례](#149-적합한-사용-사례와-부적합한-사용-사례)
    - [운영 시 고려사항](#1410-운영-시-고려사항)
15. [참고 자료](#15-참고-자료)

---

### 1. 컴팩션(Compaction) 개요

#### 1.1 컴팩션이란 무엇인가

컴팩션(Compaction)은 Cassandra가 주기적으로 SSTable들을 병합(merge)하고 오래된 데이터를 폐기하는 과정입니다. 데이터는 멤테이블(memtable)에서 시작되며, 메모리 임계값(memory threshold)에 도달하면 디스크의 불변(immutable) SSTable 파일로 기록됩니다. SSTable은 수정할 수 없기 때문에, 업데이트(update)와 삭제(deletion)는 기존 데이터를 덮어쓰지 않고 갱신된 타임스탬프를 가진 새로운 SSTable을 생성합니다. 이러한 누적으로 인해 데이터베이스 상태를 유지하려면 주기적인 병합이 필요합니다.

#### 1.2 컴팩션이 필요한 이유

읽기 작업 중에 SSTable이 참조되므로 SSTable 개수를 적게 유지하는 것이 중요합니다. 쓰기 작업은 SSTable 수를 늘리기 때문에 컴팩션이 필수적입니다. 데이터 삭제는 툼스톤(tombstone) 처리 외에도 TTL(Time-To-Live) 만료 및 명시적 삭제(explicit delete)를 통해서도 발생하며, 이 모든 요인이 컴팩션의 필요성을 만들어 냅니다.

#### 1.3 컴팩션이 달성하는 것

주요 이점은 두 가지입니다: 성능 향상(performance improvement)과 디스크 공간 회수(disk space reclamation)입니다. SSTable에 읽어야 할 중복 데이터가 있으면 읽기 작업이 느려지지만, 툼스톤과 중복이 제거되면 읽기 성능이 향상됩니다. 또한 컴팩션으로 SSTable 크기가 줄어들어 디스크 공간도 절약됩니다.

#### 1.4 컴팩션 동작 원리

컴팩션은 SSTable 집합을 대상으로 동작합니다. 각 고유 행(unique row)의 모든 버전을 수집하고, 타임스탬프 기준으로 가장 최신 버전을 사용하여 하나의 완전한 행을 조립합니다.

병합 과정의 성능이 좋은 이유는 각 SSTable 내에서 행들이 파티션 키(partition key)로 정렬되어 있어 랜덤 I/O를 피할 수 있기 때문입니다. 새로운 행 버전은 새 SSTable에 기록되며, 오래된 버전은 대기 중인 읽기(pending reads)가 완료될 때까지 원본 SSTable에 유지됩니다.

---

### 2. 컴팩션 작업의 종류

**마이너 컴팩션(Minor Compaction)** - 다음과 같은 경우에 자동으로 트리거됩니다: SSTable이 플러시(flush)될 때, 자동 컴팩션(autocompaction)이 다시 활성화될 때, 컴팩션이 새로운 SSTable을 추가할 때, 또는 5분마다 수행되는 자동 점검(automatic checks) 중에 트리거됩니다.

**메이저 컴팩션(Major Compaction)** - 노드의 모든 SSTable에 걸쳐 사용자가 실행하는(user-executed) 컴팩션입니다.

**사용자 정의 컴팩션(User-Defined Compaction)** - 특정 SSTable 집합에 대해 사용자가 트리거하는(user-triggered) 컴팩션입니다.

**스크럽(Scrub)** - SSTable 복구를 시도하는 컴팩션입니다. 손상된(corrupted) 유효 데이터를 제거할 가능성이 있으므로, 이후 전체 복구(full repair)가 필요합니다.

**UpgradeSSTables** - 메이저 버전 업그레이드(major version upgrade) 이후 SSTable을 업그레이드하는 컴팩션입니다.

**클린업(Cleanup)** - 노드가 더 이상 소유하지 않는 범위(range)를 제거하는 컴팩션으로, 일반적으로 부트스트래핑(bootstrapping) 이후 인접 노드(neighboring nodes)에서 트리거됩니다.

**보조 인덱스 재구축(Secondary Index Rebuild)** - 보조 인덱스(secondary index)를 재구축할 때 트리거되는 컴팩션입니다.

**안티컴팩션(Anticompaction)** - 복구(repair) 이후 복구된 범위(repaired ranges)를 원본 SSTable로부터 분리하는 컴팩션입니다.

**서브 레인지 컴팩션(Sub Range Compaction)** - `nodetool compact -st x -et y`를 사용하여 특정 토큰 범위(token range)를 대상으로 하는 컴팩션입니다. 과도한 업데이트나 삭제(excessive updates or deletes)로 오작동하는 토큰(misbehaving tokens)에 유용합니다.

#### 컴팩션 전략 개요

**통합 컴팩션 전략(Unified Compaction Strategy, UCS)** - 대부분의 워크로드에 권장됩니다. 불변 시계열 데이터(immutable time-series data), 업데이트/삭제가 많은(update/delete-heavy) 워크로드, 회전식 디스크(spinning disks), SSD를 모두 처리합니다.

**Size Tiered 컴팩션 전략(Size Tiered Compaction Strategy, STCS)** - 기본(default) 전략입니다. 엄격하게 시계열이 아닌(non-strictly time-series) 워크로드로서 회전식 디스크를 사용하거나, LCS의 I/O가 과도할 때 적합합니다.

**Leveled 컴팩션 전략(Leveled Compaction Strategy, LCS)** - 읽기가 많은(read-heavy) 워크로드 또는 광범위한 업데이트/삭제가 있는 워크로드에 최적화되어 있습니다. 불변 시계열 데이터에는 부적합합니다.

**Time Window 컴팩션 전략(Time Window Compaction Strategy, TWCS)** - TTL이 적용되고 대부분 불변인(mostly immutable) 시계열 데이터를 위해 설계되었습니다.

---

### 3. 툼스톤(Tombstone)

#### 3.1 툼스톤이란 무엇인가

Cassandra는 삭제를 삽입으로 취급하며, 툼스톤(tombstone)이라는 타임스탬프 기반 삭제 마커를 삽입합니다. 툼스톤은 내장된 만료 일자를 가지며, Cassandra의 일반 쓰기 경로(write path)를 통해 여러 노드의 SSTable에 기록됩니다. 쿼리는 툼스톤 삽입 이전 타임스탬프의 값을 무시합니다.

TTL이 표시된 데이터(TTL-marked data) 역시 만료 시 툼스톤과 동일한 처리를 받습니다.

#### 3.2 툼스톤이 필요한 이유

툼스톤 방식은 Cassandra의 분산 아키텍처에 적합한 방식입니다. 값을 즉시 제거하는 대신, 툼스톤을 통해 복제된 데이터 전체에서 안전하게 삭제하면서 데이터 부활(data resurrection)을 방지할 수 있습니다.

#### 3.3 좀비(Zombie)

다중 노드 클러스터에서는 복제본(replica)이 삭제 명령을 놓칠 수 있습니다. 응답하지 않는 노드는 삭제 이전 버전의 데이터를 보존합니다. 툼스톤 처리된 객체가 해당 노드가 복구되기 전에 다른 곳에서 이미 제거되었다면, 살아남은 삭제 데이터—이를 "좀비(zombie)"라고 부릅니다—가 새 데이터로서 클러스터 전체에 전파됩니다.

#### 3.4 유예 기간(Grace Period)

툼스톤의 유예 기간(grace period)은 테이블 속성 `WITH gc_grace_seconds`로 설정합니다. 기본값은 864000초(10일)입니다. 이 기간은 응답하지 않는 노드가 복구될 시간을 확보하기 위한 것입니다. 유예 기간 동안 새로운 업데이트는 툼스톤을 덮어쓰며, 읽기 작업은 툼스톤을 무시합니다. 유예 기간이 만료된 후에야 컴팩션이 툼스톤을 제거할 수 있습니다.

노드가 유예 기간 만료 이후에 복구되는 경우, Cassandra는 힌티드 핸드오프(hinted handoff)를 통해 툼스톤 처리된 객체의 변경(mutation)을 재생(replay)하지 않습니다.

#### 3.5 툼스톤 제거 조건

툼스톤 제거를 위해서는 다음이 필요합니다:
- 툼스톤의 나이(age)가 `gc_grace_seconds`를 초과해야 합니다.
- 컴팩션 범위에 툼스톤을 포함하는 SSTable과, 해당 파티션의 더 오래된 데이터를 가진 모든 SSTable이 함께 포함되어야 합니다.
- `only_purge_repaired_tombstones`가 활성화된 경우, 툼스톤 제거 전에 데이터가 복구(repair)되어 있어야 합니다.

**툼스톤이 없는 삭제(Deletes Without Tombstones)** - 3개 노드 클러스터 예시: 노드 하나가 실패한 상태에서 삭제가 단순히 값만 제거한다면, repair 이후 삭제된 데이터가 좀비로 부활합니다.

**툼스톤이 있는 삭제(Deletes With Tombstones)** - 값 [A]를 가진 3개의 복제 노드에서 삭제 시 툼스톤이 추가됩니다: [A, Tombstone[A]]. repair는 데이터를 부활시키는 대신 툼스톤을 전파(propagate)하여 올바른 삭제 상태를 유지합니다.

#### 3.6 완전히 만료된 SSTable(Fully Expired SSTables)

툼스톤만 담고 있는 SSTable(만료된 TTL 데이터 포함)은, 다른 SSTable의 데이터를 가리지 않는다는 것이 보장되는 경우 컴팩션 중에 폐기(drop)될 수 있습니다. `sstableexpiredblockers` 도구는 폐기 가능한 SSTable과 이를 막고 있는 의존성을 식별합니다. `TimeWindowCompactionStrategy`는 `unsafe_aggressive_sstable_expiration` 옵션을 통해 shadowing 보장을 우회하여 TTL이 적용된 SSTable을 적극적으로 만료시킬 수 있습니다.

---

### 4. TTL(Time-To-Live)

데이터에는 자동 만료를 위한 TTL 속성을 지정할 수 있습니다. TTL이 만료되면 데이터는 최소 `gc_grace_seconds` 동안 유지되는 툼스톤으로 변환됩니다. TTL 데이터와 비-TTL 데이터를 혼합하면 툼스톤 폐기가 복잡해지는데, 파티션이 컴팩션되지 않은 여러 SSTable에 걸쳐 분산될 수 있기 때문입니다.

---

### 5. 복구된/복구되지 않은 데이터(Repaired/Unrepaired Data)

증분 복구(incremental repair)는 복구된 데이터(repaired data)와 복구되지 않은 데이터(unrepaired data)를 추적해야 합니다. 안티컴팩션(anticompaction)은 이들을 별도의 컴팩션 전략 인스턴스를 가진 독립적인 SSTable로 분리합니다. 증분 복구가 한 번만 실행되고 반복되지 않으면, 오래된 복구된 데이터가 더 최신의 복구되지 않은 SSTable에 있는 툼스톤의 삭제를 막을 수 있습니다.

---

### 6. 데이터 디렉터리(Data Directories)

데이터 부활을 방지하기 위해, 툼스톤과 실제 데이터는 항상 동일한 데이터 디렉터리에 위치합니다. 이는 디스크 장애로 SSTable이 손실될 때 데이터 부활을 방지합니다. 컴팩션 전략 인스턴스는 복구/미복구 인스턴스 외에도 데이터 디렉터리마다 별도로 실행됩니다. 데이터 디렉터리가 4개라면 전략 인스턴스는 8개가 실행됩니다.

이점은 다음과 같습니다:
- 병렬 컴팩션(parallel compaction) 가능; Leveled Compaction은 디렉터리별로 별도의 레벨링(separate levelings)을 유지합니다.
- 단일 데이터 디렉터리의 백업/복원(backup/restoration) 가능.
- 현재 모든 디렉터리는 동등하게(equal) 취급됩니다. 디스크 크기가 일치하지 않는(mismatched disk sizes) 경우에 대한 우회책(workaround)이 존재합니다.

---

### 7. 단일 SSTable 툼스톤 컴팩션

SSTable이 기록될 때 툼스톤 만료 시간(tombstone expiry times)에 대한 히스토그램이 생성됩니다. 이를 통해 툼스톤이 많은 SSTable을 식별합니다. 단일 SSTable 컴팩션은 툼스톤 삭제를 시도하며, 실제 삭제가 일어날 가능성과 SSTable의 겹침(overlap) 여부를 사전에 확인합니다. `unchecked_tombstone_compaction` 옵션은 이 사전 확인을 건너뜁니다.

---

### 8. 모든 전략에 공통된 컴팩션 옵션

**enabled** (기본값: true)
마이너 컴팩션(minor compaction)이 실행될지 여부입니다. `'enabled': true`를 `nodetool enableautocompaction`과 결합할 수 있습니다.

**tombstone_threshold** (기본값: 0.2)
단일 SSTable 컴팩션을 고려하도록 트리거하는, SSTable 내 툼스톤의 비율(proportion)입니다.

**tombstone_compaction_interval** (기본값: 86400초 / 1일)
지속적인 재컴팩션(constant recompaction)을 방지하기 위한, 단일 SSTable 컴팩션 시도 사이의 최소 시간(minimum time)입니다.

**log_all** (기본값: false)
로그 디렉터리(log directory)에 상세한 컴팩션 로깅(detailed compaction logging)을 활성화합니다.

**unchecked_tombstone_compaction** (기본값: false)
엄격한 단일 SSTable 컴팩션 점검(strict single SSTable compaction checks)을 비활성화하여, 불필요한 SSTable 재작성(rewrite)을 줄일 수 있습니다.

**only_purge_repaired_tombstones** (기본값: false)
툼스톤 제거가 데이터 복구 완료 이후에만 일어나도록 보장합니다.

**min_threshold** (기본값: 4)
컴팩션을 트리거하는 SSTable 개수의 하한(lower limit)입니다(LeveledCompactionStrategy에서는 사용되지 않음).

**max_threshold** (기본값: 32)
컴팩션을 트리거하는 SSTable 개수의 상한(upper limit)입니다(LeveledCompactionStrategy에서는 사용되지 않음).

---

### 9. 컴팩션 관련 nodetool 명령어

- `enableautocompaction` - 컴팩션을 활성화합니다.
- `disableautocompaction` - 컴팩션을 비활성화합니다.
- `setcompactionthroughput` - 최대 컴팩션 속도(maximum compaction speed)를 설정합니다(기본값: 64MiB/s).
- `compactionstats` - 현재/대기 중인(current/pending) 컴팩션 통계를 표시합니다.
- `compactionhistory` - 최근 컴팩션 세부 정보를 나열합니다.
- `setcompactionthreshold` - 최소/최대(min/max) SSTable 개수를 조정합니다(기본값: 4/32).

---

### 10. JMX를 통한 전략 전환

JMX를 통해 단일 노드(single-node)의 컴팩션 전략과 옵션을 수정할 수 있습니다:
- mbean: `org.apache.cassandra.db:type=ColumnFamilies,keyspace=<keyspace_name>,columnfamily=<table_name>`
- 속성(attribute): `CompactionParameters` 또는 `CompactionParametersJson`

JSON 구문은 `ALTER TABLE` 구문과 동일합니다:

```json
{ 'class': 'LeveledCompactionStrategy', 'sstable_size_in_mb': 123, 'fanout_size': 10}
```

이 설정은 `ALTER TABLE`로 수정하거나 노드를 재시작(restart)할 때까지 유지됩니다.

#### 상세 컴팩션 로깅

`log_all` 컴팩션 옵션을 통해 활성화하면, 로그 디렉터리에 향상된 로깅(enhanced logging)이 생성됩니다.

---

### 11. Size Tiered 컴팩션 전략(STCS)

#### STCS를 언제 사용해야 하는가

STCS는 여전히 기본(default) 컴팩션 전략이며, 특히 "쓰기 집약적 워크로드(write-intensive workloads)"에 권장됩니다. 다만, Cassandra 5.0에서는 대부분의 워크로드에 대해 통합 컴팩션 전략(Unified Compaction Strategy, UCS)이 권장되는 접근 방식이 되었음을 공식 문서가 명시하고 있습니다.

#### STCS의 동작 원리

STCS는 Cassandra가 비슷한 크기의 SSTable을 정해진 개수(기본값: 4)만큼 축적했을 때 컴팩션을 시작합니다. 이 SSTable들은 점진적으로 더 큰 SSTable로 병합되어 클러스터 전반에 걸쳐 다양한 크기의 계층(tier) 구조를 형성합니다.

#### 버킷팅 알고리즘(The Bucketing Algorithm)

STCS는 SSTable을 크기별로 그룹화하기 위해 버킷팅(bucketing) 프로세스를 사용합니다. [평균 크기 × bucket_low]에서 [평균 크기 × bucket_high] 범위 내의 크기를 가진 SSTable들을 같은 버킷으로 묶습니다. 기본값 기준으로는 버킷 평균 크기의 50%~150% 범위에 해당하는 SSTable들이 그룹화되어 컴팩션 후보가 됩니다.

#### 성능 트레이드오프

쓰기가 많은 시나리오에 효과적이지만, STCS는 읽기 성능에 불리합니다. 크기 기반 병합 방식은 데이터를 행(row) 단위로 그룹화하지 못하므로, 특정 행의 버전들이 여러 SSTable에 걸쳐 분산될 수 있습니다. 또한 컴팩션 트리거가 SSTable 크기이기 때문에 삭제된 데이터를 예측 가능하게 제거하지 못합니다.

#### 공간 증폭(Space Amplification) 문제

메이저 컴팩션 중에 중대한 한계가 드러납니다: STCS 컴팩션 중에 새 SSTable과 기존 SSTable이 동시에 점유하는 디스크 공간이 노드의 일반적인 가용 디스크 공간을 초과할 수 있습니다. 공식 문서는 STCS에서 메이저 컴팩션은 권장하지 않는다고 명시합니다.

#### STCS 설정 옵션

| 옵션 | 기본값 | 목적 |
|------|--------|------|
| `enabled` | true | "백그라운드 컴팩션을 활성화합니다(Enables background compaction)" |
| `min_threshold` | 4 | "마이너 컴팩션을 트리거하기 위한 SSTable의 최소 개수(The minimum number of SSTables to trigger a minor compaction)" |
| `max_threshold` | 32 | "마이너 컴팩션에서 허용할 SSTable의 최대 개수(The maximum number of SSTables to allow in a minor compaction)" |
| `bucket_low` | 0.5 | "SSTable의 크기가 해당 버킷 평균 크기의 50%보다 크면 그 버킷에 추가됩니다(An SSTable is added to a bucket if the SSTable size is greater than 50% of the average size of that bucket)" |
| `bucket_high` | 1.5 | "SSTable의 크기가 평균 크기의 150%보다 작으면 그 버킷에 추가됩니다(An SSTable is added to a bucket if its size is less than 150% of the average size)" |
| `min_sstable_size` | 50MB | "이 값보다 작은 SSTable들은 하나의 버킷으로 그룹화됩니다(SSTables smaller than this value will be grouped into one bucket)" |
| `tombstone_threshold` | 0.2 | "가비지 컬렉션 가능한(garbage-collectable) 툼스톤이 전체 포함된 컬럼 대비 차지하는 비율(The ratio of garbage-collectable tombstones to all contained columns)" |
| `tombstone_compaction_interval` | 86400 | "Cassandra가 툼스톤 컴팩션을 위해 SSTable을 고려하기 전에, 해당 SSTable이 생성된 후 경과해야 하는 최소 초 단위 시간(The minimum number of seconds after which an SSTable is created before Cassandra considers the SSTable for tombstone compaction)" |
| `unchecked_tombstone_compaction` | false | "어떤 테이블이 적격인지 사전 점검 없이(without pre-checking which tables are eligible)" 툼스톤 컴팩션을 허용합니다 |
| `only_purge_repaired_tombstones` | false | 활성화 시 "복구된 SSTable에서만(only from repaired SSTables)" 툼스톤 제거를 제한합니다 |
| `log_all` | false | "전체 클러스터에 대한 고급 로깅을 활성화합니다(Activates advanced logging for the entire cluster)" |

---

### 12. Leveled 컴팩션 전략(LCS)

#### LCS를 언제 사용해야 하는가

LCS는 읽기가 많은(read-heavy) 워크로드에 적합합니다. 다만 공식 문서는 "통합 컴팩션 전략(UCS)이 Cassandra 5.0부터 대부분의 워크로드에 권장되는 컴팩션 전략입니다(The Unified Compaction Strategy (UCS) is the recommended compaction strategy for most workloads starting with {cass-50})"라고 언급합니다.

#### LCS의 동작 원리

LCS는 계층화된 레벨 시스템을 통해 동작합니다. 멤테이블이 플러시되면 SSTable은 레벨 L0에 기록되며, 이 레벨에서는 SSTable들이 서로 겹칠 수 있습니다. 이후 이 SSTable들은 레벨 L1의 더 큰 SSTable들과 병합됩니다. 각 레벨의 크기는 기본적으로 이전 레벨의 10배입니다.

데이터가 L1 이상에 도달하면, 해당 SSTable은 같은 레벨의 다른 SSTable과 겹치지 않음이 보장됩니다. 이 설계 덕분에 읽기 작업 시 어떤 행에 접근하더라도 레벨당 하나의 SSTable만 확인하면 됩니다.

#### 컴팩션 프로세스

컴팩션은 겹치는 모든 SSTable을 다음 레벨의 새 SSTable로 병합하는 방식으로 동작합니다. L0 → L1 컴팩션의 경우, 대부분의 L0 SSTable이 전체 파티션 범위를 커버하기 때문에 거의 항상 모든 L1 SSTable을 포함해야 합니다.

#### L0 안전장치(L0 Safeguard)

LCS는 페일세이프 보호 장치를 포함합니다: L0에 SSTable이 32개를 초과하면 L0에서 STCS 컴팩션이 트리거됩니다.

#### 성능 특성

LCS는 STCS에 비해 디스크 사용량이 적으며, 실행에 약 10%의 디스크만 필요합니다. 다만 I/O와 CPU를 더 많이 사용합니다. 쓰기가 많은 워크로드에는 적합하지 않으며, LCS에서도 메이저 컴팩션은 권장하지 않습니다.

#### Starved SSTable 문제

LCS는 "starved sstables" 문제를 일으킬 수 있습니다. 낮은 레벨의 SSTable이 병합·컴팩션되지 않아 높은 레벨의 SSTable이 고립(stranded)되어 컴팩션되지 못하는 상황입니다. 이는 일반적으로 `sstable_size` 설정을 낮출 때 발생합니다.

#### LCS 설정 옵션

| 옵션 | 설명 | 기본값 |
|------|------|--------|
| `enabled` | 백그라운드 컴팩션을 활성화합니다 | true |
| `tombstone_compaction_interval` | 툼스톤 컴팩션을 위해 SSTable을 고려하기 전 최소 초 단위 시간 | 86400 |
| `tombstone_threshold` | 컴팩션을 트리거하는, 가비지 컬렉션 가능한 툼스톤의 비율 | 0.2 |
| `unchecked_tombstone_compaction` | 사전 점검 없이 툼스톤 컴팩션을 허용합니다 | false |
| `log_all` | 고급 클러스터 로깅을 활성화합니다 | false |
| `sstable_size_in_mb` | 목표 SSTable 크기(Target SSTable size) | 160 |
| `fanout_size` | 레벨 크기 증가 배수(Level size increase multiplier) | 10 |
| `single_sstable_uplevel` | (목적은 문서에 명시되지 않음) | true |

시작 옵션(startup option) `-Dcassandra.disable_stcs_in_l0=true`는 L0에서의 STCS를 비활성화합니다.

---

### 13. Time Window 컴팩션 전략(TWCS)

#### 개요

TWCS는 시계열(time-series) 및 TTL로 만료되는(TTL-expiring) 데이터 워크로드를 위해 설계되었습니다. TWCS는 SSTable을 시간 윈도우(time window)별로 그룹화한 뒤, 만료 시 전체 SSTable을 폐기합니다. 이를 통해 STCS나 LCS보다 더 효율적으로 디스크 공간을 회수할 수 있습니다.

#### 동작 원리

이 전략은 데이터를 시간 기반 버킷(time-based bucket)으로 구성하여 동작합니다. 활성 윈도우(active window) 동안은 플러시된 SSTable에 대해 STCS 방식의 컴팩션을 사용합니다. 시간 윈도우가 닫히면(closes), 해당 윈도우 내 모든 SSTable이 최대 타임스탬프 기준으로 하나의 SSTable로 컴팩션됩니다. 이 컴팩션이 완료된 후에는 해당 데이터를 더 이상 컴팩션하지 않습니다. 이 과정은 이후 윈도우마다 반복됩니다.

#### 주요 설정 옵션

| 옵션 | 기본값 | 목적 |
|------|--------|------|
| `enabled` | true | 백그라운드 컴팩션을 활성화합니다 |
| `compaction_window_unit` | days | 시간 단위(DAYS, HOURS 등) |
| `compaction_window_size` | 1 | 버킷당 단위 수(Units per bucket) |
| `timestamp_resolution` | microseconds | 데이터 삽입 타임스탬프의 해상도(Data insertion timestamp resolution) |
| `tombstone_threshold` | 0.2 | 가비지 컬렉션 가능한 툼스톤의 비율 |
| `tombstone_compaction_interval` | 86400 | SSTable이 적격이 되기 전까지의 초 단위 시간 |
| `expired_sstable_check_frequency_seconds` | 600 | 만료 점검 빈도(Expiration check frequency) |

#### 운영 시 고려사항

주요 위험은 순서가 어긋난 데이터 쓰기(out-of-order data writes)입니다. 오래된 데이터와 새로운 데이터가 단일 SSTable에 혼합될 수 있으며, 이는 타임스탬프가 혼재된 직접 쓰기나 읽기 복구(read repair)를 통해 발생할 수 있습니다. 공식 문서는 데이터 혼합(data commingling)을 방지하기 위해 CQL에서 명시적 타임스탬프 지정을 피하고 빈번한 repair를 실행할 것을 권장합니다.

#### 사이징 권장사항(Sizing Recommendations)

운영자는 대략 20~30개의 전체 윈도우(total windows)를 생성하는 윈도우 파라미터를 선택해야 합니다. 예를 들어, 90일 TTL 데이터의 경우 3일 윈도우(3-day window)가 적절합니다.

---

### 14. 압축(Compression)

#### 14.1 압축이란 무엇인가

Cassandra는 테이블 단위로 압축을 지원하며, SSTable을 설정 가능한 청크(chunk) 단위로 압축하여 디스크 데이터 크기를 줄입니다. SSTable은 불변(immutable)이므로 압축의 CPU 비용은 쓰기 시에만 발생합니다. 읽기 시에는 실제 데이터를 읽기 전에 전체 청크를 압축 해제(decompress)합니다.

#### 14.2 압축 알고리즘의 트레이드오프

모든 알고리즘은 세 가지 요소(three factors)의 균형을 맞춥니다:

- **압축 속도(Compression speed)**: 플러시/컴팩션 경로(flush/compaction paths)에 중요합니다.
- **압축 해제 속도(Decompression speed)**: 읽기/컴팩션 경로(read/compaction paths)에 중요합니다.
- **압축률(Ratio)**: 압축되지 않은 데이터의 감소 비율(Uncompressed data reduction percentage)입니다.

#### 14.3 사용 가능한 압축기(Compressor)

| 알고리즘 | 클래스(Class) | 압축 | 압축 해제 | 압축률 | 버전 |
|----------|---------------|------|-----------|--------|------|
| LZ4 | `LZ4Compressor` | A+ | A+ | C+ | ≥1.2.2 |
| LZ4HC | `LZ4Compressor` | C+ | A+ | B+ | ≥3.6 |
| Zstd | `ZstdCompressor` | A- | A- | A+ | ≥4.0 |
| Snappy | `SnappyCompressor` | A- | A | C | ≥1.0 |
| Deflate (zlib) | `DeflateCompressor` | C | C | A | ≥1.0 |

**권장사항(Recommendations):**
- 성능이 중요한(performance-critical) 애플리케이션: 최적의 속도 대비 압축률을 위해 `LZ4`(기본값)를 사용하십시오.
- 저장 공간이 중요한(storage-critical) 애플리케이션: 우수한 압축을 위해 `Zstd`를 사용하십시오.
- 레거시(Legacy): `Snappy`와 `Deflate`는 하위 호환성(backward compatibility)을 위해 유지됩니다.

#### 14.4 공통 압축 옵션

**class** (기본값: `LZ4Compressor`)
- 압축 알고리즘 클래스를 지정합니다.

**chunk_length_in_kb** (기본값: `16KiB`)
- 압축 청크당 데이터 크기(킬로바이트)입니다. 청크가 클수록 압축률은 향상되지만 디스크 I/O가 증가합니다.

**crc_check_chance** (기본값: `1.0`)
- 읽기 중 체크섬 검증 확률을 결정하는 부동소수점(Float, 0.0–1.0) 값입니다. 비트로트(bitrot, 비트 변질)로부터 보호합니다.

**enabled** (기본값: `true`)
- 압축 활성화를 제어하는 불리언(Boolean) 값입니다.

#### 14.5 압축기별 전용 옵션

##### LZ4Compressor 전용 옵션

**lz4_compressor_type** (기본값: `fast`)
- 옵션: `fast`(표준 LZ4) 또는 `high`(LZ4HC).

**lz4_high_compressor_level** (기본값: `9`)
- 범위: 1–17. 값이 높을수록 속도보다 압축률을 우선합니다.

##### ZstdCompressor 전용 옵션

**compression_level** (기본값: `3`)
- 범위: -131072 ~ 22. 값이 낮을수록 속도가 증가합니다. 레벨 20–22("ultra")는 추가 메모리를 필요로 합니다.

#### 14.6 압축 설정 예제(CQL)

테이블 생성 시 압축을 활성화하기:

```sql
CREATE TABLE keyspace.table (id int PRIMARY KEY)
   WITH compression = {'class': 'LZ4Compressor'};
```

기존 테이블의 압축을 수정하기:

```sql
ALTER TABLE keyspace.table
   WITH compression = {'class': 'LZ4Compressor', 'chunk_length_in_kb': 64};
```

압축을 비활성화하기:

```sql
ALTER TABLE keyspace.table
   WITH compression = {'enabled':'false'};
```

#### 14.7 압축 변경 적용 방법

압축은 SSTable이 기록될 때 적용됩니다. 기존 SSTable은 컴팩션될 때까지 기존 설정을 유지합니다. 즉시 적용하려면 `nodetool scrub` 또는 `nodetool upgradesstables -a`를 사용하여 SSTable 재작성을 트리거하십시오.

#### 14.8 압축의 이점

- 디스크 저장 공간 요구량을 줄입니다.
- I/O 양을 줄여 읽기/쓰기 처리량(throughput)을 높이는 경우가 많습니다.
- 압축의 CPU 오버헤드는 일반적으로 비압축 디스크 작업보다 빠릅니다.

#### 14.9 적합한 사용 사례와 부적합한 사용 사례

**최적의 사용 사례(Optimal Use Cases):**
- 유사한 행(similar rows)이 많은 테이블.
- 반복되는 JSON 블롭(repeated JSON blobs)이나 텍스트 컬럼.
- 압축률이 높은(high compressibility) 데이터.

**압축이 잘 되지 않는 시나리오(Poor compression scenarios):**
- 이미 압축된 데이터(Pre-compressed data).
- 무작위/벤치마크 데이터셋(Random/benchmark datasets).

#### 14.10 운영 시 고려사항

- 압축 메타데이터는 디스크 데이터 1TB당 1–3GB의 오프힙(off-heap) RAM을 필요로 합니다.
- 스트리밍 작업(streaming operations)은 압축/압축 해제 오버헤드를 유발합니다.
- 느린 압축기(Zstd, Deflate, LZ4HC 등)는 플러시 시 기본적으로 빠른 LZ4를 사용하며, 이후 일반 컴팩션 과정에서 원하는 알고리즘으로 재압축합니다.
- `crc_check_chance` 검증은 압축된 테이블에서만 지원됩니다.

##### 고급 구현(Advanced Implementation)

커스텀 압축은 `org.apache.cassandra.io.compress.ICompressor` 인터페이스를 구현하여 사용할 수 있습니다.

---

### 15. 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [Compaction 개요](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/compaction/index.html)
- [Size Tiered Compaction Strategy (STCS)](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/compaction/stcs.html)
- [Leveled Compaction Strategy (LCS)](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/compaction/lcs.html)
- [Time Window Compaction Strategy (TWCS)](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/compaction/twcs.html)
- [Compression](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/compression.html)

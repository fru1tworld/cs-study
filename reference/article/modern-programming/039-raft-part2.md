# 이해 가능한 합의 알고리즘을 찾아서 (확장판) — Part 2

> 원문: [In Search of an Understandable Consensus Algorithm (Extended Version)](https://raft.github.io/raft.pdf)
> 저자: Diego Ongaro, John Ousterhout (Stanford University) | 번역일: 2026-07-10

[Part 1](039-raft-part1.md)에서 이어진다.

## 6. 클러스터 멤버십 변경

지금까지는 클러스터의 *구성(configuration)* (합의 알고리즘에 참여하는 서버들의 집합)이 고정되어 있다고 가정했다. 실제로는 가끔 구성을 바꿀 필요가 생긴다. 예를 들어 서버가 고장 났을 때 교체하거나 복제 수준을 바꿔야 한다. 클러스터 전체를 오프라인으로 내리고 구성 파일을 갱신한 뒤 클러스터를 재시작하는 방법도 있지만, 그러면 전환 동안 클러스터를 사용할 수 없다. 게다가 수동 단계가 있으면 운영자 실수의 위험이 생긴다. 이런 문제를 피하기 위해 우리는 구성 변경을 자동화해 Raft 합의 알고리즘에 통합하기로 했다.

구성 변경 메커니즘이 안전하려면, 전환 중 어떤 시점에도 같은 임기에 두 명의 리더가 선출될 수 있어서는 안 된다. 안타깝게도 서버들이 옛 구성에서 새 구성으로 직접 전환하는 접근은 어느 것이든 안전하지 않다. 모든 서버를 한 번에 원자적으로 전환하는 것은 불가능하므로, 전환 중에 클러스터가 두 개의 독립적인 과반으로 쪼개질 수 있다(그림 10 참고).

**그림 10: 한 구성에서 다른 구성으로 직접 전환하는 것은 안전하지 않다. 서버마다 전환 시점이 다르기 때문이다.** 이 예에서 클러스터는 서버 세 대에서 다섯 대로 늘어난다. 안타깝게도 같은 임기에 서로 다른 두 리더가 선출될 수 있는 시점이 존재한다. 하나는 옛 구성(C_old)의 과반으로, 다른 하나는 새 구성(C_new)의 과반으로 선출된다.
(그림 설명: 서버 1–5의 시간축이 그려져 있다. 서버 1–3은 C_old에서 시작해 서로 다른 시점에 C_new로 전환하고, 서버 4–5는 C_new로 참여한다. 전환 도중 어느 한 시점에 C_old의 과반(예: 서버 1, 2)과 C_new의 과반(예: 서버 3, 4, 5)이 서로 겹치지 않는 두 과반을 이룰 수 있다.)

안전성을 보장하려면 구성 변경은 2단계 접근을 써야 한다. 두 단계를 구현하는 방법은 여러 가지다. 예를 들어 어떤 시스템(예: [22])은 1단계에서 옛 구성을 비활성화해 클라이언트 요청을 처리할 수 없게 만들고, 2단계에서 새 구성을 활성화한다. Raft에서는 클러스터가 먼저 *공동 합의(joint consensus)* 라고 부르는 과도기 구성으로 전환한다. 공동 합의가 커밋되고 나면 시스템은 새 구성으로 전이한다. 공동 합의는 옛 구성과 새 구성을 결합한다.

- 로그 엔트리는 양쪽 구성의 모든 서버에 복제된다.
- 어느 구성의 서버든 리더가 될 수 있다.
- 합의(선출과 엔트리 커밋)는 옛 구성과 새 구성 *각각* 의 과반을 필요로 한다.

공동 합의는 개별 서버가 안전성을 해치지 않으면서 서로 다른 시점에 구성을 전환할 수 있게 한다. 나아가 공동 합의 덕분에 클러스터는 구성 변경 내내 클라이언트 요청을 계속 처리할 수 있다.

클러스터 구성은 복제 로그의 특수 엔트리로 저장·전달된다. 그림 11이 구성 변경 과정을 보여준다. 리더가 구성을 C_old에서 C_new로 바꾸라는 요청을 받으면, 공동 합의를 위한 구성(그림의 C_old,new)을 로그 엔트리로 저장하고 앞서 설명한 메커니즘으로 복제한다. 서버는 새 구성 엔트리를 자기 로그에 추가하는 순간부터 이후의 모든 결정에 그 구성을 사용한다(서버는 엔트리의 커밋 여부와 무관하게 항상 자기 로그의 최신 구성을 사용한다). 즉 리더는 C_old,new의 로그 엔트리가 언제 커밋되는지 판정하는 데 C_old,new의 규칙을 사용한다. 리더가 크래시하면 승리한 후보자가 C_old,new를 받았는지에 따라 C_old 또는 C_old,new 하에서 새 리더가 선출될 수 있다. 어떤 경우든 이 기간 동안 C_new는 일방적인 결정을 내릴 수 없다.

C_old,new가 커밋되고 나면 C_old도 C_new도 상대의 승인 없이는 결정을 내릴 수 없고, 리더 완전성 속성은 C_old,new 로그 엔트리를 가진 서버만 리더로 선출될 수 있음을 보장한다. 이제 리더가 C_new를 기술하는 로그 엔트리를 만들어 클러스터에 복제하는 것이 안전하다. 역시 이 구성은 각 서버가 보는 즉시 효력을 갖는다. 새 구성이 C_new의 규칙 하에서 커밋되고 나면 옛 구성은 무의미해지고, 새 구성에 없는 서버들은 종료할 수 있다. 그림 11처럼 C_old와 C_new가 동시에 일방적인 결정을 내릴 수 있는 시점은 존재하지 않는다. 이것이 안전성을 보장한다.

**그림 11: 구성 변경의 타임라인.** 점선은 생성되었지만 아직 커밋되지 않은 구성 엔트리를, 실선은 가장 최근에 커밋된 구성 엔트리를 나타낸다. 리더는 먼저 자기 로그에 C_old,new 구성 엔트리를 만들고 C_old,new(C_old의 과반과 C_new의 과반)에 커밋한다. 그런 다음 C_new 엔트리를 만들고 C_new의 과반에 커밋한다. C_old와 C_new가 동시에 독립적으로 결정을 내릴 수 있는 시점은 없다.
(그림 설명: 시간축을 따라 처음에는 C_old가 단독으로 결정할 수 있고, C_old,new 엔트리가 커밋된 뒤부터 C_new 엔트리가 커밋될 때까지는 공동 합의가 적용되며, 그 이후에는 C_new가 단독으로 결정할 수 있다. C_new에 속하지 않는 리더는 C_new를 커밋한 시점에 물러난다.)

재구성에는 다뤄야 할 문제가 세 가지 더 있다. 첫 번째 문제는 새 서버는 처음에 로그 엔트리를 하나도 갖고 있지 않을 수 있다는 것이다. 이 상태로 클러스터에 추가되면 따라잡는 데 꽤 오래 걸릴 수 있고, 그동안 새 로그 엔트리를 커밋할 수 없을 수 있다. 가용성 공백을 피하기 위해 Raft는 구성 변경 전에 추가 단계를 도입한다. 이 단계에서 새 서버들은 투표권 없는 구성원으로 클러스터에 합류한다(리더가 로그 엔트리를 복제해 주지만 과반 계산에는 포함하지 않는다). 새 서버들이 클러스터의 나머지를 따라잡으면 위에서 설명한 대로 재구성을 진행할 수 있다.

두 번째 문제는 클러스터 리더가 새 구성에 속하지 않을 수 있다는 것이다. 이 경우 리더는 C_new 로그 엔트리를 커밋하고 나면 물러난다(팔로워 상태로 돌아간다). 이는 리더가 자신을 포함하지 않는 클러스터를 관리하는 기간(C_new를 커밋하는 동안)이 존재한다는 뜻이다. 리더는 로그 엔트리를 복제하지만 자신을 과반에 포함시키지 않는다. 리더 전환은 C_new가 커밋되는 시점에 일어나는데, 이 시점이 새 구성이 독립적으로 동작할 수 있는(C_new에서 리더를 뽑는 일이 항상 가능해지는) 첫 지점이기 때문이다. 이 시점 전에는 C_old의 서버만 리더로 선출될 수 있는 상황일 수 있다.

세 번째 문제는 제거된 서버(C_new에 없는 서버)가 클러스터를 교란할 수 있다는 것이다. 이 서버들은 하트비트를 받지 못하므로 타임아웃해 새 선출을 시작한다. 그러면 새 임기 번호로 RequestVote RPC를 보내고, 이는 현재 리더를 팔로워 상태로 되돌린다. 결국 새 리더가 선출되겠지만, 제거된 서버들은 다시 타임아웃하고 이 과정이 반복되어 가용성이 나빠진다.

이 문제를 막기 위해 서버는 현재 리더가 존재한다고 믿는 동안에는 RequestVote RPC를 무시한다. 구체적으로, 서버가 현재 리더로부터 소식을 들은 지 최소 선출 타임아웃 이내에 RequestVote RPC를 받으면, 임기를 갱신하지도 표를 주지도 않는다. 이는 정상적인 선출에 영향을 주지 않는다. 각 서버는 선출을 시작하기 전에 최소 선출 타임아웃만큼 기다리기 때문이다. 반면 제거된 서버의 교란은 막는 데 도움이 된다. 리더가 자기 클러스터에 하트비트를 전달할 수 있는 한, 더 큰 임기 번호에 의해 쫓겨나지 않는다.

## 7. 로그 압축

Raft의 로그는 정상 동작 중에 클라이언트 요청을 더 많이 담으며 자라지만, 실용적인 시스템에서 로그가 무한정 자랄 수는 없다. 로그가 길어질수록 공간을 더 차지하고 재생(replay)하는 데 시간이 더 걸린다. 로그에 쌓인 낡은 정보를 버리는 메커니즘이 없으면 결국 가용성 문제가 생긴다.

스냅샷은 압축의 가장 단순한 접근이다. 스냅샷에서는 현재 시스템 상태 전체를 안정 저장소의 *스냅샷* 에 기록하고, 그 지점까지의 로그 전체를 버린다. 스냅샷은 Chubby와 ZooKeeper에서 쓰이며, 이 절의 나머지는 Raft의 스냅샷을 설명한다.

로그 클리닝[36]이나 로그 구조 병합 트리(LSM 트리)[30, 5] 같은 점진적 압축 접근도 가능하다. 이들은 한 번에 데이터의 일부만 다루므로 압축 부하를 시간에 걸쳐 더 고르게 분산한다. 먼저 삭제되고 덮어쓰인 객체가 많이 쌓인 데이터 영역을 고른 다음, 그 영역의 살아 있는 객체들을 더 압축해 다시 쓰고 영역을 해제한다. 이는 항상 전체 데이터 집합을 다루는 스냅샷에 비해 상당한 추가 메커니즘과 복잡성을 요구한다. 로그 클리닝은 Raft의 수정이 필요하겠지만, 상태 머신은 스냅샷과 같은 인터페이스로 LSM 트리를 구현할 수 있다.

**그림 12: 서버가 자기 로그의 커밋된 엔트리(인덱스 1부터 5)를 새 스냅샷으로 교체한다.** 스냅샷은 현재 상태만 저장한다(이 예에서는 변수 x와 y). 스냅샷의 마지막 포함 인덱스(last included index)와 임기는 엔트리 6 앞에서 스냅샷을 로그에 위치시키는 역할을 한다.
(그림 설명: 압축 전 로그는 인덱스 1–7에 임기 1(x←3, y←1, y←9), 임기 2(x←2), 임기 3(x←0, y←7, x←5)의 엔트리를 담고 있다. 압축 후에는 "last included index: 5, last included term: 3, 상태 머신 상태: x←0, y←9"를 담은 스냅샷과 인덱스 6–7의 엔트리(y←7, x←5)만 남는다.)

그림 12는 Raft 스냅샷의 기본 아이디어를 보여준다. 각 서버는 독립적으로 스냅샷을 찍으며, 자기 로그의 커밋된 엔트리들만 포함한다. 작업의 대부분은 상태 머신이 현재 상태를 스냅샷에 기록하는 일이다. Raft는 스냅샷에 약간의 메타데이터도 포함한다. *마지막 포함 인덱스(last included index)* 는 스냅샷이 대체하는 로그의 마지막 엔트리(상태 머신이 적용한 마지막 엔트리)의 인덱스이고, *마지막 포함 임기(last included term)* 는 그 엔트리의 임기다. 이들은 스냅샷 다음의 첫 로그 엔트리에 대한 AppendEntries 일관성 검사를 지원하기 위해 보존된다. 그 엔트리에는 직전 로그 인덱스와 임기가 필요하기 때문이다. 클러스터 멤버십 변경(6절)을 지원하기 위해, 스냅샷은 마지막 포함 인덱스 시점의 로그 내 최신 구성도 포함한다. 서버가 스냅샷 기록을 완료하면 마지막 포함 인덱스까지의 모든 로그 엔트리와 이전의 모든 스냅샷을 삭제할 수 있다.

서버들은 보통 독립적으로 스냅샷을 찍지만, 리더가 가끔 뒤처진 팔로워에게 스냅샷을 보내야 할 때가 있다. 리더가 팔로워에게 보내야 할 다음 로그 엔트리를 이미 버렸을 때 그렇다. 다행히 이 상황은 정상 동작에서는 일어날 가능성이 낮다. 리더를 따라잡고 있는 팔로워라면 이미 그 엔트리를 갖고 있을 것이다. 하지만 예외적으로 느린 팔로워나 클러스터에 새로 합류하는 서버(6절)는 그렇지 않다. 이런 팔로워를 최신 상태로 만드는 방법은 리더가 네트워크로 스냅샷을 보내는 것이다.

### 그림 13: InstallSnapshot RPC 요약

리더가 스냅샷 조각(chunk)을 팔로워에게 보내기 위해 호출한다. 리더는 항상 조각을 순서대로 보낸다. 스냅샷은 전송을 위해 조각으로 나뉘는데, 각 조각이 팔로워에게 리더가 살아 있다는 신호를 주므로 팔로워는 선출 타이머를 리셋할 수 있다.

**인자:**

| 항목 | 설명 |
|---|---|
| term | 리더의 임기 |
| leaderId | 팔로워가 클라이언트를 리다이렉트할 수 있도록 |
| lastIncludedIndex | 스냅샷이 이 인덱스까지의 모든 엔트리를 대체 |
| lastIncludedTerm | lastIncludedIndex의 임기 |
| offset | 조각이 스냅샷 파일에서 위치하는 바이트 오프셋 |
| data[] | offset부터 시작하는 스냅샷 조각의 원시 바이트 |
| done | 마지막 조각이면 true |

**결과:**

| 항목 | 설명 |
|---|---|
| term | currentTerm, 리더가 자신을 갱신할 수 있도록 |

**수신자 구현:**

1. term < currentTerm이면 즉시 응답
2. 첫 조각이면(offset이 0) 새 스냅샷 파일 생성
3. 주어진 offset 위치에 데이터를 스냅샷 파일에 기록
4. done이 false면 응답하고 다음 데이터 조각을 기다림
5. 스냅샷 파일을 저장하고, 인덱스가 더 작은 기존 또는 부분 스냅샷을 모두 폐기
6. 기존 로그 엔트리가 스냅샷의 마지막 포함 엔트리와 같은 인덱스와 임기를 갖는다면, 그 뒤의 로그 엔트리들은 유지하고 응답
7. 로그 전체를 폐기
8. 스냅샷 내용으로 상태 머신을 리셋 (스냅샷의 클러스터 구성도 로드)

리더는 너무 뒤처진 팔로워에게 스냅샷을 보내기 위해 InstallSnapshot이라는 새 RPC를 사용한다(그림 13 참고). 팔로워가 이 RPC로 스냅샷을 받으면 기존 로그 엔트리를 어떻게 할지 결정해야 한다. 보통 스냅샷은 수신자의 로그에 아직 없는 새 정보를 담고 있다. 이 경우 팔로워는 로그 전체를 버린다. 로그 전체가 스냅샷으로 대체되며, 스냅샷과 충돌하는 커밋되지 않은 엔트리가 있을 수도 있기 때문이다. 반대로 팔로워가 (재전송이나 실수로) 자기 로그의 접두사(prefix)를 기술하는 스냅샷을 받으면, 스냅샷이 커버하는 로그 엔트리는 삭제하되 그 뒤의 엔트리들은 여전히 유효하므로 유지해야 한다.

이 스냅샷 접근은 Raft의 강한 리더 원칙에서 벗어난다. 팔로워가 리더 모르게 스냅샷을 찍을 수 있기 때문이다. 하지만 우리는 이 이탈이 정당하다고 본다. 리더는 합의에 도달할 때 충돌하는 결정을 피하는 데 도움이 되지만, 스냅샷 시점에는 이미 합의에 도달한 상태이므로 어떤 결정도 충돌하지 않는다. 데이터는 여전히 리더에서 팔로워로만 흐른다. 팔로워는 이제 자기 데이터를 재조직할 수 있을 뿐이다.

우리는 리더만 스냅샷을 만들고, 리더가 그 스냅샷을 각 팔로워에게 보내는 대안적 리더 기반 접근도 검토했다. 하지만 여기에는 두 가지 단점이 있다. 첫째, 각 팔로워에게 스냅샷을 보내는 것은 네트워크 대역폭을 낭비하고 스냅샷 과정을 느리게 한다. 각 팔로워는 이미 자기 스냅샷을 만드는 데 필요한 정보를 갖고 있으며, 보통 서버가 자기 로컬 상태에서 스냅샷을 만드는 편이 네트워크로 주고받는 것보다 훨씬 싸다. 둘째, 리더의 구현이 더 복잡해진다. 예를 들어 리더는 새 로그 엔트리 복제와 병렬로 스냅샷을 팔로워들에게 보내야 새 클라이언트 요청이 막히지 않는다.

스냅샷 성능에 영향을 주는 문제가 두 가지 더 있다. 첫째, 서버는 언제 스냅샷을 찍을지 결정해야 한다. 너무 자주 찍으면 디스크 대역폭과 에너지를 낭비하고, 너무 드물게 찍으면 저장 용량을 소진할 위험이 있고 재시작 시 로그 재생 시간이 늘어난다. 간단한 전략 하나는 로그가 고정된 바이트 크기에 도달하면 스냅샷을 찍는 것이다. 이 크기를 스냅샷의 예상 크기보다 훨씬 크게 잡으면 스냅샷으로 인한 디스크 대역폭 오버헤드는 작아진다.

두 번째 성능 문제는 스냅샷 기록에 상당한 시간이 걸릴 수 있는데, 이것이 정상 동작을 지연시키면 안 된다는 것이다. 해법은 copy-on-write 기법을 사용해서, 기록 중인 스냅샷에 영향을 주지 않으면서 새 갱신을 받아들이는 것이다. 예를 들어 함수형 자료구조로 만든 상태 머신은 이를 자연스럽게 지원한다. 또는 운영체제의 copy-on-write 지원(예: Linux의 fork)으로 상태 머신 전체의 인메모리 스냅샷을 만들 수 있다(우리 구현은 이 접근을 쓴다).

## 8. 클라이언트 상호작용

이 절은 클라이언트가 Raft와 상호작용하는 방법을 설명한다. 클라이언트가 클러스터 리더를 찾는 방법과 Raft가 선형화 가능(linearizable) 시맨틱스[10]를 지원하는 방법을 포함한다. 이 문제들은 모든 합의 기반 시스템에 적용되며, Raft의 해법은 다른 시스템들과 비슷하다.

Raft의 클라이언트는 모든 요청을 리더에게 보낸다. 클라이언트가 처음 시작하면 무작위로 고른 서버에 접속한다. 처음 고른 서버가 리더가 아니면, 그 서버는 클라이언트의 요청을 거부하고 자신이 가장 최근에 들은 리더에 대한 정보를 제공한다(AppendEntries 요청에는 리더의 네트워크 주소가 포함된다). 리더가 크래시하면 클라이언트 요청은 타임아웃하고, 클라이언트는 무작위로 고른 서버로 다시 시도한다.

Raft의 목표는 선형화 가능 시맨틱스(각 연산이 호출과 응답 사이의 어느 시점에 정확히 한 번, 즉시 실행되는 것처럼 보이는 것)를 구현하는 것이다. 하지만 지금까지 설명한 대로라면 Raft는 명령을 여러 번 실행할 수 있다. 예를 들어 리더가 로그 엔트리를 커밋한 뒤 클라이언트에 응답하기 전에 크래시하면, 클라이언트는 새 리더에게 명령을 재시도하고 명령이 두 번째로 실행된다. 해법은 클라이언트가 모든 명령에 고유한 일련번호를 부여하는 것이다. 그러면 상태 머신은 클라이언트별로 처리한 최신 일련번호를 관련 응답과 함께 추적한다. 이미 실행된 일련번호의 명령을 받으면 재실행 없이 즉시 응답한다.

읽기 전용 연산은 로그에 아무것도 쓰지 않고 처리할 수 있다. 하지만 추가 조치가 없으면 낡은(stale) 데이터를 반환할 위험이 있다. 요청에 응답하는 리더가 자신도 모르는 사이에 더 새로운 리더에게 밀려났을 수 있기 때문이다. 선형화 가능 읽기는 낡은 데이터를 반환해서는 안 되며, Raft는 로그를 쓰지 않고 이를 보장하기 위해 두 가지 추가 예방책이 필요하다. 첫째, 리더는 어떤 엔트리가 커밋되었는지에 대한 최신 정보를 가져야 한다. 리더 완전성 속성은 리더가 커밋된 모든 엔트리를 갖고 있음을 보장하지만, 임기 시작 시점에는 그것이 어느 것들인지 모를 수 있다. 알아내려면 자기 임기의 엔트리를 하나 커밋해야 한다. Raft는 각 리더가 임기 시작 시 빈 *no-op* 엔트리를 로그에 커밋하게 하여 이를 처리한다. 둘째, 리더는 읽기 전용 요청을 처리하기 전에 자신이 물러났는지 확인해야 한다(더 최신의 리더가 선출되었다면 자신의 정보가 낡았을 수 있다). Raft는 리더가 읽기 전용 요청에 응답하기 전에 클러스터 과반과 하트비트 메시지를 교환하게 하여 이를 처리한다. 또 다른 방법으로 리더가 하트비트 메커니즘에 기대어 일종의 임차(lease)[9]를 제공할 수도 있지만, 이는 안전성을 위해 타이밍에 의존하게 된다(시계 오차가 유계라고 가정한다).

## 9. 구현과 평가

우리는 RAMCloud[33]의 구성 정보를 저장하고 RAMCloud 코디네이터의 장애 극복(failover)을 돕는 복제 상태 머신의 일부로 Raft를 구현했다. Raft 구현은 테스트, 주석, 빈 줄을 제외하고 대략 2000줄의 C++ 코드로 이루어져 있다. 소스 코드는 자유롭게 이용할 수 있다[23]. 또한 이 논문의 초안에 기반해 다양한 개발 단계에 있는 약 25개의 독립적인 서드파티 오픈소스 Raft 구현[34]이 있다. 여러 회사가 Raft 기반 시스템을 배포하고 있기도 하다[34].

이 절의 나머지는 이해 가능성, 정확성, 성능이라는 세 가지 기준으로 Raft를 평가한다.

### 9.1 이해 가능성

Paxos 대비 Raft의 이해 가능성을 측정하기 위해, Stanford 대학의 고급 운영체제 강의와 U.C. Berkeley의 분산 컴퓨팅 강의에서 상급 학부생과 대학원생을 대상으로 실험적 연구를 수행했다. Raft 강의와 Paxos 강의를 각각 녹화하고 그에 대응하는 퀴즈를 만들었다. Raft 강의는 로그 압축을 제외한 이 논문의 내용을 다뤘고, Paxos 강의는 동등한 복제 상태 머신을 만들기에 충분한 자료를 다뤘다. 단일 결정 Paxos, 다중 결정 Paxos, 재구성, 그리고 실무에서 필요한 몇 가지 최적화(리더 선출 등)를 포함했다. 퀴즈는 알고리즘의 기본 이해를 시험했고 코너 케이스에 대한 추론도 요구했다. 각 학생은 첫 번째 영상을 보고 해당 퀴즈를 풀고, 두 번째 영상을 보고 두 번째 퀴즈를 풀었다. 연구의 첫 부분에서 얻는 개인별 실력 차이와 경험 차이를 통제하기 위해, 참가자의 절반은 Paxos를 먼저 하고 나머지 절반은 Raft를 먼저 했다. 참가자들이 Raft를 더 잘 이해했는지 판단하기 위해 각 퀴즈의 점수를 비교했다.

Paxos와 Raft의 비교를 최대한 공정하게 만들려고 노력했다. 실험은 두 가지 면에서 Paxos에 유리했다. 43명 중 15명이 Paxos 사전 경험이 있다고 답했고, Paxos 영상이 Raft 영상보다 14% 길었다. 표 1에 요약했듯이 편향 가능성이 있는 요소들을 완화하는 조치를 취했다. 우리의 모든 자료는 검토를 위해 공개되어 있다[28, 31].

**표 1: Paxos에 불리할 수 있다는 우려, 이를 상쇄하기 위한 조치, 그리고 검토 가능한 추가 자료.**

| 우려 | 편향 완화를 위한 조치 | 검토용 자료 [28, 31] |
|---|---|---|
| 동등한 강의 품질 | 두 강의 모두 같은 강사. Paxos 강의는 여러 대학에서 사용 중인 기존 자료에 기반해 개선. Paxos 강의가 14% 더 김. | 영상 |
| 동등한 퀴즈 난이도 | 문제를 난이도별로 묶어 두 시험 간에 짝을 지음. | 퀴즈 |
| 공정한 채점 | 루브릭 사용. 퀴즈를 번갈아 가며 무작위 순서로 채점. | 루브릭 |

참가자들은 평균적으로 Paxos 퀴즈보다 Raft 퀴즈에서 4.9점 높은 점수를 받았다(가능한 60점 만점에 Raft 평균은 25.7점, Paxos 평균은 20.8점). 그림 14가 개인별 점수를 보여준다. 짝지은 t-검정에 따르면 95% 신뢰수준에서 Raft 점수의 실제 분포는 Paxos 점수의 실제 분포보다 평균이 최소 2.5점 크다.

**그림 14: 참가자 43명의 Raft 퀴즈와 Paxos 퀴즈 성적을 비교한 산점도.** 대각선 위의 점(33개)은 Raft에서 더 높은 점수를 받은 참가자를 나타낸다.
(그림 설명: 가로축은 Paxos 점수, 세로축은 Raft 점수(0–60). "Raft를 먼저 배운 그룹"과 "Paxos를 먼저 배운 그룹"이 다른 기호로 표시되어 있고, 대부분의 점이 대각선 위쪽에 분포한다.)

또한 세 요인(어느 퀴즈를 풀었는지, Paxos 사전 경험의 정도, 알고리즘을 배운 순서)에 기반해 새 학생의 퀴즈 점수를 예측하는 선형 회귀 모형도 만들었다. 이 모형은 퀴즈 선택이 Raft에 유리한 12.5점의 차이를 만든다고 예측한다. 이는 관측된 4.9점 차이보다 훨씬 높은데, 실제 학생들 중 다수가 Paxos 사전 경험이 있었고 그것이 Paxos에는 큰 도움이 된 반면 Raft에는 도움이 조금 덜 되었기 때문이다. 흥미롭게도 이 모형은 Paxos 퀴즈를 이미 푼 사람의 Raft 점수를 6.3점 낮게 예측하기도 한다. 이유는 알 수 없지만 통계적으로 유의해 보인다.

퀴즈 후에 참가자들에게 어느 알고리즘이 구현하거나 설명하기 더 쉬울 것 같은지 설문했다. 결과는 그림 15에 있다. 압도적 다수의 참가자가 Raft가 구현하고 설명하기 더 쉬울 것이라고 답했다(각 문항에서 41명 중 33명). 다만 이런 자기 보고식 인식은 퀴즈 점수보다 신뢰성이 떨어질 수 있고, 참가자들이 Raft가 더 이해하기 쉽다는 우리의 가설을 알고 있어서 편향되었을 수도 있다.

**그림 15: 5점 척도로, 참가자들에게 (왼쪽) 제대로 동작하는 정확하고 효율적인 시스템에서 어느 알고리즘이 구현하기 더 쉬울지, (오른쪽) CS 대학원생에게 설명하기에 어느 쪽이 더 쉬울지 물었다.**
(그림 설명: 막대그래프. "구현"과 "설명" 두 문항 모두에서 "Raft가 훨씬 쉽다"와 "Raft가 다소 쉽다"가 압도적으로 많고, "Paxos가 쉽다"는 응답은 소수다.)

Raft 사용자 연구에 대한 상세한 논의는 [31]에서 볼 수 있다.

### 9.2 정확성

우리는 5절에서 설명한 합의 메커니즘에 대해 형식 명세와 안전성 증명을 개발했다. 형식 명세[31]는 TLA+ 명세 언어[17]를 사용해 그림 2에 요약된 정보를 완전히 정밀하게 만든다. 길이는 약 400줄이며 증명의 대상 역할을 한다. Raft를 구현하려는 사람에게 그 자체로도 유용하다. 우리는 TLA 증명 시스템[7]으로 로그 완전성 속성을 기계적으로 증명했다. 다만 이 증명은 기계적으로 검사되지 않은 불변식들에 의존한다(예: 명세의 타입 안전성은 증명하지 않았다). 또한 상태 머신 안전성 속성에 대한 비형식 증명[31]을 작성했는데, 이는 완전하며(명세에만 의존한다) 비교적 정밀하다(약 3500단어 분량이다).

### 9.3 성능

Raft의 성능은 Paxos 같은 다른 합의 알고리즘과 비슷하다. 성능에서 가장 중요한 경우는 자리를 잡은 리더가 새 로그 엔트리를 복제할 때다. Raft는 최소한의 메시지 수(리더에서 클러스터 절반으로의 단일 왕복)로 이를 달성한다. Raft의 성능을 더 개선하는 것도 가능하다. 예를 들어 더 높은 처리량과 더 낮은 지연을 위한 배칭과 파이프라이닝을 쉽게 지원한다. 다른 알고리즘들을 위해 문헌에서 다양한 최적화가 제안되었고, 그중 다수를 Raft에 적용할 수 있겠지만 이는 향후 과제로 남긴다.

우리는 우리의 Raft 구현으로 Raft 리더 선출 알고리즘의 성능을 측정하고 두 가지 질문에 답했다. 첫째, 선출 과정은 빠르게 수렴하는가? 둘째, 리더 크래시 후 달성 가능한 최소 다운타임은 얼마인가?

**그림 16: 크래시한 리더를 탐지하고 교체하는 데 걸리는 시간.** 위 그래프는 선출 타임아웃의 무작위성 정도를 변화시킨 것이고, 아래 그래프는 최소 선출 타임아웃을 조정한 것이다. 각 선은 1000회 시도("150–150ms"만 100회)를 나타내며 특정 선출 타임아웃 선택에 대응한다. 예를 들어 "150–155ms"는 선출 타임아웃을 150ms와 155ms 사이에서 무작위 균등하게 골랐다는 뜻이다. 측정은 브로드캐스트 시간이 약 15ms인 다섯 서버 클러스터에서 이뤄졌다. 아홉 서버 클러스터의 결과도 비슷하다.
(그래프 설명: 두 개의 누적 분포 그래프. 위 그래프는 150–150ms, 150–151ms, 150–155ms, 150–175ms, 150–200ms, 150–300ms 조건에서 리더 없는 시간의 누적 백분율을 로그 스케일(약 100ms–100,000ms)로 보여준다. 무작위성이 전혀 없는 150–150ms는 표 분산 때문에 극단적으로 오래 걸린다. 아래 그래프는 12–24ms, 25–50ms, 50–100ms, 100–200ms, 150–300ms 조건으로 0–600ms 범위를 보여주며, 타임아웃이 작을수록 다운타임이 짧아진다.)

리더 선출을 측정하기 위해 다섯 서버 클러스터의 리더를 반복적으로 크래시시키고, 크래시 탐지와 새 리더 선출에 걸리는 시간을 쟀다(그림 16 참고). 최악의 시나리오를 만들기 위해 각 시도에서 서버들은 서로 다른 로그 길이를 가져 일부 후보자는 리더가 될 자격이 없었다. 또 표 분산을 유도하기 위해, 테스트 스크립트는 리더 프로세스를 종료하기 전에 리더로부터 하트비트 RPC의 동기화된 브로드캐스트를 발동했다(이는 리더가 크래시 전에 새 로그 엔트리를 복제하는 동작을 근사한다). 리더는 하트비트 간격(모든 테스트에서 최소 선출 타임아웃의 절반) 안에서 균등 무작위하게 크래시되었다. 따라서 가능한 최소 다운타임은 최소 선출 타임아웃의 약 절반이었다.

그림 16의 위 그래프는 선출 타임아웃에 약간의 무작위성만 있어도 선출에서 표 분산을 피하기에 충분함을 보여준다. 무작위성이 없으면 우리 테스트에서 리더 선출은 수많은 표 분산 때문에 일관되게 10초를 넘겼다. 단 5ms의 무작위성을 추가하는 것만으로 크게 개선되어 다운타임 중앙값이 287ms가 되었다. 무작위성을 더 쓰면 최악의 경우가 개선된다. 무작위성 50ms에서는 (1000회 시도 중) 최악의 완료 시간이 513ms였다.

아래 그래프는 선출 타임아웃을 줄여 다운타임을 줄일 수 있음을 보여준다. 선출 타임아웃이 12–24ms일 때 리더 선출은 평균 35ms밖에 걸리지 않는다(가장 긴 시도가 152ms였다). 하지만 타임아웃을 이 이하로 낮추면 Raft의 타이밍 요구 조건을 위반한다. 리더가 다른 서버들이 새 선출을 시작하기 전에 하트비트를 브로드캐스트하기 어려워진다. 이는 불필요한 리더 교체를 유발하고 시스템 전체 가용성을 낮출 수 있다. 우리는 150–300ms 같은 보수적인 선출 타임아웃을 권장한다. 이런 타임아웃은 불필요한 리더 교체를 일으킬 가능성이 낮고 여전히 좋은 가용성을 제공한다.

## 10. 관련 연구

합의 알고리즘 관련 출판물은 매우 많으며, 다수는 다음 범주 중 하나에 속한다.

- Lamport의 Paxos 원 서술[15]과 이를 더 명확히 설명하려는 시도들[16, 20, 21].
- 빠진 세부를 채우고 구현에 더 나은 기반을 제공하도록 알고리즘을 수정한 Paxos의 정교화들[26, 39, 13].
- Chubby[2, 4], ZooKeeper[11, 12], Spanner[6] 같은 합의 알고리즘을 구현한 시스템들. Chubby와 Spanner의 알고리즘은 상세히 공개되지 않았지만 둘 다 Paxos 기반이라고 주장한다. ZooKeeper의 알고리즘은 더 상세히 공개되었지만 Paxos와 상당히 다르다.
- Paxos에 적용할 수 있는 성능 최적화들[18, 19, 3, 25, 1, 27].
- Oki와 Liskov의 Viewstamped Replication(VR). Paxos와 비슷한 시기에 개발된 합의에 대한 대안적 접근이다. 원 서술[29]은 분산 트랜잭션 프로토콜과 얽혀 있었지만, 최근 갱신판[22]에서 핵심 합의 프로토콜이 분리되었다. VR은 Raft와 유사점이 많은 리더 기반 접근을 쓴다.

Raft와 Paxos의 가장 큰 차이는 Raft의 강한 리더십이다. Raft는 리더 선출을 합의 프로토콜의 필수 부분으로 사용하며, 가능한 한 많은 기능을 리더에 집중시킨다. 이 접근은 이해하기 더 쉬운 더 단순한 알고리즘을 낳는다. 예를 들어 Paxos에서 리더 선출은 기본 합의 프로토콜과 직교한다. 성능 최적화 역할만 할 뿐 합의 달성에 필수가 아니다. 하지만 그 결과 추가 메커니즘이 생긴다. Paxos는 기본 합의를 위한 2단계 프로토콜과 리더 선출을 위한 별도 메커니즘을 모두 포함한다. 반면 Raft는 리더 선출을 합의 알고리즘에 직접 통합해 합의의 두 단계 중 첫 단계로 사용한다. 이로써 Paxos보다 메커니즘이 적어진다.

Raft처럼 VR과 ZooKeeper도 리더 기반이므로 Paxos 대비 Raft의 장점 다수를 공유한다. 하지만 Raft는 VR이나 ZooKeeper보다 메커니즘이 적다. 리더가 아닌 노드의 기능을 최소화하기 때문이다. 예를 들어 Raft에서 로그 엔트리는 리더로부터 AppendEntries RPC를 통해 바깥으로, 오직 한 방향으로만 흐른다. VR에서는 로그 엔트리가 양방향으로 흐른다(리더가 선출 과정에서 로그 엔트리를 받을 수 있다). 이는 추가 메커니즘과 복잡성을 낳는다. 공개된 ZooKeeper의 서술도 로그 엔트리를 리더에게로도, 리더로부터도 전송하지만 구현은 Raft에 더 가까운 것으로 보인다[35].

Raft는 우리가 아는 어떤 합의 기반 로그 복제 알고리즘보다 메시지 종류가 적다. 예를 들어 우리는 VR과 ZooKeeper가 기본 합의와 멤버십 변경에 사용하는 메시지 종류를 세어 보았다(로그 압축과 클라이언트 상호작용은 알고리즘과 거의 독립적이므로 제외). VR과 ZooKeeper는 각각 10가지의 서로 다른 메시지 종류를 정의하는 반면 Raft는 4가지(두 개의 RPC 요청과 그 응답)뿐이다. Raft의 메시지는 다른 알고리즘들의 메시지보다 다소 밀도가 높지만, 종합하면 더 단순하다. 게다가 VR과 ZooKeeper는 리더 교체 시 로그 전체를 전송하는 방식으로 기술되어 있다. 이 메커니즘이 실용적이 되려면 추가 메시지 종류가 필요할 것이다.

Raft의 강한 리더십 접근은 알고리즘을 단순화하지만 일부 성능 최적화를 배제한다. 예를 들어 Egalitarian Paxos(EPaxos)는 리더 없는 접근으로 일부 조건에서 더 높은 성능을 낼 수 있다[27]. EPaxos는 상태 머신 명령들의 가환성(commutativity)을 활용한다. 어떤 명령과 동시에 제안된 다른 명령들이 그 명령과 가환하는 한, 어느 서버든 단 한 라운드의 통신으로 명령을 커밋할 수 있다. 하지만 동시에 제안된 명령들이 서로 가환하지 않으면 EPaxos는 추가 통신 라운드가 필요하다. 어느 서버든 명령을 커밋할 수 있으므로 EPaxos는 서버 간 부하를 잘 분산하고 WAN 환경에서 Raft보다 낮은 지연을 달성할 수 있다. 하지만 Paxos에 상당한 복잡성을 더한다.

클러스터 멤버십 변경에 대해서는 Lamport의 원 제안[15], VR[22], SMART[24] 등 여러 접근이 제안되거나 다른 연구에서 구현되었다. 우리가 Raft에 공동 합의 접근을 택한 것은, 합의 프로토콜의 나머지를 그대로 활용하므로 멤버십 변경에 필요한 추가 메커니즘이 아주 적기 때문이다. Lamport의 α 기반 접근은 리더 없이 합의에 도달할 수 있다고 가정하므로 Raft의 선택지가 아니었다. VR과 SMART에 비해 Raft의 재구성 알고리즘은 정상 요청의 처리를 제한하지 않으면서 멤버십을 변경할 수 있다는 장점이 있다. 반면 VR은 구성 변경 중 모든 정상 처리를 중단하고, SMART는 미결 요청 수에 α와 유사한 제한을 둔다. Raft의 접근은 VR이나 SMART보다 추가하는 메커니즘도 적다.

## 11. 결론

알고리즘은 흔히 정확성, 효율, 간결성을 주된 목표로 설계된다. 이들은 모두 가치 있는 목표지만, 우리는 이해 가능성도 그만큼 중요하다고 믿는다. 개발자가 알고리즘을 실용적인 구현으로 옮기기 전까지는 다른 어떤 목표도 달성될 수 없는데, 그 구현은 필연적으로 발표된 형태에서 벗어나고 확장될 것이다. 개발자가 알고리즘을 깊이 이해하고 그에 대한 직관을 만들어 낼 수 없다면, 구현에서 알고리즘의 바람직한 속성들을 유지하기 어려울 것이다.

이 논문에서 우리는 분산 합의 문제를 다뤘다. 널리 받아들여졌지만 난해한 알고리즘인 Paxos가 오랫동안 학생과 개발자를 곤경에 빠뜨려 온 영역이다. 우리는 새 알고리즘 Raft를 개발했고 그것이 Paxos보다 이해하기 쉽다는 것을 보였다. 또한 Raft가 시스템 구축에 더 나은 기반을 제공한다고 믿는다. 이해 가능성을 일차 설계 목표로 삼자 Raft 설계에 접근하는 방식이 달라졌다. 설계가 진행되면서 우리는 문제 분해와 상태 공간 단순화 같은 몇 가지 기법을 반복해서 재사용하고 있음을 발견했다. 이 기법들은 Raft의 이해 가능성을 높였을 뿐 아니라 그 정확성을 우리 스스로 확신하기도 쉽게 만들었다.

## 12. 감사의 글

사용자 연구는 Ali Ghodsi, David Mazières, 그리고 Berkeley의 CS 294-91과 Stanford의 CS 240 학생들의 지원 없이는 불가능했을 것이다. Scott Klemmer는 사용자 연구 설계를 도왔고, Nelson Ray는 통계 분석에 조언을 주었다. 사용자 연구용 Paxos 슬라이드는 원래 Lorenzo Alvisi가 만든 슬라이드 덱에서 많이 빌려 왔다. Raft의 미묘한 버그들을 찾아 준 David Mazières와 Ezra Hoch에게 특별히 감사한다. 많은 분들이 논문과 사용자 연구 자료에 유익한 피드백을 주었다. Ed Bugnion, Michael Chan, Hugues Evrard, Daniel Giffin, Arjun Gopalan, Jon Howell, Vimalkumar Jeyakumar, Ankita Kejriwal, Aleksandar Kracun, Amit Levy, Joel Martin, Satoshi Matsushita, Oleg Pesok, David Ramos, Robbert van Renesse, Mendel Rosenblum, Nicolas Schiper, Deian Stefan, Andrew Stone, Ryan Stutsman, David Terei, Stephen Yang, Matei Zaharia, 24명의 익명 학회 심사위원들(중복 포함), 그리고 특히 우리의 셰퍼드 Eddie Kohler에게 감사한다. Werner Vogels가 초기 초안 링크를 트윗해 준 덕분에 Raft가 크게 알려졌다. 이 연구는 Gigascale Systems Research Center와 Multiscale Systems Center(반도체 연구 조합 프로그램인 Focus Center Research Program이 지원하는 여섯 개 연구 센터 중 둘), MARCO와 DARPA가 후원하는 반도체 연구 조합 프로그램 STARnet, 국립과학재단(NSF) 연구비 No. 0963859, 그리고 Facebook, Google, Mellanox, NEC, NetApp, SAP, Samsung의 연구비 지원을 받았다. Diego Ongaro는 Junglee Corporation Stanford Graduate Fellowship의 지원을 받는다.

## 참고문헌

1. Bolosky, W. J., Bradshaw, D., Haagens, R. B., Kusters, N. P., and Li, P. Paxos replicated state machines as the basis of a high-performance data store. In *Proc. NSDI'11, USENIX Conference on Networked Systems Design and Implementation* (2011), USENIX, pp. 141–154.
2. Burrows, M. The Chubby lock service for loosely-coupled distributed systems. In *Proc. OSDI'06, Symposium on Operating Systems Design and Implementation* (2006), USENIX, pp. 335–350.
3. Camargos, L. J., Schmidt, R. M., and Pedone, F. Multicoordinated Paxos. In *Proc. PODC'07, ACM Symposium on Principles of Distributed Computing* (2007), ACM, pp. 316–317.
4. Chandra, T. D., Griesemer, R., and Redstone, J. Paxos made live: an engineering perspective. In *Proc. PODC'07, ACM Symposium on Principles of Distributed Computing* (2007), ACM, pp. 398–407.
5. Chang, F., Dean, J., Ghemawat, S., Hsieh, W. C., Wallach, D. A., Burrows, M., Chandra, T., Fikes, A., and Gruber, R. E. Bigtable: a distributed storage system for structured data. In *Proc. OSDI'06, USENIX Symposium on Operating Systems Design and Implementation* (2006), USENIX, pp. 205–218.
6. Corbett, J. C., Dean, J., Epstein, M., Fikes, A., Frost, C., Furman, J. J., Ghemawat, S., Gubarev, A., Heiser, C., Hochschild, P., Hsieh, W., Kanthak, S., Kogan, E., Li, H., Lloyd, A., Melnik, S., Mwaura, D., Nagle, D., Quinlan, S., Rao, R., Rolig, L., Saito, Y., Szymaniak, M., Taylor, C., Wang, R., and Woodford, D. Spanner: Google's globally-distributed database. In *Proc. OSDI'12, USENIX Conference on Operating Systems Design and Implementation* (2012), USENIX, pp. 251–264.
7. Cousineau, D., Doligez, D., Lamport, L., Merz, S., Ricketts, D., and Vanzetto, H. TLA+ proofs. In *Proc. FM'12, Symposium on Formal Methods* (2012), D. Giannakopoulou and D. Méry, Eds., vol. 7436 of *Lecture Notes in Computer Science*, Springer, pp. 147–154.
8. Ghemawat, S., Gobioff, H., and Leung, S.-T. The Google file system. In *Proc. SOSP'03, ACM Symposium on Operating Systems Principles* (2003), ACM, pp. 29–43.
9. Gray, C., and Cheriton, D. Leases: An efficient fault-tolerant mechanism for distributed file cache consistency. In *Proceedings of the 12th ACM Symposium on Operating Systems Principles* (1989), pp. 202–210.
10. Herlihy, M. P., and Wing, J. M. Linearizability: a correctness condition for concurrent objects. *ACM Transactions on Programming Languages and Systems 12* (July 1990), 463–492.
11. Hunt, P., Konar, M., Junqueira, F. P., and Reed, B. ZooKeeper: wait-free coordination for internet-scale systems. In *Proc ATC'10, USENIX Annual Technical Conference* (2010), USENIX, pp. 145–158.
12. Junqueira, F. P., Reed, B. C., and Serafini, M. Zab: High-performance broadcast for primary-backup systems. In *Proc. DSN'11, IEEE/IFIP Int'l Conf. on Dependable Systems & Networks* (2011), IEEE Computer Society, pp. 245–256.
13. Kirsch, J., and Amir, Y. Paxos for system builders. Tech. Rep. CNDS-2008-2, Johns Hopkins University, 2008.
14. Lamport, L. Time, clocks, and the ordering of events in a distributed system. *Communications of the ACM 21*, 7 (July 1978), 558–565.
15. Lamport, L. The part-time parliament. *ACM Transactions on Computer Systems 16*, 2 (May 1998), 133–169.
16. Lamport, L. Paxos made simple. *ACM SIGACT News 32*, 4 (Dec. 2001), 18–25.
17. Lamport, L. *Specifying Systems, The TLA+ Language and Tools for Hardware and Software Engineers*. Addison-Wesley, 2002.
18. Lamport, L. Generalized consensus and Paxos. Tech. Rep. MSR-TR-2005-33, Microsoft Research, 2005.
19. Lamport, L. Fast paxos. *Distributed Computing 19*, 2 (2006), 79–103.
20. Lampson, B. W. How to build a highly available system using consensus. In *Distributed Algorithms*, O. Baboaglu and K. Marzullo, Eds. Springer-Verlag, 1996, pp. 1–17.
21. Lampson, B. W. The ABCD's of Paxos. In *Proc. PODC'01, ACM Symposium on Principles of Distributed Computing* (2001), ACM, pp. 13–13.
22. Liskov, B., and Cowling, J. Viewstamped replication revisited. Tech. Rep. MIT-CSAIL-TR-2012-021, MIT, July 2012.
23. LogCabin source code. <http://github.com/logcabin/logcabin>.
24. Lorch, J. R., Adya, A., Bolosky, W. J., Chaiken, R., Douceur, J. R., and Howell, J. The SMART way to migrate replicated stateful services. In *Proc. EuroSys'06, ACM SIGOPS/EuroSys European Conference on Computer Systems* (2006), ACM, pp. 103–115.
25. Mao, Y., Junqueira, F. P., and Marzullo, K. Mencius: building efficient replicated state machines for WANs. In *Proc. OSDI'08, USENIX Conference on Operating Systems Design and Implementation* (2008), USENIX, pp. 369–384.
26. Mazières, D. Paxos made practical. <http://www.scs.stanford.edu/~dm/home/papers/paxos.pdf>, Jan. 2007.
27. Moraru, I., Andersen, D. G., and Kaminsky, M. There is more consensus in egalitarian parliaments. In *Proc. SOSP'13, ACM Symposium on Operating System Principles* (2013), ACM.
28. Raft user study. <http://ramcloud.stanford.edu/~ongaro/userstudy/>.
29. Oki, B. M., and Liskov, B. H. Viewstamped replication: A new primary copy method to support highly-available distributed systems. In *Proc. PODC'88, ACM Symposium on Principles of Distributed Computing* (1988), ACM, pp. 8–17.
30. O'Neil, P., Cheng, E., Gawlick, D., and O'Neil, E. The log-structured merge-tree (LSM-tree). *Acta Informatica 33*, 4 (1996), 351–385.
31. Ongaro, D. *Consensus: Bridging Theory and Practice*. PhD thesis, Stanford University, 2014 (work in progress). <http://ramcloud.stanford.edu/~ongaro/thesis.pdf>.
32. Ongaro, D., and Ousterhout, J. In search of an understandable consensus algorithm. In *Proc ATC'14, USENIX Annual Technical Conference* (2014), USENIX.
33. Ousterhout, J., Agrawal, P., Erickson, D., Kozyrakis, C., Leverich, J., Mazières, D., Mitra, S., Narayanan, A., Ongaro, D., Parulkar, G., Rosenblum, M., Rumble, S. M., Stratmann, E., and Stutsman, R. The case for RAMCloud. *Communications of the ACM 54* (July 2011), 121–130.
34. Raft consensus algorithm website. <http://raftconsensus.github.io>.
35. Reed, B. Personal communications, May 17, 2013.
36. Rosenblum, M., and Ousterhout, J. K. The design and implementation of a log-structured file system. *ACM Trans. Comput. Syst. 10* (February 1992), 26–52.
37. Schneider, F. B. Implementing fault-tolerant services using the state machine approach: a tutorial. *ACM Computing Surveys 22*, 4 (Dec. 1990), 299–319.
38. Shvachko, K., Kuang, H., Radia, S., and Chansler, R. The Hadoop distributed file system. In *Proc. MSST'10, Symposium on Mass Storage Systems and Technologies* (2010), IEEE Computer Society, pp. 1–10.
39. van Renesse, R. Paxos made moderately complex. Tech. rep., Cornell University, 2012.

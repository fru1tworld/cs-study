# 일관성 (Consistency)

Jepsen은 분산 시스템의 안전성 속성을 분석합니다 — 특히 일관성 모델의 위반을 식별하는 것으로 잘 알려져 있습니다. 하지만 일관성 모델이란 무엇일까요? 어떤 현상을 허용할까요? 주어진 프로그램에 실제로 어떤 종류의 일관성이 필요할까요?

이 레퍼런스 가이드에서는 다양한 일관성 모델에 대한 기본 정의, 직관적 설명, 이론적 토대, 그리고 관련 문헌 링크를 제공합니다. 우리는 일관성 속성을 업계 실무자, 학계, 그리고 관심 있는 모든 분들이 이해할 수 있도록 만드는 것을 목표로 합니다.

## [모델 (Models)](consistency-models.md)

일관성 모델은 시스템이 할 수 있는 것을 선언하는 안전성 속성입니다. 형식적으로, 일관성 모델은 시스템이 합법적으로 실행할 수 있는 *이력(history)* 의 집합을 정의합니다. 예를 들어, [직렬화 가능성(Serializability)](consistency-models.md#직렬화-가능성-serializable)이라고 알려진 모델은 모든 합법적 이력이 완전히 순서가 정해진 실행과 동등해야 한다는 것을 보장합니다.

## [현상 (Phenomena)](consistency-phenomena.md)

일관성 모델은 종종 금지된 [현상(phenomena)](consistency-phenomena.md): 특정한 연산 패턴의 관점에서 정의됩니다. 예를 들어, [G1a (중단된 읽기)](consistency-phenomena.md#g1a-중단된-읽기-aborted-read)는 트랜잭션이 다른 중단된 트랜잭션이 수행한 쓰기를 관찰할 때 발생합니다.

## [의존성 (Dependencies)](consistency-dependencies.md)

일관성 모델과 현상은 종종 서로 다른 연산 간의 [의존성(dependencies)](consistency-dependencies.md)의 관점에서 정의됩니다. 예를 들어, 트랜잭션 T2가 T1이 쓴 어떤 값을 읽으면 트랜잭션 T2는 트랜잭션 T1에 대해 쓰기-읽기 의존성(write-read dependency)을 가진다고 합니다. 이러한 의존성은 그래프를 형성합니다. 해당 그래프에서 특정 순환을 방지하면 서로 다른 일관성 모델이 만들어집니다.

---

원문 저작권: © Jepsen, LLC.
정확한 정보를 제공하기 위해 최선을 다하고 있지만, 오류를 발견하시면 [알려주세요](mailto:errata@jepsen.io).

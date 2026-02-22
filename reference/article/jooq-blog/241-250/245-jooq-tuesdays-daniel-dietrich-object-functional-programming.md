> 원문: https://blog.jooq.org/jooq-tuesdays-daniel-dietrich-explains-the-benefits-of-object-functional-programming/

# jOOQ 화요일: Daniel Dietrich가 설명하는 객체-함수형 프로그래밍(Object-Functional Programming)의 장점

이번 인터뷰에서는 vavr(Java를 위한 객체-함수형 프로그래밍 라이브러리)의 창시자인 Daniel Dietrich와 함께 그의 라이브러리가 Java 커뮤니티의 함수형 프로그래밍 애호가들 사이에서 상당한 인기를 얻게 된 이유에 대해 이야기합니다.

## 인터뷰

Q: vavr이 왜 이렇게 인기가 있나요?

Daniel은 많은 Java 개발자들이 Java 8의 제한적인 기능 통합에 실망했다고 설명합니다. "Java 8은 람다(Lambdas), 스트림 API(Stream API), CompletableFuture와 같은 획기적인 새 기능들을 가져왔지만", 이러한 추상화들은 API 관점에서 언어에 제대로 통합되지 않았습니다. 그는 "가능한 한 많은 Scala의 장점을 Java로 가져오는 것"을 목표로 합니다. Scala는 Pizza 언어에서 발전했는데, 이 언어는 Java가 수년 후에야 채택한 기능들을 이미 가지고 있었습니다. 그는 vavr을 "객체 지향 프로그래밍과 함수형 프로그래밍 양쪽의 장점"을 제공하는 것으로 설명합니다.

Q: 이전 게스트 포스트 이후로 vavr은 어떤 발전을 이루었나요?

Dietrich는 vavr이 버전 1.2.2에서 2.0.0으로 발전하면서 Option, Try, Future, Promise, 그리고 영속 컬렉션(Persistent Collections)과 같은 기능들을 추가했다고 설명합니다. 2016년 4분기에 출시 예정인 버전 2.1.0에서는 "BitSet, 여러 종류의 MultiMap, 그리고 PriorityQueue"를 추가할 예정입니다. 그는 세 가지 우선순위를 강조합니다: "하위 호환성(Backward Compatibility), 통제된 성장, 그리고 통합 측면".

Q: vavr은 jOOλ, StreamEx, Cyclops, FunctionalJava 같은 라이브러리들과 어떻게 비교되나요?

Dietrich는 세밀한 비교를 제공합니다. 그는 "jOOQ가 독립성을 유지하기 위해 jOOλ가 필요하다"고 언급하며, StreamEx는 유사하게 Java의 Stream을 보강한다고 설명합니다. Cyclops에 대해서는 그것의 "모든 것을 하나로 해결하려는 접근 방식"에 대해 회의적인 입장을 표합니다. FunctionalJava의 목표는 존중하지만, "Scalaz의 대수적 추상화를 Java로 가져오는 것이 어색해 보였다"고 말합니다.

jOOλ와 StreamEx는 Java의 Stream API 위에 구축되는 반면, vavr은 Java의 Stream API와 독립적인 언어 확장으로 기능합니다. Cyclops는 여러 라이브러리를 아우르는 파사드(Facade) 접근 방식을 시도하고, FunctionalJava는 Haskell의 영향을 받은 순수 함수형 프로그래밍을 추구합니다. vavr은 도메인 특화도 아니고 순수 대수적이지도 않은 중간 지점을 차지합니다.

Q: 당신은 컨퍼런스에서 절대 발표하지 않는데, 비결이 뭔가요?

Dietrich는 유머러스하게 자신의 "비결은 실제 일을 다른 사람들에게 위임하는 것"이라고 말합니다. 더 진지하게 말하자면, 그는 "컨퍼런스를 준비하고 여행하는 것보다 vavr 소스 코드에 시간을 쏟는 것이 더 편하다"고 느낍니다. 그는 "코드 리뷰, 토론, 프로젝트 관리"에 노력을 기울입니다.

Q: Project Valhalla와 함께 Java의 미래를 어떻게 보시나요?

그는 Brian Goetz의 미션 선언문을 칭찬하며, 값 타입(Value Types)이 "불필요한 코드와 형식적 절차를 줄여줄" 것으로 기대합니다. 확장된 제네릭(Extended Generics)은 "원시 타입(Primitive Types)만을 위해 존재하는 특수화들을 제거"할 것입니다. 그는 "지역 변수 타입 추론(Local Variable Type Inference)"을 기대하며, "구체화된 제네릭(Reified Generics)"에도 관심을 가지고 있습니다.

## 핵심 주제

Dietrich는 vavr을 Java와 Scala 같은 현대 언어 사이의 표현력 격차를 메우는 것으로 포지셔닝합니다. JVM에서 함수형 프로그래밍 역량을 발전시키면서도 안정성과 하위 호환성을 강조합니다.

## 라이브러리 철학

순수 대수적 접근 방식과 달리, vavr은 실용적인 사용성을 우선시합니다. Dietrich는 개발자들이 라이브러리를 효과적으로 사용하기 위해 카테고리 이론(Category Theory) 개념을 배울 필요가 없어야 한다고 강조합니다. 초점은 단순함에 있습니다: "vavr API의 90%에 접근하려면 하나의 import만 추가하면 충분합니다."

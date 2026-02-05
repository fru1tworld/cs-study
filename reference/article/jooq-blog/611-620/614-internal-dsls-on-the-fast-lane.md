# 내부 DSL의 고속 도로

> 원문: https://blog.jooq.org/internal-dsls-on-the-fast-lane/

Java에서의 내부 DSL에 관한 흥미로운 글을 읽었습니다. 이 글은 Martin Fowler의 DSL에 관한 책을 간략히 요약한 것입니다. 저 역시 외부 및 내부 DSL에 대해 꽤 많이 블로그에 글을 써왔는데, 이는 자연스러운 일입니다. jOOQ가 Java 생태계에서 가장 크고 가장 진보된 무료 오픈 소스 내부 DSL 구현체이기 때문입니다. 현재 개발 중인 다른 DSL들과 달리, jOOQ는 BNF를 API의 기반으로 사용합니다. 이를 통해 단순한 메서드 체이닝뿐만 아니라 문법과 같은 컨텍스트도 API에서 형식화할 수 있음을 보장합니다.

여러분만의 DSL과 문법을 위해 이러한 API를 수동으로 구성하는 방법은 다음의 인기 있는 블로그 게시물에서 설명했습니다: "The Java Fluent API Designer Crash Course"

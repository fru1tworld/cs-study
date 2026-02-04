# Java 10의 새로운 로컬 변수 타입 추론

> 원문: https://blog.jooq.org/java-as-new-local-variable-type-inference/

프로그래밍 언어 애호가에게 이보다 더 흥미로운 뉴스는 없을 것입니다! 이제 로컬 변수 타입 추론에 대한 JEP 286이 "후보" 상태로 등록되었습니다. 그리고 Brian Goetz의 피드백 요청이 있었는데, 여러분도 참여해 주시면 좋겠습니다: http://mail.openjdk.java.net/pipermail/platform-jep-discuss/2016-March/000037.html 꼭 참여해 주세요. 설문조사는 3월 9일부터 3월 16일까지만 열려 있습니다! 이것은 구현될 기능이 아닙니다. 구현될 수도 있는 기능입니다. 따라서 아직 특정 Java 버전이 정해지지 않았기 때문에 Java 버전을 "A"(Awesome의 A)라고 부르겠습니다.

## 로컬 변수 타입 추론이란 무엇이며 왜 좋은가?

다른 여러 언어들이 꽤 오래전부터 가지고 있던 기능을 살펴보겠습니다. 이 블로그 글에서는 Java에 계획되어 있을 수 있는 특정 구현이 아닌 일반적인 아이디어에 대해 논의하고자 합니다. 이것이 Java에 어떻게 맞아떨어지는지에 대한 큰 그림을 확실히 알 수 없기 때문입니다. Java와 일부 다른 언어에서는 타입을 항상 명시적이고 장황하게 선언합니다. 예를 들어, 다음과 같이 작성합니다:

```java
// Java 5와 6
List<String> list = new ArrayList<String>();

// Java 7
List<String> list = new ArrayList<>();
```

Java 7에서 유용한 다이아몬드 연산자 `<>`를 통해 약간의 문법적 설탕이 추가된 것을 주목하세요. 이것은 Java 방식으로 불필요한 중복을 제거하는 데 도움이 됩니다. 즉, "타겟 타이핑"을 적용하여 타입이 "타겟"에 의해 정의됩니다. 가능한 타겟은 다음과 같습니다:

* 로컬 변수 선언
* 메서드 인자 (메서드 외부와 내부 모두에서)
* 클래스 멤버

많은 경우에 타겟 타입은 명시적으로 선언되어야 하므로(메서드 인자, 클래스 멤버), Java의 접근 방식은 많은 의미가 있습니다. 그러나 로컬 변수의 경우, 타겟 타입이 실제로 선언될 필요가 없습니다. 타입 정의가 벗어날 수 없는 매우 제한된 스코프에 바인딩되어 있기 때문에, 소스 코드가 명시적으로 작성하지 않아도 컴파일러가 "소스 타입"에서 추론할 수 있습니다. 이것은 다음과 같은 것들을 할 수 있게 됨을 의미합니다:

```java
// JEP에서 제안된 Java 10

// ArrayList<String>으로 추론
var list = new ArrayList<String>();

// Stream<String>으로 추론
val stream = list.stream();
```

위 예제에서 `var`는 가변(non-final) 로컬 변수를 나타내고, `val`은 불변(final) 로컬 변수를 나타냅니다. 다음과 같이 작성할 때 타입이 이미 추론되는 것처럼, list의 타입이 실제로 필요하지 않았다는 것을 주목하세요:

```java
stream = new ArrayList<String>().stream();
```

이것은 Java 8에서 이미 이런 종류의 타입 추론이 있는 람다 표현식과 다르지 않게 작동할 것입니다:

```java
List<String> list = new ArrayList<>();

// String으로 추론
list.forEach(s -> {
    System.out.println(s);
});
```

람다 인자를 로컬 변수로 생각하세요. 이러한 람다 표현식의 대안 문법은 다음과 같을 수 있습니다:

```java
List<String> list = new ArrayList<>();

// String으로 추론
list.forEach((val s) -> {
    System.out.println(s);
});
```

## 다른 언어에는 있지만, 정말 좋은가?

이러한 다른 언어들 중에는 C#과 Scala, 그리고 JavaScript도 있습니다. YAGNI(You Aren't Gonna Need It)는 이 기능에 대한 일반적인 반응일 것입니다. 대부분의 사람들에게 이것은 단순히 항상 모든 타입을 입력하지 않아도 되는 편의성입니다. 일부 사람들은 코드를 읽을 때 타입이 명시적으로 작성된 것을 선호할 수 있습니다. 특히 복잡한 Java 8 Stream 처리 파이프라인이 있을 때, 그 과정에서 추론되는 모든 타입을 추적하기 어려울 수 있습니다. 이에 대한 예시는 jOOλ의 윈도우 함수 지원에 관한 우리의 글에서 볼 수 있습니다:

```java
BigDecimal currentBalance = new BigDecimal("19985.81");

Seq.of(
    tuple(9997, "2014-03-18", new BigDecimal("99.17")),
    tuple(9981, "2014-03-16", new BigDecimal("71.44")),
    tuple(9979, "2014-03-16", new BigDecimal("-94.60")),
    tuple(9977, "2014-03-16", new BigDecimal("-6.96")),
    tuple(9971, "2014-03-15", new BigDecimal("-65.95")))
.window(Comparator
    .comparing((Tuple3<Integer, String, BigDecimal> t)
        -> t.v1, reverseOrder())
    .thenComparing(t -> t.v2), Long.MIN_VALUE, -1)
.map(w -> w.value().concat(
     currentBalance.subtract(w.sum(t -> t.v3)
                              .orElse(BigDecimal.ZERO))
));
```

위 코드는 다음과 같은 결과를 산출하는 누적 합계 계산을 구현합니다:

```
+------+------------+--------+----------+
|   v0 | v1         |     v2 |       v3 |
+------+------------+--------+----------+
| 9997 | 2014-03-18 |  99.17 | 19985.81 |
| 9981 | 2014-03-16 |  71.44 | 19886.64 |
| 9979 | 2014-03-16 | -94.60 | 19815.20 |
| 9977 | 2014-03-16 |  -6.96 | 19909.80 |
| 9971 | 2014-03-15 | -65.95 | 19916.76 |
+------+------------+--------+----------+
```

`Tuple3` 타입은 기존 Java 8의 제한된 타입 추론 기능 때문에 선언되어야 하지만(일반화된 타겟 타입 추론에 관한 이 글도 참조하세요), 다른 모든 타입을 추적할 수 있나요? 결과를 쉽게 예측할 수 있나요? 일부 사람들은 짧은 스타일을 선호하고, 다른 사람들은 이렇게 주장합니다:

> "저는 항상 Scala에서 타입을 선언합니다. 이것이 문법적 설탕 이상으로 Java의 게임에 무언가를 추가한다고 생각하지 않습니다." - Steve Chaloner

반면에, `Tuple3<Integer, String, BigDecimal>` 같은 타입을 수동으로 작성하는 것을 좋아하시나요? 또는 jOOQ로 작업할 때, 다음 동일한 코드의 버전 중 어느 것을 선호하시나요?

```java
// 명시적 타이핑
// ----------------------------------------
for (Record3<String, Integer, Date> record : ctx
    .select(BOOK.TITLE, BOOK.ID, BOOK.MODIFIED_AT)
    .from(BOOK)
    .where(TITLE.like("A%"))
) {
    // record로 작업 수행
    String title = record.value1();
}

// "상관없음" 타이핑
// ----------------------------------------
for (Record record : ctx
    .select(BOOK.TITLE, BOOK.ID, BOOK.MODIFIED_AT)
    .from(BOOK)
    .where(TITLE.like("A%"))
) {
    // record로 작업 수행
    String title = record.getValue(0, String.class);
}

// 암시적 타이핑
// ----------------------------------------
for (val record : ctx
    .select(BOOK.TITLE, BOOK.ID, BOOK.MODIFIED_AT)
    .from(BOOK)
    .where(TITLE.like("A%"))
) {
    // record로 작업 수행
    String title = record.value1();
}
```

여러분 중 전체 제네릭 타입을 명시적으로 작성하고 싶어하는 분은 거의 없을 것입니다. 하지만 컴파일러가 여전히 그것을 기억할 수 있다면, 정말 멋지지 않을까요? 그리고 이것은 선택적인 기능입니다. 언제든지 명시적 타입 선언으로 돌아갈 수 있습니다.

## 사용처 가변성(Use-site variance)의 엣지 케이스

이러한 종류의 타입 추론 없이는 불가능한 것들이 있으며, 이것들은 사용처 가변성과 Java에서 구현된 제네릭의 특성과 관련이 있습니다. 사용처 가변성과 와일드카드를 사용하면, 결정 불가능하기 때문에 아무것에도 할당할 수 없는 "위험한" 타입을 구성할 수 있습니다. 자세한 내용은 Ross Tate의 Taming Wildcards in Java's Type System 논문을 읽어보세요. 사용처 가변성은 메서드 반환 타입에서 노출될 때도 골칫거리인데, 일부 라이브러리에서 볼 수 있습니다:

* 사용자에게 가하는 이 고통에 대해 신경 쓰지 않았거나
* Java에 선언처 가변성이 없기 때문에 더 나은 해결책을 찾지 못했거나
* 이 문제를 인식하지 못했거나

예를 들어:

```java
interface Node {
    void add(List<? extends Node> children);
    List<? extends Node> children();
}
```

트리 노드가 자식들의 리스트를 반환하는 트리 데이터 구조 라이브러리를 상상해 보세요. 기술적으로 올바른 children 타입은 `List<? extends Node>`일 것입니다. 왜냐하면 자식들은 Node 하위 타입이고, Node 하위 타입 리스트를 사용하는 것은 완벽하게 괜찮기 때문입니다. `add()` 메서드에서 이 타입을 받는 것은 API 설계 관점에서 훌륭합니다. 예를 들어 사람들이 `List<LeafNode>`를 추가할 수 있게 해줍니다. 하지만 `children()`에서 이것을 반환하는 것은 끔찍합니다. 왜냐하면 이제 유일한 옵션들이 다음과 같기 때문입니다:

```java
// Raw 타입. 별로
List children = parent.children();

// 와일드 카드. 별로
List<?> children = parent.children();

// 전체 타입 선언. 최악
List<? extends Node> children = parent.children();
```

JEP 286을 사용하면, 이 모든 것을 우회하고 이 멋진 네 번째 옵션을 가질 수 있을 것입니다:

```java
// 멋짐. 컴파일러는 이것이
// List<? extends Node>임을 알고 있음
val children = parent.children();
```

## 결론

로컬 변수 타입 추론은 뜨거운 주제입니다. 이것은 전적으로 선택 사항이며, 우리는 이것이 필요하지 않습니다. 하지만 이것은 많은 것들을 훨씬 더 쉽게 만들어 줍니다. 특히 수많은 제네릭을 다룰 때 그렇습니다. 우리는 타입 추론이 람다 표현식과 복잡한 Java 8 Stream 변환을 다룰 때 킬러 기능이라는 것을 보았습니다. 물론, 긴 구문 전체에서 모든 타입을 추적하는 것이 더 어려워질 것입니다. 하지만 동시에, 그 타입들이 모두 작성되어 있다면, 구문이 매우 읽기 어려워질 것입니다(그리고 종종 작성하기도 매우 어렵습니다). 타입 추론은 타입 안전성을 포기하지 않으면서 개발자들을 더 생산적으로 만드는 데 도움이 됩니다. 이것은 실제로 타입 안전성을 장려합니다. API 설계자들이 이제 복잡한 제네릭 타입을 사용자에게 노출하는 것을 덜 꺼리게 되기 때문입니다. 사용자들이 이러한 타입을 더 쉽게 사용할 수 있기 때문입니다(다시 jOOQ 예제를 참조하세요). 사실, 이 기능은 이미 Java의 다양한 상황에서 존재합니다. 단지 로컬 변수에 값을 할당하고 이름을 부여할 때는 아닙니다. 여러분의 의견이 무엇이든: 커뮤니티에 공유하고 이 설문조사에 답해 주세요: http://mail.openjdk.java.net/pipermail/platform-jep-discuss/2016-March/000037.html Java 10을 기대합니다.

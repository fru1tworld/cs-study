# 함수형 프로그래밍이 (마침내) GoF 패턴을 없앨 방법

> 원문: https://blog.jooq.org/how-functional-programming-will-finally-do-away-with-the-gof-patterns/

다음의 아름다운 Scala 코드를 살펴보세요. 대수적 데이터 타입(Algebraic Data Types)과 패턴 매칭을 사용하여 트리의 깊이를 계산하는 예제입니다:

```scala
def depth(t: Tree): Int = t match {
  case Empty => 0
  case Leaf(n) => 1
  case Node(l, r) => 1 + max(depth(l), depth(r))
}
```

아름답지 않나요? 이 코드를 Java로 작성하면 어떻게 될까요? `instanceof`를 사용하면 다음과 같이 작성할 수 있습니다:

```java
public static int depth(Tree t) {
  if (t instanceof Empty) return 0;
  if (t instanceof Leaf) return 1;
  if (t instanceof Node)
    return 1 + max(depth(((Node) t).left), depth(((Node) t).right));
  throw new RuntimeException("Inexhaustive pattern match on Tree.");
}
```

`instanceof`를 사용하는 것이 약간 꺼림칙하게 느껴질 수 있습니다. 하지만 사실 위 코드는 매우 직접적이고 간단합니다. 반면 비지터(Visitor) 패턴을 적용하면 어떻게 될까요? `instanceof`를 사용한 7줄의 코드가 약 200줄의 이상한 인터페이스들, 추상 클래스들, 그리고 암호 같은 `accept()`와 `visit()` 메서드들로 불어나게 됩니다.

## 1990년대의 객체지향 세뇌

비지터 패턴은 1990년대 객체지향 세뇌 시절에 발명되었습니다. 그 당시에는 모든 것이 객체여야 했습니다. 만약 그것이 객체가 아니라면, 당신은 그것을 잘못하고 있는 것이었죠. 모든 것이 객체가 되어야 했기 때문에, 사람들은 객체 데이터베이스를 발명했습니다(지금은 모두 사라졌습니다). SQL 표준은 ORDBMS 기능으로 확장되었고, 이는 주로 Oracle, PostgreSQL, Informix에서 구현되었습니다. 하지만 이러한 기능들은 업계에서 널리 채택되지 않았습니다.

## Java 8 이후의 회복

Java 8 이후로, 마침내 우리는 90년대 초기 객체지향의 손상으로부터 회복하기 시작했습니다. 우리는 더 데이터 중심적이고, 함수형이며, 불변적인 프로그래밍 모델로 나아가고 있습니다. 이 모델에서 SQL은 피해야 할 것이 아니라 가치 있게 여겨집니다.

## Mario Fusco의 함수형 관점에서 본 GoF 패턴

Mario Fusco는 함수형 접근 방식이 전통적인 GoF 패턴들을 어떻게 단순화할 수 있는지 보여주는 훌륭한 시리즈를 작성했습니다:

- [Gang of Four Patterns in a Functional Light: Part 1](https://www.voxxed.com/2016/04/gang-four-patterns-functional-light-part-1/)
- [Gang of Four Patterns in a Functional Light: Part 2](https://www.voxxed.com/2016/05/gang-four-patterns-functional-light-part-2/)
- [Gang of Four Patterns in a Functional Light: Part 3](https://www.voxxed.com/2016/05/gang-four-patterns-functional-light-part-3/)
- [Gang of Four Patterns in a Functional Light: Part 4](https://www.voxxed.com/2016/05/gang-four-patterns-functional-light-part-4/)

이 시리즈는 람다와 함수형 프로그래밍이 어떻게 복잡한 객체지향 패턴들을 더 간단하고 이해하기 쉬운 코드로 대체할 수 있는지 보여줍니다. 함수를 직접 전달함으로써, 정교한 클래스 계층 구조를 만들 필요 없이 더 간결하고 유지보수하기 쉬운 코드를 작성할 수 있습니다.

## 결론

함수형 프로그래밍을 받아들이세요. 많은 GoF 패턴들은 함수형 접근 방식으로 훨씬 더 우아하게 해결될 수 있습니다. Java 8의 람다, 스트림, 그리고 함수형 인터페이스를 활용하면, 과거의 복잡한 디자인 패턴들을 간단한 함수 조합으로 대체할 수 있습니다.

즐거운 함수형 프로그래밍 되세요!

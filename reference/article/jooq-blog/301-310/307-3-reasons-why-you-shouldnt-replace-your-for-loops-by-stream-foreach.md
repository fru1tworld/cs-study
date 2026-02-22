# for 루프를 Stream.forEach()로 대체하면 안 되는 3가지 이유

> 원문: https://blog.jooq.org/3-reasons-why-you-shouldnt-replace-your-for-loops-by-stream-foreach/

멋지다! 우리 코드베이스를 Java 8로 마이그레이션하고 있어. 모든 것을 함수로 대체할 거야. 디자인 패턴은 버리고, 객체지향도 제거하자. 맞아! 시작하자!

## 잠깐만요

Java 8이 출시된 지 1년이 넘었고, 그 설렘은 이제 일상적인 업무로 돌아갔습니다. baeldung.com에서 2015년 5월에 실시한 비대표적 연구에 따르면 독자의 38%가 Java 8을 도입했다고 합니다. 그 이전인 2014년 말 Typsafe의 연구에서는 사용자 중 27%가 Java 8을 도입했다고 주장했습니다.

## 이것이 코드베이스에 의미하는 바는?

일부 Java 7 -> Java 8 마이그레이션 리팩토링은 너무나 당연합니다. 예를 들어, `ExecutorService`에 `Callable`을 전달할 때:

```java
ExecutorService s = ...

// Java 7 - 별로...
Future<String> f = s.submit(
    new Callable<String>() {
        @Override
        public String call() {
            return "Hello World";
        }
    }
);

// Java 8 - 당연히!
Future<String> f = s.submit(() -> "Hello World");
```

익명 클래스 스타일은 여기서 정말 아무런 가치도 추가하지 않습니다. 이런 당연한 것들 외에도 덜 명확한 주제들이 있습니다. 예를 들어, 외부 반복자를 사용할지 내부 반복자를 사용할지에 대한 문제입니다. 2007년 Neil Gafter가 쓴 이 시대를 초월한 주제에 대한 흥미로운 글도 참조하세요: http://gafter.blogspot.ch/2007/07/internal-versus-external-iterators.html 다음 두 가지 로직의 결과는 동일합니다:

```java
List<Integer> list = Arrays.asList(1, 2, 3);

// 전통적인 방식
for (Integer i : list)
    System.out.println(i);

// "현대적인" 방식
list.forEach(System.out::println);
```

저는 "현대적인" 접근 방식은 극도로 주의해서 사용해야 한다고 주장합니다. 즉, 내부의 함수형 반복에서 진정으로 이점을 얻을 때만 사용해야 합니다(예: Stream의 `map()`, `flatMap()` 및 기타 연산을 통해 일련의 작업을 연결할 때). 다음은 전통적인 방식과 비교한 "현대적인" 접근 방식의 단점 목록입니다:

## 1. 성능 - 손해를 볼 것입니다

Angelika Langer는 이 주제를 그녀의 글과 컨퍼런스에서 발표하는 관련 강연에서 충분히 잘 정리했습니다:

> Java 성능 튜토리얼 – Java 8 스트림은 얼마나 빠른가?

참고: 그녀의 벤치마크는 Nicolai Parlog에 의해 JMH로 반복되었으며, 극단적인 경우에 약간 다른 결과가 나왔지만 본질적으로 다르지는 않았습니다:

> 스트림 성능

두 글(이 글도 마찬가지)은 2015년에 작성되었다는 점에 유의하세요. 상황이 더 나아졌을 수 있지만, 여전히 측정 가능한 차이가 있습니다. 많은 경우 성능은 중요하지 않으며, 조기 최적화를 해서는 안 됩니다 – 따라서 이 인수가 그 자체로는 실제 인수가 아니라고 주장할 수 있습니다. 하지만 이 경우에는 그런 태도에 반박하겠습니다. 일반적인 `for` 루프와 비교하여 `Stream.forEach()`의 오버헤드가 너무 심각해서 기본으로 사용하면 애플리케이션 전체에 걸쳐 쓸모없는 CPU 사이클만 쌓이게 됩니다. 루프 스타일의 선택만으로 10%-20% 더 많은 CPU 소비에 대해 이야기하고 있다면, 우리는 근본적으로 뭔가 잘못한 것입니다. 네 – 개별 루프는 중요하지 않지만, 전체 시스템의 부하는 피할 수 있었습니다. 다음은 박싱된 int 리스트에서 최대값을 찾는 일반적인 루프에 대한 Angelika의 벤치마크 결과입니다:

```
ArrayList, for-loop : 6.55 ms
ArrayList, seq. stream: 8.33 ms
```

다른 경우, 원시 데이터 타입에 대해 상대적으로 쉬운 계산을 수행할 때는, 확실히 전통적인 `for` 루프로 돌아가야 합니다(그리고 컬렉션보다는 배열을 선호해야 합니다). 다음은 원시 int 배열에서 최대값을 찾는 일반적인 루프에 대한 Angelika의 벤치마크 결과입니다:

```
int-array, for-loop : 0.36 ms
int-array, seq. stream: 5.35 ms
```

이런 극단적인 수치는 Nicolai Parlog나 Heinz Kabutz에 의해 재현되지 않았지만, 여전히 상당한 차이는 재현될 수 있었습니다. 조기 최적화는 좋지 않지만, 조기 최적화 회피를 맹목적으로 따르는 카고 컬트는 더 나쁩니다. 우리가 어떤 맥락에 있는지 성찰하고, 그 맥락에서 올바른 결정을 내리는 것이 중요합니다. 성능에 대해서는 이전에 블로그에서 다룬 적이 있습니다. Java의 쉬운 성능 최적화 Top 10 글을 참조하세요.

## 2. 가독성 - 적어도 대부분의 사람들에게는

우리는 소프트웨어 엔지니어입니다. 우리는 항상 코드 스타일이 정말 중요한 것처럼 논의합니다. 예를 들어, 공백이나 중괄호. 우리가 그렇게 하는 이유는 소프트웨어 유지보수가 어렵기 때문입니다. 특히 다른 사람이 작성한 코드. 오래전에. 아마도 Java로 전환하기 전에 C 코드만 작성했던 사람이. 물론, 지금까지 본 예제에서는 가독성 문제가 없습니다. 두 버전은 아마 동등합니다:

```java
List<Integer> list = Arrays.asList(1, 2, 3);

// 전통적인 방식
for (Integer i : list)
    System.out.println(i);

// "현대적인" 방식
list.forEach(System.out::println);
```

하지만 여기서는 어떨까요:

```java
List<Integer> list = Arrays.asList(1, 2, 3);

// 전통적인 방식
for (Integer i : list)
    for (int j = 0; j < i; j++)
        System.out.println(i * j);

// "현대적인" 방식
list.forEach(i -> {
    IntStream.range(0, i).forEach(j -> {
        System.out.println(i * j);
    });
});
```

상황이 좀 더 흥미롭고 특이해지기 시작합니다. "더 나쁘다"고 말하는 것은 아닙니다. 연습과 습관의 문제입니다. 그리고 이 문제에 대한 흑백의 답은 없습니다. 하지만 나머지 코드베이스가 명령형이라면(아마 그럴 것입니다), range 선언과 `forEach()` 호출, 그리고 람다를 중첩하는 것은 확실히 특이하여 팀에서 인지적 마찰을 일으킵니다. 명령형 접근 방식이 동등한 함수형 접근 방식보다 정말로 더 어색하게 느껴지는 예제를 구성할 수 있습니다. 다음에서 볼 수 있듯이:

> 명령형 vs. 함수형 – 관심사의 분리 pic.twitter.com/G2cC6iBkDJ
>
> — Mario Fusco 🇪🇺 (@mariofusco) 2015년 3월 1일

하지만 많은 상황에서는 그렇지 않으며, 상대적으로 쉬운 명령형의 함수형 동등물을 작성하는 것은 꽤 어렵습니다(그리고 다시, 비효율적입니다). 예제는 이전 포스트의 이 블로그에서 볼 수 있습니다:

> Java 8 함수형 프로그래밍을 사용하여 알파벳 시퀀스를 생성하는 방법

그 포스트에서 우리는 다음과 같은 문자 시퀀스를 생성했습니다:

A, B, ..., Z, AA, AB, ..., ZZ, AAA

… MS Excel의 열과 유사합니다: 명령형 접근 방식(원래 Stack Overflow의 익명 사용자가 작성):

```java
import static java.lang.Math.*;

private static String getString(int n) {
    char[] buf = new char[(int) floor(log(25 * (n + 1)) / log(26))];
    for (int i = buf.length - 1; i >= 0; i--) {
        n--;
        buf[i] = (char) ('A' + n % 26);
        n /= 26;
    }
    return new String(buf);
}
```

… 아마도 간결함 수준에서 함수형 접근 방식을 능가합니다:

```java
import java.util.List;

import org.jooq.lambda.Seq;

public class Test {
    public static void main(String[] args) {
        int max = 3;

        List<String> alphabet = Seq
            .rangeClosed('A', 'Z')
            .map(Object::toString)
            .toList();

        Seq.rangeClosed(1, max)
           .flatMap(length ->
               Seq.rangeClosed(1, length - 1)
                  .foldLeft(Seq.seq(alphabet), (s, i) ->
                      s.crossJoin(Seq.seq(alphabet))
                       .map(t -> t.v1 + t.v2)))
           .forEach(System.out::println);
    }
}
```

그리고 이것은 이미 jOOλ를 사용하여 함수형 Java 작성을 단순화한 것입니다.

## 3. 유지보수성

이전 예제를 다시 생각해 봅시다. 값을 곱하는 대신, 이제 나눕니다.

```java
List<Integer> list = Arrays.asList(1, 2, 3);

// 전통적인 방식
for (Integer i : list)
    for (int j = 0; j < i; j++)
        System.out.println(i / j);

// "현대적인" 방식
list.forEach(i -> {
    IntStream.range(0, i).forEach(j -> {
        System.out.println(i / j);
    });
});
```

분명히 이것은 문제를 야기하며, 예외 스택 트레이스에서 즉시 문제를 볼 수 있습니다.

전통적인 방식

```
Exception in thread "main" java.lang.ArithmeticException: / by zero
	at Test.main(Test.java:13)
```

현대적인 방식

```
Exception in thread "main" java.lang.ArithmeticException: / by zero
	at Test.lambda$1(Test.java:18)
	at java.util.stream.Streams$RangeIntSpliterator.forEachRemaining(Streams.java:110)
	at java.util.stream.IntPipeline$Head.forEach(IntPipeline.java:557)
	at Test.lambda$0(Test.java:17)
	at java.util.Arrays$ArrayList.forEach(Arrays.java:3880)
	at Test.main(Test.java:16)
```

와. 우리가 방금...? 네. 이것이 처음 #1에서 성능 문제가 있었던 것과 같은 이유입니다. 내부 반복은 JVM과 라이브러리에 훨씬 더 많은 작업을 요구합니다. 그리고 이것은 극도로 쉬운 사용 사례입니다. `AA, AB, .., ZZ` 시리즈 생성으로 같은 것을 보여줄 수 있었습니다. 유지보수 관점에서, 함수형 프로그래밍 스타일은 명령형 프로그래밍보다 훨씬 어려울 수 있습니다 – 특히 레거시 코드에서 두 스타일을 맹목적으로 혼합할 때.

## 결론

이것은 보통 함수형 프로그래밍 찬성, 선언적 프로그래밍 찬성 블로그입니다. 우리는 람다를 좋아합니다. 우리는 SQL을 좋아합니다. 그리고 결합하면 기적을 만들 수 있습니다. 하지만 Java 8로 마이그레이션하고 코드에서 더 함수형 스타일을 사용하는 것을 고려할 때, FP가 항상 더 좋은 것은 아니라는 점에 유의하세요 – 여러 가지 이유로. 사실, 그것은 결코 "더 좋은" 것이 아니라, 단지 다를 뿐이며 문제에 대해 다르게 추론할 수 있게 해줍니다. 우리 Java 개발자들은 연습하고, FP를 언제 사용할지, 언제 OO/명령형을 고수할지에 대한 직관적인 이해를 발전시켜야 합니다. 적절한 양의 연습을 통해, 둘을 결합하면 소프트웨어를 개선하는 데 도움이 될 것입니다. 또는 Uncle Bob의 말로 표현하자면:

> "여기서의 핵심 중의 핵심은 간단히 이것입니다. OO 프로그래밍은 그것이 무엇인지 알 때 좋습니다. 함수형 프로그래밍은 그것이 무엇인지 알 때 좋습니다. 그리고 함수형 OO 프로그래밍도 그것이 무엇인지 알게 되면 좋습니다." (http://blog.cleancoder.com/uncle-bob/2014/11/24/FPvsOO.html)

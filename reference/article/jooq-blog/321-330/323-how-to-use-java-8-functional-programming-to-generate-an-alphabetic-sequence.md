# Java 8 함수형 프로그래밍으로 알파벳 시퀀스를 생성하는 방법

> 원문: https://blog.jooq.org/how-to-use-java-8-functional-programming-to-generate-an-alphabetic-sequence/

사용자 'mip'이 올린 흥미로운 Stack Overflow 질문을 발견했습니다. 질문은 다음과 같았습니다:

> 알파벳 시퀀스를 생성하는 방법을 찾고 있습니다: A, B, C, ..., Z, AA, AB, AC, ..., ZZ

Excel 스프레드시트 열 헤더를 생성하는 것과 같은 문제입니다. 함수형 프로그래밍을 사용해서 이 도전을 해결해 보겠습니다.

## 알고리즘 분해

이 알고리즘은 세 가지 핵심 요소로 분해할 수 있습니다:

1. 재사용 가능한 알파벳 표현
2. 시퀀스에서 문자 개수의 상한값
3. 문자와 이전에 생성된 조합을 결합하는 데카르트 곱

## 알파벳 생성

[jOOλ 라이브러리](https://github.com/jOOQ/jOOL)를 사용하여 알파벳을 프로그래밍 방식으로 생성할 수 있습니다:

```java
List<String> alphabet = Seq
    .rangeClosed('A', 'Z')
    .map(Object::toString)
    .toList();
```

이 코드는 A부터 Z까지의 문자를 생성하고, 문자열로 변환한 다음, 리스트로 수집합니다.

## 상한값 제어

`rangeClosed()`와 `flatMap()`을 사용하여 다양한 시퀀스 길이를 처리할 수 있습니다:

```java
Seq.rangeClosed(1, 2)  // 1=A..Z, 2=AA..ZZ
   .flatMap(length -> ...)
   .forEach(System.out::println);
```

## 데카르트 곱

가장 까다로운 부분은 `foldLeft()`와 `crossJoin()`을 사용하여 문자들을 반복적으로 결합하는 것입니다:

```java
Seq.rangeClosed(1, length - 1)
   .foldLeft(Seq.seq(alphabet), (s, i) ->
       s.crossJoin(Seq.seq(alphabet))
        .map(t -> t.v1 + t.v2))
```

## 핵심 함수 설명

### foldLeft

`foldLeft`는 `reduce()`와 동등하지만, "결합법칙을 요구하지 않고 왼쪽에서 오른쪽으로 진행하는 것이 보장됩니다." 시드 값을 가진 명령형 루프처럼 작동합니다.

### crossJoin

`crossJoin`은 두 시퀀스 사이의 데카르트 곱을 생성하여 각 요소를 체계적으로 결합합니다.

### flatMap

`flatMap`은 각 값에 대해 새로운 스트림을 생성하고 해당 스트림들을 하나로 평탄화합니다. 명령형 프로그래밍의 중첩 루프와 기능적으로 동등합니다.

## 완전한 구현

다음은 함수형 합성을 사용하여 지정된 길이까지 시퀀스를 생성하는 전체 프로그램입니다:

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

`max` 파라미터를 3으로 설정하면 다음과 같은 출력이 생성됩니다: A, B, C, ..., Z, AA, AB, AC, ..., ZZ, AAA, AAB, ..., ZZZ

## 성능에 대한 참고사항

이 함수형 접근 방식이 이 특정 경우에 가장 최적의 알고리즘은 아닙니다. 로그 계산을 사용하는 수학적 알고리즘이 훨씬 더 빠르게 실행됩니다:

```java
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

후자의 수학적 알고리즘은 앞서 소개한 함수형 알고리즘보다 훨씬 훨씬 빠르게 실행됩니다. 그러나 함수형 접근 방식은 코드의 가독성과 선언적 특성 면에서 장점이 있으며, 성능이 크리티컬하지 않은 상황에서는 좋은 선택이 될 수 있습니다.

## 관련 자료

- [jOOλ GitHub 저장소](https://github.com/jOOQ/jOOL)
- [Sebastian Millies의 Kleisli를 이용한 데카르트 곱 분석](http://sebastian-millies.blogspot.de/2015/09/cartesian-products-with-kleisli.html)
- [jOOQ 공식 사이트](https://www.jooq.org)

# Java 8에서 명령형과 함수형 알고리즘 비교하기

> 원문: https://blog.jooq.org/comparing-imperative-and-functional-algorithms-in-java-8/

Mario Fusco의 인기 있는 트윗은 명령형과 함수형 접근 방식의 유사한 알고리즘 간의 주요 차이점이 실제로 무엇인지 인상적으로 보여줍니다:

> "명령형 vs 함수형 – 관심사의 분리"

두 알고리즘 모두 같은 일을 수행하며, 아마도 비슷한 속도와 합리적인 성능을 보일 것입니다. 그러나 한 알고리즘이 다른 것보다 훨씬 작성하고 읽기 쉽습니다. 차이점은 명령형 프로그래밍에서는 서로 다른 알고리즘적 요구사항들이 코드 블록 전체에 퍼져 있는 반면, 함수형 프로그래밍에서는 각 요구사항이 자신만의 작은 코드 라인을 갖는다는 사실에 있습니다.

색상으로 구분된 관심사 분리는 다음과 같습니다:
- 녹색: 오류 처리
- 파란색: 중지 조건
- 빨간색: IO 작업
- 노란색: "비즈니스 로직"

함수형 프로그래밍이 항상 명령형 프로그래밍을 이기는 것은 아닙니다. 그러나 Stack Overflow 사용자 Aurora_Titanium의 예제는 배열에서 중복 값을 계산할 때 명확한 차이를 보여줍니다.

## 문제

주어진 배열: `int[] list = new int[]{1,2,3,4,5,6,7,8,8,8,9,10};`

예상 출력: Duplicate: 8. Sum of all duplicate values: 24

## 명령형 접근 방식

```java
int[] array = new int[] {
    1, 2, 3, 4, 5, 6, 7, 8, 8, 8, 9, 10
};

int sum = 0;
for (int j = 0; j < array.length; j++)
{
    for (int k = j + 1; k < array.length; k++)
    {
        if (k != j && array[k] == array[j])
        {
            sum = sum + array[k];
            System.out.println(
                "Duplicate found: "
              + array[k]
              + " "
              + "Sum of the duplicate value is " + sum);
        }
    }
}
```

이 중첩 루프 접근 방식은 "읽기 어려운 코드"를 사용하며 구현 전체에 여러 관심사를 혼합합니다.

## 함수형 접근 방식

```java
int[] array = new int[] {
    1, 2, 3, 4, 5, 6, 7, 8, 8, 8, 9, 10
};

IntStream.of(array)
         .boxed()
         .collect(groupingBy(i -> i))
         .entrySet()
         .stream()
         .filter(e -> e.getValue().size() > 1)
         .forEach(e -> {
             System.out.println(
                 "Duplicates found for : "
               + e.getKey()
               + " their sum being : "
               + e.getValue()
                  .stream()
                  .collect(summingInt(i -> i)));
         });
```

설명과 함께:

```java
int[] array = new int[] {
    1, 2, 3, 4, 5, 6, 7, 8, 8, 8, 9, 10
};

// 데이터로부터 Stream<Integer> 생성
IntStream.of(array)
         .boxed()

// 값들을 Map<Integer, List<Integer>>로 그룹화
         .collect(groupingBy(i -> i))

// 그룹에 요소가 1개만 있는 맵 값들을 필터링
         .entrySet()
         .stream()
         .filter(e -> e.getValue().size() > 1)

// 나머지 그룹들의 합계 출력
         .forEach(e -> {
             System.out.println(
                 "Duplicates found for : "
               + e.getKey()
               + " their sum being : "
               + e.getValue()
                  .stream()
                  .collect(summingInt(i -> i)));
         });
```

이 접근 방식은 그룹화, 필터링, 계산 관심사를 분리함으로써 "무엇을 하는지 훨씬 더 명확하게 전달합니다".

## 트레이드오프

함수형 접근 방식은 잠재적인 성능 비용(박싱 연산, 맵 컬렉션)을 수반하지만, 요구사항에 따라 이러한 비용은 허용될 수 있습니다. 함수형 솔루션은 또한 단일 전체 합계가 아닌 중복 값별로 합계를 계산합니다.

## 결론

Java 8 Stream API를 통한 함수형 프로그래밍의 힘은 SQL과 유사한 선언적 표현력에 접근하는 것에 중점을 둡니다. 개발자들은 더 이상 개별 배열 인덱스를 관리하거나 중간 결과를 버퍼에 계산하고 저장하는 것에 신경 쓰지 않아도 됩니다. 대신, 흥미로운 논리적 질문에 집중할 수 있습니다: "무엇이 중복인가?" 또는 "어떤 합계에 관심이 있는가?"

함수형 접근 방식은 절차적 제어 흐름 조작보다는 가독성과 관심사의 분리를 강조합니다.

그러나 저자는 함수형이 명령형 프로그래밍을 보편적으로 대체하지 않는다고 언급합니다 - 컨텍스트와 팀의 전문성이 중요합니다.

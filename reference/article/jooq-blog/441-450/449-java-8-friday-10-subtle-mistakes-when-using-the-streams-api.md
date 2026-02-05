# Java 8 금요일: Streams API 사용 시 10가지 미묘한 실수

> 원문: https://blog.jooq.org/java-8-friday-10-subtle-mistakes-when-using-the-streams-api/

매주 금요일마다, 우리는 람다 표현식, 확장 메서드, 그리고 기타 훌륭한 기능들을 활용하는 멋진 새 튜토리얼 스타일의 Java 8 기능들을 여러분께 선보이고 있습니다.

우리는 이전에 SQL 실수에 대한 여러 글을 게시했지만, Java 8에 특화된 오류에 대해서는 아직 다루지 않았습니다. 이 글은 그 간극을 메우기 위한 것으로, Java 8의 스트리밍 기능을 도입할 때 개발자들이 마주치게 될 잠재적인 함정들을 살펴봅니다.

## 1. 실수로 스트림 재사용하기

스트림은 전통적인 입력 스트림처럼 한 번만 소비할 수 있습니다. 다음 코드는 작동하지 않을 것입니다:

```java
IntStream stream = IntStream.of(1, 2);
stream.forEach(System.out::println);

// 재미있었어! 한 번 더 해보자!
stream.forEach(System.out::println);
```

다음과 같은 결과를 얻게 됩니다:

```
1
2
java.lang.IllegalStateException:
  stream has already been operated upon or closed
```

스트림을 소비할 때 주의하세요. 스트림은 한 번만 소비할 수 있습니다.

## 2. 실수로 "무한" 스트림 생성하기

무한 스트림은 아주 쉽게 만들 수 있습니다. 다음 코드는 무한히 실행될 것입니다:

```java
// 무한히 실행됨
IntStream.iterate(0, i -> i + 1)
         .forEach(System.out::println);
```

스트림의 핵심은 원한다면 무한할 수 있다는 것입니다. 유일한 문제는, 그것을 원하지 않았을 수도 있다는 것입니다. 그러니 적절한 제한을 적용하도록 하세요:

```java
// 이게 더 낫다
IntStream.iterate(0, i -> i + 1)
         .limit(10)
         .forEach(System.out::println);
```

## 3. 실수로 "미묘한" 무한 스트림 생성하기

이번에는 다른 위험한 시나리오를 강조합니다. 이것은 충분히 강조할 수 없습니다. 다음은 단순히 애플리케이션을 중단시킬 것입니다:

```java
IntStream.iterate(0, i -> ( i + 1 ) % 2)
         .distinct()
         .limit(10)
         .forEach(System.out::println);
```

문제는 `distinct()` 연산이 함수가 단 두 개의 고유한 값(0과 1)만 생성할 것이라는 것을 *알지 못한다*는 것입니다. 이것은 스트림에서 새로운 값을 영원히 소비할 것이고, `limit(10)`에는 절대 도달하지 못할 것입니다.

## 4. 실수로 "미묘한" 병렬 무한 스트림 생성하기

실수 #3에 `.parallel()`을 추가하면 상황이 훨씬 더 악화됩니다:

```java
IntStream.iterate(0, i -> ( i + 1 ) % 2)
         .parallel()
         .distinct()
         .limit(10)
         .forEach(System.out::println);
```

아마 4개의 CPU 코어를 소비하게 될 것이고, 우발적인 무한 스트림 소비로 시스템 전체를 상당히 점유하게 될 것입니다. 결과적으로 컴퓨터를 강제로 재부팅해야 할 수도 있습니다.

## 5. 연산 순서 혼동하기

스트림 연산은 의도한 결과를 얻기 위해 올바른 순서로 수행되어야 합니다. `limit(10)`과 `distinct()`의 순서를 바꿔보겠습니다:

```java
IntStream.iterate(0, i -> ( i + 1 ) % 2)
         .limit(10)
         .distinct()
         .forEach(System.out::println);
```

이것은 이제 다음을 출력합니다:

```
0
1
```

왜냐하면 제한이 중복 제거 전에 발생하기 때문입니다. 먼저 10개의 값(0 1 0 1 0 1 0 1 0 1)을 생성하고, 그 다음 고유한 값만 남겨 0과 1이 됩니다.

SQL Server 2012는 `DISTINCT TOP 10`과 `FETCH NEXT 10 ROWS`를 동등하게 취급하므로, SQL 개발자들은 스트림에서 이러한 순서 차이를 예상하지 못할 수 있습니다.

## 6. 연산 순서 혼동하기 (다시)

이 섹션에서는 MySQL/PostgreSQL의 `LIMIT .. OFFSET` 구문과 SQL:2008 표준의 `OFFSET .. LIMIT` 의미론을 다룹니다. SQL:2008 표준에 따르면 `OFFSET` 절이 *먼저* 적용됩니다.

```java
IntStream.iterate(0, i -> i + 1)
         .limit(10) // LIMIT
         .skip(5)   // OFFSET
         .forEach(System.out::println);
```

위 코드는 다음을 생성합니다:

```
5
6
7
8
9
```

예상했던 10개 요소 결과가 아닙니다! 제한이 먼저 0-9의 값들을 생성하고, 5개를 건너뛰면 처음 5개 요소가 제거됩니다.

*`LIMIT .. OFFSET` 대 `OFFSET .. LIMIT` 함정을 조심하세요!*

## 7. 필터를 사용한 파일 시스템 탐색

우리는 이전에 이 문제에 대해 블로그에 글을 쓴 적이 있습니다. `Files.walk()`를 사용하여 숨겨진 디렉토리를 필터링하는 예제입니다:

```java
Files.walk(Paths.get("."))
     .filter(p -> !p.toFile().getName().startsWith("."))
     .forEach(System.out::println);
```

문제는 `.git`과 같은 경로를 필터링하여 제거하지만, `.git\refs`와 같은 숨겨진 폴더 내의 하위 디렉토리는 여전히 나타난다는 것입니다. `walk()`가 비록 지연 방식이지만 "하위 디렉토리의 전체 스트림"을 생성하기 때문입니다. 필터는 직접적인 경로 이름만 확인하고, 부모 디렉토리는 확인하지 않습니다.

파일 구분자를 포함하는 필터 검사를 사용하는 대안적 접근 방식에 주의하세요:

```java
Files.walk(Paths.get("."))
     .filter(p -> !p.toString().contains(File.separator + "."))
     .forEach(System.out::println);
```

이것은 여전히 "전체 디렉토리 하위 트리를 탐색하며, '숨겨진' 디렉토리의 모든 하위 디렉토리로 재귀합니다." 대신 함수형 인터페이스와 함께 이전의 `File.list()` API를 사용하는 것을 제안합니다.

## 8. 스트림의 배킹 컬렉션 수정하기

스트림을 통해 반복하면서 리스트를 수정하면 예측할 수 없는 동작이 발생합니다. 0-9의 리스트를 만들고 `peek(list::remove)`를 사용하여 스트림 소비 중에 각 요소를 제거하려고 시도하면 출력이 손상되고 null 값이 나타나며 `ConcurrentModificationException`이 발생합니다.

```java
List<Integer> list =
IntStream.range(0, 10)
         .boxed()
         .collect(toCollection(ArrayList::new));

list.stream()
    .peek(list::remove)
    .forEach(System.out::println);
```

제거 전에 `sorted()`를 적용하면 모든 요소가 올바르게 소비되고 이후 리스트가 비게 됩니다:

```java
list.stream()
    .sorted()
    .peek(list::remove)
    .forEach(System.out::println);
```

하지만 이 연산에 `parallel()`을 추가하면 다른 결과가 발생합니다 - 일부 요소가 제거되지 않은 채로 남아, 동시 수정이 정렬된 스트림도 망가뜨린다는 것을 보여줍니다:

```java
list.stream()
    .sorted()
    .parallel()
    .peek(list::remove)
    .forEach(System.out::println);
```

`ArrayList.ArrayListSpliterator`의 긴 주석에서는 ArrayList가 탐색 중 구조적 간섭을 "최선의 노력" 기반으로 `modCounts`를 사용하여 감지한다고 설명합니다.

핵심 권장 사항은: 스트림을 소비하는 동안 배킹 컬렉션을 실제로 수정하지 마세요. 그것은 그냥 작동하지 않습니다.

## 9. 실제로 스트림을 소비하는 것을 잊어버리기

터미널 연산이 없으면 "스트림은 그냥 거기 있고, 소비되지 않습니다". 다음 예제는 출력을 기대하며 `peek(System.out::println)`을 사용하지만, 스트림에 `forEach()`와 같은 터미널 연산이 없기 때문에 아무것도 출력되지 않습니다:

```java
IntStream.range(1, 5)
         .peek(System.out::println)
         .peek(i -> {
              if (i == 5)
                  throw new RuntimeException("bang");
          });
```

`peek()`은 `forEach()`와 아주 비슷하기 때문에 이러한 혼동은 이해할 수 있습니다.

jOOQ 쿼리에서 `execute()`를 잊어버리는 것도 같은 문제를 일으키는 병렬적인 예제입니다 - 터미널 연산 호출 없이는 update 문이 실행되지 않습니다:

```java
DSL.using(configuration)
   .update(TABLE)
   .set(TABLE.COL1, 1)
   .set(TABLE.COL2, "abc")
   .where(TABLE.ID.eq(3));
```

## 10. 병렬 스트림 데드락

이것은 "Java에서 데드락의 전형적인 예제"입니다. 여러 잠금에 대해 중첩 방식으로 동기화된 블록을 사용하는 병렬 스트림입니다:

```java
Object[] locks = { new Object(), new Object() };

IntStream
    .range(1, 5)
    .parallel()
    .peek(Unchecked.intConsumer(i -> {
        synchronized (locks[i % locks.length]) {
            Thread.sleep(100);

            synchronized (locks[(i + 1) % locks.length]) {
                Thread.sleep(50);
            }
        }
    }))
    .forEach(System.out::println);
```

이 코드는 스레드들이 충돌하는 순서로 잠금을 획득하면서 슬립하여 데드락을 생성하고, "스레드들이 영원히 블록될 것"을 보장합니다.

이것은 "직접적인 데드락을 대규모로 생성하는 방법"을 나타내며, 동시성 시스템의 복잡성에 대한 추가 세부 사항은 "이 질문에 대한 Brian Goetz의 Stack Overflow 답변"을 참조하세요.

## 결론

"스트림과 함수적 사고를 통해, 우리는 수많은 새롭고 미묘한 버그에 직면하게 될 것입니다." 이러한 버그들 대부분은 "연습과 집중을 유지하는 것" 외에는 예방할 수 없습니다.

핵심 교훈은 개발자들이 연산 순서와 스트림이 의도치 않게 무한해질 수 있는지에 대해 신중하게 생각해야 한다는 것입니다. "스트림(과 람다)은 매우 강력한 도구입니다. 하지만 먼저 익숙해져야 하는 도구입니다."

블로그의 Java 8 콘텐츠에 계속 관심을 가져주세요. 이것은 최종 가이드가 아니라 지속적인 교육 여정을 나타냅니다.

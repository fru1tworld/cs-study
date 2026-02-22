# Java에서 파일을 패턴 매칭하고 인접 줄을 표시하는 방법

> 원문: https://blog.jooq.org/how-to-pattern-match-files-and-display-adjacent-lines-in-java/

최근 우리는 jOOλ 0.9.9의 멋진 윈도우 함수 지원에 대한 글을 게시했는데, 이것이 우리가 라이브러리에 추가한 것 중 가장 훌륭한 것 중 하나라고 생각합니다. 오늘은 다음 Stack Overflow 질문에서 영감을 받은 사용 사례에서 윈도우 함수의 멋진 활용법을 살펴보겠습니다:

Sean Nguyen의 질문: [Java 8 스트림을 사용하여 grep처럼 매칭 전후의 줄을 가져오는 방법](https://stackoverflow.com/q/34920539/521799)

> 텍스트 파일에 많은 문자열 줄이 있습니다. grep에서 매칭 전후의 줄을 찾고 싶다면 다음과 같이 합니다:
>
> `grep -A 10 -B 10 "ABC" myfile.txt`
>
> Java 8에서 스트림을 사용하여 이와 동등한 것을 어떻게 구현할 수 있을까요?

## Java 8에서 스트림을 사용하여 이와 동등한 것을 어떻게 구현할 수 있을까요?

jOOλ 0.9.9를 사용하면 매우 쉽습니다. 다음 작은 코드 조각을 고려해보세요:

```java
Seq.seq(Files.readAllLines(Paths.get(
        new File("/path/to/Example.java").toURI())))
   .window()
   .filter(w -> w.value().contains("ABC"))
   .forEach(w -> {
       System.out.println("-1:" + w.lag().orElse(""));  // 이전 줄
       System.out.println(" 0:" + w.value());           // 현재 줄
       System.out.println("+1:" + w.lead().orElse(""));  // 다음 줄
   });
```

그래서, 우리가 여기서 무엇을 하고 있을까요?

`Files.readAllLines`는 우리 파일의 모든 줄 목록을 반환합니다. 그런 다음 `Seq.seq()`로 감싸서 스트림 위에 jOOλ의 `Seq` 래퍼를 생성합니다.

`Seq.window()`는 정렬 방식 없이, 그리고 프레임 없이 스트림의 모든 개별 값에 대해 "윈도우"를 생성합니다. 윈도우 `w`는 `w.value()`를 통해 기본 값에 대한 접근을 제공합니다. 그러나 "윈도우"의 다른 값에도 접근할 수 있습니다.

`Window.lag()`는 `w.value()` 앞의 값을 반환합니다. `Window.lead()`는 `w.value()` 뒤의 값을 반환합니다. 이것은 SQL의 `LAG()` 및 `LEAD()` 함수와 정확히 동일하게 작동합니다. 선택적으로 `lag()` 및 `lead()`에 오프셋을 전달할 수 있습니다.

이것이 전부입니다!

## 조금 더 나아가기

질문자는 정확히 10줄 앞과 뒤의 줄을 원했습니다. 다음과 같이 할 수 있습니다:

```java
int lower = -5;
int upper = 5;

Seq.seq(Files.readAllLines(Paths.get(
        new File("/path/to/Example.java").toURI())))
   .window(lower, upper)
   .filter(w -> w.value().contains("ABC"))
   .forEach(w -> {
       System.out.println();
       System.out.println("---------------------------");

       // 윈도우 내의 모든 줄 출력
       w.window().forEach(System.out::println);
   });
```

이 예제에서 우리는 각 줄 주변의 5줄 앞과 5줄 뒤를 포함하는 프레임을 지정했습니다. 그런 다음 `w.window()`를 사용하여 해당 윈도우 프레임 내의 모든 줄에 접근할 수 있습니다.

## 결론

윈도우 함수는 매우 강력합니다. 우리의 이전 jOOλ 윈도우 함수 지원에 대한 글에 대한 reddit 토론에서 다른 언어들도 비슷한 기능을 구축하기 위한 프리미티브를 지원한다는 것을 보여주었습니다. 하지만 보통 이러한 빌딩 블록들은 SQL에서 영감을 받은 jOOλ에서 노출된 것만큼 간결하지 않습니다.

jOOλ의 윈도우 함수를 사용하면 메모리 내 데이터 스트림에 대한 강력한 연산을 구성할 수 있으며, 전통적인 명령형 접근 방식에 비해 최소한의 코드 장황함으로 이를 달성할 수 있습니다.

# Java 8 스트림으로 리스트의 요소를 빠르게 교체하는 방법

> 원문: https://blog.jooq.org/how-to-use-java-8-streams-to-swiftly-replace-elements-in-a-list/

저는 최근에 이 흥미로운 Stack Overflow 질문을 보았습니다:

[Java 8 Stream에서 요소를 교체하는 Swift한 방법이 있나요?](http://stackoverflow.com/q/29395730/521799)

기본적으로, 질문자는 다음과 같은 책 목록을 가지고 있었습니다:

```java
List<String> books = Arrays.asList(
    "The Holy Cow: The Bovine Testament",
    "True Hip Hop",
    "Truth and Existence",
    "The Big Book of Green Design"
);
```

(이 책 제목들은 아래 사이트에서 생성된 것입니다. 만약 당신의 책이라면, 축하드립니다! 언급되셔서 기쁘시길 바랍니다 :) )

http://www.piggybackjournals.co.uk/resources/generators/random-book-title-generator.php

이제 그들은 인덱스 2의 세 번째 책을 "Pregnancy For Dummies"로 교체하고 싶어합니다:

```java
List<String> books = Arrays.asList(
    "The Holy Cow: The Bovine Testament",
    "True Hip Hop",
    "Pregnancy For Dummies",
    "The Big Book of Green Design"
);
```

## 이것이 어떻게 수행될 수 있을까요?

### 고전적인 접근 방식 #1 - 변형(Mutation)

물론, 원본 리스트를 직접 변형할 수 있습니다:

```java
books.set(2, "Pregnancy For Dummies");
```

또는...

### 고전적인 접근 방식 #2 - 복사와 변형

... 복사본을 만든 다음 복사본을 변형할 수 있습니다:

```java
List<String> copy = new ArrayList<>(books);
copy.set(2, "Pregnancy For Dummies");
```

### 함수형 접근 방식 #1 - jOOλ 사용

jOOλ을 사용하면 실용적인 한 줄짜리 코드를 작성할 수 있습니다:

```java
seq(books)
    .zipWithIndex()
    .map(t -> t.v2 == 2
            ? "Pregnancy For Dummies"
            : t.v1)
    .toList();
```

위 코드의 아이디어는 모든 값에 인덱스를 할당한 다음(zipWithIndex), 해당 인덱스로 각 값을 새 값으로 매핑하거나 단순히 이전 값을 유지하는 것입니다.

### 함수형 접근 방식 #2 - 표준 JDK API 사용

표준 JDK API만 사용하면 다음과 같이 작성할 수 있습니다:

```java
Stream.concat(
    Stream.concat(
        books.stream().limit(2),
        Stream.of("Pregnancy For Dummies")
    ),
    books.stream().skip(3)
).collect(Collectors.toList());
```

안타깝게도, 위 코드는 리스트의 첫 번째 부분을 두 번 순회하게 됩니다. 이것은 마치 [SQL의 OFFSET 페이지네이션의 비효율성](https://blog.jooq.org/2013/10/26/faster-sql-paging-with-jooq-using-the-seek-method/)과 같습니다.

## Swift할까요, 아닐까요?

확실히, JDK API는 간결한 함수형 로직을 작성하는 데 도움이 되지 않습니다. 이 경우에는 그냥 명령형으로 작성하거나 [jOOλ](https://github.com/jooq/jool), [vavr](http://www.vavr.io/), 또는 [Functional Java](http://www.functionaljava.org/)와 같은 보조 라이브러리를 사용하는 것이 좋습니다.

우리는 이전에 이미 이에 대해 블로그 글을 작성한 바 있습니다:

[Java 8 Streams API가 충분하지 않을 때](https://blog.jooq.org/2015/02/13/when-the-java-8-streams-api-is-not-enough/)

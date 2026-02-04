# JDK 8: 컬렉션의 현재 상태

> 원문: https://blog.jooq.org/jdk-8-state-of-the-collections/

Brian Goetz는 JSR 335(Project Lambda)의 Oracle 프로젝트 리드입니다. 그는 JDK 8의 컬렉션에 적용될 새로운 기능들에 대한 문서를 게시했습니다.

다음은 그가 제시한 흥미로운 코드 예제들입니다:

```java
List<String> strings = ...
int sumOfLengths = strings.stream()
                          .map(String::length)
                          .reduce(0, Integer::plus);
```

```java
int sum = shapes.stream()
                .filter(s -> s.getColor() == BLUE)
                .map(s -> s.getWeight())
                .sum();
```

여기에서 전체 게시물을 확인하세요:
http://cr.openjdk.java.net/~briangoetz/lambda/sotc3.html

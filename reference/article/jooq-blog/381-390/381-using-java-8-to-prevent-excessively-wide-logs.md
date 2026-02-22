# Java 8로 과도하게 넓은 로그를 방지하기

> 원문: https://blog.jooq.org/using-java-8-to-prevent-excessively-wide-logs/

게시일: 2015년 1월 19일
작성자: Lukas Eder
카테고리: Java, SQL and jOOQ

---

일부 로그는 기계가 읽을 수 있어야 하고 영구적으로 보존되어야 합니다. 다른 로그들은 디버깅 목적으로 사람이 소비하기 위한 것입니다. 사람이 보는 로그의 경우, 과도한 너비는 문제를 일으킵니다. 많은 도구들(Eclipse를 포함하여, 참조된 버그에 따르면)이 특정 문자 수 제한을 초과하는 줄을 처리하는 데 어려움을 겪기 때문입니다.

## 문제 해결

Java 8이 함수형 프로그래밍 기법을 통해 문자열 조작을 얼마나 단순화하는지 보여드리겠습니다. 두 가지 구현을 제공합니다:

### jOOλ와 Apache Commons Lang 사용

```java
public String truncate(String string) {
    return truncate(string, 80);
}

public String truncate(String string, int length) {
    return Seq.of(string.split("\\n"))
              .map(s -> StringUtils.abbreviate(s, 400))
              .join("\\n");
}
```

### 순수 Java 8 사용

```java
public String truncate(String string) {
    return truncate(string, 80);
}

public String truncate(String string, int length) {
    return Stream.of(string.split("\\n"))
                 .map(s -> s.substring(0, Math.min(s.length(), length)))
                 .collect(Collectors.joining("\\n"));
}
```

## 예제 데모

입력으로 여러 줄에 걸친 Lorem ipsum 텍스트가 표시됩니다. 10자로 잘라내면, 출력은 각 줄이 생략 부호 표시와 함께 축약된 것을 보여줍니다.

## 관련 도구

- jOOλ 0.9.4: Java를 위한 함수형 라이브러리
- Apache Commons Lang: `StringUtils.abbreviate()`를 제공하는 유틸리티 라이브러리

## 토론

한 댓글 작성자가 log4j와 logback이 너비 수정자(`%-20.80m`)를 제공한다고 언급했지만, 저자는 그러한 수정자가 여러 줄 메시지 내에서의 잘라내기를 처리하는지 의문을 제기했습니다.

---

Happy logging!

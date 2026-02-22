# jOOλ이나 jOOQ로 Java 8 SQL 한 줄 코드를 놓치지 마라

> 원문: https://blog.jooq.org/dont-miss-out-on-writing-java-8-sql-one-liners-with-joo%ce%bb-or-jooq/

게시일: 2014년 10월 2일, lukaseder

## 서론

점점 더 많은 개발자들이 비즈니스 애플리케이션에 Java 8의 함수형 프로그래밍 기능을 도입하고 있습니다. Data Geekery에서 우리 팀은 jOOQ 통합 테스트에 Java 8을 사용하고 있으며, Streams API와 람다 표현식을 사용하면 테스트 데이터 생성이 상당히 간소화된다는 것을 발견했습니다. 하지만 우리는 JDK가 더 많은 것을 제공할 수 있다고 느꼈고, 이로 인해 jOOλ을 만들어 오픈소스로 공개하게 되었습니다. jOOλ은 이러한 부족한 부분들을 해결하는 유틸리티 라이브러리입니다. 우리는 functionaljava와 같은 정교한 라이브러리를 대체하려는 것이 아닙니다. 오히려 jOOλ은 특정 단점들을 보완합니다.

## 문제: ResultSet 데이터 스트리밍

Stack Overflow의 한 질문이 JDBC ResultSet의 모든 컬럼을 단일 리스트로 스트리밍하는 방법에 대한 탐구를 촉발시켰습니다.

입력 테이블 예시:
```
+----+------------+------------+
| ID | FIRST_NAME | LAST_NAME  |
+----+------------+------------+
|  1 | Joslyn     | Vanderford |
|  2 | Rudolf     | Hux        |
+----+------------+------------+
```

기대 출력:
```
1
Joslyn
Vanderford
2
Rudolf
Hux
```

## 전통적인 반복 접근법

```java
ResultSet rs = ...;
ResultSetMetaData meta = rs.getMetaData();

List<Object> list = new ArrayList<>();

while (rs.next()) {
    for (int i = 0; i < meta.getColumnCount(); i++) {
        list.add(rs.getObject(i + 1));
    }
}
```

함수형 프로그래밍으로도 이 작업을 수행할 수 있지만, 반복적인 솔루션이 반드시 문제가 있는 것은 아닙니다.

## jOOλ을 사용한 솔루션

jOOλ은 우리 팀이 확인한 여러 제한 사항들을 해결합니다:

- JDBC는 간단한 `ResultSet`을 `Stream`으로 변환하는 기능이 부족합니다
- 함수형 인터페이스는 checked 예외를 던질 수 없습니다. 람다 내부의 try-catch 블록은 보기 좋지 않습니다
- JDK에는 `Iterator`나 `Spliterator`를 구현하지 않고 유한한 스트림을 생성하는 간단한 방법이 없습니다

함수형 구현:

```java
ResultSet rs = ...;
ResultSetMetaData meta = rs.getMetaData();

List<Object> list =
Seq.generate()
   .limitWhile(Unchecked.predicate(v -> rs.next()))
   .flatMap(Unchecked.function(v -> IntStream
       .range(0, meta.getColumnCount())
       .mapToObj(Unchecked.intFunction(i ->
           rs.getObject(i + 1)
       ))
   ))
   .toList()
```

사용된 주요 jOOλ 기능들:

- `Seq.generate()`는 내용이 지정되지 않은 무한 스트림을 생성합니다
- `.limitWhile()`은 조건자(predicate) 조건에 따라 종료합니다 (표준 JDK에서는 사용할 수 없음). `Unchecked`를 통해 checked 예외를 RuntimeException으로 래핑합니다
- `flatMap()`은 데이터베이스 행에서 컬럼 값으로의 중첩된 스트림 변환을 처리합니다
- `.toList()`는 표준 JDK 컬렉션 연산보다 더 편리합니다

## jOOQ를 사용한 솔루션

jOOQ는 SQL 결과 레코드를 다루는 데 추가적인 편의성을 제공합니다:

```java
ResultSet rs = ...;

List<Object> list =
DSL.using(connection)
   .fetch(rs)
   .stream()
   .flatMap(r -> Arrays.stream(r.intoArray()))
   .collect(Collectors.toList());
```

이 접근법은 jOOλ 없이 표준 JDK API만 사용합니다.

jOOQ와 jOOλ을 결합한 접근법:

```java
ResultSet rs = ...;

List<Object> list =
Seq.seq(DSL.using(connection).fetch(rs))
   .flatMap(r -> Arrays.stream(r.intoArray()))
   .toList();
```

## 이것이 수행하는 작업

이 예제는:
- JDBC ResultSet을 Java Collection으로 가져옵니다
- 각 레코드를 컬럼 값의 배열로 변환합니다
- 각 배열을 스트림으로 변환합니다
- 여러 스트림을 단일 스트림으로 평탄화합니다
- 모든 값을 하나의 리스트로 수집합니다

## 결론

이 글은 Java 8의 함수형 접근법이 엔터프라이즈 개발자들에게 점차 자연스럽게 느껴질 것임을 강조합니다. 구성 가능한 데이터 소스와 지연 평가되는 람다 표현식을 사용한 파이프라인된 변환의 개념은 매력적입니다. jOOQ는 SQL 데이터 소스를 매우 유창하고 직관적으로 캡슐화하면서 기본적으로 Streams API를 통해 변환할 수 있는 표준 JDK 컬렉션을 생성합니다. 우리는 이것이 Java 생태계가 데이터 변환에 접근하는 방식을 크게 바꿀 것이라고 믿습니다.

# Java에서 3상태 불리언

> 원문: https://blog.jooq.org/three-state-booleans-in-java/

이 블로그 글에서는 SQL의 3값 불리언 로직(TRUE, FALSE, UNKNOWN/NULL)이 Java 프로그래밍에서 때때로 유용할 수 있다는 점에 대해 논의합니다. 저자는 Java 8을 위한 SQL 스트림 라이브러리인 jOOλ에서의 실제 예제를 통해 이를 설명합니다.

## 문제: ResultSetIterator 구현

JDBC ResultSet으로부터 Java 8 Stream을 구현할 때, 개발자는 Iterator를 구성해야 합니다. 문제는 Iterator 인터페이스가 `hasNext()`와 `next()` 메서드 구현을 요구하지만, `hasNext()`가 `next()` 이전에 호출된다는 것을 보장하는 내용이 계약에 없다는 점에서 발생합니다. 개발자들이 `hasNext()`를 여러 번 호출할 수 있으므로, 구현은 결과를 효율적으로 캐시해야 합니다.

단순한 접근 방식이 실패하는 이유:
- `ResultSet.isLast()` 사용은 비용이 많이 들고 빈 ResultSet에서는 작동하지 않습니다
- 많은 JDBC 드라이버가 TYPE_FORWARD_ONLY ResultSet에 대해 `isLast()`를 지원하지 않습니다
- `ResultSet.next()`를 여러 번 호출하면 커서가 잘못 전진합니다

## 해결책: 3값 불리언 상태

저자는 세 가지 상태를 추적하기 위해 `java.lang.Boolean`(래퍼 클래스)을 사용하는 방법을 시연합니다:
- `null`: 다음 행이 존재하는지 알 수 없음
- `true`: 다음 행이 존재하고 미리 가져옴
- `false`: 더 이상 행이 없음

다음은 jOOλ에서의 구현 패턴입니다:

```java
class ResultSetIterator<T> implements Iterator<T> {
    /
     * 기저 ResultSet에 다음 행이 있는지 여부.
     * null:  다음 행이 존재하는지 알 수 없음
     * true:  다음 행이 존재하고, 미리 가져옴
     * false: 다음 행 없음
     */
    Boolean hasNext;
    ResultSet rs;

    @Override
    public boolean hasNext() {
        try {
            if (hasNext == null) {
                hasNext = rs().next();
            }
            return hasNext;
        }
        catch (SQLException e) {
            translator.accept(e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public T next() {
        try {
            if (hasNext == null) {
                rs().next();
            }
            return rowFunction.apply(rs());
        }
        catch (SQLException e) {
            translator.accept(e);
            throw new IllegalStateException(e);
        }
        finally {
            hasNext = null;
        }
    }
}
```

`hasNext()` 메서드는 `null`일 때만 결과를 캐시하므로, `next()`가 상태를 재설정할 때까지 반복 호출은 아무 효과가 없습니다.

## 가독성 우려

비평가들은 두 개의 별도 불리언 변수를 사용하는 것에 비해 가독성이 떨어진다고 주장할 수 있습니다. 그러나 저자가 언급했듯이: "당신은 여전히 3값 불리언 상태를 구현하고 있지만, 두 변수에 분산되어 있고, 이를 정말로 더 읽기 쉽게 이름 짓기가 매우 어렵습니다." 두 개의 불리언은 실제로 네 가지 가능한 상태를 만들어, 버그 위험을 증가시킵니다.

## 중요한 주의사항

저자는 강조합니다: "`null`을 상태를 모델링하는 수단으로 명시적으로 사용하는 것은 이 모델이 캡슐화되어 있기 때문에 괜찮습니다." 그러나 개발자는 가능할 때마다 공개 API 메서드에서 `null`을 반환하는 것을 피해야 합니다. 3값 불리언은 내부 구현 세부 사항이 숨겨져 있을 때 가장 잘 작동합니다.

이 글은 유머러스하게 마무리됩니다: "어떤 접근 방식이 최선일까요? `TRUE`나 `FALSE` 답은 없고, 오직 `UNKNOWN`만 있습니다."

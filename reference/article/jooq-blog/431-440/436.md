# DRY 유지하기: 메서드 오버로딩

> 원문: https://blog.jooq.org/keeping-things-dry-method-overloading/

좋은 깔끔한 애플리케이션 설계는 DRY를 유지하기 위한 규율이 필요합니다:

> "모든 것은 한 번만 수행되어야 합니다. 두 번 수행해야 한다면 그것은 우연입니다. 세 번 수행해야 한다면 그것은 패턴입니다." — 이름 모를 현자

익스트림 프로그래밍 규칙을 따른다면, 패턴을 발견했을 때 "가차 없이 리팩토링"해야 합니다.

## DRY하지 않음: 메서드 오버로딩

가장 덜 DRY한 관행 중 하나는—그래도 여전히 수용 가능하지만—메서드 오버로딩을 지원하는 언어에서의 메서드 오버로딩입니다(Ceylon이나 JavaScript와 달리). jOOQ는 내부 도메인 특화 언어이기 때문에 오버로딩을 많이 사용합니다. 데이터베이스 컬럼을 모델링하는 Field 타입을 살펴보겠습니다:

```java
public interface Field<T> {
    // [...]
    Condition eq(T value);
    Condition eq(Field<T> field);
    Condition eq(Select<? extends Record1<T>> query);
    Condition eq(QuantifiedSelect<? extends Record1<T>> query);

    Condition in(Collection<?> values);
    Condition in(T... values);
    Condition in(Field<?>... values);
    Condition in(Select<? extends Record1<T>> query);
    // [...]
}
```

특정 비DRY 구현은 불가피하지만, 핵심 원칙은 하나의 메서드에서 다른 메서드를 호출함으로써 오버로딩된 메서드들의 구현을 최소한으로 유지하는 것입니다. `eq(T value)`와 `eq(Field<T> field)` 메서드는 매우 유사합니다:

```java
@Override
public final Condition eq(T value) {
    return equal(value);
}

@Override
public final Condition equal(T value) {
    return equal(Utils.field(value, this));
}

@Override
public final Condition equal(Field<T> field) {
    return compare(EQUALS, nullSafe(field));
}

@Override
public final Condition compare(Comparator comparator, Field<T> field) {
    switch (comparator) {
        case IS_DISTINCT_FROM:
        case IS_NOT_DISTINCT_FROM:
            return new IsDistinctFrom<T>(this, nullSafe(field), comparator);

        default:
            return new CompareCondition(this, nullSafe(field), comparator);
    }
}
```

구조를 보면:

- `eq()`는 레거시 `equal()` 메서드의 동의어입니다
- `equal(T)`는 `equal(Field<T>)`를 특수화합니다
- `equal(Field<T>)`는 `compare(Comparator, Field<T>)`를 특수화합니다
- `compare()`가 실제 구현을 제공합니다

## 왜 이렇게 수고를 들일까요?

답은 간단합니다:

- API 전반에 걸쳐 복사-붙여넣기 오류의 가능성이 최소화됩니다
- 동일한 패턴이 `ne`, `gt`, `ge`, `lt`, `le`에도 제공되어야 합니다
- 어떤 부분이 통합 테스트되든, 구현이 확실히 커버됩니다
- 사용자는 범용 메서드의 세부 사항을 기억할 필요 없이 편리한 메서드가 포함된 풍부한 API를 받습니다

JDK가 항상 이 원칙을 따르지는 않습니다. Iterator에서 Java 8 Stream을 생성하려면 다음이 필요합니다:

```java
StreamSupport.stream(iterator.spliterator(), false);
```

직관적으로 사용자들은 다음을 선호할 것입니다:

```java
iterator.stream();
```

Brian Goetz의 Stack Overflow 설명을 참조하면, 미묘한 구현 세부 사항이 클라이언트 코드로 누출되어 개발자들이 반복적으로 래퍼 함수를 만들게 됩니다.

반면에, 이러한 API 설계는 더 많은 구현 작업이 필요하고 더 긴 스택 트레이스를 생성합니다. 그러나, 깊은 스택 트레이스가 좋은 코드 품질을 나타낼 수 있다는 것을 이전에 보여드린 바 있습니다.

## 핵심 교훈

패턴을 발견했을 때, 공통 분모를 찾아 구현으로 분리하고 단계별로 책임을 위임하여 직접 호출되는 일이 거의 없도록 리팩토링하세요. 이 접근 방식은 다음을 제공합니다:

- 더 적은 버그
- 더 편리한 API

즐거운 리팩토링 되세요!

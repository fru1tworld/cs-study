# Java에서 쉬운 성능 최적화 Top 10

> 원문: https://blog.jooq.org/top-10-easy-performance-optimisations-in-java/

"웹 스케일"이라는 버즈워드에 대한 과대광고가 많이 있었고, 사람들은 자신의 시스템을 "스케일"하기 위해 애플리케이션 아키텍처를 재구성하는 데 많은 노력을 기울이고 있습니다. 하지만 스케일링이란 정확히 무엇일까요?

## 스케일링의 다양한 측면

스케일링을 크게 두 가지 관점에서 바라볼 수 있습니다:

### 부하 스케일링 (스케일 아웃)

1명의 사용자에게 잘 작동하는 시스템이 10명, 100명, 또는 수백만 명의 사용자에게도 잘 작동하도록 만드는 것입니다. 이 경우 50-100ms의 개별 요청 지연 시간은 허용 가능합니다. 이것은 일반적으로 상태를 유지하지 않는(stateless) 아키텍처를 여러 머신에 분산시키는 것을 포함합니다.

### 성능 스케일링 (스케일 업)

알고리즘이 대규모 데이터셋을 효율적으로 처리하도록 하는 것입니다. 이 경우 지연 시간이 매우 중요해집니다. 모든 계산을 단일 머신에서 유지하기 위해 가능한 모든 것을 하고 싶을 것입니다.

## Big O 표기법의 현실

Java 7의 ForkJoinPool과 Java 8의 스트림이 병렬 처리를 가능하게 하지만, 병렬 처리는 알고리즘의 Big O 표기법에 영향을 미치지 않습니다. 만약 당신의 알고리즘이 O(n log n)이고, 그 알고리즘을 c개의 코어에서 실행한다면, 여전히 O(n log n / c) 알고리즘을 갖게 됩니다.

병렬 처리는 벽시계 시간(wall-clock time)에는 도움이 되지만 알고리즘적 복잡도를 줄이지는 않습니다.

## N.O.P.E. 분기 개념

O(N^3) 복잡도를 가진 알고리즘의 시각적 흐름도를 생각해 봅시다: 왼쪽 분기에는 N->M->무거운 연산이 있고, 오른쪽 분기에는 N->O->P->쉬운 연산(또는 "N.O.P.E.")이 있습니다.

핵심 통찰은 이것입니다: 개발 환경에서의 프로파일링은 하나의 병목 현상을 식별할 수 있지만, 프로덕션 데이터는 다른 비용이 많이 드는 경로를 드러낼 수 있습니다. 왜냐하면 프로덕션 데이터셋이 더 크기 때문입니다.

근본적인 원리: "리프 노드에서 낭비되는 모든 CPU 사이클은 N x O x P 번 낭비됩니다."

## 이러한 최적화가 적용되는 경우

중요한 점은 이것입니다: 이 글을 피할 수 없는 O(N^3) 알고리즘의 리프 노드에 문제가 있는 맥락에서 읽어주세요. 이러한 최적화는 스케일 확장에 도움이 되지 않습니다. 고객의 하루를 구해주고, 전체 알고리즘의 어려운 개선을 나중으로 미루는 데 도움이 됩니다!

## 프로파일링 지침

최적화를 위한 두 가지 핵심 원칙이 있습니다:

- 잘 설계된 애플리케이션은 최적화하기 더 쉽습니다
- "조기 최적화는 성능 문제를 해결하지 못하지만, 애플리케이션의 설계를 나쁘게 만들어서 최적화하기 더 어렵게 만듭니다"

---

## 1. StringBuilder 사용하기

`+` 연산자보다 항상 StringBuilder를 사용하세요. 특히 문자열이 여러 코드 경로에서 조건부로 구축될 때 그렇습니다.

`+` 연산자가 StringBuilder 바이트코드로 컴파일되지만, 각 연결은 새로운 StringBuilder 인스턴스를 생성합니다. 문자열이 (if 문을 통해) 조건부로 구축될 때, 분기들 사이에서 단일 영속적인 StringBuilder를 사용하면 낭비적인 가비지 컬렉션 사이클을 방지합니다.

핵심 통찰: "GC나 StringBuilder의 기본 용량 할당 같은 어리석은 것에 낭비하는 모든 CPU 사이클은, 고처리량 코드에서 N x O x P 번 낭비됩니다."

```java
// 덜 효율적 - 여러 StringBuilder를 생성
String x = "a" + args.length + "b";
if (args.length == 1)
    x = x + args[0];

// 더 나은 접근법 - 단일 StringBuilder 인스턴스
StringBuilder x = new StringBuilder("a");
x.append(args.length);
x.append("b");
if (args.length == 1)
    x.append(args[0]);
```

## 2. 정규 표현식 피하기

정규 표현식은 유용하지만, 성능에 민감한 섹션에서는 비용이 듭니다. 반복적으로 재컴파일하는 대신 컴파일된 `Pattern` 객체를 캐시하는 것이 좋습니다.

점으로 IP 주소를 분리하는 것과 같은 간단한 연산의 경우, 인덱스 조작을 사용한 수동 문자 기반 파싱이 더 좋은 성능을 보이지만, 가독성은 떨어집니다.

```java
// 비효율적인 정규 표현식 사용
String[] parts = ipAddress.split("\\.");

// 수동 파싱 대안
int length = ipAddress.length();
int offset = 0;
int part = 0;
for (int i = 0; i < length; i++) {
    if (i == length - 1 ||
            ipAddress.charAt(i + 1) == '.') {
        parts[part] =
            ipAddress.substring(offset, i + 1);
        part++;
        offset = i + 2;
    }
}
```

## 3. iterator() 사용하지 않기

`Iterator`를 생성하면 각 루프마다 세 개의 정수 필드를 가진 새로운 힙 객체가 할당됩니다. `.get(i)`를 사용하는 인덱스 기반 for 루프는 스택 공간만 소비합니다.

"이 반복을 매우 많이 실행한다면, 인덱스 기반 반복을 통해 이 쓸모없는 인스턴스 생성을 피해야 합니다."

```java
// 매 반복마다 Iterator 인스턴스 생성
for (String value : strings) {
    // 여기서 유용한 작업 수행
}

// 인덱스 기반 - 스택 공간만 사용
int size = strings.size();
for (int i = 0; i < size; i++) {
    String value = strings.get(i);
    // 여기서 유용한 작업 수행
}

// 배열 반복 - 가장 효율적
for (String value : stringArray) {
    // 여기서 유용한 작업 수행
}
```

## 4. 그 메서드를 호출하지 마세요

타이트한 루프에서 비용이 많이 드는 연산의 결과를 캐시하고, 반복적으로 호출하는 대신 캐시된 값을 사용하세요.

예를 들어 JDBC의 `ResultSet.wasNull()`은 비용이 많이 드는 계산을 수행할 수 있습니다. `getInt()`의 계약이 NULL 값에 대해 0을 보장하므로, `value == null && rs.wasNull()`을 확인하면 조건부로만 비용이 많이 드는 메서드를 호출하여 불필요한 오버헤드를 줄입니다.

```java
// 매 반복마다 wasNull() 호출
if (type == Integer.class) {
    result = (T) wasNull(rs,
        Integer.valueOf(rs.getInt(index)));
}

static final <T> T wasNull(ResultSet rs, T value)
throws SQLException {
    return rs.wasNull() ? null : value;
}

// 최적화됨 - null 체크를 캐시
static final <T extends Number> T wasNull(
    ResultSet rs, T value
)
throws SQLException {
    return (value == null ||
           (value.intValue() == 0 && rs.wasNull()))
        ? null : value;
}
```

## 5. 기본형과 스택 사용하기

N.O.P.E. 분기 깊숙이 있을 때, 래퍼 타입 사용에 극도로 주의해야 합니다. 많은 가비지를 생성하여 GC가 지속적으로 정리해야 할 가능성이 있습니다.

래퍼 타입은 힙 할당과 가비지 압력을 강제합니다. 래퍼 배열(`Integer[]`) 대신 기본형 배열(`int[]`)을 사용하면 객체 할당이 극적으로 줄어듭니다.

특히 유용한 최적화는 기본형 타입을 사용하고 큰 1차원 배열을 생성하는 것입니다. 기본형 컬렉션을 위한 훌륭한 라이브러리로 trove4j가 있습니다.

```java
// 힙 할당 - 불필요함
Integer i = 817598;

// 스택 할당 - 선호됨
int i = 817598;

// 세 개의 힙 객체
Integer[] i = { 1337, 424242 };

// 하나의 힙 객체 (배열 컨테이너만)
int[] i = { 1337, 424242 };
```

## 6. 재귀 피하기

성능에 민감한 경로에서는 스택 프레임이 리소스를 소비하므로 재귀를 피하세요. 함수형 언어는 꼬리 호출 최적화를 제공하지만, Java는 이를 보장하지 않습니다. 핫 코드 경로에서는 단순한 반복이 재귀 구현보다 선호됩니다.

## 7. entrySet() 사용하기

키와 값 모두가 필요한 맵 반복 시, `entrySet()`이 우수합니다. `Map.Entry` 객체를 직접 제공하기 때문입니다. 대안으로 `keySet()`을 사용한 후 `get()`을 호출하면 조회가 중복되어 반복 사이클마다 불필요한 오버헤드가 추가됩니다.

```java
// 비효율적 - 각 키에 대해 get() 호출
for (K key : map.keySet()) {
    V value = map.get(key);
}

// 효율적 - 단일 반복
for (Entry<K, V> entry : map.entrySet()) {
    K key = entry.getKey();
    V value = entry.getValue();
}
```

## 8. EnumSet 또는 EnumMap 사용하기

이러한 구조는 해시 테이블 대신 서수 인덱스 배열을 사용하기 때문에 `HashMap`보다 성능이 뛰어납니다. 구현은 각 접근 시 `hashCode()`를 계산하고 `equals()`를 호출하는 대신 `key.ordinal()`을 통해 간단한 배열 조회를 수행합니다.

```java
// EnumMap은 서수 기반 배열 접근을 사용
private transient Object[] vals;

public V put(K key, V value) {
    // ...
    int index = key.ordinal();
    vals[index] = maskNull(value);
    // ...
}
```

## 9. hashCode()와 equals() 메서드 최적화하기

최적화된 `hashCode()` 구현은 빠르게 구별되는 값을 반환해야 합니다 - 이상적으로는 (String의 캐시된 hashCode처럼) 캐시된 값이어야 합니다.

`equals()` 메서드는 일찍 단축 평가해야 합니다: 비용이 많이 드는 비교 전에 `this == argument`, 타입 호환성, 그리고 빠른 부분 유효성 검사를 확인해야 합니다.

명백한 경우에 비교를 일찍 중단한 후, 부분적인 결정을 내릴 수 있을 때도 비교를 일찍 중단하고 싶을 수 있습니다. 인수가 this와 같을 수 없고, 이를 쉽게 확인할 수 있다면, 그렇게 하고 검사가 실패하면 중단하세요. 우주에 있는 대부분의 객체는 같지 않기 때문에, 이 메서드를 단축 평가함으로써 많은 CPU 시간을 절약할 수 있습니다.

```java
// AbstractTable 구현 - 단순하고 빠름
@Override
public int hashCode() {
    // 캐시된 String hashCode 사용
    return name.hashCode();
}

// AbstractQueryPart - 비용이 많이 드는 기본값
@Override
public int hashCode() {
    // 전체 SQL 렌더링 트리거
    return create().renderInlined(this).hashCode();
}
```

```java
@Override
public boolean equals(Object that) {
    if (this == that) {
        return true;
    }

    // 호환되지 않는 타입에 대한 조기 종료
    if (that instanceof AbstractTable) {
        if (StringUtils.equals(name,
            (((AbstractTable<?>) that).name))) {
            return super.equals(that);
        }
        return false;
    }
    return false;
}
```

## 10. 개별 요소가 아닌 집합으로 생각하기

선언적 집합 연산(교집합, 합집합)은 최적화 엔진이 효율적인 알고리즘을 선택할 수 있게 합니다. 수동으로 집합 연산을 수행하는 명령형 루프를 작성하면 최적화기가 병렬 실행, 특화된 데이터 구조, 또는 코드 수준에서 보이지 않는 알고리즘적 개선을 선택하는 것을 방지합니다.

명령형과 함수형 프로그래밍 스타일에는 SQL과 R 및 유사한 언어만이 가진 것이 부족합니다: 선언적 프로그래밍입니다. SQL에서는 어떤 알고리즘적 암시도 하지 않고 데이터베이스에서 얻고자 하는 결과를 선언할 수 있습니다.

이 조언은 O(N^3)에서 O(n log n)으로 이동하는 데 도움이 될 수 있습니다. 불행히도 많은 프로그래머들은 단순하고 지역적인 알고리즘 관점에서 생각합니다. 그들은 문제를 단계별로, 분기별로, 루프별로, 메서드별로 해결합니다. 그것이 명령형 및/또는 함수형 프로그래밍 스타일입니다.

```java
// 선언적 (SQL과 유사)
SomeSet INTERSECT SomeOtherSet

// 명령형 접근
Set result = new HashSet();
for (Object candidate : someSet)
    if (someOtherSet.contains(candidate))
        result.add(candidate);

// Java 8 함수형 접근 (여전히 명령형)
someSet.stream()
       .filter(someOtherSet::contains)
       .collect(Collectors.toSet());
```

---

## 결론

저자는 이 열 가지 전략을 jOOQ의 특정 사용 사례 맥락에서 설명합니다 - SQL 생성이 수백만 번 실행되는 "먹이 사슬의 맨 아래"에서 작동합니다. jOOQ가 "N x O x P 번" 실행되기 때문에, 낭비되는 모든 CPU 사이클과 메모리 할당은 전체 알고리즘에 걸쳐 곱해집니다.

비즈니스 로직은 일반적으로 핫 경로 깊숙이 있지 않지만, 인프라 코드(커스텀 SQL 프레임워크, 커스텀 라이브러리)는 Java Mission Control이나 DynaTrace와 같은 프로파일러를 사용하여 이러한 마이크로 최적화를 적용하기 전에 실제 병목 현상을 식별하는 유사한 검토를 거쳐야 합니다.

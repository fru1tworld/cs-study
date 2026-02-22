# 좋고 규칙적인 API를 설계하는 방법

> 원문: https://blog.jooq.org/how-to-design-a-good-regular-api/

사람들은 모든 종류의 것에 대해 규칙성(regularity)을 좋아합니다. 이것이 기본적으로 "최소 놀람의 원칙(Principle of Least Astonishment)"이 의미하는 바입니다. 여러분이 많은 것들을 동시에 맞추면 사람들은 그것을 좋아합니다. 이 기사에서는 API의 규칙성에 대한 몇 가지 생각을 나열할 것입니다.

## 규칙 #1: 강력한 용어를 확립하라

이것은 자체적인 별도의 기사가 필요한 주제입니다. API를 작성할 때, 같은 작업에 대해 서로 다른 용어를 계속 사용하면 사용자가 크게 혼란스러워질 것입니다. JDBC에서 구문 실행은 항상 "execute"라고 불립니다. 여러분은 `execute()`, `executeQuery()`, `executeUpdate()`, 그리고 `executeBatch()` 메서드를 볼 수 있습니다. 다른 한편으로, 리소스를 해제하는 것은 연결, 구문, 결과 집합 모두에서 일관되게 `close()`라고 불립니다.

### 위반 사례

JDK의 `Observable` 클래스는 모든 컬렉션에서 사용되는 표준 `remove()` 패턴 대신 `deleteObserver()`를 사용합니다. Spring 프레임워크의 클래스들은 "AbstractBeanFactoryBasedTargetSourceCreator"와 같이 불필요하게 복잡한 복합 이름을 사용합니다. "SourceSource"가 무엇일까요? 아니면 제가 가장 좋아하는 것: "SourceSourceTargetProviderSource"는요? 이러한 이름들은 Spring의 명명 불일치를 보여줍니다.

## 규칙 #2: 용어 조합에 대칭성을 적용하라

일단 용어가 확립되면, 대칭적으로 조합되어야 합니다. 컬렉션 API가 이를 보여줍니다:

- `add()`는 `addAll()`과 짝을 이룹니다
- `remove()`는 `removeAll()`과 짝을 이룹니다
- `contains()`는 `containsAll()`과 짝을 이룹니다

### 위반 사례

`Map`은 `keySet()`, `values()`, `entrySet()`을 제공하지만 `containsEntry()`가 없습니다. `put()`/`putAll()`과 `remove()` 사이의 메서드 명명이 일관성이 없습니다. `Map`은 `containsKey()`를 제공하면서 `keySet()`도 제공하는데, 단순히 `keys()`를 사용하는 것이 더 일관성 있었을 것입니다.

## 규칙 #3: 오버로딩을 통해 편의성을 추가하라

오버로딩은 근본적으로 다른 동작이 아닌 진정한 편의성을 제공해야 합니다. 오버로딩된 메서드가 단순한 편의 변형처럼 보이지만 실제로는 다른 동작을 하면 사용자가 놀라게 됩니다.

### 위반 사례

`TreeSet` 생성자는 다른 동작을 보입니다: 하나는 순서를 보존하고 다른 하나는 순서를 재설정합니다. 단순한 편의 변형처럼 보임에도 불구하고 말입니다. `SortedSet` 인수에서 `Comparator`를 조용히 추출하면서 `Collection` 인수에 대해서는 이를 무시합니다.

## 규칙 #4: 일관된 인수 순서

메서드 인수는 API 전체에서 일관된 순서를 유지해야 합니다. 사용자가 메서드 시그니처를 보고 인수의 순서를 예측할 수 있어야 합니다.

### 위반 사례

`Arrays.fill(Object[], int, int, Object)`는 선택적 범위 인수를 유사한 메서드와 비교하여 일관성 없게 배치합니다. `fill(Object[], Object)`와 비교했을 때 선택적 범위 매개변수의 위치가 일관성이 없습니다.

## 규칙 #5: 반환 값 타입을 확립하라

API는 부재하는 값에 대해 일관된 패턴을 따라야 합니다:

- 단일 객체: `null` 반환
- 컬렉션: 빈 컬렉션 반환, 절대 `null`이 아님
- 예외: 진정한 오류를 위해 예약

### 주요 위반 사례

`File.list()`는 예외를 던지는 대신 I/O 오류에 대해 `null`을 반환합니다. JPA는 일관성이 없이 `find()`에서는 `null`을 사용하지만 `getSingleResult()`에서는 예외를 던집니다. `EntityManager.find()`가 `null`을 반환하는 반면 `Query.getSingleResult()`는 `NoResultException`을 던집니다.

## 핵심 통찰

"배열이나 컬렉션을 반환할 때 절대로 null을 반환하지 마세요" - 일관된 패턴을 확립하면 런타임 놀라움과 방어적 프로그래밍 오버헤드를 방지할 수 있습니다.

저자는 규칙성이 "최소 놀람의 원칙" 위반을 방지한다고 강조합니다. 잘 설계된 API를 사용하는 개발자는 문서를 참조하지 않고도 동작을 예측할 수 있습니다.

이 기사는 Joshua Bloch의 API 설계 프레젠테이션을 참조하며, 라이브러리 개발자를 위한 필수 읽을거리로 "Effective Java"를 추천합니다.

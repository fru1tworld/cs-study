# Java 8의 흥미로운 아이디어: 스트림

> 원문: https://blog.jooq.org/exciting-ideas-in-java-8-streams/

Brian Goetz가 "State of the Lambda"에 대해 게시한 글에서 Java 8의 흥미로운 개념들, 특히 "스트림(Streams)"과 "컬렉션(Collections)"의 차이점을 소개했습니다.

## 핵심 개념

Java 8의 새로운 확장 메서드를 사용하면 `Iterable` 인터페이스를 "지연(lazy)" 연산과 "즉시(eager)" 연산 모두를 포함하는 스트리밍 메서드로 확장할 수 있습니다.

### 지연 연산 (Lazy Operations)

- `filter(Predicate)` - 요소를 필터링
- `map(Mapper)` - 요소를 변환
- `flatMap(Mapper)` - 중첩된 이터러블을 평탄화
- `cumulate(BinaryOperator)` - 값을 누적
- `sorted(Comparator)` - 요소를 정렬
- `sortedBy(Mapper)` - 추출된 값으로 정렬
- `uniqueElements()` - 중복 제거
- `pipeline(Mapper)` - 사용자 정의 변환
- `mapped(Mapper)` - 바이-스트림 생성
- `groupBy(Mapper)` - 키별로 그룹화
- `groupByMulti(Mapper)` - 여러 값으로 그룹화

### 즉시 연산 (Eager Operations)

- `isEmpty()`, `count()`, `getFirst()`, `getOnly()`, `getAny()` - 정보 조회
- `forEach(Block)` - 부수 효과와 함께 반복
- `reduce(T, BinaryOperator)` - 요소들을 결합
- `into(A)` - 대상 컨테이너에 수집
- `anyMatch()`, `noneMatch()`, `allMatch()` - 불리언 술어
- `maxBy()`, `minBy()` - 극값 찾기

## 목적

이 글은 이것들이 예비적인 "허수아비(strawman)" API 아이디어임을 강조합니다. Goetz는 커뮤니티 테스트를 권장하며 다음과 같이 말했습니다: "커뮤니티가 할 수 있는 가장 가치 있는 일은... 코드를 일찍, 그리고 자주 시험해 보는 것입니다."

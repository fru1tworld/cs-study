# Scala 컬렉션 가이드 (scala.collection)

Scala 공식 컬렉션 가이드([Scala 2.13 Collections](https://docs.scala-lang.org/overviews/collections-2.13/introduction.html))를 한국어로 완역했다. 원문 설명을 따라가면서 필요한 배경을 확인할 수 있도록, 번역 사이에 '처음 배우는 분께', '왜 필요한가', '짚고 넘어가기' 세 종류의 보충 설명을 넣었다.

## 목차

- 01 [서론과 가변, 불변 컬렉션](01_introduction_and_mutability.md): 설계 목표와 패키지 계층 구조
- 02 [Iterable과 시퀀스](02_iterable_and_sequences.md): 공통 연산, `Seq`, `IndexedSeq`, `LinearSeq`, 버퍼
- 03 [집합과 맵](03_sets_and_maps.md): `Set`, `SortedSet`, `BitSet`, `Map`
- 04 [구체적인 컬렉션 클래스](04_concrete_collection_classes.md): 불변, 가변 컬렉션 구현체
- 05 [배열과 문자열](05_arrays_and_strings.md): `Array`, `ClassTag`, `String`의 시퀀스 연산
- 06 [성능 특성과 동등성](06_performance_and_equality.md): 연산 시간 복잡도와 컬렉션 비교 규칙
- 07 [뷰와 이터레이터](07_views_and_iterators.md): 지연 평가와 `Iterator` 연산
- 08 [컬렉션 생성과 변환](08_creating_and_converting.md): 팩토리 메서드와 Java, Scala 컬렉션 변환

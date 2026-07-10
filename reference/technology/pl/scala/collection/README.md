# Scala 컬렉션 가이드 (scala.collection)

Scala 공식 컬렉션 가이드([Scala 2.13 Collections](https://docs.scala-lang.org/overviews/collections-2.13/introduction.html))의 한국어 완역입니다.
원문 번역 사이사이에 세 종류의 보충 콜아웃(📘 처음 배우는 분께 / 💡 왜 필요한가 / ⚠️ 짚고 넘어가기)이 들어 있습니다.

## 목차

| # | 문서 | 내용 |
|---|------|------|
| 01 | [서론](01_introduction.md) | 컬렉션 라이브러리의 설계 목표와 특징 |
| 02 | [가변 컬렉션과 불변 컬렉션](02_mutable_and_immutable_collections.md) | `scala.collection` / `mutable` / `immutable` 패키지 계층 구조 |
| 03 | [Iterable 트레이트](03_trait_iterable.md) | 모든 컬렉션의 최상위 트레이트와 공통 연산 |
| 04 | [시퀀스 트레이트](04_seqs.md) | `Seq`, `IndexedSeq`, `LinearSeq`와 버퍼 |
| 05 | [집합](05_sets.md) | `Set`, `SortedSet`, `BitSet` |
| 06 | [맵](06_maps.md) | `Map`의 조회·추가·변환 연산 |
| 07 | [구체적인 불변 컬렉션 클래스](07_concrete_immutable_collection_classes.md) | `List`, `LazyList`, `Vector`, `Range`, 해시 트라이 등 |
| 08 | [구체적인 가변 컬렉션 클래스](08_concrete_mutable_collection_classes.md) | `ArrayBuffer`, `ListBuffer`, `Queue`, 해시 테이블 등 |
| 09 | [배열](09_arrays.md) | `Array`와 제네릭 배열 생성, `ClassTag` |
| 10 | [문자열](10_strings.md) | `String`이 시퀀스 연산을 지원하는 원리 |
| 11 | [성능 특성](11_performance_characteristics.md) | 컬렉션별 연산 시간 복잡도 표 |
| 12 | [동등성](12_equality.md) | 컬렉션 비교 규칙과 가변 컬렉션 키의 함정 |
| 13 | [뷰](13_views.md) | 지연 평가 컬렉션 변환 |
| 14 | [이터레이터](14_iterators.md) | `Iterator` 연산과 버퍼링 |
| 15 | [컬렉션 처음부터 만들기](15_creating_collections_from_scratch.md) | `apply`, `fill`, `tabulate` 등 팩토리 메서드 |
| 16 | [Java와 Scala 컬렉션 간 변환](16_conversions_between_java_and_scala_collections.md) | `CollectionConverters`의 `asScala` / `asJava` |

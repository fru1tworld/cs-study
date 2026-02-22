> 원문: https://blog.jooq.org/jooq-tuesdays-mario-fusco-talks-about-functional-and-declarative-programming/

# jOOQ 화요일: Mario Fusco가 말하는 함수형 프로그래밍과 선언적 프로그래밍

저자: lukaseder
날짜: 2016년 12월 20일

## 개요

이 인터뷰는 자바 챔피언이자 LambdaJ의 저자인 Mario Fusco가 자바 개발에서의 함수형 프로그래밍(Functional Programming)과 선언적 접근 방식(Declarative Approach)에 대해 이야기합니다.

## 주요 주제

### LambdaJ의 기원과 구현

Fusco는 2007년에 Java 5를 함수형으로 얼마나 밀어붙일 수 있는지 탐구하기 위한 개념 증명(Proof-of-Concept)으로 LambdaJ를 만들었습니다. 이 라이브러리는 Java 8 람다(Lambda)가 존재하기 전에 타입 안전한 메서드 참조를 가능하게 하기 위해 창의적인 프록시 기법을 사용했습니다.

핵심 메커니즘은 고유한 반환 값을 생성하면서 메서드 호출을 등록하는 프록시를 만드는 것이었습니다. Fusco가 설명했듯이, "프록시는 메서드 호출을 등록하는 것 외에는 유용한 일을 하지 않았지만" 함수형 스타일의 정렬 구문을 가능하게 했습니다.

### 함수형 프로그래밍이 중요한 이유

Fusco는 함수형 프로그래밍의 여섯 가지 매력적인 측면을 제시합니다:

1. 가독성(Readability) – 코드가 퍼즐이 아닌 이야기처럼 읽힙니다
2. 선언적 특성(Declarative Nature) – 구현 단계보다 원하는 결과에 집중합니다
3. 데이터/동작의 균일한 처리 – 효율적인 분산 처리를 가능하게 합니다
4. 높은 추상화 수준 – 조합 가능한 함수를 통해 코드 중복을 줄입니다
5. 불변성과 참조 투명성(Immutability and Referential Transparency) – 추론과 테스트를 단순화합니다
6. 병렬 처리 친화성(Parallelism Friendliness) – 멀티코어 아키텍처에 자연스럽게 적합합니다

### 함수형 프로그래밍과 SQL

Fusco는 SQL이 특히 데이터 선택에 있어서 함수형 프로그래밍의 선언적 패러다임을 공유한다고 언급합니다. 그러나 그는 SQL의 파괴적 수정 접근 방식(데이터 덮어쓰기/삭제)이 함수형 프로그래밍의 불변성 원칙과 대조된다고 관찰합니다.

### Red Hat에서의 현재 업무

Red Hat에서 Fusco는 "선언적 프로그래밍의 극단적인 형태"를 대표하는 비즈니스 규칙 엔진인 Drools를 개발합니다. 이 작업은 언어 설계와 알고리즘 최적화를 결합합니다.

### 커뮤니티 기여

Fusco는 티치노(Ticino), 취리히(Zurich), CERN에서 VoxxedDays 컨퍼런스를 조직하며, 신진 연사들이 유능한 청중과 전문 지식을 공유할 수 있는 저렴한 1일 로컬 이벤트를 강조합니다.

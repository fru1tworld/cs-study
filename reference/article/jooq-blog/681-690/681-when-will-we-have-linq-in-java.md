# Java에서 LINQ를 가질 수 있을까?

> 원문: https://blog.jooq.org/when-will-we-have-linq-in-java/

게시일: 2012년 7월 30일 | 작성자: Lukas Eder

## LINQ란 무엇인가?

LINQ(Language Integrated Query)는 Microsoft .NET Framework의 가장 독특한 언어 기능 중 하나입니다. C#과 다른 언어에 도입되었을 때, 언어 명세에 상당한 변경이 필요했습니다. 하지만 그것은 극도로 강력했고, 아마도 Java, Scala 등 다른 언어/플랫폼에서는 비교할 수 없는 수준의 기능이었습니다.

## 다른 언어들의 시도

Scala는 비슷한 방식으로 XML을 통합했습니다. 하지만 이 성과는 LINQ와 비교할 수 없습니다. Typesafe의 개발자들은 비슷한 목표를 가진 SLICK(Scala Language Integrated Connection Kit)을 만들고 있습니다. 그러나 노력의 차이가 있습니다: 공식 Scala 개발자 한 명이 대규모 Microsoft 팀과 맞서고 있는 셈입니다.

만약 SLICK이 인기를 얻게 된다면, Microsoft와의 특허 전쟁이 발생할 수 있다는 점도 주의해야 합니다.

## Java의 현재 상황

Stack Overflow에는 Java에서 LINQ에 해당하는 것이 무엇인지에 대한 질문이 있습니다. 거기에는 LINQ와 유사한 기능을 구현하려는 여러 Java 접근 방식이 강조되어 있습니다.

Julian Hyde는 GitHub에서 linq4j 프로젝트를 만들었습니다. 그는 lambda-dev 메일링 리스트에서 참여를 시도했지만 "지금까지 아무런 응답이 없습니다."

jOOQ와 linq4j, 그리고 유사한 도구들은 "내부 도메인 특화 언어(internal domain specific languages)"입니다. 이들은 호스트 언어의 표현력에 의해 제한됩니다.

## 미래에 대한 희망

Java 8과 프로젝트 람다가 람다 표현식과 확장 메서드를 가져올 것으로 기대됩니다. 이러한 기능들이 결국 Java가 LINQ의 강력함에 접근할 수 있게 해줄지도 모릅니다.

Java 9쯤에는 가능할까요? 우리는 오직 희망할 수 있을 뿐입니다!

---

## 역자 노트

이 글은 2012년에 작성되었습니다. 그 이후로 많은 변화가 있었습니다:

- Java 8 (2014): 람다 표현식과 Stream API가 도입되어 함수형 프로그래밍 스타일의 데이터 처리가 가능해졌습니다.
- jOOQ: 현재까지도 활발히 개발되고 있으며, Java에서 타입 안전한 SQL 작성을 위한 가장 인기 있는 라이브러리 중 하나입니다.
- LINQ vs Java Streams: Java의 Stream API는 컬렉션 처리에 있어 LINQ와 유사한 기능을 제공하지만, 데이터베이스 쿼리 통합 측면에서는 여전히 LINQ만큼 언어에 깊이 통합되지는 않았습니다.

Java는 보수적인 언어 진화 철학을 가지고 있어, 안정성을 빠른 기능 도입보다 우선시합니다. 이것이 LINQ와 같은 기능이 Java에 직접 통합되지 않은 이유 중 하나입니다.

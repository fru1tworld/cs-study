# Scala에서의 SQL, jOOQ가 갈 수 있는 곳

> 원문: https://blog.jooq.org/sql-in-scala-where-jooq-could-go/

저는 최근 jOOQ를 Scala에 통합하는 것이 얼마나 간단한지에 대해 블로그에 글을 쓴 적이 있습니다. 전체 블로그 글은 여기에서 확인하세요: [https://blog.jooq.org/the-ultimate-sql-dsl-jooq-in-scala/](https://blog.jooq.org/the-ultimate-sql-dsl-jooq-in-scala/) Scala가 요즘 가장 빠르게 부상하고 있는 JVM 언어 중 하나이기 때문에, 저는 이 선택지에 점점 더 흥분하고 있습니다.

Java 라이브러리를 Scala에 단순하게 통합하면 몇 가지 미해결 문제가 남습니다. jOOQ에는 바인드 값을 생성하기 위한 `val()` 연산자 또는 메서드가 있습니다. 매뉴얼은 여기에서 확인하세요: [https://www.jooq.org/manual/JOOQ/BindValues/](https://www.jooq.org/manual/JOOQ/BindValues/) 이 연산자는 Scala에서 사용할 수 없는데, Scala가 `val`을 예약어로 선언하고 있기 때문입니다.

저는 이전에 Java에서도 비슷한 문제를 겪은 적이 있는데, API에서 `case`나 `else`를 사용하려 할 때 마찬가지로 불가능했습니다. 가장 저항이 적은 방법은 해당 메서드들을 오버로드하거나 이름을 변경하는 것입니다.

`val`은 jOOQ 2.0.1에서 `value`로 오버로드되었습니다. `case`와 `else`는 오래전에 각각 `decode`(Oracle의 DECODE 함수에서 유래)와 `otherwise`(XSL에서처럼)로 이름이 변경되었습니다.

따라서 Scala에 완전히 통합하기 위해서는 jOOQ를 scOOQ라는 새로운 API로 래핑해야 합니다 ;-). 이 새로운 API는 jOOQ를 훨씬 더 쉽게 사용할 수 있도록 Scala의 언어 기능을 고려해야 합니다.

이것은 API의 일부를 재설계하고 모든 API 메서드를 SQL에서 일반적인 것처럼 대문자로 만들 수 있는 기회가 될 수 있습니다. Scala에서는 `.`이나 `()`와 같은 구문 요소를 생략할 수 있으므로, API에서 다음 Java 예제처럼 단일 단어 메서드를 선언할 수 있습니다:

```java
MERGE().INTO(MY_TABLE)
       .USING(SOURCE_TABLE)
       .ON(MY_TABLE.ID.equal(SOURCE_TABLE.ID))
       .WHEN().MATCHED().THEN().UPDATE()
         .SET(ID, 1)
         .SET(DATA, "Data")
       .WHEN().NOT().MATCHED().THEN().INSERT(MY_TABLE.ID, MY_TABLE.DATA)
       .VALUES(1, "Data")
```

Java에서는 이것이 꽤 보기 안 좋고 장황하게 보이지만, Scala에서는 매우 깔끔할 수 있습니다! 아래 구문은 API가 이렇게 선언된다면 Scala에서 컴파일되어야 합니다:

```scala
MERGE INTO MY_TABLE
      USING (SOURCE_TABLE)
      ON (MY_TABLE.ID equal SOURCE_TABLE.ID)
      WHEN MATCHED THEN UPDATE
        SET (ID, 1)
        SET (DATA, "Data")
      WHEN NOT MATCHED THEN INSERT (MY_TABLE.ID, MY_TABLE.DATA)
      VALUES (1, "Data")
```

납득이 되시나요? 기여를 환영합니다! :-)

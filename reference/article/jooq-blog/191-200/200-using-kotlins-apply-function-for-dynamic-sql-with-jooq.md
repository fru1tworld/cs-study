# Kotlin의 Apply 함수를 jOOQ의 동적 SQL에 사용하기

> 원문: https://blog.jooq.org/using-kotlins-apply-function-for-dynamic-sql-with-jooq/

게시일: 2017년 6월 9일
저자: lukaseder

이전에 우리는 "[jOOQ로 Kotlin에서 SQL 작성하는 10가지 멋진 예제](https://blog.jooq.org/10-nice-examples-of-writing-sql-in-kotlin-with-jooq/)"에 대해 블로그를 작성한 적이 있습니다. Kotlin은 Java 라이브러리와의 통합에 특히 유용한 수많은 기능들을 제공합니다.

## with() 함수

그 중 하나가 `with()` stdlib 함수입니다. 이 함수를 사용하면 지역 범위에서 "네임스페이스를 임포트"할 수 있습니다. 예를 들어:

```kotlin
with (AUTHOR) {
    ctx.select(FIRST_NAME, LAST_NAME)
       .from(AUTHOR)
       .where(ID.lt(5))
       .orderBy(ID)
       .fetch {
           println("${it[FIRST_NAME]} ${it[LAST_NAME]}")
       }
}
```

## apply() 함수

`apply()` 함수를 통해 매우 유사한 기능이 제공되지만, 구문적인 의미는 다릅니다. Kotlin에서 `with()` vs. `apply()`의 차이점에 대한 자세한 내용은 [Stack Overflow 질문](https://stackoverflow.com/questions/35513636/kotlin-apply-vs-with)을 확인하세요.

jOOQ를 사용할 때 `apply()`는 동적 SQL에 가장 유용합니다. 쿼리의 일부 부분을 조건부로 추가해야 하는지를 나타내는 지역 변수가 있다고 상상해보세요:

```kotlin
val filtering = true
val joining = true
```

이 boolean 변수들은 동적으로 평가됩니다. `filtering` 변수는 동적 필터/where 절이 필요한지를 지정하고, `joining`은 추가적인 JOIN이 필요한지를 지정합니다.

따라서 쿼리는 저자(author)를 선택하며:
- "filtering"이면, ID = 1인 저자만 선택합니다
- "joining"이면, books 테이블을 조인하고 저자당 책의 수를 카운트합니다

이 두 조건은 서로 독립적입니다.

전체 쿼리는 다음과 같습니다:

```kotlin
ctx.select(
      a.FIRST_NAME,
      a.LAST_NAME,
      if (joining) count() else value(""))
   .from(a)
   .apply { if (filtering) where(a.ID.eq(1)) }
   .apply { if (joining) join(b).on(a.ID.eq(b.AUTHOR_ID)) }
   .apply { if (joining) groupBy(a.FIRST_NAME, a.LAST_NAME) }
   .orderBy(a.ID)
   .fetch {
       println(it[a.FIRST_NAME] + " " +
               it[a.LAST_NAME] +
               (if (joining) " " + it[count()] else ""))
   }
```

핵심 패턴에 주목하세요:

```kotlin
.apply { if (filtering) where(a.ID.eq(1)) }
```

where() 절이 필터링하는 경우에만 추가됩니다!

물론 jOOQ(또는 다른 쿼리 빌더)는 이러한 종류의 동적 SQL에 적합하며, Java에서도 할 수 있습니다. 하지만 `apply()`를 사용한 Kotlin 특유의 유창한(fluent) 통합은 매우 깔끔합니다.

## 중요한 주의사항

이것이 작동하는 이유는 jOOQ 3.x의 jOOQ DSL API가 가변적(mutable)이고 모든 연산이 동일한 `this` 참조를 반환하기 때문입니다.

Ilya Ryzhenkov가 지적했듯이: "적용된 함수에서 `this`가 아닌 다른 것을 반환하는 fluent API에서는 작동하지 않습니다."

향후(예: 버전 4.0)에 jOOQ는 API를 더 불변(immutable)하게 만들 계획입니다 - 가변성은 역사적 유산이지만, 쿼리 빌더에서는 종종 원하는 동작이기도 합니다.

## 참고 자료

- [jOOQ 동적 SQL 문서](https://www.jooq.org/doc/latest/manual/sql-building/dynamic-sql)
- [jOOQ로 Kotlin에서 SQL 작성하는 10가지 멋진 예제](https://blog.jooq.org/10-nice-examples-of-writing-sql-in-kotlin-with-jooq/)
- [Kotlin stdlib의 apply() 및 with() 함수 문서](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/apply.html)

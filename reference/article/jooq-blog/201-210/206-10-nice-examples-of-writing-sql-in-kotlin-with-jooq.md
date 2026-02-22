> 원문: https://blog.jooq.org/10-nice-examples-of-writing-sql-in-kotlin-with-jooq/

# jOOQ와 함께 Kotlin으로 SQL 작성하기 10가지 멋진 예제

작성자: lukaseder

작성일: 2017년 5월 18일 (2022년 5월 3일 수정)

---

## 소개

Google이 Android에 대한 공식 지원을 발표한 이후 Kotlin의 인기가 높아지고 있습니다. 이 글에서는 애플리케이션을 Kotlin으로 마이그레이션할 때 jOOQ(Java Object Oriented Querying)를 어떻게 활용할 수 있는지 살펴보고, Kotlin에서 SQL을 사용하는 10가지 실용적인 예제를 소개합니다.

---

## 10가지 예제

### 1. jOOQ 레코드에서 맵 접근 리터럴 사용하기

Kotlin의 연산자 오버로딩(Operator Overloading)을 통해 레코드에 우아하게 접근할 수 있습니다:

```kotlin
val a = AUTHOR
val b = BOOK

ctx.select(a.FIRST_NAME, a.LAST_NAME, b.TITLE)
   .from(a)
   .join(b).on(a.ID.eq(b.AUTHOR_ID))
   .orderBy(1, 2, 3)
   .forEach {
       println("${it[b.TITLE]} by "
             + "${it[a.FIRST_NAME]} ${it[a.LAST_NAME]}")
   }
```

출력:
```
1984 by George Orwell
Animal Farm by George Orwell
Brida by Paulo Coelho
O Alquimista by Paulo Coelho
```

주요 특징:
- 타입 추론을 위한 `val`
- `${}`를 사용한 문자열 보간(String Interpolation)
- 암시적 람다 매개변수 `it`
- 대괄호 표기법을 위한 연산자 오버로딩

### 2. 레코드 내용 순회하기

구조 분해(Destructuring)를 통해 레코드 데이터를 우아하게 순회할 수 있습니다:

```kotlin
for (r in ctx.fetch(b))
    for ((k, v) in r.intoMap())
        println("${r[b.ID]}: ${k.padEnd(20)} = $v")
```

이 예제는 jOOQ의 `intoMap()` 메서드와 함께 Kotlin의 다중 선언(Multi-declaration, 구조 분해) 기능을 보여줍니다.

### 3. 지역 변수 타입 추론

jOOQ 쿼리에서 복잡한 제네릭 타입을 단순화합니다:

```kotlin
val subselect =
  select(CUSTOMER.FIRST_NAME, CUSTOMER.LAST_NAME).from(CUSTOMER);

// 타입 안전성은 여전히 적용됩니다:
row(AUTHOR.FIRST_NAME, AUTHOR.LAST_NAME).in(subselect)
```

### 4. 인라인 함수로 새로운 jOOQ 기능 만들기

Kotlin의 인라인 함수(Inline Functions)를 사용하여 사용자 정의 연산자를 만들 수 있습니다:

```kotlin
inline fun <F: Field<String>> F.ilike(field: String): Condition {
    return condition("{0} ilike {1}", this, field)
}

// 사용법:
ctx.select(b.TITLE)
   .from(b)
   .where(b.TITLE.ilike("%animal%"))
   .fetchOne(b.TITLE)
```

출력: `Animal Farm`

### 5. 문자열 보간과 여러 줄 문자열

Kotlin은 임베디드 SQL 처리를 단순화합니다:

```kotlin
ctx.resultQuery("""
    SELECT *
    FROM (
      VALUES (1, 'a'),
             (2, 'b')
    ) AS t
    """)
    .fetch()
    .forEach {
        println("${it.intoMap()}")
    }
```

출력:
```
{C1=1, C2=a}
{C1=2, C2=b}
```

경고: 문자열 보간은 SQL 인젝션(SQL Injection) 위험이 있으므로 주의해서 사용해야 합니다.

### 6. Null 안전 역참조

Nullable 결과를 우아하게 처리합니다:

```kotlin
println("${ctx.fetchOne(b, b.ID.eq(5))?.title ?: "not found"}")
```

이 예제는 안전 호출 연산자 `?.`와 엘비스 연산자(Elvis Operator) `?:`를 결합하여 null을 우아하게 처리합니다.

출력: `not found`

### 7. Getter와 Setter를 통한 프로퍼티

Kotlin 문법은 장황한 JavaBean 패턴을 제거합니다:

```kotlin
val author1 = ctx.selectFrom(a).where(a.ID.eq(1)).fetchOne();
println("${author1.firstName} ${author1.lastName}")

val author2 = ctx.newRecord(a);
author2.firstName = "Alice"
author2.lastName = "Doe"
author2.store()
```

### 8. `with()`를 사용하여 Setter 호출 줄이기

`with()` 인라인 함수는 반복을 줄여줍니다:

```kotlin
val author3 = ctx.newRecord(a);
with (author3) {
    firstName = "Bob"
    lastName = "Doe"
    store()
}
```

블록 내의 모든 연산은 암시적으로 `author3`를 참조합니다.

### 9. `with()`를 사용하여 테이블을 로컬로 임포트하기

특정 범위 내에서 컬럼 참조를 단순화합니다:

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

출력:
```
George Orwell
Paulo Coelho
```

### 10. 레코드를 지역 변수로 구조 분해하기

레코드 값을 개별 변수로 추출합니다:

```kotlin
operator fun <T, R : Record3<T, *, *>> R.component1() : T {
    return this.value1();
}
// component2()와 component3()에 대해서도 유사한 연산자 정의...

for ((first, last, title) in ctx
   .select(a.FIRST_NAME, a.LAST_NAME, b.TITLE)
   .from(a)
   .join(b).on(a.ID.eq(b.AUTHOR_ID))
   .orderBy(1, 2, 3))
       println("$title by $first $last")
```

---

## 결론

이 글은 Kotlin의 실용적인 설계 철학과 Java와의 원활한 상호 운용성을 강조합니다. 이러한 특징들은 Kotlin을 JVM에서 jOOQ를 사용하여 타입 안전한 SQL 쿼리를 작성하기에 매우 생산적인 선택으로 만들어 줍니다.

코드 저장소: https://github.com/jOOQ/jOOQ/tree/master/jOOQ-examples/jOOQ-kotlin-example

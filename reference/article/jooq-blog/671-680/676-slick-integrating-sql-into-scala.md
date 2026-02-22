# SLICK, SQL을 Scala에 통합하기

> 원문: https://blog.jooq.org/slick-integrating-sql-into-scala/

Typesafe가 SLICK(Scala Language-Integrated Connection Kit)을 공식 발표했습니다. 이것은 Scala의 LINQ-to-SQL에 해당하는 도구로 소개되었습니다. 이 발표는 다음과 같은 여러 채널을 통해 이루어졌습니다:

- Typesafe 블로그 공지
- Yahoo Finance 보도
- Martin Odersky가 소개된 DZone 기사

## SLICK이란?

SLICK은 "Scala Language-Integrated Connection Kit"의 약자이며, Scala 개발자를 위한 데이터베이스 쿼리 도구로 기능합니다. 이것은 넓은 의미의 LINQ가 아니라 구체적으로 LINQ-to-SQL과 비교할 수 있다는 점을 명확히 해야 합니다. Scala는 이미 내장된 컬렉션 쿼리 기능을 가지고 있기 때문입니다.

## 코드 예제

다음은 테이블 정의와 쿼리를 보여주는 SLICK 문서의 예제입니다:

```scala
object Coffees extends Table[(String, Int, Double)]("COFFEES") {
  def name = column[String]("COF_NAME", O.PrimaryKey)
  def supID = column[Int]("SUP_ID")
  def price = column[Double]("PRICE")
  def * = name ~ supID ~ price
}

Coffees.insertAll(
  ("Colombian",         101, 7.99),
  ("Colombian_Decaf",   101, 8.99),
  ("French_Roast_Decaf", 49, 9.99)
)

// supID가 101인 커피의 이름과 가격을 조회
val q = for {
  c <- Coffees if c.supID === 101
} yield (c.name, c.price)

println(q.selectStatement)

q.foreach { case (n, p) => println(n + ": " + p) }
```

## 설계 철학

SLICK의 목표는 개발자가 "SQL 대신 Scala로 데이터베이스 쿼리를 작성"할 수 있도록 하는 것입니다.

## jOOQ와의 대조

jOOQ는 근본적으로 다른 방향을 추구합니다: "SQL은 SQL 외의 다른 것이 될 의도로 만들어진 적이 없습니다!"

jOOQ의 Scala 통합 기능에 대해 언급하자면, jOOQ는 13개의 주요 데이터베이스에서 거의 SQL에 가까운 쿼리를 Scala로 작성할 수 있도록 지원합니다.

## 비교 분석의 기회

두 접근 방식을 다음과 같은 여러 차원에서 비교하는 독립적인 병렬 평가가 가치 있을 것입니다:

- 개발자 생산성
- 코드 유지보수성
- 성능 특성
- 기능 포괄성

Stack Overflow 토론에서 예비적인 비교 예제를 제공하고 있습니다.

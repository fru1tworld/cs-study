# jOOQ vs. Slick - 각 접근 방식의 장단점

> 원문: https://blog.jooq.org/jooq-vs-slick-pros-and-cons-of-each-approach/

모든 프레임워크는 새로운 타협점을 도입합니다. 이 타협점은 프레임워크가 여러분이 소프트웨어 인프라와 상호작용하고자 하는 방식에 대해 *어느 정도* 가정을 하기 때문에 도입됩니다.

이러한 타협점이 최근 사용자들에게 문제가 된 사례는 "[Slick 쿼리가 일반적으로 SQL 쿼리와 동형인가?](https://groups.google.com/d/msg/scalaquery/K2tch9yxx60/QB055F0pn3gJ)"라는 논의에서 찾을 수 있습니다. 물론 그 답은 "아니오"입니다.

## Slick 쿼리 예제

다음은 간단한 Slick 쿼리 예제입니다:

```scala
val salesJoin = sales
      join purchasers
      join products
      join suppliers on {
  case (((sale, purchaser), product), supplier) =>
    sale.productId === product.id &&
    sale.purchaserId === purchaser.id &&
    product.supplierId === supplier.id
}
```

## 생성된 SQL 출력

이 쿼리는 불필요하게 복잡한 파생 테이블이 포함된 SQL을 생성합니다 (가독성을 위해 재구성됨):

```sql
select x2.x3, x4.x5, x2.x6, x2.x7
from (
    select x8.x9 as x10,
           x8.x11 as x12,
           x8.x13 as x14,
           x8.x15 as x7,
           x8.x16 as x17,
           x8.x18 as x3,
           x8.x19 as x20,
           x21.x22 as x23,
           x21.x24 as x25,
           x21.x26 as x6
    from (
        select x27.x28 as x9,
               x27.x29 as x11,
               x27.x30 as x13,
               x27.x31 as x15,
               x32.x33 as x16,
               x32.x34 as x18,
               x32.x35 as x19
        from (
            select x36."id" as x28,
                   x36."purchaser_id" as x29,
                   x36."product_id" as x30,
                   x36."total" as x31
            from "sale" x36
        ) x27
        inner join (
            select x37."id" as x33,
                   x37."name" as x34,
                   x37."address" as x35
            from "purchaser" x37
        ) x32
        on 1=1
    ) x8
    inner join (
        select x38."id" as x22,
               x38."supplier_id" as x24,
               x38."name" as x26
        from "product" x38
    ) x21
    on 1=1
) x2
inner join (
    select x39."id" as x40,
           x39."name" as x5,
           x39."address" as x41
    from "supplier" x39
) x4
on ((x2.x14 = x2.x23)
and (x2.x12 = x2.x17))
and (x2.x25 = x4.x40)
where x2.x7 >= ?
```

## Christopher Vogt의 설명

Slick의 전 메인테이너인 Christopher Vogt는 다음과 같이 설명합니다:

> "이것은 Slick이 생성한 SQL 쿼리를 효율적으로 실행하기 위해 데이터베이스의 쿼리 옵티마이저에 의존한다는 것을 의미합니다. 현재 MySQL에서는 항상 그런 것은 아닙니다."

Slick의 설계 철학에 대해 그는 다음과 같이 덧붙입니다:

> "Slick은 정확하게 지정된 SQL 문자열을 빌드할 수 있게 해주는 DSL이 아닙니다. Slick의 Scala 쿼리 변환은 재사용과 합성을 가능하게 하며, Scala를 쿼리 작성 언어로 사용합니다. 이것은 정확한 SQL 쿼리를 예측할 수 있게 해주지 않으며, 단지 의미론(semantics)과 대략적인 구조만을 예측할 수 있습니다."

## Slick vs. jOOQ 비교

높은 수준에서 보면, Slick과 jOOQ는 합성성(compositionality)을 동등하게 수용합니다. 두 프레임워크 모두 수백 줄에 달하는 매우 복잡한 쿼리라도 여러 메서드에 걸쳐 쿼리를 합성하는 것을 지원합니다.

핵심적인 차이점은: "Slick은 Scala 컬렉션에 초점을 맞추고, jOOQ는 SQL 테이블에 초점을 맞춥니다."

### 세 가지 관점에서의 분석

- 개념적 관점 (이론적으로): 이 초점은 중요하지 않아야 합니다.

- 타입 안전성 관점: Scala 컬렉션은 SQL 테이블과 쿼리보다 타입 체크하기가 더 쉽습니다. 왜냐하면 SQL 언어 자체가 타입 체크하기 상당히 어렵기 때문입니다. 다양한 고급 SQL 절(예: outer join, grouping set, pivot 절, union, group by 등)의 의미론이 타입 구성을 다소 암묵적으로 변경하기 때문입니다.

- 실용적 관점: SQL 자체는 원래의 관계형 이론의 근사치일 뿐이며, 그 자체로 독자적인 생명을 얻었습니다.

### 핵심 구분

> "결국 이것은 Scala 컬렉션에 대해 추론하고 싶은지(쿼리가 클라이언트 코드와 더 잘 통합되고 더 관용적임) 아니면 SQL 테이블에 대해 추론하고 싶은지(쿼리가 데이터베이스와 더 잘 통합되고 더 관용적임)로 귀결됩니다."

## 객체-관계형 임피던스 불일치 문제

Hibernate의 창시자인 Gavin King은 고객과 사용자들이 ORM을 통해 SQL을 완전히 잊어버리기를 희망했다고 언급했습니다. 그러나 이것은 의도한 대로 실현되지 않았습니다. 고객과 사용자들이 Gavin과 다른 ORM 창시자들의 말을 듣지 않았기 때문에, 우리는 이제 많은 사람들이 객체-관계형 임피던스 불일치라고 부르는 것을 가지게 되었습니다. Hibernate와 JPA에 대해 많은 부당한 비판이 표출되었는데, 이 API들은 그들이 실제로 다루는 제한된 범위에 비해 단순히 너무 인기가 많았을 뿐입니다.

## 함수형-관계형 임피던스 불일치

Slick(또는 C#의 LINQ)에서도, 사용자들이 이러한 도구를 SQL의 대체재로 남용하면 유사한 불일치가 통합을 방해합니다. Slick은 관계형 모델을 Scala 언어에서 직접 모델링하는 것을 훌륭하게 수행합니다. 이것은 관계를 컬렉션처럼 추론하는 데 훌륭합니다. 그러나 이것은 SQL API가 *아닙니다*.

이것이 바로 "함수형-관계형 임피던스 불일치(Functional-Relational Impedance Mismatch)"입니다.

## 문서화된 이슈들

Slick 사용자들이 겪는 문제들:
- 원치 않는 파생 테이블 (GitHub 이슈 #623)
- outer join에 대한 제한된 지원

## SQL은 훨씬 더 많은 것

[SQL Performance Explained](http://sql-performance-explained.com/)의 저자인 Markus Winand는 최근 "[Modern SQL in Open Source and Commercial Databases](http://www.slideshare.net/MarkusWinand/modern-sql)"라는 프레젠테이션을 발표했는데, 이는 jOOQ가 완전히 수용하는 개념들을 대표합니다.

## jOOQ의 철학

Java, Scala, C#과 같은 범용 프로그래밍 언어에서 SQL을 숨기려고 시도하는 API들은 애플리케이션에 엄청난 가치를 더하는 유용한 기능들을 놓치고 있습니다. jOOQ는 SQL 언어의 모든 멋진 기능과 특이점을 완전히 수용합니다.

## 결론

이 글은 Scala에 가깝게 머무르는 것과 SQL에 가깝게 머무르는 것 사이의 선택에 대한 열린 논의를 제안하며 마무리됩니다. 후속 글에서는 객체-관계형 임피던스 불일치가 없다는 것을 보여줄 예정입니다 - 단지 SQL을 올바르게 사용하고 있지 않을 뿐입니다.

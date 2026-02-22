# LINQ와 Java

> 원문: https://blog.jooq.org/linq-and-java/

LINQ는 .NET 생태계에서 상당히 성공적이면서도 논쟁적인 추가 기능이었습니다. 많은 사람들이 Java 세계에서 비슷한 솔루션을 찾고 있습니다. 비슷한 솔루션이 무엇인지 더 잘 이해하기 위해, LINQ가 해결하는 주요 문제를 살펴보겠습니다: 쿼리 언어는 종종 많은 키워드를 가진 선언적 프로그래밍 언어입니다. 제어 흐름 요소는 거의 제공하지 않지만, 매우 설명적입니다. 가장 인기 있는 쿼리 언어는 SQL로, 주로 관계형 데이터베이스에 사용되는 ISO/IEC 표준화된 구조화 쿼리 언어입니다. 선언적 프로그래밍은 프로그래머가 알고리즘을 명시적으로 작성하지 않는다는 것을 의미합니다. 대신, 그들은 얻고자 하는 결과를 설명하고, 알고리즘적 계산은 구현 시스템에 맡깁니다. 일부 데이터베이스는 대규모 SQL 문을 해석하고, 언어 구문과 메타데이터를 기반으로 SQL 언어 변환 규칙을 적용하는 데 매우 능숙해졌습니다. 흥미로운 읽을거리로 Tom Kyte의 "metadata matters"가 있는데, 이는 Oracle의 비용 기반 옵티마이저에 투입된 엄청난 노력을 암시합니다. SQL Server, DB2 및 기타 주요 RDBMS에 대해서도 비슷한 논문을 찾을 수 있습니다.

## LINQ-to-SQL은 SQL이 아니다

LINQ는 C#이나 ASP와 같은 .NET 언어에 선언적 프로그래밍 측면을 내장할 수 있게 해주는 완전히 다른 쿼리 언어입니다. LINQ의 좋은 점은 C# 컴파일러가 C# 문 중간에 SQL처럼 보이는 것을 컴파일할 수 있다는 사실입니다. 어떤 면에서 LINQ는 .NET에게 PL/SQL에서의 SQL이나, pgplsql에서의 SQL, 또는 Java에서의 jOOQ와 같은 존재입니다. 그러나 실제 SQL 언어를 내장하는 PL/SQL과 달리, LINQ-to-SQL은 .NET 내에서 SQL 자체를 모델링하는 것을 목표로 하지 않습니다. 이것은 다양한 이기종 데이터 저장소에 대한 쿼리를 단일 언어로 통합하려는 시도의 문을 열어두는 더 높은 수준의 추상화입니다. 이러한 통합은 이전에 ORM이 그랬던 것처럼 유사한 임피던스 불일치를 만들어낼 것이며, 아마도 더 큰 불일치가 될 것입니다. 유사한 언어들은 어느 정도까지 서로 변환될 수 있지만, 숙련된 SQL 개발자가 매우 간단한 LINQ 문에서도 실제로 어떤 SQL 코드가 생성될지 예측하기가 상당히 어려워질 수 있습니다.

## LINQ 예제

LINQ-to-SQL 문서에서 제공하는 몇 가지 예제를 살펴보면 이것이 더 명확해집니다. 예를 들어 Count() 집계 함수를 보겠습니다:

```csharp
System.Int32 notDiscontinuedCount =
    (from prod in db.Products
    where !prod.Discontinued
    select prod)
    .Count();

Console.WriteLine(notDiscontinuedCount);
```

위 예제에서 `.Count()` 함수가 괄호로 묶인 쿼리 내에서 SQL `count(*)` 집계 함수로 변환되는지(그렇다면 왜 프로젝션에 넣지 않는 걸까요?), 아니면 쿼리 실행 후 애플리케이션 메모리에서만 적용되는지 즉시 명확하지 않습니다. 후자의 경우, 많은 수의 레코드를 데이터베이스에서 메모리로 전송해야 한다면 금지해야 할 정도로 문제가 됩니다. 트랜잭션 모델에 따라서는 읽기 잠금까지 필요할 수 있습니다! 또 다른 예제는 그룹화가 설명되는 곳에서 제공됩니다:

```csharp
var prodCountQuery =
    from prod in db.Products
    group prod by prod.CategoryID into grouping
    where grouping.Count() >= 10
    select new
    {
        grouping.Key,
        ProductCount = grouping.Count()
    };
```

이 경우, LINQ는 SQL과 완전히 다르게 언어적 측면을 모델링합니다. 위의 LINQ `where` 절은 분명히 SQL의 `HAVING` 절입니다. `into grouping`은 그룹화된 튜플에 대한 별칭으로, 꽤 좋은 아이디어입니다. 하지만 이것은 SQL에 직접 매핑되지 않으며, 타입이 지정된 출력을 생성하기 위해 LINQ 내부에서 사용되어야 합니다. 물론 멋진 점은 나중에 C#에서 직접 재사용할 수 있는 정적으로 타입이 지정된 프로젝션입니다! 또 다른 그룹화 예제를 살펴보겠습니다:

```csharp
var priceQuery =
    from prod in db.Products
    group prod by prod.CategoryID into grouping
    select new
    {
        grouping.Key,
        TotalPrice = grouping.Sum(p => p.UnitPrice)
    };
```

이 예제에서 C#의 함수형 측면이 LINQ의 `Sum(p => p.UnitPrice)` 집계 표현식에 내장되어 있습니다. `TotalPrice = ...`는 단순한 컬럼 별칭입니다. 위 내용은 저에게 많은 열린 질문을 남깁니다. 어떤 부분이 실제로 SQL로 변환되고, 어떤 부분이 SQL 쿼리가 부분 결과 집합을 반환한 후 내 애플리케이션에서 실행되는지 어떻게 제어할 수 있을까요? 람다 표현식이 LINQ 집계 함수에 적합한지, 언제 대량의 데이터가 인메모리 집계를 위해 메모리에 로드될지 어떻게 예측할 수 있을까요? 그리고 또한: 컴파일러가 C#/SQL 알고리즘 혼합을 생성하는 방법을 알아내지 못했다고 경고해 줄까요? 아니면 런타임에 단순히 실패할까요?

## LINQ를 사용할 것인가 말 것인가

오해하지 마세요. 영감을 얻기 위해 LINQ 매뉴얼을 들여다볼 때마다, 프로젝트에서 시도해보고 싶은 강한 충동이 듭니다. 멋져 보이고 잘 설계되어 있습니다. Stack Overflow에도 흥미로운 LINQ 질문들이 많이 있습니다. Java에 LINQ가 있어도 괜찮겠지만, 독자들에게 LINQ는 SQL이 아니라는 것을 상기시키고 싶습니다. SQL을 제어하고 싶다면, LINQ 또는 LINQ 유사 API는 두 가지 이유로 나쁜 선택일 수 있습니다:

1. 일부 SQL 메커니즘은 LINQ로 표현할 수 없습니다. JPA와 마찬가지로 순수 SQL에 의존해야 할 수도 있습니다.
2. 일부 LINQ 메커니즘은 SQL로 표현할 수 없습니다. JPA와 마찬가지로 심각한 성능 문제를 겪을 수 있으며, 따라서 다시 순수 SQL에 의존하게 됩니다.

LINQ 또는 그것의 "Java 구현"을 선택할 때 위의 사항을 주의하세요! 데이터 가져오기에는 SQL(즉, JDBC, jOOQ 또는 MyBatis)을 사용하고 인메모리 후처리에는 Java API(예: Java 8의 Stream API)를 사용하는 것이 더 나을 수 있습니다.

## Java, Scala에서 SQL을 모델링하는 LINQ 유사 라이브러리

- jOOQ: https://www.jooq.org
- Sqltyped: https://github.com/jonifreeman/sqltyped

## Java, Scala에서 SQL 구문과 데이터 저장소를 추상화하는 LINQ 유사 라이브러리

- Quaere: http://quaere.codehaus.org
- JaQu: http://www.h2database.com/html/jaqu.html
- Linq4j: https://github.com/julianhyde/linq4j
- Slick: http://slick.typesafe.com/

# SQL 접근 API의 타입 안전성 비교

> 원문: https://blog.jooq.org/a-typesafety-comparison-for-sql-access-apis/

2013년 2월 25일 lukaseder 게시

## 소개

SQL은 매우 표현력이 풍부하고 독특한 언어입니다. 일상 업무에서 광범위한 사용자층이 사용하는 몇 안 되는 선언형 언어 중 하나입니다. 선언형 언어로서 SQL은 출력이 *어떻게* 생성되어야 하는지가 아니라 *무엇*을 출력으로 기대하는지를 지정할 수 있게 해줍니다. 이것의 부수 효과로, 모든 문장에서 임시(ad-hoc) 레코드 데이터 타입이 생성됩니다. 예를 들면:

```sql
-- (id: integer, title: varchar) 타입이 생성됨
SELECT id, title
FROM book;
```

위의 문장은 다음과 같은 속성을 가진 잘 정의된 레코드 타입의 레코드들을 포함하는 커서를 생성합니다:

- 레코드의 차수(degree)는 2입니다
- 컬럼 이름은 `id`와 `title`입니다
- 컬럼 타입은 `integer`와 `varchar`입니다
- 컬럼 `id`는 인덱스 `1`에서 접근할 수 있습니다. 컬럼 `title`은 인덱스 `2`에서 접근할 수 있습니다

다시 말해, SQL 레코드는 레코드(이름으로 접근)와 튜플(인덱스로 접근)의 특성을 결합합니다. 맵 키가 배열 인덱스 및 관련 키/인덱스 타입에 형식적으로 바인딩된, 타입 안전한 연관 "맵-배열"처럼 볼 수 있습니다. 또 다른 더 복잡한 예제는 이러한 임시 레코드 타입이 SQL 문장 내에서 어떻게 재사용될 수 있는지 보여줍니다!

```sql
-- (c: bigint) 타입이 생성됨
SELECT count(*) c
FROM book

-- (..: integer, ..: integer) 타입이 생성되어 비교됨...
WHERE (author_id, language_id) IN (

  -- ... 또 다른 호환되는 (..: integer, ..: integer) 타입과
  SELECT a.id, a.language_id
  FROM author a
)
```

이 쿼리는 저자가 모국어로 쓴 책의 수를 셉니다. 위의 예제에서 투영된 레코드 타입은 조금 더 단순합니다. 하나의 컬럼만 포함합니다. 흥미로운 부분은 두 개의 호환되는 `(integer, integer)` 타입을 비교하는 행 값 표현식 IN 비교 술어입니다. SQL에서는 임시 레코드 타입을 타입 안전하게 생성하고 다른 임시 레코드 타입과 즉시 비교할 수 있습니다. 이러한 비교에서 컬럼 이름은 중요하지 않지만, 컬럼 인덱스(및 관련 컬럼 타입)는 중요합니다.

## 다양한 SQL 접근 API 비교

이전 예제들은 SQL이 레코드 차수, 컬럼 이름, 컬럼 인덱스, 컬럼 타입을 포함한 레코드 타입의 형식적 선언을 어떻게 허용하는지 보여줍니다. SQL은 이 부분에서 매우 표현력이 풍부하지만, SQL에 접근하는 많은 클라이언트 언어들은 덜 표현력이 풍부합니다. 표현력과 타입 안전성을 비교할 때, 두 가지 기능을 고려해야 합니다:

1. 클라이언트 언어로 생성된 레코드가 타입 안전한가?
2. 클라이언트 언어에서 생성된 SQL 문장이 타입 안전하고 구문 안전한가?

다양한 접근 기법들과 위의 타입 안전성 요구사항 측면에서 얼마나 표현력이 있는지 살펴보겠습니다:

### JDBC: 최소한의 타입 안전성

JDBC는 가장 적은 표현력과 타입 안전성을 제공합니다. JDBC가 매우 저수준 API이기 때문에 이는 놀라운 일이 아닙니다. JDBC는 다음을 제공합니다:

1. 결과 레코드에 접근할 때 타입 안전성이 전혀 없음
2. SQL 문장을 생성할 때 타입 안전성이나 구문 안전성이 전혀 없음

다음은 예제입니다:

```java
PreparedStatement stmt = null;
ResultSet rs = null;

try {

  // SQL 문장은 단순한 문자열입니다. 이를 구성하는 것은
  // 타입 안전하지도 구문 안전하지도 않습니다
  stmt = connection.prepareStatement(
    "SELECT id, title FROM book WHERE id = ?");

  // 바인드 값은 인덱스로 설정됩니다. 타입 안전성이나
  // "인덱스 안전성"이 없습니다
  stmt.setInt(1, 15);

  rs = stmt.executeQuery();
  while (rs.next()) {

    // 결과 레코드 값에 접근할 때 타입 안전성이나
    // "인덱스 안전성"이 없습니다
    System.out.println(
      "ID: " + rs.getInt(1) + ", TITLE: " + rs.getString(2));
  }
}
finally {
  closeSafely(stmt, rs);
}
```

이것은 놀라운 일이 아닙니다. JDBC는 타입 안전성의 부족을 절대적인 범용성으로 보완합니다. 실제로 어떤 종류의 SQL과 JDBC 기능을 지원하는지에 관계없이 모든 유형의 관계형 데이터베이스에 대해 JDBC 드라이버를 구현하는 것이 가능합니다.

### JPA: 약간의 타입 안전성

JPA는 주로 JPQL 위에, 그리고 SQL 위에도 약간의 타입 안전성을 구현했습니다. JPA를 사용하면 다음을 가질 수 있습니다:

1. 레코드에 접근할 때 약간의 타입 안전성
2. CriteriaQuery API를 통해 JPQL 문장을 생성할 때 약간의 타입 안전성과 구문 안전성 (SQL 문장이 아닌)

레코드 접근 타입 안전성은 문장의 결과를 JPA 어노테이션이 달린 엔티티에 투영할 때 보장될 수 있습니다. 매핑 자체는 실제로 타입 안전하지 않지만, Java 클래스가 SQL 레코드와 가장 가까운 매치이므로 결과는 타입 안전합니다. SQL 레코드처럼 Java 클래스도 다음을 가집니다:

- 속성의 수로 표현되는 차수
- 속성 이름으로 표현되는 컬럼 이름
- 속성 타입으로 표현되는 컬럼 타입
- 하지만: 컬럼 인덱스가 없습니다. 속성은 명시적인 순서가 없습니다

JPA 레코드 매핑은 SQL의 표현력을 초과하는 추가 기능을 가지고 있는데, "평면적인" 테이블 형식의 결과 집합을 객체 계층 구조에 매핑할 수 있습니다. 어떤 경우든, 이 타입 안전성의 이점을 얻으려면 쿼리당 하나의 레코드/엔티티 타입을 생성해야 합니다. 모든 테이블에서 모든 컬럼을 투영하지 않고 임시 레코드(함수에서 파생된 값 포함)를 투영하면, 이 타입 안전성을 다시 잃게 됩니다.

문장 타입 안전성의 경우, JPA는 타입 안전한 JPQL 문장을 생성하기 위한 CriteriaQuery API를 제공합니다. CriteriaQuery API는 장황함과 결과 클라이언트 코드가 읽기 어렵다는 점 때문에 종종 비판받습니다. 다음은 CriteriaQuery API 문서에서 가져온 예제입니다:

```java
CriteriaQuery<String> q = cb.createQuery(String.class);
Root<Order> order = q.from(Order.class);
q.select(order.get("shippingAddress").<String>get("state"));

CriteriaQuery<Product> q2 = cb.createQuery(Product.class);
q2.select(q2.from(Order.class)
            .join("items")
            .<Item,Product>join("product"));
```

위의 쿼리 구성에서 제한된 양의 타입 안전성만 있다는 것을 볼 수 있습니다:

- 컬럼은 `"shippingAddress"`와 같은 문자열 리터럴로 접근됩니다
- 제네릭 엔티티 타입은 실제로 검사되지 않습니다. `<Item,Product>` 제네릭 파라미터는 잘못되었을 수도 있습니다

물론, JPA의 CriteriaQuery API에는 더 타입 안전한 API 부분이 있습니다. 그러나 이러한 API 부분을 사용하면 다양한 Stack Overflow 토론과 Java EE 튜토리얼에서 볼 수 있듯이 앞서 언급한 장황함으로 빠르게 이어집니다.

### LINQ: 많은 타입 안전성 (.NET에서)

LINQ는 두 차원 모두에서 타입 안전성을 제공하는 데 매우 앞서 있습니다:

1. 레코드나 튜플에 접근할 때 많은 타입 안전성
2. LINQ-to-SQL 문장을 생성할 때 많은 타입 안전성 (SQL 문장이 아닌)

LINQ는 다양한 .NET 언어에 형식적으로 통합되어 있기 때문에, 대상 언어(예: C#)로 직접 형식적으로 정의된 레코드 타입을 생성할 수 있는 장점이 있습니다. 타입 안전한 레코드를 생성할 수 있을 뿐만 아니라, LINQ-to-SQL 문장도 컴파일러에 의해 형식적으로 검증됩니다. 예제:

```csharp
// 타입 안전한 이름 변경 (SQL에서 "AS"로 별칭 지정)
From p In db.Products
// 타입 안전한 (이름이 있는!) 변수 바인딩
Where p.UnitsInStock <= ReorderLevel AndAlso Not p.Discontinued
// 타입 안전한 투영이 Products 레코드를 생성합니다
Select p
```

또 다른 예제는 C# 튜플을 생성합니다:

```csharp
var r = from u in db.Users
        join s in db.Staffs on u.Id equals s.UserId
        select new Tuple<User, Staff>(u, s);

// 익명 레코드 타입 생성
var r = from u in db.Users
    select new { u.Name,
                 u.Address,
                 ...,
                 (from s in db.Staffs
                  select s.Password where u.Id == s.UserId)
               };
```

LINQ는 타입 안전성에 있어서 많은 명백한 장점이 있습니다. LINQ의 경우, 이것은 실제 SQL 표현력과 구문을 잃는 대가로 얻어집니다. LINQ-to-SQL은 실제로 SQL이 아니기 때문입니다(JPQL이 실제로 SQL이 아닌 것처럼). SQL 쿼리 API는 LINQ-to-Entities, LINQ-to-Collections, LINQ-to-XML과 같은 다른 이기종 쿼리 대상과 부분적으로 공유됩니다. 이것은 LINQ의 기능 범위를 줄입니다.

그러나 C#은 SQL 레코드가 제공하는 모든 타입 안전성 측면을 제공합니다: 차수, 컬럼 이름(익명 타입), 컬럼 인덱스(튜플), 컬럼 타입(타입과 튜플 모두).

### SLICK: 많은 타입 안전성 (Scala에서)

SLICK은 LINQ에서 영감을 받았으며, 따라서 많은 타입 안전성을 제공할 수 있습니다. SLICK은 다음을 제공합니다:

1. 튜플에 접근할 때 많은 타입 안전성 (레코드가 아닌)
2. SLICK 문장을 생성할 때 많은 타입 안전성 (SQL 문장이 아닌)

SLICK은 Scala의 통합된 튜플 표현식을 활용합니다. 이것은 예제로 가장 잘 보여줍니다:

```scala
// "for"는 DSL의 "진입점"입니다
val q = for {

    // FROM 절        WHERE 절
    c <- Coffees     if c.supID === 101

// SELECT 절과 튜플로의 투영
} yield (c.name, c.price)
```

위의 예제는 `(String, Int)` 튜플로의 투영이 `yield` 메서드에 의해 타입 안전하게 수행되는 것을 보여줍니다. 동시에, SLICK이 쿼리를 위한 내부 DSL을 도입하기 위해 Scala의 언어 기능을 많이 활용하므로, 전체 쿼리 표현식이 컴파일러에 의해 형식적으로 검증됩니다. LINQ보다 훨씬 더, SLICK은 더 이상 SQL을 연상시키지 않는 고유한 구문을 가지고 있습니다. 서브쿼리, 복잡한 조인, 그룹화 및 집계를 어떻게 표현할 수 있는지 명확하지 않습니다.

### jOOQ: 많은 타입 안전성

jOOQ는 주로 SQL 자체에서 영감을 받았으며 SQL이 제공하는 모든 기능을 수용합니다. 따라서 다음을 가집니다:

1. 레코드나 튜플에 접근할 때 많은 타입 안전성
2. SQL 문장을 생성할 때 많은 타입 안전성

jOOQ는 SQL 결과 집합을 레코드에 매핑하는 데 있어서 JPA와 유사한 기능을 제공하지만, JPA의 매핑 타입 계층 구조는 jOOQ에서 지원되지 않습니다. 그러나 jOOQ는 SLICK이 구현한 방식으로 타입 안전한 튜플 접근도 허용합니다. 임의의 쿼리 투영에 의해 생성된 임시 레코드는 제네릭 `Record1<T1>`, `Record2<T1, T2>`, `Record3<T1, T2, T3>`, ... 레코드 타입을 통해 다양한 컬럼 타입을 유지합니다. Java와 달리, 이것은 Scala에서 광범위하게 활용될 수 있으며, 이러한 타입 안전한 Record[N] 타입은 Scala의 튜플처럼 사용될 수 있습니다.

반면에, 쿼리를 .NET 언어에 일급 시민으로 형식적으로 통합한 LINQ-to-SQL처럼, jOOQ는 Java에서 SQL 문장을 작성할 때 강력한 타입 검사와 구문 검사를 허용합니다. SQL에서는 다음과 같은 것들을 타입 안전하게 작성할 수 있습니다:

```sql
SELECT * FROM t WHERE (t.a, t.b) = (1, 2)
SELECT * FROM t WHERE (t.a, t.b) OVERLAPS (date1, date2)
SELECT * FROM t WHERE (t.a, t.b) IN (SELECT x, y FROM t2)
UPDATE t SET (a, b) = (SELECT x, y FROM t2 WHERE ...)
INSERT INTO t (a, b) VALUES (1, 2)
```

jOOQ 3.0에서는 (또한 타입 안전하게!) 다음과 같이 작성할 수 있습니다:

```java
select().from(t).where(row(t.a, t.b).eq(1, 2));
// 여기서 타입 검사: ------------------->  ^^^^

select().from(t).where(row(t.a, t.b).overlaps(date1, date2));
// 여기서 타입 검사: -------------------------> ^^^^^^^^^^^^

select().from(t).where(row(t.a, t.b).in(select(t2.x, t2.y).from(t2)));
// 여기서 타입 검사: --------------------------> ^^^^^^^^^^

update(t).set(row(t.a, t.b), select(t2.x, t2.y).where(...));
// 여기서 타입 검사: ----------------> ^^^^^^^^^^

insertInto(t, t.a, t.b).values(1, 2);
// 여기서 타입 검사: ----------> ^^^^
```

이것은 행 값 표현식을 포함하지 않는 기존 API에도 적용됩니다:

```java
select().from(t).where(t.a.eq(select(t2.x).from(t2));
// 여기서 타입 검사: ----------------> ^^^^

select().from(t).where(t.a.eq(any(select(t2.x).from(t2)));
// 여기서 타입 검사: --------------------> ^^^^

select().from(t).where(t.a.in(select(t2.x).from(t2));
// 여기서 타입 검사: ----------------> ^^^^

select(t1.a, t1.b).from(t1).union(select(t2.a, t2.b).from(t2));
// 여기서 타입 검사: --------------------> ^^^^^^^^^^
```

jOOQ는 SQL이 아니지만, SQL을 Java, Scala, C#과 같은 호스트 언어에 내부 도메인 특화 언어로 도입하려는 다른 시도들과 달리, jOOQ는 기본 BNF 표기법을 비공식적으로 따르는 고유한 플루언트 API 기법 덕분에 SQL과 매우 유사하게 보입니다.

Java가 C#이나 Scala와 같은 다른 언어보다 표현력이 떨어지더라도, jOOQ는 아마도 Java 세계에서 결과 레코드 타입 안전성과 SQL 구문 안전성 모두에 가장 가깝게 다가갑니다.

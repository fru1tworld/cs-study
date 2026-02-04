# jOOQ의 Converter를 UNION 연산에서 사용하는 방법

> 원문: https://blog.jooq.org/how-to-use-jooqs-converters-with-union-operations/

jOOQ 3.15에서는 임시(ad-hoc) Converter 개념이 도입되었습니다. 이는 단일 쿼리에 "임시로" 적용되는 Converter입니다. 모든 쿼리에서 사용하기 위해 생성된 코드에 부착되는 일반적인 `Converter`와 동일한 기본 메커니즘을 사용합니다.

이러한 임시 Converter의 예시는 다음과 같습니다:

```java
// Converter 없이, BOOK.ID가 Field<Integer> 타입이라고 가정
Result<Record1<Integer>> result =
ctx.select(BOOK.ID)
   .from(BOOK)
   .fetch();

// Converter 사용 시
Result<Record1<Long>> result =
ctx.select(BOOK.ID.convertFrom(i -> i.longValue()))
   .from(BOOK)
   .fetch();
```

`CAST()`나 `COERCE()` 표현식을 사용하는 등 데이터 타입을 변환하는 다른 방법도 있지만, 이 접근 방식은 필드에 `Converter`를 부착하여 JDBC `ResultSet`에서 `Integer` 값을 읽은 직후에 호출하여 `Long`으로 변환합니다. 이 변환은 클라이언트 측에서 이루어집니다. 쿼리를 실행하는 RDBMS는 이 변환을 인식하지 못합니다.

이것은 중요한 세부 사항입니다! *RDBMS는 이를 인식하지 못합니다!*

## 주의 사항: UNION 사용 시

최근 이슈 트래커에서 이러한 임시 Converter를 `UNION`에서 사용하는 것에 관한 흥미로운 이슈(#14693)가 제기되었습니다. 예를 들어, 다음 쿼리를 실행한다고 가정해 보겠습니다:

```java
Result<Record1<Integer>> result =
ctx.select(BOOK.ID)
   .from(BOOK)
   .union(
    select(AUTHOR.ID)
   .from(AUTHOR))
   .fetch();
```

이 쿼리는 다음과 같은 결과를 생성할 수 있습니다:

| id  |
|-----|
| 1   |
| 2   |
| 3   |
| 4   |

사용 가능한 `BOOK.ID`가 `[1, 2, 3, 4]`이고 사용 가능한 `AUTHOR.ID`가 `[1, 2]`라고 가정하면, `UNION`은 중복을 제거합니다.

다음과 같이 두 번째 `UNION` 하위 쿼리에만 임시 Converter를 부착하면 어떤 일이 일어날까요?

```java
Result<Record1<Integer>> result =
ctx.select(BOOK.ID)
   .from(BOOK)
   .union(
    select(AUTHOR.ID.convertFrom(i -> -i))
   .from(AUTHOR))
   .fetch();
```

이 코드의 목적은 각 `AUTHOR.ID`의 음수 값을 가져오면서 `BOOK.ID`는 그대로 유지하는 것처럼 보입니다. 하지만 기억하세요:

- 변환은 서버가 아닌 클라이언트에서 발생하므로, RDBMS는 이를 인식하지 못합니다
- 이는 `UNION` 연산자에 아무런 영향을 미치지 않는다는 것을 의미합니다
- 게다가 jOOQ는 어떤 `UNION` 하위 쿼리가 어떤 행을 기여했는지 알 수 없으므로, Converter를 적용할지 여부를 결정하는 것이 불가능합니다!

그리고 이것이 실제로 발생하는 일입니다. 결과는 여전히 다음과 같습니다:

| id  |
|-----|
| 1   |
| 2   |
| 3   |
| 4   |

그리고 람다 `i -> -i`는 절대 호출되지 않습니다! 이것은 임시 Converter에만 해당하는 것이 아니라, 이러한 프로젝션된 컬럼에 부착하는 다른 모든 `Converter`(또는 `Binding`)에도 마찬가지입니다. jOOQ는 JDBC(또는 R2DBC) `ResultSet`에서 결과를 가져올 때 첫 번째 `UNION` 하위 쿼리의 행 타입만 고려합니다. 두 행 타입이 호환되어 Java 컴파일러가 쿼리의 타입 검사를 통과하도록 보장하기만 하면 됩니다.

## 해결 방법

이러한 상황에 대한 해결 방법은 실질적으로 2가지뿐입니다:

- 변환이 클라이언트 코드에서(서버가 아닌) 발생해야 한다고 확신하는 경우, 최소한 첫 번째 `UNION` 하위 쿼리에 이를 적용해야 합니다. 이상적으로는 일관성을 위해 모든 `UNION` 하위 쿼리에 적용하는 것이 좋으며, 하위 쿼리를 추출하여 재사용하는 경우에도 마찬가지입니다.
- 아마도 애초에 변환을 서버 측으로 옮겨야 했을 수도 있습니다

후자의 경우, 음수 `AUTHOR.ID` 값을 생성하려는 의도였다면 다음 쿼리가 더 적합할 수 있습니다:

```java
Result<Record1<Integer>> result =
ctx.select(BOOK.ID)
   .from(BOOK)
   .union(
    select(AUTHOR.ID.neg())
   .from(AUTHOR))
   .fetch();
```

이제 다음과 같은 SQL 쿼리가 생성됩니다:

```sql
SELECT book.id
FROM book
UNION
SELECT -author.id
FROM author
```

그리고 다음과 같은 결과 집합이 생성됩니다:

| id  |
|-----|
| -2  |
| -1  |
| 1   |
| 2   |
| 3   |
| 4   |

특히 `MULTISET`과 함께 임시 Converter를 사용할 때 이 점을 반드시 기억하세요!

# 행 값 표현식과 BETWEEN 조건자

> 원문: https://blog.jooq.org/row-value-expressions-and-the-between-predicate/

이 글에서는 SQL 절 시뮬레이션의 어려움을 살펴봅니다. 특히 BETWEEN 조건자와 그에 대한 동등한 변환에 초점을 맞춥니다. 이 글은 고급 SQL 기능들이 데이터베이스마다 일관되지 않은 지원을 받는 상황을 검토합니다.

## BETWEEN 조건자

BETWEEN 조건자는 하나의 표현식 A가 다른 두 표현식 B와 C 사이에 있어야 한다는 사실을 표현하는 편리한 방법을 제공합니다. 이 기능은 SQL-1992 표준의 §8.4 섹션에서 정의되었고, SQL-1999에서 ASYMMETRIC/SYMMETRIC 키워드가 추가되면서 개선되었습니다.

SQL-1999의 공식 문법은 다음과 같습니다:

```sql
<between predicate> ::=
    <row value expression> [ NOT ] BETWEEN
      [ ASYMMETRIC | SYMMETRIC ]
      <row value expression> AND <row value expression>
```

ASYMMETRIC은 기본 동작을 장황하게 표현하는 방식일 뿐입니다. 반면 SYMMETRIC은 B와 C의 순서가 상관없다는 것을 나타내는 유용한 속성을 가지고 있습니다.

## 동등한 변환

다음 문장들은 동등합니다:

```sql
A BETWEEN SYMMETRIC B AND C
```

이는 다음과 같이 확장됩니다:

```sql
(A BETWEEN B AND C) OR (A BETWEEN C AND B)
```

그리고 이것은 다시 다음과 같이 확장됩니다:

```sql
(A >= B AND A <= C) OR (A >= C AND A <= B)
```

## 행 값 표현식을 사용한 변환

행 값 표현식(Row Value Expressions, RVE)을 사용하면 여러 값을 동시에 비교할 수 있습니다. 행 값 표현식을 사용하면 변환이 상당히 더 복잡해집니다.

원본 문장:

```sql
(A1, A2) BETWEEN SYMMETRIC (B1, B2) AND (C1, C2)
```

### 1단계: BETWEEN SYMMETRIC 확장

행 값에 적용하면:

```sql
((A1, A2) >= (B1, B2) AND (A1, A2) <= (C1, C2))
OR
((A1, A2) >= (C1, C2) AND (A1, A2) <= (B1, B2))
```

### 2단계: 완전한 확장 (행 값 표현식 제거)

행 값 표현식을 제거하고 완전히 확장하면 다음과 같습니다:

```sql
(     ((A1 > B1) OR (A1 = B1 AND A2 > B2) OR (A1 = B1 AND A2 = B2))
  AND ((A1 < C1) OR (A1 = C1 AND A2 < C2) OR (A1 = C1 AND A2 = C2)) )
OR (  ((A1 > C1) OR (A1 = C1 AND A2 > C2) OR (A1 = C1 AND A2 = C2))
  AND ((A1 < B1) OR (A1 = B1 AND A2 < B2) OR (A1 = B1 AND A2 = B2)) )
```

이것이 바로 행 값 표현식을 사용한 BETWEEN [SYMMETRIC] 조건자가 실제로 하는 일입니다.

## 데이터베이스 지원 비교표

다음은 14개 SQL 방언에 대한 지원 매트릭스입니다:

| 데이터베이스 | BETWEEN SYMMETRIC | RVE = RVE | RVE 순서 비교 | RVE BETWEEN |
|------------|-------------------|-----------|--------------|-------------|
| CUBRID | 아니오 | 예 | 아니오 | 아니오 |
| DB2 | 아니오 | 예 | 예 | 예 |
| Derby | 아니오 | 아니오 | 아니오 | 아니오 |
| Firebird | 아니오 | 아니오 | 아니오 | 아니오 |
| H2 | 아니오 | 예 | 예 | 예 |
| HSQLDB | 예 | 예 | 예 | 예 |
| Ingres | 예 | 아니오 | 아니오 | 아니오 |
| MySQL | 아니오 | 예 | 예 | 아니오 |
| Oracle | 아니오 | 예 | 아니오 | 아니오 |
| PostgreSQL | 예 | 예 | 예 | 예 |
| SQL Server | 아니오 | 아니오 | 아니오 | 아니오 |
| SQLite | 아니오 | 아니오 | 아니오 | 아니오 |
| Sybase ASE | 아니오 | 아니오 | 아니오 | 아니오 |
| Sybase SQL Anywhere | 아니오 | 아니오 | 아니오 | 아니오 |

참고:
- CUBRID는 실제로 행 값 표현식을 지원하지 않습니다. 대신 SET 구조를 사용합니다.
- H2는 실제로 행 값 표현식을 지원하지 않습니다. 대신 ARRAY 구조를 사용합니다.

## 핵심 발견

테스트된 데이터베이스 중에서 HSQLDB와 PostgreSQL만이 네 가지 기능을 모두 네이티브로 지원합니다. Derby, Firebird, SQL Server, SQLite, Sybase ASE, Sybase SQL Anywhere와 같은 다른 데이터베이스들은 이러한 기능을 전혀 지원하지 않습니다.

## 결론

이 글은 데이터베이스 엔진이 고급 SQL 조건자를 네이티브로 지원하지 않을 때 jOOQ가 왜 복잡한 절 시뮬레이션을 처리해야 하는지를 보여줍니다. 이는 개발자들이 이기종 데이터베이스 환경에서 직면하는 실질적인 어려움을 강조합니다. jOOQ는 이러한 지원되지 않는 절들을 다양한 데이터베이스 플랫폼에서 시뮬레이션하여 처리합니다.

# 고급 SQL: jOOQ에서의 관계 나눗셈

> 원문: https://blog.jooq.org/advanced-sql-relational-division-in-jooq/

게시일: 2012년 3월 30일
작성자: lukaseder

## 관계 나눗셈이란 무엇인가?

관계 나눗셈(relational division)은 크로스 조인(cross join) 연산의 역연산입니다. 다음은 관계 나눗셈의 대략적인 정의입니다:

다음과 같은 크로스 조인 / 카르테시안 곱(cartesian product)이 있다고 가정합니다:

```
C = A × B
```

그러면 다음과 같이 말할 수 있습니다:

```
A = C ÷ B
B = C ÷ A
```

## 일반적으로 이것은 무엇을 의미하는가?

위키피디아에서 제공하는 예제를 살펴봅시다:

Completed ÷ DBProject의 나눗셈은 모든 프로젝트를 완료한 학생들의 목록을 산출합니다.

## 그렇다면 이것을 SQL로 어떻게 표현할 수 있을까??

보이는 것만큼 간단하지 않습니다. 가장 일반적으로 문서화된 해법은 안티 조인(anti-join)을 사용하는 이중 중첩 SELECT 문을 포함합니다. 인간의 언어로 표현하면(이중 부정을 사용하여), Fred와 Sarah가 "우리가 _완료하지 않은(Completed)_ _DB 프로젝트(DBProject)_ 는 존재하지 않는다"라고 말하는 것과 같습니다. SQL로 표현하면 다음과 같습니다:

```sql
SELECT DISTINCT "c1".Student FROM Completed "c1"
WHERE NOT EXISTS (
  SELECT 1 FROM DBProject
  WHERE NOT EXISTS (
    SELECT 1 FROM Completed "c2"
    WHERE "c2".Student = "c1".Student
    AND "c2".Task = DBProject.Task
  )
)
```

이제, 정신이 온전한 사람이라면 SQL 개발자의 일생에서 실제로 필요한 1~2번의 경우를 위해 이것을 기억하고 싶지는 않을 것입니다. 그래서 jOOQ를 사용합니다. jOOQ는 위의 괴물 같은 구문을 간결한 문법으로 감싸줍니다:

```java
create.select().from(
  Completed.divideBy(DBProject)
           .on(Completed.Task.equal(DBProject.Task))
           .returning(Completed.Student)
);
```

위의 SQL 문에서 적절한 인덱싱이 매우 중요하다는 것은 즉시 알 수 있습니다. `on(…)` 및 `returning(…)` 절에서 참조하는 모든 컬럼에 인덱스가 있는지 반드시 확인하세요.

## 추가 정보

관계 나눗셈에 대한 더 많은 정보와 실제 사례를 보려면 다음을 참고하세요:

- [https://en.wikipedia.org/wiki/Relational_algebra#Division](https://en.wikipedia.org/wiki/Relational_algebra#Division)
- [http://www.simple-talk.com/sql/t-sql-programming/divided-we-stand-the-sql-of-relational-division/](http://www.simple-talk.com/sql/t-sql-programming/divided-we-stand-the-sql-of-relational-division/)

# jOOQ와 JavaFX를 사용하여 SQL 데이터를 차트로 변환하기

> 원문: https://blog.jooq.org/transform-your-sql-data-into-charts-using-jooq-and-javafx/

게시일: 2015년 1월 8일 (2014년 12월 28일)
저자: lukaseder

## 소개

이 글에서는 Java 8 함수형 프로그래밍을 jOOQ 및 JavaFX와 결합하여 SQL 데이터를 시각적인 막대 차트로 변환하는 방법을 보여줍니다. 이 글은 jOOQ와 Java 8 람다/스트림을 사용한 함수형 데이터 변환에 대한 이전 논의를 기반으로 합니다.

## 데이터베이스 설정

이 예제에서는 PostgreSQL에서 세계은행 공개 데이터를 사용하며, 다음과 같은 스키마를 가집니다:

```sql
DROP SCHEMA IF EXISTS world;

CREATE SCHEMA world;

CREATE TABLE world.countries (
  code CHAR(2) NOT NULL,
  year INT NOT NULL,
  gdp_per_capita DECIMAL(10, 2) NOT NULL,
  govt_debt DECIMAL(10, 2) NOT NULL
);

INSERT INTO world.countries
VALUES ('CA', 2009, 40764, 51.3),
       ('CA', 2010, 47465, 51.4),
       ('CA', 2011, 51791, 52.5),
       ('CA', 2012, 52409, 53.5),
       ('DE', 2009, 40270, 47.6),
       ('DE', 2010, 40408, 55.5),
       ('DE', 2011, 44355, 55.1),
       ('DE', 2012, 42598, 56.9),
       ('FR', 2009, 40488, 85.0),
       ('FR', 2010, 39448, 89.2),
       ('FR', 2011, 42578, 93.2),
       ('FR', 2012, 39759,103.8),
       ('GB', 2009, 35455,121.3),
       ('GB', 2010, 36573, 85.2),
       ('GB', 2011, 38927, 99.6),
       ('GB', 2012, 38649,103.2),
       ('IT', 2009, 35724,121.3),
       ('IT', 2010, 34673,119.9),
       ('IT', 2011, 36988,113.0),
       ('IT', 2012, 33814,131.1),
       ('JP', 2009, 39473,166.8),
       ('JP', 2010, 43118,174.8),
       ('JP', 2011, 46204,189.5),
       ('JP', 2012, 46548,196.5),
       ('RU', 2009,  8616,  8.7),
       ('RU', 2010, 10710,  9.1),
       ('RU', 2011, 13324,  9.3),
       ('RU', 2012, 14091,  9.4),
       ('US', 2009, 46999, 76.3),
       ('US', 2010, 48358, 85.6),
       ('US', 2011, 49855, 90.1),
       ('US', 2012, 51755, 93.8);
```

## 목표

다음과 같은 두 개의 막대 차트를 생성합니다:
- 각 국가의 1인당 GDP (2009-2012)
- 각 국가의 GDP 대비 정부 부채 비율 (2009-2012)

시리즈는 평균 예측 값을 기준으로 정렬되어 쉽게 비교할 수 있어야 합니다.

## SQL 쿼리 (전통적인 접근 방식)

```sql
select
    COUNTRIES.YEAR,
    COUNTRIES.CODE,
    COUNTRIES.GOVT_DEBT
from
    COUNTRIES
join (
    select
        COUNTRIES.CODE,
        avg(COUNTRIES.GOVT_DEBT) avg
    from
        COUNTRIES
    group by
        COUNTRIES.CODE
) c1
on COUNTRIES.CODE = c1.CODE
order by
    avg asc,
    COUNTRIES.CODE asc,
    COUNTRIES.YEAR asc
```

## jOOQ와 JavaFX를 사용한 Java 구현

```java
CategoryAxis xAxis = new CategoryAxis();
NumberAxis yAxis = new NumberAxis();
xAxis.setLabel("Country");
yAxis.setLabel("% of GDP");

BarChart<String, Number> bc =
    new BarChart<>(xAxis, yAxis);
bc.setTitle("Government Debt");
bc.getData().addAll(

    // SQL 데이터 변환, 데이터베이스에서 실행됨
    // -------------------------------------------
    DSL.using(connection)
       .select(
           COUNTRIES.YEAR,
           COUNTRIES.CODE,
           COUNTRIES.GOVT_DEBT)
       .from(COUNTRIES)
       .join(
           table(
               select(
                   COUNTRIES.CODE,
                   avg(COUNTRIES.GOVT_DEBT).as("avg"))
               .from(COUNTRIES)
               .groupBy(COUNTRIES.CODE)
           ).as("c1")
       )
       .on(COUNTRIES.CODE.eq(
           field(
               name("c1", COUNTRIES.CODE.getName()),
               String.class
           )
       ))

       // 평균 예측 값을 기준으로 국가를 정렬
       .orderBy(
           field(name("avg")),
           COUNTRIES.CODE,
           COUNTRIES.YEAR)

       // 위 문장에서 생성된 결과는
       // 다음과 같습니다:
       // +----+----+---------+
       // |year|code|govt_debt|
       // +----+----+---------+
       // |2009|RU  |     8.70|
       // |2010|RU  |     9.10|
       // |2011|RU  |     9.30|
       // |2012|RU  |     9.40|
       // |2009|CA  |    51.30|
       // +----+----+---------+

    // Java 데이터 변환, 애플리케이션 메모리에서 실행됨
    // ------------------------------------------------

       // 정렬 순서를 유지하면서 연도별로 결과를 그룹화
       .fetchGroups(COUNTRIES.YEAR)

       // 이것의 제네릭 타입은 추론됩니다...
       // Stream<Entry<Integer, Result<
       //     Record3<BigDecimal, String, Integer>>
       // >>
       .entrySet()
       .stream()

       // 엔트리를 { 연도 -> 예측 값 }으로 매핑
       .map(entry -> new XYChart.Series<>(
           entry.getKey().toString(),
           observableArrayList(

           // 레코드를 차트 데이터로 매핑
           entry.getValue().map(country ->
                new XYChart.Data<String, Number>(
                  country.getValue(COUNTRIES.CODE),
                  country.getValue(COUNTRIES.GOVT_DEBT)
           ))
           )
       ))
       .collect(toList())
);
```

## SQL과 Java의 분리

이 접근 방식은 SQL과 Java 변환을 깔끔하게 분리합니다. SQL 쿼리는 먼저 데이터베이스에서 실행되어 최적의 쿼리 계획을 수립할 수 있게 합니다. 데이터가 구체화된 후에야 Java 8 스트림 변환이 실행됩니다.

## 대안: 윈도우 함수 접근 방식

쿼리는 SQL-1999 윈도우 함수를 사용하여 더 나은 효율성을 위해 리팩토링할 수 있습니다:

```java
DSL.using(connection)
   .select(
       COUNTRIES.YEAR,
       COUNTRIES.CODE,
       COUNTRIES.GOVT_DEBT)
   .from(COUNTRIES)
   .orderBy(
       avg(COUNTRIES.GOVT_DEBT)
           .over(partitionBy(COUNTRIES.CODE)),
       COUNTRIES.CODE,
       COUNTRIES.YEAR)
   ;
```

또는 SQL로:

```sql
select
    COUNTRIES.YEAR,
    COUNTRIES.CODE,
    COUNTRIES.GOVT_DEBT
from
    COUNTRIES
order by
    avg(COUNTRIES.GOVT_DEBT)
        over (partition by COUNTRIES.CODE),
    COUNTRIES.CODE,
    COUNTRIES.YEAR
```

윈도우 함수 접근 방식은 중첩된 SELECT를 더 효율적인 정렬 로직으로 대체합니다.

## 댓글

Robin Archer (2015년 1월 8일): "와, 당신의 블로그 읽는 것을 정말 좋아합니다. 정말 멋지네요."

Dardoneli (2015년 4월 15일): 처음에는 예제 실행에 문제가 있다고 보고했지만 후속 댓글에서 해결되었습니다.

lukaseder의 응답: 의존성 문제를 해결하고 수정 사항이 문제를 해결했음을 확인했습니다.

## 다운로드

전체 예제는 Maven으로 다운로드하고 실행할 수 있습니다:
`https://github.com/jOOQ/jOOQ/tree/master/jOOQ-examples/jOOQ-javafx-example`

명령어: `mvn clean install`

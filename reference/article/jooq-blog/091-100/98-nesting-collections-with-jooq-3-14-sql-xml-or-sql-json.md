# jOOQ 3.14의 SQL/XML 또는 SQL/JSON 지원으로 컬렉션 중첩하기

> 원문: https://blog.jooq.org/nesting-collections-with-jooq-3-14s-sql-xml-or-sql-json-support/

게시일: 2020년 10월 9일, Lukas Eder

## 개요

이 글에서는 jOOQ 3.14가 SQL/XML 및 SQL/JSON 기능을 사용하여 중첩된 데이터베이스 레코드를 계층적 Java 구조로 매핑하는 방법을 보여주며, 미들웨어 매핑 로직의 필요성을 제거합니다.

## 단순 평면 매핑

기본은 jOOQ의 `DefaultRecordMapper`를 사용한 기본적인 레코드-객체 매핑입니다:

```java
class Column {
    String tableSchema;
    String tableName;
    String columnName;
}

for (Column c :
    ctx.select(
            COLUMNS.TABLE_SCHEMA,
            COLUMNS.TABLE_NAME,
            COLUMNS.COLUMN_NAME)
       .from(COLUMNS)
       .where(COLUMNS.TABLE_NAME.eq("t_author"))
       .orderBy(COLUMNS.ORDINAL_POSITION)
       .fetchInto(Column.class))
    System.out.println(
        c.tableSchema + "." + c.tableName + "." + c.columnName
    );
```

## 점 표기법을 통한 중첩 매핑

jOOQ는 점 표기법을 사용한 컬럼 별칭을 통해 중첩된 Java 클래스 구조를 생성할 수 있습니다. 예를 들어:

```java
class Type {
    String name;
    int precision;
    int scale;
    int length;
}

class Column {
    String tableSchema;
    String tableName;
    String columnName;
    Type type;
}
```

쿼리에서 `"type.name"`과 같은 별칭을 사용하면 중첩된 객체가 자동으로 채워집니다.

## JSON 기반 중첩 컬렉션

버전 3.14부터 개발자는 SQL/JSON 함수를 활용하여 결과에 중첩된 컬렉션을 생성할 수 있습니다:

```java
for (Record1<JSON> record :
    ctx.select(
            jsonObject(
                key("tableSchema").value(COLUMNS.TABLE_SCHEMA),
                key("tableName").value(COLUMNS.TABLE_NAME),
                key("columns").value(jsonArrayAgg(
                    jsonObject(
                        key("columnName").value(COLUMNS.COLUMN_NAME),
                        key("type").value(jsonObject(
                            "name", COLUMNS.DATA_TYPE)
                        )
                    )
                ).orderBy(COLUMNS.ORDINAL_POSITION))
            )
       )
       .from(COLUMNS)
       .where(COLUMNS.TABLE_NAME.in("t_author", "t_book"))
       .groupBy(COLUMNS.TABLE_SCHEMA, COLUMNS.TABLE_NAME)
       .orderBy(COLUMNS.TABLE_SCHEMA, COLUMNS.TABLE_NAME)
       .fetch())
    System.out.println(record.value1());
```

이렇게 하면 중첩된 배열이 포함된 JSON 문서가 생성됩니다.

## JSON에서 Java 객체로 직접 매핑

gson, Jackson 또는 JAXB가 사용 가능한 경우, 동일한 쿼리로 복잡한 Java 구조를 직접 채울 수 있습니다:

```java
public static class Type {
    public String name;
}

public static class Column {
    public String columnName;
    public Type type;
}

public static class Table {
    public String tableSchema;
    public String tableName;
    public List<Column> columns;
}

for (Table t :
    ctx.select(...)
       .fetchInto(Table.class))
    System.out.println(t.tableName + ":\n" + t.columns
       .stream()
       .map(c -> c.columnName + " (" + c.type.name + ")")
       .collect(joining("\n  ")));
```

## 데이터베이스 네이티브 접근 방식

이 접근 방식은 SQL의 네이티브 집계 함수를 사용하여 조인 복잡성과 카테시안 곱을 제거합니다. SQL Server의 `FOR JSON`, Oracle의 `JSON_OBJECT()`, PostgreSQL의 `json_build_object()`는 유사한 개념의 벤더별 구현을 제공합니다.

## 주요 이점

- 일대다 관계에서 카테시안 곱 없음
- 데이터베이스 계층에서 직접 JSON/XML 생성
- 표준 `fetchInto()` 호출을 통한 단순화된 매핑
- jOOQ의 변환 기능을 통한 벤더 독립적
- 임의의 중첩 레벨 지원

이 기능은 근본적인 ORM 과제를 해결합니다: 미들웨어 코드에서 성능 병목 현상을 만들지 않으면서 관계형 데이터의 계층적 특성을 표현하는 것입니다.

# Groovy와 함께 jOOQ 사용하기

> 원문: https://blog.jooq.org/using-jooq-with-groovy/

## 소개

이 글에서는 스크립팅 목적으로 jOOQ를 Groovy와 함께 사용하는 방법에 대해 논의합니다. 기존의 jOOQ/Scala 통합과 비교하면서, 특정 Groovy 언어 기능을 jOOQ와 함께 어떻게 활용할 수 있는지 보여드립니다.

## 코드 예제

다음은 주요 예제입니다:

```groovy
package org.jooq.groovy

import static org.jooq.impl.DSL.*
import static org.jooq.groovy.example.h2.Tables.*

import groovy.sql.Sql
import org.jooq.*
import org.jooq.impl.DSL

sql = Sql.newInstance(
    'jdbc:h2:~/scala-test',
    'sa', '', 'org.h2.Driver')

a = T_AUTHOR.as("a")
b = T_BOOK.as("b")

DSL.using(sql.connection)
   .select(a.FIRST_NAME, a.LAST_NAME, b.TITLE)
   .from(a)
   .join(b).on(a.ID.eq(b.AUTHOR_ID))
   .fetchInto ({
       r -> println(
           "${r.getValue(a.FIRST_NAME)} " +
           "${r.getValue(a.LAST_NAME)} " +
           "has written ${r.getValue(b.TITLE)}"
       )
   } as RecordHandler)
```

## 주요 관찰 사항

Groovy의 타입 안전성은 Java나 Scala에 비해 제한적입니다. "Groovy는 그다지 타입 안전한 언어가 아닙니다"라고 말할 수 있으며, `fetchInto()`와 같은 jOOQ 메서드에 클로저를 전달할 때 명시적인 타입 변환이 필요합니다.

타입 추론 문제로 인해 개발자는 RecordHandler 컨텍스트 내에서 잘 알려진 타입인 `Record3<String, String, String>`에 접근할 수 없습니다.

jOOQ와 Groovy 사용에 대한 결론: "jOOQ를 Groovy와 함께 사용하는 것은 분명히 가능하지만, Scala나 Java 8과 함께 사용하는 것만큼 강력하지는 않습니다."

## Groovy에서의 대안적 SQL 접근 방식

### Groovy의 기본 SQL 지원

Groovy의 내장 SQL 기능을 대안으로 제시합니다:

```groovy
import groovy.sql.Sql

sql = Sql.newInstance(
    'jdbc:h2:~/scala-test',
    'sa', '', 'org.h2.Driver')
sql.eachRow('select * from t_author') {
    println "${it.first_name} ${it.last_name}"
}
```

이것은 "JDBC를 직접 사용하는 것보다 훨씬 더 편리한 문자열 기반 접근 방식"이며, "Groovy SQL은 JDBC가 처음부터 이렇게 구현되었어야 하는 방식"이라고 할 수 있습니다.

### Groovy DSL 접근 방식

Ilya Sterin의 SQL 생성을 위한 내부 DSL 구현을 참조합니다:

```groovy
Select select = sql.select ("table1") {
    join("table2", type: "INNER") {
        using(table1: "col1", table2: "col1")
    }
    join("table3", type: "OUTER") {
        using(table1: "col2", table2: "col2")
        using(table1: "col3", table2: "col3")
    }
    where("table1.col1 = 'test'")
    groupBy(table1: "col1", table2: "col1")
    orderBy(table1: "col1", table2: "col1")
}
```

이 예제는 Groovy SQL 빌더 패턴에 관한 Sterin의 ilyasterin.com 블로그 게시물을 참조합니다.

## 태그

DSL, Groovy, Groovy SQL, Ilya Sterin, Java, jOOQ, SQL API, SQL builder, SQL DSL, SQL Query Building

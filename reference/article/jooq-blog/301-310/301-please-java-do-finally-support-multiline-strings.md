# Java여, 제발 드디어 여러 줄 문자열 리터럴을 지원해다오

> 원문: https://blog.jooq.org/please-java-do-finally-support-multiline-strings/

Java에서 외부 언어(SQL, XML, JSON, 정규표현식)를 삽입할 때마다 우리는 꽤 끔찍한 코드를 작성해야 합니다.

```java
try (PreparedStatement s = connection.prepareStatement(
    "SELECT * " +
    "FROM my_table " +
    "WHERE a = b"
)) {
    ...
}
```

이 방식에는 여러 가지 문제가 있습니다:

1. 문법 오류: 각 줄 끝에 공백을 추가하는 것을 잊지 마세요! 그렇지 않으면 SQL은 `SELECT *FROM my_tableWHERE a = b`가 되어버립니다.

2. 언어 스타일 충돌: Java에서 보기 좋게 포맷된 코드가 SQL을 소비하는 서버 입장에서는 형편없이 포맷된 것처럼 보입니다. 또한 이런 SQL을 복사해서 데이터베이스 도구에서 테스트하려면 먼저 모든 문자열 연결 부분을 정리해야 합니다.

3. SQL 인젝션 위험: 문자열 연결은 경험이 적은 개발자들로 하여금 안전하지 않은 방식으로 코드를 작성하도록 유혹합니다. 변수를 쉽게 연결할 수 있기 때문에, 바인드 변수 대신 문자열 연결을 사용하는 실수를 저지르기 쉽습니다.

## 제안하는 해결책

Xtend와 같은 언어에서 채택한 삼중 따옴표 문법을 도입하면 어떨까요?

```java
try (PreparedStatement s = connection.prepareStatement(
    '''SELECT *
       FROM my_table
       WHERE a = b'''
)) {
    ...
}
```

훨씬 보기 좋지 않나요? 이제 SQL이 Java 코드 안에서도 SQL처럼 보입니다. 데이터베이스 도구에서 실행할 SQL을 복사해서 바로 붙여넣을 수 있고, 그 반대도 가능합니다.

## 추가적인 이점들

### 이스케이프 문자 문제 해결

삼중 따옴표 문자열은 정규표현식에서 특히 유용합니다. 더 이상 백슬래시를 이중으로 이스케이프할 필요가 없습니다!

현재 Java에서 정규표현식을 작성하면:

```java
// 이메일 패턴을 매칭하는 현재 방식
String pattern = "\\w+@\\w+\\.\\w+";
```

삼중 따옴표를 사용하면:

```java
// 훨씬 읽기 쉬운 정규표현식
String pattern = '''\w+@\w+\.\w+''';
```

### 문자열 보간(String Interpolation)

PHP와 다른 언어들처럼, 문자열 보간 기능도 함께 제공되면 좋겠습니다:

```java
String tableName = "my_table";
String column = "a";

String sql = '''SELECT *
                FROM ${tableName}
                WHERE ${column} = b''';
```

물론 SQL의 경우 바인드 변수를 사용해야 하지만, XML이나 JSON을 생성할 때는 이런 문자열 보간이 매우 유용할 것입니다.

## 다른 언어들은 이미 지원합니다

Scala, Kotlin, Groovy, Ceylon, C# 등 많은 현대 언어들이 이미 여러 줄 문자열 리터럴을 지원하고 있습니다. Kotlin과 Scala는 여러 줄 문자열과 문자열 보간 기능을 모두 제공합니다.

```kotlin
// Kotlin 예제
val sql = """
    SELECT *
    FROM my_table
    WHERE a = b
""".trimIndent()
```

```scala
// Scala 예제
val sql = s"""
    |SELECT *
    |FROM $tableName
    |WHERE a = b
""".stripMargin
```

## 결론

여러 줄 문자열 리터럴은 작지만 매우 효과적인 개선입니다. 언어의 복잡성을 크게 증가시키지 않으면서도 Java 커뮤니티 전반에 걸친 광범위한 고충을 해결할 수 있습니다.

Java여, 제발 드디어 여러 줄 문자열 리터럴을 지원해 주세요!

---

참고: 이 글이 작성된 2015년 이후, Java 15(2020년 9월)에서 텍스트 블록(Text Blocks)이 정식 기능으로 도입되었습니다. 삼중 따옴표(`"""`)를 사용하여 여러 줄 문자열을 작성할 수 있게 되었습니다:

```java
String sql = """
    SELECT *
    FROM my_table
    WHERE a = b
    """;
```

저자의 바람이 마침내 실현된 것입니다!

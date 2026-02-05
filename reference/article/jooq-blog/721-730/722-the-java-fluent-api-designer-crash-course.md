# Java Fluent API 디자이너 속성 과정

> 원문: https://blog.jooq.org/the-java-fluent-api-designer-crash-course/

Martin Fowler가 Fluent 인터페이스에 대해 이야기한 이후로, 사람들은 모든 곳에서 메서드를 체이닝하기 시작했고, 가능한 모든 사용 사례에 대해 Fluent API(또는 DSL)를 만들기 시작했습니다. 원칙적으로, 거의 모든 유형의 DSL을 Java로 매핑할 수 있습니다. 이것이 어떻게 이루어질 수 있는지 살펴보겠습니다.

## DSL 규칙

DSL(Domain Specific Languages, 도메인 특화 언어)은 보통 대략 다음과 같은 규칙들로 구성됩니다:

1. SINGLE-WORD (단일 단어)
2. PARAMETERISED-WORD parameter (매개변수가 있는 단어)
3. WORD1 [ OPTIONAL-WORD ] (선택적 단어)
4. WORD2 { WORD-CHOICE-A | WORD-CHOICE-B } (선택지)
5. WORD3 [ , WORD3 ... ] (반복)

또는 다음과 같이 문법을 선언할 수도 있습니다 (이 멋진 Railroad Diagrams 사이트에서 지원하는 방식입니다):

```
Grammar ::= (
  'SINGLE-WORD' |
  'PARAMETERISED-WORD' '([A-Z]+)' |
  'WORD1' 'OPTIONAL-WORD'? |
  'WORD2' ( 'WORD-CHOICE-A' | 'WORD-CHOICE-B' ) |
  'WORD3'+
)
```

말로 풀어 설명하자면, 시작 조건 또는 상태가 있고, 거기서 언어의 몇몇 단어들을 선택한 후 종료 조건 또는 상태에 도달하게 됩니다. 이것은 상태 머신(state-machine)과 같으며, 따라서 다음과 같은 그림으로 표현할 수 있습니다:

(Railroad Diagrams 도구로 만든 간단한 문법 다이어그램)

## 이 규칙들의 Java 구현

Java 인터페이스를 사용하면 위의 DSL을 모델링하는 것이 꽤 간단합니다. 본질적으로 다음의 변환 규칙을 따르면 됩니다:

- 모든 DSL "키워드"는 Java 메서드가 됩니다
- 모든 DSL "연결"은 인터페이스가 됩니다
- "필수" 선택(다음 키워드를 건너뛸 수 없는 경우)이 있을 때, 해당 선택의 모든 키워드는 현재 인터페이스의 메서드가 됩니다. 하나의 키워드만 가능한 경우에는 하나의 메서드만 있습니다
- "선택적" 키워드가 있을 때, 현재 인터페이스는 다음 인터페이스를 확장(extends)합니다 (다음 인터페이스의 모든 키워드/메서드 포함)
- 키워드의 "반복"이 있을 때, 반복 가능한 키워드를 나타내는 메서드는 다음 인터페이스 대신 자기 자신의 인터페이스를 반환합니다
- 모든 DSL 하위 정의는 매개변수가 됩니다. 이를 통해 재귀가 가능해집니다

참고로, 위의 DSL을 인터페이스 대신 클래스로 모델링하는 것도 가능합니다. 하지만 유사한 키워드를 재사용하고 싶은 경우, 메서드의 다중 상속이 매우 유용할 수 있으므로 인터페이스를 사용하는 것이 더 나을 수 있습니다.

이러한 규칙들을 설정하면, jOOQ처럼 임의의 복잡도를 가진 DSL을 만들기 위해 마음대로 규칙을 반복할 수 있습니다. 물론 모든 인터페이스를 어떻게든 구현해야 하지만, 그것은 또 다른 이야기입니다.

위 규칙들이 Java로 번역되는 방법은 다음과 같습니다:

```java
// 초기 인터페이스, DSL의 진입점
// DSL의 특성에 따라 이것은 정적 메서드를 가진 클래스가 될 수도 있으며,
// 정적 임포트하면 DSL을 더욱 유창하게 만들 수 있습니다
interface Start {
  End singleWord();
  End parameterisedWord(String parameter);
  Intermediate1 word1();
  Intermediate2 word2();
  Intermediate3 word3();
}

// 종료 인터페이스, execute()와 같은 메서드를 포함할 수도 있습니다
interface End {
  void end();
}

// optionalWord()가 반환하는 인터페이스를 확장하는 중간 DSL "단계"로,
// 해당 메서드를 "선택적"으로 만듭니다
interface Intermediate1 extends End {
  End optionalWord();
}

// 여러 선택지를 제공하는 중간 DSL "단계" (Start와 유사)
interface Intermediate2 {
  End wordChoiceA();
  End wordChoiceB();
}

// word3()에서 자기 자신을 반환하여 반복을 허용하는 중간 인터페이스.
// 이 인터페이스가 End를 확장하기 때문에 반복은 언제든 종료할 수 있습니다
interface Intermediate3 extends End {
  Intermediate3 word3();
}
```

위의 문법을 정의하면, 이제 이 DSL을 Java에서 직접 사용할 수 있습니다. 가능한 모든 구성은 다음과 같습니다:

```java
Start start = // ...

start.singleWord().end();
start.parameterisedWord("abc").end();

start.word1().end();
start.word1().optionalWord().end();

start.word2().wordChoiceA().end();
start.word2().wordChoiceB().end();

start.word3().end();
start.word3().word3().end();
start.word3().word3().word3().end();
```

그리고 가장 좋은 점은, DSL이 Java에서 직접 컴파일된다는 것입니다! 무료 파서를 얻게 되는 것입니다. 동일한 표기법을 사용하여 Scala(또는 Groovy)에서도 이 DSL을 재사용할 수 있으며, Scala에서는 점 "."과 괄호 "()"를 생략하는 약간 다른 표기법을 사용할 수 있습니다:

```scala
val start = // ...

(start singleWord) end;
(start parameterisedWord "abc") end;

(start word1) end;
((start word1) optionalWord) end;

((start word2) wordChoiceA) end;
((start word2) wordChoiceB) end;

(start word3) end;
((start word3) word3) end;
(((start word3) word3) word3) end;
```

## 실제 사례

실제 사례들은 jOOQ 문서와 코드베이스 전반에서 볼 수 있습니다. 다음은 이전 게시물에서 jOOQ로 작성한 다소 복잡한 SQL 쿼리의 발췌입니다:

```java
create().select(
    r1.ROUTINE_NAME,
    r1.SPECIFIC_NAME,
    decode()
        .when(exists(create()
            .selectOne()
            .from(PARAMETERS)
            .where(PARAMETERS.SPECIFIC_SCHEMA.equal(r1.SPECIFIC_SCHEMA))
            .and(PARAMETERS.SPECIFIC_NAME.equal(r1.SPECIFIC_NAME))
            .and(upper(PARAMETERS.PARAMETER_MODE).notEqual("IN"))),
                val("void"))
        .otherwise(r1.DATA_TYPE).as("data_type"),
    r1.NUMERIC_PRECISION,
    r1.NUMERIC_SCALE,
    r1.TYPE_UDT_NAME,
    decode().when(
    exists(
        create().selectOne()
            .from(r2)
            .where(r2.ROUTINE_SCHEMA.equal(getSchemaName()))
            .and(r2.ROUTINE_NAME.equal(r1.ROUTINE_NAME))
            .and(r2.SPECIFIC_NAME.notEqual(r1.SPECIFIC_NAME))),
        create().select(count())
            .from(r2)
            .where(r2.ROUTINE_SCHEMA.equal(getSchemaName()))
            .and(r2.ROUTINE_NAME.equal(r1.ROUTINE_NAME))
            .and(r2.SPECIFIC_NAME.lessOrEqual(r1.SPECIFIC_NAME)).asField())
    .as("overload"))
.from(r1)
.where(r1.ROUTINE_SCHEMA.equal(getSchemaName()))
.orderBy(r1.ROUTINE_NAME.asc())
.fetch()
```

다음은 꽤 매력적으로 보이는 또 다른 라이브러리의 예시입니다. jRTF라는 라이브러리로, Java에서 Fluent 스타일로 RTF 문서를 만드는 데 사용됩니다:

```java
rtf()
  .header(
    color( 0xff, 0, 0 ).at( 0 ),
    color( 0, 0xff, 0 ).at( 1 ),
    color( 0, 0, 0xff ).at( 2 ),
    font( "Calibri" ).at( 0 ) )
  .section(
        p( font( 1, "Second paragraph" ) ),
        p( color( 1, "green" ) )
  )
).out( out );
```

## 요약

Fluent API는 지난 7년간 큰 화두였습니다. Martin Fowler는 많이 인용되는 인물이 되었고, Fluent API가 그 이전부터 존재했음에도 대부분의 공을 인정받고 있습니다. Java에서 가장 오래된 "Fluent API" 중 하나는 `java.lang.StringBuffer`에서 볼 수 있으며, 이는 임의의 객체를 String에 추가(appending)할 수 있게 해줍니다. 하지만 Fluent API의 가장 큰 이점은 "외부 DSL"을 Java로 쉽게 매핑하고, 이를 임의의 복잡도를 가진 "내부 DSL"로 구현할 수 있는 능력입니다.

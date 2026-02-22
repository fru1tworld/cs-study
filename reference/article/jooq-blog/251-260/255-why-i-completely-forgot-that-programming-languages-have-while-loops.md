# 프로그래밍 언어에 while 루프가 있다는 것을 완전히 잊은 이유

> 원문: https://blog.jooq.org/why-i-completely-forgot-that-programming-languages-have-while-loops/

최근에 당혹스러운 사실을 발견했습니다:

"뭐라고. 나는 PL/SQL에서 while 루프를 한 번도 사용한 적이 없다. 오늘 알았다 :)"
— Lukas Eder (@lukaseder) 2016년 7월 26일

네. 저의 모든 전문적인 PL/SQL 작업(은행 업계에서 꽤 많이 했습니다)에서 `WHILE` 루프를 실제로 사용한 적이 없습니다 – 적어도 기억나는 한. `WHILE` 루프의 개념은 간단하며, PL/SQL과 같은 대부분의 언어에서 사용 가능합니다:

```sql
WHILE condition
LOOP
   {...statements...}
END LOOP;
```

또는 Java에서:

```java
while (condition)
    statement
```

그렇다면, 왜 저는 이것을 한 번도 사용하지 않았을까요?

### 대부분의 루프는 컬렉션을 반복합니다

돌이켜보면, Java가 버전 5가 되어서야 foreach 루프를 도입했다는 것은 미친 일입니다:

```java
String[] strings = { "a", "b", "c" };
for (String s : strings)
    System.out.println(s);
```

이것은 우리가 전혀 신경 쓰지 않는 지역 루프 변수를 사용하는 동등한 루프에 대한 Java의 가장 유용한 문법적 설탕(syntax sugar)입니다:

```java
String[] strings = { "a", "b", "c" };
for (int i = 0; i < strings.length; i++)
    System.out.println(strings[i]);
```

솔직히 말해봅시다. 우리가 정말로 이 루프 변수를 원할 때가 언제인가요? 거의 없습니다. 때때로 현재 문자열과 함께 다음 문자열에 접근해야 할 때가 있고, 어떤 이유로 명령형 패러다임을 고수하고 싶을 때가 있습니다(함수형 프로그래밍 API로 더 쉽게 할 수 있는데도). 하지만 그게 전부입니다. 대부분의 루프는 단순히 전체 컬렉션을 매우 단순하고 직접적인 방식으로 반복합니다.

### SQL은 모두 컬렉션에 관한 것입니다

SQL에서 모든 것은 테이블입니다([이 글](https://blog.jooq.org/10-sql-tricks-that-you-didnt-think-were-possible/)의 SQL 트릭 #1 참조), 관계 대수에서 모든 것이 집합인 것처럼. PL/SQL은 Oracle 데이터베이스에서 SQL 언어를 "감싸는" 유용한 절차적 언어입니다. PL/SQL에서 작업을 수행하는(예: Java 대신) 주요 이유는 다음과 같습니다:

* 성능(가장 중요한 이유), 예를 들어 ETL이나 리포팅을 할 때
* 로직이 데이터베이스에 "숨겨져" 있어야 할 때(예: 보안상의 이유로)
* 로직이 모두 데이터베이스에 접근하는 다른 시스템들 간에 재사용되어야 할 때

Java의 foreach 루프와 마찬가지로, PL/SQL은 암시적 커서(명시적 커서와 반대되는)를 정의할 수 있는 기능이 있습니다.

PL/SQL 개발자로서 커서를 반복하고 싶을 때, 적어도 다음과 같은 옵션이 있습니다:

명시적 커서

```sql
DECLARE
  -- 루프 변수
  row all_objects%ROWTYPE;

  -- 커서 명세
  CURSOR c IS SELECT * FROM all_objects;
BEGIN
  OPEN  c;
  LOOP
    FETCH c INTO row;
    EXIT WHEN c%NOTFOUND;
    dbms_output.put_line(row.object_name);
  END LOOP;
  CLOSE c;
END;
```

위의 코드는 Java 5 이전에 우리가 반복해서 작성했던 다음의 지루한 Java 코드에 해당합니다(사실, 제네릭도 없이):

```java
Iterator<Row> c = ... // SELECT * FROM all_objects
while (c.hasNext()) {
    Row row = c.next();
    System.out.println(row.objectName);
}
```

while 루프는 작성하기에 정말 지루합니다. 루프 변수와 마찬가지로, 우리는 이터레이터의 현재 상태에 대해 전혀 신경 쓰지 않습니다. 우리는 전체 컬렉션을 반복하고 싶고, 각 반복에서 현재 어디에 있는지 신경 쓰지 않습니다. PL/SQL에서는 무한 루프 문법을 사용하고 커서가 소진되면 루프를 빠져나오는 것이 일반적인 관행입니다(위 참조). Java에서 이에 해당하는 로직은 작성하기가 더 나쁩니다:

```java
Iterator<Row> c = ... // SELECT * FROM all_objects
for (;;) {
    if (!c.hasNext())
        break;
    Row row = c.next();
    System.out.println(row.objectName);
}
```

암시적 커서

대부분의 PL/SQL 개발자들이 대부분의 시간에 하는 방법은 다음과 같습니다:

```sql
BEGIN
  FOR row IN (SELECT * FROM all_objects)
  LOOP
    dbms_output.put_line(row.object_name);
  END LOOP;
END;
```

커서는 실제로 Java 컬렉션 측면에서 `Iterable`입니다. `Iterable`은 제어 흐름이 루프에 도달할 때 어떤 컬렉션(`Iterator`)이 생성될지에 대한 명세입니다. 즉, 지연 컬렉션입니다. 위의 방식으로 외부 반복을 구현하는 것은 매우 자연스럽습니다. Java에서 SQL을 작성하기 위해 jOOQ를 사용한다면(그래야 합니다), Java에서도 동일한 패턴을 적용할 수 있습니다. jOOQ의 ResultQuery 타입은 `Iterable`을 확장하기 때문에 Java의 foreach 루프에서 `Iterator` 소스로 사용할 수 있습니다:

```java
for (AllObjectsRecord row : ctx.selectFrom(ALL_OBJECTS))
    System.out.println(row.getObjectName());
```

네, 그게 전부입니다! 비즈니스 로직에만 집중하세요. 그것은 컬렉션 명세(쿼리)와 각 행에 대해 하는 작업(println 문)입니다. 커서 노이즈 같은 것은 없습니다!

### 좋아요, 하지만 왜 WHILE을 안 쓰나요?

만약 저처럼 SQL을 사랑한다면, 아마도 SQL이 하는 것처럼 데이터 집합을 선언하는 선언적 프로그래밍 언어의 아이디어를 좋아하기 때문일 것입니다. PL/SQL이나 Java에서 클라이언트 코드를 작성한다면, 전체 데이터 집합에서 계속 작업하고 집합의 관점에서 계속 생각하는 것을 좋아할 것입니다. 중간 객체인 커서에서 작동하는 명령형 프로그래밍 패러다임은 당신의 패러다임이 아닙니다. 당신은 커서에 대해 신경 쓰지 않습니다. 그것을 관리하고 싶지 않고, 열고 닫고 싶지 않습니다. 그 상태를 추적하고 싶지 않습니다. 따라서 암시적 커서 루프나 Java의 foreach 루프(또는 일부 Java 8 Stream 기능)를 선택할 것입니다. 그렇게 더 자주 하다 보면, `WHILE` 루프가 유용한 상황을 점점 덜 만나게 될 것입니다. 결국 그 존재 자체를 잊어버릴 때까지. WHILE LOOP, 당신은 그리워지지 않을 것입니다.

---

## 댓글 섹션

DATO the gonzo programmer의 댓글 (2016년 8월 8일 18:21):

"'fold'의 SQL 등가물이 무엇인지 아시나요?"

lukaseder의 답글 (2016년 8월 8일 20:14):

"고차 함수를 지원하는 구현은 알지 못합니다. 물론, 사용자 정의 집계 함수를 지원하는 데이터베이스에서 '트릭'을 통해 구현할 수는 있지만, 그것이 당신이 염두에 둔 것은 아닐 것입니다.

과거에 왜 아직 어떤 구현도 이를 지원하지 않는지 궁금해한 적이 있습니다. 아마도 쿼리 옵티마이저에게는 다소 '불투명'하고, 보통 윈도우 함수를 사용하여 fold로 하는 것을 에뮬레이트할 수 있는 방법이 있기 때문인 것 같습니다. 구체적인 예가 있으셨나요?"

DATO the gonzo programmer의 답글 (2016년 8월 9일 09:11):

"감사합니다 Lukas. 구체적인 예는 없습니다 – Elm, Prolog/Datalog 그리고 최근에는 Picat의 일부를 탐구하기 시작해서 이 질문이 머릿속을 스쳐갔습니다. 하지만 아직 아이디어를 정리하는 중입니다."

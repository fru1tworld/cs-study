# "ControlFlowException"의 드문 활용 사례

> 원문: https://blog.jooq.org/rare-uses-of-a-controlflowexception/

제어 흐름은 다양한 패러다임에 스며든 "명령형 프로그래밍의 유산"을 나타내며, 여기에는 Java의 객체 지향 접근 방식도 포함됩니다. 표준적인 분기와 루프 구조 외에도, GOTO와 같은 프리미티브와 예외와 같은 비로컬 메커니즘이 존재합니다. 이 글에서는 이러한 논쟁적인 제어 흐름 기법을 심층적으로 살펴봅니다.

## GOTO

`goto`는 Java에서 예약어이며 JVM 바이트코드에서 유효하지만, Java는 goto 연산을 쉽게 지원하지 않습니다. 두 가지 우회 방법이 있습니다:

앞으로 점프하는 예제:

```java
label: {
    // 작업 수행
    if (check) break label;
    // 추가 작업 수행
}
```

대응하는 바이트코드:

```
2  iload_1 [check]
3  ifeq 6          // 앞으로 점프
6  ..
```

뒤로 점프하는 예제:

```java
label: do {
    // 작업 수행
    if (check) continue label;
    // 추가 작업 수행
    break label;
} while(true);
```

대응하는 바이트코드:

```
 2  iload_1 [check]
 3  ifeq 9
 6  goto 2          // 뒤로 점프
 9  ..
```

이러한 트릭은 극히 드문 상황에서만 유용합니다. 유명한 xkcd 만화가 goto 사용의 위험성을 잘 보여줍니다.

## 예외로 흐름 탈출하기

일반적인 예외는 오류 발생 시 제어 흐름을 중단시킬 수 있습니다. 그러나 오류가 아닌 제어 흐름을 위해 예외를 사용하는 것은 똑같이 문제가 있어 보입니다:

```java
try {
    // 작업 수행
    if (check) throw new Exception();
    // 추가 작업 수행
}
catch (Exception notReallyAnException) {}
```

## ControlFlowException의 적법한 사용 사례

드물지만 제어 흐름 예외가 정당한 목적으로 사용되는 유효한 시나리오가 있습니다. SAX 파서 예제가 사용 사례를 보여줍니다:

ControlFlowException 생성:

```java
package com.example;

public class ControlFlowException
extends SAXException {}
```

참고: SAX 계약은 핸들러 구현이 `RuntimeException`이 아닌 `SAXException`을 던지도록 요구합니다.

SAX 핸들러에서 사용:

```java
package com.example;

import java.io.File;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class Parse {
  public static void main(String[] args)
  throws Exception {
    SAXParser parser = SAXParserFactory
        .newInstance()
        .newSAXParser();

    try {
      parser.parse(new File("test.xml"),
          new Handler());
      System.out.println(
          "3개 미만의 <check/> 요소가 발견되었습니다.");
    } catch (ControlFlowException e) {
      System.out.println(
          "3개 이상의 <check/> 요소가 발견되었습니다.");
    }
  }

  private static class Handler
  extends DefaultHandler {

    int count;

    @Override
    public void startElement(
        String uri,
        String localName,
        String qName,
        Attributes attributes) {

      if ("check".equals(qName) && ++count >= 3)
        throw new ControlFlowException();
    }
  }
}
```

## 언제 ControlFlowException을 사용해야 하는가

적법한 사용 지표는 다음과 같습니다:

- 복잡한 알고리즘에서 탈출해야 할 때 (단순한 블록과는 다르게)
- 알고리즘에 동작을 도입하는 핸들러를 구현할 때
- 핸들러 계약이 명시적으로 예외 던지기를 허용할 때
- 사용 사례가 알고리즘 리팩토링을 정당화하지 않을 때

## 실제 사례: jOOQ 배치 쿼리

jOOQ는 SQL 문을 개별적으로 실행하는 대신 수집하여 배치 저장을 가능하게 합니다. 배치 작업은 다음 의사 알고리즘을 사용합니다:

```java
// 쿼리 실행을 방지하고 대신 예외를 던지는
// "핸들러"를 연결하는 의사 코드:
context.attachQueryCollector();

// 모든 store 작업에 대해 SQL 수집
for (int i = 0; i < records.length; i++) {
  try {
    records[i].store();
  }

  // 연결된 핸들러는 실제로 레코드를 데이터베이스에
  // 저장하는 대신 이 예외가 던져지도록 함
  catch (QueryCollectorException e) {

    // 렌더링된 SQL 문이 사용 가능해진 후
    // 예외가 던져짐
    queries.add(e.query());
  }
}
```

## 실제 사례: 예외적인 동작

또 다른 jOOQ 애플리케이션은 이 기법이 드문 예외적 동작을 어떻게 도입하는지 보여줍니다. 일부 데이터베이스는 문장당 바인드 값을 제한합니다:

- SQLite: 999
- Ingres 10.1.0: 1024
- Sybase ASE 15.5: 2000
- SQL Server 2008: 2100

이 최대치에 도달하면 jOOQ는 바인드 값을 인라인해야 합니다. jOOQ의 컴포지트 패턴이 렌더링 동작을 캡슐화하기 때문에, 쿼리 모델 트리를 순회하기 전에는 바인드 값 개수를 알 수 없습니다.

정석적인 (비효율적인) 접근 방식:

```java
String sql;

query.renderWith(countRenderer);
if (countRenderer.bindValueCount() > maxBindValues) {
  sql = query.renderWithInlinedBindValues();
}
else {
  sql = query.render();
}
```

이는 두 번의 렌더링이 필요합니다: 한 번은 바인드 값을 세기 위해, 한 번은 실제 SQL을 생성하기 위해.

ControlFlowException을 사용한 최적화된 접근 방식:

```java
// 최대 바인드 값 수가 초과되면 쿼리 렌더링을
// 중단하는 "핸들러"를 연결하는 의사 코드:
context.attachBindValueCounter();
String sql;
try {

  // 대부분의 경우, 이것은 성공할 것입니다:
  sql = query.render();
}
catch (ReRenderWithInlinedVariables e) {
  sql = query.renderWithInlinedBindValues();
}
```

이 솔루션이 우수한 이유는:

- 재렌더링은 예외적인 경우에만 발생합니다
- 쿼리 렌더링은 개수를 계산하기 위해 완료되지 않고 조기에 중단됩니다
- 실제 개수 (2000, 5000, 또는 100000개의 바인드 값)는 관련이 없습니다

## 결론

모든 예외적인 기법과 마찬가지로, 적절한 순간에 사용하는 것을 기억하세요. 의심이 든다면, 다시 한번 생각해 보세요.

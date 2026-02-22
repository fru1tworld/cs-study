# Java의 깊이: 공변성을 통해 노출된 API 누수

> 원문: https://blog.jooq.org/the-depths-of-java-api-leak-exposed-through-covariance/

Java는 때때로 매우 까다로울 수 있으며, 특히 API 설계에서 그렇다. 매우 흥미로운 사례를 살펴보자. jOOQ는 API와 구현을 강하게 분리한다. 모든 API는 org.jooq 패키지에 있으며 공개(public)되어 있다. 대부분의 구현은 org.jooq.impl 패키지에 있으며 패키지 프라이빗(package-private)이다. 팩토리와 일부 전용 기본 구현만 공개되어 있다. 이를 통해 매우 강력한 패키지 수준 캡슐화가 가능하며, jOOQ 사용자에게는 대부분 인터페이스만 노출된다.

## 패키지 수준 캡슐화의 간략한 예제

다음은 jOOQ가 SQL 테이블을 모델링하는 대략적인 방식이다. (극도로 단순화된) API는 다음과 같다:

```java
package org.jooq;

/
 * 데이터베이스의 테이블
 */
public interface Table {

  /
   * 두 테이블을 조인한다
   */
  Table join(Table table);
}
```

그리고 두 개의 (극도로 단순화된) 구현 클래스는 다음과 같다:

```java
package org.jooq.impl;

import org.jooq.Table;

/
 * 기본 구현
 */
abstract class AbstractTable implements Table {

  @Override
  public Table join(Table table) {
    return null;
  }
}

/
 * 커스텀 구현, 클라이언트 코드에 공개적으로 노출됨
 */
public class CustomTable extends AbstractTable {
}
```

## 내부 API가 노출되는 방식

내부 API가 공변성(covariance)을 이용한 몇 가지 트릭을 사용한다고 가정해 보자:

```java
abstract class AbstractTable implements Table, InteralStuff {

  // 이 메서드는 AbstractTable을 반환한다는 점에 주목하라.
  // 내부 API 안에서 일부 내부 API의 사실을
  // 노출하는 것이 편리할 수 있기 때문이다.
  @Override
  public AbstractTable join(Table table) {
    return null;
  }

  /
   * 내부 API 메서드, 역시 패키지 프라이빗
   */
  void doThings() {}
  void doMoreThings() {

    // 내부 API 사용
    join(this).doThings();
  }
}
```

첫눈에 보면 모두 안전해 보이지만, 정말 그럴까? `AbstractTable`은 패키지 프라이빗이지만, `CustomTable`은 이를 상속하며 공변 메서드 오버라이드인 `AbstractTable join(Table)`을 포함한 모든 API를 물려받는다. 그 결과는 어떻게 될까? 다음의 클라이언트 코드를 확인해 보자:

```java
package org.jooq.test;

import org.jooq.Table;
import org.jooq.impl.CustomTable;

public class Test {
  public static void main(String[] args) {
    Table joined = new CustomTable();

    // 이것은 동작한다. AbstractTable에 대한 정보가 컴파일러에 노출되지 않는다.
    Table table1 = new CustomTable();
    Table join1 = table1.join(joined);

    // 이것도 동작한다. join이 AbstractTable을 노출하더라도 말이다.
    CustomTable table2 = new CustomTable();
    Table join2 = table2.join(joined);

    // 이것은 동작하지 않는다. AbstractTable 타입이 보이지 않는다.
    Table join3 = table2.join(joined).join(joined);
    //            ^^^^^^^^^^^^^^^^^^^ 이것은 역참조할 수 없다

    // ... 따라서 이러한 구현 세부 사항을 다시 숨겨야 한다.
    // API 결함은 캐스팅으로 우회할 수 있다.
    Table join4 = ((Table) table2.join(joined)).join(joined);
  }
}
```

## 결론

클래스 계층 구조에서 가시성을 조작하는 것은 위험할 수 있다. 인터페이스에 선언된 API 메서드는 비공개 요소를 포함하는 공변 구현과 관계없이 항상 공개(public)라는 사실에 주의하라. 이것은 API 설계자가 적절히 처리하지 않으면 API 사용자에게 꽤 성가신 문제가 될 수 있다. jOOQ의 다음 버전에서 수정되었다. :-)

---

## 댓글

Moandji Ezana (2012년 2월 9일 07:33)

"이것이 정말로 누수인가요? `AbstractTable#join`은 `Table#join`을 구현한 것이므로 해당 인터페이스를 통해 접근 가능할 것으로 예상해야 하는 것 아닌가요?"

"캐스팅 없이도 같은 결과를 얻을 수 있습니다:
`Table join2 = table2.join(joined);`
`join2.join(joined);`"

lukaseder (2012년 2월 9일 09:00)

"@Moandji: 누수되고 있는 것은 `join` 메서드가 아닙니다. 패키지 프라이빗인 `AbstractTable.join()`에서 상속된 `CustomTable.join()`의 공변 반환 타입이 누수되고 있는 것입니다. `CustomTable.join()`은 `org.jooq.impl` 패키지 외부에서 보이지 않는 `AbstractTable`을 반환하므로, `Table`로 업캐스팅하지 않으면 추가적으로 역참조할 수 없습니다."

"참고로 `Table`로의 업캐스팅은 `Table`에 할당하는 것과 동일한 효과를 가집니다."

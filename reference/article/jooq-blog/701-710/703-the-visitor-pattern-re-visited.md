# 비지터 패턴 재방문

> 원문: https://blog.jooq.org/the-visitor-pattern-re-visited/

게시일: 2012년 4월 10일
작성자: lukaseder

비지터 패턴은 객체 지향 설계에서 가장 과대평가되면서도 동시에 가장 과소평가된 패턴 중 하나입니다. 과대평가되는 이유는 너무 성급하게 선택되는 경우가 많고(아마도 아키텍처 우주비행사에 의해), 잘못된 방식으로 추가되면 원래 매우 단순한 설계를 부풀리기 때문입니다. 과소평가되는 이유는 교과서적인 예제를 따르지 않는다면 매우 강력할 수 있기 때문입니다. 자세히 살펴보겠습니다.

## 문제 #1: 네이밍

제 생각에 가장 큰 결함은 바로 이름 자체입니다. "비지터(visitor)" 패턴. 구글에 검색하면 대부분 관련 위키피디아 문서에 도달하게 되는데, 거기에는 다음과 같은 재미있는 이미지가 나옵니다:

(위키피디아 비지터 패턴 예제 이미지)

맞습니다. 일상적인 소프트웨어 엔지니어링 업무에서 바퀴, 엔진, 차체를 다루는 98%의 사람들에게는 이것이 즉시 이해됩니다. 왜냐하면 우리 차를 고치는 데 수천 달러를 청구하는 정비사가 먼저 바퀴를 "방문(visit)"하고, 그 다음 엔진을 "방문"한 후, 마침내 우리 지갑을 "방문"하여 현금을 "수락(accept)"한다는 것을 알기 때문입니다. 운이 나쁘면 우리가 일하는 동안 아내도 "방문"하겠지만, 그 충실한 영혼은 절대 "수락"하지 않을 것입니다. 하지만 일상 업무에서 다른 문제를 해결하는 나머지 2%는 어떨까요? 전자 뱅킹 시스템, 증권 거래 클라이언트, 인트라넷 포털 등을 위한 복잡한 데이터 구조를 코딩할 때 말입니다. 진정한 계층적 데이터 구조에 비지터 패턴을 적용해 보면 어떨까요? 폴더와 파일처럼요? (좋습니다, 사실 그렇게 복잡하지는 않습니다.) 그래서 폴더를 "방문"하고, 모든 폴더가 파일에게 "비지터"를 "수락"하게 한 다음, 비지터가 파일도 "방문"하게 하는 거죠. 뭐라고요?? 자동차가 부품에게 비지터를 수락하게 하고 비지터가 자기 자신을 방문하게 한다고요? 이 용어들은 오해를 불러일으킵니다. 디자인 패턴 자체로는 일반적이고 적합하지만, 실제 설계에서는 치명적입니다. 왜냐하면 실제로 파일 시스템을 읽고/쓰고/삭제하고/수정할 때, 아무도 "수락"과 "방문"이라는 용어로 생각하지 않기 때문입니다.

## 문제 #2: 다형성

이것은 잘못된 상황에 적용될 때 네이밍보다 더 큰 두통을 유발하는 부분입니다. 도대체 왜 비지터가 다른 모든 것을 알아야 할까요? 왜 비지터가 계층 구조에 관련된 모든 요소에 대한 메서드를 가져야 할까요? 다형성과 캡슐화는 구현이 API 뒤에 숨겨져야 한다고 주장합니다. (우리 데이터 구조의) API는 아마도 어떤 식으로든 컴포지트 패턴을 구현하고 있을 것입니다. 즉, 그 구성 요소들이 공통 인터페이스를 상속합니다. 물론 바퀴는 자동차가 아니고, 내 아내는 정비사가 아닙니다. 하지만 폴더/파일 구조를 보면, 그것들은 모두 `java.util.File` 객체가 아닌가요?

## 문제의 이해

실제 문제는 방문 코드의 네이밍이나 끔찍한 API 장황함이 아니라, 패턴에 대한 오해입니다. 이것은 다양한 타입의 많은 객체를 가진 크고 복잡한 데이터 구조를 방문하는 데 가장 적합한 패턴이 아닙니다. 이것은 타입이 적은 단순한 데이터 구조를 수백 개의 비지터로 방문하는 데 가장 적합한 패턴입니다. 파일과 폴더를 예로 들어보겠습니다. 이것은 단순한 데이터 구조입니다. 두 가지 타입이 있습니다. 하나가 다른 하나를 포함할 수 있고, 둘 다 일부 속성을 공유합니다. 다양한 비지터는 다음과 같을 수 있습니다:

- `CalculateSizeVisitor`
- `FindOldestFileVisitor`
- `DeleteAllVisitor`
- `FindFilesByContentVisitor`
- `ScanForVirusesVisitor`
- ... 그 외 다수

네이밍은 여전히 마음에 들지 않지만, 이 패러다임에서는 패턴이 완벽하게 작동합니다.

## 그렇다면 비지터 패턴이 "잘못된" 경우는 언제일까요?

jOOQ의 `QueryPart` 구조를 예로 들겠습니다. 다양한 SQL 쿼리 구성 요소를 모델링하는 것이 매우 많으며, jOOQ가 임의의 복잡도를 가진 SQL 쿼리를 빌드하고 실행할 수 있게 해줍니다. 몇 가지 예를 들어보겠습니다:

- Condition
  - CombinedCondition
  - NotCondition
  - InCondition
  - BetweenCondition
- Field
  - TableField
  - Function
  - AggregateFunction
  - BindValue
- FieldList

이보다 훨씬 더 많습니다. 각각은 두 가지 동작을 수행할 수 있어야 합니다: SQL 렌더링과 변수 바인딩. 이는 40~50개 이상의 타입을 각각 알아야 하는 두 개의 비지터가 된다는 뜻입니다...? 먼 미래에 jOOQ 쿼리가 JPQL이나 다른 쿼리 타입을 렌더링할 수 있게 될 수도 있습니다. 그러면 40~50개 타입에 대해 3개의 비지터가 됩니다. 분명히 여기서 클래식 비지터 패턴은 잘못된 선택입니다. 하지만 저는 여전히 QueryPart를 "방문"하면서 렌더링과 바인딩을 더 낮은 추상화 수준에 위임하고 싶습니다.

## 그렇다면 이것을 어떻게 구현할까요?

간단합니다: 컴포지트 패턴을 고수하세요! 이 패턴은 데이터 구조에 모든 구성 요소가 구현해야 하는 API 요소를 추가할 수 있게 해줍니다. 직관적으로 1단계는 다음과 같을 것입니다:

```java
interface QueryPart {
  // QueryPart가 자신의 SQL을 반환하게 합니다
  String getSQL();

  // QueryPart가 다음 바인드 인덱스를 받아
  // PreparedStatement에 변수를 바인딩하고,
  // 마지막 바인드 인덱스를 반환합니다
  int bind(PreparedStatement statement, int nextIndex);
}
```

이 API를 사용하면 SQL 쿼리를 쉽게 추상화하고 하위 수준 아티팩트에 책임을 위임할 수 있습니다. 예를 들어 `BetweenCondition`은 `[field] BETWEEN [lower] AND [upper]` 조건의 부분을 올바르게 정렬하고, 문법적으로 올바른 SQL을 렌더링하며, 작업의 일부를 자식 QueryPart에 위임합니다:

```java
class BetweenCondition {
  Field field;
  Field lower;
  Field upper;

  public String getSQL() {
    return field.getSQL() + " between " +
           lower.getSQL() + " and " +
           upper.getSQL();
  }

  public int bind(PreparedStatement statement, int nextIndex) {
    int result = nextIndex;

    result = field.bind(statement, result);
    result = lower.bind(statement, result);
    result = upper.bind(statement, result);

    return result;
  }
}
```

반면에 `BindValue`는 주로 변수 바인딩을 담당합니다:

```java
class BindValue {
  Object value;

  public String getSQL() {
    return "?";
  }

  public int bind(PreparedStatement statement, int nextIndex) {
    statement.setObject(nextIndex, value);
    return nextIndex + 1;
  }
}
```

이것들을 결합하면 `? BETWEEN ? AND ?` 형태의 조건을 쉽게 만들 수 있습니다. 더 많은 QueryPart가 구현되면, 적절한 Field 구현이 있을 때 `MY_TABLE.MY_FIELD BETWEEN ? AND (SELECT ? FROM DUAL)`과 같은 것도 상상할 수 있습니다. 이것이 컴포지트 패턴을 강력하게 만드는 것입니다 - 공통 API와 동작을 캡슐화하는 많은 컴포넌트가 동작의 일부를 하위 컴포넌트에 위임합니다.

2단계는 API 발전을 처리합니다

지금까지 살펴본 컴포지트 패턴은 꽤 직관적이면서도 매우 강력합니다. 하지만 조만간 부모 QueryPart에서 자식에게 상태를 전달하고 싶다는 것을 발견하면서 더 많은 매개변수가 필요해질 것입니다. 예를 들어, 일부 절에서 바인드 값을 인라인할 수 있기를 원합니다. 어쩌면 일부 SQL 방언에서는 BETWEEN 절에서 바인드 변수를 허용하지 않을 수도 있습니다. 현재 API로 이것을 어떻게 처리할까요? "boolean inline" 매개변수를 추가하여 확장할까요? 아닙니다! 이것이 바로 비지터 패턴이 발명된 이유 중 하나입니다. 컴포지트 구조 요소의 API를 단순하게 유지하기 위해서입니다(이들은 "accept"만 구현하면 됩니다). 하지만 이 경우, 진정한 비지터 패턴을 구현하는 것보다 훨씬 나은 방법은 매개변수를 "컨텍스트"로 대체하는 것입니다:

```java
interface QueryPart {
  // QueryPart가 이제 컨텍스트에 SQL을 렌더링합니다
  void toSQL(RenderContext context);

  // QueryPart가 이제 컨텍스트에 변수를 바인딩합니다
  void bind(BindContext context);
}
```

위의 컨텍스트들은 다음과 같은 속성을 포함할 것입니다(setter와 render 메서드는 메서드 체이닝을 위해 컨텍스트 자체를 반환합니다):

```java
interface RenderContext {
  // 바인드 변수를 인라인하고 있는지 여부
  boolean inline();
  RenderContext inline(boolean inline);

  // 필드가 필드 선언으로 렌더링되어야 하는지 여부
  // (필드 참조가 아닌). 이것은 별칭이 있는 필드에 사용됩니다
  boolean declareFields();
  RenderContext declareFields(boolean declare);

  // 테이블이 테이블 선언으로 렌더링되어야 하는지 여부
  // (테이블 참조가 아닌). 이것은 별칭이 있는 테이블에 사용됩니다
  boolean declareTables();
  RenderContext declareTables(boolean declare);

  // 바인드 변수를 캐스팅해야 하는지 여부
  boolean cast();

  // 렌더링 메서드
  RenderContext sql(String sql);
  RenderContext sql(char sql);
  RenderContext keyword(String keyword);
  RenderContext literal(String literal);

  // 컨텍스트의 "visit" 메서드
  RenderContext sql(QueryPart sql);
}
```

`BindContext`도 마찬가지입니다. 보시다시피 이 API는 꽤 확장 가능하며, 새로운 속성을 추가할 수 있고, SQL을 렌더링하는 다른 공통 수단도 추가할 수 있습니다. 하지만 `BetweenCondition`은 SQL을 렌더링하는 방법과 바인드 변수가 허용되는지 여부에 대한 캡슐화된 지식을 포기할 필요가 없습니다. 그 지식을 자기 자신 안에 유지합니다:

```java
class BetweenCondition {
  Field field;
  Field lower;
  Field upper;

  // QueryPart가 이제 컨텍스트에 SQL을 렌더링합니다
  public void toSQL(RenderContext context) {
    context.sql(field).keyword(" between ")
           .sql(lower).keyword(" and ")
           .sql(upper);
  }

  // QueryPart가 이제 컨텍스트에 변수를 바인딩합니다
  public void bind(BindContext context) {
    context.bind(field).bind(lower).bind(upper);
  }
}
```

반면에 `BindValue`는 주로 변수 바인딩을 담당합니다:

```java
class BindValue {
  Object value;

  public void toSQL(RenderContext context) {
    context.sql("?");
  }

  public void bind(BindContext context) {
    context.statement().setObject(context.nextIndex(), value);
  }
}
```

## 결론: 비지터 패턴이 아닌 컨텍스트 패턴이라 부르세요

비지터 패턴으로 성급하게 넘어갈 때 주의하세요. 많은 경우에 설계를 부풀려서 완전히 읽기 어렵고 디버깅하기 어렵게 만들 것입니다. 다음은 기억해야 할 규칙을 정리한 것입니다:

1. 매우 많은 비지터와 비교적 단순한 데이터 구조(적은 타입)가 있다면, 비지터 패턴은 아마 괜찮습니다.
2. 매우 많은 타입과 비교적 작은 비지터 집합(적은 동작)이 있다면, 비지터 패턴은 과잉이므로 컴포지트 패턴을 고수하세요.
3. 간단한 API 발전을 위해, 컴포지트 객체가 단일 컨텍스트 매개변수를 받는 메서드를 갖도록 설계하세요.
4. 갑자기, 컨텍스트=비지터이고 "visit"과 "accept"="여러분의 고유한 메서드 이름"인 "거의-비지터" 패턴을 다시 갖게 될 것입니다.

"컨텍스트 패턴"은 "컴포지트 패턴"처럼 직관적이면서도 "비지터 패턴"처럼 강력하여, 두 세계의 장점을 결합합니다.

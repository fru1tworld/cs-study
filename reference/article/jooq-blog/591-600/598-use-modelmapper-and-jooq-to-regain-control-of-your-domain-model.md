# ModelMapper와 jOOQ로 도메인 모델의 제어권을 되찾으라

> 원문: https://blog.jooq.org/use-modelmapper-and-jooq-to-regain-control-of-your-domain-model/

참고: 이 글은 오래된 글이며, 더 이상 SQL 결과를 도메인 모델에 매핑하기 위해 서드파티 라이브러리를 사용하는 것을 권장하지 않습니다. 대신 jOOQ 자체의 [레코드 중첩](https://www.jooq.org/doc/latest/manual/sql-building/column-expressions/nested-records/), [컬렉션 중첩](https://www.jooq.org/doc/latest/manual/sql-building/column-expressions/multiset-value-constructor/), [임시 변환](https://www.jooq.org/doc/latest/manual/sql-execution/fetching/ad-hoc-converter/) 기능을 사용하세요.

복잡한 관계형 모델을 다루면서 복잡한 도메인 모델에 데이터를 매핑해야 할 때, 매핑 프로세스에 대한 제어권을 되찾는 것이 필수적입니다. 자동 매핑은 종종 해결책보다 더 많은 문제를 만들어내기 때문입니다. ModelMapper는 [jOOQ와의 통합을 문서화](http://modelmapper.org/user-manual/jooq-integration/)하여 흥미로운 접근 방식을 보여줍니다. 저자인 Jonathan Halterman의 허락을 받아 다음 예제를 인용합니다:

---

## jOOQ 통합

ModelMapper의 jOOQ 통합은 jOOQ Record를 JavaBean으로 매핑할 수 있게 해줍니다.

### 설정

다음 Maven 의존성을 추가합니다:

```xml
<dependency>
  <groupId>org.modelmapper</groupId>
  <artifactId>modelmapper</artifactId>
  <version>0.6.1</version>
</dependency>
<dependency>
  <groupId>org.modelmapper</groupId>
  <artifactId>modelmapper-jooq</artifactId>
  <version>0.6.1</version>
</dependency>
```

RecordValueReader를 지원하도록 ModelMapper를 설정합니다:

```java
modelMapper.getConfiguration()
           .addValueReader(new RecordValueReader());
```

### 매핑 예제

주문을 나타내는 레코드가 있다고 가정해 봅시다:

| order_id | customer_id | customer_address_street | customer_address_city |
|----------|-------------|------------------------|----------------------|
| 345      | 678         | 123 Main Street        | SF                   |

이것을 복잡한 객체 모델에 매핑합니다:

```java
// 주문 클래스
public class Order {
  private int id;
  private Customer customer;
}

// 고객 클래스
public class Customer {
  private Address address;
}

// 주소 클래스
public class Address {
  private String street;
  private String city;
}
```

Record가 언더스코어 명명 규칙을 사용하므로, ModelMapper를 그에 맞게 설정합니다:

```java
modelMapper
  .getConfiguration()
  .setSourceNameTokenizer(NameTokenizers.UNDERSCORE);
```

이제 매핑이 간단해집니다:

```java
Order order =
  modelMapper.map(orderRecord, Order.class);
```

단언문으로 올바른 매핑을 검증합니다:

```java
assertEquals(456, order.getId());
assertEquals(789, order.getCustomer().getId());
assertEquals("123 Main Street",
             order.getCustomer()
                  .getAddress()
                  .getStreet());
assertEquals("SF",
             order.getCustomer()
                  .getAddress()
                  .getCity());
```

### 명시적 매핑

명시적 속성 매핑이 필요한 복잡한 시나리오의 경우:

```java
PropertyMap<Record, Order> orderMap =
  new PropertyMap<Record, Order>() {
  protected void configure() {
    map(source("customer_address_street"))
        .getCustomer()
        .getAddress()
        .setStreet(null);
  }
};
```

매핑을 ModelMapper에 추가합니다:

```java
modelMapper.createTypeMap(orderRecord, Order.class)
           .addMappings(orderMap);
```

### 주의사항

ModelMapper는 각 소스와 대상 타입 쌍에 대해 TypeMap을 유지합니다. 구조가 가변적인 Record와 같은 제네릭 타입의 경우, 동일한 대상 타입에 매핑되는 서로 다른 Record를 구분하기 위해 _타입 맵 이름_을 사용합니다:

다른 레코드 구조 예제:

| order_id | order_customer_id | order_customer_address_street | order_customer_address_city |
|----------|-------------------|-------------------------------|---------------------------|
| 444      | 777               | 123 Main Street               | LA                        |

타입 맵 이름을 사용한 매핑:

```java
Order order = modelMapper.map(
    longOrderRecord, Order.class, "long");
```

### 더 많은 예제

ModelMapper는 관계형 매핑 외에도 임의의 모델 변환을 위한 전략적 선택입니다.

자세한 내용은 [ModelMapper 웹사이트](http://modelmapper.org/)를 참조하세요.

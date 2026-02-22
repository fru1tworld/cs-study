# SPI를 개발할 때 이 실수를 저지르지 마라

> 원문: https://blog.jooq.org/do-not-make-this-mistake-when-developing-an-spi/

## 서론

대부분의 코드는 내부적이고 독점적인 상태로 유지되므로, 실수를 리팩토링해도 아무런 문제가 발생하지 않는다. 그러나 공개 API, 특히 서비스 제공자 인터페이스(SPI)를 유지보수할 때는 더 많은 주의가 필요하다. SPI에 대한 호환성을 깨뜨리는 변경은 모든 하위 구현체에 영향을 미치므로, API 진화가 특히 어려워진다.

## H2 Trigger SPI 문제

`org.h2.api.Trigger` 인터페이스는 미묘한 설계 결함을 보여준다. `init()` 메서드 시그니처를 살펴보자:

```java
void init(
    Connection conn,
    String schemaName,
    String triggerName,
    String tableName,
    boolean before,
    int type
)
```

`boolean before` 매개변수는 트리거가 이벤트 전에 실행되는지 후에 실행되는지를 나타낸다. 만약 H2가 나중에 `INSTEAD OF` 트리거를 지원하고 싶다면, 이 불리언은 충분하지 않게 된다. 이것을 열거형으로 교체하면 기존의 모든 구현체가 깨지게 되는데, 이는 공개 SPI에서 받아들일 수 없는 결과다.

## 왜 이것이 중요한가

`ENABLE`/`DISABLE` 절, `FOR EACH ROW` 처리 등 새로운 요구사항이 생길 때마다, SPI는 호환성을 깨뜨리지 않고 확장하기가 더 어려워진다. 우회책으로 보조 클래스인 `TriggerAdapter`가 등장했는데, 이는 원래 설계의 유연성 부족을 나타낸다.

## 더 나은 접근법: 인자 객체

매개변수를 직접 추가하는 대신, 포괄적인 인자 객체를 전달하라:

```java
public interface Trigger {
    default void init(InitArguments args)
        throws SQLException {}
    default void fire(FireArguments args)
        throws SQLException {}

    final class InitArguments {
        public Connection connection() { ... }
        public String schemaName() { ... }
        public String triggerName() { ... }
        public String tableName() { ... }

        @Deprecated
        public boolean before() { ... }
        public TriggerTiming timing() { ... }
        public int type() { ... }
    }

    final class FireArguments {
        public Connection connection() { ... }
        public Object[] oldRow() { ... }
        public Object[] newRow() { ... }
    }
}
```

이 접근법을 사용하면 인터페이스 계약을 수정하지 않고도 `InitArguments`에 새로운 메서드를 추가할 수 있다. 더 이상 사용되지 않는(deprecated) 메서드는 사용자들을 새로운 기능으로 안내하면서 하위 호환성을 유지한다.

## Hibernate의 UserType SPI

Hibernate의 `org.hibernate.usertype.UserType` 인터페이스는 유사한 문제를 보여준다:

```java
public interface UserType {
    int[] sqlTypes();
    Class returnedClass();
    boolean equals(Object x, Object y);
    int hashCode(Object x);

    Object nullSafeGet(
        ResultSet rs,
        String[] names,
        SessionImplementor session,
        Object owner
    ) throws SQLException;

    void nullSafeSet(
        PreparedStatement st,
        Object value,
        int index,
        SessionImplementor session
    ) throws SQLException;

    // ... 추가 메서드들
}
```

구현 시 여러 불확실성이 있다: `nullSafeSet()`에서 owner 참조가 필요한가? 이름 기반 `ResultSet` 접근을 지원하지 않는 드라이버는 어떻게 처리하는가? `CallableStatement`를 사용하는 저장 프로시저는 어떻게 하는가?

설정 객체를 사용한 리팩토링된 접근법은 이것을 단순화할 수 있다:

```java
public interface UserType {
    default void configure(ConfigureArgs args) {}

    final class ConfigureArgs {
        public void sqlTypes(int[] types) { ... }
        public void returnedClass(Class<?> clazz) { ... }
        public void mutable(boolean mutable) { ... }
    }
}
```

## SAX ContentHandler의 한계

SAX API는 오래된 SPI 설계 문제를 보여준다:

```java
public interface ContentHandler {
    void setDocumentLocator(Locator locator);
    void startDocument();
    void endDocument();
    void startPrefixMapping(String prefix, String uri);
    void endPrefixMapping(String prefix);
    void startElement(String uri, String localName,
                      String qName, Attributes atts);
    void endElement(String uri, String localName,
                    String qName);
    void characters(char ch[], int start, int length);
    // ... 더 많은 메서드들
}
```

구현자들은 실질적인 장애물에 직면한다: 요소 속성이 `endElement()` 이벤트에서는 사용할 수 없어서 수동으로 캐싱해야 한다. 접두사 매핑 세부정보는 `endPrefixMapping()`이 실행될 때 사라진다. 이러한 한계는 SPI의 메서드-당-액션 설계에서 직접 비롯된다.

## 세 가지 핵심 원칙

구현체를 깨뜨리지 않고 지속 가능한 SPI 진화를 가능하게 하려면:

1. 단일 인자 객체를 전달하라 - 인터페이스 메서드에 여러 매개변수 대신 하나의 인자 객체를 전달하라
2. 오직 void만 반환하라 - 구현자들이 반환 값 대신 인자 객체를 통해 SPI 상태와 상호작용하도록 하라
3. Java 8 default 메서드를 사용하라 - 또는 확장성을 위해 추상 기본 구현을 제공하라

이러한 관행은 인자 객체에 새로운 메서드를 추가하고, 오래된 메서드를 폐기(deprecate)하며, 하위 호환성을 무기한으로 유지할 수 있는 공간을 만들어준다.

## 결론

공개 SPI 설계는 미래를 내다보는 사고가 필요하다. 프레임워크 작성자는 모든 미래의 요구사항을 예측할 수 없다. 인자 객체, void 반환, 그리고 default 구현을 사용하면 구현자들이 코드를 업데이트하도록 강제하지 않으면서도 기능을 추가할 수 있는 지속 가능한 경로를 제공한다.

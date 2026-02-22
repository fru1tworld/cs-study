# JEP 277 "향상된 사용 중단"은 좋다. 하지만 여기 훨씬 더 나은 대안이 있다

> 원문: https://blog.jooq.org/jep-277-enhanced-deprecation-is-nice-but-heres-a-much-better-alternative/

## 서론

API를 유지보수하는 것은 어렵다. jOOQ API는 매우 복잡하지만, 우리 팀은 비교적 느슨한 시맨틱 버전 관리 규칙을 따른다. JDK의 하위 호환성 유지에 대한 Brian Goetz와 다른 분들의 의견은 존경받을 만하다. 많은 사람들이 `Vector`, `Stack`, `Hashtable` 같은 것들이 제거되기를 원하지만, 컬렉션 API와 관련된 하위 호환성의 극단적인 경우들은 예상치 못한 도전을 제시한다. 예를 들어: 왜 Java Collections의 remove 메서드들은 제네릭이 아닐까?

## 더 나은 사용 중단(Deprecation)

"Dr Deprecator"로 알려진 Stuart Marks가 JEP 277: "향상된 사용 중단(Enhanced Deprecation)"을 제안했다. 이 제안은 `@Deprecated` 어노테이션에 추가 정보 카테고리를 더해 향상시키는 것을 목표로 한다:

- UNSPECIFIED: 명시된 이유 없이 사용 중단됨 (현재 사용 중단된 항목들의 기본값)
- CONDEMNED: 향후 JDK 릴리스에서 제거 예정으로 표시된 API
- DANGEROUS: 이 API를 사용하면 데이터 손실, 데드락, 보안 취약점, 잘못된 결과, 또는 JVM 무결성 손실의 위험이 있음
- OBSOLETE: API가 더 이상 필요하지 않음; 대체물이 존재하지 않음
- SUPERSEDED: API가 새로운 API로 대체됨; 마이그레이션 권장
- UNIMPLEMENTED: 이것을 호출해도 아무 효과가 없거나 예외를 던짐
- EXPERIMENTAL: API가 안정성이 부족함; 호환되지 않게 변경되거나 사라질 수 있음

사용 중단 의도를 전달하는 것은 중요하다. `@deprecated` Javadoc 태그를 사용하면 동기를 설명하는 텍스트를 생성할 수 있다.

## 훨씬 더 나은 대안

이 제안에는 한계가 있다:

- 확장성이 없음: 서드파티 API 제공자들은 CONDEMNED, DANGEROUS 등보다 더 많은 요소가 필요하다
- Javadoc과 중복됨: 어노테이션 자체에 공식적으로 설명 텍스트를 제공할 수 없다
- 개념적으로 잘못됨: UNIMPLEMENTED나 EXPERIMENTAL을 "deprecated"로 표시하는 것은 임시방편적인 사고방식을 대표한다

대신 나는 다음을 제안한다:

```java
public @interface Warning {
    String name() default "warning";
    String description() default "";
}
```

경고 타입을 상수로 제한할 필요가 없다. 어떤 문자열이든 받아들이는 `@Warning` 어노테이션이 유연성을 제공한다. JDK는 잘 알려진 문자열 값들을 설정할 수 있다:

```java
public interface ResultSet {
    @Deprecated
    @Warning(name="OBSOLETE")
    InputStream getUnicodeStream(int columnIndex);
}
```

또는:

```java
public interface Collection<E> {
    @Warning(name="OPTIONAL")
    boolean remove(Object o);
}
```

`@SuppressWarnings` 어노테이션을 향상시켜 개발자들이 의도적인 사용을 인정할 수 있게 할 수 있다:

```java
Collection<Integer> collection = new ArrayList<>();

@SuppressWarnings("OPTIONAL")
boolean ok = collection.remove(1);
```

## 장점

이 접근 방식은 여러 관심사를 동시에 해결한다:

- JDK 유지보수자들은 기능을 부드럽게 사용 중단하기 위한 도구를 얻는다
- 현재 문서화가 부족한 `@SuppressWarnings` 메커니즘이 더 깔끔하고 공식적으로 된다
- 다양한 사용 사례에 대해 커스텀 경고를 발생시킬 수 있다
- 사용자들은 세밀한 수준에서 경고를 무시할 수 있다

jOOQ의 경우, DSL `equal()` 메서드와 `Object.equals()`를 구분하는 것이 가능해진다:

```java
public interface Field<T> {
    /
     * <code>this = value</code>.
     */
    Condition equal(T value);

    /
     * <strong>주의! 이것은 jOOQ DSL 기능이 아니라
     * {@link Object#equals(Object)}입니다!</strong>
     */
    @Override
    @Warning(
        name = "ACCIDENTAL_EQUALS",
        description = "Field.equal을 사용하려고 했나요?"
    )
    boolean equals(Object other);
}
```

이 사용 사례는 다음에서 유래했다: https://github.com/jOOQ/jOOQ/issues/4763

## 결론

JEP 277은 유용하지만 범위가 제한적이다(아마도 Jigsaw를 더 지연시키지 않기 위해서). JDK 유지보수자들이 컴파일러 경고 생성을 더 철저하게 다뤄서 "올바른 일"을 해주기를 바란다.

위의 대략적인 명세는 불완전하지만, API 설계자로서 반복적으로 필요로 하는 것을 대표한다: 사용자들에게 잠재적인 API 오용에 대해 경고하고, 사용자들이 다음을 통해 이를 억제할 수 있는 능력:

- 코드에서 직접 `@SuppressWarnings` 사용
- Eclipse, NetBeans, IntelliJ에서 쉽게 구현할 수 있는 IDE 설정

`@Warning`이 존재하게 되면, 아마도 `@Deprecated`가 마침내 deprecated 될 수 있을 것이다:

```java
@Warning(name = "OBSOLETE")
public @interface Deprecated {
}
```

## 토론

후속 토론은 다음에서 볼 수 있다:

- jdk9-dev: http://mail.openjdk.java.net/pipermail/jdk9-dev/2015-December/003336.html
- reddit: https://redd.it/3yn9ys

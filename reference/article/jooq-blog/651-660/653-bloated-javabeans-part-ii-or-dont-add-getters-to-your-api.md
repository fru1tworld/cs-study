# 비대해진 JavaBeans, Part II - 또는 API에 "Getter"를 추가하지 마세요

> 원문: https://blog.jooq.org/bloated-javabeans-part-ii-or-dont-add-getters-to-your-api/

저는 이전 글에서 JavaBean의 비대함을 줄이는 방법에 대해 다룬 적이 있습니다. 이번에는 getter와 setter 명명 규칙에 대한 주제를 다시 살펴보겠습니다. 왜 개발자들은 "get"/"is"와 "set" 접두사를 사용해야 할까요? 이 규칙은 코드베이스 전체에서 대소문자를 구분하는 검색을 복잡하게 만듭니다.

## 핵심 문제

이 글에서는 다음과 같은 전형적인 패턴이 불필요하게 장황하다고 지적합니다:

```java
public class MyBean {
    private int myProperty;

    public int getMyProperty() {
        return myProperty;
    }

    public void setMyProperty(int myProperty) {
        this.myProperty = myProperty;
    }
}
```

## java.io.File 분석

저는 File 클래스를 "JavaBean-o-mania(JavaBean 광풍)"가 잘못된 방향으로 간 경고적 사례로 사용합니다. 다음과 같이 일관성 없는 명명을 보여줍니다:

- 동사를 사용하는 액션 메서드: `delete()`, `mkdir()`, `renameTo()`
- 속성 getter: `getPath()`
- 다른 접두사를 사용하는 변환 getter: `toPath()`, `toURI()`
- "get" 없이 파일 정보를 가져오는 메서드: `lastModified()`, `length()`
- 파일을 수정하는 혼란스러운 "setter": `setLastModified()`, `setReadable()`

## 다섯 가지 개선 규칙

규칙 1: API가 JavaBeans 규칙을 요구하는 Spring/JSP/JSF 환경을 대상으로 하는지 결정하세요. 그렇다면 모든 메서드가 액션 동사로 시작하도록 규칙을 엄격히 따르세요. 그렇지 않다면 규칙을 포기하세요.

규칙 2: 속성 접근이 아닌 경우 "get" 접두사 없이 속성 이름을 사용하세요. `File.getLength()`보다 `File.length()`나 `Enum.values()` 같은 예가 더 바람직합니다.

규칙 3: 실제 속성에 대해서도 "get"/"set"을 건너뛰고, 속성 이름 자체를 사용하세요:

```java
public class MyBean {
    private int myProperty;

    public int myProperty() {
        return myProperty;
    }

    public void myProperty(int myProperty) {
        this.myProperty = myProperty;
    }
}
```

이점:
- getter, setter, 속성에 대해 동일한 이름을 사용하면 텍스트 검색이 쉬워집니다
- Scala 같은 언어에서의 속성 접근과 유사합니다
- getter와 setter가 사전순 정렬에서 인접하게 나타납니다
- "get"/"is" 접두사 간의 혼란을 제거합니다
- 일관된 규칙을 따릅니다: 동사는 액션에, 명사/형용사는 객체에

규칙 4: setter의 반환 값을 고려하세요. 메서드 체이닝:

```java
public MyBean myProperty(int myProperty) {
    this.myProperty = myProperty;
    return this;
}

myBean.myProperty(1).myOtherProperty(2).andThen(3);
```

또는 이전 값을 반환:

```java
public int myProperty(int myProperty) {
    try {
        return this.myProperty;
    }
    finally {
        this.myProperty = myProperty;
    }
}
```

"void" 반환 타입은 API 범위를 낭비합니다. 특히 Java 8 람다 문법에서 반환 값이 없는 메서드는 중괄호와 세미콜론이 필요합니다.

규칙 5: 컬렉션, 맵, 참조, ThreadLocal, Future에 접근할 때처럼 getting/setting이 속성 접근이 아닌 진정한 액션인 경우에만 "get"과 "set"을 사용하세요.

## 결론

저는 API 설계자들이 지루한 JavaBeans 규칙을 넘어서도록 권장합니다. 최근 JDK와 Google/Apache API들은 "get"/"set" 사용을 최소화하고 있습니다. Java는 정적 타입 언어이므로, 표현식 언어와 의존성 주입보다는 일상적인 사용 사례에 맞게 API를 최적화하면 더 간결하고 아름다운 인터페이스를 만들 수 있습니다. Spring과 유사한 프레임워크들이 우아한 API 설계에 적응하도록 권장하며, 업계 전체에 비대함을 강요하지 않아야 합니다.

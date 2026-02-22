> 원문: https://blog.jooq.org/how-to-ensure-your-code-works-with-older-jdks/

# 이전 버전 JDK에서 코드가 작동하도록 보장하는 방법

## 개요

이 글에서는 jOOQ와 같은 라이브러리를 개발할 때 이전 버전의 JDK(Java Development Kit)와의 하위 호환성을 유지하면서 발생하는 문제점에 대해 다룹니다. 핵심 문제는 Java 8로 컴파일하면서 Java 6 호환성을 목표로 할 때, JDK 6 API 준수가 보장되지 않는다는 것입니다.

## 문제점

jOOQ는 Java 8로 컴파일되지만 Java 6을 지원합니다. 이로 인해 차이가 발생하는데, 코드가 "언어 호환성"은 가질 수 있지만 "API 호환성"은 없을 수 있습니다. 이 글에서는 `java.lang.reflect.Method.getParameterCount()`를 호출하는 코드가 있었던 특정 버그(#6860)를 예로 들고 있는데, 이 메서드는 Java 8에서 도입되어 Java 6에서는 사용할 수 없습니다.

왜 Java 6으로 직접 컴파일하지 않을까요? Java 8의 컴파일러는 제네릭(generics), 오버로딩(overloading), 가변 인수(varargs)와 관련된 수많은 엣지 케이스를 수정했습니다. 이는 "Java 언어로 가능한 것의 한계를 밀어붙이는" jOOQ의 복잡한 API에 매우 중요합니다.

## 해결책: Animal Sniffer Maven 플러그인

저자는 빌드 중 API 호환성을 검증하기 위해 Animal Sniffer Maven 플러그인을 도입했습니다. 이 플러그인은 컴파일된 코드가 목표로 하는 JDK 버전보다 새로운 버전의 메서드나 클래스를 사용하는지 확인합니다.

### Maven 설정

```xml
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>animal-sniffer-maven-plugin</artifactId>
  <version>1.16</version>
  <executions>
    <execution>
      <phase>test</phase>
      <goals>
        <goal>check</goal>
      </goals>
      <configuration>
        <signature>
          <groupId>org.codehaus.mojo.signature</groupId>
          <artifactId>java16</artifactId>
          <version>1.1</version>
        </signature>
      </configuration>
    </execution>
  </executions>
</plugin>
```

### 빌드 출력 예시

이 플러그인은 문제가 되는 API 사용을 식별하는 명확한 오류 메시지를 생성합니다:

```
[ERROR] JavaGenerator.java:232: Undefined reference: int java.lang.reflect.Method.getParameterCount()
```

## 추가 정보

댓글에서 언급된 것처럼, `-Xbootclasspath` javac 옵션을 사용하면 플러그인 없이도 특정 JDK 버전에 대해 컴파일을 강제하여 메서드 수준의 API 불일치를 잡아낼 수 있습니다. Gradle 사용자는 유사한 기능을 위해 `ru.vyarus.animalsniffer` 플러그인을 사용할 수 있습니다.

또한 Java 9 이상에서는 JEP 247을 통해 이전 플랫폼용 컴파일을 지원하며, 이를 통해 이전 JDK 명세에 대한 컴파일을 강제할 수 있습니다.

## 결론

라이브러리를 개발할 때 특정 JDK 버전과의 호환성을 유지하는 것은 단순히 언어 수준의 호환성만으로는 충분하지 않습니다. Animal Sniffer와 같은 도구를 사용하여 API 수준의 호환성도 검증해야 합니다. 이러한 자동화된 검사를 통해 런타임 테스트에만 의존하지 않고도 대상 JDK 버전에서 코드가 올바르게 작동하는지 보장할 수 있습니다.

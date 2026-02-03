# jOOQ의 코드 생성기로 패키지 프라이빗 코드를 생성하는 방법

> 원문: https://blog.jooq.org/how-to-generate-package-private-code-with-jooqs-code-generator/

Java의 패키지 프라이빗 가시성은 과소평가된 기능입니다. Java에서 가시성 수정자를 생략하면, (대부분의 객체에 대한) 기본값은 패키지 프라이빗이 됩니다. 즉, 해당 객체는 같은 패키지 내의 타입에서만 접근할 수 있습니다.

```java
class YouDontSeeMe {}
class YouDontSeeMeEither {}
```

사실 하나의 컴파일 단위(`.java` 파일)에 이러한 클래스를 여러 개 포함할 수 있습니다. 패키지 프라이빗 타입마다 별도의 파일을 만들 필요가 없습니다. 이러한 타입들을 모두 `package-info.java` 파일에 넣어도 되며, 어디에 넣든 상관없습니다.

jOOQ의 코드 생성기를 사용할 때, 기본적으로 `public` 타입으로 생성됩니다. 생성된 코드를 어디에서나 사용할 가능성이 높기 때문입니다. 원한다면 Java 9의 `module` 시스템을 사용하여 접근을 제한할 수도 있습니다.

하지만 때로는 jOOQ로 생성된 코드에서도 패키지 프라이빗 가시성이 유용할 수 있습니다. 예를 들어 특정 데이터 접근 패키지가 모듈 내의 다른 패키지로부터 구현 세부사항을 숨기고 싶을 때가 그렇습니다.

```xml
<configuration>
  <generator>
    <strategy>
      <name>com.example.codegen.SinglePackageStrategy</name>

      <!-- 모든 객체를 동일한 패키지에 생성합니다 -->
      <!-- jOOQ 3.19부터는 여기에 전략 코드를 직접 선언할 수 있습니다.
           이렇게 하면 코드 생성 설정이 간단해집니다. 이전 버전에서는
           클래스를 보조 빌드 모듈에 넣고 의존성으로 추가하면 됩니다.
        -->
      <java><![CDATA[package com.example.codegen;

import org.jooq.codegen.DefaultGeneratorStrategy;
import org.jooq.codegen.GeneratorStrategy.Mode;
import org.jooq.meta.Definition;

public class SinglePackageStrategy extends DefaultGeneratorStrategy {
    @Override
    public String getJavaPackageName(Definition definition, Mode mode) {
        return getTargetPackage();
    }
}]]></java>
    </strategy>

    <generate>

      <!-- 모든 곳에서 "public" 가시성 수정자를 제거합니다 -->
      <visibilityModifier>NONE</visibilityModifier>
    </generate>

    <target>
      <packageName>com.example</packageName>

      <!-- src/main/java에 코드를 생성하는 경우 이 설정이 중요할 수 있습니다!
           다른 패키지 디렉토리의 내용이 삭제되는 것을 방지합니다.
           또는 별도의 대상 <directory/>를 사용할 수도 있습니다.
        -->
      <clean>false</clean>
    </target>
  </generator>
</configuration>
```

그렇게 어렵지 않았죠? 이 접근 방식을 사용하면 jOOQ로 생성된 코드가 jOOQ 타입을 볼 필요가 없는 클라이언트 코드에 절대 노출되지 않도록 보장할 수 있습니다.

참고로, Kotlin의 `internal` 가시성도 `visibilityModifier` 값으로 지원됩니다.

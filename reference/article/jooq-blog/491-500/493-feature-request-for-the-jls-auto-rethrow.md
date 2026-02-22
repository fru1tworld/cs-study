# JLS를 위한 기능 요청: Auto-Rethrow

> 원문: https://blog.jooq.org/feature-request-for-the-jls-auto-rethrow/

Java 7의 [Project Coin](http://openjdk.java.net/projects/coin/)은 [try-with-resources](http://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html) 문과 [multi-catch](http://docs.oracle.com/javase/7/docs/technotes/guides/language/catch-multiple.html) 구문이라는 두 가지 훌륭한 새 기능을 도입했습니다. 이러한 기능들은 훌륭하지만, try-catch와 관련하여 저는 아직 충분히 만족하지 못하고 있습니다.

## 현재 문제점

제 예외 처리 코드에서 자주 발생하는 패턴이 있습니다:

```java
try {
    // 코드
}
catch (OurException e) {
    throw e;
}
catch (Exception e) {
    throw new OurException(e);
}
```

저는 우리의 예외를 다시 던지거나(`rethrow`), 다른 예외를 래핑(`wrap`)합니다. 이것은 일반적이고 장황한 패턴입니다. Java 7 이전에는 이것을 여러 catch 블록으로 분리해야 했습니다. Java 7 이후에는 "rethrow"와 "wrap"을 하나의 문으로 병합할 수 있지만, 별로 좋아 보이지 않습니다:

```java
try {
    // 코드
}
catch (Exception e) {
    if (e instanceof OurException)
        throw e;
    else
        throw new OurException(e);
}
```

실제로 이것은 더 많은 코드이고, `instanceof`를 사용해야 합니다.

## 제안된 솔루션

Java에는 _'rethrow Exception'_ 문이 있어야 합니다. 이 기능은 다음과 같은 구문으로 구현될 수 있습니다:

```java
try {
    // 코드
}
throws OurException;
```

이것은 `OurException` 인스턴스를 자동으로 다시 던지고, 다른 예외는 래핑합니다. `throws`의 의미는 다음과 같습니다:

- `OurException` 또는 그 서브타입의 인스턴스가 발생하면, 그대로 다시 던집니다
- 다른 모든 예외는 새로운 `OurException`으로 래핑하여 던집니다 (원래 예외를 원인으로 설정)

이 구문은 기존의 catch 블록과 함께 사용될 수도 있습니다:

```java
try {
    // 코드
}
throws OurException
catch (Exception e) {
    throw new OurException("사용자 정의 메시지", e);
}
```

이 경우:
- `OurException`은 자동으로 다시 던져집니다
- 다른 예외에 대해서는 catch 블록이 실행되어 사용자 정의 처리가 가능합니다

## 논의

한 응답자는 광범위한 `catch(Exception e)` 접근 방식에 대해 비판했습니다. 이 방식은 `RuntimeException`을 마스킹하고, 라이브러리 업데이트 후 새로 던져지는 예외 타입의 적절한 처리를 방지한다고 주장했습니다.

하지만 실제 애플리케이션에서는 이러한 "모든 것을 캐치하고 래핑" 패턴이 계층 경계에서 매우 일반적입니다. 예를 들어:

- 데이터 액세스 계층에서 모든 예외를 `DataAccessException`으로 래핑
- 서비스 계층에서 모든 예외를 `ServiceException`으로 래핑
- 컨트롤러 계층에서 모든 예외를 적절한 HTTP 응답으로 변환

이러한 패턴에서 제안된 `throws` 구문은 코드를 크게 단순화할 수 있습니다.

## 결론

이것은 JLS(Java Language Specification)에 대한 기능 요청입니다. 이 기능이 도입된다면 예외 처리 코드의 보일러플레이트를 줄이고, 코드의 의도를 더 명확하게 표현할 수 있을 것입니다. `try-with-resources`와 `multi-catch`가 Java 7에서 예외 처리를 개선한 것처럼, `auto-rethrow` 구문도 예외 래핑 패턴을 개선할 수 있습니다.

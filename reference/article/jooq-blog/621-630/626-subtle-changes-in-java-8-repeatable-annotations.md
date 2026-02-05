# Java 8의 미묘한 변화: 반복 가능한 애노테이션

> 원문: https://blog.jooq.org/subtle-changes-in-java-8-repeatable-annotations/

확장 메서드, 람다 표현식, 스트림 API와 같은 주요 기능 외에도, Java 8은 여러 미묘한 변화를 도입했습니다. 그 중 하나의 중요한 변경 사항은 동일한 애노테이션을 단일 요소에 여러 번 적용할 수 있는 기능입니다.

## 예제

```java
@Alert(role="Manager")
@Alert(role="Administrator")
public class UnauthorizedAccessException { ... }
```

## 작동 방식

이 기능이 작동하려면 `@Alert` 애노테이션이 `java.lang.annotation.Repeatable`로 메타 애노테이션 되어야 합니다. 이를 통해 동일한 대상에 애노테이션을 반복해서 사용할 수 있습니다.

## 중요한 구현 세부 사항

애노테이션 처리에 크게 의존하는 도구들은 Java 8을 수용하기 위해 업데이트가 필요합니다. `AnnotatedElement.getAnnotations()`와 같은 기존 리플렉션 메서드는 반복 가능한 애노테이션을 반환하도록 수정되지 않았습니다. 대신 새로운 메서드가 도입되었습니다:

- `AnnotatedElement.getAnnotationsByType(Class)`
- `AnnotatedElement.getDeclaredAnnotationsByType(Class)`

이 새로운 메서드들을 통해 모든 요소에서 반복된 애노테이션을 검색할 수 있습니다.

## 참고 자료

- Oracle의 반복 애노테이션 튜토리얼
- OpenJDK 개선 제안 120 (JEP 120)
- AnnotatedElement API 문서
- Repeatable 애노테이션 API 문서
- OpenJDK 기타 기능 문서

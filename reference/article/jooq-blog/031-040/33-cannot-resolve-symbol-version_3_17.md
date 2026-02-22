# jOOQ 생성 코드에서 'VERSION_3_17' 심볼을 해결할 수 없는 문제

> 원문: https://blog.jooq.org/cannot-resolve-symbol-version_3_17-in-jooq-generated-code/

## 문제

jOOQ 3.16부터 사용자들은 생성된 코드에서 다음과 같은 컴파일 오류를 만날 수 있습니다:

> "[ERROR] cannot find symbol [ERROR] symbol: variable VERSION_3_17 [ERROR] location: class org.jooq.Constants"

이 오류는 일반적으로 다른 컴파일 오류와 함께 나타나며, jOOQ 아티팩트 간의 버전 불일치를 나타냅니다.

## 근본 원인

이 오류는 두 가지 핵심 의존성의 호환되지 않는 버전에서 비롯됩니다:

- `org.jooq:jooq` (런타임 라이브러리)
- `org.jooq:jooq-codegen` (코드 생성 라이브러리)

생성된 코드에는 런타임 호환성을 확인하는 참조가 포함되어 있습니다. 생성된 코드는 다음과 같이 명시합니다: "이것이 컴파일되지 않는다면, 런타임 라이브러리가 더 오래된 마이너 릴리스를 사용하고 있기 때문입니다."

## 버전 호환성 규칙

지원되는 구성:
- 런타임 3.17.3 + 코드 생성기 3.16.9 ✓

지원되지 않는 구성:
- 런타임 3.16.9 + 코드 생성기 3.17.3 ✗

핵심 원칙: "생성된 코드는 상위 호환(forward compatible)되지만, 하위 호환(backward compatible)되지 않습니다. 런타임 API는 하위 호환되지만, 상위 호환되지 않습니다."

모범 사례: 가능하면 항상 두 라이브러리의 버전을 일치시키세요.

## 해결 방법

Maven 설정에서 버전 참조를 비활성화할 수 있습니다:

```xml
<configuration>
  <generator>
    <generate>
      <jooqVersionReference>false</jooqVersionReference>
    </generate>
  </generator>
</configuration>
```

또는, 런타임 라이브러리 버전이 코드 생성 라이브러리 버전과 일치하거나 그보다 높은지 확인하세요.

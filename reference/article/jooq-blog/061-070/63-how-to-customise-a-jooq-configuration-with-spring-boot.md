# Spring Boot로 주입된 jOOQ Configuration을 커스터마이즈하는 방법

> 원문: https://blog.jooq.org/how-to-customise-a-jooq-configuration-that-is-injected-using-spring-boot/

작성자: Lukas Eder
작성일: 2021년 12월 16일

---

Spring Boot 2.5부터 `DefaultConfigurationCustomizer` 콜백을 구현하여 jOOQ의 Configuration을 초기화 과정에서 커스터마이즈할 수 있습니다.

다음과 같이 Configuration 클래스를 생성하고 커스터마이저를 구현하는 빈을 정의하면 됩니다:

```java
import org.jooq.conf.RenderQuotedNames;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.boot.autoconfigure.jooq.*;
import org.springframework.context.annotation.*;

@Configuration
public class Config {
    @Bean
    public DefaultConfigurationCustomizer configurationCustomiser() {
        return (DefaultConfiguration c) -> c.settings()
            .withRenderQuotedNames(
                RenderQuotedNames.EXPLICIT_DEFAULT_UNQUOTED
            );
    }
}
```

이 콜백은 `DefaultConfiguration`이 초기화되는 시점에 호출되며, 이 단계에서 기본값을 안전하게 변경하거나 추가적인 설정을 적용할 수 있습니다.

이 방식을 사용하면 개발자는 다음과 같은 작업을 수행할 수 있습니다:
- 기본 설정 수정
- 커스텀 Configuration 추가
- 따옴표로 감싸진 이름(quoted names)이 렌더링되는 방식 등의 동작 제어

이 초기화는 Spring의 jOOQ 자동 구성(auto-configuration) 프로세스 내에서 발생합니다. 람다 내부에 브레이크포인트를 설정하여 초기화 타이밍을 검증할 수 있으며, 스택 트레이스를 통해 커스터마이저가 Spring Boot의 `JooqAutoConfiguration`을 통해 호출되는 것을 확인할 수 있습니다.

실제 사용 예시는 GitHub의 jOOQ-spring-boot-example 저장소를 참고하세요.

---

## 댓글

László van den Hoek: R2DBC를 사용할 때 `JooqAutoConfiguration`과의 호환성 문제가 있습니다. 기존 JDBC DataSource와 리액티브 ConnectionFactory 설정 간에 충돌이 발생할 수 있습니다.

Lukas Eder (작성자): 이 문제는 최신 버전에서 해결되었을 수 있습니다. Spring Boot 이슈 #26439를 참고해 보시기 바랍니다.

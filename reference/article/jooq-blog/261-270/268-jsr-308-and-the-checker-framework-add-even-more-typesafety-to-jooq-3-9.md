# JSR-308과 Checker Framework가 jOOQ 3.9에 더 많은 타입 안전성을 추가하다

> 원문: https://blog.jooq.org/jsr-308-and-the-checker-framework-add-even-more-typesafety-to-jooq-3-9/

## 타입 어노테이션 소개

Java 8은 어노테이션 기능을 타입 어노테이션으로 확장한 JSR-308을 도입했습니다. 이 발전으로 개발자들은 코드의 모든 타입에 어노테이션을 달아 커스텀 방식으로 타입 시스템을 향상시킬 수 있게 되었습니다. 이 기능의 주요 동기는 고급 타입 검사를 위한 정교한 컴파일러 플러그인을 가능하게 하는 것이었으며, Checker Framework가 핵심 구현 도구로 활용되고 있습니다.

## Checker Framework 설명

Checker Framework는 향상된 타입 검사를 위한 커스텀 컴파일러 플러그인 생성을 용이하게 하는 오픈소스 라이브러리입니다. 간단한 예로 null 가능성 검사가 있습니다: 개발자가 `@Nullable`로 파라미터에 어노테이션을 달아 null일 수 있음을 표시한 다음, nullness 체커로 컴파일하여 컴파일 시점에 잠재적인 null 포인터 역참조 오류를 잡을 수 있습니다.

다음은 기본적인 nullness 검사 예제입니다:

```java
import org.checkerframework.checker.nullness.qual.Nullable;

class YourClassNameHere {
    void foo(Object nn, @Nullable Object nbl) {
        nn.toString(); // OK
        nbl.toString(); // 실패
        if (nbl != null)
            nbl.toString(); // 다시 OK
    }
}
```

이 접근 방식은 Ceylon과 Kotlin에서 볼 수 있는 흐름 민감 타이핑(flow-sensitive typing)을 반영하지만, 타입 시스템 규칙을 직접 구현하는 Java 어노테이션 프로세서를 통해 더 큰 힘을 발휘합니다.

컴파일러 명령어 예제:

```bash
javac -processor org.checkerframework.checker.nullness.NullnessChecker afile.java
```

컴파일 오류 예제:

```
[dereference.of.nullable] dereference of possibly-null reference nbl:5:9
```

## PlainSQL 어노테이션과 SQL 인젝션 방지

jOOQ는 전통적으로 `@PlainSQL`을 포함한 문서화 어노테이션을 제공해왔는데, 이는 인젝션 취약점을 도입할 수 있는 원시 SQL 문자열을 받는 메서드를 표시합니다. jOOQ의 표현식 트리 접근 방식이 대부분의 인젝션 위험을 제거하지만, plain SQL API는 개발자가 실수로 취약한 코드를 작성할 수 있는 예외를 만듭니다.

안전한 접근 방식과 안전하지 않은 접근 방식의 차이는 미묘합니다—바인드 파라미터 대신 문자열 연결을 사용하는 것은 무해해 보이지만 보안 취약점을 만들어냅니다.

다음은 SQL 인젝션 위험이 있는 잘못된 사용 예제입니다:

```java
DSL.using(configuration)
   .select(level())
   .connectBy("level < " + bindValue)  // 위험! 문자열 연결 사용
   .fetch();
```

Maven의 컴파일러 플러그인에서 `PlainSQLChecker`를 구성하면, plain SQL 문자열을 사용하는 코드는 메서드, 클래스, 또는 패키지 수준에서 명시적인 `@Allow.PlainSQL` 어노테이션이 필요합니다.

### PlainSQL Checker를 위한 Maven 설정

```xml
<plugin>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.3</version>
    <configuration>
        <source>1.8</source>
        <target>1.8</target>
        <fork>true</fork>
        <annotationProcessors>
            <annotationProcessor>org.jooq.checker.PlainSQLChecker</annotationProcessor>
        </annotationProcessors>
        <compilerArgs>
            <arg>-Xbootclasspath/p:1.8</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

### 메서드 수준의 PlainSQL 사용 (허용됨)

```java
@Allow.PlainSQL
public List<Integer> iKnowWhatImDoing() {
    return DSL.using(configuration)
              .select(level())
              .connectBy("level < ?", bindValue)
              .fetch(0, int.class);
}
```

### 클래스 수준의 PlainSQL 어노테이션

```java
@Allow.PlainSQL
public class IKnowWhatImDoing {
    public List<Integer> iKnowWhatImDoing() {
        return DSL.using(configuration)
                  .select(level())
                  .connectBy("level < ?", bindValue)
                  .fetch(0, int.class);
    }
}
```

### 패키지 수준의 PlainSQL 스코프

```java
@Allow.PlainSQL
package org.jooq.example.checker;
```

`@Allow.PlainSQL` 없이 plain SQL을 사용하면 다음과 같은 컴파일 오류가 발생합니다:

```
[Plain SQL usage not allowed at current scope. Use @Allow.PlainSQL.]
```

## 데이터베이스 호환성을 위한 SQLDialect 검사

jOOQ에 대한 Checker Framework의 주요 응용 프로그램은 API 사용이 대상 데이터베이스와 일치하는지 확인하는 것입니다. `@Support` 어노테이션은 메서드가 지원하는 SQL 방언을 문서화합니다(예: `CONNECT BY`와 같은 Oracle 전용 기능). `SQLDialectChecker`를 구성함으로써 개발자는 코드베이스가 프로덕션 데이터베이스와 호환되는 메서드만 사용하도록 보장하여, 인프라에서 지원하지 않는 기능의 배포를 방지할 수 있습니다.

### SQLDialect Checker를 위한 Maven 설정

```xml
<plugin>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.3</version>
    <configuration>
        <source>1.8</source>
        <target>1.8</target>
        <fork>true</fork>
        <annotationProcessors>
            <annotationProcessor>org.jooq.checker.SQLDialectChecker</annotationProcessor>
        </annotationProcessors>
        <compilerArgs>
            <arg>-Xbootclasspath/p:1.8</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

### Maven 의존성

```xml
<dependency>
    <groupId>org.jooq</groupId>
    <artifactId>jooq-checker</artifactId>
    <version>${org.jooq.version}</version>
</dependency>
```

## Allow와 Require 어노테이션

jOOQ 3.9는 방언 제한을 강제하기 위해 `@Allow`와 `@Require` 어노테이션을 도입합니다.

- `@Allow` 어노테이션: 스코프 내에서 허용되는 방언을 지정하여 선언적(disjunctive) 집합을 만듭니다(하나라도 사용 가능).
- `@Require` 어노테이션: 동시에 지원되어야 하는 방언을 지정하여 결합적(conjunctive) 제약을 만듭니다(모두 함께 작동해야 함).

이러한 어노테이션들은 시너지 효과를 발휘하여 코드 내 데이터베이스 호환성에 대한 세밀한 제어를 제공합니다.

### 패키지 수준의 SQLDialect 제한 (단일 방언)

```java
@Allow(ORACLE)
package org.jooq.example.checker;
```

### 패키지 수준의 SQLDialect 제한 (다중 방언)

```java
@Allow({ MYSQL, ORACLE })
package org.jooq.example.checker;
```

### 필수 방언 지정

```java
@Allow
@Require({ MYSQL, ORACLE })
package org.jooq.example.checker;
```

중요: 두 개의 방언을 선언적으로 허용하는 것은 주어진 구문이 두 데이터베이스 중 하나에서 작동할 것이라고 보장하지 않습니다.

### 다중 Allow 어노테이션 (선언적 OR 로직)

```java
@Allow(MYSQL)
class MySQLAllowed {

    @Allow(ORACLE)
    void mySQLAndOracleAllowed() {
        DSL.using(configuration)
           .select()
           .connectBy("...")  // 작동: Oracle 허용됨
           .forShare();       // 작동: MySQL 허용됨
    }
}
```

### Require 어노테이션 (결합적 AND 로직)

```java
@Allow
@Require({ MYSQL, ORACLE })
class MySQLAndOracleRequired {

    @Require(ORACLE)
    void onlyOracleRequired() {
        DSL.using(configuration)
           .select()
           .connectBy("...")   // 작동: Oracle 필수
           .forShare();        // 작동하지 않음
    }
}
```

## 스코프 계층 구조

이러한 어노테이션들은 여러 스코프 수준에서 작동합니다: 개별 메서드, 전체 클래스, 그리고 `package-info.java` 파일을 통한 패키지 수준 선언. 다중 어노테이션은 결합된 제한을 만듭니다—중첩된 스코프는 부모 스코프를 상속하고 추가로 제한할 수 있습니다.

이 계층적 접근 방식은 팀이 조직 전체에 걸쳐 데이터베이스 호환성 요구사항을 선언하면서도, 개발자가 특별한 지식을 가진 특정 코드 섹션에 대해 예외를 허용할 수 있게 합니다.

## 구현과 이점

개발자는 Maven의 컴파일러 플러그인 설정에서 적절한 체커를 구성하고, 코드가 방언 제약을 위반하면 컴파일 오류를 받습니다. 이 전환은 데이터베이스 호환성을 문서화에서 강제되는 컴파일러 규칙으로 변환하여, 프로덕션 환경에서 문제를 발견하는 대신 개발 중에 즉각적인 피드백을 제공합니다.

이 기능을 통해 팀은 런타임 검사나 코드 리뷰 대신 컴파일러 검증을 통해 데이터베이스 호환성과 보안 정책을 강제할 수 있습니다.

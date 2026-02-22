# UWS-jOOQ 플러그인으로 Grails에 jOOQ 통합하기

> 원문: https://blog.jooq.org/integrating-jooq-with-grails-featuring-the-uws-jooq-plugin/

저자: lukaseder
게시일: 2015년 3월 10일 (2015년 3월 4일 업데이트)

## 소개

Grails는 개발 생산성을 향상시키기 위해 설계된 웹 프레임워크입니다. 주요 강점은 도메인 중심 데이터베이스 스키마 생성에 있으며, 애플리케이션이 시작되기 전에 기존 스키마를 자동으로 업데이트할 수 있습니다. Grails는 내장된 도메인 매퍼나 마이그레이션을 통해 이를 수행합니다. UWS-jOOQ Grails 플러그인은 jOOQ를 Grails의 기존 라이프사이클에 통합하여 jOOQ의 기능과 Grails의 강점을 결합하는 것을 목표로 합니다.

이 글은 독일에 본사를 둔 jOOQ 통합 파트너인 UWS Software Service(UWS)에서 작성되었습니다. UWS는 Java Enterprise 생태계 내에서 맞춤형 소프트웨어 개발, 애플리케이션 현대화 및 아웃소싱을 전문으로 합니다.

## 왜 Grails와 함께 jOOQ를 사용해야 할까요?

엔터프라이즈 애플리케이션은 Hibernate 성능 문제, 제한된 구문 지원, 또는 Hibernate 모델로 인한 과도한 복잡성을 자주 겪습니다. HQL은 종종 정교한 요구사항에 불충분하여 순수 SQL을 사용해야 하는 상황이 발생합니다. 그러나 순수 SQL은 여러 개발자가 참여하는 대규모 프로젝트에서 타입 안전성을 희생합니다. jOOQ 프레임워크는 타입 안전한 SQL 생성을 제공하여 이 문제를 해결하며, 이것이 UWS-jOOQ Grails 플러그인을 특히 가치 있게 만듭니다.

## jOOQ를 Grails에 어떻게 통합할 수 있나요?

이 플러그인은 Grails의 의존성 해결을 사용하여 jOOQ 통합을 단순화합니다. BuildConfig.groovy의 plugins 섹션에 다음을 추가하세요:

```
compile ':uws-jooq:0.1.1'
```

Config.groovy 파일을 설정하세요:

```groovy
jooq.dataSource = ['dataSource']
jooq.xmlConfigDir = "src/resources/jooq/"
jooq.generatedClassOutputDirectory = 'src/java'
jooq.generatedClassPackageName.dataSource = 'ie.uws.example'
```

다음으로, 기존 데이터소스를 사용하여 jOOQ의 XML 설정 파일을 생성합니다:

```
grails jooq-generate-config
```

타입 안전한 SQL Java 클래스를 생성합니다:

```
grails jooq-init
```

컨트롤러에서 jOOQ를 통해 데이터베이스 레코드를 삽입합니다:

```java
class ExampleController {
  JooqService jooqService

  def insert() {
    DSLContext dsl = jooqService.dataSource
    BookRecord record = dsl.newRecord(Tables.BOOK)
    record.author = "John"
    record.name = "Uws"
    record.version = 1
    record.store()
  }
}
```

## jOOQ와 Grails의 통합은 내부적으로 어떻게 작동하나요?

JooqService는 주요 진입점을 제공하며, 의존성 주입과 DSL 컨텍스트 생성을 제공합니다. 여러 데이터소스를 사용하는 경우, 다음과 같이 특정 데이터소스에 접근합니다:

```groovy
DSLContext dsl = jooqService.dataSource_custom
```

Grails 도메인 모델을 변경할 때마다 `jooq-init`을 실행하세요. 이 명령어는 코드를 컴파일하고, 마이그레이션을 실행하며, 최신 데이터베이스 스키마를 반영하는 Java 클래스를 재생성합니다. 이 접근 방식은 인메모리 H2 데이터베이스도 지원합니다.

## Grails와 함께 jOOQ를 사용하는 모범 사례

레거시 데이터베이스 통합: 레거시 데이터베이스를 위해 복잡한 Hibernate 매핑을 생성하는 대신, jOOQ가 즉시 데이터베이스와 통신할 수 있는 타입 안전한 Java 클래스를 생성하도록 하세요.

데이터베이스 스키마 변경이 코드를 깨뜨리게 하세요: 타입 안전성은 스키마 변경이 런타임이 아닌 컴파일 시점에 실패하도록 보장합니다. `jooq-init`을 실행하면 애플리케이션을 재컴파일하고 클래스를 재생성하여, 비호환성을 개발자에게 즉시 알립니다.

생성된 클래스를 버전 관리 시스템에 보관하세요: jOOQ가 생성한 클래스를 애플리케이션 소스 코드와 함께 커밋하여 컴파일 가용성을 보장하는 것이 권장됩니다.

## 로드맵

계획된 개선사항에는 `jooq-init`을 일반 Grails 컴파일에 연결하는 것과 서비스 및 컨트롤러를 넘어 일반 Java 클래스로 사용을 확장하는 것이 포함됩니다.

## UWS-jOOQ Grails 플러그인에 기여하기

이 소프트웨어는 Apache License, Version 2.0에 따라 배포됩니다. 프로젝트의 Git 저장소를 통해 기여를 환영합니다.

## 추가 자료

- UWS-jOOQ Grails-Plugin
- UWS-jOOQ Grails-Plugin: 문서
- UWS-jOOQ Grails-Plugin: 사용 예제
- UWS-jOOQ Grails-Plugin: 코드 저장소
- UWS-jOOQ Grails-Plugin: 예제 통합 프로젝트
- jOOQ
- UWS Software Service

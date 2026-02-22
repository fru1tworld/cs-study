# jOOQ에서 기본 설정을 정적으로 재정의하는 방법

> 원문: https://blog.jooq.org/how-to-statically-override-the-default-settings-in-jooq/

작성자: lukaseder
작성일: 2019년 3월 14일

## 개요

jOOQ 런타임 Configuration을 구성할 때, 개발자는 SQL 생성 동작을 수정하는 플래그를 포함하는 명시적인 Settings 인스턴스를 추가할 수 있습니다. 주요 설정에는 객체 한정(object qualification), 식별자 스타일, 키워드 스타일, 문장 유형(statement type), 실행 로깅 등이 있습니다.

## 문제 상황

jOOQ를 구버전 Informix와 함께 사용하는 한 고객이 문제를 겪었습니다. 해당 데이터베이스가 FROM 절에서 따옴표로 묶인 식별자를 처리할 수 없었기 때문입니다. 생성된 SQL에는 스키마와 테이블 이름 주위에 원치 않는 따옴표가 포함되어 있었습니다.

기본 jOOQ 동작은 다음과 같이 따옴표가 포함된 식별자를 생성합니다:

```sql
from "informix"."systables"
```

하지만 Informix는 따옴표가 없는 형식을 필요로 했습니다:

```sql
from informix.systables
```

## 해결 방법

### 프로그래밍 방식 접근법

Settings는 Configuration 인스턴스에서 직접 구성할 수 있습니다:

```java
new Settings().withRenderNameStyle(RenderNameStyle.AS_IS);
```

### 구성 파일 방식 접근법

클래스패스의 `/jooq-settings.xml`에 XML 파일을 배치하거나 `-Dorg.jooq.settings` 시스템 속성을 통해 위치를 지정할 수 있습니다:

```xml
<settings>
  <renderNameStyle>AS_IS</renderNameStyle>
</settings>
```

XML은 jOOQ 런타임 스키마를 준수해야 합니다.

## 추가 옵션

스키마 한정을 추가로 제거하려면:

```xml
<settings>
  <renderNameStyle>AS_IS</renderNameStyle>
  <renderSchema>false</renderSchema>
</settings>
```

이 구성은 생성되는 SQL의 상세 수준을 점진적으로 줄여주며, 구형 데이터베이스 시스템과의 호환성을 유지합니다.

예를 들어, 원래 출력이 다음과 같았다면:

```sql
"informix"."systables"."owner"
```

위 설정을 적용하면 다음과 같이 단순화됩니다:

```sql
systables.owner
```

이 접근 방식을 통해 개발자는 코드를 수정하지 않고도 기본 식별자 인용 동작을 전역적으로 재정의할 수 있으며, 특히 데이터베이스 호환성 문제가 있을 때 유용합니다.

# 완전히 개편되고 모듈화된 jOOQ 3.11, Java 11 준비 완료

> 원문: https://blog.jooq.org/a-completely-overhauled-modularised-jooq-3-11-ready-for-java-11/

*2018년 4월 1일 게시*

빠르게 진행되는 JDK 9+ 프로젝트를 따라오셨다면, Java 9의 Jigsaw 기능 덕분에 가능해진 흥미롭고 큰 첫 번째 변화를 눈치채셨을 것입니다.

JDK 11에서는 JEP 320이 출시될 예정입니다. 아니, 더 정확히 말하면 출시되지 않을 예정입니다. JEP 320은 CORBA와 Java EE 모듈(주로 JAXB)이 Java SE와 JDK에서 제거된다는 것을 의미하기 때문입니다. 정말 대단합니다!

## 축소되는 Java 플랫폼

Azul Systems의 Simon Ritter는 ["축소되는 놀라운 Java 플랫폼(The Incredible Shrinking Java Platform)"](https://www.azul.com/blog/the-incredible-shrinking-java-platform/)이라는 블로그 글을 작성했습니다. 우리도 jOOQ가 축소되기를 원합니다! 그리고 11이라는 숫자는 완벽한 기회입니다. Data Geekery에서 곧 jOOQ 3.11을 출시할 예정이고, 프로젝트 코드명은 "jOOQ 3.11 For Workgroups"입니다.

## 모듈화된 jOOQ

우리는 처음에 jOOQ를 21개의 모듈로 분리하는 것을 생각했습니다. jOOQ 3.11 버전 기준으로 21개의 RDBMS를 지원하기 때문입니다. 이것은 미래에 MongoDB, Cassandra, Hibernate 모듈을 추가할 때 매우 쉽게 확장할 수 있습니다 - 기존 모듈과 서브모듈을 복사 붙여넣기하면 바로 작동합니다.

## 벤더별 함수

우리는 약 1337개의 벤더별 함수를 지원합니다. `SUBSTRING()`, `CONCAT()`, 심지어 `SINH()` 같은 것들입니다. 또한 `COUNT()`와 `ARRAY_AGG()` 같은 집계 함수, 그리고 `ROW_NUMBER()` 같은 윈도우 함수도 있습니다. 각 데이터베이스에서 이러한 기능들이 어떻게 작동하는지 비교하는 것은 정말 멋집니다.

## 모듈별 함수

MySQL과 Oracle에서만 `SUBSTRING()`과 `CONCAT()` 쿼리를 실행하고 싶다고 가정해봅시다. 다음과 같이 4개의 모듈만 가져오면 됩니다:

```java
module com.example {
    requires org.jooq.oracle.substring;
    requires org.jooq.oracle.concat;
    requires org.jooq.mysql.substring;
    requires org.jooq.mysql.concat;
}
```

이 접근 방식의 아름다움은 미래에 LPAD(left pad) 모듈을 쉽게 제거할 수 있다는 것입니다. 현대 모듈 시스템에서는 이것이 일반적인 관행입니다.

## 데이터베이스 간 번역

jOOQ 코드를 MySQL에서 Oracle로 번역하고 싶다면, `module-info.java`에서 `s/mysql/oracle`로 검색 치환하면 됩니다. 끝입니다!

## 렌더링 모듈

다음을 위한 모듈들도 있습니다:

- 이름과 식별자를 UPPER_CASE, lower_case, PascalCase로 렌더링
- 이름과 식별자를 "큰따옴표", \`백틱\`, [대괄호], 또는 따옴표 없이 렌더링
- SQL 키워드를 UPPER CASE로 렌더링 (기억하세요: 엄격하고 자신감 있는 SQL은 데이터베이스가 긴급함을 감지하고 SQL을 더 빠르게 실행하도록 도와줍니다)

## 코드 생성기

jOOQ 코드 생성기는 스키마/테이블당 하나의 모듈을 생성하고, 각 테이블에 대해 컬럼당 하나의 서브모듈을 생성합니다. 이렇게 하면 필요하지 않은 개별 컬럼을 무시할 수 있습니다.

패키지/프로시저/인수에 대해서도 동일합니다. 기본값이 있는 인수의 모듈을 요구하지 않으면 해당 인수를 제외할 수 있습니다.

## SQL 절 모듈

각 SQL 절에는 자체 모듈이 있습니다. jOOQ는 쿼리를 생성할 때 모듈 경로를 통해 사용 가능한 것을 확인합니다:

- SELECT 모듈
- FROM 모듈
- JOIN 모듈
- WHERE 모듈
- GROUP BY 모듈
- HAVING 모듈
- ORDER BY 모듈
- 그 외 다수

## 결론

우리는 JDK에서 프로젝트 Valhalla와 프로젝트 Amber가 1-2년 안에 어떤 기능을 출시할지 기대하고 있습니다. 계속 지켜봐 주세요, 내년 같은 날에 만나요!

---

*주의: 이 글은 2018년 4월 1일(만우절)에 게시된 유머 글입니다. 태그에 "화나기 전에 날짜를 확인하세요(check the date before you get angry)"와 "재미(fun)"가 포함되어 있습니다. 실제 jOOQ 3.11은 2018년 6월에 출시되었으며, Java 8 배포판에 Automatic-Module-Name 명세를 포함하여 향후 모듈화된 jOOQ 배포판과의 전방 호환성을 제공했습니다.*

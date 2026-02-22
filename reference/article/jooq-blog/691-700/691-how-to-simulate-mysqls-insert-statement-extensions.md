# MySQL의 INSERT 문 확장 기능을 시뮬레이션하는 방법

> 원문: https://blog.jooq.org/how-to-simulate-mysqls-insert-statement-extensions/

게시일: 2012년 5월 1일
작성자: lukaseder

## 개요

이 글에서는 MySQL의 INSERT 문 확장 기능과 jOOQ가 이러한 기능을 다양한 데이터베이스 시스템에서 어떻게 구현하는지 설명합니다.

## MySQL INSERT 확장 기능

MySQL은 두 가지 핵심 INSERT 문 확장 기능을 지원합니다:

1. IGNORE: 중복 키를 삽입하려고 할 때 경고와 함께 조용히 실패합니다
2. ON DUPLICATE KEY UPDATE: 중복 키가 발생했을 때 실패하는 대신 업데이트를 수행합니다

기본 문법 구조는 이러한 옵션들을 표준 INSERT 작업과 결합합니다.

## jOOQ 구현

jOOQ는 이러한 MySQL 기능을 API를 통해 모델링하여 개발자들이 데이터베이스에 구애받지 않는 코드를 작성할 수 있게 합니다. 두 가지 주요 메서드가 제공됩니다:

예제 1 - 중복 시 업데이트:
```java
create.insertInto(AUTHOR, AUTHOR.ID, AUTHOR.LAST_NAME)
      .values(3, "Smith")
      .onDuplicateKeyUpdate()
      .set(AUTHOR.LAST_NAME, "Smith")
      .execute();
```

예제 2 - 중복 무시:
```java
create.insertInto(AUTHOR, AUTHOR.ID, AUTHOR.LAST_NAME)
      .values(3, "Smith")
      .onDuplicateKeyIgnore()
      .execute();
```

## 데이터베이스 간 변환

네이티브 지원이 없는 데이터베이스(예: Oracle)의 경우, jOOQ는 이러한 문장들을 MERGE 작업으로 변환합니다:

ON DUPLICATE KEY UPDATE는 다음과 같이 렌더링됩니다:
```sql
merge into "AUTHOR"
using (select 1 from dual)
on ("AUTHOR"."ID" = 3)
when matched then update set "LAST_NAME" = 'Smith'
when not matched then insert ("ID", "LAST_NAME") values (3, 'Smith')
```

ON DUPLICATE KEY IGNORE는 다음과 같이 렌더링됩니다:
```sql
merge into "AUTHOR"
using (select 1 from dual)
on ("AUTHOR"."ID" = 3)
when not matched then insert ("ID", "LAST_NAME") values (3, 'Smith')
```

## 결론

이 접근 방식을 통해 개발자들은 여러 데이터베이스를 효율적으로 대상으로 하면서도 간결하고 읽기 쉬운 코드를 유지할 수 있습니다.

# jOOQ 파서 무시 주석 구문
> 원문: https://blog.jooq.org/the-jooq-parser-ignore-comment-syntax/

## 문제점

jOOQ 파서는 모든 SQL 구문을 파싱할 수 없습니다. 예를 들어, 다음 PostgreSQL 명령어는 오류를 발생시킵니다:

```sql
ALTER SYSTEM RESET ALL
```

오류 메시지: "DOMAIN, INDEX, SCHEMA, SEQUENCE, SESSION, TABLE, TYPE, or VIEW expected"

## 배경

파서의 목적은 모든 벤더별 구문을 이해하는 것이 아닙니다. jOOQ가 지원하고 방언(dialect) 간에 변환할 수 있는 구문만 처리합니다. 많은 벤더별 명령어들은 다른 데이터베이스에 동등한 기능이 없습니다.

## 해결책: 무시 주석 구문

데이터베이스 마이그레이션 시뮬레이션, 스키마 비교, DDL 코드 생성을 위해 jOOQ 파서를 사용할 때 벤더별 SQL을 건너뛰어야 할 수 있습니다. 해결책은 `Settings.parseIgnoreComments`를 활성화하고 특별한 주석 마커를 사용하는 것입니다.

## 구현 예제 1: 전체 명령어 제외

```sql
CREATE TABLE a (i int);

/* [jooq ignore start] */
ALTER SYSTEM RESET ALL;
/* [jooq ignore stop] */

CREATE TABLE b (i int);
```

동작 방식:
- RDBMS는 전체 스크립트를 변경 없이 보고 실행합니다
- jOOQ 파서는 주석 처리된 섹션을 무시하고 `CREATE TABLE` 문만 처리합니다

## 구현 예제 2: 부분 표현식 제외

```sql
CREATE TABLE t (
  a int
    /* [jooq ignore start] */
    DEFAULT some_fancy_expression()
    /* [jooq ignore stop] */
);
```

이 기법을 사용하면 표준 SQL 문 내에서 벤더별 표현식을 제외할 수 있습니다.

## 커스터마이징

다음 설정을 통해 마커 토큰을 사용자 정의할 수 있습니다:
- `Settings.parseIgnoreCommentStart`
- `Settings.parseIgnoreCommentStop`

## 주요 장점

이 접근 방식은 "완전히 투명"하며 명령어 구문 내에서도 작동하여, 전체 데이터베이스 호환성을 유지하면서 jOOQ가 파싱하는 내용을 세밀하게 제어할 수 있습니다.

## 추가 자료

더 자세한 내용은 jOOQ 매뉴얼의 커스텀 파서 설정 섹션을 참조하세요.

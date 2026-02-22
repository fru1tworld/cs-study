# Jbang으로 jOOQ 빠르게 시도하기!

> 원문: https://blog.jooq.org/quickly-trying-out-jooq-with-jbang/

## 개요

이 블로그 글은 "자체 포함된 소스 전용 Java 프로그램을 생성, 편집, 실행"할 수 있는 도구인 jbang과 함께 jOOQ를 사용하여 데이터베이스 쿼리 실험을 빠르게 수행하는 방법을 보여줍니다.

## 설치

jbang을 설정하려면:

```bash
curl -Ls https://sh.jbang.dev | bash -s - app setup
```

## 빠른 시작 예제

jOOQ 예제를 클론하고 실행합니다:

```bash
git clone https://github.com/jOOQ/jbang-example
cd jbang-example
jbang Example.java
```

이 도구는 자동으로 의존성(jOOQ와 H2 데이터베이스)을 해결하고, JAR를 빌드하고, 프로그램을 실행합니다. Maven이나 Gradle 설정이 필요 없습니다.

## 샘플 출력

이 예제는 윈도우 함수가 포함된 복잡한 SQL 쿼리를 생성하고 저자 이름, 책 제목, 서점 정보를 보여주는 포맷된 테이블 형태의 결과를 반환합니다.

## CLI 도구

기본 실행 외에도, jbang을 통해 세 가지 jOOQ 명령줄 유틸리티에 접근할 수 있습니다:

코드 생성기:
```bash
jbang codegen@jooq db.xml
```

Diff 도구:
```bash
jbang diff@jooq -T MYSQL -1 "create table t (i int);" -2 "create table t (i int, j int);"
```

파서 도구:
```bash
jbang parser@jooq -T MYSQL -s "create table t (i int generated always as identity);"
```

## 상용 버전

평가판 또는 프로 라이선스 사용자는 접미사를 사용하여 상용 데이터베이스 방언에 접근할 수 있습니다: `-trial`, `-trial-java-8`, `-trial-java-11`, `-pro`, `-pro-java-8`, 또는 `-pro-java-11`.

예시:
```bash
jbang parser-trial@jooq -T SQLSERVER -s "create table t (i int generated always as identity);"
```

## 핵심 요점

이 접근 방식은 빌드 도구의 복잡성을 제거하면서 jOOQ 기능과 데이터베이스 작업에 즉시 접근할 수 있게 해줍니다.

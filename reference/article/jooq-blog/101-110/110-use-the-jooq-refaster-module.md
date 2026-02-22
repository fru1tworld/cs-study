# jOOQ-Refaster 모듈로 더 이상 사용되지 않는 jOOQ API에서 자동 마이그레이션하기

> 원문: https://blog.jooq.org/use-the-jooq-refaster-module-for-automatic-migration-off-of-deprecated-jooq-api/

게시일: 2020년 2월 25일
수정일: 2020년 10월 28일
저자: lukaseder

---

## 지원 중단 안내

다양한 JDK 버전에서 jOOQ-refaster 모듈을 빌드하는 데 수많은 문제가 발생했고, 커뮤니티로부터 이 기능에 대한 피드백을 전혀 받지 못한 후, jOOQ 3.15에서 이 기능을 다시 제거하기로 결정했습니다: https://github.com/jOOQ/jOOQ/issues/10803

---

## 소개

jOOQ 3.13부터, 자동 API 마이그레이션을 위한 refaster 템플릿을 제공하는 jOOQ Refaster라는 새로운 모듈이 제공됩니다.

## Refaster란 무엇인가?

Refaster는 Java 컴파일러 플러그인으로 구현된 정적 코드 분석 라이브러리인 Google Error Prone의 덜 알려진 하위 프로젝트입니다. jOOQ Checker 모듈을 통해 여러 정적 코드 분석 도구에 대한 지원이 이미 존재하며, 이는 Checker Framework와 Google의 Error Prone 모두에서 작동합니다. Refaster는 코드에서 문제가 있는 API 사용 패턴을 식별하고 이러한 패턴을 수정하는 `.patch` 파일을 자동으로 생성하여 개선된 코드를 만들어냅니다.

## jOOQ가 제공하는 것

수년에 걸쳐 jOOQ는 새로운 대안이 등장하면서 상당한 양의 API를 더 이상 사용되지 않는 것으로 표시해 왔습니다. 한 예로 `ResultQuery.fetchLazy(int)` 메서드가 있는데, 여기서 `int` 매개변수는 JDBC에 전달되는 JDBC `fetchSize`를 나타냅니다. 이제 더 나은 API 대안이 사용 가능합니다:

- 전역 `fetchSize` 구성과 함께 `Settings` 값을 지정
- `fetchLazy()` 또는 모든 `fetch()` 호출과 결합되는 `ResultQuery.fetchSize(int)` 사용, 이 기능은 지연 페칭에만 국한되지 않기 때문

Javadoc에는 다음과 같이 표시되어 있습니다: "Deprecated. [#2811] – 3.3.0 – 대신 fetchSize(int)와 fetchLazy()를 사용하세요."

## 작동 방식

사용자는 https://www.jooq.org/refaster 에서 자신의 jOOQ 버전에 맞는 refaster 파일을 다운로드하고, Java 컴파일러가 ErrorProne의 refaster 라이브러리를 포함하도록 구성(jOOQ 매뉴얼에 문서화되어 있음)하면 자동 패치를 받을 수 있습니다.

### Before 예제

```java
public void runQuery() {
    try (Cursor<?> cursor = ctx.select(T.A)
       .from(T)
       .fetchLazy(42)) {
        // 무언가를 수행
    }
}
```

### 패치 출력

```
--- ..\\..\\src\\main\\java\\org\\jooq\\refaster\\test\\RefasterTests.java
+++ ..\\..\\src\\main\\java\\org\\jooq\\refaster\\test\\RefasterTests.java
@@ -62,6 +62,5 @@
 public void runQuery() {
     try (Cursor<?> cursor = ctx.select(T.A)
-       .from(T)
-       .fetchLazy(42)) {
+       .from(T).fetchSize(42).fetchLazy()) {
         // 무언가를 수행
     }
```

### After 예제

```java
public void runQuery() {
    try (Cursor<?> cursor = ctx.select(T.A)
       .from(T).fetchSize(42).fetchLazy()) {
        // 무언가를 수행
    }
}
```

## 의견

모든 라이브러리는 사용자가 버전 간에 더 쉽게 업그레이드할 수 있도록 refaster 파일(또는 유사한 기술)을 제공해야 합니다.

## 다음 단계

더 이상 사용되지 않는 API 사용을 업데이트하는 것 외에도, jOOQ는 SQL 언어 이해와 모범 사례를 기반으로 한 정적 코드 분석 및 최적화를 실험하고 있습니다. 예를 들어, 이전에 블로그에서 다뤘듯이, 단순한 `EXISTS()` 검사로 충분할 때 `COUNT(*)` 쿼리를 사용하는 것은 권장되지 않습니다. jOOQ 3.13은 이미 다음과 같은 호출을 대체하는 내부 검사 기능을 제공합니다:

- `ctx.fetchCount(table) != 0`
- `ctx.fetchCount(table) >= 1` (그리고 더 많은 변형들)

더 성능이 좋은 대안으로:
- `ctx.fetchExists(table)`

향후 추가적인 개선이 예상됩니다.

---

## 태그

Checker Framework, ErrorProne, jooq, Refaster, 정적 코드 분석

## 관련 링크

- https://www.jooq.org/doc/latest/manual/tools/refaster/
- https://www.jooq.org/refaster
- https://errorprone.info/docs/refaster
- https://blog.jooq.org/avoid-using-count-in-sql-when-you-could-use-exists/
- https://blog.jooq.org/jooq-3-13-released-with-more-api-and-tooling-for-ddl-management/

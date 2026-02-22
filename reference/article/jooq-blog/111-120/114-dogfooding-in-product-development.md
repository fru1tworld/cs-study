# 제품 개발에서의 독 푸드 먹기

> 원문: https://blog.jooq.org/dogfooding-in-product-development/

2019년 10월 25일 lukaseder 작성

독 푸드 먹기(Dogfooding), 즉 "자신의 개 사료를 직접 먹는다"는 것은 모든 제품 개발자가 실천해야 하는 관행입니다. 위키피디아에 따르면:

"독 푸드 먹기는 조직이 자신의 제품을 직접 사용하는 것을 말합니다. 이는 조직이 실제 사용 환경에서 자사 제품을 테스트하는 방법이 될 수 있습니다."

저는 최근 컨퍼런스에서 API 설계에 관한 발표를 했는데, 그 자리에서 독 푸드 먹기를 훌륭한 사용자 경험을 보장하는 접근법으로 언급했습니다. 원칙은 다음과 같습니다: "자신의 API를 더 많이 사용할수록, UX와 사용성 관점에서 더 좋아집니다."

### 테스트를 통한 독 푸드 먹기

jOOQ는 26개의 지원되는 RDBMS에 걸쳐 광범위한 테스트를 구현합니다. 새로운 API에 대한 테스트를 작성하면 초기 사용성 문제를 발견하는 데 도움이 되지만, 더 나은 접근법들이 존재합니다.

### 중요한 새 기능을 통한 독 푸드 먹기

jOOQ 3.13은 DDL 해석의 일부로 스키마 비교(diff) 도구를 제공할 예정입니다. `Meta` 타입은 데이터베이스 스키마를 나타내며 다양한 메서드를 통해 조작할 수 있습니다.

예제 1 - 스키마 스냅샷 구축:

```java
System.out.println(
  ctx.meta("")
     .apply("create table t (i int)")
     .apply("alter table t add j int")
     .apply("alter table t alter i set not null")
     .apply("alter table t add primary key (i)")
);
```

출력:
```sql
create table t(
  i int not null,
  j int null,
  primary key (i)
);
```

예제 2 - diff 도구 사용:

```java
System.out.println(
  ctx.meta(
    "create table t (i int)"
  ).migrateTo(ctx.meta(
    "create table t ("
  + "i int not null, "
  + "j int null, "
  + "primary key (i))"
  ))
);
```

출력:
```sql
alter table t alter i set not null;
alter table t add j integer null;
alter table t add constraint primary key (i);
```

### 독 푸드 먹기를 통한 발견

이 기능을 구현하면서 누락된 기능들이 드러났습니다:

- `MAXVALUE`와 같은 시퀀스 플래그가 저장되지 않았음
- `Meta.toString()`이 더 나은 디버깅을 위해 `Meta.ddl()`을 활용할 수 있었음
- `Meta.equals()`와 `Meta.hashCode()` 구현이 필요했음
- DDL 내보내기가 알파벳 순서 재정렬을 지원해야 했음
- `ALTER SEQUENCE` 문 지원이 불완전했음
- `ALTER SEQUENCE .. RESTART`에 하드코딩된 값이 있었음
- 이름 없는 외래 키를 삭제하기 위한 합성 구문이 필요했음

이러한 작은 기능들은 실제 사용 패턴에서 나타났습니다.

### 블로깅과 문서화를 통한 독 푸드 먹기

문서화는 벤더를 사용자의 관점으로 이끕니다. 초기 API 설계에는 종종 불필요한 복잡성이 포함되어 있었습니다.

원래 접근 방식:
```java
System.out.println(
  ctx.meta("create table t (i int);\n"
+ "alter table t add j int;\n"
+ "alter table t alter i set not null;\n"
+ "alter table t add primary key (i);")
);
```

독 푸드 먹기를 통해 개선됨:
```java
System.out.println(
  ctx.meta("")
     .apply("create table t (i int)")
     .apply("alter table t add j int")
     .apply("alter table t alter i set not null")
     .apply("alter table t add primary key (i)")
);
```

편의 메서드 `Meta.apply(String)`이 추가되었습니다:

```java
public final Meta apply(String diff) {
  return apply(dsl().parser().parse(diff));
}
```

### 편의 API 설계

추상화에 대한 Brian Goetz의 원칙은 여전히 유효합니다: API는 API 자체가 아니라 사용자를 위해 봉사해야 합니다. Java 9의 `InputStream.transferTo()`와 같은 편의 기능은 반복적으로 작성되는 패턴을 해결합니다.

### 결론

독 푸드 먹기는 벤더가 편의성 요구사항을 조기에 발견할 수 있게 해줍니다. 첫 번째 사용자로서, 제품 제작자는 자신이 개인적으로 필요한 기능을 구현함으로써 이익을 얻고, 궁극적으로 더 나은 설계를 통해 모든 사용자에게 혜택을 줍니다.

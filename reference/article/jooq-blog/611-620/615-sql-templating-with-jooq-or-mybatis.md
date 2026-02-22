# jOOQ 또는 MyBatis를 이용한 SQL 템플릿

> 원문: https://blog.jooq.org/sql-templating-with-jooq-or-mybatis/

2013년 7월 13일 lukaseder 작성

---

많은 사람들이 jOOQ를 MyBatis와 비교합니다. 두 도구 모두 JPA 자체보다 훨씬 더 SQL 중심적이기 때문에 Java의 영속성 표준인 JPA의 인기 있는 대안으로 여겨집니다. 두 도구를 비교할 때 첫 번째 명확한 차이점은 다음과 같습니다:

- jOOQ는 Java 플루언트 API를 통해 SQL을 모델링하는 내부 도메인 특화 언어입니다.
- MyBatis는 XML 기반 SQL 템플릿 및 매핑 엔진으로, XML-DSL을 통해 동적 SQL을 생성할 수 있습니다.

MyBatis의 현재 성공은 주로 JPA가 여전히 논쟁의 여지가 있는 표준이었고, JPA가 매우 유사한 문제를 해결하는 JDO보다 더 나은지 증명해야 했던 시기에 JPA에 대한 실행 가능한 대안을 제공했다는 데 기반합니다. 이 제공된 대안은 많은 SQL 지향적인 사용자들이 갖고 싶어하는 것입니다:

- Java 코드와 SQL 코드의 분리, SQL 코드를 외부 파일로 추출. 이를 통해 DBA가 운영 환경에서 SQL 문자열을 패치하고, 힌트 및 기타 튜닝을 추가할 수 있습니다.
- 테이블 형태의 결과 집합 데이터를 객체로 자동 매핑. 이것 또한 동적 SQL 명세와 동일한 XML DSL로 달성됩니다.

### jOOQ로 SQL 템플릿 구현하기

이러한 것들은 jOOQ로도 달성할 수 있습니다. 그러나 MyBatis와 달리, jOOQ의 SQL 템플릿(jOOQ 3.2에서 도입될 예정)은 독자적인 템플릿 언어를 사용하지 않습니다. 여러분은 자신만의 언어를 선택할 수 있어야 하며, jOOQ에 매우 간단한 어댑터를 제공하면 됩니다. 이를 통해 다음을 사용할 수 있습니다:

- Apache Velocity
- StringTemplate
- Freemarker
- Eclipse의 Xtend
- XSLT
- ... 또는 MyBatis에서 jOOQ로 마이그레이션할 때 MyBatis XML 파일을 위한 자체 제작 템플릿 어댑터까지

Velocity 템플릿 예제를 살펴보겠습니다. 이 예제는 WHERE 절에 동적 ID 파라미터 목록을 추가합니다:

```
SELECT
  a.first_name,
  a.last_name,
  count(*)
FROM
  t_author a
LEFT OUTER JOIN
  t_book b ON a.id = b.author_id
WHERE
  1 = 0
#foreach ($param in $p)
  OR a.id = ?
#end
GROUP BY
  a.first_name,
  a.last_name
ORDER BY
  a.id ASC
```

위의 템플릿은 다음 jOOQ Template 구현에 전달될 수 있으며, 임의의 입력 객체를 사용하여 구체적인 jOOQ QueryPart를 생성합니다. QueryPart는 SQL과 바인드 변수를 렌더링할 수 있는 객체입니다:

```java
class VelocityTemplate
implements org.jooq.Template {

  private final String file;

  public VelocityTemplate(String file) {
    this.file = file;
  }

  @Override
  public QueryPart transform(Object... input) {

    // Velocity 코드
    // -----------------------------------------
    URL url = this.getClass().getResource(
      "/org/jooq/test/_/templates/");
    File file = url.getFile();

    VelocityEngine ve = new VelocityEngine();
    ve.setProperty(RESOURCE_LOADER, "file");
    ve.setProperty(FILE_RESOURCE_LOADER_PATH,
      new File(file ).getAbsolutePath());
    ve.setProperty(FILE_RESOURCE_LOADER_CACHE,
      "true");
    ve.init();

    VelocityContext context = new VelocityContext();
    context.put("p", input);

    StringWriter writer = new StringWriter();
    ve.getTemplate(file, "UTF-8")
      .merge(context, writer);

    // jOOQ 코드
    // -----------------------------------------
    return DSL.queryPart(writer.toString(), input);
  }
}
```

매우 간단한 접착 코드입니다. 템플릿 엔진 구현 어댑터를 완전히 제어할 수 있으므로 어댑터에 캐싱, 객체 풀 등을 추가할 수도 있습니다. 위의 템플릿은 jOOQ가 일반 SQL을 허용하는 곳이라면 jOOQ API 전반에서 쉽게 사용할 수 있습니다. 예를 들어 최상위 쿼리로:

```java
Template tpl = new VelocityTemplate(
    "authors-and-books.vm");

DSL.using(configuration)
   .resultQuery(tpl, 1, 2, 3)
   .fetch();
```

또는 jOOQ의 타입 안전 DSL에 포함된 중첩 select로:

```java
DSL.using(configuration)
   .select()
   .from(new TableSourceTemplate("my-table.vm"))
   .fetch();
```

물론, jOOQ의 레코드 매핑 기능도 활용할 수 있으며, 이를 통해 자체 커스텀 테이블-객체 매핑 알고리즘을 구현할 수 있습니다. 이것은 MyBatis의 것과 같은 하드코딩된 XML 설정에 의존하는 것보다 더 나은 선택일 수 있습니다:

```java
List<MyType> result =
DSL.using(configuration)
   .select()
   .from(new TableSourceTemplate("my-table.vm"))
   .fetch(new RecordMapper<Record, MyType>() {
      public MyType map(Record record) {
        // 커스텀 매핑 로직을 여기에
      }
   });
```

또는 Java 8에서:

```java
List<MyType> result =
DSL.using(configuration)
   .select()
   .from(new TableSourceTemplate("my-table.vm"))
   .fetch((Record) -> new MyType().init(record));
```

### 가능성은 무궁무진합니다

SQL 템플릿은 간단한 문자열 기반 SQL을 선호할 때 강력한 도구입니다. 동적 SQL 절을 주입하기 위해 가끔 약간의 루프나 if 문으로 조정할 수 있습니다. 이 문제를 어떤 방식으로든 해결하려는 몇 가지 SQL 엔진이 있습니다:

- MyBatis - 선도적인 Java SQL 템플릿 엔진
- jOOQ - 선도적인 Java SQL 빌더 및 SQL 언어 통합
- ElSql - Stephen Colebourne이 구현한 경량 도구
- JIRM - Adam Gent가 만든 경량 도구

위의 도구들 중 모든 도구가 간단하고 독자적인 템플릿 언어를 제공하지만, jOOQ는 여러분이 원하는 템플릿 엔진을 사용하도록 권장하는 유일한 도구이며, 따라서 미래에 임의의 템플릿 확장성을 제공합니다.

---

## 댓글 섹션

agentgt (2013년 7월 13일 13:52):
훌륭한 글이에요 Lukas, JIRM을 언급해 주셔서 감사합니다. 저는 SQL 템플릿의 열렬한 팬이지만 항상 로직이 없는(mustache처럼) SQL 템플릿 언어를 원했고, SQL 쿼리 도구와 클래스패스 SQL 리소스 사이에서 복사하여 붙여넣기할 수 있기를 원했습니다.

그래서 저만의 SQL 템플릿 언어를 만들었습니다: https://github.com/agentgt/jirm/blob/master/jirm-core/README.md

다음과 같이 SQL 파라미터화를 할 수 있습니다:

```
    INSERT INTO test_bean
      (string_prop, long_prop, timets)
      VALUES (
        'HELLO' -- {stringProp}
      , 3000 -- {longProp}
      , now() -- {timeTS}
      )
```

(힌트: 'HELLO', 3000 그리고 now()는 ?로 대체됩니다).

재사용할 부분적인 SQL 블록도 정의할 수 있습니다(즉, 같은 필드 목록에 대해 반복해서 select할 필요가 없습니다).

파서는 직접 작성했고 아마 버그가 있을 수 있지만 안전한 템플릿 로직을 위해 SQL 주석을 활용한다는 아이디어는 이해하실 겁니다.

lukaseder (2013년 7월 13일 14:01):
Adam, 반갑네요. 도구와 실제 SQL 인터프리터 간에 "상호 운용 가능한" SQL을 갖는다는 당신의 아이디어를 기억하고, 당시에도 꽤 영리하다고 생각했습니다. 더 신뢰할 수 있는(하지만 조금 더 번거로운) 변수 선언 방법은 다중 행 주석을 사용하는 것일 겁니다. 예를 들어

```
INSERT INTO test_bean
  (string_prop, long_prop, timets)
VALUES (
  /* {stringProp} */ 'HELLO' /* {/stringProp} */
, /* {longProp}   */ 3000    /* {/longProp}   */
, /* {timeTS}     */ now()   /* {/timeTS}     */
)
```

jOOQ 3.2의 템플릿 기능의 좋은 점은 어떤 템플릿 동작이든 jOOQ에 주입할 수 있다는 것입니다. 일부 사용자가 원한다면 jOOQ/JIRM 통합의 여지도 있을 수 있습니다!

ericjs (2013년 7월 13일 19:44):
유용한 글이네요! 템플릿 엔진 목록에서 (제 생각에) 가장 좋은 것을 놓쳤네요, StringTemplate입니다. http://www.stringtemplate.org/ (참고로 Adam의 "로직이 없는" 요구 사항에 맞는다고 생각합니다. 이것이 ST의 엄격한 뷰/모델 분리와 동등하다고 올바르게 이해한다면요).

lukaseder (2013년 7월 14일 09:18):
물론이죠! StringTemplate을 포함하도록 글을 수정하겠습니다

Eduardo Macarron (2013년 7월 16일 18:52):
안녕하세요 Lukas. 좋은 글이고 jOOQ 3.2의 멋진 기능이네요

MyBatis의 가장 강력한 점 중 하나는 다른 것들에게는 더 큰 제한입니다. MyBatis가 SQL을 외부화하기 때문에 동적 쿼리를 구축하는 데 Java를 사용할 수 없습니다(*). 따라서 컴파일 시 타입 체크, 자동 완성 등 꽤 중요한 것들을 잃게 됩니다... 그래서 템플릿 엔진이 이에 잘 맞습니다.

(*) MyBatis는 "providers"와 쿼리 언어로 이를 부분적으로 해결합니다(Adam이 3.2에서 플루언트하게 개선했습니다 :)) 하지만 이것은 MyBatis를 사용하는 일반적인 방법은 아닙니다.

하지만 공정하게 말하자면, 지금 사용하는 사람은 매우 적고 미래에 대규모 사용을 기대하지는 않지만, MyBatis는 3.2에서 플러그형 템플릿 엔진을 도입했습니다(우연의 일치네요!). 이미 릴리스된 velocity 플러그인이 있습니다: http://mybatis.github.io/velocity-scripting/

lukaseder (2013년 7월 16일 20:41):
Eduardo, 연락해 주셔서 감사합니다.

타입 안전하고 내장된 SQL이 정말로 찾고 있는 것이라면, jOOQ를 사용하는 것이 좋을 것 같습니다. MyBatis의 접근 방식을 봤고, Adam의 개선에도 불구하고 애플리케이션에 많은 가치를 더할 것이라고 확신하지 않습니다... 물론, 여기서 제 의견은 조금 편향되어 있습니다 :-)

MyBatis의 Velocity 지원에 대해 알게 되어 좋네요. 프로젝트 팀에 계신 건가요? 아마도 몇 가지 공동 블로그 글을 작성하여 Velocity SQL 템플릿 접근 방식을 좀 더 대중화할 수 있을까요? 이 주제에 대해 분명히 다시 포스팅할 것이고, MyBatis도 Velocity를 지원한다는 사실을 홍보하겠습니다. 결국, 저(또는 jOOQ 사용자)가 jOOQ 내에서 MyBatis XML 파일을 지원하는 것을 검토할 수도 있을 겁니다.

Frank D. Martinez M. (2013년 7월 17일 18:27):
좋은 글이네요. 참고로, mybatis-scala 프로젝트도 있고 멋진 SQL 임베딩이 있습니다. 이 예제를 보세요:
https://code.google.com/p/mybatis/source/browse/sub-projects/scala/trunk/mybatis-scala-samples/src/main/scala/org/mybatis/scala/samples/select/SelectSample.scala

lukaseder (2013년 7월 21일 09:09):
맞아요, Scala에서는 다중 행 문자열을 지원하기 때문에 SQL 임베딩이 쉽습니다. MyBatis 팀에서 개선된 Scala 통합을 더 깊이 파고들 계획이 있나요?

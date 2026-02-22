# 모든 사람이 읽어야 할 10가지 SQL 기사

> 원문: https://blog.jooq.org/10-sql-articles-everyone-must-read/

우리는 jOOQ 블로그에서 Java와 SQL에 대해 꽤 오랫동안 블로깅을 해왔습니다. 수년에 걸쳐 흥미로운 블로그 주제를 조사하면서, 우리는 블로고스피어에서 우리의 작업과 SQL에 대한 열정에 영감을 준 많은 SQL 보석들을 발견했습니다. 오늘, 우리는 여러분이 반드시 읽어야 한다고 생각하는 10가지 기사 목록을 소개합니다.

---

## 1. Joe Celko: "Divided We Stand: The SQL of Relational Division"

URL: https://www.simple-talk.com/sql/t-sql-programming/divided-we-stand-the-sql-of-relational-division/

관계형 나눗셈(relational division)은 관계 대수(relational algebra)에서 가장 강력한 연산 중 하나입니다. 곱셈의 역연산처럼 작동하며, "특정 집합의 모든 과목을 수강한 모든 학생 찾기"와 같은 질문에 답할 수 있게 해줍니다. 불행히도, SQL에는 나눗셈에 대한 직접적인 동등 연산자가 없습니다. 나눗셈을 표현하는 여러 가지 창의적인 방법이 있으며, Joe Celko는 그의 기사에서 이러한 다양한 구현 방법들을 보여줍니다.

---

## 2. Alex Bolenok: "Happy New Year!"

URL: http://explainextended.com/2009/12/31/happy-new-year/

Alex Bolenok(Quassnoi라는 별명으로도 알려진)은 SQL을 사용한 예술적인 시각화 작업으로 유명합니다. 그의 블로그 Explain Extended에서는 SQL 쿼리만으로 크리스마스 트리와 같은 축제적인 디자인을 렌더링하는 것을 포함한 창의적인 SQL 시연을 선보입니다. 이것은 SQL의 표현력과 유연성을 보여주는 재미있고 독창적인 방법입니다.

---

## 3. Markus Winand: "Clustering Data: The Second Power of Indexing"

URL: http://use-the-index-luke.com/sql/clustering/index-only-scan-covering-index

Markus Winand의 책 *SQL Performance Explained*와 그의 웹사이트 "Use The Index Luke"에서 가져온 이 기사는 "커버링 인덱스(covering indexes)", "클러스터링 인덱스(clustering indexes)", 그리고 "인덱스 온리 스캔(index-only scans)"에 대해 다룹니다. 이러한 개념들은 많은 개발자들에게 과소평가되거나 알려지지 않은 강력한 SQL 성능 최적화 기법입니다. 인덱스가 어떻게 데이터를 클러스터링하고 쿼리 성능을 극적으로 향상시킬 수 있는지 이해하는 것은 모든 데이터베이스 개발자에게 필수적입니다.

---

## 4. Dimitri Fontaine: "Understanding Window Functions"

URL: http://tapoueh.org/blog/2013/08/20-Window-Functions

윈도우 함수에 대한 포괄적인 설명을 제공합니다. "윈도우 함수 이전의 SQL과 윈도우 함수 이후의 SQL이 있다"라고 말할 수 있을 정도입니다. 윈도우 함수는 SQL의 가장 강력하면서도 가장 활용되지 않는 기능 중 일부입니다. Oracle, SQL Server, DB2 등 상용 데이터베이스와 PostgreSQL에서 사용할 수 있는 이러한 강력한 분석 도구들은 복잡한 계산을 우아하게 처리할 수 있게 해줍니다.

---

## 5. Lukas Eder: "10 Common Mistakes Java Developers Make when Writing SQL"

URL: https://blog.jooq.org/10-common-mistakes-java-developers-make-when-writing-sql/

이것은 jOOQ 블로그의 저자 본인이 작성한 기사입니다. Java 개발자들이 SQL을 작성할 때 자주 범하는 실수들을 다룹니다. 이 기사는 엄청난 관심을 받았는데, 그 이유는 이러한 실수들이 Java 개발자뿐만 아니라 SQL을 사용하는 모든 개발자에게 적용되기 때문입니다. 실용적인 관련성 때문에 널리 공감을 얻었습니다.

---

## 6. András Gábor: "Techniques for Pagination in SQL"

URL: http://www.inf.unideb.hu/~gabora/pagination/results.html

페이지네이션 기법에 대한 기사입니다. 현대의 LIMIT/OFFSET 지원이 나오기 전에, 페이지네이션은 데이터베이스마다 다른 방식으로 구현되어야 했습니다. 이 기사는 Oracle의 ROWNUM 접근 방식을 포함한 다양한 상용 데이터베이스에서 적절한 기법을 사용하여 페이지네이션을 구현하는 방법을 다룹니다.

---

## 7. Markus Winand: "We need tool support for keyset pagination"

URL: http://use-the-index-luke.com/no-offset

기술적 관점과 비즈니스 관점 모두에서 OFFSET 페이지네이션을 비판하고, 키셋 페이지네이션(keyset pagination)을 우월한 대안으로 제시합니다. OFFSET 페이지네이션은 기술적으로 낭비적이며(데이터베이스가 건너뛸 모든 행을 처리해야 함), 비즈니스 측면에서도 제한된 가치를 제공합니다. 키셋 기반 대안이 왜 더 나은지에 대해 설명합니다.

---

## 8. Josh Berkus: "Tag All The Things"

URL: http://www.databasesoup.com/2015/01/tag-all-things.html

PostgreSQL에서 태깅 구현의 성능을 분석합니다. 정규화에 대한 가정에 의문을 제기하며, 공격적인 정규화가 항상 최적의 선택이 아닐 수 있음을 보여줍니다. 태그를 구현하는 다양한 방법의 성능 고려사항을 검토합니다.

---

## 9. Alek Bolenok: "10 things in SQL Server (which don't work as expected)"

URL: http://tech.pro/tutorial/1419/10-things-in-sql-server-which-don-t-work-as-expected

SQL Server의 미묘한 동작 차이와 다른 데이터베이스 구현과의 차이점을 보여줍니다. 데이터베이스별 특이점을 이해하는 데 유용하며, 교차 데이터베이스 인식을 위해 알아두면 좋은 내용들입니다. 예상대로 작동하지 않는 SQL Server의 여러 가지 사항들을 목록화합니다.

---

## 10. Aaron Bertrand: "Best approaches for running totals"

URL: http://sqlperformance.com/2012/07/t-sql-queries/running-totals

누계(running totals)를 계산하기 위한 SQL Server 2012 솔루션들을 요약합니다. 누계는 스프레드시트에서 익숙한 공식이며 보고 시나리오에서 일반적인 요구사항입니다. 이 기사는 SQL에서 누적 합계를 구현하는 여러 가지 접근 방식을 설명합니다.

---

물론, 유용한 SQL 트릭에 대한 깊은 통찰을 제공하는 다른 아주 좋은 기사들도 많이 있습니다. 이 목록을 잘 보완할 수 있는 기사를 발견하셨다면, 댓글 섹션에 링크와 설명을 남겨주세요. 미래의 독자들이 추가적인 통찰에 감사할 것입니다.

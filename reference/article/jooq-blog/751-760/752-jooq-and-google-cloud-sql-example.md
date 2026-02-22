# jOOQ와 Google Cloud SQL 예제

> 원문: https://blog.jooq.org/jooq-and-google-cloud-sql-example/

게시자: lukaseder
날짜: 2011년 10월 22일

---

이건 정말 너무 간단합니다. 다음은 jOOQ / Google Cloud SQL 통합 예제를 쉽게 만드는 방법입니다:

1. Google App Engine에 가입합니다
2. Google Cloud SQL에 가입합니다
3. Google App Engine 프로젝트를 생성합니다 (가급적 Eclipse를 사용하여)
4. 프로젝트에 jOOQ를 추가합니다
5. 프로젝트에 생성된 스키마를 추가합니다
6. 끝

Google Cloud SQL은 실제로 MySQL 데이터베이스이며, 개발 목적으로 로컬 머신에도 설치할 수 있습니다. jOOQ 통합에 있어서 이것은 일반 MySQL 데이터베이스를 사용하는 것과 똑같이 코드 생성 및 실행을 설정하면 된다는 것을 의미합니다. 간단하지 않나요?

- 실행 예제: http://jooq-test.appspot.com/jooq-test
- 소스 코드: https://github.com/lukaseder/jOOQ/blob/master/jOOQ-google-cloud-sql/src/org/jooq/test/JOOQTest.java
- 문서: https://code.google.com/apis/sql/docs/developers_guide_java.html

---

게시 후 참고 사항:

한 댓글 작성자가 GitHub 링크가 깨져 있다고 지적했습니다. 저자는 "Google Cloud SQL은 더 이상 무료로 제공되지 않으며 (개발자에게도)" 해당 예제는 더 이상 유지보수하지 않는다고 답변했습니다. 다만 jOOQ와 Cloud SQL의 통합은 "그냥 일반적인 MySQL 통합"일 뿐이라고 언급했습니다.

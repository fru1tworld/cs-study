# DB 마이그레이션을 위한 Liquibase

> 원문: https://blog.jooq.org/liquibase-for-db-migrations/

저는 방금 데이터베이스 마이그레이션을 위한 아주 멋진 도구를 발견했습니다: [Liquibase](https://www.liquibase.org/ "Liquibase. DB 마이그레이션을 위한 훌륭한 도구이자 jOOQ와의 협력 후보?"). Liquibase를 사용하면 DB 증분(increment)을 XML 파일로 모델링할 수 있으며, 이 파일은 최대 13개의 서로 다른 데이터베이스로 변환됩니다. 다음은 샘플 DB 증분입니다(Liquibase 매뉴얼에서 발췌):

```xml
<!--
  ALTER TABLE PERSON MODIFY COLUMN firstname VARCHAR(5000);
  -->
<modifyColumn tableName="person">
    <column name="firstname" type="varchar(5000)"/>
</modifyColumn>

<!--
  ALTER TABLE ADDRESS ADD CONSTRAINT fk_address_person
  FOREIGN KEY (person_id) REFERENCES person (id);
  -->
<addForeignKeyConstraint constraintName="fk_address_person"
    baseTableName="address" baseColumnNames="person_id"
    referencedTableName="person" referencedColumnNames="id"/>

<!--
  UPDATE ProductSettings SET property = 'vatCategory'
  WHERE property = 'vat';
  -->
<update tableName="ProductSettings">
    <column name="property" value="vatCategory"/>
    <where>property='vat'</where>
</update>
```

...등등. 이제 Liquibase 팀에 연락하여 협력을 요청할 때가 된 것 같습니다! 데이터베이스 스키마 관리, 데이터베이스 스키마 마이그레이션, 그리고 jOOQ의 소스 코드 생성을 포함하는 완전히 통합된 솔루션은 Java 데이터베이스 개발자에게 완벽한 도구 세트가 될 것입니다.

---

### 댓글

killbulle (2011년 11월 6일 16:11):

정말 재미있네요, 저도 데이터베이스 마이그레이션을 위한 최고의 도구를 찾고 있었습니다. 그래서 [Flyway의 Java Migration](https://code.google.com/p/flyway/wiki/JavaMigration) 사이트에서 왔는데, Java 마이그레이션을 지원하기 때문입니다. 그리고 이것을 jOOQ와 결합하면 더 좋을 것 같습니다 ;) 그래서 제 선택은 C5db migration 또는 Flyway + jOOQ로 SQL 실행을 하는 것이 될 수 있겠네요 ;)

lukaseder (2011년 11월 6일 17:35):

멋진 프레임워크네요. 저도 확실히 살펴봐야 할 것 같습니다. 마이그레이션 프레임워크를 jOOQ와 함께 사용하기 시작하면 경험을 알려주세요!

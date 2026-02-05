# SQL:2003 MERGE 문의 난해한 마법
> 원문: https://blog.jooq.org/arcane-magic-with-the-sql2003-merge-statement/

2011년 11월 29일, lukaseder

## 소개

종종 우리는 다음과 같은 이유들로 인해 INSERT와 UPDATE를 구분해야 하는 것에 불편함을 느낍니다:

- 최소 두 개의 문(statement)을 실행해야 합니다
- 성능에 대해 고민해야 합니다
- 경합 조건(race condition)에 대해 고민해야 합니다
- [UPDATE를 먼저 실행하고, UPDATE_COUNT가 0이면 INSERT] 방식과 [INSERT를 먼저 실행하고, 예외가 발생하면 UPDATE] 방식 중 하나를 선택해야 합니다
- 업데이트/삽입되는 레코드마다 이 문들을 실행해야 합니다

종합하면, 이것은 오류와 좌절의 큰 원인입니다. 동시에, SQL MERGE 문을 사용하면 이 모든 것이 매우 간단해질 수 있는데 말이죠!

## MERGE의 전형적인 사용 상황

다양한 사용 사례 중에서, MERGE 문은 다대다(many-to-many) 관계를 처리할 때 특히 유용합니다. 다음과 같은 스키마가 있다고 가정해 봅시다:

```sql
CREATE TABLE documents (
  id NUMBER(7) NOT NULL,
  CONSTRAINT docu_id PRIMARY KEY (id)
);

CREATE TABLE persons (
  id NUMBER(7) NOT NULL,
  CONSTRAINT pers_id PRIMARY KEY (id)
);

CREATE TABLE document_person (
  docu_id NUMBER(7) NOT NULL,
  pers_id NUMBER(7) NOT NULL,
  flag NUMBER(1) NULL,

  CONSTRAINT docu_pers_pk PRIMARY KEY (docu_id, pers_id),
  CONSTRAINT docu_pers_fk_docu
    FOREIGN KEY (docu_id) REFERENCES documents(id),
  CONSTRAINT docu_pers_fk_pers
    FOREIGN KEY (pers_id) REFERENCES persons(id)
);
```

위 테이블들은 어떤 사람이 어떤 문서를 읽었는지(flag=1) / 삭제했는지(flag=2)를 모델링하는 데 사용됩니다. 간단히 하기 위해, "document_person" 엔티티는 일반적으로 "documents"와 OUTER JOIN 됩니다. 이렇게 하면 "document-person" 레코드의 존재 여부가 동일한 의미를 가질 수 있습니다: "flag IS NULL"은 문서가 읽지 않은 상태임을 의미합니다. 이제 문서를 읽음으로 표시하려면, 새로운 "document_person"을 INSERT할지 기존 것을 UPDATE할지 결정해야 합니다. 삭제도 마찬가지입니다. 모든 문서를 읽음으로 표시하거나 모든 문서를 삭제하는 경우도 마찬가지입니다.

## 대신 MERGE를 사용하세요

하나의 문(statement)으로 모든 것을 처리할 수 있습니다! 한 사람에 대해 하나의 문서를 읽음으로 표시하기 위해 하나의 레코드를 INSERT/UPDATE하고 싶다고 가정해 봅시다:

```sql
-- 대상 테이블
MERGE INTO document_person dst

-- 데이터 소스. 이 경우에는 단순한 더미 레코드
USING (
  SELECT :docu_id as docu_id,
         :pers_id as pers_id,
         :flag    as flag
  FROM DUAL
) src

-- 병합 조건 (참이면 업데이트, 거짓이면 삽입)
ON (dst.docu_id = src.docu_id AND dst.pers_id = src.pers_id)

-- 업데이트 액션
WHEN MATCHED THEN UPDATE SET
  dst.flag = src.flag

-- 삽입 액션
WHEN NOT MATCHED THEN INSERT (
  dst.docu_id,
  dst.pers_id,
  dst.flag
)
VALUES (
  src.docu_id,
  src.pers_id,
  src.flag
)
```

이것은 MySQL의 INSERT .. ON DUPLICATE KEY UPDATE 문과 상당히 유사하지만, 훨씬 더 장황해 보입니다. MySQL 방식이 좀 더 간결합니다.

## 극한까지 활용하기

하지만 더 나아갈 수 있습니다! 앞서 말했듯이, 주어진 사람에 대해 모든 문서를 읽음으로 표시하고 싶을 수도 있습니다. MERGE를 사용하면 문제없습니다. 다음 문은 :docu_id를 지정하면 이전 문과 동일한 작업을 수행합니다. :docu_id를 null로 두면 모든 문서를 :flag로 표시합니다:

```sql
MERGE INTO document_person dst

-- 데이터 소스는 이제 모든 "documents"(또는 :docu_id만)를
-- "document_person" 매핑과 LEFT OUTER JOIN한 것
USING (
  SELECT d.id     as docu_id,
         :pers_id as pers_id,
         :flag    as flag
  FROM documents d
  LEFT OUTER JOIN document_person d_p
  ON d.id = d_p.docu_id AND d_p.pers_id = :pers_id
  -- :docu_id가 설정되어 있으면, 해당 문서만 선택
  WHERE (:docu_id IS NOT NULL AND d.id = :docu_id)
  -- 그렇지 않으면, 모든 문서를 선택
     OR (:docu_id IS NULL)
) src

-- 매핑이 이미 존재하면 업데이트. 그렇지 않으면 삽입
ON (dst.docu_id = src.docu_id AND dst.pers_id = src.pers_id)

-- 나머지는 동일
WHEN MATCHED THEN UPDATE SET
  dst.flag = src.flag
WHEN NOT MATCHED THEN INSERT (
  dst.docu_id,
  dst.pers_id,
  dst.flag
)
VALUES (
  src.docu_id,
  src.pers_id,
  src.flag
)
```

## jOOQ에서의 MERGE 지원

MERGE는 jOOQ에서도 완벽하게 지원됩니다. 더 자세한 내용은 매뉴얼을 참조하세요 (아래로 스크롤하세요): https://www.jooq.org/manual/JOOQ/Query/

즐거운 병합(merging) 되세요! :-)
